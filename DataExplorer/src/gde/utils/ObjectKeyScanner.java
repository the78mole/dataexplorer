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

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017 Winfried Bruegmann
****************************************************************************************/
package gde.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.exception.NotSupportedFileFormatException;
import gde.io.OsdReaderWriter;
import gde.log.Level;
import gde.ui.DataExplorer;

/**
 * This thread implementation goes through all sub folders of the data location and creates links to object related files
 * @author Winfried Brügmann
 */
public class ObjectKeyScanner extends Thread {
	final private static Logger	log					= Logger.getLogger(ObjectKeyScanner.class.getName());

	final private DataExplorer	application	= DataExplorer.getInstance();
	final private Settings			settings		= Settings.getInstance();
	String											objectKey		= GDE.STRING_EMPTY;
	//ET 20170511		boolean											searchForKeys			= false;
	final boolean								addToExistentKeys;
	final Vector<String>				objectKeys;

	private List<String>				obsoleteObjectKeys;

	/**
	 * constructor to create a object key scanner,
	 * starting this as thread all the sub files of the given data path are scanned for object key references
	 * and a file link will be created in a directory named with the object key.
	 * If newAddToExistentKeys is true: Provide obsolete object keys, i.e. without object directory or with empty directory.
	 * @param newAddToExistentKeys false will build a new object key list from scratch on without caring about any existing object keys
	 */
	public ObjectKeyScanner(boolean newAddToExistentKeys) {
		super("objectKeyScanner");
		this.addToExistentKeys = newAddToExistentKeys;
		this.objectKeys = new Vector<String>();
		if (DataExplorer.getInstance().getMenuToolBar() != null) {
			for (String tmpObjKey : DataExplorer.getInstance().getMenuToolBar().getObjectKeyList()) {
				this.objectKeys.add(tmpObjKey);
			}
			//this.setPriority(Thread.MIN_PRIORITY);
		}
	}

	/**
	 * constructor to create a object key scanner,
	 * starting this as thread all the sub files of the given data path are scanned for object key references
	 * and a file link will be created in a directory named with the object key
	 * @param newObjectKey the object key to be used for scanning existing files
	 */
	public ObjectKeyScanner(String newObjectKey) {
		super("objectKeyScanner");
		this.objectKey = newObjectKey;
		this.addToExistentKeys = false;
		this.objectKeys = new Vector<String>();
		if (DataExplorer.getInstance().getMenuToolBar() != null) {
			for (String tmpObjKey : DataExplorer.getInstance().getMenuToolBar().getObjectKeyList()) {
				this.objectKeys.add(tmpObjKey);
			}
			//this.setPriority(Thread.MIN_PRIORITY);
		}
	}

