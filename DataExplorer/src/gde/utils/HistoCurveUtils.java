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
import gde.data.HistoSet;
import gde.data.Record;
import gde.data.TrailRecord;
import gde.data.TrailRecord.TrailType;
import gde.device.IDevice;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.HistoTimeLine.Density;

import java.awt.BasicStroke;
import java.awt.Stroke;
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
public class HistoCurveUtils extends CurveUtils { // TODO merging with CurveUtils reduces number of classes
	private final static String	$CLASS_NAME	= HistoTimeLine.class.getName();
	private final static Logger	log			= Logger.getLogger($CLASS_NAME);

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
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, record.getName() + String.format(" x0 = %d, y0 = %d, width = %d, height = %d", x0, y0, width, height)); //$NON-NLS-1$
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "curve area bounds = " + record.getParent().getDrawAreaBounds().toString()); //$NON-NLS-1$

		// set line properties according adjustment
		gc.setForeground(record.getColor());
		gc.setLineWidth(record.getLineWidth());
		gc.setLineStyle(record.getLineStyle());

		// get the number of data points size to be drawn
		int displayableSize = record.size();

		// calculate time line adaption if record set is compare set, compare set max have different times for each record, (intRecordSize - 1) is number of time deltas for calculation
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "average record time step msec = " + record.getAverageTimeStep_ms()); //$NON-NLS-1$
		double displayableTime_ms = record.getDrawTimeWidth_ms();
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "displayableSize = " + displayableSize + " displayableTime_ms = " + displayableTime_ms); //$NON-NLS-1$ //$NON-NLS-2$

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
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, "xTimeFactor = " + xTimeFactor + " xScaleFactor = " + xScaleFactor + " : " + (xTimeFactor * xScaleFactor)); //$NON-NLS-1$ //$NON-NLS-2$
			record.setDisplayScaleFactorTime(xTimeFactor);
			record.setDisplayScaleFactorValue(height);
		} else {
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
				if (log.isLoggable(Level.FINEST))
					sb.append(GDE.LINE_SEPARATOR).append(oldPoint1.toString());
			} catch (RuntimeException e) {
				log.log(Level.SEVERE, e.getMessage() + " zoomed compare set ?", e); //$NON-NLS-1$
			}
		}

		Point[] points = null;
		List<Point[]> suitePoints = new ArrayList<Point[]>();
		Point[] oldSuitePoints = new Point[3];
		int boxWidth = 0; // boxplot only
		// draw scaled points to draw area - measurements can only be drawn starting with the first measurement point
		if (record.getParent().isCompareSet()) {// TODO not supported // compare set might contain records with different size
			throw new UnsupportedOperationException();
		} else if (record.getDevice().isGPSCoordinates(record)) {
			points = record.getGpsDisplayPoints(timeLine, x0, y0);
		} else if (record.getTrailRecordSuite().length > 1) {
			for (TrailRecord trailRecord : record.getTrailRecordSuite()) {
				points = trailRecord.getDisplayPoints(timeLine, x0, y0);
				suitePoints.add(points);
			}
			boxWidth = timeLine.getDensity().getBoxWidth();
		} else {
			points = record.getDisplayPoints(timeLine, x0, y0);
		}
		Point newPoint, oldPoint = null;
		for (int j = 0; j < points.length && j <= displayableSize && displayableSize > 1; j++) {
			if ((newPoint = points[j]) != null) { // in case of a suite the master triggers the display of all trails
				drawHistoMarker(gc, newPoint, record.getColor(), timeLine.getDensity());
				if (oldPoint != null) {
					if (log.isLoggable(Level.FINEST))
						sb.append(GDE.LINE_SEPARATOR).append(newPoint.toString());
					gc.drawLine(oldPoint.x, oldPoint.y, newPoint.x, newPoint.y);
				}
				oldPoint = newPoint; // remember the last draw point for next drawLine operation
			}
		}
		if (log.isLoggable(Level.FINEST))
			log.log(Level.FINEST, sb.toString());
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
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, record.getName() + String.format(" x0 = %d, y0 = %d, width = %d, height = %d", x0, y0, width, height)); //$NON-NLS-1$
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, "curve area bounds = " + record.getParent().getDrawAreaBounds().toString()); //$NON-NLS-1$

		// set line properties according adjustment
		gc.setForeground(record.getColor());
		gc.setLineWidth(record.getLineWidth());
		gc.setLineStyle(record.getLineStyle());

		// get record from the suite which is decisive for the time scale
		TrailRecord masterRecord = record.getTrailRecordSuite()[0];
		int displayableSize = masterRecord.size();
		double displayableTime_ms = masterRecord.getDrawTimeWidth_ms();

		// calculate time line adaption if record set is compare set, compare set max have different times for each record, (intRecordSize - 1) is number of time deltas for calculation
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "average record time step msec = " + masterRecord.getAverageTimeStep_ms()); //$NON-NLS-1$
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, "displayableSize = " + displayableSize + " displayableTime_ms = " + displayableTime_ms); //$NON-NLS-1$ //$NON-NLS-2$
		{
			int xScaleFactor  = 1; // x-axis scaling not supported
			record.setDisplayScaleFactorTime(xScaleFactor);
			record.setDisplayScaleFactorValue(height);
		}

		StringBuffer sb = new StringBuffer(); // logging purpose

		List<Point[]> suitePoints = new ArrayList<Point[]>(); // display point cache: one row for each record of the suite
		int boxWidth = 0; // boxplot only
		if (record.getParent().isCompareSet()) {// TODO not supported // compare set might contain records with different size
			throw new UnsupportedOperationException();
		} else if (record.getDevice().isGPSCoordinates(record)) { // TODO not supported
			// points = record.getGpsDisplayPoints(timeLine, x0, y0);
			throw new UnsupportedOperationException();
		} else if (record.getTrailRecordSuite().length > 1) {
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
			boxWidth = timeLine.getDensity().getBoxWidth();
		} else {
			throw new UnsupportedOperationException();
		}
		Point[] newSuitePoints = new Point[record.getTrailRecordSuite().length]; // all points for the current x-axis position
		Point[] oldSuitePoints = new Point[record.getTrailRecordSuite().length]; // all points for the previous x-axis position
		for (int j = 0; j < suitePoints.get(0).length && j <= displayableSize && displayableSize > 1; j++) {
			if ((suitePoints.get(0)[j]) != null) { // in case of a suite the master triggers the display of all trails
				for (int i = 0; i < record.getTrailRecordSuite().length; i++) {
					oldSuitePoints[i] = newSuitePoints[i];
					newSuitePoints[i] = suitePoints.get(i)[j];
				}
				if (record.getTrailType().equals(TrailType.SUITE_BOX_PLOT)) { // 0=AVG, 1=Q0,2=Q1,3=Q2,4=Q3,5=Q4, 6=O1,7=O7
					if (log.isLoggable(Level.FINEST))
						sb.append(GDE.LINE_SEPARATOR).append(newSuitePoints[0].toString());
					// main box
					int elementHeight = newSuitePoints[2].y - newSuitePoints[3].y;
					gc.drawRectangle(newSuitePoints[0].x - boxWidth / 2, newSuitePoints[0].y - elementHeight / 2, boxWidth, elementHeight);
					elementHeight = newSuitePoints[4].y - newSuitePoints[3].y;
					gc.drawRectangle(-1 + newSuitePoints[0].x - boxWidth / 2, newSuitePoints[0].y - elementHeight / 2, 2 + boxWidth, elementHeight);
					// antennas
					elementHeight = newSuitePoints[6].y - newSuitePoints[2].y;
					gc.drawLine(newSuitePoints[0].x, newSuitePoints[6].y, newSuitePoints[0].x, newSuitePoints[2].y);
					gc.drawLine(newSuitePoints[0].x, newSuitePoints[7].y, newSuitePoints[0].x, newSuitePoints[4].y);
					int elementHalfWidth = boxWidth * 3 / 8;
					gc.drawLine(newSuitePoints[0].x - elementHalfWidth, newSuitePoints[6].y, newSuitePoints[0].x + elementHalfWidth, newSuitePoints[6].y);
					gc.drawLine(newSuitePoints[0].x - elementHalfWidth, newSuitePoints[7].y, newSuitePoints[0].x + elementHalfWidth, newSuitePoints[7].y);
					// avg, min, max markers
					drawHistoMarker(gc, newSuitePoints[0], record.getColor(), timeLine.getDensity());
					drawHistoMarker(gc, newSuitePoints[1], record.getColor(), timeLine.getDensity());
					drawHistoMarker(gc, newSuitePoints[5], record.getColor(), timeLine.getDensity());
				} else if (record.getTrailType().isSuite()) { // other suite members do not require special treatment
					drawHistoMarker(gc, newSuitePoints[0], record.getColor(), timeLine.getDensity());
					if (oldSuitePoints[0] != null) {
						if (log.isLoggable(Level.FINEST))
							sb.append(GDE.LINE_SEPARATOR).append(suitePoints.get(0)[j].toString());
						// main curve
						gc.drawLine(oldSuitePoints[0].x, oldSuitePoints[0].y, newSuitePoints[0].x, newSuitePoints[0].y);
						// two extremum curves
						gc.setLineStyle(SWT.LINE_DASH); 
						gc.drawLine(oldSuitePoints[1].x, oldSuitePoints[1].y, newSuitePoints[1].x, newSuitePoints[1].y);
						gc.drawLine(oldSuitePoints[2].x, oldSuitePoints[2].y, newSuitePoints[2].x, newSuitePoints[2].y);
						// vertical connection lines sparsely dotted
						gc.setLineStyle(SWT.LINE_CUSTOM); 
						gc.setLineDash(new int[] {1, 9});
						gc.drawLine(newSuitePoints[1].x, newSuitePoints[1].y, newSuitePoints[2].x, newSuitePoints[2].y);
						gc.setLineStyle(SWT.LINE_SOLID);
					}
				} else {
					throw new UnsupportedOperationException();
				}
			}
		}
		if (log.isLoggable(Level.FINEST))
			log.log(Level.FINEST, sb.toString());
	}

	private static void drawHistoMarker(GC gc, Point newPoint, Color color, Density density) {
		if (density == HistoTimeLine.Density.LOW) {
			Color last = gc.getBackground();
			gc.setBackground(color);
			gc.fillOval(newPoint.x - 4, newPoint.y - 4, 8, 8);
			gc.setBackground(last);
		} else if (density == HistoTimeLine.Density.MEDIUM) {
			gc.drawOval(newPoint.x - 2, newPoint.y - 2, 4, 4);
		} else if (density == HistoTimeLine.Density.HIGH) {
			gc.drawOval(newPoint.x - 1, newPoint.y - 1, 2, 2);
		}
	}
}
