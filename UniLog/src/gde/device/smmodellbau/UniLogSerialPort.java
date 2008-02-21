package osde.device.smmodellbau;

import gnu.io.NoSuchPortException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.device.DeviceConfiguration;
import osde.device.DeviceSerialPort;
import osde.device.IDevice;
import osde.exception.CheckSumMissmatchException;
import osde.ui.StatusBar;
import osde.utils.Checksum;

/**
 * UniLog serial port implementation class, just copied from Sample projectS
 * @author Winfried Brügmann
 */
public class UniLogSerialPort extends DeviceSerialPort {
	private static Logger					log											= Logger.getLogger(UniLogSerialPort.class.getName());

	public final static String 		NUMBER_RECORD						= "number_record";
	public final static String 		TIME_MILLI_SEC					= "time_ms";
	public final static String 		A_MODUS_1_2_3						= "aModus_1_2_3";
	
	private final static byte[]		COMMAND_QUERY_STATE			= { 0x54 };		// 'T' query UniLog state
	private final static byte[]		COMMAND_RESET						= { 0x72 };		// 'r' reset UniLog to repeat data send (from the begin)
	private final static byte[]		COMMAND_READ_DATA				= { 0x6C };		// 'l' UniLog read request, answer is one data set (telegram)
	private final static byte[]		COMMAND_REPEAT					= { 0x77 };		// 'w' repeat data transmission, UniLog re-send same data set (telegram)
	private final static byte[]		COMMAND_DELETE					= { (byte) 0xC0, 0x03, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06 };
	private final static byte[]		COMMAND_QUERY_CONFIG		= { (byte) 0xC0, 0x03, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04 };
	private final static byte[]		COMMAND_LIVE_VALUES			= { 0x76 };		// 'v' query UniLog live values
	private final static byte[]		COMMAND_START_LOGGING		= { 0x53 };		// 'S' start logging data
	private final static byte[]		COMMAND_STOP_LOGGING		= { 0x73 };		// 's' stop logging data

	@SuppressWarnings("unused")
	private final static byte[]		COMMAND_PREPARE_DELETE			= { 0x78, 0x79, 0x31 };					// "xy1"
	@SuppressWarnings("unused")
	public final static byte[]		COMMAND_PREPARE_SET_CONFIG	= { 0x78, 0x79, (byte) 0xA7 };	// "xyz"
	
	private final static byte			DATA_STATE_WAITING			= 0x57;		// 'W' UniLog connected, needs some time to organize flash
	private final static byte			DATA_STATE_READY				= 0x46;		// 'F' UniLog ready to receive command
	private final static byte			DATA_STATE_OK						= 0x6A;		// 'j' operation successful ended

	public final static int				DATA_LENGTH_BYTES				= 24;			//TODO exchange with deviceConfig.get()
	
	private boolean 							isLoggingActive 				= false;
	private boolean 							isTransmitFinished			= false;
	
	private int 									reveiceErrors 					= 0;

