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
package osde.device.htronic;

import gnu.io.NoSuchPortException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.ui.OpenSerialDataExplorer;

/**
 * @author Winfried Brügmann
 * AkkuMaster C4 device class implementation
 */
public class AkkuMasterC4 extends DeviceConfiguration implements IDevice {
	private Logger									log										= Logger.getLogger(this.getClass().getName());

	private final OpenSerialDataExplorer application;
	private final AkkuMasterC4Dialog	dialog;
	private final AkkuMasterC4SerialPort serialPort;
	private final Channels channels;
	private AkkuMasterCalculationThread	threadPower, threadEnergy;

	/**
	 * constructor using the device properties file for initialization
	 * @param deviceProperties file
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NoSuchPortException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public AkkuMasterC4(String deviceProperties) throws FileNotFoundException, IOException, NoSuchPortException, ParserConfigurationException, SAXException {
		super(deviceProperties);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new AkkuMasterC4SerialPort(this, application.getStatusBar());
		this.dialog = new AkkuMasterC4Dialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
	}

	/**
	 * constructor using existing device configuration
	 * @param device configuration
	 * @throws NoSuchPortException 
	 */
	public AkkuMasterC4(DeviceConfiguration deviceConfig) throws NoSuchPortException {
		super(deviceConfig);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new AkkuMasterC4SerialPort(this, application.getStatusBar());
		this.dialog = new AkkuMasterC4Dialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double translateValue(String recordKey, double value) {
		double newValue = 0;
		if(log.isLoggable(Level.FINEST)) log.finest(String.format("input value for %s - %f", recordKey, value));
		if (recordKey.startsWith(RecordSet.VOLTAGE)) {
			newValue = value;
		}
		else if (recordKey.startsWith(RecordSet.CURRENT)) {
			newValue = value;
		}
		else if (recordKey.startsWith(RecordSet.CHARGE)) {
			newValue = value;
		}
		else if (recordKey.startsWith(RecordSet.POWER)) {
			newValue = value;
		}
		else if (recordKey.startsWith(RecordSet.ENERGY)) {
			newValue = value;
		}
		if(log.isLoggable(Level.FINEST)) log.finest(String.format("value calculated for %s - %f", recordKey, newValue));
		return newValue;
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(String recordKey, double value) {
		double newValue = 0;
		if(log.isLoggable(Level.FINEST)) log.finest(String.format("input value for %s - %f", recordKey, value));
		if (recordKey.startsWith(RecordSet.VOLTAGE)) {
			newValue = value;
		}
		else if (recordKey.startsWith(RecordSet.CURRENT)) {
			newValue = value;
		}
		else if (recordKey.startsWith(RecordSet.CHARGE)) {
			newValue = value;
		}
		else if (recordKey.startsWith(RecordSet.POWER)) {
			newValue = value;
		}
		else if (recordKey.startsWith(RecordSet.ENERGY)) {
			newValue = value;
		}
		if(log.isLoggable(Level.FINEST)) log.finest(String.format("value calculated for %s - %f", recordKey, newValue));
		return newValue;
	}

	/**
	 * function to calculate values for inactive records
	 * @return double with the adapted value
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are measurement point every 10 seconds during cpturing only and the calculation will take place directly switch all to displayable
		if (recordSet.isFromFile() && recordSet.isRaw()) {
			// calculate the values required
			try {
				threadPower = new AkkuMasterCalculationThread(RecordSet.POWER, channels.getActiveChannel().getActiveRecordSet());
				threadPower.start();
				threadEnergy = new AkkuMasterCalculationThread(RecordSet.ENERGY, channels.getActiveChannel().getActiveRecordSet());
				threadEnergy.start();
			}
			catch (RuntimeException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * @return the device dialog
	 */
	public AkkuMasterC4Dialog getDialog() {
		return dialog;
	}

	/**
	 * @return the device serialPort
	 */
	public AkkuMasterC4SerialPort getSerialPort() {
		return serialPort;
	}

}
