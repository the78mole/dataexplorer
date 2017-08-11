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
    
    Copyright (c) 2017 Winfried Bruegmann
****************************************************************************************/
package gde.device.elprog;

import gde.comm.DeviceCommPort;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.ui.DataExplorer;

/**
 * @author brueg
 *
 */
public class PulsarSerialPort extends DeviceCommPort {

	/**
	 * @param currentDevice
	 * @param currentApplication
	 */
	public PulsarSerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param deviceConfiguration
	 */
	public PulsarSerialPort(DeviceConfiguration deviceConfiguration) {
		super(deviceConfiguration);
		// TODO Auto-generated constructor stub
	}

}
