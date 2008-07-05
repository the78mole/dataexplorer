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
package osde.utils;

import java.text.DecimalFormat;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import osde.data.Record;
import osde.data.RecordSet;
import osde.ui.SWTResourceManager;


/**
 * Class contains utility methods for drawing curves with scales right hand side and left hand side
 * @author Winfried Brügmann
 */
public class GraphicsUtils {
	private static Logger log = Logger.getLogger(GraphicsUtils.class.getName());

	/**
	 * draws tick marks to a scale in vertical direction (plus 90 degrees)
	 * @param record
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
	public static void drawVerticalTickMarks(Record record, GC gc, int x0, int y0, int height, double startNumber, double endNumber, int ticklength, int miniticks, int gap, boolean isPositionLeft, DecimalFormat df) {

		// enable scale value 0.0  -- algorithm must round algorithm
		int numberTicks = 10;
		double deltaScale = (endNumber - startNumber);
		if (startNumber < 0 && endNumber > 0) {
			if (deltaScale < 1) {
				numberTicks = (int)(deltaScale * 20 / 1);
			}
			else if (deltaScale < 2) {
				numberTicks = (int)(deltaScale+0.5) * 10 / 1;
			}
			else if (deltaScale < 5) {
				numberTicks = (int)(deltaScale+1) * 5 / 1;
			}
			else if (deltaScale < 10) {
				numberTicks = (int)(deltaScale * 2);
			}
			else if (deltaScale < 50) {
				numberTicks = (int)(deltaScale/2.5);
			}
			else if (deltaScale < 100) {
				numberTicks = (int)(deltaScale / 5);
			}
			else if (deltaScale < 300) {
				numberTicks = (int)(deltaScale / 10);
			}
			else {
				numberTicks = (int)(deltaScale / 20);
				if (numberTicks > 20) numberTicks = 20;
			}
		}
		
		// prepare grid vector
		Vector<Integer> horizontalGrid = new Vector<Integer>();
		RecordSet recordSet = record.getParent();
		boolean isBuildGridVector = recordSet.getHorizontalGridType() != RecordSet.HORIZONTAL_GRID_NONE && recordSet.getHorizontalGridRecordName().equals(record.getName());
		
		int dist = 10;
		double deltaValue = deltaScale / numberTicks;
		double deltaTick = 1.0 * height / numberTicks;
		miniticks++;

		if (!isPositionLeft) {
			ticklength = ticklength * -1; // mirror drawing direction 
			gap = gap * -1;
			dist = dist * -1;
		}
		for (int i = 0; i <= numberTicks; i++) {
			//draw the main scale, length = 5 and gap to scale = 2
			int yPosition = (int) (y0 - i * deltaTick);
			gc.drawLine(x0, yPosition, x0 - ticklength, yPosition);
			if (i != 0 && isBuildGridVector) horizontalGrid.add(yPosition);
			//draw the sub scale according number of miniTicks
			int deltaPosMini = (int) (deltaTick / miniticks);
			for (int j = 1; j < miniticks && i < numberTicks; j++) {
				int yPosMini = yPosition - j * deltaPosMini;
				if(log.isLoggable(Level.FINEST)) log.finest("yPosition=" + yPosition + ", xPosMini=" + yPosMini); //$NON-NLS-1$ //$NON-NLS-2$
				gc.drawLine(x0, yPosMini, x0 - ticklength / 2, yPosMini);
			}
			//draw numbers to the scale	
			drawText(df.format(startNumber + i * deltaValue), x0 - ticklength - gap - dist, (int) (y0 - i * deltaTick), gc, SWT.HORIZONTAL);
		}
		if (isBuildGridVector) recordSet.setHorizontalGrid(horizontalGrid);
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
		Point pt = gc.textExtent(string);

		// Create an image the same size as the string
		Image stringImage = SWTResourceManager.getImage(pt.x, pt.y);

		// Create a GC so we can draw the image
		GC stringGc = SWTResourceManager.getGC(stringImage);

		// Set attributes from the original GC to the new GC
		stringGc.setForeground(gc.getForeground());
		stringGc.setBackground(gc.getBackground());
		stringGc.setFont(gc.getFont());

		// clear the image
		stringGc.fillRectangle(0, 0, pt.x, pt.y);
		// draw the text onto the image
		stringGc.drawText(string, 0, 0);

		boolean isHorizontal = (style & SWT.HORIZONTAL) == SWT.HORIZONTAL;
		if (isHorizontal) {
			// Draw the horizontally image onto the original GC
			gc.drawImage(stringImage, x - pt.x / 2, y - pt.y / 2);
		}
		else {
			// Draw the image vertically onto the original GC
			drawVerticalImage(stringImage, x, y - pt.x / 2, gc, style, string);
		}
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
	public static void drawVerticalImage(Image image, int x, int y, GC gc, int style, String imgKey) {
		// Get the current display
		Display display = Display.getCurrent();
		if (display == null) SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);

		// Draw the vertical image onto the original GC
		gc.drawImage(SWTResourceManager.getRotatedImage(image, style, imgKey), x, y);
	}
}
