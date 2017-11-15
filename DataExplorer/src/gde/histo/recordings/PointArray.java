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

import java.util.Arrays;

/**
 * Data points of multiple measurements or lines or curves.
 * @author Thomas Eickert (USER)
 */
public final class PointArray {

	private int									x;																					// x value of the curve
	private final Integer[]			y;																					// y values of the curve

	public PointArray(int ySize) {
		this.y = new Integer[ySize];
	}

	public int getX() {
		return this.x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public Integer[] getY() {
		return this.y;
	}

	public void setY(int index, Integer value) {
		this.y[index] = value;
	}

	/**
	 * @param suiteOrdinal
	 * @return the point value of the suite member
	 */
	public Integer getY(int suiteOrdinal) {
		return this.getY()[suiteOrdinal];
	}

	@Override
	public String toString() {
		return "PointArray [x=" + this.x + ", y=" + Arrays.toString(this.y) + "]";
	}

}
