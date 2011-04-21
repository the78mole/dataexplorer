package gde.device.graupner;



import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.comm.DeviceSerialPortImpl;
import gde.config.Settings;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.exception.DataInconsitsentException;
import gde.messages.Messages;

import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

/**
 * Graupner Ultramat Trio 16 S
 * @author Winfried Brügmann
 */
public class UltramatTrio14 extends Ultramat {
	final static Logger														logger															= Logger.getLogger(Ultramat.class.getName());


	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public UltramatTrio14(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2200), Messages.getString(MessageIds.GDE_MSGT2201), Messages.getString(MessageIds.GDE_MSGT2202),
				Messages.getString(MessageIds.GDE_MSGT2203), Messages.getString(MessageIds.GDE_MSGT2204), Messages.getString(MessageIds.GDE_MSGT2205), Messages.getString(MessageIds.GDE_MSGT2206),
				Messages.getString(MessageIds.GDE_MSGT2207), Messages.getString(MessageIds.GDE_MSGT2208), Messages.getString(MessageIds.GDE_MSGT2209) };
		this.CHARGE_MODE = new String[0];
		this.DISCHARGE_MODE = new String[0];
		this.DELAY_MODE = new String[0];
		this.CURRENT_MODE = new String[0];

		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		this.dialog = null;
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public UltramatTrio14(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2200), Messages.getString(MessageIds.GDE_MSGT2201), Messages.getString(MessageIds.GDE_MSGT2202),
				Messages.getString(MessageIds.GDE_MSGT2203), Messages.getString(MessageIds.GDE_MSGT2204), Messages.getString(MessageIds.GDE_MSGT2205), Messages.getString(MessageIds.GDE_MSGT2206),
				Messages.getString(MessageIds.GDE_MSGT2207), Messages.getString(MessageIds.GDE_MSGT2208), Messages.getString(MessageIds.GDE_MSGT2209) };
		this.CHARGE_MODE = new String[0];
		this.DISCHARGE_MODE = new String[0];
		this.DELAY_MODE = new String[0];
		this.CURRENT_MODE = new String[0];

		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		this.dialog = null;
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
		int deviceDataBufferSize = this.getDataBlockSize(); 
		int[] points = new int[this.getNumberOfMeasurements(recordSet.getChannelConfigNumber())];
		int offset = 4;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();

		if (dataBuffer[offset] == 0x0C) {
			byte[] convertBuffer = new byte[deviceDataBufferSize];
			if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

			for (int i = 0; i < recordDataSize; i++) {
				System.arraycopy(dataBuffer, offset + i * lovDataSize, convertBuffer, 0, deviceDataBufferSize);
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
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte array with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;

		if (points.length == 13) {
			// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=VersorgungsSpg 6=Balance 7=SpannungZelle1 8=SpannungZelle2....
			points[0] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) dataBuffer[13], (char) dataBuffer[14], (char) dataBuffer[15], (char) dataBuffer[16]), 16);
			points[1] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) dataBuffer[17], (char) dataBuffer[18], (char) dataBuffer[19], (char) dataBuffer[20]), 16);
			points[2] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) dataBuffer[21], (char) dataBuffer[22], (char) dataBuffer[23], (char) dataBuffer[24]), 16);
			points[3] = Double.valueOf(points[0] * points[1] / 1000.0).intValue(); // power U*I [W]
			points[4] = Double.valueOf(points[0] * points[2] / 1000.0).intValue(); // energy U*C [Wh]
			points[5] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) dataBuffer[5], (char) dataBuffer[6], (char) dataBuffer[7], (char) dataBuffer[8]), 16);
			points[6] = 0;
			// 7=SpannungZelle1 8=SpannungZelle2 9=SpannungZelle3 10=SpannungZelle4 11=SpannungZelle5 12=SpannungZelle6 
			for (int i = 0, j = 0; i < 6; ++i, j += 4) {
				points[i + 7] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) dataBuffer[25 + j], (char) dataBuffer[26 + j], (char) dataBuffer[27 + j], (char) dataBuffer[28 + j]),
						16);
				if (points[i + 7] > 0) {
					maxVotage = points[i + 7] > maxVotage ? points[i + 7] : maxVotage;
					minVotage = points[i + 7] < minVotage ? points[i + 7] : minVotage;
				}
			}
			//calculate balance on the fly
			points[6] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
		}
		else { //points.length = 10; dataBuffer is prepared by System.arrayCopy for outlet channel 2 and 3
			// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=VersorgungsSpg 6=Balance 
			points[0] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) dataBuffer[13], (char) dataBuffer[14], (char) dataBuffer[15], (char) dataBuffer[16]), 16);
			points[1] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) dataBuffer[17], (char) dataBuffer[18], (char) dataBuffer[19], (char) dataBuffer[20]), 16);
			points[2] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) dataBuffer[21], (char) dataBuffer[22], (char) dataBuffer[23], (char) dataBuffer[24]), 16);
			points[3] = Double.valueOf(points[0] * points[1] / 1000.0).intValue(); // power U*I [W]
			points[4] = Double.valueOf(points[0] * points[2] / 1000.0).intValue(); // energy U*C [Wh]
			points[5] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) dataBuffer[5], (char) dataBuffer[6], (char) dataBuffer[7], (char) dataBuffer[8]), 16);
			points[6] = 0;
			// 7=SpannungZelle1 8=SpannungZelle2 9=SpannungZelle3
			for (int i = 0, j = 0; i < 3; ++i, j += 4) {
				points[i + 7] = Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) dataBuffer[25 + j], (char) dataBuffer[26 + j], (char) dataBuffer[27 + j], (char) dataBuffer[28 + j]),
						16);
				if (points[i + 7] > 0) {
					maxVotage = points[i + 7] > maxVotage ? points[i + 7] : maxVotage;
					minVotage = points[i + 7] < minVotage ? points[i + 7] : minVotage;
				}
			}
			//calculate balance on the fly
			points[6] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;
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
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		if (recordSet.getChannelConfigNumber() == 1) {
			for (int i = 0; i < recordDataSize; i++) {
				int maxVotage = Integer.MIN_VALUE;
				int minVotage = Integer.MAX_VALUE;
				logger.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize); //$NON-NLS-1$
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
				logger.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize); //$NON-NLS-1$
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
		updateVisibilityStatus(recordSet, true);
		recordSet.syncScaleOfSyncableRecords();
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
		String[] recordKeys = recordSet.getRecordNames();

		recordSet.setAllDisplayable();
		int numCells = 12; //TODO
		for (int i = recordKeys.length - numCells - 1; i < recordKeys.length; ++i) {
			Record record = recordSet.get(recordKeys[i]);
			record.setDisplayable(record.getOrdinal() <= 5 || record.hasReasonableData());
			log.log(java.util.logging.Level.FINER, recordKeys[i] + " setDisplayable=" + (record.getOrdinal() <= 5 || record.hasReasonableData())); //$NON-NLS-1$
		}

		if (log.isLoggable(java.util.logging.Level.FINE)) {
			for (String recordKey : recordKeys) {
				Record record = recordSet.get(recordKey);
				log.log(java.util.logging.Level.FINE, recordKey + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
	}

	/**
	 * query the device identifier to differentiate between different device implementations
	 * @return 	-1=Ultramat16 1=Ultramat50, 2=Ultramat40, 3=UltramatTrio14, 4=Ultramat45, 5=Ultramat60, 6=Ultramat16S
	 */
	@Override
	public GraupnerDeviceType getDeviceTypeIdentifier() {
		return GraupnerDeviceType.UltramatTrio14;
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
}
