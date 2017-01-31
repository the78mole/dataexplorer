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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.function.DoubleConsumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * calculates quantiles of a probability distribution.
 * is based on a mergesort and thus avg O(n log n).
 * NB: a 500k records clone + sort takes 55 ms (in the arrayList version / on ET's machine).
 * quickselect should reduce this to 10 ms.
 * this in turn is compensated by the fact that we mostly need 3 quantiles from the same population.
 * @author Thomas Eickert
 */
public class Quantile {
	private final static String			$CLASS_NAME					= Quantile.class.getName();
	private final static Logger			log									= Logger.getLogger($CLASS_NAME);

	private final static double[]		sigmaProbabilities	= { .5, 3.17310508E-1, 4.5500264E-2, 2.699796E-3, 6.334E-5, 5.733303E-7, 1.973E-9, 2.56E-12, 0 };
	private final static int				sigmaOutlierDefault	= 6;																																														// sixSigma

	private final List<Integer>			iPopulation;
	private final List<Double>			dPopulation;
	private final EnumSet<Fixings>	fixings;
	private final double						probabilityOutlier;

	private Double									maxFigure;
	private Double									minFigure;
	private Double									sumFigure;
	private Double									avgFigure;
	private Double									sigmaValue;

	public enum Fixings {
		REMOVE_NULLS, REMOVE_ZEROS, REMOVE_TYPEMAXMIN, IS_SAMPLE
	};

	/**
	 * supports standard deviation calculation via parallel streams.
	 */
	class StatsHelper implements DoubleConsumer {
		private double	avg				= 0;
		private double	varTimesN	= 0;
		private int			count			= 0;

		public double getAvg() {
			return count > 0 ? avg : 0;
		}

		public double getSigma(boolean isSample) {
			return count > 0 ? Math.sqrt(varTimesN / (isSample ? count - 1 : count)) : 0;
		}

		public double getCount() {
			return count;
		}

		public void accept(double value) {
			varTimesN += (value - avg) * (value - avg) * count / ++count; // pls note the counter increment
			avg += (value - avg) / count;
		}

		/**
		 * http://stats.stackexchange.com/a/56000  OR  https://www.researchgate.net/post/How_do_I_combine_mean_and_standard_deviation_of_two_groups
		 * @param other
		 */
		public void combine(StatsHelper other) {
			double tmpAvg = this.avg * count / (count + other.count) + other.avg * other.count / (count + other.count);
			varTimesN += other.varTimesN + count * (avg - tmpAvg) * (avg - tmpAvg) + other.count * (other.avg - tmpAvg) * (other.avg - tmpAvg);
			avg = tmpAvg;
			count += other.count;
		}
	}

	/**
	 * constructor based on a vector, e.g. the record.
	 * @param iPopulation
	 * @param fixings defines how to proceed with the data
	 * @param sigmaOutlier sigma limit for identifying measurement outliers
	 */
	public Quantile(Vector<Integer> iPopulation, EnumSet<Fixings> fixings, int sigmaOutlier) {
		if (iPopulation.isEmpty()) throw new UnsupportedOperationException();
		this.dPopulation = null;
		this.fixings = fixings;
		this.probabilityOutlier = sigmaOutlier < Quantile.sigmaProbabilities.length && sigmaOutlier > 0 ? Quantile.sigmaProbabilities[sigmaOutlier]
				: Quantile.sigmaProbabilities[Quantile.sigmaProbabilities.length - 1];
		ArrayList<Integer> fullList = new ArrayList<>(iPopulation);

		List<Integer> excludes = new ArrayList<Integer>();
		if (fixings.contains(Fixings.REMOVE_NULLS)) excludes.add(null);
		if (fixings.contains(Fixings.REMOVE_ZEROS)) excludes.add(0);
		if (fixings.contains(Fixings.REMOVE_TYPEMAXMIN)) {
			excludes.add(Integer.MIN_VALUE);
			excludes.add(Integer.MAX_VALUE);
		}
		if (excludes.isEmpty()) {
			this.iPopulation = fullList;
		}
		else {
			this.iPopulation = new ArrayList<>();
			for (Integer value : fullList) {
				if (!excludes.contains(value)) this.iPopulation.add(value);
			}
		}

		Collections.sort(this.iPopulation);
		while (getQuantile(this.probabilityOutlier) != getQuartile0())
			this.iPopulation.remove(0);
		while (getQuantile(1. - this.probabilityOutlier) != getQuartile4())
			this.iPopulation.remove(this.iPopulation.size() - 1);
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, Arrays.toString(iPopulation.toArray()));
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, Arrays.toString(this.iPopulation.toArray()));
	}

	/**
	 * constructor based on any collection.
	 * @param dPopulation
	 * @param fixings defines how to proceed with the data
	 * @param sigmaOutlier sigma limit for identifying measurement outliers
	 */
	public Quantile(Collection<Double> dPopulation, EnumSet<Fixings> fixings, int sigmaOutlier) {
		if (dPopulation.isEmpty()) throw new UnsupportedOperationException();
		this.iPopulation = null;
		this.fixings = fixings;
		this.probabilityOutlier = sigmaOutlier < Quantile.sigmaProbabilities.length && sigmaOutlier > 0 ? Quantile.sigmaProbabilities[sigmaOutlier]
				: Quantile.sigmaProbabilities[Quantile.sigmaOutlierDefault];
		ArrayList<Double> fullList = new ArrayList<>(dPopulation);

		List<Double> excludes = new ArrayList<Double>();
		if (fixings.contains(Fixings.REMOVE_NULLS)) excludes.add(null);
		if (fixings.contains(Fixings.REMOVE_ZEROS)) excludes.add(0.);
		if (fixings.contains(Fixings.REMOVE_TYPEMAXMIN)) {
			excludes.add(-Double.MAX_VALUE);
			excludes.add(Double.MAX_VALUE);
		}
		if (excludes.isEmpty()) {
			this.dPopulation = fullList;
		}
		else {
			this.dPopulation = new ArrayList<>();
			for (Double value : fullList) {
				if (!excludes.contains(value)) this.dPopulation.add(value);
			}
		}

		Collections.sort(this.dPopulation);
		while (getQuantile(this.probabilityOutlier) != getQuartile0())
			this.dPopulation.remove(0);
		while (getQuantile(1. - this.probabilityOutlier) != getQuartile4())
			this.dPopulation.remove(this.iPopulation.size() - 1);
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, Arrays.toString(dPopulation.toArray()));
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, Arrays.toString(this.dPopulation.toArray()));
	}

	/**
	 * constructor based on a vector, e.g. the record.
	 * @param iPopulation
	 * @param fixings define how to proceed with the data
	 */
	public Quantile(Vector<Integer> iPopulation, EnumSet<Fixings> fixings, boolean obsolete) {
		this.dPopulation = null;
		if (iPopulation.isEmpty()) throw new UnsupportedOperationException();
		this.fixings = fixings;
		this.probabilityOutlier = Quantile.sigmaProbabilities[6]; // 6Sigma

		Stream<Integer> stream = iPopulation.parallelStream();
		if (fixings.contains(Fixings.REMOVE_NULLS)) stream = stream.filter(Objects::nonNull);
		if (fixings.contains(Fixings.REMOVE_ZEROS)) stream = stream.filter(x -> x != 0);
		if (fixings.contains(Fixings.REMOVE_TYPEMAXMIN)) {
			Integer[] excludes = { Integer.MIN_VALUE, Integer.MAX_VALUE };
			stream = stream.filter(x -> Arrays.asList(excludes).contains(x));
		}
		this.iPopulation = stream.sorted().collect(Collectors.toList());
		while (getQuantile(this.probabilityOutlier) != getQuartile0())
			this.dPopulation.remove(0);
		while (getQuantile(1. - this.probabilityOutlier) != getQuartile4())
			this.dPopulation.remove(this.iPopulation.size() - 1);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "i " + iPopulation.size() + " " + this.iPopulation.size()); //$NON-NLS-1$ //$NON-NLS-2$
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, Arrays.toString(iPopulation.toArray()));
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, Arrays.toString(this.iPopulation.toArray()));
	}

	/**
	 * constructor based on any collection.
	 * @param dPopulation
	 * @param fixings define how to proceed with the data
	 */
	public Quantile(Collection<Double> dPopulation, EnumSet<Fixings> fixings, boolean obsolete) {
		this.iPopulation = null;
		if (dPopulation.isEmpty()) throw new UnsupportedOperationException();
		this.fixings = fixings;
		this.probabilityOutlier = Quantile.sigmaProbabilities[6]; // 6Sigma

		Stream<Double> stream = dPopulation.parallelStream();
		if (fixings.contains(Fixings.REMOVE_NULLS)) stream = stream.filter(Objects::nonNull);
		if (fixings.contains(Fixings.REMOVE_ZEROS)) stream = stream.filter(x -> x != 0);
		if (fixings.contains(Fixings.REMOVE_TYPEMAXMIN)) {
			Double[] excludes = { -Double.MAX_VALUE, Double.MAX_VALUE };
			stream = stream.filter(x -> Arrays.asList(excludes).contains(x));
		}
		this.dPopulation = stream.sorted().collect(Collectors.toList());
		while (getQuantile(this.probabilityOutlier) != getQuartile0())
			this.dPopulation.remove(0);
		while (getQuantile(1. - this.probabilityOutlier) != getQuartile4())
			this.dPopulation.remove(this.iPopulation.size() - 1);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "d " + dPopulation.size() + " " + this.dPopulation.size()); //$NON-NLS-1$ //$NON-NLS-2$
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, Arrays.toString(dPopulation.toArray()));
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, Arrays.toString(this.dPopulation.toArray()));
	}

	public double getMaxFigure() {
		if (this.maxFigure == null) {
			if (this.dPopulation == null)
				this.maxFigure = (double) this.iPopulation.parallelStream().mapToInt(x -> x).max().getAsInt();
			else
				this.maxFigure = this.dPopulation.parallelStream().mapToDouble(x -> x).max().getAsDouble();
		}
		return this.maxFigure;
	}

	public double getMinFigure() {
		if (this.minFigure == null) {
			if (this.dPopulation == null)
				this.minFigure = (double) this.iPopulation.parallelStream().mapToInt(x -> x).min().getAsInt();
			else
				this.minFigure = this.dPopulation.parallelStream().mapToDouble(x -> x).min().getAsDouble();
		}
		return this.minFigure;
	}

	public double getSumFigure() {
		if (this.sumFigure == null) {
			if (this.dPopulation == null)
				this.sumFigure = (double) this.iPopulation.parallelStream().mapToInt(x -> x).sum();
			else
				this.sumFigure = this.dPopulation.parallelStream().mapToDouble(x -> x).sum();
		}
		return this.sumFigure;
	}

	public double getAvgFigure() {
		if (this.avgFigure == null) {
			if (this.dPopulation == null)
				this.avgFigure = this.iPopulation.parallelStream().mapToInt(x -> x).average().getAsDouble();
			else
				this.avgFigure = this.dPopulation.parallelStream().mapToDouble(x -> x).average().getAsDouble();
		}
		return this.avgFigure;
	}

	public double getAvgOBS() {
		if (this.dPopulation == null) {
			double avg = 0;
			for (int i = 0; i < this.iPopulation.size(); i++) {
				double value = this.iPopulation.get(i).doubleValue();
				avg += (value - avg) / (i + 1);
			}
			return avg;
		}
		else {
			double avg = 0;
			for (int i = 0; i < this.dPopulation.size(); i++) {
				double value = this.dPopulation.get(i);
				avg += (value - avg) / (i + 1);
			}
			return avg;
		}
	}

	public double getAvgSlow() {
		if (this.dPopulation == null)
			return this.iPopulation.parallelStream().mapToDouble(x -> x).collect(StatsHelper::new, StatsHelper::accept, StatsHelper::combine).getAvg();
		else
			return this.dPopulation.parallelStream().mapToDouble(x -> x).collect(StatsHelper::new, StatsHelper::accept, StatsHelper::combine).getAvg();
	}

	public double getSigmaOBS() {
		if (this.dPopulation == null) {
			double avg = getAvgOBS();
			double varTimesN = 0;
			for (int i = 0; i < this.iPopulation.size(); i++) {
				double value = this.iPopulation.get(i).doubleValue();
				varTimesN += (value - avg) * (value - avg);
			}
			return this.iPopulation.size() > 0 ? Math.sqrt(varTimesN / (this.fixings.contains(Fixings.IS_SAMPLE) ? this.iPopulation.size() - 1 : this.iPopulation.size())) : 0;
		}
		else {
			double avg = getAvgOBS();
			double varTimesN = 0;
			for (int i = 0; i < this.dPopulation.size(); i++) {
				double value = this.dPopulation.get(i);
				varTimesN += (value - avg) * (value - avg);
			}
			return this.dPopulation.size() > 0 ? Math.sqrt(varTimesN / (this.fixings.contains(Fixings.IS_SAMPLE) ? this.dPopulation.size() - 1 : this.dPopulation.size())) : 0;
		}
	}

	public double getSigmaRunningOBS() {
		if (this.dPopulation == null) {
			double avg = 0;
			double varTimesN = 0;
			int count = 0;
			for (int i = 0; i < this.iPopulation.size(); i++) {
				double value = this.iPopulation.get(i).doubleValue();
				varTimesN += (value - avg) * (value - avg) * count / ++count; // pls note the counter increment
				avg += (value - avg) / count;
			}
			return this.iPopulation.size() > 0 ? Math.sqrt(varTimesN / (this.fixings.contains(Fixings.IS_SAMPLE) ? this.iPopulation.size() - 1 : this.iPopulation.size())) : 0;
		}
		else {
			double avg = 0;
			double varTimesN = 0;
			double count = 0; // double count improves the performance in this case (not in the integer based branch) 
			for (int i = 0; i < this.dPopulation.size(); i++) {
				double value = this.dPopulation.get(i);
				varTimesN += (value - avg) * (value - avg) * count / ++count; // pls note the counter increment
				avg += (value - avg) / count;
			}
			return this.dPopulation.size() > 0 ? Math.sqrt(varTimesN / (this.fixings.contains(Fixings.IS_SAMPLE) ? this.dPopulation.size() - 1 : this.dPopulation.size())) : 0;
		}
	}

	public double getSigmaFigure() {
		if (this.sigmaValue == null) {
			if (this.dPopulation == null) {
				this.sigmaValue = this.iPopulation.parallelStream().mapToDouble(x -> x).collect(StatsHelper::new, StatsHelper::accept, StatsHelper::combine).getSigma(this.fixings.contains(Fixings.IS_SAMPLE));
			}
			else
				this.sigmaValue = this.dPopulation.parallelStream().mapToDouble(x -> x).collect(StatsHelper::new, StatsHelper::accept, StatsHelper::combine).getSigma(this.fixings.contains(Fixings.IS_SAMPLE));
		}
		return this.sigmaValue;
	}

	/**
	 * calculates the quantile for the population or for a sample.
	 * the sample quantile is calculated according to R-6, SAS-4, SciPy-(0,0), Maple-5 which is piecewise linear and symmetric.
	 * @param probabilityCutPoint as value between 0 and 1
	 * @return
	 */
	public double getQuantile(double probabilityCutPoint) {
		if (this.dPopulation == null) {
			int realSize = this.iPopulation.size();
			if (this.fixings.contains(Fixings.IS_SAMPLE)) {
				if (probabilityCutPoint >= 1. / (realSize + 1) && probabilityCutPoint < (double) realSize / (realSize + 1)) {
					double position = (realSize + 1) * probabilityCutPoint;
					return this.iPopulation.get((int) position - 1) + (position - (int) position) * (this.iPopulation.get((int) position) - this.iPopulation.get((int) position - 1));
				}
				else if (probabilityCutPoint < 1. / (realSize + 1))
					return this.iPopulation.get(0);
				else
					return this.iPopulation.get(realSize - 1);
			}
			else {
				if (probabilityCutPoint > 0. || probabilityCutPoint < 1.) {
					double position = realSize * probabilityCutPoint;
					if (position % 2 == 0)
						// take elements p-1 and p due to zerobased index
						return (this.iPopulation.get((int) position) + this.iPopulation.get((int) (position + 1))) / 2.;
					else
						// take element p due to zerobased index in combination with upper bound operation in the calculation rule
						return this.iPopulation.get((int) (position));
				}
				else if (probabilityCutPoint == 0.)
					return this.iPopulation.get(0);
				else
					return this.iPopulation.get(realSize - 1);
			}
		}
		else {
			int realSize = this.dPopulation.size();
			if (this.fixings.contains(Fixings.IS_SAMPLE)) {
				if (probabilityCutPoint >= 1. / (realSize + 1) && probabilityCutPoint < (double) realSize / (realSize + 1)) {
					double position = (realSize + 1) * probabilityCutPoint;
					return this.dPopulation.get((int) position - 1) + (position - (int) position) * (this.dPopulation.get((int) position) - this.dPopulation.get((int) position - 1));
				}
				else if (probabilityCutPoint < 1. / (realSize + 1))
					return this.dPopulation.get(0);
				else
					return this.dPopulation.get(realSize - 1);
			}
			else {
				if (probabilityCutPoint > 0. || probabilityCutPoint < 1.) {
					double position = realSize * probabilityCutPoint;
					if (position % 2 == 0)
						// take elements p-1 and p due to zerobased index
						return (this.dPopulation.get((int) position) + this.dPopulation.get((int) (position + 1))) / 2.;
					else
						// take element p due to zerobased index in combination with upper bound operation in the calculation rule
						return this.dPopulation.get((int) (position));
				}
				else if (probabilityCutPoint == 0.)
					return this.dPopulation.get(0);
				else
					return this.dPopulation.get(realSize - 1);
			}
		}
	}

	public double getQuartile0() {
		return this.dPopulation == null ? this.iPopulation.get(0) : this.dPopulation.get(0);
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
		return this.dPopulation == null ? this.iPopulation.get(this.iPopulation.size() - 1) : this.dPopulation.get(this.dPopulation.size() - 1);
	}

	public double getInterQuartileRange() {
		return getQuantile(.75) - getQuantile(.25);
	}

	public double getQuantileLowerWhisker() {
		final double probabilityCutPoint = .25;
		final double whiskerStartValue = getQuantile(probabilityCutPoint);
		final double whiskerLimitValue = whiskerStartValue - getInterQuartileRange() * 1.5;
		double value = whiskerStartValue;
		if (this.dPopulation == null) {
			for (int i = 0; i < this.iPopulation.size() * probabilityCutPoint; i++) {
				if (this.iPopulation.get(i) >= whiskerLimitValue) {
					// get the corrected value which is crucial for samples
					value = getQuantile((.5 + i) / this.iPopulation.size()); // add .5 due to zerobased index and rule 0<p<1 which implies an index average value
					// take the whisker limit value if the interpolation / estimation value is beyond the limit
					value = value < whiskerLimitValue ? whiskerLimitValue : value;
					break;
				}
			}
		}
		else {
			for (int i = 0; i < this.dPopulation.size() * probabilityCutPoint; i++) {
				if (this.dPopulation.get(i) >= whiskerLimitValue) {
					// get the corrected value which is crucial for samples
					value = getQuantile((.5 + i) / this.dPopulation.size()); // add .5 due to zerobased index and rule 0<p<1 which implies an index average value
					// take the whisker limit value if the interpolation / estimation value is beyond the limit
					value = value < whiskerLimitValue ? whiskerLimitValue : value;
					break;
				}
			}
		}
		return value;
	}

	public double getQuantileUpperWhisker() {
		final double probabilityCutPoint = .75;
		final double whiskerStartValue = getQuantile(probabilityCutPoint);
		final double whiskerLimitValue = whiskerStartValue + getInterQuartileRange() * 1.5;
		double value = whiskerStartValue;
		if (this.dPopulation == null) {
			for (int i = this.iPopulation.size() - 1; i > this.iPopulation.size() * probabilityCutPoint; i--) {
				if (this.iPopulation.get(i) <= whiskerLimitValue) {
					// get the corrected value which is crucial for samples
					value = getQuantile((.5 + i) / this.iPopulation.size()); // add .5 due to zerobased index and rule 0<p<1 which implies an index average value
					// take the whisker limit value if the interpolation / estimation value is beyond the limit
					value = value > whiskerLimitValue ? whiskerLimitValue : value;
					break;
				}
			}
		}
		else {
			for (int i = this.dPopulation.size() - 1; i > this.dPopulation.size() * probabilityCutPoint; i--) {
				if (this.dPopulation.get(i) <= whiskerLimitValue) {
					// get the corrected value which is crucial for samples
					value = getQuantile((.5 + i) / this.dPopulation.size()); // add .5 due to zerobased index and rule 0<p<1 which implies an index average value
					// take the whisker limit value if the interpolation / estimation value is beyond the limit
					value = value > whiskerLimitValue ? whiskerLimitValue : value;
					break;
				}
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
	 * @return size after removing nulls / zeros or 6sigma outliers as defined in the constructor
	 */
	public int getRealSize() {
		return this.dPopulation == null ? this.iPopulation.size() : this.dPopulation.size();
	}

}
