package osde.device.conrad;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.device.DeviceConfiguration;
import osde.exception.TimeOutException;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;

/**
 * Sample serial port implementation, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class VC800SerialPort extends DeviceSerialPort {
	final static Logger	log	= Logger.getLogger(VC800SerialPort.class.getName());

	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public VC800SerialPort(DeviceConfiguration currentDeviceConfig, OpenSerialDataExplorer currentApplication) {
		super(currentDeviceConfig, currentApplication);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @param channel signature if device has more than one or required by device
	 * @return map containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public byte[] getData() throws Exception {
		byte[] answer = new byte[14];

		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
			}
			this.read(answer, 5000);
		}
		catch (Exception e) {
			if(!(e instanceof TimeOutException)) log.log(Level.SEVERE, e.getMessage());
			throw e;
		}
		finally {
			if (isPortOpenedByMe) this.close();
		}
		return answer;
	}
}
