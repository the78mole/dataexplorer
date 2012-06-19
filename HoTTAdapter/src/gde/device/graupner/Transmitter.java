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

import gde.utils.StringHelper;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
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
	
	public final static byte   mc_32_BIND_INFO =  (byte) 0x05;
	public final static byte   mc_20_BIND_INFO =  (byte) 0x05;
	public final static byte   mx_20_BIND_INFO =  (byte) 0x05;
	public final static byte   mx_16_BIND_INFO =  (byte) 0x00;
	public final static byte   mx_12_BIND_INFO =  (byte) 0x00;

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

}
