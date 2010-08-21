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
    
    Copyright (c) 2008,2009,2010 Winfried Bruegmann
****************************************************************************************/
package gde.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import gde.log.Level;
import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.MissMatchDeviceException;
import gde.exception.NotSupportedFileFormatException;
import gde.exception.UnitCompareException;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

/**
 * Class to read and write comma separated value files
 * @author Winfried Brügmann
 */
public class CSVReaderWriter {
	static Logger					log			= Logger.getLogger(CSVReaderWriter.class.getName());

	static String					lineSep	= GDE.LINE_SEPARATOR;
	static DecimalFormat	df3			= new DecimalFormat("0.000"); //$NON-NLS-1$
	static StringBuffer		sb;
	
	final static DataExplorer	application	= DataExplorer.getInstance();
	final static Channels								channels		= Channels.getInstance();

	/**
	 * read the first two line of CSV file and prepare a map with all available information 
	 * @param separator
	 * @param filePath
	 * @return map with GDE.DEVICE_NAME,GDE.CSV_DATA_HEADER,[GDE.CHANNEL_CONFIG_NAME]
	 * @throws NotSupportedFileFormatException
	 * @throws IOException
	 */
	public static HashMap<String, String> getHeader(char separator, String filePath) throws NotSupportedFileFormatException, IOException {
		String					line		= GDE.STRING_STAR;
		BufferedReader reader = null; // to read the data
		HashMap<String, String> header = new HashMap<String, String>();

		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
			line = reader.readLine();
			String[] headerData = line.split(GDE.STRING_EMPTY + separator);
			if (headerData.length > 2) {
  			NotSupportedFileFormatException e = new NotSupportedFileFormatException("File does have required header information \"device_name[separator<channel/config_name>]\"");
  			log.log(Level.SEVERE, e.getMessage(), e);
  			throw e;
			}
			for (int i = 0; i < headerData.length; i++) {
				if (i == 0) header.put(GDE.DEVICE_NAME, headerData[i].split("\\r")[0].trim());
				if (i == 1) header.put(GDE.CHANNEL_CONFIG_NAME, headerData[i].split(" ")[0].split("\\r")[0].trim());
			}
			while (!((line = reader.readLine()) != null && line.contains("[") && line.contains("]"))) {
				// read until Zeit [sec];Spannung [---];Höhe [---]
				// 						Zeit [s];Spannung [V];Strom [A];Ladung [mAh];Leistung [W];Energie [Wh]
			}
    	header.put(GDE.CSV_DATA_HEADER, line);

  		if (header.size() >= 1) {
  			log.log(Level.FINE, GDE.DEVICE_NAME + header.get(GDE.DEVICE_NAME)); //$NON-NLS-1$
 				log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + (header.get(GDE.CHANNEL_CONFIG_NAME) != null ? header.get(GDE.CHANNEL_CONFIG_NAME) : "" )); //$NON-NLS-1$
 				log.log(Level.FINE, GDE.CSV_DATA_HEADER + (header.get(GDE.CSV_DATA_HEADER) != null ? header.get(GDE.CSV_DATA_HEADER) : "")); //$NON-NLS-1$
  		}
  		
