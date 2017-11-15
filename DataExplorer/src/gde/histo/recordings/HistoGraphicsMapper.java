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

import org.eclipse.swt.graphics.Point;

import gde.data.IRecord;
import gde.data.Record;
import gde.histo.utils.HistoTimeLine;
import gde.log.Logger;
import gde.ui.DataExplorer;

/**
 * Record data mapping for the histo graphics tab.
 * @author Thomas Eickert (USER)
 */
public final class HistoGraphicsMapper {
	private final static String	$CLASS_NAME	= TrailSelector.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	private final TrailRecord		trailRecord;
	private final int						suiteSize;
	private final double				yOffset;

	public HistoGraphicsMapper(TrailRecord trailRecord) {
		this.trailRecord = trailRecord;
		this.suiteSize = this.trailRecord.getTrailSelector().getTrailType().getSuiteMembers().size();
		this.yOffset = this.trailRecord.getMinDisplayValue() * 1 / this.trailRecord.getSyncMasterFactor();
	}

	/**
	 * Query the values for display.
	 * @param timeLine
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @return the display point x axis value and multiple y axis values; null if the trail record value is null
	 */
	public List<PointArray> getSuiteDisplayPoints(HistoTimeLine timeLine, int xDisplayOffset, int yDisplayOffset) {
		if (xDisplayOffset != this.trailRecord.getParentTrail().getDrawAreaBounds().x || yDisplayOffset != (this.trailRecord.getParentTrail().getDrawAreaBounds().height + this.trailRecord.getParentTrail().getDrawAreaBounds().y)) {
			throw new UnsupportedOperationException(); // replace parameters with fields access
		}
		List<PointArray> suitePoints = new ArrayList<>();
		if (this.trailRecord.getDevice().isGPSCoordinates((IRecord) (Record) this.trailRecord)) {
			int firstOrdinal = this.trailRecord.getTrailSelector().getTrailType().getSuiteMasterIndex();
			for (int i = 0; i < timeLine.getScalePositions().size(); i++) {
				if (this.trailRecord.getSuiteRecords().getSuiteValue(firstOrdinal, i) != null)
					suitePoints.add(getSuiteGpsDisplayPoints(timeLine, xDisplayOffset, yDisplayOffset, i));
				else
					suitePoints.add(null);
			}
		} else {
			int firstOrdinal = this.trailRecord.getTrailSelector().getTrailType().getSuiteMasterIndex();
			for (int i = 0; i < timeLine.getScalePositions().size(); i++) {
				if (this.trailRecord.getSuiteRecords().getSuiteValue(firstOrdinal, i) != null)
					suitePoints.add(getSuiteDisplayPoints(timeLine, xDisplayOffset, yDisplayOffset, i));
				else
					suitePoints.add(null);
			}
		}
		return suitePoints;
	}

	private PointArray getSuiteGpsDisplayPoints(HistoTimeLine timeLine, int xDisplayOffset, int yDisplayOffset, int index) {
		PointArray pointArray = new PointArray(this.suiteSize);
		pointArray.setX(xDisplayOffset + timeLine.getScalePositions().get((long) this.trailRecord.getParentTrail().getTime_ms(index)));

		for (int j = 0; j < this.suiteSize; j++) {
			int value = this.trailRecord.getSuiteRecords().getSuiteValue(j, index);
			double decimalDegreeValue = value / 1000000 + value % 1000000 / 600000.;
			pointArray.setY(j, yDisplayOffset - (int) ((decimalDegreeValue * 1000. - this.yOffset) * this.trailRecord.getDisplayScaleFactorValue()));
		}
		log.finer(() -> pointArray.toString());
		return pointArray;
	}

	private PointArray getSuiteDisplayPoints(HistoTimeLine timeLine, int xDisplayOffset, int yDisplayOffset, int index) {
		PointArray pointArray = new PointArray(this.suiteSize);
		pointArray.setX(xDisplayOffset + timeLine.getScalePositions().get((long) this.trailRecord.getParentTrail().getTime_ms(index)));

		for (int j = 0; j < this.suiteSize; j++) {
			int value = this.trailRecord.getSuiteRecords().getSuiteValue(j, index);
			pointArray.setY(j, yDisplayOffset - (int) ((value / 1000.0 - this.yOffset) * this.trailRecord.getDisplayScaleFactorValue()));
		}

		return pointArray;
	}

