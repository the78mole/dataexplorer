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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
import gde.histo.cache.HistoVault;
import gde.histo.cache.VaultCollector;
import gde.histo.datasources.SourceFolders.DirectoryType;
import gde.histo.device.IHistoDevice;
import gde.histo.io.HistoOsdReaderWriter;
import gde.io.FileHandler;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.OperatingSystemHelper;

/**
 * Exploring and checking source data sets (log files) on a file and vault attribute level.
 * @author Thomas Eickert (USER)
 */
public class AbstractSourceDataSets {
	private static final String	$CLASS_NAME	= AbstractSourceDataSets.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * Holds the selection criteria for trusses (vault skeletons) applicable prior to reading the full vault.
	 */
	protected static final class TrussCriteria {
		final List<Integer>	channelMixConfigNumbers;
		final long					minStartTimeStamp_ms;
		final String				activeObjectKey;
		final boolean				ignoreLogObjectKey;
		final Set<String>		realObjectKeys;

		public static TrussCriteria createUiBasedTrussCriteria() {
			return new TrussCriteria(DataExplorer.getInstance().getActiveDevice(), DataExplorer.getInstance().getActiveChannelNumber(),
					Settings.getInstance().getActiveObjectKey());
		}

		public static TrussCriteria createTrussCriteria(IDevice device, int channelNumber, String objectKey) {
			return new TrussCriteria(device, channelNumber, objectKey);
		}

		private TrussCriteria(IDevice device, int channelNumber, String objectKey) {
			channelMixConfigNumbers = device.getDeviceConfiguration().getChannelMixConfigNumbers(channelNumber);
			minStartTimeStamp_ms = LocalDate.now().minusMonths(Settings.getInstance().getRetrospectMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
			activeObjectKey = objectKey;
			ignoreLogObjectKey = Settings.getInstance().getIgnoreLogObjectKey();
			realObjectKeys = Settings.getInstance().getRealObjectKeys().collect(Collectors.toSet());
		}

		@Override
		public String toString() {
			return "TrussCriteria [channelMixConfigNumbers=" + this.channelMixConfigNumbers + ", minStartTimeStamp_ms=" + this.minStartTimeStamp_ms + ", filesWithOtherObject=" + this.ignoreLogObjectKey + ", realObjectKeys=" + this.realObjectKeys + "]";
		}
	}

	/**
	 * File types supported by the history.
	 */
	public static abstract class SourceDataSet {
		@SuppressWarnings("hiding")
		private static final Logger	log										= Logger.getLogger(SourceDataSet.class.getName());

		protected final Path		filePath;
		protected final IDevice	device;

		/** Use {@code getFile} only; is a link file or a real source file */
		private File						file;

