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
    
    Copyright (c) 2011,2012,2013,2014 Winfried Bruegmann
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
public class HoTTbinReader2 extends HoTTbinReader {
	final static Logger	logger					= Logger.getLogger(HoTTbinReader2.class.getName());
	static int[]				points;
	static RecordSet		recordSet;
	static boolean			isJustMigrated	= false;

	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws Exception 
	 */
	public static synchronized void read(String filePath) throws Exception {
		HashMap<String, String> header = null;
		File file = new File(filePath);
		HoTTbinReader.log.log(java.util.logging.Level.FINER, file.getName() + " - " + new SimpleDateFormat("yyyy-MM-dd").format(file.lastModified()));
		header = getFileInfo(file);

		if (Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) <= 1) {
			HoTTbinReader.isReceiverOnly = Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) == 0;
			readSingle(file);
		}
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
	static void readSingle(File file) throws IOException, DataInconsitsentException {
		final String $METHOD_NAME = "readSingle";
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
		device.getMeasurementFactor(channelNumber, 12);
		boolean isInitialSwitched = false;
		boolean isReceiverData = false;
		boolean isSensorData = false;
		HoTTbinReader2.recordSet = null;
		//0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 
		//8=Height, 9=Climb 1, 10=Climb 3, 11=Climb 10
		//12=Latitude, 13=Longitude, 14=Velocity, 15=DistanceStart, 16=DirectionStart, 17=TripDistance
		//18=VoltageG, 19=CurrentG, 20=CapacityG, 21=PowerG, 22=BalanceG, 23=CellVoltageG 1, 24=CellVoltageG 2 .... 28=CellVoltageG 6, 29=Revolution, 30=FuelLevel, 31=VoltageG 1, 32=VoltageG 2, 33=TemperatureG 1, 34=TemperatureG 2
		//35=VoltageE, 36=CurrentE, 37=CapacityE, 38=PowerE, 39=BalanceE, 40=CellVoltageE 1, 41=CellVoltageE 2 .... 53=CellVoltageE 14, 54=VoltageE 1, 55=VoltageE 2, 56=TemperatureE 1, 57=TemperatureE 2
		//58=VoltageM, 59=CurrentM, 60=CapacityM, 61=PowerM, 62=RevolutionM, 63=TemperatureM
		//channelConfig 4 -> Channel
		//58=Ch 1, 59=Ch 2 , 60=Ch 3 .. 73=Ch 16
		//74=PowerOff, 75=BattLow, 76=Reset, 77=reserved
		//78=VoltageM, 79=CurrentM, 80=CapacityM, 81=PowerM, 82=RevolutionM, 83=TemperatureM
		HoTTbinReader2.points = new int[device.getNumberOfMeasurements(channelNumber)];
		HoTTbinReader2.points[2] = 0;
		HoTTbinReader.pointsGeneral = HoTTbinReader.pointsElectric = HoTTbinReader.pointsSpeedControl = HoTTbinReader.pointsVario = HoTTbinReader.pointsGPS = HoTTbinReader2.points;
		HoTTbinReader.timeStep_ms = 0;
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
		HoTTbinReader.isTextModusSignaled = false;
		int countPackageLoss = 0;
		long numberDatablocks = fileSize / HoTTbinReader.dataBlockSize / (HoTTbinReader.isReceiverOnly && channelNumber != 4 ? 10 : 1);
		long startTimeStamp_ms = file.lastModified() - (numberDatablocks * 10);
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		MenuToolBar menuToolBar = HoTTbinReader.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		if (menuToolBar != null) HoTTbinReader.application.setProgress(0, sThreadId);

		try {
			//check if recordSet initialized, transmitter and receiver data always present, but not in the same data rate and signals
			channel = HoTTbinReader.channels.get(channelNumber);
			channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
			recordSetName = recordSetNumber + device.getRecordSetStemName() + recordSetNameExtend;
			HoTTbinReader2.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true);
			channel.put(recordSetName, HoTTbinReader2.recordSet);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (HoTTbinReader.application.getMenuToolBar() != null) {
				channel.applyTemplate(recordSetName, false);
			}
			//recordSet initialized and ready to add data

			//read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTbinReader2.logger.isLoggable(java.util.logging.Level.FINE) && i % 10 == 0) {
					HoTTbinReader2.logger.logp(java.util.logging.Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
				}
				if (HoTTbinReader2.logger.isLoggable(java.util.logging.Level.FINE)) {
					HoTTbinReader2.logger.logp(java.util.logging.Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if ((HoTTbinReader.buf[6] & 0x01) != 1) { //switch into text modus
					if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { //buf 0 to 4, tx,rx			
						HoTTAdapter.reverseChannelPackageLossCounter.add(1);
						HoTTbinReader2.points[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;

						if (HoTTbinReader2.logger.isLoggable(java.util.logging.Level.FINER))
							HoTTbinReader2.logger.logp(java.util.logging.Level.FINER, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1)
									+ GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

						//fill receiver data
						if (HoTTbinReader.buf[33] == 0 && (HoTTbinReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader.buf, 40) >= 0) {
							parseReceiver(HoTTbinReader.buf);
							isReceiverData = true;
						}
						if (channelNumber == 4) parseChannel(HoTTbinReader.buf); //Channels

						//fill data block 0 receiver voltage an temperature
						if (HoTTbinReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader.buf, 0) != 0) {
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf0, 0, HoTTbinReader.buf0.length);
						}
						if (HoTTbinReader.isReceiverOnly && channelNumber != 4) { //reduce data rate for receiver to 0.1 sec
							for (int j = 0; j < 9; j++) {
								data_in.read(HoTTbinReader.buf);
								HoTTbinReader.timeStep_ms += 10;
							}
						}

						//create and fill sensor specific data record sets 
						switch ((byte) (HoTTbinReader.buf[7] & 0xFF)) {
						case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
						case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
							if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.VARIO.ordinal()]) {
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
									version = parseVario(version, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2);
									HoTTbinReader.buf1 = HoTTbinReader.buf2 = null;
									isSensorData = true;
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_GPS_115200:
						case HoTTAdapter.SENSOR_TYPE_GPS_19200:
							if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GPS.ordinal()]) {
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
									parseGPS(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3);
									HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = null;
									isSensorData = true;
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
						case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
							if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()]) {
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
									parseGeneral(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
									HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
									isSensorData = true;
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
							if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.EAM.ordinal()]) {
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
									parseElectric(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
									HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
									isSensorData = true;
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
							if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()]) {
								//fill data block 0 to 4
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
								if (HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null) {
									parseSpeedControl(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, channelNumber);
									HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
									isSensorData = true;
								}
							}
							break;
						}

						if (isSensorData && HoTTbinReader.countLostPackages > 0) {
							HoTTbinReader.lostPackages.add(HoTTbinReader.countLostPackages);
							HoTTbinReader.countLostPackages = 0;
						}

						if (isSensorData || isReceiverData) {
							HoTTbinReader2.recordSet.addPoints(HoTTbinReader2.points, HoTTbinReader.timeStep_ms);
							isSensorData = isReceiverData = false;
						}
						else if (channelNumber == 4) {
							HoTTbinReader2.recordSet.addPoints(HoTTbinReader2.points, HoTTbinReader.timeStep_ms);
						}

						HoTTbinReader.timeStep_ms += 10; // add default time step from device of 10 msec

						if (menuToolBar != null && i % progressIndicator == 0) HoTTbinReader.application.setProgress((int) (i * 100 / numberDatablocks), sThreadId);
					}
					else { //skip empty block, but add time step
						HoTTAdapter.reverseChannelPackageLossCounter.add(0);
						HoTTbinReader2.points[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;

						++countPackageLoss; // add up lost packages in telemetry data 
						++HoTTbinReader.countLostPackages;
						//HoTTbinReader2.points[0] = (int) (countPackageLoss*100.0 / ((HoTTbinReader2.timeStep_ms+10) / 10.0)*1000.0); 

						if (channelNumber == 4) {
							parseChannel(HoTTbinReader.buf); //Channels
							HoTTbinReader2.recordSet.addPoints(HoTTbinReader2.points, HoTTbinReader.timeStep_ms);
						}
						HoTTbinReader.timeStep_ms += 10;
						//reset buffer to avoid mixing data
						HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
					}
				}
				else if (!isTextModusSignaled) {
					isTextModusSignaled = true; 
					application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			String packageLossPercentage = tmpRecordSet.getRecordDataSize(true) > 0 ? String.format("%.1f", (countPackageLoss / tmpRecordSet.getTime_ms(tmpRecordSet.getRecordDataSize(true) - 1) * 1000))
					: "0";
			tmpRecordSet.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] { countPackageLoss, packageLossPercentage, HoTTbinReader.lostPackages.getStatistics() })
					+ HoTTbinReader.sensorSignature);
			HoTTbinReader2.logger.logp(java.util.logging.Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReader2.logger.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (menuToolBar != null) {
				HoTTbinReader.application.setProgress(99, sThreadId);
				if (!isInitialSwitched) {
					HoTTbinReader.channels.switchChannel(channel.getName());
					channel.switchRecordSet(recordSetName);
				}
				else {
					device.makeInActiveDisplayable(HoTTbinReader2.recordSet);
				}
				device.updateVisibilityStatus(HoTTbinReader2.recordSet, true);

				//write filename after import to record description
				HoTTbinReader2.recordSet.descriptionAppendFilename(file.getName());

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
		boolean isInitialSwitched = false;
		HoTTbinReader2.recordSet = null;
		HoTTbinReader2.isJustMigrated = false;
		//0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 
		//8=Height, 9=Climb 1, 10=Climb 3, 11=Climb 10
		//12=Latitude, 13=Longitude, 14=Velocity, 15=DistanceStart, 16=DirectionStart, 17=TripDistance
		//18=VoltageG, 19=CurrentG, 20=CapacityG, 21=PowerG, 22=BalanceG, 23=CellVoltageG 1, 24=CellVoltageG 2 .... 28=CellVoltageG 6, 29=Revolution, 30=FuelLevel, 31=VoltageG 1, 32=VoltageG 2, 33=TemperatureG 1, 34=TemperatureG 2
		//35=VoltageE, 36=CurrentE, 37=CapacityE, 38=PowerE, 39=BalanceE, 40=CellVoltageE 1, 41=CellVoltageE 2 .... 53=CellVoltageE 14, 54=VoltageE 1, 55=VoltageE 2, 56=TemperatureE 1, 57=TemperatureE 2
		//58=VoltageM, 59=CurrentM, 60=CapacityM, 61=PowerM, 62=RevolutionM, 63=TemperatureM
		//channelConfig 4 -> Channel
		//58=Ch 1, 59=Ch 2 , 60=Ch 3 .. 73=Ch 16
		//74=PowerOff, 75=BattLow, 76=Reset, 77=reserved
		//78=VoltageM, 79=CurrentM, 80=CapacityM, 81=PowerM, 82=RevolutionM, 83=TemperatureM
		HoTTbinReader2.points = new int[device.getNumberOfMeasurements(channelNumber)];
		HoTTbinReader.pointsGeneral = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsElectric = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsSpeedControl = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsVario = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsVario[2] = 100000;
		HoTTbinReader.pointsGPS = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsChannel = new int[HoTTbinReader2.points.length];
		HoTTbinReader.timeStep_ms = 0;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		HoTTbinReader.buf0 = new byte[30];
		HoTTbinReader.buf1 = new byte[30];
		HoTTbinReader.buf2 = new byte[30];
		HoTTbinReader.buf3 = new byte[30];
		HoTTbinReader.buf4 = new byte[30];
		byte actualSensor = -1, lastSensor = -1;
		int logCountVario = 0, logCountGPS = 0, logCountGeneral = 0, logCountElectric = 0, logCountMotorDriver = 0;
		HoTTAdapter.reverseChannelPackageLossCounter.clear();
		HoTTbinReader.lostPackages.clear();
		HoTTbinReader.countLostPackages = 0;
		HoTTbinReader.isTextModusSignaled = false;
		int countPackageLoss = 0;
		long numberDatablocks = fileSize / HoTTbinReader.dataBlockSize;
		long startTimeStamp_ms = file.lastModified() - (numberDatablocks * 10);
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
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
			recordSetName = recordSetNumber + device.getRecordSetStemName() + recordSetNameExtend;
			HoTTbinReader2.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true);
			channel.put(recordSetName, HoTTbinReader2.recordSet);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (HoTTbinReader.application.getMenuToolBar() != null) {
				channel.applyTemplate(recordSetName, false);
			}
			//recordSet initialized and ready to add data

			//read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTbinReader2.logger.isLoggable(java.util.logging.Level.FINEST) && i % 10 == 0) {
					HoTTbinReader2.logger.logp(java.util.logging.Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
					HoTTbinReader2.logger.logp(java.util.logging.Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if ((HoTTbinReader.buf[6] & 0x01) != 1) { //switch into text modus
					if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { //buf 0 to 4, tx,rx
						HoTTAdapter.reverseChannelPackageLossCounter.add(1);
						HoTTbinReader2.points[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
						//create and fill sensor specific data record sets 
						if (HoTTbinReader2.logger.isLoggable(java.util.logging.Level.FINEST))
							HoTTbinReader2.logger.logp(java.util.logging.Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1)
									+ GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

						//fill receiver data
						if (HoTTbinReader.buf[33] == 0 && (HoTTbinReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader.buf, 40) >= 0) {
							parseReceiver(HoTTbinReader.buf);
							isReceiverData = true;
						}
						if (channelNumber == 4) parseChannel(HoTTbinReader.buf);

						if (actualSensor == -1)
							lastSensor = actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);
						else
							actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);

						//					switch (actualSensor) {
						//					case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
						//					case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
						//						System.out.print("VARIO, ");
						//						break;
						//					case HoTTAdapter.SENSOR_TYPE_GPS_115200:
						//					case HoTTAdapter.SENSOR_TYPE_GPS_19200:
						//						System.out.print("GPS, ");
						//						break;
						//					case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
						//					case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
						//						System.out.print("GAM, ");
						//						break;
						//					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
						//					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
						//						System.out.print("EAM, ");
						//						break;
						//					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
						//					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
						//						System.out.print("ESC, ");
						//						break;
						//					}

						if (actualSensor != lastSensor) {
							if (logCountVario >= 3 || logCountGPS >= 4 || logCountGeneral >= 5 || logCountElectric >= 5 || logCountMotorDriver >= 3) {
								//							System.out.println();
								switch (lastSensor) {
								case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
								case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
									if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.VARIO.ordinal()]) {
										if (isVarioData && isReceiverData) {
											migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber);
											//System.out.println("isVarioData i = " + i);
											isReceiverData = isVarioData = isGPSData = isGeneralData = isElectricData = isMotorDriverData = false;
										}
										parseVario(1, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2);
										isVarioData = true;
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_GPS_115200:
								case HoTTAdapter.SENSOR_TYPE_GPS_19200:
									if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GPS.ordinal()]) {
										if (isGPSData && isReceiverData) {
											migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber);
											//System.out.println("isGPSData i = " + i);
											isReceiverData = isVarioData = isGPSData = isGeneralData = isElectricData = isMotorDriverData = false;
										}
										parseGPS(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3);
										isGPSData = true;
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
								case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
									if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()]) {
										if (isGeneralData && isReceiverData) {
											migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber);
											//System.out.println("isGeneralData i = " + i);
											isReceiverData = isVarioData = isGPSData = isGeneralData = isElectricData = false;
										}
										parseGeneral(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
										isGeneralData = true;
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
									if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.EAM.ordinal()]) {
										if (isElectricData && isReceiverData) {
											migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber);
											//System.out.println("isElectricData i = " + i);
											isReceiverData = isVarioData = isGPSData = isGeneralData = isElectricData = isMotorDriverData = false;
										}
										parseElectric(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
										isElectricData = true;
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
								case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
									if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()]) {
										if (isMotorDriverData && isReceiverData) {
											migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber);
											//System.out.println("isElectricData i = " + i);
											isReceiverData = isVarioData = isGPSData = isGeneralData = isElectricData = isMotorDriverData = false;
										}
										parseSpeedControl(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, channelNumber);
										isMotorDriverData = true;
									}
									break;
								}

								if (HoTTbinReader2.logger.isLoggable(java.util.logging.Level.FINE))
									HoTTbinReader2.logger.log(java.util.logging.Level.FINE, "isReceiverData " + isReceiverData + " isVarioData " + isVarioData + " isGPSData " + isGPSData + " isGeneralData "
											+ isGeneralData + " isElectricData " + isElectricData);

							}

							if (HoTTbinReader2.logger.isLoggable(java.util.logging.Level.FINE))
								HoTTbinReader2.logger.logp(java.util.logging.Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "logCountVario = " + logCountVario + " logCountGPS = " + logCountGPS
										+ " logCountGeneral = " + logCountGeneral + " logCountElectric = " + logCountElectric);
							lastSensor = actualSensor;
							logCountVario = logCountGPS = logCountGeneral = logCountElectric = 0;
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
							++logCountMotorDriver;
							break;
						}

						if (HoTTbinReader2.isJustMigrated && HoTTbinReader.countLostPackages > 0) {
							HoTTbinReader.lostPackages.add(HoTTbinReader.countLostPackages);
							HoTTbinReader.countLostPackages = 0;
						}

						if (isReceiverData && (logCountVario > 0 || logCountGPS > 0 || logCountGeneral > 0 || logCountElectric > 0 || logCountMotorDriver > 0)) {
							HoTTbinReader2.recordSet.addPoints(HoTTbinReader2.points, HoTTbinReader.timeStep_ms);
							//System.out.println("isReceiverData i = " + i);
							isReceiverData = false;
						}
						else if (channelNumber == 4 && !HoTTbinReader2.isJustMigrated) {
							HoTTbinReader2.recordSet.addPoints(HoTTbinReader2.points, HoTTbinReader.timeStep_ms);
						}
						HoTTbinReader2.isJustMigrated = false;

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

						HoTTbinReader.timeStep_ms += 10;// add default time step from log record of 10 msec

						if (menuToolBar != null && i % progressIndicator == 0) HoTTbinReader.application.setProgress((int) (i * 100 / numberDatablocks), sThreadId);
					}
					else { //skip empty block, but add time step
						HoTTAdapter.reverseChannelPackageLossCounter.add(0);
						HoTTbinReader2.points[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;

						++countPackageLoss; // add up lost packages in telemetry data 
						++HoTTbinReader.countLostPackages;
						//HoTTbinReader2.points[0] = (int) (countPackageLoss*100.0 / ((HoTTbinReader2.timeStep_ms+10) / 10.0)*1000.0); 

						if (channelNumber == 4) {
							parseChannel(HoTTbinReader.buf); //Channels
							HoTTbinReader2.recordSet.addPoints(HoTTbinReader2.points, HoTTbinReader.timeStep_ms);
						}
						HoTTbinReader.timeStep_ms += 10;
						//reset buffer to avoid mixing data
						logCountVario = logCountGPS = logCountGeneral = logCountElectric = logCountMotorDriver = 0;
					}
				}
				else if (!isTextModusSignaled) {
					isTextModusSignaled = true; 
					application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			String packageLossPercentage = tmpRecordSet.getRecordDataSize(true) > 0 ? String.format("%.1f", (countPackageLoss / tmpRecordSet.getTime_ms(tmpRecordSet.getRecordDataSize(true) - 1) * 1000))	: "0";
			tmpRecordSet.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] { countPackageLoss, packageLossPercentage, HoTTbinReader.lostPackages.getStatistics() })
					+ HoTTbinReader.sensorSignature);
			HoTTbinReader2.logger.logp(java.util.logging.Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReader2.logger.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (menuToolBar != null) {
				HoTTbinReader.application.setProgress(99, sThreadId);
				if (!isInitialSwitched) {
					HoTTbinReader.channels.switchChannel(channel.getName());
					channel.switchRecordSet(recordSetName);
				}
				else {
					device.makeInActiveDisplayable(HoTTbinReader2.recordSet);
				}
				device.updateVisibilityStatus(HoTTbinReader2.recordSet, true);

				//write filename after import to record description
				HoTTbinReader2.recordSet.descriptionAppendFilename(file.getName());

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
	public static void migrateAddPoints(boolean isVarioData, boolean isGPSData, boolean isGeneralData, boolean isElectricData, boolean isMotorDriverData, int channelNumber)
			throws DataInconsitsentException {
		//receiver data gets integrated each cycle 0=RXSQ, 1=Height, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
		//8=Height, 9=Climb 1, 10=Climb 3
		//35=VoltageGen, 36=CurrentGen, 37=CapacityGen, 38=PowerGen, 39=BalanceGen, 40=CellVoltageGen 1, 41=CellVoltageGen 2 .... 53=CellVoltageGen 14, 54=VoltageGen 1, 55=VoltageGen 2, 56=TemperatureGen 1, 57=TemperatureGen 2 
		if (isElectricData) {
			if ((HoTTbinReader2.points[8] != 0 && HoTTbinReader.pointsElectric[8] != 0) || HoTTbinReader.pointsElectric[8] != 0) HoTTbinReader2.points[8] = HoTTbinReader.pointsElectric[8];
			for (int j = 9; j < 11; j++) {
				HoTTbinReader2.points[j] = HoTTbinReader.pointsElectric[j];
			}
			for (int k = 35; k < 58; k++) {
				HoTTbinReader2.points[k] = HoTTbinReader.pointsElectric[k];
			}
			if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()] == false) HoTTbinReader2.points[29] = HoTTbinReader.pointsGeneral[29];
		}
		//8=Height, 9=Climb 1, 10=Climb 3
		//18=VoltageGen, 19=CurrentGen, 20=CapacityGen, 21=PowerGen, 22=BalanceGen, 23=CellVoltageGen 1, 24=CellVoltageGen 2 .... 28=CellVoltageGen 6, 29=Revolution, 30=FuelLevel, 31=VoltageGen 1, 32=VoltageGen 2, 33=TemperatureGen 1, 34=TemperatureGen 2
		if (isGeneralData) {
			if ((HoTTbinReader2.points[8] != 0 && HoTTbinReader.pointsGeneral[8] != 0) || HoTTbinReader.pointsGeneral[8] != 0) HoTTbinReader2.points[8] = HoTTbinReader.pointsGeneral[8];
			for (int j = 9; j < 11; j++) {
				HoTTbinReader2.points[j] = HoTTbinReader.pointsGeneral[j];
			}
			for (int k = 18; k < 35; k++) {
				HoTTbinReader2.points[k] = HoTTbinReader.pointsGeneral[k];
			}
		}
		//8=Height, 9=Climb 1, 10=Climb 3
		//12=Latitude, 13=Longitude, 14=Velocity, 15=DistanceStart, 16=DirectionStart, 17=TripDistance
		if (isGPSData) {
			for (int j = 8; j < 11; j++) {
				HoTTbinReader2.points[j] = HoTTbinReader.pointsGPS[j];
			}
			for (int k = 12; k < 18; k++) {
				HoTTbinReader2.points[k] = HoTTbinReader.pointsGPS[k];
			}
		}
		//8=Height, 9=Climb 1, 10=Climb 3, 11=Climb 10
		if (isVarioData && (HoTTbinReader.pointsVario[8] != 0 || HoTTbinReader.pointsVario[9] != 0 || HoTTbinReader.pointsVario[10] != 0 || HoTTbinReader.pointsVario[11] != 0)) {
			for (int j = 8; j < 12; j++) {
				HoTTbinReader2.points[j] = HoTTbinReader.pointsVario[j];
			}
		}
		if (isMotorDriverData) {
			if (channelNumber == 4)
				//78=VoltageM, 79=CurrentM, 80=CapacityM, 81=PowerM, 82=RevolutionM, 83=TemperatureM
				for (int j = 78; j < 78 + 6 && j < HoTTbinReader2.points.length; j++) {
					HoTTbinReader2.points[j] = HoTTbinReader.pointsSpeedControl[j];
				}
			else
				//58=VoltageM, 59=CurrentM, 60=CapacityM, 61=PowerM, 62=RevolutionM, 63=TemperatureM
				for (int j = 58; j < 58 + 6 && j < HoTTbinReader2.points.length; j++) {
					HoTTbinReader2.points[j] = HoTTbinReader.pointsSpeedControl[j];
				}
		}
		HoTTbinReader2.recordSet.addPoints(HoTTbinReader2.points, HoTTbinReader.timeStep_ms);
		HoTTbinReader2.isJustMigrated = true;
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 * @param _buf
	 */
	private static void parseReceiver(byte[] _buf) {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 
		HoTTbinReader.tmpVoltageRx = (_buf[35] & 0xFF);
		HoTTbinReader.tmpTemperatureRx = (_buf[36] & 0xFF);
		HoTTbinReader2.points[1] = (_buf[38] & 0xFF) * 1000;
		HoTTbinReader2.points[3] = DataParser.parse2Short(_buf, 40) * 1000;
		if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpVoltageRx > -1 && HoTTbinReader.tmpVoltageRx < 100 && HoTTbinReader.tmpTemperatureRx < 100) {
			HoTTbinReader2.points[2] = (convertRxDbm2Strength(_buf[4] & 0xFF)) * 1000;
			HoTTbinReader2.points[4] = (_buf[3] & 0xFF) * -1000;
			HoTTbinReader2.points[5] = (_buf[4] & 0xFF) * -1000;
			HoTTbinReader2.points[6] = (_buf[35] & 0xFF) * 1000;
			HoTTbinReader2.points[7] = ((_buf[36] & 0xFF) - 20) * 1000;
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 2 and add points to record set
	 * @param sdLogVersion
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 */
	private static int parseVario(int sdLogVersion, byte[] _buf0, byte[] _buf1, byte[] _buf2) {
//		if (sdLogVersion == -1) sdLogVersion = getSdLogVerion(_buf1, _buf2);
//		switch (sdLogVersion) {
//		case 3:
//			//0=RXSQ, 1=Height, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
//			//8=Height, 9=Climb 1, 10=Climb 3, 11=Climb 10
//			HoTTbinReader.pointsVario[8] = (DataParser.parse2Short(_buf1, 3) - 500) * 1000;
//			//pointsVario[0]max = DataParser.parse2Short(_buf1, 5) * 1000;
//			//pointsVario[0]min = DataParser.parse2Short(_buf1, 7) * 1000;
//			HoTTbinReader.pointsVario[9] = (DataParser.parse2UnsignedShort(_buf1[9], _buf2[0]) - 30000) * 10;
//			HoTTbinReader.pointsVario[10] = (DataParser.parse2UnsignedShort(_buf1[1], _buf2[2]) - 30000) * 10;
//			HoTTbinReader.pointsVario[11] = (DataParser.parse2UnsignedShort(_buf1[3], _buf2[4]) - 30000) * 10;
//			break;
//		default:
//		case 4:
			//0=RXSQ, 1=Height, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
			//8=Height, 9=Climb 1, 10=Climb 3, 11=Climb 10
			HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf1, 2) - 500;
			if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpHeight >= -490 && HoTTbinReader.tmpHeight < 5000)) {
				HoTTbinReader.pointsVario[8] = HoTTbinReader.tmpHeight * 1000;
				//pointsVarioMax = DataParser.parse2Short(buf1, 4) * 1000;
				//pointsVarioMin = DataParser.parse2Short(buf1, 6) * 1000;
				HoTTbinReader.pointsVario[9] = (DataParser.parse2UnsignedShort(_buf1, 8) - 30000) * 10;
			}
			HoTTbinReader.tmpClimb10 = DataParser.parse2UnsignedShort(_buf2, 2) - 30000;
			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpClimb10 > -10000 && HoTTbinReader.tmpClimb10 < 10000) {
				HoTTbinReader.pointsVario[10] = (DataParser.parse2UnsignedShort(_buf2, 0) - 30000) * 10;
				HoTTbinReader.pointsVario[11] = HoTTbinReader.tmpClimb10 * 10;
			}
