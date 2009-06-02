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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.OSDE;
import osde.config.Settings;
import osde.exception.NotSupportedFileFormatException;
import osde.io.OsdReaderWriter;

/**
 * This thread implementation goes through all sub folders of the data location and creates links to object related files
 * @author Winfried Brügmann
 */
public class ObjectKeyScanner extends Thread {
	final private static Logger											log					= Logger.getLogger(ObjectKeyScanner.class.getName());

	final private Settings settings;
	final private String objectKey;
	
	/**
	 * constructor to create a object key scanner, 
	 * starting this as thread all the sub files of the given data path are scanned for object key references 
	 * and a file link will be created in a dircetory named with the object key
	 * @param newObjectKey the object key to be used for scanning existing files
	 */
	public ObjectKeyScanner(String newObjectKey) {
		super();
		this.objectKey = newObjectKey;
		this.settings = Settings.getInstance();
		this.setPriority(Thread.MIN_PRIORITY);
	}

	/**
	 * constructor to create a object key scanner, 
	 * starting this as thread all the sub files of the given data path are scanned for object key references 
	 * and a file link will be created in a dircetory named with the object key
	 * @param name name of the thread
	 * @param newObjectKey the object key to be used for scanning existing files
	 */
	public ObjectKeyScanner(String name, String newObjectKey) {
		super(name);
		this.objectKey = newObjectKey;
		this.settings = Settings.getInstance();
		this.setPriority(Thread.MIN_PRIORITY);
	}
	
	public void run() {
		try {
			String objectKeyDirPath = this.settings.getDataFilePath() + OSDE.FILE_SEPARATOR_UNIX + this.objectKey;
			
			//check directory and cleanup if already exist 
			//TODO enable update
			if (FileUtils.checkDirectoryAndCreate(objectKeyDirPath)) {
				FileUtils.deleteDirectory(objectKeyDirPath);
				FileUtils.checkDirectoryAndCreate(objectKeyDirPath);
			}
			
			//scan all data files for object key
			List<File> files = FileUtils.getFileListing(new File(this.settings.getDataFilePath()));
			for (File file : files) {
				if (file.getName().endsWith(OSDE.FILE_ENDING_OSD) && !containsKnownObjectKey(file.getPath())) {
					log.log(Level.INFO, "working with " + file.getName());
					try {
						if (this.objectKey.equals(OsdReaderWriter.getHeader(file.getCanonicalPath()).get(OSDE.OBJECT_KEY))) {
							log.log(Level.INFO, "found file with given object key " + file.getName());
							String newLinkFilePath = objectKeyDirPath + OSDE.FILE_SEPARATOR_UNIX + file.getName();
							if (!new File(newLinkFilePath).exists()) {
								OperatingSystemHelper.createFileLink(file.getCanonicalPath(), newLinkFilePath);
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
			}
		}
		catch (FileNotFoundException e) {
			log.log(Level.WARNING, e.getLocalizedMessage(), e);
		}
	}

	/**
	 * check if the actual object key list is part of the file path 
	 * @param fulleQualifiedFilePath
	 * @return
	 */
	private boolean containsKnownObjectKey(String fulleQualifiedFilePath) {
		boolean contains = false;
		for (String tmpObjKey : this.settings.getObjectList()) {
			if (fulleQualifiedFilePath.contains(tmpObjKey)) {
				contains = true;
				break;
			}
		}
		return contains;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ObjectKeyScanner objLnkCrt = new ObjectKeyScanner("ASW-27");
		objLnkCrt.start();
		
		ObjectKeyScanner objLnkCrt_ = new ObjectKeyScanner("FlugAkkuA_3200");
		objLnkCrt_.start();
	}

}
