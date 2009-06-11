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
package osde.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;

import osde.OSDE;
import osde.data.ObjectData;
import osde.exception.ApplicationConfigurationException;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.StringHelper;

/**
 * @author Winfried Brügmann
 * This class provides functionality to save and load object describing data
 */
public class ObjectDataReaderWriter {
	private static final String	LINE_DELIMITER				= "{$}";
	private static final String	DELIMITER							= "{%}";

	final static Logger					log										= Logger.getLogger(ObjectDataReaderWriter.class.getName());

	private static final String	BEGIN_STYLES					= "{BeginStyles}";
	private static final String	END_STYLES						= "{EndStyles}";
	private static final String	BEGIN_HEADER					= "{BeginHeader}";
	private static final String	BEGIN_CHARACTERISTICS	= "{BeginCharacteristics}";
	private static final String	BEGIN_STYLED_TEXT			= "{BeginStyledText}";
	private static final String	BEGIN_FONT						= "{BeginFont}";

	private ObjectData					objectData;
	private String							filePath;

	public ObjectDataReaderWriter(ObjectData newObjectData) {
		this.objectData = newObjectData;
		this.filePath = newObjectData.getFullQualifiedObjectFilePath();
	}

	@SuppressWarnings("unchecked")
	public void read() {

		String redObjectkey = ObjectData.STRING_UNKNOWN;

		File file = new File(this.filePath);
		if (file.exists()) {
			try {
				ZipFile zipFile = new ZipFile(file);
				//this.consolePrinter = new PrintStream(System.out, false, Constants.writeCodePage); 
				//-Dfile.encoding=UTF-8

				Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipFile.entries();

				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();

					if (entry.getName().endsWith(OSDE.FILE_ENDING_DOT_RTF)) {
						String[] content = StringHelper.splitString(extract(zipFile.getInputStream(entry)), ObjectDataReaderWriter.LINE_DELIMITER, OSDE.STRING_EMPTY);

						redObjectkey = content[0].substring(ObjectDataReaderWriter.BEGIN_HEADER.length());
						if (!this.objectData.getKey().equals(redObjectkey)) {
							throw new ApplicationConfigurationException("Missmatch object key versus file content detected !");
						}

						// main characteristics
						for (int i = 1; i < content.length; i++) {
							if (content[i].contains(ObjectDataReaderWriter.BEGIN_CHARACTERISTICS)) {
								String[] tmpCharacteristics = StringHelper.splitString(content[i], ObjectDataReaderWriter.DELIMITER, ObjectDataReaderWriter.BEGIN_CHARACTERISTICS);
								this.objectData.setType(tmpCharacteristics[0]);
								this.objectData.setActivationDate(tmpCharacteristics[1]);
								this.objectData.setStatus(tmpCharacteristics[2]);
								break;
							}
						}
						// font
						for (int i = 1; i < content.length; i++) {
							if (content[i].contains(ObjectDataReaderWriter.BEGIN_FONT)) {
								String[] tmpFontData = StringHelper.splitString(content[i], ObjectDataReaderWriter.DELIMITER, ObjectDataReaderWriter.BEGIN_FONT);
								this.objectData.setFont(SWTResourceManager.getFont(tmpFontData[0], Integer.parseInt(tmpFontData[1]), Integer.parseInt(tmpFontData[2]), false, false));
								break;
							}
						}					
						// styled text
						for (int i = 1; i < content.length; i++) {
							if (content[i].contains(ObjectDataReaderWriter.BEGIN_STYLED_TEXT)) {
								this.objectData.setStyledText(content[i].substring(ObjectDataReaderWriter.BEGIN_STYLED_TEXT.length()));
								break;
							}
						}
						//style ranges
						Vector<StyleRange> tmpRanges = new Vector<StyleRange>();
						for (int i = 1; i < content.length; i++) {
							String line = content[i];
							if (line.contains(ObjectDataReaderWriter.BEGIN_STYLES)) {
								String[] ranges = StringHelper.splitString(line, ObjectDataReaderWriter.DELIMITER, ObjectDataReaderWriter.BEGIN_STYLES);
								for (String styleRangeText : ranges) {
									if (styleRangeText.contains(ObjectDataReaderWriter.END_STYLES)) break;
									tmpRanges.add(buildOneStyle(styleRangeText));
								}
							}
							if (line.contains(ObjectDataReaderWriter.END_STYLES)) break;
						}
						this.objectData.setStyleRanges(tmpRanges.toArray(new StyleRange[0]));
					}
					else if (entry.getName().endsWith(OSDE.FILE_ENDING_DOT_JPG)) {
						//image
						String imageKey = entry.getName().substring(0, entry.getName().length() - OSDE.FILE_ENDING_DOT_JPG.length());
						InputStream inZip = zipFile.getInputStream(entry);
						ImageLoader imageLoader = new ImageLoader();
						this.objectData.setImage(SWTResourceManager.getImage(imageLoader.load(inZip)[0], imageKey, 400, 300, true));
						inZip.close();
					}
					else {
						ObjectDataReaderWriter.log.log(Level.WARNING, entry.getName());
					}
				}
			}
			catch (IOException e) {
				ObjectDataReaderWriter.log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
			catch (Throwable t) {
				ObjectDataReaderWriter.log.log(Level.SEVERE, t.getLocalizedMessage(), t);
				if (t instanceof ZipException) {
					int answer = OpenSerialDataExplorer.getInstance().openYesNoMessageDialog("Object file " + file.getAbsolutePath() + " seams invalid, should it be deleted ?");
					if(answer == SWT.YES) file.delete();	
				}
				else if (t instanceof ApplicationConfigurationException) {
					if (OpenSerialDataExplorer.getInstance().isVisible()) {
						String msg = "Object file " + file.getAbsolutePath() + " contains invalid object key \"" + redObjectkey + "\", should it be deleted ?";
						int answer = OpenSerialDataExplorer.getInstance().openYesNoMessageDialog(msg);
						if (answer == SWT.YES) file.delete();
					}	
					else {
						String msg = "Object file " + file.getAbsolutePath() + " contains invalid object key \"" + redObjectkey + "\", please check ?";
						OSDE.setInitError(msg);
					}
				}
			}
		}
	}

