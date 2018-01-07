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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.LongAdder;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.NotSupportedFileFormatException;
import gde.histo.cache.ExtendedVault;
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
	private final static String							$CLASS_NAME								= DirectoryScanner.class.getName();
	private final static Logger							log												= Logger.getLogger($CLASS_NAME);

	private static List<Integer>						channelMixConfigNumbers;
	private static long											minStartTimeStamp_ms;
	private static boolean									isValidatedObjectKey;

	private final DataExplorer							application								= DataExplorer.getInstance();
	private final Settings									settings									= Settings.getInstance();

	private IDevice													validatedDevice						= null;
	private Channel													validatedChannel					= null;
	private List<String>										validatedImportExtentions	= new ArrayList<>();

	private final Map<DirectoryType, Path>	validatedDirectories			= new LinkedHashMap<>();
	private final LongAdder									nonWorkableCount					= new LongAdder();
	private final List<Path>								excludedFiles							= new ArrayList<>();

	/**
	 * Data sources supported by the history including osd link files.
	 */
	public enum DirectoryType {
		DATA {
			@Override
			public Path getDataSetPath() {
				String subPathData = DataExplorer.getInstance().getActiveObject() == null ? getPureDeviceName() : DataExplorer.getInstance().getObjectKey();
				return Paths.get(Settings.getInstance().getDataFilePath()).resolve(subPathData);
			}

			@Override
			public List<String> getDataSetExtensions() {
				List<String> result = new ArrayList<>();
				result.add(GDE.FILE_ENDING_OSD);
				if (DataExplorer.getInstance().getActiveDevice() instanceof IHistoDevice && Settings.getInstance().getSearchDataPathImports()) {
					result.addAll(((IHistoDevice) DataExplorer.getInstance().getActiveDevice()).getSupportedImportExtentions());
				}
				return result;
			}
		},
		IMPORT {
			@Override
			public Path getDataSetPath() {
				if (DataExplorer.getInstance().getActiveDevice() instanceof IHistoDevice && Settings.getInstance().getSearchImportPath()) {
					Path importDir = DataExplorer.getInstance().getActiveDevice().getDeviceConfiguration().getImportBaseDir();
					if (importDir == null) {
						return null;
					} else {
						String subPathImport = DataExplorer.getInstance().getActiveObject() == null ? GDE.STRING_EMPTY
								: DataExplorer.getInstance().getObjectKey();
						Path importPath = importDir.resolve(subPathImport);
						return !importPath.equals(DirectoryType.DATA.getDataSetPath()) ? importPath : null;
					}
				} else
					return null;
			}

			@Override
			public List<String> getDataSetExtensions() {
				if (DataExplorer.getInstance().getActiveDevice() instanceof IHistoDevice && Settings.getInstance().getSearchImportPath())
					return ((IHistoDevice) DataExplorer.getInstance().getActiveDevice()).getSupportedImportExtentions();
				else
					return new ArrayList<>();
			}
		};

		/**
		 * @return the current directory path which depends on the object and the device or null
		 */
		public abstract Path getDataSetPath();

		/**
		 * @return the supported file extensions or an empty list
		 */
		public abstract List<String> getDataSetExtensions();

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
		private final IDevice device = DataExplorer.getInstance().getActiveDevice();

		private final Path				path;
		private final DataSetType	dataSetType;

		/** Use {@code getFile} only */
		private File							file;

		private enum DataSetType {
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
				List<ExtendedVault> readVaults(Path filePath, List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException,
						DataInconsitsentException, DataTypeException {
					return HistoOsdReaderWriter.readVaults(filePath, trusses);
				}
			},
			BIN {
				@Override
				List<VaultCollector> getTrusses(TreeMap<String, DeviceConfiguration> deviceConfigurations, SourceDataSet dataFile) throws IOException,
						NotSupportedFileFormatException {
					String objectDirectory = dataFile.getObjectKey(deviceConfigurations);
					String recordSetBaseName = DataExplorer.getInstance().getActiveChannel().getChannelConfigKey() + getRecordSetExtend(dataFile.getFile().getName());
					VaultCollector truss = new VaultCollector(objectDirectory, dataFile.getFile(), 0, Channels.getInstance().size(), recordSetBaseName);
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
				List<ExtendedVault> readVaults(Path filePath, List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException,
						DataInconsitsentException, DataTypeException {
					return ((IHistoDevice) DataExplorer.application.getActiveDevice()).getRecordSetFromImportFile(filePath, trusses);
				}
			},
			LOG { // was not merged with bin - we expect differences in the future
				@Override
				List<VaultCollector> getTrusses(TreeMap<String, DeviceConfiguration> deviceConfigurations, SourceDataSet dataFile) throws IOException,
						NotSupportedFileFormatException {
					return BIN.getTrusses(deviceConfigurations, dataFile);
				}

				@Override
				List<ExtendedVault> readVaults(Path filePath, List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException,
						DataInconsitsentException, DataTypeException {
					return BIN.readVaults(filePath, trusses);
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
			 * @return the fully populated vaults
			 */
			abstract List<ExtendedVault> readVaults(Path dataFile, List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException,
					DataInconsitsentException, DataTypeException;

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

		public List<ExtendedVault> readVaults(List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException,
				DataInconsitsentException, DataTypeException {
			return dataSetType.readVaults(path, trusses);
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

		this.validatedImportExtentions.clear();
		this.validatedDirectories.clear();
	}

	/**
	 * return the sourceFiles for osd reader (possibly link files) or for import
	 */
	public List<SourceDataSet> readSourceFiles4Test(Path filePath) throws IOException, NotSupportedFileFormatException {
		this.initialize();

		if (filePath == null)
			this.validatedDirectories.put(DirectoryType.DATA, DirectoryType.DATA.getDataSetPath());
		else
			this.validatedDirectories.put(DirectoryType.DATA, filePath);

		Path dataDir = this.validatedDirectories.get(DirectoryType.DATA);
		FileUtils.checkDirectoryAndCreate(dataDir.toString());

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
		Path lastHistoDataDir = this.validatedDirectories.get(DirectoryType.DATA);
		Path lastHistoImportDir = this.validatedDirectories.get(DirectoryType.IMPORT);
		List<String> lastImportExtentions = this.validatedImportExtentions;

		{ // determine validated values
			this.validatedDevice = this.application.getActiveDevice();
			this.validatedChannel = this.application.getActiveChannel();

			this.validatedDirectories.clear();
			this.validatedDirectories.put(DirectoryType.DATA, DirectoryType.DATA.getDataSetPath());
			this.validatedImportExtentions = DirectoryType.IMPORT.getDataSetExtensions();
			Path importPath = DirectoryType.IMPORT.getDataSetPath();
			if (!this.validatedImportExtentions.isEmpty() && importPath != null) {
				this.validatedDirectories.put(DirectoryType.IMPORT, importPath);
			}
		}

		boolean isFirstCall = lastDevice == null;
		boolean isChange = rebuildStep == RebuildStep.A_HISTOSET || isFirstCall;
		isChange = isChange || (lastDevice != null ? !lastDevice.getName().equals(this.validatedDevice.getName()) : this.validatedDevice != null);
		isChange = isChange || (lastChannel != null ? !lastChannel.getChannelConfigKey().equals(this.validatedChannel.getChannelConfigKey())
				: this.validatedChannel != null);
		isChange = isChange || (lastHistoDataDir != null ? !lastHistoDataDir.equals(this.validatedDirectories.get(DirectoryType.DATA)) : true);
		isChange = isChange || (lastHistoImportDir != null ? !lastHistoImportDir.equals(this.validatedDirectories.get(DirectoryType.IMPORT))
				: this.validatedDirectories.containsKey(DirectoryType.IMPORT));
		isChange = isChange || !lastImportExtentions.containsAll(this.validatedImportExtentions) || !this.validatedImportExtentions.containsAll(lastImportExtentions);
		log.log(FINE, "isChange=", isChange); //$NON-NLS-1$

		return !isChange;
	}

	/**
	 * Use file name extension lists and ignore file lists to determine the files required for the data access.
	 * @return the log files from all validated directories matching the validated extensions
	 */
	public List<SourceDataSet> readSourceFiles() throws FileNotFoundException {
		channelMixConfigNumbers = this.application.getActiveDevice().getDeviceConfiguration().getChannelMixConfigNumbers();
		minStartTimeStamp_ms = LocalDate.now().minusMonths(this.settings.getRetrospectMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
		isValidatedObjectKey = this.settings.getValidatedObjectKey(this.application.getObjectKey()).isPresent();

		List<SourceDataSet> result = new ArrayList<>();
		nonWorkableCount.reset();
		excludedFiles.clear();

		for (Entry<DirectoryType, Path> entry : this.validatedDirectories.entrySet()) {
			Path dir = entry.getValue();
			if (FileUtils.checkDirectoryExist(dir.toString())) {
				result.addAll(getFileListing(dir.toFile(), this.settings.getSubDirectoryLevelMax(), entry.getKey().getDataSetExtensions()));
			} else {
				log.log(FINE, "histoDir not found in ", dir); //$NON-NLS-1$
			}
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

	public Map<DirectoryType, Path> getValidatedDirectories() {
		return this.validatedDirectories;
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
