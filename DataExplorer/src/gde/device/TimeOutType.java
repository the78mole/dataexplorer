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
package osde.device;

import org.w3c.dom.Element;

import osde.utils.XMLUtils;

/**
 * TimeOutType class represents one element in the DeviceConfiguration DOM tree, refer to DeviceProperties style sheet
 * @author Winfried Brügmann
 */
public class TimeOutType {
	private final int	RTOCharDelayTime;
	private final int	RTOExtraDelayTime;
	private final int	WTOCharDelayTime;
	private final int	WTOExtraDelayTime;

	/**
	 * constructs a TimeOut class using a XML DOM element
	 * @param element (DOM)
	 */
	public TimeOutType(Element element) {
		this.RTOCharDelayTime = XMLUtils.getIntValue(element, "RTOCharDelayTime");
		this.RTOExtraDelayTime = XMLUtils.getIntValue(element, "RTOExtraDelayTime");
		this.WTOCharDelayTime = XMLUtils.getIntValue(element, "WTOCharDelayTime");
		this.WTOExtraDelayTime = XMLUtils.getIntValue(element, "WTOExtraDelayTime");
	}

	public TimeOutType() {
		this.RTOCharDelayTime = 0;
		this.RTOExtraDelayTime = 0;
		this.WTOCharDelayTime = 0;
		this.WTOExtraDelayTime = 0;
	}

	public TimeOutType(int RTOCharDelayTime, int RTOExtraDelayTime, int WTOCharDelayTime, int WTOExtraDelayTime) {
		this.RTOCharDelayTime = RTOCharDelayTime;
		this.RTOExtraDelayTime = RTOExtraDelayTime;
		this.WTOCharDelayTime = WTOCharDelayTime;
		this.WTOExtraDelayTime = WTOExtraDelayTime;
	}

	public String toString() {
		return String.format("RTOCharDelayTime = %d, RTOExtraDelayTime = %d, WTOCharDelayTime = %d, WTOExtraDelayTime = %d", RTOCharDelayTime, RTOExtraDelayTime, WTOCharDelayTime, WTOExtraDelayTime);
	}

	/**
	 * @return the rTOCharDelayTime
	 */
	public int getRTOCharDelayTime() {
		return RTOCharDelayTime;
	}

	/**
	 * @return the rTOExtraDelayTime
	 */
	public int getRTOExtraDelayTime() {
		return RTOExtraDelayTime;
	}

	/**
	 * @return the wTOCharDelayTime
	 */
	public int getWTOCharDelayTime() {
		return WTOCharDelayTime;
	}

	/**
	 * @return the wTOExtraDelayTime
	 */
	public int getWTOExtraDelayTime() {
		return WTOExtraDelayTime;
	}
}
