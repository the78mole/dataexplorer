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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;

import gde.GDE;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.graupner.HoTTAdapter.PickerParameters;
import gde.device.graupner.HoTTAdapter.Sensor;
import gde.device.graupner.HoTTbinReader.BinParser;
import gde.device.graupner.HoTTbinReader.BufCopier;
import gde.device.graupner.HoTTbinReader.InfoParser;
import gde.device.graupner.HoTTbinReader2.RcvBinParser;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.ThrowableUtils;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.VaultCollector;
import gde.histo.device.UniversalSampler;
import gde.io.DataParser;
import gde.log.Level;
import gde.utils.StringHelper;

/**
 * Read Graupner HoTT binary data for history analysis.
 * Collect data in a recordset and fill the vault collector.
 * Read measurements from multiple sensors for one single channel.
 * For small files (around 1 minute) no measurements are added to the recordset.
 * Support sampling to maximize the throughput.
 */
public class HoTTbinHistoReader2 extends HoTTbinHistoReader {
	private static final String	$CLASS_NAME									= HoTTbinHistoReader2.class.getName();
	private static final Logger	log													= Logger.getLogger(HoTTbinHistoReader2.$CLASS_NAME);

	/**
	 * the high number of measurement records increases the probability for excessive max/min values
	 */
	private static final int		INITIALIZE_SAMPLING_FACTOR	= 3;

	public HoTTbinHistoReader2(PickerParameters pickerParameters) {
		super(pickerParameters, pickerParameters.analyzer.getActiveChannel().getNumber() == HoTTAdapter2.CHANNELS_CHANNEL_NUMBER, INITIALIZE_SAMPLING_FACTOR);
	}

	/**
	 * @param inputStream for retrieving the file info and for loading the log data
	 * @param newTruss which is promoted to a full vault object if the file has a minimum length.
	 */
	@Override
	public void read(Supplier<InputStream> inputStream, VaultCollector newTruss) throws IOException, DataTypeException, DataInconsitsentException {
		if (newTruss.getVault().getLogFileLength() <= HoTTbinReader.NUMBER_LOG_RECORDS_MIN * HoTTbinHistoReader.DATA_BLOCK_SIZE) return;

		nanoTime = System.nanoTime();
		initiateTime = readTime = reviewTime = addTime = pickTime = finishTime = 0;
		lastTime = System.nanoTime();

		truss = newTruss;
		IDevice device = analyzer.getActiveDevice();
		ExtendedVault vault = truss.getVault();
		long numberDatablocks = vault.getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE;
		tmpRecordSet = RecordSet.createRecordSet(vault.getLogRecordsetBaseName(), analyzer, analyzer.getActiveChannel().getNumber(), true, true, false);
		tmpRecordSet.setStartTimeStamp(HoTTbinReader.getStartTimeStamp(vault.getLoadFileAsPath().getFileName().toString(), vault.getLogFileLastModified(), numberDatablocks));
		tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", tmpRecordSet.getStartTimeStamp())); //$NON-NLS-1$
		tmpRecordSet.descriptionAppendFilename(vault.getLoadFileAsPath().getFileName().toString());
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, " recordSetBaseName=" + vault.getLogRecordsetBaseName());

