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

import java.util.List;

import osde.data.Record;
import osde.data.RecordSet;
import osde.serial.DeviceSerialPort;

/**
 * Defines the interface for all device implementations, it also covers some interface methods from 
 * DeviceDialog as well as DeviceSerialPort
 * @author Winfried Brügmann
 */
public interface IDevice {
	// define some global constants for data calculation 
	public static final String 	OFFSET 		= "offset";
	public static final String	FACTOR 		= "factor";
	public static final String	REDUCTION = "reduction";
	
	/**
	 * @return the device dialog
	 */
	public DeviceDialog getDialog();
	
	/**
	 * @return the device serial port
	 */
	public DeviceSerialPort getSerialPort();
		
	/**
	 * @return the device name
	 */
	public String	getName();
	
	/**
	 * @return usage device state
	 */
	public boolean isUsed();
	
	/**
	 * @return device manufacturer
	 */
	public String getManufacturer();
	
	/**
	 * @return device group
	 */
	public String getDeviceGroup();
	
	/**
	 * @return link to manufacturer
	 */
	public String getManufacturerURL();
	
	/**
	 * query if the table tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isTableTabRequested();
	
	/**
	 * set the DesktopType.TYPE_TABLE_TAB property to the given value
	 * @param enable
	 */
	public void setTableTabRequested(boolean enable);
	
	/**
	 * query if the digital tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isDigitalTabRequested();
	
	/**
	 * set the DesktopType.TYPE_DIGITAL_TAB property to the given value
	 * @param enable
	 */
	public void setDigitalTabRequested(boolean enable);
	
	/**
	 * query if the analog tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isAnalogTabRequested();
	
	/**
	 * set the DesktopType.TYPE_ANALOG_TAB property to the given value
	 * @param enable
	 */
	public void setAnalogTabRequested(boolean enable);
	
	/**
	 * query if the voltage per cell tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isVoltagePerCellTabRequested();
	
	/**
	 * set the DesktopType.TYPE_VOLTAGE_PER_CELL_TAB property to the given value
	 * @param enable
	 */
	public void setVoltagePerCellTabRequested(boolean enable);

	/**
	 * @return time step in ms
	 */
	public Double getTimeStep_ms();
	
	/**
	 * set new time step in ms
	 */
	public void setTimeStep_ms(double newTimeStep_ms);
	
	/**
	 * @return the port configured for the device
	 */
	public String getPort();
	
	/**
	 * @param newPort - set a new port string for the device
	 */
	public void setPort(String newPort);
	
	/**
	 * @return the baude rate of the device
	 */
	public int getBaudeRate();
	
	/**
	 * @return the data bit configuration of the device
	 */
	public int getDataBits();
	
	/**
	 * @return the stop bit configuration of the device
	 */
	public int getStopBits();
	
	/**
	 * @return the flow control configuration of the device
	 */
	public int getFlowCtrlMode();
	
	/**
	 * @return the parity bit configuration of the device
	 */
	public int getParity();
	
	/**
	 * @return  the DTR configuration of the device
	 */
	public boolean isDTR();
	
	/**
	 * @return  the RTS configuration of the device
	 */
	public boolean isRTS();
	
	/**
	 * @return the channel count
	 */
	public int getChannelCount();

	/**
	 * @param channelNumber
	 * @return the channel name
	 */
	public String getChannelName(int channelNumber);
	
	/**
	 * @param channelName - size should not exceed 15 char length - this is the key to get access
	 * @param channelNumber
	 */
	public void setChannelName(String channelName, int channelNumber);

	/**
	 * @param channelNumber (starts at 1)
	 * @return the channel type
	 */
	public int getChannelType(int channelNumber);
	
	/**
	 * @return the channel measurements by given channel configuration key (name)
	 */
	public List<MeasurementType> getChannelMeasuremts(String channelConfigKey);

