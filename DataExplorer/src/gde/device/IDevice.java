/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011 Winfried Bruegmann
****************************************************************************************/
package gde.device;

import gde.comm.IDeviceCommPort;
import gde.data.Record;
import gde.data.RecordSet;
import gde.exception.DataInconsitsentException;
import gde.utils.CalculationThread;

import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.custom.CTabItem;

/**
 * Defines the interface for all device implementations, it also covers some interface methods from 
 * DeviceDialog as well as DeviceSerialPort
 * @author Winfried Brügmann
 */
public interface IDevice {
	// define some global constants for data calculation 
	public static final String 	OFFSET 		= MeasurementPropertyTypes.OFFSET.value();
	public static final String	FACTOR 		= MeasurementPropertyTypes.FACTOR.value();
	public static final String	REDUCTION = MeasurementPropertyTypes.REDUCTION.value();
	
	/**
	 * get the active device configuration for manipulation purpose
	 */
	public DeviceConfiguration getDeviceConfiguration();
	
	/**
	 * get the active device configuration file name
	 */
	public String getPropertiesFileName();
	
	/**
	 * @return true if a device property was changed
	 */
	public boolean isChangePropery();

	/**
	 * @return the device dialog
	 */
	public DeviceDialog getDialog();
	
	/**
	 * @return the device communication port
	 */
	public IDeviceCommPort getCommunicationPort();
		
	/**
	 * @return the device name
	 */
	public String	getName();
	
	/**
	 * @param newName set a new device name
	 */
	public void setName(String newName);
	
	/**
	 * @return usage device state
	 */
	public boolean isUsed();
	
	/**
	 * @return device manufacturer
	 */
	public String getManufacturer();
	
	/**
	 * @param name set a new manufacture name
	 */
	public void setManufacturer(String name);

	/**
	 * @return device group
	 */
	public DeviceTypes getDeviceGroup();
	
	/**
	 * @param name set a new manufacture name
	 */
	public void setDeviceGroup(DeviceTypes name);
	
	/**
	 * @return link to manufacturer
	 */
	public String getManufacturerURL();

	/**
	 * @param name set a new manufacture name
	 */
	public void setManufacturerURL(String name);

	/**
	 * @return the device name
	 */
	public String getImageFileName();

	/**
	 * @param newImageFileName set a new image filename(.jpg|.gif|.png)
	 */
	public void setImageFileName(String newImageFileName);

	/**
	 * query if the table tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isTableTabRequested();
	
	/**
	 * set the DesktopType.TYPE_TABLE_TAB property to the given value
	 * @param enable
	 */
	public void setTableTabRequested(boolean enable);
	
	/**
	 * query if the digital tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isDigitalTabRequested();
	
	/**
	 * set the DesktopType.TYPE_DIGITAL_TAB property to the given value
	 * @param enable
	 */
	public void setDigitalTabRequested(boolean enable);
	
	/**
	 * query if the analog tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isAnalogTabRequested();
	
	/**
	 * set the DesktopType.TYPE_ANALOG_TAB property to the given value
	 * @param enable
	 */
	public void setAnalogTabRequested(boolean enable);
	
	/**
	 * query if the voltage per cell tab should be updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isVoltagePerCellTabRequested();
	
	/**
	 * set the DesktopType.TYPE_VOLTAGE_PER_CELL_TAB property to the given value
	 * @param enable
	 */
	public void setVoltagePerCellTabRequested(boolean enable);

	/**
	 * query if the target measurement reference ordinal used by the given desktop type
	 * @return the target measurement reference ordinal, -1 if reference ordinal not set
	 */
	public int getDesktopTargetReferenceOrdinal(DesktopPropertyTypes desktopPropertyType);
	
	/**
	 * query if the utility graphics tabulator should be displayed and updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isUtilityGraphicsTabRequested(); 
	
	/**
	 * query if the utility device tabulator should be displayed and updated
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isUtilityDeviceTabRequested();
//	
//	/**
//	 * This function allows to register a device specific CTabItem to the main application tab folder to display device 
//	 * specific curve calculated from point combinations or other specific dialog
//	 * As default the function should return null which stands for no device custom tab item.  
//	 */
//	public CTabItem getUtilityGraphicsTabItem();
	
