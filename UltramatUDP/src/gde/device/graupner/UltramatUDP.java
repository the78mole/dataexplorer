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
    
    Copyright (c) 2008,2009,2010,2011 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.RecordSet;
import gde.device.DesktopPropertyType;
import gde.device.DesktopPropertyTypes;
import gde.device.DeviceConfiguration;
import gde.exception.DataInconsitsentException;
import gde.io.LogViewReader;
import gde.log.Level;
import gde.messages.Messages;

import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

/**
 * eStation base device class
 * @author Winfried Brügmann
 */
public class UltramatUDP extends Ultramat {
	final static Logger						logger	= Logger.getLogger(UltramatUDP.class.getName());

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public UltramatUDP(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		Messages.setDeviceResourceBundle("gde.device.htronic.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2200), Messages.getString(MessageIds.GDE_MSGT2201), Messages.getString(MessageIds.GDE_MSGT2202), Messages.getString(MessageIds.GDE_MSGT2203), Messages.getString(MessageIds.GDE_MSGT2204), Messages.getString(MessageIds.GDE_MSGT2205), Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2207), Messages.getString(MessageIds.GDE_MSGT2208), Messages.getString(MessageIds.GDE_MSGT2209)};
		this.CHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212), Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2214), Messages.getString(MessageIds.GDE_MSGT2215), Messages.getString(MessageIds.GDE_MSGT2216), Messages.getString(MessageIds.GDE_MSGT2217), Messages.getString(MessageIds.GDE_MSGT2218), Messages.getString(MessageIds.GDE_MSGT2219), Messages.getString(MessageIds.GDE_MSGT2220), Messages.getString(MessageIds.GDE_MSGT2221), Messages.getString(MessageIds.GDE_MSGT2222)};
		this.DISCHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212), Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2223), Messages.getString(MessageIds.GDE_MSGT2224), Messages.getString(MessageIds.GDE_MSGT2220), Messages.getString(MessageIds.GDE_MSGT2222)};
		this.DELAY_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2225), Messages.getString(MessageIds.GDE_MSGT2226), Messages.getString(MessageIds.GDE_MSGT2227), Messages.getString(MessageIds.GDE_MSGT2228)}; 
		this.CURRENT_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2229), Messages.getString(MessageIds.GDE_MSGT2230), Messages.getString(MessageIds.GDE_MSGT2231), Messages.getString(MessageIds.GDE_MSGT2232), Messages.getString(MessageIds.GDE_MSGT2233), Messages.getString(MessageIds.GDE_MSGT2234), Messages.getString(MessageIds.GDE_MSGT2235), Messages.getString(MessageIds.GDE_MSGT2236),  Messages.getString(MessageIds.GDE_MSGT2237)};

		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public UltramatUDP(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2200), Messages.getString(MessageIds.GDE_MSGT2201), Messages.getString(MessageIds.GDE_MSGT2202), Messages.getString(MessageIds.GDE_MSGT2203), Messages.getString(MessageIds.GDE_MSGT2204), Messages.getString(MessageIds.GDE_MSGT2205), Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2207), Messages.getString(MessageIds.GDE_MSGT2208), Messages.getString(MessageIds.GDE_MSGT2209)};
		this.CHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212), Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2214), Messages.getString(MessageIds.GDE_MSGT2215), Messages.getString(MessageIds.GDE_MSGT2216), Messages.getString(MessageIds.GDE_MSGT2217), Messages.getString(MessageIds.GDE_MSGT2218), Messages.getString(MessageIds.GDE_MSGT2219), Messages.getString(MessageIds.GDE_MSGT2220), Messages.getString(MessageIds.GDE_MSGT2221), Messages.getString(MessageIds.GDE_MSGT2222)};
		this.DISCHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212), Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2223), Messages.getString(MessageIds.GDE_MSGT2224), Messages.getString(MessageIds.GDE_MSGT2220), Messages.getString(MessageIds.GDE_MSGT2222)};
		this.DELAY_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2225), Messages.getString(MessageIds.GDE_MSGT2226), Messages.getString(MessageIds.GDE_MSGT2227), Messages.getString(MessageIds.GDE_MSGT2228)}; 
		this.CURRENT_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2229), Messages.getString(MessageIds.GDE_MSGT2230), Messages.getString(MessageIds.GDE_MSGT2231), Messages.getString(MessageIds.GDE_MSGT2232), Messages.getString(MessageIds.GDE_MSGT2233), Messages.getString(MessageIds.GDE_MSGT2234), Messages.getString(MessageIds.GDE_MSGT2235), Messages.getString(MessageIds.GDE_MSGT2236),  Messages.getString(MessageIds.GDE_MSGT2237)};

		this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	@Override
	public int getLovDataByteSize() {
		return 150;
	}

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
	 * this method is more usable for real logger, where data can be stored and converted in one block
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	@Override
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int deviceDataBufferSize = this.getDataBlockSize(); // const.
		int deviceDataBufferSize2 = deviceDataBufferSize / 2;
		int channel2Offset = deviceDataBufferSize2 - 5;
		int[] points = new int[this.getNumberOfMeasurements(recordSet.getChannelConfigNumber())];
		int offset = 4;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();
		long lastDateTime = 0, sumTimeDelta = 0, deltaTime = 0;
		int outputChannel = recordSet.getChannelConfigNumber();

		if (dataBuffer[offset] == 0x0C) {
			byte[] convertBuffer = new byte[deviceDataBufferSize];
			if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

			for (int i = 0; i < recordDataSize; i++) {
				if (outputChannel == 1)
					System.arraycopy(dataBuffer, offset + i * lovDataSize, convertBuffer, 0, deviceDataBufferSize2);
				else if (outputChannel == 2)
					System.arraycopy(dataBuffer, channel2Offset + offset + i * lovDataSize, convertBuffer, 0, deviceDataBufferSize2);
				else if (outputChannel == 3) System.arraycopy(dataBuffer, offset + i * lovDataSize, convertBuffer, 0, deviceDataBufferSize);
				recordSet.addPoints(convertDataBytes(points, convertBuffer));

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}

			recordSet.setTimeStep_ms(this.getAverageTimeStep_ms() != null ? this.getAverageTimeStep_ms() : 1000); // no average time available, use a hard coded one
		}
		else { // none constant time steps
			byte[] sizeBuffer = new byte[4];
			byte[] convertBuffer = new byte[deviceDataBufferSize];

			if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
			for (int i = 0; i < recordDataSize; i++) {
				System.arraycopy(dataBuffer, offset, sizeBuffer, 0, 4);
				lovDataSize = 4 + LogViewReader.parse2Int(sizeBuffer);
				System.arraycopy(dataBuffer, offset + 4, convertBuffer, 0, deviceDataBufferSize);
				recordSet.addPoints(convertDataBytes(points, convertBuffer));
				offset += lovDataSize;

				StringBuilder sb = new StringBuilder();
				byte[] timeBuffer = new byte[lovDataSize - deviceDataBufferSize - 4];
				//sb.append(timeBuffer.length).append(" - ");
				System.arraycopy(dataBuffer, offset - timeBuffer.length, timeBuffer, 0, timeBuffer.length);
				String timeStamp = new String(timeBuffer).substring(0, timeBuffer.length - 8) + "0000000000";
				long dateTime = new Long(timeStamp.substring(6, 17));
				logger.log(java.util.logging.Level.FINEST, timeStamp + " " + timeStamp.substring(6, 17) + " " + dateTime);
				sb.append(dateTime);
				//System.arraycopy(dataBuffer, offset - 4, sizeBuffer, 0, 4);
				//sb.append(" ? ").append(LogViewReader.parse2Int(sizeBuffer));
				deltaTime = lastDateTime == 0 ? 0 : (dateTime - lastDateTime) / 1000 - 217; // value 217 is a compromis manual selected
				sb.append(" - ").append(deltaTime);
				sb.append(" - ").append(sumTimeDelta += deltaTime);
				logger.log(java.util.logging.Level.FINER, sb.toString());
				lastDateTime = dateTime;

				recordSet.addTimeStep_ms(sumTimeDelta);

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}
			//			recordSet.setTimeStep_ms((double)sumTimeDelta/recordDataSize);
			//			log.log(Level.FINE, sumTimeDelta/recordDataSize + " " + sumTimeDelta);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		updateVisibilityStatus(recordSet, true);
		recordSet.syncScaleOfSyncableRecords();
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

		if (dataBuffer.length == this.getDataBlockSize() / 2) {
			// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance 8=SpannungZelle1 9=SpannungZelle2...
			points[0] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[21], (char) dataBuffer[22], (char) dataBuffer[23], (char) dataBuffer[24]), 16);
			points[1] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[25], (char) dataBuffer[26], (char) dataBuffer[27], (char) dataBuffer[28]), 16);
			points[2] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[29], (char) dataBuffer[30], (char) dataBuffer[31], (char) dataBuffer[32]), 16);
			points[3] = Double.valueOf(points[0] * points[1] / 1000.0).intValue(); // power U*I [W]
			points[4] = Double.valueOf(points[0] * points[2] / 1000.0).intValue(); // energy U*C [Wh]
			points[5] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[33], (char) dataBuffer[34], (char) dataBuffer[35], (char) dataBuffer[36]), 16);
			String sign = String.format("%c%c", (char) dataBuffer[37], (char) dataBuffer[38]);
			if (sign != null && sign.length() > 0 && Integer.parseInt(sign) == 0) points[5] = -1 * points[5]; 
			points[6] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[11], (char) dataBuffer[12], (char) dataBuffer[13], (char) dataBuffer[14]), 16);
			points[7] = 0;

			// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle7
			for (int i = 0, j = 0; i < points.length - 8; ++i, j += 4) {
				points[i + 8] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[41 + j], (char) dataBuffer[42 + j], (char) dataBuffer[43 + j], (char) dataBuffer[44 + j]), 16);
				if (points[i + 8] > 0) {
					maxVotage = points[i + 8] > maxVotage ? points[i + 8] : maxVotage;
					minVotage = points[i + 8] < minVotage ? points[i + 8] : minVotage;
				}
			}
			//calculate balance on the fly
			points[7] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
		}
		else if (dataBuffer.length == this.getDataBlockSize()) {
			if(this.isLinkedMode(dataBuffer)) {
				// 0=Spannung 1=Spannung1 2=Spannung2 3=Strom 4=Strom1 5=Strom2 6=Ladung 7=Ladung1 8=Ladung2 9=Leistung 10=Leistung1 11=Leistung2 12=Energie 13=Energie1 14=Energie2 15=BatteryTemperature1 16=BatteryTemperature2 17=VersorgungsSpg1 18=VersorgungsSpg2 19=Balance 
				points[1] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[21], (char) dataBuffer[22], (char) dataBuffer[23], (char) dataBuffer[24]), 16);
				points[2] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[85], (char) dataBuffer[86], (char) dataBuffer[87], (char) dataBuffer[88]), 16);
				points[0] = points[1] + points[2];
				points[4] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[25], (char) dataBuffer[26], (char) dataBuffer[27], (char) dataBuffer[28]), 16);
				points[5] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[89], (char) dataBuffer[90], (char) dataBuffer[91], (char) dataBuffer[92]), 16);
				points[3] = points[4] + points[5];
				points[7] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[29], (char) dataBuffer[30], (char) dataBuffer[31], (char) dataBuffer[32]), 16);
				points[8] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[93], (char) dataBuffer[94], (char) dataBuffer[95], (char) dataBuffer[96]), 16);
				points[6] = points[7] + points[8];
				points[10] = Double.valueOf(points[1] * points[4] / 1000.0).intValue(); // power U*I [W]
				points[11] = Double.valueOf(points[2] * points[5] / 1000.0).intValue(); // power U*I [W]
				points[9] = points[10] + points[11];
				points[13] = Double.valueOf(points[0] * points[2] / 1000.0).intValue(); // energy U*C [Wh]
				points[14] = Double.valueOf(points[0] * points[2] / 1000.0).intValue(); // energy U*C [Wh]
				points[12] = points[13] + points[14];
				points[15] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[33], (char) dataBuffer[34], (char) dataBuffer[35], (char) dataBuffer[36]), 16);
				String sign = String.format("%c%c", (char) dataBuffer[37], (char) dataBuffer[38]);
				if (sign != null && sign.length() > 0 && Integer.parseInt(sign) == 0) points[15] = -1 * points[15]; 
				points[16] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[97], (char) dataBuffer[98], (char) dataBuffer[99], (char) dataBuffer[100]), 16);
				sign = String.format("%c%c", (char) dataBuffer[37], (char) dataBuffer[38]);
				if (sign != null && sign.length() > 0 && Integer.parseInt(sign) == 0) points[16] = -1 * points[16]; 
				points[17] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[11], (char) dataBuffer[12], (char) dataBuffer[13], (char) dataBuffer[14]), 16);
				points[18] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[75], (char) dataBuffer[76], (char) dataBuffer[77], (char) dataBuffer[78]), 16);
				points[19] = 0;

				// 20=SpannungZelle1 21=SpannungZelle2 22=SpannungZelle3 23=SpannungZelle4 24=SpannungZelle5 25=SpannungZelle6 26=SpannungZelle7 
				for (int i = 0, j = 0; i < 7; ++i, j += 4) {
					points[i + 20] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[41 + j], (char) dataBuffer[42 + j], (char) dataBuffer[43 + j], (char) dataBuffer[44 + j]), 16);
					if (points[i + 20] > 0) {
						maxVotage = points[i + 20] > maxVotage ? points[i + 20] : maxVotage;
						minVotage = points[i + 20] < minVotage ? points[i + 20] : minVotage;
					}
				}
				// 27=SpannungZelle8 28=SpannungZelle9 29=SpannungZelle10 30=SpannungZelle11 31=SpannungZelle12 32=SpannungZelle13 33=SpannungZelle14
				for (int i = 0, j = 0; i < 7; ++i, j += 4) {
					points[i + 27] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[105 + j], (char) dataBuffer[106 + j], (char) dataBuffer[107 + j], (char) dataBuffer[108 + j]), 16);
					if (points[i + 27] > 0) {
						maxVotage = points[i + 27] > maxVotage ? points[i + 27] : maxVotage;
						minVotage = points[i + 27] < minVotage ? points[i + 27] : minVotage;
					}
				}
				//calculate balance on the fly
				points[19] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
			}
			else {
				// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance 
				points[0] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[21], (char) dataBuffer[22], (char) dataBuffer[23], (char) dataBuffer[24]), 16);
				points[1] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[25], (char) dataBuffer[26], (char) dataBuffer[27], (char) dataBuffer[28]), 16);
				points[2] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[29], (char) dataBuffer[30], (char) dataBuffer[31], (char) dataBuffer[32]), 16);
				points[3] = Double.valueOf(points[0] * points[1] / 1000.0).intValue(); // power U*I [W]
				points[4] = Double.valueOf(points[0] * points[2] / 1000.0).intValue(); // energy U*C [Wh]
				points[5] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[33], (char) dataBuffer[34], (char) dataBuffer[35], (char) dataBuffer[36]), 16);
				String sign = String.format("%c%c", (char) dataBuffer[37], (char) dataBuffer[38]);
				if (sign != null && sign.length() > 0 && Integer.parseInt(sign) == 0) points[5] = -1 * points[5]; 
				points[6] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[11], (char) dataBuffer[12], (char) dataBuffer[13], (char) dataBuffer[14]), 16);
				points[7] = 0;
	
				// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle7 
				for (int i = 0, j = 0; i < points.length - 8; ++i, j += 4) {
					points[i + 8] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[41 + j], (char) dataBuffer[42 + j], (char) dataBuffer[43 + j], (char) dataBuffer[44 + j]), 16);
					if (points[i + 8] > 0) {
						maxVotage = points[i + 8] > maxVotage ? points[i + 8] : maxVotage;
						minVotage = points[i + 8] < minVotage ? points[i + 8] : minVotage;
					}
				}
				//calculate balance on the fly
				points[7] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
			}
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
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.size()];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		if (recordSet.getChannelConfigNumber() == 3) {
			for (int i = 0; i < recordDataSize; i++) {			
				log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i*dataBufferSize);
				System.arraycopy(dataBuffer, i*dataBufferSize, convertBuffer, 0, dataBufferSize);
				// 0=Spannung 1=Spannung1 2=Spannung2 3=Strom 4=Strom1 5=Strom2 6=Ladung 7=Ladung1 8=Ladung2 9=Leistung 10=Leistung1 11=Leistung2 12=Energie 13=Energie1 14=Energie2 15=BatteryTemperature1 16=BatteryTemperature2 17=VersorgungsSpg1 18=Balance 
				points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
				points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
				points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff) << 0));
				points[3] = (((convertBuffer[12]&0xff) << 24) + ((convertBuffer[13]&0xff) << 16) + ((convertBuffer[14]&0xff) << 8) + ((convertBuffer[15]&0xff) << 0));
				points[4] = (((convertBuffer[16]&0xff) << 24) + ((convertBuffer[17]&0xff) << 16) + ((convertBuffer[18]&0xff) << 8) + ((convertBuffer[19]&0xff) << 0));
				points[5] = (((convertBuffer[20]&0xff) << 24) + ((convertBuffer[21]&0xff) << 16) + ((convertBuffer[22]&0xff) << 8) + ((convertBuffer[23]&0xff) << 0));
				points[6] = (((convertBuffer[24]&0xff) << 24) + ((convertBuffer[25]&0xff) << 16) + ((convertBuffer[26]&0xff) << 8) + ((convertBuffer[27]&0xff) << 0));
				points[7] = (((convertBuffer[28]&0xff) << 24) + ((convertBuffer[29]&0xff) << 16) + ((convertBuffer[30]&0xff) << 8) + ((convertBuffer[31]&0xff) << 0));
				points[8] = (((convertBuffer[32]&0xff) << 24) + ((convertBuffer[33]&0xff) << 16) + ((convertBuffer[34]&0xff) << 8) + ((convertBuffer[35]&0xff) << 0));
				points[9] =  Double.valueOf(points[0] / 1000.0 * points[3]).intValue(); 						// power U*I [W]
				points[10] = Double.valueOf(points[1] / 1000.0 * points[4]).intValue(); 						// power U*I [W]
				points[11] = Double.valueOf(points[2] / 1000.0 * points[5]).intValue(); 						// power U*I [W]
				points[12] = Double.valueOf(points[0] / 1000.0 * points[6]).intValue();							// energy U*C [Wh]
				points[13] = Double.valueOf(points[0] / 1000.0 * points[7]).intValue();							// energy U*C [Wh]
				points[14] = Double.valueOf(points[0] / 1000.0 * points[8]).intValue();							// energy U*C [Wh]
				points[15] = (((convertBuffer[36]&0xff) << 24) + ((convertBuffer[37]&0xff) << 16) + ((convertBuffer[38]&0xff) << 8) + ((convertBuffer[39]&0xff) << 0));
				points[16] = (((convertBuffer[40]&0xff) << 24) + ((convertBuffer[41]&0xff) << 16) + ((convertBuffer[42]&0xff) << 8) + ((convertBuffer[43]&0xff) << 0));
				points[17] = (((convertBuffer[44]&0xff) << 24) + ((convertBuffer[45]&0xff) << 16) + ((convertBuffer[46]&0xff) << 8) + ((convertBuffer[47]&0xff) << 0));
				points[18] = (((convertBuffer[48]&0xff) << 24) + ((convertBuffer[49]&0xff) << 16) + ((convertBuffer[50]&0xff) << 8) + ((convertBuffer[51]&0xff) << 0));
				points[19] = 0;
	
				// 19=SpannungZelle1 20=SpannungZelle2 21=SpannungZelle3 22=SpannungZelle4 23=SpannungZelle5 24=SpannungZelle6 25=SpannungZelle7 
				// 26=SpannungZelle8 27=SpannungZelle9 28=SpannungZelle10 29=SpannungZelle11 30=SpannungZelle12 31=SpannungZelle13 32=SpannungZelle14
				for (int j=0, k=0; j<points.length - 20; ++j, k+=GDE.SIZE_BYTES_INTEGER) {
					points[j + 20] = (((convertBuffer[k + 52] & 0xff) << 24) + ((convertBuffer[k + 53] & 0xff) << 16) + ((convertBuffer[k + 54] & 0xff) << 8) + ((convertBuffer[k + 55] & 0xff) << 0));
					if (points[j + 20] > 0) {
						maxVotage = points[j + 20] > maxVotage ? points[j + 20] : maxVotage;
						minVotage = points[j + 20] < minVotage ? points[j + 20] : minVotage;
					}
				}
				//calculate balance on the fly
				points[19] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
				
				recordSet.addPoints(points);
				
				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*2500)/recordDataSize), sThreadId);
			}
		}
		else {
			for (int i = 0; i < recordDataSize; i++) {			
				log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i*dataBufferSize);
				System.arraycopy(dataBuffer, i*dataBufferSize, convertBuffer, 0, dataBufferSize);
				// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance 
				// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle6 15=SpannungZelle7
				points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
				points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
				points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff) << 0));
				points[3] = Double.valueOf(points[0] / 1000.0 * points[1]).intValue(); 							// power U*I [W]
				points[4] = Double.valueOf(points[0] / 1000.0 * points[2]).intValue();							// energy U*C [Wh]
				points[5] = (((convertBuffer[12]&0xff) << 24) + ((convertBuffer[13]&0xff) << 16) + ((convertBuffer[14]&0xff) << 8) + ((convertBuffer[15]&0xff) << 0));
				points[6] = (((convertBuffer[16]&0xff) << 24) + ((convertBuffer[17]&0xff) << 16) + ((convertBuffer[18]&0xff) << 8) + ((convertBuffer[19]&0xff) << 0));
				points[7] = 0;
	
				// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
				for (int j=0, k=0; j<points.length - 8; ++j, k+=GDE.SIZE_BYTES_INTEGER) {
					points[j + 8] = (((convertBuffer[k + 20] & 0xff) << 24) + ((convertBuffer[k + 21] & 0xff) << 16) + ((convertBuffer[k + 22] & 0xff) << 8) + ((convertBuffer[k + 23] & 0xff) << 0));
					if (points[j + 8] > 0) {
						maxVotage = points[j + 8] > maxVotage ? points[j + 8] : maxVotage;
						minVotage = points[j + 8] < minVotage ? points[j + 8] : minVotage;
					}
				}
				//calculate balance on the fly
				points[7] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
				
				recordSet.addPoints(points);
				
				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*2500)/recordDataSize), sThreadId);
			}
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		updateVisibilityStatus(recordSet, true);
		recordSet.syncScaleOfSyncableRecords();
	}
	
	/**
	 * query if the target measurement reference ordinal used by the given desktop type
	 * @return the target measurement reference ordinal, -1 if reference ordinal not set
	 */
	@Override
	public int getDesktopTargetReferenceOrdinal(DesktopPropertyTypes desktopPropertyType) {
		DesktopPropertyType property = this.getDesktopProperty(desktopPropertyType);
		if(channels.getActiveChannelNumber() == 3) {
			return property != null ? property.getTargetReferenceOrdinal() + 12 : -1; 
		}
		return property != null ? property.getTargetReferenceOrdinal() : -1; 
	}

	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public int[] getCellVoltageOrdinals() {
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance 
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle6 15=SpannungZelle7
		// 16=SpannungZelle8 17=SpannungZelle9 18=SpannungZelle10 19=SpannungZelle11 20=SpannungZelle12 21=SpannungZelle13 22=SpannungZelle14
		
		// 0=Spannung 1=Spannung1 2=Spannung 3=Strom 4=Strom1 5=Strom2 6=Ladung 7=Ladung1 8=Ladung2 9=Leistung 10=Leistung1 11=Leistung2 12=Energie 13=Energie1 14=Energie2 15=BatteryTemperature1 16=BatteryTemperature2 17=VersorgungsSpg1 18=VersorgungsSpg2 19=Balance 
		// 20=SpannungZelle1 21=SpannungZelle2 22=SpannungZelle3 23=SpannungZelle4 24=SpannungZelle5 25=SpannungZelle6 26=SpannungZelle7 
		// 27=SpannungZelle8 28=SpannungZelle9 29=SpannungZelle10 30=SpannungZelle11 31=SpannungZelle12 32=SpannungZelle13 33=SpannungZelle14
		return new int[] {0, channels.getActiveChannelNumber() == 3 ? 6 : 2};
	}
}
