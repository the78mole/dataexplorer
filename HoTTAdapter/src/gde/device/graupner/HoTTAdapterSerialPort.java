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
    
    Copyright (c) 2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.device.DeviceConfiguration;
import gde.exception.FailedQueryException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.Checksum;
import gde.utils.FileUtils;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HoTTAdapter serial port implementation
 * @author Winfried Brügmann
 */
public class HoTTAdapterSerialPort extends DeviceCommPort {
	final static String		$CLASS_NAME									= HoTTAdapterSerialPort.class.getName();
	final static Logger		log													= Logger.getLogger(HoTTAdapterSerialPort.$CLASS_NAME);
	static final int			READ_TIMEOUT_MS							= 1000;

	//HoTT sensor bytes legacy
	final static byte[]		QUERY_SENSOR_DATA						= new byte[] { (byte) 0x80 };
	final static byte[]		ANSWER											= new byte[1];
	final static byte			DATA_BEGIN									= (byte) 0x7C;
	final static byte			DATA_END										= (byte) 0x7D;

	//HoTT sensor bytes new protocol 
	final static byte[]		QUERY_SENSOR_DATA_DBM				= { 0x00, 0x00, (byte) 0xff, 0x00, 0x00, 0x04, 0x33, (byte) 0xf4, (byte) 0xca };
	final static byte[]		QUERY_SENSOR_DATA_RECEIVER	= { 0x00, 0x03, (byte) 0xfc, 0x00, 0x00, 0x04, 0x34, 0x13, (byte) 0xba };
	final static byte[]		QUERY_SENSOR_DATA_GENERAL		= { 0x00, 0x03, (byte) 0xfc, 0x00, 0x00, 0x04, 0x35, 0x32, (byte) 0xaa };
	final static byte[]		QUERY_SENSOR_DATA_ELECTRIC	= { 0x00, 0x03, (byte) 0xfc, 0x00, 0x00, 0x04, 0x36, 0x51, (byte) 0x9a };
	final static byte[]		QUERY_SENSOR_DATA_VARIO			= { 0x00, 0x03, (byte) 0xfc, 0x00, 0x00, 0x04, 0x37, 0x70, (byte) 0x8a };
	final static byte[]		QUERY_SENSOR_DATA_GPS				= { 0x00, 0x03, (byte) 0xfc, 0x00, 0x00, 0x04, 0x38, (byte) 0x9f, 0x7b };
	final static byte[]		answerRx										= new byte[21];																																	//byte array to cache receiver answer data

	byte[]								ANSWER_DATA									= new byte[50];
	int										DATA_LENGTH									= 50;
	byte[]								SENSOR_TYPE									= new byte[] { HoTTAdapter.SENSOR_TYPE_RECEIVER_19200 };
	byte[]								QUERY_SENSOR_TYPE;
	final static int			xferErrorLimit							= 1000;
	boolean								isQueryRetry								= false;

	HoTTAdapter.Protocol	protocolType								= HoTTAdapter.Protocol.TYPE_19200_V4;

