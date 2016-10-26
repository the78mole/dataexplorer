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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
                  2016 Thomas Eickert
****************************************************************************************/

package gde.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.swt.graphics.Point;

import gde.GDE;
import gde.device.DeviceConfiguration;
import gde.device.EvaluationType;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.device.PropertyType.ScoreType;
import gde.device.SettlementType;
import gde.device.StatisticsType;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.HistoTimeLine;

/**
 * holds histo data points of one measurement or settlement.
 * a histo data point holds one aggregated value (e.g. max, avg, quantile; or any score value).
 * supports multiple curves (trail suites).
 * @author Thomas Eickert
 */
public class TrailRecord extends Record { // TODO maybe a better option is to create a common base class for Record, HistoSettlement and TrailRecord.
	final static String						$CLASS_NAME					= TrailRecord.class.getName();
	final static long							serialVersionUID		= 110124007964748556L;
	final static Logger						log									= Logger.getLogger($CLASS_NAME);

	public final static String		TRAIL_TEXT_ORDINAL	= "_trailTextOrdinal";								// reference to the selected trail //$NON-NLS-1$
	public final static String[]	trailPropertyKeys		= new String[] { TRAIL_TEXT_ORDINAL };

	public enum TrailType {
		REAL_AVG(0, false, false, false, Messages.getString(MessageIds.GDE_MSGT0750)), // average
		REAL_COUNT_OBS(11, false, false, false, Messages.getString(MessageIds.GDE_MSGT0751)), // counter
		REAL_FIRST(3, false, false, false, Messages.getString(MessageIds.GDE_MSGT0752)), //
		REAL_LAST(4, false, false, false, Messages.getString(MessageIds.GDE_MSGT0753)), //
		REAL_MIN(2, false, false, false, Messages.getString(MessageIds.GDE_MSGT0754)), //
		REAL_MAX(1, false, false, false, Messages.getString(MessageIds.GDE_MSGT0755)), //
		REAL_SD(5, true, false, false, Messages.getString(MessageIds.GDE_MSGT0756)), //
		REAL_COUNT_TRIGGERED(10, false, false, true, Messages.getString(MessageIds.GDE_MSGT0757)), //
		REAL_SUM_TRIGGERED(6, false, false, true, Messages.getString(MessageIds.GDE_MSGT0758)), //
		REAL_TIME_SUM_TRIGGERED(7, false, false, true, Messages.getString(MessageIds.GDE_MSGT0759)), //
		REAL_AVG_RATIO_TRIGGERED(8, false, false, true, Messages.getString(MessageIds.GDE_MSGT0760)), //
		REAL_MAX_RATIO_TRIGGERED(9, false, false, true, Messages.getString(MessageIds.GDE_MSGT0761)), //
		REAL_SUM(12, false, false, false, Messages.getString(MessageIds.GDE_MSGT0762)), // TODO messageID
		SCORE(13, false, false, false, Messages.getString(MessageIds.GDE_MSGT0763)), // TODO messageID
		// REAL_TIMESPAN(12, false, false, Messages.getString(MessageIds.GDE_MSGT0762)), // MaxTime - MinTime
		// REAL_TIME_STEP_AVG(13, false, false, Messages.getString(MessageIds.GDE_MSGT0763)), //
		AVG(14, false, false, false, Messages.getString(MessageIds.GDE_MSGT0764)), //
		SUM(15, false, false, false, Messages.getString(MessageIds.GDE_MSGT0765)), //
		COUNT_OBS(16, false, false, false, Messages.getString(MessageIds.GDE_MSGT0766)), // TODO messageID
		Q0(17, false, false, false, Messages.getString(MessageIds.GDE_MSGT0767)), // quantile 0 is q(0%) which is the minimum
		Q1(18, false, false, false, Messages.getString(MessageIds.GDE_MSGT0768)), // quantile 1 is q(25%)
		Q2(19, false, false, false, Messages.getString(MessageIds.GDE_MSGT0769)), // quantile 2 is q(50%) which is the median
		Q3(20, false, false, false, Messages.getString(MessageIds.GDE_MSGT0770)), // quantile 3 is q(75%)
		Q4(21, false, false, false, Messages.getString(MessageIds.GDE_MSGT0771)), // quantile 4 is q(100%) which is the maximum
		O1(22, false, false, false, Messages.getString(MessageIds.GDE_MSGT0772)), // octile 1 is q(12,5%)
		O7(23, false, false, false, Messages.getString(MessageIds.GDE_MSGT0773)), // octile 7 is q(87,5%)
		SD(24, true, false, false, Messages.getString(MessageIds.GDE_MSGT0774)), //
		SUITE_REAL_AVG_SD(1000, false, true, false, Messages.getString(MessageIds.GDE_MSGT0781)), //
		SUITE_REAL_AVG_MIN_MAX(1001, false, true, false, Messages.getString(MessageIds.GDE_MSGT0782)), //
		SUITE_AVG_SD(1002, false, true, false, Messages.getString(MessageIds.GDE_MSGT0783)), //
		SUITE_AVG_MIN_MAX(1003, false, true, false, Messages.getString(MessageIds.GDE_MSGT0784)), //
		SUITE_BOX_PLOT(1004, false, true, false, Messages.getString(MessageIds.GDE_MSGT0785)), //
		SUITE_Q0_Q2_Q4(1005, false, true, false, Messages.getString(MessageIds.GDE_MSGT0786)), //
		SUITE_Q1_Q2_Q3(1006, false, true, false, Messages.getString(MessageIds.GDE_MSGT0787));//

