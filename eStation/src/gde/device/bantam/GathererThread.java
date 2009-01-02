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
package osde.device.bantam;

import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.exception.ApplicationConfigurationException;
import osde.exception.DataInconsitsentException;
import osde.exception.SerialPortException;
import osde.exception.TimeOutException;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;

/**
 * Thread implementation to gather data from UniLog device
 * @author Winfied Brügmann
 */
public class GathererThread extends Thread {
	/**
	 * 
	 */
	private static final int	WAIT_TIME_RETRYS	= 90;

	final static Logger						log													= Logger.getLogger(GathererThread.class.getName());

	OpenSerialDataExplorer				application;
	final EStationSerialPort			serialPort;
	final eStation								device;
	final EStationDialog					dialog;
	final Channels								channels;
	final Channel									channel;
	final Integer									channelNumber;
	final String[]								measurements;
	final String									configKey;
	final int											timeStep_ms;
	Timer													timer;
	TimerTask											timerTask;
	boolean												isTimerRunning							= false;
	boolean												isPortOpenedByLiveGatherer	= false;
	boolean												isSwitchedRecordSet					= false;
	boolean												isGatheredRecordSetVisible	= true;

	RecordSet											recordSet;
	int														retryCounter								= WAIT_TIME_RETRYS;				// 90 * 2 sec timeout = 180 sec
	long													timeStamp;
	boolean												isCollectDataStopped				= false;
	String												recordSetKey								= Messages.getString(osde.messages.MessageIds.OSDE_MSGT0272);
	int														numberBatteryCells					= 0;				// only if battery type is Lithium* single cell voltages will be available
	boolean												isUpdatedRecordSetCellState	= false;
	int														waitTime_ms									= 0;
	boolean												isWaitTimeChargeDischarge 	= false;
	boolean												isProgrammExecuting					= false;
	boolean												isConfigUpdated							= false;
	HashMap<String, String>				configData									= new HashMap<String, String>();

	// offsets and factors are constant over thread live time
	final HashMap<String, Double>	calcValues									= new HashMap<String, Double>();

	/**
	 * data gathere thread definition 
	 * @throws SerialPortException 
	 * @throws ApplicationConfigurationException 
	 * @throws Exception 
	 */
	public GathererThread(OpenSerialDataExplorer currentApplication, eStation useDevice, EStationSerialPort useSerialPort, String channelName, EStationDialog useDialog) throws ApplicationConfigurationException, SerialPortException {
		this.application = currentApplication;
		this.device = useDevice;
		this.dialog = useDialog;
		this.serialPort = useSerialPort;
		this.channels = Channels.getInstance();
		this.channelNumber = new Integer(channelName.trim().split(":")[0].trim());
		this.channel = this.channels.get(this.channelNumber);
		this.configKey = channelName.trim().split(":")[1].trim();

		this.measurements = useDevice.getMeasurementNames(this.configKey); // 0=Spannung, 1=Höhe, 2=Steigrate, ....

		if (!this.serialPort.isConnected()) {
			this.serialPort.open();
			this.isPortOpenedByLiveGatherer = true;
		}
		this.timeStep_ms = new Double(this.device.getTimeStep_ms()).intValue();
	}

