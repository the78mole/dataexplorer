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

    Copyright (c) 2018 Thomas Eickert
****************************************************************************************/

package gde.histo.cache;

import static java.util.logging.Level.SEVERE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.sun.istack.internal.Nullable;

import gde.DataAccess;
import gde.GDE;
import gde.config.Settings;
import gde.histo.guard.ObjectVaultIndex.VaultKeyPair;
import gde.histo.innercache.Cache;
import gde.histo.innercache.CacheBuilder;
import gde.histo.innercache.CacheStats;
import gde.log.Level;
import gde.log.Logger;
import gde.utils.FileUtils;

/**
 * Supports reading vaults from roaming data sources.
 * Zipped cache files supported only for standard file systems.
 * @author Thomas Eickert (USER)
 */
public class SimpleVaultReader {
	private static final String											$CLASS_NAME	= SimpleVaultReader.class.getName();
	private static final Logger											log					= Logger.getLogger($CLASS_NAME);

	private static final Cache<String, HistoVault>	memoryCache	=																		//
			CacheBuilder.newBuilder().maximumSize(4444).recordStats().build();													// key is the vaultName

	/**
	 * Prevents closing.
	 * Closing is implemented by the ZipInputStream after using an entry's ZipInputStream.
	 */
	private static class CloseIgnoringInputStream extends InputStream {
		private ZipInputStream stream;

		public CloseIgnoringInputStream(ZipInputStream inStream) {
			stream = inStream;
		}

		public ZipEntry getNextEntry() throws IOException {
			return stream.getNextEntry();
		}

		@Override
		public int read() throws IOException {
			return stream.read();
		}

		@Override
		public void close() {
			// ignore
		}

		public void reallyClose() throws IOException {
			stream.close();
		}
	}

	/**
	 * @param keyPairs holds the access keys for the cache vaults
	 * @return the extracted vaults
	 */
	@Nullable
	public static List<HistoVault> readVaults(List<VaultKeyPair> keyPairs) {
		List<HistoVault> vaults = new ArrayList<>();
		for (VaultKeyPair p : keyPairs) {
			if (p == null) continue;
			HistoVault vault = SimpleVaultReader.readVault(p.getKey(), p.getValue());
			vaults.add(vault);
		}
		log.log(Level.OFF, "vaults size=", vaults.size());
		return vaults;
	}

	/**
	 * @param folderName defines the zip file holding the vaults or the vaults folder
	 * @param fileName is the vault name
	 * @return the extracted vault
	 */
	@Nullable
	private static HistoVault readVault(String folderName, String fileName) {
		HistoVault histoVault = memoryCache.getIfPresent(fileName);
		if (histoVault == null) {
			histoVault = DataAccess.getInstance().getVault(folderName, fileName);
		}
		log.fine(() -> {
			CacheStats stats = memoryCache.stats();
			return String.format("evictionCount=%d  hitCount=%d  missCount=%d hitRate=%f missRate=%f", stats.evictionCount(), stats.hitCount(), stats.missCount(), stats.hitRate(), stats.missRate());
		});
		return histoVault;
	}

	/**
	 * @param directoryName defines the zip file holding the vaults or the vaults folder
	 * @param extractor is the function for extracting the vault data
	 * @return the extracted vault data after eliminating trusses
	 * @throws IOException during opening or traversing the zip file
	 */
	public static List<Entry<String, String>> readVaultsIndices(String directoryName, Function<HistoVault, Entry<String, String>> extractor)
			throws IOException {
		List<Entry<String, String>> vaultExtract = new ArrayList<>();
		if (!DataAccess.getInstance().existsCacheDirectory(directoryName)) return vaultExtract;

		final int MIN_FILE_LENGTH = 2048;
		if (Settings.getInstance().isZippedCache()) {
			try (CloseIgnoringInputStream stream = new CloseIgnoringInputStream(DataAccess.getInstance().getCacheZipInputStream(directoryName))) {
				ZipEntry entry;
				while ((entry = stream.getNextEntry()) != null) {
					if (entry.getSize() <= MIN_FILE_LENGTH) continue;

					try {
						HistoVault histoVault = memoryCache.get(entry.getName(), () -> VaultProxy.load(stream));
						vaultExtract.add(extractor.apply(histoVault));
					} catch (Exception e) {
						log.log(SEVERE, e.getMessage(), e);
					}
				}
				stream.reallyClose();
			} finally {
			}
		} else {
			Path folderPath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, directoryName);
			List<File> dirListing = FileUtils.getFileListing(folderPath.toFile(), 0);
			for (File file : dirListing) {
				if (file.length() <= MIN_FILE_LENGTH) continue;

				try {
					HistoVault histoVault = memoryCache.get(file.getName(), () -> VaultProxy.load(new FileInputStream(file)));
					vaultExtract.add(extractor.apply(histoVault));
				} catch (Exception e) {
					log.log(SEVERE, e.getMessage(), e);
				}
			}
		}
		log.fine(() -> {
			CacheStats stats = memoryCache.stats();
			return String.format("evictionCount=%d  hitCount=%d  missCount=%d hitRate=%f missRate=%f", stats.evictionCount(), stats.hitCount(), stats.missCount(), stats.hitRate(), stats.missRate());
		});
		return vaultExtract;
	}

}
