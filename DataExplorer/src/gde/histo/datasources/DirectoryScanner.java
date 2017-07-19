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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.NotSupportedFileFormatException;
import gde.histo.cache.VaultCollector;
import gde.histo.datasources.HistoSet.RebuildStep;
import gde.histo.device.IHistoDevice;
import gde.histo.exclusions.ExclusionData;
import gde.histo.io.HistoOsdReaderWriter;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.OperatingSystemHelper;

/**
 * Access to history source directories.
 * @author Thomas Eickert (USER)
 */
public final class DirectoryScanner {
	private final static String								$CLASS_NAME								= DirectoryScanner.class.getName();
	private final static Logger								log												= Logger.getLogger($CLASS_NAME);

	private final DataExplorer								application								= DataExplorer.getInstance();
	private final Settings										settings									= Settings.getInstance();

	private IDevice														validatedDevice						= null;
	private Channel														validatedChannel					= null;
	private String														validatedImportExtention	= GDE.STRING_EMPTY;

	private final Map<DirectoryType, Path>		validatedDirectories			= new LinkedHashMap<>();
	private int																directoryFilesCount				= 0;																// all directory files
	private int																selectedFilesCount				= 0;																// selected from directory files (selected by extension)

	private final Map<String, VaultCollector>	unsuppressedTrusses				= new HashMap<>();									// authorized recordsets (excluded vaults eliminated - by the user in suppress mode)
	private final Map<String, VaultCollector>	suppressedTrusses					= new HashMap<>();									// excluded vaults

	public enum DirectoryType {
		DATA, IMPORT
	};

	/**
	 * Re- initializes the class.
	 */
	public synchronized void initialize() {
		this.validatedDevice = null;
		this.validatedChannel = null;
		this.validatedImportExtention = GDE.STRING_EMPTY;

		this.validatedDirectories.clear();
		this.directoryFilesCount = 0;
		this.selectedFilesCount = 0;

		this.unsuppressedTrusses.clear();
		this.suppressedTrusses.clear();
	}

	public void setHistoFilePaths4Test(Path filePath, int subDirLevelMax, TreeMap<String, DeviceConfiguration> devices) throws IOException, NotSupportedFileFormatException {
		this.validatedDirectories.clear();
		if (filePath == null)
			this.validatedDirectories.put(DirectoryType.DATA, Paths.get(this.settings.getDataFilePath()));
		else
			this.validatedDirectories.put(DirectoryType.DATA, filePath);
		this.validatedImportExtention = GDE.FILE_ENDING_DOT_BIN;

		this.unsuppressedTrusses.clear();
		this.suppressedTrusses.clear();
		{
			FileUtils.checkDirectoryAndCreate(this.validatedDirectories.get(DirectoryType.DATA).toString());
			List<File> files = FileUtils.getFileListing(this.validatedDirectories.get(DirectoryType.DATA).toFile(), subDirLevelMax);
			log.log(Level.INFO, String.format("%04d files found in dataDir %s", files.size(), this.validatedDirectories.get(DirectoryType.DATA))); //$NON-NLS-1$

			addTrusses(files, devices);
		}
		log.log(Level.INFO, String.format("%04d files selected", this.unsuppressedTrusses.size())); //$NON-NLS-1$
	}

