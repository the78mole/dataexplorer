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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.eclipse.swt.SWT;

import gde.GDE;
import gde.config.Settings;
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
 * sorted by startTimeStamp of the recordSet in reverse order; each timestamp may hold multiple vaults, i.e. aggregated data of a recordset.
 * @author Thomas Eickert
 */
public class HistoSet extends TreeMap<Long, List<HistoVault>> {
	private final static String		$CLASS_NAME							= HistoSet.class.getName();
	private static final long			serialVersionUID				= 1111377035274863787L;
	private final static Logger		log											= Logger.getLogger($CLASS_NAME);

	private final DataExplorer		application							= DataExplorer.getInstance();
	private final Settings				settings								= Settings.getInstance();

	private static HistoSet				histoSet								= null;

	private IDevice								lastDevice							= null;
	private Channel								lastChannel							= null;
	private Path									lastHistoDataDir				= null;
	private Path									lastHistoImportDir			= null;
	private String								lastImportFileExtention	= GDE.STRING_EMPTY;

	/**
	 * key is lastModified [ms] of the file
	 */
	private Map<Long, List<Path>>	histoFilePaths					= new TreeMap<Long, List<Path>>(Collections.reverseOrder());	// histo files coming from the last directory validation
	private long									fileSizeSum_B						= 0;																													// size of all the histo files which have been read to build the histo recordsets 
	private TrailRecordSet				trailRecordSet					= null;																												// histo data transformed in a recordset format

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
		// deep clear in order to reduce memory consumption prior to garbage collection
		for (List<HistoVault> timestampHistoVaults : this.values()) {
			timestampHistoVaults.clear();
		}
		this.clear();

