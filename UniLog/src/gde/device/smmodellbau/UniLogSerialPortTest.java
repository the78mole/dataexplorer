/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008 - 2010 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import gde.log.Level;
import java.util.logging.Logger;

import gde.device.DeviceConfiguration;
import gde.log.LogFormatter;
import gde.serial.DeviceSerialPort;

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
			deviceConfig = new DeviceConfiguration("c:\\Documents and Settings\\brueg\\Application Data\\DataExplorer\\Devices\\UniLog.xml"); //$NON-NLS-1$
			device = new UniLog(deviceConfig);
			for (String string : DeviceSerialPort.listConfiguredSerialPorts(true, "", new Vector<String>())) {
				System.out.println("found port available = " + string); //$NON-NLS-1$
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
				
				serialPort.getData(null);
				
			}
			else {
				System.out.println("configured serial port not available " + deviceConfig.getPort()); //$NON-NLS-1$
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (serialPort != null) {
				serialPort.close();
				System.out.println("port is closed"); //$NON-NLS-1$
			}
			else {
				System.out.println("port is not connected"); //$NON-NLS-1$
			}
		}
	}

	private static void initLogger() {
		Handler ch = new ConsoleHandler();
		LogFormatter lf = new LogFormatter();
		Logger rootLogger = Logger.getLogger(""); //$NON-NLS-1$

		ch.setFormatter(lf);
		ch.setLevel(Level.ALL);
		rootLogger.addHandler(ch);
		// set logging levels
		rootLogger.setLevel(Level.FINER);
		// set individual log levels
		Logger logger = Logger.getLogger("gde.device.DeviceSerialPort"); //$NON-NLS-1$
    logger.setLevel(Level.FINE);
    logger.setUseParentHandlers(true);

	}

}
