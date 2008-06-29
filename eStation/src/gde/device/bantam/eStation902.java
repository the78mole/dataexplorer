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
public class eStation902 extends eStation implements IDevice {
	final static Logger						log	= Logger.getLogger(eStation902.class.getName());

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public eStation902(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		this.dialog = new EStationDialog(this.application.getShell(), this);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public eStation902(DeviceConfiguration deviceConfig) {
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
