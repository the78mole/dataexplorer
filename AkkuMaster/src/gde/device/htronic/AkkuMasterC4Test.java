/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.device.htronic;

import gnu.io.SerialPort;

import java.util.Timer;
import java.util.TimerTask;

import osde.device.DeviceConfiguration;
import osde.serial.DeviceSerialPort;

/**
 * Class to test akkuMaster C4 device serial port communication
 * @author Winfried Brügmann
 */
public class AkkuMasterC4Test {

	static AkkuMasterC4SerialPort	akkuMaster;
	static int										counter	= 0;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DeviceConfiguration deviceConfig;
		try {
			deviceConfig = new DeviceConfiguration("c:/Documents and Settings/brueg/Application Data/OpenSerialDataExploroer/Devices/AkkumasterC4.xml");
			akkuMaster = new AkkuMasterC4SerialPort(deviceConfig, null);
			DeviceSerialPort.listConfiguredSerialPorts();
			SerialPort serialPort = akkuMaster.open();

			//akkuMaster.print(akkuMaster.getVersion());
			System.out.println();

			int delay = 10; // delay for 10 msec.
			int period = 10000; // repeat every 10 sec.
			Timer timer = new Timer();

			TimerTask timerTask = new TimerTask() {
				public void run() {
					try {
						//akkuMaster.print(akkuMaster.getData(AkkuMasterC4SerialPort.channel_1));
						
//						System.out.println("Ausgang 1");
//						akkuMaster.print(akkuMaster.getConfiguration(AkkuMasterC4SerialPort.channel_1));
//						akkuMaster.print(akkuMaster.getAdjustedValues(AkkuMasterC4SerialPort.channel_1));
//						akkuMaster.print(akkuMaster.getMeasuredValues(AkkuMasterC4SerialPort.channel_1));
//						System.out.println();
//
//						System.out.println("Ausgang 2");
//						akkuMaster.print(akkuMaster.getConfiguration(AkkuMasterC4SerialPort.channel_2));
//						akkuMaster.print(akkuMaster.getAdjustedValues(AkkuMasterC4SerialPort.channel_2));
//						akkuMaster.print(akkuMaster.getMeasuredValues(AkkuMasterC4SerialPort.channel_2));
//						System.out.println();
//
//						System.out.println("Ausgang 3");
//						akkuMaster.print(akkuMaster.getConfiguration(AkkuMasterC4SerialPort.channel_3));
//						akkuMaster.print(akkuMaster.getAdjustedValues(AkkuMasterC4SerialPort.channel_3));
//						akkuMaster.print(akkuMaster.getMeasuredValues(AkkuMasterC4SerialPort.channel_3));
//						System.out.println();
//
//						System.out.println("Ausgang 4");
//						akkuMaster.print(akkuMaster.getConfiguration(AkkuMasterC4SerialPort.channel_4));
//						akkuMaster.print(akkuMaster.getAdjustedValues(AkkuMasterC4SerialPort.channel_4));
//						akkuMaster.print(akkuMaster.getMeasuredValues(AkkuMasterC4SerialPort.channel_4));
//
//						System.out.println("counter = " + counter++);
					}
					catch (Exception e) {
						e.printStackTrace();
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
		System.out.println("fertig !");
	}
}
