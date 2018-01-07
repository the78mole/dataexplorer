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

import static java.util.logging.Level.SEVERE;

import gde.config.Settings;
import gde.histo.datasources.DirectoryScanner.SourceDataSet;
import gde.histo.datasources.VaultPicker.ProgressManager;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
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
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
	 * @return the vaults extracted from the file based on the input trusses
	 */
	public static List<ExtendedVault> loadVaultsFromFile(Path filePath, List<VaultCollector> trusses) {
		List<ExtendedVault> histoVaults = null;

		try {
			SourceDataSet dataSet = new SourceDataSet(filePath);
			histoVaults = dataSet.readVaults(trusses);
		} catch (Exception e) {
			histoVaults = new ArrayList<ExtendedVault>();
			log.log(SEVERE, e.getMessage(), e);
			log.info(() -> String.format("invalid file format: %s  channelNumber=%d  %s", //$NON-NLS-1$
					application.getActiveDevice().getName(), application.getActiveChannelNumber(), filePath));
		}

		return histoVaults;
	}

	/**
	 * Read cached vaults and reduce the trussJobs map.
	 * @param trussJobs lists all source files with a map of their vault skeletons (the key vaultFileName prevents double entries)
	 * @return the vaults and trusses loaded from the cache
	 * @throws IOException during opening or traversing the zip file
	 */
	public static synchronized List<ExtendedVault> loadVaultsFromCache(Map<Path, List<VaultCollector>> trussJobs, Optional<ProgressManager> progress) //
			throws IOException { // syn due to SAXException: FWK005 parse may not be called while parsing.
		List<ExtendedVault> vaults = new ArrayList<>();

		Path cacheFilePath = ExtendedVault.getVaultsFolder();
		if (settings.isZippedCache() && FileUtils.checkFileExist(cacheFilePath.toString())) {
			try (ZipFile zf = new ZipFile(cacheFilePath.toFile())) { // closing the zip file closes all streams
				Iterator<Map.Entry<Path, List<VaultCollector>>> trussJobsIterator = trussJobs.entrySet().iterator();
				while (trussJobsIterator.hasNext()) {
					final List<VaultCollector> map = trussJobsIterator.next().getValue();
					final Iterator<VaultCollector> trussesIterator = map.iterator();
					while (trussesIterator.hasNext()) {
						progress.ifPresent((p) -> p.countInLoop(1));
						VaultCollector truss = trussesIterator.next();
						ZipEntry vaultName = zf.getEntry(truss.getVault().getVaultName());
						if (vaultName != null) {
							HistoVault histoVault = null;
							try {
								histoVault = VaultProxy.load(new BufferedInputStream(zf.getInputStream(vaultName)));
							} catch (Exception e) {
								log.log(SEVERE, e.getMessage(), e);
							}

							if (histoVault != null) vaults.add(histoVault.getExtendedVault());
							trussesIterator.remove();
						}
					}
					if (map.isEmpty()) trussJobsIterator.remove();
				}
			}
		} else if (!settings.isZippedCache() && FileUtils.checkDirectoryExist(cacheFilePath.toString())) {
			Iterator<Map.Entry<Path, List<VaultCollector>>> trussJobsIterator = trussJobs.entrySet().iterator();
			while (trussJobsIterator.hasNext()) {
				final List<VaultCollector> map = trussJobsIterator.next().getValue();
				final Iterator<VaultCollector> trussesIterator = map.iterator();
				while (trussesIterator.hasNext()) {
					progress.ifPresent((p) -> p.countInLoop(1));
					VaultCollector truss = trussesIterator.next();
					if (FileUtils.checkFileExist(cacheFilePath.resolve(truss.getVault().getVaultName()).toString())) {
						HistoVault histoVault = null;
						File vaultFile = cacheFilePath.resolve(truss.getVault().getVaultName()).toFile();
						try (InputStream inputStream = new BufferedInputStream(new FileInputStream(vaultFile))) {
							histoVault = VaultProxy.load(inputStream);
						} catch (Exception e) {
							log.log(SEVERE, e.getMessage(), e);
						}

						if (histoVault != null) vaults.add(histoVault.getExtendedVault());
						trussesIterator.remove();
					}
				}
				if (map.isEmpty()) trussJobsIterator.remove();
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
		if (settings.isZippedCache()) {
			// use a zip file system because it supports adding files in contrast to the standard procedure using a ZipOutputStream
			Map<String, String> env = new HashMap<String, String>();
			env.put("create", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			try (FileSystem zipFileSystem = FileSystems.newFileSystem(URI.create("jar:" + cacheFilePath.toUri()), env)) { //$NON-NLS-1$
				for (ExtendedVault histoVault : newVaults) {
					// name the file inside the zip file
					Path filePath = zipFileSystem.getPath(histoVault.getVaultFileName().toString());
					if (!FileUtils.checkFileExist(filePath.toString())) {
						try (BufferedOutputStream zipOutputStream = new BufferedOutputStream(Files.newOutputStream(filePath, StandardOpenOption.CREATE_NEW))) {
							VaultProxy.store(histoVault, zipOutputStream);
						} catch (Exception e) {
							log.log(SEVERE, e.getMessage(), e);
						}
					}
				}
			}
		} else {
			FileUtils.checkDirectoryAndCreate(cacheFilePath.toString());
			for (ExtendedVault histoVault : newVaults) {
				Path filePath = cacheFilePath.resolve(histoVault.getVaultFileName());
				try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(filePath, StandardOpenOption.CREATE_NEW))) {
					VaultProxy.store(histoVault, outputStream);
				} catch (Exception e) {
					log.log(SEVERE, e.getMessage(), e);
				}
			}
		}
		return cacheFilePath.toFile().length();
	}
}
