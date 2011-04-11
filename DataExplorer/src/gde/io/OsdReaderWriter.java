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
    
    Copyright (c) 2008,2009,2010,2011 Winfried Bruegmann
****************************************************************************************/
package gde.io;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.ChannelTypes;
import gde.device.IDevice;
import gde.exception.DataInconsitsentException;
import gde.exception.NotSupportedFileFormatException;
import gde.exception.GDEInternalException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;
import gde.utils.FileUtils;
import gde.utils.OperatingSystemHelper;
import gde.utils.StringHelper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * @author Winfried Brügmann
 * This class reads and writes DataExplorer file format
 */
public class OsdReaderWriter {
	final static Logger									log												= Logger.getLogger(OsdReaderWriter.class.getName());

	final static DataExplorer	application								= DataExplorer.getInstance();
	final static Channels 							channels 									= Channels.getInstance();


	/**
	 * get data file header data
	 * @param filePath
	 * @return hash map containing header data as string accessible by public header keys
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 */

	public static HashMap<String, String> getHeader(String filePath) throws FileNotFoundException, IOException, NotSupportedFileFormatException {
		FileInputStream file_input = new FileInputStream(new File(filePath));
		DataInputStream data_in    = new DataInputStream(file_input);
		return readHeader(filePath, data_in);
	}

