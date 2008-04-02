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
package osde.device.renschler;

import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.device.PropertyType;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.CalculationThread;
import osde.utils.LinearRegression;
import osde.utils.QuasiLinearRegression;

/**
 * Picolariolog device main implementaion class
 * @author Winfried Brügmann
 */
public class Picolario extends DeviceConfiguration implements IDevice {
	final static Logger						log								= Logger.getLogger(Picolario.class.getName());

	public final static String		DO_NO_ADAPTION		= "do_no_adaption";
	public final static String		DO_OFFSET_HEIGHT	= "do_offset_height";
	public final static String		DO_SUBTRACT_FIRST	= "do_subtract_first";
	public final static String		DO_SUBTRACT_LAST	= "subtract_last";

	final OpenSerialDataExplorer	application;
	final PicolarioDialog					dialog;
	final PicolarioSerialPort			serialPort;
	final Channels								channels;
	CalculationThread							slopeCalculationThread;

	/**
	 * @param iniFile
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public Picolario(String iniFile) throws FileNotFoundException, JAXBException {
		super(iniFile);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new PicolarioSerialPort(this, this.application);
		this.dialog = new PicolarioDialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public Picolario(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new PicolarioSerialPort(this, this.application);
		this.dialog = new PicolarioDialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
	}

	/**
	 * function to translate measured value from a device to values represented (((value - reduction) * factor) + offset - firstLastAdaption)
	 * @return double with the adapted value
	 */
	public double translateValue(String channelConfigKey, String recordKey, double value) {
		if (Picolario.log.isLoggable(Level.FINEST)) Picolario.log.finest(String.format("input value for %s - %f", recordKey, value));

		String[] measurements = this.getMeasurementNames(channelConfigKey); // 0=Spannung, 1=Höhe, 2=Steigung
		double offset = this.getMeasurementOffset(channelConfigKey, recordKey); // != 0 if curve has an defined offset
		double reduction = this.getMeasurementReduction(channelConfigKey, recordKey);
		double factor = this.getMeasurementFactor(channelConfigKey, recordKey); // != 1 if a unit translation is required

		// height calculation need special procedure
		if (recordKey.startsWith(measurements[1])) { // 1=Höhe
			PropertyType property = this.getMeasurement(channelConfigKey, recordKey).getProperty(Picolario.DO_SUBTRACT_FIRST);
			boolean subtractFirst = property != null ? new Boolean(property.getValue()).booleanValue() : false;
			property = this.getMeasurement(channelConfigKey, recordKey).getProperty(Picolario.DO_SUBTRACT_LAST);
			boolean subtractLast = property != null ? new Boolean(property.getValue()).booleanValue() : false;

			if (subtractFirst) {
				// get the record set to be used
				RecordSet recordSet = this.channels.getActiveChannel().getActiveRecordSet();
				if (recordKey.substring(recordKey.length() - 2).startsWith("_")) recordSet = this.application.getCompareSet();

				reduction = recordSet.getRecord(recordKey).getFirst().intValue() / 1000;
			}
			else if (subtractLast) {
				// get the record set to be used
				RecordSet recordSet = this.channels.getActiveChannel().getActiveRecordSet();
				if (recordKey.substring(recordKey.length() - 2).startsWith("_")) recordSet = this.application.getCompareSet();

				reduction = recordSet.getRecord(recordKey).getLast().intValue() / 1000;
			}
			else
				reduction = 0;
		}

		// slope calculation needs height factor
		else if (recordKey.startsWith(measurements[2])) { // 2=Steigung
			factor = this.getMeasurementFactor(channelConfigKey, measurements[1]);
		}

		double newValue = offset + (value - reduction) * factor;

		if (Picolario.log.isLoggable(Level.FINER)) Picolario.log.finer(String.format("value calculated for %s - inValue %f - outValue %f", recordKey, value, newValue));
		return newValue;
	}

