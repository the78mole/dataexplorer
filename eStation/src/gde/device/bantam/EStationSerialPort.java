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
package gde.device.bantam;

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
public class EStationSerialPort extends DeviceCommPort {
	final static String $CLASS_NAME = EStationSerialPort.class.getName();
	final static Logger	log	= Logger.getLogger($CLASS_NAME);
	
	boolean isInSync = false;
	
	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public EStationSerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData() throws Exception {
		final String $METHOD_NAME = "getData";
		byte[] data = new byte[76];
		byte[] answer = new byte[] {0x00};

		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected()) {
				this.open();
				isPortOpenedByMe = true;
			}
			
			answer = new byte[13];
			answer = this.read(answer, 3000);
			// synchronize received data to begin of sent data 
			while (answer[0] != 0x7b) {
				this.isInSync = false;
				for (int i = 1; i < answer.length; i++) {
					if(answer[i] == 0x7b){
						System.arraycopy(answer, i, data, 0, 13-i);
						answer = new byte[i];
						answer = this.read(answer, 1000);
						System.arraycopy(answer, 0, data, 13-i, i);
						this.isInSync = true;
						log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
						break; //sync
					}
				}
				if(this.isInSync)
					break;
				answer = new byte[13];
				answer = this.read(answer, 1000);
			}
			if (answer[0] == 0x7b) {
				System.arraycopy(answer, 0, data, 0, 13);
			}
			
			answer = new byte[12];
			answer = this.read(answer, 1000);
			System.arraycopy(answer, 0, data, 13, 12);
			answer = new byte[12];
			answer = this.read(answer, 1000);
			System.arraycopy(answer, 0, data, 25, 12);
			answer = new byte[12];
			answer = this.read(answer, 1000);
			System.arraycopy(answer, 0, data, 37, 12);
			answer = new byte[12];
			answer = this.read(answer, 1000);
			System.arraycopy(answer, 0, data, 49, 12);
			answer = new byte[15];
			answer = this.read(answer, 1000);
			System.arraycopy(answer, 0, data, 61, 15);

			StringBuilder sb;
			if (log.isLoggable(Level.FINER)) {
				sb = new StringBuilder();
				for (byte b : data) {
					sb.append(String.format("0x%02x ,", b)); //$NON-NLS-1$
				}
				log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, sb.toString());
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
		finally {
			if (isPortOpenedByMe) 
				this.close();
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
		int check_sum = Checksum.ADD(buffer, 1, 72);
		if (((check_sum & 0xF0) >> 4) + 0x30 == (buffer[73]&0xFF+0x80) && (check_sum & 0x00F) + 0x30 == (buffer[74]&0xFF))
			isOK = true;
		log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "Check_sum = " + isOK); //$NON-NLS-1$
		return isOK;
	}

}
