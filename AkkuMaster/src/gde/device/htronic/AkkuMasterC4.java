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
	private Logger									log										= Logger.getLogger(this.getClass().getName());

	private final OpenSerialDataExplorer application;
	private final AkkuMasterC4Dialog	dialog;
	private final AkkuMasterC4SerialPort serialPort;
	private final Channels channels;
	private HashMap<String, AkkuMasterCalculationThread>	calculationThreads = new HashMap<String, AkkuMasterCalculationThread>();

	/**
	 * constructor using the device properties file for initialization
	 * @param deviceProperties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 * @throws NoSuchPortException 
	 */
	public AkkuMasterC4(String deviceProperties) throws FileNotFoundException, JAXBException, NoSuchPortException {
		super(deviceProperties);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new AkkuMasterC4SerialPort(this, application.getStatusBar());
		this.dialog = new AkkuMasterC4Dialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
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
	public double translateValue(String configKey, String recordKey, double value) {
		double newValue = value;
		if(log.isLoggable(Level.FINEST)) log.finest(String.format("input value for %s - %f", recordKey, value));
		if(log.isLoggable(Level.FINEST)) log.finest(String.format("value calculated for %s - %f", recordKey, newValue));
		return newValue;
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(String configKey, String recordKey, double value) {
		double newValue = value;
		if(log.isLoggable(Level.FINEST)) log.finest(String.format("input value for %s - %f", recordKey, value));
		if(log.isLoggable(Level.FINEST)) log.finest(String.format("value calculated for %s - %f", recordKey, newValue));
		return newValue;
	}

	/**
	 * function to calculate values for inactive or to be calculated records
	 * @param recordSet
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are measurement point every 10 seconds during capturing only and the calculation will take place directly switch all to displayable
		if (recordSet.isFromFile() && recordSet.isRaw()) {
			// calculate the values required
			try {
				String[] recordNames = this.getMeasurementNames(recordSet.getChannelName());
				for (String recordKey : recordNames) {
					MeasurementType measurement = this.getMeasurement(recordSet.getChannelName(), recordKey);
					if (measurement.isCalculation()) {
						log.fine(recordKey);
						calculationThreads.put(recordKey, new AkkuMasterCalculationThread(recordKey, channels.getActiveChannel().getActiveRecordSet()));
						calculationThreads.get(recordKey).start();
					}
				}
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
