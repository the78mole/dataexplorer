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
    
    Copyright (c) 2016,2017 Winfried Bruegmann
****************************************************************************************/
package gde.device.junsi;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DataBlockType;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.io.CSVSerialDataReaderWriter;
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
 * iCharger base class for USB communicating iCharger devices
 */
public abstract class iChargerUsb extends iCharger implements IDevice {

	protected final iChargerUsbPort	usbPort;
	protected boolean								isFileIO		= false;
	protected boolean								isSerialIO	= false;
	protected UsbGathererThread			dataGatherThread;

	/**
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public iChargerUsb(String deviceProperties) throws FileNotFoundException, JAXBException {
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
	 * @param deviceConfig
	 */
	public iChargerUsb(DeviceConfiguration deviceConfig) {
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
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	@Override	
	public void open_closeCommPort() {
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
								CSVSerialDataReaderWriter.read(selectedImportFile, iChargerUsb.this, recordNameExtend, 1, 
										new  DataParserDuo(getDataBlockTimeUnitFactor(), getDataBlockLeader(), getDataBlockSeparator().value(), null, null, Math.abs(getDataBlockSize(InputTypes.FILE_IO)), getDataBlockFormat(InputTypes.FILE_IO), false, 2)
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
	 * query the batteries type BATTERY_TYPE 1=LiPo 2=LiIo 3=LiFe 4=NiMH 5=NiCd 6=Pb 7=NiZn
	 * @param databuffer
	 * @return
	 */
	public String getBattrieType(final byte[] databuffer) {
		return this.BATTERIE_TYPE[databuffer[8]];
	}
}
