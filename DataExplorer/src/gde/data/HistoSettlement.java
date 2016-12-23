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
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Vector;
import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.device.CalculationType;
import gde.device.CalculationTypes;
import gde.device.ChannelType;
import gde.device.DataTypes;
import gde.device.DeltaBasisTypes;
import gde.device.IDevice;
import gde.device.LevelingTypes;
import gde.device.ObjectFactory;
import gde.device.PropertyType;
import gde.device.SettlementType;
import gde.device.TransitionClassTypes;
import gde.device.TransitionType;
import gde.log.Level;
import gde.utils.Quantile;
import gde.utils.Quantile.Fixings;

/**
 * holds settlement points of a line or curve calculated from measurements.
 * applicable for settlements with an evaluation rule.
 * does not support settlement display or table view.
 * similar to record class except: clone, serialization, triggers, syncWithRecords, setName, GPS-longitude, GPS-latitude.
 * @author Thomas Eickert
 */
public class HistoSettlement extends Vector<Integer> {
	final static String				$CLASS_NAME				= HistoSettlement.class.getName();
	final static long					serialVersionUID	= 6130190003229390899L;
	final static Logger				log								= Logger.getLogger($CLASS_NAME);

	protected final Settings	settings					= Settings.getInstance();

	TimeSteps									timeStep_ms				= null;														// timeStep_ms for each measurement point in compare set, where time step of measurement points might be individual
	IDevice										device;
	SettlementType						settlement;
	int												ordinal;																						// ordinal is referencing the source position of the record relative to the initial
	// device measurement configuration and used to find specific properties

	RecordSet									parent;
	String										name;																								// measurement name Höhe
	String										unit;																								// unit [m]
	String										symbol;																							// symbol h

	List<PropertyType>				properties				= new ArrayList<PropertyType>();	// offset, factor, reduction, ...
	int												maxValue					= 0;															// max value of the curve
	int												minValue					= 0;															// min value of the curve

	// statistics
	int												avgValue					= Integer.MIN_VALUE;							// average value (avg = sum(xi)/n)
	int												sigmaValue				= Integer.MIN_VALUE;							// sigma value of data
	int												sumValue					= Integer.MIN_VALUE;							// sum value of data
	int												countValue				= 0;															// counter value of measurements

