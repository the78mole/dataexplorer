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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.config.Settings;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.recordings.TrailRecordSetFormatter;
import gde.histo.ui.data.SummarySpots;
import gde.histo.ui.data.SummarySpots.Density;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

/**
 * Summary measuring activity mapper for mouse movements and other UI actions from menus, checkboxes etc.
 * @author Thomas Eickert (USER)
 */
public final class SummaryMeasuring extends AbstractMeasuring {
	private final static String	$CLASS_NAME	= SummaryMeasuring.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	public enum MeasuringMode { // todo check if implementing this class is appropriate

		/** Single measurement mode is active */
		MEASURE {
			@Override
			void setMode(SummaryMeasuring graphicsMeasuring) {
				graphicsMeasuring.isLeftMouseMeasure = true;
				graphicsMeasuring.isRightMouseMeasure = false;
			}

			@Override
			void drawMeasurement(SummaryMeasuring hgm) {
				AbstractChartComposite hgc = hgm.summaryComposite;

				String statusMessage = hgm.curveSurvey.drawMeasurementGraphics();
				hgc.windowActor.setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc.recordSetComment, hgm.measure.getTimestampMeasure_ms()));
				hgc.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0897));
			}

			@Override
			void drawLeftMeasurement(SummaryMeasuring hgm) {
				AbstractChartComposite hgc = hgm.summaryComposite;
				long startTime = new Date().getTime();
				String statusMessage = hgm.curveSurvey.drawMeasurementGraphics();
				hgc.windowActor.setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc.recordSetComment, hgm.measure.getTimestampMeasure_ms()));
				hgc.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0897));
				log.time(() -> "draw time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
			}

			@Override
			void drawRightMeasurement(SummaryMeasuring hgm) {
				// simple measure has only a left measurement
			}
		},

		/** delta measurement mode is active */
		MEASURE_DELTA {
			@Override
			void setMode(SummaryMeasuring graphicsMeasuring) {
				graphicsMeasuring.isLeftMouseMeasure = false;
				graphicsMeasuring.isRightMouseMeasure = true;
			}

			@Override
			void drawMeasurement(SummaryMeasuring hgm) {
				AbstractChartComposite hgc = hgm.summaryComposite;

				String statusMessage = hgm.curveSurvey.drawDeltaMeasurementGraphics();
				hgc.windowActor.setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc.recordSetComment, hgm.measure.getTimestampMeasure_ms()));
				hgc.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0897));
			}

			@Override
			void drawLeftMeasurement(SummaryMeasuring hgm) {
				AbstractChartComposite hgc = hgm.summaryComposite;
				long startTime = new Date().getTime();
				String statusMessage = hgm.curveSurvey.drawDeltaMeasurementGraphics();
				hgc.windowActor.setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc.recordSetComment, hgm.measure.getTimestampMeasure_ms()));
				hgc.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0897));
				log.time(() -> "draw time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
			}

			@Override
			void drawRightMeasurement(SummaryMeasuring hgm) {
				AbstractChartComposite hgc = hgm.summaryComposite;
				long startTime = new Date().getTime();
				String statusMessage = hgm.curveSurvey.drawDeltaMeasurementGraphics();
				hgc.windowActor.setStatusMessage(statusMessage);

				hgc.recordSetComment.setText(getSelectedMeasurementsAsTable(hgc.recordSetComment, hgm.measure.getTimestampMeasure_ms()));
				hgc.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0897));
				log.time(() -> "draw time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
			}
		};

		/**
		 * Set graphics window measurement mode.
		 */
		abstract void setMode(SummaryMeasuring graphicsMeasuring);

		/**
		 * Perform the UI activities required for a measurement.
		 */
		abstract void drawMeasurement(SummaryMeasuring hgm);

		/**
		 * Perform the UI activities for the first measurement timestamp.
		 * In case of delta measurement: the timestamp which is initially on the left.
		 */
		abstract void drawLeftMeasurement(SummaryMeasuring hgm);

		/**
		 * Perform the UI activities for the second measurement timestamp used for delta measurement.
		 */
		abstract void drawRightMeasurement(SummaryMeasuring hgm);

		private static String getSelectedMeasurementsAsTable(Text recordSetComment, long timestamp_ms) {
			recordSetComment.setFont(SWTResourceManager.getFont("Courier New", GDE.WIDGET_FONT_SIZE - 1, SWT.BOLD));
			return TrailRecordSetFormatter.getSelectedMeasurementsAsTable(timestamp_ms);
		}

		static MeasuringMode getMode(boolean isDeltaMeasuring) {
			if (isDeltaMeasuring) {
				return MeasuringMode.MEASURE_DELTA;
			} else {
				return MeasuringMode.MEASURE;
			}
		}
	};

	private final Settings								settings						= Settings.getInstance();

	boolean																isLeftMouseMeasure	= false;
	boolean																isRightMouseMeasure	= false;

	private final AbstractChartComposite	summaryComposite;
	private final CurveSurvey							curveSurvey;																	// todo decide if Summary Survey class is required

	private GC														canvasGC;
	private MeasuringMode									mode;

	public SummaryMeasuring(AbstractChartComposite summaryComposite, Measure measure) {
		super(measure);
		this.summaryComposite = summaryComposite;
		this.mode = MeasuringMode.getMode(measure.isDeltaMeasure);
		this.mode.setMode(this);

		this.curveSurvey = null; // todo decide about initial timestamps and curve survey
// long[] initialTimestamps_ms = this.mode.defineInitialTimestamps_ms(this.summaryComposite);
// measure.setTimestampMeasure_ms(initialTimestamps_ms[0]);
// measure.setTimestampDelta_ms(initialTimestamps_ms[1]);
//
// this.curveSurvey = new CurveSurvey(this.canvasGC, summaryComposite.getTimeLine(), measure);
// this.curveSurvey.initialize();
// this.curveSurvey.setMeasurePosition();
// this.curveSurvey.setDeltaPosition();
	}

	/**
	 * Draw a refreshed measurement.
	 */
	@Override
	public void drawMeasuring() {
		drawMeasuring(measure.getTimestampMeasure_ms(), measure.getTimestampDelta_ms());
	}

	/**
	 * Draw a new measurement based on the timestamp values.
	 */
	private void drawMeasuring(long timestampMeasureNew_ms, long timestampDeltaNew_ms) {
		// set the gc properties
		this.canvasGC = new GC(this.summaryComposite.graphicCanvas);
		this.canvasGC.setForeground(measure.measureRecord.getColor());

// this.curveSurvey.setCanvasGC(this.canvasGC);
// // all obsolete lines are cleaned up now draw new position marker
// this.curveSurvey.cleanMeasurementPointer(canvasImage);

		measure.setTimestampMeasure_ms(timestampMeasureNew_ms);
		measure.setTimestampDelta_ms(timestampDeltaNew_ms);

// curveSurvey.initialize();
// curveSurvey.setMeasurePosition();
// curveSurvey.setDeltaPosition();
		drawModeMeasurement(this.canvasGC);
		this.canvasGC.dispose();
	}

	private void drawModeMeasurement(GC canvasImageGC) { // todo move this into a new class analogous to CurveSurvey
		AbstractChartComposite hgc = summaryComposite;
		boolean isSummarySpotsVisible = settings.isSummarySpotsVisible();
		if (!isSummarySpotsVisible) return;

		boolean isPartialDataTable = settings.isPartialDataTable();
		TrailRecordSet trailRecordSet = hgc.retrieveTrailRecordSet();
		if (trailRecordSet.getDisplayRecords() == null || trailRecordSet.getDisplayRecords().isEmpty()) return; // concurrent activity

		final Density density = Density.toDensity(hgc.curveAreaBounds.width, trailRecordSet.getTimeStepSize());
		final int stripHeight = hgc.fixedCanvasHeight / trailRecordSet.getDisplayRecords().size();

		SummarySpots previousSpots = null;
		for (int i = 0; i < trailRecordSet.getDisplayRecords().size(); i++) {
			TrailRecord record = trailRecordSet.getDisplayRecords().get(i);
			Rectangle drawStripBounds = new Rectangle(hgc.curveAreaBounds.x, hgc.curveAreaBounds.y + stripHeight * i + SummaryComposite.UNK_GAP,
					hgc.curveAreaBounds.width, stripHeight);
			SummarySpots summarySpots = record.getSummary().getSummarySpots();
			summarySpots.initialize(drawStripBounds, density);

			if (record.isVisible() || !isPartialDataTable) {
				if (record.equals(hgc.measuring.measure.measureRecord)) {
					summarySpots.drawMarkers(canvasImageGC, hgc.measuring.measure);
				}
				if (previousSpots != null && settings.isCurveSurvey()) {
					summarySpots.drawConnections(canvasImageGC, previousSpots, hgc.measuring.measure);
				}
				previousSpots = summarySpots;
			}
		}
	}

	/**
	 * Reset the graphic area and comment.
	 */
	@Override
	public void cleanMeasuring() {
		this.isLeftMouseMeasure = false;
		this.isRightMouseMeasure = false;

		log.finer(() -> "canvasGC=" + canvasGC + " curveSurvey=" + curveSurvey + " height=" + summaryComposite.graphicCanvas.getBounds().height);
		// if (this.summaryComposite.graphicCanvas.getBounds().height != 0) { // canvas was not yet drawn
			if (curveSurvey != null) { // required if this chart was not yet displayed
				this.canvasGC = new GC(this.summaryComposite.graphicCanvas);
				this.curveSurvey.setCanvasGC(this.canvasGC);

				this.curveSurvey.cleanMeasurementPointer(this.summaryComposite.canvasImage);
				this.canvasGC.dispose();
				throw new UnsupportedOperationException();
			}
//		}
		this.summaryComposite.setRecordSetCommentStandard();
	}

	/**
	 *
	 */
	@Override
	public void processMouseDownMove(long timestamp_ms) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Perform UI activities at mouse up movements.
	 */
	@Override
	public void processMouseUpMove(Point point) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Determine which vertical line was moved.
	 */
	@Override
	public void processMouseDownAction(Point point) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Reset the identified vertical line.
	 */
	@Override
	public void processMouseUpAction(Point point) {
		throw new UnsupportedOperationException();
	}

}
