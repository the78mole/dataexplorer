/**
 * 
 */
package osde.device.renschler;

import gnu.io.NoSuchPortException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.CalculationThread;
import osde.utils.QuasiLinearRegression;

/**
 * @author Winfried Bruegmann
 *
 */
public class Picolario extends DeviceConfiguration implements IDevice {
	private Logger									log										= Logger.getLogger(this.getClass().getName());

	private final OpenSerialDataExplorer application;
	private final PicolarioDialog	dialog;
	private final PicolarioSerialPort serialPort;
	private final Channels channels;
	private CalculationThread				calculationThread;

	/**
	 * @param iniFile
	 * @param isDetailed
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NoSuchPortException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public Picolario(String iniFile) throws FileNotFoundException, IOException, NoSuchPortException, ParserConfigurationException, SAXException {
		super(iniFile);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new PicolarioSerialPort(this, this.application.getStatusBar());
		this.dialog = new PicolarioDialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
	}

	/**
	 * constructor using existing device configuration
	 * @param device configuration
	 * @throws NoSuchPortException 
	 */
	public Picolario(DeviceConfiguration deviceConfig) throws NoSuchPortException {
		super(deviceConfig);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new PicolarioSerialPort(this, this.application.getStatusBar());
		this.dialog = new PicolarioDialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
	}

