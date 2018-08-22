
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
    					2016,2017,2018 Thomas Eickert
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
import gde.device.ScoreLabelTypes;
import gde.device.graupner.HoTTAdapter.PickerParameters;
import gde.device.graupner.HoTTAdapter.Sensor;
import gde.device.graupner.HoTTbinReader.BinParser;
import gde.device.graupner.HoTTbinReader.ChnBinParser;
import gde.device.graupner.HoTTbinReader.EamBinParser;
import gde.device.graupner.HoTTbinReader.EscBinParser;
import gde.device.graupner.HoTTbinReader.GamBinParser;
import gde.device.graupner.HoTTbinReader.GpsBinParser;
import gde.device.graupner.HoTTbinReader.InfoParser;
import gde.device.graupner.HoTTbinReader.RcvBinParser;
import gde.device.graupner.HoTTbinReader.SdLogFormat;
import gde.device.graupner.HoTTbinReader.SdLogInputStream;
import gde.device.graupner.HoTTbinReader.VarBinParser;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.VaultCollector;
import gde.histo.device.UniversalSampler;
import gde.io.DataParser;
import gde.log.Level;
import gde.utils.StringHelper;

/**
 * Read Graupner HoTT binary data for history analysis.
 * Read one single channel only and provide data in a recordset.
 * For small files (around 1 minute) no measurements are added to the recordset.
 * Support sampling to maximize the throughput.
 * @author Thomas Eickert
 */
public class HoTTbinHistoReader {
	private static final String	$CLASS_NAME					= HoTTbinHistoReader.class.getName();
	private static final Logger	log									= Logger.getLogger(HoTTbinHistoReader.$CLASS_NAME);

	/**
	 * HoTT logs data rate defined by the channel log
	 */
	protected final static int	RECORD_TIMESPAN_MS	= 10;
	protected final static int	DATA_BLOCK_SIZE			= 64;

	@FunctionalInterface
	interface Procedure {
		void invoke();
	}

	protected final PickerParameters pickerParameters;
	protected final Analyzer				analyzer;
	protected final boolean					isChannelsChannelEnabled;
	protected final boolean					isFilterEnabled;
	protected final boolean					isFilterTextModus;
	protected final Procedure				initTimer, readTimer, reviewTimer, addTimer, pickTimer, finishTimer;

	protected long									nanoTime;
	protected long									currentTime, initiateTime, readTime, reviewTime, addTime, pickTime, finishTime, lastTime;
	/**
	 * The detected sensors including the receiver but without 'channel'
	 */
	protected EnumSet<Sensor>				detectedSensors;
	protected RecordSet							tmpRecordSet;
	protected VaultCollector				truss;

	public HoTTbinHistoReader(PickerParameters pickerParameters) {
		this.pickerParameters = new PickerParameters(pickerParameters);
		this.pickerParameters.isFilterEnabled = true;

		this.analyzer = pickerParameters.analyzer;
		this.isChannelsChannelEnabled = this.analyzer.getActiveChannel().getNumber() == Sensor.CHANNEL.getChannelNumber();
		this.isFilterEnabled = true;
		this.isFilterTextModus = true;
		this.detectedSensors = EnumSet.noneOf(Sensor.class);
		if (log.isLoggable(Level.TIME)) {
			initTimer = () -> {
				currentTime = System.nanoTime();
				initiateTime += currentTime - lastTime;
				lastTime = currentTime;
			};
			readTimer = () -> {
				currentTime = System.nanoTime();
				readTime += currentTime - lastTime;
				lastTime = currentTime;
			};
			reviewTimer = () -> {
				currentTime = System.nanoTime();
				reviewTime += currentTime - lastTime;
				lastTime = currentTime;
			};
			addTimer = () -> {
				currentTime = System.nanoTime();
				addTime += currentTime - lastTime;
				lastTime = currentTime;
			};
			pickTimer = () -> {
				currentTime = System.nanoTime();
				pickTime += currentTime - lastTime;
				lastTime = currentTime;
			};
			finishTimer = () -> {
				currentTime = System.nanoTime();
				finishTime += currentTime - lastTime;
				lastTime = currentTime;
			};
		} else {
			initTimer = readTimer = reviewTimer = addTimer = pickTimer = finishTimer = () -> {};
		}
	}

