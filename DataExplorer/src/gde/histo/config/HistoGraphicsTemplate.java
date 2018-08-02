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

    Copyright (c) 2016,2017,2018 Thomas Eickert
****************************************************************************************/
package gde.histo.config;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Properties;

import com.sun.istack.internal.Nullable;

import gde.GDE;
import gde.config.Settings;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.log.Level;
import gde.log.Logger;
import gde.ui.DataExplorer;

/**
 * Histo graphics visualization, store, restore.
 * Values related to records are stored like this:</br>
 * {@code <entry key="Tx_isPositionLeft">false</entry>}
 * @author Thomas Eickert
 */
public abstract class HistoGraphicsTemplate extends Properties {
	static final String	$CLASS_NAME				= HistoGraphicsTemplate.class.getName();
	static final Logger	log								= Logger.getLogger($CLASS_NAME);
	static final long		serialVersionUID	= 2088159376716311896L;

	/**
	 * Conversion into a skeleton histo template.
	 */
	protected static final class LegacyGraphicsTemplate extends Properties {
		private static final long						serialVersionUID	= -4459032385846145309L;

		private final String								legacyFileName;
		private final String								templatePath;

		private final HistoGraphicsTemplate	convertedTemplate;
		private final List<MeasurementType>	channelMeasurements;

		/**
		 * Access to the default graphics template file used by the legacy kernel.
		 * @param deviceSignature - device signature as String (Picolario_K1) used for composing the file name.
		 * @param objectKey
		 */
		LegacyGraphicsTemplate(String deviceSignature, String objectKey) {
			this.templatePath = Settings.getInstance().getGraphicsTemplatePath();
			this.legacyFileName = deviceSignature + Settings.GRAPHICS_TEMPLATES_EXTENSION.substring(Settings.GRAPHICS_TEMPLATES_EXTENSION.length() - 4);
			log.log(Level.FINE, "from signature ", this);

			this.convertedTemplate = HistoGraphicsTemplate.createGraphicsTemplate(deviceSignature, objectKey);
			this.convertedTemplate.setHistoFileName(deviceSignature + "H" + Settings.GRAPHICS_TEMPLATES_EXTENSION.substring(Settings.GRAPHICS_TEMPLATES_EXTENSION.length() - 4));

			IDevice device = DataExplorer.application.getActiveDevice();
			int channelConfigNumber = DataExplorer.application.getActiveChannelNumber();
			channelMeasurements = device.getDeviceConfiguration().getChannelMeasuremts(channelConfigNumber);
		}

		/**
		 * Access to any graphics template file used by the legacy kernel.
		 * @param file
		 * @param deviceSignature - device signature as String (Picolario_K1)
		 * @param objectKey
		 */
		public LegacyGraphicsTemplate(File file, String deviceSignature, String objectKey) {
			this.templatePath = file.toPath().getParent().toString();
			this.legacyFileName = file.toPath().getFileName().toString();
			log.log(Level.FINE, "from file ", this);

			this.convertedTemplate = HistoGraphicsTemplate.createGraphicsTemplate(deviceSignature, objectKey);
			String pureFileName = legacyFileName.substring(0, legacyFileName.lastIndexOf("."));
			if (pureFileName.endsWith("H")) pureFileName = pureFileName.substring(0, pureFileName.length() - 1); // might result in conversion in situ
			this.convertedTemplate.setHistoFileName(pureFileName + "cnvH" + Settings.GRAPHICS_TEMPLATES_EXTENSION.substring(Settings.GRAPHICS_TEMPLATES_EXTENSION.length() - 4));

			IDevice device = DataExplorer.application.getActiveDevice();
			int channelConfigNumber = DataExplorer.application.getActiveChannelNumber();
			channelMeasurements = device.getDeviceConfiguration().getChannelMeasuremts(channelConfigNumber);
		}

		/**
		 * Load the legacy graphics template and convert.
		 * @return the converted histo graphics template
		 */
		HistoGraphicsTemplate convertToHistoTemplate() throws FileNotFoundException, IOException {
			File templateFile = Paths.get(templatePath, legacyFileName).toFile();
			try (FileInputStream stream = new FileInputStream(templateFile)) {
				loadFromXML(stream);
			}
			try {
				entrySet().stream().forEach(p -> convertedTemplate.setProperty(getPrefixedName((String) p.getKey()), (String) p.getValue()));
			} catch (Exception e) {
				log.log(SEVERE, e.getMessage(), e);
			}

			convertedTemplate.setCommentSuffix(templateFile.toPath().getFileName().toString() + " " + Instant.ofEpochMilli(templateFile.lastModified()));
			return convertedTemplate;
		}

