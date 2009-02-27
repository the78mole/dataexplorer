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
 * Application exception class used if device referenced in file does not match active device
 * @author Winfried Brügmann
 */
public class MissMatchDeviceException extends Exception {
	static final long serialVersionUID = 26031957;

	public MissMatchDeviceException() {
		super();
	}

	/**
	 * @param message
	 */
	public MissMatchDeviceException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public MissMatchDeviceException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public MissMatchDeviceException(String message, Throwable cause) {
		super(message, cause);
	}

}
