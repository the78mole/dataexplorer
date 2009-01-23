/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.exception;

/**
 * @author brueg
 *
 */
public class DeclinedException extends Exception {
	static final long serialVersionUID = 26031957;
	/**
	 * 
	 */
	public DeclinedException() {
	}

	/**
	 * @param arg0
	 */
	public DeclinedException(String arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public DeclinedException(Throwable arg0) {
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public DeclinedException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}


}
