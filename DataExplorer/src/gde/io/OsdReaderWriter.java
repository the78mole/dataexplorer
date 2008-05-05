/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.ChannelTypes;
import osde.device.IDevice;
import osde.exception.DeclinedException;
import osde.exception.NotSupportedFileFormat;
import osde.exception.OSDEInternalException;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.StringHelper;

/**
 * @author Winfried Brügmann
 * This class reads and writes OpenSerialData format
 */
public class OsdReaderWriter {
	final static Logger									log												= Logger.getLogger(OsdReaderWriter.class.getName());

	public static final String									OPEN_SERIAL_DATA_VERSION	= "OpenSerialData version : ";

	public static final String									CREATION_TIME_STAMP				= "Created : ";
	public static final String									FILE_COMMENT							= "FileComment : ";
	public static final String									DEVICE_NAME								= "DeviceName : ";
	public static final String									CHANNEL_CONFIG_TYPE				= "Channel/Configuration Type : ";
	public static final String									RECORD_SET_SIZE						= "NumberRecordSets : ";
	
	static final String									RECORD_SET_NAME						= "RecordSetName : ";
	static final String									CHANNEL_CONFIG_NAME				= "Channel/Configuration Name: ";
	static final String									OBJECT_KEY								= "ObjectKey : ";
	static final String									RECORD_SET_COMMENT				= "RecordSetComment : ";
	static final String									RECORD_SET_PROPERTIES			= "RecordSetProperties : ";
	static final String									RECORDS_PROPERTIES				= "RecordProperties : ";
	static final String									RECORD_DATA_SIZE					= "RecordDataSize : ";
	static final String									RECORD_SET_DATA_POINTER		= "RecordSetDataPointer : ";
	static final String[]								OSD_FORMAT_HEADER_KEYS		= new String[] {CREATION_TIME_STAMP, FILE_COMMENT, DEVICE_NAME, CHANNEL_CONFIG_TYPE, RECORD_SET_SIZE};
	static final String[]								OSD_FORMAT_DATA_KEYS			= new String[] {OBJECT_KEY, CHANNEL_CONFIG_NAME, RECORD_SET_NAME, RECORD_SET_COMMENT, RECORD_SET_PROPERTIES, RECORDS_PROPERTIES, RECORD_DATA_SIZE, RECORD_SET_DATA_POINTER};

	static final String									DATA_DELIMITER						= "||::||";
	final static String									lineSep										= "\n";	//System.getProperty("line.separator") is OS dependent
	final static int										intSize										= Integer.SIZE/8;		// 32 bits / 8 bits per byte 
	final static int										utfSigSize								= 2;		// 2 byte UTF line header 

	final static OpenSerialDataExplorer	application								= OpenSerialDataExplorer.getInstance();
	final static Channels 							channels 									= Channels.getInstance();

	
	public static HashMap<String, String> getHeader(String filePath) throws FileNotFoundException, IOException, NotSupportedFileFormat {
		FileInputStream file_input = new FileInputStream(new File(filePath));
		DataInputStream data_in    = new DataInputStream(file_input);
		return readHeader(filePath, data_in);
	}

	private static HashMap<String, String> readHeader(final String filePath, DataInputStream data_in) throws IOException, NotSupportedFileFormat {
		String line;
		HashMap<String, String> header = new HashMap<String, String>();
		int headerCounter = OSD_FORMAT_HEADER_KEYS.length+1;
		
		line = data_in.readUTF();	
		line = line.substring(0, line.length()-1);
		log.fine(line);
		if (!line.startsWith(OPEN_SERIAL_DATA_VERSION))
			throw new NotSupportedFileFormat(filePath);
		
		int version = new Integer(line.substring(OPEN_SERIAL_DATA_VERSION.length())).intValue();
		switch (version) {
		case 1:
			header.put(OPEN_SERIAL_DATA_VERSION, ""+version);
			
			while (headerCounter-- > 0) {
				line = data_in.readUTF();
				line = line.substring(0, line.length() - 1);
				log.fine(line);
				for (String headerKey : OSD_FORMAT_HEADER_KEYS) {
					if (line.startsWith(headerKey)) {
						log.fine(line);
						header.put(headerKey, line.substring(headerKey.length()));
						if (line.startsWith(RECORD_SET_SIZE)) {
							headerCounter = new Integer(header.get(RECORD_SET_SIZE).trim()).intValue();
							//read record set descriptors
							int lastReordNumber = headerCounter;
							while (headerCounter-- > 0) {
								line = data_in.readUTF();
								line = line.substring(0, line.length() - 1);
								if (line.startsWith(RECORD_SET_NAME)) {
									log.fine(line);
									header.put((lastReordNumber-headerCounter)+" "+RECORD_SET_NAME, line.substring(RECORD_SET_NAME.length()));
								}
							}
						}
						break;
					}
				}
			}
			break;

		default:
			throw new NotSupportedFileFormat(filePath);
		}

		return header;
	}	
	
