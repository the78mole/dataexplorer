package osde.device.smmodellbau;

import gnu.io.NoSuchPortException;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.ui.OpenSerialDataExplorer;

/**
 * UniLog default device implementation, just copied from Sample project
 * @author Winfried Bruegmann
 */
public class UniLog extends DeviceConfiguration implements IDevice {

	private final OpenSerialDataExplorer application;
	private final UniLogSerialPort serialPort;
	private final UniLogDialog	dialog;
	
	/**
	 * constructor using properties file
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NoSuchPortException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public UniLog(String deviceProperties) throws FileNotFoundException, IOException, NoSuchPortException, ParserConfigurationException, SAXException {
		super(deviceProperties);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new UniLogSerialPort(this, application.getStatusBar());
		this.dialog = new UniLogDialog(this.application.getShell(), this);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 * @throws NoSuchPortException 
	 */
	public UniLog(DeviceConfiguration deviceConfig) throws NoSuchPortException {
		super(deviceConfig);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new UniLogSerialPort(this, application.getStatusBar());
		this.dialog = new UniLogDialog(this.application.getShell(), this);
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
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		//add implementation where data point are calculated
		//do not forget to make record displayable -> record.setDisplayable(true);
	}

	/**
	 * @return the dialog
	 */
	public UniLogDialog getDialog() {
		return dialog;
	}

	/**
	 * @return the serialPort
	 */
	public UniLogSerialPort getSerialPort() {
		return serialPort;
	}

}
