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

package gde.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.device.DeviceConfiguration;
import gde.histo.datasources.SupplementObjectFolder;
import gde.io.OsdReaderWriter;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

/**
 * Utilities to detect and maintain the object folders and object keys identity.
 * @author Thomas Eickert (USER)
 */
public class ObjectKeyCompliance {
	private static final String	$CLASS_NAME	= ObjectKeyCompliance.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * FilenameFilter based on valid log file extensions including those from all devices.
	 */
	private static final class LogFileFilter implements FilenameFilter {
		private final Set<String> validLogExtentions = DataExplorer.getInstance().getDeviceConfigurations().getValidLogExtentions();

		@Override
		public boolean accept(File dir, String name) {
			return validLogExtentions.stream().anyMatch(name.toLowerCase()::endsWith);
		}
	}

	/**
	 * Scan all OSD files in the data path for object names, create objects from them and merge them into the existing objects key list.
	 * Identify object keys with empty directories or without directories and query the user for deletion.
	 */
	public static void rebuildObjectKeys() {
		final ObjectKeyScanner objLnkSearch = new ObjectKeyScanner(true);
		objLnkSearch.start();
		try {
			objLnkSearch.join();
		} catch (InterruptedException e1) {
			log.log(java.util.logging.Level.WARNING, e1.getMessage(), e1);
		}

		if (!DataExplorer.getInstance().isWithUi()) {
			removeObjectKeys(objLnkSearch.getObsoleteObjectKeys());
		} else {
			if (!objLnkSearch.getObsoleteObjectKeys().isEmpty()) {
				Collections.sort(objLnkSearch.getObsoleteObjectKeys(), String.CASE_INSENSITIVE_ORDER);
				String message = Messages.getString(MessageIds.GDE_MSGI0063, new Object[] { objLnkSearch.getObsoleteObjectKeys() });
				if (SWT.YES == DataExplorer.getInstance().openYesNoMessageDialogSync(message)) {
					removeObjectKeys(objLnkSearch.getObsoleteObjectKeys());
				}
			} else if (GDE.shell.isDisposed())
				DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI0034));
			else
				DataExplorer.getInstance().openMessageDialogAsync(GDE.shell, Messages.getString(MessageIds.GDE_MSGI0034));
		}
	}

	/**
	 * Add the object directories and add the key to the object key list.
	 */
	public static void addObjectKeys(Set<String> newObjectKeys) {
		// build a new object list consisting from existing objects and new objects
		Set<String> objectListClone = Settings.getInstance().getRealObjectKeys().collect(Collectors.toSet());
		if (objectListClone.addAll(newObjectKeys)) {
			// ensure that all objects owns a directory
			for (String tmpObjectKey : newObjectKeys) {
				Path objectKeyDirPath = Paths.get(Settings.getInstance().getDataFilePath()).resolve(tmpObjectKey);
				FileUtils.checkDirectoryAndCreate(objectKeyDirPath.toString());
			}
			Settings.getInstance().setObjectList(objectListClone.toArray(new String[0]), Settings.getInstance().getActiveObject());
			log.log(Level.FINE, "object list updated and directories created for object keys : ", newObjectKeys);
		}
	}

	/**
	 * @param newObjectKeys is the list of object keys with an empty placeholder for the new object key for transfer into the settings
	 */
	public static void addObjectKey(String newObjKey, String[] newObjectKeys) {
		// reverse loop - the placeholder is at the end
		int selectionIndex = newObjectKeys.length - 1;
		for (; selectionIndex >= 0; selectionIndex--) {
			if (newObjectKeys[selectionIndex].equals(GDE.STRING_EMPTY)) {
				newObjectKeys[selectionIndex] = newObjKey;
				break;
			}
		}
		ObjectKeyCompliance.checkChannelForObjectKeyMissmatch(newObjKey);

		Settings.getInstance().setObjectList(newObjectKeys, newObjKey);
	}

	/**
	 * @param newObjectKeys is the list of object keys for transfer into the settings
	 */
	public static void renameObjectKey(String oldObjKey, String newObjKey, String[] newObjectKeys) {
		int answer = SWT.YES;
		if (DataExplorer.getInstance().isWithUi()) {
			// query if new object key should be used to modify all existing data files with the new corrected one
			answer = DataExplorer.getInstance().openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0048, new String[] { oldObjKey, newObjKey }));
		}
		if (answer == SWT.YES) {
			OsdReaderWriter.updateObjectKey(oldObjKey, newObjKey);
			DataExplorer.getInstance().updateCurrentObjectData(newObjKey);
		}

		if (FileUtils.checkDirectoryExist(Settings.getInstance().getDataFilePath() + GDE.FILE_SEPARATOR_UNIX + oldObjKey)) {
			// query for old directory deletion
			if (!DataExplorer.getInstance().isWithUi() //
					|| SWT.YES == DataExplorer.getInstance().openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGW0031)))
				FileUtils.deleteDirectory(Settings.getInstance().getDataFilePath() + GDE.FILE_SEPARATOR_UNIX + oldObjKey);
		}
		// replace modified object key
		int selectionIndex = 0;
		for (; selectionIndex < newObjectKeys.length; selectionIndex++) {
			if (newObjectKeys[selectionIndex].equals(oldObjKey)) {
				newObjectKeys[selectionIndex] = newObjKey;
				break;
			}
		}
		ObjectKeyCompliance.checkChannelForObjectKeyMissmatch(newObjKey);

		Settings.getInstance().setObjectList(newObjectKeys, newObjKey);
	}

	/**
	 * Support deleting empty object keys.
	 * @param newObjectKeys the object key list for removal and transfer into the settings
	 * @param removeIndex points to the object key to be deleted
	 */
	public static void removeObjectKey(String[] newObjectKeys, int removeIndex) {
		List<String> tmpObjectKeys = new ArrayList<>(Arrays.asList(newObjectKeys));
		String delObjectKey = tmpObjectKeys.remove(removeIndex).trim();
		int index = removeIndex >= 2 && !delObjectKey.isEmpty() ? removeIndex - 1 : tmpObjectKeys.size() > 1 ? 1 : 0;
		Settings.getInstance().setObjectList(tmpObjectKeys.toArray(new String[1]), tmpObjectKeys.get(index));
		if (!delObjectKey.isEmpty()) {
			FileUtils.deleteDirectory(Settings.getInstance().getDataFilePath() + GDE.FILE_SEPARATOR_UNIX + delObjectKey);
		}
	}

	/**
	 * Delete the object directories and remove from the object key list.
	 */
	public static void removeObjectKeys(List<String> obsoleteObjectKeys) {
		List<String> realObjectKeys = Settings.getInstance().getRealObjectKeys().collect(Collectors.toList());
		for (String tmpObjectKey : obsoleteObjectKeys) {
			Path objectKeyDirPath = Paths.get(Settings.getInstance().getDataFilePath()).resolve(tmpObjectKey);
			FileUtils.deleteDirectory(objectKeyDirPath.toString());
			realObjectKeys.remove(objectKeyDirPath.getFileName().toString());
		}
		Settings.getInstance().setObjectList(realObjectKeys.toArray(new String[0]), Settings.getInstance().getActiveObject());
	}

	/**
	 * @return the valid paths for source files
	 */
	private static ArrayList<Path> getSourcePaths() {
		ArrayList<Path> dirPaths = new ArrayList<Path>();
		{
			final String dataFilePath = Settings.getInstance().getDataFilePath();
			if (dataFilePath != null && !dataFilePath.trim().isEmpty() && !dataFilePath.equals(GDE.FILE_SEPARATOR_UNIX)) {
				dirPaths.add(Paths.get(dataFilePath));
				log.log(Level.FINE, "data path ", dataFilePath);
			}
		}
		{
			Stream<Path> dataFolders = Arrays.stream(Settings.getInstance().getDataFoldersCsv().split(GDE.STRING_CSV_SEPARATOR)) //
					.map(String::trim).filter(s -> !s.isEmpty()).map(Paths::get);
			dirPaths.addAll(dataFolders.collect(Collectors.toList()));
		}
		{
			Stream<Path> importFolders = Arrays.stream(Settings.getInstance().getImportFoldersCsv().split(GDE.STRING_CSV_SEPARATOR)) //
					.map(String::trim).filter(s -> !s.isEmpty()).map(Paths::get);
			dirPaths.addAll(importFolders.collect(Collectors.toList()));
		}
		if (DataExplorer.getInstance().getActiveDevice() != null) {
			String tmpImportDirPath = DataExplorer.getInstance().getActiveDevice().getDeviceConfiguration().getDataBlockType().getPreferredDataLocation();
			if (Settings.getInstance().getSearchImportPath() && tmpImportDirPath != null && !tmpImportDirPath.trim().isEmpty() && !tmpImportDirPath.equals(GDE.FILE_SEPARATOR_UNIX)) {
				Path path = Paths.get(tmpImportDirPath);
				// ignore object if path ends with a valid object
				String directoryName = path.getFileName().toString();
				if (Settings.getInstance().getValidatedObjectKey(directoryName).isPresent())
					path = path.getParent();
				else {
					// ignore device if path ends with a valid device
					String directoryName2 = path.getFileName().toString();
					if (DataExplorer.getInstance().getDeviceSelectionDialog().getDevices().keySet().stream().filter(s -> s.equals(directoryName2)).findFirst().isPresent())
						path = path.getParent();
					else
						// the directory is supposed to be a new object
						path = path.getParent();
					;
				}
				log.log(Level.FINE, "ImportBaseDir ", path); //$NON-NLS-1$
				dirPaths.add(path);
			}
		}
		return dirPaths;
	}

	/**
	 * Is based on {@code Files.walk} which recommends using the {@code try-with-resources} construct.
	 * @see java.nio.file.Files#walk(Path, java.nio.file.FileVisitOption...)
	 * @param basePath is any path
	 * @return the paths to all folders corresponding to the object keys
	 */
	public static Stream<Path> defineObjectPaths(Path basePath, Stream<String> objectKeys) throws IOException {
		List<String> lowerCaseKeys = objectKeys.map(String::toLowerCase).collect(Collectors.toList());
		Stream<Path> result = Files.walk(basePath).filter(Files::isDirectory) //
				.filter(p -> lowerCaseKeys.contains(p.getFileName().toString().toLowerCase()));
		return result;
	}

	/**
	 * Scan the sub-directories in the source paths.
	 * @return the non-empty sub-directory names which do not represent devices
	 */
	public static Set<String> defineObjectKeyCandidates(Map<String, DeviceConfiguration> devices) {
		final Set<String> excludedLowerCaseNames = new HashSet<>();
		excludedLowerCaseNames.add(SupplementObjectFolder.getSupplementObjectsPath().getFileName().toString().toLowerCase());
		excludedLowerCaseNames.addAll(devices.keySet().stream().map(String::toLowerCase).collect(Collectors.toList()));

		return readSourcePathsObjectKeys(excludedLowerCaseNames);
	}

	/**
	 * Scan the sub-directories in the source paths.
	 * @return the non-empty sub-directory names which neither represent devices nor object keys
	 */
	public static Set<String> defineObjectKeyNovelties(Map<String, DeviceConfiguration> devices) {
		final Set<String> excludedLowerCaseNames = Settings.getInstance().getRealObjectKeys().map(String::toLowerCase).collect(Collectors.toSet());
		excludedLowerCaseNames.add(SupplementObjectFolder.getSupplementObjectsPath().getFileName().toString().toLowerCase());
		excludedLowerCaseNames.addAll(devices.keySet().stream().map(String::toLowerCase).collect(Collectors.toList()));

		return readSourcePathsObjectKeys(excludedLowerCaseNames);
	}

	/**
	 * @return the folder names of non-empty sub-directories not matching the {@code excludedLowerCaseNames}
	 */
	private static Set<String> readSourcePathsObjectKeys(Set<String> excludedLowerCaseNames) {
		Set<String> directoryNames = new HashSet<>();
		for (Path dirPath : getSourcePaths()) {
			if (dirPath != null && !dirPath.toString().isEmpty()) {
				List<String> result = new ArrayList<>();
				Path supplementObjectsPath = SupplementObjectFolder.getSupplementObjectsPath();
				try (Stream<Path> stream = Files.walk(dirPath)) {
					result = stream //
							.filter(p -> !p.equals(dirPath)) // hasValidName
							.filter(p -> !excludedLowerCaseNames.contains(p.getFileName().toString().toLowerCase())) // hasValidName
							.filter(p -> !isSupplementMirrorRoot(p, supplementObjectsPath)) //
							.filter(Files::isDirectory) //
							.filter(p -> p.toFile().listFiles(new LogFileFilter()).length > 0) // hasLogFiles
							// .peek(p -> System.out.println(p)) //
							.map(Path::getFileName).map(Path::toString) //
							.distinct().collect(Collectors.toList());
				} catch (IOException e) {
					log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
				}
				directoryNames.addAll(result);
			}
		}
		return directoryNames;
	}

	/**
	 * @return true if {@code directoryPath} is just below the {@code supplementObjectsPath} and is a mirror copy
	 */
	private static boolean isSupplementMirrorRoot(Path directoryPath, Path supplementObjectsPath) {
		boolean isSupplementFolder = directoryPath.startsWith(supplementObjectsPath);
		Path subPath = supplementObjectsPath.relativize(directoryPath);
		if (isSupplementFolder && subPath.getNameCount() == 1) {
			if (subPath.toString().length() <= 2) return false;
			return subPath.toString().substring(0, 2).equals(GDE.STRING_UNDER_BAR + GDE.STRING_UNDER_BAR);
		}
		return false;
	}

	/**
	 * Check the new key against an existing channel key and ask for replacement.
	 * @param newObjectKey
	 * @return false if the object key has changed and the user refuses to update it
	 */
	public static void checkChannelForObjectKeyMissmatch(String newObjectKey) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null && !activeChannel.getObjectKey().isEmpty()) {
			String channelObjKey = activeChannel.getObjectKey();

			// check if selected key matches the existing object key or is new for this channel
			if (!newObjectKey.equals(channelObjKey)) { // channel has a key
				int answer = SWT.YES;
				if (DataExplorer.getInstance().isWithUi()) {
					answer = DataExplorer.getInstance().getActiveRecordSet() == null ? SWT.YES
							: (DataExplorer.getInstance().openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGT0205, new Object[] { channelObjKey,
									newObjectKey })));
				}
				if (answer == SWT.YES) { // replace existing objectkey in channel
					activeChannel.setObjectKey(newObjectKey);
					String updateFileDescription = activeChannel.getFileDescription();
					if (channelObjKey.length() >= GDE.MIN_OBJECT_KEY_LENGTH && updateFileDescription.contains(channelObjKey)) {
						if (newObjectKey.length() >= GDE.MIN_OBJECT_KEY_LENGTH) {
							updateFileDescription = updateFileDescription.substring(0, updateFileDescription.indexOf(channelObjKey)) + newObjectKey + updateFileDescription.substring(updateFileDescription.indexOf(channelObjKey) + channelObjKey.length());
						} else { // newObjectKey = ""
							updateFileDescription = updateFileDescription.substring(0, updateFileDescription.indexOf(channelObjKey) - 1) + updateFileDescription.substring(updateFileDescription.indexOf(channelObjKey) + channelObjKey.length());
						}
					} else if (newObjectKey.length() >= GDE.MIN_OBJECT_KEY_LENGTH) {
						updateFileDescription = updateFileDescription + GDE.STRING_BLANK + newObjectKey;
					}
					activeChannel.setFileDescription(updateFileDescription);
				}
				// do not exchange the object key in the channel/configuration, but keep the selector switch to enable new data load
			}
		}
	}

}
