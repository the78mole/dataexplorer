/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.device.renschler;

import gnu.io.SerialPort;
import osde.device.DeviceConfiguration;
import osde.serial.DeviceSerialPort;

/**
 * Test class for PicolarioSerialPort
 * @author Winfried Brügmann
 */
public class PicolarioSerialPortTest {

	static PicolarioSerialPort	picolario;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DeviceConfiguration deviceConfig;
		try {
			deviceConfig = new DeviceConfiguration("c:/Documents and Settings/brueg/Application Data/OpenSerialDataExploroer/Devices/Picolario.xml"); //$NON-NLS-1$
			picolario = new PicolarioSerialPort(deviceConfig, null);
			DeviceSerialPort.listConfiguredSerialPorts();

			SerialPort serialPort = picolario.open();

			picolario.readNumberAvailableRecordSets();
			//picolario.print(picolario.getData(1, null));
			System.out.println();

			serialPort.close();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("fertig !"); //$NON-NLS-1$
	}

}
