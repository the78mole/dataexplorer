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
    
    Copyright (c) 2012,2013,2014,2015,2016 Winfried Bruegmann
****************************************************************************************/
package gde.device.junsi;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DataBlockType;
import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.io.CSVSerialDataReaderWriter;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.utils.FileUtils;
import gde.utils.WaitTimer;

import java.io.FileNotFoundException;

import javax.usb.UsbClaimException;
import javax.usb.UsbException;
import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Junsi iCharger 34010DUO device class
 * @author Winfried Brügmann
 */
public class iCharger4010DUO extends iCharger {

	protected boolean							isFileIO		= false;
	protected boolean							isSerialIO	= false;
	final iChargerUsbPort					usbPort;
	protected UsbGathererThread   dataGatherThread;

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public iCharger4010DUO(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.junsi.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.BATTERIE_TYPE = new String[] { "? - ",      //unknown batteries type
				Messages.getString(MessageIds.GDE_MSGT2611), //LiPo
				Messages.getString(MessageIds.GDE_MSGT2612), //LiIo
				Messages.getString(MessageIds.GDE_MSGT2613), //LiFe
				Messages.getString(MessageIds.GDE_MSGT2614), //NiMH
				Messages.getString(MessageIds.GDE_MSGT2615), //NiCd
				Messages.getString(MessageIds.GDE_MSGT2616), //Pb
				Messages.getString(MessageIds.GDE_MSGT2617), //NiZn
				"? - " };//unknown batterie type

		if (this.application.getMenuToolBar() != null) {
			for (DataBlockType.Format format : this.getDataBlockType().getFormat()) {
				if (!isSerialIO) isSerialIO = format.getInputType() == InputTypes.SERIAL_IO;
				if (!isFileIO) isFileIO = format.getInputType() == InputTypes.FILE_IO;
			}
			if (isSerialIO) { //InputTypes.SERIAL_IO has higher relevance  
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, Messages.getString(MessageIds.GDE_MSGT2606), Messages.getString(MessageIds.GDE_MSGT2605));
			} else { //InputTypes.FILE_IO
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2603), Messages.getString(MessageIds.GDE_MSGT2603));
			}
			if (isFileIO)
				updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
		
		this.usbPort = new iChargerUsbPort(this, this.application);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public iCharger4010DUO(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.junsi.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.BATTERIE_TYPE = new String[] { "? - ",      //unknown batteries type
				Messages.getString(MessageIds.GDE_MSGT2611), //LiPo
				Messages.getString(MessageIds.GDE_MSGT2612), //LiIo
				Messages.getString(MessageIds.GDE_MSGT2613), //LiFe
				Messages.getString(MessageIds.GDE_MSGT2614), //NiMH
				Messages.getString(MessageIds.GDE_MSGT2615), //NiCd
				Messages.getString(MessageIds.GDE_MSGT2616), //Pb
				Messages.getString(MessageIds.GDE_MSGT2617), //NiZn
				"? - " };//unknown batterie type

		if (this.application.getMenuToolBar() != null) {
			for (DataBlockType.Format format : this.getDataBlockType().getFormat()) {
				if (!isSerialIO) isSerialIO = format.getInputType() == InputTypes.SERIAL_IO;
				if (!isFileIO) isFileIO = format.getInputType() == InputTypes.FILE_IO;
			}
			if (isSerialIO) { //InputTypes.SERIAL_IO has higher relevance  
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, Messages.getString(MessageIds.GDE_MSGT2606), Messages.getString(MessageIds.GDE_MSGT2605));
			} else { //InputTypes.FILE_IO
				this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2603), Messages.getString(MessageIds.GDE_MSGT2603));
			}
			if (isFileIO)
				updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
		
		this.usbPort = new iChargerUsbPort(this, this.application);
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
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT2604, GDE.MOD1));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT2604));
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
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	@Override
	public int getLovDataByteSize() {
		return 64+8+5;  
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
		int deviceDataBufferSize = Math.abs(this.getDataBlockSize(InputTypes.SERIAL_IO));
		int[] points = new int[this.getNumberOfMeasurements(1)];
		int offset = 0;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();
		byte[] convertBuffer = new byte[deviceDataBufferSize];

		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
			//prepare convert buffer for conversion
			System.arraycopy(dataBuffer, offset+5, convertBuffer, 0, deviceDataBufferSize);
			recordSet.addPoints(convertDataBytes(points, convertBuffer));
			offset += lovDataSize;

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
		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;
		
		//2C 10 01 F8 7D 07 00 02 01 00 E5 FF 04 87 2B 3C CF FF FF FF 64 01 00 00 02 0F 0B 0F 0E 0F 00 0F 00 00 00 00 00 00 00 00 00 00 00 
	  //                                    34564 15403             35,6        3842  3851  3854  3840
		//0=Strom 1=VersorgungsSpg. 2=Spannung   
		points[0] = DataParser.parse2Short(dataBuffer, 10);			
		points[1] = DataParser.parse2UnsignedShort(dataBuffer, 12);
		points[2] = DataParser.parse2Short(dataBuffer, 14);
		//3=Ladung 4=Leistung 5=Energie
		points[3] = DataParser.parse2Int(dataBuffer, 16);
		points[4] = points[0] * points[2] / 100; 							// power U*I [W]
		points[5] = Double.valueOf(points[2]/1.0 * points[3]/1000.0).intValue();					// energy U*C [mWh]
		//6=Temp.intern 7=Temp.extern 
		points[6] = DataParser.parse2Short(dataBuffer, 20);
		points[7] = DataParser.parse2Short(dataBuffer, 22);
		//9=SpannungZelle1 10=SpannungZelle2 11=SpannungZelle3 12=SpannungZelle4 13=SpannungZelle5 14=SpannungZelle6 15=SpannungZelle7 16=SpannungZelle8
		for (int i = 0,j=0; i < 10; i++,j+=2) {
			points[i+9] = DataParser.parse2Short(dataBuffer, 24+j);
			if (points[i + 9] > 0) {
				maxVotage = points[i + 9] > maxVotage ? points[i + 9] : maxVotage;
				minVotage = points[i + 9] < minVotage ? points[i + 9] : minVotage;
			}
		}
		//8=Balance
		points[8] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

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
			log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i*dataBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i*dataBufferSize, convertBuffer, 0, dataBufferSize);
			
			//0=VersorgungsSpg. 1=Spannung 2=Strom  
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
			points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
			points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff) << 0));
			//3=Ladung 4=Leistung 5=Energie
			points[3] = (((convertBuffer[12]&0xff) << 24) + ((convertBuffer[13]&0xff) << 16) + ((convertBuffer[14]&0xff) << 8) + ((convertBuffer[15]&0xff) << 0));
			points[4] = Double.valueOf((points[1] / 1000.0) * (points[2] / 1000.0) * 10000).intValue(); 						// power U*I [W]
			points[5] = Double.valueOf((points[1] / 1000.0) * (points[3] / 1000.0)).intValue();											// energy U*C [mWh]
			//6=Temp.intern 7=Temp.extern 
			points[6] = (((convertBuffer[16]&0xff) << 24) + ((convertBuffer[17]&0xff) << 16) + ((convertBuffer[18]&0xff) << 8) + ((convertBuffer[19]&0xff) << 0));
			points[7] = (((convertBuffer[20]&0xff) << 24) + ((convertBuffer[21]&0xff) << 16) + ((convertBuffer[22]&0xff) << 8) + ((convertBuffer[23]&0xff) << 0));

			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			//7=SpannungZelle1 8=SpannungZelle2 9=SpannungZelle3 10=SpannungZelle4 11=SpannungZelle5 12=SpannungZelle6 13=SpannungZelle7 14=SpannungZelle8
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
	 * query number of Lithium cells of this charger device
	 * @return
	 */
	@Override
	public int getNumberOfLithiumCells() {
		return 10;
	}

	/**
	 * query the batteries type BATTERY_TYPE 1=LiPo 2=LiIo 3=LiFe 4=NiMH 5=NiCd 6=Pb 7=NiZn
	 * @param databuffer
	 * @return
	 */
	public String getBattrieType(final byte[] databuffer) {
		return this.BATTERIE_TYPE[databuffer[8]];
	}
	
	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */

	@Override	public void open_closeCommPort() {
		switch (application.getMenuBar().getSerialPortIconSet()) {
		case DeviceCommPort.ICON_SET_IMPORT_CLOSE:
			importDeviceData();
			break;
			
		case DeviceCommPort.ICON_SET_START_STOP:
			if (this.usbPort != null) {
				if (!this.usbPort.isConnected()) {
					try {
						Channel activChannel = Channels.getInstance().getActiveChannel();
						if (activChannel != null) {
							this.dataGatherThread = new UsbGathererThread(this.application, this, this.usbPort, activChannel.getNumber());
							try {
								if (this.dataGatherThread != null && this.usbPort.isConnected()) {
									WaitTimer.delay(100);
									this.dataGatherThread.start();
								}
							}
							catch (Throwable e) {
								log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
							}
						}
					}
					catch (UsbClaimException e) {
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0051, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
						try {
							if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
						}
						catch (UsbException ex) {
							log.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
						}
					}
					catch (UsbException e) {
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0051, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
						try {
							if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
						}
						catch (UsbException ex) {
							log.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
						}
					}
					catch (ApplicationConfigurationException e) {
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						this.application.openMessageDialog(Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
						this.application.getDeviceSelectionDialog().open();
					}
					catch (Throwable e) {
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					}
				}
				else {
					if (this.dataGatherThread != null) {
						this.dataGatherThread.stopDataGatheringThread(false, null);
					}
					//if (this.boundsComposite != null && !this.isDisposed()) this.boundsComposite.redraw();
					try {
						WaitTimer.delay(1000);
						if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
					}
					catch (UsbException e) {
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					}
				}
			}
			break;
		}
	}

	/**
	 * import device specific *.bin data files
	 */
	public void importDeviceData() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT2600));

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.FILE_SEPARATOR_UNIX + tmpFileName;
						log.log(Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							try {
								String recordNameExtend;
								try {
									recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.STRING_DOT)-4, selectedImportFile.lastIndexOf(GDE.STRING_DOT));
									Integer.valueOf(recordNameExtend);
								}
								catch (Exception e) {
									try {
										recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.STRING_DOT)-3, selectedImportFile.lastIndexOf(GDE.STRING_DOT));
										Integer.valueOf(recordNameExtend);
									}
									catch (Exception e1) {
										recordNameExtend = GDE.STRING_EMPTY;
									}
								}
								CSVSerialDataReaderWriter.read(selectedImportFile, iCharger4010DUO.this, recordNameExtend, 1, 
										new  DataParserDuo(getDataBlockTimeUnitFactor(), getDataBlockLeader(), getDataBlockSeparator().value(), null, null, Math.abs(getDataBlockSize(InputTypes.FILE_IO)), getDataBlockFormat(InputTypes.FILE_IO), false, 2)
//										new DataParserDuo(getDataBlockTimeUnitFactor(), 
//												getDataBlockLeader(), getDataBlockSeparator().value(), 
//												getDataBlockCheckSumType(), getDataBlockSize(InputTypes.FILE_IO), 2)
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
				//0=Strom 1=VersorgungsSpg. 2=Spannung 3=Ladung 4=Leistung 5=Energie 6=Temp.intern 7=Temp.extern 8=Balance
				//9=SpannungZelle1 10=SpannungZelle2 11=SpannungZelle3 12=SpannungZelle4 13=SpannungZelle5 14=SpannungZelle6 15=SpannungZelle7 16=SpannungZelle8 17=Widerstand
				int displayableCounter = 0;

				Record recordCurrent = recordSet.get(0);
				Record recordVoltage = recordSet.get(2);
				Record recordCapacity = recordSet.get(3);
				Record recordPower = recordSet.get(4);
				Record recordEnergy = recordSet.get(5);
				Record recordBalance = recordSet.get(8);

				recordPower.clear();
				recordEnergy.clear();
				recordBalance.clear();

				for (int i = 0; i < recordCurrent.size(); i++) {
					//4=Leistung 5=Energie
					recordPower.add(recordVoltage.get(i) * recordCurrent.get(i) / 100); // power U*I [W]
					recordEnergy.add(recordVoltage.get(i) * recordCapacity.get(i) / 100); // energy U*C [mWh]

					int maxVotage = Integer.MIN_VALUE;
					int minVotage = Integer.MAX_VALUE;
					for (int j = 0; j < this.getNumberOfLithiumCells(); j++) {

						int value = recordSet.get(j + 9).get(i);
						if (value > 0) {
							maxVotage = value > maxVotage ? value : maxVotage;
							minVotage = value < minVotage ? value : minVotage;
						}
					}
					//8=Balance
					recordBalance.add(maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0);

				}

				// check if measurements isActive == false and set to isDisplayable == false
				for (Record record : recordSet.values()) {
					if (record.isActive() && record.hasReasonableData()) {
						++displayableCounter;
					}
				}

				log.log(Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
				recordSet.setConfiguredDisplayable(displayableCounter);

				if (this.channels.getActiveChannel().getActiveRecordSet() == null || recordSet.getName().equals(this.channels.getActiveChannel().getActiveRecordSet().getName())) {
					this.application.updateGraphicsWindow();
				}
			}
			catch (RuntimeException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
}
