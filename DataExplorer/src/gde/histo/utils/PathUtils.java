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

    Copyright (c) 2018 Thomas Eickert
****************************************************************************************/

package gde.histo.utils;

import java.nio.file.Path;

import gde.GDE;

/**
 *
 * @author Thomas Eickert (USER)
 */
public class PathUtils {

	/**
	 * @return the lower case extension without dot
	 */
	public static String getFileExtension(Path path) {
		if (path.getNameCount() == 0) {
			return GDE.STRING_EMPTY;
		} else {
			return getFileExtension(path.getFileName().toString());
		}
	}

	/**
	 * @return the lower case extension without dot
	 */
	public static String getFileExtension(String fileName) {
		String extension = fileName.toString().substring(fileName.lastIndexOf('.') + 1).toLowerCase();
		if (extension.equals(fileName)) extension = GDE.STRING_EMPTY;
		return extension;
	}

}
