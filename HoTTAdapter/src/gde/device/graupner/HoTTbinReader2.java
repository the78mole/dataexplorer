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

    Copyright (c) 2011,2012,2013,2014,2015,2016,2017,2018 Winfried Bruegmann
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
import gde.device.IDevice;
import gde.device.graupner.HoTTAdapter.PickerParameters;
import gde.device.graupner.HoTTAdapter.Sensor;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;

/**
 * Class to read Graupner HoTT binary data as saved on SD-Cards
 * @author Winfried Brügmann
 */
public class HoTTbinReader2 extends HoTTbinReader {
	final static Logger	log							= Logger.getLogger(HoTTbinReader2.class.getName());
	static int[]				points;
	static RecordSet		recordSet;
	static boolean			isJustMigrated				= false;

	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws Exception
	 */
	public static synchronized void read(String filePath, PickerParameters newPickerParameters) throws Exception {
		HoTTbinReader2.pickerParameters = newPickerParameters;
		HashMap<String, String> header = getFileInfo(new File(filePath), newPickerParameters);
		HoTTbinReader2.detectedSensors = Sensor.getSetFromDetected(header.get(HoTTAdapter.DETECTED_SENSOR));

		if (HoTTbinReader2.detectedSensors.size() <= 2) {
			HoTTbinReader.isReceiverOnly = HoTTbinReader2.detectedSensors.size() == 1;
			readSingle(new File(header.get(HoTTAdapter.FILE_PATH)));
		}
		else
			readMultiple(new File(header.get(HoTTAdapter.FILE_PATH)));
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
		IDevice device = HoTTbinReader.application.getActiveDevice();
		int recordSetNumber = HoTTbinReader.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		int channelNumber = device.getLastChannelNumber();
		device.getMeasurementFactor(channelNumber, 12);
		boolean isReceiverData = false;
		boolean isSensorData = false;
		HoTTbinReader2.recordSet = null;
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
		//46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14, 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
		//73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max, 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M

		//73=Ch 1, 74=Ch 2, 75=Ch 3 .. 88=Ch 16, 89=PowerOff, 90=BatterieLow, 91=Reset, 92=reserve
		//93=VoltageM, 94=CurrentM, 95=CapacityM, 96=PowerM, 97=RevolutionM, 98=TemperatureM 1, 99=TemperatureM 2 100=Voltage_min, 101=Current_max, 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M
		HoTTbinReader2.points = new int[device.getNumberOfMeasurements(channelNumber)];
		HoTTbinReader2.points[2] = 0;
		HoTTbinReader.pointsGAM = HoTTbinReader.pointsEAM = HoTTbinReader.pointsESC = HoTTbinReader.pointsVario = HoTTbinReader.pointsGPS = HoTTbinReader2.points;
		HoTTbinReader.timeStep_ms = 0;
		HoTTbinReader.dataBlockSize = 64;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		HoTTbinReader.buf0 = new byte[30];
		HoTTbinReader.buf1 = null;
		HoTTbinReader.buf2 = null;
		HoTTbinReader.buf3 = null;
		HoTTbinReader.buf4 = null;
		pickerParameters.reverseChannelPackageLossCounter.clear();
		HoTTbinReader.lostPackages.clear();
		HoTTbinReader.countLostPackages = 0;
		HoTTbinReader.isTextModusSignaled = false;
		int countPackageLoss = 0;
		long numberDatablocks = fileSize / HoTTbinReader.dataBlockSize;
		long startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks);
		numberDatablocks = HoTTbinReader.isReceiverOnly && channelNumber != 4 ? numberDatablocks/10 : numberDatablocks;
		String date = StringHelper.getDate();
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		MenuToolBar menuToolBar = HoTTbinReader.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		GDE.getUiNotification().setProgress(0);

		try {
			//check if recordSet initialized, transmitter and receiver data always present, but not in the same data rate and signals
			channel = HoTTbinReader.channels.get(channelNumber);
			channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
			recordSetName = recordSetNumber + device.getRecordSetStemNameReplacement() + recordSetNameExtend;
			HoTTbinReader2.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true, true);
			channel.put(recordSetName, HoTTbinReader2.recordSet);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			//recordSet initialized and ready to add data

			//read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTbinReader2.log.isLoggable(Level.FINE) && i % 10 == 0) {
					HoTTbinReader2.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
					HoTTbinReader2.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if (!pickerParameters.isFilterTextModus || (HoTTbinReader.buf[6] & 0x01) == 0) { //switch into text modus
					if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { //buf 3, 4, tx,rx
						if (HoTTbinReader2.log.isLoggable(Level.FINE))
							HoTTbinReader2.log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", HoTTbinReader.buf[7], HoTTbinReader.buf[33]));

						pickerParameters.reverseChannelPackageLossCounter.add(1);
						HoTTbinReader2.points[0] = pickerParameters.reverseChannelPackageLossCounter.getPercentage() * 1000;

						if (HoTTbinReader2.log.isLoggable(Level.FINER))
							HoTTbinReader2.log.logp(Level.FINER, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1)
									+ GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

						//fill receiver data
						if (HoTTbinReader.buf[33] == 0 && (HoTTbinReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader.buf, 40) >= 0) {
							parseReceiver(HoTTbinReader.buf);
							isReceiverData = true;
						}
						if (channelNumber == 4) {
							parseChannel(HoTTbinReader.buf); //Channels
						}

						//fill data block 0 receiver voltage an temperature
						if (HoTTbinReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader.buf, 0) != 0) {
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf0, 0, HoTTbinReader.buf0.length);
						}
						if (HoTTbinReader.isReceiverOnly && channelNumber != 4) { //reduce data rate for receiver to 0.1 sec
							for (int j = 0; j < 9; j++) {
								data_in.read(HoTTbinReader.buf);
								HoTTbinReader.timeStep_ms += 10;
							}
							isSensorData = true;
						}

