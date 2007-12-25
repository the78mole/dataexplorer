package osde.device;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import osde.utils.XMLUtils;

public class MeasurementType {
	private final String				name;
	private final String				symbol;
	private String							unit;
	private boolean							isActive;
	private DataCalculationType	dataCalculation;

	/**
	 * constructs a Measurement class using a XML DOM element
	 * @param DOM element
	 */
	public MeasurementType(Element element) {
		this.name = XMLUtils.getTextValue(element, "name");
		this.symbol = XMLUtils.getTextValue(element, "symbol");
		this.unit = XMLUtils.getTextValue(element, "unit");
		
		this.isActive = XMLUtils.getBooleanValue(element, "isActive"); // optional element

		this.dataCalculation = new DataCalculationType(); // optional element
		NodeList timeBaseNodeList = element.getElementsByTagName("DataCalculation");
		if (timeBaseNodeList != null && timeBaseNodeList.getLength() > 0) {
			Element el = (Element) timeBaseNodeList.item(0);
			this.dataCalculation = new DataCalculationType(el);
		}
	}

	public MeasurementType(String name, String symbol, String unit, boolean isActive) {
		this.name = name;
		this.symbol = symbol;
		this.unit = unit;
		this.isActive = isActive;
		this.dataCalculation = new DataCalculationType();
	}

	public String toString() {
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
}
