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

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import gde.log.Level;
import java.util.logging.Logger;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.ChannelTypes;
import gde.device.IDevice;
import gde.exception.DataInconsitsentException;
import gde.exception.NotSupportedException;
import gde.exception.NotSupportedFileFormatException;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;


/**
 * @author Winfried Brügmann
 * This class reads and writes LogView file format
 */
public class LogViewReader {	
	final static Logger										log					= Logger.getLogger(LogViewReader.class.getName());

	final static DataExplorer		application	= DataExplorer.getInstance();
	final static Channels 								channels 		= Channels.getInstance();
	final static HashMap<String, String> 	deviceMap		=	new HashMap<String, String>();
	final static HashMap<String, String> 	lov2osdMap	=	new HashMap<String, String>();
	
	// fill device Map with 
	static {
		deviceMap.put("htronic akkumaster c4", "AkkuMasterC4"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("picolario", "Picolario"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("unilog", "UniLog"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("e-station 902", "eStation902"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("e-station bc6", "eStationBC6"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("e-station bc610", "eStationBC610"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("e-station bc8", "eStationBC8"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("pichler p6", "PichlerP6"); //$NON-NLS-1$ //$NON-NLS-2$		
		deviceMap.put("pichler p60", "PichlerP60"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("lipowatch", "LiPoWatch"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("wstech datavario", "DataVario"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("wstech datavario duo", "DataVarioDuo"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("wstech linkvario", "LinkaVario"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("wstech linkvario duo", "LinkVario"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("qc copter", "QC-Copter"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("sm gps logger", "GPS-Logger"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("nmea 0183", "NMEA-Adapter"); //$NON-NLS-1$ //$NON-NLS-2$
		deviceMap.put("graupner ultra duo plus 60", "UltraDuoPlus60"); //$NON-NLS-1$ //$NON-NLS-2$
		// add more supported devices here, key in lower case
	}

	
	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws Exception 
	 */
	public static RecordSet read(String filePath) throws Exception {
		FileInputStream file_input = new FileInputStream(new File(filePath));
		DataInputStream data_in    = new DataInputStream(file_input);
		String channelConfig = GDE.STRING_EMPTY;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetComment = GDE.STRING_EMPTY;
		String recordSetProperties = GDE.STRING_EMPTY;
		String[] recordsProperties;
		int recordDataSize = 0;
		int recordSetDataBytes = 0, lastRecordSetDataBytes = 0;
		long recordSetDataPointer;
		Channel channel = null;
		RecordSet recordSet = null;
		IDevice device = LogViewReader.application.getActiveDevice();
		boolean isFirstRecordSetDisplayed = false;
		
		device.getLovKeyMappings(lov2osdMap);
		
		HashMap<String, String> header = readHeader(data_in);
		int channelNumber = new Integer(header.get(GDE.CHANNEL_CONFIG_NUMBER)).intValue();
		ChannelTypes channelType = device.getChannelTypes(channelNumber);
		//String channelConfigName = channelType.equals(ChannelTypes.TYPE_OUTLET.name()) ? device.getChannelName(channelNumber) : header.get(GDE.CHANNEL_CONFIG_NAME);
		String channelConfigName = device.getChannelName(channelNumber);
		log.log(Level.FINE, "channelConfigName = " + channelConfigName + " (" + GDE.CHANNEL_CONFIG_TYPE + channelType + "; " + GDE.CHANNEL_CONFIG_NUMBER + channelNumber + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		header.put(GDE.CHANNEL_CONFIG_TYPE, channelType.name());
		header.put(GDE.CHANNEL_CONFIG_NAME, channelConfigName);
		//header.put(GDE.RECORD_SET_DATA_POINTER, GDE.STRING_EMPTY+position);
		//header.containsKey(LOV_NUM_MEASUREMENTS)
		
		// record sets with it properties
		int numberRecordSets = new Integer(header.get(GDE.RECORD_SET_SIZE).trim()).intValue();
		List<HashMap<String,String>> recordSetsInfo = new ArrayList<HashMap<String,String>>(numberRecordSets);
		for (int i=1; i<=numberRecordSets; ++i) {
			// RecordSetName : 1) Flugaufzeichnung||::||RecordSetComment : empfangen: 12.05.2006, 20:42:52||::||RecordDataSize : 22961||::||RecordSetDataBytes : 367376
			String recordSetInfo = header.get((i)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME);
			recordSetInfo = recordSetInfo + GDE.DATA_DELIMITER + GDE.CHANNEL_CONFIG_NAME + header.get(GDE.CHANNEL_CONFIG_NAME);
			if (header.containsKey(RecordSet.TIME_STEP_MS)) {
				// do not use LogView time
				//recordSetInfo = recordSetInfo + GDE.DATA_DELIMITER + RecordSet.TIME_STEP_MS + GDE.STRING_EQUAL + header.get(RecordSet.TIME_STEP_MS);
				recordSetInfo = recordSetInfo + GDE.DATA_DELIMITER + RecordSet.TIME_STEP_MS + GDE.STRING_EQUAL +  device.getTimeStep_ms();
			}
			else { // format version 1.1x dos not have this info in  file (no devices with variable time step supported)
				recordSetInfo = recordSetInfo + GDE.DATA_DELIMITER + RecordSet.TIME_STEP_MS + GDE.STRING_EQUAL +  device.getTimeStep_ms();
			}
			// append record properties if any available
			recordSetInfo = recordSetInfo + GDE.DATA_DELIMITER;
			recordSetInfo = recordSetInfo + device.getConvertedRecordConfigurations(header, lov2osdMap, channelNumber);
			recordSetsInfo.add(getRecordSetProperties(recordSetInfo));
		}

		try { // build the data structure 
			long position = new Long(header.get(GDE.DATA_POINTER_POS).trim()).longValue(); 
			
			for (HashMap<String,String> recordSetInfo : recordSetsInfo) {
				channelConfig = recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME);
				recordSetName = recordSetInfo.get(GDE.RECORD_SET_NAME);
				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
				recordSetComment = recordSetInfo.get(GDE.RECORD_SET_COMMENT);
				recordSetProperties = recordSetInfo.get(GDE.RECORD_SET_PROPERTIES);
				recordsProperties = StringHelper.splitString(recordSetInfo.get(GDE.RECORDS_PROPERTIES), Record.END_MARKER, GDE.RECORDS_PROPERTIES);
				//recordDataSize = new Long(recordSetInfo.get(GDE.RECORD_DATA_SIZE)).longValue();
				//recordSetDataPointer = new Long(recordSetInfo.get(RECORD_SET_DATA_POINTER)).longValue();
				channel = channels.get(channels.getChannelNumber(channelConfig));
				if (channel == null) { // channelConfiguration not found
					String msg = Messages.getString(MessageIds.GDE_MSGI0018, new Object[] { recordSetName }) + " " + Messages.getString(MessageIds.GDE_MSGI0019) + "\n" + Messages.getString(MessageIds.GDE_MSGI0020);
					DataExplorer.getInstance().openMessageDialogAsync(msg);
					channel = new Channel(channelConfig, channelType);
					// do not allocate records to record set - newChannel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, activeConfig));
					channels.put(channel.getNumber(), channel);
					Vector<String> newChannelNames = new Vector<String>();
					for(String channelConfigKey : channels.getChannelNames()) {
						newChannelNames.add(channelConfigKey);
					}
					newChannelNames.add(channel.getNumber() + GDE.STRING_BLANK_COLON_BLANK + channelConfig);
					channels.setChannelNames(newChannelNames.toArray(new String[1]));
				}
				recordSet = RecordSet.createRecordSet(recordSetName, device, channel.getNumber(), true, true);
				//apply record sets properties
				recordSet.setRecordSetDescription(recordSetComment);
				recordSet.setDeserializedProperties(recordSetProperties);
				recordSet.setSaved(true);
//				try {
//					recordSet.setTimeStep_ms(new Double(recordSetInfo.get(RecordSet.TIME_STEP_MS).trim()).doubleValue());
//				}
//				catch (NumberFormatException e) { 
//					//ignore and use GDE value }
//				}
				//recordSet.setObjectKey(recordSetInfo.get(GDE.OBJECT_KEY));

				//apply record sets records properties
				String [] recordKeys = recordSet.getRecordNames();
				for (int i = 0; i < recordsProperties.length; ++i) {
					Record record = recordSet.get(recordKeys[i]);
					record.setSerializedProperties(recordsProperties[i]); //name, unit, symbol, active, ...
					record.setSerializedDeviceSpecificProperties(recordsProperties[i]); // factor, offset, ...
				}
				
				channel.put(recordSetName, recordSet);
			}
			MenuToolBar menuToolBar = LogViewReader.application.getMenuToolBar();
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
				recordDataSize = new Integer(recordSetInfo.get(GDE.RECORD_DATA_SIZE).trim()).intValue();
				log.log(Level.FINE, "recordDataSize = " + recordDataSize);
				recordSetDataBytes = new Long(recordSetInfo.get(GDE.RECORD_SET_DATA_BYTES).trim()).intValue();
				if (lastRecordSetDataBytes == 0) { // file contains more then one record set
					lastRecordSetDataBytes = recordSetDataBytes;
				}
				else {
					recordSetDataBytes = recordSetDataBytes - lastRecordSetDataBytes;
					lastRecordSetDataBytes = new Long(recordSetInfo.get(GDE.RECORD_SET_DATA_BYTES).trim()).intValue();
				}
				log.log(Level.FINE, "recordSetDataSize = " + recordSetDataBytes);
				recordSetDataPointer = position;
				log.log(Level.FINE, String.format("recordSetDataPointer = %d (0x%X)", recordSetDataPointer, recordSetDataPointer));
				channel = channels.get(channels.getChannelNumber(channelConfig));
				recordSet = channel.get(recordSetName);
				recordSet.setFileDataPointerAndSize(recordSetDataPointer, recordDataSize, recordSetDataBytes);
				//channel.setActiveRecordSet(recordSet);
				
				byte[] buffer = new byte[recordSetDataBytes];
				
				if (recordSetName.equals(firstRecordSet[1])) {
					long startTime = new Date().getTime();
					log.log(Level.FINE, "data buffer size = " + buffer.length); //$NON-NLS-1$
					data_in.readFully(buffer);
					log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
					device.addConvertedLovDataBufferAsRawDataPoints(recordSet, buffer, recordDataSize, application.getStatusBar() != null);
					log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
					device.updateVisibilityStatus(recordSet, true);
					if (application.getMenuToolBar() != null) {
						channel.applyTemplate(recordSet.getName(), true);
					}
				}
				

				// display the first record set data while reading the rest of the data
				if (!isFirstRecordSetDisplayed && firstRecordSet[0] != null && firstRecordSet[1] != null && application.getMenuToolBar() != null) {
					isFirstRecordSetDisplayed = true;
					channel.setFileName(filePath);
					channel.setFileDescription(header.get(GDE.FILE_COMMENT));
					channel.setSaved(true);
					channels.switchChannel(channels.getChannelNumber(firstRecordSet[0]), firstRecordSet[1]);
				}
				
				position += buffer.length;
				log.log(Level.FINER, String.format("data pointer position = 0x%X", position)); //$NON-NLS-1$
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
	 * read record set data with given file seek pointer and record size
	 * @param recordSet
	 * @param filePath
	 * @throws DataInconsitsentException 
	 */
	public static void readRecordSetsData(RecordSet recordSet, String filePath, boolean doUpdateProgressBar) throws FileNotFoundException, IOException, DataInconsitsentException {
		RandomAccessFile random_in = null;

		try {
			long recordSetFileDataPointer = recordSet.getFileDataPointer();
			int recordFileDataSize = recordSet.getFileDataSize();
			IDevice device = recordSet.getDevice();
			long startTime = new Date().getTime();
			byte[] buffer = new byte[recordSet.getFileDataBytesSize()];
			log.log(Level.FINE, "recordSetDataSize = " + buffer.length);
			log.log(Level.FINE, String.format("recordSetDataPointer = %d (0x%X)", recordSetFileDataPointer, recordSetFileDataPointer));
			
			random_in = new RandomAccessFile(new File(filePath), "r"); //$NON-NLS-1$
			random_in.seek(recordSetFileDataPointer);
			recordSetFileDataPointer = random_in.getFilePointer();
			log.log(Level.FINE, String.format("recordSetDataPointer = %d (0x%X)", recordSetFileDataPointer, recordSetFileDataPointer));
			random_in.readFully(buffer);
			random_in.close();
			
			device.addConvertedLovDataBufferAsRawDataPoints(recordSet, buffer, recordFileDataSize, doUpdateProgressBar);
			log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
			
			device.updateVisibilityStatus(recordSet, true);
			if (application.getMenuToolBar() != null) {
				channels.getActiveChannel().applyTemplate(recordSet.getName(), true);
			}
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
			if (random_in != null) random_in.close();
		}
	}

	/**
	 * get parsed record set properties containing all data found by OSD_FORMAT_DATA_KEYS 
	 * @param recordSetProperties
	 * @return hash map with string type data
	 */
	public static HashMap<String, String> getRecordSetProperties(String recordSetProperties) {
		return StringHelper.splitString(recordSetProperties, GDE.DATA_DELIMITER, GDE.LOV_FORMAT_DATA_KEYS);
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
		log.log(Level.FINE, GDE.LOV_HEADER_SIZE + headerSize);
		header.put(GDE.LOV_HEADER_SIZE, GDE.STRING_EMPTY+headerSize);
		
		// read LOV stream version
		buffer = new byte[4];
		position += data_in.read(buffer);
		int streamVersion = parse2Int(buffer);
		log.log(Level.FINE, GDE.LOV_STREAM_VERSION + streamVersion);
		header.put(GDE.LOV_STREAM_VERSION, GDE.STRING_EMPTY+streamVersion);
		
		// read LOV tmp string size
		buffer = new byte[4];
		position += data_in.read(buffer);
		int tmpStringSize = parse2Int(buffer);
		buffer = new byte[tmpStringSize];
		position += data_in.read(buffer);
		String stringVersion = new String(buffer);
		log.log(Level.FINE, GDE.LOV_SSTREAM_VERSION + stringVersion);
		if (streamVersion != new Integer(stringVersion.split(":V")[1])) { //$NON-NLS-1$
			NotSupportedFileFormatException e = new NotSupportedFileFormatException(Messages.getString(MessageIds.GDE_MSGE0008, new Object[] { streamVersion, stringVersion })); 
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		header.put(GDE.LOV_SSTREAM_VERSION, stringVersion);
	
		// read LOV saved with version
		buffer = new byte[4];
		position += data_in.read(buffer);
		tmpStringSize = parse2Int(buffer);
		buffer = new byte[tmpStringSize];
		position += data_in.read(buffer);
		String lovFormatVersion = new String(buffer);
		log.log(Level.FINE, GDE.LOV_FORMAT_VERSION + lovFormatVersion);
		header.put(GDE.LOV_FORMAT_VERSION, lovFormatVersion);

		// read LOV first saved date
		buffer = new byte[4];
		position += data_in.read(buffer);
		tmpStringSize = parse2Int(buffer);
		buffer = new byte[tmpStringSize];
		position += data_in.read(buffer);
		log.log(Level.FINE, GDE.CREATION_TIME_STAMP + new String(buffer));
		header.put(GDE.CREATION_TIME_STAMP, new String(buffer));
		
		// read LOV last saved date
		buffer = new byte[4];
		position += data_in.read(buffer);
		tmpStringSize = parse2Int(buffer);
		buffer = new byte[tmpStringSize];
		position += data_in.read(buffer);
		log.log(Level.FINER, GDE.LAST_UPDATE_TIME_STAMP + new String(buffer));
		header.put(GDE.LAST_UPDATE_TIME_STAMP, new String(buffer));
		
		header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
		log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
		
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
	public static HashMap<String, String> readHeader(DataInputStream data_in) throws IOException, NotSupportedFileFormatException, NotSupportedException {
		HashMap<String, String> header = new HashMap<String, String>();
		
		getBaseHeaderData(header, data_in);
		String streamVersion = header.get(GDE.LOV_STREAM_VERSION);
		String[] aVersion = header.get(GDE.LOV_FORMAT_VERSION).split(GDE.STRING_BLANK);
		String useVersion = aVersion.length > 1 ? aVersion[1] : "";
		if (aVersion.length >= 3) useVersion = useVersion + GDE.STRING_BLANK + aVersion[2];
		log.log(Level.FINE, "using format version " + useVersion); //$NON-NLS-1$
		
		if (useVersion.equals("1.13")) { //$NON-NLS-1$
			header = getHeaderInfo_1_13(data_in, header);
			header = getRecordSetInfo_1_13(data_in, header);
		}
		else if (useVersion.startsWith("1.14") || useVersion.equals("1.15")) { //$NON-NLS-1$ //$NON-NLS-2$
			header = getHeaderInfo_1_15(data_in, header);
			header = getRecordSetInfo_1_15(data_in, header);
		}
		else if (useVersion.equals("1.50 ALPHA")) { //$NON-NLS-1$
			header = getHeaderInfo_1_50_ALPHA(data_in, header); 
			//header = getHeaderInfo_1_15(data_in, header);
			header = getRecordSetInfo_1_50_ALPHA(data_in, header);
		}
		else if (useVersion.equals("1.50 PreBETA") || useVersion.startsWith("2.0 BETA")) { //$NON-NLS-1$ //$NON-NLS-2$
			header = getHeaderInfo_1_50_BETA(data_in, header);
			//header = getHeaderInfo_2_0(data_in, header);
			header = getRecordSetInfo_1_50_BETA(data_in, header);
		}
		else if (streamVersion.equals("4")) { //$NON-NLS-1$
			header = getHeaderInfo_4(data_in, header);
			header = getRecordSetInfo_4(data_in, header);
		}
		else if (streamVersion.equals("5")) { //$NON-NLS-1$
			header = getHeaderInfo_5(data_in, header);
			header = getRecordSetInfo_5(data_in, header);
		}
		else {
			NotSupportedFileFormatException e = new NotSupportedFileFormatException(Messages.getString(MessageIds.GDE_MSGI0021, new Object[] { useVersion } ));
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
		long position = new Long(header.get(GDE.DATA_POINTER_POS)).longValue();
		long headerSize = new Long(header.get(GDE.LOV_HEADER_SIZE)).longValue();
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
			fileComment.append(new String(buffer)).append(GDE.STRING_BLANK);
		}
		log.log(Level.FINE, GDE.FILE_COMMENT + " = " + fileComment.toString()); //$NON-NLS-1$
		header.put(GDE.FILE_COMMENT, fileComment.toString());
		
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
			header.put(GDE.CHANNEL_CONFIG_NAME, channelConfigName);
			log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = new Integer(new String(buffer).split(GDE.STRING_EQUAL)[1].trim()).intValue();
		log.log(Level.FINE, GDE.CHANNEL_CONFIG_NUMBER + channelNumber);		
		header.put(GDE.CHANNEL_CONFIG_NUMBER, GDE.STRING_EMPTY+channelNumber);

		
		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			log.log(Level.FINEST, "CommunicationPort = " + new String(buffer)); //$NON-NLS-1$
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
		log.log(Level.FINE, GDE.DEVICE_NAME + deviceName);
		header.put(GDE.DEVICE_NAME, deviceName);
		
		position += data_in.skip(headerSize-position);
		header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
		log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

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
		long position = new Long(header.get(GDE.DATA_POINTER_POS)).longValue();
		
		position += data_in.skip(8);
		
		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		log.log(Level.FINE, GDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(GDE.RECORD_SET_SIZE, GDE.STRING_EMPTY+numberRecordSets);

		position += data_in.skipBytes(8);
		
		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(GDE.RECORD_SET_NAME).append(recordSetName).append(GDE.DATA_DELIMITER);
			log.log(Level.FINE, GDE.RECORD_SET_NAME + recordSetName);			
			
			position += data_in.skipBytes(4);

			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetCommentSize = parse2Int(buffer);
			buffer = new byte[recordSetCommentSize];
			position += data_in.read(buffer);
			String recordSetComment = new String(buffer);
			log.log(Level.FINE, GDE.RECORD_SET_COMMENT + recordSetComment);
			sb.append(GDE.RECORD_SET_COMMENT).append(recordSetComment).append(GDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(4);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = tmpDataSize > parse2Int(buffer) ? tmpDataSize : parse2Int(buffer);

			int dataSize = tmpDataSize;
			log.log(Level.FINE, GDE.RECORD_DATA_SIZE + dataSize);
			sb.append(GDE.RECORD_DATA_SIZE).append(dataSize).append(GDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(28);
			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			log.log(Level.FINE, GDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(GDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			header.put((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME, sb.toString());
			log.log(Level.FINE, header.get((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME));
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
		long position = new Long(header.get(GDE.DATA_POINTER_POS)).longValue();
		long headerSize = new Long(header.get(GDE.LOV_HEADER_SIZE)).longValue();
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
			fileComment.append(new String(buffer)).append(GDE.STRING_BLANK);
		}
		log.log(Level.FINE, GDE.FILE_COMMENT + " = " + fileComment.toString()); //$NON-NLS-1$
		header.put(GDE.FILE_COMMENT, fileComment.toString());
		
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
			header.put(GDE.CHANNEL_CONFIG_NAME, channelConfigName);
			log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = new Integer(new String(buffer).split(GDE.STRING_EQUAL)[1].trim()).intValue();
		log.log(Level.FINE, GDE.CHANNEL_CONFIG_NUMBER + channelNumber);		
		header.put(GDE.CHANNEL_CONFIG_NUMBER, GDE.STRING_EMPTY+channelNumber);

		
		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			log.log(Level.FINEST, "CommunicationPort = " + new String(buffer)); //$NON-NLS-1$
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
		log.log(Level.FINE, GDE.DEVICE_NAME + deviceName);
		header.put(GDE.DEVICE_NAME, deviceName);
		
		position += data_in.skip(headerSize-position);
		log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);

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
		long position = new Long(header.get(GDE.DATA_POINTER_POS)).longValue();
		
		position += data_in.skip(8);
		
		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		log.log(Level.FINE, GDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(GDE.RECORD_SET_SIZE, GDE.STRING_EMPTY+numberRecordSets);

		position += data_in.skipBytes(8);
		
		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(GDE.RECORD_SET_NAME).append(recordSetName).append(GDE.DATA_DELIMITER);
			log.log(Level.FINE, GDE.RECORD_SET_NAME + recordSetName);
			
			position += data_in.skipBytes(4);

			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetCommentSize = parse2Int(buffer);
			buffer = new byte[recordSetCommentSize];
			position += data_in.read(buffer);
			String recordSetComment = new String(buffer);
			log.log(Level.FINE, GDE.RECORD_SET_COMMENT + recordSetComment);
			sb.append(GDE.RECORD_SET_COMMENT).append(recordSetComment).append(GDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(4);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = tmpDataSize > parse2Int(buffer) ? tmpDataSize : parse2Int(buffer);

			int dataSize = tmpDataSize;
			log.log(Level.FINE, GDE.RECORD_DATA_SIZE + dataSize);
			sb.append(GDE.RECORD_DATA_SIZE).append(dataSize).append(GDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(16);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			// config block n100W, ...
			StringBuilder config = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberLines = parse2Int(buffer);
			log.log(Level.FINER, "numberLines = " + numberLines); //$NON-NLS-1$
			for (int j = 0; j < numberLines; j++) {
				buffer = new byte[4];
				position += data_in.read(buffer);
				int stringSize = parse2Int(buffer);
				buffer = new byte[stringSize];
				position += data_in.read(buffer);
				config.append(new String(buffer)).append(GDE.DATA_DELIMITER);
				log.log(Level.FINER, new String(buffer));
			}
			header.put(GDE.LOV_CONFIG_DATA, config.toString());
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			position += data_in.skipBytes(8);
			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			log.log(Level.FINE, GDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(GDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			header.put((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME, sb.toString());
			log.log(Level.FINE, header.get((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME));
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
		long position = new Long(header.get(GDE.DATA_POINTER_POS)).longValue();
		long headerSize = new Long(header.get(GDE.LOV_HEADER_SIZE)).longValue();
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
			fileComment.append(new String(buffer)).append(GDE.STRING_BLANK);
		}
		log.log(Level.FINE, GDE.FILE_COMMENT + " = " + fileComment.toString()); //$NON-NLS-1$
		header.put(GDE.FILE_COMMENT, fileComment.toString());
		log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
		
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
			header.put(GDE.CHANNEL_CONFIG_NAME, channelConfigName);
			log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = new Integer(new String(buffer).split(GDE.STRING_EQUAL)[1].trim()).intValue();
		log.log(Level.FINE, GDE.CHANNEL_CONFIG_NUMBER + channelNumber);		
		header.put(GDE.CHANNEL_CONFIG_NUMBER, GDE.STRING_EMPTY+channelNumber);

		
		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			log.log(Level.FINEST, "CommunicationPort = " + new String(buffer)); //$NON-NLS-1$
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
		log.log(Level.FINE, GDE.DEVICE_NAME + deviceName);
		header.put(GDE.DEVICE_NAME, deviceName);
		
		position += data_in.skip(headerSize-position);
		log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);

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
		long position = new Long(header.get(GDE.DATA_POINTER_POS)).longValue();
		
		position += data_in.skip(88);
		log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
		
		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		log.log(Level.FINE, GDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(GDE.RECORD_SET_SIZE, GDE.STRING_EMPTY+numberRecordSets);

		position += data_in.skipBytes(8);
		
		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(GDE.RECORD_SET_NAME).append(recordSetName).append(GDE.DATA_DELIMITER);
			log.log(Level.FINE, GDE.RECORD_SET_NAME + recordSetName);
			
			position += data_in.skipBytes(4);

			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetCommentSize = parse2Int(buffer);
			buffer = new byte[recordSetCommentSize];
			position += data_in.read(buffer);
			String recordSetComment = new String(buffer);
			log.log(Level.FINE, GDE.RECORD_SET_COMMENT + recordSetComment);
			sb.append(GDE.RECORD_SET_COMMENT).append(recordSetComment).append(GDE.DATA_DELIMITER);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			
			position += data_in.skipBytes(2);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetConfigSize = parse2Long(buffer);
			buffer = new byte[(int)recordSetConfigSize];
			position += data_in.read(buffer);
			log.log(Level.FINEST, "RecordSetConfig = " + new String(buffer)); //$NON-NLS-1$
			
			position += data_in.skipBytes(112);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			//position += data_in.skipBytes(122);
			//log.log(Level.INFO, String.format("position = 0x%X", position)); //$NON-NLS-1$

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			int dataSize = parse2Int(buffer);
			
			if (tmpDataSize != dataSize) { 
				log.log(Level.WARNING, "data size calculation wrong");
			}	
				log.log(Level.FINE, GDE.RECORD_DATA_SIZE + dataSize);
				sb.append(GDE.RECORD_DATA_SIZE).append(dataSize).append(GDE.DATA_DELIMITER);
				log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
				
				position += data_in.skipBytes(216);
				log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
	
			// config block n100W, ...
			StringBuilder config = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberLines = parse2Int(buffer);
			log.log(Level.FINE, "numberLines = " + numberLines); //$NON-NLS-1$
			for (int j = 0; j < numberLines; j++) {
				buffer = new byte[4];
				position += data_in.read(buffer);
				int stringSize = parse2Int(buffer);
				buffer = new byte[stringSize];
				position += data_in.read(buffer);
				config.append(new String(buffer)).append(GDE.DATA_DELIMITER);
				log.log(Level.FINER, new String(buffer));
			}
			header.put(GDE.LOV_CONFIG_DATA, config.toString());
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			//position += data_in.skipBytes(8);
			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			log.log(Level.FINE, GDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(GDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			header.put((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME, sb.toString());
			log.log(Level.FINE, header.get((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME));
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
		long position = new Long(header.get(GDE.DATA_POINTER_POS)).longValue();
		long headerSize = new Long(header.get(GDE.LOV_HEADER_SIZE)).longValue();
		// read file comment
		byte[] buffer = new byte[8];
		position += data_in.read(buffer);
		long fileCommentSize = parse2Long(buffer); 
		buffer = new byte[(int)fileCommentSize];
		position += data_in.read(buffer);
		String rtfString = new String(buffer);
		log.log(Level.FINEST, rtfString);
		
		String fileComment = parseRtfString(rtfString);
		log.log(Level.FINE, GDE.FILE_COMMENT + " = " + fileComment); //$NON-NLS-1$
		header.put(GDE.FILE_COMMENT, fileComment);
		log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

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
			header.put(GDE.CHANNEL_CONFIG_NAME, channelConfigName);
			log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = new Integer(new String(buffer).split(GDE.STRING_EQUAL)[1].trim()).intValue();
		log.log(Level.FINE, GDE.CHANNEL_CONFIG_NUMBER + channelNumber);		
		header.put(GDE.CHANNEL_CONFIG_NUMBER, GDE.STRING_EMPTY+channelNumber);
				
		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			log.log(Level.FINEST, "CommunicationPort = " + new String(buffer)); //$NON-NLS-1$
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
		log.log(Level.FINE, GDE.DEVICE_NAME + deviceName);
		header.put(GDE.DEVICE_NAME, deviceName);
		
		position += data_in.skip(8);
		
		// read device configuration
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceConfigLineSize = parse2Int(buffer);
		log.log(Level.FINEST, "DeviceConfigLineSize = " + deviceConfigLineSize); //$NON-NLS-1$
		
		for (int i = 0; i < deviceConfigLineSize; i++) {
			// read device ini line
			buffer = new byte[4];
			position += data_in.read(buffer);
			int lineSize = parse2Int(buffer);
			buffer = new byte[lineSize];
			position += data_in.read(buffer);
			String configLine = new String(buffer);
			if (configLine.startsWith(GDE.LOV_TIME_STEP)) 
				header.put(RecordSet.TIME_STEP_MS, configLine.split(GDE.STRING_EQUAL)[1]);
			else if (configLine.startsWith(GDE.LOV_NUM_MEASUREMENTS))
				header.put(GDE.LOV_NUM_MEASUREMENTS, GDE.STRING_EMPTY+ ((new Integer(configLine.split(GDE.STRING_EQUAL)[1].trim()).intValue()) - 1)); // -1 == time
			log.log(Level.FINEST, configLine);
		}

		// end of header sometimes after headerSize
		position += data_in.skip(headerSize-position);
		//**** end main header			
		header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
		log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

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
		long position = new Long(header.get(GDE.DATA_POINTER_POS)).longValue();
		
		position += data_in.skip(88);
		log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
		
		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		log.log(Level.FINE, GDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(GDE.RECORD_SET_SIZE, GDE.STRING_EMPTY+numberRecordSets);
	
		position += data_in.skipBytes(8);
		
		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(GDE.RECORD_SET_NAME).append(recordSetName).append(GDE.DATA_DELIMITER);
			log.log(Level.FINE, GDE.RECORD_SET_NAME + recordSetName);
						
			position += data_in.skipBytes(2);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetConfigSize = parse2Long(buffer);
			buffer = new byte[(int)recordSetConfigSize];
			position += data_in.read(buffer);
			log.log(Level.FINEST, "RecordSetConfig = " + new String(buffer)); //$NON-NLS-1$
			
			position += data_in.skipBytes(112);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = tmpDataSize > parse2Int(buffer) ? tmpDataSize : parse2Int(buffer);
			
			int dataSize = tmpDataSize;
			log.log(Level.FINE, GDE.RECORD_DATA_SIZE + dataSize);
			sb.append(GDE.RECORD_DATA_SIZE).append(dataSize).append(GDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(16);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			// config block n100W, ...
			StringBuilder config = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberLines = parse2Int(buffer);
			log.log(Level.FINER, "numberLines = " + numberLines); //$NON-NLS-1$
			for (int j = 0; j < numberLines; j++) {
				buffer = new byte[4];
				position += data_in.read(buffer);
				int stringSize = parse2Int(buffer);
				buffer = new byte[stringSize];
				position += data_in.read(buffer);
				config.append(new String(buffer)).append(GDE.DATA_DELIMITER);
				log.log(Level.FINER, new String(buffer));
			}
			header.put(GDE.LOV_CONFIG_DATA, config.toString());
			
			position += data_in.skipBytes(4);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			long rtfCommentSize = parse2Long(buffer);
			buffer = new byte[(int)rtfCommentSize];
			position += data_in.read(buffer);
			String rtfString = new String(buffer);
			log.log(Level.FINEST, rtfString);
			
			String recordSetComment = parseRtfString(rtfString);
			log.log(Level.FINE, GDE.RECORD_SET_COMMENT + recordSetComment);
			sb.append(GDE.RECORD_SET_COMMENT).append(recordSetComment).append(GDE.DATA_DELIMITER);

			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			buffer = new byte[parse2Int(buffer)];
			position += data_in.read(buffer);
			log.log(Level.FINEST, new String(buffer));
			log.log(Level.FINER, String.format("position = 0x%X", position));			 //$NON-NLS-1$
			
			position += data_in.skip(175);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			log.log(Level.FINE, GDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(GDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			header.put((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME, sb.toString());
			log.log(Level.FINE, header.get((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME));
		}
		return header;
	}

	/**
	 * read extended header info which is part of base header of format stream version 4
	 * @param data_in
	 * @param header
	 * @throws IOException
	 * @throws NotSupportedException 
	 */
	private static HashMap<String, String> getHeaderInfo_4(DataInputStream data_in, HashMap<String, String> header) throws IOException, NotSupportedException {
		long position = new Long(header.get(GDE.DATA_POINTER_POS)).longValue();
		long headerSize = new Long(header.get(GDE.LOV_HEADER_SIZE)).longValue();
		// read file comment
		byte[] buffer = new byte[8];
		position += data_in.read(buffer);
		long fileCommentSize = parse2Long(buffer); 
		buffer = new byte[(int)fileCommentSize];
		position += data_in.read(buffer);
		String rtfString = new String(buffer);
		log.log(Level.FINEST, rtfString);
		
		String fileComment = parseRtfString(rtfString);
		log.log(Level.FINE, GDE.FILE_COMMENT + " = " + fileComment); //$NON-NLS-1$
		header.put(GDE.FILE_COMMENT, fileComment);
		
		log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
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
			header.put(GDE.CHANNEL_CONFIG_NAME, channelConfigName);
			log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = new Integer(new String(buffer).split(GDE.STRING_EQUAL)[1].trim()).intValue();
		log.log(Level.FINE, GDE.CHANNEL_CONFIG_NUMBER + channelNumber);		
		header.put(GDE.CHANNEL_CONFIG_NUMBER, GDE.STRING_EMPTY+channelNumber);
				
		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			log.log(Level.FINEST, "CommunicationPort = " + new String(buffer)); //$NON-NLS-1$
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
		log.log(Level.FINE, GDE.DEVICE_NAME + deviceName);
		header.put(GDE.DEVICE_NAME, deviceName);
		
		position += data_in.skip(8);
		
		// read device configuration
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceConfigLineSize = parse2Int(buffer);
		log.log(Level.FINEST, "DeviceConfigLineSize = " + deviceConfigLineSize); //$NON-NLS-1$
		
		for (int i = 0; i < deviceConfigLineSize; i++) {
			// read device ini line
			buffer = new byte[4];
			position += data_in.read(buffer);
			int lineSize = parse2Int(buffer);
			buffer = new byte[lineSize];
			position += data_in.read(buffer);
			String configLine = new String(buffer);
			if (configLine.startsWith(GDE.LOV_TIME_STEP)) 
				header.put(RecordSet.TIME_STEP_MS, configLine.split(GDE.STRING_EQUAL)[1]);
			else if (configLine.startsWith(GDE.LOV_NUM_MEASUREMENTS))
				header.put(GDE.LOV_NUM_MEASUREMENTS, GDE.STRING_EMPTY+ ((new Integer(configLine.split(GDE.STRING_EQUAL)[1].trim()).intValue()) - 1)); // -1 == time
			log.log(Level.FINEST, configLine);
		}

		// end of header sometimes after headerSize
		position += data_in.skip(headerSize-position);
		//**** end main header			
		header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
		log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

		return header;
	}	

	/**
	 * get the record set and dependent record parameters of format stream version
	 * @param device
	 * @param data_in
	 * @param header
	 * @throws IOException
	 */
	private static HashMap<String, String> getRecordSetInfo_4(DataInputStream data_in, HashMap<String, String> header) throws IOException {
		long position = new Long(header.get(GDE.DATA_POINTER_POS)).longValue();
		
		position += data_in.skip(88);
		log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
		
		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		log.log(Level.FINE, GDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(GDE.RECORD_SET_SIZE, GDE.STRING_EMPTY+numberRecordSets);
	
		position += data_in.skipBytes(8);
		
		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(GDE.RECORD_SET_NAME).append(recordSetName).append(GDE.DATA_DELIMITER);
			log.log(Level.FINE, GDE.RECORD_SET_NAME + recordSetName);
			
			position += data_in.skipBytes(2);

			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetConfigSize = parse2Long(buffer);
			buffer = new byte[(int)recordSetConfigSize];
			position += data_in.read(buffer);
			log.log(Level.FINEST, "RecordSetConfig = " + new String(buffer)); //$NON-NLS-1$
			
			position += data_in.skipBytes(112);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			int tmpDataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			tmpDataSize = tmpDataSize > parse2Int(buffer) ? tmpDataSize : parse2Int(buffer);
			
			int dataSize = tmpDataSize;
			log.log(Level.FINE, GDE.RECORD_DATA_SIZE + dataSize);
			sb.append(GDE.RECORD_DATA_SIZE).append(dataSize).append(GDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(16);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			// config block n100W, ...
			StringBuilder config = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberLines = parse2Int(buffer);
			log.log(Level.FINER, "numberLines = " + numberLines); //$NON-NLS-1$
			for (int j = 0; j < numberLines; j++) {
				buffer = new byte[4];
				position += data_in.read(buffer);
				int stringSize = parse2Int(buffer);
				buffer = new byte[stringSize];
				position += data_in.read(buffer);
				config.append(new String(buffer)).append(GDE.DATA_DELIMITER);
				log.log(Level.FINER, new String(buffer));
			}
			header.put(GDE.LOV_CONFIG_DATA, config.toString());
			
			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			long rtfCommentSize = parse2Long(buffer);
			buffer = new byte[(int)rtfCommentSize];
			position += data_in.read(buffer);
			String rtfString = new String(buffer);
			log.log(Level.FINEST, rtfString);
			
			String recordSetComment = parseRtfString(rtfString);
			log.log(Level.FINE, GDE.RECORD_SET_COMMENT + recordSetComment);
			sb.append(GDE.RECORD_SET_COMMENT).append(recordSetComment).append(GDE.DATA_DELIMITER);

			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			buffer = new byte[parse2Int(buffer)];
			position += data_in.read(buffer);
			log.log(Level.FINEST, new String(buffer));
			
			
			position += data_in.skip(175);

			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			log.log(Level.FINE, GDE.RECORD_SET_DATA_BYTES + recordSetDataBytes);
			sb.append(GDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			header.put((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME, sb.toString());
			log.log(Level.FINE, header.get((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME));
		}
		return header;
	}

	/**
	 * read extended header info which is part of base header of format stream version 5
	 * @param data_in
	 * @param header
	 * @throws IOException
	 * @throws NotSupportedException 
	 */
	private static HashMap<String, String> getHeaderInfo_5(DataInputStream data_in, HashMap<String, String> header) throws IOException, NotSupportedException {
		long position = new Long(header.get(GDE.DATA_POINTER_POS)).longValue();
		long headerSize = new Long(header.get(GDE.LOV_HEADER_SIZE)).longValue();
		byte[] buffer = new byte[0];
		long fileCommentSize = 0;
		log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
		
		// read file comment
		buffer = new byte[8];
		position += data_in.read(buffer);
		fileCommentSize = parse2Long(buffer); 
		buffer = new byte[(int)fileCommentSize];
		position += data_in.read(buffer);
		String rtfString = new String(buffer);
		
		String fileComment = parseRtfString(rtfString);
		log.log(Level.FINE, GDE.FILE_COMMENT + " = " + fileComment); //$NON-NLS-1$
		header.put(GDE.FILE_COMMENT, fileComment);
		log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

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
			header.put(GDE.CHANNEL_CONFIG_NAME, channelConfigName);
			log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + channelConfigName);
		}
		int channelNumber = new Integer(new String(buffer).split(GDE.STRING_EQUAL)[1].trim()).intValue();
		log.log(Level.FINE, GDE.CHANNEL_CONFIG_NUMBER + channelNumber);		
		header.put(GDE.CHANNEL_CONFIG_NUMBER, GDE.STRING_EMPTY+channelNumber);
				
		// read communication port
		buffer = new byte[4];
		position += data_in.read(buffer);
		int comStrSize = parse2Int(buffer);
		if (comStrSize != 0) {
			buffer = new byte[comStrSize];
			position += data_in.read(buffer);
			log.log(Level.FINEST, "CommunicationPort = " + new String(buffer)); //$NON-NLS-1$
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
		log.log(Level.FINE, GDE.DEVICE_NAME + deviceName);
		header.put(GDE.DEVICE_NAME, deviceName);
		
		position += data_in.skip(8);
		
		// read device configuration
		buffer = new byte[4];
		position += data_in.read(buffer);
		int deviceConfigLineSize = parse2Int(buffer);
		log.log(Level.FINEST, "DeviceConfigLineSize = " + deviceConfigLineSize); //$NON-NLS-1$
		
		for (int i = 0; i < deviceConfigLineSize; i++) {
			// read device ini line
			buffer = new byte[4];
			position += data_in.read(buffer);
			int lineSize = parse2Int(buffer);
			buffer = new byte[lineSize];
			position += data_in.read(buffer);
			String configLine = new String(buffer);
			if (configLine.startsWith(GDE.LOV_TIME_STEP)) 
				header.put(RecordSet.TIME_STEP_MS, configLine.split(GDE.STRING_EQUAL)[1]);
			else if (configLine.startsWith(GDE.LOV_NUM_MEASUREMENTS))
				header.put(GDE.LOV_NUM_MEASUREMENTS, GDE.STRING_EMPTY+ ((new Integer(configLine.split(GDE.STRING_EQUAL)[1].trim()).intValue()) - 1)); // -1 == time
			log.log(Level.FINEST, configLine);
		}

		// end of header sometimes after headerSize
		position += data_in.skip(headerSize-position);
		//**** end main header			
		header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
		log.log(Level.FINE, String.format("position = 0x%X", position)); //$NON-NLS-1$

		return header;
	}


	/**
	 * parse RTF String into plain text
	 * @param rtfString
	 * @return
	 */
	static String parseRtfString(String rtfString) {
		log.log(Level.FINEST, rtfString);
		StringBuilder fileComment = new StringBuilder();

		if (rtfString.indexOf("\\plain ") != -1 ) { // plain text exist in RTF string
			String[] array = rtfString.split("plain ");
			for (int i = 1; i < array.length; i++) {
				int beginIndex = 0, endIndex = 0;
				while ((beginIndex = array[i].indexOf(" ", beginIndex)) != -1				// \fs22 Text
						&& (endIndex = array[i].indexOf("\\", beginIndex)) != -1) { 
					fileComment.append(array[i].substring(beginIndex+1, endIndex));
					beginIndex = endIndex+1; 
				}
			}
		}
		while (fileComment.length() > 1 && (fileComment.lastIndexOf("\n") == fileComment.length()-1 || fileComment.lastIndexOf("\r") == fileComment.length()-1) ) 
			fileComment.deleteCharAt(fileComment.length()-1);
		
		return fileComment.toString();
	}	

	/**
	 * get the record set and dependent record parameters of format stream version 5
	 * @param device
	 * @param data_in
	 * @param header
	 * @throws IOException
	 */
	private static HashMap<String, String> getRecordSetInfo_5(DataInputStream data_in, HashMap<String, String> header) throws IOException {
		long position = new Long(header.get(GDE.DATA_POINTER_POS)).longValue();
		
		position += data_in.skip(88);
		log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
		
		// read number record sets
		byte[] buffer = new byte[4];
		position += data_in.read(buffer);
		int numberRecordSets = parse2Int(buffer);
		log.log(Level.FINE, GDE.RECORD_SET_SIZE + numberRecordSets);
		header.put(GDE.RECORD_SET_SIZE, GDE.STRING_EMPTY+numberRecordSets);
	
		position += data_in.skipBytes(8);
		
		for (int i = 0; i < numberRecordSets; i++) {
			StringBuilder sb = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int recordSetNameSize = parse2Int(buffer);
			buffer = new byte[recordSetNameSize];
			position += data_in.read(buffer);
			String recordSetName = new String(buffer);
			sb.append(GDE.RECORD_SET_NAME).append(recordSetName).append(GDE.DATA_DELIMITER);
			log.log(Level.FINE, GDE.RECORD_SET_NAME + recordSetName);
			
			position += data_in.skipBytes(2);

			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetConfigSize = parse2Long(buffer);
			buffer = new byte[(int)recordSetConfigSize];
			position += data_in.read(buffer);
			log.log(Level.FINEST, "RecordSetConfig = " + new String(buffer)); //$NON-NLS-1$			
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			position += data_in.skipBytes(112);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			int dataSize = 0;
			buffer = new byte[4];
			position += data_in.read(buffer);
			dataSize = parse2Int(buffer);
			buffer = new byte[4];
			position += data_in.read(buffer);
			dataSize = dataSize > parse2Int(buffer) ? dataSize : parse2Int(buffer);
			log.log(Level.FINE, GDE.RECORD_DATA_SIZE + dataSize);
			sb.append(GDE.RECORD_DATA_SIZE).append(dataSize).append(GDE.DATA_DELIMITER);
			
			position += data_in.skipBytes(16);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			// config block n100W, ...
			StringBuilder config = new StringBuilder();
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberLines = parse2Int(buffer);
			log.log(Level.FINER, "numberLines = " + numberLines); //$NON-NLS-1$
			for (int j = 0; j < numberLines; j++) {
				buffer = new byte[4];
				position += data_in.read(buffer);
				int stringSize = parse2Int(buffer);
				buffer = new byte[stringSize];
				position += data_in.read(buffer);
				config.append(new String(buffer)).append(GDE.DATA_DELIMITER);
				log.log(Level.FINER, new String(buffer));
			}
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			header.put(GDE.LOV_CONFIG_DATA, config.toString());
			
			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			long rtfCommentSize = parse2Long(buffer);
			buffer = new byte[(int)rtfCommentSize];
			position += data_in.read(buffer);
			String rtfString = new String(buffer);
			
			String recordSetComment = parseRtfString(rtfString);
			log.log(Level.FINE, GDE.RECORD_SET_COMMENT + recordSetComment.toString());
			sb.append(GDE.RECORD_SET_COMMENT).append(recordSetComment.toString()).append(GDE.DATA_DELIMITER);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			// rtf block
			buffer = new byte[8];
			position += data_in.read(buffer);
			buffer = new byte[parse2Int(buffer)];
			position += data_in.read(buffer);
			log.log(Level.FINEST, new String(buffer));
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$			
			
			buffer = new byte[115];
			position += data_in.read(buffer);
			log.log(Level.FINEST, new String(buffer));
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			//position += data_in.skip(115);
			//log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			//time format
			buffer = new byte[4];
			position += data_in.read(buffer);
			int numberChars = parse2Int(buffer);
			log.log(Level.FINER, "numberChars = " + numberChars); //$NON-NLS-1$
			buffer = new byte[parse2Int(buffer)];
			position += data_in.read(buffer);
			log.log(Level.FINEST, new String(buffer));			
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$			
			
			position += data_in.skip(56);
			log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$

			buffer = new byte[8];
			position += data_in.read(buffer);
			long recordSetDataBytes = parse2Long(buffer);
			log.log(Level.FINE, String.format("%s%d (%X%X%X%X%X%X%X%X)", GDE.RECORD_SET_DATA_BYTES, recordSetDataBytes, buffer[0], buffer[1], buffer[2], buffer[3], buffer[4], buffer[5], buffer[6], buffer[7]));
			sb.append(GDE.RECORD_SET_DATA_BYTES).append(recordSetDataBytes);

			header.put(GDE.DATA_POINTER_POS, GDE.STRING_EMPTY+position);
			log.log(Level.FINE, String.format("position = 0x%X", position)); //$NON-NLS-1$
			
			header.put((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME, sb.toString());
			log.log(Level.FINE, header.get((i+1)+GDE.STRING_BLANK + GDE.RECORD_SET_NAME));
		}
		log.log(Level.FINER, String.format("position = 0x%X", position)); //$NON-NLS-1$
		return header;
	}

	/**
	 * parse data buffer to integer value, length meight be 1, 2, 3, 4, 8 bytes
	 * @param buffer
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
			return (((buffer[3] & 0xff) << 24) | ((buffer[2] & 0xff) << 16) | ((buffer[1] & 0xff) << 8) | (buffer[0] & 0xff));
//		case 16:
//			return (((buffer[7] & 0xff) << 56) | ((buffer[6] & 0xff) << 48) | ((buffer[5] & 0xff) << 40) | ((buffer[4] & 0xff) << 32) | ((buffer[3] & 0xff) << 24) | ((buffer[2] & 0xff) << 16) | ((buffer[1] & 0xff) << 8) | (buffer[0] & 0xff));
		}
	}

	/**
	 * parse data buffer to long value, data buffer length must be 8 bytes
	 * @param buffer
	 */
	public static long parse2Long(byte[] buffer) {
		long tmpLong1 = ((long)(buffer[3] & 0xff) << 24) + ((buffer[2] & 0xff) << 16) + ((buffer[1] & 0xff) << 8) + ((buffer[0] & 0xff) << 0);
		long tmpLong2 = (((long)buffer[7] & 255) << 56) + ((long)(buffer[6] & 255) << 48) + ((long)(buffer[5] & 255) << 40) + ((long)(buffer[4] & 255) << 32);
    return  tmpLong2 + tmpLong1;
		
	}
	
	/**
	 * map LogView device names with GDE device names if possible
	 * @param deviceName
	 * @return
	 * @throws NotSupportedException 
	 */
	private static String mapLovDeviceNames(String deviceName) throws NotSupportedException {
		String deviceKey = deviceName.toLowerCase().trim();
		if (!deviceMap.containsKey(deviceKey)) {
			String msg = Messages.getString(MessageIds.GDE_MSGW0016, new Object[] { deviceName }); 
			NotSupportedException e = new NotSupportedException(msg);
			log.log(Level.WARNING, e.getMessage(), e);
			throw e;
		}
		
		return deviceMap.get(deviceKey);
	}
}
