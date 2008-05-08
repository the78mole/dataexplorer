/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.serial;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.config.Settings;
import osde.device.DeviceConfiguration;
import osde.exception.ApplicationConfigurationException;
import osde.exception.ReadWriteOutOfSyncException;
import osde.exception.TimeOutException;
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
	private boolean													isConnected				= false;
	private String													serialPortStr			= "";
	private Thread													closeThread;
	
	static CommPortIdentifier			portId;
	static CommPortIdentifier			saveportId;

//	public final String							PORT_OPEN							= "offen";
//	public final String							PORT_CLOSED						= "geschlossen";

	private InputStream						inputStream				= null;
	private boolean								isReadBufferEmpty	= true;
	private int										numBytesAvailable	= 0;
	private OutputStream					outputStream			= null;

	protected String							name;

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


	@SuppressWarnings("unchecked")
	public DeviceSerialPort(DeviceConfiguration currentDeviceConfig, OpenSerialDataExplorer currentApplication) {
		this.deviceConfig = currentDeviceConfig;
		this.application = currentApplication;
	}

	public static Vector<String> listConfiguredSerialPorts() {
		Vector<String> availablePorts = new Vector<String>(1, 1);
		String osname = System.getProperty("os.name", "").toLowerCase();

		if (osname.startsWith("windows")) {
			// windows
			availablePorts = getAvailablePorts(availablePorts, "COM", 1, 20); //COM1, COM2 -> COM20
		}
		else if (osname.startsWith("linux")) {
			// linux
			availablePorts = getAvailablePorts(availablePorts, "/dev/ttyS", 0, 20); // /dev/ttyS0, /dev/ttyS1 -> /dev/ttyS20
			availablePorts = getAvailablePorts(availablePorts, "/dev/ttyUSB", 0, 10); // /dev/ttyUSB0, /dev/ttyUSB1 -> /dev/ttyUSB10
		}
		else {
			log.severe("Error, your operating system is not supported");
			System.exit(-1);
		}

		availablePorts.trimToSize();
		return availablePorts;
	}

	/**
	 * find the serial ports using the given string prefix
	 * @param availablePorts
	 * @param serialPortPrefix
	 * @param startIndex
	 * @param searchCounter
	 */
	private static Vector<String> getAvailablePorts(Vector<String> availablePorts, String serialPortPrefix, int startIndex, int searchCounter) {
		String serialPortStr;
		int index = startIndex;
		CommPortIdentifier.getPortIdentifiers(); // initializes serial port
		// find all available serial ports
		for (; index < searchCounter; index++) {
			serialPortStr = serialPortPrefix + index;
			CommPortIdentifier tmpPortId;
			try {
				tmpPortId = CommPortIdentifier.getPortIdentifier(serialPortStr);
				if (tmpPortId.getPortType() == CommPortIdentifier.PORT_SERIAL && !tmpPortId.isCurrentlyOwned()) {
					try {
						((SerialPort) tmpPortId.open("OpenSerialDataExplorer", 2000)).close();
						availablePorts.add(serialPortStr);
						log.fine("Found port: " + serialPortStr);
					}
					catch (Exception e) {
						log.fine("Found port, but can't open: " + serialPortStr);
					}
				}
			}
			catch (NoSuchPortException e) {
				// ignore
			}
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
	
	public synchronized SerialPort open() throws Exception  {
		// Initialize serial port
		try {
			Settings settings = Settings.getInstance();
			this.serialPortStr = settings.isGlobalSerialPort() ? settings.getSerialPort() : this.deviceConfig.getPort();
			// check if a serial port is selected to be opened
			Vector<String> availableSerialPorts = listConfiguredSerialPorts();
			if (this.serialPortStr == null || this.serialPortStr.length() < 4 || !isMatchAvailablePorts(this.serialPortStr, availableSerialPorts)) {
				// no serial port is selected, if only one serial port is available choose this one
				if (availableSerialPorts.size() >= 1) {
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
					throw new ApplicationConfigurationException("Es ist kein serieller Port für das ausgewählte Gerät konfiguriert !");
				}
			}
			log.fine(String.format("serialPortString = %s; baudeRate = %d; dataBits = %d; stopBits = %d; parity = %d; flowControlMode = %d; RTS = %s; DTR = %s", this.serialPortStr, this.deviceConfig.getBaudeRate(), this.deviceConfig.getDataBits(), this.deviceConfig.getStopBits(), this.deviceConfig.getParity(), this.deviceConfig.getFlowCtrlMode(), this.deviceConfig.isRTS(), this.deviceConfig.isDTR()));
			
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

			this.setConnected(true);
			if (this.application != null) this.application.setPortConnected(true);
			return this.serialPort;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			if (this.serialPort != null) this.serialPort.close();
			throw e;
		}
	}

	public synchronized void write(byte[] buf) throws IOException {
		if (!this.isReadBufferEmpty) {
			int num = this.inputStream.available();
			String msg = " -> " + num + " left bytes -> " + this.inputStream.read(new byte[num]);
			this.isReadBufferEmpty = true;
			log.warning(msg);
			//throw new IOException("ERROR: read buffer is not empty, please get previous data before send new command !" + msg);
		}

		try {
			if (this.application != null) this.application.setSerialTxOn();

			if (log.isLoggable(Level.FINE)) {
				StringBuffer sb = new StringBuffer();
				sb.append("Write data: ");
				for (int i = 0; i < buf.length; i++) {
					sb.append(String.format("%02X ", buf[i]));
				}
				sb.append(" to port ").append(this.serialPort.getName()).append(System.getProperty("line.separator"));
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

	public void serialEvent(SerialPortEvent event) {

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
			break;
		case SerialPortEvent.DATA_AVAILABLE:
			// we get here if data has been received
			try {
				this.numBytesAvailable = this.inputStream.available();
				if (this.numBytesAvailable > 0) this.isReadBufferEmpty = false;
				if (log.isLoggable(Level.FINER)) {
					log.finer("inputStream numBytesAvailable = " + this.numBytesAvailable);
				}
			}
			catch (IOException e) {
			}
			break;
		}
	}

	public synchronized byte[] read(int bytes, int timeoutInSeconds) throws IOException {

		byte[] readBuffer = new byte[bytes];
		int readBytes = 0;
		int retryCounter = 5;

		try {
			if (this.application != null) this.application.setSerialRxOn();

			wait4Bytes(bytes, timeoutInSeconds);

			while (bytes != readBytes && retryCounter-- > 0){
				readBytes += this.inputStream.read(readBuffer, 0 + readBytes, bytes - readBytes);
			}
			
			this.isReadBufferEmpty = true;
			this.numBytesAvailable = 0;

			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				sb.append("Read  data: ");
				for (int i = 0; i < readBuffer.length; i++) {
					sb.append(String.format("%02X ", readBuffer[i]));
				}
				log.fine(sb.toString());
			}

			if (this.application != null) this.application.setSerialRxOff();
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		return readBuffer;
	}

	/**
	 * waits until receive buffer is filled with the number of expected bytes
	 * @param numBytes
	 * @param timeoutInSeconds
	 * @param isTerminatedByChecksum
	 * @return number of bytes in receive buffer
	 * @throws IOException
	 */
	private int wait4Bytes(int numBytes, int timeoutInSeconds) throws IOException {
		int counter = timeoutInSeconds * 1000;
		int resBytes = 0;
		try {
			// wait until readbuffer has been filled by eventListener
			while (this.numBytesAvailable < numBytes) {
				Thread.sleep(0, 1);
				counter--;
				//if(log.isLoggable(Level.FINER)) log.finer("time out counter = " + counter);
				if (counter <= 0) {
					this.close();
					IOException e = new IOException("Error: can not read result during given timeout !");
					log.log(Level.SEVERE, e.getMessage(), e);
					throw e;
				}
			}
			//if(log.isLoggable(Level.FINER)) log.finer("inputStream numBytesAvailable = " + (resBytes = this.numBytesAvailable));
		}
		catch (InterruptedException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return resBytes;
	}

	/**
	 * waits until receive buffer is filled with number of expected bytes or does not change anymore in 10 cycles
	 * @param expectedBytes
	 * @param timeout_sec in seconds, this is the maximum time this process will wait for stable byte count or maxBytes
	 * @return number of bytes in receive buffer
	 * @throws InterruptedException 
	 * @throws TimeOutException 
	 */
	public int waitForStabelReceiveBuffer(int expectedBytes, int timeout_sec) throws InterruptedException, TimeOutException {

		int timeCounter = timeout_sec * 1000;
		int stableCounter = 50;
		boolean isStable = false;
		boolean isTimedOut = false;

		// availableBytes are updated by event handler
		int byteCounter = this.numBytesAvailable;
		Thread.sleep(15);
		while (byteCounter < expectedBytes && !isStable && !isTimedOut) {
			Thread.sleep(2); // 2 ms

			if (byteCounter == this.numBytesAvailable)
				--stableCounter;
			else
				stableCounter = 50;

			if (stableCounter == 0) isStable = true;

			byteCounter = this.numBytesAvailable;
			--timeCounter;

//			if (log.isLoggable(Level.FINER)) {
//				log.finer("stableCounter = " + stableCounter);
//				log.finer(" timeCounter = " + timeCounter);
//			}
			if (timeCounter == 0) {
				this.close();
				TimeOutException e = new TimeOutException("can not receive data in given time");
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
		if (log.isLoggable(Level.FINER)) log.finer("inputStream available bytes = " + this.inputStream.available());
		if (this.inputStream.available() != 0) throw new ReadWriteOutOfSyncException("receive buffer not empty");
	}

	/**
	* function clear/reset receive buffer
	*/
	public void resetReceiveBuffer() {
		this.isReadBufferEmpty = true;
		this.setNumBytesAvailable(0);
	}

	public synchronized void close() {
		log.info("entry");
		this.closeThread = new Thread() { 
			public void run() { 
				log.info("entry");
				if (isConnected() && DeviceSerialPort.this.serialPort != null) {
					try {
						Thread.sleep(2);
						byte[] buf = new byte[getInputStream().available()];
						if (buf.length > 0) getInputStream().read(buf);
					}
					catch (Exception e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
					log.info("before close");
					DeviceSerialPort.this.serialPort.close();
					log.info("after close");
					setConnected(false);
					if (DeviceSerialPort.this.application != null) DeviceSerialPort.this.application.setPortConnected(false);
					log.info("exit");
				}
			}
		};
		this.closeThread.start();
		log.info("exit, close thread started");
	}

	public InputStream getInputStream() {
		return this.inputStream;
	}

	public OutputStream getOutputStream() {
		return this.outputStream;
	}

	public synchronized void setNumBytesAvailable(int newNumBytesAvailable) {
		this.numBytesAvailable = newNumBytesAvailable;
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
			System.out.println(key + " = " + map.get(key));
		}
	}

	/**
	 * sample method to gather data from device, implementation is individual for device
	 * @return map containing gathered data - this can individual specified per device
	 * @throws Exception
	 */
	//public HashMap<String, Object> getData(byte[] channelSignature) throws Exception;
	//public HashMap<String, Object> getData(int datagramNumber, Picolario device, String configurationKey) throws Exception {
	//public HashMap<String, Object> getData(byte[] channel, int recordNumber, String channelConfigKey) throws Exception {
//	public HashMap<String, Object> getData(IDevice device) {
//		HashMap<String, Object> dataMap = new HashMap<String, Object>();
//		dataMap.put(device.getChannelName(1), new Object());
//		return dataMap;
//	}

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
	 * @param enabled the isConnected to set
	 */
	public void setConnected(boolean enabled) {
		this.isConnected = enabled;
	}
}
