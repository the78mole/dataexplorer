/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.io;

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
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.OSDE;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.ChannelTypes;
import osde.device.IDevice;
import osde.exception.DataInconsitsentException;
import osde.exception.NotSupportedFileFormatException;
import osde.exception.OSDEInternalException;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.menu.MenuToolBar;
import osde.utils.OperatingSystemHelper;
import osde.utils.StringHelper;

/**
 * @author Winfried Brügmann
 * This class reads and writes OpenSerialData format
 */
public class OsdReaderWriter {
	final static Logger									log												= Logger.getLogger(OsdReaderWriter.class.getName());

	final static OpenSerialDataExplorer	application								= OpenSerialDataExplorer.getInstance();
	final static Channels 							channels 									= Channels.getInstance();


	/**
	 * get open serial data file header data
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
		int headerCounter = OSDE.OSD_FORMAT_HEADER_KEYS.length+1;
		
		line = data_in.readUTF();	
		line = line.substring(0, line.length()-1);
		log.log(Level.FINE, line);
		if (!line.startsWith(OSDE.OPEN_SERIAL_DATA_VERSION))
			throw new NotSupportedFileFormatException(filePath);
		
		String sVersion = line.substring(OSDE.OPEN_SERIAL_DATA_VERSION.length(), OSDE.OPEN_SERIAL_DATA_VERSION.length()+1).trim();
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
			header.put(OSDE.OPEN_SERIAL_DATA_VERSION, OSDE.STRING_EMPTY+version);
			boolean isHeaderComplete = false;
			while (!isHeaderComplete && headerCounter-- > 0) {
				line = data_in.readUTF();
				line = line.substring(0, line.length() - 1);
				log.log(Level.FINE, line);
				for (String headerKey : OSDE.OSD_FORMAT_HEADER_KEYS) {
					if (line.startsWith(headerKey)) {
						log.log(Level.FINE, line);
						header.put(headerKey, line.substring(headerKey.length()));
						if (line.startsWith(OSDE.RECORD_SET_SIZE)) {
							headerCounter = new Integer(header.get(OSDE.RECORD_SET_SIZE).trim()).intValue();
							//read record set descriptors
							int lastReordNumber = headerCounter;
							while (headerCounter-- > 0) {
								line = data_in.readUTF();
								line = line.substring(0, line.length() - 1);
								if (line.startsWith(OSDE.RECORD_SET_NAME)) {
									log.log(Level.FINE, line);
									header.put((lastReordNumber-headerCounter)+OSDE.STRING_BLANK+OSDE.RECORD_SET_NAME, line.substring(OSDE.RECORD_SET_NAME.length()));
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
		String channelConfig = OSDE.STRING_EMPTY;
		String recordSetName = OSDE.STRING_EMPTY;
		String recordSetComment = OSDE.STRING_EMPTY;
		String recordSetProperties = OSDE.STRING_EMPTY;
		String[] recordsProperties;
		int recordDataSize = 0;
		long recordSetDataPointer = 0;
		Channel channel = null;
		RecordSet recordSet = null;
		IDevice device = OsdReaderWriter.application.getActiveDevice();
		String line;
		boolean isFirstRecordSetDisplayed = false;
		
		HashMap<String, String> header = getHeader(filePath);
		String channelType = header.get(OSDE.CHANNEL_CONFIG_TYPE).trim();
		String objectKey = header.get(OSDE.OBJECT_KEY) != null ? header.get(OSDE.OBJECT_KEY) : OSDE.STRING_EMPTY;
		int numberRecordSets = new Integer(header.get(OSDE.RECORD_SET_SIZE).trim()).intValue();
		while(!data_in.readUTF().startsWith(OSDE.RECORD_SET_SIZE))
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
				channelConfig = recordSetInfo.get(OSDE.CHANNEL_CONFIG_NAME);
				recordSetName = recordSetInfo.get(OSDE.RECORD_SET_NAME);
				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
				recordSetComment = recordSetInfo.get(OSDE.RECORD_SET_COMMENT);
				recordSetProperties = recordSetInfo.get(OSDE.RECORD_SET_PROPERTIES);
				recordsProperties = StringHelper.splitString(recordSetInfo.get(OSDE.RECORDS_PROPERTIES), Record.END_MARKER, OSDE.RECORDS_PROPERTIES);
				recordDataSize = new Long(recordSetInfo.get(OSDE.RECORD_DATA_SIZE)).intValue();
				//recordSetDataPointer = new Long(recordSetInfo.get(RECORD_SET_DATA_POINTER)).longValue();
				channel = channels.get(channels.getChannelNumber(channelConfig));
				if (channel == null) { // 1.st try channelConfiguration not found
					try { // get channel last digit and use as channel config ordinal
						channel = channels.get(new Integer(channelConfig.substring(channelConfig.length()-1)));
						channelConfig = channel.getConfigKey();
						recordSetInfo.put(OSDE.CHANNEL_CONFIG_NAME, channelConfig);
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
						channelConfig = channel.getConfigKey();
						recordSetInfo.put(OSDE.CHANNEL_CONFIG_NAME, channelConfig);
					}
					catch (NullPointerException e) {
						// ignore and keep channel as null
					}
				}
				if (channel == null) { // 3.rd try channelConfiguration not found
					String msg = Messages.getString(MessageIds.OSDE_MSGI0018, new Object[] { recordSetName }) + Messages.getString(MessageIds.OSDE_MSGI0019) + Messages.getString(MessageIds.OSDE_MSGI0020);
					OpenSerialDataExplorer.getInstance().openMessageDialogAsync(msg);
					int newChannelNumber = channels.size() + 1;
					channel = new Channel(newChannelNumber, channelConfig, ChannelTypes.valueOf(channelType).ordinal());
					// do not allocate records to record set - newChannel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, activeConfig));
					channels.put(newChannelNumber, channel);
					Vector<String> newChannelNames = new Vector<String>();
					for (String channelConfigKey : channels.getChannelNames()) {
						newChannelNames.add(channelConfigKey);
					}
					newChannelNames.add(newChannelNumber + " : " + channelConfig); //$NON-NLS-1$
					channels.setChannelNames(newChannelNames.toArray(new String[1]));
				}
				channels.setActiveChannelNumber(channel.getOrdinal());
				channel.setObjectKey(objectKey);
				// "3 : Motor"
				channelConfig = channelConfig.contains(OSDE.STRING_COLON) ? channelConfig.split(OSDE.STRING_COLON)[1].trim() : channelConfig.trim();
				// "Motor 3"
				channelConfig = channelConfig.contains(OSDE.STRING_BLANK) ? channelConfig.split(OSDE.STRING_BLANK)[0].trim() : channelConfig.trim();
				//"Motor"
				recordSet = RecordSet.createRecordSet(recordSetName, device, channelConfig, true, true);
				//apply record sets properties
				recordSet.setRecordSetDescription(recordSetComment);

				//apply record sets records properties
				if (log.isLoggable(Level.FINE)) {
					StringBuilder sb = new StringBuilder().append(recordSet.getName()).append(OSDE.STRING_MESSAGE_CONCAT);
					for (String recordProps : recordsProperties) {
						sb.append(StringHelper.splitString(recordProps, Record.DELIMITER, Record.propertyKeys).get(Record.propertyKeys[0])).append(OSDE.STRING_COMMA);
					}
					log.log(Level.FINE, sb.toString());
				}
				String [] recordKeys = recordSet.getRecordNames();
				// check if the file content fits measurements form device properties XML which was used to create the record set
				if (recordsProperties.length != recordKeys.length) { 
					for (int j = recordsProperties.length; j < recordKeys.length; j++) {
						recordSet.remove(recordKeys[j]);
					}
					recordKeys = recordSet.getRecordNames();
				}
				for (int i = 0; i < recordKeys.length; ++i) {
					Record record = recordSet.get(recordKeys[i]);
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
				channelConfig = recordSetInfo.get(OSDE.CHANNEL_CONFIG_NAME);
				recordSetName = recordSetInfo.get(OSDE.RECORD_SET_NAME);
				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
				if (firstRecordSet[0] == null || firstRecordSet[1] == null) {
					firstRecordSet[0] = channelConfig;
					firstRecordSet[1] = recordSetName;
				}
				recordDataSize = new Long(recordSetInfo.get(OSDE.RECORD_DATA_SIZE)).intValue();
				log.log(Level.FINE, "recordDataSize = " + recordDataSize);
				recordSetDataPointer = new Long(recordSetInfo.get(OSDE.RECORD_SET_DATA_POINTER)).longValue();
				log.log(Level.FINE, "recordSetDataPointer = " + recordSetDataPointer);
				channel = channels.get(channels.getChannelNumber(channelConfig));
				recordSet = channel.get(recordSetName);
				recordSet.setFileDataPointerAndSize(recordSetDataPointer, recordDataSize, OSDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length  * recordDataSize);
				if (recordSetName.equals(firstRecordSet[1])) {
					long startTime = new Date().getTime();
					byte[] buffer = new byte[recordSet.getFileDataBytesSize()];
					data_in.readFully(buffer);
					recordSet.getDevice().addDataBufferAsRawDataPoints(recordSet, buffer, recordDataSize, application.getStatusBar() != null);
					log.log(Level.FINE, "read time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
				}
				// display the first record set data while reading the rest of the data
				if (!isFirstRecordSetDisplayed && firstRecordSet[0] != null && firstRecordSet[1] != null && application.getMenuToolBar() != null) {
					isFirstRecordSetDisplayed = true;
					channel.setFileName(filePath);
					channel.setFileDescription(header.get(OSDE.FILE_COMMENT));
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
		return StringHelper.splitString(recordSetProperties, OSDE.DATA_DELIMITER, OSDE.OSD_FORMAT_DATA_KEYS);
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
				String versionString = OSDE.OPEN_SERIAL_DATA_VERSION + useVersion + OSDE.STRING_NEW_LINE;
				data_out.writeUTF(versionString);
				filePointer += OSDE.SIZE_UTF_SIGNATURE + versionString.getBytes("UTF8").length; //$NON-NLS-1$
				log.log(Level.FINE, "line lenght = " + (OSDE.SIZE_UTF_SIGNATURE + versionString.getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				//creation time stamp
				StringBuilder sb = new StringBuilder();
				sb.append(OSDE.CREATION_TIME_STAMP).append(new SimpleDateFormat("yyyy-MM-dd").format(new Date())).append(' '); //$NON-NLS-1$
				sb.append(new SimpleDateFormat(" HH:mm:ss").format(new Date().getTime())).append(OSDE.STRING_NEW_LINE); //$NON-NLS-1$
				data_out.writeUTF(sb.toString());
				filePointer += OSDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				log.log(Level.FINE, "line lenght = " + (OSDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// second line : size file comment , file comment
				sb = new StringBuilder();
				sb.append(OSDE.FILE_COMMENT).append(activeChannel.getFileDescription()).append(OSDE.STRING_NEW_LINE);
				data_out.writeUTF(sb.toString());
				filePointer += OSDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				log.log(Level.FINE, "line lenght = " + (OSDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// third line : size device name , device name
				sb = new StringBuilder();
				sb.append(OSDE.DEVICE_NAME).append(activeDevice.getName()).append(OSDE.STRING_NEW_LINE);
				data_out.writeUTF(sb.toString());
				filePointer += OSDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				log.log(Level.FINE, "line lenght = " + (OSDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// fourth line : size channel/config type , channel/config type
				sb = new StringBuilder();
				sb.append(OSDE.CHANNEL_CONFIG_TYPE).append(ChannelTypes.values()[Channels.getInstance().getActiveChannel().getType()]).append(OSDE.STRING_NEW_LINE);
				data_out.writeUTF(sb.toString());
				filePointer += OSDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				log.log(Level.FINE, "line lenght = " + (OSDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// fifth line : object key
				sb = new StringBuilder();
				sb.append(OSDE.OBJECT_KEY).append(activeChannel.getObjectKey()).append(OSDE.STRING_NEW_LINE);
				data_out.writeUTF(sb.toString());
				filePointer += OSDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				log.log(Level.FINE, "line lenght = " + (OSDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// number of record sets
				sb = new StringBuilder();
				sb.append(OSDE.RECORD_SET_SIZE).append(activeChannel.size()).append(OSDE.STRING_NEW_LINE);
				data_out.writeUTF(sb.toString());
				filePointer += OSDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length; //$NON-NLS-1$
				log.log(Level.FINE, "line lenght = " + (OSDE.SIZE_UTF_SIGNATURE + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// record sets with it properties
				StringBuilder[] sbs = new StringBuilder[activeChannel.size()];
				String[] recordSetNames = activeChannel.getRecordSetNames();
				// prepare all record set describing data
				for (int i = 0; i < activeChannel.size(); ++i) {
					// channel/configuration :|: record set name :|: recordSet description :|: recordSet properties :|: all records properties :|: record size :|: data begin pointer 
					Channel recordSetChannel = Channels.getInstance().get(activeChannel.findChannelOfRecordSet(recordSetNames[i]));
					if (recordSetChannel != null) {
						RecordSet recordSet = recordSetChannel.get(recordSetNames[i]);
						if (recordSet != null) {
							sbs[i] = new StringBuilder();
							sbs[i].append(OSDE.RECORD_SET_NAME).append(recordSet.getName()).append(OSDE.DATA_DELIMITER)
								.append(OSDE.CHANNEL_CONFIG_NAME).append(recordSetChannel.getOrdinal()).append(OSDE.STRING_BLANK_COLON_BLANK).append(recordSet.getChannelConfigName()).append(OSDE.DATA_DELIMITER)
								.append(OSDE.RECORD_SET_COMMENT).append(recordSet.getRecordSetDescription()).append(OSDE.DATA_DELIMITER)
								.append(OSDE.RECORD_SET_PROPERTIES).append(recordSet.getSerializeProperties()).append(OSDE.DATA_DELIMITER);
							// serialized recordSet configuration data (record names, unit, symbol, isActive, ....) size data points , pointer data start or file name
							for (String recordKey : recordSet.getRecordNames()) {
								sbs[i].append(OSDE.RECORDS_PROPERTIES).append(recordSet.get(recordKey).getSerializeProperties());
							}
							sbs[i].append(OSDE.DATA_DELIMITER).append(OSDE.RECORD_DATA_SIZE).append(String.format("%10s", recordSet.getRecordDataSize(true))).append(OSDE.DATA_DELIMITER); //$NON-NLS-1$
							filePointer += OSDE.SIZE_UTF_SIGNATURE + sbs[i].toString().getBytes("UTF8").length; //$NON-NLS-1$
							filePointer += OSDE.RECORD_SET_DATA_POINTER.toString().getBytes("UTF8").length + 10 + OSDE.STRING_NEW_LINE.toString().getBytes("UTF8").length; // pre calculated size //$NON-NLS-1$ //$NON-NLS-2$
							log.log(Level.FINE, "line lenght = " //$NON-NLS-1$
								+ (OSDE.SIZE_UTF_SIGNATURE + sbs[i].toString().getBytes("UTF8").length + OSDE.RECORD_SET_DATA_POINTER.toString().getBytes("UTF8").length + 10 + OSDE.STRING_NEW_LINE.toString().getBytes("UTF8").length) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								+ " filePointer = " + filePointer); //$NON-NLS-1$
							
							if (log.isLoggable(Level.FINE)) {
								StringBuilder sb1 = new StringBuilder().append(recordSet.getName()).append(OSDE.STRING_MESSAGE_CONCAT);
								for (String recordName : recordSet.getRecordNames()) {
									sb1.append(recordName).append(OSDE.STRING_COMMA);
								}
								log.log(Level.FINE, sb1.toString());
							}
						}
					}
				}
				// prepare all record set data pointer
				for (int i = 0; i < activeChannel.size(); ++i) {
					// channel/configuration :: record set name :: recordSet description :: data pointer 
					Channel recordSetChannel = Channels.getInstance().get(activeChannel.findChannelOfRecordSet(recordSetNames[i]));
					if (recordSetChannel != null) {
						RecordSet recordSet = recordSetChannel.get(recordSetNames[i]);
						if (recordSet != null) {
							recordSet.resetZoomAndMeasurement(); // make sure size() returns right value
							sbs[i].append(OSDE.RECORD_SET_DATA_POINTER).append(String.format("%10s", filePointer)).append(OSDE.STRING_NEW_LINE); //$NON-NLS-1$
							log.log(Level.FINE, sbs[i].toString());
							//data_out.writeInt(sbs[i].length());
							data_out.writeUTF(sbs[i].toString());
							filePointer += (recordSet.getNoneCalculationRecordNames().length * OSDE.SIZE_BYTES_INTEGER * recordSet.getRecordDataSize(true));
							log.log(Level.FINE, "filePointer = " + filePointer); //$NON-NLS-1$
						}
					}
				}
				// check if all involved record sets have data (if loaded from file it meight be possible that some record set lack of its data)
				long startTime = new Date().getTime();
				for (int i = 0; i < activeChannel.size(); ++i) {
					Channel recordSetChannel = Channels.getInstance().get(activeChannel.findChannelOfRecordSet(recordSetNames[i]));
					if (recordSetChannel != null) {
						RecordSet recordSet = recordSetChannel.get(recordSetNames[i]);
						if (recordSet != null) {
							if (!recordSet.hasDisplayableData()) recordSet.loadFileData(recordSetChannel.getFullQualifiedFileName(), application.getStatusBar() != null);
							String[] noneCalculationRecordNames = recordSet.getNoneCalculationRecordNames();
							byte[] buffer = new byte[OSDE.SIZE_BYTES_INTEGER * recordSet.getRecordDataSize(true) * noneCalculationRecordNames.length];
							byte[] bytes = new byte[OSDE.SIZE_BYTES_INTEGER];
							if (recordSet.isRaw()) {
								for (int j = 0, l = 0; j < recordSet.getRecordDataSize(true); ++j) {
									for (int k = 0; k < noneCalculationRecordNames.length; ++k, l += OSDE.SIZE_BYTES_INTEGER) {
										int point = recordSet.get(noneCalculationRecordNames[k]).realGet(j);
										//log.log(Level.FINER, ""+point);
										bytes[0] = (byte) ((point >>> 24) & 0xFF);
										bytes[1] = (byte) ((point >>> 16) & 0xFF);
										bytes[2] = (byte) ((point >>> 8) & 0xFF);
										bytes[3] = (byte) ((point >>> 0) & 0xFF);
										System.arraycopy(bytes, 0, buffer, l, OSDE.SIZE_BYTES_INTEGER);
									}
								}
							}
							else {
								IDevice device = recordSet.getDevice();
								for (int j = 0, l = 0; j < recordSet.getRecordDataSize(true); ++j) {
									for (int k = 0; k < noneCalculationRecordNames.length; ++k, l += OSDE.SIZE_BYTES_INTEGER) {
										Record record = recordSet.get(noneCalculationRecordNames[k]);
										int point = new Double(device.reverseTranslateValue(record, record.realGet(j)/1000.0)*1000.0).intValue();
										//log.log(Level.FINER, ""+point);
										bytes[0] = (byte) ((point >>> 24) & 0xFF);
										bytes[1] = (byte) ((point >>> 16) & 0xFF);
										bytes[2] = (byte) ((point >>> 8) & 0xFF);
										bytes[3] = (byte) ((point >>> 0) & 0xFF);
										System.arraycopy(bytes, 0, buffer, l, OSDE.SIZE_BYTES_INTEGER);
									}
								}
							}
							data_out.write(buffer, 0, buffer.length);
							recordSet.setSaved(true);
						}
					}
				}
				log.log(Level.FINE, "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
				
				//update/write link if object oriented
				if (isObjectOriented) {
					OperatingSystemHelper.createFileLink(fullQualifiedFilePath, 
							OsdReaderWriter.application.getObjectFilePath() + fullQualifiedFilePath.substring(fullQualifiedFilePath.lastIndexOf(OSDE.FILE_SEPARATOR_UNIX)+1));
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
			OSDEInternalException e = new OSDEInternalException(Messages.getString(MessageIds.OSDE_MSGE0009) + activeChannel + ", " + fullQualifiedFilePath + ", " + useVersion); //$NON-NLS-1$
			OpenSerialDataExplorer.getInstance().openMessageDialogAsync(e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage());
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
			int deviceDataBufferSize = OSDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
			byte[] buffer = new byte[deviceDataBufferSize * recordFileDataSize];
			random_in.readFully(buffer);
			recordSet.getDevice().addDataBufferAsRawDataPoints(recordSet, buffer, recordFileDataSize, doUpdateProgressBar);
			log.log(Level.FINE, "read time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
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
}
