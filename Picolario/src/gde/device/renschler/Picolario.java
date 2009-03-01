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
package osde.device.renschler;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import osde.OSDE;
import osde.config.Settings;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.device.PropertyType;
import osde.exception.DataInconsitsentException;
import osde.messages.Messages;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.CalculationThread;
import osde.utils.LinearRegression;
import osde.utils.QuasiLinearRegression;

/**
 * Picolariolog device main implementaion class
 * @author Winfried Brügmann
 */
public class Picolario extends DeviceConfiguration implements IDevice {
	final static String						$CLASS_NAME				=	Picolario.class.getName();
	final static Logger						log								= Logger.getLogger($CLASS_NAME);

	public final static String		DO_NO_ADAPTION		= "do_no_adaption"; //$NON-NLS-1$
	public final static String		DO_OFFSET_HEIGHT	= "do_offset_height"; //$NON-NLS-1$
	public final static String		DO_SUBTRACT_FIRST	= "do_subtract_first"; //$NON-NLS-1$
	public final static String		DO_SUBTRACT_LAST	= "subtract_last"; //$NON-NLS-1$

	final OpenSerialDataExplorer	application;
	final PicolarioDialog					dialog;
	final PicolarioSerialPort			serialPort;
	final Channels								channels;

	/**
	 * @param iniFile
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public Picolario(String iniFile) throws FileNotFoundException, JAXBException {
		super(iniFile);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("osde.device.renschler.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new PicolarioSerialPort(this, this.application);
		this.dialog = new PicolarioDialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_OPEN_CLOSE);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public Picolario(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("osde.device.renschler.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new PicolarioSerialPort(this, this.application);
		this.dialog = new PicolarioDialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
		this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_OPEN_CLOSE);
	}
	
	/**
	 * query the default stem used as record set name
	 * @return recordSetStemName
	 */
	public String getRecordSetStemName() {
		return Messages.getString(MessageIds.OSDE_MSGT1220);
	}

	/**
	 * load the mapping exist between lov file configuration keys and OSDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		//nothing to do here
		return lov2osdMap;
	}

	/**
	 * convert record logview config data to OSDE config keys into records section
	 * @param header reference to header data, contain all key value pairs
	 * @param lov2osdMap reference to the map where the key mapping
	 * @param channelNumber 
	 * @return
	 */
	@SuppressWarnings("unused") //$NON-NLS-1$
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	public int getLovDataByteSize() {
		return 16;  // 0x0C = 12 + 4 (counter)
	}

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
	 * since this is a long term operation the progress bar should be updated to signal busyness to user 
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	public void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int offset = 4;
		int size = this.getLovDataByteSize();
		int deviceDataBufferSize = 3; // meight not equal as this.getNumberOfMeasurements(recordSet.getChannelConfigName());
		byte[] convertBuffer = new byte[deviceDataBufferSize];
		int[] points = new int[deviceDataBufferSize];
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
	
