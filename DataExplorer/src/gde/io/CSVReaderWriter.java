/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;

import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.ui.OpenSerialDataExplorer;

/**
 * Class to read and write comma separated value files
 * @author Winfried Brügmann
 */
public class CSVReaderWriter {
	private static Logger					log			= Logger.getLogger(CSVReaderWriter.class.getName());

	private static String					lineSep	= System.getProperty("line.separator");
	private static DecimalFormat	df3			= new DecimalFormat("0.000");
	private static StringBuffer		sb;
	private static String					line		= "*";

	/**
	 * read the device name from selected CSV file
	 * @throws Exception 
	 */
	public static String read(char separator, String filePath) throws Exception {
		BufferedReader reader; // to read the data

		reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1"));

		line = reader.readLine();
		String fileDeviceName = line.split("" + separator)[0].trim();
		log.fine("file device name = " + fileDeviceName);

		reader.close();
		return fileDeviceName;
	}

	/**
	 * read the selected CSV file
	 * @throws Exception 
	 */
	public static RecordSet read(char separator, String filePath, String recordSetNameExtend, boolean isRaw) throws Exception {
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
				application.setStatusMessage("Lese CVS Datei " + filePath);
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1"));
				int timeStep_ms = 0, old_time_ms = 0, new_time_ms = 0;
				StringBuilder headerStringConf = new StringBuilder().append(lineSep);
				StringBuilder keys = new StringBuilder();
				String[] recordKeys = null;
				String fileConfig = null;
				
				// check for device name and channel or configuration in first line
				while (isDeviceName && (line = reader.readLine()) != null) {
					String activeDeviceName = application.getActiveDevice().getName();
					String fileDeviceName = line.split("" + separator)[0].trim();
					log.fine("active device name = " + activeDeviceName + ", file device name = " + fileDeviceName);

					if (activeDeviceName.equals(fileDeviceName)) {
						isDeviceName = false;
					}
					else {
						throw new Exception("0" + lineSep + "erste Zeile der Datei => " + line); // mismatch device name
					}

					String activeConfig = channels.getActiveChannel().getConfigKey();
					fileConfig = line.split("" + separator).length > 1 ? line.split("" + separator)[1].trim() : null;
					log.fine("active channel name = " + activeConfig + ", file channel name = " + fileConfig);
					if (fileConfig == null) {
						fileConfig = activeConfig;
						log.fine("using as file channel name = " + fileConfig);
					}
					else if (!activeConfig.equals(fileConfig)) {
						//check if config exist
						int channelNumber = channels.getChannelNumber(fileConfig);
						if (channelNumber != 0) { // 0 channel configuration does not exist
							String msg = "Die Kanalkonfiguration der Datei entspricht nicht der Kanalkonfiguration der Anwendung, soll auf die Dateikonfiguration umgeschaltet werden ?";
							int answer = application.openYesNoCancelMessageDialog(msg);
							if (answer == SWT.YES) {
								log.fine("SWT.YES");
								channels.setActiveChannelNumber(channelNumber);
								channels.switchChannel(channelNumber, "");
								application.getMenuToolBar().updateChannelSelector();
								activeChannel = channels.getActiveChannel();
							}
							else if (answer == SWT.NO) {
								log.fine("SWT.NO");
								fileConfig = channels.getActiveChannel().getConfigKey();
							}
							else {
								log.fine("SWT.CANCEL");
								return null;
							}
						}
						else {
							String msg = "Die Kanalkonfiguration der Datei entspricht keiner aktuell vorhandenen, \n"
									+ "soll auf die Datei zur aktuell eingestellten Kanalkonfiguration geladen werde ?\n" 
									+ "Hinweis: Umstellen der KanalKonfiguration ist über den Gerätedialog möglich.";
							int answer = application.openOkCancelMessageDialog(msg);
							if (answer == SWT.OK) {
								log.fine("SWT.OK");
								fileConfig = channels.getActiveChannel().getConfigKey();
							}
							else {
								log.fine("SWT.CANCEL");
								return null;
							}
						}
					}
				} // end isDeviceName
				log.fine("device name check ok, channel/configuration ok");
					
				recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend;
				// shorten the record set name to the allowed maximum
				recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, 30);

				recordSet = RecordSet.createRecordSet(fileConfig, recordSetName, application.getActiveDevice(), isRaw, true);

