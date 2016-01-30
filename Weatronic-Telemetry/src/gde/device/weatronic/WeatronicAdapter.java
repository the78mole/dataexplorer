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
    
    Copyright (c) 2014,2015,2016 Winfried Bruegmann
****************************************************************************************/
package gde.device.weatronic;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.DeviceDialog;
import gde.device.IDevice;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;
import gde.io.LogViewReader;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.WaitTimer;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.widgets.FileDialog;

/**
 * PowerLab8 base device class
 * @author Winfried Brügmann
 */
public class WeatronicAdapter extends DeviceConfiguration implements IDevice {
	final static Logger														log												= Logger.getLogger(WeatronicAdapter.class.getName());

	protected final DataExplorer									application;
	protected final Channels											channels;

	protected HashMap<String, CalculationThread>	calculationThreads				= new HashMap<String, CalculationThread>();

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public WeatronicAdapter(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.weatronic.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public WeatronicAdapter(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.weatronic.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	@Override
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// no device specific mapping required
		return lov2osdMap;
	}

	/**
	 * convert record LogView config data to GDE config keys into records section
	 * @param header reference to header data, contain all key value pairs
	 * @param lov2osdMap reference to the map where the key mapping
	 * @param channelNumber 
	 * @return converted configuration data
	 */
	@Override
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// ...
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	@Override
	public int getLovDataByteSize() {
		return 84;
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
		int deviceDataBufferSize = 76; // const.
		int[] points = new int[this.getNumberOfMeasurements(1)];
		int offset = 0;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();
		long lastDateTime = 0, sumTimeDelta = 0, deltaTime = 0;

		if (dataBuffer[0] == 0x7B) {
			byte[] convertBuffer = new byte[deviceDataBufferSize];
			if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

			for (int i = 0; i < recordDataSize; i++) {
				System.arraycopy(dataBuffer, offset + i * lovDataSize, convertBuffer, 0, deviceDataBufferSize);
				recordSet.addPoints(convertDataBytes(points, convertBuffer));

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}

			recordSet.setTimeStep_ms(this.getAverageTimeStep_ms() != null ? this.getAverageTimeStep_ms() : 1478); // no average time available, use a hard coded one
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
				WeatronicAdapter.log.log(java.util.logging.Level.FINEST, timeStamp + " " + timeStamp.substring(6, 17) + " " + dateTime);
				sb.append(dateTime);
				//System.arraycopy(dataBuffer, offset - 4, sizeBuffer, 0, 4);
				//sb.append(" ? ").append(LogViewReader.parse2Int(sizeBuffer));
				deltaTime = lastDateTime == 0 ? 0 : (dateTime - lastDateTime) / 1000 - 217; // value 217 is a compromis manual selected
				sb.append(" - ").append(deltaTime);
				sb.append(" - ").append(sumTimeDelta += deltaTime);
				WeatronicAdapter.log.log(java.util.logging.Level.FINER, sb.toString());
				lastDateTime = dateTime;

				recordSet.addTimeStep_ms(sumTimeDelta);

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		updateVisibilityStatus(recordSet, true);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;
		int VROffset = 1;
		float VRAmps = 1;

		// 0=SupplyVoltage 1=SupplyVoltage 2=CPUTemperature 3=Voltage 4=Current 5=Capacity 6=Power 7=Energy 8=Balance
		points[0] = DataParser.parse2Short(dataBuffer, 25) * 1000;
		points[1] = DataParser.parse2Short(dataBuffer, 81) * 1000;
		points[2] = (int) ((2.5 * DataParser.parse2Short(dataBuffer, 27) / 4095 - 0.986) / 0.00355 * 1000);
		points[3] = DataParser.parse2Short(dataBuffer, 33) * 1000;
		points[4] = DataParser.parse2Short(dataBuffer, 43) * 1000;
		points[5] = DataParser.parse2Int(dataBuffer, 35) * 1000;
		points[6] = Double.valueOf((points[3] / 1000.0) * (points[4] / 1000.0) * 1000).intValue(); // power U*I [W]
		points[7] = Double.valueOf((points[3] / 1000.0) * (points[5] / 1000.0)).intValue(); // energy U*C [mWh]

		// 9=CellVoltage1 10=CellVoltage2 11=CellVoltage3 12=CellVoltage4 13=CellVoltage5 14=CellVoltage6 15=CellVoltage7 16=CellVoltage8
		points[8] = 0;
		for (int i = 0, j = 0; i < 8; ++i, j += 2) {
			points[i + 9] = DataParser.parse2Short(dataBuffer, j + 3) * 1000;
			if (points[i + 9] > 0) {
				maxVotage = points[i + 9] > maxVotage ? points[i + 9] : maxVotage;
				minVotage = points[i + 9] < minVotage ? points[i + 9] : minVotage;
			}
		}
		//calculate balance on the fly
		points[8] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

		VRAmps = DataParser.parse2Short(dataBuffer, 69) / 600;
		VROffset = DataParser.parse2Short(dataBuffer, 115);
		// 17=CellRi 18=CellRi1 19=CellRi2 20=CellRi3 21=CellRi4 22=CellRi5 23=CellRi6 24=CellRi7 25=CellRi8
		points[17] = 0;
		for (int i = 0, j = 0; i < 8; ++i, j += 2) {
			points[i + 18] = (int) ((DataParser.parse2Short(dataBuffer, j + 53) / 6.3984 - VROffset) / VRAmps * 1000);
			points[17] += points[i + 18]; // add up cell resistance
		}

		return points;
	}

	/**
	 * query if the PowerLab8 executes discharge > charge > discharge cycles
	 */
	boolean isCycleMode(byte[] dataBuffer) {
		return (dataBuffer[142] & 0xFF) > 0;
	}

	/**
	 * getNumberOfCycle for NiCd and NiMh, for LiXx it  will return 0
	 * accuCellType -> Lithium=1, NiMH=2, NiCd=3, Pb=4
	 * @param dataBuffer
	 * @return cycle count
	 */
	public int getNumberOfCycle(byte[] dataBuffer) {
		return dataBuffer[142] & 0xFF;
	}

	/**
	 * @param dataBuffer
	 * @return firmware version
	 */
	public int getFirmwareVersion(byte[] dataBuffer) {
		return DataParser.parse2Short(dataBuffer[2], dataBuffer[1]);
	}

	/**
	 * @param dataBuffer
	 * @return cell count
	 */
	public int getNumberOfLithiumXCells(byte[] dataBuffer) {
		return (dataBuffer[132] & 0xFF);
	}

	/**
	 * get battery cell type
	 * 1 = Lithium Polymer
	 * 2 = Lithium Ion
	 * 3 = A123
	 * 4 = Lithium Manganese 5 = Lithium Cobalt
	 * 6 = NiCd
	 * 7 = NiMh
	 * 8 = Lead Acid
	 * 9 = LiFE
	 * 10 = Primary
	 * 11 = Power Supply
	 * @param dataBuffer
	 * @return for 1 = LiPo,2 = LiIo,3 = A123,4 = LiMa,5 = LiCo,6 = NiCd,7 = NiMh,8 = Lead Acid,9 = LiFE,10 = Primary,11 = Power Supply
	 */
	public int getAccuCellType(byte[] dataBuffer) {
		return (dataBuffer[135] & 0xFF);
	}

	/**
	 * check if processing 
	 * 0 = Charger Ready to Start
	 * 1 = Detecting Pack
	 * 6 = Charging
	 * 7 = Trickle Charging
	 * 8 = Discharging
	 * 9 = Monitoring
	 * 10 = Halt for Safety Screen
	 * 11 = Pack Cool Down (when cycling)
	 * 99 = System Stop Error Occurred
	 * byte 44,Bit8 = Charge/Discharge Complete
	 * @param dataBuffer
	 * @return
	 */
	public boolean isProcessing(byte[] dataBuffer) {
		return getProcessingMode(dataBuffer) > 0 && getProcessingMode(dataBuffer) < 99 && (dataBuffer[44] & 0x80) == 0;
	}

	/**
	 * get processing mode 
	 * 0 = Charger Ready to Start
	 * 1 = Detecting Pack
	 * 6 = Charging
	 * 7 = Trickle Charging
	 * 8 = Discharging
	 * 9 = Monitoring
	 * 10 = Halt for Safety Screen
	 * 11 = Pack Cool Down (when cycling)
	 * 99 = System Stop Error Occurred
	 * @param dataBuffer
	 * @return
	 */
	public int getProcessingMode(byte[] dataBuffer) {
		return (dataBuffer[133] & 0xFF);
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
		Vector<Integer> timeStamps = new Vector<Integer>(1, 1);
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = 0;
		if (!recordSet.isTimeStepConstant()) {
			timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
			byte[] timeStampBuffer = new byte[timeStampBufferSize];
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8)
						+ ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
			}
		}
		WeatronicAdapter.log.log(java.util.logging.Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString());

		for (int i = 0; i < recordDataSize; i++) {
			WeatronicAdapter.log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize + timeStampBufferSize);
			System.arraycopy(dataBuffer, i * dataBufferSize + timeStampBufferSize, convertBuffer, 0, dataBufferSize);

			// 0=SupplyVoltage 1=SupplyVoltage 2=CPUTemperature 3=Voltage 4=Current 5=Capacity 6=Power 7=Energy 8=Balance
			// 9=CellVoltage1 10=CellVoltage2 11=CellVoltage3 12=CellVoltage4 13=CellVoltage5 14=CellVoltage6 15=CellVoltage7 16=CellVoltage8
			// 17=CellRi 18=CellRi1 19=CellRi2 20=CellRi3 21=CellRi4 22=CellRi5 23=CellRi6 24=CellRi7 25=CellRi8
			points[0] = (((convertBuffer[0] & 0xff) << 24) + ((convertBuffer[1] & 0xff) << 16) + ((convertBuffer[2] & 0xff) << 8) + ((convertBuffer[3] & 0xff) << 0));
			points[1] = (((convertBuffer[4] & 0xff) << 24) + ((convertBuffer[5] & 0xff) << 16) + ((convertBuffer[6] & 0xff) << 8) + ((convertBuffer[7] & 0xff) << 0));
			points[2] = (((convertBuffer[8] & 0xff) << 24) + ((convertBuffer[9] & 0xff) << 16) + ((convertBuffer[10] & 0xff) << 8) + ((convertBuffer[11] & 0xff) << 0));
			points[3] = Double.valueOf((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); // power U*I [W]
			points[4] = Double.valueOf((points[0] / 1000.0) * (points[2] / 1000.0)).intValue(); // energy U*C [mWh]
			points[5] = (((convertBuffer[12] & 0xff) << 24) + ((convertBuffer[13] & 0xff) << 16) + ((convertBuffer[14] & 0xff) << 8) + ((convertBuffer[15] & 0xff) << 0));
			points[6] = (((convertBuffer[16] & 0xff) << 24) + ((convertBuffer[17] & 0xff) << 16) + ((convertBuffer[18] & 0xff) << 8) + ((convertBuffer[19] & 0xff) << 0));
			points[7] = (((convertBuffer[20] & 0xff) << 24) + ((convertBuffer[21] & 0xff) << 16) + ((convertBuffer[22] & 0xff) << 8) + ((convertBuffer[23] & 0xff) << 0));
			points[8] = 0;
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;

			// 8=CellVoltage1 9=CellVoltage2 10=CellVoltage3 11=CellVoltage4 12=CellVoltage5 13=CellVoltage6
			for (int j = 0, k = 0; j < points.length - 9; ++j, k += GDE.SIZE_BYTES_INTEGER) {
				//log_base.info("cell " + (i+1) + " points[" + (i+8) + "]  = new Integer((((dataBuffer[" + (j+45) + "] & 0xFF)-0x80)*100 + ((dataBuffer[" + (j+46)+ "] & 0xFF)-0x80))*10);");  //45,46 CELL_420v[1];
				points[j + 9] = (((convertBuffer[k + 24] & 0xff) << 24) + ((convertBuffer[k + 25] & 0xff) << 16) + ((convertBuffer[k + 26] & 0xff) << 8) + ((convertBuffer[k + 27] & 0xff) << 0));
				if (points[j + 9] > 0) {
					maxVotage = points[j + 9] > maxVotage ? points[j + 9] : maxVotage;
					minVotage = points[j + 9] < minVotage ? points[j + 9] : minVotage;
				}
			}
			//calculate balance on the fly
			points[8] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

			if (recordSet.isTimeStepConstant())
				recordSet.addPoints(points);
			else
				recordSet.addPoints(points, timeStamps.get(i) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			for (int j = 0; j < recordSet.size(); j++) {
				Record record = recordSet.get(j);
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				if (j > 5 && record.getUnit().equals("V")) //cell voltage BC6 no temperature measurements
					dataTableRow[j + 1] = String.format("%.3f", (((record.realGet(rowIndex) / 1000.0) - reduction) * factor));
				else
					dataTableRow[j + 1] = record.getDecimalFormat().format((((record.realGet(rowIndex) / 1000.0) - reduction) * factor));
			}
		}
		catch (RuntimeException e) {
			WeatronicAdapter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double translateValue(Record record, double value) {
		// 0=SupplyVoltage 1=SupplyVoltage 2=CPUTemperature 3=Voltage 4=Current 5=Capacity 6=Power 7=Energy 8=Balance
		// 9=CellVoltage1 10=CellVoltage2 11=CellVoltage3 12=CellVoltage4 13=CellVoltage5 14=CellVoltage6 15=CellVoltage7 16=CellVoltage8
		// 17=CellRi 18=CellRi1 19=CellRi2 20=CellRi3 21=CellRi4 22=CellRi5 23=CellRi6 24=CellRi7 25=CellRi8
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required

		double newValue = value * factor + offset;
		WeatronicAdapter.log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		// 0=SupplyVoltage 1=SupplyVoltage 2=CPUTemperature 3=Voltage 4=Current 5=Capacity 6=Power 7=Energy 8=Balance
		// 9=CellVoltage1 10=CellVoltage2 11=CellVoltage3 12=CellVoltage4 13=CellVoltage5 14=CellVoltage6 15=CellVoltage7 16=CellVoltage8
		// 17=CellRi 18=CellRi1 19=CellRi2 20=CellRi3 21=CellRi4 22=CellRi5 23=CellRi6 24=CellRi7 25=CellRi8
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required

		double newValue = value / factor - offset;
		WeatronicAdapter.log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	@Override
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {

		// 0=SupplyVoltage 1=SupplyVoltage 2=CPUTemperature 3=Voltage 4=Current 5=Capacity 6=Power 7=Energy 8=Balance
		// 9=CellVoltage1 10=CellVoltage2 11=CellVoltage3 12=CellVoltage4 13=CellVoltage5 14=CellVoltage6 15=CellVoltage7 16=CellVoltage8
		// 17=CellRi 18=CellRi1 19=CellRi2 20=CellRi3 21=CellRi4 22=CellRi5 23=CellRi6 24=CellRi7 25=CellRi8
		recordSet.setAllDisplayable();
		for (String recordKey : recordSet.getNoneCalculationRecordNames()) {
			recordSet.get(recordKey).setActive(true);
		}
		for (int i = 6; i < recordSet.size(); ++i) {
			Record record = recordSet.get(i);
			record.setDisplayable(record.hasReasonableData());
			if (WeatronicAdapter.log.isLoggable(java.util.logging.Level.FINER)) WeatronicAdapter.log.log(java.util.logging.Level.FINER, record.getName() + " setDisplayable=" + record.hasReasonableData());
		}

		if (WeatronicAdapter.log.isLoggable(java.util.logging.Level.FINE)) {
			for (Record record : recordSet.values()) {
				WeatronicAdapter.log.log(java.util.logging.Level.FINE, record.getName() + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable());
			}
		}
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are live measurement points only the calculation will take place directly after switch all to displayable
		if (recordSet.isRaw()) {
			// calculate the values required
			try {
				// 0=SupplyVoltage 1=SupplyVoltage 2=CPUTemperature 3=Voltage 4=Current 5=Capacity 6=Power 7=Energy 8=Balance
				// 9=CellVoltage1 10=CellVoltage2 11=CellVoltage3 12=CellVoltage4 13=CellVoltage5 14=CellVoltage6 15=CellVoltage7 16=CellVoltage8
				// 17=CellRi 18=CellRi1 19=CellRi2 20=CellRi3 21=CellRi4 22=CellRi5 23=CellRi6 24=CellRi7 25=CellRi8
				int displayableCounter = 0;

				// check if measurements isActive == false and set to isDisplayable == false
				for (String measurementKey : recordSet.keySet()) {
					Record record = recordSet.get(measurementKey);

					if (record.isActive() && (record.getOrdinal() <= 6 || record.hasReasonableData())) {
						++displayableCounter;
					}
				}

				Record record = recordSet.get(3);//3=Leistung
				if (record != null && (record.size() == 0 || !record.hasReasonableData())) {
					this.calculationThreads.put(record.getName(), new CalculationThread(record.getName(), this.channels.getActiveChannel().getActiveRecordSet()));
					try {
						this.calculationThreads.get(record.getName()).start();
					}
					catch (RuntimeException e) {
						WeatronicAdapter.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
					}
				}
				++displayableCounter;

				record = recordSet.get(4);//4=Energie
				if (record != null && (record.size() == 0 || !record.hasReasonableData())) {
					this.calculationThreads.put(record.getName(), new CalculationThread(record.getName(), this.channels.getActiveChannel().getActiveRecordSet()));
					try {
						this.calculationThreads.get(record.getName()).start();
					}
					catch (RuntimeException e) {
						WeatronicAdapter.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
					}
				}
				++displayableCounter;

				WeatronicAdapter.log.log(java.util.logging.Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
				recordSet.setConfiguredDisplayable(displayableCounter);

				if (recordSet.getName().equals(this.channels.getActiveChannel().getActiveRecordSet().getName())) {
					this.application.updateGraphicsWindow();
				}
			}
			catch (RuntimeException e) {
				WeatronicAdapter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR };
	}

	/**
	 * @return the dialog
	 */
	@Override
	public DeviceDialog getDialog() {
		return null;
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 */
	@Override
	public void open_closeCommPort() {
		importDeviceData();
	}

	/**
	 * import device specific *.bin data files
	 */
	protected void importDeviceData() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT3700), "LogData");

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					WeatronicAdapter.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.FILE_SEPARATOR_UNIX + tmpFileName;
						if (!selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
							if (selectedImportFile.contains(GDE.STRING_DOT)) {
								selectedImportFile = selectedImportFile.substring(0, selectedImportFile.indexOf(GDE.STRING_DOT));
							}
							selectedImportFile = selectedImportFile + GDE.FILE_ENDING_DOT_LOG;
						}
						log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							Integer channelConfigNumber = WeatronicAdapter.this.application.getActiveChannelNumber();
							channelConfigNumber = channelConfigNumber == null ? 1 : channelConfigNumber;
							//String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.STRING_DOT) - 4, selectedImportFile.lastIndexOf(GDE.STRING_DOT));
							try {
								LogReader.read(selectedImportFile); //, HoTTAdapter.this, GDE.STRING_EMPTY, channelConfigNumber);
								WaitTimer.delay(500);
							}
							catch (Exception e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally  {
					WeatronicAdapter.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}

	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public int[] getCellVoltageOrdinals() {
		// 0=SupplyVoltage 1=SupplyVoltage 2=CPUTemperature 3=Voltage 4=Current 5=Capacity 6=Power 7=Energy 8=Balance
		// 9=CellVoltage1 10=CellVoltage2 11=CellVoltage3 12=CellVoltage4 13=CellVoltage5 14=CellVoltage6 15=CellVoltage7 16=CellVoltage8
		// 17=CellRi 18=CellRi1 19=CellRi2 20=CellRi3 21=CellRi4 22=CellRi5 23=CellRi6 24=CellRi7 25=CellRi8
		return new int[] { 3, 5 };
	}

	/**
	 * query device for specific smoothing index
	 * 0 do nothing at all
	 * 1 current drops just a single peak
	 * 2 current drop more or equal than 2 measurements 
	 */
	@Override
	public int	getCurrentSmoothIndex() {
		return 2;
	}
}
