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
package osde.utils;

import java.text.DecimalFormat;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import osde.ui.SWTResourceManager;


/**
 * Class contains utility methods for drawing curves with scales right hand side and left hand side
 * @author Winfried Brügmann
 */
public class GraphicsUtils {
	private static Logger log = Logger.getLogger(GraphicsUtils.class.getName());

	/**
	 * draws tick marks to a scale in vertical direction (plus 90 degrees)
	 * @param gc graphics context
	 * @param y0 start point in y horizontal direction
	 * @param x0 start point in x vertical direction
	 * @param height in points where the ticks should be drawn
	 * @param startNumber the number where the scale should start to count
	 * @param endNumber the number where the scale should start to count, endNumber - startNumber -> number of ticks drawn
	 * @param ticklength of the main ticks
	 * @param miniticks number of mini ticks drawn between the main ticks
	 * @param gap distance between ticks and the number scale
	 * @param isPositionLeft position of to be drawn scale
	 * @param df - decimal format
	 */
	public static void drawVerticalTickMarks(GC gc, int x0, int y0, int height, double startNumber, double endNumber, int ticklength, int miniticks, int gap, boolean isPositionLeft, DecimalFormat df) {

		// enable scale value 0.0  -- algorithm must round algorithm
		int numberTicks = 10;
		double deltaScale = (endNumber - startNumber);
		if (startNumber < 0 && endNumber > 0) {
			if (deltaScale < 100) {
				numberTicks = (int)deltaScale / 5;
			}
			else if (deltaScale >= 100 && deltaScale <= 300) { //part below 0
				numberTicks = (int)deltaScale / 10;
			}
			else {
				numberTicks = (int)deltaScale / 20;
				if (numberTicks > 20) numberTicks = 20;
			}
		}

		int dist = 10;
		double deltaValue = deltaScale / numberTicks;
		double deltaTick = 1.0 * height / numberTicks;
		miniticks++;

		if (!isPositionLeft) {
			ticklength = ticklength * -1; // mirrow drwaing direction 
			gap = gap * -1;
			dist = dist * -1;
		}
		for (int i = 0; i <= numberTicks; i++) {
			//draw the main scale, length = 5 and gap to scale = 2
			int yPosition = (int) (y0 - i * deltaTick);
			log.finest("yPosition=" + yPosition);			
			gc.drawLine(x0, yPosition, x0 - ticklength, yPosition);
			//draw the sub scale according number of miniTicks
			int deltaPosMini = (int) (deltaTick / miniticks);
			for (int j = 1; j < miniticks && i < numberTicks; j++) {
				int yPosMini = yPosition - j * deltaPosMini;
				log.finest("yPosition=" + yPosition + ", xPosMini=" + yPosMini);
				gc.drawLine(x0, yPosMini, x0 - ticklength / 2, yPosMini);
			}
			//draw numbers to the scale	
			//String text = new Double(startNumber + i * deltaValue).toString().replace('.', ',').concat("00");
			drawText(df.format(startNumber + i * deltaValue), x0 - ticklength - gap - dist, (int) (y0 - i * deltaTick), gc, SWT.HORIZONTAL);
		}
	}

	/**
	 * Draws text horizontal or vertically (rotates plus or minus 90 degrees). Uses the current
	 * font, color, and background.
	 * <dl>
	 * <dt><b>Styles: </b></dt>
	 * <dd>UP, DOWN</dd>
	 * </dl>
	 * 
	 * @param string the text to draw
	 * @param x the x coordinate of the top left corner of the drawing rectangle
	 * @param y the y coordinate of the top left corner of the drawing rectangle
	 * @param gc the GC on which to draw the text
	 * @param style the style (SWT.UP or SWT.DOWN)
	 *          <p>
	 *          Note: Only one of the style UP or DOWN may be specified.
	 *          </p>
	 */
	public static void drawText(String string, int x, int y, GC gc, int style) {
		// Get the current display
		Display display = Display.getCurrent();
		if (display == null) SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);

		// Determine string's dimensions
		//FontMetrics fm = gc.getFontMetrics();
		Point pt = gc.textExtent(string);

		// Create an image the same size as the string
		Image stringImage = SWTResourceManager.getImage(pt.x, pt.y);

		// Create a GC so we can draw the image
		GC stringGc = new GC(stringImage);

		// Set attributes from the original GC to the new GC
		stringGc.setForeground(gc.getForeground());
		stringGc.setBackground(gc.getBackground());
		stringGc.setFont(gc.getFont());

		// Draw the text onto the image
		stringGc.drawText(string, 0, 0);

		boolean isHorizontal = (style & SWT.HORIZONTAL) == SWT.HORIZONTAL;
		if (isHorizontal) {
			// Draw the vertical image onto the original GC
			gc.drawImage(stringImage, x - pt.x / 2, y - pt.y / 2);
		}
		else {
			// Draw the image vertically onto the original GC
			drawVerticalImage(stringImage, x, y - pt.x / 2, gc, style);
		}

		// Dispose the new GC
		stringGc.dispose();

