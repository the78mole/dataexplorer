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

    Copyright (c) 2017 Thomas Eickert
****************************************************************************************/
package gde.ui.tab;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import gde.GDE;
import gde.config.Settings;
import gde.data.TrailRecord;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;
import gde.utils.HistoTimeLine;
import gde.utils.LocalizedDateTime;
import gde.utils.LocalizedDateTime.DateTimePattern;
import gde.utils.Quantile;
import gde.utils.SingleResponseRegression;
import gde.utils.SingleResponseRegression.RegressionType;

/**
 * Curve measuring for the graphics window.
 * Supports linear regression and boxplot for delta measurements.
 * @author Thomas Eickert
 */
public class CurveSurvey {
	private final static String	$CLASS_NAME	= CurveSurvey.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	public enum LineMark {
		MEASURE_CROSS(1, SWT.COLOR_BLACK, SWT.LINE_DASH, null), //
		DELTA_CROSS(1, SWT.COLOR_BLUE, SWT.LINE_DASH, null), //
		DIAG_LINE(1, SWT.COLOR_DARK_YELLOW, SWT.LINE_DOT, new int[] { 5, 2 }), //
		AVG_LINE(2, null, SWT.LINE_DASH, null), //
		PARABOLA_LINE(2, null, SWT.LINE_DOT, null), //
		SLOPE_LINE(2, null, SWT.LINE_SOLID, null), //
		BOXPLOT(2, null, SWT.LINE_SOLID, null);

		private final int			lineWidth;
		private final Integer	lineColor;
		private final int			lineStyle;
		private final int[]		lineDash;

		private LineMark(int lineWidth, Integer lineColor, int lineStyle, int[] lineDash) {
			this.lineWidth = lineWidth;
			this.lineColor = lineColor;
			this.lineStyle = lineStyle;
			this.lineDash = lineDash;
		}
	}

	private final Settings		settings						= Settings.getInstance();
	private HistoTimeLine			timeLine;
	private final TrailRecord	trailRecord;

	private GC								canvasGC;
	private Rectangle					curveAreaBounds;
	private int								offSetX, offSetY;															// curveAreaBounds x / y for performance

	private long							timestampMeasure_ms, timestampDelta_ms;
	private int								xPosMeasure					= 0, yPosMeasure = 0;
	private int								xPosDelta						= 0, yPosDelta = 0;

	private int								yLowerTransversePos	= Integer.MIN_VALUE;			// keeps bottom position of delta measurement diagonal lines and boxplot
	private int								yUpperTransversePos	= Integer.MAX_VALUE;			// keeps top position of delta measurement diagonal lines and boxplot

	public CurveSurvey(GC canvasGC) { // todo this might support the standard graphics window in the future
		this.canvasGC = canvasGC;

		this.trailRecord = null;
	}

	public CurveSurvey(GC canvasGC, TrailRecord trailRecord, HistoTimeLine timeLine) {
		this.canvasGC = canvasGC;
		this.trailRecord = trailRecord;
		this.timeLine = timeLine;
	}

