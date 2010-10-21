/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010 Winfried Bruegmann
****************************************************************************************/
package gde.junit;

import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.exception.TimeOutException;
import gde.serial.DeviceSerialPort;
import gnu.io.SerialPort;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

class TestSerialPort extends DeviceSerialPort 
{
		public TestSerialPort(DeviceConfiguration deviceConfiguration) {
			super(deviceConfiguration, null);
		}


	public static void main(String[] args) {
		final DeviceConfiguration deviceConfig;
		final TestSerialPort testSerialPort;
		try {
			Settings settings = Settings.getInstance();
			System.out.println(settings.getApplHomePath());
			deviceConfig = new DeviceConfiguration("c:/Documents and Settings/brueg/Application Data/DataExplorer/Devices/UniLog.xml"); //$NON-NLS-1$
			testSerialPort = new TestSerialPort(deviceConfig);
			DeviceSerialPort.listConfiguredSerialPorts(true, "", new Vector<String>());
			SerialPort serialPort = testSerialPort.open();
			System.out.println();

			int delay = 10; // delay for 10 msec.
			int period = 10000; // repeat every 10 sec.
			Timer timer = new Timer();

			TimerTask timerTask = new TimerTask() {
				public void run() {
					try {
						byte[] buffer = new byte[50];
						//testSerialPort.getData());
						testSerialPort.read(buffer, 1000);
						
						StringBuilder sb = new StringBuilder();
						sb.append("Read  data: "); //$NON-NLS-1$
						for (int i = 0; i < buffer.length; i++) {
							sb.append(String.format("%02X ", buffer[i])); //$NON-NLS-1$
						}
						System.out.println(sb.toString());

					}
					catch (Exception e) {
						if (!(e instanceof TimeOutException))   {
							e.printStackTrace();
						}
					}
				}
			};

			timer.scheduleAtFixedRate(timerTask, delay, period);

			Thread.sleep(30000);
			timerTask.cancel();
			timer.cancel();
			serialPort.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("exiting !"); //$NON-NLS-1$
	}
}
