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
    
    Copyright (c) 2008,2009,2010,2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.graupner.hott.MessageIds;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.io.DataParser;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
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
	int														timeStep_ms									= 1000;
	boolean												isPortOpenedByLiveGatherer	= false;
	boolean												isSwitchedRecordSet					= false;
	boolean												isGatheredRecordSetVisible	= true;
	byte													sensorType									= (byte) 0x80;

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
	}

	@Override
	public void run() {
		int recordSetNumber = this.channels.get(1).maxSize() + 1;
		RecordSet recordSetReceiver = null, recordSetVario = null, recordSetGPS = null, recordSetGeneral = null, recordSetElectric = null;
		int[] pointsReceiver = null, pointsVario = null, pointsGPS = null, pointsGeneral = null, pointsElectric = null;
		HoTTAdapter.recordSets.clear();
		try {
			if (!this.serialPort.isConnected()) {
				this.serialPort.open();
				this.isPortOpenedByLiveGatherer = true;
			}
			this.serialPort.isInterruptedByUser = false;
			long startTime = new Date().getTime();
			//detect sensor type running a max of 3 times getting the sensor data
			for (int i = 0; i < 4; i++) {
				HoTTAdapter.isSensorType[i] = false;
			}

			//detect master slave mode, only in slave mode data are written to receive buffer without query 
			WaitTimer.delay(1000);
			if (this.serialPort.cleanInputStream() > 2)
				HoTTAdapter.IS_SLAVE_MODE = true;
			else
				HoTTAdapter.IS_SLAVE_MODE = false;
			HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "HoTTAdapter.IS_SLAVE_MODE = " + HoTTAdapter.IS_SLAVE_MODE);

			for (int i = 0; i < 3; i++) {
				try {
					detectSensorType(HoTTAdapter.isSensorType);
					if (HoTTAdapter.isSensorType[0] == true || HoTTAdapter.isSensorType[1] == true || HoTTAdapter.isSensorType[2] == true || HoTTAdapter.isSensorType[3] == true) break;
				}
				catch (Exception e) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
				}
			}

			StringBuilder sb = new StringBuilder();
			boolean[] tmpSensorType = HoTTAdapter.isSensorType.clone();
			//0=isVario, 1=isGPS, 2=isGeneral, 3=isElectric
			for (int i = HoTTAdapter.isSensorType.length - 1; i >= 0; i--) {
				if (sb.length() == 0 && tmpSensorType[i]) {
					sb.append(tmpSensorType[3] ? ">>>Electric<<<" : tmpSensorType[2] ? ">>>General<<<" : tmpSensorType[1] ? ">>>GPS<<<" : tmpSensorType[0] ? ">>>Vario<<<" : "");
					tmpSensorType[i] = false;
				}
				else if (tmpSensorType[i]) {
					sb.append(GDE.STRING_MESSAGE_CONCAT)
							.append(tmpSensorType[3] ? ">>>Electric<<<" : tmpSensorType[2] ? ">>>General<<<" : tmpSensorType[1] ? ">>>GPS<<<" : tmpSensorType[0] ? ">>>Vario<<<" : "");
					tmpSensorType[i] = false;
				}
			}
			if (sb.length() == 0) sb.append(">>>Receiver<<<");
			HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.INFO, sb.toString() + ", detecting sensor type takes " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));

			//no sensor type detected, seams only receiver is connected
			if (HoTTAdapter.isSensorType[0] == false && HoTTAdapter.isSensorType[1] == false && HoTTAdapter.isSensorType[2] == false && HoTTAdapter.isSensorType[3] == false) {
				this.serialPort.setSensorType(this.serialPort.protocolType.ordinal() < 2 ? HoTTAdapter.SENSOR_TYPE_RECEIVER_19200 : HoTTAdapter.SENSOR_TYPE_RECEIVER_115200);
				this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(MessageIds.GDE_MSGW2400));
			}
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
				@Override
				public void run() {
					HoTTAdapterLiveGatherer.this.application.getDeviceSelectionDialog().open();
				}
			});
			return;
		}
		catch (Throwable t) {
			this.serialPort.close();
			HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.SEVERE, t.getMessage(), t);
			this.application.openMessageDialogAsync(this.dialog.getDialogShell(),
					Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { t.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + t.getMessage() }));
			return;
		}
		this.application.setStatusMessage(GDE.STRING_EMPTY);

		this.channel = this.application.getActiveChannel();
		String recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + "live" + GDE.STRING_RIGHT_BRACKET;
		String recordSetKey = this.channel.getNextRecordSetNumber() + this.device.getRecordSetStemName();
		;

		//receiver is always - might be empty in slave modus
		if (!HoTTAdapter.IS_SLAVE_MODE || HoTTAdapter.isSensorType[0] == false && HoTTAdapter.isSensorType[1] == false && HoTTAdapter.isSensorType[2] == false && HoTTAdapter.isSensorType[3] == false) {
			this.dialog.selectTab(1);
			this.channel = this.channels.get(1);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.RECEIVER.value() + recordSetNameExtend;
			recordSetReceiver = RecordSet.createRecordSet(recordSetKey, this.device, 1, true, true);
			this.channel.put(recordSetKey, recordSetReceiver);
			HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.RECEIVER.value(), recordSetReceiver);
			this.channel.applyTemplate(recordSetKey, true);
			pointsReceiver = new int[recordSetReceiver.size()];
		}
		//0=isVario, 1=isGPS, 2=isGeneral, 3=isElectric
		if (HoTTAdapter.isSensorType[0]) {
			this.dialog.selectTab(2);
			this.channel = this.channels.get(2);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.VARIO.value() + recordSetNameExtend;
			recordSetVario = RecordSet.createRecordSet(recordSetKey, this.device, 2, true, true);
			this.channel.put(recordSetKey, recordSetVario);
			HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.VARIO.value(), recordSetVario);
			this.channel.applyTemplate(recordSetKey, true);
			pointsVario = new int[recordSetVario.size()];
		}
		if (HoTTAdapter.isSensorType[1]) {
			this.dialog.selectTab(3);
			this.channel = this.channels.get(3);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GPS.value() + recordSetNameExtend;
			recordSetGPS = RecordSet.createRecordSet(recordSetKey, this.device, 3, true, true);
			this.channel.put(recordSetKey, recordSetGPS);
			HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GPS.value(), recordSetGPS);
			this.channel.applyTemplate(recordSetKey, true);
			pointsGPS = new int[recordSetGPS.size()];
		}
		if (HoTTAdapter.isSensorType[2]) {
			this.dialog.selectTab(4);
			this.channel = this.channels.get(4);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GENRAL.value() + recordSetNameExtend;
			recordSetGeneral = RecordSet.createRecordSet(recordSetKey, this.device, 4, true, true);
			this.channel.put(recordSetKey, recordSetGeneral);
			HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GENRAL.value(), recordSetGeneral);
			this.channel.applyTemplate(recordSetKey, true);
			pointsGeneral = new int[recordSetGeneral.size()];
		}
		if (HoTTAdapter.isSensorType[3]) {
			this.dialog.selectTab(5);
			this.channel = this.channels.get(5);
			recordSetKey = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ELECTRIC.value() + recordSetNameExtend;
			recordSetElectric = RecordSet.createRecordSet(recordSetKey, this.device, 5, true, true);
			this.channel.put(recordSetKey, recordSetElectric);
			HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.ELECTRIC.value(), recordSetElectric);
			this.channel.applyTemplate(recordSetKey, true);
			pointsElectric = new int[recordSetElectric.size()];
		}
		this.application.getMenuToolBar().updateChannelSelector();
		this.application.getMenuToolBar().updateRecordSetSelectCombo();
		this.channels.switchChannel(this.channel.getName());
		this.channel.switchRecordSet(recordSetKey);

		Vector<Integer> queryRing = new Vector<Integer>();
		for (int i = 0; i < HoTTAdapter.isSensorType.length; ++i) {
			if (HoTTAdapter.isSensorType[i]) queryRing.add(i);
		}
		long measurementCount = 0;
		final long startTime = System.nanoTime() / 1000000;
		while (!this.serialPort.isInterruptedByUser) {
			if (HoTTAdapterLiveGatherer.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "====> entry"); //$NON-NLS-1$
			try {
				// prepare the data and add to record set
				// build the point array according curves from record set
				switch (HoTTAdapterLiveGatherer.this.serialPort.protocolType) {
				case TYPE_19200_L:
				case TYPE_19200_N:
					if (!HoTTAdapter.IS_SLAVE_MODE || HoTTAdapter.isSensorType[0] == false && HoTTAdapter.isSensorType[1] == false && HoTTAdapter.isSensorType[2] == false
							&& HoTTAdapter.isSensorType[3] == false) {
						//always gather receiver data first, anserRx are used to fill RXSQ, VoltageRx and TemperatureRx
						if (recordSetReceiver != null) {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER_19200);
							//HoTTAdapterLiveGatherer.this.serialPort.getData(true);
							//WaitTimer.delay(HoTTAdapter.queryGapTime_ms);
							HoTTAdapterLiveGatherer.this.serialPort.getData(true);
							WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
							recordSetReceiver.addPoints(this.device.convertDataBytes(pointsReceiver, HoTTAdapterLiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
						}
					}
					if (recordSetElectric != null && queryRing.firstElement() == 3) {
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200);
						HoTTAdapterLiveGatherer.this.serialPort.getData(false);
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						HoTTAdapterLiveGatherer.this.serialPort.getData(true);
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						recordSetElectric.addPoints(this.device.convertDataBytes(pointsElectric, HoTTAdapterLiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
					}
					if (recordSetGeneral != null && queryRing.firstElement() == 2) {
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_19200);
						HoTTAdapterLiveGatherer.this.serialPort.getData(false);
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						HoTTAdapterLiveGatherer.this.serialPort.getData(true);
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						recordSetGeneral.addPoints(this.device.convertDataBytes(pointsGeneral, HoTTAdapterLiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
					}
					if (recordSetGPS != null && queryRing.firstElement() == 1) {
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_19200);
						HoTTAdapterLiveGatherer.this.serialPort.getData(false);
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						HoTTAdapterLiveGatherer.this.serialPort.getData(true);
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						recordSetGPS.addPoints(this.device.convertDataBytes(pointsGPS, HoTTAdapterLiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
					}
					if (recordSetVario != null && queryRing.firstElement() == 0) {
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_19200);
						HoTTAdapterLiveGatherer.this.serialPort.getData(false);
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						HoTTAdapterLiveGatherer.this.serialPort.getData(true);
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						recordSetVario.addPoints(this.device.convertDataBytes(pointsVario, HoTTAdapterLiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
					}
					break;
				case TYPE_115200:
					if (!HoTTAdapter.IS_SLAVE_MODE || HoTTAdapter.isSensorType[0] == false && HoTTAdapter.isSensorType[1] == false && HoTTAdapter.isSensorType[2] == false
							&& HoTTAdapter.isSensorType[3] == false) {
						//always gather receiver data first, anserRx are used to fill RXSQ, VoltageRx and TemperatureRx
						if (recordSetReceiver != null) {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER_115200);
							//HoTTAdapterLiveGatherer.this.serialPort.getData(0);
							//WaitTimer.delay(HoTTAdapter.queryGapTime_ms);
							HoTTAdapterLiveGatherer.this.serialPort.getData(0);
							WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
							recordSetReceiver.addPoints(this.device.convertDataBytes(pointsReceiver, HoTTAdapterLiveGatherer.this.serialPort.getData(1)), System.nanoTime() / 1000000 - startTime);
						}
					}
					if (recordSetElectric != null && queryRing.firstElement() == 3) {
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200);
						//HoTTAdapterLiveGatherer.this.serialPort.getData(0);
						//WaitTimer.delay(HoTTAdapter.queryGapTime_ms);
						HoTTAdapterLiveGatherer.this.serialPort.getData(0);
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						recordSetElectric.addPoints(this.device.convertDataBytes(pointsElectric, HoTTAdapterLiveGatherer.this.serialPort.getData(0)), System.nanoTime() / 1000000 - startTime);
					}
					if (recordSetGeneral != null && queryRing.firstElement() == 2) {
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_115200);
						//HoTTAdapterLiveGatherer.this.serialPort.getData(0);
						//WaitTimer.delay(HoTTAdapter.queryGapTime_ms);
						HoTTAdapterLiveGatherer.this.serialPort.getData(0);
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						recordSetGeneral.addPoints(this.device.convertDataBytes(pointsGeneral, HoTTAdapterLiveGatherer.this.serialPort.getData(0)), System.nanoTime() / 1000000 - startTime);
					}
					if (recordSetGPS != null && queryRing.firstElement() == 1) {
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_115200);
						//HoTTAdapterLiveGatherer.this.serialPort.getData(0);
						//WaitTimer.delay(HoTTAdapter.queryGapTime_ms);
						HoTTAdapterLiveGatherer.this.serialPort.getData(0);
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						recordSetGPS.addPoints(this.device.convertDataBytes(pointsGPS, HoTTAdapterLiveGatherer.this.serialPort.getData(0)), System.nanoTime() / 1000000 - startTime);
					}
					if (recordSetVario != null && queryRing.firstElement() == 0) {
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_115200);
						//HoTTAdapterLiveGatherer.this.serialPort.getData(0);
						//WaitTimer.delay(HoTTAdapter.queryGapTime_ms);
						HoTTAdapterLiveGatherer.this.serialPort.getData(0);
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						recordSetVario.addPoints(this.device.convertDataBytes(pointsVario, HoTTAdapterLiveGatherer.this.serialPort.getData(0)), System.nanoTime() / 1000000 - startTime);
					}
					break;
				}
				if (queryRing.size() > 1) queryRing.add(queryRing.remove(0));

				if (++measurementCount % 5 == 0) {
					for (RecordSet recordSet : HoTTAdapter.recordSets.values()) {
						this.device.updateVisibilityStatus(recordSet, true);
					}
				}
				//if (recordSet.isChildOfActiveChannel() && recordSet.equals(HoTTAdapterLiveGatherer.this.channels.getActiveChannel().getActiveRecordSet())) {
				HoTTAdapterLiveGatherer.this.application.updateAllTabs(false);
				//}
			}
			catch (DataInconsitsentException e) {
				HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
				String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0028, new Object[] { e.getClass().getSimpleName(), e.getMessage() });
				for (RecordSet recordSet : HoTTAdapter.recordSets.values()) {
					cleanup(recordSet.getName(), message);
				}
			}
			catch (TimeOutException e) {
				HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.WARNING, e.getMessage());
				this.serialPort.addTimeoutError();
				this.application.setStatusMessage(
						Messages.getString(gde.messages.MessageIds.GDE_MSGW0045,
								new Object[] { e.getClass().getSimpleName(), this.serialPort.getTimeoutErrors() + "; xferErrors = " + this.serialPort.getXferErrors() }), SWT.COLOR_RED);
				WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS); //give time to settle 
			}
			catch (IOException e) {
				HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.WARNING, e.getMessage());
				this.application.openMessageDialogAsync(Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
			}
			catch (Throwable e) {
				HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
				this.application.openMessageDialogAsync(e.getClass().getSimpleName() + " - " + e.getMessage()); //$NON-NLS-1$
				for (RecordSet recordSet : HoTTAdapter.recordSets.values()) {
					finalizeRecordSet(recordSet);
				}
			}
			WaitTimer.delay(100); //make sure we have such a pause while receiving data even we have 
		}
		for (RecordSet recordSet : HoTTAdapter.recordSets.values()) {
			finalizeRecordSet(recordSet);
		}
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				HoTTAdapterLiveGatherer.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2404), Messages.getString(MessageIds.GDE_MSGT2404));
			}
		});
		if (HoTTAdapterLiveGatherer.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "exit"); //$NON-NLS-1$
	}

	/**
	 * close port, set isDisplayable according channel configuration and calculate slope
	 * @param recordSet
	 */
	public void finalizeRecordSet(RecordSet recordSet) {
		this.serialPort.isInterruptedByUser = true;
		this.serialPort.close();
		this.device.updateVisibilityStatus(recordSet, false);
		this.device.makeInActiveDisplayable(recordSet);
		this.application.updateStatisticsData();
		this.application.updateDataTable(recordSet.getName(), false);
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
		this.channel.switchRecordSet(recordSetKey);
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
	 * detect Sensor data, retrieves data only on not detected sensors driven by boolean[] isSensorType
	 * @param isSensorType
	 * @return points integer array of data points, to enable 3 decimal digits value is multiplied by 1000
	 * @throws Exception 
	 */
	private void detectSensorType(boolean isSensorType[]) throws Exception {

		switch (HoTTAdapterLiveGatherer.this.serialPort.protocolType) {
		case TYPE_19200_L:
			if (HoTTAdapter.IS_SLAVE_MODE) {
				if (!isSensorType[0]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ Vario");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_19200);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					byte[] dataBuffer0 = this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					isSensorType[0] = checkSignature(dataBuffer0, this.serialPort.getData(false));
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
				if (!isSensorType[1]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ GPS");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_19200);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					byte[] dataBuffer0 = this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					isSensorType[1] = checkSignature(dataBuffer0, this.serialPort.getData(false));
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
				if (!isSensorType[2]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ General");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_19200);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					byte[] dataBuffer0 = this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					isSensorType[2] = checkSignature(dataBuffer0, this.serialPort.getData(false));
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
				if (!isSensorType[3]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ Electric");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					byte[] dataBuffer0 = this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					isSensorType[3] = checkSignature(dataBuffer0, this.serialPort.getData(false));
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
			}
			else {
				if (!isSensorType[0]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ Vario");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_19200);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					byte[] dataBuffer = this.serialPort.getData(true);
					isSensorType[0] = (DataParser.parse2Short(dataBuffer, 16) != 0 || dataBuffer[22] != 0);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
				if (!isSensorType[1]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ GPS");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_19200);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					byte[] dataBuffer = this.serialPort.getData(true);
					isSensorType[1] = (dataBuffer[37] == 0x01 || (dataBuffer[20] != 0 && dataBuffer[21] != 0 && dataBuffer[25] != 0 && dataBuffer[26] != 0));
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
				if (!isSensorType[2]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ General");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_19200);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					byte[] dataBuffer = this.serialPort.getData(true);
					isSensorType[2] = (DataParser.parse2Short(dataBuffer, 40) != 0);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
				if (!isSensorType[3]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ Electric");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					byte[] dataBuffer = this.serialPort.getData(true);
					isSensorType[3] = (DataParser.parse2Short(dataBuffer, 40) != 0);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
			}
			break;

		case TYPE_19200_N:
			if (HoTTAdapter.IS_SLAVE_MODE) {
				if (!isSensorType[0]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ Vario");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_19200);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					isSensorType[0] = checkSignature(this.serialPort.getData(false));
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
				if (!isSensorType[1]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ GPS");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_19200);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					isSensorType[1] = checkSignature(this.serialPort.getData(false));
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
				if (!isSensorType[2]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ General");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_19200);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					isSensorType[2] = checkSignature(this.serialPort.getData(false));
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
				if (!isSensorType[3]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ Electric");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					isSensorType[3] = checkSignature(this.serialPort.getData(false));
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
			}
			else {
				if (!isSensorType[0]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ Vario");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_19200);
					this.serialPort.getData(true);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					this.serialPort.getData(true);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					isSensorType[0] = (this.serialPort.getData(true)[15] == HoTTAdapter.ANSWER_SENSOR_VARIO_19200);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
				if (!isSensorType[1]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ GPS");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_19200);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					this.serialPort.getData(true);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					isSensorType[1] = (this.serialPort.getData(true)[15] == HoTTAdapter.ANSWER_SENSOR_GPS_19200);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
				if (!isSensorType[2]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ General");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_19200);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					this.serialPort.getData(true);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					isSensorType[2] = (this.serialPort.getData(true)[15] == HoTTAdapter.ANSWER_SENSOR_GENERAL_19200);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
				if (!isSensorType[3]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ Electric");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200);
					this.serialPort.getData(false);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					this.serialPort.getData(true);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					isSensorType[3] = (this.serialPort.getData(true)[15] == HoTTAdapter.ANSWER_SENSOR_ELECTRIC_19200);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
			}
			break;

		case TYPE_115200:
			if (HoTTAdapter.IS_SLAVE_MODE) {
				this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200); //set to the biggest data array size
				this.serialPort.getData(0);
				Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				byte[] buffer = this.serialPort.getData(0);
				for (int i = 1; i < buffer.length - 3; i++) {
					if (buffer[i] == 0x00 && buffer[i + 1] == 0x04 && buffer[i + 2] == 0x01) {
						if (buffer[i - 1] == 0x0f)
							isSensorType[1] = isSensorType[2] = isSensorType[3] = false; //vario
						else if (buffer[i - 1] == 0x18)
							isSensorType[0] = isSensorType[2] = isSensorType[3] = false; //GPS
						else if (buffer[i - 1] == 0x23)
							isSensorType[0] = isSensorType[1] = isSensorType[3] = false; //general
						else if (buffer[i - 1] == 0x30) isSensorType[0] = isSensorType[1] = isSensorType[2] = false; //electric
					}
				}
				buffer = null;
			}
			else {
				if (!isSensorType[0]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ Vario");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_115200);
					this.serialPort.getData(0);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					this.serialPort.getData(0);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					byte[] dataBuffer = this.serialPort.getData(0);
					isSensorType[0] = (DataParser.parse2Short(dataBuffer, 10) != 0 || dataBuffer[16] != 0);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
				if (!isSensorType[1]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ GPS");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_115200);
					this.serialPort.getData(0);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					this.serialPort.getData(0);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					byte[] dataBuffer = this.serialPort.getData(0);
					isSensorType[1] = (dataBuffer[31] != 0 || (dataBuffer[16] != 0 && dataBuffer[17] != 0 && dataBuffer[20] != 0 && dataBuffer[21] != 0));
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
				if (!isSensorType[2]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ General");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_115200);
					this.serialPort.getData(0);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					this.serialPort.getData(0);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					isSensorType[2] = (DataParser.parse2Short(this.serialPort.getData(0), 36) != 0);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
				if (!isSensorType[3]) {
					HoTTAdapterLiveGatherer.log.log(java.util.logging.Level.FINE, "------------ Electric");
					this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200);
					this.serialPort.getData(0);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					this.serialPort.getData(0);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
					isSensorType[3] = (DataParser.parse2Short(this.serialPort.getData(0), 50) != 0);
					Thread.sleep(HoTTAdapter.QUERY_GAP_MS);
				}
			}
			break;
		}
	}

	/**
	 * check for begin and end signature in data buffer 19200 V3 protocol
	 * @param dataBuffer0
	 * @param dataBuffer1
	 * @return
	 */
	private boolean checkSignature(byte[] dataBuffer0, byte[] dataBuffer1) {
		boolean isDataSignature = false;
		byte[] tmpBuffer = new byte[dataBuffer0.length * 2];
		System.arraycopy(dataBuffer0, 0, tmpBuffer, 0, dataBuffer0.length);
		System.arraycopy(dataBuffer1, 0, tmpBuffer, dataBuffer0.length, dataBuffer1.length);

		for (int i = 0; i < tmpBuffer.length; i++) {
			if (tmpBuffer[i] == HoTTAdapterSerialPort.DATA_BEGIN) {
				int endIndex = i + dataBuffer0.length - 4;
				isDataSignature = endIndex < tmpBuffer.length && tmpBuffer[endIndex] == HoTTAdapterSerialPort.DATA_END;
				break;
			}
		}
		return isDataSignature;
	}

	/**
	 * check for begin and end signature in data buffer 19200 V4 protocol
	 * @param dataBuffer
	 * @return
	 */
	private boolean checkSignature(byte[] dataBuffer) {
		boolean isDataSignature = false;
		for (int i = 0; i < dataBuffer.length; i++) {
			if (dataBuffer[i] == HoTTAdapterSerialPort.DATA_BEGIN) {
				isDataSignature = dataBuffer[i - 1] == this.serialPort.SENSOR_TYPE[0];
				break;
			}
		}
		return isDataSignature;
	}
}
