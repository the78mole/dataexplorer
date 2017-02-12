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
import gde.histocache.HistoVault;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.utils.StringHelper;

/**
 * read Graupner HoTT binary data for history analysis.
 * provide data in a histo record set.
 * for small files (around 1 minute) no measurements are added to the record set. 
 * reads one single channel only.
 * supports sampling to maximize the throughput.
 */
public class HoTTbinHistoReader2 extends HoTTbinReader2 {
	final private static String	$CLASS_NAME	= HoTTbinHistoReader2.class.getName();
	final private static Logger	log					= Logger.getLogger(HoTTbinHistoReader2.$CLASS_NAME);

	private static enum TimeMark {
		INITIATED, ADDED, PICKED, READ, REVIEWED, FINISHED
	};

	private static int				recordTimespan_ms	= 10;																																		// HoTT logs data rate defined by the channel log
	private static long				nanoTime;
	private static long				currentTime, initiateTime, readTime, reviewTime, addTime, pickTime, finishTime, lastTime;
	private static Path				filePath;
	private static RecordSet	tmpRecordSet;
	private static HistoVault	truss;

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
	public static synchronized void read(HistoVault newTruss) throws IOException, DataTypeException, DataInconsitsentException {
		HoTTbinHistoReader2.nanoTime = System.nanoTime();
		HoTTbinHistoReader2.initiateTime = HoTTbinHistoReader2.readTime = HoTTbinHistoReader2.reviewTime = HoTTbinHistoReader2.addTime = HoTTbinHistoReader2.pickTime = HoTTbinHistoReader2.finishTime = 0;
		HoTTbinHistoReader2.lastTime = System.nanoTime();

		HoTTbinHistoReader2.truss = newTruss;
		HoTTbinHistoReader2.filePath = Paths.get(truss.getLogFilePath());
		File file = HoTTbinHistoReader2.filePath.toFile();
		try (BufferedInputStream data_in = new BufferedInputStream(new FileInputStream(file))) {
			HoTTbinHistoReader2.read(data_in);
		}
		catch (InvalidObjectException e) {
			// so any anther exception is propagated to the caller
		}
		finally {
			if (HoTTbinHistoReader2.filePath.getFileName().startsWith("~") && file.exists()) file.delete(); // 'GRAUPNER SD LOG8'
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
		final String $METHOD_NAME = "read";
		File file = HoTTbinHistoReader2.filePath.toFile();
		HashMap<String, String> header = null;
		HoTTAdapter2 device = (HoTTAdapter2) HoTTbinHistoReader2.application.getActiveDevice();
		boolean isChannelsChannel = device.channels.getActiveChannelNumber() == HoTTAdapter.Sensor.CHANNEL.ordinal() + 1; // instead of HoTTAdapter setting
		long numberDatablocks = file.length() / HoTTbinReader.dataBlockSize / (HoTTbinReader.isReceiverOnly && !isChannelsChannel ? 10 : 1);
		tmpRecordSet = RecordSet.createRecordSet(truss.getLogRecordsetBaseName(), device, HoTTbinHistoReader2.application.getActiveChannelNumber(), true, true, false);
		tmpRecordSet.setStartTimeStamp(HoTTbinReader.getStartTimeStamp(file, numberDatablocks));
		tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", tmpRecordSet.getStartTimeStamp())); //$NON-NLS-1$
		tmpRecordSet.descriptionAppendFilename(HoTTbinHistoReader2.filePath.getFileName().toString());
		if (HoTTbinHistoReader2.log.isLoggable(Level.FINE)) HoTTbinHistoReader2.log.logp(Level.FINE, HoTTbinHistoReader2.$CLASS_NAME, $METHOD_NAME, " recordSetBaseName=" + truss.getLogRecordsetBaseName());

		if (file.length() > NUMBER_LOG_RECORDS_MIN * dataBlockSize) {
			try {
				header = HoTTbinReader2.getFileInfo(HoTTbinHistoReader2.filePath.toFile());
			}
			catch (DataTypeException e) {
				HoTTbinHistoReader2.log.log(Level.WARNING, String.format("%s  %s", e.getMessage(), HoTTbinHistoReader2.filePath)); // 'GRAUPNER SD LOG8'
			}
			if (header != null && header.size() > 0 && HoTTAdapter.Sensor.getChannelNumbers(HoTTAdapter.isSensorType).contains(truss.getVaultChannelNumber())) {
				if (!header.get(HoTTAdapter.FILE_PATH).equals(HoTTbinHistoReader2.filePath.toString())) {
					// accept this 'GRAUPNER SD LOG8' file, extracted file starts with '~'
					HoTTbinHistoReader2.filePath = Paths.get(header.get(HoTTAdapter.FILE_PATH));
				}
				int readLimitMark = (LOG_RECORD_SCAN_START + NUMBER_LOG_RECORDS_TO_SCAN) * dataBlockSize + 1;
				if (Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) <= 1) {
					HoTTbinHistoReader2.isReceiverOnly = Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) == 0;
					data_in.mark(readLimitMark); // reduces # of overscan records from 57 to 23 (to 38 with 1500 blocks)
					HistoRandomSample histoRandomSample = HoTTbinHistoReader2.readSingle(data_in, LOG_RECORD_SCAN_START + NUMBER_LOG_RECORDS_TO_SCAN, new int[0], new int[0]);
					data_in.reset();
					HoTTbinHistoReader2.readSingle(data_in, -1, histoRandomSample.getMaxPoints(), histoRandomSample.getMinPoints());
				}
				else {
					data_in.mark(readLimitMark);
					HistoRandomSample histoRandomSample = HoTTbinHistoReader2.readMultiple(data_in, LOG_RECORD_SCAN_START + NUMBER_LOG_RECORDS_TO_SCAN, new int[0], new int[0]);
					data_in.reset();
					HoTTbinHistoReader2.readMultiple(data_in, -1, histoRandomSample.getMaxPoints(), histoRandomSample.getMinPoints());
				}
			}
		}
	}

	/**
	* read log data according to version 0.
	* allocates only one single record set for the active channel
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
		final String $METHOD_NAME = "readSingle";
		HoTTAdapter2 device = (HoTTAdapter2) HoTTbinHistoReader2.application.getActiveDevice();
		int activeChannelNumber = device.channels.getActiveChannelNumber(); 
		boolean isReceiverData = false;
		boolean isSensorData = false;
		HoTTbinReader2.recordSet = tmpRecordSet;
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
		HoTTbinReader2.points = new int[device.getNumberOfMeasurements(activeChannelNumber)];
		HoTTbinReader2.points[2] = 0;
		HoTTbinReader.pointsGeneral = HoTTbinReader.pointsElectric = HoTTbinReader.pointsSpeedControl = HoTTbinReader.pointsVario = HoTTbinReader.pointsGPS = HoTTbinReader2.points;
		HoTTbinHistoReader2.timeStep_ms = 0;
		HoTTbinHistoReader2.buf = new byte[HoTTbinHistoReader2.dataBlockSize];
		HoTTbinHistoReader2.buf0 = new byte[30];
		HoTTbinHistoReader2.buf1 = null;
		HoTTbinHistoReader2.buf2 = null;
		HoTTbinHistoReader2.buf3 = null;
		HoTTbinHistoReader2.buf4 = null;
		HoTTAdapter.reverseChannelPackageLossCounter.clear();
		HoTTbinHistoReader2.lostPackages.clear();
		HoTTbinHistoReader2.countLostPackages = 0;
		HoTTbinHistoReader2.isJustParsed = false;
		int countPackageLoss = 0;
		HistoRandomSample histoRandomSample = HistoRandomSample.createHistoRandomSample(activeChannelNumber, maxPoints, minPoints,  recordTimespan_ms);

		histoRandomSample.setMaxPoints(maxPoints);
		histoRandomSample.setMinPoints(minPoints);
		// read all the data blocks from the file, parse only for the active channel
		setTimeMarks(TimeMark.INITIATED);
		boolean isChannelsChannel = activeChannelNumber == 4;
		long fileLength = HoTTbinHistoReader2.filePath.toFile().length();
		boolean doFullRead = initializeBlocks <= 0;
		int datablocksLimit = (doFullRead ? (int) fileLength / HoTTbinHistoReader2.dataBlockSize : initializeBlocks) / (HoTTbinHistoReader2.isReceiverOnly && !isChannelsChannel ? 10 : 1);
		for (int i = 0; i < datablocksLimit; i++) {
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
					if (activeChannelNumber == 4) parseChannel(HoTTbinReader.buf); //Channels

					//fill data block 0 receiver voltage an temperature
					if (HoTTbinReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader.buf, 0) != 0) {
						System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf0, 0, HoTTbinReader.buf0.length);
					}
					if (HoTTbinReader.isReceiverOnly && activeChannelNumber != 4) { //reduce data rate for receiver to 0.1 sec
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
								parseESC(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, activeChannelNumber);
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

					if (isSensorData || (isReceiverData && tmpRecordSet.get(0).realSize() > 0)) {
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader2.points, HoTTbinHistoReader.timeStep_ms);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(HoTTbinReader2.points, HoTTbinReader.timeStep_ms);
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}
						isSensorData = isReceiverData = false;
					}
					else if (activeChannelNumber == 4) {
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader2.points, HoTTbinHistoReader.timeStep_ms);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(HoTTbinReader2.points, HoTTbinReader.timeStep_ms);
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}
					}

					HoTTbinReader.timeStep_ms += 10; // add default time step from device of 10 msec
				}
				else { //skip empty block, but add time step
					if (HoTTbinReader2.logger.isLoggable(Level.FINE)) HoTTbinReader2.logger.log(Level.FINE, "-->> Found tx=rx=0 dBm");

					HoTTAdapter.reverseChannelPackageLossCounter.add(0);
					HoTTbinReader2.points[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;

					++countPackageLoss; // add up lost packages in telemetry data 
					++HoTTbinReader.countLostPackages;
					//HoTTbinReader2.points[0] = (int) (countPackageLoss*100.0 / ((HoTTbinReader2.timeStep_ms+10) / 10.0)*1000.0); 

					if (activeChannelNumber == 4) {
						parseChannel(HoTTbinReader.buf); //Channels
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader2.points, HoTTbinHistoReader.timeStep_ms);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(HoTTbinReader2.points, HoTTbinReader.timeStep_ms);
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}
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
		if (doFullRead) {
			final Integer[] scores = new Integer[ScoreLabelTypes.values.length];
			// values are multiplied by 1000 as this is the convention for internal values in order to avoid rounding errors for values below 1.0 (0.5 -> 0)
			// scores for duration and timestep values are filled in by the HistoVault
			scores[ScoreLabelTypes.TOTAL_READINGS.ordinal()] = histoRandomSample.getReadingCount();
			scores[ScoreLabelTypes.TOTAL_PACKAGES.ordinal()] = (int) fileLength / HoTTbinHistoReader2.dataBlockSize;
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
			scores[ScoreLabelTypes.LOG_DATA_VERSION.ordinal()] = (int) (HoTTbinHistoReader2.filePath.getFileName().toString().startsWith(GDE.TEMP_FILE_STEM.substring(0, 1)) ? 4.2 * 1000 : 4.0 * 1000); // V4 with and without container
			scores[ScoreLabelTypes.LOG_DATA_EXPLORER_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_FILE_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal()] = histoRandomSample.getReadingCount() * HoTTbinHistoReader2.dataBlockSize;
			scores[ScoreLabelTypes.LOG_FILE_BYTES.ordinal()] = (int) fileLength;
			scores[ScoreLabelTypes.LOG_FILE_RECORD_SETS.ordinal()] = HoTTAdapter.Sensor.getSensorNames(HoTTAdapter.isSensorType).size() * 1000;
			scores[ScoreLabelTypes.ELAPSED_HISTO_RECORD_SET_MS.ordinal()] = (int) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - nanoTime); // do not multiply by 1000 as usual, this is the conversion from microseconds to ms
			// no display tmpRecordSet.syncScaleOfSyncableRecords();

			tmpRecordSet.setSaved(true);
			device.makeInActiveDisplayable(tmpRecordSet);
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("%s > packages:%,9d  readings:%,9d  sampled:%,9d  overSampled:%4d", tmpRecordSet.getChannelConfigName(), fileLength //$NON-NLS-1$
					/ HoTTbinHistoReader2.dataBlockSize, histoRandomSample.getReadingCount(), tmpRecordSet.getRecordDataSize(true), histoRandomSample.getOverSamplingCount()));
			HoTTbinHistoReader2.setTimeMarks(TimeMark.FINISHED);
			if (log.isLoggable(Level.TIME)) log.log(Level.TIME,
					String.format("initiateTime: %,7d  readTime: %,7d  reviewTime: %,7d  addTime: %,7d  pickTime: %,7d  finishTime: %,7d", TimeUnit.NANOSECONDS.toMillis(initiateTime), //$NON-NLS-1$
							TimeUnit.NANOSECONDS.toMillis(readTime), TimeUnit.NANOSECONDS.toMillis(reviewTime), TimeUnit.NANOSECONDS.toMillis(addTime), TimeUnit.NANOSECONDS.toMillis(pickTime),
							TimeUnit.NANOSECONDS.toMillis(finishTime)));
			if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
				if (tmpRecordSet.getMaxTime_ms() > 0) {
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE,
							String.format("lost:%,9d perMille:%,4d total:%,9d   lostMax_ms:%,4d lostAvg_ms=%,4d", countPackageLoss, //$NON-NLS-1$
									(int) (countPackageLoss / tmpRecordSet.getMaxTime_ms() * 1000. * recordTimespan_ms), fileLength / HoTTbinHistoReader2.dataBlockSize, HoTTbinReader.lostPackages.getMaxValue() * 10,
									(int) HoTTbinReader.lostPackages.getAvgValue() * 10));
				}
				else {
					log.log(Level.WARNING, String.format("RecordSet with unidentified data.  fileLength=%,11d   isTextModusSignaled=%b", fileLength, HoTTbinHistoReader2.isTextModusSignaled));
				}
			}

			truss.promoteTruss(tmpRecordSet, scores);
			// reduce memory consumption in advance to the garbage collection
			tmpRecordSet.cleanup();
		}
		else {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("doFullRead=%b > ends", doFullRead));
		}
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
		final String $METHOD_NAME = "readMultiple";
		HoTTAdapter2 device = (HoTTAdapter2) HoTTbinReader.application.getActiveDevice();
		int activeChannelNumber = device.channels.getActiveChannelNumber(); 
		boolean isReceiverData = false;
		boolean isVarioData = false;
		boolean isGPSData = false;
		boolean isGAMData = false;
		boolean isEAMData = false;
		boolean isESCData = false;
		HoTTbinReader2.isJustMigrated = false;
		HoTTbinHistoReader2.recordSet = tmpRecordSet;
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
		HoTTbinReader2.points = new int[device.getNumberOfMeasurements(activeChannelNumber)];
		HoTTbinReader.pointsGeneral = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsElectric = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsSpeedControl = new int[HoTTbinReader2.points.length];
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
		HoTTbinHistoReader2.lostPackages.clear();
		HoTTbinHistoReader2.countLostPackages = 0;
		HoTTbinHistoReader2.isJustParsed = false;
		HoTTbinHistoReader2.isTextModusSignaled = false;
		int countPackageLoss = 0;
		HistoRandomSample histoRandomSample = HistoRandomSample.createHistoRandomSample(activeChannelNumber, maxPoints, minPoints,  recordTimespan_ms);

		histoRandomSample.setMaxPoints(maxPoints);
		histoRandomSample.setMinPoints(minPoints);
		// read all the data blocks from the file, parse only for the active channel
		setTimeMarks(TimeMark.INITIATED);
		boolean doFullRead = initializeBlocks <= 0;
		int initializeBlockLimit = initializeBlocks > 0 ? initializeBlocks : Integer.MAX_VALUE;
		long fileLength = HoTTbinHistoReader2.filePath.toFile().length();
		for (int i = 0; i < initializeBlockLimit && i < fileLength / HoTTbinHistoReader2.dataBlockSize; i++) {
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
					if (activeChannelNumber == 4) parseChannel(HoTTbinReader.buf);

					if (actualSensor == -1)
						lastSensor = actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);
					else
						actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);

					if (actualSensor != lastSensor) {
						if (logCountVario >= 3 || logCountGPS >= 4 || logCountGAM >= 5 || logCountEAM >= 5 || logCountESC >= 4) {
							//							System.out.println();
							switch (lastSensor) {
							case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
							case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
								if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.VARIO.ordinal()]) {
									if (isVarioData && isReceiverData) {
										migrateAddPoints(isVarioData, isGPSData, isGAMData, isEAMData, isESCData, activeChannelNumber);
										//System.out.println("isVarioData i = " + i);
										isReceiverData = isVarioData = isGPSData = isGAMData = isEAMData = isESCData = false;
									}
									parseVario(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2);
									isVarioData = true;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GPS_115200:
							case HoTTAdapter.SENSOR_TYPE_GPS_19200:
								if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GPS.ordinal()]) {
									if (isGPSData && isReceiverData) {
										migrateAddPoints(isVarioData, isGPSData, isGAMData, isEAMData, isESCData, activeChannelNumber);
										//System.out.println("isGPSData i = " + i);
										isReceiverData = isVarioData = isGPSData = isGAMData = isEAMData = isESCData = false;
									}
									parseGPS(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3);
									isGPSData = true;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
							case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
								if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()]) {
									if (isGAMData && isReceiverData) {
										migrateAddPoints(isVarioData, isGPSData, isGAMData, isEAMData, isESCData, activeChannelNumber);
										//System.out.println("isGeneralData i = " + i);
										isReceiverData = isVarioData = isGPSData = isGAMData = isEAMData = false;
									}
									parseGAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
									isGAMData = true;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
								if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.EAM.ordinal()]) {
									if (isEAMData && isReceiverData) {
										migrateAddPoints(isVarioData, isGPSData, isGAMData, isEAMData, isESCData, activeChannelNumber);
										//System.out.println("isElectricData i = " + i);
										isReceiverData = isVarioData = isGPSData = isGAMData = isEAMData = isESCData = false;
									}
									parseEAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
									isEAMData = true;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
								if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()]) {
									if (isESCData && isReceiverData) {
										migrateAddPoints(isVarioData, isGPSData, isGAMData, isEAMData, isESCData, activeChannelNumber);
										//System.out.println("isElectricData i = " + i);
										isReceiverData = isVarioData = isGPSData = isGAMData = isEAMData = isESCData = false;
									}
									parseESC(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, activeChannelNumber);
									isESCData = true;
								}
								break;
							}

							if (HoTTbinReader2.logger.isLoggable(Level.FINE))
								HoTTbinReader2.logger.log(Level.FINE, "isReceiverData " + isReceiverData + " isVarioData " + isVarioData + " isGPSData " + isGPSData + " isGeneralData "
										+ isGAMData + " isElectricData " + isEAMData);

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
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader2.points, HoTTbinHistoReader.timeStep_ms);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}

						isReceiverData = false;
					}
					else if (activeChannelNumber == 4 && !HoTTbinReader2.isJustMigrated) {
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader2.points, HoTTbinHistoReader.timeStep_ms);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}
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
				}
				else { //skip empty block, but add time step
					if (HoTTbinReader2.logger.isLoggable(Level.FINE)) HoTTbinReader2.logger.log(Level.FINE, "-->> Found tx=rx=0 dBm");

					HoTTAdapter.reverseChannelPackageLossCounter.add(0);
					HoTTbinReader2.points[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;

					++countPackageLoss; // add up lost packages in telemetry data 
					++HoTTbinReader.countLostPackages;
					//HoTTbinReader2.points[0] = (int) (countPackageLoss*100.0 / ((HoTTbinReader2.timeStep_ms+10) / 10.0)*1000.0); 

					if (activeChannelNumber == 4) {
						parseChannel(HoTTbinReader.buf); //Channels
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.isValidSample(HoTTbinHistoReader2.points, HoTTbinHistoReader.timeStep_ms);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}
					}
					HoTTbinReader.timeStep_ms += 10;
				}
			}
			else if (!HoTTbinReader.isTextModusSignaled) {
				HoTTbinReader.isTextModusSignaled = true;
				HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
			}
		}
		if (doFullRead) {
			final Integer[] scores = new Integer[ScoreLabelTypes.values.length];
			// values are multiplied by 1000 as this is the convention for internal values in order to avoid rounding errors for values below 1.0 (0.5 -> 0)
			// scores for duration and timestep values are filled in by the HistoVault
			scores[ScoreLabelTypes.TOTAL_READINGS.ordinal()] = histoRandomSample.getReadingCount();
			scores[ScoreLabelTypes.TOTAL_PACKAGES.ordinal()] = (int) fileLength / HoTTbinHistoReader2.dataBlockSize;
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
			scores[ScoreLabelTypes.LOG_DATA_VERSION.ordinal()] = (int) (HoTTbinHistoReader2.filePath.getFileName().toString().startsWith(GDE.TEMP_FILE_STEM.substring(0, 1)) ? 4.2 * 1000 : 4.0 * 1000); // V4 with and without container
			scores[ScoreLabelTypes.LOG_DATA_EXPLORER_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_FILE_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal()] = histoRandomSample.getReadingCount() * HoTTbinHistoReader2.dataBlockSize;
			scores[ScoreLabelTypes.LOG_FILE_BYTES.ordinal()] = (int) fileLength;
			scores[ScoreLabelTypes.LOG_FILE_RECORD_SETS.ordinal()] = HoTTAdapter.Sensor.values.length;
			scores[ScoreLabelTypes.ELAPSED_HISTO_RECORD_SET_MS.ordinal()] = (int) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - nanoTime); // do not multiply by 1000 as usual, this is the conversion from microseconds to ms
			// no display tmpRecordSet.syncScaleOfSyncableRecords();

			tmpRecordSet.setSaved(true);
			device.makeInActiveDisplayable(tmpRecordSet);
			device.updateVisibilityStatus(tmpRecordSet, true);
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("%s > packages:%,9d  readings:%,9d  sampled:%,9d  overSampled:%4d", tmpRecordSet.getChannelConfigName(), fileLength //$NON-NLS-1$
					/ HoTTbinHistoReader2.dataBlockSize, histoRandomSample.getReadingCount(), tmpRecordSet.getRecordDataSize(true), histoRandomSample.getOverSamplingCount()));
			HoTTbinHistoReader2.setTimeMarks(TimeMark.FINISHED);
			if (log.isLoggable(Level.TIME)) log.log(Level.TIME,
					String.format("initiateTime: %,7d  readTime: %,7d  reviewTime: %,7d  addTime: %,7d  pickTime: %,7d  finishTime: %,7d", TimeUnit.NANOSECONDS.toMillis(initiateTime), //$NON-NLS-1$
							TimeUnit.NANOSECONDS.toMillis(readTime), TimeUnit.NANOSECONDS.toMillis(reviewTime), TimeUnit.NANOSECONDS.toMillis(addTime), TimeUnit.NANOSECONDS.toMillis(pickTime),
							TimeUnit.NANOSECONDS.toMillis(finishTime)));
			if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.ordinal() + 1) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE,
						String.format("lost:%,9d perMille:%,4d total:%,9d   lostMax_ms:%,4d lostAvg_ms=%,4d", countPackageLoss, //$NON-NLS-1$
								(int) (countPackageLoss / tmpRecordSet.getTime_ms((int) fileLength / HoTTbinHistoReader2.dataBlockSize - 1) * 1000. * recordTimespan_ms), fileLength / HoTTbinHistoReader2.dataBlockSize,
								HoTTbinReader.lostPackages.getMaxValue() * 10, (int) HoTTbinReader.lostPackages.getAvgValue() * 10));
			}

			truss.promoteTruss(tmpRecordSet, scores);
			// reduce memory consumption in advance to the garbage collection
			tmpRecordSet.cleanup();
		}
		else {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("doFullRead=%b > ends", doFullRead));
		}
		return histoRandomSample;
	}

	private static void setTimeMarks(TimeMark mark) {
		HoTTbinHistoReader2.currentTime = System.nanoTime();
		if (mark == TimeMark.INITIATED) {
			HoTTbinHistoReader2.initiateTime += HoTTbinHistoReader2.currentTime - HoTTbinHistoReader2.lastTime;
		}
		else if (mark == TimeMark.READ) {
			HoTTbinHistoReader2.readTime += HoTTbinHistoReader2.currentTime - HoTTbinHistoReader2.lastTime;
		}
		else if (mark == TimeMark.REVIEWED) {
			HoTTbinHistoReader2.reviewTime += HoTTbinHistoReader2.currentTime - HoTTbinHistoReader2.lastTime;
		}
		else if (mark == TimeMark.ADDED) {
			HoTTbinHistoReader2.addTime += HoTTbinHistoReader2.currentTime - HoTTbinHistoReader2.lastTime;
		}
		else if (mark == TimeMark.PICKED) {
			HoTTbinHistoReader2.pickTime += HoTTbinHistoReader2.currentTime - HoTTbinHistoReader2.lastTime;
		}
		else if (mark == TimeMark.FINISHED) {
			HoTTbinHistoReader2.finishTime += HoTTbinHistoReader2.currentTime - HoTTbinHistoReader2.lastTime;
		}
		HoTTbinHistoReader2.lastTime = HoTTbinHistoReader2.currentTime;
	}
}
