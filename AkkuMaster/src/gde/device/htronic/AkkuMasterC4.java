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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.ui.OpenSerialDataExplorer;

/**
 * AkkuMaster C4 device class implementation
 * @author Winfried Brügmann
 */
public class AkkuMasterC4 extends DeviceConfiguration implements IDevice {
	final static Logger														log									= Logger.getLogger(AkkuMasterC4.class.getName());

	final OpenSerialDataExplorer									application;
	final AkkuMasterC4Dialog											dialog;
	final AkkuMasterC4SerialPort									serialPort;
	final Channels																channels;
	HashMap<String, AkkuMasterCalculationThread>	calculationThreads	= new HashMap<String, AkkuMasterCalculationThread>();

	/**
	 * constructor using the device properties file for initialization
	 * @param deviceProperties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 * @throws NoSuchPortException 
	 */
	public AkkuMasterC4(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new AkkuMasterC4SerialPort(this, this.application);
		this.dialog = new AkkuMasterC4Dialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 * @throws NoSuchPortException 
	 */
	public AkkuMasterC4(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new AkkuMasterC4SerialPort(this, this.application);
		this.dialog = new AkkuMasterC4Dialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double translateValue(@SuppressWarnings("unused")
	String configKey, String recordKey, double value) {
		double newValue = value;
		if (AkkuMasterC4.log.isLoggable(Level.FINEST)) AkkuMasterC4.log.finest(String.format("input value for %s - %f", recordKey, value));
		if (AkkuMasterC4.log.isLoggable(Level.FINEST)) AkkuMasterC4.log.finest(String.format("value calculated for %s - %f", recordKey, newValue));
		return newValue;
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(@SuppressWarnings("unused")
	String configKey, String recordKey, double value) {
		double newValue = value;
		if (AkkuMasterC4.log.isLoggable(Level.FINEST)) AkkuMasterC4.log.finest(String.format("input value for %s - %f", recordKey, value));
		if (AkkuMasterC4.log.isLoggable(Level.FINEST)) AkkuMasterC4.log.finest(String.format("value calculated for %s - %f", recordKey, newValue));
		return newValue;
	}

	/**
	 * function to calculate values for inactive or to be calculated records
	 * @param recordSet
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are measurement point every 10 seconds during capturing only and the calculation will take place directly switch all to displayable
		if (recordSet.isRaw()) {
			// calculate the values required
			try {
				String[] recordNames = this.getMeasurementNames(recordSet.getChannelName());
				for (String recordKey : recordNames) {
					MeasurementType measurement = this.getMeasurement(recordSet.getChannelName(), recordKey);
					if (measurement.isCalculation()) {
						AkkuMasterC4.log.fine(recordKey);
						this.calculationThreads.put(recordKey, new AkkuMasterCalculationThread(recordKey, this.channels.getActiveChannel().getActiveRecordSet()));
						this.calculationThreads.get(recordKey).start();
					}
				}
			}
			catch (RuntimeException e) {
				AkkuMasterC4.log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * @return the device dialog
	 */
	public AkkuMasterC4Dialog getDialog() {
		return this.dialog;
	}

	/**
	 * @return the device serialPort
	 */
	public AkkuMasterC4SerialPort getSerialPort() {
		return this.serialPort;
	}

}
