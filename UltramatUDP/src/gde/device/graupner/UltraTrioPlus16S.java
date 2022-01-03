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

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.exception.DataInconsitsentException;
import gde.log.Level;
import gde.messages.Messages;

import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

/**
 * Graupner Ultramat Trio Plus 16 S
 * @author Winfried Brügmann
 */
public class UltraTrioPlus16S extends UltraTrioPlus14 {
	final static Logger														logg															= Logger.getLogger(UltraTrioPlus16S.class.getName());


	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public UltraTrioPlus16S(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2200), Messages.getString(MessageIds.GDE_MSGT2201), Messages.getString(MessageIds.GDE_MSGT2202),
				Messages.getString(MessageIds.GDE_MSGT2203), Messages.getString(MessageIds.GDE_MSGI2206), Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2207),
				Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2206), 
				Messages.getString(MessageIds.GDE_MSGT2222), Messages.getString(MessageIds.GDE_MSGT2222)};

		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		this.dialog = null;
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public UltraTrioPlus16S(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2200), Messages.getString(MessageIds.GDE_MSGT2201), Messages.getString(MessageIds.GDE_MSGT2202),
				Messages.getString(MessageIds.GDE_MSGT2203), Messages.getString(MessageIds.GDE_MSGI2206), Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2207),
				Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2206), 
				Messages.getString(MessageIds.GDE_MSGT2222), Messages.getString(MessageIds.GDE_MSGT2222)};

		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		this.dialog = null;
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	@Override
	public int getLovDataByteSize() {
		return 126;
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
	@Override
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int deviceDataBufferSize = Math.abs(this.getDataBlockSize(InputTypes.SERIAL_IO));; 
		int[] points = new int[this.getNumberOfMeasurements(recordSet.getChannelConfigNumber())];
		int offset = 4;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();
		int outputChannel = recordSet.getChannelConfigNumber();

		if (dataBuffer[offset] == 0x0C) {
			byte[] convertBuffer = new byte[deviceDataBufferSize];
			if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

			for (int i = 0; i < recordDataSize; i++) {
				if (outputChannel == 1)
					System.arraycopy(dataBuffer, offset + i * lovDataSize, convertBuffer, 0, deviceDataBufferSize);
				else if (outputChannel == 2) {
					System.arraycopy(dataBuffer, offset + i * lovDataSize, convertBuffer, 0, 9); //copy until input voltage
					System.arraycopy(dataBuffer, 57+offset + i * lovDataSize, convertBuffer, 9, 2); //copy operation mode
					convertBuffer[11] = convertBuffer[12] = 48; //blank out cycle number, channel 2 does not support cycles
					System.arraycopy(dataBuffer, 59+offset + i * lovDataSize, convertBuffer, 13, 24);
				}
				else if (outputChannel == 3) {
					System.arraycopy(dataBuffer, offset + i * lovDataSize, convertBuffer, 0, 9); //copy until input voltage
					System.arraycopy(dataBuffer, 83+offset + i * lovDataSize, convertBuffer, 9, 2); //copy operation mode
					convertBuffer[11] = convertBuffer[12] = 48; //blank out cycle number, channel 2 does not support cycles
					System.arraycopy(dataBuffer, 85+offset + i * lovDataSize, convertBuffer, 13, 24);
				}
				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
				recordSet.addPoints(convertDataBytes(points, convertBuffer));
			}
			
			recordSet.setTimeStep_ms(this.getAverageTimeStep_ms() != null ? this.getAverageTimeStep_ms() : 1000);
		}

		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		updateVisibilityStatus(recordSet, true);
		recordSet.syncScaleOfSyncableRecords();
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
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		if (recordSet.getChannelConfigNumber() == 1) {
			for (int i = 0; i < recordDataSize; i++) {
				int maxVotage = Integer.MIN_VALUE;
				int minVotage = Integer.MAX_VALUE;
				logg.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize); //$NON-NLS-1$
				System.arraycopy(dataBuffer, i * dataBufferSize, convertBuffer, 0, dataBufferSize);
				// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=VersorgungsSpg 6=Balance 7=SpannungZelle1 8=SpannungZelle2....
				// 7=SpannungZelle1 8=SpannungZelle2 9=SpannungZelle3 10=SpannungZelle4 11=SpannungZelle5 12=SpannungZelle6 
				points[0] = (((convertBuffer[0] & 0xff) << 24) + ((convertBuffer[1] & 0xff) << 16) + ((convertBuffer[2] & 0xff) << 8) + ((convertBuffer[3] & 0xff) << 0));
				points[1] = (((convertBuffer[4] & 0xff) << 24) + ((convertBuffer[5] & 0xff) << 16) + ((convertBuffer[6] & 0xff) << 8) + ((convertBuffer[7] & 0xff) << 0));
				points[2] = (((convertBuffer[8] & 0xff) << 24) + ((convertBuffer[9] & 0xff) << 16) + ((convertBuffer[10] & 0xff) << 8) + ((convertBuffer[11] & 0xff) << 0));
				points[3] = Double.valueOf(points[0] / 1000.0 * points[1]).intValue(); // power U*I [W]
				points[4] = Double.valueOf(points[0] / 1000.0 * points[2]).intValue(); // energy U*C [Wh]
				points[5] = (((convertBuffer[12] & 0xff) << 24) + ((convertBuffer[13] & 0xff) << 16) + ((convertBuffer[14] & 0xff) << 8) + ((convertBuffer[15] & 0xff) << 0));
				points[6] = 0;

				// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
				for (int j = 0, k = 0; j < 6; ++j, k += GDE.SIZE_BYTES_INTEGER) {
					points[j + 7] = (((convertBuffer[k + 16] & 0xff) << 24) + ((convertBuffer[k + 17] & 0xff) << 16) + ((convertBuffer[k + 18] & 0xff) << 8) + ((convertBuffer[k + 19] & 0xff) << 0));
					if (points[j + 7] > 0) {
						maxVotage = points[j + 7] > maxVotage ? points[j + 7] : maxVotage;
						minVotage = points[j + 7] < minVotage ? points[j + 7] : minVotage;
					}
				}
				//calculate balance on the fly
				points[6] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

				recordSet.addPoints(points);

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
			}
		}
		else {
			for (int i = 0; i < recordDataSize; i++) {
				int maxVotage = Integer.MIN_VALUE;
				int minVotage = Integer.MAX_VALUE;
				logg.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize); //$NON-NLS-1$
				System.arraycopy(dataBuffer, i * dataBufferSize, convertBuffer, 0, dataBufferSize);
				// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=VersorgungsSpg 6=Balance 
				// 7=SpannungZelle1 8=SpannungZelle2 9=SpannungZelle3
				points[0] = (((convertBuffer[0] & 0xff) << 24) + ((convertBuffer[1] & 0xff) << 16) + ((convertBuffer[2] & 0xff) << 8) + ((convertBuffer[3] & 0xff) << 0));
				points[1] = (((convertBuffer[4] & 0xff) << 24) + ((convertBuffer[5] & 0xff) << 16) + ((convertBuffer[6] & 0xff) << 8) + ((convertBuffer[7] & 0xff) << 0));
				points[2] = (((convertBuffer[8] & 0xff) << 24) + ((convertBuffer[9] & 0xff) << 16) + ((convertBuffer[10] & 0xff) << 8) + ((convertBuffer[11] & 0xff) << 0));
				points[3] = Double.valueOf(points[0] / 1000.0 * points[1]).intValue(); // power U*I [W]
				points[4] = Double.valueOf(points[0] / 1000.0 * points[2]).intValue(); // energy U*C [Wh]
				points[5] = (((convertBuffer[12] & 0xff) << 24) + ((convertBuffer[13] & 0xff) << 16) + ((convertBuffer[14] & 0xff) << 8) + ((convertBuffer[15] & 0xff) << 0));
				points[6] = 0;

				// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3
				for (int j = 0, k = 0; j < 3; ++j, k += GDE.SIZE_BYTES_INTEGER) {
					points[j + 7] = (((convertBuffer[k + 16] & 0xff) << 24) + ((convertBuffer[k + 17] & 0xff) << 16) + ((convertBuffer[k + 18] & 0xff) << 8) + ((convertBuffer[k + 19] & 0xff) << 0));
					if (points[j + 7] > 0) {
						maxVotage = points[j + 7] > maxVotage ? points[j + 7] : maxVotage;
						minVotage = points[j + 7] < minVotage ? points[j + 7] : minVotage;
					}
				}
				//calculate balance on the fly
				points[6] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

				recordSet.addPoints(points);

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
			}
		}
	
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * query the device identifier to differentiate between different device implementations
	 * @return 	-1=Ultramat16 1=Ultramat50, 2=Ultramat40, 3=UltramatTrio14, 4=Ultramat45, 5=Ultramat60, 6=Ultramat16S
	 */
	@Override
	public GraupnerDeviceType getDeviceTypeIdentifier() {
		return GraupnerDeviceType.UltraTrioPlus16S;
	}

	/**
	 * find best match of memory name with object key and select, if no match no object key will be changed
	 * @param batteryMemoryName
	 * @return
	 */
	@Override
	public void matchBatteryMemory2ObjectKey(String batteryMemoryName) {
		//no memory can be matched here
	}

	/**
	 * check if one of the outlet channels are in processing mode
	 * @param outletNum 1
	 * @param dataBuffer
	 * @return true if channel 1 is active 
	 */
	@Override
	public boolean isProcessing(int outletNum, byte[] dataBuffer) {
		if (outletNum == 1) {
			try {
				int operationMode1 = getProcessingMode(dataBuffer);
				if (log.isLoggable(java.util.logging.Level.FINE)) {
					log.log(java.util.logging.Level.FINE,	"operationMode1 = " + operationMode1);
				}
				//0 = no processing, 1 = charge, 2 = discharge, 3 = pause, 4 = current operation finished, 5 = error
				return operationMode1 > 0 && operationMode1 < 4; 
			}
			catch (NumberFormatException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
				return false;
			}
		}
		else if (outletNum == 2) {
			try {
				int operationMode2 = Integer.parseInt(String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[57], (char) dataBuffer[58]), 16);
				if (log.isLoggable(java.util.logging.Level.FINE)) {
					log.log(java.util.logging.Level.FINE,	"operationMode1 = " + operationMode2);
				}
				//0 = no processing, 1 = charge, 2 = discharge, 3 = pause, 4 = current operation finished, 5 = error
				return operationMode2 > 0 && operationMode2 < 4; 
			}
			catch (NumberFormatException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
				return false;
			}
		}
		else if (outletNum == 3) {
			try {
				int operationMode3 = Integer.parseInt(String.format(DeviceCommPort.FORMAT_2_CHAR, (char) dataBuffer[83], (char) dataBuffer[84]), 16);
				if (log.isLoggable(java.util.logging.Level.FINE)) {
					log.log(java.util.logging.Level.FINE,	"operationMode1 = " + operationMode3);
				}
				//0 = no processing, 1 = charge, 2 = discharge, 3 = pause, 4 = current operation finished, 5 = error
				return operationMode3 > 0 && operationMode3 < 4; 
			}
			catch (NumberFormatException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
				return false;
			}
		}
		return false;
	}
}
