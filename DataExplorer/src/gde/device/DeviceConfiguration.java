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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import osde.log.LogFormatter;
import osde.utils.XMLUtils;

/**
 * Device Configuration class makes the parsed DeviceProperties XML accessible for the application
 * @author Winfried Brügmann
 */
public class DeviceConfiguration {
	private final static Logger									log												= Logger.getLogger(DeviceSerialPort.class.getName());

	private final DeviceType										device;
	private final SerialPortType								serialPort;
	private final TimeBaseType									timeBase;
	private final HashMap<Integer, ChannelType>	channels									= new HashMap<Integer, ChannelType>();
	private Document														doc;
	private File																xmlFile;
	private boolean															isChangePropery						= false;

	public final static int											DEVICE_TYPE_CHARGER				= 1;
	public final static int											DEVICE_TYPE_LOGGER				= 2;
	public final static int											DEVICE_TYPE_BALANCER			= 3;
	public final static int											DEVICE_TYPE_CURRENT_SINK	= 4;
	public final static int											DEVICE_TYPE_POWER_SUPPLY	= 5;
	public final static int											DEVICE_TYPE_GPS						= 5;
	public final static int											DEVICE_TYPE_RECEIVER			= 7;
	public final static int											DEVICE_TYPE_MULTIMETER		= 8;

