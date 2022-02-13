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

/**
 * this class collect values to enable average calculation of the value difference (offset)
 */
public class ValueCollector {
	int count = 0;
	int sumStaticPressure = 0;
	int sumTecPressure = 0;
	
	public ValueCollector() {};
	
	/**
	 * initialize class while initializing count and the two pressure values
	 * @param staticPressure the static pressure value times 1000 according GDE data model
	 * @param tecPressure the TEC tube pressure value times 1000 according GDE data model
	 */
	public ValueCollector(int staticPressure, int tecPressure) {
		count = 1;
		sumStaticPressure = staticPressure;
		sumTecPressure = tecPressure;			
	}
	
	/**
	 * add up count and the two given pressure values
	 * @param staticPressure the static pressure value times 1000 according GDE data model
	 * @param tecPressure the TEC tube pressure value times 1000 according GDE data model
	 */
	public void add(int staticPressure, int tecPressure) {
		count += 1;
		sumStaticPressure += staticPressure;
		sumTecPressure += tecPressure;			
	}
	
	/**
	 * @return actual count of entries
	 */
	public int getCount() {
		return count;
	}
	
	/**
	 * @return offset average of collected pressure values
	 */
	public int getAvgOffset() {
		return (sumStaticPressure - sumTecPressure) / count;
	}
}

