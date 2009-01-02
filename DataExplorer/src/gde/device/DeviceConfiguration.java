/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.device;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import osde.OSDE;
import osde.config.Settings;
import osde.log.LogFormatter;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;

/**
 * Device Configuration class makes the parsed DeviceProperties XML accessible for the application
 * @author Winfried Brügmann
 */
public class DeviceConfiguration {
	private final static Logger									log												= Logger.getLogger(DeviceConfiguration.class.getName());

	private final Settings										settings;

	// JAXB XML environment
	private final Unmarshaller								unmarshaller;
	private final Marshaller									marshaller;
	private File															xmlFile;
	// XML JAXB representation
	private JAXBElement<DevicePropertiesType>	elememt;
	private DevicePropertiesType							deviceProps;
	private DeviceType												device;
	private SerialPortType										serialPort;
	private TimeBaseType											timeBase;
	private DesktopType												desktop;
	private boolean														isChangePropery						= false;

	public final static int										DEVICE_TYPE_CHARGER				= 1;
	public final static int										DEVICE_TYPE_LOGGER				= 2;
	public final static int										DEVICE_TYPE_BALANCER			= 3;
	public final static int										DEVICE_TYPE_CURRENT_SINK	= 4;
	public final static int										DEVICE_TYPE_POWER_SUPPLY	= 5;
	public final static int										DEVICE_TYPE_GPS						= 5;
	public final static int										DEVICE_TYPE_RECEIVER			= 7;
	public final static int										DEVICE_TYPE_MULTIMETER		= 8;

