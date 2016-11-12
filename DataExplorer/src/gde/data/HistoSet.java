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

import gde.GDE;
import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.device.IHistoDevice;
import gde.device.MeasurementType;
import gde.device.SettlementType;
import gde.exception.NotSupportedFileFormatException;
import gde.io.HistoOsdReaderWriter;
import gde.io.OsdReaderWriter;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.OperatingSystemHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;

/**
 * holds current selection of histo recordSets for the selected channel.
 * sorted by timestamp in reverse order; each timestamp may hold multiple recordSets.
 * @author Thomas Eickert
 */
public class HistoSet extends TreeMap<Long, List<HistoRecordSet>> {
	final static String												$CLASS_NAME					= HistoSet.class.getName();
	private static final long									serialVersionUID		= 1111377035274863787L;
	final static Logger												log									= Logger.getLogger($CLASS_NAME);

	private final DataExplorer								application					= DataExplorer.getInstance();
	private final Settings										settings						= Settings.getInstance();

	private HashMap<Integer, MeasurementType>	measurements				= new HashMap<Integer, MeasurementType>();
	private String														deviceName					= null;
	private String														objectKey						= null;
	private int																channelNumber				= -Integer.MAX_VALUE;
	private String														histoDataDirPath		= null;
	//TODO private String														histoImportDirPath	= null;																		// TODO for bin files histo

	private TreeMap<Date, String>							histoFilePaths;																								// current paths to the histo files as evaluated during the last check
	private TreeMap<Date, String>							lastCheckFilesPaths;																					// to be able to detect changes: paths to the histo files as evaluated during the last check

	private TrailRecordSet										trailRecordSet			= null;																		// histo data transformed in a recordset format
	private static HistoSet										histoSet						= null;

	public static HistoSet getInstance() {
		if (HistoSet.histoSet == null) 
			 HistoSet.histoSet = new HistoSet();
		return HistoSet.histoSet;
	}
	
	private HistoSet() {
		super(Collections.reverseOrder());
	}

//	/**
//	 * instantiates the singleton and initializes it.
//	 */
//	public static void resetFully() {
//		histoSet = new HistoSet();
//		histoSet.initialize();
//	}

	/**
	 * re- initializes the singleton. 
	 */
	public void initialize() {
		this.clear();
		this.trailRecordSet = null;
		//this.buildTrail(); // initialize to make sure != null, replace 
		if (this.channelNumber == -Integer.MAX_VALUE) { // this is only the case during first initialization or after resetFully
			this.deviceName = this.application.getActiveDevice() == null ? null : this.application.getActiveDevice().getName();
			this.objectKey = this.application.getActiveObject() == null ? null : this.application.getActiveObject().getKey();
			this.channelNumber = this.application.getActiveChannelNumber();
		}
		this.measurements = new HashMap<Integer, MeasurementType>();
		if (this.application.getActiveDevice() != null) {
			for (int i = 0; i < this.application.getActiveDevice().getNumberOfMeasurements(this.channelNumber); i++) {
				this.measurements.put(i, this.application.getActiveDevice().getMeasurement(this.channelNumber, i).clone());
			}
		}
		log.log(Level.OFF, String.format("device = %s  channel = %d  objectKey = %s", this.deviceName, this.channelNumber, this.objectKey)); //$NON-NLS-1$
	}

	/**
	 * add new measurement to the list. 
	 * the measurement list corresponds to the selector in the histoGraphics window.
	 * @param position The index at which the specified element is to be inserted.
	 * @param measurementName
	 * @param measurementSymbol
	 * @param measurementUnit
	 */
	public void addMeasurement(String measurementName, String measurementSymbol, String measurementUnit) {
		log.log(Level.FINE, "addMeasurement " + measurementName); //$NON-NLS-1$
		MeasurementType measurement = new MeasurementType();
		measurement.setName(measurementName);
		measurement.setSymbol(measurementSymbol);
		measurement.setUnit(measurementUnit);
		measurement.setActive(true);
		// TODO setStatistics
		// TODO setProperties
		this.measurements.put(this.measurements.size(), measurement);
	}

