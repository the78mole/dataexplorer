/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.

    Copyright (c) 2017 Thomas Eickert
****************************************************************************************/

package gde.histo.datasources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

import gde.GDE;
import gde.device.DeviceConfiguration;
import gde.device.ScoreLabelTypes;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.NotSupportedFileFormatException;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.VaultCollector;
import gde.histo.cache.VaultReaderWriter;
import gde.histo.datasources.DirectoryScanner.DirectoryType;
import gde.histo.datasources.DirectoryScanner.SourceDataSet;
import gde.histo.datasources.HistoSet.RebuildStep;
import gde.histo.exclusions.ExclusionData;
import gde.histo.recordings.RecordingsCollector;
import gde.histo.recordings.TrailRecordSet;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

/**
 * Convert the source files into a trail recordSet required for the history tabs.
 * @author Thomas Eickert (USER)
 */
public final class HistoSetCollector {
	private final static String												$CLASS_NAME								= HistoSetCollector.class.getName();
	private final static Logger												log												= Logger.getLogger($CLASS_NAME);

	private static final boolean											DISCARD_DUPLICATE_VAULTS	= true;

	private final DataExplorer												application								= DataExplorer.getInstance();
	private final DirectoryScanner										directoryScanner;

	/** Sorted by recordSet startTimeStamp in reverse order; each timestamp may hold multiple vaults. */
	private final TreeMap<Long, List<ExtendedVault>>	histoVaults								= new TreeMap<>(Collections.reverseOrder());
	private final Map<String, VaultCollector>					unsuppressedTrusses				= new HashMap<>();													// authorized recordsets (excluded vaults eliminated - by the user in suppress mode)
	private final Map<String, VaultCollector>					suppressedTrusses					= new HashMap<>();													// excluded vaults
	private final LongAdder														duplicatesCount						= new LongAdder();
	private int																				readFilesCount						= 0;

	private long																			recordSetBytesSum					= 0;																				// size of all the histo files which have been read to build the histo recordsets
	private int																				elapsedTime_ms						= 0;																				// total time for rebuilding the HistoSet

	private TrailRecordSet														trailRecordSet						= null;																			// histo data transformed in a recordset format

	public enum LoadProgress {
		STARTED(2), INITIALIZED(5), SCANNED(11), MATCHED(22), RESTORED(50), LOADED(80), CACHED(97), CONVERTED(99);
		public int endPercentage;

		private LoadProgress(int endPercentage) {
			this.endPercentage = endPercentage;
		}
	}

	/**
	 * Supports progress bars in loops and a simple progress bar setting.
	 * @author Thomas Eickert (USER)
	 */
	public class ProgressManager {

		private final DataExplorer	presenter			= DataExplorer.getInstance();
		private final String				sThreadId			= String.format("%06d", Thread.currentThread().getId());	//$NON-NLS-1$
		private final boolean				isWithUi;																																//

		private double							progressCycle	= 0.;
		private int									progressStart	= 0;
		private int									stepSize			= 0;
		private int									counter				= 0;

		public ProgressManager(boolean isWithUi) {
			this.isWithUi = isWithUi;
		}

		public ProgressManager(boolean isWithUi, int newEndPercentage, int newTotalCount, int newStepSize) {
			this(isWithUi);
			reInit(newEndPercentage, newTotalCount, newStepSize);
		}

		/**
		 * To be called prior to looping with doCount.
		 * @param newEndPercentage is reached after the total number of counts
		 * @param newTotalCount
		 * @param newStepSize defines the number of counts which are required before the bar is updated again (performance)
		 */
		public void reInit(int newEndPercentage, int newTotalCount, int newStepSize) {
			if (this.isWithUi) {
				this.progressStart = this.presenter.getProgressPercentage();
				this.stepSize = newStepSize;
				this.counter = 0;
				if (newTotalCount == 0) {
					this.presenter.setProgress(newEndPercentage, this.sThreadId);
					this.progressCycle = 0.;
				} else {
					this.presenter.setProgress(this.progressStart, this.sThreadId);
					this.progressCycle = (newEndPercentage - this.progressStart) / (double) newTotalCount;
				}
			}
		}

		/**
		 * Method option for calling in every loop.
		 * @param increment is the value added to the counter
		 */
		public void countInLoop(int increment) {
			setInLoop(this.counter + increment);
		}

