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
package osde.device.smmodellbau;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import osde.OSDE;
import osde.config.Settings;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.device.PropertyType;
import osde.device.smmodellbau.lipowatch.MessageIds;
import osde.exception.DataInconsitsentException;
import osde.messages.Messages;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.CalculationThread;
import osde.utils.StringHelper;

/**
 * Sample device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class LiPoWatch extends DeviceConfiguration implements IDevice {
	final static Logger						log															= Logger.getLogger(LiPoWatch.class.getName());

	public static final String		LOV_A1_CHECKED									= "A1Checked=";																																																						//$NON-NLS-1$
	public static final String		LOV_A1_NAME											= "A1Name=";																																																								//$NON-NLS-1$
	public static final String		LOV_A1_OFFSET										= "A1Offset=";																																																							//$NON-NLS-1$
	public static final String		LOV_A1_FACTOR										= "A1Faktor=";																																																							//$NON-NLS-1$
	public static final String		LOV_A1_UNIT											= "A1Einheit=";																																																						//$NON-NLS-1$

	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_11	= new String[] { //11=a1Value
																																LiPoWatch.LOV_A1_CHECKED, LiPoWatch.LOV_A1_NAME, LiPoWatch.LOV_A1_UNIT, LiPoWatch.LOV_A1_OFFSET, LiPoWatch.LOV_A1_FACTOR };

	public final static String		A1_FACTOR												= "a1_" + IDevice.FACTOR;																																																	//$NON-NLS-1$
	public final static String		A1_OFFSET												= "a1_" + IDevice.OFFSET;																																																	//$NON-NLS-1$

	public final static String		FIRMEWARE_VERSION								= "Firmware";																																																							//$NON-NLS-1$
	public final static String		SERIAL_NUMBER										= "S/N";																																																										//$NON-NLS-1$

	final OpenSerialDataExplorer	application;
	final LiPoWatchSerialPort			serialPort;
	final LiPoWatchDialog					dialog;

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public LiPoWatch(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("osde.device.smmodellbau.lipowatch.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new LiPoWatchSerialPort(this, this.application);
		this.dialog = new LiPoWatchDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_OPEN_CLOSE);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public LiPoWatch(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("osde.device.smmodellbau.lipowatch.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new LiPoWatchSerialPort(this, this.application);
		this.dialog = new LiPoWatchDialog(this.application.getShell(), this);
		this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_OPEN_CLOSE);
	}

	/**
	 * query the default stem used as record set name
	 * @return recordSetStemName
	 */
	public String getRecordSetStemName() {
		return Messages.getString(MessageIds.OSDE_MSGT1601);
	}

	/**
	 * load the mapping exist between lov file configuration keys and OSDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {

		lov2osdMap.put(LiPoWatch.LOV_A1_CHECKED, Record.IS_ACTIVE + "=_" + "BOOLEAN"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LiPoWatch.LOV_A1_NAME, Record.NAME);
		lov2osdMap.put(LiPoWatch.LOV_A1_UNIT, Record.UNIT);
		lov2osdMap.put(LiPoWatch.LOV_A1_OFFSET, IDevice.OFFSET + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LiPoWatch.LOV_A1_FACTOR, IDevice.FACTOR + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$

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
		String recordSetInfo = OSDE.STRING_EMPTY;
		for (int j = 0; j < this.getNumberOfMeasurements(this.getChannelName(channelNumber)); j++) {
			StringBuilder recordConfigData = new StringBuilder();
			if (j == 18) {//11=a1Value LOV_CONFIG_DATA_KEYS_UNILOG_11
				HashMap<String, String> configData = StringHelper.splitString(header.get(OSDE.LOV_CONFIG_DATA), OSDE.DATA_DELIMITER, LiPoWatch.LOV_CONFIG_DATA_KEYS_UNILOG_11);
				for (String lovKey : LiPoWatch.LOV_CONFIG_DATA_KEYS_UNILOG_11) {
					if (configData.containsKey(lovKey)) {
						recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER); //$NON-NLS-1$
					}
				}
			}
			recordSetInfo = recordSetInfo + OSDE.RECORDS_PROPERTIES + recordConfigData.toString() + Record.END_MARKER;
		}

		return recordSetInfo;
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	public int getLovDataByteSize() {
		return 32;
	}

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int timeStep_ms = 0;
		int size = this.getLovDataByteSize();
		byte[] readBuffer = new byte[size];
		int[] points = new int[this.getNumberOfMeasurements(recordSet.getChannelConfigName())];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 2; i < recordDataSize; i++) { // skip UniLog min/max line

			System.arraycopy(dataBuffer, i * size, readBuffer, 0, size);

			// time milli seconds
			if (timeStep_ms == 0) { // set time step for this record set
				timeStep_ms = timeStep_ms + ((readBuffer[3] & 0xFF) << 24) + ((readBuffer[2] & 0xFF) << 16) + ((readBuffer[1] & 0xFF) << 8) + (readBuffer[0] & 0xFF); //TODO
				if (timeStep_ms != 0) {
					recordSet.setTimeStep_ms(timeStep_ms);
					LiPoWatch.log.log(Level.FINE, "timeStep_ms = " + timeStep_ms); //$NON-NLS-1$
				}
			}
			recordSet.addPoints(convertDataBytes(points, readBuffer));

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte array with the data to be converted
	 */
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		StringBuilder sb = new StringBuilder();
		int tmpValue = 0;
		int totalVotage = 0;
		// 0=total voltage, 1=ServoImpuls on, 2=ServoImpulse off, 3=temperature, 4=cell voltage, 5=cell voltage, 6=cell voltage, .... 

		//Servoimpuls_in
		points[1] = (((dataBuffer[6] & 0xFF) << 8) + (dataBuffer[5] & 0xFF) & 0xFFF0) / 16 * 1000;
		if (LiPoWatch.log.isLoggable(Level.FINE)) sb.append("(1)" + points[16]).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
		//Servoimpuls_out 
		points[2] = (((dataBuffer[8] & 0xFF) << 8) + (dataBuffer[7] & 0xFF) & 0xFFF0) / 16 * 1000;
		if (LiPoWatch.log.isLoggable(Level.FINE)) sb.append("(2)" + points[17]).append("; "); //$NON-NLS-1$ //$NON-NLS-2$

		//Temp_intern
		tmpValue = ((dataBuffer[14] & 0xFF) << 8) + (dataBuffer[13] & 0xFF);
		points[3] = tmpValue <= 32768 ? tmpValue * 10 : (tmpValue - 65536) * 10;
		if (LiPoWatch.log.isLoggable(Level.FINE)) sb.append("(3)" + points[18]).append("; "); //$NON-NLS-1$ //$NON-NLS-2$

		// number cells (first measurement might be wrong, so use avarage if possible)
		int numberCells = (dataBuffer[5] & 0x0F); 
		LiPoWatch.log.log(Level.FINE, "numberCells = " + numberCells); //$NON-NLS-1$
		// read cell voltage values
		int i;
		for (i = 0; i < numberCells; i++) {
			tmpValue = ((dataBuffer[2 * i + 16] & 0xFF) << 8) + (dataBuffer[2 * i + 15] & 0xFF);
			points[i + 4] = (tmpValue <= 32786 ? tmpValue * 2 : (tmpValue - 65536) * 2); //cell voltage
			if (LiPoWatch.log.isLoggable(Level.FINE)) sb.append("(" + (i + 4) + ")" + points[1]).append("; "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			totalVotage += points[i + 4];
		}
		//measurement modus absolute/relative
		boolean isRelative = ((dataBuffer[9] & 0xF0) >> 4) == 1;
		if (isRelative)
			points[0] = totalVotage;
		else
			points[0] = points[i + 3];
		//points[0] = isRelative ? totalVotage : points[i-3]; // total battery voltage
		if (LiPoWatch.log.isLoggable(Level.FINE)) sb.insert(0, "(0)" + points[0] + "; "); //$NON-NLS-1$ //$NON-NLS-2$	

		LiPoWatch.log.log(Level.FINE, sb.toString());
		return points;
	}

	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * since this is a long term operation the progress bar should be updated to signal busy to user 
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
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
			System.arraycopy(dataBuffer, i * dataBufferSize, convertBuffer, 0, dataBufferSize);

			// 0=total voltage, 1=ServoImpuls on, 2=ServoImpulse off, 3=temperature, 4=cell voltage, 5=cell voltage, 6=cell voltage, .... 
			points[0] = (((convertBuffer[0] & 0xff) << 24) + ((convertBuffer[1] & 0xff) << 16) + ((convertBuffer[2] & 0xff) << 8) + ((convertBuffer[3] & 0xff) << 0));
			points[1] = (((convertBuffer[4] & 0xff) << 24) + ((convertBuffer[5] & 0xff) << 16) + ((convertBuffer[6] & 0xff) << 8) + ((convertBuffer[7] & 0xff) << 0));
			points[2] = (((convertBuffer[8] & 0xff) << 24) + ((convertBuffer[9] & 0xff) << 16) + ((convertBuffer[10] & 0xff) << 8) + ((convertBuffer[11] & 0xff) << 0));
			points[3] = (((convertBuffer[12] & 0xff) << 24) + ((convertBuffer[13] & 0xff) << 16) + ((convertBuffer[14] & 0xff) << 8) + ((convertBuffer[15] & 0xff) << 0));

			for (int j = 0; j < points.length - 4; j++) {
				points[j + 4] = (((convertBuffer[16 + (j * 4)] & 0xff) << 24) + ((convertBuffer[17 + (j * 4)] & 0xff) << 16) + ((convertBuffer[18 + (j * 4)] & 0xff) << 8) + ((convertBuffer[19 + (j * 4)] & 0xff) << 0));
			}
			recordSet.addPoints(points);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * function to prepare complete data table of record set while translating avalable measurement values
	 * @return pointer to filled data table with formated "%.3f" values
	 */
	public int[][] prepareDataTable(RecordSet recordSet, int[][] dataTable) {
		try {
			// 0=total voltage, 1=ServoImpuls on, 2=ServoImpulse off, 3=temperature, 4=cell voltage, 5=cell voltage, 6=cell voltage, .... 
			String[] recordNames = recordSet.getRecordNames();
			int numberRecords = recordNames.length;
			int recordEntries = recordSet.getRecordDataSize(true);

			for (int j = 0; j < numberRecords; j++) {
				Record record = recordSet.get(recordNames[j]);
				switch (j) {
				case 3: //3=temperature analog outlet
					double offset = record.getOffset(); // != 0 if curve has an defined offset
					double factor = record.getFactor(); // != 1 if a unit translation is required
					for (int i = 0; i < recordEntries; i++) {
						dataTable[i][j + 1] = Double.valueOf(((offset + record.get(i) / 1000.0) * factor) * 1000.0).intValue();
					}
					break;
				default:
					for (int i = 0; i < recordEntries; i++) {
						dataTable[i][j + 1] = record.get(i);
					}
					break;
				}
			}
		}
		catch (RuntimeException e) {
			LiPoWatch.log.log(Level.SEVERE, e.getMessage(), e);
		}
		return dataTable;
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double translateValue(Record record, double value) {
		double newValues = value;

		// 0=total voltage, 1=ServoImpuls on, 2=ServoImpulse off, 3=temperature, 4=cell voltage, 5=cell voltage, 6=cell voltage, .... 
		PropertyType property = null;
		if (record.getOrdinal() == 3) {//3=temperature [°C]
			property = record.getProperty(LiPoWatch.A1_FACTOR);
			double factor = property != null ? new Double(property.getValue()).doubleValue() : 1.0;
			property = record.getProperty(LiPoWatch.A1_FACTOR);
			double offset = property != null ? new Double(property.getValue()).doubleValue() : 0.0;
			newValues = value * factor + offset;
		}

		return newValues;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double reverseTranslateValue(Record record, double value) {
		double newValues = value;

		// 0=total voltage, 1=ServoImpuls on, 2=ServoImpulse off, 3=temperature, 4=cell voltage, 5=cell voltage, 6=cell voltage, .... 
		PropertyType property = null;
		if (record.getOrdinal() == 3) {//3=temperature [°C]
			property = record.getProperty(LiPoWatch.A1_FACTOR);
			double factor = property != null ? new Double(property.getValue()).doubleValue() : 1.0;
			property = record.getProperty(LiPoWatch.A1_FACTOR);
			double offset = property != null ? new Double(property.getValue()).doubleValue() : 0.0;
			newValues = (value - offset) / factor;
		}

		return newValues;
	}

	/**
	 * check and update visibility status of all records according the available device configuration
	 * this function must have only implementation code if the device implementation supports different configurations
	 * where some curves are hided for better overview 
	 * example: if device supports voltage, current and height and no sensors are connected to voltage and current
	 * it makes less sense to display voltage and current curves, if only height has measurement data
	 */
	public void updateVisibilityStatus(RecordSet recordSet) {
		String[] recordKeys = recordSet.getRecordNames();

		for (int i = 0; i < recordKeys.length; ++i) {
			Record record = recordSet.get(recordKeys[i]);
			boolean hasReasonableData = record.getRealMaxValue() != 0 || record.getRealMinValue() != record.getRealMaxValue();
			//record.setVisible(record.isActive() && hasReasonableData);
			//log.log(Level.FINER, record.getName() + ".setVisible = " + hasReasonableData);
			record.setDisplayable(hasReasonableData);
			LiPoWatch.log.log(Level.FINER, recordKeys[i] + " setDisplayable=" + (hasReasonableData)); //$NON-NLS-1$
		}
		recordSet.isSyncableDisplayableRecords(true);

		if (LiPoWatch.log.isLoggable(Level.FINE)) {
			for (String recordKey : recordKeys) {
				Record record = recordSet.get(recordKey);
				LiPoWatch.log.log(Level.FINE, recordKey + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		String[] recordKeys = recordSet.getRecordNames();
		int displayableCounter = 0;

		for (int i = 0; i < recordKeys.length; ++i) {
			Record record = recordSet.get(recordKeys[i]);
			if (record.isActive() && record.isDisplayable()) {
				LiPoWatch.log.log(Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
				++displayableCounter;
			}
		}
		LiPoWatch.log.log(Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
	}

	/**
	 * @return the dialog
	 */
	public LiPoWatchDialog getDialog() {
		return this.dialog;
	}

	/**
	 * @return the serialPort
	 */
	public LiPoWatchSerialPort getSerialPort() {
		return this.serialPort;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION, CalculationThread.REGRESSION_INTERVAL_SEC, CalculationThread.REGRESSION_TYPE };
	}

	/**
	 * enhance initial record set comment device specific
	 * LiPoWatch has serial number and a firmeware version
	 * @param recordSet
	 */
	public void updateInitialRecordSetComment(RecordSet recordSet) {
		recordSet.setRecordSetDescription(String.format("%s; \n%s : %s; %s : %s; ", //$NON-NLS-1$
				recordSet.getRecordSetDescription(), LiPoWatch.SERIAL_NUMBER, this.getDialog().serialNumber, LiPoWatch.FIRMEWARE_VERSION, this.getDialog().lipoWatchVersion));
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
					LiPoWatch.log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(osde.messages.MessageIds.OSDE_MSGE0025, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
				}
			}
			else {
				this.serialPort.close();
			}
		}
	}

	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	public int[] getCellVoltageOrdinals() {
		// 0=total voltage, 1=ServoImpuls on, 2=ServoImpulse off, 3=temperature, 4=cell voltage, 5=cell voltage, 6=cell voltage, .... 
		return new int[] { 0, 3 };
	}
}
