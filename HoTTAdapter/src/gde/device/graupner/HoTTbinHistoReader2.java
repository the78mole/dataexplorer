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
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

import gde.Analyzer;
import gde.GDE;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.ScoreLabelTypes;
import gde.device.graupner.HoTTAdapter.PickerParameters;
import gde.device.graupner.HoTTAdapter.Sensor;
import gde.device.graupner.HoTTbinReader.BinParser;
import gde.device.graupner.HoTTbinReader.InfoParser;
import gde.device.graupner.HoTTbinReader.SdLogFormat;
import gde.device.graupner.HoTTbinReader.SdLogInputStream;
import gde.device.graupner.HoTTbinReader2.RcvBinParser;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.histo.cache.ExtendedVault;
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
public class HoTTbinHistoReader2 {
	@SuppressWarnings("hiding")
	final private static String	$CLASS_NAME					= HoTTbinHistoReader2.class.getName();
	@SuppressWarnings("hiding")
	final private static Logger	log									= Logger.getLogger(HoTTbinHistoReader2.$CLASS_NAME);

	private static enum TimeMark {
		INITIATED, ADDED, PICKED, READ, REVIEWED, FINISHED
	};

	protected EnumSet<Sensor>		detectedSensors;
	protected PickerParameters	pickerParameters;
	private long								nanoTime;
	private long								currentTime, initiateTime, readTime, reviewTime, addTime, pickTime, finishTime, lastTime;
	private RecordSet						tmpRecordSet;
	private VaultCollector			truss;
	private int[]								points;

	public HoTTbinHistoReader2(PickerParameters pickerParameters) {
		this.pickerParameters = new PickerParameters(pickerParameters);

		this.pickerParameters.isFilterEnabled = true;
		this.pickerParameters.isFilterTextModus = true;
		this.pickerParameters.isChannelsChannelEnabled = this.pickerParameters.analyzer.getActiveChannel().getNumber() == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER;
	}

	/**
	 * @param inputStream for retrieving the file info and for loading the log data
	 * @param newTruss which is promoted to a full vault object if the file has a minimum length.
	 * @throws IOException
	 * @throws DataTypeException
	 * @throws DataInconsitsentException
	 */
	public void read(Supplier<InputStream> inputStream, VaultCollector newTruss) throws IOException, DataTypeException, DataInconsitsentException {
		if (newTruss.getVault().getLogFileLength() <= HoTTbinReader.NUMBER_LOG_RECORDS_MIN * HoTTbinHistoReader.DATA_BLOCK_SIZE ) return;

		nanoTime = System.nanoTime();
		initiateTime = readTime = reviewTime = addTime = pickTime = finishTime = 0;
		lastTime = System.nanoTime();

		truss = newTruss;
		try {
			read(inputStream);
		} catch (InvalidObjectException e) {
			// so any anther exception is propagated to the caller
		}
	}

	/**
	 * read file.
	 * provide data in a tmpRecordSet.
	 */
	private void read(Supplier<InputStream> inputStream) throws IOException, DataTypeException, DataInconsitsentException {
		HashMap<String, String> header = null;
		Analyzer analyzer = pickerParameters.analyzer;
		IDevice device = analyzer.getActiveDevice();
		ExtendedVault vault = truss.getVault();

		try (BufferedInputStream info_in = new BufferedInputStream(inputStream.get())) {
			header = new InfoParser((s) -> {} ).getFileInfo(info_in, vault.getLoadFilePath(), vault.getLogFileLength());
		} catch (DataTypeException e) {
			log.log(Level.WARNING, String.format("%s  %s", e.getMessage(), vault.getLoadFilePath()));
		}
		if (header == null || header.isEmpty()) return;
		detectedSensors = Sensor.getSetFromDetected(header.get(HoTTAdapter.DETECTED_SENSOR));
		if (!Sensor.getChannelNumbers(detectedSensors).contains(truss.getVault().getVaultChannelNumber())) return;

		boolean isChannelsChannel = analyzer.getActiveChannel().getNumber() == Sensor.CHANNEL.getChannelNumber(); // instead of HoTTAdapter setting
		long numberDatablocks = vault.getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE / (detectedSensors.size() == 1 && !isChannelsChannel ? 10 : 1);
		tmpRecordSet = RecordSet.createRecordSet(vault.getLogRecordsetBaseName(), analyzer, analyzer.getActiveChannel().getNumber(), true, true, false);
		tmpRecordSet.setStartTimeStamp(HoTTbinReader.getStartTimeStamp(vault.getLoadFileAsPath().getFileName().toString(), vault.getLogFileLastModified(), numberDatablocks));
		tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", tmpRecordSet.getStartTimeStamp())); //$NON-NLS-1$
		tmpRecordSet.descriptionAppendFilename(vault.getLoadFileAsPath().getFileName().toString());
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, " recordSetBaseName=" + vault.getLogRecordsetBaseName());

