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
package osde.device.smmodellbau;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import osde.log.Level;
import java.util.logging.Logger;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.smmodellbau.lipowatch.MessageIds;
import osde.exception.ApplicationConfigurationException;
import osde.exception.DataInconsitsentException;
import osde.exception.TimeOutException;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.CalculationThread;

/**
 * Thread implementation to gather data from LiPoWatch device
 * @author Winfied Brügmann
 */
public class LiPoWatchDataGatherer extends Thread {
	final static Logger			log							= Logger.getLogger(LiPoWatchDataGatherer.class.getName());

	OpenSerialDataExplorer		application;
	final String							RECORD_SET_NAME	= Messages.getString(MessageIds.OSDE_MSGT1601);
	final LiPoWatchSerialPort	serialPort;
	final LiPoWatchDialog			dialog;
	final LiPoWatch						device;
	final Integer							channelNumber;
	final String							configKey;
	CalculationThread					calculationThread;

	/**
	 * 
	 */
	public LiPoWatchDataGatherer(OpenSerialDataExplorer currrentApplication, LiPoWatch useDevice, LiPoWatchSerialPort useSerialPort) {
		this.application = currrentApplication;
		this.device = useDevice;
		this.serialPort = useSerialPort;
		this.dialog = useDevice.getDialog();
		this.channelNumber = 1; //$NON-NLS-1$
		this.configKey = this.device.getChannelName(this.channelNumber);
}

	/**
	 * method implements the data gathering from device and load data into the records
	 * if more than one available record set is created the status bar shows the status how many record sets are done
	 * this gives not a real feeling since the record sets may have big differences in number of available telegrams
	 */
	@Override
	@SuppressWarnings("unchecked") //$NON-NLS-1$
	// cast from Object to Vector<byte[]>
	public void run() {
		log.log(Level.FINE, "entry data gatherer : " + this.channelNumber + " : " + this.configKey); //$NON-NLS-1$ //$NON-NLS-2$
		Channel channel = Channels.getInstance().get(this.channelNumber);
		String recordSetKey = null;
		RecordSet recordSet = null;
		boolean isPortOpenedByMe = false;

		try {
			if(!this.serialPort.isConnected()) {
				this.serialPort.open();
				isPortOpenedByMe = true;
			}
			
			//update the config display of the dialog to enable comment enichments
			final byte[] configBuffer = this.serialPort.readConfiguration();
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					LiPoWatchDataGatherer.this.dialog.updateConfigurationValues(configBuffer);
				}
			});

			this.dialog.resetDataSetsLabel();
			this.serialPort.setTransmitFinished(false);
			HashMap<String, Object> data = this.serialPort.getData(this.dialog);
			log.log(Level.FINE, "back from gathering data"); //$NON-NLS-1$

			// iterate over number of telegram sets in map
			String[] keys = data.keySet().toArray(new String[0]);		
			Arrays.sort(keys);
			for (int i = 0; i < keys.length; i++) {
				Vector<byte[]> telegrams = (Vector<byte[]>) data.get(keys[i]); //$NON-NLS-1$
				// iterate over telegram entries to build the record set
				log.log(Level.FINER, "number record set = " + keys[i]); //$NON-NLS-1$

				recordSetKey = channel.getNextRecordSetNumber() + this.RECORD_SET_NAME;
				
				// check analog modus and update channel/configuration
				//this.device.updateMeasurementByAnalogModi(telegrams.get(3), this.configKey);
				
				channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, this.application.getActiveDevice(), channel.getNumber(), true, false));
				log.log(Level.FINE, recordSetKey + " created"); //$NON-NLS-1$

				recordSet = channel.get(recordSetKey); // record set where the data is added
				this.device.updateInitialRecordSetComment(recordSet);
				
				int[] points = new int[recordSet.realSize()];
				byte[] dataBuffer;
				for (int j = 1; j < telegrams.size(); j++) {
					dataBuffer = telegrams.get(j);
					recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer));
				}

				//reduce receodSet to really available number of cells, calculate average over first 10 measurements since I got different number of cells
				int numCells = 0;
				log.log(Level.FINE, "number of measurements = " +  telegrams.size());
				for (int j = 0; j < 10 && j < telegrams.size(); j++) {
					dataBuffer = telegrams.get(j);
					numCells += (dataBuffer[5] & 0x0F);
				}
				int numberRecords = numCells/10 + 4; // number cells + total battery voltage + servo impuls in + servio impuls out + temperature
				String[] recordKeys = recordSet.getRecordNames();
				for (int j = numberRecords; j < this.device.getNumberOfMeasurements(1); j++) {
					recordSet.remove(recordKeys[j]);
				}

				if (i == 0 && channel.getActiveRecordSet() == null) {
					Channels.getInstance().switchChannel(this.channelNumber, recordSetKey);
					channel.switchRecordSet(recordSetKey);
				}
				finalizeRecordSet(channel, recordSetKey, recordSet);
			}
			// make all record set names visible in selection combo
			this.application.getMenuToolBar().updateRecordSetSelectCombo();
			log.log(Level.FINE, "exit data gatherer"); //$NON-NLS-1$

		}
		catch (DataInconsitsentException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(osde.messages.MessageIds.OSDE_MSGE0028, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
		}
		catch (ApplicationConfigurationException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), e.getClass().getSimpleName() + " - " + e.getMessage()); //$NON-NLS-1$
		}
		catch (TimeOutException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(osde.messages.MessageIds.OSDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() } )
			+ System.getProperty("line.separator") + Messages.getString(MessageIds.OSDE_MSGW1602)); //$NON-NLS-1$
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(osde.messages.MessageIds.OSDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() } )
			+ System.getProperty("line.separator") + Messages.getString(MessageIds.OSDE_MSGW1602)); //$NON-NLS-1$
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), e.getClass().getSimpleName() + " - " + e.getMessage());
		}
		finally {
			this.dialog.resetButtons();
			if(isPortOpenedByMe) this.serialPort.close();
		}
	} // end of run()

	/**
	 * calculate missing data, check all cross dependencies and switch records to display able
	 * @param channel
	 * @param recordSetKey
	 * @param recordSet
	 */
	private void finalizeRecordSet(Channel channel, String recordSetKey, RecordSet recordSet) {
		recordSet.setTableDisplayable(true); // enable table display after calculation
		this.device.updateVisibilityStatus(recordSet);
		this.device.makeInActiveDisplayable(recordSet);
		channel.applyTemplate(recordSetKey, true);
		this.application.updateStatisticsData();
		this.application.updateDataTable(recordSetKey);
	}

	public void setThreadStop() {
		try {
			Thread.sleep(2);
			this.serialPort.setTransmitFinished(true);
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage());
		}
	}
}
