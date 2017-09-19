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

import gde.GDE;
import gde.data.Channel;
import gde.data.RecordSet;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Class to read Graupner HoTT binary data as saved on SD-Cards 
 * @author Winfried Brügmann
 */
public class HoTTlogReader2 extends HoTTbinReader {
	final static Logger	logger					= Logger.getLogger(HoTTlogReader2.class.getName());
	static int[]				points;
	static RecordSet		recordSet;
	static boolean			isJustMigrated	= false;

	/**
	* read log data according to version 0
	* @param file
	* @param data_in
	* @throws IOException 
	* @throws DataInconsitsentException 
	*/
	public static synchronized void read(String filePath) throws Exception {
		final String $METHOD_NAME = "readMultiple";
		HashMap<String, String> fileInfoHeader = getFileInfo(new File(filePath));
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
		int channelNumber = device.getLastChannelNumber();
		boolean isReceiverData = false;
		boolean isVarioData = false;
		boolean isGPSData = false;
		boolean isGeneralData = false;
		boolean isElectricData = false;
		boolean isMotorDriverData = false;
		HoTTlogReader2.recordSet = null;
		HoTTlogReader2.isJustMigrated = false;
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
		//46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14, 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
		//73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max, 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M

		//73=Ch 1, 74=Ch 2, 75=Ch 3 .. 88=Ch 16, 89=PowerOff, 90=BatterieLow, 91=Reset, 92=reserve
		//93=VoltageM, 94=CurrentM, 95=CapacityM, 96=PowerM, 97=RevolutionM, 98=TemperatureM 1, 99=TemperatureM 2 100=Voltage_min, 101=Current_max, 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M
		HoTTlogReader2.points = new int[device.getNumberOfMeasurements(channelNumber)];
		HoTTbinReader.timeStep_ms = 0;
		int numberLogChannels = Integer.valueOf(fileInfoHeader.get("LOG NOB CHANNEL"));
		HoTTbinReader.dataBlockSize = 66 + numberLogChannels * 2;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		int[] valuesChannel = new int[40];
		int[] valuesVario = new int[20];
		valuesVario[2] = 100000;
		int[] valuesGPS = new int[12];
		int[] valuesGAM = new int[25];
		int[] valuesEAM = new int[30];
		int[] valuesESC = new int[14];
		byte lastWarningDetected = 0;
		int warningKeepCounter = 1;
		int logTimeStep = 1000/Integer.valueOf(fileInfoHeader.get("COUNTER").split("/")[1].split(GDE.STRING_BLANK)[0]);
		HoTTAdapter.reverseChannelPackageLossCounter.clear();
		HoTTbinReader.lostPackages.clear();
		HoTTbinReader.countLostPackages = 0;
		HoTTbinReader.isTextModusSignaled = false;
		int countPackageLoss = 0;
		int logDataOffset = Integer.valueOf(fileInfoHeader.get("LOG DATA OFFSET"));
		long numberDatablocks = (fileSize - logDataOffset) / HoTTbinReader.dataBlockSize;
		long startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(fileInfoHeader.get("LOG START TIME"), HoTTbinReader.getStartTimeStamp(file, numberDatablocks));
		String date = StringHelper.getDate();
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		MenuToolBar menuToolBar = HoTTbinReader.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		if (menuToolBar != null) HoTTbinReader.application.setProgress(0, sThreadId);

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
			if (HoTTbinReader.application.getMenuToolBar() != null) {
				channel.applyTemplate(recordSetName, false);
			}
			//recordSet initialized and ready to add data

			//read all the data blocks from the file and parse
			data_in.skip(logDataOffset);
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTlogReader2.logger.isLoggable(Level.FINE)) {
					HoTTlogReader2.logger.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}
				
