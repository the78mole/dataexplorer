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

import static gde.histo.datasources.HistoSetCollector.LoadProgress.CACHED;
import static gde.histo.datasources.HistoSetCollector.LoadProgress.DONE;
import static gde.histo.datasources.HistoSetCollector.LoadProgress.INITIALIZED;
import static gde.histo.datasources.HistoSetCollector.LoadProgress.MATCHED;
import static gde.histo.datasources.HistoSetCollector.LoadProgress.PATHS_VERIFIED;
import static gde.histo.datasources.HistoSetCollector.LoadProgress.RECORDED;
import static gde.histo.datasources.HistoSetCollector.LoadProgress.RESTORED;
import static gde.histo.datasources.HistoSetCollector.LoadProgress.SCANNED;
import static gde.histo.datasources.HistoSetCollector.LoadProgress.STARTED;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.WARNING;

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
import gde.histo.recordings.TrailRecordSet;
import gde.log.Logger;
import gde.ui.DataExplorer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;


/**
 * Convert the source files into a trail recordSet required for the history tabs.
 * @author Thomas Eickert (USER)
 */
public final class HistoSetCollector {
	private static final String							$CLASS_NAME					= HistoSetCollector.class.getName();
	private static final Logger							log									= Logger.getLogger($CLASS_NAME);

	/**
	 * Estimated processing time ratio between reading from files and reading from cache
	 */
	private static final int								CACHE_BENEFIT				= 10;

	private static final DuplicateHandling	DUPLICATE_HANDLING	= DuplicateHandling.DISCARD;

