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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * PropertyType class represents one element in the DeviceConfiguration DOM tree, refer to DeviceProperties style sheet
 * @author Winfried Brügmann
 */
public class PropertyType {
	private Logger							log						= Logger.getLogger(PropertyType.class.getName());
	
	public enum Types {Integer, Double, Boolean, String};
	private String name;
	private Types type;
	private Object value;
	private String description;
	private Types types; 
	
	private Element domElement;
	
	/**
	 * constructs a DataCalculation class using a XML DOM element
	 * @param element (DOM)
	 */
	public PropertyType(Element element) {
		this.domElement = element;
		this.name = element.getAttributes().getNamedItem("name").getNodeValue().toLowerCase();
		
		if (element.getAttributes().getNamedItem("type").getNodeValue().equals("Double"))
			this.type = PropertyType.Types.Double;
		else if (element.getAttributes().getNamedItem("type").getNodeValue().equals("Integer"))
			this.type = PropertyType.Types.Integer;
		else if (element.getAttributes().getNamedItem("type").getNodeValue().equals("Boolean"))
			this.type = PropertyType.Types.Boolean;
		else if (element.getAttributes().getNamedItem("type").getNodeValue().equals("String"))
			this.type = PropertyType.Types.String;
		
		this.value = element.getAttributes().getNamedItem("value").getNodeValue().replace(',', '.');
		Node tmpNode = element.getAttributes().getNamedItem("description");
		this.description = tmpNode != null ? tmpNode.getNodeValue() : null;
	}

	public PropertyType(Document document) {
		this.domElement = document.createElement("Property");
	}
	
	public PropertyType(Document document, String name, PropertyType.Types type, Object value, String description) {
		try {
			this.domElement = document.createElement("Property");
			this.domElement.setAttribute("name", name);
			this.domElement.setAttribute("type", type.toString());
			this.domElement.setAttribute("value", value.toString());
			this.domElement.setAttribute("description", description);
		}
		catch (DOMException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}

    this.name = name.toLowerCase();
		this.type = type;
		this.value = value.toString().replace(',', '.');
		this.description = description;
		
		log.fine("created : " + this.toString());
	}

	public PropertyType(Document document, String name, PropertyType.Types type, Object value) {
		try {
			this.domElement = document.createElement("Property");
			this.domElement.setAttribute("name", name);
			this.domElement.setAttribute("type", type.toString());
			this.domElement.setAttribute("value", value.toString());
		}
		catch (DOMException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}

		this.name = name.toLowerCase();
		this.type = type;
		this.value = value.toString().replace(',', '.');
		this.description = null;
		
		log.fine("created : " + this.toString());
	}

	public String toString() {
		if (this.description != null)
			return String.format("<Property name = %s, type = %s, value = %s, description = %s />", name, type, value, description);
		else
			return String.format("<Property name = %s, type = %s, value = %s />", name, type, value);
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
		this.domElement.setAttribute("name", name);
		this.name = name;
	}

	/**
	 * @return the type
	 */
	public PropertyType.Types getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(PropertyType.Types type) {
		this.domElement.setAttribute("type", type.toString());
		this.type = type;
	}

	/**
	 * @return the value
	 */
	public Object getValue() {
		this.types = this.type;
		switch (types) {
		case Integer:	return new Integer(this.value.toString());
		case Double:	return new Double(this.value.toString());
		case Boolean:	return new Boolean(this.value.toString());
		default:			return this.value.toString();
		}
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(Object value) {
		this.domElement.setAttribute("value", value.toString());
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
		this.domElement.setAttribute("description", description);
		this.description = description;
	}

	/**
	 * @return the domElement
	 */
	public Element getDomElement() {
		return domElement;
	}


}
