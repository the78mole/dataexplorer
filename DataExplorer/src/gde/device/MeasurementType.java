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
import org.w3c.dom.NodeList;

import osde.utils.XMLUtils;

/**
 * MeasurementType class represents one element in the DeviceConfiguration DOM tree, refer to DeviceProperties style sheet
 * @author Winfried Brügmann
 */
public class MeasurementType {
	private final String				name;
	private final String				symbol;
	private String							unit;
	private boolean							isActive;
	private boolean							isCalculation = false;
	private DataCalculationType	dataCalculation;

	/**
	 * constructs a Measurement class using a XML DOM element
	 * @param element (DOM)
	 */
	public MeasurementType(Element element) {
		this.name = XMLUtils.getTextValue(element, "name");
		this.symbol = XMLUtils.getTextValue(element, "symbol");
		this.unit = XMLUtils.getTextValue(element, "unit");
		
		if (XMLUtils.getTextValue(element, "isActive") != null)
			this.isActive = XMLUtils.getBooleanValue(element, "isActive"); // optional element
		else 
			this.isCalculation = true;

		this.dataCalculation = new DataCalculationType(); // optional element
		NodeList timeBaseNodeList = element.getElementsByTagName("DataCalculation");
		if (timeBaseNodeList != null && timeBaseNodeList.getLength() > 0) {
			Element el = (Element) timeBaseNodeList.item(0);
			this.dataCalculation = new DataCalculationType(el);
		}
	}

	public MeasurementType(String name, String symbol, String unit, boolean isActive, boolean isCalculation) {
		this.name = name;
		this.symbol = symbol;
		this.unit = unit;
		this.isActive = isActive;
		this.isCalculation = isCalculation;
		this.dataCalculation = new DataCalculationType();
	}

	public String toString() {
		if (isCalculation)
			return String.format("<Measurement> name = %s, symbol = %s, unit = %s, isCalculation = %b, %s", name, symbol, unit, isCalculation, dataCalculation.toString());
		else
			return String.format("<Measurement> name = %s, symbol = %s, unit = %s, isActive = %b, %s", name, symbol, unit, isActive, dataCalculation.toString());
	}

	/**
	 * @return the unit
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * @param unit the unit to set
	 */
	public void setUnit(String unit) {
		this.unit = unit;
	}

	/**
	 * @return the isActive
	 */
	public boolean isActive() {
		return isActive;
	}

	/**
	 * @param isActive the isActive to set
	 */
	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	/**
	 * @return the dataCalculation
	 */
	public DataCalculationType getDataCalculation() {
		return dataCalculation;
	}

	/**
	 * @param dataCalculation the dataCalculation to set
	 */
	public void setDataCalculation(DataCalculationType dataCalculation) {
		this.dataCalculation = dataCalculation;
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
	 * @return the isCalculation
	 */
	public boolean isCalculation() {
		return isCalculation;
	}
}