		try (BufferedInputStream in = new BufferedInputStream(inputStream.get()); //
				InputStream data_in = Boolean.parseBoolean(header.get(HoTTAdapter.SD_FORMAT)) //
						? new SdLogInputStream(in, vault.getLogFileLength(), new SdLogFormat(HoTTbinReaderX.headerSize, HoTTbinReaderX.footerSize, 64)) //
						: in; ) {
			int readLimitMark = (HoTTbinReader.LOG_RECORD_SCAN_START + HoTTbinReader.NUMBER_LOG_RECORDS_TO_SCAN) * HoTTbinHistoReader.DATA_BLOCK_SIZE + 1;
			if (detectedSensors.size() <= 2) {
				data_in.mark(readLimitMark); // reduces # of overscan records from 57 to 23 (to 38 with 1500 blocks)
				UniversalSampler histoRandomSample = readSingle(data_in, HoTTbinReader.LOG_RECORD_SCAN_START + HoTTbinReader.NUMBER_LOG_RECORDS_TO_SCAN, new int[0], new int[0]);
				data_in.reset();
				readSingle(data_in, -1, histoRandomSample.getMaxPoints(), histoRandomSample.getMinPoints());
			} else {
				data_in.mark(readLimitMark);
				UniversalSampler histoRandomSample = readMultiple(data_in, HoTTbinReader.LOG_RECORD_SCAN_START + HoTTbinReader.NUMBER_LOG_RECORDS_TO_SCAN, new int[0], new int[0]);
				data_in.reset();
				readMultiple(data_in, -1, histoRandomSample.getMaxPoints(), histoRandomSample.getMinPoints());
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
	private UniversalSampler readSingle(InputStream data_in, int initializeBlocks, int[] maxPoints, int[] minPoints) throws DataInconsitsentException, IOException {
		HoTTAdapter device = (HoTTAdapter) pickerParameters.analyzer.getActiveDevice();
		int activeChannelNumber = pickerParameters.analyzer.getActiveChannel().getNumber();
		boolean isReceiverData = false;
		boolean isSensorData = false;
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
		points = new int[device.getNumberOfMeasurements(activeChannelNumber)];
		byte[] buf = new byte[HoTTbinHistoReader.DATA_BLOCK_SIZE];
		byte[] buf0 = new byte[30];
		byte[] buf1 = new byte[30];
		byte[] buf2 = new byte[30];
		byte[] buf3 = new byte[30];
		byte[] buf4 = new byte[30];
		boolean isBuf1Ready = false, isBuf2Ready = false, isBuf3Ready = false, isBuf4Ready = false;
		long[] timeSteps_ms = new long[] { 0 };
		boolean isTextModusSignaled = false;

		BinParser rcvBinParser = null, chnBinParser = null, varBinParser = null, gpsBinParser = null, gamBinParser = null, eamBinParser = null, escBinParser = null;
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
		boolean doFullRead = initializeBlocks <= 0;
		int datablocksLimit = (doFullRead ? (int) truss.getVault().getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE : initializeBlocks) / (detectedSensors.size() == 1 && !pickerParameters.isChannelsChannelEnabled ? 10 : 1);
		for (int i = 0; i < datablocksLimit; i++) {
			data_in.read(buf);
			if (log.isLoggable(Level.FINE) && i % 10 == 0) {
				log.log(Level.FINE, StringHelper.fourDigitsRunningNumber(buf.length));
				log.log(Level.FINE, StringHelper.byte2Hex4CharString(buf, buf.length));
			}

			if (!pickerParameters.isFilterTextModus || (buf[6] & 0x01) == 0) { //switch into text modus
				if (buf[33] >= 0 && buf[33] <= 4 && buf[3] != 0 && buf[4] != 0) { //buf 3, 4, tx,rx
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", buf[7], buf[33]));

					((RcvBinParser) rcvBinParser).trackPackageLoss(true);
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER,
							StringHelper.byte2Hex2CharString(new byte[] { buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(buf[7], false));

					//fill receiver data
					if (buf[33] == 0 && (buf[38] & 0x80) != 128 && DataParser.parse2Short(buf, 40) >= 0) {
						rcvBinParser.parse();
						isReceiverData = true;
					}
					if (pickerParameters.isChannelsChannelEnabled) {
						chnBinParser.parse();
					}

					//fill data block 0 receiver voltage an temperature
					if (buf[33] == 0 && DataParser.parse2Short(buf, 0) != 0) {
						System.arraycopy(buf, 34, buf0, 0, buf0.length);
					}
					if (detectedSensors.size() == 1 && !pickerParameters.isChannelsChannelEnabled) { //reduce data rate for receiver to 0.1 sec
						for (int j = 0; j < 9; j++) {
							data_in.read(buf);
							timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
						}
						isSensorData = true;
					}

					//create and fill sensor specific data record sets
					switch ((byte) (buf[7] & 0xFF)) {
					case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
					case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
						if (detectedSensors.contains(Sensor.VARIO)) {
							//fill data block 1 to 2
							if (buf[33] == 1) {
								isBuf1Ready = true;
								System.arraycopy(buf, 34, buf1, 0, buf1.length);
							}
							if (buf[33] == 2) {
								isBuf2Ready = true;
								System.arraycopy(buf, 34, buf2, 0, buf2.length);
							}
							if (isBuf1Ready && isBuf2Ready) {
								varBinParser.parse();
								isBuf1Ready = isBuf2Ready = false;
								isSensorData = true;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_GPS_115200:
					case HoTTAdapter.SENSOR_TYPE_GPS_19200:
						if (detectedSensors.contains(Sensor.GPS)) {
							//fill data block 1 to 3
							if (!isBuf1Ready && buf[33] == 1) {
								isBuf1Ready = true;
								System.arraycopy(buf, 34, buf1, 0, buf1.length);
							}
							if (!isBuf2Ready && buf[33] == 2) {
								isBuf2Ready = true;
								System.arraycopy(buf, 34, buf2, 0, buf2.length);
							}
							if (!isBuf3Ready && buf[33] == 3) {
								isBuf3Ready = true;
								System.arraycopy(buf, 34, buf3, 0, buf3.length);
							}
							if (isBuf1Ready && isBuf2Ready && isBuf3Ready) {
								gpsBinParser.parse();
								isBuf1Ready = isBuf2Ready = isBuf3Ready = false;
								isSensorData = true;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
					case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
						if (detectedSensors.contains(Sensor.GAM)) {
							//fill data block 1 to 4
							if (!isBuf1Ready && buf[33] == 1) {
								isBuf1Ready = true;
								System.arraycopy(buf, 34, buf1, 0, buf1.length);
							}
							if (!isBuf2Ready && buf[33] == 2) {
								isBuf2Ready = true;
								System.arraycopy(buf, 34, buf2, 0, buf2.length);
							}
							if (!isBuf3Ready && buf[33] == 3) {
								isBuf3Ready = true;
								System.arraycopy(buf, 34, buf3, 0, buf3.length);
							}
							if (!isBuf4Ready && buf[33] == 4) {
								isBuf4Ready = true;
								System.arraycopy(buf, 34, buf4, 0, buf4.length);
							}
							if (isBuf1Ready && isBuf2Ready && isBuf3Ready && isBuf4Ready) {
								gamBinParser.parse();
								isBuf1Ready = isBuf2Ready = isBuf3Ready = isBuf4Ready = false;
								isSensorData = true;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
						if (detectedSensors.contains(Sensor.EAM)) {
							//fill data block 1 to 4
							if (!isBuf1Ready && buf[33] == 1) {
								isBuf1Ready = true;
								System.arraycopy(buf, 34, buf1, 0, buf1.length);
							}
							if (!isBuf2Ready && buf[33] == 2) {
								isBuf2Ready = true;
								System.arraycopy(buf, 34, buf2, 0, buf2.length);
							}
							if (!isBuf3Ready && buf[33] == 3) {
								isBuf3Ready = true;
								System.arraycopy(buf, 34, buf3, 0, buf3.length);
							}
							if (!isBuf4Ready && buf[33] == 4) {
								isBuf4Ready = true;
								System.arraycopy(buf, 34, buf4, 0, buf4.length);
							}
							if (isBuf1Ready && isBuf2Ready && isBuf3Ready && isBuf4Ready) {
								eamBinParser.parse();
								isBuf1Ready = isBuf2Ready = isBuf3Ready = isBuf4Ready = false;
								isSensorData = true;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
						if (detectedSensors.contains(Sensor.ESC)) {
							//fill data block 0 to 3
							if (!isBuf1Ready && buf[33] == 1) {
								isBuf1Ready = true;
								System.arraycopy(buf, 34, buf1, 0, buf1.length);
							}
							if (!isBuf2Ready && buf[33] == 2) {
								isBuf2Ready = true;
								System.arraycopy(buf, 34, buf2, 0, buf2.length);
							}
							if (!isBuf3Ready && buf[33] == 3) {
								isBuf3Ready = true;
								System.arraycopy(buf, 34, buf3, 0, buf3.length);
							}
							if (isBuf1Ready && isBuf2Ready && isBuf3Ready) {
								escBinParser.parse();
								isBuf1Ready = isBuf2Ready = isBuf3Ready = false;
								isSensorData = true;
							}
						}
						break;
					}

					if (isSensorData) {
						((RcvBinParser) rcvBinParser).updateLossStatistics();
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
					} else if (pickerParameters.isChannelsChannelEnabled) {
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
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

					((RcvBinParser) rcvBinParser).trackPackageLoss(false);

					if (pickerParameters.isChannelsChannelEnabled) {
						chnBinParser.parse();
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
					//buf1 = buf2 = buf3 = buf4 = null;
				}
			} else if (!isTextModusSignaled) {
				isTextModusSignaled = true;
			}
		}
		if (doFullRead) {
			ExtendedVault vault = truss.getVault();
			final Integer[] scores = new Integer[ScoreLabelTypes.VALUES.length];
			// values are multiplied by 1000 as this is the convention for internal values in order to avoid rounding errors for values below 1.0 (0.5 -> 0)
			// scores for duration and timestep values are filled in by the HistoVault
			scores[ScoreLabelTypes.TOTAL_READINGS.ordinal()] = histoRandomSample.getReadingCount();
			scores[ScoreLabelTypes.TOTAL_PACKAGES.ordinal()] = (int) vault.getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE;
			scores[ScoreLabelTypes.LOST_PACKAGES.ordinal()] = ((RcvBinParser) rcvBinParser).getCountPackageLoss();
			scores[ScoreLabelTypes.LOST_PACKAGES_PER_MILLE.ordinal()] = tmpRecordSet.getMaxTime_ms() > 0 ? (int) (((RcvBinParser) rcvBinParser).getCountPackageLoss() / tmpRecordSet.getMaxTime_ms() * 1000. * HoTTbinHistoReader.RECORD_TIMESPAN_MS) * 1000 : 0;
			scores[ScoreLabelTypes.LOST_PACKAGES_AVG_MS.ordinal()] = (int) ((RcvBinParser) rcvBinParser).getLostPackages().getAvgValue() * HoTTbinHistoReader.RECORD_TIMESPAN_MS * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_MAX_MS.ordinal()] = ((RcvBinParser) rcvBinParser).getLostPackages().getMaxValue() * HoTTbinHistoReader.RECORD_TIMESPAN_MS * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_MIN_MS.ordinal()] = ((RcvBinParser) rcvBinParser).getLostPackages().getMinValue() * HoTTbinHistoReader.RECORD_TIMESPAN_MS * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_SIGMA_MS.ordinal()] = (int) ((RcvBinParser) rcvBinParser).getLostPackages().getSigmaValue() * HoTTbinHistoReader.RECORD_TIMESPAN_MS * 1000;
			scores[ScoreLabelTypes.SENSORS.ordinal()] = (detectedSensors.size() - 1) * 1000; // subtract Receiver, do not subtract Channel --- is not included
			scores[ScoreLabelTypes.SENSOR_VARIO.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.VARIO) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GPS.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.GPS) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GAM.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.GAM) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_EAM.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.EAM) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_ESC.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.ESC) ? 1000 : 0;
			scores[ScoreLabelTypes.LOG_DATA_VERSION.ordinal()] = (int) (vault.getLoadFileAsPath().getFileName().toString().startsWith(GDE.TEMP_FILE_STEM.substring(0, 1)) ? 4.2 * 1000 : 4.0 * 1000); // V4 with and without container
			scores[ScoreLabelTypes.LOG_DATA_EXPLORER_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_FILE_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal()] = histoRandomSample.getReadingCount() * HoTTbinHistoReader.DATA_BLOCK_SIZE;
			scores[ScoreLabelTypes.LOG_FILE_BYTES.ordinal()] = (int) vault.getLogFileLength();
			scores[ScoreLabelTypes.LOG_FILE_RECORD_SETS.ordinal()] = (detectedSensors.size() + 1) * 1000; // +1 for channel
			scores[ScoreLabelTypes.ELAPSED_HISTO_RECORD_SET_MS.ordinal()] = (int) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - nanoTime); // do not multiply by 1000 as usual, this is the conversion from microseconds to ms
			// no display tmpRecordSet.syncScaleOfSyncableRecords();

			tmpRecordSet.setSaved(true);
			device.calculateInactiveRecords(tmpRecordSet);
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("%s > packages:%,9d  readings:%,9d  sampled:%,9d  overSampled:%4d", tmpRecordSet.getChannelConfigName(), vault.getLogFileLength() //$NON-NLS-1$
					/ HoTTbinHistoReader.DATA_BLOCK_SIZE, histoRandomSample.getReadingCount(), tmpRecordSet.getRecordDataSize(true), histoRandomSample.getOverSamplingCount()));
			setTimeMarks(TimeMark.FINISHED);
			if (log.isLoggable(Level.TIME)) log.log(Level.TIME,
					String.format("initiateTime: %,7d  readTime: %,7d  reviewTime: %,7d  addTime: %,7d  pickTime: %,7d  finishTime: %,7d", TimeUnit.NANOSECONDS.toMillis(initiateTime), //$NON-NLS-1$
							TimeUnit.NANOSECONDS.toMillis(readTime), TimeUnit.NANOSECONDS.toMillis(reviewTime), TimeUnit.NANOSECONDS.toMillis(addTime), TimeUnit.NANOSECONDS.toMillis(pickTime),
							TimeUnit.NANOSECONDS.toMillis(finishTime)));
			if (tmpRecordSet.getMaxTime_ms() > 0) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE,
						String.format("lost:%,9d perMille:%,4d total:%,9d   lostMax_ms:%,4d lostAvg_ms=%,4d", ((RcvBinParser) rcvBinParser).getCountPackageLoss(), //$NON-NLS-1$
								(int) (((RcvBinParser) rcvBinParser).getCountPackageLoss() / tmpRecordSet.getMaxTime_ms() * 1000. * HoTTbinHistoReader.RECORD_TIMESPAN_MS), vault.getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE, ((RcvBinParser) rcvBinParser).getLostPackages().getMaxValue() * 10,
								(int) ((RcvBinParser) rcvBinParser).getLostPackages().getAvgValue() * 10));
			} else {
				log.log(Level.WARNING, String.format("RecordSet with unidentified data.  fileLength=%,11d   isTextModusSignaled=%b", vault.getLogFileLength(), isTextModusSignaled));
			}

			truss.promoteTruss(tmpRecordSet, scores);
			// reduce memory consumption in advance to the garbage collection
			tmpRecordSet.cleanup();
		} else {
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
	 * @return the sampler object holding the last value sets and the min/max value sets
	 * @throws IOException
	 * @throws DataInconsitsentException
	 */
	private UniversalSampler readMultiple(InputStream data_in, int initializeBlocks, int[] maxPoints, int[] minPoints) throws IOException, DataInconsitsentException {
		HoTTAdapter device = (HoTTAdapter) pickerParameters.analyzer.getActiveDevice();
		int activeChannelNumber = pickerParameters.analyzer.getActiveChannel().getNumber();
		boolean isReceiverData = false;
		BinParser varMigrationJob = null;
		BinParser gpsMigrationJob = null;
		BinParser gamMigrationJob = null;
		BinParser eamMigrationJob = null;
		BinParser escMigrationJob = null;
		boolean isJustMigrated = false;
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
		points = new int[device.getNumberOfMeasurements(activeChannelNumber)];
		byte[] buf = new byte[HoTTbinHistoReader.DATA_BLOCK_SIZE];
		byte[] buf0 = new byte[30];
		byte[] buf1 = new byte[30];
		byte[] buf2 = new byte[30];
		byte[] buf3 = new byte[30];
		byte[] buf4 = new byte[30];
		byte actualSensor = -1, lastSensor = -1;
		int logCountVario = 0, logCountGPS = 0, logCountGAM = 0, logCountEAM = 0, logCountESC = 0;
		long[] timeSteps_ms = new long[] { 0 };
		boolean isTextModusSignaled = false;

		BinParser rcvBinParser = null, chnBinParser = null, varBinParser = null, gpsBinParser = null, gamBinParser = null, eamBinParser = null, escBinParser = null;
		// parse in situ for receiver and channel
		rcvBinParser = Sensor.RECEIVER.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
		chnBinParser = Sensor.CHANNEL.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
		// use parser points objects
		varBinParser = Sensor.VARIO.createBinParser2(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2 });
		gpsBinParser = Sensor.GPS.createBinParser2(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3 });
		gamBinParser = Sensor.GAM.createBinParser2(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		eamBinParser = Sensor.EAM.createBinParser2(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		escBinParser = Sensor.ESC.createBinParser2(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3 });
		UniversalSampler histoRandomSample = UniversalSampler.createSampler(activeChannelNumber, points, HoTTbinHistoReader.RECORD_TIMESPAN_MS, pickerParameters.analyzer);
		histoRandomSample.setMaxMinPoints(maxPoints, minPoints);

		// read all the data blocks from the file, parse only for the active channel
		setTimeMarks(TimeMark.INITIATED);
		boolean isChannelsChannel = activeChannelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER;
		tmpRecordSet.cleanup();
		boolean doFullRead = initializeBlocks <= 0;
		int initializeBlockLimit = initializeBlocks > 0 ? initializeBlocks : Integer.MAX_VALUE;
		for (int i = 0; i < initializeBlockLimit && i < truss.getVault().getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE; i++) {
			data_in.read(buf);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, StringHelper.byte2Hex4CharString(buf, buf.length));
			}

			if (!pickerParameters.isFilterTextModus || (buf[6] & 0x01) == 0) { //switch into text modus
				if (buf[33] >= 0 && buf[33] <= 4 && buf[3] != 0 && buf[4] != 0) { //buf 3, 4, tx,rx
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", buf[7], buf[33]));

					((RcvBinParser) rcvBinParser).trackPackageLoss(true);
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST,
							StringHelper.byte2Hex2CharString(new byte[] { buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(buf[7], false));

					//fill receiver data
					if (buf[33] == 0 && (buf[38] & 0x80) != 128 && DataParser.parse2Short(buf, 40) >= 0) {
						rcvBinParser.parse();
						isReceiverData = true;
					}
					if (isChannelsChannel) {
						chnBinParser.parse();
					}

					if (actualSensor == -1)
						lastSensor = actualSensor = (byte) (buf[7] & 0xFF);
					else
						actualSensor = (byte) (buf[7] & 0xFF);

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
									varBinParser.parse();
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
									gpsBinParser.parse();
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
									gamBinParser.parse();
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
									eamBinParser.parse();
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
									escBinParser.parse();
									escMigrationJob = escBinParser;
								}
								break;
							}

							if (HoTTbinReader2.log.isLoggable(Level.FINE)) HoTTbinReader2.log.log(Level.FINE, "isReceiverData " + isReceiverData + " isVarioData " + varMigrationJob //
									+ " isGPSData " + gpsMigrationJob + " isGeneralData " + gamMigrationJob + " isElectricData " + eamMigrationJob + " isEscData " + escMigrationJob);
						}

						if (log.isLoggable(Level.FINE)) log.log(Level.FINE,
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
						((RcvBinParser) rcvBinParser).updateLossStatistics();
					}

					if (isReceiverData && (logCountVario > 0 || logCountGPS > 0 || logCountGAM > 0 || logCountEAM > 0 || logCountESC > 0)) {
						setTimeMarks(TimeMark.READ);
						boolean isValidSample = histoRandomSample.capturePoints(timeSteps_ms[BinParser.TIMESTEP_INDEX]);
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
						boolean isValidSample = histoRandomSample.capturePoints(timeSteps_ms[BinParser.TIMESTEP_INDEX]);
						setTimeMarks(TimeMark.REVIEWED);
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							setTimeMarks(TimeMark.ADDED);
							setTimeMarks(TimeMark.PICKED);
						}
					}
					isJustMigrated = false;

					//fill data block 0 to 4
					if (buf[33] == 0 && DataParser.parse2Short(buf, 0) != 0) {
						System.arraycopy(buf, 34, buf0, 0, buf0.length);
					}
					if (buf[33] == 1) {
						System.arraycopy(buf, 34, buf1, 0, buf1.length);
					}
					if (buf[33] == 2) {
						System.arraycopy(buf, 34, buf2, 0, buf2.length);
					}
					if (buf[33] == 3) {
						System.arraycopy(buf, 34, buf3, 0, buf3.length);
					}
					if (buf[33] == 4) {
						System.arraycopy(buf, 34, buf4, 0, buf4.length);
					}

					timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;// add default time step from log record of 10 msec
				} else { //skip empty block, but add time step
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

					((RcvBinParser) rcvBinParser).trackPackageLoss(false);
					if (activeChannelNumber == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER) {
						chnBinParser.parse();
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
				}
			} else if (!isTextModusSignaled) {
				isTextModusSignaled = true;
			}
		}

