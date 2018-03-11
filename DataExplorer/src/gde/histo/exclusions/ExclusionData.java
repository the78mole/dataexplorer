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

    Copyright (c) 2017,2018 Thomas Eickert
****************************************************************************************/

package gde.histo.exclusions;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import gde.GDE;
import gde.config.Settings;
import gde.histo.datasources.DirectoryScanner;
import gde.histo.utils.SecureHash;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;

/**
 * Excluding files from history analysis via user activity.
 * Is based on property files for each data directory.
 * Stores the exclusions property files in the user directory if the data directory is not accessible for writing.
 * @author Thomas Eickert
 */
public final class ExclusionData extends Properties {
	private final static String		$CLASS_NAME				= ExclusionData.class.getName();
	private final static Logger		log								= Logger.getLogger($CLASS_NAME);
	private static final long			serialVersionUID	= -2477509505185819765L;

	private static ExclusionData	currentInstance;

	private final Path						dataFileDir;

	/**
	 * @return the instance which is only created anew if the primary directory has changed
	 */
	public static ExclusionData getInstance() {
		Path newDataFileDir = DirectoryScanner.getPrimaryFolder();
		if (currentInstance == null || !currentInstance.dataFileDir.equals(newDataFileDir)) {
			currentInstance = new ExclusionData(newDataFileDir);
		}
		return currentInstance;
	}

	/**
	 * @return true if an exclusion for all recordsets in the data file is active
	 */
	public static boolean isExcluded(Path dataFilePath) {
		if (Settings.getInstance().isSuppressMode()) {
			String exclusionValue = getInstance().getProperty(dataFilePath.getFileName().toString());
			boolean result = exclusionValue != null && exclusionValue.isEmpty();
			if (result) log.log(FINE, "file excluded ", dataFilePath); //$NON-NLS-1$
			return result;
		} else {
			return false;
		}
	}

	/**
	 * @return true if an exclusion for the recordset in the data file is active
	 */
	public static boolean isExcluded(Path dataFilePath, String recordsetBaseName) {
		if (recordsetBaseName.isEmpty()) throw new UnsupportedOperationException();

		if (Settings.getInstance().isSuppressMode()) {
			String exclusionValue = getInstance().getProperty(dataFilePath.getFileName().toString());
			boolean result = exclusionValue != null && (exclusionValue.isEmpty() || exclusionValue.contains(recordsetBaseName));
			if (result) log.fine(() -> String.format("recordset excluded %s %s", dataFilePath.toString(), recordsetBaseName)); //$NON-NLS-1$
			return result;
		} else {
			return false;
		}
	}

	/**
	 * Deletes all exclusions property files from the user directory and the data directories.
	 * @param dataDirectories
	 */
	public static void deleteExclusionsDirectory(List<Path> dataDirectories) {
		FileUtils.deleteDirectory(getUserExclusionsDir().toString());
		for (Path dataPath : dataDirectories) {
			try {
				for (File file : FileUtils.getFileListing(dataPath.toFile(), Integer.MAX_VALUE, Settings.HISTO_EXCLUSIONS_FILE_NAME)) {
					FileUtils.deleteFile(file.getPath());
				}
			} catch (FileNotFoundException e) {
				log.log(WARNING, e.getMessage(), e);
			}
		}
	}

	/**
	 * Determine the exclusions which are active for the current channel.
	 * @return the exclusion information for the trusses excluded from the history
	 */
	public static String[] getExcludedTrusses() {
		return DataExplorer.getInstance().getPresentHistoExplorer().getHistoSet().getExcludedPaths().stream() //
				.map(p -> {
					String key = p.getFileName().toString();
					String property = getInstance().getProperty(key);
					return (property.isEmpty() ? key : key + GDE.STRING_BLANK_COLON_BLANK + property);
				}) //
				.distinct().sorted(Collections.reverseOrder()) //
				.toArray(String[]::new);
	}

	private ExclusionData(Path newDataFileDir) {
		super();
		this.dataFileDir = newDataFileDir;
		load();
	}

	@Override
	public synchronized String toString() {
		return this.stringPropertyNames().stream().sorted().map(this::getProperty) //
				.map(k -> k.isEmpty() ? k : k + GDE.STRING_BLANK_COLON_BLANK + getProperty(k)) //
				.collect(Collectors.joining(GDE.STRING_NEW_LINE));
	}

