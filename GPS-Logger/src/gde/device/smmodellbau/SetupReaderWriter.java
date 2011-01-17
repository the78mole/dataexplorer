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
    
    Copyright (c) 2010,2011 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import gde.GDE;
import gde.device.smmodellbau.gpslogger.MessageIds;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.Checksum;
import gde.utils.StringHelper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * class implementation to read/write SM GPS-Logger configuration in binary format (firmware 1.00)
 */
public class SetupReaderWriter {
	final static Logger	log													= Logger.getLogger(SetupReaderWriter.class.getName());
	final DataExplorer	application									= DataExplorer.getInstance();
	final GPSLogger			device;
	final Shell					parent;

	final static int		TEL_ALARM_HEIGHT						= 0x0001;
	final static int		TEL_ALARM_SPEED							= 0x0002;
	final static int		TEL_ALARM_DISTANCE					= 0x0004;
	final static int		TEL_ALARM_TRIP_LENGTH				= 0x0008;
	final static int		TEL_ALARM_VOLTAGE_RX				= 0x0010;

	final static int		TEL_ALARM_CURRENT_UL				= 0x0020;
	final static int		TEL_ALARM_VOLTAGE_START_UL	= 0x0040;
	final static int		TEL_ALARM_VOLTAGE_UL				= 0x0080;
	final static int		TEL_ALARM_CAPACITY_UL				= 0x0100;

	//$SETUP,192 Byte*
	int									serialNumber								= 357;																									// 1
	int									datarate										= 0;																										// 2 0 = 10Hz, 1 = 5Hz, 2 = 2Hz, 1 = 1Hz
	int									startmodus									= 1;																										// 3 0 = „manuell“, 1 = „3D-Fix“, 2 = „ >20 m“, 3 = „>20 km/h“
	short								timeZone										= 2;																										// 4 -12 --> +12 step 1
	int									units												= 0;																										// 5
	int									varioThreshold							= 5;																										// 6  0 --> 50 step 1
	int									varioTon										= 0;																										// 7
	//short[] A = new short[10]; // 8-17
	int									telemetryAlarms							= 0x0013;																								// 18 
	int									heightAlarm									= 200;																									// 19 10m --> 4000m step 50
	int									speedAlarm									= 200;																									// 20 10km/h --> 1000km/h
	int									distanceAlarm								= 500;																									// 21 10m --> 5000m
	int									voltageRxAlarm							= 450;																									// 22 300 --> 800 V/100
	int									tripLengthAlarm							= 50;																										// 23 10km/10 --> 999km/10
	int									currentUlAlarm							= 100;																									// 24 1A --> 400A
	int									voltageStartUlAlarm					= 124;																									// 25 10V/10 --> 600V/10
	int									voltageUlAlarm							= 100;																									// 26 10V/10 --> 600V/10
	int									capacityUlAlarm							= 2000;																									// 27 100mAh --> 30000mAh
	//short[] B = new short[10]; // 28-37
	int									mLinkAddressVario						= 0;																										// 38 0 - 15, "--"
	int									mLinkAddressSpeed						= 1;																										// 39
	int									mLinkAddressDirection				= 4;																										// 40
	int									mLinkAddressHeight					= 2;																										// 41
	int									mLinkAddressDistance				= 5;																										// 42
	int									mLinkAddressTripLength			= 3;																										// 43
	//short[] C = short int[10]; // 44-53
	int									firmwareVersion							= 100;																									// 54
	//int[]								unused										= new int[192 - 55 * 2];
	short								checkSum;																																						// 55

	int[]								setupData										= new int[96];

	public SetupReaderWriter(Shell useParent, GPSLogger useDevice) {
		this.parent = useParent;
		this.device = useDevice;
	}

