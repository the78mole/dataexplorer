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

	public final static String		NUMBER_CELLS							= "number_cells";
	public final static String		PROP_N_100_WATT						= "prop_n100W";

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
		// updateStateVoltageAndCurrentDependent
		boolean enabled = recordSet.get(recordSet.getRecordNames()[1]).isActive() && recordSet.get(recordSet.getRecordNames()[2]).isActive();
		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
		recordSet.get(recordSet.getRecordNames()[3]).setDisplayable(enabled);
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
}