		HashMap<String, String> header = null;
		try (BufferedInputStream info_in = new BufferedInputStream(inputStream.get())) {
			header = new InfoParser((s) -> {}).getFileInfo(info_in, vault.getLoadFilePath(), vault.getLogFileLength());
			if (header == null || header.isEmpty()) return;

			detectedSensors = Sensor.getSetFromDetected(header.get(HoTTAdapter.DETECTED_SENSOR));
			
			//set picker parameter setting sensor for altitude/climb usage (0=auto, 1=VARIO, 2=GPS, 3=GAM, 4=EAM)
			HoTTbinReader.setAltitudeClimbPickeParameter(pickerParameters, detectedSensors);

			read(inputStream, Boolean.parseBoolean(header.get(HoTTAdapter.SD_FORMAT)));
		} catch (DataTypeException e) {
			log.log(Level.WARNING, String.format("%s  %s", e.getMessage(), vault.getLoadFilePath()));
		} catch (InvalidObjectException e) {
			// so any anther exception is propagated to the caller
		}
	}

	/**
	 * read log data according to version 0 either in initialize mode for learning min/max values or in fully functional read mode.
	 * reads only sample records and allocates only one single record set.
	 * no progress bar support and no channel data modifications.
	 * @param data_in
	 * @param initializeBlocks if this number is greater than zero, the min/max values are initialized
	 * @param histoRandomSample is the random sampler which might use the minMax values from a previous run and thus reduces oversampling
	 */
	@Override
	protected void readSingle(InputStream data_in, int initializeBlocks, UniversalSampler histoRandomSample) throws DataInconsitsentException, IOException {
		HoTTAdapter2 device = (HoTTAdapter2) analyzer.getActiveDevice();
		boolean isReceiverData = false;
		boolean isSensorData = false;
		boolean[] isResetMinMax = new boolean[] {false, false, false, false, false}; //ESC, EAM, GAM, GPS, Vario
		int[]	points = histoRandomSample.getPoints();
		byte[] buf = new byte[HoTTbinHistoReader.DATA_BLOCK_SIZE];
		byte[] buf0 = new byte[30];
		byte[] buf1 = new byte[30];
		byte[] buf2 = new byte[30];
		byte[] buf3 = new byte[30];
		byte[] buf4 = new byte[30];
		BufCopier bufCopier = new BufCopier(buf, buf0, buf1, buf2, buf3, buf4);
		long[] timeSteps_ms = new long[] { 0 };
		boolean isTextModusSignaled = false;

		BinParser rcvBinParser = null, chnBinParser = null, varBinParser = null, gpsBinParser = null, gamBinParser = null, eamBinParser = null, escBinParser = null;
		rcvBinParser = Sensor.RECEIVER.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
		if (isChannelsChannelEnabled) {
			chnBinParser = Sensor.CHANNEL.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
		}
		if (detectedSensors.contains(Sensor.VARIO)) {
			varBinParser = Sensor.VARIO.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2 });
		}
		if (detectedSensors.contains(Sensor.GPS)) {
			gpsBinParser = Sensor.GPS.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3 });
		}
		if (detectedSensors.contains(Sensor.GAM)) {
			gamBinParser = Sensor.GAM.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		}
		if (detectedSensors.contains(Sensor.EAM)) {
			eamBinParser = Sensor.EAM.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		}
		if (detectedSensors.contains(Sensor.ESC)) {
			escBinParser = Sensor.ESC.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3 });
		}

		Procedure pointsAdder = initializeBlocks <= 0 //
				? () -> {
					readTimer.invoke();
					boolean isValidSample = histoRandomSample.capturePoints(timeSteps_ms[BinParser.TIMESTEP_INDEX]);
					reviewTimer.invoke();
					if (isValidSample) {
						try {
							int[] histoRandomSamplePoints = histoRandomSample.getSamplePoints();
							//GPS 15=Latitude, 16=Longitude, 17=Velocity, 18=Distance, 19=Direction, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
							if (!isResetMinMax[3] && histoRandomSamplePoints[22] == 3000  && histoRandomSamplePoints[15] != 0 && histoRandomSamplePoints[16] != 0) {
								for (int j=15; j<23; ++j) {
									tmpRecordSet.get(j).setMinMax(histoRandomSamplePoints[j], histoRandomSamplePoints[j]);
								}
								isResetMinMax[3] = true;
							}
							//GAM 24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6,
							// 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage,
							// 43=LowestCellNumber, 44=Pressure, 45=Event G
							if (!isResetMinMax[2] && histoRandomSamplePoints[24] != 0) {
								for (int j=24; j<45; ++j) {
									tmpRecordSet.get(j).setMinMax(histoRandomSamplePoints[j], histoRandomSamplePoints[j]);
								}
								isResetMinMax[2] = true;
							}
							//EAM 46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14,
							// 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
							if (!isResetMinMax[1] && histoRandomSamplePoints[46] != 0) {
								for (int j=46; j<72; ++j) {
									tmpRecordSet.get(j).setMinMax(histoRandomSamplePoints[j], histoRandomSamplePoints[j]);
								}
								isResetMinMax[1] = true;
							}
							if (isChannelsChannelEnabled) {
								//ESC 93=VoltageM, 94=CurrentM, 95=CapacityM, 96=PowerM, 97=RevolutionM, 98=TemperatureM 1, 99=TemperatureM 2 100=Voltage_min, 101=Current_max, 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M
								// 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M
								if (!isResetMinMax[0] && histoRandomSamplePoints[93] != 0) {
									for (int j=93; j<105; ++j) {
										tmpRecordSet.get(j).setMinMax(histoRandomSamplePoints[j], histoRandomSamplePoints[j]);
									}
									isResetMinMax[0] = true;
								}
							} else {
								//ESC 73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max,
								// 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M
								if (!isResetMinMax[0] && histoRandomSamplePoints[73] != 0) {
									for (int j=73; j<85; ++j) {
										tmpRecordSet.get(j).setMinMax(histoRandomSamplePoints[j], histoRandomSamplePoints[j]);
									}
									isResetMinMax[0] = true;
								}
							}

							tmpRecordSet.addPoints(histoRandomSamplePoints, histoRandomSample.getSampleTimeStep_ms());
						} catch (DataInconsitsentException e) {
							throw ThrowableUtils.rethrow(e);
						}
						addTimer.invoke();
						pickTimer.invoke();
					}
				} : () -> histoRandomSample.capturePoints(timeSteps_ms[BinParser.TIMESTEP_INDEX]);

		// read all the data blocks from the file, parse only for the active channel
		boolean doFullRead = initializeBlocks <= 0;
		boolean doDataSkip = detectedSensors.size() == 1 && !isChannelsChannelEnabled;
		int datablocksLimit = (doFullRead ? (int) truss.getVault().getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE : initializeBlocks) / (doDataSkip ? 10 : 1);
		for (int i = 0; i < datablocksLimit; i++) {
			data_in.read(buf);
			if (log.isLoggable(Level.FINE) && i % 10 == 0) {
				log.log(Level.FINE, StringHelper.fourDigitsRunningNumber(buf.length));
				log.log(Level.FINE, StringHelper.byte2Hex4CharString(buf, buf.length));
			}

			if (!isFilterTextModus || (buf[6] & 0x01) == 0) { //switch into text modus
				if (buf[33] >= 0 && buf[33] <= 4 && buf[3] != 0 && buf[4] != 0) { //buf 3, 4, tx,rx
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", buf[7], buf[33]));

					((RcvBinParser) rcvBinParser).trackPackageLoss(true);
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, StringHelper.byte2Hex2CharString(new byte[] { buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(buf[7], false));

					//fill receiver data
					if (buf[33] == 0 && (buf[38] & 0x80) != 128 && DataParser.parse2Short(buf, 40) >= 0) {
						rcvBinParser.parse();
						isReceiverData = true;
					}
					if (chnBinParser != null) {
						chnBinParser.parse();
					}

					//fill data block 0 receiver voltage an temperature
					if (buf[33] == 0) {
						bufCopier.copyToBuffer();
					}
					if (detectedSensors.size() == 1 && chnBinParser == null) { //reduce data rate for receiver to 0.1 sec
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
						if (varBinParser != null) {
							bufCopier.copyToVarioBuffer();
							if (bufCopier.is2BuffersFull()) {
								varBinParser.parse();
								bufCopier.clearBuffers();
								isSensorData = true;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_GPS_115200:
					case HoTTAdapter.SENSOR_TYPE_GPS_19200:
						if (gpsBinParser != null) {
							bufCopier.copyToFreeBuffer();
							if (bufCopier.is3BuffersFull()) {
								gpsBinParser.parse();
								bufCopier.clearBuffers();
								isSensorData = true;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
					case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
						if (gamBinParser != null) {
							bufCopier.copyToFreeBuffer();
							if (bufCopier.is4BuffersFull()) {
								gamBinParser.parse();
								bufCopier.clearBuffers();
								isSensorData = true;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
						if (eamBinParser != null) {
							bufCopier.copyToFreeBuffer();
							if (bufCopier.is4BuffersFull()) {
								eamBinParser.parse();
								bufCopier.clearBuffers();
								isSensorData = true;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
						if (escBinParser != null) {
							bufCopier.copyToFreeBuffer();
							if (bufCopier.is3BuffersFull()) {
								escBinParser.parse();
								bufCopier.clearBuffers();
								isSensorData = true;
							}
						}
						break;
					}

					if (isSensorData) {
						((RcvBinParser) rcvBinParser).updateLossStatistics();
					}

					if (isSensorData || (isReceiverData && this.tmpRecordSet.get(0).realSize() > 0)) {
						pointsAdder.invoke();
						isSensorData = isReceiverData = false;
					} else if (chnBinParser != null) {
						pointsAdder.invoke();
					}

					timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10; // add default time step from device of 10 msec
				} else { //skip empty block, but add time step
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

					((RcvBinParser) rcvBinParser).trackPackageLoss(false);

					if (chnBinParser != null) {
						chnBinParser.parse();
						pointsAdder.invoke();
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
			PackageLoss lostPackages  = ((RcvBinParser) rcvBinParser).getLostPackages();
			Integer[] scores = getScores(lostPackages, histoRandomSample,  truss.getVault());
			device.calculateInactiveRecords(tmpRecordSet);
			device.updateVisibilityStatus(tmpRecordSet, true);
			truss.promoteTruss(tmpRecordSet, scores);
			finishTimer.invoke();
			writeFinalLog(isTextModusSignaled, lostPackages, histoRandomSample, truss.getVault());
			// reduce memory consumption in advance to the garbage collection
			tmpRecordSet.cleanup();
		}
		log.log(Level.FINER, " > ends <  doFullRead=", doFullRead); //$NON-NLS-1$
	}

	/**
	 * read log data according to version 0 either in initialize mode for learning min/max values or in fully functional read mode.
	 * reads only sample records and allocates only one single recordset, so HoTTAdapter.isChannelsChannelEnabled does not take effect.
	 * no progress bar support and no channel data modifications.
	 * @param data_in
	 * @param initializeBlocks if this number is greater than zero, the min/max values are initialized
	 * @param histoRandomSample is the random sampler which might use the minMax values from a previous run and thus reduces oversampling
	 */
	@Override
	protected void readMultiple(InputStream data_in, int initializeBlocks, UniversalSampler histoRandomSample) throws IOException, DataInconsitsentException {
		HoTTAdapter2 device = (HoTTAdapter2) analyzer.getActiveDevice();
		boolean isReceiverData = false;
		boolean isJustMigrated = false;
		boolean[] isResetMinMax = new boolean[] {false, false, false, false, false}; //ESC, EAM, GAM, GPS, Vario
		int[]	points = histoRandomSample.getPoints();
		byte[] buf = new byte[HoTTbinHistoReader.DATA_BLOCK_SIZE];
		byte[] buf0 = new byte[30];
		byte[] buf1 = new byte[30];
		byte[] buf2 = new byte[30];
		byte[] buf3 = new byte[30];
		byte[] buf4 = new byte[30];
		BufCopier bufCopier = new BufCopier(buf, buf0, buf1, buf2, buf3, buf4);
		byte actualSensor = -1, lastSensor = -1;
		int logCountVario = 0, logCountGPS = 0, logCountGAM = 0, logCountEAM = 0, logCountESC = 0;
		long[] timeSteps_ms = new long[] { 0 };
		boolean isTextModusSignaled = false;

		BinParser rcvBinParser, chnBinParser, varBinParser, gpsBinParser, gamBinParser, eamBinParser, escBinParser;
		// parse in situ for receiver and channel
		rcvBinParser = Sensor.RECEIVER.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
    if (isChannelsChannelEnabled) {
    	chnBinParser = Sensor.CHANNEL.createBinParser2(pickerParameters, points, timeSteps_ms, new byte[][] { buf });
		} else chnBinParser = null;
		// use parser points objects
		if (detectedSensors.contains(Sensor.VARIO)) {
			varBinParser = Sensor.VARIO.createBinParser2(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2 });
		} else varBinParser = null;
		if (detectedSensors.contains(Sensor.GPS)) {
			gpsBinParser = Sensor.GPS.createBinParser2(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3 });
		} else gpsBinParser = null;
		if (detectedSensors.contains(Sensor.GAM)) {
			gamBinParser = Sensor.GAM.createBinParser2(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else gamBinParser = null;
		if (detectedSensors.contains(Sensor.EAM)) {
			eamBinParser = Sensor.EAM.createBinParser2(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else eamBinParser = null;
		if (detectedSensors.contains(Sensor.ESC)) {
			escBinParser = Sensor.ESC.createBinParser2(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3 });
		} else escBinParser = null;

		Set<BinParser> migrationJobs = new HashSet<>();
		@SuppressWarnings("null")
		Procedure migrator = () -> {
			// the sequence of the next statements is crucial, eg. for vario data
			if (migrationJobs.contains(eamBinParser)) eamBinParser.migratePoints(points);
			if (migrationJobs.contains(gamBinParser)) gamBinParser.migratePoints(points);
			if (migrationJobs.contains(gpsBinParser)) gpsBinParser.migratePoints(points);
			if (migrationJobs.contains(varBinParser)) varBinParser.migratePoints(points);
			if (migrationJobs.contains(escBinParser)) escBinParser.migratePoints(points);
			migrationJobs.clear();
		};

		Procedure pointsAdder = initializeBlocks <= 0 //
				? () -> {
					readTimer.invoke();
					boolean isValidSample = histoRandomSample.capturePoints(timeSteps_ms[BinParser.TIMESTEP_INDEX]);
					reviewTimer.invoke();
					if (isValidSample) {
						try {
							int[] histoRandomSamplePoints = histoRandomSample.getSamplePoints();
							//GPS 15=Latitude, 16=Longitude, 17=Velocity, 18=Distance, 19=Direction, 20=TripDistance 21=NumSatellites 22=GPS-Fix 23=EventGPS
							if (!isResetMinMax[3] && histoRandomSamplePoints[22] == 3000  && histoRandomSamplePoints[15] != 0 && histoRandomSamplePoints[16] != 0) {
								for (int j=15; j<23; ++j) {
									tmpRecordSet.get(j).setMinMax(histoRandomSamplePoints[j], histoRandomSamplePoints[j]);
								}
								isResetMinMax[3] = true;
							}
							//GAM 24=Voltage G, 25=Current G, 26=Capacity G, 27=Power G, 28=Balance G, 29=CellVoltage G1, 30=CellVoltage G2 .... 34=CellVoltage G6,
							// 35=Revolution G, 36=FuelLevel, 37=Voltage G1, 38=Voltage G2, 39=Temperature G1, 40=Temperature G2 41=Speed G, 42=LowestCellVoltage,
							// 43=LowestCellNumber, 44=Pressure, 45=Event G
							if (!isResetMinMax[2] && histoRandomSamplePoints[24] != 0) {
								for (int j=24; j<45; ++j) {
									tmpRecordSet.get(j).setMinMax(histoRandomSamplePoints[j], histoRandomSamplePoints[j]);
								}
								isResetMinMax[2] = true;
							}
							//EAM 46=Voltage E, 47=Current E, 48=Capacity E, 49=Power E, 50=Balance E, 51=CellVoltage E1, 52=CellVoltage E2 .... 64=CellVoltage E14,
							// 65=Voltage E1, 66=Voltage E2, 67=Temperature E1, 68=Temperature E2 69=Revolution E 70=MotorTime 71=Speed 72=Event E
							if (!isResetMinMax[1] && histoRandomSamplePoints[46] != 0) {
								for (int j=46; j<72; ++j) {
									tmpRecordSet.get(j).setMinMax(histoRandomSamplePoints[j], histoRandomSamplePoints[j]);
								}
								isResetMinMax[1] = true;
							}
							if (isChannelsChannelEnabled) {
								//ESC 93=VoltageM, 94=CurrentM, 95=CapacityM, 96=PowerM, 97=RevolutionM, 98=TemperatureM 1, 99=TemperatureM 2 100=Voltage_min, 101=Current_max, 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M
								// 102=Revolution_max, 103=Temperature1_max, 104=Temperature2_max 105=Event M
								if (!isResetMinMax[0] && histoRandomSamplePoints[93] != 0) {
									for (int j=93; j<105; ++j) {
										tmpRecordSet.get(j).setMinMax(histoRandomSamplePoints[j], histoRandomSamplePoints[j]);
									}
									isResetMinMax[0] = true;
								}
							} else {
								//ESC 73=VoltageM, 74=CurrentM, 75=CapacityM, 76=PowerM, 77=RevolutionM, 78=TemperatureM 1, 79=TemperatureM 2 80=Voltage_min, 81=Current_max,
								// 82=Revolution_max, 83=Temperature1_max, 84=Temperature2_max 85=Event M
								if (!isResetMinMax[0] && histoRandomSamplePoints[73] != 0) {
									for (int j=73; j<85; ++j) {
										tmpRecordSet.get(j).setMinMax(histoRandomSamplePoints[j], histoRandomSamplePoints[j]);
									}
									isResetMinMax[0] = true;
								}
							}

							tmpRecordSet.addPoints(histoRandomSamplePoints, histoRandomSample.getSampleTimeStep_ms());
						} catch (DataInconsitsentException e) {
							throw ThrowableUtils.rethrow(e);
						}
						addTimer.invoke();
						pickTimer.invoke();
					}
				} : () -> histoRandomSample.capturePoints(timeSteps_ms[BinParser.TIMESTEP_INDEX]);
		initTimer.invoke();

		// read all the data blocks from the file, parse only for the active channel
		boolean doFullRead = initializeBlocks <= 0;
		int initializeBlockLimit = initializeBlocks > 0 ? initializeBlocks : Integer.MAX_VALUE;
		for (int i = 0; i < initializeBlockLimit && i < truss.getVault().getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE; i++) {
			data_in.read(buf);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, StringHelper.byte2Hex4CharString(buf, buf.length));

			if (!isFilterTextModus || (buf[6] & 0x01) == 0) { //switch into text modus
				if (buf[33] >= 0 && buf[33] <= 4 && buf[3] != 0 && buf[4] != 0) { //buf 3, 4, tx,rx
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", buf[7], buf[33]));

					((RcvBinParser) rcvBinParser).trackPackageLoss(true);
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, StringHelper.byte2Hex2CharString(new byte[] { buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(buf[7], false));

					//fill receiver data
					if (buf[33] == 0 && (buf[38] & 0x80) != 128 && DataParser.parse2Short(buf, 40) >= 0) {
						rcvBinParser.parse();
						isReceiverData = true;
					}
					if (chnBinParser != null) {
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
								if (varBinParser != null) {
									if (migrationJobs.contains(varBinParser) && isReceiverData) {
										migrator.invoke();
										isJustMigrated = true;
										isReceiverData = false;
										pointsAdder.invoke();
									}
									varBinParser.parse();
									migrationJobs.add(varBinParser);
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GPS_115200:
							case HoTTAdapter.SENSOR_TYPE_GPS_19200:
								if (gpsBinParser != null) {
									if (migrationJobs.contains(gpsBinParser) && isReceiverData) {
										migrator.invoke();
										isJustMigrated = true;
										isReceiverData = false;
										pointsAdder.invoke();
									}
									gpsBinParser.parse();
									migrationJobs.add(gpsBinParser);
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
							case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
								if (gamBinParser != null) {
									if (migrationJobs.contains(gamBinParser) && isReceiverData) {
										migrator.invoke();
										isJustMigrated = true;
										isReceiverData = false;
										pointsAdder.invoke();
									}
									gamBinParser.parse();
									migrationJobs.add(gamBinParser);
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
								if (eamBinParser != null)  {
									if (migrationJobs.contains(eamBinParser) && isReceiverData) {
										migrator.invoke();
										isJustMigrated = true;
										isReceiverData = false;
										pointsAdder.invoke();
									}
									eamBinParser.parse();
									migrationJobs.add(eamBinParser);
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
								if (escBinParser != null)  {
									if (migrationJobs.contains(escBinParser) && isReceiverData) {
										migrator.invoke();
										isJustMigrated = true;
										isReceiverData = false;
										pointsAdder.invoke();
									}
									escBinParser.parse();
									migrationJobs.add(escBinParser);
								}
								break;
							}

							if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isReceiverData " + isReceiverData + " migrationJobs " + migrationJobs);
						}

						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "logCountVario = " + logCountVario + " logCountGPS = " + logCountGPS + " logCountGeneral = " + logCountGAM + " logCountElectric = " + logCountEAM);
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
						pointsAdder.invoke();
						isReceiverData = false;
					} else if (chnBinParser != null && !isJustMigrated) {
						pointsAdder.invoke();
					}
					isJustMigrated = false;

					bufCopier.copyToBuffer();
					timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;// add default time step from log record of 10 msec
				} else { //skip empty block, but add time step
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

					((RcvBinParser) rcvBinParser).trackPackageLoss(false);
					if (chnBinParser != null) {
						chnBinParser.parse();
						pointsAdder.invoke();
					}
					timeSteps_ms[BinParser.TIMESTEP_INDEX] += 10;
				}
			} else if (!isTextModusSignaled) {
				isTextModusSignaled = true;
			}
		}

		if (doFullRead) {
			PackageLoss lostPackages  = ((RcvBinParser) rcvBinParser).getLostPackages();
			Integer[] scores = getScores(lostPackages, histoRandomSample,  truss.getVault());
			device.calculateInactiveRecords(tmpRecordSet);
			device.updateVisibilityStatus(tmpRecordSet, true);
			truss.promoteTruss(tmpRecordSet, scores);
			finishTimer.invoke();
			writeFinalLog(isTextModusSignaled, lostPackages, histoRandomSample, truss.getVault());
			// reduce memory consumption in advance to the garbage collection
			tmpRecordSet.cleanup();
		}
		log.log(Level.FINER, " > ends <  doFullRead=", doFullRead); //$NON-NLS-1$
	}

}
