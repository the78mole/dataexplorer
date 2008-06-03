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
package osde.device.wb;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import osde.data.Record;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.exception.DataInconsitsentException;
import osde.ui.OpenSerialDataExplorer;

/**
 * Sample and test device implementation, scale adjustment, ...
 * @author Winfried Brügmann
 */
public class Simulator extends DeviceConfiguration implements IDevice {
	final static Logger						log	= Logger.getLogger(Simulator.class.getName());

	final OpenSerialDataExplorer	application;
	final SimulatorSerialPort			serialPort;
	final SimulatorDialog					dialog;

	/**
	 * constructor using properties file
	 * @param deviceProperties
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public Simulator(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new SimulatorSerialPort(this, this.application);
		this.dialog = new SimulatorDialog(this.application.getShell(), this);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration 
	 */
	public Simulator(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new SimulatorSerialPort(this, this.application);
		this.dialog = new SimulatorDialog(this.application.getShell(), this);
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	public int getLovDataByteSize() {
		return 16;  // seams to be modulo 16 - 12 bytes for data, 4 bytes for telegram number
	}
	
	/**
	 * get LogView data bytes offset, in most cases the real data has an offset within the data bytes array
	 */
	public int getLovDataByteOffset() {
		return 4; // the offset where the real data begins 
	}

	/**
	 * add record data size points to each measurement, if measurement is calculation 0 will be added
	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
	 * this method is more usable for real logger, where data can be stored and converted in one block
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @throws DataInconsitsentException 
	 */
	public void addConvertedDataBufferAsDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize) throws DataInconsitsentException {
		int offset = this.getLovDataByteOffset();
		int size = this.getLovDataByteSize();
		byte[] readBuffer = new byte[size];
		int[] points = new int[this.getNumberOfMeasurements(recordSet.getChannelConfigName())];
		
		for (int i = 0; i < recordDataSize; i++) { 
			System.arraycopy(dataBuffer, i*size, readBuffer, 0, size);
			recordSet.addPoints(converDataBytes(points, readBuffer, offset, null), false);
		}
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param offset if there is any offset of the data within the data byte array
	 * @param dataBuffer byte arrax with the data to be converted
	 * @param calcValues factor, offset, reduction, ....
	 */
	@SuppressWarnings("unused")
	public int[] converDataBytes(int[] points, byte[] dataBuffer, int offset, HashMap<String, Double> calcValues) {		
		// add voltage U = 2.5 + (byte3 - 45) * 0.0532 - no calculation take place here
		points[0] = new Integer(dataBuffer[2 + offset]) * 1000;

		// calculate height values and add
		if (((dataBuffer[1 + offset] & 0x80) >> 7) == 0) // we have signed [feet]
			points[1] = ((dataBuffer[offset] & 0xFF) + ((dataBuffer[1 + offset] & 0x7F) << 8)) * 1000; // only positive part of height data
		else
			points[1] = (((dataBuffer[offset] & 0xFF) + ((dataBuffer[1 + offset] & 0x7F) << 8)) * -1) * 1000; // height is negative

		return points;
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double translateValue(Record record, double value) {
		double newValues = record.getOffset() * 1000.0 + record.getFactor() * value;
		Simulator.log.fine("newValue = " + newValues);
		// do some calculation
		return newValues;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double reverseTranslateValue(Record record, double value) {
		double newValues = value / record.getFactor() - record.getOffset() * 1000.0;
		// do some calculation
		return newValues;
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
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		//add implementation where data point are calculated
		//do not forget to make record displayable -> record.setDisplayable(true);
		log.fine("working with " + recordSet.getName());
	}

	/**
	 * @return the dialog
	 */
	public SimulatorDialog getDialog() {
		return this.dialog;
	}

	/**
	 * @return the serialPort
	 */
	public SimulatorSerialPort getSerialPort() {
		return this.serialPort;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[] {IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION};
	}
}