	/**
	 * This function allows to register a device specific CTabItem to the main application tab folder to display device 
	 * specific specific dialog
	 * As default the function should return null which stands for no device custom tab item.  
	 */
	public CTabItem getUtilityDeviceTabItem();

	/**
	 * @return time step in ms
	 */
	public double getTimeStep_ms();
	
	/**
	 * set new average time step in ms
	 */
	public void setTimeStep_ms(double newTimeStep_ms);

	/**
	 * @return average time step in ms (this is an optional element, keep this in mind to have a workaround if it does not exist)
	 */
	public Double getAverageTimeStep_ms();
	
	/**
	 * set new time step in ms
	 */
	public void setAverageTimeStep_ms(double newTimeStep_ms);

	/**
	 * @return UTC delta time in hours
	 */
	public short getUTCdelta();

	/**
	 * set new UTC delta time in hours
	 */
	public void setUTCdelta(int newUTCdelta);
	
	/**
	 * @return the serial port type, optional configure for the device
	 */
	//public SerialPortType getSerialPortType();

	/**
	 * @return the port configured for the device, if SerialPortType is not defined in device specific XML a empty string will returned
	 */
	public String getPort();
	
	/**
	 * @param newPort - set a new port string for the device
	 */
	public void setPort(String newPort);
	
	/**
	 * @return the baude rate of the device
	 */
	public Integer getBaudeRate();
	
	/**
	 * @return the data bit configuration of the device
	 */
	public DataBitsTypes getDataBits();
	
	/**
	 * @return the stop bit configuration of the device
	 */
	public StopBitsTypes getStopBits();
	
	/**
	 * @return the flow control configuration of the device
	 */
	public int getFlowCtrlMode();
	
	/**
	 * @return the parity bit configuration of the device
	 */
	public ParityTypes getParity();
	
	/**
	 * @return  the DTR configuration of the device
	 */
	public boolean isDTR();
	
	/**
	 * @return  the RTS configuration of the device
	 */
	public boolean isRTS();
	
	/**
	 * @return the current data block size
	 */
	public int getDataBlockSize();

	/**
	 * @param newSize set a new date block size/length
	 */
	public void setDataBlockSize(Integer newSize);
	
	/**
	 * @return the format type of the data block ASCII(text) or BINARY(hex)
	 */
	public FormatTypes getDataBlockFormat();
	
	/**
	 * @param value set a new format type of the data block ASCII(text) or BINARY(hex)
	 */
	public void setDataBlockFormat(FormatTypes value);
	
	/**
	 * @return the checksum type of the data block XOR, ADD, ..
	 */
	public CheckSumTypes getDataBlockCheckSumType();

	/**
	 * @param value set a new date block size/length
	 */
	public void setDataBlockCheckSumType(CheckSumTypes value);
	
	/**
	 * @return the format type of the data block checksum ASCII(text) or BINARY(hex)
	 */
	public FormatTypes getDataBlockCheckSumFormat();
	
	/**
	 * @param value set a new date block checksum format type ASCII(text) or BINARY(hex)
	 */
	public void setDataBlockCheckSumFormat(FormatTypes value);
	
	/**
	 * @param value set the time unit as defined in TimeUnitTypes, msec --> timeUnitFactor = 1; sec --> timeUnitFactor = 1000
	 */
	public void setDataBlockTimeUnit(TimeUnitTypes value);
	
	/**
	 * @return query the time factor, needed for CVS 2 serial data parser, time steps are internal used in msec. 
	 */
	public int getDataBlockTimeUnitFactor();
	
	/**
	 * @return the data block leader character
	 */
	public String getDataBlockLeader();

	/**
	 * @param value set a new character to be used as data line leading edge character
	 */
	public void setDataBlockLeader(String value);	
	
	/**
	 * @return the data block value separator as CommaSeparatorTypes
	 */
	public CommaSeparatorTypes getDataBlockSeparator();

	/**
	 * set the data block value separator as CommaSeparatorTypes
	 * @param value
	 */
	public void setDataBlockSeparator(CommaSeparatorTypes value);

