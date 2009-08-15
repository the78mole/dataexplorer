/**
 * 
 */
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
	final static Logger						log	= Logger.getLogger(LiPoWatch.class.getName());

	public static final String	LOV_A1_CHECKED = "A1Checked="; //$NON-NLS-1$
	public static final String	LOV_A1_NAME = "A1Name="; //$NON-NLS-1$
	public static final String	LOV_A1_OFFSET = "A1Offset="; //$NON-NLS-1$
	public static final String	LOV_A1_FACTOR = "A1Faktor="; //$NON-NLS-1$
	public static final String	LOV_A1_UNIT = "A1Einheit="; //$NON-NLS-1$

	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_11	= new String[] { 	//11=a1Value
		LOV_A1_CHECKED, LOV_A1_NAME, LOV_A1_UNIT, LOV_A1_OFFSET, LOV_A1_FACTOR	};							

	public final static String		A1_FACTOR									= "a1_"+IDevice.FACTOR; //$NON-NLS-1$
	public final static String		A1_OFFSET									= "a1_"+IDevice.OFFSET; //$NON-NLS-1$
	
	public final static String		FIRMEWARE_VERSION					= "Firmware"; //$NON-NLS-1$
	public final static String		SERIAL_NUMBER							= "S/N"; //$NON-NLS-1$

	final OpenSerialDataExplorer	application;
	final LiPoWatchSerialPort				serialPort;
	final LiPoWatchDialog						dialog;

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
		return Messages.getString(MessageIds.OSDE_MSGT1501);
	}

	/**
	 * load the mapping exist between lov file configuration keys and OSDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
				
		lov2osdMap.put(LOV_A1_CHECKED, 		Record.IS_ACTIVE	+ "=_" + "BOOLEAN"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A1_NAME, 			Record.NAME);
		lov2osdMap.put(LOV_A1_UNIT, 			Record.UNIT);
		lov2osdMap.put(LOV_A1_OFFSET, 		IDevice.OFFSET + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A1_FACTOR, 		IDevice.FACTOR + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$

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
		String recordSetInfo = new String();
		for (int j = 0; j < this.getNumberOfMeasurements(this.getChannelName(channelNumber)); j++) {
			StringBuilder recordConfigData = new StringBuilder();
			if (j == 18) {//11=a1Value LOV_CONFIG_DATA_KEYS_UNILOG_11
				HashMap<String, String> configData = StringHelper.splitString(header.get(OSDE.LOV_CONFIG_DATA), OSDE.DATA_DELIMITER, LOV_CONFIG_DATA_KEYS_UNILOG_11);
				for (String lovKey : LOV_CONFIG_DATA_KEYS_UNILOG_11) {
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
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		for (int i = 2; i < recordDataSize; i++) { // skip UniLog min/max line
			
			System.arraycopy(dataBuffer, i*size, readBuffer, 0, size);

			// time milli seconds
			if (timeStep_ms == 0) { // set time step for this record set
				timeStep_ms = timeStep_ms + ((readBuffer[3] & 0xFF) << 24) + ((readBuffer[2] & 0xFF) << 16) + ((readBuffer[1] & 0xFF) << 8) + (readBuffer[0] & 0xFF); //TODO
				if (timeStep_ms != 0) {
					recordSet.setTimeStep_ms(timeStep_ms);
					log.log(Level.FINE, "timeStep_ms = " + timeStep_ms); //$NON-NLS-1$
				}
			}
			recordSet.addPoints(convertDataBytes(points, readBuffer));
			
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*5000)/recordDataSize), sThreadId);
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
		
		//Servoimpuls_ein = (CLng(Asc(Mid(strResult, 7, 1))) * 256 + Asc(Mid(strResult, 6, 1)) And &HFFF0) / 16
		points[1] = (((dataBuffer[6] & 0xFF) << 8) + (dataBuffer[5] & 0xFF) & 0xFFF0) / 16 * 1000;
		if (log.isLoggable(Level.FINE)) sb.append("(1)" + points[16]).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
		//Servoimpuls_aus = (CLng(Asc(Mid(strResult, 9, 1))) * 256 + Asc(Mid(strResult, 8, 1)) And &HFFF0) / 16
		points[2] = (((dataBuffer[8] & 0xFF) << 8) + (dataBuffer[7] & 0xFF) & 0xFFF0) / 16 * 1000;
		if (log.isLoggable(Level.FINE)) sb.append("(2)" + points[17]).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
		
		//Temp_intern = CLng(Asc(Mid(strResult, 15, 1))) * 256 + Asc(Mid(strResult, 14, 1))
		//Temp_intern = IIf(Temp_intern <= 32768, Temp_intern / 100, (Temp_intern - 65536) / 100)
		tmpValue = ((dataBuffer[14] & 0xFF) << 8) + (dataBuffer[13] & 0xFF);
		points[3] = tmpValue <= 32768 ? tmpValue * 10 : (tmpValue - 65536) * 10;
		if (log.isLoggable(Level.FINE)) sb.append("(3)" + points[18]).append("; "); //$NON-NLS-1$ //$NON-NLS-2$

		int numberCells = (dataBuffer[5] & 0x0F); //Zellenzahl = Asc(Mid(strResult, 6, 1)) And &HF
		log.log(Level.FINE, "numberCells = " + numberCells);
		int i;
		for (i = 0; i < numberCells; i++) {
			//For i = 0 To Zellenzahl - 1
			//   Spannung(i) = CLng(Asc(Mid(strResult, 17 + (2 * i), 1))) * 256 + Asc(Mid(strResult, 16 + (2 * i), 1))
			//   Spannung(i) = IIf(Spannung(i) <= 32768, Spannung(i) / 500, (Spannung(i) - 65536) / 500)
			//Next i
			tmpValue = ((dataBuffer[2 * i + 16] & 0xFF) << 8) + (dataBuffer[2 * i + 15] & 0xFF);
			points[i+4] = (tmpValue <= 32786 ? tmpValue * 2 : (tmpValue - 65536) * 2); //cell voltage
			if (log.isLoggable(Level.FINE)) sb.append("(" + (i+4) + ")" + points[1]).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
			totalVotage+= points[i+4];
		}
		//Messmodus = CLng(Asc(Mid(strResult, 10, 1)) And &HF0) / 16
		boolean isRelative = ((dataBuffer[9] & 0xF0) >> 4) == 1;
		if (isRelative)
			points[0] = totalVotage;
		else 
			points[0] = points[i+3];
		//points[0] = isRelative ? totalVotage : points[i-3]; // total battery voltage
		if (log.isLoggable(Level.FINE)) sb.insert(0, "(0)" + points[0] + "; "); //$NON-NLS-1$ //$NON-NLS-2$	

		log.log(Level.FINE, sb.toString());
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
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		for (int i = 0; i < recordDataSize; i++) {
			System.arraycopy(dataBuffer, i*dataBufferSize, convertBuffer, 0, dataBufferSize);
			
			// 0=total voltage, 1=ServoImpuls on, 2=ServoImpulse off, 3=temperature, 4=cell voltage, 5=cell voltage, 6=cell voltage, .... 
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
			points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
			points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff) << 0));
			points[3] = (((convertBuffer[12]&0xff) << 24) + ((convertBuffer[13]&0xff) << 16) + ((convertBuffer[14]&0xff) << 8) + ((convertBuffer[15]&0xff) << 0));
			points[4] = (((convertBuffer[16]&0xff) << 24) + ((convertBuffer[17]&0xff) << 16) + ((convertBuffer[18]&0xff) << 8) + ((convertBuffer[19]&0xff) << 0));
			points[5] = (((convertBuffer[20]&0xff) << 24) + ((convertBuffer[21]&0xff) << 16) + ((convertBuffer[22]&0xff) << 8) + ((convertBuffer[23]&0xff) << 0));
			points[6] = (((convertBuffer[24]&0xff) << 24) + ((convertBuffer[25]&0xff) << 16) + ((convertBuffer[26]&0xff) << 8) + ((convertBuffer[27]&0xff) << 0));
			points[7] = (((convertBuffer[28]&0xff) << 24) + ((convertBuffer[29]&0xff) << 16) + ((convertBuffer[30]&0xff) << 8) + ((convertBuffer[31]&0xff) << 0));
			points[8] = (((convertBuffer[32]&0xff) << 24) + ((convertBuffer[33]&0xff) << 16) + ((convertBuffer[34]&0xff) << 8) + ((convertBuffer[35]&0xff) << 0));
			points[9] = (((convertBuffer[36]&0xff) << 24) + ((convertBuffer[37]&0xff) << 16) + ((convertBuffer[38]&0xff) << 8) + ((convertBuffer[39]&0xff) << 0));
			points[10] = (((convertBuffer[40]&0xff) << 24) + ((convertBuffer[41]&0xff) << 16) + ((convertBuffer[42]&0xff) << 8) + ((convertBuffer[43]&0xff) << 0));
			points[11] = (((convertBuffer[44]&0xff) << 24) + ((convertBuffer[45]&0xff) << 16) + ((convertBuffer[46]&0xff) << 8) + ((convertBuffer[47]&0xff) << 0));
			points[12] = (((convertBuffer[48]&0xff) << 24) + ((convertBuffer[49]&0xff) << 16) + ((convertBuffer[50]&0xff) << 8) + ((convertBuffer[51]&0xff) << 0));
			points[13] = (((convertBuffer[52]&0xff) << 24) + ((convertBuffer[53]&0xff) << 16) + ((convertBuffer[54]&0xff) << 8) + ((convertBuffer[55]&0xff) << 0));
			points[14] = (((convertBuffer[56]&0xff) << 24) + ((convertBuffer[57]&0xff) << 16) + ((convertBuffer[58]&0xff) << 8) + ((convertBuffer[59]&0xff) << 0));
			points[15] = (((convertBuffer[60]&0xff) << 24) + ((convertBuffer[61]&0xff) << 16) + ((convertBuffer[62]&0xff) << 8) + ((convertBuffer[63]&0xff) << 0));
			points[16] = (((convertBuffer[64]&0xff) << 24) + ((convertBuffer[65]&0xff) << 16) + ((convertBuffer[66]&0xff) << 8) + ((convertBuffer[67]&0xff) << 0));
			points[17] = (((convertBuffer[68]&0xff) << 24) + ((convertBuffer[69]&0xff) << 16) + ((convertBuffer[70]&0xff) << 8) + ((convertBuffer[71]&0xff) << 0));
			points[18] = (((convertBuffer[72]&0xff) << 24) + ((convertBuffer[73]&0xff) << 16) + ((convertBuffer[74]&0xff) << 8) + ((convertBuffer[75]&0xff) << 0));
			
			recordSet.addPoints(points);
			
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*5000)/recordDataSize), sThreadId);
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
						dataTable[i][j+1] = new Double(((offset + record.get(i)/1000.0) * factor) * 1000.0).intValue();				
					}
					break;
				default:
					for (int i = 0; i < recordEntries; i++) {
						dataTable[i][j+1] = record.get(i);				
					}
					break;
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
		double newValues = value;
		
		// 0=total voltage, 1=ServoImpuls on, 2=ServoImpulse off, 3=temperature, 4=cell voltage, 5=cell voltage, 6=cell voltage, .... 
		String[] recordNames = record.getRecordSetNames(); 

		PropertyType property = null;
		if (record.getName().startsWith(recordNames[3])) {//3=temperature [°C]
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
		String[] recordNames = record.getRecordSetNames(); 

		PropertyType property = null;
		if (record.getName().startsWith(recordNames[3])) {//3=temperature [°C]
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

		for (String recordKey : recordSet.getNoneCalculationRecordNames()) {
			recordSet.get(recordKey).setActive(true);
		}
		for (int i=0; i<recordKeys.length; ++i) {
				Record record = recordSet.get(recordKeys[i]);
				boolean hasReasonableData = record.getRealMaxValue() != 0 || record.getRealMinValue() != record.getRealMaxValue();
				record.setVisible(record.isActive() && hasReasonableData);
				log.log(Level.FINER, record.getName() + ".setVisible = " + hasReasonableData);
				record.setDisplayable(hasReasonableData);
				log.log(Level.FINER, recordKeys[i] + " setDisplayable=" + (hasReasonableData));
		}
		recordSet.isSyncableDisplayableRecords(true);
		
		if (log.isLoggable(Level.FINE)) {
			for (String recordKey : recordKeys) {
				Record record = recordSet.get(recordKey);
				log.log(Level.FINE, recordKey + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable());
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
		//nothing to do here
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
		return new String[] {	IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION, 
				CalculationThread.REGRESSION_INTERVAL_SEC, CalculationThread.REGRESSION_TYPE};
	}

	/**
	 * enhance initial record set comment device specific
	 * UniLog has serial number and a firmeware version
	 * @param recordSet
	 */
	public void updateInitialRecordSetComment(RecordSet recordSet) {
		recordSet.setRecordSetDescription(String.format("%s; \n%s : %s; %s : %s; ",  //$NON-NLS-1$
				recordSet.getRecordSetDescription(), SERIAL_NUMBER, this.getDialog().serialNumber, FIRMEWARE_VERSION, this.getDialog().lipoWatchVersion));
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
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(osde.messages.MessageIds.OSDE_MSGE0025, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
				}
			}
			else {
				this.serialPort.close();
			}
		}
	}
}