		/**
		 * @return true if the current device supports both directory type and file extension
		 */
		public boolean isWorkableFile(Set<DirectoryType> directoryTypes, SourceFolders sourceFolders) {
			if (!isSupported()) {
				log.log(Level.INFO, "not a loadable type           ", filePath);
				return false;
			}
			if (!sourceFolders.isMatchingPath(filePath)) {
				log.log(Level.OFF, "not a matching file path      ", filePath);
				return false;
			}
			for (DirectoryType directoryType : directoryTypes) {
				if (directoryType.getDataSetExtensions(device).contains(getFileExtension(filePath))) {
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

		/**
		 * @param filePath is a file path
		 * @return null if the file is not supported
		 */
		@Nullable
		public static SourceDataSet createSourceDataSet(Path filePath, IDevice device) {
			String extension = getFileExtension(filePath);
			if (extension.equals(GDE.FILE_ENDING_OSD))
				return new OsdDataSet(filePath, device);
			else if (device instanceof IHistoDevice) {
				List<String> importExtentions = ((IHistoDevice) device).getSupportedImportExtentions();
				if (importExtentions.contains(extension)) {
					return new ImportDataSet(filePath, device); // todo implement logDataSet and native HottLogReader
				}
			}
			return null;
		}

		/**
		 * Determining the path is less costly than making a file from a path.
		 */
		protected SourceDataSet(Path filePath, IDevice device) {
			this.filePath = filePath;
			this.device = device;
		}

		/**
		 * @return the trusses delivered by the source file
		 */
		public abstract List<VaultCollector> getTrusses4Ui();

		/**
		 * @return true if the truss complies with the current device, object and start timestamp
		 */
		public abstract boolean isValidDeviceChannelObjectAndStart(TrussCriteria trussCriteria, HistoVault vault);

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
		public abstract boolean isSupported();

		/**
		 * @param desiredRecordSetName
		 */
		public abstract void loadFile(Path tmpPath, String desiredRecordSetName);

		public File getFile() {
			if (file == null) file = filePath.toFile();
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
			String dirName = filePath.getParent().getFileName().toString();
			DeviceConfigurations deviceConfigurations = DataExplorer.getInstance().getDeviceConfigurations();
			return !deviceConfigurations.contains(dirName) ? dirName : GDE.STRING_EMPTY;
		}

		/**
		 * @return the trusses or an empty stream
		 */
		public Stream<VaultCollector> defineTrusses(TrussCriteria trussCriteria, Consumer<String> signaler, IDevice uiDevice) {
			if (!uiDevice.equals(DataExplorer.getInstance().getActiveDevice())) throw new IllegalArgumentException("method applicable for legacy UI only");

			signaler.accept("get file properties    " + filePath.toString());
			return getTrusses4Ui().stream().filter(t -> isValidDeviceChannelObjectAndStart(trussCriteria, t.getVault()));
		}

		/**
		 * Promote trusses into vaults by reading the source file.
		 * @param trusses lists the requested vaults
		 */
		public abstract void readVaults4Ui(Path filePath, List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException,
				DataInconsitsentException, DataTypeException;

		/**
		 * @param desiredRecordSetName is a valid recordsetName or empty
		 * @return true if the file was opened or imported
		 */
		public boolean load(String desiredRecordSetName) {
			if (!FileUtils.checkFileExist(filePath.toString())) return false;
			if (!isSupported()) return false;

			loadFile(filePath, desiredRecordSetName);
			return true;
		}

		public Path getPath() {
			return this.filePath;
		}

		/**
		 * Compose the record set extend to give capability to identify source of this record set
		 * @return
		 */
		protected String getRecordSetExtend() {
			String fileName = filePath.getFileName().toString();
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
			return this.filePath.toString();
		}
	}

	private static final class OsdDataSet extends SourceDataSet {
		@SuppressWarnings("hiding")
		private static final Logger	log										= Logger.getLogger(OsdDataSet.class.getName());

		/** Is the real source file because {@source file} might be link file */
		protected File							actualFile						= null;
		private boolean							isActualFileVerified	= false;

		protected OsdDataSet(Path filePath, IDevice device) {
			super(filePath, device);
		}

		@Override
		public List<VaultCollector> getTrusses4Ui() {
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
		public boolean isValidDeviceChannelObjectAndStart(TrussCriteria trussCriteria, HistoVault vault) {
			if (device != null && !vault.getLogDeviceName().equals(device.getName()) //
			// HoTTViewer V3 -> HoTTViewerAdapter
					&& !(vault.getLogDeviceName().startsWith("HoTTViewer") && device.getName().equals("HoTTViewer"))) {
				log.log(Level.INFO, vault instanceof ExtendedVault //
						? String.format("no match device   %,7d kiB %s", ((ExtendedVault) vault).getLoadFileAsPath().toFile().length() / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
						: String.format("no match device  %s", vault.getLogFilePath()));
				return false;
			}
			if (!trussCriteria.channelMixConfigNumbers.contains(vault.getLogChannelNumber())) {
				log.log(Level.INFO, vault instanceof ExtendedVault //
						? String.format("no match channel%2d%,7d kiB %s", vault.getLogChannelNumber(), ((ExtendedVault) vault).getLoadFileAsPath().toFile().length() / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
						: String.format("no match channel  %s", vault.getLogFilePath()));
				return false;
			}
			if (vault.getLogStartTimestamp_ms() < trussCriteria.minStartTimeStamp_ms) {
				log.log(Level.INFO, vault instanceof ExtendedVault //
						? String.format("no match startTime%,7d kiB %s", ((ExtendedVault) vault).getLoadFileAsPath().toFile().length() / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
						: String.format("no match startTime  %s", vault.getLogFilePath()));
				return false;
			}

			if (trussCriteria.activeObjectKey.isEmpty()) {
				if (!vault.getLogObjectKey().isEmpty()) return true;
				// no object in the osd file is a undesirable case but the log is accepted in order to show all logs
				log.log(Level.INFO, vault instanceof ExtendedVault //
						? String.format("no match objectKey=%8s%,7d kiB %s", "empty", ((ExtendedVault) vault).getLoadFileAsPath().toFile().length() / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
						: String.format("no match objectKey  %s", vault.getLogFilePath()));
			} else {
				if (vault.getLogObjectKey().isEmpty()) {
					if (trussCriteria.ignoreLogObjectKey) return true;
					log.log(Level.INFO, vault instanceof ExtendedVault //
							? String.format("no match objectKey=%8s%,7d kiB %s", "empty", ((ExtendedVault) vault).getLoadFileAsPath().toFile().length() / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
							: String.format("no match objectKey  %s", vault.getLogFilePath()));
					return false;
				}
				boolean matchingObjectKey = trussCriteria.realObjectKeys.stream().anyMatch(s -> s.equalsIgnoreCase(vault.getLogObjectKey().trim()));
				if (!matchingObjectKey) {
					if (trussCriteria.ignoreLogObjectKey) return true;
					String objectDirectory = vault instanceof ExtendedVault ? ((ExtendedVault) vault).getLoadObjectDirectory() : vault.getLogObjectDirectory();
					boolean matchingObjectDirectory = trussCriteria.realObjectKeys.stream().anyMatch(s -> s.equalsIgnoreCase(objectDirectory));
					if (matchingObjectDirectory) return true;
					log.log(Level.INFO, vault instanceof ExtendedVault //
							? String.format("no match objectKey=%8s%,7d kiB %s", ((ExtendedVault) vault).getRectifiedObjectKey(), ((ExtendedVault) vault).getLoadFileAsPath().toFile().length() / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
							: String.format("no match objectKey  %s", vault.getLogFilePath()));
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
		public void readVaults4Ui(Path filePath, List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException,
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
		public boolean isSupported() {
			return true;
		}

		@Override
		public void loadFile(Path tmpPath, String desiredRecordSetName) {
			new FileHandler().openOsdFile(tmpPath.toString(), desiredRecordSetName);
		}

	}

	private static final class ImportDataSet extends SourceDataSet {
		@SuppressWarnings("hiding")
		private static final Logger	log										= Logger.getLogger(ImportDataSet.class.getName());

		protected ImportDataSet(Path filePath, IDevice device) {
			super(filePath, device);
		}

		@Override
		public List<VaultCollector> getTrusses4Ui() {
			String objectDirectory = getObjectKey();
			String recordSetBaseName = DataExplorer.getInstance().getActiveChannel().getChannelConfigKey() + getRecordSetExtend();
			VaultCollector truss = new VaultCollector(objectDirectory, getFile(), 0, Channels.getInstance().size(), recordSetBaseName,
					providesReaderSettings());
			truss.setSourceDataSet(this);
			return new ArrayList<>(Arrays.asList(truss));
		}

		@Override
		public boolean isValidDeviceChannelObjectAndStart(TrussCriteria trussCriteria, HistoVault vault) {
			if (vault.getLogStartTimestamp_ms() < trussCriteria.minStartTimeStamp_ms) {
				log.log(Level.INFO, vault instanceof ExtendedVault //
						? String.format("no match startTime%,7d kiB %s", ((ExtendedVault) vault).getLoadFileAsPath().toFile().length() / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
						: String.format("no match startTime  %s", vault.getLogFilePath()));
				return false;
			}
			if (!trussCriteria.activeObjectKey.isEmpty()) {
				boolean consistentObjectKey = vault.getLogObjectKey().equalsIgnoreCase(trussCriteria.activeObjectKey);
				if (!consistentObjectKey) {
					if (trussCriteria.ignoreLogObjectKey) return true;
					log.log(Level.INFO, vault instanceof ExtendedVault //
							? String.format("no match objectKey=%8s%,7d kiB %s", ((ExtendedVault) vault).getRectifiedObjectKey(), ((ExtendedVault) vault).getLoadFileAsPath().toFile().length() / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
							: String.format("no match objectKey  %s", vault.getLogFilePath()));
					return false;
				}
			}
			return true;
		}

		@Override
		public void readVaults4Ui(Path filePath, List<VaultCollector> trusses) throws IOException, NotSupportedFileFormatException,
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
		public boolean isSupported() {
			return device instanceof IHistoDevice;
		}

		@Override
		public void loadFile(Path tmpPath, String desiredRecordSetName) {
			((IHistoDevice) device).importDeviceData(tmpPath);
		}
	}

	protected final SourceFolders	sourceFolders;

	protected Consumer<String>		signaler	= s -> {
																					};

	/**
	 *
	 */
	protected AbstractSourceDataSets(SourceFolders sourceFolders) {
		this.sourceFolders = sourceFolders;
	}

	public void initSourceFolders(IDevice device) {
		try {
			sourceFolders.defineDirectories(device, false);
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

}