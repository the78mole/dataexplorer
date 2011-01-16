/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2010,2011 Winfried Bruegmann
****************************************************************************************/
package gde.device.nmea;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.exception.DataInconsitsentException;
import gde.io.FileHandler;
import gde.io.LogViewReader;
import gde.io.NMEAParser;
import gde.io.NMEAReaderWriter;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.GPSHelper;
import gde.utils.LinearRegression;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Sample device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class NMEAAdapter extends DeviceConfiguration implements IDevice {
	final static Logger		log								= Logger.getLogger(NMEAAdapter.class.getName());

	final static String		SM_GPS_LOGGER_INI	= "SM GPS-Logger.ini";													//$NON-NLS-1$

	final DataExplorer		application;
	final Channels				channels;
	final NMEAAdapterDialog	dialog;

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public NMEAAdapter(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.nmea.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.dialog = new NMEAAdapterDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE);
			updateFileMenu(this.application.getMenuBar().getExportMenu());
		}
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public NMEAAdapter(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.nmea.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.dialog = new NMEAAdapterDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE);
			updateFileMenu(this.application.getMenuBar().getExportMenu());
		}
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	@Override
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// ...
		return lov2osdMap;
	}

	/**
	 * convert record LogView config data to GDE config keys into records section
	 * @param header reference to header data, contain all key value pairs
	 * @param lov2osdMap reference to the map where the key mapping
	 * @param channelNumber 
	 * @return converted configuration data
	 */
	@Override
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// ...
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	@Override
	public int getLovDataByteSize() {
		return 0; // sometimes first 4 bytes give the length of data + 4 bytes for number
	}

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
	 * this method is more usable for real logger, where data can be stored and converted in one block
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	@Override
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		// prepare the serial CSV data parser
		NMEAParser data = new NMEAParser(this.getDataBlockLeader(), this.getDataBlockSeparator().value(), this.getDataBlockCheckSumType(), this.getDataBlockSize(), this,
				this.channels.getActiveChannelNumber(), this.getUTCdelta());
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		byte[] lineBuffer;
		byte[] subLengthBytes;
		int subLenght;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		try {
			int lastLength = 0;
			for (int i = 0; i < recordDataSize; i++) {
				subLengthBytes = new byte[4];
				System.arraycopy(dataBuffer, lastLength, subLengthBytes, 0, 4);
				subLenght = LogViewReader.parse2Int(subLengthBytes) - 8;
				//System.out.println((subLenght+8));
				lineBuffer = new byte[subLenght];
				System.arraycopy(dataBuffer, 4 + lastLength, lineBuffer, 0, subLenght);
				String textInput = new String(lineBuffer,"ISO-8859-1");
				//System.out.println(textInput);
				StringTokenizer st = new StringTokenizer(textInput);
				Vector<String> vec = new Vector<String>();
				while (st.hasMoreTokens())
					vec.add(st.nextToken("\r\n"));
				//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity 8=magneticVariation;
				//GPS 		9=altitudeRel 10=climb 11=tripLength 12=distance 13=azimuth 14=directionStart
				data.parse(vec, vec.size());
				lastLength += (subLenght+12);

				recordSet.addNoneCalculationRecordsPoints(data.getValues(), data.getTime_ms());

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}
			this.updateVisibilityStatus(recordSet, true);
			if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		}
		catch (Exception e) {
			String msg = e.getMessage() + Messages.getString(gde.messages.MessageIds.GDE_MSGW0543);
			log.log(java.util.logging.Level.WARNING, msg, e);
			this.application.openMessageDialog(msg);
			if (doUpdateProgressBar) this.application.setProgress(0, sThreadId);
		}
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		//noop due to previous parsed CSV data
		return points;
	}

	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
	 * since this is a long term operation the progress bar should be updated to signal busyness to user 
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.getRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		Vector<Integer> timeStamps = new Vector<Integer>(1, 1);
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
		byte[] timeStampBuffer = new byte[timeStampBufferSize];
		if (!recordSet.isTimeStepConstant()) {
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8)
						+ ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
			}
		}
		log.log(java.util.logging.Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString()); //$NON-NLS-1$

		for (int i = 0; i < recordDataSize; i++) {
			log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize + timeStampBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i * dataBufferSize + timeStampBufferSize, convertBuffer, 0, dataBufferSize);

			//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity 8=magneticVariation;
			//GPS 		9=altitudeRel 10=climb 11=tripLength 12=distance 13=azimuth 14=directionStart
			for (int j = 0; j < 8; j++) {
				points[j] = (((convertBuffer[0 + (j * 4)] & 0xff) << 24) + ((convertBuffer[1 + (j * 4)] & 0xff) << 16) + ((convertBuffer[2 + (j * 4)] & 0xff) << 8) + ((convertBuffer[3 + (j * 4)] & 0xff) << 0));
			}

			if (recordSet.isTimeStepConstant())
				recordSet.addPoints(points);
			else
				recordSet.addPoints(points, timeStamps.get(i) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		this.updateVisibilityStatus(recordSet, true);
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, int rowIndex) {
		String[] dataTableRow = new String[recordSet.size() + 1]; // this.device.getMeasurementNames(this.channelNumber).length
		try {
			String[] recordNames = recordSet.getRecordNames(); // 0=Spannung, 1=Strom, 2=Ladung, 3=Leistung, 4=Energie
			int numberRecords = recordNames.length;

			dataTableRow[0] = String.format("%.1f", (recordSet.getTime_ms(rowIndex) / 1000.0)); //$NON-NLS-1$
			for (int j = 0; j < numberRecords; j++) {
				Record record = recordSet.get(recordNames[j]);
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				dataTableRow[j + 1] = record.getDecimalFormat().format((offset + ((record.get(rowIndex) / 1000.0) - reduction) * factor));
			}
		}
		catch (RuntimeException e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double translateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required

		//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity 8=magneticVariation;
		//GPS 		9=altitudeRel 10=climb 11=tripLength 12=distance 13=azimuth 14=directionStart
		double newValue = 0;
		if (record.getOrdinal() == 0 || record.getOrdinal() == 1) { // 0=GPS-latitude 1=GPS-longitude 
			int tValue = (int)(value * 1000);
			int grad = tValue / 1000000;
			int tMinuten =  tValue - grad*1000000;   //((int)value - grad * 1000) / 10.0 + (value -(int)value);
			newValue = grad + ((int)(tMinuten/0.6) / 1000000.0);
		}
		else {
			newValue = (value - reduction) * factor + offset;
		}
		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required

		//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity 8=magneticVariation;
		//GPS 		9=altitudeRel 10=climb 11=tripLength 12=distance 13=azimuth 14=directionStart
		double newValue = 0;
		if (record.getOrdinal() == 0 || record.getOrdinal() == 1) { // 0=GPS-latitude 1=GPS-longitude 
			int grad = (int) value;
			double minuten = (value - grad * 1.0) * 60.0;
			newValue = (grad + minuten / 100.0) * 1000.0;
		}
		else {
			newValue = (value - offset) / factor + reduction;
		}
		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * check and update visibility status of all records according the available device configuration
	 * this function must have only implementation code if the device implementation supports different configurations
	 * where some curves are hided for better overview 
	 * example: if device supports voltage, current and height and no sensors are connected to voltage and current
	 * it makes less sense to display voltage and current curves, if only height has measurement data
	 * at least an update of the graphics window should be included at the end of this method
	 */
	@Override
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int channelConfigNumber = recordSet.getChannelConfigNumber();
		int displayableCounter = 0;
		boolean configChanged = this.isChangePropery();
		Record record;
		MeasurementType measurement;
		//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity 8=magneticVariation;
		//GPS 		9=altitudeRel 10=climb 11=tripLength 12=distance 13=azimuth 14=directionStart
		String[] measurementNames = this.getMeasurementNames(channelConfigNumber);
		String[] recordNames = recordSet.getRecordNames();
		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordNames.length; ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(recordNames[i]);
			measurement = this.getMeasurement(channelConfigNumber, i);
			log.log(Level.FINE, recordNames[i] + " = " + measurementNames[i]); //$NON-NLS-1$

			// update active state and displayable state if configuration switched with other names
			if (record.isActive() != measurement.isActive()) {
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				log.log(Level.FINE, "switch " + record.getName() + " to " + measurement.isActive()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (includeReasonableDataCheck) {
				record.setDisplayable(record.hasReasonableData() && (measurement.isActive() || measurement.isCalculation()));
				log.log(Level.FINE, record.getName() + " hasReasonableData " + record.hasReasonableData()); //$NON-NLS-1$ 
			}

			if (record.isActive() && record.isDisplayable()) {
				log.log(Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
				++displayableCounter;
			}
		}
		log.log(Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
		this.setChangePropery(configChanged); //reset configuration change indicator to previous value, do not vote automatic configuration change at all
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity 8=magneticVariation;
		//GPS 		9=altitudeRel 10=climb 11=tripLength 12=distance 13=azimuth 14=directionStart
		Record recordLatitude = recordSet.get(0);
		Record recordLongitude = recordSet.get(1);
		Record recordAlitude = recordSet.get(2);
		int recordSize = recordLatitude.realSize();
		int startAltitude = recordAlitude.get(0); // using this as start point might be sense less if the GPS data has no 3D-fix

		Record recordAlitudeRelative = recordSet.get(9);
		try { //calculate 9=altitudeRel
			recordAlitudeRelative.clear();
			//check GPS latitude and longitude				
			int indexGPS = 0;
			int i = 0;
			for (; i < recordSize; ++i) {
				if (recordLatitude.get(i) != 0 && recordLongitude.get(i) != 0) {
					indexGPS = i;
					++i;
					break;
				}
				recordAlitudeRelative.add(0);
			}
			recordAlitudeRelative.add(0);
			startAltitude = recordAlitude.get(indexGPS); //set initial altitude to enable absolute altitude calculation 		
			for (; i < recordSize; ++i) {
				recordAlitudeRelative.add((recordAlitude.get(i) - startAltitude));
			}
		}
		catch (RuntimeException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			recordAlitudeRelative.clear();
			for (int i=0; i < recordSize; ++i) {
				recordAlitudeRelative.add(0);
			}
		}

		//calculate 11=tripLength 12=distance 13=azimuth 14=directionStart
		GPSHelper.calculateValues(this, recordSet, 0, 1, 2, startAltitude, 11, 12, 13, 14);

		
		Record recordClimb = recordSet.get(10);
		try {//calculate 10=climb

			if (recordAlitude.hasReasonableData()) {
				recordClimb.setDisplayable(false);
				recordClimb.clear();
				PropertyType property = recordClimb.getProperty(MeasurementPropertyTypes.REGRESSION_INTERVAL_SEC.value());
				int regressionInterval = (int) (property != null ? Double.parseDouble(property.getValue().trim()) : 3);
				//this.calculationThread = new QuasiLinearRegression(recordSet, recordAlitude.getName(), recordClimb.getName(), regressionInterval);
				new LinearRegression(recordSet, recordAlitude.getName(), recordClimb.getName(), regressionInterval).start();
			}			
		}
		catch (RuntimeException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			recordClimb.clear();
			for (int i=0; i < recordSize; ++i) {
				recordClimb.add(0);
			}
		}

		this.application.updateStatisticsData(true);	
		this.updateVisibilityStatus(recordSet, true);
	}
	
	/**
	 * @return the dialog
	 */
	@Override
	public NMEAAdapterDialog getDialog() {
		return this.dialog;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION };
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	@Override
	public void open_closeCommPort() {
		String searchDirectory = Settings.getInstance().getDataFilePath();
		if (FileUtils.checkDirectoryExist(this.getDeviceConfiguration().getDataBlockPreferredDataLocation())) {
			searchDirectory = this.getDeviceConfiguration().getDataBlockPreferredDataLocation();
		}
		final FileDialog fd = this.application.openFileOpenDialog(Messages.getString(MessageIds.GDE_MSGT2100), new String[] { this.getDeviceConfiguration().getDataBlockPreferredFileExtention(),
				GDE.FILE_ENDING_STAR_STAR }, searchDirectory, null, SWT.MULTI);
		Thread reader = new Thread("reader"){
			public void run() {
				for (String tmpFileName : fd.getFileNames()) {
					String selectedImportFile = fd.getFilterPath() + GDE.FILE_SEPARATOR_UNIX + tmpFileName;
					if (!selectedImportFile.endsWith(GDE.FILE_ENDING_DOT_NMEA)) {
						if (selectedImportFile.contains(GDE.STRING_DOT)) {
							selectedImportFile = selectedImportFile.substring(0, selectedImportFile.indexOf(GDE.STRING_DOT));
						}
						selectedImportFile = selectedImportFile + GDE.FILE_ENDING_DOT_NMEA;
					}
					log.log(java.util.logging.Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

					if (fd.getFileName().length() > 4) {
						Integer channelConfigNumber = NMEAAdapter.this.application.getActiveChannelNumber();
						channelConfigNumber = channelConfigNumber == null ? 1 : channelConfigNumber;
						//String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.STRING_DOT) - 4, selectedImportFile.lastIndexOf(GDE.STRING_DOT));
						try {
							NMEAReaderWriter.read(selectedImportFile, NMEAAdapter.this, GDE.STRING_EMPTY, channelConfigNumber);
						}
						catch (Exception e) {
							log.log(Level.WARNING, e.getMessage(), e);
						}
					}
				}
			}
		};
		reader.start();
	}

	/**
	 * update the file menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	public void updateFileMenu(Menu exportMenue) {
		MenuItem convertKLM3DRelativeItem;
		MenuItem convertKLM3DAbsoluteItem;
		MenuItem convert2GPXRelativeItem;
		MenuItem convert2GPXAbsoluteItem;

		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertKLM3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKLM3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT2105));
			convertKLM3DRelativeItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKLM3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KML3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKLM3DAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKLM3DAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2106));
			convertKLM3DAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKLM3DAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KML3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convert2GPXRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convert2GPXRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT2107));
			convert2GPXRelativeItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convert2GPXRelativeItem action performed! " + e); //$NON-NLS-1$
					export2GPX(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convert2GPXAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convert2GPXAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2108));
			convert2GPXAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convert2GPXAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2GPX(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});
		}
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE
	 */
	public void export2KML3D(int type) {
		//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity 8=magneticVariation;
		//GPS 		9=altitudeRel 10=climb 11=tripLength 12=distance 13=azimuth 14=directionStart
		new FileHandler().exportFileKML(Messages.getString(MessageIds.GDE_MSGT2103), 1, 0, 2, 7, type == DeviceConfiguration.HEIGHT_RELATIVE);
	}

	/**
	 * exports the actual displayed data set to GPX file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE
	 */
	public void export2GPX(int type) {
		//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity 8=magneticVariation;
		//GPS 		9=altitudeRel 10=climb 11=tripLength 12=distance 13=azimuth 14=directionStart
		new FileHandler().exportFileGPX(Messages.getString(MessageIds.GDE_MSGT2104), 1, 0, 2, 7, 9, type == DeviceConfiguration.HEIGHT_RELATIVE);
	}

	/**
	 * query if the actual record set of this device contains GPS data to enable KML export to enable google earth visualization 
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public boolean isActualRecordSetWithGpsData() {
		boolean containsGPSdata = false;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity 8=magneticVariation;
				//GPS 		9=altitudeRel 10=climb 11=tripLength 12=distance 13=azimuth 14=directionStart
				containsGPSdata = activeRecordSet.get(0).hasReasonableData() && activeRecordSet.get(1).hasReasonableData();
			}
		}
		return containsGPSdata;
	}

	/**
	 * export a file of the actual channel/record set
	 * @return full qualified file path depending of the file ending type
	 */
	@Override
	public String exportFile(String fileEndingType) {
		String exportFileName = GDE.STRING_EMPTY;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && fileEndingType.contains(GDE.FILE_ENDING_KML)) {
				exportFileName = new FileHandler().exportFileKML(1, 0, 2, 7, true);
			}
		}
		return exportFileName;
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMLMeasurementOrdinal() {
		//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity 8=magneticVariation;
		//GPS 		9=altitudeRel 10=climb 11=tripLength 12=distance 13=azimuth 14=directionStart
		return 7;
	}
}
