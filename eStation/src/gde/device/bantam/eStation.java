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
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
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
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		for (int i = 0; i < recordDataSize; i++) { 
			System.arraycopy(dataBuffer, offset + i*lovDataSize, convertBuffer, 0, deviceDataBufferSize);
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
		
//		StringBuilder sb = new StringBuilder();
//		for (byte b : dataBuffer) {
//			sb.append(String.format("%02x", b)).append(" ");
//		}
//		log.log(Level.INFO, sb.toString());

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
	 * query if the eStation executes discharge > charge > discharge cycles
	 */
	boolean isCycleMode(byte[] dataBuffer) {
		return (((dataBuffer[8] & 0xFF)-0x80) & 0x10) > 0;
	}

	
	/**
	 * getNumberOfCycle for NiCd and NiMh, for LiXx it  will return 0
	 * accuCellType -> Lithium=1, NiMH=2, NiCd=3, Pb=4
	 * @param dataBuffer
	 * @return cycle count
	 */
	public int getNumberOfCycle(byte[] dataBuffer) {
		int cycleCount = 0;
		int accuCellType = getAccuCellType(dataBuffer);
		
		if 			(accuCellType == 2) {
			cycleCount = (dataBuffer[16] & 0xFF)- 0x80;
			//log.info("NiMh D<C " + ((dataBuffer[15] & 0xFF)- 0x80));
		}
		else if (accuCellType == 3) {
			cycleCount = (dataBuffer[12] & 0xFF)- 0x80;
			//log.info("NiCd D<C " + ((dataBuffer[11] & 0xFF)- 0x80));
		}
		
		return cycleCount;
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
	 * @param dataBuffer
	 * @return for Lithium=1, NiMH=2, NiCd=3, Pb=4
	 */
	public boolean isProcessing(byte[] dataBuffer) {
		return ((dataBuffer[24] & 0xFF)- 0x80) == 1; //processing = 1; stop = 0
	}

	/**
	 * @param dataBuffer [lenght 76 bytes]
	 * @return 0 = no processing, 1 = discharge, 2 = charge
	 */
	public int getProcessingMode(byte[] dataBuffer) {
		int modeIndex = (dataBuffer[24] & 0xFF) - 0x80; // 0=off, no processing; 1=discharge or 2=charge
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
			log.log(Level.FINE, key + " = " + configData.get(key)); //$NON-NLS-1$
		}
		return configData;
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
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		for (int i = 0; i < recordDataSize; i++) {
			System.arraycopy(dataBuffer, i*dataBufferSize, convertBuffer, 0, dataBufferSize);
			
			// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temp.extern 6=Temp.intern 7=VersorgungsSpg. 
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
			points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
			points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff) << 0));
			points[3] = new Double((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); 							// power U*I [W]
			points[4] = new Double((points[0] / 1000.0) * (points[2] / 1000.0)).intValue();											// energy U*C [mWh]
			points[5] = (((convertBuffer[12]&0xff) << 24) + ((convertBuffer[13]&0xff) << 16) + ((convertBuffer[14]&0xff) << 8) + ((convertBuffer[15]&0xff) << 0));
			points[6] = (((convertBuffer[16]&0xff) << 24) + ((convertBuffer[17]&0xff) << 16) + ((convertBuffer[18]&0xff) << 8) + ((convertBuffer[19]&0xff) << 0));
			points[7] = (((convertBuffer[20]&0xff) << 24) + ((convertBuffer[21]&0xff) << 16) + ((convertBuffer[22]&0xff) << 8) + ((convertBuffer[23]&0xff) << 0));
			// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
			for (int j=0, k=0; j<points.length - 8; ++j, k+=OSDE.SIZE_BYTES_INTEGER) {
				//log_base.info("cell " + (i+1) + " points[" + (i+8) + "]  = new Integer((((dataBuffer[" + (j+45) + "] & 0xFF)-0x80)*100 + ((dataBuffer[" + (j+46)+ "] & 0xFF)-0x80))*10);");  //45,46 CELL_420v[1];
				points[j+8] = (((convertBuffer[k+24]&0xff) << 24) + ((convertBuffer[k+25]&0xff) << 16) + ((convertBuffer[k+26]&0xff) << 8) + ((convertBuffer[k+27]&0xff) << 0));
			}
			
			recordSet.addPoints(points, false);
			
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
			// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temp.extern 6=Temp.intern 7=VersorgungsSpg. 
			// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
			String[] recordNames = recordSet.getRecordNames();	
			int numberRecords = recordNames.length;
			int recordEntries = recordSet.getRecordDataSize(true);

			for (int j = 0; j < numberRecords; j++) {
				Record record = recordSet.get(recordNames[j]);
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				for (int i = 0; i < recordEntries; i++) {
					dataTable[i][j+1] = new Double(((record.get(i)/1000.0) - reduction) * factor * 1000.0).intValue();				
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
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temp.extern 6=Temp.intern 7=VersorgungsSpg. 
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required
		
		double newValue = value * factor + offset;
		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double reverseTranslateValue(Record record, double value) {
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temp.extern 6=Temp.intern 7=VersorgungsSpg. 
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required

		double newValue = value / factor - offset;
		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
				
				log.log(Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
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
					this.getDialog().dataGatherThread.stopDataGatheringThread(false, null);
				}
				if (this.getDialog().boundsComposite != null && !this.getDialog().isDisposed()) this.getDialog().boundsComposite.redraw();
				this.serialPort.close();
			}
		}
	}
}
