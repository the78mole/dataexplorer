/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018 Winfried Bruegmann
									2017,2018 Thomas Eickert
****************************************************************************************/
package gde.histo.config;

import java.io.File;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import gde.GDE;
import gde.config.Settings;
import gde.histo.base.NonUiTestCase;
import gde.utils.FileUtils;

class HistoGraphicsTemplateTest extends NonUiTestCase {
	private final static String	$CLASS_NAME	= HistoGraphicsTemplateTest.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		log.setLevel(Level.INFO);
		log.setUseParentHandlers(true);
	}

	@ParameterizedTest
	@CsvSource({ //
			"CSV2Serial1, '', false", //
			"CSV2Serial1, 3rd, false", //
			"CSV2Serial1, 3rd, true" })
	void testConvertIntoObjectTemplateAndLoad(String deviceName, String objectKey, boolean readOnlyTemplate) {
		this.settings.setObjectTemplatesActive(true);

		this.settings.setObjectList(new String[] { "first", "2nd" }, "first");
		int channelNumber = 1;
		HistoGraphicsTemplate template = readOnlyTemplate //
				? HistoGraphicsTemplate.createReadonlyTemplate(deviceName, channelNumber, objectKey) //
				: HistoGraphicsTemplate.createGraphicsTemplate(deviceName, channelNumber, objectKey);
		template.load();
		File fileCreated = Paths.get(GDE.APPL_HOME_PATH, Settings.GRAPHICS_TEMPLATES_DIR_NAME, //
				objectKey.isEmpty() ? GDE.STRING_DEVICE_ORIENTED_FOLDER : objectKey, //
				"CSV2Serial1_1H.xml").toFile();

		if (readOnlyTemplate) {
			assertFalse("file creation skipped", fileCreated.exists());
		} else {
			assertTrue("file created", fileCreated.exists());
			long lastModified = fileCreated.lastModified();

			HistoGraphicsTemplate existingTemplate = HistoGraphicsTemplate.createGraphicsTemplate(deviceName, channelNumber, objectKey);
			existingTemplate.load();
			assertTrue("use existing file", lastModified == fileCreated.lastModified());

			FileUtils.deleteFile(fileCreated.toString());
		}
	}

}
