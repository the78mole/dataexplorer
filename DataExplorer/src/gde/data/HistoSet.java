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
package gde.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import java.util.zip.ZipFile;

import org.eclipse.swt.SWT;

import gde.GDE;
import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.device.DeviceTypes;
import gde.device.IDevice;
import gde.device.IHistoDevice;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.NotSupportedFileFormatException;
import gde.histocache.HistoVault;
import gde.io.HistoOsdReaderWriter;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.OperatingSystemHelper;
import gde.utils.StringHelper;

/**
 * supports the selection of histo vaults and provides a trail recordset based on the vaults. 
 * sorted by recordSet startTimeStamp in reverse order; each timestamp may hold multiple vaults.
 * @author Thomas Eickert
 */
public class HistoSet extends TreeMap<Long, List<HistoVault>> {
	private final static String		$CLASS_NAME								= HistoSet.class.getName();
	private static final long			serialVersionUID					= 1111377035274863787L;
	private final static Logger		log												= Logger.getLogger($CLASS_NAME);

	private final DataExplorer		application								= DataExplorer.getInstance();
	private final Settings				settings									= Settings.getInstance();

	private static HistoSet				histoSet									= null;

	private IDevice								validatedDevice						= null;
	private Channel								validatedChannel					= null;
	private Path									validatedDataDir					= null;
	private Path									validatedImportDir				= null;
	private String								validatedImportExtention	= GDE.STRING_EMPTY;

	/**
	 * histo files coming from the last directory validation.
	 * key is lastModified [ms] of the file, the list holds link file paths or file paths for all types of log files.
	 */
	private Map<Long, Set<Path>>	histoFilePaths						= new TreeMap<Long, Set<Path>>(Collections.reverseOrder());	// todo HashMap is sufficient (no sort required)
	private long									fileSizeSum_B							= 0;																												// size of all the histo files which have been read to build the histo recordsets 
	private TrailRecordSet				trailRecordSet						= null;																											// histo data transformed in a recordset format

	/**
	 * defines the first step during rebuilding the histoset data.
	 * a minimum of steps may be selected for performance reasons.
	 */
	public enum RebuildStep {
		/**
		* true starts from scratch on
		*/
		A_HISTOSET(5),
		/**
		* true starts building the histo vaults
		*/
		B_HISTOVAULTS(4),
		/**
		* true starts building the trail recordset from the histo vaults 
		*/
		C_TRAILRECORDSET(3),
		/**
		* true starts refreshing the trail data from the histo vaults
		*/
		D_TRAIL_DATA(2),
		/**
		* starts updating the graphics and table
		*/
		E_USER_INTERFACE(1),
		/**
		* starts with a file check only which decides which update activity is required
		*/
		F_FILE_CHECK(0);

		/**
		 * zero is the lowest scopeOfWork.
		 */
		public final int					scopeOfWork;
		/**
		 * use this to avoid repeatedly cloning actions instead of values()
		 */
		public static RebuildStep	values[]	= values();

		private RebuildStep(int scopeOfWork) {
			this.scopeOfWork = scopeOfWork;
		}

		/**
		 * @param scopeOfWork zero is the lowest scopeOfWork
		 * @return
		 */
		public static RebuildStep area(int scopeOfWork) {
			for (RebuildStep rebuildStep : values()) {
				if (rebuildStep.scopeOfWork == scopeOfWork) {
					return rebuildStep;
				}
			}
			throw new IllegalArgumentException(String.valueOf(scopeOfWork));
		}

	};

	public static HistoSet getInstance() {
		if (HistoSet.histoSet == null) HistoSet.histoSet = new HistoSet();
		return HistoSet.histoSet;
	}

	private HistoSet() {
		super(Collections.reverseOrder());
	}

	//	/**
	//	 * instantiates the singleton and initializes it.
	//	 */
	//	public void resetFully() {
	//		HistoSet.histoSet = new HistoSet();
	//		initialize();
	//	}

	/**
	 * re- initializes the singleton. 
	 */
	public void initialize() {
		this.clear();

		this.validatedDevice = null;
		this.validatedChannel = null;
		this.validatedDataDir = null;
		this.validatedImportDir = null;
		this.validatedImportExtention = GDE.STRING_EMPTY;

		// this.histoFilePaths.clear(); is accomplished by files validation
		this.fileSizeSum_B = 0;
		this.trailRecordSet = null;
		if (log.isLoggable(Level.OFF))
			log.log(Level.OFF, String.format("device=%s  channel=%d  objectKey=%s", this.application.getActiveDevice() == null ? null : this.application.getActiveDevice().getName(), //$NON-NLS-1$
					this.application.getActiveChannelNumber(), this.application.getObjectKey()));
	}

	/* 
	 * clears trails for refill but keeps the trail recordset.
	 * @see java.util.TreeMap#clear()
	 */
	@Override
	public void clear() {
		// deep clear in order to reduce memory consumption prior to garbage collection
		for (List<HistoVault> timestampHistoVaults : this.values()) {
			timestampHistoVaults.clear();
		}
		super.clear();

		// this.histoFilePaths.clear(); is accomplished by files validation
		this.fileSizeSum_B = 0;
		// this.trailRecordSet = null;
		if (log.isLoggable(Level.OFF))
			log.log(Level.OFF, String.format("device=%s  channel=%d  objectKey=%s", this.application.getActiveDevice() == null ? null : this.application.getActiveDevice().getName(), //$NON-NLS-1$
					this.application.getActiveChannelNumber(), this.application.getObjectKey()));
	}

