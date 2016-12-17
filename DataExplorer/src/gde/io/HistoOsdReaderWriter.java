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
    
    Copyright (c) 2016 Thomas Eickert
****************************************************************************************/
package gde.io;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import javax.naming.OperationNotSupportedException;

import gde.GDE;
import gde.data.Channel;
import gde.data.HistoRecordSet;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IHistoDevice;
import gde.device.ScoreLabelTypes;
import gde.exception.DataInconsitsentException;
import gde.exception.NotSupportedFileFormatException;
import gde.histocache.HistoVault;
import gde.log.Level;
import gde.utils.StringHelper;

/**
 * reads the DataExplorer file format into histo recordsets.
 * @author Thomas Eickert 
 */
public class HistoOsdReaderWriter extends OsdReaderWriter { // todo merging this class with the base class reduced the number of classes
	final private static String	$CLASS_NAME	= HistoOsdReaderWriter.class.getName();
	final private static Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
		 * the vault truss holds the key values only.
		 * enhanced value for the start timestamp.
		 * @param filePath
		 * @param objectDirectory holds the validated object key from the parent directory or is empty 
		 * @return the vaults skeletons created from the recordsets
		 * @throws NotSupportedFileFormatException 
		 * @throws IOException 
		 */
	public static List<HistoVault> getTrusses(File file, String objectDirectory) throws IOException, NotSupportedFileFormatException {
		List<HistoVault> trusses = new ArrayList<HistoVault>();
		final HashMap<String, String> header = HistoOsdReaderWriter.getHeader(file.toString());
		final String logObjectKey = header.containsKey(GDE.OBJECT_KEY) ? header.get(GDE.OBJECT_KEY).intern() : GDE.STRING_EMPTY;

		final List<HashMap<String, String>> recordSetsProps = HistoOsdReaderWriter.getRecordSetsInfo(header);
		for (int i = 0; i < recordSetsProps.size(); i++) {
			final int fileVersion = Integer.valueOf(header.get(GDE.DATA_EXPLORER_FILE_VERSION).trim()).intValue();
			final int recordSetSize = Integer.valueOf(header.get(GDE.RECORD_SET_SIZE).trim()).intValue();
			HashMap<String, String> recordSetInfo = recordSetsProps.get(i);
			final String logRecordSetBaseName = String.format("%s | %s", recordSetInfo.get(GDE.RECORD_SET_NAME), recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME)).intern();
			final long enhancedStartTimeStamp_ms = HistoOsdReaderWriter.getStartTimestamp_ms(recordSetInfo);
			final Channel recordSetInfoChannel = OsdReaderWriter.getChannel(recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME));
			trusses.add(HistoVault.createTruss(objectDirectory, file, fileVersion, recordSetSize, i, logRecordSetBaseName, header.get(GDE.DEVICE_NAME), enhancedStartTimeStamp_ms,
					recordSetInfoChannel.getNumber(), logObjectKey));
		}
		return trusses;
	}

	/**
	 * @param header
	 * @return the full osd recordset information for all recordsets
	 */
	private static List<HashMap<String, String>> getRecordSetsInfo(HashMap<String, String> header) {
		String line;
		// record sets with it properties and records
		List<HashMap<String, String>> recordSetsInfo = new ArrayList<HashMap<String, String>>();
		for (int i = 1; i < Integer.valueOf(header.get(GDE.RECORD_SET_SIZE).trim()).intValue() + 1; ++i) {
			StringBuilder sb = new StringBuilder();
			line = GDE.RECORD_SET_NAME + GDE.STRING_EMPTY + header.get(sb.append(i).append(GDE.STRING_BLANK).append(GDE.RECORD_SET_NAME).toString());
			recordSetsInfo.add(getRecordSetProperties(line));
		}
		return recordSetsInfo;
	}

	/**
	 * read histo record sets from one single file. 
	 * @param filePath 
	 * @param trusses referencing a subset of the recordsets in the file
	 * @return histo vault list (may contain vaults without measurements, settlements and scores)
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 * @throws DataInconsitsentException
	 */
	public static List<HistoVault> readHisto(Path filePath, Collection<HistoVault> trusses) throws IOException, NotSupportedFileFormatException, DataInconsitsentException {
		List<HistoVault> histoVaults = new ArrayList<HistoVault>();
		log.log(Level.FINE, "start " + filePath); //$NON-NLS-1$
		// build job list consisting of recordset ordinal and the corresponding truss
		Map<Integer, HistoVault> recordSetTrusses = new TreeMap<Integer, HistoVault>();
		for (HistoVault truss : trusses) {
			if (truss.getLogFilePath().equals(filePath.toString()))
				recordSetTrusses.put(truss.getLogRecordSetOrdinal(), truss);
			else
				throw new UnsupportedOperationException("all trusses must carry the same logFilePath");
		}

		File file = filePath.toFile();
		ZipInputStream zip_input = new ZipInputStream(new FileInputStream(file));
		ZipEntry zip_entry = zip_input.getNextEntry();
		InputStream inputStream;
		if (zip_entry != null) {
			inputStream = (InputStream) zip_input;
		}
		else {
			zip_input.close();
			zip_input = null;
			inputStream = (InputStream) new FileInputStream(file);
		}

		try (DataInputStream data_in = new DataInputStream((InputStream) inputStream)) {
			final HashMap<String, String> header = HistoOsdReaderWriter.getHeader(filePath.toString());
			final double logDataExplorerVersion = header.containsKey(GDE.DATA_EXPLORER_FILE_VERSION) ? Double.parseDouble(header.get(GDE.DATA_EXPLORER_FILE_VERSION)) : 1.; // OpenSerialData version : 1
			final int numberRecordSets = Integer.parseInt(header.get(GDE.RECORD_SET_SIZE));

			while (!data_in.readUTF().startsWith(GDE.RECORD_SET_SIZE))
				log.log(Level.FINEST, "skip"); //$NON-NLS-1$
			final List<HashMap<String, String>> recordSetsInfo = OsdReaderWriter.readRecordSetsInfo4AllVersions(data_in, header);

			long startTimeNs = System.nanoTime();
			for (int i = 0; i < recordSetsInfo.size(); i++) {
				HashMap<String, String> recordSetInfo = recordSetsInfo.get(i);
				final Channel recordSetInfoChannel = OsdReaderWriter.getChannel(recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME));
				String recordSetProperties = recordSetInfo.get(GDE.RECORD_SET_PROPERTIES);
				String[] recordsProperties = StringHelper.splitString(recordSetInfo.get(GDE.RECORDS_PROPERTIES), Record.END_MARKER, GDE.RECORDS_PROPERTIES);
				int recordDataSize = Long.valueOf(recordSetInfo.get(GDE.RECORD_DATA_SIZE)).intValue();
				long recordSetDataPointer = Long.parseLong(recordSetInfo.get(GDE.RECORD_SET_DATA_POINTER));
				HashMap<String, String> recordSetProps = StringHelper.splitString(recordSetProperties, Record.DELIMITER, RecordSet.propertyKeys);
				String[] cleanedMeasurementNames = getMeasurementNames(recordsProperties, recordSetInfoChannel.getNumber());
				String[] noneCalculationMeasurementNames = HistoOsdReaderWriter.application.getActiveDevice().getNoneCalculationMeasurementNames(recordSetInfoChannel.getNumber(), cleanedMeasurementNames);
				int numberRecordAndTimeStamp = recordSetProps.containsKey(RecordSet.TIME_STEP_MS) && recordSetProps.get(RecordSet.TIME_STEP_MS).length() > 0
						&& Double.parseDouble(recordSetProps.get(RecordSet.TIME_STEP_MS).trim()) < 0 ? noneCalculationMeasurementNames.length + 1 : noneCalculationMeasurementNames.length;

				if (!recordSetTrusses.containsKey(i)) {
					int toSkip = GDE.SIZE_BYTES_INTEGER * numberRecordAndTimeStamp * recordDataSize;
					while (toSkip > 0) { // The skip method may, for a variety of reasons, end up skipping over some smaller number of bytes, possibly 0. The actual number of bytes skipped is returned.
						toSkip -= data_in.skip(toSkip);
						if (toSkip > 0) {
							if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "toSkip=" + toSkip); //$NON-NLS-1$
							if (data_in.available() == 0) throw new EOFException("recordDataSize / recordSetDataPointer do not match the actual file size"); //$NON-NLS-1$
						}
					}
				}
				else {
					long nanoTime = System.nanoTime();
					byte[] buffer = new byte[GDE.SIZE_BYTES_INTEGER * numberRecordAndTimeStamp * recordDataSize];
					data_in.readFully(buffer);
					HistoRecordSet histoRecordSet = HistoOsdReaderWriter.buildRecordSet(recordSetInfo, recordSetTrusses.get(i));

					histoRecordSet.setFileDataPointerAndSize(recordSetDataPointer, recordDataSize, GDE.SIZE_BYTES_INTEGER * numberRecordAndTimeStamp * recordDataSize);
					log.log(Level.FINE, recordSetInfoChannel.getName() + " recordDataSize=" + recordDataSize + "  recordSetDataPointer=" + recordSetDataPointer //$NON-NLS-1$ //$NON-NLS-2$
							+ "  numberRecordAndTimeStamp=" + numberRecordAndTimeStamp); //$NON-NLS-1$
					if (histoRecordSet.getDevice() instanceof IHistoDevice) {
						int[] maxPoints = new int[noneCalculationMeasurementNames.length];
						int[] minPoints = new int[noneCalculationMeasurementNames.length];
						List<String> noneCalculationMeasurementNameList = Arrays.asList(noneCalculationMeasurementNames);
						for (int j = 0; j < recordsProperties.length; j++) {
							int index = noneCalculationMeasurementNameList.indexOf(cleanedMeasurementNames[j]);
							if (index > -1) {
								HashMap<String, String> recordProperties = StringHelper.splitString(recordsProperties[j], Record.DELIMITER, Record.propertyKeys);
								maxPoints[index] = Integer.parseInt(recordProperties.get(Record.MAX_VALUE).trim());
								minPoints[index] = Integer.parseInt(recordProperties.get(Record.MIN_VALUE).trim());
							}
						}
						((IHistoDevice) histoRecordSet.getDevice()).setSampling(maxPoints, minPoints);
					}
					histoRecordSet.getDevice().addDataBufferAsRawDataPoints(histoRecordSet, buffer, recordDataSize, false);

					// todo not sure if necessary OsdReaderWriter.application.getActiveDevice().makeInActiveDisplayable(histoRecordSet);

					// extract additional data
					final String recordSetComment = recordSetInfo.get(GDE.RECORD_SET_COMMENT);
					final Double[] packagesLost = parsePackageLoss(recordSetComment);
					final String[] sensors = parseSensors(recordSetComment);
					final Double logDataVersion = parseFirmware(recordSetComment);
					final Integer[] scores = new Integer[ScoreLabelTypes.values.length];
					// values are multiplied by 1000 as this is the convention for internal values in order to avoid rounding errors for values below 1.0 (0.5 -> 0)
					// scores for duration and timestep values are filled in by the HistoRecordSet
					scores[ScoreLabelTypes.TOTAL_READINGS.ordinal()] = recordDataSize;
					scores[ScoreLabelTypes.TOTAL_PACKAGES.ordinal()] = packagesLost[0] != null && packagesLost[1] != null && packagesLost[1] != 0 ? (int) (packagesLost[0] * 100. / packagesLost[1]) : 0;
					//todo recalculating the following scores from raw data would be feasible
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
					scores[ScoreLabelTypes.LOG_DATA_VERSION.ordinal()] = logDataVersion != null ? (int) (logDataVersion * 1000.) : 0;
					scores[ScoreLabelTypes.LOG_DATA_EXPLORER_VERSION.ordinal()] = (int) (logDataExplorerVersion * 1000.);
					scores[ScoreLabelTypes.LOG_FILE_VERSION.ordinal()] = Integer.valueOf(header.get(GDE.DATA_EXPLORER_FILE_VERSION).trim()).intValue() * 1000;
					scores[ScoreLabelTypes.LOG_RECORD_SET_BYTES.ordinal()] = GDE.SIZE_BYTES_INTEGER * numberRecordAndTimeStamp * recordDataSize;
					scores[ScoreLabelTypes.LOG_FILE_BYTES.ordinal()] = (int) file.length();
					scores[ScoreLabelTypes.LOG_FILE_RECORD_SETS.ordinal()] = numberRecordSets * 1000;
					// scores for elapsed times are filled in by the HistoRecordSet
					histoRecordSet.setScorePoints(scores);
					histoRecordSet.setElapsedHistoRecordSet_ns(System.nanoTime() - nanoTime);
					if (histoRecordSet.getRecordDataSize(true) > 0) {
						histoRecordSet.addSettlements();
						// put all aggregated data and scores into the history vault
						HistoVault histoVault = histoRecordSet.getHistoVault();
						histoVaults.add(histoVault);
					}
					else {
						histoVaults.add(recordSetTrusses.get(i));
					}
					// reduce memory consumption in advance to the garbage collection
					histoRecordSet.cleanup();

					log.log(Level.FINE, String.format("|%s|  startTimeStamp=%s    recordDataSize=%,d  recordSetDataPointer=%,d  numberRecordAndTimeStamp=%,d", recordSetInfoChannel.getName(), //$NON-NLS-1$
							StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", histoRecordSet.getStartTimeStamp()), recordDataSize, recordSetDataPointer, numberRecordAndTimeStamp)); //$NON-NLS-1$
				}
			}
			log.log(Level.TIME, String.format("%d of%3d recordsets in%,7d ms  recordSetOrdinals=%s from %s", histoVaults.size(), recordSetsInfo.size(), //$NON-NLS-1$
					TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNs), recordSetTrusses.keySet().toString(), filePath));
		}
		return histoVaults;
	}

	/**
	 * @param fileRecordsProperties
	 * @param channelConfigNumber
	 * @return current measurement names for exactly the number of records in the recordset
	 */
	private static String[] getMeasurementNames(String[] fileRecordsProperties, int channelConfigNumber) {
		final String[] measurementNames = HistoOsdReaderWriter.application.getActiveDevice().getMeasurementNames(channelConfigNumber);
		List<String> cleanedRecordNames = new ArrayList<String>();
		for (int i = 0; i < fileRecordsProperties.length; ++i) {
			cleanedRecordNames.add(measurementNames[i]);
		}
		return cleanedRecordNames.toArray(new String[cleanedRecordNames.size()]);
	}

	/**
	 * parse recordSetInfo from osd file. channel is determined without user interaction.
	 * @param path 
	 * @return null if this recordSet is not valid for histo (channel not allowed for histo).
	 */
	private static HistoRecordSet buildRecordSet(HashMap<String, String> recordSetInfo, HistoVault truss) {
		final String recordSetProperties = recordSetInfo.get(GDE.RECORD_SET_PROPERTIES);
		final String[] recordsProperties = StringHelper.splitString(recordSetInfo.get(GDE.RECORDS_PROPERTIES), Record.END_MARKER, GDE.RECORDS_PROPERTIES);
		// recordSetDataPointer = Long.valueOf(recordSetInfo.get(RECORD_SET_DATA_POINTER)).longValue();
		if (log.isLoggable(Level.FINER)) {
			StringBuilder sb = new StringBuilder().append(truss.getLogRecordsetBaseName()).append(GDE.STRING_MESSAGE_CONCAT).append(truss.getLogChannelNumber()).append(GDE.STRING_MESSAGE_CONCAT);
			for (String recordProps : recordsProperties) {
				sb.append(StringHelper.splitString(recordProps, Record.DELIMITER, Record.propertyKeys).get(Record.propertyKeys[0])).append(GDE.STRING_COMMA);
			}
			log.log(Level.FINER, sb.toString());
		}
		HistoRecordSet recordSet;
		if (HistoOsdReaderWriter.application.getActiveDevice().isVariableMeasurementSize()) {
			// build up the record set with the same number of records which may not fit to the current device XML
			final String[] recordNames = new String[recordsProperties.length];
			final String[] recordSymbols = new String[recordsProperties.length];
			final String[] recordUnits = new String[recordsProperties.length];
			for (int i = 0; i < recordsProperties.length; i++) {
				HashMap<String, String> recordProperties = StringHelper.splitString(recordsProperties[i], Record.DELIMITER, Record.propertyKeys);
				recordNames[i] = recordProperties.get(Record.NAME);
				recordSymbols[i] = recordProperties.get(Record.SYMBOL) == null ? GDE.STRING_EMPTY : recordProperties.get(Record.SYMBOL);
				recordUnits[i] = recordProperties.get(Record.UNIT) == null ? GDE.STRING_EMPTY : recordProperties.get(Record.UNIT);
			}
			recordSet = HistoRecordSet.createRecordSet(truss, recordNames, recordSymbols, recordUnits);
		}
		else {
			recordSet = HistoRecordSet.createRecordSet(truss);
		}

		String[] recordKeys = application.getActiveDevice().crossCheckMeasurements(recordsProperties, recordSet);
		// check if the file content fits measurements from device properties XML which was used to create the record set
		for (int i = 0; i < recordKeys.length; ++i) {
			Record record = recordSet.get(recordKeys[i]);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, record.getName() + " - setSerializedProperties " + recordKeys[i]); //$NON-NLS-1$
			record.setSerializedProperties(recordsProperties[i]);
			record.setSerializedDeviceSpecificProperties(recordsProperties[i]);
		}
		recordSet.setDeserializedProperties(recordSetProperties);
		// setDeserializedProperties does not take all possibilities to set the timestamp
		if (log.isLoggable(Level.INFO) && recordSet.getStartTimeStamp() != HistoOsdReaderWriter.getStartTimestamp_ms(recordSetInfo))
			log.log(Level.INFO, "startTimeStamp rectified " + HistoOsdReaderWriter.getStartTimestamp_ms(recordSetInfo) + "  " + recordSet.getStartTimeStamp()); //$NON-NLS-1$
		recordSet.setStartTimeStamp(HistoOsdReaderWriter.getStartTimestamp_ms(recordSetInfo));
		recordSet.setSaved(true);
		return recordSet;
	}

	/**
	 * @param recordSetComment
	 * @return names of the sensors; RECEIVER if parsing was not successful.
	 */
	private static String[] parseSensors(String recordSetComment) {
		// todo calculate Sensors from raw data
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
	 * @param header
	 * @return null if parsing was not successful.
	 */
	private static Double parseDataExplorerVersion(HashMap<String, String> header) {
		Double retVal;
		if (header.containsKey(GDE.DATA_EXPLORER_FILE_VERSION)) {
			try {
				retVal = Double.parseDouble(header.get(GDE.DATA_EXPLORER_FILE_VERSION));
			}
			catch (Exception e) {
				retVal = null;
			}
		}
		else {
			retVal = 1.; // OpenSerialData version : 1
		}
		return retVal;
	}

	/**
	 * @param recordSetComment
	 * @return packagesLostCounter, packagesLostRatio, minPackagesLost_sec, maxPackagesLost_sec, avgPackagesLost_sec, sigmaPackagesLost_sec. null if parsing was not successful.
	 */
	private static Double[] parsePackageLoss(String recordSetComment) {
		NumberFormat doubleFormat = NumberFormat.getInstance(Locale.getDefault());
		Double[] values = new Double[6];
		// todo calculate Verlorene Rückkanalpakete from raw data
		// GDE_MSGI2404=\nVerlorene Rückkanalpakete = {0} ~ {1} % ({2})
		// "min=%.2f sec; max=%.2f sec; avg=%.2f sec; sigma=%.2f sec"
		int idx0 = recordSetComment.indexOf("\n"); //$NON-NLS-1$
		int idx1 = recordSetComment.indexOf("="); //$NON-NLS-1$
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
