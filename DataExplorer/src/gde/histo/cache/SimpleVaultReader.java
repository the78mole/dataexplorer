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

import com.sun.istack.internal.Nullable;

import gde.DataAccess;
import gde.histo.innercache.Cache;
import gde.histo.innercache.CacheBuilder;
import gde.histo.innercache.CacheStats;
import gde.log.Logger;

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
	 * @param folderName defines the zip file holding the vaults or the vaults folder
	 * @param fileName is the vault name
	 * @return the extracted vault after eliminating trusses
	 */
	@Nullable
	public static HistoVault readVault(String folderName, String fileName) {
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

}
