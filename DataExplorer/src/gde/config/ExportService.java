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

    Copyright (c) 2018 Winfried Bruegmann
****************************************************************************************/
package gde.config;

import java.io.IOException;
import java.util.jar.JarFile;

import gde.GDE;

/**
 * simple structure of exported services as defined for each device implemented in a device plug-in jar manifest
 * @author brueg
 */
public class ExportService {
	
	final String name;
	final String manufacturer;
	final String dataSource;
	final String jar;

	/**
	 * build simple structure of exported services as defined for each device implemented in a device plug-in jar manifest
	 */
	public ExportService(String service, String jarPath) {
		String[] temp = service.split(GDE.STRING_COLON);
		if (temp.length != 3) 
			throw new IllegalArgumentException(String.format("wrong number of arguments in %s", service));
		this.name = temp[0].trim();
		this.manufacturer = temp[1].trim();
		this.dataSource = temp[2].trim(); //TODO replace language dependent keys 
		this.jar = jarPath;
	}

	public String getName() {
		return name;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public String getDataSource() {
		return dataSource;
	}

	public JarFile getJarFile() throws IOException {
		return new JarFile(jar);
	}
}
