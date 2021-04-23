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
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2011,2012,2013,2014,2015,2016,2017,2018,2019,2020 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.logging.Logger;

import gde.GDE;
import gde.data.Channel;
import gde.data.RecordSet;
import gde.device.DataTypes;
import gde.device.IDevice;
import gde.device.graupner.HoTTAdapter.PickerParameters;
import gde.device.graupner.HoTTAdapter.Sensor;
import gde.exception.DataInconsitsentException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;

/**
 * Class to read Graupner HoTT binary data as saved on SD-Cards
 * @author Winfried Brügmann
 */
public class HoTTlogReader2 extends HoTTlogReader {
	final static Logger	log					= Logger.getLogger(HoTTlogReader2.class.getName());
	static int[]				points;
	static RecordSet		recordSet;
	static boolean			isJustMigrated	= false;

	/**
	* read log data according to version 0
	* @param filePath
	* @param newPickerParameters
	* @throws IOException
	* @throws DataInconsitsentException
	*/
	public static synchronized void read(String filePath, PickerParameters newPickerParameters) throws Exception {
		final String $METHOD_NAME = "read";
		HoTTlogReader.pickerParameters = newPickerParameters;
		HashMap<String, String> fileInfoHeader = getFileInfo(new File(filePath), newPickerParameters);
		HoTTlogReader.detectedSensors = Sensor.getSetFromDetected(fileInfoHeader.get(HoTTAdapter.DETECTED_SENSOR));

		final File file = new File(fileInfoHeader.get(HoTTAdapter.FILE_PATH));
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		HoTTAdapter2 device = (HoTTAdapter2) HoTTbinReader.application.getActiveDevice();
		int recordSetNumber = HoTTbinReader.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		int channelNumber = HoTTlogReader.pickerParameters.analyzer.getActiveChannel().getNumber();
		boolean isReceiverData = false;
		boolean isVarioData = false;
		boolean isGPSData = false;
		boolean isGeneralData = false;
		boolean isElectricData = false;
		boolean isMotorDriverData = false;
		boolean isGPSdetected = false;
		HoTTlogReader2.recordSet = null;
		HoTTlogReader2.isJustMigrated = false;
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		// 15=Latitude, 16=Longitude, 17=Velocity, 18=Distance, 19=Direction, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		// 24=HomeDirection 25=Roll 26=Pitch 27=Yaw 28=GyroX 29=GyroY 30=GyroZ 31=Vibration 32=Version	
		// 33=Voltage G, 34=Current G, 35=Capacity G, 36=Power G, 37=Balance G, 38=CellVoltage G1, 39=CellVoltage G2 .... 43=CellVoltage G6,
		// 44=Revolution G, 45=FuelLevel, 46=Voltage G1, 47=Voltage G2, 48=Temperature G1, 49=Temperature G2 50=Speed G, 51=LowestCellVoltage,
		// 52=LowestCellNumber, 53=Pressure, 54=Event G
		// 55=Voltage E, 56=Current E, 57=Capacity E, 58=Power E, 59=Balance E, 60=CellVoltage E1, 61=CellVoltage E2 .... 73=CellVoltage E14,
		// 74=Voltage E1, 75=Voltage E2, 76=Temperature E1, 77=Temperature E2 78=Revolution E 79=MotorTime 80=Speed 81=Event E
		// 82=VoltageM, 83=CurrentM, 84=CapacityM, 85=PowerM, 86=RevolutionM, 87=TemperatureM 1, 88=TemperatureM 2 89=Voltage_min, 90=Current_max,
		// 91=Revolution_max, 92=Temperature1_max, 93=Temperature2_max 94=Event M

		// 82=Ch 1, 83=Ch 2, 84=Ch 3 .. 97=Ch 16, 98=PowerOff, 99=BatterieLow, 100=Reset, 101=reserve
		// 102=VoltageM, 103=CurrentM, 104=CapacityM, 105=PowerM, 106=RevolutionM, 107=TemperatureM 1, 108=TemperatureM 2 109=Voltage_min, 110=Current_max,
		// 111=Revolution_max, 112=Temperature1_max, 113=Temperature2_max 114=Event M
		HoTTlogReader2.points = new int[device.getNumberOfMeasurements(channelNumber)];
		HoTTbinReader.timeStep_ms = 0;
		int numberLogChannels = Integer.valueOf(fileInfoHeader.get("LOG NOB CHANNEL"));
		HoTTbinReader.dataBlockSize = 66 + numberLogChannels * 2;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		int[] valuesChannel = new int[23];
		int[] valuesVario = new int[8];
		valuesVario[2] = 100000;
		int[] valuesGPS = new int[24];
		int[] valuesGAM = new int[26];
		int[] valuesEAM = new int[31];
		int[] valuesESC = new int[14];
		int logTimeStep = 1000/Integer.valueOf(fileInfoHeader.get("COUNTER").split("/")[1].split(GDE.STRING_BLANK)[0]);
		PackageLossDeque reverseChannelPackageLossCounter = new PackageLossDeque(logTimeStep);
		HoTTbinReader.isTextModusSignaled = false;
		int countPackageLoss = 0;
		int logDataOffset = Integer.valueOf(fileInfoHeader.get("LOG DATA OFFSET"));
		long numberDatablocks = (fileSize - logDataOffset) / HoTTbinReader.dataBlockSize;
		long startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(fileInfoHeader.get("LOG START TIME"), HoTTbinReader.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks));
		String date = StringHelper.getDate();
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		MenuToolBar menuToolBar = HoTTbinReader.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		GDE.getUiNotification().setProgress(0);

