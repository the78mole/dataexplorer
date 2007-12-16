/**
 * 
 */
package osde.device;

import java.util.HashMap;

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
	public String getHersteller();
	
	/**
	 * @return the record definitions
	 */
	public HashMap<String, Object> getConfiguredRecords();
	
	/**
	 * @return device group
	 */
	public String getDeviceGroup();
	
	/**
	 * @return link to manufacturer
	 */
	public String getHerstellerLink1();
	
	/**
	 * @return link to OpenSerialExplorer home page
	 */
	public String getOpenSerialDataExplorerLink();
	
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
	public int getBaude();
	
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
	public int getFlowCtrl();
	
	/**
	 * @return the parity bit configuration of the device
	 */
	public int getParity();
	
	/**
	 * @return  the DTR configuration of the device
	 */
	public boolean isDtr();
	
	/**
	 * @return  the RTS configuration of the device
	 */
	public boolean isRts();
	
	/**
	 * @return the numberRecords
	 */
	public int getNumberRecords();
	
	/**
	 * @return the channelCount
	 */
	public int getChannelCount();
	
	/**
	 * @return the device dialog
	 */
	public DeviceDialog getDialog();
	
	/**
	 * @return the device serial port
	 */
	public DeviceSerialPort getSerialPort();
	
	/**
	 * @return the records this is the key to get access to measurement specific properties
	 */
	public HashMap<String, Object> getRecords();
	
	/**
	 * @return the dataUnit
	 */
	public String getDataUnit(String recordKey);

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

}
