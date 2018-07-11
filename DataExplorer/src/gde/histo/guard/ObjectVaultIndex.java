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

package gde.histo.guard;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import gde.Analyzer;
import gde.GDE;
import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.HistoVault;
import gde.histo.cache.SimpleVaultReader;
import gde.histo.device.IHistoDevice;
import gde.histo.exclusions.ExclusionData;
import gde.log.Level;
import gde.log.Logger;
import gde.utils.FileUtils;

/**
 * Supports access to the vaults.
 * The index corresponds to the current property settings and device settings.
 * Uses a the fleet directory on the file system.
 * @author Thomas Eickert (USER)
 */
public final class ObjectVaultIndex {
	private static final String	$CLASS_NAME	= ObjectVaultIndex.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * Implements the equals method in order to decide if two function objects represent the same query.
	 * Use a best-fit create method as a prerequisite for function result caching.
	 */
	static class DetailSelector implements Function<ObjectVaultIndexEntry, Boolean> {

		private final String																		filterText;
		private final Function<ObjectVaultIndexEntry, Boolean>	filterFunction;

		@Override
		public Boolean apply(ObjectVaultIndexEntry t) {
			return filterFunction.apply(t);
		}

		static DetailSelector createEmptyFilter() {
			return new DetailSelector(GDE.STRING_EMPTY, t -> true);
		}

		static DetailSelector createDeviceNameFilter(Collection<String> deviceNames) {
			ArrayList<String> names = new ArrayList<String>(deviceNames);
			Collections.sort(names);
			return new DetailSelector("deviceName:" + names.toString(), t -> names.contains(t.vaultDeviceName));
		}

		static DetailSelector createFunctionFilter(Function<ObjectVaultIndexEntry, Boolean> function) {
			return new DetailSelector("function:" + function.toString(), function);
		}

		/**
		 * Use the create methods instead.
		 */
		private DetailSelector(String filterText, Function<ObjectVaultIndexEntry, Boolean> filterFunction) {
			this.filterText = filterText;
			this.filterFunction = filterFunction;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((this.filterText == null) ? 0 : this.filterText.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			DetailSelector other = (DetailSelector) obj;
			if (this.filterText == null) {
				if (other.filterText != null) return false;
			} else if (!this.filterText.equals(other.filterText)) return false;
			return true;
		}

	}

	/**
	 * The keys for accessing vaults are the directory name and the file name.
	 */
	public static class VaultKeyPair extends SimpleImmutableEntry<String, String> {
		private static final long serialVersionUID = 8937538273315451095L;

		/**
		 * @param directoryName is a valid cache folder entry (directory or zip file)
		 * @param vaultName is the SHA1 name of the vault
		 */
		public VaultKeyPair(String directoryName, String vaultName) {
			super(directoryName, vaultName);
		}
	}

	/**
	 * The index map supports retrieving a selection of keys.
	 * The keys hold the information to access the vaults in the cache directory.
	 */
	static class ObjectVaultMap {
		@SuppressWarnings("hiding")
		private static final Logger							log									= Logger.getLogger(ObjectVaultMap.class.getName());

		/**
		 * Key is the objectKey, the list holds the vault index attributes.
		 */
		private final Map<String, List<String>>	indexMap;																																// todo change to
																																																										// List<ObjectVaultIndexEntry>

		private Set<ObjectVaultIndexEntry>			lastVaultIndexes		= new HashSet<>();
		private String[]												lastObjectKeys			= new String[] {};
		private DetailSelector									lastDetailSelector	= null;

		/**
		 * Use this for retrieving the full index from the file system.
		 */
		public ObjectVaultMap() {
			FilenameFilter filenameFilter = (dir, name) -> true;
			this.indexMap = loadIndex(filenameFilter);
		}

		/**
		 * Use this for retrieving a partial index to reduce file system access.
		 * @param objectKeys specifies the user's object key selection
		 */
		public ObjectVaultMap(String[] objectKeys) {
			List<String> keys = Arrays.asList(objectKeys);
			FilenameFilter filenameFilter = (dir, name) -> keys.contains(name);
			this.indexMap = loadIndex(filenameFilter);
		}

		/**
		 * Recreate the fleet index directory.
		 */
		public void storeIndex(Map<String, List<String>> indexMap) {
			String targetDir = getFleetDirectory().toString();
			FileUtils.deleteDirectory(targetDir);
			FileUtils.checkDirectoryAndCreate(targetDir);
			indexMap.forEach((objectKey, list) -> {
				try (FileWriter writer = new FileWriter(Paths.get(targetDir, objectKey).toFile())) {
					for (String str : list) {
						writer.write(str + GDE.STRING_NEW_LINE);
					}
				} catch (IOException e) {
					log.log(Level.SEVERE, "write", e);
				}
			});

			this.indexMap.clear();
			this.indexMap.putAll(indexMap);
		}

