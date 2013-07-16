/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2012,2013 Winfried Bruegmann
****************************************************************************************/
package gde.device.wb;

import gde.GDE;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.io.CSVSerialDataReaderWriter;
import gde.log.Level;
import gde.messages.Messages;
import gde.utils.FileUtils;
import gde.utils.StringHelper;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.widgets.FileDialog;

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
						int grad = record.realGet(rowIndex) / 100000;
						double minuten = record.realGet(rowIndex) % 100000 / 10000.0;
						dataTableRow[j + 1] = String.format("%02d %07.4f", grad, minuten); //$NON-NLS-1$
					}
					else { // assume degree only
						dataTableRow[j + 1] = String.format("%02.7f", record.realGet(rowIndex) / 100000.0); //$NON-NLS-1$
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

	/**
	 * import a CSV file, also called "OpenFormat" file
	 */
	@Override
	public void importCsvFiles() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT1700));

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					AkkuMonitor.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.FILE_SEPARATOR_UNIX + tmpFileName;
						log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							try {
								String recordNameExtend;
								try {
									recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.STRING_DOT)-4, selectedImportFile.lastIndexOf(GDE.STRING_DOT));
									Integer.valueOf(recordNameExtend);
								}
								catch (Exception e) {
									try {
										recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.STRING_DOT)-3, selectedImportFile.lastIndexOf(GDE.STRING_DOT));
										Integer.valueOf(recordNameExtend);
									}
									catch (Exception e1) {
										recordNameExtend = GDE.STRING_EMPTY;
									}
								}
								Integer channelConfigNumber = dialog != null && !dialog.isDisposed() ? dialog.getTabFolderSelectionIndex() + 1 : null;
								CSVSerialDataReaderWriter.read(selectedImportFile, AkkuMonitor.this, recordNameExtend, channelConfigNumber, 
										new AkkuMonitorParser(AkkuMonitor.this.getDataBlockTimeUnitFactor(), 
												AkkuMonitor.this.getDataBlockLeader(), AkkuMonitor.this.getDataBlockSeparator().value(), 
												AkkuMonitor.this.getDataBlockCheckSumType(), AkkuMonitor.this.getDataBlockSize(InputTypes.FILE_IO)));
							}
							catch (Throwable e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					AkkuMonitor.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}
}
