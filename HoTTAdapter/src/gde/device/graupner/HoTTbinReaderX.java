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
    
    Copyright (c) 2011,2012,2013,2014,2015 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.data.Channel;
import gde.data.RecordSet;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;
import gde.utils.FileUtils;
import gde.utils.StringHelper;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class to read Graupner HoTT binary data as saved on SD-Cards 
 * @author Winfried Brügmann
 */
public class HoTTbinReaderX extends HoTTbinReader {
	final static String	$CLASS_NAMEX	= HoTTbinReaderX.class.getName();
	final static Logger	logx					= Logger.getLogger(HoTTbinReaderX.$CLASS_NAMEX);

	static int[] points_1;

	/**
	 * convert from RF_RXSQ to strength using lookup table
	 * @param inValue
	 * @return
	 */
	static int convertRxDbm2Strength(int inValue) {
		if (inValue >= 40 && inValue < HoTTbinReaderX.lookup.length + 40) {
			return HoTTbinReaderX.lookup[inValue - 40];
		}
		else if (inValue < 40)
			return 100;
		else
			return 0;
	}

	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws Exception 
	 */
	public static synchronized void read(String filePath) throws Exception {
		HoTTbinReader.sensorSignature = new StringBuilder().append(GDE.STRING_LEFT_BRACKET).append(HoTTAdapter.Sensor.RECEIVER.name()).append(GDE.STRING_COMMA);
		for (int i = 0; i < HoTTAdapter.isSensorType.length; i++) {
			HoTTAdapter.isSensorType[i] = false;
		}
		File inputFile = new File(filePath);
		if (inputFile.exists()) {
			FileInputStream file_input = new FileInputStream(inputFile);
			DataInputStream data_in = new DataInputStream(file_input);
			byte[] buffer = new byte[23];
			data_in.read(buffer);
			data_in.close();
			if (new String(buffer).startsWith("GRAUPNER SD LOG8"))
				readSingle(inputFile);
			else {
				DataExplorer.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2408));
				throw new DataTypeException(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2408));
			}
		}
		else throw new IOException(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2409));
	}

	/**
	* read log data according to version 0
	* @param file
	* @throws IOException 
	* @throws DataInconsitsentException 
	*/
	static void readSingle(File file) throws IOException, DataInconsitsentException {
		final String $METHOD_NAME = "readSingle";
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		HoTTAdapterX device = (HoTTAdapterX) HoTTbinReaderX.application.getActiveDevice();
		int recordSetNumber = HoTTbinReaderX.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		boolean isInitialSwitched = false;
		HoTTbinReaderX.recordSetReceiver = null; //0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=UminRx
		HoTTbinReaderX.recordSetChannel = null; //0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 10=Ch 8
		HoTTbinReaderX.points_1 = new int[device.getNumberOfMeasurements(1)];
		HoTTbinReaderX.pointsChannel = new int[15];
		HoTTbinReaderX.timeStep_ms = 0;
		HoTTbinReaderX.dataBlockSize = 23; //device.getDataBlockSize(InputTypes.FILE_IO);
		HoTTbinReaderX.buf = new byte[HoTTbinReaderX.dataBlockSize];
		HoTTbinReaderX.buf1 = null;
		HoTTbinReaderX.buf2 = null;
		HoTTbinReaderX.buf3 = null;
		HoTTbinReaderX.buf4 = null;
		HoTTbinReaderX.buf5 = null;
		HoTTbinReaderX.buf6 = null;
		HoTTbinReaderX.buf7 = null;
		HoTTbinReaderX.buf8 = null;
		HoTTbinReaderX.buf9 = null;
		HoTTbinReaderX.bufA = null;
		HoTTbinReaderX.bufB = null;
		HoTTbinReaderX.bufC = null;
		HoTTbinReaderX.bufD = null;
		HoTTAdapter.reverseChannelPackageLossCounter.clear();
		HoTTbinReaderX.lostPackages.clear();
		HoTTbinReaderX.countLostPackages = 0;
		HoTTAdapter.isChannelsChannelEnabled = false;
		int lastCounter = 0x00;
		int countPackageLoss = 0;
		int headerSize = 27;
		int footerSize = 323;
		int lapTimes = 99;
		long numberDatablocks = (fileSize - headerSize - footerSize) / HoTTbinReaderX.dataBlockSize;
		long startTimeStamp_ms = file.lastModified() - (numberDatablocks * 10);
		String date = new SimpleDateFormat("yyyy-MM-dd").format(startTimeStamp_ms); //$NON-NLS-1$
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		MenuToolBar menuToolBar = HoTTbinReaderX.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		if (menuToolBar != null) HoTTbinReaderX.application.setProgress(0, sThreadId);

		try {
			HoTTAdapter.recordSets.clear();

			//channel data are always contained
			//check if recordSetChannel initialized, transmitter and receiver data always present, but not in the same data rate and signals
			channel = HoTTbinReaderX.channels.get(2);
			channel.setFileDescription(HoTTbinReaderX.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReaderX.application.getObjectKey() : date);
			recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.CHANNEL.value() + recordSetNameExtend;
			HoTTbinReaderX.recordSetChannel = RecordSet.createRecordSet(recordSetName, device, 2, true, true);
			channel.put(recordSetName, HoTTbinReaderX.recordSetChannel);
			HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.CHANNEL.value(), HoTTbinReaderX.recordSetChannel);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (HoTTbinReaderX.application.getMenuToolBar() != null) {
				channel.applyTemplate(recordSetName, false);
			}

			//receiver data are always contained
			//check if recordSetReceiver initialized, transmitter and receiver data always present, but not in the same data rate and signals
			channel = HoTTbinReaderX.channels.get(1);
			channel.setFileDescription(HoTTbinReaderX.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReaderX.application.getObjectKey() : date);
			recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.RECEIVER.value() + recordSetNameExtend;
			HoTTbinReaderX.recordSetReceiver = RecordSet.createRecordSet(recordSetName, device, 1, true, true);
			channel.put(recordSetName, HoTTbinReaderX.recordSetReceiver);
			HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.RECEIVER.value(), HoTTbinReaderX.recordSetReceiver);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (HoTTbinReaderX.application.getMenuToolBar() != null) {
				channel.applyTemplate(recordSetName, false);
			}
			//recordSetReceiver initialized and ready to add data
			HoTTbinReader.sensorSignature = new StringBuilder().append(GDE.STRING_LEFT_BRACKET).append(HoTTAdapter.Sensor.RECEIVER.name()).append(GDE.STRING_COMMA);

			data_in.skip(27); //header with constant length

			//read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReaderX.buf);
				if (HoTTbinReaderX.logx.isLoggable(Level.FINE)) {
					if (HoTTbinReaderX.buf[6] == 0x41) HoTTbinReaderX.logx.logp(Level.FINE, HoTTbinReaderX.$CLASS_NAMEX, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReaderX.buf.length));
					HoTTbinReaderX.logx.logp(Level.FINE, HoTTbinReaderX.$CLASS_NAMEX, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReaderX.buf, HoTTbinReaderX.buf.length));
				}

				if (lastCounter == 0x00 || lastCounter == ((HoTTbinReaderX.buf[0] & 0xFF) - 1) || (lastCounter == 255 && HoTTbinReaderX.buf[0] == 0)) {
					if (HoTTbinReaderX.buf[3] != 0 && HoTTbinReaderX.buf[4] != 0) { //buf 3, 4, tx,rx
						HoTTAdapter.reverseChannelPackageLossCounter.add(1);
						HoTTbinReaderX.points_1[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
						//create and fill sensor specific data record sets 
						if (HoTTbinReaderX.logx.isLoggable(Level.FINER))
							HoTTbinReaderX.logx.logp(Level.FINER, HoTTbinReaderX.$CLASS_NAMEX, $METHOD_NAME, StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReaderX.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT
									+ StringHelper.printBinary(HoTTbinReaderX.buf[7], false));

						if (HoTTAdapter.isChannelsChannelEnabled) {
							parseAddChannel(HoTTbinReaderX.buf);
						}

						//fill data blocks
						switch (HoTTbinReaderX.buf[6]) {
						case 0x41:
							HoTTbinReaderX.buf1 = new byte[HoTTbinReaderX.dataBlockSize];
							System.arraycopy(HoTTbinReaderX.buf, 0, HoTTbinReaderX.buf1, 0, HoTTbinReaderX.buf.length);
							break;
						case 0x42:
							HoTTbinReaderX.buf2 = new byte[HoTTbinReaderX.dataBlockSize];
							System.arraycopy(HoTTbinReaderX.buf, 0, HoTTbinReaderX.buf2, 0, HoTTbinReaderX.buf.length);
							break;
						case 0x43:
							HoTTbinReaderX.buf3 = new byte[HoTTbinReaderX.dataBlockSize];
							System.arraycopy(HoTTbinReaderX.buf, 0, HoTTbinReaderX.buf3, 0, HoTTbinReaderX.buf.length);
							break;
						case 0x44:
							HoTTbinReaderX.buf4 = new byte[HoTTbinReaderX.dataBlockSize];
							System.arraycopy(HoTTbinReaderX.buf, 0, HoTTbinReaderX.buf4, 0, HoTTbinReaderX.buf.length);
							break;
						case 0x45:
							HoTTbinReaderX.buf5 = new byte[HoTTbinReaderX.dataBlockSize];
							System.arraycopy(HoTTbinReaderX.buf, 0, HoTTbinReaderX.buf5, 0, HoTTbinReaderX.buf.length);
							break;
						case 0x46:
							HoTTbinReaderX.buf6 = new byte[HoTTbinReaderX.dataBlockSize];
							System.arraycopy(HoTTbinReaderX.buf, 0, HoTTbinReaderX.buf6, 0, HoTTbinReaderX.buf.length);
							break;
						case 0x47:
							HoTTbinReaderX.buf7 = new byte[HoTTbinReaderX.dataBlockSize];
							System.arraycopy(HoTTbinReaderX.buf, 0, HoTTbinReaderX.buf7, 0, HoTTbinReaderX.buf.length);
							break;
						case 0x48:
							HoTTbinReaderX.buf8 = new byte[HoTTbinReaderX.dataBlockSize];
							System.arraycopy(HoTTbinReaderX.buf, 0, HoTTbinReaderX.buf8, 0, HoTTbinReaderX.buf.length);
							break;
						case 0x49:
							HoTTbinReaderX.buf9 = new byte[HoTTbinReaderX.dataBlockSize];
							System.arraycopy(HoTTbinReaderX.buf, 0, HoTTbinReaderX.buf9, 0, HoTTbinReaderX.buf.length);
							break;
						case 0x4A:
							HoTTbinReaderX.bufA = new byte[HoTTbinReaderX.dataBlockSize];
							System.arraycopy(HoTTbinReaderX.buf, 0, HoTTbinReaderX.bufA, 0, HoTTbinReaderX.buf.length);
							break;
						case 0x4B:
							HoTTbinReaderX.bufB = new byte[HoTTbinReaderX.dataBlockSize];
							System.arraycopy(HoTTbinReaderX.buf, 0, HoTTbinReaderX.bufB, 0, HoTTbinReaderX.buf.length);
							break;
						case 0x4C:
							HoTTbinReaderX.bufC = new byte[HoTTbinReaderX.dataBlockSize];
							System.arraycopy(HoTTbinReaderX.buf, 0, HoTTbinReaderX.bufC, 0, HoTTbinReaderX.buf.length);
							break;
						case 0x4D:
							HoTTbinReaderX.bufD = new byte[HoTTbinReaderX.dataBlockSize];
							System.arraycopy(HoTTbinReaderX.buf, 0, HoTTbinReaderX.bufD, 0, HoTTbinReaderX.buf.length);
							if (!HoTTAdapter.isChannelsChannelEnabled)
								HoTTbinReaderX.buf1 = HoTTbinReaderX.buf2 = HoTTbinReaderX.buf3 = HoTTbinReaderX.buf4 = HoTTbinReaderX.buf5 = HoTTbinReaderX.buf6 = HoTTbinReaderX.buf7 = HoTTbinReaderX.buf8 = HoTTbinReaderX.buf9 = HoTTbinReaderX.bufA = HoTTbinReaderX.bufB = HoTTbinReaderX.bufC = HoTTbinReaderX.bufD = null;

							HoTTAdapter.isChannelsChannelEnabled = true;
							break;
						}
						//fill receiver data
						if (HoTTbinReaderX.buf1 != null && HoTTbinReaderX.buf2 != null && HoTTbinReaderX.buf3 != null && HoTTbinReaderX.buf4 != null && HoTTbinReaderX.buf5 != null && HoTTbinReaderX.buf6 != null
								&& HoTTbinReaderX.buf7 != null && HoTTbinReaderX.buf8 != null && HoTTbinReaderX.buf9 != null && HoTTbinReaderX.bufA != null && HoTTbinReaderX.bufB != null
								&& HoTTbinReaderX.bufC != null && HoTTbinReaderX.bufD != null) {
//							printByteValues(timeStep_ms, HoTTbinReaderX.buf1);
//							printByteValues(timeStep_ms, HoTTbinReaderX.buf2);
//							printByteValues(timeStep_ms, HoTTbinReaderX.buf3);
//							printByteValues(timeStep_ms, HoTTbinReaderX.buf4);
//							printByteValues(timeStep_ms, HoTTbinReaderX.buf5);
//							printByteValues(timeStep_ms, HoTTbinReaderX.buf6);
//							printByteValues(timeStep_ms, HoTTbinReaderX.buf7);
//							printByteValues(timeStep_ms, HoTTbinReaderX.buf8);
//							printByteValues(timeStep_ms, HoTTbinReaderX.buf9);
//							printByteValues(timeStep_ms, HoTTbinReaderX.bufA);
//							printByteValues(timeStep_ms, HoTTbinReaderX.bufB);
//							printByteValues(timeStep_ms, HoTTbinReaderX.bufC);
//							printByteValues(timeStep_ms, HoTTbinReaderX.bufD);
//							HoTTbinReaderX.logx.logp(Level.OFF, HoTTbinReaderX.$CLASS_NAMEX, $METHOD_NAME, GDE.STRING_BLANK);
							switch (HoTTbinReaderX.buf[7]) {
							case 00: //receiver 1
								parseAddReceiver(HoTTbinReaderX.buf1, HoTTbinReaderX.buf2, HoTTbinReaderX.bufD);
								break;
							case 02: //ESC 1
								if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()] == false) 
									HoTTbinReader.sensorSignature.append(HoTTAdapter.Sensor.ESC.name()).append(GDE.STRING_COMMA);
								HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()] = true;
								parseESC(HoTTbinReaderX.buf3, HoTTbinReaderX.buf4, HoTTbinReaderX.buf5, HoTTbinReaderX.buf6, HoTTbinReaderX.buf7, HoTTbinReaderX.buf8, HoTTbinReaderX.buf9, HoTTbinReaderX.bufA);
								break;
							}
							HoTTbinReaderX.buf1 = HoTTbinReaderX.buf2 = HoTTbinReaderX.buf3 = HoTTbinReaderX.buf4 = HoTTbinReaderX.buf5 = HoTTbinReaderX.buf6 = HoTTbinReaderX.buf7 = HoTTbinReaderX.buf8 = HoTTbinReaderX.buf9 = HoTTbinReaderX.bufA = HoTTbinReaderX.bufB = HoTTbinReaderX.bufC = HoTTbinReaderX.bufD = null;
						}

						HoTTbinReaderX.timeStep_ms += 3;// add default time step from device of 3 msec

						if (menuToolBar != null && i % progressIndicator == 0) HoTTbinReaderX.application.setProgress((int) (i * 100 / numberDatablocks), sThreadId);

						if (HoTTbinReaderX.countLostPackages > 0) {
							HoTTbinReaderX.lostPackages.add(HoTTbinReaderX.countLostPackages);
							HoTTbinReaderX.countLostPackages = 0;
						}
					}
					else { //skip empty block, but add time step
						if (HoTTbinReaderX.logx.isLoggable(Level.FINE)) HoTTbinReaderX.logx.log(Level.FINE, "-->> Found tx=rx=0 dBm");

						HoTTAdapter.reverseChannelPackageLossCounter.add(0);
						HoTTbinReaderX.points_1[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;

						++countPackageLoss; // add up lost packages in telemetry data 
						++HoTTbinReaderX.countLostPackages;
						//HoTTbinReaderX.points_1[0] = (int) (countPackageLoss*100.0 / ((HoTTbinReader2.timeStep_ms+10) / 10.0)*1000.0); 

						if (HoTTAdapter.isChannelsChannelEnabled) {
							parseAddChannel(HoTTbinReaderX.buf);
						}

						HoTTbinReaderX.timeStep_ms += 3;
						//reset buffer to avoid mixing data >> 20 Jul 14, not any longer required due to protocol change requesting next sensor data block
						//HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
					}
					lastCounter = buf[0] & 0xFF;
				}
				else {
					HoTTbinReaderX.logx.log(Level.WARNING, new String(HoTTbinReaderX.buf));
				}
			}
			String packageLossPercentage = HoTTbinReaderX.recordSetReceiver.getRecordDataSize(true) > 0 ? String.format("%.1f",
					(countPackageLoss / HoTTbinReaderX.recordSetReceiver.getTime_ms(HoTTbinReaderX.recordSetReceiver.getRecordDataSize(true) - 1) * 1000)) : "100";
			HoTTbinReader.sensorSignature.deleteCharAt(HoTTbinReader.sensorSignature.length() - 1).append(GDE.STRING_RIGHT_BRACKET);
			HoTTbinReaderX.recordSetReceiver.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] { countPackageLoss, packageLossPercentage, HoTTbinReaderX.lostPackages.getStatistics() })
					+ HoTTbinReaderX.sensorSignature);
			HoTTbinReaderX.logx.logp(Level.WARNING, HoTTbinReaderX.$CLASS_NAMEX, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReaderX.buf = new byte[footerSize];
			HoTTbinReaderX.buf0 = new byte[lapTimes]; //min
			HoTTbinReaderX.buf1 = new byte[lapTimes]; //sec
			HoTTbinReaderX.buf2 = new byte[lapTimes]; //1/100secs
			data_in.read(HoTTbinReaderX.buf);
			System.arraycopy(HoTTbinReaderX.buf, 19, HoTTbinReaderX.buf0, 0, HoTTbinReaderX.buf0.length);
			System.arraycopy(HoTTbinReaderX.buf, 19+lapTimes, HoTTbinReaderX.buf1, 0, HoTTbinReaderX.buf1.length);
			System.arraycopy(HoTTbinReaderX.buf, 19+2*lapTimes, HoTTbinReaderX.buf2, 0, HoTTbinReaderX.buf2.length);
			int numLaps = HoTTbinReaderX.buf[HoTTbinReaderX.buf.length-1];
			StringBuilder sb = new StringBuilder().append(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2406));
			for (int j=0; j < numLaps && !(HoTTbinReaderX.buf0[j] == 0 && HoTTbinReaderX.buf1[j] == 0 && HoTTbinReaderX.buf2[j] == 0); j++) {
				sb.append(String.format("%2d: %2dm %02ds %03d\n", (j+1), HoTTbinReaderX.buf0[j], HoTTbinReaderX.buf1[j], HoTTbinReaderX.buf2[j]));
			}
			if (numLaps >= 1) {
				HoTTbinReaderX.recordSetReceiver.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
						+ Messages.getString(	gde.device.graupner.hott.MessageIds.GDE_MSGI2405,
								new Object[] {	numLaps,
										String.format("%2dm %02ds %03d", HoTTbinReaderX.buf[HoTTbinReaderX.buf.length - 7], HoTTbinReaderX.buf[HoTTbinReaderX.buf.length - 6], HoTTbinReaderX.buf[HoTTbinReaderX.buf.length - 5]),
										String.format("%2dm %02ds %03d", HoTTbinReaderX.buf[HoTTbinReaderX.buf.length - 4], HoTTbinReaderX.buf[HoTTbinReaderX.buf.length - 3], HoTTbinReaderX.buf[HoTTbinReaderX.buf.length - 2]) }) + sb.toString());
			}
			HoTTbinReaderX.logx.logp(Level.TIME, HoTTbinReaderX.$CLASS_NAMEX, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (menuToolBar != null) {
				HoTTbinReaderX.application.setProgress(100, sThreadId);
				if (!isInitialSwitched) {
					HoTTbinReaderX.channels.switchChannel(channel.getName());
					channel.switchRecordSet(recordSetName);
				}

				for (RecordSet recordSet : HoTTAdapter.recordSets.values()) {
					device.makeInActiveDisplayable(recordSet);
					device.updateVisibilityStatus(recordSet, true);

					//write filename after import to record description
					recordSet.descriptionAppendFilename(file.getName());
				}

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				HoTTbinReaderX.application.setProgress(100, sThreadId);
			}
		}
		finally {
			data_in.close();
			data_in = null;
		}
	}

	/**
	 * compose the record set extend to give capability to identify source of this record set
	 * @param file
	 * @return
	 */
	protected static String getRecordSetExtend(File file) {
		String recordSetNameExtend = GDE.STRING_EMPTY;
		if (file.getName().contains(GDE.STRING_UNDER_BAR)) {
			try {
				Integer.parseInt(file.getName().substring(0, file.getName().lastIndexOf(GDE.STRING_UNDER_BAR)));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, file.getName().lastIndexOf(GDE.STRING_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (file.getName().substring(0, file.getName().lastIndexOf(GDE.STRING_UNDER_BAR)).length() <= 8)
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, file.getName().lastIndexOf(GDE.STRING_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		else {
			try {
				Integer.parseInt(file.getName().substring(0, 4));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, 4) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (file.getName().substring(0, file.getName().length()).length() <= 8 + 4)
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, file.getName().length() - 4) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		return recordSetNameExtend;
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	private static void parseAddReceiver(byte[] _buf1, byte[] _buf2, byte[] _bufD) throws DataInconsitsentException {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx, 8=Umin Rx 
		HoTTbinReaderX.tmpVoltageRx = (_buf2[18] & 0xFF);
		HoTTbinReaderX.tmpTemperatureRx = (_buf2[20] & 0xFF);
		HoTTbinReaderX.points_1[1] = (_buf2[17] & 0xFF) * 1000;
		HoTTbinReaderX.points_1[3] = DataParser.parse2Short(_buf1, 17) * 1000;
		if (!HoTTAdapter.isFilterEnabled || HoTTbinReaderX.tmpVoltageRx > -1 && HoTTbinReaderX.tmpVoltageRx < 100 && HoTTbinReaderX.tmpTemperatureRx < 120) {
			HoTTbinReaderX.points_1[2] = (convertRxDbm2Strength(_bufD[4] & 0xFF)) * 1000;
			HoTTbinReaderX.points_1[4] = (_bufD[3] & 0xFF) * -1000;
			HoTTbinReaderX.points_1[5] = (_bufD[4] & 0xFF) * -1000;
			HoTTbinReaderX.points_1[6] = (_buf2[18] & 0xFF) * 1000;
			HoTTbinReaderX.points_1[7] = (_buf2[20] & 0xFF) * 1000;
			HoTTbinReaderX.points_1[8] = (_buf2[19] & 0xFF) * 1000;
		}

		//printByteValues(_timeStep_ms, _buf);
		//if (HoTTbinReaderX.points_1[3] > 2000000)
		//	System.out.println();

		HoTTbinReaderX.recordSetReceiver.addPoints(HoTTbinReaderX.points_1, HoTTbinReaderX.timeStep_ms);
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	private static void parseAddChannel(byte[] _buf) throws DataInconsitsentException {

		//0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 10=Ch 8
		//119=PowerOff, 12=BattLow, 13=Reset, 14=reserved
		HoTTbinReaderX.pointsChannel[0] = (_buf[1] & 0xFF) * 1000;
		HoTTbinReaderX.pointsChannel[1] = (_buf[3] & 0xFF) * -1000;
		HoTTbinReaderX.pointsChannel[2] = (_buf[4] & 0xFF) * -1000;
		HoTTbinReaderX.pointsChannel[3] = (DataParser.parse2UnsignedShort(_buf, 8) / 2) * 1000;
		HoTTbinReaderX.pointsChannel[4] = (DataParser.parse2UnsignedShort(_buf, 10) / 2) * 1000;
		HoTTbinReaderX.pointsChannel[5] = (DataParser.parse2UnsignedShort(_buf, 12) / 2) * 1000;
		HoTTbinReaderX.pointsChannel[6] = (DataParser.parse2UnsignedShort(_buf, 14) / 2) * 1000;

		//printByteValues(_timeStep_ms, _buf);

		HoTTbinReaderX.recordSetChannel.addPoints(HoTTbinReaderX.pointsChannel, HoTTbinReaderX.timeStep_ms);
	}
	
	/**
	 * parse the buffered data from buffer 3 to A and add points to record set
	 * @param _buf3
	 * @param _buf4
	 * @param _buf5
	 * @param _buf6
	 * @param _buf7
	 * @param _buf8
	 * @param _buf9
	 * @param _bufA
	 * @throws DataInconsitsentException
	 */
	private static void parseESC(byte[] _buf3, byte[] _buf4, byte[] _buf5, byte[] _buf6, byte[] _buf7, byte[] _buf8, byte[] _buf9, byte[] _bufA) throws DataInconsitsentException {
		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
		//9=VoltageM, 10=VoltageM_min, 11=CurrentM, 12=CurrentM_max, 13=CapacityM, 14=PowerM, 15=RevolutionM, 16=RevolutionM_max
		//17=Temperature, 18=Temperature_max, 19=TemperatureM, 20=TemperatureM_max, 21=Speed, 22=Speed_max
		HoTTbinReader.tmpVoltage = DataParser.parse2Short(_buf4, 20);
		HoTTbinReader.tmpCurrent = DataParser.parse2Short(_buf6, 20);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf5, 20);
		HoTTbinReader.tmpRevolution = DataParser.parse2Short(_buf7[20], _buf8[17]);
		HoTTbinReader.tmpTemperatureFet = _buf8[20] - 20;
		if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpVoltage > 0 && HoTTbinReader.tmpVoltage < 1000 && HoTTbinReader.tmpCurrent < 4000 && HoTTbinReader.tmpCurrent > -10
				&& HoTTbinReader.tmpRevolution > -1 && HoTTbinReader.tmpRevolution < 20000
				&& !(HoTTbinReaderX.points_1[19] != 0 && HoTTbinReaderX.points_1[19] / 1000 - HoTTbinReader.tmpTemperatureFet > 20)) {
			HoTTbinReaderX.points_1[9] = HoTTbinReader.tmpVoltage * 1000;
			HoTTbinReaderX.points_1[10] = DataParser.parse2Short(_buf5, 18) * 1000;
			HoTTbinReaderX.points_1[11] = HoTTbinReader.tmpCurrent * 1000;
			HoTTbinReaderX.points_1[12] = DataParser.parse2Short(_buf7, 18) * 1000;	
			HoTTbinReaderX.points_1[14] = Double.valueOf(HoTTbinReaderX.points_1[9] / 1000.0 * HoTTbinReaderX.points_1[11] / 100.0).intValue();
			if (!HoTTAdapter.isFilterEnabled
					|| (HoTTbinReader.tmpCapacity != 0 && Math.abs(HoTTbinReader.tmpCapacity) <= (HoTTbinReaderX.points_1[13] / 1000 + HoTTbinReader.tmpVoltage * HoTTbinReader.tmpCurrent / 2500 + 2))) {
				HoTTbinReaderX.points_1[13] = HoTTbinReader.tmpCapacity * 1000;
			}
			else {
				if (HoTTbinReader.tmpCapacity != 0)
					HoTTbinReader.log.log(Level.WARNING, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - "
						+ (HoTTbinReaderX.points_1[13] / 1000) + " + " + (HoTTbinReader.tmpVoltage * HoTTbinReader.tmpCurrent / 2500 + 2));
			}
			HoTTbinReaderX.points_1[15] = HoTTbinReader.tmpRevolution * 1000;
			HoTTbinReaderX.points_1[16] = DataParser.parse2Short(_buf8, 18) * 1000;
			HoTTbinReaderX.points_1[17] = (_buf6[18] - 20) * 1000;
			HoTTbinReaderX.points_1[18] = (_buf6[19] - 20) * 1000;
			HoTTbinReaderX.points_1[19] = HoTTbinReader.tmpTemperatureFet * 1000;
			HoTTbinReaderX.points_1[20] = (_buf9[17] - 20) * 1000;
			HoTTbinReaderX.points_1[21] = _buf9[19] * 1000;
			HoTTbinReaderX.points_1[22] = _bufA[17] * 1000;
		}
	}


	static void printByteValues(long millisec, byte[] buffer) {
		StringBuilder sb = new StringBuilder().append(StringHelper.getFormatedTime("mm:ss:SSS", millisec)).append(" : ");
		for (int i = 16; buffer != null && i < buffer.length; i++) {
			sb.append("(").append(i).append(")").append(String.format("%3d", buffer[i]&0xFF)).append(GDE.STRING_BLANK);
		}
		HoTTbinReaderX.logx.log(Level.OFF, sb.toString());
	}

	static void printShortValues(long millisec, byte[] buffer) {
		StringBuilder sb = new StringBuilder().append(StringHelper.getFormatedTime("mm:ss:SSS", millisec)).append(" : ");
		for (int i = 0; buffer != null && i < buffer.length - 1; i++) {
			sb.append("(").append(i).append(")").append(DataParser.parse2Short(buffer, i)).append(GDE.STRING_BLANK);
		}
		HoTTbinReaderX.logx.log(Level.FINE, sb.toString());
	}

	/**
	 * main method for test purpose only !
	 * @param args
	 */
	public static void main(String[] args) {
		String directory = "f:\\Documents\\DataExplorer\\HoTTAdapter\\";

		try {
			List<File> files = FileUtils.getFileListing(new File(directory), 2);
			for (File file : files) {
				if (!file.isDirectory() && file.getName().endsWith(".bin")) {
					//					FileInputStream file_input = new FileInputStream(file);
					//					DataInputStream data_in = new DataInputStream(file_input);
					//					HoTTbinReader.buf = new byte[64];
					//					data_in.read(HoTTbinReader.buf);
					//					System.out.println(file.getName());
					//					System.out.println(StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
					//					System.out.println(StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
					//					data_in.close();
					System.out.println(file.getName() + " - " + Integer.parseInt(getFileInfo(file).get(HoTTAdapter.SD_LOG_VERSION)));

				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
