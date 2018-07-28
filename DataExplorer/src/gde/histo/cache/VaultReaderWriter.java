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

		DataExplorer uses Guava 22.0, released May 22, 2017,
		which is available from http://code.google.com/p/guava-libraries/.
		Guava is subject to the Apache License v. 2.0:
		https://github.com/google/guava/blob/master/COPYING

		Copyright 2017 Google Inc.
    Copyright (c) 2017,2018 Thomas Eickert
****************************************************************************************/

package gde.histo.cache;

import static java.util.logging.Level.SEVERE;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import gde.Analyzer;
import gde.GDE;
import gde.config.Settings;
import gde.histo.datasources.AbstractSourceDataSet;
import gde.histo.datasources.AbstractSourceDataSet.SourceDataSet;
import gde.histo.datasources.VaultPicker.ProgressManager;
import gde.histo.datasources.VaultPicker.TrussJobs;
import gde.histo.device.IHistoDevice;
import gde.histo.innercache.Cache;
import gde.histo.innercache.CacheBuilder;
import gde.histo.innercache.CacheStats;
import gde.log.Logger;
import gde.utils.FileUtils;

/**
 * File system access for vaults and trusses.
 * @author Thomas Eickert (USER)
 */
public final class VaultReaderWriter {
	private static final String											$CLASS_NAME	= VaultReaderWriter.class.getName();
	private static final Logger											log					= Logger.getLogger($CLASS_NAME);

	private static final Cache<String, HistoVault>	memoryCache	=																		//
			CacheBuilder.newBuilder().maximumSize(4444).recordStats().build();													// key is the vaultName

	private final Analyzer													analyzer;
	private final Optional<ProgressManager>					progress;

	public VaultReaderWriter(Analyzer analyzer, Optional<ProgressManager> progress) {
		this.analyzer = analyzer;
		this.progress = progress;
	}

	/**
	 * Read file and populate the vault from the recordset.
	 */
	public void loadFromFile(Path filePath, List<VaultCollector> trusses) {
		SourceDataSet dataSet = AbstractSourceDataSet.createSourceDataSet(filePath, analyzer);
		if (dataSet != null) dataSet.readVaults4Ui(filePath, trusses);
	}

	/**
	 * Read cached vaults and reduce the trussJobs map.
	 * @param trussJobs lists all source files with a map of their vault skeletons (the key vaultFileName prevents double entries)
	 * @return the vaults and trusses loaded from the cache
	 * @throws IOException during opening or traversing the zip file
	 */
	public synchronized List<ExtendedVault> loadFromCaches(TrussJobs trussJobs) //
			throws IOException { // syn due to SAXException: FWK005 parse may not be called while parsing.
		Path osdCacheFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, ExtendedVault.getVaultsDirectoryName(analyzer, GDE.STRING_EMPTY));
		List<ExtendedVault> vaults = loadFromCachePath(trussJobs, progress, osdCacheFilePath);

