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
    
    Copyright (c) 2016,2017 Thomas Eickert
****************************************************************************************/
package gde.config;

import gde.GDE;
import gde.log.Level;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Class managing histo graphics visualization, store, restore.
 * @author Thomas Eickert
 */
public class HistoGraphicsTemplate extends Properties {
	final static String	$CLASS_NAME				= HistoGraphicsTemplate.class.getName();
	final static Logger	log								= Logger.getLogger($CLASS_NAME);
	static final long		serialVersionUID	= 2088159376716311896L;

	private boolean			isAvailable				= false;
	private boolean			isSaved						= false;																// indicates if template is saved to file
	private String			defaultFileName;
	private String			histoFileName;
	private String			currentFileFilePath;
	private String			templatePath;
	private String			templateFilePath;

	/**
	 * constructor using the application home path and the device signature as initialization parameter.
	 * pure copy from GraphicsTemplate (no change, same XSD).
	 * appends "H" to device signature (file name).
	 * @param deviceSignature - device signature as String (Picolario_K1)
	 */
	public HistoGraphicsTemplate(String deviceSignature) {
		this.templatePath = Settings.getInstance().getGraphicsTemplatePath();
		this.defaultFileName = deviceSignature + Settings.GRAPHICS_TEMPLATES_EXTENSION.substring(Settings.GRAPHICS_TEMPLATES_EXTENSION.length() - 4);
		this.templateFilePath = this.defaultFileName;
		this.setHistoFileName(deviceSignature + "H" + Settings.GRAPHICS_TEMPLATES_EXTENSION.substring(Settings.GRAPHICS_TEMPLATES_EXTENSION.length() - 4));
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Histo graphics template file is " + this.templateFilePath); //$NON-NLS-1$
	}

	/**
	 * @return the isAvailable
	 */
	public boolean isAvailable() {
		return this.isAvailable;
	}

	/**
	 * @return the isSaved
	 */
	public boolean isSaved() {
		return this.isSaved;
	}

	/**
	 * load the properties from the histo template file.
	 * take the graphics template file if not available.
	 */
	public void load() {
		try {
			this.currentFileFilePath = this.templatePath + GDE.FILE_SEPARATOR_UNIX + this.histoFileName;
			File file = new File(this.currentFileFilePath);
			if (file.exists() && !file.isDirectory()) {
				// histo template is already available
			}
			else {
				file = new File(this.templatePath + GDE.FILE_SEPARATOR_UNIX + this.defaultFileName);
			}
			log.log(Level.OFF, "opening template file " + file.getAbsolutePath()); //$NON-NLS-1$
			this.loadFromXML(new FileInputStream(file));
			this.isAvailable = true;
			log.log(Level.OFF, "template file successful loaded " + this.currentFileFilePath); //$NON-NLS-1$
		}
		catch (InvalidPropertiesFormatException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage());
		}
	}

	/**
	 * store the properties to the histo template file
	 */
	public void store() {
		try {
			// check if templatePath exist, else create directory
			File tmpPath = new File(this.templatePath);
			if (!tmpPath.exists()) {
				if (!tmpPath.mkdir()) {
					log.log(Level.WARNING, "failed to create " + tmpPath);
				}
			}

			this.currentFileFilePath = this.templatePath + GDE.FILE_SEPARATOR_UNIX + this.histoFileName;
			this.storeToXML(new FileOutputStream(new File(this.currentFileFilePath)), "-- DataExplorer GraphicsTemplate --"); //$NON-NLS-1$
			this.isSaved = true;
		}
		catch (InvalidPropertiesFormatException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}

	/**
	 * @param newValue the isSaved to set
	 */
	public void setSaved(boolean newValue) {
		this.isSaved = newValue;
	}

	/**
	 * @return the newFileName
	 */
	public String getNewFileName() {
		return this.histoFileName;
	}

	/**
	 * @param fileName the newFileName to set
	 */
	public void setHistoFileName(String fileName) {
		this.histoFileName = fileName;
	}

	/**
	 * @return the usedFileName
	 */
	public String getCurrentFilePath() {
		return this.currentFileFilePath;
	}

	/**
	 * @return the default filename for the current device and channel configuration number
	 */
	public String getDefaultFileName() {
		return this.histoFileName;
	}

}
