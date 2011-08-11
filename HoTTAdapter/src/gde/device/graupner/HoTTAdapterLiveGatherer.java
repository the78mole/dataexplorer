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

import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Thread implementation to gather data from HoTTAdapter device
 * @author Winfied Brügmann
 */
public class HoTTAdapterLiveGatherer extends Thread {
	final static Logger						log													= Logger.getLogger(HoTTAdapterLiveGatherer.class.getName());

	final DataExplorer						application;
	final HoTTAdapterSerialPort		serialPort;
	final HoTTAdapter							device;
	final HoTTAdapterDialog				dialog;
	final Channels								channels;
	Channel												channel;
	Integer												channelNumber;
	int														timeStep_ms									= 500;
	Timer													timer;
	TimerTask											timerTask;
	boolean												isTimerRunning							= false;
	boolean												isPortOpenedByLiveGatherer	= false;
	boolean												isSwitchedRecordSet					= false;
	boolean												isGatheredRecordSetVisible	= true;
	byte													sensorType									= (byte) 0x80;

	// offsets and factors are constant over thread live time
	final HashMap<String, Double>	calcValues									= new HashMap<String, Double>();

	/**
	 * @throws Exception 
	 */
	public HoTTAdapterLiveGatherer(DataExplorer currentApplication, HoTTAdapter useDevice, HoTTAdapterSerialPort useSerialPort, int channelConfigNumber, HoTTAdapterDialog useDialog) throws Exception {
		super("liveDataGatherer"); //$NON-NLS-1$
		this.application = currentApplication;
		this.device = useDevice;
		this.serialPort = useSerialPort;
		this.dialog = useDialog;
		this.channels = Channels.getInstance();
		this.channelNumber = channelConfigNumber;
		this.channel = this.channels.get(this.channelNumber);
	}