		if (analyzer.getActiveDevice() instanceof IHistoDevice) {
			String readerSettings = ((IHistoDevice) analyzer.getActiveDevice()).getReaderSettingsCsv();
			if (!readerSettings.isEmpty()) {
				Path cacheFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, ExtendedVault.getVaultsDirectoryName(analyzer, readerSettings));
				List<ExtendedVault> nativeVaults = loadFromCachePath(trussJobs, progress, cacheFilePath);
				vaults.addAll(nativeVaults);
			}
		}
		return vaults;
	}

	private List<ExtendedVault> loadFromCachePath(TrussJobs trussJobs, Optional<ProgressManager> progress, Path cacheFilePath)
			throws IOException {
		List<ExtendedVault> vaults = new ArrayList<>();
		if (!cacheFilePath.toFile().exists()) return vaults;

		if (analyzer.getSettings().isZippedCache()) {
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
								histoVault = memoryCache.get(truss.getVault().getVaultName(), new Callable<HistoVault>() {
									@Override
									public HistoVault call() throws Exception {
										return VaultProxy.load(new BufferedInputStream(zf.getInputStream(vaultName)));
									}
								});
								vaults.add(ExtendedVault.createExtendedVault(histoVault, truss));
								trussesIterator.remove();
							} catch (Exception e) {
								log.log(SEVERE, e.getMessage(), e);
							}
						}
					}
					if (map.isEmpty()) trussJobsIterator.remove();
				}
			}
		} else {
			Iterator<Map.Entry<Path, List<VaultCollector>>> trussJobsIterator = trussJobs.entrySet().iterator();
			while (trussJobsIterator.hasNext()) {
				final List<VaultCollector> map = trussJobsIterator.next().getValue();
				final Iterator<VaultCollector> trussesIterator = map.iterator();
				while (trussesIterator.hasNext()) {
					progress.ifPresent((p) -> p.countInLoop(1));
					VaultCollector truss = trussesIterator.next();
					String fileName = truss.getVault().getVaultName();
					if (FileUtils.checkFileExist(cacheFilePath.resolve(fileName).toString())) {
						HistoVault histoVault = null;
						try {
							histoVault = memoryCache.get(fileName, new Callable<HistoVault>() {

								@Override
								public HistoVault call() throws Exception {
									return VaultProxy.load(cacheFilePath.resolve(fileName));
								}
							});
							vaults.add(ExtendedVault.createExtendedVault(histoVault, truss));
							trussesIterator.remove();
						} catch (Exception e) {
							log.log(SEVERE, e.getMessage(), e);
						}
					}
				}
				if (map.isEmpty()) trussJobsIterator.remove();
			}
		}
		log.fine(() -> {
			CacheStats stats = memoryCache.stats();
			return String.format("evictionCount=%d  hitCount=%d  missCount=%d hitRate=%f missRate=%f", stats.evictionCount(), stats.hitCount(), stats.missCount(), stats.hitRate(), stats.missRate());
		});
		return vaults;
	}

	/**
	 * Get the zip file name from the history vault class and add all histoset vaults to this file.
	 */
	public void storeInCaches(TrussJobs trussJobs) throws IOException {
		Map<SourceDataSet, List<VaultCollector>> groupedJobs = trussJobs.values().parallelStream().flatMap(Collection::parallelStream).collect(Collectors.groupingBy(VaultCollector::getSourceDataSet));
		groupedJobs.entrySet().stream().forEach(e -> {
			boolean providesReaderSettings = e.getKey().providesReaderSettings();
			String readerSettings = providesReaderSettings && analyzer.getActiveDevice() instanceof IHistoDevice
					? ((IHistoDevice) analyzer.getActiveDevice()).getReaderSettingsCsv() : GDE.STRING_EMPTY;
			String vaultsDirectoryName = ExtendedVault.getVaultsDirectoryName(analyzer, readerSettings);
			Path cacheFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, vaultsDirectoryName);
			try {
				storeInCachePath(e.getValue(), cacheFilePath);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	/**
	 * Get the zip file name from the history vault class and add all histoset vaults to this file.
	 * @throws IOException
	 */
	private void storeInCachePath(List<VaultCollector> newVaults, Path cacheFilePath) throws IOException {
		if (analyzer.getSettings().isZippedCache()) {
			// use a zip file system because it supports adding files in contrast to the standard procedure using a ZipOutputStream
			Map<String, String> env = new HashMap<String, String>();
			env.put("create", "true");
			try (FileSystem zipFileSystem = FileSystems.newFileSystem(URI.create("jar:" + cacheFilePath.toUri()), env)) {
				for (VaultCollector vaultCollector : newVaults) {
					ExtendedVault histoVault = vaultCollector.getVault();
					storeInVaultFileSystem(histoVault, zipFileSystem.getPath(histoVault.getVaultFileName().toString()));
				}
			}
		} else {
			FileUtils.checkDirectoryAndCreate(cacheFilePath.toString());
			for (VaultCollector vaultCollector : newVaults) {
				ExtendedVault histoVault = vaultCollector.getVault();
				storeInVaultFileSystem(histoVault, cacheFilePath.resolve(histoVault.getVaultFileName()));
			}
		}
	}

	private static void storeInVaultFileSystem(ExtendedVault histoVault, Path cacheBaseDirPath) {
		if (!FileUtils.checkFileExist(cacheBaseDirPath.toString())) {
			try (BufferedOutputStream zipOutputStream = new BufferedOutputStream(Files.newOutputStream(cacheBaseDirPath, StandardOpenOption.CREATE_NEW))) {
				VaultProxy.store(histoVault, zipOutputStream);
			} catch (Exception e) {
				log.log(SEVERE, e.getMessage(), e);
			}
			memoryCache.put(histoVault.vaultName, histoVault);
		}
	}

	/**
	 * @return the size of the cache in bytes
	 */
	public static long getCacheSize() {
		Path directory = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME);
		return FileUtils.size(directory);
	}

	public static long getInnerCacheHitCount() {
		return memoryCache.stats().hitCount();
	}
}