	/**
	 * method to test this class
	 * @param args
	 */
	public static void main(String[] args) {
		Handler ch = new ConsoleHandler();
		LogFormatter lf = new LogFormatter();
		ch.setFormatter(lf);
		ch.setLevel(Level.ALL);
		Logger.getLogger("").addHandler(ch);
		Logger.getLogger("").setLevel(Level.ALL);

		//String basePath = "C:/Documents and Settings/brueg/Application Data/OpenSerialDataExplorer/Devices/";
		String basePath = "D:/Belchen2/workspaces/test/OpenSerialDataExplorer/doc/";

		try {
			DeviceConfiguration devProp;
			devProp = new DeviceConfiguration(basePath + "Picolario.xml");
			devProp.setName("Picolario");
			devProp.setUsed(false);
			devProp.setPort("COM2");
			devProp.setDataBlockSize(32);
			devProp.store();

			new DeviceConfiguration(basePath + "AkkuMasterC4.xml").store();
			new DeviceConfiguration(basePath + "UniLog.xml").store();
			new DeviceConfiguration(basePath + "Simulator.xml").store();
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (ParserConfigurationException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (SAXException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public DeviceConfiguration(String xmlFilePath) throws ParserConfigurationException, SAXException, IOException {

		if (!(this.xmlFile = new File(xmlFilePath)).exists()) throw new FileNotFoundException("Die Gerätedatei wurde nicht gefunden - " + xmlFilePath);

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		//parse using builder to get DOM representation of the XML file
		this.doc = db.parse(this.xmlFile);
		//get the DOM document root element
		Element docEle = doc.getDocumentElement();

		//get a nodelist of <Device> element
		DeviceType dev = null;
		NodeList deviceNodeList = docEle.getElementsByTagName("Device");
		if (deviceNodeList != null && deviceNodeList.getLength() > 0) {
			Element el = (Element) deviceNodeList.item(0);
			dev = new DeviceType(el);
		}
		else
			throw new SAXException("<Device> Element fehlerhaft in " + xmlFilePath);
		this.device = dev;

		//get a nodelist of <SerialPort> element
		SerialPortType sp = null;
		NodeList serialPortNodeList = docEle.getElementsByTagName("SerialPort");
		if (serialPortNodeList != null && serialPortNodeList.getLength() > 0) {
			Element el = (Element) serialPortNodeList.item(0);
			sp = new SerialPortType(el);
		}
		else
			throw new SAXException("<SerialPort> Element fehlerhaft in " + xmlFilePath);
		this.serialPort = sp;

		//get a node list of <TimeBase> element
		TimeBaseType tb = null;
		NodeList timeBaseNodeList = docEle.getElementsByTagName("TimeBase");
		if (timeBaseNodeList != null && timeBaseNodeList.getLength() > 0) {
			Element el = (Element) timeBaseNodeList.item(0);
			tb = new TimeBaseType(el);
		}
		else
			throw new SAXException("<TimeBase> Element fehlerhaft in " + xmlFilePath);
		this.timeBase = tb;

		//get a node list of <Channel> elements
		NodeList channelNodeList = docEle.getElementsByTagName("Channel");
		if (channelNodeList != null && channelNodeList.getLength() > 0) {
			// loop through the list if more than one channel
			for (int i = 0; i < channelNodeList.getLength(); i++) {
				Element cElm = (Element) channelNodeList.item(i);
				ChannelType channel = new ChannelType(cElm);
				channels.put((i + 1), channel);
				log.info("add channel name = " + channel.getName());
				// get node list of measurements as child of channel
				NodeList measurementNodeList = cElm.getElementsByTagName("Measurement");
				if (measurementNodeList != null && measurementNodeList.getLength() > 0) {
					for (int j = 0; j < measurementNodeList.getLength(); j++) {
						Element mElm = (Element) measurementNodeList.item(j);
						MeasurementType meas = new MeasurementType(mElm);
						channel.add(meas);
						channel.addMeasurementName(meas.getName());
						log.info("channel.addMeasurementName = " + meas.getName());
						
						// get node list of data calculation properties as child of measurement - optional
						NodeList propNodeList = mElm.getElementsByTagName("Property");
						if (propNodeList != null && propNodeList.getLength() > 0) {
							for (int k = 0; k < propNodeList.getLength(); k++) {
								Element pElm = (Element) propNodeList.item(k);
								PropertyType prop = new PropertyType(pElm);
								meas.addProperty(prop.getName(), prop);
								log.info("add property to measurement id = " + prop.toString());
							}
						}
					}
				}
				else 
					throw new SAXException("<Measurement> Element fehlerhaft in " + xmlFilePath);
			}
		}
		else
			throw new SAXException("<Channel> Element fehlerhaft in " + xmlFilePath);
		
		if (log.isLoggable(Level.FINE)) log.fine(this.toString());
		
		if (log.isLoggable(Level.INFO)) XMLUtils.writeXML2Console(this.doc);
		
	}

	/**
	 * copy constructor
	 */
	public DeviceConfiguration(DeviceConfiguration deviceConfig) {
		this.device = new DeviceType((Element) deviceConfig.doc.getElementsByTagName("Device").item(0));
		this.serialPort = new SerialPortType((Element) deviceConfig.doc.getElementsByTagName("SerialPort").item(0));
		this.timeBase = new TimeBaseType((Element) deviceConfig.doc.getElementsByTagName("TimeBase").item(0));
		for (int i = 1; i <= deviceConfig.channels.size(); ++i) {
			this.channels.put(i, deviceConfig.channels.get(i));
		}
		this.doc = (Document) deviceConfig.doc.cloneNode(true);
		this.xmlFile = deviceConfig.xmlFile;
		this.isChangePropery = deviceConfig.isChangePropery;

		if (log.isLoggable(Level.FINE)) log.fine(this.toString());

	}

	public DeviceConfiguration(DeviceType device, SerialPortType serialPort, TimeBaseType timeBase, HashMap<String, ChannelType> channels) {
		this.device = device;
		this.serialPort = serialPort;
		this.timeBase = timeBase;
		for (int i = 1; i <= channels.size(); ++i) {
			this.channels.put(i, channels.get(i));
		}

		if (log.isLoggable(Level.FINE)) log.fine(this.toString());
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		String lineSep = System.getProperty("line.separator");
		sb.append("<DeviceProperties>").append(lineSep);
		sb.append(device.toString()).append(lineSep);
		sb.append(serialPort.toString()).append(lineSep);
		sb.append(timeBase.toString()).append(lineSep);
		sb.append(channels.toString()).append(lineSep);
		return sb.toString();
	}

	/**
	 * writes updated device properties XML
	 */
	public void store() {
		if (isChangePropery) {
			try {
				XMLUtils.writeXmlFile(doc, this.xmlFile.getCanonicalPath());
			}
			catch (Throwable e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * @return the device
	 */
	public DeviceType getDeviceType() {
		return device;
	}

	/**
	 * @return the serialPort
	 */
	public SerialPortType getSerialPortType() {
		return serialPort;
	}

	/**
	 * @return the timeBase
	 */
	public TimeBaseType getTimeBaseType() {
		return timeBase;
	}

	/**
	 * @return the map of measurements(records) definitions
	 */
	public HashMap<Integer, ChannelType> getChannelTypes() {
		return channels;
	}

	/**
	 * @return the channel 1 to n
	 */
	public ChannelType getChannel(int number) {
		return channels.get(number);
	}

	public boolean isUsed() {
		return this.device.isUsage();
	}

	public void setUsed(boolean value) {
		this.isChangePropery = true;
		this.device.setUsage(value);
	}

	public String getPropertiesFileName() {
		return this.xmlFile.getAbsolutePath();
	}

	public String getName() {
		return this.device.getName();
	}

	public void setName(String newName) {
		this.isChangePropery = true;
		this.device.setName(newName);
	}

	public String getManufacturer() {
		return this.device.getManufacturer();
	}

	public String getManufacturerURL() {
		return this.device.getManufacturerURL();
	}

	public String getDeviceGroup() {
		return this.device.getGroup();
	}

	public String getImageFileName() {
		return this.device.getImage();
	}

	public int getTimeStep_ms() {
		return this.timeBase.getTimeStep();
	}

	public void setTimeStep_ms(int newTimeStep_ms) {
		this.isChangePropery = true;
		this.timeBase.setTimeStep(newTimeStep_ms);
	}

	public String getPort() {
		return this.serialPort.getPort();
	}

	public void setPort(String newPort) {
		this.isChangePropery = true;
		this.serialPort.setPort(newPort);
	}

	public int getBaudeRate() {
		return this.serialPort.getBaudeRate();
	}

	public int getDataBits() {
		return this.serialPort.getDataBits();
	}

	public int getStopBits() {
		return this.serialPort.getStopBits();
	}

	public int getFlowCtrlMode() {
		return this.serialPort.getFlowControlMode();
	}

	public int getParity() {
		return this.serialPort.getParity();
	}

	public int getDataBlockSize() {
		return this.serialPort.getDataBlock().getSize();
	}

	public void setDataBlockSize(int newSize) {
		this.isChangePropery = true;
		this.serialPort.getDataBlock().setSize(newSize);
	}

	public byte getEndingByte() {
		return this.serialPort.getDataBlock().getEndingByte();
	}

	public boolean isDTR() {
		return this.serialPort.isDTR();
	}

	public boolean isRTS() {
		return this.serialPort.isRTS();
	}

	/**
	 * method to query the unit of measurement data by a given record key
	 * @param recordKey
	 * @return dataUnit as string
	 */
	public String getDataUnit(String configKey, String recordKey) {
		return this.getMeasurementDefinition(configKey, recordKey.split("_")[0]).getUnit();
	}

	/**
	 * @return the channel count
	 */
	public int getChannelCount() {
		return channels.size();
	}

	/**
	 * @return the channel name
	 */
	public String getChannelName(int channelNumber) {
		return channels.get(channelNumber).getName();
	}

	/**
	 * @param channelName - size should not exceed 15 char length
	 * @param channelNumber
	 * @return the channel name
	 */
	public void setChannelName(String channelName, int channelNumber) {
		this.isChangePropery = true;
		channels.get(channelNumber).setName(channelName);
	}

	/**
	 * @return the channel type
	 */
	public int getChannelType(int channelNumber) {
		return channels.get(channelNumber).getType();
	}
	
	/**
	 * @return the number of measurements of a channel, assuming all channels have at least identical number of measurements
	 */
	public int getNumberOfMeasurements() {
		return channels.get(1).size();
	}

	/**
	 * set active status of an measurement
	 * @param channelKey
	 * @param measurementKey
	 * @param isActive
	 */
	public void setMeasurementActive(String channelKey, String measurementKey, boolean isActive) {
		log.info("channelKey = \"" + channelKey + "\" measurementKey = \"" + measurementKey + "\"");
		this.isChangePropery = true;
		Element element = this.doc.getDocumentElement();
		NodeList channelNodeList = element.getElementsByTagName("Channel");
		if (channelNodeList != null && channelNodeList.getLength() > 0) {
			for (int i = 0; i < channelNodeList.getLength(); i++) {
				Element el = (Element) channelNodeList.item(i);
				String foundKey = el.getAttribute("name");
				if (channelKey.equals(foundKey)) {
					NodeList measurementNodeList = el.getElementsByTagName("Measurement");
					if (measurementNodeList != null && measurementNodeList.getLength() > 0) {
						for (int j = 0; j < measurementNodeList.getLength(); j++) {
							Element elm = (Element) measurementNodeList.item(j);
							foundKey = XMLUtils.getTextValue(elm, "name");
							if (measurementKey.equals(foundKey)) {
								XMLUtils.setBooleanValue(elm, "isActive", isActive);
								this.getMeasurementDefinition(channelKey, measurementKey).setActive(isActive);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * set new name of specified measurement
	 * @param channelKey
	 * @param measurementKey
	 * @param name
	 */
	public void setMeasurementName(String channelKey, String measurementKey, String name) {
		log.info("channelKey = \"" + channelKey + "\" measurementKey = \"" + measurementKey + "\"");
		this.isChangePropery = true;
		this.getMeasurementDefinition(channelKey, measurementKey).setName(name);
	}
	
	/**
	 * set new unit of specified measurement
	 * @param channelKey
	 * @param measurementKey
	 * @param unit
	 */
	public void setMeasurementUnit(String channelKey, String measurementKey, String unit) {
		log.info("channelKey = \"" + channelKey + "\" measurementKey = \"" + measurementKey + "\"");
		this.isChangePropery = true;
		this.getMeasurementDefinition(channelKey, measurementKey).setUnit(unit);
	}
	
	/**
	 * set new symbol of specified measurement
	 * @param channelKey
	 * @param measurementKey
	 * @param symbol
	 */
	public void setMeasurementSymbol(String channelKey, String measurementKey, String symbol) {
		this.isChangePropery = true;
		this.getMeasurementDefinition(channelKey, measurementKey).setSymbol(symbol);
	}
	
	/**
	 * @return the measurement definitions matching key (voltage, current, ...)
	 */
	public MeasurementType getMeasurementDefinition(String channelKey, String measurementKey) {
		MeasurementType measurementDefinition = null;
//		Element measurementElement = null;
//		Element element = this.doc.getDocumentElement();
//		NodeList channelNodeList = element.getElementsByTagName("Channel");
//		if (channelNodeList != null && channelNodeList.getLength() > 0) {
//			for (int i = 0; i < channelNodeList.getLength(); i++) {
//				Element el = (Element) channelNodeList.item(i);
//				String foundKey = el.getAttribute("name");
//				if (channelKey.equals(foundKey)) {
//					NodeList measurementNodeList = el.getElementsByTagName("Measurement");
//					if (measurementNodeList != null && measurementNodeList.getLength() > 0) {
//						for (int j = 0; j < measurementNodeList.getLength(); j++) {
//							Element elm = (Element) measurementNodeList.item(j);
//							foundKey = XMLUtils.getTextValue(elm, "name");
//							if (measurementKey.equals(foundKey)) {
//								measurementElement = elm;
//								break;
//							}
//						}
//					}
//				}
//			}
//		}
		// loop through the channels and search for one matching channelKey
		ChannelType channel = null;
		for (int i = 1; i <= channels.size(); i++) {
			channel = channels.get(i);
			if(channel.getName().equals(channelKey)) break;
		} 
		// loop through channel and search for measurementKey 
		for (int i = 0; i < channel.size(); i++) {
			measurementDefinition = channel.get(i);
			if (channel.get(i).getName().equals(measurementKey)) {
				//measurementDefinition.setDomElement(measurementElement);
				break;
			}
		}
		return measurementDefinition;
	}

	/**
	 * @return the sorted measurement names
	 */
	public String[] getMeasurementNames(String channelConfigKey) {
		// loop through the channels and search for one matching channelKey
		ChannelType channel = channels.get(0);
		for (int i = 1; i <= channels.size(); i++) {
			channel = channels.get(i);
			if(channel.getName().equals(channelConfigKey)) break;
		} 
		// loop through the measurement names and add
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < channel.size(); i++) {
			sb.append(channel.get(i).getName()).append(";");
		}
		return sb.toString().split(";");
	}

	/**
	 * @return the offset, if property does not exist return 0.0 as default value
	 */
	public double getOffset(String configKey, String measurementKey) {
		MeasurementType measurement = this.getMeasurementDefinition(configKey, measurementKey);
		log.info("measurement name = " + measurement.getName()); 
		PropertyType property = measurement.get(IDevice.OFFSET);
		if (property == null) // property does not exist
			return 0.0;
		else
			return (Double)(property.getValue());
	}

	/**
	 * @param offset the offset to set
	 */
	public void setOffset(String configKey, String measurementKey, double offset) {
		log.info("channelKey = \"" + configKey + "\" measurementKey = \"" + measurementKey + "\"");
		//TODO setDataCalculationProperty(configKey, measurementKey, DataCalculationType.OFFSET, offset);
	}

//	/**
//	 * @param configKey
//	 * @param measurementKey
//	 * @param type
//	 * @param value
//	 */
//	private void setDataCalculationProperty(String configKey, String measurementKey, String type, double value) {
//		this.isChangePropery = true;
//		MeasurementType measurement = this.getMeasurementDefinition(configKey, measurementKey);
//		DataCalculationType calcDefinition = measurement.getDataCalculation();
//		if (calcDefinition == null) {  // data calculation element and property does not exist
//			calcDefinition = createXMLDataCalculatiionType(configKey, measurementKey, type, value);
//			createXMLMeasurementDataCalculationProperty(measurement, calcDefinition, type, value);			
//		}
//		else { // dataCalculation exist
//			PropertyType property = calcDefinition.get(type);
//			if (property == null) {
//				createXMLMeasurementDataCalculationProperty(measurement, calcDefinition, type, value);
//			}
//			else {
//				updateXMLMeasurementDataCalculationProperty(calcDefinition, property, value);
//			}
//		}
//	}

//	/**
//	 * creates, appends a new DataCalculationType at measurement in XML to enable extension with PropertyTypes
//	 * @param configKey
//	 * @param measurementKey
//	 * @param type
//	 * @param value
//	 * @return created data calculation
//	 */
//	private DataCalculationType createXMLDataCalculatiionType(String configKey, String measurementKey, String type, double value) {
//		DataCalculationType calcDefinition = new DataCalculationType(this.doc);
//		MeasurementType measurement = this.getMeasurementDefinition(configKey, measurementKey);
//		measurement.setDataCalculation(calcDefinition);
//		Element element = this.doc.getDocumentElement();
//		NodeList channelNodeList = element.getElementsByTagName("Channel");
//		if (channelNodeList != null && channelNodeList.getLength() > 0) {
//			for (int i = 0; i < channelNodeList.getLength(); i++) {
//				Element el = (Element) channelNodeList.item(i);
//				String foundKey = el.getAttribute("name");
//				if (configKey.equals(foundKey)) {
//					NodeList measurementNodeList = el.getElementsByTagName("Measurement");
//					if (measurementNodeList != null && measurementNodeList.getLength() > 0) {
//						for (int j = 0; j < measurementNodeList.getLength(); j++) {
//							Element elm = (Element) measurementNodeList.item(j);
//							foundKey = XMLUtils.getTextValue(elm, "name");
//							if (measurementKey.equals(foundKey)) {
//								elm.appendChild(calcDefinition.getDomElement());
//							}
//						}
//					}
//				}
//			}
//		}
//		return calcDefinition;
//	}

//	/**
//	 * creates a property type at an existing measurement DataCalculationType
//	 * @param measurement key
//	 * @param calcDefinition
//	 * @param type (offset, factor, ..)
//	 * @param value
//	 */
//	private void createXMLMeasurementDataCalculationProperty(MeasurementType measurement, DataCalculationType calcDefinition, String type, double value) {
//		PropertyType propDefinition = new PropertyType(this.doc, type, PropertyType.Types.Double, value);
//		calcDefinition.addProperty(type, propDefinition);
//		calcDefinition.getDomElement().appendChild(propDefinition.getDomElement());
//		log.info("MeasurementType=" + measurement + " - DataCalculationType created : " + propDefinition.toString());
//	}

//	/**
//	 * update a property type at an existing measurement DataCalculationType
//	 * @param calculation definition
//	 * @param property definition
//	 * @param value
//	 */
//	private void updateXMLMeasurementDataCalculationProperty(DataCalculationType calculation, PropertyType property, double value) {
//		property.getDomElement().setAttribute("value", (""+value));
//		String key = property.getDomElement().getAttribute("name");
//		calculation.addProperty(key, property);
//		String measurement = calculation.getDomElement().getParentNode().getNodeName();
//		log.info("MeasurementType=" + measurement + " - DataCalculationType update : " + property.toString());
//	}

	/**
	 * @return the factor, , if property does not exist return 1.0 as default value
	 */
	public double getFactor(String configKey, String measurementKey) {
		MeasurementType measurement = this.getMeasurementDefinition(configKey, measurementKey);
		PropertyType property = measurement.get(IDevice.FACTOR);
		if (property == null) // property does not exist
			return 1.0;
		else
			return (Double)(property.getValue());
	}

	/**
	 * @param factor the factor to set
	 */
	public void setFactor(String configKey, String measurementKey, double factor) {
		log.info("channelKey = \"" + configKey + "\" measurementKey = \"" + measurementKey + "\"");
		//TODO setDataCalculationProperty(configKey, measurementKey, DataCalculationType.FACTOR, factor);
	}

	/**
	 * @modify or add a property at specified measurement
	 */
	public void setPropertyValue(String configKey, String measurementkey, String propertyKey, PropertyType.Types type, Object value) {
		this.isChangePropery = true;
		PropertyType propertyDefinition = this.getMeasurementDefinition(configKey, measurementkey).get(propertyKey);
		if (propertyDefinition == null) {  // property does not exist
			propertyDefinition = new PropertyType(this.doc, propertyKey, type, value);
			Element element = this.doc.getDocumentElement();
			NodeList measurementNodeList = element.getElementsByTagName("Measurement");
			if (measurementNodeList != null && measurementNodeList.getLength() > 0) {
				for (int i = 0; i < measurementNodeList.getLength(); i++) {
					Element el = (Element) measurementNodeList.item(i);
					String foundKey = XMLUtils.getTextValue(el, "name");
					if (measurementkey.equals(foundKey)) {
						el.appendChild(propertyDefinition.getDomElement());
						log.info("created : " + propertyDefinition.toString());
					}
				}
			}
		}
	}

	/**
	 * @return the property from measurement defined by key, if property does not exist return 1 as default value
	 */
	public Object getPropertyValue(String configKey, String measurementkey, String propertyKey) {
		PropertyType prop = this.getMeasurementDefinition(configKey, measurementkey).get(propertyKey);
		return prop != null ? prop.getValue() : 1;
	}

	/**
	 * @return the property from measurement defined by key
	 */
	public PropertyType getPropertyDefinition(String configKey, String measurementkey, String propertyKey) {
		this.isChangePropery = true;
		return this.getMeasurementDefinition(configKey, measurementkey).get(propertyKey);
	}

	/**
	 * @return the configuration document (DOM)
	 */
	public Document getConfigurationDocument() {
		return doc;
	}
}