//			break;
//		}
		return sdLogVersion;
	}

	/**
	 * parse the buffered data from buffer 0 to 3 and add points to record set
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 */
	private static void parseGPS(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3) {
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf2, 8) - 500;
		HoTTbinReader.tmpClimb1 = (DataParser.parse2UnsignedShort(_buf3, 0) - 30000);
		HoTTbinReader.tmpClimb3 = (_buf3[2] & 0xFF) - 120;
		HoTTbinReader.tmpVelocity = DataParser.parse2Short(_buf1, 4) * 1000;
		if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpClimb1 > -20000 && HoTTbinReader.tmpClimb3 > -90 && HoTTbinReader.tmpHeight >= -490 && HoTTbinReader.tmpHeight < 5000)) {
			//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb1, 5=Climb3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripLength, 10=VoltageRx, 11=TemperatureRx
			//8=Height, 9=Climb1, 10=Climb3
			//12=Latitude, 13=Longitude, 14=Velocity, 15=DistanceStart, 16=DirectionStart, 17=TripDistance
			HoTTbinReader.pointsGPS[14] = HoTTAdapter.isFilterEnabled && tmpVelocity > 2000000 ? HoTTbinReader.pointsGPS[14] : tmpVelocity;

			HoTTbinReader.tmpLatitude = DataParser.parse2Short(_buf1, 7) * 10000 + DataParser.parse2Short(_buf1[9], _buf2[0]);
			if (!HoTTAdapter.isTolerateSignChangeLatitude) HoTTbinReader.tmpLatitude = _buf1[6] == 1 ? -1 * HoTTbinReader.tmpLatitude : HoTTbinReader.tmpLatitude;
			HoTTbinReader.tmpLatitudeDelta = Math.abs(HoTTbinReader.tmpLatitude - HoTTbinReader.pointsGPS[12]);
			HoTTbinReader.tmpLatitudeDelta = HoTTbinReader.tmpLatitudeDelta > 400000 ? HoTTbinReader.tmpLatitudeDelta - 400000 : HoTTbinReader.tmpLatitudeDelta;
			HoTTbinReader.latitudeTolerance = (HoTTbinReader.pointsGPS[14] / 1000.0) * (HoTTbinReader.timeStep_ms - HoTTbinReader.lastLatitudeTimeStep) / HoTTAdapter.latitudeToleranceFactor;
			HoTTbinReader.latitudeTolerance = HoTTbinReader.latitudeTolerance > 0 ? HoTTbinReader.latitudeTolerance : 5;

			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.pointsGPS[12] == 0 || HoTTbinReader.tmpLatitudeDelta <= HoTTbinReader.latitudeTolerance) {
				HoTTbinReader.lastLatitudeTimeStep = HoTTbinReader.timeStep_ms;
				HoTTbinReader.pointsGPS[12] = HoTTbinReader.tmpLatitude;
			}
			else {
				if (HoTTbinReader.log.isLoggable(java.util.logging.Level.FINE))
					HoTTbinReader.log.log(java.util.logging.Level.FINE, StringHelper.getFormatedTime("HH:mm:ss:SSS", HoTTbinReader.timeStep_ms - GDE.ONE_HOUR_MS) + " Lat " + HoTTbinReader.tmpLatitude + " - "
							+ HoTTbinReader.tmpLatitudeDelta);
			}

			HoTTbinReader.tmpLongitude = DataParser.parse2Short(_buf2, 2) * 10000 + DataParser.parse2Short(_buf2, 4);
			if (!HoTTAdapter.isTolerateSignChangeLongitude) HoTTbinReader.tmpLongitude = _buf2[1] == 1 ? -1 * HoTTbinReader.tmpLongitude : HoTTbinReader.tmpLongitude;
			HoTTbinReader.tmpLongitudeDelta = Math.abs(HoTTbinReader.tmpLongitude - HoTTbinReader.pointsGPS[13]);
			HoTTbinReader.tmpLongitudeDelta = HoTTbinReader.tmpLongitudeDelta > 400000 ? HoTTbinReader.tmpLongitudeDelta - 400000 : HoTTbinReader.tmpLongitudeDelta;
			HoTTbinReader.longitudeTolerance = (HoTTbinReader.pointsGPS[14] / 1000.0) * (HoTTbinReader.timeStep_ms - HoTTbinReader.lastLongitudeTimeStep) / HoTTAdapter.longitudeToleranceFactor;
			HoTTbinReader.longitudeTolerance = HoTTbinReader.longitudeTolerance > 0 ? HoTTbinReader.longitudeTolerance : 5;

			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.pointsGPS[13] == 0 || HoTTbinReader.tmpLongitudeDelta <= HoTTbinReader.longitudeTolerance) {
				HoTTbinReader.lastLongitudeTimeStep = HoTTbinReader.timeStep_ms;
				HoTTbinReader.pointsGPS[13] = HoTTbinReader.tmpLongitude;
			}
			else {
				if (HoTTbinReader.log.isLoggable(java.util.logging.Level.FINE))
					HoTTbinReader.log.log(java.util.logging.Level.FINE, StringHelper.getFormatedTime("HH:mm:ss:SSS", HoTTbinReader.timeStep_ms - GDE.ONE_HOUR_MS) + " Long " + HoTTbinReader.tmpLongitude + " - "
							+ HoTTbinReader.tmpLongitudeDelta);
			}

			HoTTbinReader.pointsGPS[8] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsGPS[9] = HoTTbinReader.tmpClimb1 * 10;
			HoTTbinReader.pointsGPS[10] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsGPS[15] = DataParser.parse2Short(_buf2, 6) * 1000;
			HoTTbinReader.pointsGPS[16] = (_buf1[3] & 0xFF) * 1000;
			HoTTbinReader.pointsGPS[17] = 0;
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 */
	private static void parseGeneral(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) {
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf3, 0) - 500;
		HoTTbinReader.tmpClimb3 = (_buf3[4] & 0xFF) - 120;
		HoTTbinReader.tmpVoltage1 = DataParser.parse2Short(_buf1[9], _buf2[0]);
		HoTTbinReader.tmpVoltage2 = DataParser.parse2Short(_buf2, 1);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
		//0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Height, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2							
		//8=Height, 9=Climb 1, 10=Climb 3
		//18=VoltageGen, 19=CurrentGen, 20=CapacityGen, 21=PowerGen, 22=BalanceGen, 23=CellVoltageGen 1, 24=CellVoltageGen 2 .... 28=CellVoltageGen 6, 29=Revolution, 30=FuelLevel, 31=VoltageGen 1, 32=VoltageGen 2, 33=TemperatureGen 1, 34=TemperatureGen 2
		if (!HoTTAdapter.isFilterEnabled
				|| (HoTTbinReader.tmpClimb3 > -90 && HoTTbinReader.tmpHeight >= -490 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600 && Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			HoTTbinReader.pointsGeneral[18] = DataParser.parse2Short(_buf3, 7) * 1000;
			HoTTbinReader.pointsGeneral[19] = DataParser.parse2Short(_buf3, 5) * 1000;
			HoTTbinReader.pointsGeneral[20] = HoTTAdapter.isFilterEnabled && HoTTbinReader.tmpCapacity < HoTTbinReader.pointsGeneral[20] / 1000 ? HoTTbinReader.pointsGeneral[20]
					: HoTTbinReader.tmpCapacity * 1000;
			HoTTbinReader.pointsGeneral[21] = Double.valueOf(HoTTbinReader.pointsGeneral[18] / 1000.0 * HoTTbinReader.pointsGeneral[19]).intValue();
			for (int j = 0; j < 6; j++) {
				HoTTbinReader.pointsGeneral[j + 23] = (_buf1[3 + j] & 0xFF) * 1000;
				if (HoTTbinReader.pointsGeneral[j + 23] > 0) {
					maxVotage = HoTTbinReader.pointsGeneral[j + 23] > maxVotage ? HoTTbinReader.pointsGeneral[j + 23] : maxVotage;
					minVotage = HoTTbinReader.pointsGeneral[j + 23] < minVotage ? HoTTbinReader.pointsGeneral[j + 23] : minVotage;
				}
			}
			HoTTbinReader.pointsGeneral[22] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
			HoTTbinReader.pointsGeneral[29] = DataParser.parse2Short(_buf2, 8) * 1000;
			HoTTbinReader.pointsGeneral[30] = DataParser.parse2Short(_buf2, 6) * 1000;
			HoTTbinReader.pointsGeneral[8] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsGeneral[9] = (DataParser.parse2UnsignedShort(_buf3, 2) - 30000) * 10;
			HoTTbinReader.pointsGeneral[10] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsGeneral[31] = HoTTbinReader.tmpVoltage1 * 100;
			HoTTbinReader.pointsGeneral[32] = HoTTbinReader.tmpVoltage2 * 100;
			HoTTbinReader.pointsGeneral[33] = ((_buf2[3] & 0xFF) - 20) * 1000;
			HoTTbinReader.pointsGeneral[34] = ((_buf2[4] & 0xFF) - 20) * 1000;
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
	private static void parseElectric(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) throws DataInconsitsentException {
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf3, 3) - 500;
		HoTTbinReader.tmpClimb3 = (_buf4[3] & 0xFF) - 120;
		HoTTbinReader.tmpVoltage1 = DataParser.parse2Short(_buf2, 7);
		HoTTbinReader.tmpVoltage2 = DataParser.parse2Short(_buf2[9], _buf3[0]);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
		//0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2 
		//8=Height, 9=Climb 1, 10=Climb 3
		//35=VoltageGen, 36=CurrentGen, 37=CapacityGen, 38=PowerGen, 39=BalanceGen, 40=CellVoltageGen 1, 41=CellVoltageGen 2 .... 53=CellVoltageGen 14, 54=VoltageGen 1, 55=VoltageGen 2, 56=TemperatureGen 1, 57=TemperatureGen 2 
		if (!HoTTAdapter.isFilterEnabled
				|| (HoTTbinReader.tmpClimb3 > -90 && HoTTbinReader.tmpHeight >= -490 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600 && Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			HoTTbinReader.pointsElectric[35] = DataParser.parse2Short(_buf3, 7) * 1000;
			HoTTbinReader.pointsElectric[36] = DataParser.parse2Short(_buf3, 5) * 1000;
			HoTTbinReader.pointsElectric[37] = HoTTAdapter.isFilterEnabled && HoTTbinReader.tmpCapacity < HoTTbinReader.pointsElectric[37] / 1000 ? HoTTbinReader.pointsElectric[37]
					: HoTTbinReader.tmpCapacity * 1000;
			HoTTbinReader.pointsElectric[38] = Double.valueOf(HoTTbinReader.pointsElectric[35] / 1000.0 * HoTTbinReader.pointsElectric[36]).intValue(); // power U*I [W];
			for (int j = 0; j < 7; j++) {
				HoTTbinReader.pointsElectric[j + 40] = (_buf1[3 + j] & 0xFF) * 1000;
				if (HoTTbinReader.pointsElectric[j + 40] > 0) {
					maxVotage = HoTTbinReader.pointsElectric[j + 40] > maxVotage ? HoTTbinReader.pointsElectric[j + 40] : maxVotage;
					minVotage = HoTTbinReader.pointsElectric[j + 40] < minVotage ? HoTTbinReader.pointsElectric[j + 40] : minVotage;
				}
			}
			for (int j = 0; j < 7; j++) {
				HoTTbinReader.pointsElectric[j + 47] = (_buf2[j] & 0xFF) * 1000;
				if (HoTTbinReader.pointsElectric[j + 13] > 0) {
					maxVotage = HoTTbinReader.pointsElectric[j + 47] > maxVotage ? HoTTbinReader.pointsElectric[j + 47] : maxVotage;
					minVotage = HoTTbinReader.pointsElectric[j + 47] < minVotage ? HoTTbinReader.pointsElectric[j + 47] : minVotage;
				}
			}
			//calculate balance on the fly
			HoTTbinReader.pointsElectric[39] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
			HoTTbinReader.pointsElectric[8] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsElectric[9] = (DataParser.parse2UnsignedShort(_buf4, 1) - 30000) * 10;
			HoTTbinReader.pointsElectric[10] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsElectric[54] = HoTTbinReader.tmpVoltage1 * 100;
			HoTTbinReader.pointsElectric[55] = HoTTbinReader.tmpVoltage2 * 100;
			HoTTbinReader.pointsElectric[56] = ((_buf3[1] & 0xFF) - 20) * 1000;
			HoTTbinReader.pointsElectric[57] = ((_buf3[2] & 0xFF) - 20) * 1000;
			if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()] == false) HoTTbinReader.pointsGeneral[29] = DataParser.parse2Short(_buf4, 4) * 1000;
		}
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 * @param _buf
	 */
	private static void parseChannel(byte[] _buf) {
		//0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 
		HoTTbinReader2.points[4] = (_buf[3] & 0xFF) * -1000;
		HoTTbinReader2.points[5] = (_buf[4] & 0xFF) * -1000;
		//58=Ch 1, 59=Ch 2 , 50=Ch 3 .. 73=Ch 16
		HoTTbinReader2.points[58] = (DataParser.parse2UnsignedShort(_buf, 8) / 2) * 1000; //1197
		HoTTbinReader2.points[59] = (DataParser.parse2UnsignedShort(_buf, 10) / 2) * 1000; //
		HoTTbinReader2.points[60] = (DataParser.parse2UnsignedShort(_buf, 12) / 2) * 1000;
		HoTTbinReader2.points[61] = (DataParser.parse2UnsignedShort(_buf, 14) / 2) * 1000;
		HoTTbinReader2.points[62] = (DataParser.parse2UnsignedShort(_buf, 16) / 2) * 1000;
		HoTTbinReader2.points[63] = (DataParser.parse2UnsignedShort(_buf, 18) / 2) * 1000;
		HoTTbinReader2.points[64] = (DataParser.parse2UnsignedShort(_buf, 20) / 2) * 1000;
		HoTTbinReader2.points[65] = (DataParser.parse2UnsignedShort(_buf, 22) / 2) * 1000;
		HoTTbinReader2.points[74] = (_buf[50] & 0x01) * 100000;
		HoTTbinReader2.points[75] = (_buf[50] & 0x02) * 50000;
		HoTTbinReader2.points[76] = (_buf[50] & 0x04) * 25000;
		HoTTbinReader2.points[77] = (_buf[50] & 0x00) * 1000; //reserved for future use

		if (_buf[5] == 0x00) { //channel 9-12
			HoTTbinReader2.points[66] = (DataParser.parse2UnsignedShort(_buf, 24) / 2) * 1000;
			HoTTbinReader2.points[67] = (DataParser.parse2UnsignedShort(_buf, 26) / 2) * 1000;
			HoTTbinReader2.points[68] = (DataParser.parse2UnsignedShort(_buf, 28) / 2) * 1000;
			HoTTbinReader2.points[69] = (DataParser.parse2UnsignedShort(_buf, 30) / 2) * 1000;
			if (HoTTbinReader2.points[70] == 0) {
				HoTTbinReader2.points[70] = 1500 * 1000;
				HoTTbinReader2.points[71] = 1500 * 1000;
				HoTTbinReader2.points[72] = 1500 * 1000;
				HoTTbinReader2.points[73] = 1500 * 1000;
			}
		}
		else { //channel 13-16
			HoTTbinReader2.points[70] = (DataParser.parse2UnsignedShort(_buf, 24) / 2) * 1000;
			HoTTbinReader2.points[71] = (DataParser.parse2UnsignedShort(_buf, 26) / 2) * 1000;
			HoTTbinReader2.points[72] = (DataParser.parse2UnsignedShort(_buf, 28) / 2) * 1000;
			HoTTbinReader2.points[73] = (DataParser.parse2UnsignedShort(_buf, 30) / 2) * 1000;
			if (HoTTbinReader2.points[66] == 0) {
				HoTTbinReader2.points[66] = 1500 * 1000;
				HoTTbinReader2.points[67] = 1500 * 1000;
				HoTTbinReader2.points[68] = 1500 * 1000;
				HoTTbinReader2.points[69] = 1500 * 1000;
			}
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param channelNumber
	 * @throws DataInconsitsentException
	 */
	private static void parseSpeedControl(byte[] _buf0, byte[] _buf1, byte[] _buf2, int channelNumber) throws DataInconsitsentException {
		HoTTbinReader.tmpVoltage = DataParser.parse2Short(_buf1, 3);
		HoTTbinReader.tmpCurrent = DataParser.parse2Short(_buf2, 1);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf1, 7);
		HoTTbinReader.tmpRevolution = DataParser.parse2Short(_buf2, 5);
		HoTTbinReader.tmpTemperatureFet = _buf1[9];
		if (channelNumber == 4) {
			//78=VoltageM, 79=CurrentM, 80=CapacityM, 81=PowerM, 82=RevolutionM, 83=TemperatureM
			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpVoltage > 0 && HoTTbinReader.tmpVoltage < 1000 && HoTTbinReader.tmpCurrent < 1000 && HoTTbinReader.tmpRevolution > -1
					&& HoTTbinReader.tmpRevolution < 2000) {
				HoTTbinReader.pointsSpeedControl[78] = HoTTbinReader.tmpVoltage * 1000;
				HoTTbinReader.pointsSpeedControl[79] = HoTTbinReader.tmpCurrent * 1000;
				HoTTbinReader.pointsSpeedControl[81] = Double.valueOf(HoTTbinReader.pointsSpeedControl[78] / 1000.0 * HoTTbinReader.pointsSpeedControl[79]).intValue();
				HoTTbinReader.pointsSpeedControl[80] = HoTTAdapter.isFilterEnabled && HoTTbinReader.tmpCapacity < HoTTbinReader.pointsSpeedControl[80] / 1000 ? HoTTbinReader.pointsSpeedControl[80]
						: HoTTbinReader.tmpCapacity * 1000;
				HoTTbinReader.pointsSpeedControl[82] = HoTTbinReader.tmpRevolution * 1000;
				HoTTbinReader.pointsSpeedControl[83] = (HoTTbinReader.tmpTemperatureFet - 20) * 1000;
			}
		}
		else {
			//58=VoltageM, 59=CurrentM, 60=CapacityM, 61=PowerM, 62=RevolutionM, 63=TemperatureM
			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpVoltage > 0 && HoTTbinReader.tmpVoltage < 1000 && HoTTbinReader.tmpCurrent < 1000 && HoTTbinReader.tmpRevolution > -1
					&& HoTTbinReader.tmpRevolution < 2000) {
				HoTTbinReader.pointsSpeedControl[58] = HoTTbinReader.tmpVoltage * 1000;
				HoTTbinReader.pointsSpeedControl[59] = HoTTbinReader.tmpCurrent * 1000;
				HoTTbinReader.pointsSpeedControl[61] = Double.valueOf(HoTTbinReader.pointsSpeedControl[58] / 1000.0 * HoTTbinReader.pointsSpeedControl[59]).intValue();
				HoTTbinReader.pointsSpeedControl[60] = HoTTAdapter.isFilterEnabled && HoTTbinReader.tmpCapacity < HoTTbinReader.pointsSpeedControl[60] / 1000 ? HoTTbinReader.pointsSpeedControl[60]
						: HoTTbinReader.tmpCapacity * 1000;
				HoTTbinReader.pointsSpeedControl[62] = HoTTbinReader.tmpRevolution * 1000;
				HoTTbinReader.pointsSpeedControl[63] = (HoTTbinReader.tmpTemperatureFet - 20) * 1000;
			}
		}
	}
}