	private enum DuplicateHandling {
		KEEP {
			@Override
			Map<Path, List<VaultCollector>> getTrussJobs(List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException {
				final Map<Path, List<VaultCollector>> resultMap = new HashMap<>();
				for (VaultCollector truss : trusses) {
					addTruss(resultMap, truss);
				}
				return resultMap;
			}
		},

		DISCARD {
			@Override
			Map<Path, List<VaultCollector>> getTrussJobs(List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException {
				final Map<Path, List<VaultCollector>> resultMap = new HashMap<>();
				final Set<ExtendedVault> trusses4StartTimes = new HashSet<>();

				for (VaultCollector truss : trusses) {
					// add the truss to the set to find out if it is a duplicate according to the equals method criteria
					if (!trusses4StartTimes.add(truss.getVault())) {
						log.log(WARNING, "duplicate vault was discarded: ", truss);
						continue;
					}
					addTruss(resultMap, truss);
				}
				return resultMap;
			}
		};

		/**
		 * Determine the vaults which are required for the data access.
		 * @return trussJobs with the actual path (not the link file path) and a map of vault skeletons (trusses)
		 */
		abstract Map<Path, List<VaultCollector>> getTrussJobs(List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException;

		private static void addTruss(final Map<Path, List<VaultCollector>> resultMap, VaultCollector truss) {
			Path path = truss.getVault().getLogFileAsPath();
			List<VaultCollector> list = resultMap.get(path);
			if (list == null) resultMap.put(path, list = new ArrayList<>());

			list.add(truss);
		}
	}

	private final DirectoryScanner										directoryScanner;

	/**
	 * Sorted by recordSet startTimeStamp in reverse order; each timestamp may hold multiple vaults.
	 */
	private final TreeMap<Long, List<ExtendedVault>>	histoVaults					= new TreeMap<>(Collections.reverseOrder());

	/**
	 * Excluded vaults via ignore lists
	 */
	private List<ExtendedVault>												suppressedVaults		= new ArrayList<>();
	/**
	 * Number of files which have been read for getting vaults
	 */
	private int																				readFilesCount			= 0;
	/**
	 * Number of files which have log data for the object, device, analysis timespan etc.
	 */
	private int																				matchingFilesCount	= 0;
	/**
	 * Number of trusses identified in the read files
	 */
	private int																				readTrussesCount;
	/**
	 * Size of all the histo files which have been read to build the histo recordsets
	 */
	private long																			recordSetBytesSum		= 0;
	/**
	 * Total time for rebuilding the HistoSet in micorseconds
	 */
	private int																				elapsedTime_us			= 0;

	/**
	 * Histo vault data transformed in a recordset format
	 */
	private TrailRecordSet														trailRecordSet			= null;

	public enum LoadProgress {
		STARTED(2), INITIALIZED(5), PATHS_VERIFIED(7), SCANNED(11), MATCHED(22), RESTORED(50), LOADED(80), CACHED(97), RECORDED(99), DONE(100);
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

		private double							progressCycle	= 0.;
		private int									progressStart	= 0;
		private int									stepSize			= 0;
		private int									counter				= 0;

		public ProgressManager() {
		}

		public ProgressManager(int newEndPercentage, int newTotalCount, int newStepSize) {
			reInit(newEndPercentage, newTotalCount, newStepSize);
		}

		/**
		 * To be called prior to looping with doCount.
		 * @param loadProgress is the step to be accomplished
		 * @param newTotalCount
		 * @param newStepSize defines the number of counts which are required before the bar is updated again (performance)
		 */
		public void reInit(LoadProgress loadProgress, int newTotalCount, int newStepSize) {
			reInit(loadProgress.endPercentage, newTotalCount, newStepSize);
		}

		/**
		 * To be called prior to looping with doCount.
		 * @param newEndPercentage is reached after the total number of counts
		 * @param newTotalCount
		 * @param newStepSize defines the number of counts which are required before the bar is updated again (performance)
		 */
		public void reInit(int newEndPercentage, int newTotalCount, int newStepSize) {
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
			if (this.progressCycle <= 0)
				throw new UnsupportedOperationException();
			else if (newCounter % this.stepSize == 0) {
				this.counter = newCounter;
				this.presenter.setProgress((int) (newCounter * this.stepSize * this.progressCycle + this.progressStart), this.sThreadId);
			} else
				; // progress is set if stepSize is reached
		}

		/**
		 * Set the progress bar percentage.
		 * @param percentage value (reset for 0 or bigger than 99)
		 */
		public void set(int percentage) {
			this.presenter.setProgress(percentage, this.sThreadId);
		}

		/**
		 * Set the progress bar percentage.
		 * @param loadProgress
		 */
		public void set(LoadProgress loadProgress) {
			this.presenter.setProgress(loadProgress.endPercentage, this.sThreadId);
		}
	}

	public HistoSetCollector() {
		this.directoryScanner = new DirectoryScanner();
	}

	@Override
	public String toString() {
		return String.format("totalTrussesCount=%,d  availableVaultsCount=%,d recordSetBytesSum=%,d elapsedTime_ms=%,d", //$NON-NLS-1$
				this.getTrussesCount(), this.getTimeStepSize(), this.recordSetBytesSum, this.getElapsedTime_ms());
	}

	/**
	 * Re- initializes the class.
	 */
	public synchronized void initialize() {
		// deep clear in order to reduce memory consumption prior to garbage collection
		for (List<ExtendedVault> timestampHistoVaults : histoVaults.values()) {
			timestampHistoVaults.clear();
		}
		this.histoVaults.clear();
		this.suppressedVaults.clear();

		this.readFilesCount = 0;
		this.matchingFilesCount = 0;
		this.readTrussesCount = 0;
		this.recordSetBytesSum = 0;
		this.elapsedTime_us = 0;

		this.trailRecordSet = null;
	}

	public void rebuild4Test(Path filePath, TreeMap<String, DeviceConfiguration> devices)//
			throws IOException, NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {

		initialize();

		List<SourceDataSet> sourceFiles = this.directoryScanner.readSourceFiles4Test(filePath);
		this.readFilesCount = sourceFiles.size();
		List<VaultCollector> trusses = getTrusses(sourceFiles, DataExplorer.getInstance().getDeviceSelectionDialog().getDevices());
		removeSuppressed(trusses);
		log.info(() -> String.format("%04d files selected", trusses.size())); //$NON-NLS-1$

		{
			// step: build the workload map consisting of the cache key and the file path
			Map<Path, List<VaultCollector>> trussJobs = DUPLICATE_HANDLING.getTrussJobs(trusses);
			log.info(() -> String.format("trussJobs to load total     = %d", trussJobs.size())); //$NON-NLS-1$

			// step: put cached vaults into the histoSet map and reduce workload map
			int trussJobsSize = trussJobs.size();
			for (ExtendedVault histoVault : VaultReaderWriter.loadVaultsFromCache(trussJobs, Optional.empty())) {
				if (!histoVault.isTruss()) {
					putVault(histoVault);
					this.recordSetBytesSum += histoVault.getScorePoint(ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal());
				}
			}
			log.info(() -> String.format("trussJobs loaded from cache = %d", trussJobsSize - trussJobs.size())); //$NON-NLS-1$

			// step: transform log files for the truss jobs into vaults and put them into the histoSet map
			ArrayList<ExtendedVault> newVaults = new ArrayList<>();
			for (Map.Entry<Path, List<VaultCollector>> pathEntry : trussJobs.entrySet()) {
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
			log.info(() -> String.format("trussJobs loaded from file  = %d", newVaults.size())); //$NON-NLS-1$

			// step: save vaults in the file system
			if (newVaults.size() > 0) {
				VaultReaderWriter.storeVaultsInCache(newVaults);
			}
		}
		{
			this.trailRecordSet = TrailRecordSet.createRecordSet(this.histoVaults);
			this.trailRecordSet.initializeFromVaults();
		}
	}

	/**
	 * Determine the valid histo files and read the vaults from the log files or the cache.
	 * Use the vaults to populate the trail recordset.
	 * @param rebuildStep is the step type where the processing starts. The previous job types are omitted.
	 *          May be augmented if the device, channel or object was modified.
	 * @param isWithUi true allows actions on the user interface (progress bar, message boxes)
	 * @return true if the trail recordSet was rebuilt
	 */
	public synchronized boolean rebuild4Screening(RebuildStep rebuildStep, boolean isWithUi) throws FileNotFoundException, IOException,
			NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {
		RebuildStep realRebuildStep = rebuildStep; // the rebuild step might be augmented during the screening procedure

		Optional<ProgressManager> progress = isWithUi ? Optional.of(new ProgressManager()) : Optional.empty();
		try {
			long startNanoTime = System.nanoTime();
			log.log(FINER, GDE.STRING_GREATER, rebuildStep);
			progress.ifPresent((p) -> p.set(STARTED));

			if (realRebuildStep.isEqualOrBiggerThan(RebuildStep.A_HISTOSET)) {
				this.directoryScanner.initialize();
				log.fine(() -> String.format("directoryScanner      initialize")); //$NON-NLS-1$
			}
			progress.ifPresent((p) -> p.set(INITIALIZED));

			if (realRebuildStep.isEqualOrBiggerThan(RebuildStep.F_FILE_CHECK)) {
				boolean isHistoFilePathsValid = this.directoryScanner.validateHistoFilePaths(rebuildStep);
				if (!isHistoFilePathsValid) {
					realRebuildStep = RebuildStep.B_HISTOVAULTS;
				} else { // histo record sets are ready to use
					log.time(() -> String.format("  %3d file paths verified           time=%,6d [ms]", //$NON-NLS-1$
							this.directoryScanner.getValidatedDirectories().size(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanoTime + 500000)));
				}
			}
			progress.ifPresent((p) -> p.set(PATHS_VERIFIED));

			if (realRebuildStep.isEqualOrBiggerThan(RebuildStep.B_HISTOVAULTS)) {
				initialize();
				List<VaultCollector> trusses;
				{
					List<SourceDataSet> sourceFiles = this.directoryScanner.readSourceFiles();
					trusses = getTrusses(sourceFiles, DataExplorer.getInstance().getDeviceSelectionDialog().getDevices());
					this.suppressedVaults = removeSuppressed(trusses);
					this.readFilesCount = sourceFiles.size();
					this.readTrussesCount = trusses.size();

					long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanoTime);
					if (millis > 0) log.time(() -> String.format("%,5d trusses    select folders     time=%,6d [ms] :: per second:%5d", //$NON-NLS-1$
							trusses.size(), millis, trusses.size() * 1000 / millis));
				}
				progress.ifPresent((p) -> p.set(SCANNED));

				if (!trusses.isEmpty()) {
					Map<Path, List<VaultCollector>> trussJobs;
					{ // step: read from paths and identify duplicates
						trussJobs = defineTrussJobs(trusses);
						progress.ifPresent((p) -> p.set(MATCHED));
					}
					{// step: put cached vaults into the histoSet map and reduce workload map
						int jobSize = trussJobs.values().parallelStream().mapToInt(c -> c.size()).sum();
						progress.ifPresent((p) -> p.reInit(RESTORED, jobSize, CACHE_BENEFIT));
						loadVaultsFromCache(trussJobs, progress);
						int totalEstimatedEffort = this.histoVaults.size() + CACHE_BENEFIT * (jobSize - this.histoVaults.size());
						double timeQuotaDone = this.histoVaults.size() / totalEstimatedEffort;
						int progressPercentageDone = (int) ((CACHED.endPercentage - MATCHED.endPercentage) * timeQuotaDone);
						progress.ifPresent((
								p) -> p.set(Math.max(DataExplorer.application.getProgressPercentage(), MATCHED.endPercentage + progressPercentageDone)));
					}
					final long recordSetBytesCachedSum = this.recordSetBytesSum;
					ArrayList<ExtendedVault> newVaults;
					{// step: transform log files from workload map into vaults and put them into the histoSet map
						long nanoTime = System.nanoTime();
						newVaults = loadVaultsFromFiles(trussJobs, progress);
						if (newVaults.size() > 0) {
							long micros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - nanoTime);
							log.time(() -> String.format("%,5d recordsets create from files  time=%,6d [ms] :: per second:%5d :: Rate=%,6d MiB/s", //$NON-NLS-1$
									newVaults.size(), micros / 1000, newVaults.size() * 1000000 / micros, (int) ((this.recordSetBytesSum - recordSetBytesCachedSum) / 1.024 / 1.024 / micros)));
						}
					}
					{// step: save vaults in the file system
						long nanoTime = System.nanoTime(), cacheSize_B = 0;
						if (newVaults.size() > 0) {
							cacheSize_B = VaultReaderWriter.storeVaultsInCache(newVaults);
						}
						if (cacheSize_B > 0) {
							long micros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - nanoTime);
							log.time(() -> String.format("%,5d recordsets store in cache     time=%,6d [ms] :: per second:%5d :: Rate=%,6d MiB/s", //$NON-NLS-1$
									newVaults.size(), micros / 1000, newVaults.size() * 1000000 / micros, (int) ((this.recordSetBytesSum - recordSetBytesCachedSum) / 1.024 / 1.024 / micros)));
						}
					}
				}
			}
			progress.ifPresent((p) -> p.set(CACHED));

			if (!realRebuildStep.isEqualOrBiggerThan(RebuildStep.B_HISTOVAULTS)) {
				// the user may have added entries to the exclusion list
				this.suppressedVaults.addAll(removeSuppressedHistoVaults());
			}

			if (realRebuildStep.isEqualOrBiggerThan(RebuildStep.C_TRAILRECORDSET)) {
				long nanoTime = System.nanoTime();
				this.trailRecordSet = TrailRecordSet.createRecordSet(this.histoVaults);
				this.trailRecordSet.initializeFromVaults();
				this.trailRecordSet.applyTemplate(true); // needs reasonable data
				if (this.recordSetBytesSum > 0) {
					long micros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - nanoTime);
					log.fine(() -> String.format("%,5d timeSteps  to TrailRecordSet  time=%,6d [ms] :: per second:%5d", //$NON-NLS-1$
							this.histoVaults.size(), micros / 1000, this.histoVaults.size() > 0 ? this.histoVaults.size() * 1000000 / micros : 0));
				}
			} else if (realRebuildStep.isEqualOrBiggerThan(RebuildStep.D_TRAIL_DATA)) { // saves some time compared to the logic above
				this.trailRecordSet.refillRecordSet();
			}
			progress.ifPresent((p) -> p.set(RECORDED));

			this.elapsedTime_us = (int) ((System.nanoTime() - startNanoTime + 500000) / 1000);
			log.time(() -> String.format("%,5d timeSteps  total              time=%,6d [ms] :: per second:%5d :: Rate=%,6d MiB/s", //$NON-NLS-1$
					this.histoVaults.size(), elapsedTime_us / 1000, this.histoVaults.size() * 1000000 / this.elapsedTime_us, (int) (this.recordSetBytesSum / 1.024 / 1.024 / this.elapsedTime_us)));
			return realRebuildStep.isEqualOrBiggerThan(RebuildStep.D_TRAIL_DATA);
		} finally {
			progress.ifPresent((p) -> p.set(DONE));
		}
	}

