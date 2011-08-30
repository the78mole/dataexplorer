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
package gde.device.smmodellbau;

import gde.GDE;
import gde.device.smmodellbau.unilog2.MessageIds;
import gde.io.DataParser;
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
 * class implementation to read/write SM GPS-Logger configuration in binary format (firmware 1.03)
 */
public class UniLog2SetupReaderWriter {
	final static Logger	log											= Logger.getLogger(UniLog2SetupReaderWriter.class.getName());
	final DataExplorer	application							= DataExplorer.getInstance();
	final UniLog2				device;
	final Shell					parent;

	final static int		TEL_ALARM_CURRENT				= 0x0001;
	final static int		TEL_ALARM_VOLTAGE_START	= 0x0002;
	final static int		TEL_ALARM_VOLTAGE				= 0x0004;
	final static int		TEL_ALARM_CAPACITY			= 0x0008;
	final static int		TEL_ALARM_HEIGHT				= 0x0010;
	final static int		TEL_ALARM_VOLTAGE_RX		= 0x0020;
	final static int		TEL_ALARM_VOLTAGE_CELL	= 0x0040;
	
	final static int		AUTO_START_CURRENT			= 0x0001;
	final static int		AUTO_START_RX						= 0x0002;
	final static int		AUTO_START_TIME					= 0x0004;

	//$UL2SETUP,192 Byte*
	short								serialNumber						= 357;																												// 1
	short								firmwareVersion					= 103;																												// 2
	short								dataRate								= 2;																													// 3 0=50Hz, 1=20Hz, 2=10Hz, 3=5Hz, 4=4Hz, 5=1Hz
	short								startModus							= 1;																													// 4 AUTO_STROM=0x0001, AUTO_RX=0x0002, AUTO_TIME=0x0004
	short								startCurrent						= 3;																													// 5 1 - 50 A
	short								startRx									= 15;																													// 6 value/10 msec
	short								startTime								= 5;																													// 7 5 - 90 sec
	short								currentSensorType				= 1;																													// 8 0=20A, 1=40/80A, 2=150A, 3=400A
	short								modusA1									= 0;																													// 9 0=Temperature, 1=MilliVolt, 2=SpeedSensor 250, 3=SpeedSensor 450, 4=Temperature PT1000
	short								modusA2									= 0;																													// 10 0=Temperature, 1=MilliVolt, 2=SpeedSensor 250, 3=SpeedSensor 450, 4=Temperature PT1000
	short								modusA3									= 0;																													// 11 0=Temperature, 1=MilliVolt, 2=SpeedSensor 250, 3=SpeedSensor 450, 4=Temperature PT1000
	short								numberProb_MotorPole		= 2;																													// 12 value propeller blade, value*2 = motor pols
	short								gearFactor							= 100;																												// 13 value/100
	short								varioThreshold					= 5;																													// 14 value/10 m/sec
	short								varioTon								= 0;																													// 15 0=off, 1=up/down, 2=up, 3=down
	short								limiterModus						= 0;																													// 16 0=off, 1=F1Q, 2=F5D, 3=F5B, 4= F5J
	short								energyLimit							= 1000;																											// 17 1 - 2000 Wmin
	short								minMaxRx								= 0;																													// 18 0=off, 1=on
	short								stopModus								= 0;																													// 19 0=off, 1=on
	//short[] A = new short[18]; // 20-37
	short								telemetryAlarms					= 0x0000;																										// 38 current=0x0001, startVoltage=0x0002, voltage=0x0004, capacity=0x0008, height=0x0010, voltageRx=0x0020, cellVoltage=0x0040
	short								currentAlarm						= 100;																												// 39 1A --> 400A
	short								voltageStartAlarm				= 124;																												// 40 10V/10 --> 600V/10
	short								voltageAlarm						= 100;																												// 41 10V/10 --> 600V/10
	short								capacityAlarm						= 2000;																											// 42 100mAh --> 30000mAh
	short								heightAlarm							= 200;																												// 43 10m --> 4000m step 50
	short								voltageRxAlarm					= 450;																												// 44 300 --> 800 V/100
	short								cellVoltageAlarm				= 30;																												// 45 20 - 40 V/10 
	//short[] B = new short[19]; // 46-64
	byte								mLinkAddressVoltage			= 0;																													// 
	byte								mLinkAddressCurrent			= 1;																													// 65 0 - 15, "--"
	byte								mLinkAddressRevolution	= 2;																													// 
	byte								mLinkAddressCapacity		= 3;																													// 66 0 - 15, "--"
	byte								mLinkAddressVario				= 4;																													// 
	byte								mLinkAddressHeight			= 5;																													// 67 0 - 15, "--"
	byte								mLinkAddressA1					= 6;																													// 
	byte								mLinkAddressA2					= 7;																													// 68 0 - 15, "--"
	byte								mLinkAddressA3					= 8;																													// 
	byte								reserve									= 16;																												// 69
	byte								mLinkAddressCell1				= 16;																												// 70 0 - 15, "--"
	byte								mLinkAddressCell2				= 16;																												// 
	byte								mLinkAddressCell3				= 16;																												// 71 0 - 15, "--"
	byte								mLinkAddressCell4				= 16;																												// 
	byte								mLinkAddressCell5				= 16;																												// 72 0 - 15, "--"
	byte								mLinkAddressCell6				= 16;																												//
	//short[] C = new short[190/2 - 72]; // 73-95
	short								checkSum;																																						// 55

