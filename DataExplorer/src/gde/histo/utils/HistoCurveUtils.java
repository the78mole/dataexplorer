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

import static gde.histo.utils.UniversalQuantile.BoxplotItems.LOWER_WHISKER;
import static gde.histo.utils.UniversalQuantile.BoxplotItems.QUARTILE1;
import static gde.histo.utils.UniversalQuantile.BoxplotItems.QUARTILE2;
import static gde.histo.utils.UniversalQuantile.BoxplotItems.QUARTILE3;
import static gde.histo.utils.UniversalQuantile.BoxplotItems.UPPER_WHISKER;
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
import gde.histo.ui.data.SummarySpots.OutlierWarning;
import gde.histo.utils.HistoTimeLine.Density;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.GraphicsUtils;

/**
 * Draw curves.
 * Support histo curves, suites of curves.
 * @author Thomas Eickert
 */
public final class HistoCurveUtils {
	private final static String		$CLASS_NAME	= HistoCurveUtils.class.getName();
	private final static Logger		log					= Logger.getLogger($CLASS_NAME);

	private final static Settings	settings		= Settings.getInstance();

	/**
	 * Draw the vertical fences.
	 */
	public static void drawCurveAreaBorders(GC gc, Rectangle bounds, Color borderColor) {
		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setForeground(borderColor);

		int xMax = bounds.x + bounds.width;
		int y0 = bounds.y + bounds.height;
		gc.drawLine(bounds.x - 1, bounds.y - 1, bounds.x - 1, y0); // left fence
		gc.drawLine(xMax + 1, bounds.y - 1, xMax + 1, y0); // right fence
	}

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
		gc.setForeground(recordSet.getValueGridColor());

