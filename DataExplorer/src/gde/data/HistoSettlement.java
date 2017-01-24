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
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Vector;
import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.device.AmountTypes;
import gde.device.CalculusTypes;
import gde.device.ChannelType;
import gde.device.DataTypes;
import gde.device.DeltaBasisTypes;
import gde.device.FigureTypes;
import gde.device.IDevice;
import gde.device.LevelingTypes;
import gde.device.MeasurementMappingType;
import gde.device.ObjectFactory;
import gde.device.PropertyType;
import gde.device.ReferenceGroupType;
import gde.device.ReferenceRuleTypes;
import gde.device.SettlementMappingType;
import gde.device.SettlementType;
import gde.device.TransitionAmountType;
import gde.device.TransitionCalculusType;
import gde.device.TransitionFigureType;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.Quantile;
import gde.utils.Quantile.Fixings;

/**
 * holds settlement points of a line or curve calculated from measurements.
 * applicable for settlements with an evaluation rule.
 * does not support settlement display or table view.
 * similar to record class except: clone, serialization, zoom / scope, triggers, syncWithRecords, setName, GPS-longitude, GPS-latitude.
 * @author Thomas Eickert
 */
public class HistoSettlement extends Vector<Integer> {
	final static String						$CLASS_NAME						= HistoSettlement.class.getName();
	final static long							serialVersionUID			= 6130190003229390899L;
	final static Logger						log										= Logger.getLogger($CLASS_NAME);

	private final static int			initialRecordCapacity	= 111;

	private final IDevice					device								= DataExplorer.getInstance().getActiveDevice();
	private final Settings				settings							= Settings.getInstance();

	private final TimeSteps				timeStep_ms						= null;																					// timeStep_ms for each measurement point in compare set, where time step of measurement points might be individual
	private final SettlementType	settlement;
	// device measurement configuration and used to find specific properties

	private final RecordSet				parent;
	private final int							logChannelNumber;
	String												name;																																	// measurement name Höhe
	String												unit;																																	// unit [m]
	String												symbol;																																// symbol h

	List<PropertyType>						properties						= new ArrayList<PropertyType>();								// offset, factor, reduction, ...
	private int										maxValue							= 0;																						// max value of the curve
	private int										minValue							= 0;																						// min value of the curve

	// statistics
	private int										avgValue							= Integer.MIN_VALUE;														// average value (avg = sum(xi)/n)
	private int										sigmaValue						= Integer.MIN_VALUE;														// sigma value of data
	private int										sumValue							= Integer.MIN_VALUE;														// sum value of data
	private int										transitionCounter			= 0;

	/**
	 * @author Thomas Eickert
	 * performs the aggregation of translated record values.
	 * The aggregation is based on the reference rule. 
	 * null support was not tested up to now.
	 */
	private class RecordGroup {

		private IDevice							device;
		private RecordSet						recordSet;
		private ReferenceGroupType	referenceGroupType;
		private Record[]						records;

		public RecordGroup(IDevice device, RecordSet recordSet, ReferenceGroupType referenceGroupType) {
			this.device = device;
			this.recordSet = recordSet;
			this.referenceGroupType = referenceGroupType;
			this.records = new Record[referenceGroupType.getMeasurementMapping().size() + referenceGroupType.getSettlementMapping().size()];

			int i = 0;
			for (MeasurementMappingType measurementMappingType : referenceGroupType.getMeasurementMapping()) {
				this.records[i] = this.recordSet.get(this.recordSet.recordNames[measurementMappingType.getMeasurementOrdinal()]);
				i++;
			}
			for (SettlementMappingType settlementMappingType : referenceGroupType.getSettlementMapping()) {
				throw new UnsupportedOperationException();
			}
		}

		/**
		 * @param fromIndex
		 * @param toIndex
		 * @return the portion of the un-translated values between fromIndex, inclusive, and toIndex, exclusive. (If fromIndex and toIndex are equal a NoSuchElementException is thrown.) 
		 */
		public Double getRawMedian(int fromIndex, int toIndex) {
			return new Quantile(getSubGrouped(fromIndex, toIndex), EnumSet.noneOf(Fixings.class)).getQuartile2();
		}

		public double getPropertyFactor() {
			String factor = this.records[0].getProperty(IDevice.FACTOR).getValue();
			for (Record record : this.records) {
				if (factor.equals(record.getProperty(IDevice.FACTOR))) throw new UnsupportedOperationException();
			}
			return Double.valueOf(factor);
		}

