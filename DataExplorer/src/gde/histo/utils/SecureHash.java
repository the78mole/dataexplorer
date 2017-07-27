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

    Copyright (c) 2017 Thomas Eickert
****************************************************************************************/

package gde.histo.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Access to cryptographic hash functions.
 * Supports hash values rendered as hexadecimal numbers.
 * @author Thomas Eickert
 */
public final class SecureHash {

	/**
	 * @param input
	 * @return the SHA-1 hash value rendered as a hexadecimal number, 40 digits long
	 * @see <a href=" http://www.sha1-online.com/sha1-java/">Code example</a>
	 */
	public static String sha1(String input) {
		StringBuilder sb = new StringBuilder();
		byte[] hashBytes = null;
		try {
			hashBytes = MessageDigest.getInstance("SHA1").digest(input.getBytes()); //$NON-NLS-1$
			if (hashBytes != null) {
				for (byte hashByte : hashBytes) {
					sb.append(Integer.toString((hashByte & 0xff) + 0x100, 16).substring(1));
				}
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	/**
	 * @param file
	 * @return the file's full data SHA1 checksum
	 * @throws IOException
	 * @see <a href=" http://www.sha1-online.com/sha1-java/">Code example</a>
	 */
	public static String sha1(File file) throws IOException {
		StringBuilder sb = new StringBuilder();
		MessageDigest sha1Digest = null;
		try {
			sha1Digest = MessageDigest.getInstance("SHA1"); //$NON-NLS-1$

			byte[] hashBytes = null;
			try (FileInputStream fis = new FileInputStream(file)) {
				byte[] data = new byte[8192]; // most file systems are configured to use block sizes of 4096 or 8192
				int read = 0;
				while ((read = fis.read(data)) != -1) {
					sha1Digest.update(data, 0, read);
				}
				hashBytes = sha1Digest.digest();
			}
			for (byte hashByte : hashBytes) {
				sb.append(Integer.toString((hashByte & 0xff) + 0x100, 16).substring(1));
			}

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

}
