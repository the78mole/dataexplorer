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

package gde.data;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import gde.GDE;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.device.ScoreGroupType;
import gde.device.SettlementType;
import gde.device.StatisticsType;
import gde.device.TrailTypes;
import gde.device.resource.DeviceXmlResource;
import gde.histocache.HistoVault;
import gde.log.Level;
import gde.utils.HistoTimeLine;
import gde.utils.Quantile;
import gde.utils.Quantile.Fixings;
import gde.utils.StringHelper;
import gde.utils.TimeLine;

/**
 * holds histo data points of one measurement or settlement; score points are a third option.
 * a histo data point holds one aggregated value (e.g. max, avg, quantile).
 * supports multiple curves (trail suites).
 * @author Thomas Eickert
 */
public class TrailRecord extends Record { // todo maybe a better option is to create a common base class for Record and TrailRecord.
	private final static String		$CLASS_NAME							= TrailRecord.class.getName();
	private final static long			serialVersionUID				= 110124007964748556L;
	private final static Logger		log											= Logger.getLogger($CLASS_NAME);

	public final static String		TRAIL_TEXT_ORDINAL			= "_trailTextOrdinal";						// reference to the selected trail //$NON-NLS-1$

	private final TrailRecordSet	parentTrail;
	private final MeasurementType	measurementType;																					// measurement / settlement / scoregroup are options
	private final SettlementType	settlementType;																						// measurement / settlement / scoregroup are options
	private final ScoreGroupType	scoreGroupType;																						// measurement / settlement / scoregroup are options
	private int										trailTextSelectedIndex	= -1;															// user selection from applicable trails, is saved in the graphics template
	private List<String>					applicableTrailsTexts;																		// the user may select one of these entries
	private List<Integer>					applicableTrailsOrdinals;																	// maps all applicable trails in order to convert the user selection into a valid trail
	private TrailRecord[]					trailRecordSuite;																					// holds data points in case of trail suites
	private double								factor									= Double.MIN_VALUE;
	private double								offset									= Double.MIN_VALUE;
	private double								reduction								= Double.MIN_VALUE;

	final DeviceXmlResource				xmlResource							= DeviceXmlResource.getInstance();
	private LinearRegression			linearRegression				= null;
	private Quantile							quantile								= null;

	/**
	 *  Performs a simple linear regression on an set of <em>n</em> data points (<em>y<sub>i</sub></em>, <em>x<sub>i</sub></em>).
	 *  That is, it fits a straight line <em>y</em> = &alpha; + &beta; <em>x</em>, (where <em>y</em> is the response variable, <em>x</em> is the predictor variable,
	 *  &alpha; is the <em>y-intercept</em>, and &beta; is the <em>slope</em>) that minimizes the sum of squared residuals of the linear regression model.
	 *  It includes the coefficient of determination <em>R</em><sup>2</sup> and the standard deviation of the estimates for the slope and <em>y</em>-intercept.
	 *  Source: http://algs4.cs.princeton.edu/14analysis/LinearRegression.java.html
	 *  @author Robert Sedgewick
	 *  @author Kevin Wayne
	 *  @author Thomas Eickert
	 */
	public class LinearRegression { // todo harmonize with /DataExplorer/src/gde/utils/LinearRegression.java
		private final int						n;																			// size with y nulls stripped off
		private final List<Double>	xx		= new ArrayList<>();							// original values with y(!) nulls stripped off
		private final List<Double>	yy		= new ArrayList<>();							// original values with y nulls stripped off

		private double							xbar	= 0.0, ybar = 0.0;								// averages
		private double							xxbar	= 0.0, yybar = 0.0, xybar = 0.0;	// (co-)variance times n
		private double							rss		= 0.0;														// residual sum of squares
		private double							ssr		= 0.0;														// regression sum of squares

		/**
		  * Performs a linear regression on the data points {@code (y[i], x[i])}.
		  * @param  x the values of the predictor variable
		  * @param  y the corresponding values of the response variable
		  * @throws IllegalArgumentException if the lengths of the two arrays are not equal or if n<=2
		  */
		public LinearRegression(double[] x, double[] y) {
			this(DoubleStream.of(x).boxed().collect(Collectors.toList()), DoubleStream.of(y).boxed().collect(Collectors.toList()));
		}

		/**
		  * Performs a linear regression on the data points {@code (y[i], x[i])}.
		  * Removes  {@code (y[i], x[i])} if {@code y[i]} is null.
		  * @param  x the values of the predictor variable
		  * @param  y the corresponding values of the response variable (may contain nulls)
		  * @throws IllegalArgumentException if the length of non-null elements is less than 2  or if the length is not equal
		  */
		public LinearRegression(List<Double> x, List<Double> y) {
			if (x.size() != y.size()) {
				throw new IllegalArgumentException("collection lengths are not equal");
			}

			// first pass: eliminate y nulls
			double sumx = 0.0, sumy = 0.0; // , sumx2 = 0.0;
			for (int i = 0; i < x.size(); i++) {
				if (y.get(i) != null) {
					this.xx.add(x.get(i));
					this.yy.add(y.get(i));
					sumx += x.get(i);
					// sumx2 += x.get(i) * x.get(i);
					sumy += y.get(i);
				}
			}
			this.n = this.xx.size();
			if (this.n >= 1) {
				this.xbar = sumx / this.n;
				this.ybar = sumy / this.n;

				// second pass: covariances
				for (int i = 0; i < this.n; i++) {
					this.xxbar += (this.xx.get(i) - this.xbar) * (this.xx.get(i) - this.xbar);
					this.yybar += (this.yy.get(i) - this.ybar) * (this.yy.get(i) - this.ybar);
					this.xybar += (this.xx.get(i) - this.xbar) * (this.yy.get(i) - this.ybar);
				}

				if (this.n >= 2) {
					// third pass: error estimation
					for (int i = 0; i < this.n; i++) {
						double fit = getResponse(this.xx.get(i));
						this.rss += (fit - this.yy.get(i)) * (fit - this.yy.get(i));
						this.ssr += (fit - this.ybar) * (fit - this.ybar);
					}
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
		 * @return the coefficient of determination <em>R</em><sup>2</sup>,
		 *         which is a real number between 0 and 1
		 */
		public double getR2() {
			if (this.n < 2)
				return 0;
			else
				return this.ssr / this.yybar;
		}

		/**
		* @return the standard error of the estimate for the intercept
		*/
		public double getInterceptStdErr() {
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
			if (this.n <= 2)
				return 0;
			else {
				double svar = this.rss / (this.n - 2);
				return Math.sqrt(svar / this.xxbar);
			}
		}

		/**
		* @param  xValue the predictor variable
		* @return the expected response {@code y} given the value of the predictor variable {@code x} or 0 if no non-null values exist
		*/
		public double getResponse(double xValue) {
			if (this.n < 1)
				return 0;
			else
				return getSlope() * xValue + getIntercept();
		}

		/**
		 * @return the first value {@code x} or 0 if no non-null values exist
		 */
		public double getFirstPredictor() {
			if (this.n < 1)
				return 0;
			else
				return this.xx.get(0);
		}

		/**
		* @return the delta of the {@code x} bounds values or 0 if no non-null values exist
		*/
		public double getPredictorDelta() {
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
		* @return a string representation of the simple linear regression model,  including the best-fit line and the coefficient of determination  <em>R</em><sup>2</sup>
		*/
		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append(String.format("%.4f n + %.4f", getSlope(), getIntercept()));
			s.append("  (R^2 = " + String.format("%.3f", getR2()) + ")");
			return s.toString();
		}

	}

	/**
	 * creates a vector for a measurementType to hold data points.
	 * @param newDevice
	 * @param newOrdinal
	 * @param measurementType
	 * @param parentTrail
	 */
	public TrailRecord(IDevice newDevice, int newOrdinal, String newName, MeasurementType measurementType, TrailRecordSet parentTrail, int initialCapacity) {
		super(newDevice, newOrdinal, newName, measurementType.getSymbol(), measurementType.getUnit(), measurementType.isActive(), null, measurementType.getProperty(), initialCapacity);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, measurementType.getName() + " TrailRecord(IDevice newDevice, int newOrdinal, MeasurementType measurementType, TrailRecordSet parentTrail)"); //$NON-NLS-1$
		this.parentTrail = parentTrail;
		super.parent = parentTrail;
		this.measurementType = measurementType;
		this.settlementType = null;
		this.scoreGroupType = null;
	}

	/**
	 * creates a vector for a settlementType to hold data points.
	 * @param newDevice
	 * @param newOrdinal
	 * @param settlementType
	 * @param parentTrail
	 */
	public TrailRecord(IDevice newDevice, int newOrdinal, String newName, SettlementType settlementType, TrailRecordSet parentTrail, int initialCapacity) {
		super(newDevice, newOrdinal, newName, settlementType.getSymbol(), settlementType.getUnit(), settlementType.isActive(), null, settlementType.getProperty(), initialCapacity);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, settlementType.getName() + " TrailRecord(IDevice newDevice, int newOrdinal, SettlementType settlementType, TrailRecordSet parentTrail)"); //$NON-NLS-1$
		this.parentTrail = parentTrail;
		super.parent = parentTrail;
		this.measurementType = null;
		this.settlementType = settlementType;
		this.scoreGroupType = null;
	}

