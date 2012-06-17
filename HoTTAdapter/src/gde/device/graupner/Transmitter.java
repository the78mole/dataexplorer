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
    
    Copyright (c) 2012 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

public enum Transmitter {
	MC_32("mc-32"), MC_20("mc-20"), MX_20("mx-20"), MX_16("mx-16"), MX_12("mx-12"), UNSPECIFIED("unspecified");
	private final String	value;

	private Transmitter(String v) {
		this.value = v;
	}

	public String value() {
		return this.value;
	}

  public static Transmitter fromValue(String v) {
    for (Transmitter c: Transmitter.values()) {
        if (c.value.equals(v)) {
            return c;
        }
    }
    throw new IllegalArgumentException(v);
}

}
