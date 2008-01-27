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
	private final String[]											masurementNames;
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

		//get a nodelist of <TimeBase> element
		TimeBaseType tb = null;
		NodeList timeBaseNodeList = docEle.getElementsByTagName("TimeBase");
		if (timeBaseNodeList != null && timeBaseNodeList.getLength() > 0) {
			Element el = (Element) timeBaseNodeList.item(0);
			tb = new TimeBaseType(el);
		}
		else
			throw new SAXException("<TimeBase> Element fehlerhaft in " + xmlFilePath);
		this.timeBase = tb;

		//get a nodelist of <Channel> elements
		NodeList channelNodeList = docEle.getElementsByTagName("Channel");
		if (channelNodeList != null && channelNodeList.getLength() > 0) {
			// loop through the list if more than one channel
			for (int i = 0; i < channelNodeList.getLength(); i++) {
				Element el = (Element) channelNodeList.item(i);
				ChannelType channel = new ChannelType(el);
				channels.put((i + 1), channel);
			}
		}
		else
			throw new SAXException("<Channel> Element fehlerhaft in " + xmlFilePath);

		// all measurements for all channels are equal !
		this.masurementNames = channels.get(1).getMeasurementNames().toArray(new String[1]);

		if (log.isLoggable(Level.FINE)) log.fine(this.toString());
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
		this.masurementNames = deviceConfig.getMeasurementNames();
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
		this.masurementNames = getChannel(1).getMeasurementNames().toArray(new String[1]);

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
	public String getDataUnit(String recordKey) {
		return this.getMeasurementDefinition(recordKey.split("_")[0]).getUnit();
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
	 * @return the number of measurements of a channel
	 */
	public int getNumberOfMeasurements() {
		return channels.get(1).size();
	}

	/**
	 * @return the measurement definitions matching key (voltage, current, ...)
	 */
	public MeasurementType getMeasurementDefinition(String recordKey) {
		MeasurementType measurementDefinition = null;
		// assuming all channels have identical measurements
		ChannelType channel = channels.get(1);
		// loop through channels and find 
		for (int i = 0; i < channel.size(); i++) {
			if (channel.get(i).getName().equals(recordKey)) {
				measurementDefinition = channel.get(i);
			}
		}
		return measurementDefinition;
	}

	/**
	 * @return the sorted measurement names
	 */
	public String[] getMeasurementNames() {
		return masurementNames;
	}

	/**
	 * @return the offset
	 */
	public double getOffset(String recordKey) {
		return this.getMeasurementDefinition(recordKey).getDataCalculation().getOffset();
	}

	/**
	 * @param offset the offset to set
	 */
	public void setOffset(String recordKey, double offset) {
		this.getMeasurementDefinition(recordKey).getDataCalculation().setOffset(offset);
	}

	/**
	 * @return the factor
	 */
	public double getFactor(String recordKey) {
		return getMeasurementDefinition(recordKey).getDataCalculation().getFactor();
	}

	/**
	 * @param factor the factor to set
	 */
	public void setFactor(String recordKey, double factor) {
		this.getMeasurementDefinition(recordKey).getDataCalculation().setFactor(factor);
	}
}
