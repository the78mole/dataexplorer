/**
 * 
 */
package osde.device;

import osde.data.RecordSet;

/**
 * defines the interface for device dialog implementation
 */
public interface IDevice {
	
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
	 * @param set a new port string for the device
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
	 * @return the number of measurements of a channel
	 */
	public int getNumberOfMeasurements();
	
	/**
	 * @return the measurement definitions matching key (voltage, current, ...)
	 */
	public MeasurementType getMeasurementDefinition(String recordKey);
	
	/**
	 * @return the sorted measurement names
	 */
	public String[] getMeasurementNames();

		/**
	 * @return the device dialog
	 */
	public DeviceDialog getDialog();
	
	/**
	 * @return the device serial port
	 */
	public DeviceSerialPort getSerialPort();
	
	/**
	 * @return the dataUnit
	 */
	public String getDataUnit(String recordKey);

	/**
	 * @return the offset
	 */
	public double getOffset(String recordKey);

	/**
	 * @param offset the offset to set
	 */
	public void setOffset(String recordKey, double offset);

	/**
	 * @return the factor
	 */
	public double getFactor(String recordKey);

	/**
	 * @param factor the factor to set
	 */
	public void setFactor(String recordKey, double factor);

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double translateValue(String recordKey, double value);

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(String recordKey, double value);

	/**
	 * function to calculate values for inactive records
	 * @return double with the adapted value
	 */
	public void makeInActiveDisplayable(RecordSet recordSet);

	/**
	 * writes updated device properties XML
	 */
	public void store();
}
