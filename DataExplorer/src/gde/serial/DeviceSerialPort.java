/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.serial;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.TooManyListenersException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.OSDE;
import osde.config.Settings;
import osde.device.DeviceConfiguration;
import osde.exception.ApplicationConfigurationException;
import osde.exception.ReadWriteOutOfSyncException;
import osde.exception.SerialPortException;
import osde.exception.TimeOutException;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;

/**
 * DeviceSerialPort is the abstract class of the serial port implementation as parent for a device specific serial port implementation
 * @author Winfried Brügmann
 */
public abstract class DeviceSerialPort implements SerialPortEventListener {
	final static Logger					log								= Logger.getLogger(DeviceSerialPort.class.getName());

	protected final DeviceConfiguration			deviceConfig;
	protected final OpenSerialDataExplorer 	application;
	protected SerialPort										serialPort 				= null;
	boolean																	isConnected				= false;
	String																	serialPortStr			= OSDE.STRING_EMPTY;
	Thread																	closeThread;
	
	static CommPortIdentifier			portId;
	static CommPortIdentifier			saveportId;

//	public final String							PORT_OPEN							= "offen";
//	public final String							PORT_CLOSED						= "geschlossen";

	private InputStream						inputStream				= null;
	private boolean								dataAvailable			= false;
	private OutputStream					outputStream			= null;

	protected String							name;
	
	protected int									xferErrors = 0;

	// flag if device has an version string
	protected boolean							hasVersion				= false;


  //public static final int STOPBITS_1 = 1;
  //public static final int STOPBITS_2 = 2;
  //public static final int STOPBITS_1_5 = 3;

  //public static final int PARITY_NONE = 0;
  //public static final int PARITY_ODD = 1;
  //public static final int PARITY_EVEN = 2;
  //public static final int PARITY_MARK = 3;
  //public static final int PARITY_SPACE = 4;

	//public static final int DATABITS_5 = 5;
  //public static final int DATABITS_6 = 6;
  //public static final int DATABITS_7 = 7;
  //public static final int DATABITS_8 = 8;
  //public static final int FLOWCONTROL_NONE = 0;
  //public static final int FLOWCONTROL_RTSCTS_IN = 1;
  //public static final int FLOWCONTROL_RTSCTS_OUT = 2;
  //public static final int FLOWCONTROL_XONXOFF_IN = 4;
  //public static final int FLOWCONTROL_XONXOFF_OUT = 8;


	@SuppressWarnings("unchecked") //$NON-NLS-1$
	public DeviceSerialPort(DeviceConfiguration currentDeviceConfig, OpenSerialDataExplorer currentApplication) {
		this.deviceConfig = currentDeviceConfig;
		this.application = currentApplication;
	}

	/**
	 * @return a vector with actual available ports at the system
	 */
	public static Vector<String> listConfiguredSerialPorts() {
		log.fine("entry"); //$NON-NLS-1$
		
		Vector<String> availablePorts = new Vector<String>(1, 1);
		
		availablePorts = getAvailablePorts(availablePorts); //Windows COM1, COM2 -> COM20
		// Linux /dev/ttyS0, /dev/ttyS1, /dev/ttyUSB0, /dev/ttyUSB1
		availablePorts.trimToSize();
		
		log.fine("exit"); //$NON-NLS-1$
		return availablePorts;
	}