		Vector<Integer> horizontalGridVector = recordSet.getValueGrid();
		for (int i = 0; i < horizontalGridVector.size(); i += recordSet.getValueGridType()) {
			int y = horizontalGridVector.get(i);
			if (y > bounds.y && y < (bounds.y + bounds.height)) gc.drawLine(bounds.x, y, bounds.x + bounds.width, y);
		}
	}

	/**
	 * Draw the summary scale elements using given rectangle for display.
	 * @param record
	 * @param gc
	 * @param drawStripBounds holds the drawing area strip based on the upper left position
	 * @param scaleWidthSpace is the width of the left / right scale in pixels
	 * @param decodedMinScale is the value corresponding to the left border (xPos = 0)
	 * @param decodedMaxScale is the value corresponding to the right border (xPos = width)
	 * @param isDrawNumbersInRecordColor
	 */
	public static void drawChannelItemBoxplot(TrailRecord record, GC gc, Rectangle drawStripBounds, int scaleWidthSpace, double decodedMinScale,
			double decodedMaxScale, boolean isDrawNumbersInRecordColor, boolean isDrawNumbers) {
		if (record.isEmpty() && !record.isDisplayable() && !record.isScaleVisible()) return; // nothing to display

		List<String> scaleTexts = Arrays.asList(new String[] { record.getFormattedScaleValue(decodedMinScale),
				record.getFormattedScaleValue(decodedMaxScale) });

		// prepare layout
		gc.setLineWidth(2);
		gc.setLineStyle(SWT.LINE_SOLID);
		int yPos = drawStripBounds.y + drawStripBounds.height / 2;
		Point pt = gc.textExtent("0");
		int scaleXGap = pt.x; // free distance between two scale numbers
		int scaleYGap = pt.y / 2 + 1; // free distance between the boxplot and the scale numbers
		int scaleY0 = drawStripBounds.y + drawStripBounds.height + scaleYGap;

		int halfStdBoxHeight = drawStripBounds.height / 2 - 5;
		int scaledHalfBoxHeight = halfStdBoxHeight + Settings.getInstance().getBoxplotScaleOrdinal() * 2;

		int[] tukeyXPositions = record.getSummarySpots().defineTukeyXPositions();
		int xPosQ1 = drawStripBounds.x + tukeyXPositions[QUARTILE1.ordinal()];
		int xPosQ2 = drawStripBounds.x + tukeyXPositions[QUARTILE2.ordinal()];
		int xPosQ3 = drawStripBounds.x + tukeyXPositions[QUARTILE3.ordinal()];
		{
			gc.setForeground(record.getColor());
			int boxOffset = (drawStripBounds.height - scaledHalfBoxHeight * 2) / 2;
			gc.drawLine(xPosQ2, yPos - scaledHalfBoxHeight, xPosQ2, yPos + scaledHalfBoxHeight);
			gc.drawRectangle(xPosQ1, drawStripBounds.y + boxOffset, xPosQ3 - xPosQ1, scaledHalfBoxHeight * 2);
		}
		if (isDrawNumbers) {
			gc.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE - 1, SWT.NORMAL));
			Color color = isDrawNumbersInRecordColor ? record.getColor() : DataExplorer.COLOR_BLACK;
			gc.setForeground(color);
			double[] tukeyBoxPlot = record.getQuantile().getTukeyBoxPlot();

			String q1Text = record.getFormattedRangeValue(tukeyBoxPlot[QUARTILE1.ordinal()], decodedMaxScale - decodedMinScale);
			Point ptQ1 = gc.textExtent(q1Text);
			if (!scaleTexts.contains(q1Text)) {
				GraphicsUtils.drawTextCentered(q1Text, xPosQ1, scaleY0, gc, SWT.HORIZONTAL);
			}
			String q3Text = record.getFormattedRangeValue(tukeyBoxPlot[QUARTILE3.ordinal()], decodedMaxScale - decodedMinScale);
			Point ptQ3 = gc.textExtent(q3Text);
			if ((ptQ3.x + ptQ1.x) / 2 + scaleXGap < xPosQ3 - xPosQ1 && !scaleTexts.contains(q3Text)) {
				GraphicsUtils.drawTextCentered(q3Text, xPosQ3, scaleY0, gc, SWT.HORIZONTAL);
			}
			String q2Text = record.getFormattedRangeValue(tukeyBoxPlot[QUARTILE2.ordinal()], decodedMaxScale - decodedMinScale);
			Point ptQ2 = gc.textExtent(q2Text);
			if ((ptQ2.x + ptQ1.x) / 2 + scaleXGap < xPosQ2 - xPosQ1 //
					&& (ptQ3.x + ptQ2.x) / 2 + scaleXGap < xPosQ3 - xPosQ2 && !scaleTexts.contains(q2Text)) {
				GraphicsUtils.drawTextCentered(q2Text, xPosQ2, scaleY0, gc, SWT.HORIZONTAL);
			}

			gc.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		}

		{
			gc.setLineWidth(1);
			gc.setForeground(record.getColor());

			int xPosLowerWhisker = drawStripBounds.x + tukeyXPositions[LOWER_WHISKER.ordinal()];
			int xPosUpperWhisker = drawStripBounds.x + tukeyXPositions[UPPER_WHISKER.ordinal()];
			gc.drawLine(xPosLowerWhisker, yPos, xPosQ1, yPos);
			gc.drawLine(xPosUpperWhisker, yPos, xPosQ3, yPos);

			int scaledHalfAntennaHeight = (scaledHalfBoxHeight + 2) / 2;
			gc.drawLine(xPosLowerWhisker, yPos - scaledHalfAntennaHeight, xPosLowerWhisker, yPos + scaledHalfAntennaHeight);
			gc.drawLine(xPosUpperWhisker, yPos - scaledHalfAntennaHeight, xPosUpperWhisker, yPos + scaledHalfAntennaHeight);
			if (isDrawNumbers) {
				gc.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE - 1, SWT.NORMAL));
				Color color = isDrawNumbersInRecordColor ? record.getColor() : DataExplorer.COLOR_BLACK;
				gc.setForeground(color);
				double[] tukeyBoxPlot = record.getQuantile().getTukeyBoxPlot();

				{
					Point ptQ1 = gc.textExtent("" + record.getFormattedRangeValue(tukeyBoxPlot[QUARTILE1.ordinal()], decodedMaxScale - decodedMinScale));
					String lowerText = "" + record.getFormattedRangeValue(tukeyBoxPlot[LOWER_WHISKER.ordinal()], decodedMaxScale - decodedMinScale);
					Point ptLower = gc.textExtent(lowerText);
					if ((ptLower.x + ptQ1.x) / 2 + scaleXGap < xPosQ1 - xPosLowerWhisker && !scaleTexts.contains(lowerText)) {
						GraphicsUtils.drawTextCentered(lowerText, xPosLowerWhisker, scaleY0, gc, SWT.HORIZONTAL);
					}
				}
				{
					Point ptQ3 = gc.textExtent("" + record.getFormattedRangeValue(tukeyBoxPlot[QUARTILE3.ordinal()], decodedMaxScale - decodedMinScale));
					String upperText = "" + record.getFormattedRangeValue(tukeyBoxPlot[UPPER_WHISKER.ordinal()], decodedMaxScale - decodedMinScale);
					Point ptUpper = gc.textExtent(upperText);
					if ((ptUpper.x + ptQ3.x) / 2 + scaleXGap < xPosUpperWhisker - xPosQ3 && !scaleTexts.contains(upperText)) {
						GraphicsUtils.drawTextCentered(upperText, xPosUpperWhisker, scaleY0, gc, SWT.HORIZONTAL);
					}
				}

				gc.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			}
		}

	}

	/**
	 * Draw the warning images.
	 * @param record
	 * @param gc
	 * @param drawStripBounds holds the drawing area strip based on the upper left position
	 * @param scaleWidthSpace is the width of the left / right scale in pixels
	 */
	public static void drawChannelItemWarnings(TrailRecord record, GC gc, Rectangle drawStripBounds, int scaleWidthSpace) {
		if (record.isEmpty() && !record.isDisplayable() && !record.isScaleVisible()) return; // nothing to display

		int x0 = drawStripBounds.x;
		int width = drawStripBounds.width;
		int xN = x0 + width + 1;
		int yPos = drawStripBounds.y + drawStripBounds.height / 2 - 1;

		OutlierWarning[] minMaxWarning = record.getSummarySpots().getMinMaxWarning();

		if (minMaxWarning[0] == OutlierWarning.FAR) {
			GraphicsUtils.drawImageCentered("gde/resource/caution_portrait.png", x0 - scaleWidthSpace * 9 / 10, yPos, gc);
		} else if (minMaxWarning[0] == OutlierWarning.CLOSE) {
			GraphicsUtils.drawImageCentered("gde/resource/caution_portrait_yellow.png", x0 - scaleWidthSpace * 9 / 10, yPos, gc);
		}
		if (minMaxWarning[1] == OutlierWarning.FAR) {
			GraphicsUtils.drawImageCentered("gde/resource/caution_portrait.png", xN + scaleWidthSpace * 9 / 10, yPos, gc);
		} else if (minMaxWarning[1] == OutlierWarning.CLOSE) {
			GraphicsUtils.drawImageCentered("gde/resource/caution_portrait_yellow.png", xN + scaleWidthSpace * 9 / 10, yPos, gc);
		}
	}

	/**
	 * Draw the summary scale elements and the value grid using given rectangle for display.
	 * @param record
	 * @param gc
	 * @param drawStripBounds holds the drawing area strip based on the upper left position
	 * @param scaleWidthSpace is the width of the left / right scale in pixels
	 * @param decodedMinScale is the value corresponding to the left border (xPos = 0)
	 * @param decodedMaxScale is the value corresponding to the right border (xPos = width)
	 * @param isDrawName
	 * @param isDrawNameInRecordColor
	 * @param isDrawNumbersInRecordColor
	 */
	public static void drawChannelItemScale(TrailRecord record, GC gc, Rectangle drawStripBounds, int scaleWidthSpace, double decodedMinScale,
			double decodedMaxScale, boolean isDrawScaleInRecordColor, boolean isDrawNameInRecordColor, boolean isDrawNumbersInRecordColor,
			boolean isDrawName) {
		if (record.isEmpty() && !record.isDisplayable() && !record.isScaleVisible()) return; // nothing to display

		int x0 = drawStripBounds.x;
		int width = drawStripBounds.width;

		// texts
		String graphText = DeviceXmlResource.getInstance().getReplacement(record.isScaleSyncMaster() ? record.getSyncMasterName() : record.getName());
		if (record.getSymbol() != null && record.getSymbol().length() > 0) graphText = graphText + "   " + record.getSymbol();
		if (record.getUnit() != null && record.getUnit().length() > 0) graphText = graphText + "   [" + record.getUnit() + "]"; //$NON-NLS-1$ //$NON-NLS-2$

		// prepare layout
		gc.setLineWidth(2);
		gc.setLineStyle(SWT.LINE_SOLID);
		int gap = -3;
		int xN = x0 + width + 1;
		int yPos = drawStripBounds.y + drawStripBounds.height / 2 - 1;

		{
			Color color = isDrawNumbersInRecordColor ? record.getColor() : DataExplorer.COLOR_BLACK;
			gc.setForeground(color);
			GraphicsUtils.drawTextCentered(record.getFormattedRangeValue(decodedMinScale, decodedMaxScale - decodedMinScale), x0 - gap - scaleWidthSpace / 2, yPos, gc, SWT.HORIZONTAL);
			GraphicsUtils.drawTextCentered(record.getFormattedRangeValue(decodedMaxScale, decodedMaxScale - decodedMinScale), xN + gap + scaleWidthSpace / 2, yPos, gc, SWT.HORIZONTAL);
		}
		if (isDrawName) {
			Color color = isDrawNameInRecordColor ? record.getColor() : DataExplorer.COLOR_BLACK;
			gc.setForeground(color);
			GraphicsUtils.drawTextCentered(graphText, x0 + width / 2, yPos, gc, SWT.HORIZONTAL);
		}

		boolean isCurveGridEnabled = record.getParent().getValueGridType() > 0;
		if (isCurveGridEnabled) {
			gc.setLineWidth(1);
			// gc.setLineDash(dashLineStyle);
			gc.setLineStyle(SWT.LINE_DOT);
			gc.setForeground(isDrawScaleInRecordColor ? record.getColor() : DataExplorer.COLOR_DARK_GREEN);
			for (int x : record.getSummarySpots().defineGrid(record.getParent(), false)) {
				gc.drawLine(x, drawStripBounds.y, x, drawStripBounds.y + drawStripBounds.height);
			}
		}
	}

	/**
	 * Draw the data graph scale using gives rectangle for display.
	 * @param record
	 * @param gc
	 * @param curveAreaBounds
	 * @param scaleWidthSpace
	 * @param isDrawScaleInRecordColor
	 * @param isDrawNameInRecordColor
	 * @param isDrawNumbersInRecordColor
	 * @param numberTickMarks the number of ticks {numberTicks, numberMiniTicks}
	 */
	public static void drawHistoScale(TrailRecord record, GC gc, Rectangle curveAreaBounds, int scaleWidthSpace, boolean isDrawScaleInRecordColor,
			boolean isDrawNameInRecordColor, boolean isDrawNumbersInRecordColor, int[] numberTickMarks) {
		int x0 = curveAreaBounds.x;
		int y0 = curveAreaBounds.y + curveAreaBounds.height;
		int width = curveAreaBounds.width;
		int height = curveAreaBounds.height;
		log.finer(() -> record.getName() + "  x0=" + x0 + " y0=" + y0 + " width=" + width + " height=" + height + " horizontalSpace=" + scaleWidthSpace); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		if (record.isEmpty() && !record.isDisplayable() && !record.isScaleVisible()) return; // nothing to display

		String graphText = DeviceXmlResource.getInstance().getReplacement(record.isScaleSyncMaster() ? record.getSyncMasterName() : record.getName());
		if (record.getSymbol() != null && record.getSymbol().length() > 0) graphText = graphText + "   " + record.getSymbol();
		if (record.getUnit() != null && record.getUnit().length() > 0) graphText = graphText + "   [" + record.getUnit() + "]"; //$NON-NLS-1$ //$NON-NLS-2$

		// adapt number space calculation to real displayed max number
		Point pt = gc.textExtent("000,00"); //$NON-NLS-1$
		int ticklength = 5;
		int gap = 10;

		// prepare axis position
		gc.setLineWidth(2);
		gc.setLineStyle(SWT.LINE_SOLID);
		boolean isPositionLeft = record.isPositionLeft();
		int positionNumber = record.getParent().getAxisPosition(record.getName(), isPositionLeft);
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
	 * @param curveAreaBounds
	 * @param timeLine
	 */
	public static void drawHistoCurve(TrailRecord record, GC gc, Rectangle curveAreaBounds, HistoTimeLine timeLine) {
		// set line properties according adjustment
		gc.setForeground(record.getColor());
		gc.setLineWidth(record.getLineWidth());
		gc.setLineStyle(record.getLineStyle());

		// record.setDisplayScaleFactorTime(1);// x-axis scaling not supported
		record.setDisplayScaleFactorValue(curveAreaBounds.height);

		StringBuffer sb = new StringBuffer(); // logging purpose

		// draw scaled points to draw area - measurements can only be drawn starting with the first measurement point
		Point[] points = HistoGraphicsMapper.getDisplayPoints(record, timeLine);

		Point newPoint, oldPoint = null;
		int displayableSize = record.size();
		log.fine(() -> "displayableSize = " + displayableSize); //$NON-NLS-1$
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
	 * @param curveAreaBounds
	 * @param timeLine
	 */
	public static void drawHistoSuite(TrailRecord record, GC gc, Rectangle curveAreaBounds, HistoTimeLine timeLine) {
		log.fine(() -> String.format("MinScaleValue=%f   MaxScaleValue=%f   MinDisplayValue=%f   MaxDisplayValue=%f", record.getMinScaleValue(), //$NON-NLS-1$
				record.getMaxScaleValue(), record.getMinDisplayValue(), record.getMaxDisplayValue()));

		// set line properties according adjustment
		gc.setForeground(record.getColor());
		gc.setLineWidth(record.getLineWidth());
		gc.setLineStyle(record.getLineStyle());

		// int xScaleFactor = 1; // x-axis scaling not supported
		// record.setDisplayScaleFactorTime(xScaleFactor);
		record.setDisplayScaleFactorValue(curveAreaBounds.height);
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
		List<PointArray> suitePoints = HistoGraphicsMapper.getSuiteDisplayPoints(record, timeLine);
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
		List<PointArray> suitePoints = HistoGraphicsMapper.getSuiteDisplayPoints(record, timeLine);

		List<Integer> durations_mm = record.getParent().getDurations_mm();
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
