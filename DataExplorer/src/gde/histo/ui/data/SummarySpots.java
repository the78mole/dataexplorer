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

package gde.histo.ui.data;

import static gde.histo.utils.UniversalQuantile.BoxplotItems.LOWER_WHISKER;
import static gde.histo.utils.UniversalQuantile.BoxplotItems.UPPER_WHISKER;
import static java.util.logging.Level.FINEST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

import gde.config.Settings;
import gde.histo.datasources.HistoSet;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.ui.data.SummarySpots.MarkerLine;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.utils.MathUtils;

/**
 * Graph element data belonging to the summary graph.
 * Corresponds to the recordset level.
 * Key is the record ordinal number.
 * xPos/yPos values define the lower left corner of a 2x2 pixel element (or mid left of an uneven sized element)
 * @author Thomas Eickert (USER)
 */
public class SummarySpots extends HashMap<Integer, MarkerLine> {
	private static final String		$CLASS_NAME				= SummarySpots.class.getName();
	private static final Logger		log								= Logger.getLogger($CLASS_NAME);
	private static final long			serialVersionUID	= -1640475554952161258L;

	private final int							elementWidth;
	private final TrailRecordSet	recordSet;

	private int										fixedCanvasHeight;

	enum Density {
		EXTREME(2), HIGH(3), MEDIUM(4), LOW(5);

		private final Settings			settings					= Settings.getInstance();

		private final int						distanceThreshold	= 30;											// number of pixels for comparison with the average pixel distance

		private final int						markerWidth;																// in pixel

		/** use this to avoid repeatedly cloning actions instead of values() */
		public static final Density	VALUES[]					= values();

		private Density(int markerWidth) {
			this.markerWidth = markerWidth;
		}

		private static double avg() {
			return (VALUES[VALUES.length - 1].markerWidth - VALUES[0].markerWidth) / 2.;
		}

		public static Density fromOrdinal(int ordinal) {
			return Density.VALUES[ordinal];
		}

		public static String toString(Density density) {
			return density.name();
		}

		public int getThresholdDistance() {
			double symmetricItemValue = this.markerWidth - avg();
			return (int) ((1 + symmetricItemValue) * this.distanceThreshold / (1 + this.settings.getBoxplotScaleOrdinal()));
		}
	}

	/**
	 * Graph element data belonging to a record row in the summary graph.
	 * Key is the x axis position.
	 */
	public class MarkerLine {
		private static final long										serialVersionUID	= 224357427594872383L;

		@SuppressWarnings("hiding")
		private final Logger												log								= Logger.getLogger(MarkerLine.class.getName());

		private final TrailRecord										record;
		private final int														displayRank;

		private final TreeMap<Integer, PosMarkers>	xPositions				= new TreeMap<>();
		private final int														stripX0;																													// start pos for the first marker
		private final int														stripWidth;																												// start pos for the LAST marker
		private final int														stripY0;																													// start pos for the drawing strip
		private final int														stripHeight;

		MarkerLine(TrailRecord record, int displayRank) {
			this.record = record;
			this.displayRank = displayRank;

			this.stripHeight = (fixedCanvasHeight + elementWidth / 2) / recordSet.getDisplayRecords().size();
			int tmpWidth = recordSet.getDrawAreaBounds().width - elementWidth - elementWidth % 2; // left and right gap for overlapping elements
			this.stripWidth = tmpWidth - tmpWidth % elementWidth; // right gap for overlapping elements
			this.stripX0 = recordSet.getDrawAreaBounds().x + elementWidth / 2;
			this.stripY0 = recordSet.getDrawAreaBounds().y + stripHeight * displayRank + 1;
		}

