/**
 * 
 */
package osde.device.wb;

import gnu.io.NoSuchPortException;

import java.io.FileNotFoundException;
import java.io.IOException;

import osde.config.DeviceConfiguration;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.ui.OpenSerialDataExplorer;

/**
 * @author Winfried Bruegmann
 *
 */
public class Simulator extends DeviceConfiguration implements IDevice {

	private final OpenSerialDataExplorer application;
	private final SimulatorSerialPort serialPort;
	private final SimulatorDialog	dialog;
	
	/**
	 * constructor using properties file
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NoSuchPortException 
	 */
	public Simulator(String deviceProperties) throws FileNotFoundException, IOException, NoSuchPortException {
		super(deviceProperties, true);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new SimulatorSerialPort(this, application.getStatusBar());
		this.dialog = new SimulatorDialog(this.application.getShell(), this);
	}

	/**
	 * constructor using existing device configuration
	 * @param device configuration
	 * @throws NoSuchPortException 
	 */
	public Simulator(DeviceConfiguration deviceConfig) throws NoSuchPortException {
		super(deviceConfig);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new SimulatorSerialPort(this, application.getStatusBar());
		this.dialog = new SimulatorDialog(this.application.getShell(), this);
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double translateValue(String recordKey, double value) {
		double newValues = value;
		// do some calculation
		return newValues;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double reverseTranslateValue(String recordKey, double value) {
		double newValues = value;
		// do some calculation
		return newValues;
	}
	
	/**
	 * function to query the data unit of a measurement
	 * @return the dataUnit
	 */
	public String getDataUnit(String recordKey) {
		return "?";  // m/s, V, mAh
	}
	
	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 * @return double with the adapted value
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		//add implementation where data point are calculated
		//do not forget to make record displayable -> record.setDisplayable(true);
	}

	/**
	 * @return the dialog
	 */
	public SimulatorDialog getDialog() {
		return dialog;
	}

	/**
	 * @return the serialPort
	 */
	public SimulatorSerialPort getSerialPort() {
		return serialPort;
	}

}
