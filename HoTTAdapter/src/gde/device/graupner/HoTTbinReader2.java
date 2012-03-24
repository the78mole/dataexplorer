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
    
    Copyright (c) 2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.data.Channel;
import gde.data.RecordSet;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Class to read Graupner HoTT binary data as saved on SD-Cards 
 * @author Winfried Brügmann
 */
public class HoTTbinReader2 extends HoTTbinReader {
	final static Logger						logger											= Logger.getLogger(HoTTbinReader2.class.getName());
	static int[]									points;
	static RecordSet							recordSet;

	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws Exception 
	 */
	public static synchronized void read(String filePath) throws Exception {
		HashMap<String, String> header = null;
		File file = new File(filePath);
		log.log(Level.FINER, file.getName() + " - " + new SimpleDateFormat("yyyy-MM-dd").format(file.lastModified()));
		header = getFileInfo(file);

		if (Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) <= 1)
			readSingle(file);
		else
			readMultiple(file);
	}

	/**
	* read log data according to version 0
	* @param file
	* @param data_in
	* @throws IOException 
	* @throws DataInconsitsentException 
	*/
	static void readSingle(File file) throws IOException, DataInconsitsentException {
		final String $METHOD_NAME = "readSingle";
		long startTime = System.nanoTime() / 1000000;
		long actualTime_ms = 0, drawTime_ms = startTime;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		HoTTAdapter2 device = (HoTTAdapter2) HoTTbinReader2.application.getActiveDevice();
		int recordSetNumber = HoTTbinReader2.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);	
		Channel channel = null;
		int channelNumber = device.getLastChannelNumber();
		device.getMeasurementFactor(channelNumber, 12);
		boolean isInitialSwitched = false;
		boolean isReceiverData = false;
		boolean isSensorData = false;
		boolean isSensorDataPart = false;
		HoTTbinReader2.recordSet = null; 
		HoTTAdapter.latitudeTolranceFactor = device.getMeasurementFactor(channelNumber, 12);
		//0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 
		//8=Height, 9=Climb 1, 10=Climb 3, 11=Climb 10
		//12=Latitude, 13=Longitude, 14=Velocity, 15=DistanceStart, 16=DirectionStart, 17=TripDistance
		//18=VoltageGen, 19=CurrentGen, 20=CapacityGen, 21=PowerGen, 22=BalanceGen, 23=CellVoltageGen 1, 24=CellVoltageGen 2 .... 28=CellVoltageGen 6, 29=Revolution, 30=FuelLevel, 31=VoltageGen 1, 32=VoltageGen 2, 33=TemperatureGen 1, 34=TemperatureGen 2
		//35=VoltageGen, 36=CurrentGen, 37=CapacityGen, 38=PowerGen, 39=BalanceGen, 40=CellVoltageGen 1, 41=CellVoltageGen 2 .... 53=CellVoltageGen 14, 54=VoltageGen 1, 55=VoltageGen 2, 56=TemperatureGen 1, 57=TemperatureGen 2 
		HoTTbinReader2.points = new int[58];
		HoTTbinReader2.points[2] = 100000;
		HoTTbinReader2.timeStep_ms = 0;
		HoTTbinReader2.buf = new byte[HoTTbinReader2.dataBlockSize];
		HoTTbinReader2.buf0 = null;
		HoTTbinReader2.buf1 = null;
		HoTTbinReader2.buf2 = null;
		HoTTbinReader2.buf3 = null;
		HoTTbinReader2.buf4 = null;
		int version = -1;
		int countPackageLoss = 0;
		long numberDatablocks = fileSize / HoTTbinReader2.dataBlockSize;
		long startTimeStamp_ms = file.lastModified() - (numberDatablocks * 10);
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		MenuToolBar menuToolBar = HoTTbinReader2.application.getMenuToolBar();
		if (menuToolBar != null) HoTTbinReader2.application.setProgress(0, sThreadId);

		try {
			//check if recordSet initialized, transmitter and receiver data always present, but not in the same data rate and signals
			channel = HoTTbinReader2.channels.get(channelNumber);
			channel.setFileDescription(application.isObjectoriented() ? date + GDE.STRING_BLANK + application.getObjectKey() : date);
			recordSetName = recordSetNumber + device.getRecordSetStemName() + recordSetNameExtend;
			HoTTbinReader2.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true);
			channel.put(recordSetName, HoTTbinReader2.recordSet);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (HoTTbinReader2.application.getMenuToolBar() != null) {
				channel.applyTemplate(recordSetName, true);
			}
			//recordSet initialized and ready to add data

			//read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader2.buf);
				if (HoTTbinReader2.logger.isLoggable(Level.FINER) && i % 10 == 0) {
					HoTTbinReader2.logger.logp(Level.FINE, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReader2.buf.length));
				}
				HoTTbinReader2.logger.logp(Level.FINER, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader2.buf, HoTTbinReader2.buf.length));
				
				//fill receiver data
				if (HoTTbinReader2.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader2.buf, 40) != 0 && HoTTbinReader2.timeStep_ms % 10 == 0) {
					parseReceiver(HoTTbinReader2.recordSet, HoTTbinReader2.points, HoTTbinReader2.buf);
					isReceiverData = true;
				}

				if (HoTTbinReader2.buf[33] >= 0 && HoTTbinReader2.buf[33] <= 4 && HoTTbinReader2.buf[3] != 0 && HoTTbinReader2.buf[4] != 0) { //skip empty block - package loss
					//create and fill sensor specific data record sets 
					if (HoTTbinReader2.logger.isLoggable(Level.FINER))
						HoTTbinReader2.logger.logp(Level.FINER, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader2.buf[7] }, 1)
								+ GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader2.buf[7], false));

					switch ((byte) (HoTTbinReader2.buf[7] & 0xFF)) {
					case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
					case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
						isSensorDataPart = true;
						//fill data block 0 receiver voltage an temperature
						if (HoTTbinReader2.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader2.buf, 0) != 0) {
							HoTTbinReader2.buf0 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf0, 0, HoTTbinReader2.buf0.length);
						}
						//fill data block 1 to 3
						if (HoTTbinReader2.buf[33] == 1) {
							HoTTbinReader2.buf1 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf1, 0, HoTTbinReader2.buf1.length);
						}
						if (HoTTbinReader2.buf[33] == 2) {
							HoTTbinReader2.buf2 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf2, 0, HoTTbinReader2.buf2.length);
						}

						if (HoTTbinReader2.buf0 != null && HoTTbinReader2.buf1 != null && HoTTbinReader2.buf2 != null) {
							version = parseVario(HoTTbinReader2.recordSet, HoTTbinReader2.points, version, HoTTbinReader2.buf0, HoTTbinReader2.buf1, HoTTbinReader2.buf2);
							HoTTbinReader2.buf0 = HoTTbinReader2.buf1 = HoTTbinReader2.buf2 = null;
							isSensorData = true;
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_GPS_115200:
					case HoTTAdapter.SENSOR_TYPE_GPS_19200:
						isSensorDataPart = true;
						//fill data block 0 receiver voltage an temperature
						if (HoTTbinReader2.buf0 == null && HoTTbinReader2.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader2.buf, 0) != 0) {
							HoTTbinReader2.buf0 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf0, 0, HoTTbinReader2.buf0.length);
						}
						//fill data block 1 to 3
						if (HoTTbinReader2.buf1 == null && HoTTbinReader2.buf[33] == 1) {
							HoTTbinReader2.buf1 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf1, 0, HoTTbinReader2.buf1.length);
						}
						if (HoTTbinReader2.buf2 == null && HoTTbinReader2.buf[33] == 2) {
							HoTTbinReader2.buf2 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf2, 0, HoTTbinReader2.buf2.length);
						}
						if (HoTTbinReader2.buf3 == null && HoTTbinReader2.buf[33] == 3) {
							HoTTbinReader2.buf3 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf3, 0, HoTTbinReader2.buf3.length);
						}

						if (HoTTbinReader2.buf0 != null && HoTTbinReader2.buf1 != null && HoTTbinReader2.buf2 != null && HoTTbinReader2.buf3 != null) {
							parseGPS(HoTTbinReader2.recordSet, HoTTbinReader2.points, HoTTbinReader2.buf0, HoTTbinReader2.buf1, HoTTbinReader2.buf2, HoTTbinReader2.buf3);
							HoTTbinReader2.buf0 = HoTTbinReader2.buf1 = HoTTbinReader2.buf2 = HoTTbinReader2.buf3 = null;
							isSensorData = true;				
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
					case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
						isSensorDataPart = true;
						//fill data block 1 to 4
						if (HoTTbinReader2.buf0 == null && HoTTbinReader2.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader2.buf, 0) != 0) {
							HoTTbinReader2.buf0 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf0, 0, HoTTbinReader2.buf0.length);
						}
						if (HoTTbinReader2.buf1 == null && HoTTbinReader2.buf[33] == 1) {
							HoTTbinReader2.buf1 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf1, 0, HoTTbinReader2.buf1.length);
						}
						if (HoTTbinReader2.buf2 == null && HoTTbinReader2.buf[33] == 2) {
							HoTTbinReader2.buf2 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf2, 0, HoTTbinReader2.buf2.length);
						}
						if (HoTTbinReader2.buf3 == null && HoTTbinReader2.buf[33] == 3) {
							HoTTbinReader2.buf3 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf3, 0, HoTTbinReader2.buf3.length);
						}
						if (HoTTbinReader2.buf4 == null && HoTTbinReader2.buf[33] == 4) {
							HoTTbinReader2.buf4 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf4, 0, HoTTbinReader2.buf4.length);
						}

						if (HoTTbinReader2.buf0 != null && HoTTbinReader2.buf1 != null && HoTTbinReader2.buf2 != null && HoTTbinReader2.buf3 != null && HoTTbinReader2.buf4 != null) {
							parseGeneral(HoTTbinReader2.recordSet, HoTTbinReader2.points, HoTTbinReader2.buf0, HoTTbinReader2.buf1, HoTTbinReader2.buf2, HoTTbinReader2.buf3, HoTTbinReader2.buf4);
							HoTTbinReader2.buf1 = HoTTbinReader2.buf2 = HoTTbinReader2.buf3 = HoTTbinReader2.buf4 = null;
							isSensorData = true;
						}
						break;

					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
					case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
						isSensorDataPart = true;
						//fill data block 0 to 4
						if (HoTTbinReader2.buf0 == null && HoTTbinReader2.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader2.buf, 0) != 0) {
							HoTTbinReader2.buf0 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf0, 0, HoTTbinReader2.buf0.length);
						}
						if (HoTTbinReader2.buf1 == null && HoTTbinReader2.buf[33] == 1) {
							HoTTbinReader2.buf1 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf1, 0, HoTTbinReader2.buf1.length);
						}
						if (HoTTbinReader2.buf2 == null && HoTTbinReader2.buf[33] == 2) {
							HoTTbinReader2.buf2 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf2, 0, HoTTbinReader2.buf2.length);
						}
						if (HoTTbinReader2.buf3 == null && HoTTbinReader2.buf[33] == 3) {
							HoTTbinReader2.buf3 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf3, 0, HoTTbinReader2.buf3.length);
						}
						if (HoTTbinReader2.buf4 == null && HoTTbinReader2.buf[33] == 4) {
							HoTTbinReader2.buf4 = new byte[30];
							System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf4, 0, HoTTbinReader2.buf4.length);
						}

						if (HoTTbinReader2.buf0 != null && HoTTbinReader2.buf1 != null && HoTTbinReader2.buf2 != null && HoTTbinReader2.buf3 != null && HoTTbinReader2.buf4 != null) {
							parseElectric(HoTTbinReader2.recordSet, HoTTbinReader2.points, HoTTbinReader2.buf0, HoTTbinReader2.buf1, HoTTbinReader2.buf2, HoTTbinReader2.buf3, HoTTbinReader2.buf4);
							HoTTbinReader2.buf1 = HoTTbinReader2.buf2 = HoTTbinReader2.buf3 = HoTTbinReader2.buf4 = null;
							isSensorData = true;	
						}
						break;
					}

					if (isSensorData || (isReceiverData && !isSensorDataPart)) {
						HoTTbinReader2.recordSet.addPoints(points, timeStep_ms);
						if (isSensorData && numberDatablocks > 30000) { //5 minutes log time
							data_in.skip(HoTTbinReader2.dataBlockSize * 50); //take from data points only each half second 
							i += 50;
							HoTTbinReader2.timeStep_ms = HoTTbinReader2.timeStep_ms += 500;
							isSensorData = isSensorDataPart = isReceiverData = false;
						}
						// add default time step from device of 10 msec
						HoTTbinReader2.timeStep_ms += 10;
					}
					else { // add default time step from device of 10 msec
						HoTTbinReader2.timeStep_ms += 10;
					}

					if (menuToolBar != null && i % 100 == 0) HoTTbinReader2.application.setProgress((int) (i * 100 / numberDatablocks), sThreadId);
				}
				else { //skip empty block, but add time step
					++countPackageLoss;
					HoTTbinReader2.timeStep_ms += 10;
				}

				actualTime_ms = System.nanoTime() / 1000000;
				if (menuToolBar != null && (actualTime_ms - drawTime_ms) > 5000) {
					if (!isInitialSwitched) {
						HoTTbinReader2.channels.switchChannel(channel.getName());
						device.updateVisibilityStatus(channel.getActiveRecordSet(), true);
						channel.switchRecordSet(recordSetName);
						isInitialSwitched = true;
					}
					else
						HoTTbinReader2.application.updateGraphicsWindow();

					drawTime_ms = actualTime_ms;
					HoTTbinReader2.logger.logp(Level.TIME, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (actualTime_ms - startTime))); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			HoTTbinReader2.logger.logp(Level.WARNING, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReader2.logger.logp(Level.TIME, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (actualTime_ms - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (menuToolBar != null) {
				if (!isInitialSwitched) {
					HoTTbinReader.channels.switchChannel(channel.getName());
					channel.switchRecordSet(recordSetName);
				}
				else {
					device.makeInActiveDisplayable(recordSet);
				}
				device.updateVisibilityStatus(recordSet, true);

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();

				HoTTbinReader2.application.updateGraphicsWindow();
				HoTTbinReader2.application.setProgress(100, sThreadId);
			}
		}
		finally {
			data_in.close();
			data_in = null;
		}
	}

	/**
	* read log data according to version 0
	* @param file
	* @param data_in
	* @throws IOException 
	* @throws DataInconsitsentException 
	*/
	static void readMultiple(File file) throws IOException, DataInconsitsentException {
		final String $METHOD_NAME = "readMultiple";
		long startTime = System.nanoTime() / 1000000;
		long actualTime_ms = 0, drawTime_ms = startTime;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		HoTTAdapter device = (HoTTAdapter) HoTTbinReader2.application.getActiveDevice();
		int recordSetNumber = HoTTbinReader2.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);	
		Channel channel = null;
		int channelNumber = device.getLastChannelNumber();
		boolean isReceiverData = false;
		boolean isVarioData = false;
		boolean isGPSData = false;
		boolean isGeneralData = false;
		boolean isElectricData = false;
		boolean isInitialSwitched = false;
		HoTTbinReader2.recordSet = null; 
		HoTTAdapter.latitudeTolranceFactor = device.getMeasurementFactor(channelNumber, 12);
		//0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 
		//8=Height, 9=Climb 1, 10=Climb 3, 11=Climb 10
		//12=Latitude, 13=Longitude, 14=Velocity, 15=DistanceStart, 16=DirectionStart, 17=TripDistance
		//18=VoltageGen, 19=CurrentGen, 20=CapacityGen, 21=PowerGen, 22=BalanceGen, 23=CellVoltageGen 1, 24=CellVoltageGen 2 .... 28=CellVoltageGen 6, 29=Revolution, 30=FuelLevel, 31=VoltageGen 1, 32=VoltageGen 2, 33=TemperatureGen 1, 34=TemperatureGen 2
		//35=VoltageGen, 36=CurrentGen, 37=CapacityGen, 38=PowerGen, 39=BalanceGen, 40=CellVoltageGen 1, 41=CellVoltageGen 2 .... 53=CellVoltageGen 14, 54=VoltageGen 1, 55=VoltageGen 2, 56=TemperatureGen 1, 57=TemperatureGen 2 
		HoTTbinReader2.points = new int[58];
		HoTTbinReader.pointsGeneral = new int[58];
		HoTTbinReader.pointsElectric = new int[58];
		HoTTbinReader.pointsVario = new int[58];
		HoTTbinReader2.pointsVario[2] = 100000;
		HoTTbinReader.pointsGPS = new int[58];
		HoTTbinReader2.timeStep_ms = 0;
		HoTTbinReader2.buf = new byte[HoTTbinReader2.dataBlockSize];
		HoTTbinReader2.buf0 = new byte[30];
		HoTTbinReader2.buf1 = new byte[30];
		HoTTbinReader2.buf2 = new byte[30];
		HoTTbinReader2.buf3 = new byte[30];
		HoTTbinReader2.buf4 = new byte[30];
		byte actualSensor = -1, lastSensor = -1;
		int lastLogCountVario = 0, lastLogCountGPS = 0, lastLogCountGeneral = 0, lastLogCountElectric = 0, logCountVario = 0, logCountGPS = 0, logCountGeneral = 0, logCountElectric = 0;
		int countPackageLoss = 0;
		long numberDatablocks = fileSize / HoTTbinReader2.dataBlockSize;
		long startTimeStamp_ms = file.lastModified() - (numberDatablocks * 10);
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		MenuToolBar menuToolBar = HoTTbinReader2.application.getMenuToolBar();
		if (menuToolBar != null) HoTTbinReader2.application.setProgress(0, sThreadId);

		try {
			//receiver data are always contained
			channel = HoTTbinReader2.channels.get(channelNumber);
			channel.setFileDescription(application.isObjectoriented() ? date + GDE.STRING_BLANK + application.getObjectKey() : date);
			recordSetName = recordSetNumber + device.getRecordSetStemName() + recordSetNameExtend;
			HoTTbinReader2.recordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true);
			channel.put(recordSetName, HoTTbinReader2.recordSet);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (HoTTbinReader2.application.getMenuToolBar() != null) {
				channel.applyTemplate(recordSetName, true);
			}
			//recordSet initialized and ready to add data

			//read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader2.buf);
				if (HoTTbinReader2.logger.isLoggable(Level.FINEST) && i % 10 == 0) {
					HoTTbinReader2.logger.logp(Level.FINEST, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReader2.buf.length));
				}
				HoTTbinReader2.logger.logp(Level.FINEST, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader2.buf, HoTTbinReader2.buf.length));

				if (HoTTbinReader2.buf[33] >= 0 && HoTTbinReader2.buf[33] <= 4 && HoTTbinReader2.buf[3] != 0 && HoTTbinReader2.buf[4] != 0) { //skip empty block - package loss
					//create and fill sensor specific data record sets 
					if (HoTTbinReader2.logger.isLoggable(Level.FINEST))
						HoTTbinReader2.logger.logp(Level.FINEST, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader2.buf[7] }, 1)
								+ GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader2.buf[7], false));

					if (HoTTbinReader2.buf[38] != 0) { //receiver RF_RXSQ
						//fill receiver data
						if (HoTTbinReader2.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader2.buf, 40) != 0 && HoTTbinReader2.timeStep_ms % 10 == 0) {
							parseReceiver(HoTTbinReader2.recordSet, HoTTbinReader2.points, HoTTbinReader2.buf);
							isReceiverData = true;
						}

						if (actualSensor == -1)
							lastSensor = actualSensor = (byte) (HoTTbinReader2.buf[7] & 0xFF);
						else
							actualSensor = (byte) (HoTTbinReader2.buf[7] & 0xFF);

						if (actualSensor != lastSensor) {
							if (logCountVario > 3 || logCountGPS > 4 || logCountGeneral > 5 || logCountElectric > 5) {
								switch (lastSensor) {
								case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
								case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
									++logCountVario;
									if (isVarioData && isReceiverData) {
										migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, HoTTbinReader2.timeStep_ms);
										//System.out.println("isVarioData i = " + i);
										isReceiverData = isVarioData = isGPSData = isGeneralData = isElectricData = false;
									}
									parseVario(HoTTbinReader2.recordSet, HoTTbinReader2.pointsVario, 1, HoTTbinReader2.buf0, HoTTbinReader2.buf1, HoTTbinReader2.buf2);
									isVarioData = true;
									break;

								case HoTTAdapter.SENSOR_TYPE_GPS_115200:
								case HoTTAdapter.SENSOR_TYPE_GPS_19200:
									++logCountGPS;
									if (isGPSData && isReceiverData) {
										migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, HoTTbinReader2.timeStep_ms);
										//System.out.println("isGPSData i = " + i);
										isReceiverData = isVarioData = isGPSData = isGeneralData = isElectricData = false;
									}
									parseGPS(HoTTbinReader2.recordSet, HoTTbinReader2.pointsGPS, HoTTbinReader2.buf0, HoTTbinReader2.buf1, HoTTbinReader2.buf2, HoTTbinReader2.buf3);
									isGPSData = true;
									break;

								case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
								case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
									++logCountGeneral;
									if (isGeneralData && isReceiverData) {
										migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, HoTTbinReader2.timeStep_ms);
										//System.out.println("isGeneralData i = " + i);
										isReceiverData = isVarioData = isGPSData = isGeneralData = isElectricData = false;
									}
									parseGeneral(HoTTbinReader2.recordSet, HoTTbinReader2.pointsGeneral, HoTTbinReader2.buf0, HoTTbinReader2.buf1, HoTTbinReader2.buf2, HoTTbinReader2.buf3, HoTTbinReader2.buf4);
									isGeneralData = true;
									break;

								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
									++logCountElectric;
									if (isElectricData && isReceiverData) {
										migrateAddPoints(isVarioData, isGPSData, isGeneralData, isElectricData, HoTTbinReader2.timeStep_ms);
										//System.out.println("isElectricData i = " + i);
										isReceiverData = isVarioData = isGPSData = isGeneralData = isElectricData = false;
									}
									parseElectric(HoTTbinReader2.recordSet, HoTTbinReader2.pointsElectric, HoTTbinReader2.buf0, HoTTbinReader2.buf1, HoTTbinReader2.buf2, HoTTbinReader2.buf3, HoTTbinReader2.buf4);
									isElectricData = true;
									break;
								}
								
								logger.log(Level.FINE, "isReceiverData " + isReceiverData + " isVarioData " + isVarioData + " isGPSData " + isGPSData + " isGeneralData " + isGeneralData + " isElectricData " + isElectricData);

								//skip last log count - 6 logs for speed up right after sensor switch
								int skipCount = 0;
								switch (actualSensor) {
								case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
								case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
									skipCount = lastLogCountVario - 3;
									logCountVario = skipCount;
									HoTTbinReader2.logger.logp(Level.FINE, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, "lastLogCountVario = " + lastLogCountVario);
									break;
								case HoTTAdapter.SENSOR_TYPE_GPS_115200:
								case HoTTAdapter.SENSOR_TYPE_GPS_19200:
									skipCount = lastLogCountGPS - 4;
									logCountGPS = skipCount;
									HoTTbinReader2.logger.logp(Level.FINE, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, "lastLogCountGPS = " + lastLogCountGPS);
									break;
								case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
								case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
									skipCount = lastLogCountGeneral - 5;
									logCountGeneral = skipCount;
									HoTTbinReader2.logger.logp(Level.FINE, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, "lastLogCountGeneral = " + lastLogCountGeneral);
									break;
								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
									skipCount = lastLogCountElectric - 5;
									logCountElectric = skipCount;
									HoTTbinReader2.logger.logp(Level.FINE, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, "lastLogCountElectric = " + lastLogCountElectric);
									break;
								}
								if (skipCount > 0) {
									data_in.skip(HoTTbinReader2.dataBlockSize * skipCount);
									i += skipCount;
									HoTTbinReader2.timeStep_ms = HoTTbinReader2.timeStep_ms += (skipCount * 10);
								}
							}
							switch (lastSensor) {
							case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
							case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
								lastLogCountVario = logCountVario;
								break;
							case HoTTAdapter.SENSOR_TYPE_GPS_115200:
							case HoTTAdapter.SENSOR_TYPE_GPS_19200:
								lastLogCountGPS = logCountGPS;
								break;
							case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
							case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
								lastLogCountGeneral = logCountGeneral;
								break;
							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
								lastLogCountElectric = logCountElectric;
								break;
							}
							HoTTbinReader2.logger.logp(Level.FINE, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, "logCountVario = " + logCountVario + " logCountGPS = " + logCountGPS + " logCountGeneral = " + logCountGeneral + " logCountElectric = " + logCountElectric);
							lastSensor = actualSensor;
							logCountVario = logCountGPS = logCountGeneral = logCountElectric = 0;
						}
						else {
							switch (lastSensor) {
							case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
							case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
								++logCountVario;
								break;
							case HoTTAdapter.SENSOR_TYPE_GPS_115200:
							case HoTTAdapter.SENSOR_TYPE_GPS_19200:
								++logCountGPS;
								break;
							case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
							case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
								++logCountGeneral;
								break;
							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
								++logCountElectric;
								break;
							}
						}
						
						if (isReceiverData && (logCountVario > 0 || logCountGPS > 0 || logCountGeneral > 0 || logCountElectric > 0)) {
							recordSet.addPoints(points, HoTTbinReader2.timeStep_ms);
							//System.out.println("isReceiverData i = " + i);
							isReceiverData = false;
						}
					}
					//fill data block 0 to 4
					if (HoTTbinReader2.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader2.buf, 0) != 0) {
						System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf0, 0, HoTTbinReader2.buf0.length);
					}
					if (HoTTbinReader2.buf[33] == 1) {
						System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf1, 0, HoTTbinReader2.buf1.length);
					}
					if (HoTTbinReader2.buf[33] == 2) {
						System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf2, 0, HoTTbinReader2.buf2.length);
					}
					if (HoTTbinReader2.buf[33] == 3) {
						System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf3, 0, HoTTbinReader2.buf3.length);
					}
					if (HoTTbinReader2.buf[33] == 4) {
						System.arraycopy(HoTTbinReader2.buf, 34, HoTTbinReader2.buf4, 0, HoTTbinReader2.buf4.length);
					}

					// add default time step from log record of 10 msec
					HoTTbinReader2.timeStep_ms += 10;

					if (menuToolBar != null && i % 100 == 0) HoTTbinReader2.application.setProgress((int) (i * 100 / numberDatablocks), sThreadId);
				}
				else { //skip empty block, but add time step
					++countPackageLoss;
					HoTTbinReader2.timeStep_ms += 10;
				}

				actualTime_ms = System.nanoTime() / 1000000;
				if (menuToolBar != null && (actualTime_ms - drawTime_ms) > 5000) {
					if (!isInitialSwitched) {
						HoTTbinReader2.channels.switchChannel(channel.getName());
						device.updateVisibilityStatus(channel.getActiveRecordSet(), true);
						channel.switchRecordSet(recordSetName);
						isInitialSwitched = true;
					}
					else
						HoTTbinReader2.application.updateGraphicsWindow();

					drawTime_ms = actualTime_ms;
					HoTTbinReader2.logger.logp(Level.TIME, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (actualTime_ms - startTime))); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			HoTTbinReader2.logger.logp(Level.WARNING, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReader2.logger.logp(Level.TIME, HoTTbinReader2.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (actualTime_ms - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (menuToolBar != null) {
				if (!isInitialSwitched) {
					HoTTbinReader.channels.switchChannel(channel.getName());
					channel.switchRecordSet(recordSetName);
				}
				else {
					device.makeInActiveDisplayable(recordSet);
				}
				device.updateVisibilityStatus(recordSet, true);

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();

				HoTTbinReader2.application.updateGraphicsWindow();
				HoTTbinReader2.application.setProgress(100, sThreadId);
			}
		}
		finally {
			data_in.close();
			data_in = null;
		}
	}

	/**
	 * migrate sensor measurement values and add to record set, receiver data are always updated
	 * @param isVarioData
	 * @param isGPSData
	 * @param isGeneralData
	 * @param isElectricData
	 * @param _timeStep_ms
	 * @throws DataInconsitsentException
	 */
	public static void migrateAddPoints(boolean isVarioData, boolean isGPSData, boolean isGeneralData, boolean isElectricData, long _timeStep_ms) throws DataInconsitsentException {
		if (isElectricData) {
			for (int j = 8; j < 11; j++) {
				points[j] = pointsElectric[j];
			}
			for (int k = 35; k < 58; k++) {
				points[k] = pointsElectric[k];
			}
		}
		if (isGeneralData) {
			for (int j = 8; j < 11; j++) {
				points[j] = pointsGeneral[j];
			}
			for (int k = 18; k < 35; k++) {
				points[k] = pointsGeneral[k];
			}
		}
		if (isGPSData) {
			for (int j = 8; j < 11; j++) {
				points[j] = pointsGPS[j];
			}
			for (int k = 12; k < 18; k++) {
				points[k] = pointsGPS[k];
			}
		}
		if (isVarioData) {
			for (int j = 8; j < 12; j++) {
				points[j] = pointsVario[j];
			}
		}
		recordSet.addPoints(points, _timeStep_ms);
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 * @param _recordSet
	 * @param _points
	 * @param _buf
	 */
	private static void parseReceiver(RecordSet _recordSet, int[] _points, byte[] _buf) {
		//0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 
		_points[0] = (_buf[37] & 0xFF) * 1000;
		_points[1] = (_buf[38] & 0xFF) * 1000;
		_points[2] = (convertRFRXSQ2Strength(_buf[37] & 0xFF)) * 1000;
		_points[3] = DataParser.parse2Short(_buf, 40) * 1000;
		_points[4] = (_buf[3] & 0xFF) * -1000;
		_points[5] = (_buf[4] & 0xFF) * -1000;
		_points[6] = (_buf[35] & 0xFF) * 1000;
		_points[7] = ((_buf[36] & 0xFF) - 20) * 1000;
	}

	/**
	 * parse the buffered data from buffer 0 to 2 and add points to record set
	 * @param _recordSet
	 * @param _points
	 * @param sdLogVersion
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 */
	private static int parseVario(RecordSet _recordSet, int[] _points, int sdLogVersion, byte[] _buf0, byte[] _buf1, byte[] _buf2) {
		if (sdLogVersion == -1) sdLogVersion = getSdLogVerion(_buf1, _buf2);
		switch (sdLogVersion) {
		case 3:
			//0=RXSQ, 1=Height, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
			//8=Height, 9=Climb 1, 10=Climb 3, 11=Climb 10
			_points[8] = (DataParser.parse2Short(_buf1, 3) - 500) * 1000;
			//_points[0]max = DataParser.parse2Short(_buf1, 5) * 1000;
			//_points[0]min = DataParser.parse2Short(_buf1, 7) * 1000;
			_points[9]  = (DataParser.parse2UnsignedShort(_buf1[9], _buf2[0]) - 30000) * 10;
			_points[10] = (DataParser.parse2UnsignedShort(_buf1[1], _buf2[2]) - 30000) * 10;
			_points[11] = (DataParser.parse2UnsignedShort(_buf1[3], _buf2[4]) - 30000) * 10;
			break;
		case 4:
			//0=RXSQ, 1=Height, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
			//8=Height, 9=Climb 1, 10=Climb 3, 11=Climb 10
			tmpHeight = DataParser.parse2Short(_buf1, 2);
			if (tmpHeight > 1 && tmpHeight < 5000) {
				_points[8] = (tmpHeight - 500) * 1000;
				//pointsMax = DataParser.parse2Short(buf1, 4) * 1000;
				//pointsMin = DataParser.parse2Short(buf1, 6) * 1000;
				_points[9] = (DataParser.parse2UnsignedShort(_buf1, 8) - 30000) * 10;
			}
			tmpClimb10 = DataParser.parse2UnsignedShort(_buf2, 2) - 30000;
			if (tmpClimb10 > -10000 && tmpClimb10 < 10000) {
				_points[10] = (DataParser.parse2UnsignedShort(_buf2, 0) - 30000) * 10;
				_points[11] = tmpClimb10 * 10;
			}
			break;
		}
		return sdLogVersion;
	}

	/**
		 * parse the buffered data from buffer 0 to 3 and add points to record set
	 * @param _recordSet
	 * @param _points
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 */
	private static void parseGPS(RecordSet _recordSet, int[] _points, byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3) {
		tmpHeight = DataParser.parse2Short(_buf2, 8) - 500;
		tmpClimb3 = (_buf3[2] & 0xFF) - 120;
		if (tmpClimb3 > -50 && tmpHeight > -490 && tmpHeight < 5000) {
			//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripLength, 10=VoltageRx, 11=TemperatureRx
			//8=Height, 9=Climb 1, 10=Climb 3
			//12=Latitude, 13=Longitude, 14=Velocity, 15=DistanceStart, 16=DirectionStart, 17=TripDistance
			_points[14] = DataParser.parse2Short(_buf1, 4) * 1000;
			
			tmpLatitude = DataParser.parse2Short(_buf1, 7) * 10000 + DataParser.parse2Short(_buf1[9], _buf2[0]);
			tmpLatitude = _buf1[6] == 1 ? -1 * tmpLatitude : tmpLatitude;
			tmpLatitudeDelta = Math.abs(tmpLatitude -_points[12]);
			latitudeTolerance = (_points[14] / 1000.0)  * (HoTTbinReader2.timeStep_ms - lastLatitudeTimeStep) / HoTTAdapter.latitudeTolranceFactor;
			latitudeTolerance = latitudeTolerance > 0 ? latitudeTolerance : 5;

			if (_points[12] == 0 	|| tmpLatitudeDelta <= latitudeTolerance) {
				lastLatitudeTimeStep = HoTTbinReader2.timeStep_ms;
				_points[12] = tmpLatitude;
			}
			else {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, StringHelper.getFormatedTime("HH:mm:ss:SSS", HoTTbinReader2.timeStep_ms) + " Lat " + tmpLatitude + " - " + tmpLatitudeDelta);
			}
			
			tmpLongitude = DataParser.parse2Short(_buf2, 2) * 10000 + DataParser.parse2Short(_buf2, 4);
			tmpLongitude = _buf2[1] == 1 ? -1 * tmpLongitude : tmpLongitude;
//			tmpLongitudeDelta = Math.abs(tmpLongitude -_points[13]);
//			longitudeTolerance = (_points[14] / 1000.0)  * (HoTTbinReader2.timeStep_ms - lastLongitudeTimeStep) / HoTTAdapter.longitudeTolranceFactor;
//			longitudeTolerance = longitudeTolerance > 0 ? longitudeTolerance : 5;
//
//			if (_points[13] == 0 	|| tmpLongitudeDelta <= longitudeTolerance) {
//				lastLongitudeTimeStep = HoTTbinReader2.timeStep_ms;
				_points[13] = tmpLongitude;
//			}
//			else {
//				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, StringHelper.getFormatedTime("HH:mm:ss:SSS", HoTTbinReader2.timeStep_ms) + " Long " + tmpLongitude + " - " + tmpLongitudeDelta);
//			}

			_points[8] = tmpHeight * 1000;
			_points[9] = (DataParser.parse2UnsignedShort(_buf3, 0) - 30000) * 10;
			_points[10] = tmpClimb3 * 1000;
			_points[14] = DataParser.parse2Short(_buf1, 4) * 1000;
			_points[15] = DataParser.parse2Short(_buf2, 6) * 1000;
			_points[16] = (_buf1[3] & 0xFF) * 1000;
			_points[17] = 0;
		}
	}

	/**
		 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * @param _recordSet
	 * @param _points
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 */
	private static void parseGeneral(RecordSet _recordSet, int[] _points, byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) {
		tmpHeight = DataParser.parse2Short(_buf3, 0) - 500;
		tmpClimb3 = (_buf3[4] & 0xFF) - 120;
		int tmpVoltage1 = DataParser.parse2Short(_buf1[9], _buf2[0]);
		int tmpVoltage2 = DataParser.parse2Short(_buf2, 1);
		int tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
		//0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Height, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2							
		//8=Height, 9=Climb 1, 10=Climb 3
		//18=VoltageGen, 19=CurrentGen, 20=CapacityGen, 21=PowerGen, 22=BalanceGen, 23=CellVoltageGen 1, 24=CellVoltageGen 2 .... 28=CellVoltageGen 6, 29=Revolution, 30=FuelLevel, 31=VoltageGen 1, 32=VoltageGen 2, 33=TemperatureGen 1, 34=TemperatureGen 2
		if (tmpClimb3 > -50 && tmpHeight > -490 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600	&& tmpCapacity >= _points[20] / 1000) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			_points[18] = DataParser.parse2Short(_buf3, 7) * 1000;
			_points[19] = DataParser.parse2Short(_buf3, 5) * 1000;
			_points[20] = tmpCapacity * 1000;
			_points[21] = Double.valueOf(_points[18] / 1000.0 * _points[19]).intValue();
			for (int j = 0; j < 6; j++) {
				_points[j + 23] = (_buf1[3 + j] & 0xFF) * 1000;
				if (_points[j + 23] > 0) {
					maxVotage = _points[j + 23] > maxVotage ? _points[j + 23] : maxVotage;
					minVotage = _points[j + 23] < minVotage ? _points[j + 23] : minVotage;
				}
			}
			_points[22] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
			_points[29] = DataParser.parse2Short(_buf2, 8) * 1000;
			_points[8] = tmpHeight * 1000;
			_points[9] = (DataParser.parse2UnsignedShort(_buf3, 2) - 30000) * 10;
			_points[10] = tmpClimb3 * 1000;
			_points[30] = DataParser.parse2Short(_buf2, 6) * 1000;
			_points[31] = tmpVoltage1 * 1000;
			_points[32] = tmpVoltage2 * 1000;
			_points[33] = ((_buf2[3] & 0xFF) + 20) * 1000;
			_points[34] = ((_buf2[4] & 0xFF) + 20) * 1000;
		}
	}

	/**
		 * parse the buffered data from buffer 0 to 4 and add points to record set
	 * @param _recordSet
	 * @param _points
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 * @throws DataInconsitsentException
	 */
	private static void parseElectric(RecordSet _recordSet, int[] _points, byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4)
			throws DataInconsitsentException {
		tmpHeight = DataParser.parse2Short(_buf3, 3) - 500;
		tmpClimb3 = (_buf4[3] & 0xFF) - 120;
		int tmpVoltage1 = DataParser.parse2Short(_buf2, 7);
		int tmpVoltage2 = DataParser.parse2Short(_buf2[9], _buf3[0]);
		int tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
		//0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2 
		//8=Height, 9=Climb 1, 10=Climb 3
		//35=VoltageGen, 36=CurrentGen, 37=CapacityGen, 38=PowerGen, 39=BalanceGen, 40=CellVoltageGen 1, 41=CellVoltageGen 2 .... 53=CellVoltageGen 14, 54=VoltageGen 1, 55=VoltageGen 2, 56=TemperatureGen 1, 57=TemperatureGen 2 
		if (tmpClimb3 > -50 && tmpHeight > -490 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600	&& tmpCapacity >= _points[37] / 1000) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			_points[35] = DataParser.parse2Short(_buf3, 7) * 1000;
			_points[36] = DataParser.parse2Short(_buf3, 5) * 1000;
			_points[37] = tmpCapacity * 1000;
			_points[38] = Double.valueOf(_points[35] / 1000.0 * _points[36]).intValue(); // power U*I [W];
			for (int j = 0; j < 7; j++) {
				_points[j + 40] = (_buf1[3 + j] & 0xFF) * 1000;
				if (_points[j + 40] > 0) {
					maxVotage = _points[j + 40] > maxVotage ? _points[j + 40] : maxVotage;
					minVotage = _points[j + 40] < minVotage ? _points[j + 40] : minVotage;
				}
			}
			for (int j = 0; j < 7; j++) {
				_points[j + 47] = (_buf2[j] & 0xFF) * 1000;
				if (_points[j + 13] > 0) {
					maxVotage = _points[j + 47] > maxVotage ? _points[j + 47] : maxVotage;
					minVotage = _points[j + 47] < minVotage ? _points[j + 47] : minVotage;
				}
			}
			//calculate balance on the fly
			_points[39] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
			_points[8] = tmpHeight * 1000;
			_points[9] = (DataParser.parse2UnsignedShort(_buf4, 1) - 30000) * 10;
			_points[10] = tmpClimb3 * 1000;
			_points[54] = tmpVoltage1 * 1000;
			_points[55] = tmpVoltage2 * 1000;
			_points[56] = ((_buf3[1] & 0xFF) + 20) * 1000;
			_points[57] = ((_buf3[2] & 0xFF) + 20) * 1000;
		}
	}
}