	short[]							setupData								= new short[192];

	public UniLog2SetupReaderWriter(Shell useParent, UniLog2 useDevice) {
		this.parent = useParent;
		this.device = useDevice;
	}

	void loadSetup() {
		FileDialog fd = this.application.openFileOpenDialog(this.parent, Messages.getString(MessageIds.GDE_MSGT2501), new String[] { GDE.FILE_ENDING_STAR_INI, GDE.FILE_ENDING_STAR },
				this.device.getDataBlockPreferredDataLocation(), this.device.getDefaultConfigurationFileName(), SWT.SINGLE);
		String selectedSetupFile = fd.getFilterPath() + GDE.FILE_SEPARATOR_UNIX + fd.getFileName();
		log.log(java.util.logging.Level.FINE, "selectedSetupFile = " + selectedSetupFile); //$NON-NLS-1$

		if (fd.getFileName().length() > 4) {
			try {
				FileInputStream file_input = new FileInputStream(new File(selectedSetupFile));
				DataInputStream data_in = new DataInputStream(file_input);
				byte[] buffer = new byte[192];
				int size = data_in.read(buffer);
				data_in.close();

				if (size != 192) {
					log.log(java.util.logging.Level.SEVERE, "error reading configuration file, data size != 192 Bytes!"); //$NON-NLS-1$
				}
				this.serialNumber = DataParser.parse2Short(buffer, 0);
				this.firmwareVersion = DataParser.parse2Short(buffer, 2);
				this.dataRate = DataParser.parse2Short(buffer, 4); //0=50Hz, 1=20Hz, 2=10Hz, 3=5Hz, 4=4Hz, 5=1Hz
				this.startModus = DataParser.parse2Short(buffer, 6); //AUTO_STROM=0x0001, AUTO_RX=0x0002, AUTO_TIME=0x0004
				this.startCurrent = DataParser.parse2Short(buffer, 8); //1 - 50 A
				this.startRx = DataParser.parse2Short(buffer, 10); //value/10 msec
				this.startTime = DataParser.parse2Short(buffer, 12); //5 - 90 sec
				this.currentSensorType = DataParser.parse2Short(buffer, 14); //0=20A, 1=40/80A, 2=150A, 3=400A
				this.modusA1 = DataParser.parse2Short(buffer, 16); //0=Temperature, 1=MilliVolt, 2=SpeedSensor 250, 3=SpeedSensor 450, 4=Temperature PT1000
				this.modusA2 = DataParser.parse2Short(buffer, 18); //0=Temperature, 1=MilliVolt, 2=SpeedSensor 250, 3=SpeedSensor 450, 4=Temperature PT1000
				this.modusA3 = DataParser.parse2Short(buffer, 20); //0=Temperature, 1=MilliVolt, 2=SpeedSensor 250, 3=SpeedSensor 450, 4=Temperature PT1000
				this.numberProb_MotorPole = DataParser.parse2Short(buffer, 22); //value propeller blade, value*2 motor pols
				this.gearFactor = DataParser.parse2Short(buffer, 24); //value/100
				this.varioThreshold = DataParser.parse2Short(buffer, 26); //14 value/10 m/sec
				this.varioTon = DataParser.parse2Short(buffer, 28); //0=off, 1=up/down, 2=up, 3=down
				this.limiterModus = DataParser.parse2Short(buffer, 30); //0=off, 1=F1Q, 2=F5D, 3=F5B, 4= F5J
				this.energyLimit = DataParser.parse2Short(buffer, 32); //1 - 2000 Wmin
				this.minMaxRx = DataParser.parse2Short(buffer, 34); //18 0=off, 1=on
				this.stopModus = DataParser.parse2Short(buffer, 36); //18 0=off, 1=on
				//short[] A = new short[20]; // 20-37
				this.telemetryAlarms = DataParser.parse2Short(buffer, 74); //current=0x0001, startVoltage=0x0002, voltage=0x0004, capacity=0x0008, height=0x0010, voltageRx=0x0020, cellVoltage=0x0040
				this.currentAlarm = DataParser.parse2Short(buffer, 76); //1A --> 400A
				this.voltageStartAlarm = DataParser.parse2Short(buffer, 78); //10V/10 --> 600V/10
				this.voltageAlarm = DataParser.parse2Short(buffer, 80); //10V/10 --> 600V/10
				this.capacityAlarm = DataParser.parse2Short(buffer, 82); //100mAh --> 30000mAh
				this.heightAlarm = DataParser.parse2Short(buffer, 84); //10m --> 4000m step 50
				this.voltageRxAlarm = DataParser.parse2Short(buffer, 86); //44 300 --> 800 V/100
				this.cellVoltageAlarm = DataParser.parse2Short(buffer, 88); //45 20 - 40 V/10 
				//short[] B = new short[19]; // 46-64
				this.mLinkAddressVoltage = buffer[128]; //0 - 15, "--"
				this.mLinkAddressCurrent = buffer[129]; //0 - 15, "--"
				this.mLinkAddressRevolution = buffer[130]; //0 - 15, "--"
				this.mLinkAddressCapacity = buffer[131]; //0 - 15, "--"
				this.mLinkAddressVario = buffer[132]; //0 - 15, "--"
				this.mLinkAddressHeight = buffer[133]; //0 - 15, "--"
				this.mLinkAddressA1 = buffer[134]; //0 - 15, "--"
				this.mLinkAddressA2 = buffer[135]; //0 - 15, "--"
				this.mLinkAddressA3 = buffer[136]; //0 - 15, "--"
				this.reserve = buffer[137]; //0 - 15, "--"
				this.mLinkAddressCell1 = buffer[138]; //0 - 15, "--"
				this.mLinkAddressCell2 = buffer[139]; //0 - 15, "--"
				this.mLinkAddressCell3 = buffer[140]; //0 - 15, "--"
				this.mLinkAddressCell4 = buffer[141]; //0 - 15, "--"
				this.mLinkAddressCell5 = buffer[142]; //0 - 15, "--"
				this.mLinkAddressCell6 = buffer[143]; //0 - 15, "--"
				//short[] C = new short[190/2 - 72]; // 73-95
				this.checkSum = (short) (((buffer[191] & 0x00FF) << 8) + (buffer[190] & 0x00FF));

				if (log.isLoggable(java.util.logging.Level.FINE))
					log.log(java.util.logging.Level.FINE, "$UL2SETUP," + StringHelper.byte2Hex2CharString(buffer)); //$NON-NLS-1$
				byte[] chkBuffer = new byte[192 - 2];
				System.arraycopy(buffer, 0, chkBuffer, 0, chkBuffer.length);
				short checkCRC = Checksum.CRC16(chkBuffer, 0);
				if (this.checkSum != checkCRC) {
					log.log(java.util.logging.Level.WARNING, "Checksum missmatch!"); //$NON-NLS-1$
				}
			}
			catch (Throwable e) {
				log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
			}
		}
	}

