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
package gde.histo.utils;

import static java.util.logging.Level.FINEST;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import gde.GDE;
import gde.config.Settings;
import gde.device.resource.DeviceXmlResource;
import gde.histo.recordings.HistoGraphicsMapper;
import gde.histo.recordings.PointArray;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.utils.HistoTimeLine.Density;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.utils.GraphicsUtils;
import gde.utils.MathUtils;

/**
 * Draw curves.
 * Support histo curves, suites of curves.
 * @author Thomas Eickert
 */
public final class HistoCurveUtils {
	private final static String		$CLASS_NAME	= HistoTimeLine.class.getName();
	private final static Logger		log					= Logger.getLogger($CLASS_NAME);

	private final static Settings	settings		= Settings.getInstance();

	/**
	 * Draw horizontal (curve) grid lines according the vector prepared during daring specified curve scale.
	 * @param recordSet
	 * @param gc the graphics context to be used
	 * @param bounds
	 * @param dashLineStyle to be used for the custom line style
	 */
	public static void drawCurveGrid(TrailRecordSet recordSet, GC gc, Rectangle bounds, int[] dashLineStyle) {
		gc.setLineWidth(1);
		gc.setLineDash(dashLineStyle);
		gc.setLineStyle(SWT.LINE_CUSTOM);
		gc.setForeground(recordSet.getHorizontalGridColor());

		Vector<Integer> horizontalGridVector = recordSet.getHorizontalGrid();
		for (int i = 0; i < horizontalGridVector.size(); i += recordSet.getHorizontalGridType()) {
			int y = horizontalGridVector.get(i);
			if (y > bounds.y && y < (bounds.y + bounds.height)) gc.drawLine(bounds.x, y, bounds.x + bounds.width, y);
		}
	}

