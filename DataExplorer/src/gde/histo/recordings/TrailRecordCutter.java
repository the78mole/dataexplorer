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

import static gde.histo.utils.ElaborateTukeyQuantile.CLOSE_OUTLIER_LIMIT;
import static gde.histo.utils.ElaborateTukeyQuantile.INTER_QUARTILE_SIGMA;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import gde.GDE;
import gde.histo.utils.SingleResponseRegression;
import gde.histo.utils.SingleResponseRegression.RegressionType;
import gde.histo.utils.Spot;
import gde.histo.utils.UniversalQuantile;
import gde.log.Logger;

/**
 * Define a view on a section of the trail record.
 * @author Thomas Eickert
 */
public final class TrailRecordCutter {
	private final static String											$CLASS_NAME	= TrailRecordCutter.class.getName();
	private final static Logger											log					= Logger.getLogger($CLASS_NAME);

	private final TrailRecord												trailrecord;
	private final SingleResponseRegression<Double>	regression;
	private final UniversalQuantile<Double>					quantile;

	public TrailRecordCutter(TrailRecord trailrecord, long timeStamp1_ms, long timeStamp2_ms) {
		this.trailrecord = trailrecord;

		List<Spot<Double>> subPoints = trailrecord.getSubPoints(timeStamp1_ms, timeStamp2_ms);
		if (!subPoints.isEmpty()) {
			this.quantile = new UniversalQuantile<>(subPoints); // take all points for display

			UniversalQuantile<Double> tmpQuantile = new UniversalQuantile<>(subPoints, INTER_QUARTILE_SIGMA,
					CLOSE_OUTLIER_LIMIT); // todo check if removing such close outliers is correct

			// eliminate Tukey outliers for regression
			List<Double> outliers = tmpQuantile.getOutliers();
			for (Iterator<Spot<Double>> iterator = subPoints.iterator(); iterator.hasNext();) {
				Spot<Double> spot = iterator.next();
				if (outliers.contains(spot.y())) iterator.remove();
			}

			this.regression = new SingleResponseRegression<>(subPoints, RegressionType.QUADRATIC);
			log.finer(() -> String.format("xAxisSize=%d regressionRealSize=%d starts at %tF %tR", //
					this.quantile.getSize(), this.regression.getRealSize(), new Date(timeStamp1_ms), new Date(timeStamp1_ms)));
		} else

		{
			this.regression = null;
			this.quantile = null;
		}
	}

	public double getBoundedAvgValue() {
		return this.regression.getAvg();
	}

	public double getBoundedSlopeValue(long timeStamp_ms) {
		return this.regression.getResponse(timeStamp_ms);
	}

	public double[] getBoundedBoxplotValues() {
		return this.quantile.getTukeyBoxPlot();
	}

	public boolean isValidBounds() {
		return this.regression != null || this.quantile != null;
	}

	/**
	 * @return the parabola spots which correspond to the regression input values
	 */
	public List<Spot<Double>> getBoundedParabolaValues() {
		return this.regression.getResponse();
	}

	public double getBoundedParabolaValue(long timeStamp_ms) {
		return this.regression.getResponse(timeStamp_ms);
	}

	/**
	 * @return false if there is a linear regression (maybe the number of measurements is too small)
	 */
	public boolean isBoundedParabola() {
		return this.regression.getGamma() != 0.;
	}

	/**
	 * @return the formatted translated average of all bounds values
	 */
	public String getFormattedBoundsAvg() {
		return new TrailRecordFormatter(this.trailrecord).getScaleValue(this.regression.getAvg());
	}

	/**
	 * @return the formatted translated difference between the left and right bounds values
	 */
	public String getFormattedBoundsDelta() {
		return new TrailRecordFormatter(this.trailrecord).getScaleValue(0. - this.regression.getDelta());
	}

	/**
	 * @return the formatted translated regression slope of the bounds values based on months
	 */
	public String getFormattedBoundsSlope() {
		return new TrailRecordFormatter(this.trailrecord).getScaleValue(this.regression.getSlope() * GDE.ONE_HOUR_MS * 24 * 365 / 12);
	}

}
