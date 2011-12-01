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
import gde.utils.WaitTimer;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * HoTTAdapter serial port implementation
 * @author Winfried Brügmann
 */
public class HoTTAdapterSerialPort extends DeviceCommPort {
	final static String	$CLASS_NAME										= HoTTAdapterSerialPort.class.getName();
	final static Logger	log														= Logger.getLogger(HoTTAdapterSerialPort.$CLASS_NAME);
	static final int		READ_TIMEOUT_MS								= 1000;

	//HoTT sensor bytes legacy
	final static byte[]	QUERY_SENSOR_DATA							= new byte[] { (byte) 0x80 };
	final static byte[]	ANSWER												= new byte[1];
	final static byte		DATA_BEGIN										= (byte) 0x7C;
	final static byte		DATA_END											= (byte) 0x7D;

	//HoTT sensor bytes new protocol 
	final static byte[]	QUERY_SENSOR_DATA_DBM					= {0x00, 0x00, (byte) 0xff, 0x00, 0x00, 0x04, 0x33, (byte) 0xf4, (byte) 0xca};
	final static byte[]	QUERY_SENSOR_DATA_RECEIVER		= {0x00, 0x03, (byte) 0xfc, 0x00, 0x00, 0x04, 0x34, 0x13, (byte) 0xba};
	final static byte[]	QUERY_SENSOR_DATA_GENERAL			= {0x00, 0x03, (byte) 0xfc, 0x00, 0x00, 0x04, 0x35, 0x32, (byte) 0xaa};
	final static byte[]	QUERY_SENSOR_DATA_ELECTRIC		= {0x00, 0x03, (byte) 0xfc, 0x00, 0x00, 0x04, 0x36, 0x51, (byte) 0x9a};
	final static byte[]	QUERY_SENSOR_DATA_VARIO				= {0x00, 0x03, (byte) 0xfc, 0x00, 0x00, 0x04, 0x37, 0x70, (byte) 0x8a};
	final static byte[]	QUERY_SENSOR_DATA_GPS					= {0x00, 0x03, (byte) 0xfc, 0x00, 0x00, 0x04, 0x38, (byte) 0x9f, 0x7b};

	byte[]							ANSWER_DATA										= new byte[50];
	int									DATA_LENGTH										= 50;
	byte[]							SENSOR_TYPE										= new byte[] { HoTTAdapter.SENSOR_TYPE_RECEIVER_19200 };
	byte[] 							QUERY_SENSOR_TYPE;
	final static int		xferErrorLimit								= 1000;

	boolean							isInterruptedByUser						= false;
	HoTTAdapter.Protocol protocolType									= HoTTAdapter.Protocol.TYPE_19200_N;

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
		byte[] data = new byte[this.DATA_LENGTH];

