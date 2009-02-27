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
 * Application exception class used if absolute CSV data showing missmatch of units
 * @author Winfried Brügmann
 */
public class UnitCompareException extends Exception {
	static final long serialVersionUID = 26031957;

	public UnitCompareException() {
		super();
	}

	/**
	 * @param message
	 */
	public UnitCompareException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public UnitCompareException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public UnitCompareException(String message, Throwable cause) {
		super(message, cause);
	}

}
