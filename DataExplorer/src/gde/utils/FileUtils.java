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
    
    Copyright (c) 2008,2009,2010,2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.utils;

import gde.GDE;
import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.ApplicationConfigurationException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.dialog.edit.DevicePropertiesEditor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * Utility class with helpers around file and directory handling
 * @author Winfried Brügmann
 */
public class FileUtils {
	private static final Logger				log							= Logger.getLogger(FileUtils.class.getName());
	public final static List<String>	onExitRenameJar	= new ArrayList<String>();

	/**
	 * copy from to file
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	public static void copyFile(File in, File out) throws IOException {
		FileChannel inChannel = new FileInputStream(in).getChannel();
		FileChannel outChannel = new FileOutputStream(out).getChannel();
		try {
			inChannel.transferTo(0, inChannel.size(), outChannel);
		}
		catch (IOException e) {
			throw e;
		}
		finally {
			if (inChannel != null) inChannel.close();
			if (outChannel != null) outChannel.close();
		}
	}

	/**
	 * copy all files from source directory to target directory 
	 * @param srcDir
	 * @param tgtDir
	 * @throws IOException
	 * @throws ApplicationConfigurationException
	 */
	public static void copyAllFiles(String srcDir, String tgtDir) throws IOException, ApplicationConfigurationException {
		File sourceDir = new File(srcDir);
		String[] files = sourceDir.list();
		if (files == null) {
			throw new ApplicationConfigurationException(Messages.getString(MessageIds.GDE_MSGE0017, new Object[] { srcDir }));
		}
		for (String srcFile : files) {
			if (GDE.IS_WINDOWS) {
				srcFile = srcFile.replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK); //$NON-NLS-1$ //$NON-NLS-2$
			}
			File src = new File(srcDir + GDE.FILE_SEPARATOR_UNIX + srcFile);
			if (!src.isDirectory()) {
				File tgt = new File(tgtDir + GDE.FILE_SEPARATOR_UNIX + srcFile);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "copy " + src.toString() + " to " + tgt.toString()); //$NON-NLS-1$ //$NON-NLS-2$
				FileUtils.copyFile(src, tgt);
			}
		}
	}

	/**
	 * check if directory exist and create if required (not exist)
	 * @param directory
	 * @return false if directory needs to be created
	 */
	public static boolean checkDirectoryAndCreate(String directory) {
		boolean exist = true;
		File dir = new File(directory);
		if (!dir.exists() && !dir.isDirectory()) {
			exist = false;
			if(!dir.mkdirs())
				log.log(Level.WARNING, "failed to create " + directory);
		}
		return exist;
	}

	/**
	 * check existence of a directory and file version and create if not exist, backup if version does not match
	 * @param directory
	 * @param versionFileName string qualifier "_V01" checks for file *_V01.* 
	 * @return true false if directory needs to be created
	 */
	public static boolean checkDirectoryAndCreate(String directory, String versionFileName) {
		boolean exist = true;
		int version = Integer.parseInt(versionFileName.substring(versionFileName.length()-6, versionFileName.length()-4));
		File dir = new File(directory);
		//initial case Device directory does ot exist
		if (!dir.exists() && !dir.isDirectory()) {
			exist = false;
			if(!dir.mkdir())
				log.log(Level.WARNING, "failed to create " + directory);
		}
		else {
			File file = new File(directory + GDE.FILE_SEPARATOR_UNIX + versionFileName);
			if (!file.exists()) {
				try {
					//find directory contained version file and rename directory, do not use actual version -1 to enable support for several installed GDE versions
					for(File tmpFile : FileUtils.getFileListingNoSort(dir, 3)) {
						if(tmpFile.getPath().endsWith(GDE.FILE_ENDING_DOT_XSD)) {
							int oldVersion = Integer.parseInt(tmpFile.getPath().substring(tmpFile.getPath().length()-6, tmpFile.getPath().length()-4));
							if(!dir.renameTo(new File(directory + "_V" + oldVersion))) {
								log.log(Level.WARNING, "failed to rename " + directory);
							}
						}
					}
					//check if a directory ending with the actual version already exist to re-use if legacy GDE was started before and a newer version has renamed 
					File versionDir = new File(directory + "_V" + version);
					if (versionDir.exists()) {
						if(!versionDir.renameTo(new File(directory))) {
							log.log(Level.WARNING, "failed to rename " + directory);
						}
					}
					else {
						exist = false;
						File newDir = new File(directory);
						if (!newDir.mkdir())
							log.log(Level.WARNING, "failed to create " + directory);						
					}
				}
				catch (Exception e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
		return exist;
	}

	/**
	 * check existence of a directory and delete underlying files as well as the directory
	 * attention, this runs for structure up to 5 directory levels only
	 * @param fullQualifiedDirectoryPath
	 * @return true false if directory needs to be created
	 */
	public static boolean deleteDirectory(String fullQualifiedDirectoryPath) {
		boolean exist = false;
		File dir = new File(fullQualifiedDirectoryPath);
		if (dir.exists() && dir.isDirectory() && dir.canWrite()) {
			exist = true;
			
			try {
				for (File file : FileUtils.getFileListing(dir, 5)) {
					if (file.canWrite()) {
						log.log(Level.FINE, file.getAbsolutePath() + " deletion " + file.delete());
					}
					else {
						log.log(Level.WARNING, "no delete permission on " + file.getAbsolutePath()); //$NON-NLS-1$
					}
				}
				for (File directory : FileUtils.getDirListing(dir)) {
					if (!(directory.canWrite() && deleteDirectory(directory.getAbsolutePath()))) {
						log.log(Level.WARNING, "no delete permission on " + directory.getAbsolutePath()); //$NON-NLS-1$
					}
				}
				log.log(Level.WARNING, dir.getAbsolutePath() + " deletion " + dir.delete());
			}
			catch (Exception e) {
				if (e instanceof FileNotFoundException) {
					log.log(Level.WARNING, e.getMessage(), e);
				}
				else {
					log.log(Level.SEVERE, dir.getAbsolutePath(), e);
				}
			}
		}
		else {
			log.log(Level.WARNING, "no delete permission on " + dir.getAbsolutePath()); //$NON-NLS-1$
		}
		return exist;
	}	
	
	/**
	 * Deletes a file
	 * @param filename the file name to delete
	 */
	public static void deleteFile(String filename) {

		if (! FileUtils.checkFileExist(filename)) {
			return;
		}
		
		File file = new File(filename);
		try {
			if (! file.canWrite()) {
				log.log(Level.WARNING, "no delete permission on " + filename);
			}
			log.log(Level.WARNING, "deleting " + filename);
			file.delete();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, filename, e);
		}
	}
	
	/**
	 * check if a file exist, the file path given must fully qualified
	 * @param fullQualifiedFileName
	 * @return true if file exist
	 */
	public static boolean checkFileExist(final String fullQualifiedFileName) {
		File file = new File(fullQualifiedFileName);
		return file.exists() && !file.isDirectory();
	}
	
	/**
	 * check if a directory exist, the file path given must fully qualified
	 * @param fullQualifiedFileName
	 * @return true if file exist
	 */
	public static boolean checkDirectoryExist(final String fullQualifiedFileName) {
		File file = new File(fullQualifiedFileName);
		return file.exists() && file.isDirectory();
	}
	
	/**
	 * rename a file to given file extension
	 *@return newFilePath
	 */
	public static String renameFile(String filePath, String extension) {
		String resultFilePath = filePath;
		if (checkFileExist(filePath)) {
			File file = new File(filePath);
			if (file.canWrite()) {
				File newFile = new File(filePath.substring(0, filePath.lastIndexOf(GDE.STRING_DOT)+1) + extension);
				if (newFile.exists()) {
					if(!newFile.delete())
						log.log(Level.WARNING, "failed to delete " + filePath);
				}
				if(!file.renameTo(newFile))
					log.log(Level.WARNING, "failed to rename " + filePath.substring(0, filePath.lastIndexOf(GDE.STRING_DOT)+1) + extension);
				resultFilePath = filePath.substring(0, filePath.lastIndexOf(GDE.STRING_DOT)+1) + extension;
			}
			else {
				log.log(Level.WARNING, "no write permission on " + file.getAbsolutePath()); //$NON-NLS-1$
			}
		}
		return resultFilePath;
	}

	/**
	 * delete a file if exist
	 */
	public static void cleanFile(String fullQualifiedFilePath) {
	    
		if (FileUtils.checkFileExist(fullQualifiedFilePath) || FileUtils.checkDirectoryExist(fullQualifiedFilePath)) {
			File fileToBeDeleted = new File(fullQualifiedFilePath);
			if (!fileToBeDeleted.isDirectory() && fileToBeDeleted.canWrite()) {
				if (!fileToBeDeleted.delete())
					log.log(Level.WARNING, "failed to delete " + fullQualifiedFilePath);
			}
			else if (fileToBeDeleted.isDirectory() && fileToBeDeleted.canWrite()) {
				FileUtils.deleteDirectory(fileToBeDeleted.getAbsolutePath());
			}
			else
				log.log(Level.WARNING, fileToBeDeleted.getAbsolutePath() + " is a directory or no delete permission !"); //$NON-NLS-1$
		}
	}

	/**
	 * delete a file list, if exist
	 * attention, only 4 directory levels gets visited !
	 */
	public static void cleanFiles(String fileBasePath, String[] fileNames) {
		fileBasePath = fileBasePath.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
		Vector<String> fileNamesWildCard = new Vector<String>();
		for (String fileName : fileNames) {
			if (fileName.length() >= 2 && !fileName.contains(GDE.STRING_STAR)) { // "a.csv" "GDE", "OSDE"
				FileUtils.cleanFile(fileBasePath + fileName);
			}
			else {
				fileNamesWildCard.add(fileName);
			}
		}
		if (fileNamesWildCard.size() > 0) {
			for (String fileName : fileNamesWildCard) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "fileName = '" +fileName + "'");
				try {
					String startSignature = fileName.substring(0, fileName.indexOf(GDE.STRING_STAR));
					String endingSignature = fileName.substring(fileName.lastIndexOf(GDE.STRING_STAR) + 1);
					List<File> fileList = FileUtils.getFileListingNoSort(new File(fileBasePath), 0); //no directories to be scanned here
					for (File file : fileList) {
						String tmpFileName = file.getName();
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "evaluating " + tmpFileName); //$NON-NLS-1$
						if ((startSignature.length() == 0 && endingSignature.length() != 0 && tmpFileName.endsWith(endingSignature)) // "*register.sh"
								|| (startSignature.length() != 0 && tmpFileName.startsWith(startSignature) && endingSignature.length() == 0) // "bootstrap.log*"
								|| (startSignature.length() != 0 && tmpFileName.startsWith(startSignature) && endingSignature.length() != 0 && tmpFileName.endsWith(endingSignature))) { // "swt*448.dll"
							if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "deleting " + tmpFileName); //$NON-NLS-1$
							FileUtils.cleanFile(fileBasePath + tmpFileName);
						}
					}
				}
				catch (Exception e) {
					log.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}
	
	/**
	 * extract a file from source jar file to target file while replace a given placeholder key with a replacement
	 * supported character set encoding :
	 * US-ASCII 	Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the Unicode character set
	 * ISO-8859-1   	ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1
	 * UTF-8 	Eight-bit UCS Transformation Format
	 * UTF-16BE 	Sixteen-bit UCS Transformation Format, big-endian byte order
	 * UTF-16LE 	Sixteen-bit UCS Transformation Format, little-endian byte order
	 * UTF-16 	Sixteen-bit UCS Transformation Format, byte order identified by an optional byte-order mark
	 * @param placeholderKey
	 * @param replacement
	 * @param jarFilePath
	 * @param jarInternalFilePath
	 * @param targetFilePath
	 * @param sourceEncoding "UTF-8", "ISO-8859-1"
	 * @param targetEncoding "UTF-8", "ISO-8859-1"
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 */
	public static void extractWhileReplace(String placeholderKey, String replacement, String jarFilePath, String jarInternalFilePath, String targetFilePath, String sourceEncoding, String targetEncoding) throws IOException, UnsupportedEncodingException,
			FileNotFoundException {
		BufferedReader reader;
		BufferedWriter writer;
		String line;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "jarFilePath = " + jarFilePath); //$NON-NLS-1$
		JarFile jarFile = new JarFile(jarFilePath);

		reader = new BufferedReader(new InputStreamReader(FileUtils.getFileInputStream(jarFile, jarInternalFilePath), sourceEncoding)); 
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "targetPath = " + targetFilePath); //$NON-NLS-1$
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFilePath), targetEncoding)); //$NON-NLS-1$

		while ((line = reader.readLine()) != null) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, line);
			while (line.indexOf(placeholderKey) > -1) {
				StringBuilder sb = new StringBuilder();
				sb.append(line.substring(0, line.indexOf(placeholderKey)));
				sb.append(replacement);
				sb.append(line.substring(line.indexOf(placeholderKey) + placeholderKey.length()));
				line = sb.toString();
			}
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, line);
			writer.write(line+GDE.LINE_SEPARATOR);
		}
		reader.close();
		writer.flush();
		writer.close();
	}

	/**
	 * copy a file from source to target while replace a given placeholder key with a replacement
	 * @param placeHolderKey
	 * @param replacement
	 * @param sourceFilePath
	 * @param targetFilePath
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void copyFileWhileReplaceKey(String placeHolderKey, String replacement, String sourceFilePath, String targetFilePath) throws UnsupportedEncodingException,
			FileNotFoundException, IOException {
		BufferedReader reader;
		BufferedWriter writer;
		String line;
		reader = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFilePath), GDE.STRING_UTF_8)); 
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFilePath), GDE.STRING_UTF_8)); 
		while ((line = reader.readLine()) != null) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, line);
			if (line.indexOf(placeHolderKey) > -1) {
				StringBuilder sb = new StringBuilder();
					sb.append(line.substring(0, line.indexOf(placeHolderKey)));
					sb.append(replacement);
					sb.append(line.substring(line.indexOf(placeHolderKey)+placeHolderKey.length()));
					line = sb.toString();
			}
			
			line = line.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX).replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK)  + GDE.LINE_SEPARATOR;
			
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, line);
			writer.write(line);
		}
		reader.close();
		writer.flush();
		writer.close();
	}

	/**
	 * extract a jar internal file using the runtime class instance
	 * @param runtimeInstance
	 * @param fileName
	 * @param jarInternalSourceDirectory
	 * @param targetDirectory
	 * @param permissionsUNIX
	 */
	public static boolean extract(Class<?> runtimeInstance, String fileName, String jarInternalSourceDirectory, String targetDirectory, String permissionsUNIX) {
		boolean isExtracted = false;
		// normalize input directorys
		jarInternalSourceDirectory = jarInternalSourceDirectory.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
		jarInternalSourceDirectory = jarInternalSourceDirectory.endsWith(GDE.FILE_SEPARATOR_UNIX) ? jarInternalSourceDirectory : jarInternalSourceDirectory + GDE.FILE_SEPARATOR_UNIX;
		targetDirectory = targetDirectory.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
		targetDirectory = targetDirectory.endsWith(GDE.FILE_SEPARATOR_UNIX) ? targetDirectory : targetDirectory + GDE.FILE_SEPARATOR_UNIX; 

		FileOutputStream os = null;
		InputStream is = null;
		File file = new File(targetDirectory + fileName); 
		try {
			if (!file.exists ()) {
				is = runtimeInstance.getClassLoader().getResourceAsStream (jarInternalSourceDirectory + fileName); 
				if (is != null) {
					int read;
					byte [] buffer = new byte [4096];
					os = new FileOutputStream (targetDirectory + fileName);
					while ((read = is.read (buffer)) != -1) {
						os.write(buffer, 0, read);
					}
					os.close ();
					is.close ();
					setAccessPermission(fileName, permissionsUNIX);
					
					isExtracted = true;
				}
			}
		} 
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			try {
				if (os != null) os.close ();
			} catch (IOException e1) {
				//ignore
			}
			try {
				if (is != null) is.close ();
			} catch (IOException e1) {
				//ignore
			}
		}
		return isExtracted;
	}

	/**
	 * get the input stream of a file from a given jar archive
	 * @param jarFile
	 * @param jarInternalFilePath
	 * @throws IOException 
	 */
	public static InputStream getFileInputStream(JarFile jarFile, String jarInternalFilePath) throws IOException {
		// normalize input file path
		jarInternalFilePath = jarInternalFilePath.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
		
		ZipEntry ze = jarFile.getEntry(jarInternalFilePath);

		InputStream is = jarFile.getInputStream(ze);
		
		return is;
	}
	
	
	/**
	 * extract a file from a Jar archive and rename
	 * @param runtimeInstance
	 * @param sourceFileName
	 * @param targetFileName
	 * @param jarInternalSourceDirectory
	 * @param targetDirectory
	 * @param unixPermissions
	 */
	public static boolean extract(Class<?> runtimeInstance, String sourceFileName, String targetFileName, String jarInternalSourceDirectory, String targetDirectory, String unixPermissions) {
		boolean isRenamed = false;
		jarInternalSourceDirectory = jarInternalSourceDirectory.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
		jarInternalSourceDirectory = jarInternalSourceDirectory.endsWith(GDE.FILE_SEPARATOR_UNIX) ? jarInternalSourceDirectory : jarInternalSourceDirectory.length() > 1 ? jarInternalSourceDirectory + GDE.FILE_SEPARATOR_UNIX : jarInternalSourceDirectory;
		targetDirectory = targetDirectory.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
		targetDirectory = targetDirectory.endsWith(GDE.FILE_SEPARATOR_UNIX) ? targetDirectory : targetDirectory.length() > 1 ? targetDirectory + GDE.FILE_SEPARATOR_UNIX : targetDirectory;
		FileUtils.extract(runtimeInstance, sourceFileName, jarInternalSourceDirectory, targetDirectory, unixPermissions);
		File sourceFile = new File(targetDirectory + sourceFileName);
		if(new File(targetDirectory + targetFileName).exists()) {
			log.log(Level.WARNING, targetDirectory + targetFileName);
		}
		if (sourceFile.exists() && sourceFile.canWrite()) {
			isRenamed = sourceFile.renameTo(new File(targetDirectory + targetFileName));
			if (!isRenamed) {
				log.log(Level.WARNING, "renaming to " + targetDirectory + targetFileName + " failed !"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return isRenamed;
	}

			
	/**
	 * extract a file from a Jar archive
	 * @param jarFile
	 * @param fileName
	 * @param jarSourceDirectory
	 * @param targetDirectory
	 * @param unixPermissions
	 * @throws IOException
	 */
	public static void extract(JarFile jarFile, String fileName, String jarSourceDirectory, String targetDirectory, String unixPermissions) {
		// normalize input directorys
		fileName = fileName.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
		fileName = fileName.startsWith(GDE.FILE_SEPARATOR_UNIX) ? fileName.substring(1) : fileName;
		jarSourceDirectory = jarSourceDirectory.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
		jarSourceDirectory = jarSourceDirectory.endsWith(GDE.FILE_SEPARATOR_UNIX) ? jarSourceDirectory : jarSourceDirectory.length() > 1 ? jarSourceDirectory + GDE.FILE_SEPARATOR_UNIX : jarSourceDirectory;
		targetDirectory = targetDirectory.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
		targetDirectory = targetDirectory.endsWith(GDE.FILE_SEPARATOR_UNIX) ? targetDirectory : targetDirectory.length() > 1 ? targetDirectory + GDE.FILE_SEPARATOR_UNIX : targetDirectory;
		
		ZipEntry ze = jarFile.getEntry(jarSourceDirectory + fileName);

		int read;
		byte[] buffer = new byte[4096];
		InputStream is = null;
		FileOutputStream os = null;
		try {
			if (ze != null) {
				is = jarFile.getInputStream(ze);
				os = new FileOutputStream(targetDirectory + fileName);
				while ((read = is.read(buffer)) != -1) {
					os.write(buffer, 0, read);
				}
				os.close();
				is.close();
				setAccessPermission(fileName, unixPermissions);
			}
			else { 
				log.log(Level.WARNING, jarSourceDirectory + fileName + " does not exist!"); //$NON-NLS-1$
			}
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			try {
				if (os != null) os.close();
			}
			catch (IOException e1) {
				//ignore
			}
			try {
				if (is != null) is.close();
			}
			catch (IOException e1) {
				//ignore
			}
		}
	}

	/**
	 * extract directory from a Jar archive
	 * @param jarFile
	 * @param jarInternalSourceDirectory
	 * @param targetDirectory
	 * @param permissionsUNIX
	 */
	public static void extractDir(JarFile jarFile, String jarInternalSourceDirectory, String targetDirectory, String permissionsUNIX) {
		// normalize input directorys
		jarInternalSourceDirectory = jarInternalSourceDirectory.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
		jarInternalSourceDirectory = jarInternalSourceDirectory.endsWith(GDE.FILE_SEPARATOR_UNIX) ? jarInternalSourceDirectory : jarInternalSourceDirectory + GDE.FILE_SEPARATOR_UNIX;
		targetDirectory = targetDirectory.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
		targetDirectory = targetDirectory.endsWith(GDE.FILE_SEPARATOR_UNIX) ? targetDirectory : targetDirectory + GDE.FILE_SEPARATOR_UNIX;
		
		int read;
		byte[] buffer = new byte[4096];
		InputStream is = null;
		FileOutputStream os = null;
		
		FileUtils.checkDirectoryAndCreate(targetDirectory + jarInternalSourceDirectory);

		Enumeration<JarEntry> enties = jarFile.entries();
		while (enties.hasMoreElements()) {
			JarEntry jarEntry = enties.nextElement();
			String entryName = jarEntry.getName();
			if ((entryName.startsWith(jarInternalSourceDirectory) || entryName.endsWith(".css")) && entryName.contains(GDE.STRING_DOT) && !FileUtils.checkFileExist(targetDirectory + entryName)) { //$NON-NLS-1$ //$NON-NLS-2$
				ZipEntry ze = jarFile.getEntry(entryName);
				FileUtils.checkDirectoryAndCreate(targetDirectory + entryName.substring(0, entryName.lastIndexOf(GDE.FILE_SEPARATOR_UNIX)));
				try {
					is = jarFile.getInputStream(ze);
					os = new FileOutputStream(targetDirectory + entryName);
					while ((read = is.read(buffer)) != -1) {
						os.write(buffer, 0, read);
					}
					os.close();
					is.close();
					setAccessPermission(entryName, permissionsUNIX);
				}
				catch (Throwable e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					try {
						if (os != null) os.close();
					}
					catch (IOException e1) {
						//ignore
					}
					try {
						if (is != null) is.close();
					}
					catch (IOException e1) {
						//ignore
					}
				}
			}
		}
	}

	/**
	 * update an device image file within a device plug-in jar
	 * @param deviceConfig
	 * @param imageFileName
	 * @param deviceImage
	 */
	public static void updateImageInDeviceJar(DeviceConfiguration deviceConfig, String imageFileName, Image deviceImage) {
		try {
			boolean isStartedWithinEclipse = DevicePropertiesEditor.class.getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(GDE.FILE_SEPARATOR_UNIX);
			if (isStartedWithinEclipse) {
				log.log(Level.INFO, "started within Eclipse"); //$NON-NLS-1$
				String fullQualifiedImageTargetName = findDeviceProjectDirectoryPath(deviceConfig) + "/src/resource/" + imageFileName; //$NON-NLS-1$
				log.log(Level.INFO, "fullQualifiedImageTargetName = " + fullQualifiedImageTargetName); //$NON-NLS-1$
				ImageLoader imageLoader = new ImageLoader();
				imageLoader.data = new ImageData[] { deviceImage.getImageData() };
				try {
					imageLoader.save(new FileOutputStream(fullQualifiedImageTargetName), SWT.IMAGE_JPEG);
				}
				catch (IOException e) {
					log.log(Level.WARNING, e.getMessage(), e);
				}
			}
			else 
			{
				if (DataExplorer.application != null) { // started within GDE
					log.log(Level.INFO, "started within DataExplorer"); //$NON-NLS-1$
				}
				else { // started outside GDE
					log.log(Level.INFO, "started outside DataExplorer"); //$NON-NLS-1$
					try {
						Thread.currentThread().setContextClassLoader(GDE.getClassLoader());
					}
					catch (Throwable e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
				}
				String addJarEntryName = "resource/" + imageFileName; //$NON-NLS-1$
				String deviceJarPath = getJarFileNameOfDevice(deviceConfig);

				// remove comment to test within eclipse, comment out the isStartedWithinEclipe block above
//				if (isStartedWithinEclipse) {
//					deviceJarPath = "c:\\Program Files\\DataExplorer\\devices\\Simulator.jar";
//				}

				String tmpDeviceJarPath = deviceJarPath.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
				tmpDeviceJarPath = GDE.JAVA_IO_TMPDIR.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX) + tmpDeviceJarPath.substring(tmpDeviceJarPath.lastIndexOf(GDE.FILE_SEPARATOR_UNIX)+1, tmpDeviceJarPath.length());
				log.log(Level.WARNING, "deviceJarPath = " + deviceJarPath + "; tmpDeviceJarPath = " + tmpDeviceJarPath); //$NON-NLS-1$ //$NON-NLS-2$

				FileUtils.updateJarContent(deviceJarPath, tmpDeviceJarPath, addJarEntryName, deviceImage, DevicePropertiesEditor.dialogShell);
			}
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * updates an jar file with additional image content
	 * @param deviceJarPath
	 * @param tmpDeviceJarPath
	 * @param addJarEntryName
	 * @param deviceImage
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void updateJarContent(String deviceJarPath, String tmpDeviceJarPath, String addJarEntryName, Image deviceImage, Shell messageBoxShell) throws IOException, FileNotFoundException {
		if (FileUtils.checkFileExist(tmpDeviceJarPath)) {
			MessageBox mBox = new MessageBox(messageBoxShell, SWT.PRIMARY_MODAL | SWT.YES | SWT.NO | SWT.CANCEL	| SWT.ICON_QUESTION);
			mBox.setText(GDE.NAME_LONG);
			mBox.setMessage(Messages.getString(MessageIds.GDE_MSGI0043, new String[] {tmpDeviceJarPath}));
			int ret = mBox.open();
			if (SWT.CANCEL == ret)
				return;
			else if (SWT.NO == ret) {
				FileUtils.renameFile(tmpDeviceJarPath, GDE.FILE_ENDING_BAK);
				deviceJarPath = tmpDeviceJarPath.substring(0, tmpDeviceJarPath.lastIndexOf(GDE.STRING_DOT)+1) + GDE.FILE_ENDING_BAK;
			}
		}
		
		JarInputStream in = new JarInputStream(new FileInputStream(deviceJarPath));
		JarOutputStream out = new JarOutputStream(new FileOutputStream(new File(tmpDeviceJarPath)), new JarFile(deviceJarPath).getManifest());

		JarEntry inEntry;
		byte[] buf = new byte[1024];
		int len = 0;

		//copy content to tmpDeviceJarPath
		while ((inEntry = in.getNextJarEntry()) != null) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "inEntry = " + inEntry.getName()); //$NON-NLS-1$
			if (!inEntry.getName().equalsIgnoreCase(addJarEntryName) && !inEntry.getName().endsWith("MANIFEST.MF")) { //$NON-NLS-1$
				out.putNextEntry(new JarEntry(inEntry));
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				out.closeEntry();
			}
			in.closeEntry();
		}
		in.close();

		//add new entry as image file
		JarEntry jarAddEntry = new JarEntry(addJarEntryName);
		out.putNextEntry(jarAddEntry);
		ImageLoader imageLoader = new ImageLoader();
		imageLoader.data = new ImageData[] { deviceImage.getImageData() };
		imageLoader.save(out, SWT.IMAGE_JPEG);
		out.closeEntry();
		out.close();

		File tmpFile = new File(tmpDeviceJarPath);
		if (tmpFile.exists()) {
			MessageBox mBox = new MessageBox(messageBoxShell, SWT.OK);
			mBox.setText(GDE.NAME_LONG);
			mBox.setMessage(Messages.getString(MessageIds.GDE_MSGI0044, new String[] {tmpDeviceJarPath})); 
			mBox.open();
		}
	}

	/**
	 * update an device properties file within a device plug-in jar
	 * @param deviceConfig
	 * @param devicePropsFileName
	 */
	public static void updateFileInDeviceJar(DeviceConfiguration deviceConfig, String devicePropsFileName) {
		String deviceJarPath = null;
		String devicePropsFileNameResource = devicePropsFileName = devicePropsFileName.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
		if (devicePropsFileNameResource.contains(GDE.FILE_SEPARATOR_UNIX)) { //seams full qualified, strip directory
			devicePropsFileNameResource = devicePropsFileNameResource.substring(devicePropsFileNameResource.lastIndexOf(GDE.FILE_SEPARATOR_UNIX), devicePropsFileNameResource.length());
		}

		try {
			boolean isStartedWithinEclipse = DevicePropertiesEditor.class.getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(GDE.FILE_SEPARATOR_UNIX);
			if (isStartedWithinEclipse) {
				log.log(Level.INFO, "started within Eclipse"); //$NON-NLS-1$
				String fullQualifiedPropertiesTargetFileName = findDeviceProjectDirectoryPath(deviceConfig)
						+ "/src/resource/" + Settings.getInstance().getLocale() + GDE.FILE_SEPARATOR_UNIX + devicePropsFileName; //$NON-NLS-1$
				log.log(Level.INFO, "fullQualifiedImageTargetFileName = " + fullQualifiedPropertiesTargetFileName); //$NON-NLS-1$
				deviceConfig.storeDeviceProperties(fullQualifiedPropertiesTargetFileName);
			}
			else 
			{
				if (DataExplorer.application != null) { // started within GDE
					log.log(Level.INFO, "started within DataExplorer"); //$NON-NLS-1$
				}
				else { // started outside GDE
					log.log(Level.INFO, "started outside DataExplorer"); //$NON-NLS-1$
					try {
						Thread.currentThread().setContextClassLoader(GDE.getClassLoader());
					}
					catch (Throwable e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
				}
				String addJarEntryName = "resource/" + Settings.getInstance().getLocale() + devicePropsFileNameResource; //$NON-NLS-1$
				String tmpDeviceJarPath = null;
				deviceJarPath = getJarFileNameOfDevice(deviceConfig);

				tmpDeviceJarPath = GDE.JAVA_IO_TMPDIR + deviceJarPath.substring(deviceJarPath.lastIndexOf(GDE.FILE_SEPARATOR_UNIX) + 1, deviceJarPath.length());
				log.log(Level.WARNING, "deviceJarPath = " + deviceJarPath + "; tmpDeviceJarPath = " + tmpDeviceJarPath); //$NON-NLS-1$ //$NON-NLS-2$

				FileUtils.updateJarContent(deviceJarPath, tmpDeviceJarPath, addJarEntryName, devicePropsFileName, DevicePropertiesEditor.dialogShell);
			}
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * find project directory of current device while application is loaded within eclipse
	 * @param deviceConfig
	 * @return device project directory path
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws ApplicationConfigurationException
	 * @throws ClassNotFoundException
	 */
	public static String findDeviceProjectDirectoryPath(DeviceConfiguration deviceConfig) throws MalformedURLException, URISyntaxException, ApplicationConfigurationException,
			ClassNotFoundException {
		String deviceImplName = deviceConfig.getDeviceImplName().replace(GDE.STRING_BLANK, GDE.STRING_EMPTY).replace(GDE.STRING_DASH, GDE.STRING_EMPTY);
		String className = deviceImplName.contains(GDE.STRING_DOT) ? deviceImplName  // full qualified
				: "gde.device." + deviceConfig.getManufacturer().toLowerCase().replace(GDE.STRING_BLANK, GDE.STRING_EMPTY).replace(GDE.STRING_DASH, GDE.STRING_EMPTY) + GDE.STRING_DOT + deviceImplName; //$NON-NLS-1$ //$NON-NLS-2$
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "loading Class " + className); //$NON-NLS-1$
		Thread.currentThread().setContextClassLoader(GDE.getClassLoader());
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Class<?> c = loader.loadClass(className);
		String basPath = c.getProtectionDomain().getCodeSource().getLocation().getPath();
		return basPath.substring(0, basPath.indexOf("bin")-1);
	}

	/**
	 * find the device jar name, where the device corresponding to the given device configuration
	 * @param deviceConfig
	 * @return deviceJarPath
	 * @throws ClassNotFoundException
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoClassDefFoundError
	 */
	public static String getJarFileNameOfDevice(DeviceConfiguration deviceConfig) throws ClassNotFoundException, NoSuchMethodException,
			InstantiationException, IllegalAccessException, InvocationTargetException, NoClassDefFoundError {
		String deviceJarFileName = null;
		String deviceImplName = deviceConfig.getDeviceImplName().replace(GDE.STRING_BLANK, GDE.STRING_EMPTY).replace(GDE.STRING_DASH, GDE.STRING_EMPTY);
		IDevice newInst = null;
		String className = deviceImplName.contains(GDE.STRING_DOT) ? deviceImplName  // full qualified
				: "gde.device." + deviceConfig.getManufacturer().toLowerCase().replace(GDE.STRING_BLANK, GDE.STRING_EMPTY).replace(GDE.STRING_DASH, GDE.STRING_EMPTY) + GDE.STRING_DOT + deviceImplName; //$NON-NLS-1$ //$NON-NLS-2$
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "loading Class " + className); //$NON-NLS-1$
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Class<?> c = loader.loadClass(className);
		Constructor<?> constructor = c.getDeclaredConstructor(new Class[] { DeviceConfiguration.class });
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "constructor != null -> " + (constructor != null ? "true" : "false")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (constructor != null) {
			newInst = (IDevice) constructor.newInstance(new Object[] { deviceConfig });
			deviceJarFileName = newInst.getClass().getProtectionDomain().getCodeSource().getLocation().getPath().replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK);
		}
		else
			throw new NoClassDefFoundError(Messages.getString(MessageIds.GDE_MSGE0016));
		
		deviceJarFileName = deviceJarFileName.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
		log.log(Level.WARNING, "deviceJarPath = " + deviceJarFileName); //$NON-NLS-1$
		
		if(deviceJarFileName.endsWith(GDE.STRING_WITHIN_ECLIPSE)) { //stated within eclipse
			deviceJarFileName = deviceJarFileName.substring(0, deviceJarFileName.indexOf(GDE.STRING_WITHIN_ECLIPSE));
			deviceJarFileName = deviceJarFileName.substring(deviceJarFileName.lastIndexOf(GDE.FILE_SEPARATOR_UNIX) + 1);
		}
		
		return deviceJarFileName;
	}

	/**
	 * updates an jar file with additional image content
	 * @param deviceJarPath
	 * @param tmpDeviceJarPath
	 * @param addJarEntryName
	 * @param addJarFileName
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void updateJarContent(String deviceJarPath, String tmpDeviceJarPath, String addJarEntryName, String addJarFileName, Shell messageBoxShell) throws IOException, FileNotFoundException {
		if (FileUtils.checkFileExist(tmpDeviceJarPath)) {
			MessageBox mBox = new MessageBox(messageBoxShell, SWT.PRIMARY_MODAL | SWT.YES | SWT.NO | SWT.CANCEL	| SWT.ICON_QUESTION);
			mBox.setText(GDE.NAME_LONG);
			mBox.setMessage(Messages.getString(MessageIds.GDE_MSGI0043, new String[] {tmpDeviceJarPath}));
			int ret = mBox.open();
			if (SWT.CANCEL == ret)
				return;
			else if (SWT.NO == ret) {
				FileUtils.renameFile(tmpDeviceJarPath, GDE.FILE_ENDING_BAK);
				deviceJarPath = tmpDeviceJarPath.substring(0, tmpDeviceJarPath.lastIndexOf(GDE.STRING_DOT)+1) + GDE.FILE_ENDING_BAK;
			}
		}
		
		JarInputStream in = new JarInputStream(new FileInputStream(deviceJarPath));
		JarOutputStream out = new JarOutputStream(new FileOutputStream(new File(tmpDeviceJarPath)), new JarFile(deviceJarPath).getManifest());

		JarEntry inEntry;
		byte[] buf = new byte[1024];
		int len = 0;

		//copy content to tmpDeviceJarPath
		while ((inEntry = in.getNextJarEntry()) != null) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "inEntry = " + inEntry.getName()); //$NON-NLS-1$
			if (!inEntry.getName().equalsIgnoreCase(addJarEntryName) && !inEntry.getName().endsWith("MANIFEST.MF")) { //$NON-NLS-1$
				out.putNextEntry(new JarEntry(inEntry));
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				out.closeEntry();
			}
			in.closeEntry();
		}
		in.close();

		//add new entry as image file
		JarEntry jarAddEntry = new JarEntry(addJarEntryName);
		out.putNextEntry(jarAddEntry);
		InputStream addIn = new FileInputStream(addJarFileName);
		len = 0;
		while ((len = addIn.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		addIn.close();
		out.closeEntry();
		out.close();

		File tmpFile = new File(tmpDeviceJarPath);
		if (tmpFile.exists()) {
			MessageBox mBox = new MessageBox(messageBoxShell, SWT.OK);
			mBox.setText(GDE.NAME_LONG);
			mBox.setMessage(Messages.getString(MessageIds.GDE_MSGI0044, new String[] {tmpDeviceJarPath})); 
			mBox.open();
		}
	}

	/**
	 * run on exit a thread/process to rename files while the JVM shuts down, take care to have possible access rights !!!
	 */
	public static void runOnExitRenamer() {
		if (FileUtils.onExitRenameJar != null && FileUtils.onExitRenameJar.size() > 0) {
			Thread onExitThread = new Thread() {
				@Override
				public void run() {
						for (String job : FileUtils.onExitRenameJar) {
							//log.log(Level.INFO, "onExitRenameJar.job = " + job);
							String deviceJarPath = job.split("2")[0]; //$NON-NLS-1$
							String tmpDeviceJarPath = job.split("2")[1]; //$NON-NLS-1$
							try {
								String javaexec = System.getProperty("java.home").replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX) + "/bin/java"; //$NON-NLS-1$ //$NON-NLS-2$
								String classpath = OperatingSystemHelper.getClasspathAsString().replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK);
								String command = javaexec + " -classpath '" + classpath + "' gde.utils.FileUtils '" + deviceJarPath + "' '" + tmpDeviceJarPath + "'";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
								log.log(Level.INFO, "executing: " + command); //$NON-NLS-1$

								//new ProcessBuilder(java -classpath DataExplorer.jar gde.utils.FileUtils sourceFullQualifiedFileName targetFullQualifiedFileName")
								new ProcessBuilder(javaexec, "-classpath", classpath, "gde.utils.FileUtils", deviceJarPath, tmpDeviceJarPath).start(); //$NON-NLS-1$ //$NON-NLS-2$
							}
							catch (Throwable e) {
								log.log(Level.WARNING, e.getMessage());
							}
						}
				}
			};
			onExitThread.start();
		}
	}
	
	/**
	 * rename a file while deleting the source file name and rename target to source
	 * @param sourceFullQualifiedFileName
	 * @param targetFullQualifiedFileName
	 */
	public static void rename(String sourceFullQualifiedFileName, String targetFullQualifiedFileName) {
		if (!new File(sourceFullQualifiedFileName).delete()) {
			log.log(Level.WARNING, "could not delete jar file " + sourceFullQualifiedFileName); //$NON-NLS-1$
		}
		else if (!new File(targetFullQualifiedFileName).renameTo(new File(sourceFullQualifiedFileName))) {
			log.log(Level.WARNING, "could not rename jar file " + targetFullQualifiedFileName + " to " + sourceFullQualifiedFileName); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * @param fullQualifiedFilePath
	 * @param unixPermissions
	 */
	private static void setAccessPermission(String fullQualifiedFilePath, String unixPermissions) {
		if (!GDE.IS_WINDOWS) { //$NON-NLS-1$ //$NON-NLS-2$
			try {
				unixPermissions = unixPermissions.trim();
				try {
					new Integer(unixPermissions);
				}
				catch(NumberFormatException e) {
					log.log(Level.SEVERE, "Internal Error - permission not usable (" + unixPermissions +")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				//if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "chmod 755 " + fileName);
				Runtime.getRuntime ().exec (new String []{"chmod", unixPermissions, fullQualifiedFilePath}).waitFor(); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (Throwable e) {
				//ignore
			}
		}
	}

	/**
	 * find out the base path where the application Jars are located  
	 * @return operating depending path where the device plug-in jar are located
	 */
	public static String getJarBasePath() {
		String basePath;
		URL url = DataExplorer.class.getProtectionDomain().getCodeSource().getLocation();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "base URL = " + url.toExternalForm()); //$NON-NLS-1$
		if (url.getPath().endsWith("/")) { // running inside Eclipse //$NON-NLS-1$
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "started inside Eclipse"); //$NON-NLS-1$
			basePath = url.getFile().substring(GDE.IS_WINDOWS ? 1 : 0, url.getPath().lastIndexOf(DataExplorer.class.getSimpleName()));
			basePath = basePath.replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK);
			basePath = basePath + "build" + "/target/" 																																																				//$NON-NLS-1$ //$NON-NLS-2$
				+ (GDE.IS_LINUX ? "GNU" : GDE.STRING_EMPTY )+ System.getProperty("os.name").split(GDE.STRING_BLANK)[0] + GDE.STRING_UNDER_BAR + GDE.BIT_MODE		//$NON-NLS-1$ //$NON-NLS-2$ 
				+ GDE.FILE_SEPARATOR_UNIX + GDE.NAME_LONG + (GDE.IS_MAC ? GDE.STRING_MAC_DOT_APP + GDE.STRING_MAC_APP_RES_PATH : GDE.STRING_EMPTY) 
				+ GDE.FILE_SEPARATOR_UNIX;  																																																				//$NON-NLS-1$ //$NON-NLS-2$
		}
		else { // started outside java -jar *.jar
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "started outside with: java -jar *.jar"); //$NON-NLS-1$
			basePath = url.getFile().substring(0, url.getPath().lastIndexOf("/") + 1); //$NON-NLS-1$
			basePath = basePath.replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK); 
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "GDE base path = " + basePath); //$NON-NLS-1$
		return basePath;
	}

	/**
	 * find out the base path where the device plug-in jar are located 
	 * @return operating depending path where the device plug-in jar are located
	 */
	public static String getDevicePluginJarBasePath() {
		String basePath;
		String jarPath = null;
		URL url = DataExplorer.class.getProtectionDomain().getCodeSource().getLocation();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "base URL = " + url.toExternalForm()); //$NON-NLS-1$
		if (url.getPath().endsWith("/")) { // running inside Eclipse //$NON-NLS-1$
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "started inside Eclipse"); //$NON-NLS-1$
			basePath = url.getFile().substring(0, url.getPath().lastIndexOf(DataExplorer.class.getSimpleName()));
			basePath = basePath.replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "basePath = " + basePath); //$NON-NLS-1$
			try {
				//jarPath = basePath + "build" + GDE.FILE_SEPARATOR_UNIX + "target" + GDE.FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME; //$NON-NLS-1$ //$NON-NLS-2$
				//targetDirectory this.applHomePath + GDE.FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME);
				jarPath = basePath + "build" + "/target/" 																																																				//$NON-NLS-1$ //$NON-NLS-2$
					+ (GDE.IS_LINUX ? "GNU" : GDE.STRING_EMPTY )+ System.getProperty("os.name").split(GDE.STRING_BLANK)[0] + GDE.STRING_UNDER_BAR + GDE.BIT_MODE		//$NON-NLS-1$ //$NON-NLS-2$ 
					+ GDE.FILE_SEPARATOR_UNIX + GDE.NAME_LONG + (GDE.IS_MAC ? GDE.STRING_MAC_DOT_APP + GDE.STRING_MAC_APP_RES_PATH : GDE.STRING_EMPTY) + "/devices";  																																																				//$NON-NLS-1$ //$NON-NLS-2$
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		else { // started outside java -jar *.jar
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "started outside with: java -jar *.jar"); //$NON-NLS-1$
			basePath = url.getFile().substring(0, url.getPath().lastIndexOf("/") + 1); //$NON-NLS-1$
			basePath = basePath.replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "basePath = " + basePath); //$NON-NLS-1$
			try {
				//jarPath = basePath + Settings.DEVICE_PROPERTIES_DIR_NAME;
				//targetDirectory this.applHomePath + GDE.FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME);
				jarPath = basePath + "devices"; //$NON-NLS-1$
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "device plug-ins path = " + jarPath); //$NON-NLS-1$
		return jarPath;
	}

	/**
	 * get the names of the exported device services
	 * @param jarFile 
	 * @throws IOException
	 */
	public static String[] getDeviceJarServicesNames(JarFile jarFile) throws IOException {
		Vector<String> pluginNamesVector = new Vector<String>();
		Manifest m = jarFile.getManifest();
		String services = m.getMainAttributes().getValue("Export-Service"); //$NON-NLS-1$
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Export-Service = " + services); //$NON-NLS-1$
		String[] serviceNames = services.split(", *"); //$NON-NLS-1$
		for (String name : serviceNames) {
			name = name.substring(name.lastIndexOf('.') + 1);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "service name = " + name); //$NON-NLS-1$
			pluginNamesVector.add(name);
		}
		return pluginNamesVector.toArray(new String[0]);
	}

	/**
	 * check the java executable version 
	 * @param javaFullQualifiedExecutablePath /usr/lib/jvm/java-sun/java
	 * @param expectedVersionString 1.6
	 * @return true if the expected version is lower or equal the version found by executing the given java executable
	 */
	public static boolean checkJavaExecutableVersion(String javaFullQualifiedExecutablePath, String expectedVersionString) {
		final String javaVersion = "java version"; //$NON-NLS-1$
		int actualVersion = 0;
		BufferedReader brerr = null;
		BufferedReader brout = null;
		try {
			String line;
			if (javaFullQualifiedExecutablePath.indexOf("%WINDIR%") > -1) { //$NON-NLS-1$
				javaFullQualifiedExecutablePath = System.getenv("WINDIR")  //$NON-NLS-1$
					+ javaFullQualifiedExecutablePath.substring(javaFullQualifiedExecutablePath.indexOf("%WINDIR%") + "%WINDIR%".length()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (javaFullQualifiedExecutablePath.indexOf("javaw") > -1) { //$NON-NLS-1$
				javaFullQualifiedExecutablePath = javaFullQualifiedExecutablePath.substring(0, javaFullQualifiedExecutablePath.indexOf("javaw")) //$NON-NLS-1$
					+ "java" + javaFullQualifiedExecutablePath.substring(javaFullQualifiedExecutablePath.indexOf("javaw")+ "javaw".length()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			Process process = new ProcessBuilder(javaFullQualifiedExecutablePath, "-version").start(); //$NON-NLS-1$
			brerr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			
			while ((line = brerr.readLine()) != null) { // clean std err
				if (line.startsWith(javaVersion)) actualVersion = parseJavaVersion(line.substring(javaVersion.length()+2));
			}
			
			brout = new BufferedReader(new InputStreamReader(process.getInputStream()));
			while ((line = brout.readLine()) != null) 
				; // clean std out
			process.waitFor(); // waits until termination
		}
		catch (Throwable e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		finally {
			try {
				if (brerr != null) brerr.close();
				if (brout != null) brout.close();
			}
			catch (IOException e) {
				// ignore
			}
		}
		
		log.log(Level.INFO, parseJavaVersion(expectedVersionString) + " <= " + actualVersion); //$NON-NLS-1$
		return (parseJavaVersion(expectedVersionString) <= actualVersion);
	}

	/**
	 * parse the java version 1.6.0** will result in 160
	 * @param version 1.6.0*
	 * @return 1.6.0** will result in 160
	 */
	static int parseJavaVersion(String version) {
		if (version == null) return 0;
		int major = 0, minor = 0, micro = 0, index = 0, start = 0;
		int versionStringLength = version.length();
		
		while (index < versionStringLength && Character.isDigit(version.charAt(index))) index++;
		try {
			if (start < versionStringLength) major = Integer.parseInt(version.substring(start, index));
		} catch (NumberFormatException e) {
			//ignore
		}
		start = ++index;
		while (index < versionStringLength && Character.isDigit(version.charAt(index))) index++;
		try {
			if (start < versionStringLength) minor = Integer.parseInt(version.substring(start, index));
		} catch (NumberFormatException e) {
			//ignore			
		}
		start = ++index;
		while (index < versionStringLength && Character.isDigit(version.charAt(index))) index++;
		try {
			if (start < versionStringLength) micro = Integer.parseInt(version.substring(start, index));
		} catch (NumberFormatException e) {
			//ignore
		}
		
		return major*100 + minor*10 + micro;
	}

	/**
	  * Recursively walk a directory tree and return a List of all files found.
	  * @param rootDirectory is a valid directory
	  * @param recursionDepth specifies the depth of recursion cycles
	  * @return List<File> sorted using File.compareTo()
	  * @throws FileNotFoundException
	  */
	public static List<File> getFileListing(File rootDirectory, int recursionDepth) throws FileNotFoundException {
		validateDirectory(rootDirectory);
		List<File> result = getFileListingNoSort(rootDirectory, recursionDepth);
		Collections.sort(result);
		return result;
	}

	/**
	  * Recursively walk a directory tree and return a List of all files found.
	  * @param rootDirectory is a valid directory
	  * @return List<File> sorted using File.compareTo()
	  * @throws FileNotFoundException
	  */
	public static List<File> getDirListing(File rootDirectory) throws FileNotFoundException {
		validateDirectory(rootDirectory);
		List<File> result = getDirListingNoSort(rootDirectory);
		Collections.sort(result);
		return result;
	}

	/**
	 * Recursively walk a directory tree and return a List of all Files found;
	 * @param rootDirectory
	 * @param recursionDepth
	 * @return List<File>
	 * @throws FileNotFoundException
	 */
	private static List<File> getFileListingNoSort(File rootDirectory, int recursionDepth) throws FileNotFoundException {
		int depth = recursionDepth;
		List<File> result = new ArrayList<File>();
		if (rootDirectory.isDirectory() && rootDirectory.canRead()) {
			File[] filesAndDirs = rootDirectory.listFiles();
			if (filesAndDirs != null) {
				List<File> filesDirs = Arrays.asList(filesAndDirs);
				for (File file : filesDirs) {
					if (file.isFile()) {
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, file.getAbsolutePath());
						result.add(file);
					} else if (file.isDirectory() && recursionDepth > 0) {	//recursive walk by calling itself
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, depth + " " + file.getAbsolutePath());
						List<File> deeperList = getFileListingNoSort(file, --recursionDepth);
						result.addAll(deeperList);
						++recursionDepth;
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Recursively walk a directory tree and return a list of all directories found;
	 * @param rootDirectory
	 * @return List<File>
	 * @throws FileNotFoundException
	 */
	private static List<File> getDirListingNoSort(File rootDirectory) throws FileNotFoundException {
		List<File> result = new ArrayList<File>();
		if (rootDirectory.isDirectory() && rootDirectory.canRead()) {
			File[] filesAndDirs = rootDirectory.listFiles();
			List<File> filesDirs = Arrays.asList(filesAndDirs);
			for (File file : filesDirs) {
				if (file.isDirectory()) {
					result.add(file);
				}
			}
		}
		return result;
	}

	/**
	 * Directory is valid if it exists, does not represent a file, and can be read.
	 * @throws FileNotFoundException
	 * @throws IllegalArgumentException
	 */
	public static void validateDirectory(File directory) throws FileNotFoundException, IllegalArgumentException {
		if (directory == null) {
			throw new IllegalArgumentException("Directory should not be null."); //$NON-NLS-1$
		}
		if (!directory.exists()) {
			throw new FileNotFoundException("Directory does not exist: " + directory); //$NON-NLS-1$
		}
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("Is not a directory: " + directory); //$NON-NLS-1$
		}
		if (!directory.canRead()) {
			throw new IllegalArgumentException("Directory cannot be read: " + directory); //$NON-NLS-1$
		}
	}

	/**
	 * cleanup possible old files like log file, native libraries
	 */
	public static void cleanupPre() {
		if (GDE.IS_WINDOWS) 
			FileUtils.cleanFiles(GDE.JAVA_IO_TMPDIR, new String[] {"bootstrap.log.*", "WinHelper*.dll", "Register*.exe", "rxtxSerial.dll"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		else if (GDE.IS_LINUX)
			FileUtils.cleanFiles(GDE.JAVA_IO_TMPDIR, new String[] {"bootstrap.log.*", "*register.sh", "librxtxSerial.so"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		else if (GDE.IS_MAC)
			FileUtils.cleanFiles(GDE.JAVA_IO_TMPDIR, new String[] {"bootstrap.log.*", "librxtxSerial.jnilib"}); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * cleanup possible old files like log file, native libraries
	 */
	public static void cleanupPost() {
		String jarBasePath = FileUtils.getJarBasePath();
		String jarFilePath = jarBasePath + "DataExplorer.jar"; //$NON-NLS-1$

		try {
			if (GDE.IS_WINDOWS) {
				String[] commandArray = new String[] {"\""+ System.getProperty("sun.boot.library.path") + GDE.FILE_SEPARATOR + "java\"", "-classpath", jarFilePath, "-D" + GDE.CLEAN_SETTINGS_WHILE_SHUTDOWN + GDE.STRING_EQUAL + Boolean.parseBoolean(System.getProperty(GDE.CLEAN_SETTINGS_WHILE_SHUTDOWN)), "gde.utils.FileUtils"};//$NON-NLS-1$ //$NON-NLS-2$
				//if (log.isLoggable(Level.FINE)) {
				//	StringBuilder sb = new StringBuilder();
				//	for (int i = 0; i < commandArray.length; i++) {
				//		sb.append(commandArray[i]).append(GDE.STRING_BLANK);
				//	}
				//	log.log(Level.FINE, sb.toString());
				//}
				Runtime.getRuntime().exec(commandArray); 
			}
			else
				Runtime.getRuntime().exec(new String[] {"java", "-classpath", jarFilePath, "-D" + GDE.CLEAN_SETTINGS_WHILE_SHUTDOWN + GDE.STRING_EQUAL + Boolean.parseBoolean(System.getProperty(GDE.CLEAN_SETTINGS_WHILE_SHUTDOWN)), "gde.utils.FileUtils", "&"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		catch (Exception e) {
			log.log(Level.SEVERE, "failed executing cleanup settings "); //$NON-NLS-1$
		}
	}
	/**
	 * main method to execute post execution cleanup, called by cleanupPost()
	 */
	public static void main(String[] args) {
		if (Boolean.parseBoolean(System.getProperty(GDE.CLEAN_SETTINGS_WHILE_SHUTDOWN))) {
			if (GDE.IS_WINDOWS) {
				OperatingSystemHelper.removeDesktopLink();
				FileUtils.cleanFiles(System.getenv("APPDATA") + GDE.FILE_SEPARATOR, new String[] {GDE.NAME_LONG});
				OperatingSystemHelper.deregisterApplication();
			}
			else if (GDE.IS_LINUX) {
				OperatingSystemHelper.removeDesktopLink();
				OperatingSystemHelper.deregisterApplication();
				FileUtils.cleanFiles(System.getProperty("user.home") + GDE.FILE_SEPARATOR_UNIX, new String[] {GDE.STRING_DOT+GDE.NAME_LONG});
			}
			else if (GDE.IS_MAC) {
				FileUtils.cleanFiles(System.getProperty("user.home") + GDE.FILE_SEPARATOR_UNIX + "Library" + GDE.FILE_SEPARATOR_UNIX + "Application Support" + GDE.FILE_SEPARATOR_UNIX , new String[] {GDE.NAME_LONG});
			}
		}
		if (GDE.IS_WINDOWS) 
			FileUtils.cleanFiles(GDE.JAVA_IO_TMPDIR, new String[] {"bootstrap.log.lck", "OSDE", "rxtxSerial.dll", "GDE", "WinHelper*.dll", "Register*.exe", GDE.FILE_ENDING_STAR_KMZ}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		else if (GDE.IS_LINUX)
			FileUtils.cleanFiles(GDE.JAVA_IO_TMPDIR, new String[] {"bootstrap.log.lck", "GDE", "*register.sh", GDE.FILE_ENDING_STAR_KMZ}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		else if (GDE.IS_MAC)
			FileUtils.cleanFiles(GDE.JAVA_IO_TMPDIR, new String[] {"bootstrap.log.lck", "GDE", GDE.FILE_ENDING_STAR_KMZ}); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * query the import data directory in dependency of search directory, object, etc
	 * @param searchDirectory
	 * @param objectKey
	 * @return
	 */
	public static FileDialog getImportDirectoryFileDialog(IDevice device, String dialogTitleMessage) {
		String devicePath = DataExplorer.application.getActiveDevice() != null ? GDE.FILE_SEPARATOR_UNIX + DataExplorer.application.getActiveDevice().getName() : GDE.STRING_EMPTY;
		String searchDirectory = Settings.getInstance().getDataFilePath() + devicePath + GDE.FILE_SEPARATOR_UNIX;
		String objectKey = DataExplorer.application.getObjectKey();
		
		if (Settings.getInstance().isDeviceImportDirectoryObjectRelated() && DataExplorer.application.isObjectoriented() && objectKey != null && !objectKey.equals(GDE.STRING_EMPTY)) {
			String objectkeyPath = Settings.getInstance().getDataFilePath() + GDE.FILE_SEPARATOR_UNIX + objectKey;
			FileUtils.checkDirectoryAndCreate(objectkeyPath);
			searchDirectory = objectkeyPath;
		}
		else if (FileUtils.checkDirectoryExist(device.getDeviceConfiguration().getDataBlockPreferredDataLocation())) {
			searchDirectory = device.getDeviceConfiguration().getDataBlockPreferredDataLocation();
		}
		final FileDialog fd = DataExplorer.application.openFileOpenDialog(dialogTitleMessage, new String[] { device.getDeviceConfiguration().getDataBlockPreferredFileExtention(),
				(GDE.IS_WINDOWS ? GDE.FILE_ENDING_STAR_STAR : GDE.FILE_ENDING_STAR) }, searchDirectory, null, SWT.MULTI);

		if (!Settings.getInstance().isDeviceImportDirectoryObjectRelated() && !searchDirectory.equals(fd.getFilterPath())) device.getDeviceConfiguration().setDataBlockPreferredDataLocation(fd.getFilterPath());
		return fd;
	}

	/**
	 * query the import data directory in dependency of search directory, object, etc
	 * @param searchDirectory
	 * @param objectKey
	 * @return
	 */
	public static FileDialog getImportDirectoryFileDialog(IDevice device, String dialogTitleMessage, String[] fileExtensions) {
		String devicePath = DataExplorer.application.getActiveDevice() != null ? GDE.FILE_SEPARATOR_UNIX + DataExplorer.application.getActiveDevice().getName() : GDE.STRING_EMPTY;
		String searchDirectory = Settings.getInstance().getDataFilePath() + devicePath + GDE.FILE_SEPARATOR_UNIX;
		String objectKey = DataExplorer.application.getObjectKey();
		
		if (Settings.getInstance().isDeviceImportDirectoryObjectRelated() && DataExplorer.application.isObjectoriented() && objectKey != null && !objectKey.equals(GDE.STRING_EMPTY)) {
			String objectkeyPath = Settings.getInstance().getDataFilePath() + GDE.FILE_SEPARATOR_UNIX + objectKey;
			FileUtils.checkDirectoryAndCreate(objectkeyPath);
			searchDirectory = objectkeyPath;
		}
		else if (FileUtils.checkDirectoryExist(device.getDeviceConfiguration().getDataBlockPreferredDataLocation())) {
			searchDirectory = device.getDeviceConfiguration().getDataBlockPreferredDataLocation();
		}
		final FileDialog fd = DataExplorer.application.openFileOpenDialog(dialogTitleMessage, fileExtensions, searchDirectory, null, SWT.MULTI);

		if (!Settings.getInstance().isDeviceImportDirectoryObjectRelated() && !searchDirectory.equals(fd.getFilterPath())) device.getDeviceConfiguration().setDataBlockPreferredDataLocation(fd.getFilterPath());
		return fd;
	}

	/**
	 * query the import data directory in dependency of search directory, object, etc
	 * @param searchDirectory
	 * @param objectKey
	 * @param baseDirectory name of directory where object data directories are located
	 * @return
	 */
	public static FileDialog getImportDirectoryFileDialog(IDevice device, String dialogTitleMessage, String baseDirectory) {
		String devicePath = DataExplorer.application.getActiveDevice() != null ? GDE.FILE_SEPARATOR_UNIX + DataExplorer.application.getActiveDevice().getName() : GDE.STRING_EMPTY;
		String searchDirectory = Settings.getInstance().getDataFilePath() + devicePath + GDE.FILE_SEPARATOR_UNIX;
		String objectKey = DataExplorer.application.getObjectKey();
		
		if (Settings.getInstance().isDeviceImportDirectoryObjectRelated() && DataExplorer.application.isObjectoriented() && objectKey != null && !objectKey.equals(GDE.STRING_EMPTY)) {
			String objectkeyPath = Settings.getInstance().getDataFilePath() + GDE.FILE_SEPARATOR_UNIX + objectKey;
			FileUtils.checkDirectoryAndCreate(objectkeyPath);
			searchDirectory = objectkeyPath;
		}
		else if (DataExplorer.application.isObjectoriented() && objectKey != null && !objectKey.equals(GDE.STRING_EMPTY)) {
			searchDirectory = device.getDeviceConfiguration().getDataBlockPreferredDataLocation();
			if (searchDirectory.contains(baseDirectory)) {
				searchDirectory = searchDirectory.substring(0, searchDirectory.lastIndexOf(baseDirectory)) + baseDirectory + GDE.FILE_SEPARATOR_UNIX + objectKey;
			}
		}
		else if (FileUtils.checkDirectoryExist(device.getDeviceConfiguration().getDataBlockPreferredDataLocation())) {
			searchDirectory = device.getDeviceConfiguration().getDataBlockPreferredDataLocation();
		}
		final FileDialog fd = DataExplorer.application.openFileOpenDialog(dialogTitleMessage, new String[] { device.getDeviceConfiguration().getDataBlockPreferredFileExtention(),
				(GDE.IS_WINDOWS ? GDE.FILE_ENDING_STAR_STAR : GDE.FILE_ENDING_STAR) }, searchDirectory, null, SWT.MULTI);

		if (!Settings.getInstance().isDeviceImportDirectoryObjectRelated() &&  !searchDirectory.equals(fd.getFilterPath())) device.getDeviceConfiguration().setDataBlockPreferredDataLocation(fd.getFilterPath());
		return fd;
	}
}