	/**
	 * creates a vector to hold data points.
	 * @param newDevice
	 * @param newSettlement
	 * @param parent
	 * @param initialCapacity
	 */
	public HistoSettlement(IDevice newDevice, SettlementType newSettlement, RecordSet parent, int initialCapacity) {
		super(initialCapacity);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, newSettlement.getName() + " Settlement(IDevice , SettlementType , int )"); //$NON-NLS-1$
		this.device = newDevice;
		this.settlement = newSettlement;
		this.name = newSettlement.getName();
		this.parent = parent;
		initializeProperties(this, newSettlement.getProperty());
	}

	/**
	 * initialize properties, at least all records will have as default a factor, an offset and a reduction property
	 * @param recordRef
	 * @param newProperties
	 */
	private void initializeProperties(HistoSettlement recordRef, List<PropertyType> newProperties) {
		this.properties = this.properties != null ? this.properties : new ArrayList<PropertyType>(); // offset, factor, reduction, ...
		for (PropertyType property : newProperties) {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%20s - %s = %s", recordRef.name, property.getName(), property.getValue()));
			this.properties.add(property.clone());
		}
	}

	/**
	 * take histo transitions which are applicable and fetch trigger time steps and records from the parent.
	 * add settlement points according the evaluation rules.
	 * @param histoTransitions holds all transitions identified for the current recordset
	 * @param transitionType 
	 */
	public synchronized void addFromTransitions(HistoTransitions histoTransitions, TransitionType transitionType) { // todo transition type not required
		Integer transitionId = this.getSettlement().getEvaluation().getCalculation().getTransitionId();
		// find the channel which applies to the recordset
		ChannelType channelType = this.device.getDeviceConfiguration().getChannel(this.parent.parent.getNumber());
		this.countValue = 0;
		for (Transition transition : histoTransitions) {
			if (transition.transitionType.getTransitionId() == transitionId) {
				if (transitionType.getClassType() == TransitionClassTypes.PEAK) {
					calculateAndAdd4Peak(transition);
				}
				else {
					calculateAndAdd4Jump(transition);
				}
			}
			this.countValue++;
		}
	}

	/**
	 * add threshold values.
	 * the result value in case of ratio or count is multiplied by 1000; the timeSum value has the unit seconds with 3 decimal places. 
	 * @param transition
	 */
	private void calculateAndAdd4Jump(Transition transition) {
		CalculationType calculation = this.settlement.getEvaluation().getCalculation();
		Record record = this.parent.get(this.parent.recordNames[calculation.getRefOrdinal()]);
		int reverseTranslatedResult;
		if (calculation.getCalcType() == CalculationTypes.RATIO || calculation.getCalcType() == CalculationTypes.RATIO_INVERSE) {
			final double denominator = calculateLevelDelta(record, calculation.getLeveling(), transition);
			final Record divisorRecord = calculation.getRefOrdinalDivisor() != null ? this.parent.get(this.parent.recordNames[calculation.getRefOrdinalDivisor()]) : null;
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, divisorRecord.getName() + " divisors " //$NON-NLS-1$
					+ divisorRecord.subList(transition.referenceStartIndex, transition.recoveryEndIndex + 1));
			final double divisor = calculateLevelDelta(divisorRecord, calculation.getDivisorLeveling(), transition);
			if (calculation.getCalcType() == CalculationTypes.RATIO) {
				reverseTranslatedResult = calculation.isUnsigned() ? (int) Math.abs(1000. * denominator / divisor) : (int) (1000. * denominator / divisor); // all internal values are multiplied by 1000
			}
			else {
				reverseTranslatedResult = calculation.isUnsigned() ? (int) Math.abs(1000. * divisor / denominator) : (int) (1000. * divisor / denominator); // all internal values are multiplied by 1000
			}
		}
		else if (calculation.getCalcType() == CalculationTypes.COUNT) {
			reverseTranslatedResult = transition.thresholdSize * 1000; // all internal values are multiplied by 1000
		}
		else if (calculation.getCalcType() == CalculationTypes.TIME_SUM_SEC) {
			reverseTranslatedResult = (int) (record.getTime_ms(transition.recoveryStartIndex) - record.getTime_ms(transition.thresholdStartIndex));
		}
		else if (calculation.getCalcType() == CalculationTypes.MIN) {
			double min = Double.MAX_VALUE;
			for (int j = transition.thresholdStartIndex; j > transition.recoveryStartIndex; j++)
				if (record.realRealGet(j) != null) {
					min = Math.min(min, calculation.isUnsigned() ? Math.abs(this.device.translateValue(record, record.realRealGet(j))) : this.device.translateValue(record, record.realRealGet(j)));
				}
			reverseTranslatedResult = (int) reverseTranslateValue(min);
		}
		else if (calculation.getCalcType() == CalculationTypes.MAX) {
			double max = calculation.isUnsigned() ? 0 : -Double.MAX_VALUE;
			for (int j = transition.thresholdStartIndex; j > transition.recoveryStartIndex; j++)
				if (record.realRealGet(j) != null) {
					max = Math.max(max, calculation.isUnsigned() ? Math.abs(this.device.translateValue(record, record.realRealGet(j))) : this.device.translateValue(record, record.realRealGet(j)));
				}
			reverseTranslatedResult = (int) reverseTranslateValue(max);
		}
		else if (calculation.getCalcType() == CalculationTypes.AVG) {
			double avg = 0., value = 0.;
			int skipCount = 0;
			for (int j = transition.thresholdStartIndex; j > transition.recoveryStartIndex; j++)
				if (record.realRealGet(j) != null) {
					value = calculation.isUnsigned() ? Math.abs(this.device.translateValue(record, record.realRealGet(j))) : this.device.translateValue(record, record.realRealGet(j));
					double deltaAvg = value - avg;
					avg += deltaAvg / (j - transition.thresholdStartIndex - skipCount + 1);
				}
				else {
					skipCount++;
				}
			reverseTranslatedResult = (int) reverseTranslateValue(avg);
		}
		else if (calculation.getCalcType() == CalculationTypes.SIGMA) {
			double avg = 0., q = 0., value = 0.;
			int skipCount = 0;
			for (int j = transition.thresholdStartIndex; j > transition.recoveryStartIndex; j++)
				if (record.realRealGet(j) != null) {
					value = calculation.isUnsigned() ? Math.abs(this.device.translateValue(record, record.realRealGet(j))) : this.device.translateValue(record, record.realRealGet(j));
					double deltaAvg = value - avg;
					avg += deltaAvg / (j - transition.thresholdStartIndex - skipCount + 1);
					q += deltaAvg * (value - avg);
				}
				else {
					skipCount++;
				}
			reverseTranslatedResult = (int) reverseTranslateValue(Math.sqrt(q / (transition.thresholdSize - skipCount - 1)));
		}
		else if (calculation.getCalcType() == CalculationTypes.SUM) {
			double sum = 0.;
			for (int j = transition.thresholdStartIndex; j > transition.recoveryStartIndex; j++)
				if (record.realRealGet(j) != null) {
					double calculationTranslateValue = this.device.translateValue(record, record.realRealGet(j));
					sum += calculation.isUnsigned() ? Math.abs(calculationTranslateValue) : calculationTranslateValue;
				}
			reverseTranslatedResult = (int) reverseTranslateValue(sum);
		}
		else {
			reverseTranslatedResult = 0;
		}
		// add to settlement record
		addNullableRaw(reverseTranslatedResult, (long) record.getTime_ms(transition.thresholdEndIndex) * 10); // todo assign better to the time step of the extremum value in the threshold and recovery time
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("%s: timeStamp_ms=%f  reverseTranslateValue=%d", calculation.getCalcType(), record.getTime_ms(transition.recoveryStartIndex //$NON-NLS-1$
				- 1), reverseTranslatedResult));
	}

	/**
	 * walks through the measurement record and calculates the difference between the threshold level value and the base level value (reference or recovery).
	 * skips null measurement values. 
	 * @param record holds the measurement points
	 * @param leveling rule for determining the level value from the device configuration
	 * @param transition holds the transition properties which are used to access the measurement data
	 * @return peak spread value as translatedValue
	 */
	private double calculateLevelDelta(Record record, LevelingTypes leveling, Transition transition) {
		double deltaValue = 0.;
		CalculationType calculation = this.settlement.getEvaluation().getCalculation();
		// determine the direction of the peak or pulse or jump based on streams which perform good enough
		final boolean isPositiveDirection;
		final double referenceAvg = record.subList(transition.referenceStartIndex, transition.referenceEndIndex + 1).parallelStream().filter(Objects::nonNull).mapToInt(w -> w).average().getAsDouble();
		// one additional time step before and after in order to cope with potential measurement latencies
		final double thresholdAvg = record.subList(transition.thresholdStartIndex - 1, transition.thresholdEndIndex + 2).parallelStream().filter(Objects::nonNull).mapToInt(w -> w).average().getAsDouble();
		if (transition.isSlope()) {
			isPositiveDirection = referenceAvg < thresholdAvg;
		}
		else {
			final double recoveryAvg = record.subList(transition.recoveryStartIndex, transition.recoveryEndIndex + 1).parallelStream().filter(Objects::nonNull).mapToInt(w -> w).average().getAsDouble();
			isPositiveDirection = referenceAvg + recoveryAvg < 2 * thresholdAvg;
		}

		double referenceExtremum = 0.;
		double thresholdExtremum = 0.;
		double recoveryExtremum = 0.;
		if (leveling == LevelingTypes.FIRST) {
			for (int j = transition.referenceStartIndex; j < transition.thresholdStartIndex; j++) {
				if (record.realRealGet(j) != null) {
					referenceExtremum = this.device.translateValue(record, record.realRealGet(j));
					break;
				}
			}
			for (int j = transition.thresholdStartIndex; j < transition.recoveryStartIndex; j++) {
				if (record.realRealGet(j) != null) {
					thresholdExtremum = this.device.translateValue(record, record.realRealGet(j));
					break;
				}
			}
			for (int j = transition.recoveryStartIndex; j < transition.recoveryEndIndex; j++) {
				if (record.realRealGet(j) != null) {
					recoveryExtremum = this.device.translateValue(record, record.realRealGet(j));
					break;
				}
			}
		}
		else if (leveling == LevelingTypes.LAST) {
			for (int j = transition.referenceEndIndex; j >= transition.referenceStartIndex; j--) {
				if (record.realRealGet(j) != null) {
					referenceExtremum = this.device.translateValue(record, record.realRealGet(j));
					break;
				}
			}
			for (int j = transition.thresholdEndIndex; j >= transition.thresholdStartIndex; j--) {
				if (record.realRealGet(j) != null) {
					thresholdExtremum = this.device.translateValue(record, record.realRealGet(j));
					break;
				}
			}
			for (int j = transition.recoveryEndIndex; j >= transition.recoveryStartIndex; j--) {
				if (record.realRealGet(j) != null) {
					recoveryExtremum = this.device.translateValue(record, record.realRealGet(j));
					break;
				}
			}
		}
		else if (leveling == LevelingTypes.MID) {
			if (transition.referenceSize > 1) {
				int midIndex = (transition.referenceStartIndex + transition.referenceEndIndex) / 2;
				for (int i = midIndex, j = midIndex + 1; i <= transition.referenceEndIndex && j >= transition.referenceStartIndex; i++, j--) {
					if (record.realRealGet(i) != null) {
						referenceExtremum = this.device.translateValue(record, record.realRealGet(i));
						break;
					}
					if (record.realRealGet(j) != null) {
						referenceExtremum = this.device.translateValue(record, record.realRealGet(j));
						break;
					}
				}
			}
			else {
				referenceExtremum = this.device.translateValue(record, record.realRealGet(transition.referenceStartIndex));
			}
			if (transition.thresholdSize > 1) {
				int midIndex = (transition.thresholdStartIndex + transition.thresholdEndIndex) / 2;
				for (int i = midIndex, j = midIndex; i <= transition.thresholdEndIndex && j >= transition.thresholdStartIndex; j--) {
					if (record.realRealGet(i) != null) {
						thresholdExtremum = this.device.translateValue(record, record.realRealGet(i));
						break;
					}
					if (record.realRealGet(j) != null) {
						thresholdExtremum = this.device.translateValue(record, record.realRealGet(j));
						break;
					}
				}
			}
			else {
				thresholdExtremum = this.device.translateValue(record, record.realRealGet(transition.thresholdStartIndex));
			}
			if (transition.recoverySize > 1) {
				int midIndex = (transition.recoveryStartIndex + transition.recoveryEndIndex) / 2;
				for (int i = midIndex, j = midIndex; i <= transition.recoveryEndIndex && j >= transition.recoveryStartIndex; j--) {
					if (record.realRealGet(i) != null) {
						recoveryExtremum = this.device.translateValue(record, record.realRealGet(i));
						break;
					}
					if (record.realRealGet(j) != null) {
						recoveryExtremum = this.device.translateValue(record, record.realRealGet(j));
						break;
					}
				}
			}
			else {
				recoveryExtremum = this.device.translateValue(record, record.realRealGet(transition.recoveryStartIndex));
			}
		}
		else if (leveling == LevelingTypes.MIN || leveling == LevelingTypes.MAX) {
			if (isPositiveDirection) {
				int value = Integer.MAX_VALUE;
				for (int j = transition.referenceStartIndex; j < transition.thresholdStartIndex; j++) {
					if (record.realRealGet(j) != null && record.realRealGet(j) < value) value = record.realRealGet(j);
				}
				referenceExtremum = this.device.translateValue(record, value);
				value = Integer.MIN_VALUE;
				// one additional time step before and after in order to cope with potential measurement latencies
				for (int j = transition.thresholdStartIndex - 1; j < transition.recoveryStartIndex + 1; j++) {
					if (record.realRealGet(j) != null && record.realRealGet(j) > value) value = record.realRealGet(j);
				}
				thresholdExtremum = this.device.translateValue(record, value);
				value = Integer.MAX_VALUE;
				for (int j = transition.recoveryStartIndex; j < transition.recoveryEndIndex + 1; j++) {
					if (record.realRealGet(j) != null && record.realRealGet(j) < value) value = record.realRealGet(j);
				}
				recoveryExtremum = this.device.translateValue(record, value);
			}
			else {
				int value = Integer.MIN_VALUE;
				for (int j = transition.referenceStartIndex; j < transition.thresholdStartIndex; j++) {
					if (record.realRealGet(j) != null && record.realRealGet(j) > value) value = record.realRealGet(j);
				}
				referenceExtremum = this.device.translateValue(record, value);
				value = Integer.MAX_VALUE;
				// one additional time step before and after in order to cope with potential measurement latencies
				for (int j = transition.thresholdStartIndex - 1; j < transition.recoveryStartIndex + 1; j++) {
					if (record.realRealGet(j) != null && record.realRealGet(j) < value) value = record.realRealGet(j);
				}
				thresholdExtremum = this.device.translateValue(record, value);
				value = Integer.MIN_VALUE;
				for (int j = transition.recoveryStartIndex; j < transition.recoveryEndIndex + 1; j++) {
					if (record.realRealGet(j) != null && record.realRealGet(j) > value) value = record.realRealGet(j);
				}
				recoveryExtremum = this.device.translateValue(record, value);
			}
		}
		else if (leveling == LevelingTypes.AVG) {
			double value = 0.;
			int skipCount = 0;
			for (int j = transition.referenceStartIndex; j < transition.thresholdStartIndex; j++)
				if (record.realRealGet(j) != null)
					value += (record.realRealGet(j) - value) / (double) (j - transition.referenceStartIndex + 1);
				else
					skipCount++;
			referenceExtremum = this.device.translateValue(record, value / (transition.referenceSize - skipCount));
			value = 0.;
			skipCount = 0;
			for (int j = transition.thresholdStartIndex; j < transition.recoveryStartIndex; j++)
				if (record.realRealGet(j) != null)
					value += (record.realRealGet(j) - value) / (double) (j - transition.thresholdStartIndex + 1);
				else
					skipCount++;
			thresholdExtremum = this.device.translateValue(record, value / (transition.thresholdSize - skipCount));
			value = 0.;
			skipCount = 0;
			for (int j = transition.recoveryStartIndex; j < transition.recoveryEndIndex + 1; j++)
				if (record.realRealGet(j) != null)
					value += (record.realRealGet(j) - value) / (double) (j - transition.recoveryStartIndex + 1);
				else
					skipCount++;
			recoveryExtremum = this.device.translateValue(record, value / (transition.recoverySize - skipCount));
		}
		else if (leveling == LevelingTypes.QBOTTOM || leveling == LevelingTypes.QTOP) {
			List<Integer> values = new ArrayList<Integer>();
			for (int j = transition.referenceStartIndex; j < transition.thresholdStartIndex; j++) {
				if (record.realRealGet(j) != null) values.add(record.realRealGet(j));
			}
			Quantile quantile = new Quantile(values, EnumSet.noneOf(Fixings.class));
			referenceExtremum = this.device.translateValue(record,
					quantile.getQuantile(!isPositiveDirection ? 1. - this.settings.getReferenceQuantileDistance() : this.settings.getReferenceQuantileDistance()));
			values = new ArrayList<Integer>();
			// one additional time step before and after in order to cope with potential measurement latencies
			for (int j = transition.thresholdStartIndex - 1; j < transition.recoveryStartIndex + 1; j++) {
				if (record.realRealGet(j) != null) values.add(record.realRealGet(j));
			}
			quantile = new Quantile(values, EnumSet.noneOf(Fixings.class));
			thresholdExtremum = this.device.translateValue(record,
					quantile.getQuantile(isPositiveDirection ? 1. - this.settings.getReferenceQuantileDistance() : this.settings.getReferenceQuantileDistance()));
			values = new ArrayList<Integer>();
			for (int j = transition.recoveryStartIndex; j < transition.recoveryEndIndex + 1; j++) {
				if (record.realRealGet(j) != null) values.add(record.realRealGet(j));
			}
			quantile = new Quantile(values, EnumSet.noneOf(Fixings.class));
			recoveryExtremum = this.device.translateValue(record,
					quantile.getQuantile(!isPositiveDirection ? 1. - this.settings.getReferenceQuantileDistance() : this.settings.getReferenceQuantileDistance()));
		}
		else {
			throw new UnsupportedOperationException();
		}

		if (transition.isSlope() || calculation.getDeltaBasis() == null || calculation.getDeltaBasis() == DeltaBasisTypes.REFERENCE)
			deltaValue = thresholdExtremum - referenceExtremum;
		else if (calculation.getDeltaBasis() == DeltaBasisTypes.RECOVERY)
			deltaValue = thresholdExtremum - recoveryExtremum;
		else if (calculation.getDeltaBasis() == DeltaBasisTypes.BOTH_AVG)
			deltaValue = thresholdExtremum - (referenceExtremum + recoveryExtremum) / 2;
		else if (calculation.getDeltaBasis() == DeltaBasisTypes.INNER)
			deltaValue = isPositiveDirection ? thresholdExtremum - Math.max(referenceExtremum, recoveryExtremum) : thresholdExtremum - Math.min(referenceExtremum, recoveryExtremum);
		else if (calculation.getDeltaBasis() == DeltaBasisTypes.OUTER)
			deltaValue = isPositiveDirection ? thresholdExtremum - Math.min(referenceExtremum, recoveryExtremum) : thresholdExtremum - Math.max(referenceExtremum, recoveryExtremum);
		else
			throw new UnsupportedOperationException();

		if (log.isLoggable(Level.OFF)) log.log(Level.OFF, String.format("%s %s %s referenceExtremum=%f  thresholdExtremum=%f  recoveryExtremum=%f  deltaValue=%f  @ isBasedOnRecovery=%s" //$NON-NLS-1$
				, this.getName(), record.getName(), leveling.value(), referenceExtremum, thresholdExtremum, recoveryExtremum, deltaValue, calculation.getDeltaBasis()));
		return deltaValue;
	}

	/**
	 * walk forward from the time step when the trigger has fired and collect the extremum values for the calculation.
	 * add single result value.
	 * the result value in case of ratio or count is multiplied by 1000; the timeSum value has the unit seconds with 3 decimal places. 
	 * @param transition
	 */
	private void calculateAndAdd4Peak(Transition transition) {
		CalculationType calculation = this.settlement.getEvaluation().getCalculation();
		log.log(Level.OFF, calculation.toString());
		final Record record = this.parent.get(this.parent.recordNames[calculation.getRefOrdinal()]);
		//		if (log.isLoggable(Level.OFF)) log.log(Level.OFF,
		//				String.format("%s: referenceDequeSize=%d thresholdDequeSize=%d recoveryDequeSize=%d", record.getName(), transition.getReferenceSize(), transition.thresholdSize, transition.getRecoverySize())); //$NON-NLS-1$
		log.log(Level.FINE, record.getName() + " values   " //$NON-NLS-1$
				+ record.subList(transition.referenceStartIndex, transition.recoveryEndIndex + 1));
		final int reverseTranslatedResult;
		if (calculation.getCalcType() == CalculationTypes.RATIO || calculation.getCalcType() == CalculationTypes.RATIO_INVERSE) {
			final double denominator = calculateLevelDelta(record, calculation.getLeveling(), transition);
			final Record divisorRecord = calculation.getRefOrdinalDivisor() != null ? this.parent.get(this.parent.recordNames[calculation.getRefOrdinalDivisor()]) : null;
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, divisorRecord.getName() + " divisors " //$NON-NLS-1$
					+ divisorRecord.subList(transition.referenceStartIndex, transition.recoveryEndIndex + 1));
			final double divisor = calculateLevelDelta(divisorRecord, calculation.getDivisorLeveling(), transition);
			if (calculation.getCalcType() == CalculationTypes.RATIO) {
				reverseTranslatedResult = calculation.isUnsigned() ? (int) Math.abs(1000000. * denominator / divisor) : (int) (1000000. * denominator / divisor); // all internal values are multiplied by 1000, ratio in permille
			}
			else {
				reverseTranslatedResult = calculation.isUnsigned() ? (int) Math.abs(1000000. * divisor / denominator) : (int) (1000000. * divisor / denominator); // all internal values are multiplied by 1000, ratio in permille
			}
		}
		else if (calculation.getCalcType() == CalculationTypes.COUNT) {
			reverseTranslatedResult = 1000 * transition.thresholdSize; // all internal values are multiplied by 1000
		}
		else if (calculation.getCalcType() == CalculationTypes.TIME_SUM_SEC) {
			reverseTranslatedResult = (int) reverseTranslateValue(this.parent.getTime_ms(transition.recoveryStartIndex) - this.parent.getTime_ms(transition.thresholdStartIndex));
		}
		else if (calculation.getCalcType() == CalculationTypes.MIN) {
			double min = Double.MAX_VALUE;
			for (int j = transition.thresholdStartIndex; j < transition.recoveryStartIndex; j++)
				if (record.realRealGet(j) != null)
					min = Math.min(min, calculation.isUnsigned() ? Math.abs(this.device.translateValue(record, record.realRealGet(j))) : this.device.translateValue(record, record.realRealGet(j)));
			reverseTranslatedResult = (int) reverseTranslateValue(min);
		}
		else if (calculation.getCalcType() == CalculationTypes.MAX) {
			double max = calculation.isUnsigned() ? 0. : -Double.MAX_VALUE;
			for (int j = transition.thresholdStartIndex; j < transition.recoveryStartIndex; j++)
				if (record.realRealGet(j) != null)
					max = Math.max(max, calculation.isUnsigned() ? Math.abs(this.device.translateValue(record, record.realRealGet(j))) : this.device.translateValue(record, record.realRealGet(j)));
			reverseTranslatedResult = (int) reverseTranslateValue(max);
		}
		else if (calculation.getCalcType() == CalculationTypes.AVG) {
			double avg = 0., value = 0.;
			int skipCount = 0;
			for (int j = transition.thresholdStartIndex; j < transition.recoveryStartIndex; j++)
				if (record.realRealGet(j) != null) {
					value = calculation.isUnsigned() ? Math.abs(this.device.translateValue(record, record.realRealGet(j))) : this.device.translateValue(record, record.realRealGet(j));
					double deltaAvg = value - avg;
					avg += deltaAvg / (j - transition.thresholdStartIndex - skipCount + 1);
				}
				else
					skipCount++;
			reverseTranslatedResult = (int) reverseTranslateValue(avg);
		}
		else if (calculation.getCalcType() == CalculationTypes.SIGMA) {
			// https://en.wikipedia.org/wiki/Standard_deviation#Rapid_calculation_methods
			double avg = 0., q = 0., value = 0.;
			int skipCount = 0;
			for (int j = transition.thresholdStartIndex; j < transition.recoveryStartIndex; j++)
				if (record.realRealGet(j) != null) {
					value = calculation.isUnsigned() ? Math.abs(this.device.translateValue(record, record.realRealGet(j))) : this.device.translateValue(record, record.realRealGet(j));
					double deltaAvg = value - avg;
					avg += deltaAvg / (j - transition.thresholdStartIndex - skipCount + 1);
					q += deltaAvg * (value - avg);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("value=%f  deltaAvg=%f  q=%f", value, deltaAvg, q));
				}
				else
					skipCount++;
			reverseTranslatedResult = (int) reverseTranslateValue(Math.sqrt(q / (transition.thresholdSize - skipCount - 1)));
		}
		else if (calculation.getCalcType() == CalculationTypes.SUM) {
			double sum = 0.;
			for (int j = transition.thresholdStartIndex; j < transition.recoveryStartIndex; j++)
				sum += calculation.isUnsigned() ? Math.abs(this.device.translateValue(record, record.realRealGet(j))) : this.device.translateValue(record, record.realRealGet(j));
			reverseTranslatedResult = (int) reverseTranslateValue(sum);
		}
		else {
			reverseTranslatedResult = 0;
			throw new UnsupportedOperationException();
		}
		// add to settlement record  
		addNullableRaw(reverseTranslatedResult, (long) record.getTime_ms(transition.thresholdEndIndex) * 10); // todo assign better to the time step of the extremum value in the threshold and recovery time
		if (log.isLoggable(Level.OFF)) log.log(Level.OFF, String.format("%s: timeStamp_ms=%f  reverseTranslatedResult=%d  calcType=%s", this.getName(), record.getTime_ms(transition.recoveryStartIndex) //$NON-NLS-1$
				, reverseTranslatedResult, calculation.getCalcType()));
	}

	/**
	 * add a data point to the settlement data, checks for minimum and maximum to define display range
	 * @param point
	 * @param time_100ns in 0.1 ms (divide by 10 to get ms)
	 */
	private synchronized boolean addNullableRaw(Integer point, long time_100ns) {
		if (this.timeStep_ms != null) this.timeStep_ms.addRaw(time_100ns);
		return this.add(point);
	}

	/**
	 * add a data point to the record data, checks for minimum and maximum to define display range
	 * @param point
	 */
	@Override
	public synchronized boolean add(Integer point) {
		final String $METHOD_NAME = "add"; //$NON-NLS-1$
		if (point != null) {
			if (super.size() == 0) {
				this.minValue = this.maxValue = point;
			}
			else {
				if (point > this.maxValue)
					this.maxValue = point;
				else if (point < this.minValue) this.minValue = point;
			}
			if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, this.name + " adding point = " + point); //$NON-NLS-1$
			if (log.isLoggable(Level.FINEST)) log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, this.name + " minValue = " + this.minValue + " maxValue = " + this.maxValue); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return super.add(point);
	}

	@Override
	public synchronized Integer set(int index, Integer point) {
		final String $METHOD_NAME = "set"; //$NON-NLS-1$
		if (super.size() == 0) {
			this.minValue = this.maxValue = point;
		}
		else {
			if (point > this.maxValue)
				this.maxValue = point;
			else if (point < this.minValue) this.minValue = point;
		}
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, this.name + " setting point = " + point); //$NON-NLS-1$
		if (log.isLoggable(Level.FINEST)) log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, this.name + " minValue = " + this.minValue + " maxValue = " + this.maxValue); //$NON-NLS-1$ //$NON-NLS-2$
		return super.set(index, point);
	}

	/**
	 * get property reference using given property type key (IDevice.OFFSET, ...)
	 * @param propertyKey
	 * @return PropertyType
	 */
	private PropertyType getProperty(String propertyKey) {
		PropertyType property = null;
		for (PropertyType propertyType : this.properties) {
			if (propertyType.getName().equals(propertyKey)) {
				property = propertyType;
				break;
			}
		}
		return property;
	}

	/**
	 * create a property and return the reference
	 * @param propertyKey
	 * @param type
	 * @return created property with associated propertyKey
	 */
	private PropertyType createProperty(String propertyKey, DataTypes type, Object value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(propertyKey);
		newProperty.setType(type);
		newProperty.setValue(GDE.STRING_EMPTY + value);
		this.properties.add(newProperty);
		return newProperty;
	}

	public double getFactor() {
		double value = 1.0;
		PropertyType property = this.getProperty(IDevice.FACTOR);
		if (property != null)
			value = Double.valueOf(property.getValue()).doubleValue();
		else
			try {
				value = this.getDevice().getMeasurementFactor(this.parent.parent.number, this.ordinal);
			}
			catch (RuntimeException e) {
				// log.log(Level.WARNING, this.name + " use default value for property " + IDevice.FACTOR); // log warning and use default value
			}
		return value;
	}

	public void setFactor(double newValue) {
		PropertyType property = this.getProperty(IDevice.FACTOR);
		if (property != null)
			property.setValue(String.format("%.4f", newValue)); //$NON-NLS-1$
		else
			this.createProperty(IDevice.FACTOR, DataTypes.DOUBLE, String.format(Locale.ENGLISH, "%.4f", newValue)); //$NON-NLS-1$
	}

	public double getOffset() {
		double value = 0.0;
		PropertyType property = this.getProperty(IDevice.OFFSET);
		if (property != null)
			value = Double.valueOf(property.getValue()).doubleValue();
		else
			try {
				value = this.getDevice().getMeasurementOffset(this.parent.parent.number, this.ordinal);
			}
			catch (RuntimeException e) {
				// log.log(Level.WARNING, this.name + " use default value for property " + IDevice.OFFSET); // log warning and use default value
			}
		return value;
	}

	public void setOffset(double newValue) {
		PropertyType property = this.getProperty(IDevice.OFFSET);
		if (property != null)
			property.setValue(String.format("%.4f", newValue)); //$NON-NLS-1$
		else
			this.createProperty(IDevice.OFFSET, DataTypes.DOUBLE, String.format(Locale.ENGLISH, "%.4f", newValue)); //$NON-NLS-1$
	}

	public double getReduction() {
		double value = 0.0;
		PropertyType property = this.getProperty(IDevice.REDUCTION);
		if (property != null)
			value = Double.valueOf(property.getValue()).doubleValue();
		else {
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

	public void setReduction(double newValue) {
		PropertyType property = this.getProperty(IDevice.REDUCTION);
		if (property != null)
			property.setValue(String.format("%.4f", newValue)); //$NON-NLS-1$
		else
			this.createProperty(IDevice.REDUCTION, DataTypes.DOUBLE, String.format(Locale.ENGLISH, "%.4f", newValue)); //$NON-NLS-1$
	}

	public int getRealMaxValue() {
		return this.maxValue;
	}

	public int getRealMinValue() {
		return this.minValue;
	}

	/**
	 * return the 'best fit' number of measurement points in dependency of zoomMode or scopeMode
	 */
	@Override
	public synchronized int size() {
		int tmpSize = super.elementCount;

		return tmpSize;
	}

	/**
	 * time calculation needs always the real size of the record
	 * @return real vector size 
	 */
	public int realSize() {
		return super.size();
	}

	public Integer getFirst() {
		return super.size() > 0 ? super.get(0) : 0;
	}

	public Integer getLast() {
		return super.size() > 0 ? super.get(super.size() - 1) : 0;
	}

	/**
	 * overwrites vector get(int index) to enable zoom
	 * @param index
	 */
	@Override
	public synchronized Integer get(int index) {
		index = index > (this.elementCount - 1) ? (this.elementCount - 1) : index;
		index = index < 0 ? 0 : index;

		int returnValue = super.get(index);
		// log.log(Level.INFO, "index=" + index);
		if (this.elementCount != 0) {
			if (!this.parent.isCompareSet) {
			}
			return returnValue;
		}
		return 0;
	}

	public Integer realRealGet(int index) {
		return super.get(index);
	}

	/**
	 * ET: throws NullPointerException if super.get(index) is null.
	 * in debugging mode, however, the expression 'super.size() != 0 ? super.get(index) : 0' evaluates to null which is correct.
	 * could not clarify the reason for the exception <<<
	 * @param index
	 */
	public Integer realGet(int index) {
		try {
			return super.size() != 0 ? super.get(index) : 0;
		}
		catch (ArrayIndexOutOfBoundsException e) {
			log.log(Level.WARNING, String.format("%s - %20s: size = %d - indesx = %d", this.parent.name, this.name, this.size(), index));
			return super.size() != 0 ? super.get(index - 1) : 0;
		}
	}

	/**
	 * @return the parent
	 */
	public RecordSet getParent() {
		return this.parent;
	}

	/** 
	 * return the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by the start date time of this record (set)
	 * @return time stamp of the date and time when the record starts
	 */
	public long getStartTimeStamp() {
		return this.timeStep_ms == null ? this.parent.timeStep_ms.getStartTimeStamp() : this.timeStep_ms.getStartTimeStamp();
	}

	/** 
	 * query time step time in mills seconds at index
	 * @return time step in msec
	 */
	public double getTime_ms(int index) {
		return this.timeStep_ms == null ? this.parent.timeStep_ms.getTime_ms(index) : this.timeStep_ms.getTime_ms(index);
	}

	/** 
	 * query time step time in mills seconds at index
	 * @return time step in msec
	 */
	public double getLastTime_ms() {
		return this.timeStep_ms == null ? this.parent.timeStep_ms.lastElement() / 10.0 : this.timeStep_ms.lastElement() / 10.0;
	}

	/** 
	 * query time step in mills seconds, this property is hold local to be independent (compare window)
	 * @return time step in msec
	 */
	public double getAverageTimeStep_ms() {
		return this.timeStep_ms == null ? this.parent.getAverageTimeStep_ms() : this.timeStep_ms.getAverageTimeStep_ms();
	}

	/**
	 * set the time step in milli seconds, this property is hold local to be independent
	 * @param timeStep_ms the timeStep_ms to set
	 */
	void setTimeStep_ms(double newTimeStep_ms) {
		this.timeStep_ms = new TimeSteps(newTimeStep_ms, HistoRecordSet.initialRecordCapacity);
	}

	/**
	 * @return the maximum time of this record, which should correspondence to the last entry in timeSteps
	 */
	public double getMaxTime_ms() {
		return this.timeStep_ms == null ? this.parent.getMaxTime_ms() : this.timeStep_ms.isConstant ? this.timeStep_ms.getMaxTime_ms() * (this.elementCount - 1) : this.timeStep_ms.getMaxTime_ms();
	}

	/**
	 * Find the indexes in this time vector where the given time value is placed
	 * In case of the given time in in between two available measurement points both bounding indexes are returned, 
	 * only in case where the given time matches an existing entry both indexes are equal.
	 * In cases where the returned indexes are not equal the related point x/y has to be interpolated.
	 * @param time_ms
	 * @return two index values around the given time 
	 */
	public int[] findBoundingIndexes(double time_ms) {
		int[] indexs = this.timeStep_ms == null ? this.parent.timeStep_ms.findBoundingIndexes(time_ms) : this.timeStep_ms.findBoundingIndexes(time_ms);
		if (this.elementCount > 0) {
			indexs[0] = indexs[0] > this.elementCount - 1 ? this.elementCount - 1 : indexs[0];
			indexs[1] = indexs[1] > this.elementCount - 1 ? this.elementCount - 1 : indexs[1];
		}
		return indexs;
	}

	/**
	 * find the index closest to given time in msec
	 * @param time_ms
	 * @return index nearest to given time
	 */
	public int findBestIndex(double time_ms) {
		int index = this.timeStep_ms == null ? this.parent.timeStep_ms.findBestIndex(time_ms) : this.timeStep_ms.findBestIndex(time_ms);
		return index > this.elementCount - 1 ? this.elementCount - 1 : index;
	}

	/**
	 * get the device to calculate or retrieve measurement properties, this property is hold local to be independent
	 * @return the device
	 */
	public IDevice getDevice() {
		if (this.device == null) this.device = this.parent.getDevice();

		return this.device;
	}

	/**
	 * reset the min-max-values to enable new settings after re-calculation
	 */
	public void resetMinMax() {
		this.maxValue = 0;
		this.minValue = 0;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.name);
	}

	/**
	 * set new the min-max-values after external re-calculation
	 */
	public void setMinMax(int newMin, int newMax) {
		this.maxValue = newMax;
		this.minValue = newMin;
	}

	/**
	 * reset all variables to enable re-calcualation of statistics
	 */
	public void resetStatiticCalculationBase() {
		synchronized (this) {
			this.avgValue = Integer.MIN_VALUE;
			this.sigmaValue = Integer.MIN_VALUE;
			this.sumValue = Integer.MIN_VALUE;
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.name);
		}
	}

	/**
	 * @return the sumValue
	 */
	public int getSumValue() {
		this.setSumValue();
		return this.sumValue;
	}

	/**
	 * calculates the sumValue
	 */
	public void setSumValue() {
		synchronized (this) {
			if (this.size() >= 1) {
				long sum = 0;
				for (Integer xi : this) {
					sum += xi;
				}
				// all measurement values are int. so sum should also be int. pls adjust property type 'factor'.
				if (sum > Integer.MAX_VALUE || sum < Integer.MIN_VALUE) throw new IllegalArgumentException();
				this.sumValue = (int) sum;
			}
		}
	}

	/**
	 * @return the avgValue
	 */
	public int getAvgValue() {
		this.setAvgValue();
		return this.avgValue;
	}

	/**
	 * calculates the avgValue
	 */
	public void setAvgValue() {
		synchronized (this) {
			if (this.size() >= 1) { // ET changed from 2 to 1
				long sum = 0;
				int zeroCount = 0;
				for (Integer xi : this) {
					if (xi != 0) {
						sum += xi;
					}
					else {
						zeroCount++;
					}
				}
				this.avgValue = (this.size() - zeroCount) != 0 ? (int) (sum / (this.size() - zeroCount)) : 0;
			}
			else {
				this.avgValue = 0;
			}
		}
	}

	/**
	 * @return the sigmaValue
	 */
	public int getSigmaValue() {
		this.setSigmaValue();
		return this.sigmaValue;
	}

	/**
	 * calculates the sigmaValue 
	 */
	public void setSigmaValue() {
		synchronized (this) {
			if (super.size() >= 2) {
				double average = this.getAvgValue() / 1000.0;
				double sumPoweredValues = 0;
				for (Integer xi : this) {
					sumPoweredValues += Math.pow(xi / 1000.0 - average, 2);
				}
				this.sigmaValue = (int) (Math.sqrt(sumPoweredValues / (this.realSize() - 1)) * 1000);
			}
			else {
				this.sigmaValue = 0;
			}
		}
	}

	/**
	 * @return the sumValue
	 */
	public int getCountValue() {
		return this.countValue;
	}

	/**
	 * @return true if the record contained reasonable date which can be displayed
	 */
	public boolean hasReasonableData() {
		return this.realSize() > 0 && (this.minValue != this.maxValue || translateValue(this.maxValue / 1000.0) != 0.0);
	}

	/**
	 * function to translate settlement values to values represented.
	 * does not support settlements based on GPS-longitude or GPS-latitude.
	 * @return double of device dependent value
	 */
	private double translateValue(double value) {
		double factor = this.getFactor(); // != 1 if a unit translation is required
		double offset = this.getOffset(); // != 0 if a unit translation is required
		double reduction = this.getReduction(); // != 0 if a unit translation is required
		double newValue = 0.;

		if (this.getParent().getChannelConfigNumber() == 3 && (this.ordinal == 1 || this.ordinal == 2)) { // 1=GPS-longitude 2=GPS-latitude
			// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			int grad = ((int) (value / 1000));
			double minuten = (value - (grad * 1000.0)) / 10.0;
			newValue = grad + minuten / 60.0;
		}
		else {
			newValue = (value - reduction) * factor + offset;
		}
		// if (log.isLoggable(Level.FINER))
		log.log(Level.FINER, "for " + this.name + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to translate values represented into normalized settlement values.
	 * does not support settlements based on GPS-longitude or GPS-latitude.
	 * @return double of settlement dependent value
	 */
	private double reverseTranslateValue(double value) {
		double factor = this.getFactor(); // != 1 if a unit translation is required
		double offset = this.getOffset(); // != 0 if a unit translation is required
		double reduction = this.getReduction(); // != 0 if a unit translation is required
		double newValue = 0.;

		// if (this.getParent().getChannelConfigNumber() == 3 && (this.getOrdinal() == 1 || this.getOrdinal() == 2)) { // 1=GPS-longitude 2=GPS-latitude )
		// // 0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		// int grad = (int) value;
		// double minuten = (value - grad * 1.0) * 60.0;
		// newValue = (grad + minuten / 100.0) * 1000.0;
		// } else {
		newValue = (value - offset) / factor + reduction;
		// }
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + this.name + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	public String getName() {
		return this.name;
	}

	/**
	 * @return the settlement
	 */
	public SettlementType getSettlement() {
		return settlement;
	}

}
