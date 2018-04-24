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

package gde.histo.base;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import gde.GDE;
import gde.config.Settings;
import gde.log.LogFormatter;
import gde.ui.DataExplorer;

import junit.framework.TestCase;

/**
 * Provide the DataExplorer settings.
 * @author Thomas Eickert (USER)
 */
public class HistoTestCase extends TestCase {
	protected Logger rootLogger;

	static {
		GDE.display = Display.getDefault();
		GDE.shell = new Shell(GDE.display);
	}

	protected enum DataSource {
		SETTINGS {
			@Override
			public Path getDataPath(String subPath) {
				return Paths.get(Settings.getInstance().getDataFilePath()).resolve(subPath);
			}
		},
		TESTDATA {
			@Override
			public Path getDataPath(String subPath) {
				String srcDataPath = getLoaderPath().replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
				if (srcDataPath.endsWith("bin/")) { // running inside eclipse
					srcDataPath = srcDataPath.substring(0, srcDataPath.indexOf(GDE.NAME_LONG)) + "DataFilesTestSamples/" + GDE.NAME_LONG;
				} else if (srcDataPath.indexOf("classes") > -1) { // ET running inside eclipse in Debug mode
					srcDataPath = srcDataPath.substring(0, srcDataPath.indexOf(GDE.NAME_LONG)) + "DataFilesTestSamples/" + GDE.NAME_LONG;
				} else {
					srcDataPath = srcDataPath.substring(0, srcDataPath.indexOf("build")) + "DataFilesTestSamples/" + GDE.NAME_LONG;
				}
				// return Paths.get(srcDataPath).resolve(subPath); Error because of leading slash:
				// /C:/Users/USER/git/dataexplorer/DataFilesTestSamples/DataExplorer // this.dataPath = Paths.get(srcDataPath).resolve(subPath).toFile();
				return (new File(srcDataPath)).toPath().resolve(subPath);
			}
		},
		INDIVIDUAL {
			@Override
			public Path getDataPath(String subPath) {
				return Paths.get(subPath);
			}
		};

		public abstract Path getDataPath(String string);
	}

	protected final DataExplorer	application	= DataExplorer.getInstance();
	protected final Settings			settings		= Settings.getInstance();
	protected final String				tmpDir			= System.getProperty("java.io.tmpdir").endsWith(GDE.FILE_SEPARATOR) ? System.getProperty("java.io.tmpdir")
			: System.getProperty("java.io.tmpdir") + GDE.FILE_SEPARATOR;

	Handler												ch					= new ConsoleHandler();
	LogFormatter									lf					= new LogFormatter();

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.rootLogger = Logger.getLogger("");
		// clean up all handlers from outside
		Handler[] handlers = this.rootLogger.getHandlers();
		for (Handler handler : handlers) {
			this.rootLogger.removeHandler(handler);
		}
		this.rootLogger.setLevel(Level.WARNING); // applies to test method logging
		this.rootLogger.addHandler(this.ch);
		this.ch.setFormatter(this.lf);
		this.ch.setLevel(Level.FINER); // applies to console logging in the classes to be tested

		Thread.currentThread().setContextClassLoader(GDE.getClassLoader());

		this.settings.setDataFilePath(DataSource.TESTDATA.getDataPath("_ET_Exzerpt").toString());

		// set histo settings now because SearchImportPath might influence the object list
		this.application.setHisto(true);
		setHistoSettings();
	}

	/**
	 *
	 */
	protected void setHistoSettings() {
		// the next lines only hold settings which do not control the GUI appearance
		this.settings.setSearchDataPathImports(true);
		this.settings.setChannelMix(false);
		this.settings.setSamplingTimespan_ms("2"); // this index corresponds to 1 sec
		this.settings.setFilesWithoutObject(true);
		this.settings.setFilesWithOtherObject(true);
		this.settings.setRetrospectMonths("120"); // this is the current maximum value
		this.settings.setZippedCache(false);
		this.settings.setAbsoluteTransitionLevel("999"); // results in default value
		this.settings.setAbsoluteTransitionLevel("999"); // results in default value
		this.settings.setSuppressMode(false);
		this.settings.setSubDirectoryLevelMax("5");
		this.settings.setCanonicalQuantiles(true);
		this.settings.setSymmetricToleranceInterval(true);
		this.settings.setOutlierToleranceSpread("9");
		this.settings.setSourceFileListenerActive(false);
		this.settings.setDataFoldersCsv("");
		this.settings.setImportFoldersCsv("");
		this.settings.setMirrorSourceFoldersCsv("");

	}

	/**
	 * @return the path where the class GDE gets loaded
	 */
	protected static String getLoaderPath() {
		return GDE.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	}

}