	/**
	 * method to test this class
	 * @param args
	 */
	@SuppressWarnings("unchecked") //$NON-NLS-1$
	public static void main(String[] args) {
		Handler ch = new ConsoleHandler();
		LogFormatter lf = new LogFormatter();
		ch.setFormatter(lf);
		ch.setLevel(Level.ALL);
		Logger.getLogger(OSDE.STRING_EMPTY).addHandler(ch);
		Logger.getLogger(OSDE.STRING_EMPTY).setLevel(Level.ALL);

		String basePath = "C:/Documents and Settings/brueg/Application Data/OpenSerialDataExplorer/Devices/"; //$NON-NLS-1$
		//String basePath = "D:/Belchen2/workspaces/test/OpenSerialDataExplorer/doc/";

		try {
      Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new File(basePath + "DeviceProperties_V03.xsd")); //$NON-NLS-1$
			JAXBContext jc = JAXBContext.newInstance("osde.device"); //$NON-NLS-1$
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			unmarshaller.setSchema(schema);
			JAXBElement<DevicePropertiesType> elememt;
			
			// simple test with Picolario.xml
			elememt = (JAXBElement<DevicePropertiesType>)unmarshaller.unmarshal(new File (basePath + "Picolario.xml")); //$NON-NLS-1$
			DevicePropertiesType devProps = elememt.getValue();
			DeviceType device = devProps.getDevice();
			log.log(Level.INFO, "device.getName() = " + device.getName()); //$NON-NLS-1$
			SerialPortType serialPort = devProps.getSerialPort();
			log.log(Level.INFO, "serialPort.getPort() = " + serialPort.getPort()); //$NON-NLS-1$
			serialPort.setPort("COM10"); //$NON-NLS-1$
			log.log(Level.INFO, "serialPort.getPort() = " + serialPort.getPort()); //$NON-NLS-1$
			
			
			
			// store back manipulated XML
			Marshaller marshaller = jc.createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,  new Boolean(true));
	    marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,  Settings.DEVICE_PROPERTIES_XSD_NAME);

	    marshaller.marshal(elememt,
	    	   new FileOutputStream(basePath + "jaxbOutput.xml")); //$NON-NLS-1$
			
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (Throwable t) {
			log.log(Level.SEVERE, t.getMessage(), t);
		}
	}

	@SuppressWarnings("unchecked") // cast to (JAXBElement<DevicePropertiesType>) //$NON-NLS-1$
	public DeviceConfiguration(String xmlFileName) throws FileNotFoundException, JAXBException {

		if (!(this.xmlFile = new File(xmlFileName)).exists()) throw new FileNotFoundException(Messages.getString(MessageIds.OSDE_MSGE0003) + xmlFileName);

		this.settings = Settings.getInstance();
		this.unmarshaller = this.settings.getUnmarshaller();
		this.marshaller = this.settings.getMarshaller();

		this.elememt = (JAXBElement<DevicePropertiesType>)this.unmarshaller.unmarshal(this.xmlFile);
		this.deviceProps = this.elememt.getValue();
		this.device = this.deviceProps.getDevice();
		this.serialPort = this.deviceProps.getSerialPort();
		this.timeBase = this.deviceProps.getTimeBase();
		this.desktop = this.deviceProps.getDesktop();
		this.isChangePropery = false;
		
		log.log(Level.FINE, this.toString());
	}

	/**
	 * copy constructor
	 */
	public DeviceConfiguration(DeviceConfiguration deviceConfig) {
		this.settings = deviceConfig.settings;
		this.unmarshaller = deviceConfig.unmarshaller;
		this.marshaller = deviceConfig.marshaller;
		this.xmlFile = deviceConfig.xmlFile;
		this.elememt = deviceConfig.elememt;
		this.deviceProps = deviceConfig.deviceProps;
		this.device = deviceConfig.device;
		this.serialPort = deviceConfig.serialPort;	
		this.timeBase = deviceConfig.timeBase;	
		this.desktop = this.deviceProps.getDesktop();
		this.isChangePropery = deviceConfig.isChangePropery;

		log.log(Level.FINE, this.toString());
	}

	/**
	 * writes updated device properties XML
	 */
	public void storeDeviceProperties() {
		if (this.isChangePropery) {
			try {
				this.marshaller.marshal(this.elememt,  new FileOutputStream(this.xmlFile));
			}
			catch (Throwable t) {
				log.log(Level.SEVERE, t.getMessage(), t);
			}
			this.isChangePropery = false;
		}
	}

	/**
	 * @return the device
	 */
	public DeviceType getDeviceType() {
		return this.device;
	}

	/**
	 * @return the serialPort
	 */
	public SerialPortType getSerialPortType() {
		return this.serialPort;
	}

	/**
	 * @return the timeBase
	 */
	public TimeBaseType getTimeBaseType() {
		return this.timeBase;
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

	public Double getTimeStep_ms() {
		return this.timeBase.getTimeStep();
	}

	public void setTimeStep_ms(double newTimeStep_ms) {
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
		return this.serialPort.getBaudeRate().intValue();
	}

	public int getDataBits() {
		return this.serialPort.getDataBits().intValue();
	}

	public int getStopBits() {
		return this.serialPort.getStopBits().ordinal()+1; // starts with 1
	}

	public int getFlowCtrlMode() {
		return this.serialPort.getFlowControlMode().ordinal();
	}

	public int getParity() {
		return this.serialPort.getParity().ordinal();
	}

	public int getDataBlockSize() {
		return this.serialPort.getDataBlock().getSize().intValue();
	}

	public void setDataBlockSize(int newSize) {
		this.isChangePropery = true;
		this.serialPort.getDataBlock().setSize(new BigInteger(OSDE.STRING_EMPTY + newSize));
	}

	public byte getEndingByte() {
		return this.serialPort.getDataBlock().getEndingByte();
	}

	public boolean isDTR() {
		return this.serialPort.isIsDTR();
	}

	public boolean isRTS() {
		return this.serialPort.isIsRTS();
	}
	
	/**
	 * method to query desktop properties, like: table tab switched of, ...
	 * @param dektopType
	 * @return property of the queried type or null if not defined
	 */
	private PropertyType getDesktopProperty(String dektopType) {
		PropertyType property = null;
		if (this.desktop != null) {
			List<PropertyType> properties = this.desktop.getProperty();
			for (PropertyType propertyType : properties) {
				if (propertyType.getName().equals(dektopType)) {
					property = propertyType;
					break;
				}
			}
		}
		return property;
	}
	
	/**
	 * query if the table tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isTableTabRequested() {
		PropertyType property = this.getDesktopProperty(DesktopType.TYPE_TABLE_TAB);
		return new Boolean(property != null ? property.getValue() : null).booleanValue(); 
	}
	
	/**
	 * set the DesktopType.TYPE_TABLE_TAB property to the given value
	 * @param enable
	 */
	public void setTableTabRequested(boolean enable) {
		PropertyType property = this.getDesktopProperty(DesktopType.TYPE_TABLE_TAB);
		if (property == null) {
			createDesktopProperty(DesktopType.TYPE_TABLE_TAB, DataTypes.BOOLEAN, enable);
		}
		else {
			property.setValue(OSDE.STRING_EMPTY + enable);
		}
		this.isChangePropery = true;
	}
		
	/**
	 * query if the digital tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isDigitalTabRequested() {
		PropertyType property = this.getDesktopProperty(DesktopType.TYPE_DIGITAL_TAB);
		return new Boolean(property != null ? property.getValue() : null).booleanValue(); 
	}
	
	/**
	 * set the DesktopType.TYPE_DIGITAL_TAB property to the given value
	 * @param enable
	 */
	public void setDigitalTabRequested(boolean enable) {
		PropertyType property = this.getDesktopProperty(DesktopType.TYPE_DIGITAL_TAB);
		if (property == null) {
			createDesktopProperty(DesktopType.TYPE_DIGITAL_TAB, DataTypes.BOOLEAN, enable);
		}
		else {
			property.setValue(OSDE.STRING_EMPTY + enable);
		}
		this.isChangePropery = true;
	}
	
	/**
	 * query if the analog tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isAnalogTabRequested() {
		PropertyType property = this.getDesktopProperty(DesktopType.TYPE_ANALOG_TAB);
		return new Boolean(property != null ? property.getValue() : null).booleanValue(); 
	}
	
	/**
	 * set the DesktopType.TYPE_ANALOG_TAB property to the given value
	 * @param enable
	 */
	public void setAnalogTabRequested(boolean enable) {
		PropertyType property = this.getDesktopProperty(DesktopType.TYPE_ANALOG_TAB);
		if (property == null) {
			createDesktopProperty(DesktopType.TYPE_ANALOG_TAB, DataTypes.BOOLEAN, enable);
		}
		else {
			property.setValue(OSDE.STRING_EMPTY + enable);
		}
		this.isChangePropery = true;
	}
	
	/**
	 * query if the voltage per cell tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isVoltagePerCellTabRequested() {
		PropertyType property = this.getDesktopProperty(DesktopType.TYPE_VOLTAGE_PER_CELL_TAB);
		return new Boolean(property != null ? property.getValue() : null).booleanValue(); 
	}
	
	/**
	 * set the DesktopType.TYPE_VOLTAGE_PER_CELL_TAB property to the given value
	 * @param enable
	 */
	public void setVoltagePerCellTabRequested(boolean enable) {
		PropertyType property = this.getDesktopProperty(DesktopType.TYPE_VOLTAGE_PER_CELL_TAB);
		if (property == null) {
			createDesktopProperty(DesktopType.TYPE_VOLTAGE_PER_CELL_TAB, DataTypes.BOOLEAN, enable);
		}
		else {
			property.setValue(OSDE.STRING_EMPTY + enable);
		}
		this.isChangePropery = true;
	}
	
	/**
	 * @return the channel count
	 */
	public int getChannelCount() {
		return this.deviceProps.getChannel().size();
	}

	/**
	 * @return the channel name
	 */
	public String getChannelName(int channelNumber) {
		return this.deviceProps.getChannel().get(channelNumber - 1).getName();
	}

	/**
	 * @param channelName - size should not exceed 15 char length
	 * @param channelNumber
	 */
	public void setChannelName(String channelName, int channelNumber) {
		this.isChangePropery = true;
		this.deviceProps.getChannel().get(channelNumber - 1).setName(channelName);
	}

	/**
	 * @return the channel type by given channel number 
	 * 0 = TYPE_OUTLET, 1 = TYPE_CONFIG;
	 */
	public int getChannelType(int channelNumber) {
		return this.deviceProps.getChannel().get(channelNumber - 1).getType().ordinal();
	}
	
	/**
	 * @return the channel type by given channel configuration key (name)
	 * 0 = TYPE_OUTLET, 1 = TYPE_CONFIG;
	 */
	public int getChannelType(String channelConfigKey) {
		return this.getChannel(channelConfigKey).getType().ordinal();
	}
	
	/**
	 * @return the channel measurements by given channel configuration key (name)
	 */
	public List<MeasurementType> getChannelMeasuremts(String channelConfigKey) {
		return this.getChannel(channelConfigKey).getMeasurement();
	}
	
	/**
	 * @return the number of measurements of a channel by given channel number
	 */
	public int getNumberOfMeasurements(int channelNumber) {
		return this.deviceProps.getChannel().get(channelNumber - 1).getMeasurement().size();
	}

	/**
	 * @return the number of measurements of a channel by given channel configuration key (name)
	 */
	public int getNumberOfMeasurements(String channelConfigKey) {
		return this.getChannel(channelConfigKey).getMeasurement().size();
	}

	/**
	 * get the channel type by given channel configuration key (name)
	 * @param channelConfigKey
	 * @return
	 */
	private ChannelType getChannel(String channelConfigKey) {
		ChannelType channel = null;
		for (ChannelType c : this.deviceProps.getChannel()) {
			if(c.getName().trim().startsWith(channelConfigKey)) {
				channel = c;
				break;
			}
		}
		return channel;
	}

	/**
	 * set active status of an measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param isActive
	 */
	public void setMeasurementActive(String channelConfigKey, int measurementOrdinal, boolean isActive) {
		log.log(Level.FINER, "channelKey = \"" + channelConfigKey + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigKey)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.isChangePropery = true;
		this.getMeasurement(channelConfigKey, measurementOrdinal).setActive(isActive); //$NON-NLS-1$
	}

	/**
	 * get the measurement to get/set measurement specific parameter/properties
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return MeasurementType
	 */
	public MeasurementType getMeasurement(String channelConfigKey, int measurementOrdinal) {
		MeasurementType measurement = null;
		try {
			String tmpMeasurementKey = this.getMeasurementNames(channelConfigKey)[measurementOrdinal];
			for (MeasurementType meas : this.getChannel(channelConfigKey).getMeasurement()) {
				if (meas.getName().equals(tmpMeasurementKey)) {
					measurement = meas;
					break;
				}
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, channelConfigKey + " - " + this.getMeasurementNames(channelConfigKey)[measurementOrdinal], e); //$NON-NLS-1$
		}
		return measurement;
	}
	
	/**
	 * get the properties from a channel/configuration and record key name 
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return List of properties to according measurement
	 */
	public List<PropertyType> getProperties(String channelConfigKey, int measurementOrdinal) {
		List<PropertyType> list = new ArrayList<PropertyType>();
		MeasurementType measurement = this.getMeasurement(channelConfigKey, measurementOrdinal);
		if (measurement != null)
			list = measurement.getProperty();
		return list;
	}
	
	/**
	 * set new name of specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param name
	 */
	public void setMeasurementName(String channelConfigKey, int measurementOrdinal, String name) {
		log.log(Level.FINER, "channelKey = \"" + channelConfigKey + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigKey)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.isChangePropery = true;
		this.getMeasurement(channelConfigKey, measurementOrdinal).setName(name);
	}
	
	/**
	 * method to query the unit of measurement data unit by a given record key
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return dataUnit as string
	 */
	public String getMeasurementUnit(String channelConfigKey, int measurementOrdinal) {
		log.log(Level.FINER, "channelKey = \"" + channelConfigKey + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigKey)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return this.getMeasurement(channelConfigKey, measurementOrdinal).getUnit(); //$NON-NLS-1$
	}

	/**
	 * method to set the unit of measurement by a given measurement key
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param unit
	 */
	public void setMeasurementUnit(String channelConfigKey, int measurementOrdinal, String unit) {
		log.log(Level.FINER, "channelKey = \"" + channelConfigKey + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigKey)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.isChangePropery = true;
		this.getMeasurement(channelConfigKey, measurementOrdinal).setUnit(unit);
	}
	
	/**
	 * get the symbol of specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return the measurement symbol as string
	 */
	public String getMeasurementSymbol(String channelConfigKey, int measurementOrdinal) {
		return this.getMeasurement(channelConfigKey, measurementOrdinal).getSymbol();
	}

	/**
	 * set new symbol of specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param symbol
	 */
	public void setMeasurementSymbol(String channelConfigKey, int measurementOrdinal, String symbol) {
		this.isChangePropery = true;
		this.getMeasurement(channelConfigKey, measurementOrdinal).setSymbol(symbol);
	}

	/**
	 * get the statistics type of the specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return statistics, if statistics does not exist return null
	 */
	public StatisticsType getMeasurementStatistic(String channelConfigKey, int measurementOrdinal) {
		log.log(Level.FINER, "get statistics type from measurement = " + this.getMeasurement(channelConfigKey, measurementOrdinal).getName());  //$NON-NLS-1$
		return this.getMeasurement(channelConfigKey, measurementOrdinal).getStatistics();
	}
	
	/**
	 * @return the sorted measurement names
	 */
	public String[] getMeasurementNames(String channelConfigKey) {
		StringBuilder sb = new StringBuilder();
		ChannelType channel = this.getChannel(channelConfigKey);
		if (channel != null) {
			List<MeasurementType> measurement = channel.getMeasurement();
			for (MeasurementType measurementType : measurement) {
				sb.append(measurementType.getName()).append(OSDE.STRING_SEMICOLON);
			}
		}
		return sb.toString().length()>1 ? sb.toString().split(OSDE.STRING_SEMICOLON) : new String[0];
	}

	/**
	 * get property with given channel configuration key, measurement key and property type key (IDevice.OFFSET, ...)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @return PropertyType
	 */
	public PropertyType getMeasruementProperty(String channelConfigKey, int measurementOrdinal, String propertyKey) {
		PropertyType property = null;
		try {
			MeasurementType measurementType = this.getMeasurement(channelConfigKey, measurementOrdinal);
			if (measurementType != null) {
				List<PropertyType> properties = measurementType.getProperty();
				for (PropertyType propertyType : properties) {
					if (propertyType.getName().equals(propertyKey)) {
						property = propertyType;
						break;
					}
				}
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, channelConfigKey + " - " + this.getMeasurementNames(channelConfigKey)[measurementOrdinal] + " - " + propertyKey, e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return property;
	}

	/**
	 * get the offset value of the specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return the offset, if property does not exist return 0.0 as default value
	 */
	public double getMeasurementOffset(String channelConfigKey, int measurementOrdinal) {
		log.log(Level.FINER, "get offset from measurement name = " + this.getMeasurement(channelConfigKey, measurementOrdinal).getName());  //$NON-NLS-1$
		double value = 0.0;
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementOrdinal, IDevice.OFFSET);
		if (property != null)
			value = new Double(property.getValue()).doubleValue();
		
		return value;
	}

	/**
	 * set new value for offset at the specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param offset the offset to set
	 */
	public void setMeasurementOffset(String channelConfigKey, int measurementOrdinal, double offset) {
		log.log(Level.FINER, "set offset onto measurement name = " + this.getMeasurement(channelConfigKey, measurementOrdinal).getName());  //$NON-NLS-1$
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementOrdinal, IDevice.OFFSET);
		if (property == null) {
			createProperty(channelConfigKey, measurementOrdinal, IDevice.OFFSET, DataTypes.DOUBLE, offset);
		}
		else {
			property.setValue(OSDE.STRING_EMPTY + offset);
		}
	}

	/**
	 * get the factor value of the specified measurement (offset + (value - reduction) * factor)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return the factor, if property does not exist return 1.0 as default value
	 */
	public double getMeasurementFactor(String channelConfigKey, int measurementOrdinal) {
		log.log(Level.FINER, "get factor from measurement name = " + this.getMeasurement(channelConfigKey, measurementOrdinal).getName());  //$NON-NLS-1$
		double value = 1.0;
		PropertyType property = getMeasruementProperty(channelConfigKey, measurementOrdinal, IDevice.FACTOR);
		if (property != null)
			value = new Double(property.getValue()).doubleValue();
		
		return value;
	}

	/**
	 * set new value for factor at the specified measurement (offset + (value - reduction) * factor)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param factor the offset to set
	 */
	public void setMeasurementFactor(String channelConfigKey, int measurementOrdinal, double factor) {
		log.log(Level.FINER, "set factor onto measurement name = " + this.getMeasurement(channelConfigKey, measurementOrdinal).getName());  //$NON-NLS-1$
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementOrdinal, IDevice.FACTOR);
		if (property == null) {
			createProperty(channelConfigKey, measurementOrdinal, IDevice.FACTOR, DataTypes.DOUBLE, factor);
		}
		else {
			property.setValue(OSDE.STRING_EMPTY + factor);
		}
	}

	/**
	 * get the reduction value of the specified measurement (offset + (value - reduction) * factor)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return the reduction, if property does not exist return 0.0 as default value
	 */
	public double getMeasurementReduction(String channelConfigKey, int measurementOrdinal) {
		log.log(Level.FINER, "get reduction from measurement name = " + this.getMeasurement(channelConfigKey, measurementOrdinal).getName());  //$NON-NLS-1$
		double value = 0.0;
		PropertyType property = getMeasruementProperty(channelConfigKey, measurementOrdinal, IDevice.REDUCTION);
		if (property != null)
			value = new Double(property.getValue()).doubleValue();
		
		return value;
	}

	/**
	 * set new value for factor at the specified measurement (offset + (value - reduction) * factor)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param reduction of the direct measured value
	 */
	public void setMeasurementReduction(String channelConfigKey, int measurementOrdinal, double reduction) {
		log.log(Level.FINER, "set reduction onto measurement name = " + this.getMeasurement(channelConfigKey, measurementOrdinal).getName());  //$NON-NLS-1$
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementOrdinal, IDevice.REDUCTION);
		if (property == null) {
			createProperty(channelConfigKey, measurementOrdinal, IDevice.REDUCTION, DataTypes.DOUBLE, reduction);
		}
		else {
			property.setValue(OSDE.STRING_EMPTY + reduction);
		}
	}

	/**
	 * get a property of specified measurement, the data type must be known - data conversion is up to implementation
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @return the property from measurement defined by key, if property does not exist return 1 as default value
	 */
	public Object getMeasurementPropertyValue(String channelConfigKey, int measurementOrdinal, String propertyKey) {
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementOrdinal, propertyKey);
		return property != null ? property.getValue() : OSDE.STRING_EMPTY;
	}
	
	/**
	 * set new property value of specified measurement, if the property does not exist it will be created
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @param type of DataTypes
	 * @param value
	 */
	public void setMeasurementPropertyValue(String channelConfigKey, int measurementOrdinal, String propertyKey, DataTypes type, Object value) {
		this.isChangePropery = true;
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementOrdinal, propertyKey);
		if (property == null) {
			createProperty(channelConfigKey, measurementOrdinal, propertyKey, type, (OSDE.STRING_EMPTY + value).replace(OSDE.STRING_COMMA, OSDE.STRING_DOT)); //$NON-NLS-1$
		}
		else {
			property.setValue((OSDE.STRING_EMPTY + value).replace(OSDE.STRING_COMMA, OSDE.STRING_DOT));
		}
	}

	/**
	 * create a measurement property
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @param type
	 * @param value
	 */
	private void createProperty(String channelConfigKey, int measurementOrdinal, String propertyKey, DataTypes type, Object value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(propertyKey);
		newProperty.setType(type);
		newProperty.setValue(OSDE.STRING_EMPTY + value);
		this.getMeasurement(channelConfigKey, measurementOrdinal).getProperty().add(newProperty);
	}

	/**
	 * create a desktop property
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @param type
	 * @param value
	 */
	private void createDesktopProperty(String propertyKey, DataTypes type, Object value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(propertyKey);
		newProperty.setType(type);
		newProperty.setValue(OSDE.STRING_EMPTY + value);
		
		if (this.desktop == null) {
			this.desktop = factory.createDesktopType();
			this.deviceProps.setDesktop(this.desktop);
		}

		this.desktop.getProperty().add(newProperty);
	}

	/**
	 * @param enabled the isChangePropery to set
	 */
	public void setChangePropery(boolean enabled) {
		this.isChangePropery = enabled;
	}

	/**
	 * @return the isChangePropery
	 */
	public boolean isChangePropery() {
		return this.isChangePropery;
	}
	
	/**
	 * method to modify open/close serial port menu toolbar button and device menu entry
	 * this enable different naming instead open/close start/stop gathering data from device
	 * and must be called within specific device constructor
	 * @param useIconSet  DeviceSerialPort.ICON_SET_OPEN_CLOSE | DeviceSerialPort.ICON_SET_START_STOP
	 */
	public void configureSerialPortMenu(int useIconSet) {
		OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
		application.getMenuBar().setSerialPortIconSet(useIconSet);
		application.getMenuToolBar().setSerialPortIconSet(useIconSet);
	}

}
