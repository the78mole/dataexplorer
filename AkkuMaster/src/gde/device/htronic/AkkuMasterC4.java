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
package osde.device.htronic;

import gnu.io.NoSuchPortException;

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
import osde.device.MeasurementType;
import osde.exception.DataInconsitsentException;
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
		return 55; // 0x33 = 51 + 4 (counter)
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
		int lovDataSize = this.getLovDataByteSize();
		
		//byte[] configurationBuffer	= new byte[14];
		//byte[] measurementsBuffer		= new byte[16];
		byte[] convertDataBuffer = new byte[14 + 16];
		int[] points = new int[this.getNumberOfMeasurements(recordSet.getChannelConfigName())];
		
		for (int i = 0; i < recordDataSize; i++) { 
			
//		StringBuilder sb = new StringBuilder();
//		for (byte b : dataBuffer) {
//			sb.append(String.format("%02x", b)).append(" ");
//		}
//		log.info(sb.toString());
//discharge   			      Ni 12 capa  dis   charge                        disc  charge      hh:mm:ss hh:mm:ss  #                                           line
//	                      Nc    city  charge                              capac capac volt  charge   discharge                                                 counter
//33 00 00 00 51 82 00 03 01 0c 08 98 02 56 02 56 00 3c 61 05 09 02 56 52 03 ca 00 00 07 a3 01 25 19 00 00 00 01 00 3c 0d 0a 20 20 35 38 33 30 00 00 00 00 48 02 00 00             
//offset      51 82 00 03 00 0C 08 98 02 58 02 58                      52 00 02 00 00 0B C7 00 00 0D 00 00 00 01
//charge  				        Ni 12 capa  dis   charge                        disc  charge      hh:mm:ss hh:mm:ss  #                                           line
//		                    Nc    city  charge                              capac capac volt  charge   discharge                                                 counter
//33 00 00 00 51 81 00 03 01 0c 08 98 02 56 02 56 00 3c 61 05 09 02 56 52 03 cb 07 f0 0d 02 01 25 1f 03 17 38 01 00 3c 0d 0a 20 31 38 31 30 30 00 00 00 00 13 07 00 00 
//offset      51 81 00 03 00 0C 08 98 02 58 02 58                      52 02 85 0A 4A 0C DE 01 04 1F 04 17 1B 01

			System.arraycopy(dataBuffer,  4 + i*lovDataSize, convertDataBuffer,  0, 14); //configurationBuffer.length
			System.arraycopy(dataBuffer, 23 + i*lovDataSize, convertDataBuffer, 14, 16); //measurementsBuffer.length
			recordSet.addPoints(converDataBytes(points, convertDataBuffer), false);
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

		// build the point array according curves from record set
		//int[] points = new int[getRecordSet().size()];
		
		HashMap<String, Object> values = new HashMap<String, Object>(7);
		byte[] configurationBuffer	= new byte[14];
		System.arraycopy(dataBuffer, 0, configurationBuffer, 0, configurationBuffer.length);
		byte[] measurementsBuffer		= new byte[16];
		System.arraycopy(dataBuffer, 14, measurementsBuffer, 0, measurementsBuffer.length);
		values = AkkuMasterC4SerialPort.getConvertedValues(values, AkkuMasterC4SerialPort.convertConfigurationAnswer(configurationBuffer), AkkuMasterC4SerialPort.convertMeasurementValues(measurementsBuffer));


		points[0] = new Integer((Integer) values.get(AkkuMasterC4SerialPort.PROCESS_VOLTAGE)).intValue(); //Spannung 	[mV]
		points[1] = new Integer((Integer) values.get(AkkuMasterC4SerialPort.PROCESS_CURRENT)).intValue(); //Strom 			[mA]
		// display adaption * 1000  -  / 1000
		points[2] = new Integer((Integer) values.get(AkkuMasterC4SerialPort.PROCESS_CAPACITY)).intValue() * 1000; //Kapazität	[mAh] 
		points[3] = new Integer((Integer) values.get(AkkuMasterC4SerialPort.PROCESS_POWER)).intValue() / 1000; //Leistung		[mW]
		points[4] = new Integer((Integer) values.get(AkkuMasterC4SerialPort.PROCESS_ENERGIE)).intValue() / 1000; //Energie		[mWh]
		log.info(points[0] + " mV; " + points[1] + " mA; " + points[2] + " mAh; " + points[3] + " mW; " + points[4] + " mWh");

		return points;
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double translateValue(@SuppressWarnings("unused")Record record, double value) {
		double newValue = value;
		if (AkkuMasterC4.log.isLoggable(Level.FINEST)) AkkuMasterC4.log.finest(String.format("input value for %s - %f", record.getName(), value));
		if (AkkuMasterC4.log.isLoggable(Level.FINEST)) AkkuMasterC4.log.finest(String.format("value calculated for %s - %f", record.getName(), newValue));
		return newValue;
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(@SuppressWarnings("unused")Record record, double value) {
		double newValue = value;
		if (AkkuMasterC4.log.isLoggable(Level.FINEST)) AkkuMasterC4.log.finest(String.format("input value for %s - %f", record.getName(), value));
		if (AkkuMasterC4.log.isLoggable(Level.FINEST)) AkkuMasterC4.log.finest(String.format("value calculated for %s - %f", record.getName(), newValue));
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
		log.fine("no update required for " + recordSet.getName());
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
			// 0=Spannung, 1=Strom, 2=Ladung, 3=Leistung, 4=Energie
				String[] recordNames = recordSet.getRecordNames();
				String[] measurementNames = this.getMeasurementNames(recordSet.getChannelConfigName());
				
				String recordKey = recordNames[3]; //3=Leistung
				MeasurementType measurement = this.getMeasurement(recordSet.getChannelConfigName(), measurementNames[3]);
				if (measurement.isCalculation()) {
					AkkuMasterC4.log.fine(recordKey);
					this.calculationThreads.put(recordKey, new AkkuMasterCalculationThread(recordKey, this.channels.getActiveChannel().getActiveRecordSet()));
				}
				this.calculationThreads.get(recordKey).start();
				
				recordKey = recordNames[4]; //4=Energie
				measurement = this.getMeasurement(recordSet.getChannelConfigName(), measurementNames[4]);
				if (measurement.isCalculation()) {
					AkkuMasterC4.log.fine(recordKey);
					this.calculationThreads.put(recordKey, new AkkuMasterCalculationThread(recordKey, this.channels.getActiveChannel().getActiveRecordSet()));
				}
				this.calculationThreads.get(recordKey).start();
				
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

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[0];
	}
}
