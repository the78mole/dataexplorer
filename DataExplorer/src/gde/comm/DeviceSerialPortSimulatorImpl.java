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

import gde.GDE;
import gde.config.Settings;
import gde.device.IDevice;
import gde.exception.ApplicationConfigurationException;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
	@Override
	public SerialPort open() throws ApplicationConfigurationException, SerialPortException {
		try {
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
			FileDialog openFileDialog = this.application.openFileOpenDialog("Open File used as simulation input", new String[] { GDE.FILE_ENDING_STAR_LOV, GDE.FILE_ENDING_STAR_TXT, GDE.FILE_ENDING_STAR_LOG }, path, null, SWT.SINGLE);
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
			if (this.application != null) this.application.setPortConnected(this.isConnected);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#close()
	 */
	@Override
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
	@Override
	public byte[] read(byte[] readBuffer, int timeout_msec) throws IOException, TimeOutException {
		try {
			wait4Bytes(timeout_msec);
		}
		catch (InterruptedException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		if (this.isConnected) {
			if (data_in != null && this.fileType.equals(GDE.FILE_ENDING_STAR_LOV)) {
				if (data_in.read(readBuffer) > 0) {
					data_in.read(new byte[this.device.getLovDataByteSize() - this.device.getDataBlockSize()]);
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
						line = getHexDataLine(line);
						if (line != null) {
							//System.out.println(line);
							StringTokenizer token = new StringTokenizer(line);
							StringBuffer sb = new StringBuffer();
							while (token.hasMoreElements()) {
								sb.append(token.nextElement());
							}
							//System.out.println(sb.toString());
							readBuffer = StringHelper.convert2ByteArray(sb.toString());
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
	 * @param line
	 * @return
	 * @throws IOException
	 */
	String getHexDataLine(String line) throws IOException {
		while(!line.contains("Read  data:") && (line = txt_in.readLine()) != null) ;
		if(line != null) {
			line = line.substring(line.indexOf("Read  data:") + 12);
		}
		return line == null || line.length() > 1 ? line : getHexDataLine(line);
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#read(byte[], int, int)
	 */
	@Override
	public byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex) throws IOException, TimeOutException {
		try {
			waitForStableReceiveBuffer(readBuffer.length, timeout_msec, 100);
		}
		catch (InterruptedException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		if (this.isConnected) {
			if (data_in != null && this.fileType.equals(GDE.FILE_ENDING_STAR_LOV)) {
				data_in.read(readBuffer);
				data_in.read(new byte[this.device.getLovDataByteSize() - this.device.getDataBlockSize()]);
			}
			else if (txt_in != null && this.fileType.equals(GDE.FILE_ENDING_STAR_TXT)) {
				char[] cbuf = new char[readBuffer.length];
				if (txt_in.read(cbuf) > 0) {
					for (int i = 0, j = 0; j < cbuf.length; i++, j++) {
						if (cbuf[j] == '\\' && cbuf[j + 1] == 'f') {
							readBuffer = new byte[readBuffer.length - 1];
							readBuffer[j] = 12;
							j++;
						}
						else
							readBuffer[i] = (byte) cbuf[j];
					}
				}
			}
		}
		return readBuffer;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#read(byte[], int, java.util.Vector)
	 */
	@Override
	public byte[] read(byte[] readBuffer, int timeout_msec, Vector<Long> waitTimes) throws IOException, TimeOutException {
		try {
			wait4Bytes(readBuffer.length, timeout_msec);
		}
		catch (IOException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		if (this.isConnected) {
			if (data_in != null && this.fileType.equals(GDE.FILE_ENDING_STAR_LOV)) {
				data_in.read(readBuffer);
				data_in.read(new byte[this.device.getLovDataByteSize() - this.device.getDataBlockSize()]);
			}
			else if (txt_in != null && this.fileType.equals(GDE.FILE_ENDING_STAR_TXT)) {
				char[] cbuf = new char[readBuffer.length];
				if (txt_in.read(cbuf) > 0) {
					for (int i = 0, j = 0; j < cbuf.length; i++, j++) {
						if (cbuf[j] == '\\' && cbuf[j + 1] == 'f') {
							readBuffer = new byte[readBuffer.length - 1];
							readBuffer[j] = 12;
							j++;
						}
						else
							readBuffer[i] = (byte) cbuf[j];
					}
				}
			}
		}
		return readBuffer;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#write(byte[])
	 */
	@Override
	public void write(byte[] writeBuffer) throws IOException {
		log.log(Level.WARNING, "write() not supported in simulation");
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#wait4Bytes(int)
	 */
	@Override
	public long wait4Bytes(int timeout_msec) throws InterruptedException, TimeOutException, IOException {
		WaitTimer.delay(getWaitTime());
		return 0;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#wait4Bytes(int, int)
	 */
	@Override
	public int wait4Bytes(int numBytes, int timeout_msec) throws IOException {
		WaitTimer.delay(getWaitTime());
		return numBytes;
	}

	/* (non-Javadoc)
	 * @see gde.serial.IDeviceSerialPort#waitForStableReceiveBuffer(int, int, int)
	 */
	@Override
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
	 * @return number of transfer errors occur (checksum)
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
		Random randomGenerator = new Random();
    if (this.isTimeStepConstant) {
    	sleepTime = this.sleepTime_ms;
    }
    else {
			while (sleepTime < this.sleepTime_ms/3 || sleepTime > this.sleepTime_ms) {
				sleepTime = randomGenerator.nextInt(this.sleepTime_ms);
			}
		}
		log.log(Level.TIME, "sleepTime : " + sleepTime + " ms");
		return sleepTime;
	}
}
