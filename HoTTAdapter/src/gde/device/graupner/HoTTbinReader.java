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
    
    Copyright (c) 2011,2012,2013 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;
import gde.utils.FileUtils;
import gde.utils.StringHelper;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * Class to read Graupner HoTT binary data as saved on SD-Cards 
 * @author Winfried Brügmann
 */
public class HoTTbinReader {
	final static String				$CLASS_NAME						= HoTTbinReader.class.getName();
	final static Logger				log										= Logger.getLogger(HoTTbinReader.$CLASS_NAME);

	final static DataExplorer	application						= DataExplorer.getInstance();
	final static Channels			channels							= Channels.getInstance();

	static int								dataBlockSize					= 64;
	static byte[]							buf;
	static byte[]							buf0, buf1, buf2, buf3, buf4;
	static long								timeStep_ms;
	static int[]							pointsReceiver, pointsGeneral, pointsElectric, pointsVario, pointsGPS, pointsChannel, pointsSpeedControl;
	static RecordSet					recordSetReceiver, recordSetGeneral, recordSetElectric, recordSetVario, recordSetGPS, recordSetChannel, recordSetSpeedControl;
	static int								tmpVoltageRx					= 0;
	static int								tmpTemperatureRx			= 0;
	static int								tmpHeight							= 0;
	static int								tmpTemperatureFet			= 0;
	static int								tmpTemperatureExt			= 0;
	static int								tmpVoltage						= 0;
	static int								tmpCurrent						= 0;
	static int								tmpRevolution					= 0;
	static int								tmpClimb3							= 0;
	static int								tmpClimb10						= 0;
	static int								tmpVoltage1						= 0;
	static int								tmpVoltage2						= 0;
	static int								tmpCapacity						= 0;
	static int								tmpLatitude						= 0;
	static int								tmpLatitudeDelta			= 0;
	static double							latitudeTolerance			= 1;
	static long								lastLatitudeTimeStep	= 0;
	static int								tmpLongitude					= 0;
	static int								tmpLongitudeDelta			= 0;
	static double							longitudeTolerance		= 1;
	static long								lastLongitudeTimeStep	= 0;
	
