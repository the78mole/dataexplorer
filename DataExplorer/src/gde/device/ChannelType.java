package osde.device;

import java.util.Iterator;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ChannelType extends Vector<MeasurementType> {
	static final long							serialVersionUID	= 26031957L;

	private final Vector<String>	measurementNames	= new Vector<String>();

	/**
	 * constructs a Channel class using a XML DOM element
	 * @param DOM element
	 */
	public ChannelType(Element element) {
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

	public ChannelType(MeasurementType measurement) {
		this.add(measurement);
		measurementNames.add(measurement.getName());
	}

	public ChannelType(Vector<MeasurementType> measurements) {
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
}
