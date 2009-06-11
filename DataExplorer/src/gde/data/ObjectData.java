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
package osde.data;

import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextPrintOptions;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.printing.Printer;

import osde.OSDE;
import osde.io.ObjectDataReaderWriter;
import osde.ui.SWTResourceManager;

/**
 * @author Winfried Brügmann
 * Class to handle object relevant data, like object key, object description, object type, object image, ....
 */
public class ObjectData {
	/**
	 * 
	 */
	public static final String	STRING_STYLED_TEXT_DEFAULT	= "Hersteller\t\t:\nHändler\t\t\t:\nPreis\t\t\t\t:\nBauzeit\t\t\t:\n .....\t\t\t\t\t:\n";

	final static Logger	log	= Logger.getLogger(ObjectData.class.getName());

	public static final String	STRING_UNKNOWN	= "unknown";

	String							key;
	String							type;
	String							activationDate;
	String							status;
	Image								image;
	static int					imageWidth = 400;
	static int					imageHeight = 300;
	String							styledText;
	StyleRange[]				styleRanges;
	Font								font;
	String							fullQualifiedObjectFilePath;

	/**
	 * Constructor for basic initialisation
	 * @param newKey
	 * @param newType
	 * @param newActivationDate
	 * @param newStatus
	 * @param newImage
	 * @param newStyledText
	 * @param newStyleRanges
	 */
	public ObjectData(String newKey, String newType, String newActivationDate, String newStatus, Image newImage, String newStyledText, StyleRange[] newStyleRanges, FontData fd) {
		this.key = newKey;
		this.type = newType;
		this.activationDate = newActivationDate;
		this.status = newStatus;
		this.image = newImage;
		this.styledText = newStyledText;
		this.styleRanges = newStyleRanges.clone();
		this.font = SWTResourceManager.getFont(fd.getName(), fd.getHeight(), fd.getStyle(), false, false);
	}

	/**
	 * Constructor for default initialisation
	 * @param objectFilePath
	 */
	public ObjectData(String objectFilePath) {
		this.fullQualifiedObjectFilePath = objectFilePath;
		this.key = objectFilePath.replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX).substring(objectFilePath.lastIndexOf(OSDE.FILE_SEPARATOR_UNIX) + 1, objectFilePath.lastIndexOf(OSDE.STRING_DOT));
		this.key = this.key.contains(OSDE.STRING_DOT) ? this.key.substring(0, this.key.indexOf(OSDE.STRING_DOT)) : this.key;
		this.type = ObjectData.STRING_UNKNOWN;
		this.activationDate = ObjectData.STRING_UNKNOWN;
		this.status = ObjectData.STRING_UNKNOWN;
		this.image = null;
		this.styledText = ObjectData.STRING_STYLED_TEXT_DEFAULT;
		this.styleRanges = new StyleRange[] { new StyleRange(0, this.styledText.length(), null, null, SWT.BOLD) };
		this.font = OSDE.IS_WINDOWS ? SWTResourceManager.getFont("Microsoft Sans Serif", 10, SWT.NORMAL, false, false) : SWTResourceManager.getFont("Sans Serif", 10, SWT.NORMAL, false, false);
	}

	/**
	 * copy constructor
	 */
	public ObjectData(ObjectData objectData) {
		this.key = objectData.key;
		this.type = objectData.type;
		this.activationDate = objectData.activationDate;
		this.status = objectData.status;
		this.image = SWTResourceManager.getImage(objectData.image.getImageData(), this.key, imageWidth, imageHeight, false);
		this.styledText = objectData.styledText;
		Vector<StyleRange> tmpRanges = new Vector<StyleRange>();
		for (StyleRange range : objectData.styleRanges) {
			Color foreground = range.foreground != null ? SWTResourceManager.getColor(range.foreground.getRed(), range.foreground.getGreen(), range.foreground.getBlue()) : null;
			Color background = range.background != null ? SWTResourceManager.getColor(range.background.getRed(), range.background.getGreen(), range.background.getBlue()) : null;
			tmpRanges.add(new StyleRange(range.start, range.length, foreground, background, range.fontStyle));
		}
		this.styleRanges = tmpRanges.toArray(new StyleRange[0]);
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return this.key;
	}

	/**
	 * @param newKey the key to set
	 */
	public void setKey(String newKey) {
		this.key = newKey;
	}

	/**
	 * @return the activationDate
	 */
	public String getActivationDate() {
		return this.activationDate;
	}

	/**
	 * @param newActivationDate the activationDate to set
	 */
	public void setActivationDate(String newActivationDate) {
		this.activationDate = newActivationDate;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return this.status;
	}

	/**
	 * @param newStatus the status to set
	 */
	public void setStatus(String newStatus) {
		this.status = newStatus;
	}

	/**
	 * @return the image
	 */
	public Image getImage() {
		return this.image;
	}

	/**
	 * @param newImage the image to set
	 */
	public void setImage(Image newImage) {
		this.image = newImage;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * @param newType the type to set
	 */
	public void setType(String newType) {
		this.type = newType;
	}

	/**
	 * @return the styledText
	 */
	public String getStyledText() {
		return this.styledText;
	}

	/**
	 * @param newStyledText the styledText to set
	 */
	public void setStyledText(String newStyledText) {
		this.styledText = newStyledText;
	}

	/**
	 * @return the style ranges
	 */
	public StyleRange[] getStyleRanges() {
		return this.styleRanges;
	}

	/**
	 * @param newStyleRanges the style ranges to set
	 */
	public void setStyleRanges(StyleRange[] newStyleRanges) {
		this.styleRanges = newStyleRanges;
	}

	public void save() {
		ObjectDataReaderWriter objWriter = new ObjectDataReaderWriter(this);
		objWriter.write();
	}

	public void load() {
		ObjectDataReaderWriter objReader = new ObjectDataReaderWriter(this);
		objReader.read();
	}

	public void print(String objectFilePath, StyledText prtStyledText) {
		StyledTextPrintOptions options = new StyledTextPrintOptions();
		options.header = StyledTextPrintOptions.SEPARATOR + objectFilePath + StyledTextPrintOptions.SEPARATOR;
		options.footer = StyledTextPrintOptions.SEPARATOR + StyledTextPrintOptions.PAGE_TAG + StyledTextPrintOptions.SEPARATOR + OSDE.OSDE_NAME_LONG;
		options.printLineBackground = true;
		options.printTextBackground = true;
		options.printTextFontStyle = true;
		options.printTextForeground = true;

		prtStyledText.print(new Printer()).run();
	}

	/**
	 * @return the imageWidth
	 */
	public int getImageWidth() {
		return imageWidth;
	}

	/**
	 * @param newWidth the imageWidth to set
	 */
	public void setImageWidth(int newWidth) {
		imageWidth = newWidth;
	}

	/**
	 * @return the imageHeight
	 */
	public int getImageHeight() {
		return imageHeight;
	}

	/**
	 * @param newHeight the imageHeight to set
	 */
	public void setImageHeight(int newHeight) {
		imageHeight = newHeight;
	}

	/**
	 * @return the fontData
	 */
	public FontData getFontData() {
		return this.font != null ? this.font.getFontData()[0] : SWTResourceManager.getFont("Sans Serif", 10, SWT.NORMAL, false, false).getFontData()[0];
	}

	/**
	 * @param newFont the fontData to set
	 */
	public void setFont(Font newFont) {
		this.font = newFont;
	}

	/**
	 * @return the fullQualifiedObjectFilePath
	 */
	public String getFullQualifiedObjectFilePath() {
		return this.fullQualifiedObjectFilePath;
	}
}
