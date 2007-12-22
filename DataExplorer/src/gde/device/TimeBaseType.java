package osde.device;

import org.w3c.dom.Element;

import osde.utils.XMLUtils;

public class TimeBaseType {
	private final String	name;
	private final String	symbol;
	private final String	unit;
	private int						timeStep;
	private Element 			domElement;

	/**
	 * constructs a TimeBase class using a XML DOM element
	 * @param DOM element
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
