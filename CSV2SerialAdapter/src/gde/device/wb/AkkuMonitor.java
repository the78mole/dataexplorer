package gde.device.wb;

import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.log.Level;
import gde.utils.StringHelper;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

public class AkkuMonitor extends CSV2SerialAdapter {

	public AkkuMonitor(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	public AkkuMonitor(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}


	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			for (int j = 0; j < recordSet.size(); j++) {
				Record record = recordSet.get(j);
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				
				switch (record.getDataType()) {
				case GPS_LATITUDE:
				case GPS_LONGITUDE:
					if (record.getUnit().contains("°") && record.getUnit().contains("'")) {
						int grad = record.realGet(rowIndex) / 1000000;
						double minuten = record.realGet(rowIndex) % 1000000 / 10000.0;
						dataTableRow[j + 1] = String.format("%02d %07.4f", grad, minuten); //$NON-NLS-1$
					}
					else { // assume degree only
						dataTableRow[j + 1] = String.format("%02.7f", record.realGet(rowIndex) / 1000000.0); //$NON-NLS-1$
					}
					break;
					
				case DATE_TIME:
					dataTableRow[j + 1] = StringHelper.getFormatedTime(record.getUnit(), record.realGet(rowIndex));
					break;

				default:
					dataTableRow[j + 1] = record.getDecimalFormat().format((offset + ((record.realGet(rowIndex) / 1000.0) - reduction) * factor));
					break;
				}
				
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;		
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double translateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
		
		PropertyType property = record.getProperty(MeasurementPropertyTypes.DO_SUBTRACT_FIRST.value());
		boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
		property = record.getProperty(MeasurementPropertyTypes.DO_SUBTRACT_LAST.value());
		boolean subtractLast = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;

		try {
			if (subtractFirst) {
				reduction = record.getFirst() / 1000.0;
			}
			else if (subtractLast) {
				reduction = record.getLast() / 1000.0;
			}
		}
		catch (Throwable e) {
			reduction = 0;
		}
		
		double newValue = 0;
		switch (record.getDataType()) {
		case GPS_LATITUDE:
		case GPS_LONGITUDE:
			if (record.getUnit().contains("°") && record.getUnit().contains("'")) {
				int grad = ((int)(value / 100));
				double minuten = (value - (grad*100.0))/10.0;
				newValue = grad + minuten/60.0;
			}
			else { // assume degree only
				newValue = value / 10000.0;
			}
			break;
			
		case DATE_TIME:
			newValue = 0;
			break;
			
		default:
			newValue = (value - reduction) * factor + offset;
			break;
		}

		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required

		PropertyType property = record.getProperty(MeasurementPropertyTypes.DO_SUBTRACT_FIRST.value());
		boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
		property = record.getProperty(MeasurementPropertyTypes.DO_SUBTRACT_LAST.value());
		boolean subtractLast = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;

		try {
			if (subtractFirst) {
				reduction = record.getFirst() / 1000.0;
			}
			else if (subtractLast) {
				reduction = record.getLast() / 1000.0;
			}
		}
		catch (Throwable e) {
			reduction = 0;
		}

		double newValue = 0;
		switch (record.getDataType()) {
		case GPS_LATITUDE:
		case GPS_LONGITUDE:
			if (record.getUnit().contains("°") && record.getUnit().contains("'")) {
				int grad = (int)value;
				double minuten =  (value - grad*1.0) * 60.0;
				newValue = (grad + minuten/100.0)*10000.0;
			}
			else { // assume degree only
				newValue = value * 10000.0;
			}
			break;
			
		case DATE_TIME:
			newValue = 0;
			break;

		default:
			newValue = (value - offset) / factor + reduction;
			break;
		}

		log.log(Level.FINER, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}
}
