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

package gde;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.net.ssl.HttpsURLConnection;

import com.sun.istack.internal.Nullable;

import gde.config.Settings;
import gde.histo.cache.HistoVault;
import gde.histo.cache.VaultProxy;
import gde.log.Level;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.FileUtils;

/**
 * Support the roaming data sources.
 * @author Thomas Eickert (USER)
 */
public abstract class DataAccess {
	private static final String	$CLASS_NAME				= DataAccess.class.getName();
	private static final Logger	log								= Logger.getLogger($CLASS_NAME);

	private static final int		MIN_VAULT_LENGTH	= 2048;

	public static class LocalAccess extends DataAccess {
		private static final String	$CLASS_NAME	= LocalAccess.class.getName();
		private static final Logger	log					= Logger.getLogger($CLASS_NAME);

		@Override
		@Nullable
		public Reader getSettingsReader() {
			Reader reader = null;
			try {
				File file = new File(GDE.SETTINGS_FILE_PATH);
				if (!file.exists()) {
					if (!file.createNewFile()) log.log(Level.WARNING, "failed creating ", file);
				}
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")); //$NON-NLS-1$
			} catch (Exception e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
			return reader;
		}

		@Override
		@Nullable
		public Writer getSettingsWriter() {
			Writer writer = null;
			try {
				File file = new File(GDE.SETTINGS_FILE_PATH);
				if (!file.exists()) {
					if (!file.createNewFile()) log.log(Level.WARNING, "failed creating ", file);
				}
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$
			} catch (Exception e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
			return writer;
		}

		@Override
		public long getGraphicsTemplateLastModified(Path fileSubPath) {
			return Paths.get(GDE.APPL_HOME_PATH, Settings.GRAPHICS_TEMPLATES_DIR_NAME).resolve(fileSubPath).toFile().lastModified();
		}

		/**
		 * Support standard file systems only.
		 * @param file is a file in a standard file system
		 * @return the inputStream for a standard template or an object template
		 */
		@Nullable
		public InputStream getAlienTemplateInputStream(File file) throws FileNotFoundException {
			return new FileInputStream(file);
		}

		@Override
		public boolean existsGraphicsTemplate(Path fileSubPath) {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.GRAPHICS_TEMPLATES_DIR_NAME).resolve(fileSubPath);
			return targetFilePath.toFile().exists();
		}

		@Override
		@Nullable
		public InputStream getGraphicsTemplateInputStream(Path fileSubPath) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.GRAPHICS_TEMPLATES_DIR_NAME).resolve(fileSubPath);
			return new FileInputStream(targetFilePath.toFile());
		}

