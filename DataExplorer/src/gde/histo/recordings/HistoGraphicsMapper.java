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

package gde.histo.recordings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.graphics.Point;

import gde.device.IDevice;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.HistoTimeLine;

/**
 * Record data mapping for the histo graphics tab.
 * @author Thomas Eickert (USER)
 */
public final class HistoGraphicsMapper {
	private final static String			$CLASS_NAME							= TrailSelector.class.getName();
	private final static Logger			log											= Logger.getLogger($CLASS_NAME);

	private final IDevice						device									= DataExplorer.application.getActiveDevice();

	private final TrailRecord				trailRecord;

	public HistoGraphicsMapper(TrailRecord trailRecord) {
		this.trailRecord = trailRecord;
	}

	/**
	 * Query the values for display.
	 * Supports multiple entries for the same x axis position.
	 * @param timeLine
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @return point time, value; null if the trail record value is null
	 */
	public Point[] getDisplayPoints(HistoTimeLine timeLine, int xDisplayOffset, int yDisplayOffset) {
		List<Point> points = new ArrayList<>();
		double tmpOffset = this.trailRecord.getMinDisplayValue() * 1 / this.trailRecord.getSyncMasterFactor();
		for (int i = 0; i < this.trailRecord.realSize(); i++) {
			if (this.trailRecord.elementAt(i) != null) {
				points.add(new Point(xDisplayOffset + timeLine.getScalePositions().get((long) this.trailRecord.getParentTrail().getTime_ms(i)),
						yDisplayOffset - (int) (((this.trailRecord.elementAt(i) / 1000.0) - tmpOffset) * this.trailRecord.getDisplayScaleFactorValue())));
			}
			else {
				points.add(null);
			}
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, Arrays.toString(points.toArray()));
		return points.toArray(new Point[0]);
	}

	/**
	 * Query the values for display.
	 * Supports multiple entries for the same x axis position.
	 * @param timeLine
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @return point time, value; null if the trail record value is null
	 */
	public List<Point[]> getSuiteDisplayPoints(HistoTimeLine timeLine, int xDisplayOffset, int yDisplayOffset) {
		List<Point[]> suitePoints = new ArrayList<>(); // display point cache: one row for each record of the suite
		for (int i = 0; i < this.trailRecord.suiteManager.getSuiteLength(); i++) {
			suitePoints.add(this.getDisplayPoints(timeLine, xDisplayOffset, yDisplayOffset, i));
		}
		return suitePoints;
	}

	private Point[] getDisplayPoints(HistoTimeLine timeLine, int xDisplayOffset, int yDisplayOffset, int suiteOrdinal) {
		List<Point> points = new ArrayList<>();
		double tmpOffset = this.trailRecord.getMinDisplayValue() * 1 / this.trailRecord.getSyncMasterFactor();
		Vector<Integer> suiteRecord = this.trailRecord.suiteManager.get(suiteOrdinal);
		for (int i = 0; i < this.trailRecord.realSize(); i++) {
			if (suiteRecord.elementAt(i) != null) {
				points.add(new Point(xDisplayOffset + timeLine.getScalePositions().get((long) this.trailRecord.getParentTrail().getTime_ms(i)),
						yDisplayOffset - (int) (((suiteRecord.elementAt(i) / 1000.0) - tmpOffset) * this.trailRecord.getDisplayScaleFactorValue())));
			}
			else {
				points.add(null);
			}
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, Arrays.toString(points.toArray()));
		return points.toArray(new Point[0]);
	}

	/**
	 * Query the values for display.
	 * @param timeLine
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @return point time, value; null if the trail record value is null
	 */
	public List<Point[]> getSuiteGpsDisplayPoints(HistoTimeLine timeLine, int xDisplayOffset, int yDisplayOffset) {
		List<Point[]> suitePoints = new ArrayList<>(); // display point cache: one row for each record of the suite
		for (int i = 0; i < this.trailRecord.suiteManager.getSuiteLength(); i++) {
			suitePoints.add(this.getGpsDisplayPoints(timeLine, xDisplayOffset, yDisplayOffset, i));
		}
		return suitePoints;
	}