	/**
	 * function to translate measured value from a device to values represented (((value - offset + firstLastAdaption)/factor) + reduction)
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(String channelConfigKey, String recordKey, double value) {
		if (Picolario.log.isLoggable(Level.FINEST)) Picolario.log.finest(String.format("input value for %s - %f", recordKey, value));

		String[] measurements = this.getMeasurementNames(channelConfigKey); // 0=Spannung, 1=Höhe, 2=Steigung
		double offset = this.getMeasurementOffset(channelConfigKey, recordKey); // != 0 if curve has an defined offset
		double reduction = this.getMeasurementReduction(channelConfigKey, recordKey);
		double factor = this.getMeasurementFactor(channelConfigKey, recordKey); // != 1 if a unit translation is required

		// height calculation need special procedure
		if (recordKey.startsWith(measurements[1])) { // 1=Höhe
			PropertyType property = this.getMeasurement(channelConfigKey, recordKey).getProperty(Picolario.DO_SUBTRACT_FIRST);
			boolean subtractFirst = property != null ? new Boolean(property.getValue()).booleanValue() : false;
			property = this.getMeasurement(channelConfigKey, recordKey).getProperty(Picolario.DO_SUBTRACT_LAST);
			boolean subtractLast = property != null ? new Boolean(property.getValue()).booleanValue() : false;

			if (subtractFirst) {
				// get the record set to be used
				RecordSet recordSet = this.channels.getActiveChannel().getActiveRecordSet();
				if (recordKey.substring(recordKey.length() - 2).startsWith("_")) recordSet = this.application.getCompareSet();

				reduction = recordSet.getRecord(recordKey).getFirst().intValue() / 1000;
			}
			else if (subtractLast) {
				// get the record set to be used
				RecordSet recordSet = this.channels.getActiveChannel().getActiveRecordSet();
				if (recordKey.substring(recordKey.length() - 2).startsWith("_")) recordSet = this.application.getCompareSet();

				reduction = recordSet.getRecord(recordKey).getLast().intValue() / 1000;
			}
			else
				reduction = 0;
		}

		// slope calculation needs height factor
		else if (recordKey.startsWith(measurements[2])) { // 2=Steigung
			factor = this.getMeasurementFactor(channelConfigKey, measurements[1]);
		}

		double newValue = (value - offset) / factor + reduction;

		if (Picolario.log.isLoggable(Level.FINER)) Picolario.log.finer(String.format("new value calculated for %s - inValue %f - outValue %f", recordKey, value, newValue));
		return newValue;
	}

	/**
	 * function to calculate values for inactive and to be calculated records
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are measurement point every 10 seconds during capturing only and the calculation will take place directly switch all to displayable
		if (recordSet.isRaw()) {
			String[] measurements = this.getMeasurementNames(recordSet.getChannelName()); // 0=Spannung, 1=Höhe, 2=Steigrate
			if (!recordSet.get(measurements[2]).isDisplayable()) {
				// calculate the values required				
				PropertyType property = this.getMeasruementProperty(recordSet.getChannelName(), measurements[2], CalculationThread.REGRESSION_INTERVAL_SEC);
				int regressionInterval = property != null ? new Integer(property.getValue()) : 10;
				property = this.getMeasruementProperty(recordSet.getChannelName(), measurements[2], CalculationThread.REGRESSION_TYPE);
				if (property == null || property.getValue().equals(CalculationThread.REGRESSION_TYPE_CURVE))
					this.slopeCalculationThread = new QuasiLinearRegression(recordSet, measurements[1], measurements[2], regressionInterval);
				else
					this.slopeCalculationThread = new LinearRegression(recordSet, measurements[1], measurements[2], regressionInterval);

				this.slopeCalculationThread.start();
			}
		}
	}

	/**
	 * @return the dialog
	 */
	public PicolarioDialog getDialog() {
		return this.dialog;
	}

	/**
	 * @return the serialPort
	 */
	public PicolarioSerialPort getSerialPort() {
		return this.serialPort;
	}
}
