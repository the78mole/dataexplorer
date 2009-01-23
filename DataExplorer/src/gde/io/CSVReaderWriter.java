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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;

import osde.OSDE;
import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.StringHelper;

/**
 * Class to read and write comma separated value files
 * @author Winfried Brügmann
 */
public class CSVReaderWriter {
	private static Logger					log			= Logger.getLogger(CSVReaderWriter.class.getName());

	private static String					lineSep	= System.getProperty("line.separator"); //$NON-NLS-1$
	private static DecimalFormat	df3			= new DecimalFormat("0.000"); //$NON-NLS-1$
	private static StringBuffer		sb;
	private static String					line		= OSDE.STRING_STAR;

	/**
	 * read the device name from selected CSV file
	 * @throws Exception 
	 */
	public static String read(char separator, String filePath) throws Exception {
		BufferedReader reader; // to read the data

		reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$

		line = reader.readLine();
		String fileDeviceName = line.split(OSDE.STRING_EMPTY + separator)[0].trim();
		log.log(Level.FINE, "file device name = " + fileDeviceName); //$NON-NLS-1$

		reader.close();
		return fileDeviceName;
	}

	/**
	 * read the selected CSV file
	 * @throws Exception 
	 */
	public static RecordSet read(char separator, String filePath, String recordSetNameExtend, boolean isRaw) throws Exception {
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
		String recordSetName = null;
		RecordSet recordSet = null;
		BufferedReader reader; // to read the data
		IDevice device = application.getActiveDevice();
		int sizeRecords = 0;
		boolean isDeviceName = true;
		boolean isData = false;
		Channels channels = Channels.getInstance();
		Channel activeChannel = null;

		try {
			activeChannel = channels.getActiveChannel();

			if (activeChannel != null) {
				application.setStatusMessage(Messages.getString(MessageIds.OSDE_MSGT0134) + filePath);
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1")); //$NON-NLS-1$
				int timeStep_ms = 0, old_time_ms = 0, new_time_ms = 0;
				StringBuilder headerStringConf = new StringBuilder().append(lineSep);
				StringBuilder keys = new StringBuilder();
				String[] recordKeys = null;
				String fileConfig = null;
				
				// check for device name and channel or configuration in first line
				while (isDeviceName && (line = reader.readLine()) != null) {
					String activeDeviceName = application.getActiveDevice().getName();
					String fileDeviceName = line.split(OSDE.STRING_EMPTY + separator)[0].trim();
					log.log(Level.FINE, "active device name = " + activeDeviceName + ", file device name = " + fileDeviceName); //$NON-NLS-1$ //$NON-NLS-2$

					if (activeDeviceName.equals(fileDeviceName)) {
						isDeviceName = false;
					}
					else {
						throw new Exception("0" + lineSep + Messages.getString(MessageIds.OSDE_MSGT0135) + line); // mismatch device name //$NON-NLS-1$
					}

					String activeConfig = channels.getActiveChannel().getConfigKey();
					fileConfig = line.split(OSDE.STRING_EMPTY + separator).length > 1 ? line.split(OSDE.STRING_EMPTY + separator)[1].trim() : null;
					log.log(Level.FINE, "active channel name = " + activeConfig + ", file channel name = " + fileConfig); //$NON-NLS-1$ //$NON-NLS-2$
					if (fileConfig == null) {
						fileConfig = activeConfig;
						log.log(Level.FINE, "using as file channel name = " + fileConfig); //$NON-NLS-1$
					}
					else if (!activeConfig.equals(fileConfig)) {
						//check if config exist
						int channelNumber = channels.getChannelNumber(fileConfig);
						if (channelNumber != 0) { // 0 channel configuration does not exist
							String msg = Messages.getString(MessageIds.OSDE_MSGW0008);
							int answer = application.openYesNoCancelMessageDialog(msg);
							if (answer == SWT.YES) {
								log.log(Level.FINE, "SWT.YES"); //$NON-NLS-1$
								channels.setActiveChannelNumber(channelNumber);
								channels.switchChannel(channelNumber, OSDE.STRING_EMPTY);
								application.getMenuToolBar().updateChannelSelector();
								activeChannel = channels.getActiveChannel();
							}
							else if (answer == SWT.NO) {
								log.log(Level.FINE, "SWT.NO"); //$NON-NLS-1$
								fileConfig = channels.getActiveChannel().getConfigKey();
							}
							else {
								log.log(Level.FINE, "SWT.CANCEL"); //$NON-NLS-1$
								return null;
							}
						}
						else {
							String msg = Messages.getString(MessageIds.OSDE_MSGW0009);
							int answer = application.openOkCancelMessageDialog(msg);
							if (answer == SWT.OK) {
								log.log(Level.FINE, "SWT.OK"); //$NON-NLS-1$
								fileConfig = channels.getActiveChannel().getConfigKey();
							}
							else {
								log.log(Level.FINE, "SWT.CANCEL"); //$NON-NLS-1$
								return null;
							}
						}
					}
				} // end isDeviceName
				log.log(Level.FINE, "device name check ok, channel/configuration ok"); //$NON-NLS-1$
					
				recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$
				// shorten the record set name to the allowed maximum
				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, 30);

				recordSet = RecordSet.createRecordSet(fileConfig, recordSetName, application.getActiveDevice(), isRaw, true);

				String[] recordNames = recordSet.getRecordNames();

				//
				while (!isData && (line = reader.readLine()) != null) {
					// second line -> Zeit [s];Spannung [V];Strom [A];Ladung [Ah];Leistung [W];Energie [Wh]
					String[] header = line.split(OSDE.STRING_SEMICOLON);
					sizeRecords = header.length - 1;
					int countNotMeasurement = 0;
					for (int i=0; i<recordNames.length; ++i) {
						String recordKey = recordNames[i];
						MeasurementType measurement = device.getMeasurement(fileConfig, i);
						headerStringConf.append(measurement.getName()).append(separator);

						log.log(Level.FINE, measurement.getName() + " isCalculation = " + measurement.isCalculation()); //$NON-NLS-1$
						if (!measurement.isCalculation()) {
							keys.append(measurement.getName()).append(separator);
							++countNotMeasurement; // update count for possible raw
						}
						if (!isRaw) // absolute
							recordSet.get(recordKey).setDisplayable(true); // all data available 
					}
					// for raw data check if measurements which !isCalculation match number of entries in header line
					// first simple check, but name must not match, count only numbers
					if (sizeRecords != countNotMeasurement && isRaw || sizeRecords != recordNames.length && !isRaw) {
						throw new Exception("1" + headerStringConf.toString() + lineSep + keys.toString()); // mismatch data signature length //$NON-NLS-1$
					}

					int match = 0; // check match of the measurement units, relevant for absolute import 

					if (isRaw)
						recordNames = recordKeys = keys.toString().split(OSDE.STRING_EMPTY + separator);
					else
						recordKeys = recordNames;

					// check units for absolute (!raw) data only
					// absolute data will not have any calculation
					// unit for raw data might not meaningful which require some calculation to get a unit
					if (!isRaw) {
						StringBuilder unitCompare = new StringBuilder().append(lineSep);
						for (int i = 1; i < header.length; i++) {
							String recordKey = recordKeys[i - 1];
							String expectUnit = device.getMeasurementUnit(fileConfig, (i - 1) );
							String[] inMeasurement = header[i].trim().replace(OSDE.STRING_LEFT_BRACKET, OSDE.STRING_SEMICOLON).replace(OSDE.STRING_RIGHT_BRACKET, OSDE.STRING_SEMICOLON).split(OSDE.STRING_SEMICOLON);
							String inUnit = inMeasurement.length == 2 ? inMeasurement[1] : Settings.EMPTY;
							unitCompare.append(recordKey + Messages.getString(MessageIds.OSDE_MSGT0136) + inUnit + Messages.getString(MessageIds.OSDE_MSGT0137) + expectUnit).append(lineSep);
							if (inUnit.equals(expectUnit) || inUnit.equals(Settings.EMPTY)) ++match; //$NON-NLS-1$
						}
						log.log(Level.FINE, unitCompare.toString());
						if (match != header.length - 1) {
							throw new Exception("2" + unitCompare.toString()); // mismatch data header units //$NON-NLS-1$
						}
					}
					isData = true;
				} // while !isData

				// get all data   0; 14,780;  0,598;  1,000;  8,838;  0,002
				while ((line = reader.readLine()) != null && isData) {
					String[] dataStr = line.split(OSDE.STRING_EMPTY + separator);
					String data = dataStr[0].trim().replace(',', '.');
					new_time_ms = (int) (new Double(data).doubleValue() * 1000);
					timeStep_ms = new_time_ms - old_time_ms;
					old_time_ms = new_time_ms;
					sb = new StringBuffer().append(lineSep);
					// use only measurement which are isCalculation == false
					for (int i = 0; i < sizeRecords; i++) {
						data = dataStr[i + 1].trim().replace(',', '.');
						double tmpDoubleValue = new Double(data).doubleValue();
						double dPoint = tmpDoubleValue > 500000 ? tmpDoubleValue : tmpDoubleValue * 1000; // multiply by 1000 reduces rounding errors for small values
						int point = (int) dPoint;
						if (log.isLoggable(Level.FINE)) sb.append("recordKeys[" + i + "] = ").append(recordNames[i]).append(" = ").append(point).append(lineSep); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						recordSet.getRecord(recordNames[i]).add(point);
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
				activeChannel.switchRecordSet(recordSetName);
//				activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records

				reader.close();
				reader = null;
			}
		}
		catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.OSDE_MSGW0010));
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.OSDE_MSGW0011, new Object[] {filePath}));
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.OSDE_MSGW0012, new Object[] {filePath}));
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			String msg = null;
			if (e.getMessage().startsWith("0")) //$NON-NLS-1$
				msg = Messages.getString(MessageIds.OSDE_MSGW0013) + e.getMessage().substring(1);
			else if (e.getMessage().startsWith("1")) //$NON-NLS-1$
				msg = Messages.getString(MessageIds.OSDE_MSGW0014) + e.getMessage().substring(1);
			else if (e.getMessage().startsWith("2")) //$NON-NLS-1$
				msg = Messages.getString(MessageIds.OSDE_MSGW0015) + e.getMessage().substring(1);
			else
				msg = Messages.getString(MessageIds.OSDE_MSGE0004) + e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage();
			throw new Exception(msg);
		}
		finally {
			if (device.isTableTabRequested())	application.setProgress(10, sThreadId);
			else application.setProgress(100, sThreadId);
			
			application.setStatusMessage(OSDE.STRING_EMPTY);
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
			application.setStatusMessage(Messages.getString(MessageIds.OSDE_MSGT0138) + filePath);
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "ISO-8859-1")); //TODO check UTF-8 for Linux //$NON-NLS-1$
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
			application.setProgress(progressCycle, sThreadId);
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
				if (i % 50 == 0) application.setProgress(((++progressCycle*5000)/recordEntries), sThreadId);
				log.log(Level.FINE, "data line = " + sb.toString()); //$NON-NLS-1$
			}
			sb = null;
			log.log(Level.FINE, "CSV file = " + filePath + " erfolgreich geschieben"  //$NON-NLS-1$ //$NON-NLS-2$
					+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$

			writer.flush();
			writer.close();
			writer = null;
			recordSet.setSaved(true);
			application.setProgress(100, sThreadId);
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
			application.setStatusMessage(OSDE.STRING_EMPTY);
		}

	}

}
