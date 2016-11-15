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

package gde.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.graphics.Point;

import gde.GDE;
import gde.device.EvaluationType;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.device.ScoreType;
import gde.device.ScoregroupType;
import gde.device.SettlementType;
import gde.device.StatisticsType;
import gde.exception.DataInconsitsentException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.HistoTimeLine;
import gde.utils.Quantile;
import gde.utils.Quantile.Fixings;

/**
 * holds histo data points of one measurement or settlement; score points are a third option.
 * a histo data point holds one aggregated value (e.g. max, avg, quantile).
 * supports multiple curves (trail suites).
 * @author Thomas Eickert
 */
public class TrailRecord extends Record { // todo maybe a better option is to create a common base class for Record, HistoSettlement and TrailRecord.
	private final static String		$CLASS_NAME					= TrailRecord.class.getName();
	private final static long			serialVersionUID		= 110124007964748556L;
	private final static Logger		log									= Logger.getLogger($CLASS_NAME);

	public final static String		TRAIL_TEXT_ORDINAL	= "_trailTextOrdinal";								// reference to the selected trail //$NON-NLS-1$
	public final static String[]	trailPropertyKeys		= new String[] { TRAIL_TEXT_ORDINAL };
	public final static int				RANGE_PLOT_SIZE			= 3;
	public final static int				BOX_PLOT_SIZE				= 7;

