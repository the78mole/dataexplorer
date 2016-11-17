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

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.HistoRecordSet;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.DeviceTypes;
import gde.device.IHistoDevice;
import gde.device.ScoreLabelTypes;
import gde.device.ScoreType;
import gde.device.ScoregroupType;
import gde.exception.DataInconsitsentException;
import gde.exception.NotSupportedFileFormatException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.StringHelper;

import static java.util.logging.Level.INFO;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.InflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * reads the DataExplorer file format into histo recordsets.
 * @author Thomas Eickert 
 */
public class HistoOsdReaderWriter extends OsdReaderWriter { // todo merging this class with the base class
	final private static String		$CLASS_NAME	= HistoOsdReaderWriter.class.getName();
	final private static Logger		log					= Logger.getLogger($CLASS_NAME);

	private final static Settings	settings		= Settings.getInstance();

	/**
	 * read complete file data and fill histo record sets. 
	 * takes only those recordsets which conform to the user's channel setting.
	 * the file's device name and object key must match according to the user settings. 
	 * @param recordSets destination for appending file data.
	 * @param path
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 * @throws DataInconsitsentException
	 */
	public static void readHisto(List<HistoRecordSet> recordSets, Path path) throws FileNotFoundException, IOException, NotSupportedFileFormatException, DataInconsitsentException {
		log.log(Level.INFO, "start " + path.toString()); //$NON-NLS-1$
		ZipInputStream zip_input = new ZipInputStream(new FileInputStream(path.toFile()));
		ZipEntry zip_entry = zip_input.getNextEntry();
		InputStream inputStream;
		if (zip_entry != null) {
			inputStream = (InputStream) zip_input;
		}
		else {
			zip_input.close();
			zip_input = null;
			inputStream = (InputStream) new FileInputStream(path.toFile());
		}

		try (DataInputStream data_in = new DataInputStream((InputStream) inputStream)) {
			// build the data structure for the subset which holds channels relevant for histo
			final HashMap<String, String> header = HistoOsdReaderWriter.getHeader(path.toString());
			final String objectKey = header.get(GDE.OBJECT_KEY) != null ? header.get(GDE.OBJECT_KEY) : GDE.STRING_EMPTY;
			if (!header.get(GDE.DEVICE_NAME).equalsIgnoreCase(HistoOsdReaderWriter.application.getActiveDevice().getName())) { // ignoreCase due to AV4ms_FV_762\2016-09-21_StaboSet.osd
				log.log(Level.INFO, "SKIP FILE : deviceName differs " + path.toString());
			}
			else if (application.getActiveObject() != null && objectKey.equalsIgnoreCase(GDE.STRING_EMPTY) && settings.skipFilesWithoutObject()) {
				log.log(Level.INFO, "SKIP FILE : objectKey empty    " + path.toString());
			}
			else if (application.getActiveObject() != null && objectKey.equalsIgnoreCase(application.getObjectKey()) && settings.skipFilesWithOtherObject()) {
				log.log(INFO, "SKIP FILE : objectKey differs  " + path.toString());
			}
			else {
				while (!data_in.readUTF().startsWith(GDE.RECORD_SET_SIZE))
					log.log(Level.FINEST, "skip"); //$NON-NLS-1$
				final List<HashMap<String, String>> recordSetsInfo = readRecordSetsInfo4AllVersions(data_in, header);

				long startTimeNs = System.nanoTime();
				for (int i = 0; i < recordSetsInfo.size(); i++) {
					HashMap<String, String> recordSetInfo = recordSetsInfo.get(i);
					String channelConfig = recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME);
					//			  String recordSetName = recordSetInfo.get(GDE.RECORD_SET_NAME);
					//				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
					//				String recordSetComment = recordSetInfo.get(GDE.RECORD_SET_COMMENT);
					String recordSetProperties = recordSetInfo.get(GDE.RECORD_SET_PROPERTIES);
					String[] recordsProperties = StringHelper.splitString(recordSetInfo.get(GDE.RECORDS_PROPERTIES), Record.END_MARKER, GDE.RECORDS_PROPERTIES);
					int recordDataSize = Long.valueOf(recordSetInfo.get(GDE.RECORD_DATA_SIZE)).intValue();
					long recordSetDataPointer = Long.parseLong(recordSetInfo.get(GDE.RECORD_SET_DATA_POINTER));
					int channelConfigNumber = channels.getChannelNumber(channelConfig);
					HashMap<String, String> recordSetProps = StringHelper.splitString(recordSetProperties, Record.DELIMITER, RecordSet.propertyKeys);
					String[] cleanedMeasurementNames = getMeasurementNames(recordsProperties, channelConfigNumber);
					String[] noneCalculationMeasurementNames = HistoOsdReaderWriter.application.getActiveDevice().getNoneCalculationMeasurementNames(channelConfigNumber, cleanedMeasurementNames);
					int numberRecordAndTimeStamp = recordSetProps.containsKey(RecordSet.TIME_STEP_MS) && recordSetProps.get(RecordSet.TIME_STEP_MS).length() > 0
							&& Double.parseDouble(recordSetProps.get(RecordSet.TIME_STEP_MS).trim()) < 0 ? noneCalculationMeasurementNames.length + 1 : noneCalculationMeasurementNames.length;

					if (!isChannel4Histo(recordSetInfo)) {
						int toSkip = GDE.SIZE_BYTES_INTEGER * numberRecordAndTimeStamp * recordDataSize;
						while (toSkip > 0) { // The skip method may, for a variety of reasons, end up skipping over some smaller number of bytes, possibly 0. The actual number of bytes skipped is returned.
							toSkip -= data_in.skip(toSkip);
							if (toSkip > 0) {
								if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "toSkip=" + toSkip);
								if (data_in.available() == 0) throw new EOFException("recordDataSize / recordSetDataPointer do not match the actual file size");
							}
						}
					}
					else {
						byte[] buffer = new byte[GDE.SIZE_BYTES_INTEGER * numberRecordAndTimeStamp * recordDataSize];
						data_in.readFully(buffer);
						HistoRecordSet recordSet = HistoOsdReaderWriter.buildRecordSet(recordSetInfo, path);
						recordSets.add(recordSet);
						recordSet.setFileDataPointerAndSize(recordSetDataPointer, recordDataSize, GDE.SIZE_BYTES_INTEGER * numberRecordAndTimeStamp * recordDataSize);
						recordSet.setRecordedKeys(channelConfigNumber, objectKey);

						log.log(Level.FINE, recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME) + " recordDataSize=" + recordDataSize + "  recordSetDataPointer=" + recordSetDataPointer //$NON-NLS-1$
								+ "  numberRecordAndTimeStamp=" + numberRecordAndTimeStamp);
						if (recordSet.getDevice() instanceof IHistoDevice) {
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
							((IHistoDevice) recordSet.getDevice()).setSampling(maxPoints, minPoints);
						}
						recordSet.getDevice().addDataBufferAsRawDataPoints(recordSet, buffer, recordDataSize, false);
						// extract additional data
						final String recordSetComment = recordSetInfo.get(GDE.RECORD_SET_COMMENT);
						final Double[] packagesLost = parsePackageLoss(recordSetComment);
						final String[] sensors = parseSensors(recordSetComment);
						final Double logDataVersion = parseFirmware(recordSetComment);
						final Double logDataExplorerVersion = parseDataExplorerVersion(header);
						final Integer[] scores = new Integer[ScoreLabelTypes.values.length];
						// values are multiplied by 1000 as this is the convention for internal values in order to avoid rounding errors for values below 1.0 (0.5 -> 0)
						scores[ScoreLabelTypes.TOTAL_READINGS.ordinal()] = recordDataSize;
						scores[ScoreLabelTypes.TOTAL_PACKAGES.ordinal()] = packagesLost[0] != null && packagesLost[1] != null && packagesLost[1] != 0 ? (int) (packagesLost[0] * 100. / packagesLost[1]) : 0;
						scores[ScoreLabelTypes.LOG_DATA_VERSION.ordinal()] = logDataVersion != null ? (int) (logDataVersion * 1000.) : null;
						scores[ScoreLabelTypes.LOG_DATA_EXPLORER_VERSION.ordinal()] = logDataExplorerVersion != null ? (int) (logDataExplorerVersion * 1000.) : null;
						scores[ScoreLabelTypes.LOG_DATA_BYTES.ordinal()] = GDE.SIZE_BYTES_INTEGER * numberRecordAndTimeStamp * recordDataSize;
						scores[ScoreLabelTypes.LOG_FILE_BYTES.ordinal()] = (int) path.toFile().length();
						scores[ScoreLabelTypes.LOG_FILE_RECORD_SETS.ordinal()] = (int) (Double.parseDouble(header.get(GDE.RECORD_SET_SIZE)) * 1000.);
						//todo recalculating the following scores from raw data would be feasible
						scores[ScoreLabelTypes.LOST_PACKAGES.ordinal()] = packagesLost[0] != null ? (int) (packagesLost[0].intValue()) : null;
						scores[ScoreLabelTypes.LOST_PACKAGES_PER_MILLE.ordinal()] = packagesLost[1] != null ? (int) (packagesLost[1] * 10. * 1000.) : null; // percent -> per mille
						scores[ScoreLabelTypes.LOST_PACKAGES_AVG_MS.ordinal()] = packagesLost[4] != null ? (int) (packagesLost[4] * 1000000.) : null; // sec -> ms
						scores[ScoreLabelTypes.LOST_PACKAGES_MAX_MS.ordinal()] = packagesLost[3] != null ? (int) (packagesLost[3] * 1000000.) : null; // sec -> ms
						scores[ScoreLabelTypes.LOST_PACKAGES_MIN_MS.ordinal()] = packagesLost[2] != null ? (int) (packagesLost[2] * 1000000.) : null; // sec -> ms
						scores[ScoreLabelTypes.LOST_PACKAGES_SIGMA_MS.ordinal()] = packagesLost[5] != null ? (int) (packagesLost[5] * 1000000.) : null; // sec -> ms
						scores[ScoreLabelTypes.SENSORS.ordinal()] = (sensors.length - 1) > 0 ? (sensors.length - 1) * 1000 : null; // subtract Receiver
						scores[ScoreLabelTypes.SENSOR_VARIO.ordinal()] = Arrays.asList(sensors).contains("VARIO") ? 1000 : null;
						scores[ScoreLabelTypes.SENSOR_GPS.ordinal()] = Arrays.asList(sensors).contains("GPS") ? 1000 : null;
						scores[ScoreLabelTypes.SENSOR_GAM.ordinal()] = Arrays.asList(sensors).contains("GAM") ? 1000 : null;
						scores[ScoreLabelTypes.SENSOR_EAM.ordinal()] = Arrays.asList(sensors).contains("EAM") ? 1000 : null;
						scores[ScoreLabelTypes.SENSOR_ESC.ordinal()] = Arrays.asList(sensors).contains("ESC") ? 1000 : null;
						recordSet.setScorePoints(scores);
						log.log(Level.FINE, String.format("|%s|  startTimeStamp=%s    recordDataSize=%,d  recordSetDataPointer=%,d  numberRecordAndTimeStamp=%,d", recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME), //$NON-NLS-1$
								StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", recordSet.getStartTimeStamp()), recordDataSize, recordSetDataPointer, numberRecordAndTimeStamp));
						// todo not sure if necessary OsdReaderWriter.application.getActiveDevice().makeInActiveDisplayable(recordSet);
					}
				}
				if (recordSets.size() > 0) log.log(Level.TIME,
						String.format("%3d of%3d recordsets in%,7d ms  from %s", recordSets.size(), recordSetsInfo.size(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNs), path.toString())); //$NON-NLS-1$
			}
		}
	}

	/**
	 * compares the channel defined in the recordset info data with the active channel which is used for histo selection
	 * @param recordSetInfo
	 * @return true if the channels are the same or if the two channels may be mixed
	 */
	private static boolean isChannel4Histo(HashMap<String, String> recordSetInfo) {
		final Channel recordSetInfoChannel = getChannel(recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME));
		boolean isChannel4Histo = false;
		if (recordSetInfoChannel != null) {
			if (recordSetInfoChannel.getNumber() == HistoOsdReaderWriter.channels.getActiveChannelNumber()) {
				isChannel4Histo = true;
			}
			else {
				isChannel4Histo = settings.isChannelMix() && HistoOsdReaderWriter.application.getActiveDevice().getDeviceGroup() == DeviceTypes.CHARGER
						&& ((DeviceConfiguration) HistoOsdReaderWriter.application.getActiveDevice()).isPairOfChannels(recordSetInfoChannel.getNumber(), HistoOsdReaderWriter.channels.getActiveChannelNumber());
			}
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "result=" + isChannel4Histo);
		return isChannel4Histo;
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
	private static HistoRecordSet buildRecordSet(HashMap<String, String> recordSetInfo, Path path) {
		final String channelConfig = recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME);
		String recordSetName = recordSetInfo.get(GDE.RECORD_SET_NAME);
		recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
		final String recordSetComment = recordSetInfo.get(GDE.RECORD_SET_COMMENT);
		final String recordSetProperties = recordSetInfo.get(GDE.RECORD_SET_PROPERTIES);
		final String[] recordsProperties = StringHelper.splitString(recordSetInfo.get(GDE.RECORDS_PROPERTIES), Record.END_MARKER, GDE.RECORDS_PROPERTIES);
		// recordSetDataPointer = Long.valueOf(recordSetInfo.get(RECORD_SET_DATA_POINTER)).longValue();
		if (log.isLoggable(Level.FINER)) {
			StringBuilder sb = new StringBuilder().append(recordSetInfo.get(GDE.RECORD_SET_NAME)).append(GDE.STRING_MESSAGE_CONCAT).append(channelConfig).append(GDE.STRING_MESSAGE_CONCAT);
			for (String recordProps : recordsProperties) {
				sb.append(StringHelper.splitString(recordProps, Record.DELIMITER, Record.propertyKeys).get(Record.propertyKeys[0])).append(GDE.STRING_COMMA);
			}
			log.log(Level.FINER, sb.toString());
		}

		// determine channel number in file and compare with activeChannel
		final Channel recordSetInfoChannel = getChannel(channelConfig);
		final String channelName = (recordSetInfoChannel == null ? "unknown" : recordSetInfoChannel.getName());
		recordSetInfo.put(GDE.CHANNEL_CONFIG_NAME, channelName); // todo GDE.CHANNEL_CONFIG_NUMBER ???
		if (log.isLoggable(Level.FINER)) {
			StringBuilder sb = new StringBuilder().append(GDE.CHANNEL_CONFIG_NAME).append(GDE.STRING_MESSAGE_CONCAT).append(channelName); //$NON-NLS-1$
			log.log(Level.FINER, sb.toString());
		}

		// build up the record set with the same number of records which may not fit to the current device XML
		final String[] recordNames = new String[recordsProperties.length];
		final String[] recordSymbols = new String[recordsProperties.length];
		final String[] recordUnits = new String[recordsProperties.length];
		for (int i = 0; i < recordsProperties.length; i++) {
			HashMap<String, String> recordProperties = StringHelper.splitString(recordsProperties[i], Record.DELIMITER, Record.propertyKeys);
			recordNames[i] = recordProperties.get(Record.NAME);
			recordSymbols[i] = recordProperties.get(Record.SYMBOL);
			recordUnits[i] = recordProperties.get(Record.UNIT);
		}

		final double tmpTimeStep_ms = application.getActiveDevice().getTimeStep_ms();
		HistoRecordSet recordSet = HistoRecordSet.createRecordSet(path, application.getActiveDevice(), recordSetInfoChannel.getNumber(), recordNames, recordSymbols, recordUnits, tmpTimeStep_ms, true,
				true);
		recordSet.setRecordSetDescription(recordSetComment);

		String[] recordKeys = application.getActiveDevice().crossCheckMeasurements(recordsProperties, recordSet);
		// check if the file content fits measurements form device properties XML which was used to create the record set
		for (int i = 0; i < recordKeys.length; ++i) {
			Record record = recordSet.get(recordKeys[i]);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, record.getName() + " - setSerializedProperties " + recordKeys[i]); //$NON-NLS-1$
			record.setSerializedProperties(recordsProperties[i]);
			record.setSerializedDeviceSpecificProperties(recordsProperties[i]);
		}
		recordSet.setDeserializedProperties(recordSetProperties);
		recordSet.setSaved(true);
		return recordSet;
	}

	/**
	 * @param recordSetComment
	 * @return empty string if parsing was not successful.
	 */
	private static String[] parseSensors(String recordSetComment) {
		int idx1 = recordSetComment.indexOf(GDE.STRING_LEFT_BRACKET);
		int idx2 = recordSetComment.indexOf(GDE.STRING_RIGHT_BRACKET);
		if (idx1 > 0 && idx2 > idx1) {
			return recordSetComment.substring(idx1 + 1, idx2).split(",");
		}
		else {
			return new String[0];
		}
	}

	/**
	 * @param recordSetComment
	 * @return null if parsing was not successful.
	 */
	private static Double parseFirmware(String recordSetComment) {
		final String label = "Firmware";
		int idx1 = recordSetComment.indexOf(label);
		// int idx2 = recordSetComment.indexOf(GDE.STRING_RIGHT_BRACKET);
		if (idx1 > 0) {
			try {
				return Double.parseDouble(recordSetComment.substring(idx1 + label.length(), recordSetComment.length()).replace(',', '.').replaceAll("[^0-9.]", "")); // change comma to dot and take digits, dots and signs only
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
		try {
			retVal = Double.parseDouble(header.get(GDE.DATA_EXPLORER_FILE_VERSION));
		}
		catch (Exception e) {
			retVal = null;
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
		int idx0 = recordSetComment.indexOf("\n");
		int idx1 = recordSetComment.indexOf("=");
		int idx2 = recordSetComment.indexOf("~", idx1);
		int idx3 = recordSetComment.indexOf("%", idx2);
		if (idx0 > 0 && idx1 > idx0 && idx2 > idx1 && idx3 > idx2) {
			try {
				values[0] = Double.parseDouble(recordSetComment.substring(idx1 + 2, idx2 - 1)); // integer
				values[1] = doubleFormat.parse(recordSetComment.substring(idx2 + 2, idx3 - 1)).doubleValue();
				String[] times = (recordSetComment.substring(idx3 + 1)).split("=");
				if (times.length > 1) { // time statistics is available
					values[2] = doubleFormat.parse(times[1].substring(0, times[1].indexOf(" "))).doubleValue();
					values[3] = doubleFormat.parse(times[2].substring(0, times[2].indexOf(" "))).doubleValue();
					values[4] = doubleFormat.parse(times[3].substring(0, times[3].indexOf(" "))).doubleValue();
					values[5] = doubleFormat.parse(times[4].substring(0, times[4].indexOf(" "))).doubleValue();
				}
			}
			catch (ParseException e) {
				// ignore and keep packageLoss as null
			}
		}
		return values;
	}

	/**
	 * take RecordedTS from RecordSetComment, FileCreatedTS if RecordedTS not available.
	 * @param recordSetComment
	 * @return null if parsing was not successful.
	 */
	@Deprecated
	private static Date getRecordSetTimestamp(HashMap<String, String> recordSetInfo) {
		Date recordSetTimestamp = null;
		String recordSetComment = recordSetInfo.get(GDE.RECORD_SET_COMMENT);
		String recordedTag = Messages.getString(MessageIds.GDE_MSGT0129);
		int idx = recordSetComment.indexOf(recordedTag);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss");
		if (idx > 0) {
			idx += recordedTag.length();
			try {
				recordSetTimestamp = simpleDateFormat.parse(recordSetComment.substring(idx, idx + simpleDateFormat.toPattern().length()));
			}
			catch (ParseException ex) {
				// ignore and keep recordSetTimestamp as null
			}
		}
		if (recordSetTimestamp == null) {
			simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd" + ' ' + " HH:mm:ss");
			try {
				recordSetTimestamp = simpleDateFormat.parse(recordSetInfo.get(GDE.CREATION_TIME_STAMP));
			}
			catch (ParseException ex) {
				// ignore and keep recordSetTimestamp as null
			}
			// recordSetTimestamp = recordSetInfo.get(GDE.CREATION_TIME_STAMP);
		}
		return recordSetTimestamp;
	}

}
