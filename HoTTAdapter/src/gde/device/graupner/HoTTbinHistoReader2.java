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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import gde.Analyzer;
import gde.GDE;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.ScoreLabelTypes;
import gde.device.graupner.HoTTAdapter.PickerParameters;
import gde.device.graupner.HoTTAdapter.Sensor;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.histo.cache.VaultCollector;
import gde.histo.device.UniversalSampler;
import gde.io.DataParser;
import gde.log.Level;
import gde.utils.StringHelper;

/**
 * read Graupner HoTT binary data for history analysis.
 * provide data in a histo record set.
 * for small files (around 1 minute) no measurements are added to the record set.
 * reads one single channel only.
 * supports sampling to maximize the throughput.
 */
public class HoTTbinHistoReader2 extends HoTTbinReader2 {
	@SuppressWarnings("hiding")
	final private static String	$CLASS_NAME	= HoTTbinHistoReader2.class.getName();
	@SuppressWarnings("hiding")
	final private static Logger	log					= Logger.getLogger(HoTTbinHistoReader2.$CLASS_NAME);

	private static enum TimeMark {
		INITIATED, ADDED, PICKED, READ, REVIEWED, FINISHED
	};

	protected static EnumSet<Sensor>	detectedSensors;
	protected static PickerParameters pickerParameters;
	private static int						recordTimespan_ms	= 10;																																		// HoTT logs data rate defined by the channel log
	private static long						nanoTime;
	private static long						currentTime, initiateTime, readTime, reviewTime, addTime, pickTime, finishTime, lastTime;
	private static RecordSet			tmpRecordSet;
	private static VaultCollector	truss;

	@Deprecated // shadows the base class method
	public static synchronized void read(@SuppressWarnings("hiding") String filePath) throws Exception {
	}

	@Deprecated // shadows the base class method
	static void readSingle(File file) throws IOException, DataInconsitsentException {
	}

	@Deprecated // shadows the base class method
	static void readMultiple(File file) throws IOException, DataInconsitsentException {
	}

	/**
	 * @param inputStream for retrieving the file info and for loading the log data
	 * @param newTruss which is promoted to a full vault object if the file has a minimum length.
	 * @throws IOException
	 * @throws DataTypeException
	 * @throws DataInconsitsentException
	 */
	public static synchronized void read(Supplier<InputStream> inputStream, VaultCollector newTruss, PickerParameters pickerParameters) throws IOException, DataTypeException, DataInconsitsentException {
		if (newTruss.getVault().getLogFileLength() <= NUMBER_LOG_RECORDS_MIN * dataBlockSize ) return;

		HoTTbinHistoReader2.pickerParameters = pickerParameters;
		HoTTbinHistoReader2.nanoTime = System.nanoTime();
		HoTTbinHistoReader2.initiateTime = HoTTbinHistoReader2.readTime = HoTTbinHistoReader2.reviewTime = HoTTbinHistoReader2.addTime = HoTTbinHistoReader2.pickTime = HoTTbinHistoReader2.finishTime = 0;
		HoTTbinHistoReader2.lastTime = System.nanoTime();

		HoTTbinHistoReader2.truss = newTruss;
		try {
			HoTTbinHistoReader2.read(inputStream);
		} catch (InvalidObjectException e) {
			// so any anther exception is propagated to the caller
		}
	}

