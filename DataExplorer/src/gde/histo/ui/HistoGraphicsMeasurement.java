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

import gde.GDE;
import gde.histo.recordings.HistoGraphicsMapper;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.recordings.TrailRecordSetFormatter;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

/**
 * Curve survey activity mapper for mouse movements and other UI actions from menus, checkboxes etc.
 * @author Thomas Eickert (USER)
 */
public final class HistoGraphicsMeasurement {
	private final static String	$CLASS_NAME	= HistoGraphicsMeasurement.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	public enum HistoGraphicsMode {

		/** Single measurement mode is active */
		MEASURE {
			@Override
			void setMode(HistoGraphicsComposite hgc) {
				hgc.graphicsMeasurement.isLeftMouseMeasure = true;
				hgc.graphicsMeasurement.isRightMouseMeasure = false;
			}

			@Override
			void drawMeasurement(HistoGraphicsComposite hgc, long timestampMeasureNew_ms, long timestampDeltaNew_ms) {
				HistoGraphicsMeasurement hgm = hgc.graphicsMeasurement;

				hgm.curveSurvey.setPosMeasure(hgc.curveAreaBounds, timestampMeasureNew_ms);

				String statusMessage = hgm.curveSurvey.drawMeasurementGraphics();
				DataExplorer.getInstance().setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc, timestampMeasureNew_ms));
			}

