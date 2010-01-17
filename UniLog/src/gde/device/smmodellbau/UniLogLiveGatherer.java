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
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import osde.log.Level;
import java.util.logging.Logger;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.PropertyType;
import osde.device.smmodellbau.unilog.MessageIds;
import osde.exception.DataInconsitsentException;
import osde.exception.TimeOutException;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;

/**
 * Thread implementation to gather data from UniLog device
 * @author Winfied Brügmann
 */
public class UniLogLiveGatherer extends Thread {
	final static Logger			log													= Logger.getLogger(UniLogLiveGatherer.class.getName());

	OpenSerialDataExplorer	application;
	final String						RECORD_SET_NAME							= Messages.getString(MessageIds.OSDE_MSGT1378);
	final UniLogSerialPort	serialPort;
	final UniLog						device;
	final UniLogDialog			dialog;
	final Channels					channels;
	final Channel						channel;
	final Integer						channelNumber;
	final int								timeStep_ms;
	Timer										timer;
	TimerTask								timerTask;
	boolean									isTimerRunning							= false;
	boolean									isPortOpenedByLiveGatherer	= false;
	final int[]							time_ms											= { 1000 / 4, 1000 / 4, 1000 / 4, 1000 / 2, 1000, 2000, 5000, 10000 };
	boolean 								isSwitchedRecordSet 				= false;
	boolean									isGatheredRecordSetVisible	= true;


	// offsets and factors are constant over thread live time
	final HashMap<String, Double> calcValues = new HashMap<String, Double>();

	/**
	 * @throws Exception 
	 */
	public UniLogLiveGatherer(OpenSerialDataExplorer currentApplication, UniLog useDevice, UniLogSerialPort useSerialPort, int channelConfigNumber, UniLogDialog useDialog) throws Exception {
		this.application = currentApplication;
		this.device = useDevice;
		this.serialPort = useSerialPort;
		this.dialog = useDialog;
		this.channels = Channels.getInstance();
		this.channelNumber = channelConfigNumber;
		this.channel = this.channels.get(this.channelNumber);

		this.calcValues.put(UniLog.A1_FACTOR, useDevice.getMeasurementFactor(this.channelNumber, 11)); // 11 = A1
		this.calcValues.put(UniLog.A1_OFFSET, useDevice.getMeasurementOffset(this.channelNumber, 11));
		this.calcValues.put(UniLog.A2_FACTOR, useDevice.getMeasurementFactor(this.channelNumber, 12)); // 12 = A2
		this.calcValues.put(UniLog.A2_OFFSET, useDevice.getMeasurementOffset(this.channelNumber, 12));
		this.calcValues.put(UniLog.A3_FACTOR, useDevice.getMeasurementFactor(this.channelNumber, 13)); // 13 = A3
		this.calcValues.put(UniLog.A3_OFFSET, useDevice.getMeasurementOffset(this.channelNumber, 13));
		PropertyType property = useDevice.getMeasruementProperty(this.channelNumber, 6, UniLog.NUMBER_CELLS); // 6 = voltage/cell
		int numCellValue = property != null ? new Integer(property.getValue()) : 4;
		this.calcValues.put(UniLog.NUMBER_CELLS, (double)numCellValue);
		property = useDevice.getMeasruementProperty(this.channelNumber, 8, UniLog.PROP_N_100_W); // 8 = efficience
		int prop_n100W = property != null ? new Integer(property.getValue()) : 10000;
		this.calcValues.put(UniLog.PROP_N_100_W, (double)prop_n100W);

		if (!this.serialPort.isConnected()) {
			UniLogLiveGatherer.this.serialPort.open();
			this.isPortOpenedByLiveGatherer = true;
			Thread.sleep(2000);
		}
		
		// get UniLog configuration for timeStep info
		byte[] readBuffer = useSerialPort.readConfiguration();
		useDialog.updateConfigurationValues(readBuffer);
		useDialog.updateActualConfigTabItemAnalogModi(this.channel.getNumber());

		// timer interval
		int timeIntervalPosition = readBuffer[10] & 0xFF;
		this.timeStep_ms = this.time_ms[timeIntervalPosition];
		log.log(Level.FINE, "timeIntervalPosition = " + timeIntervalPosition + " timeStep_ms = " + this.timeStep_ms); //$NON-NLS-1$ //$NON-NLS-2$

	}

	public void run() {
		this.channels.switchChannel(this.channel.getName());

		// prepare timed data gatherer thread
		this.serialPort.isInterruptedByUser = false;
		int delay = 0;
		int period = this.timeStep_ms;
		log.log(Level.FINE, "timer period = " + period + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
		final String recordSetKey = this.channel.getNextRecordSetNumber() + this.RECORD_SET_NAME;

		this.channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, this.device, this.channelNumber, true, false));
		log.log(Level.FINE, recordSetKey + " created for channel " + this.channel.getName()); //$NON-NLS-1$
		final RecordSet recordSet = this.channel.get(recordSetKey);
		this.device.updateInitialRecordSetComment(recordSet);
		recordSet.setTimeStep_ms(this.timeStep_ms);
		recordSet.setTableDisplayable(false); // suppress table display during live data gathering
		updateActiveState(recordSet);
		final int[] points = new int[recordSet.size()];
		final UniLog usedDevice = this.device;