	/**
	 * constructor of default implementation
	 * @param deviceConfig - required by super class to initialize the serial communication port
	 * @param statusBar - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 * @throws NoSuchPortException
	 */
	public UniLogSerialPort(DeviceConfiguration deviceConfig, StatusBar statusBar) throws NoSuchPortException {
		super(deviceConfig, statusBar);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @param channel signature if device has more than one or required by device
	 * @return map containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized HashMap<String, Object> getData(byte[] channel, int recordNumber, IDevice device, String channelConfigKey) throws IOException {
		
		HashMap<String, Object> dataCollection = new HashMap<String, Object>();
		
		UniLogDialog dialog = (UniLogDialog)device.getDialog();
		byte[] readBuffer = new byte[DATA_LENGTH_BYTES];
		
		try {
			this.open();
			reveiceErrors = 0;

			// check data ready for read operation
			if (this.waitDataReady()) {
				// query config to have actual values -> get number of entries to calculate percentage and progress bar
				this.write(COMMAND_QUERY_CONFIG);
				readBuffer = this.read(DATA_LENGTH_BYTES, 3);
				verifyChecksum(readBuffer);
				int memoryUsed = ((readBuffer[6] & 0xFF) << 8) + (readBuffer[7] & 0xFF);
				log.fine("memoryUsed = " + memoryUsed);
				double progressFactor = 100.0 / memoryUsed;
				log.fine("progressFactor = " + progressFactor);

				// reset data and prepare for read
				this.write(COMMAND_RESET);

				if (application != null) dialog.setReadDataProgressBar(0);
				Vector<byte[]> telegrams = new Vector<byte[]>();
				int numberRecordSet = 1;
				int counter = 0;
				
				while (!isTransmitFinished && memoryUsed-- > 0) {
					readBuffer = readSingleTelegramm();
											
					// number record set
					if(numberRecordSet == ((readBuffer[5] & 0xF8) / 8 + 1)) {
						telegrams.add(readBuffer);
					}
					else {
						dataCollection.put(""+numberRecordSet, telegrams);
						++numberRecordSet;
						telegrams = new Vector<byte[]>();
					}

					++counter;
					
					if (application != null) {
						dialog.setReadDataProgressBar(new Double(counter * progressFactor).intValue());
						dialog.updateDataGatherProgress(counter, reveiceErrors);
					}
				}
				dataCollection.put(""+numberRecordSet, telegrams);
			}
			else
				throw new IOException("Gerät ist nicht angeschlossen oder nicht bereit.");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			//throw e;
		}
		finally {
			if(this.isConnected) this.close();
			if (statusBar != null) {
				statusBar.setSerialRxOff();
				statusBar.setSerialTxOff();
			}
		}
		return dataCollection;
	}

	/**
	 * read a single telegram to enable live view of measurements
	 * @return raw byte array of received data
	 * @throws Exception
	 */
	public synchronized byte[] readSingleTelegramm() throws Exception {
		byte[] readBuffer = new byte[DATA_LENGTH_BYTES];
		
		try {
			this.write(COMMAND_READ_DATA);
			readBuffer = this.read(DATA_LENGTH_BYTES, 2);
			
			// give it another try
			if (!isChecksumOK(readBuffer)) {
				++reveiceErrors;
				this.write(COMMAND_REPEAT);
				readBuffer = this.read(DATA_LENGTH_BYTES, 2);
				verifyChecksum(readBuffer); // throws exception if checksum miss match
			}
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		
		return readBuffer;
	}
	
	/**
	 * enable live data view, timer loop must gather the data which also handles open/close operations
	 * @return byte array with red data
	 * @throws Exception
	 */
	public synchronized byte[] queryLiveData() throws Exception {
		byte[] readBuffer = new byte[DATA_LENGTH_BYTES];
		try {
			if (this.isConnected) {
				this.write(COMMAND_LIVE_VALUES);
				readBuffer = this.read(DATA_LENGTH_BYTES, 2);
				
				// give it another try
				if (!isChecksumOK(readBuffer)) {
					this.write(COMMAND_REPEAT);
					readBuffer = this.read(DATA_LENGTH_BYTES, 2);
					verifyChecksum(readBuffer); // throws exception if checksum miss match
				}
			}
			else
				throw new Exception("Der serialle Port ist nicht geöffnet!");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.close();
			throw e;
		}
		return readBuffer;
	}
	
	/**
	 * start logging of UniLog, open/close port operations must be handled outside
	 * @return true if logging is enabled
	 * @throws Exception
	 */
	public synchronized boolean startLogging() throws Exception {
		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected) {
				this.open();
				isPortOpenedByMe = true;
				//waitDataReady();
			}

			this.write(COMMAND_START_LOGGING);
			isLoggingActive = true;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.close();
			throw e;
		}
		finally {
			if (isPortOpenedByMe) this.close();
		}
		return isLoggingActive;
	}
	
	/**
	 * stop logging activity of UniLog, open/close port operations must be handled outside
	 * @return true if logging is disabled
	 * @throws Exception
	 */
	public synchronized boolean stopLogging() throws Exception {
		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected) {
				this.open();
				isPortOpenedByMe = true;
				//waitDataReady();
			}

