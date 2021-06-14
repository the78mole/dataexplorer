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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021 Winfried Bruegmann
****************************************************************************************/
package gde.device.skyrc;

import gde.comm.DeviceCommPort;
import gde.comm.IDeviceCommPort;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.Checksum;
import gde.utils.StringHelper;

import java.io.IOException;
import java.util.logging.Logger;

import javax.usb.UsbException;
import javax.usb.UsbInterface;

/**
 * QC-Copter or QuadroConrtol serial port implementation
 * @author Winfried Brügmann
 */
public class MC3000UsbPort extends DeviceCommPort implements IDeviceCommPort {
	final static String $CLASS_NAME = MC3000UsbPort.class.getName();
	final static Logger	log	= Logger.getLogger($CLASS_NAME);
		
  // The vendor ID SKYRC products are actually not registered.
  protected static final short VENDOR_ID = 0x0000;
  // The product ID SKYRC MC3000 
  protected static final short PRODUCT_ID = 0x0001;
  // The SKYRC MC3000 interface address
  protected static final byte INTERFACE_ID = 0x01;
  // The SKYRC MC3000 input end point
  protected static final byte IN_ENDPOINT = (byte) 0x81;
  // The SKYRC MC3000 output end point
  protected static final byte OUT_ENDPOINT = 0x01;

  // The communication timeout in milliseconds. */
  protected static final int TIMEOUT = 1000;
  
  protected final byte interfaceId;
  protected final byte endpointIn;
  protected final byte endpointOut;

	public enum TakeMtuData {
		SLOT_0(new byte[]{0x0f, 0x04, 0x55, 0x00, 0x00, 0x55, (byte) 0xff, (byte) 0xff}), 
		SLOT_1(new byte[]{0x0f, 0x04, 0x55, 0x00, 0x01, 0x56, (byte) 0xff, (byte) 0xff}), 
		SLOT_2(new byte[]{0x0f, 0x04, 0x55, 0x00, 0x02, 0x57, (byte) 0xff, (byte) 0xff}), 
		SLOT_3(new byte[]{0x0f, 0x04, 0x55, 0x00, 0x03, 0x58, (byte) 0xff, (byte) 0xff});
		private byte[]	value;

		private TakeMtuData(byte[] v) {
			this.value = v;
		}

		public byte[] value() {
			return this.value;
		}

		public void setValue(final byte[] newValue) {
			this.value = newValue;
		}
	};

	public enum QuerySlotData {
		SLOT_0(new byte[]{0x0f, 0x04, 0x5F, 0x00, 0x00, 0x5F, (byte) 0xff, (byte) 0xff}), 
		SLOT_1(new byte[]{0x0f, 0x04, 0x5F, 0x00, 0x01, 0x60, (byte) 0xff, (byte) 0xff}), 
		SLOT_2(new byte[]{0x0f, 0x04, 0x5F, 0x00, 0x02, 0x61, (byte) 0xff, (byte) 0xff}), 
		SLOT_3(new byte[]{0x0f, 0x04, 0x5F, 0x00, 0x03, 0x62, (byte) 0xff, (byte) 0xff});
		private byte[]	value;

		private QuerySlotData(byte[] v) {
			this.value = v;
		}

		public byte[] value() {
			return this.value;
		}

		public void setValue(final byte[] newValue) {
			this.value = newValue;
		}
	};
	
	byte[] GET_SYSTEM_SETTING = new byte[]{0x0f, 0x04, 0x5a, 0x00, 0x00, 0x5a, (byte) 0xff, (byte) 0xff};
	byte[] START_PROCESSING 	= new byte[]{0x0F, 0x03, 0x05, 0x00, 0x05, (byte) 0xff, (byte) 0xff, (byte) 0xff};
	byte[] STOP_PROCESSING 		= new byte[]{0x0F, 0x03, (byte) 0xFE, 0x00, (byte) 0xFE, (byte) 0xff, (byte) 0xff, (byte) 0xff};
	
	final int  dataSize;
	final int  terminalDataSize = 345; // configuration menu string
	int retrys = 1; // re-try temporary error counter
	
	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public MC3000UsbPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
		this.dataSize = this.deviceConfig.getDataBlockSize(InputTypes.SERIAL_IO);
		this.interfaceId = this.device.getUsbInterface();
		this.endpointIn = this.device.getUsbEndpointIn();
		this.endpointOut = this.device.getUsbEndpointOut();
		