		for (int i = 0; i < recordDataSize; i++) { 
			System.arraycopy(dataBuffer, offset + i*size, convertBuffer, 0, deviceDataBufferSize);
			recordSet.addPoints(convertDataBytes(points, convertBuffer), false);
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*5000)/recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {		
		// add voltage U = 2.5 + (byte3 - 45) * 0.0532 - no calculation take place here - refer to translateValue/reverseTranslateValue
		points[0] = new Integer(dataBuffer[2]) * 1000;

		// calculate height values and add
		if (((dataBuffer[1] & 0x80) >> 7) == 0) // we have signed [feet]
			points[1] = ((dataBuffer[0] & 0xFF) + ((dataBuffer[1] & 0x7F) << 8)) * 1000; // only positive part of height data
		else
			points[1] = (((dataBuffer[0] & 0xFF) + ((dataBuffer[1] & 0x7F) << 8)) * -1) * 1000; // height is negative

		return points;
	}
	
	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwords to calualte the emissing data
	 * since this is a long term operation the progress bar should be updated to signal busyness to user 
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 */
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = OSDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.getRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		for (int i = 0; i < recordDataSize; i++) {
			System.arraycopy(dataBuffer, i*dataBufferSize, convertBuffer, 0, dataBufferSize);
			
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
			points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
			
			recordSet.addPoints(points, false);
			
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*5000)/recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * function to prepare complete data table of record set while translating avalable measurement values
	 * @return pointer to filled datatable
	 */
	public int[][] prepareDataTable(RecordSet recordSet, int[][] dataTable) {
	//public String[][] prepareDataTable(RecordSet recordSet, String[][] dataTable) {
		try {
			String[] recordNames = recordSet.getRecordNames();  // 0=Spannung, 1=Höhe, 2=Steigung
			int numberRecords = recordNames.length;
			int recordEntries = recordSet.getRecordDataSize(true);
			//DecimalFormat df = new DecimalFormat("0.000");
			

			for (int j = 0; j < numberRecords; j++) {
				Record record = recordSet.get(recordNames[j]);
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				
				switch (j) { // 0=Spannung, 1=Höhe, 2=Steigung
				case 0: //Spannung/Voltage
					break;
				case 1: //Höhe/Height
					PropertyType property = record.getProperty(Picolario.DO_SUBTRACT_FIRST);
					boolean subtractFirst = property != null ? new Boolean(property.getValue()).booleanValue() : false;
					property = record.getProperty(Picolario.DO_SUBTRACT_LAST);
					boolean subtractLast = property != null ? new Boolean(property.getValue()).booleanValue() : false;
					
					if (subtractFirst) {
						reduction = record.getFirst()/1000.0;
					}
					else if (subtractLast) {
						reduction = record.getLast()/1000.0;
					}
					else {
						reduction = 0;
					}
					break;
				case 2: //Steigung/Slope
					factor = recordSet.get(recordNames[1]).getFactor(); // 1=height
					break;
				default:
					log.log(Level.WARNING, "exceed known record names"); //$NON-NLS-1$
					break;
				}
				
				for (int i = 0; i < recordEntries; i++) {
					dataTable[i][j+1] = new Double((offset + ((record.get(i)/1000.0) - reduction) * factor) * 1000.0).intValue();				
					//dataTable[i][j+1] = df.format((offset + ((record.get(i)/1000.0) - reduction) * factor) * 1000.0);
				}
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return dataTable;
	}

	/**
	 * function to translate measured value from a device to values represented (((value - reduction) * factor) + offset - firstLastAdaption)
	 * @return double with the adapted value
	 */
	public double translateValue(Record record, double value) {
		final String $METHOD_NAME = "translateValue()";
		log.log(Level.FINEST, String.format("input value for %s - %f", record.getName(), value)); //$NON-NLS-1$

		String recordKey = "?"; //$NON-NLS-1$
		double newValue = 0.0;
		try {
			// 0=Spannung, 1=Höhe, 2=Steigung
			String[] recordNames = record.getRecordSetNames(); 
			
			recordKey = record.getName();
			double offset = record.getOffset(); // != 0 if curve has an defined offset
			double reduction = record.getReduction();
			double factor = record.getFactor(); // != 1 if a unit translation is required

			// height calculation need special procedure
			if (recordKey.startsWith(recordNames[1])) { // 1=Höhe
				PropertyType property = record.getProperty(Picolario.DO_SUBTRACT_FIRST);
				boolean subtractFirst = property != null ? new Boolean(property.getValue()).booleanValue() : false;
				property = record.getProperty(Picolario.DO_SUBTRACT_LAST);
				boolean subtractLast = property != null ? new Boolean(property.getValue()).booleanValue() : false;

				try {
					if (subtractFirst) {
						// get the record set to be used
						RecordSet recordSet = this.channels.getActiveChannel().getActiveRecordSet();
						if (recordKey.substring(recordKey.length() - 2).startsWith("_")) recordSet = this.application.getCompareSet(); //$NON-NLS-1$

						reduction = recordSet.getRecord(recordKey).getFirst().intValue() / 1000.0;
					}
					else if (subtractLast) {
						// get the record set to be used
						RecordSet recordSet = this.channels.getActiveChannel().getActiveRecordSet();
						if (recordKey.substring(recordKey.length() - 2).startsWith("_")) recordSet = this.application.getCompareSet(); //$NON-NLS-1$

						reduction = recordSet.getRecord(recordKey).getLast().intValue() / 1000.0;
					}
				}
				catch (Throwable e) {
					log.log(Level.SEVERE, record.getParent().getName() + " " + record.getName() + " " + e.getMessage() + " " + $CLASS_NAME + "." + $METHOD_NAME);
				}
			}

			// slope calculation needs height factor for calculation
			else if (recordKey.startsWith(recordNames[2])) { // 2=slope
				factor = this.getMeasurementFactor(record.getParent().getChannelConfigName(), 1); // 1=height
			}

			newValue = offset + (value - reduction) * factor;
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}

		log.log(Level.FINER, String.format("value calculated for %s - inValue %f - outValue %f", recordKey, value, newValue)); //$NON-NLS-1$
		return newValue;
	}

	/**
	 * function to translate measured value from a device to values represented (((value - offset + firstLastAdaption)/factor) + reduction)
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(Record record, double value) {
		log.log(Level.FINEST, String.format("input value for %s - %f", record.getName(), value)); //$NON-NLS-1$
		final String $METHOD_NAME = "reverseTranslateValue()";

		// 0=Spannung, 1=Höhe, 2=Steigung
		String[] recordNames = record.getRecordSetNames(); 

		String recordKey = record.getName();
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double reduction = record.getReduction();
		double factor = record.getFactor(); // != 1 if a unit translation is required

		// height calculation need special procedure
		if (recordKey.startsWith(recordNames[1])) { // 1=Höhe
			PropertyType property = record.getProperty(Picolario.DO_SUBTRACT_FIRST);
			boolean subtractFirst = property != null ? new Boolean(property.getValue()).booleanValue() : false;
			property = record.getProperty(Picolario.DO_SUBTRACT_LAST);
			boolean subtractLast = property != null ? new Boolean(property.getValue()).booleanValue() : false;

			try {
				if (subtractFirst) {
					// get the record set to be used
					RecordSet recordSet = this.channels.getActiveChannel().getActiveRecordSet();
					if (recordKey.substring(recordKey.length() - 2).startsWith("_")) recordSet = this.application.getCompareSet(); //$NON-NLS-1$

					reduction = recordSet.getRecord(recordKey).getFirst().intValue() / 1000;
				}
				else if (subtractLast) {
					// get the record set to be used
					RecordSet recordSet = this.channels.getActiveChannel().getActiveRecordSet();
					if (recordKey.substring(recordKey.length() - 2).startsWith("_")) recordSet = this.application.getCompareSet(); //$NON-NLS-1$

					reduction = recordSet.getRecord(recordKey).getLast().intValue() / 1000;
				}
			}
			catch (Throwable e) {
				log.log(Level.SEVERE, record.getParent().getName() + " " + record.getName() + " " + e.getMessage() + " " + $CLASS_NAME + "." + $METHOD_NAME);
			}
		}

		// slope calculation needs height factor for calculation
		else if (recordKey.startsWith(recordNames[2])) { // 2=slope
			factor = this.getMeasurementFactor(record.getParent().getChannelConfigName(), 1); // 1=height
		}

		double newValue = (value - offset) / factor + reduction;

		log.log(Level.FINER, String.format("new value calculated for %s - inValue %f - outValue %f", recordKey, value, newValue)); //$NON-NLS-1$
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
		log.log(Level.FINE, "no update required for " + recordSet.getName()); //$NON-NLS-1$
	}

	/**
	 * function to calculate values for inactive and to be calculated records
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are measurement point every 10 seconds during capturing only and the calculation will take place directly switch all to displayable
		if (recordSet.isRaw() && recordSet.isRecalculation()) {
			String[] measurements = recordSet.getRecordNames(); // 0=Spannung, 1=Höhe, 2=Steigrate
			// calculate the values required		
			Record slopeRecord = recordSet.get(measurements[2]);
			slopeRecord.setDisplayable(false);
			PropertyType property = slopeRecord.getProperty(CalculationThread.REGRESSION_INTERVAL_SEC);
			int regressionInterval = property != null ? new Integer(property.getValue()) : 10;
			property = slopeRecord.getProperty(CalculationThread.REGRESSION_TYPE);
			if (property == null || property.getValue().equals(CalculationThread.REGRESSION_TYPE_CURVE))
				this.calculationThread = new QuasiLinearRegression(recordSet, measurements[1], measurements[2], regressionInterval);
			else
				this.calculationThread = new LinearRegression(recordSet, measurements[1], measurements[2], regressionInterval);

			try {
				this.calculationThread.start();
			}
			catch (RuntimeException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[] {	IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION, 
				DO_NO_ADAPTION, DO_OFFSET_HEIGHT, DO_SUBTRACT_FIRST, DO_SUBTRACT_LAST,
				CalculationThread.REGRESSION_INTERVAL_SEC, CalculationThread.REGRESSION_TYPE};
	}

	/**
	 * @return the dialog
	 */
	public PicolarioDialog getDialog() {
		return this.dialog;
	}

	/**
	 * @return the serialPort
	 */
	public PicolarioSerialPort getSerialPort() {
		return this.serialPort;
	}
	
	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 */
	public void openCloseSerialPort() {
		if (this.serialPort != null) {
			if (!this.serialPort.isConnected()) {
				try {
					this.serialPort.open();
				}
				catch (Exception e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0025, new Object[] {e.getClass().getSimpleName(), e.getMessage() } ));
				}
			}
			else {
				this.serialPort.close();
			}
		}
	}
}
