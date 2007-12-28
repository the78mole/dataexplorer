package osde.device.smmodellbau;

import gnu.io.NoSuchPortException;

import java.io.IOException;
import java.util.HashMap;

import osde.device.DeviceConfiguration;
import osde.device.DeviceSerialPort;
import osde.device.IDevice;
import osde.ui.StatusBar;

/**
 * UniLog serial port implementation class, just copied from Sample projectS
 * @author Winfried Brügmann
 */
public class UniLogSerialPort extends DeviceSerialPort {

	/**
	 * constructor of default implementation
	 * @param deviceConfig - required by super class to initialize the serial communication port
	 * @param statusBar - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 * @throws NoSuchPortException
	 */
	public UniLogSerialPort(DeviceConfiguration deviceConfig, StatusBar statusBar) throws NoSuchPortException {
		super(deviceConfig, statusBar);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @param channel signature if device has more than one or required by device
	 * @return map containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public HashMap<String, Object> getData(byte[] channel, int recordNumber, IDevice dialog) throws IOException {
		// TODO add some sample code here
		return null;
	}

	/*
	 * additional device dependent implementations
	 */
	//public synchronized void start(byte[] channel) throws IOException
	//public synchronized void stop(byte[] channel) throws IOException
	//public synchronized String[] getVersion() throws IOException
}
