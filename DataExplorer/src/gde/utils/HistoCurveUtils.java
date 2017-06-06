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

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017 Winfried Bruegmann
    					2016,2017 Thomas Eickert
****************************************************************************************/
package gde.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import gde.GDE;
import gde.config.Settings;
import gde.data.TrailRecord;
import gde.data.TrailRecordSet;
import gde.device.IDevice;
import gde.device.resource.DeviceXmlResource;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.HistoTimeLine.Density;

/**
 * utilities to draw curves.
 * support histo curves, suites of curves.
 * @author Thomas Eickert
 */
public class HistoCurveUtils { // todo merging with CurveUtils reduces number of classes
	private final static String		$CLASS_NAME	= HistoTimeLine.class.getName();
	private final static Logger		log					= Logger.getLogger($CLASS_NAME);

	private final static Settings	settings		= Settings.getInstance();

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
	 * @param isDrawScaleInRecordColor
	 * @param isDrawNameInRecordColor
	 * @param isDrawNumbersInRecordColor
	 */
	public static void drawHistoScale(TrailRecord record, GC gc, int x0, int y0, int width, int height, int scaleWidthSpace, boolean isDrawScaleInRecordColor, boolean isDrawNameInRecordColor,
			boolean isDrawNumbersInRecordColor) {
		final IDevice device = record.getDevice(); // defines the link to a device where values may corrected
		int numberTicks = 10, miniticks = 5;

		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, record.getName() + "  x0=" + x0 + " y0=" + y0 + " width=" + width + " height=" + height + " horizontalSpace=" + scaleWidthSpace); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		if (record.isEmpty() && !record.isDisplayable() && !record.isScaleVisible()) return; // nothing to display

		//Draw the curve
		//(yMaxValue - yMinValue) defines the area to be used for the curve
		double yMaxValue = record.getSyncMaxValue() / 1000.0;
		double yMinValue = record.getSyncMinValue() / 1000.0;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "unmodified yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$

		// yMinValueDisplay and yMaxValueDisplay used for scales and adapted values device and measure unit dependent
		double yMinValueDisplay = yMinValue, yMaxValueDisplay = yMaxValue;