	/**
	 * @param filePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NotSupportedFileFormat 
	 * @throws DeclinedException 
	 */
	public static RecordSet read(String filePath) throws FileNotFoundException, IOException, NotSupportedFileFormat {
		FileInputStream file_input = new FileInputStream(new File(filePath));
		DataInputStream data_in    = new DataInputStream(file_input);
		String channelConfig = "";
		String recordSetName = "";
		String recordSetComment = "";
		String recordSetProperties = "";
		String[] recordsProperties;
		long recordDataSize = 0;
		Channel channel = null;
		RecordSet recordSet = null;
		IDevice device = OsdReaderWriter.application.getActiveDevice();
		String line;
		boolean isFirstRecordSetDisplayed = false;
		
		HashMap<String, String> header = getHeader(filePath);
		String channelType = header.get(CHANNEL_CONFIG_TYPE).trim();
		int numberRecordSets = new Integer(header.get(RECORD_SET_SIZE).trim()).intValue();
		while(!data_in.readUTF().startsWith(RECORD_SET_SIZE))
			log.fine("skip");
		
		// record sets with it properties
		List<HashMap<String,String>> recordSetsInfo = new ArrayList<HashMap<String,String>>();
		for (int i=0; i<numberRecordSets; ++i) {
			// channel/configuration :: record set name :: recordSet description :: data pointer :: properties
			line = data_in.readUTF();	
			line = line.substring(0, line.length()-1);
			recordSetsInfo.add(StringHelper.splitString(line, DATA_DELIMITER, OSD_FORMAT_DATA_KEYS));
		}

		try { // build the data structure 
			
			for (HashMap<String,String> recordSetInfo : recordSetsInfo) {
				channelConfig = recordSetInfo.get(CHANNEL_CONFIG_NAME);
				recordSetName = recordSetInfo.get(RECORD_SET_NAME);
				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
				recordSetComment = recordSetInfo.get(RECORD_SET_COMMENT);
				recordSetProperties = recordSetInfo.get(RECORD_SET_PROPERTIES);
				recordsProperties = StringHelper.splitString(recordSetInfo.get(RECORDS_PROPERTIES), Record.END_MARKER, RECORDS_PROPERTIES);
				recordDataSize = new Long(recordSetInfo.get(RECORD_DATA_SIZE)).longValue();
				//recordSetDataPointer = new Long(recordSetInfo.get(RECORD_SET_DATA_POINTER)).longValue();
				channel = channels.get(channels.getChannelNumber(channelConfig));
				if (channel == null) { // channelConfiguration not found
					String msg = "Die Kanalkonfiguration des Datensatzes " + recordSetName + " entspricht keiner aktuell vorhandenen.\n"
						+ "Eine Änderung von Einstellungen ist nur nach Übertrag in eine vorhandene Kanalkonfiguration möglich.\n" 
						+ "Hinweis: Ein umstellen der KanalKonfiguration is über den Gerätedialog möglich.";
					OpenSerialDataExplorer.getInstance().openMessageDialogAsync(msg);
					int newChannelNumber = channels.size()+ 1;
					channel = new Channel(newChannelNumber, channelConfig, ChannelTypes.valueOf(channelType).ordinal());
					// do not allocate records to record set - newChannel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, activeConfig));
					channels.put(newChannelNumber, channel);
					Vector<String> newChannelNames = new Vector<String>();
					for(String channelConfigKey : channels.getChannelNames()) {
						newChannelNames.add(channelConfigKey);
					}
					newChannelNames.add(newChannelNumber + " : " + channelConfig);
					channels.setChannelNames(newChannelNames.toArray(new String[1]));
				}
				recordSet = RecordSet.createRecordSet(channelConfig, recordSetName, device, true, true);
				//apply record sets properties
				recordSet.setRecordSetDescription(recordSetComment);
				recordSet.setDeserializedProperties(recordSetProperties);
				recordSet.setSaved(true);
				recordSet.setObjectKey(recordSetInfo.get(OBJECT_KEY));

				//apply record sets records properties
				String [] recordKeys = recordSet.getRecordNames();
				for (int i = 0; i < recordKeys.length; ++i) {
					Record record = recordSet.get(recordKeys[i]);
					record.setSerializedProperties(recordsProperties[i]);
					record.setSerializedDeviceSpecificProperties(recordsProperties[i]);
				}
				
				channel.put(recordSetName, recordSet);
			}
			OsdReaderWriter.application.getMenuToolBar().updateChannelSelector();
			OsdReaderWriter.application.getMenuToolBar().updateRecordSetSelectCombo();

			String[] firstRecordSet = new String[2];
			for (HashMap<String,String> recordSetInfo : recordSetsInfo) {
				channelConfig = recordSetInfo.get(CHANNEL_CONFIG_NAME);
				recordSetName = recordSetInfo.get(RECORD_SET_NAME);
				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
				if (firstRecordSet[0] == null || firstRecordSet[1] == null) {
					firstRecordSet[0] = channelConfig;
					firstRecordSet[1] = recordSetName;
				}
				recordDataSize = new Long(recordSetInfo.get(RECORD_DATA_SIZE)).longValue();
				//recordSetDataPointer = new Long(recordSetInfo.get(RECORD_SET_DATA_POINTER)).longValue();
				channel = channels.get(channels.getChannelNumber(channelConfig));
				recordSet = channel.get(recordSetName);
				for (int i = 0; i < recordDataSize; i++) {
					for (String recordKey : recordSet.getNoneCalculationRecordNames()) {
						recordSet.get(recordKey).add(data_in.readInt());
					}
				}
				// display the first record set data while reading the rest of the data
				if (!isFirstRecordSetDisplayed && firstRecordSet[0] != null && firstRecordSet[1] != null) {
					isFirstRecordSetDisplayed = true;
					channels.setFullQualifiedFileName(filePath);
					channels.setFileDescription(header.get(FILE_COMMENT));
					channels.setSaved(true);
					channels.switchChannel(channels.getChannelNumber(firstRecordSet[0]), firstRecordSet[1]);
				}
			}
					
			data_in.close ();
		}
		catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			return recordSet;
	}

