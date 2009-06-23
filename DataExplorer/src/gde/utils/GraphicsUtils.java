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
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;


/**
 * Class contains utility methods for drawing curves with scales right hand side and left hand side
 * @author Winfried Brügmann
 */
public class GraphicsUtils {
	private static Logger log = Logger.getLogger(GraphicsUtils.class.getName());
	
	static OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();

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
		double heightAdaptation = height / 350.0;
		int numberTicks = new Double(10 * heightAdaptation).intValue();
		
		// check for zero scale value
		double deltaScale = (endNumber - startNumber);
		if (startNumber < 0 && endNumber > 0) {
			if (deltaScale <= 0.1) {
				numberTicks = new Double(deltaScale * 100 * heightAdaptation).intValue();
				numberTicks = fineTuneScaleToMeetZero(numberTicks, deltaScale, 0.01, height);
			}
			else if (deltaScale <= 0.5) {
				numberTicks = new Double(deltaScale * 50 * heightAdaptation).intValue();
				numberTicks = fineTuneScaleToMeetZero(numberTicks, deltaScale, 0.05, height);
			}
			else if (deltaScale <= 1) {
				numberTicks = new Double(deltaScale * 20 * heightAdaptation).intValue();
				numberTicks = fineTuneScaleToMeetZero(numberTicks, deltaScale, 0.1, height);
			}
			else if (deltaScale <= 2) {
				numberTicks = new Double(deltaScale * 10 * heightAdaptation).intValue();
				numberTicks = fineTuneScaleToMeetZero(numberTicks, deltaScale, 0.25, height);
			}
			else if (deltaScale <= 5) {
				numberTicks = new Double(deltaScale * 5 * heightAdaptation).intValue();
				numberTicks = fineTuneScaleToMeetZero(numberTicks, deltaScale, 0.5, height);
			}
			else if (deltaScale <= 10) {
				numberTicks = new Double(deltaScale * 2 * heightAdaptation).intValue();
				numberTicks = fineTuneScaleToMeetZero(numberTicks, deltaScale, 1, height);
			}
			else if (deltaScale <= 25) {
				numberTicks = new Double(deltaScale * heightAdaptation).intValue();
				numberTicks = fineTuneScaleToMeetZero(numberTicks, deltaScale, 1.5, height);
			}
			else if (deltaScale <= 50) {
				numberTicks = new Double(deltaScale/2.5 * heightAdaptation).intValue();
				numberTicks = fineTuneScaleToMeetZero(numberTicks, deltaScale, 2.5, height);
			}
			else if (deltaScale <= 100) {
				numberTicks = new Double(deltaScale / 5 * heightAdaptation).intValue();
				numberTicks = fineTuneScaleToMeetZero(numberTicks, deltaScale, 5, height);
			}
			else if (deltaScale <= 300) {
				numberTicks = new Double(deltaScale / 10 * heightAdaptation).intValue();
				numberTicks = fineTuneScaleToMeetZero(numberTicks, deltaScale, 10, height);
			}
			else {
				numberTicks = new Double(deltaScale / 20 * heightAdaptation).intValue();
				numberTicks = fineTuneScaleToMeetZero(numberTicks, deltaScale, 20, height);
			}
		}
		log.log(Level.FINE, record.getName() + " numberTicks = " + numberTicks); //$NON-NLS-1$
		
		// prepare grid vector
		Vector<Integer> horizontalGrid = new Vector<Integer>();
		RecordSet recordSet = record.getParent();
		boolean isBuildGridVector = recordSet.getHorizontalGridType() != RecordSet.HORIZONTAL_GRID_NONE && recordSet.getHorizontalGridRecordName().equals(record.getName());
		
		int dist = 10;
		double deltaValue = deltaScale / numberTicks;
		double deltaTick = 1.0 * height / numberTicks;
		miniticks++;

		if (record.getNumberScaleTicks() != numberTicks) {
			record.setNumberScaleTicks(numberTicks);
			int cleanwidth = 35; //ticklength + gap + dist;
			if (isPositionLeft) 
				gc.fillRectangle(x0 - cleanwidth, y0-height, cleanwidth, height);
			else
				gc.fillRectangle(x0+1, y0-height, cleanwidth, height);
		}