	public void rebuild4Test(TreeMap<String, DeviceConfiguration> deviceConfigurations) throws IOException, NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {
		// this.clear();
		{
			if (this.getHistoFilePaths().size() > 0) {
				// step: build the workload map consisting of the cache key and the file path
				Map<Path, Map<String, HistoVault>> trussJobs = getTrusses4Screening(false, deviceConfigurations);
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("trussJobs to load total     = %d", trussJobs.size())); //$NON-NLS-1$

				// step: put cached vaults into the histoSet map and reduce workload map
				int trussJobsSize = trussJobs.size();
				this.fileSizeSum_B = loadVaultsFromCache(trussJobs);
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("trussJobs loaded from cache = %d", trussJobsSize - trussJobs.size())); //$NON-NLS-1$

				// step: transform log files for the truss jobs into vaults and put them into the histoSet map
				ArrayList<HistoVault> newVaults = new ArrayList<HistoVault>();
				for (Map.Entry<Path, Map<String, HistoVault>> pathEntry : trussJobs.entrySet()) {
					try {
						newVaults.addAll(loadVaultsFromFile(pathEntry.getKey(), pathEntry.getValue()));
					}
					catch (Exception e) {
						throw new UnsupportedOperationException(pathEntry.getKey().toString(), e);
					}
				}
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("trussJobs loaded from file  = %d", newVaults.size())); //$NON-NLS-1$

