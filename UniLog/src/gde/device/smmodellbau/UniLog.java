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

import gnu.io.NoSuchPortException;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import osde.OSDE;
import osde.config.Settings;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.device.PropertyType;
import osde.device.smmodellbau.unilog.MessageIds;
import osde.exception.DataInconsitsentException;
import osde.messages.Messages;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.CalculationThread;
import osde.utils.LinearRegression;
import osde.utils.QuasiLinearRegression;
import osde.utils.StringHelper;

/**
 * UniLog default device implementation, just copied from Sample project
 * @author Winfried Bruegmann
 */
public class UniLog extends DeviceConfiguration implements IDevice {
	final static Logger						log	= Logger.getLogger(UniLog.class.getName());

	public static final String	LOV_N_100_W = "n100W="; //$NON-NLS-1$
	public static final String	LOV_CURRENT_OFFSET = "Stromoffset="; //$NON-NLS-1$
	public static final String	LOV_NUMBER_MOTOR = "AnzahlMotoren="; //$NON-NLS-1$
	public static final String	LOV_NUMBER_CELLS = "AnzahlZellen="; //$NON-NLS-1$
	public static final String	LOV_RPM2_FACTOR = "DrehzahlFaktor="; //$NON-NLS-1$
	public static final String	LOV_CURRENT_INVERT = "StromInvertieren="; //$NON-NLS-1$
	public static final String	LOV_RPM_CHECKED = "RpmChecked="; //$NON-NLS-1$
	public static final String	LOV_A1_CHECKED = "A1Checked="; //$NON-NLS-1$
	public static final String	LOV_A2_CHECKED = "A2Checked="; //$NON-NLS-1$
	public static final String	LOV_A3_CHECKED = "A3Checked="; //$NON-NLS-1$
	public static final String	LOV_RPM_NAME = "RpmName="; //$NON-NLS-1$
	public static final String	LOV_A1_NAME = "A1Name="; //$NON-NLS-1$
	public static final String	LOV_A2_NAME = "A2Name="; //$NON-NLS-1$
	public static final String	LOV_A3_NAME = "A3Name="; //$NON-NLS-1$
	public static final String	LOV_RPM_OFFSET = "RpmOffset="; //$NON-NLS-1$
	public static final String	LOV_A1_OFFSET = "A1Offset="; //$NON-NLS-1$
	public static final String	LOV_A2_OFFSET = "A2Offset="; //$NON-NLS-1$
	public static final String	LOV_A3_OFFSET = "A3Offset="; //$NON-NLS-1$
	public static final String	LOV_RPM_FACTOR = "RpmFaktor="; //$NON-NLS-1$
	public static final String	LOV_A1_FACTOR = "A1Faktor="; //$NON-NLS-1$
	public static final String	LOV_A2_FACTOR = "A2Faktor="; //$NON-NLS-1$
	public static final String	LOV_A3_FACTOR = "A3Faktor="; //$NON-NLS-1$
	public static final String	LOV_RPM_UNIT = "RpmEinheit="; //$NON-NLS-1$
	public static final String	LOV_A1_UNIT = "A1Einheit="; //$NON-NLS-1$
	public static final String	LOV_A2_UNIT = " A2Einheit="; //$NON-NLS-1$
	public static final String	LOV_A3_UNIT = " A3Einheit="; //$NON-NLS-1$

//	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG	= new String[] { LOV_N_100_W, LOV_NUMBER_CELLS, LOV_RPM_CHECKED, 
//		LOV_A1_CHECKED, LOV_A2_CHECKED, LOV_A3_CHECKED, LOV_RPM_NAME, LOV_A1_NAME, LOV_A2_NAME, LOV_A3_NAME, LOV_RPM_OFFSET, 
//		LOV_A1_OFFSET, LOV_A2_OFFSET, LOV_A3_OFFSET, LOV_RPM_FACTOR, LOV_A1_FACTOR, LOV_A2_FACTOR, LOV_A3_FACTOR,
//		LOV_RPM_UNIT, LOV_A1_UNIT, LOV_A1_UNIT, LOV_A2_UNIT, LOV_A3_UNIT,
//		LOV_CURRENT_OFFSET, LOV_NUMBER_MOTOR, LOV_RPM2_FACTOR, LOV_CURRENT_INVERT,
//	};

	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_2		= new String[] { 	// 2=current
		LOV_CURRENT_OFFSET, LOV_CURRENT_INVERT };																																			
	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_6		= new String[] { 	// 6=votagePerCell
		LOV_NUMBER_CELLS };																																			
	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_7		= new String[] {	// 7=revolutionSpeed
		LOV_RPM_CHECKED, LOV_RPM_NAME, LOV_RPM_UNIT, LOV_RPM_FACTOR, LOV_RPM_OFFSET, LOV_RPM2_FACTOR, LOV_NUMBER_MOTOR };
	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_8		= new String[] { 	// 8=efficiency
		LOV_N_100_W };																																																																																																																									//A1 Modus -> 0==Temperatur, 1==Millivolt, 2=Speed 250, 3=Speed 400
	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_11	= new String[] { 	//11=a1Value
		LOV_A1_CHECKED, LOV_A1_NAME, LOV_A1_UNIT, LOV_A1_OFFSET, LOV_A1_FACTOR	};							
	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_12	= new String[] { 	//12=a2Value
		LOV_A2_CHECKED, LOV_A2_NAME, LOV_A2_UNIT, LOV_A2_OFFSET, LOV_A2_FACTOR };									
	public static final String[]	LOV_CONFIG_DATA_KEYS_UNILOG_13	= new String[] { 	//13=a3Value
		LOV_A3_CHECKED, LOV_A3_NAME, LOV_A3_UNIT, LOV_A3_OFFSET, LOV_A3_FACTOR };									