				String[] recordNames = recordSet.getRecordNames();

				//
				while (!isData && (line = reader.readLine()) != null) {
					// second line -> Zeit [s];Spannung [V];Strom [A];Ladung [Ah];Leistung [W];Energie [Wh]
					String[] header = line.split(";");
					sizeRecords = header.length - 1;
					int countNotMeasurement = 0;
					for (String recordKey : recordNames) {
						MeasurementType measurement = device.getMeasurement(fileConfig, recordKey);
						headerStringConf.append(measurement.getName()).append(separator);

						log.fine(measurement.getName() + " isCalculation = " + measurement.isCalculation());
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
						throw new Exception("1" + headerStringConf.toString() + lineSep + keys.toString()); // mismatch data signature length
					}

					int match = 0; // check match of the measurement units, relevant for absolute import 

					if (isRaw)
						recordNames = recordKeys = keys.toString().split("" + separator);
					else
						recordKeys = recordNames;

					// check units for absolute (!raw) data only
					// absolute data will not have any calculation
					// unit for raw data might not meaningful which require some calculation to get a unit
					if (!isRaw) {
						StringBuilder unitCompare = new StringBuilder().append(lineSep);
						for (int i = 1; i < header.length; i++) {
							String recordKey = recordKeys[i - 1];
							String expectUnit = device.getMeasurementUnit(fileConfig, recordKey);
							String[] inMeasurement = header[i].trim().replace('[', ';').replace(']', ';').split(";");
							String inUnit = inMeasurement.length == 2 ? inMeasurement[1] : Settings.EMPTY;
							unitCompare.append(recordKey + " inUnit = " + inUnit + " - expectUnit = " + expectUnit).append(lineSep);
							if (inUnit.equals(expectUnit) || inUnit.equals("---")) ++match;
						}
						log.fine(unitCompare.toString());
						if (match != header.length - 1) {
							throw new Exception("2" + unitCompare.toString()); // mismatch data header units
						}
					}
					isData = true;
				} // while !isData

				// get all data   0; 14,780;  0,598;  1,000;  8,838;  0,002
				while ((line = reader.readLine()) != null && isData) {
					String[] dataStr = line.split("" + separator);
					String data = dataStr[0].trim().replace(',', '.');
					new_time_ms = (int) (new Double(data).doubleValue() * 1000);
					timeStep_ms = new_time_ms - old_time_ms;
					old_time_ms = new_time_ms;
					if (log.isLoggable(Level.FINE)) sb = new StringBuffer().append(lineSep);
					// use only measurement which are isCalculation == false
					for (int i = 0; i < sizeRecords; i++) {
						data = dataStr[i + 1].trim().replace(',', '.');
						double tmpDoubleValue = new Double(data).doubleValue();
						double dPoint = tmpDoubleValue > 500000 ? tmpDoubleValue : tmpDoubleValue * 1000; // multiply by 1000 reduces rounding errors for small values
						int point = (int) dPoint;
						if (log.isLoggable(Level.FINE)) {
							sb.append("recordKeys[" + i + "] = ").append(recordNames[i]).append(" = ").append(point).append(lineSep);
						}
						recordSet.getRecord(recordNames[i]).add(point);
						if (log.isLoggable(Level.FINE)) log.fine("add point data to recordKeys[" + i + "] = " + recordNames[i]);
					}
					if (log.isLoggable(Level.FINE)) log.fine(sb.toString());
				}

				// set time base in msec
				recordSet.setTimeStep_ms(timeStep_ms);
				log.fine("timeStep_ms = " + timeStep_ms);
				recordSet.setSaved(true);
				
				activeChannel.put(recordSetName, recordSet);
				activeChannel.setActiveRecordSet(recordSetName);
				activeChannel.applyTemplate(recordSetName);
				activeChannel.switchRecordSet(recordSetName);
//				activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records

