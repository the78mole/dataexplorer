/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.io;

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
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.OSDE;
import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.exception.MissMatchDeviceException;
import osde.exception.NotSupportedFileFormatException;
import osde.exception.UnitCompareException;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.StringHelper;

/**
 * Class to read and write comma separated value files
 * @author Winfried Brügmann
 */
public class CSVReaderWriter {
	static Logger					log			= Logger.getLogger(CSVReaderWriter.class.getName());

	static String					lineSep	= OSDE.LINE_SEPARATOR;
	static DecimalFormat	df3			= new DecimalFormat("0.000"); //$NON-NLS-1$
	static StringBuffer		sb;
	static String					line		= OSDE.STRING_STAR;
	final static Channels channels = Channels.getInstance();

	/**
	 * read the device name from selected CSV file
	 * @throws IOException 
	 * @throws NotSupportedFileFormatException 
	 */
//	public static HashMap<String, String> getHeader(char separator, String filePath) throws NotSupportedFileFormatException, IOException {
//		BufferedReader reader = null; // to read the data
//		HashMap<String, String> header = null;
//
//		reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
//		header = getHeader(separator, reader, filePath);
//		reader.close();
//
//		return header;
//	}

	/**
	 * @param separator
	 * @param reader
	 * @param filePath
	 * @return map with OSDE.DEVICE_NAME,OSDE.CSV_DATA_HEADER,[OSDE.CHANNEL_CONFIG_NAME]
	 * @throws NotSupportedFileFormatException
	 * @throws IOException
	 */
	public static HashMap<String, String> getHeader(char separator, String filePath) throws NotSupportedFileFormatException, IOException {
		BufferedReader reader = null; // to read the data
		HashMap<String, String> header = new HashMap<String, String>();

		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
			line = reader.readLine();
			String[] headerData = line.split(OSDE.STRING_EMPTY + separator);
			for (int i = 0; i < headerData.length; i++) {
				if (i == 0) header.put(OSDE.DEVICE_NAME, headerData[i].split("\\r")[0].trim());
				if (i == 1) header.put(OSDE.CHANNEL_CONFIG_NAME, headerData[i].split(" ")[0].split("\\r")[0].trim());
			}
			while (!((line = reader.readLine()).contains("[") && line.contains("]"))) {
				// read until Zeit [sec];Spannung [---];Höhe [---]
				// 						Zeit [s];Spannung [V];Strom [A];Ladung [mAh];Leistung [W];Energie [Wh]
			}
    	header.put(OSDE.CSV_DATA_HEADER, line);

  		if (header.size() >= 1) {
  			log.log(Level.FINE, OSDE.DEVICE_NAME + header.get(OSDE.DEVICE_NAME)); //$NON-NLS-1$
 				log.log(Level.FINE, OSDE.CHANNEL_CONFIG_NAME + (header.get(OSDE.CHANNEL_CONFIG_NAME) != null ? header.get(OSDE.CHANNEL_CONFIG_NAME) : "" )); //$NON-NLS-1$
 				log.log(Level.FINE, OSDE.CSV_DATA_HEADER + (header.get(OSDE.CSV_DATA_HEADER) != null ? header.get(OSDE.CSV_DATA_HEADER) : "")); //$NON-NLS-1$
  		}
  		//Messages.getString(MessageIds.OSDE_MSGW0012, new Object[] {filePath})
  		else {
  			NotSupportedFileFormatException e = new NotSupportedFileFormatException("File does have required header information \"device_name[separator<channel/config_name>]\"");
  			log.log(Level.SEVERE, e.getMessage(), e);
  			throw e;
  		}
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new FileNotFoundException(Messages.getString(MessageIds.OSDE_MSGW0011, new Object[] {filePath}));
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new IOException(Messages.getString(MessageIds.OSDE_MSGW0012, new Object[] {filePath}));
		}
		finally {
			if (reader != null) 		reader.close();
		}

