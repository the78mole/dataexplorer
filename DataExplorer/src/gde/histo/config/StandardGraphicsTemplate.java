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

    Copyright (c) 2016,2017 Thomas Eickert
****************************************************************************************/
package gde.histo.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import gde.GDE;
import gde.config.Settings;

/**
 * Standard template in the standard template path.
 * @see gde.histo.config.HistoGraphicsTemplate
 * @author Thomas Eickert
 */
public class StandardGraphicsTemplate extends HistoGraphicsTemplate {
	private static final long serialVersionUID = -2725393260740286884L;

	/**
	 * @see gde.histo.config.HistoGraphicsTemplate
	 */
	protected StandardGraphicsTemplate(String deviceSignature) {
		super(deviceSignature);
	}

	@Override
	public Path getTargetFilePath() {
		String fileName = histoFileName == null || histoFileName.equals(GDE.STRING_EMPTY) ? defaultFileName : histoFileName;
		return Paths.get(Settings.getInstance().getGraphicsTemplatePath(), fileName);
	}

}