		/**
		 * @param objectSelector specifies the user's object key selection
		 * @return the object key index portion from the file system
		 */
		private Map<String, List<String>> loadIndex(FilenameFilter filenameFilter) {
			Map<String, List<String>> result = new HashMap<>();
			Path fleetDirectory = getFleetDirectory();
			FileUtils.checkDirectoryAndCreate(fleetDirectory.toString());
			for (String fileName : fleetDirectory.toFile().list(filenameFilter)) {
				Path path = fleetDirectory.resolve(fileName);
				ArrayList<String> indexList = new ArrayList<>();
				try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
					String line;
					while ((line = br.readLine()) != null) {
						if (!line.isEmpty()) {
							indexList.add(line);
						}
					}
				} catch (Exception e) {
					log.log(Level.SEVERE, "read", e);
				}
				result.put(fileName, indexList);
			}
			log.off(() -> "objects: " + result.size() + "  total vaults: " + result.values().stream().mapToLong(l -> l.size()).sum());
			return result;
		}

		/**
		 * @param objectKeys specifies the user's object key selection
		 * @param detailSelector specifies the valid vault index entries
		 * @return the vault indices
		 */
		public Set<ObjectVaultIndexEntry> selectVaultIndexes(String[] objectKeys, DetailSelector detailSelector) {
			if (Arrays.equals(this.lastObjectKeys, objectKeys) && detailSelector.equals(this.lastDetailSelector)) {
				return this.lastVaultIndexes;
			} else if (objectKeys.length == 1) {
				this.lastObjectKeys = objectKeys;
				this.lastDetailSelector = detailSelector;
				this.lastVaultIndexes = selectVaultIndexes(objectKeys[0], detailSelector);
				return this.lastVaultIndexes;
			} else {
				this.lastObjectKeys = objectKeys;
				this.lastDetailSelector = detailSelector;
				this.lastVaultIndexes = selectVaultIndexes(s -> Arrays.asList(objectKeys).contains(s), detailSelector);
				return this.lastVaultIndexes;
			}
		}

		/**
		 * @param objectKey
		 * @param detailSelector
		 * @return the vault indices
		 */
		public Set<ObjectVaultIndexEntry> selectVaultIndexes(String objectKey, DetailSelector detailSelector) {
			Set<ObjectVaultIndexEntry> indices = new HashSet<>();
			Path dataFilePath = Paths.get(Settings.getInstance().getDataFilePath());
			ExclusionData exclusionData = new ExclusionData(dataFilePath.resolve(objectKey));
			for (Entry<String, List<String>> e : indexMap.entrySet()) {
				if (!objectKey.equals(e.getKey())) continue;
				String prefix = GDE.STRING_CSV_QUOTE + e.getKey() + GDE.STRING_CSV_QUOTE + GDE.STRING_CSV_SEPARATOR;
				for (String indexCsv : e.getValue()) {
					ObjectVaultIndexEntry idx = new ObjectVaultIndexEntry(prefix + indexCsv);
					if (!detailSelector.apply(idx)) continue;
					if (!exclusionData.isExcluded(Paths.get(idx.logFilePath))) indices.add(idx);
				}
			}
			return indices;
		}

		/**
		 * @param objectSelector
		 * @param detailSelector
		 * @return the vault indices
		 */
		public Set<ObjectVaultIndexEntry> selectVaultIndexes(Function<String, Boolean> objectSelector, DetailSelector detailSelector) {
			Path dataFilePath = Paths.get(Settings.getInstance().getDataFilePath());
			Function<ObjectVaultIndexEntry, Boolean> isExcludedSelector = idx -> {
				ExclusionData exclusionData = new ExclusionData(dataFilePath.resolve(idx.vaultObjectKey));
				return exclusionData.isExcluded(Paths.get(idx.logFilePath));
			};

			Set<ObjectVaultIndexEntry> indices = new HashSet<>();
			for (Entry<String, List<String>> e : indexMap.entrySet()) {
				if (!objectSelector.apply(e.getKey())) continue;
				String prefix = GDE.STRING_CSV_QUOTE + e.getKey() + GDE.STRING_CSV_QUOTE + GDE.STRING_CSV_SEPARATOR;
				for (String indexCsv : e.getValue()) {
					ObjectVaultIndexEntry idx = new ObjectVaultIndexEntry(prefix + indexCsv);
					if (!detailSelector.apply(idx)) continue;
					if (!isExcludedSelector.apply(idx)) indices.add(idx);
				}
			}
			return indices;
		}

