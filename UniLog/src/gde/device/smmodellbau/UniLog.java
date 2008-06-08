package osde.device.smmodellbau;

import gnu.io.NoSuchPortException;

import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import osde.data.Record;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.device.PropertyType;
import osde.exception.DataInconsitsentException;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.CalculationThread;
import osde.utils.LinearRegression;
import osde.utils.QuasiLinearRegression;

/**
 * UniLog default device implementation, just copied from Sample project
 * @author Winfried Bruegmann
 */
public class UniLog extends DeviceConfiguration implements IDevice {
	final static Logger						log	= Logger.getLogger(UniLog.class.getName());

	public final static String		A1_FACTOR									= "a1_"+IDevice.FACTOR;
	public final static String		A1_OFFSET									= "a1_"+IDevice.OFFSET;
	public final static String		A2_FACTOR									= "a21_"+IDevice.FACTOR;
	public final static String		A2_OFFSET									= "a2_"+IDevice.OFFSET;
	public final static String		A3_FACTOR									= "a3_"+IDevice.FACTOR;
	public final static String		A3_OFFSET									= "a3_"+IDevice.OFFSET;
	public final static String		NUMBER_CELLS							= "number_cells";
	public final static String		PROP_N_100_WATT						= "prop_n100W";
	public final static String		FIRMEWARE_VERSION					= "Firmware";
	public final static String		SERIAL_NUMBER							= "S/N";

	final OpenSerialDataExplorer	application;
	final UniLogSerialPort				serialPort;
	final UniLogDialog						dialog;
	CalculationThread							slopeCalculationThread;

