/**
 * 
 */
package osde.device.bantam;

import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import osde.device.DeviceConfiguration;
import osde.device.IDevice;

/**
 * Sample device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class eStationBC610 extends eStation implements IDevice {
	final static Logger						log	= Logger.getLogger(eStationBC610.class.getName());

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public eStationBC610(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		this.dialog = new EStationDialog(this.application.getShell(), this);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public eStationBC610(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		this.dialog = new EStationDialog(this.application.getShell(), this);
	}

	/**
	 * @return the dialog
	 */
	public EStationDialog getDialog() {
		return this.dialog;
	}
}