		/**
		 * @param key example {@code 7_isVisible} or {@code RecordSet_horizontalGridColor}
		 * @return the prefixed key.</br>
		 *         Examples: {@code Tx__isVisible} , {@code RecordSet_horizontalGridColor}
		 */
		private String getPrefixedName(String key) {
			log.log(FINE, "key=", key);
			String[] splitKey = key.split(GDE.STRING_UNDER_BAR);
			String prefixedName;
			if (splitKey[0].isEmpty()) {
				prefixedName = key;
			} else {
				try {
					MeasurementType measurementType = channelMeasurements.get(Integer.parseInt(splitKey[0]));
					prefixedName = measurementType.getName() + GDE.STRING_UNDER_BAR + GDE.STRING_UNDER_BAR + splitKey[1];
				} catch (NumberFormatException e) {
					prefixedName = key;
				} catch (IndexOutOfBoundsException e) {
					prefixedName = "|Unk|" + key;
				} catch (Exception e) {
					prefixedName = "|Exc|" + key;
				}
			}
			return prefixedName;
		}

		@Override
		public synchronized String toString() {
			return "DefaultGraphicsTemplate [fileName=" + this.legacyFileName + ", templatePath=" + this.templatePath + "]";
		}
	}

	/**
	 * Use as file name the device signature with suffix "H".
	 * For object related templates use a sub directory.
	 * @param deviceSignature - device signature as String (Picolario_K1)
	 * @param objectKey
	 * @return an instance which conforms to the settings and the parameters
	 */
	public static HistoGraphicsTemplate createGraphicsTemplate(String deviceSignature, String objectKey) {
		if (!Settings.getInstance().getActiveObjectKey().isEmpty() && Settings.getInstance().isObjectTemplatesActive()) {
			return new ObjectGraphicsTemplate(deviceSignature, objectKey);
		} else if (Settings.getInstance().getActiveObjectKey().isEmpty() && Settings.getInstance().isObjectTemplatesActive()) {
			return new ObjectGraphicsTemplate(deviceSignature, GDE.STRING_DEVICE_ORIENTED_FOLDER);
		} else {
			return new StandardGraphicsTemplate(deviceSignature);
		}
	}

	protected final String	deviceSignature;
	protected final String	defaultHistoFileName;
	protected final String	defaultFileName;

	protected Path					currentFilePath;
	protected String				histoFileName;

	protected boolean				isAvailable		= false;
	protected boolean				isSaved				= false;	// indicates if template is saved to file
	protected String				commentSuffix	= "";

	/**
	 * Constructor using the application home path and the device signature as initialization parameter.
	 * Appends "H" to device signature (file name).
	 * @param deviceSignature - device signature as String (Picolario_K1)
	 */
	protected HistoGraphicsTemplate(String deviceSignature) {
		this.deviceSignature = deviceSignature;
		this.defaultFileName = deviceSignature + Settings.GRAPHICS_TEMPLATES_EXTENSION.substring(Settings.GRAPHICS_TEMPLATES_EXTENSION.length() - 4);
		this.defaultHistoFileName = deviceSignature + "H" + Settings.GRAPHICS_TEMPLATES_EXTENSION.substring(Settings.GRAPHICS_TEMPLATES_EXTENSION.length() - 4);
		this.setHistoFileName(defaultHistoFileName);
		log.log(FINE, "Histo graphics template file is ", this.defaultFileName);
	}

	public void setCommentSuffix(String commentSuffix) {
		this.commentSuffix = commentSuffix;
	}

	public boolean isAvailable() {
		return this.isAvailable;
	}

	public boolean isSaved() {
		return this.isSaved;
	}

	/**
	 * Load using standard procedures if the file path directory is equal to the target directory (based on object settings).
	 * Otherwise try loading w/o fall back to a default file and w/o conversion from legacy format.
	 * @param filePath is any path but is preferably an xml properties file in the same directory as the target directory
	 */
	public void load(Path filePath) {
		if (filePath.getParent().equals(getTargetFilePath().getParent())) {
			setHistoFileName(filePath.getFileName().toString());
			load();
		} else {
			// might result in template entries which are not usable (this rubbish remains even if the file is saved)
			try {
				currentFilePath = null;
				clear();
				loadFromXml(filePath.toFile());
				currentFilePath = filePath;
				setHistoFileName(filePath.getFileName().toString());
				this.isAvailable = true;
			} catch (InvalidPropertiesFormatException e) {
				log.log(SEVERE, e.getMessage(), e);
			} catch (Exception e) {
				log.log(WARNING, e.getMessage());
			}
		}
	}

	/**
	 * Load the properties from the template file.
	 * A template file with a graphics template format is imported.
	 * Take the graphics template file if not available.
	 */
	public void load() {
		try {
			currentFilePath = null;
			File file = getTargetFilePath().toFile();
			if (!file.exists()) {
				log.log(FINE, "convert legacy default template and store as a replacement for ", file.getAbsolutePath());
				LegacyGraphicsTemplate template = new LegacyGraphicsTemplate(deviceSignature, Settings.getInstance().getActiveObjectKey());
				HistoGraphicsTemplate convertedTemplate = template.convertToHistoTemplate();
				convertedTemplate.store();
				file = convertedTemplate.getTargetFilePath().toFile();

				clear();
				putAll(convertedTemplate);
			} else {
				clear();
				loadFromXml(file);
				boolean isHistoTemplate = keySet().stream().map(String.class::cast).anyMatch(k -> k.indexOf(GDE.STRING_UNDER_BAR + GDE.STRING_UNDER_BAR) >= 0);
				if (!isHistoTemplate) {
					log.log(FINE, "convert template identified as legacy template without storing ", file.getAbsolutePath());
					LegacyGraphicsTemplate template = new LegacyGraphicsTemplate(file, deviceSignature, Settings.getInstance().getActiveObjectKey());
					HistoGraphicsTemplate convertedTemplate = template.convertToHistoTemplate();

					clear();
					putAll(convertedTemplate);
				}
			}
			currentFilePath = file.toPath();
			this.isAvailable = true;
		} catch (InvalidPropertiesFormatException e) {
			log.log(SEVERE, e.getMessage(), e);
		} catch (Exception e) {
			log.log(WARNING, e.getMessage());
		}
	}

