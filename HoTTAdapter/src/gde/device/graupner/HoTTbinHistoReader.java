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
    
    Copyright (c) 2011,2012,2013,2014,2015,2016 Winfried Bruegmann
    					2016 Thomas Eickert
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.data.Channel;
import gde.data.HistoRecordSet;
import gde.data.RecordSet;
import gde.device.HistoRandomSample;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.StringHelper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * read Graupner HoTT binary data for history analysis.
 * provide data in a histo recordset and in live recordset.
 * @author Thomas Eickert
 */
public class HoTTbinHistoReader extends HoTTbinReader {
	final private static String	$CLASS_NAME	= HoTTbinHistoReader.class.getName();
	final private static Logger	log			= Logger.getLogger(HoTTbinHistoReader.$CLASS_NAME);

	private static long				currentTime, readTime, reviewTime, addTime, pickTime, lastTime;

	@Deprecated // shadows the base class method
	public static synchronized void read(String filePath) throws Exception {
	}

	/**
	 * read file.
	 * provide data in a histo recordset and in live recordset.
	 * @param recordSet is filled from the file
	 * @param filePath
	 * @throws Exception 
	 */
	public static synchronized void read(RecordSet tmpRecordSet, String filePath) throws IOException, DataTypeException, DataInconsitsentException {
		final String $METHOD_NAME = "read";
		HashMap<String, String> header = null;
		File file = new File(filePath);
		// DataInputStream data_in = new DataInputStream(file_input); // accesses the file io at every read operation
		BufferedInputStream data_in = new BufferedInputStream(new FileInputStream(file)); // performs better than DataInputStream due to reasonable read io sizes
		data_in.mark((LOG_RECORD_SCAN_START + NUMBER_LOG_RECORDS_TO_SCAN) * dataBlockSize + 1);

		HoTTAdapter device = (HoTTAdapter) HoTTbinHistoReader.application.getActiveDevice();
		Channel activeChannel = HoTTbinHistoReader.application.getActiveChannel();

		long startTimeStamp_ms = file.lastModified() - (file.length() / HoTTbinHistoReader.dataBlockSize * 10);
		// 2409 String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$

		// 2409 RecordSet tmpRecordSet;
		// 2409 HoTTAdapter.recordSets.clear();
		// add a recordset for the active channel
		String recordSetNameExtend = getRecordSetExtend(file);
		String recordSetName = activeChannel.getNumber() + GDE.STRING_RIGHT_PARENTHESIS_BLANK + activeChannel.getName() + recordSetNameExtend;
		// 2409 tmpRecordSet = RecordSet.createRecordSet(recordSetName, device, HoTTbinHistoReader.application.getActiveChannelNumber(), true, true);
		// 2409 HoTTAdapter.recordSets.put(activeChannel.getName(), tmpRecordSet);
		tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
		tmpRecordSet.descriptionAppendFilename(file.getName());
		tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
		activeChannel.applyTemplate(recordSetName, false);
		if (HoTTbinHistoReader.log.isLoggable(Level.FINE))
			HoTTbinHistoReader.log.logp(Level.FINE, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, " recordSetName=" + recordSetName);

		try {
			header = HoTTbinHistoReader.getFileInfo(data_in, file);
			if (header.size() > 0) {
				data_in.reset();
				if (Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) <= 1) {
					HoTTbinHistoReader.isReceiverOnly = Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) == 0;
					data_in.mark((LOG_RECORD_SCAN_START + NUMBER_LOG_RECORDS_TO_SCAN) * dataBlockSize + 1); // reduces # of overscan records from 57 to 23 (to 38 with 1500 blocks)
					HoTTbinHistoReader.readSingle(tmpRecordSet, data_in, file.length(), LOG_RECORD_SCAN_START + NUMBER_LOG_RECORDS_TO_SCAN);
					data_in.reset();
					HoTTbinHistoReader.readSingle(tmpRecordSet, data_in, file.length(), -1);
				} else {
					data_in.mark((LOG_RECORD_SCAN_START + NUMBER_LOG_RECORDS_TO_SCAN) * dataBlockSize + 1);
					HoTTbinHistoReader.readMultiple(tmpRecordSet, data_in, file.length(), LOG_RECORD_SCAN_START + NUMBER_LOG_RECORDS_TO_SCAN);
					data_in.reset();
					HoTTbinHistoReader.readMultiple(tmpRecordSet, data_in, file.length(), -1);
				}
			}
		} finally {
			data_in.close();
			data_in = null;
		}
	}

	/**
	 * get data file info data.
	 * no message boxes.
	 * leaves stream open for further use.
	 * @param file
	 * @return hash map containing header data as string accessible by public header keys. is empty in case of errors. 
	 * @throws IOException 
	 * @throws DataTypeException 
	 */
	private static HashMap<String, String> getFileInfo(BufferedInputStream data_in, File file) throws IOException, DataTypeException {
		final String $METHOD_NAME = "getFileInfo";
		byte[] buffer = new byte[dataBlockSize];
		HashMap<String, String> fileInfo;
		long numberLogs = (file.length() / dataBlockSize);
		int sensorCount = 0;
		HoTTbinHistoReader.sensorSignature = new StringBuilder().append(GDE.STRING_LEFT_BRACKET).append(HoTTAdapter.Sensor.RECEIVER.name()).append(GDE.STRING_COMMA);

		fileInfo = new HashMap<String, String>();
		if (numberLogs < NUMBER_LOG_RECORDS_MIN) {
			if (HoTTbinHistoReader.log.isLoggable(Level.INFO))
				HoTTbinHistoReader.log.logp(Level.INFO, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2407) + " @ "
						+ file.getAbsolutePath());
			fileInfo.clear();
			return fileInfo;
		}

		for (int i = 0; i < HoTTAdapter.isSensorType.length; i++) {
			HoTTAdapter.isSensorType[i] = false;
		}
		if (HoTTbinHistoReader.log.isLoggable(Level.FINER))
			HoTTbinHistoReader.log.logp(Level.FINER, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(buffer.length));
		long position = (file.length() / 2) - ((NUMBER_LOG_RECORDS_TO_SCAN * dataBlockSize) / 2);
		position = position - position % dataBlockSize;
		if (position <= 0) {
			sensorCount = 1;
		} else {
			if (position > dataBlockSize * LOG_RECORD_SCAN_START) {
				position = dataBlockSize * LOG_RECORD_SCAN_START;
			}
			data_in.read(buffer);
			if (new String(buffer).startsWith("GRAUPNER SD LOG8")) {
				if (HoTTbinHistoReader.log.isLoggable(Level.INFO))
					HoTTbinHistoReader.log.logp(Level.INFO, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2410) + " @ "
							+ file.getAbsolutePath());
				fileInfo.clear();
				return fileInfo;
			}
			{
				long toSkip = position - dataBlockSize;
				do { // The skip method may, for a variety of reasons, end up skipping over some smaller number of bytes, possibly 0. The actual number of bytes skipped is returned.
					toSkip -= data_in.skip(toSkip);
				} while (toSkip > 0);
			}
			for (int i = 0; i < NUMBER_LOG_RECORDS_TO_SCAN; i++) {
				data_in.read(buffer);
				if (HoTTbinHistoReader.log.isLoggable(Level.FINER))
					HoTTbinHistoReader.log.logp(Level.FINER, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(buffer, buffer.length));

				switch (buffer[7]) {
				case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
					if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.VARIO.ordinal()] == false) {
						HoTTbinHistoReader.sensorSignature.append(HoTTAdapter.Sensor.VARIO.name()).append(GDE.STRING_COMMA);
					}
					HoTTAdapter.isSensorType[HoTTAdapter.Sensor.VARIO.ordinal()] = true;
					break;
				case HoTTAdapter.SENSOR_TYPE_GPS_19200:
					if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GPS.ordinal()] == false) {
						HoTTbinHistoReader.sensorSignature.append(HoTTAdapter.Sensor.GPS.name()).append(GDE.STRING_COMMA);
					}
					HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GPS.ordinal()] = true;
					break;
				case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
					if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()] == false) {
						HoTTbinHistoReader.sensorSignature.append(HoTTAdapter.Sensor.GAM.name()).append(GDE.STRING_COMMA);
					}
					HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()] = true;
					break;
				case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
					if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.EAM.ordinal()] == false) {
						HoTTbinHistoReader.sensorSignature.append(HoTTAdapter.Sensor.EAM.name()).append(GDE.STRING_COMMA);
					}
					HoTTAdapter.isSensorType[HoTTAdapter.Sensor.EAM.ordinal()] = true;
					break;
				case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
					if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()] == false) {
						HoTTbinHistoReader.sensorSignature.append(HoTTAdapter.Sensor.ESC.name()).append(GDE.STRING_COMMA);
					}
					HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()] = true;
					break;
				}
			}
			for (boolean element : HoTTAdapter.isSensorType) {
				if (element == true)
					++sensorCount;
			}
		}
		HoTTbinHistoReader.sensorSignature.deleteCharAt(HoTTbinHistoReader.sensorSignature.length() - 1).append(GDE.STRING_RIGHT_BRACKET);
		fileInfo.put(HoTTAdapter.SENSOR_COUNT, GDE.STRING_EMPTY + sensorCount);
		fileInfo.put(HoTTAdapter.LOG_COUNT, GDE.STRING_EMPTY + (file.length() / dataBlockSize));

		if (HoTTbinHistoReader.log.isLoggable(Level.FINE))
			for (Entry<String, String> entry : fileInfo.entrySet()) {
				HoTTbinHistoReader.log.logp(Level.FINE, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, entry.getKey() + " = " + entry.getValue());
				HoTTbinHistoReader.log.log(Level.FINE, file.getName() + " - " + "sensor count = " + sensorCount);
			}

		return fileInfo;
	}

	@Deprecated // shadows the base class method
	static void readSingle(File file) throws IOException, DataInconsitsentException {
	}

	/**
	* read log data according to version 0.
	* allocates only one single recordset for the active channel, so HoTTAdapter.isChannelsChannelEnabled does not take effect.
	* no progress bar support and no channel data modifications.
	* @param file
	* @param initializeBlocks if this number is greater than zero, the min/max values are initialized
	* @throws IOException 
	* @throws DataInconsitsentException 
	*/
	private static void readSingle(RecordSet tmpRecordSet, BufferedInputStream data_in, long bytesCount, int initializeBlocks) throws IOException, DataInconsitsentException {
		final String $METHOD_NAME = "readSingle";
		long startTime = System.nanoTime() / 1000000;
		HoTTbinHistoReader.readTime = HoTTbinHistoReader.addTime = HoTTbinHistoReader.reviewTime = HoTTbinHistoReader.pickTime = 0;
		HoTTbinHistoReader.lastTime = System.nanoTime();
		HoTTbinHistoReader.recordSetReceiver = null; // 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=UminRx
		HoTTbinHistoReader.recordSetGeneral = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		HoTTbinHistoReader.recordSetElectric = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2, 27=Revolution
		HoTTbinHistoReader.recordSetVario = null; // 0=RXSQ, 1=Height, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
		HoTTbinHistoReader.recordSetGPS = null; // 0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		HoTTbinHistoReader.recordSetChannel = null; // 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16
		HoTTbinHistoReader.recordSetSpeedControl = null; // 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature
		HoTTbinHistoReader.pointsReceiver = new int[9];
		HoTTbinHistoReader.pointsGeneral = new int[21];
		HoTTbinHistoReader.pointsElectric = new int[28];
		HoTTbinHistoReader.pointsVario = new int[7];
		HoTTbinHistoReader.pointsVario[2] = 100000;
		HoTTbinHistoReader.pointsGPS = new int[12];
		HoTTbinHistoReader.pointsChannel = new int[23];
		HoTTbinHistoReader.pointsSpeedControl = new int[13];
		HoTTbinHistoReader.timeStep_ms = 0;
		HoTTbinHistoReader.buf = new byte[HoTTbinHistoReader.dataBlockSize];
		HoTTbinHistoReader.buf0 = new byte[30];
		HoTTbinHistoReader.buf1 = null;
		HoTTbinHistoReader.buf2 = null;
		HoTTbinHistoReader.buf3 = null;
		HoTTbinHistoReader.buf4 = null;
		HoTTAdapter.reverseChannelPackageLossCounter.clear();
		HoTTbinHistoReader.lostPackages.clear();
		HoTTbinHistoReader.countLostPackages = 0;
		HoTTbinHistoReader.isJustParsed = false;
		int countPackageLoss = 0;
		HistoRandomSample histoRandomSample = null;
		HoTTAdapter device = (HoTTAdapter) HoTTbinHistoReader.application.getActiveDevice();
		int activeChannelNumber = HoTTbinHistoReader.application.getActiveChannel().getNumber();
		boolean doFullRead = initializeBlocks <= 0;
		if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
			HoTTbinHistoReader.recordSetReceiver = tmpRecordSet;
			histoRandomSample = new HistoRandomSample(HoTTbinHistoReader.pointsReceiver.length, doFullRead);
		} else if (activeChannelNumber == HoTTAdapter.Sensor.CHANNEL.ordinal() + 1) {
			HoTTbinHistoReader.recordSetChannel = tmpRecordSet;
			histoRandomSample = new HistoRandomSample(HoTTbinHistoReader.pointsChannel.length, doFullRead);
		} else if (activeChannelNumber == HoTTAdapter.Sensor.VARIO.ordinal() + 1) {
			HoTTbinHistoReader.recordSetVario = tmpRecordSet;
			histoRandomSample = new HistoRandomSample(HoTTbinHistoReader.pointsVario.length, doFullRead);
		} else if (activeChannelNumber == HoTTAdapter.Sensor.GPS.ordinal() + 1) {
			HoTTbinHistoReader.recordSetGPS = tmpRecordSet;
			histoRandomSample = new HistoRandomSample(HoTTbinHistoReader.pointsGPS.length, doFullRead);
		} else if (activeChannelNumber == HoTTAdapter.Sensor.GAM.ordinal() + 1) {
			HoTTbinHistoReader.recordSetGeneral = tmpRecordSet;
			histoRandomSample = new HistoRandomSample(HoTTbinHistoReader.pointsGeneral.length, doFullRead);
		} else if (activeChannelNumber == HoTTAdapter.Sensor.EAM.ordinal() + 1) {
			HoTTbinHistoReader.recordSetElectric = tmpRecordSet;
			histoRandomSample = new HistoRandomSample(HoTTbinHistoReader.pointsElectric.length, doFullRead);
		} else if (activeChannelNumber == HoTTAdapter.Sensor.ESC.ordinal() + 1) {
			HoTTbinHistoReader.recordSetSpeedControl = tmpRecordSet;
			histoRandomSample = new HistoRandomSample(HoTTbinHistoReader.pointsSpeedControl.length, doFullRead);
		}
		// read all the data blocks from the file, parse only for the active channel
		int initializeBlockLimit = initializeBlocks > 0 ? initializeBlocks : Integer.MAX_VALUE;
		boolean isChannelsChannel = activeChannelNumber == HoTTAdapter.Sensor.CHANNEL.ordinal() + 1; // instead of HoTTAdapter setting
		long numberDatablocks = bytesCount / HoTTbinHistoReader.dataBlockSize / (HoTTbinHistoReader.isReceiverOnly && !isChannelsChannel ? 10 : 1);
		for (int i = 0; i < initializeBlockLimit && i < numberDatablocks; i++) {
			// if (log.isLoggable(Level.TIME))
			// log.log(Level.TIME, String.format("markpos: %,9d i: %,9d ", data_in.markpos, i));
			data_in.read(HoTTbinHistoReader.buf);
			if (HoTTbinHistoReader.log.isLoggable(Level.FINE) && i % 10 == 0) {
				HoTTbinHistoReader.log.logp(Level.FINE, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinHistoReader.buf.length));
				HoTTbinHistoReader.log.logp(Level.FINE, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinHistoReader.buf, HoTTbinHistoReader.buf.length));
			}

			if (!HoTTAdapter.isFilterTextModus || (HoTTbinHistoReader.buf[6] & 0x01) == 0) { // switch into text modus
				if (HoTTbinHistoReader.buf[33] >= 0 && HoTTbinHistoReader.buf[33] <= 4 && HoTTbinHistoReader.buf[3] != 0 && HoTTbinHistoReader.buf[4] != 0) { // buf 3, 4, tx,rx
					if (HoTTbinHistoReader.log.isLoggable(Level.FINE))
						HoTTbinHistoReader.log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", HoTTbinHistoReader.buf[7], HoTTbinHistoReader.buf[33]));
					if (HoTTbinHistoReader.log.isLoggable(Level.FINER))
						HoTTbinHistoReader.log.logp(Level.FINER, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(new byte[] {
								HoTTbinHistoReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinHistoReader.buf[7], false));
					if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
						HoTTAdapter.reverseChannelPackageLossCounter.add(1);
						HoTTbinHistoReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
						if (HoTTbinHistoReader.buf[33] == 0 && (HoTTbinHistoReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinHistoReader.buf, 40) >= 0) {
							parse4Receiver(HoTTbinHistoReader.buf);
							setTimeMarks("isRead");
							boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsReceiver, HoTTbinHistoReader.timeStep_ms);
							setTimeMarks("isReviewed");
							if (isValidSample && doFullRead) {
								HoTTbinHistoReader.recordSetReceiver.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
								setTimeMarks("isAdded");
								setTimeMarks("isPicked");
							}
						}
					} else if (activeChannelNumber == HoTTAdapter.Sensor.CHANNEL.ordinal() + 1) {
						parse4Channel(HoTTbinHistoReader.buf);
						setTimeMarks("isRead");
						boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsChannel, HoTTbinHistoReader.timeStep_ms);
						setTimeMarks("isReviewed");
						if (isValidSample && doFullRead) {
							HoTTbinHistoReader.recordSetChannel.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks("isAdded");
							setTimeMarks("isPicked");
						}
					}
					if (HoTTbinHistoReader.isReceiverOnly && !isChannelsChannel) {
						for (int j = 0; j < 9; j++) {
							data_in.read(HoTTbinHistoReader.buf);
							HoTTbinHistoReader.timeStep_ms += 10;
						}
					}
					// fill data block 0 receiver voltage and temperature
					if (HoTTbinHistoReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinHistoReader.buf, 0) != 0) {
						System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf0, 0, HoTTbinHistoReader.buf0.length);
					}

					HoTTbinHistoReader.timeStep_ms += 10;// add default time step from device of 10 msec

					// log.log(Level.OFF, "sensor type ID = " + StringHelper.byte2Hex2CharString(new byte[] {(byte) (HoTTbinHistoReader.buf[7] & 0xFF)}, 1));
					switch ((byte) (HoTTbinHistoReader.buf[7] & 0xFF)) {
					case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
					case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
						if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.VARIO.ordinal()]) {
							// fill data block 1 to 2
							if (HoTTbinHistoReader.buf[33] == 1) {
								HoTTbinHistoReader.buf1 = new byte[30];
								System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf1, 0, HoTTbinHistoReader.buf1.length);
							}
							if (HoTTbinHistoReader.buf[33] == 2) {
								HoTTbinHistoReader.buf2 = new byte[30];
								System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf2, 0, HoTTbinHistoReader.buf2.length);
							}
							if (HoTTbinHistoReader.buf1 != null && HoTTbinHistoReader.buf2 != null) {
								if (activeChannelNumber == HoTTAdapter.Sensor.VARIO.ordinal() + 1) {
									final int versionOBS = -99;
									HoTTbinHistoReader.parse4Vario(versionOBS, HoTTbinHistoReader.buf0, HoTTbinHistoReader.buf1, HoTTbinHistoReader.buf2);
									setTimeMarks("isRead");
									boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsVario, HoTTbinHistoReader.timeStep_ms);
									setTimeMarks("isReviewed");
									if (isValidSample && doFullRead) {
										HoTTbinHistoReader.recordSetVario.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
										setTimeMarks("isAdded");
										setTimeMarks("isPicked");
									}
								}
								HoTTbinHistoReader.isJustParsed = true;
								HoTTbinHistoReader.buf1 = HoTTbinHistoReader.buf2 = null;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_GPS_115200:
					case HoTTAdapter.SENSOR_TYPE_GPS_19200:
						if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GPS.ordinal()]) {
							// fill data block 1 to 3
							if (HoTTbinHistoReader.buf1 == null && HoTTbinHistoReader.buf[33] == 1) {
								HoTTbinHistoReader.buf1 = new byte[30];
								System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf1, 0, HoTTbinHistoReader.buf1.length);
							}
							if (HoTTbinHistoReader.buf2 == null && HoTTbinHistoReader.buf[33] == 2) {
								HoTTbinHistoReader.buf2 = new byte[30];
								System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf2, 0, HoTTbinHistoReader.buf2.length);
							}
							if (HoTTbinHistoReader.buf3 == null && HoTTbinHistoReader.buf[33] == 3) {
								HoTTbinHistoReader.buf3 = new byte[30];
								System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf3, 0, HoTTbinHistoReader.buf3.length);
							}
							if (HoTTbinHistoReader.buf1 != null && HoTTbinHistoReader.buf2 != null && HoTTbinHistoReader.buf3 != null) {
								if (activeChannelNumber == HoTTAdapter.Sensor.GPS.ordinal() + 1) {
									parse4GPS(HoTTbinHistoReader.buf0, HoTTbinHistoReader.buf1, HoTTbinHistoReader.buf2, HoTTbinHistoReader.buf3);
									setTimeMarks("isRead");
									boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsGPS, HoTTbinHistoReader.timeStep_ms);
									setTimeMarks("isReviewed");
									if (isValidSample && doFullRead) {
										HoTTbinHistoReader.recordSetGPS.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
										setTimeMarks("isAdded");
										setTimeMarks("isPicked");
									}
								}
								HoTTbinHistoReader.isJustParsed = true;
								HoTTbinHistoReader.buf1 = HoTTbinHistoReader.buf2 = HoTTbinHistoReader.buf3 = null;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
					case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
						if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()]) {
							// fill data block 1 to 4
							if (HoTTbinHistoReader.buf1 == null && HoTTbinHistoReader.buf[33] == 1) {
								HoTTbinHistoReader.buf1 = new byte[30];
								System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf1, 0, HoTTbinHistoReader.buf1.length);
							}
							if (HoTTbinHistoReader.buf2 == null && HoTTbinHistoReader.buf[33] == 2) {
								HoTTbinHistoReader.buf2 = new byte[30];
								System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf2, 0, HoTTbinHistoReader.buf2.length);
							}
							if (HoTTbinHistoReader.buf3 == null && HoTTbinHistoReader.buf[33] == 3) {
								HoTTbinHistoReader.buf3 = new byte[30];
								System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf3, 0, HoTTbinHistoReader.buf3.length);
							}
							if (HoTTbinHistoReader.buf4 == null && HoTTbinHistoReader.buf[33] == 4) {
								HoTTbinHistoReader.buf4 = new byte[30];
								System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf4, 0, HoTTbinHistoReader.buf4.length);
							}
							if (HoTTbinHistoReader.buf1 != null && HoTTbinHistoReader.buf2 != null && HoTTbinHistoReader.buf3 != null && HoTTbinHistoReader.buf4 != null) {
								if (activeChannelNumber == HoTTAdapter.Sensor.GAM.ordinal() + 1) {
									HoTTbinHistoReader.parse4GAM(HoTTbinHistoReader.buf0, HoTTbinHistoReader.buf1, HoTTbinHistoReader.buf2, HoTTbinHistoReader.buf3, HoTTbinHistoReader.buf4);
									setTimeMarks("isRead");
									boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsGeneral, HoTTbinHistoReader.timeStep_ms);
									setTimeMarks("isReviewed");
									if (isValidSample && doFullRead) {
										HoTTbinHistoReader.recordSetGeneral.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
										setTimeMarks("isAdded");
										setTimeMarks("isPicked");
									}
								}
								HoTTbinHistoReader.isJustParsed = true;
								HoTTbinHistoReader.buf1 = HoTTbinHistoReader.buf2 = HoTTbinHistoReader.buf3 = HoTTbinHistoReader.buf4 = null;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
						if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.EAM.ordinal()]) {
							// fill data block 1 to 4
							if (HoTTbinHistoReader.buf1 == null && HoTTbinHistoReader.buf[33] == 1) {
								HoTTbinHistoReader.buf1 = new byte[30];
								System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf1, 0, HoTTbinHistoReader.buf1.length);
							}
							if (HoTTbinHistoReader.buf2 == null && HoTTbinHistoReader.buf[33] == 2) {
								HoTTbinHistoReader.buf2 = new byte[30];
								System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf2, 0, HoTTbinHistoReader.buf2.length);
							}
							if (HoTTbinHistoReader.buf3 == null && HoTTbinHistoReader.buf[33] == 3) {
								HoTTbinHistoReader.buf3 = new byte[30];
								System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf3, 0, HoTTbinHistoReader.buf3.length);
							}
							if (HoTTbinHistoReader.buf4 == null && HoTTbinHistoReader.buf[33] == 4) {
								HoTTbinHistoReader.buf4 = new byte[30];
								System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf4, 0, HoTTbinHistoReader.buf4.length);
							}
							if (HoTTbinHistoReader.buf1 != null && HoTTbinHistoReader.buf2 != null && HoTTbinHistoReader.buf3 != null && HoTTbinHistoReader.buf4 != null) {
								if (activeChannelNumber == HoTTAdapter.Sensor.EAM.ordinal() + 1) {
									HoTTbinHistoReader.parse4EAM(HoTTbinHistoReader.buf0, HoTTbinHistoReader.buf1, HoTTbinHistoReader.buf2, HoTTbinHistoReader.buf3, HoTTbinHistoReader.buf4);
									setTimeMarks("isRead");
									boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsElectric, HoTTbinHistoReader.timeStep_ms);
									setTimeMarks("isReviewed");
									if (isValidSample && doFullRead) {
										HoTTbinHistoReader.recordSetElectric.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
										setTimeMarks("isAdded");
										setTimeMarks("isPicked");
									}
								}
								HoTTbinHistoReader.isJustParsed = true;
								HoTTbinHistoReader.buf1 = HoTTbinHistoReader.buf2 = HoTTbinHistoReader.buf3 = HoTTbinHistoReader.buf4 = null;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
						if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()]) {
							// fill data block 1 to 4
							if (HoTTbinHistoReader.buf1 == null && HoTTbinHistoReader.buf[33] == 1) {
								HoTTbinHistoReader.buf1 = new byte[30];
								System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf1, 0, HoTTbinHistoReader.buf1.length);
							}
							if (HoTTbinHistoReader.buf2 == null && HoTTbinHistoReader.buf[33] == 2) {
								HoTTbinHistoReader.buf2 = new byte[30];
								System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf2, 0, HoTTbinHistoReader.buf2.length);
							}
							if (HoTTbinHistoReader.buf3 == null && HoTTbinHistoReader.buf[33] == 2) {
								HoTTbinHistoReader.buf3 = new byte[30];
								System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf3, 0, HoTTbinHistoReader.buf3.length);
							}
							if (HoTTbinHistoReader.buf1 != null && HoTTbinHistoReader.buf2 != null && HoTTbinHistoReader.buf3 != null) {
								if (activeChannelNumber == HoTTAdapter.Sensor.ESC.ordinal() + 1) {
									HoTTbinHistoReader.parse4ESC(HoTTbinHistoReader.buf0, HoTTbinHistoReader.buf1, HoTTbinHistoReader.buf2, HoTTbinHistoReader.buf3);
									setTimeMarks("isRead");
									boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsSpeedControl, HoTTbinHistoReader.timeStep_ms);
									setTimeMarks("isReviewed");
									if (isValidSample && doFullRead) {
										HoTTbinHistoReader.recordSetSpeedControl.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
										setTimeMarks("isAdded");
										setTimeMarks("isPicked");
									}
								}
								HoTTbinHistoReader.isJustParsed = true;
								HoTTbinHistoReader.buf1 = HoTTbinHistoReader.buf2 = HoTTbinHistoReader.buf3 = HoTTbinHistoReader.buf4 = null;
							}
						}
						break;
					}

					if (HoTTbinHistoReader.isJustParsed && HoTTbinHistoReader.countLostPackages > 0) {
						if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1)
							HoTTbinHistoReader.lostPackages.add(HoTTbinHistoReader.countLostPackages);
						HoTTbinHistoReader.countLostPackages = 0;
						HoTTbinHistoReader.isJustParsed = false;
					}
				} else { // skip empty block, but add time step
					if (HoTTbinHistoReader.log.isLoggable(Level.FINE))
						HoTTbinHistoReader.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

					if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
						HoTTAdapter.reverseChannelPackageLossCounter.add(0);
						HoTTbinHistoReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
						++countPackageLoss; // add up lost packages in telemetry data
						++HoTTbinHistoReader.countLostPackages;
						// HoTTbinHistoReader.pointsReceiver[0] = (int) (countPackageLoss*100.0 / ((HoTTbinHistoReader.timeStep_ms+10) / 10.0)*1000.0);
					}

					if (isChannelsChannel) {
						HoTTbinHistoReader.parse4Channel(HoTTbinHistoReader.buf);
						boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsChannel, HoTTbinHistoReader.timeStep_ms);
						setTimeMarks("isRead");
						if (isValidSample && doFullRead) {
							HoTTbinHistoReader.recordSetChannel.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks("isAdded");
							setTimeMarks("isPicked");
						}
					}

					HoTTbinHistoReader.timeStep_ms += 10;
					// reset buffer to avoid mixing data >> 20 Jul 14, not any longer required due to protocol change requesting next sensor data block
					// HoTTbinHistoReader.buf1 = HoTTbinHistoReader.buf2 = HoTTbinHistoReader.buf3 = HoTTbinHistoReader.buf4 = null;
				}
			} else if (!HoTTbinHistoReader.isTextModusSignaled) {
				HoTTbinHistoReader.isTextModusSignaled = true;
			}
		}
		if (doFullRead) {
			if (log.isLoggable(Level.INFO))
				log.log(Level.INFO, String.format("%s > records:%,9d  readings:%,9d  sampled:%,9d  overSamplingCounterMax:%4d", tmpRecordSet.getChannelConfigName(), bytesCount //$NON-NLS-1$
						/ HoTTbinHistoReader.dataBlockSize, histoRandomSample.getReadingCount(), tmpRecordSet.getRecordDataSize(true), histoRandomSample.getOverSamplingCount()));
			if (log.isLoggable(Level.TIME))
				log.log(Level.TIME, String.format("readTime: %,9d  reviewTime: %,9d  addTime: %,9d  pickTime: %,9d", TimeUnit.NANOSECONDS.toMillis(readTime), TimeUnit.NANOSECONDS.toMillis(reviewTime), TimeUnit.NANOSECONDS.toMillis(addTime), TimeUnit.NANOSECONDS.toMillis(pickTime))); //$NON-NLS-1$
			if (tmpRecordSet instanceof HistoRecordSet) {
				if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
					((HistoRecordSet) tmpRecordSet).setPackagesLostCounter(countPackageLoss);
					((HistoRecordSet) tmpRecordSet).setPackagesLostPerMille((int) (countPackageLoss / tmpRecordSet.getTime_ms((int) bytesCount / HoTTbinHistoReader.dataBlockSize - 1) * 1000. * 10.));
					((HistoRecordSet) tmpRecordSet).setPackagesCounter((int) bytesCount / HoTTbinHistoReader.dataBlockSize);
					((HistoRecordSet) tmpRecordSet).setPackagesLostMin_ms(HoTTbinReader.lostPackages.getMinValue() * 10);
					((HistoRecordSet) tmpRecordSet).setPackagesLostMax_ms(HoTTbinReader.lostPackages.getMaxValue() * 10);
					((HistoRecordSet) tmpRecordSet).setPackagesLostAvg_ms((int) HoTTbinReader.lostPackages.getAvgValue() * 10);
					((HistoRecordSet) tmpRecordSet).setPackagesLostSigma_ms((int) HoTTbinReader.lostPackages.getSigmaValue() * 10);
					if (log.isLoggable(Level.INFO))
						log.log(Level.INFO, String.format("lost:%,9d perMille:%,4d total:%,9d   lostMax_ms:%,4d lostAvg_ms=%,4d", countPackageLoss, ((HistoRecordSet) tmpRecordSet).getPackagesLostPerMille(), bytesCount //$NON-NLS-1$
								/ HoTTbinHistoReader.dataBlockSize, HoTTbinReader.lostPackages.getMaxValue() * 10, (int) HoTTbinReader.lostPackages.getAvgValue() * 10));
				}
				((HistoRecordSet) tmpRecordSet).setSensors(HoTTAdapter.Sensor.getSensorNames(HoTTAdapter.isSensorType).toArray(new String[0])); // HoTTbinReader.sensorSignature
				((HistoRecordSet) tmpRecordSet).setReadingsCounter(histoRandomSample.getReadingCount());
				((HistoRecordSet) tmpRecordSet).setSampledCounter(tmpRecordSet.getRecordDataSize(true));
				((HistoRecordSet) tmpRecordSet).dispatchLiveStatistics();
			}
			tmpRecordSet.setSaved(true);
			// no display tmpRecordSet.syncScaleOfSyncableRecords();

			device.makeInActiveDisplayable(tmpRecordSet);
			device.updateVisibilityStatus(tmpRecordSet, true);
		} else {
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, String.format("doFullRead=%b > ends", doFullRead));
		}
	}

	@Deprecated // shadows the base class method
	static void readMultiple(File file) throws IOException, DataInconsitsentException {
	}

	/**
	* read log data according to version 0 either in initialize mode for learning min/max values or in fully functional read mode.
	* reads only sample records and allocates only one single recordset for the active channel, so HoTTAdapter.isChannelsChannelEnabled does not take effect.
	* no progress bar support and no channel data modifications.
	* @param file
	* @param initializeBlocks if this number is greater than zero, the min/max values are initialized
	* @throws IOException 
	* @throws DataInconsitsentException 
	*/
	private static void readMultiple(RecordSet tmpRecordSet, BufferedInputStream data_in, long bytesCount, int initializeBlocks) throws IOException, DataInconsitsentException {
		final String $METHOD_NAME = "readMultiple";
		long startTime = System.nanoTime() / 1000000;
		HoTTbinHistoReader.readTime = HoTTbinHistoReader.reviewTime = HoTTbinHistoReader.addTime = HoTTbinHistoReader.pickTime = 0;
		HoTTbinHistoReader.lastTime = System.nanoTime();
		HoTTbinHistoReader.recordSetReceiver = null; // 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=UminRx
		HoTTbinHistoReader.recordSetGeneral = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		HoTTbinHistoReader.recordSetElectric = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2, 27=Revolution
		HoTTbinHistoReader.recordSetVario = null; // 0=RXSQ, 1=Height, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
		HoTTbinHistoReader.recordSetGPS = null; // 0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		HoTTbinHistoReader.recordSetChannel = null; // 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16
		HoTTbinHistoReader.recordSetSpeedControl = null; // 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature
		HoTTbinHistoReader.pointsReceiver = new int[9];
		HoTTbinHistoReader.pointsGeneral = new int[21];
		HoTTbinHistoReader.pointsElectric = new int[28];
		HoTTbinHistoReader.pointsVario = new int[7];
		HoTTbinHistoReader.pointsVario[2] = 100000;
		HoTTbinHistoReader.pointsGPS = new int[12];
		HoTTbinHistoReader.pointsChannel = new int[23];
		HoTTbinHistoReader.pointsSpeedControl = new int[13];
		HoTTbinHistoReader.timeStep_ms = 0;
		HoTTbinHistoReader.buf = new byte[HoTTbinHistoReader.dataBlockSize];
		HoTTbinHistoReader.buf0 = new byte[30];
		HoTTbinHistoReader.buf1 = new byte[30];
		HoTTbinHistoReader.buf2 = new byte[30];
		HoTTbinHistoReader.buf3 = new byte[30];
		HoTTbinHistoReader.buf4 = new byte[30];
		byte actualSensor = -1, lastSensor = -1;
		int logCountVario = 0, logCountGPS = 0, logCountGeneral = 0, logCountElectric = 0, logCountSpeedControl = 0;
		HoTTAdapter.reverseChannelPackageLossCounter.clear();
		HoTTbinHistoReader.lostPackages.clear();
		HoTTbinHistoReader.countLostPackages = 0;
		HoTTbinHistoReader.isJustParsed = false;
		HoTTbinHistoReader.isTextModusSignaled = false;
		// HoTTbinHistoReader.oldProtocolCount = 0;
		// HoTTbinHistoReader.blockSequenceCheck = new Vector<Byte>();
		int countPackageLoss = 0;
		HistoRandomSample histoRandomSample = null;
		HoTTAdapter device = (HoTTAdapter) HoTTbinHistoReader.application.getActiveDevice();
		int activeChannelNumber = HoTTbinHistoReader.application.getActiveChannel().getNumber();
		boolean doFullRead = initializeBlocks <= 0;
		if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
			HoTTbinHistoReader.recordSetReceiver = tmpRecordSet;
			histoRandomSample = new HistoRandomSample(HoTTbinHistoReader.pointsReceiver.length, doFullRead);
		} else if (activeChannelNumber == HoTTAdapter.Sensor.CHANNEL.ordinal() + 1) {
			HoTTbinHistoReader.recordSetChannel = tmpRecordSet;
			histoRandomSample = new HistoRandomSample(HoTTbinHistoReader.pointsChannel.length, doFullRead);
		} else if (activeChannelNumber == HoTTAdapter.Sensor.VARIO.ordinal() + 1) {
			HoTTbinHistoReader.recordSetVario = tmpRecordSet;
			histoRandomSample = new HistoRandomSample(HoTTbinHistoReader.pointsVario.length, doFullRead);
		} else if (activeChannelNumber == HoTTAdapter.Sensor.GPS.ordinal() + 1) {
			HoTTbinHistoReader.recordSetGPS = tmpRecordSet;
			histoRandomSample = new HistoRandomSample(HoTTbinHistoReader.pointsGPS.length, doFullRead);
		} else if (activeChannelNumber == HoTTAdapter.Sensor.GAM.ordinal() + 1) {
			HoTTbinHistoReader.recordSetGeneral = tmpRecordSet;
			histoRandomSample = new HistoRandomSample(HoTTbinHistoReader.pointsGeneral.length, doFullRead);
		} else if (activeChannelNumber == HoTTAdapter.Sensor.EAM.ordinal() + 1) {
			HoTTbinHistoReader.recordSetElectric = tmpRecordSet;
			histoRandomSample = new HistoRandomSample(HoTTbinHistoReader.pointsElectric.length, doFullRead);
		} else if (activeChannelNumber == HoTTAdapter.Sensor.ESC.ordinal() + 1) {
			HoTTbinHistoReader.recordSetSpeedControl = tmpRecordSet;
			histoRandomSample = new HistoRandomSample(HoTTbinHistoReader.pointsSpeedControl.length, doFullRead);
		}
		// read all the data blocks from the file, parse only for the active channel
		int initializeBlockLimit = initializeBlocks > 0 ? initializeBlocks : Integer.MAX_VALUE;
		for (int i = 0; i < initializeBlockLimit && i < bytesCount / HoTTbinHistoReader.dataBlockSize; i++) {
			data_in.read(HoTTbinHistoReader.buf);
			if (HoTTbinHistoReader.log.isLoggable(Level.FINEST) && i % 10 == 0) {
				HoTTbinHistoReader.log.logp(Level.FINEST, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinHistoReader.buf.length));
				HoTTbinHistoReader.log.logp(Level.FINEST, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinHistoReader.buf, HoTTbinHistoReader.buf.length));
			}

			if (!HoTTAdapter.isFilterTextModus || (HoTTbinHistoReader.buf[6] & 0x01) == 0) { // switch into text modus
				if (HoTTbinHistoReader.buf[33] >= 0 && HoTTbinHistoReader.buf[33] <= 4 && HoTTbinHistoReader.buf[3] != 0 && HoTTbinHistoReader.buf[4] != 0) { // buf 3, 4, tx,rx
					if (HoTTbinHistoReader.log.isLoggable(Level.FINE))
						HoTTbinHistoReader.log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", HoTTbinHistoReader.buf[7], HoTTbinHistoReader.buf[33]));
					if (HoTTbinHistoReader.log.isLoggable(Level.FINEST))
						HoTTbinHistoReader.log.logp(Level.FINEST, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(new byte[] {
								HoTTbinHistoReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinHistoReader.buf[7], false));

					if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
						HoTTAdapter.reverseChannelPackageLossCounter.add(1);
						HoTTbinHistoReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
						if (HoTTbinHistoReader.buf[33] == 0 && (HoTTbinHistoReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinHistoReader.buf, 40) >= 0) {
							parse4Receiver(HoTTbinHistoReader.buf);
							boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsReceiver, HoTTbinHistoReader.timeStep_ms);
							setTimeMarks("isRead");
							if (isValidSample && doFullRead) {
								HoTTbinHistoReader.recordSetReceiver.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
								setTimeMarks("isAdded");
								setTimeMarks("isPicked");
							}
						}
					} else if (activeChannelNumber == HoTTAdapter.Sensor.CHANNEL.ordinal() + 1) {
						parse4Channel(HoTTbinHistoReader.buf);
						boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsChannel, HoTTbinHistoReader.timeStep_ms);
						setTimeMarks("isRead");
						if (isValidSample && doFullRead) {
							HoTTbinHistoReader.recordSetChannel.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks("isAdded");
							setTimeMarks("isPicked");
						}
					}

					HoTTbinHistoReader.timeStep_ms += 10;// add default time step from log record of 10 msec

					// detect sensor switch
					if (actualSensor == -1)
						lastSensor = actualSensor = (byte) (HoTTbinHistoReader.buf[7] & 0xFF);
					else
						actualSensor = (byte) (HoTTbinHistoReader.buf[7] & 0xFF);

					if (actualSensor != lastSensor) {
						// write data just after sensor switch
						if (logCountVario >= 3 || logCountGPS >= 4 || logCountGeneral >= 5 || logCountElectric >= 5 || logCountSpeedControl >= 5) {
							switch (lastSensor) {
							case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
							case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
								if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.VARIO.ordinal()]) {
									if (activeChannelNumber == HoTTAdapter.Sensor.VARIO.ordinal() + 1) {
										final int versionOBS = -99;
										HoTTbinHistoReader.parse4Vario(versionOBS, HoTTbinHistoReader.buf0, HoTTbinHistoReader.buf1, HoTTbinHistoReader.buf2);
										setTimeMarks("isRead");
										boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsVario, HoTTbinHistoReader.timeStep_ms);
										setTimeMarks("isReviewed");
										if (isValidSample && doFullRead) {
											HoTTbinHistoReader.recordSetVario.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											setTimeMarks("isAdded");
											setTimeMarks("isPicked");
										}
									}
									HoTTbinHistoReader.isJustParsed = true;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GPS_115200:
							case HoTTAdapter.SENSOR_TYPE_GPS_19200:
								if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GPS.ordinal()]) {
									if (activeChannelNumber == HoTTAdapter.Sensor.GPS.ordinal() + 1) {
										parse4GPS(HoTTbinHistoReader.buf0, HoTTbinHistoReader.buf1, HoTTbinHistoReader.buf2, HoTTbinHistoReader.buf3);
										setTimeMarks("isRead");
										boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsGPS, HoTTbinHistoReader.timeStep_ms);
										setTimeMarks("isReviewed");
									if (isValidSample && doFullRead) {
											HoTTbinHistoReader.recordSetGPS.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											setTimeMarks("isAdded");
											setTimeMarks("isPicked");
										}
									}
									HoTTbinHistoReader.isJustParsed = true;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
							case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
								if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()]) {
									if (activeChannelNumber == HoTTAdapter.Sensor.GAM.ordinal() + 1) {
										HoTTbinHistoReader.parse4GAM(HoTTbinHistoReader.buf0, HoTTbinHistoReader.buf1, HoTTbinHistoReader.buf2, HoTTbinHistoReader.buf3, HoTTbinHistoReader.buf4);
										setTimeMarks("isRead");
										boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsGeneral, HoTTbinHistoReader.timeStep_ms);
										setTimeMarks("isReviewed");
										if (isValidSample && doFullRead) {
											HoTTbinHistoReader.recordSetGeneral.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											setTimeMarks("isAdded");
											setTimeMarks("isPicked");
										}
									}
									HoTTbinHistoReader.isJustParsed = true;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
								if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.EAM.ordinal()]) {
									if (activeChannelNumber == HoTTAdapter.Sensor.EAM.ordinal() + 1) {
										HoTTbinHistoReader.parse4EAM(HoTTbinHistoReader.buf0, HoTTbinHistoReader.buf1, HoTTbinHistoReader.buf2, HoTTbinHistoReader.buf3, HoTTbinHistoReader.buf4);
										setTimeMarks("isRead");
										boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsElectric, HoTTbinHistoReader.timeStep_ms);
										setTimeMarks("isReviewed");
										if (isValidSample && doFullRead) {
											HoTTbinHistoReader.recordSetElectric.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											setTimeMarks("isAdded");
											setTimeMarks("isPicked");
										}
									}
									HoTTbinHistoReader.isJustParsed = true;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
								if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()]) {
									if (activeChannelNumber == HoTTAdapter.Sensor.ESC.ordinal() + 1) {
										HoTTbinHistoReader.parse4ESC(HoTTbinHistoReader.buf0, HoTTbinHistoReader.buf1, HoTTbinHistoReader.buf2, HoTTbinHistoReader.buf3);
										setTimeMarks("isRead");
										boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsSpeedControl, HoTTbinHistoReader.timeStep_ms);
										setTimeMarks("isReviewed");
										if (isValidSample && doFullRead) {
											HoTTbinHistoReader.recordSetSpeedControl.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											setTimeMarks("isAdded");
											setTimeMarks("isPicked");
										}
									}
									HoTTbinHistoReader.isJustParsed = true;
								}
								break;
							}
						}

						if (HoTTbinHistoReader.log.isLoggable(Level.FINE))
							HoTTbinHistoReader.log.logp(Level.FINE, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, "logCountVario = " + logCountVario + " logCountGPS = "
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
					if (HoTTbinHistoReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinHistoReader.buf, 0) != 0) {
						System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf0, 0, HoTTbinHistoReader.buf0.length);
					} else if (HoTTbinHistoReader.buf[33] == 1) {
						System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf1, 0, HoTTbinHistoReader.buf1.length);
					} else if (HoTTbinHistoReader.buf[33] == 2) {
						System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf2, 0, HoTTbinHistoReader.buf2.length);
					} else if (HoTTbinHistoReader.buf[33] == 3) {
						System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf3, 0, HoTTbinHistoReader.buf3.length);
					} else if (HoTTbinHistoReader.buf[33] == 4) {
						System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf4, 0, HoTTbinHistoReader.buf4.length);
					}

					// if (HoTTbinHistoReader.blockSequenceCheck.size() > 1) {
					// if(HoTTbinHistoReader.blockSequenceCheck.get(0) - HoTTbinHistoReader.blockSequenceCheck.get(1) > 1 && HoTTbinHistoReader.blockSequenceCheck.get(0) - HoTTbinHistoReader.blockSequenceCheck.get(1) < 4)
					// ++HoTTbinHistoReader.oldProtocolCount;
					// HoTTbinHistoReader.blockSequenceCheck.remove(0);
					// }
					// HoTTbinHistoReader.blockSequenceCheck.add(HoTTbinHistoReader.buf[33]);

					if (HoTTbinHistoReader.isJustParsed && HoTTbinHistoReader.countLostPackages > 0) {
						if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1)
							HoTTbinHistoReader.lostPackages.add(HoTTbinHistoReader.countLostPackages);
						HoTTbinHistoReader.countLostPackages = 0;
						HoTTbinHistoReader.isJustParsed = false;
					}
				} else

				{ // tx,rx == 0
					if (HoTTbinHistoReader.log.isLoggable(Level.FINE))
						HoTTbinHistoReader.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

					if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
						HoTTAdapter.reverseChannelPackageLossCounter.add(0);
						HoTTbinHistoReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
						++countPackageLoss; // add up lost packages in telemetry data
						++HoTTbinHistoReader.countLostPackages;
						// HoTTbinHistoReader.pointsReceiver[0] = (int) (countPackageLoss*100.0 / ((HoTTbinHistoReader.timeStep_ms+10) / 10.0)*1000.0);
					}

					boolean isChannelsChannel = activeChannelNumber == HoTTAdapter.Sensor.CHANNEL.ordinal() + 1;
					if (isChannelsChannel) {
						setTimeMarks("isRead");
						HoTTbinHistoReader.parse4Channel(HoTTbinHistoReader.buf);
						boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsChannel, HoTTbinHistoReader.timeStep_ms);
						if (isValidSample && doFullRead) {
							HoTTbinHistoReader.recordSetChannel.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks("isAdded");
							setTimeMarks("isPicked");
						}
					}

					HoTTbinHistoReader.timeStep_ms += 10;
					// reset buffer to avoid mixing data >> 20 Jul 14, not any longer required due to protocol change requesting next sensor data block
					// HoTTbinHistoReader.buf1 = HoTTbinHistoReader.buf2 = HoTTbinHistoReader.buf3 = HoTTbinHistoReader.buf4 = null;
				}
			} else if (!HoTTbinHistoReader.isTextModusSignaled) {
				HoTTbinHistoReader.isTextModusSignaled = true;
			}
		}
		// if (HoTTbinHistoReader.oldProtocolCount > 2) {
		// application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2405, new Object[] { HoTTbinHistoReader.oldProtocolCount }));
		// }
		if (doFullRead) {
			if (log.isLoggable(Level.INFO))
				log.log(Level.INFO, String.format("%s > records:%,9d  readings:%,9d  sampled:%,9d  overSamplingCounterMax:%4d", tmpRecordSet.getChannelConfigName(), bytesCount //$NON-NLS-1$
						/ HoTTbinHistoReader.dataBlockSize, histoRandomSample.getReadingCount(), tmpRecordSet.getRecordDataSize(true), histoRandomSample.getOverSamplingCount()));
			if (log.isLoggable(Level.TIME))
				log.log(Level.TIME, String.format("readTime: %,9d  reviewTime: %,9d  addTime: %,9d  pickTime: %,9d", TimeUnit.NANOSECONDS.toMillis(readTime), TimeUnit.NANOSECONDS.toMillis(reviewTime), TimeUnit.NANOSECONDS.toMillis(addTime), TimeUnit.NANOSECONDS.toMillis(pickTime))); //$NON-NLS-1$
			if (tmpRecordSet instanceof HistoRecordSet) {
				if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
					((HistoRecordSet) tmpRecordSet).setPackagesLostCounter(countPackageLoss);
					((HistoRecordSet) tmpRecordSet).setPackagesLostPerMille((int) (countPackageLoss / tmpRecordSet.getTime_ms((int) bytesCount / HoTTbinHistoReader.dataBlockSize - 1) * 1000. * 10.));
					((HistoRecordSet) tmpRecordSet).setPackagesCounter((int) bytesCount / HoTTbinHistoReader.dataBlockSize);
					((HistoRecordSet) tmpRecordSet).setPackagesLostMin_ms(HoTTbinReader.lostPackages.getMinValue() * 10);
					((HistoRecordSet) tmpRecordSet).setPackagesLostMax_ms(HoTTbinReader.lostPackages.getMaxValue() * 10);
					((HistoRecordSet) tmpRecordSet).setPackagesLostAvg_ms((int) HoTTbinReader.lostPackages.getAvgValue() * 10);
					((HistoRecordSet) tmpRecordSet).setPackagesLostSigma_ms((int) HoTTbinReader.lostPackages.getSigmaValue() * 10);
					if (log.isLoggable(Level.INFO))
						log.log(Level.INFO, String.format("lost:%,9d perMille:%,4d total:%,9d   lostMax_ms:%,4d lostAvg_ms=%,4d", countPackageLoss, ((HistoRecordSet) tmpRecordSet).getPackagesLostPerMille(), bytesCount //$NON-NLS-1$
								/ HoTTbinHistoReader.dataBlockSize, HoTTbinReader.lostPackages.getMaxValue() * 10, (int) HoTTbinReader.lostPackages.getAvgValue() * 10));
				}
				((HistoRecordSet) tmpRecordSet).setSensors(HoTTAdapter.Sensor.getSensorNames(HoTTAdapter.isSensorType).toArray(new String[0])); // HoTTbinReader.sensorSignature
				((HistoRecordSet) tmpRecordSet).setReadingsCounter(histoRandomSample.getReadingCount());
				((HistoRecordSet) tmpRecordSet).setSampledCounter(tmpRecordSet.getRecordDataSize(true));
				((HistoRecordSet) tmpRecordSet).dispatchLiveStatistics();
			}
			tmpRecordSet.setSaved(true);
			// no display tmpRecordSet.syncScaleOfSyncableRecords();

			device.makeInActiveDisplayable(tmpRecordSet);
			device.updateVisibilityStatus(tmpRecordSet, true);
		} else {
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, String.format("doFullRead=%b > ends", doFullRead));
		}
	}

	private static void setTimeMarks(String mark) {
		HoTTbinHistoReader.currentTime = System.nanoTime();
		if (mark.equals("isRead")) {
			HoTTbinHistoReader.readTime += HoTTbinHistoReader.currentTime - HoTTbinHistoReader.lastTime;
		} else if (mark.equals("isReviewed")) {
			HoTTbinHistoReader.reviewTime += HoTTbinHistoReader.currentTime - HoTTbinHistoReader.lastTime;
		} else if (mark.equals("isAdded")) {
			HoTTbinHistoReader.addTime += HoTTbinHistoReader.currentTime - HoTTbinHistoReader.lastTime;
		} else if (mark.equals("isPicked")) {
			HoTTbinHistoReader.pickTime += HoTTbinHistoReader.currentTime - HoTTbinHistoReader.lastTime;
		}
		HoTTbinHistoReader.lastTime = HoTTbinHistoReader.currentTime;
	}
}
