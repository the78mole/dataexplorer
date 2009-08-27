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

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.smmodellbau.lipowatch.MessageIds;
import osde.exception.DataInconsitsentException;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;

/**
 * Thread implementation to gather data from LiPoWatch device
 * @author Winfied Brügmann
 */
public class LiPoWatchLiveGatherer extends Thread {
	final static Logger			log													= Logger.getLogger(LiPoWatchLiveGatherer.class.getName());

	OpenSerialDataExplorer	application;
	final String						RECORD_SET_NAME							= Messages.getString(MessageIds.OSDE_MSGT1601);
	final LiPoWatchSerialPort	serialPort;
	final LiPoWatch						device;
	final LiPoWatchDialog			dialog;
	final Channels					channels;
	final Channel						channel;
	final Integer						channelNumber;
	final String						configKey;
	final int								timeStep_ms;
	Timer										timer;
	TimerTask								timerTask;
	boolean									isTimerRunning							= false;
	boolean									isPortOpenedByLiveGatherer	= false;
	final int[]							time_ms											= { 1000/4, 1000/2, 1000, 2000, 5000, 10000 };
	boolean 								isSwitchedRecordSet 				= false;
	boolean									isGatheredRecordSetVisible	= true;


	// offsets and factors are constant over thread live time
	final HashMap<String, Double> calcValues = new HashMap<String, Double>();

	/**
	 * @throws Exception 
	 */
	public LiPoWatchLiveGatherer(OpenSerialDataExplorer currentApplication, LiPoWatch useDevice, LiPoWatchSerialPort useSerialPort, LiPoWatchDialog useDialog) throws Exception {
		this.application = currentApplication;
		this.device = useDevice;
		this.serialPort = useSerialPort;
		this.dialog = useDialog;
		this.channels = Channels.getInstance();
		this.channelNumber = 1; 
		this.channel = this.channels.get(this.channelNumber);
		this.configKey = this.device.getChannelName(this.channelNumber);

		this.calcValues.put(LiPoWatch.A1_FACTOR, useDevice.getMeasurementFactor(this.configKey, 11)); // 11 = A1
		this.calcValues.put(LiPoWatch.A1_OFFSET, useDevice.getMeasurementOffset(this.configKey, 11));

		if (!this.serialPort.isConnected()) {
			this.serialPort.open();
			this.isPortOpenedByLiveGatherer = true;
		}

		// get LiPoWatch configuration for timeStep info
		byte[] readBuffer = useSerialPort.readConfiguration();
		useDialog.updateConfigurationValues(readBuffer);

		// timer interval
		int timeIntervalPosition = readBuffer[10] & 0xFF;
		this.timeStep_ms = this.time_ms[timeIntervalPosition];
		log.log(Level.INFO, "timeIntervalPosition = " + timeIntervalPosition + " timeStep_ms = " + this.timeStep_ms); //$NON-NLS-1$ //$NON-NLS-2$

	}

	public void run() {
		this.isTimerRunning = true;
		this.channels.switchChannel(this.channel.getName());

		// prepare timed data gatherer thread
		int delay = 0;
		int period = this.timeStep_ms;
		log.log(Level.FINE, "timer period = " + period + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
		final String recordSetKey = this.channel.getNextRecordSetNumber() + this.RECORD_SET_NAME;

		this.channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, this.device, this.device.getChannelName(this.channelNumber), true, false));
		log.log(Level.FINE, recordSetKey + " created for channel " + this.channel.getName()); //$NON-NLS-1$
		final RecordSet recordSet = this.channel.get(recordSetKey);
		this.device.updateInitialRecordSetComment(recordSet);
		recordSet.setTimeStep_ms(this.timeStep_ms);
		recordSet.setTableDisplayable(false); // suppress table display during live data gathering
		updateActiveState(recordSet);
		final int[] points = new int[recordSet.size()];
		final LiPoWatch usedDevice = this.device;
		
