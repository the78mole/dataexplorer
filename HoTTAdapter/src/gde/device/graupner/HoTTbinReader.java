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
    
    Copyright (c) 2011,2012 Winfried Bruegmann
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
	final static String						$CLASS_NAME		= HoTTbinReader.class.getName();
	final static Logger						log						= Logger.getLogger(HoTTbinReader.$CLASS_NAME);

	final static DataExplorer			application		= DataExplorer.getInstance();
	final static Channels					channels			= Channels.getInstance();

	static int										dataBlockSize	= 64;
	static byte[]									buf;
	static byte[]									buf0, buf1, buf2, buf3, buf4;
	static long										timeStep_ms;
	static int[]									pointsReceiver, pointsGeneral, pointsElectric, pointsVario, pointsGPS;
	static RecordSet							recordSetReceiver, recordSetGeneral, recordSetElectric, recordSetVario, recordSetGPS;

	/**
	 * get data file info data
	 * @param buffer byte array containing the first 64 byte to analyze the header
	 * @return hash map containing header data as string accessible by public header keys
	 * @throws IOException 
	 */
	public static HashMap<String, String> getFileInfo(File file) throws IOException {
		final String $METHOD_NAME = "getHeader";
		DataInputStream data_in = null;
		byte[] buffer = new byte[64];
		HashMap<String, String> fileInfo;
		int sensorCount = 0;
		int versionCount = 0;
		long numberLogs = (file.length() / 64);

		try {
			if (numberLogs > 50) {
				for (int i = 0; i < HoTTAdapter.isSensorType.length; i++) {
					HoTTAdapter.isSensorType[i] = false;
				}
				FileInputStream file_input = new FileInputStream(file);
				data_in = new DataInputStream(file_input);
				fileInfo = new HashMap<String, String>();
				HoTTbinReader.log.logp(Level.FINER, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(buffer.length));
				for (int i = 0; i < 50; i++) {
					data_in.read(buffer);
					HoTTbinReader.log.logp(Level.FINER, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(buffer, buffer.length));

					if (buffer[5] != 0x00) versionCount += (buffer[5] & 0xFF);

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
					}
				}
				for (boolean element : HoTTAdapter.isSensorType) {
					if (element == true) ++sensorCount;
				}
				//more then one sensor is supported with new format only, 
				fileInfo.put(HoTTAdapter.SD_LOG_VERSION, GDE.STRING_EMPTY + (sensorCount > 1 || versionCount > 0 ? 1 : 0));
				fileInfo.put(HoTTAdapter.SENSOR_COUNT, GDE.STRING_EMPTY + sensorCount);
				fileInfo.put(HoTTAdapter.LOG_COUNT, GDE.STRING_EMPTY + (file.length() / 64));

				if (HoTTbinReader.log.isLoggable(Level.FINE)) for (Entry<String, String> entry : fileInfo.entrySet()) {
					HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, entry.getKey() + " = " + entry.getValue());
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

	static int convertRFRXSQ2Strenght(int inValue) {
		// RF_RXSQ_to_Strength(72-ShortInt(buf[0].data_3_1[3]) DIV 2)
		int result = 0;
		inValue = 72 - inValue;
		if (inValue < 31)
			result = 100;
		else if (inValue >= 31 && inValue <= 64)
			result = ((int) ((inValue * (-0.5) + 117.25) / 5) * 5);
		else if (inValue >= 65 && inValue <= 76)
			result = ((int) ((inValue * (-5.0) + 410.00) / 5) * 5);
		else if (inValue >= 77 && inValue <= 89)
			result = ((int) ((inValue * (-2.5) + 223.75) / 5) * 5);
		else
			result = 0;
		return result * 5 / 10;
	}

	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws Exception 
	 */
	public static synchronized void read(String filePath) throws Exception {
		HashMap<String, String> header = null;
		int sdLogFormatVersion = 0;
		File file = new File(filePath);

		header = getFileInfo(file);
		sdLogFormatVersion = Integer.parseInt(header.get(HoTTAdapter.SD_LOG_VERSION));

		if (Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) <= 1)
			readSingle(file, sdLogFormatVersion);
		else
			readMultiple(file);
	}

	/**
	* read log data according to version 0
	* @param file
	* @param data_in
	* @throws IOException 
	* @throws DataInconsitsentException 
	*/
	static void readSingle(File file, int version) throws IOException, DataInconsitsentException {
		final String $METHOD_NAME = "readSingle";
		long startTime = System.nanoTime() / 1000000;
		long actualTime_ms = 0, drawTime_ms = startTime;
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
		HoTTbinReader.pointsReceiver = new int[8];
		HoTTbinReader.pointsGeneral = new int[21];
		HoTTbinReader.pointsElectric = new int[27];
		HoTTbinReader.pointsVario = new int[7];
		HoTTbinReader.pointsGPS = new int[12];
		HoTTbinReader.timeStep_ms = 0;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		HoTTbinReader.buf0 = null;
		HoTTbinReader.buf1 = null;
		HoTTbinReader.buf2 = null;
		HoTTbinReader.buf3 = null;
		HoTTbinReader.buf4 = null;
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
			channel.setFileDescription(application.isObjectoriented() ? date + GDE.STRING_BLANK + application.getObjectKey() : date);
			recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.RECEIVER.value() + recordSetNameExtend;
			HoTTbinReader.recordSetReceiver = RecordSet.createRecordSet(recordSetName, device, 1, true, true);
			channel.put(recordSetName, HoTTbinReader.recordSetReceiver);
			HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.RECEIVER.value(), HoTTbinReader.recordSetReceiver);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (HoTTbinReader.application.getMenuToolBar() != null) {
				channel.applyTemplate(recordSetName, true);
			}
			//recordSetReceiver initialized and ready to add data

			//read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTbinReader.log.isLoggable(Level.FINER) && i % 10 == 0) {
					HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
				}
				HoTTbinReader.log.logp(Level.FINER, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));

				if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { //skip empty block - package loss
					//create and fill sensor specific data record sets 
					if (HoTTbinReader.log.isLoggable(Level.FINER))
						HoTTbinReader.log.logp(Level.FINER, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1)
								+ GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

					//fill receiver data
					if (HoTTbinReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader.buf, 40) != 0 && HoTTbinReader.timeStep_ms % 10 == 0) {
						parseAddReceiver(HoTTbinReader.recordSetReceiver, HoTTbinReader.pointsReceiver, HoTTbinReader.buf, HoTTbinReader.timeStep_ms);
					}

					switch ((byte) (HoTTbinReader.buf[7] & 0xFF)) {
					case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
					case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
						//check if recordSetVario initialized, transmitter and receiver data always present, but not in the same data rate and signals
						if (HoTTbinReader.recordSetVario == null) {
							channel = HoTTbinReader.channels.get(2);
							channel.setFileDescription(application.isObjectoriented() ? date + GDE.STRING_BLANK + application.getObjectKey() : date);
							recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.VARIO.value() + recordSetNameExtend;
							HoTTbinReader.recordSetVario = RecordSet.createRecordSet(recordSetName, device, 2, true, true);
							channel.put(recordSetName, HoTTbinReader.recordSetVario);
							HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.VARIO.value(), HoTTbinReader.recordSetVario);
							tmpRecordSet = channel.get(recordSetName);
							tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
							tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
							if (HoTTbinReader.application.getMenuToolBar() != null) {
								channel.applyTemplate(recordSetName, true);
							}
						}
						//recordSetVario initialized and ready to add data
						//fill data block 0 receiver voltage an temperature
						if (HoTTbinReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader.buf, 0) != 0) {
							HoTTbinReader.buf0 = new byte[30];
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf0, 0, HoTTbinReader.buf0.length);
						}
						//fill data block 1 to 3
						if (HoTTbinReader.buf[33] == 1) {
							HoTTbinReader.buf1 = new byte[30];
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf1, 0, HoTTbinReader.buf1.length);
						}
						if (HoTTbinReader.buf[33] == 2) {
							HoTTbinReader.buf2 = new byte[30];
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf2, 0, HoTTbinReader.buf2.length);
						}

						if (HoTTbinReader.buf0 != null && HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null) {
							parseAddVario(HoTTbinReader.recordSetVario, HoTTbinReader.pointsVario, version, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.timeStep_ms);
							HoTTbinReader.buf0 = HoTTbinReader.buf1 = HoTTbinReader.buf2 = null;
							if (numberDatablocks > 30000) { //5 minutes
								data_in.skip(HoTTbinReader.dataBlockSize * 50); //take from data points only each half second 
								i += 50;
								HoTTbinReader.timeStep_ms = HoTTbinReader.timeStep_ms += 500;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_GPS_115200:
					case HoTTAdapter.SENSOR_TYPE_GPS_19200:
						//check if recordSetReceiver initialized, transmitter and receiver data always present, but not in the same data rate as signals
						if (HoTTbinReader.recordSetGPS == null) {
							channel = HoTTbinReader.channels.get(3);
							channel.setFileDescription(application.isObjectoriented() ? date + GDE.STRING_BLANK + application.getObjectKey() : date);
							recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GPS.value() + recordSetNameExtend;
							HoTTbinReader.recordSetGPS = RecordSet.createRecordSet(recordSetName, device, 3, true, true);
							channel.put(recordSetName, HoTTbinReader.recordSetGPS);
							HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GPS.value(), HoTTbinReader.recordSetGPS);
							tmpRecordSet = channel.get(recordSetName);
							tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
							tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
							if (HoTTbinReader.application.getMenuToolBar() != null) {
								channel.applyTemplate(recordSetName, true);
							}
						}
						//recordSetGPS initialized and ready to add data
						//fill data block 0 receiver voltage an temperature
						if (HoTTbinReader.buf0 == null && HoTTbinReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader.buf, 0) != 0) {
							HoTTbinReader.buf0 = new byte[30];
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf0, 0, HoTTbinReader.buf0.length);
						}
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

						if (HoTTbinReader.buf0 != null && HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null && HoTTbinReader.buf3 != null) {
							parseAddGPS(HoTTbinReader.recordSetGPS, HoTTbinReader.pointsGPS, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.timeStep_ms);
							HoTTbinReader.buf0 = HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = null;
							if (numberDatablocks > 30000) { //5 minutes
								data_in.skip(HoTTbinReader.dataBlockSize * 50); //take from data points only each half second 
								i += 50;
								HoTTbinReader.timeStep_ms = HoTTbinReader.timeStep_ms += 500;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
					case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
						//check if recordSetGeneral initialized, transmitter and receiver data always present, but not in the same data rate and signals
						if (HoTTbinReader.recordSetGeneral == null) {
							channel = HoTTbinReader.channels.get(4);
							channel.setFileDescription(application.isObjectoriented() ? date + GDE.STRING_BLANK + application.getObjectKey() : date);
							recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GENRAL.value() + recordSetNameExtend;
							HoTTbinReader.recordSetGeneral = RecordSet.createRecordSet(recordSetName, device, 4, true, true);
							channel.put(recordSetName, HoTTbinReader.recordSetGeneral);
							HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GENRAL.value(), HoTTbinReader.recordSetGeneral);
							tmpRecordSet = channel.get(recordSetName);
							tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
							tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
							if (HoTTbinReader.application.getMenuToolBar() != null) {
								channel.applyTemplate(recordSetName, true);
							}
						}
						//recordSetGeneral initialized and ready to add data
						//fill data block 1 to 4
						if (HoTTbinReader.buf0 == null && HoTTbinReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader.buf, 0) != 0) {
							HoTTbinReader.buf0 = new byte[30];
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf0, 0, HoTTbinReader.buf0.length);
						}
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

						if (HoTTbinReader.buf0 != null && HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null && HoTTbinReader.buf3 != null && HoTTbinReader.buf4 != null) {
							parseAddGeneral(HoTTbinReader.recordSetGeneral, HoTTbinReader.pointsGeneral, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4,
									HoTTbinReader.timeStep_ms);
							HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
							if (numberDatablocks > 30000) { //5 minutes
								data_in.skip(HoTTbinReader.dataBlockSize * 50); //take from data points only each half second 
								i += 50;
								HoTTbinReader.timeStep_ms = HoTTbinReader.timeStep_ms += 500;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
						//check if recordSetGeneral initialized, transmitter and receiver data always present, but not in the same data rate and signals
						if (HoTTbinReader.recordSetElectric == null) {
							channel = HoTTbinReader.channels.get(5);
							channel.setFileDescription(application.isObjectoriented() ? date + GDE.STRING_BLANK + application.getObjectKey() : date);
							recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ELECTRIC.value() + recordSetNameExtend;
							HoTTbinReader.recordSetElectric = RecordSet.createRecordSet(recordSetName, device, 5, true, true);
							channel.put(recordSetName, HoTTbinReader.recordSetElectric);
							HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.ELECTRIC.value(), HoTTbinReader.recordSetElectric);
							tmpRecordSet = channel.get(recordSetName);
							tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
							tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
							if (HoTTbinReader.application.getMenuToolBar() != null) {
								channel.applyTemplate(recordSetName, true);
							}
						}
						//recordSetElectric initialized and ready to add data
						//fill data block 0 to 4
						if (HoTTbinReader.buf0 == null && HoTTbinReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader.buf, 0) != 0) {
							HoTTbinReader.buf0 = new byte[30];
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf0, 0, HoTTbinReader.buf0.length);
						}
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

						if (HoTTbinReader.buf0 != null && HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null && HoTTbinReader.buf3 != null && HoTTbinReader.buf4 != null) {
							parseAddElectric(HoTTbinReader.recordSetElectric, HoTTbinReader.pointsElectric, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4,
									HoTTbinReader.timeStep_ms);
							HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
							if (numberDatablocks > 30000) { //5 minutes
								data_in.skip(HoTTbinReader.dataBlockSize * 50); //take from data points only each half second 
								i += 50;
								HoTTbinReader.timeStep_ms = HoTTbinReader.timeStep_ms += 500;
							}
						}
						break;
					}

					// add default time step from device of 10 msec
					HoTTbinReader.timeStep_ms += 10;

					if (menuToolBar != null && i % 100 == 0) HoTTbinReader.application.setProgress((int) (i * 100 / numberDatablocks), sThreadId);
				}
				else { //skip empty block, but add time step
					++countPackageLoss;
					HoTTbinReader.timeStep_ms += 10;
				}

				actualTime_ms = System.nanoTime() / 1000000;
				if (menuToolBar != null && (actualTime_ms - drawTime_ms) > 5000) {
					if (!isInitialSwitched) {
						HoTTbinReader.channels.switchChannel(channel.getName());
						device.updateVisibilityStatus(channel.getActiveRecordSet(), true);
						channel.switchRecordSet(recordSetName);
						isInitialSwitched = true;
					}
					else
						HoTTbinReader.application.updateGraphicsWindow();

					drawTime_ms = actualTime_ms;
					HoTTbinReader.log.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (actualTime_ms - startTime))); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			HoTTbinReader.log.logp(Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReader.log.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (actualTime_ms - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (menuToolBar != null) {
				if (!isInitialSwitched) {
					HoTTbinReader.channels.switchChannel(channel.getName());
					channel.switchRecordSet(recordSetName);
				}

				for (RecordSet recordSet : HoTTAdapter.recordSets.values()) {
					device.makeInActiveDisplayable(recordSet);
					device.updateVisibilityStatus(recordSet, true);
				}

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();

				HoTTbinReader.application.updateGraphicsWindow();
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
				if (file.getName().substring(0, file.getName().length()).length() <= 8+4)
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, file.getName().length()-4) + GDE.STRING_RIGHT_BRACKET;
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
		long actualTime_ms = 0, drawTime_ms = startTime;
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
		HoTTbinReader.pointsReceiver = new int[8];
		HoTTbinReader.pointsGeneral = new int[21];
		HoTTbinReader.pointsElectric = new int[27];
		HoTTbinReader.pointsVario = new int[7];
		HoTTbinReader.pointsGPS = new int[12];
		HoTTbinReader.timeStep_ms = 0;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		HoTTbinReader.buf0 = new byte[30];
		HoTTbinReader.buf1 = new byte[30];
		HoTTbinReader.buf2 = new byte[30];
		HoTTbinReader.buf3 = new byte[30];
		HoTTbinReader.buf4 = new byte[30];
		byte actualSensor = -1, lastSensor = -1;
		int lastLogCountVario = 0, lastLogCountGPS = 0, lastLogCountGeneral = 0, lastLogCountElectric = 0, logCountVario = 0, logCountGPS = 0, logCountGeneral = 0, logCountElectric = 0;
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
			channel.setFileDescription(application.isObjectoriented() ? date + GDE.STRING_BLANK + application.getObjectKey() : date);
			recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.RECEIVER.value() + recordSetNameExtend;
			HoTTbinReader.recordSetReceiver = RecordSet.createRecordSet(recordSetName, device, 1, true, true);
			channel.put(recordSetName, HoTTbinReader.recordSetReceiver);
			HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.RECEIVER.value(), HoTTbinReader.recordSetReceiver);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (HoTTbinReader.application.getMenuToolBar() != null) {
				channel.applyTemplate(recordSetName, true);
			}
			//recordSetReceiver initialized and ready to add data

			//read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTbinReader.log.isLoggable(Level.FINEST) && i % 10 == 0) {
					HoTTbinReader.log.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
				}
				HoTTbinReader.log.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));

				if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { //skip empty block - package loss
					//create and fill sensor specific data record sets 
					if (HoTTbinReader.log.isLoggable(Level.FINEST))
						HoTTbinReader.log.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1)
								+ GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

					if (HoTTbinReader.buf[38] != 0) { //receiver RF_RXSQ
						//fill receiver data
						if (HoTTbinReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader.buf, 40) != 0 && HoTTbinReader.timeStep_ms % 10 == 0) {
							parseAddReceiver(HoTTbinReader.recordSetReceiver, HoTTbinReader.pointsReceiver, HoTTbinReader.buf, HoTTbinReader.timeStep_ms);
						}
						if (actualSensor == -1)
							lastSensor = actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);
						else
							actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);

						if (actualSensor != lastSensor) {
							if (logCountVario > 3 || logCountGPS > 4 || logCountGeneral > 5 || logCountElectric > 5) {
								switch (lastSensor) {
								case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
								case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
									++logCountVario;
									//check if recordSetVario initialized, transmitter and receiver data always present, but not in the same data rate and signals
									if (HoTTbinReader.recordSetVario == null) {
										channel = HoTTbinReader.channels.get(2);
										channel.setFileDescription(application.isObjectoriented() ? date + GDE.STRING_BLANK + application.getObjectKey() : date);
										recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.VARIO.value() + recordSetNameExtend;
										HoTTbinReader.recordSetVario = RecordSet.createRecordSet(recordSetName, device, 2, true, true);
										channel.put(recordSetName, HoTTbinReader.recordSetVario);
										HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.VARIO.value(), HoTTbinReader.recordSetVario);
										tmpRecordSet = channel.get(recordSetName);
										tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
										tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
										if (HoTTbinReader.application.getMenuToolBar() != null) {
											channel.applyTemplate(recordSetName, true);
										}
									}
									//recordSetVario initialized and ready to add data
									parseAddVario(HoTTbinReader.recordSetVario, HoTTbinReader.pointsVario, 1, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.timeStep_ms);
									break;

								case HoTTAdapter.SENSOR_TYPE_GPS_115200:
								case HoTTAdapter.SENSOR_TYPE_GPS_19200:
									++logCountGPS;
									//check if recordSetReceiver initialized, transmitter and receiver data always present, but not in the same data rate ans signals
									if (HoTTbinReader.recordSetGPS == null) {
										channel = HoTTbinReader.channels.get(3);
										channel.setFileDescription(application.isObjectoriented() ? date + GDE.STRING_BLANK + application.getObjectKey() : date);
										recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GPS.value() + recordSetNameExtend;
										HoTTbinReader.recordSetGPS = RecordSet.createRecordSet(recordSetName, device, 3, true, true);
										channel.put(recordSetName, HoTTbinReader.recordSetGPS);
										HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GPS.value(), HoTTbinReader.recordSetGPS);
										tmpRecordSet = channel.get(recordSetName);
										tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
										tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
										if (HoTTbinReader.application.getMenuToolBar() != null) {
											channel.applyTemplate(recordSetName, true);
										}
									}
									//recordSetGPS initialized and ready to add data
									parseAddGPS(HoTTbinReader.recordSetGPS, HoTTbinReader.pointsGPS, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.timeStep_ms);
									break;

								case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
								case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
									++logCountGeneral;
									//check if recordSetGeneral initialized, transmitter and receiver data always present, but not in the same data rate and signals
									if (HoTTbinReader.recordSetGeneral == null) {
										channel = HoTTbinReader.channels.get(4);
										channel.setFileDescription(application.isObjectoriented() ? date + GDE.STRING_BLANK + application.getObjectKey() : date);
										recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GENRAL.value() + recordSetNameExtend;
										HoTTbinReader.recordSetGeneral = RecordSet.createRecordSet(recordSetName, device, 4, true, true);
										channel.put(recordSetName, HoTTbinReader.recordSetGeneral);
										HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GENRAL.value(), HoTTbinReader.recordSetGeneral);
										tmpRecordSet = channel.get(recordSetName);
										tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
										tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
										if (HoTTbinReader.application.getMenuToolBar() != null) {
											channel.applyTemplate(recordSetName, true);
										}
									}
									//recordSetGeneral initialized and ready to add data
									parseAddGeneral(HoTTbinReader.recordSetGeneral, HoTTbinReader.pointsGeneral, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4, HoTTbinReader.timeStep_ms);
									break;

								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
									++logCountElectric;
									//check if recordSetGeneral initialized, transmitter and receiver data always present, but not in the same data rate and signals
									if (HoTTbinReader.recordSetElectric == null) {
										channel = HoTTbinReader.channels.get(5);
										channel.setFileDescription(application.isObjectoriented() ? date + GDE.STRING_BLANK + application.getObjectKey() : date);
										recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ELECTRIC.value() + recordSetNameExtend;
										HoTTbinReader.recordSetElectric = RecordSet.createRecordSet(recordSetName, device, 5, true, true);
										channel.put(recordSetName, HoTTbinReader.recordSetElectric);
										HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.ELECTRIC.value(), HoTTbinReader.recordSetElectric);
										tmpRecordSet = channel.get(recordSetName);
										tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
										tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
										if (HoTTbinReader.application.getMenuToolBar() != null) {
											channel.applyTemplate(recordSetName, true);
										}
									}
									//recordSetElectric initialized and ready to add data
									parseAddElectric(HoTTbinReader.recordSetElectric, HoTTbinReader.pointsElectric, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4,	HoTTbinReader.timeStep_ms);
									break;
								}

								//skip last log count - 6 logs for speed up right after sensor switch
								int skipCount = 0;
								switch (actualSensor) {
								case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
								case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
									skipCount = lastLogCountVario - 3;
									logCountVario = skipCount;
									HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "lastLogCountVario = " + lastLogCountVario);
									break;
								case HoTTAdapter.SENSOR_TYPE_GPS_115200:
								case HoTTAdapter.SENSOR_TYPE_GPS_19200:
									skipCount = lastLogCountGPS - 4;
									logCountGPS = skipCount;
									HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "lastLogCountGPS = " + lastLogCountGPS);
									break;
								case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
								case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
									skipCount = lastLogCountGeneral - 5;
									logCountGeneral = skipCount;
									HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "lastLogCountGeneral = " + lastLogCountGeneral);
									break;
								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
									skipCount = lastLogCountElectric - 5;
									logCountElectric = skipCount;
									HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "lastLogCountElectric = " + lastLogCountElectric);
									break;
								}
								if (skipCount > 0) {
									data_in.skip(HoTTbinReader.dataBlockSize * skipCount);
									i += skipCount;
									HoTTbinReader.timeStep_ms = HoTTbinReader.timeStep_ms += (skipCount * 10);
								}
							}
							switch (lastSensor) {
							case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
							case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
								lastLogCountVario = logCountVario;
								break;
							case HoTTAdapter.SENSOR_TYPE_GPS_115200:
							case HoTTAdapter.SENSOR_TYPE_GPS_19200:
								lastLogCountGPS = logCountGPS;
								break;
							case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
							case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
								lastLogCountGeneral = logCountGeneral;
								break;
							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
								lastLogCountElectric = logCountElectric;
								break;
							}
							HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "logCountVario = " + logCountVario + " logCountGPS = " + logCountGPS
									+ " logCountGeneral = " + logCountGeneral + " logCountElectric = " + logCountElectric);
							lastSensor = actualSensor;
							logCountVario = logCountGPS = logCountGeneral = logCountElectric = 0;
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
							}
						}
					}
					//fill data block 0 to 4
					if (HoTTbinReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader.buf, 0) != 0) {
						System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf0, 0, HoTTbinReader.buf0.length);
					}
					if (HoTTbinReader.buf[33] == 1) {
						System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf1, 0, HoTTbinReader.buf1.length);
					}
					if (HoTTbinReader.buf[33] == 2) {
						System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf2, 0, HoTTbinReader.buf2.length);
					}
					if (HoTTbinReader.buf[33] == 3) {
						System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf3, 0, HoTTbinReader.buf3.length);
					}
					if (HoTTbinReader.buf[33] == 4) {
						System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf4, 0, HoTTbinReader.buf4.length);
					}

					// add default time step from log record of 10 msec
					HoTTbinReader.timeStep_ms += 10;

					if (menuToolBar != null && i % 100 == 0) HoTTbinReader.application.setProgress((int) (i * 100 / numberDatablocks), sThreadId);
				}
				else { //skip empty block, but add time step
					++countPackageLoss;
					HoTTbinReader.timeStep_ms += 10;
				}

				actualTime_ms = System.nanoTime() / 1000000;
				if (menuToolBar != null && (actualTime_ms - drawTime_ms) > 5000) {
					if (!isInitialSwitched) {
						HoTTbinReader.channels.switchChannel(channel.getName());
						device.updateVisibilityStatus(channel.getActiveRecordSet(), true);
						channel.switchRecordSet(recordSetName);
						isInitialSwitched = true;
					}
					else
						HoTTbinReader.application.updateGraphicsWindow();

					drawTime_ms = actualTime_ms;
					HoTTbinReader.log.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (actualTime_ms - startTime))); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			HoTTbinReader.log.logp(Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReader.log.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (actualTime_ms - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (menuToolBar != null) {
				for (RecordSet recordSet : HoTTAdapter.recordSets.values()) {
					device.makeInActiveDisplayable(recordSet);
					device.updateVisibilityStatus(recordSet, true);
				}

				if (!isInitialSwitched) {
					HoTTbinReader.channels.switchChannel(channel.getName());
					channel.switchRecordSet(recordSetName);
				}

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();

				HoTTbinReader.application.updateGraphicsWindow();
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
	 * @param _recordSetReceiver
	 * @param _pointsReceiver
	 * @param _buf
	 * @param _timeStep_ms
	 * @throws DataInconsitsentException
	 */
	private static void parseAddReceiver(RecordSet _recordSetReceiver, int[] _pointsReceiver, byte[] _buf, long _timeStep_ms) throws DataInconsitsentException {
		//0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 
		_pointsReceiver[0] = (_buf[34] & 0xFF) * 1000;
		_pointsReceiver[1] = (_buf[38] & 0xFF) * 1000;
		_pointsReceiver[2] = (convertRFRXSQ2Strenght(_buf[37] & 0xFF)) * 1000;
		_pointsReceiver[3] = DataParser.parse2Short(_buf, 40) * 1000;
		_pointsReceiver[4] = (_buf[3] & 0xFF) * 1000;
		_pointsReceiver[5] = (_buf[4] & 0xFF) * 1000;
		_pointsReceiver[6] = (_buf[35] & 0xFF) * 1000;
		_pointsReceiver[7] = (_buf[36] & 0xFF) * 1000;

		//printByteValues(_timeStep_ms, _buf);

		HoTTbinReader.recordSetReceiver.addPoints(HoTTbinReader.pointsReceiver, HoTTbinReader.timeStep_ms);
	}

	/**
	 * parse the buffered data from buffer 0 to 2 and add points to record set
	 * @param _recordSetVario
	 * @param _pointsVario
	 * @param sdLogVersion
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _timeStep_ms
	 * @throws DataInconsitsentException
	 */
	private static void parseAddVario(RecordSet _recordSetVario, int[] _pointsVario, int sdLogVersion, byte[] _buf0, byte[] _buf1, byte[] _buf2, long _timeStep_ms) throws DataInconsitsentException {
		switch (sdLogVersion) {
		case 0:
			//0=RXSQ, 1=Height, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
			_pointsVario[0] = (_buf1[0] & 0xFF) * 1000;
			_pointsVario[1] = DataParser.parse2Short(_buf1, 3) * 1000;
			//_pointsVario[0]max = DataParser.parse2Short(_buf1, 5) * 1000;
			//_pointsVario[0]min = DataParser.parse2Short(_buf1, 7) * 1000;
			_pointsVario[2] = DataParser.parse2UnsignedShort(_buf1[9], _buf2[0]) * 1000;
			_pointsVario[3] = DataParser.parse2UnsignedShort(_buf1[1], _buf2[2]) * 1000;
			_pointsVario[4] = DataParser.parse2UnsignedShort(_buf1[3], _buf2[4]) * 1000;
			_pointsVario[5] = (_buf0[1] & 0xFF) * 1000;
			_pointsVario[6] = (_buf0[2] & 0xFF) * 1000;
			break;
		case 1:
			//0=RXSQ, 1=Height, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
			_pointsVario[0] = (_buf0[4] & 0xFF) * 1000;
			int tmpHeight = DataParser.parse2Short(_buf1, 2);
			if (tmpHeight > 1 && tmpHeight < 5000) {
				_pointsVario[1] = tmpHeight * 1000;
				//pointsVarioMax = DataParser.parse2Short(buf1, 4) * 1000;
				//pointsVarioMin = DataParser.parse2Short(buf1, 6) * 1000;
				_pointsVario[2] = DataParser.parse2UnsignedShort(_buf1, 8) * 1000;
			}
			int tmpClimb10 = DataParser.parse2UnsignedShort(_buf2, 2);
			if (tmpClimb10 < 40000 && tmpClimb10 > 20000) {
				_pointsVario[3] = DataParser.parse2UnsignedShort(_buf2, 0) * 1000;
				_pointsVario[4] = tmpClimb10 * 1000;
			}
			_pointsVario[5] = (_buf0[1] & 0xFF) * 1000;
			_pointsVario[6] = (_buf0[2] & 0xFF) * 1000;
			break;
		}
		//printByteValues(_timeStep_ms, _buf0);
		//printShortValues(_timeStep_ms, _buf1);
		//printShortValues(_timeStep_ms, _buf2);
		//log.log(Level.FINEST, "");

		_recordSetVario.addPoints(_pointsVario, _timeStep_ms);
	}

	/**
		 * parse the buffered data from buffer 0 to 3 and add points to record set
	 * @param _recordSetGPS
	 * @param _pointsGPS
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _timeStep_ms
	 * @throws DataInconsitsentException
	 */
	private static void parseAddGPS(RecordSet _recordSetGPS, int[] _pointsGPS, byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, long _timeStep_ms) throws DataInconsitsentException {
		int tmpHeight = DataParser.parse2Short(_buf2, 8);
		int tmpClimb3 = (_buf3[2] & 0xFF);
		if (tmpClimb3 > 80 && tmpHeight > 1 && tmpHeight < 5000) {
			//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripLength, 10=VoltageRx, 11=TemperatureRx
			_pointsGPS[0] = (_buf0[4] & 0xFF) * 1000;
			_pointsGPS[1] = DataParser.parse2Short(_buf1, 7) * 10000 + DataParser.parse2Short(_buf1[9], _buf2[0]);
			_pointsGPS[1] = _buf1[6] == 1 ? -1 * _pointsGPS[1] : _pointsGPS[1];
			_pointsGPS[2] = DataParser.parse2Short(_buf2, 2) * 10000 + DataParser.parse2Short(_buf2, 4);
			_pointsGPS[2] = _buf2[1] == 1 ? -1 * _pointsGPS[2] : _pointsGPS[2];
			_pointsGPS[3] = tmpHeight * 1000;
			_pointsGPS[4] = DataParser.parse2UnsignedShort(_buf3, 0) * 1000;
			_pointsGPS[5] = tmpClimb3 * 1000;
			_pointsGPS[6] = DataParser.parse2Short(_buf1, 4) * 1000;
			_pointsGPS[7] = DataParser.parse2Short(_buf2, 6) * 1000;
			_pointsGPS[8] = (_buf1[3] & 0xFF) * 1000;
			_pointsGPS[9] = 0;
			_pointsGPS[10] = (_buf0[1] & 0xFF) * 1000;
			_pointsGPS[11] = (_buf0[2] & 0xFF) * 1000;
		}

		//		if (_buf3[2] < 0) {
		//printShortValues(timeStep_ms, buf0);
		//printShortValues(timeStep_ms, buf1);
		//printShortValues(timeStep_ms, buf2);
		//printShortValues(timeStep_ms, buf3);
		//			log.log(Level.FINEST, "");
		//		}

		_recordSetGPS.addPoints(_pointsGPS, _timeStep_ms);
	}

	/**
		 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * @param _recordSetGeneral
	 * @param _pointsGeneral
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 * @param _timeStep_ms
	 * @throws DataInconsitsentException
	 */
	private static void parseAddGeneral(RecordSet _recordSetGeneral, int[] _pointsGeneral, byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4, long _timeStep_ms)
			throws DataInconsitsentException {
		int tmpHeight = DataParser.parse2Short(_buf3, 0);
		int tmpClimb3 = (_buf3[4] & 0xFF);
		int tmpVoltage1 = DataParser.parse2Short(_buf1[9], _buf2[0]);
		int tmpVoltage2 = DataParser.parse2Short(_buf2, 1);
		int tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
		//0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Height, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2							
		if (tmpClimb3 > 80 && tmpHeight > 1 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600	&& tmpCapacity >= _pointsGeneral[3] / 1000) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			_pointsGeneral[0] = (_buf0[4] & 0xFF) * 1000;
			_pointsGeneral[1] = DataParser.parse2Short(_buf3, 7) * 1000;
			_pointsGeneral[2] = DataParser.parse2Short(_buf3, 5) * 1000;
			_pointsGeneral[3] = tmpCapacity * 1000;
			_pointsGeneral[4] = Double.valueOf(_pointsGeneral[1] / 1000.0 * _pointsGeneral[2]).intValue();
			_pointsGeneral[5] = 0;
			for (int j = 0; j < 6; j++) {
				_pointsGeneral[j + 6] = (_buf1[3 + j] & 0xFF) * 1000;
				if (_pointsGeneral[j + 6] > 0) {
					maxVotage = _pointsGeneral[j + 6] > maxVotage ? _pointsGeneral[j + 6] : maxVotage;
					minVotage = _pointsGeneral[j + 6] < minVotage ? _pointsGeneral[j + 6] : minVotage;
				}
			}
			_pointsGeneral[5] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
			_pointsGeneral[12] = DataParser.parse2Short(_buf2, 8) * 1000;
			_pointsGeneral[13] = tmpHeight * 1000;
			_pointsGeneral[14] = DataParser.parse2UnsignedShort(_buf3, 2) * 1000;
			_pointsGeneral[15] = tmpClimb3 * 1000;
			_pointsGeneral[16] = DataParser.parse2Short(_buf2, 6) * 1000;
			_pointsGeneral[17] = tmpVoltage1 * 1000;
			_pointsGeneral[18] = tmpVoltage2 * 1000;
			_pointsGeneral[19] = (_buf2[3] & 0xFF) * 1000;
			_pointsGeneral[20] = (_buf2[4] & 0xFF) * 1000;
		}

		//		if (DataParser.parse2Short(_buf1[9], _buf2[0]) > 200 || DataParser.parse2Short(_buf2[1], _buf2[2]) > 200) {
		//			printByteValues(timeStep_ms, buf0);
		//			printShortValues(timeStep_ms, buf1);
		//			printShortValues(timeStep_ms, buf2);
		//			printShortValues(timeStep_ms, buf3);
		//			printShortValues(timeStep_ms, buf4);
		//			log.log(Level.FINEST, "");
		//		}

		_recordSetGeneral.addPoints(_pointsGeneral, _timeStep_ms);
	}

	/**
		 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * @param _recordSetElectric
	 * @param _pointsElectric
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 * @param _timeStep_ms
	 * @throws DataInconsitsentException
	 */
	private static void parseAddElectric(RecordSet _recordSetElectric, int[] _pointsElectric, byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4, long _timeStep_ms)
			throws DataInconsitsentException {
		int tmpHeight = DataParser.parse2Short(_buf3, 3);
		int tmpClimb3 = (_buf4[3] & 0xFF);
		int tmpVoltage1 = DataParser.parse2Short(_buf2, 7);
		int tmpVoltage2 = DataParser.parse2Short(_buf2[9], _buf3[0]);
		int tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
		//0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2 
		if (tmpClimb3 > 80 && tmpHeight > 1 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600	&& tmpCapacity >= _pointsElectric[3] / 1000) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			_pointsElectric[0] = (_buf1[4] & 0xFF) * 1000;
			_pointsElectric[1] = DataParser.parse2Short(_buf3, 7) * 1000;
			_pointsElectric[2] = DataParser.parse2Short(_buf3, 5) * 1000;
			_pointsElectric[3] = tmpCapacity * 1000;
			_pointsElectric[4] = Double.valueOf(_pointsElectric[1] / 1000.0 * _pointsElectric[2]).intValue(); // power U*I [W];
			_pointsElectric[5] = 0; //5=Balance
			for (int j = 0; j < 7; j++) {
				_pointsElectric[j + 6] = (_buf1[3 + j] & 0xFF) * 1000;
				if (_pointsElectric[j + 6] > 0) {
					maxVotage = _pointsElectric[j + 6] > maxVotage ? _pointsElectric[j + 6] : maxVotage;
					minVotage = _pointsElectric[j + 6] < minVotage ? _pointsElectric[j + 6] : minVotage;
				}
			}
			for (int j = 0; j < 7; j++) {
				_pointsElectric[j + 13] = (_buf2[j] & 0xFF) * 1000;
				if (_pointsElectric[j + 13] > 0) {
					maxVotage = _pointsElectric[j + 13] > maxVotage ? _pointsElectric[j + 13] : maxVotage;
					minVotage = _pointsElectric[j + 13] < minVotage ? _pointsElectric[j + 13] : minVotage;
				}
			}
			//calculate balance on the fly
			_pointsElectric[5] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
			_pointsElectric[20] = tmpHeight * 1000;
			_pointsElectric[21] = DataParser.parse2UnsignedShort(_buf4, 1) * 1000;
			_pointsElectric[22] = tmpClimb3 * 1000;
			_pointsElectric[23] = tmpVoltage1 * 1000;
			_pointsElectric[24] = tmpVoltage2 * 1000;
			_pointsElectric[25] = (_buf3[1] & 0xFF) * 1000;
			_pointsElectric[26] = (_buf3[2] & 0xFF) * 1000;
		}

		_recordSetElectric.addPoints(_pointsElectric, _timeStep_ms);
	}

	static void printByteValues(long millisec, byte[] buffer) {
		StringBuilder sb = new StringBuilder().append(StringHelper.getFormatedTime("mm:ss:SSS", millisec)).append(" : ");
		for (int i = 0; buffer != null && i < buffer.length; i++) {
			sb.append("(").append(i).append(")").append(buffer[i]).append(GDE.STRING_BLANK);
		}
		HoTTbinReader.log.log(Level.OFF, sb.toString());
	}

	static void printShortValues(long millisec, byte[] buffer) {
		StringBuilder sb = new StringBuilder().append(StringHelper.getFormatedTime("mm:ss:SSS", millisec)).append(" : ");
		for (int i = 0; buffer != null && i < buffer.length - 1; i++) {
			sb.append("(").append(i).append(")").append(DataParser.parse2Short(buffer, i)).append(GDE.STRING_BLANK);
		}
		HoTTbinReader.log.log(Level.OFF, sb.toString());
	}

	/**
	 * main method for test purpose only !
	 * @param args
	 */
	public static void main(String[] args) {
		String directory = "f:\\Documents\\DataExplorer\\HoTTAdapter\\";

		try {
			List<File> files = FileUtils.getFileListing(new File(directory));
			for (File file : files) {
				if (!file.isDirectory() && file.getName().endsWith(".bin")) {
					FileInputStream file_input = new FileInputStream(file);
					DataInputStream data_in = new DataInputStream(file_input);
					HoTTbinReader.buf = new byte[64];
					data_in.read(HoTTbinReader.buf);
					System.out.println(file.getName());
					System.out.println(StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
					System.out.println(StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
					data_in.close();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
