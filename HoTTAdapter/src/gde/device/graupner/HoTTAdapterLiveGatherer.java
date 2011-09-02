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
import gde.device.graupner.hott.MessageIds;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;

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
	int														timeStep_ms									= 1000;
	boolean												isTimerTaskActive						= true;
	boolean												isPortOpenedByLiveGatherer	= false;
	boolean												isSwitchedRecordSet					= false;
	boolean												isGatheredRecordSetVisible	= true;
	byte													sensorType									= (byte) 0x80;
	int														queryGapTime_ms							= 30;
	int														sensorDetectError						= 0;

	// offsets and factors are constant over thread live time
	final HashMap<String, Double>	calcValues									= new HashMap<String, Double>();

	/**
	 * @throws Exception 
	 */
	public HoTTAdapterLiveGatherer(DataExplorer currentApplication, HoTTAdapter useDevice, HoTTAdapterSerialPort useSerialPort, HoTTAdapterDialog useDialog) throws Exception {
		super("liveDataGatherer"); //$NON-NLS-1$
		this.application = currentApplication;
		this.device = useDevice;
		this.serialPort = useSerialPort;
		this.dialog = useDialog;
		this.channels = Channels.getInstance();
		this.channel = this.channels.get(this.channelNumber);
	}

	@Override
	public void run() {
		try {
			if (!this.serialPort.isConnected()) {
				this.serialPort.open();
				this.isPortOpenedByLiveGatherer = true;
			}
			this.serialPort.isInterruptedByUser = false;
			long startTime = new Date().getTime();
			//detect sensor type running a max of 3 times getting the sensor data
			for (int i = 0; i < 4; i++) {
				HoTTAdapter.isSensorType[i] = true;
			}
			for (int i = 0; i < 3; i++) {
				try {
					detectSensorType(HoTTAdapter.isSensorType);
				if (HoTTAdapter.isSensorType[0] == false || HoTTAdapter.isSensorType[1] == false || HoTTAdapter.isSensorType[2] == false || HoTTAdapter.isSensorType[3] == false)
					break;
				}
				catch (Exception e) {
					log.log(Level.WARNING, e.getMessage(), e);
					if (++sensorDetectError == 3)
						this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(MessageIds.GDE_MSGW2400));
				}
			}
			//no sensor type detected, seams only receiver is connected
			if (HoTTAdapter.isSensorType[0] == true && HoTTAdapter.isSensorType[1] == true && HoTTAdapter.isSensorType[2] == true && HoTTAdapter.isSensorType[3] == true) {
				for (int i = 0; i < 4; i++) {
					HoTTAdapter.isSensorType[i] = false;
				}
				serialPort.setSensorType(serialPort.isProtocolTypeLegacy ? HoTTAdapter.SENSOR_TYPE_RECEIVER_L : HoTTAdapter.SENSOR_TYPE_RECEIVER);
			}
		
			log.log(Level.FINE, "detecting sensor type takes " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
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
		this.application.setStatusMessage(GDE.STRING_EMPTY);
		
		//0=isVario, 1=isGPS, 2=isGeneral, 3=isElectric
		if (HoTTAdapter.isSensorType[0] == true) {
			this.dialog.selectTab(this.channelNumber = 2);
			this.channels.switchChannel(this.channels.getChannelNames()[1]);
		}
		else if (HoTTAdapter.isSensorType[1] == true) {
			this.dialog.selectTab(this.channelNumber = 3);
			this.channels.switchChannel(this.channels.getChannelNames()[2]);
		}
		else if (HoTTAdapter.isSensorType[2] == true) {
			this.dialog.selectTab(this.channelNumber = 4);
			this.channels.switchChannel(this.channels.getChannelNames()[3]);
		}
		else if (HoTTAdapter.isSensorType[3] == true) {
			this.dialog.selectTab(this.channelNumber = 5);
			this.channels.switchChannel(this.channels.getChannelNames()[4]);
		}
		else {
			this.dialog.selectTab(this.channelNumber = 1);
			this.channels.switchChannel(this.channels.getChannelNames()[0]);
		}

		this.channel = this.application.getActiveChannel();
		final String recordSetKey = this.channel.getNextRecordSetNumber() + this.device.getRecordSetStemName();
		this.channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, this.device, this.channelNumber, true, false));
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, recordSetKey + " created for channel " + this.channel.getName()); //$NON-NLS-1$
		final RecordSet recordSet = this.channel.get(recordSetKey);
		//this.channel.applyTemplateBasics(recordSetKey);
		//this.device.updateInitialRecordSetComment(recordSet);
		//recordSet.setTimeStep_ms(this.timeStep_ms);
		final int[] points = new int[recordSet.size()];
		final HoTTAdapter usedDevice = this.device;

		long	measurementCount	= 0;
		final long startTime = System.nanoTime()/1000000;
		long lastTimeStamp = startTime, delayTime = 0;
		while (!this.serialPort.isInterruptedByUser) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "====> entry"); //$NON-NLS-1$
				try {
					if (HoTTAdapterLiveGatherer.this.isTimerTaskActive) {
						// prepare the data for adding to record set
						if (log.isLoggable(Level.FINE))
							log.log(Level.FINE, "recordSetKey = " + recordSetKey + " channelKonfigKey = " + recordSet.getChannelConfigName()); //$NON-NLS-1$ //$NON-NLS-2$

						// build the point array according curves from record set
						if (HoTTAdapterLiveGatherer.this.serialPort.isProtocolTypeLegacy) {
							HoTTAdapterLiveGatherer.this.serialPort.getData(true);
							WaitTimer.delay(queryGapTime_ms);
//							HoTTAdapterLiveGatherer.this.serialPort.getData(true);
//							WaitTimer.delay(queryGapTime_ms);
							recordSet.addPoints(usedDevice.convertDataBytes(points, HoTTAdapterLiveGatherer.this.serialPort.getData(true)), System.nanoTime()/1000000 - startTime);
						}
						else {
							HoTTAdapterLiveGatherer.this.serialPort.getData();
							WaitTimer.delay(queryGapTime_ms);
//							HoTTAdapterLiveGatherer.this.serialPort.getData();
//							WaitTimer.delay(queryGapTime_ms);
							recordSet.addPoints(usedDevice.convertDataBytes(points, HoTTAdapterLiveGatherer.this.serialPort.getData()), System.nanoTime()/1000000 - startTime);
						}

						// switch the active record set if the current record set is child of active channel
						if (!HoTTAdapterLiveGatherer.this.isSwitchedRecordSet && HoTTAdapterLiveGatherer.this.channel.getName().equals(HoTTAdapterLiveGatherer.this.channels.getActiveChannel().getName())) {
							HoTTAdapterLiveGatherer.this.channel.applyTemplateBasics(recordSetKey);
							HoTTAdapterLiveGatherer.this.application.getMenuToolBar().addRecordSetName(recordSetKey);
							HoTTAdapterLiveGatherer.this.channels.getActiveChannel().switchRecordSet(recordSetKey);
							HoTTAdapterLiveGatherer.this.isSwitchedRecordSet = true;
						}

						if (++measurementCount % 5 == 0) {
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
					log.log(Level.WARNING, e.getMessage());
					serialPort.addTimeoutError();
					application.setStatusMessage(Messages.getString(gde.messages.MessageIds.GDE_MSGW0045, new Object[] { e.getClass().getSimpleName(), " = " + serialPort.getTimeoutErrors() + "; xferErrors = " + serialPort.getXferErrors() }), SWT.COLOR_RED); 
				}
				catch (IOException e) {
					log.log(Level.WARNING, e.getMessage());
					application.openMessageDialogAsync(Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() })); 
				}
				catch (Throwable e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					application.openMessageDialogAsync(e.getClass().getSimpleName() + " - " + e.getMessage()); //$NON-NLS-1$
					finalizeRecordSet(recordSetKey);
				}
				WaitTimer.delay(100); //make sure we have such a pause while receiving data even we have 
				
				long deltatime = System.nanoTime()/1000000 - lastTimeStamp;
				delayTime = timeStep_ms - deltatime;

				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "======> delay time = " + delayTime); //$NON-NLS-1$
				if (delayTime > 0) WaitTimer.delay(delayTime);
				lastTimeStamp = System.nanoTime()/1000000;
			}
			finalizeRecordSet(recordSetKey);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "exit"); //$NON-NLS-1$
		}

	/**
	 * close port, set isDisplayable according channel configuration and calculate slope
	 * @param recordSetKey
	 */
	public void finalizeRecordSet(String recordSetKey) {
		this.serialPort.isInterruptedByUser = true;
		this.serialPort.close();
		RecordSet recordSet = this.channel.get(recordSetKey);
		this.device.updateVisibilityStatus(recordSet, false);
		this.device.makeInActiveDisplayable(recordSet);
		this.application.updateStatisticsData();
		this.application.updateDataTable(recordSetKey, false);
		this.device.getDialog().resetButtons();
	}

	/**
	 * cleanup all allocated resources and display the message
	 * @param recordSetKey
	 * @param message
	 */
	void cleanup(final String recordSetKey, String message) {
		this.serialPort.isInterruptedByUser = true;
		this.serialPort.close();
		this.channel.get(recordSetKey).clear();
		this.channel.remove(recordSetKey);
		this.application.getMenuToolBar().updateRecordSetSelectCombo();
		this.application.updateStatisticsData();
		this.application.updateDataTable(recordSetKey, true);
		this.device.getDialog().resetButtons();
		if (message != null && message.length() > 5) {
			this.application.openMessageDialog(this.dialog.getDialogShell(), message);
		}
	}

	/**
	 * get and detect Sensor data, retrieves data only on not detected sensors driven by boolean[] isSensorType
	 * @param isSensorType
	 * @return points integer array of data points, to enable 3 decimal digits value is multiplied by 1000
	 * @throws Exception 
	 */
	private void detectSensorType(boolean isSensorType[]) throws Exception {
		
		if (serialPort.isProtocolTypeLegacy) {
			log.log(Level.FINE, "------------ Receiver");
			serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER_L);
			serialPort.getData(true);
			Thread.sleep(queryGapTime_ms);
			serialPort.getData(true);
			Thread.sleep(queryGapTime_ms);
			if (isSensorType[0]) {
				log.log(Level.FINE, "------------ Vario");
				serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_L);
				serialPort.getData(true);
				Thread.sleep(queryGapTime_ms);
				serialPort.getData(true);
				Thread.sleep(queryGapTime_ms);
				byte[] dataBuffer = serialPort.getData(true);
				if (DataParser.parse2Short(dataBuffer, 16) != 0 || dataBuffer[22] != 0) 
					isSensorType[1] = isSensorType[2] = isSensorType[3] = false;
				Thread.sleep(queryGapTime_ms);
			}
			if (isSensorType[1]) {
				log.log(Level.FINE, "------------ GPS");
				serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_L);
				serialPort.getData(true);
				Thread.sleep(queryGapTime_ms);
				serialPort.getData(true);
				Thread.sleep(queryGapTime_ms);
				byte[] dataBuffer = serialPort.getData(true);
				if (dataBuffer[37] == 0x01 || (dataBuffer[20] != 0 && dataBuffer[21] != 0 && dataBuffer[25] != 0 && dataBuffer[26] != 0)) 
					isSensorType[0] = isSensorType[2] = isSensorType[3] = false;
				Thread.sleep(queryGapTime_ms);
			}
			if (isSensorType[2]) {
				log.log(Level.FINE, "------------ General");
				serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_L);
				serialPort.getData(true);
				Thread.sleep(queryGapTime_ms);
				serialPort.getData(true);
				Thread.sleep(queryGapTime_ms);
				byte[] dataBuffer = serialPort.getData(true);
				if (DataParser.parse2Short(dataBuffer, 40) != 0) 
					isSensorType[0] = isSensorType[1] = isSensorType[3] = false;
				Thread.sleep(queryGapTime_ms);
			}
			if (isSensorType[3]) {
				log.log(Level.FINE, "------------ Electric");
				serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_L);
				serialPort.getData(true);
				Thread.sleep(queryGapTime_ms);
				serialPort.getData(true);
				Thread.sleep(queryGapTime_ms);
				byte[] dataBuffer = serialPort.getData(true);
				if (DataParser.parse2Short(dataBuffer, 40) != 0) 
					isSensorType[0] = isSensorType[1] = isSensorType[2] = false;
				Thread.sleep(queryGapTime_ms);
			}
		}
		else {
			log.log(Level.FINE, "------------ Receiver");
			serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER);
			serialPort.getData();
			Thread.sleep(queryGapTime_ms);
			serialPort.getData();
			Thread.sleep(queryGapTime_ms);
			if (isSensorType[0]) {
				log.log(Level.FINE, "------------ Vario");
				serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO);
				serialPort.getData();
				Thread.sleep(queryGapTime_ms);
				serialPort.getData();
				Thread.sleep(queryGapTime_ms);
				byte[] dataBuffer = serialPort.getData();
				if (DataParser.parse2Short(dataBuffer, 10) != 0 || dataBuffer[16] != 0)
					isSensorType[1] = isSensorType[2] = isSensorType[3] = false;
				Thread.sleep(queryGapTime_ms);
			}
			if (isSensorType[1]) {
				log.log(Level.FINE, "------------ GPS");
				serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS);
				serialPort.getData();
				Thread.sleep(queryGapTime_ms);
				serialPort.getData();
				Thread.sleep(queryGapTime_ms);
				byte[] dataBuffer = serialPort.getData();
				if (dataBuffer[31] != 0 || (dataBuffer[16] != 0 && dataBuffer[17] != 0 && dataBuffer[20] != 0 && dataBuffer[21] != 0)) 
					isSensorType[0] = isSensorType[2] = isSensorType[3] = false;
				Thread.sleep(queryGapTime_ms);
			}
			if (isSensorType[2]) {
				log.log(Level.FINE, "------------ General");
				serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL);
				serialPort.getData();
				Thread.sleep(queryGapTime_ms);
				serialPort.getData();
				Thread.sleep(queryGapTime_ms);
				if (DataParser.parse2Short(serialPort.getData(), 36) != 0)
					isSensorType[0] = isSensorType[1] = isSensorType[3] = false;
				Thread.sleep(queryGapTime_ms);
			}
			if (isSensorType[3]) {
				log.log(Level.FINE, "------------ Electric");
				serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC);
				serialPort.getData();
				Thread.sleep(queryGapTime_ms);
				serialPort.getData();
				Thread.sleep(queryGapTime_ms);
				if (DataParser.parse2Short(serialPort.getData(), 50) != 0)
					isSensorType[0] = isSensorType[1] = isSensorType[2] = false;
				Thread.sleep(queryGapTime_ms);
			}
		}
	}
	
}
