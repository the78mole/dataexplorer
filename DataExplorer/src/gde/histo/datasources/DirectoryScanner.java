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

package gde.histo.datasources;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.istack.internal.Nullable;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.device.IDevice;
import gde.exception.NotSupportedFileFormatException;
import gde.histo.datasources.HistoSet.RebuildStep;
import gde.histo.datasources.SourceDataSetExplorer.SourceDataSet;
import gde.histo.device.IHistoDevice;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.ObjectKeyCompliance;

/**
 * Access to history source directories.
 * @author Thomas Eickert (USER)
 */
public final class DirectoryScanner {
	private final static String	$CLASS_NAME						= DirectoryScanner.class.getName();
	private final static Logger	log										= Logger.getLogger($CLASS_NAME);

	/**
	 * Average elapsed time per identified folder during folder scan.</br>
	 * local drive: 13,5 to 16 ms/folder </br>
	 * but the full data drive scan may take up to 400 ms.
	 */
	private final static int		SLOW_FOLDER_LIMIT_MS	= 222;

	private static WatchDir			watchDir;
	private static Thread				watchDirThread;

	/**
	 * Extended predicate which supports IOException and NotSupportedFileFormatException.
	 */
	@FunctionalInterface
	interface CheckedPredicate<T> {
		boolean test(T t) throws IOException, NotSupportedFileFormatException;
	}

	/**
	 * @return the data folder residing in the top level working directory (data path + object key resp device)
	 */
	public static Path getPrimaryFolder() {
		return DirectoryType.DATA.getDataSetPath();
	}

	/**
	 * Build the source folders list from the source directories.
	 * Avoid rebuilding if a selection of basic criteria did not change.
	 */
	public static final class SourceFoldersBuilder {

		private final EnumSet<DirectoryType>	validatedDirectoryTypes			= EnumSet.noneOf(DirectoryType.class);

		private IDevice												validatedDevice							= null;
		private Channel												validatedChannel						= null;
		private String												validatedImportDataLocation	= GDE.STRING_EMPTY;
		private String												validatedObjectKey					= GDE.STRING_EMPTY;

		private SourceFolders									lastFolders									= null;
		private SourceFolders									sourceFolders								= null;

		private boolean												isSlowFolderAccess					= false;
		private boolean												isMajorChange								= false;
		private boolean												isChannelChangeOnly					= false;

		/**
		 * Re- initializes the class.
		 */
		public synchronized void initialize() {
			this.validatedDirectoryTypes.clear();

			this.validatedDevice = null;
			this.validatedImportDataLocation = GDE.STRING_EMPTY;
			this.validatedObjectKey = GDE.STRING_EMPTY;

			this.lastFolders = null;
			this.sourceFolders = null;

			this.isSlowFolderAccess = false;
			this.isMajorChange = false;
			this.isChannelChangeOnly = false;
		}

		/**
		 * Determine if the criteria for determining the source log files have changed.
		 * Please note that not all criteria are checked:
		 * Thus changing any criteria like data file path or various histo settings must trigger a histo reset {@link RebuildStep#A_HISTOSET}).
		 * @param rebuildStep defines which steps during histo data collection are skipped
		 * @return true if the criteria have changed since the last invocation of the directory scanner
		 */
		public boolean validateAndBuild(RebuildStep rebuildStep) throws IOException, NotSupportedFileFormatException {
			IDevice lastDevice = validatedDevice;
			Channel lastChannel = validatedChannel;
			String lastImportDataLocations = validatedImportDataLocation;
			EnumSet<DirectoryType> lastDirectoryTypes = EnumSet.copyOf(validatedDirectoryTypes);
			String lastObjectKey = validatedObjectKey;

			boolean isFirstCall = lastDevice == null;
			isMajorChange = rebuildStep == RebuildStep.A_HISTOSET || isFirstCall;

			validatedDevice = DataExplorer.getInstance().getActiveDevice();
			boolean isNewDevice = lastDevice != null && validatedDevice != null ? !lastDevice.getName().equals(validatedDevice.getName())
					: validatedDevice != null;
			isMajorChange = isMajorChange || isNewDevice;

			validatedImportDataLocation = validatedDevice.getDeviceConfiguration().getDataBlockPreferredDataLocation();
			isMajorChange = isMajorChange || !lastImportDataLocations.equals(validatedImportDataLocation);

			// the import extentions do not have any influence on the validated folders list

			validatedDirectoryTypes.clear();
			validatedDirectoryTypes.addAll(Arrays.stream(DirectoryType.VALUES).filter(DirectoryType::isActive).collect(Collectors.toList()));
			isMajorChange = isMajorChange || !lastDirectoryTypes.equals(validatedDirectoryTypes);

			validatedObjectKey = Settings.getInstance().getActiveObjectKey();
			isMajorChange = isMajorChange || !lastObjectKey.equals(validatedObjectKey);

			if (isMajorChange) { // avoids costly directory scan and building directory file event listeners (WatchDir)
				sourceFolders = new SourceFolders();
				isSlowFolderAccess = sourceFolders.defineDirectories(validatedDirectoryTypes);
			}

			// the channel selection does not have any influence on the validated folders list
			validatedChannel = DataExplorer.getInstance().getActiveChannel();
			isChannelChangeOnly = !isMajorChange && RebuildStep.B_HISTOVAULTS.isEqualOrBiggerThan(rebuildStep) && !lastChannel.equals(validatedChannel);
			isMajorChange = isMajorChange || !lastChannel.equals(validatedChannel);
			return true;
		}