	/**
	 * creates a vector for a scoregroupType to hold all scores of a scoregroup.
	 * the scores are not related to time steps.
	 * @param newDevice
	 * @param newOrdinal
	 * @param scoregroupType
	 * @param parentTrail
	 */
	public TrailRecord(IDevice newDevice, int newOrdinal, String newName, ScoreGroupType scoregroupType, TrailRecordSet parentTrail, int initialCapacity) {
		super(newDevice, newOrdinal, newName, scoregroupType.getSymbol(), scoregroupType.getUnit(), scoregroupType.isActive(), null, scoregroupType.getProperty(), initialCapacity);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, scoregroupType.getName() + " TrailRecord(IDevice newDevice, int newOrdinal, ScoregroupType scoregroupType, TrailRecordSet parentTrail)"); //$NON-NLS-1$
		this.parentTrail = parentTrail;
		super.parent = parentTrail;
		this.measurementType = null;
		this.settlementType = null;
		this.scoreGroupType = scoregroupType;
	}

	@Override
	@Deprecated
	public synchronized Record clone() {
		throw new UnsupportedOperationException("clone"); //$NON-NLS-1$
	}

	@Override
	@Deprecated
	public Record clone(String newName) {
		throw new UnsupportedOperationException("clone"); //$NON-NLS-1$
	}

	@Override
	@Deprecated
	public Record clone(int dataIndex, boolean isFromBegin) {
		throw new UnsupportedOperationException("clone"); //$NON-NLS-1$
	}

	@Override
	@Deprecated
	public Point getDisplayPoint(int measurementPointIndex, int xDisplayOffset, int yDisplayOffset) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public Point getGPSDisplayPoint(int measurementPointIndex, int xDisplayOffset, int yDisplayOffset) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public synchronized Integer set(int index, Integer point) {
		throw new UnsupportedOperationException(" " + index + " " + point); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * take those data points from the histo vault which are assigned to the selected trail type.
	 * supports trail suites.
	 * @param histoVault
	 */
	public void add(HistoVault histoVault) {
		if (this.applicableTrailsOrdinals.size() != this.applicableTrailsTexts.size()) {
			throw new UnsupportedOperationException(String.format("%,3d %,3d %,3d", this.applicableTrailsOrdinals.size(), TrailTypes.getPrimitives().size(), this.applicableTrailsTexts.size())); //$NON-NLS-1$
		}
		if (this.trailRecordSuite == null) { // ???
			super.add(null);
		}
		else if (!this.isTrailSuite()) {
			if (this.isMeasurement())
				super.add(histoVault.getMeasurementPoint(this.ordinal, this.getTrailOrdinal()));
			else if (this.isSettlement())
				super.add(histoVault.getSettlementPoint(this.settlementType.getSettlementId(), this.getTrailOrdinal()));
			else if (this.isScoreGroup()) {
				if (log.isLoggable(Level.FINEST))
					log.log(Level.FINEST, String.format(" %s trail %3d  %s %s", this.getName(), this.getTrailOrdinal(), histoVault.getVaultFileName(), histoVault.getLogFilePath())); //$NON-NLS-1$
				super.add(histoVault.getScorePoint(this.getTrailOrdinal()));
			}
			else
				throw new UnsupportedOperationException("length == 1"); //$NON-NLS-1$
		}
		else {
			int minVal = Integer.MAX_VALUE, maxVal = Integer.MIN_VALUE; // min/max depends on all values of the suite
			int masterPoint = 0; // this is the basis value for adding or subtracting standard deviations
			boolean summationSign = false; // false means subtract, true means add
			for (TrailRecord trailRecord : this.trailRecordSuite) {
				Integer point;
				if (this.isMeasurement())
					point = histoVault.getMeasurementPoint(trailRecord.ordinal, trailRecord.getTrailOrdinal());
				else if (this.isSettlement())
					point = histoVault.getSettlementPoint(trailRecord.settlementType.getSettlementId(), trailRecord.getTrailOrdinal());
				else if (this.isScoreGroup()) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, String.format(" %s trail %3d  %s %s", trailRecord.getName(), this.getTrailOrdinal(), histoVault.getLogFilePath())); //$NON-NLS-1$
					point = histoVault.getScorePoint(this.getTrailOrdinal());
				}
				else
					throw new UnsupportedOperationException("length > 1"); //$NON-NLS-1$

				if (point != null) { // trailRecord.getMinValue() is zero if trailRecord.size() == 0 or only nulls have been added
					if (trailRecord.isSuiteForSummation()) {
						point = summationSign ? masterPoint + 2 * point : masterPoint - 2 * point;
						summationSign = !summationSign; // toggle the add / subtract mode
					}
					else {
						masterPoint = point; // use in the next iteration if summation is necessary, e.g. avg+2*sd
						summationSign = false;
					}
					trailRecord.add(point);
					minVal = Math.min(minVal, trailRecord.getRealMinValue());
					maxVal = Math.max(maxVal, trailRecord.getRealMaxValue());
				}
				else {
					trailRecord.add(point);
				}
				log.log(Level.FINEST, trailRecord.getName() + " trail ", trailRecord); //$NON-NLS-1$
			}
			if (minVal != Integer.MAX_VALUE && maxVal != Integer.MIN_VALUE) {
				this.setMinMax(minVal, maxVal);
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "setMinMax :  " + minVal + "," + maxVal); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		log.log(Level.FINEST, this.getName() + " trail ", this); //$NON-NLS-1$

	}

	/**
	 * query the values for display.
	 * supports multiple entries for the same x axis position.
	 * @param timeLine
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @return point time, value; null if the trail record value is null
	 */
	public Point[] getDisplayPoints(HistoTimeLine timeLine, int xDisplayOffset, int yDisplayOffset) {
		List<Point> points = new ArrayList<>();
		double tmpOffset = super.minDisplayValue * 1 / super.syncMasterFactor;
		for (int i = 0; i < this.parent.timeStep_ms.size(); i++) {
			if (super.realRealGet(i) != null) {
				points.add(new Point(xDisplayOffset + timeLine.getScalePositions().get((long) this.parent.timeStep_ms.getTime_ms(i)),
						yDisplayOffset - (int) (((super.realRealGet(i) / 1000.0) - tmpOffset) * super.displayScaleFactorValue)));
			}
			else {
				points.add(null);
			}
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, Arrays.toString(points.toArray()));
		return points.toArray(new Point[0]);
	}

	public String getHistoTableRowText() {
		return this.getUnit().length() > 0 ? (this.getNameReplacement() + GDE.STRING_BLANK_LEFT_BRACKET + this.getUnit() + GDE.STRING_RIGHT_BRACKET).intern() : this.getNameReplacement().intern();
	}