		try {
			//receiver data are always contained
			channel = HoTTbinReader.channels.get(channelNumber);
			channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
			recordSetName = recordSetNumber + device.getRecordSetStemNameReplacement() + recordSetNameExtend;
			HoTTlogReader2.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true, true);
			channel.put(recordSetName, HoTTlogReader2.recordSet);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			//recordSet initialized and ready to add data

			//read all the data blocks from the file and parse
			data_in.skip(logDataOffset);
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (log.isLoggable(Level.FINE)) {
					log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				//Ph(D)[4], Evt1(H)[5], Evt2(D)[6], Fch(D)[7], TXdBm(-D)[8], RXdBm(-D)[9], RfRcvRatio(D)[10], TrnRcvRatio(D)[11]
				//STATUS : Ph(D)[4], Evt1(H)[5], Evt2(D)[6], Fch(D)[7], TXdBm(-D)[8], RXdBm(-D)[9], RfRcvRatio(D)[10], TrnRcvRatio(D)[11]
				//S.INFOR : DEV(D)[22], CH(D)[23], SID(H)[24], WARN(H)[25]
				//if (!HoTTAdapter.isFilterTextModus || (HoTTbinReader.buf[6] & 0x01) == 0) { //switch into text modus
				if (HoTTbinReader.buf[8] != 0 && HoTTbinReader.buf[9] != 0) { //buf 8, 9, tx,rx, rx sensitivity data
					if (HoTTbinReader.buf[24] != 0x1F) {//rx sensitivity data
						if (log.isLoggable(Level.FINE)) {
							log.log(Level.FINE, String.format("Sensor %02X", HoTTbinReader.buf[26]));
						}
					}
					reverseChannelPackageLossCounter.add(1);
					HoTTlogReader2.points[0] = reverseChannelPackageLossCounter.getPercentage() * 1000;
					//create and fill sensor specific data record sets
					if (log.isLoggable(Level.FINEST)) {
						log.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME,
								StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));
					}

					//fill receiver data
					if (HoTTbinReader.buf[24] != 0x1F) { //receiver sensitive data
						isReceiverData = parseReceiver(HoTTbinReader.buf, HoTTlogReader2.points);
					}

					if (channelNumber == 4) {
						parseChannel(HoTTbinReader.buf, valuesChannel, numberLogChannels);
						//in 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16 19=PowerOff 20=BattLow 21=Reset 22=Warning
						//out 82=Ch 1, 83=Ch 2, 84=Ch 3 .. 97=Ch 16, 98=PowerOff, 99=BatterieLow, 100=Reset, 101=reserve
						System.arraycopy(valuesChannel, 3, HoTTlogReader2.points, 82, 20); //copy channel data and events, warning
					}