	void loadSetup() {
		FileDialog fd = this.application.openFileOpenDialog(this.parent, Messages.getString(MessageIds.GDE_MSGT2001), new String[] { GDE.FILE_ENDING_STAR_INI, GDE.FILE_ENDING_STAR },
				this.device.getDataBlockPreferredDataLocation(), this.device.getDefaultConfigurationFileName(), SWT.SINGLE);
		String selectedSetupFile = fd.getFilterPath() + GDE.FILE_SEPARATOR_UNIX + fd.getFileName();
		log.log(Level.FINE, "selectedSetupFile = " + selectedSetupFile); //$NON-NLS-1$

		if (fd.getFileName().length() > 4) {
			try {
				FileInputStream file_input = new FileInputStream(new File(selectedSetupFile));
				DataInputStream data_in = new DataInputStream(file_input);
				byte[] buffer = new byte[192];
				int size = data_in.read(buffer);
				data_in.close();

				if (size != 192) {
					log.log(Level.SEVERE, "error reading configuration file, data size != 192 Bytes!"); //$NON-NLS-1$
				}
				this.serialNumber 					= (buffer[1] << 8) + (buffer[0] & 0x00FF);
				this.datarate 							= (buffer[3] << 8) + (buffer[2] & 0x00FF);
				this.startmodus 						= (buffer[5] << 8) + (buffer[4] & 0x00FF);
				this.timeZone 							= (short) ((buffer[7] << 8) + (buffer[6] & 0x00FF));
				//units
				this.varioThreshold 				= (buffer[11] << 8) + (buffer[10] & 0x00FF);
				this.varioTon 							= (buffer[13] << 8) + (buffer[12] & 0x00FF);
				//A[10]
				this.telemetryAlarms 				= (buffer[35] << 8) + (buffer[34] & 0x00FF);
				this.heightAlarm 						= (buffer[37] << 8) + (buffer[36] & 0x00FF);
				this.speedAlarm 						= (buffer[39] << 8) + (buffer[38] & 0x00FF);
				this.distanceAlarm 					= (buffer[41] << 8) + (buffer[40] & 0x00FF);
				this.voltageRxAlarm 				= (buffer[43] << 8) + (buffer[42] & 0x00FF);
				this.tripLengthAlarm 				= (buffer[45] << 8) + (buffer[44] & 0x00FF);
				this.currentUlAlarm 				= (buffer[47] << 8) + (buffer[46] & 0x00FF);
				this.voltageStartUlAlarm 		= (buffer[49] << 8) + (buffer[48] & 0x00FF);
				this.voltageUlAlarm 				= (buffer[51] << 8) + (buffer[50] & 0x00FF);
				this.capacityUlAlarm 				= (buffer[53] << 8) + (buffer[52] & 0x00FF);
				//B[10]
				this.mLinkAddressVario 			= (buffer[75] << 8) + (buffer[74] & 0x00FF);
				this.mLinkAddressSpeed 			= (buffer[77] << 8) + (buffer[76] & 0x00FF);
				this.mLinkAddressDirection 	= (buffer[79] << 8) + (buffer[78] & 0x00FF);
				this.mLinkAddressHeight 		= (buffer[81] << 8) + (buffer[80] & 0x00FF);
				this.mLinkAddressDistance 	= (buffer[83] << 8) + (buffer[82] & 0x00FF);
				this.mLinkAddressTripLength = (buffer[85] << 8) + (buffer[84] & 0x00FF);
				//C[10]
				this.firmwareVersion = (buffer[107] << 8) + (buffer[106] & 0x00FF);
				//unused
				this.checkSum = (short) (((buffer[191] & 0x00FF) << 8) + (buffer[190] & 0x00FF));

				byte[] chkBuffer = new byte[192 - 2];
				System.arraycopy(buffer, 0, chkBuffer, 0, chkBuffer.length);
				short checkCRC = Checksum.CRC16(chkBuffer, 0);
				if (this.checkSum != checkCRC) {
					log.log(Level.WARNING, "Checksum missmatch!"); //$NON-NLS-1$
				}
			}
			catch (Throwable e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}

	void saveSetup() {
		FileDialog fileDialog = this.application.prepareFileSaveDialog(this.parent, Messages.getString(MessageIds.GDE_MSGT2002), new String[] { GDE.FILE_ENDING_STAR_INI, GDE.FILE_ENDING_STAR },
				this.device.getDataBlockPreferredDataLocation(), this.device.getDefaultConfigurationFileName());
		log.log(Level.FINE, "selectedSetupFile = " + fileDialog.getFileName()); //$NON-NLS-1$
		String setupFilePath = fileDialog.open();
		if (setupFilePath != null && setupFilePath.length() > 4) {
			File setupFile = new File(setupFilePath);
			byte[] buffer = new byte[192];
			int checkSum = 0;

			try {
				buffer[0] = (byte) (this.serialNumber & 0x00FF);
				buffer[1] = (byte) ((this.serialNumber & 0xFF00) >> 8);
				buffer[2] = (byte) (this.datarate & 0x00FF);
				buffer[3] = (byte) ((this.datarate & 0xFF00) >> 8);
				buffer[4] = (byte) (this.startmodus & 0x00FF);
				buffer[5] = (byte) ((this.startmodus & 0xFF00) >> 8);
				buffer[6] = (byte) (this.timeZone & 0x00FF);
				buffer[7] = (byte) ((this.timeZone & 0xFF00) >> 8);
				//units
				buffer[10] = (byte) (this.varioThreshold & 0x00FF);
				buffer[11] = (byte) ((this.varioThreshold & 0xFF00) >> 8);
				buffer[12] = (byte) (this.varioTon & 0x00FF);
				buffer[13] = (byte) ((this.varioTon & 0xFF00) >> 8);
				//A 10
				buffer[34] = (byte) (this.telemetryAlarms & 0x00FF);
				buffer[35] = (byte) ((this.telemetryAlarms & 0xFF00) >> 8);
				buffer[36] = (byte) (this.heightAlarm & 0x00FF);
				buffer[37] = (byte) ((this.heightAlarm & 0xFF00) >> 8);
				buffer[38] = (byte) (this.speedAlarm & 0x00FF);
				buffer[39] = (byte) ((this.speedAlarm & 0xFF00) >> 8);
				buffer[40] = (byte) (this.distanceAlarm & 0x00FF);
				buffer[41] = (byte) ((this.distanceAlarm & 0xFF00) >> 8);
				buffer[42] = (byte) (this.voltageRxAlarm & 0x00FF);
				buffer[43] = (byte) ((this.voltageRxAlarm & 0xFF00) >> 8);
				buffer[44] = (byte) (this.tripLengthAlarm & 0x00FF);
				buffer[45] = (byte) ((this.tripLengthAlarm & 0xFF00) >> 8);

				buffer[46] = (byte) (this.currentUlAlarm & 0x00FF);
				buffer[47] = (byte) ((this.currentUlAlarm & 0xFF00) >> 8);
				buffer[48] = (byte) (this.voltageStartUlAlarm & 0x00FF);
				buffer[49] = (byte) ((this.voltageStartUlAlarm & 0xFF00) >> 8);
				buffer[50] = (byte) (this.voltageUlAlarm & 0x00FF);
				buffer[51] = (byte) ((this.voltageUlAlarm & 0xFF00) >> 8);
				buffer[52] = (byte) (this.capacityUlAlarm & 0x00FF);
				buffer[53] = (byte) ((this.capacityUlAlarm & 0xFF00) >> 8);
				//B 10
				buffer[74] = (byte) (this.mLinkAddressVario & 0x00FF);
				buffer[75] = (byte) ((this.mLinkAddressVario & 0xFF00) >> 8);
				buffer[76] = (byte) (this.mLinkAddressSpeed & 0x00FF);
				buffer[77] = (byte) ((this.mLinkAddressSpeed & 0xFF00) >> 8);
				buffer[78] = (byte) (this.mLinkAddressDirection & 0x00FF);
				buffer[79] = (byte) ((this.mLinkAddressDirection & 0xFF00) >> 8);
				buffer[80] = (byte) (this.mLinkAddressHeight & 0x00FF);
				buffer[81] = (byte) ((this.mLinkAddressHeight & 0xFF00) >> 8);
				buffer[82] = (byte) (this.mLinkAddressDistance & 0x00FF);
				buffer[83] = (byte) ((this.mLinkAddressDistance & 0xFF00) >> 8);
				buffer[84] = (byte) (this.mLinkAddressTripLength & 0x00FF);
				buffer[85] = (byte) ((this.mLinkAddressTripLength & 0xFF00) >> 8);
				//C 10
				buffer[106] = (byte) (this.firmwareVersion & 0x00FF);
				buffer[107] = (byte) ((this.firmwareVersion & 0xFF00) >> 8);
				//unused 
				byte[] chkBuffer = new byte[192 - 2];
				System.arraycopy(buffer, 0, chkBuffer, 0, chkBuffer.length);
				checkSum = Checksum.CRC16(chkBuffer, 0);
				buffer[190] = (byte) (checkSum & 0x00FF);
				buffer[191] = (byte) ((checkSum & 0xFF00) >> 8);

				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "$SETUP," + StringHelper.convertHexInput(buffer));
				FileOutputStream file_out = new FileOutputStream(setupFile);
				DataOutputStream data_out = new DataOutputStream(file_out);
				data_out.write(buffer);
				data_out.close();
			}
			catch (Exception e) {
				log.log(Level.WARNING, "Error writing setupfile = " + fileDialog.getFileName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage()); //$NON-NLS-1$
			}
		}
	}
}