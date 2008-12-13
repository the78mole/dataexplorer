package osde.device.conrad;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.device.DeviceConfiguration;
import osde.messages.Messages;
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
	public HashMap<String, Object> getData(int recordNumber, VC800Dialog dialog, String channelConfigKey) throws Exception {
		HashMap<String, Object> dataMap = new HashMap<String, Object>();
		byte[] answer = new byte[24];

		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
			}
			dataMap.put(channelConfigKey + recordNumber, answer);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			dialog.okButton.setText(Messages.getString(MessageIds.OSDE_MSGT1001));
			if (isPortOpenedByMe) this.close();
		}
		return dataMap;
	}

	/*
	 * additional device dependent implementations
	 */
	//public synchronized void start(byte[] channel) throws IOException
	//public synchronized void stop(byte[] channel) throws IOException
	//public synchronized String[] getVersion() throws IOException
}