		/**
		 * @return true if the last build changed the source folders list
		 */
		public boolean isFoldersChange() {
			return lastFolders != null ? lastFolders.equals(sourceFolders) : false;
		}

		/**
		 * @return true if the last build detected a full change
		 */
		public boolean isMajorChange() {
			return this.isMajorChange;
		}

		@Deprecated
		public void build4Test(Path filePath) {
			sourceFolders = new SourceFolders();
			sourceFolders.defineDirectories4Test(filePath);
			sourceFolders.values().parallelStream().flatMap(Set::stream).map(Path::toString) //
					.forEach(FileUtils::checkDirectoryAndCreate);
		}
	}

	/**
	 * Detect valid sub paths.
	 */
	public static final class SourceFolders {

		/**
		 * Designed to hold all folder paths feeding the file scanning steps.
		 * Supports an equality check comparing the current paths to an older path map.
		 */
		private final Map<DirectoryType, Set<Path>>	folders							=									// formatting
				new EnumMap<DirectoryType, Set<Path>>(DirectoryType.class) {
					private static final long serialVersionUID = -8624409377603884008L;

					/**
					 * @return true if the keys are equal and the sets hold the same path strings
					 */
					@Override
					public boolean equals(Object obj) {
						if (this == obj) return true;
						if (obj == null) return false;
						if (getClass() != obj.getClass()) return false;
						@SuppressWarnings("unchecked")																						// reason is anonymous class
						Map<DirectoryType, Set<Path>> that = (Map<DirectoryType, Set<Path>>) obj;
						boolean hasSameDirectoryTypes = keySet().equals(that.keySet());
						if (hasSameDirectoryTypes) {
							boolean isEqual = true;
							for (Entry<DirectoryType, Set<Path>> entry : entrySet()) {
								Set<Path> thisSet = entry.getValue();
								Set<Path> thatSet = that.get(entry.getKey());
								isEqual &= thisSet.equals(thatSet);
								if (!isEqual) break;
							}
							log.log(Level.FINEST, "isEqual=", isEqual);
							return isEqual;
						} else {
							return false;
						}
					}
				};

		/**
		 * File access speed indicator. True is slow file system access.
		 * Simple algorithm.
		 */
		private boolean															isSlowFolderAccess	= false;