	/**
	 * extract the whole file content and return as string
	 * @param inZip zip file input stream
	 * @return file content and return as string
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private String extract(InputStream inZip) throws FileNotFoundException, IOException {
		StringBuffer sb = new StringBuffer();
		byte[] buffer = new byte[2048];
		int len;
		while ((len = inZip.read(buffer)) >= 0) {
			byte[] stringBuffer = new byte[len];
			System.arraycopy(buffer, 0, stringBuffer, 0, len);
			sb.append(new String(stringBuffer));
		}
		new String(buffer);
		inZip.close();
		return sb.toString();
	}

	/**
	 * construncts a style range from the input string
	 * colors will returned as null if string does not contain r,g,b color info
	 * any other numberformat exception will return a default style range
	 * @param styleRangeString
	 * @return
	 */
	private StyleRange buildOneStyle(String styleRangeString) {
		int startPos;
		int length;
		Color foreground;
		Color background;
		int textStyle;
		try {
			String[] styleRangeData = styleRangeString.split(OSDE.STRING_BLANK);
			startPos = Integer.parseInt(styleRangeData[0]);
			length = Integer.parseInt(styleRangeData[1]);
			foreground = null;
			try {
				foreground = SWTResourceManager.getColor(Integer.parseInt(styleRangeData[2]), Integer.parseInt(styleRangeData[3]), Integer.parseInt(styleRangeData[4]));
			}
			catch (NumberFormatException e) {
				foreground = null;
			}
			background = null;
			try {
				background = SWTResourceManager.getColor(Integer.parseInt(styleRangeData[5]), Integer.parseInt(styleRangeData[6]), Integer.parseInt(styleRangeData[7]));
			}
			catch (NumberFormatException e) {
				background = null;
			}
			textStyle = Integer.parseInt(styleRangeData[8]);
		}
		catch (NumberFormatException e) {
			startPos = 0;
			length = 0;
			foreground = null;
			background = null;
			textStyle = SWT.NORMAL;
		}
		return new StyleRange(startPos, length, foreground, background, textStyle);
	}

	/**
	 * write conten of ObjectData to a zip file
	 */
	public void write() {
		try {
			File targetFile = new File(this.filePath);
			if (targetFile.exists()) targetFile.delete();

			ZipOutputStream outZip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(targetFile)));

