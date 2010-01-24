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
package osde.device.wb;


import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Vector;
import osde.log.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.widgets.FileDialog;

import osde.OSDE;
import osde.config.Settings;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.exception.DataInconsitsentException;
import osde.io.CSVSerialDataReaderWriter;
import osde.io.DataParser;
import osde.messages.Messages;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;

/**
 * Sample device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class CSV2SerialAdapter extends DeviceConfiguration implements IDevice {
	final static Logger						log	= Logger.getLogger(CSV2SerialAdapter.class.getName());

	public final static String		DEFAULT_RECORD_SET_EXTEND									= "Flugaufzeichnung"; //$NON-NLS-1$

	final OpenSerialDataExplorer	application;
	final CSV2SerialAdapterDialog	dialog;

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public CSV2SerialAdapter(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("osde.device.wb.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = OpenSerialDataExplorer.getInstance();
		this.dialog = new CSV2SerialAdapterDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_IMPORT_CLOSE);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public CSV2SerialAdapter(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("osde.device.wb.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = OpenSerialDataExplorer.getInstance();
		this.dialog = new CSV2SerialAdapterDialog(this.application.getShell(), this);
		this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_IMPORT_CLOSE);
	}
	
	/**
	 * query the default stem used as record set name
	 * @return recordSetStemName
	 */
	public String getRecordSetStemName() {
		return Messages.getString(osde.messages.MessageIds.OSDE_MSGT0272);
	}

	/**
	 * load the mapping exist between lov file configuration keys and OSDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// ...
		return lov2osdMap;
	}

	/**
	 * convert record LogView config data to OSDE config keys into records section
	 * @param header reference to header data, contain all key value pairs
	 * @param lov2osdMap reference to the map where the key mapping
	 * @param channelNumber 
	 * @return converted configuration data
	 */
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// ...
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	public int getLovDataByteSize() {
		return 0;  // sometimes first 4 bytes give the length of data + 4 bytes for number
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
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		// prepare the serial CSV data parser
		DataParser data = new  DataParser(this.getDataBlockTimeUnitFactor(), this.getDataBlockSeparator().value(), this.getDataBlockCheckSumType(), this.getDataBlockSize());
		int[] startLength = new int[] {0,0};
		byte[] lineBuffer = null;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
				
		try {
			for (int i = 0; i < recordDataSize; i++) {
				setDataLineStartAndLength(dataBuffer, startLength);
				lineBuffer = new byte[startLength[1]];
				System.arraycopy(dataBuffer, startLength[0], lineBuffer, 0, startLength[1]);
				//0=Empfänger-Spannung 1=Höhe 2=Motor-Strom 3=Motor-Spannung 4=Motorakku-Kapazität 5=Geschwindigkeit 6=Temperatur 7=GPS-Länge 8=GPS-Breite 9=GPS-Höhe 10=Steigen 11=ServoImpuls
				data.parse(new String(lineBuffer));

				recordSet.addNoneCalculationRecordsPoints(data.getValues(), data.getTime_ms());

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}
			this.updateVisibilityStatus(recordSet);
			if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		}
		catch (Exception e) {
			String msg = e.getMessage() + Messages.getString(osde.messages.MessageIds.OSDE_MSGW0543);
			log.log(Level.WARNING, msg, e);
			application.openMessageDialog(msg);
			if (doUpdateProgressBar) this.application.setProgress(0, sThreadId);
		}
	}

	/**
	 * set data line end points - this method will be called within getConvertedLovDataBytes only and requires to set startPos and crlfPos to zero before first call
	 * - data line start is defined with '$ ;'
	 * - end position is defined with '0d0a' (CRLF)
	 * @param dataBuffer
	 * @param startPos
	 * @param crlfPos
	 */
	private void setDataLineStartAndLength(byte[] dataBuffer, int[] refStartLength) {
		int startPos = refStartLength[0] + refStartLength[1];

		for (; startPos < dataBuffer.length; ++startPos) {
			if (dataBuffer[startPos] == 0x24) {
				if (dataBuffer[startPos+2] == 0x31 || dataBuffer[startPos+3] == 0x31) break; // "$ ;" or "$  ;" (record set number two digits
			}
		}
		int crlfPos = refStartLength[0] = startPos;
		
		for (; crlfPos < dataBuffer.length; ++crlfPos) {
			if (dataBuffer[crlfPos] == 0x0D)
				if(dataBuffer[crlfPos+1] == 0X0A) break; //0d0a (CRLF)
		}
		refStartLength[1] = crlfPos - startPos;
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
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
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = OSDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.getRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		Vector<Integer> timeStamps = new Vector<Integer>(1,1);
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		int timeStampBufferSize = OSDE.SIZE_BYTES_INTEGER * recordDataSize;
		byte[] timeStampBuffer = new byte[timeStampBufferSize];
		if(!recordSet.isTimeStepConstant()) {
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8) + ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
			}
		}
		log.log(Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString()); //$NON-NLS-1$
		
		for (int i = 0; i < recordDataSize; i++) {
			log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i*dataBufferSize+timeStampBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i*dataBufferSize+timeStampBufferSize, convertBuffer, 0, dataBufferSize);
			
			//0=Empfänger-Spannung 1=Höhe 2=Motor-Strom 3=Motor-Spannung 4=Motorakku-Kapazität 5=Geschwindigkeit 6=Temperatur 7=GPS-Länge 8=GPS-Breite 9=GPS-Höhe 10=Steigen 11=ServoImpuls
			for (int j = 0; j < points.length; j++) {
				points[j] = (((convertBuffer[0 + (j * 4)] & 0xff) << 24) + ((convertBuffer[1 + (j * 4)] & 0xff) << 16) + ((convertBuffer[2 + (j * 4)] & 0xff) << 8) + ((convertBuffer[3 + (j * 4)] & 0xff) << 0));
			}
			
			if(recordSet.isTimeStepConstant()) 
				recordSet.addPoints(points);
			else
				recordSet.addPoints(points, timeStamps.get(i)/10.0);

			
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*5000)/recordDataSize), sThreadId);
		}
		this.updateVisibilityStatus(recordSet);
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * function to prepare complete data table of record set while translating avalable measurement values
	 * @return pointer to filled data table with formated "%.3f" values
	 */
	public int[][] prepareDataTable(RecordSet recordSet, int[][] dataTable) {
		try {
			String[] recordNames = recordSet.getRecordNames();	// 0=Spannung, 1=Strom, 2=Ladung, 3=Leistung, 4=Energie
			int numberRecords = recordNames.length;
			int recordEntries = recordSet.getRecordDataSize(true);

			for (int j = 0; j < numberRecords; j++) {
				Record record = recordSet.get(recordNames[j]);
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				for (int i = 0; i < recordEntries; i++) {
					dataTable[i][j+1] = Double.valueOf((offset + ((record.get(i)/1000.0) - reduction) * factor) * 1000.0).intValue();				
				}
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return dataTable;
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double translateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required

		double newValue = (value - reduction) * factor + offset;
		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double reverseTranslateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required

		double newValue = (value - offset) / factor + reduction;
		log.log(Level.FINER, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	public void updateVisibilityStatus(RecordSet recordSet) {
		int channelConfigNumber = recordSet.getChannelConfigNumber();
		int displayableCounter = 0;
		Record record;
		MeasurementType measurement;
		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
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

			if (record.isActive() && record.isDisplayable()) {
				log.log(Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
				++displayableCounter;
			}
		}
		log.log(Level.TIME, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		//add implementation where data point are calculated
		//do not forget to make record displayable -> record.setDisplayable(true);
		
		//for the moment there are no calculations necessary
		//String[] recordNames = recordSet.getRecordNames();
		//for (int i=0; i<recordNames.length; ++i) {
		//	MeasurementType measurement = this.getMeasurement(recordSet.getChannelConfigNumber(), i);
		//	if (measurement.isCalculation()) {
		//		log.log(Level.FINE, "do calculation for " + recordNames[i]); //$NON-NLS-1$
		//	}
		//}
	}

	/**
	 * @return the dialog
	 */
	public CSV2SerialAdapterDialog getDialog() {
		return this.dialog;
	}

	/**
	 * @return the serialPort
	 */
	public DeviceSerialPort getSerialPort() {
		return null;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[] {IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION};
	}
	
	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	public void openCloseSerialPort() {
		FileDialog fd = this.application.openFileOpenDialog(Messages.getString(MessageIds.OSDE_MSGT1800), new String[] {this.getDeviceConfiguration().getDataBlockPreferredFileExtention(), OSDE.FILE_ENDING_STAR_STAR}, this.getDeviceConfiguration().getDataBlockPreferredDataLocation());
		String selectedImportFile = fd.getFilterPath() + OSDE.FILE_SEPARATOR_UNIX + fd.getFileName();
		log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$
		
		if (fd.getFileName().length() > 4) {
			try {
				Integer channelConfigNumber = dialog != null && !dialog.isDisposed() ? dialog.getTabFolderSelectionIndex() + 1 : null;
				CSVSerialDataReaderWriter.read(selectedImportFile, this, CSV2SerialAdapter.DEFAULT_RECORD_SET_EXTEND, channelConfigNumber, true);
			}
			catch (Throwable e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}
	
	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	public int[] getCellVoltageOrdinals() {
		// 0=total voltage, 1=ServoImpuls on, 2=ServoImpulse off, 3=temperature, 4=cell voltage, 5=cell voltage, 6=cell voltage, .... 
		return new int[] {0, 3};
	}
}
