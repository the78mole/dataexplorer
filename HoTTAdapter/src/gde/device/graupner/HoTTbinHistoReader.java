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
    					2016,2017 Thomas Eickert
****************************************************************************************/
package gde.device.graupner;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import gde.GDE;
import gde.data.RecordSet;
import gde.device.HistoRandomSample;
import gde.device.ScoreLabelTypes;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.histo.cache.VaultCollector;
import gde.io.DataParser;
import gde.log.Level;
import gde.utils.StringHelper;

/**
 * read Graupner HoTT binary data for history analysis.
 * provide data in a histo recordset.
 * for small files (around 1 minute) no measurements are added to the recordset.
 * reads one single channel only.
 * supports sampling to maximize the throughput.
 * @author Thomas Eickert
 */
public class HoTTbinHistoReader extends HoTTbinReader {
	final private static String	$CLASS_NAME	= HoTTbinHistoReader.class.getName();
	final private static Logger	log					= Logger.getLogger(HoTTbinHistoReader.$CLASS_NAME);

	private static enum TimeMark {
		INITIATED, ADDED, PICKED, READ, REVIEWED, FINISHED
	};

	private static int						recordTimespan_ms	= 10;																																		// HoTT logs data rate defined by the channel log
	private static long						nanoTime;
	private static long						currentTime, initiateTime, readTime, reviewTime, addTime, pickTime, finishTime, lastTime;
	private static Path						filePath;
	private static RecordSet			tmpRecordSet;
	private static VaultCollector	truss;

	@Deprecated // shadows the base class method
	public static synchronized void read(String filePath) throws Exception {
	}

	@Deprecated // shadows the base class method
	static void readSingle(File file) throws IOException, DataInconsitsentException {
	}

	@Deprecated // shadows the base class method
	static void readMultiple(File file) throws IOException, DataInconsitsentException {
	}

	/**
	 * @param newTruss which is promoted to a full vault object if the file has a minimum length.
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws DataTypeException
	 * @throws DataInconsitsentException
	 */
	public static synchronized void read(VaultCollector newTruss) throws IOException, DataTypeException, DataInconsitsentException {
		HoTTbinHistoReader.nanoTime = System.nanoTime();
		HoTTbinHistoReader.initiateTime = HoTTbinHistoReader.readTime = HoTTbinHistoReader.reviewTime = HoTTbinHistoReader.addTime = HoTTbinHistoReader.pickTime = HoTTbinHistoReader.finishTime = 0;
		HoTTbinHistoReader.lastTime = System.nanoTime();

		HoTTbinHistoReader.truss = newTruss;
		HoTTbinHistoReader.filePath = truss.getLogFileAsPath();
		File file = HoTTbinHistoReader.filePath.toFile();
		try (BufferedInputStream data_in = new BufferedInputStream(new FileInputStream(file))) {
			HoTTbinHistoReader.read(data_in);
		}
		catch (InvalidObjectException e) {
			// so any anther exception is propagated to the caller
		}
		finally {
			if (HoTTbinHistoReader.filePath.getFileName().startsWith("~") && file.exists()) file.delete(); // 'GRAUPNER SD LOG8' //$NON-NLS-1$
		}
	}

