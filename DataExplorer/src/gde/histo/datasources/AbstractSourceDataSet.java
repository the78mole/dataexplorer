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

import static java.util.logging.Level.SEVERE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Stream;

import com.sun.istack.internal.Nullable;

import gde.Analyzer;
import gde.GDE;
import gde.config.DeviceConfigurations;
import gde.data.Channels;
import gde.device.IDevice;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.HistoVault;
import gde.histo.cache.VaultCollector;
import gde.histo.datasources.SourceFolders.DirectoryType;
import gde.histo.datasources.VaultChecker.TrussCriteria;
import gde.histo.device.IHistoDevice;
import gde.histo.io.HistoOsdReaderWriter;
import gde.histo.utils.PathUtils;
import gde.io.FileHandler;
import gde.log.Logger;
import gde.utils.FileUtils;
import gde.utils.OperatingSystemHelper;

/**
 * Exploring and checking source data sets (log files) on a file and vault attribute level.
 * @author Thomas Eickert (USER)
 */
public abstract class AbstractSourceDataSet {
	private static final String	$CLASS_NAME	= AbstractSourceDataSet.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * File types supported by the history.
	 */
	public static abstract class SourceDataSet {
		@SuppressWarnings("hiding")
		private static final Logger	log	= Logger.getLogger(SourceDataSet.class.getName());

		protected final Path				filePath;
		protected final IDevice			device;

		/** Use {@code getFile} only; is a link file or a real source file */
		private File								file;

		/**
		 * @return true if the current device supports both directory type and file extension
		 */
		public boolean isWorkableFile(Set<DirectoryType> directoryTypes, SourceFolders sourceFolders) {
			return VaultChecker.isWorkableDataSet(device, filePath, directoryTypes, sourceFolders);
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
			DeviceConfigurations deviceConfigurations = Analyzer.getInstance().getDeviceConfigurations();
			return !deviceConfigurations.contains(dirName) ? dirName : GDE.STRING_EMPTY;
		}

		/**
		 * @return the trusses or an empty stream
		 */
		public Stream<VaultCollector> defineTrusses(TrussCriteria trussCriteria, Consumer<String> signaler, IDevice uiDevice) {
			signaler.accept("get file properties    " + filePath.toString());
			return getTrusses4Ui().stream().filter(t -> isValidDeviceChannelObjectAndStart(trussCriteria, t.getVault()));
		}

		/**
		 * Promote trusses into vaults by reading the source file.
		 * @param trusses lists the requested vaults
		 */
		public abstract void readVaults4Ui(Path sourcePath, List<VaultCollector> trusses);

		/**
		 * @param desiredRecordSetName is a valid recordsetName or empty
		 * @return true if the file was opened or imported
		 */
		public boolean load(String desiredRecordSetName) {
			if (!FileUtils.checkFileExist(filePath.toString())) return false;

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
				try (InputStream sourceInputStream = Analyzer.getInstance().getDataAccess().getSourceInputStream(getActualFile().toPath());) {
					trusses = HistoOsdReaderWriter.readTrusses(sourceInputStream, getActualFile().toPath(), objectDirectory);
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
			return VaultChecker.matchDeviceChannelObjectAndStart(device, trussCriteria, vault);
		}

		@Override
		public void readVaults4Ui(Path sourcePath, List<VaultCollector> trusses) {
			try (InputStream sourceInputStream = Analyzer.getInstance().getDataAccess().getSourceInputStream(sourcePath);) {
				HistoOsdReaderWriter.readVaults(Analyzer.getInstance().getDataAccess().getSourceInputStream(sourcePath), trusses, sourcePath.toFile().length());
			} catch (Exception e) {
				log.log(SEVERE, e.getMessage(), e);
				log.info(() -> String.format("invalid file format: %s  channelNumber=%d  %s", //
						Analyzer.getInstance().getActiveDevice().getName(), Analyzer.getInstance().getActiveChannel().getNumber(), sourcePath));
			}
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
		public void loadFile(Path tmpPath, String desiredRecordSetName) {
			new FileHandler().openOsdFile(tmpPath.toString(), desiredRecordSetName);
		}

	}

	private static final class ImportDataSet extends SourceDataSet {
		@SuppressWarnings("hiding")
		private static final Logger log = Logger.getLogger(ImportDataSet.class.getName());

		protected ImportDataSet(Path filePath, IDevice device) {
			super(filePath, device);
		}

		@Override
		public List<VaultCollector> getTrusses4Ui() {
			String objectDirectory = getObjectKey();
			String recordSetBaseName = Analyzer.getInstance().getActiveChannel().getChannelConfigKey() + getRecordSetExtend();
			VaultCollector truss = new VaultCollector(objectDirectory, getFile().toPath(), 0, Channels.getInstance().size(), recordSetBaseName,
					providesReaderSettings());
			truss.setSourceDataSet(this);
			return new ArrayList<>(Arrays.asList(truss));
		}

		@Override
		public boolean isValidDeviceChannelObjectAndStart(TrussCriteria trussCriteria, HistoVault vault) {
			return VaultChecker.matchObjectAndStart(trussCriteria, vault);
		}

		@Override
		public void readVaults4Ui(Path sourcePath, List<VaultCollector> trusses) {
			if (trusses.isEmpty()) throw new IllegalArgumentException("at least one trusses entry is required");
			List<ExtendedVault> histoVaults = new ArrayList<>();

			String loadFilePath = trusses.get(0).getVault().getLoadFilePath();
			for (VaultCollector truss : trusses) {
				if (truss.getVault().getLoadFilePath().equals(loadFilePath)) {
					Supplier<InputStream> inputStream = () -> Analyzer.getInstance().getDataAccess().getSourceInputStream(sourcePath) ;
					try  {
						((IHistoDevice) Analyzer.getInstance().getActiveDevice()).getRecordSetFromImportFile(inputStream, truss);
					} catch (Exception e) {
						log.log(SEVERE, e.getMessage(), e);
						log.info(() -> String.format("invalid file format: %s  channelNumber=%d  %s", //
								Analyzer.getInstance().getActiveDevice().getName(), Analyzer.getInstance().getActiveChannel().getNumber(), sourcePath));
					}
					histoVaults.add(truss.getVault());
				} else
					throw new UnsupportedOperationException("all trusses must carry the same logFilePath");
			}
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
		public void loadFile(Path tmpPath, String desiredRecordSetName) {
			((IHistoDevice) device).importDeviceData(tmpPath);
		}
	}

	/**
	 * @param filePath is a file path
	 * @return null if the file is not supported
	 */
	@Nullable
	public static SourceDataSet createSourceDataSet(Path filePath, IDevice device) {
		String extension = PathUtils.getFileExtension(filePath);
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

	protected final SourceFolders	sourceFolders;

	protected Consumer<String>		signaler	= s -> {
																					};

	/**
	 *
	 */
	protected AbstractSourceDataSet(SourceFolders sourceFolders) {
		this.sourceFolders = sourceFolders;
	}

}