				//Ph(D)[4], Evt1(H)[5], Evt2(D)[6], Fch(D)[7], TXdBm(-D)[8], RXdBm(-D)[9], RfRcvRatio(D)[10], TrnRcvRatio(D)[11]				
				//STATUS : Ph(D)[4], Evt1(H)[5], Evt2(D)[6], Fch(D)[7], TXdBm(-D)[8], RXdBm(-D)[9], RfRcvRatio(D)[10], TrnRcvRatio(D)[11]
				//S.INFOR : DEV(D)[22], CH(D)[23], SID(H)[24], WARN(H)[25]
				if (channelNumber == 4) {
					//check for event character
					if ((HoTTbinReader.buf[25] & 0x7F) != 0 && warningKeepCounter <= 0 || (HoTTbinReader.buf[25] != 0 && HoTTbinReader.buf[25] != lastWarningDetected)) {
						warningKeepCounter = logTimeStep / 10; //keep detected warning to make it visible in graphics
						if (log.isLoggable(Level.OFF)) {
							log.log(Level.OFF, String.format("Event '%c' detected", (lastWarningDetected = (byte) (HoTTbinReader.buf[25] & 0x7F)) + 64));
						}
					}
					else {
						--warningKeepCounter;
					}
				}
				//			if (!HoTTAdapter.isFilterTextModus || (HoTTbinReader.buf[6] & 0x01) == 0) { //switch into text modus
				if (HoTTbinReader.buf[8] != 0 && HoTTbinReader.buf[9] != 0) { //buf 8, 9, tx,rx, rx sensitivity data
					if (HoTTbinReader.buf[24] != 0x1F) {//rx sensitivity data
						if (HoTTlogReader2.logger.isLoggable(Level.FINE)) {
							HoTTlogReader2.logger.log(Level.FINE, String.format("Sensor %02X", HoTTbinReader.buf[26]));
						}
					}
					HoTTAdapter.reverseChannelPackageLossCounter.add(1);
					HoTTlogReader2.points[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
					//create and fill sensor specific data record sets 
					if (HoTTlogReader2.logger.isLoggable(Level.FINEST)) {
						HoTTlogReader2.logger.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME,
								StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));
					}

					//fill receiver data
					if (HoTTbinReader.buf[24] != 0x1F) //receiver sensitive data
						isReceiverData = parseReceiver(HoTTbinReader.buf);

					if (channelNumber == 4) {
						//HoTTbinReader.buf[25] = lastWarningDetected;
						parseChannel(HoTTbinReader.buf, valuesChannel, numberLogChannels);
						//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
						System.arraycopy(valuesChannel, 0, HoTTlogReader2.points, 4, 2); //copy Tx-dbm, Rx-dbm
						//73=Ch 1, 74=Ch 2, 75=Ch 3 .. 88=Ch 16, 89=PowerOff, 90=BatterieLow, 91=Reset, 92=reserve
						System.arraycopy(valuesChannel, 2, HoTTlogReader2.points, 73, 20); //copy channel data and events, warning
					}