	/**
	 * write channel data to osd file format
	 * @param filePath
	 * @param activeChannel
	 * @param useVersion
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void write(String filePath, Channel activeChannel, int useVersion) throws FileNotFoundException, IOException {
		if (activeChannel != null && filePath != null && useVersion != 0) {
			FileOutputStream file_out = new FileOutputStream(new File(filePath));
			DataOutputStream data_out = new DataOutputStream(file_out);
			IDevice activeDevice = OsdReaderWriter.application.getActiveDevice();
			int filePointer = 0;
			// first line : header with version
			String versionString = OPEN_SERIAL_DATA_VERSION + useVersion + lineSep;
			data_out.writeUTF(versionString);
			filePointer += utfSigSize + versionString.getBytes("UTF8").length;
			log.fine("line lenght = " + (utfSigSize + versionString.getBytes("UTF8").length) + " filePointer = " + filePointer);
			//creation time stamp
			StringBuilder sb = new StringBuilder();
			sb.append(CREATION_TIME_STAMP).append(new SimpleDateFormat("yyyy-MM-dd").format(new Date())).append(' ');
			sb.append(new SimpleDateFormat(" HH:mm:ss").format(new Date().getTime())).append(lineSep);
			data_out.writeUTF(sb.toString());
			filePointer += utfSigSize + sb.toString().getBytes("UTF8").length;
			log.fine("line lenght = " + (utfSigSize + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer);
			// second line : size file comment , file comment
			sb = new StringBuilder();
			sb.append(FILE_COMMENT).append(Channels.getInstance().getFileDescription()).append(lineSep);
			data_out.writeUTF(sb.toString());
			filePointer += utfSigSize + sb.toString().getBytes("UTF8").length;
			log.fine("line lenght = " + (utfSigSize + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer);
			// third line : size device name , device name
			sb = new StringBuilder();
			sb.append(DEVICE_NAME).append(activeDevice.getName()).append(lineSep);
			data_out.writeUTF(sb.toString());
			filePointer += utfSigSize + sb.toString().getBytes("UTF8").length;
			log.fine("line lenght = " + (utfSigSize + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer);
			// fourth line : size channel/config type , channel/config type
			sb = new StringBuilder();
			sb.append(CHANNEL_CONFIG_TYPE).append(ChannelTypes.values()[activeDevice.getChannelType(Channels.getInstance().getActiveChannelNumber())]).append(lineSep);
			data_out.writeUTF(sb.toString());
			filePointer += utfSigSize + sb.toString().getBytes("UTF8").length;
			log.fine("line lenght = " + (utfSigSize + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer);
			// number of record sets
			sb = new StringBuilder();
			sb.append(RECORD_SET_SIZE).append(activeChannel.size()).append(lineSep);
			data_out.writeUTF(sb.toString());
			filePointer += utfSigSize + sb.toString().getBytes("UTF8").length;
			log.fine("line lenght = " + (utfSigSize + sb.toString().getBytes("UTF8").length) + " filePointer = " + filePointer);
			// record sets with it properties
			StringBuilder[] sbs = new StringBuilder[activeChannel.size()];
			String[] recordSetNames = activeChannel.getRecordSetNames();
			// prepare all record set lines without the to be calculated data pointer
			for (int i = 0; i < activeChannel.size(); ++i) {
				// channel/configuration :|: record set name :|: recordSet description :|: recordSet properties :|: all records properties :|: record size :|: data begin pointer 
				Channel recordSetChannel = Channels.getInstance().get(activeChannel.findChannelOfRecordSet(recordSetNames[i]));
				RecordSet recordSet = recordSetChannel.get(recordSetNames[i]);
				sbs[i] = new StringBuilder();
				sbs[i].append(RECORD_SET_NAME).append(recordSet.getName()).append(DATA_DELIMITER).append(CHANNEL_CONFIG_NAME).append(recordSet.getChannelConfigName()).append(DATA_DELIMITER)
						.append(OBJECT_KEY).append(recordSet.getObjectKey()).append(DATA_DELIMITER).append(RECORD_SET_COMMENT).append(recordSet.getRecordSetDescription()).append(DATA_DELIMITER).append(
								RECORD_SET_PROPERTIES).append(recordSet.getSerializeProperties()).append(DATA_DELIMITER);
				// serialized recordSet configuration data (record names, unit, symbol, isActive, ....) size data points , pointer data start or file name
				for (String recordKey : recordSet.getRecordNames()) {
					sbs[i].append(RECORDS_PROPERTIES).append(recordSet.get(recordKey).getSerializeProperties());
				}
				sbs[i].append(DATA_DELIMITER).append(RECORD_DATA_SIZE).append(String.format("%10s", recordSet.getRecordDataSize())).append(DATA_DELIMITER);
				filePointer += utfSigSize + sbs[i].toString().getBytes("UTF8").length;
				filePointer += RECORD_SET_DATA_POINTER.toString().getBytes("UTF8").length + 10 + lineSep.toString().getBytes("UTF8").length; // pre calculated size
				log.fine("line lenght = "
						+ (utfSigSize + sbs[i].toString().getBytes("UTF8").length + RECORD_SET_DATA_POINTER.toString().getBytes("UTF8").length + 10 + lineSep.toString().getBytes("UTF8").length)
						+ " filePointer = " + filePointer);
			}
			int dataSize = 0;
			for (int i = 0; i < activeChannel.size(); ++i) {
				// channel/configuration :: record set name :: recordSet description :: data pointer 
				Channel recordSetChannel = Channels.getInstance().get(activeChannel.findChannelOfRecordSet(recordSetNames[i]));
				RecordSet recordSet = recordSetChannel.get(recordSetNames[i]);
				recordSet.resetZoomAndMeasurement(); // make sure size() returns right value
				sbs[i].append(RECORD_SET_DATA_POINTER).append(String.format("%10s", (dataSize + filePointer))).append(lineSep);
				//data_out.writeInt(sbs[i].length());
				data_out.writeUTF(sbs[i].toString());
				dataSize += (recordSet.getNoneCalculationRecordNames().length * intSize * recordSet.getRecordDataSize());
				log.fine("filePointer = " + (filePointer + dataSize));
			}
			// data integer 1.st raw measurement, 2.nd raw measurement, 3.rd measurement, ....
			for (int i = 0; i < activeChannel.size(); ++i) {
				Channel recordSetChannel = Channels.getInstance().get(activeChannel.findChannelOfRecordSet(recordSetNames[i]));
				RecordSet recordSet = recordSetChannel.get(recordSetNames[i]);
				for (int j = 0; j < recordSet.getRecordDataSize(); j++) {
					for (String recordKey : recordSet.getNoneCalculationRecordNames()) {
						data_out.writeInt(recordSet.get(recordKey).get(j));
					}
				}
				recordSet.setSaved(true);
			}
			data_out.flush();
			data_out.close();
		}
		else {
			OSDEInternalException e = new OSDEInternalException("Aufruffehler : " + activeChannel + ", " + filePath + ", " + useVersion);
			OpenSerialDataExplorer.getInstance().openMessageDialogAsync(e.getClass().getSimpleName() + " - " + e.getMessage());
		}
	}
}