	private static byte[]	root												= new byte[5];

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
				this.read(HoTTAdapterSerialPort.ANSWER, HoTTAdapterSerialPort.READ_TIMEOUT_MS, true);
				data[0] = HoTTAdapterSerialPort.ANSWER[0];
				WaitTimer.delay(4);
				this.write(this.SENSOR_TYPE);
				this.read(HoTTAdapterSerialPort.ANSWER, HoTTAdapterSerialPort.READ_TIMEOUT_MS, true);
				HoTTAdapterSerialPort.ANSWER[0] = this.SENSOR_TYPE[0];
				data[1] = HoTTAdapterSerialPort.ANSWER[0];
			}
			else {
				//simulate answers
				data[0] = HoTTAdapterSerialPort.QUERY_SENSOR_DATA[0];
				HoTTAdapterSerialPort.ANSWER[0] = this.SENSOR_TYPE[0];
				data[1] = HoTTAdapterSerialPort.ANSWER[0];
			}

			this.read(this.ANSWER_DATA, HoTTAdapterSerialPort.READ_TIMEOUT_MS, true);

			if (checkBeginEndSignature && HoTTAdapter.IS_SLAVE_MODE) {
				synchronizeDataBlock(data, checkBeginEndSignature);
			}
			else
				System.arraycopy(this.ANSWER_DATA, 0, data, (HoTTAdapter.IS_SLAVE_MODE ? 0 : 2), this.ANSWER_DATA.length);

			if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINEST)) {
				HoTTAdapterSerialPort.log.logp(java.util.logging.Level.FINEST, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2FourDigitsIntegerString(data));
				HoTTAdapterSerialPort.log.logp(java.util.logging.Level.FINEST, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
			}

			if (!this.isInterruptedByUser && checkBeginEndSignature && !(data[2] == HoTTAdapterSerialPort.DATA_BEGIN && data[data.length - 2] == HoTTAdapterSerialPort.DATA_END)) {
				this.addXferError();
				HoTTAdapterSerialPort.log.logp(java.util.logging.Level.WARNING, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME,
						"=====> data start or end does not match, number of errors = " + this.getXferErrors());
				if (this.getXferErrors() > HoTTAdapterSerialPort.xferErrorLimit)
					throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + HoTTAdapterSerialPort.xferErrorLimit);
				WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
				data = getData(true);
			}
			this.isQueryRetry = false;
		}
		catch (FailedQueryException e) {
			if (!this.isQueryRetry) {
				this.isQueryRetry = true;
				data = getData(true);
			}
			else {
				this.isQueryRetry = false;
				WaitTimer.delay(HoTTAdapterSerialPort.READ_TIMEOUT_MS);
				TimeOutException te = new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { this.ANSWER_DATA.length, HoTTAdapterSerialPort.READ_TIMEOUT_MS }));
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.SEVERE, te.getMessage(), te);
				throw te;
			}
		}
		if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) {
			HoTTAdapterSerialPort.log.logp(java.util.logging.Level.FINER, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2FourDigitsIntegerString(data));
			HoTTAdapterSerialPort.log.logp(java.util.logging.Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
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

			HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINER, "index = " + index + " begin part size = " + (this.ANSWER_DATA.length - index + 2) + " end part size = " + (index - 2));
			if (index >= 2 && index < this.ANSWER_DATA.length) {
				System.arraycopy(this.ANSWER_DATA, index - 2, data, 0, this.ANSWER_DATA.length - index + 2);
				System.arraycopy(this.ANSWER_DATA, 0, data, this.ANSWER_DATA.length - index + 2, index - 2);
			}
			else
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.WARNING, StringHelper.byte2Hex2CharString(data, data.length));
		}
		else
			System.arraycopy(this.ANSWER_DATA, 0, data, 0, this.ANSWER_DATA.length);

		return data;
	}

	//	private boolean isParity19200(byte[] data) {
	//		final String $METHOD_NAME = "isParity";
	//		byte parity = Checksum.ADD(data, 2, data.length-2);
	//		log.logp(Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, String.format("0x%02X == 0x%02X", parity, data[data.length-1]));
	//		return data[data.length-1] == parity;
	//	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException 
	 * @throws TimeOutException 
	 */
	public synchronized byte[] getData(int queryDBM) throws IOException, TimeOutException {
		final String $METHOD_NAME = "getData";
		int rxDBM = 0, txDBM = 0;
		byte[] answerDBM = new byte[234];
		byte[] answer = new byte[this.ANSWER_DATA.length];
		byte[] data = new byte[this.DATA_LENGTH];

		try {
			this.write(this.QUERY_SENSOR_TYPE);
			this.read(answer, HoTTAdapterSerialPort.READ_TIMEOUT_MS, true);
			data[0] = this.QUERY_SENSOR_TYPE[6];
			System.arraycopy(answer, 0, data, 1, answer.length);
			if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) {
				HoTTAdapterSerialPort.log.logp(java.util.logging.Level.FINER, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2FourDigitsIntegerString(data));
				HoTTAdapterSerialPort.log.logp(java.util.logging.Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
			}
			if (!this.isInterruptedByUser && (answer[0] != 0x00 || answer[4] != 0x00 || answer[5] != 0x04 || answer[6] != 0x01 || (answer[answer.length - 3] < 0 && answer[answer.length - 3] > 100))) {
				this.addXferError();
				HoTTAdapterSerialPort.log.logp(java.util.logging.Level.WARNING, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME,
						"=====> transmission error occurred, number of errors = " + this.getXferErrors());
				if (this.getXferErrors() > HoTTAdapterSerialPort.xferErrorLimit)
					throw new SerialPortException("Number of transfer error exceed the acceptable limit of " + HoTTAdapterSerialPort.xferErrorLimit);
				WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
				data = getData(queryDBM);
			}
			//sensor type is receiver need to query DBM data in addition
			if (queryDBM > 0 && this.QUERY_SENSOR_TYPE[6] == HoTTAdapterSerialPort.QUERY_SENSOR_DATA_RECEIVER[6]) {
				WaitTimer.delay(HoTTAdapter.QUERY_GAP_MS);
				++HoTTAdapterSerialPort.QUERY_SENSOR_DATA_DBM[1];
				--HoTTAdapterSerialPort.QUERY_SENSOR_DATA_DBM[2];
				this.write(HoTTAdapterSerialPort.QUERY_SENSOR_DATA_DBM);
				this.read(answerDBM, HoTTAdapterSerialPort.READ_TIMEOUT_MS * 2, true);

				if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) {
					HoTTAdapterSerialPort.log.logp(java.util.logging.Level.FINER, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2FourDigitsIntegerString(answerDBM));
					HoTTAdapterSerialPort.log.logp(java.util.logging.Level.FINE, HoTTAdapterSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(answerDBM, answerDBM.length));
				}
				if ((HoTTAdapterSerialPort.QUERY_SENSOR_DATA_DBM[2] & 0xFF) == 0xFE) {
					HoTTAdapterSerialPort.QUERY_SENSOR_DATA_DBM[1] = 0;
					HoTTAdapterSerialPort.QUERY_SENSOR_DATA_DBM[2] = (byte) 0xFF;
				}

				for (int i = 0; i < 75; i++) {
					rxDBM += answerDBM[i + 157];
					txDBM += answerDBM[i + 82];
				}
				data[4] = (byte) (rxDBM /= 75);
				data[5] = (byte) (txDBM /= 75);
				System.arraycopy(answer, 0, HoTTAdapterSerialPort.answerRx, 0, answer.length);
			}
			else {
				data[3] = HoTTAdapterSerialPort.answerRx[16];
				data[4] = HoTTAdapterSerialPort.answerRx[14];
				data[5] = HoTTAdapterSerialPort.answerRx[9];
			}
			this.isQueryRetry = false;
		}
		catch (FailedQueryException e) {
			if (!this.isQueryRetry) {
				this.isQueryRetry = true;
				data = getData(queryDBM);
			}
			else {
				this.isQueryRetry = false;
				TimeOutException te = new TimeOutException(Messages.getString(MessageIds.GDE_MSGE0011, new Object[] { this.ANSWER_DATA.length, HoTTAdapterSerialPort.READ_TIMEOUT_MS }));
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.SEVERE, te.getMessage(), te);
				throw te;
			}
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
		case TYPE_19200_V3:
			switch (sensorType) {
			case HoTTAdapter.SENSOR_TYPE_RECEIVER_19200:
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, ">>>Receiver<<<");
				this.ANSWER_DATA = new byte[HoTTAdapter.IS_SLAVE_MODE ? 17 : 15];
				this.DATA_LENGTH = 17;
				break;
			case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, ">>>Vario<<<");
				this.ANSWER_DATA = new byte[HoTTAdapter.IS_SLAVE_MODE ? 31 : 29];
				this.DATA_LENGTH = 31;
				break;
			case HoTTAdapter.SENSOR_TYPE_GPS_19200:
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, ">>>GPS<<<");
				this.ANSWER_DATA = new byte[HoTTAdapter.IS_SLAVE_MODE ? 40 : 38];
				this.DATA_LENGTH = 40;
				break;
			case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, ">>>General<<<");
				this.ANSWER_DATA = new byte[HoTTAdapter.IS_SLAVE_MODE ? 48 : 46];
				this.DATA_LENGTH = 48;
				break;
			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, ">>>Electric<<<");
				this.ANSWER_DATA = new byte[HoTTAdapter.IS_SLAVE_MODE ? 51 : 49];
				this.DATA_LENGTH = 51;
				break;
			}
			break;
		case TYPE_115200:
			switch (sensorType) {
			case HoTTAdapter.SENSOR_TYPE_RECEIVER_115200:
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, ">>>Receiver<<<");
				this.ANSWER_DATA = new byte[20];
				this.QUERY_SENSOR_TYPE = HoTTAdapterSerialPort.QUERY_SENSOR_DATA_RECEIVER;
				this.DATA_LENGTH = 21;
				break;
			case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, ">>>Vario<<<");
				this.ANSWER_DATA = new byte[24];
				this.QUERY_SENSOR_TYPE = HoTTAdapterSerialPort.QUERY_SENSOR_DATA_VARIO;
				this.DATA_LENGTH = 25;
				break;
			case HoTTAdapter.SENSOR_TYPE_GPS_115200:
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, ">>>GPS<<<");
				this.ANSWER_DATA = new byte[33];
				this.QUERY_SENSOR_TYPE = HoTTAdapterSerialPort.QUERY_SENSOR_DATA_GPS;
				this.DATA_LENGTH = 34;
				break;
			case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, ">>>General<<<");
				this.ANSWER_DATA = new byte[48];
				this.QUERY_SENSOR_TYPE = HoTTAdapterSerialPort.QUERY_SENSOR_DATA_GENERAL;
				this.DATA_LENGTH = 49;
				break;
			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, ">>>Electric<<<");
				this.ANSWER_DATA = new byte[59];
				this.QUERY_SENSOR_TYPE = HoTTAdapterSerialPort.QUERY_SENSOR_DATA_ELECTRIC;
				this.DATA_LENGTH = 60;
				break;
			}
			break;
		case TYPE_19200_V4:
			switch (sensorType) {
			case HoTTAdapter.SENSOR_TYPE_RECEIVER_19200:
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, ">>>Receiver<<<");
				this.ANSWER_DATA = new byte[HoTTAdapter.IS_SLAVE_MODE ? 17 : 15];
				this.DATA_LENGTH = 17;
				break;
			case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
			case HoTTAdapter.SENSOR_TYPE_GPS_19200:
			case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, sensorType == HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200 ? ">>>Electric<<<"
						: sensorType == HoTTAdapter.SENSOR_TYPE_GENERAL_19200 ? ">>>General<<<" : sensorType == HoTTAdapter.SENSOR_TYPE_GPS_19200 ? ">>>GPS<<<"
								: sensorType == HoTTAdapter.SENSOR_TYPE_VARIO_19200 ? ">>>Vario<<<" : ">>>Receiver<<<");
				this.ANSWER_DATA = new byte[HoTTAdapter.IS_SLAVE_MODE ? 57 : 55];
				this.DATA_LENGTH = 57;
				break;
			}
			break;
		}
		HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINER, "ANSWER_DATA_LENGTH = " + this.ANSWER_DATA.length + " DATA_LENGTH = " + this.DATA_LENGTH);
	}

	/**
	 * @param newProtocolTypeOrdinal the isProtocolTypeLegacy to set
	 */
	public synchronized void setProtocolType(HoTTAdapter.Protocol newProtocolType) {
		HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "protocolTypeOrdinal = " + newProtocolType.value());
		this.protocolType = newProtocolType;
	}

	//transmitter SD-Card to PC communication section
	final static int		CMD_GAP_MS						= 5;
	final static int		FILE_TRANSFER_SIZE		= 0x0400;
	final static byte[]	cmd1									= new byte[7];
	byte								cntUp									= 0x00;
	byte								cntDown								= (byte) 0xFF;

	final static byte[]	QUERY_TX_INFO					= { 0x00, 0x11 };
	final static byte[]	WRITE_SCREEN					= { 0x00, 0x21 };	// 1. byte: row, 21 byte text 
	final static byte[]	RESET_SCREEN					= { 0x00, 0x22 };
	final static byte[]	CLEAR_SCREEN					= { 0x00, 0x23 };
	final static byte[]	CLOSE_SCREEN					= { 0x00, 0x24 };

	final static byte[]	PREPARE_FILE_TRANSFER	= { 0x03, 0x30 };

	final static byte[]	PREPARE_LIST_MDL			= { 0x05, 0x32 };
	final static byte[]	QUERY_MDL_NAMES				= { 0x05, 0x33 };

	final static byte[]	SELECT_SD_CARD				= { 0x06, 0x30 };
	final static byte[]	QUERY_SD_SIZES				= { 0x06, 0x33 };
	final static byte[]	FILE_XFER_INIT				= { 0x06, 0x35 };
	final static byte[]	FILE_XFER_CLOSE				= { 0x06, 0x36 };
	final static byte[]	FILE_UPLOAD						= { 0x06, 0x38 };
	final static byte[]	FILE_DELETE						= { 0x06, 0x39 };
	final static byte[]	FILE_DOWNLOAD					= { 0x06, 0x3A };
	final static byte[]	LIST_DIR							= { 0x06, 0x3C };
	final static byte[]	CHANGE_DIR						= { 0x06, 0x3D };
	final static byte[]	MK_DIR								= { 0x06, 0x3E };
	final static byte[]	FILE_INFO							= { 0x06, 0x3F };

	/**
	 * prepare simple command for sending
	 * @param cmd
	 * @return
	 */
	private byte[] prepareCmdBytes(byte[] cmd) {
		return prepareCmdBytes(cmd, GDE.STRING_EMPTY);
	}

	/**
	 * prepare command with string parameter for sending
	 * @param cmd
	 * @param body
	 * @return
	 */
	private byte[] prepareCmdBytes(byte[] cmd, String body) {
		byte[] b = new byte[body.length() == 0 ? body.length() + 9 : body.length() + 10];
		b[0] = 0x00;
		if (this.cntUp == 0xFF || this.cntDown == 0x00) {
			this.cntUp = 0x00;
			this.cntDown = (byte) 0xFF;
		}
		b[1] = this.cntUp += 0x01;
		b[2] = this.cntDown -= 0x01;
		b[3] = (byte) (body.length() == 0 ? (body.length() & 0xFF) : ((body.length() + 1) & 0xFF));
		b[4] = 0x00;
		b[5] = cmd[0];
		b[6] = cmd[1];
		int i = 7;
		for (; i < body.length() + 7; ++i) {
			b[i] = (byte) (body.getBytes()[i - 7] & 0xFF);
		}
		if (body.length() > 0) b[i++] = 0x00;
		short crc16 = Checksum.CRC16CCITT(b, 3, (body.length() == 0 ? body.length() + 4 : body.length() + 5));
		b[i++] = (byte) (crc16 & 0x00FF);
		b[i++] = (byte) ((crc16 & 0xFF00) >> 8);

		if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINER, StringHelper.byte2Hex2CharString(b, b.length));
		return b;
	}

	/**
	 * prepare command with string parameter for sending
	 * @param cmd
	 * @param line
	 * @return
	 */
	private byte[] prepareLineBytes(byte[] cmd, String line, int number) {
		byte[] b = new byte[line.length() + 10];
		b[0] = 0x00;
		if (this.cntUp == 0xFF || this.cntDown == 0x00) {
			this.cntUp = 0x00;
			this.cntDown = (byte) 0xFF;
		}
		b[1] = this.cntUp += 0x01;
		b[2] = this.cntDown -= 0x01;
		b[3] = (byte) (line.length() == 0 ? (line.length() & 0xFF) : ((line.length() + 1) & 0xFF));
		b[4] = 0x00;
		b[5] = cmd[0];
		b[6] = cmd[1];
		b[7] = (byte) (number & 0xFF);
		int i = 8;
		for (; i < line.length() + 8; ++i) {
			b[i] = (byte) (line.getBytes()[i - 8] & 0xFF);
		}
		short crc16 = Checksum.CRC16CCITT(b, 3, (line.length() == 0 ? line.length() + 4 : line.length() + 5));
		b[i++] = (byte) (crc16 & 0x00FF);
		b[i++] = (byte) ((crc16 & 0xFF00) >> 8);

		if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINER, StringHelper.byte2Hex2CharString(b, b.length));
		return b;
	}

	/**
	 * send a simple command
	 * @param cmd
	 * @throws IOException
	 */
	private void sendCmd(byte[] cmd) throws IOException {
		byte[] cmdAll = prepareCmdBytes(cmd);
		System.arraycopy(cmdAll, 0, HoTTAdapterSerialPort.cmd1, 0, 7);
		this.write(HoTTAdapterSerialPort.cmd1);

		WaitTimer.delay(HoTTAdapterSerialPort.CMD_GAP_MS);

		byte[] cmd2 = new byte[cmdAll.length - 7];
		System.arraycopy(cmdAll, 7, cmd2, 0, cmdAll.length - 7);
		this.write(cmd2);
	}

	/**
	 * send a command with string parameter
	 * @param cmd
	 * @param body
	 * @throws IOException
	 */
	private void sendCmd(byte[] cmd, String body) throws IOException {
		byte[] cmdAll = prepareCmdBytes(cmd, body);
		System.arraycopy(cmdAll, 0, HoTTAdapterSerialPort.cmd1, 0, 7);
		this.write(HoTTAdapterSerialPort.cmd1);

		WaitTimer.delay(HoTTAdapterSerialPort.CMD_GAP_MS);

		byte[] cmd2 = new byte[cmdAll.length - 7];
		System.arraycopy(cmdAll, 7, cmd2, 0, cmdAll.length - 7);
		this.write(cmd2);
	}

	/**
	 * send a command with string parameter
	 * @param cmd
	 * @param line
	 * @throws IOException
	 */
	private void sendLine(byte[] cmd, String line, int number) throws IOException {
		byte[] cmdAll = prepareLineBytes(cmd, line, number);
		System.arraycopy(cmdAll, 0, HoTTAdapterSerialPort.cmd1, 0, 7);
		this.write(HoTTAdapterSerialPort.cmd1);

		WaitTimer.delay(HoTTAdapterSerialPort.CMD_GAP_MS);

		byte[] cmd2 = new byte[cmdAll.length - 7];
		System.arraycopy(cmdAll, 7, cmd2, 0, cmdAll.length - 7);
		this.write(cmd2);
	}

	/**
	 * send a command with byte data as parameter
	 * @param cmd
	 * @param data
	 * @throws IOException
	 */
	private void sendCmd(byte[] cmd, byte[] data) throws IOException {
		byte[] cmdAll = new byte[data.length + 8 + 2 + 7];

		//cmd1 part
		cmdAll[0] = 0x00;
		if (this.cntUp == 0xFF || this.cntDown == 0x00) {
			this.cntUp = 0x00;
			this.cntDown = (byte) 0xFF;
		}
		cmdAll[1] = this.cntUp += 0x01;
		cmdAll[2] = this.cntDown -= 0x01;
		cmdAll[3] = (byte) ((data.length + 8) & 0x00FF);
		cmdAll[4] = (byte) (((data.length + 8) & 0xFF00) >> 8);
		cmdAll[5] = cmd[0];
		cmdAll[6] = cmd[1];

		System.arraycopy(String.format("0x%04x ", data.length).getBytes(), 0, cmdAll, 7, 7);
		System.arraycopy(data, 0, cmdAll, 15, data.length);
		short crc16 = Checksum.CRC16CCITT(cmdAll, 3, cmdAll.length - 5);
		cmdAll[cmdAll.length - 2] = (byte) (crc16 & 0x00FF);
		cmdAll[cmdAll.length - 1] = (byte) ((crc16 & 0xFF00) >> 8);
		if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, StringHelper.byte2Hex2CharString(cmdAll, cmdAll.length));

		System.arraycopy(cmdAll, 0, HoTTAdapterSerialPort.cmd1, 0, 7);
		if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
			HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, StringHelper.byte2Hex2CharString(HoTTAdapterSerialPort.cmd1, HoTTAdapterSerialPort.cmd1.length));
		this.write(HoTTAdapterSerialPort.cmd1);

		WaitTimer.delay(HoTTAdapterSerialPort.CMD_GAP_MS);

		byte[] cmd2 = new byte[data.length + 8 + 2];
		System.arraycopy(cmdAll, 7, cmd2, 0, cmdAll.length - 7);
		if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, StringHelper.byte2Hex2CharString(cmd2, cmd2.length));
		this.write(cmd2);
	}

	/**
	 * send command to query model data
	 * @param data
	 * @throws IOException
	 */
	private void sendMdlCmd(byte[] data) throws IOException {
		byte[] cmdAll = new byte[data.length + 2 + 7];

		//cmd1 part
		cmdAll[0] = 0x00;
		if (this.cntUp == 0xFF || this.cntDown == 0x00) {
			this.cntUp = 0x00;
			this.cntDown = (byte) 0xFF;
		}
		cmdAll[1] = this.cntUp += 0x01;
		cmdAll[2] = this.cntDown -= 0x01;
		cmdAll[3] = (byte) (data.length & 0x00FF);
		cmdAll[4] = (byte) ((data.length & 0xFF00) >> 8);
		cmdAll[5] = HoTTAdapterSerialPort.QUERY_MDL_NAMES[0];
		cmdAll[6] = HoTTAdapterSerialPort.QUERY_MDL_NAMES[1];

		System.arraycopy(data, 0, cmdAll, 7, data.length);
		short crc16 = Checksum.CRC16CCITT(cmdAll, 3, data.length + 4);
		cmdAll[cmdAll.length - 2] = (byte) (crc16 & 0x00FF);
		cmdAll[cmdAll.length - 1] = (byte) ((crc16 & 0xFF00) >> 8);
		if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, StringHelper.byte2Hex2CharString(cmdAll, cmdAll.length));

		System.arraycopy(cmdAll, 0, HoTTAdapterSerialPort.cmd1, 0, 7);
		if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
			HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, StringHelper.byte2Hex2CharString(HoTTAdapterSerialPort.cmd1, HoTTAdapterSerialPort.cmd1.length));
		this.write(HoTTAdapterSerialPort.cmd1);

		WaitTimer.delay(HoTTAdapterSerialPort.CMD_GAP_MS);

		byte[] cmd2 = new byte[data.length + 2];
		System.arraycopy(cmdAll, 7, cmd2, 0, cmdAll.length - 7);
		if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, StringHelper.byte2Hex2CharString(cmd2, cmd2.length));
		this.write(cmd2);
	}

	/**
	 * prepare transmitter for SD-card related communication
	 * @param retryCount
	 * @throws Exception
	 */
	public synchronized void prepareSdCard(int retryCount) throws Exception {
		try {
			//prepare transmitter for data interaction
			sendCmd(HoTTAdapterSerialPort.PREPARE_FILE_TRANSFER);
			this.ANSWER_DATA = this.read(new byte[9], HoTTAdapterSerialPort.READ_TIMEOUT_MS);
			if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, StringHelper.byte2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));

			sendCmd(HoTTAdapterSerialPort.SELECT_SD_CARD);
			this.ANSWER_DATA = this.read(new byte[10], HoTTAdapterSerialPort.READ_TIMEOUT_MS);
			if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, StringHelper.byte2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
		}
		catch (Exception e) {
			HoTTAdapterSerialPort.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			if (retryCount < 2)
				prepareSdCard(++retryCount);
			throw e;
		}
	}

	/**
	 * query SD-card sizes, available and free storage space
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized long[] querySdCardSizes(int retryCount) throws Exception {
		long[] ret = new long[2];
		try {
			sendCmd(HoTTAdapterSerialPort.QUERY_SD_SIZES);
			this.ANSWER_DATA = this.read(new byte[50], 2000, 5);
			if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "SD size info : " + StringHelper.byte2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
			if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE,
						"SD size info : " + StringHelper.byte2hex2int(this.ANSWER_DATA, 9, 8) + " KBytes total - " + StringHelper.byte2hex2int(this.ANSWER_DATA, 21, 8) + " KBytes free");

			if (this.ANSWER_DATA[6] == 0x02 && retryCount < 2) {
				HoTTAdapterSerialPort.log.log(Level.WARNING, "querySdCardSizes failed, check SD card");
				this.ANSWER_DATA = new byte[50];
				for (int i = 0; i < this.ANSWER_DATA.length; i++) {
					this.ANSWER_DATA[i] = 0x30;
				}
			}
			
			ret = new long[] { StringHelper.byte2hex2int(this.ANSWER_DATA, 9, 8), StringHelper.byte2hex2int(this.ANSWER_DATA, 21, 8) };
		}
		catch (Exception e) {
			HoTTAdapterSerialPort.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			if (retryCount < 2)
				ret = querySdCardSizes(++retryCount);
			throw e;
		}

		return ret;
	}

	/**
	 * delete files selected on SD-card
	 * @param dirPath
	 * @param files
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public synchronized void deleteFiles(String dirPath, String[] files) throws IOException, TimeOutException {
		for (String file : files) {
			sendCmd(HoTTAdapterSerialPort.FILE_DELETE, dirPath + file);
			this.ANSWER_DATA = this.read(new byte[9], HoTTAdapterSerialPort.READ_TIMEOUT_MS);
			if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "" + StringHelper.byte2Hex2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
		}
	}

	/**
	 * query base folders of SD-Card
	 * @param retryCount
	 * @return
	 * @throws Exception
	 */
	public synchronized String[] querySdDirs(int retryCount) throws Exception {
		StringBuilder sb = new StringBuilder();
		try {
			//change to root directory and query sub folders
			sendCmd(HoTTAdapterSerialPort.CHANGE_DIR, GDE.FILE_SEPARATOR_UNIX);
			HoTTAdapterSerialPort.root = this.read(new byte[50], HoTTAdapterSerialPort.READ_TIMEOUT_MS, 5);
			if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, StringHelper.byte2Hex2CharString(HoTTAdapterSerialPort.root, HoTTAdapterSerialPort.root.length));

			while (this.ANSWER_DATA[7] != HoTTAdapterSerialPort.root[7] && this.ANSWER_DATA[8] != HoTTAdapterSerialPort.root[8]) { //06 01 87 BA
				sendCmd(HoTTAdapterSerialPort.LIST_DIR);
				this.ANSWER_DATA = this.read(new byte[256], HoTTAdapterSerialPort.READ_TIMEOUT_MS, 5);
				if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
					HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, StringHelper.byte2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
				for (int i = 19; i < this.ANSWER_DATA.length - 2; i++) {
					sb.append(String.format("%c", this.ANSWER_DATA[i]));
				}
				sb.append(GDE.STRING_SEMICOLON);
				if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, sb.toString());
			}
		}
		catch (Exception e) {
			HoTTAdapterSerialPort.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			if (retryCount < 2)
				return querySdDirs(++retryCount);
			//else return result if any
		}
		return sb.toString().split(GDE.STRING_SEMICOLON);
	}

	/**
	 * query folders and files of a selected SD-Card directory
	 * @param dirPath
	 * @param retryCount
	 * @return
	 * @throws Exception
	 */
	public HashMap<String, String[]> queryListDir(String dirPath, int retryCount) throws Exception {
		StringBuilder folders = new StringBuilder();
		StringBuilder files = new StringBuilder();
		HashMap<String, String[]> result = new HashMap<String, String[]>();
		int fileIndex = 0;
		sendCmd(HoTTAdapterSerialPort.CHANGE_DIR, dirPath);
		HoTTAdapterSerialPort.root = this.read(new byte[50], HoTTAdapterSerialPort.READ_TIMEOUT_MS, 5);
		if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
			HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, StringHelper.byte2CharString(HoTTAdapterSerialPort.root, HoTTAdapterSerialPort.root.length));

		try {
			this.ANSWER_DATA[3] = 0x01;
			while (this.ANSWER_DATA[3] != 0x00) {
				sendCmd(HoTTAdapterSerialPort.LIST_DIR);
				this.ANSWER_DATA = this.read(new byte[256], HoTTAdapterSerialPort.READ_TIMEOUT_MS, 5);
				if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
					HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, StringHelper.byte2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
				StringBuilder content = new StringBuilder();
				for (int i = 19; i < this.ANSWER_DATA.length - 2; i++) {
					content.append(String.format("%c", this.ANSWER_DATA[i]));
				}
				if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "content : " + content.toString());
				if (content.indexOf(GDE.STRING_DOT) > 0) {//.bin
					files.append(fileIndex++).append(GDE.STRING_COMMA).append(content).append(GDE.STRING_COMMA);
					files.append("20").append(String.format("%c%c-%c%c-%c%c", this.ANSWER_DATA[9], this.ANSWER_DATA[10], this.ANSWER_DATA[11], this.ANSWER_DATA[12], this.ANSWER_DATA[13], this.ANSWER_DATA[14]))
							.append(GDE.STRING_COMMA);
					files.append(String.format("%c%c:%c%c", this.ANSWER_DATA[15], this.ANSWER_DATA[16], this.ANSWER_DATA[17], this.ANSWER_DATA[18])).append(GDE.STRING_SEMICOLON);
					if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "files : " + files.toString());
				}
				else {
					folders.append(content).append(GDE.STRING_SEMICOLON);
					if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "folders : " + folders.toString());
				}
			}

			result.put("FOLDER", folders.toString().split(GDE.STRING_SEMICOLON));
			if (files.toString().length() > 0) result.put("FILES", queryFilesInfo(dirPath + GDE.FILE_SEPARATOR_UNIX, files.toString().split(GDE.STRING_SEMICOLON), 0));
		}
		catch (RuntimeException e) {
			HoTTAdapterSerialPort.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			if (retryCount < 2)
				result = queryListDir(dirPath, ++retryCount);
			//else return result if any
		}
		return result;
	}

	/**
	 * query files info, date, time , size
	 * @param dirPath
	 * @param files
	 * @param retryCount
	 * @return
	 * @throws Exception
	 */
	public String[] queryFilesInfo(String dirPath, String[] files, int retryCount) throws Exception {

		StringBuilder filesInfo = new StringBuilder();
		try {
			for (String file : files) {
				sendCmd(HoTTAdapterSerialPort.FILE_INFO, dirPath + file.split(GDE.STRING_COMMA)[1]);
				this.ANSWER_DATA = this.read(new byte[100], 2000, 5);
				if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
					HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "" + StringHelper.byte2Hex2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
				if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
					HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE,
							"File size = " + Integer.parseInt(String.format("%02x%02x%02x%02x", this.ANSWER_DATA[10], this.ANSWER_DATA[9], this.ANSWER_DATA[8], this.ANSWER_DATA[7]), 16));
				filesInfo.append(file).append(GDE.STRING_COMMA)
						.append(Integer.parseInt(String.format("%02x%02x%02x%02x", this.ANSWER_DATA[10], this.ANSWER_DATA[9], this.ANSWER_DATA[8], this.ANSWER_DATA[7]), 16)).append(GDE.STRING_SEMICOLON);
				if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, filesInfo.toString());
				WaitTimer.delay(HoTTAdapterSerialPort.CMD_GAP_MS);
			}
		}
		catch (RuntimeException e) {
			HoTTAdapterSerialPort.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			if (retryCount < 2)
				return queryFilesInfo(dirPath, files, ++retryCount);
			throw e;
		}

		return filesInfo.toString().split(GDE.STRING_SEMICOLON);
	}

	/**
	 * upload selected files to PC selected folder
	 * @param sourceDirPath
	 * @param targetDirPath
	 * @param filesInfo
	 * @param totalSize
	 * @param parent
	 * @throws Exception
	 */
	public void upLoadFiles(String sourceDirPath, String targetDirPath, String[] filesInfo, final long totalSize, final FileTransferTabItem parent) throws Exception {
		long remainingSize = totalSize;
		DataOutputStream data_out = null;

		try {
			for (String fileInfo : filesInfo) {
				if (!this.isInterruptedByUser) {
					//fileInfo index,name,timeStamp,size
					String[] file = fileInfo.split(GDE.STRING_COMMA);
					String fileQueryAnswer = this.queryFilesInfo(sourceDirPath, new String[] { fileInfo }, 0)[0];
					long remainingFileSize = Long.parseLong(fileQueryAnswer.split(GDE.STRING_COMMA)[3]);

					File xferFile = new File(targetDirPath + GDE.FILE_SEPARATOR_UNIX + file[1]);
					data_out = new DataOutputStream(new FileOutputStream(xferFile));

					sendCmd(HoTTAdapterSerialPort.FILE_XFER_INIT, String.format("0x01 %s%s", sourceDirPath, file[1]));
					this.ANSWER_DATA = this.read(new byte[9], HoTTAdapterSerialPort.READ_TIMEOUT_MS);
					if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
						HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "" + StringHelper.byte2Hex2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));

					int retries = 0;
					while (!this.isInterruptedByUser && remainingFileSize > HoTTAdapterSerialPort.FILE_TRANSFER_SIZE) {
						try {
							if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "remainingFileSize = " + remainingFileSize);
							sendCmd(HoTTAdapterSerialPort.FILE_UPLOAD, String.format("0x%04x", HoTTAdapterSerialPort.FILE_TRANSFER_SIZE));
							this.ANSWER_DATA = this.read(this.ANSWER_DATA = new byte[7], HoTTAdapterSerialPort.READ_TIMEOUT_MS, false);
							if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
								HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "" + StringHelper.byte2Hex2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
							if (this.ANSWER_DATA[5] == 0x06 && this.ANSWER_DATA[6] == 0x01) {
								this.ANSWER_DATA = this.read(this.ANSWER_DATA = new byte[HoTTAdapterSerialPort.FILE_TRANSFER_SIZE + 2], 5000, false); //2048+2
								if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
									HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "" + StringHelper.byte2Hex2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
								data_out.write(this.ANSWER_DATA, 0, HoTTAdapterSerialPort.FILE_TRANSFER_SIZE);
							}
							else
								//error 06 02 
								if (retries++ < 3)	continue;

							remainingSize -= HoTTAdapterSerialPort.FILE_TRANSFER_SIZE;
							remainingFileSize -= HoTTAdapterSerialPort.FILE_TRANSFER_SIZE;
							if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
								HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "sizeProgress = " + remainingSize + " - " + ((totalSize - remainingSize) * 100 / totalSize) + " %");

							parent.updateFileTransferProgress(totalSize, remainingSize);
							retries = 0;
						}
						catch (TimeOutException e) {
							if (this.ANSWER_DATA.length >= 64) {
								//some data are received, write only the part, which is modulo of 64 bytes which is one sentence
								int returnedDataSize = 0;
								for (int i=this.ANSWER_DATA.length-1; i > 0; --i) {
									if (this.ANSWER_DATA[i] != 0x00) {
										returnedDataSize = i - (i % 64);
										break;
									}
								}
								HoTTAdapterSerialPort.log.log(Level.WARNING, file[1] + ": write only " + returnedDataSize + " bytes instead of " + HoTTAdapterSerialPort.FILE_TRANSFER_SIZE);
								data_out.write(this.ANSWER_DATA, 0, returnedDataSize);
								remainingSize -= HoTTAdapterSerialPort.FILE_TRANSFER_SIZE;
								remainingFileSize -= HoTTAdapterSerialPort.FILE_TRANSFER_SIZE;
								parent.updateFileTransferProgress(totalSize, remainingSize);
							}
							if (retries++ < 3) 
								continue;
							throw e;
						}
					}

					if (!this.isInterruptedByUser) {
						if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "remainingFileSize = " + remainingFileSize);
						sendCmd(HoTTAdapterSerialPort.FILE_UPLOAD, String.format("0x%04x", remainingFileSize));
						this.ANSWER_DATA = this.read(new byte[7], 2000);
						if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
							HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "" + StringHelper.byte2Hex2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
						this.ANSWER_DATA = this.read(new byte[(int) (remainingFileSize + 2)], 5000); //rest+2
						if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
							HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "" + StringHelper.byte2Hex2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
						data_out.write(this.ANSWER_DATA, 0, (int) remainingFileSize);
						remainingSize -= remainingFileSize;
					}

					data_out.close();
					data_out = null;

					sendCmd(HoTTAdapterSerialPort.FILE_XFER_CLOSE);
					this.ANSWER_DATA = this.read(new byte[9], HoTTAdapterSerialPort.READ_TIMEOUT_MS);
					if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
						HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "" + StringHelper.byte2Hex2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));

					xferFile.setLastModified(Long.parseLong(file[2])); //timeStamp
					parent.updateFileTransferProgress(totalSize, remainingSize);
					parent.updatePcFolder();
				}
			}
		}
		finally {
			if (data_out != null) data_out.close();
		}
	}

	/**
	 * download PC selected files to SD-card selected folder
	 * @param sourceDirPath
	 * @param targetDirPath
	 * @param filesInfo
	 * @param totalSize
	 * @param parent
	 * @throws Exception
	 */
	public void downLoadFiles(String sourceDirPath, String targetDirPath, String[] filesInfo, final long totalSize, final FileTransferTabItem parent) throws Exception {
		long remainingSize = totalSize;
		DataInputStream data_in = null;
		int xferDataSize = HoTTAdapterSerialPort.FILE_TRANSFER_SIZE;
		byte[] XFER_DATA = new byte[HoTTAdapterSerialPort.FILE_TRANSFER_SIZE];
		byte[] xferSize = new byte[4];

		try {
			for (String fileInfo : filesInfo) {
				if (!this.isInterruptedByUser) {
					//fileInfo index,name,size
					String[] file = fileInfo.split(GDE.STRING_COMMA);
					File xferFile = new File(targetDirPath + GDE.FILE_SEPARATOR_UNIX + file[1]);
					data_in = new DataInputStream(new FileInputStream(xferFile));
					long remainingFileSize = xferFile.length();

					//create target file
					sendCmd(HoTTAdapterSerialPort.FILE_XFER_INIT, String.format("0x0b %s%s", sourceDirPath, file[1]));
					this.ANSWER_DATA = this.read(new byte[9], HoTTAdapterSerialPort.READ_TIMEOUT_MS);
					if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
						HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "" + StringHelper.byte2Hex2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
					sendCmd(HoTTAdapterSerialPort.FILE_XFER_CLOSE);
					this.ANSWER_DATA = this.read(new byte[9], HoTTAdapterSerialPort.READ_TIMEOUT_MS);
					if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
						HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "" + StringHelper.byte2Hex2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));

					sendCmd(HoTTAdapterSerialPort.FILE_XFER_INIT, String.format("0x02 %s%s", sourceDirPath, file[1]));
					this.ANSWER_DATA = this.read(new byte[9], HoTTAdapterSerialPort.READ_TIMEOUT_MS);
					if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
						HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "" + StringHelper.byte2Hex2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));

					int retries = 0;
					while (!this.isInterruptedByUser && remainingFileSize > HoTTAdapterSerialPort.FILE_TRANSFER_SIZE) {
						if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "remainingFileSize = " + remainingFileSize);
						data_in.read(XFER_DATA);
						sendCmd(HoTTAdapterSerialPort.FILE_DOWNLOAD, XFER_DATA);
						this.ANSWER_DATA = this.read(new byte[15], 2000);
						if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
							HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "" + StringHelper.byte2Hex2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));

						if (this.ANSWER_DATA[5] == 0x06 && this.ANSWER_DATA[6] == 0x01) { //00 17 E8 06 00 06 01 30 78 30 38 30 30 19 08
							System.arraycopy(this.ANSWER_DATA, 9, xferSize, 0, 4);
							xferDataSize = Integer.parseInt(new String(xferSize), 16);
							if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "xferDataSize = 0x" + new String(xferSize));
						}
						else
							//error 06 02 -> re-try 
							if (retries++ < 3) continue;

						remainingSize -= xferDataSize;
						remainingFileSize -= xferDataSize;
						if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
							HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "sizeProgress = " + remainingSize + " - " + ((totalSize - remainingSize) * 100 / totalSize) + " %");

						parent.updateFileTransferProgress(totalSize, remainingSize);
						xferDataSize = HoTTAdapterSerialPort.FILE_TRANSFER_SIZE; //target transfer size
					}

					if (!this.isInterruptedByUser) {
						if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "remainingFileSize = " + remainingFileSize);
						XFER_DATA = new byte[(int) remainingFileSize];
						data_in.read(XFER_DATA);
						sendCmd(HoTTAdapterSerialPort.FILE_DOWNLOAD, XFER_DATA);
						this.ANSWER_DATA = this.read(new byte[15], 2000);
						if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
							HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "" + StringHelper.byte2Hex2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
						remainingSize -= remainingFileSize;
					}

					data_in.close();
					data_in = null;

					sendCmd(HoTTAdapterSerialPort.FILE_XFER_CLOSE);
					this.ANSWER_DATA = this.read(new byte[9], HoTTAdapterSerialPort.READ_TIMEOUT_MS);
					if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE))
						HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, "" + StringHelper.byte2Hex2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));

					parent.updateFileTransferProgress(totalSize, remainingSize);
					XFER_DATA = new byte[HoTTAdapterSerialPort.FILE_TRANSFER_SIZE];
				}
			}
			parent.updateSdFolder(this.querySdCardSizes(0));
		}
		finally {
			if (data_in != null) data_in.close();
		}
	}
	
	//sample data from different transmitters
	byte[] mx_20_AM_0011 =	new byte[] {0x00, 0x4F, (byte) 0xB0, 0x35, 0x00, 0x00, 0x01, 0x74, 0x32, (byte) 0xF4, 0x00, 0x60, 0x04, 0x00, 0x00, 0x74, 0x32, (byte) 0xF4, 0x00, (byte)0xD1, 0x07, 0x00, 0x00, 0x4D, 0x58, 0x2D, 0x32, 0x30, 0x20, 0x48, 0x6F, 0x54, 0x54, 0x20, 0x52, 0x61, 0x64, 0x69, 0x6F, 0x47, 0x72, 0x61, 0x75, 0x70, 0x6E, 0x65, 0x72, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00, (byte)0xEA, 0x03, 0x00, 0x00, 0x59, 0x1A};
	byte[] mx_20_RH_0011 =	new byte[] {0x00, 0x0A, (byte) 0xF5, 0x35, 0x00, 0x00, 0x01, 0x74, 0x32, (byte) 0xF4, 0x00, 0x60, 0x04, 0x00, 0x00, 0x74, 0x32, (byte) 0xF4, 0x00, (byte)0xD1, 0x07, 0x00, 0x00, 0x4D, 0x58, 0x2D, 0x32, 0x30, 0x20, 0x48, 0x6F, 0x54, 0x54, 0x20, 0x52, 0x61, 0x64, 0x69, 0x6F, 0x47, 0x72, 0x61, 0x75, 0x70, 0x6E, 0x65, 0x72, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00, (byte)0xEA, 0x03, 0x00, 0x00, 0x59, 0x1A};	
	byte[] mc_32_RH_0011 =	new byte[] {0x00, (byte) 0x87, 0x78, 0x35, 0x00, 0x00, 0x01, 0x04, 0x34, (byte) 0xF4, 0x00, 0x07, 0x04, 0x00, 0x00, 0x04, 0x34, (byte) 0xF4, 0x00, (byte)0xD1, 0x07, 0x00, 0x00, 0x4D, 0x43, 0x2D, 0x33, 0x32, 0x20, 0x32, 0x2E, 0x34, 0x47, 0x20, 0x52, 0x61, 0x64, 0x69, 0x6F, 0x47, 0x72, 0x61, 0x75, 0x70, 0x6E, 0x65, 0x72, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x01, (byte)0xE8, 0x03, 0x00, 0x00, (byte)0xF4, 0x7A};	
	byte[] mx_16_RH_0011 =	new byte[] {0x00, 0x09, (byte)0xF6, 0x35, 0x00, 0x00, 0x01, (byte)0xE4, 0x30, (byte)0xF4, 0x00, (byte)0xB3, 0x06, 0x00, 0x00, (byte)0xE4, 0x30, (byte)0xF4, 0x00, (byte)0xD1, 0x07, 0x00, 0x00, 0x4D, 0x58, 0x2D, 0x31, 0x36, 0x20, 0x48, 0x6F, 0x54, 0x54, 0x20, 0x52, 0x61, 0x64, 0x69, 0x6F, 0x47, 0x72, 0x61, 0x75, 0x70, 0x6E, 0x65, 0x72, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00, (byte)0xE9, 0x03, 0x00, 0x00, (byte)0xD1, (byte)0x57};

	byte[] mx_20_AM_0532 =	new byte[] {0x00, 0x50, (byte) 0xAF, 0x26, 0x01, 0x05, 0x01, 0x18, 0x00, 0x0A, 0x00, 0x31, 0x00, 0x00, 0x20, 0x00, 0x30, 0x00, 0x20, 0x04, 0x20, 0x07, 0x20, 0x0A, 0x20, 0x7F, 0x20, (byte)0x86, 0x20, (byte)0xA0, 0x20, 0x5A, 0x23, 0x5D, 0x23, 0x69, 0x23, 0x02, 0x00, 0x01, 0x00, 0x01, 0x00, 0x73, 0x00, 0x05, 0x00, 0x18, 0x00, (byte)0xB8, 0x02, 0x01, 0x00, 0x0A, 0x00, 0x34, 0x01,(byte) 0xFF, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, 0x01, 0x00, 0x00, 0x00, 0x76, 0x00, (byte)0xD8, 0x00, (byte)0xEA, 0x00, (byte)0xB8, 0x04, 0x7A, 0x05, (byte) 0xB4, 0x05, (byte) 0xE6, 0x05, 0x40, 0x06, 0x60, 0x06, (byte)0xB2, 0x06, 0x34, 0x07, 0x44, 0x07, 0x59, 0x07, (byte)0xBB, 0x07, (byte)0xCD, 0x07, (byte)0xDB, 0x07, 0x11, 0x08, 0x2E, 0x08, 0x40, 0x08, (byte)0x8A, 0x09, (byte)0xB8, 0x0A, (byte)0xC0, 0x0A, (byte)0xC5, 0x0A, (byte)0xE1, 0x0E, (byte)0xE9, 0x0E, 0x00, 0x10, (byte)0xC2, 0x11, (byte)0x8C, 0x13, 0x0A, 0x15, (byte)0xE8, 0x13, 0x6E, 0x14, (byte)0x86, 0x14, (byte)0x9C, 0x14, (byte)0xD8, 0x14, (byte)0xDE, 0x14, 0x3C, 0x15, 0x46, 0x15, 0x49, 0x15, 0x4D, 0x15, (byte)0x81, 0x15, (byte)0x84, 0x15, (byte)0x87, 0x15, (byte)0xA1, 0x15, (byte)0xC2, 0x15, (byte)0xC8, 0x15, (byte)0xCE, 0x15, (byte)0xD1, 0x15, 0x03, 0x16, 0x74, 0x00, 0x60, 0x00, 0x10, 0x00, (byte)0xCC, 0x03, (byte)0xC0, 0x00, 0x38, 0x00, 0x30, 0x00, 0x58, 0x00, 0x1E, 0x00, 0x50, 0x00, (byte)0x80, 0x00, 0x0E, 0x00, 0x13, 0x00, 0x60, 0x00, 0x10, 0x00, 0x0C, 0x00, 0x34, 0x00, 0x1B, 0x00, 0x10, 0x00, 0x48, 0x01, 0x2C, 0x01, 0x06, 0x00, 0x03, 0x00, 0x1A, 0x04, 0x06, 0x00, (byte)0xB0, 0x00, (byte)0xC0, 0x01,(byte) 0xC8, 0x01, 0x34, 0x00, 0x30, 0x00, (byte)0x84, 0x00, 0x16, 0x00, 0x14, 0x00, 0x3A, 0x00, 0x04, 0x00, 0x2A, 0x00, 0x08, 0x00, 0x01, 0x00, 0x02, 0x00, 0x32, 0x00, 0x01, 0x00, 0x01, 0x00, 0x18, 0x00, 0x1F, 0x00, 0x04, 0x00, 0x04, 0x00, 0x01, 0x00, 0x30, 0x00, 0x52, 0x01, (byte)0xB4, 0x41};
	byte[] mx_20_RH_0532 =	new byte[] {0x00, 0x0B, (byte) 0xF4, 0x26, 0x01, 0x05, 0x01, 0x18, 0x00, 0x0A, 0x00, 0x31, 0x00, 0x00, 0x20, 0x00, 0x30, 0x00, 0x20, 0x04, 0x20, 0x07, 0x20, 0x0A, 0x20, 0x7F, 0x20, (byte)0x86, 0x20, (byte)0xA0, 0x20, 0x5A, 0x23, 0x5D, 0x23, 0x69, 0x23, 0x02, 0x00, 0x01, 0x00, 0x01, 0x00, 0x73, 0x00, 0x05, 0x00, 0x18, 0x00,(byte) 0xB8, 0x02, 0x01, 0x00, 0x0A, 0x00, 0x34, 0x01, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, 0x00, 0x00, 0x76, 0x00, (byte) 0xD8, 0x00, (byte) 0xEA, 0x00, (byte) 0xB8, 0x04, 0x7A, 0x05, (byte)0xB4, 0x05, (byte)0xE6, 0x05, 0x40, 0x06, 0x60, 0x06, (byte)0xB2, 0x06, 0x34, 0x07, 0x44, 0x07, 0x59, 0x07, (byte)0xBB, 0x07, (byte)0xCD, 0x07, (byte)0xDB, 0x07, 0x11, 0x08, 0x2E, 0x08, 0x40, 0x08, (byte)0x8A, 0x09, (byte)0xB8, 0x0A, (byte)0xC0, 0x0A, (byte)0xC5, 0x0A, (byte)0xE1, 0x0E, (byte)0xE9, 0x0E, 0x00, 0x10, (byte)0xC2, 0x11, (byte)0x8C, 0x13, 0x0A, 0x15, (byte)0xE8, 0x13, 0x6E, 0x14, (byte)0x86, 0x14,(byte) 0x9C, 0x14, (byte)0xD8, 0x14, (byte)0xDE, 0x14, 0x3C, 0x15, 0x46, 0x15, 0x49, 0x15, 0x4D, 0x15, (byte)0x81, 0x15, (byte)0x84, 0x15, (byte)0x87, 0x15, (byte)0xA1, 0x15, (byte)0xC2, 0x15, (byte)0xC8, 0x15, (byte)0xCE, 0x15, (byte)0xD1, 0x15, 0x03, 0x16, 0x74, 0x00, 0x60, 0x00, 0x10, 0x00, (byte)0xCC, 0x03, (byte)0xC0, 0x00, 0x38, 0x00, 0x30, 0x00, 0x58, 0x00, 0x1E, 0x00, 0x50, 0x00, (byte)0x80, 0x00, 0x0E, 0x00, 0x13, 0x00, 0x60, 0x00, 0x10, 0x00, 0x0C, 0x00, 0x34, 0x00, 0x1B, 0x00, 0x10, 0x00, 0x48, 0x01, 0x2C, 0x01, 0x06, 0x00, 0x03, 0x00, 0x1A, 0x04, 0x06, 0x00, (byte)0xB0, 0x00, (byte)0xC0, 0x01, (byte)0xC8, 0x01, 0x34, 0x00, 0x30, 0x00, (byte)0x84, 0x00, 0x16, 0x00, 0x14, 0x00, 0x3A, 0x00, 0x04, 0x00, 0x2A, 0x00, 0x08, 0x00, 0x01, 0x00, 0x02, 0x00, 0x32, 0x00, 0x01, 0x00, 0x01, 0x00, 0x18, 0x00, 0x1F, 0x00, 0x04, 0x00, 0x04, 0x00, 0x01, 0x00, 0x30, 0x00, 0x52, 0x01, (byte)0xBE, (byte)0x86};
	byte[] mc_32_RH_0532 =	new byte[] {0x00, (byte)0x88, 0x77, (byte) 0x9E, 0x01, 0x05, 0x01, 0x50, 0x00, 0x0B, 0x00, 0x32, 0x00, 0x00, 0x20, 0x00, 0x30, 0x00, 0x20, 0x04, 0x20, 0x07, 0x20, 0x0A, 0x20, 0x0D, 0x20, 0x1F, 0x29, (byte)0xAA, 0x29, (byte)0xB1, 0x29, 0x03, 0x2A, 0x25, 0x2A, 0x31, 0x2A, 0x02, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x10, 0x09, (byte)0x89, 0x00, 0x05, 0x00, 0x50, 0x00, 0x20, 0x00, 0x0A, 0x00, 0x34, 0x01, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, 0x00, 0x00, 0x76, 0x00, (byte)0xD8, 0x00, (byte)0xEA, 0x00, (byte)0xB8, 0x04, 0x7A, 0x05, (byte)0xB4, 0x05, (byte)0xE6, 0x05, 0x40, 0x06, 0x60, 0x06, (byte)0xB2, 0x06, 0x34, 0x07, 0x44, 0x07, 0x59, 0x07, (byte)0xBB, 0x07, (byte)0xCD, 0x07, (byte)0xDB, 0x07, 0x11, 0x08, 0x2E, 0x08, 0x40, 0x08, (byte)0x8A, 0x09, (byte)0xB8, 0x0A, (byte)0xC0, 0x0A, (byte)0xC5, 0x0A, (byte)0xE1, 0x0E, (byte)0xE9, 0x0E, 0x00, 0x10, (byte)0xC2, 0x11, (byte)0x8C, 0x13, 0x0A, 0x15, (byte)0xE8, 0x13, 0x6E, 0x14, (byte)0x86, 0x14, (byte)0x9C, 0x14, (byte) 0xD8, 0x14, (byte)0xDE, 0x14, 0x3C, 0x15, 0x46, 0x15, 0x49, 0x15, 0x4D, 0x15, (byte)0x81, 0x15, (byte)0x84, 0x15, (byte)0x87, 0x15, (byte)0xA1, 0x15, (byte)0xC2, 0x15, (byte)0xC8, 0x15, (byte)0xCE, 0x15, (byte)0xD1, 0x15, 0x03, 0x16, 0x69, 0x17, 0x74, 0x00, 0x60, 0x00, 0x10, 0x00, (byte)0xCC, 0x03, (byte)0xC0, 0x00, 0x38, 0x00, 0x30, 0x00, 0x58, 0x00, 0x1E, 0x00, 0x50, 0x00, (byte)0x80, 0x00, 0x0E, 0x00, 0x13, 0x00, 0x60, 0x00, 0x10, 0x00, 0x0C, 0x00, 0x34, 0x00, 0x1B, 0x00, 0x10, 0x00, 0x48, 0x01, 0x2C, 0x01, 0x06, 0x00, 0x03, 0x00, 0x1A, 0x04, 0x06, 0x00, (byte)0xB0, 0x00, (byte)0xC0, 0x01, (byte)0xC8, 0x01, 0x34, 0x00, 0x30, 0x00, (byte)0x84, 0x00, 0x16, 0x00, 0x14, 0x00, 0x3A, 0x00, 0x04, 0x00, 0x2A, 0x00, 0x08, 0x00, 0x01, 0x00, 0x02, 0x00, 0x32, 0x00, 0x01, 0x00, 0x01, 0x00, 0x18, 0x00, 0x1F, 0x00, 0x04, 0x00, 0x04, 0x00, 0x01, 0x00, 0x30, 0x00, 0x64, 0x01, 0x02, 0x00, (byte)0xE0, (byte)0x87};
	byte[] mx_16_RH_0532 =	new byte[] {0x00, 0x0B, (byte)0xF4, (byte)0x92, 0x00, 0x05, 0x01, 0x14, 0x00, 0x08, 0x00, 0x10, 0x00, 0x00, 0x10, 0x00, 0x30, 0x00, 0x20, 0x22, 0x20, 0x26, 0x20, 0x29, 0x20, 0x2C, 0x20, 0x0A, 0x21, 0x0D, 0x21, 0x19, 0x21, 0x20, 0x00, 0x02, 0x00, 0x01, 0x00, 0x01, 0x00, (byte)0xDC, 0x00, 0x01, 0x00, 0x0A, 0x00, 0x34, 0x01, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, 0x00, 0x00, 0x23, 0x00, 0x2F, 0x00, 0x61, 0x00, (byte)0x81, 0x00, (byte)0x99, 0x00, 0x1F, 0x01, 0x41, 0x01, 0x53, 0x01, 0x58, 0x01, 0x68, 0x01, (byte)0xA8, 0x01, (byte)0xC6, 0x01, (byte)0xDB, 0x01, (byte)0xF1, 0x01, (byte) 0xF4, 0x01, 0x21, 0x00, 0x0A, 0x00, 0x30, 0x00, 0x1E, 0x00, 0x16, 0x00, (byte)0x84, 0x00, 0x20, 0x00, 0x10, 0x00, 0x03, 0x00, 0x0E, 0x00, 0x3E, 0x00, 0x1C, 0x00, 0x13, 0x00, 0x14, 0x00, 0x01, 0x00, 0x01, 0x00, (byte)0xD7, (byte)0xFD};

	byte[] mx_20_AM_0533 =	new byte[] {0x00, 0x51, (byte)0xAE, 0x00, 0x08, 0x05, 0x01, 0x00, 0x01, 0x63, 0x28, 0x04, 0x13, 0x22, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x03, 0x00, 0x00, 0x41, 0x72, 0x6E, 0x6F, 0x20, 0x4D, 0x61, 0x75, 0x74, 0x65, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x02, 0x00, 0x00, 0x01, 0x03, 0x01, 0x00, 0x00, 0x01, 0x01, 0x44, (byte) 0xDB, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xFF, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x01, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x41, 0x6E, 0x67, 0x65, 0x6C, 0x20, 0x20, 0x20, 0x20, 0x20, 0x44, 0x69, 0x73, 0x63, 0x75, 0x73, 0x20, 0x20, 0x20, 0x20, 0x45, 0x61, 0x73, 0x79, 0x47, 0x6C, 0x69, 0x64, 0x20, 0x20, 0x4B, 0x61, 0x65, 0x66, 0x65, 0x72, 0x20, 0x20, 0x20, 0x20, 0x45, 0x6C, 0x65, 0x78, 0x69, 0x65, 0x72, 0x20, 0x20, 0x20, 0x4C, 0x41, 0x53, 0x54, 0x20, 0x44, 0x4F, 0x57, 0x4E, 0x20, 0x53, 0x48, 0x41, 0x44, 0x4F, 0x57, 0x20, 0x20, 0x20, 0x20, 0x58, 0x70, 0x65, 0x72, 0x69, 0x65, 0x6E, 0x63, 0x65, 0x20, 0x4C, 0x6F, 0x67, 0x6F, 0x35, 0x30, 0x30, 0x20, 0x20, 0x20, 0x54, 0x45, 0x53, 0x54, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x43, 0x61, 0x72, 0x69, 0x73, 0x6D, 0x61, 0x20, 0x20, 0x20, 0x4A, 0x75, 0x6E, 0x69, 0x6F, 0x72, 0x41, 0x6E, 0x64, 0x20, 0x53, 0x6B, 0x79, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x4B, 0x61, 0x74, 0x61, 0x6E, 0x61, 0x20, 0x20, 0x20, 0x20, 0x49, 0x6E, 0x73, 0x69, 0x64, 0x65, 0x72, 0x20, 0x20, 0x20, 0x41, 0x42, 0x43, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x20, 0x47, 0x72, 0x6F, 0x62, 0x20, 0x31, 0x30, 0x39, 0x42, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x53, 0x63, 0x68, (byte) 0x81, 0x6C, 0x65, 0x72, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20};
	byte[] mx_20_RH_0533 =	new byte[] {0x00, 0x0C, (byte)0xF3, (byte)0xB8, 0x02, 0x05, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x01, 0x00, 0x01, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x54, 0x52, 0x41, 0x49, 0x4E, 0x45, 0x52, 0x20, 0x20, 0x20, 0x41, 0x53, 0x57, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x4A, 0x4F, 0x44, 0x45, 0x4C, 0x20, 0x52, 0x4F, 0x42, 0x20, 0x54, 0x41, 0x58, 0x49, 0x20, 0x43, 0x55, 0x50, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, (byte)0x8C, (byte) 0x8C, (byte) 0x84, (byte) 0x84, (byte) 0x8E, (byte) 0x8E, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x32, 0x01, 0x01, 0x38, 0x01, 0x1E, 0x00, 0x35, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05, (byte)0xFF, (byte)0xFF, 0x00, (byte)0xFF, 0x05};	
	byte[] mc_32_RH_0533 =	new byte[] {0x00, (byte)0x89, 0x76, 0x00, 0x08, 0x05, 0x01, 0x01, 0x00, 0x01, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x01, 0x01, 0x01, 0x00, 0x01, 0x01, 0x01, 0x01, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x54, 0x52, 0x36, 0x56, 0x31, 0x2D, 0x30, 0x32, 0x39, 0x20, 0x20, 0x20, 0x20, 0x4F, 0x72, 0x63, 0x61, 0x5F, 0x45, 0x58, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x54, 0x2D, 0x52, 0x36, 0x20, 0x56, 0x31, 0x30, 0x32, 0x39, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x53, 0x55, 0x4B, 0x48, 0x4F, 0x59, 0x33, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x53, 0x55, 0x4B, 0x48, 0x4F, 0x59, 0x33, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20};	
	byte[] mx_16_RH_0533 =	new byte[] {0x00, 0x0C, (byte)0xF3, (byte)0xDC, 0x00, 0x05, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x01, 0x00, 0x01, 0x01, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x54, 0x2D, 0x52, 0x45, 0x58, 0x36, 0x30, 0x30, 0x4E, 0x54, 0x2D, 0x52, 0x45, 0x58, 0x20, 0x35, 0x35, 0x30, 0x56, 0x49, 0x53, 0x49, 0x4F, 0x4E, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x45, 0x58, 0x54, 0x52, 0x41, 0x20, 0x33, 0x33, 0x30, 0x45, 0x50, 0x53, 0x49, 0x4C, 0x4F, 0x4E, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x07, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x13, 0x7A};	
	
	/**
	 * load transmitter latest model data and save to selected folder backup directory
	 * @param selectedPcFolder
	 * @param parent
	 * @throws IOException
	 * @throws TimeOutException
	 */
	public void loadModelData(String selectedPcFolder, final FileTransferTabItem parent) throws IOException, TimeOutException {
		try {
			this.preModelRead();
			
			//init header bytes
			byte[] header = new byte[4096];
			for (int i = 0; i < header.length; ++i) {
				header[i] = (byte) 0xFF;
			}
			
			StringBuilder sb = new StringBuilder();
			sendCmd(HoTTAdapterSerialPort.QUERY_TX_INFO);
			this.ANSWER_DATA = this.read(new byte[100], 2000, 5);

			sendCmd(HoTTAdapterSerialPort.QUERY_TX_INFO);
			this.ANSWER_DATA = this.read(new byte[100], 2000, 5);
//		System.arraycopy(mx_20_AM_0011, 0, this.ANSWER_DATA, 0, mx_20_AM_0011.length);
//		System.arraycopy(mx_20_RH_0011, 0, this.ANSWER_DATA, 0, mx_20_RH_0011.length);
//		System.arraycopy(mc_32_RH_0011, 0, this.ANSWER_DATA, 0, mc_32_RH_0011.length);
//		System.arraycopy(mx_16_RH_0011, 0, this.ANSWER_DATA, 0, mx_16_RH_0011.length);
			if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.OFF)) {
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.OFF, StringHelper.byte2Hex2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.OFF, StringHelper.byte2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
			}
			System.arraycopy(this.ANSWER_DATA,  7, header, 0x0000, 8);
			System.arraycopy(this.ANSWER_DATA, 56, header, 0x0008, 4);
			//System.arraycopy(this.ANSWER_DATA, 56, header, 0x0108, 4);
			
			for (int i = 23; i < 28; i++) {
				sb.append(String.format("%c", this.ANSWER_DATA[i]));
			}
			sb.append(GDE.STRING_SEMICOLON);
			if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, sb.toString());

			sendCmd(HoTTAdapterSerialPort.PREPARE_LIST_MDL);
			this.ANSWER_DATA = this.read(new byte[1000], 3000, 5);