		public Set<String> selectDeviceNames(String[] objectKeys, DetailSelector detailSelector) {
			Set<ObjectVaultIndexEntry> vaultIndexes = selectVaultIndexes(objectKeys, detailSelector);
			Set<String> deviceNames = new HashSet<>();
			for (ObjectVaultIndexEntry idx : vaultIndexes) {
				deviceNames.add(idx.vaultDeviceName);
			}
			return deviceNames;
		}

	}

	/**
	 * Represents an index item.
	 */
	static class ObjectVaultIndexEntry {
		@SuppressWarnings("hiding")
		private static final Logger	log	= Logger.getLogger(ObjectVaultIndexEntry.class.getName());

		String											vaultObjectKey;

		String											vaultName;
		String											vaultDirectory;
		String											vaultReaderSettingsCsv;
		long												vaultCreatedMs;
		String											vaultDataExplorerVersion;
		String											vaultDeviceKey;
		String											vaultDeviceName;
		int													vaultChannelNumber;
		long												vaultSamplingTimespanMs;

		long												logFileLastModified;
		long												logFileLength;
		int													logRecordSetOrdinal;
		String											logRecordsetBaseName;
		int													logChannelNumber;
		long												logStartTimestampMs;
		String											logFilePath;

		static Function<HistoVault, Map.Entry<String, String>> objectIndexExtractor() {
			return v -> {
				final String d = GDE.STRING_CSV_SEPARATOR;
				String readerSettings = v.getVaultReaderSettings().replace(GDE.STRING_CSV_SEPARATOR, GDE.STRING_UNDER_BAR);
				String attributes = v.getVaultName() + d + v.getVaultDirectory() + d + readerSettings + d + v.getVaultCreated_ms() //
						+ d + v.getVaultDataExplorerVersion() + d + v.getVaultDeviceKey() + d + v.getVaultDeviceName() + d + v.getVaultChannelNumber() + d + v.getVaultSamplingTimespan_ms() //
						+ d + v.getLogFileLastModified() + d + v.getLogFileLength() + d + v.getLogRecordSetOrdinal() + d + v.getLogRecordsetBaseName() //
						+ d + v.getLogChannelNumber() + d + v.getLogStartTimestamp_ms() + d + v.getLogFilePath();
				return new AbstractMap.SimpleImmutableEntry<>(v.getVaultObjectKey(), attributes);
			};
		}

		static Function<HistoVault, ObjectVaultIndexEntry> objectKeyExtractor() {
			return v -> {
				ObjectVaultIndexEntry idx = new ObjectVaultIndexEntry();
				idx.vaultObjectKey = v.getVaultObjectKey();

				idx.vaultName = v.getVaultName();
				idx.vaultDirectory = v.getVaultDirectory();
				idx.vaultReaderSettingsCsv = v.getVaultReaderSettings();
				idx.vaultCreatedMs = v.getVaultCreated_ms();
				idx.vaultDataExplorerVersion = v.getVaultDataExplorerVersion();
				idx.vaultDeviceKey = v.getVaultDeviceKey();
				idx.vaultDeviceName = v.getVaultDeviceName();
				idx.vaultChannelNumber = v.getVaultChannelNumber();
				idx.vaultSamplingTimespanMs = v.getVaultSamplingTimespan_ms();

				idx.logFileLastModified = v.getLogFileLastModified();
				idx.logFileLength = v.getLogFileLength();
				idx.logRecordSetOrdinal = v.getLogRecordSetOrdinal();
				idx.logRecordsetBaseName = v.getLogRecordsetBaseName();
				idx.logChannelNumber = v.getLogChannelNumber();
				idx.logStartTimestampMs = v.getLogStartTimestamp_ms();
				idx.logFilePath = v.getLogFilePath();
				return idx;
			};
		}

		private ObjectVaultIndexEntry() {
		}

