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

    Copyright (c) 2017,2018 Thomas Eickert
****************************************************************************************/

package gde.histo.exclusions;

import java.nio.file.Path;

import gde.ui.DataExplorer;

/**
 * Exclusion activity processing.
 * @author Thomas Eickert (USER)
 */
public final class ExclusionActivity {

	/**
	 * Delete the exclusion file belonging to the primary directory.
	 * The exclusion information is deleted in any case, e.g. if the suppress mode is currently OFF.
	 */
	public static void clearExcludeLists() {
		if (!DataExplorer.getInstance().getHistoExplorer().isPresent()) throw new UnsupportedOperationException();

		ExclusionData.getInstance().delete();
	}

	/**
	 * @param filePath
	 * @param recordsetBaseName empty string sets ignore to the file in total
	 */
	public static synchronized void setExcludeRecordSet(Path filePath, String recordsetBaseName) {
		final ExclusionData fileExclusionData = ExclusionData.getInstance();
		if (recordsetBaseName.isEmpty())
			fileExclusionData.setProperty((filePath.getFileName().toString()));
		else
			fileExclusionData.addToProperty(filePath.getFileName().toString(), recordsetBaseName);
		fileExclusionData.store();
	}

}