	/**
	 * read file.
	 * provide data in a histo tmpRecordSet.
	 * @param data_in
	 * @param path may hold the filename only
	 * @param startTimeStamp_ms
	 * @throws IOException
	 * @throws DataTypeException
	 * @throws DataInconsitsentException
	 */
	private static synchronized void read(BufferedInputStream data_in) throws IOException, DataTypeException, DataInconsitsentException {
		final String $METHOD_NAME = "read"; //$NON-NLS-1$
		File file = HoTTbinHistoReader.filePath.toFile();
		HashMap<String, String> header = null;
		HoTTAdapter device = (HoTTAdapter) HoTTbinHistoReader.application.getActiveDevice();
		if (HoTTbinHistoReader.log.isLoggable(Level.FINE)) HoTTbinHistoReader.log.logp(Level.FINE, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, " recordSetBaseName=" + truss.getLogRecordsetBaseName()); //$NON-NLS-1$

		if (file.length() > NUMBER_LOG_RECORDS_MIN * dataBlockSize) {
			try {
				header = HoTTbinReader.getFileInfo(HoTTbinHistoReader.filePath.toFile());
			}
			catch (DataTypeException e) {
				HoTTbinHistoReader.log.log(Level.WARNING, String.format("%s  %s", e.getMessage(), HoTTbinHistoReader.filePath)); // 'GRAUPNER SD LOG8' //$NON-NLS-1$
			}
			if (header != null && header.size() > 0 && HoTTAdapter.Sensor.getChannelNumbers(HoTTAdapter.isSensorType).contains(truss.getVaultChannelNumber())) {
				if (!header.get(HoTTAdapter.FILE_PATH).equals(HoTTbinHistoReader.filePath.toString())) {
					// accept this 'GRAUPNER SD LOG8' file, extracted file starts with '~'
					HoTTbinHistoReader.filePath = Paths.get(header.get(HoTTAdapter.FILE_PATH));
				}
				int readLimitMark = (LOG_RECORD_SCAN_START + NUMBER_LOG_RECORDS_TO_SCAN) * dataBlockSize + 1;
				if (Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) <= 1) {
					HoTTbinHistoReader.isReceiverOnly = Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) == 0;
					boolean isChannelsChannel = device.channels.getActiveChannelNumber() == HoTTAdapter.Sensor.CHANNEL.ordinal() + 1; // instead of HoTTAdapter setting
					long numberDatablocks = file.length() / HoTTbinReader.dataBlockSize / (HoTTbinReader.isReceiverOnly && !isChannelsChannel ? 10 : 1);
					tmpRecordSet = RecordSet.createRecordSet(truss.getLogRecordsetBaseName(), device, HoTTbinHistoReader.application.getActiveChannelNumber(), true, true, false);
					tmpRecordSet.setStartTimeStamp(HoTTbinReader.getStartTimeStamp(file, numberDatablocks));
					tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", tmpRecordSet.getStartTimeStamp())); //$NON-NLS-1$
					tmpRecordSet.descriptionAppendFilename(HoTTbinHistoReader.filePath.getFileName().toString());
					data_in.mark(readLimitMark); // reduces # of overscan records from 57 to 23 (to 38 with 1500 blocks)
					HistoRandomSample histoRandomSample = HoTTbinHistoReader.readSingle(data_in, LOG_RECORD_SCAN_START + NUMBER_LOG_RECORDS_TO_SCAN, new int[0], new int[0]);
					data_in.reset();
					HoTTbinHistoReader.readSingle(data_in, -1, histoRandomSample.getMaxPoints(), histoRandomSample.getMinPoints());
				}
				else {
					long numberDatablocks = file.length() / HoTTbinReader.dataBlockSize;
					tmpRecordSet = RecordSet.createRecordSet(truss.getLogRecordsetBaseName(), device, HoTTbinHistoReader.application.getActiveChannelNumber(), true, true, false);
					tmpRecordSet.setStartTimeStamp(HoTTbinReader.getStartTimeStamp(file, numberDatablocks));
					tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", tmpRecordSet.getStartTimeStamp())); //$NON-NLS-1$
					tmpRecordSet.descriptionAppendFilename(HoTTbinHistoReader.filePath.getFileName().toString());
					data_in.mark(readLimitMark);
					HistoRandomSample histoRandomSample = HoTTbinHistoReader.readMultiple(data_in, LOG_RECORD_SCAN_START + NUMBER_LOG_RECORDS_TO_SCAN, new int[0], new int[0]);
					data_in.reset();
					HoTTbinHistoReader.readMultiple(data_in, -1, histoRandomSample.getMaxPoints(), histoRandomSample.getMinPoints());
				}
			}
		}
	}

	/**
	* read log data according to version 0.
	* allocates only one single recordset for the active channel, so HoTTAdapter.isChannelsChannelEnabled does not take any effect.
	* no progress bar support and no channel data modifications.
	* @param data_in
	* @param initializeBlocks if this number is greater than zero, the min/max values are initialized
	* @param maxPoints utilizes the minMax values from a previous run and thus reduces oversampling
	* @param minPoints utilizes the minMax values from a previous run and thus reduces oversampling
	* @throws IOException
	* @throws DataInconsitsentException
	* @return
	*/
	private static HistoRandomSample readSingle(InputStream data_in, int initializeBlocks, int[] maxPoints, int[] minPoints) throws DataInconsitsentException, IOException {
		final String $METHOD_NAME = "readSingle"; //$NON-NLS-1$
		HoTTbinHistoReader.recordSetReceiver = null; // 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=UminRx
		HoTTbinHistoReader.recordSetGAM = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		HoTTbinHistoReader.recordSetEAM = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2, 27=Revolution
		HoTTbinHistoReader.recordSetVario = null; // 0=RXSQ, 1=Height, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
		HoTTbinHistoReader.recordSetGPS = null; // 0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		HoTTbinHistoReader.recordSetChannel = null; // 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16
		HoTTbinHistoReader.recordSetESC = null; // 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature
		HoTTbinHistoReader.pointsReceiver = new int[9];
		HoTTbinHistoReader.pointsGAM = new int[21];
		HoTTbinHistoReader.pointsEAM = new int[28];
		HoTTbinHistoReader.pointsVario = new int[7];
		HoTTbinHistoReader.pointsVario[2] = 100000;
		HoTTbinHistoReader.pointsGPS = new int[12];
		HoTTbinHistoReader.pointsChannel = new int[23];
		HoTTbinHistoReader.pointsESC = new int[13];
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
		boolean isFilterEnabledPre = HoTTAdapter.isFilterEnabled;
		HoTTAdapter.isFilterEnabled = true;
		HoTTbinHistoReader.isJustParsed = false;
		int countPackageLoss = 0;
		HistoRandomSample histoRandomSample = null;
		HoTTAdapter device = (HoTTAdapter) HoTTbinHistoReader.application.getActiveDevice();
		int activeChannelNumber = device.channels.getActiveChannelNumber(); // HoTTbinHistoReader.application.getActiveChannel().getNumber();

		if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
			HoTTbinHistoReader.recordSetReceiver = tmpRecordSet;
			histoRandomSample = HistoRandomSample.createHistoRandomSample(activeChannelNumber, HoTTbinHistoReader.pointsReceiver.length, recordTimespan_ms);
		}
		else if (activeChannelNumber == HoTTAdapter.Sensor.CHANNEL.ordinal() + 1) {
			HoTTbinHistoReader.recordSetChannel = tmpRecordSet;
			histoRandomSample = HistoRandomSample.createHistoRandomSample(activeChannelNumber, HoTTbinHistoReader.pointsChannel.length, recordTimespan_ms);
		}
		else if (activeChannelNumber == HoTTAdapter.Sensor.VARIO.ordinal() + 1) {
			HoTTbinHistoReader.recordSetVario = tmpRecordSet;
			histoRandomSample = HistoRandomSample.createHistoRandomSample(activeChannelNumber, HoTTbinHistoReader.pointsVario.length, recordTimespan_ms);
		}
		else if (activeChannelNumber == HoTTAdapter.Sensor.GPS.ordinal() + 1) {
			HoTTbinHistoReader.recordSetGPS = tmpRecordSet;
			histoRandomSample = HistoRandomSample.createHistoRandomSample(activeChannelNumber, HoTTbinHistoReader.pointsGPS.length, recordTimespan_ms);
		}
		else if (activeChannelNumber == HoTTAdapter.Sensor.GAM.ordinal() + 1) {
			HoTTbinHistoReader.recordSetGAM = tmpRecordSet;
			histoRandomSample = HistoRandomSample.createHistoRandomSample(activeChannelNumber, HoTTbinHistoReader.pointsGAM.length, recordTimespan_ms);
		}
		else if (activeChannelNumber == HoTTAdapter.Sensor.EAM.ordinal() + 1) {
			HoTTbinHistoReader.recordSetEAM = tmpRecordSet;
			histoRandomSample = HistoRandomSample.createHistoRandomSample(activeChannelNumber, HoTTbinHistoReader.pointsEAM.length, recordTimespan_ms);
		}
		else if (activeChannelNumber == HoTTAdapter.Sensor.ESC.ordinal() + 1) {
			HoTTbinHistoReader.recordSetESC = tmpRecordSet;
			histoRandomSample = HistoRandomSample.createHistoRandomSample(activeChannelNumber, HoTTbinHistoReader.pointsESC.length, recordTimespan_ms);
		}
		else
			throw new UnsupportedOperationException();

		histoRandomSample.setMaxPoints(maxPoints);
		histoRandomSample.setMinPoints(minPoints);
		// read all the data blocks from the file, parse only for the active channel
		setTimeMarks(TimeMark.INITIATED);
		boolean isChannelsChannel = activeChannelNumber == HoTTAdapter.Sensor.CHANNEL.ordinal() + 1; // instead of HoTTAdapter setting
		long fileLength = HoTTbinHistoReader.filePath.toFile().length();
		boolean doFullRead = initializeBlocks <= 0;
		int datablocksLimit = (doFullRead ? (int) fileLength / HoTTbinHistoReader.dataBlockSize : initializeBlocks) / (HoTTbinHistoReader.isReceiverOnly && !isChannelsChannel ? 10 : 1);
		for (int i = 0; i < datablocksLimit; i++) {
			// if (log.isLoggable(Level.TIME))
			// log.log(Level.TIME, String.format("markpos: %,9d i: %,9d ", data_in.markpos, i));
			data_in.read(HoTTbinHistoReader.buf);
			if (HoTTbinHistoReader.log.isLoggable(Level.FINE) && i % 10 == 0) {
				HoTTbinHistoReader.log.logp(Level.FINE, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinHistoReader.buf.length));
				HoTTbinHistoReader.log.logp(Level.FINE, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinHistoReader.buf, HoTTbinHistoReader.buf.length));
			}

			if (!HoTTAdapter.isFilterTextModus || (HoTTbinHistoReader.buf[6] & 0x01) == 0) { // switch into text modus
				if (HoTTbinHistoReader.buf[33] >= 0 && HoTTbinHistoReader.buf[33] <= 4 && HoTTbinHistoReader.buf[3] != 0 && HoTTbinHistoReader.buf[4] != 0) { // buf 3, 4, tx,rx
					if (HoTTbinHistoReader.log.isLoggable(Level.FINE)) HoTTbinHistoReader.log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", HoTTbinHistoReader.buf[7], HoTTbinHistoReader.buf[33])); //$NON-NLS-1$
					if (HoTTbinHistoReader.log.isLoggable(Level.FINER)) HoTTbinHistoReader.log.logp(Level.FINER, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME,
							StringHelper.byte2Hex2CharString(new byte[] { HoTTbinHistoReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinHistoReader.buf[7], false));
					if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
						HoTTAdapter.reverseChannelPackageLossCounter.add(1);
						HoTTbinHistoReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
						if (HoTTbinHistoReader.buf[33] == 0 && (HoTTbinHistoReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinHistoReader.buf, 40) >= 0) {
							parse4Receiver(HoTTbinHistoReader.buf);
							setTimeMarks(TimeMark.READ);
							boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsReceiver, HoTTbinHistoReader.timeStep_ms);
							setTimeMarks(TimeMark.REVIEWED);
							if (isValidSample && doFullRead) {
								HoTTbinHistoReader.recordSetReceiver.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
								setTimeMarks(TimeMark.ADDED);
								setTimeMarks(TimeMark.PICKED);
							}
						}
					}
					else if (activeChannelNumber == HoTTAdapter.Sensor.CHANNEL.ordinal() + 1) {
						parse4Channel(HoTTbinHistoReader.buf);
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsChannel, HoTTbinHistoReader.timeStep_ms);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							HoTTbinHistoReader.recordSetChannel.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}
					}
					if (HoTTbinHistoReader.isReceiverOnly && !isChannelsChannel) {
						for (int j = 0; j < 9; j++) {
							data_in.read(HoTTbinHistoReader.buf);
							HoTTbinHistoReader.timeStep_ms += recordTimespan_ms;
						}
					}
					// fill data block 0 receiver voltage and temperature
					if (HoTTbinHistoReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinHistoReader.buf, 0) != 0) {
						System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf0, 0, HoTTbinHistoReader.buf0.length);
					}

					HoTTbinHistoReader.timeStep_ms += recordTimespan_ms;

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
									if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && HoTTbinReader.tmpClimb10 < 40000 && HoTTbinReader.tmpClimb10 > 20000)) {
										setTimeMarks(TimeMark.READ);
										boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsVario, HoTTbinHistoReader.timeStep_ms);
										setTimeMarks(TimeMark.REVIEWED);
										if (isValidSample && doFullRead) {
											HoTTbinHistoReader.recordSetVario.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											setTimeMarks(TimeMark.ADDED);
											setTimeMarks(TimeMark.PICKED);
										}
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
									if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpClimb1 > 10000 && HoTTbinReader.tmpClimb3 > 30 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 4500)) {
										setTimeMarks(TimeMark.READ);
										boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsGPS, HoTTbinHistoReader.timeStep_ms);
										setTimeMarks(TimeMark.REVIEWED);
										if (isValidSample && doFullRead) {
											HoTTbinHistoReader.recordSetGPS.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											setTimeMarks(TimeMark.ADDED);
											setTimeMarks(TimeMark.PICKED);
										}
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
									if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpClimb3 > 30 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600
											&& Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
										setTimeMarks(TimeMark.READ);
										boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsGAM, HoTTbinHistoReader.timeStep_ms);
										setTimeMarks(TimeMark.REVIEWED);
										if (isValidSample && doFullRead) {
											HoTTbinHistoReader.recordSetGAM.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											setTimeMarks(TimeMark.ADDED);
											setTimeMarks(TimeMark.PICKED);
										}
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
									if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpClimb3 > 30 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600
											&& Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
										setTimeMarks(TimeMark.READ);
										boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsEAM, HoTTbinHistoReader.timeStep_ms);
										setTimeMarks(TimeMark.REVIEWED);
										if (isValidSample && doFullRead) {
											HoTTbinHistoReader.recordSetEAM.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											setTimeMarks(TimeMark.ADDED);
											setTimeMarks(TimeMark.PICKED);
										}
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
									if (!HoTTAdapter.isFilterEnabled
											|| HoTTbinReader.tmpVoltage > 0 && HoTTbinReader.tmpVoltage < 1000 && HoTTbinReader.tmpCurrent < 4000 && HoTTbinReader.tmpCurrent > -10 && HoTTbinReader.tmpRevolution > -1
													&& HoTTbinReader.tmpRevolution < 20000 && !(HoTTbinReader.pointsESC[6] != 0 && HoTTbinReader.pointsESC[6] / 1000 - HoTTbinReader.tmpTemperatureFet > 20)) {
										setTimeMarks(TimeMark.READ);
										boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsESC, HoTTbinHistoReader.timeStep_ms);
										setTimeMarks(TimeMark.REVIEWED);
										if (isValidSample && doFullRead) {
											HoTTbinHistoReader.recordSetESC.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											setTimeMarks(TimeMark.ADDED);
											setTimeMarks(TimeMark.PICKED);
										}
									}
								}
								HoTTbinHistoReader.isJustParsed = true;
								HoTTbinHistoReader.buf1 = HoTTbinHistoReader.buf2 = HoTTbinHistoReader.buf3 = HoTTbinHistoReader.buf4 = null;
							}
						}
						break;
					}

					if (HoTTbinHistoReader.isJustParsed && HoTTbinHistoReader.countLostPackages > 0) {
						if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) HoTTbinHistoReader.lostPackages.add(HoTTbinHistoReader.countLostPackages);
						HoTTbinHistoReader.countLostPackages = 0;
						HoTTbinHistoReader.isJustParsed = false;
					}
				}
				else { // skip empty block, but add time step
					if (HoTTbinHistoReader.log.isLoggable(Level.FINE)) HoTTbinHistoReader.log.log(Level.FINE, "-->> Found tx=rx=0 dBm"); //$NON-NLS-1$

					if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
						HoTTAdapter.reverseChannelPackageLossCounter.add(0);
						HoTTbinHistoReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
						++countPackageLoss; // add up lost packages in telemetry data
						++HoTTbinHistoReader.countLostPackages;
						// HoTTbinHistoReader.pointsReceiver[0] = (int) (countPackageLoss*100.0 / ((HoTTbinHistoReader.timeStep_ms+10) / 10.0)*1000.0);
					}

					if (isChannelsChannel) {
						HoTTbinHistoReader.parse4Channel(HoTTbinHistoReader.buf);
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsChannel, HoTTbinHistoReader.timeStep_ms);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							HoTTbinHistoReader.recordSetChannel.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}
					}

					HoTTbinHistoReader.timeStep_ms += recordTimespan_ms;
					// reset buffer to avoid mixing data >> 20 Jul 14, not any longer required due to protocol change requesting next sensor data block
					// HoTTbinHistoReader.buf1 = HoTTbinHistoReader.buf2 = HoTTbinHistoReader.buf3 = HoTTbinHistoReader.buf4 = null;
				}
			}
			else if (!HoTTbinHistoReader.isTextModusSignaled) {
				HoTTbinHistoReader.isTextModusSignaled = true;
			}
		}
		if (doFullRead) {
			final Integer[] scores = new Integer[ScoreLabelTypes.values.length];
			// values are multiplied by 1000 as this is the convention for internal values in order to avoid rounding errors for values below 1.0 (0.5 -> 0)
			// scores for duration and timestep values are filled in by the HistoVault
			scores[ScoreLabelTypes.TOTAL_READINGS.ordinal()] = histoRandomSample.getReadingCount();
			scores[ScoreLabelTypes.TOTAL_PACKAGES.ordinal()] = (int) fileLength / HoTTbinHistoReader.dataBlockSize;
			scores[ScoreLabelTypes.LOST_PACKAGES.ordinal()] = countPackageLoss;
			scores[ScoreLabelTypes.LOST_PACKAGES_PER_MILLE.ordinal()] = tmpRecordSet.getMaxTime_ms() > 0 ? (int) (countPackageLoss / tmpRecordSet.getMaxTime_ms() * 1000. * recordTimespan_ms) * 1000 : 0;
			scores[ScoreLabelTypes.LOST_PACKAGES_AVG_MS.ordinal()] = (int) HoTTbinReader.lostPackages.getAvgValue() * recordTimespan_ms * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_MAX_MS.ordinal()] = HoTTbinReader.lostPackages.getMaxValue() * recordTimespan_ms * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_MIN_MS.ordinal()] = HoTTbinReader.lostPackages.getMinValue() * recordTimespan_ms * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_SIGMA_MS.ordinal()] = (int) HoTTbinReader.lostPackages.getSigmaValue() * recordTimespan_ms * 1000;
			scores[ScoreLabelTypes.SENSORS.ordinal()] = HoTTAdapter.Sensor.getSensorNames(HoTTAdapter.isSensorType).size() * 1000; // do not subtract Channel / Receiver --- is always false
			scores[ScoreLabelTypes.SENSOR_VARIO.ordinal()] = HoTTAdapter.Sensor.getSensorNames(HoTTAdapter.isSensorType).contains(HoTTAdapter.Sensor.VARIO.name()) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GPS.ordinal()] = HoTTAdapter.Sensor.getSensorNames(HoTTAdapter.isSensorType).contains(HoTTAdapter.Sensor.GPS.name()) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GAM.ordinal()] = HoTTAdapter.Sensor.getSensorNames(HoTTAdapter.isSensorType).contains(HoTTAdapter.Sensor.GAM.name()) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_EAM.ordinal()] = HoTTAdapter.Sensor.getSensorNames(HoTTAdapter.isSensorType).contains(HoTTAdapter.Sensor.EAM.name()) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_ESC.ordinal()] = HoTTAdapter.Sensor.getSensorNames(HoTTAdapter.isSensorType).contains(HoTTAdapter.Sensor.ESC.name()) ? 1000 : 0;
			scores[ScoreLabelTypes.LOG_DATA_VERSION.ordinal()] = (int) (HoTTbinHistoReader.filePath.getFileName().toString().startsWith(GDE.TEMP_FILE_STEM.substring(0, 1)) ? 4.2 * 1000 : 4.0 * 1000); // V4 with and without container
			scores[ScoreLabelTypes.LOG_DATA_EXPLORER_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_FILE_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal()] = histoRandomSample.getReadingCount() * HoTTbinHistoReader.dataBlockSize;
			scores[ScoreLabelTypes.LOG_FILE_BYTES.ordinal()] = (int) fileLength;
			scores[ScoreLabelTypes.LOG_FILE_RECORD_SETS.ordinal()] = HoTTAdapter.Sensor.getSensorNames(HoTTAdapter.isSensorType).size() * 1000;
			scores[ScoreLabelTypes.ELAPSED_HISTO_RECORD_SET_MS.ordinal()] = (int) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - nanoTime); // do not multiply by 1000 as usual, this is the conversion from microseconds to ms
			// no display tmpRecordSet.syncScaleOfSyncableRecords();

			tmpRecordSet.setSaved(true);
			device.makeInActiveDisplayable(tmpRecordSet);
			device.updateVisibilityStatus(tmpRecordSet, true);
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("%s > packages:%,9d  readings:%,9d  sampled:%,9d  overSampled:%4d", tmpRecordSet.getChannelConfigName(), fileLength //$NON-NLS-1$
					/ HoTTbinHistoReader.dataBlockSize, histoRandomSample.getReadingCount(), tmpRecordSet.getRecordDataSize(true), histoRandomSample.getOverSamplingCount()));
			HoTTbinHistoReader.setTimeMarks(TimeMark.FINISHED);
			if (log.isLoggable(Level.TIME)) log.log(Level.TIME,
					String.format("initiateTime: %,7d  readTime: %,7d  reviewTime: %,7d  addTime: %,7d  pickTime: %,7d  finishTime: %,7d", TimeUnit.NANOSECONDS.toMillis(initiateTime), //$NON-NLS-1$
							TimeUnit.NANOSECONDS.toMillis(readTime), TimeUnit.NANOSECONDS.toMillis(reviewTime), TimeUnit.NANOSECONDS.toMillis(addTime), TimeUnit.NANOSECONDS.toMillis(pickTime),
							TimeUnit.NANOSECONDS.toMillis(finishTime)));
			if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
				if (tmpRecordSet.getMaxTime_ms() > 0) {
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE,
							String.format("lost:%,9d perMille:%,4d total:%,9d   lostMax_ms:%,4d lostAvg_ms=%,4d", countPackageLoss, //$NON-NLS-1$
									(int) (countPackageLoss / tmpRecordSet.getMaxTime_ms() * 1000. * recordTimespan_ms), fileLength / HoTTbinHistoReader.dataBlockSize, HoTTbinReader.lostPackages.getMaxValue() * 10,
									(int) HoTTbinReader.lostPackages.getAvgValue() * 10));
				}
				else {
					log.log(Level.WARNING, String.format("RecordSet with unidentified data.  fileLength=%,11d   isTextModusSignaled=%b", fileLength, HoTTbinHistoReader.isTextModusSignaled)); //$NON-NLS-1$
				}
			}

			truss.promoteTruss(tmpRecordSet, scores);
			// reduce memory consumption in advance to the garbage collection
			tmpRecordSet.cleanup();
		}
		else {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("doFullRead=%b > ends", doFullRead)); //$NON-NLS-1$
		}

		HoTTAdapter.isFilterEnabled = isFilterEnabledPre; //reset filter value to user set
		return histoRandomSample;
	}

	/**
	* read log data according to version 0 either in initialize mode for learning min/max values or in fully functional read mode.
	* reads only sample records and allocates only one single recordset for the active channel, so HoTTAdapter.isChannelsChannelEnabled does not take effect.
	* no progress bar support and no channel data modifications.
	* @param data_in
	* @param initializeBlocks if this number is greater than zero, the min/max values are initialized
	* @param maxPoints utilizes the minMax values from a previous run and thus reduces oversampling
	* @param minPoints utilizes the minMax values from a previous run and thus reduces oversampling
	* @throws IOException
	* @throws DataInconsitsentException
	*/
	private static HistoRandomSample readMultiple(InputStream data_in, int initializeBlocks, int[] maxPoints, int[] minPoints) throws IOException, DataInconsitsentException {
		final String $METHOD_NAME = "readMultiple"; //$NON-NLS-1$
		HoTTbinHistoReader.recordSetReceiver = null; // 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=UminRx
		HoTTbinHistoReader.recordSetGAM = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		HoTTbinHistoReader.recordSetEAM = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2, 27=Revolution
		HoTTbinHistoReader.recordSetVario = null; // 0=RXSQ, 1=Height, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
		HoTTbinHistoReader.recordSetGPS = null; // 0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		HoTTbinHistoReader.recordSetChannel = null; // 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16
		HoTTbinHistoReader.recordSetESC = null; // 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature
		HoTTbinHistoReader.pointsReceiver = new int[9];
		HoTTbinHistoReader.pointsGAM = new int[21];
		HoTTbinHistoReader.pointsEAM = new int[28];
		HoTTbinHistoReader.pointsVario = new int[7];
		HoTTbinHistoReader.pointsVario[2] = 100000;
		HoTTbinHistoReader.pointsGPS = new int[12];
		HoTTbinHistoReader.pointsChannel = new int[23];
		HoTTbinHistoReader.pointsESC = new int[13];
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
		boolean isFilterEnabledPre = HoTTAdapter.isFilterEnabled;
		HoTTAdapter.isFilterEnabled = true;
		HoTTbinHistoReader.isJustParsed = false;
		HoTTbinHistoReader.isTextModusSignaled = false;
		// HoTTbinHistoReader.oldProtocolCount = 0;
		// HoTTbinHistoReader.blockSequenceCheck = new Vector<Byte>();
		int countPackageLoss = 0;
		HistoRandomSample histoRandomSample = null;
		HoTTAdapter device = (HoTTAdapter) HoTTbinHistoReader.application.getActiveDevice();
		int activeChannelNumber = device.channels.getActiveChannelNumber(); // HoTTbinHistoReader.application.getActiveChannel().getNumber();
		if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
			HoTTbinHistoReader.recordSetReceiver = tmpRecordSet;
			histoRandomSample = HistoRandomSample.createHistoRandomSample(activeChannelNumber, HoTTbinHistoReader.pointsReceiver.length, recordTimespan_ms);
		}
		else if (activeChannelNumber == HoTTAdapter.Sensor.CHANNEL.ordinal() + 1) {
			HoTTbinHistoReader.recordSetChannel = tmpRecordSet;
			histoRandomSample = HistoRandomSample.createHistoRandomSample(activeChannelNumber, HoTTbinHistoReader.pointsChannel.length, recordTimespan_ms);
		}
		else if (activeChannelNumber == HoTTAdapter.Sensor.VARIO.ordinal() + 1) {
			HoTTbinHistoReader.recordSetVario = tmpRecordSet;
			histoRandomSample = HistoRandomSample.createHistoRandomSample(activeChannelNumber, HoTTbinHistoReader.pointsVario.length, recordTimespan_ms);
		}
		else if (activeChannelNumber == HoTTAdapter.Sensor.GPS.ordinal() + 1) {
			HoTTbinHistoReader.recordSetGPS = tmpRecordSet;
			histoRandomSample = HistoRandomSample.createHistoRandomSample(activeChannelNumber, HoTTbinHistoReader.pointsGPS.length, recordTimespan_ms);
		}
		else if (activeChannelNumber == HoTTAdapter.Sensor.GAM.ordinal() + 1) {
			HoTTbinHistoReader.recordSetGAM = tmpRecordSet;
			histoRandomSample = HistoRandomSample.createHistoRandomSample(activeChannelNumber, HoTTbinHistoReader.pointsGAM.length, recordTimespan_ms);
		}
		else if (activeChannelNumber == HoTTAdapter.Sensor.EAM.ordinal() + 1) {
			HoTTbinHistoReader.recordSetEAM = tmpRecordSet;
			histoRandomSample = HistoRandomSample.createHistoRandomSample(activeChannelNumber, HoTTbinHistoReader.pointsEAM.length, recordTimespan_ms);
		}
		else if (activeChannelNumber == HoTTAdapter.Sensor.ESC.ordinal() + 1) {
			HoTTbinHistoReader.recordSetESC = tmpRecordSet;
			histoRandomSample = HistoRandomSample.createHistoRandomSample(activeChannelNumber, HoTTbinHistoReader.pointsESC.length, recordTimespan_ms);
		}
		else
			throw new UnsupportedOperationException();

		histoRandomSample.setMaxPoints(maxPoints);
		histoRandomSample.setMinPoints(minPoints);
		// read all the data blocks from the file, parse only for the active channel
		setTimeMarks(TimeMark.INITIATED);
		boolean doFullRead = initializeBlocks <= 0;
		long fileLength = HoTTbinHistoReader.filePath.toFile().length();
		int datablocksLimit = (doFullRead ? (int) fileLength / HoTTbinHistoReader.dataBlockSize : initializeBlocks);
		for (int i = 0; i < datablocksLimit; i++) {
			data_in.read(HoTTbinHistoReader.buf);
			if (HoTTbinHistoReader.log.isLoggable(Level.FINEST) && i % 10 == 0) {
				HoTTbinHistoReader.log.logp(Level.FINEST, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinHistoReader.buf.length));
				HoTTbinHistoReader.log.logp(Level.FINEST, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinHistoReader.buf, HoTTbinHistoReader.buf.length));
			}

			if (!HoTTAdapter.isFilterTextModus || (HoTTbinHistoReader.buf[6] & 0x01) == 0) { // switch into text modus
				if (HoTTbinHistoReader.buf[33] >= 0 && HoTTbinHistoReader.buf[33] <= 4 && HoTTbinHistoReader.buf[3] != 0 && HoTTbinHistoReader.buf[4] != 0) { // buf 3, 4, tx,rx
					if (HoTTbinHistoReader.log.isLoggable(Level.FINE)) HoTTbinHistoReader.log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", HoTTbinHistoReader.buf[7], HoTTbinHistoReader.buf[33])); //$NON-NLS-1$
					if (HoTTbinHistoReader.log.isLoggable(Level.FINEST)) HoTTbinHistoReader.log.logp(Level.FINEST, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME,
							StringHelper.byte2Hex2CharString(new byte[] { HoTTbinHistoReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinHistoReader.buf[7], false));

					if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
						HoTTAdapter.reverseChannelPackageLossCounter.add(1);
						HoTTbinHistoReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
						if (HoTTbinHistoReader.buf[33] == 0 && (HoTTbinHistoReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinHistoReader.buf, 40) >= 0) {
							parse4Receiver(HoTTbinHistoReader.buf);
							setTimeMarks(TimeMark.READ);
							boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsReceiver, HoTTbinHistoReader.timeStep_ms);
							setTimeMarks(TimeMark.REVIEWED);
							if (isValidSample && doFullRead) {
								HoTTbinHistoReader.recordSetReceiver.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
								setTimeMarks(TimeMark.ADDED);
								setTimeMarks(TimeMark.PICKED);
							}
						}
					}
					else if (activeChannelNumber == HoTTAdapter.Sensor.CHANNEL.ordinal() + 1) {
						parse4Channel(HoTTbinHistoReader.buf);
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsChannel, HoTTbinHistoReader.timeStep_ms);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							HoTTbinHistoReader.recordSetChannel.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}
					}

					HoTTbinHistoReader.timeStep_ms += recordTimespan_ms;

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
										setTimeMarks(TimeMark.READ);
										boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsVario, HoTTbinHistoReader.timeStep_ms);
										setTimeMarks(TimeMark.REVIEWED);
										if (isValidSample && doFullRead) {
											HoTTbinHistoReader.recordSetVario.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											setTimeMarks(TimeMark.ADDED);
											setTimeMarks(TimeMark.PICKED);
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
										setTimeMarks(TimeMark.READ);
										boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsGPS, HoTTbinHistoReader.timeStep_ms);
										setTimeMarks(TimeMark.REVIEWED);
										if (isValidSample && doFullRead) {
											HoTTbinHistoReader.recordSetGPS.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											setTimeMarks(TimeMark.ADDED);
											setTimeMarks(TimeMark.PICKED);
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
										setTimeMarks(TimeMark.READ);
										boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsGAM, HoTTbinHistoReader.timeStep_ms);
										setTimeMarks(TimeMark.REVIEWED);
										if (isValidSample && doFullRead) {
											HoTTbinHistoReader.recordSetGAM.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											setTimeMarks(TimeMark.ADDED);
											setTimeMarks(TimeMark.PICKED);
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
										setTimeMarks(TimeMark.READ);
										boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsEAM, HoTTbinHistoReader.timeStep_ms);
										setTimeMarks(TimeMark.REVIEWED);
										if (isValidSample && doFullRead) {
											HoTTbinHistoReader.recordSetEAM.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											setTimeMarks(TimeMark.ADDED);
											setTimeMarks(TimeMark.PICKED);
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
										setTimeMarks(TimeMark.READ);
										boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsESC, HoTTbinHistoReader.timeStep_ms);
										setTimeMarks(TimeMark.REVIEWED);
										if (isValidSample && doFullRead) {
											HoTTbinHistoReader.recordSetESC.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											setTimeMarks(TimeMark.ADDED);
											setTimeMarks(TimeMark.PICKED);
										}
									}
									HoTTbinHistoReader.isJustParsed = true;
								}
								break;
							}
						}

						if (HoTTbinHistoReader.log.isLoggable(Level.FINE)) HoTTbinHistoReader.log.logp(Level.FINE, HoTTbinHistoReader.$CLASS_NAME, $METHOD_NAME, "logCountVario = " + logCountVario //$NON-NLS-1$
								+ " logCountGPS = " + logCountGPS + " logCountGeneral = " + logCountGeneral + " logCountElectric = " + logCountElectric + " logCountMotorDriver = " + logCountSpeedControl); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
					}
					else if (HoTTbinHistoReader.buf[33] == 1) {
						System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf1, 0, HoTTbinHistoReader.buf1.length);
					}
					else if (HoTTbinHistoReader.buf[33] == 2) {
						System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf2, 0, HoTTbinHistoReader.buf2.length);
					}
					else if (HoTTbinHistoReader.buf[33] == 3) {
						System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf3, 0, HoTTbinHistoReader.buf3.length);
					}
					else if (HoTTbinHistoReader.buf[33] == 4) {
						System.arraycopy(HoTTbinHistoReader.buf, 34, HoTTbinHistoReader.buf4, 0, HoTTbinHistoReader.buf4.length);
					}

					// if (HoTTbinHistoReader.blockSequenceCheck.size() > 1) {
					// if(HoTTbinHistoReader.blockSequenceCheck.get(0) - HoTTbinHistoReader.blockSequenceCheck.get(1) > 1 && HoTTbinHistoReader.blockSequenceCheck.get(0) - HoTTbinHistoReader.blockSequenceCheck.get(1) < 4)
					// ++HoTTbinHistoReader.oldProtocolCount;
					// HoTTbinHistoReader.blockSequenceCheck.remove(0);
					// }
					// HoTTbinHistoReader.blockSequenceCheck.add(HoTTbinHistoReader.buf[33]);

					if (HoTTbinHistoReader.isJustParsed && HoTTbinHistoReader.countLostPackages > 0) {
						if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) HoTTbinHistoReader.lostPackages.add(HoTTbinHistoReader.countLostPackages);
						HoTTbinHistoReader.countLostPackages = 0;
						HoTTbinHistoReader.isJustParsed = false;
					}
				}
				else

				{ // tx,rx == 0
					if (HoTTbinHistoReader.log.isLoggable(Level.FINE)) HoTTbinHistoReader.log.log(Level.FINE, "-->> Found tx=rx=0 dBm"); //$NON-NLS-1$

					if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
						HoTTAdapter.reverseChannelPackageLossCounter.add(0);
						HoTTbinHistoReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
						++countPackageLoss; // add up lost packages in telemetry data
						++HoTTbinHistoReader.countLostPackages;
						// HoTTbinHistoReader.pointsReceiver[0] = (int) (countPackageLoss*100.0 / ((HoTTbinHistoReader.timeStep_ms+10) / 10.0)*1000.0);
					}

					boolean isChannelsChannel = activeChannelNumber == HoTTAdapter.Sensor.CHANNEL.ordinal() + 1;
					if (isChannelsChannel) {
						HoTTbinHistoReader.parse4Channel(HoTTbinHistoReader.buf);
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader.pointsChannel, HoTTbinHistoReader.timeStep_ms);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							HoTTbinHistoReader.recordSetChannel.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}
					}

					HoTTbinHistoReader.timeStep_ms += recordTimespan_ms;
					// reset buffer to avoid mixing data >> 20 Jul 14, not any longer required due to protocol change requesting next sensor data block
					// HoTTbinHistoReader.buf1 = HoTTbinHistoReader.buf2 = HoTTbinHistoReader.buf3 = HoTTbinHistoReader.buf4 = null;
				}
			}
			else if (!HoTTbinHistoReader.isTextModusSignaled) {
				HoTTbinHistoReader.isTextModusSignaled = true;
			}
		}
		// if (HoTTbinHistoReader.oldProtocolCount > 2) {
		// application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2405, new Object[] { HoTTbinHistoReader.oldProtocolCount }));
		// }
		if (doFullRead) {
			final Integer[] scores = new Integer[ScoreLabelTypes.values.length];
			// values are multiplied by 1000 as this is the convention for internal values in order to avoid rounding errors for values below 1.0 (0.5 -> 0)
			// scores for duration and timestep values are filled in by the HistoVault
			scores[ScoreLabelTypes.TOTAL_READINGS.ordinal()] = histoRandomSample.getReadingCount();
			scores[ScoreLabelTypes.TOTAL_PACKAGES.ordinal()] = (int) fileLength / HoTTbinHistoReader.dataBlockSize;
			scores[ScoreLabelTypes.LOST_PACKAGES.ordinal()] = countPackageLoss;
			scores[ScoreLabelTypes.LOST_PACKAGES_PER_MILLE.ordinal()] = tmpRecordSet.getMaxTime_ms() > 0 ? (int) (countPackageLoss / tmpRecordSet.getMaxTime_ms() * 1000. * recordTimespan_ms) * 1000 : 0;
			scores[ScoreLabelTypes.LOST_PACKAGES_AVG_MS.ordinal()] = (int) HoTTbinReader.lostPackages.getAvgValue() * recordTimespan_ms * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_MAX_MS.ordinal()] = HoTTbinReader.lostPackages.getMaxValue() * recordTimespan_ms * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_MIN_MS.ordinal()] = HoTTbinReader.lostPackages.getMinValue() * recordTimespan_ms * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_SIGMA_MS.ordinal()] = (int) HoTTbinReader.lostPackages.getSigmaValue() * recordTimespan_ms * 1000;
			scores[ScoreLabelTypes.SENSORS.ordinal()] = HoTTAdapter.Sensor.getSensorNames(HoTTAdapter.isSensorType).size() * 1000; // do not subtract Channel / Receiver --- is always false
			scores[ScoreLabelTypes.SENSOR_VARIO.ordinal()] = HoTTAdapter.Sensor.getSensorNames(HoTTAdapter.isSensorType).contains(HoTTAdapter.Sensor.VARIO.name()) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GPS.ordinal()] = HoTTAdapter.Sensor.getSensorNames(HoTTAdapter.isSensorType).contains(HoTTAdapter.Sensor.GPS.name()) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GAM.ordinal()] = HoTTAdapter.Sensor.getSensorNames(HoTTAdapter.isSensorType).contains(HoTTAdapter.Sensor.GAM.name()) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_EAM.ordinal()] = HoTTAdapter.Sensor.getSensorNames(HoTTAdapter.isSensorType).contains(HoTTAdapter.Sensor.EAM.name()) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_ESC.ordinal()] = HoTTAdapter.Sensor.getSensorNames(HoTTAdapter.isSensorType).contains(HoTTAdapter.Sensor.ESC.name()) ? 1000 : 0;
			scores[ScoreLabelTypes.LOG_DATA_VERSION.ordinal()] = (int) (HoTTbinHistoReader.filePath.getFileName().toString().startsWith(GDE.TEMP_FILE_STEM.substring(0, 1)) ? 4.2 * 1000 : 4.0 * 1000); // V4 with and without container
			scores[ScoreLabelTypes.LOG_DATA_EXPLORER_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_FILE_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal()] = histoRandomSample.getReadingCount() * HoTTbinHistoReader.dataBlockSize;
			scores[ScoreLabelTypes.LOG_FILE_BYTES.ordinal()] = (int) fileLength;
			scores[ScoreLabelTypes.LOG_FILE_RECORD_SETS.ordinal()] = HoTTAdapter.Sensor.values.length;
			scores[ScoreLabelTypes.ELAPSED_HISTO_RECORD_SET_MS.ordinal()] = (int) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - nanoTime); // do not multiply by 1000 as usual, this is the conversion from microseconds to ms
			// no display tmpRecordSet.syncScaleOfSyncableRecords();

			tmpRecordSet.setSaved(true);
			device.makeInActiveDisplayable(tmpRecordSet);
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("%s > packages:%,9d  readings:%,9d  sampled:%,9d  overSampled:%4d", tmpRecordSet.getChannelConfigName(), fileLength //$NON-NLS-1$
					/ HoTTbinHistoReader.dataBlockSize, histoRandomSample.getReadingCount(), tmpRecordSet.getRecordDataSize(true), histoRandomSample.getOverSamplingCount()));
			HoTTbinHistoReader.setTimeMarks(TimeMark.FINISHED);
			if (log.isLoggable(Level.TIME)) log.log(Level.TIME,
					String.format("initiateTime: %,7d  readTime: %,7d  reviewTime: %,7d  addTime: %,7d  pickTime: %,7d  finishTime: %,7d", TimeUnit.NANOSECONDS.toMillis(initiateTime), //$NON-NLS-1$
							TimeUnit.NANOSECONDS.toMillis(readTime), TimeUnit.NANOSECONDS.toMillis(reviewTime), TimeUnit.NANOSECONDS.toMillis(addTime), TimeUnit.NANOSECONDS.toMillis(pickTime),
							TimeUnit.NANOSECONDS.toMillis(finishTime)));
			if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE,
						String.format("lost:%,9d perMille:%,4d total:%,9d   lostMax_ms:%,4d lostAvg_ms=%,4d", countPackageLoss, //$NON-NLS-1$
								(int) (countPackageLoss / tmpRecordSet.getTime_ms((int) fileLength / HoTTbinHistoReader.dataBlockSize - 1) * 1000. * recordTimespan_ms), fileLength / HoTTbinHistoReader.dataBlockSize,
								HoTTbinReader.lostPackages.getMaxValue() * 10, (int) HoTTbinReader.lostPackages.getAvgValue() * 10));
			}

			truss.promoteTruss(tmpRecordSet, scores);
			// reduce memory consumption in advance to the garbage collection
			tmpRecordSet.cleanup();
		}
		else {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("doFullRead=%b > ends", doFullRead)); //$NON-NLS-1$
		}

		HoTTAdapter.isFilterEnabled = isFilterEnabledPre; //reset filter value to user set
		return histoRandomSample;
	}

	private static void setTimeMarks(TimeMark mark) {
		HoTTbinHistoReader.currentTime = System.nanoTime();
		if (mark == TimeMark.INITIATED) {
			HoTTbinHistoReader.initiateTime += HoTTbinHistoReader.currentTime - HoTTbinHistoReader.lastTime;
		}
		else if (mark == TimeMark.READ) {
			HoTTbinHistoReader.readTime += HoTTbinHistoReader.currentTime - HoTTbinHistoReader.lastTime;
		}
		else if (mark == TimeMark.REVIEWED) {
			HoTTbinHistoReader.reviewTime += HoTTbinHistoReader.currentTime - HoTTbinHistoReader.lastTime;
		}
		else if (mark == TimeMark.ADDED) {
			HoTTbinHistoReader.addTime += HoTTbinHistoReader.currentTime - HoTTbinHistoReader.lastTime;
		}
		else if (mark == TimeMark.PICKED) {
			HoTTbinHistoReader.pickTime += HoTTbinHistoReader.currentTime - HoTTbinHistoReader.lastTime;
		}
		else if (mark == TimeMark.FINISHED) {
			HoTTbinHistoReader.finishTime += HoTTbinHistoReader.currentTime - HoTTbinHistoReader.lastTime;
		}
		HoTTbinHistoReader.lastTime = HoTTbinHistoReader.currentTime;
	}
}