		if (!isPositionLeft) {
			ticklength = ticklength * -1; // mirror drawing direction 
			gap = gap * -1;
			dist = dist * -1;
		}
		for (int i = 0; i <= numberTicks; i++) {
			//draw the main scale, length = 5 and gap to scale = 2
			int yPosition = new Double(y0 - i * deltaTick).intValue();
			gc.drawLine(x0, yPosition, x0 - ticklength, yPosition);
			if (i != 0 && isBuildGridVector) horizontalGrid.add(yPosition);
			//draw the sub scale according number of miniTicks
			double deltaPosMini = deltaTick / miniticks;
			for (int j = 1; j < miniticks && i < numberTicks; j++) {
				int yPosMini = yPosition - (int)(j * deltaPosMini);
				log.log(Level.FINEST, "yPosition=" + yPosition + ", xPosMini=" + yPosMini); //$NON-NLS-1$ //$NON-NLS-2$
				gc.drawLine(x0, yPosMini, x0 - ticklength / 2, yPosMini);
			}
			//draw numbers to the scale	
			drawText(df.format(startNumber + i * deltaValue), x0 - ticklength - gap - dist, yPosition, gc, SWT.HORIZONTAL);
		}
		if (isBuildGridVector) {
			recordSet.setHorizontalGrid(horizontalGrid);
		}
	}

	/**
	 * adjust the number of tick marks to enable the mark at 0.0 
	 * @param numberTicks
	 * @param deltaScale
	 * @return
	 */
	private static int fineTuneScaleToMeetZero(int numberTicks, double deltaScale, double modValue, int height) {
		/* 700 / 32 = 20 pixel minumum between main tick marks */
		int modTicks = new Double(deltaScale / modValue).intValue();
		
		if (height / modTicks > 30)
			numberTicks = modTicks * 2;
		else if (height / modTicks < 15)
			numberTicks = modTicks / 2;
		else 
			numberTicks = modTicks;

		return numberTicks>2 ? numberTicks : 2;
	}

	/**
	 * Draws text horizontal or vertically (rotates plus or minus 90 degrees). Uses the current
	 * @param string the text to draw
	 * @param x the x coordinate of the top left corner of the drawing rectangle
	 * @param y the y coordinate of the top left corner of the drawing rectangle
	 * @param gc the GC on which to draw the text
	 * @param style the style (SWT.UP or SWT.DOWN)
	 */
	public static void drawText(String string, int x, int y, GC gc, int style) {
		// Get the current display
		Display display = Display.getCurrent();
		if (display == null) SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);

		// Determine string's dimensions
		//if (GraphicsUtils.application != null) // gc already primed with font GraphicsComposite.drawCurves(); 
		//	gc.setFont(SWTResourceManager.getFont(GraphicsUtils.application, GraphicsUtils.application.getWidgetFontSize(), SWT.NORMAL));
		Point pt = gc.textExtent(string);

		// Create an image the same size as the string
		Image stringImage = SWTResourceManager.getImage(pt.x, pt.y);

		// Create a GC so we can draw the image
		GC stringGc = SWTResourceManager.getGC(stringImage);

		// Set attributes from the original GC to the new GC
		stringGc.setForeground(gc.getForeground());
		stringGc.setBackground(gc.getBackground());
		if (GraphicsUtils.application != null)
			stringGc.setFont(SWTResourceManager.getFont(GraphicsUtils.application, GraphicsUtils.application.getWidgetFontSize(), SWT.NORMAL));
		else
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
	 * Draws text horizontal or vertically (rotates plus or minus 90 degrees). Uses the current
	 * @param string the text to draw
	 * @param x the x coordinate of the top left corner of the drawing rectangle
	 * @param y the y coordinate of the top left corner of the drawing rectangle
	 * @param gc the GC on which to draw the text
	 * @param style the style (SWT.UP or SWT.DOWN)
	 */
	public static void drawTimeLineText(String string, int x, int y, GC gc, int style) {
		// Get the current display
		Display display = Display.getCurrent();
		if (display == null) SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);

		// Determine string's dimensions
		//if (GraphicsUtils.application != null) // gc already primed with font GraphicsComposite.drawCurves(); 
		//	gc.setFont(SWTResourceManager.getFont(GraphicsUtils.application, GraphicsUtils.application.getWidgetFontSize(), SWT.NORMAL));
		Point pt = gc.textExtent(string);
		
		// Create an image the same size as the string
		Image stringImage = SWTResourceManager.getImage(pt.x, pt.y);

		// Create a GC so we can draw the image
		GC stringGc = SWTResourceManager.getGC(stringImage);

		// Set attributes from the original GC to the new GC
		stringGc.setForeground(gc.getForeground());
		stringGc.setBackground(gc.getBackground());
		if (GraphicsUtils.application != null)
			stringGc.setFont(SWTResourceManager.getFont(GraphicsUtils.application, GraphicsUtils.application.getWidgetFontSize(), SWT.NORMAL));
		else
			stringGc.setFont(gc.getFont());

		// clear the image with background color
		stringGc.fillRectangle(0, 0, pt.x, pt.y);
		// draw the text into the image
		stringGc.drawText(string, 0, 0);
		
		if (string.contains(", ")) { // string [min, hrs]
			if (GraphicsUtils.application != null)
				stringGc.setFont(SWTResourceManager.getFont(GraphicsUtils.application, GraphicsUtils.application.getWidgetFontSize(), SWT.BOLD));
			else
				stringGc.setFont(gc.getFont());
			
			stringGc.drawText(string.split(", |]")[1], gc.textExtent(string.split(", ")[0]+",").x, 0);
		}

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
	 * @param image the image to draw
	 * @param x the x coordinate of the top left corner of the drawing rectangle
	 * @param y the y coordinate of the top left corner of the drawing rectangle
	 * @param gc the GC on which to draw the image
	 * @param style the style (SWT.UP or SWT.DOWN)
	 */
	public static void drawVerticalImage(Image image, int x, int y, GC gc, int style, String imgKey) {
		// Get the current display
		Display display = Display.getCurrent();
		if (display == null) SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);

		// Draw the vertical image onto the original GC
		gc.drawImage(SWTResourceManager.getRotatedImage(image, style, imgKey), x, y);
	}
}
