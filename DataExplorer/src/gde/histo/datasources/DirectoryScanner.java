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

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.OFF;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.NotSupportedFileFormatException;
import gde.histo.cache.VaultCollector;
import gde.histo.datasources.HistoSet.RebuildStep;
import gde.histo.device.IHistoDevice;
import gde.histo.exclusions.ExclusionData;
import gde.histo.io.HistoOsdReaderWriter;
import gde.io.FileHandler;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.OperatingSystemHelper;

/**
 * Access to history source directories.
 * @author Thomas Eickert (USER)
 */
public final class DirectoryScanner {
	private final static String		$CLASS_NAME	= DirectoryScanner.class.getName();
	private final static Logger		log					= Logger.getLogger($CLASS_NAME);

	private static List<Integer>	channelMixConfigNumbers;
	private static long						minStartTimeStamp_ms;
	private static boolean				isValidatedObjectKey;

	/**
	 * Detect valid sub paths.
	 */
	private static final class SourceFolders {

		/**
		 * Designed to hold all folder paths feeding the file scanning steps.
		 * Supports an equality check comparing the current paths to an older path map.
		 */
		private final Map<DirectoryType, List<Path>> folders = new EnumMap<DirectoryType, List<Path>>(DirectoryType.class) {
			private static final long serialVersionUID = -8624409377603884008L;

			/**
			 * @return true if the keys are equal and the lists hold the same path strings in arbitrary order
			 */
			@Override
			public boolean equals(Object obj) {
				if (this == obj) return true;
				if (obj == null) return false;
				if (getClass() != obj.getClass()) return false;
				@SuppressWarnings("unchecked") // reason is anonymous class
				Map<DirectoryType, List<Path>> that = (Map<DirectoryType, List<Path>>) obj;
				boolean hasSameDirectoryTypes = keySet().equals(that.keySet());
				if (hasSameDirectoryTypes) {
					boolean isEqual = true;
					for (Entry<DirectoryType, List<Path>> entry : entrySet()) {
						HashSet<Path> thisSet = new HashSet<>(entry.getValue());
						HashSet<Path> thatSet = new HashSet<>(that.get(entry.getKey()));
						isEqual &= thisSet.equals(thatSet);
						if (!isEqual) break;
					}
					log.log(Level.OFF, "isEqual=", isEqual);
					return isEqual;
				} else {
					return false;
				}
			}
		};

		/**
		 * Determine the valid directory paths from all log sources.
		 * The result depends on the active device and object.
		 */
		void defineDirectories(EnumSet<DirectoryType> directoryTypes) throws IOException {
			folders.clear();
			for (DirectoryType directoryType : directoryTypes) {
				List<Path> currentPaths = defineCurrentPaths(directoryType);
				currentPaths.addAll(defineExtraPaths(directoryType));
				folders.put(directoryType, currentPaths);
			}
		}

		void defineDirectories4Test(Path filePath) {
			List<Path> paths = new ArrayList<>();
			Path tmpPath = filePath == null ? DirectoryType.DATA.getDataSetPath() : filePath;
			paths.add(tmpPath);
			folders.put(DirectoryType.DATA, paths);
		}

		private void removeDoubleDirectories() {
			// no, because the same directory might contribute different file types to the screening
		}

		/**
		 * Determine the valid directory paths from the currently defined working / import directory.
		 * The result depends on the active device and object.
		 * @return the list with 0 .. n entries
		 */
		private List<Path> defineCurrentPaths(DirectoryType directoryType) {
			List<Path> newPaths = new ArrayList<Path>();
			if (DataExplorer.getInstance().getActiveObject() == null) {
				Path deviceSubPath = directoryType.getActiveDeviceSubPath();
				Path rootPath = deviceSubPath != null ? directoryType.getBasePath().resolve(deviceSubPath) : directoryType.getBasePath();
				newPaths.add(rootPath);
			} else {
				Path basePath = directoryType.getBasePath();
				if (basePath == null) {
					// an unavailable path results in no files found
				} else {
					newPaths = defineObjectPaths(basePath);
				}
			}
			return newPaths;
		}

