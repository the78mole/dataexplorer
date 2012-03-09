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
    
    Copyright (c) 2010,2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.comm;

import gde.GDE;
import gde.config.Settings;
import gde.device.IDevice;
import gde.exception.ApplicationConfigurationException;
import gde.exception.FailedQueryException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.io.LogViewReader;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;
import gnu.io.SerialPort;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

/**
 * class to simulate serial port where bytes read form input file
 */
public class DeviceSerialPortSimulatorImpl implements IDeviceCommPort {
	final static String	$CLASS_NAME					= DeviceSerialPortSimulatorImpl.class.getName();
	final static Logger	log									= Logger.getLogger($CLASS_NAME);

	DataInputStream			data_in;
	BufferedReader			txt_in;
	int									xferErrors					= 0;
	int									timeoutErrors				= 0;
	boolean							isConnected					= false;
	String 							fileType 						= GDE.FILE_ENDING_STAR_LOV;

	final IDevice				device;
	final DataExplorer	application;
	final Settings			settings;
	final int						sleepTime_ms;
	final boolean				isTimeStepConstant;

	/**
	 * constructor to create a communications port simulation instance
	 * @param currentDevice
	 * @param currentApplication
	 * @param timeStep_ms
	 */
	public DeviceSerialPortSimulatorImpl(IDevice currentDevice, DataExplorer currentApplication, boolean isTimeStepConstant, int timeStep_ms) {
		this.device = currentDevice;
		this.application = currentApplication;
		this.settings = Settings.getInstance();
		this.sleepTime_ms = timeStep_ms;
		this.isTimeStepConstant = isTimeStepConstant;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#open()
	 */
	public SerialPort open() throws ApplicationConfigurationException, SerialPortException {
		try {
			if (this.application != null) {
				String path;
				if (this.application.isObjectoriented()) {
					path = this.application.getObjectFilePath();
				}
				else {
					String devicePath = this.application.getActiveDevice() != null ? GDE.FILE_SEPARATOR_UNIX + this.application.getActiveDevice().getName() : GDE.STRING_EMPTY;
					path = this.application.getActiveDevice() != null ? this.settings.getDataFilePath() + devicePath + GDE.FILE_SEPARATOR_UNIX : this.settings.getDataFilePath();
					if (!FileUtils.checkDirectoryAndCreate(path)) {
						this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0012, new Object[] { path }));
					}
				}
				FileDialog openFileDialog = this.application.openFileOpenDialog("Open File used as simulation input", new String[] { GDE.FILE_ENDING_STAR_LOV, GDE.FILE_ENDING_STAR_TXT,
						GDE.FILE_ENDING_STAR_LOG }, path, null, SWT.SINGLE);
				if (openFileDialog.getFileName().length() > 4) {
					String openFilePath = (openFileDialog.getFilterPath() + GDE.FILE_SEPARATOR_UNIX + openFileDialog.getFileName()).replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);

					if (openFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_OSD)) {
						fileType = GDE.FILE_ENDING_STAR_OSD;
						//TODO add implementation to use *.osd files as simulation data input
					}
					else if (openFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_LOV)) {
						fileType = GDE.FILE_ENDING_STAR_LOV;
						data_in = new DataInputStream(new FileInputStream(new File(openFilePath)));
						LogViewReader.readHeader(data_in);
					}
					else if (openFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_TXT)) {
						fileType = GDE.FILE_ENDING_STAR_TXT;
						txt_in = new BufferedReader(new InputStreamReader(new FileInputStream(openFilePath), "ISO-8859-1")); //$NON-NLS-1$
						txt_in.read();
					}
					else if (openFilePath.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
						fileType = GDE.FILE_ENDING_STAR_LOG;
						txt_in = new BufferedReader(new InputStreamReader(new FileInputStream(openFilePath), "ISO-8859-1")); //$NON-NLS-1$
					}
					else
						this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0008) + openFilePath);
				}
				this.isConnected = data_in != null || txt_in != null;
				this.application.setPortConnected(this.isConnected);
			}
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#close()
	 */
	public void close() {
		try {
			if (data_in != null) {
				data_in.close(); 
				data_in = null;
			}
			if (txt_in != null) {
				txt_in.close(); 
				txt_in = null;
			}
			if (this.isConnected) {
				this.isConnected = false;
				if (this.application != null) this.application.setPortConnected(false);
			}
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#read(byte[], int)
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec) throws IOException, TimeOutException {
		final String $METHOD_NAME = "read"; //$NON-NLS-1$
		try {
			wait4Bytes(timeout_msec);
		}
		catch (InterruptedException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		if (this.isConnected) {
			if (data_in != null && this.fileType.equals(GDE.FILE_ENDING_STAR_LOV)) {
				if (data_in.read(readBuffer) > 0) {
					int size2Read = this.device.getLovDataByteSize() - Math.abs(this.device.getDataBlockSize());
					if (data_in.read(new byte[size2Read]) != size2Read) {
						log.log(Level.WARNING, "expected byte size to  read does not macht really red size of bytes !");
					}
				}
				else {
					readBuffer = new byte[0];
					this.close();
				}
			}
			else if (txt_in != null) {
				if (this.fileType.equals(GDE.FILE_ENDING_STAR_TXT)) {
					StringBuffer sb = new StringBuffer();
					int value;

					sb.append('\f');
					while ((value = txt_in.read()) != -1 && value != '\f')
						sb.append((char) value);

					if (sb.length() > 1)
						readBuffer = sb.toString().getBytes();
					else
						this.close();
				}
				else if (this.fileType.equals(GDE.FILE_ENDING_STAR_LOG)) {
					String line;
					if ((line = txt_in.readLine()) != null) {
						while(!line.contains("WARNING") && !(line.contains("Read") && line.length() > line.indexOf("Read") + 15) && (line = txt_in.readLine()) != null) ;
						
						//System.out.println(line);
						if(line != null && line.contains("Read")) {
							boolean isQCdata = false;
							line = line.substring(line.indexOf("Read") + 15);
							StringTokenizer token = new StringTokenizer(line);
							StringBuffer sb = new StringBuffer();
							while (token.hasMoreElements()) {
								String nextByte = token.nextToken();
								if (nextByte.equals("02")) isQCdata = true;
								sb.append(nextByte);
							}
							//System.out.println(sb.toString());
							if (isQCdata) {
								//sync with QC data
								sb.delete(0, sb.indexOf("02"));
								if (sb.length() > readBuffer.length*2) {
									sb.delete(readBuffer.length*2, sb.length()-1);
								}
								else if (sb.length() < readBuffer.length*2) {
									sb.append(StringHelper.byte2Hex2CharString(this.read(new byte[readBuffer.length - sb.length()/2], 1000), (readBuffer.length - sb.length()/2)));
								}
							}
							byte[] tmpData = StringHelper.convert2ByteArray(sb.toString());
							System.arraycopy(tmpData, 0, readBuffer, 0, tmpData.length <= readBuffer.length ? tmpData.length : readBuffer.length);
							if (log.isLoggable(Level.FINE)) {
								log.logp(java.util.logging.Level.FINER, DeviceSerialPortImpl.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2FourDigitsIntegerString(readBuffer));
								log.logp(Level.FINE, DeviceSerialPortImpl.$CLASS_NAME, $METHOD_NAME, "  Read : " + StringHelper.byte2Hex2CharString(readBuffer, readBuffer.length));
							}
						}
						else { // WARNING, assume time out
							if (log.isLoggable(Level.TIME)) log.logp(Level.TIME, $CLASS_NAME, "read()", "delay " + timeout_msec);
							WaitTimer.delay(timeout_msec);
							throw new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { "*", timeout_msec })); //$NON-NLS-1$ 
						}
					}
					else {
						this.close();
						throw new EOFException();
					} 
				}
			}
		}
		return readBuffer;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#read(byte[], int, boolean)
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, boolean checkFailedQuery) throws IOException, FailedQueryException, TimeOutException {
		log.log(Level.WARNING, "read() not supported in simulation");
		return new byte[0];
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#read(byte[], int, int)
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex) throws IOException, TimeOutException {
		byte[] resultBuffer = new byte[0];
		try {
			waitForStableReceiveBuffer(readBuffer.length, timeout_msec, 100);
		}
		catch (InterruptedException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		if (this.isConnected) {
			if (data_in != null && this.fileType.equals(GDE.FILE_ENDING_STAR_LOV)) {
				Vector<Byte> tmpVector = new Vector<Byte>();
				byte tmpByte, lastByte = 0x00;
				try {
					while ((tmpByte = data_in.readByte()) != 0xff) {
						tmpVector.add(tmpByte);
						if (tmpByte == 0x0A && lastByte == 0x0D) 
							break;
						lastByte = tmpByte;
					}
					
					int size2Read = this.device.getLovDataByteSize() - readBuffer.length; //Math.abs(this.device.getDataBlockSize());
					byte[] tmpBuffer = new byte[size2Read];
					if (data_in.read(tmpBuffer) != size2Read) {
						//end of file reached
						this.close();
					}
				}
				catch (Exception e) {
					this.close();
				}

				if (this.device.getDataBlockLeader().length() > 0) {
					while (tmpVector.size() > 1 && tmpVector.get(0) != this.device.getDataBlockLeader().charAt(0))
						tmpVector.remove(0);
				}
				if (tmpVector.size() != readBuffer.length) {
					resultBuffer = new byte[tmpVector.size()];
					for (int i = 0; i < resultBuffer.length; i++) {
						resultBuffer[i] = tmpVector.get(i);
					}
				}
				else {
					for (int i = 0; i < readBuffer.length; i++) {
						readBuffer[i] = tmpVector.get(i);
					}
				}
			}
			else if (txt_in != null) {
				if (this.fileType.equals(GDE.FILE_ENDING_STAR_TXT)) {
					StringBuffer sb = new StringBuffer();
					int value;

					sb.append('\f');
					while ((value = txt_in.read()) != -1 && value != '\f')
						sb.append((char) value);

					if (sb.length() > 1)
						readBuffer = sb.toString().getBytes();
					else
						this.close();
				}
				else if (this.fileType.equals(GDE.FILE_ENDING_STAR_LOG)) {
					String line;
					if ((line = txt_in.readLine()) != null) {
						while(!line.contains("WARNING") && !line.contains("Read  data:") && (line = txt_in.readLine()) != null) ;
						
						//System.out.println(line);
						if(line != null && line.contains("Read  data:")) {
							boolean isQCdata = false;
							line = line.substring(line.indexOf("Read  data:") + 12);
							StringTokenizer token = new StringTokenizer(line);
							StringBuffer sb = new StringBuffer();
							while (token.hasMoreElements()) {
								String nextByte = token.nextToken();
								if (nextByte.equals("02")) isQCdata = true;
								sb.append(nextByte);
							}
							//System.out.println(sb.toString());
							if (isQCdata) {
								//sync with QC data
								sb.delete(0, sb.indexOf("02"));
								if (sb.length() > readBuffer.length*2) {
									sb.delete(readBuffer.length*2, sb.length()-1);
								}
								else if (sb.length() < readBuffer.length*2) {
									sb.append(StringHelper.byte2Hex2CharString(this.read(new byte[readBuffer.length - sb.length()/2], 1000), readBuffer.length - sb.length()/2));
								}
							}
							readBuffer = StringHelper.convert2ByteArray(sb.toString());
						}
						else { // WARNING, assume time out
							if (log.isLoggable(Level.TIME)) log.logp(Level.TIME, $CLASS_NAME, "read()", "delay " + timeout_msec);
							WaitTimer.delay(timeout_msec);
							throw new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { "*", timeout_msec })); //$NON-NLS-1$ 
						}
					}
					else
						this.close();
				}
			}
		}
		return resultBuffer.length == readBuffer.length ? readBuffer : resultBuffer;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#read(byte[], int, java.util.Vector)
	 */
	public byte[] read(byte[] readBuffer, int timeout_msec, Vector<Long> waitTimes) throws IOException, TimeOutException {
		try {
			wait4Bytes(readBuffer.length, timeout_msec);
		}
		catch (IOException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		if (this.isConnected) {
			if (data_in != null && this.fileType.equals(GDE.FILE_ENDING_STAR_LOV)) {
				if (data_in.read(readBuffer) > 0) {
					int size2Read = this.device.getLovDataByteSize() - Math.abs(this.device.getDataBlockSize());
					if (data_in.read(new byte[size2Read]) != size2Read) {
						log.log(Level.WARNING, "expected byte size to  read does not macht really red size of bytes !");
					}
				}
				else {
					readBuffer = new byte[0];
					this.close();
				}
			}
			else if (txt_in != null) {
				if (this.fileType.equals(GDE.FILE_ENDING_STAR_TXT)) {
					StringBuffer sb = new StringBuffer();
					int value;

					sb.append('\f');
					while ((value = txt_in.read()) != -1 && value != '\f')
						sb.append((char) value);

					if (sb.length() > 1)
						readBuffer = sb.toString().getBytes();
					else
						this.close();
				}
				else if (this.fileType.equals(GDE.FILE_ENDING_STAR_LOG)) {
					String line;
					if ((line = txt_in.readLine()) != null) {
						while(!line.contains("WARNING") && !line.contains("Read  data:") && (line = txt_in.readLine()) != null) ;
						
						//System.out.println(line);
						if(line != null && line.contains("Read  data:")) {
							boolean isQCdata = false;
							line = line.substring(line.indexOf("Read  data:") + 12);
							StringTokenizer token = new StringTokenizer(line);
							StringBuffer sb = new StringBuffer();
							while (token.hasMoreElements()) {
								String nextByte = token.nextToken();
								if (nextByte.equals("02")) isQCdata = true;
								sb.append(nextByte);
							}
							//System.out.println(sb.toString());
							if (isQCdata) {
								//sync with QC data
								sb.delete(0, sb.indexOf("02"));
								if (sb.length() > readBuffer.length*2) {
									sb.delete(readBuffer.length*2, sb.length()-1);
								}
								else if (sb.length() < readBuffer.length*2) {
									sb.append(StringHelper.byte2Hex2CharString(this.read(new byte[readBuffer.length - sb.length()/2], 1000), readBuffer.length - sb.length()/2));
								}
							}
							readBuffer = StringHelper.convert2ByteArray(sb.toString());
						}
						else { // WARNING, assume time out
							if (log.isLoggable(Level.TIME)) log.logp(Level.TIME, $CLASS_NAME, "read()", "delay " + timeout_msec);
							WaitTimer.delay(timeout_msec);
							throw new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { "*", timeout_msec })); //$NON-NLS-1$ 
						}
					}
					else
						this.close();
				}
			}
		}
		return readBuffer;
	}

	/**
	 * write bytes to serial port output stream, cleans receive buffer if available byes prior to send data 
	 * @param writeBuffer writes size of writeBuffer to output stream
	 * @throws IOException
	 */
	public void write(byte[] writeBuffer) throws IOException {
		log.log(Level.WARNING, "write() not supported in simulation");
	}

	/**
	 * write bytes to serial port output stream, each byte individual with the given time gap in msec
	 * cleans receive buffer if available byes prior to send data 
	 * @param writeBuffer writes size of writeBuffer to output stream
	 * @throws IOException
	 */
	public synchronized void write(byte[] writeBuffer, long gap_ms) throws IOException {
		log.log(Level.WARNING, "write() not supported in simulation");
	}

	/**
	 * cleanup the input stream if there are bytes available
	 * @return number of bytes in receive buffer which get removed
	 * @throws IOException
	 */
	public int cleanInputStream() throws IOException {
		log.log(Level.WARNING, "cleanInputStream() not supported in simulation");
		return 0;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#wait4Bytes(int)
	 */
	public long wait4Bytes(int timeout_msec) throws InterruptedException, TimeOutException, IOException {
		WaitTimer.delay(getWaitTime());
		return 0;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#wait4Bytes(int, int)
	 */
	public int wait4Bytes(int numBytes, int timeout_msec) throws IOException {
		WaitTimer.delay(getWaitTime());
		return numBytes;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#waitForStableReceiveBuffer(int, int, int)
	 */
	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_msec, int stableIndex) throws InterruptedException, TimeOutException, IOException {
		WaitTimer.delay(getWaitTime());
		return expectedBytes;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#waitForStableReceiveBuffer(int, int, int)
	 */
	public int getAvailableBytes() throws IOException {
		return this.isConnected ? 20 : 0;
	}

	/**
	 * query if the port is already open
	 * @return
	 */
	public boolean isConnected() {
		return this.isConnected;
	}

	/**
	 * @return number of transfer errors occur
	 */
	public int getXferErrors() {
		return this.xferErrors;
	}

	/**
	 * add up transfer errors
	 */
	public void addXferError() {
		this.xferErrors++;
	}

	/**
	 * @return number of timeout errors occur
	 */
	public int getTimeoutErrors() {
		return this.timeoutErrors;
	}

	/**
	 * add up timeout errors
	 */
	public void addTimeoutError() {
		this.timeoutErrors++;
	}

	/**
	 * check if a configured serial port string matches actual available ports
	 * @param newSerialPortStr
	 * @return true if given port string matches one of the available once
	 */
	public boolean isMatchAvailablePorts(String newSerialPortStr) {
		return true;
	}

	/**
	 * @return calculated sleep time in milli seconds
	 */
	int getWaitTime() {
		int sleepTime = 0;
		Random randomGenerator = new Random(new Date().getTime());
    if (this.isTimeStepConstant) {
    	sleepTime = this.sleepTime_ms;
    }
    else {
			while (sleepTime < this.sleepTime_ms/2 || sleepTime > this.sleepTime_ms*2) {
				sleepTime = randomGenerator.nextInt(this.sleepTime_ms);
			}
		}
		log.log(Level.TIME, "sleepTime : " + sleepTime + " ms");
		return sleepTime;
	}
}
