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

package gde.histo.transitions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import gde.data.RecordSet;
import gde.device.TransitionGroupType;
import gde.ui.DataExplorer;

/**
 * Transition data mapping for the table tab.
 * Support synchronization for ongoing data gatherings.
 * @author Thomas Eickert (USER)
 */
public final class TransitionTableMapper {

	/**
	 * @param index
	 * @param dataTableRow
	 * @return the row with additional columns for the transitions groups
	 */
	public static synchronized String[] getExtendedRow(RecordSet recordSet, int index, String[] dataTableRow) {
		int tableColumnsSize = recordSet.getVisibleAndDisplayableRecordsForTable().size() + 1;
		HashMap<Integer, TransitionGroupType> transitionGroups = DataExplorer.application.getActiveDevice().getDeviceConfiguration().getChannel(recordSet.getChannelConfigNumber()).getTransitionGroups();
		dataTableRow = Arrays.copyOf(dataTableRow, tableColumnsSize + transitionGroups.size());

		int columnIndex = tableColumnsSize;
		for (Entry<Integer, TransitionGroupType> transitionsGroupsEntry : transitionGroups.entrySet()) {
			TreeMap<Long, Transition> transitions = recordSet.getHistoTransitions().get(transitionsGroupsEntry.getKey());
			if (transitions != null) {
				Transition transition = transitions.get((long) recordSet.getTime_ms(index));
				if (transition != null) {
					dataTableRow[columnIndex] = Integer.toString(transition.getTransitionType().getTransitionId());
				}
			}
			columnIndex++;
		}
		return dataTableRow;
	}

}
