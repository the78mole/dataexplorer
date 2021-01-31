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
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.EnumSet;
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
	final static Logger	log	= Logger.getLogger(HoTTbinReader2.class.getName());
	static int[]				points;
	static RecordSet		recordSet;

	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws Exception
	 */
	public static synchronized void read(String filePath, PickerParameters newPickerParameters) throws Exception {
		HoTTbinReader.pickerParameters = newPickerParameters;
		HashMap<String, String> header = getFileInfo(new File(filePath), newPickerParameters);
		HoTTbinReader2.detectedSensors = Sensor.getSetFromDetected(header.get(HoTTAdapter.DETECTED_SENSOR));
		
		//set picker parameter setting sensor for altitude/climb usage (0=auto, 1=VARIO, 2=GPS, 3=GAM, 4=EAM)
		HoTTbinReader.setAltitudeClimbPickeParameter(HoTTbinReader.pickerParameters, HoTTbinReader2.detectedSensors);

		if (HoTTbinReader2.detectedSensors.size() <= 2) {
			HoTTbinReader.isReceiverOnly = HoTTbinReader2.detectedSensors.size() == 1;
			readSingle(new File(header.get(HoTTAdapter.FILE_PATH)), header);
		} else
			readMultiple(new File(header.get(HoTTAdapter.FILE_PATH)), header);
	}

	/**
	 * read log data according to version 0
	 * @param file
	 * @param data_in
	 * @throws IOException
	 * @throws DataInconsitsentException
	 */
	static void readSingle(File file, HashMap<String, String> header) throws IOException, DataInconsitsentException {
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		IDevice device = HoTTbinReader.application.getActiveDevice();
		int recordSetNumber = HoTTbinReader.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		int channelNumber = HoTTbinReader2.pickerParameters.analyzer.getActiveChannel().getNumber();
		device.getMeasurementFactor(channelNumber, 12);
		boolean isReceiverData = false;
		boolean isSensorData = false;
		boolean[] isResetMinMax = new boolean[] {false, false, false, false, false}; //ESC, EAM, GAM, GPS, Vario
		HoTTbinReader2.recordSet = null;
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
		HoTTbinReader2.points = new int[device.getNumberOfMeasurements(channelNumber)];
		HoTTbinReader.pointsGAM = HoTTbinReader.pointsEAM = HoTTbinReader.pointsESC = HoTTbinReader.pointsVario = HoTTbinReader.pointsGPS = HoTTbinReader2.points;
		HoTTbinReader.dataBlockSize = 64;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		HoTTbinReader.buf0 = new byte[30];
		HoTTbinReader.buf1 = new byte[30];
		HoTTbinReader.buf2 = new byte[30];
		HoTTbinReader.buf3 = new byte[30];
		HoTTbinReader.buf4 = new byte[30];
		BufCopier bufCopier = new BufCopier(buf, buf0, buf1, buf2, buf3, buf4);
		long[] timeSteps_ms = new long[] { 0 };
		HoTTbinReader.rcvBinParser = Sensor.RECEIVER.createBinParser2(HoTTbinReader.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf });
		HoTTbinReader.chnBinParser = Sensor.CHANNEL.createBinParser2(HoTTbinReader.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf });
		HoTTbinReader.varBinParser = Sensor.VARIO.createBinParser2(HoTTbinReader.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf0, buf1, buf2 });
		HoTTbinReader.gpsBinParser = Sensor.GPS.createBinParser2(HoTTbinReader.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader.gamBinParser = Sensor.GAM.createBinParser2(HoTTbinReader.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader.eamBinParser = Sensor.EAM.createBinParser2(HoTTbinReader.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader.escBinParser = Sensor.ESC.createBinParser2(HoTTbinReader.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3 });
		HoTTbinReader.isTextModusSignaled = false;
		boolean isSdLogFormat = Boolean.parseBoolean(header.get(HoTTAdapter.SD_FORMAT));
		long numberDatablocks = isSdLogFormat ? fileSize - HoTTbinReaderX.headerSize - HoTTbinReaderX.footerSize : fileSize / HoTTbinReader.dataBlockSize;
		long startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks);
		numberDatablocks = HoTTbinReader.isReceiverOnly && channelNumber != HoTTAdapter2.CHANNELS_CHANNEL_NUMBER ? numberDatablocks / 10 : numberDatablocks;
		String date = StringHelper.getDate();
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		MenuToolBar menuToolBar = HoTTbinReader.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		GDE.getUiNotification().setProgress(0);
		if (isSdLogFormat) data_in.skip(HoTTbinReaderX.headerSize);

		try {
			// check if recordSet initialized, transmitter and receiver data always present, but not in the same data rate and signals
			channel = HoTTbinReader.channels.get(channelNumber);
			channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
					: date);
			recordSetName = recordSetNumber + device.getRecordSetStemNameReplacement() + recordSetNameExtend;
			HoTTbinReader2.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true, true);
			channel.put(recordSetName, HoTTbinReader2.recordSet);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			// recordSet initialized and ready to add data

			// read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTbinReader2.log.isLoggable(Level.FINE) && i % 10 == 0) {
					HoTTbinReader2.log.log(Level.FINE, StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
					HoTTbinReader2.log.log(Level.FINE, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if (!HoTTbinReader.pickerParameters.isFilterTextModus || (HoTTbinReader.buf[6] & 0x01) == 0) { // switch into text modus
					if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { // buf 3, 4, tx,rx
						if (HoTTbinReader2.log.isLoggable(Level.FINE))
							HoTTbinReader2.log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", HoTTbinReader.buf[7], HoTTbinReader.buf[33]));

						((RcvBinParser) HoTTbinReader.rcvBinParser).trackPackageLoss(true);
						if (HoTTbinReader2.log.isLoggable(Level.FINER)) HoTTbinReader2.log.log(Level.FINER, StringHelper.byte2Hex2CharString(new byte[] {
								HoTTbinReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

						// fill receiver data
						if (HoTTbinReader.buf[33] == 0 && (HoTTbinReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader.buf, 40) >= 0) {
							HoTTbinReader2.rcvBinParser.parse();
							isReceiverData = true;
						}
						if (channelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER) {
							HoTTbinReader2.chnBinParser.parse(); // Channels
						}

						// fill data block 0 receiver voltage an temperature
						if (buf[33] == 0) {
							bufCopier.copyToBuffer();
						}
						if (HoTTbinReader.isReceiverOnly && channelNumber != HoTTAdapter2.CHANNELS_CHANNEL_NUMBER) { // reduce data rate for receiver to 0.1 sec
							for (int j = 0; j < 9; j++) {
								data_in.read(HoTTbinReader.buf);
								timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
							}
							isSensorData = true;
						}

						// create and fill sensor specific data record sets
						switch ((byte) (HoTTbinReader.buf[7] & 0xFF)) {
						case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
						case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
							if (detectedSensors.contains(Sensor.VARIO)) {
								bufCopier.copyToVarioBuffer();
								if (bufCopier.is2BuffersFull()) {
									HoTTbinReader2.varBinParser.parse();
									bufCopier.clearBuffers();
									isSensorData = true;
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_GPS_115200:
						case HoTTAdapter.SENSOR_TYPE_GPS_19200:
							if (detectedSensors.contains(Sensor.GPS)) {
								bufCopier.copyToFreeBuffer();
								if (bufCopier.is3BuffersFull()) {
									HoTTbinReader2.gpsBinParser.parse();
									bufCopier.clearBuffers();
									isSensorData = true;
									// 15=Latitude, 16=Longitude, 17=Velocity, 18=Distance, 19=Direction, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
									// 24=HomeDirection 25=Roll 26=Pitch 27=Yaw 28=GyroX 29=GyroY 30=GyroZ 31=Vibration 32=Version	
									if (!isResetMinMax[3] && HoTTbinReader2.points[22] == 3000 && HoTTbinReader2.points[15] != 0 && HoTTbinReader2.points[16] != 0) {
										for (int j=15; j<32; ++j) {
											tmpRecordSet.get(j).setMinMax(HoTTbinReader2.points[j], HoTTbinReader2.points[j]);
										}
										isResetMinMax[3] = true;
									}
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
						case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
							if (detectedSensors.contains(Sensor.GAM)) {
								bufCopier.copyToFreeBuffer();
								if (bufCopier.is4BuffersFull()) {
									HoTTbinReader2.gamBinParser.parse();
									bufCopier.clearBuffers();
									isSensorData = true;
									// 33=Voltage G, 34=Current G, 35=Capacity G, 36=Power G, 37=Balance G, 38=CellVoltage G1, 39=CellVoltage G2 .... 43=CellVoltage G6,
									// 44=Revolution G, 45=FuelLevel, 46=Voltage G1, 47=Voltage G2, 48=Temperature G1, 49=Temperature G2 50=Speed G, 51=LowestCellVoltage,
									// 52=LowestCellNumber, 53=Pressure, 54=Event G
									if (!isResetMinMax[2] && HoTTbinReader2.points[33] != 0) {
										for (int j=33; j<54; ++j) {
											tmpRecordSet.get(j).setMinMax(HoTTbinReader2.points[j], HoTTbinReader2.points[j]);
										}
										isResetMinMax[2] = true;
									}
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
							if (detectedSensors.contains(Sensor.EAM)) {
								bufCopier.copyToFreeBuffer();
								if (bufCopier.is4BuffersFull()) {
									HoTTbinReader2.eamBinParser.parse();
									bufCopier.clearBuffers();
									isSensorData = true;
									// 55=Voltage E, 56=Current E, 57=Capacity E, 58=Power E, 59=Balance E, 60=CellVoltage E1, 61=CellVoltage E2 .... 73=CellVoltage E14,
									// 74=Voltage E1, 75=Voltage E2, 76=Temperature E1, 77=Temperature E2 78=Revolution E 79=MotorTime 80=Speed 81=Event E
									if (!isResetMinMax[1] && HoTTbinReader2.points[55] != 0) {
										for (int j=55; j<94; ++j) {
											tmpRecordSet.get(j).setMinMax(HoTTbinReader2.points[j], HoTTbinReader2.points[j]);
										}
										isResetMinMax[1] = true;
									}
								}
							}
							break;

						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
							if (detectedSensors.contains(Sensor.ESC)) {
								bufCopier.copyToFreeBuffer();
								if (bufCopier.is3BuffersFull()) {
									HoTTbinReader2.escBinParser.parse();
									bufCopier.clearBuffers();
									isSensorData = true;
									if (((EscBinParser) HoTTbinReader2.escBinParser).isChannelsChannel()) {
										// 102=VoltageM, 103=CurrentM, 104=CapacityM, 105=PowerM, 106=RevolutionM, 107=TemperatureM 1, 108=TemperatureM 2 109=Voltage_min, 110=Current_max,
										// 111=Revolution_max, 112=Temperature1_max, 113=Temperature2_max 114=Event M
										if (!isResetMinMax[0] && HoTTbinReader2.points[102] != 0) {
											for (int j=102; j<114; ++j) {
												tmpRecordSet.get(j).setMinMax(HoTTbinReader2.points[j], HoTTbinReader2.points[j]);
											}
											isResetMinMax[0] = true;
										}
									} else {
										// 82=VoltageM, 83=CurrentM, 84=CapacityM, 85=PowerM, 86=RevolutionM, 87=TemperatureM 1, 88=TemperatureM 2 89=Voltage_min, 90=Current_max,
										// 91=Revolution_max, 92=Temperature1_max, 93=Temperature2_max 94=Event M
										if (!isResetMinMax[0] && HoTTbinReader2.points[82] != 0) {
											for (int j=82; j<94; ++j) {
												tmpRecordSet.get(j).setMinMax(HoTTbinReader2.points[j], HoTTbinReader2.points[j]);
											}
											isResetMinMax[0] = true;
										}
									}
								}
							}
							break;
						}

						if (isSensorData) {
							((RcvBinParser) HoTTbinReader.rcvBinParser).updateLossStatistics();
						}

						if (isSensorData || (isReceiverData && tmpRecordSet.get(0).realSize() > 0)) {
							tmpRecordSet.addPoints(HoTTbinReader2.points, timeSteps_ms[BinParser.TIMESTEP_INDEX]);
							isSensorData = isReceiverData = false;
						} else if (channelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER) {
							tmpRecordSet.addPoints(HoTTbinReader2.points, timeSteps_ms[BinParser.TIMESTEP_INDEX]);
						}

						timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10; // add default time step from device of 10 msec

						if (i % progressIndicator == 0) GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));
					} else { // skip empty block, but add time step
						if (HoTTbinReader2.log.isLoggable(Level.FINE)) HoTTbinReader2.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

						((RcvBinParser) HoTTbinReader.rcvBinParser).trackPackageLoss(false);

						if (channelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER) {
							HoTTbinReader2.chnBinParser.parse(); // Channels
							tmpRecordSet.addPoints(HoTTbinReader2.points, timeSteps_ms[BinParser.TIMESTEP_INDEX]);
						}
						timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
						// reset buffer to avoid mixing data >> 20 Jul 14, not any longer required due to protocol change requesting next sensor data block
						// HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
					}
				} else if (!HoTTbinReader.isTextModusSignaled) {
					HoTTbinReader.isTextModusSignaled = true;
					HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			String packageLossPercentage = tmpRecordSet.getRecordDataSize(true) > 0
					? String.format("%.1f", (((RcvBinParser) HoTTbinReader2.rcvBinParser).getCountPackageLoss() / tmpRecordSet.getTime_ms(tmpRecordSet.getRecordDataSize(true) - 1) * 1000))
					: "100";
			tmpRecordSet.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] { ((RcvBinParser) HoTTbinReader2.rcvBinParser).getCountPackageLoss(), packageLossPercentage, ((RcvBinParser) HoTTbinReader.rcvBinParser).getLostPackages().getStatistics() })
					+ Sensor.getSetAsSignature(HoTTbinReader.detectedSensors));
			HoTTbinReader2.log.log(Level.WARNING, "skipped number receiver data due to package loss = " + ((RcvBinParser) HoTTbinReader2.rcvBinParser).getCountPackageLoss()); //$NON-NLS-1$
			HoTTbinReader2.log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (GDE.isWithUi()) {
				GDE.getUiNotification().setProgress(99);
				device.updateVisibilityStatus(tmpRecordSet, true);
				channel.applyTemplate(recordSetName, false);

				// write filename after import to record description
				tmpRecordSet.descriptionAppendFilename(file.getName());

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				GDE.getUiNotification().setProgress(100);
			}
		} finally {
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
	static void readMultiple(File file, HashMap<String, String> header) throws IOException, DataInconsitsentException {
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		IDevice device = HoTTbinReader.application.getActiveDevice();
		int recordSetNumber = HoTTbinReader.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		int channelNumber = HoTTbinReader2.pickerParameters.analyzer.getActiveChannel().getNumber();
		boolean isReceiverData = false;
		HoTTbinReader2.recordSet = null;
		boolean isJustMigrated = false;
		boolean[] isResetMinMax = new boolean[] {false, false, false, false, false}; //ESC, EAM, GAM, GPS, Vario
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
		HoTTbinReader2.points = new int[device.getNumberOfMeasurements(channelNumber)];
		HoTTbinReader.pointsGAM = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsEAM = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsESC = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsVario = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsVario[2] = 100000;
		HoTTbinReader.pointsGPS = new int[HoTTbinReader2.points.length];
		HoTTbinReader.dataBlockSize = 64;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		HoTTbinReader.buf0 = new byte[30];
		HoTTbinReader.buf1 = new byte[30];
		HoTTbinReader.buf2 = new byte[30];
		HoTTbinReader.buf3 = new byte[30];
		HoTTbinReader.buf4 = new byte[30];
		BufCopier bufCopier = new BufCopier(buf, buf0, buf1, buf2, buf3, buf4);
		long[] timeSteps_ms = new long[] { 0 };
		// parse in situ for receiver and channel
		HoTTbinReader.rcvBinParser = Sensor.RECEIVER.createBinParser2(HoTTbinReader.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf });
		HoTTbinReader.chnBinParser = Sensor.CHANNEL.createBinParser2(HoTTbinReader.pickerParameters, HoTTbinReader2.points, timeSteps_ms, new byte[][] { buf });
		// use parser points objects
		HoTTbinReader.varBinParser = Sensor.VARIO.createBinParser2(HoTTbinReader.pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2 });
		HoTTbinReader.gpsBinParser = Sensor.GPS.createBinParser2(HoTTbinReader.pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader.gamBinParser = Sensor.GAM.createBinParser2(HoTTbinReader.pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader.eamBinParser = Sensor.EAM.createBinParser2(HoTTbinReader.pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		HoTTbinReader.escBinParser = Sensor.ESC.createBinParser2(HoTTbinReader.pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3 });
		byte actualSensor = -1, lastSensor = -1;
		int logCountVario = 0, logCountGPS = 0, logCountGAM = 0, logCountEAM = 0, logCountESC = 0;
		EnumSet<Sensor> migrationJobs = EnumSet.noneOf(Sensor.class);

		boolean isSdLogFormat = Boolean.parseBoolean(header.get(HoTTAdapter.SD_FORMAT));
		long numberDatablocks = isSdLogFormat ? fileSize - HoTTbinReaderX.headerSize - HoTTbinReaderX.footerSize : fileSize / HoTTbinReader.dataBlockSize;
		long startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks);
		String date = StringHelper.getDate();
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		MenuToolBar menuToolBar = HoTTbinReader.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		GDE.getUiNotification().setProgress(0);
		if (isSdLogFormat) data_in.skip(HoTTbinReaderX.headerSize);

		try {
			// receiver data are always contained
			channel = HoTTbinReader.channels.get(channelNumber);
			channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
					: date);
			recordSetName = recordSetNumber + device.getRecordSetStemNameReplacement() + recordSetNameExtend;
			HoTTbinReader2.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true, true);
			channel.put(recordSetName, HoTTbinReader2.recordSet);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			// recordSet initialized and ready to add data

			// read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTbinReader2.log.isLoggable(Level.FINEST)) {
					HoTTbinReader2.log.log(Level.FINEST, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if (!HoTTbinReader.pickerParameters.isFilterTextModus || (HoTTbinReader.buf[6] & 0x01) == 0) { // switch into text modus
					if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { // buf 3, 4, tx,rx
						if (HoTTbinReader2.log.isLoggable(Level.FINE))
							HoTTbinReader2.log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", HoTTbinReader.buf[7], HoTTbinReader.buf[33]));

						((RcvBinParser) HoTTbinReader.rcvBinParser).trackPackageLoss(true);
						if (HoTTbinReader2.log.isLoggable(Level.FINEST)) HoTTbinReader2.log.log(Level.FINEST, StringHelper.byte2Hex2CharString(new byte[] {
								HoTTbinReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

						// fill receiver data
						if (HoTTbinReader.buf[33] == 0 && (HoTTbinReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader.buf, 40) >= 0) {
							HoTTbinReader2.rcvBinParser.parse();
							isReceiverData = true;
						}
						if (channelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER) {
							HoTTbinReader2.chnBinParser.parse();
						}

						if (actualSensor == -1)
							lastSensor = actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);
						else
							actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);

						if (actualSensor != lastSensor) {
							if (logCountVario >= 3 || logCountGPS >= 4 || logCountGAM >= 5 || logCountEAM >= 5 || logCountESC >= 4) {
								switch (lastSensor) {
								case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
								case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
									if (detectedSensors.contains(Sensor.VARIO)) {
										if (migrationJobs.contains(Sensor.VARIO) && isReceiverData) {
											migrateAddPoints(tmpRecordSet, migrationJobs, timeSteps_ms[BinParser.TIMESTEP_INDEX], isResetMinMax);
											isJustMigrated = true;
											isReceiverData = false;
										}
										HoTTbinReader2.varBinParser.parse();
										migrationJobs.add(Sensor.VARIO);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_GPS_115200:
								case HoTTAdapter.SENSOR_TYPE_GPS_19200:
									if (detectedSensors.contains(Sensor.GPS)) {
										if (migrationJobs.contains(Sensor.GPS) && isReceiverData) {
											migrateAddPoints(tmpRecordSet, migrationJobs, timeSteps_ms[BinParser.TIMESTEP_INDEX], isResetMinMax);
											isJustMigrated = true;
											isReceiverData = false;
										}
										HoTTbinReader2.gpsBinParser.parse();
										migrationJobs.add(Sensor.GPS);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
								case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
									if (detectedSensors.contains(Sensor.GAM)) {
										if (migrationJobs.contains(Sensor.GAM) && isReceiverData) {
											migrateAddPoints(tmpRecordSet, migrationJobs, timeSteps_ms[BinParser.TIMESTEP_INDEX], isResetMinMax);
											isJustMigrated = true;
											isReceiverData = false;
										}
										HoTTbinReader2.gamBinParser.parse();
										migrationJobs.add(Sensor.GAM);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
									if (detectedSensors.contains(Sensor.EAM)) {
										if (migrationJobs.contains(Sensor.EAM) && isReceiverData) {
											migrateAddPoints(tmpRecordSet, migrationJobs, timeSteps_ms[BinParser.TIMESTEP_INDEX], isResetMinMax);
											isJustMigrated = true;
											isReceiverData = false;
										}
										HoTTbinReader2.eamBinParser.parse();
										migrationJobs.add(Sensor.EAM);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
								case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
									if (detectedSensors.contains(Sensor.ESC)) {
										if (migrationJobs.contains(Sensor.ESC) && isReceiverData) {
											migrateAddPoints(tmpRecordSet, migrationJobs, timeSteps_ms[BinParser.TIMESTEP_INDEX], isResetMinMax);
											isJustMigrated = true;
											isReceiverData = false;
										}
										HoTTbinReader2.escBinParser.parse();
										migrationJobs.add(Sensor.ESC);
									}
									break;
								}

								if (HoTTbinReader2.log.isLoggable(Level.FINE)) HoTTbinReader2.log.log(Level.FINE, "isReceiverData " + isReceiverData + " migrationJobs " + migrationJobs);
							}

							if (HoTTbinReader2.log.isLoggable(Level.FINE))
								HoTTbinReader2.log.log(Level.FINE, "logCountVario = " + logCountVario + " logCountGPS = " + logCountGPS + " logCountGeneral = " + logCountGAM + " logCountElectric = " + logCountEAM);
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

						if (isJustMigrated) {
							((RcvBinParser) HoTTbinReader.rcvBinParser).updateLossStatistics();
						}

						if (isReceiverData && (logCountVario > 0 || logCountGPS > 0 || logCountGAM > 0 || logCountEAM > 0 || logCountESC > 0)) {
							tmpRecordSet.addPoints(HoTTbinReader2.points, timeSteps_ms[BinParser.TIMESTEP_INDEX]);
							isReceiverData = false;
						}
						else if (channelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER && !isJustMigrated) {
							tmpRecordSet.addPoints(HoTTbinReader2.points, timeSteps_ms[BinParser.TIMESTEP_INDEX]);
						}
						isJustMigrated = false;

						bufCopier.copyToBuffer();
						timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;// add default time step from log record of 10 msec

						if (i % progressIndicator == 0) GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));
					} else { // skip empty block, but add time step
						if (HoTTbinReader2.log.isLoggable(Level.FINE)) HoTTbinReader2.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

						((RcvBinParser) HoTTbinReader.rcvBinParser).trackPackageLoss(false);
						if (channelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER) {
							HoTTbinReader2.chnBinParser.parse();
							tmpRecordSet.addPoints(HoTTbinReader2.points, timeSteps_ms[BinParser.TIMESTEP_INDEX]);
						}
						timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
						// reset buffer to avoid mixing data >> 20 Jul 14, not any longer required due to protocol change requesting next sensor data block
					}
				} else if (!HoTTbinReader.isTextModusSignaled) {
					HoTTbinReader.isTextModusSignaled = true;
					HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			// if (HoTTbinReader.oldProtocolCount > 2) {
			// application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2405, new Object[] {
			// HoTTbinReader.oldProtocolCount }));
			// }
			String packageLossPercentage = tmpRecordSet.getRecordDataSize(true) > 0
					? String.format("%.1f", (((RcvBinParser) HoTTbinReader.rcvBinParser).getCountPackageLoss() / tmpRecordSet.getTime_ms(tmpRecordSet.getRecordDataSize(true) - 1) * 1000))
					: "100";
			tmpRecordSet.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] { ((RcvBinParser) HoTTbinReader2.rcvBinParser).getCountPackageLoss(), packageLossPercentage, ((RcvBinParser) HoTTbinReader.rcvBinParser).getLostPackages().getStatistics() })
					+ Sensor.getSetAsSignature(HoTTbinReader.detectedSensors));
			HoTTbinReader2.log.log(Level.WARNING, "skipped number receiver data due to package loss = " + ((RcvBinParser) HoTTbinReader.rcvBinParser).getCountPackageLoss()); //$NON-NLS-1$
			HoTTbinReader2.log.log(Level.TIME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (menuToolBar != null) {
				GDE.getUiNotification().setProgress(99);
				device.makeInActiveDisplayable(tmpRecordSet);
				device.updateVisibilityStatus(tmpRecordSet, true);
				channel.applyTemplate(recordSetName, false);

				// write filename after import to record description
				tmpRecordSet.descriptionAppendFilename(file.getName());

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				GDE.getUiNotification().setProgress(100);
			}
		} finally {
			data_in.close();
			data_in = null;
		}
	}

	/**
	 * Migrate sensor measurement values in the correct priority and add to record set.
	 * Receiver data are always updated.
	 */
	// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
	// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
	// 82=VoltageM, 83=CurrentM, 84=CapacityM, 85=PowerM, 86=RevolutionM, 87=TemperatureM 1, 88=TemperatureM 2 89=Voltage_min, 90=Current_max,
	// 91=Revolution_max, 92=Temperature1_max, 93=Temperature2_max 94=Event M

	// 82=Ch 1, 83=Ch 2, 84=Ch 3 .. 97=Ch 16, 98=PowerOff, 99=BatterieLow, 100=Reset, 101=reserve
	// 102=VoltageM, 103=CurrentM, 104=CapacityM, 105=PowerM, 106=RevolutionM, 107=TemperatureM 1, 108=TemperatureM 2 109=Voltage_min, 110=Current_max,
	// 111=Revolution_max, 112=Temperature1_max, 113=Temperature2_max 114=Event M
	public static void migrateAddPoints(RecordSet tmpRecordSet, EnumSet<Sensor> migrationJobs, long timeStep_ms, boolean[] isResetMinMax) throws DataInconsitsentException {
		if (migrationJobs.contains(Sensor.EAM)) {
			HoTTbinReader2.eamBinParser.migratePoints(HoTTbinReader2.points);
			// 55=Voltage E, 56=Current E, 57=Capacity E, 58=Power E, 59=Balance E, 60=CellVoltage E1, 61=CellVoltage E2 .... 73=CellVoltage E14,
			// 74=Voltage E1, 75=Voltage E2, 76=Temperature E1, 77=Temperature E2 78=Revolution E 79=MotorTime 80=Speed 81=Event E
			if (!isResetMinMax[1] && HoTTbinReader2.points[55] != 0) {
				for (int i=55; i<81; ++i) {
					tmpRecordSet.get(i).setMinMax(HoTTbinReader2.points[i], HoTTbinReader2.points[i]);
				}
				isResetMinMax[1] = true;
			}
		}
		if (migrationJobs.contains(Sensor.GAM)) {
			HoTTbinReader2.gamBinParser.migratePoints(HoTTbinReader2.points);
			// 33=Voltage G, 34=Current G, 35=Capacity G, 36=Power G, 37=Balance G, 38=CellVoltage G1, 39=CellVoltage G2 .... 43=CellVoltage G6,
			// 44=Revolution G, 45=FuelLevel, 46=Voltage G1, 47=Voltage G2, 48=Temperature G1, 49=Temperature G2 50=Speed G, 51=LowestCellVoltage,
			// 52=LowestCellNumber, 53=Pressure, 54=Event G
			if (!isResetMinMax[2] && HoTTbinReader2.points[33] != 0) {
				for (int i=33; i<54; ++i) {
					tmpRecordSet.get(i).setMinMax(HoTTbinReader2.points[i], HoTTbinReader2.points[i]);
				}
				isResetMinMax[2] = true;
			}
		}
		if (migrationJobs.contains(Sensor.GPS)) {
			HoTTbinReader2.gpsBinParser.migratePoints(HoTTbinReader2.points);
			// 15=Latitude, 16=Longitude, 17=Velocity, 18=Distance, 19=Direction, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
			// 24=HomeDirection 25=Roll 26=Pitch 27=Yaw 28=GyroX 29=GyroY 30=GyroZ 31=Vibration 32=Version	
			if (!isResetMinMax[3] && HoTTbinReader2.points[22] >= 3000  && HoTTbinReader2.points[15] != 0 && HoTTbinReader2.points[16] != 0) {
				for (int i=15; i<32; ++i) {
					tmpRecordSet.get(i).setMinMax(HoTTbinReader2.points[i], HoTTbinReader2.points[i]);
				}
				isResetMinMax[3] = true;
			}
		}
		if (migrationJobs.contains(Sensor.VARIO)) {
			HoTTbinReader2.varBinParser.migratePoints(HoTTbinReader2.points);
		}
		if (migrationJobs.contains(Sensor.ESC)) {
			HoTTbinReader2.escBinParser.migratePoints(HoTTbinReader2.points);
			if (((EscBinParser) HoTTbinReader2.escBinParser).isChannelsChannel()) {
				// 102=VoltageM, 103=CurrentM, 104=CapacityM, 105=PowerM, 106=RevolutionM, 107=TemperatureM 1, 108=TemperatureM 2 109=Voltage_min, 110=Current_max,
				// 111=Revolution_max, 112=Temperature1_max, 113=Temperature2_max 114=Event M
				if (!isResetMinMax[0] && HoTTbinReader2.points[102] != 0) {
					for (int i=102; i<114; ++i) {
						tmpRecordSet.get(i).setMinMax(HoTTbinReader2.points[i], HoTTbinReader2.points[i]);
					}
					isResetMinMax[0] = true;
				}
			} else {
				// 82=VoltageM, 83=CurrentM, 84=CapacityM, 85=PowerM, 86=RevolutionM, 87=TemperatureM 1, 88=TemperatureM 2 89=Voltage_min, 90=Current_max,
				// 91=Revolution_max, 92=Temperature1_max, 93=Temperature2_max 94=Event M
				if (!isResetMinMax[0] && HoTTbinReader2.points[82] != 0) {
					for (int i=82; i<94; ++i) {
						tmpRecordSet.get(i).setMinMax(HoTTbinReader2.points[i], HoTTbinReader2.points[i]);
					}
					isResetMinMax[0] = true;
				}
			}
		}
		migrationJobs.clear();

		HoTTbinReader2.recordSet.addPoints(HoTTbinReader2.points, timeStep_ms);
	}

	public static class RcvBinParser extends BinParser {
		private int							tmpVoltageRx			= 0;
		private int							tmpTemperatureRx	= 0;

		private int							countLostPackages	= 0;

		private PackageLoss			lostPackages			= new PackageLoss();

		protected final byte[]	_buf;

		protected RcvBinParser(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
			throw new UnsupportedOperationException("use in situ parsing");
		}

		protected RcvBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.RECEIVER);
			_buf = buffers[0];
			if (buffers.length != 1) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		/**
		 * parse the buffered data from buffer and add points to record set
		 */
		@Override
		protected boolean parse() {
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			this.tmpVoltageRx = (this._buf[35] & 0xFF);
			this.tmpTemperatureRx = (this._buf[36] & 0xFF);
			this.points[1] = (this._buf[38] & 0xFF) * 1000;
			this.points[3] = DataParser.parse2Short(this._buf, 40) * 1000;
			if (isPointsValid()) {
				this.points[2] = (convertRxDbm2Strength(this._buf[4] & 0xFF)) * 1000;
				this.points[4] = (this._buf[3] & 0xFF) * -1000;
				this.points[5] = (this._buf[4] & 0xFF) * -1000;
				this.points[6] = this.tmpVoltageRx * 1000;
				this.points[7] = this.tmpTemperatureRx * 1000;
				this.points[8] = (this._buf[39] & 0xFF) * 1000;
				if ((this._buf[32] & 0x40) > 0 || (this._buf[32] & 0x25) > 0 && this.tmpTemperatureRx >= 70) // T = 70 - 20 = 50 lowest temperature warning
					this.points[9] = (this._buf[32] & 0x65) * 1000; // warning E,V,T only
				else
					this.points[9] = 0;
				return true;
			}
			if ((this._buf[32] & 0x40) > 0 || (this._buf[32] & 0x25) > 0 && this.tmpTemperatureRx >= 70) // T = 70 - 20 = 50 lowest temperature warning
				this.points[9] = (this._buf[32] & 0x65) * 1000; // warning E,V,T only
			else
				this.points[9] = 0;
			return false;
		}

		/**
		 * @param isAvailable true if the package is not lost
		 */
		public void trackPackageLoss(boolean isAvailable) {
			if (isAvailable) {
				this.pickerParameters.reverseChannelPackageLossCounter.add(1);
				this.points[0] = this.pickerParameters.reverseChannelPackageLossCounter.getPercentage() * 1000;
			} else {
				this.pickerParameters.reverseChannelPackageLossCounter.add(0);
				this.points[0] = this.pickerParameters.reverseChannelPackageLossCounter.getPercentage() * 1000;

				++this.lostPackages.lossTotal; // add up lost packages in telemetry data
				++this.countLostPackages;
				// points[0] = (int) (countPackageLoss*100.0 / ((this.getTimeStep_ms()+10) / 10.0)*1000.0);
			}
		}

		/**
		 * @return true if the lost packages count is transferred into the loss statistics
		 */
		public boolean updateLossStatistics() {
			if (this.countLostPackages > 0) {
				this.lostPackages.add(this.countLostPackages);
				this.countLostPackages = 0;
				return true;
			} else {
				return false;
			}
		}

		/**
		 * @return the lost packages count (is the current number of lost packages since the last valid package)
		 */
		public int getCountPackageLoss() {
			return this.lostPackages.lossTotal;
		}

		public PackageLoss getLostPackages() {
			return this.lostPackages;
		}

		private boolean isPointsValid() {
			// WB !this.pickerParameters.isFilterEnabled || this.tmpVoltageRx > -1 && this.tmpVoltageRx < 100 && this.tmpTemperatureRx < 120; from HottbinReader
			return !this.pickerParameters.isFilterEnabled || this.tmpVoltageRx > -1 && this.tmpVoltageRx < 100 && this.tmpTemperatureRx < 100;
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			for (int j = 0; j < 10; j++) {
				targetPoints[j] = this.points[j];
			}
			throw new UnsupportedOperationException("use in situ parsing");
		}

		@Override
		public String toString() {
			return super.toString() + "  [countPackageLoss=" + this.lostPackages.lossTotal + ", countLostPackages=" + this.countLostPackages + "]";
		}

	}

	public static class VarBinParser extends BinParser {
		private int	tmpHeight		= 0;
		private int	tmpClimb10	= 0;

		protected VarBinParser(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
		}

		protected VarBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.VARIO);
			if (buffers.length != 3) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
			points[2] = 100000;
		}

		@Override
		protected boolean parse() {
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
			this.tmpHeight = DataParser.parse2Short(this._buf1, 2) - 500;
			this.tmpClimb10 = DataParser.parse2UnsignedShort(this._buf2, 2) - 30000;
			if (isPointsValid()) {
				if (this.pickerParameters.altitudeClimbSensorSelection == 1) { //sensor selection GPS (auto, Vario, GPS, GAM, EAM)
					this.points[10] = this.tmpHeight * 1000;
					// pointsVarioMax = DataParser.parse2Short(buf1, 4) * 1000;
					// pointsVarioMin = DataParser.parse2Short(buf1, 6) * 1000;
					this.points[11] = (DataParser.parse2UnsignedShort(this._buf1, 8) - 30000) * 10;
					this.points[12] = (DataParser.parse2UnsignedShort(this._buf2, 0) - 30000) * 10;
					this.points[13] = this.tmpClimb10 * 10;
				}
				this.points[14] = (this._buf1[1] & 0x3F) * 1000; // inverse event
				return true;
			}
			else 
				System.out.println();
			this.points[14] = (this._buf1[1] & 0x3F) * 1000; // inverse event
			return isPointsValid();
		}

		private boolean isPointsValid() {
			return !this.pickerParameters.isFilterEnabled || (this.tmpHeight >= -490 && this.tmpHeight < 5000);
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			if (this.points[10] != 0 || this.points[11] != 0 || this.points[12] != 0 || this.points[13] != 0) {
				for (int j = 10; j < 15; j++) {
					targetPoints[j] = this.points[j];
				}
			}
		}

	}

	public static class GpsBinParser extends BinParser {
		private int			tmpHeight							= 0;
		private int			tmpClimb1							= 0;
		private int			tmpClimb3							= 0;
		private int			tmpVelocity						= 0;
		private int			tmpLatitude						= 0;
		private int			tmpLatitudeDelta			= 0;
		private int			tmpLongitude					= 0;
		private int			tmpLongitudeDelta			= 0;
		private double	latitudeTolerance			= 1;
		private long		lastLatitudeTimeStep	= 0;
		private double	longitudeTolerance		= 1;
		private long		lastLongitudeTimeStep	= 0;

		protected GpsBinParser(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
		}

		protected GpsBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.GPS);
			if (buffers.length != 5) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		@Override
		protected boolean parse() {
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
			// 15=Latitude, 16=Longitude, 17=Velocity, 18=Distance, 19=Direction, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
			// 24=HomeDirection 25=Roll 26=Pitch 27=Yaw 28=GyroX 29=GyroY 30=GyroZ 31=Vibration 32=Version	
			this.tmpHeight = DataParser.parse2Short(this._buf2, 8) - 500;
			this.tmpClimb1 = (DataParser.parse2UnsignedShort(this._buf3, 0) - 30000);
			this.tmpClimb3 = (this._buf3[2] & 0xFF) - 120;
			this.tmpVelocity = DataParser.parse2Short(this._buf1, 4) * 1000;
			if (isPointsValid()) {
				this.points[17] = this.pickerParameters.isFilterEnabled && this.tmpVelocity > 500000 ? this.points[17] : this.tmpVelocity;

				this.tmpLatitude = DataParser.parse2Short(this._buf1, 7) * 10000 + DataParser.parse2Short(this._buf1[9], this._buf2[0]);
				if (!this.pickerParameters.isTolerateSignChangeLatitude) this.tmpLatitude = this._buf1[6] == 1 ? -1 * this.tmpLatitude : this.tmpLatitude;
				this.tmpLatitudeDelta = Math.abs(this.tmpLatitude - this.points[15]);
				this.tmpLatitudeDelta = this.tmpLatitudeDelta > 400000 ? this.tmpLatitudeDelta - 400000 : this.tmpLatitudeDelta;
				this.latitudeTolerance = (this.points[17] / 1000.0) * (this.getTimeStep_ms() - this.lastLatitudeTimeStep) / this.pickerParameters.latitudeToleranceFactor;
				this.latitudeTolerance = this.latitudeTolerance > 0 ? this.latitudeTolerance : 5;

				if (!this.pickerParameters.isFilterEnabled || this.points[15] == 0 || this.tmpLatitudeDelta <= this.latitudeTolerance) {
					this.lastLatitudeTimeStep = this.getTimeStep_ms();
					this.points[15] = this.tmpLatitude;
				} else {
					if (HoTTbinReader2.log.isLoggable(Level.FINE))
						HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("HH:mm:ss:SSS", this.getTimeStep_ms() - GDE.ONE_HOUR_MS) + " Lat " + this.tmpLatitude + " - " + this.tmpLatitudeDelta);
				}

				this.tmpLongitude = DataParser.parse2Short(this._buf2, 2) * 10000 + DataParser.parse2Short(this._buf2, 4);
				if (!this.pickerParameters.isTolerateSignChangeLongitude) this.tmpLongitude = this._buf2[1] == 1 ? -1 * this.tmpLongitude : this.tmpLongitude;
				this.tmpLongitudeDelta = Math.abs(this.tmpLongitude - this.points[16]);
				this.tmpLongitudeDelta = this.tmpLongitudeDelta > 400000 ? this.tmpLongitudeDelta - 400000 : this.tmpLongitudeDelta;
				this.longitudeTolerance = (this.points[17] / 1000.0) * (this.getTimeStep_ms() - this.lastLongitudeTimeStep) / this.pickerParameters.longitudeToleranceFactor;
				this.longitudeTolerance = this.longitudeTolerance > 0 ? this.longitudeTolerance : 5;

				if (!this.pickerParameters.isFilterEnabled || this.points[16] == 0 || this.tmpLongitudeDelta <= this.longitudeTolerance) {
					this.lastLongitudeTimeStep = this.getTimeStep_ms();
					this.points[16] = this.tmpLongitude;
				} else {
					if (HoTTbinReader2.log.isLoggable(Level.FINE))
						HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("HH:mm:ss:SSS", this.getTimeStep_ms() - GDE.ONE_HOUR_MS) + " Long " + this.tmpLongitude + " - " + this.tmpLongitudeDelta);
				}

				if (this.pickerParameters.altitudeClimbSensorSelection == 2) { //sensor selection GPS (auto, Vario, GPS, GAM, EAM)
					this.points[10] = this.tmpHeight * 1000;
					this.points[11] = this.tmpClimb1 * 10;
					this.points[12] = this.tmpClimb3 * 1000;
				}
				this.points[18] = DataParser.parse2Short(this._buf2, 6) * 1000;
				this.points[19] = (this._buf1[3] & 0xFF) * 1000;
				this.points[20] = 0;
				this.points[21] = (this._buf3[3] & 0xFF) * 1000;
				
				switch (_buf3[4]) { //sat-fix
				case '-':
					this.points[22] = 0;
					break;
				case '2':
					this.points[22] = 2000;
					break;
				case '3':
					this.points[22] = 3000;
					break;
				case 'D':
					this.points[22] = 4000;
					break;
				default:
					try {
						this.points[22] = Integer.valueOf(String.format("%c", _buf3[4])) * 1000;
					}
					catch (NumberFormatException e1) {
						this.points[22] = 1000;
					}
					break;
				}
				this.points[23] = (this._buf1[1] & 0x0F) * 1000; // inverse event
				// 24=HomeDirection 25=Roll 26=Pitch 27=Yaw 28=GyroX 29=GyroY 30=GyroZ 31=Vibration 32=Version	
				this.points[24] = (_buf3[5] & 0xFF) * 1000;
				this.points[25] = _buf3[6] * 1000; //(DataParser.parse2Short(_buf3, 6) -500) * 1000; //Sparrow GPS altitude
				this.points[26] = _buf3[7] * 1000; //_buf4[3] * 1000; //Sparrow ENL
				this.points[27] = _buf3[8] * 1000; //_buf4[4] * 1000; //Sparrow servo pulse
				this.points[28] = DataParser.parse2Short(_buf3[9], _buf4[0]) * 1000;
				this.points[29] = DataParser.parse2Short(_buf4, 1) * 1000;
				this.points[30] = DataParser.parse2Short(_buf4, 3) * 1000;
				this.points[31] = (_buf4[5] & 0xFF) * 1000;
				this.points[32] = _buf4[9] * 1000;
				return true;
			}
			this.points[23] = (this._buf1[1] & 0x0F) * 1000; // inverse event
			return false;
		}

		private boolean isPointsValid() {
			return !this.pickerParameters.isFilterEnabled || (this.tmpClimb1 > -20000 && this.tmpClimb3 > -90 && this.tmpHeight >= -490 && this.tmpHeight < 4500);
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			// 10=Altitude, 11=Climb 1, 12=Climb 3
			for (int j = 10; j < 13; j++) {
				targetPoints[j] = this.points[j];
			}
			// 15=Latitude, 16=Longitude, 17=Velocity, 18=Distance, 19=Direction, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
			// 24=HomeDirection 25=Roll 26=Pitch 27=Yaw 28=GyroX 29=GyroY 30=GyroZ 31=Vibration 32=Version	
			for (int k = 15; k < 33; k++) {
				targetPoints[k] = this.points[k];
			}
		}

	}

	public static class GamBinParser extends BinParser {
		private int	tmpHeight		= 0;
		private int	tmpClimb3		= 0;
		private int	tmpVoltage1	= 0;
		private int	tmpVoltage2	= 0;
		private int	tmpCapacity	= 0;

		private int	parseCount	= 0;

		protected GamBinParser(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
		}

		protected GamBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.GAM);
			if (buffers.length != 5) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		@Override
		protected boolean parse() {
			this.tmpHeight = DataParser.parse2Short(this._buf3, 0) - 500;
			this.tmpClimb3 = (this._buf3[4] & 0xFF) - 120;
			this.tmpVoltage1 = DataParser.parse2Short(this._buf1[9], this._buf2[0]);
			this.tmpVoltage2 = DataParser.parse2Short(this._buf2, 1);
			this.tmpCapacity = DataParser.parse2Short(this._buf3[9], this._buf4[0]);
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
			// 15=Latitude, 16=Longitude, 17=Velocity, 18=Distance, 19=Direction, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
			// 24=HomeDirection 25=Roll 26=Pitch 27=Yaw 28=GyroX 29=GyroY 30=GyroZ 31=Vibration 32=Version	
			// 33=Voltage G, 34=Current G, 35=Capacity G, 36=Power G, 37=Balance G, 38=CellVoltage G1, 39=CellVoltage G2 .... 43=CellVoltage G6,
			// 44=Revolution G, 45=FuelLevel, 46=Voltage G1, 47=Voltage G2, 48=Temperature G1, 49=Temperature G2 50=Speed G, 51=LowestCellVoltage,
			// 52=LowestCellNumber, 53=Pressure, 54=Event G
			if (isPointsValid()) {
				int maxVotage = Integer.MIN_VALUE;
				int minVotage = Integer.MAX_VALUE;
				this.points[33] = DataParser.parse2Short(this._buf3, 7) * 1000;
				this.points[34] = DataParser.parse2Short(this._buf3, 5) * 1000;
				if (!this.pickerParameters.isFilterEnabled || this.parseCount <= 20
						|| (this.tmpCapacity != 0 && Math.abs(this.tmpCapacity) <= (this.points[35] / 1000 + this.points[35] / 1000 * this.points[34] / 1000 / 2500 + 2))) {
					this.points[35] = this.tmpCapacity * 1000;
				} else {
					if (HoTTbinReader2.log.isLoggable(Level.FINE))
						HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", this.getTimeStep_ms()) + " - " + this.tmpCapacity + " - " + (this.points[26] / 1000) + " + " + (this.points[24] / 1000 * this.points[25] / 1000 / 2500 + 2));
				}
				this.points[36] = Double.valueOf(this.points[33] / 1000.0 * this.points[34]).intValue();
				// cell voltage
				for (int j = 0; j < 6; j++) {
					this.points[j + 38] = (this._buf1[3 + j] & 0xFF) * 1000;
					if (this.points[j + 38] > 0) {
						maxVotage = this.points[j + 38] > maxVotage ? this.points[j + 38] : maxVotage;
						minVotage = this.points[j + 38] < minVotage ? this.points[j + 38] : minVotage;
					}
				}
				this.points[37] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
				this.points[44] = DataParser.parse2Short(this._buf2, 8) * 1000;
				this.points[45] = DataParser.parse2Short(this._buf2, 6) * 1000;
				if (this.pickerParameters.altitudeClimbSensorSelection == 3) { //sensor selection GPS (auto, Vario, GPS, GAM, EAM)
					this.points[10] = this.tmpHeight * 1000;
					this.points[11] = (DataParser.parse2UnsignedShort(this._buf3, 2) - 30000) * 10;
					this.points[12] = this.tmpClimb3 * 1000;
				}
				this.points[46] = this.tmpVoltage1 * 100;
				this.points[47] = this.tmpVoltage2 * 100;
				this.points[48] = ((this._buf2[3] & 0xFF) - 20) * 1000;
				this.points[49] = ((this._buf2[4] & 0xFF) - 20) * 1000;
				this.points[50] = DataParser.parse2Short(this._buf4, 1) * 1000; // Speed [km/h
				this.points[51] = (this._buf4[3] & 0xFF) * 1000; // lowest cell voltage 124 = 2.48 V
				this.points[52] = (this._buf4[4] & 0xFF) * 1000; // cell number lowest cell voltage
				this.points[53] = (this._buf4[8] & 0xFF) * 1000; // Pressure
				this.points[54] = ((this._buf1[1] & 0xFF) + ((this._buf1[2] & 0x7F) << 8)) * 1000; // inverse event
				++this.parseCount;
				return true;
			}
			this.points[54] = ((this._buf1[1] & 0xFF) + ((this._buf1[2] & 0x7F) << 8)) * 1000; // inverse event
			++this.parseCount;
			return false;
		}

		private boolean isPointsValid() {
			// WB !this.pickerParameters.isFilterEnabled || (this.tmpClimb3 > 30 && this.tmpHeight > 10 && this.tmpHeight < 5000 && Math.abs(this.tmpVoltage1) < 600 && Math.abs(this.tmpVoltage2) < 600); from HottbinReader
			return !this.pickerParameters.isFilterEnabled || (this.tmpClimb3 > -90 && this.tmpHeight >= -490 && this.tmpHeight < 5000 && Math.abs(this.tmpVoltage1) < 600 && Math.abs(this.tmpVoltage2) < 600);
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			if ((targetPoints[10] != 0 && this.points[10] != 0) || this.points[10] != 0) targetPoints[10] = this.points[10];
			// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
			for (int j = 10; j < 13; j++) {
				targetPoints[j] = this.points[j];
			}
			// 33=Voltage G, 34=Current G, 35=Capacity G, 36=Power G, 37=Balance G, 38=CellVoltage G1, 39=CellVoltage G2 .... 43=CellVoltage G6,
			// 44=Revolution G, 45=FuelLevel, 46=Voltage G1, 47=Voltage G2, 48=Temperature G1, 49=Temperature G2 50=Speed G, 51=LowestCellVoltage,
			// 52=LowestCellNumber, 53=Pressure, 54=Event G
			for (int k = 33; k < 55; k++) {
				targetPoints[k] = this.points[k];
			}
		}

	}

	public static class EamBinParser extends BinParser {
		private int	tmpHeight		= 0;
		private int	tmpClimb3		= 0;
		private int	tmpVoltage1	= 0;
		private int	tmpVoltage2	= 0;
		private int	tmpCapacity	= 0;

		private int	parseCount	= 0;

		protected EamBinParser(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
		}

		protected EamBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.EAM);
			if (buffers.length != 5) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		@Override
		protected boolean parse() {
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
			// 15=Latitude, 16=Longitude, 17=Velocity, 18=Distance, 19=Direction, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
			// 24=HomeDirection 25=Roll 26=Pitch 27=Yaw 28=GyroX 29=GyroY 30=GyroZ 31=Vibration 32=Version	
			// 33=Voltage G, 34=Current G, 35=Capacity G, 36=Power G, 37=Balance G, 38=CellVoltage G1, 39=CellVoltage G2 .... 43=CellVoltage G6,
			// 44=Revolution G, 45=FuelLevel, 46=Voltage G1, 47=Voltage G2, 48=Temperature G1, 49=Temperature G2 50=Speed G, 51=LowestCellVoltage,
			// 52=LowestCellNumber, 53=Pressure, 54=Event G
			// 55=Voltage E, 56=Current E, 57=Capacity E, 58=Power E, 59=Balance E, 60=CellVoltage E1, 61=CellVoltage E2 .... 73=CellVoltage E14,
			// 74=Voltage E1, 75=Voltage E2, 76=Temperature E1, 77=Temperature E2 78=Revolution E 79=MotorTime 80=Speed 81=Event E
			this.tmpHeight = DataParser.parse2Short(this._buf3, 3) - 500;
			this.tmpClimb3 = (this._buf4[3] & 0xFF) - 120;
			this.tmpVoltage1 = DataParser.parse2Short(this._buf2, 7);
			this.tmpVoltage2 = DataParser.parse2Short(this._buf2[9], this._buf3[0]);
			this.tmpCapacity = DataParser.parse2Short(this._buf3[9], this._buf4[0]);
			if (isPointsValid()) {
				int maxVotage = Integer.MIN_VALUE;
				int minVotage = Integer.MAX_VALUE;
				this.points[55] = DataParser.parse2Short(this._buf3, 7) * 1000;
				this.points[56] = DataParser.parse2Short(this._buf3, 5) * 1000;
				if (!this.pickerParameters.isFilterEnabled || this.parseCount <= 20
						|| Math.abs(this.tmpCapacity) <= (this.points[57] / 1000 + this.points[54] / 1000 * this.points[56] / 1000 / 2500 + 2)) {
					this.points[57] = this.tmpCapacity * 1000;
				} else {
					if (HoTTbinReader2.log.isLoggable(Level.FINE))
						HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", this.getTimeStep_ms()) + " - " + this.tmpCapacity + " - " + (this.points[48] / 1000) + " + " + (this.points[46] / 1000 * this.points[47] / 1000 / 2500 + 2));
				}
				this.points[58] = Double.valueOf(this.points[55] / 1000.0 * this.points[56]).intValue(); // power U*I [W];
				for (int j = 0; j < 7; j++) {
					this.points[j + 60] = (this._buf1[3 + j] & 0xFF) * 1000;
					if (this.points[j + 60] > 0) {
						maxVotage = this.points[j + 60] > maxVotage ? this.points[j + 60] : maxVotage;
						minVotage = this.points[j + 60] < minVotage ? this.points[j + 60] : minVotage;
					}
				}
				for (int j = 0; j < 7; j++) {
					this.points[j + 67] = (this._buf2[j] & 0xFF) * 1000;
					if (this.points[j + 67] > 0) {
						maxVotage = this.points[j + 67] > maxVotage ? this.points[j + 67] : maxVotage;
						minVotage = this.points[j + 67] < minVotage ? this.points[j + 67] : minVotage;
					}
				}
				// calculate balance on the fly
				this.points[59] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
				if (this.pickerParameters.altitudeClimbSensorSelection == 4) { //sensor selection GPS (auto, Vario, GPS, GAM, EAM)
					this.points[10] = this.tmpHeight * 1000;
					this.points[11] = (DataParser.parse2UnsignedShort(this._buf4, 1) - 30000) * 10;
					this.points[12] = this.tmpClimb3 * 1000;
				}
				this.points[74] = this.tmpVoltage1 * 100;
				this.points[75] = this.tmpVoltage2 * 100;
				this.points[76] = ((this._buf3[1] & 0xFF) - 20) * 1000;
				this.points[77] = ((this._buf3[2] & 0xFF) - 20) * 1000;
				this.points[78] = DataParser.parse2Short(this._buf4, 4) * 1000;
				this.points[79] = ((this._buf4[6] & 0xFF) * 60 + (this._buf4[7] & 0xFF)) * 1000; // motor time
				this.points[80] = DataParser.parse2Short(this._buf4, 8) * 1000; // speed
				this.points[81] = ((this._buf1[1] & 0xFF) + ((this._buf1[2] & 0x7F) << 8)) * 1000; // inverse event
				++this.parseCount;
				return true;
			}
			this.points[81] = ((this._buf1[1] & 0xFF) + ((this._buf1[2] & 0x7F) << 8)) * 1000; // inverse event
			return false;
		}

		private boolean isPointsValid() {
			// WB !this.pickerParameters.isFilterEnabled || (this.tmpClimb3 > 30 && this.tmpHeight > 10 && this.tmpHeight < 5000 && Math.abs(this.tmpVoltage1) < 600 && Math.abs(this.tmpVoltage2) < 600); from HottbinReader
			return !this.pickerParameters.isFilterEnabled || (this.tmpClimb3 > -90 && this.tmpHeight >= -490 && this.tmpHeight < 5000 && Math.abs(this.tmpVoltage1) < 600 && Math.abs(this.tmpVoltage2) < 600);
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			if ((targetPoints[10] != 0 && this.points[10] != 0) || this.points[10] != 0) targetPoints[10] = this.points[10];
			// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario
			for (int j = 10; j < 13; j++) {
				targetPoints[j] = this.points[j];
			}
			// 55=Voltage E, 56=Current E, 57=Capacity E, 58=Power E, 59=Balance E, 60=CellVoltage E1, 61=CellVoltage E2 .... 73=CellVoltage E14,
			// 74=Voltage E1, 75=Voltage E2, 76=Temperature E1, 77=Temperature E2 78=Revolution E 79=MotorTime 80=Speed 81=Event E
			for (int k = 55; k < 82; k++) {
				targetPoints[k] = this.points[k];
			}
		}

	}

	public static class ChnBinParser extends BinParser {
		protected final byte[] _buf;

		protected ChnBinParser(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
			throw new UnsupportedOperationException("use in situ parsing");
		}

		protected ChnBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.CHANNEL);
			_buf = buffers[0];
			if (buffers.length != 1) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
		}

		@Override
		protected boolean parse() {
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			// 82=Ch 1, 83=Ch 2, 84=Ch 3 .. 97=Ch 16, 98=PowerOff, 99=BatterieLow, 100=Reset, 101=reserve
			this.points[4] = (this._buf[3] & 0xFF) * -1000;
			this.points[5] = (this._buf[4] & 0xFF) * -1000;

			this.points[82] = (DataParser.parse2UnsignedShort(this._buf, 8) / 2) * 1000;
			this.points[83] = (DataParser.parse2UnsignedShort(this._buf, 10) / 2) * 1000;
			this.points[84] = (DataParser.parse2UnsignedShort(this._buf, 12) / 2) * 1000;
			this.points[85] = (DataParser.parse2UnsignedShort(this._buf, 14) / 2) * 1000;
			this.points[86] = (DataParser.parse2UnsignedShort(this._buf, 16) / 2) * 1000;
			this.points[87] = (DataParser.parse2UnsignedShort(this._buf, 18) / 2) * 1000;
			this.points[88] = (DataParser.parse2UnsignedShort(this._buf, 20) / 2) * 1000;
			this.points[89] = (DataParser.parse2UnsignedShort(this._buf, 22) / 2) * 1000;
			// events
			this.points[98] = (this._buf[50] & 0x01) * 100000;
			this.points[99] = (this._buf[50] & 0x02) * 50000;
			this.points[100] = (this._buf[50] & 0x04) * 25000;
			if (this._buf[32] > 0 && this._buf[32] < 27)
				this.points[101] = this._buf[32] * 1000; // warning
			else
				this.points[101] = 0;

			if (this._buf[5] == 0x00) { // channel 9-12
				this.points[90] = (DataParser.parse2UnsignedShort(this._buf, 24) / 2) * 1000;
				this.points[91] = (DataParser.parse2UnsignedShort(this._buf, 26) / 2) * 1000;
				this.points[92] = (DataParser.parse2UnsignedShort(this._buf, 28) / 2) * 1000;
				this.points[93] = (DataParser.parse2UnsignedShort(this._buf, 30) / 2) * 1000;
				if (this.points[94] == 0) {
					this.points[94] = 1500 * 1000;
					this.points[95] = 1500 * 1000;
					this.points[96] = 1500 * 1000;
					this.points[97] = 1500 * 1000;
				}
			} else { // channel 13-16
				this.points[94] = (DataParser.parse2UnsignedShort(this._buf, 24) / 2) * 1000;
				this.points[95] = (DataParser.parse2UnsignedShort(this._buf, 26) / 2) * 1000;
				this.points[96] = (DataParser.parse2UnsignedShort(this._buf, 28) / 2) * 1000;
				this.points[97] = (DataParser.parse2UnsignedShort(this._buf, 30) / 2) * 1000;
				if (this.points[90] == 0) {
					this.points[90] = 1500 * 1000;
					this.points[91] = 1500 * 1000;
					this.points[92] = 1500 * 1000;
					this.points[93] = 1500 * 1000;
				}
			}
			return true;
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			for (int j = 4; j < 7; j++) {
				targetPoints[j] = this.points[j];
			}
			// 82=Ch 1, 83=Ch 2, 84=Ch 3 .. 97=Ch 16, 98=PowerOff, 99=BatterieLow, 100=Reset, 101=reserve
			for (int j = 82; j < 102; j++) {
				targetPoints[j] = this.points[j];
			}
			throw new UnsupportedOperationException("use in situ parsing");
		}

	}

	public static class EscBinParser extends BinParser {
		private final boolean	isChannelsChannel;

		private int						tmpTemperatureFet	= 0;
		private int						tmpVoltage				= 0;
		private int						tmpCurrent				= 0;
		private int						tmpRevolution			= 0;
		private int						tmpCapacity				= 0;

		private int						parseCount				= 0;
		
		protected boolean isChannelsChannel() { return this.isChannelsChannel; }

		protected EscBinParser(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
			this(pickerParameters,
					new int[pickerParameters.analyzer.getActiveDevice().getNumberOfMeasurements(pickerParameters.analyzer.getActiveChannel().getNumber())], //
					timeSteps_ms, buffers);
		}

		protected EscBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
			super(pickerParameters, points, timeSteps_ms, buffers, Sensor.ESC);
			if (buffers.length != 4) throw new InvalidParameterException("buffers mismatch: " + buffers.length);
			this.isChannelsChannel = this.pickerParameters.analyzer.getActiveChannel().getNumber() == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER;
		}

		@Override
		protected boolean parse() {
			this.tmpVoltage = DataParser.parse2Short(this._buf1, 3);
			this.tmpCurrent = DataParser.parse2Short(this._buf2, 1);
			this.tmpCapacity = DataParser.parse2Short(this._buf1, 7);
			this.tmpRevolution = DataParser.parse2Short(this._buf2, 5);
			this.tmpTemperatureFet = this._buf1[9] - 20;
			if (this.isChannelsChannel) {
				// 102=VoltageM, 103=CurrentM, 104=CapacityM, 105=PowerM, 106=RevolutionM, 107=TemperatureM 1, 108=TemperatureM 2 109=Voltage_min, 110=Current_max,
				// 111=Revolution_max, 112=Temperature1_max, 113=Temperature2_max 114=Event M
				if (isPointsValid()) {
					this.points[102] = this.tmpVoltage * 1000;
					this.points[103] = this.tmpCurrent * 1000;
					this.points[105] = Double.valueOf(this.points[102] / 1000.0 * this.points[103]).intValue();
					if (!this.pickerParameters.isFilterEnabled || this.parseCount <= 20
							|| (this.tmpCapacity != 0 && Math.abs(this.tmpCapacity) <= (this.points[104] / 1000 + this.tmpVoltage * this.tmpCurrent / 2500 + 2))) {
						this.points[104] = this.tmpCapacity * 1000;
					} else {
						if (this.tmpCapacity != 0 && HoTTbinReader2.log.isLoggable(Level.FINE))
							HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", this.getTimeStep_ms()) + " - " + this.tmpCapacity + " - " + (this.points[104] / 1000) + " + " + (this.tmpVoltage * this.tmpCurrent / 2500 + 2));
					}
					this.points[106] = this.tmpRevolution * 1000;
					this.points[107] = this.tmpTemperatureFet * 1000;

					this.points[108] = (this._buf2[9] - 20) * 1000;
					this.points[109] = DataParser.parse2Short(this._buf1, 5) * 1000;
					this.points[110] = DataParser.parse2Short(this._buf2, 3) * 1000;
					this.points[111] = DataParser.parse2Short(this._buf2, 7) * 1000;
					this.points[112] = (this._buf2[0] - 20) * 1000;
					this.points[113] = (this._buf3[0] - 20) * 1000;
					this.points[114] = (this._buf1[1] & 0xFF) * 1000; // inverse event
					return true;
				}
				this.points[114] = (this._buf1[1] & 0xFF) * 1000; // inverse event
				return false;
			} else {
				// 82=VoltageM, 83=CurrentM, 84=CapacityM, 85=PowerM, 86=RevolutionM, 87=TemperatureM 1, 88=TemperatureM 2 89=Voltage_min, 90=Current_max,
				// 91=Revolution_max, 92=Temperature1_max, 93=Temperature2_max 94=Event M
				if (isPointsValid()) {
					this.points[82] = this.tmpVoltage * 1000;
					this.points[83] = this.tmpCurrent * 1000;
					this.points[85] = Double.valueOf(this.points[82] / 1000.0 * this.points[83]).intValue();
					if (!this.pickerParameters.isFilterEnabled || this.parseCount <= 20
							|| (this.tmpCapacity != 0 && Math.abs(this.tmpCapacity) <= (this.points[84] / 1000 + this.tmpVoltage * this.tmpCurrent / 2500 + 2))) {
						this.points[84] = this.tmpCapacity * 1000;
					} else {
						if (this.tmpCapacity != 0 && HoTTbinReader2.log.isLoggable(Level.FINE))
							HoTTbinReader2.log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", this.getTimeStep_ms()) + " - " + this.tmpCapacity + " - " + (this.points[84] / 1000) + " + " + (this.tmpVoltage * this.tmpCurrent / 2500 + 2));
					}
					this.points[86] = this.tmpRevolution * 1000;
					this.points[87] = this.tmpTemperatureFet * 1000;

					this.points[88] = (this._buf2[9] - 20) * 1000;
					this.points[89] = DataParser.parse2Short(this._buf1, 5) * 1000;
					this.points[90] = DataParser.parse2Short(this._buf2, 3) * 1000;
					this.points[91] = DataParser.parse2Short(this._buf2, 7) * 1000;
					this.points[92] = (this._buf2[0] - 20) * 1000;
					this.points[93] = (this._buf3[0] - 20) * 1000;
					this.points[94] = (this._buf1[1] & 0xFF) * 1000; // inverse event
					++this.parseCount;
					return true;
				}
				this.points[94] = (this._buf1[1] & 0xFF) * 1000; // inverse event
				++this.parseCount;
				return false;
			}
		}

		private boolean isPointsValid() {
			if (this.isChannelsChannel) {
				return !this.pickerParameters.isFilterEnabled
						|| this.tmpVoltage > 0 && this.tmpVoltage < 1000 && this.tmpCurrent < 4000 && this.tmpCurrent > -10 && this.tmpRevolution > -1
						&& this.tmpRevolution < 20000 && !(this.points[98] != 0 && this.points[98] / 1000 - this.tmpTemperatureFet > 20);
			} else {
				return !this.pickerParameters.isFilterEnabled
						|| this.tmpVoltage > 0 && this.tmpVoltage < 1000 && this.tmpCurrent < 4000 && this.tmpCurrent > -10 && this.tmpRevolution > -1
						&& this.tmpRevolution < 20000 && !(this.points[78] != 0 && this.points[78] / 1000 - this.tmpTemperatureFet > 20);
			}
		}

		@Override
		public void migratePoints(int[] targetPoints) {
			if (this.isChannelsChannel) {
				// 102=VoltageM, 103=CurrentM, 104=CapacityM, 105=PowerM, 106=RevolutionM, 107=TemperatureM 1, 108=TemperatureM 2 109=Voltage_min, 110=Current_max,
				// 111=Revolution_max, 112=Temperature1_max, 113=Temperature2_max 114=Event M
				for (int j = 102; j < targetPoints.length; j++) {
					targetPoints[j] = this.points[j];
				}
			} else {
				// 82=VoltageM, 83=CurrentM, 84=CapacityM, 85=PowerM, 86=RevolutionM, 87=TemperatureM 1, 88=TemperatureM 2 89=Voltage_min, 90=Current_max,
				// 91=Revolution_max, 92=Temperature1_max, 93=Temperature2_max 94=Event M
				for (int j = 82; j < targetPoints.length; j++) {
					targetPoints[j] = this.points[j];
				}
			}
		}
	}

}