		return header;
	}

	/**
	 * @param header
	 * @throws NotSupportedFileFormatException
	 * @throws UnitCompareException 
	 */
	public static HashMap<String, String> evaluateType(char separator, HashMap<String, String> header, DeviceConfiguration deviceConfig) throws NotSupportedFileFormatException, UnitCompareException {
		int index = 0, innerIndex = 0;
		int countRaw = 0, countAbs = 0;
		String subString;
		boolean isRaw = false;
		
		while ((index = line.indexOf('[', index+1)) != -1) {
			if ((innerIndex = line.indexOf(']', index+1)) != -1) {
				subString = line.substring(index+1, innerIndex);
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
		if(countAbs <= 1 && countRaw > 0 || (line.split(""+separator).length -1)  < deviceConfig.getNumberOfMeasurements(1)) {
			header.put(OSDE.CSV_DATA_TYPE, OSDE.CSV_DATA_TYPE_RAW);
			isRaw = true;
		}
		else {
			header.put(OSDE.CSV_DATA_TYPE, OSDE.CSV_DATA_TYPE_ABS);
			isRaw = false;
		}
		log.log(Level.FINE, OSDE.CSV_DATA_TYPE + header.get(OSDE.CSV_DATA_TYPE));
		
		String channelConfig = header.get(OSDE.CHANNEL_CONFIG_NAME);
		int channelNumber = channels.getChannelNumber(channelConfig);
		if (channelConfig != null 
				&& !channels.getActiveChannel().getConfigKey().equals(channelConfig) 
				&& channelNumber >= 1 && channelNumber <= deviceConfig.getChannelCount() ) {
			channels.setActiveChannelNumber(channelNumber);
		}
		else { // unknown channel configuration using active one
			channelConfig = channels.getActiveChannel().getConfigKey();
			channels.setActiveChannelNumber(channels.getActiveChannelNumber());
		}
		header.put(OSDE.CHANNEL_CONFIG_NAME, channelConfig);
		header.put(OSDE.CHANNEL_CONFIG_NUMBER, ""+channels.getActiveChannelNumber());
		log.log(Level.FINE, OSDE.CHANNEL_CONFIG_NAME + header.get(OSDE.CHANNEL_CONFIG_NUMBER) + " : " + header.get(OSDE.CHANNEL_CONFIG_NAME));
		
		int match = 0; // check match of the measurement units, relevant for absolute csv data only
		if (!isRaw) {
			String [] recordKeys = deviceConfig.getMeasurementNames(channelConfig);
			StringBuilder unitCompare = new StringBuilder().append(lineSep);
			String[] headerLine = line.split(OSDE.STRING_SEMICOLON);
			for (int i = 1; i < headerLine.length; i++) {
				String recordKey = recordKeys[i - 1];
				String expectUnit = deviceConfig.getMeasurementUnit(channelConfig, (i - 1) );
				String[] inMeasurement = headerLine[i].trim().replace(OSDE.STRING_LEFT_BRACKET, OSDE.STRING_SEMICOLON).replace(OSDE.STRING_RIGHT_BRACKET, OSDE.STRING_SEMICOLON).split(OSDE.STRING_SEMICOLON);
				String inUnit = inMeasurement.length == 2 ? inMeasurement[1] : Settings.EMPTY;
				unitCompare.append(recordKey + Messages.getString(MessageIds.OSDE_MSGT0136) + inUnit + Messages.getString(MessageIds.OSDE_MSGT0137) + expectUnit).append(lineSep);
				if (inUnit.equals(expectUnit) || inUnit.equals(Settings.EMPTY)) ++match; 
			}
			log.log(Level.FINE, unitCompare.toString());
			if (match != headerLine.length - 1) {
				throw new UnitCompareException(unitCompare.toString()); // mismatch data header units //$NON-NLS-1$
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
	 * @throws UnitCompareException 
	 */
	public static RecordSet read(char separator, String filePath, String recordSetNameExtend, boolean isRaw) throws NotSupportedFileFormatException, MissMatchDeviceException, IOException, UnitCompareException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
		String recordSetName = null;
		RecordSet recordSet = null;
		BufferedReader reader; // to read the data
		IDevice device = application.getActiveDevice();
		Channel activeChannel = null;

		try {
			activeChannel = channels.getActiveChannel();

			if (activeChannel != null) {
				if (application.getStatusBar() != null) application.setStatusMessage(Messages.getString(MessageIds.OSDE_MSGT0134) + filePath);
				int timeStep_ms = 0, old_time_ms = 0, new_time_ms = 0;
				String[] recordNames = null;

				HashMap<String, String> fileHeader = CSVReaderWriter.getHeader(separator, filePath);
				// check for device name and channel or configuration in first line
				if (!application.getActiveDevice().getName().equals(fileHeader.get(OSDE.DEVICE_NAME))) {
					MissMatchDeviceException e = new MissMatchDeviceException(Messages.getString(MessageIds.OSDE_MSGT0135) + line); // mismatch device name 
					log.log(Level.SEVERE, e.getMessage(), e);
					throw e;
				}
				
				fileHeader = CSVReaderWriter.evaluateType(separator, fileHeader, (DeviceConfiguration)device);			
				log.log(Level.FINE, "device name check ok, channel/configuration ok"); //$NON-NLS-1$

				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
				while (!((line = reader.readLine()).contains("[") && line.contains("]"))) {
					// read until Zeit [sec];Spannung [---];Höhe [---]
					// 						Zeit [s];Spannung [V];Strom [A];Ladung [mAh];Leistung [W];Energie [Wh]
				}

				recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$
				// shorten the record set name to the allowed maximum
				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, 30);

				String fileConfig = fileHeader.get(OSDE.CHANNEL_CONFIG_NAME);
				if (application.getStatusBar() != null) {
					channels.switchChannel(channels.getActiveChannelNumber(), OSDE.STRING_EMPTY);
					application.getMenuToolBar().updateChannelSelector();
					activeChannel = channels.getActiveChannel();
				}
				
				recordSet = RecordSet.createRecordSet(fileConfig, recordSetName, application.getActiveDevice(), isRaw, true);

				recordNames = recordSet.getRecordNames();

				// make all records displayable since absolute data
				if (!isRaw) { // absolute
					for (String recordKey : recordNames) {
						recordSet.get(recordKey).setDisplayable(true); // all data available 
					}
				}

				recordNames = fileHeader.get(OSDE.CSV_DATA_HEADER).split(OSDE.STRING_EMPTY+separator);

				// now get all data   0; 14,780;  0,598;  1,000;  8,838;  0,002
				while ((line = reader.readLine()) != null) {
					String[] dataStr = line.split(OSDE.STRING_EMPTY + separator);
					String data = dataStr[0].trim().replace(',', '.');
					new_time_ms = (int) (new Double(data).doubleValue() * 1000);
					timeStep_ms = new_time_ms - old_time_ms;
					old_time_ms = new_time_ms;
					sb = new StringBuffer().append(lineSep);
					
					for (int i = 0; i < recordNames.length-1; i++) {
						Record record = recordSet.getRecord(recordNames[i+1].split("\\[")[0].trim());
						if (record != null) {
							data = dataStr[i + 1].trim().replace(',', '.');
							double tmpDoubleValue = new Double(data).doubleValue();
							double dPoint = tmpDoubleValue > 500000 ? tmpDoubleValue : tmpDoubleValue * 1000; // multiply by 1000 reduces rounding errors for small values
							int point = new Double(dPoint).intValue();
							if (log.isLoggable(Level.FINE)) sb.append("recordKeys[" + i + "] = ").append(recordNames[i]).append(" = ").append(point).append(lineSep); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							record.add(point);
						}
						log.log(Level.FINE, "add point data to recordKeys[" + i + "] = " + recordNames[i]); //$NON-NLS-1$ //$NON-NLS-2$
					}
					log.log(Level.FINE, sb.toString());
				}

				// set time base in msec
				recordSet.setTimeStep_ms(timeStep_ms);
				log.log(Level.FINE, "timeStep_ms = " + timeStep_ms); //$NON-NLS-1$
				recordSet.setSaved(true);

				activeChannel.put(recordSetName, recordSet);
				activeChannel.setActiveRecordSet(recordSetName);
				activeChannel.applyTemplate(recordSetName);
				if (application.getStatusBar() != null) activeChannel.switchRecordSet(recordSetName);
				//				activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records

				reader.close();
				reader = null;
			}
		}
		catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new UnsupportedEncodingException(Messages.getString(MessageIds.OSDE_MSGW0010));
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new FileNotFoundException(Messages.getString(MessageIds.OSDE_MSGW0011, new Object[] {filePath}));
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new IOException(Messages.getString(MessageIds.OSDE_MSGW0012, new Object[] {filePath}));
		}
		finally {
			if (application.getStatusBar() != null) {
				if (device.isTableTabRequested())
					application.setProgress(10, sThreadId);
				else
					application.setProgress(100, sThreadId);
				
				application.setStatusMessage(OSDE.STRING_EMPTY);
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
		OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
		try {
			if (application.getStatusBar() != null) application.setStatusMessage(Messages.getString(MessageIds.OSDE_MSGT0138) + filePath);
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
			char decimalSeparator = Settings.getInstance().getDecimalSeparator();

			df3.setGroupingUsed(false);
			sb = new StringBuffer();
			RecordSet recordSet = Channels.getInstance().getActiveChannel().get(recordSetKey);
			IDevice device = OpenSerialDataExplorer.getInstance().getActiveDevice();
			// write device name , manufacturer, and serial port string
			sb.append(device.getName()).append(separator).append(recordSet.getChannelConfigName()).append(lineSep);
			writer.write(sb.toString());
			log.log(Level.FINE, "written header line = " + sb.toString());  //$NON-NLS-1$
			
			sb = new StringBuffer();
			sb.append("Zeit [sec]").append(separator); // Spannung [V];Strom [A];Ladung [Ah];Leistung [W];Energie [Wh]"; //$NON-NLS-1$
			// write the measurements signature
			String[] recordNames = device.getMeasurementNames(recordSet.getChannelConfigName());
			for (int i = 0; i < recordNames.length; i++) {
				MeasurementType  measurement = device.getMeasurement(recordSet.getChannelConfigName(), i);
				log.log(Level.FINEST, "append " + recordNames[i]); //$NON-NLS-1$
				if (isRaw) {
					if (!measurement.isCalculation()) {	// only use active records for writing raw data 
						sb.append(measurement.getName()).append(" [---]").append(separator);	 //$NON-NLS-1$
						log.log(Level.FINEST, "append " + recordNames[i]); //$NON-NLS-1$
					}
				}
				else {
					sb.append(measurement.getName()).append(" [").append(measurement.getUnit()).append(']').append(separator);	 //$NON-NLS-1$
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
				sb.append((df3.format(new Double(i * recordSet.getTimeStep_ms() / 1000.0))).replace('.', decimalSeparator)).append(separator).append(' ');
				// add data entries
				for (int j = 0; j < recordNames.length; j++) {
					Record record = recordSet.getRecord(recordNames[j]);
					if (record == null)
						throw new Exception(Messages.getString(MessageIds.OSDE_MSGE0005, new Object[]{recordNames[j], recordSet.getChannelConfigName()}));

					MeasurementType measurement = device.getMeasurement(recordSet.getChannelConfigName(), j);
					if (isRaw) { // do not change any values
						if (!measurement.isCalculation())
							if (record.getParent().isRaw())
								sb.append(df3.format(new Double(record.get(i))/1000.0).replace('.', decimalSeparator)).append(separator);
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
			log.log(Level.FINE, "CSV file = " + filePath + " erfolgreich geschieben"  //$NON-NLS-1$ //$NON-NLS-2$
					+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$

			writer.flush();
			writer.close();
			writer = null;
			//recordSet.setSaved(true);
			if (application.getStatusBar() != null) application.setProgress(100, sThreadId);
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.OSDE_MSGE0006, new Object[]{ filePath }));
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.OSDE_MSGE0007) + e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage()); 
		}
		finally {
			if (application.getStatusBar() != null) application.setStatusMessage(OSDE.STRING_EMPTY);
		}

	}

}
