/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.device.renschler;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.device.PropertyType;
import osde.exception.DataInconsitsentException;
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
	 * load the mapping exist between lov file configuration keys and OSDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		//nothing to do here
		return lov2osdMap;
	}

	/**
	 * convert record logview config data to OSDE config keys into records section
	 * @param header reference to header data, contain all key value pairs
	 * @param lov2osdMap reference to the map where the key mapping
	 * @param channelNumber 
	 * @return
	 */
	@SuppressWarnings("unused")
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		return "";
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	public int getLovDataByteSize() {
		return 16;  // 0x0C = 12 + 4 (counter)
	}

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addAdaptedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize) throws DataInconsitsentException {
		int offset = 4;
		int size = this.getLovDataByteSize();
		int devicedataBufferSize = 3;
		byte[] convertBuffer = new byte[devicedataBufferSize];
		int[] points = new int[this.getNumberOfMeasurements(recordSet.getChannelConfigName())];
		
		for (int i = 0; i < recordDataSize; i++) { 
			System.arraycopy(dataBuffer, offset + i*size, convertBuffer, 0, devicedataBufferSize);
			recordSet.addPoints(converDataBytes(points, convertBuffer), false);
		}
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	@SuppressWarnings("unused")
	public int[] converDataBytes(int[] points, byte[] dataBuffer) {		
		// add voltage U = 2.5 + (byte3 - 45) * 0.0532 - no calculation take place here - refer to translateValue/reverseTranslateValue
		points[0] = new Integer(dataBuffer[2]) * 1000;

		// calculate height values and add
		if (((dataBuffer[1] & 0x80) >> 7) == 0) // we have signed [feet]
			points[1] = ((dataBuffer[0] & 0xFF) + ((dataBuffer[1] & 0x7F) << 8)) * 1000; // only positive part of height data
		else
			points[1] = (((dataBuffer[0] & 0xFF) + ((dataBuffer[1] & 0x7F) << 8)) * -1) * 1000; // height is negative

		return points;
	}

	/**
	 * function to translate measured value from a device to values represented (((value - reduction) * factor) + offset - firstLastAdaption)
	 * @return double with the adapted value
	 */
	public double translateValue(Record record, double value) {
		if (Picolario.log.isLoggable(Level.FINEST)) Picolario.log.finest(String.format("input value for %s - %f", record.getName(), value));

		// 0=Spannung, 1=Höhe, 2=Steigung
		String[] recordNames = record.getParent().isCompareSet() ? record.getSourceRecordSetNames() : record.getParent().getRecordNames(); 
		
		String recordKey = record.getName();
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double reduction = record.getReduction();
		double factor = record.getFactor(); // != 1 if a unit translation is required

		// height calculation need special procedure
		if (recordKey.startsWith(recordNames[1])) { // 1=Höhe
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
		else if (recordKey.startsWith(recordNames[2])) { // 2=Steigung
			factor = this.getMeasurementFactor(record.getParent().getChannelConfigName(), recordNames[1]);
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

		// 0=Spannung, 1=Höhe, 2=Steigung
		String[] recordNames = record.getParent().isCompareSet() ? record.getSourceRecordSetNames() : record.getParent().getRecordNames(); 

		String recordKey = record.getName();
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double reduction = record.getReduction();
		double factor = record.getFactor(); // != 1 if a unit translation is required

		// height calculation need special procedure
		if (recordKey.startsWith(recordNames[1])) { // 1=Höhe
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
		else if (recordKey.startsWith(recordNames[2])) { // 2=Steigung
			factor = this.getMeasurementFactor(record.getParent().getChannelConfigName(), recordNames[1]);
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
