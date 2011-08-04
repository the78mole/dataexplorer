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
    
    Copyright (c) 2011 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.exception.NotSupportedFileFormatException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Class to read Graupner HoTT binary data as saved on SD-Cards 
 * @author Winfried Brügmann
 */
public class HoTTbinReader {
	static Logger							log					= Logger.getLogger(HoTTbinReader.class.getName());

	static StringBuffer				sb;

	final static DataExplorer	application	= DataExplorer.getInstance();
	final static Channels			channels		= Channels.getInstance();
	
	/**
	 * get data file header data
	 * @param filePath
	 * @return hash map containing header data as string accessible by public header keys
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 */
	public static HashMap<String, String> getHeader(DataInputStream data_in) throws FileNotFoundException, IOException, NotSupportedFileFormatException {
		byte[] buffer = new byte[16];
		data_in.read(buffer);
		HashMap<String, String> header = new HashMap<String, String>();
		header.put(HoTTAdapter.SENSOR_TYPE, GDE.STRING_EMPTY + buffer[0]);
		header.put(HoTTAdapter.LOG_COUNT, GDE.STRING_EMPTY + (((buffer[4] & 0xff) << 24) | ((buffer[3] & 0xff) << 16) | ((buffer[2] & 0xff) << 8) | (buffer[1] & 0xff)));
		return header;
	}

