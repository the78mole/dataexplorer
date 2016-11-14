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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
    					2016 Thomas Eickert
****************************************************************************************/
package gde.utils;

import gde.GDE;
import gde.config.Settings;
import gde.data.Record;
import gde.data.TrailRecord;
import gde.data.TrailRecordSet;
import gde.log.Level;
import gde.utils.HistoTimeLine.Density;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

/**
 * utilities to draw curves.
 * support histo curves, suites of curves.
 * vertical scales for histo graphics are supported by the base class.
 * @author Thomas Eickert
 */
public class HistoCurveUtils extends CurveUtils { // todo merging with CurveUtils reduces number of classes
	private final static String		$CLASS_NAME	= HistoTimeLine.class.getName();
	private final static Logger		log					= Logger.getLogger($CLASS_NAME);

	private final static Settings	settings		= Settings.getInstance();

	/**
	 * method draw the curve using the given graphics context (GC)
	 * @param record
	 * @param gc
	 * @param x0
	 * @param y0
	 * @param width
	 * @param height
	 * @param isCompareSet
	 */
	@Deprecated
	public static void drawCurve(Record record, GC gc, int x0, int y0, int width, int height, boolean isCompareSet) {

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

		// calculate time line adaption if record set is compare set, compare set max have different times for each record, (intRecordSize - 1) is number of time deltas for calculation
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "average record time step msec = " + record.getAverageTimeStep_ms()); //$NON-NLS-1$
		double displayableTime_ms = record.getDrawTimeWidth_ms();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "displayableSize = " + displayableSize + " displayableTime_ms = " + displayableTime_ms); //$NON-NLS-1$ //$NON-NLS-2$

		int xScaleFactor; // x-axis scaling not supported
		if (false) {
			// calculate xScale for curves with much to many data points, it makes no sense to draw all the small lines on the same part of the screen
			xScaleFactor = (int) (displayableSize / (width * 2.2));
			xScaleFactor = xScaleFactor > 0 ? xScaleFactor : 1; // check for curves with less points than draw area width
			while (displayableSize % xScaleFactor > 3 && xScaleFactor > 1) {
				--xScaleFactor;
			}
			// xScaleFactor+=2;
			// calculate scale factor to fit time into draw bounds display pixel based
			double xTimeFactor = width / displayableTime_ms; // * (xScaleFactor - 0.44);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "xTimeFactor = " + xTimeFactor + " xScaleFactor = " + xScaleFactor + " : " + (xTimeFactor * xScaleFactor)); //$NON-NLS-1$ //$NON-NLS-2$
			record.setDisplayScaleFactorTime(xTimeFactor);
			record.setDisplayScaleFactorValue(height);
		}
		else {
			xScaleFactor = 1;
			record.setDisplayScaleFactorTime(1);
			record.setDisplayScaleFactorValue(height);
		}

		StringBuffer sb = new StringBuffer(); // logging purpose

		Point oldPoint1 = null;
		if (false) {
			// do NOT draw the first point with possible interpolated values if it does not match a measurement point at time value
			try {
				// calculate start point of the curve, which is the first oldPoint
				// oldPoint = record.getParent().isScopeMode() ? record.getDisplayPoint(0, x0, y0) : record.getDisplayEndPoint(0, x0);
				int xPosNewestPoint = timeLine.getScalePositions().firstEntry().getValue(); // is the leftmost point only if the scale is not reversed (reversed is standard)
				oldPoint1 = record.getDisplayEndPoint(xPosNewestPoint);
				if (log.isLoggable(Level.FINEST)) sb.append(GDE.LINE_SEPARATOR).append(oldPoint1.toString());
			}
			catch (RuntimeException e) {
				log.log(Level.SEVERE, e.getMessage() + " zoomed compare set ?", e); //$NON-NLS-1$
			}
		}

		Point[] points = null;
		List<Point[]> suitePoints = new ArrayList<>();
		Point[] oldSuitePoints = new Point[3];
		int boxWidth = 0; // boxplot only
		// draw scaled points to draw area - measurements can only be drawn starting with the first measurement point
		if (record.getParent().isCompareSet()) {// todo not supported // compare set might contain records with different size
			throw new UnsupportedOperationException();
		}
		else if (record.getDevice().isGPSCoordinates(record)) {
			points = record.getGpsDisplayPoints(timeLine, x0, y0);
		}
		else if (record.getTrailRecordSuite().length > 1) {
			for (TrailRecord trailRecord : record.getTrailRecordSuite()) {
				points = trailRecord.getDisplayPoints(timeLine, x0, y0);
				suitePoints.add(points);
			}
			boxWidth = timeLine.getDensity().getScaledBoxWidth();
		}
		else {
			points = record.getDisplayPoints(timeLine, x0, y0);
		}
		Point newPoint, oldPoint = null;
		for (int j = 0; j < points.length && j <= displayableSize && displayableSize > 1; j++) {
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

		// set line properties according adjustment
		gc.setForeground(record.getColor());
		gc.setLineWidth(record.getLineWidth());
		gc.setLineStyle(record.getLineStyle());

		// get record from the suite which is decisive for the time scale
		TrailRecord masterRecord = record.getTrailRecordSuite()[0];
		int displayableSize = masterRecord.size();
		double displayableTime_ms = masterRecord.getDrawTimeWidth_ms();

		// calculate time line adaption if record set is compare set, compare set max have different times for each record, (intRecordSize - 1) is number of time deltas for calculation
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "average record time step msec = " + masterRecord.getAverageTimeStep_ms()); //$NON-NLS-1$
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "displayableSize = " + displayableSize + " displayableTime_ms = " + displayableTime_ms); //$NON-NLS-1$ //$NON-NLS-2$
		{
			int xScaleFactor = 1; // x-axis scaling not supported
			record.setDisplayScaleFactorTime(xScaleFactor);
			record.setDisplayScaleFactorValue(height);
		}

		StringBuffer sb = new StringBuffer(); // logging purpose

		List<Point[]> suitePoints = new ArrayList<>(); // display point cache: one row for each record of the suite
		int boxWidth = 0; // boxplot only
		if (record.getParent().isCompareSet()) {// todo not supported // compare set might contain records with different size
			throw new UnsupportedOperationException();
		}
		else if (record.getDevice().isGPSCoordinates(record)) { // todo not supported
			// points = record.getGpsDisplayPoints(timeLine, x0, y0);
			throw new UnsupportedOperationException();
		}
		else if (record.getTrailRecordSuite().length > 1) {
			for (TrailRecord trailRecord : record.getTrailRecordSuite()) {
				// this was done in drawScale for the record
				trailRecord.setMinScaleValue(record.getMinScaleValue());
				trailRecord.setMaxScaleValue(record.getMaxScaleValue());
				trailRecord.setMinDisplayValue(record.getMinDisplayValue());
				trailRecord.setMaxDisplayValue(record.getMaxDisplayValue());
				// this was done here for the record
				trailRecord.setDisplayScaleFactorTime(1);
				trailRecord.setDisplayScaleFactorValue(height); // for getDisplayPoints
				suitePoints.add(trailRecord.getDisplayPoints(timeLine, x0, y0));
			}
			boxWidth = timeLine.getDensity().getScaledBoxWidth();
		}
		else {
			throw new UnsupportedOperationException();
		}
		double averageDuration = ((TrailRecordSet) record.getParent()).getAverageDuration_mm();
		List<Integer> durations_mm = ((TrailRecordSet) record.getParent()).getDurations_mm();
		Point[] newSuitePoints = new Point[record.getTrailRecordSuite().length]; // all points for the current x-axis position
		Point[] oldSuitePoints = new Point[record.getTrailRecordSuite().length]; // all points for the previous x-axis position
		for (int j = 0; j < suitePoints.get(0).length && j <= displayableSize && displayableSize > 1; j++) {
			if ((suitePoints.get(0)[j]) != null) { // in case of a suite the master triggers the display of all trails
				for (int i = 0; i < record.getTrailRecordSuite().length; i++) {
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
					int halfBoxWidth = (int) (boxWidth
							* (1. + (Math.sqrt(durations_mm.get(j) / averageDuration) - 1) * timeLine.getDensity().boxWidthAmplitude * HistoCurveUtils.settings.getBoxplotSizeAdaptationOrdinal() / 3.) / 2.); // divison by 3 is the best fit divisor; 2 results in bigger modulation rates
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
						gc.drawLine(oldPosX, oldSuitePoints[1].y, posX, newSuitePoints[1].y);
						gc.drawLine(oldPosX, oldSuitePoints[2].y, posX, newSuitePoints[2].y);
					}
					// draw vertical connection lines sparsely dotted
					gc.setLineStyle(SWT.LINE_CUSTOM);
					gc.setLineDash(new int[] { 2, 9 });
					gc.drawLine(posX, newSuitePoints[1].y, posX, newSuitePoints[2].y);
					gc.setLineStyle(SWT.LINE_SOLID);
				}
				else {
					throw new UnsupportedOperationException();
				}
			}
		}
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, sb.toString());
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