					switch ((byte) (HoTTbinReader.buf[26] & 0xFF)) { //actual sensor
					case HoTTAdapter.ANSWER_SENSOR_VARIO_19200:
						isVarioData = parseVario(HoTTbinReader.buf, valuesVario, true);
						if (isVarioData && isReceiverData) {
							migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber, valuesVario, valuesGPS, valuesGAM, valuesEAM, valuesESC);
							isReceiverData = false;
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_GPS_19200:
						isGPSData = parseGPS(HoTTbinReader.buf, valuesGPS, true);
						if (isGPSData && isReceiverData) {
							migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber, valuesVario, valuesGPS, valuesGAM, valuesEAM, valuesESC);
							isReceiverData = false;
						}
						if (isGPSData && !isGPSdetected) {
							if ((HoTTbinReader.buf[65] & 0xFF) > 100) { //SM GPS-Logger
								// 24=HomeDirection 25=Roll 26=Pitch 27=Yaw 28=GyroX 29=GyroY 30=GyroZ 31=Vibration 32=Version	
								tmpRecordSet.get(25).setName(device.getMeasurementReplacement("servo_impulse") + " GPS");
								tmpRecordSet.get(25).setUnit("");
								tmpRecordSet.get(28).setName(device.getMeasurementReplacement("acceleration") + " X");
								tmpRecordSet.get(28).setUnit("g");
								tmpRecordSet.get(28).setFactor(0.01);
								tmpRecordSet.get(29).setName(device.getMeasurementReplacement("acceleration") + " Y");
								tmpRecordSet.get(29).setUnit("g");
								tmpRecordSet.get(29).setFactor(0.01);
								tmpRecordSet.get(29).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 28); //$NON-NLS-1$
								tmpRecordSet.get(30).setName(device.getMeasurementReplacement("acceleration") + " Z");
								tmpRecordSet.get(30).setUnit("g");
								tmpRecordSet.get(30).setFactor(0.01);
								tmpRecordSet.get(30).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 28); //$NON-NLS-1$
								tmpRecordSet.get(31).setName("ENL");
								tmpRecordSet.get(31).setUnit("");
							}
							else if ((HoTTbinReader.buf[65] & 0xFF) == 4) { //RC Electronics Sparrow
								tmpRecordSet.get(25).setName(device.getMeasurementReplacement("servo_impulse") + " GPS");
								tmpRecordSet.get(25).setUnit("%");
								tmpRecordSet.get(27).setName(device.getMeasurementReplacement("voltage") + " GPS");
								tmpRecordSet.get(27).setUnit("V");
								tmpRecordSet.get(28).setName(device.getMeasurementReplacement("time") + " GPS");
								tmpRecordSet.get(28).setUnit("HH:mm:ss.SSS");
								tmpRecordSet.get(28).setFactor(1.0);
								tmpRecordSet.get(29).setName(device.getMeasurementReplacement("date") + " GPS");
								tmpRecordSet.get(29).setUnit("yy-MM-dd");
								tmpRecordSet.get(29).setFactor(1.0);
								tmpRecordSet.get(30).setName(device.getMeasurementReplacement("altitude") + " MSL");
								tmpRecordSet.get(30).setUnit("m");
								tmpRecordSet.get(30).setFactor(1.0);
								tmpRecordSet.get(31).setName("ENL");
								tmpRecordSet.get(31).setUnit("%");
								tmpRecordSet.setStartTimeStamp(HoTTbinReader.getStartTimeStamp(tmpRecordSet.getStartTimeStamp(), tmpRecordSet.get(28).lastElement(), 0));
							}
							else if ((HoTTbinReader.buf[65] & 0xFF) == 1) { //Graupner GPS #1= 33602/S8437,
								tmpRecordSet.get(25).setName("velNorth");
								tmpRecordSet.get(25).setUnit("mm/s");
								tmpRecordSet.get(27).setName("speedAcc");
								tmpRecordSet.get(27).setUnit("cm/s");
								tmpRecordSet.get(28).setName(device.getMeasurementReplacement("time") + " GPS");
								tmpRecordSet.get(28).setUnit("HH:mm:ss.SSS");
								tmpRecordSet.get(28).setFactor(1.0);
//								tmpRecordSet.get(29).setName("GPS ss.SSS");
//								tmpRecordSet.get(29).setUnit("ss.SSS");
//								tmpRecordSet.get(29).setFactor(1.0);
								tmpRecordSet.get(30).setName("velEast");
								tmpRecordSet.get(30).setUnit("mm/s");
								tmpRecordSet.get(30).setFactor(1.0);
								tmpRecordSet.get(31).setName("HDOP");
								tmpRecordSet.get(31).setUnit("dm");
								tmpRecordSet.setStartTimeStamp(HoTTbinReader.getStartTimeStamp(tmpRecordSet.getStartTimeStamp(), tmpRecordSet.get(28).lastElement(), 0));
							}
							else { //Graupner GPS #0=GPS #33600
								tmpRecordSet.get(28).setName(device.getMeasurementReplacement("time") + " GPS");
								tmpRecordSet.get(28).setUnit("HH:mm:ss.SSS");
								tmpRecordSet.get(28).setFactor(1.0);
//								tmpRecordSet.get(29).setName("GPS ss.SSS");
//								tmpRecordSet.get(29).setUnit("ss.SSS");
//								tmpRecordSet.get(29).setFactor(1.0);
								tmpRecordSet.get(30).setName(device.getMeasurementReplacement("altitude") + " MSL");
								tmpRecordSet.get(30).setUnit("m");
								tmpRecordSet.get(30).setFactor(1.0);
								tmpRecordSet.setStartTimeStamp(HoTTbinReader.getStartTimeStamp(tmpRecordSet.getStartTimeStamp(), tmpRecordSet.get(28).lastElement(), 0));
							}
							isGPSdetected = true;					
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_GENERAL_19200:
						isGeneralData = parseGAM(HoTTbinReader.buf, valuesGAM, HoTTlogReader2.recordSet, true);
						if (isGeneralData && isReceiverData) {
							migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber, valuesVario, valuesGPS, valuesGAM, valuesEAM, valuesESC);
							isReceiverData = false;
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_ELECTRIC_19200:
						isElectricData = parseEAM(HoTTbinReader.buf, valuesEAM, HoTTlogReader2.recordSet, true);
						if (isElectricData && isReceiverData) {
							migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber, valuesVario, valuesGPS, valuesGAM, valuesEAM, valuesESC);
							isReceiverData = false;
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_MOTOR_DRIVER_19200:
						isMotorDriverData = parseESC(HoTTbinReader.buf, valuesESC, HoTTlogReader2.recordSet);
						if (isMotorDriverData && isReceiverData) {
							migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber, valuesVario, valuesGPS, valuesGAM, valuesEAM, valuesESC);
							isReceiverData = false;
						}
						break;
					case 0x1F: //receiver sensitive data
					default:
						break;
					}

					if (HoTTlogReader2.isJustMigrated && countPackageLoss > 0) {
						HoTTbinReader.lostPackages.add(countPackageLoss * 10);
						countPackageLoss = 0;
					}

					if (isReceiverData) { //this will only be true if no other sensor is connected
						HoTTlogReader2.recordSet.addPoints(HoTTlogReader2.points, HoTTbinReader.timeStep_ms);
						isReceiverData = false;
					}
					else if (channelNumber == 4 && !HoTTlogReader2.isJustMigrated) { //this will only be true if no other sensor is connected and channel 4
						HoTTlogReader2.recordSet.addPoints(HoTTlogReader2.points, HoTTbinReader.timeStep_ms);
					}
					HoTTlogReader2.isJustMigrated = false;

					HoTTbinReader.timeStep_ms += logTimeStep;// add default time step from log record of 100 msec

					if (i % progressIndicator == 0) GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));
				}
					else { //skip empty block, but add time step
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

						reverseChannelPackageLossCounter.add(0);
						HoTTlogReader2.points[0] = reverseChannelPackageLossCounter.getPercentage() * 1000;
						countPackageLoss+=1; // add up lost packages in telemetry data

						if (channelNumber == 4) {
							parseChannel(HoTTbinReader.buf, valuesChannel, numberLogChannels);
							//in 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16 19=PowerOff 20=BattLow 21=Reset 22=Warning
							//out 82=Ch 1, 83=Ch 2, 84=Ch 3 .. 97=Ch 16, 98=PowerOff, 99=BatterieLow, 100=Reset, 101=reserve
							System.arraycopy(valuesChannel, 3, HoTTlogReader2.points, 82, 20); //copy channel data and events, warning
							HoTTlogReader2.recordSet.addPoints(HoTTlogReader2.points, HoTTbinReader.timeStep_ms);
						}
						HoTTbinReader.timeStep_ms += logTimeStep;
						//reset buffer to avoid mixing data >> 20 Jul 14, not any longer required due to protocol change requesting next sensor data block
						//logCountVario = logCountGPS = logCountGeneral = logCountElectric = logCountMotorDriver = 0;
					}