			// save the image
			ImageLoader imageLoader = new ImageLoader();
			imageLoader.data = new ImageData[] { this.objectData.getImage().getImageData() };
			outZip.putNextEntry(new ZipEntry(this.objectData.getKey() + OSDE.FILE_ENDING_DOT_JPG));
			imageLoader.save(outZip, SWT.IMAGE_JPEG);
			outZip.closeEntry();

			//save the text document
			outZip.putNextEntry(new ZipEntry(this.objectData.getKey() + OSDE.FILE_ENDING_DOT_RTF));

			String text = this.objectData.getKey();
			write(outZip, ObjectDataReaderWriter.BEGIN_HEADER + text + ObjectDataReaderWriter.LINE_DELIMITER);
			ObjectDataReaderWriter.log.log(Level.INFO, ObjectDataReaderWriter.BEGIN_HEADER + text + ObjectDataReaderWriter.LINE_DELIMITER);
			
			text = this.objectData.getType() + ObjectDataReaderWriter.DELIMITER + this.objectData.getActivationDate() + ObjectDataReaderWriter.DELIMITER + this.objectData.getStatus();
			write(outZip, ObjectDataReaderWriter.BEGIN_CHARACTERISTICS + text + ObjectDataReaderWriter.LINE_DELIMITER);
			ObjectDataReaderWriter.log.log(Level.INFO, ObjectDataReaderWriter.BEGIN_CHARACTERISTICS + text + ObjectDataReaderWriter.LINE_DELIMITER);

			FontData fd = this.objectData.getFontData();
			text = fd.getName() + DELIMITER + fd.getHeight() + DELIMITER + fd.getStyle();
			write(outZip, ObjectDataReaderWriter.BEGIN_FONT + text + ObjectDataReaderWriter.LINE_DELIMITER);
			ObjectDataReaderWriter.log.log(Level.INFO, ObjectDataReaderWriter.BEGIN_FONT + text + ObjectDataReaderWriter.LINE_DELIMITER);

			text = this.objectData.getStyledText();
			write(outZip, ObjectDataReaderWriter.BEGIN_STYLED_TEXT + text + ObjectDataReaderWriter.LINE_DELIMITER);
			ObjectDataReaderWriter.log.log(Level.INFO, ObjectDataReaderWriter.BEGIN_STYLED_TEXT + text + ObjectDataReaderWriter.LINE_DELIMITER);

			write(outZip, ObjectDataReaderWriter.BEGIN_STYLES);
			ObjectDataReaderWriter.log.log(Level.INFO, ObjectDataReaderWriter.BEGIN_STYLES);
			StyleRange[] styles = this.objectData.getStyleRanges();
			for (StyleRange style : styles) {
				text = style.start + OSDE.STRING_BLANK + style.length + OSDE.STRING_BLANK + (style.foreground == null ? OSDE.STRING_DASH : style.foreground.getRed()) + OSDE.STRING_BLANK
						+ (style.foreground == null ? OSDE.STRING_DASH : style.foreground.getGreen()) + OSDE.STRING_BLANK + (style.foreground == null ? OSDE.STRING_DASH : style.foreground.getBlue())
						+ OSDE.STRING_BLANK + (style.background == null ? OSDE.STRING_DASH : style.background.getRed()) + OSDE.STRING_BLANK
						+ (style.background == null ? OSDE.STRING_DASH : style.background.getGreen()) + OSDE.STRING_BLANK + (style.background == null ? OSDE.STRING_DASH : style.background.getBlue())
						+ OSDE.STRING_BLANK + style.fontStyle + ObjectDataReaderWriter.DELIMITER;
				write(outZip, text);
				ObjectDataReaderWriter.log.log(Level.INFO, text);
			}
			write(outZip, ObjectDataReaderWriter.END_STYLES + OSDE.STRING_NEW_LINE);
			ObjectDataReaderWriter.log.log(Level.INFO, ObjectDataReaderWriter.END_STYLES);

			outZip.flush();
			outZip.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * helper method to write the zip entry bytes
	 * @param outZip
	 * @param text
	 * @throws IOException
	 */
	void write(ZipOutputStream outZip, String text) throws IOException {
		byte[] buffer = text.getBytes();
		int length = buffer.length;
		outZip.write(buffer, 0, length);
	}
}