		//make sure commands have enough bytes to fill endpoint buffer
		byte[] tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(TakeMtuData.SLOT_0.value(), 0, tmpData, 0, TakeMtuData.SLOT_0.value().length);
		TakeMtuData.SLOT_0.setValue(tmpData);
		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(TakeMtuData.SLOT_1.value(), 0, tmpData, 0, TakeMtuData.SLOT_1.value().length);
		TakeMtuData.SLOT_1.setValue(tmpData);
		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(TakeMtuData.SLOT_2.value(), 0, tmpData, 0, TakeMtuData.SLOT_2.value().length);
		TakeMtuData.SLOT_2.setValue(tmpData);
		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(TakeMtuData.SLOT_3.value(), 0, tmpData, 0, TakeMtuData.SLOT_3.value().length);
		TakeMtuData.SLOT_3.setValue(tmpData);
		
		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(QuerySlotData.SLOT_0.value(), 0, tmpData, 0, QuerySlotData.SLOT_0.value().length);
		QuerySlotData.SLOT_0.setValue(tmpData);
		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(QuerySlotData.SLOT_1.value(), 0, tmpData, 0, QuerySlotData.SLOT_1.value().length);
		QuerySlotData.SLOT_1.setValue(tmpData);
		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(QuerySlotData.SLOT_2.value(), 0, tmpData, 0, QuerySlotData.SLOT_2.value().length);
		QuerySlotData.SLOT_2.setValue(tmpData);
		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(QuerySlotData.SLOT_3.value(), 0, tmpData, 0, QuerySlotData.SLOT_3.value().length);
		QuerySlotData.SLOT_3.setValue(tmpData);

		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(GET_SYSTEM_SETTING, 0, tmpData, 0, GET_SYSTEM_SETTING.length);
		GET_SYSTEM_SETTING = tmpData;
		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(START_PROCESSING, 0, tmpData, 0, START_PROCESSING.length);
		START_PROCESSING = tmpData;
		tmpData = new byte[Math.abs(this.dataSize)];
		System.arraycopy(STOP_PROCESSING, 0, tmpData, 0, STOP_PROCESSING.length);
		STOP_PROCESSING = tmpData;
	}
	
	/**
	 * start the processing just before query slot data
	 * @param usbInterface
	 * @throws Exception
	 */
	public synchronized void startProcessing(UsbInterface usbInterface) throws Exception {
		final String $METHOD_NAME = "startProcessing"; //$NON-NLS-1$
		UsbInterface iface = null;
		boolean isPortOpenedByCall = false;

		try {
			if (usbInterface == null) {
				iface = this.openUsbPort(this.device);
				isPortOpenedByCall = true;
			}
			iface = usbInterface == null ? this.openUsbPort(this.device) : usbInterface;
			this.write(iface, this.endpointIn, this.START_PROCESSING);					
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				log.logp(Level.SEVERE, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
			throw e;
		}
		finally {
				if (isPortOpenedByCall) this.closeUsbPort(iface);
		}
	}
	
	/**
	 * stop the data collection not really processing
	 * @param usbInterface
	 * @throws Exception
	 */
	public synchronized void stopProcessing(UsbInterface usbInterface) {
		final String $METHOD_NAME = "stopProcessing"; //$NON-NLS-1$
		UsbInterface iface = null;
		boolean isPortOpenedByCall = false;

		try {
			if (usbInterface == null) {
				iface = this.openUsbPort(this.device);
				isPortOpenedByCall = true;
			}
			iface = usbInterface == null ? this.openUsbPort(this.device) : usbInterface;
			this.write(iface, this.endpointIn, this.STOP_PROCESSING);					
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				log.logp(Level.SEVERE, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
		}
		finally {
				if (isPortOpenedByCall) try {
					this.closeUsbPort(iface);
				}
				catch (Throwable e) {
					log.logp(Level.SEVERE, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
				}
		}
	}
	
	/**
	 * query the actual system settings
	 * @param usbInterface
	 * @return
	 * @throws Exception
	 */
	public synchronized byte[] getSystemSettings(UsbInterface usbInterface) throws Exception {
		final String $METHOD_NAME = "getSystemSettings"; //$NON-NLS-1$
		byte[] data = new byte[Math.abs(this.dataSize)];
		UsbInterface iface = null;
		boolean isPortOpenedByCall = false;

		try {
			if (usbInterface == null) {
				iface = this.openUsbPort(this.device);
				isPortOpenedByCall = true;
			}
			iface = usbInterface == null ? this.openUsbPort(this.device) : usbInterface;
			this.write(iface, this.endpointIn, this.GET_SYSTEM_SETTING);					
			try {
				Thread.sleep(10);
			}
			catch (Exception e) {
				// ignore
			}
			this.read(iface, this.endpointOut, data);
			
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, String.format("Checksum = 0x%02X -> %b", MC3000UsbPort.calculateCheckSum(data, data.length-2), this.isChecksumOK(data, 16, 30, 31)));
			
			if (!this.isChecksumOK(data, 16, 30, 31) && this.retrys-- >= 0) {
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, String.format("Error: Checksum = 0x%02X -> %b", MC3000UsbPort.calculateCheckSum(data, data.length-2), this.isChecksumOK(data, 16, 30, 31)));
				return this.getSystemSettings(iface);
			}
		}
		catch (Exception e) {
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
		}
		finally {
				if (isPortOpenedByCall) this.closeUsbPort(iface);
		}
		this.retrys = 1;
		return data;
	
	}
	
	/**
	 * query the actual system settings
	 * @param usbInterface
	 * @return
	 * @throws Exception
	 */
	public synchronized byte[] setSlotProgram(final UsbInterface usbInterface, final byte[] buffer) throws Exception {
		final String $METHOD_NAME = "setSlotProgram"; //$NON-NLS-1$
		byte[] data = new byte[Math.abs(this.dataSize)];
		UsbInterface iface = null;
		boolean isPortOpenedByCall = false;

		try {
			if (usbInterface == null) {
				iface = this.openUsbPort(this.device);
				isPortOpenedByCall = true;
			}
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(buffer, buffer.length));
			iface = usbInterface == null ? this.openUsbPort(this.device) : usbInterface;
			this.write(iface, this.endpointIn, buffer);					
			try {
				Thread.sleep(10);
			}
			catch (Exception e) {
				// ignore
			}
			this.read(iface, this.endpointOut, data);			
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
			
			if ((data[0]&0xFF) != 0xF0) {
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
				throw new UsbException("Error: answer != 0xF0");
			}
		}
		catch (Exception e) {
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
		}
		finally {
				if (isPortOpenedByCall) this.closeUsbPort(iface);
		}
		this.retrys = 1;
		return data;
	
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData(final UsbInterface iface, final byte[] request) throws Exception {
		final String $METHOD_NAME = "getData"; //$NON-NLS-1$
		byte[] data = new byte[Math.abs(this.dataSize)];

		try {
			this.write(iface, this.endpointIn, request);			
			try {
				Thread.sleep(10);
			}
			catch (Exception e) {
				// ignore
			}
			this.read(iface, this.endpointOut, data);
			
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
			if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, String.format("Checksum = 0x%02X -> %b", MC3000UsbPort.calculateCheckSum(data), this.isChecksumOK(data)));
			
			if (!this.isChecksumOK(data) && this.retrys-- >= 0) {
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, String.format("Error: Checksum = 0x%02X -> %b", MC3000UsbPort.calculateCheckSum(data), this.isChecksumOK(data)));
				return this.getData(iface, request);
			}
		}
		catch (Exception e) {
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
		}
		this.retrys = 1;
		return data;
	}
	

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getSlotData(final UsbInterface iface, final byte[] request) throws Exception {
		final String $METHOD_NAME = "getSlotData"; //$NON-NLS-1$
		byte[] data = new byte[Math.abs(this.dataSize)];

		try {
			this.write(iface, this.endpointIn, request);			
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(request, request.length));
			try {
				Thread.sleep(10);
			}
			catch (Exception e) {
				// ignore
			}
			this.read(iface, this.endpointOut, data);
			
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, String.format("Checksum = 0x%02X -> %b", MC3000UsbPort.calculateCheckSum(data), this.isChecksumOK(data)));
			
			if (!this.isChecksumOK(data) && this.retrys-- >= 0) {
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, String.format("Error: Checksum = 0x%02X -> %b", MC3000UsbPort.calculateCheckSum(data), this.isChecksumOK(data)));
				return this.getData(iface, request);
			}
		}
		catch (Exception e) {
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
		}
		this.retrys = 1;
		return data;
	}

	public static byte calculateCheckSum(byte[] buffer) {
		return  (byte) (Checksum.ADD(buffer, 0, buffer.length-2)%256);
	}

	public static byte calculateCheckSum(byte[] buffer, final int length) {
		return (byte) (Checksum.ADD(buffer, 2, length)%256);
	}

	/**
	 * check check sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	private boolean isChecksumOK(final byte[] buffer, final int start, final int end, final int chkSumPosition) {
		final String $METHOD_NAME = "isChecksumOK"; //$NON-NLS-1$
		log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME,"CheckSum = " + (Checksum.ADD(buffer, start, end))); //$NON-NLS-1$
		return Checksum.ADD(buffer, start, end) == (0x100 - buffer[chkSumPosition]);
	}

	/**
	 * check check sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	private boolean isChecksumOK(final byte[] buffer) {
		final String $METHOD_NAME = "isChecksumOK"; //$NON-NLS-1$
		log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME,"CheckSum = " + (Checksum.ADD(buffer, 2, buffer.length-2))); //$NON-NLS-1$
		return calculateCheckSum(buffer) == buffer[buffer.length-1];
	}
}
