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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import gde.GDE;
import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.device.ScoreLabelTypes;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.NotSupportedFileFormatException;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.HistoVault;
import gde.histo.cache.VaultCollector;
import gde.histo.cache.VaultReaderWriter;
import gde.histo.datasources.HistoSet.RebuildStep;
import gde.histo.device.IHistoDevice;
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
	private final static String		$CLASS_NAME							= HistoSetCollector.class.getName();
	private final static Logger		log											= Logger.getLogger($CLASS_NAME);

	private static final boolean	UNIQUE_HISTO_TIME_STAMP	= false;

	private final DataExplorer		application							= DataExplorer.getInstance();
	private final Settings				settings								= Settings.getInstance();

	private HistoSet							histoSet;
	private int										matchingTrussesCount		= 0;																// selected by menu settings (eliminated by checks, e.g. device, object and retrospect period)
	private int										availableTrussesCount		= 0;																// added to the trailrecordset (empty vaults eliminated, identical vaults eliminated based on setting)
	private long									fileSizeSum_B						= 0;																// size of all the histo files which have been read to build the histo recordsets
	private int										elapsedTime_ms					= 0;																// total time for rebuilding the HistoSet

	private TrailRecordSet				trailRecordSet					= null;															// histo data transformed in a recordset format

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
				}
				else {
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
				}
				else
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

	/**
	 * Re- initializes the class.
	 */
	public synchronized void initialize() {
		this.histoSet = HistoSet.getInstance();

		this.fileSizeSum_B = 0;
		this.matchingTrussesCount = 0;
		this.availableTrussesCount = 0;
		this.elapsedTime_ms = 0;

		this.trailRecordSet = null;
	}

	public void rebuild4Test(TreeMap<String, DeviceConfiguration> deviceConfigurations) throws IOException, NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {
		// this.clear();
		{
			if (this.histoSet.getUnsuppressedTrusses().size() > 0) {
				// step: build the workload map consisting of the cache key and the file path
				Map<Path, Map<String, VaultCollector>> trussJobs = getTrusses4Screening(deviceConfigurations);
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("trussJobs to load total     = %d", trussJobs.size())); //$NON-NLS-1$

				// step: put cached vaults into the histoSet map and reduce workload map
				int trussJobsSize = trussJobs.size();
				ProgressManager progress = new ProgressManager(false);
				for (ExtendedVault histoVault : VaultReaderWriter.loadVaultsFromCache(trussJobs, progress)) {
					if (!histoVault.isTruss()) {
						this.histoSet.putVault(histoVault);
						this.fileSizeSum_B += histoVault.getScorePoint(ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal());
					}
				}
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("trussJobs loaded from cache = %d", trussJobsSize - trussJobs.size())); //$NON-NLS-1$

				// step: transform log files for the truss jobs into vaults and put them into the histoSet map
				ArrayList<ExtendedVault> newVaults = new ArrayList<>();
				for (Map.Entry<Path, Map<String, VaultCollector>> pathEntry : trussJobs.entrySet()) {
					try {
						for (ExtendedVault histoVault : VaultReaderWriter.loadVaultsFromFile(pathEntry.getKey(), pathEntry.getValue())) {
							if (!histoVault.isTruss()) {
								this.histoSet.putVault(histoVault);
								this.fileSizeSum_B += histoVault.getScorePoint(ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal());
							}
							newVaults.add(histoVault);
						}
					}
					catch (Exception e) {
						throw new UnsupportedOperationException(pathEntry.getKey().toString(), e);
					}
				}
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("trussJobs loaded from file  = %d", newVaults.size())); //$NON-NLS-1$

				// step: save vaults in the file system
				if (newVaults.size() > 0) {
					VaultReaderWriter.storeVaultsInCache(newVaults);
				}
			}
		}
	}

	/**
	 * Determine histo files, build a recordset based job list and read from the log file or the cache for each job.
	 * Populate the trail recordset.
	 * Disregard rebuild steps if histo file paths have changed which may occur if new files have been added by the user or the device, channel or object was modified.
	 * @param rebuildStep
	 * @param isWithUi true allows actions on the user interface (progress bar, message boxes)
	 * @return true if the HistoSet was rebuilt
	 * @throws DataTypeException
	 * @throws DataInconsitsentException
	 * @throws NotSupportedFileFormatException
	 * @throws IOException
	 * @throws FileNotFoundException
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
					this.histoSet.initialize();
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("histoSet             initialize")); //$NON-NLS-1$
					progress.set(LoadProgress.INITIALIZED.endPercentage);
				}
			}
			boolean isHistoFilePathsValid = this.histoSet.getDirectoryScanner().validateHistoFilePaths(rebuildStep);
			int trussesCount = this.histoSet.getUnsuppressedTrusses().size();
			{
				if (!isHistoFilePathsValid) {
					this.histoSet.clear();
					if ((new Date().getTime() - startTimeFileValid) > 0) log.log(Level.TIME, String.format("%,5d trusses        select folders     time=%,6d [ms]  ::  per second:%5d", trussesCount, //$NON-NLS-1$
							new Date().getTime() - startTimeFileValid, trussesCount * 1000 / (new Date().getTime() - startTimeFileValid)));
				}
				else { // histo record sets are ready to use
					if (log.isLoggable(Level.TIME)) log.log(Level.TIME, String.format("%,5d trusses  file paths verified     time=%s [ss.SSS]", trussesCount, //$NON-NLS-1$
							StringHelper.getFormatedDuration("ss.SSS", new Date().getTime() - startTimeFileValid))); //$NON-NLS-1$
				}
				progress.set(LoadProgress.SCANNED.endPercentage);
			}
			{
				if (!isHistoFilePathsValid || EnumSet.of(RebuildStep.A_HISTOSET, RebuildStep.B_HISTOVAULTS).contains(rebuildStep)) {
					isRebuilt = true;
					this.fileSizeSum_B = 0;
					if (trussesCount <= 0)
						progress.set(LoadProgress.CACHED.endPercentage);
					else {
						Map<Path, Map<String, VaultCollector>> trussJobs;
						{
							long nanoTimeCheckFilesSum = -System.nanoTime();
							// step: build the workload map consisting of the cache key and the file path
							trussJobs = getTrusses4Screening(DataExplorer.getInstance().getDeviceSelectionDialog().getDevices());
							nanoTimeCheckFilesSum += System.nanoTime();
							if (TimeUnit.NANOSECONDS.toMillis(nanoTimeCheckFilesSum) > 0)
								log.log(Level.TIME, String.format("%,5d trusses        job check          time=%,6d [ms]  ::  per second:%5d", trussesCount, //$NON-NLS-1$
										TimeUnit.NANOSECONDS.toMillis(nanoTimeCheckFilesSum), trussesCount * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeCheckFilesSum)));
							progress.set(LoadProgress.MATCHED.endPercentage);
						}
						long fileSizeSumCached_B = 0;
						{// step: put cached vaults into the histoSet map and reduce workload map
							long nanoTimeReadVaultSum = -System.nanoTime();
							int tmpHistoSetsSize = this.histoSet.size();
							int jobSize = trussJobs.entrySet().parallelStream().mapToInt(c -> c.getValue().size()).sum();
							progress.reInit(LoadProgress.RESTORED.endPercentage, jobSize, 1);
							for (ExtendedVault histoVault : VaultReaderWriter.loadVaultsFromCache(trussJobs, progress)) {
								if (!histoVault.isTruss()) {
									this.histoSet.putVault(histoVault);
									fileSizeSumCached_B += histoVault.getScorePoint(ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal());
								}
							}
							this.fileSizeSum_B = fileSizeSumCached_B;
							nanoTimeReadVaultSum += System.nanoTime();
							if (TimeUnit.NANOSECONDS.toMillis(nanoTimeReadVaultSum) > 0) log.log(Level.TIME,
								String.format("%,5d vaults         load from cache    time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,6d MB/s", this.histoSet.size() - tmpHistoSetsSize, //$NON-NLS-1$
											TimeUnit.NANOSECONDS.toMillis(nanoTimeReadVaultSum), (this.histoSet.size() - tmpHistoSetsSize) * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeReadVaultSum),
											this.fileSizeSum_B / TimeUnit.NANOSECONDS.toMicros(nanoTimeReadVaultSum)));
							int realProgress = DataExplorer.application.getProgressPercentage()
									+ (int) ((LoadProgress.CACHED.endPercentage - DataExplorer.application.getProgressPercentage()) * this.histoSet.size() / (double) (this.histoSet.size() + 10 * jobSize)); // 10 is the estimated processing time ratio between reading from files and reading from cache
							progress.set(Math.max(LoadProgress.RESTORED.endPercentage, realProgress));
						}
						ArrayList<ExtendedVault> newVaults = new ArrayList<>();
						{// step: transform log files from workload map into vaults and put them into the histoSet map
							long nanoTimeReadRecordSetSum = -System.nanoTime();
							int jobSize = trussJobs.entrySet().parallelStream().mapToInt(c -> c.getValue().size()).sum();
							progress.reInit(LoadProgress.CACHED.endPercentage, jobSize, 1);
							for (Map.Entry<Path, Map<String, VaultCollector>> trussJobsEntry : trussJobs.entrySet()) {
								for (ExtendedVault histoVault : VaultReaderWriter.loadVaultsFromFile(trussJobsEntry.getKey(), trussJobsEntry.getValue())) {
									if (!histoVault.isTruss()) {
										this.histoSet.putVault(histoVault);
										this.fileSizeSum_B += histoVault.getScorePoint(ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal());
									}
									newVaults.add(histoVault);
								}
								progress.countInLoop(trussJobsEntry.getValue().size());
							}
							nanoTimeReadRecordSetSum += System.nanoTime();
							if (newVaults.size() > 0) log.log(Level.TIME,
								String.format("%,5d recordsets     create from files  time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,6d MB/s", newVaults.size(), //$NON-NLS-1$
											TimeUnit.NANOSECONDS.toMillis(nanoTimeReadRecordSetSum), newVaults.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeReadRecordSetSum),
											(this.fileSizeSum_B - fileSizeSumCached_B) / TimeUnit.NANOSECONDS.toMicros(nanoTimeReadRecordSetSum)));
						}
						{// step: save vaults in the file system
							long nanoTimeWriteVaultSum = -System.nanoTime(), cacheSize_B = 0;
							if (newVaults.size() > 0) {
								cacheSize_B = VaultReaderWriter.storeVaultsInCache(newVaults);
							}
							nanoTimeWriteVaultSum += System.nanoTime();
							if (TimeUnit.NANOSECONDS.toMillis(nanoTimeWriteVaultSum) > 0 && cacheSize_B > 0) log.log(Level.TIME,
								String.format("%,5d recordsets     store in cache     time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,6d MB/s", newVaults.size(), //$NON-NLS-1$
											TimeUnit.NANOSECONDS.toMillis(nanoTimeWriteVaultSum), newVaults.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeWriteVaultSum),
											(this.fileSizeSum_B - fileSizeSumCached_B) / TimeUnit.NANOSECONDS.toMicros(nanoTimeWriteVaultSum)));
							progress.set(LoadProgress.CACHED.endPercentage);
						}
						// step: identify duplicate vaults (origin is duplicated log files with the same contents)
						if (UNIQUE_HISTO_TIME_STAMP)
							for (List<ExtendedVault> vaults : this.histoSet.values().parallelStream().filter(l -> l.size() > 1).collect(Collectors.toList())) {
								Set<Integer> channelNumbers = new HashSet<>();
								for (Iterator<ExtendedVault> iterator = vaults.iterator(); iterator.hasNext();) {
									HistoVault histoVault = iterator.next();
									if (channelNumbers.contains(histoVault.getLogChannelNumber()))
										iterator.remove();
									else
										channelNumbers.add(histoVault.getLogChannelNumber());
								}
							}
						else
							this.histoSet.values().parallelStream().filter(l -> l.size() > 1).forEach(l -> l.forEach(v -> log.log(Level.WARNING, "same timeStamp: ", v)));
					}
				}
				this.availableTrussesCount = this.histoSet.size();
				{
					if (!isHistoFilePathsValid || EnumSet.of(RebuildStep.A_HISTOSET, RebuildStep.B_HISTOVAULTS, RebuildStep.C_TRAILRECORDSET).contains(rebuildStep)) {
						isRebuilt = true;
						long nanoTimeTrailRecordSet = -System.nanoTime();
						this.trailRecordSet = TrailRecordSet.createRecordSet(this.application.getActiveDevice(), this.application.getActiveChannelNumber());
						// this.trailRecordSet.checkAllDisplayable();
						this.trailRecordSet.applyTemplate(true); // needs reasonable data
						nanoTimeTrailRecordSet += System.nanoTime();
						if (this.fileSizeSum_B > 0 && log.isLoggable(Level.TIME)) log.log(Level.TIME,
								String.format("%,5d trailTimeSteps to TrailRecordSet  time=%,6d [ms]  ::  per second:%5d", this.histoSet.size(), TimeUnit.NANOSECONDS.toMillis(nanoTimeTrailRecordSet), //$NON-NLS-1$
										this.histoSet.size() > 0 ? this.histoSet.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeTrailRecordSet) : 0));
						log.log(Level.TIME,
								String.format("%,5d trailTimeSteps total              time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,6d MB/s", this.histoSet.size(), //$NON-NLS-1$
										new Date().getTime() - startTimeFileValid, (int) (this.histoSet.size() * 1000. / (new Date().getTime() - startTimeFileValid) + .5),
										(int) (this.fileSizeSum_B / 1000. / (new Date().getTime() - startTimeFileValid) + .5)));
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
		}
		finally {
			progress.set(100);
		}
	}

	/**
	 * Determine the vaults which are required for the data access.
	 * Select osd file candidates for the active device and the active channel; select as well for objectKey and start timestamp.
	 * Select bin file candidates for object key based on the parent directory name and last modified.
	 * @param deviceConfigurations
	 * @return trussJobs with the actual path (not the link file path) and a map of vault skeletons (the key vaultFileName prevents double entries)
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	*/
	private Map<Path, Map<String, VaultCollector>> getTrusses4Screening(TreeMap<String, DeviceConfiguration> deviceConfigurations) throws IOException, NotSupportedFileFormatException {
		final Map<Path, Map<String, VaultCollector>> trusses4Paths = new LinkedHashMap<>();
		final Map<Long, Set<String>> trusses4Start = new HashMap<Long, Set<String>>();
		final List<Integer> channelMixConfigNumbers = this.application.getActiveDevice().getDeviceConfiguration().getChannelMixConfigNumbers();
		final long minStartTimeStamp_ms = LocalDate.now().minusMonths(this.settings.getRetrospectMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
		final String supportedImportExtention = this.application.getActiveDevice() instanceof IHistoDevice ? ((IHistoDevice) this.application.getActiveDevice()).getSupportedImportExtention()
				: GDE.STRING_EMPTY;

		int invalidRecordSetsCount = 0;
		for (VaultCollector truss : this.histoSet.getUnsuppressedTrusses().values()) {
			File actualFile = truss.getVault().getLogFileAsPath().toFile();
			if (actualFile.getName().endsWith(GDE.FILE_ENDING_OSD)) {
				boolean isValidObject = truss.isConsistentDevice() && truss.isConsistentChannel(channelMixConfigNumbers) && truss.isConsistentStartTimeStamp(minStartTimeStamp_ms)
						&& truss.isConsistentObjectKey();
				if (isValidObject) {
					if (!trusses4Paths.containsKey(actualFile.toPath())) trusses4Paths.put(actualFile.toPath(), new HashMap<String, VaultCollector>());
					if (!trusses4Start.containsKey(truss.getVault().getLogStartTimestamp_ms())) trusses4Start.put(truss.getVault().getLogStartTimestamp_ms(), new HashSet<String>());

					if (trusses4Start.get(truss.getVault().getLogStartTimestamp_ms()).add(truss.getVault().getVaultFileName().toString()))
						trusses4Paths.get(actualFile.toPath()).put(truss.getVault().getVaultFileName().toString(), truss);
					else
						log.log(Level.WARNING, "duplicate vault was discarded: ", truss);
				}
				else {
					invalidRecordSetsCount++;
					log.log(Level.FINE, "skip   ", truss);
				}
			}
			else if (!supportedImportExtention.isEmpty() && actualFile.getName().endsWith(supportedImportExtention)) {
				boolean isValidObject = truss.isConsistentStartTimeStamp(minStartTimeStamp_ms) && truss.isConsistentObjectKey();
				if (isValidObject) {
					if (!trusses4Paths.containsKey(actualFile.toPath())) trusses4Paths.put(actualFile.toPath(), new HashMap<String, VaultCollector>());
					if (!trusses4Start.containsKey(truss.getVault().getLogStartTimestamp_ms())) trusses4Start.put(truss.getVault().getLogStartTimestamp_ms(), new HashSet<String>());

					if (trusses4Start.get(truss.getVault().getLogStartTimestamp_ms()).add(truss.getVault().getVaultFileName().toString()))
						trusses4Paths.get(actualFile.toPath()).put(truss.getVault().getVaultFileName().toString(), truss);
					else
						log.log(Level.WARNING, "duplicate vault was discarded: ", truss);
				}
				else {
					invalidRecordSetsCount++;
					log.log(Level.FINE, "skip   ", truss);
				}
			}
		}
		this.matchingTrussesCount = this.histoSet.getUnsuppressedTrusses().size() - invalidRecordSetsCount;
		log.log(Level.INFO,
				String.format("%04d trusses taken --- %04d checked trusses --- %04d invalid trusses", trusses4Paths.size(), this.histoSet.getUnsuppressedTrusses().size(), invalidRecordSetsCount)); //$NON-NLS-1$
		return trusses4Paths;

	}

	public int getMatchingTrussesCount() {
		return this.matchingTrussesCount;
	}

	public int getAvailableTrussesCount() {
		return this.availableTrussesCount;
	}

	public int getElapsedTime_ms() {
		return this.elapsedTime_ms;
	}

	public TrailRecordSet getTrailRecordSet() {
		return this.trailRecordSet;
	}

}
