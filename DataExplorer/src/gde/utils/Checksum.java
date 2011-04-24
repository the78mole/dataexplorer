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
    
    Copyright (c) 2008,2009,2010,2011 Winfried Bruegmann
****************************************************************************************/
package gde.utils;

import java.util.zip.Adler32;
import java.util.zip.CRC32;

/**
 * checksum calculation utility class
 * @author Winfried Brügmann
 */
public class Checksum {

	/**
	 * main method to test this class
	 * @param args
	 */
	public static void main(String[] args) {
		byte[] b = new byte[] { (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B,
				(byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C,
				(byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x00 };
		byte[] b1 = new byte[] { (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C,
				(byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C,
				(byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x07 };

		System.out.println("CRC16    	= " + Integer.toHexString(Checksum.CRC16(b, 0))); //$NON-NLS-1$
		System.out.println("CRC16CCITT= " + Integer.toHexString(Checksum.CRC16CCITT(b, 0))); //$NON-NLS-1$
		System.out.println("CRC32 		= " + Integer.toHexString(Checksum.CRC32(b))); //$NON-NLS-1$
		System.out.println("Adler32  	= " + Long.toHexString(Checksum.adler32(b))); //$NON-NLS-1$
		System.out.println("ADD      	= " + Integer.toHexString(Checksum.AND(b))); //$NON-NLS-1$
		System.out.println("XOR     	= " + Integer.toHexString(Checksum.XOR(b))); //$NON-NLS-1$
		System.out.println("XOR      	= " + Integer.toHexString(Checksum.XOR(b1))); //$NON-NLS-1$

	}

	/**
	 * generator polynomial
	 */
	private static final int	poly			= 0x1021;			/* x16 + x12 + x5 + 1 generator polynomial */
	/* 0x8408 used in European X.25 */
	
	/**
	 * calculate CRC16
	 * @param bytes byte array to compute CRC16
	 * @return 16-bit CRC, unsigned
	 */
	public static short CRC16(byte[] bytes, int initValue) {
		int crc = initValue; // initial value
		int polynomial = 0x1021; // 0001 0000 0010 0001  (0, 5, 12) 
		for (byte b : bytes) {
			for (int i = 0; i < 8; i++) {
				boolean bit = ((b >> (7 - i) & 1) == 1);
				boolean c15 = ((crc >> 15 & 1) == 1);
				crc <<= 1;
				if (c15 ^ bit) crc ^= polynomial;
			}
		}
		return (short) (crc &= 0xffff);
	}

	private static int[] crcTable	= new int[256];
	static {
		// initialize lookup table
		for (int i = 0; i < 256; i++) {
			int fcs = 0;
			int d = i << 8;
			for (int k = 0; k < 8; k++) {
				if (((fcs ^ d) & 0x8000) != 0) {
					fcs = (fcs << 1) ^ poly;
				}
				else {
					fcs = (fcs << 1);
				}
				d <<= 1;
				fcs &= 0xffff;
			}
			crcTable[i] = fcs;
		}
	}

	/**
	 * calculate CRC16 with CCITT method.
	 * @param b byte array to compute CRC on
	 * @return 16-bit CRC, unsigned
	 */
	public static short CRC16CCITT(byte[] b, int initValue) {
		int crc = initValue;
		for (int i = 0; i < b.length; i++) {
			crc = (crcTable[(b[i] ^ (crc >>> 8)) & 0xff] ^ (crc << 8)) & 0xffff;
		}
		return (short) crc;
	}
	
	/**
	 * calculate CRC-32 with Java method
	 * @param b byte array to compute CRC on
	 * @return 32-bit CRC, signed
	 */
	public static int CRC32(byte[] b) {
		// create a new CRC-calculating object
		CRC32 crc = new CRC32();
		// loop, calculating CRC for each byte of the string
		for (int i = 0; i < b.length; i++) {
			crc.update(b[i]);
		}
		return (int) crc.getValue();
	}

	/**
	 * calculate Adler32 checksum
	 * @param b byte array to compute
	 * @return 64-bit result
	 */
	public static long adler32(byte[] b) {
		Adler32 digester = new Adler32();
		digester.update(b);
		return digester.getValue();
	}

	/**
	 * calculate OR bit operation
	 * @param b byte array to compute
	 * @return 8-bit result
	 */
	public static byte OR(byte[] b) {
		int value = b[0];
		for (int i = 1; i < b.length; i++) {
			value = value | b[i];
		}
		return (byte) value;
	}

	/**
	 * calculate OR bit operation
	 * @param a byte array to compute
	 * @return 8-bit result
	 */
	public static int OR(int[] a, int offset, int length) {
		int value = a[offset];
		for (int i = 1; i < length; i++) {
			value = value | a[i];
		}
		return value;
	}

	/**
	 * calculate XOR bit operation
	 * @param c character array to compute
	 * @return 8-bit result
	 */
	public static int XOR(char[] c) {
		int value = 0;
		for (int i = 0; i < c.length; i++) {
			value ^= c[i];
		}
		return value;
	}

	/**
	 * calculate XOR bit operation
	 * @param b byte array to compute
	 * @return 8-bit result
	 */
	public static int XOR(byte[] b) {
		int value = 0;
		for (int i = 0; i < b.length; i++) {
			value ^= b[i];
		}
		return value;
	}

	/**
	 * calculate XOR bit operation
	 * @param a integer array to compute
	 * @return integer result
	 */
	public static int XOR(int[] a, int offset, int length) {
		int value = a[offset];
		for (int i = offset+1; i < length; i++) {
			value ^= a[i];
		}
		return value;
	}

	/**
	 * calculate AND bit operation
	 * @param b byte array to compute
	 * @return 8-bit result
	 */
	public static byte AND(byte[] b) {
		int value = b[0];
		for (int i = 1; i < b.length; i++) {
			value = value & b[i];
		}
		return (byte) value;
	}

	/**
	 * calculate AND bit operation
	 * @param a integer array to compute
	 * @return integer result
	 */
	public static int AND(int[] a, int offset, int length) {
		int value = a[offset];
		for (int i = offset+1; i < length; i++) {
			value = value & a[i];
		}
		return value;
	}
	
	/**
	 * calculate ADD operation
	 * @param b byte array to compute
	 * @param len length of checksum if part of the byte array
	 * @return 16-bit result
	 */
	public static int ADD(byte[] b, int len) {
		int value = b[0] & 0xFF;
		for (int i = 1; i < b.length - len; i++) {
			value = value  + (b[i] & 0xFF);
		}
		return value;
	}

	/**
	 * calculate ADD operation
	 * @param b byte array to compute
	 * @param len used for calculation, attention checksum if part of the byte array
	 * @return 16-bit result
	 */
	public static int ADD(byte[] b, int start, int len) {
		int value = b[start] & 0xFF;
		for (int i = start+1; i <= len; i++) {
			value = value  + (b[i] & 0xFF);
		}
		return value;
	}

	/**
	 * calculate ADD operation
	 * @param a integer array to compute
	 * @param length length of checksum if part of the byte array
	 * @return integer result
	 */
	public static long ADD(int[] a, int offset, int length) {
		long value = a[offset];
		for (int i = offset+1; i <= length; i++) {
			value = value  + a[i];
		}
		return value;
	}

}
