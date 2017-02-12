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
    
    Copyright (c) 2011,2012,2013,2014,2015,2016,2017 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;

/**
 * Class to read Graupner HoTT binary data as saved on SD-Cards
 * 
 * @author Winfried Brügmann
 */
public class HoTTbinReader {
	final static String													$CLASS_NAME									= HoTTbinReader.class.getName();
	final static Logger													log													= Logger.getLogger(HoTTbinReader.$CLASS_NAME);
	protected static final int									LOG_RECORD_SCAN_START				= 4000;																																														// 64 byte = 0.01
	// seconds for
	// 40 seconds
	// maximum
	// sensor scan
	// time (40 /
	// 0.01 = 4000)
	protected static final int									NUMBER_LOG_RECORDS_TO_SCAN	= 1500;
	protected static final int									NUMBER_LOG_RECORDS_MIN			= 7000;

	final static DataExplorer										application									= DataExplorer.getInstance();
	final static Channels												channels										= Channels.getInstance();

	static int																	dataBlockSize								= 64;
	static byte[]																buf;
	static byte[]																buf0, buf1, buf2, buf3, buf4, buf5, buf6, buf7, buf8, buf9, bufA, bufB, bufC, bufD;
	static long																	timeStep_ms;
	static int[]																pointsReceiver, pointsGeneral, pointsElectric, pointsVario, pointsGPS, pointsChannel, pointsSpeedControl;
	static RecordSet														recordSetReceiver, recordSetGeneral, recordSetElectric, recordSetVario, recordSetGPS, recordSetChannel, recordSetSpeedControl;
	static int																	tmpVoltageRx								= 0;
	static int																	tmpTemperatureRx						= 0;
	static int																	tmpHeight										= 0;
	static int																	tmpTemperatureFet						= 0;
	static int																	tmpTemperatureExt						= 0;
	static int																	tmpVoltage									= 0;
	static int																	tmpCurrent									= 0;
	static int																	tmpRevolution								= 0;
	static int																	tmpClimb1										= 0;
	static int																	tmpClimb3										= 0;
	static int																	tmpClimb10									= 0;
	static int																	tmpVoltage1									= 0;
	static int																	tmpVoltage2									= 0;
	static int																	tmpCapacity									= 0;
	static int																	tmpVelocity									= 0;
	static int																	tmpLatitude									= 0;
	static int																	tmpLatitudeDelta						= 0;
	static double																latitudeTolerance						= 1;
	static long																	lastLatitudeTimeStep				= 0;
	static int																	tmpLongitude								= 0;
	static int																	tmpLongitudeDelta						= 0;
	static double																longitudeTolerance					= 1;
	static long																	lastLongitudeTimeStep				= 0;
	static int																	countLostPackages						= 0;
	static boolean															isJustParsed								= false;
	static boolean															isReceiverOnly							= false;
	static StringBuilder												sensorSignature;
	static boolean															isTextModusSignaled					= false;
	static int																	oldProtocolCount						= 0;
	static Vector<Byte>													blockSequenceCheck;

	static ReverseChannelPackageLossStatistics	lostPackages								= new ReverseChannelPackageLossStatistics();

