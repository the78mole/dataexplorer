/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author Winfried Bruegmann 
 * class managing graphics visualization, store, restore
 */
public class GraphicsTemplate extends Properties {
	static final long			serialVersionUID	= 260357;
	private Logger				log								= Logger.getLogger(this.getClass().getName());
	private final String	fileSep						= System.getProperty("file.separator");

	private boolean				isAvailable				= false;
	private boolean				isSaved						= false;																				// indicates if template is saved to file
	private String				defaultFileName;
	private String				newFileName;
	private String				currentFileFilePath;
	private String				templatePath;
	private String				templateFilePath;

	/**
	 * constructor using the application home path and the device signature as initialization parameter
	 * @param full qualified application home path
	 * @param device signature as String (Picolario_K1)
	 */
	public GraphicsTemplate(String applHomePath, String deviceSignature) {
		this.templatePath = Settings.getInstance().getGraphicsTemplatePath();
		this.defaultFileName = deviceSignature + Settings.GRAPHICS_TEMPLATE_EXTENSION.substring(Settings.GRAPHICS_TEMPLATE_EXTENSION.length() - 4);
		this.templateFilePath = this.defaultFileName;
		log.fine("graphics template file is " + this.templateFilePath);
	}

	/**
	 * @return the isAvailable
	 */
	public boolean isAvailable() {
		return isAvailable;
	}

	/**
	 * @return the isSaved
	 */
	public boolean isSaved() {
		return isSaved;
	}

	/**
	 * load the properties from file
	 */
	public void load() {
		try {
			currentFileFilePath = this.templatePath + fileSep + ((this.newFileName == null) ? this.defaultFileName : this.newFileName);
			log.fine("opening template file " + currentFileFilePath);
			this.loadFromXML(new FileInputStream(new File(currentFileFilePath)));
			isAvailable = true;
			log.fine("template file successful loaded " + currentFileFilePath);
		}
		catch (InvalidPropertiesFormatException e) {
			log.severe(e.getMessage());
		}
		catch (FileNotFoundException e) {
			log.info(e.getMessage());
		}
		catch (IOException e) {
			log.warning(e.getMessage());
		}
	}

	/**
	 * store the properties to file
	 */
	public void store() {
		try {
			// check if templatePath exist, else create directory
			if (!(new File(this.templatePath).exists())) {
				File tmpPath = new File(this.templatePath);
				tmpPath.mkdir();
			}

			currentFileFilePath = this.templatePath + fileSep + ((this.newFileName == null) ? this.defaultFileName : this.newFileName);
			this.storeToXML(new FileOutputStream(new File(currentFileFilePath)), "-- OpenSerialDataExplorer GraphicsTemplate --");
			isSaved = true;
			newFileName = null;
		}
		catch (InvalidPropertiesFormatException e) {
			log.severe(e.getMessage());
		}
		catch (FileNotFoundException e) {
			log.info(e.getMessage());
		}
		catch (IOException e) {
			log.warning(e.getMessage());
		}
	}

	/**
	 * @param isSaved the isSaved to set
	 */
	public void setSaved(boolean isSaved) {
		this.isSaved = isSaved;
	}

	/**
	 * @return the newFileName
	 */
	public String getNewFileName() {
		return newFileName;
	}

	/**
	 * @param newFileName the newFileName to set
	 */
	public void setNewFileName(String newFileName) {
		this.newFileName = newFileName;
	}

	/**
	 * @return the usedFileName
	 */
	public String getCurrentFilePath() {
		return currentFileFilePath;
	}

}
