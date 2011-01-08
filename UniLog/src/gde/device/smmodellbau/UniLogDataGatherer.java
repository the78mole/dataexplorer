/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import gde.log.Level;
import java.util.logging.Logger;

import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.smmodellbau.unilog.MessageIds;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.TimeOutException;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.CalculationThread;
import gde.utils.WaitTimer;

/**
 * Thread implementation to gather data from UniLog device
 * @author Winfied Brügmann
 */
public class UniLogDataGatherer extends Thread {
	final static Logger			log							= Logger.getLogger(UniLogDataGatherer.class.getName());

	final DataExplorer			application;
	final UniLogSerialPort	serialPort;
	final UniLogDialog			dialog;
	final UniLog						device;
	final Integer						channelNumber;
	final String						configKey;
	CalculationThread				calculationThread;

	/**
	 * 
	 */
	public UniLogDataGatherer(DataExplorer currrentApplication, UniLog useDevice, UniLogSerialPort useSerialPort, String useConfigKey) {
		super("dataGatherer");
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
				WaitTimer.delay(3000);
			}
			
			// get UniLog configuration for timeStep info
			byte[] readBuffer = this.serialPort.readConfiguration();
			if (this.dialog != null) {
				this.dialog.updateConfigurationValues(readBuffer);
				this.dialog.updateActualConfigTabItemAnalogModi(this.channelNumber);
			}

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

				recordSetKey = channel.getNextRecordSetNumber() + this.device.getRecordSetStemName();
				
				// check analog modus and update channel/configuration
				this.device.updateMeasurementByAnalogModi(telegrams.get(3), this.channelNumber);
				
				channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, this.application.getActiveDevice(), channel.getNumber(), true, false));
				log.log(Level.FINE, recordSetKey + " created"); //$NON-NLS-1$

				recordSet = channel.get(recordSetKey); // record set where the data is added
				this.device.updateInitialRecordSetComment(recordSet);
				channel.applyTemplate(recordSetKey, true);
				

				int[] points = new int[recordSet.realSize()];

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
			log.log(Level.FINE, "exit data gatherer"); //$NON-NLS-1$

		}
		catch (DataInconsitsentException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0028, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
		}
		catch (ApplicationConfigurationException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), e.getClass().getSimpleName() + " - " + e.getMessage());
		}
		catch (TimeOutException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() } )
			+ System.getProperty("line.separator") + Messages.getString(MessageIds.GDE_MSGW1300)); //$NON-NLS-1$
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() } )
			+ System.getProperty("line.separator") + Messages.getString(MessageIds.GDE_MSGW1300)); //$NON-NLS-1$
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialog.getDialogShell(), e.getClass().getSimpleName() + " - " + e.getMessage());
		}
		finally {
			this.device.getDialog().resetButtons();
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
		this.device.makeInActiveDisplayable(recordSet);
		channel.applyTemplate(recordSetKey, true);
		this.application.updateStatisticsData();
		this.application.updateDataTable(recordSetKey, false);
	}

	public void setThreadStop() {
		try {
			WaitTimer.delay(5);
			this.serialPort.setTransmitFinished(true);
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage());
		}
	}
}
