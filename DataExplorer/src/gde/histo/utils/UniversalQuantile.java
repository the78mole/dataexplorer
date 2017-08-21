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
package gde.histo.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.logging.Logger;

import gde.log.Level;

/**
 * Immutable quantile calculation of a probability distribution after removing outliers.
 * Is based on a mergesort and thus avg O(n log n).<br>
 * NB: a 500k records clone + sort takes 45 ms (T is {@code Number} or {@code Spot<Number>}) on ET's machine.
 * @author Thomas Eickert
 */
public final class UniversalQuantile<T extends Number & Comparable<T>> {
	private final static String	$CLASS_NAME							= UniversalQuantile.class.getName();
	private final static Logger	log											= Logger.getLogger($CLASS_NAME);

	/**
	 * Corresponds to the interquartile range (<em>0.25 < p < 0.75</em>)
	 */
	public static final double	BOXPLOT_SIGMA_FACTOR		= 0.674489694;
	/**
	 * Specifies the outlier distance limit ODL from the tolerance interval (<em>ODL = &rho; * TI with &rho; > 0</em>).<br>
	 * Tolerance interval: <em>TI = &plusmn; z * &sigma; with z >= 0</em><br>
	 * Outliers are identified only if they lie beyond this limit.
	 * @see <a href="https://www.google.de/search?q=Tukey+boxplot">Tukey Boxplot</a>
	 */
	public static final double	BOXPLOT_OUTLIER_FACTOR	= 1.5;

	private final boolean				isSample;																										// required for probability calculations from the population
	private final List<T>				trunk;																											// remaining population after removing the elimination members
	private final List<T>				outcasts;																										// values to be eliminated from the population
	private final List<T>				castaways								= new ArrayList<>();								// outlier and outcast members not contained in the trunk

	private T										firstValidElement;
	private T										lastValidElement;
	private Double							sumFigure;
	private Double							avgFigure;
	private Double							sigmaFigure;

	public enum BoxplotItems {
		QUARTILE0, LOWER_WHISKER, QUARTILE1, QUARTILE2, QUARTILE3, UPPER_WHISKER, QUARTILE4
	};

	/**
	 *  Implements the Gauss error function.<br>
	 *  <em>erf(z) = 2 / &radic;&pi;  &int;e<sup>-t&sup2;</sup>,  t = 0..z</em>
	 *  <p>
	 *  Examples:<br>
	 *  {@code erf(1.0) = 0.8427007877600067}         // actual = 0.84270079294971486934 <br>
	 *  {@code Phi(1.0) = 0.8413447386043253}         // actual = 0.8413447460 <br>
	 *  <br>
	 *  {@code erf(-1.0) = -0.8427007877600068} <br>
	 *  {@code Phi(-1.0) = 0.15865526139567465} <br>
	 *  <br>
	 *  {@code erf(3.0) = 0.9999779095015785}         // actual = 0.99997790950300141456 <br>
	 *  {@code Phi(3.0) = 0.9986501019267444} <br>
	 *  <br>
	 *  {@code erf(30.0) = 1.0} <br>
	 *  {@code Phi(30.0) = 1.0} <br>
	 *  <br>
	 *  {@code erf(-30.0) = -1.0} <br>
	 *  {@code Phi(-30.0) = 0.0} <br>
	 *  <br>
	 *  {@code erf(1.0E-20)  = -3.0000000483809686E-8}     // true answer 1.13E-20 <br>
	 *  {@code Phi(1.0E-20)  = 0.49999998499999976} <br>
	 *
	 *  @see <a href="http://introcs.cs.princeton.edu/java/21function/ErrorFunction.java.html">Error Function</a>
	 *  @author Robert Sedgewick
	 *  @author Kevin Wayne
	 *  @author Thomas Eickert
	 */
	private static class ErrorFunction {

