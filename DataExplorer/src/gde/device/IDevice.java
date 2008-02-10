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

import osde.data.RecordSet;

/**
 * Defines the interface for all device implementations, it also covers some interface methods from 
 * DeviceDialog as well as DeviceSerialPort
 * @author Winfried Brügmann
 */
public interface IDevice {
	// define some global constants for data calculation 
	public static final String 	OFFSET = "offset";
	public static final String	FACTOR = "factor";
	
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
	 * @return time step in ms
	 */
	public int getTimeStep_ms();
	
	/**
	 * set new time step in ms
	 */
	public void setTimeStep_ms(int newTimeStep_ms);
	
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
	 * @return the number of measurements of a channel, assume channels have different number of measurements
	 */
	public int getNumberOfMeasurements(String channelConfigKey);
	
	/**
	 * @return the measurement definitions matching key (voltage, current, ...)
	 */
	public MeasurementType getMeasurement(String channelConfigKey, String measurementKey);
	
	/**
	 * @return the property element of the measurement key (voltage, current, ...)
	 */
	public PropertyType getProperty(String channelConfigKey, String measurementkey, String propertyKey);

	/**
	 * @return the sorted measurement names
	 */
	public String[] getMeasurementNames(String channelConfigKey);

		/**
	 * @return the device dialog
	 */
	public DeviceDialog getDialog();
	
	/**
	 * @return the device serial port
	 */
	public DeviceSerialPort getSerialPort();
	
	/**
	 * @return the data unit of the specified measurement at the specified channel configuration
	 */
	public String getUnit(String channelConfigKey, String measurementKey);

	/**
	 * @set the data unit of the specified measurement at the specified channel configuration
	 */
	public void setUnit(String channelConfigKey, String measurementKey, String unit);

	/**
	 * @return the offset
	 */
	public double getOffset(String channelConfigKey, String measurementKey);

	/**
	 * @param offset the offset to set
	 */
	public void setOffset(String channelConfigKey, String measurementKey, double offset);

	/**
	 * @return the factor
	 */
	public double getFactor(String channelConfigKey, String measurementKey);

	/**
	 * @param factor the factor to set
	 */
	public void setFactor(String channelConfigKey, String measurementKey, double factor);

	/**
	 * set measurement property value 
	 * @param measurementkey
	 * @param propertyKey
	 * @param type
	 * @param value
	 */
	public void setPropertyValue(String channelConfigKey, String measurementkey, String propertyKey, DataTypes type, Object value);
	
	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double translateValue(String channelConfigKey, String measurementKey, double value);

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(String channelConfigKey, String measurementKey, double value);

	/**
	 * function to calculate values for inactive which need to be calculated records
	 */
	public void makeInActiveDisplayable(RecordSet recordSet);

	/**
	 * writes updated device properties XML
	 */
	public void store();
}
