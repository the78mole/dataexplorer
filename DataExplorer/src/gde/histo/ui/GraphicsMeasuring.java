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

import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.histo.recordings.HistoGraphicsMapper;
import gde.histo.recordings.TrailRecordSetFormatter;
import gde.histo.utils.HistoTimeLine;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

/**
 * Curve survey activity mapper for mouse movements and other UI actions from menus, checkboxes etc.
 * @author Thomas Eickert (USER)
 */
public final class GraphicsMeasuring extends AbstractMeasuring {
	private final static String	$CLASS_NAME	= GraphicsMeasuring.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	public enum HistoGraphicsMode {

		/** Single measurement mode is active */
		MEASURE {
			@Override
			void setMode(GraphicsMeasuring graphicsMeasuring) {
				graphicsMeasuring.isLeftMouseMeasure = true;
				graphicsMeasuring.isRightMouseMeasure = false;
			}

			@Override
			void drawMeasurement(GraphicsMeasuring hgm, long timestampMeasureNew_ms, long timestampDeltaNew_ms) {
				AbstractChartComposite hgc = hgm.graphicsComposite;
				hgm.curveSurvey.setPosMeasure(hgc.curveAreaBounds, timestampMeasureNew_ms);

				String statusMessage = hgm.curveSurvey.drawMeasurementGraphics();
				hgc.windowActor.setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc.recordSetComment, timestampMeasureNew_ms));
				hgc.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0897));
			}

			@Override
			void drawLeftMeasurement(GraphicsMeasuring hgm, long timestamp_ms) {
				AbstractChartComposite hgc = hgm.graphicsComposite;
				long startTime = new Date().getTime();
				hgm.curveSurvey.cleanMeasurementPointer(hgc.canvasImage);

				hgm.curveSurvey.setPosMeasure(hgc.curveAreaBounds, timestamp_ms);
				String statusMessage = hgm.curveSurvey.drawMeasurementGraphics();
				hgc.windowActor.setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc.recordSetComment, timestamp_ms));
				hgc.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0897));
				log.time(() -> "draw time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
			}

			@Override
			void drawRightMeasurement(GraphicsMeasuring hgm, long timestamp_ms) {
				// simple measure has only a left measurement
			}

			@Override
			long[] defineInitialTimestamps_ms(AbstractChartComposite hgc) {
				long timestampMeasureNew_ms = ((GraphicsComposite) hgc).getTimeLine().getAdjacentTimestamp(hgc.curveAreaBounds.width / 4);
				long timestampDeltaNew_ms = timestampMeasureNew_ms; // is not required for single measurement
				return new long[] { timestampMeasureNew_ms, timestampDeltaNew_ms };
			}
		},

		/** delta measurement mode is active */
		MEASURE_DELTA {
			@Override
			void setMode(GraphicsMeasuring graphicsMeasuring) {
				graphicsMeasuring.isLeftMouseMeasure = false;
				graphicsMeasuring.isRightMouseMeasure = true;
			}

			@Override
			void drawMeasurement(GraphicsMeasuring hgm, long timestampMeasureNew_ms, long timestampDeltaNew_ms) {
				AbstractChartComposite hgc = hgm.graphicsComposite;
				hgm.curveSurvey.setPosMeasure(hgc.curveAreaBounds, timestampMeasureNew_ms);
				hgm.curveSurvey.setPosDelta(hgc.curveAreaBounds, timestampDeltaNew_ms);

				String statusMessage = hgm.curveSurvey.drawDeltaMeasurementGraphics();
				hgc.windowActor.setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc.recordSetComment, timestampMeasureNew_ms));
				hgc.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0897));
			}

			@Override
			void drawLeftMeasurement(GraphicsMeasuring hgm, long timestamp_ms) {
				AbstractChartComposite hgc = hgm.graphicsComposite;
				long startTime = new Date().getTime();
				hgm.curveSurvey.cleanMeasurementPointer(hgc.canvasImage);

				hgm.curveSurvey.setPosMeasure(hgc.curveAreaBounds, timestamp_ms);
				String statusMessage = hgm.curveSurvey.drawDeltaMeasurementGraphics();
				hgc.windowActor.setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc.recordSetComment, timestamp_ms));
				hgc.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0897));
				log.time(() -> "draw time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
			}

			@Override
			void drawRightMeasurement(GraphicsMeasuring hgm, long timestamp_ms) {
				AbstractChartComposite hgc = hgm.graphicsComposite;
				long startTime = new Date().getTime();
				hgm.curveSurvey.cleanMeasurementPointer(hgc.canvasImage);

				hgm.curveSurvey.setPosDelta(hgc.curveAreaBounds, timestamp_ms);
				String statusMessage = hgm.curveSurvey.drawDeltaMeasurementGraphics();
				hgc.windowActor.setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc.recordSetComment, timestamp_ms));
				hgc.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0897));
				log.time(() -> "draw time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
			}

			@Override
			long[] defineInitialTimestamps_ms(AbstractChartComposite hgc) {
				HistoTimeLine timeLine = ((GraphicsComposite) hgc).getTimeLine();
				if (timeLine.getScalePositions().size() > 1) { // todo check if one position would be sufficient
					int margin = hgc.curveAreaBounds.width / (timeLine.getScalePositions().size() + 1);
					long timestampMeasureNew_ms = timeLine.getAdjacentTimestamp(margin);
					// long timestampDeltaNew_ms = hgc.timeLine.getAdjacentTimestamp(hgc.curveAreaBounds.width - margin);
					long timestampDeltaNew_ms = timeLine.getAdjacentTimestamp(hgc.curveAreaBounds.width * 2 / 3);
					return new long[] { timestampMeasureNew_ms, timestampDeltaNew_ms };
				} else {
					return new long[0];
				}
			}
		};

		/**
		 * Set graphics window measurement mode.
		 */
		abstract void setMode(GraphicsMeasuring graphicsMeasuring);

		/**
		 * Perform the UI activities required for a measurement.
		 */
		abstract void drawMeasurement(GraphicsMeasuring hgm, long timestampMeasureNew_ms, long timestampDeltaNew_ms);

		/**
		 * Perform the UI activities for the first measurement timestamp.
		 * In case of delta measurement: the timestamp which is initially on the left.
		 */
		abstract void drawLeftMeasurement(GraphicsMeasuring hgm, long timestamp_ms);

		/**
		 * Perform the UI activities for the second measurement timestamp used for delta measurement.
		 */
		abstract void drawRightMeasurement(GraphicsMeasuring hgm, long timestamp_ms);

		/**
		 * Determine the upper and lower timestamps for a new measurement mode.
		 */
		abstract long[] defineInitialTimestamps_ms(AbstractChartComposite hgc);

		private static String getSelectedMeasurementsAsTable(Text recordSetComment, long timestamp_ms) {
			recordSetComment.setFont(SWTResourceManager.getFont("Courier New", GDE.WIDGET_FONT_SIZE - 1, SWT.BOLD)); //$NON-NLS-1$
			return TrailRecordSetFormatter.getSelectedMeasurementsAsTable(timestamp_ms);
		}

		static HistoGraphicsMode getMode(boolean isDeltaMeasuring) {
			if (isDeltaMeasuring) {
				return HistoGraphicsMode.MEASURE_DELTA;
			} else {
				return HistoGraphicsMode.MEASURE;
			}
		}
	};

	boolean																isLeftMouseMeasure	= false;
	boolean																isRightMouseMeasure	= false;

	private final AbstractChartComposite	graphicsComposite;
	private final CurveSurvey							curveSurvey;

	private GC														canvasGC;
	private HistoGraphicsMode							mode;

	public GraphicsMeasuring(GraphicsComposite graphicsComposite, Measure measure) {
		super(measure);
		this.graphicsComposite = graphicsComposite;
		this.mode = HistoGraphicsMode.getMode(measure.isDeltaMeasure);
		this.mode.setMode(this);

		this.curveSurvey = new CurveSurvey(this.canvasGC, measure.measureRecord, graphicsComposite.getTimeLine());
		long[] initialTimestamps_ms = this.mode.defineInitialTimestamps_ms(this.graphicsComposite);
		this.curveSurvey.setPosMeasure(graphicsComposite.curveAreaBounds, initialTimestamps_ms[0]);
		this.curveSurvey.setPosDelta(graphicsComposite.curveAreaBounds, initialTimestamps_ms[1]);
	}

	/**
	 * Draw a refreshed summary measurement.
	 */
	public void drawSummaryMeasurement() {
		drawSummaryMeasurement(this.curveSurvey.getTimestampMeasure_ms(), this.curveSurvey.getTimestampDelta_ms());
	}

	/**
	 * Draw a new measurement based on the timestamp values.
	 */
	private void drawSummaryMeasurement(long timestampMeasureNew_ms, long timestampDeltaNew_ms) {
		Image canvasImage = this.graphicsComposite.canvasImage;
		// set the gc properties
		this.canvasGC = new GC(this.graphicsComposite.graphicCanvas);
		this.canvasGC.setForeground(measure.measureRecord.getColor());

		this.curveSurvey.setCanvasGC(this.canvasGC);
		// all obsolete lines are cleaned up now draw new position marker
		this.curveSurvey.cleanMeasurementPointer(canvasImage);

		this.mode.drawMeasurement(this, timestampMeasureNew_ms, timestampDeltaNew_ms);
		this.canvasGC.dispose();
	}

	/**
	 * Draw a refreshed measurement.
	 */
	@Override
	public void drawMeasuring() {
		drawMeasurement(this.curveSurvey.getTimestampMeasure_ms(), this.curveSurvey.getTimestampDelta_ms());
	}

	/**
	 * Draw a new measurement based on the timestamp values.
	 */
	private void drawMeasurement(long timestampMeasureNew_ms, long timestampDeltaNew_ms) {
		Image canvasImage = this.graphicsComposite.canvasImage;
		// set the gc properties
		this.canvasGC = new GC(this.graphicsComposite.graphicCanvas);
		this.canvasGC.setForeground(measure.measureRecord.getColor());

		this.curveSurvey.setCanvasGC(this.canvasGC);
		// all obsolete lines are cleaned up now draw new position marker
		this.curveSurvey.cleanMeasurementPointer(canvasImage);

		this.mode.drawMeasurement(this, timestampMeasureNew_ms, timestampDeltaNew_ms);
		this.canvasGC.dispose();
	}

	/**
	 * Reset the graphic area and comment.
	 */
	@Override
	public void cleanMeasuring() {
		this.isLeftMouseMeasure = false;
		this.isRightMouseMeasure = false;

		this.canvasGC = new GC(this.graphicsComposite.graphicCanvas);
		this.curveSurvey.setCanvasGC(this.canvasGC);

		this.curveSurvey.cleanMeasurementPointer(this.graphicsComposite.canvasImage);

		this.graphicsComposite.setRecordSetCommentStandard();
	}

	/**
	 * Draw the survey graphics while moving the vertical line.
	 */
	@Override
	public void processMouseDownMove(long timestamp_ms) {
		this.canvasGC = new GC(this.graphicsComposite.graphicCanvas);
		this.curveSurvey.setCanvasGC(this.canvasGC);
		this.canvasGC.setForeground(measure.measureRecord.getColor());

		if (this.isLeftMouseMeasure) {
			int height = graphicsComposite.curveAreaBounds.height;
			int yPosMeasureNew = HistoGraphicsMapper.getVerticalDisplayPos(measure.measureRecord, height, measure.measureRecord.getParent().getIndex(timestamp_ms));
			if (this.curveSurvey.isNewMeasureSpot(timestamp_ms, yPosMeasureNew)) {
				this.mode.drawLeftMeasurement(this, timestamp_ms);
			}
		} else if (this.isRightMouseMeasure) {
			int height = graphicsComposite.curveAreaBounds.height;
			int yPosDeltaNew = HistoGraphicsMapper.getVerticalDisplayPos(measure.measureRecord, height, measure.measureRecord.getParent().getIndex(timestamp_ms));
			if (this.curveSurvey.isNewDeltaSpot(timestamp_ms, yPosDeltaNew)) {
				this.mode.drawRightMeasurement(this, timestamp_ms);
			}
		}

		this.canvasGC.dispose();
	}

	/**
	 * Perform UI activities at mouse up movements.
	 */
	@Override
	public void processMouseUpMove(Point point) {
		if (this.curveSurvey.isOverVerticalLine(point.x)) {
			this.graphicsComposite.graphicCanvas.setCursor(SWTResourceManager.getCursor("gde/resource/MoveH.gif")); //$NON-NLS-1$
		} else {
			this.graphicsComposite.graphicCanvas.setCursor(DataExplorer.getInstance().getCursor());
		}
	}

	/**
	 * Determine which vertical line was moved.
	 */
	@Override
	public void processMouseDownAction(Point point) {
		if (this.curveSurvey.isOverVerticalLine(point.x)) {
			if (this.curveSurvey.isNearMeasureLine(point.x)) {
				this.isLeftMouseMeasure = true;
				this.isRightMouseMeasure = false;
			} else if (this.curveSurvey.isNearDeltaLine(point.x)) {
				this.isRightMouseMeasure = true;
				this.isLeftMouseMeasure = false;
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}

	/**
	 * Reset the identified vertical line.
	 */
	@Override
	public void processMouseUpAction(Point point) {
		if (this.isLeftMouseMeasure) {
			this.isLeftMouseMeasure = false;
			// application.setStatusMessage(GDE.STRING_EMPTY);
		} else if (this.isRightMouseMeasure) {
			this.isRightMouseMeasure = false;
			// application.setStatusMessage(GDE.STRING_EMPTY);
		}
		log.time(() -> "isMouseMeasure = " + this.isLeftMouseMeasure + " isMouseDeltaMeasure = " + this.isRightMouseMeasure); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