	/**
	 * Query the values for display.
	 * @param timeLine
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @return point time, value; null if the trail record value is null
	 */
	public Point[] getDisplayPoints(HistoTimeLine timeLine, int xDisplayOffset, int yDisplayOffset) {
		if (xDisplayOffset != this.trailRecord.getParentTrail().getDrawAreaBounds().x || yDisplayOffset != (this.trailRecord.getParentTrail().getDrawAreaBounds().height + this.trailRecord.getParentTrail().getDrawAreaBounds().y)) {
			throw new UnsupportedOperationException(); // replace parameters with fields access
		}
		Point[] points = new Point[this.trailRecord.realSize()];
		if (this.trailRecord.getDevice().isGPSCoordinates((IRecord) (Record) this.trailRecord)) {
			Integer value = 0;
			for (int i = 0; i < this.trailRecord.realSize(); i++) {
				value = this.trailRecord.elementAt(i);
				if (value != null) {
					double decimalDegreeValue = value / 1000000 + value % 1000000 / 600000.;
					points[i] = new Point(xDisplayOffset + timeLine.getScalePositions().get((long) this.trailRecord.getParentTrail().getTime_ms(i)),
							yDisplayOffset - (int) ((decimalDegreeValue * 1000. - this.yOffset) * this.trailRecord.getDisplayScaleFactorValue()));
				}
			}
			log.finer(() -> "yPos = " + Arrays.toString(points)); //$NON-NLS-1$
		} else {
			Integer value = 0;
			for (int i = 0; i < this.trailRecord.realSize(); i++) {
				value = this.trailRecord.elementAt(i);
				if (value != null) {
					points[i] = new Point(xDisplayOffset + timeLine.getScalePositions().get((long) this.trailRecord.getParentTrail().getTime_ms(i)),
							yDisplayOffset - (int) ((value / 1000.0 - this.yOffset) * this.trailRecord.getDisplayScaleFactorValue()));
				}
			}
			log.finer(() -> Arrays.toString(points));
		}
		return points;
	}

	/**
	 * @param index
	 * @return yPos in pixel (with top@0 and bottom@drawAreaBounds.height) or Integer.MIN_VALUE if the index value is null
	 */
	public int getVerticalDisplayPos(int index) {
		final Vector<Integer> points = this.trailRecord.getPoints();

		int verticalDisplayPos = Integer.MIN_VALUE;
		Integer value = 0;
		if (DataExplorer.application.getActiveDevice().isGPSCoordinates((IRecord) (Record) this.trailRecord)) {
			value = points.elementAt(index);
			if (value != null) {
				double decimalDegreeValue = value / 1000000 + value % 1000000 / 600000.;
				verticalDisplayPos = this.trailRecord.getParentTrail().getDrawAreaBounds().height - (int) ((decimalDegreeValue * 1000. - this.yOffset) * this.trailRecord.getDisplayScaleFactorValue());
			}
		} else {
			value = points.elementAt(index);
			if (value != null)
				verticalDisplayPos = this.trailRecord.getParentTrail().getDrawAreaBounds().height - (int) ((value / 1000.0 - this.yOffset) * this.trailRecord.getDisplayScaleFactorValue());
		}
		return verticalDisplayPos;
	}

	public int getVerticalDisplayPos(double translatedValue) {
		int verticalDisplayPos;

		int point = (int) (DataExplorer.application.getActiveDevice().reverseTranslateValue(this.trailRecord, translatedValue) * 1000.);
		if (DataExplorer.application.getActiveDevice().isGPSCoordinates((IRecord) (Record) this.trailRecord)) {
			double decimalDegreeValue = point / 1000000 + point % 1000000 / 600000.;
			verticalDisplayPos = this.trailRecord.getParentTrail().getDrawAreaBounds().height - (int) ((decimalDegreeValue * 1000. - this.yOffset) * this.trailRecord.getDisplayScaleFactorValue());
		} else {
			verticalDisplayPos = this.trailRecord.getParentTrail().getDrawAreaBounds().height - (int) ((point / 1000.0 - this.yOffset) * this.trailRecord.getDisplayScaleFactorValue());
		}
		log.finer(() -> String.format("translatedValue=%f reverseTranslatedPoint=%d yPos=%d", translatedValue, point, verticalDisplayPos)); //$NON-NLS-1$
		return verticalDisplayPos;
	}

}
