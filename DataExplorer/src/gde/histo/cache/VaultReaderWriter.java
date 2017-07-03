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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

import gde.GDE;
import gde.config.Settings;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.NotSupportedFileFormatException;
import gde.histo.datasources.HistoSetCollector.ProgressManager;
import gde.histo.device.IHistoDevice;
import gde.histo.io.HistoOsdReaderWriter;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;

/**
 * File system access for vaults and trusses.
 * @author Thomas Eickert (USER)
 */
public final class VaultReaderWriter {
	private final static String				$CLASS_NAME	= VaultReaderWriter.class.getName();
	private final static Logger				log					= Logger.getLogger($CLASS_NAME);

	private final static DataExplorer	application	= DataExplorer.getInstance();
	private final static Settings			settings		= Settings.getInstance();

	/**
	 * Read file and populate the vault from the recordset.
	 * @param filePath is the actual file path, not the path to the link file
	 * @param trusses
	 * @throws DataTypeException for the bin file reader only
	 * @throws DataInconsitsentException
	 * @throws NotSupportedFileFormatException
	 * @throws IOException
	 * @return the vaults extracted from the file based on the input trusses
	 */
	public static List<ExtendedVault> loadVaultsFromFile(Path filePath, Map<String, VaultCollector> trusses)
			throws IOException, NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {
		List<ExtendedVault> histoVaults = null;

		final String supportedImportExtention = application.getActiveDevice() instanceof IHistoDevice ? ((IHistoDevice) application.getActiveDevice()).getSupportedImportExtention() : GDE.STRING_EMPTY;
		try {
			if (!supportedImportExtention.isEmpty() && filePath.toString().endsWith(supportedImportExtention))
				histoVaults = ((IHistoDevice) DataExplorer.application.getActiveDevice()).getRecordSetFromImportFile(filePath, trusses.values());
			else if (filePath.toString().endsWith(GDE.FILE_ENDING_DOT_OSD))
				histoVaults = HistoOsdReaderWriter.readVaults(filePath, trusses.values());
			else
				throw new IllegalArgumentException();
		}
		catch (Exception e) {
			histoVaults = new ArrayList<ExtendedVault>();
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			log.log(Level.INFO, String.format("invalid file format: %s  channelNumber=%d  %s", //$NON-NLS-1$
					application.getActiveDevice().getName(), application.getActiveChannelNumber(), filePath));
		}

		return histoVaults;
	}