	@Override
	public void run() {
		try {
			String objectKeyDirPath = this.settings.getDataFilePath() + GDE.FILE_SEPARATOR_UNIX + this.objectKey;

			if (this.objectKey.length() > 1) { // use exact defined object key
				String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
				//check directory and cleanup if already exist
				FileUtils.checkDirectoryAndCreate(objectKeyDirPath);

				// check if object key known (settings)
				String[] objectList = this.settings.getObjectList();
				boolean isKnown = true;
				for (String objectName : objectList) {
					if (!objectName.equals(this.objectKey)) {
						isKnown = false;
						break;
					}
				}
				if (!isKnown && !this.objectKeys.contains(this.objectKey)) {
					this.objectKeys.add(this.objectKey);
				}

				//scan all data files for object key
				List<File> files = FileUtils.getFileListing(new File(this.settings.getDataFilePath()), 1);
				final int progressDistance = 50;
				double progressStep = (99. - this.application.getProgressPercentage()) / (files.size() + 1.) * progressDistance;
				int i = 0;
				for (File file : files) {
					try {
						String actualFilePath = file.getAbsolutePath().replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
						if (actualFilePath.endsWith(GDE.FILE_ENDING_OSD) && actualFilePath.equals(OperatingSystemHelper.getLinkContainedFilePath(actualFilePath))) {
							log.log(Level.FINER, "working with " + file.getName()); //$NON-NLS-1$
							if (this.objectKey.equals(OsdReaderWriter.getHeader(file.getCanonicalPath()).get(GDE.OBJECT_KEY))) {
								log.log(Level.FINER, "found file with given object key " + file.getName()); //$NON-NLS-1$
								String newLinkFilePath = objectKeyDirPath + GDE.FILE_SEPARATOR_UNIX + file.getName();
								if (!new File(newLinkFilePath).exists()) {
									OperatingSystemHelper.createFileLink(file.getCanonicalPath(), newLinkFilePath);
								}
							}
						}
						if (this.application.getMenuToolBar() != null && i % progressDistance == 0) this.application.setProgress((int) (i * progressStep), sThreadId);
						i++;
					}
					catch (IOException e) {
						log.log(Level.WARNING, file.getAbsolutePath(), e);
					}
					catch (NotSupportedFileFormatException e) {
						log.log(Level.WARNING, e.getLocalizedMessage(), e);
					}
					catch (Throwable t) {
						log.log(Level.WARNING, t.getLocalizedMessage(), t);
					}
				}
				if (this.application.getMenuToolBar() != null) this.application.setProgress(100, sThreadId);
			}
			else { // search for all available keys
				log.log(Level.WARNING, "object key not set, actual object key = \"" + this.objectKey + "\" !"); //$NON-NLS-1$ //$NON-NLS-2$
				String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
				final int progressPercentageLimit = 80;

				final File rootDirectory = new File(this.settings.getDataFilePath());
				log.log(Level.FINE, "this.settings.getDataFilePath() = " + rootDirectory.toString());
				this.objectKeys.clear();
				HashMap<String, Vector<File>> objectFilesMap = new HashMap<String, Vector<File>>();
				{
					int fileCounter = 0;
					//ET 20170511				if (this.searchForKeys) {
					List<File> files = FileUtils.getFileListing(rootDirectory, 1);
					final int progressDistance = 50;
					double progressStep = (99. - this.application.getProgressPercentage()) / (files.size() + 1.) * progressDistance;
					int i = 0;
					for (File file : files) {
						try {
							String actualFilePath = file.getAbsolutePath().replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
							if (actualFilePath.endsWith(GDE.FILE_ENDING_OSD)) {
								fileCounter++;
								if (actualFilePath.equals(OperatingSystemHelper.getLinkContainedFilePath(actualFilePath))) { // this is not a link
									log.log(Level.FINE, "working with " + file.getName()); //$NON-NLS-1$
									String foundObjectKey = OsdReaderWriter.getHeader(file.getCanonicalPath()).get(GDE.OBJECT_KEY);
									if (foundObjectKey != null && foundObjectKey.length() > 1) { // is a valid object key
										if (!this.objectKeys.contains(foundObjectKey)) {
											log.log(Level.FINE, "found new object key " + foundObjectKey); //$NON-NLS-1$
											this.objectKeys.add(foundObjectKey);
											Vector<File> tmpObjectFiles = new Vector<File>();
											tmpObjectFiles.add(file);
											objectFilesMap.put(foundObjectKey, tmpObjectFiles);
										}
										else {
											objectFilesMap.get(foundObjectKey).add(file);
										}
										log.log(Level.FINE, "add file " + file.getName() + " to object key " + foundObjectKey); //$NON-NLS-1$
									}
								}
							}
							if (this.application.getMenuToolBar() != null && i % progressDistance == 0) this.application.setProgress((int) (i * progressStep), sThreadId);
							i++;
						}
						catch (IOException e) {
							log.log(Level.WARNING, e.getLocalizedMessage(), e);
						}
						catch (NotSupportedFileFormatException e) {
							log.log(Level.WARNING, e.getLocalizedMessage(), e);
						}
						catch (Throwable t) {
							log.log(Level.WARNING, t.getLocalizedMessage(), t);
						}
					}
					log.log(Level.FINE, "scanned " + fileCounter + " files for object key , foundKeysSize=" + this.objectKeys.size()); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (this.application.getMenuToolBar() != null) this.application.setProgress(progressPercentageLimit, sThreadId);
				{ // createFileLinks: Take the object key list and create file links for all files assigned to object keys.
					int progressPercentageStart = this.application.getProgressPercentage();
					double progressStep = (100. - progressPercentageStart) / (this.objectKeys.size() + 1.);

					int j = 0;
					Iterator<String> iterator = this.objectKeys.iterator();
					//iterate all found object keys
					while (iterator.hasNext()) {
						String tmpObjKey = iterator.next();
						log.log(Level.FINE, "found object key in vector = " + tmpObjKey); //$NON-NLS-1$
						//iterate all files of temporary object key
						objectKeyDirPath = this.settings.getDataFilePath() + GDE.FILE_SEPARATOR_UNIX + tmpObjKey;
						FileUtils.checkDirectoryAndCreate(objectKeyDirPath);
						for (File file : objectFilesMap.get(tmpObjKey)) {
							try {
								String newLinkFilePath = objectKeyDirPath + GDE.FILE_SEPARATOR_UNIX + file.getName();
								if (!new File(newLinkFilePath).exists()) {
									OperatingSystemHelper.createFileLink(file.getCanonicalPath(), newLinkFilePath);
								}
							}
							catch (IOException e) {
								log.log(Level.WARNING, e.getLocalizedMessage(), e);
							}
						}
						j++;
						if (this.application.getMenuToolBar() != null) this.application.setProgress(progressPercentageStart + (int) (j * progressStep), sThreadId);
					}
				}
				if (this.addToExistentKeys) {
					Set<String> newObjectList = Settings.getInstance().getRealObjectKeys();
					if (newObjectList.addAll(this.objectKeys) || !newObjectList.equals(new HashSet<>(this.objectKeys))) {
						this.application.setObjectList(newObjectList.toArray(new String[0]), this.settings.getActiveObject());
						log.log(Level.FINE, "object list updated: ", newObjectList); //$NON-NLS-1$
					}

					this.obsoleteObjectKeys = getObsoleteObjectKeys(rootDirectory);
				}
				else {
					this.application.setObjectList(this.objectKeys.toArray(new String[0]), this.settings.getActiveObject());
				}
				if (this.application.getMenuToolBar() != null) this.application.setProgress(100, sThreadId);
			}
		}
		catch (FileNotFoundException e) {
			log.log(Level.WARNING, e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Build list of object keys without a directory or an empty directory based on the current user selection.
	 * @param rootDirectory
	 * @throws FileNotFoundException
	 */
	private List<String> getObsoleteObjectKeys(File rootDirectory) throws FileNotFoundException {
		// get current object key list as a basis for determining the obsolete object keys
		List<String> resultObjectKeys = new ArrayList<>(this.settings.getRealObjectKeys());
		resultObjectKeys.removeAll(this.settings.getObjectKeyCandidates());

		for (File dir : FileUtils.getDirectories(rootDirectory)) {
			if (!FileUtils.getFileListing(dir, Integer.MAX_VALUE).isEmpty()) {
				if (GDE.IS_WINDOWS) {
					// iterate due to windows which allows only one directory for object keys with casing differences
					ListIterator<String> iterator = resultObjectKeys.listIterator();
					while (iterator.hasNext()) {
						if (iterator.next().toLowerCase().equals(dir.getName().toLowerCase())) {
							iterator.remove();
							// break; leave this statement out to eliminate multiple casing versions of the name
						}
					}
				}
				else {
					resultObjectKeys.remove(dir.getName());
				}
			}
		}
		log.log(Level.INFO, "obsoleteObjectKeys: ", resultObjectKeys); //$NON-NLS-1$
		return resultObjectKeys;
	}

	//	/**
	//	 * @param newObjectKey the objectKey to set
	//	 */
	//	public void setObjectKey(String newObjectKey) {
	//		this.objectKey = newObjectKey;
	//	}
	//
	//	/**
	//	 * @param enable the searchForKeys to set
	//	 */
	//	public void setSearchForKeys(boolean enable) {
	//		this.searchForKeys = enable;
	//	}
	//
	/**
	 * @return object key list found during scan
	 */
	public String[] getObjectList() {
		return this.objectKeys.toArray(new String[0]);
	}

	/**
	 * deletes all file links under standard data directory
	 * this is the reverse operation of the run() method creating such file links
	 */
	public static void cleanFileLinks() {
		try {
			List<File> files = FileUtils.getFileListing(new File(Settings.getInstance().getDataFilePath()), 1);
			for (File file : files) {
				try {
					String actualFilePath = file.getAbsolutePath().replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
					if (actualFilePath.endsWith(GDE.FILE_ENDING_OSD) && !actualFilePath.equals(OperatingSystemHelper.getLinkContainedFilePath(actualFilePath))) {
						log.log(Level.FINE, "working with " + file.getName()); //$NON-NLS-1$
						if (!file.delete()) {
							log.log(Level.FINE, "could not delete " + file.getName()); //$NON-NLS-1$
						}
					}
				}
				catch (IOException e) {
					log.log(Level.WARNING, file.getAbsolutePath(), e);
				}
				catch (Throwable t) {
					log.log(Level.WARNING, t.getLocalizedMessage(), t);
				}
			}
		}
		catch (FileNotFoundException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}

	public List<String> getObsoleteObjectKeys() {
		return this.obsoleteObjectKeys;
	}
}
