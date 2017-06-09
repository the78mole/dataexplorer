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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
import gde.utils.SingleResponseRegression;
import gde.utils.SingleResponseRegression.RegressionType;
import gde.utils.Spot;
import gde.utils.UniversalQuantile;

/**
 * holds histo data points of one measurement or settlement; score points are a third option.
 * a histo data point holds one aggregated value (e.g. max, avg, quantile).
 * supports multiple curves (trail suites).
 * @author Thomas Eickert
 */
public class TrailRecord extends Record { // todo maybe a better option is to create a common base class for Record and TrailRecord.
	private final static String								$CLASS_NAME							= TrailRecord.class.getName();
	private final static long									serialVersionUID				= 110124007964748556L;
	private final static Logger								log											= Logger.getLogger($CLASS_NAME);

	public final static String								TRAIL_TEXT_ORDINAL			= "_trailTextOrdinal";						// reference to the selected trail //$NON-NLS-1$

	private final TrailRecordSet							parentTrail;
	private final MeasurementType							measurementType;																					// measurement / settlement / scoregroup are options
	private final SettlementType							settlementType;																						// measurement / settlement / scoregroup are options
	private final ScoreGroupType							scoreGroupType;																						// measurement / settlement / scoregroup are options
	private int																trailTextSelectedIndex	= -1;															// user selection from applicable trails, is saved in the graphics template
	private List<String>											applicableTrailsTexts;																		// the user may select one of these entries
	private List<Integer>											applicableTrailsOrdinals;																	// maps all applicable trails in order to convert the user selection into a valid trail
	private SuiteRecord[]											trailRecordSuite				= new SuiteRecord[0];							// holds data points in case of trail suites
	private double														factor									= Double.MIN_VALUE;
	private double														offset									= Double.MIN_VALUE;
	private double														reduction								= Double.MIN_VALUE;

	final DeviceXmlResource										xmlResource							= DeviceXmlResource.getInstance();
	private SingleResponseRegression<Double>	regression							= null;
	private UniversalQuantile<Spot<Double>>		quantile								= null;

	/**
	 * Data points of one measurement or line or curve.
	 * Member of a trail record suite.
	 */
	private class SuiteRecord extends Vector<Integer> {
		private static final long	serialVersionUID	= 8757759753520551985L;

		private final int					trailOrdinal;

		private int								maxRecordValue		= Integer.MIN_VALUE;		// max value of the curve
		private int								minRecordValue		= Integer.MAX_VALUE;		// min value of the curve

		public SuiteRecord(int newTrailOrdinal, int initialCapacity) {
			super(initialCapacity);
			this.trailOrdinal = newTrailOrdinal;
		}