		try {
			this.serialPort.checkConnectionStatus();
			this.serialPort.wait4LiveData(100);
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			String message = Messages.getString(osde.messages.MessageIds.OSDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() }) + System.getProperty("line.separator")
					+ Messages.getString(MessageIds.OSDE_MSGW1301);
			cleanup(recordSetKey, message);
		}

		if (!this.serialPort.isInterruptedByUser) {
			this.timer = new Timer();
			this.timerTask = new TimerTask() {

				public void run() {
					log.log(Level.FINE, "====> entry"); //$NON-NLS-1$
					try {
						if (UniLogLiveGatherer.this.isTimerRunning) {
							// prepare the data for adding to record set
							log.log(Level.FINE, "recordSetKey = " + recordSetKey + " channelKonfigKey = " + recordSet.getChannelConfigName()); //$NON-NLS-1$ //$NON-NLS-2$

							// build the point array according curves from record set
							byte[] dataBuffer = UniLogLiveGatherer.this.serialPort.queryLiveData();

							recordSet.addPoints(usedDevice.convertDataBytes(points, dataBuffer));

							// switch the active record set if the current record set is child of active channel
							if (!UniLogLiveGatherer.this.isSwitchedRecordSet && UniLogLiveGatherer.this.channel.getName().equals(UniLogLiveGatherer.this.channels.getActiveChannel().getName())) {
								UniLogLiveGatherer.this.device.updateMeasurementByAnalogModi(dataBuffer, recordSet.getChannelConfigNumber());
								UniLogLiveGatherer.this.channel.applyTemplateBasics(recordSetKey);
								UniLogLiveGatherer.this.application.getMenuToolBar().addRecordSetName(recordSetKey);
								UniLogLiveGatherer.this.channels.getActiveChannel().switchRecordSet(recordSetKey);
								UniLogLiveGatherer.this.isSwitchedRecordSet = true;
							}

							if (recordSet.isChildOfActiveChannel() && recordSet.equals(UniLogLiveGatherer.this.channels.getActiveChannel().getActiveRecordSet())) {
								OpenSerialDataExplorer.display.asyncExec(new Runnable() {
									public void run() {
										UniLogLiveGatherer.this.application.updateGraphicsWindow();
										UniLogLiveGatherer.this.application.updateStatisticsData();
										//UniLogLiveGatherer.this.application.updateDataTable(recordSetKey);
										UniLogLiveGatherer.this.application.updateDigitalWindowChilds();
										UniLogLiveGatherer.this.application.updateAnalogWindowChilds();
										//LiveGathererThread.this.application.updateCellVoltageChilds();
									}
								});
							}
						}
					}
					catch (DataInconsitsentException e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						String message = Messages.getString(osde.messages.MessageIds.OSDE_MSGE0028, new Object[] { e.getClass().getSimpleName(), e.getMessage() });
						cleanup(recordSetKey, message);
					}
					catch (TimeOutException e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						String message = Messages.getString(osde.messages.MessageIds.OSDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() })
								+ System.getProperty("line.separator") + Messages.getString(MessageIds.OSDE_MSGW1301); //$NON-NLS-1$ 
						cleanup(recordSetKey, message);
					}
					catch (IOException e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						String message = Messages.getString(osde.messages.MessageIds.OSDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() })
								+ System.getProperty("line.separator") + Messages.getString(MessageIds.OSDE_MSGW1301); //$NON-NLS-1$ 
						cleanup(recordSetKey, message);
					}
					catch (Throwable e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						String message = e.getClass().getSimpleName() + " - " + e.getMessage(); //$NON-NLS-1$ 
						cleanup(recordSetKey, message);
					}
					log.log(Level.FINE, "======> exit"); //$NON-NLS-1$
				}
			};

			// start the prepared timer thread within the live data gatherer thread
			this.timer.scheduleAtFixedRate(this.timerTask, delay, period);
			this.isTimerRunning = true;
			log.log(Level.FINE, "exit"); //$NON-NLS-1$
		}
		else {
			cleanup(recordSetKey, null);
		}
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
	public void stopTimerThread() {
		if (this.timerTask != null) 
			this.timerTask.cancel();
		if (this.timer != null) {
			this.timer.cancel();
			this.timer.purge();
		}
		this.isTimerRunning = false;
	}

	/**
	 * cleanup all allocated resources and display the message
	 * @param recordSetKey
	 * @param message
	 */
	void cleanup(final String recordSetKey, String message) {
		boolean isErrorState =  this.isTimerRunning;
		this.stopTimerThread();
		if(this.isPortOpenedByLiveGatherer) 
			this.serialPort.close(); 
		this.channel.get(recordSetKey).clear();
		this.channel.remove(recordSetKey);
		this.application.getMenuToolBar().updateRecordSetSelectCombo();
		this.application.updateStatisticsData();
		this.application.updateDataTable(recordSetKey);
		this.device.getDialog().resetButtons();
		if (message != null && message.length() > 5 && isErrorState) {
			this.application.openMessageDialog(this.dialog.getDialogShell(), message);
		}
	}
}