	/**
	 * query the values for display.
	 * @param timeLine
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @return point time, value; null if the trail record value is null
	 */
	public Point[] getGpsDisplayPoints(HistoTimeLine timeLine, int xDisplayOffset, int yDisplayOffset) {
		Point[] points = new Point[timeLine.getScalePositions().size()];
		int i = 0;
		Integer value = 0;
		final double tmpOffset = super.minDisplayValue * 1 / super.syncMasterFactor; // minDisplayValue is GPS DD format * 1000
		for (Integer xPos : timeLine.getScalePositions().values()) {
			if ((value = super.realRealGet(i)) != null) {
				final double decimalDegreeValue = value / 1000000 + value % 1000000 / 600000.;
				points[i] = new Point(xDisplayOffset + xPos, yDisplayOffset - (int) ((decimalDegreeValue * 1000. - tmpOffset) * super.displayScaleFactorValue));
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
	 * as suite records are not synchronized we need a separate method for them.
	 * is based on DMM record values.
	 * @param timeLine
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @param drawAreaHeight - used to calculate the displayScaleFactorValue to set
	 * @return point time, value; null if the trail record value is null
	 */
	public Point[] getSuiteGpsDisplayPoints(HistoTimeLine timeLine, int xDisplayOffset, int yDisplayOffset, int drawAreaHeight) {
		Point[] points = new Point[timeLine.getScalePositions().size()];
		int i = 0;
		Integer value = 0;
		final double tmpOffset = super.minScaleValue; // in decimal degrees
		final double tmpDisplayScaleFactor = (drawAreaHeight * 1.) / (this.maxScaleValue - this.minScaleValue);
		for (Integer xPos : timeLine.getScalePositions().values()) {
			if ((value = super.realRealGet(i)) != null) {
				final double decimalDegreeValue = value / 1000000 + value % 1000000 / 600000.;
				points[i] = new Point(xDisplayOffset + xPos, yDisplayOffset - (int) ((decimalDegreeValue - tmpOffset) * tmpDisplayScaleFactor));
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
	 * get all calculated and formated data table points.
	 * @return record name and trail text followed by formatted values as string array
	 */
	public String[] getHistoTableRow() {
		final TrailRecord masterRecord = this.getTrailRecordSuite()[0]; // the master record is always available and is in case of a single suite identical with this record
		String[] dataTableRow = new String[masterRecord.size() + 2];
		dataTableRow[0] = getHistoTableRowText().intern();
		dataTableRow[1] = this.getTrailText().intern();
		if (!this.isTrailSuite()) {
			if (this.settings.isXAxisReversed()) {
				for (int i = 0; i < masterRecord.size(); i++)
					dataTableRow[i + 2] = masterRecord.realRealGet(i) != null ? masterRecord.getFormattedTableValue(i) : GDE.STRING_STAR;
			}
			else {
				for (int i = 0, j = masterRecord.size() - 1; i < masterRecord.size(); i++, j--)
					dataTableRow[i + 2] = masterRecord.realRealGet(j) != null ? masterRecord.getFormattedTableValue(j) : GDE.STRING_STAR;
			}
		}
		else if (this.isBoxPlotSuite()) {
			final TrailRecord lowerWhiskerRecord = this.getTrailRecordSuite()[5];
			final TrailRecord medianRecord = this.getTrailRecordSuite()[2];
			final TrailRecord upperWhiskerRecord = this.getTrailRecordSuite()[6];
			if (this.settings.isXAxisReversed()) {
				for (int i = 0; i < masterRecord.size(); i++) {
					if (masterRecord.realRealGet(i) != null) {
						StringBuilder sb = new StringBuilder();
						sb.append(lowerWhiskerRecord.realRealGet(i) != null ? String.format("%.8s", lowerWhiskerRecord.getFormattedTableValue(i)) : GDE.STRING_STAR); //$NON-NLS-1$
						String delimiter = sb.length() > 3 ? Character.toString((char) 183) : GDE.STRING_BLANK_COLON_BLANK;
						sb.append(delimiter).append(medianRecord.realRealGet(i) != null ? String.format("%.8s", medianRecord.getFormattedTableValue(i)) : GDE.STRING_STAR); //$NON-NLS-1$
						sb.append(delimiter).append(upperWhiskerRecord.realRealGet(i) != null ? String.format("%.8s", upperWhiskerRecord.getFormattedTableValue(i)) : GDE.STRING_STAR); //$NON-NLS-1$
						dataTableRow[i + 2] = sb.toString().intern();
					}
				}
			}
			else {
				for (int i = 0, j = masterRecord.size() - 1; i < masterRecord.size(); i++, j--)
					if (masterRecord.realRealGet(j) != null) {
						StringBuilder sb = new StringBuilder();
						sb.append(lowerWhiskerRecord.realRealGet(j) != null ? String.format("%.8s", lowerWhiskerRecord.getFormattedTableValue(j)) : GDE.STRING_STAR); //$NON-NLS-1$
						String delimiter = sb.length() > 3 ? Character.toString((char) 183) : GDE.STRING_BLANK_COLON_BLANK;
						sb.append(delimiter).append(medianRecord.realRealGet(j) != null ? String.format("%.8s", medianRecord.getFormattedTableValue(j)) : GDE.STRING_STAR); //$NON-NLS-1$
						sb.append(delimiter).append(upperWhiskerRecord.realRealGet(j) != null ? String.format("%.8s", upperWhiskerRecord.getFormattedTableValue(j)) : GDE.STRING_STAR); //$NON-NLS-1$
						dataTableRow[i + 2] = sb.toString().intern();
					}
			}
		}
		else if (isRangePlotSuite()) {
			final TrailRecord lowerRecord = this.getTrailRecordSuite()[1];
			final TrailRecord middleRecord = this.getTrailRecordSuite()[0];
			final TrailRecord upperRecord = this.getTrailRecordSuite()[2];
			if (this.settings.isXAxisReversed()) {
				for (int i = 0; i < masterRecord.size(); i++) {
					if (masterRecord.realRealGet(i) != null) {
						StringBuilder sb = new StringBuilder();
						sb.append(lowerRecord.realRealGet(i) != null ? String.format("%.8s", lowerRecord.getFormattedTableValue(i)) : GDE.STRING_STAR); //$NON-NLS-1$
						String delimiter = sb.length() > 3 ? Character.toString((char) 183) : GDE.STRING_BLANK_COLON_BLANK;
						sb.append(delimiter).append(middleRecord.realRealGet(i) != null ? String.format("%.8s", middleRecord.getFormattedTableValue(i)) : GDE.STRING_STAR); //$NON-NLS-1$
						sb.append(delimiter).append(upperRecord.realRealGet(i) != null ? String.format("%.8s", upperRecord.getFormattedTableValue(i)) : GDE.STRING_STAR); //$NON-NLS-1$
						dataTableRow[i + 2] = sb.toString().intern();
					}
				}
			}
			else {
				for (int i = 0, j = masterRecord.size() - 1; i < masterRecord.size(); i++, j--)
					if (masterRecord.realRealGet(j) != null) {
						StringBuilder sb = new StringBuilder();
						sb.append(lowerRecord.realRealGet(j) != null ? String.format("%.8s", lowerRecord.getFormattedTableValue(j)) : GDE.STRING_STAR); //$NON-NLS-1$
						String delimiter = sb.length() > 3 ? Character.toString((char) 183) : GDE.STRING_BLANK_COLON_BLANK;
						sb.append(delimiter).append(middleRecord.realRealGet(j) != null ? String.format("%.8s", middleRecord.getFormattedTableValue(j)) : GDE.STRING_STAR); //$NON-NLS-1$
						sb.append(delimiter).append(upperRecord.realRealGet(j) != null ? String.format("%.8s", upperRecord.getFormattedTableValue(j)) : GDE.STRING_STAR); //$NON-NLS-1$
						dataTableRow[i + 2] = sb.toString().intern();
					}
			}
		}
		else
			throw new UnsupportedOperationException();

		return dataTableRow;
	}

	/**
	 * build applicable trail type lists and textIndex for display purposes for one single trail.
	 * @param trailOrdinal
	 */
	public void setApplicableTrailTypes(int trailOrdinal) {
		this.applicableTrailsOrdinals = new ArrayList<Integer>(1);
		this.applicableTrailsOrdinals.add(trailOrdinal);
		this.applicableTrailsTexts = new ArrayList<String>(1);
		this.applicableTrailsTexts.add(TrailTypes.fromOrdinal(trailOrdinal).getDisplayName());
		this.trailTextSelectedIndex = 0;
	}

	/**
	 * analyze device configuration entries to find applicable trail types.
	 * build applicable trail type lists for display purposes.
	 * use device settings trigger texts for trigger trail types and score labels for score trail types; message texts otherwise.
	 */
	public void setApplicableTrailTypes() {
		boolean[] applicablePrimitiveTrails;
		// step 1: analyze device entries to find applicable primitive trail types
		if (this.measurementType != null) {
			final boolean hideAllTrails = this.measurementType.getTrailDisplay().map(x -> x.isDiscloseAll()).orElse(false);
			if (this.measurementType.getTrailDisplay().map(x -> x.getDefaultTrail()).map(x -> x.isSuite()).orElse(false))
				throw new UnsupportedOperationException("suite trail as a device measurement default"); //$NON-NLS-1$
			applicablePrimitiveTrails = new boolean[TrailTypes.getPrimitives().size()];
			StatisticsType measurementStatistics = getStatistics();

			// set legacy trail types
			if (!this.settings.isSmartStatistics()) {
				if (!hideAllTrails && measurementStatistics != null) {
					if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
						applicablePrimitiveTrails[TrailTypes.REAL_SUM_TRIGGERED.ordinal()] = (measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1);
						if (measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1 && measurementStatistics.getRatioRefOrdinal() != null) {
							StatisticsType referencedStatistics = this.device.getMeasurementStatistic(this.parent.getChannelConfigNumber(), measurementStatistics.getRatioRefOrdinal());
							applicablePrimitiveTrails[TrailTypes.REAL_AVG_RATIO_TRIGGERED.ordinal()] = referencedStatistics.isAvg();
							applicablePrimitiveTrails[TrailTypes.REAL_MAX_RATIO_TRIGGERED.ordinal()] = referencedStatistics.isMax();
						}
					}
					applicablePrimitiveTrails[TrailTypes.REAL_TIME_SUM_TRIGGERED.ordinal()] = (measurementStatistics.getTrigger() != null && measurementStatistics.getSumTriggerTimeText() != null
							&& measurementStatistics.getSumTriggerTimeText().length() > 1);
					applicablePrimitiveTrails[TrailTypes.REAL_COUNT_TRIGGERED.ordinal()] = (measurementStatistics.isCountByTrigger() != null);
				}
				// applicablePrimitiveTrails[TrailTypes.REAL_SUM.ordinal()] = false; // in settlements only
			}

			// set non-suite trail types : triggered values like count/sum are not supported
			if (!hideAllTrails)
				TrailTypes.getPrimitives().stream().filter(x -> !x.isTriggered() && x.isSmartStatistics() == this.settings.isSmartStatistics()).forEach(x -> applicablePrimitiveTrails[x.ordinal()] = true);

			// set visible and reset hidden trails based on device settlement settings
			this.measurementType.getTrailDisplay().ifPresent(x -> x.getExposed().stream().filter(y -> !y.getTrail().isSuite()).forEach(y -> applicablePrimitiveTrails[y.getTrail().ordinal()] = true));
			this.measurementType.getTrailDisplay().ifPresent(x -> x.getDisclosed().stream().filter(y -> !y.getTrail().isSuite()).forEach(y -> applicablePrimitiveTrails[y.getTrail().ordinal()] = false));

			// set at least one trail if no trail is applicable
			boolean hasApplicablePrimitiveTrails = false;
			for (boolean value : applicablePrimitiveTrails) {
				hasApplicablePrimitiveTrails = value;
				if (hasApplicablePrimitiveTrails) break;
			}
			if (!hasApplicablePrimitiveTrails) applicablePrimitiveTrails[this.measurementType.getTrailDisplay().map(x -> x.getDefaultTrail()).orElse(TrailTypes.getSubstitute()).ordinal()] = true;
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.getName() + " data " + Arrays.toString(applicablePrimitiveTrails)); //$NON-NLS-1$

			// build applicable trail type lists for display purposes
			this.applicableTrailsOrdinals = new ArrayList<Integer>();
			this.applicableTrailsTexts = new ArrayList<String>();
			for (int i = 0; i < applicablePrimitiveTrails.length; i++) {
				if (applicablePrimitiveTrails[i]) {
					this.applicableTrailsOrdinals.add(i);
					if (TrailTypes.values[i].isTriggered()) {
						if (TrailTypes.values[i].equals(TrailTypes.REAL_COUNT_TRIGGERED)) {
							this.applicableTrailsTexts.add(getDeviceXmlReplacement(getStatistics().getCountTriggerText()));
						}
						else if (TrailTypes.values[i].equals(TrailTypes.REAL_SUM_TRIGGERED)) {
							this.applicableTrailsTexts.add(getDeviceXmlReplacement(getStatistics().getSumTriggerText()));
						}
						else if (TrailTypes.values[i].equals(TrailTypes.REAL_TIME_SUM_TRIGGERED)) {
							this.applicableTrailsTexts.add(getDeviceXmlReplacement(getStatistics().getSumTriggerTimeText()));
						}
						else if (TrailTypes.values[i].equals(TrailTypes.REAL_AVG_RATIO_TRIGGERED)) {
							this.applicableTrailsTexts.add(getDeviceXmlReplacement(getStatistics().getRatioText()));
						}
						else if (TrailTypes.values[i].equals(TrailTypes.REAL_MAX_RATIO_TRIGGERED)) {
							this.applicableTrailsTexts.add(getDeviceXmlReplacement(getStatistics().getRatioText()));
						}
						else
							throw new UnsupportedOperationException("TrailTypes.isTriggered"); //$NON-NLS-1$
					}
					else {
						this.applicableTrailsTexts.add(TrailTypes.values[i].getDisplayName().intern());
					}
				}
			}

			if (!this.measurementType.getTrailDisplay().map(x -> x.isDiscloseAll()).orElse(false)) {
				// define trail suites which are applicable for display
				List<TrailTypes> exposed = this.measurementType.getTrailDisplay().map(x -> x.getExposed().stream().map(y -> y.getTrail()).collect(Collectors.toList())).orElse(new ArrayList<TrailTypes>());
				List<TrailTypes> disclosed = this.measurementType.getTrailDisplay().map(x -> x.getDisclosed().stream().map(y -> y.getTrail()).collect(Collectors.toList())).orElse(new ArrayList<TrailTypes>());
				for (TrailTypes suiteTrailType : TrailTypes.getSuites()) {
					if ((suiteTrailType.isSmartStatistics() == this.settings.isSmartStatistics() || exposed.contains(suiteTrailType)) && !disclosed.contains(suiteTrailType)) {
						this.applicableTrailsOrdinals.add(suiteTrailType.ordinal());
						this.applicableTrailsTexts.add(suiteTrailType.getDisplayName().intern());
					}
				}
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, super.getName() + " texts " + this.applicableTrailsTexts); //$NON-NLS-1$
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, super.getName() + " ordinals " + this.applicableTrailsOrdinals); //$NON-NLS-1$
		}
		else if (this.settlementType != null)

		{
			if (this.settlementType.getTrailDisplay().map(x -> x.getDefaultTrail()).map(x -> x.isSuite()).orElse(false))
				throw new UnsupportedOperationException("suite trail as a device settlement default"); //$NON-NLS-1$
			applicablePrimitiveTrails = new boolean[TrailTypes.getPrimitives().size()];

			// todo set quantile-based non-suite trail types : triggered value sum are CURRENTLY not supported
			if (!this.settlementType.getTrailDisplay().map(x -> x.isDiscloseAll()).orElse(false)) {
				if (this.settlementType.getEvaluation().getTransitionAmount() == null)
					TrailTypes.getPrimitives().stream().filter(x -> !x.isTriggered() && x.isSmartStatistics() == this.settings.isSmartStatistics()).forEach(x -> applicablePrimitiveTrails[x.ordinal()] = true);
				else
					throw new UnsupportedOperationException("TransitionAmount not implemented"); //$NON-NLS-1$
			}

			// set visible non-suite trails based on device settlement settings
			this.settlementType.getTrailDisplay()
					.ifPresent(x -> x.getExposed().stream().map(z -> z.getTrail()) //
							.filter(o -> !o.isSuite()) //
							.forEach(y -> applicablePrimitiveTrails[y.ordinal()] = true));

			// reset hidden non-suite trails based on device settlement settings
			this.settlementType.getTrailDisplay().ifPresent(x -> x.getDisclosed().stream().map(z -> z.getTrail()).filter(o -> !o.isSuite()).forEach(y -> applicablePrimitiveTrails[y.ordinal()] = false));

			// set at least one trail if no trail is applicable
			boolean hasApplicablePrimitiveTrails = false;
			for (boolean value : applicablePrimitiveTrails) {
				hasApplicablePrimitiveTrails = value;
				if (hasApplicablePrimitiveTrails) break;
			}
			if (!hasApplicablePrimitiveTrails) applicablePrimitiveTrails[this.settlementType.getTrailDisplay().map(x -> x.getDefaultTrail()).orElse(TrailTypes.getSubstitute()).ordinal()] = true;
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.getName() + " data " + Arrays.toString(applicablePrimitiveTrails)); //$NON-NLS-1$

			// build applicable trail type lists for display purposes
			this.applicableTrailsOrdinals = new ArrayList<Integer>();
			this.applicableTrailsTexts = new ArrayList<String>();
			for (int i = 0; i < applicablePrimitiveTrails.length; i++) {
				if (applicablePrimitiveTrails[i]) {
					this.applicableTrailsOrdinals.add(i);
					this.applicableTrailsTexts.add(TrailTypes.values[i].getDisplayName().intern());
				}
			}

			if (!this.settlementType.getTrailDisplay().map(x -> x.isDiscloseAll()).orElse(false)) {
				// define trail suites which are applicable for display
				List<TrailTypes> exposed = this.settlementType.getTrailDisplay() //
						.map(x -> x.getExposed().stream().map(y -> y.getTrail()).collect(Collectors.toList())) // collect all the trails from the collection of exposed TrailVisibility members
						.orElse(new ArrayList<TrailTypes>()); // 																									get an empty list if there is no trailDisplay tag in the device.xml for this settlement
				List<TrailTypes> disclosed = this.settlementType.getTrailDisplay().map(x -> x.getDisclosed().stream().map(y -> y.getTrail()).collect(Collectors.toList())).orElse(new ArrayList<TrailTypes>());
				for (TrailTypes suiteTrailType : TrailTypes.getSuites()) {
					if ((suiteTrailType.isSmartStatistics() == this.settings.isSmartStatistics() || exposed.contains(suiteTrailType)) && !disclosed.contains(suiteTrailType)) {
						this.applicableTrailsOrdinals.add(suiteTrailType.ordinal());
						this.applicableTrailsTexts.add(suiteTrailType.getDisplayName().intern());
					}
				}
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, super.getName() + " texts " + this.applicableTrailsTexts); //$NON-NLS-1$
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, super.getName() + " ordinals " + this.applicableTrailsOrdinals); //$NON-NLS-1$
		}
		else if (this.scoreGroupType != null) {
			applicablePrimitiveTrails = new boolean[0]; // not required

			// build applicable trail type lists for display purposes
			this.applicableTrailsOrdinals = new ArrayList<Integer>();
			this.applicableTrailsTexts = new ArrayList<String>();
			if (this.scoreGroupType != null) {
				for (int i = 0; i < this.scoreGroupType.getScore().size(); i++) {
					this.applicableTrailsOrdinals.add(this.scoreGroupType.getScore().get(i).getLabel().ordinal());
					this.applicableTrailsTexts.add(getDeviceXmlReplacement(this.scoreGroupType.getScore().get(i).getValue()));
				}
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.getName() + " score "); //$NON-NLS-1$
			}
		}
		else {
			throw new UnsupportedOperationException(" >>> no trails found <<< "); //$NON-NLS-1$
		}
	}