				reader.close();
			}
		}
		catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception("Die CSV Datei entspricht nicht dem unterstützten Encoding - \"ISO-8859-1\"");
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception("Die CSV Datei existiert nicht - \"" + filePath + "\"");
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception("Die CSV Datei kann nicht gelesen werden - \"" + filePath + "\"");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			String msg = null;
			if (e.getMessage().startsWith("0"))
				msg = "Die geöffnete CSV Datei entspricht nicht dem eingestellten Gerät" + e.getMessage().substring(1);
			else if (e.getMessage().startsWith("1"))
				msg = "Die geöffnete CSV Datei entspricht nicht der Messgrößensignatur des eingestellten Gerätes (vermutlich raw/absolute) :" + e.getMessage().substring(1);
			else if (e.getMessage().startsWith("2"))
				msg = "Bei der geöffneten CVS Datei stimmen die Einheiten nicht mit der Konfiguration überein : " + e.getMessage().substring(1);
			else
				msg = "Beim Einlesen der CSV Datei ist folgender Fehler aufgetreten : " + e.getClass().getSimpleName() + " - " + e.getMessage();
			throw new Exception(msg);
		}
		finally {
			if (device.isTableTabRequested())	application.setProgress(10);
			else application.setProgress(100);
			
			application.setStatusMessage("");
		}
		
		return recordSet;
	}

	/**
	 * write data CVS file
	 * @throws Exception 
	 */
	public static void write(char separator, String recordSetKey, String filePath, boolean isRaw) throws Exception {
		BufferedWriter writer;
		OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
		try {
			application.setStatusMessage("Schreibe CVS Datei " + filePath);
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "ISO-8859-1")); //TODO check UTF-8 for Linux
			char decimalSeparator = Settings.getInstance().getDecimalSeparator();

			df3.setGroupingUsed(false);
			sb = new StringBuffer();
			RecordSet recordSet = Channels.getInstance().getActiveChannel().get(recordSetKey);
			IDevice device = OpenSerialDataExplorer.getInstance().getActiveDevice();
			// write device name , manufacturer, and serial port string
			sb.append(device.getName()).append(separator).append(recordSet.getChannelConfigName()).append(lineSep);
			writer.write(sb.toString());
			log.fine("written header line = " + sb.toString()); 
			
			sb = new StringBuffer();
			sb.append("Zeit [sec]").append(separator); // Spannung [V];Strom [A];Ladung [Ah];Leistung [W];Energie [Wh]";
			// write the measurements signature
			String[] recordNames = device.getMeasurementNames(recordSet.getChannelConfigName());
			for (int i = 0; i < recordNames.length; i++) {
				MeasurementType  measurement = device.getMeasurement(recordSet.getChannelConfigName(), recordNames[i]);
				log.finest("append " + recordNames[i]);
				if (isRaw) {
					if (!measurement.isCalculation()) {	// only use active records for writing raw data 
						sb.append(measurement.getName()).append(" [---]").append(separator);	
						log.finest("append " + recordNames[i]);
					}
				}
				else {
					sb.append(measurement.getName()).append(" [").append(measurement.getUnit()).append(']').append(separator);	
					log.finest("append " + recordNames[i]);
				}
			}
			sb.deleteCharAt(sb.length() - 1).append(lineSep);
			log.finer("header line = " + sb.toString());
			writer.write(sb.toString());

			// write data
			int recordEntries = recordSet.getRecordDataSize();
			double stausIncrement = recordEntries/100.0;
			for (int i = 0; i < recordEntries; i++) {
				sb = new StringBuffer();
				// add time entry
				sb.append((df3.format(new Double(i * recordSet.getTimeStep_ms() / 1000.0))).replace('.', decimalSeparator)).append(separator).append(' ');
				// add data entries
				for (int j = 0; j < recordNames.length; j++) {
					Record record = recordSet.getRecord(recordNames[j]);
					if (record == null)
						throw new Exception("Es wird kein passender Record zu dem Namen " + recordNames[j] + " gefunden. Vermutlich wurde die Konfiguration \"" + recordSet.getChannelConfigName() + "\" zwischenzeitlich verändert.");

					MeasurementType measurement = device.getMeasurement(recordSet.getChannelConfigName(), recordNames[j]);
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
				log.fine("CSV file = " + filePath + " erfolgreich geschieben");
				writer.write(sb.toString());
				application.setProgress(new Double(stausIncrement * i).intValue());
			}

			writer.flush();
			writer.close();
			recordSet.setSaved(true);
			log.fine("data line = " + sb.toString());
			application.setProgress(100);
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception("Die CSV Datei kann nicht geschrieben werden - \"" + filePath + "\"");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception("Ein Fehler ist aufgetreten : " + e.getClass().getSimpleName() + " - " + e.getMessage());
		}
		finally {
			application.setStatusMessage("");
		}

	}

}