	/**
	 * Draw the summary scale elements using given rectangle for display.
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
	public static void drawChannelItemScale(TrailRecord record, GC gc, int x0, int y0, int width, int height, int scaleWidthSpace,
			boolean isDrawScaleInRecordColor, boolean isDrawNameInRecordColor, boolean isDrawNumbersInRecordColor) {
		log.finer(() -> record.getName() + "  x0=" + x0 + " y0=" + y0 + " width=" + width + " height=" + height + " horizontalSpace=" + scaleWidthSpace); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		if (record.isEmpty() && !record.isDisplayable() && !record.isScaleVisible()) return; // nothing to display

		Double tmpMin = record.getSyncSummaryMin();
		Double tmpMax = record.getSyncSummaryMax();
		if (tmpMin == null || tmpMax == null) return; // missing min/max means no values at all

		// texts
		String minScaleText = record.getFormattedScaleValue(MathUtils.floorStepwise(tmpMin, tmpMax - tmpMin));
		String maxScaleText = record.getFormattedScaleValue(MathUtils.ceilStepwise(tmpMax, tmpMax - tmpMin));
		String graphText = DeviceXmlResource.getInstance().getReplacement(record.isScaleSyncMaster() ? record.getSyncMasterName() : record.getName());
		if (record.getSymbol() != null && record.getSymbol().length() > 0) graphText = graphText + "   " + record.getSymbol();
		if (record.getUnit() != null && record.getUnit().length() > 0) graphText = graphText + "   [" + record.getUnit() + "]"; //$NON-NLS-1$ //$NON-NLS-2$

		// prepare layout
		gc.setLineWidth(2);
		gc.setLineStyle(SWT.LINE_SOLID);
		int gap = 1;
		int xN = x0 + width;
		int yPos = y0 + height / 2;

		{
			Color color = isDrawScaleInRecordColor ? record.getColor() : DataExplorer.COLOR_BLACK;
			gc.setForeground(color);
			gc.drawLine(x0, y0 + 1, x0, y0 + height);
			gc.drawLine(xN, y0 + 1, xN, y0 + height);
		}
		{
			Color color = isDrawNumbersInRecordColor ? record.getColor() : DataExplorer.COLOR_BLACK;
			gc.setForeground(color);
			GraphicsUtils.drawTextCentered(minScaleText, x0 - gap - scaleWidthSpace / 2, yPos, gc, SWT.HORIZONTAL);
			GraphicsUtils.drawTextCentered(maxScaleText, xN + gap + scaleWidthSpace / 2, yPos, gc, SWT.HORIZONTAL);
		}
		{
			Color color = isDrawNameInRecordColor ? record.getColor() : DataExplorer.COLOR_BLACK;
			gc.setForeground(color);
			GraphicsUtils.drawTextCentered(graphText, x0 + width / 2, yPos, gc, SWT.HORIZONTAL);
		}
	}

	/**
	 * Draw the data graph scale using gives rectangle for display.
	 * All data point are multiplied with factor 1000 to avoid rounding errors for values below 1.0 (0.5 -> 0).
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
	 * @param numberTickMarks the number of ticks {numberTicks, numberMiniTicks}
	 */
	public static void drawHistoScale(TrailRecord record, GC gc, int x0, int y0, int width, int height, int scaleWidthSpace,
			boolean isDrawScaleInRecordColor, boolean isDrawNameInRecordColor, boolean isDrawNumbersInRecordColor, int[] numberTickMarks) {
		log.finer(() -> record.getName() + "  x0=" + x0 + " y0=" + y0 + " width=" + width + " height=" + height + " horizontalSpace=" + scaleWidthSpace); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		if (record.isEmpty() && !record.isDisplayable() && !record.isScaleVisible()) return; // nothing to display

		String graphText = DeviceXmlResource.getInstance().getReplacement(record.isScaleSyncMaster() ? record.getSyncMasterName() : record.getName());
		if (record.getSymbol() != null && record.getSymbol().length() > 0) graphText = graphText + "   " + record.getSymbol();
		if (record.getUnit() != null && record.getUnit().length() > 0) graphText = graphText + "   [" + record.getUnit() + "]"; //$NON-NLS-1$ //$NON-NLS-2$

		// adapt number space calculation to real displayed max number
		// Point pt = gc.textExtent(df.format(yMaxValueDisplay));
		// if (log.isLoggable(Level.FINE)) log.log(Level.FINE, df.format(yMaxValueDisplay) + " gc.textExtent = " + pt.toString());
		Point pt = gc.textExtent("000,00"); //$NON-NLS-1$
		int ticklength = 5;
		int gap = 10;

		// prepare axis position
		gc.setLineWidth(2);
		gc.setLineStyle(SWT.LINE_SOLID);
		boolean isPositionLeft = record.isPositionLeft();
		int positionNumber = record.getParentTrail().getAxisPosition(record.getName(), isPositionLeft);
		log.fine(() -> record.getName() + " positionNumber = " + positionNumber); //$NON-NLS-1$
		if (isDrawScaleInRecordColor)
			gc.setForeground(record.getColor()); // draw the main scale line in same color as the curve
		else
			gc.setForeground(DataExplorer.COLOR_BLACK);
		if (isPositionLeft) {
			int xPos = x0 - 1 - positionNumber * scaleWidthSpace;
			gc.drawLine(xPos, y0 + 1, xPos, y0 - height - 1); // xPos = x0
			log.fine(() -> "y-Achse = " + xPos + ", " + y0 + ", " + xPos + ", " + (y0 - height)); // yMax //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			GraphicsUtils.drawVerticalTickMarks(record, gc, xPos, y0, height, record.getMinScaleValue(), record.getMaxScaleValue(), ticklength, numberTickMarks[1], gap, isPositionLeft, numberTickMarks[0], isDrawNumbersInRecordColor);
			log.finest(() -> "drawText x = " + (xPos - pt.y - 15)); // xPosition Text Spannung [] //$NON-NLS-1$
			if (isDrawNameInRecordColor)
				gc.setForeground(record.getColor());
			else
				gc.setForeground(DataExplorer.COLOR_BLACK);
			GraphicsUtils.drawTextCentered(graphText, (xPos - scaleWidthSpace + 3), y0 / 2 + (y0 - height), gc, SWT.UP);
		} else {
			int xPos = x0 + 1 + width + positionNumber * scaleWidthSpace;
			gc.drawLine(xPos, y0 + 1, xPos, y0 - height - 1); // yMax
			log.finest(() -> "y-Achse = " + xPos + ", " + y0 + ", " + xPos + ", " + (y0 - height)); // yMax //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			GraphicsUtils.drawVerticalTickMarks(record, gc, xPos, y0, height, record.getMinScaleValue(), record.getMaxScaleValue(), ticklength, numberTickMarks[1], gap, isPositionLeft, numberTickMarks[0], isDrawNumbersInRecordColor);
			if (isDrawNameInRecordColor)
				gc.setForeground(record.getColor());
			else
				gc.setForeground(DataExplorer.COLOR_BLACK);
			GraphicsUtils.drawTextCentered(graphText, (xPos + scaleWidthSpace - pt.y - 5), y0 / 2 + (y0 - height), gc, SWT.UP);
		}
	}