//		System.arraycopy(mx_20_AM_0532, 0, this.ANSWER_DATA, 0, mx_20_AM_0532.length);
//		System.arraycopy(mx_20_RH_0532, 0, this.ANSWER_DATA, 0, mx_20_RH_0532.length);
//		System.arraycopy(mc_32_RH_0532, 0, this.ANSWER_DATA, 0, this.ANSWER_DATA.length);
//		System.arraycopy(mx_16_RH_0532, 0, this.ANSWER_DATA, 0, mx_16_RH_0532.length);
			if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.OFF)) {
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.FINE, StringHelper.byte2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.OFF, StringHelper.fourDigitsRunningNumber(this.ANSWER_DATA.length));
				//HoTTAdapterSerialPort.log.log(java.util.logging.Level.OFF, StringHelper.byte2FourDigitsIntegerString(this.ANSWER_DATA));
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.OFF, StringHelper.byte2Hex4CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
			}
			
			
			int numMdls = this.ANSWER_DATA[7]; //number of mdl configurations
			sb.append(numMdls).append(GDE.STRING_SEMICOLON);
			
			int startIndex = 60;
			for (; startIndex < this.ANSWER_DATA.length - 4; ++startIndex) {
				if (this.ANSWER_DATA[startIndex] == 0x00 && this.ANSWER_DATA[startIndex+1] == 0x00 && this.ANSWER_DATA[startIndex+2] == 0x00 && (this.ANSWER_DATA[startIndex+3] > 0x01 && this.ANSWER_DATA[startIndex+3] < 0xFF) && this.ANSWER_DATA[startIndex+4] == 0x00)
					break;
			}
			startIndex -= numMdls * 2 - 1;
			
			for (int j=1; startIndex < this.ANSWER_DATA.length - 2 && j <= numMdls; startIndex+=2,j++) {
				if (this.ANSWER_DATA[startIndex] == 1 || this.ANSWER_DATA[startIndex] == 0) sb.append(j);
				else 
					if (this.ANSWER_DATA[startIndex] == -1) sb.append(-1*j);
				else 
					break;
				sb.append(GDE.STRING_SEMICOLON);
			}
			sendCmd(HoTTAdapterSerialPort.QUERY_MDL_NAMES, new String(new byte[]{0x00, 0x08, 0x00, 0x00, 0x0D, 0x20, 0x00}));
			this.ANSWER_DATA = this.read(new byte[2057], 5000);