	public Point[] getGpsDisplayPoints(HistoTimeLine timeLine, int xDisplayOffset, int yDisplayOffset) {
		Point[] points = new Point[timeLine.getScalePositions().size()];
		int i = 0;
		Integer value = 0;
		final double tmpOffset = this.trailRecord.getMinDisplayValue() * 1 / this.trailRecord.getSyncMasterFactor(); // minDisplayValue is GPS DD format * 1000
		for (Integer xPos : timeLine.getScalePositions().values()) {
			if ((value = this.trailRecord.elementAt(i)) != null) {
				final double decimalDegreeValue = value / 1000000 + value % 1000000 / 600000.;
				points[i] = new Point(xDisplayOffset + xPos, yDisplayOffset - (int) ((decimalDegreeValue * 1000. - tmpOffset) * this.trailRecord.getDisplayScaleFactorValue()));
			}
			i++;
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "yPos = " + Arrays.toString(points)); //$NON-NLS-1$
		return points;
		// int grad = super.get(measurementPointIndex) / 1000000;
		// return new Point(xDisplayOffset + Double.valueOf(super.getTime_ms(measurementPointIndex) * super.displayScaleFactorTime).intValue(), yDisplayOffset
		// - Double.valueOf((((grad + ((this.get(measurementPointIndex) / 1000000.0 - grad) / 0.60)) * 1000.0) - (this.minDisplayValue * 1 / this.syncMasterFactor))
		// * this.displayScaleFactorValue).intValue());
	}

	/**
	 * Query the values for display.
	 * @param timeLine
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @param suiteOrdinal the 0-based ordinal number of the requested suite record
	 * @return point time, value; null if the trail record value is null
	 */
	public Point[] getGpsDisplayPoints(HistoTimeLine timeLine, int xDisplayOffset, int yDisplayOffset, int suiteOrdinal) {
		Point[] points = new Point[timeLine.getScalePositions().size()];
		int i = 0;
		Integer value = 0;
		final double tmpOffset = this.trailRecord.getMinDisplayValue() * 1 / this.trailRecord.getSyncMasterFactor(); // minDisplayValue is GPS DD format * 1000
		Vector<Integer> suiteRecord = this.trailRecord.suiteManager.get(suiteOrdinal);
		for (Integer xPos : timeLine.getScalePositions().values()) {
			if ((value = suiteRecord.elementAt(i)) != null) {
				final double decimalDegreeValue = value / 1000000 + value % 1000000 / 600000.;
				points[i] = new Point(xDisplayOffset + xPos, yDisplayOffset - (int) ((decimalDegreeValue * 1000. - tmpOffset) * this.trailRecord.getDisplayScaleFactorValue()));
			}
			i++;
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "yPos = " + Arrays.toString(points)); //$NON-NLS-1$
		return points;
	}

	/**
	 * @param index
	 * @return yPos in pixel (with top@0 and bottom@drawAreaBounds.height) or Integer.MIN_VALUE if the index value is null
	 */
	public int getVerticalDisplayPos(int index) {
		final Vector<Integer> points = this.trailRecord.getPoints();

		int verticalDisplayPos = Integer.MIN_VALUE;
		if (this.device.isGPSCoordinates(this.trailRecord)) {
			if (points.elementAt(index) != null) {
				double decimalDegreeValue = points.elementAt(index) / 1000000 + points.elementAt(index) % 1000000 / 600000.;
				verticalDisplayPos = this.trailRecord.getParentTrail().getDrawAreaBounds().height - (int) ((decimalDegreeValue * 1000. - this.trailRecord.getMinDisplayValue() * 1 / this.trailRecord.getSyncMasterFactor()) * this.trailRecord.getDisplayScaleFactorValue());
			}
		}
		else if (points.elementAt(index) != null)
			verticalDisplayPos = this.trailRecord.getParentTrail().getDrawAreaBounds().height - (int) ((points.elementAt(index) / 1000.0 - this.trailRecord.getMinDisplayValue() * 1 / this.trailRecord.getSyncMasterFactor()) * this.trailRecord.getDisplayScaleFactorValue());

		return verticalDisplayPos;
	}

	public int getVerticalDisplayPos(double translatedValue) {
		int verticalDisplayPos = Integer.MIN_VALUE;

		int point = (int) (this.device.reverseTranslateValue(this.trailRecord, translatedValue) * 1000.);
		if (this.device.isGPSCoordinates(this.trailRecord)) {
			double decimalDegreeValue = point / 1000000 + point % 1000000 / 600000.;
			verticalDisplayPos = this.trailRecord.getParentTrail().getDrawAreaBounds().height - (int) ((decimalDegreeValue * 1000. - this.trailRecord.getMinDisplayValue() * 1 / this.trailRecord.getSyncMasterFactor()) * this.trailRecord.getDisplayScaleFactorValue());
		}
		else {
			verticalDisplayPos = this.trailRecord.getParentTrail().getDrawAreaBounds().height - (int) ((point / 1000.0 - this.trailRecord.getMinDisplayValue() * 1 / this.trailRecord.getSyncMasterFactor()) * this.trailRecord.getDisplayScaleFactorValue());
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("translatedValue=%f reverseTranslatedPoint=%d yPos=%d", translatedValue, point, verticalDisplayPos)); //$NON-NLS-1$
		return verticalDisplayPos;
	}

}