		// Dispose the image
		//stringImage.dispose();
	}

	/**
	 * Draws an image vertically (rotates plus or minus 90 degrees)
	 * <dl>
	 * <dt><b>Styles: </b></dt>
	 * <dd>UP, DOWN</dd>
	 * </dl>
	 * 
	 * @param image the image to draw
	 * @param x the x coordinate of the top left corner of the drawing rectangle
	 * @param y the y coordinate of the top left corner of the drawing rectangle
	 * @param gc the GC on which to draw the image
	 * @param style the style (SWT.UP or SWT.DOWN)
	 *          <p>
	 *          Note: Only one of the style UP or DOWN may be specified.
	 *          </p>
	 */
	public static void drawVerticalImage(Image image, int x, int y, GC gc, int style) {
		// Get the current display
		Display display = Display.getCurrent();
		if (display == null) SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);

		// Use the image's data to create a rotated image's data
		ImageData sd = image.getImageData();
		ImageData dd = new ImageData(sd.height, sd.width, sd.depth, sd.palette);

		// Determine which way to rotate, depending on up or down
		boolean up = (style & SWT.UP) == SWT.UP;
		int dx=0, dy=0;
		// Run through the horizontal pixels
		for (int sx = 0; sx < sd.width; sx++) {
			// Run through the vertical pixels
			for (int sy = 0; sy < sd.height; sy++) {
				// Determine where to move pixel to in destination image data
				dx = up ? sy : sd.height - sy - 1;
				dy = up ? sd.width - sx - 1 : sx;

				// Swap the x, y source data to y, x in the destination
				dd.setPixel(dx, dy, sd.getPixel(sx, sy));
			}
		}

		// Create the vertical image
		//Image vertical = new Image(display, dd);
		log.fine("imageData x = " + dd.width + " y = " + dd.height); 
		Image vertical = SWTResourceManager.getImage(dd);

		// Draw the vertical image onto the original GC
		gc.drawImage(vertical, x, y);

		// Dispose the vertical image
		//vertical.dispose();
	}

	/**
	 * Creates an image containing the specified text, rotated either plus or minus
	 * 90 degrees.
	 * <dl>
	 * <dt><b>Styles: </b></dt>
	 * <dd>UP, DOWN</dd>
	 * </dl>
	 * 
	 * @param text the text to rotate
	 * @param font the font to use
	 * @param foreground the color for the text
	 * @param background the background color
	 * @param style direction to rotate (up or down)
	 * @return Image
	 *         <p>
	 *         Note: Only one of the style UP or DOWN may be specified.
	 *         </p>
	 */
	public static Image createRotatedText(String text, Font font, Color foreground, Color background, int style) {
		// Get the current display
		Display display = Display.getCurrent();
		if (display == null) SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);

		// Create a GC to calculate font's dimensions
		GC gc = new GC(display);
		//gc.setFont(font);

		// Determine string's dimensions
		//FontMetrics fm = gc.getFontMetrics();
		Point pt = gc.textExtent(text);

		// Dispose that gc
		gc.dispose();

		// Create an image the same size as the string
		Image stringImage = SWTResourceManager.getImage(pt.x, pt.y);

		// Create a gc for the image
		gc = new GC(stringImage);
		//gc.setFont(font);
		gc.setForeground(foreground);
		gc.setBackground(background);

		// Draw the text onto the image
		gc.drawText(text, 0, 0);

		// Draw the image vertically onto the original GC
		Image image = createRotatedImage(stringImage, style);

		// Dispose the new GC
		gc.dispose();

		// Dispose the horizontal image
		//stringImage.dispose();

		// Return the rotated image
		return image;
	}

	/**
	 * Creates a rotated image (plus or minus 90 degrees)
	 * <dl>
	 * <dt><b>Styles: </b></dt>
	 * <dd>UP, DOWN</dd>
	 * </dl>
	 * 
	 * @param image the image to rotate
	 * @param style direction to rotate (up or down)
	 * @return Image
	 *         <p>
	 *         Note: Only one of the style UP or DOWN may be specified.
	 *         </p>
	 */
	public static Image createRotatedImage(Image image, int style) {
		// Get the current display
		Display display = Display.getCurrent();
		if (display == null) SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);

		// Use the image's data to create a rotated image's data
		ImageData sd = image.getImageData();
		ImageData dd = new ImageData(sd.height, sd.width, sd.depth, sd.palette);

		// Determine which way to rotate, depending on up or down
		boolean up = (style & SWT.UP) == SWT.UP;
		int dx = 0, dy = 0;
		// Run through the horizontal pixels
		for (int sx = 0; sx < sd.width; sx++) {
			// Run through the vertical pixels
			for (int sy = 0; sy < sd.height; sy++) {
				// Determine where to move pixel to in destination image data
				dx = up ? sy : sd.height - sy - 1;
				dy = up ? sd.width - sx - 1 : sx;

				// Swap the x, y source data to y, x in the destination
				dd.setPixel(dx, dy, sd.getPixel(sx, sy));
			}
		}

		// Create the vertical image
		return SWTResourceManager.getImage(dx, dy);
	}
}