	/**
	 * find the serial ports using the given string prefix
	 * @param availablePorts
	 */
	@SuppressWarnings("unchecked") //$NON-NLS-1$
	private static Vector<String> getAvailablePorts(Vector<String> availablePorts) {
		String serialPortStr;
		Enumeration<CommPortIdentifier> enumIdentifiers = CommPortIdentifier.getPortIdentifiers(); // initializes serial port
		// find all available serial ports
		while (enumIdentifiers.hasMoreElements()) {
			CommPortIdentifier commPortIdentifier = enumIdentifiers.nextElement();
				if (commPortIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL && !commPortIdentifier.isCurrentlyOwned()) {
					serialPortStr = commPortIdentifier.getName();
					try {
						if(Settings.getInstance().doPortAvailabilityCheck()) {
							((SerialPort) commPortIdentifier.open("OpenSerialDataExplorer", 2000)).close(); //$NON-NLS-1$
						}
						availablePorts.add(serialPortStr);
						if (log.isLoggable(Level.FINER)) log.finer("Found available port: " + serialPortStr); //$NON-NLS-1$
					}
					catch (Exception e) {
						if (log.isLoggable(Level.FINER)) log.finer("Found port, but can't open: " + serialPortStr); //$NON-NLS-1$
					}
				}
		}
		if (log.isLoggable(Level.FINE)) {
			StringBuilder sb = new StringBuilder().append("Available serial Ports : "); //$NON-NLS-1$
			for (String comPort : availablePorts) {
				sb.append(comPort).append(" "); //$NON-NLS-1$
			}
			log.fine(sb.toString());
		}
		return availablePorts;
	}

	/**
	 * check if a configures serial port string matches actual available ports
	 * @param newSerialPortStr
	 * @param availableSerialPorts
	 * @return true if given port string matches one of the available once
	 */
	private boolean isMatchAvailablePorts(String newSerialPortStr, Vector<String> availableSerialPorts) {
		boolean match = false;
		for (String availablePort : availableSerialPorts) {
			if (availablePort.equals(newSerialPortStr)) {
				match = true;
				break;
			}
		}
		return match;
	}
	
	public synchronized SerialPort open() throws ApplicationConfigurationException, SerialPortException {
		this.xferErrors = 0;
		// Initialize serial port
		try {
			Settings settings = Settings.getInstance();
			this.serialPortStr = settings.isGlobalSerialPort() ? settings.getSerialPort() : this.deviceConfig.getPort();
			// check if a serial port is selected to be opened
			Vector<String> availableSerialPorts = listConfiguredSerialPorts();
			if (this.serialPortStr == null || this.serialPortStr.length() < 4 || !isMatchAvailablePorts(this.serialPortStr, availableSerialPorts)) {
				// no serial port is selected, if only one serial port is available choose this one
				if (availableSerialPorts.size() == 1) {
					this.serialPortStr = availableSerialPorts.firstElement();
					if (settings.isGlobalSerialPort())
						settings.setSerialPort(this.serialPortStr);
					else
						this.deviceConfig.setPort(this.serialPortStr);
					
					this.deviceConfig.storeDeviceProperties();
					this.application.updateTitleBar(this.deviceConfig.getName(), this.deviceConfig.getPort());
				}
				else {
//					application.openMessageDialog("Es ist kein serieller Port für das ausgewählte Gerät konfiguriert !");
//					application.getDeviceSelectionDialog().open();
					throw new ApplicationConfigurationException(Messages.getString(MessageIds.OSDE_MSGE0010));
				}
			}
			log.fine(String.format("serialPortString = %s; baudeRate = %d; dataBits = %d; stopBits = %d; parity = %d; flowControlMode = %d; RTS = %s; DTR = %s", this.serialPortStr, this.deviceConfig.getBaudeRate(), this.deviceConfig.getDataBits(), this.deviceConfig.getStopBits(), this.deviceConfig.getParity(), this.deviceConfig.getFlowCtrlMode(), this.deviceConfig.isRTS(), this.deviceConfig.isDTR())); //$NON-NLS-1$
			
			portId = CommPortIdentifier.getPortIdentifier(this.serialPortStr);
			this.serialPort = (SerialPort) portId.open("OpenSerialDataExplorer", 2000);
			// set port parameters
			this.serialPort.setInputBufferSize(4096);
			this.serialPort.setOutputBufferSize(4096);
			this.serialPort.setSerialPortParams(this.deviceConfig.getBaudeRate(), this.deviceConfig.getDataBits(), this.deviceConfig.getStopBits(), this.deviceConfig.getParity());
			this.serialPort.setFlowControlMode(this.deviceConfig.getFlowCtrlMode());
			this.serialPort.setRTS(this.deviceConfig.isRTS());
			this.serialPort.setDTR(this.deviceConfig.isDTR());

			this.serialPort.addEventListener(this);
			// activate the DATA_AVAILABLE notifier to read available data
			this.serialPort.notifyOnDataAvailable(true);

			// init in and out stream for writing and reading
			this.inputStream = this.serialPort.getInputStream();
			this.outputStream = this.serialPort.getOutputStream();

			this.isConnected = true;
			if (this.application != null) this.application.setPortConnected(true);
			return this.serialPort;
		}
		catch (ApplicationConfigurationException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			if (this.serialPort != null) this.serialPort.close();
			throw e;
		}
		catch (IOException e) {
			SerialPortException en = new SerialPortException(e.getMessage());
			log.log(Level.SEVERE, en.getMessage(), en);
			if (this.serialPort != null) this.serialPort.close();
			throw en;
		}
		catch (NoSuchPortException e) {
			SerialPortException en = new SerialPortException(e.getMessage());
			log.log(Level.SEVERE, en.getMessage(), en);
			if (this.serialPort != null) this.serialPort.close();
			throw en;
		}
		catch (UnsupportedCommOperationException e) {
			SerialPortException en = new SerialPortException(e.getMessage());
			log.log(Level.SEVERE, en.getMessage(), en);
			if (this.serialPort != null) this.serialPort.close();
			throw en;
		}
		catch (TooManyListenersException e) {
			SerialPortException en = new SerialPortException(e.getMessage());
			log.log(Level.SEVERE, en.getMessage(), en);
			if (this.serialPort != null) this.serialPort.close();
			throw en;
		}
		catch (PortInUseException e) {
			SerialPortException en = new SerialPortException(e.getMessage());
			log.log(Level.SEVERE, en.getMessage(), en);
			if (this.serialPort != null) this.serialPort.close();
			throw en;
		}
	}