//				} //switch into text modus - telemetry menu
//				else if (!HoTTbinReader.isTextModusSignaled) {
//					HoTTbinReader.isTextModusSignaled = true;
//					HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
//				}
			}
			String packageLossPercentage = tmpRecordSet.getRecordDataSize(true) > 0 ? String.format("%.1f", (countPackageLoss / tmpRecordSet.getTime_ms(tmpRecordSet.getRecordDataSize(true) - 1) * 1000)) : "100";
			tmpRecordSet.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] { countPackageLoss, packageLossPercentage, HoTTbinReader.lostPackages.getStatistics() })
					+ Sensor.getSetAsSignature(HoTTbinReader.detectedSensors));
			log.logp(Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			log.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (menuToolBar != null) {
				GDE.getUiNotification().setProgress(99);
				device.makeInActiveDisplayable(HoTTlogReader2.recordSet);
				device.updateVisibilityStatus(HoTTlogReader2.recordSet, true);
				channel.applyTemplate(recordSetName, false);

				//write filename after import to record description
				HoTTlogReader2.recordSet.descriptionAppendFilename(file.getName());

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				GDE.getUiNotification().setProgress(100);
			}
		}
		finally {
			data_in.close();
			data_in = null;
		}
	}

	/**
	 * migrate sensor measurement values and add to record set, receiver data are always updated
	 * @param isVarioData
	 * @param isGPSData
	 * @param isGeneralData
	 * @param isElectricData
	 * @param isMotorDriverData
	 * @param channelNumber
	 * @throws DataInconsitsentException
	 */
	public static void migrateAddPoints(boolean isVarioData, boolean isGPSData, boolean isGeneralData, boolean isElectricData, boolean isMotorDriverData, int channelNumber,
			int[] valuesVario, int[] valuesGPS, int[] valuesGAM, int[] valuesEAM, int[] valuesESC)
			throws DataInconsitsentException {
		//receiver data gets integrated each cycle
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		// 15=Latitude, 16=Longitude, 17=Velocity, 18=Distance, 19=Direction, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		// 24=HomeDirection 25=Roll 26=Pitch 27=Yaw 28=GyroX 29=GyroY 30=GyroZ 31=Vibration 32=Version	
		// 33=Voltage G, 34=Current G, 35=Capacity G, 36=Power G, 37=Balance G, 38=CellVoltage G1, 39=CellVoltage G2 .... 43=CellVoltage G6,
		// 44=Revolution G, 45=FuelLevel, 46=Voltage G1, 47=Voltage G2, 48=Temperature G1, 49=Temperature G2 50=Speed G, 51=LowestCellVoltage,
		// 52=LowestCellNumber, 53=Pressure, 54=Event G
		// 55=Voltage E, 56=Current E, 57=Capacity E, 58=Power E, 59=Balance E, 60=CellVoltage E1, 61=CellVoltage E2 .... 73=CellVoltage E14,
		// 74=Voltage E1, 75=Voltage E2, 76=Temperature E1, 77=Temperature E2 78=Revolution E 79=MotorTime 80=Speed 81=Event E
		// 82=VoltageM, 83=CurrentM, 84=CapacityM, 85=PowerM, 86=RevolutionM, 87=TemperatureM 1, 88=TemperatureM 2 89=Voltage_min, 90=Current_max,
		// 91=Revolution_max, 92=Temperature1_max, 93=Temperature2_max 94=Event M

		// 82=Ch 1, 83=Ch 2, 84=Ch 3 .. 97=Ch 16, 98=PowerOff, 99=BatterieLow, 100=Reset, 101=reserve
		// 102=VoltageM, 103=CurrentM, 104=CapacityM, 105=PowerM, 106=RevolutionM, 107=TemperatureM 1, 108=TemperatureM 2 109=Voltage_min, 110=Current_max,
		// 111=Revolution_max, 112=Temperature1_max, 113=Temperature2_max 114=Event M

		//in 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14,
		//in 20=Altitude, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2 27=RPM 28=MotorTime 29=Speed 30=Event
		if (isElectricData) {
			//out 10=Altitude, 11=Climb 1, 12=Climb 3
			for (int j = 0; !isVarioData && !isGPSData && !isGeneralData && j < 3; j++) { //0=altitude 1=climb1 2=climb3
				HoTTlogReader2.points[j+10] = valuesEAM[j+20];
			}
			//out 55=Voltage E, 56=Current E, 57=Capacity E, 58=Power E, 59=Balance E, 60=CellVoltage E1, 61=CellVoltage E2 .... 73=CellVoltage E14,
			for (int k = 0; k < 19; k++) { //3=voltage 4=current 5=capacity...
				HoTTlogReader2.points[k+55] = valuesEAM[k+1];
			}
			//out 74=Voltage E1, 75=Voltage E2, 76=Temperature E1, 77=Temperature E2 78=Revolution E 79=MotorTime 80=Speed 81=Event E
			for (int k = 0; k < 8; k++) { //3=voltage 4=current 5=capacity...
				HoTTlogReader2.points[k+74] = valuesEAM[k+23];
			}
		}
		//in 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6,
		//in 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		//in 21=Speed, 22=LowestCellVoltage, 23=LowestCellNumber, 24=Pressure, 24=Event
		if (isGeneralData) {
			//out 10=Altitude, 11=Climb 1, 12=Climb 3
			for (int k = 0; !isVarioData && !isGPSData && k < 3; k++) {
				HoTTlogReader2.points[k+10] = valuesGAM[k+13];
			}
			//out 33=Voltage G, 34=Current G, 35=Capacity G, 36=Power G, 37=Balance G, 38=CellVoltage G1, 39=CellVoltage G2 .... 43=CellVoltage G6, 44=Revolution G
			for (int j = 0; j < 12; j++) {
				HoTTlogReader2.points[j+33] = valuesGAM[j+1];
			}
			//out 45=FuelLevel, 46=Voltage G1, 47=Voltage G2, 48=Temperature G1, 49=Temperature G2 50=Speed G, 51=LowestCellVoltage, 52=LowestCellNumber, 53=Pressure, 54=Event G
			for (int j = 0; !isVarioData && !isGPSData && j < 10; j++) {
				HoTTlogReader2.points[j+45] = valuesGAM[j+16];
			}
		}
		//in 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripLength, 10=VoltageRx, 11=TemperatureRx 12=satellites 13=GPS-fix 14=EventGPS
		if (isGPSData) {
			//out 10=Altitude, 11=Climb 1, 12=Climb 3
			for (int j = 0; !isVarioData && j < 3; j++) { //0=altitude 1=climb1 2=climb3
				HoTTlogReader2.points[j+10] = valuesGPS[j+3];
			}
			//out 15=Latitude, 16=Longitude, 17=Velocity, 18=Distance, 19=Direction, 20=TripDistance
			HoTTlogReader2.points[15] = valuesGPS[1];
			HoTTlogReader2.points[16] = valuesGPS[2];
			for (int k = 0; k < 4; k++) {
				HoTTlogReader2.points[k+17] = valuesGPS[k+6];
			}
			//out 21=NumSatellites 22=GPS-Fix 23=EventGPS
			for (int k = 0; k < 3; k++) {
				HoTTlogReader2.points[k+21] = valuesGPS[k+12];
			}
			// 24=HomeDirection 25=Roll 26=Pitch 27=Yaw 28=GyroX 29=GyroY 30=GyroZ 31=Vibration 32=Version	
			for (int k = 0; k < 9; k++) {
				HoTTlogReader2.points[k+24] = valuesGPS[k+15];
			}
		}
		//in 0=RXSQ, 1=Altitude, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx	7=EventVario
		if (isVarioData) {
			//out 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
			for (int j = 0; j < 4; j++) {
				HoTTlogReader2.points[j+10] = valuesVario[j+1];
			}
			HoTTlogReader2.points[14] = valuesVario[7];
		}
		//in 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature1, 7=Temperature2
		//in 8=Voltage_min, 9=Current_max, 10=Revolution_max, 11=Temperature1_max, 12=Temperature2_max 13=Event
		if (isMotorDriverData) {
			if (channelNumber == 4)
				//out 102=VoltageM, 103=CurrentM, 104=CapacityM, 105=PowerM, 106=RevolutionM, 107=TemperatureM 1, 108=TemperatureM 2 109=Voltage_min, 110=Current_max, 111=Revolution_max, 112=Temperature1_max, 113=Temperature2_max 114=Event M
				for (int j = 0; j < 13; j++) {
					HoTTlogReader2.points[j+102] = valuesESC[j+1];
				}
			else
				//out 82=VoltageM, 83=CurrentM, 84=CapacityM, 85=PowerM, 86=RevolutionM, 87=TemperatureM 1, 88=TemperatureM 2 89=Voltage_min, 90=Current_max, 91=Revolution_max, 92=Temperature1_max, 93=Temperature2_max 94=Event M
				for (int j = 0; j < 13; j++) {
					HoTTlogReader2.points[j+82] = valuesESC[j+1];
				}
		}
		
		//add altitude and climb values from selected sensor
		//log.log(Level.OFF, String.format("pickerParameters.altitudeClimbSensorSelection = %s", pickerParameters.altitudeClimbSensorSelection));
		switch (Sensor.VALUES[HoTTbinReader.pickerParameters.altitudeClimbSensorSelection]) {
		case VARIO:
			//8=Altitude, 9=Climb 1, 10=Climb 3, 11=Climb 10
			if (isVarioData)
				for (int j = 0; j < 4; j++) {
					HoTTlogReader2.points[j+10] = valuesVario[j+1];
				}
			break;
		case GPS:
			//8=Altitude, 9=Climb 1, 10=Climb 3
			if (isGPSData)
				for (int j = 0; j < 3; j++) { //0=altitude 1=climb1 2=climb3
					HoTTlogReader2.points[j+10] = valuesGPS[j+3];
				}
			HoTTlogReader2.points[11] = 0;
			break;
		case GAM:
			//8=Altitude, 9=Climb 1, 10=Climb 3
			if (isGeneralData)
				for (int j = 0; j < 3; j++) {
					HoTTlogReader2.points[j+10] = valuesGAM[j+13];
				}
			HoTTlogReader2.points[11] = 0;
			break;
		case EAM:
			//8=Altitude, 9=Climb 1, 10=Climb 3
			if (isElectricData)
				for (int j = 0; j < 3; j++) { //0=altitude 1=climb1 2=climb3
					HoTTlogReader2.points[j+10] = valuesEAM[j+20];
				}
			HoTTlogReader2.points[11] = 0;
			break;
		default:
			break;
		}
		
		HoTTlogReader2.recordSet.addPoints(HoTTlogReader2.points, HoTTbinReader.timeStep_ms);
		HoTTlogReader2.isJustMigrated = true;
	}
}
