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

import java.util.Iterator;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * ChannelType class represents one element in the DeviceConfiguration DOM tree, refer to DeviceProperties style sheet
 * @author Winfried Brügmann
 */
public class ChannelType extends Vector<MeasurementType> {
	static final long							serialVersionUID	= 26031957L;

	public final static int				TYPE_OUTLET				= 0;
	public final static int				TYPE_CONFIG				= 1;

	private String								name;
	private final int							type;
	private final Vector<String>	measurementNames	= new Vector<String>();

	/**
	 * constructs a Channel class using a XML DOM element
	 * @param element (DOM)
	 */
	public ChannelType(Element element) {
		this.name = element.getAttributes().getNamedItem("name").getNodeValue();
		this.type = getChannelType(element.getAttributes().getNamedItem("type").getNodeValue());
		NodeList measurementNodeList = element.getElementsByTagName("Measurement");
		if (measurementNodeList != null && measurementNodeList.getLength() > 0) {
			for (int i = 0; i < measurementNodeList.getLength(); i++) {
				Element el = (Element) measurementNodeList.item(i);
				MeasurementType meas = new MeasurementType(el);
				this.add(meas);
				measurementNames.add(meas.getName());
			}
		}
	}

	public ChannelType(String name, int type, MeasurementType measurement) {
		this.name = name;
		this.type = type;
		this.add(measurement);
		measurementNames.add(measurement.getName());
	}

	public ChannelType(String name, int type, Vector<MeasurementType> measurements) {
		this.name = name;
		this.type = type;
		for (Iterator<MeasurementType> iterator = measurements.iterator(); iterator.hasNext();) {
			MeasurementType meas = iterator.next();
			this.add(meas);
			measurementNames.add(meas.getName());
		}
	}

	public String toString() {
		String lineSep = System.getProperty("line.separator");
		StringBuffer sb = new StringBuffer().append("<Channel>").append(lineSep);
		for (Iterator<MeasurementType> iterator = this.iterator(); iterator.hasNext();) {
			sb.append(iterator.next().toString()).append(lineSep);
		}
		return sb.toString();
	}

	/**
	 * @return the measurementNames
	 */
	public Vector<String> getMeasurementNames() {
		return measurementNames;
	}

	private int getChannelType(String key) {
		int result = 0;
		if (key.equals("TYPE_OUTLET"))
			result = TYPE_OUTLET;
		else if (key.equals("TYPE_CONFIG")) result = TYPE_CONFIG;
		return result;
	}

	/**
	 * @return the type
	 */
	public int getType() {
		return type;
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
}
