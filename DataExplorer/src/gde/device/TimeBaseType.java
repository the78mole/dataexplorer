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
 * @author Winfried Brügmann
 * TimeBaseType class represents one element in the DeviceConfiguration DOM tree, refer to DeviceProperties style sheet
 */
public class TimeBaseType {
	private final String	name;
	private final String	symbol;
	private final String	unit;
	private int						timeStep;
	private Element 			domElement;

	/**
	 * constructs a TimeBase class using a XML DOM element
	 * @param element (DOM)
	 */
	public TimeBaseType(Element element) {
		this.name = XMLUtils.getTextValue(element, "name");
		this.symbol = XMLUtils.getTextValue(element, "symbol");
		this.unit = XMLUtils.getTextValue(element, "unit");
		this.timeStep = XMLUtils.getIntValue(element, "timeStep");
		this.domElement = element;
	}

	public TimeBaseType(String name, String symbol, String unit, int timeStep) {
		this.name = name;
		this.symbol = symbol;
		this.unit = unit;
		this.timeStep = timeStep;
	}

	public String toString() {
		return String.format("<TimeBase> name = %s, symbol = %s, unit = %s, timeStep[ms] = %d", name, symbol, unit, timeStep);
	}

	/**
	 * @return the timeStep
	 */
	public int getTimeStep() {
		return timeStep;
	}

	/**
	 * @param timeStep the timeStep to set
	 */
	public void setTimeStep(int timeStep) {
		XMLUtils.setIntValue(domElement, "timeStep", timeStep);
		this.timeStep = timeStep;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the symbol
	 */
	public String getSymbol() {
		return symbol;
	}

	/**
	 * @return the unit
	 */
	public String getUnit() {
		return unit;
	}
}
