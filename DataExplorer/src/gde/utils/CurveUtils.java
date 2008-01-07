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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import osde.data.Record;
import osde.device.IDevice;
import osde.ui.OpenSerialDataExplorer;

/**
 * This class contains utilities to draw curves and vertical scales
 * @author Winfried Brügmann
 */
public class CurveUtils {
	private static Logger				log			= Logger.getLogger(CurveUtils.class.getName());
	private static final String	lineSep	= System.getProperty("line.separator");

	/**
	 * draws the data as graph using gives rectangle for display
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

		if (log.isLoggable(Level.FINEST)) log.finest("x0=" + x0 + " y0=" + y0 + " width=" + width + " height=" + height + " horizontalSpace=" + scaleWidthSpace);
		if (record.isEmpty() && !record.isDisplayable()) return; // nothing to display
		boolean isCompareSet = record.getParent().isCompareSet();
		String recordName = isCompareSet ? record.getKeyName() : record.getName();
		log.fine("drawing record =" + recordName + " isCompareSet = " + isCompareSet);

		//Draw the curve
		//(yMaxValue - yMinValue) defines the area to be used for the curve
		double yMaxValue = record.getMaxValue() / 1000.0;
		double yMinValue = record.getMinValue() / 1000.0;
		if (log.isLoggable(Level.FINE)) log.fine("unmodified yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue);

		// yMinValueDisplay and yMaxValueDisplay used for scales and adapted values device and measure unit dependent
		double yMinValueDisplay = yMinValue, yMaxValueDisplay = yMaxValue;
		boolean isRaw = record.getParent().isRaw();

		if (yMaxValue == yMinValue && !isRaw) {
			yMinValueDisplay = yMinValue = new Double(yMinValue - 1).intValue();
			yMaxValueDisplay = yMaxValue = new Double(yMaxValue + 1).intValue();
		}
		if (record.isStartEndDefined()) {
			yMinValueDisplay = record.getDefinedMinValue();
			yMaxValueDisplay = record.getDefinedMaxValue();
			if (isRaw) {
				yMinValue = device.reverseTranslateValue(recordName, yMinValueDisplay);
				yMaxValue = device.reverseTranslateValue(recordName, yMaxValueDisplay);
			}
			else {
				yMinValue = yMinValueDisplay;
				yMaxValue = yMaxValueDisplay;
			}

			if (log.isLoggable(Level.FINE)) log.fine("defined yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue);
		}
		else {
			// TODO exclude imported data where values don't need correction
			if (device != null && isRaw) { // adapt to device specific range
				yMinValueDisplay = device.translateValue(recordName, yMinValue);
				yMaxValueDisplay = device.translateValue(recordName, yMaxValue);
			}

			if (device != null && (record.isRoundOut() || yMaxValue == yMinValue)) { // equal value disturbs the scaling alogorithm
				double[] roundValues = round(yMinValueDisplay, yMaxValueDisplay);
				yMinValueDisplay = roundValues[0]; 	// min
				yMaxValueDisplay = roundValues[1];	// max
				if (isRaw) {
					yMinValue = device.reverseTranslateValue(recordName, yMinValueDisplay);
					yMaxValue = device.reverseTranslateValue(recordName, yMaxValueDisplay);
				}
				else {
					yMinValue = yMinValueDisplay;
					yMaxValue = yMaxValueDisplay;
				}

				if (log.isLoggable(Level.FINE)) log.fine(String.format("rounded yMinValue = %5.3f - yMaxValue = %5.3f", yMinValue, yMaxValue));
			}
			if (record.isStartpointZero()) {
				yMinValue = yMinValueDisplay = 0;
				if (log.isLoggable(Level.FINE)) log.fine("start 0 yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue);
			}
		}
		record.setMinScaleValue(yMinValueDisplay);
		record.setMaxScaleValue(yMaxValueDisplay);
		if (log.isLoggable(Level.FINE)) log.fine("yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay);
		String graphText = recordName.split("_")[0] + "   " + record.getSymbol() + "   [" + device.getDataUnit(recordName) + "]";

		// adapt number space calculation to real displayed max number
		//Point pt = gc.textExtent(df.format(yMaxValueDisplay));
		//log.fine(df.format(yMaxValueDisplay) + " gc.textExtent = " + pt.toString());
		Point pt = gc.textExtent("000,00");
		int ticklength = 5;
		int gap = 10;
		int miniticks = 3;

		// prepare axis position
		boolean isPositionLeft = record.isPositionLeft();
		int positionNumber = isCompareSet ? 0 : record.getParent().getAxisPosition(recordName, isPositionLeft);
		log.fine(recordName + " positionNumber = " + positionNumber);
		DecimalFormat df = record.getDecimalFormat();
		gc.setForeground(record.getColor()); // draw the main scale line in same color as the curve
		if (isPositionLeft) {
			int xPos = x0 - 1 - positionNumber * scaleWidthSpace;
			gc.drawLine(xPos, y0, xPos, y0 - height); //xPos = x0
			if (log.isLoggable(Level.FINE)) log.fine("y-Achse = " + xPos + ", " + y0 + ", " + xPos + ", " + (y0 - height)); //yMax
			gc.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
			GraphicsUtils.drawVerticalTickMarks(gc, xPos, y0, height, yMinValueDisplay, yMaxValueDisplay, ticklength, miniticks, gap, isPositionLeft, df);
			if (log.isLoggable(Level.FINEST)) log.finest("drawText x = " + (int) (xPos - pt.y - 15)); //xPosition Text Spannung []
			GraphicsUtils.drawText(graphText, (int) (xPos - pt.x - pt.y - 15), y0 / 2 + (y0 - height), gc, SWT.UP);
		}
		else {
			int xPos = x0 + width + positionNumber * scaleWidthSpace;
			gc.drawLine(xPos, y0, xPos, y0 - height); //yMax
			gc.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
			if (log.isLoggable(Level.FINEST)) log.finest("y-Achse = " + xPos + ", " + y0 + ", " + xPos + ", " + (y0 - height)); //yMax
			GraphicsUtils.drawVerticalTickMarks(gc, xPos, y0, height, yMinValueDisplay, yMaxValueDisplay, ticklength, miniticks, gap, isPositionLeft, df);
			GraphicsUtils.drawText(graphText, (int) (xPos + pt.x + 15), y0 / 2 + (y0 - height), gc, SWT.UP);
		}
		
		// set the values corresponding to the display area of this curve
		record.setMinDisplayValue(yMinValue);
		record.setMaxDisplayValue(yMaxValue);
	}

	/**
	 * method draw the curve using the given graphics context (GC)
	 * @param gc
	 * @param record
	 * @param x0
	 * @param y0
	 * @param width
	 * @param height
	 * @param isCompareSet
	 * @param yMinValue
	 * @param yMaxValue
	 */
	public static void drawCurve(Record record, GC gc, int x0, int y0, int width, int height, boolean isCompareSet) {
		if (log.isLoggable(Level.FINER)) log.finer(String.format("x0 = %d, y0 = %d, width = %d, height = %d", x0, y0, width, height));
		if (log.isLoggable(Level.FINER)) log.finer("curve area bounds = " + record.getParent().getCurveBounds().toString());
		//gc.setClipping(record.getParent().getCurveBounds());
	
		// set line properties according adjustment
		gc.setForeground(record.getColor());
		gc.setLineWidth(record.getLineWidth());
		gc.setLineStyle(record.getLineStyle());
		
		// get the data points size
		int recordSize = record.size();

		// calculate time line adaption if record set is compare set, compare set max have different times for each record, (intRecordSize - 1) is number of time deltas for calculation 
		int timeStep = record.getTimeStep_ms();
		double adaptXMaxValue = isCompareSet ? (1.0 * (recordSize - 1) * record.getParent().getMaxSize() / (recordSize - 1) * timeStep) : (1.0 * (recordSize - 1) * timeStep);
		
		// calculate scale factor to fit time into draw bounds
		double factorX = (1.0 * width) / adaptXMaxValue;
		// calculate xScale for curves with much to many data points -it makes no sense to draw all the small lines on the same part of the screen
		int xScale = 1;
		if (recordSize > (width * 2)) {
			xScale = recordSize / (width * 2);
			factorX = factorX * xScale;
		}
		record.setDisplayScaleFactorTime(factorX);
		record.setDisplayScaleFactorValue(height);
		
		StringBuffer sb = new StringBuffer(); // logging purpose
		Point newPoint, oldPoint;
		
		// calculate start point of the curve, which is the first oldPoint
		oldPoint = record.getDisplayPoint(0, 0, x0, y0);
		if (log.isLoggable(Level.INFO)) sb.append(lineSep).append(oldPoint.toString());
		
		// draw scaled points to draw area - measurements can only be drawn starting with the first measurement point
		for (int i = 0, j = 0; j < recordSize && recordSize > 1; ++i, j = j + xScale) {
			// get the point to be drawn
			newPoint = record.getDisplayPoint(i, j, x0, y0);
			if (log.isLoggable(Level.INFO)) sb.append(lineSep).append(newPoint.toString());

			gc.drawLine(oldPoint.x, oldPoint.y, newPoint.x, newPoint.y);

			// remember the last draw point for next drawLine operation
			oldPoint = newPoint;
		}
		if (log.isLoggable(Level.INFO)) log.finest(sb.toString());
	}
	