	public final static String		A1_FACTOR									= "a1_"+IDevice.FACTOR; //$NON-NLS-1$
	public final static String		A1_OFFSET									= "a1_"+IDevice.OFFSET; //$NON-NLS-1$
	public final static String		A2_FACTOR									= "a2_"+IDevice.FACTOR; //$NON-NLS-1$
	public final static String		A2_OFFSET									= "a2_"+IDevice.OFFSET; //$NON-NLS-1$
	public final static String		A3_FACTOR									= "a3_"+IDevice.FACTOR; //$NON-NLS-1$
	public final static String		A3_OFFSET									= "a3_"+IDevice.OFFSET; //$NON-NLS-1$
	
	public final static String		NUMBER_CELLS							= "number_cells"; //$NON-NLS-1$
	public final static String		PROP_N_100_WATT						= "prop_n100W"; //$NON-NLS-1$
	
	public final static String		IS_INVERT_CURRENT					= "is_invert_current"; //$NON-NLS-1$
	public final static String		CURRENT_OFFSET						= IDevice.OFFSET;
	
	public final static String		NUMBER_MOTOR							= "number_motor"; //$NON-NLS-1$
	public final static String		RPM2_FACTOR								= "revolution_factor"; //$NON-NLS-1$
	public final static String		RPM_FACTOR								= IDevice.FACTOR;
	
	public final static String		FIRMEWARE_VERSION					= "Firmware"; //$NON-NLS-1$
	public final static String		SERIAL_NUMBER							= "S/N"; //$NON-NLS-1$

	final OpenSerialDataExplorer	application;
	final UniLogSerialPort				serialPort;
	final UniLogDialog						dialog;

	/**
	 * constructor using properties file
	 * @param deviceProperties
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public UniLog(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("osde.device.smmodellbau.unilog.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = this.application != null ? new UniLogSerialPort(this, this.application) : new UniLogSerialPort(this, null);
		this.dialog = this.application != null ? new UniLogDialog(this.application.getShell(), this) : new UniLogDialog(new Shell(Display.getDefault()), this);
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_OPEN_CLOSE);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 * @throws NoSuchPortException 
	 */
	public UniLog(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("osde.device.smmodellbau.unilog.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = this.application != null ? new UniLogSerialPort(this, this.application) : new UniLogSerialPort(this, null);
		this.dialog = this.application != null ? new UniLogDialog(this.application.getShell(), this) : new UniLogDialog(new Shell(Display.getDefault()), this);
		this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_OPEN_CLOSE);
	}
	
	/**
	 * query the default stem used as record set name
	 * @return recordSetStemName
	 */
	public String getRecordSetStemName() {
		return Messages.getString(MessageIds.OSDE_MSGT1378);
	}