	/**
	 * browse all record sets for measurement data.
	 * @param recordOrdinal
	 * @return the first array element is the measurement name which is followed by the measurement values from all histo record sets.
	 */
	public String[] getTableRow(int recordOrdinal) {
		StringBuilder sb = new StringBuilder();
		List<String> result = new ArrayList<String>();
		MeasurementType measurementType = this.getMeasurements().get(recordOrdinal);
		sb.append(measurementType.getName()).append(GDE.STRING_BLANK);
		sb.append(GDE.STRING_BLANK_LEFT_BRACKET).append(measurementType.getUnit()).append(GDE.STRING_RIGHT_BRACKET);
		result.add(sb.toString());
		for (List<HistoRecordSet> histoRecordSets : this.values()) {
			for (HistoRecordSet histoRecordSet : histoRecordSets) {
				if (histoRecordSet.realSize()-1 > recordOrdinal) {
					Record record = histoRecordSet.get(recordOrdinal);
					double value = this.application.getActiveDevice().translateValue(record, record.getAvgValue()) / 1000.;
					result.add(record.getDecimalFormat().format(value));
				}
			}
		}
		return result.toArray(new String[result.size()]);
	}

	/**
	 * build a new trail structure.
	 * the trails data structure fits more closely to the graphics algorithms. 
	 */
	public void buildTrail() {
		this.trailRecordSet = TrailRecordSet.createRecordSet(this.application.getActiveDevice(), this.channelNumber);
		this.trailRecordSet.defineTrailTypes();
		// me.trailRecordSet.checkAllDisplayable();
	}

	/**
	 * copy the applicable aggregate values from the list of histo recordsets into the trail structure.
	 */
	public void populateTrail() {
		this.trailRecordSet.addHisto(this);
		this.trailRecordSet.applyTemplate(true); // needs reasonable data
	}

	/**
	 * builds recordsets from the file and adds settlements to them.
	 * skips those which exist in the histo set already.
	 * @param histoFilePath
	 * @return record sets found in the file which fit to the device, the channel and the object key 
	 */
	public List<HistoRecordSet> readRecordSets(String histoFilePath) {
		List<HistoRecordSet> recordSets = null;
		try {
			recordSets = HistoOsdReaderWriter.readHisto(histoFilePath);
			for (HistoRecordSet recordSet : recordSets) {
				// some devices create multiple recordsets which may have identical timestamp values
				if (this.containsKey(recordSet.getStartTimeStamp())) {
					boolean alreadyExists = false;
					for (HistoRecordSet myHistoRecordSet : this.get(recordSet.getStartTimeStamp())) {
						if (myHistoRecordSet.getChannelConfigNumber() == recordSet.getChannelConfigNumber() &&
								myHistoRecordSet.getDevice().getName() == recordSet.getDevice().getName()) {
							if (log.isLoggable(Level.FINE))
								log.log(Level.FINE, String.format("duplicate histo recordSet was discarded: device = %s  channelConfigNumber = %d  timestamp = %,d  histoFilePath = %s", recordSet.getDevice().getName(), recordSet.getChannelConfigNumber(), recordSet.getStartTimeStamp(), histoFilePath)); //$NON-NLS-1$
							alreadyExists = true;
							break;
						}
					}
					if (!alreadyExists) {
						if (log.isLoggable(Level.INFO))
							log.log(Level.INFO, String.format("different histo recordSet with identical timestamp: device = %s  channelConfigNumber = %d  timestamp = %,d  histoFilePath = %s", recordSet.getDevice().getName(), recordSet.getChannelConfigNumber(), recordSet.getStartTimeStamp(), histoFilePath)); //$NON-NLS-1$
						this.get(recordSet.getStartTimeStamp()).add(recordSet);
					}
				} else {
					ArrayList<HistoRecordSet> arrayList = new ArrayList<HistoRecordSet>();
					arrayList.add(recordSet);
					this.put(recordSet.getStartTimeStamp(), arrayList);
				}
				recordSet.addEvaluationSettlements();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return recordSets;
	}

	/**
	 * builds recordsets from the file and adds settlements to them.
	 * skips those which exist in the histo set already.
	 * new version with access to bin files. 
	 * @param histoFilePath
	 * @return record sets found in the file which fit to the device, the channel and the object key 
	 */
	public List<HistoRecordSet> readRecordSets4Bin(String histoFilePath) {
		List<HistoRecordSet> recordSets = null;
		// see a bunch of device construction code lines at gde.ui.dialog.DeviceSelectionDialog.getInstanceOfDevice()
		IHistoDevice histoDevice = (IHistoDevice) this.application.getActiveDevice();
			RecordSet recordSet = HistoRecordSet.createRecordSet("test", application.getActiveDevice(), application.getActiveChannelNumber(), true, true);
			try {
				histoDevice.addBinFileAsRawDataPoints(recordSet, "C:\\_Thomas\\Eigene Dokumente\\2016\\2016_Fliegen\\LogData\\FS14\\0213_2016-5-28.bin");
				recordSet = HistoRecordSet.createRecordSet("test", application.getActiveDevice(), application.getActiveChannelNumber(), true, true);
				histoDevice.addBinFileAsRawDataPoints(recordSet, "C:\\_Thomas\\Eigene Dokumente\\2016\\2016_Fliegen\\LogData\\FS14\\0210_2016-5-25.bin");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			}
		return recordSets;
	}

	/**
	 * decide about validity via comparison against the last call of this method.
	 * determine files from an input directory which fit to the objectKey, the device, the channel and the file extensions.
	 * resets the histoset singleton if the trail is invalid.
	 * @param doUpdateProgressBar
	 * @return true: trail data are valid 
	 */
	public boolean validateHistoFilePaths(boolean doUpdateProgressBar) {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		if (doUpdateProgressBar)
			this.application.setProgress(2, sThreadId);

		boolean isChannelChanged = detectChannelChange();
		boolean isFolderChanged = detectFolderChange(false);
		boolean isInvalid = isChannelChanged | isFolderChanged;
		if (doUpdateProgressBar)
			this.application.setProgress(5, sThreadId);
		boolean isFilesChanged = detectFilesChange(isFolderChanged);
		if (isFilesChanged && !isInvalid) {
			isInvalid = SWT.YES == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0064)); // ask user if he wants new files added into the history
		}
		if (isInvalid)
			this.initialize();

		if (doUpdateProgressBar)
			this.application.setProgress(7, sThreadId);
		if (log.isLoggable(Level.OFF))
			log.log(Level.OFF, String.format("isChannelChanged %s  isFolderChanged %s  isFilesChanged %s  isInvalid %s", isChannelChanged, isFolderChanged, isFilesChanged, isInvalid)); //$NON-NLS-1$
		return !isInvalid;
	}

