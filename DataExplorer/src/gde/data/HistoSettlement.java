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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.device.AmountTypes;
import gde.device.CalculusTypes;
import gde.device.ChannelPropertyType;
import gde.device.ChannelPropertyTypes;
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
import gde.device.SettlementType;
import gde.device.TransitionAmountType;
import gde.device.TransitionCalculusType;
import gde.device.TransitionFigureType;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.SingleResponseRegression;
import gde.utils.SingleResponseRegression.RegressionType;
import gde.utils.Spot;
import gde.utils.UniversalQuantile;

/**
 * holds settlement points of a line or curve calculated from measurements.
 * applicable for settlements with an evaluation rule.
 * does not support settlement display or table view.
 * similar to record class except: clone, serialization, zoom / scope, triggers, syncWithRecords, setName, GPS-longitude, GPS-latitude.
 * @author Thomas Eickert
 */
public class HistoSettlement extends Vector<Integer> {
	final static String									$CLASS_NAME								= HistoSettlement.class.getName();
	final static long										serialVersionUID					= 6130190003229390899L;
	final static Logger									log												= Logger.getLogger($CLASS_NAME);

	/**
	 * we allow 1 lower and 1 upper outlier for a log with 740 measurements
	 */
	public final static double					outlierSigmaDefault				= 3.;
	/**
	 * Specifies the outlier distance limit ODL from the tolerance interval (<em>ODL = &rho; * TI with &rho; > 0</em>).<br>
	 * Tolerance interval: <em>TI = &plusmn; z * &sigma; with z >= 0</em><br>
	 * Outliers are identified only if they lie beyond this limit.
	 */
	public final static double					outlierRangeFactorDefault	= 2.;

	private final static int						initialRecordCapacity			= 22;

	private final IDevice								device										= DataExplorer.getInstance().getActiveDevice();
	private final Settings							settings									= Settings.getInstance();

	private final SettlementType				settlement;

	private final RecordSet							parent;
	private final int										logChannelNumber;
	String															name;																																			// measurement name Höhe

	List<PropertyType>									properties								= new ArrayList<PropertyType>();								// offset, factor, reduction, ...
	private int													transitionCounter					= 0;
	private UniversalQuantile<Integer>	quantile									= null;

	/**
	 * @author Thomas Eickert
	 * performs the aggregation of translated record values.
	 * The aggregation is based on the reference rule.
	 * null support was not tested up to now.
	 */
	private class RecordGroup {

		private ReferenceGroupType	referenceGroupType;
		private Record[]						records;

		public RecordGroup(ReferenceGroupType referenceGroupType) {
			this.referenceGroupType = referenceGroupType;
			this.records = new Record[referenceGroupType.getMeasurementMapping().size() + referenceGroupType.getSettlementMapping().size()];

			int i = 0;
			for (MeasurementMappingType measurementMappingType : referenceGroupType.getMeasurementMapping()) {
				this.records[i] = HistoSettlement.this.parent.get(HistoSettlement.this.parent.recordNames[measurementMappingType.getMeasurementOrdinal()]);
				i++;
			}
			if (!referenceGroupType.getSettlementMapping().isEmpty()) throw new UnsupportedOperationException("settlements based on settlements not supported");
		}

		/**
		 * @return true if at least one of the records contains reasonable data which can be displayed
		 */
		public boolean hasReasonableData() {
			for (Record record : this.records) {
				if (record.hasReasonableData()) {
					return true;
				}
			}
			return false;
		}

		/**
		 * @return the aggregated translated maximum value
		 */
		public double getRealMax() {
			double result = 0;
			for (int i = 0; i < this.records.length; i++) {
				Record record = this.records[i];
				final double translatedValue = HistoSettlement.this.device.translateValue(record, record.getRealMaxValue() / 1000.);
				result = calculateAggregate(result, i, translatedValue);
			}
			return result;
		}

