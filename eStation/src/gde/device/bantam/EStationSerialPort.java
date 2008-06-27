package osde.device.bantam;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.device.DeviceConfiguration;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.Checksum;

/**
 * Sample serial port implementation, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class EStationSerialPort extends DeviceSerialPort {
	final static Logger	log	= Logger.getLogger(EStationSerialPort.class.getName());
	
	boolean isInSync = false;
	
	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public EStationSerialPort(DeviceConfiguration currentDeviceConfig, OpenSerialDataExplorer currentApplication) {
		super(currentDeviceConfig, currentApplication);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @param channel signature if device has more than one or required by device
	 * @return map containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public byte[] getData() throws Exception {
		byte[] data = new byte[76];
		byte[] answer = new byte[] {0x00};

		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
			}
			
			answer = this.read(13, 1);
			while (answer[0] != 0x7b) {
				this.isInSync = false;
				for (int i = 1; i < answer.length; i++) {
					if(answer[i] == 0x7b){
						System.arraycopy(answer, i, data, 0, 13-i);
						answer = this.read(i, 1);
						System.arraycopy(answer, 0, data, 13-i, i);
						this.isInSync = true;
						break; //sync
					}
				}
				if(this.isInSync)
					break;
				answer = this.read(13, 1);
			}
			if (answer[0] == 0x7b) {
				System.arraycopy(answer, 0, data, 0, 13);
			}
			answer = this.read(12, 1);
			System.arraycopy(answer, 0, data, 13, 12);
			answer = this.read(12, 1);
			System.arraycopy(answer, 0, data, 25, 12);
			answer = this.read(12, 1);
			System.arraycopy(answer, 0, data, 37, 12);
			answer = this.read(12, 1);
			System.arraycopy(answer, 0, data, 49, 12);
			answer = this.read(15, 1);
			System.arraycopy(answer, 0, data, 61, 15);

			StringBuilder sb = new StringBuilder();
			for (byte b : data) {
				sb.append(String.format("0x%02x ,", b));
			}
			if (log.isLoggable(Level.FINER)) log.finer(sb.toString());
			
			if (!isChecksumOK(data)) {
				this.xferErrors++;
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			if (isPortOpenedByMe) 
				this.close();
		}
		return data;
	}

	/**
	 * check ckeck sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	private boolean isChecksumOK(byte[] buffer) {
		boolean isOK = false;
		int check_sum = Checksum.ADD(buffer, 1, 72);
		if (((check_sum & 0xF0) >> 4) + 0x30 == (buffer[73]&0xFF+0x80) && (check_sum & 0x00F) + 0x30 == (buffer[74]&0xFF))
			isOK = true;
		if (log.isLoggable(Level.FINER)) log.finer("Check_sum = " + isOK);
		return isOK;
	}

}