		/**
		 * Determine the valid directory paths from all log sources.
		 * The result depends on the active device and object.
		 * @return true if the file access is estimated as slow (due to network folders, ...)
		 */
		public boolean defineDirectories(EnumSet<DirectoryType> directoryTypes) throws IOException {
			long nanoTime = System.nanoTime();
			String activeObjectKey = Settings.getInstance().getActiveObjectKey();
			folders.clear();
			for (DirectoryType directoryType : directoryTypes) {
				Set<Path> currentPaths = defineCurrentPaths(directoryType);
				log.log(Level.FINE, directoryType.toString(), currentPaths);
				if (!Settings.getInstance().getActiveObjectKey().isEmpty()) {
					Set<Path> externalObjectPaths = defineExternalObjectPaths(directoryType, activeObjectKey);
					currentPaths.addAll(externalObjectPaths);
					log.log(Level.FINE, directoryType.toString(), externalObjectPaths);
				}
				folders.put(directoryType, currentPaths);
			}
			long foldersCount = folders.values().stream().flatMap(Collection::stream).count();
			long elapsed_ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime);
			boolean slowFolderAccess = elapsed_ms / (foldersCount + 3) > SLOW_FOLDER_LIMIT_MS; // +3 for initial overhead (data path)
			log.fine(() -> "slowFolderAccess=" + slowFolderAccess + "  numberOfFolders=" + foldersCount + " in " + elapsed_ms + " [ms]");
			return slowFolderAccess;
		}

		@Deprecated
		public void defineDirectories4Test(Path filePath) {
			Set<Path> paths = new HashSet<>();
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
		private Set<Path> defineCurrentPaths(DirectoryType directoryType) {
			Set<Path> newPaths = new HashSet<>();
			if (Settings.getInstance().getActiveObjectKey().isEmpty()) {
				Path deviceSubPath = directoryType.getActiveDeviceSubPath();
				Path rootPath = deviceSubPath != null ? directoryType.getBasePath().resolve(deviceSubPath) : directoryType.getBasePath();
				newPaths.add(rootPath);
			} else {
				Path basePath = directoryType.getBasePath();
				if (basePath == null) {
					// an unavailable path results in no files found
				} else {
					newPaths = defineObjectPaths(basePath, Settings.getInstance().getActiveObjectKey());
				}
			}
			return newPaths;
		}

		private Set<Path> defineObjectPaths(Path basePath, String objectKey) {
			Set<Path> result = new HashSet<Path>();
			Stream<String> objectKeys = Stream.of(objectKey);
			if (isSlowFolderAccess) DataExplorer.getInstance().setStatusMessage("find object folders in " + basePath.toString());
			try (Stream<Path> objectPaths = ObjectKeyCompliance.defineObjectPaths(basePath, objectKeys)) {
				result = objectPaths.collect(Collectors.toSet());
			} catch (IOException e) {
				log.log(Level.SEVERE, e.getMessage(), " is not accessible : " + e.getClass());
			}
			if (isSlowFolderAccess) DataExplorer.getInstance().setStatusMessage("");
			return result;
		}

		/**
		 * Determine the object directory paths from the external directories (data / import).
		 * They are defined in the DataExplorer properties.
		 * The result depends on the active device and object.
		 * @return the list with 0 .. n entries
		 */
		private Set<Path> defineExternalObjectPaths(DirectoryType directoryType, String objectKey) {
			Stream<Path> externalBaseDirs = directoryType.getExternalBaseDirs();
			return externalBaseDirs.map(p -> defineObjectPaths(p, objectKey)) //
					.flatMap(Set::stream).collect(Collectors.toSet());
		}

		public Set<Entry<DirectoryType, Set<Path>>> entrySet() {
			return folders.entrySet();
		}

		public Collection<Set<Path>> values() {
			return folders.values();
		}

		public Map<Path, Set<DirectoryType>> getMap() { // todo change folders to Map<Path, Set<DirectoryType>>
			Map<Path, Set<DirectoryType>> result = new HashMap<>();
			for (Entry<DirectoryType, Set<Path>> entry : folders.entrySet()) {
				for (Path path : entry.getValue()) {
					Set<DirectoryType> set = result.get(path);
					if (set == null) {
						set = EnumSet.noneOf(DirectoryType.class);
						result.put(path, set);
					}
					set.add(entry.getKey());
				}
			}
			return result;
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

		public int getFoldersCount() {
			return (int) values().parallelStream().flatMap(Collection::parallelStream).count();
		}

		/**
		 * @return the rightmost folder names, e.g. 'FS14 | MiniEllipse'
		 */
		public String getTruncatedFileNamesCsv() {
			String ellipsisText = Messages.getString(MessageIds.GDE_MSGT0864);
			return folders.values().stream().flatMap(Collection::stream).map(Path::getFileName).map(Path::toString) //
					.map(s -> s.length() > 22 ? s.substring(0, 22) + ellipsisText : s) //
					.distinct().collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR));
		}