		private final int							displaySequence;
		private final boolean					isForSummation;
		private final boolean					isSuite;
		private final boolean					isTriggered;
		private final String					displayName;
		public static final TrailType	values[]	= values();	// use this to avoid cloning if calling values()

		private TrailType(int displaySequence, boolean isForSummation, boolean isSuite, boolean isTriggered, String displayName) {
			this.displaySequence = displaySequence;
			this.isForSummation = isForSummation;
			this.isSuite = isSuite;
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

		public static List<TrailType> getAsList() {
			return new ArrayList<TrailType>(Arrays.asList(TrailType.values));
		}

		public static List<TrailType> getPrimitivesAsList() {
			List<TrailType> trailTypes = new ArrayList<TrailType>();
			for (TrailType type : TrailType.values) {
				if (!type.isSuite) trailTypes.add(type);
			}
			return trailTypes;
		}

		private static List<TrailType> getSuitesAsList() {
			List<TrailType> trailTypes = new ArrayList<TrailType>();
			for (TrailType type : TrailType.values) {
				if (type.isSuite) trailTypes.add(type);
			}
			return trailTypes;
		}

		public static List<TrailType> getSuite(TrailType trailType) {
			List<TrailType> trailTypes = new ArrayList<TrailType>();
			if (trailType.isSuite) {
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
				else if (trailType.equals(SUITE_AVG_SD)) {
					trailTypes.add(AVG); // master record for adding sd must be in front of the sd records
					trailTypes.add(SD); // avg - n times sd
					trailTypes.add(SD); // avg + n times sd
				}
				else if (trailType.equals(SUITE_AVG_MIN_MAX)) {
					trailTypes.add(AVG);
					trailTypes.add(Q0);
					trailTypes.add(Q4);
				}
				else if (trailType.equals(SUITE_BOX_PLOT)) {
					trailTypes.add(AVG);
					trailTypes.add(Q0);
					trailTypes.add(Q1);
					trailTypes.add(Q2);
					trailTypes.add(Q3);
					trailTypes.add(Q4);
					trailTypes.add(O1);
					trailTypes.add(O7);
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
			return trailTypes;
		}

		private static List<String> getNamesAsList() {
			List<String> trailTypes = new ArrayList<String>();
			for (TrailType type : TrailType.values()) {
				trailTypes.add(type.name());
			}
			return trailTypes;
		}

		private static String[] getNames() {
			List<String> trailTypes = getNamesAsList();
			return trailTypes.toArray(new String[trailTypes.size()]);
		}

		public boolean isSuite() {
			return this.isSuite;
		}
	};

	private final TrailRecordSet	parentTrail;
	private MeasurementType				measurementType;							// measurement / settlement are options
	private SettlementType				settlementType;								// measurement / settlement are options
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
		super(newDevice, newOrdinal, measurementType.getName(), measurementType.getSymbol(), measurementType.getUnit(), measurementType.isActive(), null, measurementType.getProperty(), initialCapacity);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, measurementType.getName() + " TrailRecord(IDevice newDevice, int newOrdinal, MeasurementType measurementType, TrailRecordSet parentTrail)"); //$NON-NLS-1$
		this.parentTrail = parentTrail;
		super.parent = parentTrail;
		this.measurementType = measurementType;
	}

