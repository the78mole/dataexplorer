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

import org.eclipse.swt.graphics.Point;

import gde.histo.datasources.HistoSet;
import gde.histo.utils.HistoTimeLine;
import gde.log.Logger;

/**
 * Record data mapping for the histo graphics tab.
 * @author Thomas Eickert (USER)
 */
public final class HistoGraphicsMapper {
	private final static String	$CLASS_NAME	= TrailSelector.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	private final TrailRecord		trailRecord;
	private final double				yOffset;

	public HistoGraphicsMapper(TrailRecord trailRecord) {
		this.trailRecord = trailRecord;
		this.yOffset = this.trailRecord.getMinDisplayValue() * 1 / this.trailRecord.getSyncMasterFactor();
	}

	/**
	 * Query the values for display.
	 * @param timeLine
	 * @return the display point x axis value and multiple y axis values; null if the trail record value is null
	 */
	public List<PointArray> getSuiteDisplayPoints(HistoTimeLine timeLine) {
		List<PointArray> suitePoints = new ArrayList<>();
		int firstOrdinal = this.trailRecord.getTrailSelector().getTrailType().getSuiteMasterIndex();
		for (int i = 0; i < timeLine.getScalePositions().size(); i++) {
			if (this.trailRecord.getSuiteRecords().getSuiteValue(firstOrdinal, i) != null)
				suitePoints.add(getSuiteDisplayPoints(timeLine, i));
			else
				suitePoints.add(null);
		}
		return suitePoints;
	}

	private PointArray getSuiteDisplayPoints(HistoTimeLine timeLine, int index) {
		int xDisplayOffset = this.trailRecord.getParentTrail().getDrawAreaBounds().x;
		int yDisplayOffset = this.trailRecord.getParentTrail().getDrawAreaBounds().height + this.trailRecord.getParentTrail().getDrawAreaBounds().y;
		int suiteSize = this.trailRecord.getTrailSelector().getTrailType().getSuiteMembers().size();
		PointArray pointArray = new PointArray(suiteSize);
		pointArray.setX(xDisplayOffset + timeLine.getScalePositions().get((long) this.trailRecord.getParentTrail().getTime_ms(index)));

		SuiteRecords suiteRecords = this.trailRecord.getSuiteRecords();
		for (int j = 0; j < suiteSize; j++) {
			Integer value = suiteRecords.getSuiteValue(j, index);
			if (value != null) {
				double decodedValue = HistoSet.decodeVaultValue(trailRecord, value / 1000.0);
				pointArray.setY(j, yDisplayOffset - (int) ((decodedValue - this.yOffset) * this.trailRecord.getDisplayScaleFactorValue()));
			}
		}
		log.finer(() -> pointArray.toString());
		return pointArray;
	}

	/**
	 * Query the values for display.
	 * @param timeLine
	 * @return point time, value; null if the trail record value is null
	 */
	public Point[] getDisplayPoints(HistoTimeLine timeLine) {
		int xDisplayOffset = this.trailRecord.getParentTrail().getDrawAreaBounds().x;
		int yDisplayOffset = this.trailRecord.getParentTrail().getDrawAreaBounds().height + this.trailRecord.getParentTrail().getDrawAreaBounds().y;
		Point[] points = new Point[this.trailRecord.realSize()];
		for (int i = 0; i < this.trailRecord.realSize(); i++) {
			Integer value = this.trailRecord.elementAt(i);
			if (value != null) {
				double decodedValue = HistoSet.decodeVaultValue(trailRecord, value / 1000.0);
				points[i] = new Point(xDisplayOffset + timeLine.getScalePositions().get((long) this.trailRecord.getParentTrail().getTime_ms(i)),
						yDisplayOffset - (int) ((decodedValue - this.yOffset) * this.trailRecord.getDisplayScaleFactorValue()));
			}
		}
		log.finer(() -> Arrays.toString(points));
		return points;
	}

	/**
	 * @param index
	 * @return yPos in pixel (with top@0 and bottom@drawAreaBounds.height) or Integer.MIN_VALUE if the index value is null
	 */
	public int getVerticalDisplayPos(int index) {
		int verticalDisplayPos = Integer.MIN_VALUE;
		Integer value = this.trailRecord.getPoints().elementAt(index);
		if (value != null) {
			double decodedValue = HistoSet.decodeVaultValue(trailRecord, value / 1000.0);
			verticalDisplayPos = this.trailRecord.getParentTrail().getDrawAreaBounds().height - (int) ((decodedValue - this.yOffset) * this.trailRecord.getDisplayScaleFactorValue());
		}
		return verticalDisplayPos;
	}

	public int getVerticalDisplayPos(double translatedValue) {
		int verticalDisplayPos;
		verticalDisplayPos = this.trailRecord.getParentTrail().getDrawAreaBounds().height - (int) ((translatedValue - this.yOffset) * this.trailRecord.getDisplayScaleFactorValue());
		log.finer(() -> String.format("translatedValue=%f yPos=%d", translatedValue, verticalDisplayPos)); //$NON-NLS-1$
		return verticalDisplayPos;
	}

}
