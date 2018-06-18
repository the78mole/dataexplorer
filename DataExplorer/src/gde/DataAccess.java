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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipFile;

import com.sun.istack.internal.Nullable;

import gde.config.Settings;
import gde.histo.cache.HistoVault;
import gde.histo.cache.VaultProxy;
import gde.log.Level;
import gde.log.Logger;
import gde.utils.FileUtils;

/**
 * Support the roaming data sources.
 * @author Thomas Eickert (USER)
 */
public abstract class DataAccess {

	private static final int MIN_VAULT_LENGTH = 2048;

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
			final Path cachePath = Settings.getInstance().getHistoCacheDirectory().resolve(folderName);
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
	 * @return the vault retrieved from the file system if it exceeds the minumum file size
	 */
	@Nullable
	public abstract HistoVault getVault(String folderName, String fileName);

}
