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

	static UniLogSerialPort	unilog;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DeviceConfiguration deviceConfig;
		boolean isUniLogPortAvailable = false;
		UniLogSerialPortTest.initLogger();
		try {
			deviceConfig = new DeviceConfiguration("c:\\Documents and Settings\\brueg\\Application Data\\OpenSerialDataExplorer\\Devices\\UniLog.xml");
			unilog = new UniLogSerialPort(deviceConfig, null);
			Vector<String> ports = DeviceSerialPort.listConfiguredSerialPorts();
			for (String string : ports) {
				System.out.println("found port available = " + string);
				if (deviceConfig.getPort().equals(string)) isUniLogPortAvailable = true;
			}
			if (isUniLogPortAvailable) {
				//unilog.open();
				//System.out.println("port is open");
				//System.out.println("connect status = " + unilog.checkConnectionStatus());
				//System.out.println("data    status = " + unilog.checkDataReady());
				//System.out.println("checkSum = " + last2bytes);
				
				//read UniLog version
				//unilog.readVersion();
				
				
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
			if (unilog != null && unilog.isConnected()) {
				unilog.close();
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
		rootLogger.setLevel(Level.INFO);
		// set individual log levels
		Logger logger = Logger.getLogger("osde.device.DeviceSerialPort");
    logger.setLevel(Level.FINER);
    logger.setUseParentHandlers(true);

	}

}
