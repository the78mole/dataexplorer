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
    
    Copyright (c) 2008,2009,2010,2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.device.junsi;

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
public class iChargerSerialPort extends DeviceCommPort {
	final static String $CLASS_NAME = iChargerSerialPort.class.getName();
	final static Logger	log	= Logger.getLogger($CLASS_NAME);
	
	boolean isInSync = false;
	
	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public iChargerSerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData() throws Exception {
		final String $METHOD_NAME = "getData";
		byte[] data = new byte[] {0x00};
		byte[] tmpData = new byte[150];
		byte[] answer = new byte[] {0x00};
		byte[] tmpByte = new byte[0];
		byte lastByte = 0x00;

		try {
			
			answer = new byte[tmpData.length];
			answer = this.read(answer, 3000, 100);

			// synchronize received data to begin of sent data 
			while (answer[0] != '$') {
				this.isInSync = false;
				for (int i = 1; i < answer.length; i++) {
					if (answer[i] == '$') {
						System.arraycopy(answer, i, tmpData, 0, answer.length - i);

						//check if ending is contained in answer already
						int arrayLength = getArrayLengthByCheckEnding(answer);
						if (arrayLength < i + 15) {
							//answer is incomplete 
							while ((this.read(tmpByte, 500)).length > 0) {
								tmpData[++i] = tmpByte[0];
								if (tmpByte[0] == 0x0A && lastByte == 0x0D) break;
								lastByte = tmpByte[0];
							}
						}

						this.isInSync = true;
						log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
						break; //sync
					}
				}
				if (this.isInSync) break;

				//re-read data
				tmpData = getData();
			}
			if (answer[0] == '$') System.arraycopy(answer, 0, tmpData, 0, answer.length);
			data = new byte[getArrayLengthByCheckEnding(tmpData)];
			System.arraycopy(tmpData, 0, data, 0, data.length);

			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				for (byte b : data) {
					sb.append((char)b);
				}
				log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, sb.toString());
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
	 * @param tmpData
	 * @return
	 */
	public int getArrayLengthByCheckEnding(byte[] tmpData) {
		//real answer might be shorter as the maximum of 150 bytes
		int lenght = tmpData.length;
		while (lenght > 10 && !(tmpData[lenght-1] == 0x0A && tmpData[lenght-2] == 0x0D))
			--lenght;
		return lenght;
	}

	/**
	 * check check sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	private boolean isChecksumOK(byte[] buffer) {
		final String $METHOD_NAME = "isChecksumOK";
		boolean isOK = false;
		int check_sum = Checksum.XOR(buffer, buffer.length-4);
		if (Integer.parseInt(String.format("%c%c", buffer[buffer.length-4], buffer[buffer.length-3])) == check_sum)
			isOK = true;
		log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "Check_sum = " + isOK); //$NON-NLS-1$
		return isOK;
	}

}