		/**
		 * Fractional error in math formula less than {@code 1.2 * 10 ^ -7}.
		 * Although subject to catastrophic cancellation when z in very close to 0.
		 * @param z
		 * @return from Chebyshev fitting formula for erf(z) from Numerical Recipes, 6.2
		 */
		public static double erf(double z) {
			double t = 1.0 / (1.0 + 0.5 * Math.abs(z));
			// use Horner's method
			double ans = 1 - t * Math.exp(-z * z - 1.26551223
					+ t * (1.00002368 + t * (0.37409196 + t * (0.09678418 + t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587 + t * (-0.82215223 + t * (0.17087277))))))))));
			if (z >= 0)
				return ans;
			else
				return -ans;
		}

		/**
		 * fractional error less than <em>x.xx * 10<sup>-4</sup> </em>.
		 * @param z
		 * @return erf(z) acc. to Algorithm 26.2.17 in Abromowitz and Stegun, Handbook of Mathematical
		 */
		public static double erf2(double z) {
			double t = 1.0 / (1.0 + 0.47047 * Math.abs(z));
			double poly = t * (0.3480242 + t * (-0.0958798 + t * (0.7478556)));
			double ans = 1.0 - poly * Math.exp(-z * z);
			if (z >= 0)
				return ans;
			else
				return -ans;
		}

		/**
		 * <em>&Phi;(x) = &frac12; erfc(-x/&radic;2) </em><br>
		 * See Gaussia.java for a better way to compute <em>&Phi;(x)</em>.
		 * @param z
		 * @return the cumulative normal distribution
		 */
		public static double Phi(double z) {
			return 0.5 * (1.0 + erf(z / Math.sqrt(2.0)));
		}

		/**
		 * @param sigmaFactor defines the tolerance interval (<em>TI = &plusmn; z * &sigma; with z >= 0</em>)
		 * @return the probability that a normal deviate lies in the tolerance interval
		 */
		public static double getProbability(double sigmaFactor) {
			return erf(sigmaFactor / Math.sqrt(2.));
		}

		/**
		 * Test client
		 * @param args
		 */
		public static void main(String[] args) {
			double x = Double.parseDouble(args[0]);

			System.out.println(String.format("erf(%f)  = %f", x, ErrorFunction.erf(x))); //$NON-NLS-1$
			System.out.println(String.format("erf2(%f)  = %f", x, ErrorFunction.erf2(x))); //$NON-NLS-1$
			System.out.println(String.format("Phi(%f)  = %f", x, ErrorFunction.Phi(x))); //$NON-NLS-1$
			System.out.println();
		}
	}

	/**
	 * Support standard deviation calculation via parallel streams.
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

		@Override
		public void accept(double value) {
			varTimesN += (value - avg) * (value - avg) * count / ++count; // pls note the counter increment
			avg += (value - avg) / count;
		}

		/**
		 * @param other
		 * @see <a href="http://stats.stackexchange.com/a/56000">Formulae</a>
		 */
		public void combine(StatsHelper other) {
			double tmpAvg = this.avg * count / (count + other.count) + other.avg * other.count / (count + other.count);
			// the next line is also valid for samples as we work with varTimesN which is in fact var times (N-1) for the Bessel corrected version
			varTimesN += other.varTimesN + count * (avg - tmpAvg) * (avg - tmpAvg) + other.count * (other.avg - tmpAvg) * (other.avg - tmpAvg);
			avg = tmpAvg;
			count += other.count;
		}
	}

	/**
	 * Does not remove outliers.
	 * @param population is taken as a sample (for the standard deviation)
	 */
	public UniversalQuantile(List<Spot<T>> population) {
		this(population, 99., 99.);
	}

	/**
	 * @param population is taken as a sample (for the standard deviation)
	 * @param sigmaFactor specifies the tolerance interval <em>TI = &plusmn; z * &sigma; with z >= 0</em>
	 * @param outlierFactor specifies the outlier distance limit ODL from the tolerance interval (<em>ODL = &rho; * TI with &rho; > 0</em>)
	 */
	public UniversalQuantile(List<Spot<T>> population, double sigmaFactor, double outlierFactor) {
		if (population == null || population.isEmpty()) throw new IllegalArgumentException("empty population");
		this.isSample = true;
		this.outcasts = new ArrayList<T>();

		this.trunk = new ArrayList<T>();
		for (Spot<T> spot : population) {
			this.trunk.add(spot.y());
		}

		Collections.sort(this.trunk);

		this.firstValidElement = population.get(0).y();
		this.lastValidElement = population.get(population.size() - 1).y();

		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "" + population.size() + Arrays.toString(population.toArray()));
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "" + this.trunk.size() + Arrays.toString(this.trunk.toArray()));
	}

	/**
	 * @param population
	 * @param isSample true calculates the sample standard deviation
	 */
	public UniversalQuantile(List<T> population, boolean isSample) {
		this(population, isSample, 99., 99., new ArrayList<>());
	}

	/**
	 * @param population
	 * @param isSample true calculates the sample standard deviation
	 * @param sigmaFactor specifies the tolerance interval <em>TI = &plusmn; z * &sigma; with z >= 0</em>
	 * @param outlierFactor specifies the outlier distance limit ODL from the tolerance interval (<em>ODL = &rho; * TI with &rho; > 0</em>)
	 */
	public UniversalQuantile(List<T> population, boolean isSample, double sigmaFactor, double outlierFactor) {
		this(population, isSample, 99., 99., new ArrayList<>());
	}

	/**
	 * @param population
	 * @param isSample true calculates the sample standard deviation
	 * @param sigmaFactor specifies the tolerance interval <em>TI = &plusmn; z * &sigma; with z >= 0</em>
	 * @param outlierFactor specifies the outlier distance limit ODL from the tolerance interval (<em>ODL = &rho; * TI with &rho; > 0</em>)
	 * @param outcasts holds list members which are eliminated before the quantiles calculation
	 */
	public UniversalQuantile(List<T> population, boolean isSample, double sigmaFactor, double outlierFactor, List<T> outcasts) {
		if (population == null || population.isEmpty()) throw new IllegalArgumentException("empty population");
		if (outcasts == null) throw new IllegalArgumentException("outcast is null");
		this.isSample = isSample;
		this.outcasts = new ArrayList<T>(outcasts);

		if (outcasts.isEmpty()) {
			this.trunk = new ArrayList<T>(population);
		}
		else {
			this.trunk = new ArrayList<T>();
			for (T element : population) {
				if (outcasts.contains(element))
					this.castaways.add(element);
				else {
					this.trunk.add(element);
				}
			}
		}

		Collections.sort(this.trunk);

		// remove outliers except: if all outliers have the same value we expect them to carry a real value (e.g. height 0 m)
		double outlierProbability = (1 - ErrorFunction.getProbability(sigmaFactor)) / 2.;
		double extremumRange = (getQuantile(1. - outlierProbability) - getQuantile(outlierProbability)) * outlierFactor;
		while (this.trunk.size() > 0 && this.trunk.get(0).doubleValue() < getQuantile(outlierProbability) - extremumRange) {
			this.castaways.add(this.trunk.get(0));
			this.trunk.remove(0);
		}
		while (this.trunk.size() > 0 && this.trunk.get(this.trunk.size() - 1).doubleValue() > getQuantile(1. - outlierProbability) + extremumRange) {
			this.castaways.add(this.trunk.get(this.trunk.size() - 1));
			this.trunk.remove(this.trunk.size() - 1);
		}
		if (this.trunk.isEmpty()) throw new UnsupportedOperationException("empty trunk");

		this.firstValidElement = null;
		// walk forward and get the first element which is not in the outcast / outlier lists
		for (int i = 0; i < population.size(); i++) {
			T t = population.get(i);
			if (!this.castaways.contains(t)) {
				this.firstValidElement = t;
				break;
			}
		}
		this.lastValidElement = null;
		// walk backward and get the first element which is not in the outcast / outlier lists
		for (int j = population.size() - 1; j >= 0; j--) {
			T t = population.get(j);
			if (!this.castaways.contains(t)) {
				this.lastValidElement = t;
				break;
			}
		}

		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "" + population.size() + Arrays.toString(population.toArray()));
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "" + this.trunk.size() + Arrays.toString(this.trunk.toArray()));
	}

	/**
	 * @return the value of the first element after outcast elimination and removing the outliers
	 */
	public double getFirstFigure() {
		return this.firstValidElement.doubleValue();
	}

	/**
	 * @return the value of the last element after outcast elimination and removing the outliers
	 */
	public double getLastFigure() {
		return this.lastValidElement.doubleValue();
	}

	/**
	 * @return the value of the maximum element before outcast elimination and removing the outliers
	 */
	public double getPopulationMaxFigure() {
		double realMax = getQuartile4();
		for (T t : this.castaways) {
			realMax = Math.max(realMax, t.doubleValue());
		}
		return realMax;
	}

	/**
	 * @return the value of the minimum element before outcast elimination and removing the outliers
	 */
	public double getPopulationMinFigure() {
		double realMin = getQuartile0();
		for (T t : this.castaways) {
			realMin = Math.min(realMin, t.doubleValue());
		}
		return realMin;
	}

	public double getSumFigure() {
		if (this.sumFigure == null) {
			this.sumFigure = this.trunk.parallelStream().mapToDouble(x -> x.doubleValue()).sum();
		}
		return this.sumFigure;
	}

	public double getAvgOBS() {
		double avg = 0;
		for (int i = 0; i < this.trunk.size(); i++) {
			double value = this.trunk.get(i).doubleValue();
			avg += (value - avg) / (i + 1);
		}
		return avg;
	}

	public double getAvgFigure() {
		if (this.avgFigure == null) {
			this.avgFigure = this.trunk.parallelStream().mapToDouble(x -> x.doubleValue()).average().getAsDouble();
		}
		return this.avgFigure;
	}

	public double getSigmaOBS() {
		double avg = getAvgFigure();
		double varTimesN = 0;
		for (int i = 0; i < this.trunk.size(); i++) {
			double value = this.trunk.get(i).doubleValue();
			varTimesN += (value - avg) * (value - avg);
		}
		return this.trunk.size() > 0 ? Math.sqrt(varTimesN / (this.isSample ? this.trunk.size() - 1 : this.trunk.size())) : 0;
	}

	public double getSigmaRunningOBS() {
		double avg = 0;
		double varTimesN = 0;
		int count = 0;
		for (int i = 0; i < this.trunk.size(); i++) {
			double value = this.trunk.get(i).doubleValue();
			varTimesN += (value - avg) * (value - avg) * count / ++count; // pls note the counter increment
			avg += (value - avg) / count;
		}
		return this.trunk.size() > 0 ? Math.sqrt(varTimesN / (this.isSample ? this.trunk.size() - 1 : this.trunk.size())) : 0;
	}

	public double getSigmaFigure() {
		if (this.sigmaFigure == null) {
			this.sigmaFigure = this.trunk.parallelStream().mapToDouble(t -> t.doubleValue()).collect(StatsHelper::new, StatsHelper::accept, StatsHelper::combine).getSigma(this.isSample);
		}
		return this.sigmaFigure;
	}

	/**
	 * The sample quantile is calculated according to R-6, SAS-4, SciPy-(0,0), Maple-5 which is piecewise linear and symmetric.
	 * @param probabilityCutPoint as value between 0 and 1
	 * @return the quantile for the population or for a sample
	 */
	public double getQuantile(double probabilityCutPoint) {
		int pSize = this.trunk.size();
		if (this.isSample) {
			if (probabilityCutPoint >= 1. / (pSize + 1) && probabilityCutPoint < (double) pSize / (pSize + 1)) {
				double position = (pSize + 1) * probabilityCutPoint;
				return this.trunk.get((int) position - 1).doubleValue() + (position - (int) position) * (this.trunk.get((int) position).doubleValue() - this.trunk.get((int) position - 1).doubleValue());
			}
			else if (probabilityCutPoint < 1. / (pSize + 1))
				return this.trunk.get(0).doubleValue();
			else
				return this.trunk.get(pSize - 1).doubleValue();
		}
		else {
			if (probabilityCutPoint > 0. && probabilityCutPoint < 1.) {
				double position = pSize * probabilityCutPoint;
				if (position % 2 == 0)
					return (this.trunk.get((int) position).doubleValue() + this.trunk.get((int) (position + 1)).doubleValue()) / 2.;
				else
					return this.trunk.get((int) (position)).doubleValue();
			}
			else if (probabilityCutPoint == 0.)
				return this.trunk.get(0).doubleValue();
			else
				return this.trunk.get(pSize - 1).doubleValue();
		}
	}

	public double getQuartile0() {
		return this.trunk.get(0).doubleValue();
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
		return this.trunk.get(this.trunk.size() - 1).doubleValue();
	}

	public double getInterQuartileRange() {
		return getQuantile(.75) - getQuantile(.25);
	}

	public double getQuantileLowerWhisker() {
		final double probabilityCutPoint = .25;
		final double whiskerStartValue = getQuantile(probabilityCutPoint);
		final double whiskerLimitValue = whiskerStartValue - getInterQuartileRange() * 1.5;
		double value = whiskerStartValue;
		for (int i = 0; i < this.trunk.size() * probabilityCutPoint; i++) {
			if (this.trunk.get(i).doubleValue() >= whiskerLimitValue) {
				// get the corrected value which is crucial for samples
				value = getQuantile((.5 + i) / this.trunk.size()); // add .5 due to zerobased index and rule 0<p<1 which implies an index average value
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
		for (int i = this.trunk.size() - 1; i > this.trunk.size() * probabilityCutPoint; i--) {
			if (this.trunk.get(i).doubleValue() <= whiskerLimitValue) {
				// get the corrected value which is crucial for samples
				value = getQuantile((.5 + i) / this.trunk.size()); // add .5 due to zerobased index and rule 0<p<1 which implies an index average value
				// take the whisker limit value if the interpolation / estimation value is beyond the limit
				value = value > whiskerLimitValue ? whiskerLimitValue : value;
				break;
			}
		}
		return value;
	}

	public double[] getTukeyBoxPlot() {
		double[] values = new double[7];
		values[BoxplotItems.QUARTILE0.ordinal()] = getQuartile0();
		values[BoxplotItems.LOWER_WHISKER.ordinal()] = getQuantileLowerWhisker();
		values[BoxplotItems.QUARTILE1.ordinal()] = getQuartile1();
		values[BoxplotItems.QUARTILE2.ordinal()] = getQuartile2();
		values[BoxplotItems.QUARTILE3.ordinal()] = getQuartile3();
		values[BoxplotItems.UPPER_WHISKER.ordinal()] = getQuantileUpperWhisker();
		values[BoxplotItems.QUARTILE4.ordinal()] = getQuartile4();
		return values;
	}

	/**
	 * @return the population size after eliminations and removing outliers
	 */
	public int getSize() {
		return this.trunk.size();
	}

	/**
	 * @return the outliers based on the sigmaFactor and the outlierFactor
	 */
	public List<T> getOutliers() {
		List<T> outliers = new ArrayList<>(this.castaways);
		outliers.removeAll(this.outcasts);
		return outliers;
	}

}