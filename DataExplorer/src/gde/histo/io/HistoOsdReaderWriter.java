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

    Copyright (c) 2016,2017 Thomas Eickert
****************************************************************************************/
package gde.histo.io;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import gde.GDE;
import gde.data.Channel;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.ScoreLabelTypes;
import gde.exception.DataInconsitsentException;
import gde.exception.NotSupportedFileFormatException;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.VaultCollector;
import gde.histo.device.IHistoDevice;
import gde.histo.recordings.TrailRecordSet;
import gde.io.OsdReaderWriter;
import gde.log.Level;
import gde.utils.StringHelper;

/**
 * Read the DataExplorer file format into histo recordsets.
 * @author Thomas Eickert
 */
public final class HistoOsdReaderWriter extends OsdReaderWriter {
	final private static String	$CLASS_NAME	= HistoOsdReaderWriter.class.getName();
	final private static Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * Identify an enhanced value for the start timestamp.
	 * @param file
	 * @param objectDirectory holds the validated object key from the parent directory or is empty
	 * @return the vaults skeletons created from the recordsets (for valid channels in the current device only)
	 * @throws NotSupportedFileFormatException
	 * @throws IOException
	 */
	public static List<VaultCollector> readTrusses(File file, String objectDirectory) throws IOException, NotSupportedFileFormatException {
		List<VaultCollector> trusses = new ArrayList<>();
		final HashMap<String, String> header = HistoOsdReaderWriter.getHeader(file.toString());
		final String logObjectKey = header.containsKey(GDE.OBJECT_KEY) ? header.get(GDE.OBJECT_KEY).intern() : GDE.STRING_EMPTY;
		final int fileVersion = Integer.valueOf(header.get(GDE.DATA_EXPLORER_FILE_VERSION).trim()).intValue();
		final int recordSetSize = Integer.valueOf(header.get(GDE.RECORD_SET_SIZE).trim()).intValue();

		for (int i = 0; i < Integer.valueOf(header.get(GDE.RECORD_SET_SIZE).trim()).intValue(); ++i) {
			StringBuilder sb = new StringBuilder();
			final String line = GDE.RECORD_SET_NAME + GDE.STRING_EMPTY + header.get(sb.append(i + 1).append(GDE.STRING_BLANK).append(GDE.RECORD_SET_NAME).toString());
			final HashMap<String, String> recordSetInfo = OsdReaderWriter.getRecordSetProperties(line);
			final String logRecordSetBaseName = String.format("%s%s%s", recordSetInfo.get(GDE.RECORD_SET_NAME), TrailRecordSet.BASE_NAME_SEPARATOR, recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME));
			final long enhancedStartTimeStamp_ms = HistoOsdReaderWriter.getStartTimestamp_ms(recordSetInfo);
			final Channel recordSetInfoChannel = OsdReaderWriter.getChannel(recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME));
			if (recordSetInfoChannel != null) {
				VaultCollector vaultCollector = new VaultCollector(objectDirectory, file, fileVersion, recordSetSize, i, logRecordSetBaseName, header.get(GDE.DEVICE_NAME), enhancedStartTimeStamp_ms,
						recordSetInfoChannel.getNumber(), logObjectKey);
				trusses.add(vaultCollector);
			}
		}
		log.log(Level.FINER, " " + trusses.size() + " identified in " + file.getPath()); //$NON-NLS-1$
		return trusses;
	}

	/**
	 * Read histo record sets from one single file.
	 * @param filePath
	 * @param trusses referencing a subset of the recordsets in the file
	 * @return histo vault list (may contain trusses without measurements, settlements and scores)
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 * @throws DataInconsitsentException
	 */
	public static List<ExtendedVault> readVaults(Path filePath, Collection<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException, DataInconsitsentException {
		List<ExtendedVault> histoVaults = new ArrayList<>();
		log.log(Level.FINE, "start " + filePath); //$NON-NLS-1$
		// build job list consisting of recordset ordinal and the corresponding truss
		Map<Integer, VaultCollector> recordSetTrusses = new TreeMap<>();
		for (VaultCollector truss : trusses) {
			if (truss.getVault().getLogFileAsPath().equals(filePath))
				recordSetTrusses.put(truss.getVault().getLogRecordSetOrdinal(), truss);
			else
				throw new UnsupportedOperationException("all trusses must carry the same logFilePath");
		}

		File file = filePath.toFile();
		ZipInputStream zip_input = new ZipInputStream(new FileInputStream(file));
		ZipEntry zip_entry = zip_input.getNextEntry();
		InputStream inputStream;
		if (zip_entry != null) {
			inputStream = zip_input;
		}
		else {
			zip_input.close();
			zip_input = null;
			inputStream = new FileInputStream(file);
		}

		try (DataInputStream data_in = new DataInputStream(inputStream)) { // closes the inputStream also
			final HashMap<String, String> header = HistoOsdReaderWriter.getHeader(filePath.toString());
			final double logDataExplorerVersion = header.containsKey(GDE.DATA_EXPLORER_FILE_VERSION) ? Double.parseDouble(header.get(GDE.DATA_EXPLORER_FILE_VERSION)) : 1.; // OpenSerialData version : 1
			final int numberRecordSets = Integer.parseInt(header.get(GDE.RECORD_SET_SIZE));

			while (!data_in.readUTF().startsWith(GDE.RECORD_SET_SIZE))
				log.log(Level.FINEST, "skip"); //$NON-NLS-1$
			final List<HashMap<String, String>> recordSetsInfo = OsdReaderWriter.readRecordSetsInfo4AllVersions(data_in, header);

			long startTimeNs = System.nanoTime();
			long unreadDataPointer = -1;
			for (int i = 0; i < recordSetsInfo.size(); i++) {
				HashMap<String, String> recordSetInfo = recordSetsInfo.get(i);
				final Channel recordSetInfoChannel = OsdReaderWriter.getChannel(recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME));
				String[] recordsProperties = StringHelper.splitString(recordSetInfo.get(GDE.RECORDS_PROPERTIES), Record.END_MARKER, GDE.RECORDS_PROPERTIES);
				int recordDataSize = Long.valueOf(recordSetInfo.get(GDE.RECORD_DATA_SIZE)).intValue();
				long recordSetDataPointer = Long.parseLong(recordSetInfo.get(GDE.RECORD_SET_DATA_POINTER));
				//				String recordSetProperties = recordSetInfo.get(GDE.RECORD_SET_PROPERTIES);
				//				HashMap<String, String> recordSetProps = StringHelper.splitString(recordSetProperties, Record.DELIMITER, RecordSet.propertyKeys);
				//				String[] cleanedMeasurementNames = HistoOsdReaderWriter.getMeasurementNames(recordsProperties, recordSetInfoChannel.getNumber());
				//				String[] recordKeys = HistoOsdReaderWriter.getRecordKeys(recordsProperties);
				//				String[] noneCalculationMeasurementNames = HistoOsdReaderWriter.application.getActiveDevice().getNoneCalculationMeasurementNames(recordSetInfoChannel.getNumber(), recordKeys);

				if (!recordSetTrusses.containsKey(i)) {
					// defer reading any unused recordsets until a recordset is actually required
					if (unreadDataPointer < 0) unreadDataPointer = recordSetDataPointer;
				}
				else {
					long nanoTime = System.nanoTime();
					if (unreadDataPointer > -1) {
						// make up all deferred readings in one single step
						long toSkip = recordSetDataPointer - unreadDataPointer;
						while (toSkip > 0) { // The skip method may, for a variety of reasons, end up skipping over some smaller number of bytes, possibly 0. The actual number of bytes skipped is returned.
							toSkip -= data_in.skip(toSkip);
							if (toSkip > 0) {
								if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "toSkip=" + toSkip); //$NON-NLS-1$
								if (data_in.available() == 0) throw new EOFException("recordDataSize / recordSetDataPointer do not match the actual file size"); //$NON-NLS-1$
							}
						}
						unreadDataPointer = -1;
					}
					RecordSet histoRecordSet = OsdReaderWriter.buildRecordSet(recordSetTrusses.get(i).getVault().getLogRecordsetBaseName(), recordSetTrusses.get(i).getVault().getLogChannelNumber(), recordSetInfo, false);
					String[] noneCalculationMeasurementNames = histoRecordSet.getNoneCalculationRecordNames();
					int numberRecordAndTimeStamp = noneCalculationMeasurementNames.length + (histoRecordSet.isTimeStepConstant() ? 0 : 1);

					byte[] buffer = new byte[GDE.SIZE_BYTES_INTEGER * numberRecordAndTimeStamp * recordDataSize];
					data_in.readFully(buffer);

					// setDeserializedProperties does not take all possible solutions to set the timestamp
					if (log.isLoggable(Level.INFO) && histoRecordSet.getStartTimeStamp() != HistoOsdReaderWriter.getStartTimestamp_ms(recordSetInfo))
						log.log(Level.INFO, "startTimeStamp rectified " + HistoOsdReaderWriter.getStartTimestamp_ms(recordSetInfo) + "  " + histoRecordSet.getStartTimeStamp()); //$NON-NLS-1$
					histoRecordSet.setStartTimeStamp(HistoOsdReaderWriter.getStartTimestamp_ms(recordSetInfo));
					histoRecordSet.setFileDataPointerAndSize(recordSetDataPointer, recordDataSize, GDE.SIZE_BYTES_INTEGER * numberRecordAndTimeStamp * recordDataSize);
					log.log(Level.FINE, recordSetInfoChannel.getName() + " recordDataSize=" + recordDataSize + "  recordSetDataPointer=" + recordSetDataPointer //$NON-NLS-1$ //$NON-NLS-2$
							+ "  numberRecordAndTimeStamp=" + numberRecordAndTimeStamp); //$NON-NLS-1$

					if (histoRecordSet.getDevice() instanceof IHistoDevice) {
						int[] maxPoints = new int[noneCalculationMeasurementNames.length];
						int[] minPoints = new int[noneCalculationMeasurementNames.length];
						String[] recordKeys = recordKeys = HistoOsdReaderWriter.getRecordKeys(recordsProperties);
						List<String> noneCalculationMeasurementNameList = Arrays.asList(noneCalculationMeasurementNames);
						for (int j = 0; j < recordKeys.length; j++) {
							int index = noneCalculationMeasurementNameList.indexOf(recordKeys[j]);
							if (index > -1) {
								HashMap<String, String> recordProperties = StringHelper.splitString(recordsProperties[j], Record.DELIMITER, Record.propertyKeys);
								maxPoints[index] = Integer.parseInt(recordProperties.get(Record.MAX_VALUE).trim());
								minPoints[index] = Integer.parseInt(recordProperties.get(Record.MIN_VALUE).trim());
							}
						}
						((IHistoDevice) histoRecordSet.getDevice()).setSampling(recordSetTrusses.get(i).getVault().getLogChannelNumber(), maxPoints, minPoints);
					}
					histoRecordSet.getDevice().addDataBufferAsRawDataPoints(histoRecordSet, buffer, recordDataSize, false);

					// extract additional data
					final String recordSetComment = recordSetInfo.get(GDE.RECORD_SET_COMMENT);
					final Double[] packagesLost = HistoOsdReaderWriter.parsePackageLoss(recordSetComment);
					final String[] sensors = HistoOsdReaderWriter.parseSensors(recordSetComment);
					final Double logDataVersion = HistoOsdReaderWriter.parseFirmware(recordSetComment);
					final Integer[] scores = new Integer[ScoreLabelTypes.VALUES.length];
					// values are multiplied by 1000 as this is the convention for internal values in order to avoid rounding errors for values below 1.0 (0.5 -> 0)
					// scores for duration and timestep values are filled in by the HistoRecordSet
					scores[ScoreLabelTypes.TOTAL_READINGS.ordinal()] = recordDataSize;
					scores[ScoreLabelTypes.TOTAL_PACKAGES.ordinal()] = packagesLost[0] != null && packagesLost[1] != null && packagesLost[1] != 0 ? (int) (packagesLost[0] * 100. / packagesLost[1]) : 0;
					// recalculating the following scores from raw data would be feasible
					scores[ScoreLabelTypes.LOST_PACKAGES.ordinal()] = packagesLost[0] != null ? (int) (packagesLost[0].intValue()) : 0;
					scores[ScoreLabelTypes.LOST_PACKAGES_PER_MILLE.ordinal()] = packagesLost[1] != null ? (int) (packagesLost[1] * 10. * 1000.) : 0; // percent -> per mille
					scores[ScoreLabelTypes.LOST_PACKAGES_AVG_MS.ordinal()] = packagesLost[4] != null ? (int) (packagesLost[4] * 1000000.) : 0; // sec -> ms
					scores[ScoreLabelTypes.LOST_PACKAGES_MAX_MS.ordinal()] = packagesLost[3] != null ? (int) (packagesLost[3] * 1000000.) : 0; // sec -> ms
					scores[ScoreLabelTypes.LOST_PACKAGES_MIN_MS.ordinal()] = packagesLost[2] != null ? (int) (packagesLost[2] * 1000000.) : 0; // sec -> ms
					scores[ScoreLabelTypes.LOST_PACKAGES_SIGMA_MS.ordinal()] = packagesLost[5] != null ? (int) (packagesLost[5] * 1000000.) : 0; // sec -> ms
					scores[ScoreLabelTypes.SENSORS.ordinal()] = (sensors.length - 1) > 0 ? (sensors.length - 1) * 1000 : 0; // subtract Receiver
					scores[ScoreLabelTypes.SENSOR_VARIO.ordinal()] = Arrays.asList(sensors).contains("VARIO") ? 1000 : 0; //$NON-NLS-1$
					scores[ScoreLabelTypes.SENSOR_GPS.ordinal()] = Arrays.asList(sensors).contains("GPS") ? 1000 : 0; //$NON-NLS-1$
					scores[ScoreLabelTypes.SENSOR_GAM.ordinal()] = Arrays.asList(sensors).contains("GAM") ? 1000 : 0; //$NON-NLS-1$
					scores[ScoreLabelTypes.SENSOR_EAM.ordinal()] = Arrays.asList(sensors).contains("EAM") ? 1000 : 0; //$NON-NLS-1$
					scores[ScoreLabelTypes.SENSOR_ESC.ordinal()] = Arrays.asList(sensors).contains("ESC") ? 1000 : 0; //$NON-NLS-1$
					scores[ScoreLabelTypes.LOG_DATA_VERSION.ordinal()] = logDataVersion != null ? (int) (logDataVersion * 1000.) : 0; // Firmware
					scores[ScoreLabelTypes.LOG_DATA_EXPLORER_VERSION.ordinal()] = (int) (logDataExplorerVersion * 1000.);
					scores[ScoreLabelTypes.LOG_FILE_VERSION.ordinal()] = Integer.valueOf(header.get(GDE.DATA_EXPLORER_FILE_VERSION).trim()).intValue() * 1000;
					scores[ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal()] = GDE.SIZE_BYTES_INTEGER * numberRecordAndTimeStamp * recordDataSize;
					scores[ScoreLabelTypes.LOG_FILE_BYTES.ordinal()] = (int) file.length();
					scores[ScoreLabelTypes.LOG_FILE_RECORD_SETS.ordinal()] = numberRecordSets * 1000;
					scores[ScoreLabelTypes.ELAPSED_HISTO_RECORD_SET_MS.ordinal()] = (int) TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - nanoTime); // do not multiply by 1000 as usual, this is the conversion from microseconds to ms

					recordSetTrusses.get(i).promoteTruss(histoRecordSet, scores);
					histoVaults.add(recordSetTrusses.get(i).getVault());

					// reduce memory consumption in advance to the garbage collection
					histoRecordSet.cleanup();

					if (log.isLoggable(Level.FINE) ) log.log(Level.FINE, String.format("|%s|  startTimeStamp=%s    recordDataSize=%,d  recordSetDataPointer=%,d  numberRecordAndTimeStamp=%,d", recordSetInfoChannel.getName(), //$NON-NLS-1$
							recordSetTrusses.get(i).getVault().getStartTimeStampFormatted(), recordDataSize, recordSetDataPointer, numberRecordAndTimeStamp));
				}
			}
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("%d of%3d recordsets in%,7d ms  recordSetOrdinals=%s from %s", histoVaults.size(), recordSetsInfo.size(), //$NON-NLS-1$
					TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNs), recordSetTrusses.keySet().toString(), filePath));
		}
		return histoVaults;
	}

	/**
	 * @param fileRecordsProperties
	 * @return the record names (measurement names at the time of osd creation)
	 */
	private static String[] getRecordKeys(String[] fileRecordsProperties) {
		String[] recordKeys = new String[fileRecordsProperties.length];
		for (int i = 0; i < fileRecordsProperties.length; i++) {
			HashMap<String, String> recordProperties = StringHelper.splitString(fileRecordsProperties[i], Record.DELIMITER, Record.propertyKeys);
			recordKeys[i] = recordProperties.get(Record.NAME);
		}
		return recordKeys;
	}

	/**
	 * @param recordSetComment
	 * @return names of the sensors; RECEIVER if parsing was not successful.
	 */
	private static String[] parseSensors(String recordSetComment) {
		int idx1 = recordSetComment.indexOf(GDE.STRING_LEFT_BRACKET);
		int idx2 = recordSetComment.indexOf(GDE.STRING_RIGHT_BRACKET);
		if (idx1 > 0 && idx2 > idx1) {
			return recordSetComment.substring(idx1 + 1, idx2).split(","); //$NON-NLS-1$
		}
		else {
			return new String[] { "RECEIVER" };
		}
	}

	/**
	 * @param recordSetComment
	 * @return null if parsing was not successful.
	 */
	private static Double parseFirmware(String recordSetComment) {
		final String label = "Firmware"; //$NON-NLS-1$
		int idx1 = recordSetComment.indexOf(label);
		// int idx2 = recordSetComment.indexOf(GDE.STRING_RIGHT_BRACKET);
		if (idx1 > 0) {
			try {
				return Double.parseDouble(recordSetComment.substring(idx1 + label.length(), recordSetComment.length()).replace(',', '.').replaceAll("[^0-9.]", "")); // change comma to dot and take digits, dots and signs only //$NON-NLS-1$ //$NON-NLS-2$
			}
			catch (Exception e) {
				return null;
			}
		}
		else {
			return null;
		}
	}

	/**
	 * @param recordSetComment
	 * @return packagesLostCounter, packagesLostRatio, minPackagesLost_sec, maxPackagesLost_sec, avgPackagesLost_sec, sigmaPackagesLost_sec. null if parsing was not successful.
	 */
	private static Double[] parsePackageLoss(String recordSetComment) {
		NumberFormat doubleFormat = NumberFormat.getInstance(Locale.getDefault());
		Double[] values = new Double[6];
		// GDE_MSGI2404=\nVerlorene Rückkanalpakete = {0} ~ {1} % ({2})
		// "min=%.2f sec; max=%.2f sec; avg=%.2f sec; sigma=%.2f sec"
		int idx0 = recordSetComment.indexOf(GDE.STRING_NEW_LINE);
		int idx1 = recordSetComment.indexOf(GDE.STRING_EQUAL);
		int idx2 = recordSetComment.indexOf("~", idx1); //$NON-NLS-1$
		int idx3 = recordSetComment.indexOf("%", idx2); //$NON-NLS-1$
		if (idx0 > 0 && idx1 > idx0 && idx2 > idx1 && idx3 > idx2) {
			try {
				values[0] = Double.parseDouble(recordSetComment.substring(idx1 + 2, idx2 - 1)); // integer
				values[1] = doubleFormat.parse(recordSetComment.substring(idx2 + 2, idx3 - 1)).doubleValue();
				String[] times = (recordSetComment.substring(idx3 + 1)).split("="); //$NON-NLS-1$
				if (times.length > 1) { // time statistics is available
					values[2] = doubleFormat.parse(times[1].substring(0, times[1].indexOf(" "))).doubleValue(); //$NON-NLS-1$
					values[3] = doubleFormat.parse(times[2].substring(0, times[2].indexOf(" "))).doubleValue(); //$NON-NLS-1$
					values[4] = doubleFormat.parse(times[3].substring(0, times[3].indexOf(" "))).doubleValue(); //$NON-NLS-1$
					values[5] = doubleFormat.parse(times[4].substring(0, times[4].indexOf(" "))).doubleValue(); //$NON-NLS-1$
				}
			}
			catch (ParseException e) {
				// ignore and keep packageLoss as null
			}
		}
		return values;
	}

	/**
	 * @param recordSetInfo
	 * @return start timestamp from recordset properties, alternatively take RecordedTS from RecordSetComment, alternatively FileCreatedTS
	 */
	private static long getStartTimestamp_ms(HashMap<String, String> recordSetInfo) {
		long startTimestamp_ms = 0;

		HashMap<String, String> recordSetProps = StringHelper.splitString(recordSetInfo.get(GDE.RECORD_SET_PROPERTIES), Record.DELIMITER, RecordSet.propertyKeys);
		// try 1: osd version >= 3 only --- V 1 and 2 do not carry a recordset start timestamp
		if (recordSetProps.containsKey(RecordSet.START_TIME_STAMP) && recordSetProps.get(RecordSet.START_TIME_STAMP).length() > 0) {
			startTimestamp_ms = Long.parseLong(recordSetProps.get(RecordSet.START_TIME_STAMP).trim());
		}
		else { // copied from gde.data.RecordSet.setDeserializedProperties(String)
			try {
				// try 2: check if the original time stamp in the recordset comment is available
				String recordSetDescription = recordSetInfo.get(GDE.RECORD_SET_COMMENT);
				Pattern datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}"); //$NON-NLS-1$
				Matcher dateMatcher = datePattern.matcher(recordSetDescription);
				Pattern timePattern = Pattern.compile("\\d{2}:\\d{2}:\\d{2}"); //$NON-NLS-1$
				Matcher timeMatcher = timePattern.matcher(recordSetDescription);
				if (dateMatcher.find() && timeMatcher.find()) {
					String date = dateMatcher.group();
					String time = timeMatcher.group();

					String[] strValueDate = date.split(GDE.STRING_DASH);
					int year = Integer.parseInt(strValueDate[0]);
					int month = Integer.parseInt(strValueDate[1]);
					int day = Integer.parseInt(strValueDate[2]);

					String[] strValueTime = time.split(GDE.STRING_COLON);
					int hour = Integer.parseInt(strValueTime[0]);
					int minute = Integer.parseInt(strValueTime[1]);
					int second = Integer.parseInt(strValueTime[2]);

					GregorianCalendar calendar = new GregorianCalendar(year, month - 1, day, hour, minute, second);
					startTimestamp_ms = calendar.getTimeInMillis();
				}
			}
			catch (Exception e) {
				// ignore and keep startTimestamp_ms = 0
			}
		}
		// try 3: take the osd file creation timestamp
		if (startTimestamp_ms == 0 && recordSetInfo.containsKey(GDE.CREATION_TIME_STAMP)) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd" + ' ' + " HH:mm:ss"); //$NON-NLS-1$ //$NON-NLS-2$
			try {
				startTimestamp_ms = simpleDateFormat.parse(recordSetInfo.get(GDE.CREATION_TIME_STAMP)).getTime();
			}
			catch (Exception ex) { // ParseException ex) {
				throw new RuntimeException(ex);
			}
		}
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "startTimestamp_ms=" + startTimestamp_ms); //$NON-NLS-1$
		return startTimestamp_ms;
	}

}
