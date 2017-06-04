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
package gde.utils;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *  It fits a straight line <em>y</em> = &alpha; + &beta; <em>x</em>, (where <em>y</em> is the response variable, <em>x</em> is the independent variable,
 *  &alpha; is the <em>y-intercept</em>, and &beta; is the <em>slope</em>).
 *  It includes the coefficient of determination <em>R</em><sup>2</sup> and the standard deviation of the estimates for the slope.<br>
 *  Also supports quadratic regression of the best-fit parabola <em>y = &alpha; + &beta; x + &gamma; x<sup>2</sup></em>.<br>
 * @see <a href="http://algs4.cs.princeton.edu/14analysis/LinearRegression.java.html">Basic algorithm</a>
 * @see <a href="http://www.stksachs.uni-leipzig.de/tl_files/media/pdf/lehrbuecher/informatik/Regressionsanalyse.pdf">Quadratic regression</a>
 */
public class SingleResponseRegression { // todo harmonize with /DataExplorer/src/gde/utils/LinearRegression.java

	public enum RegressionType {
		LINEAR, QUADRATIC
	};

	private int									n;																			// size with y nulls stripped off
	private final List<Double>	xx		= new ArrayList<>();							// original values with y(!) nulls stripped off
	private final List<Double>	yy		= new ArrayList<>();							// original values with y nulls stripped off
	private final List<Double>	zz		= new ArrayList<>();							// square(xx)

	private double							xbar	= 0.0, ybar = 0.0, zbar = 0.0;		// averages
	private double							xxbar	= 0.0, yybar = 0.0, xybar = 0.0;	// (co-)variance times n
	private double							zzbar	= 0.0, zxbar = 0.0, zybar = 0.0;	// parabolic (co-)variance times n

	private double							rss		= 0.0;														// residual sum of squares
	private double							ssr		= 0.0;														// regression (explained) sum of squares

	/**
	 * @param points with <em>x<sub>i</sub></em> (independent variable) and <em>y<sub>i</sub></em> (response variable)
	 * @param type
	 */
	public SingleResponseRegression(Collection<Point> points, RegressionType type) {
		double sumx = 0.0, sumy = 0.0, sumz = 0.0;
		// first pass: uniform lists
		for (Point point : points) {
			this.xx.add(point.getX());
			this.yy.add(point.getY());
			sumx += point.getX();
			sumy += point.getY();

			// build quadratic transformation into third parameter
			if (type == RegressionType.QUADRATIC) {
				this.zz.add(point.getX() * point.getX());
				sumz += point.getX() * point.getX();
			}
		}
		// second pass: averages
		this.n = this.xx.size();
		if (this.n >= 1) {
			this.xbar = sumx / this.n;
			this.ybar = sumy / this.n;
			this.zbar = sumz / this.n;
		}

		setCovariances();
	}

	/**
	 * @param points with <em>x<sub>i</sub></em> (independent variable) and <em>y<sub>i</sub></em> (response variable)
	 * @param type
	 */
	public SingleResponseRegression(List<Point2D.Double> points, RegressionType type) {
		double sumx = 0.0, sumy = 0.0, sumz = 0.0;
		// first pass: uniform lists
		for (Point2D.Double point : points) {
			this.xx.add(point.getX());
			this.yy.add(point.getY());
			sumx += point.getX();
			sumy += point.getY();

			// build quadratic transformation into third parameter
			if (type == RegressionType.QUADRATIC) {
				this.zz.add(point.getX() * point.getX());
				sumz += point.getX() * point.getX();
			}
		}
		// second pass: averages
		this.n = this.xx.size();
		if (this.n >= 1) {
			this.xbar = sumx / this.n;
			this.ybar = sumy / this.n;
			this.zbar = sumz / this.n;
		}

		setCovariances();
	}

	/**
	 * third pass: covariances
	 */
	private void setCovariances() {
		if (this.n >= 1) {
			for (int i = 0; i < this.n; i++) {
				this.xxbar += (this.xx.get(i) - this.xbar) * (this.xx.get(i) - this.xbar);
				this.yybar += (this.yy.get(i) - this.ybar) * (this.yy.get(i) - this.ybar);
				this.xybar += (this.xx.get(i) - this.xbar) * (this.yy.get(i) - this.ybar);
			}

			if (!this.zz.isEmpty()) {
				for (int i = 0; i < this.n; i++) {
					this.zzbar += (this.zz.get(i) - this.zbar) * (this.zz.get(i) - this.zbar);
					this.zxbar += (this.zz.get(i) - this.zbar) * (this.xx.get(i) - this.xbar);
					this.zybar += (this.zz.get(i) - this.zbar) * (this.yy.get(i) - this.ybar);
				}
			}
		}
	}

	/**
	 * forth pass: error estimation
	 */
	private void setErrorSums() {
		if (this.n >= 2) {
			for (int i = 0; i < this.n; i++) {
				double fit = getResponse(this.xx.get(i));
				this.rss += (fit - this.yy.get(i)) * (fit - this.yy.get(i));
				this.ssr += (fit - this.ybar) * (fit - this.ybar);
			}
		}
	}

