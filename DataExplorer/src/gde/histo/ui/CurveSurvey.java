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
package gde.histo.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import gde.GDE;
import gde.config.Settings;
import gde.histo.recordings.HistoGraphicsMapper;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordCutter;
import gde.histo.recordings.TrailRecordFormatter;
import gde.histo.utils.HistoTimeLine;
import gde.histo.utils.SingleResponseRegression;
import gde.histo.utils.SingleResponseRegression.RegressionType;
import gde.histo.utils.Spot;
import gde.histo.utils.UniversalQuantile;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;
import gde.utils.LocalizedDateTime;
import gde.utils.LocalizedDateTime.DateTimePattern;

/**
 * Curve measuring for the graphics window.
 * Supports linear regression and boxplot for delta measurements.
 * @author Thomas Eickert
 */
public final class CurveSurvey {
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

	/**
	 * Painting the survey area lines and boxplots.
	 * Includes erasing the existing survey objects.
	 * @author Thomas Eickert (USER)
	 */
	private final class LinePainter {

		private final int	offSetX, offSetY;								// curveAreaBounds x / y for performance
		private final int	width, height;									// for performance

		// survey area axis position borders
		private int				yLowerLimit	= Integer.MIN_VALUE;
		private int				yUpperLimit	= Integer.MAX_VALUE;
		private int				xLeftLimit	= Integer.MAX_VALUE;
		private int				xRightLimit	= Integer.MIN_VALUE;

		public LinePainter(Rectangle curveAreaBounds) {
			super();
			this.offSetX = curveAreaBounds.x;
			this.offSetY = curveAreaBounds.y;
			this.width = curveAreaBounds.width;
			this.height = curveAreaBounds.height;

			// support lines with start or end beyond the y axis drawing area
			CurveSurvey.this.canvasGC.setClipping(curveAreaBounds);
		}

		/**
		 * Draw horizontal line as defined relative to curve draw area, where there is an offset from left and an offset from top
		 * for performance reason specify line width, line style and line color outside
		 * @param posFromLeft
		 * @param posFromTop
		 * @param length
		 */
		private void drawVerticalLine(int posFromLeft, int posFromTop, int length, LineMark lineMark) {
			setLineMark(lineMark);
			CurveSurvey.this.canvasGC.drawLine(posFromLeft + this.offSetX, posFromTop + this.offSetY, posFromLeft + this.offSetX, posFromTop + this.offSetY + length - 1);
		}

		/**
		 * Draw vertical line as defined relative to curve draw area, where there is an offset from left and an offset from top
		 * for performance reason specify line width, line style and line color outside
		 * @param posFromTop
		 * @param posFromLeft
		 * @param length
		 */
		private void drawHorizontalLine(int posFromTop, int posFromLeft, int length, LineMark lineMark) {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("posFromLeft=%d posFromTop=%d length=%d", posFromLeft, posFromTop, length)); //$NON-NLS-1$
			setLineMark(lineMark);
			CurveSurvey.this.canvasGC.drawLine(posFromLeft + this.offSetX, posFromTop + this.offSetY, posFromLeft + this.offSetX + length - 1, posFromTop + this.offSetY);
		}