	/**
	 * @return the format type of the data block ASCII(text) or BINARY(hex)
	 */
	public byte[] getDataBlockEnding();

	/**
	 * @param value set a new date block ending as LineEndingTypes.XY.value
	 */
	public void setDataBlockEnding(String value);

	/**
	 * @return the preferred specified data location as full qualified path
	 */
	public String getDataBlockPreferredDataLocation();

	/**
	 * @param value set a new full qualified data path location
	 */
	public void setDataBlockPreferredDataLocation(String value);
	
	/**
	 * @return the preferred file extension used in file selection dialog
	 */
	public String getDataBlockPreferredFileExtention();

	/**
	 * @param value set a new file extension if other than *.csv should be used
	 */
	public void setDataBlockPreferredFileExtention(String value);

	/**
	 * @return actual StateType
	 */
	public StateType getStateType();
	
	/**
	 * @return the channel count
	 */
	public int getChannelCount();

	/**
	 * @param channelNumber
	 * @return the channel name
	 */
	public String getChannelName(int channelNumber);
	
	/**
	 * @param channelName - size should not exceed 15 char length - this is the key to get access
	 * @param channelNumber
	 */
	public void setChannelName(String channelName, int channelNumber);

	/**
	 * @param channelNumber (starts at 1)
	 * @return the channel type
	 */
	public ChannelTypes getChannelTypes(int channelNumber);
	
	/**
	 * @return the channel measurements by given channel configuration number
	 */
	public List<MeasurementType> getChannelMeasuremts(int channelConfigNumber);
	
	/**
	 * @return the channel measurements by given channel configuration key (name)
	 */
	@Deprecated
	public List<MeasurementType> getChannelMeasuremts(String channelConfigKey);

	/**
	 * get the properties from a channel/configuration and record key name 
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return list of properties according measurement
	 */
	public List<PropertyType> getProperties(int channelConfigNumber, int measurementOrdinal);

	/**
	 * get the properties from a channel/configuration and record key name 
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return list of properties according measurement
	 */
	@Deprecated
	public List<PropertyType> getProperties(String channelConfigKey, int measurementOrdinal);

	/**
	 * @return the number (size) of measurements of a channel, assume existing channels have different number of measurements
	 */
	public int getNumberOfMeasurements(int channelConfigNumber);

	/**
	 * @return the number of measurements of a channel, assume channels have different number of measurements
	 */
	@Deprecated
	public int getNumberOfMeasurements(String channelConfigKey);
	
	/**
	 * get the measurement to get/set measurement specific parameter/properties (voltage, current, height, slope, ..)
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return PropertyType object
	 */
	public MeasurementType getMeasurement(int channelConfigNumber, int measurementOrdinal);
	
	/**
	 * get the measurement to get/set measurement specific parameter/properties (voltage, current, height, slope, ..)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return PropertyType object
	 */
	@Deprecated
	public MeasurementType getMeasurement(String channelConfigKey, int measurementOrdinal);

	/**
	 * set active status of an measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param isActive
	 */
	public void setMeasurementActive(int channelConfigNumber, int measurementOrdinal, boolean isActive);
	
	/**
	 * @return the sorted measurement names
	 */
	public String[] getMeasurementNames(int channelConfigNumber);
	
	/**
	 * @return the sorted measurement names
	 */
	@Deprecated
	public String[] getMeasurementNames(String channelConfigKey);

	/**
	 * set new name of specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param name
	 */
	public void setMeasurementName(int channelConfigNumber, int measurementOrdinal, String name);

	/**
	 * set new name of specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param name
	 */
	@Deprecated
	public void setMeasurementName(String channelConfigKey, int measurementOrdinal, String name);

	/**
	 * method to query the unit of measurement data unit by a given record key
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return dataUnit as string
	 */
	public String getMeasurementUnit(int channelConfigNumber, int measurementOrdinal);

	/**
	 * method to query the unit of measurement data unit by a given record key
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return dataUnit as string
	 */
	@Deprecated
	public String getMeasurementUnit(String channelConfigKey, int measurementOrdinal);

