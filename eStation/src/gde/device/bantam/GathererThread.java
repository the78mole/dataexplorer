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

import java.util.HashMap;
import java.util.logging.Level;
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
	final static String						$CLASS_NAME									= GathererThread.class.getName();			
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
	boolean												isPortOpenedByLiveGatherer	= false;
	boolean												isSwitchedRecordSet					= false;
	boolean												isGatheredRecordSetVisible	= true;

	RecordSet											recordSet;
	final static int							WAIT_TIME_RETRYS						= 36;
	int														retryCounter								= WAIT_TIME_RETRYS;				// 36 * 5 sec timeout = 180 sec
	long													timeStamp;
	boolean												isCollectDataStopped				= false;
	String												recordSetKey								= Messages.getString(osde.messages.MessageIds.OSDE_MSGT0272);
	int														numberBatteryCells					= 0;				// only if battery type is Lithium* single cell voltages will be available
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
		this.setPriority(Thread.MAX_PRIORITY);
	}

	public void run() {
		final String $METHOD_NAME = "run()";
		this.isCollectDataStopped = false;
		long startCycleTime = 0;
		long tmpCycleTime = 0;
		long sumCycleTime = 0;
		int cycleCount = 0;
		byte[] dataBuffer;
		StringBuilder sb = new StringBuilder();

		final int[] points = new int[this.device.getMeasurementNames(this.configKey).length];

		log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "====> entry "); //$NON-NLS-1$
		while (!this.isCollectDataStopped) {
			try {
				// get data from device
				dataBuffer = this.serialPort.getData();
				
				// calculate time step average to enable adjustment
				tmpCycleTime = System.currentTimeMillis();
				if (this.isProgrammExecuting) {
					++cycleCount;
					sumCycleTime += tmpCycleTime - startCycleTime;
					if (log.isLoggable(Level.FINE)) sb.append(", ").append(tmpCycleTime - startCycleTime);
					if (cycleCount % 10 == 0 ) {
						String avgCycleTime = String.format("%.1f", 1.0*sumCycleTime/cycleCount);
						this.device.setTimeStep_ms(new Double(avgCycleTime).doubleValue());
						log.logp(Level.INFO, $CLASS_NAME, $METHOD_NAME, avgCycleTime + sb.toString());
						sb = new StringBuilder();
					}
				}
				startCycleTime = tmpCycleTime;

				// check if device is ready for data capturing, discharge or charge allowed only
				// else wait for 180 seconds max. for actions
				String processName = this.device.USAGE_MODE[this.device.getProcessingMode(dataBuffer)];
				log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "processing mode = " + processName); //$NON-NLS-1$
				this.device.getConfigurationValues(this.configData, dataBuffer);
				
				
				if (processName.equals(this.device.USAGE_MODE[1]) || processName.equals(this.device.USAGE_MODE[2])) { // 1=discharge; 2=charge -> eStation active
					// check state change waiting to discharge to charge
					// check if a record set matching for re-use is available and prepare a new if required
					if (this.channel.size() == 0 
							|| !this.isProgrammExecuting  
							|| !this.recordSetKey.endsWith(processName) ) {
							//|| (System.currentTimeMillis() - this.getTimeStamp()) > 3 * 60000) {
						this.isWaitTimeChargeDischarge = true;
						this.isConfigUpdated = false;
						this.isProgrammExecuting = true;
						setRetryCounter(WAIT_TIME_RETRYS); // 36 * receive timeout sec timeout = 180 sec
						this.waitTime_ms = new Integer(this.configData.get(eStation.CONFIG_WAIT_TIME)).intValue() * 60000;
						log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "waitTime_ms = " + this.waitTime_ms); //$NON-NLS-1$
						// record set does not exist or is outdated, build a new name and create
						this.recordSetKey = this.channel.getNextRecordSetNumber() + ") [" + this.configData.get(eStation.CONFIG_BATTERY_TYPE) + "] " + processName; //$NON-NLS-1$ //$NON-NLS-2$
						this.channel.put(this.recordSetKey, RecordSet.createRecordSet(getName().trim(), this.recordSetKey, this.application
								.getActiveDevice(), true, false));
						this.channel.applyTemplateBasics(this.recordSetKey);
						log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, this.recordSetKey + " created for channel " + this.channel.getName()); //$NON-NLS-1$
						if (this.channel.getActiveRecordSet() == null) this.channel.setActiveRecordSet(this.recordSetKey);
						this.recordSet = this.channel.get(this.recordSetKey);
						this.recordSet.setTableDisplayable(false); // suppress table calc + display 
						this.recordSet.setAllDisplayable();
						this.channel.applyTemplate(this.recordSetKey);
						// switch the active record set if the current record set is child of active channel
						// for eStation its always the case since we have only one channel
						if (this.channel.getName().equals(this.channels.getActiveChannel().getName())) {
							this.channels.getActiveChannel().switchRecordSet(this.recordSetKey);
						}
					}
					else { //this.device.USAGE_MODE[0].equals("off");
						log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "re-using " + this.recordSetKey); //$NON-NLS-1$
					}
					//this.setTimeStamp();
					this.dialog.updateGlobalConfigData(this.configData);

					// prepare the data for adding to record set
					this.recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer), false);

					int posCells = this.device.getName().endsWith("BC6") ? 6 : 8; //$NON-NLS-1$
					this.numberBatteryCells = 0; //this.device.getNumberOfLithiumXCells(dataBuffer);
					String[] recordKeys = this.recordSet.getRecordNames();
					for (int i = posCells; i < this.recordSet.size(); i++) {
						Record record = this.recordSet.get(recordKeys[i]);
						if (record.getRealMinValue() != 0 && record.getRealMaxValue() != 0) {
							this.numberBatteryCells++;
							log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "record = " + record.getName() + " " + record.getRealMinValue() + " " + record.getRealMaxValue()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					}

					this.isGatheredRecordSetVisible = this.recordSetKey.equals(this.channels.getActiveChannel().getActiveRecordSet().getName());
					if (this.isGatheredRecordSetVisible) {
						this.application.updateGraphicsWindow();
						this.application.updateStatisticsData();
						this.application.updateDataTable(this.recordSetKey);
						this.application.updateDigitalWindowChilds();
						this.application.updateAnalogWindowChilds();

						log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "numberBatteryCells = " + this.numberBatteryCells); //$NON-NLS-1$
						if (this.numberBatteryCells > 0) {
							this.application.updateCellVoltageChilds();
						}
					}

					//switch off single cell voltage lines if not battery type of lithium where cell voltages are available
					for (int i = posCells + this.numberBatteryCells; !this.isConfigUpdated && i < points.length; i++) {
						this.recordSet.get(recordKeys[i]).setActive(false);
						this.recordSet.get(recordKeys[i]).setDisplayable(false);
						this.recordSet.get(recordKeys[i]).setVisible(false);
					}
					this.isConfigUpdated = true;
					//OsdReaderWriter.write("E:\\Temp\\not.osd", this.channel, 1);
				}
				else { // else wait for 180 seconds max. for actions
					log.logp(Level.INFO, $CLASS_NAME, $METHOD_NAME, "eStation inactiv"); //$NON-NLS-1$
					this.isProgrammExecuting = false;
					cycleCount = 0;
					if (this.recordSet != null && this.recordSet.getRecordDataSize(true) > 5) { // some other exception while program execution, record set has data points
						finalizeRecordSet(this.recordSetKey, false);
						this.recordSetKey	= Messages.getString(osde.messages.MessageIds.OSDE_MSGT0272);
						this.recordSet = null;
						this.application.openMessageDialogAsync(Messages.getString(MessageIds.OSDE_MSGT1409));
					}
					if (0 == (setRetryCounter(getRetryCounter() - 1))) {
						stopGathererThread();
					}
					log.log(Level.FINE, "inner retry counter = " + getRetryCounter());
				}
			}
			catch (DataInconsitsentException e) {
				String message = "Das Datenmodell der Anwendung wird fehlerhaft bedient.\n" + e.getClass().getSimpleName() + " - " + e.getMessage(); //$NON-NLS-1$
				cleanup(this.recordSetKey, message);
			}
			catch (Throwable e) {
				if (e instanceof TimeOutException && this.isWaitTimeChargeDischarge) {
					String batteryType = this.configData.get(eStation.CONFIG_BATTERY_TYPE);
					if (!batteryType.equals(this.device.ACCU_TYPES[0])) { // Lithium programm has no charge/discharge
						try {
							finalizeRecordSet(this.recordSetKey, false);
							log.logp(Level.INFO, $CLASS_NAME, $METHOD_NAME, "(dry time) waiting..."); //$NON-NLS-1$
							Thread.sleep(this.waitTime_ms);
							this.isWaitTimeChargeDischarge = false;
							this.isProgrammExecuting = false;
						}
						catch (InterruptedException e1) {
							// ignore
						}
					}
				}
				else if (e instanceof TimeOutException && !this.isProgrammExecuting) {
					if (0 == (setRetryCounter(getRetryCounter() - 1))) {
						log.log(Level.INFO, "waiting for eStation activation ..."); //$NON-NLS-1$
						setRetryCounter(WAIT_TIME_RETRYS); // 36 * receive timeout sec timeout = 180 sec
					}
					log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "retry counter = " + getRetryCounter());
				}
				else if (this.recordSet != null && this.recordSet.getRecordDataSize(true) > 5) { // some other exception while program execution, record set has data points
					this.stopGathererThread();
					finalizeRecordSet(this.recordSetKey, false);
					this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGT1409));
				}
				else {
					log.log(Level.WARNING, e.getMessage(), e);
					this.stopGathererThread();
					cleanup(this.recordSetKey, 
							Messages.getString(osde.messages.MessageIds.OSDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() }) + Messages.getString(MessageIds.OSDE_MSGT1408));
				}
			}
			//log.logp(Level.INFO,$CLASS_NAME, $METHOD_NAME, "processTime = " + (this.device.getTimeStep_ms().longValue()-10));
			wait($METHOD_NAME, this.device.getTimeStep_ms().longValue()-50);
		}
		log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "======> exit"); //$NON-NLS-1$
	}

	/**
	 * Thread.sleep(waitTime)
	 * @param $METHOD_NAME
	 */
	private void wait(final String $METHOD_NAME, long waitTime) {
		try {
			Thread.sleep(waitTime);
		}
		catch (InterruptedException e) {
			log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, e.getMessage());
		}
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
			//this.channel.applyTemplate(newRecordSetKey);
			//this.application.updateStatisticsData();
			this.application.updateDataTable(newRecordSetKey);
		}
	}

	/**
	 * stop the timer task thread, this tops data capturing
	 * waits for all running timers tasks are ended before return 
	 */
	public void stopGathererThread() {
		this.isCollectDataStopped = true;
		
		if (this.serialPort != null && this.serialPort.getXferErrors() > 0) {
			log.log(Level.WARNING, "During complete data transfer " + this.serialPort.getXferErrors() + " number of errors occured!"); //$NON-NLS-1$ //$NON-NLS-2$
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
			this.application.openMessageDialog(message);
	}

	/**
	 * set timeStamp using Date().getTime()/System.currentTimeMillis()
	 */
	void setTimeStamp() {
		this.timeStamp = System.currentTimeMillis();
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
}