  		else {
  			NotSupportedFileFormatException e = new NotSupportedFileFormatException("File does have required header information \"device_name[separator<channel/config_name>]\"");
  			log.log(Level.SEVERE, e.getMessage(), e);
  			throw e;
  		}
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new FileNotFoundException(Messages.getString(MessageIds.GDE_MSGW0011, new Object[] {filePath}));
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new IOException(Messages.getString(MessageIds.GDE_MSGW0012, new Object[] {filePath}));
		}
		finally {
			if (reader != null) 		reader.close();
		}

		return header;
	}

	/**
	 * evaluate channel/configuartion and check units for absolute data
	 * <ul>
	 * <li>if the channel/configuration does not match device the first channel/configuration of the device will choosen
	 * <li>if units of absolute data will not match a warning dialog will show all red measurements kesy with expected units 
	 * </ul>
	 * @param header
	 */
	public static HashMap<String, String> evaluateType(char separator, HashMap<String, String> header, DeviceConfiguration deviceConfig) {
		int index = 0, innerIndex = 0;
		int countRaw = 0, countAbs = 0;
		StringBuilder sb_measurements = new StringBuilder();
		StringBuilder sb_units = new StringBuilder();
		String subString;
		boolean isRaw = false;
		
		String headerLine = header.get(GDE.CSV_DATA_HEADER);
		while ((index = headerLine.indexOf('[', index+1)) != -1) {
			if ((innerIndex = headerLine.indexOf(']', index+1)) != -1) {
				subString = headerLine.substring(index+1, innerIndex);
				if (subString.split("-").length == 0) {
					++countRaw;
				}
				else																			 {
					++countAbs;
				}
			}
			++index;
		}
		
		// Spannung;Strom;Ladung; < Spannung;Strom;Ladung;Leistung;Energie; 	
		String[] headerLineArray = headerLine.split(GDE.STRING_EMPTY+separator);
		if(countAbs <= 1 && countRaw > 0 || (headerLineArray.length - 1)  < deviceConfig.getNumberOfMeasurements(1)) {
			header.put(GDE.CSV_DATA_TYPE, GDE.CSV_DATA_TYPE_RAW);
			isRaw = true;
		}
		else {
			header.put(GDE.CSV_DATA_TYPE, GDE.CSV_DATA_TYPE_ABS);
			isRaw = false;
		}
		log.log(Level.FINE, GDE.CSV_DATA_TYPE + header.get(GDE.CSV_DATA_TYPE));
		
		String channelConfig = header.get(GDE.CHANNEL_CONFIG_NAME);
		int channelNumber = channels.getChannelNumber(channelConfig);
		if (channelConfig != null 
				&& !channels.getActiveChannel().getChannelConfigKey().equals(channelConfig) 
				&& channelNumber >= 1 && channelNumber <= deviceConfig.getChannelCount() ) {
			channels.setActiveChannelNumber(channelNumber);
		}
		else { // unknown channel configuration using active one
			channelConfig = channels.getActiveChannel().getChannelConfigKey();
			channels.setActiveChannelNumber(channels.getActiveChannelNumber());
			channelNumber = channels.getActiveChannelNumber();
		}
		header.put(GDE.CHANNEL_CONFIG_NAME, channelConfig);
		header.put(GDE.CHANNEL_CONFIG_NUMBER, ""+channels.getActiveChannelNumber());
		log.log(Level.FINE, GDE.CHANNEL_CONFIG_NAME + header.get(GDE.CHANNEL_CONFIG_NUMBER) + " : " + header.get(GDE.CHANNEL_CONFIG_NAME));
		
		int match = 0; // check match of the measurement units, relevant for absolute csv data only
		StringBuilder unitCompare = new StringBuilder().append(lineSep);
		for (int i = 1; i < headerLineArray.length; i++) {
			String expectUnit = deviceConfig.getMeasurementUnit(channelNumber, (i - 1));
			String[] inHeaderMeasurement = headerLineArray[i].trim().split("\\[|]"); //$NON-NLS-1$
			String inMeasurement = inHeaderMeasurement.length >= 1 ? inHeaderMeasurement[0].trim() : Settings.EMPTY;
			String inUnit = inHeaderMeasurement.length == 2 ? inHeaderMeasurement[1].trim() : Settings.EMPTY;
			sb_measurements.append(inMeasurement).append(GDE.STRING_SEMICOLON);
			sb_units.append(inUnit).append(GDE.STRING_SEMICOLON);
			unitCompare.append(String.format("%-30s \t%s \n", inMeasurement, Messages.getString(MessageIds.GDE_MSGT0136, new Object[]{inUnit, expectUnit})));
			if (inUnit.equals(expectUnit) || inUnit.equals(Settings.EMPTY)) ++match;
		}
		header.put(GDE.CSV_DATA_HEADER_MEASUREMENTS, sb_measurements.toString());
		header.put(GDE.CSV_DATA_HEADER_UNITS, sb_units.toString());

		if (!isRaw) {
			log.log(Level.FINE, unitCompare.toString());
			if (match != headerLineArray.length - 1) {
				UnitCompareException e = new UnitCompareException(Messages.getString(MessageIds.GDE_MSGW0015, new Object[] {unitCompare.toString()})); // mismatch data header units //$NON-NLS-1$
				log.log(Level.WARNING, e.getMessage());
				application.openMessageDialogAsync(e.getMessage());
			}
		}
				
		return header;
	}

	/**
	 * read the selected CSV file
	 * @return record set created
	 * @throws NotSupportedFileFormatException 
	 * @throws MissMatchDeviceException 
	 * @throws IOException 
	 * @throws DataInconsitsentException 
	 * @throws DataTypeException 
	 */
	public static RecordSet read(char separator, String filePath, String recordSetNameExtend, boolean isRaw) throws NotSupportedFileFormatException, MissMatchDeviceException, IOException, DataInconsitsentException, DataTypeException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		String line = GDE.STRING_STAR;
		RecordSet recordSet = null;
		BufferedReader reader; // to read the data
		IDevice device = application.getActiveDevice();
		Channel activeChannel = null;

		try {
			HashMap<String, String> fileHeader = CSVReaderWriter.getHeader(separator, filePath);
			activeChannel = channels.get(channels.getChannelNumber(fileHeader.get(GDE.CHANNEL_CONFIG_NAME)));
			activeChannel = activeChannel == null ? channels.getActiveChannel() : activeChannel;

			if (activeChannel != null) {
				if (application.getStatusBar() != null) application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0134) + filePath);
				int time_ms = 0;

				// check for device name and channel or configuration in first line
				if (!application.getActiveDevice().getName().equals(fileHeader.get(GDE.DEVICE_NAME))) {
					MissMatchDeviceException e = new MissMatchDeviceException(Messages.getString(MessageIds.GDE_MSGW0013, new Object[] {fileHeader.get(GDE.DEVICE_NAME)})); // mismatch device name 
					log.log(Level.SEVERE, e.getMessage(), e);
					throw e;
				}
				
				fileHeader = CSVReaderWriter.evaluateType(separator, fileHeader, (DeviceConfiguration)device);			
				if (isRaw != fileHeader.get(GDE.CSV_DATA_TYPE).equals(GDE.CSV_DATA_TYPE_RAW)) {
					throw new DataTypeException(Messages.getString(MessageIds.GDE_MSGW0014));
				}
				log.log(Level.FINE, "device name check ok, channel/configuration ok"); //$NON-NLS-1$
				
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
				while (!(((line = reader.readLine())!= null) && line.contains("[") && line.contains("]"))) {
					// read until Zeit [sec];Spannung [---];Höhe [---]
					// 						Zeit [s];Spannung [V];Strom [A];Ladung [mAh];Leistung [W];Energie [Wh]
				}

				if (application.getStatusBar() != null) {
					channels.switchChannel(activeChannel.getNumber(), GDE.STRING_EMPTY);
					application.getMenuToolBar().updateChannelSelector();
					activeChannel = channels.getActiveChannel();
				}
				
				String recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$
				String[] recordNames = device.getMeasurementNames(activeChannel.getNumber());
				String[] recordSymbols = new String[recordNames.length];
				String[] recordUnits = new String[recordNames.length];
				String[] tmpRecordNames	=	fileHeader.get(GDE.CSV_DATA_HEADER_MEASUREMENTS).split(GDE.STRING_SEMICOLON);
				String[] tmpRecordUnits = fileHeader.get(GDE.CSV_DATA_HEADER_UNITS).split(GDE.STRING_SEMICOLON);
				for (int i=0, j=0; i < recordNames.length; i++) {
					MeasurementType measurement = device.getMeasurement(activeChannel.getNumber(), i);
					if (isRaw) {
						if (!measurement.isCalculation()) {
							recordNames[i] = recordNames[i].equals(tmpRecordNames[j]) ? recordNames[i] : tmpRecordNames[j];
							recordSymbols[i] = measurement.getSymbol();
							recordUnits[i] = measurement.getUnit();
							++j;
						}
						else {
							recordSymbols[i] = measurement.getSymbol();
							recordUnits[i] = measurement.getUnit();
						}
					}
					else {
						recordNames[i] = recordNames[i].equals(tmpRecordNames[i]) ? recordNames[i] : tmpRecordNames[i];
						recordSymbols[i] = recordNames[i].equals(tmpRecordNames[i]) ? measurement.getSymbol() : "";
						recordUnits[i] = measurement.getUnit().equals(tmpRecordUnits[i]) ? measurement.getUnit() : tmpRecordUnits[i];
					}
				}
				recordSet = RecordSet.createRecordSet(recordSetName, device, activeChannel.getNumber(), recordNames, recordSymbols, recordUnits, device.getTimeStep_ms(), isRaw, true);
				recordSetName = recordSet.getName(); // cut length
				
				// make all records displayable while absolute data
				if (!isRaw) { // absolute
					for (String recordKey : recordNames) {
						recordSet.get(recordKey).setDisplayable(true); // all data available 
					}
				}

				// now get all data   0; 14,780;  0,598;  1,000;  8,838;  0,002
				String[] updateRecordNames = isRaw ? recordSet.getNoneCalculationRecordNames() : recordNames;
				int[] points = new int[updateRecordNames.length];
				while ((line = reader.readLine()) != null) {
					String[] dataStr = line.split(GDE.STRING_EMPTY + separator);
					String data = dataStr[0].trim().replace(',', '.');
					time_ms = (int) (new Double(data).doubleValue() * 1000);
					
					for (int i = 0; i < updateRecordNames.length; i++) { // only iterate over record names found in file
						data = dataStr[i + 1].trim().replace(',', '.');
						points[i] = Double.valueOf(data).intValue()*1000;
					}
					if (isRaw) 	recordSet.addNoneCalculationRecordsPoints(points, time_ms);
					else 				recordSet.addPoints(points, time_ms);
				}

				recordSet.setSaved(true);

				activeChannel.put(recordSetName, recordSet);
				activeChannel.setActiveRecordSet(recordSetName);
				activeChannel.applyTemplate(recordSetName, true);
				if (application.getStatusBar() != null) activeChannel.switchRecordSet(recordSetName);
				//				activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records

				reader.close();
				reader = null;
			}
		}
		catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new UnsupportedEncodingException(Messages.getString(MessageIds.GDE_MSGW0010));
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new FileNotFoundException(Messages.getString(MessageIds.GDE_MSGW0011, new Object[] {filePath}));
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new IOException(Messages.getString(MessageIds.GDE_MSGW0012, new Object[] {filePath}));
		}
		finally {
			if (application.getStatusBar() != null) {
				application.setProgress(10, sThreadId);
				application.setStatusMessage(GDE.STRING_EMPTY);
			}
		}
		
		return recordSet;
	}

	/**
	 * write data CVS file
	 * @throws Exception 
	 */
	public static void write(char separator, String recordSetKey, String filePath, boolean isRaw) throws Exception {
		BufferedWriter writer;
		String sThreadId = String.format("%06d", Thread.currentThread().getId());

		try {
			if (application.getStatusBar() != null) application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0138) + filePath);
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
			char decimalSeparator = Settings.getInstance().getDecimalSeparator();

			df3.setGroupingUsed(false);
			sb = new StringBuffer();
			RecordSet recordSet = Channels.getInstance().getActiveChannel().get(recordSetKey);
			IDevice device = DataExplorer.getInstance().getActiveDevice();
			// write device name , manufacturer, and serial port string
			sb.append(device.getName()).append(separator).append(recordSet.getChannelConfigName()).append(lineSep);
			writer.write(sb.toString());
			log.log(Level.FINE, "written header line = " + sb.toString());  //$NON-NLS-1$
			
			sb = new StringBuffer();
			sb.append(Messages.getString(MessageIds.GDE_MSGT0137)).append(separator); // Spannung [V];Strom [A];Ladung [Ah];Leistung [W];Energie [Wh]"; //$NON-NLS-1$
			// write the measurements signature
			String[] recordNames = recordSet.getRecordNames();
			for (int i = 0; i < recordNames.length; i++) {
				MeasurementType  measurement = device.getMeasurement(recordSet.getChannelConfigNumber(), i);
				Record record = recordSet.get(recordNames[i]);
				log.log(Level.FINEST, "append " + recordNames[i]); //$NON-NLS-1$
				if (isRaw) {
					if (!measurement.isCalculation()) {	// only use active records for writing raw data 
						sb.append(recordNames[i]).append(" [---]").append(separator);	 //$NON-NLS-1$
						log.log(Level.FINEST, "append " + recordNames[i]); //$NON-NLS-1$
					}
				}
				else {
					sb.append(recordNames[i]).append(" [").append(record.getUnit()).append(']').append(separator);	 //$NON-NLS-1$
					log.log(Level.FINEST, "append " + recordNames[i]); //$NON-NLS-1$
				}
			}
			sb.deleteCharAt(sb.length() - 1).append(lineSep);
			log.log(Level.FINER, "header line = " + sb.toString()); //$NON-NLS-1$
			writer.write(sb.toString());

			// write data
			long startTime = new Date().getTime();
			int recordEntries = recordSet.getRecordDataSize(true);
			int progressCycle = 0;
			if (application.getStatusBar() != null) application.setProgress(progressCycle, sThreadId);
			for (int i = 0; i < recordEntries; i++) {
				sb = new StringBuffer();
				// add time entry
				sb.append((df3.format((recordSet.getTime_ms(i) / 1000.0))).replace('.', decimalSeparator)).append(separator).append(' ');
				// add data entries
				for (int j = 0; j < recordNames.length; j++) {
					Record record = recordSet.getRecord(recordNames[j]);
					if (record == null)
						throw new Exception(Messages.getString(MessageIds.GDE_MSGE0005, new Object[]{recordNames[j], recordSet.getChannelConfigName()}));

					MeasurementType measurement = device.getMeasurement(recordSet.getChannelConfigNumber(), j);
					if (isRaw) { // do not change any values
						if (!measurement.isCalculation())
							if (record.getParent().isRaw())
								sb.append(df3.format((record.get(i)/1000.0)).replace('.', decimalSeparator)).append(separator);
							else
								sb.append(df3.format(device.reverseTranslateValue(record, record.get(i)/1000.0)).replace('.', decimalSeparator)).append(separator);
					}
					else
						// translate according device and measurement unit
						sb.append(df3.format(device.translateValue(record, record.get(i)/1000.0)).replace('.', decimalSeparator)).append(separator);
				}
				sb.deleteCharAt(sb.length() - 1).append(lineSep);
				writer.write(sb.toString());
				if (application.getStatusBar() != null && i % 50 == 0) application.setProgress(((++progressCycle*5000)/recordEntries), sThreadId);
				log.log(Level.FINE, "data line = " + sb.toString()); //$NON-NLS-1$
			}
			sb = null;
			log.log(Level.TIME, "CSV file = " + filePath + " erfolgreich geschieben"  //$NON-NLS-1$ //$NON-NLS-2$
					+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$

			writer.flush();
			writer.close();
			writer = null;
			//recordSet.setSaved(true);
			if (application.getStatusBar() != null) application.setProgress(100, sThreadId);
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0006, new Object[]{ GDE.FILE_ENDING_CSV, filePath, e.getMessage() }));
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0007) + e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage()); 
		}
		finally {
			if (application.getStatusBar() != null) application.setStatusMessage(GDE.STRING_EMPTY);
		}

	}

}
