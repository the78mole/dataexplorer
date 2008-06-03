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
package osde.device.wb;

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import osde.device.DeviceConfiguration;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;

/**
 * Dummy serial port implementation for the simulator device
 * @author Winfried Brügmann
 */
public class SimulatorSerialPort extends DeviceSerialPort {

	int	lastRecord	= -1;
	int	lastVoltage	= 0;
	int	lastCurrent	= 0;
	int	xBound			= 0;

	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public SimulatorSerialPort(DeviceConfiguration currentDeviceConfig, OpenSerialDataExplorer currentApplication) {
		super(currentDeviceConfig, currentApplication);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return map containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public HashMap<String, Object> getData(int recordNumber, String channelConfigKey) throws Exception {
		if (recordNumber != this.lastRecord) {
			this.lastRecord = recordNumber;
			this.lastVoltage = 0;
			this.lastCurrent = 0;
			this.xBound = 0;
		}

		HashMap<String, Object> data = new HashMap<String, Object>();
		Vector<Integer> voltage = new Vector<Integer>();
		Vector<Integer> current = new Vector<Integer>();

		//int xBound = new Double(Math.random() * 1000000).intValue();
		this.xBound = this.xBound + 100;
		this.lastVoltage = this.xBound / 2;
		int yBound = this.deviceConfig.getDataBlockSize();
		for (int i = 0; i < this.deviceConfig.getDataBlockSize(); i++) {
			current.add(i * 3000 + this.lastCurrent);
			voltage.add(getNormalizedSine(i, this.xBound / 2, yBound) - this.lastVoltage);
		}
		this.lastCurrent = yBound * 3000;

		String[] measurements = this.deviceConfig.getMeasurementNames(channelConfigKey); // 0=Spannung, 1=Strom
		data.put(measurements[0], voltage);
		data.put(measurements[1], current);
		return data;
	}

	/**
	 * calculates the sine value
	 * @param x the value along the x-axis
	 * @param halfY the value of the y-axis
	 * @param maxX the width of the x-axis
	 * @return int
	 */
	int getNormalizedSine(int x, int halfY, int maxX) {
		double piDouble = 2 * Math.PI;
		double factor = piDouble / maxX;
		return (int) (Math.sin(x * factor) * halfY + halfY);
	}
}