	/**
	 * load the mapping exist between lov file configuration keys and OSDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
				
		lov2osdMap.put(LOV_CURRENT_OFFSET, CURRENT_OFFSET + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_CURRENT_INVERT, IS_INVERT_CURRENT	+ "=_" + "BOOLEAN"); //$NON-NLS-1$ //$NON-NLS-2$
		
		lov2osdMap.put(LOV_NUMBER_CELLS, NUMBER_CELLS + "=_" + "INTEGER"); //$NON-NLS-1$ //$NON-NLS-2$

		lov2osdMap.put(LOV_RPM_CHECKED, 	Record.IS_ACTIVE	+ "=_" + "BOOLEAN"); //$NON-NLS-1$ //$NON-NLS-2$
		//lov2osdMap.put(LOV_RPM_NAME, 		Record.NAME);
		//lov2osdMap.put(LOV_RPM_UNIT, 		Record.UNIT);
		lov2osdMap.put(LOV_RPM_OFFSET, 	IDevice.OFFSET + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_RPM_FACTOR, 	IDevice.FACTOR + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_NUMBER_MOTOR, NUMBER_MOTOR); 
		lov2osdMap.put(LOV_RPM2_FACTOR, 	RPM2_FACTOR + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_N_100_W, 			PROP_N_100_WATT 	 + "=_" + "INTEGER"); //$NON-NLS-1$ //$NON-NLS-2$
	
		lov2osdMap.put(LOV_A1_CHECKED, 		Record.IS_ACTIVE	+ "=_" + "BOOLEAN"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A2_CHECKED, 		Record.IS_ACTIVE	+ "=_" + "BOOLEAN"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A3_CHECKED, 		Record.IS_ACTIVE	+ "=_" + "BOOLEAN"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A1_NAME, 			Record.NAME);
		lov2osdMap.put(LOV_A2_NAME, 			Record.NAME);
		lov2osdMap.put(LOV_A3_NAME, 			Record.NAME);
		lov2osdMap.put(LOV_A1_UNIT, 			Record.UNIT);
		lov2osdMap.put(LOV_A2_UNIT, 			Record.UNIT);
		lov2osdMap.put(LOV_A3_UNIT, 			Record.UNIT);
		lov2osdMap.put(LOV_A1_OFFSET, 		IDevice.OFFSET + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A2_OFFSET, 		IDevice.OFFSET + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A3_OFFSET, 		IDevice.OFFSET + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A1_FACTOR, 		IDevice.FACTOR + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A2_FACTOR, 		IDevice.FACTOR + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$
		lov2osdMap.put(LOV_A3_FACTOR, 		IDevice.FACTOR + "=_" + "DOUBLE"); //$NON-NLS-1$ //$NON-NLS-2$

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
			if (j == 2) {// 6=votage LOV_CONFIG_DATA_KEYS_UNILOG_2
				HashMap<String, String> configData = StringHelper.splitString(header.get(OSDE.LOV_CONFIG_DATA), OSDE.DATA_DELIMITER, LOV_CONFIG_DATA_KEYS_UNILOG_2);
				for (String lovKey : LOV_CONFIG_DATA_KEYS_UNILOG_2) {
					if (configData.containsKey(lovKey)) {
						recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER); //$NON-NLS-1$
					}
				}
			}
			else if (j == 6) {// 6=votagePerCell LOV_CONFIG_DATA_KEYS_UNILOG_6
				HashMap<String, String> configData = StringHelper.splitString(header.get(OSDE.LOV_CONFIG_DATA), OSDE.DATA_DELIMITER, LOV_CONFIG_DATA_KEYS_UNILOG_6);
				for (String lovKey : LOV_CONFIG_DATA_KEYS_UNILOG_6) {
					if (configData.containsKey(lovKey)) {
						recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER); //$NON-NLS-1$
					}
				}
			}
			else if (j == 7) { // 7=revolutionSpeed LOV_CONFIG_DATA_KEYS_UNILOG_7	
				HashMap<String, String> configData = StringHelper.splitString(header.get(OSDE.LOV_CONFIG_DATA), OSDE.DATA_DELIMITER, LOV_CONFIG_DATA_KEYS_UNILOG_7);
				for (String lovKey : LOV_CONFIG_DATA_KEYS_UNILOG_7) {
					if (configData.containsKey(lovKey)) {
						recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER); //$NON-NLS-1$
					}
				}
			}
			else if (j == 8) {// 8=efficiency LOV_CONFIG_DATA_KEYS_UNILOG_8
				HashMap<String, String> configData = StringHelper.splitString(header.get(OSDE.LOV_CONFIG_DATA), OSDE.DATA_DELIMITER, LOV_CONFIG_DATA_KEYS_UNILOG_8);
				for (String lovKey : LOV_CONFIG_DATA_KEYS_UNILOG_8) {
					if (configData.containsKey(lovKey)) {
						recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER); //$NON-NLS-1$
					}
				}
			}
			else if (j == 11) {//11=a1Value LOV_CONFIG_DATA_KEYS_UNILOG_11
				HashMap<String, String> configData = StringHelper.splitString(header.get(OSDE.LOV_CONFIG_DATA), OSDE.DATA_DELIMITER, LOV_CONFIG_DATA_KEYS_UNILOG_11);
				for (String lovKey : LOV_CONFIG_DATA_KEYS_UNILOG_11) {
					if (configData.containsKey(lovKey)) {
						recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER); //$NON-NLS-1$
					}
				}
			}
			else if (j == 12) {//12=a2Value LOV_CONFIG_DATA_KEYS_UNILOG_12
				HashMap<String, String> configData = StringHelper.splitString(header.get(OSDE.LOV_CONFIG_DATA), OSDE.DATA_DELIMITER, LOV_CONFIG_DATA_KEYS_UNILOG_12);
				for (String lovKey : LOV_CONFIG_DATA_KEYS_UNILOG_12) {
					if (configData.containsKey(lovKey)) {
						recordConfigData.append(lov2osdMap.get(lovKey)).append("=").append(configData.get(lovKey)).append(Record.DELIMITER); //$NON-NLS-1$
					}
				}
			}
			else if (j == 13) {//13=a3Value LOV_CONFIG_DATA_KEYS_UNILOG_13
				HashMap<String, String> configData = StringHelper.splitString(header.get(OSDE.LOV_CONFIG_DATA), OSDE.DATA_DELIMITER, LOV_CONFIG_DATA_KEYS_UNILOG_13);
				for (String lovKey : LOV_CONFIG_DATA_KEYS_UNILOG_13) {
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
				timeStep_ms = timeStep_ms + ((readBuffer[3] & 0xFF) << 24) + ((readBuffer[2] & 0xFF) << 16) + ((readBuffer[1] & 0xFF) << 8) + (readBuffer[0] & 0xFF);
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
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		StringBuilder sb = new StringBuilder();
		String lineSep = OSDE.LINE_SEPARATOR;
		int tmpValue = 0;
		
		// voltageReceiver *** power/drive *** group
		tmpValue = (((dataBuffer[7] & 0xFF) << 8) + (dataBuffer[6] & 0xFF)) & 0x0FFF;
		points[0] = (tmpValue * 10); //0=voltageReceiver
		if (log.isLoggable(Level.FINE)) sb.append("voltageReceiver [V] = " + points[0]).append(lineSep); //$NON-NLS-1$

		// voltage *** power/drive *** group
		tmpValue = (((dataBuffer[9] & 0xFF) << 8) + (dataBuffer[8] & 0xFF));
		if (tmpValue > 32768) tmpValue = tmpValue - 65536;
		points[1] = (tmpValue * 10); //1=voltage
		if (log.isLoggable(Level.FINE)) sb.append("voltage [V] = " + points[1]).append(lineSep); //$NON-NLS-1$

		// current *** power/drive *** group - asymmetric for 400 A sensor 
		tmpValue = (((dataBuffer[11] & 0xFF) << 8) + (dataBuffer[10] & 0xFF));
		tmpValue = tmpValue <= 55536 ? tmpValue : (tmpValue - 65536);
		points[2] = tmpValue * 10; //2=current [A]
		if (log.isLoggable(Level.FINE)) sb.append("current [A] = " + points[2]).append(lineSep); //$NON-NLS-1$

		//capacity = cycleCount > 0 ? capacity + ((points[2] * timeStep_ms * 1.0) / 3600) : 0.0;
		//points[3] = capacity.intValue(); //3=capacity [Ah]
		points[3] = 0; //3=capacity [mAh]

		//points[4] = new Double(1.0 * points[1] * points[2] / 1000).intValue(); //4=power [W]
		points[4] = 0; //4=power [W]

		//points[5] = new Double(1.0 * points[1] * points[3] / 1000000).intValue(); //5=energy [Wh]
		points[5] = 0; //5=energy [Wh]

		//points[6] = points[1] / numberCells; //6=votagePerCell
		points[6] = 0; //6=votagePerCellS

		// revolution speed *** power/drive *** group
		tmpValue = (((dataBuffer[13] & 0xFF) << 8) + (dataBuffer[12] & 0xFF));
		if (tmpValue > 50000) tmpValue = (tmpValue - 50000) * 10 + 50000;
		points[7] = (tmpValue * 1000); //7=revolutionSpeed
		if (log.isLoggable(Level.FINE)) sb.append("revolution speed [1/min] = " + points[7]).append(lineSep); //$NON-NLS-1$

		//double motorPower = (points[7]*100.0)/prop100WValue;
		//double eta = points[4] > motorPower ? (motorPower*100.0)/points[4] : 0;
		//points[8] = new Double(eta * 1000).intValue();//8=efficiency
		points[8] = 0; //8=efficiency

		// height *** power/drive *** group
		tmpValue = (((dataBuffer[15] & 0xFF) << 8) + (dataBuffer[14] & 0xFF)) + 20000;
		if (tmpValue > 32768) tmpValue = tmpValue - 65536;
		points[9] = (tmpValue * 100); //9=height
		if (log.isLoggable(Level.FINE)) sb.append("height [m] = " + points[9]).append(lineSep); //$NON-NLS-1$

		points[10] = 0; //10=slope

		// a1Modus -> 0==Temperatur, 1==Millivolt, 2=Speed 250, 3=Speed 400
		int a1Modus = (dataBuffer[7] & 0xF0) >> 4; // 11110000
		tmpValue = (((dataBuffer[17] & 0xFF) << 8) + (dataBuffer[16] & 0xFF));
		if (tmpValue > 32768) tmpValue = tmpValue - 65536;
		points[11] = new Integer(tmpValue * 100).intValue(); //11=a1Value
		if (log.isLoggable(Level.FINE)) {
			sb.append("a1Modus = " + a1Modus).append(lineSep); //$NON-NLS-1$ //$NON-NLS-2$
			if (a1Modus == 0)
				sb.append("temperatur [°C] = " + points[12]).append(lineSep); //$NON-NLS-1$
			else if (a1Modus == 1)  
				sb.append("voltage [mV] = " + points[12]).append(lineSep); //$NON-NLS-1$
			else if (a1Modus == 2)  
				sb.append("speed 250 [km/h] = " + points[12]).append(lineSep); //$NON-NLS-1$
			else if (a1Modus == 3)
				sb.append("speed 400 [km/h] = " + points[12]).append(lineSep); //$NON-NLS-1$
		}

		// A2 Modus == 0 -> temperature; A2 Modus == 1 -> impulse time length
		// A2 Modus == 2 -> mills voltage; A2 Modus == 3 -> capacity mAh
		int a2Modus = (dataBuffer[4] & 0x30) >> 4; // 00110000
		if (a2Modus == 0 || a2Modus == 2) {
			tmpValue = (((dataBuffer[19] & 0xEF) << 8) + (dataBuffer[18] & 0xFF));
			tmpValue = tmpValue > 32768 ? (tmpValue - 65536) : tmpValue;
			points[12] = new Integer(tmpValue * 100).intValue(); //12=a2Value
		}
		else if (a2Modus == 1 || a2Modus == 3) {
			tmpValue = (((dataBuffer[19] & 0xFF) << 8) + (dataBuffer[18] & 0xFF));
			points[12] = new Integer(tmpValue * 1000).intValue(); //12=a2Value
		}
		if (log.isLoggable(Level.FINE)) {
			sb.append("a2Modus = " + a2Modus).append(lineSep); //$NON-NLS-1$
			if (a2Modus == 0)
				sb.append("tempreature [°C] = " + points[12]).append(lineSep); //$NON-NLS-1$
			else if (a2Modus == 1)  
				sb.append("impulseTime [us] = " + points[12]).append(lineSep); //$NON-NLS-1$
			else if (a2Modus == 2)  
				sb.append("voltage [mV] = " + points[12]).append(lineSep); //$NON-NLS-1$
			else if (a2Modus == 3)
				sb.append("capacity [mAh] = " + points[12]).append(lineSep); //$NON-NLS-1$
		}

		// A3 Modus == 0 -> external temperature sensor; A3 Modus == 1 -> internal temperature
		// A3 Modus == 2 -> external temperature sensor; A3 Modus == 3 -> internal temperature
		int a3Modus = (dataBuffer[4] & 0xC0) >> 6; // 11000000
		tmpValue = (((dataBuffer[21] & 0xEF) << 8) + (dataBuffer[20] & 0xFF));
		if (a3Modus == 0 || a3Modus == 1 || a3Modus == 3) {
			if (tmpValue > 32768) tmpValue = tmpValue - 65536;
			points[13] = new Integer(tmpValue * 100).intValue(); //13=a3Value
		}
		else if (a3Modus == 2) {
			points[13] = new Integer(tmpValue * 100).intValue(); //13=a3Value
		}
		if (log.isLoggable(Level.FINE)) {
			sb.append("a3Modus = " + a3Modus).append(lineSep); //$NON-NLS-1$
			if (a3Modus == 0)
				sb.append("external Temperature [°C] = " + points[13]).append(lineSep); //$NON-NLS-1$
			else if (a3Modus == 1)
				sb.append("internal Temperature [°C] = " + points[13]).append(lineSep); //$NON-NLS-1$
			else if (a3Modus == 2)
				sb.append("energy [Wmin] = " + points[13]).append(lineSep); //$NON-NLS-1$
			else if (a3Modus == 3)
				sb.append("voltage [mV] = " + points[13]).append(lineSep); //$NON-NLS-1$
			else
				sb.append("a3Value = " + points[13]).append(lineSep); //$NON-NLS-1$
		}
		
		log.log(Level.FINE, sb.toString());
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
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		for (int i = 0; i < recordDataSize; i++) {
			System.arraycopy(dataBuffer, i*dataBufferSize, convertBuffer, 0, dataBufferSize);
			
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
			points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
			points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff) << 0));
			//points[3] = capacity.intValue(); //3=capacity [Ah]
			//points[4] = new Double(1.0 * points[1] * points[2] / 1000).intValue(); //4=power [W]
			//points[5] = new Double(1.0 * points[1] * points[3] / 1000000).intValue(); //5=energy [Wh]
			//points[6] = points[1] / numberCells; //6=votagePerCell
			points[7] = (((convertBuffer[12]&0xff) << 24) + ((convertBuffer[13]&0xff) << 16) + ((convertBuffer[14]&0xff) << 8) + ((convertBuffer[15]&0xff) << 0));
			//points[8] = new Double(eta * 1000).intValue();//8=efficiency
			points[9] = (((convertBuffer[16]&0xff) << 24) + ((convertBuffer[17]&0xff) << 16) + ((convertBuffer[18]&0xff) << 8) + ((convertBuffer[19]&0xff) << 0));
			//points[10] = slope			
			points[11] = (((convertBuffer[20]&0xff) << 24) + ((convertBuffer[21]&0xff) << 16) + ((convertBuffer[22]&0xff) << 8) + ((convertBuffer[23]&0xff) << 0));
			points[12] = (((convertBuffer[24]&0xff) << 24) + ((convertBuffer[25]&0xff) << 16) + ((convertBuffer[26]&0xff) << 8) + ((convertBuffer[27]&0xff) << 0));
			points[13] = (((convertBuffer[28]&0xff) << 24) + ((convertBuffer[29]&0xff) << 16) + ((convertBuffer[30]&0xff) << 8) + ((convertBuffer[31]&0xff) << 0));
			
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
			// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
			String[] recordNames = recordSet.getRecordNames();	
			int numberRecords = recordNames.length;
			int recordEntries = recordSet.getRecordDataSize(true);

			for (int j = 0; j < numberRecords; j++) {
				Record record = recordSet.get(recordNames[j]);
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				double currentOffset = 0;
				double rpmFactor = 1;
				double numberMotor = 1;
				PropertyType property = null;
				
				switch (j) { 
				case 0: //voltageReceiver
				case 1: //voltage
					break;
				case 2: //current
					property = record.getProperty(UniLog.CURRENT_OFFSET);
					currentOffset = property != null ? new Double(property.getValue()).doubleValue() : 0;
					//newValues = value + currentOffset;
					break;
				case 3: //capacity
				case 4: //power
				case 5: //energy
				case 6: //votagePerCell
					break;
				case 7: //revolutionSpeed
					property = record.getProperty(UniLog.RPM_FACTOR);
					rpmFactor = property != null ? new Double(property.getValue()).doubleValue() : 1.0;
					property = record.getProperty(UniLog.NUMBER_MOTOR);
					numberMotor = property != null ? new Double(property.getValue()).doubleValue() : 1.0;
					//newValues = value * rpmFactor / numberMotor;
					break;
				case 8: //efficiency
				case 9: //height
				case 10: //slope
				case 11: //a1Value
				case 12: //a2Value
				case 13: //a3Value
					break;
				default:
					log.log(Level.WARNING, "exceed known record names"); //$NON-NLS-1$
					break;
				}
				
				for (int i = 0; i < recordEntries; i++) {
					dataTable[i][j+1] = new Double((offset + (((record.get(i)/1000.0 + currentOffset) * rpmFactor / numberMotor) - reduction) * factor) * 1000.0).intValue();				
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
		
		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
		PropertyType property = null;
		if (record.getOrdinal() == 2) {//2=current [A]
			property = record.getProperty(UniLog.CURRENT_OFFSET);
			double currentOffset = property != null ? new Double(property.getValue()).doubleValue() : 1.0;
			newValues = value + currentOffset;
		}
		else if (record.getOrdinal() == 7) {//7=revolutionSpeed [1/min]
			property = record.getProperty(UniLog.RPM_FACTOR);
			double rpmFactor = property != null ? new Double(property.getValue()).doubleValue() : 1.0;
			property = record.getProperty(UniLog.NUMBER_MOTOR);
			double numberMotor = property != null ? new Double(property.getValue()).doubleValue() : 1.0;
			newValues = value * rpmFactor / numberMotor;
		}
		else if (record.getOrdinal() == 11 	//11=a1Value
				|| record.getOrdinal() == 12		//12=a2Value
				|| record.getOrdinal() == 13) {	//13=a3Value
			newValues = record.getFactor() * value + record.getOffset();
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
		
		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
		PropertyType property = null;
		if (record.getOrdinal() == 2) {//2=current [A]
			property = record.getProperty(UniLog.CURRENT_OFFSET);
			double currentOffset = property != null ? new Double(property.getValue()).doubleValue() : 1.0;
			newValues = value - currentOffset;
		}
		else if (record.getOrdinal() == 7) {//7=revolutionSpeed [1/min]
			property = record.getProperty(UniLog.RPM_FACTOR);
			double rpmFactor = property != null ? new Double(property.getValue()).doubleValue() : 1.0;
			property = record.getProperty(UniLog.NUMBER_MOTOR);
			double numberMotor = property != null ? new Double(property.getValue()).doubleValue() : 1.0;
			newValues = value * numberMotor / rpmFactor;
		}
		else if (record.getOrdinal() == 11 	//11=a1Value
				|| record.getOrdinal() == 12		//12=a2Value
				|| record.getOrdinal() == 13) {	//13=a3Value
			newValues = value / record.getFactor() - record.getOffset();
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
		String channelConfigKey = recordSet.getChannelConfigName();
		Record record;
		MeasurementType measurement;
		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
		String[] measurementNames = this.getMeasurementNames(channelConfigKey);
		String[] recordNames = recordSet.getRecordNames();
		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordNames.length; ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(recordNames[i]);		
			measurement = this.getMeasurement(channelConfigKey, i);
			log.log(Level.FINE, recordNames[i] + " = " + measurementNames[i]); //$NON-NLS-1$
			
			// update active state and displayable state if configuration switched with other names
			if (record.isActive() != measurement.isActive()) {
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				log.log(Level.FINE, "switch " + record.getName() + " to " + measurement.isActive()); //$NON-NLS-1$ //$NON-NLS-2$
			}	
		}
		
		// updateStateCurrentDependent
		boolean enabled = recordSet.get(recordSet.getRecordNames()[2]).isActive();
		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
		recordSet.get(recordSet.getRecordNames()[3]).setDisplayable(enabled);
		
		// updateStateVoltageAndCurrentDependent
		enabled = recordSet.get(recordSet.getRecordNames()[1]).isActive() && recordSet.get(recordSet.getRecordNames()[2]).isActive();
		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
		recordSet.get(recordSet.getRecordNames()[4]).setDisplayable(enabled);
		recordSet.get(recordSet.getRecordNames()[5]).setDisplayable(enabled);
		recordSet.get(recordSet.getRecordNames()[6]).setDisplayable(enabled);

		// updateStateVoltageCurrentRevolutionDependent
		enabled = recordSet.get(recordSet.getRecordNames()[1]).isActive() && recordSet.get(recordSet.getRecordNames()[2]).isActive() && recordSet.get(recordSet.getRecordNames()[7]).isActive();
		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
		recordSet.get(recordSet.getRecordNames()[8]).setDisplayable(enabled);
		
		// updateHeightDependent
		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
		recordSet.get(recordSet.getRecordNames()[10]).setDisplayable(recordSet.get(recordSet.getRecordNames()[9]).isActive());
	}
	
	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		//do not forget to make record displayable -> record.setDisplayable(true);
		if (recordSet.isRaw()) {
			// calculate the values required
			Record record;
			String recordKey;
			// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
			String[] measurements = recordSet.getRecordNames();
			int displayableCounter = 0;
			
			// check if measurements isActive == false and set to isDisplayable == false
			for (String measurementKey : measurements) {
				record = recordSet.get(measurementKey);
				
				if (record.isActive() && record.isDisplayable()) {
					log.log(Level.FINE, "add to displayable counter: " + record.getName());
					++displayableCounter;
				}
			}

			recordKey = measurements[3]; // 3=capacity [Ah]
			log.log(Level.FINE, "start data calculation for record = " + recordKey); //$NON-NLS-1$
			record = recordSet.get(recordKey);
			record.setDisplayable(false);
			record.clear();
			Record recordCurrent = recordSet.get(measurements[2]); // 2=current
			double timeStep_ms = recordCurrent.getTimeStep_ms(); // timeStep_ms
			Double capacity = 0.0;
			for (int i = 0; i < recordCurrent.size(); i++) {
				capacity = i > 0 ? capacity + ((recordCurrent.get(i) * timeStep_ms) / 3600) : 0.0;
				record.add(capacity.intValue());
				log.log(Level.FINEST, "adding value = " + record.get(i)); //$NON-NLS-1$
			}
			if (recordCurrent.isDisplayable()) {
				record.setDisplayable(true);
				++displayableCounter;
			}

			recordKey = measurements[4]; // 4=power [W]
			log.log(Level.FINE, "start data calculation for record = " + recordKey); //$NON-NLS-1$
			record = recordSet.get(recordKey);
			record.setDisplayable(false);
			record.clear();
			Record recordVoltage = recordSet.get(measurements[1]); // 1=voltage
			recordCurrent = recordSet.get(measurements[2]); // 2=current
			for (int i = 0; i < recordVoltage.size(); i++) {
				record.add(new Double(1.0 * recordVoltage.get(i) * recordCurrent.get(i) / 1000.0).intValue());
				log.log(Level.FINEST, "adding value = " + record.get(i)); //$NON-NLS-1$
			}
			if (recordVoltage.isDisplayable() && recordCurrent.isDisplayable()) {
				record.setDisplayable(true);
				++displayableCounter;
			}

			recordKey = measurements[5]; // 5=energy [Wh]
			log.log(Level.FINE, "start data calculation for record = " + recordKey); //$NON-NLS-1$
			record = recordSet.get(recordKey);
			record.setDisplayable(false);
			record.clear();
			recordVoltage = recordSet.get(measurements[1]); // 1=voltage
			recordCurrent = recordSet.get(measurements[2]); // 2=current
			timeStep_ms = recordVoltage.getTimeStep_ms(); // timeStep_ms
			Double power = 0.0;
			for (int i = 0; i < recordVoltage.size(); i++) {
				power = i > 0 ? power + ((recordVoltage.get(i) / 1000.0) * (recordCurrent.get(i) / 1000.0) * (timeStep_ms / 3600.0)) : 0.0;
				record.add(power.intValue());
				log.log(Level.FINEST, "adding value = " + record.get(i)); //$NON-NLS-1$
			}
			if (recordVoltage.isDisplayable() && recordCurrent.isDisplayable()) {
				record.setDisplayable(true);
				++displayableCounter;
			}

			recordKey = measurements[6]; // 6=votagePerCell
			log.log(Level.FINE, "start data calculation for record = " + recordKey); //$NON-NLS-1$
			record = recordSet.get(recordKey);
			record.setDisplayable(false);
			record.clear();
			recordVoltage = recordSet.get(measurements[1]); // 1=voltage
			PropertyType property = record.getProperty(UniLog.NUMBER_CELLS);
			int numberCells = property != null ? new Integer(property.getValue()) : 4;
			for (int i = 0; i < recordVoltage.size(); i++) {
				record.add(new Double(recordVoltage.get(i) / numberCells).intValue());
				log.log(Level.FINEST, "adding value = " + record.get(i)); //$NON-NLS-1$
			}
			if (recordVoltage.isDisplayable()) {
				record.setDisplayable(true);
				++displayableCounter;
			}

			recordKey = measurements[8]; // 8=efficiency
			log.log(Level.FINE, "start data calculation for record = " + recordKey); //$NON-NLS-1$
			record = recordSet.get(recordKey);
			record.setDisplayable(false);
			record.clear();
			Record recordRevolution = recordSet.get(measurements[7]); // 7=revolutionSpeed
			property = recordRevolution.getProperty(UniLog.RPM_FACTOR);
			double rpmFactor = property != null ? new Double(property.getValue()).doubleValue() : 1.0;
			property = recordRevolution.getProperty(UniLog.NUMBER_MOTOR);
			double numberMotor = property != null ? new Double(property.getValue()).doubleValue() : 1.0;
			Record recordPower = recordSet.get(measurements[4]); // 4=power [w]
			property = record.getProperty(UniLog.PROP_N_100_WATT);
			int prop_n100W = property != null ? new Integer(property.getValue()) : 10000;
			for (int i = 0; i < recordRevolution.size(); i++) {
				double motorPower = Math.pow(((recordRevolution.get(i) * rpmFactor / numberMotor) / 1000.0 * 4.64) / prop_n100W, 3) * 1000.0;
				//if (recordRevolution.get(i)> 100) log.log(Level.INFO, "recordPower=" + recordPower.get(i) + " motorPower=" + motorPower);
				double eta = (recordPower.get(i)) > motorPower ? (motorPower * 100.0) / recordPower.get(i) : 0.0;
				record.add(new Double(eta * 1000).intValue());
				log.log(Level.FINEST, "adding value = " + record.get(i)); //$NON-NLS-1$
			}
			if (recordRevolution.isDisplayable() && recordPower.isDisplayable()) {
				record.setDisplayable(true);
				++displayableCounter;
			}

			boolean isNoSlopeCalculationStarted = true;
			recordKey = measurements[10]; // 10=slope
			log.log(Level.FINE, "start data calculation for record = " + recordKey); //$NON-NLS-1$
			record = recordSet.get(recordKey);
			record.setDisplayable(false);
			record.clear();
			property = record.getProperty(CalculationThread.REGRESSION_INTERVAL_SEC);
			int regressionInterval = property != null ? new Integer(property.getValue()) : 4;
			property = record.getProperty(CalculationThread.REGRESSION_TYPE);
			if (property == null || property.getValue().equals(CalculationThread.REGRESSION_TYPE_CURVE))
				this.calculationThread = new QuasiLinearRegression(recordSet, measurements[9], measurements[10], regressionInterval);
			else
				this.calculationThread = new LinearRegression(recordSet, measurements[9], measurements[10], regressionInterval);

			try {
				this.calculationThread.start();
			}
			catch (RuntimeException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
			if (recordSet.get(measurements[9]).isDisplayable()) {
				//record.setDisplayable(true); // set within calculation thread
				isNoSlopeCalculationStarted = false;
				++displayableCounter;
			}
			
			log.log(Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
			recordSet.setConfiguredDisplayable(displayableCounter);

			if (isNoSlopeCalculationStarted) this.application.updateGraphicsWindow();
			log.log(Level.FINE, "finished data calculation for record = " + recordKey); //$NON-NLS-1$
		}
	}

	/**
	 * @return the dialog
	 */
	public UniLogDialog getDialog() {
		return this.dialog;
	}