		if (doFullRead) {
			ExtendedVault vault = truss.getVault();
			final Integer[] scores = new Integer[ScoreLabelTypes.VALUES.length];
			// values are multiplied by 1000 as this is the convention for internal values in order to avoid rounding errors for values below 1.0 (0.5 -> 0)
			// scores for duration and timestep values are filled in by the HistoVault
			scores[ScoreLabelTypes.TOTAL_READINGS.ordinal()] = histoRandomSample.getReadingCount();
			scores[ScoreLabelTypes.TOTAL_PACKAGES.ordinal()] = (int) truss.getVault().getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE;
			scores[ScoreLabelTypes.LOST_PACKAGES.ordinal()] = ((RcvBinParser) rcvBinParser).getCountPackageLoss();
			scores[ScoreLabelTypes.LOST_PACKAGES_PER_MILLE.ordinal()] = tmpRecordSet.getMaxTime_ms() > 0 ? (int) (((RcvBinParser) rcvBinParser).getCountPackageLoss() / tmpRecordSet.getMaxTime_ms() * 1000. * HoTTbinHistoReader.RECORD_TIMESPAN_MS) * 1000 : 0;
			scores[ScoreLabelTypes.LOST_PACKAGES_AVG_MS.ordinal()] = (int) ((RcvBinParser) rcvBinParser).getLostPackages().getAvgValue() * HoTTbinHistoReader.RECORD_TIMESPAN_MS * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_MAX_MS.ordinal()] = ((RcvBinParser) rcvBinParser).getLostPackages().getMaxValue() * HoTTbinHistoReader.RECORD_TIMESPAN_MS * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_MIN_MS.ordinal()] = ((RcvBinParser) rcvBinParser).getLostPackages().getMinValue() * HoTTbinHistoReader.RECORD_TIMESPAN_MS * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_SIGMA_MS.ordinal()] = (int) ((RcvBinParser) rcvBinParser).getLostPackages().getSigmaValue() * HoTTbinHistoReader.RECORD_TIMESPAN_MS * 1000;
			scores[ScoreLabelTypes.SENSORS.ordinal()] = (detectedSensors.size() - 1) * 1000; // subtract Receiver, do not subtract Channel --- is not included
			scores[ScoreLabelTypes.SENSOR_VARIO.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.VARIO) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GPS.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.GPS) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GAM.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.GAM) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_EAM.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.EAM) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_ESC.ordinal()] = detectedSensors.contains(HoTTAdapter.Sensor.ESC) ? 1000 : 0;
			scores[ScoreLabelTypes.LOG_DATA_VERSION.ordinal()] = (int) (truss.getVault().getLoadFileAsPath().getFileName().toString().startsWith(GDE.TEMP_FILE_STEM.substring(0, 1)) ? 4.2 * 1000 : 4.0 * 1000); // V4 with and without container
			scores[ScoreLabelTypes.LOG_DATA_EXPLORER_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_FILE_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal()] = histoRandomSample.getReadingCount() * HoTTbinHistoReader.DATA_BLOCK_SIZE;
			scores[ScoreLabelTypes.LOG_FILE_BYTES.ordinal()] = (int) truss.getVault().getLogFileLength();
			scores[ScoreLabelTypes.LOG_FILE_RECORD_SETS.ordinal()] = (detectedSensors.size() + 1) * 1000; // +1 for channel
			scores[ScoreLabelTypes.ELAPSED_HISTO_RECORD_SET_MS.ordinal()] = (int) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - nanoTime); // do not multiply by 1000 as usual, this is the conversion from microseconds to ms
			// no display tmpRecordSet.syncScaleOfSyncableRecords();

			tmpRecordSet.setSaved(true);
			device.calculateInactiveRecords(tmpRecordSet);
			device.updateVisibilityStatus(tmpRecordSet, true);
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("%s > packages:%,9d  readings:%,9d  sampled:%,9d  overSampled:%4d", tmpRecordSet.getChannelConfigName(), vault.getLogFileLength() //$NON-NLS-1$
					/ HoTTbinHistoReader.DATA_BLOCK_SIZE, histoRandomSample.getReadingCount(), tmpRecordSet.getRecordDataSize(true), histoRandomSample.getOverSamplingCount()));

			setTimeMarks(TimeMark.FINISHED);
			if (log.isLoggable(Level.TIME)) log.log(Level.TIME,
					String.format("initiateTime: %,7d  readTime: %,7d  reviewTime: %,7d  addTime: %,7d  pickTime: %,7d  finishTime: %,7d", TimeUnit.NANOSECONDS.toMillis(initiateTime), //$NON-NLS-1$
							TimeUnit.NANOSECONDS.toMillis(readTime), TimeUnit.NANOSECONDS.toMillis(reviewTime), TimeUnit.NANOSECONDS.toMillis(addTime), TimeUnit.NANOSECONDS.toMillis(pickTime),
							TimeUnit.NANOSECONDS.toMillis(finishTime)));

			if (log.isLoggable(Level.FINE)) log.log(Level.FINE,
					String.format("lost:%,9d perMille:%,4d total:%,9d   lostMax_ms:%,4d lostAvg_ms=%,4d", ((RcvBinParser) rcvBinParser).getCountPackageLoss(), //$NON-NLS-1$
							(int) (((RcvBinParser) rcvBinParser).getCountPackageLoss() / tmpRecordSet.getTime_ms((int) vault.getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE - 1) * 1000. * HoTTbinHistoReader.RECORD_TIMESPAN_MS),
							vault.getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE, ((RcvBinParser) rcvBinParser).getLostPackages().getMaxValue() * 10, (int) ((RcvBinParser) rcvBinParser).getLostPackages().getAvgValue() * 10));

			truss.promoteTruss(tmpRecordSet, scores);
			// reduce memory consumption in advance to the garbage collection
			tmpRecordSet.cleanup();
		} else {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("doFullRead=%b > ends", doFullRead));
		}

		return histoRandomSample;
	}

	/**
	 * Migrate sensor measurement values in the correct priority and add to record set.
	 * Receiver data are always updated.
	 * @param doAdd false skips adding the points to the recordset
	 */
	private void migrateAddPoints(BinParser varMigrationJob, BinParser gpsMigrationJob, BinParser gamMigrationJob, BinParser eamMigrationJob, BinParser escMigrationJob, //
			long timeStep_ms, boolean doAdd) throws DataInconsitsentException {
		if (eamMigrationJob != null) eamMigrationJob.migratePoints(points);
		if (gamMigrationJob != null) gamMigrationJob.migratePoints(points);
		if (gpsMigrationJob != null) gpsMigrationJob.migratePoints(points);
		if (varMigrationJob != null) varMigrationJob.migratePoints(points);
		if (escMigrationJob != null) escMigrationJob.migratePoints(points);

		// this is the spot to activate the sampling by using the histoRandomSample object
		if (doAdd) tmpRecordSet.addPoints(points, timeStep_ms);
	}

	private void setTimeMarks(TimeMark mark) {
		currentTime = System.nanoTime();
		if (mark == TimeMark.INITIATED) {
			initiateTime += currentTime - lastTime;
		} else if (mark == TimeMark.READ) {
			readTime += currentTime - lastTime;
		} else if (mark == TimeMark.REVIEWED) {
			reviewTime += currentTime - lastTime;
		} else if (mark == TimeMark.ADDED) {
			addTime += currentTime - lastTime;
		} else if (mark == TimeMark.PICKED) {
			pickTime += currentTime - lastTime;
		} else if (mark == TimeMark.FINISHED) {
			finishTime += currentTime - lastTime;
		}
		lastTime = currentTime;
	}
}
