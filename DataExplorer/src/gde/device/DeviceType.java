package osde.device;

import org.w3c.dom.Element;

import osde.utils.XMLUtils;

public class DeviceType {
	private String				name;
	private final String	manufacturer;
	private final String	manufacturerURL;
	private final String	group;
	private final String	image;
	private boolean				usage;

	private Element				domElement;

	/**
	 * constructs a Device class using a XML DOM element
	 * @param DOM element
	 */
	public DeviceType(Element element) {
		this.domElement = element;
		this.name = XMLUtils.getTextValue(element, "name");
		this.manufacturer = XMLUtils.getTextValue(element, "manufacturer");
		this.manufacturerURL = XMLUtils.getTextValue(element, "manufacturerURL");
		this.group = XMLUtils.getTextValue(element, "group");
		this.image = XMLUtils.getTextValue(element, "image");
		this.usage = XMLUtils.getBooleanValue(element, "usage");
	}

	public DeviceType(String name, String manufacturer, String manufacturerURL, String group, String image, boolean usage) {
		this.name = name;
		this.manufacturer = manufacturer;
		this.manufacturerURL = manufacturerURL;
		this.group = group;
		this.image = image;
		this.usage = usage;
	}

	public String toString() {
		return String.format("<Device> name = %s, manufacturer = %s, manufacturerURL = %s, image = %s, usage = %s", name, manufacturer, manufacturerURL, image, usage);
	}

	/**
	 * @return the usage
	 */
	public boolean isUsage() {
		return usage;
	}

	/**
	 * @param usage the usage to set
	 */
	public void setUsage(boolean usage) {
		XMLUtils.setBooleanValue(domElement, "usage", usage);
		this.usage = usage;
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
		XMLUtils.setStringValue(domElement, "name", name);
		this.name = name;
	}

	/**
	 * @return the manufacturer
	 */
	public String getManufacturer() {
		return manufacturer;
	}

	/**
	 * @return the manufacturerURL
	 */
	public String getManufacturerURL() {
		return manufacturerURL;
	}

	/**
	 * @return the image
	 */
	public String getImage() {
		return image;
	}

	/**
	 * @return the group
	 */
	public String getGroup() {
		return group;
	}
}
