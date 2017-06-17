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
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.histo.datasources.HistoSet;
import gde.histo.recordings.HistoGraphicsMapper;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.recordings.TrailRecordSetFormatter;
import gde.histo.utils.HistoTimeLine;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

/**
 * Curve survey activity mapper for mouse movements and other UI actions like menus, checkboxes.
 * @author Thomas Eickert (USER)
 */
public final class HistoGraphicsMeasurement {
	private final static String	$CLASS_NAME	= HistoGraphicsComposite.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	private final DataExplorer	application	= DataExplorer.getInstance();
	private final HistoSet			histoSet		= HistoSet.getInstance();

	public enum HistoGraphicsMode {
		MEASURE, MEASURE_DELTA, RESET
	};

	boolean										isLeftMouseMeasure	= false;
	boolean										isRightMouseMeasure	= false;

	private final TrailRecord	trailRecord;
	private final CurveSurvey	curveSurvey;
	private final boolean			isSingleMeasurement;

	private final Canvas			graphicCanvas;
	private final Text				recordSetComment;

	private GC								canvasGC;

	public HistoGraphicsMeasurement(Canvas graphicCanvas, Text recordSetComment, HistoTimeLine timeLine) {
		this.graphicCanvas = graphicCanvas;

		TrailRecordSet trailRecordSet = this.histoSet.getTrailRecordSet();
		this.trailRecord = (TrailRecord) trailRecordSet.get(trailRecordSet.getRecordKeyMeasurement());
		this.curveSurvey = new CurveSurvey(this.canvasGC, this.trailRecord, timeLine);
		this.isSingleMeasurement = trailRecordSet.isMeasurementMode(trailRecordSet.getRecordKeyMeasurement());

		this.recordSetComment = recordSetComment;
	}

	/**
	 * Draw a refreshed measurement.
	 */
	public void drawMeasurement(Image canvasImage, Rectangle curveAreaBounds) {
		drawMeasurement(this.curveSurvey.getTimestampMeasure_ms(), this.curveSurvey.getTimestampDelta_ms(), canvasImage, curveAreaBounds);
	}

	/**
	 * Draw a new measurement based on the timestamp values.
	 */
	public void drawMeasurement(long timestampMeasureNew_ms, long timestampDeltaNew_ms, Image canvasImage, Rectangle curveAreaBounds) {
		// set the gc properties
		this.canvasGC = new GC(this.graphicCanvas);
		this.canvasGC.setForeground(this.trailRecord.getColor());

		this.curveSurvey.setCanvasGC(this.canvasGC);
		// all obsolete lines are cleaned up now draw new position marker
		this.curveSurvey.cleanMeasurementPointer(canvasImage);

		if (this.isSingleMeasurement) {
			setModeState(HistoGraphicsMode.MEASURE);
			this.curveSurvey.setPosMeasure(curveAreaBounds, timestampMeasureNew_ms);

			String statusMessage = this.curveSurvey.drawMeasurementGraphics();
			this.application.setStatusMessage(statusMessage);

			this.recordSetComment.setText(this.getSelectedMeasurementsAsTable(timestampMeasureNew_ms));
		}
		else {
			setModeState(HistoGraphicsMode.MEASURE_DELTA);
			this.curveSurvey.setPosMeasure(curveAreaBounds, timestampMeasureNew_ms);
			this.curveSurvey.setPosDelta(curveAreaBounds, timestampDeltaNew_ms);

			String statusMessage = this.curveSurvey.drawDeltaMeasurementGraphics();
			this.application.setStatusMessage(statusMessage);

			this.recordSetComment.setText(this.getSelectedMeasurementsAsTable(timestampMeasureNew_ms));
		}

		this.canvasGC.dispose();
	}

	/**
	 * Clean (old) measurement pointer - check pointer in curve area.
	 */
	public void cleanMeasurementPointer(Image canvasImage) {
		this.canvasGC = new GC(this.graphicCanvas);
		this.curveSurvey.setCanvasGC(this.canvasGC);

		this.curveSurvey.cleanMeasurementPointer(canvasImage);
	}

