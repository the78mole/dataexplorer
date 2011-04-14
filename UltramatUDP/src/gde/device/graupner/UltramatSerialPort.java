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

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.comm.DeviceSerialPortImpl;
import gde.device.DeviceConfiguration;
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
	
	final static byte		BEGIN				= 0x0C;
	final static byte		END					= 0x0D;
	final static byte		ACK					= 0x06;
	final static byte		NAK					= 0x15;
	final static byte[]	RESET_BEGIN										= new byte[] { BEGIN, 0x41, 0x37, 0x30, 0x30, 0x30, 0x30, 0x44, 0x38, END };	//1000 1111
	final static byte[]	RESET_END											= new byte[] { BEGIN, 0x43, 0x30, 0x30, 0x30, 0x30, 0x30, 0x44, 0x33, END };	//1000 1111
	static byte[]				READ_MEMORY_NAME							= new byte[] { BEGIN, '8', '0', '0', '0', '0', '0', '0', '0', END };					//1000 0000
	static byte[]				WRITE_MEMORY_NAME							= new byte[] { BEGIN, '0', '0' };																							//0000 0000
	static byte[]				READ_MEMORY_SETUP							= new byte[] { BEGIN, '8', '1', '0', '0', '0', '0', '0', '0', END };					//1000 0001
	static byte[]				WRITE_MEMORY_SETUP						= new byte[] { BEGIN, '0', '1' };																							//0000 0001
	static byte[]				READ_MEMORY_STEP_CHARGE_SETUP	= new byte[] { BEGIN, '8', '2', '0', '0', '0', '0', '0', '0', END };					//1000 0010
	static byte[]				WRITE_STEP_CHARGE_SETUP				= new byte[] { BEGIN, '0', '2' };																							//0000 0010
	static byte[]				READ_MEMORY_CYCLE_DATA				= new byte[] { BEGIN, '8', '3', '0', '0', '0', '0', '0', '0', END };					//1000 0011
	static byte[]				WRITE_CYCLE_DATA							= new byte[] { BEGIN, '0', '3' };																							//0000 0011
	static byte[]				READ_MEMORY_TRACE_DATA				= new byte[] { BEGIN, '8', '4', '0', '0', '0', '0', '0', '0', END };					//1000 0100
	static byte[]				WRITE_TRACE_DATA							= new byte[] { BEGIN, '0', '4' };																							//0000 0100
	static byte[]				READ_TIRE_HEATER							= new byte[] { BEGIN, '8', '5', '0', '0', '0', '0', '0', '0', END };					//1000 0101
	static byte[]				WRITE_TIRE_HEATER							= new byte[] { BEGIN, '0', '5' };																							//0000 0101
	static byte[]				READ_MOTOR_RUN								= new byte[] { BEGIN, '8', '6', '0', '0', '0', '0', '0', '0', END };					//1000 0110
	static byte[]				WRITE_MOTOR_RUN								= new byte[] { BEGIN, '0', '6' };																							//0000 0110
	static byte[]				READ_CHANNEL_SETUP						= new byte[] { BEGIN, '8', '7', '0', '0', '0', '0', '0', '0', END };					//1000 0111
	static byte[]				WRITE_CHANNEL_SETUP						= new byte[] { BEGIN, '0', '7' };																							//0000 0111
	static byte[]				READ_DEVICE_IDENTIFIER_NAME		= new byte[] { BEGIN, '8', '8', '0', '0', '0', '0', '0', '0', END };					//1000 1000
	static byte[]				WRITE_DEVICE_IDENTIFIER_NAME	= new byte[] { BEGIN, '0', '8' };																							//0000 1000
	static byte[]				READ_GRAPHICS_DATA						= new byte[] { BEGIN, '8', '9', '0', '0', '0', '0', '0', '0', END };					//1000 1001
	static byte[]				WRITE_GRAPHICS_DATA						= new byte[] { BEGIN, '0', '9' };																							//0000 1001
	
	static int					SIZE_MEMORY_SETUP							= 28;
	static int					SIZE_MEMORY_STEP_CHARGE_SETUP	= 20;
	static int					SIZE_MEMORY_TRACE							= 6;
	static int					SIZE_MEMORY_CYCLE							= 121;
	static int					SIZE_CHANNEL_1_SETUP					= 16;
	static int					SIZE_CHANNEL_2_SETUP					= 4;
	static int					SIZE_TIRE_HEATER_SETUP				= 8;
	static int					SIZE_MOTOR_RUN_SETUP					= 17;

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
	 * constructor for testing purpose
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public UltramatSerialPort(DeviceConfiguration deviceConfiguration) {
		super(deviceConfiguration);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData() throws Exception {
		final String $METHOD_NAME = "getData"; //$NON-NLS-1$
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
				if(this.isInSync)	break;
				
				answer = new byte[data.length];
				answer = this.read(answer, 3000);
			}
			if (log.isLoggable(Level.FINER)) {
				StringBuilder sb = new StringBuilder().append(DeviceSerialPortImpl.STRING_FF); //$NON-NLS-1$
				for (int i = 1; i < answer.length - 1; ++i) {
					sb.append(String.format("%c", (char) answer[i])); //$NON-NLS-1$
				}
				sb.append(DeviceSerialPortImpl.STRING_CR); //$NON-NLS-1$
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
	public synchronized String readDeviceUserName() throws IOException, TimeOutException  {
			byte[] answer = this.readConfigData(READ_DEVICE_IDENTIFIER_NAME, 23, 2);
			return String.format(DeviceSerialPortImpl.FORMAT_16_CHAR, answer[1], answer[2], answer[3], answer[4], answer[5], answer[6], answer[7], answer[8], answer[9], answer[10], answer[11], answer[12], answer[13], answer[14], answer[15], answer[16]);
	}
	
	/**
	 * read the outlet channel setup data
	 * @param channelNumber
	 * @return values integer array
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized String readChannelData(int channelNumber) throws IOException, TimeOutException {
		if (channelNumber == 1) {
			return new String(this.readConfigData(READ_CHANNEL_SETUP, SIZE_CHANNEL_1_SETUP * 4 + 7, channelNumber)).substring(1, SIZE_CHANNEL_1_SETUP * 4 + 1);
		}
		else if (channelNumber == 2) {
			return new String(this.readConfigData(READ_CHANNEL_SETUP, SIZE_CHANNEL_2_SETUP * 4 + 7, channelNumber)).substring(1, SIZE_CHANNEL_2_SETUP * 4 + 1);
		}
		return GDE.STRING_EMPTY;
	}

	/**
	 * read the name of memory with given number 0 - 39 for memory 01 - 40
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized String readMemoryName(int number) throws IOException, TimeOutException {
		byte[] answer = this.readConfigData(READ_MEMORY_NAME, 23, number);
		return String.format(DeviceSerialPortImpl.FORMAT_16_CHAR, answer[1], answer[2], answer[3], answer[4], answer[5], answer[6], answer[7], answer[8], answer[9], answer[10], answer[11], answer[12], answer[13], answer[14], answer[15], answer[16]);
	}
	
	/**
	 * read the memory setup values for given memory number
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized String readMemorySetup(int number) throws IOException, TimeOutException {
		return new String(this.readConfigData(READ_MEMORY_SETUP, SIZE_MEMORY_SETUP * 4 + 7, number)).substring(1, SIZE_MEMORY_SETUP * 4+1);
	}
	
	/**
	 * read the memory trace data for given memory number
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized String readMemoryTrace(int number) throws IOException, TimeOutException {
		return new String(this.readConfigData(READ_MEMORY_TRACE_DATA, SIZE_MEMORY_TRACE * 4 + 7, number)).substring(1, SIZE_MEMORY_TRACE * 4+1);
	}
	
	/**
	 * read the memory cycle data for given memory number
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized String readMemoryCycle(int number) throws IOException, TimeOutException {
		return new String(this.readConfigData(READ_MEMORY_CYCLE_DATA, SIZE_MEMORY_CYCLE * 4 + 7, number)).substring(1, SIZE_MEMORY_CYCLE * 4+1);
	}
	
	/**
	 * read the memory step charge data for given memory number
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized String readMemoryStepChargeSetup(int number) throws IOException, TimeOutException {
		return new String(this.readConfigData(READ_MEMORY_STEP_CHARGE_SETUP, SIZE_MEMORY_STEP_CHARGE_SETUP * 4 + 7, number)).substring(1, SIZE_MEMORY_STEP_CHARGE_SETUP * 4+1);
	}

	/**
	 * read configuration data according given type and expected data size for a given index (channel, memory number, ...)
	 * @param type
	 * @param expectedDataSize
	 * @param index
	 * @return byte array containing the requested configuration data
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized byte[] readConfigData(byte[] type, int expectedDataSize, int index) throws IOException, TimeOutException {
		final String $METHOD_NAME = "readConfigData"; //$NON-NLS-1$
		log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, "entry"); //$NON-NLS-1$
		byte[] readBuffer = new byte[expectedDataSize];
		
		if (this.isConnected()) {
			byte[] writeBuffer = type;
			byte[] num = String.format("%02X", index).getBytes(); //$NON-NLS-1$
			System.arraycopy(num, 0, writeBuffer, 3, 2);
			byte[] checkSum = getChecksum(writeBuffer);
			System.arraycopy(checkSum, 0, writeBuffer, 5, 4);
			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < writeBuffer.length; ++i) {
					if (writeBuffer[i] == BEGIN)
						sb.append(DeviceSerialPortImpl.STRING_FF); //$NON-NLS-1$
					else if (writeBuffer[i] == END)
						sb.append(DeviceSerialPortImpl.STRING_CR); //$NON-NLS-1$
					else
						sb.append((char) writeBuffer[i]);
				}
				log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "write = " + sb.toString()); //$NON-NLS-1$
			}
			this.write(writeBuffer);
			byte[] answer = this.read(readBuffer, 3000);
			while (answer[0] != BEGIN) {
				this.isInSync = false;
				for (int i = 1; i < answer.length; i++) {
					if (answer[i] == BEGIN) {
						System.arraycopy(answer, i, readBuffer, 0, readBuffer.length - i);
						answer = new byte[i];
						answer = this.read(answer, 1000);
						System.arraycopy(answer, 0, readBuffer, readBuffer.length - i, i);
						this.isInSync = true;
						log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
						break; //sync
					}
				}
				if (this.isInSync) break;
				
				answer = new byte[expectedDataSize];
				answer = this.read(answer, 3000);
			}
			if (!(readBuffer[0] == BEGIN && readBuffer[readBuffer.length - 1] == ACK && isCommandChecksumOK(readBuffer))) {
				this.addXferError();
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "=====> data start or end does not match, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				readBuffer = readConfigData(type, expectedDataSize, index);
			}
			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < readBuffer.length; ++i) {
					if (readBuffer[i] == BEGIN)
						sb.append(DeviceSerialPortImpl.STRING_FF); //$NON-NLS-1$
					else if (readBuffer[i] == END)
						sb.append(DeviceSerialPortImpl.STRING_CR); //$NON-NLS-1$
					else if (readBuffer[i] == ACK)
						sb.append(DeviceSerialPortImpl.STRING_ACK); //$NON-NLS-1$
					else if (readBuffer[i] == NAK)
						sb.append(DeviceSerialPortImpl.STRING_NAK); //$NON-NLS-1$
					else if (i == readBuffer.length - 6)
						sb.append(GDE.STRING_OR).append((char) readBuffer[i]);
					else
						sb.append((char) readBuffer[i]);
				}
				log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "answer = " + sb.toString()); //$NON-NLS-1$
			}
		}
		return readBuffer;
	}

	/**
	 * write configuration data according given type and expected data size for a given index (channel, memory number, ...)
	 * @param type
	 * @param configData
	 * @param index
	 * @throws IOException if writing configuration data failed
	 * @throws TimeOutException
	 */
	public synchronized void writeConfigData(byte[] type, byte[] configData, int index) throws IOException, TimeOutException {
		final String $METHOD_NAME = "writeConfigData"; //$NON-NLS-1$
		log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, "entry"); //$NON-NLS-1$
		byte[] writeBuffer = new byte[configData.length + 10]; //0x0C, type, index, data..., checksum, 0x0D
		
		if (this.isConnected()) {
			System.arraycopy(type, 0, writeBuffer, 0, 3);
			writeBuffer[writeBuffer.length - 1] = END;
			byte[] num = String.format("%02X", index).getBytes(); //$NON-NLS-1$
			System.arraycopy(num, 0, writeBuffer, 3, 2);
			System.arraycopy(configData, 0, writeBuffer, 5, configData.length);
			byte[] checkSum = getChecksum(writeBuffer);
			System.arraycopy(checkSum, 0, writeBuffer, writeBuffer.length - 5, 4);
			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < writeBuffer.length; ++i) {
					if (writeBuffer[i] == BEGIN)
						sb.append(DeviceSerialPortImpl.STRING_FF); //$NON-NLS-1$
					else if (writeBuffer[i] == END)
						sb.append(DeviceSerialPortImpl.STRING_CR); //$NON-NLS-1$
					else if (writeBuffer[i] == ACK)
						sb.append(DeviceSerialPortImpl.STRING_ACK); //$NON-NLS-1$
					else if (writeBuffer[i] == NAK)
						sb.append(DeviceSerialPortImpl.STRING_NAK); //$NON-NLS-1$
					else if (i == writeBuffer.length - 5)
						sb.append(GDE.STRING_OR).append((char) writeBuffer[i]);
					else
						sb.append((char) writeBuffer[i]);
				}
				log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "answer = " + sb.toString()); //$NON-NLS-1$
			}
			this.write(writeBuffer);
			byte[] answer = this.read(new byte[1], 3000);
			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < answer.length; ++i) {
					if (answer[i] == BEGIN)
						sb.append(DeviceSerialPortImpl.STRING_FF); //$NON-NLS-1$
					else if (answer[i] == END)
						sb.append(DeviceSerialPortImpl.STRING_CR); //$NON-NLS-1$
					else if (answer[i] == ACK)
						sb.append(DeviceSerialPortImpl.STRING_ACK); //$NON-NLS-1$
					else if (answer[i] == NAK)
						sb.append(DeviceSerialPortImpl.STRING_NAK); //$NON-NLS-1$
					else if (i == answer.length - 6)
						sb.append(GDE.STRING_OR).append((char) answer[i]); //$NON-NLS-1$
					else
						sb.append((char) answer[i]);
				}
				log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "answer = " + sb.toString()); //$NON-NLS-1$
			}
			if ((answer[0] == NAK)) {
				log.log(Level.WARNING, "Writing UltraDuoPlus configuration type (" + new String(type) + ") data failed!"); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IOException("Writing UltraDuoPlus configuration type (" + new String(type) + ") data failed!"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	
	/**
	 * check check sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	private synchronized boolean isChecksumOK(byte[] buffer) {
		final String $METHOD_NAME = "isChecksumOK"; //$NON-NLS-1$
		boolean isOK = false;
		int length = Math.abs(this.device.getDataBlockSize());
		int check_sum = Checksum.ADD(buffer, 1, length-6);
		if (check_sum == Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) buffer[length-5], (char) buffer[length-4], (char) buffer[length-3], (char) buffer[length-2]), 16)) //$NON-NLS-1$
			isOK = true;
		log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "Check_sum = " + isOK); //$NON-NLS-1$
		return isOK;
	}
	/**
	 * check check sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	private synchronized boolean isCommandChecksumOK(byte[] buffer) {
		final String $METHOD_NAME = "isChecksumOK"; //$NON-NLS-1$
		boolean isOK = false;
		int length = buffer.length;
		int check_sum = Checksum.ADD(buffer, 1, length-7);
		if (check_sum == Integer.parseInt(String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) buffer[length-6], (char) buffer[length-5], (char) buffer[length-4], (char) buffer[length-3]), 16))
			isOK = true;
		log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "Check_sum = " + isOK); //$NON-NLS-1$
		return isOK;
	}

	/**
	 * calculate the checksum of the buffer and translate it to required format
	 * @param buffer
	 * @return
	 */
	private synchronized byte[] getChecksum(byte[] buffer) {
		final String $METHOD_NAME = "getChecksum"; //$NON-NLS-1$
		String check_sum = String.format("%04X", Checksum.ADD(buffer, 1, buffer.length-6)); //$NON-NLS-1$
		log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "Check_sum char[]= " + check_sum); //$NON-NLS-1$
		return check_sum.getBytes();
	}
}