	/**
	 * @param inputStream for retrieving the file info and for loading the log data
	 * @param newTruss which is promoted to a full vault object if the file has a minimum length.
	 */
	public void read(Supplier<InputStream> inputStream, VaultCollector newTruss) throws IOException, DataTypeException, DataInconsitsentException {
		if (newTruss.getVault().getLogFileLength() <= HoTTbinReader.NUMBER_LOG_RECORDS_MIN * HoTTbinHistoReader.DATA_BLOCK_SIZE) return;

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
	 * provide data in a histo tmpRecordSet.
	 */
	private void read(Supplier<InputStream> inputStream) throws IOException, DataTypeException, DataInconsitsentException {
		HashMap<String, String> header = null;
		HoTTAdapter device = (HoTTAdapter) analyzer.getActiveDevice();
		ExtendedVault vault = truss.getVault();
		long numberDatablocks = vault.getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE;
		tmpRecordSet = RecordSet.createRecordSet(vault.getLogRecordsetBaseName(), analyzer, analyzer.getActiveChannel().getNumber(), true, true, false);
		tmpRecordSet.setStartTimeStamp(HoTTbinReader.getStartTimeStamp(vault.getLoadFileAsPath().getFileName().toString(), vault.getLogFileLastModified(), numberDatablocks));
		tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", tmpRecordSet.getStartTimeStamp())); //$NON-NLS-1$
		tmpRecordSet.descriptionAppendFilename(vault.getLoadFileAsPath().getFileName().toString());
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, " recordSetBaseName=" + vault.getLogRecordsetBaseName()); //$NON-NLS-1$

		try (BufferedInputStream info_in = new BufferedInputStream(inputStream.get())) {
			header = new InfoParser((s) -> {} ).getFileInfo(info_in, vault.getLoadFilePath(), vault.getLogFileLength());
		} catch (DataTypeException e) {
			log.log(Level.WARNING, String.format("%s  %s", e.getMessage(), vault.getLoadFilePath()));
		}
		if (header == null || header.isEmpty()) return;
		detectedSensors = Sensor.getSetFromDetected(header.get(HoTTAdapter.DETECTED_SENSOR));
		if (!Sensor.getChannelNumbers(detectedSensors).contains(truss.getVault().getVaultChannelNumber())) return;


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
	 * allocates only one single recordset for the active channel, so HoTTAdapter.isChannelsChannelEnabled does not take any effect.
	 * no progress bar support and no channel data modifications.
	 * @param data_in
	 * @param initializeBlocks if this number is greater than zero, the min/max values are initialized
	 * @param maxPoints utilizes the minMax values from a previous run and thus reduces oversampling
	 * @param minPoints utilizes the minMax values from a previous run and thus reduces oversampling
	 * @return the sampler object holding the last value sets and the min/max value sets
	 */
	private UniversalSampler readSingle(InputStream data_in, int initializeBlocks, int[] maxPoints, int[] minPoints) throws DataInconsitsentException, IOException {
		byte[] buf = new byte[HoTTbinHistoReader.DATA_BLOCK_SIZE];
		byte[] buf0 = new byte[30];
		byte[] buf1 = new byte[30];
		byte[] buf2 = new byte[30];
		byte[] buf3 = new byte[30];
		byte[] buf4 = new byte[30];
		boolean isBuf1Ready = false, isBuf2Ready = false, isBuf3Ready = false, isBuf4Ready = false;
		long[] timeSteps_ms = new long[] { 0 };
		boolean isSensorData = false;
		boolean	isTextModusSignaled	= false;
		BinParser binParser;
		UniversalSampler histoRandomSample = null;
		HoTTAdapter device = (HoTTAdapter) analyzer.getActiveDevice();
		int activeChannelNumber = device.channels.getActiveChannelNumber();
		if (activeChannelNumber == Sensor.RECEIVER.getChannelNumber()) {
			binParser = Sensor.RECEIVER.createBinParser(pickerParameters, timeSteps_ms, new byte[][] { buf });
		} else if (activeChannelNumber == Sensor.CHANNEL.getChannelNumber()) {
			binParser = Sensor.CHANNEL.createBinParser(pickerParameters, timeSteps_ms, new byte[][] { buf });
		} else if (activeChannelNumber == Sensor.VARIO.getChannelNumber() && detectedSensors.contains(Sensor.VARIO)) {
			binParser = Sensor.VARIO.createBinParser(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2 });
		} else if (activeChannelNumber == Sensor.GPS.getChannelNumber() && detectedSensors.contains(Sensor.GPS)) {
			binParser = Sensor.GPS.createBinParser(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3 });
		} else if (activeChannelNumber == Sensor.GAM.getChannelNumber() && detectedSensors.contains(Sensor.GAM)) {
			binParser = Sensor.GAM.createBinParser(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else if (activeChannelNumber == Sensor.EAM.getChannelNumber() && detectedSensors.contains(Sensor.EAM)) {
			binParser = Sensor.EAM.createBinParser(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else if (activeChannelNumber == Sensor.ESC.getChannelNumber() && detectedSensors.contains(Sensor.ESC)) {
			binParser = Sensor.ESC.createBinParser(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3 });
		} else {
			throw new UnsupportedOperationException();
		}
		histoRandomSample = UniversalSampler.createSampler(activeChannelNumber, binParser.getPoints(), RECORD_TIMESPAN_MS, analyzer);
		histoRandomSample.setMaxMinPoints(maxPoints, minPoints);
		// read all the data blocks from the file, parse only for the active channel
		initTimer.invoke();
		// instead of HoTTAdapter setting
		boolean doFullRead = initializeBlocks <= 0;
		boolean doDataSkip = detectedSensors.size() == 1 && !isChannelsChannelEnabled;
		int datablocksLimit = (doFullRead ? (int) truss.getVault().getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE : initializeBlocks) / (doDataSkip ? 10 : 1);
		for (int i = 0; i < datablocksLimit; i++) {
			// if (log.isLoggable(Level.TIME))
			// log.log(Level.TIME, String.format("markpos: %,9d i: %,9d ", data_in.markpos, i));
			data_in.read(buf);
			if (log.isLoggable(Level.FINE) && i % 10 == 0) {
				log.log(Level.FINE, StringHelper.fourDigitsRunningNumber(buf.length));
				log.log(Level.FINE, StringHelper.byte2Hex4CharString(buf, buf.length));
			}

			if (!this.isFilterTextModus || (buf[6] & 0x01) == 0) { // switch into text modus
				if (buf[33] >= 0 && buf[33] <= 4 && buf[3] != 0 && buf[4] != 0) { // buf 3, 4, tx,rx
					if (log.isLoggable(Level.FINE))
						log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", buf[7], buf[33])); //$NON-NLS-1$
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, StringHelper.byte2Hex2CharString(new byte[] { buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(buf[7], false));
					if (binParser instanceof RcvBinParser) {
						((RcvBinParser) binParser).trackPackageLoss(true);
						if (buf[33] == 0 && (buf[38] & 0x80) != 128 && DataParser.parse2Short(buf, 40) >= 0) {
							binParser.parse();
							readTimer.invoke();
							boolean isValidSample = histoRandomSample.capturePoints(binParser.getTimeStep_ms());
							reviewTimer.invoke();
							if (isValidSample && doFullRead) {
								tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
								addTimer.invoke();
								pickTimer.invoke();
							}
						}
					} else if (binParser instanceof ChnBinParser) {
						binParser.parse();
						readTimer.invoke();
						boolean isValidSample = histoRandomSample.capturePoints(binParser.getTimeStep_ms());
						reviewTimer.invoke();
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							addTimer.invoke();
							pickTimer.invoke();
						}
					}
					if (doDataSkip) {
						for (int j = 0; j < 9; j++) {
							data_in.read(buf);
							timeSteps_ms[BinParser.TIMESTEP_INDEX] += RECORD_TIMESPAN_MS;
						}
						isSensorData = true;
					}
					// fill data block 0 receiver voltage and temperature
					if (buf[33] == 0 && DataParser.parse2Short(buf, 0) != 0) {
						System.arraycopy(buf, 34, buf0, 0, buf0.length);
					}

					timeSteps_ms[BinParser.TIMESTEP_INDEX] += RECORD_TIMESPAN_MS;

					// log.log(Level.OFF, "sensor type ID = " + StringHelper.byte2Hex2CharString(new byte[] {(byte) (HoTTbinHistoReader.buf[7] & 0xFF)}, 1));
					switch ((byte) (buf[7] & 0xFF)) {
					case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
					case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
						if (detectedSensors.contains(Sensor.VARIO)) {
							// fill data block 1 to 2
							if (buf[33] == 1) {
								isBuf1Ready = true;
								System.arraycopy(buf, 34, buf1, 0, buf1.length);
							}
							if (buf[33] == 2) {
								isBuf2Ready = true;
								System.arraycopy(buf, 34, buf2, 0, buf2.length);
							}
							if (isBuf1Ready && isBuf2Ready) {
								if (binParser instanceof VarBinParser) {
									if (binParser.parse()) {
										readTimer.invoke();
										boolean isValidSample = histoRandomSample.capturePoints(binParser.getTimeStep_ms());
										reviewTimer.invoke();
										if (isValidSample && doFullRead) {
											tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											addTimer.invoke();
											pickTimer.invoke();
										}
									}
								}
								isSensorData = true;
								isBuf1Ready = isBuf2Ready = false;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_GPS_115200:
					case HoTTAdapter.SENSOR_TYPE_GPS_19200:
						if (detectedSensors.contains(Sensor.GPS)) {
							// fill data block 1 to 3
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
								if (binParser instanceof GpsBinParser) {
									if (binParser.parse()) {
										readTimer.invoke();
										boolean isValidSample = histoRandomSample.capturePoints(binParser.getTimeStep_ms());
										reviewTimer.invoke();
										if (isValidSample && doFullRead) {
											tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											addTimer.invoke();
											pickTimer.invoke();
										}
									}
								}
								isSensorData = true;
								isBuf1Ready = isBuf2Ready = isBuf3Ready = false;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
					case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
						if (detectedSensors.contains(Sensor.GAM)) {
							// fill data block 1 to 4
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
								if (binParser instanceof GamBinParser) {
									if (binParser.parse()) {
										readTimer.invoke();
										boolean isValidSample = histoRandomSample.capturePoints(binParser.getTimeStep_ms());
										reviewTimer.invoke();
										if (isValidSample && doFullRead) {
											tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											addTimer.invoke();
											pickTimer.invoke();
										}
									}
								}
								isSensorData = true;
								isBuf1Ready = isBuf2Ready = isBuf3Ready = isBuf4Ready = false;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
						if (detectedSensors.contains(Sensor.EAM)) {
							// fill data block 1 to 4
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
								if (binParser instanceof EamBinParser) {
									if (binParser.parse()) {
										readTimer.invoke();
										boolean isValidSample = histoRandomSample.capturePoints(binParser.getTimeStep_ms());
										reviewTimer.invoke();
										if (isValidSample && doFullRead) {
											tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											addTimer.invoke();
											pickTimer.invoke();
											tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
										}
									}
								}
								isSensorData = true;
								isBuf1Ready = isBuf2Ready = isBuf3Ready = isBuf4Ready = false;
							}
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
					case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
						if (detectedSensors.contains(Sensor.ESC)) {
							// fill data block 1 to 3
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
								if (binParser instanceof EscBinParser) {
									if (binParser.parse()) {
										readTimer.invoke();
										boolean isValidSample = histoRandomSample.capturePoints(binParser.getTimeStep_ms());
										reviewTimer.invoke();
										if (isValidSample && doFullRead) {
											tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
											addTimer.invoke();
											pickTimer.invoke();
										}
									}
								}
								isSensorData = true;
								isBuf1Ready = isBuf2Ready = isBuf3Ready = false;
							}
						}
						break;
					}

					if (isSensorData) {
						if (binParser instanceof RcvBinParser)
							isSensorData = !((RcvBinParser) binParser).updateLossStatistics();
						else
							isSensorData = false;
					}
				} else { // skip empty block, but add time step
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "-->> Found tx=rx=0 dBm"); //$NON-NLS-1$

					if (binParser instanceof RcvBinParser) {
						((RcvBinParser) binParser).trackPackageLoss(false);
					}

					if (binParser instanceof ChnBinParser) {
						binParser.parse();
						readTimer.invoke();
						boolean isValidSample = histoRandomSample.capturePoints(binParser.getTimeStep_ms());
						reviewTimer.invoke();
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							addTimer.invoke();
							pickTimer.invoke();
						}
					}

					timeSteps_ms[BinParser.TIMESTEP_INDEX] += RECORD_TIMESPAN_MS;
					// reset buffer to avoid mixing data >> 20 Jul 14, not any longer required due to protocol change requesting next sensor data block
					// buf1 = buf2 = buf3 = buf4 = null;
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
			if (binParser instanceof RcvBinParser) {
			scores[ScoreLabelTypes.LOST_PACKAGES.ordinal()] = binParser == null ? 0 : ((RcvBinParser) binParser).getCountPackageLoss();
			scores[ScoreLabelTypes.LOST_PACKAGES_PER_MILLE.ordinal()] = binParser == null || tmpRecordSet.getMaxTime_ms() <= 0 ? 0 : (int) (((RcvBinParser) binParser).getCountPackageLoss() / tmpRecordSet.getMaxTime_ms() * 1000. * RECORD_TIMESPAN_MS) * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_AVG_MS.ordinal()] = binParser == null ? 0 : (int) ((RcvBinParser) binParser).getLostPackages().getAvgValue() * RECORD_TIMESPAN_MS * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_MAX_MS.ordinal()] = binParser == null ? 0 : ((RcvBinParser) binParser).getLostPackages().getMaxValue() * RECORD_TIMESPAN_MS * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_MIN_MS.ordinal()] = binParser == null ? 0 : ((RcvBinParser) binParser).getLostPackages().getMinValue() * RECORD_TIMESPAN_MS * 1000;
			scores[ScoreLabelTypes.LOST_PACKAGES_SIGMA_MS.ordinal()] = binParser == null ? 0 : (int) ((RcvBinParser) binParser).getLostPackages().getSigmaValue() * RECORD_TIMESPAN_MS * 1000;
			}
			scores[ScoreLabelTypes.SENSORS.ordinal()] = (detectedSensors.size() - 1) * 1000; // subtract Receiver, do not subtract Channel --- is not included
			scores[ScoreLabelTypes.SENSOR_VARIO.ordinal()] = detectedSensors.contains(Sensor.VARIO) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GPS.ordinal()] = detectedSensors.contains(Sensor.GPS) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GAM.ordinal()] = detectedSensors.contains(Sensor.GAM) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_EAM.ordinal()] = detectedSensors.contains(Sensor.EAM) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_ESC.ordinal()] = detectedSensors.contains(Sensor.ESC) ? 1000 : 0;
			scores[ScoreLabelTypes.LOG_DATA_VERSION.ordinal()] = (int) (truss.getVault().getLoadFileAsPath().getFileName().toString().startsWith(GDE.TEMP_FILE_STEM.substring(0, 1)) ? 4.2 * 1000 : 4.0 * 1000); // V4 with and without container
			scores[ScoreLabelTypes.LOG_DATA_EXPLORER_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_FILE_VERSION.ordinal()] = 0;
			scores[ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal()] = histoRandomSample.getReadingCount() * HoTTbinHistoReader.DATA_BLOCK_SIZE;
			scores[ScoreLabelTypes.LOG_FILE_BYTES.ordinal()] = (int) vault.getLogFileLength();
			scores[ScoreLabelTypes.LOG_FILE_RECORD_SETS.ordinal()] = (detectedSensors.size() + 1) * 1000; // +1 for channel
			scores[ScoreLabelTypes.ELAPSED_HISTO_RECORD_SET_MS.ordinal()] = (int) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - nanoTime); // do not multiply by 1000 as usual, this is the conversion from microseconds to ms
			// no display tmpRecordSet.syncScaleOfSyncableRecords();

			tmpRecordSet.setSaved(true);
			device.calculateInactiveRecords(tmpRecordSet);
			device.updateVisibilityStatus(tmpRecordSet, true);
			if (log.isLoggable(Level.INFO)) log.log(Level.INFO, String.format("%s > packages:%,9d  readings:%,9d  sampled:%,9d  overSampled:%4d", tmpRecordSet.getChannelConfigName(), vault.getLogFileLength() //$NON-NLS-1$
					/ HoTTbinHistoReader.DATA_BLOCK_SIZE, histoRandomSample.getReadingCount(), tmpRecordSet.getRecordDataSize(true), histoRandomSample.getOverSamplingCount()));
			finishTimer.invoke();
			if (log.isLoggable(Level.TIME)) log.log(Level.TIME,
					String.format("initiateTime: %,7d  readTime: %,7d  reviewTime: %,7d  addTime: %,7d  pickTime: %,7d  finishTime: %,7d", TimeUnit.NANOSECONDS.toMillis(initiateTime), //$NON-NLS-1$
							TimeUnit.NANOSECONDS.toMillis(readTime), TimeUnit.NANOSECONDS.toMillis(reviewTime), TimeUnit.NANOSECONDS.toMillis(addTime), TimeUnit.NANOSECONDS.toMillis(pickTime),
							TimeUnit.NANOSECONDS.toMillis(finishTime)));
			if (binParser instanceof RcvBinParser) {
				if (tmpRecordSet.getMaxTime_ms() > 0) {
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("lost:%,9d perMille:%,4d total:%,9d   lostMax_ms:%,4d lostAvg_ms=%,4d", ((RcvBinParser) binParser).getCountPackageLoss(), //$NON-NLS-1$
							(int) (((RcvBinParser) binParser).getCountPackageLoss() / tmpRecordSet.getMaxTime_ms() * 1000. * RECORD_TIMESPAN_MS), truss.getVault().getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE, ((RcvBinParser) binParser).getLostPackages().getMaxValue() * 10,
							(int) ((RcvBinParser) binParser).getLostPackages().getAvgValue() * 10));
				} else {
					log.log(Level.WARNING, String.format("RecordSet with unidentified data.  fileLength=%,11d   isTextModusSignaled=%b", vault.getLogFileLength(), isTextModusSignaled)); //$NON-NLS-1$
				}
			}

			truss.promoteTruss(tmpRecordSet, scores);
			// reduce memory consumption in advance to the garbage collection
			tmpRecordSet.cleanup();
		} else {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("doFullRead=%b > ends", doFullRead)); //$NON-NLS-1$
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
	 */
	private UniversalSampler readMultiple(InputStream data_in, int initializeBlocks, int[] maxPoints, int[] minPoints) throws IOException, DataInconsitsentException {
		byte[] buf = new byte[HoTTbinHistoReader.DATA_BLOCK_SIZE];
		byte[] buf0 = new byte[30];
		byte[] buf1 = new byte[30];
		byte[] buf2 = new byte[30];
		byte[] buf3 = new byte[30];
		byte[] buf4 = new byte[30];
		long[] timeSteps_ms = new long[] { 0 };
		byte actualSensor = -1, lastSensor = -1;
		int logCountVario = 0, logCountGPS = 0, logCountGeneral = 0, logCountElectric = 0, logCountSpeedControl = 0;
		boolean isSensorData = false;
		boolean	isTextModusSignaled	= false;
		BinParser binParser;
		UniversalSampler histoRandomSample = null;
		HoTTAdapter device = (HoTTAdapter) analyzer.getActiveDevice();
		int activeChannelNumber = device.channels.getActiveChannelNumber();
		if (activeChannelNumber == Sensor.RECEIVER.getChannelNumber()) {
			binParser = Sensor.RECEIVER.createBinParser(pickerParameters, timeSteps_ms, new byte[][] { buf });
		} else if (activeChannelNumber == Sensor.CHANNEL.getChannelNumber()) {
			binParser = Sensor.CHANNEL.createBinParser(pickerParameters, timeSteps_ms, new byte[][] { buf });
		} else if (activeChannelNumber == Sensor.VARIO.getChannelNumber() && detectedSensors.contains(Sensor.VARIO)) {
			binParser = Sensor.VARIO.createBinParser(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2 });
		} else if (activeChannelNumber == Sensor.GPS.getChannelNumber() && detectedSensors.contains(Sensor.GPS)) {
			binParser = Sensor.GPS.createBinParser(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3 });
		} else if (activeChannelNumber == Sensor.GAM.getChannelNumber() && detectedSensors.contains(Sensor.GAM)) {
			binParser = Sensor.GAM.createBinParser(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else if (activeChannelNumber == Sensor.EAM.getChannelNumber() && detectedSensors.contains(Sensor.EAM)) {
			binParser = Sensor.EAM.createBinParser(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3, buf4 });
		} else if (activeChannelNumber == Sensor.ESC.getChannelNumber() && detectedSensors.contains(Sensor.ESC)) {
			binParser = Sensor.ESC.createBinParser(pickerParameters, timeSteps_ms, new byte[][] { buf0, buf1, buf2, buf3 });
		} else {
			throw new UnsupportedOperationException();
		}
		histoRandomSample = UniversalSampler.createSampler(activeChannelNumber, binParser.getPoints(), RECORD_TIMESPAN_MS, analyzer);
		histoRandomSample.setMaxMinPoints(maxPoints, minPoints);
		// read all the data blocks from the file, parse only for the active channel
		initTimer.invoke();
		boolean doFullRead = initializeBlocks <= 0;
		int datablocksLimit = (doFullRead ? (int) truss.getVault().getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE : initializeBlocks);
		for (int i = 0; i < datablocksLimit; i++) {
			data_in.read(buf);
			if (log.isLoggable(Level.FINEST) && i % 10 == 0) {
				log.log(Level.FINEST, StringHelper.fourDigitsRunningNumber(buf.length));
				log.log(Level.FINEST, StringHelper.byte2Hex4CharString(buf, buf.length));
			}

			if (!this.isFilterTextModus || (buf[6] & 0x01) == 0) { // switch into text modus
				if (buf[33] >= 0 && buf[33] <= 4 && buf[3] != 0 && buf[4] != 0) { // buf 3, 4, tx,rx
					if (log.isLoggable(Level.FINE))
						log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", buf[7], buf[33])); //$NON-NLS-1$
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, StringHelper.byte2Hex2CharString(new byte[] { buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(buf[7], false));

					if (binParser instanceof RcvBinParser) {
						((RcvBinParser) binParser).trackPackageLoss(true);
						if (buf[33] == 0 && (buf[38] & 0x80) != 128 && DataParser.parse2Short(buf, 40) >= 0) {
							binParser.parse();
							readTimer.invoke();
							boolean isValidSample = histoRandomSample.capturePoints(binParser.getTimeStep_ms());
							reviewTimer.invoke();
							if (isValidSample && doFullRead) {
								tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
								addTimer.invoke();
								pickTimer.invoke();
							}
						}
					} else if (binParser instanceof ChnBinParser) {
						binParser.parse();
						readTimer.invoke();
						boolean isValidSample = histoRandomSample.capturePoints(binParser.getTimeStep_ms());
						reviewTimer.invoke();
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							addTimer.invoke();
							pickTimer.invoke();
						}
					}

					timeSteps_ms[BinParser.TIMESTEP_INDEX] += RECORD_TIMESPAN_MS;

					// detect sensor switch
					if (actualSensor == -1)
						lastSensor = actualSensor = (byte) (buf[7] & 0xFF);
					else
						actualSensor = (byte) (buf[7] & 0xFF);

					if (actualSensor != lastSensor) {
						// write data just after sensor switch
						if (logCountVario >= 3 || logCountGPS >= 4 || logCountGeneral >= 5 || logCountElectric >= 5 || logCountSpeedControl >= 5) {
							switch (lastSensor) {
							case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
							case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
								if (detectedSensors.contains(Sensor.VARIO)) {
									if (binParser instanceof VarBinParser) {
										if (binParser.parse()) {
											readTimer.invoke();
											boolean isValidSample = histoRandomSample.capturePoints(binParser.getTimeStep_ms());
											reviewTimer.invoke();
											if (isValidSample && doFullRead) {
												tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
												addTimer.invoke();
												pickTimer.invoke();
											}
										}
									}
									isSensorData = true;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GPS_115200:
							case HoTTAdapter.SENSOR_TYPE_GPS_19200:
								if (detectedSensors.contains(Sensor.GPS)) {
									if (binParser instanceof GpsBinParser) {
										if (binParser.parse()) {
											readTimer.invoke();
											boolean isValidSample = histoRandomSample.capturePoints(binParser.getTimeStep_ms());
											reviewTimer.invoke();
											if (isValidSample && doFullRead) {
												tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
												addTimer.invoke();
												pickTimer.invoke();
											}
										}
									}
									isSensorData = true;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
							case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
								if (detectedSensors.contains(Sensor.GAM)) {
									if (binParser instanceof GamBinParser) {
										if (binParser.parse()) {
											readTimer.invoke();
											boolean isValidSample = histoRandomSample.capturePoints( binParser.getTimeStep_ms());
											reviewTimer.invoke();
											if (isValidSample && doFullRead) {
												tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
												addTimer.invoke();
												pickTimer.invoke();
											}
										}
									}
									isSensorData = true;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
								if (detectedSensors.contains(Sensor.EAM)) {
									if (binParser instanceof EamBinParser) {
										if (binParser.parse()) {
											readTimer.invoke();
											boolean isValidSample = histoRandomSample.capturePoints(binParser.getTimeStep_ms());
											reviewTimer.invoke();
											if (isValidSample && doFullRead) {
												tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
												addTimer.invoke();
												pickTimer.invoke();
											}
										}
									}
									isSensorData = true;
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
								if (detectedSensors.contains(Sensor.ESC)) {
									if (binParser instanceof EscBinParser) {
										if (binParser.parse()) {
											readTimer.invoke();
											boolean isValidSample = histoRandomSample.capturePoints(binParser.getTimeStep_ms());
											reviewTimer.invoke();
											if (isValidSample && doFullRead) {
												tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
												addTimer.invoke();
												pickTimer.invoke();
											}
										}
									}
									isSensorData = true;
								}
								break;
							}
						}

						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "logCountVario = " + logCountVario //$NON-NLS-1$
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
					if (buf[33] == 0 && DataParser.parse2Short(buf, 0) != 0) {
						System.arraycopy(buf, 34, buf0, 0, buf0.length);
					} else if (buf[33] == 1) {
						System.arraycopy(buf, 34, buf1, 0, buf1.length);
					} else if (buf[33] == 2) {
						System.arraycopy(buf, 34, buf2, 0, buf2.length);
					} else if (buf[33] == 3) {
						System.arraycopy(buf, 34, buf3, 0, buf3.length);
					} else if (buf[33] == 4) {
						System.arraycopy(buf, 34, buf4, 0, buf4.length);
					}

					if (isSensorData) {
						if (binParser instanceof RcvBinParser)
							isSensorData = !((RcvBinParser) binParser).updateLossStatistics();
						else
							isSensorData = false;
					}
				} else { // tx,rx == 0
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "-->> Found tx=rx=0 dBm"); //$NON-NLS-1$

					if (binParser instanceof RcvBinParser) {
						((RcvBinParser) binParser).trackPackageLoss(false);
					}

					if (binParser instanceof ChnBinParser) {
						binParser.parse();
						readTimer.invoke();
						boolean isValidSample = histoRandomSample.capturePoints(binParser.getTimeStep_ms());
						reviewTimer.invoke();
						if (isValidSample && doFullRead) {
							tmpRecordSet.addPoints(histoRandomSample.getSamplePoints(), histoRandomSample.getSampleTimeStep_ms());
							addTimer.invoke();
							pickTimer.invoke();
						}
					}

					timeSteps_ms[BinParser.TIMESTEP_INDEX] += RECORD_TIMESPAN_MS;
					// reset buffer to avoid mixing data >> 20 Jul 14, not any longer required due to protocol change requesting next sensor data block
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
			if (binParser instanceof RcvBinParser) {
				scores[ScoreLabelTypes.LOST_PACKAGES.ordinal()] = ((RcvBinParser) binParser).getCountPackageLoss();
				scores[ScoreLabelTypes.LOST_PACKAGES_PER_MILLE.ordinal()] = tmpRecordSet.getMaxTime_ms() <= 0 ? 0 : (int) (((RcvBinParser) binParser).getCountPackageLoss() / tmpRecordSet.getMaxTime_ms() * 1000. * RECORD_TIMESPAN_MS) * 1000;
				scores[ScoreLabelTypes.LOST_PACKAGES_AVG_MS.ordinal()] = (int) ((RcvBinParser) binParser).getLostPackages().getAvgValue() * RECORD_TIMESPAN_MS * 1000;
				scores[ScoreLabelTypes.LOST_PACKAGES_MAX_MS.ordinal()] = ((RcvBinParser) binParser).getLostPackages().getMaxValue() * RECORD_TIMESPAN_MS * 1000;
				scores[ScoreLabelTypes.LOST_PACKAGES_MIN_MS.ordinal()] = ((RcvBinParser) binParser).getLostPackages().getMinValue() * RECORD_TIMESPAN_MS * 1000;
				scores[ScoreLabelTypes.LOST_PACKAGES_SIGMA_MS.ordinal()] = (int) ((RcvBinParser) binParser).getLostPackages().getSigmaValue() * RECORD_TIMESPAN_MS * 1000;
			}
			scores[ScoreLabelTypes.SENSORS.ordinal()] = (detectedSensors.size() - 1) * 1000; // subtract Receiver, do not subtract Channel --- is not included
			scores[ScoreLabelTypes.SENSOR_VARIO.ordinal()] = detectedSensors.contains(Sensor.VARIO) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GPS.ordinal()] = detectedSensors.contains(Sensor.GPS) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_GAM.ordinal()] = detectedSensors.contains(Sensor.GAM) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_EAM.ordinal()] = detectedSensors.contains(Sensor.EAM) ? 1000 : 0;
			scores[ScoreLabelTypes.SENSOR_ESC.ordinal()] = detectedSensors.contains(Sensor.ESC) ? 1000 : 0;
			scores[ScoreLabelTypes.LOG_DATA_VERSION.ordinal()] = (int) (truss.getVault().getLoadFilePath().startsWith(GDE.TEMP_FILE_STEM.substring(0, 1)) ? 4.2 * 1000 : 4.0 * 1000); // V4 with and without container
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
			finishTimer.invoke();
			if (log.isLoggable(Level.TIME)) log.log(Level.TIME,
					String.format("initiateTime: %,7d  readTime: %,7d  reviewTime: %,7d  addTime: %,7d  pickTime: %,7d  finishTime: %,7d", TimeUnit.NANOSECONDS.toMillis(initiateTime), //$NON-NLS-1$
							TimeUnit.NANOSECONDS.toMillis(readTime), TimeUnit.NANOSECONDS.toMillis(reviewTime), TimeUnit.NANOSECONDS.toMillis(addTime), TimeUnit.NANOSECONDS.toMillis(pickTime),
							TimeUnit.NANOSECONDS.toMillis(finishTime)));
			if (binParser instanceof RcvBinParser) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("lost:%,9d perMille:%,4d total:%,9d   lostMax_ms:%,4d lostAvg_ms=%,4d", ((RcvBinParser) binParser).getCountPackageLoss(), //$NON-NLS-1$
						(int) (((RcvBinParser) binParser).getCountPackageLoss() / tmpRecordSet.getTime_ms((int) truss.getVault().getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE - 1) * 1000. * RECORD_TIMESPAN_MS), truss.getVault().getLogFileLength() / HoTTbinHistoReader.DATA_BLOCK_SIZE,
						((RcvBinParser) binParser).getLostPackages().getMaxValue() * 10, (int) ((RcvBinParser) binParser).getLostPackages().getAvgValue() * 10));
			}

			truss.promoteTruss(tmpRecordSet, scores);
			// reduce memory consumption in advance to the garbage collection
			tmpRecordSet.cleanup();
		} else {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("doFullRead=%b > ends", doFullRead)); //$NON-NLS-1$
		}
		return histoRandomSample;
	}

}