//		System.arraycopy(mx_20_AM_0533, 0, this.ANSWER_DATA, 0, mx_20_AM_0533.length);
//		System.arraycopy(mx_20_RH_0533, 0, this.ANSWER_DATA, 0, mx_20_RH_0533.length);
//		System.arraycopy(mc_32_RH_0533, 0, this.ANSWER_DATA, 0, mc_32_RH_0533.length);
//		System.arraycopy(mx_16_RH_0533, 0, this.ANSWER_DATA, 0, mx_16_RH_0533.length);
			if (HoTTAdapterSerialPort.log.isLoggable(java.util.logging.Level.OFF)) {
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.OFF, StringHelper.byte2Hex4CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
				HoTTAdapterSerialPort.log.log(java.util.logging.Level.OFF, StringHelper.byte2CharString(this.ANSWER_DATA, this.ANSWER_DATA.length));
			}

			int modelNameLength = 10;//mx-20 i=178 j%10
			switch (Transmitter.fromValue(sb.substring(0,5).toLowerCase())) {
			case MC_32:
				modelNameLength = 13;//mc-32 i=86 j%13
				startIndex = 86;
				break;
			default:
			case MC_20:
			case MX_20:
				modelNameLength = 10;
				startIndex = 177;
				break;
			case MX_16:
			case MX_12:
				modelNameLength = 9;
				startIndex = 53;
				break;
			}
			
			for (int j=0; startIndex < this.ANSWER_DATA.length - 2; startIndex++,j++) {
				if (j%(modelNameLength) != 0) {
					sb.append(String.format("%c", (char)this.ANSWER_DATA[startIndex]));
				}
				else if (j != 0) 
					sb.append(String.format("%c", (char)this.ANSWER_DATA[startIndex])).append(GDE.STRING_SEMICOLON);
			}
			sb.append(GDE.STRING_SEMICOLON);
			HoTTAdapterSerialPort.log.log(java.util.logging.Level.OFF, sb.toString());
			
			Vector<String> vModels = new Vector<String>();
			int numValidMdls = 0;
			String[] sModels = sb.toString().split(GDE.STRING_SEMICOLON);
			for (int i = 0; i < numMdls; i++) {
				if (Integer.parseInt(sModels[i+2].trim()) > 0){
					String mdlName = sModels[i+2+numMdls].trim();
					if (mdlName.length() == 0) mdlName = (i+1)+"_NAME";
					vModels.add(mdlName);
					++numValidMdls;
				}
				else vModels.add(GDE.STRING_DASH);
			}
			HoTTAdapterSerialPort.log.log(Level.OFF, vModels.size() + " - " + vModels.toString());
			String dirName = selectedPcFolder + GDE.FILE_SEPARATOR_UNIX + "backup_" + sModels[0].toLowerCase();
			FileUtils.checkDirectoryAndCreate(dirName);

			long remainingSize = 0, totalSize = 0;
			switch (Transmitter.fromValue(sb.substring(0,5).toLowerCase())) {
			default:
			case MC_32:
			case MC_20:
			case MX_20:
				remainingSize = numValidMdls * 12288;
				totalSize = numValidMdls * 12288;
				break;
			case MX_16:
			case MX_12:
				remainingSize = numValidMdls * 8192;
				totalSize = numValidMdls * 8192;
				break;
			}

			byte[] queryModels = new byte[]{0x00, 0x08, 0x00, 0x00, 0x00, 0x30, 0x00, 0x00};
			int iQueryModels = 0x30;
			for (String modelName : vModels) {
				if (!modelName.equals(GDE.STRING_DASH)) {
					String outputFile = dirName + GDE.FILE_SEPARATOR_UNIX + "a" + modelName + ".mdl";
					DataOutputStream out = new DataOutputStream(new FileOutputStream(outputFile));
					HoTTAdapterSerialPort.log.log(Level.FINE, "writing " + outputFile);

					//			switch (Transmitter.fromValue(sb.substring(0,5).toLowerCase())) {
					//			default:
					//			case MC_32:
					//				System.arraycopy(Transmitter.mc_32_APP_VERSION, 0, header, 0x08, Transmitter.mc_32_APP_VERSION.length);
					//				//System.arraycopy(Transmitter.mc_32_TxRFID, 0, header, 0x100, Transmitter.mc_32_TxRFID.length);
					//				//System.arraycopy(Transmitter.mc_32_APP_VERSION, 0, header, 0x108, Transmitter.mc_32_APP_VERSION.length);
					//				//System.arraycopy(Transmitter.mc_32_MEM_INFO, 0, header, 0x140, Transmitter.mc_32_MEM_INFO.length);
					//				break;
					//			case MC_20:
					//				System.arraycopy(Transmitter.mc_20_APP_VERSION, 0, header, 0x08, Transmitter.mc_20_APP_VERSION.length);
					//				//System.arraycopy(Transmitter.mc_20_TxRFID, 0, header, 0x100, Transmitter.mc_20_TxRFID.length);
					//				//System.arraycopy(Transmitter.mc_20_APP_VERSION, 0, header, 0x108, Transmitter.mc_20_APP_VERSION.length);
					//				//System.arraycopy(Transmitter.mc_20_MEM_INFO, 0, header, 0x140, Transmitter.mc_20_MEM_INFO.length);
					//				break;
					//			case MX_20:
					//				System.arraycopy(Transmitter.mx_20_APP_VERSION, 0, header, 0x08, Transmitter.mx_20_APP_VERSION.length);
					//				//System.arraycopy(Transmitter.mx_20_TxRFID, 0, header, 0x100, Transmitter.mx_20_TxRFID.length);
					//				//System.arraycopy(Transmitter.mx_20_APP_VERSION, 0, header, 0x108, Transmitter.mx_20_APP_VERSION.length);
					//				//System.arraycopy(Transmitter.mx_20_MEM_INFO, 0, header, 0x140, Transmitter.mx_20_MEM_INFO.length);
					//				break;
					//			case MX_16:
					//				System.arraycopy(Transmitter.mx_16_APP_VERSION, 0, header, 0x08, Transmitter.mx_16_APP_VERSION.length);
					//				//System.arraycopy(Transmitter.mx_16_TxRFID, 0, header, 0x100, Transmitter.mx_16_TxRFID.length);
					//				//System.arraycopy(Transmitter.mx_16_APP_VERSION, 0, header, 0x108, Transmitter.mx_16_APP_VERSION.length);
					//				break;
					//			case MX_12:
					//				System.arraycopy(Transmitter.mx_12_APP_VERSION, 0, header, 0x08, Transmitter.mx_12_APP_VERSION.length);
					//				//System.arraycopy(Transmitter.mx_12_TxRFID, 0, header, 0x100, Transmitter.mx_12_TxRFID.length);
					//				//System.arraycopy(Transmitter.mx_12_APP_VERSION, 0, header, 0x108, Transmitter.mx_12_APP_VERSION.length);
					//				break;
					//			}
					out.write(header);

					switch (Transmitter.fromValue(sb.substring(0, 5).toLowerCase())) {
					default:
					case MC_32:
					case MC_20:
					case MX_20:
						for (int i = 0; i < 4; i++) {
							sendMdlCmd(queryModels);
							this.ANSWER_DATA = this.read(new byte[2057], 5000);
							out.write(this.ANSWER_DATA, 7, 2048);
							iQueryModels += 8;
							queryModels[5] = (byte) ((iQueryModels & 0x00FF)       & 0xFF);
							queryModels[6] = (byte)(((iQueryModels & 0xFF00) >> 8) & 0xFF);
						}
						break;
					case MX_16:
					case MX_12:
						for (int i = 0; i < 2; i++) {
							sendMdlCmd(queryModels);
							this.ANSWER_DATA = this.read(new byte[2057], 5000);
							iQueryModels += 8;
							queryModels[5] = (byte) ((iQueryModels & 0x00FF)       & 0xFF);
							queryModels[6] = (byte)(((iQueryModels & 0xFF00) >> 8) & 0xFF);
						}
						break;
					}

					out.close();
					out = null;

					parent.updateFileTransferProgress(totalSize, remainingSize -= 12288);
				}
				else {
					switch (Transmitter.fromValue(sb.substring(0, 5).toLowerCase())) {
					default:
					case MC_32:
					case MC_20:
					case MX_20:
						for (int i = 0; i < 4; i++) {
							iQueryModels += 8;
							queryModels[5] = (byte) ((iQueryModels & 0x00FF)       & 0xFF);
							queryModels[6] = (byte)(((iQueryModels & 0xFF00) >> 8) & 0xFF);
						}
						break;
					case MX_16:
					case MX_12:
						for (int i = 0; i < 2; i++) {
							iQueryModels += 8;
							queryModels[5] = (byte) ((iQueryModels & 0x00FF)       & 0xFF);
							queryModels[6] = (byte)(((iQueryModels & 0xFF00) >> 8) & 0xFF);
						}
						break;
					}
				}
			}
		}
		finally {
			this.postModelRead();
		}
		
	}
	
	/**
	 * prepare the transmitter to transfer model data, write on screen
	 * @throws IOException
	 * @throws TimeOutException
	 */
	private void preModelRead() throws IOException, TimeOutException {
		byte[] answer = new byte[9];
		this.sendCmd(CLEAR_SCREEN);
		answer = this.read(answer, READ_TIMEOUT_MS);
		this.sendCmd(RESET_SCREEN);
		answer = this.read(answer, READ_TIMEOUT_MS);

		this.sendLine(WRITE_SCREEN, "---------------------", 0);
		answer = this.read(answer, READ_TIMEOUT_MS);
		this.sendLine(WRITE_SCREEN, "*   Model Data      *", 1);
		answer = this.read(answer, READ_TIMEOUT_MS);
		this.sendLine(WRITE_SCREEN, "*   Read Start      *", 2);
		answer = this.read(answer, READ_TIMEOUT_MS);
		this.sendLine(WRITE_SCREEN, "*   Please Wait.... *", 3);
		answer = this.read(answer, READ_TIMEOUT_MS);
		this.sendLine(WRITE_SCREEN, "---------------------", 4);
		answer = this.read(answer, READ_TIMEOUT_MS);
	}
	
	/**
	 * reset the transmitter after reading model data
	 * @throws IOException
	 * @throws TimeOutException
	 */
	private void postModelRead() throws IOException, TimeOutException {
		byte[] answer = new byte[9];
		this.sendCmd(CLOSE_SCREEN);
		answer = this.read(answer, READ_TIMEOUT_MS);
	}
}
