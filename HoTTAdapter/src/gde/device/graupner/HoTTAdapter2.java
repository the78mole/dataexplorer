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
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.Analyzer;
import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.data.Channel;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.ChannelPropertyTypes;
import gde.device.DataTypes;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.graupner.hott.MessageIds;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.histo.cache.VaultCollector;
import gde.histo.device.IHistoDevice;
import gde.histo.device.UniversalSampler;
import gde.histo.utils.PathUtils;
import gde.io.DataParser;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.Messages;
import gde.utils.FileUtils;
import gde.utils.GPSHelper;
import gde.utils.ObjectKeyCompliance;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

/**
 * Sample device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class HoTTAdapter2 extends HoTTAdapter implements IDevice, IHistoDevice {
	final static Logger			log											= Logger.getLogger(HoTTAdapter2.class.getName());

	public static final int	CHANNELS_CHANNEL_NUMBER	= 4;
	private static boolean isGPSdetected = false;

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public HoTTAdapter2(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		if (this.application.getMenuToolBar() != null) {
			String toolTipText = HoTTAdapter.getImportToolTip();
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, toolTipText, toolTipText);
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}

		setPickerParameters();
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public HoTTAdapter2(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		if (this.application.getMenuToolBar() != null) {
			String toolTipText = HoTTAdapter.getImportToolTip();
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, toolTipText, toolTipText);
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}

		setPickerParameters();
	}

	private void setPickerParameters() {
		this.pickerParameters.isChannelsChannelEnabled = this.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL) != null && this.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL).getValue() != ""
				? Boolean.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL).getValue()) : false;
		this.pickerParameters.isFilterEnabled = this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER) != null && this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER).getValue() != ""
				? Boolean.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER).getValue()) : true;
		this.pickerParameters.isFilterTextModus = this.getChannelProperty(ChannelPropertyTypes.TEXT_MODE) != null && this.getChannelProperty(ChannelPropertyTypes.TEXT_MODE).getValue() != ""
				? Boolean.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.TEXT_MODE).getValue()) : false;
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		this.pickerParameters.latitudeToleranceFactor = this.getMeasurementPropertyValue(application.getActiveChannelNumber(), 20, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().length() > 0
				? Double.parseDouble(this.getMeasurementPropertyValue(application.getActiveChannelNumber(), 20, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString()) : 90.0;
		this.pickerParameters.longitudeToleranceFactor = this.getMeasurementPropertyValue(application.getActiveChannelNumber(), 21, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().length() > 0
				? Double.parseDouble(this.getMeasurementPropertyValue(application.getActiveChannelNumber(), 21, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString()) : 25.0;
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte array with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;
		int tmpHeight, tmpClimb3, tmpClimb10, tmpCapacity, tmpVoltage, tmpCurrent, tmpRevolution, tmpCellVoltage, tmpVoltage1, tmpVoltage2, tmpLatitudeGrad, tmpLongitudeGrad, tmpPackageLoss, tmpVoltageRx,
				tmpTemperatureRx;

		switch (this.serialPort.protocolType) {
		case TYPE_19200_V3:
			switch (dataBuffer[1]) {
			case HoTTAdapter2.SENSOR_TYPE_RECEIVER_19200:
				if (dataBuffer.length == 17) {
					//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
					points[0] = 0; // Rx->Tx PLoss
					points[1] = (dataBuffer[9] & 0xFF) * 1000;
					points[2] = (dataBuffer[5] & 0xFF) * 1000;
					points[3] = DataParser.parse2Short(dataBuffer, 11) * 1000;
					points[4] = (dataBuffer[13] & 0xFF) * -1000;
					points[5] = (dataBuffer[9] & 0xFF) * -1000;
					points[6] = (dataBuffer[6] & 0xFF) * 1000;
					points[7] = ((dataBuffer[7] & 0xFF) - 20) * 1000;
				}
				break;

			case HoTTAdapter2.SENSOR_TYPE_VARIO_19200:
				if (dataBuffer.length == 31) {
					//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
					//10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
					points[10] = (DataParser.parse2Short(dataBuffer, 16) - 500) * 1000;
					points[11] = (DataParser.parse2Short(dataBuffer, 22) - 30000) * 10;
					points[12] = (DataParser.parse2Short(dataBuffer, 24) - 30000) * 10;
					points[13] = (DataParser.parse2Short(dataBuffer, 26) - 30000) * 10;
				}
				break;

			case HoTTAdapter2.SENSOR_TYPE_GPS_19200:
				if (dataBuffer.length == 40) {
					// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
					// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
					// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
					// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
					points[20] = DataParser.parse2Short(dataBuffer, 20) * 10000 + DataParser.parse2Short(dataBuffer, 22);
					points[20] = dataBuffer[19] == 1 ? -1 * points[15] : points[15];
					points[21] = DataParser.parse2Short(dataBuffer, 25) * 10000 + DataParser.parse2Short(dataBuffer, 27);
					points[21] = dataBuffer[24] == 1 ? -1 * points[16] : points[16];
					points[10] = (DataParser.parse2Short(dataBuffer, 31) - 500) * 1000;
					points[11] = (DataParser.parse2Short(dataBuffer, 33) - 30000) * 10;
					points[12] = ((dataBuffer[35] & 0xFF) - 120) * 1000;
					points[22] = DataParser.parse2Short(dataBuffer, 17) * 1000;
					points[23] = DataParser.parse2Short(dataBuffer, 29) * 1000;
					points[24] = (dataBuffer[16] & 0xFF) * 1000;
					points[25] = 0;
				}
				break;

			case HoTTAdapter2.SENSOR_TYPE_GENERAL_19200:
				if (dataBuffer.length == 48) {
					// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
					// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
					// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
					// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
					// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
					// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
					// 57=LowestCellNumber, 58=Pressure, 59=Event G
					points[38] = DataParser.parse2Short(dataBuffer, 40) * 1000;
					points[39] = DataParser.parse2Short(dataBuffer, 38) * 1000;
					points[40] = DataParser.parse2Short(dataBuffer, 42) * 1000;
					points[41] = Double.valueOf(points[38] / 1000.0 * points[39]).intValue(); // power U*I [W];
					for (int j = 0; j < 6; j++) {
						points[j + 43] = (dataBuffer[23 + j] & 0xFF) * 1000;
						if (points[j + 43] > 0) {
							maxVotage = points[j + 43] > maxVotage ? points[j + 43] : maxVotage;
							minVotage = points[j + 43] < minVotage ? points[j + 43] : minVotage;
						}
					}
					//calculate balance on the fly
					points[42] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0);
					points[49] = DataParser.parse2Short(dataBuffer, 31) * 1000;
					points[10] = (DataParser.parse2Short(dataBuffer, 33) - 500) * 1000;
					points[11] = (DataParser.parse2Short(dataBuffer, 35) - 30000) * 10;
					points[12] = ((dataBuffer[37] & 0xFF) - 120) * 1000;
					points[50] = DataParser.parse2Short(dataBuffer, 29) * 1000;
					points[51] = DataParser.parse2Short(dataBuffer, 22) * 1000;
					points[52] = DataParser.parse2Short(dataBuffer, 24) * 1000;
					points[53] = ((dataBuffer[26] & 0xFF) - 20) * 1000;
					points[54] = ((dataBuffer[27] & 0xFF) - 20) * 1000;
				}
				break;

			case HoTTAdapter2.SENSOR_TYPE_ELECTRIC_19200:
				if (dataBuffer.length == 51) {
					// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
					// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
					// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
					// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
					// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
					// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
					// 57=LowestCellNumber, 58=Pressure, 59=Event G
					// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
					// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
					points[60] = DataParser.parse2Short(dataBuffer, 40) * 1000;
					points[61] = DataParser.parse2Short(dataBuffer, 38) * 1000;
					points[62] = DataParser.parse2Short(dataBuffer, 42) * 1000;
					points[63] = Double.valueOf(points[60] / 1000.0 * points[61]).intValue(); // power U*I [W];
					for (int j = 0; j < 14; j++) {
						points[j + 65] = (dataBuffer[40 + j] & 0xFF) * 1000;
						if (points[j + 65] > 0) {
							maxVotage = points[j + 65] > maxVotage ? points[j + 65] : maxVotage;
							minVotage = points[j + 65] < minVotage ? points[j + 65] : minVotage;
						}
					}
					//calculate balance on the fly
					points[64] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0);
					points[10] = (DataParser.parse2Short(dataBuffer, 36) - 500) * 1000;
					points[11] = (DataParser.parse2Short(dataBuffer, 44) - 30000) * 10;
					points[12] = ((dataBuffer[46] & 0xFF) - 120) * 1000;
					points[79] = DataParser.parse2Short(dataBuffer, 30) * 1000;
					points[80] = DataParser.parse2Short(dataBuffer, 32) * 1000;
					points[81] = ((dataBuffer[34] & 0xFF) - 20) * 1000;
					points[82] = ((dataBuffer[35] & 0xFF) - 20) * 1000;
				}
				break;
			}
			break;

		case TYPE_19200_V4:
			//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
			tmpPackageLoss = DataParser.parse2Short(dataBuffer, 11);
			tmpVoltageRx = (dataBuffer[6] & 0xFF);
			tmpTemperatureRx = (dataBuffer[7] & 0xFF) - 20;
			if (!this.pickerParameters.isFilterEnabled || tmpPackageLoss > -1 && tmpVoltageRx > -1 && tmpVoltageRx < 100 && tmpTemperatureRx < 120) {
				points[0] = 0; //Rx->Tx PLoss
				points[1] = (dataBuffer[9] & 0xFF) * 1000;
				points[2] = (dataBuffer[5] & 0xFF) * 1000;
				points[3] = tmpPackageLoss * 1000;
				points[4] = (dataBuffer[13] & 0xFF) * 1000;
				points[5] = (dataBuffer[8] & 0xFF) * 1000;
				points[6] = tmpVoltageRx * 1000;
				points[7] = tmpTemperatureRx * 1000;
				points[8] = (dataBuffer[10] & 0xFF) * 1000;
			}
			switch (dataBuffer[1]) {

			case HoTTAdapter2.SENSOR_TYPE_VARIO_19200:
				if (dataBuffer.length == 57) {
					// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
					// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
					tmpHeight = DataParser.parse2Short(dataBuffer, 16) - 500;
					if (tmpHeight > -490 && tmpHeight < 5000) {
						points[10] = tmpHeight * 1000;
						points[11] = (DataParser.parse2Short(dataBuffer, 22) - 30000) * 10;
					}
					tmpClimb3 = DataParser.parse2Short(dataBuffer, 24) - 30000;
					tmpClimb10 = DataParser.parse2Short(dataBuffer, 26) - 30000;
					if (tmpClimb3 > -10000 && tmpClimb10 > -10000 && tmpClimb3 < 10000 && tmpClimb10 < 10000) {
						points[12] = tmpClimb3 * 10;
						points[13] = tmpClimb10 * 10;
					}
				}
				break;

			case HoTTAdapter2.SENSOR_TYPE_GPS_19200:
				if (dataBuffer.length == 57) {
					//log.log(Level.INFO, StringHelper.byte2Hex2CharString(dataBuffer, dataBuffer.length));
					// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
					// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
					// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
					// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
					tmpLatitudeGrad = DataParser.parse2Short(dataBuffer, 20);
					tmpLongitudeGrad = DataParser.parse2Short(dataBuffer, 25);
					tmpHeight = DataParser.parse2Short(dataBuffer, 31) - 500;
					tmpClimb3 = (dataBuffer[35] & 0xFF) - 120;
					if ((tmpLatitudeGrad == tmpLongitudeGrad || tmpLatitudeGrad > 0) && tmpHeight > -490 && tmpHeight < 5000 && tmpClimb3 > -50) {
						points[20] = tmpLatitudeGrad * 10000 + DataParser.parse2Short(dataBuffer, 22);
						points[20] = dataBuffer[19] == 1 ? -1 * points[15] : points[15];
						points[21] = tmpLongitudeGrad * 10000 + DataParser.parse2Short(dataBuffer, 27);
						points[21] = dataBuffer[24] == 1 ? -1 * points[16] : points[16];
						points[10] = tmpHeight * 1000;
						points[11] = (DataParser.parse2Short(dataBuffer, 33) - 30000) * 10;
						points[12] = tmpClimb3 * 1000;
						points[22] = DataParser.parse2Short(dataBuffer, 17) * 1000;
						points[23] = DataParser.parse2Short(dataBuffer, 29) * 1000;
						points[24] = (dataBuffer[38] & 0xFF) * 1000;
						points[25] = 0;
						//26=NumSatellites 27=GPS-Fix 28=EventGPS
						points[26] = (dataBuffer[36] & 0xFF) * 1000;
						switch (dataBuffer[37]) { //sat-fix
						case '-':
							points[27] = 0;
							break;
						case '2':
							points[27] = 2000;
							break;
						case '3':
							points[27] = 3000;
							break;
						case 'D':
							points[27] = 4000;
							break;
						default:
							try {
								points[27] = Integer.valueOf(String.format("%c",dataBuffer[37])) * 1000;
							}
							catch (NumberFormatException e1) {
								points[27] = 1000;
							}
							break;
						}
						points[28] = (dataBuffer[14] & 0xFF) * 1000; //28=EventGPS
						//29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version
						points[29] = (dataBuffer[38] & 0xFF) * 1000; //Home direction
						//log.log(Level.INFO, StringHelper.byte2Hex2CharString(dataBuffer, 37, dataBuffer.length));
						if (HoTTAdapter2.isGPSdetected || (dataBuffer[46] & 0xFF) == 0x01) { //RCE Sparrow
							//30=servoPulse 31=n/a 32=voltage GU 33=HH:mm:ss.SSS 34=yy-dd-mm 35=Altitude MSL 36=ENL 37=Version
							points[30] = dataBuffer[47] * 1000; //servo pulse
							points[31] = 0;
							points[32] = dataBuffer[41] * 100; //voltage GU
							points[33] = dataBuffer[42] * 10000000 + dataBuffer[43] * 100000 + dataBuffer[44] * 1000 + dataBuffer[45]*10;//HH:mm:ss.SSS
							points[34] = ((dataBuffer[48]-48) * 1000000 + (dataBuffer[50]-48) * 10000 + (dataBuffer[49]-48) * 100) * 10;//yy-dd-mm
							points[35] = DataParser.parse2Short(dataBuffer, 39) * 1000;; //Altitude MSL
							points[36] = (dataBuffer[46] & 0xFF) * 1000; //ENL
							//three char
							points[37] = 4 * 1000; //Version
							HoTTAdapter2.isGPSdetected = true;
						}
						else { //SM GPS-Logger				
							//30=servoPulse 31=n/a 32=n/a 33=GyroX 34=GyroY 35=GyroZ 36=ENL 37=Version	
							points[30] = dataBuffer[39] * 1000; //Roll
							points[31] = dataBuffer[40] * 1000; //Pitch
							points[32] = dataBuffer[41] * 1000; //Yaw
							points[33] = DataParser.parse2Short(dataBuffer, 42) * 1000;; //Acc x
							points[34] = DataParser.parse2Short(dataBuffer, 44) * 1000;; //Acc y
							points[35] = DataParser.parse2Short(dataBuffer, 46) * 1000;; //Acc z
							points[36] = (dataBuffer[48] & 0xFF) * 1000; //ENL
							//three char
							points[37] = 125 * 1000; //Version
						}
					}
				}
				break;

			case HoTTAdapter2.SENSOR_TYPE_GENERAL_19200:
				if (dataBuffer.length == 57) {
					// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
					// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
					// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
					// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
					// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
					// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
					// 57=LowestCellNumber, 58=Pressure, 59=Event G
					tmpVoltage = DataParser.parse2Short(dataBuffer, 40);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 42);
					tmpHeight = DataParser.parse2Short(dataBuffer, 33) - 500;
					tmpClimb3 = (dataBuffer[37] & 0xFF) - 120;
					tmpVoltage1 = DataParser.parse2Short(dataBuffer, 22);
					tmpVoltage2 = DataParser.parse2Short(dataBuffer, 24);
					if (tmpClimb3 > -50 && tmpHeight > -490 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600 && tmpCapacity >= points[20] / 1000) {
						points[38] = tmpVoltage * 1000;
						points[39] = DataParser.parse2Short(dataBuffer, 38) * 1000;
						points[40] = tmpCapacity * 1000;
						points[41] = Double.valueOf(points[38] / 1000.0 * points[39]).intValue(); // power U*I [W];
						if (tmpVoltage > 0) {
							for (int j = 0; j < 6; j++) {
								tmpCellVoltage = (dataBuffer[16 + j] & 0xFF);
								points[j + 43] = tmpCellVoltage > 0 ? tmpCellVoltage * 1000 : points[j + 38];
								if (points[j + 38] > 0) {
									maxVotage = points[j + 43] > maxVotage ? points[j + 43] : maxVotage;
									minVotage = points[j + 43] < minVotage ? points[j + 43] : minVotage;
								}
							}
							//calculate balance on the fly
							points[42] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 10;
						}
						points[49] = DataParser.parse2Short(dataBuffer, 31) * 1000;
						points[10] = tmpHeight * 1000;
						points[11] = (DataParser.parse2Short(dataBuffer, 35) - 30000) * 10;
						points[12] = tmpClimb3 * 1000;
						points[50] = DataParser.parse2Short(dataBuffer, 29) * 1000;
						points[51] = tmpVoltage1 * 100;
						points[52] = tmpVoltage2 * 100;
						points[53] = ((dataBuffer[26] & 0xFF) - 20) * 1000;
						points[54] = ((dataBuffer[27] & 0xFF) - 20) * 1000;
						points[55] = 0; //55=Speed G
						points[56] = 0; //56=LowestCellVoltage
						points[57] = 0; //57=LowestCellNumber
						points[58] = 0; //58=Pressure
						points[59] = 0; //59=Event G
					}
				}
				break;

			case HoTTAdapter2.SENSOR_TYPE_ELECTRIC_19200:
				if (dataBuffer.length == 57) {
					// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
					// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
					// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
					// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
					// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
					// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
					// 57=LowestCellNumber, 58=Pressure, 59=Event G
					// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
					// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
					tmpVoltage = DataParser.parse2Short(dataBuffer, 40);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 42);
					tmpHeight = DataParser.parse2Short(dataBuffer, 36) - 500;
					tmpClimb3 = (dataBuffer[46] & 0xFF) - 120;
					tmpVoltage1 = DataParser.parse2Short(dataBuffer, 30);
					tmpVoltage2 = DataParser.parse2Short(dataBuffer, 32);
					if (tmpClimb3 > -50 && tmpHeight > -490 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600 && tmpCapacity >= points[37] / 1000) {
						points[60] = tmpVoltage * 1000;
						points[61] = DataParser.parse2Short(dataBuffer, 38) * 1000;
						points[62] = tmpCapacity * 1000;
						points[63] = Double.valueOf(points[60] / 1000.0 * points[61]).intValue(); // power U*I [W];
						if (tmpVoltage > 0) {
							for (int j = 0; j < 14; j++) {
								tmpCellVoltage = (dataBuffer[16 + j] & 0xFF);
								points[j + 65] = tmpCellVoltage > 0 ? tmpCellVoltage * 1000 : points[j + 65];
								if (points[j + 65] > 0) {
									maxVotage = points[j + 65] > maxVotage ? points[j + 65] : maxVotage;
									minVotage = points[j + 65] < minVotage ? points[j + 65] : minVotage;
								}
							}
							//calculate balance on the fly
							points[64] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 10;
						}
						points[10] = tmpHeight * 1000;
						points[11] = (DataParser.parse2Short(dataBuffer, 44) - 30000) * 10;
						points[12] = tmpClimb3 * 1000;
						points[79] = tmpVoltage1 * 100;
						points[80] = tmpVoltage2 * 100;
						points[81] = ((dataBuffer[34] & 0xFF) - 20) * 1000;
						points[82] = ((dataBuffer[35] & 0xFF) - 20) * 1000;
						points[83] = DataParser.parse2Short(dataBuffer, 47) * 1000;
						points[84] = 0; //84=MotorTime
						points[85] = 0; //85=Speed 81=Event E
						points[86] = 0; //86=Event E
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
				// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
				// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
				// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
				// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
				// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
				// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
				// 57=LowestCellNumber, 58=Pressure, 59=Event G
				// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
				// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
				// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
				// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M

				// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
				// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
				// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
				if (dataBuffer.length == 57) {
					tmpVoltage = DataParser.parse2Short(dataBuffer, 17);
					tmpCurrent = DataParser.parse2Short(dataBuffer, 21);
					tmpRevolution = DataParser.parse2Short(dataBuffer, 25);
					if (this.application.getActiveChannelNumber() == 4) {
						if (!this.pickerParameters.isFilterEnabled || tmpVoltage > -1 && tmpVoltage < 1000 && tmpCurrent < 2550 && tmpRevolution > -1 && tmpRevolution < 2000) {
							// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
							// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
							points[107] = tmpVoltage * 1000;
							points[108] = tmpCurrent * 1000;
							points[109] = DataParser.parse2Short(dataBuffer, 29) * 1000;
							points[110] = Double.valueOf(points[107] / 1000.0 * points[108]).intValue(); // power U*I [W];
							points[111] = tmpRevolution * 1000;
							points[112] = DataParser.parse2Short(dataBuffer, 33) * 1000;
						}
					}
					else {
						// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
						// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M
						if (!this.pickerParameters.isFilterEnabled || tmpVoltage > -1 && tmpVoltage < 1000 && tmpCurrent < 2550 && tmpRevolution > -1 && tmpRevolution < 2000) {
							points[87] = tmpVoltage * 1000;
							points[88] = tmpCurrent * 1000;
							points[89] = DataParser.parse2Short(dataBuffer, 29) * 1000;
							points[90] = Double.valueOf(points[87] / 1000.0 * points[88]).intValue(); // power U*I [W];
							points[91] = tmpRevolution * 1000;
							points[92] = DataParser.parse2Short(dataBuffer, 33) * 1000;
						}
					}
				}
				break;
			}
			break;

		case TYPE_115200:
			switch (dataBuffer[0]) {
			case HoTTAdapter2.SENSOR_TYPE_RECEIVER_115200:
				if (dataBuffer.length >= 21) {
					//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
					tmpPackageLoss = DataParser.parse2Short(dataBuffer, 12);
					tmpVoltageRx = dataBuffer[15] & 0xFF;
					tmpTemperatureRx = DataParser.parse2Short(dataBuffer, 10);
					if (!this.pickerParameters.isFilterEnabled || tmpPackageLoss > -1 && tmpVoltageRx > -1 && tmpVoltageRx < 100 && tmpTemperatureRx < 100) {
						this.pickerParameters.reverseChannelPackageLossCounter.add((dataBuffer[5] & 0xFF) == 0 && (dataBuffer[4] & 0xFF) == 0 ? 0 : 1);
						points[0] = this.pickerParameters.reverseChannelPackageLossCounter.getPercentage() * 1000;
						points[1] = (dataBuffer[17] & 0xFF) * 1000;
						points[2] = (dataBuffer[14] & 0xFF) * 1000;
						points[3] = tmpPackageLoss * 1000;
						points[4] = (dataBuffer[5] & 0xFF) * 1000;
						points[5] = (dataBuffer[4] & 0xFF) * 1000;
						points[6] = tmpVoltageRx * 1000;
						points[7] = tmpTemperatureRx * 1000;
						points[8] = (dataBuffer[18] & 0xFF) * 1000;
					}
				}
				break;

			case HoTTAdapter2.SENSOR_TYPE_VARIO_115200:
				if (dataBuffer.length >= 25) {
					// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
					// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
					tmpHeight = DataParser.parse2Short(dataBuffer, 10);
					if (tmpHeight > -490 && tmpHeight < 5000) {
						points[10] = tmpHeight * 1000;
						points[11] = DataParser.parse2Short(dataBuffer, 16) * 10;
					}
					tmpClimb3 = DataParser.parse2Short(dataBuffer, 18);
					tmpClimb10 = DataParser.parse2Short(dataBuffer, 20);
					if (tmpClimb3 > -10000 && tmpClimb10 > -10000 && tmpClimb3 < 10000 && tmpClimb10 < 10000) {
						points[12] = tmpClimb3 * 10;
						points[13] = tmpClimb10 * 10;
					}
				}
				break;

			case HoTTAdapter2.SENSOR_TYPE_GPS_115200:
				if (dataBuffer.length >= 46) {
					log.log(Level.INFO, StringHelper.byte2Hex2CharString(dataBuffer, dataBuffer.length));
					// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
					// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
					// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
					// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
					tmpLatitudeGrad = DataParser.parse2Short(dataBuffer, 16);
					tmpLongitudeGrad = DataParser.parse2Short(dataBuffer, 20);
					tmpHeight = DataParser.parse2Short(dataBuffer, 14);
					tmpClimb3 = dataBuffer[30];
					if ((tmpLatitudeGrad == tmpLongitudeGrad || tmpLatitudeGrad > 0) && tmpHeight > -490 && tmpHeight < 4500 && tmpClimb3 > -90) {
						points[20] = tmpLatitudeGrad * 10000 + DataParser.parse2Short(dataBuffer, 18);
						points[20] = dataBuffer[26] == 1 ? -1 * points[15] : points[15];
						points[21] = tmpLongitudeGrad * 10000 + DataParser.parse2Short(dataBuffer, 22);
						points[21] = dataBuffer[27] == 1 ? -1 * points[16] : points[16];
						points[10] = tmpHeight * 1000;
						points[11] = DataParser.parse2Short(dataBuffer, 28) * 10;
						points[12] = tmpClimb3 * 1000;
						points[22] = DataParser.parse2Short(dataBuffer, 10) * 1000;
						points[23] = DataParser.parse2Short(dataBuffer, 12) * 1000;
						points[24] = DataParser.parse2Short(dataBuffer, 24) * 500;
						points[25] = 0;
						points[26] = (dataBuffer[32] & 0xFF) * 1000;
						switch (dataBuffer[33]) { //sat-fix
						case '-':
							points[27] = 0;
							break;
						case '2':
							points[27] = 2000;
							break;
						case '3':
							points[27] = 3000;
							break;
						case 'D':
							points[27] = 4000;
							break;
						default:
							try {
								points[27] = Integer.valueOf(String.format("%c",dataBuffer[33])) * 1000;
							}
							catch (NumberFormatException e1) {
								points[27] = 1000;
							}
							break;
						}
						points[28] = (dataBuffer[1] & 0x0F) * 1000; // inverse event
						//29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
						points[29] = DataParser.parse2Short(dataBuffer, 34) * 1000; //Home direction
						if ((dataBuffer[46] & 0xFF) == 0x15) { //RCE Sparrow
							//30=servoPulse 31=fixed 32=Voltage 33=GPS hh:mm 34=GPS sss.SSS 35=MSL Altitude 36=ENL 37=Version	
							points[30] = dataBuffer[45] * 1000; //servo pulse
							points[31] = (dataBuffer[46] & 0xFF) * 1000; //0xDF
							points[32] = dataBuffer[38] * 100; //voltage GPS
							points[33] = DataParser.parse2Short(dataBuffer, 40) * 1000;; //GPS hh:mm
							points[34] = DataParser.parse2Short(dataBuffer, 42) * 1000;; //GPS ss:SSS
							points[35] = DataParser.parse2Short(dataBuffer, 36) * 1000;; //Altitude MSL
							points[36] = (dataBuffer[44] & 0xFF) * 1000; //ENL
							//three char
							log.log(Level.INFO, StringHelper.byte2Hex2CharString(dataBuffer, 47, dataBuffer.length));
						}
						else { //SM GPS-Logger				
							points[30] = dataBuffer[36] * 1000; //Roll
							points[31] = dataBuffer[37] * 1000; //Pitch
							points[32] = dataBuffer[38] * 1000; //Yaw
							points[33] = DataParser.parse2Short(dataBuffer, 40) * 1000; //Gyro x or GPS hh:mm
							points[34] = DataParser.parse2Short(dataBuffer, 42) * 1000; //Gyro y or GPS ss:SSS
							points[35] = DataParser.parse2Short(dataBuffer, 44) * 1000; //Gyro z or Altitude MSL
							points[36] = (dataBuffer[46] & 0xFF) * 1000; //ENL
						}
						points[37] = 0; //Version
					}
				}
				break;

			case HoTTAdapter2.SENSOR_TYPE_GENERAL_115200:
				if (dataBuffer.length >= 49) {
					// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
					// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
					// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
					// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
					// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
					// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
					// 57=LowestCellNumber, 58=Pressure, 59=Event G
					tmpVoltage = DataParser.parse2Short(dataBuffer, 36);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 38);
					tmpHeight = DataParser.parse2Short(dataBuffer, 32);
					tmpClimb3 = dataBuffer[44];
					tmpVoltage1 = DataParser.parse2Short(dataBuffer, 22);
					tmpVoltage2 = DataParser.parse2Short(dataBuffer, 24);
					if (tmpClimb3 > -50 && tmpHeight > -490 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600 && tmpCapacity >= points[20] / 1000) {
						points[38] = tmpVoltage * 1000;
						points[39] = DataParser.parse2Short(dataBuffer, 34) * 1000;
						points[40] = tmpCapacity * 1000;
						points[41] = Double.valueOf(points[38] / 1000.0 * points[39]).intValue(); // power U*I [W];
						if (tmpVoltage > 0) {
							for (int i = 0, j = 0; i < 6; i++, j += 2) {
								tmpCellVoltage = DataParser.parse2Short(dataBuffer, j + 10);
								points[i + 43] = tmpCellVoltage > 0 ? tmpCellVoltage * 500 : points[i + 38];
								if (points[i + 43] > 0) {
									maxVotage = points[i + 43] > maxVotage ? points[i + 43] : maxVotage;
									minVotage = points[i + 43] < minVotage ? points[i + 43] : minVotage;
								}
							}
							//calculate balance on the fly
							points[42] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 10;
						}
						points[49] = DataParser.parse2Short(dataBuffer, 30) * 1000;
						points[10] = tmpHeight * 1000;
						points[11] = DataParser.parse2Short(dataBuffer, 42) * 10;
						points[12] = tmpClimb3 * 1000;
						points[50] = DataParser.parse2Short(dataBuffer, 40) * 1000;
						points[51] = tmpVoltage1 * 100;
						points[52] = tmpVoltage2 * 100;
						points[53] = DataParser.parse2Short(dataBuffer, 26) * 1000;
						points[54] = DataParser.parse2Short(dataBuffer, 28) * 1000;
						points[55] = 0; //55=Speed G
						points[56] = 0; //56=LowestCellVoltage
						points[57] = 0; //57=LowestCellNumber
						points[58] = 0; //58=Pressure
						points[59] = 0; //59=Event G
					}
				}
				break;

			case HoTTAdapter2.SENSOR_TYPE_ELECTRIC_115200:
				if (dataBuffer.length >= 60) {
					// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
					// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
					// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
					// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
					// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
					// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
					// 57=LowestCellNumber, 58=Pressure, 59=Event G
					// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
					// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
					tmpVoltage = DataParser.parse2Short(dataBuffer, 50);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 52);
					tmpHeight = DataParser.parse2Short(dataBuffer, 46);
					tmpClimb3 = dataBuffer[56];
					tmpVoltage1 = DataParser.parse2Short(dataBuffer, 38);
					tmpVoltage2 = DataParser.parse2Short(dataBuffer, 40);
					if (tmpClimb3 > -50 && tmpHeight > -490 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600 && tmpCapacity >= points[37] / 1000) {
						points[60] = DataParser.parse2Short(dataBuffer, 50) * 1000;
						points[61] = DataParser.parse2Short(dataBuffer, 48) * 1000;
						points[62] = tmpCapacity * 1000;
						points[63] = Double.valueOf(points[60] / 1000.0 * points[61]).intValue(); // power U*I [W];
						if (tmpVoltage > 0) {
							for (int i = 0, j = 0; i < 14; i++, j += 2) {
								tmpCellVoltage = DataParser.parse2Short(dataBuffer, j + 10);
								points[i + 65] = tmpCellVoltage > 0 ? tmpCellVoltage * 500 : points[i + 65];
								if (points[i + 65] > 0) {
									maxVotage = points[i + 65] > maxVotage ? points[i + 65] : maxVotage;
									minVotage = points[i + 65] < minVotage ? points[i + 65] : minVotage;
								}
							}
							//calculate balance on the fly
							points[64] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 10;
						}
						points[10] = tmpHeight * 1000;
						points[11] = DataParser.parse2Short(dataBuffer, 54) * 10;
						points[12] = dataBuffer[46] * 1000;
						points[79] = tmpVoltage1 * 100;
						points[80] = tmpVoltage2 * 100;
						points[81] = DataParser.parse2Short(dataBuffer, 42) * 1000;
						points[82] = DataParser.parse2Short(dataBuffer, 44) * 1000;
						points[83] = DataParser.parse2Short(dataBuffer, 58) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
				// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
				// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
				// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
				// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
				// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
				// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
				// 57=LowestCellNumber, 58=Pressure, 59=Event G
				// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
				// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
				// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
				// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M

				// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
				// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
				// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
				if (dataBuffer.length >= 34) {
					tmpVoltage = DataParser.parse2Short(dataBuffer, 10);
					tmpCurrent = DataParser.parse2Short(dataBuffer, 14);
					tmpRevolution = DataParser.parse2Short(dataBuffer, 18);
					if (this.application.getActiveChannelNumber() == 4) {
						//107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
						if (!this.pickerParameters.isFilterEnabled || tmpVoltage > -1 && tmpVoltage < 1000 && tmpCurrent < 2550 && tmpRevolution > -1 && tmpRevolution < 2000) {
							points[107] = tmpVoltage * 1000;
							points[108] = tmpCurrent * 1000;
							points[109] = DataParser.parse2Short(dataBuffer, 22) * 1000;
							points[110] = Double.valueOf(points[107] / 1000.0 * points[108]).intValue(); // power U*I [W];
							points[111] = tmpRevolution * 1000;
							points[112] = DataParser.parse2Short(dataBuffer, 24) * 1000;
						}
					}
					else {
						//87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
						if (!this.pickerParameters.isFilterEnabled || tmpVoltage > -1 && tmpVoltage < 1000 && tmpCurrent < 2550 && tmpRevolution > -1 && tmpRevolution < 2000) {
							points[87] = tmpVoltage * 1000;
							points[88] = tmpCurrent * 1000;
							points[89] = DataParser.parse2Short(dataBuffer, 22) * 1000;
							points[90] = Double.valueOf(points[87] / 1000.0 * points[88]).intValue(); // power U*I [W];
							points[91] = tmpRevolution * 1000;
							points[92] = DataParser.parse2Short(dataBuffer, 24) * 1000;
						}
					}
				}
				break;
			case HoTTAdapter.SENSOR_TYPE_SERVO_POSITION_115200:
				if (dataBuffer.length >= 74) {
					//log.log(Level.INFO, StringHelper.byte2Hex2CharString(dataBuffer, dataBuffer.length));
					StringBuffer sb = new StringBuffer();
					for (int i = 0, j = 0; i < 16; i++, j+=2) {
						sb.append(String.format("%2d = %4d; ", i+1, DataParser.parse2Short(dataBuffer, 8 + j) / 16 + 50));					
					}
					log.log(Level.FINE, sb.toString());
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
	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		int[] points = new int[recordSet.getNoneCalculationRecordNames().length];
					String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 1;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
		int index = 0;
		for (int i = 0; i < recordDataSize; i++) {
			index = i * dataBufferSize + timeStampBufferSize;
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + index); //$NON-NLS-1$

			for (int j = 0; j < points.length; j++) {
				points[j] = (((dataBuffer[0 + (j * 4) + index] & 0xff) << 24) + ((dataBuffer[1 + (j * 4) + index] & 0xff) << 16) + ((dataBuffer[2 + (j * 4) + index] & 0xff) << 8)
						+ ((dataBuffer[3 + (j * 4) + index] & 0xff) << 0));
			}

			recordSet.addNoneCalculationRecordsPoints(points,
					(((dataBuffer[0 + (i * 4)] & 0xff) << 24) + ((dataBuffer[1 + (i * 4)] & 0xff) << 16) + ((dataBuffer[2 + (i * 4)] & 0xff) << 8) + ((dataBuffer[3 + (i * 4)] & 0xff) << 0)) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);		
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * Add record data points from file stream to each measurement.
	 * It is possible to add only none calculation records if makeInActiveDisplayable calculates the rest.
	 * Do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * Reduces memory and cpu load by taking measurement samples every x ms based on device setting |histoSamplingTime| .
	 * @param recordSet is the target object holding the records (curves) which include measurement curves and calculated curves
	 * @param dataBuffer holds rows for each time step (i = recordDataSize) with measurement data (j = recordNamesLength equals the number of measurements)
	 * @param recordDataSize is the number of time steps
	 */
	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, int[] maxPoints, int[] minPoints, Analyzer analyzer) throws DataInconsitsentException {
		if (maxPoints.length != minPoints.length || maxPoints.length == 0) throw new DataInconsitsentException("number of max/min points differs: " + maxPoints.length + "/" + minPoints.length); //$NON-NLS-1$

		int recordTimespan_ms = 10;
		UniversalSampler histoRandomSample = UniversalSampler.createSampler(recordSet.getChannelConfigNumber(), maxPoints, minPoints, recordTimespan_ms, analyzer);
		int[] points = histoRandomSample.getPoints();
		IntBuffer intBuffer = ByteBuffer.wrap(dataBuffer).asIntBuffer(); // no performance penalty compared to familiar bit shifting solution
		for (int i = 0, pointsLength = points.length; i < recordDataSize; i++) {
			for (int j = 0, iOffset = i * pointsLength + recordDataSize; j < pointsLength; j++) {
				points[j] = intBuffer.get(j + iOffset);
			}
			int timeStep_ms = intBuffer.get(i) / 10;
			if (histoRandomSample.capturePoints(timeStep_ms)) recordSet.addNoneCalculationRecordsPoints(points, timeStep_ms);
		}
		recordSet.syncScaleOfSyncableRecords();
		if (log.isLoggable(Level.FINE)) log.log(Level.INFO, String.format("%s processed: %,9d", recordSet.getChannelConfigName(), recordDataSize)); //$NON-NLS-1$
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				int ordinal = record.getOrdinal();
				// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
				// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
				// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
				// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
				// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
				// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
				// 57=LowestCellNumber, 58=Pressure, 59=Event G
				// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
				// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
				// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
				// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M

				// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
				// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
				// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
				if (ordinal >= 0 && ordinal <= 5) {
					dataTableRow[index + 1] = String.format("%.0f", (record.realGet(rowIndex) / 1000.0)); //$NON-NLS-1$
				}
				else if ((ordinal == 99 || ordinal == 119) && record.getUnit().equals(GDE.STRING_EMPTY)) { //ESC Warning
					dataTableRow[index + 1] = record.realGet(rowIndex) == 0
							? GDE.STRING_EMPTY
									: String.format("'%c'", ((record.realGet(rowIndex) / 1000)+64));
				}
				//RCE Sparrow 33=GPS hh:mm 34=GPS sss.SSS 
				else if (ordinal == 33 && record.getUnit().endsWith("HH:mm:ss.SSS")) { 
					dataTableRow[index + 1] = HoTTAdapter.getFormattedTime(record.realGet(rowIndex));
				}
				else if (ordinal == 34 && record.getUnit().endsWith("yy-MM-dd")) {
					dataTableRow[index + 1] = HoTTAdapter.getFormattedDate(record.realGet(rowIndex)/10);
				}
				else {
					dataTableRow[index + 1] = record.getFormattedTableValue(rowIndex);
				}
				++index;
			}
		}
		catch (RuntimeException e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * query if the given record is longitude or latitude of GPS data, such data needs translation for display as graph
	 * @param record
	 * @return
	 */
	@Override
	public boolean isGPSCoordinates(Record record) {
		//20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		final int latOrdinal = 20, lonOrdinal = 21;
		return record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal;
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
		double newValue = 0;
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
		// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
		// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M

		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
		// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
		// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
		final int latOrdinal = 20, lonOrdinal = 21;
		if (record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal) { //15=Latitude, 16=Longitude
			int grad = ((int) (value / 1000));
			double minuten = (value - (grad * 1000.0)) / 10.0;
			newValue = grad + minuten / 60.0;
		}
		else if (record.getAbstractParent().getChannelConfigNumber() == 4 && (record.getOrdinal() >= 87 && record.getOrdinal() <= 102)) {
			if (this.pickerParameters.isChannelPercentEnabled) {
				if (!record.getUnit().equals("%")) record.setUnit("%");
				factor = 0.250;
				reduction = 1500.0;
				newValue = (value - reduction) * factor;
			}
			else {
				if (!record.getUnit().equals("µsec")) record.setUnit("µsec");
				newValue = (value - reduction) * factor;
			}
		}
		else {
			newValue = (value - reduction) * factor + offset;
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		double newValue = 0;
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
		// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
		// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M

		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
		// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
		// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
		final int latOrdinal = 20, lonOrdinal = 21;
		if (record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal) { // 20=Latitude, 21=Longitude
			int grad = (int) value;
			double minuten = (value - grad * 1.0) * 60.0;
			newValue = (grad + minuten / 100.0) * 1000.0;
		}
		else if (record.getAbstractParent().getChannelConfigNumber() == 4 && (record.getOrdinal() >= 87 && record.getOrdinal() <= 102)) {
			if (this.pickerParameters.isChannelPercentEnabled) {
				if (!record.getUnit().equals("%")) record.setUnit("%");
				factor = 0.250;
				reduction = 1500.0;
				newValue = value / factor + reduction;
			}
			else {
				if (!record.getUnit().equals("µsec")) record.setUnit("µsec");
				newValue = (value - reduction) * factor;
			}
		}
		else {
			newValue = (value - offset) / factor + reduction;
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread,
	 * target is to make sure all data point not coming from device directly are available and can be displayed
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		if (recordSet != null) {
			calculateInactiveRecords(recordSet);
			recordSet.syncScaleOfSyncableRecords();
			this.updateVisibilityStatus(recordSet, true);
			this.application.updateStatisticsData();
		}
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 */
	@Override
	public void calculateInactiveRecords(RecordSet recordSet) {
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
		// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
		// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M

		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
		// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
		// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
		final int latOrdinal = 20, lonOrdinal = 21, altOrdinal = 10, tripOrdinal = 25;
		Record recordLatitude = recordSet.get(latOrdinal);
		Record recordLongitude = recordSet.get(lonOrdinal);
		Record recordAlitude = recordSet.get(altOrdinal);
		if (recordLatitude.hasReasonableData() && recordLongitude.hasReasonableData() && recordAlitude.hasReasonableData()) { // 13=Latitude,
																																																													// 14=Longitude 9=Altitude
			int recordSize = recordLatitude.realSize();
			int startAltitude = recordAlitude.get(8); // using this as start point might be sense less if the GPS data has no 3D-fix
			// check GPS latitude and longitude
			int indexGPS = 0;
			int i = 0;
			for (; i < recordSize; ++i) {
				if (recordLatitude.get(i) != 0 && recordLongitude.get(i) != 0) {
					indexGPS = i;
					++i;
					break;
				}
			}
			startAltitude = recordAlitude.get(indexGPS); // set initial altitude to enable absolute altitude calculation

			GPSHelper.calculateTripLength(this, recordSet, latOrdinal, lonOrdinal, altOrdinal, startAltitude, tripOrdinal);
		}
	}

	/**
	 * import device specific *.bin data files
	 */
	@Override
	protected void importDeviceData() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT2400), "LogData"); //$NON-NLS-1$

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					boolean isInitialSwitched = false;
					HoTTAdapter2.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						if (!selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_BIN) && !selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
							log.log(Level.WARNING, String.format("skip selectedImportFile %s since it has not a supported file ending", selectedImportFile));
						}
						log.log(java.util.logging.Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > MIN_FILENAME_LENGTH) {
							//String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT) - 4, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));

							String directoryName = ObjectKeyCompliance.getUpcomingObjectKey(Paths.get(selectedImportFile));
							if (!directoryName.isEmpty()) ObjectKeyCompliance.createObjectKey(directoryName);

							try {
								// use a copy of the picker parameters to avoid changes by the reader
								if (selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_BIN)) {
									HoTTbinReader2.read(selectedImportFile, new PickerParameters(HoTTAdapter2.this.pickerParameters));
								}
								else if (selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
									HoTTlogReader2.read(selectedImportFile, new PickerParameters(HoTTAdapter2.this.pickerParameters));
								}
								if (!isInitialSwitched) {
									Channel activeChannel = HoTTAdapter2.this.application.getActiveChannel();
									HoTTbinReader2.channels.switchChannel(activeChannel.getName());
									if (selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_BIN)) {
										activeChannel.switchRecordSet(HoTTbinReader2.recordSet.getName());
									}
									else if (selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
										activeChannel.switchRecordSet(HoTTlogReader2.recordSet.getName());
									}
									isInitialSwitched = true;
								}
								else {
									HoTTAdapter2.this.makeInActiveDisplayable(HoTTbinReader2.recordSet);
								}
								WaitTimer.delay(500);
							}
							catch (Exception e) {
								log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					HoTTAdapter2.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}

	/**
	 * update the file export menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	@Override
	public void updateFileExportMenu(Menu exportMenue) {
		MenuItem convertKMZ3DRelativeItem;
		MenuItem convertKMZDAbsoluteItem;
		//		MenuItem convertGPXItem;
		//		MenuItem convertGPXGarminItem;

		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0732))) {
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertKMZ3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT2405));
			convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKMZ3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2406));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2407));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
				}
			});

			//			convertGPXItem = new MenuItem(exportMenue, SWT.PUSH);
			//			convertGPXItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0728));
			//			convertGPXItem.addListener(SWT.Selection, new Listener() {
			//				public void handleEvent(Event e) {
			//					log.log(java.util.logging.Level.FINEST, "convertGPXItem action performed! " + e); //$NON-NLS-1$
			//					export2GPX(false);
			//				}
			//			});
			//
			//			convertGPXGarminItem = new MenuItem(exportMenue, SWT.PUSH);
			//			convertGPXGarminItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0729));
			//			convertGPXGarminItem.addListener(SWT.Selection, new Listener() {
			//				public void handleEvent(Event e) {
			//					log.log(java.util.logging.Level.FINEST, "convertGPXGarminItem action performed! " + e); //$NON-NLS-1$
			//					export2GPX(true);
			//				}
			//			});
		}
	}

	/**
	 * update the file import menu by adding new entry to import device specific files
	 * @param importMenue
	 */
	@Override
	public void updateFileImportMenu(Menu importMenue) {
		MenuItem importDeviceLogItem;

		if (importMenue.getItem(importMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {
			new MenuItem(importMenue, SWT.SEPARATOR);

			importDeviceLogItem = new MenuItem(importMenue, SWT.PUSH);
			String[] messageParams = new String[GDE.MOD1.length + 1];
			System.arraycopy(GDE.MOD1, 0, messageParams, 1, GDE.MOD1.length);
			messageParams[0] = this.getDeviceConfiguration().getDataBlockPreferredFileExtention();
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT2416, messageParams));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT2416));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					importDeviceData();
				}
			});
		}
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE
	 */
	@Override
	public void export2KMZ3D(int type) {
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
		// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
		// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M

		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
		// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
		// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
		final int latOrdinal = 20, lonOrdinal = 21, altOrdinal = 10, climbOrdinal = 11, speedOrdinal = 22, tripOrdinal = 25;
		new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGT2403), lonOrdinal, latOrdinal, altOrdinal, speedOrdinal, climbOrdinal, tripOrdinal, -1,
				type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE | DeviceConfiguration.HEIGHT_CLAMPTOGROUND
	 */
	@Override
	public void export2GPX(final boolean isGarminExtension) {
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
		// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
		// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M

		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
		// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
		// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
		if (isGarminExtension)
			new FileHandler().exportFileGPX(Messages.getString(gde.messages.MessageIds.GDE_MSGT0730), 20, 21, 10, 22, -1, -1, -1, -1, new int[] { -1, -1, -1 });
		else
			new FileHandler().exportFileGPX(Messages.getString(gde.messages.MessageIds.GDE_MSGT0730), 20, 21, 10, 22, -1, -1, -1, -1, new int[0]);
	}

	/**
	 * @return the translated latitude and longitude to IGC latitude {DDMMmmmN/S, DDDMMmmmE/W} for GPS devices only
	 */
	@Override
	public String translateGPS2IGC(RecordSet recordSet, int index, char fixValidity, int startAltitude, int offsetAltitude) {
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
		// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
		// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M

		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
		// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
		// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
		final int latOrdinal = 20, lonOrdinal = 21, altOrdinal = 10;
		Record recordLatitude = recordSet.get(latOrdinal);
		Record recordLongitude = recordSet.get(lonOrdinal);
		Record gpsAlitude = recordSet.get(altOrdinal);

		return String.format("%02d%05d%s%03d%05d%s%c%05.0f%05.0f", //$NON-NLS-1$
				recordLatitude.get(index) / 1000000, Double.valueOf(recordLatitude.get(index) % 1000000 / 10.0 + 0.5).intValue(), recordLatitude.get(index) > 0 ? "N" : "S", //$NON-NLS-1$
				recordLongitude.get(index) / 1000000, Double.valueOf(recordLongitude.get(index) % 1000000 / 10.0 + 0.5).intValue(), recordLongitude.get(index) > 0 ? "E" : "W", //$NON-NLS-1$
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
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
				// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
				// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
				// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
				// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
				// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
				// 57=LowestCellNumber, 58=Pressure, 59=Event G
				// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
				// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
				// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
				// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M

				// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
				// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
				// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
				final int latOrdinal = 20, lonOrdinal = 21;
				containsGPSdata = activeRecordSet.get(latOrdinal).hasReasonableData() && activeRecordSet.get(lonOrdinal).hasReasonableData();
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
				// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
				// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
				// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
				// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
				// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
				// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
				// 57=LowestCellNumber, 58=Pressure, 59=Event G
				// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
				// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
				// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
				// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M

				// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
				// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
				// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
				final int latOrdinal = 20, lonOrdinal = 21, altOrdinal = 10, climbOrdinal = 11, tripOrdinal = 25;
				final int additionalMeasurementOrdinal = this.getGPS2KMZMeasurementOrdinal();
				exportFileName = new FileHandler().exportFileKMZ(lonOrdinal, latOrdinal, altOrdinal, additionalMeasurementOrdinal, climbOrdinal, tripOrdinal, -1, true, isExport2TmpDir);
			}
		}
		return exportFileName;
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
		// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
		// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M

		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
		// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
		// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
		if (kmzMeasurementOrdinal == null) // keep usage as initial supposed and use speed measurement ordinal
			return 22;

		return kmzMeasurementOrdinal;
	}

	/**
	 * check and adapt stored measurement properties against actual record set records which gets created by device properties XML
	 * - calculated measurements could be later on added to the device properties XML
	 * - devices with battery cell voltage does not need to all the cell curves which does not contain measurement values
	 * @param fileRecordsProperties - all the record describing properties stored in the file
	 * @param recordSet - the record sets with its measurements build up with its measurements from device properties XML
	 * @return string array of measurement names which match the ordinal of the record set requirements to restore file record properties
	 */
	@Override
	public String[] crossCheckMeasurements(String[] fileRecordsProperties, RecordSet recordSet) {
		//check for HoTTAdapter2 file contained record properties which are not contained in actual configuration

		//3.2.7 extend this measurements: 66/86=TemperatureM 2 67/87=Voltage_min, 68/88=Current_max, 69/89=Revolution_max, 70/90=Temperature1_max, 71/91=Temperature2_max
		//3.3.1 extend this measurements: 9=EventRx, 14=EventVario, 21=NumSatellites 22=GPS-Fix 23=EventGPS, 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G, 70=MotorTime 71=Speed 72=Event E, 85/105=Event M
		//3.4.6 extend this.measurements: 24=HomeDirection 25=Roll 26=Pitch 27=Yaw 28=GyroX 29=GyroY 30=GyroZ 31=Vibration 32=Version
		//3.5.0 extend this.measurements: 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5

		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
		// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
		// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M

		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
		// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
		// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M

		StringBuilder sb = new StringBuilder().append(GDE.LINE_SEPARATOR);

		String[] recordKeys = recordSet.getRecordNames();
		Vector<String> cleanedRecordNames = new Vector<String>();
		//incoming filePropertiesRecordNames may mismatch recordKeyNames, but addNoneCalculation will use original name
		Vector<String> noneCalculationRecordNames = new Vector<String>();
		Vector<String> fileRecordsPropertiesVector = new Vector<String>();
		fileRecordsPropertiesVector.addAll(Arrays.asList(fileRecordsProperties));


		try {
			switch (fileRecordsProperties.length) {
			case 44: //Android HoTTAdapter3 - special case
				for (int i = 0, j = 0; i < recordKeys.length; i++) {
					switch (i) {
					case 8:  //8=VoltageRx_min
					case 9:  //EventRx
					case 14: //EventVario
					case 15: //misc Vario_1
					case 16: //misc Vario_2
					case 17: //misc Vario_3
					case 18: //misc Vario_4
					case 19: //misc Vario_5
					case 26: //NumSatellites
					case 27: //GPS-Fix
					case 28: //EventGPS
					case 29: //HomeDirection
					case 30: //Roll
					case 31: //Pitch
					case 32: //Yaw
					case 33: //GyroX
					case 34: //GyroY
					case 35: //GyroZ
					case 36: //Vibration
					case 37: //Version
					case 42: //Balance G,
					case 43: //CellVoltage G1
					case 44: //CellVoltage G2
					case 45: //CellVoltage G3
					case 46: //CellVoltage G4
					case 47: //CellVoltage G5
					case 48: //CellVoltage G6
					case 49: //Voltage G1,
					case 50: //Voltage G2,
					case 53: //Temperature G1,
					case 54: //Temperature G2
					case 55: //Speed G
					case 56: //LowestCellVoltage
					case 57: //LowestCellNumber
					case 58: //Pressure
					case 59: //Event G
					case 60: //Voltage E,
					case 61: //Current E,
					case 62: //Capacity E,
					case 63: //Power E,
					case 83: //Revolution E
					case 84: //MotorTime E
					case 85: //Speed E
					case 86: //Event E
					case 87: //Voltage M,
					case 88: //Current M,
					case 89: //Capacity M,
					case 90: //Power M,
					case 91: //Revolution M
					case 92: //TemperatureM 2
					case 94: //Voltage_min
					case 95: //Current_max
					case 96: //Revolution_max
					case 97: //Temperature1_max
					case 98: //Temperature2_max
					case 99: //Event M
						sb.append(String.format("added measurement set to isCalculation=true -> %s\n", recordKeys[i]));
						recordSet.get(i).setActive(null);
						break;
					default:
						HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[j], Record.DELIMITER, Record.propertyKeys);
						sb.append(String.format("%19s match %19s isAvtive = %s\n", recordKeys[i], recordProps.get(Record.NAME), recordProps.get(Record.IS_ACTIVE)));
						cleanedRecordNames.add(recordKeys[i]);
						noneCalculationRecordNames.add(recordProps.get(Record.NAME));
						if (fileRecordsProperties[j].contains("_isActive=false"))
							recordSet.get(i).setActive(false);
						++j;
						break;
					}
				}
				break;

			case 58: //General, GAM, EAM (no ESC) prior to 3.0.4 added PowerOff, BatterieLow, Reset, reserve
				for (int i = 0, j = 0; i < recordKeys.length; i++) {
					switch (i) { //list of added measurements
					case 8:  //8=VoltageRx_min
					case 9:  //EventRx
					case 14: //EventVario
					case 15: //misc Vario_1
					case 16: //misc Vario_2
					case 17: //misc Vario_3
					case 18: //misc Vario_4
					case 19: //misc Vario_5
					case 26: //NumSatellites
					case 27: //GPS-Fix
					case 28: //EventGPS
					case 29: //HomeDirection
					case 30: //Roll
					case 31: //Pitch
					case 32: //Yaw
					case 33: //GyroX
					case 34: //GyroY
					case 35: //GyroZ
					case 36: //Vibration
					case 37: //Version
					case 55: //Speed G
					case 56: //LowestCellVoltage
					case 57: //LowestCellNumber
					case 58: //Pressure
					case 59: //Event G
					case 83: //Revolution E
					case 84: //MotorTime E
					case 85: //Speed E
					case 86: //Event E
					case 87: //Voltage M,
					case 88: //Current M,
					case 89: //Capacity M,
					case 90: //Power M,
					case 91: //Revolution M
					case 92: //TemperatureM 1
					case 93: //TemperatureM 2
					case 94: //Voltage_min
					case 95: //Current_max
					case 96: //Revolution_max
					case 97: //Temperature1_max
					case 98: //Temperature2_max
					case 99: //Event M
						sb.append(String.format("added measurement set to isCalculation=true -> %s\n", recordKeys[i]));
						recordSet.get(i).setActive(null);
						break;
					default:
						HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[j], Record.DELIMITER, Record.propertyKeys);
						sb.append(String.format("%19s match %19s isAvtive = %s\n", recordKeys[i], recordProps.get(Record.NAME), recordProps.get(Record.IS_ACTIVE)));
						cleanedRecordNames.add(recordKeys[i]);
						noneCalculationRecordNames.add(recordProps.get(Record.NAME));
						if (fileRecordsProperties[j].contains("_isActive=false"))
							recordSet.get(i).setActive(false);
						++j;
						break;
					}
				}
				break;
				
			case 74: //Channels (no ESC) prior to 3.0.4 added PowerOff, BatterieLow, Reset, reserve
				for (int i = 0, j = 0; i < recordKeys.length; i++) {
					switch (i) { //list of added measurements
					case 8:  //8=VoltageRx_min
					case 9:  //EventRx
					case 14: //EventVario
					case 15: //misc Vario_1
					case 16: //misc Vario_2
					case 17: //misc Vario_3
					case 18: //misc Vario_4
					case 19: //misc Vario_5
					case 26: //NumSatellites
					case 27: //GPS-Fix
					case 28: //EventGPS
					case 29: //HomeDirection
					case 30: //Roll
					case 31: //Pitch
					case 32: //Yaw
					case 33: //GyroX
					case 34: //GyroY
					case 35: //GyroZ
					case 36: //Vibration
					case 37: //Version
					case 55: //Speed G
					case 56: //LowestCellVoltage
					case 57: //LowestCellNumber
					case 58: //Pressure
					case 59: //Event G
					case 83: //Revolution E
					case 84: //MotorTime E
					case 85: //Speed E
					case 86: //Event E
					case 103: //PowerOff
					case 104: //BatterieLow
					case 105: //Reset
					case 106: //reserve
					case 107: //Voltage M,
					case 108: //Current M,
					case 109: //Capacity M,
					case 110: //Power M,
					case 111: //Revolution M
					case 112: //TemperatureM 1
					case 113: //TemperatureM 2
					case 114: //Voltage_min
					case 115: //Current_max
					case 116: //Revolution_max
					case 117: //Temperature1_max
					case 118: //Temperature2_max
					case 119: //Event M
						sb.append(String.format("added measurement set to isCalculation=true -> %s\n", recordKeys[i]));
						recordSet.get(i).setActive(null);
						break;
					default:
						HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[j], Record.DELIMITER, Record.propertyKeys);
						sb.append(String.format("%19s match %19s isAvtive = %s\n", recordKeys[i], recordProps.get(Record.NAME), recordProps.get(Record.IS_ACTIVE)));
						cleanedRecordNames.add(recordKeys[i]);
						noneCalculationRecordNames.add(recordProps.get(Record.NAME));
						if (fileRecordsProperties[j].contains("_isActive=false"))
							recordSet.get(i).setActive(false);
						++j;
						break;
					}
				}
				break;

			case 64: //General, GAM, EAM, ESC 3.0.4 added VoltageM, CurrentM, CapacityM, PowerM, RevolutionM, TemperatureM 1
				for (int i = 0, j = 0; i < recordKeys.length; i++) {
					switch (i) { //list of added measurements
					case 8:  //8=VoltageRx_min
					case 9:  //EventRx
					case 14: //EventVario
					case 15: //misc Vario_1
					case 16: //misc Vario_2
					case 17: //misc Vario_3
					case 18: //misc Vario_4
					case 19: //misc Vario_5
					case 26: //NumSatellites
					case 27: //GPS-Fix
					case 28: //EventGPS
					case 29: //HomeDirection
					case 30: //Roll
					case 31: //Pitch
					case 32: //Yaw
					case 33: //GyroX
					case 34: //GyroY
					case 35: //GyroZ
					case 36: //Vibration
					case 37: //Version
					case 55: //Speed G
					case 56: //LowestCellVoltage
					case 57: //LowestCellNumber
					case 58: //Pressure
					case 59: //Event G
					case 83: //Revolution E
					case 84: //MotorTime E
					case 85: //Speed E
					case 86: //Event E
					case 93: //TemperatureM 2
					case 94: //Voltage_min
					case 95: //Current_max
					case 96: //Revolution_max
					case 97: //Temperature1_max
					case 98: //Temperature2_max
					case 99: //Event M
						sb.append(String.format("added measurement set to isCalculation=true -> %s\n", recordKeys[i]));
						recordSet.get(i).setActive(null);
						break;
					default:
						HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[j], Record.DELIMITER, Record.propertyKeys);
						sb.append(String.format("%19s match %19s isAvtive = %s\n", recordKeys[i], recordProps.get(Record.NAME), recordProps.get(Record.IS_ACTIVE)));
						cleanedRecordNames.add(recordKeys[i]);
						noneCalculationRecordNames.add(recordProps.get(Record.NAME));
						if (fileRecordsProperties[j].contains("_isActive=false"))
							recordSet.get(i).setActive(false);
						++j;
						break;
					}
				}
				break;
				
			case 84: //Channels 3.0.4 added VoltageM, CurrentM, CapacityM, PowerM, RevolutionM, TemperatureM 1
				for (int i = 0, j = 0; i < recordKeys.length; i++) {
					switch (i) { //list of added measurements
					case   8: //8=VoltageRx_min
					case   9: //EventRx
					case  14: //EventVario
					case  15: //misc Vario_1
					case  16: //misc Vario_2
					case  17: //misc Vario_3
					case  18: //misc Vario_4
					case  19: //misc Vario_5
					case  26: //NumSatellites
					case  27: //GPS-Fix
					case  28: //EventGPS
					case  29: //HomeDirection
					case  30: //Roll
					case  31: //Pitch
					case  32: //Yaw
					case  33: //GyroX
					case  34: //GyroY
					case  35: //GyroZ
					case  36: //Vibration
					case  37: //Version
					case  55: //Speed G
					case  56: //LowestCellVoltage
					case  57: //LowestCellNumber
					case  58: //Pressure
					case  59: //Event G
					case  83: //Revolution E
					case  84: //MotorTime E
					case  85: //Speed E
					case  86: //Event E
					case 113: //TemperatureM 2
					case 114: //Voltage_min
					case 115: //Current_max
					case 116: //Revolution_max
					case 117: //Temperature1_max
					case 118: //Temperature2_max
					case 119: //Event M
						sb.append(String.format("added measurement set to isCalculation=true -> %s\n", recordKeys[i]));
						recordSet.get(i).setActive(null);
						break;
					default:
						HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[j], Record.DELIMITER, Record.propertyKeys);
						sb.append(String.format("%19s match %19s isAvtive = %s\n", recordKeys[i], recordProps.get(Record.NAME), recordProps.get(Record.IS_ACTIVE)));
						cleanedRecordNames.add(recordKeys[i]);
						noneCalculationRecordNames.add(recordProps.get(Record.NAME));
						if (fileRecordsProperties[j].contains("_isActive=false"))
							recordSet.get(i).setActive(false);
						++j;
						break;
					}
				}
				break;


			case 66: //General, GAM, EAM, ESC 3.1.9 added VoltageRx_min, Revolution EAM
				for (int i = 0, j = 0; i < recordKeys.length; i++) {
					switch (i) { //list of added measurements
					case 9:  //EventRx
					case 14: //EventVario
					case 15: //misc Vario_1
					case 16: //misc Vario_2
					case 17: //misc Vario_3
					case 18: //misc Vario_4
					case 19: //misc Vario_5
					case 26: //NumSatellites
					case 27: //GPS-Fix
					case 28: //EventGPS
					case 29: //HomeDirection
					case 30: //Roll
					case 31: //Pitch
					case 32: //Yaw
					case 33: //GyroX
					case 34: //GyroY
					case 35: //GyroZ
					case 36: //Vibration
					case 37: //Version
					case 55: //Speed G
					case 56: //LowestCellVoltage
					case 57: //LowestCellNumber
					case 58: //Pressure
					case 59: //Event G
					case 84: //MotorTime E
					case 85: //Speed E
					case 86: //Event E
					case 93: //TemperatureM 2
					case 94: //Voltage_min
					case 95: //Current_max
					case 96: //Revolution_max
					case 97: //Temperature1_max
					case 98: //Temperature2_max
					case 99: //Event M
						sb.append(String.format("added measurement set to isCalculation=true -> %s\n", recordKeys[i]));
						recordSet.get(i).setActive(null);
						break;
					default:
						HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[j], Record.DELIMITER, Record.propertyKeys);
						sb.append(String.format("%19s match %19s isAvtive = %s\n", recordKeys[i], recordProps.get(Record.NAME), recordProps.get(Record.IS_ACTIVE)));
						cleanedRecordNames.add(recordKeys[i]);
						noneCalculationRecordNames.add(recordProps.get(Record.NAME));
						if (fileRecordsProperties[j].contains("_isActive=false")) {
							switch (i) { //OSD saved initially after 3.0.4 and after 3.1.9
							case   8: //8=VoltageRx_min
							case  83: //Revolution E
								sb.append(String.format("previous added measurement set to isCalculation=true -> %s\n", recordKeys[i]));
								cleanedRecordNames.remove(recordKeys[i]);
								noneCalculationRecordNames.remove(recordProps.get(Record.NAME));
								fileRecordsPropertiesVector.remove(fileRecordsProperties[j]);
								recordSet.get(i).setActive(null);
								break;
							default:
								recordSet.get(i).setActive(false);
								break;
							}
						}
						++j;
						break;
					}
				}
				break;
				
			case 86:
				if (recordSet.getChannelConfigNumber() == 4) { //Channels 3.1.9 added VoltageRx_min, Revolution EAM
					for (int i = 0, j = 0; i < recordKeys.length; i++) {
						switch (i) { //list of added measurements
						case 9: //EventRx
						case 14: //EventVario
						case 15: //misc Vario_1
						case 16: //misc Vario_2
						case 17: //misc Vario_3
						case 18: //misc Vario_4
						case 19: //misc Vario_5
						case 26: //NumSatellites
						case 27: //GPS-Fix
						case 28: //EventGPS
						case 29: //HomeDirection
						case 30: //Roll
						case 31: //Pitch
						case 32: //Yaw
						case 33: //GyroX
						case 34: //GyroY
						case 35: //GyroZ
						case 36: //Vibration
						case 37: //Version
						case 55: //Speed G
						case 56: //LowestCellVoltage
						case 57: //LowestCellNumber
						case 58: //Pressure
						case 59: //Event G
						case 84: //MotorTime E
						case 85: //Speed E
						case 86: //Event E
						case 113: //TemperatureM 2
						case 114: //Voltage_min
						case 115: //Current_max
						case 116: //Revolution_max
						case 117: //Temperature1_max
						case 118: //Temperature2_max
						case 119: //Event M
							sb.append(String.format("added measurement set to isCalculation=true -> %s\n", recordKeys[i]));
							recordSet.get(i).setActive(null);
							break;
						default:
							HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[j], Record.DELIMITER, Record.propertyKeys);
							sb.append(String.format("%19s match %19s isAvtive = %s\n", recordKeys[i], recordProps.get(Record.NAME), recordProps.get(Record.IS_ACTIVE)));
							cleanedRecordNames.add(recordKeys[i]);
							noneCalculationRecordNames.add(recordProps.get(Record.NAME));
							if (fileRecordsProperties[j].contains("_isActive=false")) {
								switch (i) { //OSD saved initially after 3.0.4 and after 3.1.9
								case   8: //8=VoltageRx_min
								case  83: //Revolution E
									sb.append(String.format("previous added measurement set to isCalculation=true -> %s\n", recordKeys[i]));
									cleanedRecordNames.remove(recordKeys[i]);
									noneCalculationRecordNames.remove(recordProps.get(Record.NAME));
									fileRecordsPropertiesVector.remove(fileRecordsProperties[j]);
									recordSet.get(i).setActive(null);
									break;
								default:
									recordSet.get(i).setActive(false);
									break;
								}
							}
							++j;
							break;
						}
					}
				}
				else { //3.3.1 General, GAM, EAM, ESC
					//3.3.1 extend this measurements: 9=EventRx, 14=EventVario, 21=NumSatellites 22=GPS-Fix 23=EventGPS, 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G, 70=MotorTime 71=Speed 72=Event E, 85/105=Event M
					for (int i = 0, j = 0; i < recordKeys.length; i++) {
						// 3.4.6 extended with 24=HomeDirection 25=Roll 26=Pitch 27=Yaw 28=GyroX 29=GyroY 30=GyroZ 31=Vibration 32=Version	
						// 3.5.0 extend with 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
						switch (i) { //list of added measurements
						case 15: //misc Vario_1
						case 16: //misc Vario_2
						case 17: //misc Vario_3
						case 18: //misc Vario_4
						case 19: //misc Vario_5
						case 29: //HomeDirection
						case 30: //Roll
						case 31: //Pitch
						case 32: //Yaw
						case 33: //GyroX
						case 34: //GyroY
						case 35: //GyroZ
						case 36: //Vibration
						case 37: //Version
							sb.append(String.format("added measurement set to isCalculation=true -> %s\n", recordKeys[i]));
							recordSet.get(i).setActive(null);
							break;
						default:
							HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[j], Record.DELIMITER, Record.propertyKeys);
							sb.append(String.format("%19s match %19s isAvtive = %s\n", recordKeys[i], recordProps.get(Record.NAME), recordProps.get(Record.IS_ACTIVE)));
							cleanedRecordNames.add(recordKeys[i]);
							noneCalculationRecordNames.add(recordProps.get(Record.NAME));
							if (fileRecordsProperties[j].contains("_isActive=false"))
								recordSet.get(i).setActive(false);
							++j;
							break;
						}
					}
				}
				break;

			case 72:	//3.2.7 General, GAM, EAM, ESC
			case 77:	//3.2.7 Lab-Time
				for (int i = 0, j = 0; i < recordKeys.length; i++) {
					//3.3.1 extend this measurements: 9=EventRx, 14=EventVario, 21=NumSatellites 22=GPS-Fix 23=EventGPS, 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G, 70=MotorTime 71=Speed 72=Event E, 85/105=Event M
					switch (i) {
					case 9:  //EventRx
					case 14: //EventVario
					case 15: //misc Vario_1
					case 16: //misc Vario_2
					case 17: //misc Vario_3
					case 18: //misc Vario_4
					case 19: //misc Vario_5
					case 26: //NumSatellites
					case 27: //GPS-Fix
					case 28: //EventGPS
					case 29: //HomeDirection
					case 30: //Roll
					case 31: //Pitch
					case 32: //Yaw
					case 33: //GyroX
					case 34: //GyroY
					case 35: //GyroZ
					case 36: //Vibration
					case 37: //Version
					case 55: //Speed G
					case 56: //LowestCellVoltage
					case 57: //LowestCellNumber
					case 58: //Pressure
					case 59: //Event G
					case 83: //MotorTime E
					case 85: //Speed E
					case 86: //Event E
					case 99: //Event M
						sb.append(String.format("added measurement set to isCalculation=true -> %s\n", recordKeys[i]));
						recordSet.get(i).setActive(null);
						break;
					default:
						HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[j], Record.DELIMITER, Record.propertyKeys);
						sb.append(String.format("%19s match %19s isAvtive = %s\n", recordKeys[i], recordProps.get(Record.NAME), recordProps.get(Record.IS_ACTIVE)));
						cleanedRecordNames.add(recordKeys[i]);
						noneCalculationRecordNames.add(recordProps.get(Record.NAME));
						if (fileRecordsProperties[j].contains("_isActive=false"))
							recordSet.get(i).setActive(false);
						++j;
						break;
					}
				}
				break;
				
			case 92:	//3.2.7 Channels
				for (int i = 0, j = 0; i < recordKeys.length; i++) {
					//3.3.1 extend this measurements: 9=EventRx, 14=EventVario, 21=NumSatellites 22=GPS-Fix 23=EventGPS, 41=Speed G, 42=LowestCellVoltage, 43=LowestCellNumber, 44=Pressure, 45=Event G, 70=MotorTime 71=Speed 72=Event E, 85/105=Event M
					switch (i) {
					case 9:   //EventRx
					case 14:  //EventVario
					case 15:  //misc Vario_1
					case 16:  //misc Vario_2
					case 17:  //misc Vario_3
					case 18:  //misc Vario_4
					case 19:  //misc Vario_5
					case 26:  //NumSatellites
					case 27:  //GPS-Fix
					case 28:  //EventGPS
					case 29:  //HomeDirection
					case 30:  //Roll
					case 31:  //Pitch
					case 32:  //Yaw
					case 33:  //GyroX
					case 34:  //GyroY
					case 35:  //GyroZ
					case 36:  //Vibration
					case 37:  //Version
					case 55:  //Speed G
					case 56:  //LowestCellVoltage
					case 57:  //LowestCellNumber
					case 58:  //Pressure
					case 59:  //Event G
					case 84:  //MotorTime E
					case 85:  //Speed E
					case 86:  //Event E
					case 119: //Event M
						sb.append(String.format("added measurement set to isCalculation=true -> %s\n", recordKeys[i]));
						recordSet.get(i).setActive(null);
						break;
					default:
						HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[j], Record.DELIMITER, Record.propertyKeys);
						sb.append(String.format("%19s match %19s isAvtive = %s\n", recordKeys[i], recordProps.get(Record.NAME), recordProps.get(Record.IS_ACTIVE)));
						cleanedRecordNames.add(recordKeys[i]);
						noneCalculationRecordNames.add(recordProps.get(Record.NAME));
						if (fileRecordsProperties[j].contains("_isActive=false"))
							recordSet.get(i).setActive(false);
						++j;
						break;
					}
				}
				break;

			case 106:	//3.3.1 - 3.4.6 Channels
			case 91:	//3.3.1 - 3.4.6 Lab-Time
				for (int i = 0, j = 0; i < recordKeys.length; i++) {
					// 3.4.6 extended with 24=HomeDirection 25=Roll 26=Pitch 27=Yaw 28=GyroX 29=GyroY 30=GyroZ 31=Vibration 32=Version	
					switch (i) { //list of added measurements
					case 15: //misc Vario_1
					case 16: //misc Vario_2
					case 17: //misc Vario_3
					case 18: //misc Vario_4
					case 19: //misc Vario_5
					case 29: //HomeDirection
					case 30: //Roll
					case 31: //Pitch
					case 32: //Yaw
					case 33: //GyroX
					case 34: //GyroY
					case 35: //GyroZ
					case 36: //Vibration
					case 37: //Version
						sb.append(String.format("added measurement set to isCalculation=true -> %s\n", recordKeys[i]));
						recordSet.get(i).setActive(null);
						break;
					default:
						HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[j], Record.DELIMITER, Record.propertyKeys);
						sb.append(String.format("%19s match %19s isAvtive = %s\n", recordKeys[i], recordProps.get(Record.NAME), recordProps.get(Record.IS_ACTIVE)));
						cleanedRecordNames.add(recordKeys[i]);
						noneCalculationRecordNames.add(recordProps.get(Record.NAME));
						if (fileRecordsProperties[j].contains("_isActive=false"))
							recordSet.get(i).setActive(false);
						++j;
						break;
					}
				}
				break;
				
			case 95:	//3.4.6 no channels
			case 115:	//3.4.6 with channels
				for (int i = 0, j = 0; i < recordKeys.length; i++) {
					// 3.5.0 extend with 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
					switch (i) { //list of added measurements
					case 15: //misc Vario_1
					case 16: //misc Vario_2
					case 17: //misc Vario_3
					case 18: //misc Vario_4
					case 19: //misc Vario_5
						sb.append(String.format("added measurement set to isCalculation=true -> %s\n", recordKeys[i]));
						recordSet.get(i).setActive(null);
						break;
					default:
						HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[j], Record.DELIMITER, Record.propertyKeys);
						sb.append(String.format("%19s match %19s isAvtive = %s\n", recordKeys[i], recordProps.get(Record.NAME), recordProps.get(Record.IS_ACTIVE)));
						cleanedRecordNames.add(recordKeys[i]);
						noneCalculationRecordNames.add(recordProps.get(Record.NAME));
						if (fileRecordsProperties[j].contains("_isActive=false"))
							recordSet.get(i).setActive(false);
						++j;
						break;
					}
				}
				break;
				
			case 100:	//3.4.6 no channels
			case 120:	//3.4.6 with channels
			default:
				cleanedRecordNames.addAll(Arrays.asList(recordKeys));
				for (int i = 0; i < fileRecordsProperties.length; i++) {
					HashMap<String, String> recordProps = StringHelper.splitString(fileRecordsProperties[i], Record.DELIMITER, Record.propertyKeys);
					if (fileRecordsProperties[i].contains("_isActive=true")) {
						noneCalculationRecordNames.add(recordProps.get(Record.NAME));
					}
					else if (fileRecordsProperties[i].contains("_isActive=false")) {
						recordSet.get(i).setActive(false);
						noneCalculationRecordNames.add(recordProps.get(Record.NAME));
					}
					else {
						recordSet.get(i).setActive(null);
					}
				}
				break;
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, String.format("recordKey to fileRecordsProperties mismatch, check:\n %s \nfileRecordsProperties.length = %d recordKeys.length = %d %s", e.getMessage(), fileRecordsProperties.length, recordKeys.length, sb.toString()));
		}

		recordKeys = cleanedRecordNames.toArray(new String[1]);
		//incoming filePropertiesRecordNames may mismatch recordKeyNames, but addNoneCalculation will use original incoming name
		recordSet.setNoneCalculationRecordNames(noneCalculationRecordNames.toArray(new String[1]));

		if (fileRecordsProperties.length != fileRecordsPropertiesVector.size()) { //if stored again with newer version and isActive handled null as false
			for (int i = 0; i < fileRecordsPropertiesVector.size(); i++) {
				fileRecordsProperties[i] = fileRecordsPropertiesVector.get(i);
			}
			//fileRecordsProperties = fileRecordsPropertiesVector.toArray(new String[1]); //can't be used since it will no be propagated
		}

		if ((recordKeys.length < 100 || (recordKeys.length < 120 && recordSet.getChannelConfigNumber() == 4)) && noneCalculationRecordNames.size() < fileRecordsPropertiesVector.size()) {
			sb.append(String.format("recordKeys.length = %d\n", recordKeys.length));
			sb.append(String.format("noneCalculationRecords.length = %d\n", noneCalculationRecordNames.size()));
			sb.append(String.format("fileRecordsProperties.length = %d\n", fileRecordsProperties.length));
			sb.append(String.format("recordSet.getNoneCalculationMeasurementNames.length = %d\n", this.getNoneCalculationMeasurementNames(recordSet.getChannelConfigNumber(), recordSet.getRecordNames()).length));
			sb.append(String.format("recordSet.getRecordNames.length = %d\n", recordSet.getRecordNames().length));
			log.log(Level.SEVERE, sb.toString());
			if (GDE.isWithUi()) this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGE2402));
		}
		return recordKeys;
	}

	//IHistoDevice functions

	/**
	 * @return true if the device supports a native file import for histo purposes
	 */
	@Override
	public boolean isHistoImportSupported() {
		return this.getClass().equals(HoTTAdapter2.class) && !this.getClass().equals(HoTTAdapter2M.class);
	}

	/**
	 * create recordSet and add record data size points from binary file to each measurement.
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest.
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data.
	 * reduces memory and cpu load by taking measurement samples every x ms based on device setting |histoSamplingTime| .
	 * @param inputStream for loading the log data
	 * @param truss references the requested vault for feeding with the results (vault might be without measurements, settlements and scores)
	 */
	@Override
	public void getRecordSetFromImportFile(Supplier<InputStream> inputStream, VaultCollector truss, Analyzer analyzer) throws DataInconsitsentException,
			IOException, DataTypeException {
		String fileEnding = PathUtils.getFileExtention(truss.getVault().getLoadFilePath());
		if (GDE.FILE_ENDING_DOT_BIN.equals(fileEnding)) {
			new HoTTbinHistoReader2(new PickerParameters(analyzer)).read(inputStream, truss);
		} else if (GDE.FILE_ENDING_DOT_LOG.equals(fileEnding)) {
			// todo implement HoTTlogHistoReader
		} else {
			throw new UnsupportedOperationException(truss.getVault().getLoadFilePath());
		}
	}
	
	/**
	 * get the measurement ordinal of altitude, speed and trip length
	 * @return empty integer array if device does not fulfill complete requirement
	 */
	@Override
	public int[] getAtlitudeTripSpeedOrdinals() { 
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		RecordSet activeRecordSet = this.application.getActiveChannel().getActiveRecordSet();
		if (activeRecordSet.get(10).hasReasonableData() && activeRecordSet.get(20).hasReasonableData() && activeRecordSet.get(21).hasReasonableData())
			return new int[] { 10, 20, 21 };
		else
			return new int[0];
	}  

	/**
	 * check and adapt stored measurement specialties properties against actual record set records which gets created by device properties XML
	 * - like GPS type dependent properties
	 * @param fileRecordsProperties - all the record describing properties stored in the file
	 * @param recordSet - the record sets with its measurements build up with its measurements from device properties XML
	 */
	@Override
	public void applyMeasurementSpecialties(String[] fileRecordsProperties, RecordSet recordSet) {
		
		//3.5.0 extend this.measurements: 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5		
		for (int i = 15; i < 19; ++i) {
			Record record = recordSet.get(i);
			if (record != null && !record.getName().startsWith("vari")) {//misc will be replaced with variable in OSD
				if (fileRecordsProperties[i].contains("factor_DOUBLE=")) {
					int startIndex = fileRecordsProperties[i].indexOf("factor_DOUBLE=") + "factor_DOUBLE=".length();
					int endIndex = fileRecordsProperties[i].indexOf(Record.DELIMITER, startIndex);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " set - factor_DOUBLE " + fileRecordsProperties[i].substring(startIndex, endIndex));
					record.setFactor(Double.parseDouble(fileRecordsProperties[i].substring(startIndex, endIndex)));	
				}
				if (fileRecordsProperties[i].contains("scale_sync_ref_ordinal_INTEGER=")) {
					int startIndex = fileRecordsProperties[i].indexOf("scale_sync_ref_ordinal_INTEGER=") + "scale_sync_ref_ordinal_INTEGER=".length();
					int endIndex = fileRecordsProperties[i].indexOf(Record.DELIMITER, startIndex);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " set - scale_sync_ref_ordinal_INTEGER " + fileRecordsProperties[i].substring(startIndex, endIndex));
					record.createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, Integer.parseInt(fileRecordsProperties[i].substring(startIndex, endIndex))); //$NON-NLS-1$
				}
			}
		}
		
		//3.4.6 extend this.measurements: 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version
		for (int i = 33; i < 36; ++i) {
			Record record = recordSet.get(i);
			if (record != null && !record.getName().startsWith("vari")) {//misc will be replaced with variable in OSD
				if (fileRecordsProperties[i].contains("factor_DOUBLE=")) {
					int startIndex = fileRecordsProperties[i].indexOf("factor_DOUBLE=") + "factor_DOUBLE=".length();
					int endIndex = fileRecordsProperties[i].indexOf(Record.DELIMITER, startIndex);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " set - factor_DOUBLE " + fileRecordsProperties[i].substring(startIndex, endIndex));
					record.setFactor(Double.parseDouble(fileRecordsProperties[i].substring(startIndex, endIndex)));	
				}
				if (fileRecordsProperties[i].contains("scale_sync_ref_ordinal_INTEGER=")) {
					int startIndex = fileRecordsProperties[i].indexOf("scale_sync_ref_ordinal_INTEGER=") + "scale_sync_ref_ordinal_INTEGER=".length();
					int endIndex = fileRecordsProperties[i].indexOf(Record.DELIMITER, startIndex);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " set - scale_sync_ref_ordinal_INTEGER " + fileRecordsProperties[i].substring(startIndex, endIndex));
					record.createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, Integer.parseInt(fileRecordsProperties[i].substring(startIndex, endIndex))); //$NON-NLS-1$
				}
			}
		}
		return;
	}
	
	/**
	 * update the record set Vario dependent record meta data
	 * @param version detected in byte buffer
	 * @param device HoTTAdapter
	 * @param tmpRecordSet the record set to be updated
	 */
	protected static void updateVarioTypeDependent(int version, IDevice device, RecordSet tmpRecordSet) {
		if (version > 100 && version < 120) { //SM MicroVario
			//15=accX 16=accY 17=accZ 18=reserved 19=version
			tmpRecordSet.get(15).setName(device.getMeasurementReplacement("acceleration") + " X Vario");
			tmpRecordSet.get(15).setUnit("g");
			tmpRecordSet.get(16).setName(device.getMeasurementReplacement("acceleration") + " Y Vario");
			tmpRecordSet.get(16).setUnit("g");
			tmpRecordSet.get(16).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 15); //$NON-NLS-1$
			tmpRecordSet.get(17).setName(device.getMeasurementReplacement("acceleration") + " Z Vario");
			tmpRecordSet.get(17).setUnit("g");
			tmpRecordSet.get(17).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 15); //$NON-NLS-1$
			tmpRecordSet.get(18).setName(device.getMeasurementReplacement("air_speed") + " Vario");
			tmpRecordSet.get(18).setUnit("km/h");
			tmpRecordSet.get(18).setFactor(2.0);
			tmpRecordSet.get(19).setName("Version Vario");
			tmpRecordSet.get(19).setUnit("#");
		}
	}

	/**
	 * update the record set GPS dependent record meta data
	 * @param version detected in byte buffer
	 * @param device HoTTAdapter2
	 * @param tmpRecordSet the record set to be updated
	 * @param numberLogEntriesProcessed the number of already processed log entries to correct time synchronization
	 */
	protected static void updateGpsTypeDependent(int version, IDevice device, RecordSet tmpRecordSet, int numberLogEntriesProcessed) {
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin 9=EventRx
		// 10=Altitude, 11=Climb 1, 12=Climb 3, 13=Climb 10 14=EventVario 15=misc Vario_1 16=misc Vario_2 17=misc Vario_3 18=misc Vario_4 19=misc Vario_5
		// 20=Latitude, 21=Longitude, 22=Velocity, 23=Distance, 24=Direction, 25=TripDistance 26=NumSatellites 27=GPS-Fix 28=EventGPS
		// 29=HomeDirection 30=Roll 31=Pitch 32=Yaw 33=GyroX 34=GyroY 35=GyroZ 36=Vibration 37=Version	
		// 38=Voltage G, 39=Current G, 40=Capacity G, 41=Power G, 42=Balance G, 43=CellVoltage G1, 44=CellVoltage G2 .... 48=CellVoltage G6,
		// 49=Revolution G, 50=FuelLevel, 51=Voltage G1, 52=Voltage G2, 53=Temperature G1, 54=Temperature G2 55=Speed G, 56=LowestCellVoltage,
		// 57=LowestCellNumber, 58=Pressure, 59=Event G
		// 60=Voltage E, 61=Current E, 62=Capacity E, 63=Power E, 64=Balance E, 65=CellVoltage E1, 66=CellVoltage E2 .... 78=CellVoltage E14,
		// 79=Voltage E1, 80=Voltage E2, 81=Temperature E1, 82=Temperature E2 83=Revolution E 84=MotorTime 85=Speed 86=Event E
		// 87=VoltageM, 88=CurrentM, 89=CapacityM, 90=PowerM, 91=RevolutionM, 92=TemperatureM 1, 93=TemperatureM 2 94=Voltage_min, 95=Current_max,
		// 96=Revolution_max, 97=Temperature1_max, 98=Temperature2_max 99=Event M

		// 87=Ch 1, 88=Ch 2, 89=Ch 3 .. 102=Ch 16, 103=PowerOff, 104=BatterieLow, 105=Reset, 106=reserve
		// 107=VoltageM, 108=CurrentM, 109=CapacityM, 110=PowerM, 111=RevolutionM, 112=TemperatureM 1, 113=TemperatureM 2 114=Voltage_min, 115=Current_max,
		// 116=Revolution_max, 117=Temperature1_max, 118=Temperature2_max 119=Event M
		if (version > 100) { //SM GPS-Logger
			// 29=HomeDirection 30=ServoPulse 31=AirSpeed 32=n/a 33=GyroX 34=GyroY 35=GyroZ 36=ENL 37=Version	
			tmpRecordSet.get(30).setName(device.getMeasurementReplacement("servo_impulse") + " GPS");
			tmpRecordSet.get(30).setUnit("");
			tmpRecordSet.get(31).setName(device.getMeasurementReplacement("air_speed") + " GPS");
			tmpRecordSet.get(31).setUnit("km/h");
			tmpRecordSet.get(31).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 22); //$NON-NLS-1$
			tmpRecordSet.get(33).setName(device.getMeasurementReplacement("acceleration") + " X GPS");
			tmpRecordSet.get(33).setUnit("g");
			tmpRecordSet.get(33).setFactor(0.01);
			tmpRecordSet.get(34).setName(device.getMeasurementReplacement("acceleration") + " Y GPS");
			tmpRecordSet.get(34).setUnit("g");
			tmpRecordSet.get(34).setFactor(0.01);
			tmpRecordSet.get(34).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 33); //$NON-NLS-1$
			tmpRecordSet.get(35).setName(device.getMeasurementReplacement("acceleration") + " Z GPS");
			tmpRecordSet.get(35).setUnit("g");
			tmpRecordSet.get(35).setFactor(0.01);
			tmpRecordSet.get(35).createProperty(IDevice.SYNC_ORDINAL, DataTypes.INTEGER, 33); //$NON-NLS-1$
			tmpRecordSet.get(36).setName("ENL");
			tmpRecordSet.get(36).setUnit("");
		}
		else if (version == 4) { //RC Electronics Sparrow
			tmpRecordSet.get(30).setName(device.getMeasurementReplacement("servo_impulse") + " GPS");
			tmpRecordSet.get(30).setUnit("%");
			tmpRecordSet.get(32).setName(device.getMeasurementReplacement("voltage") + " GPS");
			tmpRecordSet.get(32).setUnit("V");
			tmpRecordSet.get(33).setName(device.getMeasurementReplacement("time") + " GPS");
			tmpRecordSet.get(33).setUnit("HH:mm:ss.SSS");
			tmpRecordSet.get(33).setFactor(1.0);
			tmpRecordSet.get(34).setName(device.getMeasurementReplacement("date") + " GPS");
			tmpRecordSet.get(34).setUnit("yy-MM-dd");
			tmpRecordSet.get(34).setFactor(1.0);
			tmpRecordSet.get(35).setName(device.getMeasurementReplacement("altitude") + " MSL");
			tmpRecordSet.get(35).setUnit("m");
			tmpRecordSet.get(35).setFactor(1.0);
			tmpRecordSet.get(36).setName("ENL");
			tmpRecordSet.get(36).setUnit("%");
			if (numberLogEntriesProcessed >= 0)
				tmpRecordSet.setStartTimeStamp(HoTTbinReader.getStartTimeStamp(tmpRecordSet.getStartTimeStamp(), tmpRecordSet.get(28).lastElement(), numberLogEntriesProcessed));
		}
		else if (version == 1) { //Graupner GPS #1= 33602/S8437,
			tmpRecordSet.get(30).setName("velNorth");
			tmpRecordSet.get(30).setUnit("mm/s");
			tmpRecordSet.get(32).setName("speedAcc");
			tmpRecordSet.get(32).setUnit("cm/s");
			tmpRecordSet.get(33).setName(device.getMeasurementReplacement("time") + " GPS");
			tmpRecordSet.get(33).setUnit("HH:mm:ss.SSS");
			tmpRecordSet.get(33).setFactor(1.0);
//		tmpRecordSet.get(34).setName("GPS ss.SSS");
//		tmpRecordSet.get(34).setUnit("ss.SSS");
//		tmpRecordSet.get(34).setFactor(1.0);
			tmpRecordSet.get(35).setName("velEast");
			tmpRecordSet.get(35).setUnit("mm/s");
			tmpRecordSet.get(35).setFactor(1.0);
			tmpRecordSet.get(36).setName("HDOP");
			tmpRecordSet.get(36).setUnit("dm");
			if (numberLogEntriesProcessed >= 0)
				tmpRecordSet.setStartTimeStamp(HoTTbinReader.getStartTimeStamp(tmpRecordSet.getStartTimeStamp(), tmpRecordSet.get(28).lastElement(), numberLogEntriesProcessed-1));
		}
		else if (version == 0) { //Graupner GPS #0=GPS #33600
			tmpRecordSet.get(33).setName(device.getMeasurementReplacement("time") + " GPS");
			tmpRecordSet.get(33).setUnit("HH:mm:ss.SSS");
			tmpRecordSet.get(33).setFactor(1.0);
//		tmpRecordSet.get(34).setName("GPS ss.SSS");
//		tmpRecordSet.get(34).setUnit("ss.SSS");
//		tmpRecordSet.get(34).setFactor(1.0);
			tmpRecordSet.get(35).setName(device.getMeasurementReplacement("altitude") + " MSL");
			tmpRecordSet.get(35).setUnit("m");
			tmpRecordSet.get(35).setFactor(1.0);
			if (numberLogEntriesProcessed >= 0)
				tmpRecordSet.setStartTimeStamp(HoTTbinReader.getStartTimeStamp(tmpRecordSet.getStartTimeStamp(), tmpRecordSet.get(28).lastElement(), numberLogEntriesProcessed-1));
		}
		else {
			tmpRecordSet.get(30).setName("Byte GPS_1");
			tmpRecordSet.get(31).setName("Byte GPS_2");
			tmpRecordSet.get(32).setName("Byte GPS_3");

			tmpRecordSet.get(33).setName("Short GPS_4");
			tmpRecordSet.get(34).setName("Short GPS_5");
			tmpRecordSet.get(35).setName("Short GPS_6");

			tmpRecordSet.get(36).setName("Byte GPS_7");
		}

	}
}
