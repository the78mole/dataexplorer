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
package gde.device.graupner;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.TimeLine;
import gde.utils.WaitTimer;

import java.util.logging.Logger;

/**
 * Thread implementation to gather data from eStation device
 * @author Winfied Brügmann
 */
public class GathererThread extends Thread {
	final static String				$CLASS_NAME									= GathererThread.class.getName();
	final static Logger				log													= Logger.getLogger(GathererThread.class.getName());
	final static int					TIME_STEP_DEFAULT						= 1000;
	final static int					WAIT_TIME_RETRYS						= 180;

	final DataExplorer				application;
	final UltramatSerialPort	serialPort;
	final Ultramat						device;
	final UltraDuoPlusDialog	dialog;
	final Channels						channels;

	String										recordSetKey1								= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization
	String										recordSetKey2								= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization
	String										recordSetKey3								= Messages.getString(gde.messages.MessageIds.GDE_MSGT0272); //default initialization
	boolean										isGatheredRecordSetVisible	= true;
	int												retryCounter								= GathererThread.WAIT_TIME_RETRYS;													// 36 * 5 sec timeout = 180 sec
	boolean										isCollectDataStopped				= false;

	boolean										isProgrammExecuting1				= false;
	boolean										isProgrammExecuting2				= false;
	boolean										isProgrammExecuting3				= false;
	boolean										isCombinedMode							= false;
	long											startCycleTime1							= 0;
	long											startCycleTime2							= 0;
	long											startCycleTime3							= 0;
	long											measurementCount1						= 0;
	long											measurementCount2						= 0;
	long											measurementCount3						= 0;

	/**
	 * data gatherer thread definition 
	 * @throws SerialPortException 
	 * @throws ApplicationConfigurationException 
	 * @throws Exception 
	 */
	public GathererThread() throws ApplicationConfigurationException, SerialPortException {
		super("dataGatherer"); //$NON-NLS-1$
		this.application = DataExplorer.getInstance();
		this.device = (Ultramat) this.application.getActiveDevice();
		this.dialog = null;//(UltraDuoPlusDialog) this.device.getDialog();
		this.serialPort = this.device.getCommunicationPort();
		this.channels = Channels.getInstance();

		this.setPriority(Thread.MAX_PRIORITY);
	}

	@Override
	public void run() {
		final String $METHOD_NAME = "run"; //$NON-NLS-1$
		RecordSet recordSet1 = null;
		RecordSet recordSet2 = null;
		RecordSet recordSet3 = null;
		int[] points1 = new int[this.device.getMeasurementNames(1).length];
		int[] points2 = new int[this.device.getMeasurementNames(2).length];
		int[] points3 = new int[this.device.getMeasurementNames(3).length];
		byte[] dataBuffer = null;

		this.isProgrammExecuting1 = false;
		this.isProgrammExecuting2 = false;
		this.isProgrammExecuting3 = false;
		this.startCycleTime1 = 0;
		this.startCycleTime2 = 0;
		this.startCycleTime3 = 0;
		this.measurementCount1 = 0;
		this.measurementCount2 = 0;
		this.measurementCount3 = 0;
		Object[] ch1, ch2, ch3;

		this.isCollectDataStopped = false;
		log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "====> entry initial time step ms = " + this.device.getTimeStep_ms()); //$NON-NLS-1$