	public void run() {
		this.isTimerRunning = true;

		// prepare timed data gatherer thread
		int delay = 0;
		int period = this.timeStep_ms;
		log.fine("timer period = " + period + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
		this.isCollectDataStopped = false;
		this.isUpdatedRecordSetCellState = false;

		final int[] points = new int[this.device.getMeasurementNames(this.configKey).length];
		final eStation usedDevice = this.device;

		this.timer = new Timer();
		this.timerTask = new TimerTask() {

			public void run() {
				log.fine("====> entry"); //$NON-NLS-1$
				try {
					if (GathererThread.this.isTimerRunning) {
						// prepare the data for adding to record set
						//log.fine("recordSetKey = " + recordSetKey + " channelKonfigKey = " + recordSet.getChannelConfigName());

						// build the point array according curves from record set
						byte[] dataBuffer = GathererThread.this.serialPort.getData();

						// check if device is ready for data capturing, discharge or charge allowed only
						// else wait for 180 seconds max. for actions
						String processName = GathererThread.this.device.USAGE_MODE[usedDevice.getProcessingMode(dataBuffer)];
						log.fine("processing mode = " + processName); //$NON-NLS-1$
						usedDevice.getConfigurationValues(GathererThread.this.configData, dataBuffer);
						if ((processName.equals(GathererThread.this.device.USAGE_MODE[1]) || processName.equals(GathererThread.this.device.USAGE_MODE[2]))// 1=discharge; 2=charge -> eStation active
							&& !GathererThread.this.isCollectDataStopped) { 
							// check state change waiting to discharge to charge
							// check if a record set matching for re-use is available and prepare a new if required
							if (GathererThread.this.channel.size() == 0 
									|| !GathererThread.this.channel.getRecordSetNames()[GathererThread.this.channel.getRecordSetNames().length - 1].endsWith(processName)
									|| !GathererThread.this.isWaitTimeChargeDischarge
									|| (new Date().getTime() - getTimeStamp()) > 3*60000) {
								GathererThread.this.isCollectDataStopped = false;
								GathererThread.this.isWaitTimeChargeDischarge = true;
								GathererThread.this.isConfigUpdated	= false;
								GathererThread.this.isProgrammExecuting	= true;
								GathererThread.this.waitTime_ms = new Integer(GathererThread.this.configData.get(eStation.CONFIG_WAIT_TIME)).intValue() * 60000;
								log.fine("waitTime_ms = " + GathererThread.this.waitTime_ms); //$NON-NLS-1$
								// record set does not exist or is outdated, build a new name and create
								GathererThread.this.recordSetKey = GathererThread.this.channel.getNextRecordSetNumber() + ") [" + GathererThread.this.configData.get(eStation.CONFIG_BATTERY_TYPE) + "] " + processName; //$NON-NLS-1$ //$NON-NLS-2$
								GathererThread.this.channel.put(GathererThread.this.recordSetKey, RecordSet.createRecordSet(getName().trim(), GathererThread.this.recordSetKey, GathererThread.this.application
										.getActiveDevice(), true, false));
								GathererThread.this.channel.applyTemplateBasics(GathererThread.this.recordSetKey);
								log.fine(GathererThread.this.recordSetKey + " created for channel " + GathererThread.this.channel.getName()); //$NON-NLS-1$
								if (GathererThread.this.channel.getActiveRecordSet() == null) GathererThread.this.channel.setActiveRecordSet(GathererThread.this.recordSetKey);
								GathererThread.this.recordSet = GathererThread.this.channel.get(GathererThread.this.recordSetKey);
								GathererThread.this.recordSet.setTableDisplayable(false); // suppress table calc + display 
								GathererThread.this.recordSet.setAllDisplayable();
								GathererThread.this.channel.applyTemplate(GathererThread.this.recordSetKey);
								// switch the active record set if the current record set is child of active channel
								// for eStation its always the case since we have only one channel
								if (GathererThread.this.channel.getName().equals(GathererThread.this.channels.getActiveChannel().getName())) {
									GathererThread.this.channels.getActiveChannel().switchRecordSet(GathererThread.this.recordSetKey);
								}
							}
							else {
								GathererThread.this.recordSetKey = GathererThread.this.channel.size() + ") [" + GathererThread.this.configData.get(eStation.CONFIG_BATTERY_TYPE) + "] " + processName; //$NON-NLS-1$ //$NON-NLS-2$
								log.fine("re-using " + GathererThread.this.recordSetKey); //$NON-NLS-1$
							}
							setTimeStamp();
							GathererThread.this.dialog.updateGlobalConfigData(GathererThread.this.configData);

							// prepare the data for adding to record set
							GathererThread.this.recordSet.addPoints(usedDevice.convertDataBytes(points, dataBuffer), false);

							int posCells = GathererThread.this.device.getName().endsWith("BC6") ? 6 : 8; //$NON-NLS-1$
							GathererThread.this.numberBatteryCells = 0; //GathererThread.this.device.getNumberOfLithiumXCells(dataBuffer);
							String[] recordKeys = GathererThread.this.recordSet.getRecordNames();
							for (int i = posCells; i < GathererThread.this.recordSet.size(); i++) {
								Record record = GathererThread.this.recordSet.get(recordKeys[i]);
								if (record.getRealMinValue() != 0 && record.getRealMaxValue() != 0) {
									GathererThread.this.numberBatteryCells++;
									log.fine("record = " + record.getName() + " " + record.getRealMinValue() + " " + record.getRealMaxValue()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								}
							}
							
							GathererThread.this.isGatheredRecordSetVisible = GathererThread.this.recordSetKey.equals(GathererThread.this.channels.getActiveChannel().getActiveRecordSet().getName());						
							if (GathererThread.this.isGatheredRecordSetVisible) {
								GathererThread.this.application.updateGraphicsWindow();
								GathererThread.this.application.updateStatisticsData();
								GathererThread.this.application.updateDataTable(GathererThread.this.recordSetKey);
								GathererThread.this.application.updateDigitalWindowChilds();
								GathererThread.this.application.updateAnalogWindowChilds();
								
								log.fine("numberBatteryCells = " + GathererThread.this.numberBatteryCells); //$NON-NLS-1$
								if (GathererThread.this.numberBatteryCells > 0) {
									GathererThread.this.application.updateCellVoltageChilds();
								}
							}

							//switch off single cell voltage lines if not battery type of lithium where cell voltages are available
							for (int i = posCells+GathererThread.this.numberBatteryCells; !GathererThread.this.isConfigUpdated && i < points.length; i++) {
								GathererThread.this.recordSet.get(recordKeys[i]).setActive(false);
								GathererThread.this.recordSet.get(recordKeys[i]).setDisplayable(false);
								GathererThread.this.recordSet.get(recordKeys[i]).setVisible(false);
							}
							GathererThread.this.isConfigUpdated = true;
						}
						else { // else wait for 180 seconds max. for actions
							if (0 == (setRetryCounter(getRetryCounter() - 1)) || GathererThread.this.isCollectDataStopped) {
								stopTimerThread();
								log.fine("Timer stopped eStation inactiv"); //$NON-NLS-1$
								setRetryCounter(WAIT_TIME_RETRYS); // 90 * receive timeout sec timeout = 180 sec
							}
							log.fine("retryCounter = " + getRetryCounter()); //$NON-NLS-1$
						}
					}
				}
				catch (DataInconsitsentException e) {
					String message = "Das Datenmodell der Anwendung wird fehlerhaft bedient.\n" + e.getClass().getSimpleName() + " - " + e.getMessage(); //$NON-NLS-1$
					cleanup(GathererThread.this.recordSetKey, message);
				}
				catch (Throwable e) {
					if (e instanceof TimeOutException && GathererThread.this.isWaitTimeChargeDischarge) {
						String batteryType = GathererThread.this.configData.get(eStation.CONFIG_BATTERY_TYPE);
						if (!batteryType.equals(GathererThread.this.device.ACCU_TYPES[0])) { // Lithium programm has no charge/discharge
							try {
								finalizeRecordSet(GathererThread.this.recordSetKey, false);
								log.fine("waiting..."); //$NON-NLS-1$
								Thread.sleep(GathererThread.this.waitTime_ms + GathererThread.this.timeStep_ms);
								GathererThread.this.isWaitTimeChargeDischarge = false;
								return;
							}
							catch (InterruptedException e1) {
								// ignore
							}
						}
					}
					String message = Messages.getString(osde.messages.MessageIds.OSDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() } )
					+ Messages.getString(MessageIds.OSDE_MSGT1408);
					if (GathererThread.this.isProgrammExecuting) {
						finalizeRecordSet(GathererThread.this.recordSetKey, false);
						GathererThread.this.stopTimerThread();
						if (GathererThread.this.isPortOpenedByLiveGatherer) 
							GathererThread.this.serialPort.close();
						GathererThread.this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGT1409));
					}
					else {
						cleanup(GathererThread.this.recordSetKey, message);
					}
				}
				log.fine("======> exit"); //$NON-NLS-1$
			}
		};

