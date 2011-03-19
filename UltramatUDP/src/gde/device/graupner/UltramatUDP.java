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
import gde.device.DeviceConfiguration;
import gde.exception.DataInconsitsentException;
import gde.io.LogViewReader;
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
		this.DISCHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212), Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2214), Messages.getString(MessageIds.GDE_MSGT2223), Messages.getString(MessageIds.GDE_MSGT2224), Messages.getString(MessageIds.GDE_MSGT2221), Messages.getString(MessageIds.GDE_MSGT2222)};

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
		this.DISCHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212), Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2214), Messages.getString(MessageIds.GDE_MSGT2223), Messages.getString(MessageIds.GDE_MSGT2224), Messages.getString(MessageIds.GDE_MSGT2221), Messages.getString(MessageIds.GDE_MSGT2222)};

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
		int[] points = new int[this.getNumberOfMeasurements(1)];
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
			// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance 
			points[0] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[21], (char) dataBuffer[22], (char) dataBuffer[23], (char) dataBuffer[24]), 16);
			points[1] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[25], (char) dataBuffer[26], (char) dataBuffer[27], (char) dataBuffer[28]), 16);
			points[2] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[29], (char) dataBuffer[30], (char) dataBuffer[31], (char) dataBuffer[32]), 16);
			points[3] = Double.valueOf(points[0] * points[1] / 1000.0).intValue(); // power U*I [W]
			points[4] = Double.valueOf(points[0] * points[2] / 1000.0).intValue(); // energy U*C [Wh]
			points[5] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[33], (char) dataBuffer[34], (char) dataBuffer[35], (char) dataBuffer[36]), 16);
			points[6] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[11], (char) dataBuffer[12], (char) dataBuffer[13], (char) dataBuffer[14]), 16);
			points[7] = 0;

			// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle6 15=SpannungZelle7
			for (int i = 0, j = 0; i < points.length - 8; ++i, j += 4) {
				points[i + 8] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[41 + j], (char) dataBuffer[42 + j], (char) dataBuffer[43 + j], (char) dataBuffer[44 + j]), 16);
				if (points[i + 8] > 0) {
					maxVotage = points[i + 8] > maxVotage ? points[i + 8] : maxVotage;
					minVotage = points[i + 8] < minVotage ? points[i + 8] : minVotage;
				}
			}
			// 16=SpannungZelle8 17=SpannungZelle9 18=SpannungZelle10 19=SpannungZelle11 20=SpannungZelle12 21=SpannungZelle13 22=SpannungZelle14
			for (int i = 0, j = 0; i < points.length - 15; ++i, j += 4) {
				points[i + 15] = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[105 + j], (char) dataBuffer[106 + j], (char) dataBuffer[107 + j], (char) dataBuffer[108 + j]), 16);
				if (points[i + 15] > 0) {
					maxVotage = points[i + 15] > maxVotage ? points[i + 15] : maxVotage;
					minVotage = points[i + 15] < minVotage ? points[i + 15] : minVotage;
				}
			}
			//calculate balance on the fly
			points[7] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
		}
		return points;
	}
}