	/**
	 * get data file info data
	 * @param buffer byte array containing the first 64 byte to analyze the header
	 * @return hash map containing header data as string accessible by public header keys
	 * @throws IOException 
	 */
	public static HashMap<String, String> getFileInfo(File file) throws IOException {
		final String $METHOD_NAME = "getFileInfo";
		DataInputStream data_in = null;
		byte[] buffer = new byte[64];
		HashMap<String, String> fileInfo;
		int sensorCount = 0;
		long numberLogs = (file.length() / 64);

		try {
			if (numberLogs > 120) {
				for (int i = 0; i < HoTTAdapter.isSensorType.length; i++) {
					HoTTAdapter.isSensorType[i] = false;
				}
				FileInputStream file_input = new FileInputStream(file);
				data_in = new DataInputStream(file_input);
				fileInfo = new HashMap<String, String>();
				if (HoTTbinReader.log.isLoggable(java.util.logging.Level.FINER))
					HoTTbinReader.log.logp(java.util.logging.Level.FINER, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(buffer.length));
				long position = (file.length()-120*64)/2;
				if (position <= 0) 
					sensorCount = 1;
				
				if (sensorCount == 0) {
					data_in.skip(position);
					for (int i = 0; i < 120; i++) {
						data_in.read(buffer);
						if (HoTTbinReader.log.isLoggable(java.util.logging.Level.FINER))
							HoTTbinReader.log.logp(java.util.logging.Level.FINER, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(buffer, buffer.length));

						switch (buffer[7]) {
						case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
							HoTTAdapter.isSensorType[1] = true;
							break;
						case HoTTAdapter.SENSOR_TYPE_GPS_19200:
							HoTTAdapter.isSensorType[2] = true;
							break;
						case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
							HoTTAdapter.isSensorType[3] = true;
							break;
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
							HoTTAdapter.isSensorType[4] = true;
							break;
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
							HoTTAdapter.isSensorType[5] = true;
							break;
						}
					}
					for (boolean element : HoTTAdapter.isSensorType) {
						if (element == true) ++sensorCount;
					}
				}
				fileInfo.put(HoTTAdapter.SENSOR_COUNT, GDE.STRING_EMPTY + sensorCount);
				fileInfo.put(HoTTAdapter.LOG_COUNT, GDE.STRING_EMPTY + (file.length() / 64));

				if (HoTTbinReader.log.isLoggable(Level.FINE)) for (Entry<String, String> entry : fileInfo.entrySet()) {
					HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, entry.getKey() + " = " + entry.getValue());
					HoTTbinReader.log.log(Level.FINE, file.getName() + " - " + "sensor count = " + sensorCount);
				}
			}
			else {
				throw new IOException("file size to small");
			}
		}
		finally {
			if (data_in != null) data_in.close();
		}
		return fileInfo;
	}

	//0=Rx dbm to Strength lookup table
	static int[]	lookup	= new int[] { 95, 95, 95, 95, 95, 95, 95, 95, 95, 95, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 85, 85, 85, 85, 85, 80, 75, 70, 65, 60, 55, 50, 45, 40, 35, 30, 30, 25, 25, 20,
			20, 20, 15, 15, 10, 10, 5, 5, 5, 5, 5, 5, 0, 0, 0, 0, 0, 0, 0 };

	/**
	 * convert from RF_RXSQ to strength using lookup table
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
	 * @param filePath
	 * @throws Exception 
	 */
	public static synchronized void read(String filePath) throws Exception {
		HashMap<String, String> header = null;
		File file = new File(filePath);

		header = getFileInfo(file);

		if (Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) <= 1)
			readSingle(file);
		else
			readMultiple(file);
	}

	/**
	* read log data according to version 0
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
		HoTTbinReader.recordSetReceiver = null; //0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 
		HoTTbinReader.recordSetGeneral = null; //0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		HoTTbinReader.recordSetElectric = null; //0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2 
		HoTTbinReader.recordSetVario = null; //0=RXSQ, 1=Height, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
		HoTTbinReader.recordSetGPS = null; //0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		HoTTbinReader.recordSetChannel = null; //0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16
		HoTTbinReader.recordSetSpeedControl = null; //0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature
		HoTTbinReader.pointsReceiver = new int[8];
		HoTTbinReader.pointsGeneral = new int[21];
		HoTTbinReader.pointsElectric = new int[27];
		HoTTbinReader.pointsVario = new int[7];
		HoTTbinReader.pointsVario[2] = 100000;
		HoTTbinReader.pointsGPS = new int[12];
		HoTTbinReader.pointsChannel = new int[23];
		HoTTbinReader.pointsSpeedControl = new int[7];
		HoTTbinReader.timeStep_ms = 0;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		HoTTbinReader.buf0 = new byte[30];
		HoTTbinReader.buf1 = null;
		HoTTbinReader.buf2 = null;
		HoTTbinReader.buf3 = null;
		HoTTbinReader.buf4 = null;
		int version = -1;
		int countPackageLoss = 0;
		long numberDatablocks = fileSize / HoTTbinReader.dataBlockSize;
		long startTimeStamp_ms = file.lastModified() - (numberDatablocks * 10);
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		MenuToolBar menuToolBar = HoTTbinReader.application.getMenuToolBar();
		if (menuToolBar != null) HoTTbinReader.application.setProgress(0, sThreadId);

		try {
			HoTTAdapter.recordSets.clear();
			//receiver data are always contained
			//check if recordSetReceiver initialized, transmitter and receiver data always present, but not in the same data rate and signals
			channel = HoTTbinReader.channels.get(1);
			channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
			recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.RECEIVER.value() + recordSetNameExtend;
			HoTTbinReader.recordSetReceiver = RecordSet.createRecordSet(recordSetName, device, 1, true, true);
			channel.put(recordSetName, HoTTbinReader.recordSetReceiver);
			HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.RECEIVER.value(), HoTTbinReader.recordSetReceiver);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (HoTTbinReader.application.getMenuToolBar() != null) {
				channel.applyTemplate(recordSetName, false);
			}
			//recordSetReceiver initialized and ready to add data

			if (HoTTAdapter.isChannelsChannelEnabled) {
				//channel data are always contained
				//check if recordSetChannel initialized, transmitter and receiver data always present, but not in the same data rate and signals
				channel = HoTTbinReader.channels.get(6);
				channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
				recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.CHANNEL.value() + recordSetNameExtend;
				HoTTbinReader.recordSetChannel = RecordSet.createRecordSet(recordSetName, device, 6, true, true);
				channel.put(recordSetName, HoTTbinReader.recordSetChannel);
				HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.CHANNEL.value(), HoTTbinReader.recordSetChannel);
				tmpRecordSet = channel.get(recordSetName);
				tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
				tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
				if (HoTTbinReader.application.getMenuToolBar() != null) {
					channel.applyTemplate(recordSetName, false);
				}
				//recordSetChannel initialized and ready to add data
			}

			//read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTbinReader.log.isLoggable(java.util.logging.Level.FINE) && i % 10 == 0) {
					HoTTbinReader.log.logp(java.util.logging.Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
					HoTTbinReader.log.logp(java.util.logging.Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { //buf 0 to 4, tx,rx
					HoTTAdapter.reverseChannelPackageLossCounter.add(1);
					HoTTbinReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
					//create and fill sensor specific data record sets 
					if (HoTTbinReader.log.isLoggable(java.util.logging.Level.FINER))
						HoTTbinReader.log.logp(java.util.logging.Level.FINER, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1)
								+ GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));
					
					//fill receiver data
					if (HoTTbinReader.buf[33] == 0 && (HoTTbinReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader.buf, 40) >= 0) {
						parseAddReceiver(HoTTbinReader.buf);
					}
					if (HoTTAdapter.isChannelsChannelEnabled) {
						parseAddChannel(HoTTbinReader.buf);
					}
					//fill data block 0 receiver voltage an temperature
					if (HoTTbinReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader.buf, 0) != 0) {
						System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf0, 0, HoTTbinReader.buf0.length);
					}

					HoTTbinReader.timeStep_ms += 10;// add default time step from device of 10 msec
					
					//log.log(Level.OFF, "sensor type ID = " + StringHelper.byte2Hex2CharString(new byte[] {(byte) (HoTTbinReader.buf[7] & 0xFF)}, 1));
					switch ((byte) (HoTTbinReader.buf[7] & 0xFF)) {
					case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
					case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
						//check if recordSetVario initialized, transmitter and receiver data always present, but not in the same data rate and signals
						if (HoTTbinReader.recordSetVario == null) {
							channel = HoTTbinReader.channels.get(2);
							channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
							recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.VARIO.value() + recordSetNameExtend;
							HoTTbinReader.recordSetVario = RecordSet.createRecordSet(recordSetName, device, 2, true, true);
							channel.put(recordSetName, HoTTbinReader.recordSetVario);
							HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.VARIO.value(), HoTTbinReader.recordSetVario);
							tmpRecordSet = channel.get(recordSetName);
							tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
							tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
							if (HoTTbinReader.application.getMenuToolBar() != null) {
								channel.applyTemplate(recordSetName, false);
							}
						}
						//recordSetVario initialized and ready to add data
						//fill data block 1 to 2
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
						break;

					case HoTTAdapter.SENSOR_TYPE_GPS_115200:
					case HoTTAdapter.SENSOR_TYPE_GPS_19200:
						//check if recordSetReceiver initialized, transmitter and receiver data always present, but not in the same data rate as signals
						if (HoTTbinReader.recordSetGPS == null) {
							channel = HoTTbinReader.channels.get(3);
							channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
							recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GPS.value() + recordSetNameExtend;
							HoTTbinReader.recordSetGPS = RecordSet.createRecordSet(recordSetName, device, 3, true, true);
							channel.put(recordSetName, HoTTbinReader.recordSetGPS);
							HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GPS.value(), HoTTbinReader.recordSetGPS);
							tmpRecordSet = channel.get(recordSetName);
							tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
							tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
							if (HoTTbinReader.application.getMenuToolBar() != null) {
								channel.applyTemplate(recordSetName, false);
							}
						}
						//recordSetGPS initialized and ready to add data
						//fill data block 1 to 3
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
						break;

					case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
					case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
						//check if recordSetGeneral initialized, transmitter and receiver data always present, but not in the same data rate and signals
						if (HoTTbinReader.recordSetGeneral == null) {
							channel = HoTTbinReader.channels.get(4);
							channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
							recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GENRAL.value() + recordSetNameExtend;
							HoTTbinReader.recordSetGeneral = RecordSet.createRecordSet(recordSetName, device, 4, true, true);
							channel.put(recordSetName, HoTTbinReader.recordSetGeneral);
							HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GENRAL.value(), HoTTbinReader.recordSetGeneral);
							tmpRecordSet = channel.get(recordSetName);
							tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
							tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
							if (HoTTbinReader.application.getMenuToolBar() != null) {
								channel.applyTemplate(recordSetName, false);
							}
						}
						//recordSetGeneral initialized and ready to add data
						//fill data block 1 to 4
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
							parseAddGeneral(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
							HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
						//check if recordSetGeneral initialized, transmitter and receiver data always present, but not in the same data rate and signals
						if (HoTTbinReader.recordSetElectric == null) {
							channel = HoTTbinReader.channels.get(5);
							channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
							recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ELECTRIC.value() + recordSetNameExtend;
							HoTTbinReader.recordSetElectric = RecordSet.createRecordSet(recordSetName, device, 5, true, true);
							channel.put(recordSetName, HoTTbinReader.recordSetElectric);
							HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.ELECTRIC.value(), HoTTbinReader.recordSetElectric);
							tmpRecordSet = channel.get(recordSetName);
							tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
							tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
							if (HoTTbinReader.application.getMenuToolBar() != null) {
								channel.applyTemplate(recordSetName, false);
							}
						}
						//recordSetElectric initialized and ready to add data
						//fill data block 1 to 4
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
							parseAddElectric(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
							HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
						//check if recordSetMotorDriver initialized, transmitter and receiver data always present, but not in the same data rate and signals
						if (HoTTbinReader.recordSetSpeedControl == null) {
							channel = HoTTbinReader.channels.get(7);
							channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
							recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.SPEED_CONTROL.value() + recordSetNameExtend;
							HoTTbinReader.recordSetSpeedControl = RecordSet.createRecordSet(recordSetName, device, 7, true, true);
							channel.put(recordSetName, HoTTbinReader.recordSetSpeedControl);
							HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.SPEED_CONTROL.value(), HoTTbinReader.recordSetSpeedControl);
							tmpRecordSet = channel.get(recordSetName);
							tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
							tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
							if (HoTTbinReader.application.getMenuToolBar() != null) {
								channel.applyTemplate(recordSetName, false);
							}
						}
						//recordSetMotorDriver initialized and ready to add data
						//fill data block 1 to 4
						if (HoTTbinReader.buf1 == null && HoTTbinReader.buf[33] == 1) {
							HoTTbinReader.buf1 = new byte[30];
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf1, 0, HoTTbinReader.buf1.length);
						}
						if (HoTTbinReader.buf2 == null && HoTTbinReader.buf[33] == 2) {
							HoTTbinReader.buf2 = new byte[30];
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf2, 0, HoTTbinReader.buf2.length);
						}
						
						if (HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null) {
							parseAddSpeedControl(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2);
							HoTTbinReader.buf1 = HoTTbinReader.buf2 = null;
						}
						break;
					}

					if (menuToolBar != null && i % 100 == 0) HoTTbinReader.application.setProgress((int) (i * 100 / numberDatablocks), sThreadId);
				}
				else { //skip empty block, but add time step
					HoTTAdapter.reverseChannelPackageLossCounter.add(0);
					HoTTbinReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;

					++countPackageLoss;	// add up lost packages in telemetry data 
					//HoTTbinReader.pointsReceiver[0] = (int) (countPackageLoss*100.0 / ((HoTTbinReader2.timeStep_ms+10) / 10.0)*1000.0); 

					if (HoTTAdapter.isChannelsChannelEnabled) {
						parseAddChannel(HoTTbinReader.buf);
					}
					
					HoTTbinReader.timeStep_ms += 10;
					//reset buffer to avoid mixing data
					HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
				}
			}
			String packageLossPercentage = HoTTbinReader.recordSetReceiver.getRecordDataSize(true) > 0 ? String.format("%.1f", (countPackageLoss / HoTTbinReader.recordSetReceiver.getTime_ms(HoTTbinReader.recordSetReceiver.getRecordDataSize(true)-1) * 1000)) : "0";
			HoTTbinReader.recordSetReceiver.setRecordSetDescription(tmpRecordSet.getRecordSetDescription() + Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] {countPackageLoss, packageLossPercentage}));
			HoTTbinReader.log.logp(java.util.logging.Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReader.log.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (menuToolBar != null) {
				if (!isInitialSwitched) {
					HoTTbinReader.channels.switchChannel(channel.getName());
					channel.switchRecordSet(recordSetName);
				}
	
				for (RecordSet recordSet : HoTTAdapter.recordSets.values()) {
					device.makeInActiveDisplayable(recordSet);
					device.updateVisibilityStatus(recordSet, true);
					
					//write filename after import to record description
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
	 * compose the record set extend to give capability to identify source of this record set
	 * @param file
	 * @return
	 */
	protected static String getRecordSetExtend(File file) {
		String recordSetNameExtend = GDE.STRING_EMPTY;
		if (file.getName().contains(GDE.STRING_UNDER_BAR)) {
			try {
				Integer.parseInt(file.getName().substring(0, file.getName().lastIndexOf(GDE.STRING_UNDER_BAR)));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, file.getName().lastIndexOf(GDE.STRING_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (file.getName().substring(0, file.getName().lastIndexOf(GDE.STRING_UNDER_BAR)).length() <= 8)
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, file.getName().lastIndexOf(GDE.STRING_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		else {
			try {
				Integer.parseInt(file.getName().substring(0, 4));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, 4) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (file.getName().substring(0, file.getName().length()).length() <= 8 + 4)
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, file.getName().length() - 4) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		return recordSetNameExtend;
	}

	/**
	* read log data according to version 0
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
		HoTTbinReader.recordSetReceiver = null; //0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 
		HoTTbinReader.recordSetGeneral = null; //0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		HoTTbinReader.recordSetElectric = null; //0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2 
		HoTTbinReader.recordSetVario = null; //0=RXSQ, 1=Height, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
		HoTTbinReader.recordSetGPS = null; //0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		HoTTbinReader.recordSetChannel = null; //0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16
		HoTTbinReader.recordSetSpeedControl = null; //0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperaure
		HoTTbinReader.pointsReceiver = new int[8];
		HoTTbinReader.pointsGeneral = new int[21];
		HoTTbinReader.pointsElectric = new int[27];
		HoTTbinReader.pointsVario = new int[7];
		HoTTbinReader.pointsVario[2] = 100000;
		HoTTbinReader.pointsGPS = new int[12];
		HoTTbinReader.pointsChannel = new int[23];
		HoTTbinReader.pointsSpeedControl = new int[7];
		HoTTbinReader.timeStep_ms = 0;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		HoTTbinReader.buf0 = new byte[30];
		HoTTbinReader.buf1 = new byte[30];
		HoTTbinReader.buf2 = new byte[30];
		HoTTbinReader.buf3 = new byte[30];
		HoTTbinReader.buf4 = new byte[30];
		byte actualSensor = -1, lastSensor = -1;
		int logCountVario = 0, logCountGPS = 0, logCountGeneral = 0, logCountElectric = 0, logCountSpeedControl = 0;
		int countPackageLoss = 0;
		long numberDatablocks = fileSize / HoTTbinReader.dataBlockSize;
		long startTimeStamp_ms = file.lastModified() - (numberDatablocks * 10);
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		MenuToolBar menuToolBar = HoTTbinReader.application.getMenuToolBar();
		if (menuToolBar != null) HoTTbinReader.application.setProgress(0, sThreadId);

		try {
			HoTTAdapter.recordSets.clear();
			//receiver data are always contained
			//check if recordSetReceiver initialized, transmitter and receiver data always present, but not in the same data rate and signals
			channel = HoTTbinReader.channels.get(1);
			channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
			recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.RECEIVER.value() + recordSetNameExtend;
			HoTTbinReader.recordSetReceiver = RecordSet.createRecordSet(recordSetName, device, 1, true, true);
			channel.put(recordSetName, HoTTbinReader.recordSetReceiver);
			HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.RECEIVER.value(), HoTTbinReader.recordSetReceiver);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (HoTTbinReader.application.getMenuToolBar() != null) {
				channel.applyTemplate(recordSetName, false);
			}
			//recordSetReceiver initialized and ready to add data

			//channel data are always contained
			if (HoTTAdapter.isChannelsChannelEnabled) {
				//check if recordSetChannel initialized, transmitter and receiver data always present, but not in the same data rate and signals
				channel = HoTTbinReader.channels.get(6);
				channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
				recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.CHANNEL.value() + recordSetNameExtend;
				HoTTbinReader.recordSetChannel = RecordSet.createRecordSet(recordSetName, device, 6, true, true);
				channel.put(recordSetName, HoTTbinReader.recordSetChannel);
				HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.CHANNEL.value(), HoTTbinReader.recordSetChannel);
				tmpRecordSet = channel.get(recordSetName);
				tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
				tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
				if (HoTTbinReader.application.getMenuToolBar() != null) {
					channel.applyTemplate(recordSetName, false);
				}
				//recordSetChannel initialized and ready to add data
			}

			//read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTbinReader.log.isLoggable(java.util.logging.Level.FINEST) && i % 10 == 0) {
					HoTTbinReader.log.logp(java.util.logging.Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
					HoTTbinReader.log.logp(java.util.logging.Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { //buf 0 to 4, tx,rx
					HoTTAdapter.reverseChannelPackageLossCounter.add(1);
					HoTTbinReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
					//create and fill sensor specific data record sets 
					if (HoTTbinReader.log.isLoggable(java.util.logging.Level.FINEST))
						HoTTbinReader.log.logp(java.util.logging.Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1)
								+ GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

					//fill receiver data
					if (HoTTbinReader.buf[33] == 0 && (HoTTbinReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader.buf, 40) >= 0) {
						parseAddReceiver(HoTTbinReader.buf);
					}
					if (HoTTAdapter.isChannelsChannelEnabled) {
						parseAddChannel(HoTTbinReader.buf);
					}

					HoTTbinReader.timeStep_ms += 10;// add default time step from log record of 10 msec

					//detect sensor switch
					if (actualSensor == -1)
						lastSensor = actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);
					else
						actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);

					if (actualSensor != lastSensor) {
						//write data just after sensor switch
						if (logCountVario >= 3 || logCountGPS >= 4 || logCountGeneral >= 5 || logCountElectric >= 5 || logCountSpeedControl >= 3) {
							switch (lastSensor) {
							case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
							case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
								//check if recordSetVario initialized, transmitter and receiver data always present, but not in the same data rate and signals
								if (HoTTbinReader.recordSetVario == null) {
									channel = HoTTbinReader.channels.get(2);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.VARIO.value() + recordSetNameExtend;
									HoTTbinReader.recordSetVario = RecordSet.createRecordSet(recordSetName, device, 2, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetVario);
									HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.VARIO.value(), HoTTbinReader.recordSetVario);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								//recordSetVario initialized and ready to add data
								parseAddVario(1, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2);
								break;

							case HoTTAdapter.SENSOR_TYPE_GPS_115200:
							case HoTTAdapter.SENSOR_TYPE_GPS_19200:
								//check if recordSetReceiver initialized, transmitter and receiver data always present, but not in the same data rate ans signals
								if (HoTTbinReader.recordSetGPS == null) {
									channel = HoTTbinReader.channels.get(3);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GPS.value() + recordSetNameExtend;
									HoTTbinReader.recordSetGPS = RecordSet.createRecordSet(recordSetName, device, 3, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetGPS);
									HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GPS.value(), HoTTbinReader.recordSetGPS);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								//recordSetGPS initialized and ready to add data
								parseAddGPS(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3);
								break;

							case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
							case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
								//check if recordSetGeneral initialized, transmitter and receiver data always present, but not in the same data rate and signals
								if (HoTTbinReader.recordSetGeneral == null) {
									channel = HoTTbinReader.channels.get(4);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GENRAL.value() + recordSetNameExtend;
									HoTTbinReader.recordSetGeneral = RecordSet.createRecordSet(recordSetName, device, 4, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetGeneral);
									HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GENRAL.value(), HoTTbinReader.recordSetGeneral);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								//recordSetGeneral initialized and ready to add data
								parseAddGeneral(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
								break;

							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
								//check if recordSetGeneral initialized, transmitter and receiver data always present, but not in the same data rate and signals
								if (HoTTbinReader.recordSetElectric == null) {
									channel = HoTTbinReader.channels.get(5);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ELECTRIC.value() + recordSetNameExtend;
									HoTTbinReader.recordSetElectric = RecordSet.createRecordSet(recordSetName, device, 5, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetElectric);
									HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.ELECTRIC.value(), HoTTbinReader.recordSetElectric);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								//recordSetElectric initialized and ready to add data
								parseAddElectric(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
								break;

							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
								//check if recordSetGeneral initialized, transmitter and receiver data always present, but not in the same data rate and signals
								if (HoTTbinReader.recordSetSpeedControl == null) {
									channel = HoTTbinReader.channels.get(7);
									channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.SPEED_CONTROL.value() + recordSetNameExtend;
									HoTTbinReader.recordSetSpeedControl = RecordSet.createRecordSet(recordSetName, device, 7, true, true);
									channel.put(recordSetName, HoTTbinReader.recordSetSpeedControl);
									HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.SPEED_CONTROL.value(), HoTTbinReader.recordSetSpeedControl);
									tmpRecordSet = channel.get(recordSetName);
									tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
									tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
									if (HoTTbinReader.application.getMenuToolBar() != null) {
										channel.applyTemplate(recordSetName, false);
									}
								}
								//recordSetElectric initialized and ready to add data
								parseAddSpeedControl(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2);
								break;
							}
						}

						if (HoTTbinReader.log.isLoggable(java.util.logging.Level.FINE))
							HoTTbinReader.log.logp(java.util.logging.Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "logCountVario = " + logCountVario + " logCountGPS = " + logCountGPS
									+ " logCountGeneral = " + logCountGeneral + " logCountElectric = " + logCountElectric + " logCountMotorDriver = " + logCountSpeedControl);
						lastSensor = actualSensor;
						logCountVario = logCountGPS = logCountGeneral = logCountElectric = logCountSpeedControl = 0;
					}
					else {
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
					}

					//fill data block 0 to 4
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

					if (menuToolBar != null && i % 100 == 0) HoTTbinReader.application.setProgress((int) (i * 100 / numberDatablocks), sThreadId);
				}
				else { //tx,rx == 0
					HoTTAdapter.reverseChannelPackageLossCounter.add(0);
					HoTTbinReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
					
					++countPackageLoss;	// add up lost packages in telemetry data 
					//HoTTbinReader.pointsReceiver[0] = (int) (countPackageLoss*100.0 / ((HoTTbinReader2.timeStep_ms+10) / 10.0)*1000.0); 

					if (HoTTAdapter.isChannelsChannelEnabled) {
						parseAddChannel(HoTTbinReader.buf);
					}
					
					HoTTbinReader.timeStep_ms += 10;
					//reset buffer to avoid mixing data
					logCountVario = logCountGPS = logCountGeneral = logCountElectric = logCountSpeedControl = 0;
				}
			}
			String packageLossPercentage = HoTTbinReader.recordSetReceiver.getRecordDataSize(true) > 0 ? String.format("%.1f", (countPackageLoss / HoTTbinReader.recordSetReceiver.getTime_ms(HoTTbinReader.recordSetReceiver.getRecordDataSize(true)-1) * 1000)) : "0";
			HoTTbinReader.recordSetReceiver.setRecordSetDescription(tmpRecordSet.getRecordSetDescription() + Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] {countPackageLoss, packageLossPercentage}));
			HoTTbinReader.log.logp(java.util.logging.Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReader.log.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (menuToolBar != null) {
				if (!isInitialSwitched) {
					HoTTbinReader.channels.switchChannel(channel.getName());
					channel.switchRecordSet(recordSetName);
				}
	
				for (RecordSet recordSet : HoTTAdapter.recordSets.values()) {
					device.makeInActiveDisplayable(recordSet);
					device.updateVisibilityStatus(recordSet, true);
					
					//write filename after import to record description
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
	 * parse the buffered data from buffer and add points to record set
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	private static void parseAddReceiver(byte[] _buf) throws DataInconsitsentException {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 
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
		}

		//printByteValues(_timeStep_ms, _buf);

		HoTTbinReader.recordSetReceiver.addPoints(HoTTbinReader.pointsReceiver, HoTTbinReader.timeStep_ms);
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	private static void parseAddChannel(byte[] _buf) throws DataInconsitsentException {
		
		//0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16
		//19=PowerOff, 20=BattLow, 21=Reset, 22=reserved
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
		HoTTbinReader.pointsChannel[22] = (_buf[50] & 0x00) * 1000; //reserved for future use
		if (_buf[5] == 0x00) { //channel 9-12
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
		else { //channel 13-16
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

		//printByteValues(_timeStep_ms, _buf);

		HoTTbinReader.recordSetChannel.addPoints(HoTTbinReader.pointsChannel, HoTTbinReader.timeStep_ms);
	}

	/**
	 * parse the buffered data from buffer 0 to 2 and add points to record set
	 * @param sdLogVersion
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @throws DataInconsitsentException
	 */
	private static int parseAddVario(int sdLogVersion, byte[] _buf0, byte[] _buf1, byte[] _buf2) throws DataInconsitsentException {
		if (sdLogVersion == -1) sdLogVersion = getSdLogVerion(_buf1, _buf2);
		switch (sdLogVersion) {
		case 3:
			//0=RXSQ, 1=Height, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
			HoTTbinReader.pointsVario[0] = (_buf1[0] & 0xFF) * 1000;
			HoTTbinReader.pointsVario[1] = DataParser.parse2Short(_buf1, 3) * 1000;
			//pointsVario[0]max = DataParser.parse2Short(_buf1, 5) * 1000;
			//pointsVario[0]min = DataParser.parse2Short(_buf1, 7) * 1000;
			HoTTbinReader.pointsVario[2] = DataParser.parse2UnsignedShort(_buf1[9], _buf2[0]) * 1000;
			HoTTbinReader.pointsVario[3] = DataParser.parse2UnsignedShort(_buf1[1], _buf2[2]) * 1000;
			HoTTbinReader.pointsVario[4] = DataParser.parse2UnsignedShort(_buf1[3], _buf2[4]) * 1000;
			HoTTbinReader.pointsVario[5] = (_buf0[1] & 0xFF) * 1000;
			HoTTbinReader.pointsVario[6] = (_buf0[2] & 0xFF) * 1000;

			HoTTbinReader.recordSetVario.addPoints(HoTTbinReader.pointsVario, HoTTbinReader.timeStep_ms);
			break;
		default:
		case 4:
			//0=RXSQ, 1=Height, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
			HoTTbinReader.pointsVario[0] = (_buf0[4] & 0xFF) * 1000;
			HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf1, 2);
			HoTTbinReader.tmpClimb10 = DataParser.parse2UnsignedShort(_buf2, 2);
			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && HoTTbinReader.tmpClimb10 < 40000 && HoTTbinReader.tmpClimb10 > 20000) {
				HoTTbinReader.pointsVario[1] = HoTTbinReader.tmpHeight * 1000;
				//pointsVarioMax = DataParser.parse2Short(buf1, 4) * 1000;
				//pointsVarioMin = DataParser.parse2Short(buf1, 6) * 1000;
				HoTTbinReader.pointsVario[2] = DataParser.parse2UnsignedShort(_buf1, 8) * 1000;
				HoTTbinReader.pointsVario[3] = DataParser.parse2UnsignedShort(_buf2, 0) * 1000;
				HoTTbinReader.pointsVario[4] = HoTTbinReader.tmpClimb10 * 1000;
				HoTTbinReader.pointsVario[5] = (_buf0[1] & 0xFF) * 1000;
				HoTTbinReader.pointsVario[6] = (_buf0[2] & 0xFF) * 1000;

				HoTTbinReader.recordSetVario.addPoints(HoTTbinReader.pointsVario, HoTTbinReader.timeStep_ms);
			}
			break;
		}
		//printByteValues(_timeStep_ms, _buf0);
		//printShortValues(_timeStep_ms, _buf1);
		//printShortValues(_timeStep_ms, _buf2);
		//log.log(Level.FINEST, "");
		return sdLogVersion;
	}

	/**
	 * detect the SD Log Version V3 or V4
	 * @param _buf1
	 * @param _buf2
	 * @return
	 */
	protected static int getSdLogVerion(byte[] _buf1, byte[] _buf2) {
		//printByteValues(1, _buf1);
		//printByteValues(2, _buf2);
		if (HoTTbinReader.log.isLoggable(java.util.logging.Level.FINER))
			HoTTbinReader.log.log(java.util.logging.Level.FINER, "version = " + (((DataParser.parse2UnsignedShort(_buf1[3], _buf2[4]) - 30000) < -100) ? 4 : 3));
		return ((DataParser.parse2UnsignedShort(_buf1[3], _buf2[4]) - 30000) < -100) ? 4 : 3;
	}

	/**
	 * parse the buffered data from buffer 0 to 3 and add points to record set
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @throws DataInconsitsentException
	 */
	private static void parseAddGPS(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3) throws DataInconsitsentException {
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf2, 8);
		HoTTbinReader.tmpClimb3 = (_buf3[2] & 0xFF);
		HoTTbinReader.pointsGPS[0] = (_buf0[4] & 0xFF) * 1000;
		if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpClimb3 > 50 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000) {
			//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripLength, 10=VoltageRx, 11=TemperatureRx
			HoTTbinReader.pointsGPS[6] = DataParser.parse2Short(_buf1, 4) * 1000;

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
				if (HoTTbinReader.log.isLoggable(java.util.logging.Level.FINE))
					HoTTbinReader.log.log(java.util.logging.Level.FINE, StringHelper.getFormatedTime("HH:mm:ss:SSS", HoTTbinReader.timeStep_ms - GDE.ONE_HOUR_MS) + " Lat " + HoTTbinReader.tmpLatitude + " - "
							+ HoTTbinReader.tmpLatitudeDelta);
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
				if (HoTTbinReader.log.isLoggable(java.util.logging.Level.FINE))
					HoTTbinReader.log.log(java.util.logging.Level.FINE, StringHelper.getFormatedTime("HH:mm:ss:SSS", HoTTbinReader.timeStep_ms - GDE.ONE_HOUR_MS) + " Long " + HoTTbinReader.tmpLongitude + " - "
							+ HoTTbinReader.tmpLongitudeDelta + " - " + HoTTbinReader.longitudeTolerance);
			}

			HoTTbinReader.pointsGPS[3] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsGPS[4] = DataParser.parse2UnsignedShort(_buf3, 0) * 1000;
			HoTTbinReader.pointsGPS[5] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsGPS[7] = DataParser.parse2Short(_buf2, 6) * 1000;
			HoTTbinReader.pointsGPS[8] = (_buf1[3] & 0xFF) * 1000;
			HoTTbinReader.pointsGPS[9] = 0;
			HoTTbinReader.pointsGPS[10] = (_buf0[1] & 0xFF) * 1000;
			HoTTbinReader.pointsGPS[11] = (_buf0[2] & 0xFF) * 1000;

			HoTTbinReader.recordSetGPS.addPoints(HoTTbinReader.pointsGPS, HoTTbinReader.timeStep_ms);
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 * @throws DataInconsitsentException
	 */
	private static void parseAddGeneral(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) throws DataInconsitsentException {
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf3, 0);
		HoTTbinReader.tmpClimb3 = (_buf3[4] & 0xFF);
		HoTTbinReader.tmpVoltage1 = DataParser.parse2Short(_buf1[9], _buf2[0]);
		HoTTbinReader.tmpVoltage2 = DataParser.parse2Short(_buf2, 1);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
		//0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Height, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2							
		HoTTbinReader.pointsGeneral[0] = (_buf0[4] & 0xFF) * 1000;
		if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpClimb3 > 50 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600
				&& Math.abs(HoTTbinReader.tmpVoltage2) < 600 && HoTTbinReader.tmpCapacity >= HoTTbinReader.pointsGeneral[3] / 1000) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			HoTTbinReader.pointsGeneral[1] = DataParser.parse2Short(_buf3, 7) * 1000;
			//filter current drops to zero if current > 10 A
			HoTTbinReader.tmpCurrent = DataParser.parse2Short(_buf3, 5);
			HoTTbinReader.pointsGeneral[2] = (!HoTTAdapter.isFilterEnabled && (HoTTbinReader.pointsGeneral[2] > 10000 && (HoTTbinReader.pointsGeneral[2] - HoTTbinReader.tmpCurrent) == HoTTbinReader.pointsElectric[2])) ? HoTTbinReader.pointsGeneral[2] : HoTTbinReader.tmpCurrent * 1000;
			HoTTbinReader.pointsGeneral[3] = HoTTbinReader.tmpCapacity * 1000;
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

			HoTTbinReader.recordSetGeneral.addPoints(HoTTbinReader.pointsGeneral, HoTTbinReader.timeStep_ms);
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 * @throws DataInconsitsentException
	 */
	private static void parseAddElectric(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) throws DataInconsitsentException {
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf3, 3);
		HoTTbinReader.tmpClimb3 = (_buf4[3] & 0xFF);
		HoTTbinReader.tmpVoltage1 = DataParser.parse2Short(_buf2, 7);
		HoTTbinReader.tmpVoltage2 = DataParser.parse2Short(_buf2[9], _buf3[0]);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
		//0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2 
		HoTTbinReader.pointsElectric[0] = (_buf1[4] & 0xFF) * 1000;
		if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpClimb3 > 50 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600
				&& Math.abs(HoTTbinReader.tmpVoltage2) < 600 && HoTTbinReader.tmpCapacity >= HoTTbinReader.pointsElectric[3] / 1000) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			HoTTbinReader.pointsElectric[1] = DataParser.parse2Short(_buf3, 7) * 1000;
			//filter current drops to zero if current > 10 A
			HoTTbinReader.tmpCurrent = DataParser.parse2Short(_buf3, 5);
			HoTTbinReader.pointsElectric[2] = (!HoTTAdapter.isFilterEnabled && (HoTTbinReader.pointsElectric[2] > 10000 && (HoTTbinReader.pointsElectric[2] - HoTTbinReader.tmpCurrent) == HoTTbinReader.pointsElectric[2])) ? HoTTbinReader.pointsElectric[2] : HoTTbinReader.tmpCurrent * 1000;
			HoTTbinReader.pointsElectric[3] = HoTTbinReader.tmpCapacity * 1000;
			HoTTbinReader.pointsElectric[4] = Double.valueOf(HoTTbinReader.pointsElectric[1] / 1000.0 * HoTTbinReader.pointsElectric[2]).intValue(); // power U*I [W];
			HoTTbinReader.pointsElectric[5] = 0; //5=Balance
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
			//calculate balance on the fly
			HoTTbinReader.pointsElectric[5] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
			HoTTbinReader.pointsElectric[20] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsElectric[21] = DataParser.parse2UnsignedShort(_buf4, 1) * 1000;
			HoTTbinReader.pointsElectric[22] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsElectric[23] = HoTTbinReader.tmpVoltage1 * 1000;
			HoTTbinReader.pointsElectric[24] = HoTTbinReader.tmpVoltage2 * 1000;
			HoTTbinReader.pointsElectric[25] = (_buf3[1] & 0xFF) * 1000;
			HoTTbinReader.pointsElectric[26] = (_buf3[2] & 0xFF) * 1000;

			HoTTbinReader.recordSetElectric.addPoints(HoTTbinReader.pointsElectric, HoTTbinReader.timeStep_ms);
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 * @throws DataInconsitsentException
	 */
	private static void parseAddSpeedControl(byte[] _buf0, byte[] _buf1, byte[] _buf2) throws DataInconsitsentException {
		//0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature
		HoTTbinReader.tmpVoltage = DataParser.parse2Short(_buf1, 3);
		HoTTbinReader.tmpCurrent = DataParser.parse2Short(_buf2, 1);
		HoTTbinReader.pointsSpeedControl[0] = (_buf0[4] & 0xFF) * 1000;
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf1, 7);
		HoTTbinReader.tmpRevolution = DataParser.parse2Short(_buf2, 5);
		HoTTbinReader.tmpTemperatureFet = _buf1[9];
		//HoTTbinReader.tmpTemperatureExt = _buf2[9];
		if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpVoltage > 0 && HoTTbinReader.tmpVoltage < 1000 && HoTTbinReader.tmpCurrent < 1000
				&& HoTTbinReader.tmpRevolution > -1 && HoTTbinReader.tmpRevolution < 2000 && HoTTbinReader.tmpCapacity < 2000 && HoTTbinReader.tmpCapacity >= HoTTbinReader.pointsSpeedControl[3]/1000) {
			HoTTbinReader.pointsSpeedControl[1] = HoTTbinReader.tmpVoltage * 1000;
			HoTTbinReader.pointsSpeedControl[2] = HoTTbinReader.tmpCurrent * 1000;
			HoTTbinReader.pointsSpeedControl[4] = Double.valueOf(HoTTbinReader.pointsSpeedControl[1] / 1000.0 * HoTTbinReader.pointsSpeedControl[2]).intValue();
			HoTTbinReader.pointsSpeedControl[3] = HoTTbinReader.tmpCapacity * 1000;
			HoTTbinReader.pointsSpeedControl[5] = HoTTbinReader.tmpRevolution * 1000;
			HoTTbinReader.pointsSpeedControl[6] = (HoTTbinReader.tmpTemperatureFet - 20) * 1000;
			//HoTTbinReader.pointsSpeedControl[7] = HoTTbinReader.tmpTemperatureExt * 1000;
		}
		HoTTbinReader.recordSetSpeedControl.addPoints(HoTTbinReader.pointsSpeedControl, HoTTbinReader.timeStep_ms);
	}

	static void printByteValues(long millisec, byte[] buffer) {
		StringBuilder sb = new StringBuilder().append(StringHelper.getFormatedTime("mm:ss:SSS", millisec)).append(" : ");
		for (int i = 0; buffer != null && i < buffer.length; i++) {
			sb.append("(").append(i).append(")").append(buffer[i]).append(GDE.STRING_BLANK);
		}
		HoTTbinReader.log.log(java.util.logging.Level.FINE, sb.toString());
	}

	static void printShortValues(long millisec, byte[] buffer) {
		StringBuilder sb = new StringBuilder().append(StringHelper.getFormatedTime("mm:ss:SSS", millisec)).append(" : ");
		for (int i = 0; buffer != null && i < buffer.length - 1; i++) {
			sb.append("(").append(i).append(")").append(DataParser.parse2Short(buffer, i)).append(GDE.STRING_BLANK);
		}
		HoTTbinReader.log.log(java.util.logging.Level.FINE, sb.toString());
	}

	/**
	 * main method for test purpose only !
	 * @param args
	 */
	public static void main(String[] args) {
		String directory = "f:\\Documents\\DataExplorer\\HoTTAdapter\\";

		try {
			List<File> files = FileUtils.getFileListing(new File(directory), 2);
			for (File file : files) {
				if (!file.isDirectory() && file.getName().endsWith(".bin")) {
					//					FileInputStream file_input = new FileInputStream(file);
					//					DataInputStream data_in = new DataInputStream(file_input);
					//					HoTTbinReader.buf = new byte[64];
					//					data_in.read(HoTTbinReader.buf);
					//					System.out.println(file.getName());
					//					System.out.println(StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
					//					System.out.println(StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
					//					data_in.close();
					System.out.println(file.getName() + " - " + Integer.parseInt(getFileInfo(file).get(HoTTAdapter.SD_LOG_VERSION)));

				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