			this.write(COMMAND_STOP_LOGGING);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.close();
			throw e;
		}
		finally {
			if (isPortOpenedByMe) this.close();
		}
		return !isLoggingActive;
	}
	
	/**
	 * clears the flash memory of UniLog
	 * @return true for successful operation
	 * @throws Exception
	 */
	public synchronized boolean clearMemory() throws Exception {
		boolean success = false;
		try {
			if (!this.isConnected) { // port may not used by other
				this.open();
				// check data ready for read operation
				if (this.waitDataReady()) {
					this.write(COMMAND_PREPARE_DELETE);
					this.write(COMMAND_DELETE);
					byte[] readBuffer = this.read(1, 2);
					if (readBuffer[0] != DATA_STATE_OK) success = true;

				}
				else
					throw new IOException("Gerät ist nicht angeschlossen oder nicht bereit.");
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			this.close();
		}
		return success;
	}
	/**
	 * set UniLog configuration with new values
	 * @param updateBuffer, byte array to be written
	 * @return true | false for state of the operation
	 * @throws Exception
	 */
	public synchronized boolean setConfiguration(byte[] updateBuffer) throws Exception {
		boolean success = false;
		try {
			this.open();
			// check device connected
			if (this.checkConnectionStatus()) {
				// check data ready for read operation
				if (this.checkDataReady()) {
					//this.write(COMMAND_PREPARE_SET_CONFIG);

					this.write(updateBuffer);
					byte[] readBuffer = this.read(1, 2);
					if (readBuffer[0] == DATA_STATE_OK) success = true;
					
				}
				else
					throw new IOException("Daten im Gerät sind nicht bereit zum Abholen.");
			}
			else
				throw new IOException("Gerät ist nicht angeschlossen oder nicht bereit.");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			this.close();
		}
		return success;
	}
	
	/**
	 * query the configuration information from UniLog
	 * @return byte array containing the configuration information 
	 * @throws Exception
	 */
	public synchronized byte[] readConfiguration() throws Exception {
		byte[] readBuffer = new byte[DATA_LENGTH_BYTES];
		boolean isPortOpenedByMe = false;
		try {
			if(!this.isConnected) {
				this.open();
				isPortOpenedByMe = true;
			}

			// check device connected
			if (this.checkConnectionStatus()) {
				// check data ready for read operation
				if (this.checkDataReady()) {

					this.write(COMMAND_QUERY_CONFIG);
					readBuffer = this.read(DATA_LENGTH_BYTES, 5);

					verifyChecksum(readBuffer); // valid data set -> set values
					
				}
				else
					throw new IOException("Daten im Gerät sind nicht bereit zum Abholen.");
			}
			else
				throw new IOException("Gerät ist nicht angeschlossen oder nicht bereit.");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			if(isPortOpenedByMe) this.close();
		}
		return readBuffer;
	}

	/**
	 * query if UniLog is connected and capable for communication
	 * @return true/false
	 * @throws IOException
	 */
	public synchronized boolean checkConnectionStatus() throws IOException {
		boolean isConnect = false;
		int counter = 10;

		while (!isConnect && counter-- > 0) {
			this.write(COMMAND_QUERY_STATE);
			byte[] buffer = this.read(1, 1);
			if (buffer[0] == DATA_STATE_WAITING || buffer[0] == DATA_STATE_READY) {
				isConnect = true;
			}
		}
		return isConnect;
	}

	/**
	 * check if UniLog is capable to send data
	 * @return
	 * @throws Exception
	 */
	public synchronized boolean checkDataReady() throws Exception {
		boolean isReady = false;
		int counter = 10;

		while (!isReady && counter-- > 0) {
			this.write(COMMAND_QUERY_STATE);
			byte[] buffer = this.read(1, 1);
			if (buffer[0] == DATA_STATE_READY) {
				isReady = true;
			}
		}
		
		return isReady;
	}

	/**
	 * loop while writing status request until data state signaled as ready
	 * @param timeout in seconds
	 * @return true if UniLog signals data ready for transmission
	 * @throws Exception
	 */
	public synchronized boolean waitDataReady() throws Exception {
		boolean isReady = false;
		int retryCount = 10;
		
		while (!isReady && retryCount-- > 0) {
			this.write(COMMAND_QUERY_STATE); 
			byte[] buffer = this.read(1, 1);
			if (buffer[0] == DATA_STATE_READY) { 
				isReady = true;
			}
		}
		return isReady;
	}

	/**
	 * verify the check sum
	 * @param readBuffer
	 * @throws CheckSumMissmatchException 
	 */
	private void verifyChecksum(byte[] readBuffer) throws CheckSumMissmatchException {
		int checkSum = 0;
		int checkSumLast2Bytes = 0;
		checkSum = Checksum.ADD(readBuffer, 2) + 1;
		log.finer("checkSum = " + checkSum);
		checkSumLast2Bytes = ((readBuffer[DATA_LENGTH_BYTES - 2] & 0xFF) << 8) + (readBuffer[DATA_LENGTH_BYTES - 1] & 0xFF);
		log.finer("checkSumLast2Bytes = " + checkSumLast2Bytes);
		
		if (checkSum != checkSumLast2Bytes)
			throw new CheckSumMissmatchException("Die Checksumme ist fehlerhaft - " + checkSum + " / " + checkSumLast2Bytes);
	}
	
	/**
	 * verify the check sum
	 * @param readBuffer
	 * @return true for checksum match
	 * @throws CheckSumMissmatchException 
	 */
	private boolean isChecksumOK(byte[] readBuffer) throws CheckSumMissmatchException {
		int checkSum = 0;
		int checkSumLast2Bytes = 0;
		checkSum = Checksum.ADD(readBuffer, 2) + 1;
		log.finer("checkSum = " + checkSum);
		checkSumLast2Bytes = ((readBuffer[DATA_LENGTH_BYTES - 2] & 0xFF) << 8) + (readBuffer[DATA_LENGTH_BYTES - 1] & 0xFF);
		log.finer("checkSumLast2Bytes = " + checkSumLast2Bytes);

		return (checkSum == checkSumLast2Bytes);
	}

	/**
	 * @param isTransmitFinished the isTransmitFinished to set
	 */
	public void setTransmitFinished(boolean isTransmitFinished) {
		this.isTransmitFinished = isTransmitFinished;
	}
}
