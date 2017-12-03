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

import static java.util.logging.Level.FINEST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import org.eclipse.swt.SWT;
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

	private final Settings				settings					= Settings.getInstance();
	private final Density					density						= Density.EXTREME;
	private final int							elementWidth			= density.boxWidth / 2;

	private final TrailRecordSet	recordSet;
	private int										fixedCanvasHeight;

	enum Density {
		EXTREME(4), HIGH(8), MEDIUM(10), LOW(16);

		private final Settings			settings					= Settings.getInstance();

		/** box may grow or shrink by this value based on log duration */
		public final int						boxWidthAmplitude	= 2;

		private final int						boxWidth;																		// in pixel

		/** use this to avoid repeatedly cloning actions instead of values() */
		public static final Density	VALUES[]					= values();

		private Density(int boxWidth) {
			this.boxWidth = boxWidth;
		}

		public static Density fromOrdinal(int ordinal) {
			return Density.VALUES[ordinal];
		}

		public static String toString(Density density) {
			return density.name();
		}

		public int getScaledBoxWidth() {
			return (int) (this.boxWidth * (1 + (this.settings.getBoxplotScaleOrdinal() - 1) / 3.));
		}
	}

	/**
	 * Graph element data belonging to a record row in the summary graph.
	 * Key is the x axis position.
	 */
	public class MarkerLine extends TreeMap<Integer, PosMarkers> {
		private static final long	serialVersionUID	= 224357427594872383L;

		private final Logger			log								= Logger.getLogger(MarkerLine.class.getName());
		private final TrailRecord	record;
		private final int					displayRank;

		private final int					stripX0;																													// start pos for the first marker
		private final int					stripWidth;																												// start pos for the LAST marker
		private final int					stripY0;																													// start pos for the drawing strip for a record
		private final int					stripHeight;

		MarkerLine(TrailRecord record, int displayRank) {
			this.record = record;
			this.displayRank = displayRank;

			this.stripHeight = (fixedCanvasHeight + elementWidth / 2) / recordSet.getDisplayRecords().size();
			this.stripWidth = recordSet.getDrawAreaBounds().width - elementWidth; // left and right gap for overlapping elements
			this.stripX0 = recordSet.getDrawAreaBounds().x + elementWidth / 2;
			this.stripY0 = recordSet.getDrawAreaBounds().y + stripHeight * displayRank + 1;

			initialize();
		}

		/**
		 * Rebuild from record data.
		 */
		private void initialize() {
			int pointOffset = definePointOffset();
			double scaleFactor = defineScaleFactor();
			double xOffset = pointOffset * scaleFactor + .5;

			for (int i = 0; i < record.size(); i++) {
				Integer point = record.get(i);
				if (point != null) {
					int xPos = (int) (point * scaleFactor - xOffset);
					log.finest(() -> "xPos=" + xPos);
					PosMarkers posMarkers = get(xPos);
					if (posMarkers == null) {
						posMarkers = new PosMarkers(stripHeight);
						put(xPos, posMarkers);
					}
					posMarkers.add(i);
				}
			}
		}

		public void drawMarkers(GC gc) {
			gc.setLineWidth(1);
			gc.setLineStyle(SWT.LINE_SOLID);
			gc.setForeground(DataExplorer.COLOR_GREY);
			gc.setBackground(DataExplorer.COLOR_GREY);

			for (Map.Entry<Integer, PosMarkers> xEntry : this.entrySet()) {
				for (Integer yPos : xEntry.getValue().yPositions) {
					gc.fillRectangle(stripX0 + xEntry.getKey(), stripY0 + yPos, elementWidth, elementWidth);
					if (log.isLoggable(FINEST)) log.log(FINEST, String.format("x=%d y=%d", stripX0 + xEntry.getKey(), stripY0 + yPos));
				}
			}
		}

		public double defineDecodedScaleMin() { // todo consider caching
			log.finer(()-> "'" + this.record.getName() + "'  syncSummaryMin=" + record.getSyncSummaryMin() + " syncSummaryMax=" + record.getSyncSummaryMax());
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

	}

	/**
	 * Graph elements for an x axis position in the summary record row.
	 */
	public class PosMarkers extends ArrayList<Integer> {
		private static final long		serialVersionUID	= 5955746265581640576L;

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
			yPositions.add(-nextRelativeYPos + halfDrawingHeight + elementWidth / 2);

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

		for (int i = 0; i < recordSet.getDisplayRecords().size(); i++) {
			TrailRecord record = recordSet.getDisplayRecords().get(i);
			log.fine(() -> String.format("record=%s  isVisible=%b isDisplayable=%b isScaleVisible=%b",
					record.getName(), record.isVisible(), record.isDisplayable(), record.isScaleSynced(), record.isScaleVisible()));

			put(record.getOrdinal(), new MarkerLine(record, i));
		}

	}

	public int determineDeltaXPos(int recordOrdinal) {
		TrailRecord record = recordSet.get(recordOrdinal);
		int deltaXPos = Integer.MAX_VALUE;
		int lastXPos = recordSet.getDrawAreaBounds().width - deltaXPos; // this will result in a huge start value for the minimum search
		for (Entry<Integer, PosMarkers> entry : get(recordOrdinal).entrySet()) {
			deltaXPos = Math.min(deltaXPos, entry.getKey() - lastXPos);
			lastXPos = entry.getKey();
		}
		log.log(Level.OFF, "min delta xPos: ", deltaXPos);
		return deltaXPos;
	}

	public int determineMaxPointsPerXPos(int recordOrdinal) {
		int maxPointsPerXPos = values().parallelStream().mapToInt(p -> p.size()).max().orElse(0);
		log.off(() -> recordSet.get(recordOrdinal).getName() + " max points per xPos: " + maxPointsPerXPos);
		return maxPointsPerXPos;
	}

}