	/**
	 * @return the serialPort
	 */
	public UniLogSerialPort getSerialPort() {
		return this.serialPort;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[] {	IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION, 
				NUMBER_CELLS, PROP_N_100_WATT, 
				CalculationThread.REGRESSION_INTERVAL_SEC, CalculationThread.REGRESSION_TYPE};
	}

	/**
	 * enhance initial record set comment device specific
	 * UniLog has serial number and a firmeware version
	 * @param recordSet
	 */
	public void updateInitialRecordSetComment(RecordSet recordSet) {
		recordSet.setRecordSetDescription(String.format("%s; \n%s : %s; %s : %s; ",  //$NON-NLS-1$
				recordSet.getRecordSetDescription(), SERIAL_NUMBER, this.getDialog().serialNumber, FIRMEWARE_VERSION, this.getDialog().unilogVersion));
	}
	
	/**
	 * invert data of current curve
	 */
	public void invertRecordData(Record record) {
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		for (int i=0; i<record.realSize(); ++i) {
			int value = record.get(i) * -1;
			record.set(i, value);
			if (i != 0) {
				if (value > max) max = value;
				if (value < min) min = value;
			}
			else {
				min = max = value;
			}
		}
		record.setMinMax(min, max);
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
	
	/**
	 * get the analog modus of A1, A2 and A3 to update the analog measurements of the given channel configuration
	 * @param dataBuffer
	 * @param configKey
	 */
	public void updateMeasurementByAnalogModi(byte[] dataBuffer, String configKey) {
		// a1Modus -> 0==Temperature, 1==Millivolt, 2=Speed 250, 3=Speed 400
		int a1Modus = (dataBuffer[7] & 0xF0) >> 4; // 11110000

		// A2 Modus == 0 -> temperature; A2 Modus == 1 -> impulse time length
		// A2 Modus == 2 -> mills voltage; A2 Modus == 3 -> capacity mAh
		int a2Modus = (dataBuffer[4] & 0x30) >> 4; // 00110000

		// A3 Modus == 0 -> external temperature sensor; A3 Modus == 1 -> internal temperature
		// A3 Modus == 2 -> energy in Wmin; A3 Modus == 3 -> mills voltage
		int a3Modus = (dataBuffer[4] & 0xC0) >> 6; // 11000000

		if (log.isLoggable(Level.INFO)) {
			StringBuilder sb = new StringBuilder();
			sb.append("a1Modus = " + a1Modus).append(OSDE.LINE_SEPARATOR); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("a2Modus = " + a2Modus).append(OSDE.LINE_SEPARATOR); //$NON-NLS-1$
			sb.append("a3Modus = " + a3Modus).append(OSDE.LINE_SEPARATOR); //$NON-NLS-1$
			log.log(Level.FINE, sb.toString());
		}
		
		MeasurementType measurement = this.getMeasurement(configKey, 11); // 11=A1
		measurement.setName(UniLogDialog.A1_MODUS_NAMES[a1Modus].trim());
		measurement.setUnit(UniLogDialog.A1_MODUS_UNITS[a1Modus].trim());

		measurement = this.getMeasurement(configKey, 12); // 12=A1
		measurement.setName(UniLogDialog.A2_MODUS_NAMES[a2Modus].trim());
		measurement.setUnit(UniLogDialog.A2_MODUS_UNITS[a2Modus].trim());
		
		measurement = this.getMeasurement(configKey, 13); // 13=A1
		measurement.setName(UniLogDialog.A3_MODUS_NAMES[a3Modus].trim());
		measurement.setUnit(UniLogDialog.A3_MODUS_UNITS[a3Modus].trim());
	}
	
	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	public int[] getCellVoltageOrdinals() {
		return new int[] {-1, -1};
	}
}