			@Override
			void drawLeftMeasurement(HistoGraphicsComposite hgc, long timestamp_ms) {
				HistoGraphicsMeasurement hgm = hgc.graphicsMeasurement;
				long startTime = new Date().getTime();
				hgm.curveSurvey.cleanMeasurementPointer(hgc.canvasImage);

				hgm.curveSurvey.setPosMeasure(hgc.curveAreaBounds, timestamp_ms);
				String statusMessage = hgm.curveSurvey.drawMeasurementGraphics();
				DataExplorer.getInstance().setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc, timestamp_ms));
				log.time(() -> "draw time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
			}

			@Override
			void drawRightMeasurement(HistoGraphicsComposite hgc, long timestamp_ms) {
			}

			@Override
			void drawInitialMeasurement(HistoGraphicsComposite hgc) {
				long timestampMeasureNew_ms = hgc.timeLine.getAdjacentTimestamp(hgc.curveAreaBounds.width / 4);
				long timestampDeltaNew_ms = timestampMeasureNew_ms; // is not required for single measurement
				hgc.graphicsMeasurement.drawMeasurement(timestampMeasureNew_ms, timestampDeltaNew_ms);
			}
		},

		/** delta measurement mode is active */
		MEASURE_DELTA {
			@Override
			void setMode(HistoGraphicsComposite hgc) {
				hgc.graphicsMeasurement.isLeftMouseMeasure = false;
				hgc.graphicsMeasurement.isRightMouseMeasure = true;
			}

			@Override
			void drawMeasurement(HistoGraphicsComposite hgc, long timestampMeasureNew_ms, long timestampDeltaNew_ms) {
				HistoGraphicsMeasurement hgm = hgc.graphicsMeasurement;
				hgm.curveSurvey.setPosMeasure(hgc.curveAreaBounds, timestampMeasureNew_ms);
				hgm.curveSurvey.setPosDelta(hgc.curveAreaBounds, timestampDeltaNew_ms);

				String statusMessage = hgm.curveSurvey.drawDeltaMeasurementGraphics();
				DataExplorer.getInstance().setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc, timestampMeasureNew_ms));
			}

			@Override
			void drawLeftMeasurement(HistoGraphicsComposite hgc, long timestamp_ms) {
				HistoGraphicsMeasurement hgm = hgc.graphicsMeasurement;
				long startTime = new Date().getTime();
				hgm.curveSurvey.cleanMeasurementPointer(hgc.canvasImage);

				hgm.curveSurvey.setPosMeasure(hgc.curveAreaBounds, timestamp_ms);
				String statusMessage = hgm.curveSurvey.drawDeltaMeasurementGraphics();
				DataExplorer.getInstance().setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc, timestamp_ms));
				log.time(() -> "draw time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
			}

			@Override
			void drawRightMeasurement(HistoGraphicsComposite hgc, long timestamp_ms) {
				HistoGraphicsMeasurement hgm = hgc.graphicsMeasurement;
				long startTime = new Date().getTime();
				hgm.curveSurvey.cleanMeasurementPointer(hgc.canvasImage);

				hgm.curveSurvey.setPosDelta(hgc.curveAreaBounds, timestamp_ms);
				String statusMessage = hgm.curveSurvey.drawDeltaMeasurementGraphics();
				DataExplorer.getInstance().setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc, timestamp_ms));
				log.time(() -> "draw time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
			}

			@Override
			void drawInitialMeasurement(HistoGraphicsComposite hgc) {
				if (hgc.timeLine.getScalePositions().size() > 1) {
					int margin = hgc.curveAreaBounds.width / (hgc.timeLine.getScalePositions().size() + 1);
					long timestampMeasureNew_ms = hgc.timeLine.getAdjacentTimestamp(margin);
// long timestampDeltaNew_ms = hgc.timeLine.getAdjacentTimestamp(hgc.curveAreaBounds.width - margin);
					long timestampDeltaNew_ms = hgc.timeLine.getAdjacentTimestamp(hgc.curveAreaBounds.width * 2 / 3);

					hgc.graphicsMeasurement.drawMeasurement(timestampMeasureNew_ms, timestampDeltaNew_ms);
				}
			}
		};

		/**
		 * Set graphics window measurement mode.
		 */
		abstract void setMode(HistoGraphicsComposite graphicsComposite);

		/**
		 * Perform the UI activities required for a measurement.
		 */
		abstract void drawMeasurement(HistoGraphicsComposite hgc, long timestampMeasureNew_ms, long timestampDeltaNew_ms);

		/**
		 * Perform the UI activities for the first measurement timestamp.
		 * In case of delta measurement: the timestamp which is initially on the left.
		 */
		abstract void drawLeftMeasurement(HistoGraphicsComposite hgc, long timestamp_ms);

		/**
		 * Perform the UI activities for the second measurement timestamp used for delta measurement.
		 */
		abstract void drawRightMeasurement(HistoGraphicsComposite hgc, long timestamp_ms);

		/**
		 * Perform the UI activities in case the measurement mode is changed.
		 */
		abstract void drawInitialMeasurement(HistoGraphicsComposite hgc);

		private static String getSelectedMeasurementsAsTable(HistoGraphicsComposite hgc, long timestamp_ms) {
			hgc.recordSetComment.setFont(SWTResourceManager.getFont("Courier New", GDE.WIDGET_FONT_SIZE - 1, SWT.BOLD)); //$NON-NLS-1$
			return TrailRecordSetFormatter.getSelectedMeasurementsAsTable(timestamp_ms);
		}

	};

	boolean																isLeftMouseMeasure	= false;
	boolean																isRightMouseMeasure	= false;

	private final HistoGraphicsComposite	graphicsComposite;
	private final TrailRecord							trailRecord;
	private final CurveSurvey							curveSurvey;

	private GC														canvasGC;
	private HistoGraphicsMode							mode;

	public HistoGraphicsMeasurement(HistoGraphicsComposite graphicsComposite) {
		this.graphicsComposite = graphicsComposite;

		TrailRecordSet trailRecordSet = DataExplorer.getInstance().getHistoSet().getTrailRecordSet();
		this.trailRecord = (TrailRecord) trailRecordSet.get(trailRecordSet.getRecordKeyMeasurement());

		this.curveSurvey = new CurveSurvey(this.canvasGC, this.trailRecord, graphicsComposite.timeLine);
	}

	/**
	 * Draw a refreshed measurement.
	 */
	public void drawMeasurement() {
		TrailRecordSet trailRecordSet = DataExplorer.getInstance().getHistoSet().getTrailRecordSet();
		if (trailRecordSet.isSurveyMode(trailRecordSet.getRecordKeyMeasurement())) {
			drawMeasurement(this.curveSurvey.getTimestampMeasure_ms(), this.curveSurvey.getTimestampDelta_ms());
		}
	}

	/**
	 * Draw a new measurement based on the timestamp values.
	 */
	private void drawMeasurement(long timestampMeasureNew_ms, long timestampDeltaNew_ms) {
		Image canvasImage = this.graphicsComposite.canvasImage;
		// set the gc properties
		this.canvasGC = new GC(this.graphicsComposite.graphicCanvas);
		this.canvasGC.setForeground(this.trailRecord.getColor());

		this.curveSurvey.setCanvasGC(this.canvasGC);
		// all obsolete lines are cleaned up now draw new position marker
		this.curveSurvey.cleanMeasurementPointer(canvasImage);

		this.mode.drawMeasurement(this.graphicsComposite, timestampMeasureNew_ms, timestampDeltaNew_ms);

		this.canvasGC.dispose();
	}

	/**
	 * Reset the graphic area and status message.
	 */
	public void cleanMeasurement() {
		this.isLeftMouseMeasure = false;
		this.isRightMouseMeasure = false;

		this.canvasGC = new GC(this.graphicsComposite.graphicCanvas);
		this.curveSurvey.setCanvasGC(this.canvasGC);

		this.curveSurvey.cleanMeasurementPointer(this.graphicsComposite.canvasImage);

		this.graphicsComposite.setRecordSetCommentStandard();
		DataExplorer.getInstance().setStatusMessage(GDE.STRING_EMPTY);
	}

	/**
	 * Switch measurement mode.
	 */
	public void setModeState(HistoGraphicsMode mode) {
		this.mode = mode;
		mode.setMode(this.graphicsComposite);
	}

	/**
	 * Draw the survey graphics while moving the vertical line.
	 */
	public void processMouseDownMove(long timestamp_ms) {
// , this.canvasImage, this.curveAreaBounds
		this.canvasGC = new GC(this.graphicsComposite.graphicCanvas);
		this.curveSurvey.setCanvasGC(this.canvasGC);
		this.canvasGC.setForeground(this.trailRecord.getColor());

		if (this.isLeftMouseMeasure) {
			int yPosMeasureNew = new HistoGraphicsMapper(this.trailRecord).getVerticalDisplayPos(this.trailRecord.getParentTrail().getIndex(timestamp_ms));
			if (this.curveSurvey.isNewMeasureSpot(timestamp_ms, yPosMeasureNew)) {
				this.mode.drawLeftMeasurement(this.graphicsComposite, timestamp_ms);
			}
		} else if (this.isRightMouseMeasure) {
			int yPosDeltaNew = new HistoGraphicsMapper(this.trailRecord).getVerticalDisplayPos(this.trailRecord.getParentTrail().getIndex(timestamp_ms));
			if (this.curveSurvey.isNewDeltaSpot(timestamp_ms, yPosDeltaNew)) {
				this.mode.drawRightMeasurement(this.graphicsComposite, timestamp_ms);
			}
		}

		this.canvasGC.dispose();
	}

	/**
	 * Perform UI activities at mouse up movements.
	 */
	public void processMouseUpMove(int xPos) {
		if (this.curveSurvey.isOverVerticalLine(xPos)) {
			this.graphicsComposite.graphicCanvas.setCursor(SWTResourceManager.getCursor("gde/resource/MoveH.gif")); //$NON-NLS-1$
		} else {
			this.graphicsComposite.graphicCanvas.setCursor(DataExplorer.getInstance().getCursor());
		}
	}

	/**
	 * Determine which vertical line was moved.
	 */
	public void processMouseDownAction(int xPos) {
		if (this.curveSurvey.isOverVerticalLine(xPos)) {
			if (this.curveSurvey.isNearMeasureLine(xPos)) {
				this.isLeftMouseMeasure = true;
				this.isRightMouseMeasure = false;
			} else if (this.curveSurvey.isNearDeltaLine(xPos)) {
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
	public void processMouseUpAction() {
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