		try {
			if (!HoTTAdapter.IS_SLAVE_MODE) {
				this.write(HoTTAdapterSerialPort.QUERY_SENSOR_DATA);
				this.read(HoTTAdapterSerialPort.ANSWER, READ_TIMEOUT_MS);
				data[0] = HoTTAdapterSerialPort.ANSWER[0];
				this.write(this.SENSOR_TYPE);
				this.read(HoTTAdapterSerialPort.ANSWER, READ_TIMEOUT_MS);
				data[1] = HoTTAdapterSerialPort.ANSWER[0];
			}
			else {
				//simulate answers
				data[0] = (byte) 0x80;
				HoTTAdapterSerialPort.ANSWER[0] = this.SENSOR_TYPE[0];
				data[1] = HoTTAdapterSerialPort.ANSWER[0];
			}

			if (HoTTAdapterSerialPort.ANSWER[0] == this.SENSOR_TYPE[0]) {
				this.read(this.ANSWER_DATA, READ_TIMEOUT_MS);
			}

			if (checkBeginEndSignature && HoTTAdapter.IS_SLAVE_MODE) {
				synchronizeDataBlock(data, checkBeginEndSignature);
			}
			else 
				System.arraycopy(this.ANSWER_DATA, 0, data, (HoTTAdapter.IS_SLAVE_MODE ? 0 : 2), this.ANSWER_DATA.length);

			if (log.isLoggable(Level.FINEST)) {
				log.logp(Level.FINEST, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2FourDigitsIntegerString(data));
				log.logp(Level.FINEST, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
			}
			
			if (!this.isInterruptedByUser && checkBeginEndSignature && !(data[2] == HoTTAdapterSerialPort.DATA_BEGIN && data[data.length - 2] == HoTTAdapterSerialPort.DATA_END)) {
				this.addXferError();
				log.logp(Level.WARNING, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, "=====> data start or end does not match, number of errors = " + this.getXferErrors());
				if (this.getXferErrors() > HoTTAdapterSerialPort.xferErrorLimit)
					throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + HoTTAdapterSerialPort.xferErrorLimit);
				WaitTimer.delay(200);
				data = getData(true);
			}
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				log.logp(Level.SEVERE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
			throw e;
		}
		if (log.isLoggable(Level.FINE)) {
			log.logp(Level.FINER, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2FourDigitsIntegerString(data));
			log.logp(Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
		}
		return data;
	}

	/**
	 * synchronize data block in slave mode
	 * @param data
	 * @param checkBeginEndSignature
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 */
	private synchronized byte[] synchronizeDataBlock(byte[] data, boolean checkBeginEndSignature) throws IOException, TimeOutException {
		
		if (!(this.ANSWER_DATA[2] == HoTTAdapterSerialPort.DATA_BEGIN && this.ANSWER_DATA[this.ANSWER_DATA.length - 2] == HoTTAdapterSerialPort.DATA_END)) {
			int index = 0;
			for (byte b : this.ANSWER_DATA) {
				if (b == HoTTAdapterSerialPort.DATA_BEGIN) break;
				++index;
			}

			log.log(Level.FINE, "index = " + index + " begin part size = " + (this.ANSWER_DATA.length-index+2) + " end part size = " + (index-2));
			if (index >= 2 && index < this.ANSWER_DATA.length) {
				System.arraycopy(this.ANSWER_DATA, index-2, data, 0, this.ANSWER_DATA.length-index+2);
				System.arraycopy(this.ANSWER_DATA, 0, data, this.ANSWER_DATA.length-index+2, index-2);
			}
			else
				log.log(Level.WARNING, StringHelper.byte2Hex2CharString(data, data.length));
		}
		
		return data;
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException 
	 * @throws TimeOutException 
	 */
	public synchronized byte[] getData() throws IOException, TimeOutException {
		final String $METHOD_NAME = "getData";
		int rxDBM = 0, txDBM = 0;
		byte[] answerDBM = new byte[234];
		byte[] answerRx = new byte[20];
		byte[] answer = new byte[this.ANSWER_DATA.length];
		byte[] data = new byte[this.ANSWER_DATA.length + 1];

		//sensor type is receiver need to query DBM data in addition
		if (QUERY_SENSOR_TYPE[6] == QUERY_SENSOR_DATA_RECEIVER[6]) {
			++QUERY_SENSOR_DATA_DBM[1];
			--QUERY_SENSOR_DATA_DBM[2];
			this.write(QUERY_SENSOR_DATA_DBM);
			this.read(answerDBM, READ_TIMEOUT_MS);

			if (log.isLoggable(Level.FINE)) {
				log.logp(Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2FourDigitsIntegerString(answerDBM));
				log.logp(Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(answerDBM, answerDBM.length));
			}
			if (QUERY_SENSOR_DATA_DBM[2] == 0xFE) {
				QUERY_SENSOR_DATA_DBM[1] = 0;
				QUERY_SENSOR_DATA_DBM[2] = (byte) 0xFF;
			}
		}
		//query receiver to add voltageRx and temperatureRx
		else {
			this.write(QUERY_SENSOR_DATA_RECEIVER);
			this.read(answerRx, READ_TIMEOUT_MS);

			if (log.isLoggable(Level.FINE)) {
				log.logp(Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2FourDigitsIntegerString(answerDBM));
				log.logp(Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(answerDBM, answerDBM.length));
			}
		}
		WaitTimer.delay(20);

		this.write(QUERY_SENSOR_TYPE);
		
		this.read(answer, READ_TIMEOUT_MS);
		data[0] = QUERY_SENSOR_TYPE[6];
		System.arraycopy(answer, 0, data, 1, answer.length);

		if (log.isLoggable(Level.FINE)) {
			log.logp(Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2FourDigitsIntegerString(data));
			log.logp(Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
		}
		
		if (!this.isInterruptedByUser && answer[0] != 0x00 || answer[4] != 0x00 || answer[5] != 0x04 || answer[6] != 0x01 || (answer[answer.length-3] < 0 && answer[answer.length-3] > 100)) {
			this.addXferError();
			log.logp(Level.WARNING, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, "=====> transmission error occurred, number of errors = " + this.getXferErrors());
			if (this.getXferErrors() > HoTTAdapterSerialPort.xferErrorLimit)
				throw new SerialPortException("Number of transfer error exceed the acceptable limit of " + HoTTAdapterSerialPort.xferErrorLimit);
			data = getData();
		}
	
		//sensor type is receiver need to query DBM data in addition
		if (QUERY_SENSOR_TYPE[6] == QUERY_SENSOR_DATA_RECEIVER[6]) {
			for (int i = 0; i < 75; i++) {
				rxDBM += answerDBM[i + 82];
				txDBM += answerDBM[i + 157];
			}
			data[4] = (byte) (rxDBM /= 75);
			data[5] = (byte) (txDBM /= 75);
		}
		else {
			data[4] = answerRx[14];
			data[5] = answerRx[9];
		}

		return data;
	}

	/**
	 * allocate answer byte array depending on sensor type
	 * @param SENSOR_TYPE the SENSOR_TYPE to set
	 */
	public synchronized void setSensorType(byte sensorType) {
		this.SENSOR_TYPE[0] = sensorType;
		switch (this.protocolType) {
		case TYPE_19200_L:
			switch (sensorType) {
			case HoTTAdapter.SENSOR_TYPE_RECEIVER_19200:
				this.ANSWER_DATA = new byte[HoTTAdapter.IS_SLAVE_MODE ? 17 : 15];
				this.DATA_LENGTH = 17;
				break;
			case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
				this.ANSWER_DATA = new byte[HoTTAdapter.IS_SLAVE_MODE ? 31 : 29];
				this.DATA_LENGTH = 31;
				break;
			case HoTTAdapter.SENSOR_TYPE_GPS_19200:
				this.ANSWER_DATA = new byte[HoTTAdapter.IS_SLAVE_MODE ? 40 : 38];
				this.DATA_LENGTH = 40;
				break;
			case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
				this.ANSWER_DATA = new byte[HoTTAdapter.IS_SLAVE_MODE ? 48 : 46];
				this.DATA_LENGTH = 48;
				break;
			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
				this.ANSWER_DATA = new byte[HoTTAdapter.IS_SLAVE_MODE ? 51 : 49];
				this.DATA_LENGTH = 51;
				break;
			}
			break; 
		case TYPE_115200:
			switch (sensorType) {
			case HoTTAdapter.SENSOR_TYPE_RECEIVER_115200:
				this.ANSWER_DATA = new byte[20];
				this.QUERY_SENSOR_TYPE = QUERY_SENSOR_DATA_RECEIVER;
				break;
			case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
				this.ANSWER_DATA = new byte[24];
				this.QUERY_SENSOR_TYPE = QUERY_SENSOR_DATA_VARIO;
				break;
			case HoTTAdapter.SENSOR_TYPE_GPS_115200:
				this.ANSWER_DATA = new byte[33];
				this.QUERY_SENSOR_TYPE = QUERY_SENSOR_DATA_GPS;
				break;
			case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
				this.ANSWER_DATA = new byte[48];
				this.QUERY_SENSOR_TYPE = QUERY_SENSOR_DATA_GENERAL;
				break;
			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
				this.ANSWER_DATA = new byte[59];
				this.QUERY_SENSOR_TYPE = QUERY_SENSOR_DATA_ELECTRIC;
				break;
			}
			break;
		case TYPE_19200_N:
			switch (sensorType) {
			case HoTTAdapter.SENSOR_TYPE_RECEIVER_19200:
				this.ANSWER_DATA = new byte[HoTTAdapter.IS_SLAVE_MODE ? 17 : 15];
				this.DATA_LENGTH = 17;
				break;
			case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
			case HoTTAdapter.SENSOR_TYPE_GPS_19200:
			case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
				this.ANSWER_DATA = new byte[HoTTAdapter.IS_SLAVE_MODE ? 57 : 55];
				this.DATA_LENGTH = 57;
				break;
			}
			break;
		}
		log.log(Level.OFF, "ANSWER_DATA_LENGTH = " + this.ANSWER_DATA.length + " DATA_LENGTH = " + DATA_LENGTH);
	}

	/**
	 * @param newProtocolTypeOrdinal the isProtocolTypeLegacy to set
	 */
	public synchronized void setProtocolType(HoTTAdapter.Protocol newProtocolType) {
		log.log(Level.FINE, "protocolTypeOrdinal = " + newProtocolType.value());
		this.protocolType = newProtocolType;
	}
}