		/**
		 * @param fieldsCsv holds the objectKey enclosed in quotes plus additional attributes
		 */
		public ObjectVaultIndexEntry(String fieldsCsv) {
			log.log(Level.FINEST, fieldsCsv);
			String[] values = fieldsCsv.split(GDE.STRING_CSV_SEPARATOR);
			this.vaultObjectKey = values[0].replace(GDE.STRING_CSV_QUOTE, "");

			this.vaultName = values[1];
			this.vaultDirectory = values[2];
			this.vaultReaderSettingsCsv = values[3].replace(GDE.STRING_UNDER_BAR, GDE.STRING_CSV_SEPARATOR);
			this.vaultCreatedMs = Long.parseLong(values[4]);
			this.vaultDataExplorerVersion = values[5];
			this.vaultDeviceKey = values[6];
			this.vaultDeviceName = values[7];
			this.vaultChannelNumber = Integer.parseInt(values[8]);
			this.vaultSamplingTimespanMs = Long.parseLong(values[9]);

			this.logFileLastModified = Long.parseLong(values[10]);
			this.logFileLength = Long.parseLong(values[11]);
			this.logRecordSetOrdinal = Integer.parseInt(values[12]);
			this.logRecordsetBaseName = values[13];
			this.logChannelNumber = Integer.parseInt(values[14]);
			this.logStartTimestampMs = Long.parseLong(values[15]);
			this.logFilePath = values[16];
		}

		@Override
		public String toString() {
			String d = GDE.STRING_CSV_SEPARATOR;
			String readerSettings = this.vaultReaderSettingsCsv.replace(GDE.STRING_CSV_SEPARATOR, GDE.STRING_UNDER_BAR);
			return GDE.STRING_CSV_QUOTE + this.vaultObjectKey + GDE.STRING_CSV_QUOTE + d + this.vaultName + d + this.vaultDirectory + d + readerSettings + d + this.vaultCreatedMs //
					+ d + this.vaultDataExplorerVersion + d + this.vaultDeviceKey + d + this.vaultDeviceName + d + this.vaultChannelNumber + d + this.vaultSamplingTimespanMs //
					+ d + this.logFileLastModified + d + this.logFileLength + d + this.logRecordSetOrdinal + d + this.logRecordsetBaseName //
					+ d + this.logChannelNumber + d + this.logStartTimestampMs + d + this.logFilePath;
		}

	}