		if (record.isStartEndDefined()) {
			yMinValueDisplay = record.getMinScaleValue();
			yMaxValueDisplay = record.getMaxScaleValue();
			yMinValue = device.reverseTranslateValue(record, yMinValueDisplay);
			yMaxValue = device.reverseTranslateValue(record, yMaxValueDisplay);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "defined yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "defined -> yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else {
			if (device != null) { // adapt to device specific range
				if (!record.isTrailSuite() && record.parallelStream().noneMatch(Objects::nonNull))
					; // in case of an empty record leave the values unchanged
				else {
					yMinValueDisplay = device.translateValue(record, yMinValue);
					yMaxValueDisplay = device.translateValue(record, yMaxValue);
				}
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "undefined -> yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (device != null && (Math.abs(yMaxValue - yMinValue) < .0001)) { // equal value disturbs the scaling algorithm
				double deltaValueDisplay = yMaxValueDisplay - yMinValueDisplay;
				yMaxValueDisplay = MathUtils.roundUp(yMaxValueDisplay, deltaValueDisplay); // max
				yMinValueDisplay = MathUtils.roundDown(yMinValueDisplay, deltaValueDisplay); // min
				Object[] roundResult = MathUtils.adaptRounding(yMinValueDisplay, yMaxValueDisplay, false, height / 25 >= 3 ? height / 25 : 2);
				yMinValueDisplay = (Double) roundResult[0];
				yMaxValueDisplay = (Double) roundResult[1];
				numberTicks = (Integer) roundResult[2];
				miniticks = (Integer) roundResult[3];
				yMinValue = device.reverseTranslateValue(record, yMinValueDisplay);
				yMaxValue = device.reverseTranslateValue(record, yMaxValueDisplay);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("rounded yMinValue = %5.3f - yMaxValue = %5.3f", yMinValue, yMaxValue)); //$NON-NLS-1$
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "rounded -> yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (record.isStartpointZero()) {
				// check if the main part of the curve is on positive side
				if (record.getAvgValue() > 0) { // main part of curve is on positive side
					yMinValueDisplay = 0;
					yMinValue = yMinValueDisplay - record.getOffset();
				}
				else {// main part of curve is on negative side
					yMaxValueDisplay = 0;
					yMaxValue = yMaxValueDisplay - record.getOffset();
				}
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "scale starts at 0; yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "scale starts at 0 -> yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		record.setMinScaleValue(yMinValueDisplay);
		record.setMaxScaleValue(yMaxValueDisplay);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "scale  -> yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay); //$NON-NLS-1$ //$NON-NLS-2$
		String graphText = DeviceXmlResource.getInstance().getReplacement(record.isScaleSyncMaster() ? record.getSyncMasterName() : record.getName());
		if (record.getSymbol() != null && record.getSymbol().length() > 0) graphText = graphText + "   " + record.getSymbol();
		if (record.getUnit() != null && record.getUnit().length() > 0) graphText = graphText + "   [" + record.getUnit() + "]"; //$NON-NLS-1$ //$NON-NLS-2$

		// adapt number space calculation to real displayed max number
		//Point pt = gc.textExtent(df.format(yMaxValueDisplay));
		//if (log.isLoggable(Level.FINE)) log.log(Level.FINE, df.format(yMaxValueDisplay) + " gc.textExtent = " + pt.toString());
		Point pt = gc.textExtent("000,00"); //$NON-NLS-1$
		int ticklength = 5;
		int gap = 10;

		// prepare axis position
		gc.setLineWidth(2);
		gc.setLineStyle(SWT.LINE_SOLID);
		boolean isPositionLeft = record.isPositionLeft();
		int positionNumber = record.getParent().getAxisPosition(record.getName(), isPositionLeft);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " positionNumber = " + positionNumber); //$NON-NLS-1$
		if (isDrawScaleInRecordColor)
			gc.setForeground(record.getColor()); // draw the main scale line in same color as the curve
		else
			gc.setForeground(DataExplorer.COLOR_BLACK);
		if (isPositionLeft) {
			int xPos = x0 - 1 - positionNumber * scaleWidthSpace;
			gc.drawLine(xPos, y0 + 1, xPos, y0 - height - 1); //xPos = x0
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "y-Achse = " + xPos + ", " + y0 + ", " + xPos + ", " + (y0 - height)); //yMax //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			GraphicsUtils.drawVerticalTickMarks(record, gc, xPos, y0, height, yMinValueDisplay, yMaxValueDisplay, ticklength, miniticks, gap, isPositionLeft, numberTicks, isDrawNumbersInRecordColor);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "drawText x = " + (xPos - pt.y - 15)); //xPosition Text Spannung [] //$NON-NLS-1$
			if (isDrawNameInRecordColor)
				gc.setForeground(record.getColor());
			else
				gc.setForeground(DataExplorer.COLOR_BLACK);
			GraphicsUtils.drawTextCentered(graphText, (xPos - scaleWidthSpace + 3), y0 / 2 + (y0 - height), gc, SWT.UP);
		}
		else {
			int xPos = x0 + 1 + width + positionNumber * scaleWidthSpace;
			gc.drawLine(xPos, y0 + 1, xPos, y0 - height - 1); //yMax
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "y-Achse = " + xPos + ", " + y0 + ", " + xPos + ", " + (y0 - height)); //yMax //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			GraphicsUtils.drawVerticalTickMarks(record, gc, xPos, y0, height, yMinValueDisplay, yMaxValueDisplay, ticklength, miniticks, gap, isPositionLeft, numberTicks, isDrawNumbersInRecordColor);
			if (isDrawNameInRecordColor)
				gc.setForeground(record.getColor());
			else
				gc.setForeground(DataExplorer.COLOR_BLACK);
			GraphicsUtils.drawTextCentered(graphText, (xPos + scaleWidthSpace - pt.y - 5), y0 / 2 + (y0 - height), gc, SWT.UP);
		}

		// set the values corresponding to the display area of this curve
		record.setMinDisplayValue(yMinValue);
		record.setMaxDisplayValue(yMaxValue);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " data limit  -> yMinValue = " + yMinValue + "; yMaxValue = " + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * draw single curve.
	 * support logarithmic distances and elaborate x-axis point placement based on box width and number of points.
	 * @param record
	 * @param gc
	 * @param x0
	 * @param y0
	 * @param width
	 * @param height
	 * @param timeLine
	 */
	public static void drawHistoCurve(TrailRecord record, GC gc, int x0, int y0, int width, int height, HistoTimeLine timeLine) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + String.format(" x0 = %d, y0 = %d, width = %d, height = %d", x0, y0, width, height)); //$NON-NLS-1$
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "curve area bounds = " + record.getParent().getDrawAreaBounds().toString()); //$NON-NLS-1$

		// set line properties according adjustment
		gc.setForeground(record.getColor());
		gc.setLineWidth(record.getLineWidth());
		gc.setLineStyle(record.getLineStyle());

		// get the number of data points size to be drawn
		int displayableSize = record.size();

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "average record time step msec = " + record.getAverageTimeStep_ms()); //$NON-NLS-1$
		double displayableTime_ms = record.getDrawTimeWidth_ms();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "displayableSize = " + displayableSize + " displayableTime_ms = " + displayableTime_ms); //$NON-NLS-1$ //$NON-NLS-2$

		record.setDisplayScaleFactorTime(1);// x-axis scaling not supported
		record.setDisplayScaleFactorValue(height);

		StringBuffer sb = new StringBuffer(); // logging purpose

		Point[] points = null;
		// draw scaled points to draw area - measurements can only be drawn starting with the first measurement point
		if (record.getDevice().isGPSCoordinates(record))
			points = record.getGpsDisplayPoints(timeLine, x0, y0);
		else
			points = record.getDisplayPoints(timeLine, x0, y0);

		Point newPoint, oldPoint = null;
		for (int j = 0; j < points.length && j <= displayableSize && displayableSize >= 1; j++) {
			if ((newPoint = points[j]) != null) { // in case of a suite the master triggers the display of all trails
				drawHistoMarker(gc, newPoint, record.getColor(), timeLine.getDensity());
				if (oldPoint != null) {
					if (log.isLoggable(Level.FINEST)) sb.append(GDE.LINE_SEPARATOR).append(newPoint.toString());
					gc.drawLine(oldPoint.x, oldPoint.y, newPoint.x, newPoint.y);
				}
				oldPoint = newPoint; // remember the last draw point for next drawLine operation
			}
		}
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, sb.toString());
	}

	/**
	 * draw multiple curves (e.g. trails for min, max, avg).
	 * support logarithmic distances and elaborate x-axis point placement based on box width and number of points.
	 * @param record holds display properties and the reference to the suite trail records but no data
	 * @param gc
	 * @param x0
	 * @param y0
	 * @param width
	 * @param height
	 * @param timeLine
	 */
	public static void drawHistoSuite(TrailRecord record, GC gc, int x0, int y0, int width, int height, HistoTimeLine timeLine) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + String.format(" x0 = %d, y0 = %d, width = %d, height = %d", x0, y0, width, height)); //$NON-NLS-1$
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "curve area bounds = " + record.getParent().getDrawAreaBounds().toString()); //$NON-NLS-1$
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("MinScaleValue=%f   MaxScaleValue=%f   MinDisplayValue=%f   MaxDisplayValue=%f", record.getMinScaleValue(), //$NON-NLS-1$
				record.getMaxScaleValue(), record.getMinDisplayValue(), record.getMaxDisplayValue()));

		// set line properties according adjustment
		gc.setForeground(record.getColor());
		gc.setLineWidth(record.getLineWidth());
		gc.setLineStyle(record.getLineStyle());

		int xScaleFactor = 1; // x-axis scaling not supported
		record.setDisplayScaleFactorTime(xScaleFactor);
		record.setDisplayScaleFactorValue(height);

		List<Point[]> suitePoints = new ArrayList<>(); // display point cache: one row for each record of the suite
		if (record.getDevice().isGPSCoordinates(record)) {
			for (int i = 0; i < record.getSuiteSize(); i++) {
				suitePoints.add(record.getGpsDisplayPoints(timeLine, x0, y0, i));
			}
		}
		else {
			for (int i = 0; i < record.getSuiteSize(); i++) {
				suitePoints.add(record.getDisplayPoints(timeLine, x0, y0, i));
			}
		}

		StringBuffer sb = new StringBuffer(); // logging purpose
		double averageDuration = ((TrailRecordSet) record.getParent()).getAverageDuration_mm();
		List<Integer> durations_mm = ((TrailRecordSet) record.getParent()).getDurations_mm();
		Point[] newSuitePoints = new Point[record.getSuiteSize()]; // all points for the current x-axis position
		Point[] oldSuitePoints = new Point[record.getSuiteSize()]; // all points for the previous x-axis position
		for (int j = 0; j < suitePoints.get(0).length; j++) {
			if ((suitePoints.get(0)[j]) != null) { // in case of a suite the master triggers the display of all trails
				for (int i = 0; i < record.getSuiteSize(); i++) {
					oldSuitePoints[i] = newSuitePoints[i];
					newSuitePoints[i] = suitePoints.get(i)[j];
				}
				if (record.isBoxPlotSuite()) {
					if (log.isLoggable(Level.FINEST)) sb.append(GDE.LINE_SEPARATOR).append(newSuitePoints[0].toString());
					// helper variables
					final int posX = newSuitePoints[0].x;
					final int q0PosY = newSuitePoints[0].y, q1PosY = newSuitePoints[1].y, q2PosY = newSuitePoints[2].y, q3PosY = newSuitePoints[3].y, q4PosY = newSuitePoints[4].y,
							qLowerWhiskerY = newSuitePoints[5].y, qUpperWhiskerY = newSuitePoints[6].y;
					final int interQuartileRange = q1PosY - q3PosY;
					int boxWidth = timeLine.getDensity().getScaledBoxWidth();
					int halfBoxWidth = (int) (boxWidth
							* (1. + (Math.sqrt(durations_mm.get(j) / averageDuration) - 1) * timeLine.getDensity().boxWidthAmplitude * HistoCurveUtils.settings.getBoxplotSizeAdaptationOrdinal() / 3.0) / 2.); // divison by 3 is the best fit divisor; 2 results in bigger modulation rates
					halfBoxWidth = halfBoxWidth < 1 ? 1 : halfBoxWidth;
					// draw main box
					gc.drawRectangle(posX - halfBoxWidth, q3PosY, halfBoxWidth * 2, interQuartileRange);
					gc.drawLine(posX - halfBoxWidth, q2PosY, posX + halfBoxWidth, q2PosY);
					// draw min and lower whisker
					if (q0PosY > qLowerWhiskerY) {
						drawHistoMarker(gc, newSuitePoints[0], record.getColor(), timeLine.getDensity());
					}
					gc.drawLine(posX, qLowerWhiskerY, posX, q1PosY);
					gc.drawLine(posX - halfBoxWidth / 2, qLowerWhiskerY, posX + halfBoxWidth / 2, qLowerWhiskerY);
					// draw max and upper whisker
					if (q4PosY < qUpperWhiskerY) {
						drawHistoMarker(gc, newSuitePoints[4], record.getColor(), timeLine.getDensity());
					}
					gc.drawLine(posX, qUpperWhiskerY, posX, q3PosY);
					gc.drawLine(posX - halfBoxWidth / 2, qUpperWhiskerY, posX + halfBoxWidth / 2, qUpperWhiskerY);
				}
				else if (record.isRangePlotSuite()) { // other suite members do not require a special treatment
					drawHistoMarker(gc, newSuitePoints[0], record.getColor(), timeLine.getDensity());
					// helper variables
					final int posX = newSuitePoints[0].x;
					if (oldSuitePoints[0] != null) {
						final int oldPosX = oldSuitePoints[0].x;
						if (log.isLoggable(Level.FINEST)) sb.append(GDE.LINE_SEPARATOR).append(suitePoints.get(0)[j].toString());
						// draw main curve
						gc.drawLine(oldPosX, oldSuitePoints[0].y, posX, newSuitePoints[0].y);
						// draw two extremum curves
						gc.setLineStyle(SWT.LINE_DASH);
						//						gc.drawLine(oldPosX, oldSuitePoints[1].y, posX, newSuitePoints[1].y);
						//						gc.drawLine(oldPosX, oldSuitePoints[2].y, posX, newSuitePoints[2].y);
						drawNullableLine(gc, oldSuitePoints[1], newSuitePoints[1]);
						drawNullableLine(gc, oldSuitePoints[2], newSuitePoints[2]);
					}
					// draw vertical connection lines sparsely dotted
					gc.setLineStyle(SWT.LINE_CUSTOM);
					gc.setLineDash(new int[] { 2, 9 });
					//					gc.drawLine(posX, newSuitePoints[1].y, posX, newSuitePoints[2].y);
					drawNullableLine(gc, newSuitePoints[1], newSuitePoints[2]);
					gc.setLineStyle(SWT.LINE_SOLID);
				}
				else {
					throw new UnsupportedOperationException();
				}
			}
		}
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, sb.toString());
	}

	/**
	 * draws a line from posA to posB.
	 * the y coordinate may be null.
	 */
	private static void drawNullableLine(GC gc, Point posA, Point posB) {
		if (posA != null && posB != null) {
			gc.drawLine(posA.x, posA.y, posB.x, posB.y);
		}
	}

	private static void drawHistoMarker(GC gc, Point newPoint, Color color, Density density) {
		if (density == HistoTimeLine.Density.LOW) {
			//Color last = gc.getBackground();
			//gc.setBackground(color);
			//gc.fillOval(newPoint.x - 4, newPoint.y - 4, 8, 8);
			//gc.setBackground(last);
			gc.drawOval(newPoint.x - 3, newPoint.y - 3, 6, 6);
		}
		else if (density == HistoTimeLine.Density.MEDIUM) {
			gc.drawOval(newPoint.x - 2, newPoint.y - 2, 4, 4);
		}
		else if (density == HistoTimeLine.Density.HIGH) {
			gc.drawOval(newPoint.x - 1, newPoint.y - 1, 2, 2);
		}
		else if (density == HistoTimeLine.Density.EXTREME) {
			gc.drawOval(newPoint.x - 1, newPoint.y - 1, 2, 2);
		}
	}
}