						//create and fill sensor specific data record sets
						switch ((byte) (HoTTbinReader.buf[7] & 0xFF)) {
						case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
						case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
							if (detectedSensors.contains(Sensor.VARIO)) {
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
									parseVario(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2);
									HoTTbinReader.buf1 = HoTTbinReader.buf2 = null;
									isSensorData = true;
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_GPS_115200:
						case HoTTAdapter.SENSOR_TYPE_GPS_19200:
							if (detectedSensors.contains(Sensor.GPS)) {
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
							if (detectedSensors.contains(Sensor.GAM)) {
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
									parseGAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
									HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
									isSensorData = true;
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
							if (detectedSensors.contains(Sensor.EAM)) {
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
									parseEAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
									HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
									isSensorData = true;
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
							if (detectedSensors.contains(Sensor.ESC)) {
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
								if (HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null && HoTTbinReader.buf3 != null) {
									parseESC(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, channelNumber);
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

						if (isSensorData || (isReceiverData && HoTTbinReader2.recordSet.get(0).realSize() > 0)) {
							HoTTbinReader2.recordSet.addPoints(HoTTbinReader2.points, HoTTbinReader.timeStep_ms);
							isSensorData = isReceiverData = false;
						}
						else if (channelNumber == 4) {
							HoTTbinReader2.recordSet.addPoints(HoTTbinReader2.points, HoTTbinReader.timeStep_ms);
						}

						HoTTbinReader.timeStep_ms += 10; // add default time step from device of 10 msec

						if (i % progressIndicator == 0) GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));
					}
					else { //skip empty block, but add time step
						if (HoTTbinReader2.log.isLoggable(Level.FINE)) HoTTbinReader2.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

						pickerParameters.reverseChannelPackageLossCounter.add(0);
						HoTTbinReader2.points[0] = pickerParameters.reverseChannelPackageLossCounter.getPercentage() * 1000;

						++countPackageLoss; // add up lost packages in telemetry data
						++HoTTbinReader.countLostPackages;
						//HoTTbinReader2.points[0] = (int) (countPackageLoss*100.0 / ((HoTTbinReader2.timeStep_ms+10) / 10.0)*1000.0);

						if (channelNumber == 4) {
							parseChannel(HoTTbinReader.buf); //Channels
							HoTTbinReader2.recordSet.addPoints(HoTTbinReader2.points, HoTTbinReader.timeStep_ms);
						}
						HoTTbinReader.timeStep_ms += 10;
						//reset buffer to avoid mixing data >> 20 Jul 14, not any longer required due to protocol change requesting next sensor data block
						//HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
					}
				}
				else if (!HoTTbinReader.isTextModusSignaled) {
					HoTTbinReader.isTextModusSignaled = true;
					HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			String packageLossPercentage = tmpRecordSet.getRecordDataSize(true) > 0 ? String.format("%.1f", (countPackageLoss / tmpRecordSet.getTime_ms(tmpRecordSet.getRecordDataSize(true) - 1) * 1000)) : "100";
			tmpRecordSet.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] { countPackageLoss, packageLossPercentage, HoTTbinReader.lostPackages.getStatistics() })
					+ Sensor.getSetAsSignature(HoTTbinReader.detectedSensors));
			HoTTbinReader2.log.logp(Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReader2.log.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (GDE.isWithUi()) {
				GDE.getUiNotification().setProgress(99);
				device.updateVisibilityStatus(HoTTbinReader2.recordSet, true);
				channel.applyTemplate(recordSetName, false);

				//write filename after import to record description
				HoTTbinReader2.recordSet.descriptionAppendFilename(file.getName());

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
		IDevice device = HoTTbinReader.application.getActiveDevice();
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
		HoTTbinReader2.recordSet = null;
		HoTTbinReader2.isJustMigrated = false;
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
		//46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14, 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
		//73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max, 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M

		//73=Ch 1, 74=Ch 2, 75=Ch 3 .. 88=Ch 16, 89=PowerOff, 90=BatterieLow, 91=Reset, 92=reserve
		//93=VoltageM, 94=CurrentM, 95=CapacityM, 96=PowerM, 97=RevolutionM, 98=TemperatureM 1, 99=TemperatureM 2 100=Voltage_min, 101=Current_max, 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M
		HoTTbinReader2.points = new int[device.getNumberOfMeasurements(channelNumber)];
		HoTTbinReader.pointsGAM = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsEAM = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsESC = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsVario = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsVario[2] = 100000;
		HoTTbinReader.pointsGPS = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsChannel = new int[HoTTbinReader2.points.length];
		HoTTbinReader.timeStep_ms = 0;
		HoTTbinReader.dataBlockSize = 64;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		HoTTbinReader.buf0 = new byte[30];
		HoTTbinReader.buf1 = new byte[30];
		HoTTbinReader.buf2 = new byte[30];
		HoTTbinReader.buf3 = new byte[30];
		HoTTbinReader.buf4 = new byte[30];
		byte actualSensor = -1, lastSensor = -1;
		int logCountVario = 0, logCountGPS = 0, logCountGAM = 0, logCountEAM = 0, logCountESC = 0;
		pickerParameters.reverseChannelPackageLossCounter.clear();
		HoTTbinReader.lostPackages.clear();
		HoTTbinReader.countLostPackages = 0;
		HoTTbinReader.isTextModusSignaled = false;
		//		HoTTbinReader.oldProtocolCount = 0;
		//		HoTTbinReader.blockSequenceCheck		= new Vector<Byte>();
		int countPackageLoss = 0;
		long numberDatablocks = fileSize / HoTTbinReader.dataBlockSize;
		long startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks);
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
			HoTTbinReader2.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true, true);
			channel.put(recordSetName, HoTTbinReader2.recordSet);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			//recordSet initialized and ready to add data

			//read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTbinReader2.log.isLoggable(Level.FINEST)) {
					HoTTbinReader2.log.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if (!pickerParameters.isFilterTextModus || (HoTTbinReader.buf[6] & 0x01) == 0) { //switch into text modus
					if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { //buf 3, 4, tx,rx
						if (HoTTbinReader2.log.isLoggable(Level.FINE))
							HoTTbinReader2.log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", HoTTbinReader.buf[7], HoTTbinReader.buf[33]));

						pickerParameters.reverseChannelPackageLossCounter.add(1);
						HoTTbinReader2.points[0] = pickerParameters.reverseChannelPackageLossCounter.getPercentage() * 1000;
						//create and fill sensor specific data record sets
						if (HoTTbinReader2.log.isLoggable(Level.FINEST))
							HoTTbinReader2.log.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1)
									+ GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

						//fill receiver data
						if (HoTTbinReader.buf[33] == 0 && (HoTTbinReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader.buf, 40) >= 0) {
							parseReceiver(HoTTbinReader.buf);
							isReceiverData = true;
						}
						if (channelNumber == 4) {
							parseChannel(HoTTbinReader.buf);
						}

						if (actualSensor == -1)
							lastSensor = actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);
						else
							actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);

