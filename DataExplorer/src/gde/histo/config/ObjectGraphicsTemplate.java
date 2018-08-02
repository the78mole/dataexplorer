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

    Copyright (c) 2017,2018 Thomas Eickert
****************************************************************************************/

package gde.histo.config;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.InvalidPropertiesFormatException;

import gde.GDE;
import gde.config.Settings;

/**
 * Supports individual device channel templates for objects in sub directories.
 * @author Thomas Eickert (USER)
 */
public final class ObjectGraphicsTemplate extends HistoGraphicsTemplate {
	private static final long	serialVersionUID	= -4197176848694996415L;
	private final String			objectFolderName;

	/**
	 * Constructor using the application home path and the device signature as initialization parameter.
	 * @param deviceSignature - device signature as String (Picolario_K1)
	 */
	protected ObjectGraphicsTemplate(String deviceSignature, String objectFolderName) {
		super(deviceSignature);
		this.objectFolderName = objectFolderName;
	}

	@Override
	public Path getTargetFilePath() {
		String fileName = histoFileName == null || histoFileName.equals(GDE.STRING_EMPTY) ? defaultHistoFileName : histoFileName;
		return Paths.get(Settings.getInstance().getGraphicsTemplatePath(), objectFolderName, fileName);
	}

	/**
	 * Load the properties from the object template file.
	 * Alternatively browse the default path for a valid file.
	 */
	@Override
	public void load() {
		File file = getTargetFilePath().toFile();
		try {
			currentFilePath = null;
			if (!file.exists()) {
				super.load();
			} else {
				this.clear();
				loadFromXml(file);
				currentFilePath = file.toPath();
			}
			this.isAvailable = true;
		} catch (InvalidPropertiesFormatException e) {
			log.log(SEVERE, e.getMessage(), e);
		} catch (Exception e) {
			log.log(WARNING, e.getMessage());
		}
	}

	/**
	 * Store the properties to the object template file.
	 */
	@Override
	public void store() {
		try {
			currentFilePath = null;
			Path targetFilePath = getTargetFilePath();
			File tmpPath = targetFilePath.getParent().toFile();
			if (!tmpPath.exists()) {
				if (!tmpPath.mkdir()) {
					log.log(WARNING, "failed to create ", tmpPath);
				}
			}
			try (FileOutputStream stream = new FileOutputStream(targetFilePath.toFile())) {
				this.storeToXML(stream, "-- DataExplorer ObjectGraphicsTemplate " + deviceSignature + GDE.STRING_UNDER_BAR + objectFolderName + " -- " + targetFilePath.getFileName().toString() + " " + ZonedDateTime.now().toInstant() + " -- " + commentSuffix);
			}
			currentFilePath = targetFilePath;
			this.isSaved = true;
		} catch (InvalidPropertiesFormatException e) {
			log.log(SEVERE, e.getMessage(), e);
		} catch (Exception e) {
			log.log(WARNING, e.getMessage(), e);
		}
	}

}