	/**
	 * Read cached vaults and reduce the trussJobs map.
	 * @param trussJobs with the actual path (not the link file path) and a map of vault skeletons (the key vaultFileName prevents double entries)
	 * @return the vaults and trusses loaded from the cache
	 * @throws IOException during opening or traversing the zip file
	 */
	public static synchronized List<ExtendedVault> loadVaultsFromCache(Map<Path, Map<String, VaultCollector>> trussJobs, ProgressManager progress) throws IOException { // syn due to SAXException: FWK005 parse may not be called while parsing.
		List<ExtendedVault> vaults = new ArrayList<>();

		Path cacheFilePath = ExtendedVault.getVaultsFolder();
		log.log(Level.FINER, "cacheFilePath=", cacheFilePath); //$NON-NLS-1$
		VaultProxy vaultIO = new VaultProxy();
		if (VaultReaderWriter.settings.isZippedCache() && FileUtils.checkFileExist(cacheFilePath.toString())) {
			try (ZipFile zf = new ZipFile(cacheFilePath.toFile())) { // closing the zip file closes all streams
				Iterator<Map.Entry<Path, Map<String, VaultCollector>>> trussJobsIterator = trussJobs.entrySet().iterator();
				while (trussJobsIterator.hasNext()) {
					final Map<String, VaultCollector> map = trussJobsIterator.next().getValue();
					final Iterator<Map.Entry<String, VaultCollector>> trussesIterator = map.entrySet().iterator();
					while (trussesIterator.hasNext()) {
						progress.countInLoop(1);
						VaultCollector truss = trussesIterator.next().getValue();
						if (zf.getEntry(truss.getVault().getVaultName()) != null) {
							HistoVault histoVault = null;
							try {
								histoVault = vaultIO.load(new BufferedInputStream(zf.getInputStream(zf.getEntry(truss.getVault().getVaultName()))));
							}
							catch (Exception e) {
								log.log(Level.SEVERE, e.getMessage(), e);
							}

							if (histoVault != null) {
								vaults.add(histoVault.getExtendedVault());
							}
							trussesIterator.remove();
						}
					}
					if (map.size() == 0) trussJobsIterator.remove();
				}
			}
		}
		else if (!VaultReaderWriter.settings.isZippedCache() && FileUtils.checkDirectoryAndCreate(cacheFilePath.toString())) {
			Iterator<Map.Entry<Path, Map<String, VaultCollector>>> trussJobsIterator = trussJobs.entrySet().iterator();
			while (trussJobsIterator.hasNext()) {
				final Map<String, VaultCollector> map = trussJobsIterator.next().getValue();
				final Iterator<Map.Entry<String, VaultCollector>> trussesIterator = map.entrySet().iterator();
				while (trussesIterator.hasNext()) {
					progress.countInLoop(1);
					VaultCollector truss = trussesIterator.next().getValue();
					if (FileUtils.checkFileExist(cacheFilePath.resolve(truss.getVault().getVaultName()).toString())) {
						HistoVault histoVault = null;
						try (InputStream inputStream = new BufferedInputStream(new FileInputStream(cacheFilePath.resolve(truss.getVault().getVaultName()).toFile()))) {
							histoVault = vaultIO.load(inputStream);
						}
						catch (Exception e) {
							log.log(Level.SEVERE, e.getMessage(), e);
						}

						if (histoVault != null) {
							vaults.add(histoVault.getExtendedVault());
						}
						trussesIterator.remove();
					}
				}
				if (map.size() == 0) trussJobsIterator.remove();
			}
		}
		return vaults;
	}

	/**
	 * Get the zip file name from the history vault class and add all histoset vaults to this file.
	 * @return the full size (in bytes) of the cache directory
	 * @throws IOException
	 */
	public static long storeVaultsInCache(List<ExtendedVault> newVaults) throws IOException {
		Path cacheFilePath = ExtendedVault.getVaultsFolder();
		VaultProxy vaultIO = new VaultProxy();
		if (settings.isZippedCache()) {
			// use a zip file system because it supports adding files in contrast to the standard procedure using a ZipOutputStream
			Map<String, String> env = new HashMap<String, String>();
			env.put("create", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			try (FileSystem zipFileSystem = FileSystems.newFileSystem(URI.create("jar:" + cacheFilePath.toUri()), env)) { //$NON-NLS-1$
				for (ExtendedVault histoVault : newVaults) {
					// name the file inside the zip file
					Path filePath = zipFileSystem.getPath(histoVault.getVaultFileName().toString());
					if (!FileUtils.checkFileExist(filePath.toString())) {
						//					if (!filePath.toFile().exists()) {
						try (BufferedOutputStream zipOutputStream = new BufferedOutputStream(Files.newOutputStream(filePath, StandardOpenOption.CREATE_NEW))) {
							vaultIO.store(histoVault, zipOutputStream);
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%s  %s", histoVault.getVaultFileName(), cacheFilePath.toString())); //$NON-NLS-1$
						}
						catch (Exception e) {
							log.log(Level.SEVERE, e.getMessage(), e);
						}
					}
				}
			}
		}
		else {
			FileUtils.checkDirectoryAndCreate(cacheFilePath.toString());
			for (ExtendedVault histoVault : newVaults) {
				Path filePath = cacheFilePath.resolve(histoVault.getVaultFileName());
				try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(filePath, StandardOpenOption.CREATE_NEW))) {
					vaultIO.store(histoVault, outputStream);
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%s  %s", histoVault.getVaultFileName(), cacheFilePath.toString())); //$NON-NLS-1$
				}
				catch (Exception e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
		return cacheFilePath.toFile().length();
	}
}
