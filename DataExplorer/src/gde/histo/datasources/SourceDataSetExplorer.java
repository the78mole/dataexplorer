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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.istack.internal.Nullable;

import gde.GDE;
import gde.config.DeviceConfigurations;
import gde.config.Settings;
import gde.data.Channels;
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
		final String				activeObjectKey					= Settings.getInstance().getActiveObjectKey();
		final boolean				filesWithOtherObject		= Settings.getInstance().getFilesWithOtherObject();
		final Set<String>		realObjectKeys					= Settings.getInstance().getRealObjectKeys().collect(Collectors.toSet());

		@Override
		public String toString() {
			return "TrussCriteria [channelMixConfigNumbers=" + this.channelMixConfigNumbers + ", minStartTimeStamp_ms=" + this.minStartTimeStamp_ms + ", filesWithOtherObject=" + this.filesWithOtherObject + ", realObjectKeys=" + this.realObjectKeys + "]";
		}
	}

	/**
	 * File types supported by the history.
	 */
	public static abstract class SourceDataSet {
		protected final Path	path;

		/** Use {@code getFile} only; is a link file or a real source file */
		private File					file;

		/**
		 * @return true if the current device supports both directory type and file extension
		 */
		public static boolean isWorkableFile(Path filePath, Set<DirectoryType> directoryTypes) {
			SourceDataSet sourceDataSet = createSourceDataSet(filePath);
			if (sourceDataSet == null) {
				log.log(Level.INFO, "unsupported data type         ", filePath);
				return false;
			}
			if (!sourceDataSet.isLoadable()) {
				log.log(Level.INFO, "not a loadable type           ", filePath);
				return false;
			}
			for (DirectoryType directoryType : directoryTypes) {
				if (directoryType.getDataSetExtensions().contains(getFileExtension(filePath))) {
					return true;
				}
			}
			log.log(Level.INFO, "not a valid extension         ", filePath);
			return false;
		}

		/**
		 * @return the lower case extension without dot
		 */
		public static String getFileExtension(Path path) {
			String fileName = path.getFileName().toString();
			String extension = fileName.toString().substring(fileName.lastIndexOf('.') + 1).toLowerCase();
			if (extension.equals(fileName)) extension = GDE.STRING_EMPTY;
			return extension;
		}

		public static SourceDataSet createSourceDataSet(File file) {
			return createSourceDataSet(file.toPath());
		}

		@Nullable
		public static SourceDataSet createSourceDataSet(Path absolutePath) {
			String extension = getFileExtension(absolutePath);
			if (extension.equals(GDE.FILE_ENDING_OSD))
				return new OsdDataSet(absolutePath);
			else if (DataExplorer.getInstance().getActiveDevice() instanceof IHistoDevice) {
				List<String> importExtentions = ((IHistoDevice) DataExplorer.getInstance().getActiveDevice()).getSupportedImportExtentions();
				if (importExtentions.contains(extension))
					return new ImportDataSet(absolutePath); // todo implement logDataSet and native HottLogReader
			}
			return null;
		}

		protected SourceDataSet(File file) {
			this.file = file;
			this.path = file.toPath();
		}

		/**
		 * Determining the path is less costly than making a file from a path.
		 */
		protected SourceDataSet(Path absolutePath) {
			this.path = absolutePath;
		}

		/**
		 * @return the trusses delivered by the source file
		 */
		public abstract List<VaultCollector> getTrusses();

		/**
		 * @return true if the truss complies with the current device, object and start timestamp
		 */
		public abstract boolean isValidDeviceChannelObjectAndStart(TrussCriteria trussCriteria, VaultCollector truss);

		/**
		 * @returns true is a file reader capable to deliver different measurement values based on device settings
		 */
		public abstract boolean providesReaderSettings();

		/**
		 * Adds support for link files pointing to data files.
		 * Deletes a link file if the data file does not exist.
		 * @return the data file (differs from {@code file} in case of a link file)
		 */
		public abstract File getActualFile() throws IOException;

		/**
		 * @return true if the device supports loading the file and if settings do not prohibit loading
		 */
		public abstract boolean isLoadable();

		/**
		 * @param desiredRecordSetName
		 */
		public abstract void loadFile(Path tmpPath, String desiredRecordSetName);

		public File getFile() {
			if (file == null) file = path.toFile();
			return file;
		}

		/**
		 * @return the path to the link file if the actual file does not hold the data
		 */
		public abstract String getLinkPath();

		/**
		 * @param deviceConfigurations
		 * @return the file directory name if it is not a device directory or an empty string
		 */
		protected String getObjectKey() {
			String dirName = path.getParent().getFileName().toString();
			DeviceConfigurations deviceConfigurations = DataExplorer.getInstance().getDeviceConfigurations();
			return !deviceConfigurations.contains(dirName) ? dirName : GDE.STRING_EMPTY;
		}

		/**
		 * @return the trusses or an empty stream
		 */
		public Stream<VaultCollector> defineTrusses(TrussCriteria trussCriteria, Consumer<String> signaler) {
			signaler.accept("get file properties    " + path.toString());
			return getTrusses().stream().filter(t -> isValidDeviceChannelObjectAndStart(trussCriteria, t));
		}

		/**
		 * Promote trusses into vaults by reading the source file.
		 * @param trusses lists the requested vaults
		 */
		public abstract void readVaults(Path filePath, List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException,
				DataInconsitsentException, DataTypeException;

		/**
		 * @param desiredRecordSetName is a valid recordsetName or empty
		 * @return true if the file was opened or imported
		 */
		public boolean load(String desiredRecordSetName) {
			if (!FileUtils.checkFileExist(path.toString())) return false;
			if (!isLoadable()) return false;

			loadFile(path, desiredRecordSetName);
			return true;
		}

		public Path getPath() {
			return this.path;
		}

		/**
		 * Compose the record set extend to give capability to identify source of this record set
		 * @return
		 */
		protected String getRecordSetExtend() {
			String fileName = path.getFileName().toString();
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

		@Override
		public String toString() {
			return this.path.toString();
		}
	}

	public static final class OsdDataSet extends SourceDataSet {
		@SuppressWarnings("hiding")
		private static final String	$CLASS_NAME						= OsdDataSet.class.getName();
		@SuppressWarnings("hiding")
		private static final Logger	log										= Logger.getLogger($CLASS_NAME);

		/** Is the real source file because {@source file} might be link file */
		protected File							actualFile						= null;
		private boolean							isActualFileVerified	= false;

		protected OsdDataSet(File file) {
			super(file);
		}

		protected OsdDataSet(Path absolutePath) {
			super(absolutePath);
		}

		@Override
		public List<VaultCollector> getTrusses() {
			List<VaultCollector> trusses = new ArrayList<>();
			if (getActualFile() != null) {
				String objectDirectory = getObjectKey();
				try {
					trusses = HistoOsdReaderWriter.readTrusses(getActualFile(), objectDirectory);
				} catch (Exception e) {
					// link file points to non existent file
					log.log(Level.SEVERE, e.getMessage(), e);
				}
				for (VaultCollector truss : trusses) {
					truss.getVault().setLoadLinkPath(Paths.get(getLinkPath()));
					truss.getVault().setLogLinkPath(getLinkPath());
					truss.setSourceDataSet(this);
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

			if (trussCriteria.activeObjectKey.isEmpty()) {
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
				boolean consistentObjectKey = vault.getLogObjectKey().equalsIgnoreCase(trussCriteria.activeObjectKey);
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
		public File getActualFile() {
			if (actualFile == null && !isActualFileVerified) {
				setActualFile();
			}
			return actualFile;
		}

		protected void setActualFile() {
			long startMillis = System.currentTimeMillis();
			try {
				actualFile = new File(OperatingSystemHelper.getLinkContainedFilePath(getFile().toPath().toString()));
				// getLinkContainedFilePath may have long response times in case of an unavailable network resources
				// This is a workaround: Much better solution would be a function 'getLinkContainedFilePathWithoutAccessingTheLinkedFile'
				log.log(Level.FINER, "time_ms=", System.currentTimeMillis() - startMillis);
				if (getFile().equals(actualFile) && (System.currentTimeMillis() - startMillis > 555) || !getFile().exists()) {
					log.warning(() -> String.format("Dead OSD link %s pointing to %s", getFile(), actualFile));
					if (!getFile().delete()) {
						log.warning(() -> String.format("could not delete link file ", getFile()));
					}
					actualFile = null;
				}
			} catch (Exception e) {
				log.log(Level.SEVERE, e.getMessage(), e);
				actualFile = null;
			}
			isActualFileVerified = true;
		}

		/**
		 * @return the path to the link file if the actual file does not hold the data
		 */
		@Override
		public String getLinkPath() {
			String linkPath = !getFile().equals(getActualFile()) ? getFile().getAbsolutePath() : GDE.STRING_EMPTY;
			return linkPath;
		}

		@Override
		public boolean isLoadable() {
			return true;
		}

		@Override
		public void loadFile(Path tmpPath, String desiredRecordSetName) {
			new FileHandler().openOsdFile(tmpPath.toString(), desiredRecordSetName);
		}

	}

	public static final class ImportDataSet extends SourceDataSet {
		@SuppressWarnings("hiding")
		private static final String	$CLASS_NAME	= ImportDataSet.class.getName();
		@SuppressWarnings("hiding")
		private static final Logger	log					= Logger.getLogger($CLASS_NAME);

		protected ImportDataSet(File file) {
			super(file);
		}

		protected ImportDataSet(Path absolutePath) {
			super(absolutePath);
		}

		@Override
		public List<VaultCollector> getTrusses() {
			String objectDirectory = getObjectKey();
			String recordSetBaseName = DataExplorer.getInstance().getActiveChannel().getChannelConfigKey() + getRecordSetExtend();
			VaultCollector truss = new VaultCollector(objectDirectory, getFile(), 0, Channels.getInstance().size(), recordSetBaseName,
					providesReaderSettings());
			truss.setSourceDataSet(this);
			return new ArrayList<>(Arrays.asList(truss));
		}

		@Override
		public boolean isValidDeviceChannelObjectAndStart(TrussCriteria trussCriteria, VaultCollector truss) {
			ExtendedVault vault = truss.getVault();
			if (vault.getLogStartTimestamp_ms() < trussCriteria.minStartTimeStamp_ms) {
				log.info(() -> String.format("no match startTime%,7d kiB %s", vault.getLoadFileAsPath().toFile().length() / 1024, vault.getLoadFileAsPath().toString()));
				return false;
			}
			if (!trussCriteria.activeObjectKey.isEmpty()) {
				boolean consistentObjectKey = vault.getLogObjectKey().equalsIgnoreCase(trussCriteria.activeObjectKey);
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
		public File getActualFile() throws IOException {
			return getFile();
		}

		@Override
		public String getLinkPath() {
			return GDE.STRING_EMPTY;
		}

		@Override
		public boolean isLoadable() {
			return DataExplorer.getInstance().getActiveDevice() instanceof IHistoDevice;
		}

		@Override
		public void loadFile(Path tmpPath, String desiredRecordSetName) {
			((IHistoDevice) DataExplorer.getInstance().getActiveDevice()).importDeviceData(tmpPath);
		}
	}

	private final LongAdder						nonWorkableCount	= new LongAdder();
	private final List<Path>					excludedFiles			= new ArrayList<>();
	private final List<SourceDataSet>	sourceDataSets		= new ArrayList<>();

	private List<VaultCollector>			trusses						= new ArrayList<>();
	private Consumer<String>					signaler					= s -> {
																											};

	/**
	 * @param isStatusMessagesActive true activates status messages during file system access which is advisable in case of high file system
	 *          latency
	 */
	public SourceDataSetExplorer(boolean isStatusMessagesActive) {
		setStatusMessages(isStatusMessagesActive);
	}

	/**
	 * Explore the source files matching the validated extensions.
	 * Use file name extension lists and ignore file lists to determine the files required for the data access.
	 */
	private void listFiles(Map<Path, Set<DirectoryType>> pathsWithPermissions) {
		sourceDataSets.clear();
		nonWorkableCount.reset();
		excludedFiles.clear();

		int subDirectoryLevelMax = Settings.getInstance().getSubDirectoryLevelMax();
		for (Entry<Path, Set<DirectoryType>> entry : pathsWithPermissions.entrySet()) {
			try {
				sourceDataSets.addAll(getFileListing(entry.getKey().toFile(), subDirectoryLevelMax, entry.getValue()));
			} catch (FileNotFoundException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * Recursively walk a directory tree.
	 * @param rootDirectory
	 * @param recursionDepth specifies the depth of recursion cycles (0 means no recursion)
	 * @param directoryTypes which the returned files must match (file extension)
	 * @return the unsorted files matching the {@code extensions} and not discarded as per exclusion list
	 */
	private List<SourceDataSet> getFileListing(File rootDirectory, int recursionDepth, Set<DirectoryType> directoryTypes) throws FileNotFoundException {
		signaler.accept("get file names     " + rootDirectory.toString());
		List<SourceDataSet> result = new ArrayList<>();
		if (rootDirectory.isDirectory() && rootDirectory.canRead()) {
			File[] filesAndDirs = rootDirectory.listFiles();
			if (filesAndDirs != null) {
				for (File file : Arrays.asList(filesAndDirs)) {
					if (file.isFile()) {
						if (SourceDataSet.isWorkableFile(file.toPath(), directoryTypes)) {
							if (!ExclusionData.isExcluded(file.toPath())) {
								SourceDataSet originFile = SourceDataSet.createSourceDataSet(file);
								result.add(originFile);
							} else {
								excludedFiles.add(file.toPath());
								log.log(INFO, "file is excluded              ", file);
							}
						} else {
							nonWorkableCount.increment();
						}
					} else if (recursionDepth > 0) { // recursive walk by calling itself
						List<SourceDataSet> deeperList = getFileListing(file, recursionDepth - 1, directoryTypes);
						result.addAll(deeperList);
					}
				}
			}
		}
		signaler.accept("");
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
	public List<SourceDataSet> getSourceFiles() {
		return sourceDataSets;
	}

	/**
	 * Collect the trusses in the osd files and in the native files (if supported by the device) which comply with the {@code trussCriteria)}
	 * @param doListFiles true gets the files from the file system whereas false uses the files list from the last call
	 */
	public void screen4Trusses(Map<Path, Set<DirectoryType>> pathsWithPermissions, boolean doListFiles) {
		// a channel change without any additional criterion change can use the existent list of files for reading the trusses (performance)
		if (doListFiles) listFiles(pathsWithPermissions);

		TrussCriteria trussCriteria = new TrussCriteria(); // construct once only and save some time
		trusses = sourceDataSets.parallelStream() //
				.flatMap(d -> d.defineTrusses(trussCriteria, signaler)) //
				.collect(Collectors.toList());
		signaler.accept("");
	}

	/**
	 * @param isActive true activates status messages during file system access which is advisable in case of high file system latency
	 */
	public void setStatusMessages(boolean isActive) {
		if (isActive)
			signaler = s -> DataExplorer.getInstance().setStatusMessage(s);
		else
			signaler = s -> {
			};
	}

	public List<VaultCollector> getTrusses() {
		return this.trusses;
	}
}