	/**
	 * @return the message text for the status message
	 */
	public String drawMeasurementGraphics() {
		String statusMessage;
		drawVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height, LineMark.MEASURE_CROSS);
		if (isYInCurveAreaBounds(this.yPosMeasure)) drawHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width, LineMark.MEASURE_CROSS);

		if (this.yPosMeasure >= Integer.MIN_VALUE) {
			//			String formattedTimeWithUnit = LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmmss, this.timeLine.getAdjacentTimestamp(this.xPosMeasure));
			statusMessage = Messages.getString(MessageIds.GDE_MSGT0256,
					new Object[] { this.trailRecord.getName(), this.trailRecord.getFormattedMeasureValue(this.trailRecord.getParentTrail().getIndex(this.timestampMeasure_ms)), this.trailRecord.getUnit(),
							LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmmss, this.timestampMeasure_ms) });
		}
		else {
			statusMessage = Messages.getString(MessageIds.GDE_MSGT0848, new Object[] { this.trailRecord.getName(), GDE.STRING_STAR, this.trailRecord.getUnit(), GDE.STRING_STAR });
		}
		return statusMessage;
	}

	/**
	 * @return the message text for the status message
	 */
	public String drawDeltaMeasurementGraphics() {
		String statusMessage;

		drawVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height, LineMark.MEASURE_CROSS);
		if (isYInCurveAreaBounds(this.yPosMeasure)) drawHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width, LineMark.MEASURE_CROSS);

		drawVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height, LineMark.DELTA_CROSS);
		if (isYInCurveAreaBounds(this.yPosDelta)) drawHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width, LineMark.DELTA_CROSS);

		if (this.yPosMeasure != Integer.MIN_VALUE || this.yPosDelta != Integer.MIN_VALUE) {
			if (this.yPosMeasure != Integer.MIN_VALUE && this.yPosDelta != Integer.MIN_VALUE) {
				drawConnectingLine(this.xPosMeasure, this.yPosMeasure, this.xPosDelta, this.yPosDelta, LineMark.DIAG_LINE);
			}

			this.trailRecord.setBounds(this.timestampMeasure_ms, this.timestampDelta_ms);
			if (this.settings.isCurveSurvey()) {
				drawCurveSurvey();
				String deltaText = this.trailRecord.getFormattedBoundsDelta();
				String unitText = this.trailRecord.getUnit();
				String avgText = this.trailRecord.getFormattedBoundsAvg();
				String slopeText = this.trailRecord.getFormattedBoundsSlope();
				statusMessage = Messages.getString(MessageIds.GDE_MSGT0848,
						new Object[] { this.trailRecord.getName(), unitText, deltaText, LocalizedDateTime.getFormatedDistance(this.timestampMeasure_ms, this.timestampDelta_ms) })
						+ Messages.getString(MessageIds.GDE_MSGT0879, new Object[] { unitText, avgText, unitText, slopeText });
			}
			else {
				String deltaText = this.trailRecord.getFormattedBoundsDelta();
				String unitText = this.trailRecord.getUnit();
				statusMessage = Messages.getString(MessageIds.GDE_MSGT0848,
						new Object[] { this.trailRecord.getName(), unitText, deltaText, LocalizedDateTime.getFormatedDistance(this.timestampMeasure_ms, this.timestampDelta_ms) });
			}
		}
		else {
			this.trailRecord.setBounds(this.timestampMeasure_ms, this.timestampDelta_ms);
			if (this.trailRecord.isValidBounds() && this.settings.isCurveSurvey()) {
				drawCurveSurvey();
				String unitText = this.trailRecord.getUnit();
				String avgText = this.trailRecord.getFormattedBoundsAvg();
				String slopeText = this.trailRecord.getFormattedBoundsSlope();
				statusMessage = Messages.getString(MessageIds.GDE_MSGT0848,
						new Object[] { this.trailRecord.getName(), GDE.STRING_STAR, this.trailRecord.getUnit(), LocalizedDateTime.getFormatedDistance(this.timestampMeasure_ms, this.timestampDelta_ms) })
						+ Messages.getString(MessageIds.GDE_MSGT0879, new Object[] { unitText, avgText, unitText, slopeText });
			}
			else {
				statusMessage = Messages.getString(MessageIds.GDE_MSGT0848, new Object[] { this.trailRecord.getName(), GDE.STRING_STAR });
			}
		}
		return statusMessage;
	}

	/**
	 * Draw horizontal line as defined relative to curve draw area, where there is an offset from left and an offset from top
	 * for performance reason specify line width, line style and line color outside
	 * @param posFromLeft
	 * @param posFromTop
	 * @param length
	 */
	public void drawVerticalLine(int posFromLeft, int posFromTop, int length, LineMark lineMark) {
		setLineMark(lineMark);
		this.canvasGC.drawLine(posFromLeft + this.offSetX, posFromTop + this.offSetY, posFromLeft + this.offSetX, posFromTop + this.offSetY + length - 1);
	}

	/**
	 * Draw vertical line as defined relative to curve draw area, where there is an offset from left and an offset from top
	 * for performance reason specify line width, line style and line color outside
	 * @param posFromTop
	 * @param posFromLeft
	 * @param length
	 */
	public void drawHorizontalLine(int posFromTop, int posFromLeft, int length, LineMark lineMark) {
		setLineMark(lineMark);
		this.canvasGC.drawLine(posFromLeft + this.offSetX, posFromTop + this.offSetY, posFromLeft + this.offSetX + length - 1, posFromTop + this.offSetY);
	}

	/**
	 * Draw line as defined relative to curve draw area, where there is an offset from left and an offset from top.
	 * Expands the transverse lines drawing area if this line goes beyond the current area.
	 * @param posFromTop1
	 * @param posFromLeft1
	 * @param posFromTop2
	 * @param posFromLeft2
	 */
	public void drawConnectingLine(int posFromLeft1, int posFromTop1, int posFromLeft2, int posFromTop2, LineMark lineMark) {
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "posFromLeft1=" + posFromLeft1 + " posFromTop1=" + posFromTop1 + " posFromLeft2=" + posFromLeft2 + " posFromTop2=" + posFromTop2); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		this.yLowerTransversePos = Math.max(this.yLowerTransversePos, Math.max(posFromTop1, posFromTop2));
		this.yUpperTransversePos = Math.min(this.yUpperTransversePos, Math.min(posFromTop1, posFromTop2));
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "yUpperTransversePos=" + this.yUpperTransversePos + " yLowerTransversePos=" + this.yLowerTransversePos); //$NON-NLS-1$ //$NON-NLS-2$

		if (posFromLeft1 != posFromLeft2 || posFromTop1 != posFromTop2) {
			setLineMark(lineMark);
			// support lines with start or end beyond the y axis drawing area
			this.canvasGC.setClipping(this.curveAreaBounds.x, this.curveAreaBounds.y, this.curveAreaBounds.width, this.curveAreaBounds.height);
			this.canvasGC.drawLine(posFromLeft1 + this.offSetX, posFromTop1 + this.offSetY, posFromLeft2 + this.offSetX, posFromTop2 + this.offSetY);
			this.canvasGC.setClipping(this.curveAreaBounds);
		}
	}

	/**
	 * Draw regression lines and the boxplot element.
	 * @param trailRecord
	 */
	private void drawCurveSurvey() {
		if (!this.trailRecord.isBoundedParabola()) { // hide these curves for better overview whenever a parabola is shown
			int yBoundedAvg = this.trailRecord.getVerticalDisplayPos(this.trailRecord.getBoundedAvgValue());
			drawHorizontalLine(yBoundedAvg, this.xPosMeasure, this.xPosDelta - this.xPosMeasure, LineMark.AVG_LINE);
			drawConnectingLine(this.xPosMeasure, yBoundedAvg, this.xPosDelta, yBoundedAvg, LineMark.AVG_LINE);

			int yRegressionPosMeasure = this.trailRecord.getVerticalDisplayPos(this.trailRecord.getBoundedSlopeValue(this.timestampMeasure_ms));
			int yRegressionPosDelta = this.trailRecord.getVerticalDisplayPos(this.trailRecord.getBoundedSlopeValue(this.timestampDelta_ms));
			drawConnectingLine(this.xPosMeasure, yRegressionPosMeasure, this.xPosDelta, yRegressionPosDelta, LineMark.SLOPE_LINE);
		}
		else {
			NavigableMap<Long, Integer> boundedXpos = this.xPosMeasure < this.xPosDelta ? this.timeLine.getScalePositions().subMap(this.timestampMeasure_ms, true, this.timestampDelta_ms, true)
					: this.timeLine.getScalePositions().subMap(this.timestampDelta_ms, true, this.timestampMeasure_ms, true);

			List<Point> points = new ArrayList<>(boundedXpos.size());
			for (Entry<Long, Integer> entry : boundedXpos.entrySet()) {
				points.add(new Point(entry.getValue(), this.trailRecord.getVerticalDisplayPos(this.trailRecord.getBoundedParabolaValue(entry.getKey()))));
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "values " + Arrays.toString(points.toArray(new Point2D.Double[0])));

			drawRegressionParabolaLine(points, LineMark.PARABOLA_LINE);
		}
		{
			int xPosMidBounds = (this.xPosDelta + this.xPosMeasure) / 2;
			int halfBoxWidth = getBoxWidth() / 2;
			double[] values = this.trailRecord.getBoundedBoxplotValues();

			int yPosQuartile1 = this.trailRecord.getVerticalDisplayPos(values[Quantile.BoxplotItems.QUARTILE1.ordinal()]);
			drawHorizontalLine(yPosQuartile1, xPosMidBounds - halfBoxWidth, halfBoxWidth * 2, LineMark.BOXPLOT);
			drawHorizontalLine(this.trailRecord.getVerticalDisplayPos(values[Quantile.BoxplotItems.QUARTILE2.ordinal()]), xPosMidBounds - halfBoxWidth, halfBoxWidth * 2, LineMark.BOXPLOT);
			int yPosQuartile3 = this.trailRecord.getVerticalDisplayPos(values[Quantile.BoxplotItems.QUARTILE3.ordinal()]);
			drawHorizontalLine(yPosQuartile3, xPosMidBounds - halfBoxWidth, halfBoxWidth * 2, LineMark.BOXPLOT);
			drawVerticalLine(xPosMidBounds - halfBoxWidth, yPosQuartile3, yPosQuartile1 - yPosQuartile3, LineMark.BOXPLOT);
			drawVerticalLine(xPosMidBounds + halfBoxWidth, yPosQuartile3, yPosQuartile1 - yPosQuartile3, LineMark.BOXPLOT);

			// Connecting lines define the bounds rectangle transverse area
			int yPosLowerWhisker = this.trailRecord.getVerticalDisplayPos(values[Quantile.BoxplotItems.LOWER_WHISKER.ordinal()]);
			drawConnectingLine(xPosMidBounds - halfBoxWidth / 2, yPosLowerWhisker, xPosMidBounds + halfBoxWidth / 2, yPosLowerWhisker, LineMark.BOXPLOT);
			// drawHorizontalLine(yPosLowerWhisker, xPosMidBounds - halfBoxWidth / 2, halfBoxWidth, LineMark.BOXPLOT);
			drawVerticalLine(xPosMidBounds, yPosQuartile1, yPosLowerWhisker - yPosQuartile1, LineMark.BOXPLOT);

			int yPosUpperWhisker = this.trailRecord.getVerticalDisplayPos(values[Quantile.BoxplotItems.UPPER_WHISKER.ordinal()]);
			drawConnectingLine(xPosMidBounds - halfBoxWidth / 2, yPosUpperWhisker, xPosMidBounds + halfBoxWidth / 2, yPosUpperWhisker, LineMark.BOXPLOT);
			// drawHorizontalLine(yPosUpperWhisker, xPosMidBounds - halfBoxWidth / 2, halfBoxWidth, LineMark.BOXPLOT);
			drawVerticalLine(xPosMidBounds, yPosUpperWhisker, yPosQuartile3 - yPosUpperWhisker, LineMark.BOXPLOT);

			if (values[Quantile.BoxplotItems.QUARTILE0.ordinal()] != values[Quantile.BoxplotItems.LOWER_WHISKER.ordinal()])
				drawOutlier(xPosMidBounds, this.trailRecord.getVerticalDisplayPos(values[Quantile.BoxplotItems.QUARTILE0.ordinal()]), halfBoxWidth / 4, LineMark.BOXPLOT);

			if (values[Quantile.BoxplotItems.QUARTILE4.ordinal()] != values[Quantile.BoxplotItems.UPPER_WHISKER.ordinal()])
				drawOutlier(xPosMidBounds, this.trailRecord.getVerticalDisplayPos(values[Quantile.BoxplotItems.QUARTILE4.ordinal()]), halfBoxWidth / 4, LineMark.BOXPLOT);

			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, String.format("LW=%d Q1=%d Q2=%d Q3=%d UW=%d ", yPosLowerWhisker, yPosQuartile1, //$NON-NLS-1$
					this.trailRecord.getVerticalDisplayPos(values[Quantile.BoxplotItems.QUARTILE2.ordinal()]), yPosQuartile3, yPosUpperWhisker));
		}
	}

	/**
	 * Draw a parabola based on bounded trailrecord values as defined relative to curve draw area, where there is an offset from left and an offset from top.
	 * @param points is a list of the display points approximated by the parabola (one point per log timestamp)
	 * @param lineMark
	 */
	private void drawRegressionParabolaLine(List<Point> points, LineMark lineMark) {
		// set the erase area max/min values
		double[] boundedParabolaCoefficients = this.trailRecord.getBoundedParabolaCoefficients();
		double extremumTimeStamp_ms = boundedParabolaCoefficients[1] / boundedParabolaCoefficients[2] / -2.;
		double mid = (this.timestampMeasure_ms + this.timestampDelta_ms) / 2.;
		if (Math.abs(extremumTimeStamp_ms - mid) <= (Math.abs(this.timestampMeasure_ms - mid))) { // extremum is between
			int yPosExtremum = this.trailRecord.getVerticalDisplayPos(this.trailRecord.getBoundedParabolaValue((long) extremumTimeStamp_ms));
			this.yLowerTransversePos = Math.max(this.yLowerTransversePos, yPosExtremum);
			this.yUpperTransversePos = Math.min(this.yUpperTransversePos, yPosExtremum);
		}
		int yPos1 = this.trailRecord.getVerticalDisplayPos(this.trailRecord.getBoundedParabolaValue(this.timestampMeasure_ms));
		int yPos2 = this.trailRecord.getVerticalDisplayPos(this.trailRecord.getBoundedParabolaValue(this.timestampDelta_ms));
		this.yLowerTransversePos = Math.max(this.yLowerTransversePos, Math.max(yPos1, yPos2));
		this.yUpperTransversePos = Math.min(this.yUpperTransversePos, Math.min(yPos1, yPos2));
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "yUpperTransversePos=" + this.yUpperTransversePos + " yLowerTransversePos=" + this.yLowerTransversePos); //$NON-NLS-1$ //$NON-NLS-2$

		// determine the display points for all x axis pixels within the bounded survey range
		SingleResponseRegression singleResponseRegression = new SingleResponseRegression(points, RegressionType.QUADRATIC);

		int xPosStart = Math.min(this.xPosDelta, this.xPosMeasure);
		int[] pointArray = new int[Math.abs(this.xPosDelta - this.xPosMeasure) * 2];
		for (int i = 0; i < Math.abs(this.xPosDelta - this.xPosMeasure); i++) {
			pointArray[i * 2] = this.offSetX + xPosStart + i;
			pointArray[i * 2 + 1] = this.offSetY + (int) (singleResponseRegression.getResponse(xPosStart + i) + .5);
		}

		setLineMark(lineMark);
		// support lines with start or end beyond the y axis drawing area
		this.canvasGC.setClipping(this.curveAreaBounds.x, this.curveAreaBounds.y, this.curveAreaBounds.width, this.curveAreaBounds.height);
		this.canvasGC.drawPolyline(pointArray);
		this.canvasGC.setClipping(this.curveAreaBounds);
	}

	/**
	 * Draw outlier circle as defined relative to curve draw area, where there is an offset from left and an offset from top.
	 * Expand the transverse lines drawing area if this line goes beyond the current area.
	 * @param posFromTop1
	 * @param posFromLeft1
	 * @param radius
	 * @param lineMark
	 */
	private void drawOutlier(int posFromLeft1, int posFromTop1, int radius, LineMark lineMark) {
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "posFromLeft1=" + posFromLeft1 + " posFromTop1=" + posFromTop1); //$NON-NLS-1$ //$NON-NLS-2$
		this.yLowerTransversePos = Math.max(this.yLowerTransversePos, posFromTop1 + radius);
		this.yUpperTransversePos = Math.min(this.yUpperTransversePos, posFromTop1 - radius);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "yUpperTransversePos=" + this.yUpperTransversePos + " yLowerTransversePos=" + this.yLowerTransversePos); //$NON-NLS-1$ //$NON-NLS-2$

		setLineMark(lineMark);

		this.canvasGC.drawOval(posFromLeft1 - radius + this.offSetX, posFromTop1 - radius + this.offSetY, radius * 2, radius * 2);
	}

	/**
	 * Set the line properties for the next line draw operations.
	 * @param lineMark
	 * @param defaultColor in case of a null color in the lineMark
	 */
	private void setLineMark(LineMark lineMark) {
		this.canvasGC.setLineWidth(lineMark.lineWidth);
		this.canvasGC.setLineDash(lineMark.lineDash);
		this.canvasGC.setLineStyle(lineMark.lineStyle);
		if (lineMark.lineColor == null)
			this.canvasGC.setForeground(this.trailRecord.getColor());
		else
			this.canvasGC.setForeground(SWTResourceManager.getColor(lineMark.lineColor));
	}

	private int getBoxWidth() {
		int boxWidth = 15;
		if (Math.abs(this.xPosDelta - this.xPosMeasure) > boxWidth) {
			boxWidth += ((int) Math.log(Math.abs(this.xPosDelta - this.xPosMeasure))) * 4 - 10;
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, String.format("boxwidth=%d ", boxWidth)); //$NON-NLS-1$
		}
		return boxWidth + boxWidth * (this.settings.getBoxplotScaleOrdinal() - 1) / 2;
	}

	/**
	 * @param canvasImage
	 */
	public void clearOldMeasureLines(Image canvasImage) {
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "yPosMeasure = " + this.yPosMeasure + " yPosDelta = " + this.yPosDelta); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.xPosMeasure > 0 || this.xPosDelta > 0) {
			eraseVerticalLine(canvasImage, this.xPosMeasure, 0, this.curveAreaBounds.height, 1);
			// no change don't needs to be calculated, but the calculation limits to bounds
			this.yPosMeasure = this.trailRecord.getVerticalDisplayPos(this.trailRecord.getParentTrail().getIndex(this.timeLine.getAdjacentTimestamp(this.xPosMeasure)));
			if (isYInCurveAreaBounds(this.yPosMeasure)) eraseHorizontalLine(canvasImage, this.yPosMeasure, 0, this.curveAreaBounds.width, 1);

			if (this.trailRecord.getParentTrail().isDeltaMeasurementMode(this.trailRecord.getName())) {
				eraseVerticalLine(canvasImage, this.xPosDelta, 0, this.curveAreaBounds.height, 1);
				// no change don't needs to be calculated, but the calculation limits to bounds
				this.yPosDelta = this.trailRecord.getVerticalDisplayPos(this.trailRecord.getParentTrail().getIndex(this.timeLine.getAdjacentTimestamp(this.xPosDelta)));
				if (isYInCurveAreaBounds(this.yPosDelta)) eraseHorizontalLine(canvasImage, this.yPosDelta, 0, this.curveAreaBounds.width, 1);

				cleanSurveyObsoleteRectangle(canvasImage);
			}
		}
	}

	/**
	 * erase a vertical line by re-drawing the curve area image
	 * @param posFromLeft
	 * @param posFromTop
	 * @param length
	 * @param lineWidth
	 */
	public void eraseVerticalLine(Image canvasImage, int posFromLeft, int posFromTop, int length, int lineWidth) {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, String.format("imageDisposed=%s posFromLeft=%d posFromTop=%d lineWidth=%d length=%d", canvasImage.isDisposed(), posFromLeft, posFromTop, this.yPosDelta, lineWidth, length)); //$NON-NLS-1$
		this.canvasGC.drawImage(canvasImage, posFromLeft + this.offSetX, posFromTop + this.offSetY, lineWidth, length, posFromLeft + this.offSetX, posFromTop + this.offSetY, lineWidth, length);
	}

	/**
	 * erase a horizontal line by re-drawing the curve area image
	 * @param posFromTop
	 * @param posFromLeft
	 * @param length
	 * @param lineWidth
	 */
	public void eraseHorizontalLine(Image canvasImage, int posFromTop, int posFromLeft, int length, int lineWidth) {
		// do not erase lines beyond the y axis drawing area
		if (posFromTop >= 0 && posFromTop <= this.curveAreaBounds.height)
			this.canvasGC.drawImage(canvasImage, posFromLeft + this.offSetX, posFromTop + this.offSetY, length, lineWidth, posFromLeft + this.offSetX, posFromTop + this.offSetY, length, lineWidth);
	}

	/**
	 * Clean transverse area lines by re-drawing the untouched curve area image of this rectangle.
	 */
	public void cleanSurveyObsoleteRectangle(Image canvasImage) { // todo in case of performance issues reduce the rectangle height (without whiskers and outliers) and add 2 upright rectangles for whiskers and outliers
		if (this.yLowerTransversePos != Integer.MIN_VALUE || this.yUpperTransversePos != Integer.MAX_VALUE) {
			int maxBoxplotSize = this.settings.isCurveSurvey() ? getBoxWidth() : 0;
			int maxLineWidth = 3;

			// extend the rectangle to avoid remains of thicker lines or boxplot parts which lie beyond the rectangle
			int left = Math.min(this.xPosMeasure, this.xPosDelta) - maxBoxplotSize / 2 - maxLineWidth;
			left = left > 0 ? left : 0;
			int width = Math.max(this.xPosMeasure, this.xPosDelta) - left + maxBoxplotSize + maxLineWidth * 2;
			width = Math.min(width, this.curveAreaBounds.width - this.offSetX - left);
			int top = this.yUpperTransversePos - maxLineWidth;
			top = top > 0 ? top : 0;
			int height = this.yLowerTransversePos - this.yUpperTransversePos + maxLineWidth * 2;
			height = Math.min(this.curveAreaBounds.height - this.offSetY - top, height);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "left=" + left + " top=" + top + " width=" + width + " height=" + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

			this.canvasGC.drawImage(canvasImage, left + this.offSetX, top + this.offSetY, width + this.offSetX, height + this.offSetY, left + this.offSetX, top + this.offSetY, width + this.offSetX,
					height + this.offSetY);

			this.yLowerTransversePos = Integer.MIN_VALUE;
			this.yUpperTransversePos = Integer.MAX_VALUE;
		}
	}

	/**
	 * clean (old) measurement pointer - check pointer in curve area
	 */
	public void cleanMeasurementPointer(Image canvasImage) {
		if (this.xPosMeasure > 0) {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "xPosMeasure=" + this.xPosMeasure + " xPosTimestamp=" + this.timeLine.getXPosTimestamp(this.timestampMeasure_ms)); //$NON-NLS-1$ //$NON-NLS-2$
			eraseVerticalLine(canvasImage, this.xPosMeasure, 0, this.curveAreaBounds.height, 1);
			eraseHorizontalLine(canvasImage, this.yPosMeasure, 0, this.curveAreaBounds.width, 1);
		}
		if (this.xPosDelta > 0) {
			eraseVerticalLine(canvasImage, this.xPosDelta, 0, this.curveAreaBounds.height, 1);
			eraseHorizontalLine(canvasImage, this.yPosDelta, 0, this.curveAreaBounds.width, 1);
			cleanSurveyObsoleteRectangle(canvasImage);
		}
	}

	private boolean isYInCurveAreaBounds(int yPos) {
		return yPos <= this.curveAreaBounds.height && yPos >= 0;
	}

	/**
	 * Set the measure canvas positions.
	 * @param curveAreaBounds
	 * @param timeStampMeasure_ms is a valid time stamp
	 */
	public void setPosMeasure(Rectangle curveAreaBounds, long timeStampMeasure_ms) {
		boolean isNullAcceptable = this.settings.isCurveSurvey(); // allow positioning on timestamps with null values
		this.curveAreaBounds = curveAreaBounds;
		this.offSetX = curveAreaBounds.x;
		this.offSetY = curveAreaBounds.y;

		int yPosMeasureNew = this.trailRecord.getVerticalDisplayPos(this.trailRecord.getParentTrail().getIndex(timeStampMeasure_ms));
		if (isNullAcceptable || yPosMeasureNew != Integer.MIN_VALUE) {
			this.timestampMeasure_ms = timeStampMeasure_ms;
			this.xPosMeasure = this.timeLine.getXPosTimestamp(timeStampMeasure_ms);
			this.yPosMeasure = yPosMeasureNew;
		}
		else {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("timestampMeasure_ms=%d search first non-null valuefrom the left", timeStampMeasure_ms)); //$NON-NLS-1$
			long timestampMeasureNew_ms = timeStampMeasure_ms;
			for (int i = 0; i < this.trailRecord.size(); i++) {
				timestampMeasureNew_ms = this.trailRecord.getParentTrail().getDisplayTimeStamp_ms(i);
				yPosMeasureNew = this.trailRecord.getVerticalDisplayPos(this.trailRecord.getParentTrail().getIndex(timestampMeasureNew_ms));
				if (yPosMeasureNew > Integer.MIN_VALUE) break;
			}
			this.timestampMeasure_ms = timestampMeasureNew_ms;
			this.xPosMeasure = this.timeLine.getXPosTimestamp(timestampMeasureNew_ms);
			this.yPosMeasure = yPosMeasureNew;
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("timestampMeasure_ms=%d xPosMeasure=%d yPosMeasure=%d", this.timestampMeasure_ms, this.xPosMeasure, this.yPosMeasure)); //$NON-NLS-1$
	}

	/**
	 * Set the delta canvas positions.
	 * @param curveAreaBounds
	 * @param timeStampDelta_ms is a valid time stamp
	 */
	public void setPosDelta(Rectangle curveAreaBounds, long timeStampDelta_ms) {
		boolean isNullAcceptable = this.settings.isCurveSurvey(); // allow positioning on timestamps with null values
		this.curveAreaBounds = curveAreaBounds;
		this.offSetX = curveAreaBounds.x;
		this.offSetY = curveAreaBounds.y;

		int yPosDeltaNew = this.trailRecord.getVerticalDisplayPos(this.trailRecord.getParentTrail().getIndex(timeStampDelta_ms));
		if (isNullAcceptable || yPosDeltaNew != Integer.MIN_VALUE) {
			this.timestampDelta_ms = timeStampDelta_ms;
			this.xPosDelta = this.timeLine.getXPosTimestamp(timeStampDelta_ms);
			this.yPosDelta = yPosDeltaNew;
		}
		else {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("timestampDelta_ms=%d search first non-null value from the right", timeStampDelta_ms)); //$NON-NLS-1$
			long timestampDeltaNew_ms = timeStampDelta_ms;
			for (int i = this.trailRecord.size() - 1; i >= 0; i--) {
				timestampDeltaNew_ms = this.trailRecord.getParentTrail().getDisplayTimeStamp_ms(i);
				yPosDeltaNew = this.trailRecord.getVerticalDisplayPos(this.trailRecord.getParentTrail().getIndex(timestampDeltaNew_ms));
				if (yPosDeltaNew > Integer.MIN_VALUE) break;
			}
			this.timestampDelta_ms = timestampDeltaNew_ms;
			this.xPosDelta = this.timeLine.getXPosTimestamp(timestampDeltaNew_ms);
			this.yPosDelta = yPosDeltaNew;
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("timestampDelta_ms=%d xPosDelta=%d yPosDelta=%d", this.timestampDelta_ms, this.xPosDelta, this.yPosDelta)); //$NON-NLS-1$
	}

	public GC getCanvasGC() {
		return this.canvasGC;
	}

	public void setCanvasGC(GC canvasGC) {
		this.canvasGC = canvasGC;
	}

	public long getTimestampMeasure_ms() {
		return this.timestampMeasure_ms;
	}

	public long getTimestampDelta_ms() {
		return this.timestampDelta_ms;
	}

	public int getxPosMeasure() {
		return this.xPosMeasure;
	}

	public int getyPosMeasure() {
		return this.yPosMeasure;
	}

	public int getxPosDelta() {
		return this.xPosDelta;
	}

	public int getyPosDelta() {
		return this.yPosDelta;
	}

}