						//						switch (actualSensor) {
						//						case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
						//						case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
						//							System.out.print("VARIO, ");
						//							break;
						//						case HoTTAdapter.SENSOR_TYPE_GPS_115200:
						//						case HoTTAdapter.SENSOR_TYPE_GPS_19200:
						//							System.out.print("GPS, ");
						//							break;
						//						case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
						//						case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
						//							System.out.print("GAM, ");
						//							break;
						//						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
						//						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
						//							System.out.print("EAM, ");
						//							break;
						//						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
						//						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
						//							System.out.print("ESC, ");
						//							break;
						//						}

						if (actualSensor != lastSensor) {
							if (logCountVario >= 3 || logCountGPS >= 4 || logCountGAM >= 5 || logCountEAM >= 5 || logCountESC >= 4) {
								switch (lastSensor) {
								case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
								case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
									if (detectedSensors.contains(Sensor.VARIO)) {
										if (isVarioData && isReceiverData) {
											migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber);
											//System.out.println("isVarioData i = " + i);
											isReceiverData = isVarioData = isGPSData = isGeneralData = isElectricData = isMotorDriverData = false;
										}
										parseVario(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2);
										isVarioData = true;
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_GPS_115200:
								case HoTTAdapter.SENSOR_TYPE_GPS_19200:
									if (detectedSensors.contains(Sensor.GPS)) {
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
									if (detectedSensors.contains(Sensor.GAM)) {
										if (isGeneralData && isReceiverData) {
											migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber);
											//System.out.println("isGeneralData i = " + i);
											isReceiverData = isVarioData = isGPSData = isGeneralData = isElectricData = false;
										}
										parseGAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
										isGeneralData = true;
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
									if (detectedSensors.contains(Sensor.EAM)) {
										if (isElectricData && isReceiverData) {
											migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber);
											//System.out.println("isElectricData i = " + i);
											isReceiverData = isVarioData = isGPSData = isGeneralData = isElectricData = isMotorDriverData = false;
										}
										parseEAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
										isElectricData = true;
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
								case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
									if (detectedSensors.contains(Sensor.ESC)) {
										if (isMotorDriverData && isReceiverData) {
											migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, isMotorDriverData, channelNumber);
											//System.out.println("isElectricData i = " + i);
											isReceiverData = isVarioData = isGPSData = isGeneralData = isElectricData = isMotorDriverData = false;
										}
										parseESC(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, channelNumber);
										isMotorDriverData = true;
									}
									break;
								}

								if (HoTTbinReader2.log.isLoggable(Level.FINE))
									HoTTbinReader2.log.log(Level.FINE, "isReceiverData " + isReceiverData + " isVarioData " + isVarioData + " isGPSData " + isGPSData + " isGeneralData "
											+ isGeneralData + " isElectricData " + isElectricData);

							}

							if (HoTTbinReader2.log.isLoggable(Level.FINE))
								HoTTbinReader2.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "logCountVario = " + logCountVario + " logCountGPS = " + logCountGPS
										+ " logCountGeneral = " + logCountGAM + " logCountElectric = " + logCountEAM);
							lastSensor = actualSensor;
							logCountVario = logCountGPS = logCountGAM = logCountEAM = logCountESC = 0;
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
							++logCountGAM;
							break;
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
							++logCountEAM;
							break;
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
							++logCountESC;
							break;
						}

						if (HoTTbinReader2.isJustMigrated && HoTTbinReader.countLostPackages > 0) {
							HoTTbinReader.lostPackages.add(HoTTbinReader.countLostPackages);
							HoTTbinReader.countLostPackages = 0;
						}

						if (isReceiverData && (logCountVario > 0 || logCountGPS > 0 || logCountGAM > 0 || logCountEAM > 0 || logCountESC > 0)) {
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

						//						if (HoTTbinReader.blockSequenceCheck.size() > 1) {
						//							if(HoTTbinReader.blockSequenceCheck.get(0) - HoTTbinReader.blockSequenceCheck.get(1) > 1 && HoTTbinReader.blockSequenceCheck.get(0) - HoTTbinReader.blockSequenceCheck.get(1) < 4)
						//								++HoTTbinReader.oldProtocolCount;
						//							HoTTbinReader.blockSequenceCheck.remove(0);
						//						}
						//						HoTTbinReader.blockSequenceCheck.add(HoTTbinReader.buf[33]);

						HoTTbinReader.timeStep_ms += 10;// add default time step from log record of 10 msec

						if (i % progressIndicator == 0) GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));
					}
					else { //skip empty block, but add time step
						if (HoTTbinReader2.log.isLoggable(Level.FINE)) HoTTbinReader2.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

						pickerParameters.reverseChannelPackageLossCounter.add(0);
						HoTTbinReader2.points[0] = pickerParameters.reverseChannelPackageLossCounter.getPercentage() * 1000;

						++countPackageLoss; // add up lost packages in telemetry data
						++HoTTbinReader.countLostPackages;
						//HoTTbinReader2.points[0] = (int) (countPackageLoss*100.0 / ((HoTTbinReader2.timeStep_ms+10) / 10.0)*1000.0);

						if (channelNumber == 4) {
							parseChannel(HoTTbinReader.buf); //Channels
							HoTTbinReader2.recordSet.addPoints(HoTTbinReader2.points, HoTTbinReader.timeStep_ms);
						}
						HoTTbinReader.timeStep_ms += 10;
						//reset buffer to avoid mixing data >> 20 Jul 14, not any longer required due to protocol change requesting next sensor data block
						//logCountVario = logCountGPS = logCountGeneral = logCountElectric = logCountMotorDriver = 0;
					}
				}
				else if (!HoTTbinReader.isTextModusSignaled) {
					HoTTbinReader.isTextModusSignaled = true;
					HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			//			if (HoTTbinReader.oldProtocolCount > 2) {
			//				application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2405, new Object[] { HoTTbinReader.oldProtocolCount }));
			//			}
			String packageLossPercentage = tmpRecordSet.getRecordDataSize(true) > 0 ? String.format("%.1f", (countPackageLoss / tmpRecordSet.getTime_ms(tmpRecordSet.getRecordDataSize(true) - 1) * 1000))
					: "100";
			tmpRecordSet.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] { countPackageLoss, packageLossPercentage, HoTTbinReader.lostPackages.getStatistics() })
					+ Sensor.getSetAsSignature(HoTTbinReader.detectedSensors));
			HoTTbinReader2.log.logp(Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReader2.log.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (menuToolBar != null) {
				GDE.getUiNotification().setProgress(99);
				device.makeInActiveDisplayable(HoTTbinReader2.recordSet);
				device.updateVisibilityStatus(HoTTbinReader2.recordSet, true);
				channel.applyTemplate(recordSetName, false);

				//write filename after import to record description
				HoTTbinReader2.recordSet.descriptionAppendFilename(file.getName());

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
	public static void migrateAddPoints(boolean isVarioData, boolean isGPSData, boolean isGeneralData, boolean isElectricData, boolean isMotorDriverData, int channelNumber)
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
			if ((HoTTbinReader2.points[10] != 0 && HoTTbinReader.pointsEAM[10] != 0) || HoTTbinReader.pointsEAM[10] != 0) HoTTbinReader2.points[10] = HoTTbinReader.pointsEAM[10];
			for (int j = 10; j < 13; j++) {
				HoTTbinReader2.points[j] = HoTTbinReader.pointsEAM[j];
			}
			for (int k = 46; k < 73; k++) {
				HoTTbinReader2.points[k] = HoTTbinReader.pointsEAM[k];
			}
		}
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
		if (isGeneralData) {
			if ((HoTTbinReader2.points[10] != 0 && HoTTbinReader.pointsGAM[10] != 0) || HoTTbinReader.pointsGAM[10] != 0) HoTTbinReader2.points[10] = HoTTbinReader.pointsGAM[10];
			for (int j = 10; j < 13; j++) {
				HoTTbinReader2.points[j] = HoTTbinReader.pointsGAM[j];
			}
			for (int k = 24; k < 46; k++) {
				HoTTbinReader2.points[k] = HoTTbinReader.pointsGAM[k];
			}
		}
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		if (isGPSData) {
			for (int j = 10; j < 13; j++) {
				HoTTbinReader2.points[j] = HoTTbinReader.pointsGPS[j];
			}
			for (int k = 15; k < 24; k++) {
				HoTTbinReader2.points[k] = HoTTbinReader.pointsGPS[k];
			}
		}
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		if (isVarioData && (HoTTbinReader.pointsVario[10] != 0 || HoTTbinReader.pointsVario[11] != 0 || HoTTbinReader.pointsVario[12] != 0 || HoTTbinReader.pointsVario[13] != 0)) {
			for (int j = 10; j < 15; j++) {
				HoTTbinReader2.points[j] = HoTTbinReader.pointsVario[j];
			}
		}
		if (isMotorDriverData) {
			if (channelNumber == 4)
				//93=VoltageM, 94=CurrentM, 95=CapacityM, 96=PowerM, 97=RevolutionM, 98=TemperatureM 1, 99=TemperatureM 2 100=Voltage_min, 101=Current_max, 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M
				for (int j = 93; j < HoTTbinReader2.points.length; j++) {
					HoTTbinReader2.points[j] = HoTTbinReader.pointsESC[j];
				}
			else
				//73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max, 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M
				for (int j = 73; j < HoTTbinReader2.points.length; j++) {
					HoTTbinReader2.points[j] = HoTTbinReader.pointsESC[j];
				}
		}
		HoTTbinReader2.recordSet.addPoints(HoTTbinReader2.points, HoTTbinReader.timeStep_ms);
		HoTTbinReader2.isJustMigrated = true;
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 * @param _buf
	 */
	protected static void parseReceiver(byte[] _buf) {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		HoTTbinReader.tmpVoltageRx = (_buf[35] & 0xFF);
		HoTTbinReader.tmpTemperatureRx = (_buf[36] & 0xFF);
		HoTTbinReader2.points[1] = (_buf[38] & 0xFF) * 1000;
		HoTTbinReader2.points[3] = DataParser.parse2Short(_buf, 40) * 1000;
		if (!pickerParameters.isFilterEnabled || HoTTbinReader.tmpVoltageRx > -1 && HoTTbinReader.tmpVoltageRx < 100 && HoTTbinReader.tmpTemperatureRx < 100) {
			HoTTbinReader2.points[2] = (convertRxDbm2Strength(_buf[4] & 0xFF)) * 1000;
			HoTTbinReader2.points[4] = (_buf[3] & 0xFF) * -1000;
			HoTTbinReader2.points[5] = (_buf[4] & 0xFF) * -1000;
			HoTTbinReader2.points[6] = (_buf[35] & 0xFF) * 1000;
			HoTTbinReader2.points[7] = ((_buf[36] & 0xFF) - 20) * 1000;
			HoTTbinReader2.points[8] = (_buf[39] & 0xFF) * 1000;
		}
		if ((_buf[32] & 0x40) > 0 || (_buf[32] & 0x25) > 0 && HoTTbinReader.tmpTemperatureRx >= 70) //T = 70 - 20 = 50 lowest temperature warning
			HoTTbinReader2.points[9] = (_buf[32] & 0x65) * 1000; //warning E,V,T only
		else
			HoTTbinReader2.points[9] = 0;
	}

	/**
	 * parse the buffered data from buffer 0 to 2 and add points to record set
	 * @param sdLogVersion
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 */
	protected static void parseVario(byte[] _buf0, byte[] _buf1, byte[] _buf2) {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf1, 2) - 500;
		if (!pickerParameters.isFilterEnabled || (HoTTbinReader.tmpHeight >= -490 && HoTTbinReader.tmpHeight < 5000)) {
			HoTTbinReader.pointsVario[10] = HoTTbinReader.tmpHeight * 1000;
			//pointsVarioMax = DataParser.parse2Short(buf1, 4) * 1000;
			//pointsVarioMin = DataParser.parse2Short(buf1, 6) * 1000;
			HoTTbinReader.pointsVario[11] = (DataParser.parse2UnsignedShort(_buf1, 8) - 30000) * 10;
		}
		HoTTbinReader.tmpClimb10 = DataParser.parse2UnsignedShort(_buf2, 2) - 30000;
		if (!pickerParameters.isFilterEnabled || HoTTbinReader.tmpClimb10 > -10000 && HoTTbinReader.tmpClimb10 < 10000) {
			HoTTbinReader.pointsVario[12] = (DataParser.parse2UnsignedShort(_buf2, 0) - 30000) * 10;
			HoTTbinReader.pointsVario[13] = HoTTbinReader.tmpClimb10 * 10;
		}
		HoTTbinReader.pointsVario[14] = (_buf1[1] & 0x3F) * 1000; //inverse event
	}

	/**
	 * parse the buffered data from buffer 0 to 3 and add points to record set
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 */
	protected static void parseGPS(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3) {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf2, 8) - 500;
		HoTTbinReader.tmpClimb1 = (DataParser.parse2UnsignedShort(_buf3, 0) - 30000);
		HoTTbinReader.tmpClimb3 = (_buf3[2] & 0xFF) - 120;
		HoTTbinReader.tmpVelocity = DataParser.parse2Short(_buf1, 4) * 1000;
		if (!pickerParameters.isFilterEnabled || (HoTTbinReader.tmpClimb1 > -20000 && HoTTbinReader.tmpClimb3 > -90 && HoTTbinReader.tmpHeight >= -490 && HoTTbinReader.tmpHeight < 4500)) {
			HoTTbinReader.pointsGPS[17] = pickerParameters.isFilterEnabled && HoTTbinReader.tmpVelocity > 500000 ? HoTTbinReader.pointsGPS[17] : HoTTbinReader.tmpVelocity;

			HoTTbinReader.tmpLatitude = DataParser.parse2Short(_buf1, 7) * 10000 + DataParser.parse2Short(_buf1[9], _buf2[0]);
			if (!pickerParameters.isTolerateSignChangeLatitude) HoTTbinReader.tmpLatitude = _buf1[6] == 1 ? -1 * HoTTbinReader.tmpLatitude : HoTTbinReader.tmpLatitude;
			HoTTbinReader.tmpLatitudeDelta = Math.abs(HoTTbinReader.tmpLatitude - HoTTbinReader.pointsGPS[15]);
			HoTTbinReader.tmpLatitudeDelta = HoTTbinReader.tmpLatitudeDelta > 400000 ? HoTTbinReader.tmpLatitudeDelta - 400000 : HoTTbinReader.tmpLatitudeDelta;
			HoTTbinReader.latitudeTolerance = (HoTTbinReader.pointsGPS[17] / 1000.0) * (HoTTbinReader.timeStep_ms - HoTTbinReader.lastLatitudeTimeStep) / pickerParameters.latitudeToleranceFactor;
			HoTTbinReader.latitudeTolerance = HoTTbinReader.latitudeTolerance > 0 ? HoTTbinReader.latitudeTolerance : 5;

			if (!pickerParameters.isFilterEnabled || HoTTbinReader.pointsGPS[15] == 0 || HoTTbinReader.tmpLatitudeDelta <= HoTTbinReader.latitudeTolerance) {
				HoTTbinReader.lastLatitudeTimeStep = HoTTbinReader.timeStep_ms;
				HoTTbinReader.pointsGPS[15] = HoTTbinReader.tmpLatitude;
			}
			else {
				if (HoTTbinReader2.log.isLoggable(Level.FINE))
					HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("HH:mm:ss:SSS", HoTTbinReader.timeStep_ms - GDE.ONE_HOUR_MS) + " Lat " + HoTTbinReader.tmpLatitude + " - "
							+ HoTTbinReader.tmpLatitudeDelta);
			}

			HoTTbinReader.tmpLongitude = DataParser.parse2Short(_buf2, 2) * 10000 + DataParser.parse2Short(_buf2, 4);
			if (!pickerParameters.isTolerateSignChangeLongitude) HoTTbinReader.tmpLongitude = _buf2[1] == 1 ? -1 * HoTTbinReader.tmpLongitude : HoTTbinReader.tmpLongitude;
			HoTTbinReader.tmpLongitudeDelta = Math.abs(HoTTbinReader.tmpLongitude - HoTTbinReader.pointsGPS[16]);
			HoTTbinReader.tmpLongitudeDelta = HoTTbinReader.tmpLongitudeDelta > 400000 ? HoTTbinReader.tmpLongitudeDelta - 400000 : HoTTbinReader.tmpLongitudeDelta;
			HoTTbinReader.longitudeTolerance = (HoTTbinReader.pointsGPS[17] / 1000.0) * (HoTTbinReader.timeStep_ms - HoTTbinReader.lastLongitudeTimeStep) / pickerParameters.longitudeToleranceFactor;
			HoTTbinReader.longitudeTolerance = HoTTbinReader.longitudeTolerance > 0 ? HoTTbinReader.longitudeTolerance : 5;

			if (!pickerParameters.isFilterEnabled || HoTTbinReader.pointsGPS[16] == 0 || HoTTbinReader.tmpLongitudeDelta <= HoTTbinReader.longitudeTolerance) {
				HoTTbinReader.lastLongitudeTimeStep = HoTTbinReader.timeStep_ms;
				HoTTbinReader.pointsGPS[16] = HoTTbinReader.tmpLongitude;
			}
			else {
				if (HoTTbinReader2.log.isLoggable(Level.FINE))
					HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("HH:mm:ss:SSS", HoTTbinReader.timeStep_ms - GDE.ONE_HOUR_MS) + " Long " + HoTTbinReader.tmpLongitude + " - "
							+ HoTTbinReader.tmpLongitudeDelta);
			}

			HoTTbinReader.pointsGPS[10] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsGPS[11] = HoTTbinReader.tmpClimb1 * 10;
			HoTTbinReader.pointsGPS[12] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsGPS[18] = DataParser.parse2Short(_buf2, 6) * 1000;
			HoTTbinReader.pointsGPS[19] = (_buf1[3] & 0xFF) * 1000;
			HoTTbinReader.pointsGPS[20] = 0;
			HoTTbinReader.pointsGPS[21] = (_buf3[3] & 0xFF) * 1000;
			try {
				HoTTbinReader.pointsGPS[22] = Integer.valueOf(String.format("%c", _buf3[4])) * 1000;
			}
			catch (NumberFormatException e1) {
				//ignore;
			}
		}
		HoTTbinReader.pointsGPS[23] = (_buf1[1] & 0x0F) * 1000; //inverse event
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 */
	protected static void parseGAM(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) {
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf3, 0) - 500;
		HoTTbinReader.tmpClimb3 = (_buf3[4] & 0xFF) - 120;
		HoTTbinReader.tmpVoltage1 = DataParser.parse2Short(_buf1[9], _buf2[0]);
		HoTTbinReader.tmpVoltage2 = DataParser.parse2Short(_buf2, 1);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
		if (!pickerParameters.isFilterEnabled
				|| (HoTTbinReader.tmpClimb3 > -90 && HoTTbinReader.tmpHeight >= -490 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600 && Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			HoTTbinReader.pointsGAM[24] = DataParser.parse2Short(_buf3, 7) * 1000;
			HoTTbinReader.pointsGAM[25] = DataParser.parse2Short(_buf3, 5) * 1000;
			if (!pickerParameters.isFilterEnabled || HoTTbinReader2.recordSet.getRecordDataSize(true) <= 20
					|| (HoTTbinReader.tmpCapacity != 0 && Math.abs(HoTTbinReader.tmpCapacity) <= (HoTTbinReader.pointsGAM[26] / 1000 + HoTTbinReader.pointsGAM[24] / 1000 * HoTTbinReader.pointsGAM[25] / 1000 / 2500 + 2))) {
				HoTTbinReader.pointsGAM[26] = HoTTbinReader.tmpCapacity * 1000;
			}
			else {
				if (HoTTbinReader2.log.isLoggable(Level.FINE))
					HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - "
							+ (HoTTbinReader.pointsGAM[26] / 1000) + " + " + (HoTTbinReader.pointsGAM[24] / 1000 * HoTTbinReader.pointsGAM[25] / 1000 / 2500 + 2));
			}
			HoTTbinReader.pointsGAM[27] = Double.valueOf(HoTTbinReader.pointsGAM[24] / 1000.0 * HoTTbinReader.pointsGAM[25]).intValue();
			//cell voltage
			for (int j = 0; j < 6; j++) {
				HoTTbinReader.pointsGAM[j + 29] = (_buf1[3 + j] & 0xFF) * 1000;
				if (HoTTbinReader.pointsGAM[j + 29] > 0) {
					maxVotage = HoTTbinReader.pointsGAM[j + 29] > maxVotage ? HoTTbinReader.pointsGAM[j + 29] : maxVotage;
					minVotage = HoTTbinReader.pointsGAM[j + 29] < minVotage ? HoTTbinReader.pointsGAM[j + 29] : minVotage;
				}
			}
			HoTTbinReader.pointsGAM[28] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
			HoTTbinReader.pointsGAM[35] = DataParser.parse2Short(_buf2, 8) * 1000;
			HoTTbinReader.pointsGAM[36] = DataParser.parse2Short(_buf2, 6) * 1000;
			HoTTbinReader.pointsGAM[10] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsGAM[11] = (DataParser.parse2UnsignedShort(_buf3, 2) - 30000) * 10;
			HoTTbinReader.pointsGAM[12] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsGAM[37] = HoTTbinReader.tmpVoltage1 * 100;
			HoTTbinReader.pointsGAM[38] = HoTTbinReader.tmpVoltage2 * 100;
			HoTTbinReader.pointsGAM[39] = ((_buf2[3] & 0xFF) - 20) * 1000;
			HoTTbinReader.pointsGAM[40] = ((_buf2[4] & 0xFF) - 20) * 1000;
			HoTTbinReader.pointsGAM[41] = DataParser.parse2Short(_buf4, 1) * 1000; //Speed [km/h
			HoTTbinReader.pointsGAM[42] = (_buf4[3] & 0xFF) * 1000; //lowest cell voltage 124 = 2.48 V
			HoTTbinReader.pointsGAM[43] = (_buf4[4] & 0xFF) * 1000; //cell number lowest cell voltage
			HoTTbinReader.pointsGAM[44] = (_buf4[8] & 0xFF) * 1000; //Pressure
		}
		HoTTbinReader.pointsGAM[45] = ((_buf1[1] & 0xFF) + ((_buf1[2] & 0x7F) << 8)) * 1000; //inverse event
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
	protected static void parseEAM(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) throws DataInconsitsentException {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
		//46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14, 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf3, 3) - 500;
		HoTTbinReader.tmpClimb3 = (_buf4[3] & 0xFF) - 120;
		HoTTbinReader.tmpVoltage1 = DataParser.parse2Short(_buf2, 7);
		HoTTbinReader.tmpVoltage2 = DataParser.parse2Short(_buf2[9], _buf3[0]);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
		if (!pickerParameters.isFilterEnabled
				|| (HoTTbinReader.tmpClimb3 > -90 && HoTTbinReader.tmpHeight >= -490 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600 && Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			HoTTbinReader.pointsEAM[46] = DataParser.parse2Short(_buf3, 7) * 1000;
			HoTTbinReader.pointsEAM[47] = DataParser.parse2Short(_buf3, 5) * 1000;
			if (!pickerParameters.isFilterEnabled || HoTTbinReader2.recordSet.getRecordDataSize(true) <= 20
					|| Math.abs(HoTTbinReader.tmpCapacity) <= (HoTTbinReader.pointsEAM[48] / 1000 + HoTTbinReader.pointsEAM[46] / 1000 * HoTTbinReader.pointsEAM[47] / 1000 / 2500 + 2)) {
				HoTTbinReader.pointsEAM[48] = HoTTbinReader.tmpCapacity * 1000;
			}
			else {
				if (HoTTbinReader2.log.isLoggable(Level.FINE))
					HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - "
						+ (HoTTbinReader.pointsEAM[48] / 1000) + " + " + (HoTTbinReader.pointsEAM[46] / 1000 * HoTTbinReader.pointsEAM[47] / 1000 / 2500 + 2));
			}
			HoTTbinReader.pointsEAM[49] = Double.valueOf(HoTTbinReader.pointsEAM[46] / 1000.0 * HoTTbinReader.pointsEAM[47]).intValue(); // power U*I [W];
			for (int j = 0; j < 7; j++) {
				HoTTbinReader.pointsEAM[j + 51] = (_buf1[3 + j] & 0xFF) * 1000;
				if (HoTTbinReader.pointsEAM[j + 51] > 0) {
					maxVotage = HoTTbinReader.pointsEAM[j + 51] > maxVotage ? HoTTbinReader.pointsEAM[j + 51] : maxVotage;
					minVotage = HoTTbinReader.pointsEAM[j + 51] < minVotage ? HoTTbinReader.pointsEAM[j + 51] : minVotage;
				}
			}
			for (int j = 0; j < 7; j++) {
				HoTTbinReader.pointsEAM[j + 58] = (_buf2[j] & 0xFF) * 1000;
				if (HoTTbinReader.pointsEAM[j + 58] > 0) {
					maxVotage = HoTTbinReader.pointsEAM[j + 58] > maxVotage ? HoTTbinReader.pointsEAM[j + 58] : maxVotage;
					minVotage = HoTTbinReader.pointsEAM[j + 58] < minVotage ? HoTTbinReader.pointsEAM[j + 58] : minVotage;
				}
			}
			//calculate balance on the fly
			HoTTbinReader.pointsEAM[50] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
			HoTTbinReader.pointsEAM[10] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsEAM[11] = (DataParser.parse2UnsignedShort(_buf4, 1) - 30000) * 10;
			HoTTbinReader.pointsEAM[12] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsEAM[65] = HoTTbinReader.tmpVoltage1 * 100;
			HoTTbinReader.pointsEAM[66] = HoTTbinReader.tmpVoltage2 * 100;
			HoTTbinReader.pointsEAM[67] = ((_buf3[1] & 0xFF) - 20) * 1000;
			HoTTbinReader.pointsEAM[68] = ((_buf3[2] & 0xFF) - 20) * 1000;
			HoTTbinReader.pointsEAM[69] = DataParser.parse2Short(_buf4, 4) * 1000;
			HoTTbinReader.pointsEAM[70] = ((_buf4[6] & 0xFF) * 60 + (_buf4[7] & 0xFF)) * 1000; // motor time
			HoTTbinReader.pointsEAM[71] = DataParser.parse2Short(_buf4, 8) * 1000; // speed
		}
		HoTTbinReader.pointsEAM[72] = ((_buf1[1] & 0xFF) + ((_buf1[2] & 0x7F) << 8)) * 1000; //inverse event
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 * @param _buf
	 */
	protected static void parseChannel(byte[] _buf) {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
		//46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14, 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
		//73=Ch 1, 74=Ch 2, 75=Ch 3 .. 88=Ch 16, 89=PowerOff, 90=BatterieLow, 91=Reset, 92=reserve
		HoTTbinReader2.points[4] = (_buf[3] & 0xFF) * -1000;
		HoTTbinReader2.points[5] = (_buf[4] & 0xFF) * -1000;

		HoTTbinReader2.points[73] = (DataParser.parse2UnsignedShort(_buf, 8) / 2) * 1000;
		HoTTbinReader2.points[74] = (DataParser.parse2UnsignedShort(_buf, 10) / 2) * 1000;
		HoTTbinReader2.points[75] = (DataParser.parse2UnsignedShort(_buf, 12) / 2) * 1000;
		HoTTbinReader2.points[76] = (DataParser.parse2UnsignedShort(_buf, 14) / 2) * 1000;
		HoTTbinReader2.points[77] = (DataParser.parse2UnsignedShort(_buf, 16) / 2) * 1000;
		HoTTbinReader2.points[78] = (DataParser.parse2UnsignedShort(_buf, 18) / 2) * 1000;
		HoTTbinReader2.points[79] = (DataParser.parse2UnsignedShort(_buf, 20) / 2) * 1000;
		HoTTbinReader2.points[80] = (DataParser.parse2UnsignedShort(_buf, 22) / 2) * 1000;
		//events
		HoTTbinReader2.points[89] = (_buf[50] & 0x01) * 100000;
		HoTTbinReader2.points[90] = (_buf[50] & 0x02) * 50000;
		HoTTbinReader2.points[91] = (_buf[50] & 0x04) * 25000;
		if (_buf[32] > 0 && _buf[32] < 27)
			HoTTbinReader2.points[92] = _buf[32] * 1000; //warning
		else
			HoTTbinReader2.points[92] = 0;

		if (_buf[5] == 0x00) { //channel 9-12
			HoTTbinReader2.points[81] = (DataParser.parse2UnsignedShort(_buf, 24) / 2) * 1000;
			HoTTbinReader2.points[82] = (DataParser.parse2UnsignedShort(_buf, 26) / 2) * 1000;
			HoTTbinReader2.points[83] = (DataParser.parse2UnsignedShort(_buf, 28) / 2) * 1000;
			HoTTbinReader2.points[84] = (DataParser.parse2UnsignedShort(_buf, 30) / 2) * 1000;
			if (HoTTbinReader2.points[85] == 0) {
				HoTTbinReader2.points[85] = 1500 * 1000;
				HoTTbinReader2.points[86] = 1500 * 1000;
				HoTTbinReader2.points[87] = 1500 * 1000;
				HoTTbinReader2.points[88] = 1500 * 1000;
			}
		}
		else { //channel 13-16
			HoTTbinReader2.points[85] = (DataParser.parse2UnsignedShort(_buf, 24) / 2) * 1000;
			HoTTbinReader2.points[86] = (DataParser.parse2UnsignedShort(_buf, 26) / 2) * 1000;
			HoTTbinReader2.points[87] = (DataParser.parse2UnsignedShort(_buf, 28) / 2) * 1000;
			HoTTbinReader2.points[88] = (DataParser.parse2UnsignedShort(_buf, 30) / 2) * 1000;
			if (HoTTbinReader2.points[81] == 0) {
				HoTTbinReader2.points[81] = 1500 * 1000;
				HoTTbinReader2.points[82] = 1500 * 1000;
				HoTTbinReader2.points[83] = 1500 * 1000;
				HoTTbinReader2.points[84] = 1500 * 1000;
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
	protected static void parseESC(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, int channelNumber) throws DataInconsitsentException {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=EventGPS
		//22=Voltage G, 23=Current G, 24=Capacity G, 25=Power G, 26=Balance G, 27=CellVoltage G1, 28=CellVoltage G2 .... 32=CellVoltage G6, 33=Revolution G, 34=FuelLevel, 35=Voltage G1, 36=Voltage G2, 37=Temperature G1, 38=Temperature G2 39=Speed G, 40=LowestCellVoltage, 41=LowestCellNumber, 42=Pressure, 43=Event G
		//44=Voltage E, 45=Current E, 46=Capacity E, 47=Power E, 48=Balance E, 49=CellVoltage E1, 50=CellVoltage E2 .... 62=CellVoltage E14, 63=Voltage E1, 64=Voltage E2, 65=Temperature E1, 66=Temperature E2 67=Revolution E 68=MotorTime 69=Speed 70=Event E
		//71=VoltageM, 72=CurrentM, 73=CapacityM, 74=PowerM, 75=RevolutionM, 76=TemperatureM 1, 77=TemperatureM 2 78=Voltage_min, 79=Current_max, 80=Revolution_max, 81=Temperature1_max, 82=Temperature2_max 83=Event M

		//71=Ch 1, 72=Ch 2, 73=Ch 3 .. 86=Ch 16, 87=PowerOff, 88=BatterieLow, 89=Reset, 90=reserve
		//91=VoltageM, 92=CurrentM, 93=CapacityM, 94=PowerM, 95=RevolutionM, 96=TemperatureM 1, 97=TemperatureM 2 98=Voltage_min, 99=Current_max, 100=Revolution_max, 101=Temperature1_max, 102=Temperature2_max 103=Event M

		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		//10=Height, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
		//15=Latitude, 16=Longitude, 17=Velocity, 18=DistanceStart, 19=DirectionStart, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
		//24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6, 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G
		//46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14, 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
		//73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max, 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M

		//73=Ch 1, 74=Ch 2, 75=Ch 3 .. 88=Ch 16, 89=PowerOff, 90=BatterieLow, 91=Reset, 92=reserve
		//93=VoltageM, 94=CurrentM, 95=CapacityM, 96=PowerM, 97=RevolutionM, 98=TemperatureM 1, 99=TemperatureM 2 100=Voltage_min, 101=Current_max, 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M
		HoTTbinReader.tmpVoltage = DataParser.parse2Short(_buf1, 3);
		HoTTbinReader.tmpCurrent = DataParser.parse2Short(_buf2, 1);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf1, 7);
		HoTTbinReader.tmpRevolution = DataParser.parse2Short(_buf2, 5);
		HoTTbinReader.tmpTemperatureFet = _buf1[9] - 20;
		if (channelNumber == 4) {
			//93=VoltageM, 94=CurrentM, 95=CapacityM, 96=PowerM, 97=RevolutionM, 98=TemperatureM 1, 99=TemperatureM 2 100=Voltage_min, 101=Current_max, 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M
			if (!pickerParameters.isFilterEnabled || HoTTbinReader.tmpVoltage > 0 && HoTTbinReader.tmpVoltage < 1000 && HoTTbinReader.tmpCurrent < 4000 && HoTTbinReader.tmpCurrent > -10
					&& HoTTbinReader.tmpRevolution > -1 && HoTTbinReader.tmpRevolution < 20000
					&& !(HoTTbinReader.pointsESC[85] != 0 && HoTTbinReader.pointsESC[85] / 1000 - HoTTbinReader.tmpTemperatureFet > 20)) {
				HoTTbinReader.pointsESC[93] = HoTTbinReader.tmpVoltage * 1000;
				HoTTbinReader.pointsESC[94] = HoTTbinReader.tmpCurrent * 1000;
				HoTTbinReader.pointsESC[96] = Double.valueOf(HoTTbinReader.pointsESC[93] / 1000.0 * HoTTbinReader.pointsESC[94]).intValue();
				if (!pickerParameters.isFilterEnabled || HoTTbinReader2.recordSet.getRecordDataSize(true) <= 20
						|| (HoTTbinReader.tmpCapacity != 0 && Math.abs(HoTTbinReader.tmpCapacity) <= (HoTTbinReader.pointsESC[95] / 1000 + HoTTbinReader.tmpVoltage * HoTTbinReader.tmpCurrent / 2500 + 2))) {
					HoTTbinReader.pointsESC[95] = HoTTbinReader.tmpCapacity * 1000;
				}
				else {
					if (HoTTbinReader.tmpCapacity != 0 && HoTTbinReader2.log.isLoggable(Level.FINE))
						HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - "
							+ (HoTTbinReader.pointsESC[95] / 1000) + " + " + (HoTTbinReader.tmpVoltage * HoTTbinReader.tmpCurrent / 2500 + 2));
				}
				HoTTbinReader.pointsESC[97] = HoTTbinReader.tmpRevolution * 1000;
				HoTTbinReader.pointsESC[98] = HoTTbinReader.tmpTemperatureFet * 1000;

				HoTTbinReader.pointsESC[99] = (_buf2[9] - 20) * 1000;
				HoTTbinReader.pointsESC[100] = DataParser.parse2Short(_buf1, 5) * 1000;
				HoTTbinReader.pointsESC[101] = DataParser.parse2Short(_buf2, 3) * 1000;
				HoTTbinReader.pointsESC[102] = DataParser.parse2Short(_buf2, 7) * 1000;
				HoTTbinReader.pointsESC[103] = (_buf2[0] - 20) * 1000;
				HoTTbinReader.pointsESC[104] = (_buf3[0] - 20) * 1000;
			}
			HoTTbinReader.pointsESC[105] = (_buf1[1] & 0xFF) * 1000; //inverse event
		}
		else {
			//73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max, 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M
			if (!pickerParameters.isFilterEnabled || HoTTbinReader.tmpVoltage > 0 && HoTTbinReader.tmpVoltage < 1000 && HoTTbinReader.tmpCurrent < 4000 && HoTTbinReader.tmpCurrent > -10
					&& HoTTbinReader.tmpRevolution > -1 && HoTTbinReader.tmpRevolution < 20000
					&& !(HoTTbinReader.pointsESC[78] != 0 && HoTTbinReader.pointsESC[78] / 1000 - HoTTbinReader.tmpTemperatureFet > 20)) {
				HoTTbinReader.pointsESC[73] = HoTTbinReader.tmpVoltage * 1000;
				HoTTbinReader.pointsESC[74] = HoTTbinReader.tmpCurrent * 1000;
				HoTTbinReader.pointsESC[76] = Double.valueOf(HoTTbinReader.pointsESC[73] / 1000.0 * HoTTbinReader.pointsESC[74]).intValue();
				if (!pickerParameters.isFilterEnabled || HoTTbinReader2.recordSet.getRecordDataSize(true) <= 20
						|| (HoTTbinReader.tmpCapacity != 0 && Math.abs(HoTTbinReader.tmpCapacity) <= (HoTTbinReader.pointsESC[75] / 1000 + HoTTbinReader.tmpVoltage * HoTTbinReader.tmpCurrent / 2500 + 2))) {
					HoTTbinReader.pointsESC[75] = HoTTbinReader.tmpCapacity * 1000;
				}
				else {
					if (HoTTbinReader.tmpCapacity != 0 && HoTTbinReader2.log.isLoggable(Level.FINE))
						HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - "
							+ (HoTTbinReader.pointsESC[75] / 1000) + " + " + (HoTTbinReader.tmpVoltage * HoTTbinReader.tmpCurrent / 2500 + 2));
				}
				HoTTbinReader.pointsESC[77] = HoTTbinReader.tmpRevolution * 1000;
				HoTTbinReader.pointsESC[78] = HoTTbinReader.tmpTemperatureFet * 1000;

				HoTTbinReader.pointsESC[79] = (_buf2[9] - 20) * 1000;
				HoTTbinReader.pointsESC[80] = DataParser.parse2Short(_buf1, 5) * 1000;
				HoTTbinReader.pointsESC[81] = DataParser.parse2Short(_buf2, 3) * 1000;
				HoTTbinReader.pointsESC[82] = DataParser.parse2Short(_buf2, 7) * 1000;
				HoTTbinReader.pointsESC[83] = (_buf2[0] - 20) * 1000;
				HoTTbinReader.pointsESC[84] = (_buf3[0] - 20) * 1000;
			}
			HoTTbinReader.pointsESC[85] = (_buf1[1] & 0xFF) * 1000; //inverse event
		}
	}
}