		while (!this.isCollectDataStopped) {
			try {
				// get data from device
				if (this.serialPort.isConnected()) dataBuffer = this.serialPort.getData();

				switch (this.device.getDeviceTypeIdentifier()) {

				case Ultramat16S:
				case Ultramat18:
					this.isProgrammExecuting1 = this.device.isProcessing(1, dataBuffer);
					this.isProgrammExecuting2 = this.isProgrammExecuting3 = false;
					break;

				case UltramatTrio14:
				case UltramatTrio16S:
					this.isProgrammExecuting1 = this.device.isProcessing(1, dataBuffer);
					this.isProgrammExecuting2 = this.device.isProcessing(2, dataBuffer);
					this.isProgrammExecuting3 = this.device.isProcessing(3, dataBuffer);
					break;

				case UltraDuoPlus45:
					this.isProgrammExecuting1 = this.device.isProcessing(1, dataBuffer);
					this.isProgrammExecuting2 = this.device.isProcessing(2, dataBuffer);
					this.isProgrammExecuting3 = false;
					break;

				case UltraDuoPlus60:
					this.isProgrammExecuting3 = this.device.isLinkedMode(dataBuffer);
					if (!this.isProgrammExecuting3 && !this.isCombinedMode) { // outlet channel 1+2 combined 
						this.isProgrammExecuting1 = this.device.isProcessing(1, dataBuffer);
						this.isProgrammExecuting2 = this.device.isProcessing(2, dataBuffer);
					}
					break;
				}

				// check if device is ready for data capturing, discharge or charge allowed only
				// else wait for 180 seconds max. for actions
				if (this.isProgrammExecuting1 || this.isProgrammExecuting2 || this.isProgrammExecuting3) {
					switch (this.device.getDeviceTypeIdentifier()) {

					case Ultramat16S:
					case Ultramat18:
						ch1 = processDataChannel(1, recordSet1, this.recordSetKey1, dataBuffer, points1, this.measurementCount1, this.startCycleTime1);
						recordSet1 = (RecordSet) ch1[0];
						this.recordSetKey1 = (String) ch1[1];
						this.measurementCount1 = (Long) ch1[2];
						this.startCycleTime1 = (Long) ch1[3];
						break;

					case UltramatTrio14:
					case UltramatTrio16S:
						this.isProgrammExecuting1 = this.device.isProcessing(1, dataBuffer);
						this.isProgrammExecuting2 = this.device.isProcessing(2, dataBuffer);
						this.isProgrammExecuting3 = this.device.isProcessing(3, dataBuffer);
						break;

					case UltraDuoPlus45:
						if (this.isProgrammExecuting1) { // checks for processes active includes check state change waiting to discharge to charge
							ch1 = processDataChannel(1, recordSet1, this.recordSetKey1, dataBuffer, points1, this.measurementCount1, this.startCycleTime1);
							recordSet1 = (RecordSet) ch1[0];
							this.recordSetKey1 = (String) ch1[1];
							this.measurementCount1 = (Long) ch1[2];
							this.startCycleTime1 = (Long) ch1[3];
						}
						if (this.isProgrammExecuting2) { // checks for processes active includes check state change waiting to discharge to charge
							byte[] buffer = new byte[Math.abs(this.device.getDataBlockSize()) / 2];
							System.arraycopy(dataBuffer, 97, buffer, 11, 8); //sync to same value positions, only point array length is different
							buffer[19] = buffer[20] = 48; //blank out cycle number
							System.arraycopy(dataBuffer, 105, buffer, 21, 41);
							ch2 = processDataChannel(2, recordSet2, this.recordSetKey2, buffer, points2, this.measurementCount2, this.startCycleTime2);
							recordSet2 = (RecordSet) ch2[0];
							this.recordSetKey2 = (String) ch2[1];
							this.measurementCount2 = (Long) ch2[2];
							this.startCycleTime2 = (Long) ch2[3];
						}
						break;

					case UltraDuoPlus60:
						if (this.isProgrammExecuting3) { // checks for processes active includes check state change waiting to discharge to charge
							this.isCombinedMode = true;
							ch3 = processDataChannel(3, recordSet3, this.recordSetKey3, dataBuffer, points3, this.measurementCount3, this.startCycleTime3);
							recordSet3 = (RecordSet) ch3[0];
							this.recordSetKey3 = (String) ch3[1];
							this.measurementCount3 = (Long) ch3[2];
							this.startCycleTime3 = (Long) ch3[3];
						}
						else {
							if (this.isProgrammExecuting1) { // checks for processes active includes check state change waiting to discharge to charge
								ch1 = processDataChannel(1, recordSet1, this.recordSetKey1, dataBuffer, points1, this.measurementCount1, this.startCycleTime1);
								recordSet1 = (RecordSet) ch1[0];
								this.recordSetKey1 = (String) ch1[1];
								this.measurementCount1 = (Long) ch1[2];
								this.startCycleTime1 = (Long) ch1[3];
							}
							if (this.isProgrammExecuting2) { // checks for processes active includes check state change waiting to discharge to charge
								byte[] buffer = new byte[Math.abs(this.device.getDataBlockSize()) / 2];
								System.arraycopy(dataBuffer, buffer.length - 5, buffer, 0, buffer.length);
								ch2 = processDataChannel(2, recordSet2, this.recordSetKey2, buffer, points2, this.measurementCount2, this.startCycleTime2);
								recordSet2 = (RecordSet) ch2[0];
								this.recordSetKey2 = (String) ch2[1];
								this.measurementCount2 = (Long) ch2[2];
								this.startCycleTime2 = (Long) ch2[3];
							}
						}
						break;
					}
				}
				else { // no program is executing, wait for 180 seconds max. for actions
					this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI2200));
					log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "wait for device activation"); //$NON-NLS-1$

