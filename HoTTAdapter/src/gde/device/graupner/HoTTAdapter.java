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
    
    Copyright (c) 2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.ChannelPropertyTypes;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.MeasurementType;
import gde.device.graupner.hott.MessageIds;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;
import gde.io.FileHandler;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.dialog.IgcExportDialog;
import gde.utils.FileUtils;
import gde.utils.GPSHelper;
import gde.utils.WaitTimer;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Sample device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class HoTTAdapter extends DeviceConfiguration implements IDevice {
	final static Logger									log														= Logger.getLogger(HoTTAdapter.class.getName());

	final static String									SENSOR_COUNT									= "SensorCount";																//$NON-NLS-1$
	final static String									LOG_COUNT											= "LogCount";																		//$NON-NLS-1$
	final static String									SD_LOG_VERSION								= "SD-Log Version";															//$NON-NLS-1$
	final static Map<String, RecordSet>	recordSets										= new HashMap<String, RecordSet>();

	//HoTT sensor bytes 19200 Baud protocol 
	static boolean											IS_SLAVE_MODE									= false;
	final static byte										SENSOR_TYPE_RECEIVER_19200		= (byte) (0x80 & 0xFF);
	final static byte										SENSOR_TYPE_VARIO_19200				= (byte) (0x89 & 0xFF);
	final static byte										SENSOR_TYPE_GPS_19200					= (byte) (0x8A & 0xFF);
	final static byte										SENSOR_TYPE_GENERAL_19200			= (byte) (0x8D & 0xFF);
	final static byte										SENSOR_TYPE_ELECTRIC_19200		= (byte) (0x8E & 0xFF);
	final static byte										ANSWER_SENSOR_VARIO_19200			= (byte) (0x90 & 0xFF);
	final static byte										ANSWER_SENSOR_GPS_19200				= (byte) (0xA0 & 0xFF);
	final static byte										ANSWER_SENSOR_GENERAL_19200		= (byte) (0xD0 & 0xFF);
	final static byte										ANSWER_SENSOR_ELECTRIC_19200	= (byte) (0xE0 & 0xFF);

	//HoTT sensor bytes 115200 Baud protocol (actual no slave mode)
	//there is no real slave mode for this protocol
	final static byte										SENSOR_TYPE_RECEIVER_115200		= 0x34;
	final static byte										SENSOR_TYPE_VARIO_115200			= 0x37;
	final static byte										SENSOR_TYPE_GPS_115200				= 0x38;
	final static byte										SENSOR_TYPE_GENERAL_115200		= 0x35;
	final static byte										SENSOR_TYPE_ELECTRIC_115200		= 0x36;

	final static int										QUERY_GAP_MS									= 30;
	final static boolean								isSensorType[]								= { false, false, false, false, false };		//isReceiver, isVario, isGPS, isGeneral, isElectric

	public enum Sensor {
		RECEIVER("Receiver"), VARIO("Vario"), GPS("GPS"), GENRAL("General-Air"), ELECTRIC("Electric-Air");
		private final String	value;

		private Sensor(String v) {
			this.value = v;
		}

		public String value() {
			return this.value;
		}
	};

	//protocol definitions
	public enum Protocol {
		TYPE_19200_V3("19200 V3"), TYPE_19200_V4("19200 V4"), TYPE_115200("115200");

		private final String	value;

		private Protocol(String v) {
			this.value = v;
		}

		public String value() {
			return this.value;
		}

		public static Protocol fromValue(String v) {
			for (Protocol c : Protocol.values()) {
				if (c.value.equals(v)) {
					return c;
				}
			}
			throw new IllegalArgumentException(v);
		}

		public static String[] valuesAsStingArray() {
			StringBuilder sb = new StringBuilder();
			for (Protocol protocol : Protocol.values()) {
				sb.append(protocol.value).append(GDE.STRING_SEMICOLON);
			}
			return sb.toString().split(GDE.STRING_SEMICOLON);
		}
	}

	final DataExplorer					application;
	final Channels							channels;
	final HoTTAdapterDialog			dialog;
	final HoTTAdapterSerialPort	serialPort;

	static boolean							isFilterEnabled								= true;
	static boolean							isTolerateSignChangeLatitude	= false;
	static boolean							isTolerateSignChangeLongitude	= false;
	static double								latitudeTolranceFactor				= 90.0;
	static double								longitudeTolranceFactor				= 25.0;
	
	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public HoTTAdapter(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.hott.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.serialPort = this.application != null ? new HoTTAdapterSerialPort(this, this.application) : new HoTTAdapterSerialPort(this, null);
		this.dialog = new HoTTAdapterDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2404), Messages.getString(MessageIds.GDE_MSGT2404));
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
		
		HoTTAdapter.isFilterEnabled = this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER) != null ? Boolean.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER).getValue()) : true;
		HoTTAdapter.isTolerateSignChangeLatitude = this.getMeasruementProperty(3, 1, MeasurementPropertyTypes.TOLERATE_SIGN_CHANGE.value()) != null ? Boolean.parseBoolean(this.getMeasruementProperty(3, 1, MeasurementPropertyTypes.TOLERATE_SIGN_CHANGE.value()).getValue()) : false;
		HoTTAdapter.isTolerateSignChangeLongitude = this.getMeasruementProperty(3, 2, MeasurementPropertyTypes.TOLERATE_SIGN_CHANGE.value()) != null ? Boolean.parseBoolean(this.getMeasruementProperty(3, 2, MeasurementPropertyTypes.TOLERATE_SIGN_CHANGE.value()).getValue()) : false;
		HoTTAdapter.latitudeTolranceFactor = this.getMeasurementPropertyValue(3, 1, MeasurementPropertyTypes.FILTER_FACTOR.name()).toString().length() > 0 ? Double.parseDouble(this.getMeasurementPropertyValue(3, 1, MeasurementPropertyTypes.FILTER_FACTOR.name()).toString()) : 90.0;
		HoTTAdapter.longitudeTolranceFactor = this.getMeasurementPropertyValue(3, 2, MeasurementPropertyTypes.FILTER_FACTOR.name()).toString().length() > 0 ? Double.parseDouble(this.getMeasurementPropertyValue(3, 2, MeasurementPropertyTypes.FILTER_FACTOR.name()).toString()) : 25.0;
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public HoTTAdapter(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.hott.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.serialPort = this.application != null ? new HoTTAdapterSerialPort(this, this.application) : new HoTTAdapterSerialPort(this, null);
		this.dialog = new HoTTAdapterDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2404), Messages.getString(MessageIds.GDE_MSGT2404));
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
		
		HoTTAdapter.isFilterEnabled = this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER) != null ? Boolean.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER).getValue()) : true;
		HoTTAdapter.isTolerateSignChangeLatitude = this.getMeasruementProperty(3, 1, MeasurementPropertyTypes.TOLERATE_SIGN_CHANGE.value()) != null ? Boolean.parseBoolean(this.getMeasruementProperty(3, 1, MeasurementPropertyTypes.TOLERATE_SIGN_CHANGE.value()).getValue()) : false;
		HoTTAdapter.isTolerateSignChangeLongitude = this.getMeasruementProperty(3, 2, MeasurementPropertyTypes.TOLERATE_SIGN_CHANGE.value()) != null ? Boolean.parseBoolean(this.getMeasruementProperty(3, 2, MeasurementPropertyTypes.TOLERATE_SIGN_CHANGE.value()).getValue()) : false;
		HoTTAdapter.latitudeTolranceFactor = this.getMeasurementPropertyValue(3, 1, MeasurementPropertyTypes.FILTER_FACTOR.name()).toString().length() > 0 ? Double.parseDouble(this.getMeasurementPropertyValue(3, 1, MeasurementPropertyTypes.FILTER_FACTOR.name()).toString()) : 90.0;
		HoTTAdapter.longitudeTolranceFactor = this.getMeasurementPropertyValue(3, 2, MeasurementPropertyTypes.FILTER_FACTOR.name()).toString().length() > 0 ? Double.parseDouble(this.getMeasurementPropertyValue(3, 2, MeasurementPropertyTypes.FILTER_FACTOR.name()).toString()) : 25.0;
	}

	/**
	 * @return the serialPort
	 */
	@Override
	public HoTTAdapterSerialPort getCommunicationPort() {
		return this.serialPort;
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// ...
		return lov2osdMap;
	}

	/**
	 * convert record LogView config data to GDE config keys into records section
	 * @param header reference to header data, contain all key value pairs
	 * @param lov2osdMap reference to the map where the key mapping
	 * @param channelNumber 
	 * @return converted configuration data
	 */
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// ...
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	public int getLovDataByteSize() {
		return 0; // sometimes first 4 bytes give the length of data + 4 bytes for number
	}

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * this method is more usable for real logger, where data can be stored and converted in one block
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		//LogView doesn't support HoTT sensor logfiles
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte array with the data to be converted
	 */
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;
		int tmpVoltageRx, tmpHeight, tmpClimb3, tmpClimb10, tmpCapacity, tmpVoltage, tmpCellVoltage, tmpVoltage1, tmpVoltage2, tmpLatitude, tmpLongitude;

		switch (this.serialPort.protocolType) {
		case TYPE_19200_V3:
			switch (dataBuffer[1]) {
			case HoTTAdapter.SENSOR_TYPE_RECEIVER_19200:
				if (dataBuffer.length == 17) {
					//0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx
					tmpVoltageRx = (dataBuffer[6] & 0xFF);
					if (tmpVoltageRx > 0 && tmpVoltageRx < 100) {
						points[0] = 0; // seams not part of live data ?? (dataBuffer[15] & 0xFF) * 1000;
						points[1] = (dataBuffer[9] & 0xFF) * 1000;
						points[2] = (dataBuffer[5] & 0xFF) * 1000;
						points[3] = DataParser.parse2Short(dataBuffer, 11) * 1000;
						points[4] = (dataBuffer[13] & 0xFF) * -1000;
						points[5] = (dataBuffer[9] & 0xFF) * -1000;
						points[6] = tmpVoltageRx * 1000;
						points[7] = (dataBuffer[7] & 0xFF) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
				if (dataBuffer.length == 31) {
					//0=RXSQ, 1=Height, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
					points[0] = (dataBuffer[15] & 0xFF) * 1000;
					points[1] = DataParser.parse2Short(dataBuffer, 16) * 1000;
					points[2] = DataParser.parse2Short(dataBuffer, 22) * 1000;
					points[3] = DataParser.parse2Short(dataBuffer, 24) * 1000;
					points[4] = DataParser.parse2Short(dataBuffer, 26) * 1000;
					points[5] = (dataBuffer[8] & 0xFF) * 1000;
					points[6] = (dataBuffer[5] & 0xFF) * 1000;
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GPS_19200:
				if (dataBuffer.length == 40) {
					//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
					points[0] = (dataBuffer[15] & 0xFF) * 1000;
					points[1] = DataParser.parse2Short(dataBuffer, 20) * 10000 + DataParser.parse2Short(dataBuffer, 22);
					points[1] = dataBuffer[19] == 1 ? -1 * points[1] : points[1];
					points[2] = DataParser.parse2Short(dataBuffer, 25) * 10000 + DataParser.parse2Short(dataBuffer, 27);
					points[2] = dataBuffer[24] == 1 ? -1 * points[2] : points[2];
					points[3] = DataParser.parse2Short(dataBuffer, 31) * 1000;
					points[4] = DataParser.parse2Short(dataBuffer, 33) * 1000;
					points[5] = (dataBuffer[35] & 0xFF) * 1000;
					points[6] = DataParser.parse2Short(dataBuffer, 17) * 1000;
					points[7] = DataParser.parse2Short(dataBuffer, 29) * 1000;
					points[8] = (dataBuffer[16] & 0xFF) * 1000;
					points[9] = 0;
					points[10] = (dataBuffer[8] & 0xFF) * 1000;
					points[11] = (dataBuffer[5] & 0xFF) * 1000;
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
				if (dataBuffer.length == 48) {
					//0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2							
					points[0] = (dataBuffer[15] & 0xFF) * 1000;
					points[1] = DataParser.parse2Short(dataBuffer, 40) * 1000;
					points[2] = DataParser.parse2Short(dataBuffer, 38) * 1000;
					points[3] = DataParser.parse2Short(dataBuffer, 42) * 1000;
					points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
					points[5] = 0; //5=Balance
					for (int j = 0; j < 6; j++) {
						points[j + 6] = (dataBuffer[16 + j] & 0xFF) * 1000;
						if (points[j + 6] > 0) {
							maxVotage = points[j + 6] > maxVotage ? points[j + 6] : maxVotage;
							minVotage = points[j + 6] < minVotage ? points[j + 6] : minVotage;
						}
					}
					//calculate balance on the fly
					points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0);
					points[12] = DataParser.parse2Short(dataBuffer, 31) * 1000;
					points[13] = DataParser.parse2Short(dataBuffer, 33) * 1000;
					points[14] = DataParser.parse2Short(dataBuffer, 35) * 1000;
					points[15] = (dataBuffer[37] & 0xFF) * 1000;
					points[16] = DataParser.parse2Short(dataBuffer, 29) * 1000;
					points[17] = DataParser.parse2Short(dataBuffer, 22) * 1000;
					points[18] = DataParser.parse2Short(dataBuffer, 24) * 1000;
					points[19] = (dataBuffer[26] & 0xFF) * 1000;
					points[20] = (dataBuffer[27] & 0xFF) * 1000;
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
				if (dataBuffer.length == 51) {
					//0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2 		
					points[0] = (dataBuffer[15] & 0xFF) * 1000;
					points[1] = DataParser.parse2Short(dataBuffer, 40) * 1000;
					points[2] = DataParser.parse2Short(dataBuffer, 38) * 1000;
					points[3] = DataParser.parse2Short(dataBuffer, 42) * 1000;
					points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
					points[5] = 0; //5=Balance
					for (int j = 0; j < 14; j++) {
						points[j + 6] = (dataBuffer[16 + j] & 0xFF) * 1000;
						if (points[j + 6] > 0) {
							maxVotage = points[j + 6] > maxVotage ? points[j + 6] : maxVotage;
							minVotage = points[j + 6] < minVotage ? points[j + 6] : minVotage;
						}
					}
					//calculate balance on the fly
					points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0);
					points[20] = DataParser.parse2Short(dataBuffer, 36) * 1000;
					points[21] = DataParser.parse2Short(dataBuffer, 44) * 1000;
					points[22] = (dataBuffer[46] & 0xFF) * 1000;
					points[23] = DataParser.parse2Short(dataBuffer, 30) * 1000;
					points[24] = DataParser.parse2Short(dataBuffer, 32) * 1000;
					points[25] = (dataBuffer[34] & 0xFF) * 1000;
					points[26] = (dataBuffer[35] & 0xFF) * 1000;
				}
				break;
			}
			break;

		case TYPE_19200_V4:
			switch (dataBuffer[1]) {
			case HoTTAdapter.SENSOR_TYPE_RECEIVER_19200:
				if (dataBuffer.length == 17) {
					//0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx
					tmpVoltageRx = (dataBuffer[6] & 0xFF);
					if (tmpVoltageRx > 0 && tmpVoltageRx < 10) {
						points[0] = 0; // seams not part of live data ?? (dataBuffer[15] & 0xFF) * 1000;
						points[1] = (dataBuffer[9] & 0xFF) * 1000;
						points[2] = (dataBuffer[5] & 0xFF) * 1000;
						points[3] = DataParser.parse2Short(dataBuffer, 11) * 1000;
						points[4] = (dataBuffer[13] & 0xFF) * 1000;
						points[5] = (dataBuffer[8] & 0xFF) * 1000;
						points[6] = tmpVoltageRx * 1000;
						points[7] = (dataBuffer[7] & 0xFF) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
				if (dataBuffer.length == 57) {
					//0=RXSQ, 1=Height, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
					points[0] = (dataBuffer[9] & 0xFF) * 1000;
					tmpHeight = DataParser.parse2Short(dataBuffer, 16);
					if (tmpHeight > 10 && tmpHeight < 5000) {
						points[1] = tmpHeight * 1000;
						points[2] = DataParser.parse2Short(dataBuffer, 22) * 1000;
					}
					tmpClimb3 = DataParser.parse2Short(dataBuffer, 24);
					tmpClimb10 = DataParser.parse2Short(dataBuffer, 26);
					if (tmpClimb3 > 20000 && tmpClimb10 > 20000 && tmpClimb3 < 40000 && tmpClimb10 < 40000) {
						points[3] = tmpClimb3 * 1000;
						points[4] = tmpClimb10 * 1000;
					}
					points[5] = (dataBuffer[6] & 0xFF) * 1000;
					points[6] = (dataBuffer[7] & 0xFF) * 1000;
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GPS_19200:
				if (dataBuffer.length == 57) {
					//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
					tmpLatitude = DataParser.parse2Short(dataBuffer, 20);
					tmpLongitude = DataParser.parse2Short(dataBuffer, 25);
					tmpHeight = DataParser.parse2Short(dataBuffer, 31);
					tmpClimb3 = dataBuffer[35] & 0xFF;
					if ((tmpLatitude == tmpLongitude || tmpLatitude > 0) && tmpHeight > 10 && tmpHeight < 5000 && tmpClimb3 > 80) {
						points[0] = (dataBuffer[9] & 0xFF) * 1000;
						points[1] = DataParser.parse2Short(dataBuffer, 20) * 10000 + DataParser.parse2Short(dataBuffer, 22);
						points[1] = dataBuffer[19] == 1 ? -1 * points[1] : points[1];
						points[2] = tmpLongitude * 10000 + DataParser.parse2Short(dataBuffer, 27);
						points[2] = dataBuffer[24] == 1 ? -1 * points[2] : points[2];
						points[3] = tmpHeight * 1000;
						points[4] = DataParser.parse2Short(dataBuffer, 33) * 1000;
						points[5] = tmpClimb3 * 1000;
						points[6] = DataParser.parse2Short(dataBuffer, 17) * 1000;
						points[7] = DataParser.parse2Short(dataBuffer, 29) * 1000;
						points[8] = (dataBuffer[38] & 0xFF) * 1000;
						points[9] = 0;
						points[10] = (dataBuffer[6] & 0xFF) * 1000;
						points[11] = (dataBuffer[7] & 0xFF) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
				if (dataBuffer.length == 57) {
					//0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2							
					tmpVoltage = DataParser.parse2Short(dataBuffer, 40);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 42);
					tmpHeight = DataParser.parse2Short(dataBuffer, 33);
					tmpClimb3 = dataBuffer[37] & 0xFF;
					tmpVoltage1 = DataParser.parse2Short(dataBuffer, 22);
					tmpVoltage2 = DataParser.parse2Short(dataBuffer, 24);
					if (tmpClimb3 > 80 && tmpHeight > 10 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600 && tmpCapacity >= points[3] / 1000) {
						points[0] = (dataBuffer[9] & 0xFF) * 1000;
						points[1] = tmpVoltage * 1000;
						points[2] = DataParser.parse2Short(dataBuffer, 38) * 1000;
						points[3] = tmpCapacity * 1000;
						points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
						if (tmpVoltage > 0) {
							for (int j = 0; j < 6; j++) {
								tmpCellVoltage = (dataBuffer[16 + j] & 0xFF);
								points[j + 6] = tmpCellVoltage > 0 ? tmpCellVoltage * 1000 : points[j + 6];
								if (points[j + 6] > 0) {
									maxVotage = points[j + 6] > maxVotage ? points[j + 6] : maxVotage;
									minVotage = points[j + 6] < minVotage ? points[j + 6] : minVotage;
								}
							}
							//calculate balance on the fly
							points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 10;
						}
						points[12] = DataParser.parse2Short(dataBuffer, 31) * 1000;
						points[13] = tmpHeight * 1000;
						points[14] = DataParser.parse2Short(dataBuffer, 35) * 1000;
						points[15] = tmpClimb3 * 1000;
						points[16] = DataParser.parse2Short(dataBuffer, 29) * 1000;
						points[17] = tmpVoltage1 * 1000;
						points[18] = tmpVoltage2 * 1000;
						points[19] = (dataBuffer[26] & 0xFF) * 1000;
						points[20] = (dataBuffer[27] & 0xFF) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
				if (dataBuffer.length == 57) {
					//0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2 		
					tmpVoltage = DataParser.parse2Short(dataBuffer, 40);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 42);
					tmpHeight = DataParser.parse2Short(dataBuffer, 36);
					tmpClimb3 = dataBuffer[46] & 0xFF;
					tmpVoltage1 = DataParser.parse2Short(dataBuffer, 30);
					tmpVoltage2 = DataParser.parse2Short(dataBuffer, 32);
					if (tmpClimb3 > 80 && tmpHeight > 10 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600 && tmpCapacity >= points[3] / 1000) {
						points[0] = (dataBuffer[9] & 0xFF) * 1000;
						points[1] = tmpVoltage * 1000;
						points[2] = DataParser.parse2Short(dataBuffer, 38) * 1000;
						points[3] = tmpCapacity * 1000;
						points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
						if (tmpVoltage > 0) {
							for (int j = 0; j < 14; j++) {
								tmpCellVoltage = (dataBuffer[16 + j] & 0xFF);
								points[j + 6] = tmpCellVoltage > 0 ? tmpCellVoltage * 1000 : points[j + 6];
								if (points[j + 6] > 0) {
									maxVotage = points[j + 6] > maxVotage ? points[j + 6] : maxVotage;
									minVotage = points[j + 6] < minVotage ? points[j + 6] : minVotage;
								}
							}
							//calculate balance on the fly
							points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 10;
						}
						points[20] = tmpHeight * 1000;
						points[21] = DataParser.parse2Short(dataBuffer, 44) * 1000;
						points[22] = tmpClimb3 * 1000;
						points[23] = tmpVoltage1 * 1000;
						points[24] = tmpVoltage2 * 1000;
						points[25] = (dataBuffer[34] & 0xFF) * 1000;
						points[26] = (dataBuffer[35] & 0xFF) * 1000;
					}
				}
				break;
			}
			break;

		case TYPE_115200:
			switch (dataBuffer[0]) {
			case HoTTAdapter.SENSOR_TYPE_RECEIVER_115200:
				if (dataBuffer.length == 21) {
					//0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx
					tmpVoltageRx = (dataBuffer[15] & 0xFF);
					if (tmpVoltageRx > 0 && tmpVoltageRx < 10) {
						points[0] = (dataBuffer[16] & 0xFF) * 1000;
						points[1] = (dataBuffer[17] & 0xFF) * 1000;
						points[2] = (dataBuffer[14] & 0xFF) * 1000;
						points[3] = DataParser.parse2Short(dataBuffer, 12) * 1000;
						points[4] = (dataBuffer[5] & 0xFF) * 1000;
						points[5] = (dataBuffer[4] & 0xFF) * 1000;
						points[6] = tmpVoltageRx * 1000;
						points[7] = (DataParser.parse2Short(dataBuffer, 10) + 20) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
				if (dataBuffer.length == 25) {
					//0=RXSQ, 1=Height, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
					points[0] = (dataBuffer[3] & 0xFF) * 1000;
					tmpHeight = DataParser.parse2Short(dataBuffer, 10) + 500;
					if (tmpHeight > 10 && tmpHeight < 5000) {
						points[1] = tmpHeight * 1000;
						points[2] = (DataParser.parse2Short(dataBuffer, 16) + 30000) * 1000;
					}
					tmpClimb3 = DataParser.parse2Short(dataBuffer, 18) + 30000;
					tmpClimb10 = DataParser.parse2Short(dataBuffer, 20) + 30000;
					if (tmpClimb3 > 20000 && tmpClimb10 > 20000 && tmpClimb3 < 40000 && tmpClimb10 < 40000) {
						points[3] = tmpClimb3 * 1000;
						points[4] = tmpClimb10 * 1000;
					}
					points[5] = dataBuffer[4] * 1000;
					points[6] = (dataBuffer[5] + 20) * 1000;
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GPS_115200:
				if (dataBuffer.length == 34) {
					//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
					tmpLatitude = DataParser.parse2Short(dataBuffer, 16);
					tmpLongitude = DataParser.parse2Short(dataBuffer, 20);
					tmpHeight = DataParser.parse2Short(dataBuffer, 14) + 500;
					tmpClimb3 = dataBuffer[30] + 120;
					if ((tmpLatitude == tmpLongitude || tmpLatitude > 0) && tmpHeight > 10 && tmpHeight < 5000 && tmpClimb3 > 80) {
						points[0] = (dataBuffer[3] & 0xFF) * 1000;
						points[1] = tmpLatitude * 10000 + DataParser.parse2Short(dataBuffer, 18);
						points[1] = dataBuffer[26] == 1 ? -1 * points[1] : points[1];
						points[2] = tmpLongitude * 10000 + DataParser.parse2Short(dataBuffer, 22);
						points[2] = dataBuffer[27] == 1 ? -1 * points[2] : points[2];
						points[3] = tmpHeight * 1000;
						points[4] = (DataParser.parse2Short(dataBuffer, 28) + 30000) * 1000;
						points[5] = tmpClimb3 * 1000;
						points[6] = DataParser.parse2Short(dataBuffer, 10) * 1000;
						points[7] = DataParser.parse2Short(dataBuffer, 12) * 1000;
						points[8] = DataParser.parse2Short(dataBuffer, 24) * 500;
						points[9] = 0;
						points[10] = dataBuffer[4] * 1000;
						points[11] = (dataBuffer[5] + 20) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
				if (dataBuffer.length == 49) {
					//0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
					tmpVoltage = DataParser.parse2Short(dataBuffer, 36);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 38);
					tmpHeight = DataParser.parse2Short(dataBuffer, 32) + 500;
					tmpClimb3 = dataBuffer[44] + 120;
					tmpVoltage1 = DataParser.parse2Short(dataBuffer, 22);
					tmpVoltage2 = DataParser.parse2Short(dataBuffer, 24);
					if (tmpClimb3 > 80 && tmpHeight > 10 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600 && tmpCapacity >= points[3] / 1000) {
						points[0] = (dataBuffer[3] & 0xFF) * 1000;
						points[1] = tmpVoltage * 1000;
						points[2] = DataParser.parse2Short(dataBuffer, 34) * 1000;
						points[3] = tmpCapacity * 1000;
						points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
						if (tmpVoltage > 0) {
							for (int i = 0, j = 0; i < 6; i++, j += 2) {
								tmpCellVoltage = DataParser.parse2Short(dataBuffer, j + 10);
								points[i + 6] = tmpCellVoltage > 0 ? tmpCellVoltage * 500 : points[i + 6];
								if (points[i + 6] > 0) {
									maxVotage = points[i + 6] > maxVotage ? points[i + 6] : maxVotage;
									minVotage = points[i + 6] < minVotage ? points[i + 6] : minVotage;
								}
							}
							//calculate balance on the fly
							points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 10;
						}
						points[12] = DataParser.parse2Short(dataBuffer, 30) * 1000;
						points[13] = tmpHeight * 1000;
						points[14] = (DataParser.parse2Short(dataBuffer, 42) + 30000) * 1000;
						points[15] = tmpClimb3 * 1000;
						points[16] = DataParser.parse2Short(dataBuffer, 40) * 1000;
						points[17] = tmpVoltage1 * 1000;
						points[18] = tmpVoltage2 * 1000;
						points[19] = (DataParser.parse2Short(dataBuffer, 26) + 20) * 1000;
						points[20] = (DataParser.parse2Short(dataBuffer, 28) + 20) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
				if (dataBuffer.length == 60) {
					//0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2 		
					tmpVoltage = DataParser.parse2Short(dataBuffer, 50);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 52);
					tmpHeight = DataParser.parse2Short(dataBuffer, 46) + 500;
					tmpClimb3 = dataBuffer[56] + 120;
					tmpVoltage1 = DataParser.parse2Short(dataBuffer, 38);
					tmpVoltage2 = DataParser.parse2Short(dataBuffer, 40);
					if (tmpClimb3 > 80 && tmpHeight > 10 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600 && tmpCapacity >= points[3] / 1000) {
						points[0] = (dataBuffer[3] & 0xFF) * 1000;
						points[1] = DataParser.parse2Short(dataBuffer, 50) * 1000;
						points[2] = DataParser.parse2Short(dataBuffer, 48) * 1000;
						points[3] = tmpCapacity * 1000;
						points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
						if (tmpVoltage > 0) {
							for (int i = 0, j = 0; i < 14; i++, j += 2) {
								tmpCellVoltage = DataParser.parse2Short(dataBuffer, j + 10);
								points[i + 6] = tmpCellVoltage > 0 ? tmpCellVoltage * 500 : points[i + 6];
								if (points[i + 6] > 0) {
									maxVotage = points[i + 6] > maxVotage ? points[i + 6] : maxVotage;
									minVotage = points[i + 6] < minVotage ? points[i + 6] : minVotage;
								}
							}
							//calculate balance on the fly
							points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 10;
						}
						points[20] = tmpHeight * 1000;
						points[21] = (DataParser.parse2Short(dataBuffer, 54) + 30000) * 1000;
						points[22] = (dataBuffer[46] + 120) * 1000;
						points[23] = tmpVoltage1 * 1000;
						points[24] = tmpVoltage2 * 1000;
						points[25] = (DataParser.parse2Short(dataBuffer, 42) + 20) * 1000;
						points[26] = (DataParser.parse2Short(dataBuffer, 44) + 20) * 1000;
					}
				}
				break;
			}
			break;
		}
		return points;
	}

	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * since this is a long term operation the progress bar should be updated to signal business to user 
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.getRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 1;
		Vector<Integer> timeStamps = new Vector<Integer>(1, 1);
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
		byte[] timeStampBuffer = new byte[timeStampBufferSize];
		if (!recordSet.isTimeStepConstant()) {
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8)
						+ ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
			}
		}
		HoTTAdapter.log.log(java.util.logging.Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString()); //$NON-NLS-1$

		for (int i = 0; i < recordDataSize; i++) {
			HoTTAdapter.log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize + timeStampBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i * dataBufferSize + timeStampBufferSize, convertBuffer, 0, dataBufferSize);

			for (int j = 0; j < points.length; j++) {
				points[j] = (((convertBuffer[0 + (j * 4)] & 0xff) << 24) + ((convertBuffer[1 + (j * 4)] & 0xff) << 16) + ((convertBuffer[2 + (j * 4)] & 0xff) << 8) + ((convertBuffer[3 + (j * 4)] & 0xff) << 0));
			}

			if (recordSet.isTimeStepConstant())
				recordSet.addPoints(points);
			else
				recordSet.addPoints(points, timeStamps.get(i) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		this.updateVisibilityStatus(recordSet, true);
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			String[] recordNames = recordSet.getRecordNames();
			for (int j = 0; j < recordNames.length; j++) {
				Record record = recordSet.get(recordNames[j]);
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb, 5=Velocity, 6=DistanceStart, 7=DirectionStart, 8=TripDistance, 9=VoltageRx, 10=TemperatureRx
				if ((j == 1 || j == 2) && record.getParent().getChannelConfigNumber() == 3) { // 1=GPS-longitude 2=GPS-latitude  
					int grad = record.get(rowIndex) / 1000000;
					double minuten = record.get(rowIndex) % 1000000 / 10000.0;
					dataTableRow[j + 1] = String.format("%d %.4f", grad, minuten); //$NON-NLS-1$
				}
				//0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx
				else if (j >= 0 && j <= 5 && record.getParent().getChannelConfigNumber() == 1){ //Receiver
					dataTableRow[j + 1] = String.format("%.0f",(record.get(rowIndex) / 1000.0));
				}
				else {
					dataTableRow[j + 1] = record.getDecimalFormat().format((offset + ((record.get(rowIndex) / 1000.0) - reduction) * factor));
				}
			}
		}
		catch (RuntimeException e) {
			HoTTAdapter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double translateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
		double newValue = 0;

		if (record.getParent().getChannelConfigNumber() == 3 && (record.getOrdinal() == 1 || record.getOrdinal() == 2)) { // 1=GPS-longitude 2=GPS-latitude 
			//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			int grad = ((int) (value / 1000));
			double minuten = (value - (grad * 1000.0)) / 10.0;
			newValue = grad + minuten / 60.0;
		}
		else {
			newValue = (value - reduction) * factor + offset;
		}

		HoTTAdapter.log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double reverseTranslateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
		double newValue = 0;

		if ((record.getOrdinal() == 1 || record.getOrdinal() == 2) && record.getParent().getChannelConfigNumber() == 3) { // 1=GPS-longitude 2=GPS-latitude  ) 
			//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			int grad = (int) value;
			double minuten = (value - grad * 1.0) * 60.0;
			newValue = (grad + minuten / 100.0) * 1000.0;
		}
		else {
			newValue = (value - offset) / factor + reduction;
		}

		HoTTAdapter.log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int channelConfigNumber = recordSet.getChannelConfigNumber();
		int displayableCounter = 0;
		boolean configChanged = this.isChangePropery();
		Record record;

		String[] measurementNames = this.getMeasurementNames(channelConfigNumber);
		String[] recordNames = recordSet.getRecordNames();
		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordNames.length; ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(recordNames[i]);
			HoTTAdapter.log.log(java.util.logging.Level.FINE, recordNames[i] + " = " + measurementNames[i]); //$NON-NLS-1$

			// update active state and displayable state if configuration switched with other names
			MeasurementType measurement = this.getMeasurement(channelConfigNumber, i);
			if (record.isActive() != measurement.isActive()) {
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				HoTTAdapter.log.log(java.util.logging.Level.FINE, "switch " + record.getName() + " to " + measurement.isActive()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (includeReasonableDataCheck) {
				record.setDisplayable(record.hasReasonableData() && measurement.isActive());
				HoTTAdapter.log.log(java.util.logging.Level.FINE, record.getName() + " hasReasonableData " + record.hasReasonableData()); //$NON-NLS-1$ 
			}

			if (record.isActive() && record.isDisplayable()) {
				HoTTAdapter.log.log(java.util.logging.Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
				++displayableCounter;
			}
		}
		HoTTAdapter.log.log(java.util.logging.Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
		this.setChangePropery(configChanged); //reset configuration change indicator to previous value, do not vote automatic configuration change at all
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {

		if (recordSet.getChannelConfigNumber() == 3) { // 1=GPS-longitude 2=GPS-latitude 3=Height
			//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			Record recordLatitude = recordSet.get(1);
			Record recordLongitude = recordSet.get(2);
			Record recordAlitude = recordSet.get(3);
			if (recordLatitude.hasReasonableData() && recordLongitude.hasReasonableData() && recordAlitude.hasReasonableData()) {
				int recordSize = recordLatitude.realSize();
				int startAltitude = recordAlitude.get(0); // using this as start point might be sense less if the GPS data has no 3D-fix
				//check GPS latitude and longitude				
				int indexGPS = 0;
				int i = 0;
				for (; i < recordSize; ++i) {
					if (recordLatitude.get(i) != 0 && recordLongitude.get(i) != 0) {
						indexGPS = i;
						++i;
						break;
					}
				}
				startAltitude = recordAlitude.get(indexGPS); //set initial altitude to enable absolute altitude calculation 		

				GPSHelper.calculateTripLength(this, recordSet, 1, 2, 3, startAltitude, 7, 9);
				this.application.updateStatisticsData(true);
				this.updateVisibilityStatus(recordSet, true);
			}
		}
	}

	/**
	 * @return the dialog
	 */
	@Override
	public HoTTAdapterDialog getDialog() {
		return this.dialog;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION };
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	public void open_closeCommPort() {
		switch (application.getMenuBar().getSerialPortIconSet()) {
		case DeviceCommPort.ICON_SET_IMPORT_CLOSE:
			importDeviceData();
			break;
			
		case DeviceCommPort.ICON_SET_START_STOP:
			this.serialPort.isInterruptedByUser = true;
			break;
		}
	}

	/**
	 * import device specific *.bin data files
	 */
	protected void importDeviceData() {
		String devicePath = this.application.getActiveDevice() != null ? GDE.FILE_SEPARATOR_UNIX + this.application.getActiveDevice().getName() : GDE.STRING_EMPTY;
		String searchDirectory = Settings.getInstance().getDataFilePath() + devicePath + GDE.FILE_SEPARATOR_UNIX;
		if (FileUtils.checkDirectoryExist(this.getDeviceConfiguration().getDataBlockPreferredDataLocation())) {
			searchDirectory = this.getDeviceConfiguration().getDataBlockPreferredDataLocation();
		}
		final FileDialog fd = this.application.openFileOpenDialog(Messages.getString(MessageIds.GDE_MSGT2400), new String[] { this.getDeviceConfiguration().getDataBlockPreferredFileExtention(),
				GDE.FILE_ENDING_STAR_STAR }, searchDirectory, null, SWT.MULTI);

		this.getDeviceConfiguration().setDataBlockPreferredDataLocation(fd.getFilterPath());

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					HoTTAdapter.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.FILE_SEPARATOR_UNIX + tmpFileName;
						if (!selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_BIN)) {
							if (selectedImportFile.contains(GDE.STRING_DOT)) {
								selectedImportFile = selectedImportFile.substring(0, selectedImportFile.indexOf(GDE.STRING_DOT));
							}
							selectedImportFile = selectedImportFile + GDE.FILE_ENDING_DOT_BIN;
						}
						HoTTAdapter.log.log(java.util.logging.Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							Integer channelConfigNumber = HoTTAdapter.this.application.getActiveChannelNumber();
							channelConfigNumber = channelConfigNumber == null ? 1 : channelConfigNumber;
							//String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.STRING_DOT) - 4, selectedImportFile.lastIndexOf(GDE.STRING_DOT));
							try {
								HoTTbinReader.read(selectedImportFile); //, HoTTAdapter.this, GDE.STRING_EMPTY, channelConfigNumber);
								WaitTimer.delay(500);
							}
							catch (Exception e) {
								HoTTAdapter.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally  {
					HoTTAdapter.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}

	/**
	 * update the file export menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	public void updateFileExportMenu(Menu exportMenue) {
		MenuItem convertKMZ3DRelativeItem;
		MenuItem convertKMZDAbsoluteItem;
		MenuItem convertIGCItem;

		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertKMZ3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT2405));
			convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					HoTTAdapter.log.log(java.util.logging.Level.FINEST, "convertKMZ3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2406));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					HoTTAdapter.log.log(java.util.logging.Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2407));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					HoTTAdapter.log.log(java.util.logging.Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
				}
			});
			
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertIGCItem = new MenuItem(exportMenue, SWT.PUSH);
			convertIGCItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0611));
			convertIGCItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					HoTTAdapter.log.log(java.util.logging.Level.FINEST, "convertIGCItem action performed! " + e); //$NON-NLS-1$
					new IgcExportDialog().open(2, 1, 3);
				}
			});
		}
	}

	/**
	 * update the file import menu by adding new entry to import device specific files
	 * @param importMenue
	 */
	public void updateFileImportMenu(Menu importMenue) {
		MenuItem importDeviceLogItem;

		if (importMenue.getItem(importMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {			
			new MenuItem(importMenue, SWT.SEPARATOR);

			importDeviceLogItem = new MenuItem(importMenue, SWT.PUSH);
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT2416));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					HoTTAdapter.log.log(java.util.logging.Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					importDeviceData();
				}
			});
		}
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE
	 */
	public void export2KMZ3D(int type) {
		//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGT2403), 2, 1, 3, 6, 5, 9, -1, type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
	}
		
	/**
	 * @return the translated latitude and longitude to IGC latitude {DDMMmmmN/S, DDDMMmmmE/W} for GPS devices only
	 */
	@Override
	public String translateGPS2IGC(RecordSet recordSet, int index, char fixValidity, int startAltitude, int offsetAltitude) {
		//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		Record recordLatitude = recordSet.get(1);
		Record recordLongitude = recordSet.get(2);
		Record gpsAlitude = recordSet.get(3);
		
		return String.format("%02d%05d%s%03d%05d%s%c%05.0f%05.0f", 																																														//$NON-NLS-1$
				recordLatitude.get(index) / 1000000, Double.valueOf(recordLatitude.get(index) % 1000000 / 10.0 + 0.5).intValue(), recordLatitude.get(index) > 0 ? "N" : "S",//$NON-NLS-1$
				recordLongitude.get(index) / 1000000, Double.valueOf(recordLongitude.get(index) % 1000000 / 10.0 + 0.5).intValue(), recordLongitude.get(index) > 0 ? "E" : "W",//$NON-NLS-1$
				fixValidity, (this.translateValue(gpsAlitude, gpsAlitude.get(index) / 1000.0) + offsetAltitude), (this.translateValue(gpsAlitude, gpsAlitude.get(index) / 1000.0) + offsetAltitude));
	}

	/**
	 * query if the actual record set of this device contains GPS data to enable KML export to enable google earth visualization 
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public boolean isActualRecordSetWithGpsData() {
		boolean containsGPSdata = false;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null && activeChannel.getNumber() == 3) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
				containsGPSdata = activeRecordSet.get(1).hasReasonableData() && activeRecordSet.get(2).hasReasonableData();
			}
		}
		return containsGPSdata;
	}

	/**
	 * export a file of the actual channel/record set
	 * @return full qualified file path depending of the file ending type
	 */
	@Override
	public String exportFile(String fileEndingType, boolean isExport2TmpDir) {
		String exportFileName = GDE.STRING_EMPTY;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && fileEndingType.contains(GDE.FILE_ENDING_KMZ)) {
				//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
				exportFileName = new FileHandler().exportFileKMZ(2, 1, 3, 6, 5, 9, -1, true, isExport2TmpDir);
			}
		}
		return exportFileName;
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
		//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		return 6;
	}
	
	
	/**
	 * query the channel property of type getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER)
	 * @return true if curve point should be filtered
	 */
	@Override
	public boolean isFilterEnabled() {
		return HoTTAdapter.isFilterEnabled;
	}

	/**
	 * get the curve point device individual filtered if required
	 */
	@Override
	public Integer getFilteredPoint(int channelNumber, Record record, int index) {
		switch (channelNumber) {
		case 3: //GPS
			 switch (record.getOrdinal()) {
			 case 1: //Latitude
				 return record.realGet(index);
			 case 2: //Longitude
				 return record.realGet(index);
			 }			
		}
		return record.realGet(index);
	}

}
