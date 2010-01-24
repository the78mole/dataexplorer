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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import osde.log.Level;
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
import osde.utils.CalculationThread;
import osde.utils.StringHelper;

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
	private DataBlockType											dataBlock;
	private StateType													state;
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
	
	protected 					CalculationThread			calculationThread 				= null; // universal device calculation thread (slope)


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
			log.log(Level.ALL, "device.getName() = " + device.getName()); //$NON-NLS-1$
			SerialPortType serialPort = devProps.getSerialPort();
			log.log(Level.ALL, "serialPort.getPort() = " + serialPort.getPort()); //$NON-NLS-1$
			serialPort.setPort("COM10"); //$NON-NLS-1$
			log.log(Level.ALL, "serialPort.getPort() = " + serialPort.getPort()); //$NON-NLS-1$
			
			
			
			// store back manipulated XML
			Marshaller marshaller = jc.createMarshaller();
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,  Boolean.valueOf(true));
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
		this.dataBlock = this.deviceProps.getDataBlock();
		this.state = this.deviceProps.getState();
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
		this.dataBlock = deviceProps.dataBlock;
		this.state = deviceProps.state;
		this.timeBase = deviceConfig.timeBase;	
		this.desktop = deviceProps.desktop;
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
	 * writes device properties XML to given full qualified file name
	 * @param fullQualifiedFileName
	 */
	public void storeDeviceProperties(String fullQualifiedFileName) {
			try {
				this.marshaller.marshal(this.elememt,  new FileOutputStream(fullQualifiedFileName));
			}
			catch (Throwable t) {
				log.log(Level.SEVERE, t.getMessage(), t);
			}
			this.isChangePropery = false;
	}

	/**
	 * get the active device configuration
	 */
	public DeviceConfiguration getDeviceConfiguration() {
		return this;
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
	 * @return the serialPort
	 */
	public void removeSerialPortType() {
		this.isChangePropery = true;
		this.serialPort = this.deviceProps.serialPort = null;
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

	/**
	 * @return the device name
	 */
	public String getName() {
		return this.device.getName().getValue();
	}

	/**
	 * @return the device name
	 */
	public String getDeviceImplName() {
		return this.device.getName().getImplementation() == null ? this.device.getName().getValue() : this.device.getName().getImplementation();
	}

	/**
	 * @param set a new device name
	 */
	public void setName(String newDeviceName) {
		this.isChangePropery = true;
		this.device.getName().setValue(newDeviceName);
	}

	/**
	 * @return the device name
	 */
	public void setDeviceImplName(String newDeviceImplClass) {
		this.isChangePropery = true;
		this.device.getName().setImplementation(newDeviceImplClass);
	}

	/**
	 * @return the device name
	 */
	public String getImageFileName() {
		return this.device.getImage();
	}

	/**
	 * @param set a new image filename(.jpg|.gif|.png)
	 */
	public void setImageFileName(String newImageFileName) {
		this.isChangePropery = true;
		this.device.setImage(newImageFileName);
	}

	public String getManufacturer() {
		return this.device.getManufacturer();
	}

	/**
	 * @param set a new device manufacture name
	 */
	public void setManufacturer(String name) {
		this.isChangePropery = true;
		this.device.setManufacturer(name);
	}

	public String getManufacturerURL() {
		return this.device.getManufacturerURL();
	}

	/**
	 * @param set a new manufacture name
	 */
	public void setManufacturerURL(String name) {
		this.isChangePropery = true;
		this.device.setManufacturerURL(name);
	}

	public DeviceTypes getDeviceGroup() {
		return this.device.getGroup();
	}

	/**
	 * @param set a new manufacture name
	 */
	public void setDeviceGroup(DeviceTypes name) {
		this.isChangePropery = true;
		this.device.setGroup(name);
	}

	public double getTimeStep_ms() {
		return this.timeBase.getTimeStep();
	}

	public void setTimeStep_ms(double newTimeStep_ms) {
		this.isChangePropery = true;
		this.timeBase.setTimeStep(newTimeStep_ms);
	}

	/**
	 * @return the port configured for the device, if SerialPortType is not defined in device specific XML a empty string will returned
	 */
	public String getPort() {
		if (this.serialPort == null) createSerialPort();
		return this.settings.isGlobalSerialPort() ? this.settings.getSerialPort() : this.serialPort.getPort();
	}

	/**
	 * @return the port configured in SerialPortType
	 */
	public String getPortString() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.getPort();
	}

	public void setPort(String newPort) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setPort(newPort);
	}

	public Integer getBaudeRate() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.getBaudeRate();
	}
	
	public void setBaudeRate(Integer value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setBaudeRate(value);
	}

	public DataBitsTypes getDataBits() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.getDataBits();
	}

	public void setDataBits(DataBitsTypes value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setDataBits(value);
	}

	public StopBitsTypes getStopBits() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.getStopBits();
	}

	public void setStopBits(StopBitsTypes enumOrdinal) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setStopBits(enumOrdinal);
	}

	public FlowControlTypes getFlowCtrlMode() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.getFlowControlMode();
	}

	public void setFlowCtrlMode(FlowControlTypes value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setFlowControlMode(value);
	}

	public ParityTypes getParity() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.getParity();
	}

	/**
	 * create a serial port type with default values
	 */
	private void createSerialPort() {
		this.deviceProps.serialPort = this.serialPort = new ObjectFactory().createSerialPortType();
		this.serialPort.setPort(OSDE.STRING_EMPTY);
		this.serialPort.setBaudeRate(9600);
		this.serialPort.setParity(ParityTypes.PARITY_NONE);
		this.serialPort.setDataBits(DataBitsTypes.DATABITS_8);
		this.serialPort.setStopBits(StopBitsTypes.STOPBITS_1);
		this.serialPort.setFlowControlMode(FlowControlTypes.FLOWCONTROL_NONE);
		this.serialPort.setIsDTR(false);
		this.serialPort.setIsRTS(false);
	}

	public void setParity(ParityTypes value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setParity(value);
	}

	public boolean isDTR() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.isIsDTR();
	}

	public void setIsDTR(boolean value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setIsDTR(value);
	}

	public boolean isRTS() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.isIsRTS();
	}

	public void setIsRTS(boolean value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		this.serialPort.setIsRTS(value);
	}
	
	public int getRTOCharDelayTime() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.getTimeOut() != null ? this.serialPort.getTimeOut().getRTOCharDelayTime() : 0;
	}

	public void setRTOCharDelayTime(int value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		if (this.serialPort.getTimeOut() == null) {
			this.serialPort.setTimeOut(new ObjectFactory().createTimeOutType());
		}
		this.serialPort.getTimeOut().setRTOCharDelayTime(value);
	}
	
	public int getRTOExtraDelayTime() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.getTimeOut() != null ? this.serialPort.getTimeOut().getRTOExtraDelayTime() : 0;
	}

	public void setRTOExtraDelayTime(int value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		if (this.serialPort.getTimeOut() == null) {
			this.serialPort.setTimeOut(new ObjectFactory().createTimeOutType());
		}
		this.serialPort.getTimeOut().setRTOExtraDelayTime(value);
	}
	
	public int getWTOCharDelayTime() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.getTimeOut() != null ? this.serialPort.getTimeOut().getWTOCharDelayTime() : 0;
	}

	public void setWTOCharDelayTime(int value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		if (this.serialPort.getTimeOut() == null) {
			this.serialPort.setTimeOut(new ObjectFactory().createTimeOutType());
		}
		this.serialPort.getTimeOut().setWTOCharDelayTime(value);
	}
	
	public int getWTOExtraDelayTime() {
		if (this.serialPort == null) createSerialPort();
		return this.serialPort.getTimeOut() != null ? this.serialPort.getTimeOut().getWTOExtraDelayTime() : 0;
	}

	public void setWTOExtraDelayTime(int value) {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		if (this.serialPort.getTimeOut() == null) {
			this.serialPort.setTimeOut(new ObjectFactory().createTimeOutType());
		}
		this.serialPort.getTimeOut().setWTOExtraDelayTime(value);
	}
	
	public void removeSerialPortTimeOut() {
		this.isChangePropery = true;
		if (this.serialPort == null) createSerialPort();
		if (this.serialPort.getTimeOut() != null) {
			this.serialPort.setTimeOut(null);
		}
	}

	/**
	 * set a new desktop type
	 * @param newDesktopType
	 */
	public void setDesktopType(DesktopType newDesktopType) {
		this.deviceProps.setDesktop(newDesktopType);
		this.desktop = this.deviceProps.desktop;
		this.isChangePropery = true;
	}
	
	/**
	 * get the desktop type
	 * @return DesktopType
	 */
	public DesktopType getDesktopType() {
		return this.desktop;
	}
	
	/**
	 * method to query desktop properties, like: table tab switched of, ...
	 * @param dektopType
	 * @return property of the queried type or null if not defined
	 */
	public PropertyType getDesktopProperty(DesktopPropertyTypes dektopType) {
		PropertyType property = null;
		if (this.desktop != null) {
			List<PropertyType> properties = this.desktop.getProperty();
			for (PropertyType propertyType : properties) {
				if (propertyType.getName().equals(dektopType.value())) {
					property = propertyType;
					break;
				}
			}
		}
		return property;
	}
	
	/**
	 * set the desktop type value
	 * @param dektopType
	 * @param newValue
	 */
	public void setDesktopTypeValue(DesktopPropertyTypes desktopType, Boolean newValue) {
			this.getDesktopProperty(desktopType).setValue(newValue);
			this.isChangePropery = true;
	}
	
	/**
	 * @return size of mode states
	 */
	public int getStateSize() {
		return this.state.property.size();
	}
	
	/**
	 * @return actual StateType
	 */
	public StateType getStateType() {
		return this.deviceProps.state;
	}
	
	/**
	 * remove optional mode state
	 */
	public void removeStateType() {
		this.isChangePropery = true;
		this.state = this.deviceProps.state = null;
	}
	
	/**
	 * append a new mode state type property
	 * @param newStateProperty
	 */
	public void appendStateType(PropertyType newStateProperty) {
		this.isChangePropery = true;
		if (this.deviceProps.state == null) {
			this.deviceProps.state = new ObjectFactory().createStateType();
		}
		this.deviceProps.state.getProperty().add(newStateProperty);
	}
	
	/**
	 * remove a mode state type property
	 * @param removeStateProperty
	 */
	public void removeStateType(PropertyType removeStateProperty) {
		this.isChangePropery = true;
		this.deviceProps.state.remove(removeStateProperty);
	}
	
	/**
	 * set a new mode state name
	 * @param modeStateOrdinal
	 * @param newName
	 */
	public void setStateName(int modeStateOrdinal, String newName) {
		this.isChangePropery = true;
		PropertyType tmpPoperty = this.getStateProperty(modeStateOrdinal);
		if (tmpPoperty != null) {
			tmpPoperty.setName(newName);
		}
	}
	
	/**
	 * set a new mode state value
	 * @param modeStateOrdinal
	 * @param newValue
	 */
	public void setStateValue(int modeStateOrdinal, String newValue) {
		this.isChangePropery = true;
		PropertyType tmpPoperty = this.getStateProperty(modeStateOrdinal);
		if (tmpPoperty != null) {
			tmpPoperty.setValue(StringHelper.verifyTypedInput(tmpPoperty.getType(), newValue));
		}
	}
	
	/**
	 * set a new mode state description
	 * @param modeStateOrdinal
	 * @param newDescription
	 */
	public void setStateDescription(int modeStateOrdinal, String newDescription) {
		this.isChangePropery = true;
		PropertyType tmpPoperty = this.getStateProperty(modeStateOrdinal);
		if (tmpPoperty != null) {
			tmpPoperty.setDescription(newDescription);
		}
	}
	
	/**
	 * method to query desktop properties, like: table tab switched of, ...
	 * @param dektopType
	 * @return property of the queried type or null if not defined
	 */
	public PropertyType getStateProperty(int modeStateOrdinal) {
		PropertyType property = null;
		if (this.state != null) {
			List<PropertyType> properties = this.state.getProperty();
			for (PropertyType propertyType : properties) {
				try {
					int propertyValue = Integer.parseInt(propertyType.getValue());
					if (propertyValue == modeStateOrdinal) {
						property = propertyType;
						break;
					}
				}
				catch (NumberFormatException e) {
					log.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
		return property;
	}
	
	public DataBlockType getDataBlockType() {
		return this.dataBlock;
	}
	
	public void removeDataBlockType() {
		this.isChangePropery = true;
		this.dataBlock = this.deviceProps.dataBlock = null;
	}
	
	public void addDataBlockType() {
		this.isChangePropery = true;
		this.dataBlock = this.deviceProps.dataBlock = new DataBlockType();
	}
	
	public int getDataBlockSize() {
		return this.dataBlock != null && this.dataBlock.getFormat() != null ? this.dataBlock.getFormat().getSize() : -1;
	}

	public void setDataBlockSize(Integer newSize) {
		this.isChangePropery = true;
		if (this.dataBlock == null) {
			this.dataBlock = this.deviceProps.dataBlock = new DataBlockType();
			if (this.dataBlock.getFormat() == null) {
				this.dataBlock.format = new DataBlockType.Format();
			}
		}
		this.dataBlock.getFormat().setSize(newSize);
	}
	
	public FormatTypes getDataBlockFormat() {
		return this.dataBlock.getFormat() == null ? null : this.dataBlock.getFormat().getType();
	}
	
	public void setDataBlockFormat(FormatTypes value) {
		this.isChangePropery = true;
		if (this.dataBlock == null) {
			this.dataBlock = this.deviceProps.dataBlock = new DataBlockType();
			if (this.dataBlock.getFormat() == null) {
				this.dataBlock.format = new DataBlockType.Format();
			}
		}
		this.dataBlock.getFormat().setType(value);
	}

	public boolean isDataBlockCheckSumDefined() {
		return this.dataBlock.getCheckSum() != null && (this.dataBlock.getCheckSum() != null && this.dataBlock.getCheckSum().getFormat() != null);
	}
	
	public CheckSumTypes getDataBlockCheckSumType() {
		return this.dataBlock.getCheckSum() == null ? null : this.dataBlock.getCheckSum().getType(); 
	}

	public void setDataBlockCheckSumType(CheckSumTypes value) {
		this.isChangePropery = true;
		if (value == null)
			this.dataBlock.setCheckSum(null);
		else {
			if (this.dataBlock.getCheckSum() == null) this.dataBlock.setCheckSum(new DataBlockType.CheckSum());
			this.dataBlock.getCheckSum().setType(value); 
		}
	}
	
	public FormatTypes getDataBlockCheckSumFormat() {
		return this.dataBlock.getCheckSum() == null ? null : this.dataBlock.getCheckSum().getFormat();
	}
	
	public void setDataBlockCheckSumFormat(FormatTypes value) {
		this.isChangePropery = true;
		if (value == null)
			this.dataBlock.setCheckSum(null);
		else {
			if (this.dataBlock.getCheckSum() == null) this.dataBlock.setCheckSum(new DataBlockType.CheckSum());
			this.dataBlock.getCheckSum().setFormat(value);
		}
	}
	
	public boolean isDataBlockEndingDefined() {
		return this.dataBlock.getFormat().ending != null;
	}

	public byte[] getDataBlockEnding() {
		return this.dataBlock.getFormat().getEnding();
	}

	public String getDataBlockEndingLineEndingType() {
		return LineEndingTypes.valueFrom(this.dataBlock.getFormat().getEnding());
	}

	public void setDataBlockEnding(String value) {
		this.isChangePropery = true;
		if (value == null)
			this.dataBlock.getFormat().setEnding(null);
		else
			this.dataBlock.getFormat().setEnding(LineEndingTypes.bytesFromValue(value));
	}

	public TimeUnitTypes getDataBlockTimeUnit() {
		return this.dataBlock.getFormat().getTimeUnit();
	}

	public void setDataBlockTimeUnit(TimeUnitTypes value) {
		this.isChangePropery = true;
		this.dataBlock.getFormat().setTimeUnit(value);
	}

	public int getDataBlockTimeUnitFactor() {
		return this.dataBlock.getFormat().getTimeUnit() == null ? 1000 : this.dataBlock.getFormat().getTimeUnit().equals(TimeUnitTypes.MSEC) ? 1 : 1000;
	}

	public CommaSeparatorTypes getDataBlockSeparator() {
		return this.dataBlock.getFormat().getSeparator();
	}

	public void setDataBlockSeparator(CommaSeparatorTypes value) {
		this.isChangePropery = true;
		this.dataBlock.getFormat().setSeparator(value);
	}
	
	public String getDataBlockPreferredDataLocation() {
		return this.dataBlock != null ? (this.dataBlock.getPreferredDataLocation() != null ? this.dataBlock.getPreferredDataLocation() : OSDE.STRING_BLANK) : OSDE.STRING_BLANK;
	}

	public void setDataBlockPreferredDataLocation(String value) {
		this.isChangePropery = true;
		this.dataBlock.setPreferredDataLocation(value);
	}
	
	public boolean isDataBlockPreferredFileExtentionDefined() {
		return this.dataBlock.preferredFileExtention != null;
	}
	
	public String getDataBlockPreferredFileExtention() {
		return this.dataBlock.getPreferredFileExtention();
	}

	public void setDataBlockPreferredFileExtention(String value) {
		boolean isValidExt = this.isChangePropery = true;
		if (value != null) {
			isValidExt = (value = value.replace(OSDE.STRING_BLANK, OSDE.STRING_EMPTY).replace(OSDE.STRING_STAR, OSDE.STRING_EMPTY).replace(OSDE.STRING_DOT, OSDE.STRING_EMPTY).trim()).length() >= 1;
			if (!isValidExt) {
				this.dataBlock.setPreferredFileExtention(null);
			}
			else {
				value = "*." + value;
			}
		} 
		this.dataBlock.setPreferredFileExtention(value != null && isValidExt ? value : this.dataBlock.getPreferredFileExtention());
	}

	/**
	 * query if the table tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isTableTabRequested() {
		PropertyType property = this.getDesktopProperty(DesktopPropertyTypes.TABLE_TAB);
		return Boolean.valueOf(property != null ? property.getValue() : null); 
	}
	
	/**
	 * set the DesktopType.TYPE_TABLE_TAB property to the given value
	 * @param enable
	 */
	public void setTableTabRequested(boolean enable) {
		PropertyType property = this.getDesktopProperty(DesktopPropertyTypes.TABLE_TAB);
		if (property == null) {
			createDesktopProperty(DesktopPropertyTypes.TABLE_TAB.name(), DataTypes.BOOLEAN, enable);
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
		PropertyType property = this.getDesktopProperty(DesktopPropertyTypes.DIGITAL_TAB);
		return Boolean.valueOf(property != null ? property.getValue() : null); 
	}
	
	/**
	 * set the DesktopType.TYPE_DIGITAL_TAB property to the given value
	 * @param enable
	 */
	public void setDigitalTabRequested(boolean enable) {
		PropertyType property = this.getDesktopProperty(DesktopPropertyTypes.DIGITAL_TAB);
		if (property == null) {
			createDesktopProperty(DesktopPropertyTypes.DIGITAL_TAB.name(), DataTypes.BOOLEAN, enable);
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
		PropertyType property = this.getDesktopProperty(DesktopPropertyTypes.ANALOG_TAB);
		return Boolean.valueOf(property != null ? property.getValue() : null); 
	}
	
	/**
	 * set the DesktopType.TYPE_ANALOG_TAB property to the given value
	 * @param enable
	 */
	public void setAnalogTabRequested(boolean enable) {
		PropertyType property = this.getDesktopProperty(DesktopPropertyTypes.ANALOG_TAB);
		if (property == null) {
			createDesktopProperty(DesktopPropertyTypes.ANALOG_TAB.name(), DataTypes.BOOLEAN, enable);
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
		PropertyType property = this.getDesktopProperty(DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB);
		return Boolean.valueOf(property != null ? property.getValue() : null); 
	}
	
	/**
	 * set the DesktopType.TYPE_VOLTAGE_PER_CELL_TAB property to the given value
	 * @param enable
	 */
	public void setVoltagePerCellTabRequested(boolean enable) {
		PropertyType property = this.getDesktopProperty(DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB);
		if (property == null) {
			createDesktopProperty(DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB.name(), DataTypes.BOOLEAN, enable);
		}
		else {
			property.setValue(OSDE.STRING_EMPTY + enable);
		}
		this.isChangePropery = true;
	}
	
	/**
	 * set a new desktop type description
	 * @param desktopType
	 * @param newDescription
	 */
	public void setDesktopTypeDesription(DesktopPropertyTypes desktopType, String newDescription) {
		this.getDesktopProperty(desktopType).setDescription(newDescription);
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
	public void setChannelName(String channelConfigName, int channelNumber) {
		this.isChangePropery = true;
		this.deviceProps.getChannel().get(channelNumber - 1).setName(channelConfigName);
	}

	/**
	 * @return the channel type by given channel number 
	 */
	public ChannelType getChannelType(int channelConfigNumber) {
		return this.deviceProps.getChannel().get(channelConfigNumber - 1);
	}

	/**
	 * @return the channel types by given channel number 
	 */
	public ChannelTypes getChannelTypes(int channelConfigNumber) {
		return this.deviceProps.getChannel().get(channelConfigNumber - 1).getType();
	}
	
	/**
	 * @return the channel types by given channel configuration key (name)
	 */
	@Deprecated
	public ChannelTypes getChannelTypes(String channelConfigKey) {
		return this.getChannel(channelConfigKey).getType();
	}

	/**
	 * @return the channel type by given channel number 
	 */
	public void setChannelTypes(ChannelTypes newChannleType, int channelNumber) {
		this.isChangePropery = true;
		this.deviceProps.getChannel().get(channelNumber - 1).setType(newChannleType);
	}
	
	/**
	 * add a new channel/config type
	 * @param newChannelType
	 */
	public void addChannelType(ChannelType newChannelType) {
		this.isChangePropery = true;
		this.deviceProps.getChannel().add(newChannelType);
	}
	
	/**
	 * remove a channel/configuration type at index
	 * @param channelTypeIndex
	 */
	public void removeChannelType(int channelNumber) {
		this.isChangePropery = true;
		this.deviceProps.getChannel().remove(channelNumber - 1);
	}
	
	/**
	 * @return the channel measurements by given channel configuration key (name)
	 */
	public List<MeasurementType> getChannelMeasuremts(int channelConfigNumber) {
		return this.getChannel(channelConfigNumber).getMeasurement();
	}
	
	/**
	 * @return the channel measurements by given channel configuration key (name)
	 */
	@Deprecated
	public List<MeasurementType> getChannelMeasuremts(String channelConfigKey) {
		return this.getChannel(channelConfigKey).getMeasurement();
	}
	
	/**
	 * @return the number of measurements of a channel by given channel number
	 */
	public int getNumberOfMeasurements(int channelConfigNumber) {
		return this.getChannel(channelConfigNumber).getMeasurement().size();
	}

	/**
	 * @return the number of measurements of a channel by given channel configuration key (name)
	 */
	@Deprecated
	public int getNumberOfMeasurements(String channelConfigKey) {
		return this.getChannel(channelConfigKey).getMeasurement().size();
	}
	
	/**
	 * add (append) a new MeasurementType object to channel with channel number as given
	 * @param channelNumber
	 * @param newMeasurementType
	 */
	public void addMeasurement2Channel(int channelNumber, MeasurementType newMeasurementType) {
		this.isChangePropery = true;
		this.getChannel(channelNumber).measurement.add(newMeasurementType);
	}

	/**
	 * add (append) a new MeasurementType object to channel with channel/config key as given
	 * @param channelConfigKey
	 * @param newMeasurementType
	 */
	@Deprecated
	public void addMeasurement2Channel(String channelConfigKey, MeasurementType newMeasurementType) {
		this.isChangePropery = true;
		this.getChannel(channelConfigKey).measurement.add(newMeasurementType);
	}
	
	/**
	 * remove a MeasurementType object from channel with channel number as given
	 * @param channelConfigNumber
	 * @param newMeasurementType
	 */
	public void removeMeasurementFromChannel(int channelConfigNumber, MeasurementType newMeasurementType) {
		this.isChangePropery = true;
		this.getChannel(channelConfigNumber).measurement.remove(newMeasurementType);
	}
	
	/**
	 * remove a MeasurementType object from channel with channel/config key as given
	 * @param channelConfigKey
	 * @param newMeasurementType
	 */
	@Deprecated
	public void removeMeasurementFromChannel(String channelConfigKey, MeasurementType newMeasurementType) {
		this.getChannel(channelConfigKey).measurement.remove(newMeasurementType);
	}

	/**
	 * get the channel type by given channel configuration key (name)
	 * @param channelConfigNumber
	 * @return
	 */
	public ChannelType getChannel(int channelConfigNumber) {
		return this.deviceProps.getChannel().get(channelConfigNumber - 1);
	}

	/**
	 * get the channel type by given channel configuration key (name)
	 * @param channelConfigKey
	 * @return
	 */
	@Deprecated
	public ChannelType getChannel(String channelConfigKey) {
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
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param isActive
	 */
	public void setMeasurementActive(int channelConfigNumber, int measurementOrdinal, boolean isActive) {
		log.log(Level.FINER, "channelConfigNumber = \"" + channelConfigNumber + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigNumber)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.isChangePropery = true;
		this.getMeasurement(channelConfigNumber, measurementOrdinal).setActive(isActive); //$NON-NLS-1$
	}

	/**
	 * set active status of an measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param isActive
	 */
	@Deprecated
	public void setMeasurementActive(String channelConfigKey, int measurementOrdinal, boolean isActive) {
		log.log(Level.FINER, "channelKey = \"" + channelConfigKey + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigKey)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.isChangePropery = true;
		this.getMeasurement(channelConfigKey, measurementOrdinal).setActive(isActive); //$NON-NLS-1$
	}

	/**
	 * get the measurement to get/set measurement specific parameter/properties
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return MeasurementType
	 */
	public MeasurementType getMeasurement(int channelConfigNumber, int measurementOrdinal) {
		return this.getChannel(channelConfigNumber).getMeasurement().get(measurementOrdinal);
	}

	/**
	 * get the measurement to get/set measurement specific parameter/properties
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return MeasurementType
	 */
	@Deprecated
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
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return List of properties to according measurement
	 */
	public List<PropertyType> getProperties(int channelConfigNumber, int measurementOrdinal) {
		List<PropertyType> list = new ArrayList<PropertyType>();
		MeasurementType measurement = this.getMeasurement(channelConfigNumber, measurementOrdinal);
		if (measurement != null)
			list = measurement.getProperty();
		return list;
	}
	
	/**
	 * get the properties from a channel/configuration and record key name 
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return List of properties to according measurement
	 */
	@Deprecated
	public List<PropertyType> getProperties(String channelConfigKey, int measurementOrdinal) {
		List<PropertyType> list = new ArrayList<PropertyType>();
		MeasurementType measurement = this.getMeasurement(channelConfigKey, measurementOrdinal);
		if (measurement != null)
			list = measurement.getProperty();
		return list;
	}

	/**
	 * add a property to a given list of PropertyTypes
	 * @param properties
	 * @param propertyKey
	 * @param type
	 * @param value
	 */
	public static void addProperty(List<PropertyType> properties, String propertyKey, DataTypes type, Object value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(propertyKey);
		newProperty.setType(type);
		newProperty.setValue(OSDE.STRING_EMPTY + value);
		properties.add(newProperty);
	}
	
	/**
	 * set new name of specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param name
	 */
	public void setMeasurementName(int channelConfigNumber, int measurementOrdinal, String name) {
		log.log(Level.FINER, "channelConfigNumber = \"" + channelConfigNumber + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigNumber)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.isChangePropery = true;
		this.getMeasurement(channelConfigNumber, measurementOrdinal).setName(name);
	}
	
	/**
	 * set new name of specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param name
	 */
	@Deprecated
	public void setMeasurementName(String channelConfigKey, int measurementOrdinal, String name) {
		log.log(Level.FINER, "channelKey = \"" + channelConfigKey + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigKey)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.isChangePropery = true;
		this.getMeasurement(channelConfigKey, measurementOrdinal).setName(name);
	}
	
	/**
	 * method to query the unit of measurement data unit by a given record key
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return dataUnit as string
	 */
	public String getMeasurementUnit(int channelConfigNumber, int measurementOrdinal) {
		log.log(Level.FINER, "channelConfigNumber = \"" + channelConfigNumber + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigNumber)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return this.getMeasurement(channelConfigNumber, measurementOrdinal).getUnit(); //$NON-NLS-1$
	}
	
	/**
	 * method to query the unit of measurement data unit by a given record key
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return dataUnit as string
	 */
	@Deprecated
	public String getMeasurementUnit(String channelConfigKey, int measurementOrdinal) {
		log.log(Level.FINER, "channelKey = \"" + channelConfigKey + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigKey)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return this.getMeasurement(channelConfigKey, measurementOrdinal).getUnit(); //$NON-NLS-1$
	}

	/**
	 * method to set the unit of measurement by a given measurement key
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param unit
	 */
	public void setMeasurementUnit(int channelConfigNumber, int measurementOrdinal, String unit) {
		log.log(Level.FINER, "channelConfigNumber = \"" + channelConfigNumber + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigNumber)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.isChangePropery = true;
		this.getMeasurement(channelConfigNumber, measurementOrdinal).setUnit(unit);
	}

	/**
	 * method to set the unit of measurement by a given measurement key
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param unit
	 */
	@Deprecated
	public void setMeasurementUnit(String channelConfigKey, int measurementOrdinal, String unit) {
		log.log(Level.FINER, "channelKey = \"" + channelConfigKey + "\" measurementKey = \"" + this.getMeasurementNames(channelConfigKey)[measurementOrdinal] + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.isChangePropery = true;
		this.getMeasurement(channelConfigKey, measurementOrdinal).setUnit(unit);
	}
	
	/**
	 * get the symbol of specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return the measurement symbol as string
	 */
	public String getMeasurementSymbol(int channelConfigNumber, int measurementOrdinal) {
		return this.getMeasurement(channelConfigNumber, measurementOrdinal).getSymbol();
	}
	
	/**
	 * get the symbol of specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return the measurement symbol as string
	 */
	@Deprecated
	public String getMeasurementSymbol(String channelConfigKey, int measurementOrdinal) {
		return this.getMeasurement(channelConfigKey, measurementOrdinal).getSymbol();
	}

	/**
	 * set new symbol of specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param symbol
	 */
	public void setMeasurementSymbol(int channelConfigNumber, int measurementOrdinal, String symbol) {
		this.isChangePropery = true;
		this.getMeasurement(channelConfigNumber, measurementOrdinal).setSymbol(symbol);
	}

	/**
	 * set new symbol of specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param symbol
	 */
	@Deprecated
	public void setMeasurementSymbol(String channelConfigKey, int measurementOrdinal, String symbol) {
		this.isChangePropery = true;
		this.getMeasurement(channelConfigKey, measurementOrdinal).setSymbol(symbol);
	}

	/**
	 * get the statistics type of the specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return statistics, if statistics does not exist return null
	 */
	public StatisticsType getMeasurementStatistic(int channelConfigNumber, int measurementOrdinal) {
		log.log(Level.FINER, "get statistics type from measurement = " + this.getMeasurement(channelConfigNumber, measurementOrdinal).getName());  //$NON-NLS-1$
		return this.getMeasurement(channelConfigNumber, measurementOrdinal).getStatistics();
	}

	/**
	 * get the statistics type of the specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return statistics, if statistics does not exist return null
	 */
	@Deprecated
	public StatisticsType getMeasurementStatistic(String channelConfigKey, int measurementOrdinal) {
		log.log(Level.FINER, "get statistics type from measurement = " + this.getMeasurement(channelConfigKey, measurementOrdinal).getName());  //$NON-NLS-1$
		return this.getMeasurement(channelConfigKey, measurementOrdinal).getStatistics();
	}

	/**
	 * remove the statistics type of the specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return statistics, if statistics does not exist return null
	 */
	public void removeStatisticsTypeFromMeasurement(int channelConfigNumber, int measurementOrdinal) {
		log.log(Level.FINER, "remove statistics type from measurement = " + this.getMeasurement(channelConfigNumber, measurementOrdinal).getName());  //$NON-NLS-1$
		this.getMeasurement(channelConfigNumber, measurementOrdinal).setStatistics(null);
	}
	
	/**
	 * @return the sorted measurement names
	 */
	public String[] getMeasurementNames(int channelConfigNumber) {
		StringBuilder sb = new StringBuilder();
		ChannelType channel = this.getChannel(channelConfigNumber);
		if (channel != null) {
			List<MeasurementType> measurement = channel.getMeasurement();
			for (MeasurementType measurementType : measurement) {
				sb.append(measurementType.getName()).append(OSDE.STRING_SEMICOLON);
			}
		}
		return sb.toString().length()>1 ? sb.toString().split(OSDE.STRING_SEMICOLON) : new String[0];
	}
	
	/**
	 * @return the sorted measurement names
	 */
	@Deprecated
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
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @return PropertyType
	 */
	public PropertyType getMeasruementProperty(int channelConfigNumber, int measurementOrdinal, String propertyKey) {
		PropertyType property = null;
		try {
			MeasurementType measurementType = this.getMeasurement(channelConfigNumber, measurementOrdinal);
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
			log.log(Level.SEVERE, channelConfigNumber + " - " + this.getMeasurementNames(channelConfigNumber)[measurementOrdinal] + " - " + propertyKey, e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return property;
	}

	/**
	 * get property with given channel configuration key, measurement key and property type key (IDevice.OFFSET, ...)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @return PropertyType
	 */
	@Deprecated
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
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return the offset, if property does not exist return 0.0 as default value
	 */
	public double getMeasurementOffset(int channelConfigNumber, int measurementOrdinal) {
		log.log(Level.FINER, "get offset from measurement name = " + this.getMeasurement(channelConfigNumber, measurementOrdinal).getName());  //$NON-NLS-1$
		double value = 0.0;
		PropertyType property = this.getMeasruementProperty(channelConfigNumber, measurementOrdinal, IDevice.OFFSET);
		if (property != null)
			value = new Double(property.getValue()).doubleValue();
		
		return value;
	}

	/**
	 * get the offset value of the specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return the offset, if property does not exist return 0.0 as default value
	 */
	@Deprecated
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
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param offset the offset to set
	 */
	public void setMeasurementOffset(int channelConfigNumber, int measurementOrdinal, double offset) {
		log.log(Level.FINER, "set offset onto measurement name = " + this.getMeasurement(channelConfigNumber, measurementOrdinal).getName());  //$NON-NLS-1$
		PropertyType property = this.getMeasruementProperty(channelConfigNumber, measurementOrdinal, IDevice.OFFSET);
		if (property == null) {
			createProperty(channelConfigNumber, measurementOrdinal, IDevice.OFFSET, DataTypes.DOUBLE, offset);
		}
		else {
			property.setValue(OSDE.STRING_EMPTY + offset);
		}
	}

	/**
	 * set new value for offset at the specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param offset the offset to set
	 */
	@Deprecated
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
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return the factor, if property does not exist return 1.0 as default value
	 */
	public double getMeasurementFactor(int channelConfigNumber, int measurementOrdinal) {
		log.log(Level.FINER, "get factor from measurement name = " + this.getMeasurement(channelConfigNumber, measurementOrdinal).getName());  //$NON-NLS-1$
		double value = 1.0;
		PropertyType property = getMeasruementProperty(channelConfigNumber, measurementOrdinal, IDevice.FACTOR);
		if (property != null)
			value = new Double(property.getValue()).doubleValue();
		
		return value;
	}

	/**
	 * get the factor value of the specified measurement (offset + (value - reduction) * factor)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return the factor, if property does not exist return 1.0 as default value
	 */
	@Deprecated
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
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param factor the offset to set
	 */
	public void setMeasurementFactor(int channelConfigNumber, int measurementOrdinal, double factor) {
		log.log(Level.FINER, "set factor onto measurement name = " + this.getMeasurement(channelConfigNumber, measurementOrdinal).getName());  //$NON-NLS-1$
		PropertyType property = this.getMeasruementProperty(channelConfigNumber, measurementOrdinal, IDevice.FACTOR);
		if (property == null) {
			createProperty(channelConfigNumber, measurementOrdinal, IDevice.FACTOR, DataTypes.DOUBLE, factor);
		}
		else {
			property.setValue(OSDE.STRING_EMPTY + factor);
		}
	}

	/**
	 * set new value for factor at the specified measurement (offset + (value - reduction) * factor)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param factor the offset to set
	 */
	@Deprecated
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
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return the reduction, if property does not exist return 0.0 as default value
	 */
	public double getMeasurementReduction(int channelConfigNumber, int measurementOrdinal) {
		log.log(Level.FINER, "get reduction from measurement name = " + this.getMeasurement(channelConfigNumber, measurementOrdinal).getName());  //$NON-NLS-1$
		double value = 0.0;
		PropertyType property = getMeasruementProperty(channelConfigNumber, measurementOrdinal, IDevice.REDUCTION);
		if (property != null)
			value = new Double(property.getValue()).doubleValue();
		
		return value;
	}

	/**
	 * get the reduction value of the specified measurement (offset + (value - reduction) * factor)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return the reduction, if property does not exist return 0.0 as default value
	 */
	@Deprecated
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
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param reduction of the direct measured value
	 */
	public void setMeasurementReduction(int channelConfigNumber, int measurementOrdinal, double reduction) {
		log.log(Level.FINER, "set reduction onto measurement name = " + this.getMeasurement(channelConfigNumber, measurementOrdinal).getName());  //$NON-NLS-1$
		PropertyType property = this.getMeasruementProperty(channelConfigNumber, measurementOrdinal, IDevice.REDUCTION);
		if (property == null) {
			createProperty(channelConfigNumber, measurementOrdinal, IDevice.REDUCTION, DataTypes.DOUBLE, reduction);
		}
		else {
			property.setValue(OSDE.STRING_EMPTY + reduction);
		}
	}

	/**
	 * set new value for factor at the specified measurement (offset + (value - reduction) * factor)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param reduction of the direct measured value
	 */
	@Deprecated
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
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @return the property from measurement defined by key, if property does not exist return 1 as default value
	 */
	public Object getMeasurementPropertyValue(int channelConfigNumber, int measurementOrdinal, String propertyKey) {
		PropertyType property = this.getMeasruementProperty(channelConfigNumber, measurementOrdinal, propertyKey);
		return property != null ? property.getValue() : OSDE.STRING_EMPTY;
	}

	/**
	 * get a property of specified measurement, the data type must be known - data conversion is up to implementation
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @return the property from measurement defined by key, if property does not exist return 1 as default value
	 */
	@Deprecated
	public Object getMeasurementPropertyValue(String channelConfigKey, int measurementOrdinal, String propertyKey) {
		PropertyType property = this.getMeasruementProperty(channelConfigKey, measurementOrdinal, propertyKey);
		return property != null ? property.getValue() : OSDE.STRING_EMPTY;
	}
	
	/**
	 * set new property value of specified measurement, if the property does not exist it will be created
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @param type of DataTypes
	 * @param value
	 */
	public void setMeasurementPropertyValue(int channelConfigNumber, int measurementOrdinal, String propertyKey, DataTypes type, Object value) {
		this.isChangePropery = true;
		PropertyType property = this.getMeasruementProperty(channelConfigNumber, measurementOrdinal, propertyKey);
		if (property == null) {
			createProperty(channelConfigNumber, measurementOrdinal, propertyKey, type, (OSDE.STRING_EMPTY + value).replace(OSDE.STRING_COMMA, OSDE.STRING_DOT)); //$NON-NLS-1$
		}
		else {
			property.setValue((OSDE.STRING_EMPTY + value).replace(OSDE.STRING_COMMA, OSDE.STRING_DOT));
		}
	}
	
	/**
	 * set new property value of specified measurement, if the property does not exist it will be created
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @param type of DataTypes
	 * @param value
	 */
	@Deprecated
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
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @param type
	 * @param value
	 */
	private void createProperty(int channelConfigNumber, int measurementOrdinal, String propertyKey, DataTypes type, Object value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(propertyKey);
		newProperty.setType(type);
		newProperty.setValue(OSDE.STRING_EMPTY + value);
		this.getMeasurement(channelConfigNumber, measurementOrdinal).getProperty().add(newProperty);
	}

	/**
	 * create a measurement property
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @param type
	 * @param value
	 */
	@Deprecated
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
		if (application.getMenuBar() != null)	application.getMenuBar().setSerialPortIconSet(useIconSet);
		if (application.getMenuToolBar() != null)	application.getMenuToolBar().setSerialPortIconSet(useIconSet);
	}

	/**
	 * @return the calculationThread
	 */
	public CalculationThread getCalculationThread() {
		return this.calculationThread;
	}

}
