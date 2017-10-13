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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import gde.histo.cache.ExtendedVault;
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
	private List<String>											validatedImportExtentions	= new ArrayList<>();

	private final Map<DirectoryType, Path>		validatedDirectories			= new LinkedHashMap<>();
	private int																directoryFilesCount				= 0;																// all directory files
	private int																selectedFilesCount				= 0;																// selected from directory files (selected by extension)

	private final Map<String, VaultCollector>	unsuppressedTrusses				= new HashMap<>();									// authorized recordsets (excluded vaults eliminated - by the user in suppress mode)
	private final Map<String, VaultCollector>	suppressedTrusses					= new HashMap<>();									// excluded vaults

	public enum DirectoryType {
		DATA, IMPORT
	}

	/**
	 * Re- initializes the class.
	 */
	public synchronized void initialize() {
		this.validatedDevice = null;
		this.validatedChannel = null;
		this.validatedImportExtentions = new ArrayList<>();

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
		this.validatedImportExtentions = ((IHistoDevice) DataExplorer.getInstance().getActiveDevice()).getSupportedImportExtentions();

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
		List<String> lastImportExtentions = this.validatedImportExtentions;

		{ // determine validated values
			this.validatedDevice = this.application.getActiveDevice();
			this.validatedChannel = this.application.getActiveChannel();

			//special directory handling for MC3000 and Q200 supporting battery sets but store data in normal device folder
			String validatedDeviceName = this.validatedDevice.getName();
			if (this.application.getActiveDevice().getName().endsWith("-Set")) { // MC3000-Set -> MC3000, Q200-Set -> Q200 //$NON-NLS-1$
				validatedDeviceName = this.application.getActiveDevice().getName().substring(0, this.application.getActiveDevice().getName().length() - 4);
			}

			this.validatedImportExtentions.clear();
			this.validatedDirectories.clear();
			String subPathData = this.application.getActiveObject() == null ? validatedDeviceName : this.application.getObjectKey();
			this.validatedDirectories.put(DirectoryType.DATA, Paths.get(this.settings.getDataFilePath()).resolve(subPathData));
			String subPathImport = this.application.getActiveObject() == null ? GDE.STRING_EMPTY : this.application.getObjectKey();
			if (isNativeImport()) {
				this.validatedImportExtentions = ((IHistoDevice) this.validatedDevice).getSupportedImportExtentions();
				if (!this.validatedImportExtentions.isEmpty()) {
					Path validatedImportDir = this.validatedDevice.getDeviceConfiguration().getImportBaseDir();
					if (this.settings.getSearchImportPath() && validatedImportDir != null && !validatedImportDir.resolve(subPathImport).equals(this.validatedDirectories.get(DirectoryType.DATA)))
						this.validatedDirectories.put(DirectoryType.IMPORT, validatedImportDir.resolve(subPathImport));
				}
			}
		}

		boolean isFullChange = rebuildStep == RebuildStep.A_HISTOSET || this.unsuppressedTrusses.size() == 0;
		isFullChange = isFullChange || (lastDevice != null ? !lastDevice.getName().equals(this.validatedDevice.getName()) : this.validatedDevice != null);
		isFullChange = isFullChange || (lastChannel != null ? !lastChannel.getChannelConfigKey().equals(this.validatedChannel.getChannelConfigKey()) : this.validatedChannel != null);
		isFullChange = isFullChange || (lastHistoDataDir != null ? !lastHistoDataDir.equals(this.validatedDirectories.get(DirectoryType.DATA)) : true);
		isFullChange = isFullChange
				|| (lastHistoImportDir != null ? !lastHistoImportDir.equals(this.validatedDirectories.get(DirectoryType.IMPORT)) : this.validatedDirectories.containsKey(DirectoryType.IMPORT));
		isFullChange = isFullChange || !lastImportExtentions.containsAll(this.validatedImportExtentions) || !this.validatedImportExtentions.containsAll(lastImportExtentions);
		if (log.isLoggable(Level.OFF)) log.log(Level.OFF, String.format("isFullChange %s", isFullChange)); //$NON-NLS-1$

		if (isFullChange) {
			this.unsuppressedTrusses.clear();
			this.suppressedTrusses.clear();

			this.directoryFilesCount = addTrussesForDirectory(this.validatedDirectories.get(DirectoryType.DATA));
			if (this.validatedDirectories.containsKey(DirectoryType.IMPORT)) {
				this.directoryFilesCount += addTrussesForDirectory(this.validatedDirectories.get(DirectoryType.IMPORT));
			}

			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, String.format("in total %04d trusses found --- %04d valid trusses --- %04d excluded trusses", this.unsuppressedTrusses.size() + this.suppressedTrusses.size(), //$NON-NLS-1$
						this.unsuppressedTrusses.size(), this.suppressedTrusses.size()));
		}
		return !isFullChange;
	}

	/**
	 * @param directoryPath
	 * @return the number of files detected up to the specified sub-directory level
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 */
	private int addTrussesForDirectory(Path directoryPath) throws FileNotFoundException, IOException, NotSupportedFileFormatException {
		if (FileUtils.checkDirectoryExist(directoryPath.toString())) {
			List<File> files = FileUtils.getFileListing(directoryPath.toFile(), this.settings.getSubDirectoryLevelMax());
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("%04d files in histoDir '%s'  %s", files.size(), directoryPath.getFileName(), directoryPath)); //$NON-NLS-1$

			addTrusses(files);
			return files.size();
		} else {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("histoDir not found in %s", directoryPath)); //$NON-NLS-1$
			return 0;
		}
	}

	/**
	 * Use ignore lists to determine the vaults which are required for the data access.
	 * @param deviceConfigurations
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	*/
	private void addTrusses(List<File> files) throws IOException, NotSupportedFileFormatException {
		addTrusses(files, DataExplorer.getInstance().getDeviceSelectionDialog().getDevices());
	}

	private void addTrusses(List<File> files, TreeMap<String, DeviceConfiguration> deviceConfigurations) throws IOException, NotSupportedFileFormatException {
		final int suppressedSize = this.suppressedTrusses.size();
		final int unsuppressedSize = this.unsuppressedTrusses.size();
		int tmpSelectedFilesCount = 0;
		for (File file : files) {
			String extension = file.getName().substring(file.getName().lastIndexOf('.') + 1).toLowerCase();
			if (extension.equals(file.getName())) {
				// file w/o extension is discarded
			} else if (extension.equals(GDE.FILE_ENDING_OSD)) {
				File actualFile = getActualFile(file);
				if (actualFile != null) {
					tmpSelectedFilesCount++;
					String objectDirectory = getObjectKey(deviceConfigurations, file);
					for (VaultCollector truss : HistoOsdReaderWriter.readTrusses(actualFile, objectDirectory)) {
						truss.getVault().setLogLinkPath(file.getAbsolutePath());
						addTruss(truss);
					}
				}
			} else if (isNativeImport(file.toPath().getParent(), extension)) {
					tmpSelectedFilesCount++;
					String objectDirectory = getObjectKey(deviceConfigurations, file);
					String recordSetBaseName = DataExplorer.getInstance().getActiveChannel().getChannelConfigKey() + getRecordSetExtend(file.getName());
					addTruss(new VaultCollector(objectDirectory, file, 0, Channels.getInstance().size(), recordSetBaseName));
			} else {
				// file w/o histo support is discarded
			}
		}
		this.selectedFilesCount += tmpSelectedFilesCount;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%04d files found --- %04d total trusses --- %04d excluded trusses", files.size(), //$NON-NLS-1$
				this.unsuppressedTrusses.size() - unsuppressedSize + this.suppressedTrusses.size() - suppressedSize, this.suppressedTrusses.size() - suppressedSize));
	}

	/**
	 * Adds support for link files pointing to data files.
	 * Deletes a link file if the data file does not exist.
	 * @return the data file (differs from {@code file} if {@code file} is a link file)
	 */
	private static File getActualFile(File file) throws IOException {
		long startMillis = System.currentTimeMillis();
		File actualFile = new File(OperatingSystemHelper.getLinkContainedFilePath(file.getAbsolutePath()));
		// getLinkContainedFilePath may have long response times in case of an unavailable network resources
		// This is a workaround: Much better solution would be a function 'getLinkContainedFilePathWithoutAccessingTheLinkedFile'
		if (file.equals(actualFile) && (System.currentTimeMillis() - startMillis > 555) || !file.exists()) {
			log.log(Level.WARNING, "Dead OSD link " + file + " pointing to " + actualFile); //$NON-NLS-1$ //$NON-NLS-2$
			if (!file.delete()) {
				log.log(Level.WARNING, "could not delete link file ", file); //$NON-NLS-1$
			}
			actualFile = null;
		}
		return actualFile;
	}

	/**
	 * @param directoryPath is the path without filename
	 * @param extension is the extension without dot
	 * @return true if native import is supported and allowed
	 */
	private boolean isNativeImport(Path directoryPath, String extension) {
		return isNativeImport() && this.validatedImportExtentions.contains(extension) && this.validatedDirectories.containsKey(DirectoryType.IMPORT)
				&& directoryPath.startsWith(this.validatedDirectories.get(DirectoryType.IMPORT));
	}

	/**
	 * @return true if native import is supported and allowed
	 */
	private boolean isNativeImport() {
		return this.validatedDevice instanceof IHistoDevice && this.settings.getSearchDataPathImports();
	}

	/**
	 * @param deviceConfigurations
	 * @param file
	 * @return the file directory name if it is not a device directory or an empty string
	 */
	private static String getObjectKey(TreeMap<String, DeviceConfiguration> deviceConfigurations, File file) {
		String dirName = file.toPath().getParent().getFileName().toString();
		return !deviceConfigurations.containsKey(dirName) ? dirName : GDE.STRING_EMPTY;
	}

	/**
	 * Decide which list and put the truss into the list.
	 * @param truss
	 */
	private void addTruss(VaultCollector truss) {
		ExtendedVault vault = truss.getVault();
		if (this.settings.isSuppressMode() && ExclusionData.isExcluded(vault.getLogFileAsPath(), vault.getLogRecordsetBaseName())) {
			log.log(Level.INFO, String.format("discarded as per exclusion list %s %s   %s", vault.getLogFilePath(), vault.getLogRecordsetBaseName(), vault.getStartTimeStampFormatted()));
			this.suppressedTrusses.put(vault.getVaultName(), truss);
		} else {
			this.unsuppressedTrusses.put(vault.getVaultName(), truss);
		}
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
