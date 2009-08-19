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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import osde.OSDE;
import osde.data.Record;
import osde.device.IDevice;
import osde.ui.OpenSerialDataExplorer;

/**
 * This class contains utilities to draw curves and vertical scales
 * @author Winfried Brügmann
 */
public class CurveUtils {
	private static Logger			log								= Logger.getLogger(CurveUtils.class.getName());

	/**
	 * draws the data graph scale using gives rectangle for display
	 * all data point are multiplied with factor 1000 to avoid rounding errors for values below 1.0 (0.5 -> 0)
	 * @param record
	 * @param gc
	 * @param x0
	 * @param y0
	 * @param width
	 * @param height
	 * @param scaleWidthSpace
	 */
	public static void drawScale(Record record, GC gc, int x0, int y0, int width, int height, int scaleWidthSpace) {
		final IDevice device = record.getDevice(); // defines the link to a device where values may corrected
		final boolean isCompareSet = record.getParent().isCompareSet();

		log.log(Level.FINER, "x0=" + x0 + " y0=" + y0 + " width=" + width + " height=" + height + " horizontalSpace=" + scaleWidthSpace); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		if (record.isEmpty() && !record.isDisplayable() && !record.isSyncPlaceholder()) return; // nothing to display
		String recordName = isCompareSet ? record.getKeyName() : record.getName();
		log.log(Level.FINE, "drawing record =" + recordName + " isCompareSet = " + isCompareSet); //$NON-NLS-1$ //$NON-NLS-2$

		//Draw the curve
		//(yMaxValue - yMinValue) defines the area to be used for the curve
		double yMaxValue = record.getMaxValue() / 1000.0;
		double yMinValue = record.getMinValue() / 1000.0;
		log.log(Level.FINE, "unmodified yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$

		// yMinValueDisplay and yMaxValueDisplay used for scales and adapted values device and measure unit dependent
		double yMinValueDisplay = yMinValue, yMaxValueDisplay = yMaxValue;
		boolean isRaw = record.getParent().isRaw();

		if (yMaxValue == yMinValue && !isRaw) {
			yMinValueDisplay = yMinValue = new Double(yMinValue - 1).intValue();
			yMaxValueDisplay = yMaxValue = new Double(yMaxValue + 1).intValue();
		}
		if (record.isStartEndDefined()) {
			yMinValueDisplay = record.getMinScaleValue();
			yMaxValueDisplay = record.getMaxScaleValue();
			if (isRaw) {
				yMinValue = device.reverseTranslateValue(record, yMinValueDisplay);
				yMaxValue = device.reverseTranslateValue(record, yMaxValueDisplay);
			}
			else {
				yMinValue = yMinValueDisplay;
				yMaxValue = yMaxValueDisplay;
			}

			log.log(Level.FINE, "defined yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else {
			//exclude imported data where values don't need correction
			if (device != null && isRaw) { // adapt to device specific range
				yMinValueDisplay = device.translateValue(record, yMinValue);
				yMaxValueDisplay = device.translateValue(record, yMaxValue);
			}

			if (device != null && (record.isRoundOut() || yMaxValue == yMinValue)) { // equal value disturbs the scaling alogorithm
				double deltaValueDisplay = yMaxValueDisplay - yMinValueDisplay;
				yMaxValueDisplay = yMaxValueDisplay > 0 ? MathUtils.roundUp(yMaxValueDisplay, deltaValueDisplay) : MathUtils.roundDown(yMaxValueDisplay, deltaValueDisplay); // max
				yMinValueDisplay = yMinValueDisplay > 0 ? MathUtils.roundDown(yMinValueDisplay, deltaValueDisplay) : MathUtils.roundUp(yMinValueDisplay, deltaValueDisplay); // min
				if (isRaw) {
					yMinValue = device.reverseTranslateValue(record, yMinValueDisplay);
					yMaxValue = device.reverseTranslateValue(record, yMaxValueDisplay);
				}
				else {
					yMinValue = yMinValueDisplay;
					yMaxValue = yMaxValueDisplay;
				}

				log.log(Level.FINE, String.format("rounded yMinValue = %5.3f - yMaxValue = %5.3f", yMinValue, yMaxValue)); //$NON-NLS-1$
			}
			if (record.isStartpointZero()) {
				yMinValueDisplay = 0;
				if (isRaw) {
					yMinValue = yMinValueDisplay - record.getOffset();
				}
				else {
					yMinValue = yMinValueDisplay;
				}
				log.log(Level.FINE, "scale starts at 0; yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		record.setMinScaleValue(yMinValueDisplay);
		record.setMaxScaleValue(yMaxValueDisplay);
		log.log(Level.FINE, "scale  -> yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay); //$NON-NLS-1$ //$NON-NLS-2$
		String graphText = recordName + "   " + record.getSymbol() + "   [" + record.getUnit() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		// adapt number space calculation to real displayed max number
		//Point pt = gc.textExtent(df.format(yMaxValueDisplay));
		//log.log(Level.FINE, df.format(yMaxValueDisplay) + " gc.textExtent = " + pt.toString());
		Point pt = gc.textExtent("000,00"); //$NON-NLS-1$
		int ticklength = 5;
		int gap = 10;
		int miniticks = 4;

		// prepare axis position
		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_SOLID);
		boolean isPositionLeft = record.isPositionLeft();
		int positionNumber = isCompareSet ? 0 : record.getParent().getAxisPosition(recordName, isPositionLeft);
		log.log(Level.FINE, recordName + " positionNumber = " + positionNumber); //$NON-NLS-1$
		DecimalFormat df = record.getDecimalFormat();
		gc.setForeground(record.getColor()); // draw the main scale line in same color as the curve
		if (isPositionLeft) {
			int xPos = x0 - 1 - positionNumber * scaleWidthSpace;
			gc.drawLine(xPos, y0+1, xPos, y0-height-1); //xPos = x0
			log.log(Level.FINE, "y-Achse = " + xPos + ", " + y0 + ", " + xPos + ", " + (y0 - height)); //yMax //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			gc.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
			GraphicsUtils.drawVerticalTickMarks(record, gc, xPos, y0, height, yMinValueDisplay, yMaxValueDisplay, ticklength, miniticks, gap, isPositionLeft, df);
			log.log(Level.FINEST, "drawText x = " + (xPos - pt.y - 15)); //xPosition Text Spannung [] //$NON-NLS-1$
			if (!isCompareSet) GraphicsUtils.drawText(graphText, (xPos - scaleWidthSpace + 3), y0 / 2 + (y0 - height), gc, SWT.UP);
		}
		else {
			int xPos = x0 + 1 + width + positionNumber * scaleWidthSpace;
			gc.drawLine(xPos, y0+1, xPos, y0-height-1); //yMax
			gc.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
			log.log(Level.FINEST, "y-Achse = " + xPos + ", " + y0 + ", " + xPos + ", " + (y0 - height)); //yMax //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			GraphicsUtils.drawVerticalTickMarks(record, gc, xPos, y0, height, yMinValueDisplay, yMaxValueDisplay, ticklength, miniticks, gap, isPositionLeft, df);
			if (!isCompareSet) GraphicsUtils.drawText(graphText, (xPos + scaleWidthSpace - pt.y - 5), y0 / 2 + (y0 - height), gc, SWT.UP);
		}

		// set the values corresponding to the display area of this curve
		record.setMinDisplayValue(yMinValue);
		record.setMaxDisplayValue(yMaxValue);
		log.log(Level.FINE, record.getName() + " data limit  -> yMinValue = " + yMinValue + "; yMaxValue = " + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * method draw the curve using the given graphics context (GC)
	 * @param record
	 * @param gc
	 * @param x0
	 * @param y0
	 * @param width
	 * @param height
	 * @param isCompareSet
	 * @param isZoomMode
	 */
	public static void drawCurve(Record record, GC gc, int x0, int y0, int width, int height, boolean isCompareSet, boolean isZoomMode) {
		log.log(Level.FINE, record.getName()+ String.format(" x0 = %d, y0 = %d, width = %d, height = %d", x0, y0, width, height)); //$NON-NLS-1$
		log.log(Level.FINER, "curve area bounds = " + record.getParent().getDrawAreaBounds().toString()); //$NON-NLS-1$

		// set line properties according adjustment
		gc.setForeground(record.getColor());
		gc.setLineWidth(record.getLineWidth());
		gc.setLineStyle(record.getLineStyle());

		// get the data points size
		int recordSize = isCompareSet ? record.realSize() : record.size();

		// calculate time line adaption if record set is compare set, compare set max have different times for each record, (intRecordSize - 1) is number of time deltas for calculation 
		double timeStep = record.getTimeStep_ms();
		double adaptXMaxValue = isCompareSet ? (1.0 * (recordSize - 1) * (record.getParent().getRecordDataSize(false) - 1) / (recordSize - 1) * timeStep) : (1.0 * (recordSize - 1) * timeStep);
		log.log(Level.FINE, "recordSize = " + recordSize + " adaptXMaxValue = " + adaptXMaxValue); //$NON-NLS-1$ //$NON-NLS-2$

		// calculate scale factor to fit time into draw bounds
		double factorX = (1.0 * width) / adaptXMaxValue;
		// calculate xScale for curves with much to many data points -it makes no sense to draw all the small lines on the same part of the screen
		int xScale = 1;
		if (recordSize > (width * 2)) {
			if (record.getParent().getXScale() == 0) {
				xScale = new Double(recordSize / (width * 2)).intValue();
				while (!(recordSize % xScale <= 5) && xScale > 1) {
					--xScale;
				}
				record.getParent().setXScale(xScale);
			}
			xScale = record.getParent().getXScale();
			factorX = factorX * xScale;
		}
		record.setDisplayScaleFactorTime(factorX);
		record.setDisplayScaleFactorValue(height);

		StringBuffer sb = new StringBuffer(); // logging purpose
		Point newPoint, oldPoint = new Point(0, 0);

		// compare sets might have different size curves, in zoom mode draw only until the curve ends
		if (isCompareSet && isZoomMode) recordSize = recordSize - record.getParent().getRecordZoomOffset();

		try {
			// calculate start point of the curve, which is the first oldPoint
			oldPoint = record.getDisplayPoint(0, 0, x0, y0);
			if (log.isLoggable(Level.FINEST)) sb.append(OSDE.LINE_SEPARATOR).append(oldPoint.toString());
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage() + " zoomed compare set ?", e); //$NON-NLS-1$
		}

		try {
			// draw scaled points to draw area - measurements can only be drawn starting with the first measurement point
			for (int i = 0, j = 0; j < recordSize && recordSize > 1; ++i, j = j+xScale) {
				// get the point to be drawn
				newPoint = record.getDisplayPoint(i, j, x0, y0);
				if (log.isLoggable(Level.FINEST)) sb.append(OSDE.LINE_SEPARATOR).append(newPoint.toString());

				gc.drawLine(oldPoint.x, oldPoint.y, newPoint.x, newPoint.y);

				// remember the last draw point for next drawLine operation
				oldPoint = newPoint;
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage() + " zoomed compare set ?", e); //$NON-NLS-1$
		}
		log.log(Level.FINEST, sb.toString());
	}
}
