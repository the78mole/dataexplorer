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
	byte[]							SENSOR_TYPE										= new byte[] { HoTTAdapter.SENSOR_TYPE_RECEIVER_L };
	byte[] 							QUERY_SENSOR_TYPE;
	final static int		xferErrorLimit								= 100;

	boolean							isInSync											= false;
	boolean							isDataMissmatchWarningWritten	= false;
	int									dataCheckSumOffset						= 0;
	boolean							isCmdMissmatchWarningWritten	= false;
	int									cmdCheckSumOffset							= 0;
	boolean							isInterruptedByUser						= false;
	boolean							isProtocolTypeLegacy					= true;

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
		this.read(HoTTAdapterSerialPort.ANSWER, 3000);
		data[0] = this.SENSOR_TYPE[0];
		data[1] = HoTTAdapterSerialPort.ANSWER[0];
		if (HoTTAdapterSerialPort.ANSWER[0] == this.SENSOR_TYPE[0]) {
			this.read(this.ANSWER_DATA, 3000);
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
			this.read(answerDBM, 3000);

			if (log.isLoggable(Level.FINE)) {
				log.logp(Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2FourDigitsIntegerString(answerDBM));
				log.logp(Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(answerDBM));
			}
			if (QUERY_SENSOR_DATA_DBM[2] == 0xFE) {
				QUERY_SENSOR_DATA_DBM[1] = 0;
				QUERY_SENSOR_DATA_DBM[2] = (byte) 0xFF;
			}
		}
		//query receiver to add voltageRx and temperatureRx
		else {
			this.write(QUERY_SENSOR_DATA_RECEIVER);
			this.read(answerRx, 3000);

			if (log.isLoggable(Level.FINE)) {
				log.logp(Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2FourDigitsIntegerString(answerDBM));
				log.logp(Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(answerDBM));
			}
		}
		WaitTimer.delay(20);

		this.write(QUERY_SENSOR_TYPE);
		this.read(answer, 3000);
		data[0] = QUERY_SENSOR_TYPE[6];
		System.arraycopy(answer, 0, data, 1, answer.length);

		if (log.isLoggable(Level.FINE)) {
			log.logp(Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2FourDigitsIntegerString(data));
			log.logp(Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data));
		}
		
		if (answer[0] != 0x00 || answer[4] != 0x00 || answer[5] != 0x04 || answer[6] != 0x01 || (answer[answer.length-3] < 0 && answer[answer.length-3] > 100)) {
			this.addXferError();
			log.logp(Level.WARNING, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME,
					"=====> transmission error occurred, number of errors = " + this.getXferErrors());
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
		switch (sensorType) {
		default:
		case HoTTAdapter.SENSOR_TYPE_RECEIVER_L:
			this.ANSWER_DATA = new byte[15];
			break;
		case HoTTAdapter.SENSOR_TYPE_RECEIVER:
			this.ANSWER_DATA = new byte[20];
			this.QUERY_SENSOR_TYPE = QUERY_SENSOR_DATA_RECEIVER;
			break;
		case HoTTAdapter.SENSOR_TYPE_VARIO_L:
			this.ANSWER_DATA = new byte[29];
			break;
		case HoTTAdapter.SENSOR_TYPE_VARIO:
			this.ANSWER_DATA = new byte[24];
			this.QUERY_SENSOR_TYPE = QUERY_SENSOR_DATA_VARIO;
			break;
		case HoTTAdapter.SENSOR_TYPE_GPS_L:
			this.ANSWER_DATA = new byte[38];
			break;
		case HoTTAdapter.SENSOR_TYPE_GPS:
			this.ANSWER_DATA = new byte[33];
			this.QUERY_SENSOR_TYPE = QUERY_SENSOR_DATA_GPS;
			break;
		case HoTTAdapter.SENSOR_TYPE_GENERAL_L:
			this.ANSWER_DATA = new byte[46];
			break;
		case HoTTAdapter.SENSOR_TYPE_GENERAL:
			this.ANSWER_DATA = new byte[48];
			this.QUERY_SENSOR_TYPE = QUERY_SENSOR_DATA_GENERAL;
			break;
		case HoTTAdapter.SENSOR_TYPE_ELECTRIC_L:
			this.ANSWER_DATA = new byte[49];
			break;
		case HoTTAdapter.SENSOR_TYPE_ELECTRIC:
			this.ANSWER_DATA = new byte[59];
			this.QUERY_SENSOR_TYPE = QUERY_SENSOR_DATA_ELECTRIC;
			break;
		}
	}

	/**
	 * @param isProtocolTypeLegacy the isProtocolTypeLegacy to set
	 */
	public synchronized void setProtocolTypeLegacy(boolean isProtocolTypeLegacy) {
		log.log(Level.FINE, "isProtocolTypeLegacy = " + isProtocolTypeLegacy);
		this.isProtocolTypeLegacy = isProtocolTypeLegacy;
	}
}
