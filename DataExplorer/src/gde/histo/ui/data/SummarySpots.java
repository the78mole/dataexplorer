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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

import gde.config.Settings;
import gde.histo.datasources.HistoSet;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.utils.MathUtils;

/**
 * Graph element data belonging to a record row in the summary graph.
 * xPos/yPos values define the lower left corner of a 2x2 pixel element.
 * @author Thomas Eickert (USER)
 */
public class SummarySpots { // MarkerLine + Boxplot + Warnings
	private static final String	$CLASS_NAME					= SummarySpots.class.getName();
	private static final Logger	log									= Logger.getLogger($CLASS_NAME);

	/**
	 * Type of the outlier.
	 */
	public enum OutlierWarning {
		FAR, CLOSE
	}

	public enum Density {
		EXTREME(2), HIGH(3), MEDIUM(4), LOW(5);

		private final Settings			settings					= Settings.getInstance();

		private final int						distanceThreshold	= 65;											// number of pixels for comparison with the average pixel distance

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

		public static Density toDensity(int drawAreaWidth, int markerNumber) {
			Density density;
			int convenientDistance = (int) (0. + drawAreaWidth / markerNumber);
			// use the box size as a standard of comparison
			if (convenientDistance > Density.LOW.getThresholdDistance())
				density = Density.LOW;
			else if (convenientDistance > Density.MEDIUM.getThresholdDistance())
				density = Density.MEDIUM;
			else if (convenientDistance > Density.HIGH.getThresholdDistance())
				density = Density.HIGH;
			else
				density = Density.EXTREME;
			log.finest(() -> String.format("density=%s  convenientDistance=%d  thresholdDistance=%d  elementWidth=%d", //$NON-NLS-1$
					density, convenientDistance, density.getThresholdDistance(), density.markerWidth));
			return density;
		}

		private int getThresholdDistance() {
			double symmetricItemValue = markerWidth - avg();
			return (int) ((1 + symmetricItemValue) * distanceThreshold / (1 + settings.getBoxplotScaleOrdinal()));
		}
	}

	public static List<Integer> defineGrid(TrailRecordSet recordSet) {
		List<Integer> grid = new ArrayList<>();
		int stripNetX0 = recordSet.getDrawAreaBounds().x + Density.MEDIUM.markerWidth / 2;
		int tmpWidth = recordSet.getDrawAreaBounds().width - Density.MEDIUM.markerWidth; // half left and right gap for overlapping elements
		double xStep = tmpWidth / 10.;
		for (int i = 1; i < 10; i++) {
			grid.add((stripNetX0 + (int) (xStep * i)));
		}
		return grid;
	}

	private final TrailRecord										record;

	private int																	elementWidth;
	private int																	stripHeight;

	/**
	 * The markers at the x position.
	 * Key is the x axis position with a distance defined by the element size.
	 */
	private final TreeMap<Integer, PosMarkers>	xPositions		= new TreeMap<>();
	private int																	stripNetX0;											// start pos for the first marker
	private int																	stripNetWidth;									// relative start pos for the LAST marker

	private double															xValueScaleFactor;
	private double															xValueOffset;
	private double															xPointScaleFactor;
	private double															xPointOffset;

	private OutlierWarning[]										minMaxWarning	= null;

	public SummarySpots(TrailRecord record) {
		this.record = record;
	}

	public void initialize(int newStripHeight, Density newDensity) {
		clear();

		stripHeight = newStripHeight;
		elementWidth = newDensity.markerWidth;

		Rectangle drawAreaBounds = record.getParentTrail().getDrawAreaBounds();
		// elements
		stripNetX0 = drawAreaBounds.x + elementWidth / 2;
		int tmpWidth = drawAreaBounds.width - elementWidth; // half left and right gap for overlapping elements
		stripNetWidth = tmpWidth - tmpWidth % elementWidth; // additional right gap because of x position delta (is the elements size)

		double decodedScaleMin = defineDecodedScaleMin();
		double decodedScaleMax = defineDecodedScaleMax();

		xValueScaleFactor = stripNetWidth / (decodedScaleMax - decodedScaleMin);
		xValueOffset = decodedScaleMin * xValueScaleFactor - .5;

		xPointScaleFactor = HistoSet.decodeDeltaValue(record, 1. / 1000.) / ((decodedScaleMax - decodedScaleMin) / stripNetWidth);
		xPointOffset = HistoSet.encodeVaultValue(record, decodedScaleMin) * 1000. * xPointScaleFactor - .5;
	}