	static int convertRFRXSQ2Strenght(int inValue) {
		// RF_RXSQ_to_Strength(72-ShortInt(buf[0].data_3_1[3]) DIV 2)
		int result = 0;
		inValue = 72 - inValue;
	  if (inValue < 31) result = 100;
	  else if (inValue >= 31 && inValue <= 64) result = ((int)((inValue * (-0.5) + 117.25) / 5) * 5);
	  else if (inValue >= 65 && inValue <= 76) result = ((int)((inValue * (-5.0) + 410.00) / 5) * 5);
	  else if (inValue >= 77 && inValue <= 89) result = ((int)((inValue * (-2.5) + 223.75) / 5) * 5);
	  else result = 0;
	  return result * 500;
	}

	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws Exception 
	 */
	public static synchronized void read(String filePath) throws Exception {
		long startTime = new Date().getTime();
		File file = new File(filePath);
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in    = new DataInputStream(file_input);
		long fileSize = file.length();
		IDevice device = HoTTbinReader.application.getActiveDevice();
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = device.getRecordSetStemName() +  GDE.STRING_BLANK_LEFT_BRACKET 
				+ filePath.substring(filePath.lastIndexOf(GDE.FILE_SEPARATOR_UNIX)+1, filePath.lastIndexOf(GDE.STRING_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
		Channel channel = null;
		RecordSet 
			recordSetReceiver = null, 	//0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 
			recordSetGeneral = null, 		//0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel 17=OilLevel, 18=Voltage 1, 19=Voltage 2, 20=Temperature 1, 21=Temperature 2
			recordSetElectric = null, 	//0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2 
			recordSetVario = null, 			//0=RXSQ, 1=Height, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
			recordSetGPS = null; 				//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		int[] 
		    pointsReceiver = new int[8],
		    pointsGeneral = new int[22],
		    pointsElectric = new int[27],
		    pointsVario = new int[7],
		    pointsGPS = new int[12];
		long timeStep_ms = 0, timeOffsetReceiver_ms = 0, timeOffsetVario_ms = 0, timeOffsetGPS_ms = 0, timeOffsetGeneral_ms = 0, timeOffsetElectric_ms = 0;
		String[] lastLoadedSensorType = new String[2];
		int countPackageLoss = 0;
		int  dataBlockSize = 64;
		long numberDatablocks = fileSize/dataBlockSize;
		byte[] buf = new byte[dataBlockSize];
		byte[] buf0 = new byte[42], buf1 = null, buf2 = null, buf3 = null, buf4 = null;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		if (application.getStatusBar() != null) application.setProgress(0, sThreadId);
		
		try {
			//read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(buf);
				if (i==0) log.log(Level.FINE, StringHelper.byte2Hex2CharString(new byte[] {buf[7]}) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(buf[7], false));
				
				if (buf[33] >= 0 && buf[33] <= 4 && buf[3] != 0 && buf[4] != 0) { //skip empty block - package loss
					//create and fill sensor specific data record sets 
					switch ((byte)(buf[7] & 0xFF)) {
					case HoTTAdapter.SENSOR_TYPE_RECEIVER: //receiver data only
						//check if recordSetReceiver initialized, transmitter and receiver data always present, but not in the same data rate and signals
						if (recordSetReceiver == null) {
							channel = channels.get(1);
							recordSetName = (channel.size() + 1) + recordSetNameExtend; //$NON-NLS-1$
							recordSetReceiver = RecordSet.createRecordSet(recordSetName, device, channel.getNumber(), true, true);
							channel.put(recordSetName, recordSetReceiver);
							if (application.getMenuToolBar() != null) {
								channel.applyTemplate(recordSetName, true);
							}
							if (lastLoadedSensorType[0] == null || lastLoadedSensorType[1] == null) {
								lastLoadedSensorType[0] = device.getChannelName(2);
								lastLoadedSensorType[1] = recordSetName;
							}
						}
						//recordSetReceiver initialized and ready to add data
						//0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 
						if (buf[33] == 0 && parse2Short(buf, 40) != 0) {
							if (timeOffsetReceiver_ms == 0) timeOffsetReceiver_ms = timeStep_ms;
							pointsReceiver[0] = (buf[34] & 0xFF) * 1000;
							pointsReceiver[1] = (buf[38] & 0xFF) * 1000;
							pointsReceiver[2] = (buf[34] & 0xFF) * 1000;
							pointsReceiver[3] = parse2Short(buf, 40) * 1000;
							//pointsReceiver[3] = (pointsReceiver[3] > 2000 ? 2000 : pointsReceiver[3]) * 1000;
							pointsReceiver[4] = (buf[3] & 0xFF) * 1000;
							pointsReceiver[5] = (buf[4] & 0xFF) * 1000;
							pointsReceiver[6] = (buf[35] & 0xFF) * 1000;
							pointsReceiver[7] = (buf[36] & 0xFF) * 1000;
	
							recordSetReceiver.addPoints(pointsReceiver, timeStep_ms-timeOffsetReceiver_ms);
						}
						if (numberDatablocks > 30000) { //5 minutes
							data_in.skip(dataBlockSize * 50); //take from data points only each half second 
							i += 50;
							timeStep_ms = timeStep_ms += 500;
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_VARIO:
						//check if recordSetVario initialized, transmitter and receiver data always present, but not in the same data rate and signals
						if (recordSetVario == null) {
							channel = channels.get(2);
							recordSetName = (channel.size() + 1) + recordSetNameExtend; //$NON-NLS-1$
							recordSetVario = RecordSet.createRecordSet(recordSetName, device, channel.getNumber(), true, true);
							channel.put(recordSetName, recordSetVario);
							if (application.getMenuToolBar() != null) {
								channel.applyTemplate(recordSetName, true);
							}
							if (lastLoadedSensorType[0] == null || lastLoadedSensorType[1] == null) {
								lastLoadedSensorType[0] = device.getChannelName(3);
								lastLoadedSensorType[1] = recordSetName;
							}
						}
						//recordSetVario initialized and ready to add data
						//fill data block 0 receiver voltage an temperature
						if (buf[33] == 0 && parse2Short(buf, 0) != 0) {		
							buf0 = new byte[2];
							System.arraycopy(buf, 35, buf0, 0, buf0.length);
						}
						//fill data block 1 to 3
						if (buf[33] == 1) {		
							buf1 = new byte[30];
							System.arraycopy(buf, 34, buf1, 0, buf1.length);
						}
						if (buf[33] == 2) {		
							buf2 = new byte[30];
							System.arraycopy(buf, 34, buf2, 0, buf2.length);
						}
						
						if (buf0 != null && buf1 != null && buf2 != null) {
							if (timeOffsetVario_ms == 0) timeOffsetVario_ms = timeStep_ms;
							//0=RXSQ, 1=Height, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
							pointsVario[0] = (buf1[0] & 0xFF) * 1000;
							pointsVario[1] = parse2Short(buf1, 3) * 1000;
							//pointsVario[0]max = parse2Short(buf1, 5) * 1000;
							//pointsVario[0]min = parse2Short(buf1, 7) * 1000;
							pointsVario[2] = parse2Short(buf1[9], buf2[0]) * 1000;
							pointsVario[3] = parse2Short(buf1[1], buf2[2]) * 1000;
							pointsVario[4] = parse2Short(buf1[3], buf2[4]) * 1000;
							pointsVario[5] = (buf0[0] & 0xFF) * 1000;
							pointsVario[6] = (buf0[1] & 0xFF) * 1000;
	
							recordSetVario.addPoints(pointsVario, timeStep_ms-timeOffsetVario_ms);
							buf0 = buf1 = buf2 = null;
							if (numberDatablocks > 30000) { //5 minutes
								data_in.skip(dataBlockSize * 50); //take from data points only each half second 
								i += 50;
								timeStep_ms = timeStep_ms += 500;
							}
						}
					break;
					
					case HoTTAdapter.SENSOR_TYPE_GPS:
						//check if recordSetReceiver initialized, transmitter and receiver data always present, but not in the same data rate ans signals
						if (recordSetGPS == null) {
							channel = channels.get(3);
							recordSetName = (channel.size() + 1) + recordSetNameExtend; //$NON-NLS-1$
							recordSetGPS = RecordSet.createRecordSet(recordSetName, device, channel.getNumber(), true, true);
							channel.put(recordSetName, recordSetGPS);
							if (application.getMenuToolBar() != null) {
								channel.applyTemplate(recordSetName, true);
							}
							if (lastLoadedSensorType[0] == null || lastLoadedSensorType[1] == null) {
								lastLoadedSensorType[0] = device.getChannelName(4);
								lastLoadedSensorType[1] = recordSetName;
							}
						}
						//recordSetGPS initialized and ready to add data
						//fill data block 0 receiver voltage an temperature
						if (buf0 == null && buf[33] == 0 && parse2Short(buf, 0) != 0) {		
							buf0 = new byte[2];
							System.arraycopy(buf, 35, buf0, 0, buf0.length);
						}
						//fill data block 1 to 3
						if (buf1 == null && buf[33] == 1) {		
							buf1 = new byte[30];
							System.arraycopy(buf, 34, buf1, 0, buf1.length);
						}
						if (buf2 == null && buf[33] == 2) {		
							buf2 = new byte[30];
							System.arraycopy(buf, 34, buf2, 0, buf2.length);
						}
						if (buf3 == null && buf[33] == 3) {		
							buf3 = new byte[30];
							System.arraycopy(buf, 34, buf3, 0, buf3.length);
						}
						
						if (buf0 != null && buf1 != null && buf2 != null && buf3 != null) {
							if (timeOffsetGPS_ms == 0) timeOffsetGPS_ms = timeStep_ms;
							//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripLength, 10=VoltageRx, 11=TemperatureRx
							pointsGPS[0] = (buf1[0] & 0xFF) * 1000;										
							pointsGPS[1] = parse2Short(buf1, 7) * 10000 + parse2Short(buf1[9], buf2[0]);			
							pointsGPS[1] = buf1[6] == 1 ? -1 * pointsGPS[1] : pointsGPS[1];
							pointsGPS[2] = parse2Short(buf2, 2) * 10000 + parse2Short(buf2, 4);
							pointsGPS[2] = buf2[1] == 1 ? -1 * pointsGPS[2] : pointsGPS[2];
							pointsGPS[3] = parse2Short(buf2, 8) * 1000;
							pointsGPS[4] = parse2Short(buf3, 0) * 1000;
							pointsGPS[5] = (buf3[2] & 0xFF) * 1000;
							pointsGPS[6] = parse2Short(buf1, 4) * 1000;
							pointsGPS[7] = parse2Short(buf2, 6) * 1000;
							pointsGPS[8] = (buf1[3] & 0xFF) * 1000;
							pointsGPS[9] = 0; 
							pointsGPS[10] = (buf0[0] & 0xFF) * 1000;
							pointsGPS[11] = (buf0[1] & 0xFF) * 1000;
	
							recordSetGPS.addPoints(pointsGPS, timeStep_ms-timeOffsetGPS_ms);
							buf0 = buf1 = buf2 = buf3 = null;
							if (numberDatablocks > 30000) { //5 minutes
								data_in.skip(dataBlockSize * 50); //take from data points only each half second 
								i += 50;
								timeStep_ms = timeStep_ms += 500;
							}
						}
					break;
					
					case HoTTAdapter.SENSOR_TYPE_GENERAL:
						//check if recordSetGeneral initialized, transmitter and receiver data always present, but not in the same data rate and signals
						if (recordSetGeneral == null) {
							channel = channels.get(4);
							recordSetName = (channel.size() + 1) + recordSetNameExtend; //$NON-NLS-1$
							recordSetGeneral = RecordSet.createRecordSet(recordSetName, device, channel.getNumber(), true, true);
							channel.put(recordSetName, recordSetGeneral);
							if (application.getMenuToolBar() != null) {
								channel.applyTemplate(recordSetName, true);
							}
							if (lastLoadedSensorType[0] == null || lastLoadedSensorType[1] == null) {
								lastLoadedSensorType[0] = device.getChannelName(5);
								lastLoadedSensorType[1] = recordSetName;
							}
						}
						//recordSetGeneral initialized and ready to add data
						//fill data block 1 to 4
						if (buf1 == null && buf[33] == 1) {		
							buf1 = new byte[30];
							System.arraycopy(buf, 34, buf1, 0, buf1.length);
						}
						if (buf2 == null && buf[33] == 2) {		
							buf2 = new byte[30];
							System.arraycopy(buf, 34, buf2, 0, buf2.length);
						}
						if (buf3 == null && buf[33] == 3) {		
							buf3 = new byte[30];
							System.arraycopy(buf, 34, buf3, 0, buf3.length);
						}
						if (buf4 == null && buf[33] == 4) {		
							buf4 = new byte[30];
							System.arraycopy(buf, 34, buf4, 0, buf4.length);
						}

						if (buf1 != null && buf2 != null && buf3 != null && buf4 != null) {
							if (timeOffsetGeneral_ms == 0) timeOffsetGeneral_ms = timeStep_ms;
							int maxVotage = Integer.MIN_VALUE;
							int minVotage = Integer.MAX_VALUE;
							//0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel 17=OilLevel, 18=Voltage 1, 19=Voltage 2, 20=Temperature 1, 21=Temperature 2							
							pointsGeneral[0] = (buf1[0] & 0xFF) * 1000;
							pointsGeneral[1] = parse2Short(buf3, 7) * 1000;
							pointsGeneral[2] = parse2Short(buf3, 5) * 1000;
							pointsGeneral[3] = parse2Short(buf3[9], buf4[0]) * 1000;
							pointsGeneral[4] = Double.valueOf(pointsGeneral[1] / 1000.0 * pointsGeneral[2]).intValue(); // power U*I [W];
							pointsGeneral[5] = 0; //5=Balance
							for (int j = 0; j < 6; j++) {
								pointsGeneral[j + 6] = (buf1[3+j] & 0xFF) * 1000;
								if (pointsGeneral[j + 6] > 0) {
									maxVotage = pointsGeneral[j + 6] > maxVotage ? pointsGeneral[j + 6] : maxVotage;
									minVotage = pointsGeneral[j + 6] < minVotage ? pointsGeneral[j + 6] : minVotage;
								}
							}
							//calculate balance on the fly
							pointsGeneral[5] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
							pointsGeneral[12] = parse2Short(buf2, 8) * 1000;				
							pointsGeneral[13] = parse2Short(buf3, 0) * 1000;				
							pointsGeneral[14] = parse2Short(buf3, 2) * 1000;				
							pointsGeneral[15] = (buf3[4] & 0xFF) * 1000;						
							pointsGeneral[16] = parse2Short(buf2, 6) * 1000;			
							pointsGeneral[17] = 0; //TODO Oil level ?????
							pointsGeneral[18] = parse2Short(buf1[9], buf2[0]) * 1000;
							pointsGeneral[19] = parse2Short(buf2[1], buf2[2]) * 1000;
							pointsGeneral[20] = (buf2[3] & 0xFF) * 1000;					
							pointsGeneral[21] = (buf2[4] & 0xFF) * 1000;					
	
							recordSetGeneral.addPoints(pointsGeneral, timeStep_ms-timeOffsetGeneral_ms);
							buf1 = buf2 = buf3 = buf4 = null;
							if (numberDatablocks > 30000) { //5 minutes
								data_in.skip(dataBlockSize * 50); //take from data points only each half second 
								i += 50;
								timeStep_ms = timeStep_ms += 500;
							}
						}
					break;
					
					case HoTTAdapter.SENSOR_TYPE_ELECTRIC:
						//check if recordSetGeneral initialized, transmitter and receiver data always present, but not in the same data rate and signals
						if (recordSetElectric == null) {
							channel = channels.get(5);
							recordSetName = (channel.size() + 1) + recordSetNameExtend; //$NON-NLS-1$
							recordSetElectric = RecordSet.createRecordSet(recordSetName, device, channel.getNumber(), true, true);
							channel.put(recordSetName, recordSetElectric);
							if (application.getMenuToolBar() != null) {
								channel.applyTemplate(recordSetName, true);
							}
							if (lastLoadedSensorType[0] == null || lastLoadedSensorType[1] == null) {
								lastLoadedSensorType[0] = device.getChannelName(5);
								lastLoadedSensorType[1] = recordSetName;
							}
						}
						//recordSetElectric initialized and ready to add data
						//fill data block 1 to 4
						if (buf1 == null && buf[33] == 1) {		
							buf1 = new byte[30];
							System.arraycopy(buf, 34, buf1, 0, buf1.length);
						}
						if (buf2 == null && buf[33] == 2) {		
							buf2 = new byte[30];
							System.arraycopy(buf, 34, buf2, 0, buf2.length);
						}
						if (buf3 == null && buf[33] == 3) {		
							buf3 = new byte[30];
							System.arraycopy(buf, 34, buf3, 0, buf3.length);
						}
						if (buf4 == null && buf[33] == 4) {		
							buf4 = new byte[30];
							System.arraycopy(buf, 34, buf4, 0, buf4.length);
						}
						
						if (buf1 != null && buf2 != null && buf3 != null && buf4 != null) {
//							printByteContent("buf1", buf1);
//							printByteContent("buf2", buf2);
//							printByteContent("buf3", buf3);
//							printByteContent("buf4", buf4);
							if (timeOffsetElectric_ms == 0) timeOffsetElectric_ms = timeStep_ms;
							int maxVotage = Integer.MIN_VALUE;
							int minVotage = Integer.MAX_VALUE;
							//0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2 
							pointsElectric[0] = (buf1[0] & 0xFF) * 1000;
							pointsElectric[1] = parse2Short(buf3, 7) * 1000;
							pointsElectric[2] = parse2Short(buf3, 5) * 1000;
							pointsElectric[3] = parse2Short(buf3[9], buf4[0]) * 1000;
							pointsElectric[4] = Double.valueOf(pointsElectric[1] / 1000.0 * pointsElectric[2]).intValue(); // power U*I [W];
							pointsElectric[5] = 0; //5=Balance
							for (int j = 0; j < 7; j++) {
								pointsElectric[j + 6] = (buf1[3+j] & 0xFF) * 1000;
								if (pointsElectric[j + 6] > 0) {
									maxVotage = pointsElectric[j + 6] > maxVotage ? pointsElectric[j + 6] : maxVotage;
									minVotage = pointsElectric[j + 6] < minVotage ? pointsElectric[j + 6] : minVotage;
								}
							}
							for (int j = 0; j < 7; j++) {
								pointsElectric[j + 13] = (buf2[j] & 0xFF) * 1000;
								if (pointsElectric[j + 13] > 0) {
									maxVotage = pointsElectric[j + 13] > maxVotage ? pointsElectric[j + 13] : maxVotage;
									minVotage = pointsElectric[j + 13] < minVotage ? pointsElectric[j + 13] : minVotage;
								}
							}
							//calculate balance on the fly
							pointsElectric[5] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
							pointsElectric[20] = parse2Short(buf3, 3) * 1000;
							pointsElectric[21] = parse2Short(buf4, 1) * 1000;
							pointsElectric[22] = (buf4[3] & 0xFF) * 1000;
							pointsElectric[23] = parse2Short(buf1[9], buf2[0]) * 1000;
							pointsElectric[24] = parse2Short(buf2[1], buf2[2]) * 1000;
							pointsElectric[25] = (buf2[3] & 0xFF) * 1000;
							pointsElectric[26] = (buf2[4] & 0xFF) * 1000;
	
							recordSetElectric.addPoints(pointsElectric, timeStep_ms-timeOffsetElectric_ms);
							buf1 = buf2 = buf3 = buf4 = null;
							if (numberDatablocks > 30000) { //5 minutes
								data_in.skip(dataBlockSize * 50); //take from data points only each half second 
								i += 50;
								timeStep_ms = timeStep_ms += 500;
							}
						}
					break;
					}
						
					// add default time step from device of 10 msec
					timeStep_ms = timeStep_ms += 10;

					if (application.getStatusBar() != null && i % 100 == 0) application.setProgress((int)(i * 100 / numberDatablocks), sThreadId);
				}
				else { //skip empty block, but add time step
					++countPackageLoss;
					timeStep_ms = timeStep_ms += 10;
				}
			}
			log.log(Level.WARNING, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$

			if (channel != null) {
				RecordSet actualRecordSet = channel.get(recordSetName);
				device.updateVisibilityStatus(actualRecordSet, true);
				String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new File(filePath).lastModified()); //$NON-NLS-1$
				actualRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT	+ Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
				channels.switchChannel(channel.getName());
				channel.switchRecordSet(recordSetName);
				log.log(Level.FINE, "switch to channel " + channel.getName() + GDE.STRING_MESSAGE_CONCAT + recordSetName); //$NON-NLS-1$
			}
			
			MenuToolBar menuToolBar = HoTTbinReader.application.getMenuToolBar();
			if (menuToolBar != null) {
				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
			}

			log.log(Level.FINE, "read time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (application.getStatusBar() != null) application.setProgress(100, sThreadId);
		}
		finally {
			data_in.close ();
			data_in = null;
			file_input = null;
		}
	}

	/**
	 * @param buf[]
	 */
	public static void printByteContent(String name, byte[] buf4) {
		StringBuilder s = new StringBuilder().append(name).append(": ");
		for (int j = 0; j < buf4.length; j++) {
			s.append(String.format("%4d", buf4[j] & 0xff));
		}
		log.log(Level.OFF, s.toString());
	}
	
	/**
	 * parse 4 byte of a data buffer to integer value, length might be 1, 2, 3, 4, 8 bytes
	 * @param buffer
	 */
	static int parse2Int(byte[] buffer, int start) {
		return (((buffer[start+3] & 0xff) << 24) | ((buffer[start+2] & 0xff) << 16) | ((buffer[start+1] & 0xff) << 8) | (buffer[start] & 0xff));
	}
	
	/**
	 * parse 2 byte of a data buffer to integer value, buffer byte sequence low byte high byte
	 * @param buffer
	 * @param start position of low byte 
	 */
	static int parse2Short(byte[] buffer, int start) {
		return (((buffer[start+1] & 0xff) << 8) | (buffer[start] & 0xff));
	}
	
	/**
	 * parse high and low byte to integer value
	 * @param low byte
	 * @param high byte
	 */
	static int parse2Short(byte low, byte high) {
		return (((high & 0xff) << 8) | (low & 0xff));
	}

}
