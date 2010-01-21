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
package osde.device.pichler;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import osde.device.DeviceConfiguration;
import osde.device.bantam.eStationBC6;

/**
 * Pichler P6 device class which is 100% eStation BC6
 * @author Winfried Brügmann
 */
public class PichlerP6 extends eStationBC6 {

	/**
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public PichlerP6(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * @param deviceConfig
	 */
	public PichlerP6(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

}
