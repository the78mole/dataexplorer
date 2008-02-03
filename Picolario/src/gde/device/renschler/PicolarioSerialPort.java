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
package osde.device.renschler;

import gnu.io.NoSuchPortException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.device.DeviceSerialPort;
import osde.exception.ReadWriteOutOfSyncException;
import osde.exception.TimeOutException;
import osde.ui.StatusBar;
import osde.utils.Checksum;

/**
 * Serial communication implementation class for the Renschler Picolariolog device
 * @author Winfried Brügmann
 */
public class PicolarioSerialPort extends DeviceSerialPort {
	private Logger							log											= Logger.getLogger(this.getClass().getName());

	private boolean 						isTransmitFinished = false;

	// Datentyp Kommando Beschreibung
	private final byte					readNumberRecordSets[]	= new byte[] { (byte) 0xAA, (byte) 0xAA };
	private final byte					readRecordSets[]				= new byte[] { (byte) 0xAA, (byte) 0x00 };

	/**
	 * PicolarioSerialPort constructor
	 * @param deviceConfig
	 * @param statusBar
	 * @throws NoSuchPortException
	 */
	public PicolarioSerialPort(DeviceConfiguration deviceConfig, StatusBar statusBar) throws NoSuchPortException {
		super(deviceConfig, statusBar);
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
	 * @return hash map contining gathered data points (voltage and height in separate vector)
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public synchronized HashMap<String, Object> getData(byte[] channel, int datagramNumber, IDevice device) throws IOException {
		Vector<Integer> height = new Vector<Integer>(100);
		Vector<Integer> voltage = new Vector<Integer>(100);
		HashMap<String, Object> data = new HashMap<String, Object>();
		boolean isGoodPackage = true;
		byte[] readBuffer;
		int numberRed = 0;
		byte[] readRecordSetsWithNumber = new byte[] { readRecordSets[0], (byte) datagramNumber, readRecordSets[0], (byte) datagramNumber };
		write(readRecordSetsWithNumber);
		isTransmitFinished = false;

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
						if (device != null) ((PicolarioDialog)device.getDialog()).setAlreadyRedText(numberRed++);
					}
					else {
						// write wrong checksum to repeat data package receive cycle
						log.warning("write wrong checksum required");
						byte wrongChecksum = readBuffer[readBuffer.length - 1];
						byte[] requestAgain = new byte[] {wrongChecksum, wrongChecksum};
						this.write(requestAgain);
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

		String[] measurements = device.getMeasurementNames(); // 0=Spannung, 1=Höhe, 2=Steigrate
		data.put(measurements[0], voltage);
		data.put(measurements[1], height);
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
		else isGoodPackage = verifyChecksum(readBuffer);

		return isGoodPackage;
	}
	
	/**
	 * method to check telegram trailing checksum
	 * @param readBuffer byte array
	 * @return boolean value comparing last value of the byte array with the XOR checksum
	 */
	private boolean verifyChecksum(final byte[] readBuffer) {
		return readBuffer[readBuffer.length-1] == Checksum.XOR(readBuffer);
	}

	/**
	 * @param isTransmitFinished the isTransmitFinished to set
	 */
	public void setTransmitFinished(boolean isTransmitFinished) {
		this.isTransmitFinished = isTransmitFinished;
	}
}