	/**
	 * @param trussJobs is the list of trusses which must be read
	 * @return the list containing the vaults read from the original logs
	 */
	private ArrayList<ExtendedVault> loadVaultsFromFiles(Map<Path, List<VaultCollector>> trussJobs, Optional<ProgressManager> progress) {
		ArrayList<ExtendedVault> newVaults;
		newVaults = new ArrayList<>();
		int remainingJobSize = trussJobs.values().parallelStream().mapToInt(c -> c.size()).sum();
		progress.ifPresent((p) -> p.reInit(CACHED, remainingJobSize, 1));
		for (Map.Entry<Path, List<VaultCollector>> trussJobsEntry : trussJobs.entrySet()) {
			for (ExtendedVault histoVault : VaultReaderWriter.loadVaultsFromFile(trussJobsEntry.getKey(), trussJobsEntry.getValue())) {
				if (!histoVault.isTruss()) {
					putVault(histoVault);
					this.recordSetBytesSum += histoVault.getScorePoint(ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal());
				} else {
					log.info(() -> String.format("vault has no log data %,7d kiB %s", histoVault.getLogFileLength() / 1024, histoVault.getLogFilePath()));
				}
				newVaults.add(histoVault);
			}
			progress.ifPresent((p) -> p.countInLoop(trussJobsEntry.getValue().size()));
		}
		return newVaults;
	}

