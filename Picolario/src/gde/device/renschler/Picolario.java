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
import osde.data.Record;
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
	public double translateValue(Record record, double value) {
		if (Picolario.log.isLoggable(Level.FINEST)) Picolario.log.finest(String.format("input value for %s - %f", record.getName(), value));

		String[] measurements = this.getMeasurementNames(record.getParent().getChannelConfigName()); // 0=Spannung, 1=Höhe, 2=Steigung
		String recordKey = record.getName();
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double reduction = record.getReduction();
		double factor = record.getFactor(); // != 1 if a unit translation is required

		// height calculation need special procedure
		if (recordKey.startsWith(measurements[1])) { // 1=Höhe
			PropertyType property = record.getProperty(Picolario.DO_SUBTRACT_FIRST);
			boolean subtractFirst = property != null ? new Boolean(property.getValue()).booleanValue() : false;
			property = record.getProperty(Picolario.DO_SUBTRACT_LAST);
			boolean subtractLast = property != null ? new Boolean(property.getValue()).booleanValue() : false;

			if (subtractFirst) {
				// get the record set to be used
				RecordSet recordSet = this.channels.getActiveChannel().getActiveRecordSet();
				if (recordKey.substring(recordKey.length() - 2).startsWith("_")) recordSet = this.application.getCompareSet();

				reduction = recordSet.getRecord(recordKey).getFirst().intValue() / 1000.0;
			}
			else if (subtractLast) {
				// get the record set to be used
				RecordSet recordSet = this.channels.getActiveChannel().getActiveRecordSet();
				if (recordKey.substring(recordKey.length() - 2).startsWith("_")) recordSet = this.application.getCompareSet();

				reduction = recordSet.getRecord(recordKey).getLast().intValue() / 1000.0;
			}
			else
				reduction = 0;
		}

		// slope calculation needs height factor for calculation
		else if (recordKey.startsWith(measurements[2])) { // 2=Steigung
			factor = this.getMeasurementFactor(record.getParent().getChannelConfigName(), measurements[1]);
		}

		double newValue = offset + (value - reduction) * factor;

		if (Picolario.log.isLoggable(Level.FINER)) Picolario.log.finer(String.format("value calculated for %s - inValue %f - outValue %f", recordKey, value, newValue));
		return newValue;
	}

	/**
	 * function to translate measured value from a device to values represented (((value - offset + firstLastAdaption)/factor) + reduction)
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(Record record, double value) {
		if (Picolario.log.isLoggable(Level.FINEST)) Picolario.log.finest(String.format("input value for %s - %f", record.getName(), value));

		String[] measurements = this.getMeasurementNames(record.getChannelConfigKey()); // 0=Spannung, 1=Höhe, 2=Steigung
		String recordKey = record.getName();
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double reduction = record.getReduction();
		double factor = record.getFactor(); // != 1 if a unit translation is required

		// height calculation need special procedure
		if (recordKey.startsWith(measurements[1])) { // 1=Höhe
			PropertyType property = record.getProperty(Picolario.DO_SUBTRACT_FIRST);
			boolean subtractFirst = property != null ? new Boolean(property.getValue()).booleanValue() : false;
			property = record.getProperty(Picolario.DO_SUBTRACT_LAST);
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

		// slope calculation needs height factor for calculation
		else if (recordKey.startsWith(measurements[2])) { // 2=Steigung
			factor = this.getMeasurementFactor(record.getParent().getChannelConfigName(), measurements[1]);
		}

		double newValue = (value - offset) / factor + reduction;

		if (Picolario.log.isLoggable(Level.FINER)) Picolario.log.finer(String.format("new value calculated for %s - inValue %f - outValue %f", recordKey, value, newValue));
		return newValue;
	}

	/**
	 * check and update visibility status of all records according the available device configuration
	 * this function must have only implementation code if the device implementation supports different configurations
	 * where some curves are hided for better overview 
	 * example: if device supports voltage, current and height and no sensors are connected to voltage and current
	 * it makes less sense to display voltage and current curves, if only height has measurement data
	 * at least an update of the graphics window should be included at the end of this method
	 */
	public void updateVisibilityStatus(RecordSet recordSet) {
		log.info("no update required for " + recordSet.getName());
	}

	/**
	 * function to calculate values for inactive and to be calculated records
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are measurement point every 10 seconds during capturing only and the calculation will take place directly switch all to displayable
		if (recordSet.isRaw() && recordSet.isRecalculation()) {
			String[] measurements = recordSet.getRecordNames(); // 0=Spannung, 1=Höhe, 2=Steigrate
			// calculate the values required		
			Record slopeRecord = recordSet.get(measurements[2]);
			slopeRecord.setDisplayable(false);
			PropertyType property = slopeRecord.getProperty(CalculationThread.REGRESSION_INTERVAL_SEC);
			int regressionInterval = property != null ? new Integer(property.getValue()) : 10;
			property = slopeRecord.getProperty(CalculationThread.REGRESSION_TYPE);
			if (property == null || property.getValue().equals(CalculationThread.REGRESSION_TYPE_CURVE))
				this.slopeCalculationThread = new QuasiLinearRegression(recordSet, measurements[1], measurements[2], regressionInterval);
			else
				this.slopeCalculationThread = new LinearRegression(recordSet, measurements[1], measurements[2], regressionInterval);

			this.slopeCalculationThread.start();
		}
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[] {	IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION, 
				DO_NO_ADAPTION, DO_OFFSET_HEIGHT, DO_SUBTRACT_FIRST, DO_SUBTRACT_LAST,
				CalculationThread.REGRESSION_INTERVAL_SEC, CalculationThread.REGRESSION_TYPE};
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
