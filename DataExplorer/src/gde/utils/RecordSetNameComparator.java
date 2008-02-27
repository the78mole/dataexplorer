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
import java.util.regex.Pattern;

/**
 * To sort by starting number or alphabetically
 * @author Winfried Brügmann
 */
public class RecordSetNameComparator implements Comparator<String> {

	public static void main(String[] args) {
		RecordSetNameComparator rc = new RecordSetNameComparator();
		rc.compare("1) asdfer", "12) 12121");
	}
	// Comparator interface requires defining compare method.
	public int compare(String nameA, String nameB) {
		try {
			Pattern p = Pattern.compile("^[ ]*[0-9]+[^0-9]");
			if (p.matcher(nameA).find() && p.matcher(nameB).find() ) {
				Integer intA = new Integer(nameA.trim().split("\\D")[0]);
				Integer intB = new Integer(nameB.trim().split("\\D")[0]);
				if (intA > intB)
					return 1;
				else if (intA < intB)
					return -1;
				else 
					return 0;
			}
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
