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

package gde.histo.cache;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import gde.GDE;
import gde.config.Settings;
import gde.histo.utils.SecureHash;
import gde.ui.DataExplorer;

/**
 * The key attributes for vaults.
 * @author Thomas Eickert (USER)
 */
public final class VaultAttributes {

	private static Path		activeDevicePath;							// criterion for the active device version key cache
	private static String	activeDeviceKey;							// caches the version key for the active device which is calculated only if the device is changed by the user
	private static long		activeDeviceLastModified_ms;	// caches the version key for the active device which is calculated only if the device is changed by the user

	public static Path getCacheDirectory() {
		return Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_CACHE_ENTRIES_DIR_NAME);
	}

	/**
	 * @param newVaultsDirectoryName directory or zip file name
	 * @return true if the vault directory name conforms to current versions of the Data Explorer / device XML, to the current channel and to user settings (e.g. sampling timespan) and to various additional attributes
	 */
	public static boolean isValidDirectory(String newVaultsDirectoryName) {
		return newVaultsDirectoryName.equals(getVaultsDirectoryName());
	}

	/**
	 * The vaults directory name is determined without any log file contents.
	 * This supports vault directory scanning functions in the future.
	 * @return directory or zip file name as a unique identifier encoding the data explorer version, the device xml file contents(sha1) plus channel number and some settings values
	 */
	public static String getVaultsDirectoryName() {
		String tmpSubDirectoryLongKey = String.format("%s,%s,%d,%d,%f,%f", GDE.VERSION, getActiveDeviceKey(), DataExplorer.getInstance().getActiveChannelNumber(), //$NON-NLS-1$
				Settings.getInstance().getSamplingTimespan_ms(), Settings.getInstance().getMinmaxQuantileDistance(), Settings.getInstance().getAbsoluteTransitionLevel());
		return SecureHash.sha1(tmpSubDirectoryLongKey);

	}

	/**
	 * @return the path to the directory or zip file
	 */
	public static Path getVaultsFolder() {
		return getCacheDirectory().resolve(getVaultsDirectoryName());
	}

	/**
	 * The vaults file name is determined without any log file contents and without file location properties (path).
	 * This supports creating a truss or accessing a vault without having read the original log file.
	 * @param newLogFileName file name + lastModified + newFileLength are a simple solution for getting a SHA-1 hash from the file contents
	 * @param newFileLastModified_ms
	 * @param newFileLength in bytes
	 * @param newLogRecordSetOrdinal identifies multiple recordsets in one single file (0-based)
	 * @return the file name as a unique identifier (sha1)
	 */
	public static String getVaultName(Path newLogFileName, long newFileLastModified_ms, long newFileLength, int newLogRecordSetOrdinal) {
		return SecureHash.sha1(getVaultsDirectoryName() + String.format("%s,%d,%d,%d", newLogFileName.getFileName(), newFileLastModified_ms, newFileLength, newLogRecordSetOrdinal)); //$NON-NLS-1$
	}

	/**
	 * @param newLogFileName file name + lastModified + newFileLength are a simple solution for getting a SHA-1 hash from the file contents
	 * @param newFileLastModified_ms
	 * @param newFileLength in bytes
	 * @param newLogRecordSetOrdinal identifies multiple recordsets in one single file (0-based)
	 * @return the path with filename as a unique identifier (sha1)
	 */
	public static Path getVaultRelativePath(Path newLogFileName, long newFileLastModified_ms, long newFileLength, int newLogRecordSetOrdinal) {
		// do not include as these attributes are determined after reading the histoset: logChannelNumber, logObjectKey, logStartTimestampMs
		Path subDirectory = Paths.get(getVaultsDirectoryName());
		return subDirectory.resolve(getVaultName(newLogFileName.getFileName(), newFileLastModified_ms, newFileLength, newLogRecordSetOrdinal));
	}

	/**
	 * Supports key caching which is essential for the system performance.
	 * @return sha1 key as a unique identifier for the device xml file contents
	 */
	public static String getActiveDeviceKey() {
		File file = new File(DataExplorer.getInstance().getActiveDevice().getPropertiesFileName());
		if (activeDeviceKey == null || activeDevicePath == null || !activeDevicePath.equals(file.toPath()) || activeDeviceLastModified_ms != file.lastModified()) {
			try {
				activeDeviceKey = SecureHash.sha1(file);
				activeDevicePath = file.toPath();
				activeDeviceLastModified_ms = file.lastModified();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return activeDeviceKey;
	}

}
