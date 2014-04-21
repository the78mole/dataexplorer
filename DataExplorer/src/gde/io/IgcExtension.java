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
    
    Copyright (c) 2014 Winfried Bruegmann
****************************************************************************************/
package gde.io;

/**
 * class to handle IGC file extension records
 */
public class IgcExtension {

	String	threeLetterCode;
	int			start;
	int			end;

	public IgcExtension(final int startByte, final int stopByte, final String identifier) {
		this.start = startByte;
		this.end = stopByte;
		this.threeLetterCode = identifier;
	}

	public int getValue(final String fRecord) {
		try {
			return Integer.parseInt(fRecord.substring(this.start, this.end)) * 1000;
		}
		catch (NumberFormatException e) {
			return 0;
		}
	}

	public String getThreeLetterCode() {
		return this.threeLetterCode;
	}
}
