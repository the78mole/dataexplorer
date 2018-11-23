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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import gde.GDE;
import gde.device.InputTypes;

/**
 * simple structure of exported services as defined for each device implemented in a device plug-in jar manifest
 * @author brueg
 */
public class ExportService {

	final String									name;
	final String									manufacturer;
	final Collection<InputTypes>	inputTypes;
	final String									jar;

	/**
	 * Create from the device properties.
	 */
	public ExportService(String name, String manufacturer, Collection<InputTypes> inputTypes, String jarPath) {
		this.name = name;
		this.manufacturer = manufacturer;
		this.inputTypes = inputTypes;
		this.jar = jarPath;
	}

	/**
	 * build simple structure of exported services as defined for each device implemented in a device plug-in jar manifest
	 */
	public ExportService(String service, String jarPath) {
		String[] temp = service.split(GDE.STRING_COLON);
		if (temp.length != 3) throw new IllegalArgumentException(String.format("wrong number of arguments in %s", service));
		this.name = temp[0].trim();
		this.manufacturer = temp[1].trim();
		String[] inputTexts = temp[2].trim().split(GDE.STRING_SEMICOLON);
		// todo remove this if clause after functional test with old jars
		if (!Arrays.asList(InputTypes.valuesAsStingArray()).contains(inputTexts[0])) {
			// todo remove
			this.inputTypes = EnumSet.noneOf(InputTypes.class);
			for (String inputText : inputTexts) {
				this.inputTypes.add(inputText.equals("file_import") ? InputTypes.FILE_IO : InputTypes.SERIAL_IO);
			}
		} else {
			// keep only this line
			this.inputTypes = InputTypes.fromStringArray(inputTexts);
		}
		this.jar = jarPath;
	}

	public String getName() {
		return name;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public String getDataSource() {
		return inputTypes.stream().map(InputTypes::displayText).collect(Collectors.joining(GDE.STRING_SEMICOLON));
	}

	public JarFile getJarFile() throws IOException {
		return new JarFile(jar);
	}

	public Path getJarPath() {
		return Paths.get(jar);
	}

	public String getDisplayText() {
		return String.join(GDE.STRING_COLON, name, manufacturer, getDataSource());
	}

	@Override
	public String toString() {
		String neutralSource = inputTypes.stream().map(InputTypes::name).collect(Collectors.joining(GDE.STRING_SEMICOLON));
		return String.join(GDE.STRING_COLON, name, manufacturer, neutralSource);
	}

}
