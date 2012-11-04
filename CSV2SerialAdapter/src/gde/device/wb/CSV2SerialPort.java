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
package gde.device.wb;

import gde.comm.DeviceCommPort;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.Checksum;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * eStation serial port implementation
 * @author Winfried Brügmann
 */
public class CSV2SerialPort extends DeviceCommPort {
	final static String	$CLASS_NAME	= CSV2SerialPort.class.getName();
	final static Logger	log					= Logger.getLogger(CSV2SerialPort.$CLASS_NAME);

	final byte					startByte;
	final byte					endByte;
	final byte					endByte_1;
	final byte[]				tmpByte			= new byte[1];
	final int						timeout;
	final int						tmpDataLength;

	byte[]							tmpData;
	byte[]							data				= new byte[] { 0x00 };
	long								time				= 0;

	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public CSV2SerialPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
		this.startByte = (byte) this.device.getDataBlockLeader().charAt(0);
		this.endByte = this.device.getDataBlockEnding()[this.device.getDataBlockEnding().length - 1];
		this.endByte_1 = this.device.getDataBlockEnding().length == 2 ? this.device.getDataBlockEnding()[0] : 0x00;
		this.tmpDataLength = Math.abs(this.device.getDataBlockSize(InputTypes.SERIAL_IO)) * 10;
		this.timeout = this.device.getDeviceConfiguration().getRTOCharDelayTime() + this.device.getDeviceConfiguration().getRTOExtraDelayTime();
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData() throws Exception {
		final String $METHOD_NAME = "getData";

		try {
			this.tmpData = new byte[this.tmpDataLength];
			this.time = System.nanoTime() / 1000000;
			
			while ((this.read(this.tmpByte, this.timeout)).length > 0 && (System.nanoTime() / 1000000 - this.time) <= this.timeout) {
				if (this.tmpByte[0] == this.startByte) {
					this.tmpData[0] = this.tmpByte[0];
					int index = 1;
					while ((this.read(this.tmpByte, this.timeout)).length > 0) {
						this.tmpData[index] = this.tmpByte[0];
						if ((this.endByte_1 != 0x00 || this.tmpData[index - 1] == this.endByte_1) && this.tmpData[index] == this.endByte) break;
						++index;
					}

					this.data = new byte[getArrayLengthByCheckEnding()];
					if (this.data.length == 0)
						this.data = getData();
					else
						System.arraycopy(this.tmpData, 0, this.data, 0, this.data.length);

					if (CSV2SerialPort.log.isLoggable(Level.OFF)) {
						StringBuilder sb = new StringBuilder();
						for (byte b : this.data) {
							sb.append((char) b);
						}
						while (sb.length() > 5 && (sb.charAt(sb.length() - 1) == '\n' || sb.charAt(sb.length() - 1) == '\r'))
							sb.deleteCharAt(sb.length() - 1);
						CSV2SerialPort.log.logp(Level.OFF, CSV2SerialPort.$CLASS_NAME, $METHOD_NAME, sb.toString());
					}
					return this.data;
				}
			}
			throw new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { this.data.length, this.timeout }));
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				CSV2SerialPort.log.logp(Level.SEVERE, CSV2SerialPort.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
			throw e;
		}
	}

	/**
	 * @param this.tmpData
	 * @return
	 */
	public int getArrayLengthByCheckEnding() {
		//real answer might be shorter as the maximum of 150 bytes
		int lenght = this.tmpData.length;
		while (lenght > 0 && !((this.endByte_1 != 0x00 || this.tmpData[lenght - 2] == this.endByte_1) && this.tmpData[lenght - 1] == this.endByte))
			--lenght;
		if (CSV2SerialPort.log.isLoggable(Level.FINER))
			CSV2SerialPort.log.logp(Level.FINER, CSV2SerialPort.$CLASS_NAME, "getArrayLengthByCheckEnding", "array length = " + lenght);
		return lenght;
	}

	/**
	 * check check sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	protected boolean isChecksumOK(byte[] buffer) {
		final String $METHOD_NAME = "isChecksumOK";
		boolean isOK = false;
		int check_sum = Checksum.XOR(buffer, buffer.length - 4);
		if (Integer.parseInt(String.format("%c%c", buffer[buffer.length - 4], buffer[buffer.length - 3])) == check_sum) isOK = true;
		if (CSV2SerialPort.log.isLoggable(Level.FINER))
			CSV2SerialPort.log.logp(Level.FINER, CSV2SerialPort.$CLASS_NAME, $METHOD_NAME, "Check_sum = " + isOK); //$NON-NLS-1$
		return isOK;
	}

}
