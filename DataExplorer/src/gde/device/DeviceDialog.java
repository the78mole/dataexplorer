/**
 * 
 */
package osde.device;

import osde.common.RecordSet;

/**
 * defines the interface for device dialog implementation
 */
public interface DeviceDialog {
	
	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	public void open();
	
	/**
	 * method to close the dialog, overwrite this if extra cleanup is required
	 */
	public void close();

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