	/**
	 * @param value is a real measurement value
	 * @return the x axis position
	 */
	public int getXPos(double value) {
		return (int) (value * xValueScaleFactor - xValueOffset);
	}

	/**
	 * @param point is the record point value
	 * @return the x axis position
	 */
	public int getXPos(Integer point) {
		return (int) (point * xPointScaleFactor - xPointOffset);
	}

	public void clear() {
		xPositions.clear();
		minMaxWarning = null;
	}

	public OutlierWarning[] getMinMaxWarning() {
		if (minMaxWarning == null) {
			minMaxWarning = record.defineMinMaxWarning(Settings.getInstance().getWarningCount());
		}
		return minMaxWarning;
	}

	/**
	 * Rebuild from record or suite master record data.
	 * @return the list of marker objects which holds all marker positions on the x axis
	 */
	private TreeMap<Integer, PosMarkers> defineXPositions(int limit) {
		log.finest(() -> record.getName());
		TreeMap<Integer, PosMarkers> resultXPositions = new TreeMap<>();

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
				int xPos = getXPos(point);
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

	public int[] defineTukeyXPositions() {
		double[] tukeyBoxPlot = record.getQuantile().getTukeyBoxPlot();
		int[] resultXPositions = new int[tukeyBoxPlot.length];
		for (int i = 0; i < tukeyBoxPlot.length; i++) {
			int xPos = getXPos(tukeyBoxPlot[i]);
			resultXPositions[i] = xPos;
		}
		log.finer(() -> Arrays.toString(resultXPositions));
		return resultXPositions;
	}

	public void drawRecentMarkers(GC gc, int stripY0) {
		drawMarkers(gc, defineXPositions(Settings.getInstance().getWarningCount()), stripY0, DataExplorer.COLOR_RED);
	}

	public void drawAllMarkers(GC gc, int stripY0) {
		drawMarkers(gc, getXPositions(), stripY0, DataExplorer.COLOR_GREY);
	}

	private void drawMarkers(GC gc, TreeMap<Integer, PosMarkers> tmpXPositions, int stripY0, Color color) {
		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setForeground(color);
		gc.setBackground(color);

		int[] tukeyXPositions = defineTukeyXPositions();
		for (Map.Entry<Integer, PosMarkers> xEntry : tmpXPositions.entrySet()) {
			int lowerLimit = tukeyXPositions[LOWER_WHISKER.ordinal()] / elementWidth * elementWidth; // floor
			int upperLimit = (tukeyXPositions[UPPER_WHISKER.ordinal()] / elementWidth + 1) * elementWidth; // ceiling
			int actualWidth = xEntry.getKey() < lowerLimit || xEntry.getKey() > upperLimit ? elementWidth * 2 : elementWidth;

			int xPosOffset = record.getParentTrail().getDrawAreaBounds().x - actualWidth / 2 + 1;
			int yPosOffset = stripY0 - actualWidth / 2 + 1;
			for (Integer yPos : xEntry.getValue().yPositions) {
				gc.fillRectangle(xEntry.getKey() + xPosOffset, yPos + yPosOffset, actualWidth, actualWidth);
				if (log.isLoggable(FINEST)) log.log(FINEST, String.format("x=%d y=%d", xEntry.getKey() + xPosOffset, yPos + yPosOffset));
			}
		}
	}

	public double defineDecodedScaleMin() { // todo consider caching
		log.finer(() -> "'" + record.getName() + "'  syncSummaryMin=" + record.getSyncSummaryMin() + " syncSummaryMax=" + record.getSyncSummaryMax());
		return MathUtils.floorStepwise(record.getSyncSummaryMin(), record.getSyncSummaryMax() - record.getSyncSummaryMin());
	}

	public double defineDecodedScaleMax() { // todo consider caching
		return MathUtils.ceilStepwise(record.getSyncSummaryMax(), record.getSyncSummaryMax() - record.getSyncSummaryMin());
	}

	public int getStripHeight() {
		return stripHeight;
	}

	public int getElementWidth() {
		return elementWidth;
	}

	public TreeMap<Integer, PosMarkers> getXPositions() {
		if (xPositions.isEmpty()) xPositions.putAll(defineXPositions(-1));
		return xPositions;
	}

	private List<Integer> getSnappedIndexes(int stripXPos) {
		int index = stripXPos - stripXPos % elementWidth;
		PosMarkers posMarkers = xPositions.get(index);
		log.log(Level.FINER, "stripXPos=", stripXPos);
		return posMarkers != null ? posMarkers : new ArrayList<>();
	}

	/**
	 * @param stripXPos is the 0-based x axis position on the record drawing strip area
	 * @param stripYPos is the 0-based x axis position on the record drawing strip area (top is zero)
	 * @return the recordset timestamp indices of the log data identified by the mouse position or an empty list
	 */
	public List<Integer> getSnappedIndexes(int stripXPos, int stripYPos) {
		if (stripYPos < 0 || stripYPos > stripHeight) return new ArrayList<>();
		if (getXPositions().isEmpty()) return new ArrayList<>(); // either spots not initialized or no markers

		List<Integer> indexes = getSnappedIndexes(stripXPos);
		if (indexes.size() <= 1) {
			return indexes;
		} else {
			int maxAdditionalMarker = (stripHeight - elementWidth) / 2 / (elementWidth + 1) * 2;
			if (maxAdditionalMarker == 0 || indexes.size() > maxAdditionalMarker + 1) {
				return indexes;
			} else {
				// transform stripYPos into a scale relative to the markers area
				int markersHeight = (elementWidth + 1) * (maxAdditionalMarker + 1);
				int markersYPos = stripYPos - ((stripHeight - markersHeight) / 2);
				if (markersYPos >= 0 && markersYPos < markersHeight) {
					// determine the markers index which the cursor is pointing to (relative position is 0-based)
					int relativePosition = markersYPos / (elementWidth + 1);
					int alternatingPosition = maxAdditionalMarker / 2 - relativePosition;
					log.finest(() -> "stripYPos=" + stripYPos + "  markersYPos=" + markersYPos + "  elementWidth=" + elementWidth + "  relativePosition=" + relativePosition + "  alternatingPosition=" + alternatingPosition);
					int index = alternatingPosition > 0 ? alternatingPosition * 2 - 1 : -alternatingPosition * 2;
					if (index >= indexes.size()) { // the vertical space is not filled with markers
						return new ArrayList<>();
					} else {
						log.finest(() -> "alternatingPosition=" + alternatingPosition + "  index=" + index);
						return new ArrayList<>(Arrays.asList(new Integer[] { indexes.get(index) }));
					}
				} else {
					return new ArrayList<>();
				}
			}
		}
	}

	/**
	 * Graph elements for an x axis position in the summary record row.
	 * The value is the record's data point index in order to get a back reference to the vault data.
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
			halfDrawingHeight = (stripHeight - 1) / 2; // leave at least one pixel at the top of the strip
		}

		/**
		 * @param index is the record's data point index
		 */
		@Override
		public boolean add(Integer index) {
			yPositions.add(-nextRelativeYPos + halfDrawingHeight + 0); // add 0 pixel for a little shift to the bottom

			// the next position is one step towards the outer border and alternates from the lower half to the upper half
			if ((size() - 1) % 2 == 0 && nextRelativeYPos != 0) {
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
			return super.add(index);
		}

	}
}