					if (recordSet1 != null && recordSet1.getRecordDataSize(true) > 5) { // record set has data points, save data and wait
						finalizeRecordSet(recordSet1.getName());
						this.isProgrammExecuting1 = false;
						recordSet1 = null;
						setRetryCounter(GathererThread.WAIT_TIME_RETRYS); // reset retry counter 180 sec
						this.application.openMessageDialogAsync(this.dialog != null ? this.dialog.getDialogShell() : null, Messages.getString(MessageIds.GDE_MSGI2204, new String[] { "1" })); //$NON-NLS-1$
					}
					if (recordSet2 != null && recordSet2.getRecordDataSize(true) > 5) { // record set has data points, save data and wait
						finalizeRecordSet(recordSet2.getName());
						this.isProgrammExecuting2 = false;
						recordSet2 = null;
						setRetryCounter(GathererThread.WAIT_TIME_RETRYS); // reset retry counter 180 sec
						this.application.openMessageDialogAsync(this.dialog != null ? this.dialog.getDialogShell() : null, Messages.getString(MessageIds.GDE_MSGI2204, new String[] { "2" })); //$NON-NLS-1$
					}
					if (recordSet3 != null && recordSet3.getRecordDataSize(true) > 5) { // record set has data points, save data and wait
						finalizeRecordSet(recordSet3.getName());
						this.isProgrammExecuting3 = false;
						recordSet3 = null;
						setRetryCounter(GathererThread.WAIT_TIME_RETRYS); // reset retry counter 180 sec
						this.application.openMessageDialogAsync(this.dialog != null ? this.dialog.getDialogShell() : null, Messages.getString(MessageIds.GDE_MSGI2204, new String[] { "3" })); //$NON-NLS-1$
					}
					if (0 == (setRetryCounter(getRetryCounter() - 1))) {
						log.log(java.util.logging.Level.FINE, "device activation timeout"); //$NON-NLS-1$
						this.application.openMessageDialogAsync(this.dialog != null ? this.dialog.getDialogShell() : null, Messages.getString(MessageIds.GDE_MSGI2203));
						stopDataGatheringThread(false, null);
					}
				}

			}
			catch (DataInconsitsentException e) {
				String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0036, new Object[] { this.getClass().getSimpleName(), $METHOD_NAME });
				if (recordSet1 != null) cleanup(recordSet1.getName(), message);
				if (recordSet2 != null) cleanup(recordSet2.getName(), message);
				if (recordSet3 != null) cleanup(recordSet3.getName(), message);
			}
			catch (Throwable e) {
				// this case will be reached while NiXx Akku discharge/charge/discharge cycle
				if (e instanceof TimeOutException) {
					if (recordSet1 != null) {
						finalizeRecordSet(recordSet1.getName());
						recordSet1 = null;
						WaitTimer.delay(1000);
					}
					if (recordSet2 != null) {
						finalizeRecordSet(recordSet2.getName());
						recordSet2 = null;
						WaitTimer.delay(1000);
					}
					if (recordSet3 != null) {
						finalizeRecordSet(recordSet3.getName());
						recordSet3 = null;
						WaitTimer.delay(1000);
					}
				}
				// this case will be reached while eStation program is started, checked and the check not asap committed, stop pressed
				else if (e instanceof TimeOutException && !this.isProgrammExecuting1 || !this.isProgrammExecuting2) {
					this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGI2200));
					log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "wait for device activation ..."); //$NON-NLS-1$
					if (0 == (setRetryCounter(getRetryCounter() - 1))) {
						log.log(java.util.logging.Level.FINE, "device activation timeout"); //$NON-NLS-1$
						this.application.openMessageDialogAsync(this.dialog.getDialogShell(), Messages.getString(MessageIds.GDE_MSGI2200));
						stopDataGatheringThread(false, null);
					}
				}
				// program end or unexpected exception occurred, stop data gathering to enable save data by user
				else {
					log.log(java.util.logging.Level.FINE, "program end detected"); //$NON-NLS-1$
					stopDataGatheringThread(true, e);
				}
			}
		}
		this.application.setStatusMessage(""); //$NON-NLS-1$
		log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "======> exit"); //$NON-NLS-1$
	}

	/**
	 * process a outlet channel
	 * @param number
	 * @param recordSet
	 * @param recordSetKey
	 * @param dataBuffer
	 * @param points
	 * @param measurementCount
	 * @param startCycleTime
	 * @param numCells
	 * @return object array with updated values {recordSet, recordSetKey, measurementCount, startCycleTime, numCells}
	 * @throws DataInconsitsentException
	 */
	private Object[] processDataChannel(int number, RecordSet recordSet, String recordSetKey, byte[] dataBuffer, int[] points, long measurementCount, long startCycleTime)
			throws DataInconsitsentException {
		final String $METHOD_NAME = "processOutlet"; //$NON-NLS-1$
		Object[] result = new Object[4];
		String processName = this.device.USAGE_MODE[this.device.getProcessingMode(dataBuffer)];

		Channel channel = this.channels.get(number);
		if (channel != null) {
			// check if a record set matching for re-use is available and prepare a new if required
			if (recordSet == null || !recordSetKey.contains(processName)) {
				this.application.setStatusMessage(""); //$NON-NLS-1$
				setRetryCounter(GathererThread.WAIT_TIME_RETRYS); // reset to 180 sec

				// record set does not exist or is outdated, build a new name and create
				StringBuilder extend = new StringBuilder();
				if (this.device.getProcessingMode(dataBuffer) < 5) {
					if (this.device.getProcessingType(dataBuffer).length() > 3 || this.device.getCycleNumber(number, dataBuffer) > 0) extend.append(GDE.STRING_BLANK_LEFT_BRACKET);
					if (this.device.getProcessingType(dataBuffer).length() > 3) extend.append(this.device.getProcessingType(dataBuffer));
					if (this.device.getCycleNumber(number, dataBuffer) > 0) {
						if (this.device.getProcessingType(dataBuffer).equals(Messages.getString(MessageIds.GDE_MSGT2302)))
							extend.append(GDE.STRING_COLON).append(this.device.getCycleNumber(number, dataBuffer));
						else if (this.device.getProcessingType(dataBuffer).length() > 3) 
							extend.append(GDE.STRING_MESSAGE_CONCAT);
						
						extend.append(Messages.getString(MessageIds.GDE_MSGT2302)).append(GDE.STRING_COLON).append(this.device.getCycleNumber(number, dataBuffer));
					}
					if (this.device.getProcessingType(dataBuffer).length() > 3 || this.device.getCycleNumber(number, dataBuffer) > 0) extend.append(GDE.STRING_RIGHT_BRACKET);
				}
				recordSetKey = channel.getNextRecordSetNumber() + GDE.STRING_RIGHT_PARENTHESIS_BLANK + processName + extend.toString();
				recordSetKey = recordSetKey.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetKey : recordSetKey.substring(0, RecordSet.MAX_NAME_LENGTH);

				channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, this.application.getActiveDevice(), channel.getNumber(), true, false));
				channel.applyTemplateBasics(recordSetKey);
				log.logp(java.util.logging.Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, recordSetKey + " created for channel " + channel.getName()); //$NON-NLS-1$
				recordSet = channel.get(recordSetKey);
				this.device.setTemperatureUnit(number, recordSet, dataBuffer); //°C or °F
				recordSet.setAllDisplayable();
				String description = recordSet.getRecordSetDescription() + GDE.LINE_SEPARATOR 
					+ "Firmware  : " + this.device.firmware //$NON-NLS-1$
					+ (this.device.getBatteryMemoryNumber(number, dataBuffer) > 1 ? "; Memory #" + this.device.getBatteryMemoryNumber(number, dataBuffer) : GDE.STRING_EMPTY); //$NON-NLS-1$
				try {
					if (this.device.ultraDuoPlusSetup != null && this.device.ultraDuoPlusSetup.getMemory().get(this.device.getBatteryMemoryNumber(number, dataBuffer)) != null) {
						String batteryMemoryName = this.device.ultraDuoPlusSetup.getMemory().get(this.device.getBatteryMemoryNumber(number, dataBuffer) - 1).getName();
						description = description + GDE.STRING_MESSAGE_CONCAT + batteryMemoryName;
						this.device.matchBatteryMemory2ObjectKey(batteryMemoryName);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					// ignore and do not append memory name
				}
				recordSet.setRecordSetDescription(description);
				channel.applyTemplate(recordSetKey, false);
				// switch the active record set if the current record set is child of active channel
				this.channels.switchChannel(channel.getNumber(), recordSetKey);
				channel.switchRecordSet(recordSetKey);
				measurementCount = 0;
				startCycleTime = 0;
			}

			// prepare the data for adding to record set
			this.isGatheredRecordSetVisible = recordSetKey.equals(channel.getActiveRecordSet().getName());
			long tmpCycleTime = System.nanoTime() / 1000000;
			if (measurementCount++ == 0) {
				startCycleTime = tmpCycleTime;
			}

			recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer), (tmpCycleTime - startCycleTime));
			log.logp(Level.TIME, GathererThread.$CLASS_NAME, $METHOD_NAME, "time = " + TimeLine.getFomatedTimeWithUnit(tmpCycleTime - startCycleTime)); //$NON-NLS-1$

			GathererThread.this.application.updateAllTabs(false);

			if (measurementCount < 3 || measurementCount % 10 == 0) {
				this.device.updateVisibilityStatus(recordSet, true);
			}
		}
		result[0] = recordSet;
		result[1] = recordSetKey;
		result[2] = measurementCount;
		result[3] = startCycleTime;
		return result;
	}

	/**
	 * stop the data gathering and check if reasonable data in record set to finalize or clear
	 * @param enableEndMessage
	 * @param throwable
	 */
	void stopDataGatheringThread(boolean enableEndMessage, Throwable throwable) {
		final String $METHOD_NAME = "stopDataGatheringThread"; //$NON-NLS-1$

		if (throwable != null) {
			log.logp(java.util.logging.Level.WARNING, GathererThread.$CLASS_NAME, $METHOD_NAME, throwable.getMessage(), throwable);
		}

		this.isCollectDataStopped = true;

		if (this.serialPort != null && this.serialPort.getXferErrors() > 0) {
			log.log(java.util.logging.Level.WARNING, "During complete data transfer " + this.serialPort.getXferErrors() + " number of errors occured!"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * close port, set isDisplayable according channel configuration and calculate slope
	 */
	void finalizeRecordSet(String recordSetKey) {
		//if (doClosePort && this.isPortOpenedByLiveGatherer && this.serialPort.isConnected()) this.serialPort.close();

		RecordSet tmpRecordSet = this.channels.getActiveChannel().getActiveRecordSet();
		if (tmpRecordSet != null) {
			this.device.updateVisibilityStatus(tmpRecordSet, true);
			this.device.makeInActiveDisplayable(tmpRecordSet);
			this.application.updateStatisticsData();
			this.application.updateDataTable(recordSetKey, false);

			log.log(Level.TIME, "set average time step msec = " + this.device.getAverageTimeStep_ms()); //$NON-NLS-1$
		}
	}

	/**
	 * cleanup all allocated resources and display the message
	 * @param this.recordSetKey
	 * @param message
	 * @param e
	 */
	void cleanup(String recordSetKey, final String message) {
		RecordSet activeRecordSet = this.channels.getActiveChannel().getActiveRecordSet();
		if (activeRecordSet != null) {
			activeRecordSet.clear();
			this.channels.getActiveChannel().remove(recordSetKey);
			if (Thread.currentThread().getId() == this.application.getThreadId()) {
				this.application.getMenuToolBar().updateRecordSetSelectCombo();
				this.application.updateStatisticsData();
				this.application.updateDataTable(recordSetKey, true);
				this.application.openMessageDialog(GathererThread.this.dialog.getDialogShell(), message);
			}
			else {
				final String useRecordSetKey = recordSetKey;
				GDE.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						GathererThread.this.application.getMenuToolBar().updateRecordSetSelectCombo();
						GathererThread.this.application.updateStatisticsData();
						GathererThread.this.application.updateDataTable(useRecordSetKey, true);
						GathererThread.this.application.openMessageDialog(GathererThread.this.dialog.getDialogShell(), message);
					}
				});
			}
		}
		else
			this.application.openMessageDialog(this.dialog.getDialogShell(), message);
	}

	/**
	 * @param enabled the isCollectDataStopped to set
	 */
	void setCollectDataStopped(boolean enabled) {
		this.isCollectDataStopped = enabled;
	}

	/**
	 * @return the isCollectDataStopped
	 */
	boolean isCollectDataStopped() {
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
