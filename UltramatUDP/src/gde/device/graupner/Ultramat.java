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
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.io.LogViewReader;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;

/**
 * eStation base device class
 * @author Winfried Brügmann
 */
public class Ultramat extends DeviceConfiguration implements IDevice {
	final static Logger						log	= Logger.getLogger(Ultramat.class.getName());
	
	public	String[]	USAGE_MODE;
	public	String[]	CHARGE_MODE;
	public	String[]	DISCHARGE_MODE;
	public	String[]	DELAY_MODE;
	public	String[]	CURRENT_MODE;
	public	String[]	ERROR_MODE;

	protected final DataExplorer									application;
	protected final UltramatSerialPort						serialPort;
	protected final Channels											channels;
	protected UltraDuoPlusDialog									dialog;

	protected HashMap<String, CalculationThread>	calculationThreads	= new HashMap<String, CalculationThread>();

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public Ultramat(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);		Messages.setDeviceResourceBundle("gde.device.htronic.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2200), Messages.getString(MessageIds.GDE_MSGT2201), Messages.getString(MessageIds.GDE_MSGT2202), Messages.getString(MessageIds.GDE_MSGT2203), Messages.getString(MessageIds.GDE_MSGT2204), Messages.getString(MessageIds.GDE_MSGT2205), Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2207), Messages.getString(MessageIds.GDE_MSGT2208), Messages.getString(MessageIds.GDE_MSGT2209)};
		this.CHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212), Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2214), Messages.getString(MessageIds.GDE_MSGT2215), Messages.getString(MessageIds.GDE_MSGT2216), Messages.getString(MessageIds.GDE_MSGT2217), Messages.getString(MessageIds.GDE_MSGT2218), Messages.getString(MessageIds.GDE_MSGT2219), Messages.getString(MessageIds.GDE_MSGT2220), Messages.getString(MessageIds.GDE_MSGT2221), Messages.getString(MessageIds.GDE_MSGT2222)};
		this.DISCHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212), Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2214), Messages.getString(MessageIds.GDE_MSGT2223), Messages.getString(MessageIds.GDE_MSGT2224), Messages.getString(MessageIds.GDE_MSGT2221), Messages.getString(MessageIds.GDE_MSGT2222)};
		this.DELAY_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2225), Messages.getString(MessageIds.GDE_MSGT2226), Messages.getString(MessageIds.GDE_MSGT2227), Messages.getString(MessageIds.GDE_MSGT2228)}; 
		this.CURRENT_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2229), Messages.getString(MessageIds.GDE_MSGT2230), Messages.getString(MessageIds.GDE_MSGT2231), Messages.getString(MessageIds.GDE_MSGT2232), Messages.getString(MessageIds.GDE_MSGT2233), Messages.getString(MessageIds.GDE_MSGT2234), Messages.getString(MessageIds.GDE_MSGT2235), Messages.getString(MessageIds.GDE_MSGT2236),  Messages.getString(MessageIds.GDE_MSGT2237)};

		this.application = DataExplorer.getInstance();
		this.serialPort = new UltramatSerialPort(this, this.application);
		this.channels = Channels.getInstance();
		if (this.application.getMenuToolBar() != null) this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		this.dialog = new UltraDuoPlusDialog(this.application.getShell(), this);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public Ultramat(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.USAGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2200), Messages.getString(MessageIds.GDE_MSGT2201), Messages.getString(MessageIds.GDE_MSGT2202), Messages.getString(MessageIds.GDE_MSGT2203), Messages.getString(MessageIds.GDE_MSGT2204), Messages.getString(MessageIds.GDE_MSGT2205), Messages.getString(MessageIds.GDE_MSGT2206), Messages.getString(MessageIds.GDE_MSGT2207), Messages.getString(MessageIds.GDE_MSGT2208), Messages.getString(MessageIds.GDE_MSGT2209)};
		this.CHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212), Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2214), Messages.getString(MessageIds.GDE_MSGT2215), Messages.getString(MessageIds.GDE_MSGT2216), Messages.getString(MessageIds.GDE_MSGT2217), Messages.getString(MessageIds.GDE_MSGT2218), Messages.getString(MessageIds.GDE_MSGT2219), Messages.getString(MessageIds.GDE_MSGT2220), Messages.getString(MessageIds.GDE_MSGT2221), Messages.getString(MessageIds.GDE_MSGT2222)};
		this.DISCHARGE_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2210), Messages.getString(MessageIds.GDE_MSGT2211), Messages.getString(MessageIds.GDE_MSGT2212), Messages.getString(MessageIds.GDE_MSGT2213), Messages.getString(MessageIds.GDE_MSGT2214), Messages.getString(MessageIds.GDE_MSGT2223), Messages.getString(MessageIds.GDE_MSGT2224), Messages.getString(MessageIds.GDE_MSGT2221), Messages.getString(MessageIds.GDE_MSGT2222)};
		this.DELAY_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2225), Messages.getString(MessageIds.GDE_MSGT2226), Messages.getString(MessageIds.GDE_MSGT2227), Messages.getString(MessageIds.GDE_MSGT2228)}; 
		this.CURRENT_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT2229), Messages.getString(MessageIds.GDE_MSGT2230), Messages.getString(MessageIds.GDE_MSGT2231), Messages.getString(MessageIds.GDE_MSGT2232), Messages.getString(MessageIds.GDE_MSGT2233), Messages.getString(MessageIds.GDE_MSGT2234), Messages.getString(MessageIds.GDE_MSGT2235), Messages.getString(MessageIds.GDE_MSGT2236),  Messages.getString(MessageIds.GDE_MSGT2237)};

		this.application = DataExplorer.getInstance();
		this.serialPort = new UltramatSerialPort(this, this.application);
		this.channels = Channels.getInstance();
		this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		this.dialog = new UltraDuoPlusDialog(this.application.getShell(), this);
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
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
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// ...
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	public int getLovDataByteSize() {
		return 150;  
	}
	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
	 * this method is more usable for real log, where data can be stored and converted in one block
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int deviceDataBufferSize = this.getDataBlockSize(); // const.
		int[] points = new int[this.getNumberOfMeasurements(1)];
		int offset = 4;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();
		long lastDateTime = 0, sumTimeDelta = 0, deltaTime = 0; 
		//int outputChannel = recordSet.getChannelConfigNumber(); 
		
		if (dataBuffer[offset] == 0x0C) {
			byte[] convertBuffer = new byte[deviceDataBufferSize];
			if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

			for (int i = 0; i < recordDataSize; i++) {
				System.arraycopy(dataBuffer, offset + i * lovDataSize, convertBuffer, 0, deviceDataBufferSize);
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
				String timeStamp = new String(timeBuffer).substring(0, timeBuffer.length-8)+"0000000000";
				long dateTime = new Long(timeStamp.substring(6,17));
				log.log(Level.FINEST, timeStamp + " " + timeStamp.substring(6,17) + " " + dateTime);
				sb.append(dateTime);
				//System.arraycopy(dataBuffer, offset - 4, sizeBuffer, 0, 4);
				//sb.append(" ? ").append(LogViewReader.parse2Int(sizeBuffer));
				deltaTime = lastDateTime == 0 ? 0 : (dateTime - lastDateTime)/1000 - 217; // value 217 is a compromis manual selected
				sb.append(" - ").append(deltaTime);
				sb.append(" - ").append(sumTimeDelta += deltaTime);
				log.log(Level.FINER, sb.toString());
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
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {		
		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;
		
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

		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle6 15=SpannungZelle7
		for (int i=0, j=0; i<points.length - 8; ++i, j+=4) {
			points[i + 8]  = Integer.parseInt(String.format("%c%c%c%c", (char) dataBuffer[41+j], (char) dataBuffer[42+j], (char) dataBuffer[43+j], (char) dataBuffer[44+j]), 16);
			if (points[i + 8] > 0) {
				maxVotage = points[i + 8] > maxVotage ? points[i + 8] : maxVotage;
				minVotage = points[i + 8] < minVotage ? points[i + 8] : minVotage;
			}
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
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.size()];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
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
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;

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
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		updateVisibilityStatus(recordSet, true);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	public String[] prepareDataTableRow(RecordSet recordSet, int rowIndex) {
		String[] dataTableRow = new String[recordSet.size()+1]; // this.device.getMeasurementNames(this.channelNumber).length
		try {
			String[] recordNames = recordSet.getRecordNames();				
			// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance 
			// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle6 15=SpannungZelle7
			// 16=SpannungZelle8 17=SpannungZelle9 18=SpannungZelle10 19=SpannungZelle11 20=SpannungZelle12 21=SpannungZelle13 22=SpannungZelle14
			int numberRecords = recordNames.length;			
			
			dataTableRow[0] = String.format("%.3f", (recordSet.getTime_ms(rowIndex) / 1000.0));
			for (int j = 0; j < numberRecords; j++) {
				Record record = recordSet.get(recordNames[j]);
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				if(j > 5 && record.getUnit().equals("V")) //cell voltage BC6 no temperature measurements
					dataTableRow[j + 1] = String.format("%.3f", (((record.get(rowIndex) / 1000.0) - reduction) * factor));
				else
					dataTableRow[j + 1] = record.getDecimalFormat().format((((record.get(rowIndex) / 1000.0) - reduction) * factor));
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;		
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double translateValue(Record record, double value) {
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance 
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle6 15=SpannungZelle7
		// 16=SpannungZelle8 17=SpannungZelle9 18=SpannungZelle10 19=SpannungZelle11 20=SpannungZelle12 21=SpannungZelle13 22=SpannungZelle14
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required
		
		double newValue = value * factor + offset;
		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	public double reverseTranslateValue(Record record, double value) {
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance 
		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle6 15=SpannungZelle7
		// 16=SpannungZelle8 17=SpannungZelle9 18=SpannungZelle10 19=SpannungZelle11 20=SpannungZelle12 21=SpannungZelle13 22=SpannungZelle14
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double factor = record.getFactor(); // != 1 if a unit translation is required

		double newValue = value / factor - offset;
		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		String[] recordKeys = recordSet.getRecordNames();

		recordSet.setAllDisplayable();
//		for (String recordKey : recordSet.getNoneCalculationRecordNames()) {
//			recordSet.get(recordKey).setActive(true);
//		}
		int numCells = recordSet.getChannelConfigNumber() < 3 ? 7 : 14;
		for (int i=recordKeys.length-numCells-1; i<recordKeys.length; ++i) {
				Record record = recordSet.get(recordKeys[i]);
				record.setDisplayable(record.getOrdinal() <= 5 || record.hasReasonableData());
				log.log(Level.FINER, recordKeys[i] + " setDisplayable=" + (record.getOrdinal() <= 5 || record.hasReasonableData()));
		}
		
		if (log.isLoggable(Level.FINE)) {
			for (String recordKey : recordKeys) {
				Record record = recordSet.get(recordKey);
				log.log(Level.FINE, recordKey + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable());
			}
		}
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are live measurement points only the calculation will take place directly after switch all to displayable
		if (recordSet.isRaw()) {
			// calculate the values required
			try {
				// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature 6=VersorgungsSpg 7=Balance 
				// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 14=SpannungZelle6 15=SpannungZelle7
				// 16=SpannungZelle8 17=SpannungZelle9 18=SpannungZelle10 19=SpannungZelle11 20=SpannungZelle12 21=SpannungZelle13 22=SpannungZelle14
				String[] recordNames = recordSet.getRecordNames();
				int displayableCounter = 0;

				
				// check if measurements isActive == false and set to isDisplayable == false
				for (String measurementKey : recordNames) {
					Record record = recordSet.get(measurementKey);
					
					if (record.isActive() && (record.getOrdinal() <= 5 || record.getRealMaxValue() != 0 || record.getRealMinValue() != record.getRealMaxValue())) {
						++displayableCounter;
					}
				}
				
				String recordKey = recordNames[3]; //3=Leistung
				Record record = recordSet.get(recordKey);
				if (record != null && (record.size() == 0 || (record.getRealMinValue() == 0 && record.getRealMaxValue() == 0))) {
					this.calculationThreads.put(recordKey, new CalculationThread(recordKey, this.channels.getActiveChannel().getActiveRecordSet()));
					try {
						this.calculationThreads.get(recordKey).start();
					}
					catch (RuntimeException e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
				}
				++displayableCounter;
				
				recordKey = recordNames[4]; //4=Energie
				record = recordSet.get(recordKey);
				if (record != null && (record.size() == 0 || (record.getRealMinValue() == 0 && record.getRealMaxValue() == 0))) {
					this.calculationThreads.put(recordKey, new CalculationThread(recordKey, this.channels.getActiveChannel().getActiveRecordSet()));
					try {
						this.calculationThreads.get(recordKey).start();
					}
					catch (RuntimeException e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
				}		
				++displayableCounter;
				
				log.log(Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
				recordSet.setConfiguredDisplayable(displayableCounter);		

				if (recordSet.getName().equals(this.channels.getActiveChannel().getActiveRecordSet().getName())) {
					this.application.updateGraphicsWindow();
				}
			}
			catch (RuntimeException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * @return the serialPort
	 */
	@Override
	public UltramatSerialPort getCommunicationPort() {
		return this.serialPort;
	}

	/**
	 * @return the device specific dialog instance
	 */
	@Override
	public UltraDuoPlusDialog getDialog() {
		return this.dialog;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	public String[] getUsedPropertyKeys() {
		return new String[] {IDevice.OFFSET, IDevice.FACTOR};
	}
	
	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 */
	@Override
	public void open_closeCommPort() {
		if (this.serialPort != null) {
			if (!this.serialPort.isConnected()) {
				try {
					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						GathererThread dataGatherThread = GathererThread.getInstance();
						try {
							if (this.serialPort.isConnected()) {
								dataGatherThread.start();
							}
						}
						catch (RuntimeException e) {
							log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						}
						catch (Throwable e) {
							log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						}
					}
				}
				catch (SerialPortException e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog != null ? this.dialog.getDialogShell() : null,
							Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
				}
				catch (ApplicationConfigurationException e) {
					this.application.openMessageDialog(this.dialog != null ? this.dialog.getDialogShell() : null, Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
					this.application.getDeviceSelectionDialog().open();
				}
				catch (Throwable e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
				}
			}
			else {
				if (GathererThread.isInstance()) {
					try {
						GathererThread.getInstance().stopDataGatheringThread(false, null);
					}
					catch (Exception e) {
						// ignore, while stopping no exception will be thrown
					}
				}
				this.serialPort.close();
			}
		}
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
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=BatteryTemperature1 6=BatteryTemperature2 7=VersorgungsSpg1 8=VersorgungsSpg2 9=Balance 
		// 10=SpannungZelle1 11=SpannungZelle2 12=SpannungZelle3 13=SpannungZelle4 14=SpannungZelle5 15=SpannungZelle6 16=SpannungZelle7 
		// 17=SpannungZelle8 18=SpannungZelle9 19=SpannungZelle10 20=SpannungZelle11 21=SpannungZelle12 22=SpannungZelle13 23=SpannungZelle14
		// 24=Spannung1 25=Spannung2 26=Strom1 27=Strom2 28=Ladung1 29=Ladung2 30=Leistung1 31=Leistung2 32=Energie1 33=Energie2 
		return new int[] {0, 2};
	}

	/**
	 * check if one of the outlet channels are in processing mode
	 * @param outletNum 1 or 2
	 * @param dataBuffer
	 * @return true if channel 1 or 2 is active 
	 */
	public boolean isProcessing(int outletNum, byte[] dataBuffer) {
		if(outletNum == 1) {
			String operationModeOut1 = String.format("%c%c", (char) dataBuffer[15], (char) dataBuffer[16]);
			if(log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "operationModeOut1 = " + (operationModeOut1 != null && operationModeOut1.length() > 0 ? USAGE_MODE[Integer.parseInt(operationModeOut1)] : operationModeOut1));
			}
			return operationModeOut1 != null && operationModeOut1.length() == 2 && !(operationModeOut1.equals("00") || operationModeOut1.equals("06"));
		}
		else if (outletNum == 2) {
			String operationModeOut2 = String.format("%c%c", (char) dataBuffer[79], (char) dataBuffer[80]);
			if(log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "operationModeOut2 = " + (operationModeOut2 != null && operationModeOut2.length() > 0 ? USAGE_MODE[Integer.parseInt(operationModeOut2)] : operationModeOut2));
			}
			return operationModeOut2 != null && operationModeOut2.length() == 2 && !(operationModeOut2.equals("00") || operationModeOut2.equals("06"));
		}
		else 
			return false;
	}

	/**
	 * query the processing mode, main modes are charge/discharge, make sure the data buffer contains at index 15,16 the processing modes
	 * @param dataBuffer 
	 * @return 0 = no processing, 1 = charge, 2 = discharge, 3 = delay, 4 = pause, 5 = current operation finished, 6 = error, 7 = balancer, 8 = tire heater, 9 = motor
	 */
	public int getProcessingMode(byte[] dataBuffer) {
		String operationMode = String.format("%c%c", (char) dataBuffer[15], (char) dataBuffer[16]);
		return operationMode != null && operationMode.length() > 0 ? Integer.parseInt(operationMode) : 0;
	}

	/**
	 * query the charge mode, main modes are automatic/normal/CVCC, make sure the data buffer contains at index 15,16 the processing modes, type at index 17,18
	 * @param dataBuffer
	 * @return string of charge mode
	 */
	public String getProcessingType(byte[] dataBuffer) {
		String type = GDE.STRING_EMPTY;
		String operationMode = String.format("%c%c", (char) dataBuffer[15], (char) dataBuffer[16]);
		int opMode = operationMode != null && operationMode.length() > 0 ? Integer.parseInt(operationMode) : 0;
		String operationType = String.format("%c%c", (char) dataBuffer[17], (char) dataBuffer[18]);
		switch (opMode) {
		case 1: //charge
			type = operationType != null && operationType.length() > 0 ? CHARGE_MODE[Integer.parseInt(operationType)] : GDE.STRING_EMPTY;
			break;
		case 2: //discharge
			type = operationType != null && operationType.length() > 0 ? DISCHARGE_MODE[Integer.parseInt(operationType)] : GDE.STRING_EMPTY;
			break;
		case 3: //delay
			type = operationType != null && operationType.length() > 0 ? DELAY_MODE[Integer.parseInt(operationType)] : GDE.STRING_EMPTY;
			break;
		case 5: //last current 
			type = operationType != null && operationType.length() > 0 ? CURRENT_MODE[Integer.parseInt(operationType)] : GDE.STRING_EMPTY;
			break;
		case 6: //error
			this.application.setStatusMessage(Messages.getString("GDE_MSGT22"+operationType), SWT.COLOR_RED);
			break;
		}
		return type;
	}

	/**
	 * query if outlets are linked together to charge identical batteries in parallel
	 * @param dataBuffer
	 * @return true | false
	 */
	public boolean isLinkedMode(byte[] dataBuffer) {
		String operationMode1 = String.format("%c%c", (char) dataBuffer[15], (char) dataBuffer[16]);
		String operationMode2 = String.format("%c%c", (char) dataBuffer[79], (char) dataBuffer[80]);
		String operationType1 = String.format("%c%c", (char) dataBuffer[17], (char) dataBuffer[18]);
		String operationType2 = String.format("%c%c", (char) dataBuffer[81], (char) dataBuffer[82]);
		return operationMode1.equals(operationMode2) && operationType1.equals(operationType2) && (operationType1.equals("09") || operationType1.equals("05"));
	}
	
	public void setTemperatureUnit(RecordSet recordSet, byte[] dataBuffer) {
		String unit = String.format("%c%c", (char) dataBuffer[39], (char) dataBuffer[40]);
		if (unit != null && unit.length() > 0)
			if (Integer.parseInt(unit) == 0)      this.setMeasurementUnit(recordSet.getChannelConfigNumber(), 5, "°C");
			else if (Integer.parseInt(unit) == 1) this.setMeasurementUnit(recordSet.getChannelConfigNumber(), 5, "°F");
	}
}
