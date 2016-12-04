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
 
 Copyright (c) 2016 Thomas Eickert
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import org.eclipse.swt.SWT;

import gde.GDE;
import gde.config.Settings;
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
 * holds current selection of histo vaults for the selected channel, device and object.
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
	 * key is lastModified [ms] of the file, the list holds link file paths or file paths for all types of log files
	 */
	private Map<Long, Set<Path>>	histoFilePaths						= new TreeMap<Long, Set<Path>>(Collections.reverseOrder());	// histo files coming from the last directory validation
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
		* true starts building the histo recordsets
		*/
		B_HISTORECORDSETS(4),
		/**
		* true starts building the trail recordset from the histo recordsets 
		*/
		C_TRAILRECORDSET(3),
		/**
		* true starts refreshing the trail data from the histo recordsets
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
		log.log(Level.SEVERE, String.format("device=%s  channel=%d  objectKey=%s", this.application.getActiveDevice() == null ? null : this.application.getActiveDevice().getName(), //$NON-NLS-1$
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
		log.log(Level.SEVERE, String.format("device=%s  channel=%d  objectKey=%s", this.application.getActiveDevice() == null ? null : this.application.getActiveDevice().getName(), //$NON-NLS-1$
				this.application.getActiveChannelNumber(), this.application.getObjectKey()));
	}

	/**
	 * determine histo files, build the trail recordset anf for each file read histo recordsets and populate the trail recordset.
	 * Disregard rebuild steps if histo file paths have changed which may occur if new files have been added by the user or the device, channel or object was modified. 
	 * @param rebuildStep
	 * @param isWithUi true allows actions on the user interface (progress bar only)
	 * @return true if the HistoSet was rebuilt
	 * @throws DataTypeException 
	 * @throws DataInconsitsentException 
	 * @throws NotSupportedFileFormatException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public boolean rebuild4Screening(RebuildStep rebuildStep, boolean isWithUi) throws FileNotFoundException, IOException, NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {
		boolean isRebuilt = false;
		log.log(Level.INFO, rebuildStep.toString());
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) this.application.setProgress(2, sThreadId);

		long startTimeFileValid = new Date().getTime();
		boolean isHistoFilePathsValid = this.validateHistoFilePaths(rebuildStep);
		{
			if (!isHistoFilePathsValid) {
				this.clear();
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("histoSet             clear")); //$NON-NLS-1$
				if ((new Date().getTime() - startTimeFileValid) > 0) log.log(Level.TIME, String.format("%,5d files          select folders     time=%,6d [ms]  ::  per second:%5d", this.histoFilePaths.size(), //$NON-NLS-1$
						new Date().getTime() - startTimeFileValid, this.histoFilePaths.size() * 1000 / (new Date().getTime() - startTimeFileValid)));
			}
			else { // histo record sets are ready to use
				if (log.isLoggable(Level.TIME)) log.log(Level.TIME, String.format("%,5d files    file paths verified     time=%s [ss.SSS]", this.getHistoFilePaths().size(), //$NON-NLS-1$
						StringHelper.getFormatedTime("ss.SSS", new Date().getTime() - startTimeFileValid))); //$NON-NLS-1$
			}
			if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) this.application.setProgress(5, sThreadId);
		}
		{
			if (RebuildStep.A_HISTOSET == rebuildStep) {
				isRebuilt = true;
				this.initialize();
				if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("histoSet             initialize")); //$NON-NLS-1$
			}
			if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) this.application.setProgress(7, sThreadId);
		}
		{
			if (!isHistoFilePathsValid || EnumSet.of(RebuildStep.A_HISTOSET, RebuildStep.B_HISTORECORDSETS).contains(rebuildStep)) {
				isRebuilt = true;
				this.fileSizeSum_B = 0;
				if (this.getHistoFilePaths().size() > 0) {
					long nanoTimeCheckFilesSum = -System.nanoTime();
					// step: build the workload map consisting of the cache key and the file path
					Map<String, Path> cacheKeyJobs = getCacheKeyJobs4Screening();
					nanoTimeCheckFilesSum += System.nanoTime();
					if (TimeUnit.NANOSECONDS.toMillis(nanoTimeCheckFilesSum) > 0)
						log.log(Level.TIME, String.format("%,5d files          job check          time=%,6d [ms]  ::  per second:%5d", this.histoFilePaths.size(), //$NON-NLS-1$
								TimeUnit.NANOSECONDS.toMillis(nanoTimeCheckFilesSum), this.histoFilePaths.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeCheckFilesSum)));

					// step: put cached vaults into the histoSet map and reduce workload map
					long nanoTimeReadVaultSum = -System.nanoTime();
					int cacheUsedSize = cacheKeyJobs.size();
					long fileSizeSumCached_B = this.fileSizeSum_B = loadVaultsFromCache(cacheKeyJobs);
					cacheUsedSize -= cacheKeyJobs.size();
					nanoTimeReadVaultSum += System.nanoTime();
					if (TimeUnit.NANOSECONDS.toMillis(nanoTimeReadVaultSum) > 0) log.log(Level.TIME,
							String.format("%,5d recordsets     load from cache    time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,6d MB/s", cacheUsedSize, //$NON-NLS-1$
									TimeUnit.NANOSECONDS.toMillis(nanoTimeReadVaultSum), cacheUsedSize * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeReadVaultSum),
									this.fileSizeSum_B / TimeUnit.NANOSECONDS.toMicros(nanoTimeReadVaultSum)));

					// step: calculate progress bar parameters
					double progressCycle = 0.;
					int progressStart = 0;
					if (isWithUi) {
						progressStart = DataExplorer.application.getProgressPercentage();
						int progressEstimation = progressStart - (int) ((95 - progressStart) * this.size() / (double) (this.size() + 333 * cacheKeyJobs.size())); // 333 is the estimated processing time ratio between reading from files and reading from cache
						this.application.setProgress(progressEstimation, sThreadId);
						progressCycle = (95 - progressEstimation) / (double) cacheKeyJobs.size();
					}

					// step: transform log files from workload map into vaults and put them into the histoSet map
					long nanoTimeReadRecordSetSum = -System.nanoTime();
					ArrayList<HistoVault> newVaults = new ArrayList<HistoVault>();
					// get set holding the remaining workload in the same order but without path duplicates arising from files with multiple recordsets
					LinkedHashSet<Path> workloadPaths = new LinkedHashSet<Path>(cacheKeyJobs.values());
					int i = 0;
					for (Path filePath : workloadPaths) {
						newVaults.addAll(loadVaultsFromFile(filePath));
						this.fileSizeSum_B += filePath.toFile().length();
						if (isWithUi) this.application.setProgress((int) (++i * progressCycle + progressStart), sThreadId);
						if (this.size() >= this.settings.getMaxLogCount()) //
							break;
					}
					nanoTimeReadRecordSetSum += System.nanoTime();
					if (newVaults.size() > 0)
						log.log(Level.TIME,
								String.format("%,5d recordsets     create from files  time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,6d MB/s", newVaults.size(), //$NON-NLS-1$
										TimeUnit.NANOSECONDS.toMillis(nanoTimeReadRecordSetSum), newVaults.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeReadRecordSetSum),
										(this.fileSizeSum_B - fileSizeSumCached_B) / TimeUnit.NANOSECONDS.toMicros(nanoTimeReadRecordSetSum)));
					else if (TimeUnit.NANOSECONDS.toMillis(nanoTimeReadRecordSetSum) > 0) log.log(Level.TIME,
							String.format("%,5d files          read w/o data      time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,6d MB/s", workloadPaths.size(), //$NON-NLS-1$
									TimeUnit.NANOSECONDS.toMillis(nanoTimeReadRecordSetSum), workloadPaths.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeReadRecordSetSum),
									(this.fileSizeSum_B - fileSizeSumCached_B) / TimeUnit.NANOSECONDS.toMicros(nanoTimeReadRecordSetSum)));

					// step: save vaults in the file system
					long nanoTimeWriteVaultSum = -System.nanoTime(), cacheSize_B = 0;
					if (newVaults.size() > 0) {
						cacheSize_B = storeVaultsInCache(newVaults);
					}
					nanoTimeWriteVaultSum += System.nanoTime();
					if (TimeUnit.NANOSECONDS.toMillis(nanoTimeWriteVaultSum) > 0 && cacheSize_B > 0) log.log(Level.TIME,
							String.format("%,5d recordsets     store in cache     time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,6d MB/s", newVaults.size(), //$NON-NLS-1$
									TimeUnit.NANOSECONDS.toMillis(nanoTimeWriteVaultSum), newVaults.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeWriteVaultSum),
									(this.fileSizeSum_B - fileSizeSumCached_B) / TimeUnit.NANOSECONDS.toMicros(nanoTimeWriteVaultSum)));
					log.log(Level.TIME, String.format("%,5d recordsets     total              time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,6d MB/s", this.size(), //$NON-NLS-1$
							new Date().getTime() - startTimeFileValid, this.size() * 1000 / (new Date().getTime() - startTimeFileValid), this.fileSizeSum_B / 1000 / (new Date().getTime() - startTimeFileValid)));
				}
			}
			if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) this.application.setProgress(95, sThreadId);
		}
		{
			if (!isHistoFilePathsValid || EnumSet.of(RebuildStep.A_HISTOSET, RebuildStep.B_HISTORECORDSETS, RebuildStep.C_TRAILRECORDSET).contains(rebuildStep)) {
				isRebuilt = true;
				long nanoTimeTrailRecordSet = -System.nanoTime();
				this.trailRecordSet = TrailRecordSet.createRecordSet(this.application.getActiveDevice(), this.application.getActiveChannelNumber());
				this.trailRecordSet.defineTrailTypes();
				// this.trailRecordSet.checkAllDisplayable();
				this.trailRecordSet.setPoints();
				this.trailRecordSet.setTags();
				this.trailRecordSet.applyTemplate(true); // needs reasonable data
				nanoTimeTrailRecordSet += System.nanoTime();
				if (this.fileSizeSum_B > 0 && log.isLoggable(Level.TIME)) log.log(Level.TIME,
						String.format("%,5d trailTimeSteps build and populate time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,5d MB/s", this.size(), TimeUnit.NANOSECONDS.toMillis(nanoTimeTrailRecordSet), //$NON-NLS-1$
								this.size() > 0 ? this.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeTrailRecordSet) : 0, this.fileSizeSum_B / TimeUnit.NANOSECONDS.toMicros(nanoTimeTrailRecordSet)));
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
		return isRebuilt;
	}

	/**
	 * read file and populate vaults from the histo recordsets.
	 * put the vaults into the histoset map.
	 * @param filePath holds a link file path or a log file path
	 * @throws DataTypeException  origin is bin file reader only
	 * @throws DataInconsitsentException 
	 * @throws NotSupportedFileFormatException 
	 * @throws IOException 	
	 * @throws FileNotFoundException 
	 * @return the vaults extracted from the file
	 */
	private List<HistoVault> loadVaultsFromFile(Path filePath) throws FileNotFoundException, IOException, NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {
		List<HistoVault> result = new ArrayList<HistoVault>();
		Path actualPath;

		// populate history record sets from the file
		long nanoTime = System.nanoTime();
		List<HistoRecordSet> histoRecordSets = new ArrayList<HistoRecordSet>();
		if (filePath.toString().endsWith(GDE.FILE_ENDING_DOT_BIN)) {
			actualPath = filePath;
			HistoRecordSet recordSet = ((IHistoDevice) this.application.getActiveDevice()).getRecordSetFromImportFile(filePath);
			if (recordSet.getRecordDataSize(true) > 0) {
				histoRecordSets.add(recordSet);
			}
		}
		else if (filePath.toString().endsWith(GDE.FILE_ENDING_DOT_OSD)) {
			actualPath = FileSystems.getDefault().getPath(OperatingSystemHelper.getLinkContainedFilePath(filePath.toAbsolutePath().toString()));
			String logPathObjectKey = this.settings.getValidateObjectKey(filePath.getParent().getFileName().toString()).orElse(GDE.STRING_EMPTY);
			if (this.settings.isChannelMix() && this.application.getActiveDevice().getDeviceGroup() == DeviceTypes.CHARGER) {
				HistoOsdReaderWriter.readHisto(histoRecordSets, actualPath, this.application.getActiveDevice().getDeviceConfiguration().getChannelBundle(this.application.getActiveChannelNumber()),
						logPathObjectKey);
			}
			else {
				HistoOsdReaderWriter.readHisto(histoRecordSets, actualPath, Arrays.asList(new Integer[] { this.application.getActiveChannelNumber() }), logPathObjectKey);
			}
		}
		else {
			actualPath = null;
			log.log(Level.SEVERE, String.format("file format not supported: device = %s  channelConfigNumber = %d  histoFilePath = %s", //$NON-NLS-1$
					this.application.getActiveDevice().getName(), this.application.getActiveChannelNumber(), filePath));
		}

		// transform history record sets into vaults
		for (HistoRecordSet histoRecordSet : histoRecordSets) {
			// prepare historecordset and histoset for history vault
			histoRecordSet.addSettlements();
			histoRecordSet.setElapsedHistoRecordSet_ns(histoRecordSets.size() > 0 ? (System.nanoTime() - nanoTime) / histoRecordSets.size() : 0);
			boolean alreadyExists = false;
			if (this.containsKey(histoRecordSet.getStartTimeStamp())) {
				// some devices create multiple recordsets which might have identical timestamp values
				for (HistoVault histoVault : this.get(histoRecordSet.getStartTimeStamp())) {
					if (histoVault.getUiChannelNumber() == histoRecordSet.getChannelConfigNumber() && histoVault.getLogChannelNumber() == histoRecordSet.getChannelConfigNumber() // 
							&& histoVault.getUiDeviceName().equals(histoRecordSet.getDevice().getName())) {
						if (log.isLoggable(Level.WARNING))
							log.log(Level.WARNING, String.format("duplicate histo recordSet was discarded: device = %s  channelConfigNumber = %d  timestamp = %,d  histoFilePath = %s", //$NON-NLS-1$
									histoRecordSet.getDevice().getName(), histoRecordSet.getChannelConfigNumber(), histoRecordSet.getStartTimeStamp(), actualPath));
						alreadyExists = true;
						break;
					}
				}
				if (!alreadyExists) {
					if (log.isLoggable(Level.INFO)) log.log(Level.WARNING,
							String.format("WARNING  startTimeStamp=%s :: different recordSet with identical start time was added : %s   device = %s  channelConfigNumber = %d", //$NON-NLS-1$
									StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", histoRecordSet.getStartTimeStamp()), actualPath, histoRecordSet.getDevice().getName(), //$NON-NLS-1$
									histoRecordSet.getChannelConfigNumber()));
				}
			}
			else {
				this.put(histoRecordSet.getStartTimeStamp(), new ArrayList<HistoVault>());
			}
			// put all aggregated data and scores into the history vault and put the vault into the histoSet map
			HistoVault histoVault = histoRecordSet.getHistoVault();
			result.add(histoVault);
			if (!alreadyExists) {
				this.get(histoVault.getLogStartTimestamp_ms()).add(histoVault);
			}
			// reduce memory consumption in advance to the garbage collection
			histoRecordSet.cleanup();
		}
		return result;
	}

	/**
	 * put cached vaults into the histoSet map and reduce workload map.
	 * @param cacheKeyJobs cache key and the link file or file path of the log source file
	 * @return total length (bytes) of the original log files of those vaults which were put into the histoset 
	 * @throws IOException origin is opening or traversing the zip file
	 */
	private long loadVaultsFromCache(Map<String, Path> cacheKeyJobs) throws IOException {
		long localSizeSum_B = 0;
		Path cacheFilePath = Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_CACHE_ENTRIES_DIR_NAME).resolve(HistoVault.getVaultSubDirectory());
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("cacheFilePath=%s", cacheFilePath.toString())); //$NON-NLS-1$
		if (this.settings.isZippedCache()) {
			if (FileUtils.checkFileExist(cacheFilePath.toString())) {
				try (ZipFile zf = new ZipFile(cacheFilePath.toFile())) { // closing the zip file closes all streams
					Iterator<Map.Entry<String, Path>> iter = cacheKeyJobs.entrySet().iterator();
					while (iter.hasNext()) {
						Map.Entry<String, Path> workloadEntry = iter.next();
						if (zf.getEntry(workloadEntry.getKey()) != null) {
							HistoVault histoVault = HistoVault.load(new BufferedInputStream(zf.getInputStream(zf.getEntry(workloadEntry.getKey()))));
							if (loadVaultFromCache(histoVault)) localSizeSum_B += cacheKeyJobs.get(workloadEntry.getKey()).toFile().length();
							iter.remove();
						}
					}
				}
			}
		}
		else {
			if (FileUtils.checkDirectoryAndCreate(cacheFilePath.toString())) {
				Iterator<Map.Entry<String, Path>> iter = cacheKeyJobs.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry<String, Path> workloadEntry = iter.next();
					if (FileUtils.checkFileExist(cacheFilePath.resolve(workloadEntry.getKey()).toString())) {
						try (InputStream inputStream = new BufferedInputStream(new FileInputStream(cacheFilePath.resolve(workloadEntry.getKey()).toFile()))) {
							if (loadVaultFromCache(HistoVault.load(inputStream))) localSizeSum_B += cacheKeyJobs.get(workloadEntry.getKey()).toFile().length();
						}
						iter.remove();
					}
				}
			}
		}
		return localSizeSum_B;
	}

	/**
	 * @param histoVault
	 * @return true if the vault was added to the vaults list
	 */
	private boolean loadVaultFromCache(HistoVault histoVault) {
		boolean fileTakenFromCache = true;
		if (this.containsKey(histoVault.getLogStartTimestamp_ms())) {
			// some devices create multiple recordsets which might have identical timestamp values
			for (HistoVault tmpHistoVault : this.get(histoVault.getLogStartTimestamp_ms())) {
				if (histoVault.getUiChannelNumber() == tmpHistoVault.getUiChannelNumber() && histoVault.getLogChannelNumber() == tmpHistoVault.getLogChannelNumber() // 
						&& histoVault.getUiDeviceName().equals(tmpHistoVault.getUiDeviceName())) {
					if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, String.format("duplicate vault was discarded: device = %s  channelConfigNumber = %d  timestamp = %,d  histoFilePath = %s", //$NON-NLS-1$
							tmpHistoVault.getUiDeviceName(), tmpHistoVault.getLogChannelNumber(), tmpHistoVault.getLogStartTimestamp_ms(), tmpHistoVault.getFilePath()));
					fileTakenFromCache = false;
					break;
				}
			}
			if (fileTakenFromCache) {
				if (log.isLoggable(Level.INFO)) log.log(Level.WARNING,
						String.format("WARN same startTimeStamp=%s  %s ::  %s   device = %s  channelConfigNumber = %d", //$NON-NLS-1$
								StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", histoVault.getLogStartTimestamp_ms()), histoVault.getVaultKey(), histoVault.getFilePath(), histoVault.getUiDeviceName(), //$NON-NLS-1$
								histoVault.getLogChannelNumber()));
			}
		}
		else {
			this.put(histoVault.getLogStartTimestamp_ms(), new ArrayList<HistoVault>());
		}
		// put the vault into the histoSet map
		if (fileTakenFromCache) {
			// put the vault into the histoSet map and sum up the file length
			this.get(histoVault.getLogStartTimestamp_ms()).add(histoVault);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER,
					String.format("added   startTimeStamp=%s  %s ::  %s   device = %s  channelConfigNumber = %d", //$NON-NLS-1$
							StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", histoVault.getLogStartTimestamp_ms()), histoVault.getVaultKey(), histoVault.getFilePath(), histoVault.getUiDeviceName(), //$NON-NLS-1$
							histoVault.getLogChannelNumber()));
		}
		return fileTakenFromCache;
	}

	/**
	 * get the zip file name from the history vault class and add all histoset vaults to this file.
	 * source http://stackoverflow.com/a/17504151
	 * @return cache file bytes length
	 * @throws IOException 
	 */
	private long storeVaultsInCache(List<HistoVault> newVaults) throws IOException {
		Path cacheFilePath = Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_CACHE_ENTRIES_DIR_NAME).resolve(HistoVault.getVaultSubDirectory());
		if (this.settings.isZippedCache()) {
			// use a zip file system because it supports adding files in contrast to the standard procedure using a ZipOutputStream
			Map<String, String> env = new HashMap<String, String>();
			env.put("create", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			try (FileSystem zipFileSystem = FileSystems.newFileSystem(URI.create("jar:" + cacheFilePath.toUri()), env)) { //$NON-NLS-1$
				for (HistoVault histoVault : newVaults) {
					// name the file inside the zip file 
					Path filePath = zipFileSystem.getPath(histoVault.getVaultKey());
					if (!FileUtils.checkFileExist(filePath.toString())) {
						//					if (!filePath.toFile().exists()) {
						try (BufferedOutputStream zipOutputStream = new BufferedOutputStream(Files.newOutputStream(filePath, StandardOpenOption.CREATE_NEW))) {
							histoVault.store(zipOutputStream);
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%s  %s", histoVault.getVaultKey(), cacheFilePath.toString())); //$NON-NLS-1$
						}
					}
				}
			}
		}
		else {
			FileUtils.checkDirectoryAndCreate(cacheFilePath.toString());
			for (HistoVault histoVault : newVaults) {
				Path filePath = cacheFilePath.resolve(histoVault.getVaultKey());
				try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(filePath, StandardOpenOption.CREATE_NEW))) {
					histoVault.store(outputStream);
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%s  %s", histoVault.getVaultKey(), cacheFilePath.toString())); //$NON-NLS-1$
				}
			}
		}
		return cacheFilePath.toFile().length();
	}

	/**
	 * determine file paths from an input directory and an import directory which fit to the objectKey, the device, the channel and the file extensions.
	 * resets the histoset singleton if the trail is invalid.
	 * decide if the file list valid via comparison against the last call of this method.
	 * @param rebuildStep define which steps during histo data collection are skipped
	 * @return true if the list of file paths has already been valid (no modification by this method)
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
		String subPath = this.application.getActiveObject() == null ? this.validatedDevice.getName() : this.application.getObjectKey();
		this.validatedDataDir = this.settings.getDataBaseDir().resolve(subPath);
		subPath = this.application.getActiveObject() == null ? GDE.STRING_EMPTY : this.application.getObjectKey();
		this.validatedImportDir = this.validatedDevice.getDeviceConfiguration().getImportBaseDir();
		this.validatedImportDir = this.settings.getSearchImportPath() && this.validatedImportDir != null ? this.validatedImportDir.resolve(subPath) : null;
		this.validatedImportExtention = this.validatedDevice.getDeviceConfiguration().getDataBlockType().getPreferredFileExtention();
		this.validatedImportExtention = this.settings.getSearchImportPath() && !this.validatedImportExtention.isEmpty() ? this.validatedImportExtention.substring(1) : GDE.STRING_EMPTY;

		boolean isFullChange = rebuildStep == RebuildStep.A_HISTOSET || this.histoFilePaths.size() == 0;
		isFullChange = isFullChange || (lastDevice != null ? !lastDevice.getName().equals(this.validatedDevice.getName()) : this.validatedDevice != null);
		isFullChange = isFullChange || (lastChannel != null ? !lastChannel.channelConfigName.equals(this.validatedChannel.channelConfigName) : this.validatedChannel != null);
		isFullChange = isFullChange || (lastHistoDataDir != null ? !lastHistoDataDir.equals(this.validatedDataDir) : this.validatedDataDir != null);
		isFullChange = isFullChange || (lastHistoImportDir != null ? !lastHistoImportDir.equals(this.validatedImportDir) : this.validatedImportDir != null);
		isFullChange = isFullChange || (lastImportExtention != null ? !lastImportExtention.equals(this.validatedImportExtention) : this.validatedImportExtention != null);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("isFullChange %s", isFullChange)); //$NON-NLS-1$

		if (isFullChange) {
			if (this.validatedDataDir != null) {
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
	 * selects osd file candidates for the active device and the active channel; select as well for objectKey and start timestamp in case of screening.
	 * select all bin files; select as well for object key based on the parent directory name and last modified in case of screening.
	 * @return map with cache keys and link file paths or file paths copied from the current list of histoFilePaths with identical order ('lastModified' descending)
	 * @throws IOException 
	 * @throws NotSupportedFileFormatException 
	*/
	private LinkedHashMap<String, Path> getCacheKeyJobs4Screening() throws IOException, NotSupportedFileFormatException {
		LinkedHashMap<String, Path> result = new LinkedHashMap<String, Path>();
		int invalidFilesCount = 0;
		for (Map.Entry<Long, Set<Path>> pathListEntry : this.histoFilePaths.entrySet()) {
			for (Path path : pathListEntry.getValue()) {
				File file = path.toFile();
				if (file.getName().endsWith(GDE.FILE_ENDING_OSD)) {
					long startMillis = System.currentTimeMillis();
					Path actualPath = FileSystems.getDefault().getPath(OperatingSystemHelper.getLinkContainedFilePath(path.toAbsolutePath().toString()));
					// getLinkContainedFilePath may have long response times in case of an unavailable network resources
					// This is a workaround: Much better solution would be a function 'getLinkContainedFilePathWithoutAccessingTheLinkedFile'
					if (path.toAbsolutePath().equals(actualPath.toAbsolutePath()) && (System.currentTimeMillis() - startMillis > 555)) {
						log.log(Level.FINER, "Dead OSD link " + path + " pointing to " + actualPath); //$NON-NLS-1$ //$NON-NLS-2$
						if (!file.delete()) {
							log.log(Level.FINE, "could not delete link file " + file.getName()); //$NON-NLS-1$
						}
					}
					else {
						HashMap<String, String> fileHeader = HistoOsdReaderWriter.getHeader(actualPath.toString());
						if (this.application.getActiveDevice() != null && !fileHeader.get(GDE.DEVICE_NAME).equals(this.application.getActiveDevice().getName())) {
							log.log(Level.WARNING, String.format("OSD candidate found for wrong device \"%s\" in %s %s %s   %s %s", fileHeader.get(GDE.DEVICE_NAME), actualPath, GDE.CREATION_TIME_STAMP, //$NON-NLS-1$
									fileHeader.get(GDE.CREATION_TIME_STAMP), GDE.DATA_EXPLORER_FILE_VERSION, fileHeader.get(GDE.DATA_EXPLORER_FILE_VERSION)));
							continue; // ignore file
						}
						boolean isValidFile = false;
						boolean isValidObject = false;
						if (this.application.getActiveObject() != null && fileHeader.get(GDE.OBJECT_KEY).isEmpty()) {
							log.log(Level.INFO, String.format("OSD candidate found for empty object \"%s\" in %s %s %s   %s %s", fileHeader.get(GDE.OBJECT_KEY), actualPath, GDE.CREATION_TIME_STAMP, //$NON-NLS-1$
									fileHeader.get(GDE.CREATION_TIME_STAMP), GDE.DATA_EXPLORER_FILE_VERSION, fileHeader.get(GDE.DATA_EXPLORER_FILE_VERSION)));
							if (!this.settings.skipFilesWithoutObject() || SWT.YES == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0065, new String[] { actualPath.toString() }))) {
								isValidObject = true;
								this.settings.setFilesWithoutObject(false);
							}
						}
						else if (this.application.getActiveObject() != null && !fileHeader.get(GDE.OBJECT_KEY).equalsIgnoreCase(this.application.getObjectKey())) {
							log.log(Level.INFO, String.format("OSD candidate found for wrong object \"%s\" in %s %s %s   %s %s", fileHeader.get(GDE.OBJECT_KEY), actualPath, GDE.CREATION_TIME_STAMP, //$NON-NLS-1$
									fileHeader.get(GDE.CREATION_TIME_STAMP), GDE.DATA_EXPLORER_FILE_VERSION, fileHeader.get(GDE.DATA_EXPLORER_FILE_VERSION)));
							if (!this.settings.skipFilesWithOtherObject()
									|| SWT.YES == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0062, new String[] { fileHeader.get(GDE.OBJECT_KEY), actualPath.toString() }))) {
								isValidObject = true;
								this.settings.setFilesWithOtherObject(false);
							}
						}
						else if (this.application.getActiveObject() == null || fileHeader.get(GDE.OBJECT_KEY).equalsIgnoreCase(this.application.getObjectKey())) {
							log.log(Level.FINER,
									String.format("OSD candidate found for object       \"%s\" in %s %s %s   %s %s", //$NON-NLS-1$
											(this.application.getObjectKey()), actualPath, GDE.CREATION_TIME_STAMP, fileHeader.get(GDE.CREATION_TIME_STAMP), GDE.DATA_EXPLORER_FILE_VERSION,
											fileHeader.get(GDE.DATA_EXPLORER_FILE_VERSION)));
							isValidObject = true;
						}
						if (isValidObject) {
							if (this.settings.isChannelMix() && this.application.getActiveDevice().getDeviceGroup() == DeviceTypes.CHARGER) {
								isValidFile = !HistoOsdReaderWriter.isOutOfScope(fileHeader, this.application.getActiveDevice().getDeviceConfiguration().getChannelBundle(this.application.getActiveChannelNumber()),
										LocalDate.now().minusMonths(this.settings.getRetrospectMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
							}
							else {
								isValidFile = !HistoOsdReaderWriter.isOutOfScope(fileHeader, Arrays.asList(new Integer[] { this.application.getActiveChannelNumber() }),
										LocalDate.now().minusMonths(this.settings.getRetrospectMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
							}
						}
						if (isValidFile) {
							for (java.util.Map.Entry<String, String> headerEntry : fileHeader.entrySet()) {
								if (headerEntry.getKey().contains(GDE.RECORD_SET_NAME)) {
									int recordSetNumber = Integer.parseInt(headerEntry.getValue().split(Pattern.quote(GDE.STRING_RIGHT_PARENTHESIS_BLANK))[0]); // Pattern prevents regex interpretation
									result.put(HistoVault.getVaultKey(path, file.lastModified(), recordSetNumber).toString(), path);
									if (log.isLoggable(Level.FINER))
										log.log(Level.FINER, String.format("%s  %s", HistoVault.getVaultKey(actualPath, actualPath.toFile().lastModified(), recordSetNumber).toString(), actualPath.toString())); //$NON-NLS-1$
								}
							}
						}
						else {
							invalidFilesCount++;
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("isValidFile=false  lastModified=%,d  %s", file.lastModified(), actualPath.toString())); //$NON-NLS-1$
						}
					}
				}
				else if (file.getName().endsWith(GDE.FILE_ENDING_DOT_BIN)) {
					boolean isValidFile = false;
					if (this.application.getActiveObject() == null) {
						isValidFile = file.lastModified() > LocalDate.now().minusMonths(this.settings.getRetrospectMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
					}
					else {
						String logObjectKey = this.settings.getValidateObjectKey(path.getParent().getFileName().toString()).orElse(GDE.STRING_EMPTY);
						if (logObjectKey.equals(this.application.getObjectKey())) {
							isValidFile = file.lastModified() > LocalDate.now().minusMonths(this.settings.getRetrospectMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
						}
						else {
							log.log(Level.INFO, String.format("BIN candidate found for wrong object \"%s\" in %s %s %s", logObjectKey, path, "lastModified=", file.lastModified())); //$NON-NLS-1$ //$NON-NLS-2$
							if (!this.settings.skipFilesWithOtherObject()
									|| SWT.YES == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0062, new String[] { logObjectKey, path.toString() }))) {
								isValidFile = file.lastModified() > LocalDate.now().minusMonths(this.settings.getRetrospectMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
								this.settings.setFilesWithOtherObject(true);
							}
						}
					}
					if (isValidFile) {
						log.log(Level.FINER, String.format("BIN candidate found for object       \"%s\" in %s %s %s", (this.application.getObjectKey()), path, "lastModified=", file.lastModified())); //$NON-NLS-1$ //$NON-NLS-2$
						result.put(HistoVault.getVaultKey(path, file.lastModified(), this.application.getActiveChannelNumber()).toString(), path); // recordSetNumber is 1
					}
					else {
						invalidFilesCount++;
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("isValidFile=false  lastModified=%,d  %s", file.lastModified(), path)); //$NON-NLS-1$
					}
				}
			}
		}
		log.log(Level.INFO, String.format("%04d files taken --- %04d checked files --- %04d invalid files", result.size(), this.histoFilePaths.size(), invalidFilesCount)); //$NON-NLS-1$
		return result;

	}

	/**
	 * @return all the paths which have been identified in the last validation sorted by lastModified in reverse order
	 */
	public Map<Long, Set<Path>> getHistoFilePaths() {
		return this.histoFilePaths;
	}

	@Deprecated
	public void clearMeasurementModes() {
		// todo Auto-generated method stub -> copy from RecordSet
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

}