	/**
	 * method to set the unit of measurement by a given measurement key
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param unit
	 */
	public void setMeasurementUnit(int channelConfigNumber, int measurementOrdinal, String unit);

	/**
	 * method to set the unit of measurement by a given measurement key
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param unit
	 */
	@Deprecated
	public void setMeasurementUnit(String channelConfigKey, int measurementOrdinal, String unit);

	/**
	 * get the symbol of specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 */
	public String getMeasurementSymbol(int channelConfigNumber, int measurementOrdinal);

	/**
	 * get the symbol of specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 */
	@Deprecated
	public String getMeasurementSymbol(String channelConfigKey, int measurementOrdinal);

	/**
	 * set new symbol of specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param symbol
	 */
	public void setMeasurementSymbol(int channelConfigNumber, int measurementOrdinal, String symbol);

	/**
	 * set new symbol of specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param symbol
	 */
	@Deprecated
	public void setMeasurementSymbol(String channelConfigKey, int measurementOrdinal, String symbol);
	
	/**
	 * get the statistics type of the specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return statistics, if statistics does not exist return null
	 */
	public StatisticsType getMeasurementStatistic(int channelConfigNumber, int measurementOrdinal);
	
	/**
	 * get the statistics type of the specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return statistics, if statistics does not exist return null
	 */
	@Deprecated
	public StatisticsType getMeasurementStatistic(String channelConfigKey, int measurementOrdinal);

	/**
	 * get property with given channel configuration key, measurement key and property type key (IDevice.OFFSET, ...)
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @return PropertyType object
	 */
	public PropertyType getMeasruementProperty(int channelConfigNumber, int measurementOrdinal, String propertyKey);

	/**
	 * get property with given channel configuration key, measurement key and property type key (IDevice.OFFSET, ...)
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @return PropertyType object
	 */
	@Deprecated
	public PropertyType getMeasruementProperty(String channelConfigKey, int measurementOrdinal, String propertyKey);

	/**
	 * get the offset value of the specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return the offset, if property does not exist return 0.0 as default value
	 */
	public double getMeasurementOffset(int channelConfigNumber, int measurementOrdinal);

	/**
	 * get the offset value of the specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return the offset, if property does not exist return 0.0 as default value
	 */
	@Deprecated
	public double getMeasurementOffset(String channelConfigKey, int measurementOrdinal);

	/**
	 * set new value for offset at the specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param offset the offset to set
	 */
	public void setMeasurementOffset(int channelConfigNumber, int measurementOrdinal, double offset);

	/**
	 * set new value for offset at the specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param offset the offset to set
	 */
	@Deprecated
	public void setMeasurementOffset(String channelConfigKey, int measurementOrdinal, double offset);

	/**
	 * get the factor value of the specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @return the factor, if property does not exist return 1.0 as default value
	 */
	public double getMeasurementFactor(int channelConfigNumber, int measurementOrdinal);

	/**
	 * get the factor value of the specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @return the factor, if property does not exist return 1.0 as default value
	 */
	@Deprecated
	public double getMeasurementFactor(String channelConfigKey, int measurementOrdinal);

	/**
	 * set new value for factor at the specified measurement
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param factor the offset to set
	 */
	public void setMeasurementFactor(int channelConfigNumber, int measurementOrdinal, double factor);

	/**
	 * set new value for factor at the specified measurement
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param factor the offset to set
	 */
	@Deprecated
	public void setMeasurementFactor(String channelConfigKey, int measurementOrdinal, double factor);

	/**
	 * get a property of specified measurement, the data type must be known - data conversion is up to implementation
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @return the property from measurement defined by key, if property does not exist return 1 as default value
	 */
	public Object getMeasurementPropertyValue(int channelConfigNumber, int measurementOrdinal, String propertyKey);

	/**
	 * get a property of specified measurement, the data type must be known - data conversion is up to implementation
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @return the property from measurement defined by key, if property does not exist return 1 as default value
	 */
	@Deprecated
	public Object getMeasurementPropertyValue(String channelConfigKey, int measurementOrdinal, String propertyKey);

