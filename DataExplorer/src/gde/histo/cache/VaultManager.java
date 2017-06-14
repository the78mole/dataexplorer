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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;

import gde.GDE;
import gde.config.Settings;
import gde.device.IHistoDevice;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.NotSupportedFileFormatException;
import gde.io.HistoOsdReaderWriter;
import gde.ui.DataExplorer;
import gde.utils.SecureHash;

/**
 * Vaults administration.
 * @author Thomas Eickert
 */
public final class VaultManager {

	private static Path		activeDevicePath;							// criterion for the active device version key cache
	private static String	activeDeviceKey;							// caches the version key for the active device which is calculated only if the device is changed by the user
	private static long		activeDeviceLastModified_ms;	// caches the version key for the active device which is calculated only if the device is changed by the user

	private HistoVault		vault;

	/**
	 * @param newVaultsDirectoryName directory or zip file name
	 * @return true if the vault directory name conforms to current versions of the Data Explorer / device XML, to the current channel and to user settings (e.g. sampling timespan) and to various additional attributes
	 */
	public static boolean isValidDirectory(String newVaultsDirectoryName) {
		return newVaultsDirectoryName.equals(VaultManager.getVaultsDirectoryName());
	}

	/**
	 * The vaults directory name is determined without any log file contents.
	 * This supports vault directory scanning functions in the future.
	 * @return directory or zip file name as a unique identifier encoding the data explorer version, the device xml file contents(sha1) plus channel number and some settings values
	 */
	public static String getVaultsDirectoryName() {
		String tmpSubDirectoryLongKey = String.format("%s,%s,%d,%d,%f,%f", GDE.VERSION, VaultManager.getActiveDeviceKey(), DataExplorer.getInstance().getActiveChannelNumber(), //$NON-NLS-1$
				Settings.getInstance().getSamplingTimespan_ms(), Settings.getInstance().getMinmaxQuantileDistance(), Settings.getInstance().getAbsoluteTransitionLevel());
		return SecureHash.sha1(tmpSubDirectoryLongKey);

	}

	/**
	 * @return the path to the directory or zip file
	 */
	public static Path getVaultsFolder() {
		return HistoVault.getCacheDirectory().resolve(getVaultsDirectoryName());
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
		return SecureHash.sha1(VaultManager.getVaultsDirectoryName() + String.format("%s,%d,%d,%d", newLogFileName.getFileName(), newFileLastModified_ms, newFileLength, newLogRecordSetOrdinal)); //$NON-NLS-1$
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
		Path subDirectory = Paths.get(VaultManager.getVaultsDirectoryName());
		return subDirectory.resolve(VaultManager.getVaultName(newLogFileName.getFileName(), newFileLastModified_ms, newFileLength, newLogRecordSetOrdinal));
	}

	/**
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

	/**
	 * @param trusses
	 * @param fullQualifiedFileName
	 * @return the histo vault list collected for the trusses (may contain vaults without measurements, settlements and scores)
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 * @throws DataInconsitsentException
	 */
	public static List<HistoVault> readOsd(Collection<VaultCollector> trusses, Path fullQualifiedFileName) throws IOException, NotSupportedFileFormatException, DataInconsitsentException {
		return HistoOsdReaderWriter.readHisto(fullQualifiedFileName, trusses);
	}

	/**
	 * @param trusses
	 * @param fullQualifiedFileName
	 * @return the histo vault list collected for the trusses (may contain vaults without measurements, settlements and scores)
	 * @throws DataInconsitsentException
	 * @throws IOException
	 * @throws DataTypeException
	 */
	public static List<HistoVault> readBin(Collection<VaultCollector> trusses, Path fullQualifiedFileName) throws DataInconsitsentException, IOException, DataTypeException {
		return ((IHistoDevice) DataExplorer.application.getActiveDevice()).getRecordSetFromImportFile(fullQualifiedFileName, trusses);
	}

	/**
	 * @param fullQualifiedFileName is the vault path
	 * @return the vault or null
	 * @throws JAXBException
	 */
	public HistoVault load(Path fullQualifiedFileName) throws JAXBException {
		this.vault = null;
		this.vault = (HistoVault) HistoVault.getUnmarshaller().unmarshal(fullQualifiedFileName.toFile());
		return this.vault;
	}

	/**
	 * @param inputStream is a stream to the source path
	 * @return the vault or null
	 * @throws JAXBException
	 */
	public HistoVault load(InputStream inputStream) throws JAXBException {
		this.vault = null;
		this.vault = (HistoVault) HistoVault.getUnmarshaller().unmarshal(inputStream);
		return this.vault;
	}

	/**
	 * @param newVault is the vault to be stored
	 * @param fullQualifiedFileName is the vault path
	 * @throws JAXBException
	 */
	public void store(HistoVault newVault, Path fullQualifiedFileName) throws JAXBException {
		this.vault = null;
		HistoVault.getMarshaller().marshal(this, fullQualifiedFileName.toFile());
		this.vault = newVault;
	}

	/**
	 * @param newVault is the vault to be stored
	 * @param outputStream is a stream to the target path
	 * @throws JAXBException
	 */
	public void store(HistoVault newVault, OutputStream outputStream) throws JAXBException {
		this.vault = null;
		HistoVault.getMarshaller().marshal(newVault, outputStream);
		this.vault = newVault;
	}

	/**
	 * Checks if the vault conforms to the current environment.
	 * Does not check if the file is accessible, carries the last modified timestamp and holds the recordset ordinal.
	 * @return true if the vault object conforms to current versions of the Data Explorer / device XML, to current user settings (e.g. sampling timespan) and to various additional attributes
	 */
	public boolean isValid() {
		return this.vault.vaultName.equals(getVaultName(Paths.get(this.vault.logFilePath), this.vault.logFileLastModified, this.vault.logFileLength, this.vault.logRecordSetOrdinal));
	}

	/**
	 * Checks if the vault conforms to the current environment, if the file is accessible and has the same last modified timestamp.
	 * Reads the history recordset from the file based on the active device, converts it into a vault and compares the key values.
	 * @return true if the vault's origin file exists and produces the same vault key values compared to this vault
	 * @throws DataInconsitsentException
	 * @throws NotSupportedFileFormatException
	 * @throws IOException
	 */
	public boolean isSubstantiated() throws IOException, NotSupportedFileFormatException, DataInconsitsentException {
		File file = HistoVault.getCacheDirectory().resolve(this.vault.vaultDirectory).resolve(this.vault.vaultName).toFile();
		if (this.vault.vaultName.equals(getVaultName(Paths.get(this.vault.logFilePath), file.lastModified(), file.length(), this.vault.logRecordSetOrdinal))) {
			List<VaultCollector> trusses = new ArrayList<>();
			trusses.add(new VaultCollector(this.vault));
			List<HistoVault> histoVaults = HistoOsdReaderWriter.readHisto(file.toPath(), trusses);
			return this.vault.getVaultName().equals(histoVaults.get(0).getVaultName());
		}
		else {
			return false;
		}
	}

}