	public synchronized void write(byte[] buf) throws IOException {
		int num = 0;
		if ((num = this.inputStream.available()) != 0) {
			log.warning("clean inputStreaam left bytes -> " + this.inputStream.read(new byte[num])); //$NON-NLS-1$
		}

		try {
			if (this.application != null) this.application.setSerialTxOn();

			if (log.isLoggable(Level.FINE)) {
				StringBuffer sb = new StringBuffer();
				sb.append("Write data: "); //$NON-NLS-1$
				for (int i = 0; i < buf.length; i++) {
					sb.append(String.format("%02X ", buf[i])); //$NON-NLS-1$
				}
				sb.append(" to port ").append(this.serialPort.getName()).append(System.getProperty("line.separator")); //$NON-NLS-1$ //$NON-NLS-2$
				log.fine(sb.toString());
			}

			// write string to serial port
			this.outputStream.write(buf);
			//this.outputStream.flush();
			if (this.application != null) this.application.setSerialTxOff();
		}
		catch (IOException e) {
			throw e;
		}
	}

	public synchronized void serialEvent(SerialPortEvent event) {

		switch (event.getEventType()) {
		case SerialPortEvent.BI:
		case SerialPortEvent.OE:
		case SerialPortEvent.FE:
		case SerialPortEvent.PE:
		case SerialPortEvent.CD:
		case SerialPortEvent.CTS:
		case SerialPortEvent.DSR:
		case SerialPortEvent.RI:
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
			this.dataAvailable = false;
			break;
		case SerialPortEvent.DATA_AVAILABLE:
			this.dataAvailable = true;
			break;
		}
	}