	protected void loadFromXml(File file) throws IOException, InvalidPropertiesFormatException, FileNotFoundException {
		log.log(FINE, "opening template file ", file.getAbsolutePath());
		try (FileInputStream stream = new FileInputStream(file)) {
			this.loadFromXML(stream);
			log.log(FINE, "template file successful loaded ", file.getPath());
		}
	}

	/**
	 * Store the properties to the histo template file.
	 */
	public void store() {
		try {
			currentFilePath = null;
			// check if templatePath exist, else create directory
			Path targetFilePath = getTargetFilePath();
			File tmpPath = targetFilePath.getParent().toFile();
			if (!tmpPath.exists()) {
				if (!tmpPath.mkdir()) {
					log.log(WARNING, "failed to create ", tmpPath);
				}
			}
			try (FileOutputStream stream = new FileOutputStream(targetFilePath.toFile())) {
				this.storeToXML(stream, "-- DataExplorer Histo GraphicsTemplate " + deviceSignature + " -- " + targetFilePath.getFileName().toString() + " " + ZonedDateTime.now().toInstant() + " -- " + commentSuffix);
			}
			currentFilePath = targetFilePath;
			this.isSaved = true;
		} catch (InvalidPropertiesFormatException e) {
			log.log(SEVERE, e.getMessage(), e);
		} catch (Exception e) {
			log.log(WARNING, e.getMessage(), e);
		}
	}

	public void setSaved(boolean newValue) {
		this.isSaved = newValue;
	}

	public void setHistoFileName(String fileName) {
		log.log(FINE, "fileName=", fileName);
		this.histoFileName = fileName;
	}

	public String getHistoFileName() {
		return this.histoFileName;
	}

	public String getDefaultHistoFileName() {
		return this.defaultHistoFileName;
	}

	/**
	 * @return the file path (templates for objects in sub directories)
	 */
	public abstract Path getTargetFilePath();

	/**
	 * @return the path residual compared to the standard template path
	 */
	@Nullable // if there was no successful load / store
	private Path getCurrentRelativeFilePath() {
		if (currentFilePath == null) {
			return Paths.get("");
		} else {
			Path relativePath;
			try {
				relativePath = Paths.get(Settings.getInstance().getGraphicsTemplatePath()).relativize(currentFilePath);
			} catch (IllegalArgumentException e) {
				relativePath = Paths.get("");
			}
			return relativePath;
		}
	}

	public String getFilePathMessage() {
		Path relativeFilePath = getCurrentRelativeFilePath();
		if (relativeFilePath != null && !relativeFilePath.getFileName().toString().equals(getDefaultHistoFileName())) {
			return relativeFilePath.toString();
		} else {
			return "";
		}
	}

	/**
	 * @return the default filename for the current device and channel configuration number
	 */
	public String getDefaultFileName() {
		return this.histoFileName;
	}

	/**
	 * @param recordName
	 * @param keyPostfix is one of the constant strings for record properties
	 * @param defaultValue is the default value in case the property does not exist
	 * @return the value in this property list with the specified key value.
	 */
	public String getRecordProperty(String recordName, String keyPostfix, String defaultValue) {
		return super.getProperty(recordName + GDE.STRING_UNDER_BAR + keyPostfix, defaultValue);
	}

	/**
	 * @param recordName
	 * @param keyPostfix is one of the constant strings for record properties
	 * @return the value in this property list with the specified key value.
	 */
	public String getRecordProperty(String recordName, String keyPostfix) {
		return super.getProperty(recordName + GDE.STRING_UNDER_BAR + keyPostfix);
	}

	/**
	 * @param recordName
	 * @param keyPostfix is one of the constant strings for record properties
	 * @param value the value corresponding to key.
	 * @return the previous value of the specified key in this property list
	 */
	@Nullable // if there was no previous value
	public synchronized String setRecordProperty(String recordName, String keyPostfix, String value) {
		return (String) super.setProperty(recordName + GDE.STRING_UNDER_BAR + keyPostfix, value);
	}

	@Override
	public synchronized String toString() {
		return "HistoGraphicsTemplate [defaultHistoFileName=" + this.defaultHistoFileName + ", defaultFileName=" + this.defaultFileName + ", histoFileName=" + this.histoFileName + ", commentSuffix=" + this.commentSuffix + "]";
	}

}