		// start the prepared timer thread within the life data gatherer thread
		this.timer.scheduleAtFixedRate(this.timerTask, delay, period);
		this.isTimerRunning = true;
		
		log.fine("exit"); //$NON-NLS-1$
	}

	/**
	 * close port, set isDisplayable according channel configuration and calculate slope
	 * @param newRecordSetKey
	 */
	public void finalizeRecordSet(String newRecordSetKey, boolean doClosePort) {
		if (doClosePort && this.isPortOpenedByLiveGatherer && this.serialPort.isConnected()) 
			this.serialPort.close();

		RecordSet tmpRecordSet = this.channel.get(newRecordSetKey);
		if (tmpRecordSet != null) {
			tmpRecordSet.setTableDisplayable(true); // enable table display after calculation
			this.device.updateVisibilityStatus(tmpRecordSet);
			this.device.makeInActiveDisplayable(tmpRecordSet);
			this.channel.applyTemplate(newRecordSetKey);
			this.application.updateStatisticsData();
			this.application.updateDataTable(newRecordSetKey);
		}
	}

	/**
	 * stop the timer task thread, this tops data capturing
	 * waits for all running timers tasks are ended before return 
	 */
	public synchronized void stopTimerThread() {
		if (this.timerTask != null) this.timerTask.cancel();
		if (this.timer != null) this.timer.cancel();
		this.timerTask = null;
		this.timer = null;
		this.isTimerRunning = false;
		this.isCollectDataStopped = true;
		
		if (this.serialPort != null && this.serialPort.getXferErrors() > 0) {
			log.warning("During complete data transfer " + this.serialPort.getXferErrors() + " number of errors occured!"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (this.serialPort.isConnected() && this.isPortOpenedByLiveGatherer == true && this.serialPort.isConnected()) {
			this.serialPort.close();
		}
	}

	/**
	 * cleanup all allocated resources and display the message
	 * @param useRecordSetKey
	 * @param message
	 * @param e
	 */
	void cleanup(final String useRecordSetKey, final String message) {
		this.stopTimerThread();
		if (this.isPortOpenedByLiveGatherer) 
			this.serialPort.close();
		
		if (this.channel.get(useRecordSetKey) != null) {
			this.channel.get(useRecordSetKey).clear();
			this.channel.remove(useRecordSetKey);
			if (Thread.currentThread().getId() == this.application.getThreadId()) {
				this.application.getMenuToolBar().updateRecordSetSelectCombo();
				this.application.updateStatisticsData();
				this.application.updateDataTable(useRecordSetKey);
				this.application.openMessageDialog(message);
				this.device.getDialog().resetButtons();
			}
			else {
				OpenSerialDataExplorer.display.asyncExec(new Runnable() {
					public void run() {
						GathererThread.this.application.getMenuToolBar().updateRecordSetSelectCombo();
						GathererThread.this.application.updateStatisticsData();
						GathererThread.this.application.updateDataTable(useRecordSetKey);
						GathererThread.this.application.openMessageDialog(message);
						GathererThread.this.device.getDialog().resetButtons();
					}
				});
			}
		}
		else
			GathererThread.this.application.openMessageDialog(message);
	}

	/**
	 * set timeStamp using Date().getTime()
	 */
	void setTimeStamp() {
		this.timeStamp = new Date().getTime();
	}

	/**
	 * @return the timeStamp
	 */
	long getTimeStamp() {
		return this.timeStamp;
	}

	/**
	 * @param enabled the isCollectDataStopped to set
	 */
	public void setCollectDataStopped(boolean enabled) {
		this.isCollectDataStopped = enabled;
	}

	/**
	 * @return the isCollectDataStopped
	 */
	public boolean isCollectDataStopped() {
		return this.isCollectDataStopped;
	}

	/**
	 * @return the retryCounter
	 */
	int getRetryCounter() {
		return this.retryCounter;
	}

	/**
	 * @param newRetryCounter the retryCounter to set
	 */
	int setRetryCounter(int newRetryCounter) {
		return this.retryCounter = newRetryCounter;
	}

	/**
	 * @return the isTimerRunning
	 */
	public boolean isTimerRunning() {
		return this.isTimerRunning;
	}
}