	public enum TrailType {
		REAL_AVG(0, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0750)), // average
		//		REAL_COUNT_OBS(11, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0751)), // counter
		REAL_FIRST(3, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0752)), //
		REAL_LAST(4, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0753)), //
		REAL_MAX(1, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0754)), //
		REAL_MIN(2, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0755)), //
		REAL_SD(5, true, 1, false, Messages.getString(MessageIds.GDE_MSGT0756)), //
		REAL_COUNT_TRIGGERED(10, false, 1, true, Messages.getString(MessageIds.GDE_MSGT0757)), //
		REAL_SUM_TRIGGERED(6, false, 1, true, Messages.getString(MessageIds.GDE_MSGT0758)), //
		REAL_TIME_SUM_TRIGGERED(7, false, 1, true, Messages.getString(MessageIds.GDE_MSGT0759)), //
		REAL_AVG_RATIO_TRIGGERED(8, false, 1, true, Messages.getString(MessageIds.GDE_MSGT0760)), //
		REAL_MAX_RATIO_TRIGGERED(9, false, 1, true, Messages.getString(MessageIds.GDE_MSGT0761)), //
		REAL_SUM(12, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0762)), //
		//		SCORE(13, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0763)),
		//		AVG(14, false, false, false, Messages.getString(MessageIds.GDE_MSGT0764)), //
		//		SUM(15, false, false, false, Messages.getString(MessageIds.GDE_MSGT0765)), //
		//		COUNT_OBS(16, false, false, false, Messages.getString(MessageIds.GDE_MSGT0766)),
		Q0(17, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0767)), // quantile 0 is q(0%) which is the minimum
		Q1(18, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0768)), // quantile 1 is q(25%)
		Q2(19, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0769)), // quantile 2 is q(50%) which is the median
		Q3(20, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0770)), // quantile 3 is q(75%)
		Q4(21, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0771)), // quantile 4 is q(100%) which is the maximum
		Q_25_PERMILLE(22, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0772)), // octile 1 is q(12,5%)
		Q_975_PERMILLE(23, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0773)), // octile 7 is q(87,5%)
		Q_LOWER_WHISKER(24, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0774)), // quantile of the closest value to the 4 * IQR lower limit
		Q_UPPER_WHISKER(25, false, 1, false, Messages.getString(MessageIds.GDE_MSGT0775)), // quantile of the closest value to the 4 * IQR upper limit
		//		SD(26, true, false, false, Messages.getString(MessageIds.GDE_MSGT0776)), //
		SUITE_REAL_AVG_SD(1001, false, RANGE_PLOT_SIZE, false, Messages.getString(MessageIds.GDE_MSGT0781)), //
		SUITE_REAL_AVG_MIN_MAX(1002, false, RANGE_PLOT_SIZE, false, Messages.getString(MessageIds.GDE_MSGT0782)), //
		//		SUITE_AVG_SD(1003, false, true, false, Messages.getString(MessageIds.GDE_MSGT0783)), //
		SUITE_BOX_PLOT(1004, false, BOX_PLOT_SIZE, false, Messages.getString(MessageIds.GDE_MSGT0784)), // 4 * IQR range (John. W. Tukey)
		SUITE_BOX_PLOT_95(1005, false, BOX_PLOT_SIZE, false, Messages.getString(MessageIds.GDE_MSGT0785)), // 95% range
		SUITE_Q0_Q2_Q4(1006, false, RANGE_PLOT_SIZE, false, Messages.getString(MessageIds.GDE_MSGT0786)), //
		SUITE_Q1_Q2_Q3(1007, false, RANGE_PLOT_SIZE, false, Messages.getString(MessageIds.GDE_MSGT0787));//

		private final int							displaySequence;
		private final boolean					isForSummation;
		private final int							suiteSize;
		private final boolean					isTriggered;
		private final String					displayName;
		/**
		 * use this to avoid repeatedly cloning actions instead of values()
		 */
		public final static TrailType	values[]	= values();

		private TrailType(int displaySequence, boolean isForSummation, int suiteSize, boolean isTriggered, String displayName) {
			this.displaySequence = displaySequence;
			this.isForSummation = isForSummation;
			this.suiteSize = suiteSize;
			this.isTriggered = isTriggered;
			this.displayName = displayName;
		}

		public static TrailType fromDisplayName(String displayName) {
			if (displayName != null) {
				for (TrailType trailType : TrailType.values()) {
					if (displayName.equalsIgnoreCase(trailType.displayName)) {
						return trailType;
					}
				}
			}
			return null;
		}

		public static TrailType fromOrdinal(int ordinal) {
			return TrailType.values[ordinal];
		}

		public static EnumSet<TrailType> getPrimitives() {
			List<TrailType> trailTypes = new ArrayList<>();
			for (TrailType type : TrailType.values) {
				if (!type.isSuite()) trailTypes.add(type);
			}
			return EnumSet.copyOf(trailTypes);
		}

		private static EnumSet<TrailType> getSuites() {
			List<TrailType> trailTypes = new ArrayList<>();
			for (TrailType type : TrailType.values) {
				if (type.isSuite()) trailTypes.add(type);
			}
			return EnumSet.copyOf(trailTypes);
		}

		public static EnumSet<TrailType> getSuite(TrailType trailType) {
			List<TrailType> trailTypes = new ArrayList<>();
			if (trailType.isSuite()) {
				if (trailType.equals(SUITE_REAL_AVG_SD)) {
					trailTypes.add(REAL_AVG); // master record for adding sd must be in front of the sd records
					trailTypes.add(REAL_SD); // avg - n times sd
					trailTypes.add(REAL_SD); // avg + n times sd
				}
				else if (trailType.equals(SUITE_REAL_AVG_MIN_MAX)) {
					trailTypes.add(REAL_AVG);
					trailTypes.add(REAL_MIN);
					trailTypes.add(REAL_MAX);
				}
				else if (trailType.equals(SUITE_BOX_PLOT)) {
					trailTypes.add(Q0);
					trailTypes.add(Q1);
					trailTypes.add(Q2);
					trailTypes.add(Q3);
					trailTypes.add(Q4);
					trailTypes.add(Q_LOWER_WHISKER);
					trailTypes.add(Q_UPPER_WHISKER);
				}
				else if (trailType.equals(SUITE_BOX_PLOT_95)) {
					trailTypes.add(Q0);
					trailTypes.add(Q1);
					trailTypes.add(Q2);
					trailTypes.add(Q3);
					trailTypes.add(Q4);
					trailTypes.add(Q_25_PERMILLE);
					trailTypes.add(Q_975_PERMILLE);
				}
				else if (trailType.equals(SUITE_Q0_Q2_Q4)) {
					trailTypes.add(Q2);
					trailTypes.add(Q0);
					trailTypes.add(Q4);
				}
				else if (trailType.equals(SUITE_Q1_Q2_Q3)) {
					trailTypes.add(Q2);
					trailTypes.add(Q1);
					trailTypes.add(Q3);
				}
				else
					throw new IllegalArgumentException(String.valueOf(trailType));
			}
			else {
				trailTypes.add(trailType);
			}
			return EnumSet.copyOf(trailTypes);
		}

		public boolean isRangePlot() {
			return this.suiteSize == RANGE_PLOT_SIZE;
		}

		public boolean isBoxPlot() {
			return this.suiteSize == BOX_PLOT_SIZE;
		}

		public boolean isSuite() {
			return this.suiteSize > 1;
		}
	};

	private final TrailRecordSet	parentTrail;
	private final MeasurementType	measurementType;							// measurement / settlement / scoregroup are options
	private final SettlementType	settlementType;								// measurement / settlement / scoregroup are options
	private final ScoregroupType	scoregroupType;								// measurement / settlement / scoregroup are options
	private int										trailTextSelectedIndex	= -1;	// user selection from applicable trails, is saved in the graphics template
	private List<String>					applicableTrailsTexts;				// the user may select one of these entries
	private List<Integer>					applicableTrailsOrdinals;			// maps all applicable trails in order to convert the user selection into a valid trail
	private TrailRecord[]					trailRecordSuite;							// holds data points in case of trail suites

	/**
	 * creates a vector for a measurementType to hold data points.
	 * @param newDevice
	 * @param newOrdinal
	 * @param measurementType
	 * @param TrailType
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
	 * @param settlementType
	 * @param parentTrail
	 */
	public TrailRecord(IDevice newDevice, int newOrdinal, String newName, ScoregroupType scoregroupType, TrailRecordSet parentTrail, int initialCapacity) {
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
		throw new UnsupportedOperationException("clone");
	}

	@Override
	@Deprecated
	public Record clone(String newName) {
		throw new UnsupportedOperationException("clone");
	}

	@Override
	@Deprecated
	public Record clone(int dataIndex, boolean isFromBegin) {
		throw new UnsupportedOperationException("clone");
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
		throw new UnsupportedOperationException(" " + index + " " + point);
	}

	/**
	 * take those aggregated values from the histo record which are assigned to the selected trail type.
	 * builds and fills the additional trail records in case this record is a trail suite record.
	 * @param histoRecordSet
	 */
	public void addHistoSetPoints(HistoRecordSet histoRecordSet) {
		if (applicableTrailsOrdinals.size() != applicableTrailsTexts.size()) {
			System.out.println(String.format("%,3d %,3d %,3d", applicableTrailsOrdinals.size(), TrailType.getPrimitives().size(), applicableTrailsTexts.size()));
		}
		Integer[] trailTypePoints;
		if (this.measurementType != null) {
			trailTypePoints = new Integer[TrailType.getPrimitives().size()];
			Record record = histoRecordSet.get(this.measurementType.getName());
			StatisticsType measurementStatistics = getStatistics();
			if (measurementStatistics != null && record.hasReasonableData()) {
				int triggerRefOrdinal = getTriggerReferenceOrdinal(histoRecordSet);
				boolean isTriggerLevel = measurementStatistics.getTrigger() != null;
				boolean isGpsCoordinates = this.device.isGPSCoordinates(record);
				if (measurementStatistics.isAvg()) {
					if (isTriggerLevel)
						trailTypePoints[TrailType.REAL_AVG.ordinal()] = record.getAvgValueTriggered();
					else if (isGpsCoordinates)
						trailTypePoints[TrailType.REAL_AVG.ordinal()] = record.getAvgValue();
					else
						trailTypePoints[TrailType.REAL_AVG.ordinal()] = triggerRefOrdinal < 0 ? record.getAvgValue() : (Integer) record.getAvgValueTriggered(triggerRefOrdinal);
				}
				if (measurementStatistics.isCountByTrigger() != null) {
					trailTypePoints[TrailType.REAL_COUNT_TRIGGERED.ordinal()] = record.getTriggerRanges() != null ? record.getTriggerRanges().size() : 0;
				}
				if (measurementStatistics.isMin()) {
					if (isTriggerLevel)
						trailTypePoints[TrailType.REAL_MIN.ordinal()] = record.getMinValueTriggered();
					else if (triggerRefOrdinal < 0 || record.getMinValueTriggered(triggerRefOrdinal) != Integer.MAX_VALUE) if (isGpsCoordinates)
						trailTypePoints[TrailType.REAL_MIN.ordinal()] = record.getRealMinValue();
					else
						trailTypePoints[TrailType.REAL_MIN.ordinal()] = triggerRefOrdinal < 0 ? record.getRealMinValue() : (Integer) record.getMinValueTriggered(triggerRefOrdinal);
				}
				if (measurementStatistics.isMax()) {
					if (isTriggerLevel)
						trailTypePoints[TrailType.REAL_MAX.ordinal()] = record.getMaxValueTriggered();
					else if (triggerRefOrdinal < 0 || record.getMaxValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE) if (isGpsCoordinates)
						trailTypePoints[TrailType.REAL_MAX.ordinal()] = record.getRealMaxValue();
					else
						trailTypePoints[TrailType.REAL_MAX.ordinal()] = (triggerRefOrdinal < 0 ? record.getRealMaxValue() : (Integer) record.getMaxValueTriggered(triggerRefOrdinal));
				}
				if (measurementStatistics.isSigma()) {
					if (isTriggerLevel)
						trailTypePoints[TrailType.REAL_SD.ordinal()] = record.getSigmaValueTriggered();
					else
						trailTypePoints[TrailType.REAL_SD.ordinal()] = (triggerRefOrdinal < 0 ? (Integer) record.getSigmaValue() : (Integer) record.getSigmaValueTriggered(triggerRefOrdinal));
				}
				if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
					if (measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1)
						trailTypePoints[TrailType.REAL_SUM_TRIGGERED.ordinal()] = record.getSumTriggeredRange();
					else if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
						trailTypePoints[TrailType.REAL_SUM_TRIGGERED.ordinal()] = record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal());
					}
					if (measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1 && measurementStatistics.getRatioRefOrdinal() != null) {
						Record referencedRecord = histoRecordSet.get(measurementStatistics.getRatioRefOrdinal().intValue());
						StatisticsType referencedStatistics = this.device.getMeasurementStatistic(this.parent.getChannelConfigNumber(), measurementStatistics.getRatioRefOrdinal());
						if (referencedRecord != null && (referencedStatistics.isAvg() || referencedStatistics.isMax()))
							// todo trigger ratio is multiplied by 1000 (per mille)
							if (referencedStatistics.isAvg())
							trailTypePoints[TrailType.REAL_AVG_RATIO_TRIGGERED.ordinal()] = (int) Math.round(referencedRecord.getAvgValue() * 1000.0 / record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal()));
							else if (referencedStatistics.isMax()) trailTypePoints[TrailType.REAL_MAX_RATIO_TRIGGERED.ordinal()] = (int) Math.round(referencedRecord.getMaxValue() * 1000.0 / record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal()));
					}
				}
				if (measurementStatistics.getTrigger() != null && measurementStatistics.getSumTriggerTimeText() != null && measurementStatistics.getSumTriggerTimeText().length() > 1) {
					trailTypePoints[TrailType.REAL_TIME_SUM_TRIGGERED.ordinal()] = record.getTimeSumTriggeredRange_ms();
				}
			}
			trailTypePoints[TrailType.REAL_FIRST.ordinal()] = record.get(0);
			trailTypePoints[TrailType.REAL_LAST.ordinal()] = record.get(record.size() - 1);
			if (settings.isQuantilesActive()) {
				Quantile quantile = new Quantile(record, histoRecordSet.isSampled() ? EnumSet.of(Fixings.IS_SAMPLE) : EnumSet.noneOf(Fixings.class));
				trailTypePoints[TrailType.Q0.ordinal()] = (int) quantile.getQuartile0();
				trailTypePoints[TrailType.Q1.ordinal()] = (int) quantile.getQuartile1();
				trailTypePoints[TrailType.Q2.ordinal()] = (int) quantile.getQuartile2();
				trailTypePoints[TrailType.Q3.ordinal()] = (int) quantile.getQuartile3();
				trailTypePoints[TrailType.Q4.ordinal()] = (int) quantile.getQuartile4();
				trailTypePoints[TrailType.Q_25_PERMILLE.ordinal()] = (int) quantile.getQuantile(.025);
				trailTypePoints[TrailType.Q_975_PERMILLE.ordinal()] = (int) quantile.getQuantile(.975);
				trailTypePoints[TrailType.Q_LOWER_WHISKER.ordinal()] = (int) quantile.getQuantileLowerWhisker();
				trailTypePoints[TrailType.Q_UPPER_WHISKER.ordinal()] = (int) quantile.getQuantileUpperWhisker();
			}
		}
		else if (this.settlementType != null) {
			trailTypePoints = new Integer[TrailType.getPrimitives().size()];
			if (this.settlementType.getEvaluation() != null) { // evaluation and scores are choices
				EvaluationType settlementEvaluations = this.settlementType.getEvaluation();
				HistoSettlement record = histoRecordSet.getSettlement(this.settlementType.getName());
				if (record.hasReasonableData()) {
					if (settlementEvaluations.isAvg()) trailTypePoints[TrailType.REAL_AVG.ordinal()] = record.getAvgValue();
					if (settlementEvaluations.isFirst()) trailTypePoints[TrailType.REAL_FIRST.ordinal()] = record.get(0);
					if (settlementEvaluations.isLast()) trailTypePoints[TrailType.REAL_LAST.ordinal()] = record.get(record.size() - 1);
					if (settlementEvaluations.isMin()) trailTypePoints[TrailType.REAL_MIN.ordinal()] = record.getMinValue();
					if (settlementEvaluations.isMax()) trailTypePoints[TrailType.REAL_MAX.ordinal()] = record.getMaxValue();
					if (settlementEvaluations.isSigma()) trailTypePoints[TrailType.REAL_SD.ordinal()] = record.getSigmaValue();
					if (settlementEvaluations.isSum()) trailTypePoints[TrailType.REAL_SUM.ordinal()] = record.getSumValue();
				}
			}
		}
		else if (this.scoregroupType != null) {
			trailTypePoints = new Integer[this.scoregroupType.getScore().size()];
			for (int i = 0; i < this.scoregroupType.getScore().size(); i++) {
				ScoreType scoreType = this.scoregroupType.getScore().get(i);
				trailTypePoints[i] = null;
			}
		}
		else {
			trailTypePoints = null; // never reached: only measurements, settlements and scores are allowed.
		}

		if (this.trailRecordSuite == null) {
			super.add(null);
		}
		else {
			if (this.trailRecordSuite.length == 1) {
				super.add(trailTypePoints[this.getTrailOrdinal()]);
			}
			else { // min/max depends on all values of the suite
				int minValue = Integer.MAX_VALUE, maxValue = Integer.MIN_VALUE;
				int masterPoint = 0; // this is the basis value for adding or subtracting standard deviations
				boolean summationSign = false; // false means subtract, true means add
				for (int i = 0; i < this.trailRecordSuite.length; i++) {
					TrailRecord trailRecord = this.trailRecordSuite[i];
					Integer point = trailTypePoints[trailRecord.getTrailOrdinal()];
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
						minValue = Math.min(minValue, trailRecord.getRealMinValue());
						maxValue = Math.max(maxValue, trailRecord.getRealMaxValue());
					}
					else {
						trailRecord.add(point);
					}
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, trailRecord.getName() + " data " + Arrays.toString(trailTypePoints)); //$NON-NLS-1$
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, trailRecord.getName() + " trail " + trailRecord.toString()); //$NON-NLS-1$
				}
				if (minValue != Integer.MAX_VALUE && maxValue != Integer.MIN_VALUE) {
					this.setMinMax(minValue, maxValue);
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "setMinMax :  " + minValue + "," + maxValue); //$NON-NLS-1$
				}
			}
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.getName() + " data " + Arrays.toString(trailTypePoints)); //$NON-NLS-1$
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, this.getName() + " trail " + this.toString()); //$NON-NLS-1$
	}

	/**
	 * take those data points from the histo vault which are assigned to the selected trail type.
	 * supports trail suites.
	 * @param histoVault
	 */
	public void add(HistoVault histoVault) {
		if (applicableTrailsOrdinals.size() != applicableTrailsTexts.size()) {
			throw new UnsupportedOperationException(String.format("%,3d %,3d %,3d", applicableTrailsOrdinals.size(), TrailType.getPrimitives().size(), applicableTrailsTexts.size()));
		}
		if (this.trailRecordSuite == null) { // ???
			super.add(null);
		}
		else {
			if (this.trailRecordSuite.length == 1) {
				if (this.isMeasurement())
					super.add(histoVault.getMeasurements(this.ordinal)[this.getTrailOrdinal()]);
				else if (this.isSettlement())
					super.add(histoVault.getSettlements(this.settlementType.getSettlementId())[this.getTrailOrdinal()]);
				else if (this.isScoregroup())
					super.add(histoVault.getScores()[this.getTrailOrdinal()]);
				else
					throw new UnsupportedOperationException("length == 1");
			}
			else {
				int minValue = Integer.MAX_VALUE, maxValue = Integer.MIN_VALUE; // min/max depends on all values of the suite
				int masterPoint = 0; // this is the basis value for adding or subtracting standard deviations
				boolean summationSign = false; // false means subtract, true means add
				for (int i = 0; i < this.trailRecordSuite.length; i++) {
					TrailRecord trailRecord = this.trailRecordSuite[i];
					Integer point;
					if (this.isMeasurement())
						point = histoVault.getMeasurements(this.ordinal)[this.getTrailOrdinal()];
					else if (this.isSettlement())
						point = histoVault.getSettlements(this.settlementType.getSettlementId())[this.getTrailOrdinal()];
					else if (this.isScoregroup())
						point = histoVault.getScores()[this.getTrailOrdinal()];
					else
						throw new UnsupportedOperationException("length > 1");

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
						minValue = Math.min(minValue, trailRecord.getRealMinValue());
						maxValue = Math.max(maxValue, trailRecord.getRealMaxValue());
					}
					else {
						trailRecord.add(point);
					}
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, trailRecord.getName() + " trail " + trailRecord.toString()); //$NON-NLS-1$
				}
				if (minValue != Integer.MAX_VALUE && maxValue != Integer.MIN_VALUE) {
					this.setMinMax(minValue, maxValue);
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "setMinMax :  " + minValue + "," + maxValue); //$NON-NLS-1$
				}
			}
		}
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, this.getName() + " trail " + this.toString()); //$NON-NLS-1$
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
				points[i++] = new Point(xDisplayOffset + xPos,
						yDisplayOffset - Double.valueOf((((grad + ((super.realRealGet(i) / 1000000.0 - grad) / 0.60)) * 1000.0) - offset) * super.displayScaleFactorValue).intValue());
			}
		}
		if (log.isLoggable(Level.SEVERE)) log.log(Level.SEVERE, "yPos = " + Arrays.toString(points));
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
		dataTableRow[0] = this.getUnit().length() > 0 ? this.getName() + GDE.STRING_BLANK_LEFT_BRACKET + this.getUnit() + GDE.STRING_RIGHT_BRACKET : this.getName();
		dataTableRow[1] = this.getTrailText();
		double factor = getFactor();
		double offset = getOffset();
		double reduction = getReduction();
		if (this.getTrailRecordSuite().length == 1) { // standard curve
			for (int i = 0; i < masterRecord.size(); i++) {
				if (masterRecord.realRealGet(i) != null) {
					dataTableRow[i + 2] = this.getDecimalFormat().format((masterRecord.get(i) / 1000. - reduction) * factor + offset);
				}
			}
		}
		else {
			if (this.getTrailRecordSuite().length > 6) { // boxplot
				final TrailRecord lowerWhiskerRecord = this.getTrailRecordSuite()[5];
				final TrailRecord medianRecord = this.getTrailRecordSuite()[2];
				final TrailRecord upperWhiskerRecord = this.getTrailRecordSuite()[6];
				for (int i = 0; i < masterRecord.size(); i++) {
					if (masterRecord.realRealGet(i) != null) {
						StringBuilder sb = new StringBuilder();
						sb.append(this.getDecimalFormat().format((lowerWhiskerRecord.get(i) / 1000. - reduction) * factor + offset));
						sb.append(GDE.STRING_BLANK_COLON_BLANK).append(this.getDecimalFormat().format((medianRecord.get(i) / 1000. - reduction) * factor + offset));
						sb.append(GDE.STRING_BLANK_COLON_BLANK).append(this.getDecimalFormat().format((upperWhiskerRecord.get(i) / 1000. - reduction) * factor + offset));
						dataTableRow[i + 2] = sb.toString();
					}
				}
			}
			else if (this.getTrailRecordSuite().length > 2) { // envelope		
				final TrailRecord lowerRecord = this.getTrailRecordSuite()[1];
				final TrailRecord middleRecord = this.getTrailRecordSuite()[0];
				final TrailRecord upperRecord = this.getTrailRecordSuite()[2];
				for (int i = 0; i < masterRecord.size(); i++) {
					if (masterRecord.realRealGet(i) != null) {
						StringBuilder sb = new StringBuilder();
						sb.append(this.getDecimalFormat().format((lowerRecord.get(i) / 1000. - reduction) * factor + offset));
						sb.append(GDE.STRING_BLANK_COLON_BLANK).append(this.getDecimalFormat().format((middleRecord.get(i) / 1000. - reduction) * factor + offset));
						sb.append(GDE.STRING_BLANK_COLON_BLANK).append(this.getDecimalFormat().format((upperRecord.get(i) / 1000. - reduction) * factor + offset));
						dataTableRow[i + 2] = sb.toString();
					}
				}
			}
		}
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
		this.applicableTrailsTexts.add(TrailType.fromOrdinal(trailOrdinal).displayName);
		this.trailTextSelectedIndex = 0;
	}

	/**
	 * analyze device configuration entries to find applicable trail types.
	 * build applicable trail type lists for display purposes.
	 * a trail type suite is applicable if all type items of this suite are applicable.
	 * use device settings trigger texts for trigger trail types and score labels for score trail types; message texts otherwise.
	 */
	public void setApplicableTrailTypes() {
		boolean[] applicablePrimitiveTrails;
		// step 1: analyze device entries to find applicable primitive trail types
		if (this.measurementType != null) {
			applicablePrimitiveTrails = new boolean[TrailType.getPrimitives().size()];
			StatisticsType measurementStatistics = getStatistics();
			if (measurementStatistics != null) {
				applicablePrimitiveTrails[TrailType.REAL_AVG.ordinal()] = measurementStatistics.isAvg();
				applicablePrimitiveTrails[TrailType.REAL_COUNT_TRIGGERED.ordinal()] = (measurementStatistics.isCountByTrigger() != null);
				applicablePrimitiveTrails[TrailType.REAL_MIN.ordinal()] = measurementStatistics.isMin();
				applicablePrimitiveTrails[TrailType.REAL_MAX.ordinal()] = measurementStatistics.isMax();
				applicablePrimitiveTrails[TrailType.REAL_SD.ordinal()] = measurementStatistics.isSigma();
				if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
					applicablePrimitiveTrails[TrailType.REAL_SUM_TRIGGERED.ordinal()] = (measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1);
					if (measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1 && measurementStatistics.getRatioRefOrdinal() != null) {
						StatisticsType referencedStatistics = this.device.getMeasurementStatistic(this.parent.getChannelConfigNumber(), measurementStatistics.getRatioRefOrdinal());
						applicablePrimitiveTrails[TrailType.REAL_AVG_RATIO_TRIGGERED.ordinal()] = referencedStatistics.isAvg();
						applicablePrimitiveTrails[TrailType.REAL_MAX_RATIO_TRIGGERED.ordinal()] = referencedStatistics.isMax();
					}
				}
				applicablePrimitiveTrails[TrailType.REAL_TIME_SUM_TRIGGERED.ordinal()] = (measurementStatistics.getTrigger() != null && measurementStatistics.getSumTriggerTimeText() != null
						&& measurementStatistics.getSumTriggerTimeText().length() > 1);
			}
			applicablePrimitiveTrails[TrailType.REAL_FIRST.ordinal()] = false; // in settlements only
			applicablePrimitiveTrails[TrailType.REAL_LAST.ordinal()] = false; // in settlements only
			applicablePrimitiveTrails[TrailType.REAL_SUM.ordinal()] = false; // in settlements only
			if (settings.isQuantilesActive()) {
				//				applicablePrimitiveTrails[TrailType.AVG.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.Q0.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.Q1.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.Q2.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.Q3.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.Q4.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.Q_25_PERMILLE.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.Q_975_PERMILLE.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.Q_LOWER_WHISKER.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.Q_UPPER_WHISKER.ordinal()] = true;
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.getName() + " " + measurementStatistics.toString()); //$NON-NLS-1$
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.getName() + " data " + Arrays.toString(applicablePrimitiveTrails)); //$NON-NLS-1$
		}
		else if (this.settlementType != null) {
			applicablePrimitiveTrails = new boolean[TrailType.getPrimitives().size()];
			EvaluationType settlementEvaluation = this.settlementType.getEvaluation();
			if (settlementEvaluation != null) {
				applicablePrimitiveTrails[TrailType.REAL_AVG.ordinal()] = settlementEvaluation.isAvg();
				applicablePrimitiveTrails[TrailType.REAL_MIN.ordinal()] = settlementEvaluation.isMin();
				applicablePrimitiveTrails[TrailType.REAL_MAX.ordinal()] = settlementEvaluation.isMax();
				applicablePrimitiveTrails[TrailType.REAL_SD.ordinal()] = settlementEvaluation.isSigma();

				applicablePrimitiveTrails[TrailType.REAL_SUM.ordinal()] = settlementEvaluation.isSum();
				applicablePrimitiveTrails[TrailType.REAL_FIRST.ordinal()] = settlementEvaluation.isFirst();
				applicablePrimitiveTrails[TrailType.REAL_LAST.ordinal()] = settlementEvaluation.isLast();
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.getName() + " " + settlementEvaluation.toString()); // $NON-NLS-1$
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.getName() + " data " + Arrays.toString(applicablePrimitiveTrails)); //$NON-NLS-1$
			}
		}
		else if (this.scoregroupType != null) {
			applicablePrimitiveTrails = new boolean[0]; // not required
		}
		else {
			throw new UnsupportedOperationException(" >>> no trails found <<< ");
		}

		// step 2: build applicable trail type lists for display purposes
		{
			this.applicableTrailsOrdinals = new ArrayList<>();
			this.applicableTrailsTexts = new ArrayList<>();
			if (this.scoregroupType != null) {
				for (int i = 0; i < this.scoregroupType.getScore().size(); i++) {
					this.applicableTrailsOrdinals.add(this.scoregroupType.getScore().get(i).getLabel().ordinal());
					this.applicableTrailsTexts.add(this.scoregroupType.getScore().get(i).getValue());
				}
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.getName() + " score "); //$NON-NLS-1$
			}
			else {
				// step 2a: find primitive trail types which are applicable for display
				for (int i = 0; i < applicablePrimitiveTrails.length; i++) {
					if (applicablePrimitiveTrails[i]) {
						this.applicableTrailsOrdinals.add(i);
						TrailType trailType = TrailType.values[i];
						if (trailType.isTriggered) {
							if (trailType.equals(TrailType.REAL_COUNT_TRIGGERED)) {
								this.applicableTrailsTexts.add(getStatistics().getCountTriggerText());
							}
							else if (trailType.equals(TrailType.REAL_SUM_TRIGGERED)) {
								this.applicableTrailsTexts.add(getStatistics().getSumTriggerText());
							}
							else if (trailType.equals(TrailType.REAL_TIME_SUM_TRIGGERED)) {
								this.applicableTrailsTexts.add(getStatistics().getSumTriggerTimeText());
							}
							else if (trailType.equals(TrailType.REAL_AVG_RATIO_TRIGGERED)) {
								this.applicableTrailsTexts.add(getStatistics().getRatioText());
							}
							else if (trailType.equals(TrailType.REAL_MAX_RATIO_TRIGGERED)) {
								this.applicableTrailsTexts.add(getStatistics().getRatioText());
							}
							else
								throw new UnsupportedOperationException("trailType.isTriggered");
						}
						else {
							this.applicableTrailsTexts.add(trailType.displayName);
						}
					}
				}
				// step 2b: decide about set trail types which are applicable for display
				for (TrailType setTrailType : TrailType.getSuites()) {
					boolean bb = true;
					for (TrailType trailType : TrailType.getSuite(setTrailType)) {
						bb &= applicablePrimitiveTrails[trailType.ordinal()];
					}
					if (bb) {
						this.applicableTrailsOrdinals.add(setTrailType.ordinal());
						this.applicableTrailsTexts.add(setTrailType.displayName);
					}
				}
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, super.getName() + " texts " + this.applicableTrailsTexts);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, super.getName() + " ordinals " + this.applicableTrailsOrdinals);
		}
	}

	/**
	 * get the trigger reference ordinal value without checking if any referenced record is in state displayable.
	 * @param measurementStatistics
	 * @return -1 if referenced record does not fulfill the criteria required, else the ordinal of the referenced record
	 */
	private int getTriggerReferenceOrdinal(StatisticsType measurementStatistics) {
		return measurementStatistics.getTriggerRefOrdinal() == null ? -1 : measurementStatistics.getTriggerRefOrdinal().intValue();
	}

	/**
	 * get the trigger reference ordinal value while checking the referenced record is in state displayable
	 * @param recordSet
	 * @param measurementStatistics
	 * @return -1 if referenced record does not fulfill the criteria required, else the ordinal of the referenced record
	 */
	private int getTriggerReferenceOrdinal(RecordSet recordSet) {
		int triggerRefOrdinal = -1;
		if (getStatistics().getTriggerRefOrdinal() != null && recordSet != null) {
			int tmpOrdinal = getStatistics().getTriggerRefOrdinal().intValue();
			Record record = recordSet.get(tmpOrdinal);
			if (record != null && record.isDisplayable()) {
				triggerRefOrdinal = tmpOrdinal;
			}
		}
		return triggerRefOrdinal;
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
				EnumSet<TrailType> suite = TrailType.getSuite(TrailType.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex)));
				this.trailRecordSuite = new TrailRecord[suite.size()];
				int i = 0;
				for (TrailType trailType : suite) {
					if (this.measurementType != null) {
						this.trailRecordSuite[i] = new TrailRecord(this.device, trailType.ordinal() + 1000, "suite" + trailType.ordinal(), this.measurementType, this.parentTrail, this.size());
						this.trailRecordSuite[i].setApplicableTrailTypes(trailType.ordinal());
					}
					else if (this.settlementType != null) {
						this.trailRecordSuite[i] = new TrailRecord(this.device, trailType.ordinal() + 1000, "suite" + trailType.ordinal(), this.settlementType, this.parentTrail, this.size());
						this.trailRecordSuite[i].setApplicableTrailTypes(trailType.ordinal());
					}
					else if (this.scoregroupType != null) {
						this.trailRecordSuite[i] = new TrailRecord(this.device, trailType.ordinal() + 1000, "suite" + trailType.ordinal(), this.scoregroupType, this.parentTrail, this.size());
						this.trailRecordSuite[i].setApplicableTrailTypes(trailType.ordinal());
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
		return this.scoregroupType == null ? TrailType.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex)).isSuite() : false;
	}

	public boolean isRangePlotSuite() {
		return this.scoregroupType == null ? TrailType.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex)).isRangePlot() : false;
	}

	public boolean isBoxPlotSuite() {
		return this.scoregroupType == null ? TrailType.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex)).isBoxPlot() : false;
	}

	public boolean isSuiteForSummation() {
		return this.scoregroupType == null ? TrailType.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex)).isForSummation : false;
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

	public ScoregroupType getScoregroup() {
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
				int tmpDisplaySequence = (TrailType.fromOrdinal(this.applicableTrailsOrdinals.get(i))).displaySequence;
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

	// todo a bunch of base class methods is not applicable for this class (e.g. trigger): Use common base class for Trail and Record or remove inheritance from Trail and copy code from Record.

}
