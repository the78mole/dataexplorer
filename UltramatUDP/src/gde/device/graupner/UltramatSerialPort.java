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
import gde.exception.SerialPortException;
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
	
	final static byte		BEGIN				= 0x0C;
	final static byte		END					= 0x0D;
	final static byte		ACK					= 0x06;
	final static byte		NAK					= 0x15;
	final static byte[]	RESET_BEGIN				= new byte[] { BEGIN, 0x41, 0x37, 0x30, 0x30, 0x30, 0x30, 0x44, 0x38, END }; //1000 1111
	final static byte[]	RESET_END					= new byte[] { BEGIN, 0x43, 0x30, 0x30, 0x30, 0x30, 0x30, 0x44, 0x33, END }; //1000 1111
	static byte[]	READ_MEMORY_NAME				= new byte[] { BEGIN, '8', '0', '0', '0', '0', '0', '0', '0', END }; //1000 0000
	static byte[]	WRITE_MEMORY_NAME				= new byte[] { BEGIN, '0', '0', '0', '0', '0', '0', '0', '0', END }; //0000 0000
	static byte[]	READ_MEMORY_SETUP				= new byte[] { BEGIN, '8', '1', '0', '0', '0', '0', '0', '0', END }; //1000 0001
	static byte[]	WRITE_MEMORY_SETUP			= new byte[] { BEGIN, '0', '1', '0', '0', '0', '0', '0', '0', END }; //0000 0001
	static byte[]	READ_STEP_CHARGE_SETUP	= new byte[] { BEGIN, '8', '2', '0', '0', '0', '0', '0', '0', END }; //1000 0010
	static byte[]	WRITE_STEP_CHARGE_SETUP	= new byte[] { BEGIN, '0', '2', '0', '0', '0', '0', '0', '0', END }; //0000 0010
	static byte[]	READ_CYCLE_DATA					= new byte[] { BEGIN, '8', '3', '0', '0', '0', '0', '0', '0', END }; //1000 0011
	static byte[]	WRITE_CYCLE_DATA				= new byte[] { BEGIN, '0', '3', '0', '0', '0', '0', '0', '0', END }; //0000 0011
	static byte[]	READ_TRACE_DATA					= new byte[] { BEGIN, '8', '4', '0', '0', '0', '0', '0', '0', END }; //1000 0100
	static byte[]	WRITE_TRACE_DATA				= new byte[] { BEGIN, '0', '4', '0', '0', '0', '0', '0', '0', END }; //0000 0100
	static byte[]	READ_TIRE_HEATER				= new byte[] { BEGIN, '8', '5', '0', '0', '0', '0', '0', '0', END }; //1000 0101
	static byte[]	WRITE_TIRE_HEATER				= new byte[] { BEGIN, '0', '5', '0', '0', '0', '0', '0', '0', END }; //0000 0101
	static byte[]	READ_MOTOR_RUN					= new byte[] { BEGIN, '8', '6', '0', '0', '0', '0', '0', '0', END }; //1000 0110
	static byte[]	WRITE_MOTOR_RUN					= new byte[] { BEGIN, '0', '6', '0', '0', '0', '0', '0', '0', END }; //0000 0110
	static byte[]	READ_CHANNEL_SETUP			= new byte[] { BEGIN, '8', '7', '0', '0', '0', '0', '0', '0', END }; //1000 0111
	static byte[]	WRITE_USER_SETUP				= new byte[] { BEGIN, '0', '7', '0', '0', '0', '0', '0', '0', END }; //0000 0111
	static byte[]	READ_USER_NAME					= new byte[] { BEGIN, '8', '8', '0', '0', '0', '0', '0', '0', END }; //1000 1000
	static byte[]	WRITE_USER_NAME					= new byte[] { BEGIN, '0', '8', '0', '0', '0', '0', '0', '0', END }; //0000 1000
	static byte[]	READ_GRAPHICS_DATA			= new byte[] { BEGIN, '8', '9', '0', '0', '0', '0', '0', '0', END }; //1000 1001
	static byte[]	WRITE_GRAPHICS_DATA			= new byte[] { BEGIN, '0', '9', '0', '0', '0', '0', '0', '0', END }; //0000 1001

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
			while (answer[0] != BEGIN) {
				this.isInSync = false;
				for (int i = 1; i < answer.length; i++) {
					if(answer[i] == BEGIN){
						System.arraycopy(answer, i, data, 0, data.length-i);
						answer = new byte[i];
						answer = this.read(answer, 1000);
						System.arraycopy(answer, 0, data, data.length-i, i);
						this.isInSync = true;
						log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
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

			if (answer[0] == BEGIN && answer[data.length-1] == END) {
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
	 * read the device user name
	 * @return string configured in device
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public String readDeviceUserName() throws IOException, TimeOutException  {
			byte[] answer = this.readCommand(READ_USER_NAME, 23, 2);
			return String.format("%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c", answer[1], answer[2], answer[3], answer[4], answer[5], answer[6], answer[7], answer[8], answer[9], answer[10], answer[11], answer[12], answer[13], answer[14], answer[15], answer[16]);
	}
	
	/**
	 * read the outlet channel setup data
	 * @param channelNumber
	 * @return values integer array
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public int[] readChannelData(int channelNumber) throws IOException, TimeOutException {
		int[] values = new int[0];
		byte[] answer = new byte[0];
		if (channelNumber == 1) {
			answer = this.readCommand(READ_CHANNEL_SETUP, 71, 1);
			values = new int[16];
		}
		else {
			answer = this.readCommand(READ_CHANNEL_SETUP, 23, 2);
			values = new int[4];
		}
		for (int i = 0; i < values.length; i++) {
			values[i] = Integer.parseInt(String.format("%c%c%c%c", (char) answer[i*4+1], (char) answer[i*4+2], (char) answer[i*4+3], (char) answer[i*4+4]), 16);
		}
		return values;
	}

	/**
	 * read the name of memory with given number 0 - 39 for memory 01 - 40
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public String readMemoryName(int number) throws IOException, TimeOutException {
		byte[] answer = this.readCommand(READ_MEMORY_NAME, 23, number);
		return String.format("%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c%c", answer[1], answer[2], answer[3], answer[4], answer[5], answer[6], answer[7], answer[8], answer[9], answer[10], answer[11], answer[12], answer[13], answer[14], answer[15], answer[16]);
	}
	
	/**
	 * read the memory setup values for given memory number
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public int[] readMemorySetup(int number) throws IOException, TimeOutException {
		int[] values = new int[28];
		byte[] answer = this.readCommand(READ_MEMORY_SETUP, 119, number);
		
		for (int i = 0; i < values.length; i++) {
			values[i] = Integer.parseInt(String.format("%c%c%c%c", (char) answer[i*4+1], (char) answer[i*4+2], (char) answer[i*4+3], (char) answer[i*4+4]), 16);
		}
		return values;
	}
	
	public void readSetup() throws SerialPortException {
		final String $METHOD_NAME = "readSetup";
		boolean isConnectedByMe = false;
		try {
			if (!this.isConnected()) {
				this.open();
				isConnectedByMe = true;
			}
			this.write(RESET_BEGIN);

			//read user name 
			byte[] answer = this.readCommand(READ_USER_NAME, 23, 2);
			//read user setup 
			answer = this.readCommand(READ_CHANNEL_SETUP, 71, 1);
			answer = this.readCommand(READ_CHANNEL_SETUP, 23, 2);

			//read memory names
			for (int j = 0; j <= 60; j++) {
				System.out.print(j + " = ");
				answer = this.readCommand(READ_MEMORY_NAME, 23, j);
			}
			//			//read memory setup
			//			for (int j = 0; j < 40; j++) {
			//				answer = this.readCommand(READ_MEMORY_SETUP, 119, j);
			//			}
			//			//read step charge memory
			//			for (int j = 0; j < 40; j++) {
			//				answer = this.readCommand(READ_STEP_CHARGE_SETUP, 87, j);
			//			}
			//			//read cycle data memory
			//			for (int j = 0; j < 40; j++) {
			//				answer = this.readCommand(READ_CYCLE_DATA, 491, j);
			//			}
			//			//read trace data memory
			//			for (int j = 0; j < 40; j++) {
			//				answer = this.readCommand(READ_TRACE_DATA, 27, j);
			//			}
			//			//read tire data memory
			//			for (int j = 0; j < 40; j++) {
			//				answer = this.readCommand(READ_TIRE_HEATER, 39, j);
			//			}
			//			//read motor run channel
			//			for (int j = 0; j < 40; j++) {
			//				answer = this.readCommand(READ_MOTOR_RUN, 75, j);
			//			}

			this.write(RESET_END);
		}
		catch (Exception e) {
			throw new SerialPortException(e.getMessage());
		}
		finally {
			if (isConnectedByMe) {
				this.close();
			}
		}
	}

	public byte[] readCommand(byte[] command, int anserSize, int number) throws IOException, TimeOutException {
		final String $METHOD_NAME = "readCommand";
		byte[] writeBuffer = command;
		byte[] num = String.format("%02X", number).getBytes();
		System.arraycopy(num, 0, writeBuffer, 3, 2);
		byte[] checkSum = getChecksum(writeBuffer);
		System.arraycopy(checkSum, 0, writeBuffer, 5, 4);
		
		if(log.isLoggable(Level.OFF)) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i<writeBuffer.length; ++i) {
				if (writeBuffer[i] == BEGIN) sb.append("<FF>");
				else if (writeBuffer[i] == END) sb.append("<CR>");
				else sb.append((char)writeBuffer[i]);
			}
			log.logp(Level.OFF, $CLASS_NAME, $METHOD_NAME, "write = " + sb.toString()); //$NON-NLS-1$
		}
		this.write(writeBuffer);
		
		byte[] readBuffer = new byte[anserSize];
		byte[] answer = this.read(readBuffer, 3000);
		
		while (answer[0] != BEGIN) {
			this.isInSync = false;
			for (int i = 1; i < answer.length; i++) {
				if(answer[i] == BEGIN){
					System.arraycopy(answer, i, readBuffer, 0, readBuffer.length-i);
					answer = new byte[i];
					answer = this.read(answer, 1000);
					System.arraycopy(answer, 0, readBuffer, readBuffer.length-i, i);
					this.isInSync = true;
					log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
					break; //sync
				}
			}
			if(this.isInSync)
				break;
		}
		if (!(readBuffer[0] == BEGIN && readBuffer[readBuffer.length-1] == ACK && isCommandChecksumOK(readBuffer))) {
			this.addXferError();
			log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "=====> data start or end does not match, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
			readBuffer = readCommand(command, anserSize, number);
		}
		
		if (log.isLoggable(Level.OFF)) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < readBuffer.length; ++i) {
				if (readBuffer[i] == BEGIN)					sb.append("<FF>");
				else if (readBuffer[i] == END)			sb.append("<CR>");
				else if (readBuffer[i] == ACK)			sb.append("<ACK>");
				else if (readBuffer[i] == NAK)			sb.append("<NAK>");
				else if (i == readBuffer.length-6)	sb.append("|").append((char) readBuffer[i]);
				else																sb.append((char) readBuffer[i]);
			}
			log.logp(Level.OFF, $CLASS_NAME, $METHOD_NAME, "answer = " + sb.toString()); //$NON-NLS-1$
		}
		return readBuffer;
	}
	
	/**
	 * check check sum of data buffer
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
	/**
	 * check check sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	private boolean isCommandChecksumOK(byte[] buffer) {
		final String $METHOD_NAME = "isChecksumOK";
		boolean isOK = false;
		int length = buffer.length;
		int check_sum = Checksum.ADD(buffer, 1, length-7);
		if (check_sum == Integer.parseInt(String.format("%c%c%c%c", (char) buffer[length-6], (char) buffer[length-5], (char) buffer[length-4], (char) buffer[length-3]), 16))
			isOK = true;
		log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "Check_sum = " + isOK); //$NON-NLS-1$
		return isOK;
	}

	/**
	 * calculate the checksum of the buffer and translate it to required format
	 * @param buffer
	 * @return
	 */
	private byte[] getChecksum(byte[] buffer) {
		final String $METHOD_NAME = "getChecksum";
		String check_sum = String.format("%04X", Checksum.ADD(buffer, 1, buffer.length-6));
		log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "Check_sum char[]= " + check_sum); //$NON-NLS-1$
		return check_sum.getBytes();
	}
}
