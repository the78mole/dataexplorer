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

package gde.histo.datasources;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.sun.istack.internal.Nullable;

import gde.GDE;
import gde.config.DeviceConfigurations;
import gde.config.Settings;
import gde.data.Channels;
import gde.data.ObjectData;
import gde.device.IDevice;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.NotSupportedFileFormatException;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.VaultCollector;
import gde.histo.datasources.DirectoryScanner.DirectoryType;
import gde.histo.device.IHistoDevice;
import gde.histo.exclusions.ExclusionData;
import gde.histo.io.HistoOsdReaderWriter;
import gde.io.FileHandler;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.OperatingSystemHelper;

/**
 * Read the log file listings from the source folders list.
 * Avoid rebuilding if a choice of basic criteria did not change.
 * @author Thomas Eickert (USER)
 */
public class SourceDataSetExplorer {
	private static final String	$CLASS_NAME	= SourceDataSetExplorer.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * Holds the selection criteria for trusses (vault skeletons) applicable prior to reading the full vault.
	 */
	public static final class TrussCriteria {
		final List<Integer>	channelMixConfigNumbers	= DataExplorer.getInstance().getActiveDevice().getDeviceConfiguration().getChannelMixConfigNumbers();
		final long					minStartTimeStamp_ms		= LocalDate.now().minusMonths(Settings.getInstance().getRetrospectMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
		// use criteria independent from UI for JUnit (replace isObjectoriented)
		final ObjectData		activeObject						= DataExplorer.getInstance().getActiveObject();
		final boolean				filesWithOtherObject		= Settings.getInstance().getFilesWithOtherObject();
		final List<String>	realObjectKeys					= Settings.getInstance().getRealObjectKeys().stream().collect(Collectors.toList());

		@Override
		public String toString() {
			return "TrussCriteria [channelMixConfigNumbers=" + this.channelMixConfigNumbers + ", minStartTimeStamp_ms=" + this.minStartTimeStamp_ms + ", filesWithOtherObject=" + this.filesWithOtherObject + ", realObjectKeys=" + this.realObjectKeys + "]";
		}
	}

	/**
	 * File types supported by the history.
	 */
	public static final class SourceDataSet {
		private final Path				path;
		private final DataSetType	dataSetType;

		/** Use {@code getFile} only; is a link file or a real source file */
		private File							file;
		/** Is the real source file because {@source file} might be link file */
		private File							actualFile;

