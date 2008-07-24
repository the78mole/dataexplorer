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
	protected int														xferErrors 				= 0;
	
	boolean																	isConnected				= false;
	String																	serialPortStr			= OSDE.STRING_EMPTY;
	Thread																	closeThread;
	
	static CommPortIdentifier								portId;
	static CommPortIdentifier								saveportId;
	static Vector<String> 									availablePorts 		= new Vector<String>();

	InputStream															inputStream				= null;
	OutputStream														outputStream			= null;
	
	// event handling does not work reliable
	//boolean																	dataAvailable			= false;

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


	public DeviceSerialPort(DeviceConfiguration currentDeviceConfig, OpenSerialDataExplorer currentApplication) {
		this.deviceConfig = currentDeviceConfig;
		this.application = currentApplication;
	}

	/**
	 * @return a vector with actual available ports at the system
	 */
	public static Vector<String> listConfiguredSerialPorts() {
		log.fine("entry"); //$NON-NLS-1$
		
		availablePorts = getAvailablePorts();
		// Windows COM1, COM2 -> COM20
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
	private static Vector<String> getAvailablePorts() {
		String serialPortStr;
		Enumeration<CommPortIdentifier> enumIdentifiers = CommPortIdentifier.getPortIdentifiers(); // initializes serial port
		availablePorts.clear();
		
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
	
	/**
	 * opens the serial port specified in device configuration or settings (global)
	 * @return reference to instance of serialPort
	 * @throws ApplicationConfigurationException
	 * @throws SerialPortException
	 */
	public synchronized SerialPort open() throws ApplicationConfigurationException, SerialPortException {
		this.xferErrors = 0;
		// Initialize serial port
		try {
			Settings settings = Settings.getInstance();
			this.serialPortStr = settings.isGlobalSerialPort() ? settings.getSerialPort() : this.deviceConfig.getPort();
			// check if a serial port is selected to be opened
			if(availablePorts.size() == 0 ) availablePorts = listConfiguredSerialPorts();
			if (this.serialPortStr == null || this.serialPortStr.length() < 4 || !isMatchAvailablePorts(this.serialPortStr, availablePorts)) {
				// no serial port is selected, if only one serial port is available choose this one
				if (availablePorts.size() == 1) {
					this.serialPortStr = availablePorts.firstElement();
					if (settings.isGlobalSerialPort())
						settings.setSerialPort(this.serialPortStr);
					else
						this.deviceConfig.setPort(this.serialPortStr);
					
					this.deviceConfig.storeDeviceProperties();
					this.application.updateTitleBar(this.deviceConfig.getName(), this.deviceConfig.getPort());
				}
				else {
					throw new ApplicationConfigurationException(Messages.getString(MessageIds.OSDE_MSGE0010));
				}
			}
			log.fine(String.format("serialPortString = %s; baudeRate = %d; dataBits = %d; stopBits = %d; parity = %d; flowControlMode = %d; RTS = %s; DTR = %s", this.serialPortStr, this.deviceConfig.getBaudeRate(), this.deviceConfig.getDataBits(), this.deviceConfig.getStopBits(), this.deviceConfig.getParity(), this.deviceConfig.getFlowCtrlMode(), this.deviceConfig.isRTS(), this.deviceConfig.isDTR())); //$NON-NLS-1$
			
			portId = CommPortIdentifier.getPortIdentifier(this.serialPortStr);
			this.serialPort = (SerialPort) portId.open("OpenSerialDataExplorer", 2000);
			// set port parameters
			this.serialPort.setInputBufferSize(2048);
			this.serialPort.setOutputBufferSize(2048);
			this.serialPort.setSerialPortParams(this.deviceConfig.getBaudeRate(), this.deviceConfig.getDataBits(), this.deviceConfig.getStopBits(), this.deviceConfig.getParity());
			this.serialPort.setFlowControlMode(this.deviceConfig.getFlowCtrlMode());
			this.serialPort.setRTS(this.deviceConfig.isRTS());
			this.serialPort.setDTR(this.deviceConfig.isDTR());

			// event handling does not work reliable
			//this.serialPort.addEventListener(this);
			// activate the DATA_AVAILABLE notifier to read available data
			//this.serialPort.notifyOnDataAvailable(true);
			// activate the OUTPUT_BUFFER_EMPTY notifier
			//this.serialPort.notifyOnOutputEmpty(true);

			// init in and out stream for writing and reading
			this.inputStream = this.serialPort.getInputStream();
			this.outputStream = this.serialPort.getOutputStream();

			this.isConnected = true;
			if (this.application != null) this.application.setPortConnected(true);
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
		// event handling does not work reliable
		//catch (TooManyListenersException e) {
		//	SerialPortException en = new SerialPortException(e.getMessage());
		//	log.log(Level.SEVERE, en.getMessage(), en);
		//	if (this.serialPort != null) this.serialPort.close();
		//	throw en;
		//}
		catch (PortInUseException e) {
			SerialPortException en = new SerialPortException(e.getMessage());
			log.log(Level.SEVERE, en.getMessage(), en);
			if (this.serialPort != null) this.serialPort.close();
			throw en;
		}
		
		return this.serialPort;
	}

	/**
	 * write bytes to serial port output stream, cleans receive buffer if available byes prior to send data 
	 * @param writeBuffer writes size of writeBuffer to output stream
	 * @throws IOException
	 */
	public synchronized void write(byte[] writeBuffer) throws IOException {
		int num = 0;
		if ((num = this.inputStream.available()) != 0) {
			log.warning("clean inputStreaam left bytes -> " + this.inputStream.read(new byte[num])); //$NON-NLS-1$
		}

		try {
			if (this.application != null) this.application.setSerialTxOn();

			if (log.isLoggable(Level.FINE)) {
				StringBuffer sb = new StringBuffer();
				sb.append("Write data: "); //$NON-NLS-1$
				for (int i = 0; i < writeBuffer.length; i++) {
					sb.append(String.format("%02X ", writeBuffer[i])); //$NON-NLS-1$
				}
				sb.append(" to port ").append(this.serialPort.getName()).append(System.getProperty("line.separator")); //$NON-NLS-1$ //$NON-NLS-2$
				log.fine(sb.toString());
			}

			// write string to serial port
			this.outputStream.write(writeBuffer);
			//this.outputStream.flush();
			if (this.application != null) this.application.setSerialTxOff();
		}
		catch (IOException e) {
			throw e;
		}
	}

	/**
	 * event handler method handles only events as previous registered
	 * - activate the DATA_AVAILABLE notifier to read available data -> dataAvailable = true;
	 * - activate the OUTPUT_BUFFER_EMPTY notifier -> dataAvailable = false;
	 */
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
			//this.dataAvailable = false;
			log.fine("OUTPUT_BUFFER_EMPTY");
			break;
		case SerialPortEvent.DATA_AVAILABLE:
			//this.dataAvailable = true;
			log.fine("DATA_AVAILABLE");
			break;
		}
	}

	/**
	 * read number of given bytes by the length of the referenced read buffer in a given time frame defined by time out value
	 * @param bytes
	 * @param timeout_msec
	 * @param waitTimes
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized byte[] read(byte[] readBuffer, int timeout_msec) throws IOException, TimeOutException {
		int sleepTime = 4; // ms
		int bytes = readBuffer.length;
		int readBytes = 0;
		int timeOutCounter = timeout_msec / sleepTime;

		try {
			if (this.application != null) this.application.setSerialRxOn();
			wait4Bytes(bytes, timeout_msec);

			while (bytes != readBytes && timeOutCounter-- > 0){
				readBytes += this.inputStream.read(readBuffer, 0 + readBytes, bytes - readBytes);
				if (bytes != readBytes) Thread.sleep(sleepTime);
			}
			//this.dataAvailable = false;
			if (timeOutCounter <= 0) {
				TimeOutException e = new TimeOutException(Messages.getString(MessageIds.OSDE_MSGE0011, new Object[] { bytes, timeout_msec })); 
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
	 * @param timeout_msec
	 * @param waitTimes
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized byte[] read(byte[] readBuffer, int timeout_msec, Vector<Long> waitTimes) throws IOException, TimeOutException {
		int sleepTime = 4; // ms
		int bytes = readBuffer.length;
		int readBytes = 0;
		int timeOutCounter = timeout_msec / sleepTime;

		try {
			long startTime_ms = new Date().getTime();
			if (this.application != null) this.application.setSerialRxOn();
			wait4Bytes(timeout_msec);

			while (bytes != readBytes && timeOutCounter-- > 0){
				readBytes += this.inputStream.read(readBuffer, readBytes, bytes - readBytes);
				if (bytes != readBytes) Thread.sleep(sleepTime);
			}
			//this.dataAvailable = false;
			if (timeOutCounter <= 0) {
				TimeOutException e = new TimeOutException(Messages.getString(MessageIds.OSDE_MSGE0011, new Object[] { bytes, timeout_msec }));
				log.log(Level.SEVERE, e.getMessage(), e);
				throw e;
			}
			
			long ms = (new Date().getTime()) - startTime_ms;
			log.fine("waitTime = " + ms);
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
	 * function check for available bytes on receive buffer
	 * @return false if there are available bytes
	 * @throws InterruptedException 
	 * @throws TimeOutException 
	 * @throws IOException 
	 */
	public void wait4Bytes(int timeout_msec) throws InterruptedException, TimeOutException, IOException {
		int sleepTime = 2;
		int timeOutCounter = timeout_msec / sleepTime;

		while (0 == this.inputStream.available()) {
			Thread.sleep(sleepTime);	
			if (timeOutCounter-- <= 0) {
				TimeOutException e = new TimeOutException(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0011, new Object[] { "*", timeout_msec })); //$NON-NLS-1$ 
				log.log(Level.WARNING, e.getMessage(), e);
				throw e;
			}
		}
	}

	/**
	 * waits until receive buffer is filled with the number of expected bytes while checking inputStream
	 * @param numBytes
	 * @param timeout_msec
	 * @return number of bytes in receive buffer
	 * @throws TimeOutException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public int wait4Bytes(int numBytes, int timeout_msec) throws TimeOutException, IOException, InterruptedException {
		int sleepTime = 1; // usec
		int timeOutCounter = timeout_msec / sleepTime;
		int resBytes = 0;

		while ((resBytes = this.inputStream.available()) < numBytes) {
			Thread.sleep(sleepTime);
			timeOutCounter--;
			//if(log.isLoggable(Level.FINER)) log.finer("time out counter = " + counter);
			if (timeOutCounter <= 0) {
				TimeOutException e = new TimeOutException(Messages.getString(MessageIds.OSDE_MSGE0011, new Object[] { numBytes, timeout_msec }));
				log.log(Level.SEVERE, e.getMessage(), e);
				throw e;
			}
		}
		
		return resBytes;
	}
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
	public synchronized byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex) throws IOException, TimeOutException {
		int sleepTime = 4; // ms
		int expectedBytes = readBuffer.length;
		int readBytes = 0;
		int timeOutCounter = timeout_msec / sleepTime;
		if (stableIndex >= timeOutCounter) {
			log.severe(Messages.getString(MessageIds.OSDE_MSGE0013));
		}


		try {
			if (this.application != null) this.application.setSerialRxOn();
			expectedBytes = waitForStableReceiveBuffer(expectedBytes, timeout_msec, stableIndex);

			while (readBytes < expectedBytes && timeOutCounter-- > 0) {
				readBytes += this.inputStream.read(readBuffer, 0 + readBytes, expectedBytes - readBytes);

				if (expectedBytes != readBytes) {
					Thread.sleep(sleepTime);
				}
			}
			//this.dataAvailable = false;
			if (timeOutCounter <= 0) {
				TimeOutException e = new TimeOutException(Messages.getString(MessageIds.OSDE_MSGE0011, new Object[] { expectedBytes, timeout_msec }));
				log.log(Level.SEVERE, e.getMessage(), e);
				throw e;
			}
			
			// resize the data buffer to real red data 
			if (readBytes < readBuffer.length) { 
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
	 * waits until receive buffer is filled with number of expected bytes or does not change anymore in stableIndex cycles * 10 msec
	 * @param expectedBytes
	 * @param timeout_msec in milli seconds, this is the maximum time this process will wait for stable byte count or maxBytes
	 * @param stableIndex cycle count times 10 msec to be treat as stable
	 * @return number of bytes in receive buffer
	 * @throws InterruptedException 
	 * @throws TimeOutException 
	 * @throws IOException 
	 */
	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_msec, int stableIndex) throws InterruptedException, TimeOutException, IOException {
		int sleepTime = 1; // ms
		int timeOutCounter = timeout_msec / sleepTime;
		int stableCounter = stableIndex;
		boolean isStable = false;
		boolean isTimedOut = false;

		// availableBytes are updated by event handler
		int byteCounter = 0, numBytesAvailable = 0;
		while (byteCounter < expectedBytes && !isStable && !isTimedOut) {
			Thread.sleep(sleepTime);

			if (byteCounter == (numBytesAvailable = this.inputStream.available()))
				--stableCounter;
			else
				stableCounter = stableIndex;

			if (stableCounter == 0) isStable = true;

			byteCounter = numBytesAvailable;
			
			--timeOutCounter;

			if (log.isLoggable(Level.INFO)) {
				log.fine("stableCounter = " + stableCounter + " timeOutCounter = " + timeOutCounter);
			}
			if (timeOutCounter == 0) {
				TimeOutException e = new TimeOutException(Messages.getString(MessageIds.OSDE_MSGE0011, new Object[] { expectedBytes, timeout_msec })); 
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

	/**
	 * function to close the serial port
	 * this is done within a tread since the port can't close if it stays open for a long time period ??
	 */
	public synchronized void close() {
		if (this.isConnected && DeviceSerialPort.this.serialPort != null) {
			this.closeThread = new Thread() {
				public void run() {
					log.info("entry"); //$NON-NLS-1$
					try {
						Thread.sleep(10);
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
	 * @return the serialPortStr
	 */
	public String getSerialPortStr() {
		return this.serialPortStr == null ? this.deviceConfig.getPort() : this.serialPortStr;
	}

	/**
	 * @return number of transfer errors occur (checksum)
	 */
	public int getXferErrors() {
		return this.xferErrors;
	}
}
