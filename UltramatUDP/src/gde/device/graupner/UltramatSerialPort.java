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

import gde.comm.DeviceCommPort;
import gde.device.IDevice;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.Checksum;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * eStation serial port implementation
 * @author Winfried Brügmann
 */
public class UltramatSerialPort extends DeviceCommPort {
	final static String $CLASS_NAME = UltramatSerialPort.class.getName();
	final static Logger	log	= Logger.getLogger($CLASS_NAME);
	
	boolean isInSync = false;
	
	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public UltramatSerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData() throws Exception {
		final String $METHOD_NAME = "getData";
		byte[] data = new byte[Math.abs(this.device.getDataBlockSize())];
		byte[] answer = new byte[] {0x00};

		try {
			
			answer = new byte[data.length];
			answer = this.read(answer, 3000);
			// synchronize received data to begin of sent data 
			while (answer[0] != 0x0C) {
				this.isInSync = false;
				for (int i = 1; i < answer.length; i++) {
					if(answer[i] == 0x0C){
						System.arraycopy(answer, i, data, 0, data.length-i);
						answer = new byte[i];
						answer = this.read(answer, 1000);
						System.arraycopy(answer, 0, data, data.length-i, i);
						this.isInSync = true;
						log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
						break; //sync
					}
				}
				if(this.isInSync)
					break;
				answer = new byte[data.length];
				answer = this.read(answer, 3000);
			}
			if (log.isLoggable(Level.FINER)) {
				StringBuilder sb = new StringBuilder().append("<FF>");
				for (int i = 1; i < answer.length - 1; ++i) {
					sb.append(String.format("%c", (char) answer[i])); //$NON-NLS-1$
				}
				sb.append("<CR>");
				log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, sb.toString());
			}

			if (answer[0] == 0x0C && answer[data.length-1] == 0x0D) {
				System.arraycopy(answer, 0, data, 0, data.length);
			}
			else {
				this.addXferError();
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "=====> data start or end does not match, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				data = getData();
			}

			if (!isChecksumOK(data)) {
				this.addXferError();
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "=====> checksum error occured, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				data = getData();
			}
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				log.logp(Level.SEVERE, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
			throw e;
		}
		return data;
	}

	/**
	 * check ckeck sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	private boolean isChecksumOK(byte[] buffer) {
		final String $METHOD_NAME = "isChecksumOK";
		boolean isOK = false;
		int length = Math.abs(this.device.getDataBlockSize());
		int check_sum = Checksum.ADD(buffer, 1, length-6);
		if (check_sum == Integer.parseInt(String.format("%c%c%c%c", (char) buffer[length-5], (char) buffer[length-4], (char) buffer[length-3], (char) buffer[length-2]), 16))
			isOK = true;
		log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "Check_sum = " + isOK); //$NON-NLS-1$
		return isOK;
	}

}
