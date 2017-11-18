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

import gde.device.TrailTypes;

/**
 * Suite records for trail records.
 * Used if a suite trail type is selected the trail record.
 * Key: 0-based, corresponds to the suite members index of the trail type
 * @author Thomas Eickert
 */
public final class SuiteRecords extends HashMap<Integer, SuiteRecord> {
	private static final long		serialVersionUID	= -5963216308453730035L;

	public SuiteRecords() {
	}

	public int getSuiteMaxValue() {
		return this.values().parallelStream().mapToInt(s -> s.getMaxRecordValue()).max().orElseThrow(() -> new UnsupportedOperationException());
	}

	public int getSuiteMinValue() {
		return this.values().parallelStream().mapToInt(s -> s.getMinRecordValue()).min().orElseThrow(() -> new UnsupportedOperationException());
	}

	public int realSize() {
		int result = 0;
		for (SuiteRecord suiteRecord : this.values()) {
			result = suiteRecord.size();
			break;
		}
		return result;
	}

	public int getSuiteLength() {
		return this.size();
	}

	/**
	 * @param suiteOrdinal
	 * @param index
	 * @return the point value at the index position of the suite record identified by the suite ordinal
	 */
	public Integer getSuiteValue(int suiteOrdinal, int index) {
		return this.get(suiteOrdinal).elementAt(index);
	}

	/**
	 * @param trailType
	 * @param index
	 * @return true if the suite values are null
	 */
	public boolean isNullValue(TrailTypes trailType, int index) {
		return getSuiteValue(trailType.getSuiteMasterIndex(), index) == null;
	}
}
