/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.OSDE;
import osde.config.Settings;
import osde.exception.NotSupportedFileFormatException;
import osde.io.OsdReaderWriter;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;

/**
 * This thread implementation goes through all sub folders of the data location and creates links to object related files
 * @author Winfried Brügmann
 */
public class ObjectKeyScanner extends Thread {
	final private static Logger											log					= Logger.getLogger(ObjectKeyScanner.class.getName());

	final private Settings settings;
	final String deviceOriented;
	String objectKey = OSDE.STRING_EMPTY;
	boolean searchForKeys = false;
	final Vector<String> objectKeys;
	
	/**
	 * constructor to create a object key scanner, 
	 * starting this as thread all the sub files of the given data path are scanned for object key references 
	 * using this constructor the object key must be set before starting the scan thread
	 * and a file link will be created in a directory named with the object key
	 */
	public ObjectKeyScanner() {
		super();
		this.settings = Settings.getInstance();
		this.deviceOriented = Messages.getString(MessageIds.OSDE_MSGT0200).split(OSDE.STRING_SEMICOLON)[0];
		this.objectKeys = new Vector<String>();
		for (String tmpObjKey : OpenSerialDataExplorer.getInstance().getMenuToolBar().getObjectKeyList()) {
			this.objectKeys.add(tmpObjKey);
		}
		//this.setPriority(Thread.MIN_PRIORITY);
	}

	/**
	 * constructor to create a object key scanner, 
	 * starting this as thread all the sub files of the given data path are scanned for object key references 
	 * and a file link will be created in a directory named with the object key
	 * @param newObjectKey the object key to be used for scanning existing files
	 */
	public ObjectKeyScanner(String newObjectKey) {
		super();
		this.objectKey = newObjectKey;
		this.settings = Settings.getInstance();
		this.deviceOriented = Messages.getString(MessageIds.OSDE_MSGT0200).split(OSDE.STRING_SEMICOLON)[0];
		this.objectKeys = new Vector<String>();
		for (String tmpObjKey : OpenSerialDataExplorer.getInstance().getMenuToolBar().getObjectKeyList()) {
			this.objectKeys.add(tmpObjKey);
		}
		//this.setPriority(Thread.MIN_PRIORITY);
	}

	public void run() {
		try {
			String objectKeyDirPath = this.settings.getDataFilePath() + OSDE.FILE_SEPARATOR_UNIX + this.objectKey;

			if (this.objectKey.length() > 1) {
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
				if (!isKnown && this.objectKey != null && !this.objectKeys.contains(this.objectKey)) {
					this.objectKeys.add(this.objectKey);
				}
				
				//scan all data files for object key
				List<File> files = FileUtils.getFileListing(new File(this.settings.getDataFilePath()));
				for (File file : files) {
					try {
						String actualFilePath = file.getAbsolutePath();
						if (actualFilePath.endsWith(OSDE.FILE_ENDING_OSD) && actualFilePath.equals(OperatingSystemHelper.getLinkContainedFilePath(actualFilePath))) {
							log.log(Level.FINER, "working with " + file.getName()); //$NON-NLS-1$
							if (this.objectKey.equals(OsdReaderWriter.getHeader(file.getCanonicalPath()).get(OSDE.OBJECT_KEY))) {
								log.log(Level.FINER, "found file with given object key " + file.getName()); //$NON-NLS-1$
								String newLinkFilePath = objectKeyDirPath + OSDE.FILE_SEPARATOR_UNIX + file.getName();
								if (!new File(newLinkFilePath).exists()) {
									OperatingSystemHelper.createFileLink(file.getCanonicalPath(), newLinkFilePath);
								}
							}
						}
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
			}
			else {
				log.log(Level.WARNING, "object key not set, actual object key = \"" + this.objectKey + "\" !"); //$NON-NLS-1$ //$NON-NLS-2$

				this.objectKeys.clear();
				HashMap<String,Vector<File>> objectFilesMap = new HashMap<String,Vector<File>>();
				int fileCounter = 0;
				if (this.searchForKeys) {
					List<File> files = FileUtils.getFileListing(new File(this.settings.getDataFilePath()));
					for (File file : files) {
						try {
							String actualFilePath = file.getAbsolutePath();
							if (actualFilePath.endsWith(OSDE.FILE_ENDING_OSD)) {
								fileCounter++;
								if (actualFilePath.equals(OperatingSystemHelper.getLinkContainedFilePath(actualFilePath))) { // this is not a link
									log.log(Level.FINE, "working with " + file.getName()); //$NON-NLS-1$
									String foundObjectKey = OsdReaderWriter.getHeader(file.getCanonicalPath()).get(OSDE.OBJECT_KEY);
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
									}
								}
							}
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
					Iterator<String> iterator = this.objectKeys.iterator();
					log.log(Level.FINE, "\nscanned " + fileCounter + " files for object key , found following keys"); //$NON-NLS-1$ //$NON-NLS-2$
					//iterate all found object keys
					while (iterator.hasNext()) {
						String tmpObjKey = iterator.next();
						log.log(Level.FINE, "found object key in vector = " + tmpObjKey); //$NON-NLS-1$
						//iterate all files of temporary object key
						objectKeyDirPath = this.settings.getDataFilePath() + OSDE.FILE_SEPARATOR_UNIX + tmpObjKey;
						FileUtils.checkDirectoryAndCreate(objectKeyDirPath);
						for (File file : objectFilesMap.get(tmpObjKey)) {
							try {
								String newLinkFilePath = objectKeyDirPath + OSDE.FILE_SEPARATOR_UNIX + file.getName();
								if (!new File(newLinkFilePath).exists()) {
									OperatingSystemHelper.createFileLink(file.getCanonicalPath(), newLinkFilePath);
								}
							}
							catch (IOException e) {
								log.log(Level.WARNING, e.getLocalizedMessage(), e);
							}
						}
					}
					OpenSerialDataExplorer.getInstance().setObjectList(this.objectKeys.toArray(new String[0]), this.settings.getActiveObject());
				}
			}
		}
		catch (FileNotFoundException e) {
			log.log(Level.WARNING, e.getLocalizedMessage(), e);
		}
	}

	/**
	 * @param newObjectKey the objectKey to set
	 */
	public void setObjectKey(String newObjectKey) {
		this.objectKey = newObjectKey;
	}

	/**
	 * @param enable the searchForKeys to set
	 */
	public void setSearchForKeys(boolean enable) {
		this.searchForKeys = enable;
	}

	/**
	 * 
	 * @return object key list found during scan
	 */
	public String[] getObjectList() {
		return this.objectKeys.toArray(new String[0]);
	}
}
