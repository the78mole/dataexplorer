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

package gde.histo.exclusions;

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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import gde.GDE;
import gde.config.Settings;
import gde.histo.utils.SecureHash;
import gde.log.Logger;
import gde.utils.FileUtils;

/**
 * Including records from summary warnings via user activity.
 * Is based on property files for each data directory.
 * Stores the inclusions property files in the user directory if the data directory is not accessible for writing.
 * @author Thomas Eickert
 */
public final class InclusionData extends Properties {
	private final static String		$CLASS_NAME				= InclusionData.class.getName();
	private final static Logger		log								= Logger.getLogger($CLASS_NAME);
	private static final long			serialVersionUID	= -2477509505185819765L;

	private static InclusionData	currentInstance;

	private final Path						dataDir;

	/**
	 * @param newDataDir
	 * @return the instance which is only created anew if the data file directory has changed
	 */
	public static InclusionData getInstance(Path newDataDir) {
		if (currentInstance == null || !currentInstance.dataDir.equals(newDataDir)) {
			currentInstance = new InclusionData(newDataDir);
		}
		return currentInstance;
	}

	/**
	 * @return the instance in case the property file already exists with any of the record names
	 */
	public static Optional<InclusionData> getExistingInstance(Path dataDir, String[] recordNames) {
		if (currentInstance == null || !currentInstance.dataDir.equals(dataDir)) {
			currentInstance = new InclusionData(dataDir);
		}
		return currentInstance.hasIncludedRecords(recordNames)  ? Optional.of(currentInstance) : Optional.empty();
	}

	/**
	 * @return true if any inclusions for this directory are active
	 */
	public boolean hasIncludedRecords(String[] recordNames) {
		return !stringPropertyNames().isEmpty() && Arrays.stream(recordNames).anyMatch(s -> stringPropertyNames().contains(s));
	}

	/**
	 * Deletes all inclusions property files from the user directory and the data directories.
	 * @param dataDirectories
	 */
	public static void deleteInclusionsDirectory(List<Path> dataDirectories) {
		FileUtils.deleteDirectory(getUserInclusionsDir().toString());
		for (Path dataPath : dataDirectories) {
			try {
				for (File file : FileUtils.getFileListing(dataPath.toFile(), Integer.MAX_VALUE, Settings.HISTO_INCLUSIONS_FILE_NAME)) {
					FileUtils.deleteFile(file.getPath());
				}
			} catch (FileNotFoundException e) {
				log.log(WARNING, e.getMessage(), e);
			}
		}
	}

	private InclusionData(Path newDataDir) {
		super();
		this.dataDir = newDataDir;
		load();
	}

	/**
	 * @return true if an inclusion for the record in the data file is active
	 */
	public boolean isIncluded(String recordName) {
		if (recordName.isEmpty()) throw new UnsupportedOperationException();

		String inclusionValue = getProperty(recordName);
		boolean result = inclusionValue != null;
		if (result) log.fine(() -> String.format("record included %s %s", dataDir.toString(), recordName));
		return result;
	}

	@Override
	public synchronized String toString() {
		return this.stringPropertyNames().stream() //
				.sorted(Comparator.comparing(k -> getProperty(k))) //
				.map(k -> getProperty(k).isEmpty() ? k : k + GDE.STRING_BLANK_COLON_BLANK + getProperty(k)) //
				.collect(Collectors.joining(","));
	}

	@Override
	public synchronized InclusionData setProperty(String recordName, String irrelevantValue) {
		if (recordName.isEmpty()) throw new UnsupportedOperationException();

		return (InclusionData) super.setProperty(recordName, irrelevantValue);
	}

	/**
	 * @return the previous value which is a csv string
	 */
	public synchronized InclusionData setProperty(String recordName) {
		return (InclusionData) super.setProperty(recordName, GDE.STRING_EMPTY);
	}

	/**
	 * Deletes and purges the file if there is no property left.
	 * @return the property value prior to deletion
	 */
	public synchronized String deleteProperty(String recordName) {
		String value = (String) remove(recordName);
		return value;
	}

	private void load() {
		boolean takeUserDir = Settings.getInstance().isDataSettingsAtHomePath();
		if (!takeUserDir) {
			FileUtils.checkDirectoryAndCreate(this.dataDir.toString());
			if (this.dataDir.resolve(Settings.HISTO_INCLUSIONS_FILE_NAME).toFile().exists()) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(
						this.dataDir.resolve(Settings.HISTO_INCLUSIONS_FILE_NAME).toFile()), "UTF-8"))) {
					this.load(reader);
				} catch (Exception e) {
					if (e instanceof FileNotFoundException)
						takeUserDir = true; // supports write protected data drives
					else
						log.log(SEVERE, e.getLocalizedMessage(), e);
				}
			}
		} else {
			Path exclusionsDir = getUserInclusionsDir();
			FileUtils.checkDirectoryAndCreate(exclusionsDir.toString());
			String fileName = SecureHash.sha1(this.dataDir.toString());
			if (exclusionsDir.resolve(fileName).toFile().exists()) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream( //
						exclusionsDir.resolve(fileName).toFile())))) {
					this.load(reader);
				} catch (Throwable e) {
					log.log(SEVERE, e.getMessage(), e);
				}
			}
		}
	}

	private static Path getUserInclusionsDir() {
		return Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_INCLUSIONS_DIR_NAME);
	}

	/**
	 * Write the file if excludes are defined, else delete the file.
	 */
	public void store() {
		Path exclusionsDir = getUserInclusionsDir();
		if (this.size() > 0) {
			boolean takeUserDir = Settings.getInstance().isDataSettingsAtHomePath();
			if (!takeUserDir) {
				FileUtils.checkDirectoryAndCreate(this.dataDir.toString());
				try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
						this.dataDir.resolve(Settings.HISTO_INCLUSIONS_FILE_NAME).toFile()), "UTF-8"))) {
					this.store(writer, this.dataDir.toString());
				} catch (Throwable e) {
					if (e instanceof FileNotFoundException)
						takeUserDir = true; // supports write protected data drives
					else
						log.log(SEVERE, e.getMessage(), e);
				}
			} else {
				FileUtils.checkDirectoryAndCreate(exclusionsDir.toString());
				String fileName = SecureHash.sha1(this.dataDir.toString());
				try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream( //
						exclusionsDir.resolve(fileName).toFile()), "UTF-8"))) {
					this.store(writer, this.dataDir.toString());
				} catch (Throwable e) {
					log.log(SEVERE, e.getMessage(), e);
				}
			}
		} else {
			delete();
		}
	}

	public void delete() {
		Path exclusionsDir = getUserInclusionsDir();
		FileUtils.deleteFile(this.dataDir.resolve(Settings.HISTO_INCLUSIONS_FILE_NAME).toString());
		String fileName = SecureHash.sha1(this.dataDir.toString());
		FileUtils.deleteFile(this.dataDir.resolve(exclusionsDir.resolve(fileName)).toString());

		currentInstance = null;
	}

	/**
	 * @return the names of the records with warnings activated
	 */
	public String[] getIncludedRecordNames() {
		return InclusionData.getInstance(dataDir).stringPropertyNames().toArray(new String[0]);
	}

	/**
	 * @return the names of the records with warnings activated
	 */
	public String[] getIncludedRecordNames(String[] validRecordNames) {
		Set<String> propertyNames = InclusionData.getInstance(dataDir).stringPropertyNames();
		return Arrays.stream(validRecordNames).filter(s -> propertyNames.contains(s)).toArray(String[]::new);
	}

}