	/**
	 * Determine file paths from an input directory and an import directory which fit to the objectKey, the device, the channel and the file extensions.
	 * @param rebuildStep defines which steps during histo data collection are skipped
	 * @return true if the list of file paths has already been valid
	 * @throws NotSupportedFileFormatException
	 * @throws IOException
	 */
	public boolean validateHistoFilePaths(RebuildStep rebuildStep) throws IOException, NotSupportedFileFormatException {
		IDevice lastDevice = this.validatedDevice;
		Channel lastChannel = this.validatedChannel;
		Path lastHistoDataDir = this.validatedDirectories.get(DirectoryType.DATA);
		Path lastHistoImportDir = this.validatedDirectories.get(DirectoryType.IMPORT);
		String lastImportExtention = this.validatedImportExtention;

		this.validatedDevice = this.application.getActiveDevice();
		this.validatedChannel = this.application.getActiveChannel();

		//special directory handling for MC3000 and Q200 supporting battery sets but store data in normal device folder
		String validatedDeviceName = this.validatedDevice.getName();
		if (this.application.getActiveDevice().getName().endsWith("-Set")) { // MC3000-Set -> MC3000, Q200-Set -> Q200 //$NON-NLS-1$
			validatedDeviceName = this.application.getActiveDevice().getName().substring(0, this.application.getActiveDevice().getName().length() - 4);
		}

		this.validatedDirectories.clear();
		String subPathData = this.application.getActiveObject() == null ? validatedDeviceName : this.application.getObjectKey();
		this.validatedDirectories.put(DirectoryType.DATA, Paths.get(this.settings.getDataFilePath()).resolve(subPathData));
		String subPathImport = this.application.getActiveObject() == null ? GDE.STRING_EMPTY : this.application.getObjectKey();
		this.validatedImportExtention = this.validatedDevice instanceof IHistoDevice ? ((IHistoDevice) this.validatedDevice).getSupportedImportExtention() : GDE.STRING_EMPTY;
		Path validatedImportDir = this.validatedDevice.getDeviceConfiguration().getImportBaseDir();
		if (this.settings.getSearchImportPath() && validatedImportDir != null && !this.validatedImportExtention.isEmpty()
				&& !validatedImportDir.resolve(subPathImport).equals(this.validatedDirectories.get(DirectoryType.DATA)))
			this.validatedDirectories.put(DirectoryType.IMPORT, validatedImportDir.resolve(subPathImport));

		boolean isFullChange = rebuildStep == RebuildStep.A_HISTOSET || this.unsuppressedTrusses.size() == 0;
		isFullChange = isFullChange || (lastDevice != null ? !lastDevice.getName().equals(validatedDeviceName) : this.validatedDevice != null);
		isFullChange = isFullChange || (lastChannel != null ? !lastChannel.getChannelConfigKey().equals(this.validatedChannel.getChannelConfigKey()) : this.validatedChannel != null);
		isFullChange = isFullChange || (lastHistoDataDir != null ? !lastHistoDataDir.equals(this.validatedDirectories.get(DirectoryType.DATA)) : true);
		isFullChange = isFullChange
				|| (lastHistoImportDir != null ? !lastHistoImportDir.equals(this.validatedDirectories.get(DirectoryType.IMPORT)) : this.validatedDirectories.containsKey(DirectoryType.IMPORT));
		isFullChange = isFullChange || (lastImportExtention != null ? !lastImportExtention.equals(this.validatedImportExtention) : this.validatedImportExtention != null);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("isFullChange %s", isFullChange)); //$NON-NLS-1$

		if (isFullChange) {
			this.unsuppressedTrusses.clear();
			this.suppressedTrusses.clear();
			{
				FileUtils.checkDirectoryAndCreate(this.validatedDirectories.get(DirectoryType.DATA).toString());
				List<File> files = FileUtils.getFileListing(this.validatedDirectories.get(DirectoryType.DATA).toFile(), this.settings.getSubDirectoryLevelMax());
				this.directoryFilesCount = files.size();
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE,
						String.format("%04d files in histoDataDir '%s'  %s", files.size(), this.validatedDirectories.get(DirectoryType.DATA).getFileName(), this.validatedDirectories.get(DirectoryType.DATA))); //$NON-NLS-1$

				addTrusses(files, DataExplorer.getInstance().getDeviceSelectionDialog().getDevices());
			}
			if (this.validatedDirectories.containsKey(DirectoryType.IMPORT)) {
				FileUtils.checkDirectoryAndCreate(this.validatedDirectories.get(DirectoryType.IMPORT).toString());
				List<File> files = FileUtils.getFileListing(this.validatedDirectories.get(DirectoryType.IMPORT).toFile(), this.settings.getSubDirectoryLevelMax());
				this.directoryFilesCount += files.size();
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("%04d files in histoImportDir '%s'  %s", files.size(), this.validatedDirectories.get(DirectoryType.IMPORT).getFileName(), //$NON-NLS-1$
						this.validatedDirectories.get(DirectoryType.IMPORT)));