		/**
		 * @return the full paths with prefix directory type, e.g. 'DATA: E:\User\Logs\FS14'
		 */
		public String getDecoratedPathsCsv() {
			List<String> directoryTypeTexts = new ArrayList<>();
			for (Entry<DirectoryType, Set<Path>> directoryEntry : folders.entrySet()) {
				String text = directoryEntry.getValue().stream().map(Path::toString) //
						.map(p -> directoryEntry.getKey().toString() + GDE.STRING_BLANK_COLON_BLANK + p) //
						.collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR));
				directoryTypeTexts.add(text);
			}
			return directoryTypeTexts.stream().collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR));
		}
	}

	private final SourceFoldersBuilder					sourceFoldersBuilder	= new SourceFoldersBuilder();
	private final CheckedPredicate<RebuildStep>	sourceFileValidator;

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
				return Paths.get(DataExplorer.getInstance().getActiveDevice().getDeviceConfiguration().getPureDeviceName());
			}

			@Override
			public Path getDataSetPath() {
				String activeObjectKey = Settings.getInstance().getActiveObjectKey();
				String subPathData = activeObjectKey.isEmpty() ? DataExplorer.getInstance().getActiveDevice().getDeviceConfiguration().getPureDeviceName() //
						: activeObjectKey;
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

			@Override
			public Stream<Path> getExternalBaseDirs() {
				try {
					return Arrays.stream(Settings.getInstance().getDataFoldersCsv().split(GDE.STRING_CSV_SEPARATOR)) //
							.map(String::trim).filter(s -> !s.isEmpty()).map(Paths::get);
				} catch (Exception e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					return Stream.empty();
				}
			}

			@Override
			public boolean isActive() {
				// the DataSetPath is always a valid path
				return true;
			}
		},
		IMPORT {
			@Override
			@Nullable
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
			@Nullable
			public Path getDataSetPath() {
				if (DataExplorer.getInstance().getActiveDevice() instanceof IHistoDevice && Settings.getInstance().getSearchImportPath()) {
					Path importDir = DataExplorer.getInstance().getActiveDevice().getDeviceConfiguration().getImportBaseDir();
					if (importDir == null) {
						return null;
					} else {
						Path importPath = importDir.resolve(Settings.getInstance().getActiveObjectKey());
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

			@Override
			public Stream<Path> getExternalBaseDirs() {
				try {
					return Arrays.stream(Settings.getInstance().getImportFoldersCsv().split(GDE.STRING_CSV_SEPARATOR)) //
							.map(String::trim).filter(s -> !s.isEmpty()).map(Paths::get);
				} catch (Exception e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					return Stream.empty();
				}
			}

			@Override
			public boolean isActive() {
				log.finest(() -> " IMPORT : Extensions.isEmpty=" + getDataSetExtensions().isEmpty() + " ExternalFolders.exist=" + getExternalBaseDirs().anyMatch(e -> true) + " dataSetPath=" + getDataSetPath());
				if (getDataSetExtensions().isEmpty()) {
					return false;
				} else {
					return getDataSetPath() != null || getExternalBaseDirs().anyMatch(e -> true);
				}
			};
		};

		/**
		 * Use this instead of values() to avoid repeatedly cloning actions.
		 */
		public static final DirectoryType[]	VALUES			= values();

		private final static DataExplorer		application	= DataExplorer.getInstance();

		/**
		 * @return the current directory path independent from object / device
		 */
		@Nullable
		public abstract Path getBasePath();

		/**
		 * @return the sub path for finding files assigned to the device
		 */
		@Nullable
		public abstract Path getActiveDeviceSubPath();

		/**
		 * @return the current directory path which depends on the object and the device
		 */
		@Nullable // if the device does not support imports or if the import path is empty
		public abstract Path getDataSetPath();

		/**
		 * @return the supported file extensions (e.g. '*.bin') or an empty list
		 */
		public abstract List<String> getDataSetExtensions();

		/**
		 * @return true if the prerequisites for the directory type are fulfilled
		 */
		public abstract boolean isActive();

		public abstract Stream<Path> getExternalBaseDirs();
	}

	public DirectoryScanner() {
		// define if the log paths are checked for any changed / new / deleted files
		if (Settings.getInstance().isSourceFileListenerActive()) {
			this.sourceFileValidator = (RebuildStep r) -> validateDirectoryFiles(r);
		} else {
			this.sourceFileValidator = (RebuildStep r) -> validateDirectoryPaths(r);
		}

		initialize();
	}

	/**
	 * Re- initializes the class.
	 */
	public synchronized void initialize() {
		sourceFoldersBuilder.initialize();
	}

	/**
	 * Re- Initializes the source log paths watcher.
	 */
	private void initializeWatchDir(List<Path> sourceLogPaths) {
		closeWatchDir();

		Map<Path, Set<DirectoryType>> folders = sourceFoldersBuilder.sourceFolders.getMap();
		Predicate<Path> workableFileDecider = new Predicate<Path>() {
			@Override
			public boolean test(Path filePath) {
				Set<DirectoryType> directoryTypes = folders.entrySet().stream() //
						.filter(e -> filePath.startsWith(e.getKey())) //
						.map(e -> e.getValue()).flatMap(Set::stream)//
						.collect(Collectors.toSet());
				return SourceDataSet.isWorkableFile(filePath, directoryTypes);
			}
		};

		try {
			boolean recursive = true;
			watchDir = new WatchDir(sourceLogPaths, recursive, workableFileDecider);
			watchDirThread = new Thread(watchDir::processEvents, "watchDir");
			try {
				watchDirThread.start();
				log.log(Level.FINEST, "watchDir thread started");
			} catch (RuntimeException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		} catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 *
	 */
	public static void closeWatchDir() {
		if (watchDirThread != null) {
			try {
				watchDirThread.interrupt();
				watchDirThread.join();
			} catch (InterruptedException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * Use alternatively a path or files checker function which conforms to the DE settings.
	 * @return the rebuild step conforming to the input value and the validation scan results
	 */
	public RebuildStep isValidated(RebuildStep rebuildStep) throws IOException, NotSupportedFileFormatException {
		if (sourceFileValidator.test(rebuildStep))
			return rebuildStep;
		else if (rebuildStep.isEqualOrBiggerThan(RebuildStep.B_HISTOVAULTS))
			return rebuildStep;
		else
			return RebuildStep.B_HISTOVAULTS;
	}

	/**
	 * @param rebuildStep defines which steps during histo data collection are skipped
	 * @return true if the list of source log directories has not changed and the directories did not send any events.
	 */
	private boolean validateDirectoryFiles(RebuildStep rebuildStep) throws IOException, NotSupportedFileFormatException {
		sourceFoldersBuilder.validateAndBuild(rebuildStep);
		boolean isValid = !sourceFoldersBuilder.isMajorChange();

		if (watchDir == null || !isValid) {
			initializeWatchDir(sourceFoldersBuilder.sourceFolders.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
		}
		log.log(Level.FINER, "hasChangedLogFiles=", watchDir.hasChangedLogFiles());
		return isValid && !watchDir.hasChangedLogFilesThenReset();

	}

	/**
	 * @param rebuildStep defines which steps during histo data collection are skipped
	 * @return true if the list of source log directories has not changed
	 */
	private boolean validateDirectoryPaths(RebuildStep rebuildStep) throws IOException, NotSupportedFileFormatException {
		sourceFoldersBuilder.validateAndBuild(rebuildStep);
		return !sourceFoldersBuilder.isMajorChange();
	}

	public int getValidatedFoldersCount() {
		if (sourceFoldersBuilder.sourceFolders != null) {
			return sourceFoldersBuilder.sourceFolders.getFoldersCount();
		} else {
			return 0;
		}
	}

	public SourceFolders getSourceFolders() {
		return sourceFoldersBuilder.sourceFolders;
	}

	public boolean isSlowFolderAccess() {
		return sourceFoldersBuilder.isSlowFolderAccess;
	}

	/**
	 * @return true if nothing else except the channel has changed since the last directory scan
	 */
	public boolean isChannelChangeOnly() {
		return sourceFoldersBuilder.isChannelChangeOnly;
	}

	@Deprecated
	public void build4Test(Path filePath) {
		sourceFoldersBuilder.build4Test(filePath);
	}
}
