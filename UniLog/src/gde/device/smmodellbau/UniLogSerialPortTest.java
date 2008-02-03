package osde.device.smmodellbau;

import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.device.DeviceConfiguration;
import osde.device.DeviceSerialPort;
import osde.log.LogFormatter;

public class UniLogSerialPortTest {

	//private static UniLog device;
	private static UniLog device;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		boolean isUniLogPortAvailable = false;
		DeviceConfiguration deviceConfig;
		UniLogSerialPort serialPort = null;
		
		UniLogSerialPortTest.initLogger();
		try {
			deviceConfig = new DeviceConfiguration("c:\\Documents and Settings\\brueg\\Application Data\\OpenSerialDataExplorer\\Devices\\UniLog.xml");
			device = new UniLog(deviceConfig);
			Vector<String> ports = DeviceSerialPort.listConfiguredSerialPorts();
			for (String string : ports) {
				System.out.println("found port available = " + string);
				if (deviceConfig.getPort().equals(string)) isUniLogPortAvailable = true;
			}
			if (isUniLogPortAvailable) {
				serialPort = device.getSerialPort();
				//serialPort.open();
				//System.out.println("port is open");
				//System.out.println("connect status = " + serialPort.checkConnectionStatus());
				//System.out.println("data    status = " + serialPort.checkDataReady());
				//System.out.println("checkSum = " + last2bytes);
				//serialPort.waitDataReady(5);
				
				serialPort.getData(null, 0, device);
				
			}
			else {
				System.out.println("configured serial port not available " + deviceConfig.getPort());
			}
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			if (serialPort != null && serialPort.isConnected()) {
				serialPort.close();
				System.out.println("port is closed");
			}
			else {
				System.out.println("port is not connected");
			}
		}
	}

	private static void initLogger() {
		Handler ch = new ConsoleHandler();
		LogFormatter lf = new LogFormatter();
		Logger rootLogger = Logger.getLogger("");

		ch.setFormatter(lf);
		ch.setLevel(Level.ALL);
		rootLogger.addHandler(ch);
		// set logging levels
		rootLogger.setLevel(Level.FINER);
		// set individual log levels
		Logger logger = Logger.getLogger("osde.device.DeviceSerialPort");
    logger.setLevel(Level.FINE);
    logger.setUseParentHandlers(true);

	}

}