	/**
	 * method to read the data using a given input stream
	 * @param filePath
	 * @param data_in
	 * @return
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 */
	private static HashMap<String, String> readHeader(final String filePath, DataInputStream data_in) throws IOException, NotSupportedFileFormatException {
		String line;
		HashMap<String, String> header = new HashMap<String, String>();
		int headerCounter = GDE.OSD_FORMAT_HEADER_KEYS.length+1;

		line = data_in.readUTF();
		line = line.substring(0, line.length()-1);
		log.log(Level.FINE, line);
		if (!line.startsWith(GDE.DATA_EXPLORER_FILE_VERSION) && !line.startsWith(GDE.LEGACY_FILE_VERSION))
			throw new NotSupportedFileFormatException(filePath);

		String sVersion = line.startsWith(GDE.DATA_EXPLORER_FILE_VERSION) ? line.substring(GDE.DATA_EXPLORER_FILE_VERSION.length(), GDE.DATA_EXPLORER_FILE_VERSION.length()+1).trim()
				: line.substring(GDE.LEGACY_FILE_VERSION.length(), GDE.LEGACY_FILE_VERSION.length()+1).trim();
		int version;
		try {
			version = new Integer(sVersion).intValue(); // one digit only
		}
		catch (NumberFormatException e) {
			log.log(Level.SEVERE, "can not interprete red version information " + sVersion);
			throw new NotSupportedFileFormatException(filePath);
		}

		switch (version) {
		case 1:
		case 2: // added OBJECT_KEY to header
			header.put(GDE.DATA_EXPLORER_FILE_VERSION, GDE.STRING_EMPTY+version);
			boolean isHeaderComplete = false;
			while (!isHeaderComplete && headerCounter-- > 0) {
				line = data_in.readUTF();
				line = line.substring(0, line.length() - 1);
				log.log(Level.FINE, line);
				for (String headerKey : GDE.OSD_FORMAT_HEADER_KEYS) {
					if (line.startsWith(headerKey)) {
						log.log(Level.FINE, line);
						header.put(headerKey, line.substring(headerKey.length()));
						if (line.startsWith(GDE.RECORD_SET_SIZE)) {
							headerCounter = new Integer(header.get(GDE.RECORD_SET_SIZE).trim()).intValue();
							//read record set descriptors
							int lastReordNumber = headerCounter;
							while (headerCounter-- > 0) {
								line = data_in.readUTF();
								line = line.substring(0, line.length() - 1);
								if (line.startsWith(GDE.RECORD_SET_NAME)) {
									log.log(Level.FINE, line);
									header.put((lastReordNumber-headerCounter)+GDE.STRING_BLANK+GDE.RECORD_SET_NAME, line.substring(GDE.RECORD_SET_NAME.length()));
								}
							}
							isHeaderComplete = true;
						}
						break;
					}
				}
			}
			break;

		default:
			throw new NotSupportedFileFormatException(filePath);
		}

		return header;
	}

	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 * @throws DataInconsitsentException
	 */
	public static RecordSet read(String filePath) throws FileNotFoundException, IOException, NotSupportedFileFormatException, DataInconsitsentException {
		FileInputStream file_input = new FileInputStream(new File(filePath));
		DataInputStream data_in    = new DataInputStream(file_input);
		String channelConfig = GDE.STRING_EMPTY;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetComment = GDE.STRING_EMPTY;
		String recordSetProperties = GDE.STRING_EMPTY;
		String[] recordsProperties;
		int recordDataSize = 0;
		long recordSetDataPointer = 0;
		Channel channel = null;
		RecordSet recordSet = null;
		IDevice device = OsdReaderWriter.application.getActiveDevice();
		String line;
		boolean isFirstRecordSetDisplayed = false;

		HashMap<String, String> header = getHeader(filePath);
		ChannelTypes channelType = ChannelTypes.valueOf(header.get(GDE.CHANNEL_CONFIG_TYPE).trim());
		String objectKey = header.get(GDE.OBJECT_KEY) != null ? header.get(GDE.OBJECT_KEY) : GDE.STRING_EMPTY;
		int numberRecordSets = new Integer(header.get(GDE.RECORD_SET_SIZE).trim()).intValue();
		while(!data_in.readUTF().startsWith(GDE.RECORD_SET_SIZE))
			log.log(Level.FINE, "skip"); //$NON-NLS-1$

		// record sets with it properties and records
		List<HashMap<String,String>> recordSetsInfo = new ArrayList<HashMap<String,String>>();
		for (int i=0; i<numberRecordSets; ++i) {
			// channel/configuration :: record set name :: recordSet description :: data pointer :: properties
			line = data_in.readUTF();
			line = line.substring(0, line.length()-1);
			recordSetsInfo.add(getRecordSetProperties(line));
		}

		try { // build the data structure
			for (HashMap<String,String> recordSetInfo : recordSetsInfo) {
				channelConfig = recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME);
				recordSetName = recordSetInfo.get(GDE.RECORD_SET_NAME);
				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
				recordSetComment = recordSetInfo.get(GDE.RECORD_SET_COMMENT);
				recordSetProperties = recordSetInfo.get(GDE.RECORD_SET_PROPERTIES);
				recordsProperties = StringHelper.splitString(recordSetInfo.get(GDE.RECORDS_PROPERTIES), Record.END_MARKER, GDE.RECORDS_PROPERTIES);
				recordDataSize = new Long(recordSetInfo.get(GDE.RECORD_DATA_SIZE)).intValue();
				//recordSetDataPointer = new Long(recordSetInfo.get(RECORD_SET_DATA_POINTER)).longValue();
				
				channel = channels.get(channels.getChannelNumber(channelConfig));
				if (channel == null) { // 1.st try channelConfiguration not found
					try { // get channel last digit and use as channel config ordinal
						channel = channels.get(new Integer(channelConfig.substring(channelConfig.length()-1)));
						channelConfig = channel.getChannelConfigKey();
						recordSetInfo.put(GDE.CHANNEL_CONFIG_NAME, channelConfig);
					}
					catch (NumberFormatException e) {
						// ignore and keep channel as null
					}
					catch (NullPointerException e) {
						// ignore and keep channel as null
					}
				}
				if (channel == null) { // 2.nd try channelConfiguration not found
					try { // try to get channel startsWith configuration name
						channel = channels.get(channels.getChannelNumber(channelConfig.split(" ")[0]));
						channelConfig = channel.getChannelConfigKey();
						recordSetInfo.put(GDE.CHANNEL_CONFIG_NAME, channelConfig);
					}
					catch (NullPointerException e) {
						// ignore and keep channel as null
					}
				}
				if (channel == null) { // 3.rd try channelConfiguration not found
					String msg = Messages.getString(MessageIds.GDE_MSGI0018, new Object[] { recordSetName }) + " " + Messages.getString(MessageIds.GDE_MSGI0019) + "\n" + Messages.getString(MessageIds.GDE_MSGI0020);
					DataExplorer.getInstance().openMessageDialogAsync(msg);
					channel = new Channel(channelConfig, channelType);
					// do not allocate records to record set - newChannel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, activeConfig));
					channels.put(channel.getNumber(), channel);
					Vector<String> newChannelNames = new Vector<String>();
					for (String channelConfigKey : channels.getChannelNames()) {
						newChannelNames.add(channelConfigKey);
					}
					newChannelNames.add(channel.getNumber() + " : " + channelConfig); //$NON-NLS-1$
					channels.setChannelNames(newChannelNames.toArray(new String[1]));
				}
				channels.setActiveChannelNumber(channel.getNumber());
				channel.setObjectKey(objectKey);
				// "3 : Motor"
				channelConfig = channelConfig.contains(GDE.STRING_COLON) ? channelConfig.split(GDE.STRING_COLON)[1].trim() : channelConfig.trim();
				// "Motor 3"
				channelConfig = channelConfig.contains(GDE.STRING_BLANK) ? channelConfig.split(GDE.STRING_BLANK)[0].trim() : channelConfig.trim();
				//"Motor"
				recordSet = RecordSet.createRecordSet(recordSetName, device, channel.getNumber(), true, true);
				//apply record sets properties
				recordSet.setRecordSetDescription(recordSetComment);

				//apply record sets records properties
				if (log.isLoggable(Level.FINE)) {
					StringBuilder sb = new StringBuilder().append(recordSet.getName()).append(GDE.STRING_MESSAGE_CONCAT);
					for (String recordProps : recordsProperties) {
						sb.append(StringHelper.splitString(recordProps, Record.DELIMITER, Record.propertyKeys).get(Record.propertyKeys[0])).append(GDE.STRING_COMMA);
					}
					log.log(Level.FINE, sb.toString());
				}

				String [] recordKeys = device.crossCheckMeasurements(recordsProperties, recordSet);
				// check if the file content fits measurements form device properties XML which was used to create the record set
				for (int i = 0; i < recordKeys.length; ++i) {
					Record record = recordSet.get(recordKeys[i]);
					log.log(Level.FINER, "setSerializedProperties " + recordKeys[i]);
					record.setSerializedProperties(recordsProperties[i]);
					record.setSerializedDeviceSpecificProperties(recordsProperties[i]);
				}
				recordSet.setDeserializedProperties(recordSetProperties);
				recordSet.setSaved(true);

				channel.put(recordSetName, recordSet);
			}
			MenuToolBar menuToolBar = OsdReaderWriter.application.getMenuToolBar();
			if (menuToolBar != null) {
				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
			}

			String[] firstRecordSet = new String[2];
			for (HashMap<String,String> recordSetInfo : recordSetsInfo) {
				channelConfig = recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME);
				recordSetName = recordSetInfo.get(GDE.RECORD_SET_NAME);
				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
				if (firstRecordSet[0] == null || firstRecordSet[1] == null) {
					firstRecordSet[0] = channelConfig;
					firstRecordSet[1] = recordSetName;
				}
				recordDataSize = new Long(recordSetInfo.get(GDE.RECORD_DATA_SIZE)).intValue();
				log.log(Level.FINE, "recordDataSize = " + recordDataSize);
				recordSetDataPointer = new Long(recordSetInfo.get(GDE.RECORD_SET_DATA_POINTER)).longValue();
				log.log(Level.FINE, "recordSetDataPointer = " + recordSetDataPointer);
				channel = channels.get(channels.getChannelNumber(channelConfig));
				recordSet = channel.get(recordSetName);
				int numberRecordAndTimeStamp = recordSet.getNoneCalculationRecordNames().length + (recordSet.isTimeStepConstant() ? 0 : 1);
				recordSet.setFileDataPointerAndSize(recordSetDataPointer, recordDataSize, GDE.SIZE_BYTES_INTEGER * numberRecordAndTimeStamp  * recordDataSize);
				if (recordSetName.equals(firstRecordSet[1])) {
					long startTime = new Date().getTime();
					byte[] buffer = new byte[recordSet.getFileDataBytesSize()];
					data_in.readFully(buffer);
					recordSet.getDevice().addDataBufferAsRawDataPoints(recordSet, buffer, recordDataSize, application.getStatusBar() != null);
					log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (new Date().getTime() - startTime)));
				}
				// display the first record set data while reading the rest of the data
				if (!isFirstRecordSetDisplayed && firstRecordSet[0] != null && firstRecordSet[1] != null && application.getMenuToolBar() != null) {
					isFirstRecordSetDisplayed = true;
					channel.setFileName(filePath);
					channel.setFileDescription(header.get(GDE.FILE_COMMENT));
					channel.setSaved(true);
					channels.switchChannel(channels.getChannelNumber(firstRecordSet[0]), firstRecordSet[1]);
				}
			}

			return recordSet;
		}
		finally {
			data_in.close ();
			data_in = null;
			file_input = null;
		}
	}

	/**
	 * get parsed record set properties containing all data found by OSD_FORMAT_DATA_KEYS
	 * @param recordSetProperties
	 * @return hash map with string type data
	 */
	public static HashMap<String, String> getRecordSetProperties(String recordSetProperties) {
		return StringHelper.splitString(recordSetProperties, GDE.DATA_DELIMITER, GDE.OSD_FORMAT_DATA_KEYS);
	}

	/**
	 * write channel data to osd file format
	 * - if channel type is TYPE_OUTLET only this channel record sets are part of the written file
	 * - if channel type is TYPE_CONFIG all records sets of all channel configurations are written to the file
	 * @param fullQualifiedFilePath
	 * @param activeChannel
	 * @param useVersion
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void write(String fullQualifiedFilePath, Channel activeChannel, int useVersion) throws FileNotFoundException, IOException {
		if (activeChannel != null && fullQualifiedFilePath != null && useVersion != 0) {
			FileOutputStream file_out = new FileOutputStream(new File(fullQualifiedFilePath));
			DataOutputStream data_out = new DataOutputStream(file_out);
			IDevice activeDevice = OsdReaderWriter.application.getActiveDevice();
			boolean isObjectOriented = OsdReaderWriter.application.isObjectoriented();
			int filePointer = 0;
			try {
				// before do anything make sure all data is loaded, if data comes from another file
				activeChannel.checkAndLoadData();

				// first line : header with version
				String versionString = GDE.DATA_EXPLORER_FILE_VERSION + useVersion + GDE.STRING_NEW_LINE;
				data_out.writeUTF(versionString);
				filePointer += GDE.SIZE_UTF_SIGNATURE + versionString.getBytes("UTF8").length; //$NON-NLS-1$
				log.log(Level.FINE, "line lenght = " + (GDE.SIZE_UTF_SIGNATURE + versionString.getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				//creation time stamp
				StringBuilder sb = new StringBuilder();
				sb.append(GDE.CREATION_TIME_STAMP).append(new SimpleDateFormat("yyyy-MM-dd").format(new Date())).append(' '); //$NON-NLS-1$
				sb.append(new SimpleDateFormat(" HH:mm:ss").format(new Date().getTime())).append(GDE.STRING_NEW_LINE); //$NON-NLS-1$
				data_out.writeUTF(sb.toString());
				filePointer += GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				log.log(Level.FINE, "line lenght = " + (GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// second line : size file comment , file comment
				sb = new StringBuilder();
				sb.append(GDE.FILE_COMMENT).append(activeChannel.getFileDescription()).append(GDE.STRING_NEW_LINE);
				data_out.writeUTF(sb.toString());
				filePointer += GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				log.log(Level.FINE, "line lenght = " + (GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// third line : size device name , device name
				sb = new StringBuilder();
				sb.append(GDE.DEVICE_NAME).append(activeDevice.getName()).append(GDE.STRING_NEW_LINE);
				data_out.writeUTF(sb.toString());
				filePointer += GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				log.log(Level.FINE, "line lenght = " + (GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// fourth line : size channel/config type , channel/config type
				sb = new StringBuilder();
				sb.append(GDE.CHANNEL_CONFIG_TYPE).append(activeChannel.getType().name()).append(GDE.STRING_NEW_LINE);
				data_out.writeUTF(sb.toString());
				filePointer += GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				log.log(Level.FINE, "line lenght = " + (GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// fifth line : object key
				sb = new StringBuilder();
				sb.append(GDE.OBJECT_KEY).append(activeChannel.getObjectKey()).append(GDE.STRING_NEW_LINE);
				data_out.writeUTF(sb.toString());
				filePointer += GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				log.log(Level.FINE, "line lenght = " + (GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// number of record sets
				sb = new StringBuilder();
				sb.append(GDE.RECORD_SET_SIZE).append(activeChannel.size()).append(GDE.STRING_NEW_LINE);
				data_out.writeUTF(sb.toString());
				filePointer += GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				log.log(Level.FINE, "line lenght = " + (GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// record sets with it properties
				StringBuilder[] sbs = new StringBuilder[activeChannel.size()];
				String[] recordSetNames = activeChannel.getRecordSetNames();
				// prepare all record set describing data
				for (int i = 0; i < activeChannel.size(); ++i) {
					//if ChannelTypes.TYPE_OUTLET only record sets associated to that channel goes into one file
					//if ChannelTypes.TYPE_CONFIG all record sets with different configurations goes into one file
					//RecordSetName :: ChannelConfigurationName :: RecordSetComment :: RecordSetProperties :: RecordDataSize :: RecordSetDataPointer
					Channel recordSetChannel = activeChannel.getType().equals(ChannelTypes.TYPE_OUTLET) ? activeChannel : Channels.getInstance().get(activeChannel.findChannelOfRecordSet(recordSetNames[i]));
					if (recordSetChannel != null) {
						RecordSet recordSet = recordSetChannel.get(recordSetNames[i]);
						if (recordSet != null) {
							sbs[i] = new StringBuilder();
							sbs[i].append(GDE.RECORD_SET_NAME).append(recordSet.getName()).append(GDE.DATA_DELIMITER)
								.append(GDE.CHANNEL_CONFIG_NAME).append(recordSetChannel.getNumber()).append(GDE.STRING_BLANK_COLON_BLANK).append(recordSet.getChannelConfigName()).append(GDE.DATA_DELIMITER)
								.append(GDE.RECORD_SET_COMMENT).append(recordSet.getRecordSetDescription()).append(GDE.DATA_DELIMITER)
								.append(GDE.RECORD_SET_PROPERTIES).append(recordSet.getSerializeProperties()).append(GDE.DATA_DELIMITER);
							// serialized recordSet configuration data (record names, unit, symbol, isActive, ....) size data points , pointer data start or file name
							for (String recordKey : recordSet.getRecordNames()) {
								sbs[i].append(GDE.RECORDS_PROPERTIES).append(recordSet.get(recordKey).getSerializeProperties());
							}
							sbs[i].append(GDE.DATA_DELIMITER).append(GDE.RECORD_DATA_SIZE).append(String.format("%10s", recordSet.getRecordDataSize(true))).append(GDE.DATA_DELIMITER); //$NON-NLS-1$
							filePointer += GDE.SIZE_UTF_SIGNATURE + sbs[i].toString().getBytes("UTF8").length; //$NON-NLS-1$
							filePointer += GDE.RECORD_SET_DATA_POINTER.toString().getBytes("UTF8").length + 10 + GDE.STRING_NEW_LINE.toString().getBytes("UTF8").length; // pre calculated size //$NON-NLS-1$ //$NON-NLS-2$
							log.log(Level.FINE, "line lenght = " //$NON-NLS-1$
								+ (GDE.SIZE_UTF_SIGNATURE + sbs[i].toString().getBytes("UTF8").length + GDE.RECORD_SET_DATA_POINTER.toString().getBytes("UTF8").length + 10 + GDE.STRING_NEW_LINE.toString().getBytes("UTF8").length) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								+ " filePointer = " + filePointer); //$NON-NLS-1$

							if (log.isLoggable(Level.FINE)) {
								StringBuilder sb1 = new StringBuilder().append(recordSet.getName()).append(GDE.STRING_MESSAGE_CONCAT);
								for (String recordName : recordSet.getRecordNames()) {
									sb1.append(recordName).append(GDE.STRING_COMMA);
								}
								log.log(Level.FINE, sb1.toString());
							}
						}
					}
				}
				// prepare all record set data pointer
				for (int i = 0; i < activeChannel.size(); ++i) {
					//if ChannelTypes.TYPE_OUTLET only record sets associated to that channel goes into one file
					//if ChannelTypes.TYPE_CONFIG all record sets with different configurations goes into one file
					//RecordSetName :: ChannelConfigurationName :: RecordSetComment :: RecordSetProperties :: RecordDataSize :: RecordSetDataPointer
					Channel recordSetChannel = activeChannel.getType().equals(ChannelTypes.TYPE_OUTLET) ? activeChannel : Channels.getInstance().get(activeChannel.findChannelOfRecordSet(recordSetNames[i]));
					if (recordSetChannel != null) {
						RecordSet recordSet = recordSetChannel.get(recordSetNames[i]);
						if (recordSet != null) {
							recordSet.resetZoomAndMeasurement(); // make sure size() returns right value
							sbs[i].append(GDE.RECORD_SET_DATA_POINTER).append(String.format("%10s", filePointer)).append(GDE.STRING_NEW_LINE); //$NON-NLS-1$
							log.log(Level.FINE, sbs[i].toString());
							//data_out.writeInt(sbs[i].length());
							data_out.writeUTF(sbs[i].toString());
							int dataSizeRecord = GDE.SIZE_BYTES_INTEGER * recordSet.getRecordDataSize(true);
							int dataSizeRecords = dataSizeRecord * recordSet.getNoneCalculationRecordNames().length;
							int dataSizeRecordsTimeStamp = dataSizeRecord + dataSizeRecords;
							filePointer += (recordSet.isTimeStepConstant() ? dataSizeRecords : dataSizeRecordsTimeStamp);
							log.log(Level.FINE, (recordSet.isTimeStepConstant() ? dataSizeRecords : dataSizeRecordsTimeStamp) + " filePointer = " + filePointer); //$NON-NLS-1$
						}
					}
				}
				// check if all involved record sets have data (if loaded from file it might be possible that some record set lack of its data)
				long startTime = new Date().getTime();
				for (int i = 0; i < activeChannel.size(); ++i) {
					//if ChannelTypes.TYPE_OUTLET only record sets associated to that channel goes into one file
					//if ChannelTypes.TYPE_CONFIG all record sets with different configurations goes into one file
					Channel recordSetChannel = activeChannel.getType().equals(ChannelTypes.TYPE_OUTLET) ? activeChannel : Channels.getInstance().get(activeChannel.findChannelOfRecordSet(recordSetNames[i]));
					if (recordSetChannel != null) {
						RecordSet recordSet = recordSetChannel.get(recordSetNames[i]);
						if (recordSet != null) {
							if (!recordSet.hasDisplayableData()) recordSet.loadFileData(recordSetChannel.getFullQualifiedFileName(), application.getStatusBar() != null);
							String[] noneCalculationRecordNames = recordSet.getNoneCalculationRecordNames();
							int dataSizeRecord = GDE.SIZE_BYTES_INTEGER * recordSet.getRecordDataSize(true);
							int dataSizeRecords = dataSizeRecord * noneCalculationRecordNames.length;
							int dataSizeRecordsTimeStamp = dataSizeRecord + dataSizeRecords;
							byte[] buffer = new byte[recordSet.isTimeStepConstant() ? dataSizeRecords : dataSizeRecordsTimeStamp];
							byte[] bytes = new byte[GDE.SIZE_BYTES_INTEGER];
							int l = 0;
							if (!recordSet.isTimeStepConstant()) {
								for (int j = 0; j < recordSet.getRecordDataSize(true); ++j, l += GDE.SIZE_BYTES_INTEGER) {
										int timeStamp = recordSet.getTime(j);
										//log.log(Level.FINER, ""+point);
										bytes[0] = (byte) ((timeStamp >>> 24) & 0xFF);
										bytes[1] = (byte) ((timeStamp >>> 16) & 0xFF);
										bytes[2] = (byte) ((timeStamp >>> 8) & 0xFF);
										bytes[3] = (byte) ((timeStamp >>> 0) & 0xFF);
										System.arraycopy(bytes, 0, buffer, l, GDE.SIZE_BYTES_INTEGER);
								}
							}
							if (recordSet.isRaw()) {
								for (int j = 0; j < recordSet.getRecordDataSize(true); ++j) {
									for (int k = 0; k < noneCalculationRecordNames.length; ++k, l += GDE.SIZE_BYTES_INTEGER) {
										int point = recordSet.get(noneCalculationRecordNames[k]).realGet(j);
										//log.log(Level.FINER, ""+point);
										bytes[0] = (byte) ((point >>> 24) & 0xFF);
										bytes[1] = (byte) ((point >>> 16) & 0xFF);
										bytes[2] = (byte) ((point >>> 8) & 0xFF);
										bytes[3] = (byte) ((point >>> 0) & 0xFF);
										System.arraycopy(bytes, 0, buffer, l, GDE.SIZE_BYTES_INTEGER);
									}
								}
							}
							else {
								IDevice device = recordSet.getDevice();
								for (int j = 0; j < recordSet.getRecordDataSize(true); ++j) {
									for (int k = 0; k < noneCalculationRecordNames.length; ++k, l += GDE.SIZE_BYTES_INTEGER) {
										Record record = recordSet.get(noneCalculationRecordNames[k]);
										int point = Double.valueOf(device.reverseTranslateValue(record, record.realGet(j)/1000.0)*1000.0).intValue();
										//log.log(Level.FINER, ""+point);
										bytes[0] = (byte) ((point >>> 24) & 0xFF);
										bytes[1] = (byte) ((point >>> 16) & 0xFF);
										bytes[2] = (byte) ((point >>> 8) & 0xFF);
										bytes[3] = (byte) ((point >>> 0) & 0xFF);
										System.arraycopy(bytes, 0, buffer, l, GDE.SIZE_BYTES_INTEGER);
									}
								}
							}
							data_out.write(buffer, 0, buffer.length);
							recordSet.setSaved(true);
						}
					}
				}
				log.log(Level.TIME, "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));

				//update/write link if object oriented
				if (isObjectOriented) {
					OperatingSystemHelper.createFileLink(fullQualifiedFilePath,
							OsdReaderWriter.application.getObjectFilePath() + fullQualifiedFilePath.substring(fullQualifiedFilePath.lastIndexOf(GDE.FILE_SEPARATOR_UNIX)+1));
				}
			}
			finally {
				data_out.flush();
				data_out.close();
				data_out = null;
				file_out = null;
			}
		}
		else {
			GDEInternalException e = new GDEInternalException(Messages.getString(MessageIds.GDE_MSGE0009) + activeChannel + ", " + fullQualifiedFilePath + ", " + useVersion); //$NON-NLS-1$
			DataExplorer.getInstance().openMessageDialogAsync(e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
	}

	/**
	 * read record set data with given file seek pointer and record size
	 * @param recordSet
	 * @param filePath
	 * @throws DataInconsitsentException
	 */
	public static void readRecordSetsData(RecordSet recordSet, String filePath, boolean doUpdateProgressBar) throws FileNotFoundException, IOException, DataInconsitsentException {
		RandomAccessFile random_in = new RandomAccessFile(new File(filePath), "r"); //$NON-NLS-1$
		try {
			long recordSetFileDataPointer = recordSet.getFileDataPointer();
			int recordFileDataSize = recordSet.getFileDataSize();
			random_in.seek(recordSetFileDataPointer);
			long startTime = new Date().getTime();
			int dataSizeRecord = GDE.SIZE_BYTES_INTEGER * recordFileDataSize;
			int dataSizeRecords = dataSizeRecord * recordSet.getNoneCalculationRecordNames().length;
			int dataSizeRecordsTimeStamp = dataSizeRecord + dataSizeRecords;
			byte[] buffer = new byte[recordSet.isTimeStepConstant() ? dataSizeRecords : dataSizeRecordsTimeStamp];
			random_in.readFully(buffer);
			recordSet.getDevice().addDataBufferAsRawDataPoints(recordSet, buffer, recordFileDataSize, doUpdateProgressBar);
			log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		catch (DataInconsitsentException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			random_in.close();
		}
	}
	
	/**
	 * Search through all data files and update the old object key with the new one
	 * @param oldObjectKey
	 * @param newObjectKey
	 */
	public static void updateObjectKey(String oldObjectKey, String newObjectKey) {
		try {
			//scan all data files for object key
			List<File> files = FileUtils.getFileListing(new File(Settings.getInstance().getDataFilePath()));
			Iterator<File> iterator = files.iterator();
			while(iterator.hasNext()){
				File file  = iterator.next();			
				try {
					String actualFilePath = file.getAbsolutePath();
					if (actualFilePath.endsWith(GDE.FILE_ENDING_OSD) && actualFilePath.equals(OperatingSystemHelper.getLinkContainedFilePath(actualFilePath))) {
						log.log(Level.FINER, "working with " + file.getName()); //$NON-NLS-1$
						if (oldObjectKey.equals(OsdReaderWriter.getHeader(file.getCanonicalPath()).get(GDE.OBJECT_KEY))) {
							log.log(Level.FINER, "found file with given object key " + file.getName()); //$NON-NLS-1$
						}
						else {
							iterator.remove();
						}
					}
					else {
						iterator.remove();
					}
				}
				catch (IOException e) {
					log.log(Level.WARNING, file.getAbsolutePath(), e);
				}
				catch (NotSupportedFileFormatException e) {
					log.log(Level.WARNING, e.getLocalizedMessage(), e);
				}
				catch (Throwable t) {
					log.log(Level.WARNING, t.getLocalizedMessage(), t);
				}
			}
			//at this point I have all data files containing the old object key 
			iterator = files.iterator();
			while(iterator.hasNext()) {
				String filePath  = iterator.next().getPath();		
				String tmpFilePath = FileUtils.renameFile(filePath, GDE.FILE_ENDING_TMP); // rename existing file to *.tmp
				File tmpFile = new File(tmpFilePath);
				File updatedFile = new File(filePath);
				DataInputStream data_in = null;
				DataOutputStream data_out = null;

				try {
					long filePointer = 0;
					String tmpData;
					data_in  = new DataInputStream(new FileInputStream(tmpFile));
					data_out = new DataOutputStream(new FileOutputStream(updatedFile));
					
					while(!(tmpData = data_in.readUTF()).startsWith(GDE.OBJECT_KEY)) {
						data_out.writeUTF(tmpData);
						filePointer += tmpData.getBytes("UTF8").length; //$NON-NLS-1$
					}
					//write the new object key
					StringBuilder sb = new StringBuilder();
					sb.append(GDE.OBJECT_KEY).append(newObjectKey).append(GDE.STRING_NEW_LINE);
					data_out.writeUTF(sb.toString());
					filePointer += GDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
					
					tmpData = data_in.readUTF();
					data_out.writeUTF(tmpData);
					filePointer += tmpData.getBytes("UTF8").length; //$NON-NLS-1$
					
					int numberRecordSets = 0;
					if (tmpData.startsWith(GDE.RECORD_SET_SIZE)) {
						numberRecordSets = new Integer(tmpData.substring(GDE.RECORD_SET_SIZE.length()).trim()).intValue();
					}
					else {
						throw new Exception();
					}
					int deltaSizeObjectKey = newObjectKey.getBytes("UTF8").length - oldObjectKey.getBytes("UTF8").length;

					//RecordSetName :: ChannelConfigurationName :: RecordSetComment :: RecordSetProperties :: RecordDataSize :: RecordSetDataPointer
					for (int i=0; i<numberRecordSets; ++i) {
						tmpData = data_in.readUTF();
						long dataPointer = Long.parseLong(tmpData.substring(tmpData.length()-11).trim());
						tmpData = tmpData.substring(0, tmpData.length()-11) + String.format("%10s\n", dataPointer + deltaSizeObjectKey);
						data_out.writeUTF(tmpData);
					}

					//read/write until EOF binary data
					byte[] buffer = new byte[4096];
					int len = 0;
					while((len = data_in.read(buffer)) > 0) {
						data_out.write(buffer, 0, len);
					}

					data_out.close();
					data_in.close();

					FileUtils.renameFile(tmpFilePath, GDE.FILE_ENDING_BAK); // rename existing file to *.bak
				}
				catch (Exception e) {
					try {
						if(data_out != null) data_out.close();
						if (updatedFile.exists()) 
							if(updatedFile.delete())
								log.log(Level.WARNING, "failed to delete " + filePath);
						if(data_in != null) data_in.close();
					}
					catch (IOException e1) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
					FileUtils.renameFile(tmpFilePath, GDE.FILE_ENDING_OSD); // rename existing file to *.osd
				}

			}
		}
		catch (FileNotFoundException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0038, new Object[] {e.getMessage()}));
		}
	}
}
