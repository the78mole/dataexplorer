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
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.swt.graphics.Point;

import gde.GDE;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.device.ScoreGroupType;
import gde.device.SettlementType;
import gde.device.StatisticsType;
import gde.device.TrailTypes;
import gde.histocache.HistoVault;
import gde.log.Level;
import gde.utils.HistoTimeLine;

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

	public final static String		TRAIL_TEXT_ORDINAL			= "_trailTextOrdinal";					// reference to the selected trail //$NON-NLS-1$

	private final TrailRecordSet	parentTrail;
	private final MeasurementType	measurementType;																				// measurement / settlement / scoregroup are options
	private final SettlementType	settlementType;																					// measurement / settlement / scoregroup are options
	private final ScoreGroupType	scoregroupType;																					// measurement / settlement / scoregroup are options
	private int										trailTextSelectedIndex	= -1;														// user selection from applicable trails, is saved in the graphics template
	private List<String>					applicableTrailsTexts;																	// the user may select one of these entries
	private List<Integer>					applicableTrailsOrdinals;																// maps all applicable trails in order to convert the user selection into a valid trail
	private TrailRecord[]					trailRecordSuite;																				// holds data points in case of trail suites

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
		this.scoregroupType = null;
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
		this.scoregroupType = null;
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
		this.scoregroupType = scoregroupType;
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
			else if (this.isScoregroup()) {
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
			for (int i = 0; i < this.trailRecordSuite.length; i++) {
				TrailRecord trailRecord = this.trailRecordSuite[i];
				Integer point;
				if (this.isMeasurement())
					point = histoVault.getMeasurementPoint(trailRecord.ordinal, trailRecord.getTrailOrdinal());
				else if (this.isSettlement())
					point = histoVault.getSettlementPoint(trailRecord.settlementType.getSettlementId(), trailRecord.getTrailOrdinal());
				else if (this.isScoregroup()) {
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
	 * @param timeLine
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @return point time, value; null if the trail record value is null
	 */
	public Point[] getDisplayPoints(HistoTimeLine timeLine, int xDisplayOffset, int yDisplayOffset) {
		Point[] points = new Point[timeLine.getScalePositions().size()];
		int i = 0;
		Integer value = 0;
		double offset = super.minDisplayValue * 1 / super.syncMasterFactor;
		for (Integer xPos : timeLine.getScalePositions().values()) {
			if ((value = super.realRealGet(i)) != null) {
				points[i] = new Point(xDisplayOffset + xPos, yDisplayOffset - Double.valueOf(((value / 1000.0) - offset) * super.displayScaleFactorValue).intValue());
			}
			i++;
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, Arrays.toString(points));
		return points;
		// return new Point(xDisplayOffset + Double.valueOf(this.getTime_ms(measurementPointIndex) * this.displayScaleFactorTime).intValue(), yDisplayOffset
		// - Double.valueOf(((this.get(measurementPointIndex) / 1000.0) - (this.minDisplayValue * 1 / this.syncMasterFactor)) * this.displayScaleFactorValue).intValue());
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
		double offset = super.minDisplayValue * 1 / super.syncMasterFactor;
		for (Integer xPos : timeLine.getScalePositions().values()) {
			if ((value = super.realRealGet(i)) != null) {
				int grad = value / 1000000;
				points[i] = new Point(xDisplayOffset + xPos,
						yDisplayOffset - Double.valueOf((((grad + ((super.realRealGet(i) / 1000000.0 - grad) / 0.60)) * 1000.0) - offset) * super.displayScaleFactorValue).intValue());
				i++;
			}
		}
		if (log.isLoggable(Level.OFF)) log.log(Level.OFF, "yPos = " + Arrays.toString(points)); //$NON-NLS-1$
		return points;
		// int grad = super.get(measurementPointIndex) / 1000000;
		// return new Point(xDisplayOffset + Double.valueOf(super.getTime_ms(measurementPointIndex) * super.displayScaleFactorTime).intValue(), yDisplayOffset
		// - Double.valueOf((((grad + ((this.get(measurementPointIndex) / 1000000.0 - grad) / 0.60)) * 1000.0) - (this.minDisplayValue * 1 / this.syncMasterFactor))
		// * this.displayScaleFactorValue).intValue());
	}

	public String getHistoTableRowText() {
		return this.getUnit().length() > 0 ? (this.getName() + GDE.STRING_BLANK_LEFT_BRACKET + this.getUnit() + GDE.STRING_RIGHT_BRACKET).intern() : this.getName().intern();
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
		double factor = getFactor();
		double offset = getOffset();
		double reduction = getReduction();
		if (!this.isTrailSuite()) {
			if (this.settings.isXAxisReversed()) {
				for (int i = 0; i < masterRecord.size(); i++)
					dataTableRow[i + 2] = masterRecord.realRealGet(i) != null ? this.getDecimalFormat().format((masterRecord.realRealGet(i) / 1000. - reduction) * factor + offset) : GDE.STRING_STAR;
			}
			else {
				for (int i = 0, j = masterRecord.size() - 1; i < masterRecord.size(); i++, j--)
					dataTableRow[i + 2] = masterRecord.realRealGet(i) != null ? this.getDecimalFormat().format((masterRecord.realRealGet(j) / 1000. - reduction) * factor + offset) : GDE.STRING_STAR;
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
						sb.append(lowerWhiskerRecord.realRealGet(i) != null ? this.getDecimalFormat().format((lowerWhiskerRecord.realGet(i) / 1000. - reduction) * factor + offset) : GDE.STRING_STAR);
						String delimiter = sb.length() > 3 ? GDE.STRING_COLON : GDE.STRING_BLANK_COLON_BLANK;
						sb.append(delimiter).append(medianRecord.realRealGet(i) != null ? this.getDecimalFormat().format((medianRecord.realRealGet(i) / 1000. - reduction) * factor + offset) : GDE.STRING_STAR);
						sb.append(delimiter)
								.append(upperWhiskerRecord.realRealGet(i) != null ? this.getDecimalFormat().format((upperWhiskerRecord.realRealGet(i) / 1000. - reduction) * factor + offset) : GDE.STRING_STAR);
						dataTableRow[i + 2] = sb.toString().intern();
					}
				}
			}
			else {
				for (int i = 0, j = masterRecord.size() - 1; i < masterRecord.size(); i++, j--)
					if (masterRecord.realRealGet(j) != null) {
						StringBuilder sb = new StringBuilder();
						sb.append(lowerWhiskerRecord.realRealGet(i) != null ? this.getDecimalFormat().format((lowerWhiskerRecord.realRealGet(j) / 1000. - reduction) * factor + offset) : GDE.STRING_STAR);
						String delimiter = sb.length() > 3 ? GDE.STRING_COLON : GDE.STRING_BLANK_COLON_BLANK;
						sb.append(delimiter).append(medianRecord.realRealGet(i) != null ? this.getDecimalFormat().format((medianRecord.realRealGet(j) / 1000. - reduction) * factor + offset) : GDE.STRING_STAR);
						sb.append(delimiter)
								.append(upperWhiskerRecord.realRealGet(i) != null ? this.getDecimalFormat().format((upperWhiskerRecord.realRealGet(j) / 1000. - reduction) * factor + offset) : GDE.STRING_STAR);
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
						sb.append(lowerRecord.realRealGet(i) != null ? this.getDecimalFormat().format((lowerRecord.realRealGet(i) / 1000. - reduction) * factor + offset) : GDE.STRING_STAR);
						String delimiter = sb.length() > 3 ? GDE.STRING_COLON : GDE.STRING_BLANK_COLON_BLANK;
						sb.append(delimiter).append(middleRecord.realRealGet(i) != null ? this.getDecimalFormat().format((middleRecord.realRealGet(i) / 1000. - reduction) * factor + offset) : GDE.STRING_STAR);
						sb.append(delimiter).append(upperRecord.realRealGet(i) != null ? this.getDecimalFormat().format((upperRecord.realRealGet(i) / 1000. - reduction) * factor + offset) : GDE.STRING_STAR);
						dataTableRow[i + 2] = sb.toString().intern();
					}
				}
			}
			else {
				for (int i = 0, j = masterRecord.size() - 1; i < masterRecord.size(); i++, j--)
					if (masterRecord.realRealGet(j) != null) {
						StringBuilder sb = new StringBuilder();
						sb.append(lowerRecord.realRealGet(i) != null ? this.getDecimalFormat().format((lowerRecord.realRealGet(j) / 1000. - reduction) * factor + offset) : GDE.STRING_STAR);
						String delimiter = sb.length() > 3 ? GDE.STRING_COLON : GDE.STRING_BLANK_COLON_BLANK;
						sb.append(delimiter).append(middleRecord.realRealGet(i) != null ? this.getDecimalFormat().format((middleRecord.realRealGet(j) / 1000. - reduction) * factor + offset) : GDE.STRING_STAR);
						sb.append(delimiter).append(upperRecord.realRealGet(i) != null ? this.getDecimalFormat().format((upperRecord.realRealGet(j) / 1000. - reduction) * factor + offset) : GDE.STRING_STAR);
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
				// todo applicablePrimitiveTrails[TrailTypes.REAL_SUM.ordinal()] = false; // in settlements only
			}

			// set non-suite trail types : triggered values like count/sum are not supported
			if (!hideAllTrails && !this.getDevice().isGPSCoordinates(this))
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
							this.applicableTrailsTexts.add(getStatistics().getCountTriggerText().intern());
						}
						else if (TrailTypes.values[i].equals(TrailTypes.REAL_SUM_TRIGGERED)) {
							this.applicableTrailsTexts.add(getStatistics().getSumTriggerText().intern());
						}
						else if (TrailTypes.values[i].equals(TrailTypes.REAL_TIME_SUM_TRIGGERED)) {
							this.applicableTrailsTexts.add(getStatistics().getSumTriggerTimeText().intern());
						}
						else if (TrailTypes.values[i].equals(TrailTypes.REAL_AVG_RATIO_TRIGGERED)) {
							this.applicableTrailsTexts.add(getStatistics().getRatioText().intern());
						}
						else if (TrailTypes.values[i].equals(TrailTypes.REAL_MAX_RATIO_TRIGGERED)) {
							this.applicableTrailsTexts.add(getStatistics().getRatioText().intern());
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
			if (!this.settlementType.getTrailDisplay().map(x -> x.isDiscloseAll()).orElse(false) && !this.getDevice().isGPSCoordinates(this) ) {
				if (this.settlementType.getEvaluation().getTransitionAmount() == null)
					TrailTypes.getPrimitives().stream().filter(x -> !x.isTriggered() && x.isSmartStatistics() == this.settings.isSmartStatistics()).forEach(x -> applicablePrimitiveTrails[x.ordinal()] = true);
				else
					throw new UnsupportedOperationException("TransitionAmount not implemented");
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
		else if (this.scoregroupType != null) {
			applicablePrimitiveTrails = new boolean[0]; // not required

			// build applicable trail type lists for display purposes
			this.applicableTrailsOrdinals = new ArrayList<Integer>();
			this.applicableTrailsTexts = new ArrayList<String>();
			if (this.scoregroupType != null) {
				for (int i = 0; i < this.scoregroupType.getScore().size(); i++) {
					this.applicableTrailsOrdinals.add(this.scoregroupType.getScore().get(i).getLabel().ordinal());
					this.applicableTrailsTexts.add(this.scoregroupType.getScore().get(i).getValue().intern());
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
					else if (this.scoregroupType != null) {
						this.trailRecordSuite[i] = new TrailRecord(this.device, this.ordinal, "suite" + trailTypes.getDisplaySequence(), this.scoregroupType, this.parentTrail, this.size()); //$NON-NLS-1$
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
		return this.scoregroupType == null ? TrailTypes.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex)).isSuite() : false;
	}

	public boolean isRangePlotSuite() {
		return this.scoregroupType == null ? TrailTypes.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex)).isRangePlot() : false;
	}

	public boolean isBoxPlotSuite() {
		return this.scoregroupType == null ? TrailTypes.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex)).isBoxPlot() : false;
	}

	public boolean isSuiteForSummation() {
		return this.scoregroupType == null ? TrailTypes.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex)).isForSummation() : false;
	}

	public boolean isMeasurement() {
		return this.measurementType != null;
	}

	public boolean isSettlement() {
		return this.settlementType != null;
	}

	public boolean isScoregroup() {
		return this.scoregroupType != null;
	}

	public MeasurementType getMeasurement() {
		return this.measurementType;
	}

	public SettlementType getSettlement() {
		return this.settlementType;
	}

	public ScoreGroupType getScoregroup() {
		return this.scoregroupType;
	}

	public StatisticsType getStatistics() {
		return this.measurementType.getStatistics();
	}

	/**
	 * select the most prioritized trail from the applicable trails. 
	 * @return
	 */
	public void setMostApplicableTrailTextOrdinal() {
		if (this.scoregroupType != null) {
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
	public double getFactor() { // todo maybe this is a better solution for the record class also (so we get rid of this override)
		double value = 1.0;
		PropertyType property = this.getProperty(IDevice.FACTOR);
		if (property != null) {
			value = Double.valueOf(property.getValue()).doubleValue();
		}
		else if (this.scoregroupType != null) {
			value = this.scoregroupType.getFactor();
		}
		else if (this.settlementType != null) {
			value = this.settlementType.getFactor();
		}
		else if (this.measurementType != null) {
			value = this.measurementType.getFactor();
		}
		else { // this is the old code which hopefully does never apply
			try {
				value = this.getDevice().getMeasurementFactor(this.parent.parent.number, this.ordinal);
			}
			catch (RuntimeException e) {
				// log.log(Level.WARNING, this.name + " use default value for property " + IDevice.FACTOR); // log warning and use default value
			}
		}
		return value;
	}

	@Override // reason is translateValue which accesses the device for offset etc.
	public double getOffset() { // todo maybe this is a better solution for the record class also (so we get rid of this override)
		double value = 0.0;
		PropertyType property = this.getProperty(IDevice.OFFSET);
		if (property != null) {
			value = Double.valueOf(property.getValue()).doubleValue();
		}
		else if (this.scoregroupType != null) {
			value = this.scoregroupType.getOffset();
		}
		else if (this.settlementType != null) {
			value = this.settlementType.getOffset();
		}
		else if (this.measurementType != null) {
			value = this.measurementType.getOffset();
		}
		else { // this is the old code which hopefully does never apply
			try {
				value = this.getDevice().getMeasurementOffset(this.parent.parent.number, this.ordinal);
			}
			catch (RuntimeException e) {
				// log.log(Level.WARNING, this.name + " use default value for property " + IDevice.OFFSET); // log warning and use default value
			}
		}
		return value;
	}

	@Override // reason is translateValue which accesses the device for offset etc.
	public double getReduction() { // todo maybe this is a better solution for the record class also (so we get rid of this override)
		double value = 0.0;
		PropertyType property = this.getProperty(IDevice.REDUCTION);
		if (property != null) {
			value = Double.valueOf(property.getValue()).doubleValue();
		}
		else if (this.scoregroupType != null) {
			value = this.scoregroupType.getReduction();
		}
		else if (this.settlementType != null) {
			value = this.settlementType.getReduction();
		}
		else if (this.measurementType != null) {
			value = this.measurementType.getReduction();
		}
		else { // this is the old code which hopefully does never apply
			try {
				String strValue = (String) this.getDevice().getMeasurementPropertyValue(this.parent.parent.number, this.ordinal, IDevice.REDUCTION);
				if (strValue != null && strValue.length() > 0) value = Double.valueOf(strValue.trim().replace(',', '.')).doubleValue();
			}
			catch (RuntimeException e) {
				// log.log(Level.WARNING, this.name + " use default value for property " + IDevice.REDUCTION); // log warning and use default value
			}
		}
		return value;
	}

	/**
	 * @return the array of suite records; it may consist of the trail record itself which simplifies things
	 */
	public TrailRecord[] getTrailRecordSuite() {
		return this.trailRecordSuite;
	}

	/**
	 * @return the value of the label property from the device channel entry.
	 */
	public String getLabel() {
		return this.measurementType != null ? this.measurementType.getLabel() : this.settlementType != null ? this.settlementType.getLabel() : this.scoregroupType.getLabel();
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
		boolean isValidDisplayRecord = this.isMeasurement() || (this.isSettlement() && this.settings.isDisplaySettlements()) || (this.isScoregroup() && this.settings.isDisplayScores());
		return isValidDisplayRecord && super.isScaleVisible();
	}

}
