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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
    					2016 Thomas Eickert
****************************************************************************************/
package gde.io;

import gde.GDE;
import gde.data.Channel;
import gde.data.HistoRecordSet;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.exception.DataInconsitsentException;
import gde.exception.NotSupportedFileFormatException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.StringHelper;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.swt.SWT;

/**
 * reads the DataExplorer file format 
 * @author Thomas Eickert 
 */
public class HistoOsdReaderWriter extends OsdReaderWriter { // TODO merging this class in the base class
	final private static String			$CLASS_NAME				= HistoOsdReaderWriter.class.getName();
	final private static Logger			log						= Logger.getLogger($CLASS_NAME);

	final private static String			IS_CHANNEL_IN_HISTO	= "isChannelInHisto";

	/**
	 * read complete file data and fill histo record sets. some recordsets from the input file may be discarded based on user's channel setting.
	 * @param HistoSet destination for appending file data.
	 * @param filePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 * @throws DataInconsitsentException
	 */
	public static List<HistoRecordSet> readHisto(String filePath) throws FileNotFoundException, IOException, NotSupportedFileFormatException, DataInconsitsentException {
		List<HistoRecordSet> selectedRecordSets = new ArrayList<HistoRecordSet>();
		log.log(Level.INFO, "start with " + filePath); //$NON-NLS-1$
		filePath = filePath.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
		ZipInputStream zip_input = new ZipInputStream(new FileInputStream(new File(filePath)));
		ZipEntry zip_entry = zip_input.getNextEntry();
		FileInputStream file_input = null;
		DataInputStream data_in = null;
		if (zip_entry != null && zip_entry.getName().equals(filePath.substring(filePath.lastIndexOf(GDE.FILE_SEPARATOR_UNIX) + 1))) {
			data_in = new DataInputStream(zip_input);
		} else {
			zip_input.close();
			zip_input = null;
			file_input = new FileInputStream(new File(filePath));
			data_in = new DataInputStream(file_input);
		}

		final HashMap<String, String> header = getHeader(filePath);
		final String deviceName = header.get(GDE.DEVICE_NAME);
		final String objectKey = header.get(GDE.OBJECT_KEY) != null ? header.get(GDE.OBJECT_KEY) : GDE.STRING_EMPTY;
		boolean histoFilesWithoutObject = false; // TODO new setting
		boolean histoFilesWrongObject = false; // TODO new setting
		if (!deviceName.equals(HistoOsdReaderWriter.application.getActiveDevice().getName())) {
			if (SWT.OK != application.openOkCancelMessageDialog(Messages.getString(MessageIds.GDE_MSGI0063, new String[] { deviceName, filePath }))) {
				return selectedRecordSets;
			}
		} else if (!(application.getActiveObject() == null || objectKey.equals(application.getActiveObject().getKey())
				|| histoFilesWithoutObject && objectKey.equals(GDE.STRING_EMPTY)
				|| histoFilesWrongObject && !objectKey.equals(application.getActiveObject().getKey()))) {
			if (objectKey.equals(GDE.STRING_EMPTY)) {
				if (SWT.YES == application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0061, new String[] { filePath }))) {
					// TODO Settings.HISTO_FILES_WITHOUT_OBJECT = true;
				} else {
					return selectedRecordSets;
				}
			} else {
				if (SWT.YES == application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0062, new String[] { objectKey, filePath }))) {
					// TODO Settings.HISTO_FILES_WRONG_OBJECT = true;
				} else {
					return selectedRecordSets;
				}
			}
		}

		while (!data_in.readUTF().startsWith(GDE.RECORD_SET_SIZE))
			log.log(Level.FINEST, "skip"); //$NON-NLS-1$
		final List<HashMap<String, String>> recordSetsInfo = readRecordSetsInfo4AllVersions(data_in, header);

		try { // build the data structure for the subset which holds channels relevant for histo
			// step 1: build histo record sets with it properties and records without timeline data
			List<HistoRecordSet> recordSets = new ArrayList<HistoRecordSet>();
			for (HashMap<String, String> recordSetInfo : recordSetsInfo) {
				recordSets.add(buildRecordSet(recordSetInfo));
			}
			// step 2: select channels relevant for histo and add timeline data (curves) to records.
			long startTimeNs = System.nanoTime();
			for (int i = 0; i < recordSetsInfo.size(); i++) {
				HashMap<String, String> selectedRecordSetInfo = recordSetsInfo.get(i);
				HistoRecordSet recordSet = recordSets.get(i);

				if (log.isLoggable(Level.FINE))
					log.log(Level.FINE, "start step 2 for RecordSet" + selectedRecordSetInfo.get(GDE.CHANNEL_CONFIG_NAME)); //$NON-NLS-1$
				int recordDataSize = Integer.parseInt(selectedRecordSetInfo.get(GDE.RECORD_DATA_SIZE));
				long recordSetDataPointer = Long.parseLong(selectedRecordSetInfo.get(GDE.RECORD_SET_DATA_POINTER));
				int numberRecordAndTimeStamp = recordSet.getNoneCalculationRecordNames().length + (recordSet.isTimeStepConstant() ? 0 : 1);
				recordSet.setFileDataPointerAndSize(recordSetDataPointer, recordDataSize, GDE.SIZE_BYTES_INTEGER * numberRecordAndTimeStamp * recordDataSize);
				byte[] buffer = new byte[recordSet.getFileDataBytesSize()];
				data_in.readFully(buffer);
				if (Boolean.parseBoolean(selectedRecordSetInfo.get(IS_CHANNEL_IN_HISTO))) {
					log.log(Level.FINE, selectedRecordSetInfo.get(GDE.CHANNEL_CONFIG_NAME) + " is prepared for histo "); //$NON-NLS-1$
					log.log(Level.FINE, "recordDataSize = " + recordDataSize); //$NON-NLS-1$
					log.log(Level.FINE, "recordSetDataPointer = " + recordSetDataPointer); //$NON-NLS-1$
					log.log(Level.FINE, "numberRecordAndTimeStamp = " + numberRecordAndTimeStamp); //$NON-NLS-1$
					recordSet.getDevice().addDataBufferAsRawDataPoints(recordSet, buffer, recordDataSize, false);
					boolean isSettlement = true; // TODO device settings
					recordSet.setReadingsCounter(recordDataSize);
					recordSet.setSampledCounter(((DeviceConfiguration) recordSet.getDevice()).getSampledCounter());
					application.isHistoInProgress = true; // TODO find better solution for screen updates --- IDevice addition?
					// TODO not sure if necessary OsdReaderWriter.application.getActiveDevice().makeInActiveDisplayable(recordSet);
					application.isHistoInProgress = false; // TODO find better solution for screen updates --- IDevice addition?
					recordSet.dispatchLiveStatistics();
					selectedRecordSets.add(recordSet);
				}
			}
			if (log.isLoggable(Level.TIME))
				log.log(Level.TIME, String.format("read time for all recordsets = %,11d ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNs))); //$NON-NLS-1$

		} finally {
			if (zip_input != null) {
				zip_input.closeEntry();
				data_in.close();
				zip_input.close();
			} else if (file_input != null) {
				data_in.close();
				file_input.close();
			}
			data_in = null;
			file_input = null;
			zip_input = null;
		}
		log.log(Level.INFO, "leave  " + filePath); //$NON-NLS-1$
		Collections.reverse(selectedRecordSets); // newer record sets are presumed at the end of the osd file
		return selectedRecordSets;
	}

	/**
	 * parse recordSetInfo from osd file. channel is determined without user interaction.
	 * @return null if this recordSet is not valid for histo (channel not allowed for histo).
	 */
	private static HistoRecordSet buildRecordSet(HashMap<String, String> recordSetInfo) {
		boolean filterByChannel = true; // TODO filterByChannel comes from the current device (HoTTAdapter true, chargers false)
		final String channelConfig = recordSetInfo.get(GDE.CHANNEL_CONFIG_NAME);
		String recordSetName = recordSetInfo.get(GDE.RECORD_SET_NAME);
		recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);
		final String recordSetComment = recordSetInfo.get(GDE.RECORD_SET_COMMENT);
		final String recordSetProperties = recordSetInfo.get(GDE.RECORD_SET_PROPERTIES);
		final String[] recordsProperties = StringHelper.splitString(recordSetInfo.get(GDE.RECORDS_PROPERTIES), Record.END_MARKER, GDE.RECORDS_PROPERTIES);
		final int recordDataSize = Integer.parseInt(recordSetInfo.get(GDE.RECORD_DATA_SIZE));
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
		final boolean isChannelInHisto = !filterByChannel || (recordSetInfoChannel != null && recordSetInfoChannel.getNumber() == channels.getActiveChannelNumber());
		final String channelName = (recordSetInfoChannel == null ? "unknown" : recordSetInfoChannel.getName());
		recordSetInfo.put(GDE.CHANNEL_CONFIG_NAME, channelName); // TODO GDE.CHANNEL_CONFIG_NUMBER ???
		recordSetInfo.put(IS_CHANNEL_IN_HISTO, Boolean.toString(isChannelInHisto));
		if (log.isLoggable(Level.FINER)) {
			StringBuilder sb = new StringBuilder().append(GDE.CHANNEL_CONFIG_NAME).append(GDE.STRING_MESSAGE_CONCAT).append(channelName).append(" is in histo ").append(isChannelInHisto); //$NON-NLS-1$
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

		final double tmpTimeStep_ms = application.getActiveDevice().getTimeStep_ms(); // will be replaced later by setDeserializedProperties
		HistoRecordSet recordSet = HistoRecordSet.createRecordSet(recordSetName, application.getActiveDevice(), recordSetInfoChannel.getNumber(), recordNames, recordSymbols, recordUnits, tmpTimeStep_ms, true, true);
		recordSet.setRecordSetDescription(recordSetComment);
		for (int i = 0; i < recordsProperties.length; ++i) {
			Record record = recordSet.get(recordNames[i]);
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, record.getName() + " - setSerializedProperties "); //$NON-NLS-1$
			record.setSerializedProperties(recordsProperties[i]);
			record.setSerializedDeviceSpecificProperties(recordsProperties[i]);
		}
		// extract additional data
		Double[] recordSetPackagesLost = parsePackageLoss(recordSetComment);
		recordSet.setPackagesLostCounter(recordSetPackagesLost[0] != null ? recordSetPackagesLost[0].intValue() : 0);
		recordSet.setPackagesLostPerMille(recordSetPackagesLost[1] != null ? (int) (recordSetPackagesLost[1] * 10.) : null);
		recordSet.setPackagesCounter(recordSetPackagesLost[0] != null && recordSetPackagesLost[1] != null
				&& recordSetPackagesLost[1] != 0 ? (int) (recordSetPackagesLost[0] * 100. / recordSetPackagesLost[1]) : 0);
		recordSet.setPackagesLostMin_ms(recordSetPackagesLost[2] != null ? (int) (recordSetPackagesLost[2] * 1000.) : null);
		recordSet.setPackagesLostMax_ms(recordSetPackagesLost[3] != null ? (int) (recordSetPackagesLost[3] * 1000.) : null);
		recordSet.setPackagesLostAvg_ms(recordSetPackagesLost[4] != null ? (int) (recordSetPackagesLost[4] * 1000.) : null);
		recordSet.setPackagesLostSigma_ms(recordSetPackagesLost[5] != null ? (int) (recordSetPackagesLost[5] * 1000.) : null);
		recordSet.setSensors(getSensors(recordSetComment));

		recordSet.setDeserializedProperties(recordSetProperties);
		recordSet.setSaved(true);
		return recordSet;
	}

	/**
	 * @param recordSetComment
	 * @return empty array if parsing was not successful.
	 */
	private static String[] getSensors(String recordSetComment) {
		String[] sensors = new String[0];
		int idx1 = recordSetComment.indexOf(GDE.STRING_LEFT_BRACKET);
		int idx2 = recordSetComment.indexOf(GDE.STRING_RIGHT_BRACKET);
		if (idx1 > 0 && idx2 > idx1) {
			sensors = recordSetComment.substring(idx1 + 1, idx2).split(GDE.STRING_COMMA);
		}
		return sensors;
	}

	/**
	 * @param recordSetComment
	 * @return packagesLostCounter, packagesLostRatio, minPackagesLost_sec, maxPackagesLost_sec, avgPackagesLost_sec, sigmaPackagesLost_sec. null if parsing was not successful.
	 */
	private static Double[] parsePackageLoss(String recordSetComment) {
		NumberFormat doubleFormat = NumberFormat.getInstance(Locale.getDefault());
		Double[] values = new Double[6];
		// TODO better solution for Verlorene Rückkanalpakete
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
			} catch (ParseException e) {
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
			} catch (ParseException ex) {
				// ignore and keep recordSetTimestamp as null
			}
		}
		if (recordSetTimestamp == null) {
			simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd" + ' ' + " HH:mm:ss");
			try {
				recordSetTimestamp = simpleDateFormat.parse(recordSetInfo.get(GDE.CREATION_TIME_STAMP));
			} catch (ParseException ex) {
				// ignore and keep recordSetTimestamp as null
			}
			// recordSetTimestamp = recordSetInfo.get(GDE.CREATION_TIME_STAMP);
		}
		return recordSetTimestamp;
	}

}
