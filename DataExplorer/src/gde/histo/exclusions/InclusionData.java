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

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import gde.GDE;
import gde.config.Settings;
import gde.histo.innercache.Cache;
import gde.histo.innercache.CacheBuilder;
import gde.histo.innercache.CacheStats;
import gde.histo.utils.SecureHash;
import gde.log.Logger;
import gde.utils.FileUtils;

/**
 * Including records from summary reminders via user activity.
 * Is based on property files for the current primary folder.
 * Stores the inclusions property files in the user directory or the primary folder.
 * @author Thomas Eickert
 */
public final class InclusionData extends Properties {
	private static final String												$CLASS_NAME				= InclusionData.class.getName();
	private static final Logger												log								= Logger.getLogger($CLASS_NAME);
	private static final long													serialVersionUID	= -2477509505185819765L;

	private static final Cache<String, InclusionData>	memoryCache				=																//
			CacheBuilder.newBuilder().maximumSize(111).recordStats().build();																// key is the file Name

	private final Path																activeFolder;

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

	/**
	 * Use the singleton getInstance instead for calls by the legacy UI based on the settings (object key and active device name).
	 * @param activeFolder is the current data files folder based on the object key and active device
	 */
	public InclusionData(Path activeFolder) {
		super();
		this.activeFolder = activeFolder;

		String fileName = SecureHash.sha1(this.activeFolder.toString());
		Properties cachedInstance = memoryCache.getIfPresent(fileName);
		log.finer(() -> {
			CacheStats stats = memoryCache.stats();
			return String.format("evictionCount=%d  hitCount=%d  missCount=%d hitRate=%f missRate=%f", stats.evictionCount(), stats.hitCount(), stats.missCount(), stats.hitRate(), stats.missRate());
		});
		if (cachedInstance == null) {
			load();
		} else {
			this.putAll(cachedInstance);
		}
	}

	public boolean isIncluded(String recordName, boolean defaultValue) {
		if (recordName.isEmpty()) throw new UnsupportedOperationException();

		boolean included = defaultValue;
		if (!this.isEmpty()) {
			included = getProperty(recordName) != null;
			if (included) log.fine(() -> String.format("record included by user %s %s", activeFolder.toString(), recordName));
		}
		return included;
	}

	@Override
	public synchronized String toString() {
		return this.stringPropertyNames().stream() //
				.sorted(Comparator.comparing(this::getProperty)).map(this::getProperty) //
				.map(k -> k.isEmpty() ? k : k + GDE.STRING_BLANK_COLON_BLANK + k) //
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
			FileUtils.checkDirectoryAndCreate(this.activeFolder.toString());
			if (this.activeFolder.resolve(Settings.HISTO_INCLUSIONS_FILE_NAME).toFile().exists()) {
				try (Reader reader = new InputStreamReader(new FileInputStream(this.activeFolder.resolve(Settings.HISTO_INCLUSIONS_FILE_NAME).toFile()),
						"UTF-8")) {
					String fileName = SecureHash.sha1(this.activeFolder.toString());
					this.load(reader);
					memoryCache.put(fileName, this);
				} catch (Exception e) {
					if (e instanceof FileNotFoundException)
						takeUserDir = true; // supports write protected data drives
					else
						log.log(SEVERE, e.getLocalizedMessage(), e);
				}
			}
		}
		if (takeUserDir) {
			Path exclusionsDir = getUserInclusionsDir();
			FileUtils.checkDirectoryAndCreate(exclusionsDir.toString());
			String fileName = SecureHash.sha1(this.activeFolder.toString());
			if (exclusionsDir.resolve(fileName).toFile().exists()) {
				try (Reader reader = new InputStreamReader(new FileInputStream( //
						exclusionsDir.resolve(fileName).toFile()))) {
					this.load(reader);
					memoryCache.put(fileName, this);
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
				FileUtils.checkDirectoryAndCreate(this.activeFolder.toString());
				try (Writer writer = new OutputStreamWriter(new FileOutputStream(this.activeFolder.resolve(Settings.HISTO_INCLUSIONS_FILE_NAME).toFile()),
						"UTF-8")) {
					this.store(writer, this.activeFolder.toString());
					String fileName = SecureHash.sha1(this.activeFolder.toString());
					memoryCache.put(fileName, this);
				} catch (Throwable e) {
					if (e instanceof FileNotFoundException)
						takeUserDir = true; // supports write protected data drives
					else
						log.log(SEVERE, e.getMessage(), e);
				}
			}
			if (takeUserDir) {
				FileUtils.checkDirectoryAndCreate(exclusionsDir.toString());
				String fileName = SecureHash.sha1(this.activeFolder.toString());
				try (Writer writer = new OutputStreamWriter(new FileOutputStream( //
						exclusionsDir.resolve(fileName).toFile()), "UTF-8")) {
					this.store(writer, this.activeFolder.toString());
					memoryCache.put(fileName, this);
				} catch (Throwable e) {
					log.log(SEVERE, e.getMessage(), e);
				}
			}
		} else {
			delete();
			String fileName = SecureHash.sha1(this.activeFolder.toString());
			memoryCache.invalidate(fileName);
		}
	}

	public void delete() {
		Path exclusionsDir = getUserInclusionsDir();
		FileUtils.deleteFile(this.activeFolder.resolve(Settings.HISTO_INCLUSIONS_FILE_NAME).toString());
		String fileName = SecureHash.sha1(this.activeFolder.toString());
		FileUtils.deleteFile(this.activeFolder.resolve(exclusionsDir.resolve(fileName)).toString());

		memoryCache.invalidateAll(); // todo invalidate only the inclusions for the specific data file
	}

	/**
	 * @return the names of the records with reminders activated
	 */
	public String[] getIncludedRecordNames() {
		return stringPropertyNames().toArray(new String[0]);
	}

}