	/**
	 * @param trussJobs is the job list which is worked on and reduced for each vault found in the cache
	 */
	private void loadVaultsFromCache(Map<Path, List<VaultCollector>> trussJobs, Optional<ProgressManager> progress) throws IOException {
		long nanoTime = System.nanoTime();
		int tmpHistoSetsSize = this.histoVaults.size();
		for (ExtendedVault histoVault : VaultReaderWriter.loadVaultsFromCache(trussJobs, progress)) {
			if (!histoVault.isTruss()) {
				putVault(histoVault);
				this.recordSetBytesSum += histoVault.getScorePoint(ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal());
			} else {
				log.info(() -> String.format("vault has no log data %,7d kiB %s", histoVault.getLogFileLength() / 1024, histoVault.getLogFilePath()));
			}
		}
		int loadCount = this.histoVaults.size() - tmpHistoSetsSize;
		if (loadCount > 0) {
			long micros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - nanoTime);
			log.time(() -> String.format("%,5d vaults     load from cache    time=%,6d [ms] :: per second:%5d :: Rate=%,6d MiB/s", //$NON-NLS-1$
					loadCount, micros / 1000, loadCount * 1000000 / micros, (int) (this.recordSetBytesSum / 1.024 / 1.024 / micros)));
		}
	}

	/**
	 * @return the jobs determined from the paths after duplicates elimination
	 */
	private Map<Path, List<VaultCollector>> defineTrussJobs(List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException {
		Map<Path, List<VaultCollector>> trussJobs;
		long nanoTime = System.nanoTime();
		// step: build the workload map consisting of the cache key and the file path
		trussJobs = DUPLICATE_HANDLING.getTrussJobs(trusses);
		int jobSize = trussJobs.values().parallelStream().mapToInt(c -> c.size()).sum();
		if (jobSize > 0) {
			long micros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - nanoTime);
			log.finer(() -> String.format("%,5d trusses    job check          time=%,6d [ms] :: per second:%5d", //$NON-NLS-1$
					jobSize, micros / 1000, trusses.size() * 1000000 / micros));
		}
		return trussJobs;
	}

	/**
	 * Check the histo vaults list for suppressed vaults in order to cope with additional suppressions by the user.
	 * @return the suppressed vaults which have been detected in the vaults list
	 */
	private List<ExtendedVault> removeSuppressedHistoVaults() {
		List<ExtendedVault> removed = new ArrayList<>();
		this.histoVaults.values().stream().flatMap(Collection::stream).forEach(v -> {
			if (ExclusionData.isExcluded(v.getLogFileAsPath(), v.getLogRecordsetBaseName())) {
				log.info(() -> String.format("discarded as per exclusion list   %s %s   %s", //$NON-NLS-1$
						v.getLogFilePath(), v.getLogRecordsetBaseName(), v.getStartTimeStampFormatted()));
				removed.add(v);
			}
		});
		for (ExtendedVault vault : removed) {
			List<ExtendedVault> vaultList = this.histoVaults.get(vault.getLogStartTimestamp_ms());
			vaultList.remove(vault);
			if (vaultList.isEmpty()) this.histoVaults.remove(vault.getLogStartTimestamp_ms());
		}
		return removed;
	}

	/**
	 * Use ignore lists (for recordSets only) to determine the vaults which are required for the data access.
	 * @return the vaults which have been removed due to user exclusions
	 */
	private List<ExtendedVault> removeSuppressed(List<VaultCollector> vaults) {
		List<ExtendedVault> removed = new ArrayList<>();
		int totalSize = vaults.size();
		for (Iterator<VaultCollector> iterator = vaults.iterator(); iterator.hasNext();) {
			ExtendedVault vault = iterator.next().getVault();
			if (ExclusionData.isExcluded(vault.getLogFileAsPath(), vault.getLogRecordsetBaseName())) {
				log.info(() -> String.format("discarded as per exclusion list   %s %s   %s", //$NON-NLS-1$
						vault.getLogFilePath(), vault.getLogRecordsetBaseName(), vault.getStartTimeStampFormatted()));
				iterator.remove();
				removed.add(vault);
			}
		}
		log.finer(() -> String.format("%04d total trusses --- %04d excluded trusses", totalSize, removed.size()));
		return removed;
	}

	/**
	 * Determine trusses in the osd files and in the native files (if supported by the device).
	 * @param sourceFiles for osd reader (possibly link files) or for import
	 */
	private List<VaultCollector> getTrusses(List<SourceDataSet> sourceFiles, TreeMap<String, DeviceConfiguration> deviceConfigurations)
			throws IOException, NotSupportedFileFormatException {
		List<VaultCollector> result = new ArrayList<>();
		for (SourceDataSet sourceFile : sourceFiles) {
			List<VaultCollector> trusses = sourceFile.getTrusses(deviceConfigurations);
			if (!trusses.isEmpty()) {
				for (VaultCollector truss : trusses) {
					result.add(truss);
				}
				this.matchingFilesCount += 1;
			} else {
				log.info(() -> String.format("file w/o matching data%,7d kiB %s", //$NON-NLS-1$
						sourceFile.getFile().length() / 1024, sourceFile.getPath()));
			}
		}
		return result;
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
		log.finer(() -> String.format("added   startTimeStamp=%s  %s  logRecordSetOrdinal=%d  logChannelNumber=%d  %s", //$NON-NLS-1$
				histoVault.getStartTimeStampFormatted(), histoVault.getVaultFileName(), histoVault.getLogRecordSetOrdinal(), histoVault.getLogChannelNumber(), histoVault.getLogFilePath()));
	}

	/**
	 * @return the total number of trusses identified in the files
	 */
	public int getTrussesCount() {
		return this.readTrussesCount;
	}

	/**
	 * @return the number of timestamp entries
	 */
	public int getTimeStepSize() {
		return this.getTrailRecordSet() != null ? this.getTrailRecordSet().getTimeStepSize() : 0;
	}

	public int getElapsedTime_ms() {
		return this.elapsedTime_us / 1000;
	}

	public TrailRecordSet getTrailRecordSet() {
		return this.trailRecordSet;
	}

	public long getRecordSetBytesSum() {
		return this.recordSetBytesSum;
	}

	public List<ExtendedVault> getSuppressedTrusses() {
		return this.suppressedVaults;
	}

	public List<Path> getExcludedFiles() {
		return this.directoryScanner.getExcludedFiles();
	}

	public Map<DirectoryType, Path> getValidatedDirectories() {
		return this.directoryScanner.getValidatedDirectories();
	}

	public int getDirectoryFilesCount() {
		return this.readFilesCount + this.directoryScanner.getNonWorkableCount() + this.directoryScanner.getExcludedFiles().size();
	}

	public int getReadFilesCount() {
		return this.readFilesCount;
	}

	public int getMatchingFilesCount() {
		return this.matchingFilesCount;
	}

}
