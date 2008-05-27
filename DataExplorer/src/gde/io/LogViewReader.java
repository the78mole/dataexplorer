package osde.io;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.OSDE;
import osde.exception.NotSupportedFileFormat;

/**
 * @author brueg
 */
public class LogViewReader {
	public static final String	DATA_POINTER_POS		= "Data_Pointer_Pos : ";
	public static final String	LOV_HEADER_SIZE			= "Header_Size : ";
	public static final String	LOV_FORMAT_VERSION	= "Format_Version : ";
	public static final String	LOV_STRING_VERSION	= "String_Version : ";
	public static final String	LOV_STREAM_VERSION	= "Stream_Version : ";
	public static final String LOV_RTF_START_USER_TEXT = "\\plain\\fs22 ";
	public static final String LOV_RTF_END_USER_TEXT = "\\par";

	
	public static final String	LOV_N_100_W = "n100W=";
	public static final String	LOV_CURRENT_OFFSET = "Stromoffset=";
	public static final String	LOV_NUMBER_MOTOR = "AnzahlMotoren=";
	public static final String	LOV_NUMBER_CELLS = "AnzahlZellen=";
	public static final String	LOV_GEAR_FACTOR = "DrehzahlFaktor=";
	public static final String	LOV_CURRENT_INVERT = "StromInvertieren=";
	public static final String	LOV_RPM_ACTIVE = "RpmChecked=";
	public static final String	LOV_A1_ACTIVE = "A1Checked=";
	public static final String	LOV_A2_ACTIVE = "A2Checked=";
	public static final String	LOV_A3_ACTIVE = "A3Checked=";
	public static final String	LOV_RPM_NAME = "RpmName=";
	public static final String	LOV_A1_NAME = "A1Name=";
	public static final String	LOV_A2_NAME = "A2Name=";
	public static final String	LOV_A3_NAME = "A3Name=";
	public static final String	LOV_RPM_OFFSET = "RpmOffset=";
	public static final String	LOV_A1_OFFSET = "A1Offset=";
	public static final String	LOV_A2_OFFSET = "A2Offset=";
	public static final String	LOV_A3_OFFSET = "A3Offset=";
	public static final String	LOV_RPM_FACTOR = "RpmFaktor=";
	public static final String	LOV_A1_FACTOR = "A1Faktor=";
	public static final String	LOV_A2_FACTOR = "A2Faktor=";
	public static final String	LOV_A3_FACTOR = "A3Faktor=";
	public static final String	LOV_RPM_UNIT = "RpmEinheit=";
	public static final String	LOV_A1_UNIT = "A1Einheit=";
	public static final String	LOV_A2_UNIT = " A2Einheit=";
	public static final String	LOV_A3_UNIT = " A3Einheit=";
	
