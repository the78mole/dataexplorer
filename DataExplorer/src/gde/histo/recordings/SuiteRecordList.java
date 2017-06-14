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

package gde.histo.recordings;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import gde.device.TrailTypes;

/**
 * Suite records administration.
 * Key: TrailTypes.ordinal
 * @author Thomas Eickert
 */
public final class SuiteRecordList extends HashMap<Integer, SuiteRecord> {
	private final static String	$CLASS_NAME				= SuiteRecordList.class.getName();
	private static final long		serialVersionUID	= -5963216308453730035L;
	private final static Logger	log								= Logger.getLogger($CLASS_NAME);

	private final TrailRecord		trailRecord;

	public enum SuiteMember {
		// range suite
		LOWER(1), MIDDLE(0), UPPER(2),
		// boxplot
		LOWER_WHISKER(5), UPPER_WHISKER(6), MEDIAN(2);

		/**
		 * use this instead of values() to avoid repeatedly cloning actions.
		 */
		public final static SuiteMember	values[]	= values();
		private final int								suiteOrdinal;

		private SuiteMember(int v) {
			this.suiteOrdinal = v;
		}

		public int getSuiteOrdinal() {
			return this.suiteOrdinal;
		}
	}

	public SuiteRecordList(TrailRecord trailRecord) {
		this.trailRecord = trailRecord;
	}

	/**
	 * Defines new suite records from the trailType list.
	 * @param suiteTrails holds the trail types applicable for the suite
	 */
	public void setSuite(List<TrailTypes> suiteTrails) {
		for (int i = 0; i < suiteTrails.size(); i++) {
			this.put(suiteTrails.get(i).ordinal(), new SuiteRecord(suiteTrails.get(i).ordinal(), this.trailRecord.size()));
		}
	}

	public int getSuiteMaxValue() {
		return this.values().parallelStream().mapToInt(s -> s.getMaxRecordValue()).max().orElseThrow(() -> new UnsupportedOperationException());
	}

	public int getSuiteMinValue() {
		return this.values().parallelStream().mapToInt(s -> s.getMinRecordValue()).min().orElseThrow(() -> new UnsupportedOperationException());
	}

	public int realSize() {
		return this.get(0).size();
	}

	public int getSuiteLength() {
		return this.size();
	}

	public SuiteRecord getSuiteRecord(SuiteMember suiteMember) {
		return this.get(suiteMember.getSuiteOrdinal());
	}

	/**
	 * @param suiteMember
	 * @param index
	 * @return the point value at the index position of the suite member
	 */
	public Integer elementAt(SuiteMember suiteMember, int index) {
		return this.get(suiteMember.getSuiteOrdinal()).get(index);
	}

}
