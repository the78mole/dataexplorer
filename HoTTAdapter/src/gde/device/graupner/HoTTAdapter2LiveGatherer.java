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
import gde.data.RecordSet;
import gde.device.graupner.hott.MessageIds;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;

/**
 * Thread implementation to gather data from HoTTAdapter device
 * @author Winfied Brügmann
 */
public class HoTTAdapter2LiveGatherer extends HoTTAdapterLiveGatherer {
	final static Logger						logger											= Logger.getLogger(HoTTAdapter2LiveGatherer.class.getName());

	/**
	 * @throws Exception 
	 */
	public HoTTAdapter2LiveGatherer(DataExplorer currentApplication, HoTTAdapter useDevice, HoTTAdapterSerialPort useSerialPort, HoTTAdapterDialog useDialog) throws Exception {
		super(currentApplication, useDevice, useSerialPort, useDialog); //$NON-NLS-1$
	}

	@Override
	public void run() {
		RecordSet recordSet = null;
		int[] points = null;
		HoTTAdapter2.recordSets.clear();
		try {
			if (!this.serialPort.isConnected()) {
				this.serialPort.open();
				this.isPortOpenedByLiveGatherer = true;
			}
			this.serialPort.isInterruptedByUser = false;
			long startTime = new Date().getTime();
			//detect sensor type running a max of 3 times getting the sensor data
			for (int i = 0; i < HoTTAdapter2.isSensorType.length; i++) {
				HoTTAdapter2.isSensorType[i] = false;
			}

			//detect master slave mode, only in slave mode data are written to receive buffer without query 
			WaitTimer.delay(1000);
			if (this.serialPort.cleanInputStream() > 2) {
				HoTTAdapter2.IS_SLAVE_MODE = true;
				logger.log(Level.FINE, "HoTTAdapter2.IS_SLAVE_MODE = " + HoTTAdapter2.IS_SLAVE_MODE);
			}
			else {
				HoTTAdapter2.IS_SLAVE_MODE = false;
				logger.log(Level.FINE, "HoTTAdapter2.IS_SLAVE_MODE = " + HoTTAdapter2.IS_SLAVE_MODE);

				for (int i = 0; i < 10 && (HoTTAdapter2.isSensorType[0] == false && HoTTAdapter2.isSensorType[1] == false && HoTTAdapter2.isSensorType[2] == false && HoTTAdapter2.isSensorType[3] == false && HoTTAdapter2.isSensorType[4] == false); i++) {
					try {
						detectSensorType(HoTTAdapter2.isSensorType);
					}
					catch (Exception e) {
						logger.log(Level.WARNING, e.getMessage(), e);
					}
				}
				try {
					detectSensorType(HoTTAdapter2.isSensorType);
				}
				catch (Exception e) {
					logger.log(Level.WARNING, e.getMessage(), e);
				}
				
				StringBuilder sb = new StringBuilder();
				boolean[] tmpSensorType = HoTTAdapter.isSensorType.clone();
				//0=isReceiver, 1=isVario, 2=isGPS, 3=isGeneral, 4=isElectric
				for (int i = HoTTAdapter.isSensorType.length - 1; i >= 0; i--) {
					if (sb.length() == 0 && tmpSensorType[i]) {
						sb.append(tmpSensorType[4] ? ">>>Electric<<<" : tmpSensorType[3] ? ">>>General<<<" : tmpSensorType[2] ? ">>>GPS<<<" : tmpSensorType[1] ? ">>>Vario<<<" : tmpSensorType[0] ? ">>>Receiver<<<" : "");
						tmpSensorType[i] = false;
					}
					else if (tmpSensorType[i]) {
						sb.append(GDE.STRING_MESSAGE_CONCAT).append(tmpSensorType[4] ? ">>>Electric<<<" : tmpSensorType[3] ? ">>>General<<<" : tmpSensorType[2] ? ">>>GPS<<<" : tmpSensorType[1] ? ">>>Vario<<<" : tmpSensorType[0] ? ">>>Receiver<<<" : "");
						tmpSensorType[i] = false;
					}
				}
				HoTTAdapterLiveGatherer.log.log(Level.OFF, sb.toString() + ", detecting sensor type takes " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));

				//no sensor type detected, seams only receiver is connected
				if (HoTTAdapter2.isSensorType[1] == false && HoTTAdapter2.isSensorType[2] == false && HoTTAdapter2.isSensorType[3] == false && HoTTAdapter2.isSensorType[4] == false) {
					this.serialPort.setSensorType(this.serialPort.protocolType.ordinal() < 2 ? HoTTAdapter2.SENSOR_TYPE_RECEIVER_19200 : HoTTAdapter2.SENSOR_TYPE_RECEIVER_115200);
					if (HoTTAdapter2.isSensorType[0] == false)
						this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(MessageIds.GDE_MSGW2400));
				}
			}
		}
		catch (SerialPortException e) {
			this.serialPort.close();
			this.dialog.resetButtons();
			this.application.openMessageDialogAsync(this.dialog.getDialogShell(),
					Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
			return;
		}
		catch (ApplicationConfigurationException e) {
			this.serialPort.close();
			this.dialog.resetButtons();
			this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					HoTTAdapter2LiveGatherer.this.application.getDeviceSelectionDialog().open();
				}
			});
			return;
		}
		catch (Throwable t) {
			this.serialPort.close();
			logger.log(Level.SEVERE, t.getMessage(), t);
			this.dialog.resetButtons();
			this.application.openMessageDialogAsync(this.dialog.getDialogShell(),
					Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { t.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + t.getMessage() }));
			return;
		}
		this.application.setStatusMessage(GDE.STRING_EMPTY);

		this.channel = this.application.getActiveChannel();
		String recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + "live" + GDE.STRING_RIGHT_BRACKET;
		String recordSetKey = this.channel.getNextRecordSetNumber() + this.device.getRecordSetStemName() + recordSetNameExtend;

		this.dialog.selectTab(device.getLastChannelNumber());
		this.channel = this.channels.get(device.getLastChannelNumber());
		recordSet = RecordSet.createRecordSet(recordSetKey, this.device, device.getLastChannelNumber(), true, true);
		this.channel.put(recordSetKey, recordSet);
		HoTTAdapter2.recordSets.put(HoTTAdapter2.Sensor.RECEIVER.value(), recordSet);
		this.channel.applyTemplate(recordSetKey, true);
		points = new int[recordSet.size()];

		this.application.getMenuToolBar().updateChannelSelector();
		this.application.getMenuToolBar().updateRecordSetSelectCombo();
		this.channels.switchChannel(this.channel.getName());
		this.channel.switchRecordSet(recordSetKey);

		Vector<Integer> queryRing = new Vector<Integer>();
		for (int i = 1; i < HoTTAdapter2.isSensorType.length; ++i) {
			if (HoTTAdapter2.isSensorType[i]) queryRing.add(i);
		}
		long measurementCount = 0;
		final long startTime = System.nanoTime() / 1000000;
		while (!this.serialPort.isInterruptedByUser) {
			if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "====> entry"); //$NON-NLS-1$
			try {
				// prepare the data and add to record set
				// build the point array according curves from record set
				switch (HoTTAdapter2LiveGatherer.this.serialPort.protocolType) {
				case TYPE_19200_V3:
				case TYPE_19200_V4:
					if (HoTTAdapter.IS_SLAVE_MODE) {
						this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER_19200); //receiver has shortest data array
						byte[] dataBuffer = HoTTAdapter2LiveGatherer.this.serialPort.getData(false);
						while (!this.serialPort.isInterruptedByUser && !checkContainsDataBegin(dataBuffer)) {
							WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
							dataBuffer = HoTTAdapter2LiveGatherer.this.serialPort.getData(false);
						}
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						HoTTAdapter2LiveGatherer.this.serialPort.getData(false);
						WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
						if (checkSignature(dataBuffer, HoTTAdapter.SENSOR_TYPE_RECEIVER_19200)) {
							recordSet.addPoints(this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
						}
						else if (checkSignature(dataBuffer, HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200)) {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200);
							recordSet.addPoints(this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
						}
						else if (checkSignature(dataBuffer, HoTTAdapter.SENSOR_TYPE_GENERAL_19200)) {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL_19200);
							recordSet.addPoints(this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
						}
						else if (checkSignature(dataBuffer, HoTTAdapter.SENSOR_TYPE_GPS_19200)) {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS_19200);
							recordSet.addPoints(this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
						}
						else if (checkSignature(dataBuffer, HoTTAdapter.SENSOR_TYPE_VARIO_19200)) {
							this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO_19200);
							recordSet.addPoints(this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true)), System.nanoTime() / 1000000 - startTime);
						}			
					}
					else {
						if (HoTTAdapter2.isSensorType[0] == true) {
							try {
								//always gather receiver data first, anserRx are used to fill RXSQ, VoltageRx and TemperatureRx
								this.serialPort.setSensorType(HoTTAdapter2.SENSOR_TYPE_RECEIVER_19200);
								HoTTAdapter2LiveGatherer.this.serialPort.getData(false);
								WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
								HoTTAdapter2LiveGatherer.this.serialPort.getData(true);
								WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
								this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true));
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
						if (queryRing.firstElement() == 4) {
							try {
								this.serialPort.setSensorType(HoTTAdapter2.SENSOR_TYPE_ELECTRIC_19200);
								HoTTAdapter2LiveGatherer.this.serialPort.getData(false);
								WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
								HoTTAdapter2LiveGatherer.this.serialPort.getData(true);
								WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
								this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true));
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
						if (queryRing.firstElement() == 3) {
							try {
								this.serialPort.setSensorType(HoTTAdapter2.SENSOR_TYPE_GENERAL_19200);
								HoTTAdapter2LiveGatherer.this.serialPort.getData(false);
								WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
								HoTTAdapter2LiveGatherer.this.serialPort.getData(true);
								WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
								this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true));
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
						if (queryRing.firstElement() == 2) {
							try {
								this.serialPort.setSensorType(HoTTAdapter2.SENSOR_TYPE_GPS_19200);
								HoTTAdapter2LiveGatherer.this.serialPort.getData(false);
								WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
								HoTTAdapter2LiveGatherer.this.serialPort.getData(true);
								WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
								this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true));
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
						if (queryRing.firstElement() == 1) {
							try {
								this.serialPort.setSensorType(HoTTAdapter2.SENSOR_TYPE_VARIO_19200);
								HoTTAdapter2LiveGatherer.this.serialPort.getData(false);
								WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
								HoTTAdapter2LiveGatherer.this.serialPort.getData(true);
								WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
								this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(true));
							}
							catch (TimeOutException e) {
								// ignore and go ahead gathering sensor data
								this.serialPort.addTimeoutError();
							}
						}
					}
					break;
				case TYPE_115200:
					if (HoTTAdapter2.isSensorType[0]) {
						try {
							//always gather receiver data first, anserRx are used to fill RXSQ, VoltageRx and TemperatureRx
							this.serialPort.setSensorType(HoTTAdapter2.SENSOR_TYPE_RECEIVER_115200);
							HoTTAdapter2LiveGatherer.this.serialPort.getData(0);
							WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
							HoTTAdapter2LiveGatherer.this.serialPort.getData(0);
							WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
							this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(0));
						}
						catch (TimeOutException e) {
							// ignore and go ahead gathering sensor data
							this.serialPort.addTimeoutError();
						}
					}
					if (queryRing.firstElement() == 4) {
						try {
							this.serialPort.setSensorType(HoTTAdapter2.SENSOR_TYPE_ELECTRIC_115200);
							HoTTAdapter2LiveGatherer.this.serialPort.getData(0);
							WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
							HoTTAdapter2LiveGatherer.this.serialPort.getData(0);
							WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
							this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(0));
						}
						catch (TimeOutException e) {
							// ignore and go ahead gathering sensor data
							this.serialPort.addTimeoutError();
						}
					}
					if (queryRing.firstElement() == 3) {
						try {
							this.serialPort.setSensorType(HoTTAdapter2.SENSOR_TYPE_GENERAL_115200);
							HoTTAdapter2LiveGatherer.this.serialPort.getData(0);
							WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
							HoTTAdapter2LiveGatherer.this.serialPort.getData(0);
							WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
							this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(0));
						}
						catch (TimeOutException e) {
							// ignore and go ahead gathering sensor data
							this.serialPort.addTimeoutError();
						}
					}
					if (queryRing.firstElement() == 2) {
						try {
							this.serialPort.setSensorType(HoTTAdapter2.SENSOR_TYPE_GPS_115200);
							HoTTAdapter2LiveGatherer.this.serialPort.getData(0);
							WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
							HoTTAdapter2LiveGatherer.this.serialPort.getData(0);
							WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
							this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(0));
						}
						catch (TimeOutException e) {
							// ignore and go ahead gathering sensor data
							this.serialPort.addTimeoutError();
						}
					}
					if (queryRing.firstElement() == 1) {
						try {
							this.serialPort.setSensorType(HoTTAdapter2.SENSOR_TYPE_VARIO_115200);
							HoTTAdapter2LiveGatherer.this.serialPort.getData(0);
							WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
							HoTTAdapter2LiveGatherer.this.serialPort.getData(0);
							WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS);
							this.device.convertDataBytes(points, HoTTAdapter2LiveGatherer.this.serialPort.getData(0));
						}
						catch (TimeOutException e) {
							// ignore and go ahead gathering sensor data
							this.serialPort.addTimeoutError();
						}
					}
					break;
				}
				recordSet.addPoints(points, System.nanoTime() / 1000000 - startTime);

				if (queryRing.size() > 1) queryRing.add(queryRing.remove(0));

				if (++measurementCount % 5 == 0) {
					for (RecordSet tmpRecordSet : HoTTAdapter2.recordSets.values()) {
						this.device.updateVisibilityStatus(tmpRecordSet, true);
					}
				}

				HoTTAdapter2LiveGatherer.this.application.updateAllTabs(false);
				
				if (this.serialPort.getTimeoutErrors() > 0 && this.serialPort.getTimeoutErrors() % 10 == 0) {
					this.application.setStatusMessage(
							Messages.getString(gde.messages.MessageIds.GDE_MSGW0045,
									new Object[] { "TimeOutException", this.serialPort.getTimeoutErrors() + "; xferErrors = " + this.serialPort.getXferErrors() }), SWT.COLOR_RED);
				}
			}
			catch (DataInconsitsentException e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
				String message = Messages.getString(gde.messages.MessageIds.GDE_MSGE0028, new Object[] { e.getClass().getSimpleName(), e.getMessage() });
				for (RecordSet tmpRecordSet : HoTTAdapter2.recordSets.values()) {
					cleanup(tmpRecordSet.getName(), message);
				}
			}
			catch (TimeOutException e) {
				logger.log(Level.WARNING, e.getMessage());
				this.serialPort.addTimeoutError();
				this.application.setStatusMessage(
						Messages.getString(gde.messages.MessageIds.GDE_MSGW0045,
								new Object[] { e.getClass().getSimpleName(), this.serialPort.getTimeoutErrors() + "; xferErrors = " + this.serialPort.getXferErrors() }), SWT.COLOR_RED);
				WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS); //give time to settle 
			}
			catch (IOException e) {
				logger.log(Level.WARNING, e.getMessage());
				this.application.openMessageDialogAsync(Messages.getString(gde.messages.MessageIds.GDE_MSGE0022, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
			}
			catch (Throwable e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
				this.application.openMessageDialogAsync(e.getClass().getSimpleName() + " - " + e.getMessage()); //$NON-NLS-1$
				for (RecordSet tmpRecordSet : HoTTAdapter2.recordSets.values()) {
					finalizeRecordSet(tmpRecordSet);
				}
			}
			WaitTimer.delay(HoTTAdapter2.QUERY_GAP_MS); //make sure we have such a pause while receiving data even we have 
		}
		for (RecordSet tmpRecordSet : HoTTAdapter2.recordSets.values()) {
			finalizeRecordSet(tmpRecordSet);
		}
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				HoTTAdapter2LiveGatherer.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2404), Messages.getString(MessageIds.GDE_MSGT2404));
			}
		});
		if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "exit"); //$NON-NLS-1$
	}
}
