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

import java.util.HashMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import gde.data.RecordSet;

/**
 * Collect transitions per transition group.
 * @author Thomas Eickert (USER)
 */
public final class GroupTransitions extends HashMap<Integer, TreeMap<Long, Transition>> {
	private final static String	$CLASS_NAME				= TransitionCollector.class.getName();
	private static final long		serialVersionUID	= 4194776523408560821L;
	private final static Logger	log								= Logger.getLogger($CLASS_NAME);

	private final RecordSet			recordSet;
	private final int						recordDataSize;

	public GroupTransitions(RecordSet recordSet) {
		this.recordSet = recordSet;
		this.recordDataSize = recordSet.getRecordDataSize(true);
	}

	/**
	 * Identify all transitions for the recordset and channel.
	 * Remove transition duplicates or overlapping transitions in all transition groups.
	 * @param logChannelNumber
	 */
	public void add4Channel(int logChannelNumber) {
		this.putAll(TransitionCollector.add4Channel(this.recordSet, logChannelNumber));
	}

	/**
	 * @return the total number of transitions over all groups
	 */
	public long getTransitionsCount() {
		return this.values().parallelStream().count();
	}

	public int getRecordDataSize() {
		return this.recordDataSize;
	}
}