	/**
	 * constructor using properties file
	 * @param deviceProperties
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public UniLog(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = this.application != null ? new UniLogSerialPort(this, this.application) : new UniLogSerialPort(this, null);
		this.dialog = this.application != null ? new UniLogDialog(this.application.getShell(), this) : new UniLogDialog(new Shell(Display.getDefault()), this);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 * @throws NoSuchPortException 
	 */
	public UniLog(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = this.application != null ? new UniLogSerialPort(this, this.application) : new UniLogSerialPort(this, null);
		this.dialog = this.application != null ? new UniLogDialog(this.application.getShell(), this) : new UniLogDialog(new Shell(Display.getDefault()), this);
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
	 * @throws DataInconsitsentException 
	 */
	public void addAdaptedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize) throws DataInconsitsentException {
		int timeStep_ms = 0;		
		int size = this.getLovDataByteSize();
		byte[] readBuffer = new byte[size];
		int[] points = new int[this.getNumberOfMeasurements(recordSet.getChannelConfigName())];
		
		for (int i = 2; i < recordDataSize; i++) { // skip UniLog min/max line
			
			System.arraycopy(dataBuffer, i*size, readBuffer, 0, size);

			// time milli seconds
			if (timeStep_ms == 0) { // set time step for this record set
				timeStep_ms = timeStep_ms + ((readBuffer[3] & 0xFF) << 24) + ((readBuffer[2] & 0xFF) << 16) + ((readBuffer[1] & 0xFF) << 8) + (readBuffer[0] & 0xFF);
				if (timeStep_ms != 0) {
					recordSet.setTimeStep_ms(timeStep_ms);
					if (log.isLoggable(Level.FINE)) log.info("timeStep_ms = " + timeStep_ms);
				}
			}

			recordSet.addPoints(converDataBytes(points, readBuffer), false);
		}
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	public int[] converDataBytes(int[] points, byte[] dataBuffer) {
		StringBuilder sb = new StringBuilder();
		String lineSep = System.getProperty("line.separator");
		int tmpValue = 0;
		
		// voltageReceiver *** power/drive *** group
		tmpValue = (((dataBuffer[7] & 0xFF) << 8) + (dataBuffer[6] & 0xFF)) & 0x0FFF;
		points[0] = (tmpValue * 10); //0=voltageReceiver
		if (log.isLoggable(Level.FINE)) sb.append("voltageReceiver [V] = " + points[0]).append(lineSep);

		// voltage *** power/drive *** group
		tmpValue = (((dataBuffer[9] & 0xFF) << 8) + (dataBuffer[8] & 0xFF));
		if (tmpValue > 32768) tmpValue = tmpValue - 65536;
		points[1] = (tmpValue * 10); //1=voltage
		if (log.isLoggable(Level.FINE)) sb.append("voltage [V] = " + points[1]).append(lineSep);

		// current *** power/drive *** group - asymmetric for 400 A sensor 
		tmpValue = (((dataBuffer[11] & 0xFF) << 8) + (dataBuffer[10] & 0xFF));
		tmpValue = tmpValue <= 55536 ? tmpValue : (tmpValue - 65536);
		points[2] = tmpValue * 10; //2=current [A]
		if (log.isLoggable(Level.FINE)) sb.append("current [A] = " + points[2]).append(lineSep);

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
		if (log.isLoggable(Level.FINE)) sb.append("revolution speed [1/min] = " + points[7]).append(lineSep);

		//double motorPower = (points[7]*100.0)/prop100WValue;
		//double eta = points[4] > motorPower ? (motorPower*100.0)/points[4] : 0;
		//points[8] = new Double(eta * 1000).intValue();//8=efficiency
		points[8] = 0; //8=efficiency

		// height *** power/drive *** group
		tmpValue = (((dataBuffer[15] & 0xFF) << 8) + (dataBuffer[14] & 0xFF)) + 20000;
		if (tmpValue > 32768) tmpValue = tmpValue - 65536;
		points[9] = (tmpValue * 100); //9=height
		if (log.isLoggable(Level.FINE)) sb.append("height [m] = " + points[9]).append(lineSep);

		points[10] = 0; //10=slope

		// a1Modus -> 0==Temperatur, 1==Millivolt, 2=Speed 250, 3=Speed 400
		int a1Modus = (dataBuffer[7] & 0xF0) >> 4; // 11110000
		tmpValue = (((dataBuffer[17] & 0xFF) << 8) + (dataBuffer[16] & 0xFF));
		if (tmpValue > 32768) tmpValue = tmpValue - 65536;
		points[11] = new Integer(tmpValue * 100).intValue(); //11=a1Value
		if (log.isLoggable(Level.FINE)) {
			sb.append("a1Modus = " + a1Modus + " (0==Temperatur, 1==Millivolt, 2=Speed 250, 3=Speed 400)").append(lineSep);
			sb.append("a1Value = " + points[11]).append(lineSep);
		}

		// A2 Modus == 0 -> external sensor; A2 Modus != 0 -> impulse time length
		int a2Modus = (dataBuffer[4] & 0x30); // 00110000
		if (a2Modus == 0) {
			tmpValue = (((dataBuffer[19] & 0xEF) << 8) + (dataBuffer[18] & 0xFF));
			tmpValue = tmpValue > 32768 ? (tmpValue - 65536) : tmpValue;
			points[12] = new Integer(tmpValue * 100).intValue(); //12=a2Value
		}
		else {
			tmpValue = (((dataBuffer[19] & 0xFF) << 8) + (dataBuffer[18] & 0xFF));
			points[12] = new Integer(tmpValue * 1000).intValue(); //12=a2Value
		}
		if (log.isLoggable(Level.FINE)) {
			sb.append("a2Modus = " + a2Modus + " (0 -> external temperature sensor; !0 -> impulse time length)").append(lineSep);
			if (a2Modus == 0)
				sb.append("a2Value = " + points[12]).append(lineSep);
			else
				sb.append("impulseTime [us]= " + points[12]).append(lineSep);
		}

		// A3 Modus == 0 -> external sensor; A3 Modus != 0 -> internal temperature
		int a3Modus = (dataBuffer[4] & 0xC0); // 11000000
		tmpValue = (((dataBuffer[21] & 0xEF) << 8) + (dataBuffer[20] & 0xFF));
		if (tmpValue > 32768) tmpValue = tmpValue - 65536;
		points[13] = new Integer(tmpValue * 100).intValue(); //13=a3Value
		if (log.isLoggable(Level.FINE)) {
			sb.append("a3Modus = " + a3Modus + " (0 -> external temperature sensor; !0 -> internal temperature)").append(lineSep);
			if (a3Modus == 0)
				sb.append("a3Value = " + points[13]).append(lineSep);
			else
				sb.append("tempIntern = " + points[13]).append(lineSep);
		}
		
		if (log.isLoggable(Level.FINE)) log.info(sb.toString());
		return points;
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double translateValue(Record record, double value) {
		double newValues = (record.getOffset() * 1000.0) + record.getFactor() * value;
		// do some calculation
		return newValues;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double reverseTranslateValue(Record record, double value) {
		double newValues = value / record.getFactor() - (record.getOffset() * 1000.0);
		// do some calculation
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
		String configKey = recordSet.getChannelConfigName();
		Record record;
		MeasurementType measurement;
		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
		String[] measurements = this.getMeasurementNames(configKey);
		// check if measurements isActive == false and set to isDisplayable == false
		for (String measurementKey : measurements) {
			record = recordSet.get(measurementKey);
			measurement = this.getMeasurement(configKey, measurementKey);
			
			// update active state and displayable state if configuration switched with other names
			if (record.isActive() != measurement.isActive()) {
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				log.info("switch " + record.getName() + " to " + measurement.isActive());
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
					
		this.application.updateGraphicsWindow();
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
				
				if (record.isActive()) {
					++displayableCounter;
				}
			}

			recordKey = measurements[3]; // 3=capacity [Ah]
			if (log.isLoggable(Level.FINE)) log.fine("start data calculation for record = " + recordKey);
			record = recordSet.get(recordKey);
			record.clear();
			Record recordCurrent = recordSet.get(measurements[2]); // 2=current
			double timeStep_ms = recordSet.getTimeStep_ms(); // timeStep_ms
			Double capacity = 0.0;
			for (int i = 0; i < recordCurrent.size(); i++) {
				capacity = i > 0 ? capacity + ((recordCurrent.get(i) * timeStep_ms) / 3600) : 0.0;
				record.add(capacity.intValue());
				if (log.isLoggable(Level.FINEST)) log.finest("adding value = " + record.get(i));
			}
			if (recordCurrent.isDisplayable()) {
				record.setDisplayable(true);
				++displayableCounter;
			}

			recordKey = measurements[4]; // 4=power [W]
			if (log.isLoggable(Level.FINE)) log.fine("start data calculation for record = " + recordKey);
			record = recordSet.get(recordKey);
			record.clear();
			Record recordVoltage = recordSet.get(measurements[1]); // 1=voltage
			recordCurrent = recordSet.get(measurements[2]); // 2=current
			for (int i = 0; i < recordVoltage.size(); i++) {
				record.add(new Double(1.0 * recordVoltage.get(i) * recordCurrent.get(i) / 1000.0).intValue());
				if (log.isLoggable(Level.FINEST)) log.finest("adding value = " + record.get(i));
			}
			if (recordVoltage.isDisplayable() && recordCurrent.isDisplayable()) {
				record.setDisplayable(true);
				++displayableCounter;
			}

			recordKey = measurements[5]; // 5=energy [Wh]
			if (log.isLoggable(Level.FINE)) log.fine("start data calculation for record = " + recordKey);
			record = recordSet.get(recordKey);
			record.clear();
			recordVoltage = recordSet.get(measurements[1]); // 1=voltage
			recordCurrent = recordSet.get(measurements[2]); // 2=current
			timeStep_ms = recordSet.getTimeStep_ms(); // timeStep_ms
			Double power = 0.0;
			for (int i = 0; i < recordVoltage.size(); i++) {
				power = i > 0 ? power + ((recordVoltage.get(i) / 1000.0) * (recordCurrent.get(i) / 1000.0) * (timeStep_ms / 3600.0)) : 0.0;
				record.add(power.intValue());
				if (log.isLoggable(Level.FINEST)) log.finest("adding value = " + record.get(i));
			}
			if (recordVoltage.isDisplayable() && recordCurrent.isDisplayable()) {
				record.setDisplayable(true);
				++displayableCounter;
			}

			recordKey = measurements[6]; // 6=votagePerCell
			if (log.isLoggable(Level.FINE)) log.fine("start data calculation for record = " + recordKey);
			record = recordSet.get(recordKey);
			record.clear();
			recordVoltage = recordSet.get(measurements[1]); // 1=voltage
			PropertyType property = record.getProperty(UniLog.NUMBER_CELLS);
			int numberCells = property != null ? new Integer(property.getValue()) : 4;
			for (int i = 0; i < recordVoltage.size(); i++) {
				record.add(new Double((recordVoltage.get(i) / 1000.0 / numberCells) * 1000).intValue());
				if (log.isLoggable(Level.FINEST)) log.finest("adding value = " + record.get(i));
			}
			if (recordVoltage.isDisplayable()) {
				record.setDisplayable(true);
				++displayableCounter;
			}

			recordKey = measurements[8]; // 8=efficiency
			if (log.isLoggable(Level.FINE)) log.fine("start data calculation for record = " + recordKey);
			record = recordSet.get(recordKey);
			record.clear();
			Record recordRevolution = recordSet.get(measurements[7]); // 7=revolutionSpeed
			Record recordPower = recordSet.get(measurements[4]); // 4=power [w]
			property = record.getProperty(UniLog.PROP_N_100_WATT);
			int prop_n100W = property != null ? new Integer(property.getValue()) : 10000;
			for (int i = 0; i < recordRevolution.size(); i++) {
				double motorPower = Math.pow((recordRevolution.get(i) / 1000.0 * 4.64) / prop_n100W, 3) * 1000.0;
				double eta = (recordPower.get(i)) > motorPower ? (motorPower * 100.0) / recordPower.get(i) : 0.0;
				record.add(new Double(eta * 1000).intValue());
				if (log.isLoggable(Level.FINEST)) log.finest("adding value = " + record.get(i));
			}
			if (recordRevolution.isDisplayable() && recordPower.isDisplayable()) {
				record.setDisplayable(true);
				++displayableCounter;
			}

			boolean isNoSlopeCalculationStarted = true;
			recordKey = measurements[10]; // 10=slope
			if (log.isLoggable(Level.FINE)) log.fine("start data calculation for record = " + recordKey);
			record = recordSet.get(recordKey);
			record.clear();
			property = record.getProperty(CalculationThread.REGRESSION_INTERVAL_SEC);
			int regressionInterval = property != null ? new Integer(property.getValue()) : 4;
			property = record.getProperty(CalculationThread.REGRESSION_TYPE);
			if (property == null || property.getValue().equals(CalculationThread.REGRESSION_TYPE_CURVE))
				this.slopeCalculationThread = new QuasiLinearRegression(recordSet, measurements[9], measurements[10], regressionInterval);
			else
				this.slopeCalculationThread = new LinearRegression(recordSet, measurements[9], measurements[10], regressionInterval);

			this.slopeCalculationThread.start();
			if (recordSet.get(measurements[9]).isDisplayable()) {
				record.setDisplayable(true);
				isNoSlopeCalculationStarted = false;
				++displayableCounter;
			}
			
			log.fine("displayableCounter = " + displayableCounter);
			recordSet.setConfiguredDisplayable(displayableCounter);

			if (isNoSlopeCalculationStarted) this.application.updateGraphicsWindow();
			log.fine("finished data calculation for record = " + recordKey);
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
		recordSet.setRecordSetDescription(String.format("%s; %s : %s; %s : %s; ", 
				recordSet.getRecordSetDescription(), SERIAL_NUMBER, this.getDialog().serialNumber, FIRMEWARE_VERSION, this.getDialog().unilogVersion));
	}
}