		/**
		 * @return the aggregated translated minimum value
		 */
		public double getRealMin() {
			double result = 0;
			for (int i = 0; i < this.records.length; i++) {
				Record record = this.records[i];
				final double translatedValue = HistoSettlement.this.device.translateValue(record, record.getRealMinValue() / 1000.);
				result = calculateAggregate(result, i, translatedValue);
			}
			return result;
		}

		/**
		 * @param index
		 * @return the aggregated translated value at this real index position (irrespective of zoom / scope)
		 */
		public Double getReal(int index) {
			Double result = 0.;
			for (int i = 0; i < this.records.length; i++) {
				Record record = this.records[i];
				if (record.elementAt(index) == null) {
					result = null;
					break;
				}
				else {
					final double translatedValue = HistoSettlement.this.device.translateValue(record, record.elementAt(index) / 1000.);
					result = calculateAggregate(result, i, translatedValue);
				}
			}
			return result;
		}

		/**
		 * @param recurrentResult from the previous aggregation step
		 * @param aggregationStepIndex is the 0-based number of the current aggregation step
		 * @param translatedValue
		 * @return the recurrentResult aggregated based on the translated value
		 */
		private double calculateAggregate(double recurrentResult, int aggregationStepIndex, double translatedValue) {
			if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.AVG) {
				recurrentResult += (translatedValue - recurrentResult) / (aggregationStepIndex + 1);
			}
			else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.MAX) {
				if (aggregationStepIndex != 0)
					recurrentResult = Math.max(recurrentResult, translatedValue);
				else
					recurrentResult = translatedValue;
			}
			else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.MIN) {
				if (aggregationStepIndex == 0)
					recurrentResult = translatedValue;
				else
					recurrentResult = Math.max(recurrentResult, translatedValue);
			}
			else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.PRODUCT) {
				if (aggregationStepIndex == 0)
					recurrentResult = translatedValue;
				else
					recurrentResult *= translatedValue;
			}
			else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.QUOTIENT) {
				if (aggregationStepIndex == 0)
					recurrentResult = translatedValue;
				else
					recurrentResult /= translatedValue;
			}
			else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.SPREAD) {
				if (aggregationStepIndex == 0)
					recurrentResult = translatedValue;
				else
					recurrentResult -= translatedValue;
			}
			else if (this.referenceGroupType.getReferenceRule() == ReferenceRuleTypes.SUM) {
				recurrentResult += translatedValue;
			}
			else
				throw new UnsupportedOperationException();
			return recurrentResult;
		}

		/**
		 * @param fromIndex
		 * @param toIndex
		 * @return the portion of the timestamps_ms and aggregated translated values between fromIndex, inclusive, and toIndex, exclusive. (If fromIndex and toIndex are equal, the returned List is empty.)
		 */
		public List<Spot<Double>> getSubPoints(int fromIndex, int toIndex) {
			int recordSize = toIndex - fromIndex;
			List<Spot<Double>> result = new ArrayList<>(recordSize);
			for (int i = fromIndex; i < toIndex; i++) {
				if (getReal(i) != null) result.add(new Spot<Double>(HistoSettlement.this.parent.timeStep_ms.getTime_ms(i), getReal(i)));
			}
			log.log(Level.FINER, "", Arrays.toString(result.toArray()));
			return result;
		}

		public String getComment() {
			return this.referenceGroupType.getComment();
		}
	}

	/**
	 * creates a vector to hold data points.
	 * @param newSettlement
	 * @param parent
	 * @param logChannelNumber
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
	 * @param recordGroup holds the measurement points
	 * @param leveling rule for determining the level value from the device configuration
	 * @param transition holds the transition properties which are used to access the measurement data
	 * @return peak spread value as translatedValue
	 */
	private double calculateLevelDelta(RecordGroup recordGroup, LevelingTypes leveling, Transition transition) {
		double deltaValue = 0.;
		TransitionCalculusType transitionCalculus = this.settlement.getEvaluation().getTransitionCalculus();

		// determine the direction of the peak or pulse or slope
		final boolean isPositiveDirection;
		{
			if (transition.isSlope()) {
				int fromIndex = transition.referenceStartIndex;
				int toIndex = transition.thresholdEndIndex + 1;
				SingleResponseRegression<Double> regression = new SingleResponseRegression<>(recordGroup.getSubPoints(fromIndex, toIndex), RegressionType.LINEAR);
				isPositiveDirection = regression.getSlope() > 0;
			}
			else {
				int fromIndex = transition.referenceStartIndex;
				int toIndex = transition.recoveryEndIndex + 1;
				SingleResponseRegression<Double> regression = new SingleResponseRegression<>(recordGroup.getSubPoints(fromIndex, toIndex), RegressionType.QUADRATIC);
				isPositiveDirection = regression.getGamma() < 0;
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "direction: ", isPositiveDirection);
		}

		double referenceExtremum = 0.;
		double thresholdExtremum = 0.;
		double recoveryExtremum = 0.;
		if (leveling == LevelingTypes.FIRST) {
			for (int j = transition.referenceStartIndex; j < transition.referenceEndIndex + 1; j++) {
				Double aggregatedValue = recordGroup.getReal(j);
				if (aggregatedValue != null) {
					referenceExtremum = aggregatedValue;
					break;
				}
			}
			// one additional time step before and after in order to cope with potential measurement latencies
			for (int j = transition.thresholdStartIndex - 1; j < transition.thresholdEndIndex + 1 + 1; j++) {
				Double aggregatedValue = recordGroup.getReal(j);
				if (aggregatedValue != null) {
					thresholdExtremum = aggregatedValue;
					break;
				}
			}
			if (transition.recoveryStartIndex > 0) {
				for (int j = transition.recoveryStartIndex; j < transition.recoveryEndIndex + 1; j++) {
					Double aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null) {
						recoveryExtremum = aggregatedValue;
						break;
					}
				}
			}
		}
		else if (leveling == LevelingTypes.LAST) {
			for (int j = transition.referenceEndIndex; j >= transition.referenceStartIndex; j--) {
				Double aggregatedValue = recordGroup.getReal(j);
				if (aggregatedValue != null) {
					referenceExtremum = aggregatedValue;
					break;
				}
			}
			// one additional time step before and after in order to cope with potential measurement latencies
			for (int j = transition.thresholdEndIndex + 1; j >= transition.thresholdStartIndex - 1; j--) {
				Double aggregatedValue = recordGroup.getReal(j);
				if (aggregatedValue != null) {
					thresholdExtremum = aggregatedValue;
					break;
				}
			}
			if (transition.recoveryStartIndex > 0) {
				for (int j = transition.recoveryEndIndex; j >= transition.recoveryStartIndex; j--) {
					Double aggregatedValue = recordGroup.getReal(j);
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
					Double aggregatedValue = recordGroup.getReal(i);
					if (aggregatedValue != null) {
						referenceExtremum = aggregatedValue;
						break;
					}
					aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null) {
						referenceExtremum = aggregatedValue;
						break;
					}
				}
			}
			else {
				referenceExtremum = recordGroup.getReal(transition.referenceStartIndex);
			}
			if (transition.thresholdSize > 1) {
				int midIndex = (transition.thresholdStartIndex + transition.thresholdEndIndex) / 2;
				for (int i = midIndex, j = midIndex; i <= transition.thresholdEndIndex && j >= transition.thresholdStartIndex; j--) {
					Double aggregatedValue = recordGroup.getReal(i);
					if (aggregatedValue != null) {
						thresholdExtremum = aggregatedValue;
						break;
					}
					aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null) {
						thresholdExtremum = aggregatedValue;
						break;
					}
				}
			}
			else {
				thresholdExtremum = recordGroup.getReal(transition.thresholdStartIndex);
			}
			if (transition.recoveryStartIndex > 0) {
				if (transition.recoverySize > 1) {
					int midIndex = (transition.recoveryStartIndex + transition.recoveryEndIndex) / 2;
					for (int i = midIndex, j = midIndex; i <= transition.recoveryEndIndex && j >= transition.recoveryStartIndex; j--) {
						Double aggregatedValue = recordGroup.getReal(i);
						if (aggregatedValue != null) {
							recoveryExtremum = aggregatedValue;
							break;
						}
						aggregatedValue = recordGroup.getReal(j);
						if (aggregatedValue != null) {
							recoveryExtremum = aggregatedValue;
							break;
						}
					}
				}
				else {
					recoveryExtremum = recordGroup.getReal(transition.recoveryStartIndex);
				}
			}
		}
		else if (leveling == LevelingTypes.AVG) {
			double value = 0.;
			int skipCount = 0;
			for (int j = transition.referenceStartIndex; j < transition.referenceEndIndex + 1; j++) {
				Double aggregatedValue = recordGroup.getReal(j);
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
				Double aggregatedValue = recordGroup.getReal(j);
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
					Double aggregatedValue = recordGroup.getReal(j);
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
					Double aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null && aggregatedValue < referenceExtremum) referenceExtremum = aggregatedValue;
				}
				thresholdExtremum = -Double.MAX_VALUE;
				// one additional time step before and after in order to cope with potential measurement latencies
				for (int j = transition.thresholdStartIndex - 1; j < transition.thresholdEndIndex + 1 + 1; j++) {
					Double aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null && aggregatedValue > thresholdExtremum) thresholdExtremum = aggregatedValue;
				}
				if (transition.recoveryStartIndex > 0) {
					recoveryExtremum = Double.MAX_VALUE;
					for (int j = transition.recoveryStartIndex; j < transition.recoveryEndIndex + 1; j++) {
						Double aggregatedValue = recordGroup.getReal(j);
						if (aggregatedValue != null && aggregatedValue < recoveryExtremum) recoveryExtremum = aggregatedValue;
					}
				}
			}
			else {
				referenceExtremum = -Double.MAX_VALUE;
				for (int j = transition.referenceStartIndex; j < transition.referenceEndIndex + 1; j++) {
					Double aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null && aggregatedValue > referenceExtremum) referenceExtremum = aggregatedValue;
				}
				thresholdExtremum = Double.MAX_VALUE;
				// one additional time step before and after in order to cope with potential measurement latencies
				for (int j = transition.thresholdStartIndex - 1; j < transition.thresholdEndIndex + 1 + 1; j++) {
					Double aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null && aggregatedValue < thresholdExtremum) thresholdExtremum = aggregatedValue;
				}
				if (transition.recoveryStartIndex > 0) {
					recoveryExtremum = -Double.MAX_VALUE;
					for (int j = transition.recoveryStartIndex; j < transition.recoveryEndIndex + 1; j++) {
						Double aggregatedValue = recordGroup.getReal(j);
						if (aggregatedValue != null && aggregatedValue > recoveryExtremum) recoveryExtremum = aggregatedValue;
					}
				}
			}
		}
		else if (leveling == LevelingTypes.SMOOTH_MINMAX) {
			final ChannelPropertyType channelProperty = this.device.getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.OUTLIER_SIGMA);
			final double sigmaFactor = channelProperty.getValue() != null && !channelProperty.getValue().isEmpty() ? Double.parseDouble(channelProperty.getValue()) : HistoSettlement.outlierSigmaDefault;
			final ChannelPropertyType channelProperty2 = this.device.getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.OUTLIER_RANGE_FACTOR);
			final double outlierFactor = channelProperty2.getValue() != null && !channelProperty2.getValue().isEmpty() ? Double.parseDouble(channelProperty2.getValue())
					: HistoSettlement.outlierRangeFactorDefault;
			final double probabilityCutPoint = !isPositiveDirection ? 1. - this.settings.getMinmaxQuantileDistance() : this.settings.getMinmaxQuantileDistance();
			{
				List<Double> values = new ArrayList<Double>();
				for (int j = transition.referenceStartIndex; j < transition.referenceEndIndex + 1; j++) {
					Double aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null) values.add(aggregatedValue);
				}
				UniversalQuantile<Double> tmpQuantile = new UniversalQuantile<>(values, true, sigmaFactor, outlierFactor);
				referenceExtremum = tmpQuantile.getQuantile(probabilityCutPoint);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "reference " + Arrays.toString(values.toArray()));
			}
			{
				List<Double> values = new ArrayList<Double>();
				// one additional time step before and after in order to cope with potential measurement latencies
				for (int j = transition.thresholdStartIndex - 1; j < transition.thresholdEndIndex + 1 + 1; j++) {
					Double aggregatedValue = recordGroup.getReal(j);
					if (aggregatedValue != null) values.add(aggregatedValue);
				}
				UniversalQuantile<Double> tmpQuantile = new UniversalQuantile<>(values, true, sigmaFactor, outlierFactor);
				thresholdExtremum = tmpQuantile.getQuantile(isPositiveDirection ? 1. - this.settings.getMinmaxQuantileDistance() : this.settings.getMinmaxQuantileDistance());
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "threshold " + Arrays.toString(values.toArray()));
			}
			{
				if (transition.recoveryStartIndex > 0) {
					List<Double> values = new ArrayList<Double>();
					for (int j = transition.recoveryStartIndex; j < transition.recoveryEndIndex + 1; j++) {
						Double aggregatedValue = recordGroup.getReal(j);
						if (aggregatedValue != null) values.add(aggregatedValue);
					}
					UniversalQuantile<Double> tmpQuantile = new UniversalQuantile<>(values, true, sigmaFactor, outlierFactor);
					recoveryExtremum = tmpQuantile.getQuantile(probabilityCutPoint);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "recovery " + Arrays.toString(values.toArray()));
				}
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

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("%s %s %s referenceExtremum=%f  thresholdExtremum=%f  recoveryExtremum=%f  deltaValue=%f  @ isBasedOnRecovery=%s" //$NON-NLS-1$
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
		add(reverseTranslatedResult);
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
		log.log(Level.FINE, GDE.STRING_GREATER, transitionAmount);
		final Record record = this.parent.get(this.parent.recordNames[transitionAmount.getRefOrdinal()]);
		log.log(Level.FINE, record.getName() + " values   " + record.subList(transition.referenceStartIndex, transition.thresholdEndIndex + 1)); //$NON-NLS-1$
		final int reverseTranslatedResult;
		if (transitionAmount.getAmountType() == AmountTypes.MIN) {
			double min = Double.MAX_VALUE;
			for (int j = transition.thresholdStartIndex; j < transition.thresholdEndIndex + 1; j++)
				if (record.elementAt(j) != null) min = Math.min(min,
						transitionAmount.isUnsigned() ? Math.abs(this.device.translateValue(record, record.elementAt(j) / 1000.)) : this.device.translateValue(record, record.elementAt(j) / 1000.));
			reverseTranslatedResult = (int) (reverseTranslateValue(min) * 1000.);
		}
		else if (transitionAmount.getAmountType() == AmountTypes.MAX) {
			double max = transitionAmount.isUnsigned() ? 0. : -Double.MAX_VALUE;
			for (int j = transition.thresholdStartIndex; j < transition.thresholdEndIndex + 1; j++)
				if (record.elementAt(j) != null) max = Math.max(max,
						transitionAmount.isUnsigned() ? Math.abs(this.device.translateValue(record, record.elementAt(j) / 1000.)) : this.device.translateValue(record, record.elementAt(j) / 1000.));
			reverseTranslatedResult = (int) (reverseTranslateValue(max) * 1000.);
		}
		else if (transitionAmount.getAmountType() == AmountTypes.AVG) {
			double avg = 0., value = 0.;
			int skipCount = 0;
			for (int j = transition.thresholdStartIndex; j < transition.thresholdEndIndex + 1; j++)
				if (record.elementAt(j) != null) {
					value = transitionAmount.isUnsigned() ? Math.abs(this.device.translateValue(record, record.elementAt(j) / 1000.)) : this.device.translateValue(record, record.elementAt(j) / 1000.);
					double deltaAvg = value - avg;
					avg += deltaAvg / (j - transition.thresholdStartIndex - skipCount + 1);
				}
				else
					skipCount++;
			reverseTranslatedResult = (int) (reverseTranslateValue(avg) * 1000.);
		}
		else if (transitionAmount.getAmountType() == AmountTypes.SIGMA) {
			// https://en.wikipedia.org/wiki/Standard_deviation#Rapid_calculation_methods
			double avg = 0., q = 0., value = 0.;
			int skipCount = 0;
			for (int j = transition.thresholdStartIndex; j < transition.thresholdEndIndex + 1; j++)
				if (record.elementAt(j) != null) {
					value = transitionAmount.isUnsigned() ? Math.abs(this.device.translateValue(record, record.elementAt(j) / 1000.)) : this.device.translateValue(record, record.elementAt(j) / 1000.);
					double deltaAvg = value - avg;
					avg += deltaAvg / (j - transition.thresholdStartIndex - skipCount + 1);
					q += deltaAvg * (value - avg);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("value=%f  deltaAvg=%f  q=%f", value, deltaAvg, q)); //$NON-NLS-1$
				}
				else
					skipCount++;
			reverseTranslatedResult = (int) (reverseTranslateValue(Math.sqrt(q / (transition.thresholdSize - skipCount - 1))) * 1000.);
		}
		else {
			reverseTranslatedResult = 0;
			throw new UnsupportedOperationException();
		}
		// add to settlement record
		add(reverseTranslatedResult);
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, String.format("%s: timeStamp_ms=%d  reverseTranslatedResult=%d  amountType=%s", this.getName(), (int) this.parent.getTime_ms(transition.thresholdEndIndex + 1) //$NON-NLS-1$
					, reverseTranslatedResult, transitionAmount.getAmountType()));
	}

	/**
	 * walk forward from the time step when the trigger has fired and collect the extremum values for the calculation.
	 * add single result value.
	 * @param transition
	 */
	private void addCalculus(Transition transition) {
		TransitionCalculusType calculus = this.settlement.getEvaluation().getTransitionCalculus();
		log.log(Level.FINEST, GDE.STRING_GREATER, calculus);
		final ChannelType logChannel = this.device.getDeviceConfiguration().getChannel(this.logChannelNumber);
		final RecordGroup recordGroup = new RecordGroup(logChannel.getReferenceGroupById(calculus.getReferenceGroupId()));
		if (recordGroup.hasReasonableData()) {
			final int reverseTranslatedResult;
			if (calculus.getCalculusType() == CalculusTypes.DELTA) {
				final double deltaValue = calculateLevelDelta(recordGroup, calculus.getLeveling(), transition);
				reverseTranslatedResult = (int) (reverseTranslateValue(calculus.isUnsigned() ? Math.abs(deltaValue) : deltaValue) * 1000.);
			}
			else if (calculus.getCalculusType() == CalculusTypes.DELTA_PERMILLE) {
				final double deltaValue = calculateLevelDelta(recordGroup, calculus.getLeveling(), transition);
				reverseTranslatedResult = (int) (reverseTranslateValue(calculus.isUnsigned() ? Math.abs(deltaValue) : deltaValue) * 1000. * 1000.);
			}
			else if (calculus.getCalculusType() == CalculusTypes.RELATIVE_DELTA_PERCENT) {
				final double relativeDeltaValue = calculateLevelDelta(recordGroup, calculus.getLeveling(), transition) / (recordGroup.getRealMax() - recordGroup.getRealMin());
				reverseTranslatedResult = (int) (calculus.isUnsigned() ? Math.abs(relativeDeltaValue) * 1000. * 100. : relativeDeltaValue * 1000. * 100.); // all internal values are multiplied by 1000
			}
			else if (calculus.getCalculusType() == CalculusTypes.RATIO || calculus.getCalculusType() == CalculusTypes.RATIO_PERMILLE) {
				final double denominator = calculateLevelDelta(recordGroup, calculus.getLeveling(), transition);
				final RecordGroup divisorRecordGroup = new RecordGroup(logChannel.getReferenceGroupById(calculus.getReferenceGroupIdDivisor()));
				if (!divisorRecordGroup.hasReasonableData()) {
					return;
				}

				final double divisor = calculateLevelDelta(divisorRecordGroup, calculus.getDivisorLeveling(), transition);
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, recordGroup.getComment() + " denominator " + denominator + " divisor " + divisor); //$NON-NLS-1$
				if (calculus.getCalculusType() == CalculusTypes.RATIO) {
					reverseTranslatedResult = (int) (calculus.isUnsigned() ? Math.abs(denominator / divisor * 1000.) : denominator / divisor * 1000.); // all internal values are multiplied by 1000
				}
				else {
					reverseTranslatedResult = (int) (calculus.isUnsigned() ? Math.abs(denominator / divisor * 1000. * 1000.) : denominator / divisor * 1000. * 1000.); // all internal values are multiplied by 1000
				}
			}
			else {
				reverseTranslatedResult = 0;
				throw new UnsupportedOperationException();
			}
			// add to settlement record --- no recordgroup zero ratios which often occur for discharge logs from UDP60
			boolean isNeglectableRatioValue = recordGroup.records.length > 1 && reverseTranslatedResult == 0.0
					&& (calculus.getCalculusType() == CalculusTypes.RATIO || calculus.getCalculusType() == CalculusTypes.RATIO_PERMILLE);
			if (!isNeglectableRatioValue) add(reverseTranslatedResult);
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, String.format("%s: timeStamp_ms=%d  reverseTranslatedResult=%d  calcType=%s", this.getName(), (int) this.parent.getTime_ms(transition.thresholdEndIndex + 1) //$NON-NLS-1$
						, reverseTranslatedResult, calculus.getCalculusType()));
		}
	}

	@Override
	@Deprecated // use elaborated add methods for settlements
	public synchronized Integer set(int index, Integer point) {
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
		else {
			// take default
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
		else {
			// take default
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
			// take default
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

	/**
	 * @return all the translated record values including nulls (do not consider zoom, scope, ...)
	 */
	public Vector<Double> getTranslatedValues() {
		Vector<Double> translatedValues = new Vector<>();
		for (Integer value : this) { // loops without calling the overridden getter
			if (value != null)
				translatedValues.add(this.translateValue(value / 1000.));
			else
				translatedValues.add(null);
		}
		return translatedValues;
	}

	public RecordSet getParent() {
		return this.parent;
	}

	public int getTransitionCounter() {
		return this.transitionCounter;
	}

	/**
	 * @return true if the record contained reasonable date which can be displayed
	 */
	public boolean hasReasonableData() {
		return this.realSize() > 0 && (getQuantile().getQuartile0() != getQuantile().getQuartile4() || translateValue(getQuantile().getQuartile4() / 1000.0) != 0.0);
	}

	/**
	 * function to translate settlement values to values represented.
	 * does not support settlements based on GPS-longitude or GPS-latitude.
	 * @return double of device dependent value
	 */
	private double translateValue(double value) {
		double newValue = (value - this.getReduction()) * this.getFactor() + this.getOffset();
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "for " + this.name + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to translate values represented into normalized settlement values.
	 * does not support settlements based on GPS-longitude or GPS-latitude.
	 * @return double of settlement dependent value
	 */
	public double reverseTranslateValue(double value) { // todo support settlements based on GPS-longitude or GPS-latitude with a base class common for Record, TrailRecord and Settlement
		double newValue = (value - this.getOffset()) / this.getFactor() + this.getReduction();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + this.name + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	public String getName() {
		return this.name;
	}

	public SettlementType getSettlement() {
		return this.settlement;
	}

	public UniversalQuantile<Integer> getQuantile() {
		if (this.quantile == null) {
			this.quantile = new UniversalQuantile<>(this, true);
		}
		return this.quantile;
	}

}
