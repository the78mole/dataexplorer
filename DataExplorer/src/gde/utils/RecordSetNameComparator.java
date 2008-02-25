/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.utils;

import java.util.Comparator;

/**
 * To sort by starting number or alphabetically
 * @author Winfried Brügmann
 */
public class RecordSetNameComparator implements Comparator<String> {

	// Comparator interface requires defining compare method.
	public int compare(String nameA, String nameB) {
		try {
			Integer intA = new Integer(nameA.substring(0, nameA.indexOf(')')));
			Integer intB = new Integer(nameB.substring(0, nameB.indexOf(')')));
			if (intA > intB)
				return 1;
			else if (intA < intB)
				return -1;
			else {
				//... If no number, sort alphabetically.
				return nameA.compareToIgnoreCase(nameB);
			}
		}
		catch (Exception e) {
			// NumberFormatException, sort alphabetically.
			return nameA.compareToIgnoreCase(nameB);
		}
	}
}
