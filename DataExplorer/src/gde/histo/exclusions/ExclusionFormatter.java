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

import gde.GDE;
import gde.histo.cache.VaultCollector;
import gde.histo.datasources.DirectoryScanner;
import gde.histo.datasources.HistoSet;

/**
 * Exclusion data administration.
 * @author Thomas Eickert (USER)
 */
public final class ExclusionFormatter {
	private final static String								$CLASS_NAME								= DirectoryScanner.class.getName();
	private final static Logger								log												= Logger.getLogger($CLASS_NAME);

	/**
	 * @return the exclusion information for the trusses excluded from the history
	 */
	public static String getExcludedTrussesAsText() {
		Set<String> exclusionTexts = new HashSet<>();
		for (VaultCollector truss : HistoSet.getInstance().getSuppressedTrusses().values()) {
			Path fileDir = truss.getVault().getLogFileAsPath().getParent();
			exclusionTexts.add(getFormattedProperty(ExclusionData.getInstance(fileDir), truss.getVault().getLogFileAsPath().getFileName().toString()));
		}

		StringBuilder sb = new StringBuilder();
		for (String text : exclusionTexts) {
			sb.append(GDE.STRING_NEW_LINE).append(text);
		}
		return sb.length() > 0 ? sb.substring(1) : GDE.STRING_EMPTY;
	}

	/**
	 * @param key is the data file name of the vault / truss
	 * @return the formated key value pair ('0199_2015-11-8.bin' or '0199_2015-11-8.bin : recordsetname')
	 */
	private static String getFormattedProperty(ExclusionData fileExclusionData, String key) {
		return fileExclusionData.getProperty(key).isEmpty() ? key : key + GDE.STRING_BLANK_COLON_BLANK + fileExclusionData.getProperty(key);
	}

}
