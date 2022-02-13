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
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2022 Winfried Bruegmann
****************************************************************************************/
package gde.utils;

import java.util.HashMap;
import java.util.logging.Logger;

import gde.log.Level;

/**
 * this class collects ValueCollector events sorted by one of the values used as key 
 * an average calculation using the previous detected maximal event count can be queried
 * the maximal count can be queried whenever required
 *
 */
public class ValueEventCollector extends HashMap<Integer, ValueCollector> {
	private static final Logger	log												= Logger.getLogger(ValueEventCollector.class.getName());
	private static final long serialVersionUID = 1L;
	
	int maxCount = 0;
	int minCount = 0;
	int avgOffsetMaxCount = Integer.MIN_VALUE;
	int avgOffsetMinCount = Integer.MAX_VALUE;
	
	public ValueEventCollector() {}
	
	/**
	 * @return average offset or difference between the two given values of maximal entry count
	 */
	public int getAvgDiffMaxCount() {
		if (avgOffsetMaxCount == Integer.MIN_VALUE) {
			int maxCountKey = 0;
			for (int key : this.keySet()) {
				if (get(key).getCount() > maxCount) {
					maxCount = get(key).getCount();
					maxCountKey = key;		
					//log.log(Level.OFF, "pressureOffsetCount = " + maxCount);
				}
			}
			if (get(maxCountKey) != null) {
				avgOffsetMaxCount = get(maxCountKey).getAvgOffset();
				log.log(Level.OFF, String.format("pressureOffsetCount = %3d avgOffset = %.3f hPa", get(maxCountKey).getCount(), avgOffsetMaxCount/1000.));
			}
		}
		return avgOffsetMaxCount;
	}
	
	/**
	 * @return average offset or difference between the two given values of maximal entry count
	 */
	public int getAvgDiffMinCount() {
		if (avgOffsetMinCount == Integer.MAX_VALUE) {
			int minCountKey = 0;
			for (int key : this.keySet()) {
				if (get(key).getCount() < minCount) {
					minCount = get(key).getCount();
					minCountKey = key;		
					//log.log(Level.OFF, "pressureOffsetCount = " + minCount);
				}
			}
			if (get(minCountKey) != null) {
				avgOffsetMinCount = get(minCountKey).getAvgOffset();
				log.log(Level.OFF, String.format("pressureOffsetCount = %3d avgOffset = %.3f hPa", get(minCountKey).getCount(), avgOffsetMaxCount/1000.));
			}
		}
		return avgOffsetMaxCount;
	}
	
	/**
	 * @return maximal event count over all entries 
	 */
	public int getMaxEntryCount() {
		if (maxCount == 0) {
			for (int key : this.keySet()) {
				if (get(key).getCount() > maxCount) {
					maxCount = get(key).getCount();
					//log.log(Level.OFF, "valueOffsetCount = " + maxCount);
				}
			}
		}
		return maxCount;
	}
	
	/**
	 * @return minimal event count over all entries 
	 */
	public int getMinEntryCount() {
		if (minCount == 0) {
			for (int key : this.keySet()) {
				if (get(key).getCount() < minCount) {
					minCount = get(key).getCount();
					//log.log(Level.OFF, "valueOffsetCount = " + minCount);
				}
			}
		}
		return maxCount;
	}
	
	/**
	 * reset initial values to enable previous called maximal count or offset average
	 */
	public void reset() {
		maxCount = minCount = 0;
		avgOffsetMaxCount = Integer.MIN_VALUE;
		avgOffsetMinCount = Integer.MAX_VALUE;
	}
}
