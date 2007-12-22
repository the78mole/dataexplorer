/**
 * 
 */
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

import osde.config.Settings;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.ui.OpenSerialDataExplorer;


/**
 * Class to read and write comma separated value files
 */
public class CSVReaderWriter {
	private static Logger					log			= Logger.getLogger(CSVReaderWriter.class.getName());

	private static String					newLine	= System.getProperty("line.separator");
	private static DecimalFormat	df2			= new DecimalFormat("0.00");
	private static DecimalFormat	df3			= new DecimalFormat("0.000");
	private static StringBuffer		sb;
	private static String					line		= "*";

	/**
	 * @param args
	 */
//	public static void main(String[] args) {
//		final String csvFile = "c:\\Documents and Settings\\brueg\\My Documents\\LogView\\Htronic Akkumaster C4\\2007-05-23-FlugakkuA 2) Laden.csv";
//		// simulate record set of AkkuMaster C4
//		String[] recordNames = new String[] { "Spannung", "Strom", "Ladung", "Leistung", "Energie" };
//		String recordSetKey = "1) CSV import";
//
//		Channel newChannel = new Channel(1, new RecordSet(recordSetKey, recordNames, 10000));
//
//		// now add empty records for all of the given record names
//		for (String key : recordNames) {
//			newChannel.get(recordSetKey).put(key, new Record(key, "", "", 10000, 0, 0, 0, 50));
//			newChannel.applyTemplate(newChannel.get(recordSetKey));
//		}
//
//		read(';', recordSetKey, csvFile, newChannel, false);
//	}