		public enum DataSetType {
			OSD {
				@Override
				public List<VaultCollector> getTrusses(SourceDataSet sourceDataSet) throws IOException, NotSupportedFileFormatException {
					List<VaultCollector> trusses = new ArrayList<>();
					File actualFile = sourceDataSet.getActualFile();
					if (actualFile != null) {
						String objectDirectory = sourceDataSet.getObjectKey();
						try {
							trusses = HistoOsdReaderWriter.readTrusses(actualFile, objectDirectory);
						} catch (FileNotFoundException e) {
							// link file points to non existent file
							log.log(Level.SEVERE, e.getMessage(), e);
						}
						for (VaultCollector truss : trusses) {
							truss.getVault().setLoadLinkPath(Paths.get(sourceDataSet.getLinkPath()));
							truss.getVault().setLogLinkPath(sourceDataSet.getLinkPath());
							truss.setSourceDataSet(sourceDataSet);
						}
					}
					return trusses;
				}

				@Override
				public boolean isValidDeviceChannelObjectAndStart(TrussCriteria trussCriteria, VaultCollector truss) {
					IDevice device = DataExplorer.getInstance().getActiveDevice();
					ExtendedVault vault = truss.getVault();
					if (device != null && !vault.getLogDeviceName().equals(device.getName()) //
					// HoTTViewer V3 -> HoTTViewerAdapter
							&& !(vault.getLogDeviceName().startsWith("HoTTViewer") && device.getName().equals("HoTTViewer"))) {
						log.info(() -> String.format("no match device   %,7d kiB %s", vault.getLoadFileAsPath().toFile().length() / 1024, vault.getLoadFileAsPath().toString()));
						return false;
					}
					if (!trussCriteria.channelMixConfigNumbers.contains(vault.getLogChannelNumber())) {
						log.info(() -> String.format("no match channel%2d%,7d kiB %s", vault.getLogChannelNumber(), vault.getLoadFileAsPath().toFile().length() / 1024, vault.getLoadFileAsPath().toString()));
						return false;
					}
					if (vault.getLogStartTimestamp_ms() < trussCriteria.minStartTimeStamp_ms) {
						log.info(() -> String.format("no match startTime%,7d kiB %s", vault.getLoadFileAsPath().toFile().length() / 1024, vault.getLoadFileAsPath().toString()));
						return false;
					}

					if (trussCriteria.activeObject == null) {
						if (!vault.getLogObjectKey().isEmpty()) return true;
						// no object in the osd file is a undesirable case but the log is accepted in order to show all logs
						log.info(() -> String.format("objectKey=%8s%,7d kiB %s", "empty", vault.getLoadFileAsPath().toFile().length() / 1024, vault.getLoadFileAsPath().toString()));
					} else {
						if (vault.getLogObjectKey().isEmpty()) {
							if (trussCriteria.filesWithOtherObject) return true;
							log.info(() -> String.format("objectKey:%8s%,7d kiB %s", "empty", vault.getLoadFileAsPath().toFile().length() / 1024, vault.getLoadFileAsPath().toString()));
							return false;
						}
						boolean matchingObjectKey = trussCriteria.realObjectKeys.stream().anyMatch(s -> s.equalsIgnoreCase(vault.getLogObjectKey().trim()));
						if (!matchingObjectKey) {
							if (trussCriteria.filesWithOtherObject) return true;
							boolean matchingObjectDirectory = trussCriteria.realObjectKeys.stream().anyMatch(s -> s.equalsIgnoreCase(vault.getLoadObjectDirectory()));
							if (matchingObjectDirectory) return true;
							log.info(() -> String.format("objectKey=%8s%,7d kiB %s", vault.getRectifiedObjectKey(), vault.getLoadFileAsPath().toFile().length() / 1024, vault.getLoadFileAsPath().toString()));
							return false;
						}
						boolean consistentObjectKey = vault.getLogObjectKey().equalsIgnoreCase(trussCriteria.activeObject.getKey());
						if (!consistentObjectKey) {
							// finally we found a log with a validated object key which does not correspond to the desired one
							return false;
						}
					}
					return true;
				}

				@Override
				public void readVaults(Path filePath, List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException,
						DataInconsitsentException, DataTypeException {
					HistoOsdReaderWriter.readVaults(filePath, trusses);
				}

				@Override
				public boolean providesReaderSettings() {
					return false;
				}

				@Override
				@Nullable
				public File getActualFile(File file) throws IOException {
					long startMillis = System.currentTimeMillis();
					File actualFile = new File(OperatingSystemHelper.getLinkContainedFilePath(file.toPath().toString()));
					// getLinkContainedFilePath may have long response times in case of an unavailable network resources
					// This is a workaround: Much better solution would be a function 'getLinkContainedFilePathWithoutAccessingTheLinkedFile'
					log.log(Level.FINER, "time_ms=", System.currentTimeMillis() - startMillis);
					if (file.equals(actualFile) && (System.currentTimeMillis() - startMillis > 555) || !file.exists()) {
						log.warning(() -> String.format("Dead OSD link %s pointing to %s", file, actualFile)); //$NON-NLS-1$
						if (!file.delete()) {
							log.warning(() -> String.format("could not delete link file ", file)); //$NON-NLS-1$
						}
						return null;
					}
					return actualFile;
				}

				@Override
				public boolean isLoadable() {
					return true;
				}

				@Override
				public void loadFile(Path path, String desiredRecordSetName) {
					new FileHandler().openOsdFile(path.toString(), desiredRecordSetName);
				}
			},
			BIN {
				@Override
				public List<VaultCollector> getTrusses(SourceDataSet sourceDataSet) throws IOException, NotSupportedFileFormatException {
					String objectDirectory = sourceDataSet.getObjectKey();
					String recordSetBaseName = DataExplorer.getInstance().getActiveChannel().getChannelConfigKey() + getRecordSetExtend(sourceDataSet.getFile().getName());
					VaultCollector truss = new VaultCollector(objectDirectory, sourceDataSet.getFile(), 0, Channels.getInstance().size(), recordSetBaseName,
							providesReaderSettings());
					truss.setSourceDataSet(sourceDataSet);
					return new ArrayList<>(Arrays.asList(truss));
				}

				@Override
				public boolean isValidDeviceChannelObjectAndStart(TrussCriteria trussCriteria, VaultCollector truss) {
					ExtendedVault vault = truss.getVault();
					if (vault.getLogStartTimestamp_ms() < trussCriteria.minStartTimeStamp_ms) {
						log.info(() -> String.format("no match startTime%,7d kiB %s", vault.getLoadFileAsPath().toFile().length() / 1024, vault.getLoadFileAsPath().toString()));
						return false;
					}
					if (trussCriteria.activeObject != null) {
						boolean consistentObjectKey = vault.getLogObjectKey().equalsIgnoreCase(trussCriteria.activeObject.getKey());
						if (!consistentObjectKey) {
							if (trussCriteria.filesWithOtherObject) return true;
							log.info(() -> String.format("objectKey=%8s%,7d kiB %s", vault.getRectifiedObjectKey(), vault.getLoadFileAsPath().toFile().length() / 1024, vault.getLoadFileAsPath().toString()));
							return false;
						}
					}
					return true;
				}

				@Override
				public void readVaults(Path filePath, List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException,
						DataInconsitsentException, DataTypeException {
					((IHistoDevice) DataExplorer.application.getActiveDevice()).getRecordSetFromImportFile(filePath, trusses);
				}

				@Override
				public boolean providesReaderSettings() {
					return true;
				}

				@Override
				public File getActualFile(File file) throws IOException {
					return file;
				}

				@Override
				public boolean isLoadable() {
					if (DataExplorer.getInstance().getActiveDevice() instanceof IHistoDevice) {
						List<String> importExtentions = ((IHistoDevice) DataExplorer.getInstance().getActiveDevice()).getSupportedImportExtentions();
						return importExtentions.contains(getFileExtension());
					}
					return false;
				}

				@Override
				public void loadFile(Path path, String desiredRecordSetName) {
					((IHistoDevice) DataExplorer.getInstance().getActiveDevice()).importDeviceData(path);
				}
			},
			LOG { // was not merged with bin - we expect differences in the future
				@Override
				public List<VaultCollector> getTrusses(SourceDataSet sourceDataSet) throws IOException, NotSupportedFileFormatException {
					return BIN.getTrusses(sourceDataSet);
				}

				@Override
				public boolean isValidDeviceChannelObjectAndStart(TrussCriteria trussCriteria, VaultCollector truss) {
					return BIN.isValidDeviceChannelObjectAndStart(trussCriteria, truss);
				}

				@Override
				public void readVaults(Path filePath, List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException,
						DataInconsitsentException, DataTypeException {
					BIN.readVaults(filePath, trusses);
				}

				@Override
				public boolean providesReaderSettings() {
					return true;
				}

				@Override
				public File getActualFile(File file) throws IOException {
					return file;
				}

				@Override
				public boolean isLoadable() {
					return BIN.isLoadable();
				}

				@Override
				public void loadFile(Path path, String desiredRecordSetName) {
					BIN.loadFile(path, desiredRecordSetName);
				}
			};

			/**
			 * @return the trusses delivered by the source file
			 */
			public abstract List<VaultCollector> getTrusses(SourceDataSet sourceDataSet) throws IOException, NotSupportedFileFormatException;

			/**
			 * @return true if the truss complies with the current device, object and start timestamp
			 */
			public abstract boolean isValidDeviceChannelObjectAndStart(TrussCriteria trussCriteria, VaultCollector truss);

			/**
			 * Promote trusses into vaults by reading the source file.
			 * @param dataFile
			 * @param trusses lists the requested vaults
			 */
			public abstract void readVaults(Path dataFile, List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException,
					DataInconsitsentException, DataTypeException;

			/**
			 * @returns true is a file reader capable to deliver different measurement values based on device settings
			 */
			public abstract boolean providesReaderSettings();

			/**
			 * Adds support for link files pointing to data files.
			 * Deletes a link file if the data file does not exist.
			 * @return the data file (differs from {@code file} in case of a link file)
			 */
			public abstract File getActualFile(File file) throws IOException;

			/**
			 * @return true if the device supports loading the file and if settings do not prohibit loading
			 */
			public abstract boolean isLoadable();

			/**
			 * @param desiredRecordSetName
			 */
			public abstract void loadFile(Path path, String desiredRecordSetName);

			/**
			 * @param extension of the file w/o dot
			 * @return the supported histo data set type
			 */
			@Nullable
			public static DataSetType getFromString(String extension) {
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
			public String getFileExtension() {
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

		public File getFile() {
			if (file == null) file = path.toFile();
			return file;
		}

		/**
		 * @return true if the current device supports both directory type and file extension
		 */
		public static boolean isWorkableFile(Path filePath, DirectoryType directoryType) {
			String extensionWithoutDot = SourceDataSet.getExtension(filePath);
			DataSetType type = DataSetType.getFromString(extensionWithoutDot);
			if (type == null) {
				log.log(Level.INFO, "unsupported data type         ", filePath);
				return false;
			}
			if (!type.isLoadable()) {
				log.log(Level.INFO, "not a loadable type           ", filePath);
				return false;
			}
			if (!directoryType.getDataSetExtensions().contains(type.getFileExtension())) {
				log.log(Level.INFO, "not a valid extension         ", filePath);
				return false;
			}
			return true;
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
		public File getActualFile() throws IOException {
			if (this.actualFile == null) this.actualFile = dataSetType.getActualFile(getFile());
			return this.actualFile;
		}

		/**
		 * @return the path to the link file if the actual file does not hold the data
		 */
		public String getLinkPath() {
			String linkPath = !getFile().equals(actualFile) ? getFile().getAbsolutePath() : GDE.STRING_EMPTY;
			return linkPath;
		}

		/**
		 * @param deviceConfigurations
		 * @return the file directory name if it is not a device directory or an empty string
		 */
		private String getObjectKey() {
			String dirName = path.getParent().getFileName().toString();
			DeviceConfigurations deviceConfigurations = DataExplorer.getInstance().getDeviceConfigurations();
			return !deviceConfigurations.contains(dirName) ? dirName : GDE.STRING_EMPTY;
		}

		/**
		 * @param trussCriteria
		 * @return the trusses
		 */
		public List<VaultCollector> defineSelectedTrusses(TrussCriteria trussCriteria, boolean isSlowFolderAccess) //
				throws IOException, NotSupportedFileFormatException {
			if (isSlowFolderAccess) DataExplorer.getInstance().setStatusMessage("get file properties    " + path.toString());
			List<VaultCollector> trusses = dataSetType.getTrusses(this);
			for (Iterator<VaultCollector> iterator = trusses.iterator(); iterator.hasNext();) {
				VaultCollector vaultCollector = iterator.next();
				if (!dataSetType.isValidDeviceChannelObjectAndStart(trussCriteria, vaultCollector)) {
					iterator.remove();
				}
			}
			if (isSlowFolderAccess) DataExplorer.getInstance().setStatusMessage("");
			return trusses;
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
			if (!dataSetType.isLoadable()) return false;

			dataSetType.loadFile(path, desiredRecordSetName);
			return true;
		}

		public Path getPath() {
			return this.path;
		}

		@Override
		public String toString() {
			return this.path.toString();
		}
	}

	private final DirectoryScanner		directoryScanner;

	private final LongAdder						nonWorkableCount	= new LongAdder();
	private final List<Path>					excludedFiles			= new ArrayList<>();
	private final List<SourceDataSet>	sourceDataSets		= new ArrayList<>();

	public SourceDataSetExplorer(DirectoryScanner directoryScanner) {
		this.directoryScanner = directoryScanner;
	}

	/**
	 * Use file name extension lists and ignore file lists to determine the files required for the data access.
	 * Read the log files from all validated directories matching the validated extensions.
	 */
	private void listFiles() {
		sourceDataSets.clear();
		nonWorkableCount.reset();
		excludedFiles.clear();

		for (Entry<DirectoryType, Set<Path>> entry : directoryScanner.getSourceFolders().entrySet()) {
			List<SourceDataSet> resultDataSets = entry.getValue().stream() //
					.map(p -> {
						try {
							return getFileListing(p.toFile(), Settings.getInstance().getSubDirectoryLevelMax(), entry.getKey());
						} catch (FileNotFoundException e) {
							log.log(Level.SEVERE, e.getMessage(), e);
							return new ArrayList<SourceDataSet>();
						}
					}) //
					.flatMap(List::stream).collect(Collectors.toList());
			sourceDataSets.addAll(resultDataSets);
		}
	}

	/**
	 * Recursively walk a directory tree.
	 * @param rootDirectory
	 * @param recursionDepth specifies the depth of recursion cycles (0 means no recursion)
	 * @param directoryType
	 * @return the unsorted files matching the {@code extensions} and not discarded as per exclusion list
	 */
	private List<SourceDataSet> getFileListing(File rootDirectory, int recursionDepth, DirectoryType directoryType) throws FileNotFoundException {
		if (directoryScanner.isSlowFolderAccess()) DataExplorer.getInstance().setStatusMessage("get file names     " + rootDirectory.toString());
		List<SourceDataSet> result = new ArrayList<>();
		if (rootDirectory.isDirectory() && rootDirectory.canRead()) {
			File[] filesAndDirs = rootDirectory.listFiles();
			if (filesAndDirs != null) {
				for (File file : Arrays.asList(filesAndDirs)) {
					if (file.isFile()) {
						if (SourceDataSet.isWorkableFile(file.toPath(), directoryType)) {
							if (!ExclusionData.isExcluded(file.toPath())) {
								SourceDataSet originFile = new SourceDataSet(file);
								result.add(originFile);
							} else {
								excludedFiles.add(file.toPath());
								log.log(INFO, "file is excluded              ", file);
							}
						} else {
							nonWorkableCount.increment();
						}
					} else if (recursionDepth > 0) { // recursive walk by calling itself
						List<SourceDataSet> deeperList = getFileListing(file, recursionDepth - 1, directoryType);
						result.addAll(deeperList);
					}
				}
			}
		}
		if (directoryScanner.isSlowFolderAccess()) DataExplorer.getInstance().setStatusMessage("");
		return result;
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

	/**
	 * @return the log files from all validated directories matching the validated extensions
	 */
	public List<SourceDataSet> listSourceFiles() {
		// a channel change without any additional criterion change can use the existent list of files for reading the trusses (performance)
		if (!directoryScanner.isChannelChangeOnly()) listFiles();

		return sourceDataSets;
	}

	/**
	 * return the sourceFiles for osd reader (possibly link files) or for import
	 */
	public List<SourceDataSet> readSourceFiles4Test(Path filePath) throws IOException, NotSupportedFileFormatException {
		directoryScanner.initialize();
		directoryScanner.build4Test(filePath);

		listFiles();
		return sourceDataSets;
	}

	/**
	 * @param sourceFiles for the osd reader (possibly link files) or for import
	 * @return the trusses in the osd files and in the native files (if supported by the device) which comply with the {@code trussCriteria)}
	 */
	public List<VaultCollector> defineSelectedTrusses(List<SourceDataSet> sourceFiles) throws IOException, NotSupportedFileFormatException {
		List<VaultCollector> result = new ArrayList<>();
		TrussCriteria trussCriteria = new TrussCriteria();
		for (SourceDataSet sourceDataSet : sourceFiles) {
			List<VaultCollector> trusses = sourceDataSet.defineSelectedTrusses(trussCriteria, directoryScanner.isSlowFolderAccess());
			result.addAll(trusses);
		}
		return result;
	}

}
