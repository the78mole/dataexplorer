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
    
    Copyright (c) 2008,2009,2010,2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.io;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.ChannelTypes;
import gde.device.IDevice;
import gde.exception.DeclinedException;
import gde.exception.NotSupportedFileFormatException;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.FileUtils;
import gde.utils.OperatingSystemHelper;
import gde.utils.StringHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

/**
 * Class to provide all file IO relevant functionality
 * @author Winfried Brügmann
 */
public class FileHandler {
	final static Logger	log	= Logger.getLogger(FileHandler.class.getName());

	final DataExplorer	application;
	final Channels			channels;
	final Settings			settings;

	public FileHandler() {
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
	}

	/**
	 * handles the import of an CVS file
	 * @param dialogName
	 * @param isRaw
	 */
	public void importFileCSV(String dialogName, final boolean isRaw) {
		IDevice activeDevice = this.application.getActiveDevice();
		if (activeDevice == null) {
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0004));
			return;
		}
		Settings deviceSetting = Settings.getInstance();
		String devicePath = this.application.getActiveDevice() != null ? GDE.FILE_SEPARATOR_UNIX + this.application.getActiveDevice().getName() : GDE.STRING_EMPTY;
		String path = deviceSetting.getDataFilePath() + devicePath + GDE.FILE_SEPARATOR_UNIX;
		FileDialog csvFileDialog = this.application.openFileOpenDialog(dialogName, new String[] { GDE.FILE_ENDING_STAR_CSV }, path, null, SWT.SINGLE);
		if (csvFileDialog.getFileName().length() > 4) {
			final String csvFilePath = csvFileDialog.getFilterPath() + GDE.FILE_SEPARATOR_UNIX + csvFileDialog.getFileName();

			try {
				char listSeparator = deviceSetting.getListSeparator();
				//check current device and switch if required
				String fileDeviceName = CSVReaderWriter.getHeader(listSeparator, csvFilePath).get(GDE.DEVICE_NAME);
				String activeDeviceName = this.application.getActiveDevice().getName();
				if (!activeDeviceName.equals(fileDeviceName)) { // different device in file
					String msg = Messages.getString(MessageIds.GDE_MSGI0009, new Object[] { fileDeviceName });
					if (SWT.NO == this.application.openYesNoMessageDialog(msg)) return;
					this.application.getDeviceSelectionDialog().setupDevice(fileDeviceName);
				}

				this.application.enableMenuActions(false);
				this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));
				CSVReaderWriter.read(listSeparator, csvFilePath, this.application.getActiveDevice().getRecordSetStemName(), isRaw);
			}
			catch (Exception e) {
				FileHandler.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
				this.application.openMessageDialog(e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
			}
			finally {
				this.application.enableMenuActions(true);
				this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
			}
		}
	}

	/**
	 * handles the export of an CVS file
	 * @param dialogName
	 * @param isRaw
	 */
	public void exportFileCSV(final String dialogName, final boolean isRaw) {
		final Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel == null) {
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0005));
			return;
		}
		RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
		if (activeRecordSet == null) {
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0005));
			return;
		}

		Settings deviceSetting = Settings.getInstance();
		String devicePath = this.application.getActiveDevice() != null ? GDE.FILE_SEPARATOR_UNIX + this.application.getActiveDevice().getName() : GDE.STRING_EMPTY;
		String path = deviceSetting.getDataFilePath() + devicePath + GDE.FILE_SEPARATOR_UNIX;
		FileDialog csvFileDialog = this.application.prepareFileSaveDialog(dialogName, new String[] { GDE.FILE_ENDING_STAR_CSV }, path, getFileNameProposal(true));
		String recordSetKey = activeRecordSet.getName();
		String csvFilePath = csvFileDialog.open();
		if (csvFilePath != null && csvFilePath.length() > 4) { // file name has a reasonable length
			if (FileUtils.checkFileExist(csvFilePath) && SWT.NO == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0007, new Object[] { csvFilePath }))) {
				return;
			}

			try {
				this.application.enableMenuActions(false);
				this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));
				CSVReaderWriter.write(deviceSetting.getListSeparator(), recordSetKey, csvFilePath, isRaw);
			}
			catch (Exception e) {
				FileHandler.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
				this.application.openMessageDialog(e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
			}
			this.application.enableMenuActions(true);
			this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
		}
	}

	/**
	 * get a filename proposal based on global settings configuration and optionally record set name
	 * @param addRecordSetName
	 * @return the file name proposal as string
	 */
	String getFileNameProposal(String recordSetName) {
		String fileName = GDE.STRING_EMPTY;
		if (Settings.getInstance().getUsageDateAsFileNameLeader()) {
			String fileDescriptionDate = GDE.STRING_EMPTY;
			if (this.application.getActiveChannel() != null && this.application.getActiveChannel().getFileDescription().split(GDE.STRING_DASH).length >= 3) {
				String[] chunks = this.application.getActiveChannel().getFileDescription().split(GDE.STRING_DASH);
				int chunkSize = chunks.length;
				if (chunks[chunkSize == 4 ? 1 : 0].length() >= 4 && chunks[chunkSize == 4 ? 2 : 1].length() == 2 && chunks[chunkSize == 4 ? 3 : 2].length() >= 2) {
					try {
						int year = Integer.parseInt(chunks[0].substring(chunks[0].length() - 4));
						int month = Integer.parseInt(chunks[1]);
						int day = Integer.parseInt(chunks[2].substring(0, 2));
						fileDescriptionDate = String.format("%04d-%02d-%02d", year, month, day); //$NON-NLS-1$
					}
					catch (NumberFormatException e) {
						// ignore
					}
				}
			}
			fileName = (fileDescriptionDate.length() > 0 ? fileDescriptionDate : StringHelper.getDate()) + GDE.STRING_UNDER_BAR;
		}
		if (Settings.getInstance().getUsageObjectKeyInFileName() && Channels.getInstance().getActiveChannel() != null && Channels.getInstance().getActiveChannel().getActiveRecordSet() != null) {
			fileName = fileName + Channels.getInstance().getActiveChannel().getObjectKey();
		}
		if (this.application.getActiveChannel() != null) {
			fileName = fileName + (fileName.endsWith(GDE.STRING_UNDER_BAR) ? GDE.STRING_EMPTY : GDE.STRING_UNDER_BAR) + recordSetName.replace(") ", GDE.STRING_UNDER_BAR);
		}
		return fileName;
	}

	/**
	 * get a filename proposal based on global settings configuration and optionally record set name
	 * @param addRecordSetName
	 * @return the file name proposal as string
	 */
	String getFileNameProposal(boolean addRecordSetName) {
		String fileName = GDE.STRING_EMPTY;
		if (Settings.getInstance().getUsageDateAsFileNameLeader()) {
			String fileDescriptionDate = GDE.STRING_EMPTY;
			if (this.application.getActiveRecordSet() != null) {
				fileDescriptionDate = StringHelper.getFormatedTime("yyyy-MM-dd", this.application.getActiveRecordSet().getStartTimeStamp()); //$NON-NLS-1$
			}
			fileName = (fileDescriptionDate.length() > 0 ? fileDescriptionDate : StringHelper.getDate()) + GDE.STRING_UNDER_BAR;
		}
		if (Settings.getInstance().getUsageObjectKeyInFileName() && Channels.getInstance().getActiveChannel() != null && Channels.getInstance().getActiveChannel().getActiveRecordSet() != null) {
			fileName = fileName + Channels.getInstance().getActiveChannel().getObjectKey();
		}
		if (addRecordSetName && this.application.getActiveChannel() != null && this.application.getActiveChannel().getActiveRecordSet() != null) {
			fileName = fileName + GDE.STRING_UNDER_BAR + this.application.getActiveChannel().getActiveRecordSet().getName().replace(") ", GDE.STRING_UNDER_BAR); //$NON-NLS-1$
		}
		return fileName;
	}

	/**
	 * handles the file dialog to open DataExplorer or LogView files
	 * @param dialogName
	 */
	public void openFileDialog(final String dialogName) {
		if (this.application.getDeviceSelectionDialog().checkDataSaved()) {
			Settings deviceSetting = Settings.getInstance();
			String path;
			if (this.application.isObjectoriented()) {
				path = this.application.getObjectFilePath();
			}
			else {
				String devicePath = this.application.getActiveDevice() != null ? GDE.FILE_SEPARATOR_UNIX + this.application.getActiveDevice().getName() : GDE.STRING_EMPTY;
				path = this.application.getActiveDevice() != null ? deviceSetting.getDataFilePath() + devicePath + GDE.FILE_SEPARATOR_UNIX : deviceSetting.getDataFilePath();
				if (!FileUtils.checkDirectoryAndCreate(path)) {
					this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0012, new Object[] { path }));
				}
			}
			FileDialog openFileDialog = this.application.openFileOpenDialog(dialogName, new String[] { GDE.FILE_ENDING_STAR_OSD, GDE.FILE_ENDING_STAR_LOV }, path, null, SWT.SINGLE);
			if (openFileDialog.getFileName().length() > 4) {
				String openFilePath = (openFileDialog.getFilterPath() + GDE.FILE_SEPARATOR_UNIX + openFileDialog.getFileName()).replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);

				if (openFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_OSD))
					openOsdFile(openFilePath);
				else if (openFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_LOV))
					openLovFile(openFilePath);
				else
					this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0008) + openFilePath);
			}
		}
	}

	/**
	 * open a DataExplorer file and load data into a cleaned device/channel
	 * @param openFilePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 * @throws DeclinedException
	 */
	public void openOsdFile(String openFilePath) {
		try {
			boolean existAsObjectLinkFile = true;
			openFilePath = OperatingSystemHelper.getLinkContainedFilePath(openFilePath); // check if windows link
			//check current device and switch if required
			HashMap<String, String> osdHeader = OsdReaderWriter.getHeader(openFilePath);
			String fileDeviceName = osdHeader.get(GDE.DEVICE_NAME);
			// check and switch device, if required
			IDevice activeDevice = this.application.getActiveDevice();
			if (activeDevice == null || !activeDevice.getName().equals(fileDeviceName)) { // new device in file
				this.application.getDeviceSelectionDialog().setupDevice(fileDeviceName);
			}
			//only switch object key, if application is object oriented
			String objectkey = osdHeader.get(GDE.OBJECT_KEY);
			if (this.application.isObjectoriented() && objectkey != null && !objectkey.equals(GDE.STRING_EMPTY)) {
				existAsObjectLinkFile = FileUtils.checkDirectoryAndCreate(Settings.getInstance().getDataFilePath() + GDE.FILE_SEPARATOR_UNIX + objectkey);
				this.application.getMenuToolBar().selectObjectKey(0, objectkey);
			}
			else {
				this.application.getMenuToolBar().selectObjectKeyDeviceOriented();
			}

			String recordSetPropertys = osdHeader.get("1 " + GDE.RECORD_SET_NAME); //$NON-NLS-1$
			String channelConfigName = OsdReaderWriter.getRecordSetProperties(recordSetPropertys).get(GDE.CHANNEL_CONFIG_NAME);
			// channel/configuration type is outlet
			boolean isChannelTypeOutlet = this.channels.getActiveChannel().getType() == ChannelTypes.TYPE_OUTLET;
			if (isChannelTypeOutlet && this.channels.size() > 1) {
				String[] splitChannel = channelConfigName.split(GDE.STRING_BLANK);
				int channelNumber = 1;
				try {
					channelNumber = splitChannel.length == 2 ? new Integer(splitChannel[1]) : (splitChannel.length > 2 ? new Integer(splitChannel[0]) : 1);
				}
				catch (NumberFormatException e) {// ignore
				}
				// at this point we have a channel/config ordinal
				Channel channel = this.channels.get(channelNumber);
				if (channel.size() > 0) { // check for records to be exchanged
					int answer = this.application.openOkCancelMessageDialog(Messages.getString(MessageIds.GDE_MSGI0010,
							new Object[] { channelNumber + GDE.STRING_BLANK_COLON_BLANK + channel.getChannelConfigKey() }));
					if (answer != SWT.OK) return;
				}
				// clean existing channel record sets for new data
				channel.clear();
			}
			else
				this.application.getDeviceSelectionDialog().setupDevice(fileDeviceName);

			try {
				this.application.enableMenuActions(false);
				OsdReaderWriter.read(openFilePath);
				this.channels.getActiveChannel().setFileName(openFilePath.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX));
				if (!existAsObjectLinkFile) this.channels.getActiveChannel().setUnsaved(Channel.UNSAVED_REASON_ADD_OBJECT_KEY);
			}
			catch (Exception e) {
				FileHandler.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
				this.application.openMessageDialog(e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
			}
			this.application.enableMenuActions(true);
			this.application.updateSubHistoryMenuItem(openFilePath);
		}
		catch (Throwable e) {
			FileHandler.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			this.application.openMessageDialog(e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
	}

	/**
	 * handles the save as functionality
	 * @param dialogName
	 * @param fileName
	 */
	public void saveOsdFile(final String dialogName, final String fileName) {
		final Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel == null) {
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0011));
			return;
		}
		RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
		if (activeRecordSet == null) {
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0011));
			return;
		}

		Settings deviceSetting = Settings.getInstance();
		String devicePath = this.application.getActiveDevice() != null ? GDE.FILE_SEPARATOR_UNIX + this.application.getActiveDevice().getName() : GDE.STRING_EMPTY;
		String path = deviceSetting.getDataFilePath() + devicePath;
		if (!FileUtils.checkDirectoryAndCreate(path)) {
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0012, new Object[] { path }));
		}
		FileDialog fileDialog;
		String osdFilePath;
		if (fileName == null || fileName.length() < 5 || fileName.equals(getFileNameProposal(false))) {
			fileDialog = this.application.prepareFileSaveDialog(dialogName, new String[] { GDE.FILE_ENDING_STAR_OSD }, path + GDE.FILE_SEPARATOR_UNIX, getFileNameProposal(false));
			osdFilePath = fileDialog.open();
		}
		else {
			osdFilePath = path + GDE.FILE_SEPARATOR_UNIX + fileName; // including ending ".osd"
		}

		if (osdFilePath != null && osdFilePath.length() > 4 && !osdFilePath.endsWith(getFileNameProposal(false))) { // file name has a reasonable length
			while (osdFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_OSD) || osdFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOV)) {
				osdFilePath = osdFilePath.substring(0, osdFilePath.lastIndexOf('.'));
			}
			osdFilePath = (osdFilePath + GDE.FILE_ENDING_DOT_OSD).replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
			if (FileUtils.checkFileExist(osdFilePath) && SWT.NO == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0013, new Object[] { osdFilePath }))) {
				return;
			}

			try {
				this.application.enableMenuActions(false);
				this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));
				FileUtils.renameFile(osdFilePath, GDE.FILE_ENDING_BAK); // rename existing file to *.bak
				OsdReaderWriter.write(osdFilePath, activeChannel, GDE.DATA_EXPLORER_FILE_VERSION_INT);
				activeChannel.setFileName(osdFilePath.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX));
				activeChannel.setSaved(true);
			}
			catch (Exception e) {
				FileHandler.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
				this.application.openMessageDialog(e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
			}

			this.application.enableMenuActions(true);
			this.application.updateSubHistoryMenuItem(osdFilePath);
			this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
		}
	}

	/**
	 * open a LogView Data file and load data into a cleaned device/channel
	 * @param openFilePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 * @throws DeclinedException
	 */
	public void openLovFile(final String openFilePath) {
		try {
			//check current device and switch if required
			HashMap<String, String> lovHeader = LogViewReader.getHeader(openFilePath);
			String fileDeviceName = lovHeader.get(GDE.DEVICE_NAME);
			String activeDeviceName = this.application.getActiveDevice().getName();
			// check and switch device if required
			if (!activeDeviceName.equals(fileDeviceName)) { // new device in file
				this.application.getDeviceSelectionDialog().setupDevice(fileDeviceName);
			}

			int channelNumber = new Integer(lovHeader.get(GDE.CHANNEL_CONFIG_NUMBER)).intValue();
			IDevice activeDevice = this.application.getActiveDevice();
			String channelType = activeDevice.getChannelTypes(channelNumber).name();
			String channelConfigName = activeDevice.getChannelName(channelNumber);
			FileHandler.log.log(java.util.logging.Level.FINE,
					"channelConfigName = " + channelConfigName + " (" + GDE.CHANNEL_CONFIG_TYPE + channelType + "; " + GDE.CHANNEL_CONFIG_NUMBER + channelNumber + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			Channel channel = this.channels.get(channelNumber);

			if (channel != null && this.channels.getActiveChannel() != null && this.channels.getActiveChannel().getType() == ChannelTypes.TYPE_OUTLET && this.channels.size() > 1) {
				if (this.channels.getActiveChannelNumber() != channelNumber) {
					int answer = this.application.openOkCancelMessageDialog(Messages.getString(MessageIds.GDE_MSGI0006, new Object[] { channelConfigName }));
					if (answer != SWT.OK) return;

					// clean existing channel for new data, if channel does not exist ignore, 
					// this will be covered by the reader by creating a new channel
					channel.clear();
				}
			}
			else
				this.application.getDeviceSelectionDialog().setupDevice(fileDeviceName);

			try {
				this.application.enableMenuActions(false);
				LogViewReader.read(openFilePath);
				this.channels.getActiveChannel().setFileName(openFilePath.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX));
			}
			catch (Exception e) {
				FileHandler.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
				this.application.openMessageDialog(e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
			}
			this.application.enableMenuActions(true);
			this.application.updateSubHistoryMenuItem(openFilePath);
		}
		catch (Exception e) {
			FileHandler.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			this.application.openMessageDialog(e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
	}

	/**
	 * handles the export of an KMZ file
	 * @param dialogName
	 * @param ordinalLongitude
	 * @param ordinalLatitude
	 * @param ordinalHeight
	 * @param ordinalVelocity
	 * @param ordinalSlope
	 * @param ordinalTripLength (optional, if not exist -1)
	 * @param ordinalAzimuth (optional, if not exist -1)
	 * @param isRelative
	 * @param isClampToGround
	 */
	public void exportFileKMZ(final String dialogName, final int ordinalLongitude, final int ordinalLatitude, final int ordinalHeight, final int ordinalVelocity, final int ordinalSlope,
			final int ordinalTripLength, final int ordinalAzimuth, final boolean isRelative, final boolean isClampToGround) {
		final Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel == null) {
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0005));
			return;
		}
		RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
		if (activeRecordSet == null) {
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0005));
			return;
		}

		Settings deviceSetting = Settings.getInstance();
		String devicePath = this.application.getActiveDevice() != null ? GDE.FILE_SEPARATOR_UNIX + this.application.getActiveDevice().getName() : GDE.STRING_EMPTY;
		String path = deviceSetting.getDataFilePath() + devicePath + GDE.FILE_SEPARATOR_UNIX;
		String fileName = activeChannel.getFileName() == null ? this.getFileNameProposal(true) : activeChannel.getFileName();
		fileName = fileName != null && fileName.contains(GDE.STRING_DOT) ? fileName.substring(0, fileName.indexOf(GDE.STRING_DOT)) : fileName;
		if (activeRecordSet.getName().contains(GDE.STRING_RIGHT_BRACKET) && activeRecordSet.getName().contains(GDE.STRING_LEFT_BRACKET)) {
			try {
				String flightNumber = activeRecordSet.getName().substring(activeRecordSet.getName().lastIndexOf(GDE.STRING_LEFT_BRACKET) + 1, activeRecordSet.getName().lastIndexOf(GDE.STRING_RIGHT_BRACKET));
				if (fileName != null && !fileName.contains(GDE.STRING_EMPTY + Integer.parseInt(flightNumber))) {
					fileName = fileName + GDE.STRING_UNDER_BAR + flightNumber;
				}
			}
			catch (NumberFormatException e) {
				// ignore
			}
		}
		FileDialog kmlFileDialog = this.application.prepareFileSaveDialog(dialogName, new String[] { GDE.FILE_ENDING_STAR_KMZ }, path, fileName != null && fileName.length() > 4 ? fileName
				: getFileNameProposal(true));
		String kmzFilePath = kmlFileDialog.open();
		if (kmzFilePath != null && kmlFileDialog.getFileName().length() > 4) {
			if (FileUtils.checkFileExist(kmzFilePath) && SWT.NO == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0007, new Object[] { kmzFilePath }))) {
				return;
			}

			try {
				this.application.enableMenuActions(false);
				this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));

				if (KMZWriter.application.getStatusBar() != null) KMZWriter.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0138, new String[] { GDE.FILE_ENDING_KMZ, kmzFilePath }));

				String kmlFileName = this.getFileNameProposal(activeChannel.getActiveRecordSet().getName());
				kmlFileName = kmlFileName != null && kmlFileName.contains(GDE.STRING_DOT) ? kmlFileName.substring(0, kmlFileName.indexOf(GDE.STRING_DOT)) : kmlFileName;
				kmlFileName = kmlFileName + GDE.FILE_ENDING_DOT_KML;

				KMZWriter.write(kmzFilePath, kmlFileName, activeChannel.getActiveRecordSet(), ordinalLongitude, ordinalLatitude, ordinalHeight, ordinalVelocity, ordinalSlope, ordinalTripLength,
						ordinalAzimuth, isRelative, isClampToGround);

			}
			catch (Exception e) {
				FileHandler.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
				this.application.openMessageDialog(e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
			}
			this.application.enableMenuActions(true);
			this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
		}
	}

	/**
	 * handles the export of an KMZ file without using a file dialog
	 * @param dialogName
	 * @param ordinalLongitude
	 * @param ordinalLatitude
	 * @param ordinalHeight
	 * @param ordinalVelocity
	 * @param ordinalSlope
	 * @param ordinalTripLength (optional, if not exist -1)
	 * @param ordinalAzimuth (optional, if not exist -1)
	 * @param isHeightRelative
	 * @param isExportTmpDir
	 * @return full qualified file path to the exported KML file
	 */
	public String exportFileKMZ(final int ordinalLongitude, final int ordinalLatitude, final int ordinalHeight, final int ordinalVelocity, final int ordinalSlope, final int ordinalTripLength,
			final int ordinalAzimuth, final boolean isHeightRelative, boolean isExportTmpDir) {
		String kmzFilePath = GDE.STRING_EMPTY;
		final Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel == null) {
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0005));
			return GDE.STRING_EMPTY;
		}
		RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
		if (activeRecordSet == null) {
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0005));
			return GDE.STRING_EMPTY;
		}

		Settings deviceSetting = Settings.getInstance();
		String devicePath = this.application.getActiveDevice() != null ? GDE.FILE_SEPARATOR_UNIX + this.application.getActiveDevice().getName() : GDE.STRING_EMPTY;
		String path = isExportTmpDir ? GDE.JAVA_IO_TMPDIR : (deviceSetting.getDataFilePath() + devicePath + GDE.FILE_SEPARATOR_UNIX);
		FileUtils.checkDirectoryAndCreate(path);
		String fileName = path + (activeChannel.getFileName() == null ? this.getFileNameProposal(true) : activeChannel.getFileName());
		fileName = fileName.contains(GDE.STRING_DOT) ? fileName.substring(0, fileName.indexOf(GDE.STRING_DOT)) : fileName;
		if (activeRecordSet.getName().contains(GDE.STRING_RIGHT_BRACKET) && activeRecordSet.getName().contains(GDE.STRING_LEFT_BRACKET)) {
			try {
				String flightNumber = activeRecordSet.getName().substring(activeRecordSet.getName().lastIndexOf(GDE.STRING_LEFT_BRACKET) + 1, activeRecordSet.getName().lastIndexOf(GDE.STRING_RIGHT_BRACKET));
				if (!fileName.contains(GDE.STRING_EMPTY + Integer.parseInt(flightNumber))) {
					fileName = fileName + GDE.STRING_UNDER_BAR + flightNumber;
				}
			}
			catch (NumberFormatException e) {
				// ignore
			}
		}
		kmzFilePath = fileName + GDE.FILE_ENDING_DOT_KMZ;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		if (kmzFilePath.length() > 4) {
			try {
				this.application.enableMenuActions(false);
				this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));

				KMZWriter.write(kmzFilePath, fileName.substring(fileName.lastIndexOf(GDE.FILE_SEPARATOR) + 1) + GDE.FILE_ENDING_DOT_KML, activeRecordSet, ordinalLongitude, ordinalLatitude, ordinalHeight,
						ordinalVelocity, ordinalSlope, ordinalTripLength, ordinalAzimuth, isHeightRelative, false);
			}
			catch (Exception e) {
				FileHandler.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
				this.application.openMessageDialog(e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
			}
			this.application.enableMenuActions(true);
			this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
		}
		if (KMZWriter.application.getStatusBar() != null) KMZWriter.application.setProgress(100, sThreadId);
		return kmzFilePath;
	}

	//
	//	/**
	//	 * handles the export of an GPX file
	//	 * @param dialogName
	//	 * @param ordinalLongitude
	//	 * @param ordinalLatitude
	//	 * @param ordinalGPSHeight
	//	 * @param ordinalVelocity
	//	 * @param ordinalHeight
	//	 * @param isRelative
	//	 */
	//	public void exportFileGPX(final String dialogName, final int ordinalLongitude, final int ordinalLatitude, final int ordinalGPSHeight, final int ordinalVelocity, final int ordinalHeight,
	//			final boolean isRelative) {
	//		final Channel activeChannel = this.channels.getActiveChannel();
	//		if (activeChannel == null) {
	//			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0005));
	//			return;
	//		}
	//		RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
	//		if (activeRecordSet == null) {
	//			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0005));
	//			return;
	//		}
	//
	//		Settings deviceSetting = Settings.getInstance();
	//		String devicePath = this.application.getActiveDevice() != null ? GDE.FILE_SEPARATOR_UNIX + this.application.getActiveDevice().getName() : GDE.STRING_EMPTY;
	//		String path = deviceSetting.getDataFilePath() + devicePath + GDE.FILE_SEPARATOR_UNIX;
	//		String fileName = activeChannel.getFileName() == null ? this.getFileNameProposal(true) : activeChannel.getFileName();
	//		fileName = fileName != null && fileName.contains(GDE.STRING_DOT) ? fileName.substring(0, fileName.indexOf(GDE.STRING_DOT)) : fileName;
	//		if (activeRecordSet.getName().contains(GDE.STRING_RIGHT_BRACKET) && activeRecordSet.getName().contains(GDE.STRING_LEFT_BRACKET)) {
	//			try {
	//				String flightNumber = activeRecordSet.getName().substring(activeRecordSet.getName().lastIndexOf(GDE.STRING_LEFT_BRACKET)+1, activeRecordSet.getName().lastIndexOf(GDE.STRING_RIGHT_BRACKET));
	//				if (!fileName.contains(GDE.STRING_EMPTY + Integer.parseInt(flightNumber))) {
	//					fileName = fileName + GDE.STRING_UNDER_BAR + flightNumber;
	//				}
	//			}
	//			catch (NumberFormatException e) {
	//				// ignore
	//			}
	//		}
	//		FileDialog gpxFileDialog = this.application.prepareFileSaveDialog(dialogName, new String[] { GDE.FILE_ENDING_STAR_GPX }, path, fileName != null && fileName.length() > 4 ? fileName : getFileNameProposal(true));
	//		String gpxFilePath = gpxFileDialog.open();
	//		if (gpxFilePath != null && gpxFilePath.length() > 4) {
	//			if (FileUtils.checkFileExist(gpxFilePath) && SWT.NO == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0007, new Object[] { gpxFilePath }))) {
	//				return;
	//			}
	//
	//			try {
	//				this.application.enableMenuActions(false);
	//				this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));
	//				GPXWriter.write(activeRecordSet, gpxFilePath, ordinalLongitude, ordinalLatitude, ordinalGPSHeight, ordinalVelocity, ordinalHeight, isRelative);
	//			}
	//			catch (Exception e) {
	//				log.log(Level.WARNING, e.getMessage(), e);
	//				this.application.openMessageDialog(e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
	//			}
	//			this.application.enableMenuActions(true);
	//			this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
	//		}
	//	}

	/**
	 * handles the export of an IGC file
	 * @param dialogName
	 * @param device
	 * @param igcFileHeader
	 * @param ordinalLongitude
	 * @param ordinalLatitude
	 * @param ordinalAltitude
	 * @param offsetAltitude
	 * @param offsetUTC
	 */
	public void exportFileIGC(final String dialogName, final IDevice device, final StringBuilder igcFileHeader, final int ordinalLongitude, final int ordinalLatitude, final int ordinalAltitude,
			final int offsetAltitude, final int offsetUTC) {
		final Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel == null) {
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0005));
			return;
		}
		RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
		if (activeRecordSet == null) {
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0005));
			return;
		}

		Settings deviceSetting = Settings.getInstance();
		String devicePath = this.application.getActiveDevice() != null ? GDE.FILE_SEPARATOR_UNIX + this.application.getActiveDevice().getName() : GDE.STRING_EMPTY;
		String path = deviceSetting.getDataFilePath() + devicePath + GDE.FILE_SEPARATOR_UNIX;
		String fileName = activeChannel.getFileName() == null ? this.getFileNameProposal(true) : activeChannel.getFileName();
		fileName = fileName != null && fileName.contains(GDE.STRING_DOT) ? fileName.substring(0, fileName.indexOf(GDE.STRING_DOT)) : fileName;
		if (activeRecordSet.getName().contains(GDE.STRING_RIGHT_BRACKET) && activeRecordSet.getName().contains(GDE.STRING_LEFT_BRACKET)) {
			try {
				String flightNumber = activeRecordSet.getName().substring(activeRecordSet.getName().lastIndexOf(GDE.STRING_LEFT_BRACKET) + 1, activeRecordSet.getName().lastIndexOf(GDE.STRING_RIGHT_BRACKET));
				if (fileName != null && !fileName.contains(GDE.STRING_EMPTY + Integer.parseInt(flightNumber))) {
					fileName = fileName + GDE.STRING_UNDER_BAR + flightNumber;
				}
			}
			catch (NumberFormatException e) {
				// ignore
			}
		}
		FileDialog igcFileDialog = this.application.prepareFileSaveDialog(dialogName, new String[] { GDE.FILE_ENDING_STAR_IGC }, path, fileName != null && fileName.length() > 4 ? fileName
				: getFileNameProposal(true));
		String igcFilePath = igcFileDialog.open();
		if (igcFilePath != null && igcFileDialog.getFileName().length() > 4) {
			if (FileUtils.checkFileExist(igcFilePath) && SWT.NO == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0007, new Object[] { igcFilePath }))) {
				return;
			}

			try {
				this.application.enableMenuActions(false);
				this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));

				IGCWriter.write(device, igcFilePath, igcFileHeader, activeChannel.getActiveRecordSet(), ordinalLongitude, ordinalLatitude, ordinalAltitude, offsetAltitude, offsetUTC);
			}
			catch (Exception e) {
				FileHandler.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
				this.application.openMessageDialog(e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
			}
			this.application.enableMenuActions(true);
			this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
		}
	}
}