	/**
	 * creates a vector for a settlemenType to hold data points.
	 * @param newDevice
	 * @param newOrdinal
	 * @param settlementType
	 * @param parentTrail
	 */
	public TrailRecord(IDevice newDevice, int newOrdinal, String newName, SettlementType settlementType, TrailRecordSet parentTrail, int initialCapacity) {
		super(newDevice, newOrdinal, settlementType.getName(), settlementType.getSymbol(), settlementType.getUnit(), settlementType.isActive(), null, settlementType.getProperty(), initialCapacity);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, settlementType.getName() + " TrailRecord(IDevice newDevice, int newOrdinal, SettlementType settlementType, TrailRecordSet parentTrail)"); //$NON-NLS-1$
		this.parentTrail = parentTrail;
		super.parent = parentTrail;
		this.settlementType = settlementType;
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
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, Arrays.toString(points));
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
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "yPos = " + Arrays.toString(points));
		return points;
		// int grad = super.get(measurementPointIndex) / 1000000;
		// return new Point(xDisplayOffset + Double.valueOf(super.getTime_ms(measurementPointIndex) * super.displayScaleFactorTime).intValue(), yDisplayOffset
		// - Double.valueOf((((grad + ((this.get(measurementPointIndex) / 1000000.0 - grad) / 0.60)) * 1000.0) - (this.minDisplayValue * 1 / this.syncMasterFactor))
		// * this.displayScaleFactorValue).intValue());
	}

	/**
	 * add data points to the records identified by the trail type.
	 * @param points data points for all trails except set trails. the array index equals the trail type ordinal.
	 */
	@Deprecated // is now part of addHisto
	public synchronized boolean add(Integer[] points) {
		if (TrailType.getPrimitivesAsList().size() != points.length) {
			throw new UnsupportedOperationException("points.length");
		}
		return super.add(points[getTrailType().ordinal()]); // TODO multiple TrailTypes are not relevant --> access them via parentTrail
	}

	/**
	 * take those aggregated values from the histo record which are assigned to the selected trail type.
	 * builds and fills the additional trail records in case this record is a trail set record.
	 * @param histoRecordSet
	 */
	public void addHisto(HistoRecordSet histoRecordSet) { // TODO fill only those points for applicable trails
		Integer[] points = new Integer[TrailType.getPrimitivesAsList().size()];
		if (this.measurementType != null) {
			Record record = histoRecordSet.get(this.measurementType.getName());
			if (record != null) {
				StatisticsType measurementStatistics = getStatistics();
				if (measurementStatistics != null && record.hasReasonableData()) {
					int triggerRefOrdinal = getTriggerReferenceOrdinal(histoRecordSet);
					boolean isTriggerLevel = measurementStatistics.getTrigger() != null;
					boolean isGpsCoordinates = this.device.isGPSCoordinates(record);
					if (measurementStatistics.isAvg()) {
						if (isTriggerLevel)
							points[TrailType.REAL_AVG.ordinal()] = record.getAvgValueTriggered();
						else if (isGpsCoordinates)
							points[TrailType.REAL_AVG.ordinal()] = record.getAvgValue();
						else
							points[TrailType.REAL_AVG.ordinal()] = triggerRefOrdinal < 0 ? record.getAvgValue() : (Integer) record.getAvgValueTriggered(triggerRefOrdinal);
					}
					if (measurementStatistics.isCountByTrigger() != null) {
						points[TrailType.REAL_COUNT_TRIGGERED.ordinal()] = record.getTriggerRanges() != null ? record.getTriggerRanges().size() : 0;
					}
					if (measurementStatistics.isMin()) {
						if (isTriggerLevel)
							points[TrailType.REAL_MIN.ordinal()] = record.getMinValueTriggered();
						else if (triggerRefOrdinal < 0 || record.getMinValueTriggered(triggerRefOrdinal) != Integer.MAX_VALUE) if (isGpsCoordinates)
							points[TrailType.REAL_MIN.ordinal()] = record.getRealMinValue();
						else
							points[TrailType.REAL_MIN.ordinal()] = triggerRefOrdinal < 0 ? record.getRealMinValue() : (Integer) record.getMinValueTriggered(triggerRefOrdinal);
					}
					if (measurementStatistics.isMax()) {
						if (isTriggerLevel)
							points[TrailType.REAL_MAX.ordinal()] = record.getMaxValueTriggered();
						else if (triggerRefOrdinal < 0 || record.getMaxValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE) if (isGpsCoordinates)
							points[TrailType.REAL_MAX.ordinal()] = record.getRealMaxValue();
						else
							points[TrailType.REAL_MAX.ordinal()] = (triggerRefOrdinal < 0 ? record.getRealMaxValue() : (Integer) record.getMaxValueTriggered(triggerRefOrdinal));
					}
					if (measurementStatistics.isSigma()) {
						if (isTriggerLevel)
							points[TrailType.REAL_SD.ordinal()] = record.getSigmaValueTriggered();
						else
							points[TrailType.REAL_SD.ordinal()] = (triggerRefOrdinal < 0 ? (Integer) record.getSigmaValue() : (Integer) record.getSigmaValueTriggered(triggerRefOrdinal));
					}
					if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
						if (measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1)
							points[TrailType.REAL_SUM_TRIGGERED.ordinal()] = record.getSumTriggeredRange();
						else if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
							points[TrailType.REAL_SUM_TRIGGERED.ordinal()] = record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal());
						}
						if (measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1 && measurementStatistics.getRatioRefOrdinal() != null) {
							Record referencedRecord = histoRecordSet.get(measurementStatistics.getRatioRefOrdinal().intValue());
							StatisticsType referencedStatistics = this.device.getMeasurementStatistic(this.parent.getChannelConfigNumber(), measurementStatistics.getRatioRefOrdinal());
							if (referencedRecord != null && (referencedStatistics.isAvg() || referencedStatistics.isMax()))
							// TODO trigger ratio is multiplied by 1000 (per mille)
								if (referencedStatistics.isAvg())
									points[TrailType.REAL_AVG_RATIO_TRIGGERED.ordinal()] = (int) Math.round(referencedRecord.getAvgValue() * 1000.0
											/ record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal()));
								else if (referencedStatistics.isMax())
									points[TrailType.REAL_MAX_RATIO_TRIGGERED.ordinal()] = (int) Math.round(referencedRecord.getMaxValue() * 1000.0
											/ record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal()));
						}
					}
					if (measurementStatistics.getTrigger() != null && measurementStatistics.getSumTriggerTimeText() != null && measurementStatistics.getSumTriggerTimeText().length() > 1) {
						points[TrailType.REAL_TIME_SUM_TRIGGERED.ordinal()] = record.getTimeSumTriggeredRange_ms();
					}
				}
				points[TrailType.REAL_FIRST.ordinal()] = record.get(0);
				points[TrailType.REAL_LAST.ordinal()] = record.get(record.size() - 1);
				boolean calculateQuantiles = false; // TODO settings
				if (calculateQuantiles) {
					for (int i = 0; i < 1; i++) {
						long startTimeNs = System.nanoTime();
						log.log(Level.TIME, String.format("read time= %,11d ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNs))); //$NON-NLS-1$
						startTimeNs = System.nanoTime();
						ArrayList<Integer> list = new ArrayList<Integer>(record);
						Collections.sort(list);
						log.log(Level.TIME, String.format("clone+sort list time= %,11d ms  size=%d value=%d", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNs), list.size(), list.get(1111))); //$NON-NLS-1$
						startTimeNs = System.nanoTime();
						Integer[] array1 = record.toArray(new Integer[record.size()]);
						Arrays.sort(array1);
						log.log(Level.TIME, String.format("clone+sort array time= %,11d ms  size=%d value=%d", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNs), array1.length, array1[1111])); //$NON-NLS-1$
						startTimeNs = System.nanoTime();
						Vector<Integer> vector = record.clone();
						Collections.sort(vector);
						log.log(Level.TIME, String.format("clone+sort vector time= %,11d ms  size=%d value=%d", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() //$NON-NLS-1$
								- startTimeNs), vector.size(), vector.get(1111)));
					}
					// points[TrailType.Q0.ordinal()] = record.getLiveRecord().getOctileValue(0);
					// points[TrailType.Q1.ordinal()] = record.getLiveRecord().getOctileValue(2);
					// points[TrailType.Q2.ordinal()] = record.getLiveRecord().getOctileValue(4);
					// points[TrailType.Q3.ordinal()] = record.getLiveRecord().getOctileValue(6);
					// points[TrailType.Q4.ordinal()] = record.getLiveRecord().getOctileValue(8);
					// points[TrailType.O1.ordinal()] = record.getLiveRecord().getOctileValue(1);
					// points[TrailType.O7.ordinal()] = record.getLiveRecord().getOctileValue(7);
				}
			}
		}
		else {
			if (this.settlementType.getEvaluation() != null) { // evaluation and scores are choices
				EvaluationType settlementEvaluations = this.settlementType.getEvaluation();
				HistoSettlement record = histoRecordSet.getEvaluationSettlement(this.settlementType.getName());
				if (record.hasReasonableData()) {
					if (settlementEvaluations.isAvg()) points[TrailType.REAL_AVG.ordinal()] = record.getAvgValue();
					if (settlementEvaluations.isFirst()) points[TrailType.REAL_FIRST.ordinal()] = record.get(0);
					if (settlementEvaluations.isLast()) points[TrailType.REAL_LAST.ordinal()] = record.get(record.size() - 1);
					if (settlementEvaluations.isMin()) points[TrailType.REAL_MIN.ordinal()] = record.getMinValue();
					if (settlementEvaluations.isMax()) points[TrailType.REAL_MAX.ordinal()] = record.getMaxValue();
					if (settlementEvaluations.isSigma()) points[TrailType.REAL_SD.ordinal()] = record.getSigmaValue();
					if (settlementEvaluations.isSum()) points[TrailType.REAL_SUM.ordinal()] = record.getSumValue();
				}
			}
			else if (this.settlementType.getScores() != null) { // evaluation and scores are choices
				String scoreName = this.settlementType.getScores().get(this.trailTextSelectedIndex).getName();
				// values are multiplied by 1000 as this is the convention for internal values in order to avoid rounding errors for values below 1.0 (0.5 -> 0)
				if (scoreName.equals(ScoreType.REAL_SIZE.value)) {
					points[TrailType.SCORE.ordinal()] = histoRecordSet.get(0).realSize() * 1000;
				}
				else if (scoreName.equals(ScoreType.TOTAL_READINGS.value)) {
					points[TrailType.SCORE.ordinal()] = histoRecordSet.getReadingsCounter() * 1000;
				}
				else if (scoreName.equals(ScoreType.SAMPLED_READINGS.value)) {
					points[TrailType.SCORE.ordinal()] = histoRecordSet.getSampledCounter() * 1000;
				}
				else if (scoreName.equals(ScoreType.TOTAL_PACKAGES.value)) {
					points[TrailType.SCORE.ordinal()] = histoRecordSet.getPackagesCounter() * 1000;
				}
				else if (scoreName.equals(ScoreType.LOST_PACKAGES.value)) {
					points[TrailType.SCORE.ordinal()] = histoRecordSet.getPackagesLostCounter() * 1000;
				}
				else if (scoreName.equals(ScoreType.SENSORS.value)) {
					points[TrailType.SCORE.ordinal()] = histoRecordSet.getSensors().length * 1000;
				}
				else if (scoreName.equals(ScoreType.LOST_PACKAGES_PERMILLE.value)) {
					points[TrailType.SCORE.ordinal()] = histoRecordSet.getPackagesLostPerMille() != null ? histoRecordSet.getPackagesLostPerMille().intValue() * 1000 : null;
				}
				else if (scoreName.equals(ScoreType.LOST_PACKAGES_AVG_MS.value)) {
					points[TrailType.SCORE.ordinal()] = histoRecordSet.getPackagesLostAvg_ms() != null ? histoRecordSet.getPackagesLostAvg_ms() * 1000 : null;
				}
				else if (scoreName.equals(ScoreType.LOST_PACKAGES_MIN_MS.value)) {
					points[TrailType.SCORE.ordinal()] = histoRecordSet.getPackagesLostMin_ms() != null ? histoRecordSet.getPackagesLostMin_ms() * 1000 : null;
				}
				else if (scoreName.equals(ScoreType.LOST_PACKAGES_MAX_MS.value)) {
					points[TrailType.SCORE.ordinal()] = histoRecordSet.getPackagesLostMax_ms() != null ? histoRecordSet.getPackagesLostMax_ms() * 1000 : null;
				}
				else if (scoreName.equals(ScoreType.LOST_PACKAGES_SIGMA_MS.value)) {
					points[TrailType.SCORE.ordinal()] = histoRecordSet.getPackagesLostSigma_ms() != null ? histoRecordSet.getPackagesLostSigma_ms() * 1000 : null;
				}
				else if (scoreName.equals(ScoreType.AVG_TIMESTEP_MS.value)) {
					points[TrailType.SCORE.ordinal()] = (int) (histoRecordSet.getAverageTimeStep_ms() * 1000.);
				}
				else if (scoreName.equals(ScoreType.MIN_TIMESTEP_MS.value)) {
					points[TrailType.SCORE.ordinal()] = (int) (histoRecordSet.getMinimumTimeStep_ms() * 1000.);
				}
				else if (scoreName.equals(ScoreType.MAX_TIMESTEP_MS.value)) {
					points[TrailType.SCORE.ordinal()] = (int) (histoRecordSet.getMaximumTimeStep_ms() * 1000.);
				}
				else if (scoreName.equals(ScoreType.SIGMA_TIMESTEP_MS.value)) {
					points[TrailType.SCORE.ordinal()] = (int) (histoRecordSet.getSigmaTimeStep_ms() * 1000.);
				}
				else if (scoreName.equals(ScoreType.DURATION_MM.value)) {
					points[TrailType.SCORE.ordinal()] = (int) (histoRecordSet.getMaxTime_ms() / 60000. * 1000.);
				}
			}
		}
		if (this.trailRecordSuite == null) {
			super.add(null);
		} else {
			if (this.trailRecordSuite.length == 1) {
				super.add(points[getTrailType().ordinal()]);
			}
			else { // min/max depends on all values of the suite
				int minValue = Integer.MAX_VALUE, maxValue = Integer.MIN_VALUE;
				int masterPoint = 0; // this is the basis value for adding or subtracting standard deviations
				boolean summationSign = false; // false means subtract, true means add
				for (int i = 0; i < this.trailRecordSuite.length; i++) {
					TrailRecord trailRecord = this.trailRecordSuite[i];
					Integer point = points[trailRecord.getTrailType().ordinal()];
					if (point != null) { // trailRecord.getMinValue() is zero if trailRecord.size() == 0 or only nulls have been added
						if (trailRecord.getTrailType().isForSummation) {
							point = summationSign ? masterPoint + 2 * point : masterPoint - 2 * point; // TODO add multiplier in settings
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
					// if (log.isLoggable(Level.FINER))
					log.log(Level.FINE, trailRecord.getName() + " data " + Arrays.toString(points)); //$NON-NLS-1$
					// if (log.isLoggable(Level.FINEST))
					log.log(Level.FINE, trailRecord.getName() + " trail " + trailRecord.toString()); //$NON-NLS-1$
				}
				if (minValue != Integer.MAX_VALUE && maxValue != Integer.MIN_VALUE) {
					this.setMinMax(minValue, maxValue);
					// if (log.isLoggable(Level.FINER))
					log.log(Level.FINE, "setMinMax :  " + minValue + "," + maxValue); //$NON-NLS-1$
				}
			}
		}
		// if (log.isLoggable(Level.FINER))
		log.log(Level.FINE, this.getName() + " data " + Arrays.toString(points)); //$NON-NLS-1$
		// if (log.isLoggable(Level.FINEST))
		log.log(Level.FINE, this.getName() + " trail " + this.toString()); //$NON-NLS-1$
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
	 * analyze device statistics entries to find applicable trail types.
	 * build applicable trail type lists for display purposes.
	 * a type set is applicable if all type items of this suite are applicable.
	 * use device settings trigger texts for trigger trail types; message texts otherwise.
	 * @param measurementType
	 */
	public void setApplicableTrailTypes() {
		boolean[] applicablePrimitiveTrails = new boolean[TrailType.getPrimitivesAsList().size()];
		// step 1: analyze device statistics entries to find applicable primitive trail types
		if (this.measurementType != null) {
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
			applicablePrimitiveTrails[TrailType.REAL_COUNT_OBS.ordinal()] = false;
			applicablePrimitiveTrails[TrailType.COUNT_OBS.ordinal()] = false;

			applicablePrimitiveTrails[TrailType.REAL_FIRST.ordinal()] = false; // in settlements only
			applicablePrimitiveTrails[TrailType.REAL_LAST.ordinal()] = false; // in settlements only
			applicablePrimitiveTrails[TrailType.REAL_SUM.ordinal()] = false; // in settlements only
			boolean calculateQuantiles = false; // TODO settings
			if (calculateQuantiles) {
				applicablePrimitiveTrails[TrailType.AVG.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.Q0.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.Q1.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.Q2.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.Q3.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.Q4.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.O1.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.O7.ordinal()] = true;
				applicablePrimitiveTrails[TrailType.SD.ordinal()] = true;
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.getName() + " " + measurementStatistics.toString()); //$NON-NLS-1$
			// if (log.isLoggable(Level.FINER))
			log.log(Level.FINE, this.getName() + " data " + Arrays.toString(applicablePrimitiveTrails)); //$NON-NLS-1$
		}
		else {
			EvaluationType settlementEvaluation = this.settlementType.getEvaluation();
			if (settlementEvaluation != null) {
				applicablePrimitiveTrails[TrailType.REAL_AVG.ordinal()] = settlementEvaluation.isAvg();
				applicablePrimitiveTrails[TrailType.REAL_MIN.ordinal()] = settlementEvaluation.isMin();
				applicablePrimitiveTrails[TrailType.REAL_MAX.ordinal()] = settlementEvaluation.isMax();
				applicablePrimitiveTrails[TrailType.REAL_SD.ordinal()] = settlementEvaluation.isSigma();

				applicablePrimitiveTrails[TrailType.REAL_SUM.ordinal()] = settlementEvaluation.isSum();
				applicablePrimitiveTrails[TrailType.REAL_FIRST.ordinal()] = settlementEvaluation.isFirst();
				applicablePrimitiveTrails[TrailType.REAL_LAST.ordinal()] = settlementEvaluation.isLast();
				// if (log.isLoggable(Level.FINER))
				log.log(Level.FINE, this.getName() + " " + settlementEvaluation.toString()); // $NON-NLS-1$
				// if (log.isLoggable(Level.FINER))
				log.log(Level.FINE, this.getName() + " data " + Arrays.toString(applicablePrimitiveTrails)); //$NON-NLS-1$
			}
			else if (isTrailTypeScore()) {
				applicablePrimitiveTrails[TrailType.SCORE.ordinal()] = true;
				// if (log.isLoggable(Level.FINER))
				log.log(Level.FINE, this.getName() + " score "); //$NON-NLS-1$
			}
			else
				log.log(Level.FINE, this.getName() + " >>> no trails found <<< "); //$NON-NLS-1$
		}
		// step 2: build applicable trail type lists for display purposes
		{
			this.applicableTrailsOrdinals = new ArrayList<Integer>();
			this.applicableTrailsTexts = new ArrayList<String>();
			if (applicablePrimitiveTrails[TrailType.SCORE.ordinal()] == true) { // equivalent to: isTrailTypeScore()
				for (int i = 0; i < this.settlementType.getScores().size(); i++) {
					PropertyType propertyType = this.settlementType.getScores().get(i);
					this.applicableTrailsOrdinals.add(i);
					this.applicableTrailsTexts.add(propertyType.getValue());
				}
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
						else if (trailType.equals(TrailType.SCORE)) {
							for (int j = 0; j < this.settlementType.getScores().size(); j++) {
								this.applicableTrailsTexts.add(this.settlementType.getScores().get(j).getValue());
							}
						}
						else {
							this.applicableTrailsTexts.add(trailType.displayName);
						}
					}
				}
				// step 2b: decide about set trail types which are applicable for display
				for (TrailType setTrailType : TrailType.getSuitesAsList()) {
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
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, super.getName() + " texts " + this.applicableTrailsTexts);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, super.getName() + " ordinals " + this.applicableTrailsOrdinals);
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

			List<TrailType> suite = TrailType.getSuite(getTrailType());
			this.trailRecordSuite = new TrailRecord[suite.size()];
			if (suite.size() == 1) {
				this.trailRecordSuite[0] = this;
			}
			else {
				for (int i = 0; i < suite.size(); i++) {
					int trailTypeOrdinal = suite.get(i).ordinal();
					if (this.measurementType != null) {
						this.trailRecordSuite[i] = new TrailRecord(this.device, trailTypeOrdinal + 1000, "suite" + trailTypeOrdinal, this.measurementType, this.parentTrail, this.size());
						this.trailRecordSuite[i].setApplicableTrailTypes(trailTypeOrdinal);
					}
					else if (this.settlementType != null) {
						this.trailRecordSuite[i] = new TrailRecord(this.device, trailTypeOrdinal + 1000, "suite" + trailTypeOrdinal, this.settlementType, this.parentTrail, this.size());
						this.trailRecordSuite[i].setApplicableTrailTypes(trailTypeOrdinal);
					}
				}
			}
		}
	}

	public int getTrailOrdinal() {
		if (isTrailTypeScore()) {
			return TrailType.SCORE.ordinal();
		}
		else {
			return this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex);
		}
	}

	public TrailType getTrailType() {
		if (isTrailTypeScore()) {
			return TrailType.SCORE;
		}
		else {
			return TrailType.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex));
		}
	}

	public boolean isTrailTypeScore() {
		return this.measurementType == null && this.settlementType.getScores().size() > 0;
	}

	public boolean isMeasurement() {
		return this.measurementType != null ? true : false;
	}

	public boolean isSettlement() {
		return !this.isMeasurement();
	}

	public MeasurementType getMeasurement() {
		return this.measurementType;
	}

	public SettlementType getSettlement() {
		return this.settlementType;
	}

	public StatisticsType getStatistics() {
		return this.measurementType.getStatistics();
	}

	/**
	 * select the most prioritized trail from the applicable trails. 
	 * @return
	 */
	public void setMostApplicableTrailTextOrdinal() {
		if (isTrailTypeScore()) {
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
	public double getFactor() { // TODO maybe this is a better solution for the record class also (so we get rid of this override)
		double value = 1.0;
		PropertyType property = this.getProperty(IDevice.FACTOR);
		if (property != null) {
			value = Double.valueOf(property.getValue()).doubleValue();
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
	public double getOffset() { // TODO maybe this is a better solution for the record class also (so we get rid of this override)
		double value = 0.0;
		PropertyType property = this.getProperty(IDevice.OFFSET);
		if (property != null) {
			value = Double.valueOf(property.getValue()).doubleValue();
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
	public double getReduction() { // TODO maybe this is a better solution for the record class also (so we get rid of this override)
		double value = 0.0;
		PropertyType property = this.getProperty(IDevice.REDUCTION);
		if (property != null) {
			value = Double.valueOf(property.getValue()).doubleValue();
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

	public TrailRecord[] getTrailRecordSuite() {
		return this.trailRecordSuite;
	}

	// TODO a bunch of base class methods is not applicable for this class (e.g. trigger): Use common base class for Trail and Record or remove inheritance from Trail and copy code from Record.

}
