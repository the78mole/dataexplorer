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
		HashMap<String, String> header = getFileInfo(new File(filePath));

		if (Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) <= 1) {
			HoTTbinReader.isReceiverOnly = Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) == 0;
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
		HoTTAdapter2 device = (HoTTAdapter2) HoTTbinReader.application.getActiveDevice();
		int recordSetNumber = HoTTbinReader.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		int channelNumber = device.getLastChannelNumber();
		device.getMeasurementFactor(channelNumber, 12);
		boolean isReceiverData = false;
		boolean isSensorData = false;
		HoTTbinReader2.recordSet = null;
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
		//9=Height, 10=Climb 1, 11=Climb 3, 12=Climb 10
		//13=Latitude, 14=Longitude, 15=Velocity, 16=DistanceStart, 17=DirectionStart, 18=TripDistance
		//19=Voltage G, 20=Current G, 21=Capacity G, 22=Power G, 23=Balance G, 24=CellVoltage G1, 25=CellVoltage G2 .... 29=CellVoltage G6, 30=Revolution G, 31=FuelLevel, 32=Voltage G1, 33=Voltage G2, 34=Temperature G1, 35=Temperature G2
		//36=Voltage E, 37=Current E, 38=Capacity E, 39=Power E, 40=Balance E, 41=CellVoltage E1, 42=CellVoltage E2 .... 54=CellVoltage E14, 55=Voltage E1, 56=Voltage E2, 57=Temperature E1, 58=Temperature E2 59=Revolution E
		//60=Ch 1, 61=Ch 2, 62=Ch 3 .. 75=Ch 16, 76=PowerOff, 77=BatterieLow, 78=Reset, 79=reserve
		//80=VoltageM, 81=CurrentM, 82=CapacityM, 83=PowerM, 84=RevolutionM, 85=TemperatureM 1, 86=TemperatureM 2
	  //87=Voltage_min, 88=Current_max, 89=Revolution_max, 90=Temperature1_max, 91=Temperature2_max
		//60=VoltageM, 61=CurrentM, 62=CapacityM, 63=PowerM, 64=RevolutionM, 65=TemperatureM 1, 66=TemperatureM 2
	  //67=Voltage_min, 68=Current_max, 69=Revolution_max, 70=Temperature1_max, 71=Temperature2_max
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
		HoTTAdapter.reverseChannelPackageLossCounter.clear();
		HoTTbinReader.lostPackages.clear();
		HoTTbinReader.countLostPackages = 0;
		HoTTbinReader.isTextModusSignaled = false;
		int countPackageLoss = 0;
		long numberDatablocks = fileSize / HoTTbinReader.dataBlockSize / (HoTTbinReader.isReceiverOnly && channelNumber != 4 ? 10 : 1);
		long startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(file, numberDatablocks);
		String date = StringHelper.getDate();
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
			recordSetName = recordSetNumber + device.getRecordSetStemNameReplacement() + recordSetNameExtend;
			HoTTbinReader2.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true, true);
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
				if (HoTTbinReader2.logger.isLoggable(Level.FINE) && i % 10 == 0) {
					HoTTbinReader2.logger.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
					HoTTbinReader2.logger.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if (!HoTTAdapter.isFilterTextModus || (HoTTbinReader.buf[6] & 0x01) == 0) { //switch into text modus
					if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { //buf 3, 4, tx,rx		
						if (HoTTbinReader2.logger.isLoggable(Level.FINE))
							HoTTbinReader2.logger.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", HoTTbinReader.buf[7], HoTTbinReader.buf[33]));

						HoTTAdapter.reverseChannelPackageLossCounter.add(1);
						HoTTbinReader2.points[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;

						if (HoTTbinReader2.logger.isLoggable(Level.FINER))
							HoTTbinReader2.logger.logp(Level.FINER, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1)
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
							isSensorData = true;
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
									parseVario(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2);
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
									parseGAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
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
									parseEAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
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

						if (menuToolBar != null && i % progressIndicator == 0) HoTTbinReader.application.setProgress((int) (i * 100 / numberDatablocks), sThreadId);
					}
					else { //skip empty block, but add time step
						if (HoTTbinReader2.logger.isLoggable(Level.FINE)) HoTTbinReader2.logger.log(Level.FINE, "-->> Found tx=rx=0 dBm");

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
						//reset buffer to avoid mixing data >> 20 Jul 14, not any longer required due to protocol change requesting next sensor data block
						//HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
					}
				}
				else if (!HoTTbinReader.isTextModusSignaled) {
					HoTTbinReader.isTextModusSignaled = true;
					HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			String packageLossPercentage = tmpRecordSet.getRecordDataSize(true) > 0 ? String.format("%.1f", (countPackageLoss / tmpRecordSet.getTime_ms(tmpRecordSet.getRecordDataSize(true) - 1) * 1000))
					: "100";
			tmpRecordSet.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] { countPackageLoss, packageLossPercentage, HoTTbinReader.lostPackages.getStatistics() })
					+ HoTTbinReader.sensorSignature);
			HoTTbinReader2.logger.logp(Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReader2.logger.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (menuToolBar != null) {
				HoTTbinReader.application.setProgress(99, sThreadId);
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
		HoTTbinReader2.recordSet = null;
		HoTTbinReader2.isJustMigrated = false;
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
		//9=Height, 10=Climb 1, 11=Climb 3, 12=Climb 10
		//13=Latitude, 14=Longitude, 15=Velocity, 16=DistanceStart, 17=DirectionStart, 18=TripDistance
		//19=Voltage G, 20=Current G, 21=Capacity G, 22=Power G, 23=Balance G, 24=CellVoltage G1, 25=CellVoltage G2 .... 29=CellVoltage G6, 30=Revolution G, 31=FuelLevel, 32=Voltage G1, 33=Voltage G2, 34=Temperature G1, 35=Temperature G2
		//36=Voltage E, 37=Current E, 38=Capacity E, 39=Power E, 40=Balance E, 41=CellVoltage E1, 42=CellVoltage E2 .... 54=CellVoltage E14, 55=Voltage E1, 56=Voltage E2, 57=Temperature E1, 58=Temperature E2 59=Revolution E
		//60=Ch 1, 61=Ch 2, 62=Ch 3 .. 75=Ch 16, 76=PowerOff, 77=BatterieLow, 78=Reset, 79=reserve
		//80=VoltageM, 81=CurrentM, 82=CapacityM, 83=PowerM, 84=RevolutionM, 85=TemperatureM 1, 86=TemperatureM 2
	  //87=Voltage_min, 88=Current_max, 89=Revolution_max, 90=Temperature1_max, 91=Temperature2_max
		//60=VoltageM, 61=CurrentM, 62=CapacityM, 63=PowerM, 64=RevolutionM, 65=TemperatureM 1, 66=TemperatureM 2
	  //67=Voltage_min, 68=Current_max, 69=Revolution_max, 70=Temperature1_max, 71=Temperature2_max
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
		HoTTAdapter.reverseChannelPackageLossCounter.clear();
		HoTTbinReader.lostPackages.clear();
		HoTTbinReader.countLostPackages = 0;
		HoTTbinReader.isTextModusSignaled = false;
		//		HoTTbinReader.oldProtocolCount = 0;
		//		HoTTbinReader.blockSequenceCheck		= new Vector<Byte>();
		int countPackageLoss = 0;
		long numberDatablocks = fileSize / HoTTbinReader.dataBlockSize;
		long startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(file, numberDatablocks);
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
			HoTTbinReader2.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true, true);
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
				if (HoTTbinReader2.logger.isLoggable(Level.FINEST)) {
					HoTTbinReader2.logger.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if (!HoTTAdapter.isFilterTextModus || (HoTTbinReader.buf[6] & 0x01) == 0) { //switch into text modus
					if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { //buf 3, 4, tx,rx
						if (HoTTbinReader2.logger.isLoggable(Level.FINE))
							HoTTbinReader2.logger.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", HoTTbinReader.buf[7], HoTTbinReader.buf[33]));

						HoTTAdapter.reverseChannelPackageLossCounter.add(1);
						HoTTbinReader2.points[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
						//create and fill sensor specific data record sets 
						if (HoTTbinReader2.logger.isLoggable(Level.FINEST))
							HoTTbinReader2.logger.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1)
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
										parseVario(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2);
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
										parseGAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
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
										parseEAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
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
										parseESC(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, channelNumber);
										isMotorDriverData = true;
									}
									break;
								}

								if (HoTTbinReader2.logger.isLoggable(Level.FINE))
									HoTTbinReader2.logger.log(Level.FINE, "isReceiverData " + isReceiverData + " isVarioData " + isVarioData + " isGPSData " + isGPSData + " isGeneralData "
											+ isGeneralData + " isElectricData " + isElectricData);

							}

							if (HoTTbinReader2.logger.isLoggable(Level.FINE))
								HoTTbinReader2.logger.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "logCountVario = " + logCountVario + " logCountGPS = " + logCountGPS
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

						if (menuToolBar != null && i % progressIndicator == 0) HoTTbinReader.application.setProgress((int) (i * 100 / numberDatablocks), sThreadId);
					}
					else { //skip empty block, but add time step
						if (HoTTbinReader2.logger.isLoggable(Level.FINE)) HoTTbinReader2.logger.log(Level.FINE, "-->> Found tx=rx=0 dBm");

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
					+ HoTTbinReader.sensorSignature);
			HoTTbinReader2.logger.logp(Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReader2.logger.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (menuToolBar != null) {
				HoTTbinReader.application.setProgress(99, sThreadId);
				device.makeInActiveDisplayable(HoTTbinReader2.recordSet);
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
		//receiver data gets integrated each cycle 
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
		//9=Height, 10=Climb 1, 11=Climb 3, 12=Climb 10
		//13=Latitude, 14=Longitude, 15=Velocity, 16=DistanceStart, 17=DirectionStart, 18=TripDistance
		//19=Voltage G, 20=Current G, 21=Capacity G, 22=Power G, 23=Balance G, 24=CellVoltage G1, 25=CellVoltage G2 .... 29=CellVoltage G6, 30=Revolution G, 31=FuelLevel, 32=Voltage G1, 33=Voltage G2, 34=Temperature G1, 35=Temperature G2
		//36=Voltage E, 37=Current E, 38=Capacity E, 39=Power E, 40=Balance E, 41=CellVoltage E1, 42=CellVoltage E2 .... 54=CellVoltage E14, 55=Voltage E1, 56=Voltage E2, 57=Temperature E1, 58=Temperature E2 59=Revolution E
		//60=Ch 1, 61=Ch 2, 62=Ch 3 .. 75=Ch 16, 76=PowerOff, 77=BatterieLow, 78=Reset, 79=reserve
		//80=VoltageM, 81=CurrentM, 82=CapacityM, 83=PowerM, 84=RevolutionM, 85=TemperatureM 1, 86=TemperatureM 2
	  //87=Voltage_min, 88=Current_max, 89=Revolution_max, 90=Temperature1_max, 91=Temperature2_max
		//60=VoltageM, 61=CurrentM, 62=CapacityM, 63=PowerM, 64=RevolutionM, 65=TemperatureM 1, 66=TemperatureM 2
	  //67=Voltage_min, 68=Current_max, 69=Revolution_max, 70=Temperature1_max, 71=Temperature2_max

		//9=Height, 10=Climb 1, 11=Climb 3
		//36=Voltage E, 37=Current E, 38=Capacity E, 39=Power E, 40=Balance E, 41=CellVoltage E1, 42=CellVoltage E2 .... 54=CellVoltage E14, 55=Voltage E1, 56=Voltage E2, 57=Temperature E1, 58=Temperature E2 59=Revolution E
		if (isElectricData) {
			if ((HoTTbinReader2.points[9] != 0 && HoTTbinReader.pointsEAM[9] != 0) || HoTTbinReader.pointsEAM[9] != 0) HoTTbinReader2.points[9] = HoTTbinReader.pointsEAM[9];
			for (int j = 9; j < 12; j++) {
				HoTTbinReader2.points[j] = HoTTbinReader.pointsEAM[j];
			}
			for (int k = 36; k < 60; k++) {
				HoTTbinReader2.points[k] = HoTTbinReader.pointsEAM[k];
			}
		}
		//9=Height, 10=Climb 1, 11=Climb 3
		//19=Voltage G, 20=Current G, 21=Capacity G, 22=Power G, 23=Balance G, 24=CellVoltage G1, 25=CellVoltage G2 .... 29=CellVoltage G6, 30=Revolution G, 31=FuelLevel, 32=Voltage G1, 33=Voltage G2, 34=Temperature G1, 35=Temperature G2
		if (isGeneralData) {
			if ((HoTTbinReader2.points[9] != 0 && HoTTbinReader.pointsGAM[9] != 0) || HoTTbinReader.pointsGAM[9] != 0) HoTTbinReader2.points[9] = HoTTbinReader.pointsGAM[9];
			for (int j = 9; j < 12; j++) {
				HoTTbinReader2.points[j] = HoTTbinReader.pointsGAM[j];
			}
			for (int k = 19; k < 36; k++) {
				HoTTbinReader2.points[k] = HoTTbinReader.pointsGAM[k];
			}
		}
		//9=Height, 10=Climb 1, 11=Climb 3
		//13=Latitude, 14=Longitude, 15=Velocity, 16=DistanceStart, 17=DirectionStart, 18=TripDistance
		if (isGPSData) {
			for (int j = 9; j < 12; j++) {
				HoTTbinReader2.points[j] = HoTTbinReader.pointsGPS[j];
			}
			for (int k = 13; k < 19; k++) {
				HoTTbinReader2.points[k] = HoTTbinReader.pointsGPS[k];
			}
		}
		//9=Height, 10=Climb 1, 11=Climb 3, 12=Climb 10
		if (isVarioData && (HoTTbinReader.pointsVario[9] != 0 || HoTTbinReader.pointsVario[10] != 0 || HoTTbinReader.pointsVario[11] != 0 || HoTTbinReader.pointsVario[12] != 0)) {
			for (int j = 9; j < 13; j++) {
				HoTTbinReader2.points[j] = HoTTbinReader.pointsVario[j];
			}
		}
		if (isMotorDriverData) {
			if (channelNumber == 4)
				//80=VoltageM, 81=CurrentM, 82=CapacityM, 83=PowerM, 84=RevolutionM, 85=TemperatureM 1, 86=TemperatureM 2
			  //87=Voltage_min, 88=Current_max, 89=Revolution_max, 90=Temperature1_max, 91=Temperature2_max
				for (int j = 80; j < HoTTbinReader2.points.length; j++) {
					HoTTbinReader2.points[j] = HoTTbinReader.pointsESC[j];
				}
			else
				//60=VoltageM, 61=CurrentM, 62=CapacityM, 63=PowerM, 64=RevolutionM, 65=TemperatureM 1, 66=TemperatureM 2
			  //67=Voltage_min, 68=Current_max, 69=Revolution_max, 70=Temperature1_max, 71=Temperature2_max
				for (int j = 60; j < HoTTbinReader2.points.length; j++) {
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
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
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
			HoTTbinReader2.points[8] = (_buf[39] & 0xFF) * 1000;
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 2 and add points to record set
	 * @param sdLogVersion
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 */
	protected static void parseVario(byte[] _buf0, byte[] _buf1, byte[] _buf2) {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
		//9=Height, 10=Climb 1, 11=Climb 3, 12=Climb 10
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf1, 2) - 500;
		if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpHeight >= -490 && HoTTbinReader.tmpHeight < 5000)) {
			HoTTbinReader.pointsVario[9] = HoTTbinReader.tmpHeight * 1000;
			//pointsVarioMax = DataParser.parse2Short(buf1, 4) * 1000;
			//pointsVarioMin = DataParser.parse2Short(buf1, 6) * 1000;
			HoTTbinReader.pointsVario[10] = (DataParser.parse2UnsignedShort(_buf1, 8) - 30000) * 10;
		}
		HoTTbinReader.tmpClimb10 = DataParser.parse2UnsignedShort(_buf2, 2) - 30000;
		if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpClimb10 > -10000 && HoTTbinReader.tmpClimb10 < 10000) {
			HoTTbinReader.pointsVario[11] = (DataParser.parse2UnsignedShort(_buf2, 0) - 30000) * 10;
			HoTTbinReader.pointsVario[12] = HoTTbinReader.tmpClimb10 * 10;
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 3 and add points to record set
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 */
	protected static void parseGPS(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3) {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
		//9=Height, 10=Climb 1, 11=Climb 3, 12=Climb 10
		//13=Latitude, 14=Longitude, 15=Velocity, 16=DistanceStart, 17=DirectionStart, 18=TripDistance
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf2, 8) - 500;
		HoTTbinReader.tmpClimb1 = (DataParser.parse2UnsignedShort(_buf3, 0) - 30000);
		HoTTbinReader.tmpClimb3 = (_buf3[2] & 0xFF) - 120;
		HoTTbinReader.tmpVelocity = DataParser.parse2Short(_buf1, 4) * 1000;
		if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpClimb1 > -20000 && HoTTbinReader.tmpClimb3 > -90 && HoTTbinReader.tmpHeight >= -490 && HoTTbinReader.tmpHeight < 4500)) {
			HoTTbinReader.pointsGPS[15] = HoTTAdapter.isFilterEnabled && HoTTbinReader.tmpVelocity > 500000 ? HoTTbinReader.pointsGPS[15] : HoTTbinReader.tmpVelocity;

			HoTTbinReader.tmpLatitude = DataParser.parse2Short(_buf1, 7) * 10000 + DataParser.parse2Short(_buf1[9], _buf2[0]);
			if (!HoTTAdapter.isTolerateSignChangeLatitude) HoTTbinReader.tmpLatitude = _buf1[6] == 1 ? -1 * HoTTbinReader.tmpLatitude : HoTTbinReader.tmpLatitude;
			HoTTbinReader.tmpLatitudeDelta = Math.abs(HoTTbinReader.tmpLatitude - HoTTbinReader.pointsGPS[13]);
			HoTTbinReader.tmpLatitudeDelta = HoTTbinReader.tmpLatitudeDelta > 400000 ? HoTTbinReader.tmpLatitudeDelta - 400000 : HoTTbinReader.tmpLatitudeDelta;
			HoTTbinReader.latitudeTolerance = (HoTTbinReader.pointsGPS[15] / 1000.0) * (HoTTbinReader.timeStep_ms - HoTTbinReader.lastLatitudeTimeStep) / HoTTAdapter.latitudeToleranceFactor;
			HoTTbinReader.latitudeTolerance = HoTTbinReader.latitudeTolerance > 0 ? HoTTbinReader.latitudeTolerance : 5;

			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.pointsGPS[13] == 0 || HoTTbinReader.tmpLatitudeDelta <= HoTTbinReader.latitudeTolerance) {
				HoTTbinReader.lastLatitudeTimeStep = HoTTbinReader.timeStep_ms;
				HoTTbinReader.pointsGPS[13] = HoTTbinReader.tmpLatitude;
			}
			else {
				if (HoTTbinReader2.log.isLoggable(Level.FINE))
					HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("HH:mm:ss:SSS", HoTTbinReader.timeStep_ms - GDE.ONE_HOUR_MS) + " Lat " + HoTTbinReader.tmpLatitude + " - "
							+ HoTTbinReader.tmpLatitudeDelta);
			}

			HoTTbinReader.tmpLongitude = DataParser.parse2Short(_buf2, 2) * 10000 + DataParser.parse2Short(_buf2, 4);
			if (!HoTTAdapter.isTolerateSignChangeLongitude) HoTTbinReader.tmpLongitude = _buf2[1] == 1 ? -1 * HoTTbinReader.tmpLongitude : HoTTbinReader.tmpLongitude;
			HoTTbinReader.tmpLongitudeDelta = Math.abs(HoTTbinReader.tmpLongitude - HoTTbinReader.pointsGPS[14]);
			HoTTbinReader.tmpLongitudeDelta = HoTTbinReader.tmpLongitudeDelta > 400000 ? HoTTbinReader.tmpLongitudeDelta - 400000 : HoTTbinReader.tmpLongitudeDelta;
			HoTTbinReader.longitudeTolerance = (HoTTbinReader.pointsGPS[15] / 1000.0) * (HoTTbinReader.timeStep_ms - HoTTbinReader.lastLongitudeTimeStep) / HoTTAdapter.longitudeToleranceFactor;
			HoTTbinReader.longitudeTolerance = HoTTbinReader.longitudeTolerance > 0 ? HoTTbinReader.longitudeTolerance : 5;

			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.pointsGPS[14] == 0 || HoTTbinReader.tmpLongitudeDelta <= HoTTbinReader.longitudeTolerance) {
				HoTTbinReader.lastLongitudeTimeStep = HoTTbinReader.timeStep_ms;
				HoTTbinReader.pointsGPS[14] = HoTTbinReader.tmpLongitude;
			}
			else {
				if (HoTTbinReader2.log.isLoggable(Level.FINE))
					HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("HH:mm:ss:SSS", HoTTbinReader.timeStep_ms - GDE.ONE_HOUR_MS) + " Long " + HoTTbinReader.tmpLongitude + " - "
							+ HoTTbinReader.tmpLongitudeDelta);
			}

			HoTTbinReader.pointsGPS[9] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsGPS[10] = HoTTbinReader.tmpClimb1 * 10;
			HoTTbinReader.pointsGPS[11] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsGPS[16] = DataParser.parse2Short(_buf2, 6) * 1000;
			HoTTbinReader.pointsGPS[17] = (_buf1[3] & 0xFF) * 1000;
			HoTTbinReader.pointsGPS[18] = 0;
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
	protected static void parseGAM(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) {
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf3, 0) - 500;
		HoTTbinReader.tmpClimb3 = (_buf3[4] & 0xFF) - 120;
		HoTTbinReader.tmpVoltage1 = DataParser.parse2Short(_buf1[9], _buf2[0]);
		HoTTbinReader.tmpVoltage2 = DataParser.parse2Short(_buf2, 1);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
		//9=Height, 10=Climb 1, 11=Climb 3, 12=Climb 10
		//13=Latitude, 14=Longitude, 15=Velocity, 16=DistanceStart, 17=DirectionStart, 18=TripDistance
		//19=Voltage G, 20=Current G, 21=Capacity G, 22=Power G, 23=Balance G, 24=CellVoltage G1, 25=CellVoltage G2 .... 29=CellVoltage G6, 30=Revolution G, 31=FuelLevel, 32=Voltage G1, 33=Voltage G2, 34=Temperature G1, 35=Temperature G2
		if (!HoTTAdapter.isFilterEnabled
				|| (HoTTbinReader.tmpClimb3 > -90 && HoTTbinReader.tmpHeight >= -490 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600 && Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			HoTTbinReader.pointsGAM[19] = DataParser.parse2Short(_buf3, 7) * 1000;
			HoTTbinReader.pointsGAM[20] = DataParser.parse2Short(_buf3, 5) * 1000;
			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader2.recordSet.getRecordDataSize(true) <= 20
					|| (HoTTbinReader.tmpCapacity != 0 && Math.abs(HoTTbinReader.tmpCapacity) <= (HoTTbinReader.pointsGAM[21] / 1000 + HoTTbinReader.pointsGAM[19] / 1000 * HoTTbinReader.pointsGAM[20] / 1000 / 2500 + 2))) {
				HoTTbinReader.pointsGAM[21] = HoTTbinReader.tmpCapacity * 1000;
			}
			else {
				HoTTbinReader2.log.log(Level.WARNING, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - "
						+ (HoTTbinReader.pointsGAM[21] / 1000) + " + " + (HoTTbinReader.pointsGAM[19] / 1000 * HoTTbinReader.pointsGAM[20] / 1000 / 2500 + 2));
			}
			HoTTbinReader.pointsGAM[22] = Double.valueOf(HoTTbinReader.pointsGAM[19] / 1000.0 * HoTTbinReader.pointsGAM[20]).intValue();
			for (int j = 0; j < 6; j++) {
				HoTTbinReader.pointsGAM[j + 24] = (_buf1[3 + j] & 0xFF) * 1000;
				if (HoTTbinReader.pointsGAM[j + 24] > 0) {
					maxVotage = HoTTbinReader.pointsGAM[j + 24] > maxVotage ? HoTTbinReader.pointsGAM[j + 24] : maxVotage;
					minVotage = HoTTbinReader.pointsGAM[j + 24] < minVotage ? HoTTbinReader.pointsGAM[j + 24] : minVotage;
				}
			}
			HoTTbinReader.pointsGAM[23] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
			HoTTbinReader.pointsGAM[30] = DataParser.parse2Short(_buf2, 8) * 1000;
			HoTTbinReader.pointsGAM[31] = DataParser.parse2Short(_buf2, 6) * 1000;
			HoTTbinReader.pointsGAM[9] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsGAM[10] = (DataParser.parse2UnsignedShort(_buf3, 2) - 30000) * 10;
			HoTTbinReader.pointsGAM[11] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsGAM[32] = HoTTbinReader.tmpVoltage1 * 100;
			HoTTbinReader.pointsGAM[33] = HoTTbinReader.tmpVoltage2 * 100;
			HoTTbinReader.pointsGAM[34] = ((_buf2[3] & 0xFF) - 20) * 1000;
			HoTTbinReader.pointsGAM[35] = ((_buf2[4] & 0xFF) - 20) * 1000;
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
	protected static void parseEAM(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) throws DataInconsitsentException {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
		//9=Height, 10=Climb 1, 11=Climb 3, 12=Climb 10
		//13=Latitude, 14=Longitude, 15=Velocity, 16=DistanceStart, 17=DirectionStart, 18=TripDistance
		//19=Voltage G, 20=Current G, 21=Capacity G, 22=Power G, 23=Balance G, 24=CellVoltage G1, 25=CellVoltage G2 .... 29=CellVoltage G6, 30=Revolution G, 31=FuelLevel, 32=Voltage G1, 33=Voltage G2, 34=Temperature G1, 35=Temperature G2
		//36=Voltage E, 37=Current E, 38=Capacity E, 39=Power E, 40=Balance E, 41=CellVoltage E1, 42=CellVoltage E2 .... 54=CellVoltage E14, 55=Voltage E1, 56=Voltage E2, 57=Temperature E1, 58=Temperature E2 59=Revolution E
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf3, 3) - 500;
		HoTTbinReader.tmpClimb3 = (_buf4[3] & 0xFF) - 120;
		HoTTbinReader.tmpVoltage1 = DataParser.parse2Short(_buf2, 7);
		HoTTbinReader.tmpVoltage2 = DataParser.parse2Short(_buf2[9], _buf3[0]);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
		if (!HoTTAdapter.isFilterEnabled
				|| (HoTTbinReader.tmpClimb3 > -90 && HoTTbinReader.tmpHeight >= -490 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600 && Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			HoTTbinReader.pointsEAM[36] = DataParser.parse2Short(_buf3, 7) * 1000;
			HoTTbinReader.pointsEAM[37] = DataParser.parse2Short(_buf3, 5) * 1000;
			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader2.recordSet.getRecordDataSize(true) <= 20
					|| Math.abs(HoTTbinReader.tmpCapacity) <= (HoTTbinReader.pointsEAM[38] / 1000 + HoTTbinReader.pointsEAM[36] / 1000 * HoTTbinReader.pointsEAM[37] / 1000 / 2500 + 2)) {
				HoTTbinReader.pointsEAM[38] = HoTTbinReader.tmpCapacity * 1000;
			}
			else {
				HoTTbinReader2.log.log(Level.WARNING, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - "
						+ (HoTTbinReader.pointsEAM[38] / 1000) + " + " + (HoTTbinReader.pointsEAM[36] / 1000 * HoTTbinReader.pointsEAM[37] / 1000 / 2500 + 2));
			}
			HoTTbinReader.pointsEAM[39] = Double.valueOf(HoTTbinReader.pointsEAM[36] / 1000.0 * HoTTbinReader.pointsEAM[37]).intValue(); // power U*I [W];
			for (int j = 0; j < 7; j++) {
				HoTTbinReader.pointsEAM[j + 41] = (_buf1[3 + j] & 0xFF) * 1000;
				if (HoTTbinReader.pointsEAM[j + 41] > 0) {
					maxVotage = HoTTbinReader.pointsEAM[j + 41] > maxVotage ? HoTTbinReader.pointsEAM[j + 41] : maxVotage;
					minVotage = HoTTbinReader.pointsEAM[j + 41] < minVotage ? HoTTbinReader.pointsEAM[j + 41] : minVotage;
				}
			}
			for (int j = 0; j < 7; j++) {
				HoTTbinReader.pointsEAM[j + 48] = (_buf2[j] & 0xFF) * 1000;
				if (HoTTbinReader.pointsEAM[j + 48] > 0) {
					maxVotage = HoTTbinReader.pointsEAM[j + 48] > maxVotage ? HoTTbinReader.pointsEAM[j + 48] : maxVotage;
					minVotage = HoTTbinReader.pointsEAM[j + 48] < minVotage ? HoTTbinReader.pointsEAM[j + 48] : minVotage;
				}
			}
			//calculate balance on the fly
			HoTTbinReader.pointsEAM[40] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
			HoTTbinReader.pointsEAM[9] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsEAM[10] = (DataParser.parse2UnsignedShort(_buf4, 1) - 30000) * 10;
			HoTTbinReader.pointsEAM[11] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsEAM[55] = HoTTbinReader.tmpVoltage1 * 100;
			HoTTbinReader.pointsEAM[56] = HoTTbinReader.tmpVoltage2 * 100;
			HoTTbinReader.pointsEAM[57] = ((_buf3[1] & 0xFF) - 20) * 1000;
			HoTTbinReader.pointsEAM[58] = ((_buf3[2] & 0xFF) - 20) * 1000;
			HoTTbinReader.pointsEAM[59] = DataParser.parse2Short(_buf4, 4) * 1000;
		}
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 * @param _buf
	 */
	protected static void parseChannel(byte[] _buf) {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
		//9=Height, 10=Climb 1, 11=Climb 3, 12=Climb 10
		//13=Latitude, 14=Longitude, 15=Velocity, 16=DistanceStart, 17=DirectionStart, 18=TripDistance
		//19=Voltage G, 20=Current G, 21=Capacity G, 22=Power G, 23=Balance G, 24=CellVoltage G1, 25=CellVoltage G2 .... 29=CellVoltage G6, 30=Revolution G, 31=FuelLevel, 32=Voltage G1, 33=Voltage G2, 34=Temperature G1, 35=Temperature G2
		//36=Voltage E, 37=Current E, 38=Capacity E, 39=Power E, 40=Balance E, 41=CellVoltage E1, 42=CellVoltage E2 .... 54=CellVoltage E14, 55=Voltage E1, 56=Voltage E2, 57=Temperature E1, 58=Temperature E2 59=Revolution E
		//60=Ch 1, 61=Ch 2, 62=Ch 3 .. 75=Ch 16, 76=PowerOff, 77=BatterieLow, 78=Reset, 79=reserve
		HoTTbinReader2.points[4] = (_buf[3] & 0xFF) * -1000;
		HoTTbinReader2.points[5] = (_buf[4] & 0xFF) * -1000;

		HoTTbinReader2.points[60] = (DataParser.parse2UnsignedShort(_buf, 8) / 2) * 1000; //1197
		HoTTbinReader2.points[61] = (DataParser.parse2UnsignedShort(_buf, 10) / 2) * 1000; //
		HoTTbinReader2.points[62] = (DataParser.parse2UnsignedShort(_buf, 12) / 2) * 1000;
		HoTTbinReader2.points[63] = (DataParser.parse2UnsignedShort(_buf, 14) / 2) * 1000;
		HoTTbinReader2.points[64] = (DataParser.parse2UnsignedShort(_buf, 16) / 2) * 1000;
		HoTTbinReader2.points[65] = (DataParser.parse2UnsignedShort(_buf, 18) / 2) * 1000;
		HoTTbinReader2.points[66] = (DataParser.parse2UnsignedShort(_buf, 20) / 2) * 1000;
		HoTTbinReader2.points[67] = (DataParser.parse2UnsignedShort(_buf, 22) / 2) * 1000;
		//events
		HoTTbinReader2.points[76] = (_buf[50] & 0x01) * 100000;
		HoTTbinReader2.points[77] = (_buf[50] & 0x02) * 50000;
		HoTTbinReader2.points[78] = (_buf[50] & 0x04) * 25000;
		HoTTbinReader2.points[79] = (_buf[50] & 0x00) * 1000; //reserved for future use

		if (_buf[5] == 0x00) { //channel 9-12
			HoTTbinReader2.points[68] = (DataParser.parse2UnsignedShort(_buf, 24) / 2) * 1000;
			HoTTbinReader2.points[69] = (DataParser.parse2UnsignedShort(_buf, 26) / 2) * 1000;
			HoTTbinReader2.points[70] = (DataParser.parse2UnsignedShort(_buf, 28) / 2) * 1000;
			HoTTbinReader2.points[71] = (DataParser.parse2UnsignedShort(_buf, 30) / 2) * 1000;
			if (HoTTbinReader2.points[72] == 0) {
				HoTTbinReader2.points[72] = 1500 * 1000;
				HoTTbinReader2.points[73] = 1500 * 1000;
				HoTTbinReader2.points[74] = 1500 * 1000;
				HoTTbinReader2.points[75] = 1500 * 1000;
			}
		}
		else { //channel 13-16
			HoTTbinReader2.points[72] = (DataParser.parse2UnsignedShort(_buf, 24) / 2) * 1000;
			HoTTbinReader2.points[73] = (DataParser.parse2UnsignedShort(_buf, 26) / 2) * 1000;
			HoTTbinReader2.points[74] = (DataParser.parse2UnsignedShort(_buf, 28) / 2) * 1000;
			HoTTbinReader2.points[75] = (DataParser.parse2UnsignedShort(_buf, 30) / 2) * 1000;
			if (HoTTbinReader2.points[68] == 0) {
				HoTTbinReader2.points[68] = 1500 * 1000;
				HoTTbinReader2.points[69] = 1500 * 1000;
				HoTTbinReader2.points[70] = 1500 * 1000;
				HoTTbinReader2.points[71] = 1500 * 1000;
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
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
		//9=Height, 10=Climb 1, 11=Climb 3, 12=Climb 10
		//13=Latitude, 14=Longitude, 15=Velocity, 16=DistanceStart, 17=DirectionStart, 18=TripDistance
		//19=Voltage G, 20=Current G, 21=Capacity G, 22=Power G, 23=Balance G, 24=CellVoltage G1, 25=CellVoltage G2 .... 29=CellVoltage G6, 30=Revolution G, 31=FuelLevel, 32=Voltage G1, 33=Voltage G2, 34=Temperature G1, 35=Temperature G2
		//36=Voltage E, 37=Current E, 38=Capacity E, 39=Power E, 40=Balance E, 41=CellVoltage E1, 42=CellVoltage E2 .... 54=CellVoltage E14, 55=Voltage E1, 56=Voltage E2, 57=Temperature E1, 58=Temperature E2 59=Revolution E
		//60=Ch 1, 61=Ch 2, 62=Ch 3 .. 75=Ch 16, 76=PowerOff, 77=BatterieLow, 78=Reset, 79=reserve
		//80=VoltageM, 81=CurrentM, 82=CapacityM, 83=PowerM, 84=RevolutionM, 85=TemperatureM 1, 86=TemperatureM 2
	  //87=Voltage_min, 88=Current_max, 89=Revolution_max, 90=Temperature1_max, 91=Temperature2_max
		//60=VoltageM, 61=CurrentM, 62=CapacityM, 63=PowerM, 64=RevolutionM, 65=TemperatureM 1, 66=TemperatureM 2
	  //67=Voltage_min, 68=Current_max, 69=Revolution_max, 70=Temperature1_max, 71=Temperature2_max
		HoTTbinReader.tmpVoltage = DataParser.parse2Short(_buf1, 3);
		HoTTbinReader.tmpCurrent = DataParser.parse2Short(_buf2, 1);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf1, 7);
		HoTTbinReader.tmpRevolution = DataParser.parse2Short(_buf2, 5);
		HoTTbinReader.tmpTemperatureFet = _buf1[9] - 20;
		if (channelNumber == 4) {
			//80=VoltageM, 81=CurrentM, 82=CapacityM, 83=PowerM, 84=RevolutionM, 85=TemperatureM 1, 86=TemperatureM 2
			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpVoltage > 0 && HoTTbinReader.tmpVoltage < 1000 && HoTTbinReader.tmpCurrent < 4000 && HoTTbinReader.tmpCurrent > -10
					&& HoTTbinReader.tmpRevolution > -1 && HoTTbinReader.tmpRevolution < 20000
					&& !(HoTTbinReader.pointsESC[85] != 0 && HoTTbinReader.pointsESC[85] / 1000 - HoTTbinReader.tmpTemperatureFet > 20)) {
				HoTTbinReader.pointsESC[80] = HoTTbinReader.tmpVoltage * 1000;
				HoTTbinReader.pointsESC[81] = HoTTbinReader.tmpCurrent * 1000;
				HoTTbinReader.pointsESC[83] = Double.valueOf(HoTTbinReader.pointsESC[80] / 1000.0 * HoTTbinReader.pointsESC[81]).intValue();
				if (!HoTTAdapter.isFilterEnabled || HoTTbinReader2.recordSet.getRecordDataSize(true) <= 20
						|| (HoTTbinReader.tmpCapacity != 0 && Math.abs(HoTTbinReader.tmpCapacity) <= (HoTTbinReader.pointsESC[82] / 1000 + HoTTbinReader.tmpVoltage * HoTTbinReader.tmpCurrent / 2500 + 2))) {
					HoTTbinReader.pointsESC[82] = HoTTbinReader.tmpCapacity * 1000;
				}
				else {
					if (HoTTbinReader.tmpCapacity != 0)
						HoTTbinReader2.log.log(Level.WARNING, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - "
							+ (HoTTbinReader.pointsESC[82] / 1000) + " + " + (HoTTbinReader.tmpVoltage * HoTTbinReader.tmpCurrent / 2500 + 2));
				}
				HoTTbinReader.pointsESC[84] = HoTTbinReader.tmpRevolution * 1000;
				HoTTbinReader.pointsESC[85] = HoTTbinReader.tmpTemperatureFet * 1000;

				HoTTbinReader.pointsESC[86] = (_buf2[9] - 20) * 1000;
			  //87=Voltage_min, 88=Current_max, 89=Revolution_max, 90=Temperature1_max, 91=Temperature2_max
				HoTTbinReader.pointsESC[87] = DataParser.parse2Short(_buf1, 5) * 1000;
				HoTTbinReader.pointsESC[88] = DataParser.parse2Short(_buf2, 3) * 1000;
				HoTTbinReader.pointsESC[89] = DataParser.parse2Short(_buf2, 7) * 1000;
				HoTTbinReader.pointsESC[90] = (_buf2[0] - 20) * 1000;
				HoTTbinReader.pointsESC[91] = (_buf3[0] - 20) * 1000;
			}
		}
		else {
			//60=VoltageM, 61=CurrentM, 62=CapacityM, 63=PowerM, 64=RevolutionM, 65=TemperatureM 1, 66=TemperatureM 2
		  //67=Voltage_min, 68=Current_max, 69=Revolution_max, 70=Temperature1_max, 71=Temperature2_max
			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpVoltage > 0 && HoTTbinReader.tmpVoltage < 1000 && HoTTbinReader.tmpCurrent < 4000 && HoTTbinReader.tmpCurrent > -10
					&& HoTTbinReader.tmpRevolution > -1 && HoTTbinReader.tmpRevolution < 20000
					&& !(HoTTbinReader.pointsESC[65] != 0 && HoTTbinReader.pointsESC[65] / 1000 - HoTTbinReader.tmpTemperatureFet > 20)) {
				HoTTbinReader.pointsESC[60] = HoTTbinReader.tmpVoltage * 1000;
				HoTTbinReader.pointsESC[61] = HoTTbinReader.tmpCurrent * 1000;
				HoTTbinReader.pointsESC[63] = Double.valueOf(HoTTbinReader.pointsESC[60] / 1000.0 * HoTTbinReader.pointsESC[61]).intValue();
				if (!HoTTAdapter.isFilterEnabled || HoTTbinReader2.recordSet.getRecordDataSize(true) <= 20
						|| (HoTTbinReader.tmpCapacity != 0 && Math.abs(HoTTbinReader.tmpCapacity) <= (HoTTbinReader.pointsESC[62] / 1000 + HoTTbinReader.tmpVoltage * HoTTbinReader.tmpCurrent / 2500 + 2))) {
					HoTTbinReader.pointsESC[62] = HoTTbinReader.tmpCapacity * 1000;
				}
				else {
					if (HoTTbinReader.tmpCapacity != 0)
						HoTTbinReader2.log.log(Level.WARNING, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - "
							+ (HoTTbinReader.pointsESC[62] / 1000) + " + " + (HoTTbinReader.tmpVoltage * HoTTbinReader.tmpCurrent / 2500 + 2));
				}
				HoTTbinReader.pointsESC[64] = HoTTbinReader.tmpRevolution * 1000;
				HoTTbinReader.pointsESC[65] = HoTTbinReader.tmpTemperatureFet * 1000;

				HoTTbinReader.pointsESC[66] = (_buf2[9] - 20) * 1000;
			  //67=Voltage_min, 68=Current_max, 69=Revolution_max, 70=Temperature1_max, 71=Temperature2_max
				HoTTbinReader.pointsESC[67] = DataParser.parse2Short(_buf1, 5) * 1000;
				HoTTbinReader.pointsESC[68] = DataParser.parse2Short(_buf2, 3) * 1000;
				HoTTbinReader.pointsESC[69] = DataParser.parse2Short(_buf2, 7) * 1000;
				HoTTbinReader.pointsESC[70] = (_buf2[0] - 20) * 1000;
				HoTTbinReader.pointsESC[71] = (_buf3[0] - 20) * 1000;
			}
		}
	}
}
