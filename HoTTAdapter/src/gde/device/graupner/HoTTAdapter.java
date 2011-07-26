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
    
    Copyright (c) 2011 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.graupner.hott.MessageIds;
import gde.exception.DataInconsitsentException;
import gde.io.FileHandler;
import gde.io.LogViewReader;
import gde.io.NMEAParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.GPSHelper;
import gde.utils.WaitTimer;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Sample device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class HoTTAdapter extends DeviceConfiguration implements IDevice {
	final static Logger					log										= Logger.getLogger(HoTTAdapter.class.getName());

	final static String					SENSOR_TYPE						= "SensorType";																	//$NON-NLS-1$
	final static String					LOG_COUNT							= "LogCount";																		//$NON-NLS-1$

	final static byte						SENSOR_TYPE_RECEIVER	= (byte) (0x80 & 0xFF);
	final static byte						SENSOR_TYPE_VARIO			= (byte) (0x89 & 0xFF);
	final static byte						SENSOR_TYPE_GPS				= (byte) (0x8A & 0xFF);
	final static byte						SENSOR_TYPE_GENERAL		= (byte) (0x8D & 0xFF);
	final static byte						SENSOR_TYPE_ELECTRIC	= (byte) (0x8E & 0xFF);

	final DataExplorer					application;
	final Channels							channels;
	final HoTTAdapterDialog			dialog;
	final HoTTAdapterSerialPort	serialPort;

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public HoTTAdapter(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.hott.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.serialPort = this.application != null ? new HoTTAdapterSerialPort(this, this.application) : new HoTTAdapterSerialPort(this, null);
		this.dialog = new HoTTAdapterDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2404), Messages.getString(MessageIds.GDE_MSGT2404));
			updateFileMenu(this.application.getMenuBar().getExportMenu());
		}
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public HoTTAdapter(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.hott.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.serialPort = this.application != null ? new HoTTAdapterSerialPort(this, this.application) : new HoTTAdapterSerialPort(this, null);
		this.dialog = new HoTTAdapterDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2404), Messages.getString(MessageIds.GDE_MSGT2404));
			updateFileMenu(this.application.getMenuBar().getExportMenu());
		}
	}

	/**
	 * @return the serialPort
	 */
	@Override
	public HoTTAdapterSerialPort getCommunicationPort() {
		return this.serialPort;
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	@Override
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// ...
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
		return 0; // sometimes first 4 bytes give the length of data + 4 bytes for number
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
		NMEAParser data = new NMEAParser(this.getDataBlockLeader(), this.getDataBlockSeparator().value(), this.getDataBlockCheckSumType(), Math.abs(this.getDataBlockSize()), this,
				this.channels.getActiveChannelNumber(), this.getUTCdelta());
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		byte[] lineBuffer;
		byte[] subLengthBytes;
		int subLenght;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		try {
			int lastLength = 0;
			for (int i = 0; i < recordDataSize; i++) {
				subLengthBytes = new byte[4];
				System.arraycopy(dataBuffer, lastLength, subLengthBytes, 0, 4);
				subLenght = LogViewReader.parse2Int(subLengthBytes) - 8;
				//System.out.println((subLenght+8));
				lineBuffer = new byte[subLenght];
				System.arraycopy(dataBuffer, 4 + lastLength, lineBuffer, 0, subLenght);
				String textInput = new String(lineBuffer, "ISO-8859-1"); //$NON-NLS-1$
				//System.out.println(textInput);
				StringTokenizer st = new StringTokenizer(textInput);
				Vector<String> vec = new Vector<String>();
				while (st.hasMoreTokens())
					vec.add(st.nextToken("\r\n"));
				data.parse(vec, vec.size());
				lastLength += (subLenght + 12);

				recordSet.addNoneCalculationRecordsPoints(data.getValues(), data.getTime_ms());

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}
			this.updateVisibilityStatus(recordSet, true);
			if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		}
		catch (Exception e) {
			String msg = e.getMessage() + Messages.getString(gde.messages.MessageIds.GDE_MSGW0543);
			log.log(java.util.logging.Level.WARNING, msg, e);
			this.application.openMessageDialog(msg);
			if (doUpdateProgressBar) this.application.setProgress(0, sThreadId);
		}
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

		switch (dataBuffer[0]) {
		case HoTTAdapter.SENSOR_TYPE_RECEIVER:
			//0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx
			points[0] = 0; // seams not part of live data ?? (dataBuffer[15] & 0xFF) * 1000;
			points[1] = (dataBuffer[9] & 0xFF) * 1000;
			points[2] = (dataBuffer[5] & 0xFF) * 1000;
			;
			points[3] = HoTTbinReader.parse2Short(dataBuffer, 11) * 1000;
			//points[3] = (points[3] > 2000 ? 2000 : points[3]) * 1000;
			points[4] = (dataBuffer[13] & 0xFF) * 1000;
			;
			points[5] = (dataBuffer[9] & 0xFF) * 1000;
			;
			points[6] = (dataBuffer[6] & 0xFF) * 1000;
			points[7] = (dataBuffer[7] & 0xFF) * 1000;
			break;

		case HoTTAdapter.SENSOR_TYPE_VARIO:
			//0=RXSQ, 1=Height, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
			points[0] = (dataBuffer[15] & 0xFF) * 1000;
			points[1] = HoTTbinReader.parse2Short(dataBuffer, 16) * 1000;
			//pointsVario[0]max = parse2Short(buf1, 5) * 1000; 			// VD.MaxAlt:=SmallInt(buf[1].data_3_1[5] or (buf[1].data_3_1[6] SHL 8))-500;
			//pointsVario[0]min = parse2Short(buf1, 7) * 1000; 			// VD.MinAlt:=SmallInt(buf[1].data_3_1[7] or (buf[1].data_3_1[8] SHL 8))-500;
			points[2] = HoTTbinReader.parse2Short(dataBuffer[22], dataBuffer[0]) * 1000;
			points[3] = HoTTbinReader.parse2Short(dataBuffer[24], dataBuffer[2]) * 1000;
			points[4] = HoTTbinReader.parse2Short(dataBuffer[26], dataBuffer[4]) * 1000;
			points[5] = (dataBuffer[8] & 0xFF) * 1000;
			points[6] = (dataBuffer[5] & 0xFF) * 1000;
			break;

		case HoTTAdapter.SENSOR_TYPE_GPS:
			//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			points[0] = (dataBuffer[15] & 0xFF) * 1000;
			points[1] = HoTTbinReader.parse2Short(dataBuffer, 20) * 10000 + HoTTbinReader.parse2Short(dataBuffer, 22);
			points[1] = dataBuffer[19] == 1 ? -1 * points[1] : points[1];
			points[2] = HoTTbinReader.parse2Short(dataBuffer, 25) * 10000 + HoTTbinReader.parse2Short(dataBuffer, 27);
			points[2] = dataBuffer[24] == 1 ? -1 * points[2] : points[2];
			points[3] = HoTTbinReader.parse2Short(dataBuffer, 31) * 1000;
			points[4] = HoTTbinReader.parse2Short(dataBuffer, 33) * 1000;
			points[5] = (dataBuffer[35] & 0xFF) * 1000;
			points[6] = HoTTbinReader.parse2Short(dataBuffer, 17) * 1000;
			points[7] = HoTTbinReader.parse2Short(dataBuffer, 29) * 1000;
			points[8] = (dataBuffer[16] & 0xFF) * 1000;
			points[9] = 0;
			points[10] = (dataBuffer[8] & 0xFF) * 1000;
			points[11] = (dataBuffer[5] & 0xFF) * 1000;
			break;

		case HoTTAdapter.SENSOR_TYPE_GENERAL:
			//0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel 17=OilLevel, 18=Voltage 1, 19=Voltage 2, 20=Temperature 1, 21=Temperature 2							
			points[0] = (dataBuffer[15] & 0xFF) * 1000;
			points[1] = HoTTbinReader.parse2Short(dataBuffer, 40) * 1000;
			points[2] = HoTTbinReader.parse2Short(dataBuffer, 38) * 1000;
			points[3] = HoTTbinReader.parse2Short(dataBuffer, 42) * 1000;
			points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
			points[5] = 0; //5=Balance
			for (int j = 0; j < 6; j++) {
				points[j + 6] = (dataBuffer[16 + j] & 0xFF) * 1000;
				if (points[j + 6] > 0) {
					maxVotage = points[j + 6] > maxVotage ? points[j + 6] : maxVotage;
					minVotage = points[j + 6] < minVotage ? points[j + 6] : minVotage;
				}
			}
			//calculate balance on the fly
			points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 1000;
			points[12] = HoTTbinReader.parse2Short(dataBuffer, 31) * 1000;
			points[13] = HoTTbinReader.parse2Short(dataBuffer, 33) * 1000;
			points[14] = HoTTbinReader.parse2Short(dataBuffer, 35) * 1000;
			points[15] = (dataBuffer[37] & 0xFF) * 1000;
			points[16] = HoTTbinReader.parse2Short(dataBuffer, 29) * 1000;
			points[17] = 0;
			points[18] = HoTTbinReader.parse2Short(dataBuffer, 22) * 1000;
			points[19] = HoTTbinReader.parse2Short(dataBuffer, 24) * 1000;
			points[20] = (dataBuffer[26] & 0xFF) * 1000;
			points[21] = (dataBuffer[27] & 0xFF) * 1000;
			break;

		case HoTTAdapter.SENSOR_TYPE_ELECTRIC:
			//0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Revolution, 21=Height, 22=Climb 1, 23=Climb 3, 24=Voltage 1, 25=Voltage 2, 26=Temperature 1, 27=Temperature 2			
			points[0] = (dataBuffer[15] & 0xFF) * 1000;
			points[1] = HoTTbinReader.parse2Short(dataBuffer, 40) * 1000;
			points[2] = HoTTbinReader.parse2Short(dataBuffer, 38) * 1000;
			points[3] = HoTTbinReader.parse2Short(dataBuffer, 42) * 1000;
			points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
			points[5] = 0; //5=Balance
			for (int j = 0; j < 14; j++) {
				points[j + 6] = (dataBuffer[16 + j] & 0xFF) * 1000;
				if (points[j + 6] > 0) {
					maxVotage = points[j + 6] > maxVotage ? points[j + 6] : maxVotage;
					minVotage = points[j + 6] < minVotage ? points[j + 6] : minVotage;
				}
			}
			//calculate balance on the fly
			points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 1000;
			points[20] = HoTTbinReader.parse2Short(dataBuffer, 33) * 1000;
			points[21] = HoTTbinReader.parse2Short(dataBuffer, 36) * 1000;
			points[22] = HoTTbinReader.parse2Short(dataBuffer, 44) * 1000;
			points[23] = (dataBuffer[46] & 0xFF) * 1000;
			points[24] = HoTTbinReader.parse2Short(dataBuffer, 30) * 1000;
			points[26] = HoTTbinReader.parse2Short(dataBuffer, 32) * 1000;
			points[26] = (dataBuffer[34] & 0xFF) * 1000;
			points[27] = (dataBuffer[35] & 0xFF) * 1000;
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
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.getRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		Vector<Integer> timeStamps = new Vector<Integer>(1, 1);
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
		byte[] timeStampBuffer = new byte[timeStampBufferSize];
		if (!recordSet.isTimeStepConstant()) {
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8)
						+ ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
			}
		}
		log.log(java.util.logging.Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString()); //$NON-NLS-1$

		for (int i = 0; i < recordDataSize; i++) {
			log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize + timeStampBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i * dataBufferSize + timeStampBufferSize, convertBuffer, 0, dataBufferSize);

			for (int j = 0; j < points.length; j++) {
				points[j] = (((convertBuffer[0 + (j * 4)] & 0xff) << 24) + ((convertBuffer[1 + (j * 4)] & 0xff) << 16) + ((convertBuffer[2 + (j * 4)] & 0xff) << 8) + ((convertBuffer[3 + (j * 4)] & 0xff) << 0));
			}

			if (recordSet.isTimeStepConstant())
				recordSet.addPoints(points);
			else
				recordSet.addPoints(points, timeStamps.get(i) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		this.updateVisibilityStatus(recordSet, true);
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			String[] recordNames = recordSet.getRecordNames();
			for (int j = 0; j < recordNames.length; j++) {
				Record record = recordSet.get(recordNames[j]);
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb, 5=Velocity, 6=DistanceStart, 7=DirectionStart, 8=TripDistance, 9=VoltageRx, 10=TemperatureRx
				if ((j == 1 || j == 2) && record.getParent().getChannelConfigNumber() == 3) { // 1=GPS-longitude 2=GPS-latitude  
					int grad = record.get(rowIndex) / 1000000;
					double minuten = record.get(rowIndex) % 1000000 / 10000.0;
					dataTableRow[j + 1] = String.format("%d %.4f", grad, minuten); //$NON-NLS-1$
				}
				else {
					dataTableRow[j + 1] = record.getDecimalFormat().format((offset + ((record.get(rowIndex) / 1000.0) - reduction) * factor));
				}
			}
		}
		catch (RuntimeException e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
		double newValue = 0;

		if (record.getParent().getChannelConfigNumber() == 3 && (record.getOrdinal() == 1 || record.getOrdinal() == 2)) { // 1=GPS-longitude 2=GPS-latitude 
			//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			int grad = ((int) (value / 1000));
			double minuten = (value - (grad * 1000.0)) / 10.0;
			newValue = grad + minuten / 60.0;
		}
		else {
			newValue = (value - reduction) * factor + offset;
		}

		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

		if ((record.getOrdinal() == 1 || record.getOrdinal() == 2) && record.getParent().getChannelConfigNumber() == 3) { // 1=GPS-longitude 2=GPS-latitude  ) 
			//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			int grad = (int) value;
			double minuten = (value - grad * 1.0) * 60.0;
			newValue = (grad + minuten / 100.0) * 1000.0;
		}
		else {
			newValue = (value - offset) / factor + reduction;
		}

		log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		int channelConfigNumber = recordSet.getChannelConfigNumber();
		int displayableCounter = 0;
		boolean configChanged = this.isChangePropery();
		Record record;

		String[] measurementNames = this.getMeasurementNames(channelConfigNumber);
		String[] recordNames = recordSet.getRecordNames();
		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordNames.length; ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(recordNames[i]);
			log.log(java.util.logging.Level.FINE, recordNames[i] + " = " + measurementNames[i]); //$NON-NLS-1$

			// update active state and displayable state if configuration switched with other names
			MeasurementType measurement = this.getMeasurement(channelConfigNumber, i);
			if (record.isActive() != measurement.isActive()) {
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				log.log(Level.OFF, "switch " + record.getName() + " to " + measurement.isActive()); //$NON-NLS-1$ //$NON-NLS-2$
			}	
			if (includeReasonableDataCheck) {
				record.setDisplayable(record.hasReasonableData() && measurement.isActive());
				log.log(java.util.logging.Level.FINE, record.getName() + " hasReasonableData " + record.hasReasonableData()); //$NON-NLS-1$ 
			}

			if (record.isActive() && record.isDisplayable()) {
				log.log(java.util.logging.Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
				++displayableCounter;
			}
		}
		log.log(java.util.logging.Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
		this.setChangePropery(configChanged); //reset configuration change indicator to previous value, do not vote automatic configuration change at all
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {

		if (recordSet.getChannelConfigNumber() == 3) { // 1=GPS-longitude 2=GPS-latitude 3=Height
			//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			Record recordLongitude = recordSet.get(1);
			Record recordLatitude = recordSet.get(2);
			Record recordAlitude = recordSet.get(3);
			if (recordLatitude.hasReasonableData() && recordLongitude.hasReasonableData() && recordAlitude.hasReasonableData()) {
				int recordSize = recordLatitude.realSize();
				int startAltitude = recordAlitude.get(0); // using this as start point might be sense less if the GPS data has no 3D-fix
				//check GPS latitude and longitude				
				int indexGPS = 0;
				int i = 0;
				for (; i < recordSize; ++i) {
					if (recordLatitude.get(i) != 0 && recordLongitude.get(i) != 0) {
						indexGPS = i;
						++i;
						break;
					}
				}
				startAltitude = recordAlitude.get(indexGPS); //set initial altitude to enable absolute altitude calculation 		

				GPSHelper.calculateTripLength(this, recordSet, 2, 1, 3, startAltitude, 7, 9);
				this.application.updateStatisticsData(true);
				this.updateVisibilityStatus(recordSet, true);
			}
		}
		//TODO check strength calculation
		//		else if (recordSet.getChannelConfigNumber() == 1) { // receiver
		//			//0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 			
		//			Record recordRF_RXSQ = recordSet.get(1);
		//			Record recordStrength = recordSet.get(2);
		//			recordStrength.clear();
		//			for (int i=0; i < recordRF_RXSQ.size(); ++i) {
		//				recordStrength.add(HoTTbinReader.convertRFRXSQ2Strenght(recordRF_RXSQ.get(i) / 1000));
		//			}		
		//			this.application.updateStatisticsData(true);
		//			this.updateVisibilityStatus(recordSet, true);
		//		}

	}

	/**
	 * @return the dialog
	 */
	@Override
	public HoTTAdapterDialog getDialog() {
		return this.dialog;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION };
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	@Override
	public void open_closeCommPort() {
		String devicePath = this.application.getActiveDevice() != null ? GDE.FILE_SEPARATOR_UNIX + this.application.getActiveDevice().getName() : GDE.STRING_EMPTY;
		String searchDirectory = Settings.getInstance().getDataFilePath() + devicePath + GDE.FILE_SEPARATOR_UNIX;
		if (FileUtils.checkDirectoryExist(this.getDeviceConfiguration().getDataBlockPreferredDataLocation())) {
			searchDirectory = this.getDeviceConfiguration().getDataBlockPreferredDataLocation();
		}
		final FileDialog fd = this.application.openFileOpenDialog(Messages.getString(MessageIds.GDE_MSGT2400), new String[] { this.getDeviceConfiguration().getDataBlockPreferredFileExtention(),
				GDE.FILE_ENDING_STAR_STAR }, searchDirectory, null, SWT.MULTI);
		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				for (String tmpFileName : fd.getFileNames()) {
					String selectedImportFile = fd.getFilterPath() + GDE.FILE_SEPARATOR_UNIX + tmpFileName;
					if (!selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_BIN)) {
						if (selectedImportFile.contains(GDE.STRING_DOT)) {
							selectedImportFile = selectedImportFile.substring(0, selectedImportFile.indexOf(GDE.STRING_DOT));
						}
						selectedImportFile = selectedImportFile + GDE.FILE_ENDING_DOT_BIN;
					}
					log.log(java.util.logging.Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

					if (fd.getFileName().length() > 4) {
						Integer channelConfigNumber = HoTTAdapter.this.application.getActiveChannelNumber();
						channelConfigNumber = channelConfigNumber == null ? 1 : channelConfigNumber;
						//String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.STRING_DOT) - 4, selectedImportFile.lastIndexOf(GDE.STRING_DOT));
						try {
							HoTTbinReader.read(selectedImportFile); //, HoTTAdapter.this, GDE.STRING_EMPTY, channelConfigNumber);
							WaitTimer.delay(500);
						}
						catch (Exception e) {
							log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
						}
					}
				}
			}
		};
		reader.start();
	}

	/**
	 * update the file menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	public void updateFileMenu(Menu exportMenue) {
		MenuItem convertKMZ3DRelativeItem;
		MenuItem convertKMZDAbsoluteItem;
		//		MenuItem convert2GPXRelativeItem;
		//		MenuItem convert2GPXAbsoluteItem;

		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {
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
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});
		}
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE
	 */
	public void export2KMZ3D(int type) {
		//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGT2403), 2, 1, 3, 6, 5, 9, -1, type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
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
				//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
				containsGPSdata = activeRecordSet.get(1).hasReasonableData() && activeRecordSet.get(2).hasReasonableData();
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
				//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
				exportFileName = new FileHandler().exportFileKMZ(2, 1, 3, 6, 5, 9, -1, true, isExport2TmpDir);
			}
		}
		return exportFileName;
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
		//0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		return 6;
	}
}