		/**
		 * Rebuild from record or suite master record data.
		 * @return
		 */
		private TreeMap<Integer, PosMarkers> defineXPositions(int limit) {
			log.finest(() -> record.getName());
			TreeMap<Integer, PosMarkers> resultXPositions = new TreeMap<>();
			int pointOffset = definePointOffset();
			double scaleFactor = defineScaleFactor();
			double xOffset = pointOffset * scaleFactor - .5;

			final Vector<Integer> tmpRecord;
			if (record.getTrailSelector().isTrailSuite()) {
				tmpRecord = record.getSuiteRecords().get(record.getTrailSelector().getTrailType().getSuiteMasterIndex());
			} else {
				tmpRecord = record;
			}

			int actualLimit = limit > 0 && limit < tmpRecord.size() ? limit : tmpRecord.size();
			for (int i = 0; i < actualLimit; i++) {
				Integer point = tmpRecord.get(i);
				if (point != null) {
					int xPos = (int) (point * scaleFactor - xOffset);
					int xDrawer = xPos - xPos % elementWidth;
					log.finest(() -> "xPos=" + xPos);
					PosMarkers posMarkers = resultXPositions.get(xDrawer);
					if (posMarkers == null) {
						posMarkers = new PosMarkers(stripHeight);
						resultXPositions.put(xDrawer, posMarkers);
					}
					posMarkers.add(i);
				}
			}
			return resultXPositions;
		}

		private int[] defineTukeyXPositions() {
			double pointOffset = defineDecodedScaleMin();
			double scaleFactor = stripWidth / (defineDecodedScaleMax() - defineDecodedScaleMin());
			double xOffset = pointOffset * scaleFactor - .5;

			double[] tukeyBoxPlot = record.getQuantile().getTukeyBoxPlot();
			int[] resultXPositions = new int[tukeyBoxPlot.length];
			for (int i = 0; i < tukeyBoxPlot.length; i++) {
				int xPos = (int) (tukeyBoxPlot[i] * scaleFactor - xOffset);
				int xDrawer = xPos - xPos % elementWidth;
				resultXPositions[i] = xDrawer;
			}
			log.finer(() -> Arrays.toString(resultXPositions));
			return resultXPositions;
		}

		public List<Integer> defineGrid() {
			List<Integer> grid = new ArrayList<>();
			double xStep = stripWidth / 10.;
			for (int i = 1; i < 10; i++) {
				grid.add((stripX0 + (int) (xStep * i)));
			}
			return grid;
		}

		public void drawRecentMarkers(GC gc) {
			drawMarkers(gc, defineXPositions(3), DataExplorer.COLOR_RED);
		}

		public void drawAllMarkers(GC gc) {
			drawMarkers(gc, getXPositions(), DataExplorer.COLOR_GREY);
		}

		private void drawMarkers(GC gc, TreeMap<Integer, PosMarkers> tmpXPositions, Color color) {
			gc.setLineWidth(1);
			gc.setLineStyle(SWT.LINE_SOLID);
			gc.setForeground(color);
			gc.setBackground(color);

			int[] tukeyXPositions = defineTukeyXPositions();
			double lowerXPos = tukeyXPositions[LOWER_WHISKER.ordinal()];
			double upperXPos = tukeyXPositions[UPPER_WHISKER.ordinal()];
			for (Map.Entry<Integer, PosMarkers> xEntry : tmpXPositions.entrySet()) {
				int actualWidth = xEntry.getKey() < lowerXPos || xEntry.getKey() > upperXPos ? elementWidth * 2 : elementWidth;
				for (Integer yPos : xEntry.getValue().yPositions) {
					gc.fillRectangle(stripX0 + xEntry.getKey(), stripY0 + yPos - actualWidth / 2 + 1, actualWidth, actualWidth);
					if (log.isLoggable(FINEST)) log.log(FINEST, String.format("x=%d y=%d", stripX0 + xEntry.getKey(), stripY0 + yPos));
				}
			}
		}

		public double defineDecodedScaleMin() { // todo consider caching
			log.finer(() -> "'" + this.record.getName() + "'  syncSummaryMin=" + record.getSyncSummaryMin() + " syncSummaryMax=" + record.getSyncSummaryMax());
			return MathUtils.floorStepwise(record.getSyncSummaryMin(), record.getSyncSummaryMax() - record.getSyncSummaryMin());
		}