	/**
	 * function to translate measured values from a device to values represented
	 * @return double[] where value[0] is the min value and value[1] the max value
	 */
	public double[] translateValues(String recordKey, double minValue, double maxValue) {
		double[] newValues = new double[2];

		newValues[0] = translateValue(recordKey, minValue);
		newValues[1] = translateValue(recordKey, maxValue);

		return newValues;
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double translateValue(String recordKey, double value) {
		double newValue = 0.0;
		if(log.isLoggable(Level.FINEST)) log.finest(String.format("input value for %s - %f", recordKey, value));
		if (recordKey.startsWith(RecordSet.VOLTAGE)) {
			// calculate voltage U = 2.5 + (byte3 - 45) * 0.0532
			newValue = 2.5 + (value - 45.0) * 0.0532;
		}
		else if (recordKey.startsWith(RecordSet.HEIGHT)) {
			int firstValue = 0; // != 0 if first value must subtracted
			double offset = 0; // != 0 if curve has an defined offset
			double factor = 1.0; // != 1 if a unit translation is required
			// prepare the data for adding to record set
			switch (dialog.getHeightUnitSelection()) { // Feet 1, Meter 0
			case 0: // Meter , Feet is default
				factor = 1852.0 / 6076.0;
				break;
			case 1: // Feet /Fuß
				factor = 1.0;
				break;
			}
			if (dialog.isDoSubtractFirst()) {
				try { // use exception handling instead of transfer graphicsWindow type
					firstValue = channels.getActiveChannel().getActiveRecordSet().getRecord(recordKey).get(0).intValue() / 1000;
				}
				catch (NullPointerException e) {
					firstValue = application.getCompareSet().get(recordKey).get(0).intValue() / 1000;
				}
				catch (Exception e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			else if (dialog.isDoSubtractLast()) {
				try { // use exception handling instead of transfer graphicsWindow type
					Record record = channels.getActiveChannel().getActiveRecordSet().getRecord(recordKey);
					firstValue = record.get(record.size()-1).intValue() / 1000;
				}
				catch (NullPointerException e) {
					Record record = application.getCompareSet().get(recordKey);
					firstValue = record.get(record.size()-1).intValue() / 1000;
				}
				catch (Exception e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			else if (dialog.isDoReduceHeight()) {
				offset = dialog.getHeightOffsetValue();
				if (log.isLoggable(Level.FINER)) log.finer("dialog.isDoReduceHeight() == true -> value = " + offset);
			}
			if (log.isLoggable(Level.FINER)) log.finer(String.format("firstValue = %d, offset = %f, factor = %f", firstValue, offset, factor));
			if (log.isLoggable(Level.FINER)) log.finer(String.format("doSubtractFirst = %s, doReduceHeight = %s", dialog.isDoSubtractFirst(), dialog.isDoReduceHeight()));
			// ((height.get(i).intValue() - firstValue) * 1000 * multiplyValue / devideValue - subtractValue); // Höhe [m]
			newValue = (value - firstValue) * factor - offset;
		}
		else if (recordKey.startsWith(RecordSet.SLOPE)) {
			double factor = 1.0; // != 1 if a unit translation is required
			switch (dialog.getHeightUnitSelection()) { // Feet 1, Meter 0
			case 0: // Meter , Feet is default
				factor = 1852.0 / 6076.0;
				break;
			case 1: // Feet /Fuß
				factor = 1.0;
				break;
			}
			newValue = value * factor;
		}
		if(log.isLoggable(Level.FINEST)) log.finest(String.format("value calculated for %s - inValue %f - outValue %f", recordKey, value, newValue));
		return newValue;
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(String recordKey, double value) {
		if(log.isLoggable(Level.FINEST)) log.finest(String.format("input value for %s - %f", recordKey, value));
		double newValue = 0;
		if (recordKey.startsWith(RecordSet.VOLTAGE)) {
			// calculate voltage U = 2.5 + (value - 45) * 0.0532
			newValue = (value - 2.5) / 0.0532 + 45.0;
		}
		else if (recordKey.startsWith(RecordSet.HEIGHT)) {
			int firstValue = 0; // != 0 if first value must subtracted
			double offset = 0; // != 0 if curve has an defined offset
			double factor = 1.0; // != 1 if a unit translation is required
			// prepare the data for adding to record set
			switch (dialog.getHeightUnitSelection()) { // Feet 1, Meter 0
			case 0: // Meter , Feet is default
				if (log.isLoggable(Level.FINER)) log.finer("heightUnitSelection = " + dialog.getHeightUnitSelection());
				factor = 1852.0 / 6076.0;
				break;
			case 1: // Feet /Fuß
				if (log.isLoggable(Level.FINER)) log.finer("heightUnitSelection = " + dialog.getHeightUnitSelection());
				factor = 1.0;
				break;
			}
			if (dialog.isDoSubtractFirst()) {
				try { // use exception handling instead of transfer graphicsWindow type
					firstValue = channels.getActiveChannel().getActiveRecordSet().getRecord(recordKey).get(0).intValue() / 1000;
				}
				catch (NullPointerException e) {
					firstValue = application.getCompareSet().get(recordKey).get(0).intValue() / 1000;
				}
				catch (Exception e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			else if (dialog.isDoSubtractLast()) {
				try { // use exception handling instead of transfer graphicsWindow type
					Record record = channels.getActiveChannel().getActiveRecordSet().getRecord(recordKey);
					firstValue = record.get(record.size()-1).intValue() / 1000;
				}
				catch (NullPointerException e) {
					Record record = application.getCompareSet().get(recordKey);
					firstValue = record.get(record.size()-1).intValue() / 1000;
				}
				catch (Exception e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			else if (dialog.isDoReduceHeight()) {
				offset = dialog.getHeightOffsetValue();
			}
			// ((height.get(i).intValue() - firstValue) * 1000 * multiplyValue / devideValue - subtractValue); // Höhe [m]
			newValue = (value + offset) / factor + firstValue;
		}
		else if (recordKey.startsWith(RecordSet.SLOPE)) {
			double factor = 1.0; // != 1 if a unit translation is required
			switch (dialog.getHeightUnitSelection()) { // Feet 1, Meter 0
			case 0: // Meter , Feet is default
				factor = 1852.0 / 6076.0;
				break;
			case 1: // Feet /Fuß
				factor = 1.0;
				break;
			}
			newValue = value / factor;
		}
		if(log.isLoggable(Level.FINEST)) log.finest(String.format("new value calculated for %s - inValue %f - outValue %f", recordKey, value, newValue));
		return newValue;
	}

	/**
	 * @return the dataUnit
	 */
	public String getDataUnit(String recordKey) {
		String unit = "";
		recordKey = recordKey.split("_")[0];
		MeasurementType measurement = this.getMeasurementDefinition(recordKey);
		//channel.get("Messgröße1");
		if (recordKey.startsWith(RecordSet.VOLTAGE)) {
			unit = (String) measurement.getUnit();
		}
		else if (recordKey.startsWith(RecordSet.HEIGHT)) {
			unit = dialog.getHeightDataUnit();
		}
		else if (recordKey.startsWith(RecordSet.SLOPE)) {
			unit = dialog.getHeightDataUnit() + "/" +  measurement.getUnit().split("/")[1];
		}
		return unit;
	}

	/**
	 * function to calculate values for inactive records
	 * @return double with the adapted value
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are measurement point every 10 seconds during capturing only and the calculation will take place directly switch all to displayable
		if (recordSet.isFromFile() && recordSet.isRaw()) {
			for (String recordKey : recordSet.getRecordNames()) {
				if (!recordSet.get(recordKey).isDisplayable()) {
					// calculate the values required				
					calculationThread = new QuasiLinearRegression();
					//calculationThread = new LinearRegression(this);
					calculationThread.setRecordSet(recordSet);
					calculationThread.start();
				}
			}
		}
	}

	/**
	 * @return the dialog
	 */
	public PicolarioDialog getDialog() {
		return dialog;
	}

	/**
	 * @return the serialPort
	 */
	public PicolarioSerialPort getSerialPort() {
		return serialPort;
	}
}
