/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2010 Winfried Bruegmann
****************************************************************************************/
package gde.comm;

import gde.exception.ApplicationConfigurationException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gnu.io.SerialPort;

import java.io.IOException;
import java.util.Vector;

/**
 * interface to device serial port to enable overloading with different implementations
 */
public interface IDeviceCommPort {

	/**
	 * opens the serial port specified in device configuration or settings (global)
	 * @return reference to instance of serialPort
	 * @throws ApplicationConfigurationException
	 * @throws SerialPortException
	 */
	public SerialPort open() throws ApplicationConfigurationException, SerialPortException;
	
	/**
	 * function to close the serial port
	 * this is done within a tread since the port can't close if it stays open for a long time period ??
	 */
	public void close();
	
	/**
	 * read number of given bytes by the length of the referenced read buffer in a given time frame defined by time out value
	 * @param readBuffer
	 * @param timeout_msec
	 * @return the red byte array
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec) throws IOException, TimeOutException;

	/**
	 * read number of given bytes by the length of the referenced read buffer in a given time frame defined by time out value
	 * if the readBuffer kan not be filled a stable counter will be active where a number of retries can be specified
	 * @param readBuffer with the size expected bytes
	 * @param timeout_msec
	 * @param stableIndex a number of cycles to treat as telegram transmission finished
	 * @return the reference of the given byte array, byte array meight be adapted to received size
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex) throws IOException, TimeOutException;

	/**
	 * read number of given bytes by the length of the referenced read buffer in a given time frame defined by time out value
	 * the reference to the wait time vector will add the actual wait time to have the read buffer ready to read the given number of bytes
	 * @param readBuffer
	 * @param timeout_msec
	 * @param waitTimes
	 * @return the red byte array
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, Vector<Long> waitTimes) throws IOException, TimeOutException;


	/**
	 * write bytes to serial port output stream, cleans receive buffer if available byes prior to send data 
	 * @param writeBuffer writes size of writeBuffer to output stream
	 * @throws IOException
	 */
	public void write(byte[] writeBuffer) throws IOException;

	/**
	 * function check for available bytes on receive buffer
	 * @return System.currentTimeMillis() if data available within time out, else an exception
	 * @throws InterruptedException 
	 * @throws TimeOutException 
	 * @throws IOException 
	 */
	public long wait4Bytes(int timeout_msec) throws InterruptedException, TimeOutException, IOException;

	/**
	 * waits until receive buffer is filled with the number of expected bytes while checking inputStream
	 * @param numBytes
	 * @param timeout_msec
	 * @return number of bytes in receive buffer
	 * @throws TimeOutException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public int wait4Bytes(int numBytes, int timeout_msec) throws IOException;

	/**
	 * waits until receive buffer is filled with number of expected bytes or does not change anymore in stableIndex cycles * 10 msec
	 * @param expectedBytes
	 * @param timeout_msec in milli seconds, this is the maximum time this process will wait for stable byte count or maxBytes
	 * @param stableIndex cycle count times 10 msec to be treat as stable
	 * @return number of bytes in receive buffer
	 * @throws InterruptedException 
	 * @throws TimeOutException 
	 * @throws IOException 
	 */
	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_msec, int stableIndex) throws InterruptedException, TimeOutException, IOException;

	/**
	 * query if the port is already open
	 * @return
	 */
	public boolean isConnected();

	/**
	 * @return number of transfer errors occur (checksum)
	 */
	public int getXferErrors();

	/**
	 * add up transfer errors
	 */
	public void addXferError();

	/**
	 * check if a configured serial port string matches actual available ports
	 * @param newSerialPortStr
	 * @return true if given port string matches one of the available once
	 */
	public boolean isMatchAvailablePorts(String newSerialPortStr);

	/**
	 * check available bytes on inputstream
	 * @return number of bytes available on inputstream
	 * @throws IOException
	 */
	public int getAvailableBytes() throws IOException;
}