		/**
		 * Method option for calling in every loop.
		 * @param newCounter
		 */
		public void setInLoop(int newCounter) {
			if (this.isWithUi) {
				if (this.progressCycle <= 0)
					throw new UnsupportedOperationException();
				else if (newCounter % this.stepSize == 0) {
					this.counter = newCounter;
					this.presenter.setProgress((int) (newCounter * this.stepSize * this.progressCycle + this.progressStart), this.sThreadId);
				} else
					; // progress is set if stepSize is reached
			}
		}

		/**
		 * Set the progress bar percentage.
		 * @param percentage value (reset for 0 or bigger than 99)
		 */
		public void set(int percentage) {
			if (this.isWithUi) this.presenter.setProgress(percentage, this.sThreadId);
		}
	}

	public HistoSetCollector() {
		this.directoryScanner = new DirectoryScanner();
	}

	@Override
	public String toString() {
		return String.format("totalTrussesCount=%,d  availableVaultsCount=%,d recordSetBytesSum=%,d elapsedTime_ms=%,d", //$NON-NLS-1$
				this.getSuppressedTrusses().size() + this.getUnsuppressedTrusses().size(), this.getTimeStepSize(), this.recordSetBytesSum, this.elapsedTime_ms);
	}

	/**
	 * Re- initializes the class.
	 */
	public synchronized void initialize() {
		// deep clear in order to reduce memory consumption prior to garbage collection
		for (List<ExtendedVault> timestampHistoVaults : histoVaults.values()) {
			timestampHistoVaults.clear();
		}
		histoVaults.clear();

		this.recordSetBytesSum = 0;
		this.duplicatesCount.reset();
		this.elapsedTime_ms = 0;

		this.trailRecordSet = null;
	}

	public void rebuild4Test(Path filePath, TreeMap<String, DeviceConfiguration> devices)//
			throws IOException, NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {

		initialize();

		this.unsuppressedTrusses.clear();
		this.suppressedTrusses.clear();
		List<SourceDataSet> sourceFiles = this.directoryScanner.readSourceFiles4Test(filePath);
		this.readFilesCount = sourceFiles.size();
		addTrusses(sourceFiles, devices);
		log.log(Level.INFO, String.format("%04d files selected", this.unsuppressedTrusses.size())); //$NON-NLS-1$

		{
			// step: build the workload map consisting of the cache key and the file path
			Map<Path, Map<String, VaultCollector>> trussJobs = getTrusses4Screening();
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("trussJobs to load total     = %d", trussJobs.size())); //$NON-NLS-1$

			// step: put cached vaults into the histoSet map and reduce workload map
			int trussJobsSize = trussJobs.size();
			ProgressManager progress = new ProgressManager(false);
			for (ExtendedVault histoVault : VaultReaderWriter.loadVaultsFromCache(trussJobs, progress)) {
				if (!histoVault.isTruss()) {
					putVault(histoVault);
					this.recordSetBytesSum += histoVault.getScorePoint(ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal());
				}
			}
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("trussJobs loaded from cache = %d", trussJobsSize - trussJobs.size())); //$NON-NLS-1$