	/**
	 * comparison against the last call of this method.
	 * determine input directory which fits to the objectKey, the device and the file extensions.
	 * @param reBuild true perform full check even if neither the device nor the object key was changed 
	 * @return true if the histoDataDirPath was reinitialized. False if no activity is required.
	 */
	private boolean detectChannelChange() {
		int tmpChannelNumber = this.application.getActiveChannelNumber();
		boolean hasChanged = tmpChannelNumber != this.channelNumber;
		this.channelNumber = tmpChannelNumber;
		return hasChanged;
	}

	/**
	 * comparison against the last call of this method.
	 * initialize histoDataDirPath. 
	 * determine input directory which fits to the objectKey, the device and the file extensions.
	 * @param reBuild true perform full check even if neither the device nor the object key was changed 
	 * @return true if the histoDataDirPath was reinitialized. False if no activity is required.
	 */
	private boolean detectFolderChange(boolean reBuild) {
		log.log(Level.FINER, "reInitialize " + reBuild); //$NON-NLS-1$
		boolean hasChanged = false;
		String tmpDeviceName = this.application.getActiveDevice().getName();
		hasChanged = hasChanged || !(tmpDeviceName.equals(this.deviceName));

		// String tmpObjectData = me.application.getActiveObject().getKey();
		// String tmpObjectKey = null;
		// hasChanged = hasChanged || !(tmpObjectData.equals(me.objectData));
		// if (hasChanged) {
		// if (tmpObjectData != null && tmpObjectData.length() > 1) { // use exact defined object key
		// tmpObjectKey = tmpObjectData;
		// }
		// }
		String tmpObjectKey = null;
		if (this.application.getActiveObject() == null) {
			hasChanged = hasChanged || this.objectKey != null;
		} else {
			tmpObjectKey = this.application.getActiveObject().getKey();
			hasChanged = hasChanged || !(tmpObjectKey.equals(this.objectKey));
		}

		if (reBuild || hasChanged || this.histoDataDirPath == null) {
			String tmpHistoDataDirPath;
			if (tmpObjectKey != null) {
				tmpHistoDataDirPath = this.settings.getDataFilePath() + GDE.FILE_SEPARATOR_UNIX + tmpObjectKey;
			} else if (tmpDeviceName != null) {
				tmpHistoDataDirPath = this.settings.getDataFilePath() + GDE.FILE_SEPARATOR_UNIX + tmpDeviceName;
			} else {
				tmpHistoDataDirPath = this.settings.getDataFilePath();
			}
			hasChanged = hasChanged || !tmpHistoDataDirPath.equals(this.histoDataDirPath);

			/*
			 * activate when bin files will be processed
			 * String tmpHistoImportDirPath; String tmpPreferredFileExtention; if (me.device != null) { tmpHistoImportDirPath = me.device.getDeviceConfiguration().getDataBlockType().getPreferredDataLocation(); if (tmpHistoImportDirPath != null) {
			 * tmpHistoImportDirPath = tmpHistoImportDirPath.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX); // adapt import path to current objectKey for (String objectKeyTmp : me.settings.getObjectList()) { if (tmpHistoImportDirPath.endsWith(objectKeyTmp)) { tmpHistoImportDirPath =
			 * tmpHistoImportDirPath.substring(0, tmpHistoImportDirPath.length() - objectKeyTmp.length()) + me.objectKey; break; } } } tmpPreferredFileExtention = me.device.getDeviceConfiguration().getDataBlockType().getPreferredFileExtention(); hasChanged = hasChanged ||
			 * !tmpHistoImportDirPath.equals(me.histoImportDirPath); hasChanged = hasChanged || !tmpPreferredFileExtention.equals(me.preferredFileExtention); } else { tmpHistoImportDirPath = null; tmpPreferredFileExtention = null; hasChanged = hasChanged || me.histoImportDirPath != null; hasChanged =
			 * hasChanged || me.preferredFileExtention != null; }
			 */
			if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, String.format("device=%s, object=\"%s\"", (tmpDeviceName != null ? tmpDeviceName : GDE.STRING_EMPTY), (tmpObjectKey != null ? tmpObjectKey : GDE.STRING_EMPTY))); //$NON-NLS-1$
			this.deviceName = tmpDeviceName;
			this.objectKey = tmpObjectKey;
			this.histoDataDirPath = tmpHistoDataDirPath;
			if (hasChanged) {
				if (log.isLoggable(Level.INFO))
				log.log(Level.INFO, "new histoDataDirPath " + tmpHistoDataDirPath); //$NON-NLS-1$
				/*
				 * activate when bin files will be processed me.histoImportDirPath = tmpHistoImportDirPath; me.preferredFileExtention = tmpPreferredFileExtention; log.log(Level.FINER, "new histoImportDirPath " + tmpHistoImportDirPath); //$NON-NLS-1$ log.log(Level.FINER, "new preferredFileExtention " +
				 * tmpPreferredFileExtention); //$NON-NLS-1$
				 */
			}
		}
		return hasChanged;
	}

	/**
	 * reads files and selects histo file candidates for the active device and objectKey and updates the histo files list.
	 * comparison against the last call of this method.
	 * @param reBuild false: omits the files search for performance reasons, takes the file list from the last call.
	 * @return true if list of files has changed.
	 */
	private boolean detectFilesChange(boolean reBuild) {
		boolean hasChanged = false;
		if (reBuild || this.histoFilePaths == null) {
			this.lastCheckFilesPaths = new TreeMap<Date, String>(Collections.reverseOrder());
			FileUtils.checkDirectoryAndCreate(this.histoDataDirPath);
			try {
				List<File> files = FileUtils.getFileListing(new File(this.histoDataDirPath), 0);
				log.log(Level.INFO, String.format("%04d files found in histoDataDirPath %s", files.size(), this.histoDataDirPath)); //$NON-NLS-1$
				int countFiles = 0;
				for (File file : files) {
					try {
						String linkOrFilePath = file.getAbsolutePath().replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
						// TODO examine lov files similar to osd files.
						if (linkOrFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_OSD)) {
							long startMillis = System.currentTimeMillis();
							String actualFilePath = OperatingSystemHelper.getLinkContainedFilePath(linkOrFilePath).replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
							// getLinkContainedFilePath may have long response times in case of an unavailable network resources
							// This is a workaround: Much better solution would be a function 'getLinkContainedFilePathWithoutAccessingTheLinkedFile'
							if (!linkOrFilePath.equals(actualFilePath) && (System.currentTimeMillis() - startMillis > 555)) {
								log.log(Level.FINER, "Dead OSD link " + linkOrFilePath + " pointing to " + actualFilePath); //$NON-NLS-1$ //$NON-NLS-2$
								if (!file.delete()) {
									log.log(Level.FINE, "could not delete " + file.getName()); //$NON-NLS-1$
								}
							} else {
								boolean histoFilesWithoutObject = false; // TODO new setting
								boolean histoFilesWrongObject = false; // TODO new setting
								HashMap<String, String> fileHeader = OsdReaderWriter.getHeader(actualFilePath);
								String creationDate = fileHeader.get(GDE.CREATION_TIME_STAMP);
								SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd" + ' ' + " HH:mm:ss");
								Date parsedCreationDate = formatter.parse(creationDate);
								if (this.application.getActiveDevice() == null || fileHeader.get(GDE.DEVICE_NAME).equals(this.application.getActiveDevice().getName())) {
									if (this.application.getActiveObject() == null || fileHeader.get(GDE.OBJECT_KEY).equals(this.application.getActiveObject().getKey())
											|| histoFilesWithoutObject && fileHeader.get(GDE.OBJECT_KEY).equals(GDE.STRING_EMPTY)
											|| histoFilesWrongObject && !fileHeader.get(GDE.OBJECT_KEY).equals(this.application.getActiveObject().getKey())) {
										log.log(Level.FINER, String.format("OSD candidate found for object       \"%s\" in %s %s%s  %s%s", (this.application.getActiveObject() != null ? this.application.getActiveObject().getKey() : GDE.STRING_EMPTY), actualFilePath, //$NON-NLS-1$
												GDE.CREATION_TIME_STAMP, fileHeader.get(GDE.CREATION_TIME_STAMP), GDE.DATA_EXPLORER_FILE_VERSION, fileHeader.get(GDE.DATA_EXPLORER_FILE_VERSION)));
										// me.lastCheckFilesPaths1.put(new Date(file.lastModified()), actualFilePath);
										this.lastCheckFilesPaths.put(parsedCreationDate, actualFilePath);
									} else {
										log.log(Level.WARNING, String.format("OSD candidate found for wrong object \"%s\" in %s %s%s  %s%s", fileHeader.get(GDE.OBJECT_KEY), //$NON-NLS-1$
												actualFilePath, GDE.CREATION_TIME_STAMP, fileHeader.get(GDE.CREATION_TIME_STAMP), GDE.DATA_EXPLORER_FILE_VERSION, fileHeader.get(GDE.DATA_EXPLORER_FILE_VERSION)));
										if (fileHeader.get(GDE.OBJECT_KEY).equals(GDE.STRING_EMPTY)) {
											if (SWT.YES == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0065, new String[] { actualFilePath }))) {
												this.lastCheckFilesPaths.put(parsedCreationDate, actualFilePath);
												// TODO Settings.HISTO_FILES_WITHOUT_OBJECT = true;
											}
										} else {
											if (SWT.YES == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0062, new String[] { fileHeader.get(GDE.OBJECT_KEY),
													actualFilePath }))) {
												this.lastCheckFilesPaths.put(parsedCreationDate, actualFilePath);
												// TODO Settings.HISTO_FILES_WRONG_OBJECT = true;
											}
										}
									}
								}
							}
						}
					} catch (IOException e) {
						log.log(Level.WARNING, file.getAbsolutePath(), e);
					} catch (NotSupportedFileFormatException e) {
						log.log(Level.WARNING, e.getLocalizedMessage(), e);
					} catch (Throwable t) {
						log.log(Level.WARNING, t.getLocalizedMessage(), t);
					}
				}
				log.log(Level.INFO, String.format("%04d files taken in histoDataDirPath %s", this.lastCheckFilesPaths.size() - countFiles, this.histoDataDirPath)); // TODO SEVERE -> FINE //$NON-NLS-1$
			} catch (FileNotFoundException e) {
				log.log(Level.WARNING, e.getLocalizedMessage(), e);
			}
			// if (hasChanged = !(tmpHistoFilePaths.containsAll(me.histoFilePaths) && me.histoFilePaths.containsAll(tmpHistoFilePaths))) {
			// compares key and value in Java if both have 'equals' implemented
			hasChanged = this.histoFilePaths == null || !this.histoFilePaths.equals(this.lastCheckFilesPaths);
			if (hasChanged) {
				this.histoFilePaths = this.lastCheckFilesPaths;
			}
		}
		return hasChanged;
	}

	/**
	 * corresponds to the selector in the histoGraphics window. 
	 * may have more or less measurements than the device channel.
	 * @return
	 */
	@Deprecated
	public HashMap<Integer, MeasurementType> getMeasurements() {
		return this.measurements;
	}

	public List<SettlementType> getSettlements() {
		return ((DeviceConfiguration) this.application.getActiveDevice()).getChannel(this.channelNumber).getSettlement();
	}

	@Deprecated
	public List<String> getMeasurementNames_OBS() {
		List<String> result = new ArrayList<String>();
		for (int i = 0; i < this.measurements.size(); i++) {
			result.add(this.measurements.get(i).getName());
		}
		return result;
	}

	public TreeMap<Date, String> getHistoFilePaths() {
		return this.histoFilePaths;
	}

	public void clearMeasurementModes() {
		// TODO Auto-generated method stub -> copy from RecordSet
		throw new UnsupportedOperationException();
	}

	public TrailRecordSet getTrailRecordSet() {
		return this.trailRecordSet;
	}

}
