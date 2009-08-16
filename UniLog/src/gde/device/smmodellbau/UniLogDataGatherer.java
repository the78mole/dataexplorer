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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.smmodellbau.unilog.MessageIds;
import osde.exception.ApplicationConfigurationException;
import osde.exception.DataInconsitsentException;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.CalculationThread;

/**
 * Thread implementation to gather data from UniLog device
 * @author Winfied Brügmann
 */
public class UniLogDataGatherer extends Thread {
	final static Logger			log							= Logger.getLogger(UniLogDataGatherer.class.getName());

	OpenSerialDataExplorer	application;
	final String						RECORD_SET_NAME	= Messages.getString(MessageIds.OSDE_MSGT1378);
	final UniLogSerialPort	serialPort;
	final UniLogDialog			dialog;
	final UniLog						device;
	final Integer						channelNumber;
	final String						configKey;
	CalculationThread				calculationThread;

	/**
	 * 
	 */
	public UniLogDataGatherer(OpenSerialDataExplorer currrentApplication, UniLog useDevice, UniLogSerialPort useSerialPort, String useConfigKey) {
		this.application = currrentApplication;
		this.device = useDevice;
		this.serialPort = useSerialPort;
		this.dialog = useDevice.getDialog();
		this.channelNumber = new Integer(useConfigKey.trim().split(":")[0].trim()); //$NON-NLS-1$
		this.configKey = useConfigKey.trim().split(":")[1].trim(); //$NON-NLS-1$
}

	/**
	 * method implements the data gathering from device and load data into the records
	 * if more than one available record set is created the status bar shows the status how many record sets are done
	 * this gives not a real feeling since the record sets may have big differences in number of available telegrams
	 */
	@Override
	@SuppressWarnings("unchecked") //$NON-NLS-1$
	// cast from Object to Vector<Integer>
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
					UniLogDataGatherer.this.dialog.updateConfigurationValues(configBuffer);
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
				this.device.updateMeasurementByAnalogModi(telegrams.get(3), this.configKey);
				
				channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, this.application.getActiveDevice(), this.configKey, true, false));
				log.log(Level.FINE, recordSetKey + " created"); //$NON-NLS-1$

				recordSet = channel.get(recordSetKey); // record set where the data is added
				this.device.updateInitialRecordSetComment(recordSet);
				

				int[] points = new int[this.device.getNumberOfMeasurements(recordSet.getChannelConfigName())];

				for (int j = 2; j < telegrams.size(); j++) {
					byte[] dataBuffer = telegrams.get(j);
					recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer));
				}

				if (i == 0 && channel.getActiveRecordSet() == null) {
					Channels.getInstance().switchChannel(this.channelNumber, recordSetKey);
					channel.switchRecordSet(recordSetKey);
				}
				finalizeRecordSet(channel, recordSetKey, recordSet);
			}
			// make all record set names visible in selection combo
			this.application.getMenuToolBar().updateRecordSetSelectCombo();
			this.dialog.resetButtons();
			log.log(Level.FINE, "exit data gatherer"); //$NON-NLS-1$

		}
		catch (DataInconsitsentException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(osde.messages.MessageIds.OSDE_MSGE0028, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
			this.device.getDialog().resetButtons();
		}
		catch (ApplicationConfigurationException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), e.getClass().getSimpleName() + " - " + e.getMessage());
			this.device.getDialog().resetButtons();
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(osde.messages.MessageIds.OSDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() } )
			+ System.getProperty("line.separator") + Messages.getString(MessageIds.OSDE_MSGW1300)); //$NON-NLS-1$
			this.device.getDialog().resetButtons();
		}
		finally {
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
