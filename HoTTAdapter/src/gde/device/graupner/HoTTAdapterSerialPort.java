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
    
    Copyright (c) 2011 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.comm.DeviceCommPort;
import gde.device.DeviceConfiguration;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * HoTTAdapter serial port implementation
 * @author Winfried Brügmann
 */
public class HoTTAdapterSerialPort extends DeviceCommPort {
	final static String	$CLASS_NAME										= HoTTAdapterSerialPort.class.getName();
	final static Logger	log														= Logger.getLogger(HoTTAdapterSerialPort.$CLASS_NAME);

	final static byte[]	ANSWER												= new byte[1];
	byte[]							ANSWER_DATA										= new byte[38];
	final static byte[]	QUERY_SENSOR_DATA							= new byte[] { (byte) 0x80 };
	byte[]							SENSOR_TYPE										= new byte[] { HoTTAdapter.SENSOR_TYPE_RECEIVER };
	final static byte		DATA_BEGIN										= (byte) 0x7C;
	final static byte		DATA_END											= (byte) 0x7D;

	final static int		xferErrorLimit								= 15;

	boolean							isInSync											= false;
	boolean							isDataMissmatchWarningWritten	= false;
	int									dataCheckSumOffset						= 0;
	boolean							isCmdMissmatchWarningWritten	= false;
	int									cmdCheckSumOffset							= 0;
	boolean							isInterruptedByUser						= false;

	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public HoTTAdapterSerialPort(HoTTAdapter currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
	}

	/**
	 * constructor for testing purpose
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public HoTTAdapterSerialPort(DeviceConfiguration deviceConfiguration) {
		super(deviceConfiguration);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData(boolean checkBeginEndSignature) throws Exception {
		final String $METHOD_NAME = "getData";
		byte[] data = new byte[this.ANSWER_DATA.length + 2];
		byte[] answer = new byte[] { 0x00 };

		try {
			this.write(HoTTAdapterSerialPort.QUERY_SENSOR_DATA);
			this.read(answer, 3000);
			readDataBlock(data);

			if (log.isLoggable(Level.FINE)) {
				log.logp(Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2FourDigitsIntegerString(data));
				log.logp(Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data));
			}
			
			if (checkBeginEndSignature && !(data[2] == HoTTAdapterSerialPort.DATA_BEGIN && data[data.length - 2] == HoTTAdapterSerialPort.DATA_END)) {
				this.addXferError();
				log.logp(Level.WARNING, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME,
						"=====> data start or end does not match, number of errors = " + this.getXferErrors());
				if (this.getXferErrors() > HoTTAdapterSerialPort.xferErrorLimit)
					throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + HoTTAdapterSerialPort.xferErrorLimit);
				data = getData(true);
			}
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				log.logp(Level.SEVERE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
			throw e;
		}
		return data;
	}

	/**
	 * read a sensor data block
	 * @param data
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 */
	private synchronized byte[] readDataBlock(byte[] data) throws IOException, TimeOutException {
		this.write(this.SENSOR_TYPE);
		this.read(HoTTAdapterSerialPort.ANSWER, 1000);
		data[0] = this.SENSOR_TYPE[0];
		data[1] = HoTTAdapterSerialPort.ANSWER[0];
		if (HoTTAdapterSerialPort.ANSWER[0] == this.SENSOR_TYPE[0]) {
			this.read(this.ANSWER_DATA, 1000);
		}
		System.arraycopy(this.ANSWER_DATA, 0, data, 2, this.ANSWER_DATA.length);
		//		byte[] b = new byte[1];
		//		int i = 2;
		//		for (; i < data.length; i++) {
		//			data[i] = this.read(b, 500)[0];
		//			if (data[i] == DATA_END)
		//				break;
		//		}
		//		data[i+1] = this.read(b, 500)[0];
		return data;
	}

	/**
	 * allocate answer byte array depending on sensor type
	 * @param SENSOR_TYPE the SENSOR_TYPE to set
	 */
	public synchronized void setSensorType(byte sensorTypeReceiver) {
		this.SENSOR_TYPE[0] = sensorTypeReceiver;
		switch (sensorTypeReceiver) {
		default:
		case HoTTAdapter.SENSOR_TYPE_RECEIVER:
			this.ANSWER_DATA = new byte[15];
			break;
		case HoTTAdapter.SENSOR_TYPE_VARIO:
			this.ANSWER_DATA = new byte[29];
			break;
		case HoTTAdapter.SENSOR_TYPE_GPS:
			this.ANSWER_DATA = new byte[38];
			break;
		case HoTTAdapter.SENSOR_TYPE_GENERAL:
			this.ANSWER_DATA = new byte[46];
			break;
		case HoTTAdapter.SENSOR_TYPE_ELECTRIC:
			this.ANSWER_DATA = new byte[49];
			break;
		}
	}
}