	/**
	 * get data file info data
	 * 
	 * @param buffer
	 *            byte array containing the first 64 byte to analyze the header
	 * @return hash map containing header data as string accessible by public
	 *         header keys
	 * @throws IOException 
	 * @throws DataTypeException 
	 * @throws Exception
	 */
	public static HashMap<String, String> getFileInfo(File file) throws IOException, DataTypeException {
		final String $METHOD_NAME = "getFileInfo";
		FileInputStream file_input = null;
		DataInputStream data_in = null;
		byte[] buffer = new byte[64];
		HashMap<String, String> fileInfo;
		int sensorCount = 0;
		long numberLogs = (file.length() / 64);
		HoTTbinReader.sensorSignature = new StringBuilder().append(GDE.STRING_LEFT_BRACKET).append(HoTTAdapter.Sensor.RECEIVER.name()).append(GDE.STRING_COMMA);

		try {
			fileInfo = new HashMap<String, String>();
			fileInfo.put(HoTTAdapter.FILE_PATH, file.getPath());
			file_input = new FileInputStream(file);
			data_in = new DataInputStream(file_input); // MARK ### see below

			// begin evaluate for HoTTAdapterX files containing normal HoTT V4
			// sensor data
			data_in.read(buffer);

			if (new String(buffer).startsWith("GRAUPNER SD LOG8")) {
				boolean isHoTTV4 = true;
				data_in.close();
				file_input = new FileInputStream(file);
				data_in = new DataInputStream(file_input);
				buffer = new byte[HoTTbinReaderX.headerSize];
				data_in.read(buffer);
				buffer = new byte[64];
				for (int i = 0; i < 4; i++) {
					data_in.read(buffer);
					if (buffer[0] != i + 1)
						isHoTTV4 = false;
				}
				data_in.close();
				if (isHoTTV4) {
					file_input = new FileInputStream(file);
					data_in = new DataInputStream(file_input);
					buffer = new byte[HoTTbinReaderX.headerSize];
					File outputFile = new File(GDE.JAVA_IO_TMPDIR + GDE.FILE_SEPARATOR + "~" + file.getPath().substring(1 + file.getPath().lastIndexOf(GDE.FILE_SEPARATOR)));
					FileOutputStream file_output = new FileOutputStream(outputFile);
					DataOutputStream data_out = new DataOutputStream(file_output);
					long fileSize = file.length() - HoTTbinReaderX.headerSize - HoTTbinReaderX.footerSize;
					buffer = new byte[HoTTbinReaderX.headerSize];
					data_in.read(buffer);
					buffer = new byte[64];
					for (int i = 0; i < fileSize / buffer.length; i++) {
						if (buffer.length == data_in.read(buffer))
							data_out.write(buffer);
					}
					data_out.close();
					data_out = null;
					data_in.close();
					data_in = null;
					return getFileInfo(outputFile);
				}
				DataExplorer.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2410));
				throw new DataTypeException(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2410));
			}
			// end evaluate for HoTTAdapterX files containing normal HoTT V4
			// sensor data

			if (numberLogs < 7000) {
				HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2406));
			}
			else if (numberLogs < 5500) {
				HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2407));
				throw new IOException(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2407));
			}

			for (int i = 0; i < HoTTAdapter.isSensorType.length; i++) {
				HoTTAdapter.isSensorType[i] = false;
			}

			if (data_in != null)
				data_in.close(); // ET MARK ### not sure if there is a resource
			// leak without this line
			file_input = new FileInputStream(file);
			data_in = new DataInputStream(file_input);
			data_in.read(buffer);

			long position = (file.length() / 2) - ((NUMBER_LOG_RECORDS_TO_SCAN * 64) / 2);
			position = position - position % 64;
			if (position <= 0) {
				sensorCount = 1;
			}
			else {
				if (position > 64 * 4000) {
					// 64 byte = 0.01 seconds for 40 seconds maximum sensor scan
					// time (40 / 0.01 = 6000)
					position = 64 * 4000;
				}

				data_in.skip(position - 64);
				for (int i = 0; i < NUMBER_LOG_RECORDS_TO_SCAN; i++) {
					data_in.read(buffer);
					if (HoTTbinReader.log.isLoggable(Level.FINER)) HoTTbinReader.log.logp(Level.FINER, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(buffer, buffer.length));

					switch (buffer[7]) {
					case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
						if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.VARIO.ordinal()] == false) HoTTbinReader.sensorSignature.append(HoTTAdapter.Sensor.VARIO.name()).append(GDE.STRING_COMMA);
						HoTTAdapter.isSensorType[HoTTAdapter.Sensor.VARIO.ordinal()] = true;
						break;
					case HoTTAdapter.SENSOR_TYPE_GPS_19200:
						if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GPS.ordinal()] == false) HoTTbinReader.sensorSignature.append(HoTTAdapter.Sensor.GPS.name()).append(GDE.STRING_COMMA);
						HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GPS.ordinal()] = true;
						break;
					case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
						if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()] == false) HoTTbinReader.sensorSignature.append(HoTTAdapter.Sensor.GAM.name()).append(GDE.STRING_COMMA);
						HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()] = true;
						break;
					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
						if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.EAM.ordinal()] == false) HoTTbinReader.sensorSignature.append(HoTTAdapter.Sensor.EAM.name()).append(GDE.STRING_COMMA);
						HoTTAdapter.isSensorType[HoTTAdapter.Sensor.EAM.ordinal()] = true;
						break;
					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
						if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()] == false) HoTTbinReader.sensorSignature.append(HoTTAdapter.Sensor.ESC.name()).append(GDE.STRING_COMMA);
						HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()] = true;
						break;
					}
				}
				for (boolean element : HoTTAdapter.isSensorType) {
					if (element == true)
					++sensorCount;
				}
			}
			HoTTbinReader.sensorSignature.deleteCharAt(HoTTbinReader.sensorSignature.length() - 1).append(GDE.STRING_RIGHT_BRACKET);
			fileInfo.put(HoTTAdapter.SENSOR_COUNT, GDE.STRING_EMPTY + sensorCount);
			fileInfo.put(HoTTAdapter.LOG_COUNT, GDE.STRING_EMPTY + (file.length() / 64));

			if (HoTTbinReader.log.isLoggable(Level.FINE)) for (Entry<String, String> entry : fileInfo.entrySet()) {
				HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, entry.getKey() + " = " + entry.getValue());
				HoTTbinReader.log.log(Level.FINE, file.getName() + " - " + "sensor count = " + sensorCount);
			}
		}
		finally {
			if (data_in != null) data_in.close();
			if (file_input != null) file_input.close();
		}
		return fileInfo;
	}

	// 0=Rx dbm to Strength lookup table
	static int[] lookup = new int[] { 95, 95, 95, 95, 95, 95, 95, 95, 95, 95, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 85, 85, 85, 85, 85, 80, 75, 70, 65, 60, 55, 50, 45, 40, 35, 30, 30, 25, 25, 20, 20,
			20, 15, 15, 10, 10, 5, 5, 5, 5, 5, 5, 0, 0, 0, 0, 0, 0, 0 };

	/**
	 * convert from Rx dbm to strength using lookup table
	 * 
	 * @param inValue
	 * @return
	 */
	static int convertRxDbm2Strength(int inValue) {
		if (inValue >= 40 && inValue < HoTTbinReader.lookup.length + 40) {
			return HoTTbinReader.lookup[inValue - 40];
		}
		else if (inValue < 40)
			return 100;
		else
			return 0;
	}

	/**
	 * read complete file data and display the first found record set
	 * 
	 * @param filePath
	 * @throws Exception
	 */
	public static synchronized void read(String filePath) throws Exception {
		File file = null;
		try {
			HashMap<String, String> header = getFileInfo(new File(filePath));

			if (Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) <= 1) {
				HoTTbinReader.isReceiverOnly = Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) == 0;
				readSingle(file = new File(header.get(HoTTAdapter.FILE_PATH)));
			}
			else
				readMultiple(file = new File(header.get(HoTTAdapter.FILE_PATH)));
		}
		finally {
			if (file != null && file.getName().startsWith("~") && file.exists()) file.delete();
		}
	}

	/**
	 * read log data according to version 0
	 * 
	 * @param file
	 * @throws IOException
	 * @throws DataInconsitsentException
	 */
	static void readSingle(File file) throws IOException, DataInconsitsentException {
		final String $METHOD_NAME = "readSingle";
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		HoTTAdapter device = (HoTTAdapter) HoTTbinReader.application.getActiveDevice();
		int recordSetNumber = HoTTbinReader.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		boolean isInitialSwitched = false;
		HoTTbinReader.recordSetReceiver = null; // 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=UminRx
		HoTTbinReader.recordSetGeneral = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		HoTTbinReader.recordSetElectric = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2, 27=Revolution
		HoTTbinReader.recordSetVario = null; // 0=RXSQ, 1=Height, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
		HoTTbinReader.recordSetGPS = null; // 0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		HoTTbinReader.recordSetChannel = null; // 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16
		HoTTbinReader.recordSetSpeedControl = null; // 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature
		HoTTbinReader.pointsReceiver = new int[9];
		HoTTbinReader.pointsGeneral = new int[21];
		HoTTbinReader.pointsElectric = new int[28];
		HoTTbinReader.pointsVario = new int[7];
		HoTTbinReader.pointsVario[2] = 100000;
		HoTTbinReader.pointsGPS = new int[12];
		HoTTbinReader.pointsChannel = new int[23];
		HoTTbinReader.pointsSpeedControl = new int[13];
		HoTTbinReader.timeStep_ms = 0;
		HoTTbinReader.dataBlockSize = 64;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		HoTTbinReader.buf0 = new byte[30];
		HoTTbinReader.buf1 = null;
		HoTTbinReader.buf2 = null;
		HoTTbinReader.buf3 = null;
		HoTTbinReader.buf4 = null;
		int version = -1;
		HoTTAdapter.reverseChannelPackageLossCounter.clear();
		HoTTbinReader.lostPackages.clear();
		HoTTbinReader.countLostPackages = 0;
		HoTTbinReader.isJustParsed = false;
		int countPackageLoss = 0;
		long numberDatablocks = fileSize / HoTTbinReader.dataBlockSize / (HoTTbinReader.isReceiverOnly && !HoTTAdapter.isChannelsChannelEnabled ? 10 : 1);
		long startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(file, numberDatablocks);
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		MenuToolBar menuToolBar = HoTTbinReader.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		if (menuToolBar != null)
			HoTTbinReader.application.setProgress(0, sThreadId);

		try {
			HoTTAdapter.recordSets.clear();
			// receiver data are always contained
			// check if recordSetReceiver initialized, transmitter and receiver
			// data always present, but not in the same data rate and signals
			channel = HoTTbinReader.channels.get(1);
			channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
			recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.RECEIVER.value() + recordSetNameExtend;
			HoTTbinReader.recordSetReceiver = RecordSet.createRecordSet(recordSetName, device, 1, true, true, true);
			channel.put(recordSetName, HoTTbinReader.recordSetReceiver);
			HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.RECEIVER.value(), HoTTbinReader.recordSetReceiver);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (HoTTbinReader.application.getMenuToolBar() != null) {
				channel.applyTemplate(recordSetName, false);
			}
			// recordSetReceiver initialized and ready to add data

			if (HoTTAdapter.isChannelsChannelEnabled) {
				// channel data are always contained
				// check if recordSetChannel initialized, transmitter and
				// receiver data always present, but not in the same data rate
				// and signals
				channel = HoTTbinReader.channels.get(6);
				channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
				recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.CHANNEL.value() + recordSetNameExtend;
				HoTTbinReader.recordSetChannel = RecordSet.createRecordSet(recordSetName, device, 6, true, true, true);
				channel.put(recordSetName, HoTTbinReader.recordSetChannel);
				HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.CHANNEL.value(), HoTTbinReader.recordSetChannel);
				tmpRecordSet = channel.get(recordSetName);
				tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT
						+ Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
				tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
				if (HoTTbinReader.application.getMenuToolBar() != null) {
					channel.applyTemplate(recordSetName, false);
				}
				// recordSetChannel initialized and ready to add data
			}

			// read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTbinReader.log.isLoggable(Level.FINE) && i % 10 == 0) {
					HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
					HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if (!HoTTAdapter.isFilterTextModus || (HoTTbinReader.buf[6] & 0x01) == 0) { // switch
					// into
					// text
					// modus
					if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { // buf 3, 4, tx,rx
						if (HoTTbinReader2.logger.isLoggable(Level.FINE)) HoTTbinReader2.logger.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", HoTTbinReader.buf[7], HoTTbinReader.buf[33]));

						HoTTAdapter.reverseChannelPackageLossCounter.add(1);
						HoTTbinReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
						// create and fill sensor specific data record sets
						if (HoTTbinReader.log.isLoggable(Level.FINER)) HoTTbinReader.log.logp(Level.FINER, HoTTbinReader.$CLASS_NAME, $METHOD_NAME,
								StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

						// fill receiver data
						if (HoTTbinReader.buf[33] == 0 && (HoTTbinReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader.buf, 40) >= 0) {
							parseAddReceiver(HoTTbinReader.buf);
						}
						if (HoTTAdapter.isChannelsChannelEnabled) {
							parseAddChannel(HoTTbinReader.buf);
						}
						if (HoTTbinReader.isReceiverOnly && !HoTTAdapter.isChannelsChannelEnabled) {
							for (int j = 0; j < 9; j++) {
								data_in.read(HoTTbinReader.buf);
								HoTTbinReader.timeStep_ms += 10;
							}
						}
						// fill data block 0 receiver voltage an temperature
						if (HoTTbinReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader.buf, 0) != 0) {
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf0, 0, HoTTbinReader.buf0.length);
						}

						HoTTbinReader.timeStep_ms += 10;// add default time step
						// from device of 10
						// msec

						// log.log(Level.OFF, "sensor type ID = " +
						// StringHelper.byte2Hex2CharString(new byte[] {(byte)
						// (HoTTbinReader.buf[7] & 0xFF)}, 1));
						switch ((byte) (HoTTbinReader.buf[7] & 0xFF)) {
						case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
						case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
							if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.VARIO.ordinal()]) {
								// check if recordSetVario initialized,
								// transmitter and receiver data always present,
								// but not in the same data rate and signals
								if (HoTTbinReader.recordSetVario == null) {
									channel = HoTTbinReader.channels.get(2);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
											: date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK
											+ HoTTAdapter.Sensor.VARIO.value() + recordSetNameExtend;
									HoTTbinReader.recordSetVario = RecordSet.createRecordSet(recordSetName, device, 2, true, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetVario);
									HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.VARIO.value(), HoTTbinReader.recordSetVario);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT
											+ Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								// recordSetVario initialized and ready to add
								// data
								// fill data block 1 to 2
								if (HoTTbinReader.buf[33] == 1) {
									HoTTbinReader.buf1 = new byte[30];
									System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf1, 0, HoTTbinReader.buf1.length);
								}
								if (HoTTbinReader.buf[33] == 2) {
									HoTTbinReader.buf2 = new byte[30];
									System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf2, 0, HoTTbinReader.buf2.length);
								}
								if (HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null) {
									version = parseAddVario(version, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2);
									HoTTbinReader.buf1 = HoTTbinReader.buf2 = null;
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_GPS_115200:
						case HoTTAdapter.SENSOR_TYPE_GPS_19200:
							if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GPS.ordinal()]) {
								// check if recordSetReceiver initialized,
								// transmitter and receiver data always present,
								// but not in the same data rate as signals
								if (HoTTbinReader.recordSetGPS == null) {
									channel = HoTTbinReader.channels.get(3);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
											: date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK+ HoTTAdapter.Sensor.GPS.value() + recordSetNameExtend;
									HoTTbinReader.recordSetGPS = RecordSet.createRecordSet(recordSetName, device, 3, true, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetGPS);
									HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GPS.value(), HoTTbinReader.recordSetGPS);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT+ Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								// recordSetGPS initialized and ready to add
								// data
								// fill data block 1 to 3
								if (HoTTbinReader.buf1 == null && HoTTbinReader.buf[33] == 1) {
									HoTTbinReader.buf1 = new byte[30];
									System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf1, 0, HoTTbinReader.buf1.length);
								}
								if (HoTTbinReader.buf2 == null && HoTTbinReader.buf[33] == 2) {
									HoTTbinReader.buf2 = new byte[30];
									System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf2, 0, HoTTbinReader.buf2.length);
								}
								if (HoTTbinReader.buf3 == null && HoTTbinReader.buf[33] == 3) {
									HoTTbinReader.buf3 = new byte[30];
									System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf3, 0, HoTTbinReader.buf3.length);
								}
								if (HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null && HoTTbinReader.buf3 != null) {
									parseAddGPS(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3);
									HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = null;
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
						case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
							if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()]) {
								// check if recordSetGeneral initialized,
								// transmitter and receiver data always present,
								// but not in the same data rate and signals
								if (HoTTbinReader.recordSetGeneral == null) {
									channel = HoTTbinReader.channels.get(4);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
											: date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GAM.value() + recordSetNameExtend;
									HoTTbinReader.recordSetGeneral = RecordSet.createRecordSet(recordSetName, device, 4, true, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetGeneral);
									HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GAM.value(), HoTTbinReader.recordSetGeneral);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								// recordSetGeneral initialized and ready to add
								// data
								// fill data block 1 to 4
								if (HoTTbinReader.buf1 == null && HoTTbinReader.buf[33] == 1) {
									HoTTbinReader.buf1 = new byte[30];
									System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf1, 0, HoTTbinReader.buf1.length);
								}
								if (HoTTbinReader.buf2 == null && HoTTbinReader.buf[33] == 2) {
									HoTTbinReader.buf2 = new byte[30];
									System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf2, 0, HoTTbinReader.buf2.length);
								}
								if (HoTTbinReader.buf3 == null && HoTTbinReader.buf[33] == 3) {
									HoTTbinReader.buf3 = new byte[30];
									System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf3, 0, HoTTbinReader.buf3.length);
								}
								if (HoTTbinReader.buf4 == null && HoTTbinReader.buf[33] == 4) {
									HoTTbinReader.buf4 = new byte[30];
									System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf4, 0, HoTTbinReader.buf4.length);
								}
								if (HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null && HoTTbinReader.buf3 != null && HoTTbinReader.buf4 != null) {
									parseAddGAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
									HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
							if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.EAM.ordinal()]) {
								// check if recordSetGeneral initialized,
								// transmitter and receiver data always present,
								// but not in the same data rate and signals
								if (HoTTbinReader.recordSetElectric == null) {
									channel = HoTTbinReader.channels.get(5);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
											: date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.EAM.value() + recordSetNameExtend;
									HoTTbinReader.recordSetElectric = RecordSet.createRecordSet(recordSetName, device, 5, true, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetElectric);
									HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.EAM.value(), HoTTbinReader.recordSetElectric);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								// recordSetElectric initialized and ready to
								// add data
								// fill data block 1 to 4
								if (HoTTbinReader.buf1 == null && HoTTbinReader.buf[33] == 1) {
									HoTTbinReader.buf1 = new byte[30];
									System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf1, 0, HoTTbinReader.buf1.length);
								}
								if (HoTTbinReader.buf2 == null && HoTTbinReader.buf[33] == 2) {
									HoTTbinReader.buf2 = new byte[30];
									System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf2, 0, HoTTbinReader.buf2.length);
								}
								if (HoTTbinReader.buf3 == null && HoTTbinReader.buf[33] == 3) {
									HoTTbinReader.buf3 = new byte[30];
									System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf3, 0, HoTTbinReader.buf3.length);
								}
								if (HoTTbinReader.buf4 == null && HoTTbinReader.buf[33] == 4) {
									HoTTbinReader.buf4 = new byte[30];
									System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf4, 0, HoTTbinReader.buf4.length);
								}
								if (HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null && HoTTbinReader.buf3 != null && HoTTbinReader.buf4 != null) {
									parseAddEAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
									HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
							if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()]) {
								// check if recordSetMotorDriver initialized,
								// transmitter and receiver data always present,
								// but not in the same data rate and signals
								if (HoTTbinReader.recordSetSpeedControl == null) {
									channel = HoTTbinReader.channels.get(7);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
											: date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ESC.value() + recordSetNameExtend;
									HoTTbinReader.recordSetSpeedControl = RecordSet.createRecordSet(recordSetName, device, 7, true, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetSpeedControl);
									HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.ESC.value(), HoTTbinReader.recordSetSpeedControl);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								// recordSetMotorDriver initialized and ready to
								// add data
								// fill data block 1 to 4
								if (HoTTbinReader.buf1 == null && HoTTbinReader.buf[33] == 1) {
									HoTTbinReader.buf1 = new byte[30];
									System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf1, 0, HoTTbinReader.buf1.length);
								}
								if (HoTTbinReader.buf2 == null && HoTTbinReader.buf[33] == 2) {
									HoTTbinReader.buf2 = new byte[30];
									System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf2, 0, HoTTbinReader.buf2.length);
								}
								if (HoTTbinReader.buf3 == null && HoTTbinReader.buf[33] == 2) {
									HoTTbinReader.buf3 = new byte[30];
									System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf3, 0, HoTTbinReader.buf3.length);
								}
								if (HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null && HoTTbinReader.buf3 != null) {
									parseAddESC(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3);
									HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
								}
							}
							break;
						}

						if (menuToolBar != null && i % progressIndicator == 0)
							HoTTbinReader.application.setProgress((int) (i * 100 / numberDatablocks), sThreadId);

						if (HoTTbinReader.isJustParsed && HoTTbinReader.countLostPackages > 0) {
							HoTTbinReader.lostPackages.add(HoTTbinReader.countLostPackages);
							HoTTbinReader.countLostPackages = 0;
							HoTTbinReader.isJustParsed = false;
						}
					}
					else { // skip empty block, but add time step
						if (HoTTbinReader2.logger.isLoggable(Level.FINE)) HoTTbinReader2.logger.log(Level.FINE, "-->> Found tx=rx=0 dBm");

						HoTTAdapter.reverseChannelPackageLossCounter.add(0);
						HoTTbinReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;

						++countPackageLoss; // add up lost packages in telemetry
						// data
						++HoTTbinReader.countLostPackages;
						// HoTTbinReader.pointsReceiver[0] = (int)
						// (countPackageLoss*100.0 /
						// ((HoTTbinReader2.timeStep_ms+10) / 10.0)*1000.0);

						if (HoTTAdapter.isChannelsChannelEnabled) {
							parseAddChannel(HoTTbinReader.buf);
						}

						HoTTbinReader.timeStep_ms += 10;
						// reset buffer to avoid mixing data >> 20 Jul 14, not
						// any longer required due to protocol change requesting
						// next sensor data block
						// HoTTbinReader.buf1 = HoTTbinReader.buf2 =
						// HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
					}
				}
				else if (!HoTTbinReader.isTextModusSignaled) {
					HoTTbinReader.isTextModusSignaled = true;
					HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			String packageLossPercentage = HoTTbinReader.recordSetReceiver.getRecordDataSize(true) > 0
					? String.format("%.1f", (countPackageLoss / HoTTbinReader.recordSetReceiver.getTime_ms(HoTTbinReader.recordSetReceiver.getRecordDataSize(true) - 1) * 1000))
					: "100";
			HoTTbinReader.recordSetReceiver.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] { countPackageLoss, packageLossPercentage, HoTTbinReader.lostPackages.getStatistics() })
					+ HoTTbinReader.sensorSignature);
			HoTTbinReader.log.logp(Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReader.log.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " //$NON-NLS-1$
					+ StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$

			if (menuToolBar != null) {
				HoTTbinReader.application.setProgress(100, sThreadId);
				if (!isInitialSwitched) {
					HoTTbinReader.channels.switchChannel(channel.getName());
					channel.switchRecordSet(recordSetName);
				}

				for (RecordSet recordSet : HoTTAdapter.recordSets.values()) {
					device.makeInActiveDisplayable(recordSet);

					// write filename after import to record description
					recordSet.descriptionAppendFilename(file.getName());
				}

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				HoTTbinReader.application.setProgress(100, sThreadId);
			}
		}
		finally {
			data_in.close();
			data_in = null;
		}
	}

	/**
	 * compose the record set extend to give capability to identify source of
	 * this record set
	 * 
	 * @param file
	 * @return
	 */
	protected static String getRecordSetExtend(File file) {
		return getRecordSetExtend(file.getName());
	}

	/**
	 * compose the record set extend to give capability to identify source of
	 * this record set
	 * 
	 * @param fileName
	 * @return
	 */
	protected static String getRecordSetExtend(String fileName) {
		String recordSetNameExtend = GDE.STRING_EMPTY;
		if (fileName.contains(GDE.STRING_UNDER_BAR)) {
			try {
				Integer.parseInt(fileName.substring(0, fileName.lastIndexOf(GDE.STRING_UNDER_BAR)));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.lastIndexOf(GDE.STRING_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (fileName.substring(0, fileName.lastIndexOf(GDE.STRING_UNDER_BAR)).length() <= 8)
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.lastIndexOf(GDE.STRING_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		else {
			try {
				Integer.parseInt(fileName.substring(0, 4));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, 4) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (fileName.substring(0, fileName.length()).length() <= 8 + 4) recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.length() - 4) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		return recordSetNameExtend;
	}

	/**
	 * read log data according to version 0
	 * 
	 * @param file
	 * @param data_in
	 * @throws IOException
	 * @throws DataInconsitsentException
	 */
	static void readMultiple(File file) throws IOException, DataInconsitsentException {
		final String $METHOD_NAME = "readMultiple";
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		HoTTAdapter device = (HoTTAdapter) HoTTbinReader.application.getActiveDevice();
		int recordSetNumber = HoTTbinReader.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		boolean isInitialSwitched = false;
		HoTTbinReader.recordSetReceiver = null; // 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=UminRx
		HoTTbinReader.recordSetGeneral = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		HoTTbinReader.recordSetElectric = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2, 27=Revolution
		HoTTbinReader.recordSetVario = null; // 0=RXSQ, 1=Height, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
		HoTTbinReader.recordSetGPS = null; // 0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		HoTTbinReader.recordSetChannel = null; // 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16
		HoTTbinReader.recordSetSpeedControl = null; // 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperaure
		HoTTbinReader.pointsReceiver = new int[9];
		HoTTbinReader.pointsGeneral = new int[21];
		HoTTbinReader.pointsElectric = new int[28];
		HoTTbinReader.pointsVario = new int[7];
		HoTTbinReader.pointsVario[2] = 100000;
		HoTTbinReader.pointsGPS = new int[12];
		HoTTbinReader.pointsChannel = new int[23];
		HoTTbinReader.pointsSpeedControl = new int[13];
		HoTTbinReader.timeStep_ms = 0;
		HoTTbinReader.dataBlockSize = 64;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		HoTTbinReader.buf0 = new byte[30];
		HoTTbinReader.buf1 = new byte[30];
		HoTTbinReader.buf2 = new byte[30];
		HoTTbinReader.buf3 = new byte[30];
		HoTTbinReader.buf4 = new byte[30];
		byte actualSensor = -1, lastSensor = -1;
		int logCountVario = 0, logCountGPS = 0, logCountGeneral = 0, logCountElectric = 0, logCountSpeedControl = 0;
		HoTTAdapter.reverseChannelPackageLossCounter.clear();
		HoTTbinReader.lostPackages.clear();
		HoTTbinReader.countLostPackages = 0;
		HoTTbinReader.isJustParsed = false;
		HoTTbinReader.isTextModusSignaled = false;
		int countPackageLoss = 0;
		long numberDatablocks = fileSize / HoTTbinReader.dataBlockSize;
		long startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(file, numberDatablocks);
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		MenuToolBar menuToolBar = HoTTbinReader.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		if (menuToolBar != null)
			HoTTbinReader.application.setProgress(0, sThreadId);

		try {
			HoTTAdapter.recordSets.clear();
			// receiver data are always contained
			// check if recordSetReceiver initialized, transmitter and receiver
			// data always present, but not in the same data rate and signals
			channel = HoTTbinReader.channels.get(1);
			channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
					? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
			recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.RECEIVER.value() + recordSetNameExtend;
			HoTTbinReader.recordSetReceiver = RecordSet.createRecordSet(recordSetName, device, 1, true, true, true);
			channel.put(recordSetName, HoTTbinReader.recordSetReceiver);
			HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.RECEIVER.value(), HoTTbinReader.recordSetReceiver);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (HoTTbinReader.application.getMenuToolBar() != null) {
				channel.applyTemplate(recordSetName, false);
			}
			// recordSetReceiver initialized and ready to add data

			// channel data are always contained
			if (HoTTAdapter.isChannelsChannelEnabled) {
				// check if recordSetChannel initialized, transmitter and
				// receiver data always present, but not in the same data rate
				// and signals
				channel = HoTTbinReader.channels.get(6);
				channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
						? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
				recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.CHANNEL.value() + recordSetNameExtend;
				HoTTbinReader.recordSetChannel = RecordSet.createRecordSet(recordSetName, device, 6, true, true, true);
				channel.put(recordSetName, HoTTbinReader.recordSetChannel);
				HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.CHANNEL.value(), HoTTbinReader.recordSetChannel);
				tmpRecordSet = channel.get(recordSetName);
				tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
				tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
				if (HoTTbinReader.application.getMenuToolBar() != null) {
					channel.applyTemplate(recordSetName, false);
				}
				// recordSetChannel initialized and ready to add data
			}

			// read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTbinReader.log.isLoggable(Level.FINEST) && i % 10 == 0) {
					HoTTbinReader.log.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
					HoTTbinReader.log.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if (!HoTTAdapter.isFilterTextModus || (HoTTbinReader.buf[6] & 0x01) == 0) { // switch into text modus
					if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { // buf 3, 4, tx,rx
						if (HoTTbinReader2.logger.isLoggable(Level.FINE)) HoTTbinReader2.logger.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", HoTTbinReader.buf[7], HoTTbinReader.buf[33]));

						HoTTAdapter.reverseChannelPackageLossCounter.add(1);
						HoTTbinReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
						// create and fill sensor specific data record sets
						if (HoTTbinReader.log.isLoggable(Level.FINEST)) HoTTbinReader.log.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME,
								StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

						// fill receiver data
						if (HoTTbinReader.buf[33] == 0 && (HoTTbinReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader.buf, 40) >= 0) {
							parseAddReceiver(HoTTbinReader.buf);
						}
						if (HoTTAdapter.isChannelsChannelEnabled) {
							parseAddChannel(HoTTbinReader.buf);
						}

						HoTTbinReader.timeStep_ms += 10;// add default time step
						// from log record of 10
						// msec

						// detect sensor switch
						if (actualSensor == -1)
							lastSensor = actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);
						else
							actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);

						if (actualSensor != lastSensor) {
							// write data just after sensor switch
							if (logCountVario >= 3 || logCountGPS >= 4 || logCountGeneral >= 5 || logCountElectric >= 5 || logCountSpeedControl >= 5) {
								switch (lastSensor) {
								case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
								case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
									if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.VARIO.ordinal()]) {
										// check if recordSetVario initialized,
										// transmitter and receiver data always
										// present, but not in the same data
										// rate and signals
										if (HoTTbinReader.recordSetVario == null) {
											channel = HoTTbinReader.channels.get(2);
											channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
													? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
											recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.VARIO.value() + recordSetNameExtend;
											HoTTbinReader.recordSetVario = RecordSet.createRecordSet(recordSetName, device, 2, true, true, true);
											channel.put(recordSetName, HoTTbinReader.recordSetVario);
											HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.VARIO.value(), HoTTbinReader.recordSetVario);
											tmpRecordSet = channel.get(recordSetName);
											tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
											tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
											if (HoTTbinReader.application.getMenuToolBar() != null) {
												channel.applyTemplate(recordSetName, false);
											}
										}
										// recordSetVario initialized and ready
										// to add data
										parseAddVario(1, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_GPS_115200:
								case HoTTAdapter.SENSOR_TYPE_GPS_19200:
									if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GPS.ordinal()]) {
										// check if recordSetReceiver
										// initialized, transmitter and receiver
										// data always present, but not in the
										// same data rate ans signals
										if (HoTTbinReader.recordSetGPS == null) {
											channel = HoTTbinReader.channels.get(3);
											channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
													? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
											recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GPS.value() + recordSetNameExtend;
											HoTTbinReader.recordSetGPS = RecordSet.createRecordSet(recordSetName, device, 3, true, true, true);
											channel.put(recordSetName, HoTTbinReader.recordSetGPS);
											HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GPS.value(), HoTTbinReader.recordSetGPS);
											tmpRecordSet = channel.get(recordSetName);
											tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
											tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
											if (HoTTbinReader.application.getMenuToolBar() != null) {
												channel.applyTemplate(recordSetName, false);
											}
										}
										// recordSetGPS initialized and ready to
										// add data
										parseAddGPS(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
								case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
									if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()]) {
										// check if recordSetGeneral
										// initialized, transmitter and receiver
										// data always present, but not in the
										// same data rate and signals
										if (HoTTbinReader.recordSetGeneral == null) {
											channel = HoTTbinReader.channels.get(4);
											channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
													? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
											recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GAM.value() + recordSetNameExtend;
											HoTTbinReader.recordSetGeneral = RecordSet.createRecordSet(recordSetName, device, 4, true, true, true);
											channel.put(recordSetName, HoTTbinReader.recordSetGeneral);
											HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GAM.value(), HoTTbinReader.recordSetGeneral);
											tmpRecordSet = channel.get(recordSetName);
											tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
											tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
											if (HoTTbinReader.application.getMenuToolBar() != null) {
												channel.applyTemplate(recordSetName, false);
											}
										}
										// recordSetGeneral initialized and
										// ready to add data
										parseAddGAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
									if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.EAM.ordinal()]) {
										// check if recordSetGeneral
										// initialized, transmitter and receiver
										// data always present, but not in the
										// same data rate and signals
										if (HoTTbinReader.recordSetElectric == null) {
											channel = HoTTbinReader.channels.get(5);
											channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
													? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
											recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.EAM.value() + recordSetNameExtend;
											HoTTbinReader.recordSetElectric = RecordSet.createRecordSet(recordSetName, device, 5, true, true, true);
											channel.put(recordSetName, HoTTbinReader.recordSetElectric);
											HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.EAM.value(), HoTTbinReader.recordSetElectric);
											tmpRecordSet = channel.get(recordSetName);
											tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
											tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
											if (HoTTbinReader.application.getMenuToolBar() != null) {
												channel.applyTemplate(recordSetName, false);
											}
										}
										// recordSetElectric initialized and
										// ready to add data
										parseAddEAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
								case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
									if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()]) {
										// check if recordSetGeneral
										// initialized, transmitter and receiver
										// data always present, but not in the
										// same data rate and signals
										if (HoTTbinReader.recordSetSpeedControl == null) {
											channel = HoTTbinReader.channels.get(7);
											channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
													? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
											recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ESC.value() + recordSetNameExtend;
											HoTTbinReader.recordSetSpeedControl = RecordSet.createRecordSet(recordSetName, device, 7, true, true, true);
											channel.put(recordSetName, HoTTbinReader.recordSetSpeedControl);
											HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.ESC.value(), HoTTbinReader.recordSetSpeedControl);
											tmpRecordSet = channel.get(recordSetName);
											tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
											tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
											if (HoTTbinReader.application.getMenuToolBar() != null) {
												channel.applyTemplate(recordSetName, false);
											}
										}
										// recordSetElectric initialized and
										// ready to add data
										parseAddESC(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3);
									}
									break;
								}
							}

							if (HoTTbinReader.log.isLoggable(Level.FINE)) HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "logCountVario = " + logCountVario + " logCountGPS = "
									+ logCountGPS + " logCountGeneral = " + logCountGeneral + " logCountElectric = " + logCountElectric + " logCountMotorDriver = " + logCountSpeedControl);
							lastSensor = actualSensor;
							logCountVario = logCountGPS = logCountGeneral = logCountElectric = logCountSpeedControl = 0;
						}

						switch (lastSensor) {
						case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
						case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
							++logCountVario;
							break;
						case HoTTAdapter.SENSOR_TYPE_GPS_115200:
						case HoTTAdapter.SENSOR_TYPE_GPS_19200:
							++logCountGPS;
							break;
						case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
						case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
							++logCountGeneral;
							break;
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
							++logCountElectric;
							break;
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
							++logCountSpeedControl;
							break;
						}

						// fill data block 0 to 4
						if (HoTTbinReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader.buf, 0) != 0) {
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf0, 0, HoTTbinReader.buf0.length);
						}
						else if (HoTTbinReader.buf[33] == 1) {
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf1, 0, HoTTbinReader.buf1.length);
						}
						else if (HoTTbinReader.buf[33] == 2) {
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf2, 0, HoTTbinReader.buf2.length);
						}
						else if (HoTTbinReader.buf[33] == 3) {
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf3, 0, HoTTbinReader.buf3.length);
						}
						else if (HoTTbinReader.buf[33] == 4) {
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf4, 0, HoTTbinReader.buf4.length);
						}

						// if (HoTTbinReader.blockSequenceCheck.size() > 1) {
						// if(HoTTbinReader.blockSequenceCheck.get(0) -
						// HoTTbinReader.blockSequenceCheck.get(1) > 1 &&
						// HoTTbinReader.blockSequenceCheck.get(0) -
						// HoTTbinReader.blockSequenceCheck.get(1) < 4)
						// ++HoTTbinReader.oldProtocolCount;
						// HoTTbinReader.blockSequenceCheck.remove(0);
						// }
						// HoTTbinReader.blockSequenceCheck.add(HoTTbinReader.buf[33]);

						if (menuToolBar != null && i % progressIndicator == 0)
							HoTTbinReader.application.setProgress((int) (i * 100 / numberDatablocks), sThreadId);

						if (HoTTbinReader.isJustParsed && HoTTbinReader.countLostPackages > 0) {
							HoTTbinReader.lostPackages.add(HoTTbinReader.countLostPackages);
							HoTTbinReader.countLostPackages = 0;
							HoTTbinReader.isJustParsed = false;
						}
					}
					else { // tx,rx == 0
						if (HoTTbinReader2.logger.isLoggable(Level.FINE)) HoTTbinReader2.logger.log(Level.FINE, "-->> Found tx=rx=0 dBm");

						HoTTAdapter.reverseChannelPackageLossCounter.add(0);
						HoTTbinReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;

						++countPackageLoss; // add up lost packages in telemetry
						// data
						++HoTTbinReader.countLostPackages;
						// HoTTbinReader.pointsReceiver[0] = (int)
						// (countPackageLoss*100.0 /
						// ((HoTTbinReader2.timeStep_ms+10) / 10.0)*1000.0);

						if (HoTTAdapter.isChannelsChannelEnabled) {
							parseAddChannel(HoTTbinReader.buf);
						}

						HoTTbinReader.timeStep_ms += 10;
						// reset buffer to avoid mixing data
						// logCountVario = logCountGPS = logCountGeneral =
						// logCountElectric = logCountSpeedControl = 0;
					}
				}
				else if (!HoTTbinReader.isTextModusSignaled) {
					HoTTbinReader.isTextModusSignaled = true;
					HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			// if (HoTTbinReader.oldProtocolCount > 2) {
			// application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2405,
			// new Object[] { HoTTbinReader.oldProtocolCount }));
			// }
			String packageLossPercentage = HoTTbinReader.recordSetReceiver.getRecordDataSize(true) > 0
					? String.format("%.1f", (countPackageLoss / HoTTbinReader.recordSetReceiver.getTime_ms(HoTTbinReader.recordSetReceiver.getRecordDataSize(true) - 1) * 1000)) : "100";
			HoTTbinReader.recordSetReceiver.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] { countPackageLoss, packageLossPercentage, HoTTbinReader.lostPackages.getStatistics() })
					+ HoTTbinReader.sensorSignature);
			HoTTbinReader.log.logp(Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReader.log.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " //$NON-NLS-1$
					+ StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$

			if (menuToolBar != null) {
				HoTTbinReader.application.setProgress(99, sThreadId);
				if (!isInitialSwitched) {
					HoTTbinReader.channels.switchChannel(channel.getName());
					channel.switchRecordSet(recordSetName);
				}

				for (RecordSet recordSet : HoTTAdapter.recordSets.values()) {
					device.makeInActiveDisplayable(recordSet);
					device.updateVisibilityStatus(recordSet, true);

					// write filename after import to record description
					recordSet.descriptionAppendFilename(file.getName());
				}

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				HoTTbinReader.application.setProgress(100, sThreadId);
			}
		}
		finally {
			data_in.close();
			data_in = null;
		}
	}

	// ET splitting the parseAddxyz methods was preferred against copying the
	// code. so this specific code is not scattered over two classes.
	/**
	 * parse the buffered data from buffer and add points to record set
	 * 
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddReceiver(byte[] _buf) throws DataInconsitsentException {
		HoTTbinReader.parse4Receiver(_buf);
		HoTTbinReader.recordSetReceiver.addPoints(HoTTbinReader.pointsReceiver, HoTTbinReader.timeStep_ms);
	}

	/**
	 * parse the buffered data from buffer
	 * 
	 * @param _buf
	 */
	protected static void parse4Receiver(byte[] _buf) {
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx,
		// 6=VoltageRx, 7=TemperatureRx, 8=Umin Rx
		HoTTbinReader.tmpVoltageRx = (_buf[35] & 0xFF);
		HoTTbinReader.tmpTemperatureRx = (_buf[36] & 0xFF);
		HoTTbinReader.pointsReceiver[1] = (_buf[38] & 0xFF) * 1000;
		HoTTbinReader.pointsReceiver[3] = DataParser.parse2Short(_buf, 40) * 1000;
		if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpVoltageRx > -1 && HoTTbinReader.tmpVoltageRx < 100 && HoTTbinReader.tmpTemperatureRx < 120) {
			HoTTbinReader.pointsReceiver[2] = (convertRxDbm2Strength(_buf[4] & 0xFF)) * 1000;
			HoTTbinReader.pointsReceiver[4] = (_buf[3] & 0xFF) * -1000;
			HoTTbinReader.pointsReceiver[5] = (_buf[4] & 0xFF) * -1000;
			HoTTbinReader.pointsReceiver[6] = (_buf[35] & 0xFF) * 1000;
			HoTTbinReader.pointsReceiver[7] = (_buf[36] & 0xFF) * 1000;
			HoTTbinReader.pointsReceiver[8] = (_buf[39] & 0xFF) * 1000;
		}

		// printByteValues(_timeStep_ms, _buf);
		// if (HoTTbinReader.pointsReceiver[3] > 2000000)
		// System.out.println();
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 * 
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddChannel(byte[] _buf) throws DataInconsitsentException {
		HoTTbinReader.parse4Channel(_buf);
		HoTTbinReader.recordSetChannel.addPoints(HoTTbinReader.pointsChannel, HoTTbinReader.timeStep_ms);
	}

	/**
	 * parse the buffered data from buffer
	 * 
	 * @param _buf
	 */
	protected static void parse4Channel(byte[] _buf) {
		// 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16
		// 19=PowerOff, 20=BattLow, 21=Reset, 22=reserved
		HoTTbinReader.pointsChannel[0] = (_buf[1] & 0xFF) * 1000;
		HoTTbinReader.pointsChannel[1] = (_buf[3] & 0xFF) * -1000;
		HoTTbinReader.pointsChannel[2] = (_buf[4] & 0xFF) * -1000;
		HoTTbinReader.pointsChannel[3] = (DataParser.parse2UnsignedShort(_buf, 8) / 2) * 1000;
		HoTTbinReader.pointsChannel[4] = (DataParser.parse2UnsignedShort(_buf, 10) / 2) * 1000;
		HoTTbinReader.pointsChannel[5] = (DataParser.parse2UnsignedShort(_buf, 12) / 2) * 1000;
		HoTTbinReader.pointsChannel[6] = (DataParser.parse2UnsignedShort(_buf, 14) / 2) * 1000;
		HoTTbinReader.pointsChannel[7] = (DataParser.parse2UnsignedShort(_buf, 16) / 2) * 1000;
		HoTTbinReader.pointsChannel[8] = (DataParser.parse2UnsignedShort(_buf, 18) / 2) * 1000;
		HoTTbinReader.pointsChannel[9] = (DataParser.parse2UnsignedShort(_buf, 20) / 2) * 1000;
		HoTTbinReader.pointsChannel[10] = (DataParser.parse2UnsignedShort(_buf, 22) / 2) * 1000;
		HoTTbinReader.pointsChannel[19] = (_buf[50] & 0x01) * 100000;
		HoTTbinReader.pointsChannel[20] = (_buf[50] & 0x02) * 50000;
		HoTTbinReader.pointsChannel[21] = (_buf[50] & 0x04) * 25000;
		HoTTbinReader.pointsChannel[22] = (_buf[50] & 0x00) * 1000; // reserved for future use
		if (_buf[5] == 0x00) { // channel 9-12
			HoTTbinReader.pointsChannel[11] = (DataParser.parse2UnsignedShort(_buf, 24) / 2) * 1000;
			HoTTbinReader.pointsChannel[12] = (DataParser.parse2UnsignedShort(_buf, 26) / 2) * 1000;
			HoTTbinReader.pointsChannel[13] = (DataParser.parse2UnsignedShort(_buf, 28) / 2) * 1000;
			HoTTbinReader.pointsChannel[14] = (DataParser.parse2UnsignedShort(_buf, 30) / 2) * 1000;
			if (HoTTbinReader.pointsChannel[15] == 0) {
				HoTTbinReader.pointsChannel[15] = 1500 * 1000;
				HoTTbinReader.pointsChannel[16] = 1500 * 1000;
				HoTTbinReader.pointsChannel[17] = 1500 * 1000;
				HoTTbinReader.pointsChannel[18] = 1500 * 1000;
			}
		}
		else { // channel 13-16
			HoTTbinReader.pointsChannel[15] = (DataParser.parse2UnsignedShort(_buf, 24) / 2) * 1000;
			HoTTbinReader.pointsChannel[16] = (DataParser.parse2UnsignedShort(_buf, 26) / 2) * 1000;
			HoTTbinReader.pointsChannel[17] = (DataParser.parse2UnsignedShort(_buf, 28) / 2) * 1000;
			HoTTbinReader.pointsChannel[18] = (DataParser.parse2UnsignedShort(_buf, 30) / 2) * 1000;
			if (HoTTbinReader.pointsChannel[11] == 0) {
				HoTTbinReader.pointsChannel[11] = 1500 * 1000;
				HoTTbinReader.pointsChannel[12] = 1500 * 1000;
				HoTTbinReader.pointsChannel[13] = 1500 * 1000;
				HoTTbinReader.pointsChannel[14] = 1500 * 1000;
			}
		}

		// printByteValues(_timeStep_ms, _buf);
	}

	/**
	 * parse the buffered data from buffer 0 to 2 and add points to record set
	 * 
	 * @param sdLogVersion
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @throws DataInconsitsentException
	 */
	protected static int parseAddVario(int sdLogVersion, byte[] _buf0, byte[] _buf1, byte[] _buf2) throws DataInconsitsentException {
		HoTTbinReader.parse4Vario(sdLogVersion, _buf0, _buf1, _buf2);
		if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && HoTTbinReader.tmpClimb10 < 40000 && HoTTbinReader.tmpClimb10 > 20000)) {
			HoTTbinReader.recordSetVario.addPoints(HoTTbinReader.pointsVario, HoTTbinReader.timeStep_ms);
		}
		return sdLogVersion;
	}

	/**
	 * parse the buffered data from buffer 0 to 2
	 * 
	 * @param sdLogVersion
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 */
	protected static int parse4Vario(int sdLogVersion, byte[] _buf0, byte[] _buf1, byte[] _buf2) {
		// 0=RXSQ, 1=Height, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
		HoTTbinReader.pointsVario[0] = (_buf0[4] & 0xFF) * 1000;
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf1, 2);
		HoTTbinReader.tmpClimb10 = DataParser.parse2UnsignedShort(_buf2, 2);
		if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && HoTTbinReader.tmpClimb10 < 40000 && HoTTbinReader.tmpClimb10 > 20000)) {
			HoTTbinReader.pointsVario[1] = HoTTbinReader.tmpHeight * 1000;
			// pointsVarioMax = DataParser.parse2Short(buf1, 4) * 1000;
			// pointsVarioMin = DataParser.parse2Short(buf1, 6) * 1000;
			HoTTbinReader.pointsVario[2] = DataParser.parse2UnsignedShort(_buf1, 8) * 1000;
			HoTTbinReader.pointsVario[3] = DataParser.parse2UnsignedShort(_buf2, 0) * 1000;
			HoTTbinReader.pointsVario[4] = HoTTbinReader.tmpClimb10 * 1000;
			HoTTbinReader.pointsVario[5] = (_buf0[1] & 0xFF) * 1000;
			HoTTbinReader.pointsVario[6] = (_buf0[2] & 0xFF) * 1000;
		}
		// break;
		// }
		HoTTbinReader.isJustParsed = true;
		return sdLogVersion;
	}

	/**
	 * detect the SD Log Version V3 or V4
	 * 
	 * @param _buf1
	 * @param _buf2
	 * @return
	 */
	protected static int getSdLogVerion(byte[] _buf1, byte[] _buf2) {
		// printByteValues(1, _buf1);
		// printByteValues(2, _buf2);
		if (HoTTbinReader.log.isLoggable(Level.FINER))
			HoTTbinReader.log.log(Level.FINER, "version = " + (((DataParser.parse2UnsignedShort(_buf1[3], _buf2[4]) - 30000) < -100) ? 4 : 3));
		return ((DataParser.parse2UnsignedShort(_buf1[3], _buf2[4]) - 30000) < -100) ? 4 : 3;
	}

	/**
	 * parse the buffered data from buffer 0 to 3 and add points to record set
	 * 
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddGPS(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3) throws DataInconsitsentException {
		HoTTbinReader.parse4GPS(_buf0, _buf1, _buf2, _buf3);
		if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpClimb1 > 10000 && HoTTbinReader.tmpClimb3 > 30 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 4500)) {
			HoTTbinReader.recordSetGPS.addPoints(HoTTbinReader.pointsGPS, HoTTbinReader.timeStep_ms);
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 3
	 * 
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 */
	protected static void parse4GPS(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3) {
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf2, 8);
		HoTTbinReader.tmpClimb1 = DataParser.parse2UnsignedShort(_buf3, 0);
		HoTTbinReader.tmpClimb3 = (_buf3[2] & 0xFF);
		HoTTbinReader.tmpVelocity = DataParser.parse2Short(_buf1, 4) * 1000;
		HoTTbinReader.pointsGPS[0] = (_buf0[4] & 0xFF) * 1000;
		if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpClimb1 > 10000 && HoTTbinReader.tmpClimb3 > 30 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 4500)) {
			// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripLength, 10=VoltageRx, 11=TemperatureRx
			HoTTbinReader.pointsGPS[6] = HoTTAdapter.isFilterEnabled && HoTTbinReader.tmpVelocity > 2000000 ? HoTTbinReader.pointsGPS[6] : HoTTbinReader.tmpVelocity;

			HoTTbinReader.tmpLatitude = DataParser.parse2Short(_buf1, 7) * 10000 + DataParser.parse2Short(_buf1[9], _buf2[0]);
			if (!HoTTAdapter.isTolerateSignChangeLatitude) HoTTbinReader.tmpLatitude = _buf1[6] == 1 ? -1 * HoTTbinReader.tmpLatitude : HoTTbinReader.tmpLatitude;
			HoTTbinReader.tmpLatitudeDelta = Math.abs(HoTTbinReader.tmpLatitude - HoTTbinReader.pointsGPS[1]);
			HoTTbinReader.tmpLatitudeDelta = HoTTbinReader.tmpLatitudeDelta > 400000 ? HoTTbinReader.tmpLatitudeDelta - 400000 : HoTTbinReader.tmpLatitudeDelta;
			HoTTbinReader.latitudeTolerance = HoTTbinReader.pointsGPS[6] / 1000.0 * (HoTTbinReader.timeStep_ms - HoTTbinReader.lastLatitudeTimeStep) / HoTTAdapter.latitudeToleranceFactor;
			HoTTbinReader.latitudeTolerance = HoTTbinReader.latitudeTolerance > 0 ? HoTTbinReader.latitudeTolerance : 5;

			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.pointsGPS[1] == 0 || HoTTbinReader.tmpLatitudeDelta <= HoTTbinReader.latitudeTolerance) {
				HoTTbinReader.lastLatitudeTimeStep = HoTTbinReader.timeStep_ms;
				HoTTbinReader.pointsGPS[1] = HoTTbinReader.tmpLatitude;
			}
			else {
				if (HoTTbinReader.log.isLoggable(Level.FINE)) HoTTbinReader.log.log(Level.FINE,
						StringHelper.getFormatedTime("HH:mm:ss:SSS", HoTTbinReader.timeStep_ms - GDE.ONE_HOUR_MS) + " Lat " + HoTTbinReader.tmpLatitude + " - " + HoTTbinReader.tmpLatitudeDelta);
			}

			HoTTbinReader.tmpLongitude = DataParser.parse2Short(_buf2, 2) * 10000 + DataParser.parse2Short(_buf2, 4);
			if (!HoTTAdapter.isTolerateSignChangeLongitude) HoTTbinReader.tmpLongitude = _buf2[1] == 1 ? -1 * HoTTbinReader.tmpLongitude : HoTTbinReader.tmpLongitude;
			HoTTbinReader.tmpLongitudeDelta = Math.abs(HoTTbinReader.tmpLongitude - HoTTbinReader.pointsGPS[2]);
			HoTTbinReader.tmpLongitudeDelta = HoTTbinReader.tmpLongitudeDelta > 400000 ? HoTTbinReader.tmpLongitudeDelta - 400000 : HoTTbinReader.tmpLongitudeDelta;
			HoTTbinReader.longitudeTolerance = HoTTbinReader.pointsGPS[6] / 1000.0 * (HoTTbinReader.timeStep_ms - HoTTbinReader.lastLongitudeTimeStep) / HoTTAdapter.longitudeToleranceFactor;
			HoTTbinReader.longitudeTolerance = HoTTbinReader.longitudeTolerance > 0 ? HoTTbinReader.longitudeTolerance : 5;

			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.pointsGPS[2] == 0 || HoTTbinReader.tmpLongitudeDelta <= HoTTbinReader.longitudeTolerance) {
				HoTTbinReader.lastLongitudeTimeStep = HoTTbinReader.timeStep_ms;
				HoTTbinReader.pointsGPS[2] = HoTTbinReader.tmpLongitude;
			}
			else {
				if (HoTTbinReader.log.isLoggable(Level.FINE)) HoTTbinReader.log.log(Level.FINE, StringHelper.getFormatedTime("HH:mm:ss:SSS", HoTTbinReader.timeStep_ms - GDE.ONE_HOUR_MS) + " Long "
						+ HoTTbinReader.tmpLongitude + " - " + HoTTbinReader.tmpLongitudeDelta + " - " + HoTTbinReader.longitudeTolerance);
			}

			HoTTbinReader.pointsGPS[3] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsGPS[4] = HoTTbinReader.tmpClimb1 * 1000;
			HoTTbinReader.pointsGPS[5] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsGPS[7] = DataParser.parse2Short(_buf2, 6) * 1000;
			HoTTbinReader.pointsGPS[8] = (_buf1[3] & 0xFF) * 1000;
			HoTTbinReader.pointsGPS[9] = 0;
			HoTTbinReader.pointsGPS[10] = (_buf0[1] & 0xFF) * 1000;
			HoTTbinReader.pointsGPS[11] = (_buf0[2] & 0xFF) * 1000;

			HoTTbinReader.isJustParsed = true;
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * 
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddGAM(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) throws DataInconsitsentException {
		HoTTbinReader.parse4GAM(_buf0, _buf1, _buf2, _buf3, _buf4);
		if (!HoTTAdapter.isFilterEnabled
				|| (HoTTbinReader.tmpClimb3 > 30 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600 && Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
			HoTTbinReader.recordSetGeneral.addPoints(HoTTbinReader.pointsGeneral, HoTTbinReader.timeStep_ms);
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4
	 * 
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 */
	protected static void parse4GAM(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) {
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf3, 0);
		HoTTbinReader.tmpClimb3 = (_buf3[4] & 0xFF);
		HoTTbinReader.tmpVoltage1 = DataParser.parse2Short(_buf1[9], _buf2[0]);
		HoTTbinReader.tmpVoltage2 = DataParser.parse2Short(_buf2, 1);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
		// 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance,
		// 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6,
		// 12=Revolution, 13=Height, 14=Climb, 15=Climb3, 16=FuelLevel,
		// 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		HoTTbinReader.pointsGeneral[0] = (_buf0[4] & 0xFF) * 1000;
		if (!HoTTAdapter.isFilterEnabled
				|| (HoTTbinReader.tmpClimb3 > 30 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600 && Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			HoTTbinReader.pointsGeneral[1] = DataParser.parse2Short(_buf3, 7) * 1000;
			HoTTbinReader.pointsGeneral[2] = DataParser.parse2Short(_buf3, 5) * 1000;
			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.recordSetGeneral.getRecordDataSize(true) == 0
					|| Math.abs(HoTTbinReader.tmpCapacity) <= (HoTTbinReader.pointsGeneral[3] / 1000 + HoTTbinReader.pointsGeneral[1] / 1000 * HoTTbinReader.pointsGeneral[2] / 1000 / 2500 + 2)) {
				HoTTbinReader.pointsGeneral[3] = HoTTbinReader.tmpCapacity * 1000;
			}
			else {
				HoTTbinReader.log.log(Level.WARNING, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - " + (HoTTbinReader.pointsGeneral[3] / 1000)
						+ " + " + (HoTTbinReader.pointsGeneral[1] / 1000 * HoTTbinReader.pointsGeneral[2] / 1000 / 2500 + 2));
			}
			HoTTbinReader.pointsGeneral[4] = Double.valueOf(HoTTbinReader.pointsGeneral[1] / 1000.0 * HoTTbinReader.pointsGeneral[2]).intValue();
			HoTTbinReader.pointsGeneral[5] = 0;
			for (int j = 0; j < 6; j++) {
				HoTTbinReader.pointsGeneral[j + 6] = (_buf1[3 + j] & 0xFF) * 1000;
				if (HoTTbinReader.pointsGeneral[j + 6] > 0) {
					maxVotage = HoTTbinReader.pointsGeneral[j + 6] > maxVotage ? HoTTbinReader.pointsGeneral[j + 6] : maxVotage;
					minVotage = HoTTbinReader.pointsGeneral[j + 6] < minVotage ? HoTTbinReader.pointsGeneral[j + 6] : minVotage;
				}
			}
			HoTTbinReader.pointsGeneral[5] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
			HoTTbinReader.pointsGeneral[12] = DataParser.parse2Short(_buf2, 8) * 1000;
			HoTTbinReader.pointsGeneral[13] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsGeneral[14] = DataParser.parse2UnsignedShort(_buf3, 2) * 1000;
			HoTTbinReader.pointsGeneral[15] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsGeneral[16] = DataParser.parse2Short(_buf2, 6) * 1000;
			HoTTbinReader.pointsGeneral[17] = HoTTbinReader.tmpVoltage1 * 1000;
			HoTTbinReader.pointsGeneral[18] = HoTTbinReader.tmpVoltage2 * 1000;
			HoTTbinReader.pointsGeneral[19] = (_buf2[3] & 0xFF) * 1000;
			HoTTbinReader.pointsGeneral[20] = (_buf2[4] & 0xFF) * 1000;

			HoTTbinReader.isJustParsed = true;
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * 
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddEAM(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) throws DataInconsitsentException {
		HoTTbinReader.parse4EAM(_buf0, _buf1, _buf2, _buf3, _buf4);
		if (!HoTTAdapter.isFilterEnabled
				|| (HoTTbinReader.tmpClimb3 > 30 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600 && Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
			HoTTbinReader.recordSetElectric.addPoints(HoTTbinReader.pointsElectric, HoTTbinReader.timeStep_ms);
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4
	 * 
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 */
	protected static void parse4EAM(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) {
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf3, 3);
		HoTTbinReader.tmpClimb3 = (_buf4[3] & 0xFF);
		HoTTbinReader.tmpVoltage1 = DataParser.parse2Short(_buf2, 7);
		HoTTbinReader.tmpVoltage2 = DataParser.parse2Short(_buf2[9], _buf3[0]);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
		// 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance,
		// 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height,
		// 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1,
		// 26=Temperature 2 27=revolution
		HoTTbinReader.pointsElectric[0] = (_buf0[4] & 0xFF) * 1000;
		if (!HoTTAdapter.isFilterEnabled
				|| (HoTTbinReader.tmpClimb3 > 30 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600 && Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			HoTTbinReader.pointsElectric[1] = DataParser.parse2Short(_buf3, 7) * 1000;
			HoTTbinReader.pointsElectric[2] = DataParser.parse2Short(_buf3, 5) * 1000;
			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.recordSetElectric.getRecordDataSize(true) == 0
					|| Math.abs(HoTTbinReader.tmpCapacity) <= (HoTTbinReader.pointsElectric[3] / 1000 + HoTTbinReader.pointsElectric[1] / 1000 * HoTTbinReader.pointsElectric[2] / 1000 / 2500 + 2)) {
				HoTTbinReader.pointsElectric[3] = HoTTbinReader.tmpCapacity * 1000;
			}
			else {
				HoTTbinReader.log.log(Level.WARNING, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - " + (HoTTbinReader.pointsElectric[3] / 1000)
						+ " + " + (HoTTbinReader.pointsElectric[1] / 1000 * HoTTbinReader.pointsElectric[2] / 1000 / 2500 + 2));
			}
			HoTTbinReader.pointsElectric[4] = Double.valueOf(HoTTbinReader.pointsElectric[1] / 1000.0 * HoTTbinReader.pointsElectric[2]).intValue(); // power
			// U*I
			// [W];
			HoTTbinReader.pointsElectric[5] = 0; // 5=Balance
			for (int j = 0; j < 7; j++) {
				HoTTbinReader.pointsElectric[j + 6] = (_buf1[3 + j] & 0xFF) * 1000;
				if (HoTTbinReader.pointsElectric[j + 6] > 0) {
					maxVotage = HoTTbinReader.pointsElectric[j + 6] > maxVotage ? HoTTbinReader.pointsElectric[j + 6] : maxVotage;
					minVotage = HoTTbinReader.pointsElectric[j + 6] < minVotage ? HoTTbinReader.pointsElectric[j + 6] : minVotage;
				}
			}
			for (int j = 0; j < 7; j++) {
				HoTTbinReader.pointsElectric[j + 13] = (_buf2[j] & 0xFF) * 1000;
				if (HoTTbinReader.pointsElectric[j + 13] > 0) {
					maxVotage = HoTTbinReader.pointsElectric[j + 13] > maxVotage ? HoTTbinReader.pointsElectric[j + 13] : maxVotage;
					minVotage = HoTTbinReader.pointsElectric[j + 13] < minVotage ? HoTTbinReader.pointsElectric[j + 13] : minVotage;
				}
			}
			// calculate balance on the fly
			HoTTbinReader.pointsElectric[5] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
			HoTTbinReader.pointsElectric[20] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsElectric[21] = DataParser.parse2UnsignedShort(_buf4, 1) * 1000;
			HoTTbinReader.pointsElectric[22] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsElectric[23] = HoTTbinReader.tmpVoltage1 * 1000;
			HoTTbinReader.pointsElectric[24] = HoTTbinReader.tmpVoltage2 * 1000;
			HoTTbinReader.pointsElectric[25] = (_buf3[1] & 0xFF) * 1000;
			HoTTbinReader.pointsElectric[26] = (_buf3[2] & 0xFF) * 1000;
			HoTTbinReader.pointsElectric[27] = DataParser.parse2Short(_buf4, 4) * 1000; // revolution

			HoTTbinReader.isJustParsed = true;
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * 
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddESC(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3) throws DataInconsitsentException {
		HoTTbinReader.parse4ESC(_buf0, _buf1, _buf2, _buf3);
		if (!HoTTAdapter.isFilterEnabled
				|| HoTTbinReader.tmpVoltage > 0 && HoTTbinReader.tmpVoltage < 1000 && HoTTbinReader.tmpCurrent < 4000 && HoTTbinReader.tmpCurrent > -10 && HoTTbinReader.tmpRevolution > -1
						&& HoTTbinReader.tmpRevolution < 20000 && !(HoTTbinReader.pointsSpeedControl[6] != 0 && HoTTbinReader.pointsSpeedControl[6] / 1000 - HoTTbinReader.tmpTemperatureFet > 20)) {
			HoTTbinReader.recordSetSpeedControl.addPoints(HoTTbinReader.pointsSpeedControl, HoTTbinReader.timeStep_ms);
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * 
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 */
	protected static void parse4ESC(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3) {
		// 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution,
		// 6=Temperature1, 7=Temperature2
		// 8=Voltage_min, 9=Current_max, 10=Revolution_max, 11=Temperature1_max,
		// 12=Temperature2_max
		HoTTbinReader.tmpVoltage = DataParser.parse2Short(_buf1, 3);
		HoTTbinReader.tmpCurrent = DataParser.parse2Short(_buf2, 1);
		HoTTbinReader.pointsSpeedControl[0] = (_buf0[4] & 0xFF) * 1000;
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf1, 7);
		HoTTbinReader.tmpRevolution = DataParser.parse2Short(_buf2, 5);
		HoTTbinReader.tmpTemperatureFet = _buf1[9] - 20;
		if (!HoTTAdapter.isFilterEnabled
				|| HoTTbinReader.tmpVoltage > 0 && HoTTbinReader.tmpVoltage < 1000 && HoTTbinReader.tmpCurrent < 4000 && HoTTbinReader.tmpCurrent > -10 && HoTTbinReader.tmpRevolution > -1
						&& HoTTbinReader.tmpRevolution < 20000 && !(HoTTbinReader.pointsSpeedControl[6] != 0 && HoTTbinReader.pointsSpeedControl[6] / 1000 - HoTTbinReader.tmpTemperatureFet > 20)) {
			HoTTbinReader.pointsSpeedControl[1] = HoTTbinReader.tmpVoltage * 1000;
			HoTTbinReader.pointsSpeedControl[2] = HoTTbinReader.tmpCurrent * 1000;
			HoTTbinReader.pointsSpeedControl[4] = Double.valueOf(HoTTbinReader.pointsSpeedControl[1] / 1000.0 * HoTTbinReader.pointsSpeedControl[2]).intValue();
			if (!HoTTAdapter.isFilterEnabled
					|| (HoTTbinReader.tmpCapacity != 0 && Math.abs(HoTTbinReader.tmpCapacity) <= (HoTTbinReader.pointsSpeedControl[3] / 1000 + HoTTbinReader.tmpVoltage * HoTTbinReader.tmpCurrent / 2500 + 2))) {
				HoTTbinReader.pointsSpeedControl[3] = HoTTbinReader.tmpCapacity * 1000;
			}
			else {
				if (HoTTbinReader.tmpCapacity != 0) HoTTbinReader.log.log(Level.WARNING, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - "
						+ (HoTTbinReader.pointsSpeedControl[3] / 1000) + " + " + (HoTTbinReader.tmpVoltage * HoTTbinReader.tmpCurrent / 2500 + 2));
			}
			HoTTbinReader.pointsSpeedControl[5] = HoTTbinReader.tmpRevolution * 1000;
			HoTTbinReader.pointsSpeedControl[6] = HoTTbinReader.tmpTemperatureFet * 1000;

			HoTTbinReader.pointsSpeedControl[7] = (_buf2[9] - 20) * 1000;
			// 8=Voltage_min, 9=Current_max, 10=Revolution_max,
			// 11=Temperature1_max, 12=Temperature2_max
			HoTTbinReader.pointsSpeedControl[8] = DataParser.parse2Short(_buf1, 5) * 1000;
			HoTTbinReader.pointsSpeedControl[9] = DataParser.parse2Short(_buf2, 3) * 1000;
			HoTTbinReader.pointsSpeedControl[10] = DataParser.parse2Short(_buf2, 7) * 1000;
			HoTTbinReader.pointsSpeedControl[11] = (_buf2[0] - 20) * 1000;
			HoTTbinReader.pointsSpeedControl[12] = (_buf3[0] - 20) * 1000;
		}
		HoTTbinReader.isJustParsed = true;
	}

	static void printByteValues(long millisec, byte[] buffer) {
		StringBuilder sb = new StringBuilder().append(StringHelper.getFormatedTime("mm:ss:SSS", millisec)).append(" : ");
		for (int i = 0; buffer != null && i < buffer.length; i++) {
			sb.append("(").append(i).append(")").append(buffer[i]).append(GDE.STRING_BLANK);
		}
		HoTTbinReader.log.log(Level.FINE, sb.toString());
	}

	static void printShortValues(long millisec, byte[] buffer) {
		StringBuilder sb = new StringBuilder().append(StringHelper.getFormatedTime("mm:ss:SSS", millisec)).append(" : ");
		for (int i = 0; buffer != null && i < buffer.length - 1; i++) {
			sb.append("(").append(i).append(")").append(DataParser.parse2Short(buffer, i)).append(GDE.STRING_BLANK);
		}
		HoTTbinReader.log.log(Level.FINE, sb.toString());
	}

	/**
	 * @param file
	 * @param numberDatablocks
	 * @return get a rectified start timestamp if the files's last modified timestamp does not correspond with the filename 
	 */
	protected static long getStartTimeStamp(File file, long numberDatablocks) {
		final long startTimeStamp;
		String name = file.getName();
		log.log(Level.FINE, "name=", name); //$NON-NLS-1$
		Pattern hoTTNamePattern = Pattern.compile("\\d{4}\\_\\d{4}-\\d{1,2}-\\d{1,2}"); //$NON-NLS-1$
		Matcher hoTTMatcher = hoTTNamePattern.matcher(name);
		if (hoTTMatcher.find()) {
			String logName = hoTTMatcher.group();

			String[] strValueLogName = logName.split(GDE.STRING_UNDER_BAR);
			int logCounter = Integer.parseInt(strValueLogName[0]);

			String[] strValueDate = strValueLogName[1].split(GDE.STRING_DASH);
			int year = Integer.parseInt(strValueDate[0]);
			int month = Integer.parseInt(strValueDate[1]);
			int day = Integer.parseInt(strValueDate[2]);

			// check if the zoned date is equal
			GregorianCalendar lastModifiedDate = new GregorianCalendar();
			lastModifiedDate.setTimeInMillis(file.lastModified() - numberDatablocks * 10);
			if (year == lastModifiedDate.get(Calendar.YEAR) && month == lastModifiedDate.get(Calendar.MONTH)+1 && day == lastModifiedDate.get(Calendar.DAY_OF_MONTH)) {
				startTimeStamp = lastModifiedDate.getTimeInMillis();
				log.log(Level.FINE,	"lastModified=" + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", file.lastModified()) + " " + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", lastModifiedDate.getTimeInMillis()));  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
			else {
				// spread the logCounter into steps of 10 minutes 
				GregorianCalendar fileNameDate = new GregorianCalendar(year, month - 1, day, 0, 0, 0);
				fileNameDate.add(GregorianCalendar.MINUTE, 10 * (logCounter % (24 * 6)));
				startTimeStamp = fileNameDate.getTimeInMillis();
				log.log(Level.FINE, "fileNameDate=" + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", fileNameDate.getTimeInMillis())); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		else {
			startTimeStamp = file.lastModified();
		}
		return startTimeStamp;
	}
}
