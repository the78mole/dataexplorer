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
    
    Copyright (c) 2008,2009,2010 Winfried Bruegmann
****************************************************************************************/
package gde.device.tttronix;

import gde.GDE;
import gde.device.DeviceConfiguration;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.serial.DeviceSerialPort;
import gde.ui.DataExplorer;
import gde.utils.Checksum;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * eStation serial port implementation
 * @author Winfried Brügmann
 */
public class QcCopterSerialPort extends DeviceSerialPort {
	final static String $CLASS_NAME = QcCopterSerialPort.class.getName();
	final static Logger	log	= Logger.getLogger($CLASS_NAME);
	
	boolean isInSync = false;
	
	final byte STX = 0x02;
	final byte ETX = 0x03;
	final int  dataSize;
	
	final DeviceConfiguration deviceConfig;
	
	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public QcCopterSerialPort(DeviceConfiguration currentDeviceConfig, DataExplorer currentApplication) {
		super(currentDeviceConfig, currentApplication);
		this.deviceConfig = currentDeviceConfig;
		this.dataSize = this.deviceConfig.getDataBlockSize();
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData() throws Exception {
		final String $METHOD_NAME = "getData";
		byte[] data = new byte[this.dataSize];

		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
			}
			data = this.read(data, 1500);
			
			//check data for content
			// synchronize received data to begin of sent data 
			while (containsSTX(data) && data[0] != STX) {  //&& data[this.dataSize - 1] != ETX) {
				this.isInSync = false;
				for (int i = 1; i < data.length; i++) {
					if(data[i] == STX){
						System.arraycopy(data, i, data, 0, this.dataSize-i);
						data = new byte[i];
						data = this.read(data, 1000);
						System.arraycopy(data, 0, data, this.dataSize-i, i);
						this.isInSync = true;
						log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
						break; //sync
					}
				}
				if(this.isInSync)
					break;
			}

			if (log.isLoggable(Level.FINER)) {
				StringBuilder sb = new StringBuilder();
				for (byte b : data) {
					sb.append(String.format("0x%02x ,", b)); //$NON-NLS-1$
				}
				log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, sb.toString());
			}
			
			if (!isChecksumOK(data)) {
				this.xferErrors++;
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "=====> checksum error occured, number of errors = " + this.xferErrors); //$NON-NLS-1$
				//TODO retry or skip in case of xfer errors
				data = getData();
			}
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				log.logp(Level.SEVERE, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
			throw e;
		}
		finally {
			if (isPortOpenedByMe) 
				this.close();
		}
		return data;
	}
	
	/**
	 * check if received data contains STX which
	 * @return true if one byte of data represent STX else false
	 */
	boolean containsSTX(byte[] data) {
		boolean isContained = false;
		for (int i = 0; i < data.length; i++) {
			if (data[i] == STX) {
				isContained = true;
				break;
			}
		}
		return isContained;
	}

	/**
	 * receive terminal data using stable counter which sends data back to gatherer thread while dialog is open
	 * @return String containing gathered data 
	 * @throws IOException
	 */
	public synchronized String getTerminalData() throws Exception {
		final String $METHOD_NAME = "getTerminalData";
		String returnString = GDE.STRING_EMPTY;

		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
			}
			
			byte[] data = new byte[200];
			waitForStableReceiveBuffer(20, 2000, 200);// stable counter max 3000/4 = 750
			data = this.read(data, 1000); 
			
			if (log.isLoggable(Level.FINER)) {
				StringBuilder sb = new StringBuilder();
				for (byte b : data) {
					sb.append(String.format("%c", (char)b)); //$NON-NLS-1$
				}
				log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, sb.toString());
			}
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				log.logp(Level.SEVERE, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
		}
		finally {
			if (isPortOpenedByMe) 
				this.close();
		}
		return returnString;
	}

	/**
	 * check check sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	private boolean isChecksumOK(byte[] buffer) {
		final String $METHOD_NAME = "isChecksumOK";
		log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, (Checksum.ADD(buffer, 1, 57)) + "; " + ( (((buffer[58]&0xFF) - 94) << 6) | (((buffer[59]&0xFF) - 94) & 0x3F) ) );
		return Checksum.ADD(buffer, 1, 57) == ((((buffer[58]&0xFF) - 94) << 6) | (((buffer[59]&0xFF) - 94) & 0x3F));
	}

}