		// this.histoFilePaths.clear(); is accomplished by files validation
		this.fileSizeSum_B = 0;
		this.trailRecordSet = null;
		log.log(Level.SEVERE, String.format("device=%s  channel=%d  objectKey=%s", this.application.getActiveDevice() == null ? null : this.application.getActiveDevice().getName(), //$NON-NLS-1$
				this.application.getActiveChannelNumber(), this.application.getObjectKey()));
	}

	//	/**
	//	 * determine histo files, read histo recordsets and build / populate the trail recordset.
	//	 * Disregard rebuild steps if histo file paths have changed which may occur if new files have been added by the user or the device, channel or object was modified. 
	//	 * @param rebuildStep
	//	 * @param isWithUi true allows actions on the user interface (progress bar only)
	//	 * @return true if the HistoSet was rebuilt
	//	 */
	//	public boolean rebuild(RebuildStep rebuildStep, boolean isWithUi) {
	//		boolean isRebuilt = false;
	//		log.log(Level.INFO, rebuildStep.toString());
	//		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
	//		if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) application.setProgress(2, sThreadId);
	//
	//		long startTimeFileValid = new Date().getTime();
	//		boolean isHistoFilePathsValid = HistoSet.this.validateHistoFilePaths(rebuildStep);
	//		{
	//			if (!isHistoFilePathsValid) {
	//				if (log.isLoggable(Level.TIME)) log.log(Level.TIME,
	//						String.format("%,5d files    scan folders and select time=%s [ss.SSS]  ::  found files per second:%5d", HistoSet.this.getHistoFilePaths().size(), //$NON-NLS-1$
	//								StringHelper.getFormatedTime("ss.SSS", new Date().getTime() - startTimeFileValid),
	//								HistoSet.this.getHistoFilePaths().size() > 0 ? HistoSet.this.getHistoFilePaths().size() * 1000 / (new Date().getTime() - startTimeFileValid) : 0));
	//			}
	//			else { // histo record sets are ready to use
	//				if (log.isLoggable(Level.TIME)) log.log(Level.TIME,
	//						String.format("files are verified=%d time=%s [ss.SSS]", HistoSet.this.getHistoFilePaths().size(), StringHelper.getFormatedTime("ss.SSS", new Date().getTime() - startTimeFileValid))); //$NON-NLS-1$
	//			}
	//			if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) application.setProgress(7, sThreadId);
	//		}
	//		{
	//			if (RebuildStep.B_HISTORECORDSETS == rebuildStep || !isHistoFilePathsValid) {
	//				isRebuilt = true;
	//				HistoSet.this.initialize();
	//				this.fileSizeSum_B = 0;
	//				if (HistoSet.this.getHistoFilePaths().size() > 0) {
	//					int filesCount = 0;
	//					int progressStart = DataExplorer.application.getProgressPercentage();
	//					double progressCycle = (95 - progressStart) / (double) HistoSet.this.getHistoFilePaths().size();
	//					long nanoTimeHistoRecordSet = System.nanoTime();
	//					for (List<Path> filePaths : HistoSet.this.getHistoFilePaths().values()) {
	//						for (Path filePath : filePaths) {
	//							try {
	//								HistoSet.this.readRecordSets(filePath);
	//							}
	//							catch (IOException | NotSupportedFileFormatException | DataInconsitsentException | DataTypeException e) {
	//								// TODO Auto-generated catch block
	//								e.printStackTrace();
	//							}
	//							this.fileSizeSum_B += filePath.toFile().length();
	//							if (isWithUi) application.setProgress((int) (++filesCount * progressCycle + progressStart), sThreadId);
	//							if (HistoSet.this.size() >= settings.getMaxLogCount())//
	//								break;
	//						}
	//						if (HistoSet.this.size() >= settings.getMaxLogCount())//
	//							break;
	//					}
	//					nanoTimeHistoRecordSet = System.nanoTime() - nanoTimeHistoRecordSet;
	//					if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("files read=%d  recordsets extracted=%d", filesCount, HistoSet.this.size())); //$NON-NLS-1$
	//					if (this.fileSizeSum_B > 0 && log.isLoggable(Level.TIME)) log.log(Level.TIME,
	//							String.format("%,5d recordsets    create from files  time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,5d MB/s", HistoSet.this.size(), TimeUnit.NANOSECONDS.toMillis(nanoTimeHistoRecordSet), //$NON-NLS-1$
	//									HistoSet.this.size() > 0 ? HistoSet.this.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeHistoRecordSet) : 0,
	//									fileSizeSum_B / TimeUnit.NANOSECONDS.toMicros(nanoTimeHistoRecordSet)));
	//				}
	//			}
	//			if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) application.setProgress(95, sThreadId);
	//		}
	//		{
	//			if (EnumSet.of(RebuildStep.B_HISTORECORDSETS, RebuildStep.C_TRAILRECORDSET).contains(rebuildStep) || !isHistoFilePathsValid) {
	//				isRebuilt = true;
	//				long nanoTimeTrailRecordSet = System.nanoTime();
	//				HistoSet.this.trailRecordSet = TrailRecordSet.createRecordSet(this.application.getActiveDevice(), this.application.getActiveChannelNumber());
	//				HistoSet.this.trailRecordSet.defineTrailTypes();
	//				// this.trailRecordSet.checkAllDisplayable();
	//				HistoSet.this.trailRecordSet.clear();
	//				HistoSet.this.trailRecordSet.addHistoSetPoints(this);
	//				HistoSet.this.trailRecordSet.applyTemplate(true); // needs reasonable data
	//				nanoTimeTrailRecordSet = System.nanoTime() - nanoTimeTrailRecordSet;
	//				if (this.fileSizeSum_B > 0 && log.isLoggable(Level.TIME)) log.log(Level.TIME,
	//						String.format("%,5d trails        build and populate time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,5d MB/s", HistoSet.this.size(), TimeUnit.NANOSECONDS.toMillis(nanoTimeTrailRecordSet), //$NON-NLS-1$
	//								HistoSet.this.size() > 0 ? HistoSet.this.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeTrailRecordSet) : 0,
	//								this.fileSizeSum_B / TimeUnit.NANOSECONDS.toMicros(nanoTimeTrailRecordSet)));
	//			}
	//			if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) application.setProgress(97, sThreadId);
	//		}
	//		{
	//			if (EnumSet.of(RebuildStep.D_TRAIL_DATA).contains(rebuildStep)) { // saves some time compared to the logic above
	//				isRebuilt = true;
	//				HistoSet.this.trailRecordSet.clear();
	//				HistoSet.this.trailRecordSet.addHistoSetPoints(this);
	//			}
	//			if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) application.setProgress(98, sThreadId);
	//		}
	//		return isRebuilt;
	//	}

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
	public boolean rebuild(RebuildStep rebuildStep, boolean isWithUi) throws FileNotFoundException, IOException, NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {
		boolean isRebuilt = false;
		log.log(Level.INFO, rebuildStep.toString());
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) application.setProgress(2, sThreadId);

		long startTimeFileValid = new Date().getTime();
		boolean isHistoFilePathsValid = HistoSet.this.validateHistoFilePaths(rebuildStep);
		{
			if (!isHistoFilePathsValid) {
				if (log.isLoggable(Level.TIME)) log.log(Level.TIME,
						String.format("%,5d files     scan folders and select time=%s [ss.SSS]  ::  found files per second:%5d", HistoSet.this.getHistoFilePaths().size(), //$NON-NLS-1$
								StringHelper.getFormatedTime("ss.SSS", new Date().getTime() - startTimeFileValid),
								HistoSet.this.getHistoFilePaths().size() > 0 ? HistoSet.this.getHistoFilePaths().size() * 1000 / (new Date().getTime() - startTimeFileValid) : 0));
			}
			else { // histo record sets are ready to use
				if (log.isLoggable(Level.TIME)) log.log(Level.TIME, String.format("%,5d files    file paths verified     time=%s [ss.SSS]", HistoSet.this.getHistoFilePaths().size(), //$NON-NLS-1$
						StringHelper.getFormatedTime("ss.SSS", new Date().getTime() - startTimeFileValid)));
			}
			if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) application.setProgress(5, sThreadId);
		}
		{
			if (RebuildStep.A_HISTOSET == rebuildStep || !isHistoFilePathsValid) {
				isRebuilt = true;
				long nanoTimeHistoSet = System.nanoTime();
				HistoSet.this.initialize();
				if (log.isLoggable(Level.TIME))
					log.log(Level.TIME, String.format("histoSet             initialize         time=%,6d [ms]", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTimeHistoSet)));
			}
			if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) application.setProgress(7, sThreadId);
		}
		{
			if (EnumSet.of(RebuildStep.A_HISTOSET, RebuildStep.B_HISTORECORDSETS).contains(rebuildStep) || !isHistoFilePathsValid) {
				isRebuilt = true;
				this.fileSizeSum_B = 0;
				if (HistoSet.this.getHistoFilePaths().size() > 0) {
					long nanoTimeReadVaultSum = -System.nanoTime();
					// step: build the workload map consisting of the cache key and the file path
					Map<String, Path> cacheKeyWorkload = getCacheKeyWorkload(this.histoFilePaths);

					// step: put cached vaults into the histoSet map and reduce workload map
					this.fileSizeSum_B += loadVaultsFromCache(cacheKeyWorkload);
					nanoTimeReadVaultSum += System.nanoTime();
					log.log(Level.TIME,
							String.format("%,5d recordsets     load from cache    time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,5d MB/s", HistoSet.this.size(), //$NON-NLS-1$
									TimeUnit.NANOSECONDS.toMillis(nanoTimeReadVaultSum), HistoSet.this.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeReadVaultSum),
									this.fileSizeSum_B / TimeUnit.NANOSECONDS.toMicros(nanoTimeReadVaultSum)));
					int remaining = HistoSet.this.getHistoFilePaths().size() - HistoSet.this.size();
					int progressStart = DataExplorer.application.getProgressPercentage();
					int progressEstimation = progressStart - (int) ((95 - progressStart) * HistoSet.this.size() / (double) (HistoSet.this.size() + 333 * remaining)); // 333 is the estimated processing time ratio between reading from files and reading from cache
					if (isWithUi) application.setProgress(progressEstimation, sThreadId);
					double progressCycle = (95 - progressEstimation) / (double) remaining;

					// step: transform log files from workload map into vaults and put them into the histoSet map
					int filesCounter = 0, recordSetCounter = HistoSet.this.size();
					long nanoTimeReadRecordSetSum = -System.nanoTime(), filesSize_B = this.fileSizeSum_B;
					// get set in the same order but without path duplicates arising from files with multiple recordsets
					LinkedHashSet<Path> workloadPaths = new LinkedHashSet<Path>(cacheKeyWorkload.values());
					for (Path filePath : workloadPaths) {
						this.fileSizeSum_B += readVaultsFromFile(filePath);
						if (isWithUi) application.setProgress((int) (++filesCounter * progressCycle + progressStart), sThreadId);
						if (HistoSet.this.size() >= settings.getMaxLogCount()) //
							break;
					}
					nanoTimeReadRecordSetSum += System.nanoTime();

					// step: save vaults on the file system
					long nanoTimeWriteVaultSum = -System.nanoTime(), cacheSize_B = 0;
					if (this.fileSizeSum_B > filesSize_B) {
						cacheSize_B = storeVaultsInCache();
					}
					nanoTimeWriteVaultSum += System.nanoTime();
					log.log(Level.TIME,
							String.format("%,5d recordsets     create from files  time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,5d MB/s", HistoSet.this.size() - recordSetCounter, //$NON-NLS-1$
									TimeUnit.NANOSECONDS.toMillis(nanoTimeReadRecordSetSum), (HistoSet.this.size() - recordSetCounter) * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeReadRecordSetSum),
									(this.fileSizeSum_B - filesSize_B) / TimeUnit.NANOSECONDS.toMicros(nanoTimeReadRecordSetSum)));
					log.log(Level.TIME,
							String.format("%,5d recordsets     store in cache     time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,5d MB/s  ::  cache to log file size ratio=%d per million", HistoSet.this.size(), //$NON-NLS-1$
									TimeUnit.NANOSECONDS.toMillis(nanoTimeWriteVaultSum), HistoSet.this.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeWriteVaultSum),
									this.fileSizeSum_B / TimeUnit.NANOSECONDS.toMicros(nanoTimeWriteVaultSum), cacheSize_B * 1000000 / this.fileSizeSum_B));
					log.log(Level.TIME,
							String.format("%,5d recordsets     total              time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,5d MB/s", HistoSet.this.size(), //$NON-NLS-1$
									TimeUnit.NANOSECONDS.toMillis(nanoTimeReadVaultSum + nanoTimeReadRecordSetSum + nanoTimeWriteVaultSum),
									HistoSet.this.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeReadVaultSum + nanoTimeReadRecordSetSum + nanoTimeWriteVaultSum),
									this.fileSizeSum_B / TimeUnit.NANOSECONDS.toMicros(nanoTimeReadVaultSum + nanoTimeReadRecordSetSum + nanoTimeWriteVaultSum)));
				}
			}
			if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) application.setProgress(95, sThreadId);
		}
		{
			if (EnumSet.of(RebuildStep.A_HISTOSET, RebuildStep.B_HISTORECORDSETS, RebuildStep.C_TRAILRECORDSET).contains(rebuildStep) || !isHistoFilePathsValid) {
				isRebuilt = true;
				long nanoTimeTrailRecordSet = -System.nanoTime();
				HistoSet.this.trailRecordSet = TrailRecordSet.createRecordSet(this.application.getActiveDevice(), this.application.getActiveChannelNumber());
				HistoSet.this.trailRecordSet.defineTrailTypes();
				// this.trailRecordSet.checkAllDisplayable();
				HistoSet.this.trailRecordSet.setPoints();
				HistoSet.this.trailRecordSet.applyTemplate(true); // needs reasonable data
				nanoTimeTrailRecordSet += System.nanoTime();
				if (this.fileSizeSum_B > 0 && log.isLoggable(Level.TIME)) log.log(Level.TIME,
						String.format("%,5d trailTimeSteps build and populate time=%,6d [ms]  ::  per second:%5d  ::  Rate=%,5d MB/s", HistoSet.this.size(), TimeUnit.NANOSECONDS.toMillis(nanoTimeTrailRecordSet), //$NON-NLS-1$
								HistoSet.this.size() > 0 ? HistoSet.this.size() * 1000 / TimeUnit.NANOSECONDS.toMillis(nanoTimeTrailRecordSet) : 0,
								this.fileSizeSum_B / TimeUnit.NANOSECONDS.toMicros(nanoTimeTrailRecordSet)));
			}
			if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) application.setProgress(97, sThreadId);
		}
		{
			if (EnumSet.of(RebuildStep.D_TRAIL_DATA).contains(rebuildStep)) { // saves some time compared to the logic above
				isRebuilt = true;
				HistoSet.this.trailRecordSet.cleanup();
				HistoSet.this.trailRecordSet.setPoints();
			}
			if (isWithUi && rebuildStep.scopeOfWork > RebuildStep.D_TRAIL_DATA.scopeOfWork) application.setProgress(98, sThreadId);
		}
		return isRebuilt;
	}

	/**
	 * read file and populate vaults from the histo recordsets.
	 * put the vaults into the histoset map.
	 * @param filePath
	 * @throws DataTypeException 
	 * @throws DataInconsitsentException 
	 * @throws NotSupportedFileFormatException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	private long readVaultsFromFile(Path filePath) throws FileNotFoundException, IOException, NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {
		long fileSizeSum_B = 0;
		// populate history record sets from the file
		long nanoTime = System.nanoTime();
		List<HistoRecordSet> histoRecordSets = HistoSet.this.readRecordSets(filePath);

		for (HistoRecordSet histoRecordSet : histoRecordSets) {
			if (histoRecordSet.getStartTimeStamp() > LocalDate.now().minusMonths(settings.getRetrospectMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()) {
				// prepare historecordset and histoset for history vault
				histoRecordSet.setElapsedHistoRecordSet_ns((System.nanoTime() - nanoTime) / histoRecordSets.size());
				boolean alreadyExists = false;
				if (this.containsKey(histoRecordSet.getStartTimeStamp())) {
					// some devices create multiple recordsets which might have identical timestamp values
					for (HistoVault histoVault : this.get(histoRecordSet.getStartTimeStamp())) {
						if (histoVault.getUiChannelNumber() == histoRecordSet.getChannelConfigNumber() && histoVault.getLogChannelNumber() == histoRecordSet.getChannelConfigNumber() // 
								&& histoVault.getUiDeviceName() == histoRecordSet.getDevice().getName()) {
							if (log.isLoggable(Level.WARNING))
								log.log(Level.WARNING, String.format("duplicate histo recordSet was discarded: device = %s  channelConfigNumber = %d  timestamp = %,d  histoFilePath = %s", //$NON-NLS-1$
										histoRecordSet.getDevice().getName(), histoRecordSet.getChannelConfigNumber(), histoRecordSet.getStartTimeStamp(), filePath));
							alreadyExists = true;
							break;
						}
					}
					if (!alreadyExists) {
						if (log.isLoggable(Level.INFO)) log.log(Level.WARNING,
								String.format("WARNING  startTimeStamp=%s :: different recordSet with identical start time was added : %s   device = %s  channelConfigNumber = %d", //$NON-NLS-1$
										StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", histoRecordSet.getStartTimeStamp()), filePath, histoRecordSet.getDevice().getName(),
										histoRecordSet.getChannelConfigNumber()));
					}
				}
				else {
					this.put(histoRecordSet.getStartTimeStamp(), new ArrayList<HistoVault>());
				}
				// put all aggregated data and scores into the history vault and put the vault into the histoSet map
				if (!alreadyExists) {
					this.get(histoRecordSet.getStartTimeStamp()).add(histoRecordSet.getHistoVault(filePath));
					fileSizeSum_B += filePath.toFile().length() / histoRecordSets.size();
				}
				// sum up the file length and reduce memory consumption in advance to the garbage collection
				histoRecordSet.cleanup();
			}
		}
		return fileSizeSum_B;
	}

	/**
	 * put cached vaults into the histoSet map and reduce workload map.
	 * @param cacheKeyWorkload cache key and the file path of the log source file
	 * @return total length (bytes) of the original log files which were the sources of those vaults which were put into the histoset 
	 * @throws IOException origin is opening or traversing the zip file
	 */
	private long loadVaultsFromCache(Map<String, Path> cacheKeyWorkload) throws IOException {
		long fileSizeSum_B = 0;
		Path cacheFilePath = Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_CACHE_ENTRIES_DIR_NAME).resolve(HistoVault.getVaultSubDirectory());
		if (FileUtils.checkFileExist(cacheFilePath.toString())) {
			try (ZipFile zf = new ZipFile(cacheFilePath.toFile())) { // closing the zip file closes all streams
				while (zf.entries().hasMoreElements() && HistoSet.this.size() <= settings.getMaxLogCount()) {
					ZipEntry histoVaultEntry = zf.entries().nextElement();
					if (cacheKeyWorkload.containsKey(histoVaultEntry.getName())) {
						HistoVault histoVault = HistoVault.load(zf.getInputStream(histoVaultEntry));
						if (log.isLoggable(Level.SEVERE)) log.log(Level.SEVERE, String.format("%s  %s", histoVault.getCacheKey(), cacheFilePath.toString()));
						if (histoVault.getLogStartTimestamp_ms() > LocalDate.now().minusMonths(settings.getRetrospectMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()) {
							if (this.containsKey(histoVault.getLogStartTimestamp_ms())) {
								log.log(Level.WARNING, String.format("WARNING  startTimeStamp=%s :: recordSet with identical start time was added : %s", //$NON-NLS-1$
										StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", histoVault.getLogStartTimestamp_ms()), Paths.get(histoVault.getFilePath()).getFileName().toString()));
							}
							else {
								this.put(histoVault.getLogStartTimestamp_ms(), new ArrayList<HistoVault>());
							}
							// put the vault into the histoSet map
							this.get(histoVault.getLogStartTimestamp_ms()).add(histoVault);
							// sum up the file length and reduce workload map
							fileSizeSum_B += cacheKeyWorkload.get(histoVaultEntry.getName()).toFile().length();
							cacheKeyWorkload.remove(histoVaultEntry.getName());
						}
					}
				}
			}
		}
		return fileSizeSum_B;
	}

	/**
	 * get the zip file name from the history vault class and store all histoset vaults into this file.
	 * overwrites an existing file.
	 * @return cache file bytes length
	 * @throws IOException 
	 */
	private long storeVaultsInCache() throws IOException {
		Path cacheFilePath = Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_CACHE_ENTRIES_DIR_NAME).resolve(HistoVault.getVaultSubDirectory());
		FileUtils.cleanFile(cacheFilePath.toString());

		try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFilePath.toString())))) {
			for (Map.Entry<Long, List<HistoVault>> listEntry : this.entrySet()) {
				for (HistoVault histoVault : listEntry.getValue()) {
					// name the file inside the zip  file 
					zipOutputStream.putNextEntry(new ZipEntry(histoVault.getCacheKey()));
					histoVault.store(zipOutputStream);
					if (log.isLoggable(Level.SEVERE)) log.log(Level.SEVERE, String.format("%s  %s", histoVault.getCacheKey(), cacheFilePath.toString()));
				}
			}
			zipOutputStream.flush();
		}
		return cacheFilePath.toFile().length();
	}

	/**
	* builds recordsets from the file for all file types.
	* @param histoFile
	* @return record sets found in the file which fit to the device, the channel and the object key 
	* @throws DataInconsitsentException 
	* @throws NotSupportedFileFormatException 
	* @throws IOException 
	* @throws FileNotFoundException 
	* @throws DataTypeException origin is bin file reader only
	*/
	private List<HistoRecordSet> readRecordSets(Path histoFile) throws DataInconsitsentException, IOException, DataTypeException, NotSupportedFileFormatException {
		List<HistoRecordSet> histoRecordSets = new ArrayList<>();
		long nanoTime = System.nanoTime();
		if (histoFile.toString().endsWith(GDE.FILE_ENDING_DOT_BIN)) {
			HistoRecordSet recordSet = ((IHistoDevice) this.application.getActiveDevice()).getRecordSetFromImportFile(this.application.getActiveChannelNumber(), histoFile);
			if (recordSet.getRecordDataSize(true) > 0) {
				histoRecordSets.add(recordSet);
			}
		}
		else if (histoFile.toString().endsWith(GDE.FILE_ENDING_DOT_OSD)) {
			HistoOsdReaderWriter.readHisto(histoRecordSets, histoFile);
		}
		else {
			log.log(Level.SEVERE, String.format("file format not supported: device = %s  channelConfigNumber = %d  histoFilePath = %s", //$NON-NLS-1$
					this.application.getActiveDevice().getName(), this.application.getActiveChannelNumber(), histoFile));
		}
		long nanoTimeSpan = histoRecordSets.size() > 0 ? (System.nanoTime() - nanoTime) / histoRecordSets.size() : 0;

		for (HistoRecordSet histoRecordSet : histoRecordSets) {
			histoRecordSet.addSettlements();
			histoRecordSet.setElapsedHistoRecordSet_ns(nanoTimeSpan / histoRecordSets.size());
		}
		return histoRecordSets;
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
		IDevice device = this.application.getActiveDevice();
		Channel channel = this.application.getActiveChannel();

		Path histoDataDir = getHistoDataDir();
		Path histoImportDir = settings.getSearchImportPath() ? getHistoImportDir() : null;
		String importFileExtention = settings.getSearchImportPath() ? getImportFileExtention() : null;
		boolean isFullChange = rebuildStep == RebuildStep.A_HISTOSET || this.histoFilePaths.size() == 0;
		isFullChange = isFullChange || (lastDevice != null ? !lastDevice.getName().equals(device.getName()) : device != null);
		isFullChange = isFullChange || (lastChannel != null ? !lastChannel.channelConfigName.equals(channel.channelConfigName) : channel != null);
		isFullChange = isFullChange || (lastHistoDataDir != null ? !lastHistoDataDir.equals(histoDataDir) : histoDataDir != null);
		isFullChange = isFullChange || (lastHistoImportDir != null ? !lastHistoImportDir.equals(histoImportDir) : histoImportDir != null);
		isFullChange = isFullChange || (lastImportFileExtention != null ? !lastImportFileExtention.equals(importFileExtention) : importFileExtention != null);

		if (isFullChange) {
			if (histoDataDir != null) setHistoFilePaths(histoDataDir, histoImportDir);
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("isFullChange %s", isFullChange)); //$NON-NLS-1$

		lastDevice = device;
		lastChannel = channel;
		lastHistoDataDir = histoDataDir;
		lastHistoImportDir = histoImportDir;
		lastImportFileExtention = importFileExtention;
		return !isFullChange;
	}

	/**
	 * @return device path or object path (in case an object key is selected) after getting the parent path if a device or object path is selected
	 */
	private Path getHistoDataDir() {
		Path path = null;
		String tmpHistoDataDirPath = this.settings.getDataFilePath();
		if (!(tmpHistoDataDirPath == null || tmpHistoDataDirPath.trim().isEmpty() || tmpHistoDataDirPath.equals(GDE.FILE_SEPARATOR_UNIX))) {
			path = Paths.get(tmpHistoDataDirPath);
			if (path.getFileName().toString().equalsIgnoreCase(this.application.getActiveDevice().getName())) {
				path = path.getParent();
			}
			else {
				for (String objectKeyTmp : this.settings.getObjectList()) {
					if (path.getFileName().toString().equalsIgnoreCase(objectKeyTmp)) {
						path = path.getParent();
						break;
					}
				}
			}
			if (this.application.getActiveObject() == null) {
				path = path.resolve(this.application.getActiveDevice().getName());
			}
			else {
				path = path.resolve(this.application.getObjectKey());
			}
		}
		log.log(Level.INFO, "histoDataDir " + path); //$NON-NLS-1$
		return path;
	}

	/**
	 * @return import path or object path (in case an object key is selected) after getting the parent path if an object path is selected
	 */
	private Path getHistoImportDir() {
		Path path = null;
		String tmpHistoImportDirPath = this.application.getActiveDevice() != null ? this.application.getActiveDevice().getDeviceConfiguration().getDataBlockType().getPreferredDataLocation() : null;
		if (tmpHistoImportDirPath != null && !tmpHistoImportDirPath.trim().isEmpty()) {
			path = Paths.get(tmpHistoImportDirPath);
			// do not look for a device directory because 
			for (String objectKeyTmp : this.settings.getObjectList()) {
				if (path.getFileName().toString().equalsIgnoreCase(objectKeyTmp)) {
					path = path.getParent();
					break;
				}
			}
			path = path.resolve(this.application.getObjectKey());
		}
		log.log(Level.INFO, "histoImportDir " + path); //$NON-NLS-1$
		return path;
	}

	private String getImportFileExtention() {
		String tmpImportFileExtention = this.application.getActiveDevice() != null ? this.application.getActiveDevice().getDeviceConfiguration().getDataBlockType().getPreferredFileExtention()
				: GDE.STRING_EMPTY;
		return tmpImportFileExtention != null ? tmpImportFileExtention : GDE.STRING_EMPTY;
	}

	/**
	*  selects histo file candidates from file paths including one subdirectory level and sets the histo filepaths list.
	 * @param histoDataDirPath must not be null
	 * @param histoImportDirPath may be null
	 * @throws FileNotFoundException 
	 */
	private void setHistoFilePaths(Path histoDataDirPath, Path histoImportDirPath) throws FileNotFoundException {
		final int subDirLevelMax = 1;
		this.histoFilePaths.clear();
		{
			FileUtils.checkDirectoryAndCreate(histoDataDirPath.toString());
			List<File> files = FileUtils.getFileListing(histoDataDirPath.toFile(), subDirLevelMax);
			log.log(Level.INFO, String.format("%04d files found in histoDataDir %s", files.size(), histoDataDirPath)); //$NON-NLS-1$
			if (settings.getSearchDataPathImports() && !getImportFileExtention().equals(GDE.STRING_EMPTY)) {
				for (File file : files) {
					if (file.getName().endsWith(GDE.FILE_ENDING_OSD) || file.getName().endsWith(getImportFileExtention())) {
						if (!this.histoFilePaths.containsKey(file.lastModified())) this.histoFilePaths.put(file.lastModified(), new ArrayList<Path>());
						this.histoFilePaths.get(file.lastModified()).add(file.toPath());
					}
				}
			}
			else {
				for (File file : files) {
					if (file.getName().endsWith(GDE.FILE_ENDING_OSD)) {
						if (!this.histoFilePaths.containsKey(file.lastModified())) this.histoFilePaths.put(file.lastModified(), new ArrayList<Path>());
						this.histoFilePaths.get(file.lastModified()).add(file.toPath());
					}
				}
			}
		}
		if (histoImportDirPath != null && settings.getSearchImportPath() && !getImportFileExtention().equals(GDE.STRING_EMPTY)) {
			FileUtils.checkDirectoryAndCreate(histoImportDirPath.toString());
			List<File> files = FileUtils.getFileListing(histoImportDirPath.toFile(), subDirLevelMax);
			log.log(Level.INFO, String.format("%04d files found in histoImportDir %s", files.size(), histoImportDirPath)); //$NON-NLS-1$
			for (File file : files) {
				if (file.getName().endsWith(getImportFileExtention())) {
					if (!this.histoFilePaths.containsKey(file.lastModified())) this.histoFilePaths.put(file.lastModified(), new ArrayList<Path>());
					this.histoFilePaths.get(file.lastModified()).add(file.toPath());
				}
			}
		}
		log.log(Level.INFO, String.format("%04d files selected", this.histoFilePaths.size())); //$NON-NLS-1$
	}

	/**
	* reads histo paths files and selects histo file candidates for the active device and objectKey and updates the histo files list. <<<<<<<<<<<<<<<<<<<<<<<
	 * bin files can not be checked for a matching object key.
	* @param histoPaths1 false: omits the files search for performance reasons, takes the file list from the last call.
	 * @return map with cache keys and paths copied from the current list of histoFilePaths with identical order ('lastModified' descending)
	 * @throws IOException 
	 * @throws NotSupportedFileFormatException 
	*/
	private LinkedHashMap<String, Path> getCacheKeyWorkload(Map<Long, List<Path>> histoPaths1) throws IOException, NotSupportedFileFormatException {
		LinkedHashMap<String, Path> cacheKeyWorkload = new LinkedHashMap<String, Path>();
		final int binFileRecordSetNumber = 1;

		for (Map.Entry<Long, List<Path>> pathListEntry : histoPaths1.entrySet()) {
			for (Path path : pathListEntry.getValue()) {
				File file = path.toFile();
				if (file.getName().endsWith(GDE.FILE_ENDING_OSD)) {
					long startMillis = System.currentTimeMillis();
					Path actualPath = FileSystems.getDefault().getPath(OperatingSystemHelper.getLinkContainedFilePath(file.getAbsolutePath()));
					// getLinkContainedFilePath may have long response times in case of an unavailable network resources
					// This is a workaround: Much better solution would be a function 'getLinkContainedFilePathWithoutAccessingTheLinkedFile'
					if (file.equals(actualPath) && (System.currentTimeMillis() - startMillis > 555)) {
						log.log(Level.FINER, "Dead OSD link " + file.getAbsolutePath() + " pointing to " + actualPath); //$NON-NLS-1$ //$NON-NLS-2$
						if (!file.delete()) {
							log.log(Level.FINE, "could not delete " + file.getName()); //$NON-NLS-1$
						}
					}
					else {
						boolean isValidFile = false;
						HashMap<String, String> fileHeader = HistoOsdReaderWriter.getHeader(actualPath.toString());
						if (this.application.getActiveDevice() != null && !fileHeader.get(GDE.DEVICE_NAME).equals(this.application.getActiveDevice().getName())) {
							log.log(Level.WARNING, String.format("OSD candidate found for wrong device \"%s\" in %s %s %s   %s %s", fileHeader.get(GDE.DEVICE_NAME), //$NON-NLS-1$
									actualPath, GDE.CREATION_TIME_STAMP, fileHeader.get(GDE.CREATION_TIME_STAMP), GDE.DATA_EXPLORER_FILE_VERSION, fileHeader.get(GDE.DATA_EXPLORER_FILE_VERSION)));
							if (SWT.OK != application.openOkCancelMessageDialog(Messages.getString(MessageIds.GDE_MSGI0063, new String[] { fileHeader.get(GDE.DEVICE_NAME), actualPath.toString() }))) {
								continue; // ignore file
							}
							else {
								break; // leave files loop
							}
						}
						if (this.application.getActiveObject() != null && fileHeader.get(GDE.OBJECT_KEY).equals(GDE.STRING_EMPTY)) {
							log.log(Level.WARNING, String.format("OSD candidate found for empty object \"%s\" in %s %s %s   %s %s", fileHeader.get(GDE.OBJECT_KEY), //$NON-NLS-1$
									actualPath, GDE.CREATION_TIME_STAMP, fileHeader.get(GDE.CREATION_TIME_STAMP), GDE.DATA_EXPLORER_FILE_VERSION, fileHeader.get(GDE.DATA_EXPLORER_FILE_VERSION)));
							if (!settings.skipFilesWithoutObject() || SWT.YES == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0065, new String[] { actualPath.toString() }))) {
								isValidFile = true;
								settings.setFilesWithoutObject(true);
							}
						}
						else if (this.application.getActiveObject() != null && !fileHeader.get(GDE.OBJECT_KEY).equalsIgnoreCase(this.application.getObjectKey())) {
							log.log(Level.WARNING, String.format("OSD candidate found for wrong object \"%s\" in %s %s %s   %s %s", fileHeader.get(GDE.OBJECT_KEY), //$NON-NLS-1$
									actualPath, GDE.CREATION_TIME_STAMP, fileHeader.get(GDE.CREATION_TIME_STAMP), GDE.DATA_EXPLORER_FILE_VERSION, fileHeader.get(GDE.DATA_EXPLORER_FILE_VERSION)));
							if (!settings.skipFilesWithOtherObject()
									|| SWT.YES == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0062, new String[] { fileHeader.get(GDE.OBJECT_KEY), actualPath.toString() }))) {
								isValidFile = true;
								settings.setFilesWithOtherObject(true);
							}
						}
						else if (this.application.getActiveObject() == null || fileHeader.get(GDE.OBJECT_KEY).equalsIgnoreCase(this.application.getObjectKey())) {
							log.log(Level.FINER,
									String.format("OSD candidate found for object       \"%s\" in %s %s %s   %s %s", //$NON-NLS-1$
											(this.application.getObjectKey()), actualPath, GDE.CREATION_TIME_STAMP, fileHeader.get(GDE.CREATION_TIME_STAMP), GDE.DATA_EXPLORER_FILE_VERSION,
											fileHeader.get(GDE.DATA_EXPLORER_FILE_VERSION)));
							isValidFile = true;
						}
						else { // ignore file
						}
						if (isValidFile) {
							if (!this.histoFilePaths.containsKey(file.lastModified())) this.histoFilePaths.put(file.lastModified(), new ArrayList<Path>());
							this.histoFilePaths.get(file.lastModified()).add(file.toPath());
							for (java.util.Map.Entry<String, String> headerEntry : fileHeader.entrySet()) {
								if (headerEntry.getKey().contains(GDE.RECORD_SET_NAME)) {
									String recordSetName = headerEntry.getKey().split(GDE.RECORD_SET_NAME)[1];
									int recordSetNumber = Integer.parseInt(recordSetName.split(GDE.STRING_RIGHT_PARENTHESIS_BLANK)[0]);
									cacheKeyWorkload.put(HistoVault.getCacheKey(actualPath, actualPath.toFile().lastModified(), recordSetNumber).toString(), actualPath);
									if (log.isLoggable(Level.FINER))
										log.log(Level.FINER, String.format("%s  %s", HistoVault.getCacheKey(actualPath, actualPath.toFile().lastModified(), recordSetNumber).toString(), actualPath.toString()));
								}
							}
						}
					}
				}
				else if (file.getName().endsWith(GDE.FILE_ENDING_DOT_BIN)) {
					if (!this.histoFilePaths.containsKey(file.lastModified())) this.histoFilePaths.put(file.lastModified(), new ArrayList<Path>());
					this.histoFilePaths.get(file.lastModified()).add(file.toPath());
					cacheKeyWorkload.put(HistoVault.getCacheKey(file.toPath(), file.lastModified(), binFileRecordSetNumber).toString(), file.toPath()); // recordSetNumber is 1
				}
			}
		}
		log.log(Level.INFO, String.format("%04d files taken from histoDirectories", this.histoFilePaths.size())); //$NON-NLS-1$
		return cacheKeyWorkload;
	}

	/**
	 * @return all the paths which have been identified in the last validation sorted by lastModified in reverse order
	 */
	public Map<Long, List<Path>> getHistoFilePaths() {
		return this.histoFilePaths;
	}

	public void clearMeasurementModes() {
		// todo Auto-generated method stub -> copy from RecordSet
		throw new UnsupportedOperationException();
	}

	public TrailRecordSet getTrailRecordSet() {
		return this.trailRecordSet;
	}

	public long getFileSizeSum_B() {
		return fileSizeSum_B;
	}

}
