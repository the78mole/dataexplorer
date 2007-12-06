package osde.device.renschler;

import gnu.io.NoSuchPortException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.config.DeviceConfiguration;
import osde.device.DeviceDialog;
import osde.device.DeviceSerialPort;
import osde.exception.ReadWriteOutOfSyncException;
import osde.exception.TimeOutException;
import osde.ui.StatusBar;

/**
 * this class is used to communicate with the Renschler Picolario device
 */
public class PicolarioSerialPort extends DeviceSerialPort {
	private Logger							log											= Logger.getLogger(this.getClass().getName());

	private StatusBar						statusBar;

	// Datentyp Kommando Beschreibung
	private final byte					readNumberRecordSets[]	= new byte[] { (byte) 0xAA, (byte) 0xAA };
	private final byte					readRecordSets[]				= new byte[] { (byte) 0xAA, (byte) 0x00 };

	public final static String	HEIGHT									= "Höhe";
	public final static String	VOLTAGE									= "Spannung";

	public PicolarioSerialPort(DeviceConfiguration deviceConfig, StatusBar statusBar) throws NoSuchPortException {
		super(deviceConfig, statusBar);
		this.statusBar = statusBar;
	}

	/**
	 * ask the Picolario about the number of available record sets
	 * @return number of record sets available
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public synchronized int readNumberAvailableRecordSets() throws IOException, InterruptedException {
		int recordSets = 0;

		this.write(readNumberRecordSets);
		Thread.sleep(30);
		this.write(readNumberRecordSets);

		byte[] answer = this.read(4, 2);

		if (answer[0] != readNumberRecordSets[0] && answer[2] != readNumberRecordSets[0])
			throw new IOException("command to answer missmatch");
		else
			recordSets = (int) (answer[1] & 0xFF);

		log.fine("number available record sets = " + recordSets);
		return recordSets;
	}

	/**
	 * method to receive full set of Picolario data
	 * @param datagramNumber
	 * @return
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public synchronized HashMap<String, Object> getData(byte[] channel, int datagramNumber, DeviceDialog dialog) throws IOException {
		Vector<Integer> height = new Vector<Integer>(100);
		Vector<Integer> voltage = new Vector<Integer>(100);
		HashMap<String, Object> data = new HashMap<String, Object>();
		boolean isTransmitFinished = false;
		boolean isGoodPackage = true;
		byte[] readBuffer;
		int numberRed = 0;
		byte[] readRecordSetsWithNumber = new byte[] { readRecordSets[0], (byte) datagramNumber, readRecordSets[0], (byte) datagramNumber };
		write(readRecordSetsWithNumber);
		//		Thread.sleep(30);
		//		this.write(readRecordSetsWithNumber);

		try {
			Thread.sleep(20); // wait for 20 ms since it makes no sense to check receive buffer earlier

			while (!isTransmitFinished) {

				int numberAvailableBytes = waitForStabelReceiveBuffer(31, 1);

				if (numberAvailableBytes > 0) {
					readBuffer = read(numberAvailableBytes, 1); // throws timeout exception

					checkForLeftBytes(); // on receive buffer -> wait for stable bytes failed

					resetReceiveBuffer();
					isGoodPackage = checkGoodPackage(readBuffer, 31, 3);

					if (isGoodPackage) {
						// append data to data container
						for (int i = 0; i < readBuffer.length - 1; i = i + 3) {
							// calculate height values and add
							if (((readBuffer[i + 1] & 0x80) >> 7) == 0) // we have signed [feet]
								height.add(((readBuffer[i] & 0xFF) + ((readBuffer[i + 1] & 0x7F) << 8)) * 1000); // only positive part of height data
							else
								height.add((((readBuffer[i] & 0xFF) + ((readBuffer[i + 1] & 0x7F) << 8)) * -1) * 1000);// height is negative

							// add voltage U = 2.5 + (byte3 - 45) * 0.0532 - no calculation take place here
							voltage.add(new Integer(readBuffer[i + 2]) * 1000);
						}
						//acknowledge request next
						this.write(new byte[] { readBuffer[readBuffer.length - 1], readBuffer[readBuffer.length - 1] });
						// update the dialog
						if (dialog != null) ((PicolarioDialog)dialog).setAlreadyRedText(numberRed++);
					}
					else {
						// write wrong checksum to repeat data package receive cycle
						System.err.println("write wrong checksum required");
						//this.write(readNumberRecordSets); // use 0xAA, 0xAA as wrong checksum
					}
				}
				else {
					isTransmitFinished = checkTransmissionFinished(31, 1);
				}
			} // end while receive loop

		}
		catch (TimeOutException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (ReadWriteOutOfSyncException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (InterruptedException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}

		data.put(HEIGHT, height);
		data.put(VOLTAGE, voltage);
		if (statusBar != null) {
			statusBar.setSerialRxOff();
			statusBar.setSerialTxOff();
		}
		return data;
	}

	/**
	 * function check transmission finished, set isTransmitFinished 
	 * @throws TimeOutException 
	 * @throws InterruptedException 
	 */
	private boolean checkTransmissionFinished(int size, int timeout_sec) throws InterruptedException, TimeOutException {
		boolean isTransMitFinished = false;
		if (0 == waitForStabelReceiveBuffer(size, timeout_sec)) isTransMitFinished = true;
		//error in transmittion
		//transmission canceled
		resetReceiveBuffer();
		return isTransMitFinished;
	}

	/**
	 * function check good package, set isGoodPackage throws read/write out of sync exception
	 * @throws ReadWriteOutOfSyncException 
	 */
	private boolean checkGoodPackage(byte[] readBuffer, int maxBytes, int modulo) throws ReadWriteOutOfSyncException {
		boolean isGoodPackage = false;
		if (readBuffer.length == maxBytes)
			isGoodPackage = true;
		else if (readBuffer.length < maxBytes && (readBuffer.length - 1) % modulo == 0) isGoodPackage = true;
		//		else
		//			isGoodPackage = verifyChecksum(readBuffer);

		return isGoodPackage;
	}

	@SuppressWarnings("unused")
	private boolean verifyChecksum(final byte[] readBuffer) {
		//TODO need to implement algorithm here, wait for answer Uwe Renschler
		return true;
	}

	public int calculateSimpleCecksum(byte[] readBuffer) {
		int checksum = 0;

		//		for (int i = 0; i < readBuffer.length - 1; i++) {
		//			checksum = checksum + readBuffer[i]; // 1250 -> 8

		//						checksum = checksum + (readBuffer[i] - (readBuffer[i] % 10)) / 10; // 260 -> 8
		//						checksum = checksum + (readBuffer[i] % 10);

		//						checksum = checksum + ((readBuffer[i] & 0xF0) >> 4); // 350 -> 8
		//						checksum = checksum + ((readBuffer[i] & 0x0F) >> 0);

		//			checksum = checksum + ((readBuffer[i] & 0xF0) / 10); // 380 -> 11 -> 2
		//			checksum = checksum + ((readBuffer[i] & 0x0F) >> 0);
		//		}

		return checksum;
	}

}