		/**
		 * @param index
		 * @return the aggregated un-translated value at this real index position (irrespective of zoom / scope)
		 */
		public Integer getRaw(int index) {
			Double result = 0.;
			for (int i = 0; i < this.records.length; i++) {
				Record record = this.records[i];
				if (record.realRealGet(index) == null) {
					result = null;
					break;
				}
				else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.AVG) {
					result += ((double) record.realRealGet(index) - result) / (i + 1);
				}
				else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.MAX) {
					if (i == 0)
						result = (double) record.realRealGet(index);
					else
						result = Math.max(result, (double) record.realRealGet(index));
				}
				else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.MIN) {
					if (i == 0)
						result = (double) record.realRealGet(index);
					else
						result = Math.max(result, (double) record.realRealGet(index));
				}
				else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.PRODUCT) {
					if (i == 0)
						result = (double) record.realRealGet(index);
					else
						result *= (double) record.realRealGet(index);
				}
				else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.QUOTIENT) {
					if (i == 0)
						result = (double) record.realRealGet(index);
					else
						result /= (double) record.realRealGet(index);
				}
				else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.SPREAD) {
					if (i == 0)
						result = (double) record.realRealGet(index);
					else
						result -= (double) record.realRealGet(index);
				}
				else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.SUM) {
					result += (double) record.realRealGet(index);
				}
				else
					throw new UnsupportedOperationException();
			}
			return result == null ? null : result.intValue();
		}

		/**
		 * @param index
		 * @return the aggregated translated value at this real index position (irrespective of zoom / scope)
		 */
		public Double get(int index) {
			Double result = 0.;
			for (int i = 0; i < this.records.length; i++) {
				Record record = this.records[i];
				if (record.realRealGet(index) == null) {
					result = null;
					break;
				}
				else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.AVG) {
					result += (this.device.translateValue(record, record.realRealGet(index)) - result) / (i + 1);
				}
				else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.MAX) {
					if (i != 0)
						result = Math.max(result, this.device.translateValue(record, record.realRealGet(index)));
					else
						result = this.device.translateValue(record, record.realRealGet(index));
				}
				else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.MIN) {
					if (i == 0)
						result = this.device.translateValue(record, record.realRealGet(index));
					else
						result = Math.max(result, this.device.translateValue(record, record.realRealGet(index)));
				}
				else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.PRODUCT) {
					if (i == 0)
						result = this.device.translateValue(record, record.realRealGet(index));
					else
						result *= this.device.translateValue(record, record.realRealGet(index));
				}
				else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.QUOTIENT) {
					if (i == 0)
						result = this.device.translateValue(record, record.realRealGet(index));
					else
						result /= this.device.translateValue(record, record.realRealGet(index));
				}
				else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.SPREAD) {
					if (i == 0)
						result = this.device.translateValue(record, record.realRealGet(index));
					else
						result -= this.device.translateValue(record, record.realRealGet(index));
				}
				else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.SUM) {
					result += this.device.translateValue(record, record.realRealGet(index));
				}
				else
					throw new UnsupportedOperationException();
			}
			return result;
		}

		/**
		 * @return the aggregated translated values.
		 */
		public Vector<Double> getGrouped() {
			return getSubGrouped(0, this.records[0].realSize());
		}

		/**
		 * @param fromIndex
		 * @param toIndex
		 * @return the portion of the aggregated translated values between fromIndex, inclusive, and toIndex, exclusive. (If fromIndex and toIndex are equal, the returned List is empty.) 
		 */
		public Vector<Double> getSubGrouped(int fromIndex, int toIndex) {
			int recordSize = toIndex - fromIndex;
			Vector<Double> result = new Vector<Double>(recordSize);
			for (int i = fromIndex; i < toIndex; i++) {
				result.add(get(i));
			}
			log.log(Level.FINER, getComment(), result);
			return result;
		}

		public String getComment() {
			return this.referenceGroupType.getComment();
		}
	}

	/**
	 * creates a vector to hold data points.
	 * @param newDevice
	 * @param newSettlement
	 * @param parent
	 * @param initialCapacity
	 */
	public HistoSettlement(SettlementType newSettlement, RecordSet parent, int logChannelNumber) {
		super(initialRecordCapacity);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, newSettlement.getName() + " Settlement(IDevice , SettlementType , int )"); //$NON-NLS-1$
		this.settlement = newSettlement;
		this.name = newSettlement.getName();
		this.parent = parent;
		this.logChannelNumber = logChannelNumber;
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
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%20s - %s = %s", recordRef.name, property.getName(), property.getValue())); //$NON-NLS-1$
			this.properties.add(property.clone());
		}
	}

	/**
	 * take histo transitions which are applicable and fetch trigger time steps and records from the parent.
	 * add settlement points according the evaluation rules.
	 * @param transitions holds all transitions identified for this settlement
	 */
	public synchronized void addFromTransitions(Collection<Transition> transitions) {
		this.transitionCounter = 0;
		for (Transition transition : transitions) {
			if (this.settlement.getEvaluation().getTransitionFigure() != null) {
				addFigure(transition);
			}
			else if (this.settlement.getEvaluation().getTransitionAmount() != null) {
				addAmount(transition);
			}
			else if (this.settlement.getEvaluation().getTransitionCalculus() != null) {
				addCalculus(transition);
			}
			else {
				throw new UnsupportedOperationException();
			}
		}
		this.transitionCounter++;
	}

	/**
	 * walks through the measurement record and calculates the difference between the threshold level value and the base level value (reference or recovery).
	 * skips null measurement values. 
	 * @param record holds the measurement points
	 * @param leveling rule for determining the level value from the device configuration
	 * @param transition holds the transition properties which are used to access the measurement data
	 * @return peak spread value as translatedValue
	 */
	private double calculateLevelDelta(int referenceGroupId, LevelingTypes leveling, Transition transition) {
		double deltaValue = 0.;
		final ChannelType logChannel = this.device.getDeviceConfiguration().getChannel(this.logChannelNumber);
		final RecordGroup recordGroup = new RecordGroup(this.device, this.parent, logChannel.getReferenceGroupById(referenceGroupId));
		TransitionCalculusType transitionCalculus = this.settlement.getEvaluation().getTransitionCalculus();

		// determine the direction of the peak or pulse or jump
		final boolean isPositiveDirection;
		final double referenceMedian = recordGroup.getRawMedian(transition.referenceStartIndex, transition.referenceEndIndex + 1);
		// one additional time step before and after in order to cope with potential measurement latencies
		final double thresholdMedian = recordGroup.getRawMedian(transition.thresholdStartIndex - 1, transition.thresholdEndIndex + 1 + 1);
		if (transition.isSlope()) {
			isPositiveDirection = (referenceMedian < thresholdMedian) ^ (recordGroup.getPropertyFactor() < 0.);
		}
		else {
			final double recoveryMedian = recordGroup.getRawMedian(transition.recoveryStartIndex, transition.recoveryEndIndex + 1);
			isPositiveDirection = (referenceMedian + recoveryMedian < 2. * thresholdMedian) ^ (recordGroup.getPropertyFactor() < 0.);
		}

		double referenceExtremum = 0.;
		double thresholdExtremum = 0.;
		double recoveryExtremum = 0.;
		if (leveling == LevelingTypes.FIRST) {
			for (int j = transition.referenceStartIndex; j < transition.referenceEndIndex + 1; j++) {
				Double aggregatedValue = recordGroup.get(j);
				if (aggregatedValue != null) {
					referenceExtremum = aggregatedValue;
					break;
				}
			}
			// one additional time step before and after in order to cope with potential measurement latencies
			for (int j = transition.thresholdStartIndex - 1; j < transition.thresholdEndIndex + 1 + 1; j++) {
				Double aggregatedValue = recordGroup.get(j);
				if (aggregatedValue != null) {
					thresholdExtremum = aggregatedValue;
					break;
				}
			}
			if (transition.recoveryStartIndex > 0) {
				for (int j = transition.recoveryStartIndex; j < transition.recoveryEndIndex + 1; j++) {
					Double aggregatedValue = recordGroup.get(j);
					if (aggregatedValue != null) {
						recoveryExtremum = aggregatedValue;
						break;
					}
				}
			}
		}
		else if (leveling == LevelingTypes.LAST) {
			for (int j = transition.referenceEndIndex; j >= transition.referenceStartIndex; j--) {
				Double aggregatedValue = recordGroup.get(j);
				if (aggregatedValue != null) {
					referenceExtremum = aggregatedValue;
					break;
				}
			}
			// one additional time step before and after in order to cope with potential measurement latencies
			for (int j = transition.thresholdEndIndex + 1; j >= transition.thresholdStartIndex - 1; j--) {
				Double aggregatedValue = recordGroup.get(j);
				if (aggregatedValue != null) {
					thresholdExtremum = aggregatedValue;
					break;
				}
			}
			if (transition.recoveryStartIndex > 0) {
				for (int j = transition.recoveryEndIndex; j >= transition.recoveryStartIndex; j--) {
					Double aggregatedValue = recordGroup.get(j);
					if (aggregatedValue != null) {
						recoveryExtremum = aggregatedValue;
						break;
					}
				}
			}
		}
		else if (leveling == LevelingTypes.MID) {
			if (transition.referenceSize > 1) {
				int midIndex = (transition.referenceStartIndex + transition.referenceEndIndex) / 2;
				for (int i = midIndex, j = midIndex + 1; i <= transition.referenceEndIndex && j >= transition.referenceStartIndex; i++, j--) {
					Double aggregatedValue = recordGroup.get(i);
					if (aggregatedValue != null) {
						referenceExtremum = aggregatedValue;
						break;
					}
					aggregatedValue = recordGroup.get(j);
					if (aggregatedValue != null) {
						referenceExtremum = aggregatedValue;
						break;
					}
				}
			}
			else {
				referenceExtremum = recordGroup.get(transition.referenceStartIndex);
			}
			if (transition.thresholdSize > 1) {
				int midIndex = (transition.thresholdStartIndex + transition.thresholdEndIndex) / 2;
				for (int i = midIndex, j = midIndex; i <= transition.thresholdEndIndex && j >= transition.thresholdStartIndex; j--) {
					Double aggregatedValue = recordGroup.get(i);
					if (aggregatedValue != null) {
						thresholdExtremum = aggregatedValue;
						break;
					}
					aggregatedValue = recordGroup.get(j);
					if (aggregatedValue != null) {
						thresholdExtremum = aggregatedValue;
						break;
					}
				}
			}
			else {
				thresholdExtremum = recordGroup.get(transition.thresholdStartIndex);
			}
			if (transition.recoveryStartIndex > 0) {
				if (transition.recoverySize > 1) {
					int midIndex = (transition.recoveryStartIndex + transition.recoveryEndIndex) / 2;
					for (int i = midIndex, j = midIndex; i <= transition.recoveryEndIndex && j >= transition.recoveryStartIndex; j--) {
						Double aggregatedValue = recordGroup.get(i);
						if (aggregatedValue != null) {
							recoveryExtremum = aggregatedValue;
							break;
						}
						aggregatedValue = recordGroup.get(j);
						if (aggregatedValue != null) {
							recoveryExtremum = aggregatedValue;
							break;
						}
					}
				}
				else {
					recoveryExtremum = recordGroup.get(transition.recoveryStartIndex);
				}
			}
		}
		else if (leveling == LevelingTypes.AVG) {
			double value = 0.;
			int skipCount = 0;
			for (int j = transition.referenceStartIndex; j < transition.referenceEndIndex + 1; j++) {
				Double aggregatedValue = recordGroup.get(j);
				if (aggregatedValue != null)
					value += (aggregatedValue - value) / (j - transition.referenceStartIndex + 1);
				else
					skipCount++;
			}
			referenceExtremum = value / (transition.referenceSize - skipCount);
			value = 0.;
			skipCount = 0;
			// one additional time step before and after in order to cope with potential measurement latencies
			for (int j = transition.thresholdStartIndex - 1; j < transition.thresholdEndIndex + 1 + 1; j++) {
				Double aggregatedValue = recordGroup.get(j);
				if (aggregatedValue != null)
					value += (aggregatedValue - value) / (j - transition.thresholdStartIndex + 1);
				else
					skipCount++;
			}
			thresholdExtremum = value / (transition.thresholdSize - skipCount);
			if (transition.recoveryStartIndex > 0) {
				value = 0.;
				skipCount = 0;
				for (int j = transition.recoveryStartIndex; j < transition.recoveryEndIndex + 1; j++) {
					Double aggregatedValue = recordGroup.get(j);
					if (aggregatedValue != null)
						value += (aggregatedValue - value) / (j - transition.recoveryStartIndex + 1);
					else
						skipCount++;
				}
				recoveryExtremum = value / (transition.recoverySize - skipCount);
			}
		}
		else if (leveling == LevelingTypes.MINMAX) {
			if (isPositiveDirection) {
				referenceExtremum = Double.MAX_VALUE;
				for (int j = transition.referenceStartIndex; j < transition.referenceEndIndex + 1; j++) {
					Double aggregatedValue = recordGroup.get(j);
					if (aggregatedValue != null && aggregatedValue < referenceExtremum) referenceExtremum = aggregatedValue;
				}
				thresholdExtremum = -Double.MAX_VALUE;
				// one additional time step before and after in order to cope with potential measurement latencies
				for (int j = transition.thresholdStartIndex - 1; j < transition.thresholdEndIndex + 1 + 1; j++) {
					Double aggregatedValue = recordGroup.get(j);
					if (aggregatedValue != null && aggregatedValue > thresholdExtremum) thresholdExtremum = aggregatedValue;
				}
				if (transition.recoveryStartIndex > 0) {
					recoveryExtremum = Double.MAX_VALUE;
					for (int j = transition.recoveryStartIndex; j < transition.recoveryEndIndex + 1; j++) {
						Double aggregatedValue = recordGroup.get(j);
						if (aggregatedValue != null && aggregatedValue < recoveryExtremum) recoveryExtremum = aggregatedValue;
					}
				}
			}
			else {
				referenceExtremum = -Double.MAX_VALUE;
				for (int j = transition.referenceStartIndex; j < transition.referenceEndIndex + 1; j++) {
					Double aggregatedValue = recordGroup.get(j);
					if (aggregatedValue != null && aggregatedValue > referenceExtremum) referenceExtremum = aggregatedValue;
				}
				thresholdExtremum = Double.MAX_VALUE;
				// one additional time step before and after in order to cope with potential measurement latencies
				for (int j = transition.thresholdStartIndex - 1; j < transition.thresholdEndIndex + 1 + 1; j++) {
					Double aggregatedValue = recordGroup.get(j);
					if (aggregatedValue != null && aggregatedValue < thresholdExtremum) thresholdExtremum = aggregatedValue;
				}
				if (transition.recoveryStartIndex > 0) {
					recoveryExtremum = -Double.MAX_VALUE;
					for (int j = transition.recoveryStartIndex; j < transition.recoveryEndIndex + 1; j++) {
						Double aggregatedValue = recordGroup.get(j);
						if (aggregatedValue != null && aggregatedValue > recoveryExtremum) recoveryExtremum = aggregatedValue;
					}
				}
			}
		}
		else if (leveling == LevelingTypes.SMOOTH_MINMAX) {
			List<Double> values = new ArrayList<Double>();
			for (int j = transition.referenceStartIndex; j < transition.referenceEndIndex + 1; j++) {
				Double aggregatedValue = recordGroup.get(j);
				if (aggregatedValue != null) values.add(aggregatedValue);
			}
			Quantile quantile = new Quantile(values, EnumSet.noneOf(Fixings.class));
			referenceExtremum = quantile.getQuantile(!isPositiveDirection ? 1. - this.settings.getMinmaxQuantileDistance() : this.settings.getMinmaxQuantileDistance());
			values = new ArrayList<Double>();
			// one additional time step before and after in order to cope with potential measurement latencies
			for (int j = transition.thresholdStartIndex - 1; j < transition.thresholdEndIndex + 1 + 1; j++) {
				Double aggregatedValue = recordGroup.get(j);
				if (aggregatedValue != null) values.add(aggregatedValue);
			}
			quantile = new Quantile(values, EnumSet.noneOf(Fixings.class));
			thresholdExtremum = quantile.getQuantile(isPositiveDirection ? 1. - this.settings.getMinmaxQuantileDistance() : this.settings.getMinmaxQuantileDistance());
			if (transition.recoveryStartIndex > 0) {
				values = new ArrayList<Double>();
				for (int j = transition.recoveryStartIndex; j < transition.recoveryEndIndex + 1; j++) {
					Double aggregatedValue = recordGroup.get(j);
					if (aggregatedValue != null) values.add(aggregatedValue);
				}
				quantile = new Quantile(values, EnumSet.noneOf(Fixings.class));
				recoveryExtremum = quantile.getQuantile(!isPositiveDirection ? 1. - this.settings.getMinmaxQuantileDistance() : this.settings.getMinmaxQuantileDistance());
			}
		}
		else {
			throw new UnsupportedOperationException();
		}

		if (transition.isSlope() || transitionCalculus.getDeltaBasis() == null || transitionCalculus.getDeltaBasis() == DeltaBasisTypes.REFERENCE)
			deltaValue = thresholdExtremum - referenceExtremum;
		else if (transitionCalculus.getDeltaBasis() == DeltaBasisTypes.RECOVERY)
			deltaValue = thresholdExtremum - recoveryExtremum;
		else if (transitionCalculus.getDeltaBasis() == DeltaBasisTypes.BOTH_AVG)
			deltaValue = thresholdExtremum - (referenceExtremum + recoveryExtremum) / 2.;
		else if (transitionCalculus.getDeltaBasis() == DeltaBasisTypes.INNER)
			deltaValue = isPositiveDirection ? thresholdExtremum - Math.max(referenceExtremum, recoveryExtremum) : thresholdExtremum - Math.min(referenceExtremum, recoveryExtremum);
		else if (transitionCalculus.getDeltaBasis() == DeltaBasisTypes.OUTER)
			deltaValue = isPositiveDirection ? thresholdExtremum - Math.min(referenceExtremum, recoveryExtremum) : thresholdExtremum - Math.max(referenceExtremum, recoveryExtremum);
		else
			throw new UnsupportedOperationException();

		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%s %s %s referenceExtremum=%f  thresholdExtremum=%f  recoveryExtremum=%f  deltaValue=%f  @ isBasedOnRecovery=%s" //$NON-NLS-1$
				, this.getName(), recordGroup.getComment(), leveling.value(), referenceExtremum, thresholdExtremum, recoveryExtremum, deltaValue, transitionCalculus.getDeltaBasis()));
		return deltaValue;
	}

	/**
	 * figure out the result value and add result value.
	 * the timeSum and timeStep value has the unit seconds with 3 decimal places. 
	 * @param transition
	 */
	private void addFigure(Transition transition) {
		TransitionFigureType transitionFigure = this.settlement.getEvaluation().getTransitionFigure();
		log.log(Level.FINE, GDE.STRING_GREATER, transitionFigure);
		final int reverseTranslatedResult;
		if (transitionFigure.getFigureType() == FigureTypes.COUNT) {
			reverseTranslatedResult = transition.thresholdSize * 1000; // all internal values are multiplied by 1000
		}
		else if (transitionFigure.getFigureType() == FigureTypes.TIME_SUM_SEC) {
			reverseTranslatedResult = (int) (this.parent.getTime_ms(transition.thresholdEndIndex + 1) - this.parent.getTime_ms(transition.thresholdStartIndex));
		}
		else if (transitionFigure.getFigureType() == FigureTypes.TIME_STEP_SEC) {
			reverseTranslatedResult = (int) this.parent.getTime_ms(transition.thresholdStartIndex);
		}
		else {
			reverseTranslatedResult = 0;
			throw new UnsupportedOperationException();
		}
		// add to settlement record  
		addNullableRaw(reverseTranslatedResult, (long) this.parent.getTime_ms(transition.thresholdStartIndex) * 10);
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, String.format("%s: timeStamp_ms=%d  reverseTranslatedResult=%d  figureType=%s", this.getName(), (int) this.parent.getTime_ms(transition.thresholdEndIndex + 1) //$NON-NLS-1$
					, reverseTranslatedResult, transitionFigure.getFigureType()));
	}

	/**
	 * walk forward from the time step when the trigger has fired and amount the extremum values for the calculation.
	 * add single result value.
	 * @param transition
	 */
	private void addAmount(Transition transition) {
		TransitionAmountType transitionAmount = this.settlement.getEvaluation().getTransitionAmount();
		log.log(Level.OFF, GDE.STRING_GREATER, transitionAmount);
		final Record record = this.parent.get(this.parent.recordNames[transitionAmount.getRefOrdinal()]);
		log.log(Level.FINE, record.getName() + " values   " + record.subList(transition.referenceStartIndex, transition.thresholdEndIndex + 1)); //$NON-NLS-1$
		final int reverseTranslatedResult;
		if (transitionAmount.getAmountType() == AmountTypes.MIN) {
			double min = Double.MAX_VALUE;
			for (int j = transition.thresholdStartIndex; j < transition.thresholdEndIndex + 1; j++)
				if (record.realRealGet(j) != null)
					min = Math.min(min, transitionAmount.isUnsigned() ? Math.abs(this.device.translateValue(record, record.realRealGet(j))) : this.device.translateValue(record, record.realRealGet(j)));
			reverseTranslatedResult = (int) reverseTranslateValue(min);
		}
		else if (transitionAmount.getAmountType() == AmountTypes.MAX) {
			double max = transitionAmount.isUnsigned() ? 0. : -Double.MAX_VALUE;
			for (int j = transition.thresholdStartIndex; j < transition.thresholdEndIndex + 1; j++)
				if (record.realRealGet(j) != null)
					max = Math.max(max, transitionAmount.isUnsigned() ? Math.abs(this.device.translateValue(record, record.realRealGet(j))) : this.device.translateValue(record, record.realRealGet(j)));
			reverseTranslatedResult = (int) reverseTranslateValue(max);
		}
		else if (transitionAmount.getAmountType() == AmountTypes.AVG) {
			double avg = 0., value = 0.;
			int skipCount = 0;
			for (int j = transition.thresholdStartIndex; j < transition.thresholdEndIndex + 1; j++)
				if (record.realRealGet(j) != null) {
					value = transitionAmount.isUnsigned() ? Math.abs(this.device.translateValue(record, record.realRealGet(j))) : this.device.translateValue(record, record.realRealGet(j));
					double deltaAvg = value - avg;
					avg += deltaAvg / (j - transition.thresholdStartIndex - skipCount + 1);
				}
				else
					skipCount++;
			reverseTranslatedResult = (int) reverseTranslateValue(avg);
		}
		else if (transitionAmount.getAmountType() == AmountTypes.SIGMA) {
			// https://en.wikipedia.org/wiki/Standard_deviation#Rapid_calculation_methods
			double avg = 0., q = 0., value = 0.;
			int skipCount = 0;
			for (int j = transition.thresholdStartIndex; j < transition.thresholdEndIndex + 1; j++)
				if (record.realRealGet(j) != null) {
					value = transitionAmount.isUnsigned() ? Math.abs(this.device.translateValue(record, record.realRealGet(j))) : this.device.translateValue(record, record.realRealGet(j));
					double deltaAvg = value - avg;
					avg += deltaAvg / (j - transition.thresholdStartIndex - skipCount + 1);
					q += deltaAvg * (value - avg);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("value=%f  deltaAvg=%f  q=%f", value, deltaAvg, q)); //$NON-NLS-1$
				}
				else
					skipCount++;
			reverseTranslatedResult = (int) reverseTranslateValue(Math.sqrt(q / (transition.thresholdSize - skipCount - 1)));
		}
		else if (transitionAmount.getAmountType() == AmountTypes.SUM) {
			double sum = 0.;
			for (int j = transition.thresholdStartIndex; j < transition.thresholdEndIndex + 1; j++)
				sum += transitionAmount.isUnsigned() ? Math.abs(this.device.translateValue(record, record.realRealGet(j))) : this.device.translateValue(record, record.realRealGet(j));
			reverseTranslatedResult = (int) reverseTranslateValue(sum);
		}
		else {
			reverseTranslatedResult = 0;
			throw new UnsupportedOperationException();
		}
		// add to settlement record  
		addNullableRaw(reverseTranslatedResult, (long) this.parent.getTime_ms(transition.thresholdStartIndex) * 10);
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, String.format("%s: timeStamp_ms=%d  reverseTranslatedResult=%d  amountType=%s", this.getName(), (int) this.parent.getTime_ms(transition.thresholdEndIndex + 1) //$NON-NLS-1$
					, reverseTranslatedResult, transitionAmount.getAmountType()));
	}

	/**
	 * walk forward from the time step when the trigger has fired and collect the extremum values for the calculation.
	 * add single result value.
	 * the result value is multiplied by 1000. 
	 * @param transition
	 */
	private void addCalculus(Transition transition) {
		TransitionCalculusType calculus = this.settlement.getEvaluation().getTransitionCalculus();
		log.log(Level.FINEST, GDE.STRING_GREATER, calculus);
		final ChannelType logChannel = this.device.getDeviceConfiguration().getChannel(this.logChannelNumber);
		final RecordGroup recordGroup = new RecordGroup(this.device, this.parent, logChannel.getReferenceGroupById(calculus.getReferenceGroupId()));
		final int reverseTranslatedResult;
		if (calculus.getCalculusType() == CalculusTypes.RATIO || calculus.getCalculusType() == CalculusTypes.RATIO_PERMILLE) {
			final double denominator = calculateLevelDelta(calculus.getReferenceGroupId(), calculus.getLeveling(), transition);
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, recordGroup.getComment() + " denominator " + denominator + recordGroup.getSubGrouped(transition.referenceStartIndex, transition.thresholdEndIndex + 1)); //$NON-NLS-1$
			final RecordGroup divisorRecordGroup = new RecordGroup(this.device, this.parent, logChannel.getReferenceGroupById(calculus.getReferenceGroupIdDivisor()));
			final double divisor = calculateLevelDelta(calculus.getReferenceGroupIdDivisor(), calculus.getDivisorLeveling(), transition);
			if (calculus.getCalculusType() == CalculusTypes.RATIO) {
				reverseTranslatedResult = calculus.isUnsigned() ? (int) Math.abs(1000. * denominator / divisor) : (int) (1000. * denominator / divisor); // all internal values are multiplied by 1000
			}
			else {
				reverseTranslatedResult = calculus.isUnsigned() ? (int) Math.abs(1000000. * denominator / divisor) : (int) (1000000. * denominator / divisor); // all internal values are multiplied by 1000, ratio in permille
			}
		}
		else {
			reverseTranslatedResult = 0;
			throw new UnsupportedOperationException();
		}
		// add to settlement record  
		addNullableRaw(reverseTranslatedResult, (long) this.parent.getTime_ms(transition.thresholdStartIndex) * 10);
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, String.format("%s: timeStamp_ms=%d  reverseTranslatedResult=%d  calcType=%s", this.getName(), (int) this.parent.getTime_ms(transition.thresholdEndIndex + 1) //$NON-NLS-1$
					, reverseTranslatedResult, calculus.getCalculusType()));
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
	@Deprecated // use elaborated add methods for settlements 
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
			throw new UnsupportedOperationException();
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
			throw new UnsupportedOperationException();
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
			throw new UnsupportedOperationException();
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
	@Deprecated
	public synchronized int size() {
		return super.size(); // zoom and scope not supported
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
	@Deprecated
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
			log.log(Level.WARNING, String.format("%s - %20s: size = %d - indesx = %d", this.parent.name, this.name, this.size(), index)); //$NON-NLS-1$
			return super.size() != 0 ? super.get(index - 1) : 0;
		}
	}

	/**
	 * @return the parent
	 */
	public RecordSet getParent() {
		return this.parent;
	}

	//	/** 
	//	 * return the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by the start date time of this record (set)
	//	 * @return time stamp of the date and time when the record starts
	//	 */
	//	public long getStartTimeStamp() {
	//		return this.timeStep_ms == null ? this.parent.timeStep_ms.getStartTimeStamp() : this.timeStep_ms.getStartTimeStamp();
	//	}
	//
	//	/** 
	//	 * query time step time in mills seconds at index
	//	 * @return time step in msec
	//	 */
	//	public double getTime_ms(int index) {
	//		return this.timeStep_ms == null ? this.parent.timeStep_ms.getTime_ms(index) : this.timeStep_ms.getTime_ms(index);
	//	}
	//
	//	/** 
	//	 * query time step time in mills seconds at index
	//	 * @return time step in msec
	//	 */
	//	public double getLastTime_ms() {
	//		return this.timeStep_ms == null ? this.parent.timeStep_ms.lastElement() / 10.0 : this.timeStep_ms.lastElement() / 10.0;
	//	}
	//
	//	/** 
	//	 * query time step in mills seconds, this property is hold local to be independent (compare window)
	//	 * @return time step in msec
	//	 */
	//	public double getAverageTimeStep_ms() {
	//		return this.timeStep_ms == null ? this.parent.getAverageTimeStep_ms() : this.timeStep_ms.getAverageTimeStep_ms();
	//	}
	//
	//	/**
	//	 * set the time step in milli seconds, this property is hold local to be independent
	//	 * @param timeStep_ms the timeStep_ms to set
	//	 */
	//	void setTimeStep_ms(double newTimeStep_ms) {
	//		this.timeStep_ms = new TimeSteps(newTimeStep_ms, HistoSettlement.initialRecordCapacity);
	//	}
	//
	//	/**
	//	 * @return the maximum time of this record, which should correspondence to the last entry in timeSteps
	//	 */
	//	public double getMaxTime_ms() {
	//		return this.timeStep_ms == null ? this.parent.getMaxTime_ms() : this.timeStep_ms.isConstant ? this.timeStep_ms.getMaxTime_ms() * (this.elementCount - 1) : this.timeStep_ms.getMaxTime_ms();
	//	}
	//
	//	/**
	//	 * Find the indexes in this time vector where the given time value is placed
	//	 * In case of the given time in in between two available measurement points both bounding indexes are returned, 
	//	 * only in case where the given time matches an existing entry both indexes are equal.
	//	 * In cases where the returned indexes are not equal the related point x/y has to be interpolated.
	//	 * @param time_ms
	//	 * @return two index values around the given time 
	//	 */
	//	public int[] findBoundingIndexes(double time_ms) {
	//		int[] indexs = this.timeStep_ms == null ? this.parent.timeStep_ms.findBoundingIndexes(time_ms) : this.timeStep_ms.findBoundingIndexes(time_ms);
	//		if (this.elementCount > 0) {
	//			indexs[0] = indexs[0] > this.elementCount - 1 ? this.elementCount - 1 : indexs[0];
	//			indexs[1] = indexs[1] > this.elementCount - 1 ? this.elementCount - 1 : indexs[1];
	//		}
	//		return indexs;
	//	}
	//
	//	/**
	//	 * find the index closest to given time in msec
	//	 * @param time_ms
	//	 * @return index nearest to given time
	//	 */
	//	public int findBestIndex(double time_ms) {
	//		int index = this.timeStep_ms == null ? this.parent.timeStep_ms.findBestIndex(time_ms) : this.timeStep_ms.findBestIndex(time_ms);
	//		return index > this.elementCount - 1 ? this.elementCount - 1 : index;
	//	}
	//
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
	public int getTransitionCounter() {
		return this.transitionCounter;
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

		//		if (this.getParent().getChannelConfigNumber() == 3 && (this.ordinal == 1 || this.ordinal == 2)) { // 1=GPS-longitude 2=GPS-latitude
		//			// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		//			int grad = ((int) (value / 1000));
		//			double minuten = (value - (grad * 1000.0)) / 10.0;
		//			newValue = grad + minuten / 60.0;
		//		}
		//		else {
		newValue = (value - reduction) * factor + offset;

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
		return this.settlement;
	}

}
