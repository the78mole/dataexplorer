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
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.TooManyListenersException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.config.Settings;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.exception.ReadWriteOutOfSyncException;
import osde.exception.TimeOutException;
import osde.ui.OpenSerialDataExplorer;

/**
 * DeviceSerialPort is the abstract class of the serial port implementation as parent for a device specific serial port implementation
 * @author Winfried Brügmann
 */
public abstract class DeviceSerialPort implements SerialPortEventListener {
	private static Logger					log								= Logger.getLogger(DeviceSerialPort.class.getName());

	protected final DeviceConfiguration			deviceConfig;
	protected final OpenSerialDataExplorer 	application;
	protected SerialPort										serialPort 				= null;
	protected boolean												isConnected				= false;
	private String													serialPortStr			= "";
	
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


	private static String					newLine						= System.getProperty("line.separator");
	
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
	public DeviceSerialPort(DeviceConfiguration deviceConfig, OpenSerialDataExplorer application) throws NoSuchPortException {
		this.deviceConfig = deviceConfig;
		this.application = application;
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
		CommPortIdentifier.getPortIdentifiers(); // initializes serial port
		// find all available serial ports
		for (; startIndex < searchCounter; startIndex++) {
			serialPortStr = serialPortPrefix + startIndex;
			CommPortIdentifier portId;
			try {
				portId = CommPortIdentifier.getPortIdentifier(serialPortStr);
				if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					availablePorts.add(serialPortStr);
					log.fine("Found port: " + serialPortStr);
				}
			}
			catch (NoSuchPortException e) {
				// ignore
			}
		}
		return availablePorts;
	}

	public synchronized SerialPort open() throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, TooManyListenersException, IOException  {
		// Initialize serial port
		try {
			Settings settings = Settings.getInstance();
			serialPortStr = settings.isGlobalSerialPort() ? settings.getSerialPort() : deviceConfig.getPort();
			log.fine(String.format("serialPortString = %s; baudeRate = %d; dataBits = %d; stopBits = %d; parity = %d; flowControlMode = %d; RTS = %s; DTR = %s", serialPortStr, deviceConfig.getBaudeRate(), deviceConfig.getDataBits(), deviceConfig.getStopBits(), deviceConfig.getParity(), deviceConfig.getFlowCtrlMode(), deviceConfig.isRTS(), deviceConfig.isDTR()));
			portId = CommPortIdentifier.getPortIdentifier(serialPortStr);
			serialPort = (SerialPort) portId.open("OpenSerialDataExplorer", 2000);
			// set port parameters
			serialPort.setInputBufferSize(4096);
			serialPort.setOutputBufferSize(4096);
			serialPort.setSerialPortParams(deviceConfig.getBaudeRate(), deviceConfig.getDataBits(), deviceConfig.getStopBits(), deviceConfig.getParity());
			serialPort.setFlowControlMode(deviceConfig.getFlowCtrlMode());
			serialPort.setRTS(deviceConfig.isRTS());
			serialPort.setDTR(deviceConfig.isDTR());

			serialPort.addEventListener(this);
			// activate the DATA_AVAILABLE notifier to read available data
			serialPort.notifyOnDataAvailable(true);

			// init in and out stream for writing and reading
			inputStream = serialPort.getInputStream();
			outputStream = serialPort.getOutputStream();

			this.isConnected = true;
			if (application != null) application.setPortConnected(true);
			return serialPort;
		}
		catch (PortInUseException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			if (serialPort != null) serialPort.close();
			throw e;
		}
		catch (UnsupportedCommOperationException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			if (serialPort != null) serialPort.close();
			throw e;
		}
		catch (TooManyListenersException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			if (serialPort != null) serialPort.close();
			throw e;
		}
	}

	public synchronized void write(byte[] buf) throws IOException {
		if (!isReadBufferEmpty) {
			int num = inputStream.available();
			String msg = " -> " + num + " left bytes -> " + inputStream.read(new byte[num]);
			isReadBufferEmpty = true;
			log.warning(msg);
			//throw new IOException("ERROR: read buffer is not empty, please get previous data before send new command !" + msg);
		}

		try {
			if (application != null) application.setSerialTxOn();

			if (log.isLoggable(Level.FINE)) {
				StringBuffer sb = new StringBuffer();
				sb.append("Write data: ");
				for (int i = 0; i < buf.length; i++) {
					sb.append(String.format("%02X ", buf[i]));
				}
				sb.append(" to port ").append(serialPort.getName()).append(newLine);
				log.fine(sb.toString());
			}

			// write string to serial port
			outputStream.write(buf);
			outputStream.flush();
			if (application != null) application.setSerialTxOff();
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
				this.numBytesAvailable = inputStream.available();
				if (this.numBytesAvailable > 0) isReadBufferEmpty = false;
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
			if (application != null) application.setSerialRxOn();

			wait4Bytes(bytes, timeoutInSeconds);

			while (bytes != (readBytes += inputStream.read(readBuffer, 0 + readBytes, bytes - readBytes)) && retryCounter-- > 0)
				;
			isReadBufferEmpty = true;
			this.setNumBytesAvailable(0); 

			if (readBytes != bytes) {
				throw new IOException("Warning: missed expected number of bytes to be read");
			}

			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				sb.append("Read  data: ");
				for (int i = 0; i < readBuffer.length; i++) {
					sb.append(String.format("%02X ", readBuffer[i]));
				}
				log.fine(sb.toString());
			}

			if (application != null) application.setSerialRxOff();
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
			Thread.sleep(1);
			while (this.numBytesAvailable < numBytes) {
				Thread.sleep(1);
				counter--;
				if(log.isLoggable(Level.FINER)) log.finer("time out counter = " + counter);
				if (counter <= 0) throw new IOException("Error: can not read result during given timeout !");
			}
			if(log.isLoggable(Level.FINER)) log.finer("inputStream numBytesAvailable = " + (resBytes = this.numBytesAvailable));
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

		int timeCounter = timeout_sec * 1000/2; // 2 msec per time interval
		int stableCounter = 20;
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
				stableCounter = 20;

			if (stableCounter == 0) isStable = true;

			byteCounter = this.numBytesAvailable;
			--timeCounter;

			if (log.isLoggable(Level.FINER)) {
				log.finer("stableCounter = " + stableCounter);
				log.finer(" timeCounter = " + timeCounter);
			}
			if (timeCounter == 0) throw new TimeOutException("can not receive data in given time");

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
		if (log.isLoggable(Level.FINER)) log.finer("inputStream available bytes = " + inputStream.available());
		if (inputStream.available() != 0) throw new ReadWriteOutOfSyncException("receive buffer not empty");
	}

	/**
	* function clear/reset receive buffer
	*/
	public void resetReceiveBuffer() {
		isReadBufferEmpty = true;
		this.setNumBytesAvailable(0);
	}

	public synchronized void close() {
		if (isConnected && serialPort != null) {
			try {
				byte[] buf = new byte[inputStream.available()];
				if (buf.length > 0) inputStream.read(buf);
			}
			catch (IOException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
			serialPort.close();
			isConnected = false;
			if (application != null) application.setPortConnected(false);
		}
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public synchronized void setNumBytesAvailable(int numBytesAvailable) {
		this.numBytesAvailable = numBytesAvailable;
	}

	public boolean isConnected() {
		return isConnected;
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
	 * method to gather data from device, implementation is individual for device
	 * @param channel signature if device has more than one or required by device
	 * @return map containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public abstract  HashMap<String, Object> getData(byte[] channel, int recordNumber, IDevice dialog, String channelConfigKey) throws IOException;

	/**
	 * @return the serialPortStr
	 */
	public String getSerialPortStr() {
		return serialPortStr == null ? deviceConfig.getPort() : this.serialPortStr;
	}

	/**
	 * @param serialPortStr the serialPortStr to set
	 */
	public void setSerialPortStr(String serialPortStr) {
		this.serialPortStr = serialPortStr;
	}
}
