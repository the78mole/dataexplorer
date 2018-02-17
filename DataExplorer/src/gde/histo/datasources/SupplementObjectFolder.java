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

package gde.histo.datasources;

import java.nio.file.Path;

import gde.utils.FileUtils;

/**
 * Folder for additional object directories in the working directory.
 * @author Thomas Eickert (USER)
 */
public class SupplementObjectFolder {

	private final static String	OBJECT_DIR_NAME	= "_SupplementObjectDirs";
	private final static String	README_NAME			= "README.TXT";
	private final static String	PERMISSION_555	= "555";
	private final static String	PATH_RESOURCE		= "resource/";

	public void checkAndCreate(Path workingFolder) {
		Path folderPath = workingFolder.resolve(OBJECT_DIR_NAME);
		FileUtils.checkDirectoryAndCreate(folderPath.toString());
		FileUtils.extract(this.getClass(), README_NAME, PATH_RESOURCE, folderPath.toString(), PERMISSION_555);
	}

}
