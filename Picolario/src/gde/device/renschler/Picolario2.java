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
    
    Copyright (c) 2013 Winfried Bruegmann
****************************************************************************************/
package gde.device.renschler;

import gde.device.DeviceConfiguration;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

/**
 * Picolario2 device main implementation class
 * @author Winfried Brügmann
 */
public class Picolario2 extends Picolario {

	/**
	 * @param iniFile
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public Picolario2(String iniFile) throws FileNotFoundException, JAXBException {
		super(iniFile);
	}
	

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public Picolario2(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}
}