	/**
	 * Draw single curve.
	 * Support logarithmic distances and elaborate x-axis point placement based on box width and number of points.
	 * @param record
	 * @param gc
	 * @param x0
	 * @param y0
	 * @param width
	 * @param height
	 * @param timeLine
	 */
	public static void drawHistoCurve(TrailRecord record, GC gc, int x0, int y0, int width, int height, HistoTimeLine timeLine) {
		log.fine(() -> record.getName() + String.format(" x0 = %d, y0 = %d, width = %d, height = %d", x0, y0, width, height)); //$NON-NLS-1$
		log.finer(() -> "curve area bounds = " + record.getParentTrail().getDrawAreaBounds().toString()); //$NON-NLS-1$

		// set line properties according adjustment
		gc.setForeground(record.getColor());
		gc.setLineWidth(record.getLineWidth());
		gc.setLineStyle(record.getLineStyle());

		// get the number of data points size to be drawn
		int displayableSize = record.realSize();
		log.fine(() -> "displayableSize = " + displayableSize); //$NON-NLS-1$

		// record.setDisplayScaleFactorTime(1);// x-axis scaling not supported
		record.setDisplayScaleFactorValue(height);

		StringBuffer sb = new StringBuffer(); // logging purpose

		// draw scaled points to draw area - measurements can only be drawn starting with the first measurement point
		Point[] points = new HistoGraphicsMapper(record).getDisplayPoints(timeLine, x0, y0);

		Point newPoint, oldPoint = null;
		for (int j = 0; j < points.length && j <= displayableSize && displayableSize >= 1; j++) {
			if ((newPoint = points[j]) != null) { // in case of a suite the master triggers the display of all trails
				drawHistoMarker(gc, newPoint, timeLine.getDensity());
				if (oldPoint != null) {
					if (log.isLoggable(FINEST)) sb.append(GDE.LINE_SEPARATOR).append(newPoint.toString());
					gc.drawLine(oldPoint.x, oldPoint.y, newPoint.x, newPoint.y);
				}
				oldPoint = newPoint; // remember the last draw point for next drawLine operation
			}
		}
		log.finest(() -> sb.toString());
	}

