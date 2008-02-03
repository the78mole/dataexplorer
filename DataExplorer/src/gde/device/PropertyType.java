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
import org.w3c.dom.Node;

/**
 * PropertyType class represents one element in the DeviceConfiguration DOM tree, refer to DeviceProperties style sheet
 * @author Winfried Brügmann
 */
public class PropertyType {
	
	private String name;
	private String type;
	private Object value;
	private String description;
	public enum Types {Integer, Double, Boolean};
	private Types types; 
	
	/**
	 * constructs a DataCalculation class using a XML DOM element
	 * @param element (DOM)
	 */
	public PropertyType(Element element) {
		this.name = element.getAttributes().getNamedItem("name").getNodeValue().toLowerCase();
		this.type = element.getAttributes().getNamedItem("type").getNodeValue();
		this.value = element.getAttributes().getNamedItem("value").getNodeValue().replace(',', '.');
		Node tmpNode = element.getAttributes().getNamedItem("description");
		this.description = tmpNode != null ? tmpNode.getNodeValue() : null;
	}

	public PropertyType(String name, String type, Object value, String description) {
		this.name = name.toLowerCase();
		this.type = type;
		this.value = ((String)value).replace(',', '.');
		this.description = description;
	}

	public PropertyType(String name, String type, Object value) {
		this.name = name.toLowerCase();
		this.type = type;
		this.value = ((String)value).replace(',', '.');
		this.description = null;
	}

	public String toString() {
		if (this.description != null)
			return String.format("<Property> name = %s, type = %s, value = %s, description = %s", name, type, value, description);
		else
			return String.format("<Property> name = %s, type = %s, value = %s", name, type, value);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the value
	 */
	public Object getValue() {
		switch (types) {
		case Integer:	return new Integer((String)this.value);
		case Double:	return new Double((String)this.value);
		case Boolean:	return new Boolean((String)this.value);
		default:			return (String)this.value;
		}
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}


}
