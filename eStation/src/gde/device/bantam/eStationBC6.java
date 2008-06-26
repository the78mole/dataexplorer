/**
 * 
 */
package osde.device.bantam;

import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import osde.device.DeviceConfiguration;
import osde.device.IDevice;

/**
 * Sample device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class eStationBC6 extends eStation implements IDevice {
	final static Logger						log	= Logger.getLogger(eStationBC6.class.getName());

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public eStationBC6(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		this.dialog = new EStationDialog(this.application.getShell(), this);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public eStationBC6(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		this.dialog = new EStationDialog(this.application.getShell(), this);
	}

//	/**
//	 * load the mapping exist between lov file configuration keys and OSDE keys
//	 * @param lov2osdMap reference to the map where the key mapping has to be put
//	 * @return lov2osdMap same reference as input parameter
//	 */
//	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
//		// ...
//		return lov2osdMap;
//	}
//
//	/**
//	 * convert record logview config data to OSDE config keys into records section
//	 * @param header reference to header data, contain all key value pairs
//	 * @param lov2osdMap reference to the map where the key mapping
//	 * @param channelNumber 
//	 * @return
//	 */
//	@SuppressWarnings("unused")
//	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
//		// ...
//		return "";
//	}
//
//	/**
//	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
//	 */
//	public int getLovDataByteSize() {
//		return 16;  // sometimes first 4 bytes give the length of data + 4 bytes for number
//	}
//	/**
//	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
//	 * adaption from LogView stream data format into the device data buffer format is required
//	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
//	 * this method is more usable for real logger, where data can be stored and converted in one block
//	 * @param recordSet
//	 * @param dataBuffer
//	 * @param recordDataSize
//	 * @throws DataInconsitsentException 
//	 */
//	public void addAdaptedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize) throws DataInconsitsentException {
//		// prepare the hash map containing the calculation values like factor offset, reduction, ...
//		String[] measurements = this.getMeasurementNames(recordSet.getChannelConfigName()); // 0=Spannung, 1=Höhe, 2=Steigrate, ....
//		HashMap<String, Double> calcValues = new HashMap<String, Double>();
//		calcValues.put(X_FACTOR, recordSet.get(measurements[11]).getFactor());
//		calcValues.put(X_OFFSET, recordSet.get(measurements[11]).getOffset());
//		
//		int offset = 4;
//		int lovDataSize = this.getLovDataByteSize();
//		int deviceDataBufferSize = 8;
//		byte[] convertBuffer = new byte[deviceDataBufferSize];
//		int[] points = new int[this.getNumberOfMeasurements(recordSet.getChannelConfigName())];
//		
//		for (int i = 0; i < recordDataSize; i++) { 
//			System.arraycopy(dataBuffer, offset + i*lovDataSize, convertBuffer, 0, deviceDataBufferSize);
//			recordSet.addPoints(converDataBytes(points, convertBuffer), false);
//		}
//	}
//
//	/**
//	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
//	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
//	 * @param points pointer to integer array to be filled with converted data
//	 * @param dataBuffer byte arrax with the data to be converted
//	 */
//	@SuppressWarnings("unused")
//	public int[] converDataBytes(int[] points, byte[] dataBuffer) {		
//		
//		StringBuilder sb = new StringBuilder();
//		for (byte b : dataBuffer) {
//			sb.append(String.format("%02x", b)).append(" ");
//		}
//		log.info(sb.toString());
//		
//// LogView data section analyse 		
////discharge   			      Ni 12 capa  dis   charge                        disc  charge      hh:mm:ss hh:mm:ss  #                                           line
////                        Nc    city  charge                              capac capac volt  charge   discharge                                                 counter
////33 00 00 00 51 82 00 03 01 0c 08 98 02 56 02 56 00 3c 61 05 09 02 56 52 03 ca 00 00 07 a3 01 25 19 00 00 00 01 00 3c 0d 0a 20 20 35 38 33 30 00 00 00 00 48 02 00 00             
////offset      51 82 00 03 00 0C 08 98 02 58 02 58                      52 00 02 00 00 0B C7 00 00 0D 00 00 00 01
////charge  				        Ni 12 capa  dis   charge                        disc  charge      hh:mm:ss hh:mm:ss  #                                           line
////	                      Nc    city  charge                              capac capac volt  charge   discharge                                                 counter
////33 00 00 00 51 81 00 03 01 0c 08 98 02 56 02 56 00 3c 61 05 09 02 56 52 03 cb 07 f0 0d 02 01 25 1f 03 17 38 01 00 3c 0d 0a 20 31 38 31 30 30 00 00 00 00 13 07 00 00 
////offset      51 81 00 03 00 0C 08 98 02 58 02 58                      52 02 85 0A 4A 0C DE 01 04 1F 04 17 1B 01
//
//		return points;
//	}
//
//	/**
//	 * function to translate measured values from a device to values represented
//	 * this function should be over written by device and measurement specific algorithm
//	 * @return double of device dependent value
//	 */
//	public double translateValue(Record record, double value) {
//		// 0=Spannung, 1=Höhe, 2=Steigung
//		String[] recordNames = record.getRecordSetNames(); 
//		
//		String recordKey = record.getName();
//		double offset = record.getOffset(); // != 0 if curve has an defined offset
//		double factor = record.getFactor(); // != 1 if a unit translation is required
//
//		// example height calculation need special procedure
//		if (recordKey.startsWith(recordNames[1])) { // 1=Höhe
//			// do some calculation
//		}
//		
//		double newValue = value * factor + offset;
//		log.fine("for " + record.getName() + " in value = " + value + " out value = " + newValue);
//		return newValue;
//	}
//
//	/**
//	 * function to reverse translate measured values from a device to values represented
//	 * this function should be over written by device and measurement specific algorithm
//	 * @return double of device dependent value
//	 */
//	public double reverseTranslateValue(Record record, double value) {
//		// 0=Spannung, 1=Höhe, 2=Steigung
//		String[] recordNames = record.getRecordSetNames(); 
//		
//		String recordKey = record.getName();
//		double offset = record.getOffset(); // != 0 if curve has an defined offset
//		double factor = record.getFactor(); // != 1 if a unit translation is required
//
//		// example height calculation need special procedure
//		if (recordKey.startsWith(recordNames[1])) { // 1=Höhe
//			// do some calculation
//		}
//		
//		double newValue = value / factor - offset;
//		log.fine("for " + record.getName() + " in value = " + value + " out value = " + newValue);
//		return newValue;
//	}
//
//	/**
//	 * check and update visibility status of all records according the available device configuration
//	 * this function must have only implementation code if the device implementation supports different configurations
//	 * where some curves are hided for better overview 
//	 * example: if device supports voltage, current and height and no sensors are connected to voltage and current
//	 * it makes less sense to display voltage and current curves, if only height has measurement data
//	 * at least an update of the graphics window should be included at the end of this method
//	 */
//	public void updateVisibilityStatus(RecordSet recordSet) {
//		log.info("no update required for " + recordSet.getName());
//	}
//
//	/**
//	 * function to calculate values for inactive records, data not readable from device
//	 * if calculation is done during data gathering this can be a loop switching all records to displayable
//	 * for calculation which requires more effort or is time consuming it can call a background thread, 
//	 * target is to make sure all data point not coming from device directly are available and can be displayed 
//	 */
//	public void makeInActiveDisplayable(RecordSet recordSet) {
//		//add implementation where data point are calculated
//		//do not forget to make record displayable -> record.setDisplayable(true);
//		String[] recordNames = this.getMeasurementNames(recordSet.getChannelConfigName());
//		for (String recordKey : recordNames) {
//			MeasurementType measurement = this.getMeasurement(recordSet.getChannelConfigName(), recordKey);
//			if (measurement.isCalculation()) {
//				EStationBC6.log.fine("do calculation for " + recordKey);
//			}
//		}
//	}

	/**
	 * @return the dialog
	 */
	public EStationDialog getDialog() {
		return this.dialog;
	}

//	/**
//	 * @return the serialPort
//	 */
//	public EStationSerialPort getSerialPort() {
//		return this.serialPort;
//	}
//
//	/**
//	 * query for all the property keys this device has in use
//	 * - the property keys are used to filter serialized properties form OSD data file
//	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
//	 */
//	public String[] getUsedPropertyKeys() {
//		return new String[] {IDevice.OFFSET, IDevice.FACTOR};
//	}
}