	/**
	 * read file.
	 * provide data in a histo tmpRecordSet.
	 */
	private static synchronized void read(Supplier<InputStream> inputStream) throws IOException, DataTypeException, DataInconsitsentException {
		HashMap<String, String> header = null;
		IDevice device = Analyzer.getInstance().getActiveDevice();
		boolean isChannelsChannel = Analyzer.getInstance().getActiveChannel().getNumber() == HoTTAdapter.Sensor.CHANNEL.getChannelNumber(); // instead of HoTTAdapter setting
		long numberDatablocks = HoTTbinHistoReader2.truss.getVault().getLogFileLength() / HoTTbinReader.dataBlockSize / (HoTTbinReader.isReceiverOnly && !isChannelsChannel ? 10 : 1);
		tmpRecordSet = RecordSet.createRecordSet(HoTTbinHistoReader2.truss.getVault().getLogRecordsetBaseName(), Analyzer.getInstance(), HoTTbinHistoReader2.application.getActiveChannelNumber(), true, true, false);
		tmpRecordSet.setStartTimeStamp(HoTTbinReader.getStartTimeStamp(truss.getVault().getLoadFileAsPath().getFileName().toString(), truss.getVault().getLogFileLastModified(), numberDatablocks));
		tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", tmpRecordSet.getStartTimeStamp())); //$NON-NLS-1$
		tmpRecordSet.descriptionAppendFilename(HoTTbinHistoReader2.truss.getVault().getLoadFileAsPath().getFileName().toString());
		if (HoTTbinHistoReader2.log.isLoggable(Level.FINE))
			HoTTbinHistoReader2.log.log(Level.FINE, " recordSetBaseName=" + HoTTbinHistoReader2.truss.getVault().getLogRecordsetBaseName());

		HoTTbinReader.pickerParameters = pickerParameters;
		try (BufferedInputStream info_in = new BufferedInputStream(inputStream.get())) {
			header = new InfoParser((s) -> {} ).getFileInfo(info_in, truss.getVault().getLoadFilePath(), truss.getVault().getLogFileLength());
		} catch (DataTypeException e) {
			HoTTbinHistoReader2.log.log(Level.WARNING, String.format("%s  %s", e.getMessage(), HoTTbinHistoReader2.truss.getVault().getLoadFilePath())); // 'GRAUPNER SD LOG8'
		}

		if (header == null || header.isEmpty()) return;
		detectedSensors = Sensor.getSetFromDetected(header.get(HoTTAdapter.DETECTED_SENSOR));
		if (!Sensor.getChannelNumbers(detectedSensors).contains(truss.getVault().getVaultChannelNumber())) return;

		EnumSet<Sensor> detectedSensors = EnumSet.noneOf(Sensor.class); // todo
		detectedSensors.addAll(Arrays.stream(header.get(HoTTAdapter.DETECTED_SENSOR).split(GDE.STRING_COMMA)).map((s) -> Sensor.fromDetectedName(s)).collect(Collectors.toSet()));
		try (BufferedInputStream in = new BufferedInputStream(inputStream.get()); //
				InputStream data_in = Boolean.parseBoolean(header.get(HoTTAdapter.SD_FORMAT)) //
						? new SdLogInputStream(in, truss.getVault().getLogFileLength(), new SdLogFormat(HoTTbinReaderX.headerSize, HoTTbinReaderX.footerSize, 64)) //
								: in; ) {
			int readLimitMark = (LOG_RECORD_SCAN_START + NUMBER_LOG_RECORDS_TO_SCAN) * dataBlockSize + 1;
			if (detectedSensors.size() <= 2) {
				HoTTbinHistoReader2.isReceiverOnly = detectedSensors.size() == 1;
				data_in.mark(readLimitMark); // reduces # of overscan records from 57 to 23 (to 38 with 1500 blocks)
				UniversalSampler histoRandomSample = HoTTbinHistoReader2.readSingle(data_in, LOG_RECORD_SCAN_START + NUMBER_LOG_RECORDS_TO_SCAN, new int[0], new int[0]);
				data_in.reset();
				HoTTbinHistoReader2.readSingle(data_in, -1, histoRandomSample.getMaxPoints(), histoRandomSample.getMinPoints());
			} else {
				data_in.mark(readLimitMark);
				UniversalSampler histoRandomSample = HoTTbinHistoReader2.readMultiple(data_in, LOG_RECORD_SCAN_START + NUMBER_LOG_RECORDS_TO_SCAN, new int[0], new int[0]);
				data_in.reset();
				HoTTbinHistoReader2.readMultiple(data_in, -1, histoRandomSample.getMaxPoints(), histoRandomSample.getMinPoints());
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
	 * @return the sampler object holding the last value sets and the min/max value sets
	 */
	private static UniversalSampler readSingle(InputStream data_in, int initializeBlocks, int[] maxPoints, int[] minPoints) throws DataInconsitsentException, IOException {
		IDevice device = Analyzer.getInstance().getActiveDevice();
		int activeChannelNumber = Analyzer.getInstance().getActiveChannel().getNumber();
		boolean isReceiverData = false;
		boolean isSensorData = false;
		boolean isFilterEnabledPre = pickerParameters.isFilterEnabled;
		pickerParameters.isFilterEnabled = true;
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
		HoTTbinReader.pointsGAM = HoTTbinReader.pointsEAM = HoTTbinReader.pointsESC = HoTTbinReader.pointsVario = HoTTbinReader.pointsGPS = HoTTbinReader2.points;
		HoTTbinHistoReader2.timeStep_ms = 0;
		HoTTbinHistoReader2.buf = new byte[HoTTbinHistoReader2.dataBlockSize];
		HoTTbinReader.buf0 = new byte[30];
		HoTTbinReader.buf1 = new byte[30];
		HoTTbinReader.buf2 = new byte[30];
		HoTTbinReader.buf3 = new byte[30];
		HoTTbinReader.buf4 = new byte[30];
		long[] timeSteps_ms = new long[] { 0 };
		HoTTbinHistoReader2.isTextModusSignaled = false;

		rcvBinParser = Sensor.RECEIVER.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
		chnBinParser = Sensor.CHANNEL.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
		varBinParser = Sensor.VARIO.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2 });
		gpsBinParser = Sensor.GPS.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3 });
		gamBinParser = Sensor.GAM.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		eamBinParser = Sensor.EAM.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		escBinParser = Sensor.ESC.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3 });
		UniversalSampler histoRandomSample = UniversalSampler.createSampler(activeChannelNumber, points, HoTTbinHistoReader.RECORD_TIMESPAN_MS, pickerParameters.analyzer);
		histoRandomSample.setMaxMinPoints(maxPoints, minPoints);

		// read all the data blocks from the file, parse only for the active channel
		setTimeMarks(TimeMark.INITIATED);
		boolean isChannelsChannel = activeChannelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER;
		boolean doFullRead = initializeBlocks <= 0;
		int datablocksLimit = (doFullRead ? (int) HoTTbinHistoReader2.truss.getVault().getLogFileLength() / HoTTbinHistoReader2.dataBlockSize : initializeBlocks) / (HoTTbinHistoReader2.isReceiverOnly && !isChannelsChannel ? 10 : 1);
		for (int i = 0; i < datablocksLimit; i++) {
			data_in.read(HoTTbinReader.buf);
			if (HoTTbinHistoReader2.log.isLoggable(Level.FINE) && i % 10 == 0) {
				HoTTbinHistoReader2.log.log(Level.FINE, StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
				HoTTbinHistoReader2.log.log(Level.FINE, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
			}

			if (!pickerParameters.isFilterTextModus || (HoTTbinReader.buf[6] & 0x01) == 0) { //switch into text modus
				if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { //buf 3, 4, tx,rx
					if (HoTTbinHistoReader2.log.isLoggable(Level.FINE)) HoTTbinHistoReader2.log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", HoTTbinReader.buf[7], HoTTbinReader.buf[33]));

					((RcvBinParser) HoTTbinReader.rcvBinParser).trackPackageLoss(true);
					if (HoTTbinHistoReader2.log.isLoggable(Level.FINER)) HoTTbinHistoReader2.log.log(Level.FINER,
							StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

					//fill receiver data
					if (HoTTbinReader.buf[33] == 0 && (HoTTbinReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader.buf, 40) >= 0) {
						HoTTbinReader2.rcvBinParser.parse();
						isReceiverData = true;
					}
					if (isChannelsChannel) {
						HoTTbinReader2.chnBinParser.parse();
					}

					//fill data block 0 receiver voltage an temperature
					if (HoTTbinReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader.buf, 0) != 0) {
						System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf0, 0, HoTTbinReader.buf0.length);
					}
					if (HoTTbinReader.isReceiverOnly && !isChannelsChannel) { //reduce data rate for receiver to 0.1 sec
						for (int j = 0; j < 9; j++) {
							data_in.read(HoTTbinReader.buf);
							timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
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
								HoTTbinReader2.varBinParser.parse();
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
								HoTTbinReader2.gpsBinParser.parse();
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
								HoTTbinReader2.gamBinParser.parse();
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
								HoTTbinReader2.eamBinParser.parse();
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
								HoTTbinReader2.escBinParser.parse();
								HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
								isSensorData = true;
							}
						}
						break;
					}

					if (isSensorData) {
						((RcvBinParser) HoTTbinReader.rcvBinParser).updateLossStatistics();
					}

					if (isSensorData || (isReceiverData && tmpRecordSet.get(0).realSize() > 0)) {
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.capturePoints(timeSteps_ms[BinParser.TIMESTEP_INDEX]);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}
						isSensorData = isReceiverData = false;
					} else if (isChannelsChannel) {
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.capturePoints(timeSteps_ms[BinParser.TIMESTEP_INDEX]);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}
					}

					timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10; // add default time step from device of 10 msec
				} else { //skip empty block, but add time step
					if (HoTTbinHistoReader2.log.isLoggable(Level.FINE)) HoTTbinHistoReader2.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

					((RcvBinParser) HoTTbinReader.rcvBinParser).trackPackageLoss(false);

					if (isChannelsChannel) {
						HoTTbinReader2.chnBinParser.parse(); //Channels
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.capturePoints(timeSteps_ms[BinParser.TIMESTEP_INDEX]);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}
					}
					timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
					//reset buffer to avoid mixing data >> 20 Jul 14, not any longer required due to protocol change requesting next sensor data block
					//HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
				}
			} else if (!HoTTbinReader.isTextModusSignaled) {
				HoTTbinReader.isTextModusSignaled = true;
			}
		}
		if (doFullRead) {
			final Integer[] scores = new Integer[ScoreLabelTypes.VALUES.length];
			// values are multiplied by 1000 as this is the convention for internal values in order to avoid rounding errors for values below 1.0 (0.5 -> 0)
			// scores for duration and timestep values are filled in by the HistoVault
			scores[ScoreLabelTypes.TOTAL_READINGS.ordinal()] = histoRandomSample.getReadingCount();
			scores[ScoreLabelTypes.TOTAL_PACKAGES.ordinal()] = (int) HoTTbinHistoReader2.truss.getVault().getLogFileLength() / HoTTbinHistoReader2.dataBlockSize;
			scores[ScoreLabelTypes.LOST_PACKAGES.ordinal()] = ((RcvBinParser) rcvBinParser).getCountPackageLoss();
			scores[ScoreLabelTypes.LOST_PACKAGES_PER_MILLE.ordinal()] = tmpRecordSet.getMaxTime_ms() > 0 ? (int) (((RcvBinParser) rcvBinParser).getCountPackageLoss() / tmpRecordSet.getMaxTime_ms() * 1000. * recordTimespan_ms) * 1000 : 0;
			scores[ScoreLabelTypes.LOST_PACKAGES_AVG_MS.ordinal()] = (int) HoTTbinReader.lostPackages.getAvgValue() * recordTimespan_ms * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_MAX_MS.ordinal()] = HoTTbinReader.lostPackages.getMaxValue() * recordTimespan_ms * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_MIN_MS.ordinal()] = HoTTbinReader.lostPackages.getMinValue() * recordTimespan_ms * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_SIGMA_MS.ordinal()] = (int) HoTTbinReader.lostPackages.getSigmaValue() * recordTimespan_ms * 1000;
			scores[ScoreLabelTypes.SENSORS.ordinal()] = (detectedSensors.size() - 1) * 1000; // subtract Receiver, do not subtract Channel --- is not included
			scores[ScoreLabelTypes.SENSOR_VARIO.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.VARIO) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GPS.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.GPS) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GAM.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.GAM) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_EAM.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.EAM) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_ESC.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.ESC) ? 1000 : 0;
			scores[ScoreLabelTypes.LOG_DATA_VERSION.ordinal()] = (int) (HoTTbinHistoReader2.truss.getVault().getLoadFileAsPath().getFileName().toString().startsWith(GDE.TEMP_FILE_STEM.substring(0, 1)) ? 4.2 * 1000 : 4.0 * 1000); // V4 with and without container
			scores[ScoreLabelTypes.LOG_DATA_EXPLORER_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_FILE_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal()] = histoRandomSample.getReadingCount() * HoTTbinHistoReader2.dataBlockSize;
			scores[ScoreLabelTypes.LOG_FILE_BYTES.ordinal()] = (int) HoTTbinHistoReader2.truss.getVault().getLogFileLength();
			scores[ScoreLabelTypes.LOG_FILE_RECORD_SETS.ordinal()] = (detectedSensors.size() + 1) * 1000; // +1 for channel
			scores[ScoreLabelTypes.ELAPSED_HISTO_RECORD_SET_MS.ordinal()] = (int) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - nanoTime); // do not multiply by 1000 as usual, this is the conversion from microseconds to ms
			// no display tmpRecordSet.syncScaleOfSyncableRecords();

			tmpRecordSet.setSaved(true);
			((HoTTAdapter2) device).calculateInactiveRecords(tmpRecordSet);
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("%s > packages:%,9d  readings:%,9d  sampled:%,9d  overSampled:%4d", tmpRecordSet.getChannelConfigName(), HoTTbinHistoReader2.truss.getVault().getLogFileLength() //$NON-NLS-1$
					/ HoTTbinHistoReader2.dataBlockSize, histoRandomSample.getReadingCount(), tmpRecordSet.getRecordDataSize(true), histoRandomSample.getOverSamplingCount()));
			HoTTbinHistoReader2.setTimeMarks(TimeMark.FINISHED);
			if (log.isLoggable(Level.TIME)) log.log(Level.TIME,
					String.format("initiateTime: %,7d  readTime: %,7d  reviewTime: %,7d  addTime: %,7d  pickTime: %,7d  finishTime: %,7d", TimeUnit.NANOSECONDS.toMillis(initiateTime), //$NON-NLS-1$
							TimeUnit.NANOSECONDS.toMillis(readTime), TimeUnit.NANOSECONDS.toMillis(reviewTime), TimeUnit.NANOSECONDS.toMillis(addTime), TimeUnit.NANOSECONDS.toMillis(pickTime),
							TimeUnit.NANOSECONDS.toMillis(finishTime)));
			if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.getChannelNumber()) {
				if (tmpRecordSet.getMaxTime_ms() > 0) {
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE,
							String.format("lost:%,9d perMille:%,4d total:%,9d   lostMax_ms:%,4d lostAvg_ms=%,4d", ((RcvBinParser) HoTTbinReader.rcvBinParser).getCountPackageLoss(), //$NON-NLS-1$
									(int) (((RcvBinParser) HoTTbinReader.rcvBinParser).getCountPackageLoss() / tmpRecordSet.getMaxTime_ms() * 1000. * recordTimespan_ms), HoTTbinHistoReader2.truss.getVault().getLogFileLength() / HoTTbinHistoReader2.dataBlockSize, HoTTbinReader.lostPackages.getMaxValue() * 10,
									(int) HoTTbinReader.lostPackages.getAvgValue() * 10));
				} else {
					log.log(Level.WARNING, String.format("RecordSet with unidentified data.  fileLength=%,11d   isTextModusSignaled=%b", HoTTbinHistoReader2.truss.getVault().getLogFileLength(), HoTTbinHistoReader2.isTextModusSignaled));
				}
			}

			truss.promoteTruss(tmpRecordSet, scores);
			// reduce memory consumption in advance to the garbage collection
			tmpRecordSet.cleanup();
		} else {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("doFullRead=%b > ends", doFullRead));
		}

		pickerParameters.isFilterEnabled = isFilterEnabledPre; //reset filter value to user set
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
	 * @return the sampler object holding the last value sets and the min/max value sets
	 * @throws IOException
	 * @throws DataInconsitsentException
	 */
	private static UniversalSampler readMultiple(InputStream data_in, int initializeBlocks, int[] maxPoints, int[] minPoints) throws IOException, DataInconsitsentException {
		IDevice device = Analyzer.getInstance().getActiveDevice();
		int activeChannelNumber = Analyzer.getInstance().getActiveChannel().getNumber();
		boolean isReceiverData = false;
		BinParser varMigrationJob = null;
		BinParser gpsMigrationJob = null;
		BinParser gamMigrationJob = null;
		BinParser eamMigrationJob = null;
		BinParser escMigrationJob = null;
		boolean isFilterEnabledPre = pickerParameters.isFilterEnabled;
		pickerParameters.isFilterEnabled = true;
		boolean isJustMigrated = false;
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
		HoTTbinReader.pointsGAM = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsEAM = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsESC = new int[HoTTbinReader2.points.length];
		HoTTbinReader.pointsVario = new int[HoTTbinReader2.points.length];
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
		long[] timeSteps_ms = new long[] { 0 };
		HoTTbinHistoReader2.isTextModusSignaled = false;

		rcvBinParser = Sensor.RECEIVER.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
		chnBinParser = Sensor.CHANNEL.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
		varBinParser = Sensor.VARIO.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2 });
		gpsBinParser = Sensor.GPS.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3 });
		gamBinParser = Sensor.GAM.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		eamBinParser = Sensor.EAM.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		escBinParser = Sensor.ESC.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3 });
		UniversalSampler histoRandomSample = UniversalSampler.createSampler(activeChannelNumber, points, HoTTbinHistoReader.RECORD_TIMESPAN_MS, pickerParameters.analyzer);
		histoRandomSample.setMaxMinPoints(maxPoints, minPoints);

		// read all the data blocks from the file, parse only for the active channel
		setTimeMarks(TimeMark.INITIATED);
		boolean isChannelsChannel = activeChannelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER;
		tmpRecordSet.cleanup();
		boolean doFullRead = initializeBlocks <= 0;
		int initializeBlockLimit = initializeBlocks > 0 ? initializeBlocks : Integer.MAX_VALUE;
		for (int i = 0; i < initializeBlockLimit && i < HoTTbinHistoReader2.truss.getVault().getLogFileLength() / HoTTbinHistoReader2.dataBlockSize; i++) {
			data_in.read(HoTTbinReader.buf);
			if (HoTTbinHistoReader2.log.isLoggable(Level.FINEST)) {
				HoTTbinHistoReader2.log.log(Level.FINEST, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
			}

			if (!pickerParameters.isFilterTextModus || (HoTTbinReader.buf[6] & 0x01) == 0) { //switch into text modus
				if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { //buf 3, 4, tx,rx
					if (HoTTbinHistoReader2.log.isLoggable(Level.FINE)) HoTTbinHistoReader2.log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", HoTTbinReader.buf[7], HoTTbinReader.buf[33]));

					((RcvBinParser) HoTTbinReader.rcvBinParser).trackPackageLoss(true);
					if (HoTTbinHistoReader2.log.isLoggable(Level.FINEST)) HoTTbinHistoReader2.log.log(Level.FINEST,
							StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

					//fill receiver data
					if (HoTTbinReader.buf[33] == 0 && (HoTTbinReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader.buf, 40) >= 0) {
						HoTTbinReader2.rcvBinParser.parse();
						isReceiverData = true;
					}
					if (isChannelsChannel) {
						HoTTbinReader2.chnBinParser.parse();
					}

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
								if (detectedSensors.contains(Sensor.VARIO)) {
									if (varMigrationJob != null && isReceiverData) {
										migrateAddPoints(varMigrationJob, gpsMigrationJob, gamMigrationJob, eamMigrationJob, escMigrationJob, timeSteps_ms[BinParser.TIMESTEP_INDEX], doFullRead);
										isJustMigrated = true;
										isReceiverData = false;
										varMigrationJob = gpsMigrationJob = gamMigrationJob = eamMigrationJob = escMigrationJob = null;
									}
									HoTTbinReader2.varBinParser.parse();
									varMigrationJob = varBinParser;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GPS_115200:
							case HoTTAdapter.SENSOR_TYPE_GPS_19200:
								if (detectedSensors.contains(Sensor.GPS)) {
									if (gpsMigrationJob != null && isReceiverData) {
										migrateAddPoints(varMigrationJob, gpsMigrationJob, gamMigrationJob, eamMigrationJob, escMigrationJob, timeSteps_ms[BinParser.TIMESTEP_INDEX], doFullRead);
										isJustMigrated = true;
										isReceiverData = false;
										varMigrationJob = gpsMigrationJob = gamMigrationJob = eamMigrationJob = escMigrationJob = null;
									}
									HoTTbinReader2.gpsBinParser.parse();
									gpsMigrationJob = gpsBinParser;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
							case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
								if (detectedSensors.contains(Sensor.GAM)) {
									if (gamMigrationJob != null && isReceiverData) {
										migrateAddPoints(varMigrationJob, gpsMigrationJob, gamMigrationJob, eamMigrationJob, escMigrationJob, timeSteps_ms[BinParser.TIMESTEP_INDEX], doFullRead);
										isJustMigrated = true;
										isReceiverData = false;
										varMigrationJob = gpsMigrationJob = gamMigrationJob = eamMigrationJob = escMigrationJob = null;
									}
									HoTTbinReader2.gamBinParser.parse();
									gamMigrationJob = gamBinParser;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
								if (detectedSensors.contains(Sensor.EAM)) {
									if (eamMigrationJob != null && isReceiverData) {
										migrateAddPoints(varMigrationJob, gpsMigrationJob, gamMigrationJob, eamMigrationJob, escMigrationJob, timeSteps_ms[BinParser.TIMESTEP_INDEX], doFullRead);
										isJustMigrated = true;
										isReceiverData = false;
										varMigrationJob = gpsMigrationJob = gamMigrationJob = eamMigrationJob = escMigrationJob = null;
									}
									HoTTbinReader2.eamBinParser.parse();
									eamMigrationJob = eamBinParser;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
								if (detectedSensors.contains(Sensor.ESC)) {
									if (escMigrationJob != null && isReceiverData) {
										migrateAddPoints(varMigrationJob, gpsMigrationJob, gamMigrationJob, eamMigrationJob, escMigrationJob, timeSteps_ms[BinParser.TIMESTEP_INDEX], doFullRead);
										isJustMigrated = true;
										isReceiverData = false;
										varMigrationJob = gpsMigrationJob = gamMigrationJob = eamMigrationJob = escMigrationJob = null;
									}
									HoTTbinReader2.escBinParser.parse();
									escMigrationJob = escBinParser;
								}
								break;
							}

							if (HoTTbinReader2.log.isLoggable(Level.FINE)) HoTTbinReader2.log.log(Level.FINE, "isReceiverData " + isReceiverData + " isVarioData " + varMigrationJob //
									+ " isGPSData " + gpsMigrationJob + " isGeneralData " + gamMigrationJob + " isElectricData " + eamMigrationJob + " isEscData " + escMigrationJob);
						}

						if (HoTTbinHistoReader2.log.isLoggable(Level.FINE)) HoTTbinHistoReader2.log.log(Level.FINE,
								"logCountVario = " + logCountVario + " logCountGPS = " + logCountGPS + " logCountGeneral = " + logCountGAM + " logCountElectric = " + logCountEAM);
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
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.capturePoints(HoTTbinReader2.timeStep_ms);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							//System.out.println("samp: " + histoRandomSample.getSamplePoints()[74] + " - " + histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}

						isReceiverData = false;
					} else if (activeChannelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER && !isJustMigrated) {
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.capturePoints(HoTTbinReader2.timeStep_ms);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}
					}
					isJustMigrated = false;

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

					timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;// add default time step from log record of 10 msec
				} else { //skip empty block, but add time step
					if (HoTTbinHistoReader2.log.isLoggable(Level.FINE)) HoTTbinHistoReader2.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

					((RcvBinParser) HoTTbinReader.rcvBinParser).trackPackageLoss(false);
					if (activeChannelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER) {
						HoTTbinReader2.chnBinParser.parse();
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.capturePoints(HoTTbinReader2.timeStep_ms);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}
					}
					timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
				}
			} else if (!HoTTbinReader.isTextModusSignaled) {
				HoTTbinReader.isTextModusSignaled = true;
			}
		}

		if (doFullRead) {
			final Integer[] scores = new Integer[ScoreLabelTypes.VALUES.length];
			// values are multiplied by 1000 as this is the convention for internal values in order to avoid rounding errors for values below 1.0 (0.5 -> 0)
			// scores for duration and timestep values are filled in by the HistoVault
			scores[ScoreLabelTypes.TOTAL_READINGS.ordinal()] = histoRandomSample.getReadingCount();
			scores[ScoreLabelTypes.TOTAL_PACKAGES.ordinal()] = (int) HoTTbinHistoReader2.truss.getVault().getLogFileLength() / HoTTbinHistoReader2.dataBlockSize;
			scores[ScoreLabelTypes.LOST_PACKAGES.ordinal()] = ((RcvBinParser) rcvBinParser).getCountPackageLoss();
			scores[ScoreLabelTypes.LOST_PACKAGES_PER_MILLE.ordinal()] = tmpRecordSet.getMaxTime_ms() > 0 ? (int) (((RcvBinParser) rcvBinParser).getCountPackageLoss() / tmpRecordSet.getMaxTime_ms() * 1000. * recordTimespan_ms) * 1000 : 0;
			scores[ScoreLabelTypes.LOST_PACKAGES_AVG_MS.ordinal()] = (int) HoTTbinReader.lostPackages.getAvgValue() * recordTimespan_ms * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_MAX_MS.ordinal()] = HoTTbinReader.lostPackages.getMaxValue() * recordTimespan_ms * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_MIN_MS.ordinal()] = HoTTbinReader.lostPackages.getMinValue() * recordTimespan_ms * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_SIGMA_MS.ordinal()] = (int) HoTTbinReader.lostPackages.getSigmaValue() * recordTimespan_ms * 1000;
			scores[ScoreLabelTypes.SENSORS.ordinal()] = (detectedSensors.size() - 1) * 1000; // subtract Receiver, do not subtract Channel --- is not included
			scores[ScoreLabelTypes.SENSOR_VARIO.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.VARIO) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GPS.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.GPS) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GAM.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.GAM) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_EAM.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.EAM) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_ESC.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.ESC) ? 1000 : 0;
			scores[ScoreLabelTypes.LOG_DATA_VERSION.ordinal()] = (int) (HoTTbinHistoReader2.truss.getVault().getLoadFileAsPath().getFileName().toString().startsWith(GDE.TEMP_FILE_STEM.substring(0, 1)) ? 4.2 * 1000 : 4.0 * 1000); // V4 with and without container
			scores[ScoreLabelTypes.LOG_DATA_EXPLORER_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_FILE_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal()] = histoRandomSample.getReadingCount() * HoTTbinHistoReader2.dataBlockSize;
			scores[ScoreLabelTypes.LOG_FILE_BYTES.ordinal()] = (int) HoTTbinHistoReader2.truss.getVault().getLogFileLength();
			scores[ScoreLabelTypes.LOG_FILE_RECORD_SETS.ordinal()] = (detectedSensors.size() + 1) * 1000; // +1 for channel
			scores[ScoreLabelTypes.ELAPSED_HISTO_RECORD_SET_MS.ordinal()] = (int) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - nanoTime); // do not multiply by 1000 as usual, this is the conversion from microseconds to ms
			// no display tmpRecordSet.syncScaleOfSyncableRecords();

			tmpRecordSet.setSaved(true);
			((HoTTAdapter2) device).calculateInactiveRecords(tmpRecordSet);
			device.updateVisibilityStatus(tmpRecordSet, true);
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("%s > packages:%,9d  readings:%,9d  sampled:%,9d  overSampled:%4d", tmpRecordSet.getChannelConfigName(), HoTTbinHistoReader2.truss.getVault().getLogFileLength() //$NON-NLS-1$
					/ HoTTbinHistoReader2.dataBlockSize, histoRandomSample.getReadingCount(), tmpRecordSet.getRecordDataSize(true), histoRandomSample.getOverSamplingCount()));

			HoTTbinHistoReader2.setTimeMarks(TimeMark.FINISHED);
			if (log.isLoggable(Level.TIME)) log.log(Level.TIME,
					String.format("initiateTime: %,7d  readTime: %,7d  reviewTime: %,7d  addTime: %,7d  pickTime: %,7d  finishTime: %,7d", TimeUnit.NANOSECONDS.toMillis(initiateTime), //$NON-NLS-1$
							TimeUnit.NANOSECONDS.toMillis(readTime), TimeUnit.NANOSECONDS.toMillis(reviewTime), TimeUnit.NANOSECONDS.toMillis(addTime), TimeUnit.NANOSECONDS.toMillis(pickTime),
							TimeUnit.NANOSECONDS.toMillis(finishTime)));

			if (activeChannelNumber == HoTTAdapter.Sensor.RECEIVER.getChannelNumber()) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE,
						String.format("lost:%,9d perMille:%,4d total:%,9d   lostMax_ms:%,4d lostAvg_ms=%,4d", ((RcvBinParser) HoTTbinReader.rcvBinParser).getCountPackageLoss(), //$NON-NLS-1$
								(int) (((RcvBinParser) HoTTbinReader.rcvBinParser).getCountPackageLoss() / tmpRecordSet.getTime_ms((int) HoTTbinHistoReader2.truss.getVault().getLogFileLength() / HoTTbinHistoReader2.dataBlockSize - 1) * 1000. * recordTimespan_ms),
								HoTTbinHistoReader2.truss.getVault().getLogFileLength() / HoTTbinHistoReader2.dataBlockSize, HoTTbinReader.lostPackages.getMaxValue() * 10, (int) HoTTbinReader.lostPackages.getAvgValue() * 10));
			}

			truss.promoteTruss(tmpRecordSet, scores);
			// reduce memory consumption in advance to the garbage collection
			tmpRecordSet.cleanup();
		} else {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("doFullRead=%b > ends", doFullRead));
		}

		pickerParameters.isFilterEnabled = isFilterEnabledPre; //reset filter value to user set
		return histoRandomSample;
	}

	/**
	 * Migrate sensor measurement values in the correct priority and add to record set.
	 * Receiver data are always updated.
	 * @param doAdd false skips adding the points to the recordset
	 */
	public static void migrateAddPoints(BinParser varMigrationJob, BinParser gpsMigrationJob, BinParser gamMigrationJob, BinParser eamMigrationJob, BinParser escMigrationJob, //
			long timeStep_ms, boolean doAdd) throws DataInconsitsentException {
		if (eamMigrationJob != null) eamMigrationJob.migratePoints(HoTTbinReader2.points);
		if (gamMigrationJob != null) gamMigrationJob.migratePoints(HoTTbinReader2.points);
		if (gpsMigrationJob != null) gpsMigrationJob.migratePoints(HoTTbinReader2.points);
		if (varMigrationJob != null) varMigrationJob.migratePoints(HoTTbinReader2.points);
		if (escMigrationJob != null) escMigrationJob.migratePoints(HoTTbinReader2.points);

		// this is the spot to activate the sampling by using the histoRandomSample object
		if (doAdd) recordSet.addPoints(points, timeStep_ms);
	}

	private static void setTimeMarks(TimeMark mark) {
		HoTTbinHistoReader2.currentTime = System.nanoTime();
		if (mark == TimeMark.INITIATED) {
			HoTTbinHistoReader2.initiateTime += HoTTbinHistoReader2.currentTime - HoTTbinHistoReader2.lastTime;
		} else if (mark == TimeMark.READ) {
			HoTTbinHistoReader2.readTime += HoTTbinHistoReader2.currentTime - HoTTbinHistoReader2.lastTime;
		} else if (mark == TimeMark.REVIEWED) {
			HoTTbinHistoReader2.reviewTime += HoTTbinHistoReader2.currentTime - HoTTbinHistoReader2.lastTime;
		} else if (mark == TimeMark.ADDED) {
			HoTTbinHistoReader2.addTime += HoTTbinHistoReader2.currentTime - HoTTbinHistoReader2.lastTime;
		} else if (mark == TimeMark.PICKED) {
			HoTTbinHistoReader2.pickTime += HoTTbinHistoReader2.currentTime - HoTTbinHistoReader2.lastTime;
		} else if (mark == TimeMark.FINISHED) {
			HoTTbinHistoReader2.finishTime += HoTTbinHistoReader2.currentTime - HoTTbinHistoReader2.lastTime;
		}
		HoTTbinHistoReader2.lastTime = HoTTbinHistoReader2.currentTime;
	}
}
