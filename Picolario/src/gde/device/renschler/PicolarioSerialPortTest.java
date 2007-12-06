package osde.device.renschler;

import osde.config.DeviceConfiguration;
import osde.device.DeviceSerialPort;
import gnu.io.SerialPort;

/**
 *
 */
public class PicolarioSerialPortTest {

	static PicolarioSerialPort	picolario;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DeviceConfiguration deviceConfig;
		try {
			deviceConfig = new DeviceConfiguration("c:/Documents and Settings/brueg/Application Data/OpenSerialDataExploroer/Devices/Picolario.ini", true);
			picolario = new PicolarioSerialPort(deviceConfig, null);
			DeviceSerialPort.listConfiguredSerialPorts();

			//byte[] readBuffer = new byte[] {(byte)0x1B, (byte)0x06, (byte)0x5C, (byte)0x1B, (byte)0x06, (byte)0x5C, (byte)0x1B, (byte)0x06, (byte)0x5C, (byte)0x1B, (byte)0x06, (byte)0x5C, (byte)0x1B, (byte)0x06, (byte)0x5C, (byte)0x1B, (byte)0x06, (byte)0x5C, (byte)0x1B, (byte)0x06, (byte)0x5C, (byte)0x1B, (byte)0x06, (byte)0x5C, (byte)0x1B, (byte)0x06, (byte)0x5C, (byte)0x1B, (byte)0x06, (byte)0x5C, (byte)0x00};
			//byte[]readBuffer = new byte[] {(byte)0x1B, (byte)0x06, (byte)0x5C, (byte)0x1C, (byte)0x06, (byte)0x5C, (byte)0x1C, (byte)0x06, (byte)0x5C, (byte)0x1C, (byte)0x06, (byte)0x5C, (byte)0x1C, (byte)0x06, (byte)0x5C, (byte)0x1C, (byte)0x06, (byte)0x5C, (byte)0x1C, (byte)0x06, (byte)0x5C, (byte)0x1C, (byte)0x06, (byte)0x5C, (byte)0x1C, (byte)0x06, (byte)0x5C, (byte)0x1C, (byte)0x06, (byte)0x5C, (byte)0x07};
			//System.out.println("Checksum = " + picolario.calculateSimpleCecksum(readBuffer));

			SerialPort serialPort = picolario.open();

			picolario.readNumberAvailableRecordSets();
			picolario.print(picolario.getData(null, 1, null));
			System.out.println();

			serialPort.close();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("fertig !");
	}

}