	/**
	 * read the selected CSV file
	 * @throws Exception 
	 */
	public static RecordSet read(char separator, String filePath, RecordSet recordSet, boolean isRaw) throws Exception {
		BufferedReader reader; // to read the data
		String[] recordNames = null;
		int sizeRecords = 0;
		boolean isData = false;

		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "ISO-8859-1"));

			StringBuffer sb = new StringBuffer();
			int timeStep_ms = 0, old_time_ms = 0, new_time_ms = 0;
			char decimalSeparator = Settings.getInstance().getDecimalSeparator();

			while ((line = reader.readLine()) != null) {
				if (!isData) {
					// first line -> Zeit [s];Spannung [V];Strom [A];Ladung [Ah];Leistung [W];Energie [Wh]
					String[] header = line.split(";");
					sizeRecords = header.length - 1;
					recordNames = recordSet.getRecordNames();
					int countActive = 0;
					for (String recordKey : recordNames) {
						if (recordSet.get(recordKey).isActive())
							++countActive;			// update count for raw
						if (!isRaw)
							recordSet.get(recordKey).setDisplayable(true);	// all data available 
					}
					if (sizeRecords != countActive && isRaw || sizeRecords != recordNames.length && !isRaw) { // first simple check, but name must not match
						throw new Exception("1"); // mismatch data signature
					}
					else {
						int match = 0;
						for (int i = 1; i < header.length; i++) {
							if (header[i].trim().split(" ")[0].equals(recordNames[i - 1])) ++match;
						}
						if (match != header.length - 1) {
							throw new Exception("2"); // mismatch data header
						}
					}
					recordNames = new String[sizeRecords];
					for (int i = 0; i < sizeRecords; i++) {
						recordNames[i] = header[i + 1].trim().split(" ")[0];
						sb.append(recordNames[i] + ", ");
					}
					log.fine(sb.toString());
					isData = true;
				}
				else { // isData
					// 0; 14,780;  0,598;  1,000;  8,838;  0,002
					String[] dataStr = line.split("" + separator);
					String data = dataStr[0].trim().replace(decimalSeparator, '.');
					new_time_ms = (int)(new Double(data).doubleValue() * 1000);
					timeStep_ms = new_time_ms - old_time_ms;
					old_time_ms = new_time_ms;
					sb = new StringBuffer();
					for (int i = 0; i < sizeRecords; i++) {
						data = dataStr[i + 1].trim().replace(',', '.');
						double dPoint = new Double(data).doubleValue() * 1000; // multiply by 1000 reduces rounding errors for small values
						int point = (int) dPoint;
						sb.append(point + ", ");
						recordSet.getRecord(recordNames[i]).add(point);
					}
					log.fine(sb.toString());
				}
			}
			// set time base in msec
			recordSet.setTimeStep_ms(timeStep_ms);
			log.fine("timeStep_ms = " + timeStep_ms);

			reader.close();
		}
		catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			OpenSerialDataExplorer.getInstance().openMessageDialog("Die CSV Datei entspricht nicht dem unterstützten Encoding - \"ISO-8859-1\"");
			throw e;
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			OpenSerialDataExplorer.getInstance().openMessageDialog("Die CSV Datei existiert nicht - \"filePath\"");
			throw e;
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			OpenSerialDataExplorer.getInstance().openMessageDialog("Die CSV Datei kann nicht gelesen werden - \"filePath\"");
			throw e;
		}
		catch (Exception e) {
			if (e.getMessage().startsWith("1"))
				OpenSerialDataExplorer.getInstance().openMessageDialog("Die geöffnete CSV Datei entspricht nicht der Messgrößensignatur des eingestellten Gerätes");
			else
				OpenSerialDataExplorer.getInstance().openMessageDialog("Die Kopfzeile der geöffnete CSV Datei (" + line + ")entspricht nicht der des eingestellten Gerätes");
			throw e;
		}
		return recordSet;
	}

	/**
	 * write the application settings file
	 */
	public static void write(char separator, String recordSetKey, String filePath, boolean isRaw) {
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "ISO-8859-1")); //TODO check UTF-8 for Linux
			char decimalSeparator = Settings.getInstance().getDecimalSeparator();

			df2.setGroupingUsed(false);
			df3.setGroupingUsed(false);
			sb = new StringBuffer();
			sb.append("Zeit [sec]").append(separator); // Spannung [V];Strom [A];Ladung [Ah];Leistung [W];Energie [Wh]";
			RecordSet recordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
			IDevice device = OpenSerialDataExplorer.getInstance().getActiveDevice();
			String[] recordNames = device.getMeasurementNames();
			for (int i = 0; i < recordNames.length; i++) {
				Record record = recordSet.getRecord(recordNames[i]);
				log.finest("append " + recordNames[i]);
				if (isRaw) {
					if (record.isActive()) {// only use active records for writing raw data
						sb.append(record.getName()).append(" [").append(record.getUnit()).append(']').append(separator);	
						log.finest("append " + recordNames[i]);
					}
				}
				else {
					sb.append(record.getName()).append(" [").append(record.getUnit()).append(']').append(separator);	
					log.finest("append " + recordNames[i]);
				}
			}
			sb.deleteCharAt(sb.length() - 1).append(newLine);
			log.finer("header line = " + sb.toString());
			writer.write(sb.toString());

			// write data
			int recordEntries = recordSet.getRecord(recordSet.getRecordNames()[0]).size();
			for (int i = 0; i < recordEntries; i++) {
				sb = new StringBuffer();
				// add time entry
				sb.append((df2.format(new Double(i * recordSet.getTimeStep_ms() / 1000.0))).replace(',', decimalSeparator)).append(separator).append(' ');
				// add data entries
				for (int j = 0; j < recordNames.length; j++) {
					Record record = recordSet.getRecord(recordNames[j]);
					if (isRaw) { // do not change any values
						if (record.isActive())
							sb.append(df3.format(new Double(record.get(i))/1000.0).replace(',', decimalSeparator)).append(separator);
					}
					else
						// translate according device and measurement unit
						sb.append(df3.format(device.translateValue(record.getName(), record.get(i)/1000.0)).replace(',', decimalSeparator)).append(separator);
				}
				sb.deleteCharAt(sb.length() - 1).append(newLine);
				log.fine("CSV file = " + filePath + " erfolgreich geschieben");
				writer.write(sb.toString());
			}

			writer.flush();
			writer.close();
			log.fine("data line = " + sb.toString());
		}
		catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			OpenSerialDataExplorer.getInstance().openMessageDialog("Die CSV Datei entspricht nicht dem unterstützten Encoding - \"ISO-8859-1\"");
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			OpenSerialDataExplorer.getInstance().openMessageDialog("Die CSV Datei existiert nicht - \"filePath\"");
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			OpenSerialDataExplorer.getInstance().openMessageDialog("Die CSV Datei kann nicht gelesen werden - \"filePath\"");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			OpenSerialDataExplorer.getInstance().openMessageDialog("Die kopfzeile der geöffnete CSV Datei (" + line + ")entspricht nicht der des eingestellten Gerätes");
		}

	}

}