	/**
	* @return the <em>y</em>-intercept &alpha; of the best-fit line <em>y = &alpha; + &beta; x</em>
	*/
	public double getIntercept() {
		return this.ybar - getSlope() * this.xbar;
	}

	/**
	* @return the slope &beta; of the best-fit line <em>y</em> = &alpha; + &beta; <em>x</em>
	*/
	public double getSlope() {
		if (this.n < 2)
			return 0;
		else
			return this.xybar / this.xxbar;
	}

	/**
	 * @return the coefficient of determination <em>R</em><sup>2</sup>, which is a real number between 0 and 1
	 */
	public double getR2() {
		if (this.ssr == 0.) setErrorSums();

		if (this.n < 2)
			return 0;
		else {
			return 1. - this.rss / this.yybar;
			// ET 02.06.2017 changed : the original line was not valid for quadratic regression   -> return this.ssr / this.yybar;
		}
	}

	/**
	* @return the standard error of the estimate for the intercept
	*/
	public double getInterceptStdErr() {
		if (this.ssr == 0.) setErrorSums();

		if (this.n <= 2)
			return 0;
		else {
			double svar = this.rss / (this.n - 2);
			double svar1 = svar / this.xxbar;
			return Math.sqrt(svar / this.n + this.xbar * this.xbar * svar1);
		}
	}

	/**
	* @return the standard error of the estimate for the slope
	*/
	public double getSlopeStdErr() {
		if (this.ssr == 0.) setErrorSums();

		if (this.n <= 2)
			return 0;
		else {
			double svar = this.rss / (this.n - 2);
			return Math.sqrt(svar / this.xxbar);
		}
	}

	/**
	* @return the <em>y</em>-curvature &gamma; of the best-fit parabola <em>y = &alpha; + &beta; x + &gamma; x<sup>2</sup></em>
	*/
	public double getGamma() {
		if (this.zz.isEmpty()) throw new UnsupportedOperationException();
		double numerator = this.zybar * this.xxbar - this.xybar * this.zxbar;
		double denominator = this.zzbar * this.xxbar - this.zxbar * this.zxbar;

		return numerator / denominator;
	}

	/**
	* @return the <em>y</em>-slope &beta; of the best-fit parabola <em>y = &alpha; + &beta; x + &gamma; x<sup>2</sup></em>
	*/
	public double getBeta() {
		if (this.zz.isEmpty()) throw new UnsupportedOperationException();
		double numerator = this.xybar - getGamma() * this.zxbar;
		return numerator / this.xxbar;
	}

	/**
	* @return the <em>y</em>-intercept &alpha; of the best-fit parabola <em>y = &alpha; + &beta; x + &gamma; x<sup>2</sup></em>
	*/
	public double getAlpha() {
		if (this.zz.isEmpty()) throw new UnsupportedOperationException();
		return this.ybar - getGamma() * this.zbar - getBeta() * this.xbar;
	}

	/**
	* @param  xValue the independent variable
	* @return the expected response {@code y} given the value of the independent variable {@code x} or 0 if no non-null values exist
	*/
	public double getResponse(double xValue) {
		if (this.n < 1)
			return 0;
		else if (this.zz.isEmpty())
			return getSlope() * xValue + getIntercept();
		else
			return getAlpha() + getBeta() * xValue + getGamma() * xValue * xValue;
	}

	/**
	 * @return the first independent value {@code x} or 0 if no non-null values exist
	 */
	public double getFirstRegressor() {
		if (this.n < 1)
			return 0;
		else
			return this.xx.get(0);
	}

	/**
	* @return the delta of the {@code x} bounds values or 0 if no non-null values exist
	*/
	public double getRegressorDelta() {
		if (this.n < 1)
			return 0;
		else
			return this.xx.get(this.xx.size() - 1) - this.xx.get(0);
	}

	/**
	* @return the delta of the {@code y} bounds values or 0 if no non-null values exist
	*/
	public double getDelta() {
		if (this.n < 1)
			return 0;
		else
			return this.yy.get(this.yy.size() - 1) - this.yy.get(0);
	}

	/**
	* @return the average of the y values
	*/
	public double getAvg() {
		if (this.n < 1)
			return 0;
		else
			return this.ybar;
	}

	/**
	* @return the standard deviation of the y values
	*/
	public double getSigma() {
		if (this.n < 2)
			return 0;
		else
			return Math.sqrt(this.yybar / (this.n - 1));
	}

	public int getRealSize() {
		return this.n;
	}

	/**
	* @return a string representation of the regression results
	*/
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (this.zz.isEmpty()) {
			s.append(String.format("%.4f n + %.4f", getSlope(), getIntercept()));
			s.append("  (R^2 = " + String.format("%.3f", getR2()) + ")");
		}
		else {
			s.append(String.format("%.4f + %.4f n + %.4f n2", getGamma(), getBeta(), getAlpha()));
		}
		return s.toString();
	}

}