		/**
		 * Draw line as defined relative to curve draw area, where there is an offset from left and an offset from top.
		 * Expands the lines drawing area if this line goes beyond the current area.
		 * @param posFromTop1
		 * @param posFromLeft1
		 * @param posFromTop2
		 * @param posFromLeft2
		 */
		private void drawConnectingLine(int posFromLeft1, int posFromTop1, int posFromLeft2, int posFromTop2, LineMark lineMark) {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "posFromLeft1=" + posFromLeft1 + " posFromTop1=" + posFromTop1 + " posFromLeft2=" + posFromLeft2 + " posFromTop2=" + posFromTop2); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			if (posFromLeft1 != posFromLeft2 || posFromTop1 != posFromTop2) {
				setLineMark(lineMark);
				CurveSurvey.this.canvasGC.drawLine(posFromLeft1 + this.offSetX, posFromTop1 + this.offSetY, posFromLeft2 + this.offSetX, posFromTop2 + this.offSetY);
			}
		}

		/**
		 * Draw outlier circle as defined relative to curve draw area, where there is an offset from left and an offset from top.
		 * Expand the lines drawing area if this line goes beyond the current area.
		 * @param posFromTop1
		 * @param posFromLeft1
		 * @param radius
		 * @param lineMark
		 */
		private void drawOutlier(int posFromLeft1, int posFromTop1, int radius, LineMark lineMark) {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "posFromLeft1=" + posFromLeft1 + " posFromTop1=" + posFromTop1); //$NON-NLS-1$ //$NON-NLS-2$
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "yUpperLimit=" + this.yUpperLimit + " yLowerLimit=" + this.yLowerLimit); //$NON-NLS-1$ //$NON-NLS-2$

			setLineMark(lineMark);

			CurveSurvey.this.canvasGC.drawOval(posFromLeft1 - radius + this.offSetX, posFromTop1 - radius + this.offSetY, radius * 2, radius * 2);
		}

		public void drawAverageLine(int posFromLeft1, int posFromTop1, int length) {
			this.drawHorizontalLine(posFromLeft1, posFromTop1, length, LineMark.AVG_LINE);
		}

		public void drawLinearRegressionLine(int posFromLeft1, int posFromTop1, int posFromLeft2, int posFromTop2) {
			this.drawConnectingLine(posFromLeft1, posFromTop1, posFromLeft2, posFromTop2, LineMark.SLOPE_LINE);

			this.yLowerLimit = Math.max(this.yLowerLimit, Math.max(posFromTop1, posFromTop2));
			this.yUpperLimit = Math.min(this.yUpperLimit, Math.min(posFromTop1, posFromTop2));
			this.xLeftLimit = Math.min(this.xLeftLimit, Math.min(posFromLeft1, posFromLeft2));
			this.xRightLimit = Math.max(this.xRightLimit, Math.max(posFromLeft1, posFromLeft2));
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "xLeftLimit=" + this.xLeftLimit + " xRightLimit=" + this.xRightLimit + "yUpperLimit=" + this.yUpperLimit //$NON-NLS-1$//$NON-NLS-2$
					+ " yLowerLimit=" + this.yLowerLimit);
		}

		/**
		 * Draw a parabola based on bounded trailrecord values as defined relative to curve draw area.
		 * Note the offsets from left and from top.
		 * @param points is a list of the display points approximated by the parabola (one point per log timestamp)
		 */
		public void drawRegressionParabolaLine(List<Spot<Integer>> points) {
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, "xPos0=" + points.get(0).x() + " xPosN=" + points.get(points.size() - 1).x() + " yPos0=" + points.get(0).y() + " yPosN=" + points.get(points.size() - 1).y()); //$NON-NLS-1$ //$NON-NLS-2$

			// determine the display points for all x axis pixels within the bounded survey range
			SingleResponseRegression<Integer> singleResponseRegression = new SingleResponseRegression<>(points, RegressionType.QUADRATIC);

			int xPosStart = Math.min(points.get(0).x(), points.get(points.size() - 1).x());
			int xPosEnd = Math.max(points.get(0).x(), points.get(points.size() - 1).x());
			int[] pointArray;
			if (singleResponseRegression.getGamma() == 0.) {
				int yPosStart = (int) (singleResponseRegression.getResponse(xPosStart) + .5);
				int yPosEnd = (int) (singleResponseRegression.getResponse(xPosEnd) + .5);
				drawConnectingLine(xPosStart, yPosStart, xPosEnd, yPosEnd, LineMark.PARABOLA_LINE);
			}
			else {
				pointArray = new int[(xPosEnd - xPosStart) * 2];
				for (int i = 0; i < xPosEnd - xPosStart; i++) {
					pointArray[i * 2] = this.offSetX + xPosStart + i;
					pointArray[i * 2 + 1] = this.offSetY + (int) (singleResponseRegression.getResponse(xPosStart + i) + .5);
				}
				setLineMark(LineMark.PARABOLA_LINE);
				CurveSurvey.this.canvasGC.drawPolyline(pointArray);
			}

			{ // calculate the extremum values
				int yLowerLimitTmp;
				int yUpperLimitTmp;
				{ // calculate the display parabola's left and right value
					int leftExtremum = (int) singleResponseRegression.getResponse(points.get(0).x());
					int rightExtremum = (int) singleResponseRegression.getResponse(points.get(points.size() - 1).x());
					yLowerLimitTmp = Math.max(this.yLowerLimit, Math.max(leftExtremum, rightExtremum));
					yUpperLimitTmp = Math.min(this.yUpperLimit, Math.min(leftExtremum, rightExtremum));
					// check the parabola extremum
					if (singleResponseRegression.getGamma() != 0.) {
						Double xPosExtremum = singleResponseRegression.getParabolaExtremum();
						double mid = (points.get(0).x() + points.get(points.size() - 1).x()) / 2.;
						if (Math.abs(xPosExtremum - mid) <= (Math.abs(points.get(0).x() - mid))) { // extremum is between
							int absoluteExtremum = (int) singleResponseRegression.getResponse(xPosExtremum);
							yLowerLimitTmp = Math.max(this.yLowerLimit, absoluteExtremum);
							yUpperLimitTmp = Math.min(this.yUpperLimit, absoluteExtremum);
						}
					}
				}
				// add 3 pixels to compensate truncation errors
				this.yLowerLimit = Math.min(this.height, yLowerLimitTmp + 3);
				this.yUpperLimit = Math.max(0, yUpperLimitTmp - 3);
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "yUpperLimit=" + this.yUpperLimit + " yLowerLimit=" + this.yLowerLimit); //$NON-NLS-1$ //$NON-NLS-2$
		}

		public void drawCross(int posFromLeft1, int posFromTop1) {
			drawVerticalLine(posFromLeft1, 0, this.height, LineMark.MEASURE_CROSS);
			drawHorizontalLine(posFromTop1, 0, this.width, LineMark.MEASURE_CROSS);
		}

		public void drawDoubleCross(int posFromLeft1, int posFromTop1, int posFromLeft2, int posFromTop2) {
			drawVerticalLine(posFromLeft1, 0, this.height, LineMark.MEASURE_CROSS);
			drawHorizontalLine(posFromTop1, 0, this.width, LineMark.MEASURE_CROSS);

			drawVerticalLine(posFromLeft2, 0, this.height, LineMark.DELTA_CROSS);
			drawHorizontalLine(posFromTop2, 0, this.width, LineMark.DELTA_CROSS);

			this.drawConnectingLine(posFromLeft1, posFromTop1, posFromLeft2, posFromTop2, LineMark.DIAG_LINE);

			this.yLowerLimit = Math.max(this.yLowerLimit, Math.max(posFromTop1, posFromTop2));
			this.yUpperLimit = Math.min(this.yUpperLimit, Math.min(posFromTop1, posFromTop2));
			this.xLeftLimit = Math.min(this.xLeftLimit, Math.min(posFromLeft1, posFromLeft2));
			this.xRightLimit = Math.max(this.xRightLimit, Math.max(posFromLeft1, posFromLeft2));
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "xLeftLimit=" + this.xLeftLimit + " xRightLimit=" + this.xRightLimit + "yUpperLimit=" + this.yUpperLimit //$NON-NLS-1$//$NON-NLS-2$
					+ " yLowerLimit=" + this.yLowerLimit);
		}

		public void drawBoxplot(int xPos, int[] yPosBoxplot) {
			int halfBoxWidth = getBoxWidth() / 2;
			int xPosLeft = xPos - halfBoxWidth;
			int xPosRight = xPos + halfBoxWidth;
			int radius = halfBoxWidth / 4;
			{
				// x axis limits are defined by the boxplot frame
				this.xLeftLimit = Math.min(xPosLeft, this.xLeftLimit);
				this.xRightLimit = Math.max(xPosRight, this.xRightLimit);

				int yPosQuartile1 = yPosBoxplot[UniversalQuantile.BoxplotItems.QUARTILE1.ordinal()];
				drawHorizontalLine(yPosQuartile1, xPosLeft, halfBoxWidth * 2, LineMark.BOXPLOT);
				drawHorizontalLine(yPosBoxplot[UniversalQuantile.BoxplotItems.QUARTILE2.ordinal()], xPosLeft, halfBoxWidth * 2, LineMark.BOXPLOT);
				int yPosQuartile3 = yPosBoxplot[UniversalQuantile.BoxplotItems.QUARTILE3.ordinal()];
				drawHorizontalLine(yPosQuartile3, xPosLeft, halfBoxWidth * 2, LineMark.BOXPLOT);
				drawVerticalLine(xPosLeft, yPosQuartile3, yPosQuartile1 - yPosQuartile3, LineMark.BOXPLOT);
				drawVerticalLine(xPosRight, yPosQuartile3, yPosQuartile1 - yPosQuartile3, LineMark.BOXPLOT);

				int yPosLowerWhisker = yPosBoxplot[UniversalQuantile.BoxplotItems.LOWER_WHISKER.ordinal()];
				drawHorizontalLine(yPosLowerWhisker, xPos - halfBoxWidth / 2, halfBoxWidth, LineMark.BOXPLOT);
				drawVerticalLine(xPos, yPosQuartile1, yPosLowerWhisker - yPosQuartile1, LineMark.BOXPLOT);

				int yPosUpperWhisker = yPosBoxplot[UniversalQuantile.BoxplotItems.UPPER_WHISKER.ordinal()];
				drawHorizontalLine(yPosUpperWhisker, xPos - halfBoxWidth / 2, halfBoxWidth, LineMark.BOXPLOT);
				drawVerticalLine(xPos, yPosUpperWhisker, yPosQuartile3 - yPosUpperWhisker, LineMark.BOXPLOT);

				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("xPos=%d  LW=%d Q1=%d Q2=%d Q3=%d UW=%d ", xPos, yPosLowerWhisker, yPosQuartile1, //$NON-NLS-1$
						yPosBoxplot[UniversalQuantile.BoxplotItems.QUARTILE2.ordinal()], yPosQuartile3, yPosUpperWhisker));
			}
			{
				int yPosBottom = yPosBoxplot[UniversalQuantile.BoxplotItems.QUARTILE0.ordinal()];
				if (yPosBottom != yPosBoxplot[UniversalQuantile.BoxplotItems.LOWER_WHISKER.ordinal()]) drawOutlier(xPos, yPosBottom, radius, LineMark.BOXPLOT);
				int yPosTop = yPosBoxplot[UniversalQuantile.BoxplotItems.QUARTILE4.ordinal()];
				if (yPosTop != yPosBoxplot[UniversalQuantile.BoxplotItems.UPPER_WHISKER.ordinal()]) drawOutlier(xPos, yPosTop, radius, LineMark.BOXPLOT);

				// y axis limits may cross the top or bottom defined by the curve values
				this.yLowerLimit = Math.max(this.yLowerLimit, yPosBottom + radius);
				this.yUpperLimit = Math.min(this.yUpperLimit, yPosTop - radius);
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "xLeftLimit=" + this.xLeftLimit + " xRightLimit=" + this.xRightLimit + " yUpperLimit=" + this.yUpperLimit //$NON-NLS-1$//$NON-NLS-2$
					+ " yLowerLimit=" + this.yLowerLimit);
		}

		/**
		 * Required for resizings down to a very small area.
		 * If the canvas image is too small we will not be able to use it as a source for copy.
		 * @param canvasImage
		 * @return true if the canvas image is bigger than the curve area bounds at the time of the last drawing
		 */
		public boolean isSizeForErasure(Image canvasImage) {
			return canvasImage.getBounds().width > this.offSetX + this.width && canvasImage.getBounds().height > this.offSetY + this.height;
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
				log.log(Level.FINER, String.format("imageDisposed=%s posFromLeft=%d posFromTop=%d lineWidth=%d length=%d", canvasImage.isDisposed(), posFromLeft, posFromTop, lineWidth, length)); //$NON-NLS-1$
			CurveSurvey.this.canvasGC.drawImage(canvasImage, posFromLeft + this.offSetX, posFromTop + this.offSetY, lineWidth, length, posFromLeft + this.offSetX, posFromTop + this.offSetY, lineWidth,
					length);
		}

		/**
		 * erase a horizontal line by re-drawing the curve area image
		 * @param posFromTop
		 * @param posFromLeft
		 * @param length
		 * @param lineWidth
		 */
		public void eraseHorizontalLine(Image canvasImage, int posFromTop, int posFromLeft, int length, int lineWidth) {
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, String.format("imageDisposed=%s posFromLeft=%d posFromTop=%d lineWidth=%d length=%d", canvasImage.isDisposed(), posFromLeft, posFromTop, lineWidth, length)); //$NON-NLS-1$
			// do not erase lines beyond the y axis drawing area
			if (posFromTop >= 0 && posFromTop <= this.height) CurveSurvey.this.canvasGC.drawImage(canvasImage, posFromLeft + this.offSetX, posFromTop + this.offSetY, length, lineWidth,
					posFromLeft + this.offSetX, posFromTop + this.offSetY, length, lineWidth);
		}

		/**
		 * Clean survey area lines by re-drawing the untouched curve area image of this rectangle.
		 */
		public void cleanRectangle(Image canvasImage) {
			if (this.yLowerLimit != Integer.MIN_VALUE || this.yUpperLimit != Integer.MAX_VALUE) {
				int maxLineWidth = 3;
				{
					// extend the rectangle to avoid remains of thicker lines or boxplot parts which lie beyond the rectangle
					int left = Math.max(0, this.xLeftLimit - maxLineWidth); // left is never bigger than width due to the lineWidth
					int right = Math.min(this.width, this.xRightLimit + maxLineWidth); // right is never less than 0 due to the lineWidth
					int top = Math.min(this.height, Math.max(0, this.yUpperLimit - maxLineWidth));
					int bottom = Math.min(this.height, Math.max(0, this.yLowerLimit + maxLineWidth));
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "left=" + left + " top=" + top + " width=" + (right - left) + " height=" + (bottom - top)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

					CurveSurvey.this.canvasGC.drawImage(canvasImage, left + this.offSetX, top + this.offSetY, right - left, bottom - top, left + this.offSetX, top + this.offSetY, right - left, bottom - top);
				}
				this.yLowerLimit = Integer.MIN_VALUE;
				this.yUpperLimit = Integer.MAX_VALUE;
				this.xLeftLimit = Integer.MAX_VALUE;
				this.xRightLimit = Integer.MIN_VALUE;
			}
		}

	}

	private final Settings			settings		= Settings.getInstance();
	private final HistoTimeLine	timeLine;
	private final TrailRecord		trailRecord;

	private TrailRecordCutter		recordSection;
	private LinePainter					linePainter;

	private GC									canvasGC;
	private Rectangle						curveAreaBounds;

	private long								timestampMeasure_ms;
	private long								timestampDelta_ms;
	private int									xPosMeasure	= 0;
	private int									yPosMeasure	= 0;
	private int									xPosDelta		= 0;
	private int									yPosDelta		= 0;

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

		this.linePainter = new LinePainter(this.curveAreaBounds);
		this.linePainter.drawCross(this.xPosMeasure, this.yPosMeasure);

		if (this.yPosMeasure >= Integer.MIN_VALUE) {
			//			String formattedTimeWithUnit = LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmmss, this.timeLine.getAdjacentTimestamp(this.xPosMeasure));
			statusMessage = Messages.getString(MessageIds.GDE_MSGT0256,
					new Object[] { this.trailRecord.getName(), new TrailRecordFormatter(this.trailRecord).getMeasureValue(this.trailRecord.getParentTrail().getIndex(this.timestampMeasure_ms)),
							this.trailRecord.getUnit(), LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmmss, this.timestampMeasure_ms) });
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

		this.linePainter = new LinePainter(this.curveAreaBounds);
		this.linePainter.drawDoubleCross(this.xPosMeasure, this.yPosMeasure, this.xPosDelta, this.yPosDelta);

		if (this.yPosMeasure != Integer.MIN_VALUE || this.yPosDelta != Integer.MIN_VALUE) {
			if (this.settings.isCurveSurvey()) {
				drawCurveSurvey();
				String deltaText = this.recordSection.getFormattedBoundsDelta();
				String unitText = this.trailRecord.getUnit();
				String avgText = this.recordSection.getFormattedBoundsAvg();
				String slopeText = this.recordSection.getFormattedBoundsSlope();
				statusMessage = Messages.getString(MessageIds.GDE_MSGT0848,
						new Object[] { this.trailRecord.getName(), unitText, deltaText, LocalizedDateTime.getFormatedDistance(this.timestampMeasure_ms, this.timestampDelta_ms) })
						+ Messages.getString(MessageIds.GDE_MSGT0879, new Object[] { unitText, avgText, unitText, slopeText });
			}
			else {
				String deltaText = this.recordSection.getFormattedBoundsDelta();
				String unitText = this.trailRecord.getUnit();
				statusMessage = Messages.getString(MessageIds.GDE_MSGT0848,
						new Object[] { this.trailRecord.getName(), unitText, deltaText, LocalizedDateTime.getFormatedDistance(this.timestampMeasure_ms, this.timestampDelta_ms) });
			}
		}
		else {
			if (this.recordSection.isValidBounds() && this.settings.isCurveSurvey()) {
				drawCurveSurvey();
				String unitText = this.trailRecord.getUnit();
				String avgText = this.recordSection.getFormattedBoundsAvg();
				String slopeText = this.recordSection.getFormattedBoundsSlope();
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
	 * Draw regression lines and the boxplot element.
	 * @param trailRecord
	 */
	private void drawCurveSurvey() {
		HistoGraphicsMapper mapper = new HistoGraphicsMapper(this.trailRecord);

		if (!this.recordSection.isBoundedParabola()) { // hide these curves for better overview whenever a parabola is shown
			int yBoundedAvg = mapper.getVerticalDisplayPos(this.recordSection.getBoundedAvgValue());
			this.linePainter.drawAverageLine(yBoundedAvg, this.xPosMeasure, this.xPosDelta - this.xPosMeasure);

			int yRegressionPosMeasure = mapper.getVerticalDisplayPos(this.recordSection.getBoundedSlopeValue(this.timestampMeasure_ms));
			int yRegressionPosDelta = mapper.getVerticalDisplayPos(this.recordSection.getBoundedSlopeValue(this.timestampDelta_ms));
			this.linePainter.drawLinearRegressionLine(this.xPosMeasure, yRegressionPosMeasure, this.xPosDelta, yRegressionPosDelta);
		}
		else {
			List<Spot<Integer>> points = new ArrayList<>();
			for (Spot<Double> entry : this.recordSection.getBoundedParabolaValues()) {
				points.add(new Spot<Integer>(this.timeLine.getXPosTimestamp((long) entry.x().doubleValue()), mapper.getVerticalDisplayPos(entry.y())));
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "values " + Arrays.toString(points.toArray()));
			this.linePainter.drawRegressionParabolaLine(points);
		}
		{
			int xPosMidBounds = (this.xPosDelta + this.xPosMeasure) / 2;
			double[] values = this.recordSection.getBoundedBoxplotValues();
			int[] yPosBoxplot = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				yPosBoxplot[i] = mapper.getVerticalDisplayPos(values[i]);
			}
			this.linePainter.drawBoxplot(xPosMidBounds, yPosBoxplot);
		}
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
	 * clean (old) measurement pointer - check pointer in curve area
	 * @param canvasImage
	 */
	public void cleanMeasurementPointer(Image canvasImage) {
		if (this.linePainter != null && this.linePainter.isSizeForErasure(canvasImage)) {
			if (this.xPosMeasure > 0) {
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "xPosMeasure=" + this.xPosMeasure + " xPosTimestamp=" + this.timeLine.getXPosTimestamp(this.timestampMeasure_ms)); //$NON-NLS-1$ //$NON-NLS-2$
				this.linePainter.eraseVerticalLine(canvasImage, this.xPosMeasure, 0, this.curveAreaBounds.height, LineMark.MEASURE_CROSS.lineWidth);
				this.linePainter.eraseHorizontalLine(canvasImage, this.yPosMeasure, 0, this.curveAreaBounds.width, LineMark.MEASURE_CROSS.lineWidth);
			}
			if (this.xPosDelta > 0) {
				this.linePainter.eraseVerticalLine(canvasImage, this.xPosDelta, 0, this.curveAreaBounds.height, LineMark.DELTA_CROSS.lineWidth);
				this.linePainter.eraseHorizontalLine(canvasImage, this.yPosDelta, 0, this.curveAreaBounds.width, LineMark.DELTA_CROSS.lineWidth);
				this.linePainter.cleanRectangle(canvasImage);
			}
		}
		else {
			// not erasing the lines provokes a full redraw if the size has changed sufficiently
		}
	}

	/**
	 * Set the measure canvas positions.
	 * @param curveAreaBounds
	 * @param timeStampMeasure_ms is a valid time stamp
	 */
	public void setPosMeasure(Rectangle curveAreaBounds, long timeStampMeasure_ms) {
		HistoGraphicsMapper mapper = new HistoGraphicsMapper(this.trailRecord);

		boolean isNullAcceptable = false; // do not allow positioning on timestamps with null values
		this.curveAreaBounds = curveAreaBounds;

		long timestampMeasureNew_ms = timeStampMeasure_ms;
		int yPosMeasureNew = mapper.getVerticalDisplayPos(this.trailRecord.getParentTrail().getIndex(timeStampMeasure_ms));
		if (!isNullAcceptable && yPosMeasureNew == Integer.MIN_VALUE) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("timestampMeasure_ms=%d search first non-null value from the left", timeStampMeasure_ms)); //$NON-NLS-1$
			int i = -1;
			while (yPosMeasureNew == Integer.MIN_VALUE) {
				yPosMeasureNew = mapper.getVerticalDisplayPos(++i);
			}
			timestampMeasureNew_ms = this.trailRecord.getParentTrail().getDisplayTimeStamp_ms(i);
		}

		this.timestampMeasure_ms = timestampMeasureNew_ms;
		this.xPosMeasure = this.timeLine.getXPosTimestamp(timestampMeasureNew_ms);
		this.yPosMeasure = yPosMeasureNew;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("timestampMeasure_ms=%d xPosMeasure=%d yPosMeasure=%d", this.timestampMeasure_ms, this.xPosMeasure, this.yPosMeasure)); //$NON-NLS-1$

		this.recordSection = new TrailRecordCutter(this.trailRecord, this.timestampMeasure_ms, this.timestampDelta_ms);
	}

	/**
	 * Set the delta canvas positions.
	 * @param curveAreaBounds
	 * @param timeStampDelta_ms is a valid time stamp
	 */
	public void setPosDelta(Rectangle curveAreaBounds, long timeStampDelta_ms) {
		HistoGraphicsMapper mapper = new HistoGraphicsMapper(this.trailRecord);

		boolean isNullAcceptable = false; // do not allow positioning on timestamps with null values
		this.curveAreaBounds = curveAreaBounds;

		long timestampDeltaNew_ms = timeStampDelta_ms;
		int yPosDeltaNew = mapper.getVerticalDisplayPos(this.trailRecord.getParentTrail().getIndex(timeStampDelta_ms));
		if (!isNullAcceptable && yPosDeltaNew == Integer.MIN_VALUE) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("timestampDelta_ms=%d search first non-null value from the right", timeStampDelta_ms)); //$NON-NLS-1$
			int i = this.trailRecord.getParentTrail().getTimeStepSize();
			while (yPosDeltaNew == Integer.MIN_VALUE) {
				yPosDeltaNew = mapper.getVerticalDisplayPos(--i);
			}
			timestampDeltaNew_ms = this.trailRecord.getParentTrail().getDisplayTimeStamp_ms(i);
		}

		this.timestampDelta_ms = timestampDeltaNew_ms;
		this.xPosDelta = this.timeLine.getXPosTimestamp(timestampDeltaNew_ms);
		this.yPosDelta = yPosDeltaNew;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("timestampDelta_ms=%d xPosDelta=%d yPosDelta=%d", this.timestampDelta_ms, this.xPosDelta, this.yPosDelta)); //$NON-NLS-1$

		this.recordSection = new TrailRecordCutter(this.trailRecord, this.timestampMeasure_ms, this.timestampDelta_ms);
	}

	/**
	 * @param xPos
	 * @return true if the x position is close to the vertical line
	 */
	public boolean isNearDeltaLine(int xPos) {
		return this.xPosDelta + 1 >= xPos && this.xPosDelta - 1 <= xPos;
	}

	/**
	 * @param xPos
	 * @return true if the x position is close to the vertical line
	 */
	public boolean isNearMeasureLine(int xPos) {
		return this.xPosMeasure + 1 >= xPos && this.xPosMeasure - 1 <= xPos;
	}

	/**
	 * @param xPos
	 * @return true if the x position is exactly over the measurement or delta vertical line
	 */
	public boolean isOverVerticalLine(int xPos) {
		return xPos > 0 && this.xPosMeasure > 0 && isNearMeasureLine(xPos) || xPos > 0 && this.xPosDelta > 0 && isNearDeltaLine(xPos);
	}

	public boolean isNewMeasureSpot(long timestampNew_ms, int yPosNew) {
		return yPosNew != Integer.MIN_VALUE && (timestampNew_ms != this.timestampMeasure_ms || yPosNew != this.yPosMeasure);
	}

	public boolean isNewDeltaSpot(long timestampNew_ms, int yPosNew) {
		return yPosNew != Integer.MIN_VALUE && (timestampNew_ms != this.timestampDelta_ms || yPosNew != this.yPosDelta);
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

}