					switch ((byte) (HoTTbinReader.buf[26] & 0xFF)) { //actual sensor 
					case HoTTAdapter.ANSWER_SENSOR_VARIO_19200:
						isVarioData = parseVario(HoTTbinReader.buf, valuesVario);
						if (isVarioData && isReceiverData) {
							migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber, valuesVario, valuesGPS, valuesGAM, valuesEAM, valuesESC);
							isReceiverData = false;
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_GPS_19200:
						isGPSData = parseGPS(HoTTbinReader.buf, valuesGPS);
						if (isGPSData && isReceiverData) {
							migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber, valuesVario, valuesGPS, valuesGAM, valuesEAM, valuesESC);
							isReceiverData = false;
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_GENERAL_19200:
						isGeneralData = parseGAM(HoTTbinReader.buf, valuesGAM);
						if (isGeneralData && isReceiverData) {
							migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber, valuesVario, valuesGPS, valuesGAM, valuesEAM, valuesESC);
							isReceiverData = false;
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_ELECTRIC_19200:
						isElectricData = parseEAM(HoTTbinReader.buf, valuesEAM);
						if (isElectricData && isReceiverData) {
							migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber, valuesVario, valuesGPS, valuesGAM, valuesEAM, valuesESC);
							isReceiverData = false;
						}
						break;
					case HoTTAdapter.ANSWER_SENSOR_MOTOR_DRIVER_19200:
						isMotorDriverData = parseESC(HoTTbinReader.buf, valuesESC, channelNumber);
						if (isMotorDriverData && isReceiverData) {
							migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber, valuesVario, valuesGPS, valuesGAM, valuesEAM, valuesESC);
							isReceiverData = false;
						}
						break;
					case 0x1F: //receiver sensitive data
					default:
						break;
					}

					if (HoTTlogReader2.isJustMigrated && HoTTbinReader.countLostPackages > 0) {
						HoTTbinReader.lostPackages.add(HoTTbinReader.countLostPackages);
						HoTTbinReader.countLostPackages = 0;
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

					if (menuToolBar != null && i % progressIndicator == 0) HoTTbinReader.application.setProgress((int) (i * 100 / numberDatablocks), sThreadId);
				}
					else { //skip empty block, but add time step
						if (HoTTlogReader2.logger.isLoggable(Level.FINE)) HoTTlogReader2.logger.log(Level.FINE, "-->> Found tx=rx=0 dBm");

						HoTTAdapter.reverseChannelPackageLossCounter.add(0);
						HoTTlogReader2.points[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;

						++countPackageLoss; // add up lost packages in telemetry data 
						++HoTTbinReader.countLostPackages;
						//HoTTbinReader2.points[0] = (int) (countPackageLoss*100.0 / ((HoTTbinReader2.timeStep_ms+10) / 10.0)*1000.0); 

						if (channelNumber == 4) {
							//HoTTbinReader.buf[25] = lastWarningDetected;
							parseChannel(HoTTbinReader.buf, valuesChannel, numberLogChannels);
							//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
							System.arraycopy(valuesChannel, 0, HoTTlogReader2.points, 4, 2); //copy Tx-dbm, Rx-dbm
							//73=Ch 1, 74=Ch 2, 75=Ch 3 .. 88=Ch 16, 89=PowerOff, 90=BatterieLow, 91=Reset, 92=reserve
							System.arraycopy(valuesChannel, 2, HoTTlogReader2.points, 73, 20); //copy channel data and events, warning
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
					+ HoTTbinReader.sensorSignature);
			HoTTlogReader2.logger.logp(Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTlogReader2.logger.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (menuToolBar != null) {
				HoTTbinReader.application.setProgress(99, sThreadId);
				device.makeInActiveDisplayable(HoTTlogReader2.recordSet);
				device.updateVisibilityStatus(HoTTlogReader2.recordSet, true);

				//write filename after import to record description
				HoTTlogReader2.recordSet.descriptionAppendFilename(file.getName());

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
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
		//46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14, 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
		//73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max, 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M
		//Channels
		//73=Ch 1, 74=Ch 2, 75=Ch 3 .. 88=Ch 16, 89=PowerOff, 90=BatterieLow, 91=Reset, 92=reserve
		//93=VoltageM, 94=CurrentM, 95=CapacityM, 96=PowerM, 97=RevolutionM, 98=TemperatureM 1, 99=TemperatureM 2 100=Voltage_min, 101=Current_max, 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M

		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14, 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
		if (isElectricData) {
			for (int j = 0; !isVarioData && !isGPSData && !isGeneralData && j < 3; j++) { //0=altitude 1=climb1 2=climb3
				HoTTlogReader2.points[j+10] = valuesEAM[j];
			}
			for (int k = 0; k < 27; k++) { //3=voltage 4=current 5=capacity...
				HoTTlogReader2.points[k+46] = valuesEAM[k+3];
			}
		}
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
		if (isGeneralData) {		
			for (int j = 0; !isVarioData && !isGPSData && j < 3; j++) { //0=altitude 1=climb1 2=climb3
				HoTTlogReader2.points[j+10] = valuesGAM[j];
			}
			for (int k = 0; k < 22; k++) { //3=voltage 4=current 5=capacity...
				HoTTlogReader2.points[k+24] = valuesGAM[k+3];
			}
		}
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		if (isGPSData) {
			for (int j = 0; !isVarioData && j < 3; j++) { //0=altitude 1=climb1 2=climb3
				HoTTlogReader2.points[j+10] = valuesGPS[j];
			}
			for (int k = 0; k < 9; k++) {
				HoTTlogReader2.points[k+15] = valuesGPS[k+3];
			}
		}
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		if (isVarioData) {
			for (int j = 0; j < 5; j++) {
				HoTTlogReader2.points[j+10] = valuesVario[j];
			}
		}
		if (isMotorDriverData) {
			if (channelNumber == 4)
				//93=VoltageM, 94=CurrentM, 95=CapacityM, 96=PowerM, 97=RevolutionM, 98=TemperatureM 1, 99=TemperatureM 2 100=Voltage_min, 101=Current_max, 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M
				for (int j = 0; j < 13; j++) {
					HoTTlogReader2.points[j+93] = valuesESC[j];
				}
			else
				//73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max, 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M
				for (int j = 0; j < 13; j++) {
					HoTTlogReader2.points[j+73] = valuesESC[j];
				}
		}
		HoTTlogReader2.recordSet.addPoints(HoTTlogReader2.points, HoTTbinReader.timeStep_ms);
		HoTTlogReader2.isJustMigrated = true;
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 * @param _buf
	 */
	protected static boolean parseReceiver(byte[] _buf) {
		//log.log(Level.OFF, StringHelper.byte2Hex2CharString(_buf, 12, 10));
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//data bytes: 12=RX-S-STR 13=voltageRx 14=TemperatureRx 15=Rx-DBM 16=RX-S-QUA 17=voltageRx_min 18=VPack_low_byte 19=VPack_high_byte 20=Tx-DBM 21=?
		HoTTbinReader.tmpVoltageRx = (_buf[13] & 0xFF);
		HoTTbinReader.tmpTemperatureRx = (_buf[2] & 0xFF);
		HoTTlogReader2.points[1] = (_buf[16] & 0xFF) * 1000;
		HoTTlogReader2.points[3] = DataParser.parse2Short(_buf, 18) * 1000;
		if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpVoltageRx > -1 && HoTTbinReader.tmpVoltageRx < 100 && HoTTbinReader.tmpTemperatureRx < 100) {
			HoTTlogReader2.points[2] = (convertRxDbm2Strength(_buf[9] & 0xFF)) * 1000;
			HoTTlogReader2.points[4] = _buf[8] * -1000;
			HoTTlogReader2.points[5] = _buf[9] * -1000;
			HoTTlogReader2.points[6] = (_buf[13] & 0xFF) * 1000;
			HoTTlogReader2.points[7] = ((_buf[14] & 0xFF) - 20) * 1000;
			HoTTlogReader2.points[8] = (_buf[17] & 0xFF) * 1000;
		}
		if ((_buf[25] & 0x40) > 0 || (_buf[25] & 0x20) > 0 && HoTTbinReader.tmpTemperatureRx >= 70) //T = 70 - 20 = 50 lowest temperature warning
			HoTTlogReader2.points[9] = (_buf[25] & 0x60) * 1000; //warning V,T only
		else
			HoTTlogReader2.points[9] = 0;
		
		if (log.isLoggable(Level.FINER)) {
			//data bytes: 8=TXdBm(-D), 9=RXdBm(-D)
			StringBuilder sb = new StringBuilder().append(String.format("Tx-dbm = -%d Rx-dbm = -%d", _buf[8], _buf[9]));
			for (int i = 0; i < 10; i++) {
				sb.append(String.format(" %6.3f", HoTTlogReader2.points[i] / 1000.0));
			}
			log.log(Level.FINER, sb.toString());
		}
		
		return true;
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 * @param _buf
	 */
	protected static boolean parseVario(byte[] _buf, int[] values) {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//sensor byte: 26=sensor byte
		//27=inverseBits 28,29=altitude 30,31=altitude_max 32,33=altitude_min 34,35=climb1 36,37=climb3 38,39=climb10
 
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf, 28) - 500;
		if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpHeight >= -490 && HoTTbinReader.tmpHeight < 5000)) {
			values[0] = HoTTbinReader.tmpHeight * 1000;
			//pointsVarioMax = DataParser.parse2Short(buf1, 30) * 1000;
			//pointsVarioMin = DataParser.parse2Short(buf1, 32) * 1000;
			values[1] = (DataParser.parse2UnsignedShort(_buf, 34) - 30000) * 10;
		}
		HoTTbinReader.tmpClimb10 = DataParser.parse2UnsignedShort(_buf , 38) - 30000;
		if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpClimb10 > -10000 && HoTTbinReader.tmpClimb10 < 10000) {
			values[2] = (DataParser.parse2UnsignedShort(_buf, 36) - 30000) * 10;
			values[3] = HoTTbinReader.tmpClimb10 * 10;
		}
		values[4] = (_buf[27] & 0x3F) * 1000; //inverse event
		
		if (log.isLoggable(Level.FINER)) {
			printSensorValues(_buf, values, 5);
		}
		return true;
	}

	/**
	 * parse the buffered data from buffer 0 to 3 and add points to record set
	 * @param _buf
	 */
	protected static boolean parseGPS(byte[] _buf, int[] values) {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		//sensor byte: 26=sensor byte
		//27,28=InverseBits 29=moveDirection 30,31=speed 32,33,34,35,36=latitude 37,38,39,40,41=longitude 42,43=distanceStart 44,45=altitude
		//46,47=climb1 48=climb3 49=#satellites 50=GPS-Fix 51=homeDirection 52,53=northVelocity 54=hAcc 55-58=GPStime 59,60=eastVelocity
		//61=hAcc 62-65=freeChars 66=version

		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf, 44) - 500;
		HoTTbinReader.tmpClimb1 = (DataParser.parse2UnsignedShort(_buf, 46) - 30000);
		HoTTbinReader.tmpClimb3 = (_buf[48] & 0xFF) - 120;
		HoTTbinReader.tmpVelocity = DataParser.parse2Short(_buf, 30) * 1000;
		//values 3=Latitude, 4=Longitude, 5=Velocity, 6=DistanceStart...
		if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpClimb1 > -20000 && HoTTbinReader.tmpClimb3 > -90 && HoTTbinReader.tmpHeight >= -490 && HoTTbinReader.tmpHeight < 4500)) {
			values[5] = HoTTAdapter.isFilterEnabled && HoTTbinReader.tmpVelocity > 500000 ? values[5] : HoTTbinReader.tmpVelocity;

			HoTTbinReader.tmpLatitude = DataParser.parse2Short(_buf, 33) * 10000 + DataParser.parse2Short(_buf, 35);
			if (!HoTTAdapter.isTolerateSignChangeLatitude) HoTTbinReader.tmpLatitude = _buf[32] == 1 ? -1 * HoTTbinReader.tmpLatitude : HoTTbinReader.tmpLatitude;
			HoTTbinReader.tmpLatitudeDelta = Math.abs(HoTTbinReader.tmpLatitude - values[3]);
			HoTTbinReader.tmpLatitudeDelta = HoTTbinReader.tmpLatitudeDelta > 400000 ? HoTTbinReader.tmpLatitudeDelta - 400000 : HoTTbinReader.tmpLatitudeDelta;
			HoTTbinReader.latitudeTolerance = (values[5] / 1000.0) * (HoTTbinReader.timeStep_ms - HoTTbinReader.lastLatitudeTimeStep) / HoTTAdapter.latitudeToleranceFactor;
			HoTTbinReader.latitudeTolerance = HoTTbinReader.latitudeTolerance > 0 ? HoTTbinReader.latitudeTolerance : 5;

			if (!HoTTAdapter.isFilterEnabled || values[3] == 0 || HoTTbinReader.tmpLatitudeDelta <= HoTTbinReader.latitudeTolerance) {
				HoTTbinReader.lastLatitudeTimeStep = HoTTbinReader.timeStep_ms;
				values[3] = HoTTbinReader.tmpLatitude;
			}
			else {
				if (HoTTlogReader2.log.isLoggable(Level.FINE))
					HoTTlogReader2.log.log(Level.FINE, StringHelper.getFormatedTime("HH:mm:ss:SSS", HoTTbinReader.timeStep_ms - GDE.ONE_HOUR_MS) + " Lat " + HoTTbinReader.tmpLatitude + " - "
							+ HoTTbinReader.tmpLatitudeDelta);
			}

			HoTTbinReader.tmpLongitude = DataParser.parse2Short(_buf, 38) * 10000 + DataParser.parse2Short(_buf, 40);
			if (!HoTTAdapter.isTolerateSignChangeLongitude) HoTTbinReader.tmpLongitude = _buf[37] == 1 ? -1 * HoTTbinReader.tmpLongitude : HoTTbinReader.tmpLongitude;
			HoTTbinReader.tmpLongitudeDelta = Math.abs(HoTTbinReader.tmpLongitude - values[4]);
			HoTTbinReader.tmpLongitudeDelta = HoTTbinReader.tmpLongitudeDelta > 400000 ? HoTTbinReader.tmpLongitudeDelta - 400000 : HoTTbinReader.tmpLongitudeDelta;
			HoTTbinReader.longitudeTolerance = (values[5] / 1000.0) * (HoTTbinReader.timeStep_ms - HoTTbinReader.lastLongitudeTimeStep) / HoTTAdapter.longitudeToleranceFactor;
			HoTTbinReader.longitudeTolerance = HoTTbinReader.longitudeTolerance > 0 ? HoTTbinReader.longitudeTolerance : 5;

			if (!HoTTAdapter.isFilterEnabled || values[4] == 0 || HoTTbinReader.tmpLongitudeDelta <= HoTTbinReader.longitudeTolerance) {
				HoTTbinReader.lastLongitudeTimeStep = HoTTbinReader.timeStep_ms;
				values[4] = HoTTbinReader.tmpLongitude;
			}
			else {
				if (HoTTlogReader2.log.isLoggable(Level.FINE))
					HoTTlogReader2.log.log(Level.FINE, StringHelper.getFormatedTime("HH:mm:ss:SSS", HoTTbinReader.timeStep_ms - GDE.ONE_HOUR_MS) + " Long " + HoTTbinReader.tmpLongitude + " - "
							+ HoTTbinReader.tmpLongitudeDelta);
			}

			//values 0=altitude 1=climb1 2=climb3
			values[0] = HoTTbinReader.tmpHeight * 1000;
			values[1] = HoTTbinReader.tmpClimb1 * 10;
			values[2] = HoTTbinReader.tmpClimb3 * 1000;
			
			values[6] = DataParser.parse2Short(_buf, 42) * 1000;
			values[7] = (_buf[51] & 0xFF) * 1000;
			values[8] = 0;
			values[9] = (_buf[49] & 0xFF) * 1000;
			try {
				values[10] = Integer.valueOf(String.format("%c", _buf[50])) * 1000;
			}
			catch (NumberFormatException e1) {
				//ignore;
			}
		}
		if ((_buf[27] & 0xFF) != 0)
		values[11] = (_buf[27] & 0x0F) * 1000; //inverse event
		
		if (log.isLoggable(Level.FINER)) {
			printSensorValues(_buf, values, 12);
		}
		return true;
	}

	/**
	 * print values evaluated by parsing sensor bytes
	 * @param buffer
	 * @param values
	 * @param endIndex
	 */
	protected static void printSensorValues(byte[] buffer, int[] values, int endIndex) {
		log.log(Level.FINER, StringHelper.byte2Hex2CharString(buffer, 26, 38));
		StringBuilder sb = new StringBuilder().append(String.format("Sensor = 0x%X", buffer[26]));
		sb.append(String.format(" %d", values[endIndex-1] / 1000));
		for (int i = 0; i < 3; i++) { //values 0=altitude 1=climb1 2=climb3
			sb.append(String.format(" %6.3f", values[i] / 1000.0));
		}
		for (int i = 3; i < endIndex; i++) {
			sb.append(String.format(" %6.3f", values[i] / 1000.0));
		}
		log.log(Level.FINER, sb.toString());
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * @param _buf
	 */
	protected static boolean parseGAM(byte[] _buf, int[] values) {
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf, 46) - 500;
		HoTTbinReader.tmpClimb3 = (_buf[50] & 0xFF) - 120;
		HoTTbinReader.tmpVoltage1 = DataParser.parse2Short(_buf, 35);
		HoTTbinReader.tmpVoltage2 = DataParser.parse2Short(_buf, 37);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf, 55);
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
		//sensor byte: 26=sensor byte
		//27,28=InverseBits 29=cell1, 30=cell2 31=cell3 32=cell4 33=cell5 34=cell6 35,36=voltage1 37,38=voltage2 39=temperature1 40=temperature2
		//41=? 42,43=fuel 44,45=rpm 46,47=altitude 48,49=climb1 50=climb3 51,52=current 53,54=voltage 55,56=capacity 57,58=speed 
		//59=cellVoltage_min 60=#cellVoltage_min 61,62=rpm2 63=#error 64=pressure 65=version
		
		if (!HoTTAdapter.isFilterEnabled
				|| (HoTTbinReader.tmpClimb3 > -90 && HoTTbinReader.tmpHeight >= -490 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600 && Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			values[3] = DataParser.parse2Short(_buf, 53) * 1000;
			values[4] = DataParser.parse2Short(_buf, 51) * 1000;
			if (!HoTTAdapter.isFilterEnabled || HoTTlogReader2.recordSet.getRecordDataSize(true) <= 20
					|| (HoTTbinReader.tmpCapacity != 0 && Math.abs(HoTTbinReader.tmpCapacity) <= (values[5] / 1000 + values[3] / 1000 * values[4] / 1000 / 2500 + 2))) {
				values[5] = HoTTbinReader.tmpCapacity * 1000;
			}
			else {
				HoTTlogReader2.log.log(Level.WARNING, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - "
						+ (values[5] / 1000) + " + " + (values[3] / 1000 * values[4] / 1000 / 2500 + 2));
			}
			values[6] = Double.valueOf(values[3] / 1000.0 * values[4]).intValue();
			//cell voltage
			for (int j = 0; j < 6; j++) {
				values[j + 8] = (_buf[j + 29] & 0xFF) * 1000;
				if (values[j + 5] > 0) {
					maxVotage = values[j + 8] > maxVotage ? values[j + 8] : maxVotage;
					minVotage = values[j + 8] < minVotage ? values[j + 8] : minVotage;
				}
			}
			values[7] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
			values[14] = DataParser.parse2Short(_buf, 44) * 1000;
			values[15] = DataParser.parse2Short(_buf, 42) * 1000;
			//values 0=altitude 1=climb1 2=climb3
			values[0] = HoTTbinReader.tmpHeight * 1000;
			values[1] = (DataParser.parse2UnsignedShort(_buf, 48) - 30000) * 10;
			values[2] = HoTTbinReader.tmpClimb3 * 1000;
			
			values[16] = HoTTbinReader.tmpVoltage1 * 100;
			values[17] = HoTTbinReader.tmpVoltage2 * 100;
			values[18] = ((_buf[39] & 0xFF) - 20) * 1000;
			values[19] = ((_buf[40] & 0xFF) - 20) * 1000;
			values[20] = DataParser.parse2Short(_buf, 57) * 1000; //Speed [km/h
			values[21] = (_buf[59] & 0xFF) * 1000; //lowest cell voltage 124 = 2.48 V
			values[22] = (_buf[60] & 0xFF) * 1000; //cell number lowest cell voltage
			values[23] = (_buf[64] & 0xFF) * 1000; //Pressure
		}
		values[24] = ((_buf[27] & 0xFF) + ((_buf[28] & 0x7F) << 8)) * 1000; //inverse event
		
		if (log.isLoggable(Level.FINER)) {
			printSensorValues(_buf, values, 25);
		}
		return true;
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static boolean parseEAM(byte[] _buf, int[] values) throws DataInconsitsentException {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
		//46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14, 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
		//sensor byte: 26=sensor byte
		//27,28=InverseBits 29=cell1, 30=cell2 31=cell3 32=cell4 33=cell5 34=cell6 35=cell7 36=cell8 37=cell9 38=cell10 39=cell11 40=cell12 41=cell13 42=cell14
		//43,44=voltage1 45,46=voltage2 47=temperature1 48=temperature2 49,50=altitude 51,52=current 53,54=voltage 55,56=capacity 57,58=climb1 59=climb3
		//60,61=rpm 62,63=runtime>3A 64,65=speed

		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf, 49) - 500;
		HoTTbinReader.tmpClimb3 = (_buf[59] & 0xFF) - 120;
		HoTTbinReader.tmpVoltage1 = DataParser.parse2Short(_buf, 43);
		HoTTbinReader.tmpVoltage2 = DataParser.parse2Short(_buf, 45);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf, 55);
		if (!HoTTAdapter.isFilterEnabled
				|| (HoTTbinReader.tmpClimb3 > -90 && HoTTbinReader.tmpHeight >= -490 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600 && Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			values[3] = DataParser.parse2Short(_buf, 53) * 1000;
			values[4] = DataParser.parse2Short(_buf, 51) * 1000;
			if (!HoTTAdapter.isFilterEnabled || HoTTlogReader2.recordSet.getRecordDataSize(true) <= 20
					|| Math.abs(HoTTbinReader.tmpCapacity) <= (values[5] / 1000 + values[3] / 1000 * values[4] / 1000 / 2500 + 2)) {
				values[5] = HoTTbinReader.tmpCapacity * 1000;
			}
			else {
				HoTTlogReader2.log.log(Level.WARNING, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - "
						+ (values[5] / 1000) + " + " + (values[3] / 1000 * values[4] / 1000 / 2500 + 2));
			}
			values[6] = Double.valueOf(values[3] / 1000.0 * values[4]).intValue(); // power U*I [W];
			for (int j = 0; j < 14; j++) {
				values[j + 8] = (_buf[j + 29] & 0xFF) * 1000;
				if (values[j + 8] > 0) {
					maxVotage = values[j + 8] > maxVotage ? values[j + 8] : maxVotage;
					minVotage = values[j + 8] < minVotage ? values[j + 8] : minVotage;
				}
			}
			//calculate balance on the fly
			values[7] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
			//values 0=altitude 1=climb1 2=climb3
			values[0] = HoTTbinReader.tmpHeight * 1000;
			values[1] = (DataParser.parse2UnsignedShort(_buf, 57) - 30000) * 10;
			values[2] = HoTTbinReader.tmpClimb3 * 1000;
			
			values[22] = HoTTbinReader.tmpVoltage1 * 100;
			values[23] = HoTTbinReader.tmpVoltage2 * 100;
			values[24] = ((_buf[47] & 0xFF) - 20) * 1000;
			values[25] = ((_buf[48] & 0xFF) - 20) * 1000;
			values[26] = DataParser.parse2Short(_buf, 60) * 1000;
			values[27] = ((_buf[62] & 0xFF) * 60 + (_buf[63] & 0xFF)) * 1000; // motor time
			values[28] = DataParser.parse2Short(_buf, 64) * 1000; // speed
		}
		values[29] = ((_buf[27] & 0xFF) + ((_buf[28] & 0x7F) << 8)) * 1000; //inverse event

		if (log.isLoggable(Level.FINER)) {
			printSensorValues(_buf, values, 30);
		}

		return true;
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 * @param _buf
	 */
	protected static void parseChannel(byte[] _buf,final int[] values, int numberUsedChannels) {
		
//		typedef struct _MASS_STORAGE_LOG {
//			DWORD	timestamp;
//			BYTE	STATUS[8];
//			BYTE	RECEIVER[10];
//			BYTE	S_INFOR[4];
//			BYTE	S_DATA[40];
//			SWORD	CHANNELS[32]; 8 + 4
//		} MASS_STORAGE_LOG;

		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
		//46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14, 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
		//73=Ch 1, 74=Ch 2, 75=Ch 3 .. 88=Ch 16, 89=PowerOff, 90=BatterieLow, 91=Reset, 92=reserve
		//offset 66
		values[0] = _buf[8] * -1000;
		values[1] = _buf[9] * -1000;

		for (int i = 0,j = 0; i < numberUsedChannels && i < 16; i++,j+=2) {
			values[2 + i] = (DataParser.parse2UnsignedShort(_buf, (66 + j)) / 2) * 1000; 
		}
		
//		values[2] = (DataParser.parse2UnsignedShort(_buf, 66) / 2) * 1000; 
//		values[3] = (DataParser.parse2UnsignedShort(_buf, 68) / 2) * 1000; 
//		values[4] = (DataParser.parse2UnsignedShort(_buf, 70) / 2) * 1000;
//		values[5] = (DataParser.parse2UnsignedShort(_buf, 72) / 2) * 1000;
//		values[6] = (DataParser.parse2UnsignedShort(_buf, 74) / 2) * 1000;
//		values[78] = (DataParser.parse2UnsignedShort(_buf, 76) / 2) * 1000;
//	values[79] = (DataParser.parse2UnsignedShort(_buf, 20) / 2) * 1000;
//	values[80] = (DataParser.parse2UnsignedShort(_buf, 22) / 2) * 1000;
//	values[79] = (DataParser.parse2UnsignedShort(_buf, 20) / 2) * 1000;
//	values[80] = (DataParser.parse2UnsignedShort(_buf, 22) / 2) * 1000;
//	values[79] = (DataParser.parse2UnsignedShort(_buf, 20) / 2) * 1000;
//	values[80] = (DataParser.parse2UnsignedShort(_buf, 22) / 2) * 1000;
//	values[79] = (DataParser.parse2UnsignedShort(_buf, 20) / 2) * 1000;
//	values[80] = (DataParser.parse2UnsignedShort(_buf, 22) / 2) * 1000;
//	values[79] = (DataParser.parse2UnsignedShort(_buf, 20) / 2) * 1000;
//	values[80] = (DataParser.parse2UnsignedShort(_buf, 22) / 2) * 1000;
		//events
//		values[35] = 0;
//		values[36] = 0;
//		values[37] = 0;
		
		values[38] = _buf[25] * 1000; //warning
//		values[92] = _buf[6] * 1000; //warning
//		values[92] = (_buf[5] &0xFF) * 1000; //warning
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param channelNumber
	 * @throws DataInconsitsentException
	 */
	protected static boolean parseESC(byte[] _buf, int[] values, int channelNumber) throws DataInconsitsentException {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
		//46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14, 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
		//73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max, 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M

		//73=Ch 1, 74=Ch 2, 75=Ch 3 .. 88=Ch 16, 89=PowerOff, 90=BatterieLow, 91=Reset, 92=reserve
		//93=VoltageM, 94=CurrentM, 95=CapacityM, 96=PowerM, 97=RevolutionM, 98=TemperatureM 1, 99=TemperatureM 2 100=Voltage_min, 101=Current_max, 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M

		//sensor byte: 26=sensor byte
		//27,28=InverseBits 29,30=voltageIn 31,32=voltageIn_min 33,34=capacity 35=temperature1 36=temperature1_max 37,38=current 39,40=current_max
		//41,42=rpm 43,44=rpm_max 45=temperature2 46=temperature2_max

		HoTTbinReader.tmpVoltage = DataParser.parse2Short(_buf, 29);
		HoTTbinReader.tmpCurrent = DataParser.parse2Short(_buf, 37);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf, 33);
		HoTTbinReader.tmpRevolution = DataParser.parse2Short(_buf, 41);
		HoTTbinReader.tmpTemperatureFet = _buf[35] - 20;
		//73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max, 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M
		if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpVoltage > 0 && HoTTbinReader.tmpVoltage < 1000 && HoTTbinReader.tmpCurrent < 4000 && HoTTbinReader.tmpCurrent > -10
				&& HoTTbinReader.tmpRevolution > -1 && HoTTbinReader.tmpRevolution < 20000
				&& !(values[6] != 0 && values[6] / 1000 - HoTTbinReader.tmpTemperatureFet > 20)) {
			values[0] = HoTTbinReader.tmpVoltage * 1000;
			values[1] = HoTTbinReader.tmpCurrent * 1000;
			values[3] = Double.valueOf(values[0] / 1000.0 * values[1]).intValue();
			if (!HoTTAdapter.isFilterEnabled || HoTTlogReader2.recordSet.getRecordDataSize(true) <= 20
					|| (HoTTbinReader.tmpCapacity != 0 && Math.abs(HoTTbinReader.tmpCapacity) <= (values[2] / 1000 + HoTTbinReader.tmpVoltage * HoTTbinReader.tmpCurrent / 2500 + 2))) {
				values[2] = HoTTbinReader.tmpCapacity * 1000;
			}
			else {
				if (HoTTbinReader.tmpCapacity != 0)
					HoTTlogReader2.log.log(Level.WARNING, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - "
						+ (values[4] / 1000) + " + " + (HoTTbinReader.tmpVoltage * HoTTbinReader.tmpCurrent / 2500 + 2));
			}
			values[4] = HoTTbinReader.tmpRevolution * 1000;
			values[5] = HoTTbinReader.tmpTemperatureFet * 1000;

			values[6] = (_buf[45] - 20) * 1000;
			values[7] = DataParser.parse2Short(_buf, 31) * 1000;
			values[8] = DataParser.parse2Short(_buf, 39) * 1000;
			values[9] = DataParser.parse2Short(_buf, 43) * 1000;
			values[10] = (_buf[36] - 20) * 1000;
			values[11] = (_buf[46] - 20) * 1000;
		}
		values[12] = ((_buf[27] & 0xFF) + ((_buf[28] & 0x7F) << 8)) * 1000; //inverse event

		if (log.isLoggable(Level.FINER)) {
			printSensorValues(_buf, values, 13);
		}
		return true;
	}
}