		try {
			this.serialPort.checkConnectionStatus();
			this.serialPort.wait4LifeData(100);
		}
		catch (Throwable e) {
			String message = Messages.getString(osde.messages.MessageIds.OSDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() } );
			cleanup(recordSetKey, message, e);
		}
		
		this.timer = new Timer();
		this.timerTask = new TimerTask() {

			public void run() {
				log.log(Level.FINE, "====> entry"); //$NON-NLS-1$
				try {
					if (LiPoWatchLiveGatherer.this.isTimerRunning) {
						// prepare the data for adding to record set
						log.log(Level.FINE, "recordSetKey = " + recordSetKey + " channelKonfigKey = " + recordSet.getChannelConfigName()); //$NON-NLS-1$ //$NON-NLS-2$

						// build the point array according curves from record set
						recordSet.addPoints(usedDevice.convertDataBytes(points, LiPoWatchLiveGatherer.this.serialPort.queryLiveData()));

						// switch the active record set if the current record set is child of active channel
						if (!LiPoWatchLiveGatherer.this.isSwitchedRecordSet && LiPoWatchLiveGatherer.this.channel.getName().equals(LiPoWatchLiveGatherer.this.channels.getActiveChannel().getName())) {
							//LiPoWatchLiveGatherer.this.device.updateMeasurementByAnalogModi(dataBuffer, recordSet.getChannelConfigName());
							LiPoWatchLiveGatherer.this.channel.applyTemplateBasics(recordSetKey);
							LiPoWatchLiveGatherer.this.application.getMenuToolBar().addRecordSetName(recordSetKey);
							LiPoWatchLiveGatherer.this.channels.getActiveChannel().switchRecordSet(recordSetKey);
							LiPoWatchLiveGatherer.this.isSwitchedRecordSet = true;
						}
						
						if (recordSet.isChildOfActiveChannel() && recordSet.equals(LiPoWatchLiveGatherer.this.channels.getActiveChannel().getActiveRecordSet())) {
							OpenSerialDataExplorer.display.asyncExec(new Runnable() {
								public void run() {
									LiPoWatchLiveGatherer.this.application.updateGraphicsWindow();
									LiPoWatchLiveGatherer.this.application.updateStatisticsData();
									LiPoWatchLiveGatherer.this.application.updateDataTable(recordSetKey);
									LiPoWatchLiveGatherer.this.application.updateDigitalWindowChilds();
									LiPoWatchLiveGatherer.this.application.updateAnalogWindowChilds();
									LiPoWatchLiveGatherer.this.application.updateCellVoltageChilds();
								}
							});
						}
					}
				}
				catch (DataInconsitsentException e) {
					String message = Messages.getString(osde.messages.MessageIds.OSDE_MSGE0028, new Object[] { e.getClass().getSimpleName(), e.getMessage() } );
					cleanup(recordSetKey, message, e);				}
				catch (Throwable e) {
					String message = Messages.getString(osde.messages.MessageIds.OSDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() } );
					cleanup(recordSetKey, message, e);
				}
				log.log(Level.FINE, "======> exit"); //$NON-NLS-1$
			}
		};
		
		// start the prepared timer thread within the life data gatherer thread
		this.timer.scheduleAtFixedRate(this.timerTask, delay, period);
		log.log(Level.FINE, "exit"); //$NON-NLS-1$
	}

	/**
	 * @param recordSet
	 * @param displayableCounter
	 * @return
	 */
	private void updateActiveState(RecordSet recordSet) {
		// check if measurements isActive == false and set to isDisplayable == false
		for (String element : recordSet.getRecordNames()) {
			Record record = recordSet.get(element);
			if (!record.isActive()) {
				record.setDisplayable(false);
				record.setVisible(false);
			}
		}
	}

	/**
	 * close port, set isDisplayable according channel configuration and calculate slope
	 * @param recordSetKey
	 */
	public void finalizeRecordSet(String recordSetKey) {
		if (this.isPortOpenedByLiveGatherer) this.serialPort.close();

		RecordSet recordSet = this.channel.get(recordSetKey);
		recordSet.setTableDisplayable(true); // enable table display after calculation
		this.device.updateVisibilityStatus(recordSet);
		this.device.makeInActiveDisplayable(recordSet);
		this.channel.applyTemplate(recordSetKey, true);
		this.application.updateStatisticsData();
		this.application.updateDataTable(recordSetKey);
	}

	/**
	 * stop the timer task thread, this tops data capturing
	 * waits for all running timers tasks are ended before return 
	 */
	public synchronized void stopTimerThread() {
		if (this.timerTask != null) this.timerTask.cancel();
		if (this.timer != null) this.timer.cancel();
		this.isTimerRunning = false;
	}

	/**
	 * cleanup all allocated resources and display the message
	 * @param recordSetKey
	 * @param message
	 * @param e
	 */
	void cleanup(final String recordSetKey, String message, Throwable e) {
		this.stopTimerThread();
		if(this.isPortOpenedByLiveGatherer) this.serialPort.close(); 
		this.channel.get(recordSetKey).clear();
		this.channel.remove(recordSetKey);
		this.application.getMenuToolBar().updateRecordSetSelectCombo();
		this.application.updateStatisticsData();
		this.application.updateDataTable(recordSetKey);
		this.application.openMessageDialog(this.dialog.getDialogShell(), message);
		this.device.getDialog().resetButtons();
		log.log(Level.SEVERE, e.getMessage(), e);
	}
}