			// step: transform log files for the truss jobs into vaults and put them into the histoSet map
			ArrayList<ExtendedVault> newVaults = new ArrayList<>();
			for (Map.Entry<Path, Map<String, VaultCollector>> pathEntry : trussJobs.entrySet()) {
				try {
					for (ExtendedVault histoVault : VaultReaderWriter.loadVaultsFromFile(pathEntry.getKey(), pathEntry.getValue())) {
						if (!histoVault.isTruss()) {
							putVault(histoVault);
							this.recordSetBytesSum += histoVault.getScorePoint(ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal());
						}
						newVaults.add(histoVault);
					}
				} catch (Exception e) {
					throw new UnsupportedOperationException(pathEntry.getKey().toString(), e);
				}
			}
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("trussJobs loaded from file  = %d", newVaults.size())); //$NON-NLS-1$

			// step: save vaults in the file system
			if (newVaults.size() > 0) {
				VaultReaderWriter.storeVaultsInCache(newVaults);
			}
		}
		{
			this.trailRecordSet = TrailRecordSet.createRecordSet(this.application.getActiveDevice(), this.application.getActiveChannelNumber(), this.histoVaults);
		}
	}

	/**
	 * Determine the valid histo files and read the vaults from the log files or the cache.
	 * Use the vaults to populate the trail recordset.
	 * @param rebuildStep is the step type where the processing starts. The previous job types are omitted.
	 * 				May be disregarded if the device, channel or object was modified.
	 * @param isWithUi true allows actions on the user interface (progress bar, message boxes)
	 * @return true if the trail recordSet was rebuilt
	 */
	public synchronized boolean rebuild4Screening(RebuildStep rebuildStep, boolean isWithUi)
			throws FileNotFoundException, IOException, NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {
		ProgressManager progress = new ProgressManager(isWithUi);

		try {
			long startNanoTime = System.nanoTime();
			boolean isRebuilt = false;
			log.log(Level.FINER, GDE.STRING_GREATER, rebuildStep);
			if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) progress.set(LoadProgress.STARTED.endPercentage);

			long startTimeFileValid = new Date().getTime();
			{
				if (RebuildStep.A_HISTOSET == rebuildStep) {
					isRebuilt = true;
					this.directoryScanner.initialize();
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("directoryScanner      initialize")); //$NON-NLS-1$
					progress.set(LoadProgress.INITIALIZED.endPercentage);
				}
			}
			boolean isHistoFilePathsValid = this.directoryScanner.validateHistoFilePaths(rebuildStep);
			{
				if (!isHistoFilePathsValid) {
					initialize();
					this.unsuppressedTrusses.clear();
					this.suppressedTrusses.clear();
					List<SourceDataSet> sourceFiles = this.directoryScanner.readSourceFiles();
					this.readFilesCount = sourceFiles.size();
					addTrusses(sourceFiles, DataExplorer.getInstance().getDeviceSelectionDialog().getDevices());

					if ((new Date().getTime() - startTimeFileValid) > 0) log.log(Level.TIME, String.format("%,5d trusses    select folders     time=%,6d [ms] :: per second:%5d", //
							this.unsuppressedTrusses.size(), new Date().getTime() - startTimeFileValid, this.unsuppressedTrusses.size() * 1000 / (new Date().getTime() - startTimeFileValid)));
				} else { // histo record sets are ready to use
					if (log.isLoggable(Level.TIME)) log.log(Level.TIME, String.format("%,5d trusses   file paths verified     time=%s [ss.SSS]", //
							this.unsuppressedTrusses.size(), StringHelper.getFormatedDuration("ss.SSS", new Date().getTime() - startTimeFileValid))); //$NON-NLS-1$
				}
				progress.set(LoadProgress.SCANNED.endPercentage);
			}
			{
				if (!isHistoFilePathsValid || EnumSet.of(RebuildStep.A_HISTOSET, RebuildStep.B_HISTOVAULTS).contains(rebuildStep)) {
					isRebuilt = true;
					this.recordSetBytesSum = 0;
					if (this.unsuppressedTrusses.size() <= 0)
						progress.set(LoadProgress.CACHED.endPercentage);
					else {
						Map<Path, Map<String, VaultCollector>> trussJobs;
						{
							long nanoTimeCheckFilesSum = -System.nanoTime();
							// step: build the workload map consisting of the cache key and the file path
							trussJobs = getTrusses4Screening();
							nanoTimeCheckFilesSum += System.nanoTime();
							if (TimeUnit.NANOSECONDS.toMillis(nanoTimeCheckFilesSum) > 0) log.log(Level.FINER,
									String.format("%,5d trusses    job check          time=%,6d [ms] :: per second:%5d", trussJobs.values().parallelStream().mapToInt(c -> c.size()).sum(), //$NON-NLS-1$
											TimeUnit.NANOSECONDS.toMillis(nanoTimeCheckFilesSum), this.unsuppressedTrusses.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeCheckFilesSum)));
							progress.set(LoadProgress.MATCHED.endPercentage);
						}
						long recordSetBytesCachedSum = 0;
						{// step: put cached vaults into the histoSet map and reduce workload map
							long nanoTimeReadVaultSum = -System.nanoTime();
							int tmpHistoSetsSize = this.histoVaults.size();
							int jobSize = trussJobs.values().parallelStream().mapToInt(c -> c.size()).sum();
							progress.reInit(LoadProgress.RESTORED.endPercentage, jobSize, 1);
							for (ExtendedVault histoVault : VaultReaderWriter.loadVaultsFromCache(trussJobs, progress)) {
								if (!histoVault.isTruss()) {
									putVault(histoVault);
									recordSetBytesCachedSum += histoVault.getScorePoint(ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal());
								} else {
									log.log(Level.INFO, String.format("vault has no log data %,7d kiB %s", histoVault.getLogFileLength() / 1024 ,histoVault.getLogFilePath()));
								}
							}
							this.recordSetBytesSum = recordSetBytesCachedSum;
							nanoTimeReadVaultSum += System.nanoTime();
							if (TimeUnit.NANOSECONDS.toMillis(nanoTimeReadVaultSum) > 0) log.log(Level.TIME,
									String.format("%,5d vaults     load from cache    time=%,6d [ms] :: per second:%5d :: Rate=%,6d MB/s", this.histoVaults.size() - tmpHistoSetsSize, //$NON-NLS-1$
											TimeUnit.NANOSECONDS.toMillis(nanoTimeReadVaultSum), (this.histoVaults.size() - tmpHistoSetsSize) * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeReadVaultSum),
											this.recordSetBytesSum / TimeUnit.NANOSECONDS.toMicros(nanoTimeReadVaultSum)));
							int realProgress = DataExplorer.application.getProgressPercentage() + (int) ((LoadProgress.CACHED.endPercentage - DataExplorer.application.getProgressPercentage())
									* this.histoVaults.size() / (double) (this.histoVaults.size() + 10 * jobSize)); // 10 is the estimated processing time ratio between reading from files and reading from cache
							progress.set(Math.max(LoadProgress.RESTORED.endPercentage, realProgress));
						}
						ArrayList<ExtendedVault> newVaults = new ArrayList<>();
						{// step: transform log files from workload map into vaults and put them into the histoSet map
							long nanoTimeReadRecordSetSum = -System.nanoTime();
							int jobSize = trussJobs.values().parallelStream().mapToInt(c -> c.size()).sum();
							progress.reInit(LoadProgress.CACHED.endPercentage, jobSize, 1);
							for (Map.Entry<Path, Map<String, VaultCollector>> trussJobsEntry : trussJobs.entrySet()) {
								for (ExtendedVault histoVault : VaultReaderWriter.loadVaultsFromFile(trussJobsEntry.getKey(), trussJobsEntry.getValue())) {
									if (!histoVault.isTruss()) {
										putVault(histoVault);
										this.recordSetBytesSum += histoVault.getScorePoint(ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal());
									} else {
										log.log(Level.INFO, String.format("vault has no log data %,7d kiB %s", histoVault.getLogFileLength() / 1024 ,histoVault.getLogFilePath()));
									}
									newVaults.add(histoVault);
								}
								progress.countInLoop(trussJobsEntry.getValue().size());
							}
							nanoTimeReadRecordSetSum += System.nanoTime();
							if (newVaults.size() > 0) log.log(Level.TIME, String.format("%,5d recordsets create from files  time=%,6d [ms] :: per second:%5d :: Rate=%,6d MB/s", newVaults.size(), //$NON-NLS-1$
									TimeUnit.NANOSECONDS.toMillis(nanoTimeReadRecordSetSum), newVaults.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeReadRecordSetSum),
									(this.recordSetBytesSum - recordSetBytesCachedSum) / TimeUnit.NANOSECONDS.toMicros(nanoTimeReadRecordSetSum)));
						}
						{// step: save vaults in the file system
							long nanoTimeWriteVaultSum = -System.nanoTime(), cacheSize_B = 0;
							if (newVaults.size() > 0) {
								cacheSize_B = VaultReaderWriter.storeVaultsInCache(newVaults);
							}
							nanoTimeWriteVaultSum += System.nanoTime();
							if (TimeUnit.NANOSECONDS.toMillis(nanoTimeWriteVaultSum) > 0 && cacheSize_B > 0) log.log(Level.TIME,
									String.format("%,5d recordsets store in cache     time=%,6d [ms] :: per second:%5d :: Rate=%,6d MB/s", newVaults.size(), //$NON-NLS-1$
											TimeUnit.NANOSECONDS.toMillis(nanoTimeWriteVaultSum), newVaults.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeWriteVaultSum),
											(this.recordSetBytesSum - recordSetBytesCachedSum) / TimeUnit.NANOSECONDS.toMicros(nanoTimeWriteVaultSum)));
							progress.set(LoadProgress.CACHED.endPercentage);
						}
					}
				}
				{
					if (!isHistoFilePathsValid || EnumSet.of(RebuildStep.A_HISTOSET, RebuildStep.B_HISTOVAULTS, RebuildStep.C_TRAILRECORDSET).contains(rebuildStep)) {
						isRebuilt = true;
						long nanoTimeTrailRecordSet = -System.nanoTime();
						this.trailRecordSet = TrailRecordSet.createRecordSet(this.application.getActiveDevice(), this.application.getActiveChannelNumber(), this.histoVaults);
						// this.trailRecordSet.checkAllDisplayable();
						this.trailRecordSet.applyTemplate(true); // needs reasonable data
						nanoTimeTrailRecordSet += System.nanoTime();
						if (this.recordSetBytesSum > 0 && log.isLoggable(Level.FINE)) log.log(Level.FINE,
								String.format("%,5d timeSteps  to TrailRecordSet  time=%,6d [ms] :: per second:%5d", this.histoVaults.size(), TimeUnit.NANOSECONDS.toMillis(nanoTimeTrailRecordSet), //$NON-NLS-1$
										this.histoVaults.size() > 0 ? this.histoVaults.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeTrailRecordSet) : 0));
						log.log(Level.TIME, String.format("%,5d timeSteps  total              time=%,6d [ms] :: per second:%5d :: Rate=%,6d MB/s", this.histoVaults.size(), //$NON-NLS-1$
								new Date().getTime() - startTimeFileValid, (int) (this.histoVaults.size() * 1000. / (new Date().getTime() - startTimeFileValid) + .5),
								(int) (this.recordSetBytesSum / 1000. / (new Date().getTime() - startTimeFileValid) + .5)));
					}
				}
				{
					if (EnumSet.of(RebuildStep.D_TRAIL_DATA).contains(rebuildStep)) { // saves some time compared to the logic above
						isRebuilt = true;
						RecordingsCollector.refillRecordSet(this.trailRecordSet);
					}
					progress.set(LoadProgress.CONVERTED.endPercentage);
				}
			}
			this.elapsedTime_ms = (int) ((System.nanoTime() - startNanoTime + 500000) / 1000000);
			if (log.isLoggable(Level.FINE)) log.log(Level.TIME, "time = " + StringHelper.getFormatedTime("mm:ss:SSS", this.elapsedTime_ms)); //$NON-NLS-1$ //$NON-NLS-2$
			return isRebuilt;
		} finally {
			progress.set(100);
		}
	}

	/**
	 * Determine trusses in the osd files and in the native files (if supported by the device).
	 * Use ignore lists (for recordSets only) to determine the vaults which are required for the data access.
	 * @param sourceFiles for osd reader (possibly link files) or for import
	 */
	private void addTrusses(List<SourceDataSet> sourceFiles, TreeMap<String, DeviceConfiguration> deviceConfigurations) throws IOException, NotSupportedFileFormatException {
		final int suppressedSize = this.suppressedTrusses.size();
		final int unsuppressedSize = this.unsuppressedTrusses.size();
		for (SourceDataSet sourceFile : sourceFiles) {
			List<VaultCollector> trusses = sourceFile.getTrusses(deviceConfigurations);
			if (!trusses.isEmpty()) {
				for (VaultCollector truss : trusses) {
					ExtendedVault vault = truss.getVault();
					if (ExclusionData.isExcluded(vault.getLogFileAsPath(), vault.getLogRecordsetBaseName())) {
						log.log(Level.INFO, String.format("discarded as per exclusion list   %s %s   %s", vault.getLogFilePath(), vault.getLogRecordsetBaseName(), vault.getStartTimeStampFormatted()));
						this.suppressedTrusses.put(vault.getVaultName(), truss);
					} else {
						this.unsuppressedTrusses.put(vault.getVaultName(), truss);
					}
				}
			} else {
				log.log(Level.INFO, String.format("file has no valid data%,7d kiB %s", sourceFile.getFile().length() / 1024 ,sourceFile.getPath()));
			}
		}

		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%04d files found --- %04d total trusses --- %04d excluded trusses", sourceFiles.size(), //$NON-NLS-1$
				this.unsuppressedTrusses.size() - unsuppressedSize + this.suppressedTrusses.size() - suppressedSize, this.suppressedTrusses.size() - suppressedSize));
	}

	/**
	 * Determine the vaults which are required for the data access.
	 * @return trussJobs with the actual path (not the link file path) and a map of vault skeletons (trusses)
	*/
	private Map<Path, Map<String, VaultCollector>> getTrusses4Screening() throws IOException, NotSupportedFileFormatException {
		final Map<Path, Map<String, VaultCollector>> trusses4Paths = new LinkedHashMap<>();
		final Map<Long, Set<ExtendedVault>> trusses4StartTimes = new HashMap<Long, Set<ExtendedVault>>();

		for (VaultCollector truss : this.unsuppressedTrusses.values()) {
			if (DISCARD_DUPLICATE_VAULTS) {
				// add the truss to the multimap to find out if it is a duplicate according to the equals method criteria
				long logStartTimestamp_ms = truss.getVault().getLogStartTimestamp_ms();
				if (!trusses4StartTimes.containsKey(logStartTimestamp_ms)) trusses4StartTimes.put(logStartTimestamp_ms, new HashSet<ExtendedVault>());
				if (!trusses4StartTimes.get(logStartTimestamp_ms).add(truss.getVault())) {
					this.duplicatesCount.increment();
					log.log(Level.WARNING, "duplicate vault was discarded: ", truss);
					continue;
				}
			}

			Path path = truss.getVault().getLogFileAsPath();
			if (!trusses4Paths.containsKey(path)) trusses4Paths.put(path, new HashMap<String, VaultCollector>());
			trusses4Paths.get(path).put(truss.getVault().getVaultFileName().toString(), truss);
		}
		return trusses4Paths;
	}

	/**
	 * @param histoVault
	 */
	public void putVault(ExtendedVault histoVault) {
		List<ExtendedVault> timeStampHistoVaults = this.histoVaults.get(histoVault.getLogStartTimestamp_ms());
		if (timeStampHistoVaults == null) {
			this.histoVaults.put(histoVault.getLogStartTimestamp_ms(), timeStampHistoVaults = new ArrayList<ExtendedVault>());
		}
		timeStampHistoVaults.add(histoVault);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("added   startTimeStamp=%s  %s  logRecordSetOrdinal=%d  logChannelNumber=%d  %s", //$NON-NLS-1$
				histoVault.getStartTimeStampFormatted(), histoVault.getVaultFileName(), histoVault.getLogRecordSetOrdinal(), histoVault.getLogChannelNumber(), histoVault.getLogFilePath()));
	}

	public long getDuplicateTrussesCount() {
		return this.duplicatesCount.sum();
	}

	/**
	 * @return the number of timestamp entries
	 */
	public int getTimeStepSize() {
		return this.getTrailRecordSet() != null ? this.getTrailRecordSet().getTimeStepSize() : 0;
	}

	public int getElapsedTime_ms() {
		return this.elapsedTime_ms;
	}

	/**
	 * Is thread safe with respect to concurrent rebuilds or initializings.
	 */
	public synchronized TrailRecordSet getTrailRecordSet() {
		return this.trailRecordSet;
	}

	public long getRecordSetBytesSum() {
		return this.recordSetBytesSum;
	}

	public Map<String, VaultCollector> getUnsuppressedTrusses() {
		return this.unsuppressedTrusses;
	}

	public Map<String, VaultCollector> getSuppressedTrusses() {
		return this.suppressedTrusses;
	}

	public List<Path> getIgnoredFiles() {
		return this.directoryScanner.getIgnoredFiles();
	}

	public Map<DirectoryType, Path> getValidatedDirectories() {
		return this.directoryScanner.getValidatedDirectories();
	}

	public int getDirectoryFilesCount() {
		return this.readFilesCount + this.directoryScanner.getNonWorkableCount() + this.directoryScanner.getIgnoredFiles().size();
	}

	public int getReadFilesCount() {
		return this.readFilesCount;
	}

}