	/**
	 * Switch measurement mode.
	 * @param mode
	 */
	public void setModeState(HistoGraphicsMode mode) {
		switch (mode) {
		case MEASURE:
			this.isLeftMouseMeasure = true;
			this.isRightMouseMeasure = false;
			break;
		case MEASURE_DELTA:
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = true;
			break;
		default:
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			break;
		}
	}

	/**
	 * Draw the survey graphics.
	 * @param timestamp_ms
	 * @param canvasImage
	 */
	public void processMouseMove(long timestamp_ms, Image canvasImage, Rectangle curveAreaBounds) {
		this.canvasGC = new GC(this.graphicCanvas);
		this.curveSurvey.setCanvasGC(this.canvasGC);
		this.canvasGC.setForeground(this.trailRecord.getColor());

		if (this.isLeftMouseMeasure) {
			int yPosMeasureNew = new HistoGraphicsMapper(this.trailRecord).getVerticalDisplayPos(this.trailRecord.getParentTrail().getIndex(timestamp_ms));
			if (this.curveSurvey.isNewMeasureSpot(timestamp_ms, yPosMeasureNew)) {
				long startTime = new Date().getTime();
				this.curveSurvey.cleanMeasurementPointer(canvasImage);

				if (this.isSingleMeasurement) {
					this.curveSurvey.setPosMeasure(curveAreaBounds, timestamp_ms);
					String statusMessage = this.curveSurvey.drawMeasurementGraphics();
					this.application.setStatusMessage(statusMessage);
				}
				else {
					this.curveSurvey.setPosMeasure(curveAreaBounds, timestamp_ms);
					String statusMessage = this.curveSurvey.drawDeltaMeasurementGraphics();
					this.application.setStatusMessage(statusMessage);
				}

				this.recordSetComment.setText(this.getSelectedMeasurementsAsTable(timestamp_ms));
				if (log.isLoggable(Level.TIME)) log.log(Level.TIME, "draw time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
			}
			this.canvasGC.dispose();
		}
		else if (this.isRightMouseMeasure) {
			int yPosDeltaNew = new HistoGraphicsMapper(this.trailRecord).getVerticalDisplayPos(this.trailRecord.getParentTrail().getIndex(timestamp_ms));
			if (this.curveSurvey.isNewDeltaSpot(timestamp_ms, yPosDeltaNew)) {
				long startTime = new Date().getTime();
				this.curveSurvey.cleanMeasurementPointer(canvasImage);

				this.curveSurvey.setPosDelta(curveAreaBounds, timestamp_ms);
				String statusMessage = this.curveSurvey.drawDeltaMeasurementGraphics();
				this.application.setStatusMessage(statusMessage);

				this.recordSetComment.setText(this.getSelectedMeasurementsAsTable(timestamp_ms));
				if (log.isLoggable(Level.TIME)) log.log(Level.TIME, "draw time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
			}
		}

		this.canvasGC.dispose();
	}

	/**
	 * Determine which vertical line was moved.
	 * @param xPos
	 */
	public void processMouseDownAction(int xPos) {
		if (this.curveSurvey.isNearMeasureLine(xPos)) {
			this.isLeftMouseMeasure = true;
			this.isRightMouseMeasure = false;
		}
		else if (this.curveSurvey.isNearDeltaLine(xPos)) {
			this.isRightMouseMeasure = true;
			this.isLeftMouseMeasure = false;
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Reset the identified vertical line.
	 */
	public void processMouseUpAction() {
		if (this.isLeftMouseMeasure) {
			this.isLeftMouseMeasure = false;
			// application.setStatusMessage(GDE.STRING_EMPTY);
		}
		else if (this.isRightMouseMeasure) {
			this.isRightMouseMeasure = false;
			// application.setStatusMessage(GDE.STRING_EMPTY);
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "isMouseMeasure = " + this.isLeftMouseMeasure + " isMouseDeltaMeasure = " + this.isRightMouseMeasure); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String getSelectedMeasurementsAsTable(long timestamp_ms) {
		this.recordSetComment.setFont(SWTResourceManager.getFont("Courier New", GDE.WIDGET_FONT_SIZE - 1, SWT.BOLD)); //$NON-NLS-1$
		return TrailRecordSetFormatter.getSelectedMeasurementsAsTable(timestamp_ms);
	}

	public boolean isOverVerticalLine(int xPos) {
		return this.curveSurvey.isOverVerticalLine(xPos);
	}

}
