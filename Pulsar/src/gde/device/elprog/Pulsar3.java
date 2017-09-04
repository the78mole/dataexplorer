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
    
    Copyright (c) 2017 Winfried Bruegmann
****************************************************************************************/
package gde.device.elprog;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DataBlockType;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.exception.SerialPortException;
import gde.io.CSVSerialDataReaderWriter;
import gde.io.LogViewReader;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;

/**
 * @author brueg
 *
 */
public class Pulsar3 extends DeviceConfiguration implements IDevice {
	final static Logger	log	= Logger.getLogger(Pulsar3.class.getName());

	protected final Settings					settings			= Settings.getInstance();
	protected final DataExplorer			application		= DataExplorer.getInstance();
	protected final Channels					channels			= Channels.getInstance(application);
	protected final PulsarSerialPort	serialPort;
	protected PulsarGathererThread		gathererThread;
	protected boolean									isFileIO			= false;
	protected boolean									isSerialIO		= false;
	protected Map<Integer, String>		batteryTypes	= new HashMap<Integer, String>();

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public Pulsar3(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.elprog.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		if (this.application.getMenuToolBar() != null) {
			for (DataBlockType.Format format : this.getDataBlockType().getFormat()) {
				if (!isSerialIO) isSerialIO = format.getInputType() == InputTypes.SERIAL_IO;
				if (!isFileIO) isFileIO = format.getInputType() == InputTypes.FILE_IO;
			}
			if (isSerialIO) { //InputTypes.SERIAL_IO has higher relevance  
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
			} else { //InputTypes.FILE_IO
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT3903), Messages.getString(MessageIds.GDE_MSGT3903));
			}
			if (isFileIO)
				updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}

		this.serialPort = new PulsarSerialPort(this, this.application);
		initBatteryTypes();
		LogViewReader.putDeviceMap("pulsar 3", "Pulsar3"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public Pulsar3(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.elprog.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		if (this.application.getMenuToolBar() != null) {
			for (DataBlockType.Format format : this.getDataBlockType().getFormat()) {
				if (!isSerialIO) isSerialIO = format.getInputType() == InputTypes.SERIAL_IO;
				if (!isFileIO) isFileIO = format.getInputType() == InputTypes.FILE_IO;
			}
			if (isSerialIO) { //InputTypes.SERIAL_IO has higher relevance  
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
			} else { //InputTypes.FILE_IO
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT3903), Messages.getString(MessageIds.GDE_MSGT3903));
			}
			if (isFileIO)
				updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}

		this.serialPort = new PulsarSerialPort(this, this.application);
		initBatteryTypes();
		LogViewReader.putDeviceMap("pulsar 3", "Pulsar3"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * initialize known battery types
	 */
	protected void initBatteryTypes() {
		batteryTypes.put(0, "NiCd");
		batteryTypes.put(1, "NiMH");
		batteryTypes.put(4, "LiIo");
		batteryTypes.put(5, "LiPo");
		batteryTypes.put(7, "LiFe");
	}

	/**
	 * update the file import menu by adding new entry to import device specific files
	 * @param importMenue
	 */
	public void updateFileImportMenu(Menu importMenue) {
		MenuItem importDeviceLogItem;

		if (importMenue.getItem(importMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {			
			new MenuItem(importMenue, SWT.SEPARATOR);

			importDeviceLogItem = new MenuItem(importMenue, SWT.PUSH);
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT3904, GDE.MOD1));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT3904));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					if (!isSerialIO) open_closeCommPort();
					else importDeviceData();
				}
			});
		}
	}

	/**
	 * import device specific *.acp data files
	 */
	public void importDeviceData() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT3900));		

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.FILE_SEPARATOR_UNIX + tmpFileName;
						log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							String recordNameExtend = GDE.STRING_EMPTY;
							try {
								BufferedReader reader = null;
								try {
									reader = new BufferedReader(new InputStreamReader(new FileInputStream(selectedImportFile), "ISO-8859-1")); //$NON-NLS-1$	
									String line = reader.readLine();
									if (line.startsWith("#")) {
										recordNameExtend = batteryTypes.get(Integer.valueOf(line.substring(4, 6)));		
										recordNameExtend = recordNameExtend == null ? GDE.STRING_EMPTY : recordNameExtend;
									}
									reader.close();
								}
								catch (Exception e) {
									//ignore and use empty string as battery type since it is unknown
									if (reader != null) reader.close();
								}
								CSVSerialDataReaderWriter.read(selectedImportFile, Pulsar3.this, recordNameExtend, 1, 
										new  PulsarDataParser(getDataBlockTimeUnitFactor(), getDataBlockLeader(), getDataBlockSeparator().value(), null, null, Math.abs(getDataBlockSize(InputTypes.FILE_IO)), getDataBlockFormat(InputTypes.FILE_IO), false, 2)
								);
							}
							catch (Throwable e) {
								log.log(Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}

	/**
	 * query if the record set numbering should follow channel configuration numbering
	 * @return true where devices does not distinguish between channels (for example Av4ms_FV_762)
	 */
	public boolean recordSetNumberFollowChannel() {
		return false;
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
		return GDE.STRING_BLANK;
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
	public void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int deviceDataBufferSize = this.getLovDataByteSize();
		int[] points = new int[this.getNumberOfMeasurements(1)];
		int offset = 0;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();
		byte[] convertBuffer = new byte[deviceDataBufferSize];

		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
			System.arraycopy(dataBuffer, offset, convertBuffer, 0, deviceDataBufferSize);
			recordSet.addPoints(convertDataBytes(points, convertBuffer));
			offset += lovDataSize+10;

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}

		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {

		PulsarDataParser data = new PulsarDataParser(this.getDataBlockTimeUnitFactor(), this.getDataBlockLeader(), this.getDataBlockSeparator().value(), null, null,
				Math.abs(this.getDataBlockSize(InputTypes.FILE_IO)), this.getDataBlockFormat(InputTypes.SERIAL_IO), false);

		try {
			//System.out.println(new String(dataBuffer));
			byte[] lineBuffer = new byte[160];
			System.arraycopy(dataBuffer, 0, lineBuffer, 0, dataBuffer.length);
			data.parse(new String(lineBuffer), 1);
		}
		catch (DevicePropertiesInconsistenceException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return data.getValues();
	}

	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * since this is a long term operation the progress bar should be updated to signal business to user
	 * @param recordSet
	 * @param dataBuffer
	 * @param doUpdateProgressBar
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
			log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i*dataBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i*dataBufferSize, convertBuffer, 0, dataBufferSize);
			
			//0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temperature 6=Ri 7=Balance
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
			points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
			points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff) << 0));
			//3=Ladung 4=Leistung 5=Energie
			points[3] = (((convertBuffer[12]&0xff) << 24) + ((convertBuffer[13]&0xff) << 16) + ((convertBuffer[14]&0xff) << 8) + ((convertBuffer[15]&0xff) << 0));
			points[4] = Double.valueOf((points[1] / 1000.0) * (points[2] / 1000.0) * 10000).intValue();							// power U*I [W]
			points[5] = Double.valueOf((points[1] / 1000.0) * (points[3] / 1000.0)).intValue();											// energy U*C [mWh]
			//6=Temp.intern 7=Temp.extern 
			points[6] = (((convertBuffer[32]&0xff) << 24) + ((convertBuffer[33]&0xff) << 16) + ((convertBuffer[34]&0xff) << 8) + ((convertBuffer[35]&0xff) << 0));
			points[7] = (((convertBuffer[36]&0xff) << 24) + ((convertBuffer[37]&0xff) << 16) + ((convertBuffer[38]&0xff) << 8) + ((convertBuffer[39]&0xff) << 0));

			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			//8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 ... 23=SpannungZelle16
			//24=BalancerZelle1 25=BalancerZelle2 26=BalancerZelle3 27=BalancerZelle4 28=BalancerZelle5 29=BalancerZelle6 ... 39=BalancerZelle16
			for (int j=0, k=0; j<10; ++j, k+=GDE.SIZE_BYTES_INTEGER) {
				//log_base.info("cell " + (i+1) + " points[" + (i+8) + "]  = new Integer((((dataBuffer[" + (j+45) + "] & 0xFF)-0x80)*100 + ((dataBuffer[" + (j+46)+ "] & 0xFF)-0x80))*10);");  //45,46 CELL_420v[1];
				//log.log(Level.OFF, j + " k+19 = " + (k+19));
				points[j + 9] = (((convertBuffer[k+24]&0xff) << 24) + ((convertBuffer[k+25]&0xff) << 16) + ((convertBuffer[k+26]&0xff) << 8) + ((convertBuffer[k+27]&0xff) << 0));
				if (points[j + 9] > 0) {
					maxVotage = points[j + 9] > maxVotage ? points[j + 9] : maxVotage;
					minVotage = points[j + 9] < minVotage ? points[j + 9] : minVotage;
				}
			}
			//calculate balance on the fly
			points[8] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

			recordSet.addPoints(points);
			
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*2500)/recordDataSize), sThreadId);
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
		//0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temperature 6=Ri 7=Balance
		//8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 ... 23=SpannungZelle16
		//24=BalancerZelle1 25=BalancerZelle2 26=BalancerZelle3 27=BalancerZelle4 28=BalancerZelle5 29=BalancerZelle6 ... 39=BalancerZelle16
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				double factor = record.getFactor(); // != 1 if a unit translation is required
				if(index > 7 && record.getUnit().equals("V"))
					try {
						dataTableRow[index + 1] = String.format("%.3f", ((record.realGet(rowIndex) / 1000.0) * factor)); //$NON-NLS-1$
					}
					catch (Exception e) {
						dataTableRow[index + 1] = String.format("%.3f", ((record.realGet(record.realSize()-1) / 1000.0) * factor)); //$NON-NLS-1$
					}
				else
					try {
						dataTableRow[index + 1] = record.getDecimalFormat().format(((record.realGet(rowIndex) / 1000.0) * factor));
					}
					catch (Exception e) {
						dataTableRow[index + 1] = record.getDecimalFormat().format(((record.realGet(record.realSize()-1) / 1000.0) * factor));
					}
				++index;
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;		
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	@Override
	public double translateValue(Record record, double value) {
		//0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temperature 6=Ri 7=Balance
		//8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 ... 23=SpannungZelle16
		//24=BalancerZelle1 25=BalancerZelle2 26=BalancerZelle3 27=BalancerZelle4 28=BalancerZelle5 29=BalancerZelle6 ... 39=BalancerZelle16
		double factor = record.getFactor(); // != 1 if a unit translation is required
		
		double newValue = value * factor;
		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		//0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temperature 6=Ri 7=Balance
		//8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 ... 23=SpannungZelle16
		//24=BalancerZelle1 25=BalancerZelle2 26=BalancerZelle3 27=BalancerZelle4 28=BalancerZelle5 29=BalancerZelle6 ... 39=BalancerZelle16
		double factor = record.getFactor(); // != 1 if a unit translation is required

		double newValue = value / factor;
		log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to calculate values for inactive record which need to be calculated
	 * at least an update of the graphics window should be included at the end of this method
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are live measurement points only the calculation will take place directly after switch all to displayable
		if (recordSet.isRaw()) {
			// calculate the values required
			try {
				//0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temperature 6=Ri 7=Balance
				//8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 ... 23=SpannungZelle16
				//24=BalancerZelle1 25=BalancerZelle2 26=BalancerZelle3 27=BalancerZelle4 28=BalancerZelle5 29=BalancerZelle6 ... 39=BalancerZelle16
				int displayableCounter = 0;

				
				// check if measurements isActive == false and set to isDisplayable == false
				for (Record record : recordSet.values()) {
					if (record.isActive() && record.hasReasonableData()) {
						++displayableCounter;
					}
				}
				
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
	 * check and update visibility status of all records according the available device configuration
	 * this function must have only implementation code if the device implementation supports different configurations
	 * where some curves are hided for better overview
	 * example: if device supports voltage, current and height and no sensors are connected to voltage and current
	 * it makes less sense to display voltage and current curves, if only height has measurement data
	 * at least an update of the graphics window should be included at the end of this method
	 */
	@Override
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {

		//0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temperature 6=Ri 7=Balance
		//8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 ... 23=SpannungZelle16
		//24=BalancerZelle1 25=BalancerZelle2 26=BalancerZelle3 27=BalancerZelle4 28=BalancerZelle5 29=BalancerZelle6 ... 39=BalancerZelle16
		recordSet.setAllDisplayable();
		for (int i=7; i<recordSet.size(); ++i) {
				Record record = recordSet.get(i);
				record.setDisplayable(record.hasReasonableData());
				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, record.getName() + " setDisplayable=" + record.hasReasonableData()); //$NON-NLS-1$
		}
		
		if (log.isLoggable(Level.FINE)) {
			for (Record record : recordSet.values()) {
				log.log(Level.FINE, record.getName() + " isActive=" + record.isActive() + " isVisible=" + record.isVisible() + " isDisplayable=" + record.isDisplayable()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		return new String[] {IDevice.OFFSET, IDevice.FACTOR};
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data
	 */
	@Override
	public void open_closeCommPort() {
		if (this.serialPort != null) {
			if (!this.serialPort.isConnected()) {
				try {
					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						this.gathererThread = new PulsarGathererThread(this.application, this, this.serialPort, activChannel.getNumber());
						try {
							if (this.serialPort.isConnected()) {
								this.gathererThread.start();
							}
						}
						catch (RuntimeException e) {
							log.log(Level.SEVERE, e.getMessage(), e);
						}
						catch (Throwable e) {
							log.log(Level.SEVERE, e.getMessage(), e);
						}
					}
				}
				catch (SerialPortException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage()}));
				}
				catch (ApplicationConfigurationException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
					this.application.getDeviceSelectionDialog().open();
				}
				catch (Throwable e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			else {
				if (this.gathererThread != null) {
					this.gathererThread.stopDataGatheringThread(false, null);
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
		//0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temperature 6=Ri 7=Balance
		//8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 ... 23=SpannungZelle16
		//24=BalancerZelle1 25=BalancerZelle2 26=BalancerZelle3 27=BalancerZelle4 28=BalancerZelle5 29=BalancerZelle6 ... 39=BalancerZelle16
		return new int[] {0, 2};	}
}