	@Override
	public void run() {
		try {
			if (!this.serialPort.isConnected()) {
				this.serialPort.open();
				this.isPortOpenedByLiveGatherer = true;
			}

			// get HoTTAdapter configuration for timeStep info
			byte[] sensorData = this.serialPort.getData(true);
			this.sensorType = sensorData[0];
			// timer interval
		}
		catch (SerialPortException e) {
			this.serialPort.close();
			this.application.openMessageDialogAsync(this.dialog.getDialogShell(),
					Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
			return;
		}
		catch (ApplicationConfigurationException e) {
			this.serialPort.close();
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					HoTTAdapterLiveGatherer.this.application.getDeviceSelectionDialog().open();
				}
			});
			return;
		}
		catch (Throwable t) {
			this.serialPort.close();
			log.log(Level.SEVERE, t.getMessage(), t);
			this.application.openMessageDialogAsync(this.dialog.getDialogShell(),
					Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { t.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + t.getMessage() }));
			return;
		}

		switch (this.sensorType) {
		case HoTTAdapter.SENSOR_TYPE_RECEIVER:
			this.channelNumber = 1;
			this.channels.switchChannel(this.channels.getChannelNames()[0]);
			break;
		case HoTTAdapter.SENSOR_TYPE_VARIO:
			this.channelNumber = 2;
			this.channels.switchChannel(this.channels.getChannelNames()[1]);
			break;
		case HoTTAdapter.SENSOR_TYPE_GPS:
			this.channelNumber = 3;
			this.channels.switchChannel(this.channels.getChannelNames()[2]);
			break;
		case HoTTAdapter.SENSOR_TYPE_GENERAL:
			this.channelNumber = 4;
			this.channels.switchChannel(this.channels.getChannelNames()[3]);
			break;
		case HoTTAdapter.SENSOR_TYPE_ELECTRIC:
			this.channelNumber = 5;
			this.channels.switchChannel(this.channels.getChannelNames()[4]);
			break;
		}

		// prepare timed data gatherer thread
		this.serialPort.isInterruptedByUser = false;
		int delay = 0;
		int period = this.timeStep_ms;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "timer period = " + period + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
		final String recordSetKey = this.channel.getNextRecordSetNumber() + this.device.getRecordSetStemName();

		this.channel = this.application.getActiveChannel();
		this.channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, this.device, this.channelNumber, true, false));
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, recordSetKey + " created for channel " + this.channel.getName()); //$NON-NLS-1$
		final RecordSet recordSet = this.channel.get(recordSetKey);
		this.channel.applyTemplateBasics(recordSetKey);
		//this.device.updateInitialRecordSetComment(recordSet);
		recordSet.setTimeStep_ms(this.timeStep_ms);
		final int[] points = new int[recordSet.size()];
		final HoTTAdapter usedDevice = this.device;

		if (!this.serialPort.isInterruptedByUser) {
			this.timer = new Timer();
			this.timerTask = new TimerTask() {
				long	measurementCount	= 0;

				@Override
				public void run() {
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "====> entry"); //$NON-NLS-1$
					try {
						if (HoTTAdapterLiveGatherer.this.isTimerRunning) {
							// prepare the data for adding to record set
							if (log.isLoggable(Level.FINE))
								log.log(Level.FINE, "recordSetKey = " + recordSetKey + " channelKonfigKey = " + recordSet.getChannelConfigName()); //$NON-NLS-1$ //$NON-NLS-2$

							// build the point array according curves from record set
							byte[] dataBuffer = HoTTAdapterLiveGatherer.this.serialPort.getData(true);

							recordSet.addPoints(usedDevice.convertDataBytes(points, dataBuffer));

							// switch the active record set if the current record set is child of active channel
							if (!HoTTAdapterLiveGatherer.this.isSwitchedRecordSet && HoTTAdapterLiveGatherer.this.channel.getName().equals(HoTTAdapterLiveGatherer.this.channels.getActiveChannel().getName())) {
								HoTTAdapterLiveGatherer.this.channel.applyTemplateBasics(recordSetKey);
								HoTTAdapterLiveGatherer.this.application.getMenuToolBar().addRecordSetName(recordSetKey);
								HoTTAdapterLiveGatherer.this.channels.getActiveChannel().switchRecordSet(recordSetKey);
								HoTTAdapterLiveGatherer.this.isSwitchedRecordSet = true;
							}

							if (++this.measurementCount % 5 == 0) {
								HoTTAdapterLiveGatherer.this.device.updateVisibilityStatus(recordSet, true);
							}
							if (recordSet.isChildOfActiveChannel() && recordSet.equals(HoTTAdapterLiveGatherer.this.channels.getActiveChannel().getActiveRecordSet())) {
								HoTTAdapterLiveGatherer.this.application.updateAllTabs(false);
							}
						}
					}
					catch (DataInconsitsentException e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0028, new Object[] { e.getClass().getSimpleName(), e.getMessage() });
						cleanup(recordSetKey, message);
					}
					catch (TimeOutException e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() });
						//TODO + System.getProperty("line.separator") + Messages.getString(MessageIds.GDE_MSGW1301); //$NON-NLS-1$ 
						cleanup(recordSetKey, message);
					}
					catch (IOException e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() });
						//TODO + System.getProperty("line.separator") + Messages.getString(MessageIds.GDE_MSGW1301); //$NON-NLS-1$ 
						cleanup(recordSetKey, message);
					}
					catch (Throwable e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						String message = e.getClass().getSimpleName() + " - " + e.getMessage(); //$NON-NLS-1$ 
						cleanup(recordSetKey, message);
					}
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "======> exit"); //$NON-NLS-1$
				}
			};

			// start the prepared timer thread within the live data gatherer thread
			this.timer.scheduleAtFixedRate(this.timerTask, delay, period);
			this.isTimerRunning = true;
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "exit"); //$NON-NLS-1$
		}
		else {
			cleanup(recordSetKey, null);
		}
	}

	/**
	 * close port, set isDisplayable according channel configuration and calculate slope
	 * @param recordSetKey
	 */
	public void finalizeRecordSet(String recordSetKey) {
		if (this.isPortOpenedByLiveGatherer) this.serialPort.close();

		RecordSet recordSet = this.channel.get(recordSetKey);
		this.device.updateVisibilityStatus(recordSet, false);
		this.device.makeInActiveDisplayable(recordSet);
		this.application.updateStatisticsData();
		this.application.updateDataTable(recordSetKey, false);
	}

	/**
	 * stop the timer task thread, this tops data capturing
	 * waits for all running timers tasks are ended before return 
	 */
	public void stopTimerThread() {
		if (this.timerTask != null) this.timerTask.cancel();
		if (this.timer != null) {
			this.timer.cancel();
			this.timer.purge();
		}
		this.isTimerRunning = false;
		if (this.isPortOpenedByLiveGatherer) this.serialPort.close();
	}

	/**
	 * cleanup all allocated resources and display the message
	 * @param recordSetKey
	 * @param message
	 */
	void cleanup(final String recordSetKey, String message) {
		boolean isErrorState = this.isTimerRunning;
		this.stopTimerThread();
		if (this.isPortOpenedByLiveGatherer) this.serialPort.close();
		this.channel.get(recordSetKey).clear();
		this.channel.remove(recordSetKey);
		this.application.getMenuToolBar().updateRecordSetSelectCombo();
		this.application.updateStatisticsData();
		this.application.updateDataTable(recordSetKey, true);
		//TODO this.device.getDialog().resetButtons();
		if (message != null && message.length() > 5 && isErrorState) {
			this.application.openMessageDialog(this.dialog.getDialogShell(), message);
		}
	}
}
