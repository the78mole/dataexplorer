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

import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import osde.utils.XMLUtils;

/**
 * MeasurementType class represents one element in the DeviceConfiguration DOM tree, refer to DeviceProperties style sheet
 * @author Winfried Brügmann
 */
public class MeasurementType {
	private String							name;
	private String							symbol;
	private String							unit;
	private boolean							isActive;
	private boolean							isCalculation	= false;

	private Element						domElement;

	private static HashMap<String, PropertyType> properties = new HashMap<String, PropertyType>();

	/**
	 * constructs a Measurement class using a XML DOM element
	 * @param element (DOM)
	 */
	public MeasurementType(Element element) {
		this.domElement = element;
		this.name = XMLUtils.getTextValue(element, "name");
		this.symbol = XMLUtils.getTextValue(element, "symbol");
		this.unit = XMLUtils.getTextValue(element, "unit");
		
		if (XMLUtils.getTextValue(element, "isActive") != null)
			this.isActive = XMLUtils.getBooleanValue(element, "isActive"); // optional element
		else 
			this.isCalculation = true;
	}

	public String toString() {
		return String.format("<Measurement> name = %s, symbol = %s, unit = %s, isActive = %b", name, symbol, unit, isActive);
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
		XMLUtils.setStringValue(this.domElement, "unit", unit);
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
		XMLUtils.setBooleanValue(this.domElement, "isActive", isActive);
		this.isActive = isActive;
	}

	public PropertyType get(String key) {
		return properties.get(key);
	}
		
	public void addProperty(String key, PropertyType property) {
		properties.put(key, property);
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

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		XMLUtils.setStringValue(this.domElement, "name", name);
		this.name = name;
	}

	/**
	 * @param symbol the symbol to set
	 */
	public void setSymbol(String symbol) {
		XMLUtils.setStringValue(this.domElement, "symbol", symbol);
		this.symbol = symbol;
	}

	/**
	 * @return the domElement
	 */
	public Element getDomElement() {
		return domElement;
	}

	/**
	 * @param domElement the domElement to set
	 */
	public void setDomElement(Element domElement) {
		this.domElement = domElement;
	}
}