				addTrusses(files, DataExplorer.getInstance().getDeviceSelectionDialog().getDevices());
			}

			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, String.format("in total %04d trusses found --- %04d valid trusses --- %04d excluded trusses", this.unsuppressedTrusses.size() + this.suppressedTrusses.size(), //$NON-NLS-1$
						this.unsuppressedTrusses.size(), this.suppressedTrusses.size()));
		}
		return !isFullChange;
	}

	/**
	 * Use ignore lists to determine the vaults which are required for the data access.
	 * @param deviceConfigurations
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	*/
	private void addTrusses(List<File> files, TreeMap<String, DeviceConfiguration> deviceConfigurations) throws IOException, NotSupportedFileFormatException {
		final String supportedImportExtention = this.application.getActiveDevice() instanceof IHistoDevice ? ((IHistoDevice) this.application.getActiveDevice()).getSupportedImportExtention()
				: GDE.STRING_EMPTY;
		final int suppressedSize = this.suppressedTrusses.size();
		final int unsuppressedSize = this.unsuppressedTrusses.size();
		int tmpSelectedFilesCount = 0;
		for (File file : files) {
			if (file.getName().endsWith(GDE.FILE_ENDING_OSD)) {
				long startMillis = System.currentTimeMillis();
				File actualFile = new File(OperatingSystemHelper.getLinkContainedFilePath(file.getAbsolutePath()));
				// getLinkContainedFilePath may have long response times in case of an unavailable network resources
				// This is a workaround: Much better solution would be a function 'getLinkContainedFilePathWithoutAccessingTheLinkedFile'
				if (file.equals(actualFile) && (System.currentTimeMillis() - startMillis > 555) || !file.exists()) {
					log.log(Level.WARNING, "Dead OSD link " + file + " pointing to " + actualFile); //$NON-NLS-1$ //$NON-NLS-2$
					if (!file.delete()) {
						log.log(Level.WARNING, "could not delete link file ", file); //$NON-NLS-1$
					}
				} else {
					tmpSelectedFilesCount++;
					String objectDirectory = !deviceConfigurations.containsKey(file.toPath().getParent().getFileName().toString()) ? file.toPath().getParent().getFileName().toString() : GDE.STRING_EMPTY;
					for (VaultCollector truss : HistoOsdReaderWriter.readTrusses(actualFile, objectDirectory)) {
						truss.getVault().setLogLinkPath(file.getAbsolutePath());
						if (this.settings.isSuppressMode()) {
							if (ExclusionData.isExcluded(truss.getVault().getLogFileAsPath(), truss.getVault().getLogRecordsetBaseName())) {
								log.log(Level.INFO,
										String.format("OSD candidate is in the exclusion list %s %s   %s", actualFile, truss.getVault().getLogRecordsetBaseName(), truss.getVault().getStartTimeStampFormatted())); //$NON-NLS-1$
								this.suppressedTrusses.put(truss.getVault().getVaultName(), truss);
							} else
								this.unsuppressedTrusses.put(truss.getVault().getVaultName(), truss);
						} else
							this.unsuppressedTrusses.put(truss.getVault().getVaultName(), truss);
					}
				}
			} else if (!supportedImportExtention.isEmpty() && file.getName().endsWith(supportedImportExtention)) {
				if (this.settings.getSearchDataPathImports()
						|| (this.validatedDirectories.containsKey(DirectoryType.IMPORT) && file.toPath().startsWith(this.validatedDirectories.get(DirectoryType.IMPORT)))) {
					tmpSelectedFilesCount++;
					String objectDirectory = !deviceConfigurations.containsKey(file.toPath().getParent().getFileName().toString()) ? file.toPath().getParent().getFileName().toString() : GDE.STRING_EMPTY;
					String recordSetBaseName = DataExplorer.getInstance().getActiveChannel().getChannelConfigKey() + getRecordSetExtend(file.getName());
					VaultCollector truss = new VaultCollector(objectDirectory, file, 0, Channels.getInstance().size(), recordSetBaseName);
					if (this.settings.isSuppressMode()) {
						if (ExclusionData.isExcluded(truss.getVault().getLogFileAsPath(), truss.getVault().getLogRecordsetBaseName())) {
							log.log(Level.INFO, String.format("BIN candidate is in the exclusion list %s %s  %s", file, truss.getVault().getLogRecordsetBaseName(), truss.getVault().getStartTimeStampFormatted())); //$NON-NLS-1$
							this.suppressedTrusses.put(truss.getVault().getVaultName(), truss);
						} else
							this.unsuppressedTrusses.put(truss.getVault().getVaultName(), truss);
					} else
						this.unsuppressedTrusses.put(truss.getVault().getVaultName(), truss);
				}
			} else {
				// file is discarded
			}
		}
		this.selectedFilesCount += tmpSelectedFilesCount;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%04d files found --- %04d total trusses --- %04d excluded trusses", files.size(), //$NON-NLS-1$
				this.unsuppressedTrusses.size() - unsuppressedSize + this.suppressedTrusses.size() - suppressedSize, this.suppressedTrusses.size() - suppressedSize));
	}

	/**
	 * Compose the record set extend to give capability to identify source of this record set
	 * @param fileName
	 * @return
	 */
	private String getRecordSetExtend(String fileName) {
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
				if (fileName.substring(0, fileName.length()).length() <= 8 + 4) recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.length() - 4) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		return recordSetNameExtend;
	}

	public Map<String, VaultCollector> getUnsuppressedTrusses() {
		return this.unsuppressedTrusses;
	}

	public Map<String, VaultCollector> getSuppressedTrusses() {
		return this.suppressedTrusses;
	}

	public Map<DirectoryType, Path> getValidatedDirectories() {
		return this.validatedDirectories;
	}

	public int getDirectoryFilesCount() {
		return this.directoryFilesCount;
	}

	public int getSelectedFilesCount() {
		return this.selectedFilesCount;
	}

}