	/**
	 *  read number of given bytes by the length of the referenced read buffer in a given time frame defined by time out value
	 * @param bytes
	 * @param timeoutInSeconds
	 * @param waitTimes
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized byte[] read(byte[] readBuffer, int timeoutInSeconds) throws IOException, TimeOutException {
		int sleepTime = 10; // ms
		int bytes = readBuffer.length;
		int readBytes = 0;
		int retryCounter = timeoutInSeconds * 1000 / sleepTime;

		try {
			if (this.application != null) this.application.setSerialRxOn();

			wait4Bytes(timeoutInSeconds);

			Thread.sleep(18);
			while (bytes != readBytes && retryCounter-- > 0){
				readBytes += this.inputStream.read(readBuffer, 0 + readBytes, bytes - readBytes);
				if (bytes != readBytes) Thread.sleep(sleepTime);
			}
			if (retryCounter <= 0) {
				TimeOutException e = new TimeOutException(Messages.getString(MessageIds.OSDE_MSGE0011, new Object[] { bytes, timeoutInSeconds })); 
				log.log(Level.SEVERE, e.getMessage(), e);
				throw e;
			}
			
			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				sb.append("Read  data: "); //$NON-NLS-1$
				for (int i = 0; i < readBuffer.length; i++) {
					sb.append(String.format("%02X ", readBuffer[i])); //$NON-NLS-1$
				}
				log.fine(sb.toString());
			}

			if (this.application != null) this.application.setSerialRxOff();
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		catch (InterruptedException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		return readBuffer;
	}

	/**
	 * read number of given bytes by the length of the referenced read buffer in a given time frame defined by time out value
	 * the reference to the wait time vector will add the actual wait time to have the read buffer ready to read the given number of bytes
	 * @param bytes
	 * @param timeoutInSeconds
	 * @param waitTimes
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized byte[] read(byte[] readBuffer, int timeoutInSeconds, Vector<Long> waitTimes) throws IOException, TimeOutException {
		int sleepTime = 10; // ms
		int bytes = readBuffer.length;
		int readBytes = 0;
		int retryCounter = timeoutInSeconds * 1000 / sleepTime;

		try {
			long startTime_ms = new Date().getTime();
			wait4Bytes(timeoutInSeconds);

			if (this.application != null) this.application.setSerialRxOn();

			Thread.sleep(18);
			while (bytes != readBytes && retryCounter-- > 0){
				readBytes += this.inputStream.read(readBuffer, readBytes, bytes - readBytes);
				if (bytes != readBytes) Thread.sleep(sleepTime);
			}
			if (retryCounter <= 0) {
				TimeOutException e = new TimeOutException(Messages.getString(MessageIds.OSDE_MSGE0011, new Object[] { bytes, timeoutInSeconds }));
				log.log(Level.SEVERE, e.getMessage(), e);
				throw e;
			}
			
			long ms = (new Date().getTime()) - startTime_ms;
			log.info("waitTime = " + ms);
			waitTimes.add(ms);
			
			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				sb.append("Read  data: "); //$NON-NLS-1$
				for (int i = 0; i < readBuffer.length; i++) {
					sb.append(String.format("%02X ", readBuffer[i])); //$NON-NLS-1$
				}
				log.fine(sb.toString());
			}

			if (this.application != null) this.application.setSerialRxOff();
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		catch (InterruptedException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		return readBuffer;
	}

	/**
	 * function check transmission finished
	 * @return false if there are available bytes
	 * @throws InterruptedException 
	 * @throws TimeOutException 
	 */
	public void wait4Bytes(int timeout_sec) throws InterruptedException, TimeOutException {
		int sleepTime = 5;
		int timeCounter = timeout_sec * 1000 / sleepTime;

		while (!this.dataAvailable && timeCounter-- > 0) {
			Thread.sleep(sleepTime);	
			if (timeCounter-- <= 0) {
				TimeOutException e = new TimeOutException(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0011, new Object[] { 0, timeout_sec })); 
				log.log(Level.WARNING, e.getMessage(), e);
				throw e;
			}
		}
	}

	/**
	 * waits until receive buffer is filled with the number of expected bytes
	 * @param numBytes
	 * @param timeoutInSeconds
	 * @param isTerminatedByChecksum
	 * @return number of bytes in receive buffer
	 * @throws TimeOutException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public int wait4Bytes(int numBytes, int timeoutInSeconds) throws TimeOutException, IOException, InterruptedException {
		int counter = timeoutInSeconds * 1000;
		int resBytes = 0;

			// wait until readbuffer has been filled by eventListener
		while (this.inputStream.available() < numBytes) {
			Thread.sleep(3, 1);
			counter--;
			//if(log.isLoggable(Level.FINER)) log.finer("time out counter = " + counter);
			if (counter <= 0) {
				TimeOutException e = new TimeOutException(Messages.getString(MessageIds.OSDE_MSGE0011, new Object[] { numBytes, timeoutInSeconds }));
				log.log(Level.SEVERE, e.getMessage(), e);
				throw e;
			}
		}
		
		return resBytes;
	}
	/**
	 * read number of given bytes by the length of the referenced read buffer in a given time frame defined by time out value
	 * if the readBuffer kan not be filled a stable counter will be active where a number of retries can be specified
	 * @param bytes
	 * @param timeoutInSeconds
	 * @param waitTimes
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized byte[] read(byte[] readBuffer, int timeoutInSeconds, int stableIndex) throws IOException, TimeOutException {
		int sleepTime = 2; // ms
		int expectedBytes = readBuffer.length;
		int readBytes = 0;
		int lastRead = 0;
		boolean isStable = false;
		int timeCounter = timeoutInSeconds * 1000 / (sleepTime*10);
		int stableCounter = stableIndex;
		if (stableIndex >= timeCounter) {
			log.severe(Messages.getString(MessageIds.OSDE_MSGE0013));
		}


		try {
			wait4Bytes(timeoutInSeconds);

			if (this.application != null) this.application.setSerialRxOn();

			Thread.sleep(18);
			while (readBytes < expectedBytes && !isStable) {
				readBytes += this.inputStream.read(readBuffer, 0 + readBytes, expectedBytes - readBytes);
				//log.info("readBytes " + readBytes + " available " + this.inputStream.available());
				if (expectedBytes != readBytes) {
					Thread.sleep(sleepTime);
				}
				if (lastRead == readBytes) {
					//log.info("stableCounter " + stableCounter + " timecounter " + timeCounter);
					if (stableCounter-- == 0) {
						isStable = true;
					}
				}
				else {
					lastRead = readBytes;
					stableCounter = stableIndex;
				}
				if (timeCounter-- <= 0) {
					TimeOutException e = new TimeOutException(Messages.getString(MessageIds.OSDE_MSGE0011, new Object[] { expectedBytes, timeoutInSeconds }));
					log.log(Level.SEVERE, e.getMessage(), e);
					throw e;
				}
				//log.info("timeCounter " + timeCounter);
			}
			
			if (readBytes < readBuffer.length) { // resize the data buffer to real red data 
				byte[] tmpBuffer = new byte[readBytes];
				System.arraycopy(readBuffer, 0, tmpBuffer, 0, readBytes);
				readBuffer = tmpBuffer;
			}

			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				sb.append("Read  data: "); //$NON-NLS-1$
				for (int i = 0; i < readBuffer.length; i++) {
					sb.append(String.format("%02X ", readBuffer[i])); //$NON-NLS-1$
				}
				log.fine(sb.toString());
			}

			if (this.application != null) this.application.setSerialRxOff();
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		catch (InterruptedException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		return readBuffer;
	}

	/**
	 * waits until receive buffer is filled with number of expected bytes or does not change anymore in 100 msec (50 cycles * 2 msec)
	 * @param expectedBytes
	 * @param timeout_sec in seconds, this is the maximum time this process will wait for stable byte count or maxBytes
	 * @return number of bytes in receive buffer
	 * @throws InterruptedException 
	 * @throws TimeOutException 
	 * @throws IOException 
	 */
	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_sec) throws InterruptedException, TimeOutException, IOException {
		int sleepTime = 3; // ms
		int timeCounter = timeout_sec * 1000 / sleepTime;
		int stableCounter = 50;
		boolean isStable = false;
		boolean isTimedOut = false;

		// availableBytes are updated by event handler
		int byteCounter = 0, numBytesAvailable = 0;
		Thread.sleep(15);
		while (byteCounter < expectedBytes && !isStable && !isTimedOut) {
			Thread.sleep(sleepTime);

			if (byteCounter == (numBytesAvailable = this.inputStream.available()))
				--stableCounter;
			else
				stableCounter = 50;

			if (stableCounter == 0) isStable = true;

			byteCounter = numBytesAvailable;
			--timeCounter;

			if (log.isLoggable(Level.INFO)) {
				log.info("stableCounter = " + stableCounter + " timeCounter = " + timeCounter);
			}
			if (timeCounter == 0) {
				//this.close();
				TimeOutException e = new TimeOutException(Messages.getString(MessageIds.OSDE_MSGE0011, new Object[] { expectedBytes, timeout_sec })); 
				log.log(Level.SEVERE, e.getMessage(), e);
				throw e;
			}

		} // end while

		return byteCounter;
	}

	/**
	 * function check for left bytes on receive buffer -> called to check wait for stable bytes missed
	 * @throws ReadWriteOutOfSyncException 
	 * @throws IOException 
	 */
	public void checkForLeftBytes() throws ReadWriteOutOfSyncException, IOException {
		//check available bytes in receive buffer == 0
		if (log.isLoggable(Level.FINER)) log.finer("inputStream available bytes = " + this.inputStream.available()); //$NON-NLS-1$
		if (this.inputStream.available() != 0) throw new ReadWriteOutOfSyncException(Messages.getString(MessageIds.OSDE_MSGE0014));
	}

	public synchronized void close() {
		if (this.isConnected && DeviceSerialPort.this.serialPort != null) {
			this.closeThread = new Thread() {
				public void run() {
					log.info("entry"); //$NON-NLS-1$
					try {
						Thread.sleep(2);
						byte[] buf = new byte[getInputStream().available()];
						if (buf.length > 0) getInputStream().read(buf);
					}
					catch (Exception e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
					log.info("before close"); //$NON-NLS-1$
					DeviceSerialPort.this.serialPort.close();
					log.info("after close"); //$NON-NLS-1$
					DeviceSerialPort.this.isConnected = false;
					if (DeviceSerialPort.this.application != null) DeviceSerialPort.this.application.setPortConnected(false);
					log.info("exit"); //$NON-NLS-1$
				}
			};
			this.closeThread.start();
		}
	}

	public InputStream getInputStream() {
		return this.inputStream;
	}

	public OutputStream getOutputStream() {
		return this.outputStream;
	}

	public boolean isConnected() {
		return this.isConnected;
	}
	
	/**
	 * sys out map content
	 * @param map
	 */
	public void print(HashMap<String, Object> map) {
		String[] dataNameKeys = map.keySet().toArray(new String[1]);
		for (String key : dataNameKeys) {
			System.out.println(key + " = " + map.get(key)); //$NON-NLS-1$
		}
	}

	/**
	 * @return the serialPortStr
	 */
	public String getSerialPortStr() {
		return this.serialPortStr == null ? this.deviceConfig.getPort() : this.serialPortStr;
	}

	/**
	 * @param newSerialPortStr the serialPortStr to set
	 */
	public void setSerialPortStr(String newSerialPortStr) {
		this.serialPortStr = newSerialPortStr;
	}

	/**
	 * @return number of transfer errors occur (checksum)
	 */
	public int getXferErrors() {
		return this.xferErrors;
	}
}
