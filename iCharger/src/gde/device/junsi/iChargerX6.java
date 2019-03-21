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
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2019 Winfried Bruegmann
****************************************************************************************/
package gde.device.junsi;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import gde.GDE;
import gde.device.DeviceConfiguration;

/**
 * Junsi iCharger X6 device class
 * @author Winfried Brügmann
 */
public class iChargerX6 extends iChargerUsb {

	protected enum BatteryTypesX { //is different to iChargerUSB.BatteryTypesDuo
		BT_UNKNOWN("?"), BT_LIPO("LiPo"), BT_LIIO("LiIo"), BT_LIFE("LiFe"), BT_LIHV("LiHv"), BT_LTO("LTO"), BT_NIMH("NiMH"), BT_NICD("NiCd"), BT_NIZN("NiZn"), BT_PB("PB"), BT_POWER("Power"), BT_USER("User"), BT_UNKNOWN_("?");

		private String value;
		
		private BatteryTypesX(String newValue) {
			value = newValue;
		}
		
		protected String getName() {
			return value;
		}
		
		protected static String[] getValues() {
			StringBuilder sb = new StringBuilder();
			for (BatteryTypesX bt : BatteryTypesX.values()) 
				sb.append(bt.value).append(GDE.CHAR_CSV_SEPARATOR);
			return sb.toString().split(GDE.STRING_CSV_SEPARATOR);
		}
	};
		
	/**
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public iChargerX6(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		this.BATTERIE_TYPE = BatteryTypesX.getValues(); 
	}

	/**
	 * @param deviceConfig
	 */
	public iChargerX6(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		this.BATTERIE_TYPE = BatteryTypesX.getValues(); 
	}

	/**
	 * query number of Lithium cells of this charger device
	 * @return
	 */
	@Override
	public int getNumberOfLithiumCells() {
		return 6;
	}
}
