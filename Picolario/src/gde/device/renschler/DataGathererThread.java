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
package osde.device.renschler;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.PropertyType;
import osde.exception.DataInconsitsentException;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.CalculationThread;
import osde.utils.QuasiLinearRegression;

/**
 * Thread implementation to gather data from Picolariolog device
 * @author Winfied Brügmann
 */
public class DataGathererThread extends Thread {
	final static Logger				log							= Logger.getLogger(DataGathererThread.class.getName());

	OpenSerialDataExplorer		application;
	String[]									datagramNumbers;
	final String							RECORD_SET_NAME	= Messages.getString(MessageIds.OSDE_MSGT1220);
	final String							configKey;
	final PicolarioSerialPort	serialPort;
	final PicolarioDialog			dialog;
	final Picolario						device;
	CalculationThread					calculationThread;
	boolean										threadStop			= false;

	/**
	 * 
	 */
	public DataGathererThread(OpenSerialDataExplorer currentApplication, Picolario currentDevice, PicolarioSerialPort currentSerialPort, String[] useDatagramNumbers) {
		this.application = currentApplication;
		this.device = currentDevice;
		this.serialPort = currentSerialPort;
		this.dialog = currentDevice.getDialog();
		this.datagramNumbers = useDatagramNumbers;
		this.configKey = currentDevice.getChannelName(1);
	}

	/**
	 * method implements the data gathering from device and load data into the records
	 * if more than one available record set is created the status bar shows the status how many record sets are done
	 * this gives not a real feeling since the record sets may have big differences in number of available telegrams
	 */
	public void run() {
		boolean isPortOpenedByMe = false;
		try {
			DataGathererThread.log.log(Level.FINE, "entry data gatherer"); //$NON-NLS-1$
			Channel channel = Channels.getInstance().getActiveChannel();
			String[] measurements = this.device.getMeasurementNames(channel.getNumber()); // 0=Spannung, 1=Höhe, 2=Steigrate
			String recordSetKey;

			this.dialog.resetDataSetsLabel();
			if (!this.serialPort.isConnected()) {
				this.serialPort.open();
				isPortOpenedByMe = true;
			}

			for (int j = 0; j < this.datagramNumbers.length && !this.threadStop; ++j) {
				this.dialog.resetTelegramLabel();
				this.dialog.setAlreadyRedDataSets(this.datagramNumbers[j]);
				Vector<byte[]> data = this.serialPort.getData(new Integer(this.datagramNumbers[j]).intValue(), this.device);
				recordSetKey = channel.getNextRecordSetNumber() + this.RECORD_SET_NAME;
				channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, this.application.getActiveDevice(), channel.getNumber(), true, false));
				DataGathererThread.log.log(Level.FINE, recordSetKey + " created"); //$NON-NLS-1$
				if (channel.getActiveRecordSet() == null) Channels.getInstance().getActiveChannel().setActiveRecordSet(recordSetKey);
				RecordSet recordSet = channel.get(recordSetKey);

				byte[] dataBuffer = new byte[recordSet.size()];
				int[] points = new int[this.device.getNumberOfMeasurements(1)];
				
				for (byte[] reveivedBuffer : data) { // 31 or x*3 + 1
					for (int i = 0; i < reveivedBuffer.length/3; ++i) {  // three bytes per datapoint
						System.arraycopy(reveivedBuffer, i*3, dataBuffer, 0, 3);
						recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer));
					}
				}
				
				// start slope calculation
				PropertyType property = recordSet.get(measurements[2]).getProperty(CalculationThread.REGRESSION_INTERVAL_SEC);
				int regressionInterval = property != null ? new Integer(property.getValue()) : 4;
				this.calculationThread = new QuasiLinearRegression(recordSet, measurements[1], measurements[2], regressionInterval);
				try {
					this.calculationThread.start();
				}
				catch (RuntimeException e) {
					log.log(Level.WARNING, e.getMessage(), e);
				}

				this.application.getMenuToolBar().addRecordSetName(recordSetKey);
				if (channel.getRecordSetNames().length <= 1 || this.dialog.isDoSwtichRecordSet()) channel.switchRecordSet(recordSetKey);

				// update the progress bar reading one after the other only
				channel.get(recordSetKey).setAllDisplayable();
				channel.applyTemplate(recordSetKey, true);
			}// end for
			this.dialog.enableReadButtons();
			DataGathererThread.log.log(Level.FINE, "exit data gatherer"); //$NON-NLS-1$

		}
		catch (DataInconsitsentException e) {
			DataGathererThread.log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(osde.messages.MessageIds.OSDE_MSGE0028, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
		}
		catch (Exception e) {
			DataGathererThread.log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(osde.messages.MessageIds.OSDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
		}
		finally {
			if (isPortOpenedByMe) this.serialPort.close();
			this.dialog.resetButtons();
		}
	} // end of run()

	public void setThreadStop(boolean enable) {
		this.threadStop = enable;
		this.serialPort.setTransmitFinished(true);
	}

}