	/**
	 * adapted rounding  
	 * - a small number needs different rounding compared to a big number 0.05 -> 0.1, 529 -> 550
	 * - a small value delta needs different rounding compared to a big delta 10 -> +-1, 200 +-10 
	 * @param double array minValue, maxValue 
	 * @return double array roundMinValue, roundMaxValue 
	 */
	private static double[] round(double minValue, double maxValue) {
		double[] outValues = {0.0, 0.0};
		
		if (minValue != 0) {
			if (minValue < 0) {
				if (minValue > -1)
					outValues[0] = minValue - (0.1 + (minValue - 0.1) % 0.1);
				else if (minValue > -2.5)
					outValues[0] = minValue - (0.25 + (minValue - 0.25) % 0.25);
				else if (minValue > -5)
					outValues[0] = minValue - (0.5 + (minValue - 0.5) % 0.5);
				else if (minValue > -10)
					outValues[0] = (int) (minValue - 1);
				else if (minValue < -50)
					outValues[0] = minValue - (10 + (minValue % 10));
				else
					outValues[0] = minValue - (5 + (minValue % 5));
			}
			else {// minValue > 0 
				if (minValue < 1)
					outValues[0] = minValue - (0.1 + (minValue - 0.1) % 0.1);
				else if (minValue < 2.5)
					outValues[0] = minValue - (0.25 + (minValue - 0.25) % 0.25);
				else if (minValue < 5)
					outValues[0] = minValue - (0.5 + (minValue - 0.5) % 0.5);
				else if (minValue < 10)
					outValues[0] = (int) (minValue - 1);
				else if (minValue < 50)
					outValues[0] = minValue - (minValue % 10);
				else
					outValues[0] = minValue - (minValue % 5);
			}
		}
		
		if (maxValue != 0) {
			if (maxValue < 0) {
				if (maxValue > -1)
					outValues[1] = maxValue + (0.1 - (maxValue - 0.1) % 0.1);
				else if (maxValue > -2.5)
					outValues[1] = maxValue + (0.25 - (maxValue - 0.25) % 0.25);
				else if (maxValue > -5)
					outValues[1] = maxValue + (0.5 - (maxValue - 0.5) % 0.5);
				else if (maxValue > -10)
					outValues[1] = (int) (maxValue + 1);
				else if (maxValue > -50)
					outValues[1] = maxValue + 5 - (maxValue % 5);
				else
					outValues[1] = maxValue + 10 - (maxValue % 10);
			}
			else {
				if (maxValue < 1)
					outValues[1] = maxValue + (0.1 - (maxValue + 0.1) % 0.1);
				else if (maxValue < 2.5)
					outValues[1] = maxValue + (0.25 - (maxValue + 0.25) % 0.25);
				else if (maxValue < 5)
					outValues[1] = maxValue + (0.5 - (maxValue + 0.5) % 0.5);
				else if (maxValue < 10)
					outValues[1] = (int) (maxValue + 1);
				else if (maxValue > 50)
					outValues[1] = maxValue + 10 - (maxValue % 10);
				else
					outValues[1] = maxValue + 5 - (maxValue % 5);
			}
		}
		
		// enable scale value 0.0  -- algorithm must fit scale tick mark calculation
		if(minValue < 0 && maxValue > 0) {
			double deltaScale = outValues[1] - outValues[0];
			if (deltaScale < 2) {
				//numberTicks = (int)(deltaScale+0.5) * 10 / 1;
				outValues[0] = outValues[0] - (0.05 + outValues[0] % 0.1);
				outValues[1] = outValues[1] + (outValues[1] % 0.5);
			}
			else if (deltaScale < 5) {
				//numberTicks = (int)(deltaScale+1) * 5 / 1;
				outValues[0] = outValues[0] - (0.2 + outValues[0] % 0.2);
				outValues[1] = outValues[1] + (outValues[1] % 0.2);
			}
			else if (deltaScale < 10) {
				//numberTicks = (int)deltaScale;
				outValues[0] = outValues[0] - (0.5 + outValues[0] % 0.5);
				outValues[1] = outValues[1] + (outValues[1] % 0.5);
			}
			else if (deltaScale < 50) {
				//numberTicks = (int)deltaScale;
				outValues[0] = outValues[0] - (2.5 + outValues[0] % 2.5);
				outValues[1] = outValues[1] + (outValues[1] % 2.5);
			}
			else if (deltaScale < 100) {
				//numberTicks = (int)(deltaScale / 5);
				outValues[0] = outValues[0] - (5 + (outValues[0] % 5));
				outValues[1] = outValues[1] + (outValues[1] % 5);
			}		
			else if (deltaScale < 300) { 
				//numberTicks = (int)(deltaScale / 20);
				outValues[0] = outValues[0] - (10 + (outValues[0] % 10));
				outValues[1] = outValues[1] + (outValues[1] % 10);
			}	
			else { // > 300
				outValues[0] = outValues[0] - (20 + (outValues[0] % 20));
				outValues[1] = outValues[1] + (outValues[1] % 20);
			}
		}

		if (log.isLoggable(Level.FINE)) log.fine(minValue + " --> " + outValues[0] + " " + maxValue + " --> " + outValues[1]);
		return outValues;
	}
}