	public TrailRecordSet getParentTrail() {
		return this.parentTrail;
	}

	/**
	 * @return display text for the trail (may have been modified due to special texts for triggers)
	 */
	public String getTrailText() {
		return this.applicableTrailsTexts.size() == 0 ? GDE.STRING_EMPTY : this.applicableTrailsTexts.get(this.trailTextSelectedIndex);
	}

	public List<String> getApplicableTrailsTexts() {
		return this.applicableTrailsTexts;
	}

	public Integer getTrailTextSelectedIndex() {
		return this.trailTextSelectedIndex;
	}

	/**
	 * builds the suite of trail records if the selection has changed.
	 * @param value position / index of the trail type in the current list of applicable trails
	 */
	public void setTrailTextSelectedIndex(int value) {
		if (this.trailTextSelectedIndex != value) {
			this.trailTextSelectedIndex = value;

			if (isTrailSuite()) {
				List<TrailTypes> suite = TrailTypes.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex)).getSuiteMembers();
				this.trailRecordSuite = new TrailRecord[suite.size()];
				int i = 0;
				for (TrailTypes trailTypes : suite) {
					if (this.measurementType != null) {
						this.trailRecordSuite[i] = new TrailRecord(this.device, this.ordinal, "suite" + trailTypes.getDisplaySequence(), this.measurementType, this.parentTrail, this.size()); //$NON-NLS-1$
						this.trailRecordSuite[i].setApplicableTrailTypes(trailTypes.ordinal());
					}
					else if (this.settlementType != null) {
						this.trailRecordSuite[i] = new TrailRecord(this.device, this.ordinal, "suite" + trailTypes.getDisplaySequence(), this.settlementType, this.parentTrail, this.size()); //$NON-NLS-1$
						this.trailRecordSuite[i].setApplicableTrailTypes(trailTypes.ordinal());
					}
					else if (this.scoreGroupType != null) {
						this.trailRecordSuite[i] = new TrailRecord(this.device, this.ordinal, "suite" + trailTypes.getDisplaySequence(), this.scoreGroupType, this.parentTrail, this.size()); //$NON-NLS-1$
						this.trailRecordSuite[i].setApplicableTrailTypes(trailTypes.ordinal());
					}
					else {
						throw new UnsupportedOperationException();
					}
					i++;
				}
			}
			else {
				this.trailRecordSuite = new TrailRecord[] { this };
			}
		}
	}

	public int getTrailOrdinal() {
		return this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex);
	}

	public boolean isTrailSuite() {
		return this.scoreGroupType == null ? TrailTypes.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex)).isSuite() : false;
	}

	public boolean isRangePlotSuite() {
		return this.scoreGroupType == null ? TrailTypes.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex)).isRangePlot() : false;
	}

	public boolean isBoxPlotSuite() {
		return this.scoreGroupType == null ? TrailTypes.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex)).isBoxPlot() : false;
	}

	public boolean isSuiteForSummation() {
		return this.scoreGroupType == null ? TrailTypes.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex)).isForSummation() : false;
	}

	public boolean isMeasurement() {
		return this.measurementType != null;
	}

	public boolean isSettlement() {
		return this.settlementType != null;
	}

	public boolean isScoreGroup() {
		return this.scoreGroupType != null;
	}

	public MeasurementType getMeasurement() {
		return this.measurementType;
	}

	public SettlementType getSettlement() {
		return this.settlementType;
	}

	public ScoreGroupType getScoregroup() {
		return this.scoreGroupType;
	}

	public StatisticsType getStatistics() {
		return this.measurementType.getStatistics();
	}

	/**
	 * select the most prioritized trail from the applicable trails.
	 * @return
	 */
	public void setMostApplicableTrailTextOrdinal() {
		if (this.scoreGroupType != null) {
			setTrailTextSelectedIndex(0);
		}
		else {
			int displaySequence = Integer.MAX_VALUE;
			for (int i = 0; i < this.applicableTrailsOrdinals.size(); i++) {
				int tmpDisplaySequence = (TrailTypes.fromOrdinal(this.applicableTrailsOrdinals.get(i))).getDisplaySequence();
				if (tmpDisplaySequence < displaySequence) {
					displaySequence = tmpDisplaySequence;
					setTrailTextSelectedIndex(i);
				}
			}
		}
	}

	@Override // reason is translateValue which accesses the device for offset etc.
	public double getFactor() {
		if (this.factor == Double.MIN_VALUE) {
			this.factor = 1.0;
			PropertyType property = this.getProperty(IDevice.FACTOR);
			if (property != null)
				this.factor = Double.valueOf(property.getValue());
			else if (this.scoreGroupType != null)
				this.factor = this.scoreGroupType.getFactor();
			else if (this.settlementType != null)
				this.factor = this.settlementType.getFactor();
			else if (this.measurementType != null) {
				this.factor = this.measurementType.getFactor();
			}
			else
				try {
					this.factor = this.getDevice().getMeasurementFactor(this.parent.parent.number, this.ordinal);
				}
				catch (RuntimeException e) {
					// log.log(Level.WARNING, this.name + " use default value for property " + IDevice.FACTOR); // log warning and use default value
				}
		}
		return this.factor;
	}

	@Override // reason is translateValue which accesses the device for offset etc.
	public double getOffset() {
		if (this.offset == Double.MIN_VALUE) {
			this.offset = 0.0;
			PropertyType property = this.getProperty(IDevice.OFFSET);
			if (property != null)
				this.offset = Double.valueOf(property.getValue());
			else if (this.scoreGroupType != null)
				this.offset = this.scoreGroupType.getOffset();
			else if (this.settlementType != null)
				this.offset = this.settlementType.getOffset();
			else if (this.measurementType != null)
				this.offset = this.measurementType.getOffset();
			else
				try {
					this.offset = this.getDevice().getMeasurementOffset(this.parent.parent.number, this.ordinal);
				}
				catch (RuntimeException e) {
					// log.log(Level.WARNING, this.name + " use default value for property " + IDevice.OFFSET); // log warning and use default value
				}
		}
		return this.offset;
	}

	@Override // reason is translateValue which accesses the device for offset etc.
	public double getReduction() {
		if (this.reduction == Double.MIN_VALUE) {
			this.reduction = 0.0;
			PropertyType property = this.getProperty(IDevice.REDUCTION);
			if (property != null)
				this.reduction = Double.valueOf(property.getValue());
			else if (this.scoreGroupType != null)
				this.reduction = this.scoreGroupType.getReduction();
			else if (this.settlementType != null)
				this.reduction = this.settlementType.getReduction();
			else if (this.measurementType != null)
				this.reduction = this.measurementType.getReduction();
			else
				try {
					String strValue = (String) this.getDevice().getMeasurementPropertyValue(this.parent.parent.number, this.ordinal, IDevice.REDUCTION);
					if (strValue != null && strValue.length() > 0) this.reduction = Double.valueOf(strValue.trim().replace(',', '.'));
				}
				catch (RuntimeException e) {
					// log.log(Level.WARNING, this.name + " use default value for property " + IDevice.REDUCTION); // log warning and use default value
				}
		}
		return this.reduction;
	}

	@Override // reason: formatting of values <= 100 with decimal places; define category based on maxValueAbs AND minValueAbs
	public void setNumberFormat(int newNumberFormat) {
		this.numberFormat = newNumberFormat;
		switch (newNumberFormat) {
		case -1:
			if (Math.abs(this.maxScaleValue) < 10 && Math.abs(this.minScaleValue) < 10) {
				if (this.maxScaleValue - this.minScaleValue < 0.01)
					this.df.applyPattern("0.0000"); //$NON-NLS-1$
				else if (this.maxScaleValue - this.minScaleValue < 0.1)
					this.df.applyPattern("0.000"); //$NON-NLS-1$
				else
					this.df.applyPattern("0.00"); //$NON-NLS-1$
			}
			else if (Math.abs(this.maxScaleValue) < 100 && Math.abs(this.minScaleValue) < 100) {
				if (this.maxScaleValue - this.minScaleValue < 0.1)
					this.df.applyPattern("0.000"); //$NON-NLS-1$
				else if (this.maxScaleValue - this.minScaleValue < 1.)
					this.df.applyPattern("0.00"); //$NON-NLS-1$
				else
					this.df.applyPattern("0.0"); //$NON-NLS-1$
			}
			else if (Math.abs(this.maxScaleValue) < 1000 && Math.abs(this.minScaleValue) < 1000) {
				if (this.maxScaleValue - this.minScaleValue < 1.)
					this.df.applyPattern("0.00"); //$NON-NLS-1$
				else if (this.maxScaleValue - this.minScaleValue < 10.)
					this.df.applyPattern("0.0"); //$NON-NLS-1$
				else
					this.df.applyPattern("0"); //$NON-NLS-1$
			}
			else if (Math.abs(this.maxScaleValue) < 10000 && Math.abs(this.minScaleValue) < 10000) {
				if (this.maxScaleValue - this.minScaleValue < 10.)
					this.df.applyPattern("0.0"); //$NON-NLS-1$
				else
					this.df.applyPattern("0"); //$NON-NLS-1$
			}
			else {
				this.df.applyPattern("0"); //$NON-NLS-1$
			}
			break;
		case 0:
			this.df.applyPattern("0"); //$NON-NLS-1$
			break;
		case 1:
			this.df.applyPattern("0.0"); //$NON-NLS-1$
			break;
		case 2:
		default:
			this.df.applyPattern("0.00"); //$NON-NLS-1$
			break;
		case 3:
			this.df.applyPattern("0.000"); //$NON-NLS-1$
			break;
		}
	}

	/**
	 * @return the array of suite records; it may consist of the trail record itself which simplifies things
	 */
	public TrailRecord[] getTrailRecordSuite() {
		return this.trailRecordSuite;
	}

	public String getNameReplacement() {
		return getDeviceXmlReplacement(this.name);
	}

	/**
	 * @return the localized value of the label property from the device channel entry or an empty string.
	 */
	public String getLabel() {
		String label = this.measurementType != null ? this.measurementType.getLabel() : this.settlementType != null ? this.settlementType.getLabel() : this.scoreGroupType.getLabel();
		return getDeviceXmlReplacement(label);
	}

	/**
	 * @return true if the record or the suite contains reasonable data
	 */
	@Override // reason is trail record suites with a master record without point values
	public boolean hasReasonableData() {
		boolean hasReasonableData = false;
		if (this.trailRecordSuite == null || this.trailRecordSuite.length == 1) {
			hasReasonableData = super.hasReasonableData();
		}
		else {
			for (TrailRecord trailRecord : this.trailRecordSuite) {
				if (trailRecord.hasReasonableData()) { // no recursion because suites do not contain suites
					hasReasonableData = true;
					break;
				}
			}
		}
		return hasReasonableData;
	}

	/**
	 * @return true if the record is the scale sync master and if the record is for display according to histo display settings
	 */
	@Override // reason are the histo display settings which hide records
	public boolean isScaleVisible() {
		boolean isValidDisplayRecord = this.isMeasurement() || (this.isSettlement() && this.settings.isDisplaySettlements()) || (this.isScoreGroup() && this.settings.isDisplayScores());
		return isValidDisplayRecord && super.isScaleVisible();
	}

	/**
	 * Builds value lists from the points within the bounds.
	 * The list is ordered from timeStamp1 to timeStamp2.
	 * @param timeStamp1_ms is the time of the delta measurement start value
	 * @param timeStamp2_ms is the time of the delta measurement end value
	 */
	public void setBounds(long timeStamp1_ms, long timeStamp2_ms) {
		this.linearRegression = null;
		this.quantile = null;

		final TrailRecord trailRecord;
		final List<Double> boundedTimeStamps_ms = new ArrayList<>();
		final List<Double> boundedValues = new ArrayList<>();

		if (!this.isTrailSuite())
			trailRecord = this.getTrailRecordSuite()[0];
		else if (this.isBoxPlotSuite())
			trailRecord = this.getTrailRecordSuite()[2];
		else if (isRangePlotSuite())
			trailRecord = this.getTrailRecordSuite()[0];
		else
			throw new UnsupportedOperationException();

		final int measureIndex = this.parentTrail.getIndex(timeStamp1_ms);
		final int deltaIndex = this.parentTrail.getIndex(timeStamp2_ms);
		boundedTimeStamps_ms.clear();
		boundedValues.clear();
		if (measureIndex < deltaIndex) {
			for (int i = measureIndex; i < deltaIndex + 1; i++) {
				if (trailRecord.realRealGet(i) != null) {
					boundedTimeStamps_ms.add(this.parent.timeStep_ms.getTime_ms(i));
					boundedValues.add(trailRecord.device.translateValue(trailRecord, trailRecord.realRealGet(i) / 1000.));
				}
			}
		}
		else {
			for (int i = measureIndex; i > deltaIndex - 1; i--) {
				if (trailRecord.realRealGet(i) != null) {
					boundedTimeStamps_ms.add(this.parent.timeStep_ms.getTime_ms(i));
					boundedValues.add(trailRecord.device.translateValue(trailRecord, trailRecord.realRealGet(i) / 1000.));
				}
			}
		}

		if (!boundedTimeStamps_ms.isEmpty()) {
			this.linearRegression = new LinearRegression(boundedTimeStamps_ms, boundedValues);
			this.quantile = new Quantile(boundedValues, EnumSet.of(Fixings.IS_SAMPLE), 99., 99.);
			if (log.isLoggable(Level.FINER)) {
				Date date = new Date(boundedTimeStamps_ms.get(0).longValue());
				log.log(Level.FINER, String.format("regressionSize=%d regressionRealSize=%d starts at %tF %tR", boundedValues.size(), this.linearRegression.getRealSize(), date, date));
			}
		}
	}

	/**
	 * @return the formatted translated average of all bounds values
	 */
	public String getFormattedBoundsAvg() {
		return this.getFormattedScaleValue(this.linearRegression.getAvg());
	}

	/**
	 * @return the formatted translated difference between the left and right bounds values
	 */
	public String getFormattedBoundsDelta() {
		return this.getFormattedScaleValue(0. - this.linearRegression.getDelta());
	}

	/**
	 * @return the formatted translated regression slope of the bounds values based on months
	 */
	@Deprecated
	public String getFormattedBoundsDeltaSlope() {
		final long endTimeStamp_ms = (long) this.linearRegression.getFirstPredictor(); // inverse sorted order
		final long startTimeStamp_ms = (long) (this.linearRegression.getFirstPredictor() + this.linearRegression.getPredictorDelta());
		Calendar startCalendar = new GregorianCalendar();
		startCalendar.setTimeInMillis(startTimeStamp_ms);
		Calendar endCalendar = new GregorianCalendar();
		endCalendar.setTimeInMillis(endTimeStamp_ms);

		// determine number of full calendar months
		int month1 = startCalendar.get(Calendar.YEAR) * 12 + startCalendar.get(Calendar.MONTH);
		int month2 = endCalendar.get(Calendar.YEAR) * 12 + endCalendar.get(Calendar.MONTH);
		int monthDelta = month2 - month1;

		// determine the number of months with decimal places
		double fractionalMonthsDelta;
		{
			// calculate the number of days in a year's sliding window
			final int d2m = 24 * 60; // days to minutes factor
			int minute1 = startCalendar.get(Calendar.DAY_OF_YEAR) * d2m + startCalendar.get(Calendar.HOUR_OF_DAY) * 60 + startCalendar.get(Calendar.MINUTE);
			int minute2 = endCalendar.get(Calendar.DAY_OF_YEAR) * d2m + endCalendar.get(Calendar.HOUR_OF_DAY) * 60 + endCalendar.get(Calendar.MINUTE);
			int minuteDelta = startCalendar.getActualMaximum(Calendar.DAY_OF_YEAR) == 366 ? (366 * d2m + minute2 - minute1 + d2m) % (366 * d2m) : (365 * d2m + minute2 - minute1) % (365 * d2m);
			// transform the number of days into number of months
			int y2m = endCalendar.getActualMaximum(Calendar.DAY_OF_YEAR) == 366 ? d2m * 366 : d2m * 365; // year to minutes factor
			fractionalMonthsDelta = endCalendar.getActualMaximum(Calendar.DAY_OF_YEAR) == 366 ? minuteDelta / (y2m / 12.) : minuteDelta / (y2m / 12.);
		}
		if (monthDelta > 12) fractionalMonthsDelta = monthDelta - 12 + fractionalMonthsDelta;
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, String.format("%tF %tR %tF %tR delta=%f months=%f", startCalendar.getTime(), startCalendar.getTime(), endCalendar.getTime(), //$NON-NLS-1$
				endCalendar.getTime(), this.linearRegression.getDelta(), fractionalMonthsDelta));

		return this.getFormattedScaleValue(-this.linearRegression.getDelta() / fractionalMonthsDelta);
	}

	/**
	 * @return the formatted translated regression slope of the bounds values based on months
	 */
	public String getFormattedBoundsSlope() {
		return this.getFormattedScaleValue(this.linearRegression.getSlope() * GDE.ONE_HOUR_MS * 24 * 365 / 12);
	}

	/**
	 * Supports suites and null values.
	 * @param index
	 * @return the translated and decimal formatted value at the given index or a standard string in case of a null value
	 */
	@Override
	public String getFormattedMeasureValue(int index) {
		final TrailRecord trailRecord;
		if (!this.isTrailSuite())
			trailRecord = this.getTrailRecordSuite()[0];
		else if (this.isBoxPlotSuite())
			trailRecord = this.getTrailRecordSuite()[2];
		else if (isRangePlotSuite())
			trailRecord = this.getTrailRecordSuite()[0];
		else
			throw new UnsupportedOperationException();

		if (trailRecord.realRealGet(index) != null) {
			return trailRecord.getFormattedTableValue(index);
		}
		else
			return GDE.STRING_STAR;
	}

	/**
	 * find the index closest to given time in msec
	 * @param time_ms
	 * @return index nearest to given time
	 */
	@Override
	@Deprecated // replaced by gde.data.TrailRecordSet.getIndex(long)
	public int findBestIndex(double time_ms) {
		int index = this.timeStep_ms == null ? this.parent.timeStep_ms.findBestIndex(time_ms) : this.timeStep_ms.findBestIndex(time_ms);
		return index > this.elementCount - 1 ? this.elementCount - 1 : index;
	}

	/**
	* get the time in msec at given horizontal display position
	* @param xPos of the display point
	* @return time value in msec
	*/
	@Override
	@Deprecated // replaced by gde.utils.HistoTimeLine.getTimestamp(int)
	public double getHorizontalDisplayPointTime_ms(int xPos) {
		return this.drawTimeWidth * xPos / this.parent.drawAreaBounds.width;
	}

	/**
	* get the formatted time with unit at given position
	* @param xPos of the display point
	* @return string of time value in simple date format HH:ss:mm:SSS
	*/
	@Override
	@Deprecated // replaced by gde.utils.HistoTimeLine.getTimestamp(int)
	public String getHorizontalDisplayPointAsFormattedTimeWithUnit(int xPos) {
		return TimeLine.getFomatedTimeWithUnit(
				this.getHorizontalDisplayPointTime_ms(xPos) + this.getDrawTimeOffset_ms() + (this.settings != null && this.settings.isTimeFormatAbsolute() ? this.getStartTimeStamp() : 1292400000.0)); //1292400000 == 1970-01-16 00:00:00.000
	}

	/**
	 * calculate best fit index in data vector from given display point relative to the (zoomed) display width
	 * @param xPos
	 * @return position integer value
	 */
	@Override
	@Deprecated // replaced by gde.data.TrailRecordSet.getIndex(gde.utils.HistoTimeLine.getTimestamp(int))
	public int getHorizontalPointIndexFromDisplayPoint(int xPos) {
		return this.findBestIndex(getHorizontalDisplayPointTime_ms(xPos) + this.getDrawTimeOffset_ms());
	}

	/**
	 * supports suites and null values.
	 * @param index
	 * @return yPos in pixel (with top@0 and bottom@drawAreaBounds.height) or Integer.MIN_VALUE if the index value is null
	 */
	public int getVerticalDisplayPos(int index) {
		final TrailRecord trailRecord;
		if (!this.isTrailSuite())
			trailRecord = this.getTrailRecordSuite()[0];
		else if (this.isBoxPlotSuite())
			trailRecord = this.getTrailRecordSuite()[2];
		else if (isRangePlotSuite())
			trailRecord = this.getTrailRecordSuite()[0];
		else
			throw new UnsupportedOperationException();

		int verticalDisplayPos = Integer.MIN_VALUE;
		if (this.device.isGPSCoordinates(this)) {
			if (trailRecord.realRealGet(index) != null) {
				double decimalDegreeValue = trailRecord.realRealGet(index) / 1000000 + trailRecord.realRealGet(index) % 1000000 / 600000.;
				verticalDisplayPos = trailRecord.parent.drawAreaBounds.height
						- (int) ((decimalDegreeValue * 1000. - trailRecord.minDisplayValue * 1 / trailRecord.syncMasterFactor) * trailRecord.displayScaleFactorValue);
			}
		}
		else if (trailRecord.realRealGet(index) != null) verticalDisplayPos = trailRecord.parent.drawAreaBounds.height
				- (int) ((trailRecord.realRealGet(index) / 1000.0 - trailRecord.minDisplayValue * 1 / trailRecord.syncMasterFactor) * trailRecord.displayScaleFactorValue);

		return verticalDisplayPos;
	}

	public int getVerticalDisplayPos(double translatedValue) {
		int verticalDisplayPos = Integer.MIN_VALUE;

		final TrailRecord trailRecord;
		if (!this.isTrailSuite())
			trailRecord = this.getTrailRecordSuite()[0];
		else if (this.isBoxPlotSuite())
			trailRecord = this.getTrailRecordSuite()[2];
		else if (isRangePlotSuite())
			trailRecord = this.getTrailRecordSuite()[0];
		else
			throw new UnsupportedOperationException();

		int point = (int) (this.device.reverseTranslateValue(this, translatedValue) * 1000.);
		if (this.device.isGPSCoordinates(this)) {
			double decimalDegreeValue = point / 1000000 + point % 1000000 / 600000.;
			verticalDisplayPos = trailRecord.parent.drawAreaBounds.height
					- (int) ((decimalDegreeValue * 1000. - trailRecord.minDisplayValue * 1 / trailRecord.syncMasterFactor) * trailRecord.displayScaleFactorValue);
		}
		else {
			verticalDisplayPos = trailRecord.parent.drawAreaBounds.height - (int) ((point / 1000.0 - trailRecord.minDisplayValue * 1 / trailRecord.syncMasterFactor) * trailRecord.displayScaleFactorValue);
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("translatedValue=%f reverseTranslatedPoint=%d yPos=%d", translatedValue, point, verticalDisplayPos)); //$NON-NLS-1$
		return verticalDisplayPos;
	}

	/**
	 * query data value (not translated in device units) from a display position point
	 * @param xPos
	 * @return displays yPos in pixel
	 */
	@Override
	public int getVerticalDisplayPointValue(int xPos) {
		int pointPosY = 0;

		try {
			double tmpTimeValue = this.getHorizontalDisplayPointTime_ms(xPos) + this.getDrawTimeOffset_ms();
			int[] indexs = this.findBoundingIndexes(tmpTimeValue);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, tmpTimeValue + "; " + indexs[0] + "; " + indexs[1]); //$NON-NLS-1$ //$NON-NLS-2$
			if (super.size() > 0) {
				if (this.getDevice().isGPSCoordinates(this)) {
					int grad0 = this.get(indexs[0]) / 1000000;
					if (indexs[0] == indexs[1]) {
						pointPosY = (int) (this.parent.drawAreaBounds.height
								- ((((grad0 + ((this.get(indexs[0]) / 1000000.0 - grad0) / 0.60)) * 1000.0) - (this.minDisplayValue * 1 / this.syncMasterFactor)) * this.displayScaleFactorValue));
					}
					else {
						int grad1 = this.get(indexs[1]) / 1000000;
						double deltaValueY = (grad1 + ((this.get(indexs[1]) / 1000000.0 - grad1) / 0.60)) - (grad0 + ((this.get(indexs[0]) / 1000000.0 - grad0) / 0.60));
						double deltaTimeIndex01 = this.timeStep_ms != null ? this.timeStep_ms.getTime_ms(indexs[1]) - this.timeStep_ms.getTime_ms(indexs[0])
								: this.parent.timeStep_ms.getTime_ms(indexs[1]) - this.parent.timeStep_ms.getTime_ms(indexs[0]);
						double xPosDeltaTime2Index0 = tmpTimeValue - (this.timeStep_ms != null ? this.timeStep_ms.getTime_ms(indexs[0]) : this.parent.timeStep_ms.getTime_ms(indexs[0]));
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "deltyValueY = " + deltaValueY + " deltaTime = " + deltaTimeIndex01 + " deltaTimeValue = " + xPosDeltaTime2Index0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						pointPosY = Double
								.valueOf(this.parent.drawAreaBounds.height - ((((grad0 + ((this.get(indexs[0]) / 1000000.0 - grad0) / 0.60)) + (xPosDeltaTime2Index0 / deltaTimeIndex01 * deltaValueY)) * 1000.0)
										- (this.minDisplayValue * 1 / this.syncMasterFactor)) * this.displayScaleFactorValue)
								.intValue();
					}
				}
				else {
					if (indexs[0] == indexs[1]) {
						pointPosY = (int) (this.parent.drawAreaBounds.height - (((super.get(indexs[0]) / 1000.0) - (this.minDisplayValue * 1 / this.syncMasterFactor)) * this.displayScaleFactorValue));
					}
					else {
						int deltaValueY = super.get(indexs[1]) - super.get(indexs[0]);
						double deltaTimeIndex01 = this.timeStep_ms != null ? this.timeStep_ms.getTime_ms(indexs[1]) - this.timeStep_ms.getTime_ms(indexs[0])
								: this.parent.timeStep_ms.getTime_ms(indexs[1]) - this.parent.timeStep_ms.getTime_ms(indexs[0]);
						double xPosDeltaTime2Index0 = tmpTimeValue - (this.timeStep_ms != null ? this.timeStep_ms.getTime_ms(indexs[0]) : this.parent.timeStep_ms.getTime_ms(indexs[0]));
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "deltyValueY = " + deltaValueY + " deltaTime = " + deltaTimeIndex01 + " deltaTimeValue = " + xPosDeltaTime2Index0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						pointPosY = Double
								.valueOf(this.parent.drawAreaBounds.height
										- (((super.get(indexs[0]) + (xPosDeltaTime2Index0 / deltaTimeIndex01 * deltaValueY)) / 1000.0) - (this.minDisplayValue * 1 / this.syncMasterFactor)) * this.displayScaleFactorValue)
								.intValue();
					}
				}
			}
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, xPos + " -> timeValue = " + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", (long) tmpTimeValue) + " pointPosY = " + pointPosY); //$NON-NLS-1$ //$NON-NLS-2$

			//check yPos out of range, the graph might not visible within this area
			//		if(pointPosY > this.parent.drawAreaBounds.height)
			//			log.log(Level.WARNING, "pointPosY > drawAreaBounds.height");
			//		if(pointPosY < 0)
			//			log.log(Level.WARNING, "pointPosY < 0");
		}
		catch (RuntimeException e) {
			log.log(Level.WARNING, e.getMessage() + " xPos = " + xPos, e); //$NON-NLS-1$
		}
		return pointPosY > this.parent.drawAreaBounds.height ? this.parent.drawAreaBounds.height : pointPosY < 0 ? 0 : pointPosY;
	}

	/**
	 * get the formatted scale value corresponding the vertical display point
	 * @param yPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	@Override
	@Deprecated // todo harmonize with getVerticalDisplayPointValue  ---  replaced by gde.data.TrailRecord.getVerticalDisplayPointAsFormattedScaleValue(int)
	public String getVerticalDisplayPointAsFormattedScaleValue(int yPos, Rectangle drawAreaBounds) {
		String displayPointValue;
		//scales are all synchronized in viewpoint of end values (min/max)
		//PropertyType syncProperty = this.parent.isCompareSet ? null : this.device.getMeasruementProperty(this.parent.parent.number, this.ordinal, MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value());
		//if (syncProperty != null && !syncProperty.getValue().equals(GDE.STRING_EMPTY)) {
		//	Record syncRecord = this.parent.get(this.ordinal);
		//		displayPointValue = syncRecord.df.format(Double.valueOf(syncRecord.minDisplayValue +  ((syncRecord.maxDisplayValue - syncRecord.minDisplayValue) * (drawAreaBounds.height-yPos) / drawAreaBounds.height)));
		//}
		//else
		if (this.parent.isZoomMode)
			displayPointValue = this.df.format(this.minZoomScaleValue + ((this.maxZoomScaleValue - this.minZoomScaleValue) * (drawAreaBounds.height - yPos) / drawAreaBounds.height));
		else
			displayPointValue = this.df.format(this.minScaleValue + ((this.maxScaleValue - this.minScaleValue) * (drawAreaBounds.height - yPos) / drawAreaBounds.height));

		return displayPointValue;
	}

	/**
	 * get the scale value corresponding the vertical display point
	 * @param yPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	@Override
	@Deprecated // todo harmonize with getVerticalDisplayPointValue  ---  replaced by gde.data.TrailRecord.getVerticalDisplayPointAsFormattedScaleValue(int)
	public double getVerticalDisplayPointScaleValue(int yPos, Rectangle drawAreaBounds) {
		double value;
		if (this.parent.isZoomMode || this.parent.isScopeMode)
			value = this.minZoomScaleValue + ((this.maxZoomScaleValue - this.minZoomScaleValue) * yPos) / drawAreaBounds.height;
		else
			value = this.minScaleValue + ((this.maxScaleValue - this.minScaleValue) * yPos) / drawAreaBounds.height;

		return value;
	}

	/**
	 * get the value corresponding the display point
	 * @param deltaPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	@Override
	@Deprecated
	public String getVerticalDisplayDeltaAsFormattedValue(int deltaPos, Rectangle drawAreaBounds) {
		String textValue;
		if (this.parent.isZoomMode || this.parent.isScopeMode)
			textValue = this.df.format((this.maxZoomScaleValue - this.minZoomScaleValue) * deltaPos / drawAreaBounds.height);
		else
			textValue = this.df.format((this.maxScaleValue - this.minScaleValue) * deltaPos / drawAreaBounds.height);

		return textValue;
	}

	/**
	 * get the slope value of two given points, unit depends on device configuration
	 * @param points describing the time difference (x) as well as the measurement difference (y)
	 * @return formated string of value
	 */
	@Override
	@Deprecated
	public String getSlopeValue(Point points) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.STRING_EMPTY + points.toString());
		double measureDelta;
		if (this.parent.isZoomMode)
			measureDelta = (this.maxZoomScaleValue - this.minZoomScaleValue) * points.y / this.parent.drawAreaBounds.height;
		else
			measureDelta = (this.maxScaleValue - this.minScaleValue) * points.y / this.parent.drawAreaBounds.height;
		//double timeDelta = (1.0 * points.x * this.size() - 1) / drawAreaBounds.width * this.getTimeStep_ms() / 1000; //sec
		//this.drawTimeWidth * xPos / this.parent.drawAreaBounds.width;
		double timeDelta = this.drawTimeWidth * points.x / this.parent.drawAreaBounds.width / 1000; //sec
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "measureDelta = " + measureDelta + " timeDelta = " + timeDelta); //$NON-NLS-1$ //$NON-NLS-2$
		return new DecimalFormat("0.0").format(measureDelta / timeDelta); //$NON-NLS-1$
	}

	/**
	 * @param replacementKey
	 * @return the replacement name of the specified key or an empty string if there is no key entry
	 */
	private String getDeviceXmlReplacement(String replacementKey) {
		return replacementKey != null ? this.xmlResource.getReplacement(replacementKey) : GDE.STRING_EMPTY;
	}

	public double getBoundedAvgValue() {
		return this.linearRegression.getAvg();
	}

	public double getBoundedSlopeValue(long timeStamp_ms) {
		return this.linearRegression.getResponse(timeStamp_ms);
	}

	public double getBoundedQuartile0Value() {
		return this.quantile.getQuartile0();
	}

	public double getBoundedQuartile1Value() {
		return this.quantile.getQuartile1();
	}

	public double getBoundedQuartile2Value() {
		return this.quantile.getQuartile2();
	}

	public double getBoundedQuartile3Value() {
		return this.quantile.getQuartile3();
	}

	public double getBoundedQuartile4Value() {
		return this.quantile.getQuartile4();
	}

	public double getBoundedLowerWhiskerValue() {
		return this.quantile.getQuantileLowerWhisker();
	}

	public double getBoundedUpperWhiskerValue() {
		return this.quantile.getQuantileUpperWhisker();
	}

	public boolean isValidBounds() {
		return this.linearRegression != null;
	}

}
