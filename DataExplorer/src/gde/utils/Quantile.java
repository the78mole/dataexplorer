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
    
    Copyright (c) 2016 Thomas Eickert
****************************************************************************************/
package gde.utils;

import gde.log.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import gde.data.Record;
import gde.data.RecordSet;
import gde.data.TrailRecord;

/**
 * calculates quantiles of a probability distribution.
 * is based on a mergesort and thus avg O(n log n).
 * NB: a 500k records clone + sort takes 55 ms (in the arrayList version / on ET's machine).
 * quickselect should reduce this to 10 ms.
 * this in turn is compensated by the fact that we mostly need 3 quantiles from the same population.
 * @author Thomas Eickert
 */
public class Quantile {
	private final static String			$CLASS_NAME	= Quantile.class.getName();
	private final static Logger			log					= Logger.getLogger($CLASS_NAME);

	private final List<Integer>			population;
	private final EnumSet<Fixings>	fixings;
	private int											realSize;																		// size of the population without zero values

	public enum Fixings {
		ALLOW_NULLS, REMOVE_NULLS, REMOVE_ZEROS, IS_SAMPLE
	};

	/**
	 * constructor based on the record itself.
	 * @param currentRecord
	 * @param fixings defines how to proceed with the data
	 */
	public Quantile(Vector<Integer> population, EnumSet<Fixings> fixings) {
		this.population = new ArrayList<>(population);
		this.fixings = fixings;
		if (fixings.contains(Fixings.REMOVE_NULLS) && fixings.contains(Fixings.REMOVE_ZEROS)) {
			for (int i = this.population.size() - 1; i >= 0; i--) {
				if (this.population.get(i) == null || this.population.get(i) == 0) {
					this.population.remove(i);
				}
			}
		}
		else if (fixings.contains(Fixings.REMOVE_NULLS)) {
			for (int i = this.population.size() - 1; i >= 0; i--) {
				if (this.population.get(i) == null) {
					this.population.remove(i);
				}
			}
		}
		else if (fixings.contains(Fixings.REMOVE_ZEROS)) {
			for (int i = this.population.size() - 1; i >= 0; i--) {
				if (this.population.get(i) == 0) {
					this.population.remove(i);
				}
			}
		}
		if (fixings.contains(Fixings.ALLOW_NULLS)) {
			Collections.sort(this.population, Comparator.nullsLast(Integer::compareTo)); // approx. 20% performance loss 
			for (int i = this.population.size() - 1; i >= 0; i--) {
				if (this.population.get(i) != null) {
					this.realSize = i + 1;
					break;
				}
			}
		}
		else {
			Collections.sort(this.population);
			this.realSize = this.population.size();
		}
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, Arrays.toString(this.population.toArray()));
	}

	/**
	 * calculates the quantile for the population or for a sample.
	 * the sample quantile is calculated according to R-6, SAS-4, SciPy-(0,0), Maple-5 which is piecewise linear and symmetric.
	 * @param probabilityCutPoint as value between 0 and 1
	 * @return
	 */
	public double getQuantile(double probabilityCutPoint) {
		if (this.fixings.contains(Fixings.IS_SAMPLE)) {
			if (probabilityCutPoint >= 1. / (this.realSize + 1) || probabilityCutPoint < (double) this.realSize / (this.realSize + 1)) {
				double position = (this.realSize + 1) * probabilityCutPoint;
				return this.population.get((int) position - 1) + (position - (int) position) * (this.population.get((int) position) - this.population.get((int) position - 1));
			}
			else if (probabilityCutPoint < 1. / (this.realSize + 1))
				return this.population.get(0);
			else
				return this.population.get(this.realSize - 1);
		}
		else {
			if (probabilityCutPoint > 0. || probabilityCutPoint < 1.) {
				double position = this.realSize * probabilityCutPoint;
				if (position % 2 == 0)
					// take elements p-1 and p due to zerobased index
					return (this.population.get((int) position - 1) + this.population.get((int) (position))) / 2.;
				else
					// take element p due to zerobased index in combination with upper bound operation in the calculation rule
					return this.population.get((int) (position));
			}
			else if (probabilityCutPoint == 0.)
				return this.population.get(0);
			else
				return this.population.get(this.realSize - 1);
		}
	}

	public double getQuartile0() {
		return this.population.get(0);
	}

	public double getQuartile1() {
		return getQuantile(.25);
	}

	public double getQuartile2() {
		return getQuantile(.5);
	}

	public double getQuartile3() {
		return getQuantile(.75);
	}

	public double getQuartile4() {
		return this.population.get(this.realSize - 1);
	}
	
	public double getInterQuartileRange() {
		return getQuantile(.75) - getQuantile(.25);
	}
	
	public double getQuantileLowerWhisker() {
		final double probabilityCutPoint = .25;
		final double whiskerStartValue = getQuantile(probabilityCutPoint);
		final double whiskerLimitValue = whiskerStartValue - getInterQuartileRange() * 1.5;
		double value = whiskerStartValue;
		for (int i = 0; i < this.realSize * probabilityCutPoint; i++) {
			if (this.population.get(i) >= whiskerLimitValue) {
				// get the corrected value which is crucial for samples
				value = getQuantile( (.5 + i) / this.realSize); // add .5 due to zerobased index and rule 0<p<1 which implies an index average value
				// take the whisker limit value if the interpolation / estimation value is beyond the limit
				value = value < whiskerLimitValue ? whiskerLimitValue : value;
				break;
			}
		}
		return value;
	}

	public double getQuantileUpperWhisker() {
		final double probabilityCutPoint = .75;
		final double whiskerStartValue = getQuantile(probabilityCutPoint);
		final double whiskerLimitValue = whiskerStartValue + getInterQuartileRange() * 1.5;
		double value = whiskerStartValue;
		for (int i = this.realSize - 1; i > this.realSize * probabilityCutPoint; i--) {
			if (this.population.get(i) <= whiskerLimitValue) {
				// get the corrected value which is crucial for samples
				value = getQuantile((.5 + i) / this.realSize); // add .5 due to zerobased index and rule 0<p<1 which implies an index average value
				// take the whisker limit value if the interpolation / estimation value is beyond the limit
				value = value > whiskerLimitValue ? whiskerLimitValue : value;
				break;
			}
		}
		return value;
	}

	public double[] getTukeyBoxPlot() {
		final int quartile0Idx = 0, lowerWhiskerIdx = 1, quartile1Idx = 2, quartile2Idx = 3, quartile3Idx = 4, upperWhiskerIndx = 5, quartile4Idx = 6;
		double[] values = new double[7];
		values[quartile0Idx] = getQuartile0();
		values[quartile1Idx] = getQuartile1();
		values[quartile2Idx] = getQuartile2();
		values[quartile3Idx] = getQuartile3();
		values[quartile4Idx] = getQuartile4();
		
		return values;
	}

	/**
	 * @return size after removing nulls or zeros as defined in the constructor
	 */
	public int getRealSize() {
		return realSize;
	}

}