	void saveSetup() {
		FileDialog fileDialog = this.application.prepareFileSaveDialog(this.parent, Messages.getString(MessageIds.GDE_MSGT2502), new String[] { GDE.FILE_ENDING_STAR_INI, GDE.FILE_ENDING_STAR },
				this.device.getDataBlockPreferredDataLocation(), this.device.getDefaultConfigurationFileName());
		log.log(java.util.logging.Level.FINE, "selectedSetupFile = " + fileDialog.getFileName()); //$NON-NLS-1$
		String setupFilePath = fileDialog.open();
		if (setupFilePath != null && setupFilePath.length() > 4) {
			File setupFile = new File(setupFilePath);
			byte[] buffer = new byte[192];
			int tmpCheckSum = 0;

			try {
				buffer[0] = (byte) (this.serialNumber & 0x00FF);
				buffer[1] = (byte) ((this.serialNumber & 0xFF00) >> 8);
				buffer[2] = (byte) (this.firmwareVersion & 0x00FF);
				buffer[3] = (byte) ((this.firmwareVersion & 0xFF00) >> 8);
				buffer[4] = (byte) (this.dataRate & 0x00FF); //0=50Hz, 1=20Hz, 2=10Hz, 3=5Hz, 4=4Hz, 5=1Hz
				buffer[5] = (byte) ((this.dataRate & 0xFF00) >> 8);
				buffer[6] = (byte) (this.startModus & 0x00FF); //AUTO_STROM=0x0001, AUTO_RX=0x0002, AUTO_TIME=0x0004
				buffer[7] = (byte) ((this.startModus & 0xFF00) >> 8);
				buffer[8] = (byte) (this.startCurrent & 0x00FF); //1 - 50 A
				buffer[9] = (byte) ((this.startCurrent & 0xFF00) >> 8);
				buffer[10] = (byte) (this.startRx & 0x00FF); //value/10 msec
				buffer[11] = (byte) ((this.startRx & 0xFF00) >> 8);
				buffer[12] = (byte) (this.startTime & 0x00FF); //5 - 90 sec
				buffer[13] = (byte) ((this.startTime & 0xFF00) >> 8);
				buffer[14] = (byte) (this.currentSensorType & 0x00FF); //0=20A, 1=40/80A, 2=150A, 3=400A
				buffer[15] = (byte) ((this.currentSensorType & 0xFF00) >> 8);
				buffer[16] = (byte) (this.modusA1 & 0x00FF); //0=Temperature, 1=MilliVolt, 2=SpeedSensor 250, 3=SpeedSensor 450, 4=Temperature PT1000
				buffer[17] = (byte) ((this.modusA1 & 0xFF00) >> 8);
				buffer[18] = (byte) (this.modusA2 & 0x00FF); //0=Temperature, 1=MilliVolt, 2=SpeedSensor 250, 3=SpeedSensor 450, 4=Temperature PT1000
				buffer[19] = (byte) ((this.modusA2 & 0xFF00) >> 8);
				buffer[20] = (byte) (this.modusA3 & 0x00FF); //0=Temperature, 1=MilliVolt, 2=SpeedSensor 250, 3=SpeedSensor 450, 4=Temperature PT1000
				buffer[21] = (byte) ((this.modusA3 & 0xFF00) >> 8);
				buffer[22] = (byte) (this.numberProb_MotorPole & 0x00FF); //value propeller blade, value*2 motor pols
				buffer[23] = (byte) ((this.numberProb_MotorPole & 0xFF00) >> 8);
				buffer[24] = (byte) (this.gearFactor & 0x00FF); //value/100
				buffer[25] = (byte) ((this.gearFactor & 0xFF00) >> 8);
				buffer[26] = (byte) (this.varioThreshold & 0x00FF); //14 value/10 m/sec
				buffer[27] = (byte) ((this.varioThreshold & 0xFF00) >> 8);
				buffer[28] = (byte) (this.varioTon & 0x00FF); //0=off, 1=up/down, 2=up, 3=down
				buffer[29] = (byte) ((this.varioTon & 0xFF00) >> 8);
				buffer[30] = (byte) (this.limiterModus & 0x00FF); //0=off, 1=F1Q, 2=F5D, 3=F5B, 4= F5J
				buffer[31] = (byte) ((this.limiterModus & 0xFF00) >> 8);
				buffer[32] = (byte) (this.energyLimit & 0x00FF); //1 - 2000 Wmin
				buffer[33] = (byte) ((this.energyLimit & 0xFF00) >> 8);
				buffer[34] = (byte) (this.minMaxRx & 0x00FF); //18 0=off, 1=on
				buffer[35] = (byte) ((this.minMaxRx & 0xFF00) >> 8);
				buffer[36] = (byte) (this.stopModus & 0x00FF); //19 0=off, 1=on
				buffer[37] = (byte) ((this.stopModus & 0xFF00) >> 8);
				//short[] A = new short[20]; // 20-37
				buffer[74] = (byte) (this.telemetryAlarms & 0x00FF); //current=0x0001, startVoltage=0x0002, voltage=0x0004, capacity=0x0008, height=0x0010, voltageRx=0x0020, cellVoltage=0x0040
				buffer[75] = (byte) ((this.telemetryAlarms & 0xFF00) >> 8);
				buffer[76] = (byte) (this.currentAlarm & 0x00FF); //1A --> 400A
				buffer[77] = (byte) ((this.currentAlarm & 0xFF00) >> 8);
				buffer[78] = (byte) (this.voltageStartAlarm & 0x00FF); //10V/10 --> 600V/10
				buffer[79] = (byte) ((this.voltageStartAlarm & 0xFF00) >> 8);
				buffer[80] = (byte) (this.voltageAlarm & 0x00FF); //10V/10 --> 600V/10
				buffer[81] = (byte) ((this.voltageAlarm & 0xFF00) >> 8);
				buffer[82] = (byte) (this.capacityAlarm & 0x00FF); //100mAh --> 30000mAh
				buffer[83] = (byte) ((this.capacityAlarm & 0xFF00) >> 8);
				buffer[84] = (byte) (this.heightAlarm & 0x00FF); //10m --> 4000m step 50
				buffer[85] = (byte) ((this.heightAlarm & 0xFF00) >> 8);
				buffer[86] = (byte) (this.voltageRxAlarm & 0x00FF); //300 --> 800 V/100
				buffer[87] = (byte) ((this.voltageRxAlarm & 0xFF00) >> 8);
				buffer[88] = (byte) (this.cellVoltageAlarm & 0x00FF); //20 - 40 V/10 
				buffer[89] = (byte) ((this.cellVoltageAlarm & 0xFF00) >> 8);
				//short[] B = new short[19]; // 46-64
				buffer[128] = this.mLinkAddressVoltage; //0 - 15, "--"
				buffer[129] = this.mLinkAddressCurrent; //0 - 15, "--"
				buffer[130] = this.mLinkAddressRevolution; //0 - 15, "--"
				buffer[131] = this.mLinkAddressCapacity; //0 - 15, "--"
				buffer[132] = this.mLinkAddressVario; //0 - 15, "--"
				buffer[133] = this.mLinkAddressHeight; //0 - 15, "--"
				buffer[134] = this.mLinkAddressA1; //0 - 15, "--"
				buffer[135] = this.mLinkAddressA2; //0 - 15, "--"
				buffer[136] = this.mLinkAddressA3; //0 - 15, "--"
				buffer[137] = this.reserve; //0 - 15, "--"
				buffer[138] = this.mLinkAddressCell1; //0 - 15, "--"
				buffer[139] = this.mLinkAddressCell2; //0 - 15, "--"
				buffer[140] = this.mLinkAddressCell3; //0 - 15, "--"
				buffer[141] = this.mLinkAddressCell4; //0 - 15, "--"
				buffer[142] = this.mLinkAddressCell5; //0 - 15, "--"
				buffer[143] = this.mLinkAddressCell6; //0 - 15, "--"
				//short[] C = new short[190/2 - 72]; 					// 73-95
				byte[] chkBuffer = new byte[192 - 2];
				System.arraycopy(buffer, 0, chkBuffer, 0, chkBuffer.length);
				tmpCheckSum = Checksum.CRC16(chkBuffer, 0);
				buffer[190] = (byte) (tmpCheckSum & 0x00FF);
				buffer[191] = (byte) ((tmpCheckSum & 0xFF00) >> 8);

				if (log.isLoggable(java.util.logging.Level.FINE))
					log.log(java.util.logging.Level.FINE, "$UL2SETUP," + StringHelper.byte2Hex2CharString(buffer)); //$NON-NLS-1$
				FileOutputStream file_out = new FileOutputStream(setupFile);
				DataOutputStream data_out = new DataOutputStream(file_out);
				data_out.write(buffer);
				data_out.close();
			}
			catch (Throwable e) {
				log.log(java.util.logging.Level.WARNING, "Error writing setupfile = " + fileDialog.getFileName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage()); //$NON-NLS-1$
			}
		}
	}
}