	/**
	 * set new property value of specified measurement, if the property does not exist it will be created
	 * @param channelConfigNumber
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @param type of DataTypes
	 * @param value
	 */
	public void setMeasurementPropertyValue(int channelConfigNumber, int measurementOrdinal, String propertyKey, DataTypes type, Object value);

	/**
	 * set new property value of specified measurement, if the property does not exist it will be created
	 * @param channelConfigKey
	 * @param measurementOrdinal
	 * @param propertyKey
	 * @param type of DataTypes
	 * @param value
	 */
	@Deprecated
	public void setMeasurementPropertyValue(String channelConfigKey, int measurementOrdinal, String propertyKey, DataTypes type, Object value);

	/**
	 * get the last used channel number (ordinal + 1 = channel number)
	 * @return the last used channel number
	 */
	public int getLastChannelNumber();

	/**
	 * set the last used channel number (ordinal + 1 = channel number)
	 * @return the last used channel number
	 */
	public void setLastChannelNumber(int channelNumber);
	
	/**
	 * query the default stem used as record set name
	 * @return recordSetStemName
	 */
	public String getRecordSetStemName();
	
	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap);

	/**
	 * convert record logview config data to GDE config keys into records section
	 * @param header reference to header data, contain all key value pairs
	 * @param lov2osdMap reference to the map where the key mapping
	 * @param channelNumber
	 * @return adapted record configuration as delimited string
	 */
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber);
	
	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device
	 */
	public int getLovDataByteSize();
	
	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	public int[] convertDataBytes(int[] points, byte[] dataBuffer);

	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
	 * since this is a long term operation the progress bar should be updated to signal busyness to user 
	 * @param recordSet
	 * @param dataBuffer
	 * @param doUpdateProgressBar
	 */
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException;

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
	 * since this is a long term operation the progress bar should be updated to signal busyness to user 
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 */
	public void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException;

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	public String[] prepareDataTableRow(RecordSet recordSet, int rowIndex);
	
	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double translateValue(Record record, double value);

	/**
	 * function to reverse translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(Record record, double value);

	/**
	 * function to calculate values for inactive record which need to be calculated
	 * at least an update of the graphics window should be included at the end of this method
	 */
	public void makeInActiveDisplayable(RecordSet recordSet);

	/**
	 * check and update visibility status of all records according the available device configuration
	 * this function must have only implementation code if the device implementation supports different configurations
	 * where some curves are hided for better overview 
	 * example: if device supports voltage, current and height and no sensors are connected to voltage and current
	 * it makes less sense to display voltage and current curves, if only height has measurement data
	 * at least an update of the graphics window should be included at the end of this method
	 */
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck);

	/**
	 * @param isChangePropery the isChangePropery to set
	 */
	public void setChangePropery(boolean isChangePropery);

	/**
	 * writes updated device properties XML if isChangePropery == true;
	 */
	public void storeDeviceProperties();
	
	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys();
	
	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	public void open_closeCommPort(); 
	
	/**
	 * method to modify open/close serial port menu toolbar button and device menu entry
	 * this enable different naming instead open/close start/stop gathering data from device
	 * and must be called within specific device constructor
	 * @param useIconSet  DeviceSerialPort.ICON_SET_OPEN_CLOSE | DeviceSerialPort.ICON_SET_START_STOP
	 */
	void configureSerialPortMenu(int useIconSet); 
	
	/**
	 * get calculation thread to enable join , isAlive, ...
	 */
	public CalculationThread getCalculationThread();
	
	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	public int[] getCellVoltageOrdinals();
	
	/**
	 * query if the actual record set of this device contains GPS data to enable KML export to enable google earth visualization 
	 * @return true|false
	 */
	public boolean isActualRecordSetWithGpsData();
	
	/**
	 * export a file of the actual channel/record set
	 * @return full qualified file path depending of the file ending type
	 */
	public String exportFile(String fileEndingType);
	
	/**
	 * query the jar name of the active device implementation
	 * @return jar name of the active device
	 */
	public String getJarName();
	
	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	public Integer getGPS2KMLMeasurementOrdinal();
}