				// step: save vaults in the file system
				if (newVaults.size() > 0) {
					storeVaultsInCache(newVaults);
				}
			}
		}
	}

	/**
	 * determine histo files, build a recordset based job list and read from the log file or the cache for each job.
	 * populate the trail recordset.
	 * disregard rebuild steps if histo file paths have changed which may occur if new files have been added by the user or the device, channel or object was modified. 
	 * @param rebuildStep
	 * @param isWithUi true allows actions on the user interface (progress bar, message boxes)
	 * @return true if the HistoSet was rebuilt
	 * @throws DataTypeException 
	 * @throws DataInconsitsentException 
	 * @throws NotSupportedFileFormatException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public boolean rebuild4Screening(RebuildStep rebuildStep, boolean isWithUi) throws FileNotFoundException, IOException, NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		try {
			long startTime = System.nanoTime() / 1000000;
			boolean isRebuilt = false;
			log.log(Level.FINER, GDE.STRING_GREATER, rebuildStep);
			if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) this.application.setProgress(2, sThreadId);

			long startTimeFileValid = new Date().getTime();
			boolean isHistoFilePathsValid = this.validateHistoFilePaths(rebuildStep);
			{
				if (!isHistoFilePathsValid) {
					this.clear();
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("histoSet             clear")); //$NON-NLS-1$
					if ((new Date().getTime() - startTimeFileValid) > 0)
						log.log(Level.TIME, String.format("%,5d files          select folders     time=%,6d [ms]  ::  per second:%5d", this.histoFilePaths.size(), //$NON-NLS-1$
								new Date().getTime() - startTimeFileValid, this.histoFilePaths.size() * 1000 / (new Date().getTime() - startTimeFileValid)));
				}
				else { // histo record sets are ready to use
					if (log.isLoggable(Level.TIME)) log.log(Level.TIME, String.format("%,5d files    file paths verified     time=%s [ss.SSS]", this.getHistoFilePaths().size(), //$NON-NLS-1$
							StringHelper.getFormatedDuration("ss.SSS", new Date().getTime() - startTimeFileValid))); //$NON-NLS-1$
				}
				if (isWithUi) this.application.setProgress(5, sThreadId);
			}
			{
				if (RebuildStep.A_HISTOSET == rebuildStep) {
					isRebuilt = true;
					this.initialize();
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("histoSet             initialize")); //$NON-NLS-1$
					if (isWithUi) this.application.setProgress(DataExplorer.application.getProgressPercentage() + 2, sThreadId);
				}
			}
			{
				if (!isHistoFilePathsValid || EnumSet.of(RebuildStep.A_HISTOSET, RebuildStep.B_HISTOVAULTS).contains(rebuildStep)) {
					isRebuilt = true;
					this.fileSizeSum_B = 0;
					if (this.getHistoFilePaths().size() > 0) {
						long nanoTimeCheckFilesSum = -System.nanoTime();
						// step: build the workload map consisting of the cache key and the file path
						Map<Path, Map<String, HistoVault>> trussJobs = getTrusses4Screening(isWithUi, DataExplorer.getInstance().getDeviceSelectionDialog().getDevices());
						nanoTimeCheckFilesSum += System.nanoTime();
						if (TimeUnit.NANOSECONDS.toMillis(nanoTimeCheckFilesSum) > 0)
							log.log(Level.TIME, String.format("%,5d files          job check          time=%,6d [ms]  ::  per second:%5d", this.histoFilePaths.size(), //$NON-NLS-1$
									TimeUnit.NANOSECONDS.toMillis(nanoTimeCheckFilesSum), this.histoFilePaths.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeCheckFilesSum)));

						if (isWithUi) this.application.setProgress(DataExplorer.application.getProgressPercentage() + 10, sThreadId);

						// step: put cached vaults into the histoSet map and reduce workload map
						long nanoTimeReadVaultSum = -System.nanoTime();
						int timeStepsSize = -this.size();
						long fileSizeSumCached_B = this.fileSizeSum_B = loadVaultsFromCache(trussJobs);
						timeStepsSize += this.size();
						nanoTimeReadVaultSum += System.nanoTime();
						if (TimeUnit.NANOSECONDS.toMillis(nanoTimeReadVaultSum) > 0) log.log(Level.TIME,
								String.format("%,5d trailTimeSteps load from cache    time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,6d MB/s", timeStepsSize, //$NON-NLS-1$
										TimeUnit.NANOSECONDS.toMillis(nanoTimeReadVaultSum), timeStepsSize * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeReadVaultSum),
										this.fileSizeSum_B / TimeUnit.NANOSECONDS.toMicros(nanoTimeReadVaultSum)));

						// step: calculate progress bar parameters
						double progressCycle = 0.;
						int progressStart = 0;
						if (isWithUi) {
							progressStart = DataExplorer.application.getProgressPercentage()
									+ (int) ((95 - DataExplorer.application.getProgressPercentage()) * this.size() / (double) (this.size() + 10 * trussJobs.size())); // 10 is the estimated processing time ratio between reading from files and reading from cache
							this.application.setProgress(progressStart, sThreadId);
							progressCycle = trussJobs.size() > 0 ? (95 - progressStart) / (double) trussJobs.size() : 1;
						}

						// step: transform log files from workload map into vaults and put them into the histoSet map
						long nanoTimeReadRecordSetSum = -System.nanoTime();
						ArrayList<HistoVault> newVaults = new ArrayList<HistoVault>();
						int i = 0;
						for (Map.Entry<Path, Map<String, HistoVault>> trussJobsEntry : trussJobs.entrySet()) {
							newVaults.addAll(loadVaultsFromFile(trussJobsEntry.getKey(), trussJobsEntry.getValue()));
							this.fileSizeSum_B += new File(trussJobsEntry.getKey().toString()).length();
							if (isWithUi) this.application.setProgress((int) (++i * progressCycle + progressStart), sThreadId);
						}
						nanoTimeReadRecordSetSum += System.nanoTime();
						if (newVaults.size() > 0) log.log(Level.TIME,
								String.format("%,5d recordsets     create from files  time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,6d MB/s", newVaults.size(), //$NON-NLS-1$
										TimeUnit.NANOSECONDS.toMillis(nanoTimeReadRecordSetSum), newVaults.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeReadRecordSetSum),
										(this.fileSizeSum_B - fileSizeSumCached_B) / TimeUnit.NANOSECONDS.toMicros(nanoTimeReadRecordSetSum)));

						// step: save vaults in the file system
						long nanoTimeWriteVaultSum = -System.nanoTime(), cacheSize_B = 0;
						if (newVaults.size() > 0) {
							cacheSize_B = storeVaultsInCache(newVaults);
						}
						if (isWithUi) this.application.setProgress(95, sThreadId);
						nanoTimeWriteVaultSum += System.nanoTime();
						if (TimeUnit.NANOSECONDS.toMillis(nanoTimeWriteVaultSum) > 0 && cacheSize_B > 0) log.log(Level.TIME,
								String.format("%,5d recordsets     store in cache     time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,6d MB/s", newVaults.size(), //$NON-NLS-1$
										TimeUnit.NANOSECONDS.toMillis(nanoTimeWriteVaultSum), newVaults.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeWriteVaultSum),
										(this.fileSizeSum_B - fileSizeSumCached_B) / TimeUnit.NANOSECONDS.toMicros(nanoTimeWriteVaultSum)));
						log.log(Level.TIME, String.format("%,5d trailTimeSteps total              time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,6d MB/s", this.size(), //$NON-NLS-1$
								new Date().getTime() - startTimeFileValid, this.size() * 1000 / (new Date().getTime() - startTimeFileValid), this.fileSizeSum_B / 1000 / (new Date().getTime() - startTimeFileValid)));
						for (List<HistoVault> vaultList : this.values()) {
							if (vaultList.size() > 1) {
								for (HistoVault histoVault : vaultList) {
									log.log(Level.WARNING, "same timeStamp: ", histoVault); //$NON-NLS-1$
								}
							}
						}
					}
				}
				if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) this.application.setProgress(95, sThreadId);
			}

			{
				if (!isHistoFilePathsValid || EnumSet.of(RebuildStep.A_HISTOSET, RebuildStep.B_HISTOVAULTS, RebuildStep.C_TRAILRECORDSET).contains(rebuildStep)) {
					isRebuilt = true;
					long nanoTimeTrailRecordSet = -System.nanoTime();
					this.trailRecordSet = TrailRecordSet.createRecordSet(this.application.getActiveDevice(), this.application.getActiveChannelNumber());
					this.trailRecordSet.defineTrailTypes();
					// this.trailRecordSet.checkAllDisplayable();
					this.trailRecordSet.setPoints();
					this.trailRecordSet.setTags();
					this.trailRecordSet.applyTemplate(true); // needs reasonable data
					nanoTimeTrailRecordSet += System.nanoTime();
					if (this.fileSizeSum_B > 0 && log.isLoggable(Level.TIME))
						log.log(Level.TIME, String.format("%,5d trailTimeSteps build and populate time=%,6d [ms]  ::  per second:%5d", this.size(), TimeUnit.NANOSECONDS.toMillis(nanoTimeTrailRecordSet), //$NON-NLS-1$
								this.size() > 0 ? this.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeTrailRecordSet) : 0));
				}
				if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) this.application.setProgress(97, sThreadId);
			}
			{
				if (EnumSet.of(RebuildStep.D_TRAIL_DATA).contains(rebuildStep)) { // saves some time compared to the logic above
					isRebuilt = true;
					this.trailRecordSet.cleanup();
					this.trailRecordSet.setPoints();
					this.trailRecordSet.setTags();
				}
				if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) this.application.setProgress(98, sThreadId);
			}
			if (log.isLoggable(Level.FINE)) log.log(Level.TIME, "time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$
			return isRebuilt;
		}
		finally {
			this.application.setProgress(100, sThreadId);
		}
	}

	/**
	 * read file and populate vault from the histo recordset.
	 * put the vault into the histoset map.
	 * @param filePath is the actual file path, not the path to the link file 
	 * @param trusses
	 * @throws DataTypeException for the bin file reader only
	 * @throws DataInconsitsentException 
	 * @throws NotSupportedFileFormatException 
	 * @throws IOException 	
	 * @return the vaults extracted from the file based on the input trusses
	 */
	private List<HistoVault> loadVaultsFromFile(Path filePath, Map<String, HistoVault> trusses) throws IOException, NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {
		List<HistoVault> histoVaults = null;
		final String supportedImportExtention = this.application.getActiveDevice() instanceof IHistoDevice ? ((IHistoDevice) this.application.getActiveDevice()).getSupportedImportExtention()
				: GDE.STRING_EMPTY;
		if (!supportedImportExtention.isEmpty() && filePath.toString().endsWith(supportedImportExtention)) {
			histoVaults = ((IHistoDevice) this.application.getActiveDevice()).getRecordSetFromImportFile(filePath, trusses.values());
		}
		else if (filePath.toString().endsWith(GDE.FILE_ENDING_DOT_OSD)) {
			histoVaults = HistoOsdReaderWriter.readHisto(filePath, trusses.values());
		}

		if (histoVaults == null) {
			histoVaults = new ArrayList<HistoVault>();
			log.log(Level.INFO, String.format("invalid file format: %s  channelNumber=%d  %s", //$NON-NLS-1$
					this.application.getActiveDevice().getName(), this.application.getActiveChannelNumber(), filePath));
		}
		else {
			// put vaults into the histoSet
			for (HistoVault histoVault : histoVaults) {
				if (!histoVault.isTruss()) {
					List<HistoVault> timeStampHistoVaults = this.get(histoVault.getLogStartTimestamp_ms());
					if (timeStampHistoVaults == null) {
						this.put(histoVault.getLogStartTimestamp_ms(), timeStampHistoVaults = new ArrayList<HistoVault>());
					}
					timeStampHistoVaults.add(histoVault);
				}
			}
		}
		return histoVaults;
	}

	/**
	 * put cached vaults into the histoSet map and reduce the trussJobs map.
	 * @param trussJobs with the actual path (not the link file path) and a map of vault skeletons (the key vaultFileName prevents double entries)
	 * @return total length (bytes) of the original log files of those vaults which were put into the histoset 
	 * @throws IOException during opening or traversing the zip file
	 */
	private long loadVaultsFromCache(Map<Path, Map<String, HistoVault>> trussJobs) throws IOException {
		long localSizeSum_B = 0;
		Path cacheFilePath = Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_CACHE_ENTRIES_DIR_NAME).resolve(HistoVault.getVaultsDirectory());
		log.log(Level.FINER, "cacheFilePath=", cacheFilePath); //$NON-NLS-1$
		if (this.settings.isZippedCache() && FileUtils.checkFileExist(cacheFilePath.toString())) {
			try (ZipFile zf = new ZipFile(cacheFilePath.toFile())) { // closing the zip file closes all streams
				Iterator<Map.Entry<Path, Map<String, HistoVault>>> trussJobsIterator = trussJobs.entrySet().iterator();
				while (trussJobsIterator.hasNext()) {
					final Map<String, HistoVault> map = trussJobsIterator.next().getValue();
					final Iterator<Map.Entry<String, HistoVault>> trussesIterator = map.entrySet().iterator();
					while (trussesIterator.hasNext()) {
						HistoVault truss = trussesIterator.next().getValue();
						if (zf.getEntry(truss.getVaultName()) != null) {
							HistoVault histoVault = HistoVault.load(new BufferedInputStream(zf.getInputStream(zf.getEntry(truss.getVaultName()))));
							// put the vault into the histoSet map and sum up the file length
							if (!histoVault.isTruss()) {
								List<HistoVault> vaultsList = this.get(histoVault.getLogStartTimestamp_ms());
								if (vaultsList == null) this.put(histoVault.getLogStartTimestamp_ms(), vaultsList = new ArrayList<HistoVault>());
								vaultsList.add(histoVault);
								if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("added   startTimeStamp=%s  %s  logRecordSetOrdinal=%d  logChannelNumber=%d  %s", //$NON-NLS-1$
										histoVault.getStartTimeStampFormatted(), histoVault.getVaultFileName(), histoVault.getLogRecordSetOrdinal(), histoVault.getLogChannelNumber(), histoVault.getLogFilePath()));
								localSizeSum_B += Paths.get(histoVault.getLogFilePath()).toFile().length();
							}
							trussesIterator.remove();
						}
					}
					if (map.size() == 0) trussJobsIterator.remove();
				}
			}
		}
		else if (!this.settings.isZippedCache() && FileUtils.checkDirectoryAndCreate(cacheFilePath.toString())) {
			Iterator<Map.Entry<Path, Map<String, HistoVault>>> trussJobsIterator = trussJobs.entrySet().iterator();
			while (trussJobsIterator.hasNext()) {
				final Map<String, HistoVault> map = trussJobsIterator.next().getValue();
				final Iterator<Map.Entry<String, HistoVault>> trussesIterator = map.entrySet().iterator();
				while (trussesIterator.hasNext()) {
					HistoVault truss = trussesIterator.next().getValue();
					if (FileUtils.checkFileExist(cacheFilePath.resolve(truss.getVaultName()).toString())) {
						HistoVault histoVault = null;
						try (InputStream inputStream = new BufferedInputStream(new FileInputStream(cacheFilePath.resolve(truss.getVaultName()).toFile()))) {
							histoVault = HistoVault.load(inputStream);
						}
						// put the vault into the histoSet map and sum up the file length
						if (histoVault != null && !histoVault.isTruss()) {
							List<HistoVault> vaultsList = this.get(histoVault.getLogStartTimestamp_ms());
							if (vaultsList == null) this.put(histoVault.getLogStartTimestamp_ms(), vaultsList = new ArrayList<HistoVault>());
							vaultsList.add(histoVault);
							if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("added   startTimeStamp=%s  %s  logRecordSetOrdinal=%d  logChannelNumber=%d  %s", //$NON-NLS-1$
									histoVault.getStartTimeStampFormatted(), histoVault.getVaultFileName(), histoVault.getLogRecordSetOrdinal(), histoVault.getLogChannelNumber(), histoVault.getLogFilePath()));
							localSizeSum_B += Paths.get(histoVault.getLogFilePath()).toFile().length();
						}
						trussesIterator.remove();
					}
				}
				if (map.size() == 0) trussJobsIterator.remove();
			}
		}
		return localSizeSum_B;
	}

	/**
	 * some devices create multiple recordsets which might have identical timestamp values.
	 * @param timeStampTrusses is a map with vault skeletons for the startTimeStamp 
	 * @param histoVault is the item to identify as duplicate
	 * @return true if a histoVault with identical device and channel number is already in timeStampTrusses  
	 */
	private boolean isDuplicateVault(Map<String, HistoVault> timeStampTrusses, HistoVault histoVault) {
		boolean isDuplicate = false;
		// some devices create multiple recordsets which might have identical timestamp values
		for (HistoVault tmpHistoVault : timeStampTrusses.values()) {
			if (histoVault.getVaultChannelNumber() == tmpHistoVault.getVaultChannelNumber() && histoVault.getLogChannelNumber() == tmpHistoVault.getLogChannelNumber() // 
					&& histoVault.getVaultDeviceName().equals(tmpHistoVault.getVaultDeviceName())) {
				if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, String.format("duplicate vault was discarded: device=%s  logChannelNumber=%d  logRecordSetOrdinal=%d  startTimestamp=%s  %s", //$NON-NLS-1$
						histoVault.getVaultDeviceName(), histoVault.getLogChannelNumber(), histoVault.getLogRecordSetOrdinal(), histoVault.getStartTimeStampFormatted(), histoVault.getLogFilePath()));
				isDuplicate = true;
				break;
			}
		}
		if (!isDuplicate) {
			if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, String.format("WARN same startTimeStamp=%s  logRecordSetOrdinal=%d  logChannelNumber=%d  %s", histoVault.getStartTimeStampFormatted(), //$NON-NLS-1$
					histoVault.getVaultFileName(), histoVault.getLogRecordSetOrdinal(), histoVault.getLogChannelNumber(), histoVault.getLogFilePath()));
		}
		return isDuplicate;
	}

	/**
	 * get the zip file name from the history vault class and add all histoset vaults to this file.
	 * source http://stackoverflow.com/a/17504151
	 * @return cache file bytes length
	 * @throws IOException 
	 */
	private long storeVaultsInCache(List<HistoVault> newVaults) throws IOException {
		Path cacheFilePath = Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_CACHE_ENTRIES_DIR_NAME).resolve(HistoVault.getVaultsDirectory());
		if (this.settings.isZippedCache()) {
			// use a zip file system because it supports adding files in contrast to the standard procedure using a ZipOutputStream
			Map<String, String> env = new HashMap<String, String>();
			env.put("create", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			try (FileSystem zipFileSystem = FileSystems.newFileSystem(URI.create("jar:" + cacheFilePath.toUri()), env)) { //$NON-NLS-1$
				for (HistoVault histoVault : newVaults) {
					// name the file inside the zip file 
					Path filePath = zipFileSystem.getPath(histoVault.getVaultFileName().toString());
					if (!FileUtils.checkFileExist(filePath.toString())) {
						//					if (!filePath.toFile().exists()) {
						try (BufferedOutputStream zipOutputStream = new BufferedOutputStream(Files.newOutputStream(filePath, StandardOpenOption.CREATE_NEW))) {
							histoVault.store(zipOutputStream);
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%s  %s", histoVault.getVaultFileName(), cacheFilePath.toString())); //$NON-NLS-1$
						}
					}
				}
			}
		}
		else {
			FileUtils.checkDirectoryAndCreate(cacheFilePath.toString());
			for (HistoVault histoVault : newVaults) {
				Path filePath = cacheFilePath.resolve(histoVault.getVaultFileName());
				try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(filePath, StandardOpenOption.CREATE_NEW))) {
					histoVault.store(outputStream);
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%s  %s", histoVault.getVaultFileName(), cacheFilePath.toString())); //$NON-NLS-1$
				}
			}
		}
		return cacheFilePath.toFile().length();
	}

	public void setHistoFilePaths4Test(Path filePath, int subDirLevelMax) throws IOException {
		//		this.validatedDevice = this.application.getActiveDevice();
		//		this.validatedChannel = this.application.getActiveChannel();

		if (filePath == null)
			this.validatedDataDir = Paths.get(this.settings.getDataFilePath());
		else
			this.validatedDataDir = filePath;
		this.validatedImportDir = null;
		this.validatedImportExtention = GDE.FILE_ENDING_DOT_BIN;

		this.histoFilePaths.clear();
		{
			FileUtils.checkDirectoryAndCreate(this.validatedDataDir.toString());
			List<File> files = FileUtils.getFileListing(this.validatedDataDir.toFile(), subDirLevelMax);
			log.log(Level.INFO, String.format("%04d files found in dataDir %s", files.size(), this.validatedDataDir)); //$NON-NLS-1$
			if (this.settings.getSearchDataPathImports() && !this.validatedImportExtention.isEmpty()) {
				for (File file : files) {
					if (file.getName().endsWith(GDE.FILE_ENDING_OSD) || file.getName().endsWith(this.validatedImportExtention)) {
						if (!this.histoFilePaths.containsKey(file.lastModified())) this.histoFilePaths.put(file.lastModified(), new HashSet<Path>());
						this.histoFilePaths.get(file.lastModified()).add(file.toPath());
					}
				}
			}
			else {
				for (File file : files) {
					if (file.getName().endsWith(GDE.FILE_ENDING_OSD)) {
						if (!this.histoFilePaths.containsKey(file.lastModified())) this.histoFilePaths.put(file.lastModified(), new HashSet<Path>());
						this.histoFilePaths.get(file.lastModified()).add(file.toPath());
					}
				}
			}
		}
		log.log(Level.INFO, String.format("%04d files selected", this.histoFilePaths.size())); //$NON-NLS-1$
	}

	/**
	 * determine file paths from an input directory and an import directory which fit to the objectKey, the device, the channel and the file extensions.
	 * @param rebuildStep defines which steps during histo data collection are skipped
	 * @return true if the list of file paths has already been valid
	 * @throws NotSupportedFileFormatException 
	 * @throws IOException 
	 */
	private boolean validateHistoFilePaths(RebuildStep rebuildStep) throws IOException, NotSupportedFileFormatException {
		final int subDirLevelMax = 1;

		IDevice lastDevice = this.validatedDevice;
		Channel lastChannel = this.validatedChannel;
		Path lastHistoDataDir = this.validatedDataDir;
		Path lastHistoImportDir = this.validatedImportDir;
		String lastImportExtention = this.validatedImportExtention;

		this.validatedDevice = this.application.getActiveDevice();
		this.validatedChannel = this.application.getActiveChannel();

		//special directory handling for MC3000 and Q200 supporting battery sets but store data in normal device folder
		String validatedDeviceName = validatedDevice.getName();
		if (validatedDeviceName.startsWith("MC3000")) {
			validatedDeviceName = "MC3000"; //store MC3000-Set record set as well in MC3000 directory
		}
		else if (validatedDeviceName.startsWith("Q200")) {
			validatedDeviceName = "Q200"; //store Q00-Set record set as well in Q200 directory
		}

		String subPathData = this.application.getActiveObject() == null ? validatedDeviceName : this.application.getObjectKey();
		this.validatedDataDir = Paths.get(this.settings.getDataFilePath()).resolve(subPathData);
		String subPathImport = this.application.getActiveObject() == null ? GDE.STRING_EMPTY : this.application.getObjectKey();
		this.validatedImportDir = this.validatedDevice.getDeviceConfiguration().getImportBaseDir();
		this.validatedImportDir = this.settings.getSearchImportPath() && this.validatedImportDir != null ? this.validatedImportDir.resolve(subPathImport) : null;
		this.validatedImportExtention = this.validatedDevice instanceof IHistoDevice ? ((IHistoDevice) this.validatedDevice).getSupportedImportExtention() : GDE.STRING_EMPTY;

		boolean isFullChange = rebuildStep == RebuildStep.A_HISTOSET || this.histoFilePaths.size() == 0;
		isFullChange = isFullChange || (lastDevice != null ? !lastDevice.getName().equals(validatedDeviceName) : this.validatedDevice != null);
		isFullChange = isFullChange || (lastChannel != null ? !lastChannel.channelConfigName.equals(this.validatedChannel.channelConfigName) : this.validatedChannel != null);
		isFullChange = isFullChange || (lastHistoDataDir != null ? !lastHistoDataDir.equals(this.validatedDataDir) : this.validatedDataDir != null);
		isFullChange = isFullChange || (lastHistoImportDir != null ? !lastHistoImportDir.equals(this.validatedImportDir) : this.validatedImportDir != null);
		isFullChange = isFullChange || (lastImportExtention != null ? !lastImportExtention.equals(this.validatedImportExtention) : this.validatedImportExtention != null);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("isFullChange %s", isFullChange)); //$NON-NLS-1$

		if (this.validatedDataDir != null) {
			if (isFullChange) {
				this.histoFilePaths.clear();
				{
					FileUtils.checkDirectoryAndCreate(this.validatedDataDir.toString());
					List<File> files = FileUtils.getFileListing(this.validatedDataDir.toFile(), subDirLevelMax);
					log.log(Level.INFO, String.format("%04d files found in histoDataDir %s", files.size(), this.validatedDataDir)); //$NON-NLS-1$
					if (this.settings.getSearchDataPathImports() && !this.validatedImportExtention.isEmpty()) {
						for (File file : files) {
							if (file.getName().endsWith(GDE.FILE_ENDING_OSD) || file.getName().endsWith(this.validatedImportExtention)) {
								if (!this.histoFilePaths.containsKey(file.lastModified())) this.histoFilePaths.put(file.lastModified(), new HashSet<Path>());
								this.histoFilePaths.get(file.lastModified()).add(file.toPath());
							}
						}
					}
					else {
						for (File file : files) {
							if (file.getName().endsWith(GDE.FILE_ENDING_OSD)) {
								if (!this.histoFilePaths.containsKey(file.lastModified())) this.histoFilePaths.put(file.lastModified(), new HashSet<Path>());
								this.histoFilePaths.get(file.lastModified()).add(file.toPath());
							}
						}
					}
				}
				if (this.validatedImportDir != null && this.settings.getSearchImportPath() && !this.validatedImportExtention.isEmpty()) {
					FileUtils.checkDirectoryAndCreate(this.validatedImportDir.toString());
					List<File> files = FileUtils.getFileListing(this.validatedImportDir.toFile(), subDirLevelMax);
					log.log(Level.INFO, String.format("%04d files found in histoImportDir %s", files.size(), this.validatedImportDir)); //$NON-NLS-1$
					for (File file : files) {
						if (file.getName().endsWith(this.validatedImportExtention)) {
							if (!this.histoFilePaths.containsKey(file.lastModified())) this.histoFilePaths.put(file.lastModified(), new HashSet<Path>());
							this.histoFilePaths.get(file.lastModified()).add(file.toPath());
						}
					}
				}
				log.log(Level.INFO, String.format("%04d files selected", this.histoFilePaths.size())); //$NON-NLS-1$
			}
		}
		return !isFullChange;
	}

	/**
	 * determine the vaults which are required for the data access.
	 * selects osd file candidates for the active device and the active channel; select as well for objectKey and start timestamp.
	 * selects bin file candidates for object key based on the parent directory name and last modified.
	 * @param isWithUi true allows actions on the user interface (progress bar, message boxes)
	 * @param deviceConfigurations 
	 * @return trussJobs with the actual path (not the link file path) and a map of vault skeletons (the key vaultFileName prevents double entries)
	 * @throws IOException 
	 * @throws NotSupportedFileFormatException 
	*/
	private Map<Path, Map<String, HistoVault>> getTrusses4Screening(boolean isWithUi, TreeMap<String, DeviceConfiguration> deviceConfigurations) throws IOException, NotSupportedFileFormatException {
		final Map<Path, Map<String, HistoVault>> trusses4Paths = new LinkedHashMap<Path, Map<String, HistoVault>>();
		final Map<Long, Map<String, HistoVault>> trusses4Start = new LinkedHashMap<Long, Map<String, HistoVault>>();
		final List<Integer> channelMixConfigNumbers;
		if (this.settings.isChannelMix() && this.application.getActiveDevice().getDeviceGroup() == DeviceTypes.CHARGER) // WBrueg is channel type a better criterion?
			channelMixConfigNumbers = this.application.getActiveDevice().getDeviceConfiguration().getChannelBundle(this.application.getActiveChannelNumber());
		else
			channelMixConfigNumbers = Arrays.asList(new Integer[] { this.application.getActiveChannelNumber() });
		final long minStartTimeStamp_ms = LocalDate.now().minusMonths(this.settings.getRetrospectMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
		final String supportedImportExtention = this.application.getActiveDevice() instanceof IHistoDevice ? ((IHistoDevice) this.application.getActiveDevice()).getSupportedImportExtention()
				: GDE.STRING_EMPTY;

		boolean ask4FilesWithoutObject = isWithUi;
		boolean ask4FilesWithOtherObject = isWithUi;
		int invalidRecordSetsCount = 0;
		for (Map.Entry<Long, Set<Path>> pathListEntry : this.histoFilePaths.entrySet()) {
			for (Path path : pathListEntry.getValue()) {
				Map<String, HistoVault> pathMap = new HashMap<String, HistoVault>();
				File actualFile = path.toFile();
				if (actualFile.getName().endsWith(GDE.FILE_ENDING_OSD)) {
					long startMillis = System.currentTimeMillis();
					actualFile = new File(OperatingSystemHelper.getLinkContainedFilePath(path.toString()));
					// getLinkContainedFilePath may have long response times in case of an unavailable network resources
					// This is a workaround: Much better solution would be a function 'getLinkContainedFilePathWithoutAccessingTheLinkedFile'
					if (path.equals(actualFile.toPath()) && (System.currentTimeMillis() - startMillis > 555)) {
						log.log(Level.FINER, "Dead OSD link " + path + " pointing to " + actualFile); //$NON-NLS-1$ //$NON-NLS-2$
						if (!path.toFile().delete()) {
							log.log(Level.FINE, "could not delete link file ", path); //$NON-NLS-1$
						}
					}
					else {
						String objectDirectory = GDE.STRING_EMPTY;
						if (!deviceConfigurations.containsKey(path.getParent().getFileName().toString())) objectDirectory = path.getParent().getFileName().toString();
						for (HistoVault truss : HistoOsdReaderWriter.getTrusses(actualFile, objectDirectory)) {
							boolean isValidObject = false;
							if (this.application.getActiveDevice() != null && !truss.getLogDeviceName().equals(this.application.getActiveDevice().getName())
									&& !truss.getLogDeviceName().equals(this.application.getActiveDevice().getName() + "Adapter")) { // WBrueg hard wired for HoTTViewerAdapter?
								log.log(Level.INFO, String.format("OSD candidate found for wrong device \"%s\" in %s  %s", truss.getVaultDeviceName(), actualFile, truss.getStartTimeStampFormatted()));
								break; // ignore all log file trusses 
							}
							else if (!channelMixConfigNumbers.contains(truss.getLogChannelNumber()) || truss.getLogStartTimestamp_ms() < minStartTimeStamp_ms) {
								// discard truss
							}
							else if (this.application.getActiveObject() != null && !truss.getValidatedObjectKey().isPresent()) {
								log.log(Level.INFO, String.format("OSD candidate found for empty object \"%s\" in %s  %s", truss.getLogObjectKey(), actualFile, truss.getStartTimeStampFormatted())); //$NON-NLS-1$
								if (!this.settings.skipFilesWithoutObject()) {
									if (ask4FilesWithoutObject) {
										if (SWT.YES == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0065, new String[] { path.toString() }))) {
											isValidObject = true;
											this.settings.setFilesWithoutObject(false);
										}
										ask4FilesWithoutObject = false;
									}
									else {
										isValidObject = true;
									}
								}
							}
							else if (this.application.getActiveObject() != null && !truss.getValidatedObjectKey().equals(this.application.getObjectKey())) {
								log.log(Level.INFO, String.format("OSD candidate found for wrong object \"%s\" in %s  %s", truss.getRectifiedObjectKey(), actualFile, truss.getStartTimeStampFormatted())); //$NON-NLS-1$
								if (!this.settings.skipFilesWithOtherObject()) {
									if (ask4FilesWithOtherObject) {
										if (SWT.YES == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0062, new String[] { truss.getLogObjectKey(), path.toString() }))) {
											isValidObject = true;
											this.settings.setFilesWithOtherObject(false);
										}
										ask4FilesWithoutObject = false;
									}
									else {
										isValidObject = true;
									}
								}
							}
							else if (this.application.getActiveObject() == null || truss.getValidatedObjectKey().equals(this.application.getObjectKey())) {
								log.log(Level.FINER, String.format("OSD candidate found for object       \"%s\" in %s  %s", truss.getRectifiedObjectKey(), actualFile, truss.getStartTimeStampFormatted())); //$NON-NLS-1$
								isValidObject = true;
							}

							Map<String, HistoVault> timeStampTrusses = trusses4Start.get(truss.getLogStartTimestamp_ms());
							if (isValidObject && timeStampTrusses == null) {
								pathMap.put(truss.getVaultFileName().toString(), truss);
								timeStampTrusses = new HashMap<String, HistoVault>();
								trusses4Start.put(truss.getLogStartTimestamp_ms(), timeStampTrusses);
								timeStampTrusses.put(truss.getVaultFileName().toString(), truss);
							}
							else if (isValidObject && timeStampTrusses != null && !isDuplicateVault(timeStampTrusses, truss)) {
								pathMap.put(truss.getVaultFileName().toString(), truss);
								timeStampTrusses.put(truss.getVaultFileName().toString(), truss);
							}
							else {
								invalidRecordSetsCount++;
								if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("skip   %s %3.7s    %s %2d %s", truss.getStartTimeStampFormatted(), truss.getVaultDeviceName(), //$NON-NLS-1$
										truss.getLogChannelNumber(), truss.getLogRecordSetOrdinal(), actualFile.toString()));
							}
						}
					}
				}
				else if (!supportedImportExtention.isEmpty() && actualFile.getName().endsWith(supportedImportExtention)) {
					boolean isValidObject = false;
					String objectDirectory = GDE.STRING_EMPTY;
					if (!deviceConfigurations.containsKey(path.getParent().getFileName().toString())) objectDirectory = path.getParent().getFileName().toString();
					int fileRecordSetSize = Channels.getInstance().size();
					String recordSetBaseName = DataExplorer.getInstance().getActiveChannel().getChannelConfigKey() + getRecordSetExtend(actualFile.getName());
					HistoVault truss = HistoVault.createTruss(objectDirectory, actualFile, 0, fileRecordSetSize, recordSetBaseName);
					if (truss.getLogStartTimestamp_ms() < minStartTimeStamp_ms) {
						// discard truss
					}
					else if (this.application.getActiveObject() != null && !truss.getValidatedObjectKey().equals(this.application.getObjectKey())) {
						log.log(Level.INFO, String.format("BIN candidate found for wrong object \"%s\" in %s lastModified=%d", objectDirectory, actualFile.getAbsolutePath(), actualFile.lastModified())); //$NON-NLS-1$ 
						if (!this.settings.skipFilesWithOtherObject()) {
							if (ask4FilesWithOtherObject) {
								if (SWT.YES == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0062, new String[] { objectDirectory, path.toString() }))) {
									isValidObject = true;
									this.settings.setFilesWithOtherObject(false);
								}
								ask4FilesWithoutObject = false;
							}
							else {
								isValidObject = true;
							}
						}
					}
					else if (this.application.getActiveObject() == null || truss.getValidatedObjectKey().equals(this.application.getObjectKey())) {
						isValidObject = true;
					}

					Map<String, HistoVault> timeStampTrusses = trusses4Start.get(truss.getLogStartTimestamp_ms());
					if (isValidObject && timeStampTrusses == null) {
						pathMap.put(truss.getVaultFileName().toString(), truss);
						timeStampTrusses = new HashMap<String, HistoVault>();
						trusses4Start.put(truss.getLogStartTimestamp_ms(), timeStampTrusses);
						timeStampTrusses.put(truss.getVaultFileName().toString(), truss);
					}
					else if (isValidObject && timeStampTrusses != null && !isDuplicateVault(timeStampTrusses, truss)) {
						pathMap.put(truss.getVaultFileName().toString(), truss);
						timeStampTrusses.put(truss.getVaultFileName().toString(), truss);
					}
					else {
						invalidRecordSetsCount++;
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("isValidRecordSet=false  lastModified=%,d  %s", actualFile.lastModified(), actualFile.getAbsolutePath())); //$NON-NLS-1$
					}
				}
				if (pathMap.size() > 0) trusses4Paths.put(actualFile.toPath(), pathMap);
			}
		}
		log.log(Level.INFO, String.format("%04d files taken --- %04d checked files --- %04d invalid recordsets", trusses4Paths.size(), this.histoFilePaths.size(), invalidRecordSetsCount)); //$NON-NLS-1$
		return trusses4Paths;
	}

	/**
	 * @return all the paths which have been identified in the last validation sorted by lastModified in reverse order
	 */
	public Map<Long, Set<Path>> getHistoFilePaths() {
		return this.histoFilePaths;
	}

	@Deprecated
	public void clearMeasurementModes() {
		throw new UnsupportedOperationException();
	}

	public TrailRecordSet getTrailRecordSet() {
		return this.trailRecordSet;
	}

	/**
	 * @return the validatedDataDir
	 */
	public Path getValidatedDataDir() {
		return this.validatedDataDir;
	}

	/**
	 * @return the validatedImportDir
	 */
	public Path getValidatedImportDir() {
		return this.validatedImportDir;
	}

	/**
	 * @return the validatedImportExtention
	 */
	public String getValidatedImportExtention() {
		return this.validatedImportExtention;
	}

	/**
	 * compose the record set extend to give capability to identify source of this record set
	 * @param fileName
	 * @return
	 */
	private String getRecordSetExtend(String fileName) {
		String recordSetNameExtend = GDE.STRING_EMPTY;
		if (fileName.contains(GDE.STRING_UNDER_BAR)) {
			try {
				Integer.parseInt(fileName.substring(0, fileName.lastIndexOf(GDE.STRING_UNDER_BAR)));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.lastIndexOf(GDE.STRING_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (fileName.substring(0, fileName.lastIndexOf(GDE.STRING_UNDER_BAR)).length() <= 8)
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.lastIndexOf(GDE.STRING_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		else {
			try {
				Integer.parseInt(fileName.substring(0, 4));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, 4) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (fileName.substring(0, fileName.length()).length() <= 8 + 4) recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.length() - 4) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		return recordSetNameExtend;
	}

}
