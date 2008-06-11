package osde.io;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
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
import osde.exception.NotSupportedException;
import osde.exception.NotSupportedFileFormatException;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.StringHelper;

/**
 * @author brueg
 */
public class LogViewReader {	
	final static Logger										log					= Logger.getLogger(LogViewReader.class.getName());

	final static OpenSerialDataExplorer		application	= OpenSerialDataExplorer.getInstance();
	final static Channels 								channels 		= Channels.getInstance();
	final static HashMap<String, String> 	deviceMap		=	new HashMap<String, String>();
	final static HashMap<String, String> 	lov2osdMap	=	new HashMap<String, String>();
	
	// fill device Map with 
	static {
		deviceMap.put("htronic akkumaster c4", "AkkuMasterC4");
		deviceMap.put("picolario", "Picolario");
		deviceMap.put("unilog", "UniLog");
		// add more supported devices here, key in lower case
		
		// UniLog mappings
		lov2osdMap.put(OSDE.LOV_N_100_W, 			"prop_n100W" 	 + "=_" + "INTEGER");
		lov2osdMap.put(OSDE.LOV_NUMBER_CELLS, "number_cells" + "=_" + "INTEGER");
		
		lov2osdMap.put(OSDE.LOV_RPM_ACTIVE, 	Record.IS_ACTIVE	+ "=_" + "BOOLEAN");
		lov2osdMap.put(OSDE.LOV_A1_ACTIVE, 		Record.IS_ACTIVE	+ "=_" + "BOOLEAN");
		lov2osdMap.put(OSDE.LOV_A2_ACTIVE, 		Record.IS_ACTIVE	+ "=_" + "BOOLEAN");
		lov2osdMap.put(OSDE.LOV_A3_ACTIVE, 		Record.IS_ACTIVE	+ "=_" + "BOOLEAN");
		//lov2osdMap.put(OSDE.LOV_RPM_NAME, 		Record.NAME);
		lov2osdMap.put(OSDE.LOV_A1_NAME, 			Record.NAME);
		lov2osdMap.put(OSDE.LOV_A2_NAME, 			Record.NAME);
		lov2osdMap.put(OSDE.LOV_A3_NAME, 			Record.NAME);
		//lov2osdMap.put(OSDE.LOV_RPM_UNIT, 		Record.UNIT);
		lov2osdMap.put(OSDE.LOV_A1_UNIT, 			Record.UNIT);
		lov2osdMap.put(OSDE.LOV_A2_UNIT, 			Record.UNIT);
		lov2osdMap.put(OSDE.LOV_A3_UNIT, 			Record.UNIT);
		lov2osdMap.put(OSDE.LOV_RPM_OFFSET, 	IDevice.OFFSET + "=_" + "DOUBLE");
		lov2osdMap.put(OSDE.LOV_A1_OFFSET, 		IDevice.OFFSET + "=_" + "DOUBLE");
		lov2osdMap.put(OSDE.LOV_A2_OFFSET, 		IDevice.OFFSET + "=_" + "DOUBLE");
		lov2osdMap.put(OSDE.LOV_A3_OFFSET, 		IDevice.OFFSET + "=_" + "DOUBLE");
		lov2osdMap.put(OSDE.LOV_RPM_FACTOR, 	IDevice.FACTOR + "=_" + "DOUBLE");
		lov2osdMap.put(OSDE.LOV_A1_FACTOR, 		IDevice.FACTOR + "=_" + "DOUBLE");
		lov2osdMap.put(OSDE.LOV_A2_FACTOR, 		IDevice.FACTOR + "=_" + "DOUBLE");
		lov2osdMap.put(OSDE.LOV_A3_FACTOR, 		IDevice.FACTOR + "=_" + "DOUBLE");
		//lov2osdMap.put(OSDE.LOV_CURRENT_OFFSET, value);
		//lov2osdMap.put(OSDE.LOV_CURRENT_INVERT, value);
		//lov2osdMap.put(OSDE.LOV_NUMBER_MOTOR, value); // handled UniLog internal
		lov2osdMap.put(OSDE.LOV_PROPELLER_FACTOR, IDevice.FACTOR + "=_" + "INTEGER");

		// add more device specific mappings
	}

	
	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws Exception 
	 */
	public static RecordSet read(String filePath) throws Exception {
		FileInputStream file_input = new FileInputStream(new File(filePath));
		DataInputStream data_in    = new DataInputStream(file_input);
		String channelConfig = "";
		String recordSetName = "";
		String recordSetComment = "";
		String recordSetProperties = "";
		String[] recordsProperties;
		int recordDataSize = 0;
		Channel channel = null;
		RecordSet recordSet = null;
		IDevice device = OsdReaderWriter.application.getActiveDevice();
		boolean isFirstRecordSetDisplayed = false;
		
		HashMap<String, String> header = readHeader(data_in);
		int channelNumber = new Integer(header.get(OSDE.CHANNEL_CONFIG_NUMBER)).intValue();
		String channelType = ChannelTypes.values()[device.getChannelType(channelNumber)].name();
		//String channelConfigName = channelType.equals(ChannelTypes.TYPE_OUTLET.name()) ? device.getChannelName(channelNumber) : header.get(OSDE.CHANNEL_CONFIG_NAME);
		String channelConfigName = device.getChannelName(channelNumber);
		if (log.isLoggable(Level.FINE)) log.fine("channelConfigName = " + channelConfigName + " (" + OSDE.CHANNEL_CONFIG_TYPE + channelType + "; " + OSDE.CHANNEL_CONFIG_NUMBER + channelNumber + ")");
		header.put(OSDE.CHANNEL_CONFIG_TYPE, channelType);
		header.put(OSDE.CHANNEL_CONFIG_NAME, channelConfigName);
		//header.put(OSDE.RECORD_SET_DATA_POINTER, ""+position);
		//header.containsKey(LOV_NUM_MEASUREMENTS)
		
		// record sets with it properties
		int numberRecordSets = new Integer(header.get(OSDE.RECORD_SET_SIZE).trim()).intValue();
		List<HashMap<String,String>> recordSetsInfo = new ArrayList<HashMap<String,String>>(numberRecordSets);
		for (int i=1; i<=numberRecordSets; ++i) {
			// RecordSetName : 1) Flugaufzeichnung||::||RecordSetComment : empfangen: 12.05.2006, 20:42:52||::||RecordDataSize : 22961||::||RecordSetDataBytes : 367376
			String recordSetInfo = header.get((i)+" " + OSDE.RECORD_SET_NAME);
			recordSetInfo = recordSetInfo + OSDE.DATA_DELIMITER + OSDE.CHANNEL_CONFIG_NAME + header.get(OSDE.CHANNEL_CONFIG_NAME);
			if (header.containsKey(RecordSet.TIME_STEP_MS)) {
				recordSetInfo = recordSetInfo + OSDE.DATA_DELIMITER + RecordSet.TIME_STEP_MS + "=" + header.get(RecordSet.TIME_STEP_MS);
			}
			else { // format version 1.1x dos not have this info in  file (no devices with variable time step supported)
				recordSetInfo = recordSetInfo + OSDE.DATA_DELIMITER + OSDE.RECORD_SET_PROPERTIES + RecordSet.TIME_STEP_MS + "=" + device.getTimeStep_ms();
			}
			// append record properties if any available
			recordSetInfo = recordSetInfo + OSDE.DATA_DELIMITER;
			for (int j = 0; j < device.getNumberOfMeasurements(device.getChannelName(channelNumber)); j++) {
				StringBuilder recordConfigData = new StringBuilder();
				// convert record logview config data to OSDE config keys into records section
				if (device.getName().equals("UniLog")) {
					if (j == 6) {// 6=votagePerCell LOV_CONFIG_DATA_KEYS_UNILOG_6
						HashMap<String, String> configData = StringHelper.splitString(header.get(OSDE.LOV_CONFIG_DATA), OSDE.DATA_DELIMITER, OSDE.LOV_CONFIG_DATA_KEYS_UNILOG_6);
						for (String lovKey : OSDE.LOV_CONFIG_DATA_KEYS_UNILOG_6) {
							if (configData.containsKey(lovKey)) {
								recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER);
							}
						}
					}
					else if (j == 7) { // 7=revolutionSpeed LOV_CONFIG_DATA_KEYS_UNILOG_7	
						HashMap<String, String> configData = StringHelper.splitString(header.get(OSDE.LOV_CONFIG_DATA), OSDE.DATA_DELIMITER, OSDE.LOV_CONFIG_DATA_KEYS_UNILOG_7);
						for (String lovKey : OSDE.LOV_CONFIG_DATA_KEYS_UNILOG_7) {
							if (configData.containsKey(lovKey)) {
								recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER);
							}
						}
					}
					else if (j == 8) {// 8=efficiency LOV_CONFIG_DATA_KEYS_UNILOG_8
						HashMap<String, String> configData = StringHelper.splitString(header.get(OSDE.LOV_CONFIG_DATA), OSDE.DATA_DELIMITER, OSDE.LOV_CONFIG_DATA_KEYS_UNILOG_8);
						for (String lovKey : OSDE.LOV_CONFIG_DATA_KEYS_UNILOG_8) {
							if (configData.containsKey(lovKey)) {
								recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER);
							}
						}
					}
					else					if (j == 11) {//11=a1Value LOV_CONFIG_DATA_KEYS_UNILOG_11
						HashMap<String, String> configData = StringHelper.splitString(header.get(OSDE.LOV_CONFIG_DATA), OSDE.DATA_DELIMITER, OSDE.LOV_CONFIG_DATA_KEYS_UNILOG_11);
						for (String lovKey : OSDE.LOV_CONFIG_DATA_KEYS_UNILOG_11) {
							if (configData.containsKey(lovKey)) {
								recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER);
							}
						}
					}
					else					if (j == 12) {//12=a2Value LOV_CONFIG_DATA_KEYS_UNILOG_12
						HashMap<String, String> configData = StringHelper.splitString(header.get(OSDE.LOV_CONFIG_DATA), OSDE.DATA_DELIMITER, OSDE.LOV_CONFIG_DATA_KEYS_UNILOG_12);
						for (String lovKey : OSDE.LOV_CONFIG_DATA_KEYS_UNILOG_12) {
							if (configData.containsKey(lovKey)) {
								recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER);
							}
						}
					}
					else					if (j == 13) {//13=a3Value LOV_CONFIG_DATA_KEYS_UNILOG_13
						HashMap<String, String> configData = StringHelper.splitString(header.get(OSDE.LOV_CONFIG_DATA), OSDE.DATA_DELIMITER, OSDE.LOV_CONFIG_DATA_KEYS_UNILOG_13);
						for (String lovKey : OSDE.LOV_CONFIG_DATA_KEYS_UNILOG_13) {
							if (configData.containsKey(lovKey)) {
								recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER);
							}
						}
					}
				}
				recordSetInfo = recordSetInfo + OSDE.RECORDS_PROPERTIES + recordConfigData.toString() + Record.END_MARKER;
			}
			recordSetsInfo.add(getRecordSetProperties(recordSetInfo));
		}

		try { // build the data structure 
			long position = new Long(header.get(OSDE.DATA_POINTER_POS).trim()).longValue(); 
			
			for (HashMap<String,String> recordSetInfo : recordSetsInfo) {
				channelConfig = recordSetInfo.get(OSDE.CHANNEL_CONFIG_NAME);
				recordSetName = recordSetInfo.get(OSDE.RECORD_SET_NAME);
				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
				recordSetComment = recordSetInfo.get(OSDE.RECORD_SET_COMMENT);
				recordSetProperties = recordSetInfo.get(OSDE.RECORD_SET_PROPERTIES);
				recordsProperties = StringHelper.splitString(recordSetInfo.get(OSDE.RECORDS_PROPERTIES), Record.END_MARKER, OSDE.RECORDS_PROPERTIES);
				//recordDataSize = new Long(recordSetInfo.get(OSDE.RECORD_DATA_SIZE)).longValue();
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
				//recordSet.setObjectKey(recordSetInfo.get(OSDE.OBJECT_KEY));

				//apply record sets records properties
				String [] recordKeys = recordSet.getRecordNames();
				for (int i = 0; i < recordKeys.length; ++i) {
					Record record = recordSet.get(recordKeys[i]);
					record.setSerializedProperties(recordsProperties[i]); //name, unit, symbol, active, ...
					record.setSerializedDeviceSpecificProperties(recordsProperties[i]); // factor, offset, ...
				}
				
				channel.put(recordSetName, recordSet);
			}
			OsdReaderWriter.application.getMenuToolBar().updateChannelSelector();
			OsdReaderWriter.application.getMenuToolBar().updateRecordSetSelectCombo();

			String[] firstRecordSet = new String[2];
			for (HashMap<String,String> recordSetInfo : recordSetsInfo) {
				channelConfig = recordSetInfo.get(OSDE.CHANNEL_CONFIG_NAME);
				recordSetName = recordSetInfo.get(OSDE.RECORD_SET_NAME);
				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
				if (firstRecordSet[0] == null || firstRecordSet[1] == null) {
					firstRecordSet[0] = channelConfig;
					firstRecordSet[1] = recordSetName;
				}
				recordDataSize = new Integer(recordSetInfo.get(OSDE.RECORD_DATA_SIZE).trim()).intValue();
				//recordSetDataPointer = new Long(recordSetInfo.get(RECORD_SET_DATA_POINTER)).longValue();
				channel = channels.get(channels.getChannelNumber(channelConfig));
				recordSet = channel.get(recordSetName);
				if (log.isLoggable(Level.FINER)) log.finer(String.format("data pointer position = 0x%x", position));
				
				int dataBufferSize = device.getLovDataByteSize();
				byte[] buffer = new byte[dataBufferSize * recordDataSize];
				if (log.isLoggable(Level.FINE)) log.fine("data buffer size = " + buffer.length);

				data_in.readFully(buffer);
				position += buffer.length;
				device.addAdaptedLovDataBufferAsRawDataPoints(recordSet, buffer, recordDataSize);

				// display the first record set data while reading the rest of the data
				if (!isFirstRecordSetDisplayed && firstRecordSet[0] != null && firstRecordSet[1] != null) {
					isFirstRecordSetDisplayed = true;
					channels.setFileName(filePath.substring(filePath.lastIndexOf("/")+1));
					channels.setFileDescription(header.get(OSDE.FILE_COMMENT));
					channels.setSaved(true);
					channels.switchChannel(channels.getChannelNumber(firstRecordSet[0]), firstRecordSet[1]);
				}
				//channel.applyTemplate(recordSet.getName());
				
				if (log.isLoggable(Level.FINER)) log.finer(String.format("data pointer position = 0x%x", position));
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
		return StringHelper.splitString(recordSetProperties, OSDE.DATA_DELIMITER, OSDE.LOV_FORMAT_DATA_KEYS);
	}

	/**
	 * get the basic header data like the version, header size, ... (no difference for all known format versions)
	 * @param data_in
	 * @throws IOException
	 * @throws NotSupportedFileFormat 
	 */
	private static HashMap<String, String> getBaseHeaderData(HashMap<String, String> header, DataInputStream data_in) throws IOException, NotSupportedFileFormatException {
		long position = 0;
		//read total header size
		byte[] buffer = new byte[8];
		position += data_in.read(buffer);
		long headerSize = parse2Long(buffer);
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.LOV_HEADER_SIZE + headerSize);
		header.put(OSDE.LOV_HEADER_SIZE, ""+headerSize);
		
		// read LOV stream version
		buffer = new byte[4];
		position += data_in.read(buffer);
		int streamVersion = parse2Int(buffer);
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.LOV_STREAM_VERSION + streamVersion);
		header.put(OSDE.LOV_STREAM_VERSION, ""+streamVersion);
		
		// read LOV tmp string size
		buffer = new byte[4];
		position += data_in.read(buffer);
		int tmpStringSize = parse2Int(buffer);
		buffer = new byte[tmpStringSize];
		position += data_in.read(buffer);
		String stringVersion = new String(buffer);
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.LOV_STRING_VERSION + stringVersion);
		if (streamVersion != new Integer(stringVersion.split(":V")[1])) {
			NotSupportedFileFormatException e = new NotSupportedFileFormatException("missmatch streamVersion (" + streamVersion + ") vs stringVersion (" + stringVersion + ")");
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		header.put(OSDE.LOV_STRING_VERSION, stringVersion);
	
		// read LOV saved with version
		buffer = new byte[4];
		position += data_in.read(buffer);
		tmpStringSize = parse2Int(buffer);
		buffer = new byte[tmpStringSize];
		position += data_in.read(buffer);
		String lovFormatVersion = new String(buffer);
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.LOV_FORMAT_VERSION + lovFormatVersion);
		header.put(OSDE.LOV_FORMAT_VERSION, lovFormatVersion);

		// read LOV first saved date
		buffer = new byte[4];
		position += data_in.read(buffer);
		tmpStringSize = parse2Int(buffer);
		buffer = new byte[tmpStringSize];
		position += data_in.read(buffer);
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.CREATION_TIME_STAMP + new String(buffer));
		header.put(OSDE.CREATION_TIME_STAMP, new String(buffer));
		
		// read LOV last saved date
		buffer = new byte[4];
		position += data_in.read(buffer);
		tmpStringSize = parse2Int(buffer);
		buffer = new byte[tmpStringSize];
		position += data_in.read(buffer);
		if (log.isLoggable(Level.FINER)) log.finer(OSDE.LAST_UPDATE_TIME_STAMP + new String(buffer));
		header.put(OSDE.LAST_UPDATE_TIME_STAMP, new String(buffer));
		
		header.put(OSDE.DATA_POINTER_POS, ""+position);
		if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
		
		return header;
	}
	
	/**
	 * get LogView data file header data
	 * @param filePath
	 * @return hash map containing header data as string accessible by public header keys
	 * @throws IOException 
	 * @throws NotSupportedFileFormat 
	 * @throws Exception 
	 */
	
	public static HashMap<String, String> getHeader(final String filePath) throws IOException, NotSupportedFileFormatException, Exception {
		FileInputStream file_input = new FileInputStream(new File(filePath));
		DataInputStream data_in    = new DataInputStream(file_input);
		HashMap<String, String> header = null;
		try {
			header = readHeader(data_in);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			if (e instanceof IOException) {
				throw (IOException)e;
			}
			else if (e instanceof NotSupportedFileFormatException) {
				throw (NotSupportedFileFormatException)e;
			}
			else
				throw e;
		}
		finally {
			data_in.close();
		}
		return header;
	}

	/**
	 * method to read the data using a given input stream
	 * @param filePath
	 * @param data_in
	 * @return
	 * @throws IOException
	 * @throws NotSupportedException 
	 * @throws NotSupportedFileFormat
	 */
	private static HashMap<String, String> readHeader(DataInputStream data_in) throws IOException, NotSupportedFileFormatException, NotSupportedException {
		HashMap<String, String> header = new HashMap<String, String>();
		
		getBaseHeaderData(header, data_in);
		
		String[] aVersion = header.get(OSDE.LOV_FORMAT_VERSION).split(" ");
		String useVersion = header.get(OSDE.LOV_FORMAT_VERSION).split(" ")[1];
		if (aVersion.length >= 3) useVersion = useVersion + " " + aVersion[2];
		if (log.isLoggable(Level.FINE)) log.fine("using format version " + useVersion);
		
		if (useVersion.equals("1.13")) {
			header = getHeaderInfo_1_13(data_in, header);
			header = getRecordSetInfo_1_13(data_in, header);
		}
		else if (useVersion.startsWith("1.14") || useVersion.equals("1.15")) {
			header = getHeaderInfo_1_15(data_in, header);
			header = getRecordSetInfo_1_15(data_in, header);
		}
		else if (useVersion.equals("1.50 ALPHA")) {
			header = getHeaderInfo_1_50_ALPHA(data_in, header); 
			//header = getHeaderInfo_1_15(data_in, header);
			header = getRecordSetInfo_1_50_ALPHA(data_in, header);
		}
		else if (useVersion.equals("1.50 PreBETA") || useVersion.startsWith("2.0 BETA")) {
			header = getHeaderInfo_1_50_BETA(data_in, header);
			//header = getHeaderInfo_2_0(data_in, header);
			header = getRecordSetInfo_1_50_BETA(data_in, header);
		}
		else if (useVersion.equals("2.0")) {
			header = getHeaderInfo_2_0(data_in, header);
			header = getRecordSetInfo_2_0(data_in, header);
		}
		else {
			NotSupportedFileFormatException e = new NotSupportedFileFormatException("Dateiformatversions ist nicht unterstützt, da kein Beispile dafür vorliegt - Version = " + useVersion);
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		return header;
	}
	
	/**
	 * read extended header info which is part of base header of format version 1.13
	 * @param data_in
	 * @param header
	 * @throws IOException
	 * @throws NotSupportedException 
	 */
	private static HashMap<String, String> getHeaderInfo_1_13(DataInputStream data_in, HashMap<String, String> header) throws IOException, NotSupportedException {
		long position = new Long(header.get(OSDE.DATA_POINTER_POS)).longValue();
		long headerSize = new Long(header.get(OSDE.LOV_HEADER_SIZE)).longValue();
		// read file comment
		StringBuilder fileComment = new StringBuilder();
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberCommentLines = parse2Int(buffer);
		for (int i = 0; i < numberCommentLines; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			int fileCommentSize = parse2Int(buffer);
			buffer = new byte[fileCommentSize];
			position += data_in.read(buffer);
			fileComment.append(new String(buffer)).append(" ");
		}
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.FILE_COMMENT + " = " + fileComment.toString());
		header.put(OSDE.FILE_COMMENT, fileComment.toString());
		
		// read data set channel
		buffer = new byte[4];
		position += data_in.read(buffer);
		int numberChannels = parse2Int(buffer);
		for (int i = 0; i < numberChannels; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			int fileCommentSize = parse2Int(buffer);
			buffer = new byte[fileCommentSize];
			position += data_in.read(buffer);
			String channelConfigName = new String(buffer);
			header.put(OSDE.CHANNEL_CONFIG_NAME, channelConfigName);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = new Integer(new String(buffer).split("=")[1].trim()).intValue();
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.CHANNEL_CONFIG_NUMBER + channelNumber);		
		header.put(OSDE.CHANNEL_CONFIG_NUMBER, ""+channelNumber);

		
		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.finest("CommunicationPort = " + new String(buffer));
		}
		position += data_in.skipBytes(4);

		// read device name
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceNameSize = parse2Int(buffer);
		buffer = new byte[deviceNameSize];
		position += data_in.read(buffer);
		String deviceName = new String(buffer);
		deviceName = mapLovDeviceNames(deviceName);
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.DEVICE_NAME + deviceName);
		header.put(OSDE.DEVICE_NAME, deviceName);
		
		position += data_in.skip(headerSize-position);
		header.put(OSDE.DATA_POINTER_POS, ""+position);
		if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));

		return header;
	}

	/**
	 * get the record set and dependent record parameters of format version 1.13
	 * @param device
	 * @param data_in
	 * @param header
	 * @throws IOException
	 */
	private static HashMap<String, String> getRecordSetInfo_1_13(DataInputStream data_in, HashMap<String, String> header) throws IOException {
		long position = new Long(header.get(OSDE.DATA_POINTER_POS)).longValue();
		
		position += data_in.skip(8);
		
		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(OSDE.RECORD_SET_SIZE, ""+numberRecordSets);

		position += data_in.skipBytes(8);
		
		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(OSDE.RECORD_SET_NAME).append(recordSetName).append(OSDE.DATA_DELIMITER);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_NAME + recordSetName);			
			
			position += data_in.skipBytes(4);

			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetCommentSize = parse2Int(buffer);
			buffer = new byte[recordSetCommentSize];
			position += data_in.read(buffer);
			String recordSetComment = new String(buffer);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_COMMENT + recordSetComment);
			sb.append(OSDE.RECORD_SET_COMMENT).append(recordSetComment).append(OSDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(4);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = tmpDataSize > parse2Int(buffer) ? tmpDataSize : parse2Int(buffer);

			int dataSize = tmpDataSize;
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_DATA_SIZE + dataSize);
			sb.append(OSDE.RECORD_DATA_SIZE).append(dataSize).append(OSDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(28);
			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(OSDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(OSDE.DATA_POINTER_POS, ""+position);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));

			header.put((i+1)+" " + OSDE.RECORD_SET_NAME, sb.toString());
			if (log.isLoggable(Level.FINE)) log.fine(header.get((i+1)+" " + OSDE.RECORD_SET_NAME));
		}
		return header;
	}

	/**
	 * read extended header info which is part of base header of format version 1.15
	 * @param data_in
	 * @param header
	 * @throws IOException
	 * @throws NotSupportedException 
	 */
	private static HashMap<String, String> getHeaderInfo_1_15(DataInputStream data_in, HashMap<String, String> header) throws IOException, NotSupportedException {
		long position = new Long(header.get(OSDE.DATA_POINTER_POS)).longValue();
		long headerSize = new Long(header.get(OSDE.LOV_HEADER_SIZE)).longValue();
		// read file comment
		StringBuilder fileComment = new StringBuilder();
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberCommentLines = parse2Int(buffer);
		for (int i = 0; i < numberCommentLines; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			int fileCommentSize = parse2Int(buffer);
			buffer = new byte[fileCommentSize];
			position += data_in.read(buffer);
			fileComment.append(new String(buffer)).append(" ");
		}
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.FILE_COMMENT + " = " + fileComment.toString());
		header.put(OSDE.FILE_COMMENT, fileComment.toString());
		
		// read data set channel
		buffer = new byte[4];
		position += data_in.read(buffer);
		int numberChannels = parse2Int(buffer);
		for (int i = 0; i < numberChannels; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			int fileCommentSize = parse2Int(buffer);
			buffer = new byte[fileCommentSize];
			position += data_in.read(buffer);
			String channelConfigName = new String(buffer);
			header.put(OSDE.CHANNEL_CONFIG_NAME, channelConfigName);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = new Integer(new String(buffer).split("=")[1].trim()).intValue();
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.CHANNEL_CONFIG_NUMBER + channelNumber);		
		header.put(OSDE.CHANNEL_CONFIG_NUMBER, ""+channelNumber);

		
		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.finest("CommunicationPort = " + new String(buffer));
		}
		position += data_in.skipBytes(4);

		// read device name
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceNameSize = parse2Int(buffer);
		buffer = new byte[deviceNameSize];
		position += data_in.read(buffer);
		String deviceName = new String(buffer);
		deviceName = mapLovDeviceNames(deviceName);
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.DEVICE_NAME + deviceName);
		header.put(OSDE.DEVICE_NAME, deviceName);
		
		position += data_in.skip(headerSize-position);
		if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));

		header.put(OSDE.DATA_POINTER_POS, ""+position);

		return header;
	}	

	/**
	 * get the record set and dependent record parameters of format version 1.15
	 * @param device
	 * @param data_in
	 * @param header
	 * @throws IOException
	 */
	private static HashMap<String, String> getRecordSetInfo_1_15(DataInputStream data_in, HashMap<String, String> header) throws IOException {
		long position = new Long(header.get(OSDE.DATA_POINTER_POS)).longValue();
		
		position += data_in.skip(8);
		
		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(OSDE.RECORD_SET_SIZE, ""+numberRecordSets);

		position += data_in.skipBytes(8);
		
		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(OSDE.RECORD_SET_NAME).append(recordSetName).append(OSDE.DATA_DELIMITER);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_NAME + recordSetName);
			
			position += data_in.skipBytes(4);

			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetCommentSize = parse2Int(buffer);
			buffer = new byte[recordSetCommentSize];
			position += data_in.read(buffer);
			String recordSetComment = new String(buffer);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_COMMENT + recordSetComment);
			sb.append(OSDE.RECORD_SET_COMMENT).append(recordSetComment).append(OSDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(4);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = tmpDataSize > parse2Int(buffer) ? tmpDataSize : parse2Int(buffer);

			int dataSize = tmpDataSize;
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_DATA_SIZE + dataSize);
			sb.append(OSDE.RECORD_DATA_SIZE).append(dataSize).append(OSDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(16);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
			
			// config block n100W, ...
			StringBuilder config = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberLines = parse2Int(buffer);
			if (log.isLoggable(Level.FINER)) log.finer("numberLines = " + numberLines);
			for (int j = 0; j < numberLines; j++) {
				buffer = new byte[4];
				position += data_in.read(buffer);
				int stringSize = parse2Int(buffer);
				buffer = new byte[stringSize];
				position += data_in.read(buffer);
				config.append(new String(buffer)).append(OSDE.DATA_DELIMITER);
				if (log.isLoggable(Level.FINER)) log.finer(new String(buffer));
			}
			header.put(OSDE.LOV_CONFIG_DATA, config.toString());
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
			
			position += data_in.skipBytes(8);
			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(OSDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(OSDE.DATA_POINTER_POS, ""+position);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));

			header.put((i+1)+" " + OSDE.RECORD_SET_NAME, sb.toString());
			if (log.isLoggable(Level.FINE)) log.fine(header.get((i+1)+" " + OSDE.RECORD_SET_NAME));
		}
		return header;
	}
	
	/**
	 * read extended header info which is part of base header of format version 1.50 ALPHA
	 * @param data_in
	 * @param header
	 * @throws IOException
	 * @throws NotSupportedException 
	 */
	private static HashMap<String, String> getHeaderInfo_1_50_ALPHA(DataInputStream data_in, HashMap<String, String> header) throws IOException, NotSupportedException {
		long position = new Long(header.get(OSDE.DATA_POINTER_POS)).longValue();
		long headerSize = new Long(header.get(OSDE.LOV_HEADER_SIZE)).longValue();
		// read file comment
		StringBuilder fileComment = new StringBuilder();
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberCommentLines = parse2Int(buffer);
		for (int i = 0; i < numberCommentLines; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			int fileCommentSize = parse2Int(buffer);
			buffer = new byte[fileCommentSize];
			position += data_in.read(buffer);
			fileComment.append(new String(buffer)).append(" ");
		}
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.FILE_COMMENT + " = " + fileComment.toString());
		header.put(OSDE.FILE_COMMENT, fileComment.toString());
		if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
		
		// read data set channel
		buffer = new byte[4];
		position += data_in.read(buffer);
		int numberChannels = parse2Int(buffer);
		for (int i = 0; i < numberChannels; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			int fileCommentSize = parse2Int(buffer);
			buffer = new byte[fileCommentSize];
			position += data_in.read(buffer);
			String channelConfigName = new String(buffer);
			header.put(OSDE.CHANNEL_CONFIG_NAME, channelConfigName);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = new Integer(new String(buffer).split("=")[1].trim()).intValue();
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.CHANNEL_CONFIG_NUMBER + channelNumber);		
		header.put(OSDE.CHANNEL_CONFIG_NUMBER, ""+channelNumber);

		
		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.finest("CommunicationPort = " + new String(buffer));
		}
		position += data_in.skipBytes(4);

		// read device name
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceNameSize = parse2Int(buffer);
		buffer = new byte[deviceNameSize];
		position += data_in.read(buffer);
		String deviceName = new String(buffer);
		deviceName = mapLovDeviceNames(deviceName);
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.DEVICE_NAME + deviceName);
		header.put(OSDE.DEVICE_NAME, deviceName);
		
		position += data_in.skip(headerSize-position);
		if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));

		header.put(OSDE.DATA_POINTER_POS, ""+position);

		return header;
	}	


	/**
	 * get the record set and dependent record parameters of format version 1.50 ALPHA
	 * @param device
	 * @param data_in
	 * @param header
	 * @throws IOException
	 */
	private static HashMap<String, String> getRecordSetInfo_1_50_ALPHA(DataInputStream data_in, HashMap<String, String> header) throws IOException {
		long position = new Long(header.get(OSDE.DATA_POINTER_POS)).longValue();
		
		position += data_in.skip(88);
		if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
		
		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(OSDE.RECORD_SET_SIZE, ""+numberRecordSets);

		position += data_in.skipBytes(8);
		
		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(OSDE.RECORD_SET_NAME).append(recordSetName).append(OSDE.DATA_DELIMITER);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_NAME + recordSetName);
			
			position += data_in.skipBytes(4);

			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetCommentSize = parse2Int(buffer);
			buffer = new byte[recordSetCommentSize];
			position += data_in.read(buffer);
			String recordSetComment = new String(buffer);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_COMMENT + recordSetComment);
			sb.append(OSDE.RECORD_SET_COMMENT).append(recordSetComment).append(OSDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(122);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = tmpDataSize > parse2Int(buffer) ? tmpDataSize : parse2Int(buffer);

			int dataSize = tmpDataSize;
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_DATA_SIZE + dataSize);
			sb.append(OSDE.RECORD_DATA_SIZE).append(dataSize).append(OSDE.DATA_DELIMITER);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
			
			position += data_in.skipBytes(216);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
			
			// config block n100W, ...
			StringBuilder config = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberLines = parse2Int(buffer);
			if (log.isLoggable(Level.FINER)) log.finer("numberLines = " + numberLines);
			for (int j = 0; j < numberLines; j++) {
				buffer = new byte[4];
				position += data_in.read(buffer);
				int stringSize = parse2Int(buffer);
				buffer = new byte[stringSize];
				position += data_in.read(buffer);
				config.append(new String(buffer)).append(OSDE.DATA_DELIMITER);
				if (log.isLoggable(Level.FINER)) log.finer(new String(buffer));
			}
			header.put(OSDE.LOV_CONFIG_DATA, config.toString());
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
			
			//position += data_in.skipBytes(8);
			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(OSDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(OSDE.DATA_POINTER_POS, ""+position);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));

			header.put((i+1)+" " + OSDE.RECORD_SET_NAME, sb.toString());
			if (log.isLoggable(Level.FINE)) log.fine(header.get((i+1)+" " + OSDE.RECORD_SET_NAME));
		}
		return header;
	}

	/**
	 * read extended header info which is part of base header of format version 1.50 BETA
	 * @param data_in
	 * @param header
	 * @throws IOException
	 * @throws NotSupportedException 
	 */
	private static HashMap<String, String> getHeaderInfo_1_50_BETA(DataInputStream data_in, HashMap<String, String> header) throws IOException, NotSupportedException {
		long position = new Long(header.get(OSDE.DATA_POINTER_POS)).longValue();
		long headerSize = new Long(header.get(OSDE.LOV_HEADER_SIZE)).longValue();
		// read file comment
		StringBuilder fileComment = new StringBuilder();
		byte[] buffer = new byte[8];
		position += data_in.read(buffer);
		long fileCommentSize = parse2Long(buffer); 
		buffer = new byte[(int)fileCommentSize];
		position += data_in.read(buffer);
		String tmpString = new String(buffer);
		if (log.isLoggable(Level.FINEST)) log.finest(tmpString);
		
		int index = 0;
		while ((index = tmpString.indexOf(OSDE.LOV_RTF_START_USER_TEXT, index)) != -1) {
			fileComment.append(tmpString.substring(index+OSDE.LOV_RTF_START_USER_TEXT.length(), tmpString.indexOf(OSDE.LOV_RTF_END_USER_TEXT, index))).append(" ");
			index += OSDE.LOV_RTF_START_USER_TEXT.length();
		}
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.FILE_COMMENT + " = " + fileComment.toString());
		header.put(OSDE.FILE_COMMENT, fileComment.toString());
		if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));

		// read data set channel
		buffer = new byte[4];
		position += data_in.read(buffer);
		int numberChannels = parse2Int(buffer);
		for (int i = 0; i < numberChannels; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			fileCommentSize = parse2Int(buffer);
			buffer = new byte[(int)fileCommentSize];
			position += data_in.read(buffer);
			String channelConfigName = new String(buffer);
			header.put(OSDE.CHANNEL_CONFIG_NAME, channelConfigName);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = new Integer(new String(buffer).split("=")[1].trim()).intValue();
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.CHANNEL_CONFIG_NUMBER + channelNumber);		
		header.put(OSDE.CHANNEL_CONFIG_NUMBER, ""+channelNumber);
				
		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.finest("CommunicationPort = " + new String(buffer));
		}
		position += data_in.skipBytes(4);

		// read device name
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceNameSize = parse2Int(buffer);
		buffer = new byte[deviceNameSize];
		position += data_in.read(buffer);
		String deviceName = new String(buffer);
		deviceName = mapLovDeviceNames(deviceName);
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.DEVICE_NAME + deviceName);
		header.put(OSDE.DEVICE_NAME, deviceName);
		
		position += data_in.skip(8);
		
		// read device configuration
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceConfigLineSize = parse2Int(buffer);
		if (log.isLoggable(Level.FINEST)) log.finest("DeviceConfigLineSize = " + deviceConfigLineSize);
		
		for (int i = 0; i < deviceConfigLineSize; i++) {
			// read device ini line
			buffer = new byte[4];
			position += data_in.read(buffer);
			int lineSize = parse2Int(buffer);
			buffer = new byte[lineSize];
			position += data_in.read(buffer);
			String configLine = new String(buffer);
			if (configLine.startsWith(OSDE.LOV_TIME_STEP)) 
				header.put(RecordSet.TIME_STEP_MS, configLine.split("=")[1]);
			else if (configLine.startsWith(OSDE.LOV_NUM_MEASUREMENTS))
				header.put(OSDE.LOV_NUM_MEASUREMENTS, ""+ ((new Integer(configLine.split("=")[1].trim()).intValue()) - 1)); // -1 == time
			if (log.isLoggable(Level.FINEST)) log.finest(configLine);
		}

		// end of header sometimes after headerSize
		position += data_in.skip(headerSize-position);
		//**** end main header			
		header.put(OSDE.DATA_POINTER_POS, ""+position);
		if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));

		return header;
	}	

	/**
	 * get the record set and dependent record parameters of format version 2.0
	 * @param device
	 * @param data_in
	 * @param header
	 * @throws IOException
	 */
	private static HashMap<String, String> getRecordSetInfo_1_50_BETA(DataInputStream data_in, HashMap<String, String> header) throws IOException {
		long position = new Long(header.get(OSDE.DATA_POINTER_POS)).longValue();
		
		position += data_in.skip(88);
		if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
		
		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(OSDE.RECORD_SET_SIZE, ""+numberRecordSets);
	
		position += data_in.skipBytes(8);
		
		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(OSDE.RECORD_SET_NAME).append(recordSetName).append(OSDE.DATA_DELIMITER);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_NAME + recordSetName);
						
			position += data_in.skipBytes(2);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
			
			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetConfigSize = parse2Long(buffer);
			buffer = new byte[(int)recordSetConfigSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.finest("RecordSetConfig = " + new String(buffer));
			
			position += data_in.skipBytes(112);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = tmpDataSize > parse2Int(buffer) ? tmpDataSize : parse2Int(buffer);
			
			int dataSize = tmpDataSize;
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_DATA_SIZE + dataSize);
			sb.append(OSDE.RECORD_DATA_SIZE).append(dataSize).append(OSDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(16);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
			
			// config block n100W, ...
			StringBuilder config = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberLines = parse2Int(buffer);
			if (log.isLoggable(Level.FINER)) log.finer("numberLines = " + numberLines);
			for (int j = 0; j < numberLines; j++) {
				buffer = new byte[4];
				position += data_in.read(buffer);
				int stringSize = parse2Int(buffer);
				buffer = new byte[stringSize];
				position += data_in.read(buffer);
				config.append(new String(buffer)).append(OSDE.DATA_DELIMITER);
				if (log.isLoggable(Level.FINER)) log.finer(new String(buffer));
			}
			header.put(OSDE.LOV_CONFIG_DATA, config.toString());
			
			position += data_in.skipBytes(4);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
			
			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			long rtfCommentSize = parse2Long(buffer);
			buffer = new byte[(int)rtfCommentSize];
			position += data_in.read(buffer);
			String tmpString = new String(buffer);
			if (log.isLoggable(Level.FINEST)) log.finest(tmpString);
			
			int index = 0;
			StringBuilder recordSetComment = new StringBuilder();
			while ((index = tmpString.indexOf(OSDE.LOV_RTF_START_USER_TEXT, index)) != -1) {
				recordSetComment.append(tmpString.substring(index+OSDE.LOV_RTF_START_USER_TEXT.length(), tmpString.indexOf(OSDE.LOV_RTF_END_USER_TEXT, index))).append(" ");
				index += OSDE.LOV_RTF_START_USER_TEXT.length();
			}
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_COMMENT + recordSetComment.toString());
			sb.append(OSDE.RECORD_SET_COMMENT).append(recordSetComment.toString()).append(OSDE.DATA_DELIMITER);

			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			buffer = new byte[parse2Int(buffer)];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.finest(new String(buffer));
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));			
			
			position += data_in.skip(175);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
			
			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(OSDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(OSDE.DATA_POINTER_POS, ""+position);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
			
			header.put((i+1)+" " + OSDE.RECORD_SET_NAME, sb.toString());
			if (log.isLoggable(Level.FINE)) log.fine(header.get((i+1)+" " + OSDE.RECORD_SET_NAME));
		}
		return header;
	}

	/**
	 * read extended header info which is part of base header of format version 2.0
	 * @param data_in
	 * @param header
	 * @throws IOException
	 * @throws NotSupportedException 
	 */
	private static HashMap<String, String> getHeaderInfo_2_0(DataInputStream data_in, HashMap<String, String> header) throws IOException, NotSupportedException {
		long position = new Long(header.get(OSDE.DATA_POINTER_POS)).longValue();
		long headerSize = new Long(header.get(OSDE.LOV_HEADER_SIZE)).longValue();
		// read file comment
		StringBuilder fileComment = new StringBuilder();
		byte[] buffer = new byte[8];
		position += data_in.read(buffer);
		long fileCommentSize = parse2Long(buffer); 
		buffer = new byte[(int)fileCommentSize];
		position += data_in.read(buffer);
		String tmpString = new String(buffer);
		if (log.isLoggable(Level.FINEST)) log.finest(tmpString);
		
		int index = 0;
		while ((index = tmpString.indexOf(OSDE.LOV_RTF_START_USER_TEXT, index)) != -1) {
			fileComment.append(tmpString.substring(index+OSDE.LOV_RTF_START_USER_TEXT.length(), tmpString.indexOf(OSDE.LOV_RTF_END_USER_TEXT, index))).append(" ");
			index += OSDE.LOV_RTF_START_USER_TEXT.length();
		}
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.FILE_COMMENT + " = " + fileComment.toString());
		header.put(OSDE.FILE_COMMENT, fileComment.toString());
		
		if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
		// read data set channel
		buffer = new byte[4];
		position += data_in.read(buffer);
		int numberChannels = parse2Int(buffer);
		for (int i = 0; i < numberChannels; i++) {
			buffer = new byte[4];
			position += data_in.read(buffer);
			fileCommentSize = parse2Int(buffer);
			buffer = new byte[(int)fileCommentSize];
			position += data_in.read(buffer);
			String channelConfigName = new String(buffer);
			header.put(OSDE.CHANNEL_CONFIG_NAME, channelConfigName);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = new Integer(new String(buffer).split("=")[1].trim()).intValue();
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.CHANNEL_CONFIG_NUMBER + channelNumber);		
		header.put(OSDE.CHANNEL_CONFIG_NUMBER, ""+channelNumber);
				
		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.finest("CommunicationPort = " + new String(buffer));
		}
		position += data_in.skipBytes(4);

		// read device name
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceNameSize = parse2Int(buffer);
		buffer = new byte[deviceNameSize];
		position += data_in.read(buffer);
		String deviceName = new String(buffer);
		deviceName = mapLovDeviceNames(deviceName);
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.DEVICE_NAME + deviceName);
		header.put(OSDE.DEVICE_NAME, deviceName);
		
		position += data_in.skip(8);
		
		// read device configuration
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceConfigLineSize = parse2Int(buffer);
		if (log.isLoggable(Level.FINEST)) log.finest("DeviceConfigLineSize = " + deviceConfigLineSize);
		
		for (int i = 0; i < deviceConfigLineSize; i++) {
			// read device ini line
			buffer = new byte[4];
			position += data_in.read(buffer);
			int lineSize = parse2Int(buffer);
			buffer = new byte[lineSize];
			position += data_in.read(buffer);
			String configLine = new String(buffer);
			if (configLine.startsWith(OSDE.LOV_TIME_STEP)) 
				header.put(RecordSet.TIME_STEP_MS, configLine.split("=")[1]);
			else if (configLine.startsWith(OSDE.LOV_NUM_MEASUREMENTS))
				header.put(OSDE.LOV_NUM_MEASUREMENTS, ""+ ((new Integer(configLine.split("=")[1].trim()).intValue()) - 1)); // -1 == time
			if (log.isLoggable(Level.FINEST)) log.finest(configLine);
		}

		// end of header sometimes after headerSize
		position += data_in.skip(headerSize-position);
		//**** end main header			
		header.put(OSDE.DATA_POINTER_POS, ""+position);
		if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));

		return header;
	}	

	/**
	 * get the record set and dependent record parameters of format version 2.0
	 * @param device
	 * @param data_in
	 * @param header
	 * @throws IOException
	 */
	private static HashMap<String, String> getRecordSetInfo_2_0(DataInputStream data_in, HashMap<String, String> header) throws IOException {
		long position = new Long(header.get(OSDE.DATA_POINTER_POS)).longValue();
		
		position += data_in.skip(88);
		if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
		
		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(OSDE.RECORD_SET_SIZE, ""+numberRecordSets);
	
		position += data_in.skipBytes(8);
		
		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(OSDE.RECORD_SET_NAME).append(recordSetName).append(OSDE.DATA_DELIMITER);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_NAME + recordSetName);
			
			position += data_in.skipBytes(2);

			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetConfigSize = parse2Long(buffer);
			buffer = new byte[(int)recordSetConfigSize];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.finest("RecordSetConfig = " + new String(buffer));
			
			position += data_in.skipBytes(112);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = tmpDataSize > parse2Int(buffer) ? tmpDataSize : parse2Int(buffer);
			
			int dataSize = tmpDataSize;
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_DATA_SIZE + dataSize);
			sb.append(OSDE.RECORD_DATA_SIZE).append(dataSize).append(OSDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(16);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
			
			// config block n100W, ...
			StringBuilder config = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberLines = parse2Int(buffer);
			if (log.isLoggable(Level.FINER)) log.finer("numberLines = " + numberLines);
			for (int j = 0; j < numberLines; j++) {
				buffer = new byte[4];
				position += data_in.read(buffer);
				int stringSize = parse2Int(buffer);
				buffer = new byte[stringSize];
				position += data_in.read(buffer);
				config.append(new String(buffer)).append(OSDE.DATA_DELIMITER);
				if (log.isLoggable(Level.FINER)) log.finer(new String(buffer));
			}
			header.put(OSDE.LOV_CONFIG_DATA, config.toString());
			
			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			long rtfCommentSize = parse2Long(buffer);
			buffer = new byte[(int)rtfCommentSize];
			position += data_in.read(buffer);
			String tmpString = new String(buffer);
			if (log.isLoggable(Level.FINEST)) log.finest(tmpString);
			
			int index = 0;
			StringBuilder recordSetComment = new StringBuilder();
			while ((index = tmpString.indexOf(OSDE.LOV_RTF_START_USER_TEXT, index)) != -1) {
				recordSetComment.append(tmpString.substring(index+OSDE.LOV_RTF_START_USER_TEXT.length(), tmpString.indexOf(OSDE.LOV_RTF_END_USER_TEXT, index))).append(" ");
				index += OSDE.LOV_RTF_START_USER_TEXT.length();
			}
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_COMMENT + recordSetComment.toString());
			sb.append(OSDE.RECORD_SET_COMMENT).append(recordSetComment.toString()).append(OSDE.DATA_DELIMITER);

			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			buffer = new byte[parse2Int(buffer)];
			position += data_in.read(buffer);
			if (log.isLoggable(Level.FINEST)) log.finest(new String(buffer));
			
			
			position += data_in.skip(175);

			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			if (log.isLoggable(Level.FINE)) log.fine(OSDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(OSDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(OSDE.DATA_POINTER_POS, ""+position);
			if (log.isLoggable(Level.FINER)) log.finer(String.format("position = 0x%x", position));
			
			header.put((i+1)+" " + OSDE.RECORD_SET_NAME, sb.toString());
			if (log.isLoggable(Level.FINE)) log.fine(header.get((i+1)+" " + OSDE.RECORD_SET_NAME));
		}
		return header;
	}

	/**
	 * @param buffer
	 * @return
	 */
	public static int parse2Int(byte[] buffer) {
		switch (buffer.length) {
		case 1:
			return (buffer[0] & 0xff);
		case 2:
			return (((buffer[1] & 0xff) << 8) | (buffer[0] & 0xff));
		case 3:
			return (((buffer[2] & 0xff) << 16) | ((buffer[1] & 0xff) << 8) | (buffer[0] & 0xff));
		default:
		case 4:
			return (((buffer[3] & 0xff) << 24) | ((buffer[2] & 0xff) << 16) | ((buffer[1] & 0xff) << 8) | (buffer[0] & 0xff));
		case 8:
			return (((buffer[7] & 0xff) << 56) | ((buffer[6] & 0xff) << 48) | ((buffer[5] & 0xff) << 40) | ((buffer[4] & 0xff) << 32) | ((buffer[3] & 0xff) << 24) | ((buffer[2] & 0xff) << 16) | ((buffer[1] & 0xff) << 8) | (buffer[0] & 0xff));
		}
	}

	public static long parse2Long(byte[] buffer) {
		long tmpLong1 = ((long)(buffer[3] & 0xff) << 24) + ((buffer[2] & 0xff) << 16) + ((buffer[1] & 0xff) << 8) + ((buffer[0] & 0xff) << 0);
		long tmpLong2 = (((long)buffer[7] & 255) << 56) + ((long)(buffer[6] & 255) << 48) + ((long)(buffer[5] & 255) << 40) + ((long)(buffer[4] & 255) << 32);
    return  tmpLong2 + tmpLong1;
		
	}
	
	/**
	 * map LogView device names with OSDE device names if possible
	 * @param deviceName
	 * @return
	 * @throws NotSupportedException 
	 */
	private static String mapLovDeviceNames(String deviceName) throws NotSupportedException {
		String deviceKey = deviceName.toLowerCase().trim();
		if (!deviceMap.containsKey(deviceKey)) {
			String msg = "Ein Gerät mit dem Namen = " + deviceName + " ist nicht unterstützt !";
			NotSupportedException e = new NotSupportedException(msg);
			log.log(Level.WARNING, e.getMessage(), e);
			throw e;
		}
		
		return deviceMap.get(deviceKey);
	}
}
