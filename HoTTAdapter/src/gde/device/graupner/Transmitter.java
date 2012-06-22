/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2012 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.device.graupner.hott.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.StringHelper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public enum Transmitter {
	MC_32("mc-32"), MC_20("mc-20"), MX_20("mx-20"), MX_16("mx-16"), MX_12("mx-12"), UNSPECIFIED("unspecified");

	final static Logger						log	= Logger.getLogger(Transmitter.class.getName());

	public final static byte[] mc_32_PROD_CODE =  new byte[] {0x04, 0x34, (byte) 0xf4, 0x00, 0x05, 0x04, 0x00, 0x00};
	public final static byte[] mc_20_PROD_CODE =  new byte[] {(byte) 0xcc, 0x34, (byte) 0xf4, 0x00, (byte) 0xe8, 0x03, 0x00, 0x00};
	public final static byte[] mx_20_PROD_CODE =  new byte[] {0x74, 0x32, (byte) 0xf4, 0x00, 0x5f, 0x04, 0x00, 0x00};
	public final static byte[] mx_16_PROD_CODE =  new byte[] {(byte) 0xe4, 0x30, (byte) 0xf4, 0x00, (byte) 0xb3, 0x06, 0x00, 0x00};
	public final static byte[] mx_12_PROD_CODE =  new byte[] {0x10, 0x32, (byte) 0xf4, 0x00, 0x73, 0x06, 0x00, 0x00};
	
	public final static byte[] mc_32_APP_VERSION =  new byte[] {(byte) 0xe8, 0x03, 0x00, 0x00};
	public final static byte[] mc_20_APP_VERSION =  new byte[] {(byte) 0xea, 0x03, 0x00, 0x00};
	public final static byte[] mx_20_APP_VERSION =  new byte[] {(byte) 0xea, 0x03, 0x00, 0x00};
	public final static byte[] mx_16_APP_VERSION =  new byte[] {(byte) 0xe9, 0x03, 0x00, 0x00};
	public final static byte[] mx_12_APP_VERSION =  new byte[] {(byte) 0xe9, 0x03, 0x00, 0x00};

	public final static byte[] MEMORY_VERSION =  new byte[] {(byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff};

	public final static byte[] mc_32_TxRFID =  new byte[] {0x05, 0x51, 0x00, 0x00, 0x30, 0x38, 0x00, 0x00};
	public final static byte[] mc_20_TxRFID =  new byte[] {0x2a, 0x00,0x00,(byte) 0x80,0x7f, 0x38, 0x00, 0x00};
	public final static byte[] mx_20_TxRFID =  new byte[] {(byte) 0x3c,0x0a,0x00,0x00,0x30, 0x38, 0x00, 0x00};
	public final static byte[] mx_16_TxRFID =  new byte[] {0x03,0x00,0x00,(byte) 0x81,0x7f, 0x38, 0x00, 0x00};
	public final static byte[] mx_12_TxRFID =  new byte[] {(byte) 0x9e,0x0d,0x00,0x00,0x30, 0x38, 0x00, 0x00};

	public final static byte[] mc_32_MEM_INFO =  new byte[] {0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, (byte) 0xFF,(byte) 0xFF,(byte) 0xFF};
	public final static byte[] mc_20_MEM_INFO =  new byte[] {0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, (byte) 0x20,(byte) 0x20,(byte) 0x20};
	public final static byte[] mx_20_MEM_INFO =  new byte[] {0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, (byte) 0x20,(byte) 0x20,(byte) 0x20};
	
	public final static byte   mc_32_BIND_INFO =  (byte) 0xff;
	public final static byte   mc_20_BIND_INFO =  (byte) 0x05;
	public final static byte   mx_20_BIND_INFO =  (byte) 0x05;

	private final String	value;
	
	private Transmitter(String v) {
		this.value = v;
	}

	public String value() {
		return this.value;
	}

  public static Transmitter fromValue(String v) {
    for (Transmitter c: Transmitter.values()) {
        if (c.value.equals(v)) {
            return c;
        }
    }
    throw new IllegalArgumentException(v);
}

	/**
	 * detect transmitter type using a few identification items
	 * @param fileName
	 * @return
	 */
	public static Transmitter detectTransmitter(String fileName, String fqPathName) {
		Transmitter result = Transmitter.UNSPECIFIED;
		byte[] inBytes = new byte[0x141];
		DataInputStream in = null;
		
		try {
			in = new DataInputStream( new FileInputStream(new File(fqPathName)));
			in.read(inBytes);
			in.close();
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		
		log.log(Level.OFF, StringHelper.byte2Hex2CharString(inBytes, inBytes.length));
		if ((inBytes[0x08]&0xFF) == 0xE8) {
			boolean isMC32 = true;
			for (int i = 0; i < 2; i++) {
				if (inBytes[i] != Transmitter.mc_32_PROD_CODE[i]) {
					isMC32 = false;
					break;
				}
			}
			result = isMC32 ? Transmitter.MC_32 : Transmitter.UNSPECIFIED;
		}
		else if ((inBytes[0x08]&0xFF) == 0xEA) {
			boolean isMC20 = true, isMX20 = true;
			for (int i = 0; i < 2; i++) {
				if (inBytes[i] != Transmitter.mc_20_PROD_CODE[i]) {
					isMC20 = false;
					break;
				}
			}
			for (int i = 0; i < 2; i++) {
				if ((inBytes[0x00 + i]&0xFF) != Transmitter.mx_20_PROD_CODE[i]){
					isMX20 = false;
					break;
				}
			}
			result = isMC20 ? Transmitter.MC_20 : isMX20 ? Transmitter.MX_20 : Transmitter.UNSPECIFIED;
		}
		else if ((inBytes[0x08]&0xFF) == 0xE9) {
			boolean isMX16 = true, isMX12 = true;
			for (int i = 0; i < 2; i++) {
				if (inBytes[i] != Transmitter.mx_16_PROD_CODE[i]) {
					isMX16 = false;
					break;
				}
			}
			for (int i = 0; i < 2; i++) {
				if (inBytes[i] != Transmitter.mx_12_PROD_CODE[i]){
					isMX12 = false;
					break;
				}
			}
			result = isMX16 ? Transmitter.MX_16 : isMX12 ? Transmitter.MX_12 : Transmitter.UNSPECIFIED;
		}
		return result;
	}

	/**
	 * simple model configuration convert function based on exchanged product code and application code
	 * transmitter ID seams to be not required to replace and will be updated during use
	 * @param filepath
	 * @param target
	 */
	public static void convert2target(String filepath, Transmitter target) {
		DataInputStream in = null;
		DataOutputStream out = null;
		byte[] bytes = new byte[8192];

		try {
			filepath = filepath.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
			File inputFile = new File(filepath);
			in = new DataInputStream( new FileInputStream(inputFile));
			String outFilePath = filepath.substring(0, filepath.lastIndexOf(GDE.FILE_SEPARATOR_UNIX));
			outFilePath = outFilePath.substring(0, outFilePath.lastIndexOf(GDE.FILE_SEPARATOR_UNIX)+1) + target.value() + GDE.FILE_SEPARATOR_UNIX;
			FileUtils.checkDirectoryAndCreate(outFilePath);
			outFilePath = outFilePath+ inputFile.getName();
			File outputFile = new File(outFilePath);
			
			out = new DataOutputStream( new FileOutputStream(outputFile));
			in.read(bytes);
			switch (target) {
			case MC_32:
				System.arraycopy(Transmitter.mc_32_PROD_CODE, 0, bytes, 0x00, Transmitter.mc_32_PROD_CODE.length);
				bytes[0x08] = (byte) 0xE8;
				//System.arraycopy(mc_32_TxRFID, 0, bytes, 0x100, mc_32_TxRFID.length);
				bytes[0x108] = (byte) 0xE8;
				System.arraycopy(Transmitter.mc_32_MEM_INFO, 0, bytes, 0x140, Transmitter.mc_32_MEM_INFO.length);
				bytes[0x160] = (byte) 0xFF;
				break;
			case MC_20:
				System.arraycopy(Transmitter.mc_20_PROD_CODE, 0, bytes, 0x00, Transmitter.mc_20_PROD_CODE.length);
				bytes[0x08] = (byte) 0xEA;
				//System.arraycopy(mc_20_TxRFID, 0, bytes, 0x100, mc_20_TxRFID.length);
				bytes[0x108] = (byte) 0xEA;
				System.arraycopy(Transmitter.mc_20_MEM_INFO, 0, bytes, 0x140, Transmitter.mc_20_MEM_INFO.length);
				bytes[0x160] = (byte) 0x05;
				break;
			case MX_20:
				System.arraycopy(Transmitter.mx_20_PROD_CODE, 0, bytes, 0x00, Transmitter.mx_20_PROD_CODE.length);
				bytes[0x08] = (byte) 0xEA;
				//System.arraycopy(mx_20_TxRFID, 0, bytes, 0x100, mx_20_TxRFID.length);
				bytes[0x108] = (byte) 0xEA;
				System.arraycopy(Transmitter.mc_20_MEM_INFO, 0, bytes, 0x140, Transmitter.mc_20_MEM_INFO.length);
				break;
			case MX_16:
				System.arraycopy(Transmitter.mx_16_PROD_CODE, 0, bytes, 0x00, Transmitter.mx_16_PROD_CODE.length);
				bytes[0x08] = (byte) 0xE9;
				//System.arraycopy(mx_16_TxRFID, 0, bytes, 0x100, mx_16_TxRFID.length);
				bytes[0x108]  = (byte) 0xE9;
				//sensor
				bytes[0x11db] = (byte) 0xfd;
				bytes[0x11ef] = (byte) 0x82;
				bytes[0x11f0] = (byte) 0x81; 
				break;
			case MX_12:
				System.arraycopy(Transmitter.mx_12_PROD_CODE, 0, bytes, 0x00, Transmitter.mx_12_PROD_CODE.length);
				bytes[0x08] = (byte) 0xE9;
				//System.arraycopy(mx_12_TxRFID, 0, bytes, 0x100, mx_12_TxRFID.length);
				bytes[0x108]  = (byte) 0xE9;
				//sensor
				bytes[0x11db] = (byte) 0xdd;
				bytes[0x11ef] = (byte) 0x5d;
				bytes[0x11f0] = (byte) 0x74; 
				break;
			}
			out.write(bytes);
			byte[] rest = new byte[4096];
			int count = in.read(rest);

			//mc-32 conversion padding
			switch (target) {
			case MC_32:
			case MC_20:
			case MX_20:
				if (count > 0) {
					byte[] writable = new byte[count];
					System.arraycopy(rest, 0, writable, 0, count);
					out.write(writable);
				}
				int i = count >= 0 ? count : 0;
				for (; i < 4096; i++) {
					out.write(0xFF);
				}
				break;
			}			
			in.close();
			in = null;
			out.close();
			out = null;
			
			if (DataExplorer.getInstance() != null)
				DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI2401));
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		finally {
			try {
				if (in != null) in.close();
				if (out != null) out.close();
			}
			catch (IOException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}
}