	/**
	 * get the properties from a channel/configuration and record key name 
	 * @param channelConfigKey
	 * @param measurementKey
	 * @return
	 */
	public List<PropertyType> getProperties(String channelConfigKey, String measurementKey);

	/**
	 * @return the number of measurements of a channel, assume channels have different number of measurements
	 */
	public int getNumberOfMeasurements(String channelConfigKey);
	
	/**
	 * get the measurement to get/set measurement specific parameter/properties (voltage, current, height, slope, ..)
	 * @param channelConfigKey
	 * @param measurementKey
	 * @return PropertyType object
	 */
	public MeasurementType getMeasurement(String channelConfigKey, String measurementKey);
	
	/**
	 * @return the sorted measurement names
	 */
	public String[] getMeasurementNames(String channelConfigKey);

	/**
	 * set new name of specified measurement
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param name
	 */
	public void setMeasurementName(String channelConfigKey, String measurementKey, String name);

	/**
	 * method to query the unit of measurement data unit by a given record key
	 * @param channelConfigKey
	 * @param measurementKey
	 * @return dataUnit as string
	 */
	public String getMeasurementUnit(String channelConfigKey, String measurementKey);

	/**
	 * method to set the unit of measurement by a given measurement key
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param unit
	 */
	public void setMeasurementUnit(String channelConfigKey, String measurementKey, String unit);

	/**
	 * get the symbol of specified measurement
	 * @param channelConfigKey
	 * @param measurementKey
	 */
	public String getMeasurementSymbol(String channelConfigKey, String measurementKey);

	/**
	 * set new symbol of specified measurement
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param symbol
	 */
	public void setMeasurementSymbol(String channelConfigKey, String measurementKey, String symbol);

	/**
	 * get property with given channel configuration key, measurement key and property type key (IDevice.OFFSET, ...)
	 * @param channelConfigKey
	 * @param measurementkey
	 * @param propertyKey
	 * @return PropertyType object
	 */
	public PropertyType getMeasruementProperty(String channelConfigKey, String measurementkey, String propertyKey);

	/**
	 * get the offset value of the specified measurement
	 * @param channelConfigKey
	 * @param measurementKey
	 * @return the offset, if property does not exist return 0.0 as default value
	 */
	public double getMeasurementOffset(String channelConfigKey, String measurementKey);

	/**
	 * set new value for offset at the specified measurement
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param offset the offset to set
	 */
	public void setMeasurementOffset(String channelConfigKey, String measurementKey, double offset);

	/**
	 * get the factor value of the specified measurement
	 * @param channelConfigKey
	 * @param measurementKey
	 * @return the factor, if property does not exist return 1.0 as default value
	 */
	public double getMeasurementFactor(String channelConfigKey, String measurementKey);

	/**
	 * set new value for factor at the specified measurement
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param factor the offset to set
	 */
	public void setMeasurementFactor(String channelConfigKey, String measurementKey, double factor);

	/**
	 * get a property of specified measurement, the data type must be known - data conversion is up to implementation
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param propertyKey
	 * @return the property from measurement defined by key, if property does not exist return 1 as default value
	 */
	public Object getMeasurementPropertyValue(String channelConfigKey, String measurementKey, String propertyKey);
	/**
	 * set new property value of specified measurement, if the property does not exist it will be created
	 * @param channelConfigKey
	 * @param measurementKey
	 * @param propertyKey
	 * @param type of DataTypes
	 * @param value
	 */
	public void setMeasurementPropertyValue(String channelConfigKey, String measurementKey, String propertyKey, DataTypes type, Object value);
	
	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double translateValue(Record record, double value);

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(Record record, double value);

	/**
	 * function to calculate values for inactive which need to be calculated records
	 */
	public void makeInActiveDisplayable(RecordSet recordSet);

	/**
	 * @param isChangePropery the isChangePropery to set
	 */
	public void setChangePropery(boolean isChangePropery);

	/**
	 * writes updated device properties XML if isChangePropery == true;
	 */
	public void storeDeviceProperties();
}
