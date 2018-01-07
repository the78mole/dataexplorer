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

import java.util.stream.Collectors;

import gde.GDE;
import gde.ui.DataExplorer;

/**
 * Exclusion data administration.
 * @author Thomas Eickert (USER)
 */
public final class ExclusionFormatter {

	/**
	 * @return the exclusion information for the trusses excluded from the history
	 */
	public static String getExcludedTrussesAsText() {
		return DataExplorer.getInstance().getPresentHistoExplorer().getHistoSet().getExcludedPaths().stream().distinct() //
				.map(p -> {
					String key = p.getFileName().toString();
					String property = ExclusionData.getInstance(p.getParent()).getProperty(key);
					return (property.isEmpty() ? key : key + GDE.STRING_BLANK_COLON_BLANK + property);
				}) //
				.collect(Collectors.joining("\n"));
	}

}
