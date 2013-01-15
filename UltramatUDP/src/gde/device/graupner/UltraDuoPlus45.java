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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.comm.DeviceSerialPortImpl;
import gde.config.Settings;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DesktopPropertyType;
import gde.device.DesktopPropertyTypes;
import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.exception.DataInconsitsentException;
import gde.log.Level;
import gde.messages.Messages;

import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;

/**
 * Graupner Ultra Duo Plus base class which extends the Ultramat base class
 * @author Winfried Brügmann
 */
public class UltraDuoPlus45 extends Ultramat {
	final static Logger	logger	= Logger.getLogger(UltraDuoPlus45.class.getName());

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public UltraDuoPlus45(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2200), Messages.getString(MessageIds.GDE_MSGT2201), Messages.getString(MessageIds.GDE_MSGT2202),
				Messages.getString(MessageIds.GDE_MSGT2203), Messages.getString(MessageIds.GDE_MSGT2204), Messages.getString(MessageIds.GDE_MSGT2205), Messages.getString(MessageIds.GDE_MSGT2206),
				Messages.getString(MessageIds.GDE_MSGT2207), Messages.getString(MessageIds.GDE_MSGT2208), Messages.getString(MessageIds.GDE_MSGT2209) };
		this.CHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212),
				Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2214), Messages.getString(MessageIds.GDE_MSGT2215), Messages.getString(MessageIds.GDE_MSGT2216),
				Messages.getString(MessageIds.GDE_MSGT2217), Messages.getString(MessageIds.GDE_MSGT2218), Messages.getString(MessageIds.GDE_MSGT2219), Messages.getString(MessageIds.GDE_MSGT2220),
				Messages.getString(MessageIds.GDE_MSGT2221), Messages.getString(MessageIds.GDE_MSGT2222) };
		this.DISCHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212),
				Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2223), Messages.getString(MessageIds.GDE_MSGT2224), Messages.getString(MessageIds.GDE_MSGT2220),
				Messages.getString(MessageIds.GDE_MSGT2222) };
		this.DELAY_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2225), Messages.getString(MessageIds.GDE_MSGT2226), Messages.getString(MessageIds.GDE_MSGT2227),
				Messages.getString(MessageIds.GDE_MSGT2228) };
		this.CURRENT_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2229), Messages.getString(MessageIds.GDE_MSGT2230), Messages.getString(MessageIds.GDE_MSGT2231),
				Messages.getString(MessageIds.GDE_MSGT2232), Messages.getString(MessageIds.GDE_MSGT2233), Messages.getString(MessageIds.GDE_MSGT2234), Messages.getString(MessageIds.GDE_MSGT2235),
				Messages.getString(MessageIds.GDE_MSGT2236), Messages.getString(MessageIds.GDE_MSGT2237) };

		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public UltraDuoPlus45(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2200), Messages.getString(MessageIds.GDE_MSGT2201), Messages.getString(MessageIds.GDE_MSGT2202),
				Messages.getString(MessageIds.GDE_MSGT2203), Messages.getString(MessageIds.GDE_MSGT2204), Messages.getString(MessageIds.GDE_MSGT2205), Messages.getString(MessageIds.GDE_MSGT2206),
				Messages.getString(MessageIds.GDE_MSGT2207), Messages.getString(MessageIds.GDE_MSGT2208), Messages.getString(MessageIds.GDE_MSGT2209) };
		this.CHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212),
				Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2214), Messages.getString(MessageIds.GDE_MSGT2215), Messages.getString(MessageIds.GDE_MSGT2216),
				Messages.getString(MessageIds.GDE_MSGT2217), Messages.getString(MessageIds.GDE_MSGT2218), Messages.getString(MessageIds.GDE_MSGT2219), Messages.getString(MessageIds.GDE_MSGT2220),
				Messages.getString(MessageIds.GDE_MSGT2221), Messages.getString(MessageIds.GDE_MSGT2222) };
		this.DISCHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212),
				Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2223), Messages.getString(MessageIds.GDE_MSGT2224), Messages.getString(MessageIds.GDE_MSGT2220),
				Messages.getString(MessageIds.GDE_MSGT2222) };
		this.DELAY_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2225), Messages.getString(MessageIds.GDE_MSGT2226), Messages.getString(MessageIds.GDE_MSGT2227),
				Messages.getString(MessageIds.GDE_MSGT2228) };
		this.CURRENT_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2229), Messages.getString(MessageIds.GDE_MSGT2230), Messages.getString(MessageIds.GDE_MSGT2231),
				Messages.getString(MessageIds.GDE_MSGT2232), Messages.getString(MessageIds.GDE_MSGT2233), Messages.getString(MessageIds.GDE_MSGT2234), Messages.getString(MessageIds.GDE_MSGT2235),
				Messages.getString(MessageIds.GDE_MSGT2236), Messages.getString(MessageIds.GDE_MSGT2237) };

		this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	@Override
	public int getLovDataByteSize() {
		return 158;
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
		int deviceDataBufferSize = Math.abs(this.getDataBlockSize(InputTypes.SERIAL_IO)); // const.
		int channel2Offset1 = 97;
		int channel2Offset2 = 105;
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
					System.arraycopy(dataBuffer, channel2Offset1 + offset + i * lovDataSize, convertBuffer, 11, 8); //sync to same value positions, only point array length is different
					convertBuffer[19] = convertBuffer[20] = 48; //blank out cycle number
					System.arraycopy(dataBuffer, channel2Offset2 + offset + i * lovDataSize, convertBuffer, 21, 41);
				}

				recordSet.addPoints(convertDataBytes(points, convertBuffer));

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}

			recordSet.setTimeStep_ms(this.getAverageTimeStep_ms() != null ? this.getAverageTimeStep_ms() : 1000); // no average time available, use a hard coded one
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

		try {
			// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance 
			points[0] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) dataBuffer[21], (char) dataBuffer[22], (char) dataBuffer[23], (char) dataBuffer[24]), 16);
			points[1] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) dataBuffer[25], (char) dataBuffer[26], (char) dataBuffer[27], (char) dataBuffer[28]), 16);
			points[2] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) dataBuffer[29], (char) dataBuffer[30], (char) dataBuffer[31], (char) dataBuffer[32]), 16);
			points[3] = Double.valueOf(points[0] * points[1] / 1000.0).intValue(); // power U*I [W]
			points[4] = Double.valueOf(points[0] * points[2] / 1000.0).intValue(); // energy U*C [Wh]
			points[5] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) dataBuffer[33], (char) dataBuffer[34], (char) dataBuffer[35], (char) dataBuffer[36]), 16);
			String sign = String.format(DeviceSerialPortImpl.FORMAT_2_CHAR, (char) dataBuffer[37], (char) dataBuffer[38]);
			if (sign != null && sign.length() > 0 && Integer.parseInt(sign) == 0) points[5] = -1 * points[5];
			points[6] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) dataBuffer[11], (char) dataBuffer[12], (char) dataBuffer[13], (char) dataBuffer[14]), 16);
			points[7] = 0;

			// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle7 
			for (int i = 0, j = 0; i < points.length - 8; ++i, j += 4) {
				points[i + 8] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) dataBuffer[41 + j], (char) dataBuffer[42 + j], (char) dataBuffer[43 + j], (char) dataBuffer[44 + j]),
						16);
				if (points[i + 8] > 0) {
					maxVotage = points[i + 8] > maxVotage ? points[i + 8] : maxVotage;
					minVotage = points[i + 8] < minVotage ? points[i + 8] : minVotage;
				}
			}
		}
		catch (NumberFormatException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		//calculate balance on the fly
		points[7] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

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
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			logger.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i * dataBufferSize, convertBuffer, 0, dataBufferSize);
			// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance 
			// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle6 15=SpannungZelle7
			points[0] = (((convertBuffer[0] & 0xff) << 24) + ((convertBuffer[1] & 0xff) << 16) + ((convertBuffer[2] & 0xff) << 8) + ((convertBuffer[3] & 0xff) << 0));
			points[1] = (((convertBuffer[4] & 0xff) << 24) + ((convertBuffer[5] & 0xff) << 16) + ((convertBuffer[6] & 0xff) << 8) + ((convertBuffer[7] & 0xff) << 0));
			points[2] = (((convertBuffer[8] & 0xff) << 24) + ((convertBuffer[9] & 0xff) << 16) + ((convertBuffer[10] & 0xff) << 8) + ((convertBuffer[11] & 0xff) << 0));
			points[3] = Double.valueOf(points[0] / 1000.0 * points[1]).intValue(); // power U*I [W]
			points[4] = Double.valueOf(points[0] / 1000.0 * points[2]).intValue(); // energy U*C [Wh]
			points[5] = (((convertBuffer[12] & 0xff) << 24) + ((convertBuffer[13] & 0xff) << 16) + ((convertBuffer[14] & 0xff) << 8) + ((convertBuffer[15] & 0xff) << 0));
			points[6] = (((convertBuffer[16] & 0xff) << 24) + ((convertBuffer[17] & 0xff) << 16) + ((convertBuffer[18] & 0xff) << 8) + ((convertBuffer[19] & 0xff) << 0));
			points[7] = 0;

			// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
			for (int j = 0, k = 0; j < points.length - 8; ++j, k += GDE.SIZE_BYTES_INTEGER) {
				points[j + 8] = (((convertBuffer[k + 20] & 0xff) << 24) + ((convertBuffer[k + 21] & 0xff) << 16) + ((convertBuffer[k + 22] & 0xff) << 8) + ((convertBuffer[k + 23] & 0xff) << 0));
				if (points[j + 8] > 0) {
					maxVotage = points[j + 8] > maxVotage ? points[j + 8] : maxVotage;
					minVotage = points[j + 8] < minVotage ? points[j + 8] : minVotage;
				}
			}
			//calculate balance on the fly
			points[7] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

			recordSet.addPoints(points);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
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

		// LINK
		// 0=Spannung 1=Spannung1 2=Spannung2 3=Strom 4=Strom1 5=Strom2 6=Ladung 7=Ladung1 8=Ladung2 9=Leistung 10=Leistung1 11=Leistung2 12=Energie 13=Energie1 14=Energie2 15=BatteryTemperature1 16=BatteryTemperature2 17=VersorgungsSpg1 18=Balance 
		// 19=SpannungZelle1 20=SpannungZelle2 21=SpannungZelle3 22=SpannungZelle4 23=SpannungZelle5 24=SpannungZelle6 25=SpannungZelle7 
		// 26=SpannungZelle8 27=SpannungZelle9 28=SpannungZelle10 29=SpannungZelle11 30=SpannungZelle12 31=SpannungZelle13 32=SpannungZelle14
		return new int[] { 0, this.channels.getActiveChannelNumber() == 3 ? 6 : 2 };
	}

	/**
	 * check and update visibility status of all records according the available device configuration
	 * this function must have only implementation code if the device implementation supports different configurations
	 * where some curves are hided for better overview 
	 * example: if device supports voltage, current and height and no sensors are connected to voltage and current
	 * it makes less sense to display voltage and current curves, if only height has measurement data
	 * at least an update of the graphics window should be included at the end of this method
	 */
	@Override
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {

		recordSet.setAllDisplayable();
		int numCells = recordSet.getChannelConfigNumber() == 1 ? 14 : 4;
		for (int i = recordSet.size() - numCells - 1; i < recordSet.size(); ++i) {
			Record record = recordSet.get(i);
			record.setDisplayable(record.getOrdinal() <= 5 || record.hasReasonableData());
			if (logger.isLoggable(Level.FINER))
				logger.log(Level.FINER, record.getName() + " setDisplayable=" + (record.getOrdinal() <= 5 || record.hasReasonableData())); //$NON-NLS-1$
		}

		if (logger.isLoggable(Level.FINE)) {
			for (Record record : recordSet.values()) {
				logger.log(Level.FINE, record.getName() + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
	}

	/**
	 * query the device identifier to differentiate between different device implementations
	 * @return 	-1=Ultramat16 1=Ultramat50, 2=Ultramat40, 3=UltramatTrio14, 4=Ultramat45, 5=Ultramat60, 6=Ultramat16S
	 */
	@Override
	public GraupnerDeviceType getDeviceTypeIdentifier() {
		return GraupnerDeviceType.UltraDuoPlus45;
	}

	/**
	 * check if one of the outlet channels are in processing mode
	 * @param outletNum 1 or 2
	 * @param dataBuffer
	 * @return true if channel 1 or 2 is active 
	 */
	@Override
	public boolean isProcessing(int outletNum, byte[] dataBuffer) {
		if (outletNum == 1) {
			String operationModeOut1 = String.format(DeviceSerialPortImpl.FORMAT_2_CHAR, (char) dataBuffer[15], (char) dataBuffer[16]);
			if (logger.isLoggable(Level.FINE)) {
				logger.log(Level.FINE,
						"operationModeOut1 = " + (operationModeOut1 != null && operationModeOut1.length() > 0 ? this.USAGE_MODE[Integer.parseInt(operationModeOut1, 16)] : operationModeOut1)); //$NON-NLS-1$
			}
			return operationModeOut1 != null && operationModeOut1.length() == 2 && !(operationModeOut1.equals(Ultramat.OPERATIONS_MODE_NONE) || operationModeOut1.equals(Ultramat.OPERATIONS_MODE_ERROR));
		}
		else if (outletNum == 2) {
			String operationModeOut2 = String.format(DeviceSerialPortImpl.FORMAT_2_CHAR, (char) dataBuffer[101], (char) dataBuffer[102]);
			if (logger.isLoggable(Level.FINE)) {
				logger.log(Level.FINE,
						"operationModeOut2 = " + (operationModeOut2 != null && operationModeOut2.length() > 0 ? this.USAGE_MODE[Integer.parseInt(operationModeOut2, 16)] : operationModeOut2)); //$NON-NLS-1$
			}
			return operationModeOut2 != null && operationModeOut2.length() == 2 && !(operationModeOut2.equals(Ultramat.OPERATIONS_MODE_NONE) || operationModeOut2.equals(Ultramat.OPERATIONS_MODE_ERROR));
		}
		else
			return false;
	}

	/**
	 * query the processing mode, main modes are charge/discharge, make sure the data buffer contains at index 15,16 the processing modes
	 * @param dataBuffer 
	 * @return 0 = no processing, 1 = charge, 2 = discharge, 3 = delay, 4 = pause, 5 = current operation finished, 6 = error, 7 = balancer, 8 = tire heater, 9 = motor
	 */
	@Override
	public int getProcessingMode(byte[] dataBuffer) {
		String operationMode = String.format(DeviceSerialPortImpl.FORMAT_2_CHAR, (char) dataBuffer[15], (char) dataBuffer[16]);
		return operationMode != null && operationMode.length() > 0 ? Integer.parseInt(operationMode, 16) : 0;
	}

	/**
	 * query the charge mode, main modes are automatic/normal/CVCC, make sure the data buffer contains at index 15,16 the processing modes, type at index 17,18
	 * @param dataBuffer
	 * @return string of charge mode
	 */
	@Override
	public String getProcessingType(byte[] dataBuffer) {
		String type = GDE.STRING_EMPTY;
		String operationMode = String.format(DeviceSerialPortImpl.FORMAT_2_CHAR, (char) dataBuffer[15], (char) dataBuffer[16]);
		int opMode = operationMode != null && operationMode.length() > 0 ? Integer.parseInt(operationMode, 16) : 0;
		String operationType = String.format(DeviceSerialPortImpl.FORMAT_2_CHAR, (char) dataBuffer[17], (char) dataBuffer[18]);
		switch (opMode) {
		case 1: //charge
			type = operationType != null && operationType.length() > 0 ? this.CHARGE_MODE[Integer.parseInt(operationType, 16)] : GDE.STRING_EMPTY;
			break;
		case 2: //discharge
			type = operationType != null && operationType.length() > 0 ? this.DISCHARGE_MODE[Integer.parseInt(operationType, 16)] : GDE.STRING_EMPTY;
			break;
		case 3: //delay
			type = operationType != null && operationType.length() > 0 ? this.DELAY_MODE[Integer.parseInt(operationType, 16)] : GDE.STRING_EMPTY;
			break;
		case 5: //last active
			type = operationType != null && operationType.length() > 0 ? this.CURRENT_MODE[Integer.parseInt(operationType, 16)] : GDE.STRING_EMPTY;
			break;
		case 6: //error
			this.application.setStatusMessage(Messages.getString("GDE_MSGT22" + String.format("%02d", Integer.parseInt(operationType, 16))), SWT.COLOR_RED); //$NON-NLS-1$
			break;
		}
		return type;
	}

	/**
	 * query the battery memory number of the given outlet channel
	 * @param outletNum
	 * @param dataBuffer
	 * @return
	 */
	@Override
	public int getBatteryMemoryNumber(int outletNum, byte[] dataBuffer) {
		int memoryNumber = 0;
		if (outletNum == 1) {
			try {
				String batteryMemoryNumber = String.format(DeviceSerialPortImpl.FORMAT_2_CHAR, (char) dataBuffer[5], (char) dataBuffer[6]);
				memoryNumber = Integer.parseInt(batteryMemoryNumber, 16);
			}
			catch (NumberFormatException e) {
				// ignore and return 0
			}
		}
		return memoryNumber;
	}

	/**
	 * query the cycle number of the given outlet channel
	 * @param outletNum
	 * @param dataBuffer
	 * @return
	 */
	@Override
	public int getCycleNumber(int outletNum, byte[] dataBuffer) {
		String cycleNumber = String.format(DeviceSerialPortImpl.FORMAT_2_CHAR, (char) dataBuffer[19], (char) dataBuffer[20]);
		if (outletNum == 2) {
			cycleNumber = "0";
		}
		return Integer.parseInt(cycleNumber, 16);
	}

	/**
	 * set the temperature unit right after creating a record set according to the used oultet channel
	 * @param channelNumber
	 * @param recordSet
	 * @param dataBuffer
	 */
	@Override
	public void setTemperatureUnit(int channelNumber, RecordSet recordSet, byte[] dataBuffer) {
		String unit = String.format(DeviceSerialPortImpl.FORMAT_2_CHAR, (char) dataBuffer[39], (char) dataBuffer[40]);
		if (Integer.parseInt(unit) == 0)
			this.setMeasurementUnit(recordSet.getChannelConfigNumber(), 5, DeviceConfiguration.UNIT_DEGREE_CELSIUS);
		else if (Integer.parseInt(unit) == 1) this.setMeasurementUnit(recordSet.getChannelConfigNumber(), 5, DeviceConfiguration.UNIT_DEGREE_FAHRENHEIT);
	}

	/**
	 * This function allows to register a custom CTabItem to the main application tab folder to display device 
	 * specific curve calculated from point combinations or other specific dialog
	 * As default the function should return null which stands for no device custom tab item.  
	 */
	@Override
	public CTabItem getUtilityDeviceTabItem() {
		return this.application.getUtilGraphicsWindow(Messages.getString(MessageIds.GDE_MSGT2340));
	}
}