	public static Path getFleetDirectory() {
		return Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_OBJECTS_DIR_NAME);
	}

	private final ObjectVaultMap objectVaultMap;

	/**
	 *
	 */
	public ObjectVaultIndex() {
		objectVaultMap = new ObjectVaultMap();
		log.log(Level.OFF, "vault key index loaded");
	}

	public ObjectVaultIndex(String[] objectKeys) {
		objectVaultMap = new ObjectVaultMap(objectKeys);
		log.log(Level.OFF, "vault key index loaded for", objectKeys);
	}

	/**
	 * Reads all vaults and builds the fleet directory.
	 */
	public void rebuild() {
		List<String> cacheDirectoryNames = defineCacheDirectoryNames();
		objectVaultMap.storeIndex(readIndex(cacheDirectoryNames));
		log.off(() -> "Fleet directory      size=" + getFleetDirectory().toFile().listFiles().length);
	}

	/**
	 * @param objectSelector specifies the user's object key selection
	 * @return the object key entries in the vault index
	 */
	public String[] selectObjectKeys(Function<String, Boolean> objectSelector) {
		FilenameFilter filter = (dir, name) -> objectSelector.apply(name);
		return getFleetDirectory().toFile().list(filter);
	}

	/**
	 * @param objectKeys specifies the user's object key selection
	 * @return the devices used in the vault index selection
	 */
	public HashMap<String, IDevice> selectExistingDevices(String[] objectKeys) {
		Set<String> indexedDeviceNames = objectVaultMap.selectDeviceNames(objectKeys, DetailSelector.createEmptyFilter());

		HashMap<String, IDevice> existingDevices = new HashMap<>();
		Map<String, DeviceConfiguration> allConfigurations = Analyzer.getInstance().getDeviceConfigurations().getAllConfigurations();
		for (String deviceName : indexedDeviceNames) {
			try {
				IDevice device = allConfigurations.get(deviceName).getAsDevice();
				existingDevices.put(deviceName, device);
			} catch (Exception e) {
				log.log(Level.SEVERE, "device instance exception", e);
			}
		}
		log.log(Level.OFF, "Selected      size=", existingDevices.size());
		return existingDevices;
	}

	/**
	 * @param detailSelector specifies the valid vault index entries
	 * @return the vault key entries consisting of directory name and vault name
	 */
	public Set<VaultKeyPair> selectVaultKeys(DetailSelector detailSelector) {
		Function<String, Boolean> objectSelector = s -> true;
		Set<VaultKeyPair> result = new HashSet<>();
		for (ObjectVaultIndexEntry idx : objectVaultMap.selectVaultIndexes(objectSelector, detailSelector)) {
			result.add(new VaultKeyPair(idx.vaultDirectory, idx.vaultName));
		}
		log.off(() -> "Selected      size=" + result.size());
		return result;
	}

	/**
	 * @param objectKeys specifies the user's object key selection
	 * @param detailSelector specifies the valid vault index entries
	 * @return the vault key entries consisting of directory name and vault name
	 */
	public Set<VaultKeyPair> selectVaultKeys(String[] objectKeys, DetailSelector detailSelector) {
		Set<VaultKeyPair> result = new HashSet<>();
		for (ObjectVaultIndexEntry idx : objectVaultMap.selectVaultIndexes(objectKeys, detailSelector)) {
			result.add(new VaultKeyPair(idx.vaultDirectory, idx.vaultName));
		}
		log.off(() -> "Selected      size=" + result.size());
		return result;
	}

	/**
	 * @param objectKey specifies the user's object key selection
	 * @param detailSelector specifies the valid vault index entries
	 * @param classifier specifies the group key values
	 * @return the map with the key acc. to the classifier and the keys for accessing the vaults
	 */
	public TreeMap<String, List<VaultKeyPair>> selectGroupedVaultKeys(String objectKey, DetailSelector detailSelector,
			Function<ObjectVaultIndexEntry, String> classifier) {
		TreeMap<String, List<VaultKeyPair>> result = new TreeMap<>();
		Set<ObjectVaultIndexEntry> indexEntriesA = objectVaultMap.selectVaultIndexes(new String[] { objectKey }, detailSelector);
		for (ObjectVaultIndexEntry idx : indexEntriesA) {
			String key = classifier.apply(idx);
			if (result.get(key) == null) result.put(key, new ArrayList<>());
			result.get(key).add(new VaultKeyPair(idx.vaultDirectory, idx.vaultName));
		}
		log.log(Level.OFF, "Size   ", result.size());
		return result;
	}

	/**
	 * @return the current vault directory names based on settings etc.
	 */
	private List<String> defineCacheDirectoryNames(String deviceName) {
		List<String> result = new ArrayList<>();
		try {
			IDevice device = Analyzer.getInstance().getDeviceConfigurations().get(deviceName).getAsDevice();

			List<String> validReaderSettings = new ArrayList<>();
			validReaderSettings.add(GDE.STRING_EMPTY);
			if (device instanceof IHistoDevice) validReaderSettings.add(((IHistoDevice) device).getReaderSettingsCsv());

			for (String vaultReaderSettings : validReaderSettings) {
				for (int j = 1; j <= device.getChannelCount(); j++) {
					result.add(ExtendedVault.getVaultsDirectoryName(device, j, vaultReaderSettings));
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "device instance exception", e);
		}
		return result;
	}

	/**
	 * @return the current vault directory names based on settings etc.
	 */
	private List<String> defineCacheDirectoryNames() {
		List<String> result = new ArrayList<>();
		Collection<String> deviceNames = Analyzer.getInstance().getDeviceConfigurations().getAllConfigurations().keySet();
		for (String deviceName : deviceNames) {
			result.addAll(defineCacheDirectoryNames(deviceName));
		}
		log.off(() -> "cache directories: " + result.size() + "  from deviceNames: " + deviceNames.size());
		return result;
	}

	/**
	 * @return the list of vault index data with key objectKey
	 */
	private Map<String, List<String>> readIndex(List<String> cacheDirectoryNames) {
		Map<String, List<String>> objectKeyMap = new HashMap<>();
		try {
			for (String directoryName : cacheDirectoryNames) {
				List<Entry<String, String>> vaultIndices = SimpleVaultReader.readVaultsIndices(directoryName, ObjectVaultIndexEntry.objectIndexExtractor());
				for (Entry<String, String> e : vaultIndices) {
					String objectKey = e.getKey().isEmpty() ? "leer" : e.getKey(); // todo replace leer
					if (objectKeyMap.get(objectKey) == null) {
						objectKeyMap.put(objectKey, new ArrayList<>());
					}
					objectKeyMap.get(objectKey).add(e.getValue());
				}
			}
		} catch (IOException e) {
			log.log(Level.SEVERE, "read", e);
			return objectKeyMap;
		}
		log.off(() -> "objectKeys       selected=" + objectKeyMap.keySet().size() + "  " + new TreeSet<String>(objectKeyMap.keySet()).toString());
		log.off(() -> "vaults           selected=" + objectKeyMap.values().parallelStream().mapToInt(l -> l.size()).sum());
		return objectKeyMap;
	}

}