		/**
		 * Add a data point to the record and set minimum and maximum.
		 * @param point
		 */
		@Override
		public synchronized void addElement(Integer point) {
			if (point == null) {
				if (this.isEmpty()) {
					this.maxRecordValue = Integer.MIN_VALUE;
					this.minRecordValue = Integer.MAX_VALUE;
				}
			}
			else {
				if (this.isEmpty())
					this.minRecordValue = this.maxRecordValue = point;
				else {
					if (point > this.maxRecordValue) this.maxRecordValue = point;
					if (point < this.minRecordValue) this.minRecordValue = point;
				}
			}
			super.addElement(point);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.trailOrdinal + " adding point = " + point); //$NON-NLS-1$
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, this.trailOrdinal + " minValue = " + this.minRecordValue + " maxValue = " + this.maxRecordValue); //$NON-NLS-1$ //$NON-NLS-2$
		}

		public int getTrailOrdinal() {
			return this.trailOrdinal;
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

	@Deprecated
	@Override
	public synchronized boolean add(Integer point) {
		throw new UnsupportedOperationException(" " + point); //$NON-NLS-1$
	}

	/**
	 * Add a data point to the record and set minimum and maximum.
	 * @param point
	 */
	@Override
	public synchronized void addElement(Integer point) {
		if (point == null) {
			if (this.isEmpty()) {
				this.minValue = Integer.MAX_VALUE;
				this.maxValue = Integer.MIN_VALUE;
			}
		}
		else {
			if (this.isEmpty())
				this.minValue = this.maxValue = point;
			else {
				if (point > this.maxValue) this.maxValue = point;
				if (point < this.minValue) this.minValue = point;
			}
		}
		super.addElement(point);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.name + " adding point = " + point); //$NON-NLS-1$
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, this.name + " minValue = " + this.minValue + " maxValue = " + this.maxValue); //$NON-NLS-1$ //$NON-NLS-2$
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

		if (this.trailRecordSuite == null) {
			super.addElement(null);
		}
		else if (!this.isTrailSuite()) {
			Integer point;
			if (this.isMeasurement())
				point = histoVault.getMeasurementPoint(this.ordinal, this.getTrailOrdinal());
			else if (this.isSettlement())
				point = histoVault.getSettlementPoint(this.settlementType.getSettlementId(), this.getTrailOrdinal());
			else if (this.isScoreGroup()) {
				if (log.isLoggable(Level.FINEST))
					log.log(Level.FINEST, String.format(" %s trail %3d  %s %s", this.getName(), this.getTrailOrdinal(), histoVault.getVaultFileName(), histoVault.getLogFilePath())); //$NON-NLS-1$
				point = histoVault.getScorePoint(this.getTrailOrdinal());
			}
			else
				throw new UnsupportedOperationException("length == 1"); //$NON-NLS-1$

			this.addElement(point);
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, String.format(" %s trail %3d  %s %s %d", getName(), this.getTrailOrdinal(), histoVault.getLogFilePath(), point)); //$NON-NLS-1$
		}
		else {
			int minVal = Integer.MAX_VALUE, maxVal = Integer.MIN_VALUE; // min/max depends on all values of the suite

			int masterPoint = 0; // this is the basis value for adding or subtracting standard deviations
			boolean summationSign = false; // false means subtract, true means add
			for (SuiteRecord trailRecord : this.trailRecordSuite) {
				Integer point;
				if (this.isMeasurement())
					point = histoVault.getMeasurementPoint(this.ordinal, trailRecord.getTrailOrdinal());
				else if (this.isSettlement())
					point = histoVault.getSettlementPoint(this.settlementType.getSettlementId(), trailRecord.getTrailOrdinal());
				else if (this.isScoreGroup()) {
					point = histoVault.getScorePoint(this.getTrailOrdinal());
				}
				else
					throw new UnsupportedOperationException("length > 1"); //$NON-NLS-1$

				if (point != null) {
					if (this.isSuiteForSummation()) {
						point = summationSign ? masterPoint + 2 * point : masterPoint - 2 * point;
						summationSign = !summationSign; // toggle the add / subtract mode
					}
					else {
						masterPoint = point; // use in the next iteration if summation is necessary, e.g. avg+2*sd
						summationSign = false;
					}
					trailRecord.addElement(point);
				}
				else {
					trailRecord.addElement(point);
				}
				if (log.isLoggable(Level.FINEST))
					log.log(Level.FINEST, String.format(" %s trail %3d  %s %s %d minVal=%d maxVal=%d", this.getName(), this.getTrailOrdinal(), histoVault.getLogFilePath(), point, minVal, maxVal)); //$NON-NLS-1$
			}

		}
		log.log(Level.FINEST, this.getName() + " trail ", this); //$NON-NLS-1$
	}

	/**
	 * @return the point size of a single curve or a suite
	 */
	@Override
	public int realSize() {
		return isTrailSuite() ? this.trailRecordSuite[0].size() : super.size();
	}

	public int getSuiteMaxValue() {
		return Arrays.stream(this.trailRecordSuite).mapToInt(s -> s.maxRecordValue).max().orElseThrow(() -> new UnsupportedOperationException());
	}

	public int getSuiteMinValue() {
		return Arrays.stream(this.trailRecordSuite).mapToInt(s -> s.minRecordValue).min().orElseThrow(() -> new UnsupportedOperationException());
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
			if (super.elementAt(i) != null) {
				points.add(new Point(xDisplayOffset + timeLine.getScalePositions().get((long) this.parent.timeStep_ms.getTime_ms(i)),
						yDisplayOffset - (int) (((super.elementAt(i) / 1000.0) - tmpOffset) * super.displayScaleFactorValue)));
			}
			else {
				points.add(null);
			}
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, Arrays.toString(points.toArray()));
		return points.toArray(new Point[0]);
	}

	/**
	 * query the values for display.
	 * supports multiple entries for the same x axis position.
	 * @param timeLine
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @param suiteOrdinal the 0-based ordinal number of the requested suite record
	 * @return point time, value; null if the trail record value is null
	 */
	public Point[] getDisplayPoints(HistoTimeLine timeLine, int xDisplayOffset, int yDisplayOffset, int suiteOrdinal) {
		List<Point> points = new ArrayList<>();
		double tmpOffset = this.minDisplayValue * 1 / this.syncMasterFactor;
		Vector<Integer> suiteRecord = this.trailRecordSuite[suiteOrdinal];
		for (int i = 0; i < this.parent.timeStep_ms.size(); i++) {
			if (suiteRecord.elementAt(i) != null) {
				points.add(new Point(xDisplayOffset + timeLine.getScalePositions().get((long) this.parent.timeStep_ms.getTime_ms(i)),
						yDisplayOffset - (int) (((suiteRecord.elementAt(i) / 1000.0) - tmpOffset) * this.displayScaleFactorValue)));
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
			if ((value = super.elementAt(i)) != null) {
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
	 * query the values for display.
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
		final double tmpOffset = this.minDisplayValue * 1 / this.syncMasterFactor; // minDisplayValue is GPS DD format * 1000
		Vector<Integer> suiteRecord = this.trailRecordSuite[suiteOrdinal];
		for (Integer xPos : timeLine.getScalePositions().values()) {
			if ((value = suiteRecord.elementAt(i)) != null) {
				final double decimalDegreeValue = value / 1000000 + value % 1000000 / 600000.;
				points[i] = new Point(xDisplayOffset + xPos, yDisplayOffset - (int) ((decimalDegreeValue * 1000. - tmpOffset) * this.displayScaleFactorValue));
			}
			i++;
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "yPos = " + Arrays.toString(points)); //$NON-NLS-1$
		return points;
	}

	/**
	 * get all calculated and formated data table points.
	 * @return record name and trail text followed by formatted values as string array
	 */
	public String[] getHistoTableRow() {
		final String[] dataTableRow = new String[this.realSize() + 2];
		dataTableRow[0] = getHistoTableRowText().intern();
		dataTableRow[1] = this.getTrailText().intern();
		if (!this.isTrailSuite()) {
			if (this.settings.isXAxisReversed()) {
				for (int i = 0; i < this.size(); i++)
					if (this.elementAt(i) != null) dataTableRow[i + 2] = this.getFormattedTableValue(this.realGet(i) / 1000.);
			}
			else {
				for (int i = 0, j = this.size() - 1; i < this.size(); i++, j--)
					if (this.elementAt(j) != null) dataTableRow[i + 2] = this.getFormattedTableValue(this.realGet(j) / 1000.);
			}
		}
		else if (this.isBoxPlotSuite()) {
			final SuiteRecord lowerWhiskerRecord = this.trailRecordSuite[5];
			final SuiteRecord medianRecord = this.trailRecordSuite[2];
			final SuiteRecord upperWhiskerRecord = this.trailRecordSuite[6];
			if (this.settings.isXAxisReversed()) {
				for (int i = 0; i < medianRecord.size(); i++) {
					if (medianRecord.elementAt(i) != null) {
						StringBuilder sb = new StringBuilder();
						sb.append(String.format("%.8s", getFormattedTableValue(lowerWhiskerRecord.get(i) / 1000.))); //$NON-NLS-1$
						String delimiter = sb.length() > 3 ? Character.toString((char) 183) : GDE.STRING_BLANK_COLON_BLANK;
						sb.append(delimiter).append(String.format("%.8s", getFormattedTableValue(medianRecord.get(i) / 1000.))); //$NON-NLS-1$
						sb.append(delimiter).append(String.format("%.8s", getFormattedTableValue(upperWhiskerRecord.get(i) / 1000.))); //$NON-NLS-1$
						dataTableRow[i + 2] = sb.toString().intern();
					}
				}
			}
			else {
				for (int i = 0, j = medianRecord.size() - 1; i < medianRecord.size(); i++, j--)
					if (medianRecord.elementAt(j) != null) {
						StringBuilder sb = new StringBuilder();
						sb.append(String.format("%.8s", getFormattedTableValue(lowerWhiskerRecord.get(j) / 1000.))); //$NON-NLS-1$
						String delimiter = sb.length() > 3 ? Character.toString((char) 183) : GDE.STRING_BLANK_COLON_BLANK;
						sb.append(delimiter).append(String.format("%.8s", getFormattedTableValue(medianRecord.get(j) / 1000.))); //$NON-NLS-1$
						sb.append(delimiter).append(String.format("%.8s", getFormattedTableValue(upperWhiskerRecord.get(j) / 1000.))); //$NON-NLS-1$
						dataTableRow[i + 2] = sb.toString().intern();
					}
			}
		}
		else if (isRangePlotSuite()) {
			final SuiteRecord lowerRecord = this.trailRecordSuite[1];
			final SuiteRecord middleRecord = this.trailRecordSuite[0];
			final SuiteRecord upperRecord = this.trailRecordSuite[2];
			if (this.settings.isXAxisReversed()) {
				for (int i = 0; i < middleRecord.size(); i++) {
					if (middleRecord.elementAt(i) != null) {
						StringBuilder sb = new StringBuilder();
						sb.append(String.format("%.8s", getFormattedTableValue(lowerRecord.get(i) / 1000.))); //$NON-NLS-1$
						String delimiter = sb.length() > 3 ? Character.toString((char) 183) : GDE.STRING_BLANK_COLON_BLANK;
						sb.append(delimiter).append(String.format("%.8s", getFormattedTableValue(middleRecord.get(i) / 1000.))); //$NON-NLS-1$
						sb.append(delimiter).append(String.format("%.8s", getFormattedTableValue(upperRecord.get(i) / 1000.))); //$NON-NLS-1$
						dataTableRow[i + 2] = sb.toString().intern();
					}
				}
			}
			else {
				for (int i = 0, j = middleRecord.size() - 1; i < middleRecord.size(); i++, j--)
					if (middleRecord.elementAt(j) != null) {
						StringBuilder sb = new StringBuilder();
						sb.append(String.format("%.8s", getFormattedTableValue(lowerRecord.get(j) / 1000.))); //$NON-NLS-1$
						String delimiter = sb.length() > 3 ? Character.toString((char) 183) : GDE.STRING_BLANK_COLON_BLANK;
						sb.append(delimiter).append(String.format("%.8s", getFormattedTableValue(middleRecord.get(j) / 1000.))); //$NON-NLS-1$
						sb.append(delimiter).append(String.format("%.8s", getFormattedTableValue(middleRecord.get(j) / 1000.))); //$NON-NLS-1$
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
				this.trailRecordSuite = new SuiteRecord[suite.size()];
				for (int i = 0; i < suite.size(); i++) {
					this.trailRecordSuite[i] = new SuiteRecord(suite.get(i).ordinal(), this.size());
				}
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
	 * @return the suite size
	 */
	public int getSuiteSize() {
		return this.trailRecordSuite.length;
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
	 * @return true if the record or the suite contains reasonable data which can be displayed
	 */
	@Override // reason is trail record suites with a master record without point values and minValue/maxValue != 0 in case of empty records
	public boolean hasReasonableData() {
		boolean hasReasonableData = false;
		if (this.trailRecordSuite.length == 0) {
			hasReasonableData = this.realSize() > 0 && this.minValue != Integer.MAX_VALUE && this.maxValue != Integer.MIN_VALUE
					&& (this.minValue != this.maxValue || this.device.translateValue(this, this.maxValue / 1000.0) != 0.0);
		}
		else {
			for (SuiteRecord suiteRecord : this.trailRecordSuite) {
				if (suiteRecord.size() > 0 && suiteRecord.minRecordValue != Integer.MAX_VALUE && suiteRecord.maxRecordValue != Integer.MIN_VALUE
						&& (suiteRecord.minRecordValue != suiteRecord.maxRecordValue || this.device.translateValue(this, suiteRecord.maxRecordValue / 1000.0) != 0.0)) {
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
	 * @param points
	 * @param fromIndex
	 * @param toIndex
	 * @return the portion of the timestamps_ms and aggregated translated values between fromIndex, inclusive, and toIndex, exclusive. (If fromIndex and toIndex are equal, the returned List is empty.)
	 */
	public List<Spot<Double>> getSubPoints(Vector<Integer> points, int fromIndex, int toIndex) {
		int recordSize = toIndex - fromIndex;
		List<Spot<Double>> result = new ArrayList<>(recordSize);
		for (int i = fromIndex; i < toIndex; i++) {
			if (points.elementAt(i) != null) {
				result.add(new Spot<Double>(this.parent.timeStep_ms.getTime_ms(i), this.device.translateValue(this, points.elementAt(i) / 1000.)));
			}
		}
		log.log(Level.FINER, "", Arrays.toString(result.toArray()));
		return result;
	}

	/**
	 * Builds value lists from the points within the bounds.
	 * The list is ordered from timeStamp1 to timeStamp2.
	 * @param timeStamp1_ms is the time of the delta measurement start value
	 * @param timeStamp2_ms is the time of the delta measurement end value
	 */
	public void setBounds(long timeStamp1_ms, long timeStamp2_ms) {
		this.regression = null;
		this.quantile = null;

		final Vector<Integer> points;
		if (!this.isTrailSuite())
			points = this;
		else if (this.isBoxPlotSuite())
			points = this.trailRecordSuite[2];
		else if (isRangePlotSuite())
			points = this.trailRecordSuite[0];
		else
			throw new UnsupportedOperationException();

		final int measureIndex = this.parentTrail.getIndex(timeStamp1_ms);
		final int deltaIndex = this.parentTrail.getIndex(timeStamp2_ms);
		final List<Spot<Double>> subPoints = getSubPoints(points, Math.min(measureIndex, deltaIndex), Math.max(measureIndex, deltaIndex) + 1);

		if (!subPoints.isEmpty()) {
			boolean isSample = true;
			this.quantile = new UniversalQuantile<>(subPoints, isSample); // take all points for display

			UniversalQuantile<Spot<Double>> tmpQuantile = new UniversalQuantile<>(subPoints, isSample, UniversalQuantile.boxplotSigmaFactor, UniversalQuantile.boxplotOutlierFactor); // regression without Tukey outliers
			subPoints.removeAll(tmpQuantile.getOutliers());
			this.regression = new SingleResponseRegression<>(subPoints, RegressionType.QUADRATIC);
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER,
						String.format("xAxisSize=%d regressionRealSize=%d starts at %tF %tR", this.quantile.getSize(), this.regression.getRealSize(), new Date(timeStamp1_ms), new Date(timeStamp1_ms)));
			}
		}
	}

	/**
	 * @return the formatted translated average of all bounds values
	 */
	public String getFormattedBoundsAvg() {
		return this.getFormattedScaleValue(this.regression.getAvg());
	}

	/**
	 * @return the formatted translated difference between the left and right bounds values
	 */
	public String getFormattedBoundsDelta() {
		return this.getFormattedScaleValue(0. - this.regression.getDelta());
	}

	/**
	 * @return the formatted translated regression slope of the bounds values based on months
	 */
	public String getFormattedBoundsSlope() {
		return this.getFormattedScaleValue(this.regression.getSlope() * GDE.ONE_HOUR_MS * 24 * 365 / 12);
	}

	/**
	 * Supports suites and null values.
	 * @param index
	 * @return the translated and decimal formatted value at the given index or a standard string in case of a null value
	 */
	@Override
	public String getFormattedMeasureValue(int index) {
		final Vector<Integer> points;
		if (!this.isTrailSuite())
			points = this;
		else if (this.isBoxPlotSuite())
			points = this.trailRecordSuite[2];
		else if (isRangePlotSuite())
			points = this.trailRecordSuite[0];
		else
			throw new UnsupportedOperationException();

		if (points.elementAt(index) != null) {
			return getFormattedTableValue(index);
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
		throw new UnsupportedOperationException();
	}

	/**
	* get the time in msec at given horizontal display position
	* @param xPos of the display point
	* @return time value in msec
	*/
	@Override
	@Deprecated // replaced by gde.utils.HistoTimeLine.getTimestamp(int)
	public double getHorizontalDisplayPointTime_ms(int xPos) {
		throw new UnsupportedOperationException();
	}

	/**
	* get the formatted time with unit at given position
	* @param xPos of the display point
	* @return string of time value in simple date format HH:ss:mm:SSS
	*/
	@Override
	@Deprecated // replaced by gde.utils.HistoTimeLine.getTimestamp(int)
	public String getHorizontalDisplayPointAsFormattedTimeWithUnit(int xPos) {
		throw new UnsupportedOperationException();
	}

	/**
	 * calculate best fit index in data vector from given display point relative to the (zoomed) display width
	 * @param xPos
	 * @return position integer value
	 */
	@Override
	@Deprecated // replaced by gde.data.TrailRecordSet.getIndex(gde.utils.HistoTimeLine.getTimestamp(int))
	public int getHorizontalPointIndexFromDisplayPoint(int xPos) {
		throw new UnsupportedOperationException();
	}

	/**
	 * supports suites and null values.
	 * @param index
	 * @return yPos in pixel (with top@0 and bottom@drawAreaBounds.height) or Integer.MIN_VALUE if the index value is null
	 */
	public int getVerticalDisplayPos(int index) {
		final Vector<Integer> points;
		if (!this.isTrailSuite())
			points = this;
		else if (this.isBoxPlotSuite())
			points = this.trailRecordSuite[2];
		else if (isRangePlotSuite())
			points = this.trailRecordSuite[0];
		else
			throw new UnsupportedOperationException();

		int verticalDisplayPos = Integer.MIN_VALUE;
		if (this.device.isGPSCoordinates(this)) {
			if (points.elementAt(index) != null) {
				double decimalDegreeValue = points.elementAt(index) / 1000000 + points.elementAt(index) % 1000000 / 600000.;
				verticalDisplayPos = this.parent.drawAreaBounds.height - (int) ((decimalDegreeValue * 1000. - this.minDisplayValue * 1 / this.syncMasterFactor) * this.displayScaleFactorValue);
			}
		}
		else if (points.elementAt(index) != null)
			verticalDisplayPos = this.parent.drawAreaBounds.height - (int) ((points.elementAt(index) / 1000.0 - this.minDisplayValue * 1 / this.syncMasterFactor) * this.displayScaleFactorValue);

		return verticalDisplayPos;
	}

	public int getVerticalDisplayPos(double translatedValue) {
		int verticalDisplayPos = Integer.MIN_VALUE;

		int point = (int) (this.device.reverseTranslateValue(this, translatedValue) * 1000.);
		if (this.device.isGPSCoordinates(this)) {
			double decimalDegreeValue = point / 1000000 + point % 1000000 / 600000.;
			verticalDisplayPos = this.parent.drawAreaBounds.height - (int) ((decimalDegreeValue * 1000. - this.minDisplayValue * 1 / this.syncMasterFactor) * this.displayScaleFactorValue);
		}
		else {
			verticalDisplayPos = this.parent.drawAreaBounds.height - (int) ((point / 1000.0 - this.minDisplayValue * 1 / this.syncMasterFactor) * this.displayScaleFactorValue);
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
	@Deprecated // replaced by gde.data.TrailRecord.getVerticalDisplayPos
	public int getVerticalDisplayPointValue(int xPos) {
		throw new UnsupportedOperationException();
	}

	/**
	 * get the formatted scale value corresponding the vertical display point
	 * @param yPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	@Override
	@Deprecated // replaced by gde.data.TrailRecord.getVerticalDisplayPointAsFormattedScaleValue(int)
	public String getVerticalDisplayPointAsFormattedScaleValue(int yPos, Rectangle drawAreaBounds) {
		throw new UnsupportedOperationException();
	}

	/**
	 * get the scale value corresponding the vertical display point
	 * @param yPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	@Override
	@Deprecated // replaced by gde.data.TrailRecord.getVerticalDisplayPos
	public double getVerticalDisplayPointScaleValue(int yPos, Rectangle drawAreaBounds) {
		throw new UnsupportedOperationException();
	}

	/**
	 * get the value corresponding the display point
	 * @param deltaPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	@Override
	@Deprecated // replaced by gde.data.TrailRecord.getVerticalDisplayPos
	public String getVerticalDisplayDeltaAsFormattedValue(int deltaPos, Rectangle drawAreaBounds) {
		throw new UnsupportedOperationException();
	}

	/**
	 * get the slope value of two given points, unit depends on device configuration
	 * @param points describing the time difference (x) as well as the measurement difference (y)
	 * @return formated string of value
	 */
	@Override
	@Deprecated // replaced by getBoundedSlopeValue
	public String getSlopeValue(Point points) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @param replacementKey
	 * @return the replacement name of the specified key or an empty string if there is no key entry
	 */
	private String getDeviceXmlReplacement(String replacementKey) {
		return replacementKey != null ? this.xmlResource.getReplacement(replacementKey) : GDE.STRING_EMPTY;
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
		return this.regression != null;
	}

	public double[] getBoundedParabolaCoefficients() {
		return new double[] { this.regression.getAlpha(), this.regression.getBeta(), this.regression.getGamma() };
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
}
