/**
 * 
 */
package osde.device.bantam;

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
import osde.exception.ApplicationConfigurationException;
import osde.exception.DataInconsitsentException;
import osde.exception.SerialPortException;
import osde.messages.Messages;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;

/**
 * Sample device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class eStation extends DeviceConfiguration implements IDevice {
	final static Logger						log	= Logger.getLogger(eStation.class.getName());
	
	public final	String[]	USAGE_MODE;
	public final	String[]	ACCU_TYPES;

	public final static String		CONFIG_EXT_TEMP_CUT_OFF			= "ext_temp_cut_off"; //$NON-NLS-1$
	public final static String		CONFIG_WAIT_TIME						= "wait_time"; //$NON-NLS-1$
	public final static String		CONFIG_IN_VOLTAGE_CUT_OFF		= "in_voltage_cut_off"; //$NON-NLS-1$
	public final static String		CONFIG_SAFETY_TIME					= "safety_time"; //$NON-NLS-1$
	public final static String		CONFIG_SET_CAPASITY					= "capacity_cut_off"; //$NON-NLS-1$
	public final static String		CONFIG_PROCESSING						= "processing"; //$NON-NLS-1$
	public final static String		CONFIG_BATTERY_TYPE					= "battery_type"; //$NON-NLS-1$
	public final static String		CONFIG_PROCESSING_TIME			= "processing_time"; //$NON-NLS-1$

	protected final OpenSerialDataExplorer				application;
	protected final EStationSerialPort						serialPort;
	protected final Channels											channels;
	protected       EStationDialog								dialog;

	protected HashMap<String, CalculationThread>	calculationThreads	= new HashMap<String, CalculationThread>();

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public eStation(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);		Messages.setDeviceResourceBundle("osde.device.htronic.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("osde.device.bantam.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.OSDE_MSGT1400), Messages.getString(MessageIds.OSDE_MSGT1401), Messages.getString(MessageIds.OSDE_MSGT1402)};
		this.ACCU_TYPES = new String[] { Messages.getString(MessageIds.OSDE_MSGT1403), Messages.getString(MessageIds.OSDE_MSGT1404), Messages.getString(MessageIds.OSDE_MSGT1405), Messages.getString(MessageIds.OSDE_MSGT1406)};

		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new EStationSerialPort(this, this.application);
		this.channels = Channels.getInstance();
		this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_START_STOP);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public eStation(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("osde.device.bantam.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.OSDE_MSGT1400), Messages.getString(MessageIds.OSDE_MSGT1401), Messages.getString(MessageIds.OSDE_MSGT1402)};
		this.ACCU_TYPES = new String[] { Messages.getString(MessageIds.OSDE_MSGT1403), Messages.getString(MessageIds.OSDE_MSGT1404), Messages.getString(MessageIds.OSDE_MSGT1405), Messages.getString(MessageIds.OSDE_MSGT1406)};

		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new EStationSerialPort(this, this.application);
		this.channels = Channels.getInstance();
		this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_START_STOP);
	}

	/**
	 * load the mapping exist between lov file configuration keys and OSDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// no device specific mapping required
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
		// ...
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	public int getLovDataByteSize() {
		return 84;  
	}
	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
	 * this method is more usable for real logger, where data can be stored and converted in one block
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @throws DataInconsitsentException 
	 */
	public void addAdaptedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize) throws DataInconsitsentException {
		// prepare the hash map containing the calculation values like factor offset, reduction, ...
		//String[] measurements = this.getMeasurementNames(recordSet.getChannelConfigName()); // 0=Spannung, 1=Höhe, 2=Steigrate, ....
		//HashMap<String, Double> calcValues = new HashMap<String, Double>();
		//calcValues.put(X_FACTOR, recordSet.get(measurements[11]).getFactor());
		//calcValues.put(X_OFFSET, recordSet.get(measurements[11]).getOffset());
		
		int offset = 0;
		int lovDataSize = this.getLovDataByteSize();
		int deviceDataBufferSize = 72;
		byte[] convertBuffer = new byte[deviceDataBufferSize];
		int[] points = new int[this.getNumberOfMeasurements(recordSet.getChannelConfigName())];
		
		for (int i = 0; i < recordDataSize; i++) { 
			System.arraycopy(dataBuffer, offset + i*lovDataSize, convertBuffer, 0, deviceDataBufferSize);
			recordSet.addPoints(convertDataBytes(points, convertBuffer), false);
		}
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {		
		
//		StringBuilder sb = new StringBuilder();
//		for (byte b : dataBuffer) {
//			sb.append(String.format("%02x", b)).append(" ");
//		}
//		log.info(sb.toString());

//		int modeIndex = getProcessingMode(dataBuffer);
//		String mode = USAGE_MODE[modeIndex];
//		int accuIndex = getAccuCellType(dataBuffer);
//		String accuType = ACCU_TYPES[accuIndex - 1]; 
//		getNumberOfLithiumXCells(dataBuffer);
		
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temp.extern 6=Temp.intern 7=VersorgungsSpg. 
		points[0] = new Integer((((dataBuffer[35] & 0xFF)-0x80)*100 + ((dataBuffer[36] & 0xFF)-0x80))*10);  //35,36   feed-back voltage
		points[1] = new Integer((((dataBuffer[33] & 0xFF)-0x80)*100 + ((dataBuffer[34] & 0xFF)-0x80))*10);  //33,34   feed-back current : 0=0.0A,900=9.00A
		points[2] = new Integer((((dataBuffer[43] & 0xFF)-0x80)*100 + ((dataBuffer[44] & 0xFF)-0x80))*1000);//43,44  cq_capa_dis;  : charged capacity
		points[3] = new Double((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); 							// power U*I [W]
		points[4] = new Double((points[0] / 1000.0) * (points[2] / 1000.0)).intValue();											// energy U*C [mWh]
		points[5] = new Integer((((dataBuffer[37] & 0xFF)-0x80)*100 + ((dataBuffer[38] & 0xFF)-0x80))*10);  //37,38  fd_ex_th;     : external temperature
		points[6] = new Integer((((dataBuffer[39] & 0xFF)-0x80)*100 + ((dataBuffer[40] & 0xFF)-0x80))*10);  //39,40  fd_in_th      : internal temperature
		points[7] = new Integer((((dataBuffer[41] & 0xFF)-0x80)*100 + ((dataBuffer[42] & 0xFF)-0x80))*10);  //41,42  fd_in_12v;    : input voltage(00.00V 30.00V)
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
		for (int i=0, j=0; i<points.length - 8; ++i, j+=2) {
			//log_base.info("cell " + (i+1) + " points[" + (i+8) + "]  = new Integer((((dataBuffer[" + (j+45) + "] & 0xFF)-0x80)*100 + ((dataBuffer[" + (j+46)+ "] & 0xFF)-0x80))*10);");  //45,46 CELL_420v[1];
			points[i+8]  = new Integer((((dataBuffer[j+45] & 0xFF)-0x80)*100 + ((dataBuffer[j+46] & 0xFF)-0x80))*10);  //45,46 CELL_420v[1];
		}

		return points;
	}

	/**
	 * @param dataBuffer
	 * @return cell count (0=auto, 1=1cell, 12=12cells)
	 */
	public int getNumberOfLithiumXCells(byte[] dataBuffer) {
		return (dataBuffer[18] & 0xFF)- 0x80;// cell count (0=auto, 1=1cell, 12=12cells)
	}

	/**
	 * @param dataBuffer
	 * @return for Lithium=1, NiMH=2, NiCd=3, Pb=4
	 */
	public int getAccuCellType(byte[] dataBuffer) {
		return (dataBuffer[23] & 0xFF)- 0x80; //Lithium=1, NiMH=2, NiCd=3, Pb=4
	}

	/**
	 * @param dataBuffer [lenght 76 bytes]
	 * @return 0 = no processing, 1 = discharge, 2 = charge
	 */
	public int getProcessingMode(byte[] dataBuffer) {
		int modeIndex = (dataBuffer[24] & 0xFF) - 0x80; // 0=off, no processing; 1=discharge or charge
		if(modeIndex != 0) {
			modeIndex = (dataBuffer[8] & 0xFF)-0x80 == 0x01 || (dataBuffer[8] & 0xFF)-0x80 == 0x11 ? 2 : 1;
		}
		return modeIndex;
	}

	/**
	 * @param dataBuffer [lenght 76 bytes]
	 * @return processing time in seconds
	 */
	public int getProcessingTime(byte[] dataBuffer) {
		return  ((dataBuffer[69] & 0xFF - 0x80)*100 + (dataBuffer[70] & 0xFF - 0x80));
	}

	/**
	 * @param dataBuffer [lenght 76 bytes]
	 * @return processing current
	 */
	public int getFeedBackCurrent(byte[] dataBuffer) {
		return  (((dataBuffer[33] & 0xFF)-0x80)*100 + ((dataBuffer[34] & 0xFF)-0x80))*10;
	}

	/**
	 * get global device configuration values
	 * @param configData
	 * @param dataBuffer
	 */
	public HashMap<String, String> getConfigurationValues(HashMap<String, String> configData, byte[] dataBuffer) {
		configData.put(eStation.CONFIG_EXT_TEMP_CUT_OFF,   ""+(dataBuffer[ 4] & 0xFF - 0x80)); //$NON-NLS-1$
		configData.put(eStation.CONFIG_WAIT_TIME,      ""+(dataBuffer[ 5] & 0xFF - 0x80)); //$NON-NLS-1$
		configData.put(eStation.CONFIG_IN_VOLTAGE_CUT_OFF, ""+(dataBuffer[ 7] & 0xFF - 0x80)/10); //$NON-NLS-1$
		configData.put(eStation.CONFIG_SAFETY_TIME,  ""+((dataBuffer[29] & 0xFF - 0x80)*100 + (dataBuffer[30] & 0xFF - 0x80) * 10)); //$NON-NLS-1$
		configData.put(eStation.CONFIG_SET_CAPASITY, ""+(((dataBuffer[31] & 0xFF - 0x80)*100 + (dataBuffer[32] & 0xFF - 0x80)))); //$NON-NLS-1$
		if(getProcessingMode(dataBuffer) != 0) {
			configData.put(eStation.CONFIG_BATTERY_TYPE, this.ACCU_TYPES[(dataBuffer[23] & 0xFF - 0x80) - 1]);
			configData.put(eStation.CONFIG_PROCESSING_TIME, ""+((dataBuffer[69] & 0xFF - 0x80)*100 + (dataBuffer[70] & 0xFF - 0x80))); //$NON-NLS-1$
		}
		for (String key : configData.keySet()) {
			log.fine(key + " = " + configData.get(key)); //$NON-NLS-1$
		}
		return configData;
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double translateValue(Record record, double value) {
		// 0=Spannung, 1=Höhe, 2=Steigung
		String[] recordNames = record.getRecordSetNames(); 
		
		String recordKey = record.getName();
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required

		// example height calculation need special procedure
		if (recordKey.startsWith(recordNames[1])) { // 1=Höhe
			// do some calculation
		}
		
		double newValue = value * factor + offset;
		log.fine("for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double reverseTranslateValue(Record record, double value) {
		// 0=Spannung, 1=Höhe, 2=Steigung
		String[] recordNames = record.getRecordSetNames(); 
		
		String recordKey = record.getName();
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required

		// example height calculation need special procedure
		if (recordKey.startsWith(recordNames[1])) { // 1=Höhe
			// do some calculation
		}
		
		double newValue = value / factor - offset;
		log.fine("for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		log.fine("no update required for " + recordSet.getName()); //$NON-NLS-1$
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are live measurement points only the calculation will take place directly after switch all to displayable
		if (recordSet.isRaw()) {
			// calculate the values required
			try {
				// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temp.extern 6=Temp.intern 7=VersorgungsSpg. 
				String[] recordNames = recordSet.getRecordNames();
				int displayableCounter = 0;

				
				// check if measurements isActive == false and set to isDisplayable == false
				for (String measurementKey : recordNames) {
					Record record = recordSet.get(measurementKey);
					
					if (record.isActive()) {
						++displayableCounter;
					}
				}
				
				String recordKey = recordNames[3]; //3=Leistung
				Record record = recordSet.get(recordKey);
				if (record != null && (record.size() == 0 || (record.getRealMinValue() == 0 && record.getRealMaxValue() == 0))) {
					this.calculationThreads.put(recordKey, new CalculationThread(recordKey, this.channels.getActiveChannel().getActiveRecordSet()));
					this.calculationThreads.get(recordKey).start();
				}
				++displayableCounter;
				
				recordKey = recordNames[4]; //4=Energie
				record = recordSet.get(recordKey);
				if (record != null && (record.size() == 0 || (record.getRealMinValue() == 0 && record.getRealMaxValue() == 0))) {
					this.calculationThreads.put(recordKey, new CalculationThread(recordKey, this.channels.getActiveChannel().getActiveRecordSet()));
					this.calculationThreads.get(recordKey).start();
				}		
				++displayableCounter;
				
				log.fine("displayableCounter = " + displayableCounter); //$NON-NLS-1$
				recordSet.setConfiguredDisplayable(displayableCounter);		
			}
			catch (RuntimeException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * @return the serialPort
	 */
	public EStationSerialPort getSerialPort() {
		return this.serialPort;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[] {IDevice.OFFSET, IDevice.FACTOR};
	}
	
	/**
	 * @return the dialog
	 */
	public EStationDialog getDialog() {
		return this.dialog;
	}
	
	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 */
	public void openCloseSerialPort() {
		if (this.serialPort != null) {
			if (!this.serialPort.isConnected()) {
				try {
					if (Channels.getInstance().getActiveChannel() != null) {
						String channelConfigKey = Channels.getInstance().getActiveChannel().getName();
						this.getDialog().dataGatherThread = new GathererThread(this.application, this, this.serialPort, channelConfigKey, this.getDialog());
						this.getDialog().dataGatherThread.start();
						if (this.getDialog().boundsComposite != null && !this.getDialog().isDisposed()) this.getDialog().boundsComposite.redraw();
					}
				}
				catch (SerialPortException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + OSDE.STRING_BLANK_COLON_BLANK + e.getMessage()}));
				}
				catch (ApplicationConfigurationException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0010));
					this.application.getDeviceSelectionDialog().open();
				}
			}
			else {
				if (this.getDialog().dataGatherThread != null) {
					this.getDialog().dataGatherThread.stopTimerThread();
					this.getDialog().dataGatherThread.interrupt();
					
					if (Channels.getInstance().getActiveChannel() != null) {
							RecordSet activeRecordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
							if (activeRecordSet != null) {
								// active record set name == life gatherer record name
								this.getDialog().dataGatherThread.finalizeRecordSet(activeRecordSet.getName(), true);
							}
					}
				}
				if (this.getDialog().boundsComposite != null && !this.getDialog().isDisposed()) this.getDialog().boundsComposite.redraw();
				this.serialPort.close();
			}
		}
	}
}