	private static Logger					log			= Logger.getLogger(LogViewReader.class.getName());

//	private static String					lineSep	= System.getProperty("line.separator");
//	private static DecimalFormat	df3			= new DecimalFormat("0.000");
//	private static StringBuffer		sb;
//	private static String					line		= "*";
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
			
/*********

		// read data
		log.info("active device name = " + "?" + ", file device name = " + deviceName);
		log.info("active channel name = " + "?" + ", file channel name = " + channelNumber);
		if (deviceName.equals("Picolario")) {
			// read 2 active measurements
			int[] points = new int[3]; //[recordSet.size()];

			for (int k = 0; k < numberRecordSets; k++) {
				log.info("begin record set " + k);
				for (int j = 0; j < dataSize[k]; j++) {
					StringBuilder sb = new StringBuilder();
					buffer = new byte[16];
					data_in.readFully(buffer);
					// calculate height values and add
					if (((buffer[5] & 0x80) >> 7) == 0) // we have signed [feet]
						points[1] = ((buffer[4] & 0xFF) + ((buffer[5] & 0x7F) << 8)) * 1000; 				// only positive part of height data
					else
						points[1] = (((buffer[4] & 0xFF) + ((buffer[5] & 0x7F) << 8)) * -1) * 1000; // height is negative

					// add voltage U = 2.5 + (byte3 - 45) * 0.0532 - no calculation take place here
					points[0] = new Integer(buffer[6]) * 1000;

					sb.append("voltage_raw = ").append(points[0]).append("(").append(2.5 + (points[0] - 45) * 0.0532).append(")");
					sb.append("height_raw = ").append(points[1]).append("(").append(points[1] * 0.304806).append(")").append("; ");
					log.info(sb.toString());
					
					//recordSet.addPoints(points, false);
				}
				log.info("end record set " + k);
			}
		}
		else if  (deviceName.equals("UniLog")) {
			// read active measurements and calculate some others, slope calculation is extra thread
			int timeStep_ms = 0;
			Double capacity = 0.0;
			Double power = 0.0;
			StringBuilder sb = new StringBuilder();
			String lineSep = System.getProperty("line.separator");
			int tmpValue = 0;
			int[] points = new int[14]; //[recordSet.size()];

			position += data_in.skip(32 * 2);  // skip min/max lines
			for (int k = 0; k < numberRecordSets; k++) {
				timeStep_ms = 0;
				capacity = 0.0;
				power = 0.0;
				log.info("begin record set " + k);
				for (int i = 0; i < dataSize[k]; i++) {

					byte[] readBuffer = new byte[32];
					data_in.readFully(readBuffer);
					sb = new StringBuilder();

					// time milli seconds
					if (timeStep_ms == 0) { // set time step for this record set
						timeStep_ms = timeStep_ms + ((readBuffer[3] & 0xFF) << 24) + ((readBuffer[2] & 0xFF) << 16) + ((readBuffer[1] & 0xFF) << 8) + (readBuffer[0] & 0xFF);
						if (timeStep_ms != 0) {
							//recordSet.setTimeStep_ms(timeStep_ms);
							if (log.isLoggable(Level.INFO)) sb.append("timeStep_ms = " + timeStep_ms).append(lineSep);
						}
					}

					// voltageReceiver *** power/drive *** group
					tmpValue = (((readBuffer[7] & 0xFF) << 8) + (readBuffer[6] & 0xFF)) & 0x0FFF;
					points[0] = tmpValue * 10; //0=voltageReceiver						
					if (log.isLoggable(Level.INFO)) sb.append("voltageReceiver [V] = " + points[0]).append(lineSep);

					// voltage *** power/drive *** group
					tmpValue = (((readBuffer[9] & 0xFF) << 8) + (readBuffer[8] & 0xFF));
					tmpValue = tmpValue > 32768 ? tmpValue - 65536 : tmpValue;
					points[1] = tmpValue * 10; //1=voltage
					if (log.isLoggable(Level.INFO)) sb.append("voltage [V] = " + points[1]).append(lineSep);

					// current *** power/drive *** group - asymmetric for 400 A sensor 
					tmpValue = (((readBuffer[11] & 0xFF) << 8) + (readBuffer[10] & 0xFF));
					tmpValue = tmpValue <= 55536 ? tmpValue : (tmpValue - 65536);
					points[2] = tmpValue * 10; //2=current [A]
					if (log.isLoggable(Level.INFO)) sb.append("current [A] = " + points[2]).append(lineSep);
					if (points[2] > 4){
						points[2] = points[2];
					}

					capacity = i > 0 ? capacity + ((points[2] * timeStep_ms * 1.0) / 3600) : 0.0;
					points[3] = capacity.intValue(); //3=capacity [mAh]

					points[4] = new Double(1.0 * points[1] * points[2] / 1000.0).intValue(); //4=power [W]

					power = i > 0 ? power + ((points[1] / 1000.0) * (points[2] / 1000.0) * (timeStep_ms / 3600.0)) : 0.0;
					points[5] = power.intValue(); //5=energy [Wh]

					PropertyType property = null; //recordSet.get(measurements[6]).getProperty(UniLog.NUMBER_CELLS);
					int numCellValue = property != null ? new Integer(property.getValue()) : 4;
					points[6] = points[1] / numCellValue; //6=votagePerCell

					// revolution speed *** power/drive *** group
					tmpValue = (((readBuffer[13] & 0xFF) << 8) + (readBuffer[12] & 0xFF));
					tmpValue = tmpValue <= 50000 ? tmpValue : (tmpValue - 50000) * 10 + 50000;
					points[7] = tmpValue * 1000; //7=revolutionSpeed
					if (log.isLoggable(Level.INFO)) sb.append("revolution speed [1/min] = " + points[7]).append(lineSep);

					property = null; //recordSet.get(measurements[8]).getProperty(UniLog.PROP_N_100_WATT);
					int prop_n100W = property != null ? new Integer(property.getValue()) : 10000;
					double motorPower = Math.pow((points[7] / 1000.0 * 4.64) / prop_n100W, 3) * 1000.0;
					double eta = points[4] > motorPower ? (motorPower * 100.0) / points[4] : 0;
					points[8] = new Double(eta * 1000).intValue();//8=efficiency

					// height *** power/drive *** group
					tmpValue = (((readBuffer[15] & 0xFF) << 8) + (readBuffer[14] & 0xFF)) + 20000;
					tmpValue = tmpValue > 32768 ? tmpValue - 65536 : tmpValue;
					points[9] = tmpValue * 100; //9=height
					if (log.isLoggable(Level.INFO)) sb.append("height [m] = " + points[9]).append(lineSep);

					points[10] = 0; //10=slope

					// a1Modus -> 0==Temperatur, 1==Millivolt, 2=Speed 250, 3=Speed 400
					int a1Modus = (readBuffer[7] & 0xF0) >> 4; // 11110000
					tmpValue = (((readBuffer[17] & 0xFF) << 8) + (readBuffer[16] & 0xFF));
					tmpValue = tmpValue > 32768 ? tmpValue - 65536 : tmpValue;
					points[11] = new Double(tmpValue * 100.0).intValue(); //new Double(tmpValue * 100.0 * this.a1Factor + (this.a1Offset * 1000.0)).intValue(); //11=a1Value
					if (log.isLoggable(Level.INFO)) {
						sb.append("a1Modus = " + a1Modus + " (0==Temperatur, 1==Millivolt, 2=Speed 250, 3=Speed 400)").append(lineSep);
						sb.append("a1Value = " + points[11]).append(lineSep);
					}

					// A2 Modus == 0 -> external sensor; A2 Modus != 0 -> impulse time length
					int a2Modus = (readBuffer[4] & 0x30); // 00110000
					if (a2Modus == 0) {
						tmpValue = (((readBuffer[19] & 0xEF) << 8) + (readBuffer[18] & 0xFF));
						tmpValue = tmpValue > 32768 ? (tmpValue - 65536) : tmpValue;
						points[12] = new Double(tmpValue * 100.0).intValue(); //new Double(tmpValue * 100.0 * this.a2Factor + (this.a2Offset * 1000.0)).intValue(); //12=a2Value						if (log.isLoggable(Level.FINER)) 
					}
					else {
						tmpValue = (((readBuffer[19] & 0xFF) << 8) + (readBuffer[18] & 0xFF));
						points[12] = new Double(tmpValue * 1000).intValue(); //new Double(tmpValue * 1000 * this.a2Factor + (this.a2Offset * 1000)).intValue(); //12=a2Value
					}
					if (log.isLoggable(Level.INFO)) {
						sb.append("a2Modus = " + a2Modus + " (0 -> external temperature sensor; !0 -> impulse time length)").append(lineSep);
						if (a2Modus == 0)
							sb.append("a2Value = " + points[12]).append(lineSep);
						else
							sb.append("impulseTime [us]= " + points[12]).append(lineSep);
					}

					// A3 Modus == 0 -> external sensor; A3 Modus != 0 -> internal temperature
					int a3Modus = (readBuffer[4] & 0xC0); // 11000000
					tmpValue = (((readBuffer[21] & 0xEF) << 8) + (readBuffer[20] & 0xFF));
					tmpValue = tmpValue > 32768 ? tmpValue - 65536 : tmpValue;
					points[13] = new Double(tmpValue * 100.0).intValue(); //new Double(tmpValue * 100.0 * this.a3Factor + (this.a3Offset * 1000.0)).intValue(); //13=a3Value
					if (log.isLoggable(Level.INFO)) {
						sb.append("a3Modus = " + a3Modus + " (0 -> external temperature sensor; !0 -> internal temperature)").append(lineSep);
						if (a3Modus == 0)
							sb.append("a3Value = " + points[13]).append(lineSep);
						else
							sb.append("tempIntern = " + points[13]).append(lineSep);
					}

					//recordSet.addPoints(points, false);
					log.info(sb.toString());
				}
				log.info("end record set " + k);
			}
		}
		else {
			log.info("Device not supported");
		}
*********/
    
	}

	/**
	 * get the basic header data like the version, header size, ... (no difference for all known format versions)
	 * @param data_in
	 * @throws IOException
	 * @throws NotSupportedFileFormat 
	 */
	private static HashMap<String, String> getBaseHeaderData(HashMap<String, String> header, DataInputStream data_in) throws IOException, NotSupportedFileFormat {
		long position = 0;
		//read total header size
		byte[] buffer = new byte[8];
		position += data_in.read(buffer);
		long headerSize = parse2Long(buffer);
		log.info(LOV_HEADER_SIZE + headerSize);
		header.put(LOV_HEADER_SIZE, ""+headerSize);
		
		// read LOV stream version
		buffer = new byte[4];
		position += data_in.read(buffer);
		int streamVersion = parse2Int(buffer);
		log.info(LOV_STREAM_VERSION + streamVersion);
		header.put(LOV_STREAM_VERSION, ""+streamVersion);
		
		// read LOV tmp string size
		buffer = new byte[4];
		position += data_in.read(buffer);
		int tmpStringSize = parse2Int(buffer);
		buffer = new byte[tmpStringSize];
		position += data_in.read(buffer);
		String stringVersion = new String(buffer);
		log.info(LOV_STRING_VERSION + stringVersion);
		if (streamVersion != new Integer(stringVersion.split(":V")[1])) {
			NotSupportedFileFormat e = new NotSupportedFileFormat("missmatch streamVersion (" + streamVersion + ") vs stringVersion (" + stringVersion + ")");
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		header.put(LOV_STRING_VERSION, stringVersion);
	
		// read LOV saved with version
		buffer = new byte[4];
		position += data_in.read(buffer);
		tmpStringSize = parse2Int(buffer);
		buffer = new byte[tmpStringSize];
		position += data_in.read(buffer);
		String lovFormatVersion = new String(buffer);
		log.info(LOV_FORMAT_VERSION + lovFormatVersion);
		header.put(LOV_FORMAT_VERSION, lovFormatVersion);

		// read LOV first saved date
		buffer = new byte[4];
		position += data_in.read(buffer);
		tmpStringSize = parse2Int(buffer);
		buffer = new byte[tmpStringSize];
		position += data_in.read(buffer);
		log.info(OSDE.CREATION_TIME_STAMP + new String(buffer));
		header.put(OSDE.CREATION_TIME_STAMP, new String(buffer));
		
		// read LOV last saved date
		buffer = new byte[4];
		position += data_in.read(buffer);
		tmpStringSize = parse2Int(buffer);
		buffer = new byte[tmpStringSize];
		position += data_in.read(buffer);
		log.info(OSDE.LAST_UPDATE_TIME_STAMP + new String(buffer));
		header.put(OSDE.LAST_UPDATE_TIME_STAMP, new String(buffer));
		
		header.put(DATA_POINTER_POS, ""+position);
		log.info(String.format("position = 0x%x", position));
		
		return header;
	}
	
	/**
	 * get LogView data file header data
	 * @param filePath
	 * @return hash map containing header data as string accessible by public header keys
	 * @throws Exception 
	 */
	
	public static HashMap<String, String> getHeader(final String filePath) throws Exception {
		FileInputStream file_input = new FileInputStream(new File(filePath));
		DataInputStream data_in    = new DataInputStream(file_input);
		HashMap<String, String> header = null;
		try {
			header = readHeader(data_in);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
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
	 * @throws NotSupportedFileFormat
	 */
	private static HashMap<String, String> readHeader(DataInputStream data_in) throws IOException, NotSupportedFileFormat {
		HashMap<String, String> header = new HashMap<String, String>();
		
		getBaseHeaderData(header, data_in);
		
		String[] aVersion = header.get(LOV_FORMAT_VERSION).split(" ");
		String useVersion = header.get(LOV_FORMAT_VERSION).split(" ")[1];
		if (aVersion.length >= 3) useVersion = useVersion + " " + aVersion[2];
		log.info("using format version " + useVersion);
		
		if (useVersion.equals("1.13")) {
			header = getHeaderInfo_1_13(data_in, header);
			header = getRecordSetInfo_1_13(data_in, header);
		}
		else if (useVersion.equals("1.15")) {
			header = getHeaderInfo_1_15(data_in, header);
			header = getRecordSetInfo_1_15(data_in, header);
		}
		else if (useVersion.equals("1.50 BETA") || useVersion.equals("1.50 PreBETA") || useVersion.startsWith("2.0")) {
			header = getHeaderInfo_2_0(data_in, header);
			header = getRecordSetInfo_2_0(data_in, header);
		}
		else {
			NotSupportedFileFormat e = new NotSupportedFileFormat("Version = " + useVersion + " - not suported !");
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
	 */
	private static HashMap<String, String> getHeaderInfo_1_13(DataInputStream data_in, HashMap<String, String> header) throws IOException {
		long position = new Long(header.get(DATA_POINTER_POS)).longValue();
		long headerSize = new Long(header.get(LOV_HEADER_SIZE)).longValue();
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
		log.info(OSDE.FILE_COMMENT + " = " + fileComment.toString());
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
			log.info(new String(buffer));
		}
		int channelNumber = new Integer(new String(buffer).split("=")[1].trim()).intValue();
		log.info(OSDE.CHANNEL_CONFIG_NUMBER + channelNumber);		
		header.put(OSDE.CHANNEL_CONFIG_NUMBER, ""+channelNumber);

		
		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			log.info("CommunicationPort = " + new String(buffer));
		}
		position += data_in.skipBytes(4);

		// read device name
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceNameSize = parse2Int(buffer);
		buffer = new byte[deviceNameSize];
		position += data_in.read(buffer);
		String deviceName = new String(buffer);
		log.info(OSDE.DEVICE_NAME + " = " + deviceName);
		header.put(OSDE.DEVICE_NAME, deviceName);
		
		position += data_in.skip(headerSize-position);
		header.put(DATA_POINTER_POS, ""+position);
		log.info(String.format("position = 0x%x", position));

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
//		int channelNumber = new Integer(header.get(OSDE.CHANNEL_CONFIG_NUMBER)).intValue();
//		String channelType = ChannelTypes.fromValue(device.getChannelName(channelNumber)).name();
//		log.info(OSDE.CHANNEL_CONFIG_TYPE + channelType);
//		header.put(OSDE.CHANNEL_CONFIG_TYPE, channelType);
//		header.put(OSDE.RECORD_SET_DATA_POINTER, ""+position);

		
		long position = new Long(header.get(DATA_POINTER_POS)).longValue();
		
		position += data_in.skip(8);
		
		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		log.info(OSDE.RECORD_SET_SIZE + numberRecordSets);
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
			log.info(OSDE.RECORD_SET_NAME + recordSetName);
			
//			sb.append(OSDE.CHANNEL_CONFIG_NAME).append(device.getChannelName(channelNumber)).append(OSDE.DATA_DELIMITER);
			
			
			position += data_in.skipBytes(4);

			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetCommentSize = parse2Int(buffer);
			buffer = new byte[recordSetCommentSize];
			position += data_in.read(buffer);
			String recordSetComment = new String(buffer);
			log.info(OSDE.RECORD_SET_COMMENT + recordSetComment);
			sb.append(OSDE.RECORD_SET_COMMENT).append(recordSetComment).append(OSDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(4);
			log.info(String.format("position = 0x%x", position));

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = tmpDataSize > parse2Int(buffer) ? tmpDataSize : parse2Int(buffer);

			int dataSize = tmpDataSize;
			log.info(OSDE.RECORD_DATA_SIZE + dataSize);
			sb.append(OSDE.RECORD_DATA_SIZE).append(dataSize).append(OSDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(28);
			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			log.info(OSDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(OSDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(DATA_POINTER_POS, ""+position);
			log.info(String.format("position = 0x%x", position));

			header.put((i+1)+" " + OSDE.RECORD_SET_NAME, sb.toString());
			log.info(header.get((i+1)+" " + OSDE.RECORD_SET_NAME));
		}
		return header;
	}

	/**
	 * read extended header info which is part of base header of format version 1.15
	 * @param data_in
	 * @param header
	 * @throws IOException
	 */
	private static HashMap<String, String> getHeaderInfo_1_15(DataInputStream data_in, HashMap<String, String> header) throws IOException {
		long position = new Long(header.get(DATA_POINTER_POS)).longValue();
		long headerSize = new Long(header.get(LOV_HEADER_SIZE)).longValue();
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
		log.info(OSDE.FILE_COMMENT + " = " + fileComment.toString());
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
			log.info(new String(buffer));
		}
		int channelNumber = new Integer(new String(buffer).split("=")[1].trim()).intValue();
		log.info(OSDE.CHANNEL_CONFIG_NUMBER + channelNumber);		
		header.put(OSDE.CHANNEL_CONFIG_NUMBER, ""+channelNumber);

		
		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			log.info("CommunicationPort = " + new String(buffer));
		}
		position += data_in.skipBytes(4);

		// read device name
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceNameSize = parse2Int(buffer);
		buffer = new byte[deviceNameSize];
		position += data_in.read(buffer);
		String deviceName = new String(buffer);
		log.info(OSDE.DEVICE_NAME + " = " + deviceName);
		header.put(OSDE.DEVICE_NAME, deviceName);
		
		position += data_in.skip(headerSize-position);
		log.info(String.format("position = 0x%x", position));

		header.put(DATA_POINTER_POS, ""+position);

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
		long position = new Long(header.get(DATA_POINTER_POS)).longValue();
		
		position += data_in.skip(8);
		
		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		log.info(OSDE.RECORD_SET_SIZE + numberRecordSets);
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
			log.info(OSDE.RECORD_SET_NAME + recordSetName);
			
			position += data_in.skipBytes(4);

			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetCommentSize = parse2Int(buffer);
			buffer = new byte[recordSetCommentSize];
			position += data_in.read(buffer);
			String recordSetComment = new String(buffer);
			log.info(OSDE.RECORD_SET_COMMENT + recordSetComment);
			sb.append(OSDE.RECORD_SET_COMMENT).append(recordSetComment).append(OSDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(4);
			log.info(String.format("position = 0x%x", position));

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = tmpDataSize > parse2Int(buffer) ? tmpDataSize : parse2Int(buffer);

			int dataSize = tmpDataSize;
			log.info(OSDE.RECORD_DATA_SIZE + dataSize);
			sb.append(OSDE.RECORD_DATA_SIZE).append(dataSize).append(OSDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(16);
			log.info(String.format("position = 0x%x", position));
			
			// config block n100W, ...
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberLines = parse2Int(buffer);
			log.info("numberLines = " + numberLines);
			for (int j = 0; j < numberLines; j++) {
				buffer = new byte[4];
				position += data_in.read(buffer);
				int stringSize = parse2Int(buffer);
				buffer = new byte[stringSize];
				position += data_in.read(buffer);
				log.info(new String(buffer));
			}
			log.info(String.format("position = 0x%x", position));
			
			position += data_in.skipBytes(8);
			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			log.info(OSDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(OSDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(DATA_POINTER_POS, ""+position);
			log.info(String.format("position = 0x%x", position));

			header.put((i+1)+" " + OSDE.RECORD_SET_NAME, sb.toString());
			log.info(header.get((i+1)+" " + OSDE.RECORD_SET_NAME));
		}
		return header;
	}

	/**
	 * read extended header info which is part of base header of format version 1.50
	 * @param data_in
	 * @param header
	 * @throws IOException
	 */
	private static HashMap<String, String> getHeaderInfo_1_50(DataInputStream data_in, HashMap<String, String> header) throws IOException {
		long position = new Long(header.get(DATA_POINTER_POS)).longValue();
		long headerSize = new Long(header.get(LOV_HEADER_SIZE)).longValue();
		log.info(String.format("position = 0x%x", position));
		// read file comment
		StringBuilder fileComment = new StringBuilder();
		byte[] buffer = new byte[8];
		position += data_in.read(buffer);
		long fileCommentSize = parse2Long(buffer); 
		buffer = new byte[(int)fileCommentSize];
		position += data_in.read(buffer);
		String tmpString = new String(buffer);
		//log.info(tmpString);
		
		int index = 0;
		while ((index = tmpString.indexOf(LOV_RTF_START_USER_TEXT, index)) != -1) {
			fileComment.append(tmpString.substring(index+LOV_RTF_START_USER_TEXT.length(), tmpString.indexOf(LOV_RTF_END_USER_TEXT, index))).append(" ");
			index += LOV_RTF_START_USER_TEXT.length();
		}
		log.info(OSDE.FILE_COMMENT + " = " + fileComment.toString());
		header.put(OSDE.FILE_COMMENT, fileComment.toString());
		
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
			log.info(new String(buffer));
		}
		int channelNumber = new Integer(new String(buffer).split("=")[1].trim()).intValue();
		log.info(OSDE.CHANNEL_CONFIG_NUMBER + channelNumber);		
		header.put(OSDE.CHANNEL_CONFIG_NUMBER, ""+channelNumber);
				
		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			log.info("CommunicationPort = " + new String(buffer));
		}
		position += data_in.skipBytes(4);

		// read device name
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceNameSize = parse2Int(buffer);
		buffer = new byte[deviceNameSize];
		position += data_in.read(buffer);
		String deviceName = new String(buffer);
		log.info(OSDE.DEVICE_NAME + deviceName);
		header.put(OSDE.DEVICE_NAME, deviceName);
		
		position += data_in.skip(8);
		
		// read device configuration
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceConfigLineSize = parse2Int(buffer);
		//log.info("DeviceConfigLineSize = " + deviceConfigLineSize);
		
		for (int i = 0; i < deviceConfigLineSize; i++) {
			// read device ini line
			buffer = new byte[4];
			position += data_in.read(buffer);
			int lineSize = parse2Int(buffer);
			buffer = new byte[lineSize];
			position += data_in.read(buffer);
			log.info(new String(buffer));
		}

		// end of header sometimes after headerSize
		//position += data_in.skip(headerSize-position);
		//log.info(String.format("position = 0x%x", position));
		//**** end main header			
		header.put(DATA_POINTER_POS, ""+position);
		log.info(String.format("position = 0x%x", position));

		return header;
	}	

	/**
	 * read extended header info which is part of base header of format version 2.0
	 * @param data_in
	 * @param header
	 * @throws IOException
	 */
	private static HashMap<String, String> getHeaderInfo_2_0(DataInputStream data_in, HashMap<String, String> header) throws IOException {
		long position = new Long(header.get(DATA_POINTER_POS)).longValue();
		//long headerSize = new Long(header.get(LOV_HEADER_SIZE)).longValue();
		// read file comment
		StringBuilder fileComment = new StringBuilder();
		byte[] buffer = new byte[8];
		position += data_in.read(buffer);
		long fileCommentSize = parse2Long(buffer); 
		buffer = new byte[(int)fileCommentSize];
		position += data_in.read(buffer);
		String tmpString = new String(buffer);
		//log.info(tmpString);
		
		int index = 0;
		while ((index = tmpString.indexOf(LOV_RTF_START_USER_TEXT, index)) != -1) {
			fileComment.append(tmpString.substring(index+LOV_RTF_START_USER_TEXT.length(), tmpString.indexOf(LOV_RTF_END_USER_TEXT, index))).append(" ");
			index += LOV_RTF_START_USER_TEXT.length();
		}
		log.info(OSDE.FILE_COMMENT + " = " + fileComment.toString());
		header.put(OSDE.FILE_COMMENT, fileComment.toString());
		
		log.info(String.format("position = 0x%x", position));
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
			log.info(new String(buffer));
		}
		int channelNumber = new Integer(new String(buffer).split("=")[1].trim()).intValue();
		log.info(OSDE.CHANNEL_CONFIG_NUMBER + channelNumber);		
		header.put(OSDE.CHANNEL_CONFIG_NUMBER, ""+channelNumber);
				
		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			log.info("CommunicationPort = " + new String(buffer));
		}
		position += data_in.skipBytes(4);

		// read device name
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceNameSize = parse2Int(buffer);
		buffer = new byte[deviceNameSize];
		position += data_in.read(buffer);
		String deviceName = new String(buffer);
		log.info(OSDE.DEVICE_NAME + deviceName);
		header.put(OSDE.DEVICE_NAME, deviceName);
		
		position += data_in.skip(8);
		
		// read device configuration
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceConfigLineSize = parse2Int(buffer);
		//log.info("DeviceConfigLineSize = " + deviceConfigLineSize);
		
		for (int i = 0; i < deviceConfigLineSize; i++) {
			// read device ini line
			buffer = new byte[4];
			position += data_in.read(buffer);
			int lineSize = parse2Int(buffer);
			buffer = new byte[lineSize];
			position += data_in.read(buffer);
			log.info(new String(buffer));
		}

		// end of header sometimes after headerSize
		//position += data_in.skip(headerSize-position);
		//log.info(String.format("position = 0x%x", position));
		//**** end main header			
		header.put(DATA_POINTER_POS, ""+position);
		log.info(String.format("position = 0x%x", position));

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
		long position = new Long(header.get(DATA_POINTER_POS)).longValue();
		
		position += data_in.skip(80);
		log.info(String.format("position = 0x%x", position));
		
		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		log.info(OSDE.RECORD_SET_SIZE + numberRecordSets);
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
			log.info(OSDE.RECORD_SET_NAME + recordSetName);
			
			position += data_in.skipBytes(2);

			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetConfigSize = parse2Long(buffer);
			buffer = new byte[(int)recordSetConfigSize];
			position += data_in.read(buffer);
			//log.info("RecordSetConfig = " + new String(buffer));
			
			position += data_in.skipBytes(112);
			log.info(String.format("position = 0x%x", position));

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = tmpDataSize > parse2Int(buffer) ? tmpDataSize : parse2Int(buffer);
			
			int dataSize = tmpDataSize;
			log.info(OSDE.RECORD_DATA_SIZE + dataSize);
			sb.append(OSDE.RECORD_DATA_SIZE).append(dataSize).append(OSDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(16);
			log.info(String.format("position = 0x%x", position));
			
			// config block n100W, ...
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberLines = parse2Int(buffer);
			log.info("numberLines = " + numberLines);
			for (int j = 0; j < numberLines; j++) {
				buffer = new byte[4];
				position += data_in.read(buffer);
				int stringSize = parse2Int(buffer);
				buffer = new byte[stringSize];
				position += data_in.read(buffer);
				log.info(new String(buffer));
			}
			
			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			long rtfCommentSize = parse2Long(buffer);
			buffer = new byte[(int)rtfCommentSize];
			position += data_in.read(buffer);
			String tmpString = new String(buffer);
			//log.info(tmpString);
			
			int index = 0;
			StringBuilder recordSetComment = new StringBuilder();
			while ((index = tmpString.indexOf(LOV_RTF_START_USER_TEXT, index)) != -1) {
				recordSetComment.append(tmpString.substring(index+LOV_RTF_START_USER_TEXT.length(), tmpString.indexOf(LOV_RTF_END_USER_TEXT, index))).append(" ");
				index += LOV_RTF_START_USER_TEXT.length();
			}
			log.info(OSDE.RECORD_SET_COMMENT + recordSetComment.toString());
			sb.append(OSDE.RECORD_SET_COMMENT).append(recordSetComment.toString()).append(OSDE.DATA_DELIMITER);

			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			buffer = new byte[parse2Int(buffer)];
			position += data_in.read(buffer);
			//log.info(new String(buffer));
			
			
			position += data_in.skip(175);

			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			log.info(OSDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(OSDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(DATA_POINTER_POS, ""+position);
			log.info(String.format("position = 0x%x", position));
			
			header.put((i+1)+" " + OSDE.RECORD_SET_NAME, sb.toString());
			log.info(header.get((i+1)+" " + OSDE.RECORD_SET_NAME));
		}
		return header;
	}

	/**
	 * @param buffer
	 * @return
	 */
	private static int parse2Int(byte[] buffer) {
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

	private static long parse2Long(byte[] buffer) {
		long tmpLong1 = ((long)(buffer[3] & 0xff) << 24) + ((buffer[2] & 0xff) << 16) + ((buffer[1] & 0xff) << 8) + ((buffer[0] & 0xff) << 0);
		long tmpLong2 = (((long)buffer[7] & 255) << 56) + ((long)(buffer[6] & 255) << 48) + ((long)(buffer[5] & 255) << 40) + ((long)(buffer[4] & 255) << 32);
    return  tmpLong2 + tmpLong1;
		
	}
}