	/**
	 * Draw multiple curves (e.g. trails for min, max, avg).
	 * Support logarithmic distances and elaborate x-axis point placement based on box width and number of points.
	 * @param record holds display properties and the reference to the suite trail records but no data
	 * @param gc
	 * @param x0
	 * @param y0
	 * @param width
	 * @param height
	 * @param timeLine
	 */
	public static void drawHistoSuite(TrailRecord record, GC gc, int x0, int y0, int width, int height, HistoTimeLine timeLine) {
		log.fine(() -> record.getName() + String.format(" x0 = %d, y0 = %d, width = %d, height = %d", x0, y0, width, height)); //$NON-NLS-1$
		log.finer(() -> "curve area bounds = " + record.getParentTrail().getDrawAreaBounds().toString()); //$NON-NLS-1$
		log.fine(() -> String.format("MinScaleValue=%f   MaxScaleValue=%f   MinDisplayValue=%f   MaxDisplayValue=%f", record.getMinScaleValue(), //$NON-NLS-1$
				record.getMaxScaleValue(), record.getMinDisplayValue(), record.getMaxDisplayValue()));

		// set line properties according adjustment
		gc.setForeground(record.getColor());
		gc.setLineWidth(record.getLineWidth());
		gc.setLineStyle(record.getLineStyle());

		// int xScaleFactor = 1; // x-axis scaling not supported
		// record.setDisplayScaleFactorTime(xScaleFactor);
		record.setDisplayScaleFactorValue(height);
		if (record.getTrailSelector().isBoxPlotSuite()) {
			drawBoxPlot(record, gc, timeLine);
		} else if (record.getTrailSelector().isRangePlotSuite()) {
			drawRangePlot(record, gc, timeLine);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * @param record
	 * @param gc
	 * @param timeLine
	 */
	public static void drawRangePlot(TrailRecord record, GC gc, HistoTimeLine timeLine) {
		StringBuffer sb = new StringBuffer(); // logging purpose
		// draw scaled points to draw area - measurements can only be drawn starting with the first measurement point
		List<PointArray> suitePoints = new HistoGraphicsMapper(record).getSuiteDisplayPoints(timeLine);
		PointArray oldPoints = null;

		for (PointArray pointArray : suitePoints) {
			if (pointArray == null) {
				; // neither boxplot nor the rangeplot needs this information
			} else {
				final int posX = pointArray.getX();
				drawHistoMarker(gc, new Point(posX, pointArray.getY(0)), timeLine.getDensity());
				// helper variables
				if (oldPoints == null) {
					; // no connecting lines required
				} else {
					final int oldPosX = oldPoints.getX();
					if (log.isLoggable(FINEST)) sb.append(GDE.LINE_SEPARATOR).append(Arrays.toString(pointArray.getY()));
					// draw main curve
					gc.drawLine(oldPosX, oldPoints.getY(0), posX, pointArray.getY(0));
					// draw two extremum curves
					gc.setLineStyle(SWT.LINE_DASH);
					// gc.drawLine(oldPosX, oldSuitePoints[1].y, posX, newSuitePoints[1].y);
					// gc.drawLine(oldPosX, oldSuitePoints[2].y, posX, newSuitePoints[2].y);
					drawNullableLine(gc, new Point(oldPosX, oldPoints.getY(1)), new Point(posX, pointArray.getY(1)));
					drawNullableLine(gc, new Point(oldPosX, oldPoints.getY(2)), new Point(posX, pointArray.getY(2)));
				}
				// draw vertical connection lines sparsely dotted
				gc.setLineStyle(SWT.LINE_CUSTOM);
				gc.setLineDash(new int[] { 2, 9 });
				// gc.drawLine(posX, newSuitePoints[1].y, posX, newSuitePoints[2].y);
				drawNullableLine(gc, new Point(posX, pointArray.getY(1)), new Point(posX, pointArray.getY(2)));
				gc.setLineStyle(SWT.LINE_SOLID);

				oldPoints = pointArray;
			}
		}
		log.finest(() -> sb.toString());
	}

	/**
	 * @param record
	 * @param gc
	 * @param timeLine
	 */
	public static void drawBoxPlot(TrailRecord record, GC gc, HistoTimeLine timeLine) {
		StringBuffer sb = new StringBuffer(); // logging purpose
		List<PointArray> suitePoints = new HistoGraphicsMapper(record).getSuiteDisplayPoints(timeLine);

		List<Integer> durations_mm = record.getParentTrail().getDurations_mm();
		double averageDuration = durations_mm.parallelStream().mapToDouble(d -> d).average().getAsDouble();
		Iterator<Integer> durationIterator = durations_mm.iterator();

		int boxWidth = timeLine.getDensity().getScaledBoxWidth();
		// divison by 3 is the best fit divisor; 2 results in bigger modulation rates
		double boxSizeFactor = timeLine.getDensity().boxWidthAmplitude * HistoCurveUtils.settings.getBoxplotSizeAdaptationOrdinal() / 3.0;

		for (PointArray pointArray : suitePoints) {
			if (pointArray == null) {
				; // neither boxplot nor the rangeplot needs this information
			} else {
				if (log.isLoggable(FINEST)) sb.append(GDE.LINE_SEPARATOR).append(Arrays.toString(pointArray.getY()));
				// helper variables
				final int posX = pointArray.getX();
				final int q0PosY = pointArray.getY(0), q1PosY = pointArray.getY(1), q2PosY = pointArray.getY(2), q3PosY = pointArray.getY(3),
						q4PosY = pointArray.getY(4), qLowerWhiskerY = pointArray.getY(5), qUpperWhiskerY = pointArray.getY(6);
				final int interQuartileRange = q1PosY - q3PosY;
				int halfBoxWidth = (int) (boxWidth * (1. + (Math.sqrt(durationIterator.next() / averageDuration) - 1) * boxSizeFactor) / 2.);
				halfBoxWidth = halfBoxWidth < 1 ? 1 : halfBoxWidth;
				// draw main box
				gc.drawRectangle(posX - halfBoxWidth, q3PosY, halfBoxWidth * 2, interQuartileRange);
				gc.drawLine(posX - halfBoxWidth, q2PosY, posX + halfBoxWidth, q2PosY);
				// draw min and lower whisker
				if (q0PosY > qLowerWhiskerY) {
					drawHistoMarker(gc, new Point(posX, pointArray.getY(0)), timeLine.getDensity());
				}
				gc.drawLine(posX, qLowerWhiskerY, posX, q1PosY);
				gc.drawLine(posX - halfBoxWidth / 2, qLowerWhiskerY, posX + halfBoxWidth / 2, qLowerWhiskerY);
				// draw max and upper whisker
				if (q4PosY < qUpperWhiskerY) {
					drawHistoMarker(gc, new Point(posX, pointArray.getY(4)), timeLine.getDensity());
				}
				gc.drawLine(posX, qUpperWhiskerY, posX, q3PosY);
				gc.drawLine(posX - halfBoxWidth / 2, qUpperWhiskerY, posX + halfBoxWidth / 2, qUpperWhiskerY);
			}
		}
		log.finest(() -> sb.toString());
	}

	/**
	 * Draw a line from posA to posB.
	 * The y coordinate may be null.
	 */
	private static void drawNullableLine(GC gc, Point posA, Point posB) {
		if (posA != null && posB != null) {
			gc.drawLine(posA.x, posA.y, posB.x, posB.y);
		}
	}

	private static void drawHistoMarker(GC gc, Point newPoint, Density density) {
		if (density == HistoTimeLine.Density.LOW) {
			// Color last = gc.getBackground();
			// gc.setBackground(color);
			// gc.fillOval(newPoint.x - 4, newPoint.y - 4, 8, 8);
			// gc.setBackground(last);
			gc.drawOval(newPoint.x - 3, newPoint.y - 3, 6, 6);
		} else if (density == HistoTimeLine.Density.MEDIUM) {
			gc.drawOval(newPoint.x - 2, newPoint.y - 2, 4, 4);
		} else if (density == HistoTimeLine.Density.HIGH) {
			gc.drawOval(newPoint.x - 1, newPoint.y - 1, 2, 2);
		} else if (density == HistoTimeLine.Density.EXTREME) {
			gc.drawOval(newPoint.x - 1, newPoint.y - 1, 2, 2);
		}
	}

}