	@Override
	public synchronized Object setProperty(String dataFileName, String recordsetBaseName) {
		if (recordsetBaseName.isEmpty()) throw new UnsupportedOperationException();

		return super.setProperty(dataFileName, recordsetBaseName);
	}

	/**
	 * Set the dataFileName as excluded for all recordSets.
	 * @param dataFileName
	 * @return the previous value which is a csv string
	 */
	public synchronized Object setProperty(String dataFileName) {
		return super.setProperty(dataFileName, GDE.STRING_EMPTY);
	}

	/**
	 * @param dataFileName
	 * @param recordsetBaseName
	 * @return the previous value
	 */
	public Object addToProperty(String dataFileName, String recordsetBaseName) {
		if (recordsetBaseName.isEmpty()) throw new UnsupportedOperationException();

		if (this.getProperty(dataFileName) == null)
			return super.setProperty(dataFileName, recordsetBaseName);
		else {
			if (this.getProperty(dataFileName).isEmpty() || this.getProperty(dataFileName).contains(recordsetBaseName))
				return this.getProperty(dataFileName);
			else
				return super.setProperty(dataFileName, this.getProperty(dataFileName) + GDE.STRING_BLANK_PLUS_BLANK + recordsetBaseName);
		}
	}

	private void load() {
		boolean takeUserDir = Settings.getInstance().isDataSettingsAtHomePath();
		if (!takeUserDir) {
			FileUtils.checkDirectoryAndCreate(this.dataFileDir.toString());
			if (this.dataFileDir.resolve(Settings.HISTO_EXCLUSIONS_FILE_NAME).toFile().exists()) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(
						this.dataFileDir.resolve(Settings.HISTO_EXCLUSIONS_FILE_NAME).toFile()), "UTF-8"))) { //$NON-NLS-1$
					this.load(reader);
				} catch (Exception e) {
					if (e instanceof FileNotFoundException)
						takeUserDir = true; // supports write protected data drives
					else
						log.log(SEVERE, e.getLocalizedMessage(), e);
				}
			}
		} else {
			Path exclusionsDir = getUserExclusionsDir();
			FileUtils.checkDirectoryAndCreate(exclusionsDir.toString());
			String fileName = SecureHash.sha1(this.dataFileDir.toString());
			if (exclusionsDir.resolve(fileName).toFile().exists()) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(exclusionsDir.resolve(fileName).toFile())))) {
					this.load(reader);
				} catch (Throwable e) {
					log.log(SEVERE, e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * @return
	 */
	private static Path getUserExclusionsDir() {
		return Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_EXCLUSIONS_DIR_NAME);
	}

	/**
	 * Write the file if excludes are defined, else delete the file.
	 */
	public void store() {
		Path exclusionsDir = getUserExclusionsDir();
		if (this.size() > 0) {
			boolean takeUserDir = Settings.getInstance().isDataSettingsAtHomePath();
			if (!takeUserDir) {
				FileUtils.checkDirectoryAndCreate(this.dataFileDir.toString());
				try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
						this.dataFileDir.resolve(Settings.HISTO_EXCLUSIONS_FILE_NAME).toFile()), "UTF-8"))) { //$NON-NLS-1$
					this.store(writer, this.dataFileDir.toString());
				} catch (Throwable e) {
					if (e instanceof FileNotFoundException)
						takeUserDir = true; // supports write protected data drives
					else
						log.log(SEVERE, e.getMessage(), e);
				}
			} else {
				FileUtils.checkDirectoryAndCreate(exclusionsDir.toString());
				String fileName = SecureHash.sha1(this.dataFileDir.toString());
				try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(exclusionsDir.resolve(fileName).toFile()),
						"UTF-8"))) { //$NON-NLS-1$
					this.store(writer, this.dataFileDir.toString());
				} catch (Throwable e) {
					log.log(SEVERE, e.getMessage(), e);
				}
			}
		} else
			delete();
	}

	public void delete() {
		Path exclusionsDir = getUserExclusionsDir();
		log.log(FINE, "deleted : ", exclusionsDir);
		FileUtils.deleteFile(this.dataFileDir.resolve(Settings.HISTO_EXCLUSIONS_FILE_NAME).toString());
		String fileName = SecureHash.sha1(this.dataFileDir.toString());
		FileUtils.deleteFile(this.dataFileDir.resolve(exclusionsDir.resolve(fileName)).toString());

		currentInstance = null;
	}

}