		private List<Path> defineObjectPaths(Path basePath) {
			List<Path> result = new ArrayList<Path>();
			String objectKey = DataExplorer.getInstance().getObjectKey();
			try {
				result = Files.walk(basePath).filter(Files::isDirectory) //
						.filter(p -> p.getFileName().equals(Paths.get(objectKey))).collect(Collectors.toList());
			} catch (IOException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
			return result;
		}

		/**
		 * Determine the valid directory paths from the supplementary working / import directories.
		 * They are defined in the DataExplorer properties.
		 * The result depends on the active device and object.
		 * @return the list with 0 .. n entries
		 */
		private List<Path> defineExtraPaths(DirectoryType directoryType) {
			Stream<Path> supplementFoldersStream = directoryType.getSupplementFolders();
			return supplementFoldersStream.map(p -> defineObjectPaths(p)) //
					.flatMap(List::stream).collect(Collectors.toList());
		}

		public Set<Entry<DirectoryType, List<Path>>> entrySet() {
			return folders.entrySet();
		}

		public Collection<List<Path>> values() {
			return folders.values();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((this.folders == null) ? 0 : this.folders.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			SourceFolders other = (SourceFolders) obj;
			if (this.folders == null) {
				if (other.folders != null) return false;
			} else if (!this.folders.equals(other.folders)) return false;
			return true;
		}

		@Override
		public String toString() {
			return "" + this.folders;
		}
	}

	private final DataExplorer						application								= DataExplorer.getInstance();
	private final Settings								settings									= Settings.getInstance();

	private IDevice												validatedDevice						= null;
	private Channel												validatedChannel					= null;
	private List<String>									validatedImportExtentions	= new ArrayList<>();

	private final LongAdder								nonWorkableCount					= new LongAdder();
	private final List<Path>							excludedFiles							= new ArrayList<>();

	private final EnumSet<DirectoryType>	validatedDirectoryTypes		= EnumSet.noneOf(DirectoryType.class);

	private SourceFolders									validatedFolders;

	/**
	 * Data sources supported by the history including osd link files.
	 */
	public enum DirectoryType {
		DATA {
			@Override
			public Path getBasePath() {
				String dataFilePath = Settings.getInstance().getDataFilePath();
				return dataFilePath == null || dataFilePath.isEmpty() ? null : Paths.get(dataFilePath);
			}

			@Override
			public Path getActiveDeviceSubPath() {
				return Paths.get(getPureDeviceName());
			}

			@Override
			public Path getDataSetPath() {
				String subPathData = application.getActiveObject() == null ? getPureDeviceName() : application.getObjectKey();
				return Paths.get(Settings.getInstance().getDataFilePath()).resolve(subPathData);
			}

			@Override
			public List<String> getDataSetExtensions() {
				List<String> result = new ArrayList<>();
				result.add(GDE.FILE_ENDING_OSD);
				if (device instanceof IHistoDevice && Settings.getInstance().getSearchDataPathImports()) {
					result.addAll(((IHistoDevice) device).getSupportedImportExtentions());
				}
				return result;
			}

			@Override
			public Stream<Path> getSupplementFolders() {
				return Arrays.stream(Settings.getInstance().getDataFoldersCsv().split(GDE.STRING_CSV_SEPARATOR)) //
						.map(p -> Paths.get(p));
			}
		},
		IMPORT {
			@Override
			public Path getBasePath() {
				if (application.getActiveDevice() instanceof IHistoDevice && Settings.getInstance().getSearchImportPath()) {
					Path importDir = application.getActiveDevice().getDeviceConfiguration().getImportBaseDir();
					if (importDir == null) {
						return null;
					} else {
						return importDir;
					}
				} else
					return null;
			}

			@Override
			public Path getActiveDeviceSubPath() {
				return null; // native files are not segregated by devices
			}

			@Override
			public Path getDataSetPath() {
				if (device instanceof IHistoDevice && Settings.getInstance().getSearchImportPath()) {
					Path importDir = device.getDeviceConfiguration().getImportBaseDir();
					if (importDir == null) {
						return null;
					} else {
						String subPathImport = application.getActiveObject() == null ? GDE.STRING_EMPTY : application.getObjectKey();
						Path importPath = importDir.resolve(subPathImport);
						return !importPath.equals(DirectoryType.DATA.getDataSetPath()) ? importPath : null;
					}
				} else
					return null;
			}

			@Override
			public List<String> getDataSetExtensions() {
				if (device instanceof IHistoDevice && Settings.getInstance().getSearchImportPath())
					return ((IHistoDevice) device).getSupportedImportExtentions();
				else
					return new ArrayList<>();
			}

			@Override
			public Stream<Path> getSupplementFolders() {
				return Arrays.stream(Settings.getInstance().getImportFoldersCsv().split(GDE.STRING_CSV_SEPARATOR)) //
						.map(p -> Paths.get(p));
			}
		};

		private final static DataExplorer	application	= DataExplorer.getInstance();
		private final static IDevice			device			= DataExplorer.getInstance().getActiveDevice();

		/**
		 * @return the current directory path independent from object / device or null
		 */
		public abstract Path getBasePath();

		/**
		 * @return the sub path for finding files assigned to the device or null
		 */
		public abstract Path getActiveDeviceSubPath();

		/**
		 * @return the current directory path which depends on the object and the device or null
		 */
		public abstract Path getDataSetPath();

		/**
		 * @return the supported file extensions or an empty list
		 */
		public abstract List<String> getDataSetExtensions();

		public abstract Stream<Path> getSupplementFolders();

		/**
		 * Special directory handling for MC3000 and Q200 supporting battery sets but store data in normal device folder.
		 * @return the device name stripped by the 'set' extension for devices supporting battery sets
		 */
		private static String getPureDeviceName() {
			String pureDeviceName = DataExplorer.getInstance().getActiveDevice().getName();
			if (pureDeviceName.endsWith("-Set")) { // MC3000-Set -> MC3000, Q200-Set -> Q200 //$NON-NLS-1$
				pureDeviceName = pureDeviceName.substring(0, pureDeviceName.length() - 4);
			}
			return pureDeviceName;
		}
	}

	/**
	 * File types supported by the history.
	 */
	public static class SourceDataSet {
		private final IDevice			device	= DataExplorer.getInstance().getActiveDevice();

		private final Path				path;
		private final DataSetType	dataSetType;

		/** Use {@code getFile} only */
		private File							file;

		public enum DataSetType {
			OSD {
				@Override
				List<VaultCollector> getTrusses(TreeMap<String, DeviceConfiguration> deviceConfigurations, SourceDataSet dataFile) throws IOException,
						NotSupportedFileFormatException {
					List<VaultCollector> trusses = new ArrayList<>();
					File actualFile = dataFile.getActualFile();
					if (actualFile != null) {
						String linkPath = !dataFile.getFile().equals(actualFile) ? dataFile.getFile().getAbsolutePath() : GDE.STRING_EMPTY;
						String objectDirectory = dataFile.getObjectKey(deviceConfigurations);
						for (VaultCollector truss : HistoOsdReaderWriter.readTrusses(actualFile, objectDirectory)) {
							truss.getVault().setLogLinkPath(linkPath);
							truss.setSourceDataSet(dataFile);
							if (isValidObject(truss)) trusses.add(truss);
						}
					}
					return trusses;
				}

				public boolean isValidObject(VaultCollector truss) {
					return truss.isConsistentDevice() && truss.isConsistentChannel(channelMixConfigNumbers) && truss.isConsistentStartTimeStamp(minStartTimeStamp_ms) && truss.isConsistentObjectKey(isValidatedObjectKey);
				}

				@Override
				void readVaults(Path filePath, List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException, DataInconsitsentException,
						DataTypeException {
					HistoOsdReaderWriter.readVaults(filePath, trusses);
				}

				@Override
				public boolean providesReaderSettings() {
					return false;
				}
			},
			BIN {
				@Override
				List<VaultCollector> getTrusses(TreeMap<String, DeviceConfiguration> deviceConfigurations, SourceDataSet dataFile) throws IOException,
						NotSupportedFileFormatException {
					String objectDirectory = dataFile.getObjectKey(deviceConfigurations);
					String recordSetBaseName = DataExplorer.getInstance().getActiveChannel().getChannelConfigKey() + getRecordSetExtend(dataFile.getFile().getName());
					VaultCollector truss = new VaultCollector(objectDirectory, dataFile.getFile(), 0, Channels.getInstance().size(), recordSetBaseName,
							providesReaderSettings());
					truss.setSourceDataSet(dataFile);
					if (isValidObject(truss))
						return new ArrayList<>(Arrays.asList(truss));
					else
						return new ArrayList<>();
				}

				public boolean isValidObject(VaultCollector truss) {
					return truss.isConsistentStartTimeStamp(minStartTimeStamp_ms) && truss.isConsistentObjectKey(isValidatedObjectKey);
				}

				@Override
				void readVaults(Path filePath, List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException, DataInconsitsentException,
						DataTypeException {
					((IHistoDevice) DataExplorer.application.getActiveDevice()).getRecordSetFromImportFile(filePath, trusses);
				}

				@Override
				public boolean providesReaderSettings() {
					return true;
				}
			},
			LOG { // was not merged with bin - we expect differences in the future
				@Override
				List<VaultCollector> getTrusses(TreeMap<String, DeviceConfiguration> deviceConfigurations, SourceDataSet dataFile) throws IOException,
						NotSupportedFileFormatException {
					return BIN.getTrusses(deviceConfigurations, dataFile);
				}

				@Override
				void readVaults(Path filePath, List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException, DataInconsitsentException,
						DataTypeException {
					BIN.readVaults(filePath, trusses);
				}

				@Override
				public boolean providesReaderSettings() {
					return true;
				}
			};

			/**
			 * Determine which trusses are requested from the source file.
			 * @param deviceConfigurations helps to determine if the source file resides in an object directory
			 * @param dataFile is the source file
			 * @return the vault skeletons delivered by the source file based on the current device (and channel and object in case of bin files)
			 */
			abstract List<VaultCollector> getTrusses(TreeMap<String, DeviceConfiguration> deviceConfigurations, SourceDataSet dataFile) throws IOException,
					NotSupportedFileFormatException;

			/**
			 * Promote trusses into vaults by reading the source file.
			 * @param dataFile
			 * @param trusses lists the requested vaults
			 */
			abstract void readVaults(Path dataFile, List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException,
					DataInconsitsentException, DataTypeException;

			/**
			 * @returns true is a file reader capable to deliver different measurement values based on device settings
			 */
			public abstract boolean providesReaderSettings();

			/**
			 * @param extension of the file w/o dot
			 * @return the supported histo data set type or null
			 */
			static DataSetType getFromString(String extension) {
				if (extension.equals(DataSetType.OSD.getFileExtension()))
					return DataSetType.OSD;
				else if (extension.equals(DataSetType.BIN.getFileExtension()))
					return DataSetType.BIN;
				else if (extension.equals(DataSetType.LOG.getFileExtension()))
					return DataSetType.LOG;
				else
					return null;
			}

			/**
			 * Compose the record set extend to give capability to identify source of this record set
			 * @param fileName
			 * @return
			 */
			private static String getRecordSetExtend(String fileName) {
				String recordSetNameExtend = GDE.STRING_EMPTY;
				if (fileName.contains(GDE.STRING_UNDER_BAR)) {
					try {
						Integer.parseInt(fileName.substring(0, fileName.lastIndexOf(GDE.STRING_UNDER_BAR)));
						recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.lastIndexOf(GDE.STRING_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
					} catch (Exception e) {
						if (fileName.substring(0, fileName.lastIndexOf(GDE.STRING_UNDER_BAR)).length() <= 8)
							recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.lastIndexOf(GDE.STRING_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
					}
				} else {
					try {
						Integer.parseInt(fileName.substring(0, 4));
						recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, 4) + GDE.STRING_RIGHT_BRACKET;
					} catch (Exception e) {
						if (fileName.substring(0, fileName.length()).length() <= 8 + 4)
							recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.length() - 4) + GDE.STRING_RIGHT_BRACKET;
					}
				}
				return recordSetNameExtend;
			}

			/**
			 * @return the file extension w/o dot
			 */
			String getFileExtension() {
				return toString().toLowerCase();
			}
		}

		public SourceDataSet(File file) {
			this.file = file;
			this.path = file.toPath();
			this.dataSetType = DataSetType.getFromString(getExtension(this.path));
		}

		/**
		 * Determining the path is less costly than making a file from a path.
		 */
		public SourceDataSet(Path absolutePath) {
			this.path = absolutePath;
			this.dataSetType = DataSetType.getFromString(getExtension(this.path));
		}

		File getFile() {
			if (file == null) file = path.toFile();
			return file;
		}

		public static boolean isWorkableFile(Path path, List<String> extensions) {
			return extensions.contains(getExtension(path));
		}

		private static String getExtension(Path path) {
			String fileName = path.getFileName().toString();
			String extension = fileName.toString().substring(fileName.lastIndexOf('.') + 1).toLowerCase();
			if (extension.equals(fileName)) extension = GDE.STRING_EMPTY;
			return extension;
		}

		/**
		 * Adds support for link files pointing to data files.
		 * Deletes a link file if the data file does not exist.
		 * @return the data file (differs from {@code file} in case of a link file)
		 */
		private File getActualFile() throws IOException {
			long startMillis = System.currentTimeMillis();
			File actualFile = new File(OperatingSystemHelper.getLinkContainedFilePath(path.toString()));
			// getLinkContainedFilePath may have long response times in case of an unavailable network resources
			// This is a workaround: Much better solution would be a function 'getLinkContainedFilePathWithoutAccessingTheLinkedFile'
			if (getFile().equals(actualFile) && (System.currentTimeMillis() - startMillis > 555) || !getFile().exists()) {
				log.warning(() -> String.format("Dead OSD link %s pointing to %s", getFile(), actualFile)); //$NON-NLS-1$
				if (!getFile().delete()) {
					log.warning(() -> String.format("could not delete link file ", getFile())); //$NON-NLS-1$
				}
				return null;
			}
			return actualFile;
		}

		/**
		 * @param deviceConfigurations
		 * @return the file directory name if it is not a device directory or an empty string
		 */
		private String getObjectKey(TreeMap<String, DeviceConfiguration> deviceConfigurations) {
			String dirName = path.getParent().getFileName().toString();
			return !deviceConfigurations.containsKey(dirName) ? dirName : GDE.STRING_EMPTY;
		}

		List<VaultCollector> getTrusses(TreeMap<String, DeviceConfiguration> deviceConfigurations) throws IOException, NotSupportedFileFormatException {
			return dataSetType.getTrusses(deviceConfigurations, this);
		}

		public DataSetType getDataSetType() {
			return this.dataSetType;
		}

		public void readVaults(List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException, DataInconsitsentException,
				DataTypeException {
			dataSetType.readVaults(path, trusses);
		}

		/**
		 * @param desiredRecordSetName is a valid recordsetName or empty
		 * @return true if the file was opened or imported
		 */
		public boolean load(String desiredRecordSetName) {
			if (!FileUtils.checkFileExist(path.toString())) return false;

			if (path.toString().endsWith(GDE.FILE_ENDING_DOT_OSD)) {
				new FileHandler().openOsdFile(path.toString(), desiredRecordSetName);
				return true;
			} else if (device instanceof IHistoDevice) {
				List<String> validatedImportExtentions = ((IHistoDevice) device).getSupportedImportExtentions();
				if (!validatedImportExtentions.isEmpty() && SourceDataSet.isWorkableFile(path, validatedImportExtentions)) {
					((IHistoDevice) device).importDeviceData(path);
					return true;
				}
			}
			return false;
		}

		public Path getPath() {
			return this.path;
		}

		@Override
		public String toString() {
			return this.path.toString();
		}
	}

	/**
	 * Re- initializes the class.
	 */
	public synchronized void initialize() {
		this.validatedDevice = null;
		this.validatedChannel = null;
		this.validatedFolders = null;

		this.validatedImportExtentions.clear();
	}

	/**
	 * return the sourceFiles for osd reader (possibly link files) or for import
	 */
	public List<SourceDataSet> readSourceFiles4Test(Path filePath) throws IOException, NotSupportedFileFormatException {
		this.initialize();

		validatedFolders.defineDirectories4Test(filePath);
		validatedFolders.values().parallelStream().flatMap(List::stream).forEach(p -> FileUtils.checkDirectoryAndCreate(p.toString()));

		return readSourceFiles();
	}

	/**
	 * Determine file paths from an input directory and an import directory which fit to the objectKey, the device, the channel and the file
	 * extensions.
	 * @param rebuildStep defines which steps during histo data collection are skipped
	 * @return true if the list of file paths has already been valid
	 * @throws NotSupportedFileFormatException
	 * @throws IOException
	 */
	public synchronized boolean validateHistoFilePaths(RebuildStep rebuildStep) throws IOException, NotSupportedFileFormatException {
		IDevice lastDevice = this.validatedDevice;
		Channel lastChannel = this.validatedChannel;
		SourceFolders lastFolders = this.validatedFolders;
		List<String> lastImportExtentions = this.validatedImportExtentions;

		{ // determine validated values
			this.validatedDevice = this.application.getActiveDevice();
			this.validatedChannel = this.application.getActiveChannel();

			this.validatedDirectoryTypes.clear();
			this.validatedDirectoryTypes.add(DirectoryType.DATA);
			this.validatedImportExtentions = DirectoryType.IMPORT.getDataSetExtensions();
			Path importPath = DirectoryType.IMPORT.getDataSetPath();
			if (!this.validatedImportExtentions.isEmpty() && importPath != null) {
				this.validatedDirectoryTypes.add(DirectoryType.IMPORT);
			}
			this.validatedFolders = new SourceFolders();
			this.validatedFolders.defineDirectories(validatedDirectoryTypes); // *
		}

		boolean isFirstCall = lastDevice == null;
		boolean isChange = rebuildStep == RebuildStep.A_HISTOSET || isFirstCall;
		isChange = isChange || (lastDevice != null ? !lastDevice.getName().equals(this.validatedDevice.getName()) : this.validatedDevice != null);
		isChange = isChange || (lastChannel != null ? !lastChannel.getChannelConfigKey().equals(this.validatedChannel.getChannelConfigKey())
				: this.validatedChannel != null);

		isChange = isChange || (lastFolders != null ? !lastFolders.equals(this.validatedFolders) : true);
		log.log(OFF, "isChange=", isChange + "  lastFolders.equals=" + (lastFolders != null ? !lastFolders.equals(this.validatedFolders) : true));

		isChange = isChange || !lastImportExtentions.containsAll(this.validatedImportExtentions) || !this.validatedImportExtentions.containsAll(lastImportExtentions);
		log.log(FINE, "isChange=", isChange); //$NON-NLS-1$

		return !isChange;
	}

	/**
	 * Use file name extension lists and ignore file lists to determine the files required for the data access.
	 * @return the log files from all validated directories matching the validated extensions
	 */
	public List<SourceDataSet> readSourceFiles() {
		channelMixConfigNumbers = this.application.getActiveDevice().getDeviceConfiguration().getChannelMixConfigNumbers();
		minStartTimeStamp_ms = LocalDate.now().minusMonths(this.settings.getRetrospectMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
		isValidatedObjectKey = this.settings.getValidatedObjectKey(this.application.getObjectKey()).isPresent();

		List<SourceDataSet> result = new ArrayList<>();
		nonWorkableCount.reset();
		excludedFiles.clear();

		for (Entry<DirectoryType, List<Path>> entry : this.validatedFolders.entrySet()) {
			List<SourceDataSet> sourceDataSets = entry.getValue().stream() //
					.map(p -> {
						try {
							return getFileListing(p.toFile(), this.settings.getSubDirectoryLevelMax(), entry.getKey().getDataSetExtensions());
						} catch (FileNotFoundException e) {
							log.log(Level.SEVERE, e.getMessage(), e);
							return new ArrayList<SourceDataSet>();
						}
					}) //
					.flatMap(List::stream).collect(Collectors.toList());
			result.addAll(sourceDataSets);
		}
		return result;
	}

	/**
	 * Recursively walk a directory tree.
	 * @param rootDirectory
	 * @param recursionDepth specifies the depth of recursion cycles (0 means no recursion)
	 * @param extensions contains lowercase file extensions without dot
	 * @return the unsorted files matching the {@code extensions} and not discarded as per exclusion list
	 */
	private List<SourceDataSet> getFileListing(File rootDirectory, int recursionDepth, List<String> extensions) throws FileNotFoundException {
		List<SourceDataSet> result = new ArrayList<>();
		if (rootDirectory.isDirectory() && rootDirectory.canRead()) {
			File[] filesAndDirs = rootDirectory.listFiles();
			if (filesAndDirs != null) {
				for (File file : Arrays.asList(filesAndDirs)) {
					if (file.isFile()) {
						if (SourceDataSet.isWorkableFile(file.toPath(), extensions)) {
							if (!ExclusionData.isExcluded(file.toPath())) {
								SourceDataSet originFile = new SourceDataSet(file);
								result.add(originFile);
							} else {
								excludedFiles.add(file.toPath());
								log.log(INFO, "file is excluded              ", file);
							}
						} else {
							nonWorkableCount.increment();
							log.log(INFO, "file is not workable          ", file);
						}
					} else if (recursionDepth > 0) { // recursive walk by calling itself
						List<SourceDataSet> deeperList = getFileListing(file, recursionDepth - 1, extensions);
						result.addAll(deeperList);
					}
				}
			}
		}
		return result;
	}

	public int getValidatedFoldersCount() {
		if (validatedFolders != null) {
				return (int) validatedFolders.values().parallelStream().flatMap(Collection::parallelStream).count();
		} else {
			return 0;
		}
	}

	public Map<DirectoryType, Path> getValidatedDirectories() { // todo find solution for many paths
		Map<DirectoryType, Path> truncatedResult = new EnumMap<>(DirectoryType.class);
		if (validatedFolders != null) {
			for (Entry<DirectoryType, List<Path>> e : validatedFolders.entrySet()) {
				truncatedResult.put(e.getKey(), e.getValue().get(0));
			}
		}
		return truncatedResult;
	}

	/**
	 * @return the number of files which have been discarded during the last read operation due to an invalid file format
	 */
	public int getNonWorkableCount() {
		return nonWorkableCount.intValue();
	}

	/**
	 * @return the files which have been discarded during the last read operation based on the exclusion lists
	 */
	public List<Path> getExcludedFiles() {
		return excludedFiles;
	}
}