		@Override
		@Nullable
		public OutputStream getGraphicsTemplateOutputStream(Path fileSubPath) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.GRAPHICS_TEMPLATES_DIR_NAME).resolve(fileSubPath);
			File folder = targetFilePath.getParent().toFile();
			if (!folder.exists()) {
				if (!folder.mkdir()) {
					log.log(WARNING, "failed to create ", folder);
				}
			}
			return new FileOutputStream(targetFilePath.toFile());
		}

		@Override
		public boolean existsExclusionFile(String fileName) {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_EXCLUSIONS_DIR_NAME, fileName);
			return targetFilePath.toFile().exists();
		}

		/**
		 * @param fileName of the inclusions file in the standard directory
		 */
		@Override
		public void deleteExclusionFile(String fileName) {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_EXCLUSIONS_DIR_NAME, fileName);
			FileUtils.deleteFile(targetFilePath.toString());
		}

		@Override
		@Nullable
		public InputStream getExclusionsInputStream(String fileName) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_EXCLUSIONS_DIR_NAME, fileName);
			return new FileInputStream(targetFilePath.toFile());
		}

		@Override
		@Nullable
		public OutputStream getExclusionsOutputStream(String fileName) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_EXCLUSIONS_DIR_NAME, fileName);
			File folder = targetFilePath.getParent().toFile();
			if (!folder.exists()) {
				if (!folder.mkdir()) {
					log.log(WARNING, "failed to create ", folder);
				}
			}
			return new FileOutputStream(targetFilePath.toFile());
		}

		@Override
		public boolean existsInclusionFile(String fileName) {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_INCLUSIONS_DIR_NAME, fileName);
			return targetFilePath.toFile().exists();
		}

		@Override
		public void deleteInclusionFile(String fileName) {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_INCLUSIONS_DIR_NAME, fileName);
			FileUtils.deleteFile(targetFilePath.toString());
		}

		@Override
		@Nullable
		public InputStream getInclusionsInputStream(String fileName) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_INCLUSIONS_DIR_NAME, fileName);
			return new FileInputStream(targetFilePath.toFile());
		}

		@Override
		@Nullable
		public OutputStream getInclusionsOutputStream(String fileName) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_INCLUSIONS_DIR_NAME, fileName);
			File folder = targetFilePath.getParent().toFile();
			if (!folder.exists()) {
				if (!folder.mkdir()) {
					log.log(WARNING, "failed to create ", folder);
				}
			}
			return new FileOutputStream(targetFilePath.toFile());
		}

		@Override
		@Nullable
		public HistoVault getVault(String folderName, String fileName) {
			Path cachePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME, folderName);
			HistoVault histoVault = null;
			if (Settings.getInstance().isZippedCache()) {
				try (ZipFile zf = new ZipFile(cachePath.toFile())) {
					if (zf.getEntry(fileName) != null && zf.getEntry(fileName).getSize() > MIN_VAULT_LENGTH) {
						histoVault = VaultProxy.load(zf.getInputStream(zf.getEntry(fileName)));
					}
				} catch (Exception e) {
					log.log(SEVERE, e.getMessage(), e);
				}
			} else {
				File file = cachePath.resolve(fileName).toFile();
				if (file.length() > MIN_VAULT_LENGTH) {
					try {
						histoVault = VaultProxy.load(cachePath.resolve(file.getName()));
					} catch (Exception e) {
						log.log(SEVERE, e.getMessage(), e);
					}
				}
			}
			return histoVault;
		}

		@Override
		public boolean existsDeviceXml(Path fileSubPath) {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH).resolve(fileSubPath);
			return targetFilePath.toFile().exists();
		}

		@Override
		public InputStream getDeviceXsdInputStream() throws FileNotFoundException {
			String xmlBasePath = GDE.APPL_HOME_PATH + GDE.FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME + GDE.FILE_SEPARATOR_UNIX;
			Path targetFilePath = Paths.get(xmlBasePath, Settings.DEVICE_PROPERTIES_XSD_NAME);
			return new FileInputStream(targetFilePath.toFile());
		}

		@Override
		public InputStream getDeviceXmlInputStream(Path fileSubPath) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH).resolve(fileSubPath);
			return new FileInputStream(targetFilePath.toFile());
		}

		public InputStream getDeviceXsdMigrationStream(int versionNumber) throws FileNotFoundException {
			Path migratePropertyPath = Paths.get(GDE.APPL_HOME_PATH + GDE.FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME + "_V" + versionNumber);
			Path targetFilePath = migratePropertyPath.resolve("/DeviceProperties_V" + versionNumber + GDE.FILE_ENDING_DOT_XSD);
			return new FileInputStream(targetFilePath.toFile());
		}

		public boolean existsDeviceMigrationFolder(int versionNumber) {
			Path migratePropertyPath = Paths.get(GDE.APPL_HOME_PATH + GDE.FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME + "_V" + versionNumber);
			return migratePropertyPath.toFile().exists();
		}

		public List<File> getDeviceXmls(int versionNumber) throws FileNotFoundException {
			Path migratePropertyPath = Paths.get(GDE.APPL_HOME_PATH + GDE.FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME + "_V" + versionNumber);
			return FileUtils.getFileListing(migratePropertyPath.toFile(), 1, GDE.FILE_ENDING_DOT_XML);
		}

		@Override
		public InputStream getGeoCodeInputStream(String geoFileName) throws FileNotFoundException {
			Path filePath = Paths.get(GDE.APPL_HOME_PATH, Settings.GPS_LOCATIONS_DIR_NAME, geoFileName);
			return new FileInputStream(filePath.toFile());
		}

		public InputStream getHttpsInputStream(URL requestUrl) throws IOException {
			HttpsURLConnection conn = (HttpsURLConnection) requestUrl.openConnection();
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
			InputStream httpInputStream = conn.getInputStream();
			return httpInputStream;
		}

		public FileOutputStream getGeoCodeOutputStream(String geoFileName) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.GPS_LOCATIONS_DIR_NAME, geoFileName);
			return new FileOutputStream(targetFilePath.toString());
		}

		@Override
		public boolean existsGeoCodeFile(String geoFileName) {
			Path filePath = Paths.get(GDE.APPL_HOME_PATH, Settings.GPS_LOCATIONS_DIR_NAME, geoFileName);
			return FileUtils.checkFileExist(filePath.toString());
		}

		@Override
		public void deleteGeoCodeFile(String geoFileName) {
			Path filePath = Paths.get(GDE.APPL_HOME_PATH, Settings.GPS_LOCATIONS_DIR_NAME, geoFileName);
			FileUtils.deleteFile(filePath.toString());
		}

		@Override
		public List<String> getGeoCodeFolderList() throws FileNotFoundException {
			Path locationsPath = Paths.get(GDE.APPL_HOME_PATH, Settings.GPS_LOCATIONS_DIR_NAME);
			List<File> fileListing = FileUtils.getFileListing(locationsPath.toFile(), 0);

			List<String> fileNames = new ArrayList<>();
			for (File file : fileListing) {
				fileNames.add(file.getName());
			}
			return fileNames;
		}

		/**
		 * @return true if files were actually deleted
		 */
		public boolean resetHistolocations() {
			Path locationsPath = Paths.get(GDE.APPL_HOME_PATH, Settings.GPS_LOCATIONS_DIR_NAME);
			if (FileUtils.checkDirectoryExist(locationsPath.toString())) {
				FileUtils.deleteDirectory(locationsPath.toString());
				log.log(Level.CONFIG, "histo geo locations deleted"); //$NON-NLS-1$
				return true;
			} else
				return false;
		}

		/**
		 * @return true if the folder already exists
		 */
		public boolean checkAndCreateHistoLocations() {
			return FileUtils.checkDirectoryAndCreate(GDE.APPL_HOME_PATH + GDE.FILE_SEPARATOR_UNIX + Settings.GPS_LOCATIONS_DIR_NAME);
		}

		public String resetHistoCache() {
			Path cachePath = Paths.get(GDE.APPL_HOME_PATH, Settings.HISTO_CACHE_ENTRIES_DIR_NAME);
			int initialSize_KiB = (int) FileUtils.size(cachePath) / 1024;
			FileUtils.deleteDirectory(cachePath.toString());
			FileUtils.checkDirectoryAndCreate(cachePath.toString());
			int deletedSize_KiB = (int) FileUtils.size(cachePath) / 1024;
			FileUtils.extract(this.getClass(), Settings.HISTO_CACHE_ENTRIES_XSD_NAME, Settings.PATH_RESOURCE, cachePath.toString(), Settings.PERMISSION_555);
			String message = Messages.getString(MessageIds.GDE_MSGT0831, new Object[] { initialSize_KiB, deletedSize_KiB, cachePath });
			log.log(Level.CONFIG, message);
			return message;
		}

		@Override
		public boolean existsCacheDirectory(String directoryName) {
			Path cacheDirectoryPath = Paths.get(GDE.APPL_HOME_PATH , Settings.HISTO_CACHE_ENTRIES_DIR_NAME , directoryName);
			return cacheDirectoryPath.toFile().exists();
		}

		@Override
		public InputStream getCacheXsdInputStream() throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH , Settings.HISTO_CACHE_ENTRIES_DIR_NAME, Settings.HISTO_CACHE_ENTRIES_XSD_NAME);
			return new FileInputStream(targetFilePath.toFile());
		}

		@Override
		public ZipInputStream getCacheZipInputStream(String directoryName) throws ZipException, IOException {
			Path cacheDirectoryPath = Paths.get(GDE.APPL_HOME_PATH , Settings.HISTO_CACHE_ENTRIES_DIR_NAME , directoryName);
			return new ZipInputStream(new FileInputStream(cacheDirectoryPath.toFile()));
		}

		@Override
		public InputStream getMappingInputStream(String fileName) throws FileNotFoundException {
			Path targetFilePath = Paths.get(GDE.APPL_HOME_PATH, Settings.MAPPINGS_DIR_NAME, fileName);
			return new BufferedInputStream(new FileInputStream(targetFilePath.toFile()));
		}

		@Override
		public void checkMappingFileAndCreate(Class<?>  sourceClass, String fileName) {
			File path = new File(GDE.APPL_HOME_PATH + GDE.FILE_SEPARATOR_UNIX + Settings.MAPPINGS_DIR_NAME);
			Path targetFilePath = Paths.get(path.toString() + GDE.FILE_SEPARATOR_UNIX + fileName);
			if (!targetFilePath.toFile().exists()) {
				if (!path.exists() && !path.isDirectory()) path.mkdir();
				//extract initial property files
				FileUtils.extract(sourceClass, fileName, Locale.getDefault().equals(Locale.ENGLISH) ? "resource/en" : "resource/de", path.getAbsolutePath(), Settings.PERMISSION_555); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		@Override
		@Nullable
		public InputStream getSourceInputStream(Path fittedFilePath) {
			try {
				return new FileInputStream(fittedFilePath.toFile());
			} catch (FileNotFoundException e) {
				throw new IllegalArgumentException("invalid path " + fittedFilePath);
			}
		}

	}

	public static DataAccess getInstance() {
		if (DataAccess.dataAccess == null) {
			if (GDE.EXECUTION_ENV == null) {
				DataAccess.dataAccess = new LocalAccess();
			} else if (GDE.EXECUTION_ENV.equals("AWS_EXECUTION_ENV")) {
				DataAccess.dataAccess = null;
			} else {
				throw new UnsupportedOperationException();
			}
		}
		return DataAccess.dataAccess;
	}

	private static DataAccess dataAccess;

	/**
	 * @param fileSubPath to retrieve the object template from the standard directory
	 * @return the outputStream for a standard template or an object template
	 */
	public abstract OutputStream getGraphicsTemplateOutputStream(Path fileSubPath) throws FileNotFoundException;

	/**
	 * @param fileSubPath to retrieve the object template from the standard directory
	 * @return the inputStream for a standard template or an object template
	 */
	public abstract InputStream getGraphicsTemplateInputStream(Path fileSubPath) throws FileNotFoundException;

	/**
	 * @param fileSubPath to retrieve the object template from the standard directory
	 * @return true if the file exists
	 */
	public abstract boolean existsGraphicsTemplate(Path fileSubPath);

	public abstract long getGraphicsTemplateLastModified(Path fileSubPath);

	public abstract Writer getSettingsWriter();

	public abstract Reader getSettingsReader();

	/**
	 * @param fileName to retrieve the object template from the standard directory
	 * @return the outputStream for an exclusions file
	 */
	public abstract OutputStream getInclusionsOutputStream(String fileName) throws FileNotFoundException;

	/**
	 * @param fileName to retrieve the object template from the standard directory
	 * @return the inputStream for an exclusions file
	 */
	public abstract InputStream getInclusionsInputStream(String fileName) throws FileNotFoundException;

	/**
	 * @param fileName to retrieve the inclusions from the standard directory
	 * @return true if the file exists
	 */
	public abstract boolean existsInclusionFile(String fileName);

	/**
	 * @param fileName of the inclusions file in the standard directory
	 */
	public abstract void deleteInclusionFile(String fileName);

	/**
	 * @param fileName to retrieve the object template from the standard directory
	 * @return the outputStream for an inclusions file
	 */
	public abstract OutputStream getExclusionsOutputStream(String fileName) throws FileNotFoundException;

	/**
	 * @param fileName to retrieve the object template from the standard directory
	 * @return the inputStream for an inclusions file
	 */
	public abstract InputStream getExclusionsInputStream(String fileName) throws FileNotFoundException;

	/**
	 * @param fileName to retrieve the exclusions from the standard directory
	 * @return true if the file exists
	 */
	public abstract boolean existsExclusionFile(String fileName);

	/**
	 * @param fileName of the exclusions file in the standard directory
	 */
	public abstract void deleteExclusionFile(String fileName);

	/**
	 * @return the vault retrieved from the file system if it exceeds the minimum file size
	 */
	@Nullable
	public abstract HistoVault getVault(String folderName, String fileName);

	/**
	 * @param fileSubPath is a relative path based on the roaming folder
	 * @return true if the file exists
	 */
	public abstract boolean existsDeviceXml(Path fileSubPath);

	public abstract InputStream getDeviceXsdInputStream() throws FileNotFoundException;

	/**
	 * @param fileSubPath is a relative path based on the roaming folder
	 * @return the inputStream for a device properties file
	 */
	public abstract InputStream getDeviceXmlInputStream(Path fileSubPath) throws FileNotFoundException;

	public abstract InputStream getGeoCodeInputStream(String geoFileName) throws FileNotFoundException;

	public abstract boolean existsGeoCodeFile(String geoFileName);

	public abstract void deleteGeoCodeFile(String geoFileName);

	public abstract List<String> getGeoCodeFolderList() throws FileNotFoundException;

	public abstract boolean existsCacheDirectory(String directoryName);

	public abstract InputStream getCacheXsdInputStream() throws FileNotFoundException;

	public abstract ZipInputStream getCacheZipInputStream(String directoryName) throws ZipException, IOException;

	public abstract InputStream getMappingInputStream(String fileName) throws FileNotFoundException;

	public abstract void checkMappingFileAndCreate(Class<?> sourceClass, String fileName);

	/**
	 * @param fittedFilePath is a customized path (full path for local file system access or relative path for other data sources)
	 * @return the input stream for a logging source file (osd, bin , ...)
	 */
	public abstract InputStream getSourceInputStream(Path fittedFilePath);

}
