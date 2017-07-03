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

package gde.histo.exclusions;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import gde.histo.cache.VaultCollector;
import gde.histo.datasources.HistoSet;
import gde.log.Level;

/**
 * Exclusion activity processing.
 * @author Thomas Eickert (USER)
 */
public final class ExclusionActivity {
	private final static String								$CLASS_NAME								= ExclusionActivity.class.getName();
	private final static Logger								log												= Logger.getLogger($CLASS_NAME);

	/**
	 * Delete the exclusion files belonging to the directories with ignored files.
	 * @param defaultPath this exclusion information is deleted in any case, e.g. if the suppress mode is currently OFF
	 */
	public static void clearExcludeLists(Path defaultPath) {
		Set<Path> exclusionDirectories = new HashSet<>();
		if (defaultPath != null) exclusionDirectories.add(defaultPath);
		for (VaultCollector truss : HistoSet.getInstance().getSuppressedTrusses().values()) {
			exclusionDirectories.add(truss.getVault().getLogFileAsPath().getParent());
		}
		for (Path ignorePath : exclusionDirectories) {
			ExclusionData.getInstance(ignorePath).delete();
			log.log(Level.FINE, "deleted : ", ignorePath); //$NON-NLS-1$
		}
	}

	/**
	 * @param filePath
	 * @param recordsetBaseName empty string sets ignore to the file in total
	 */
	public static synchronized void setExcludeRecordSet(Path filePath, String recordsetBaseName) {
		final ExclusionData fileExclusionData = ExclusionData.getInstance(filePath.getParent());
		if (recordsetBaseName.isEmpty()) fileExclusionData.setProperty((filePath.getFileName().toString()));
		else
			fileExclusionData.addToProperty(filePath.getFileName().toString(), recordsetBaseName);
		fileExclusionData.store();
	}

}