		public double defineDecodedScaleMax() { // todo consider caching
			return MathUtils.ceilStepwise(record.getSyncSummaryMax(), record.getSyncSummaryMax() - record.getSyncSummaryMin());
		}

		private int definePointOffset() {
			return (int) (HistoSet.encodeVaultValue(record, defineDecodedScaleMin()) * 1000.);
		}

		private double defineScaleFactor() {
			return record.getFactor() / ((defineDecodedScaleMax() - defineDecodedScaleMin()) / stripWidth) / 1000.;
		}

		public int getStripHeight() {
			return this.stripHeight;
		}

		public TreeMap<Integer, PosMarkers> getXPositions() {
			if (xPositions.isEmpty()) xPositions.putAll(defineXPositions(-1));
			return xPositions;
		}

	}

	/**
	 * Graph elements for an x axis position in the summary record row.
	 */
	public class PosMarkers extends ArrayList<Integer> {
		private static final long		serialVersionUID	= 5955746265581640576L;

		@SuppressWarnings("hiding")
		private final Logger				log								= Logger.getLogger(PosMarkers.class.getName());

		private final List<Integer>	yPositions				= new ArrayList<>();

		private final int						yStep							= elementWidth + 1;

		private final int						halfDrawingHeight;

		private int									cycleNumber				= 0;																						// number of fully populated element columns
		private int									nextRelativeYPos	= 0;																						// lower corner of a 2x2 element

		PosMarkers(int stripHeight) {
			this.halfDrawingHeight = (stripHeight - 1) / 2; // leave at least one pixel at the top of the strip
		}

		@Override
		public boolean add(Integer e) {
			yPositions.add(-nextRelativeYPos + halfDrawingHeight + 1); // add 1 pixel for a little shift to the top

			// the next position is one step towards the outer border and alternates from the lower half to the upper half
			if (this.size() % 2 == 0) {
				nextRelativeYPos = -Math.abs(nextRelativeYPos); // lower half
			} else {
				nextRelativeYPos = Math.abs(nextRelativeYPos) + yStep; // upper half
			}
			// check if the next element exceeds the upper half border
			if (nextRelativeYPos > halfDrawingHeight - elementWidth / 2) {
				cycleNumber += 1;
				// shift the next position downwards from the center line or start at the center line again
				nextRelativeYPos = yStep - cycleNumber % yStep - 1;
			}
			log.log(Level.FINEST, "" + size(), String.format(" cycleNumber=%d nextRelativeYPos=%d", cycleNumber, nextRelativeYPos));
			return super.add(e);
		}
	}

	public SummarySpots(TrailRecordSet recordSet, int fixedCanvasHeight) {
		super();
		this.recordSet = recordSet;
		this.fixedCanvasHeight = fixedCanvasHeight;

		this.elementWidth = defineDensity();

		for (int i = 0; i < recordSet.getDisplayRecords().size(); i++) {
			TrailRecord record = recordSet.getDisplayRecords().get(i);
			log.fine(() -> String.format("record=%s  isVisible=%b isDisplayable=%b isScaleVisible=%b", record.getName(), record.isVisible(), record.isDisplayable(), record.isScaleSynced(), record.isScaleVisible()));

			put(record.getOrdinal(), new MarkerLine(record, i));
		}

	}

	private int defineDensity() {
		Density density;
		int convenientDistance = (int) (0. + recordSet.getDrawAreaBounds().width / recordSet.getTimeStepSize());
		// use the box size as a standard of comparison
		if (convenientDistance > Density.LOW.getThresholdDistance())
			density = Density.LOW;
		else if (convenientDistance > Density.MEDIUM.getThresholdDistance())
			density = Density.MEDIUM;
		else if (convenientDistance > Density.HIGH.getThresholdDistance())
			density = Density.HIGH;
		else
			density = Density.EXTREME;
		log.finer(() -> String.format("density=%s  convenientDistance=%d  thresholdDistance=%d  elementWidth=%d", //$NON-NLS-1$
				density, convenientDistance, density.getThresholdDistance(), density.markerWidth));
		return density.markerWidth;
	}

}
