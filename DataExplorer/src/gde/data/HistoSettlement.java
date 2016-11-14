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

import gde.GDE;
import gde.config.Settings;
import gde.device.CalculationType;
import gde.device.CalculationTypes;
import gde.device.DataTypes;
import gde.device.EvaluationType;
import gde.device.IDevice;
import gde.device.LevelingTypes;
import gde.device.ObjectFactory;
import gde.device.PropertyType;
import gde.device.SettlementType;
import gde.device.TransitionType;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.TimeLine;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * holds settlement points of a line or curve calculated from measurements.
 * applicable for settlements with an evaluation rule.
 * does not support settlement display or table view.
 * similar to record class except: clone, serialization, triggers, syncWithRecords, setName, GPS-longitude, GPS-latitude.
 * @author Thomas Eickert
 */
public class HistoSettlement extends Vector<Integer> { // todo maybe a better option is to create a common base class for Record, HistoSettlement and TrailRecord.
	// todo get rid of all the rubbish copied from the data class.
	final static String				$CLASS_NAME								= HistoSettlement.class.getName();
	final static long					serialVersionUID					= 6130190003229390899L;
	final static Logger				log												= Logger.getLogger($CLASS_NAME);

	protected final Settings	settings									= Settings.getInstance();

	// this variables are used to make a record selfcontained within compare set
	@Deprecated
	String										channelConfigKey;																							// used as channelConfigKey
	String										keyName;
	TimeSteps									timeStep_ms								= null;															// timeStep_ms for each measurement point in compare set, where time step of measurement points might be individual
	IDevice										device;
	SettlementType						settlement;
	int												ordinal;																											// ordinal is referencing the source position of the record relative to the initial
	// device measurement configuration and used to find specific properties

	HistoRecordSet						parent;
	String										name;																													// measurement name Höhe
	String										unit;																													// unit [m]
	String										symbol;																												// symbol h
	String										description								= GDE.STRING_BLANK;									// only set if copied into compare set
	boolean										isActive;

	boolean										isDisplayable;
	boolean										isVisible									= true;
	EvaluationType						statistics								= null;

	List<PropertyType>				properties								= new ArrayList<PropertyType>();								// offset, factor, reduction, ...
	boolean										isPositionLeft						= true;
	Color											color											= DataExplorer.COLOR_BLACK;
	int												lineWidth									= 1;
	int												lineStyle									= SWT.LINE_SOLID;
	boolean										isRoundOut								= false;
	boolean										isStartpointZero					= false;
	boolean										isStartEndDefined					= false;
	DecimalFormat							df;
	int												numberFormat							= -1;																// -1 = automatic, 0 = 0000, 1 = 000.0, 2 = 00.00
	int												maxValue									= 0;																// max value of the curve
	int												minValue									= 0;																// min value of the curve
	double										maxScaleValue							= this.maxValue;										// overwrite calculated boundaries
	double										minScaleValue							= this.minValue;
	DataType									dataType									= HistoSettlement.DataType.DEFAULT;

	// synchronize
	int												syncMaxValue							= 0;																// max value of the curve if synced
	int												syncMinValue							= 0;																// min value of the curve if synced

	// scope view
	int												scopeMin									= 0;																// min value of the curve within scope display area
	int												scopeMax									= 0;																// max value of the curve within scope display area

	// statistics
	int												avgValue									= Integer.MIN_VALUE;								// avarage value (avg = sum(xi)/n)
	int												sigmaValue								= Integer.MIN_VALUE;								// sigma value of data
	int												sumValue									= Integer.MIN_VALUE;								// sum value of data

	double										drawLimit_ms							= Integer.MAX_VALUE;								// above this limit the record will not be drawn (compareSet with different records)
	double										zoomTimeOffset						= 0;																// time where the zoom area begins
	int												zoomOffset								= 0;																// number of measurements point until zoom area begins approximation only
	double										drawTimeWidth							= 0;																// all or zoomed area time width
	double										tmpMaxZoomScaleValue			= this.maxScaleValue;
	double										tmpMinZoomScaleValue			= this.minScaleValue;
	double										maxZoomScaleValue					= this.maxScaleValue;
	double										minZoomScaleValue					= this.minScaleValue;
	int												numberScaleTicks					= 0;

	// display the record
	double										displayScaleFactorTime;
	double										displayScaleFactorValue;
	double										syncMasterFactor					= 1.0;															// synchronized scale and different measurement factors
	double										minDisplayValue;																							// min value in device units, correspond to draw area
	double										maxDisplayValue;																							// max value in device units, correspond to draw area

	// measurement
	boolean										isMeasurementMode					= false;
	boolean										isDeltaMeasurementMode		= false;

	public final static int		TYPE_AXIS_END_VALUES			= 0;																// defines axis end values types like isRoundout, isStartpointZero, isStartEndDefined
	public final static int		TYPE_AXIS_NUMBER_FORMAT		= 1;																// defines axis scale values format
	public final static int		TYPE_AXIS_SCALE_POSITION	= 2;																// defines axis scale position left or right

	public enum DataType { // some data types require in some situation special execution algorithm
		DEFAULT("default"), // all normal measurement values which do not require special handling
		GPS_LATITUDE("GPS latitude"), // GPS geo-coordinate require at least 6 decimal digits [°]
		GPS_LONGITUDE("GPS longitude"), // GPS geo-coordinate require at least 6 decimal digits [°]
		GPS_ALTITUDE("GPS altitude"), // GPS or absolute altitude required in some case for GPS related calculations like speed, distance, ...
		GPS_AZIMUTH("GPS azimuth"), // GPS azimuth, to be used for live display and positioning of icon if used
		SPEED("speed"), // speed, to be used for KMZ export with colors of specified velocity
		DATE_TIME("date time"), // special data type where no formatting or calculation can be executed, just display
		CURRENT("current"), // data type to unique identify current type, mainly used for smoothing current drops
		VOLTAGE("voltage"); // data type to unique identify voltage type, to smoothing reflex or pulsing voltage values

		private final String value;

		private DataType(String v) {
			this.value = v;
		}

		public String value() {
			return this.value;
		}

		public static DataType fromValue(String v) {
			for (DataType c : DataType.values()) {
				if (c.value.equals(v)) {
					return c;
				}
			}
			throw new IllegalArgumentException(v);
		}

		public static List<HistoSettlement.DataType> getAsList() {
			List<HistoSettlement.DataType> dataTypes = new ArrayList<HistoSettlement.DataType>();
			for (DataType type : DataType.values()) {
				dataTypes.add(type);
			}
			return dataTypes;
		}

		public static List<String> getValuesAsList() {
			List<String> dataTypeValues = new ArrayList<String>();
			for (DataType type : DataType.values()) {
				dataTypeValues.add(type.value());
			}
			return dataTypeValues;
		}
	};

	/**
	 * creates a vector to hold data points.
	 * @param newDevice
	 * @param newSettlement
	 * @param initialCapacity
	 */
	public HistoSettlement(IDevice newDevice, SettlementType newSettlement, HistoRecordSet parent, int initialCapacity) {
		super(initialCapacity);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, newSettlement.getName() + " Settlement(IDevice , SettlementType , int )"); //$NON-NLS-1$
		this.device = newDevice;
		this.settlement = newSettlement;
		this.name = newSettlement.getName();
		this.parent = parent;
		initializeProperties(this, newSettlement.getProperty());
		this.df = new DecimalFormat("0.0"); //$NON-NLS-1$
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
	 * Method to initialize color and scale position defaults
	 * @param recordOrdinal
	 */
	public void setColorDefaultsAndPosition(int recordOrdinal) {
		// set color defaults
		switch (recordOrdinal) {
		case 0: // erste Kurve
			this.color = SWTResourceManager.getColor(0, 0, 255); // (SWT.COLOR_BLUE));
			break;
		case 1: // zweite Kurve
			this.color = SWTResourceManager.getColor(0, 128, 0); // SWT.COLOR_DARK_GREEN));
			break;
		case 2: // dritte Kurve
			this.color = SWTResourceManager.getColor(128, 0, 0); // (SWT.COLOR_DARK_RED));
			break;
		case 3: // vierte Kurve
			this.color = SWTResourceManager.getColor(255, 0, 255); // (SWT.COLOR_MAGENTA));
			break;
		case 4: // fünfte Kurve
			this.color = SWTResourceManager.getColor(64, 0, 64); // (SWT.COLOR_CYAN));
			break;
		case 5: // sechste Kurve
			this.color = SWTResourceManager.getColor(0, 128, 128); // (SWT.COLOR_DARK_YELLOW));
			break;
		case 6: // Kurve
			this.color = SWTResourceManager.getColor(128, 128, 0);
			break;
		case 7: // Kurve
			this.color = SWTResourceManager.getColor(128, 0, 128);
			break;
		case 8: // Kurve
			this.color = SWTResourceManager.getColor(0, 128, 255);
			break;
		case 9: // Kurve
			this.color = SWTResourceManager.getColor(128, 255, 0);
			break;
		case 10: // Kurve
			this.color = SWTResourceManager.getColor(255, 0, 128);
			break;
		case 11: // Kurve
			this.color = SWTResourceManager.getColor(0, 64, 128);
			break;
		case 12: // Kurve
			this.color = SWTResourceManager.getColor(64, 128, 0);
			break;
		case 13: // Kurve
			this.color = SWTResourceManager.getColor(128, 0, 64);
			break;
		case 14: // Kurve
			this.color = SWTResourceManager.getColor(128, 64, 0);
			break;
		case 15: // Kurve
			this.color = SWTResourceManager.getColor(0, 128, 64);
			break;
		default:
			Random rand = new Random();
			this.color = SWTResourceManager.getColor(rand.nextInt() & 0xff, rand.nextInt() & 0xff, rand.nextInt() & 0xff); // (SWT.COLOR_GREEN));
			break;
		}
		// set position defaults
		if (recordOrdinal % 2 == 0) {
			this.setPositionLeft(true); // position left
		}
		else {
			this.setPositionLeft(false); // position right
		}
	}

	/**
	 * fetch time steps and records from the parent and add settlement points according the evaluation rules.
	 * omit time the steps when the trigger was not fired.
	 */
	public synchronized void addFromTransitions(HistoTransitions histoTransitions, TransitionType transitionType) {
		if (transitionType.isPercent()) {
			for (Transition transition : histoTransitions) {
				calculateAndAdd4Peak(transition);
			}
		}
		else {
			for (Transition transition : histoTransitions) {
				calculateAndAdd4Level(transition);
			}
		}
	}

	/**
	 * add threshold values.
	 * @param transition
	 */
	private void calculateAndAdd4Level(Transition transition) {
		CalculationType calculation = this.settlement.getEvaluation().getCalculation();
		Record calculationRecord = this.parent.get(this.parent.recordNames[calculation.getRefOrdinal()]);
		double calculationValue;
		if (calculation.getCalcType().equals(CalculationTypes.COUNT.value())) {
			calculationValue = transition.thresholdSize * 1000.; // all internal values are multiplied by 1000
		}
		else if (calculation.getCalcType().equals(CalculationTypes.TIME_SUM_MS.value())) {
			calculationValue = (calculationRecord.getTime_ms(transition.recoveryStartIndex) - calculationRecord.getTime_ms(transition.thresholdStartIndex)) * 10.;
		}
		else if (calculation.getCalcType().equals(CalculationTypes.MIN.value())) {
			calculationValue = Double.MAX_VALUE;
			for (int j = transition.thresholdStartIndex; j > transition.recoveryStartIndex; j++)
				if (calculationRecord.get(j) != null) {
					double calculationTranslateValue = this.device.translateValue(calculationRecord, calculationRecord.get(j));
					calculationValue = Math.min(calculationValue, calculation.isUnsigned() ? Math.abs(calculationTranslateValue) : calculationTranslateValue);
				}
		}
		else if (calculation.getCalcType().equals(CalculationTypes.MAX.value())) {
			calculationValue = calculation.isUnsigned() ? Double.MAX_VALUE : -Double.MAX_VALUE;
			for (int j = transition.thresholdStartIndex; j > transition.recoveryStartIndex; j++)
				if (calculationRecord.get(j) != null) {
					double calculationTranslateValue = this.device.translateValue(calculationRecord, calculationRecord.get(j));
					calculationValue = Math.max(calculationValue, calculation.isUnsigned() ? Math.abs(calculationTranslateValue) : calculationTranslateValue);
				}
		}
		else if (calculation.getCalcType().equals(CalculationTypes.AVG.value())) {
			double sum = 0;
			int skipCount = 0;
			for (int j = transition.thresholdStartIndex; j > transition.recoveryStartIndex; j++)
				if (calculationRecord.get(j) != null) {
					double calculationTranslateValue = this.device.translateValue(calculationRecord, calculationRecord.get(j));
					sum += calculation.isUnsigned() ? Math.abs(calculationTranslateValue) : calculationTranslateValue;
				}
				else {
					skipCount++;
				}
			calculationValue = sum / (double) (transition.thresholdSize - skipCount);
		}
		else if (calculation.getCalcType().equals(CalculationTypes.SIGMA.value())) {
			double avg = 0, q = 0, value = 0;
			int skipCount = 0;
			calculationValue = 0;
			for (int j = transition.thresholdStartIndex; j > transition.recoveryStartIndex; j++)
				if (calculationRecord.get(j) != null) {
					value = calculation.isUnsigned() ? Math.abs(this.device.translateValue(calculationRecord, calculationRecord.get(j)))
							: this.device.translateValue(calculationRecord, calculationRecord.get(j));
					double deltaAvg = value - avg;
					avg += deltaAvg / (j - transition.thresholdStartIndex - skipCount + 1);
					q += deltaAvg * (value - avg);
				}
				else {
					skipCount++;
				}
			calculationValue = Math.sqrt(q / (transition.thresholdSize - skipCount - 1));
		}
		else if (calculation.getCalcType().equals(CalculationTypes.SUM.value())) {
			calculationValue = 0;
			for (int j = transition.thresholdStartIndex; j > transition.recoveryStartIndex; j++)
				if (calculationRecord.get(j) != null) {
					double calculationTranslateValue = this.device.translateValue(calculationRecord, calculationRecord.get(j));
					calculationValue += calculation.isUnsigned() ? Math.abs(calculationTranslateValue) : calculationTranslateValue;
				}
		}
		else
			calculationValue = 0;
		// add to settlement record
		addNullableRaw((int) reverseTranslateValue(calculationValue), (long) calculationRecord.getTime_ms(transition.thresholdEndIndex) * 10); // todo assign better to the time step of the extremum value in the threshold and recovery time
		 if (log.isLoggable(Level.FINE))
		log.log(Level.FINE, String.format("%s: timeStamp_ms=%f  calculationValue=%d  reverseTranslateValue=%f", calculation.getCalcType(), calculationRecord.getTime_ms(transition.recoveryStartIndex //$NON-NLS-1$
				- 1), calculationValue, reverseTranslateValue(calculationValue)));
	}

	/**
	 * walks through the measurement record and calculates the difference between the threshold extremum value and the level value (reference or recovery).
	 * skips null measurement values. 
	 * @param record measurement points
	 * @param leveling rule for determining the level value from the device configuration
	 * @param transition
	 * @return peak spread value as translatedValue
	 */
	private double calculateDelta4Peak(Record record, LevelingTypes leveling, Transition transition) {
		double deltaValue = 0;
		CalculationType calculation = this.settlement.getEvaluation().getCalculation();
		double referenceExtremum = 0;
		double thresholdExtremum = 0;
		double recoveryExtremum = 0;
		if (leveling.equals("first")) {
			for (int j = transition.startIndex; j < transition.thresholdStartIndex; j++) {
				if (record.get(j) != null) {
					referenceExtremum = this.device.translateValue(record, record.get(j));
					break;
				}
			}
			for (int j = transition.thresholdStartIndex; j < transition.recoveryStartIndex; j++) {
				if (record.get(j) != null) {
					thresholdExtremum = this.device.translateValue(record, record.get(j));
					break;
				}
			}
			for (int j = transition.recoveryStartIndex; j < transition.endIndex; j++) {
				if (record.get(j) != null) {
					recoveryExtremum = this.device.translateValue(record, record.get(j));
					break;
				}
			}
			deltaValue = calculation.isBasedOnRecovery() == null ? thresholdExtremum - referenceExtremum
					: calculation.isBasedOnRecovery() ? thresholdExtremum - recoveryExtremum : thresholdExtremum - referenceExtremum;
		}
		else if (leveling.equals("last")) {
			for (int j = transition.referenceEndIndex; j >= transition.startIndex; j--) {
				if (record.get(j) != null) {
					referenceExtremum = this.device.translateValue(record, record.get(j));
					break;
				}
			}
			for (int j = transition.thresholdEndIndex; j >= transition.thresholdStartIndex; j--) {
				if (record.get(j) != null) {
					thresholdExtremum = this.device.translateValue(record, record.get(j));
					break;
				}
			}
			for (int j = transition.endIndex; j >= transition.recoveryStartIndex; j--) {
				if (record.get(j) != null) {
					recoveryExtremum = this.device.translateValue(record, record.get(j));
					break;
				}
			}
			deltaValue = calculation.isBasedOnRecovery() == null ? thresholdExtremum - recoveryExtremum
					: calculation.isBasedOnRecovery() ? thresholdExtremum - recoveryExtremum : thresholdExtremum - referenceExtremum;
		}
		else if (leveling.equals("mid")) {
			if (transition.referenceSize > 1) {
				int midIndex = (transition.startIndex + transition.referenceEndIndex) / 2;
				for (int i = midIndex, j = midIndex + 1; i <= transition.referenceEndIndex && j >= transition.startIndex; i++, j--) {
					if (record.get(i) != null) {
						referenceExtremum = this.device.translateValue(record, record.get(i));
						break;
					}
					if (record.get(j) != null) {
						referenceExtremum = this.device.translateValue(record, record.get(j));
						break;
					}
				}
			}
			else {
				referenceExtremum = this.device.translateValue(record, record.get(transition.startIndex));
			}
			if (transition.thresholdSize > 1) {
				int midIndex = (transition.thresholdStartIndex + transition.thresholdEndIndex) / 2;
				for (int i = midIndex, j = midIndex; i <= transition.thresholdEndIndex && j >= transition.thresholdStartIndex; j--) {
					if (record.get(i) != null) {
						thresholdExtremum = this.device.translateValue(record, record.get(i));
						break;
					}
					if (record.get(j) != null) {
						thresholdExtremum = this.device.translateValue(record, record.get(j));
						break;
					}
				}
			}
			else {
				thresholdExtremum = this.device.translateValue(record, record.get(transition.thresholdStartIndex));
			}
			if (transition.recoverySize > 1) {
				int midIndex = (transition.recoveryStartIndex + transition.endIndex) / 2;
				for (int i = midIndex, j = midIndex; i <= transition.endIndex && j >= transition.recoveryStartIndex; j--) {
					if (record.get(i) != null) {
						recoveryExtremum = this.device.translateValue(record, record.get(i));
						break;
					}
					if (record.get(j) != null) {
						recoveryExtremum = this.device.translateValue(record, record.get(j));
						break;
					}
				}
			}
			else {
				recoveryExtremum = this.device.translateValue(record, record.get(transition.recoveryStartIndex));
			}
			deltaValue = calculation.isBasedOnRecovery() == null ? thresholdExtremum - recoveryExtremum
					: calculation.isBasedOnRecovery() ? thresholdExtremum - recoveryExtremum : thresholdExtremum - referenceExtremum;
		}
		else if (leveling.equals("min") || leveling.equals("max")) {
			final boolean isPositivePeak;
			{ // detect if the peak is positive (ascending) or negative (descending) - required for devices which charge and discharge with electric current values always positive
				int lastValue = record.get(transition.thresholdStartIndex - 2); // start one time step ahead in order to cope with potential measurement latencies
				long positiveWeightSum = 0, negativeWeightSum = 0;
				long positiveDeltaSum = 0, negativeDeltaSum = 0;
				for (int j = transition.referenceEndIndex, delta; j < transition.endIndex + 1; j++) {
					if (record.get(j) != null) {
						if ((delta = record.get(j) - lastValue) > 0) {
							positiveWeightSum += delta * (j - transition.startIndex);
							positiveDeltaSum += delta;
						}
						else {
							negativeWeightSum += delta * (j - transition.startIndex);
							negativeDeltaSum += delta;
						}
						lastValue = record.get(j);
					}
				}
				isPositivePeak = (negativeWeightSum / (double) negativeDeltaSum - positiveWeightSum / (double) positiveDeltaSum) > 0;
				 if (log.isLoggable(Level.FINE))
				log.log(Level.FINE,
						String.format("%s isPositivePeak=%b  based on PositionIndicator=%f", record.getName(), isPositivePeak,
								((double) negativeWeightSum / negativeDeltaSum - (double) positiveWeightSum / positiveDeltaSum)
										/ ((double) negativeWeightSum / negativeDeltaSum + (double) positiveWeightSum / positiveDeltaSum)));
			}
			if (isPositivePeak) {
				int value = Integer.MAX_VALUE;
				for (int j = transition.startIndex; j < transition.thresholdStartIndex; j++)
					if (record.get(j) != null && record.get(j) < value) value = record.get(j);
				referenceExtremum = this.device.translateValue(record, value);
				value = -Integer.MAX_VALUE;
				for (int j = transition.referenceEndIndex; j < transition.recoveryStartIndex + 1; j++)// one additional time step before and after in order to cope with potential measurement latencies
					if (record.get(j) != null && record.get(j) > value) value = record.get(j);
				thresholdExtremum = this.device.translateValue(record, value);
				value = Integer.MAX_VALUE;
				for (int j = transition.recoveryStartIndex; j < transition.endIndex + 1; j++)
					if (record.get(j) != null && record.get(j) < value) value = record.get(j);
				recoveryExtremum = this.device.translateValue(record, value);
				deltaValue = calculation.isBasedOnRecovery() == null ? thresholdExtremum - Math.min(referenceExtremum, recoveryExtremum)
						: calculation.isBasedOnRecovery() ? thresholdExtremum - recoveryExtremum : thresholdExtremum - referenceExtremum;
			}
			else {
				int value = -Integer.MAX_VALUE;
				for (int j = transition.startIndex; j < transition.thresholdStartIndex; j++)
					if (record.get(j) != null && record.get(j) > value) value = record.get(j);
				referenceExtremum = this.device.translateValue(record, value);
				value = Integer.MAX_VALUE;
				for (int j = transition.referenceEndIndex; j < transition.recoveryStartIndex + 1; j++)// one additional time step before and after in order to cope with potential measurement latencies
					if (record.get(j) != null && record.get(j) < value) value = record.get(j);
				thresholdExtremum = this.device.translateValue(record, value);
				value = -Integer.MAX_VALUE;
				for (int j = transition.recoveryStartIndex; j < transition.endIndex + 1; j++)
					if (record.get(j) != null && record.get(j) > value) value = record.get(j);
				recoveryExtremum = this.device.translateValue(record, value);
				deltaValue = calculation.isBasedOnRecovery() == null ? thresholdExtremum - Math.max(referenceExtremum, recoveryExtremum)
						: calculation.isBasedOnRecovery() ? thresholdExtremum - recoveryExtremum : thresholdExtremum - referenceExtremum;
			}
		}
		else if (leveling.equals("avg")) {
			double value = 0;
			int skipCount = 0;
			for (int j = transition.startIndex; j < transition.thresholdStartIndex; j++)
				if (record.get(j) != null)
					value += (record.get(j) - value) / (double) (j - transition.startIndex + 1);
				else
					skipCount++;
			referenceExtremum = this.device.translateValue(record, value / (transition.referenceSize - skipCount));
			value = 0;
			skipCount = 0;
			for (int j = transition.thresholdStartIndex; j < transition.recoveryStartIndex; j++)
				if (record.get(j) != null)
					value += (record.get(j) - value) / (double) (j - transition.thresholdStartIndex + 1);
				else
					skipCount++;
			thresholdExtremum = this.device.translateValue(record, value / (transition.thresholdSize - skipCount));
			value = 0;
			skipCount = 0;
			for (int j = transition.recoveryStartIndex; j < transition.endIndex + 1; j++)
				if (record.get(j) != null)
					value += (record.get(j) - value) / (double) (j - transition.recoveryStartIndex + 1);
				else
					skipCount++;
			recoveryExtremum = this.device.translateValue(record, value / (transition.recoverySize - skipCount));
			deltaValue = calculation.isBasedOnRecovery() == null ? thresholdExtremum - (referenceExtremum + recoveryExtremum) / 2
					: calculation.isBasedOnRecovery() ? thresholdExtremum - recoveryExtremum : thresholdExtremum - referenceExtremum;
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("referenceExtremum=%f  thresholdExtremum=%f  recoveryExtremum=%f  deltaValue=%f  @ isBasedOnRecovery=%s" //$NON-NLS-1$
				, referenceExtremum, thresholdExtremum, recoveryExtremum, deltaValue, calculation.isBasedOnRecovery()));
		return deltaValue;
	}

	/**
	 * walk forward from the time step when the trigger has fired and collect the extremum values for the calculation.
	 * add single result value.
	 * @param transition
	 */
	private void calculateAndAdd4Peak(Transition transition) {
		CalculationType calculation = this.settlement.getEvaluation().getCalculation();
		final Record record = this.parent.get(this.parent.recordNames[calculation.getRefOrdinal()]);
		//		if (log.isLoggable(Level.FINE)) log.log(Level.FINE,
		//				String.format("%s: referenceDequeSize=%d thresholdDequeSize=%d recoveryDequeSize=%d", record.getName(), transition.getReferenceSize(), transition.thresholdSize, transition.getRecoverySize())); //$NON-NLS-1$
		log.log(Level.FINE, record.getName() + " values   " //$NON-NLS-1$
				+ record.subList(transition.startIndex, transition.endIndex + 1));
		final double calculationValue;
		if (calculation.getCalcType().equals(CalculationTypes.RATIO.value()) || calculation.getCalcType().equals(CalculationTypes.RATIO_INVERSE.value())) {
			final double denominator = calculateDelta4Peak(record, calculation.getLeveling(), transition);
			final Record divisorRecord = calculation.getRefOrdinalDivisor() != null ? this.parent.get(this.parent.recordNames[calculation.getRefOrdinalDivisor()]) : null;
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, divisorRecord.getName() + " divisors " //$NON-NLS-1$
					+ divisorRecord.subList(transition.startIndex, transition.endIndex + 1));
			final double divisor = calculateDelta4Peak(divisorRecord, calculation.getDivisorLeveling(), transition);
			if (calculation.getCalcType().equals(CalculationTypes.RATIO.value())) {
				calculationValue = calculation.isUnsigned() ? Math.abs(1000. * denominator / divisor) : 1000. * denominator / divisor; // all internal values are multiplied by 1000
			}
			else {
				calculationValue = calculation.isUnsigned() ? Math.abs(1000. * divisor / denominator) : 1000. * divisor / denominator; // all internal values are multiplied by 1000
			}
		}
		else if (calculation.getCalcType().equals(CalculationTypes.COUNT.value())) {
			calculationValue = 1000. * transition.thresholdSize; // all internal values are multiplied by 1000
		}
		else if (calculation.getCalcType().equals(CalculationTypes.TIME_SUM_MS.value())) {
			calculationValue = (record.getTime_ms(transition.recoveryStartIndex) - record.getTime_ms(transition.thresholdStartIndex));
		}
		else if (calculation.getCalcType().equals(CalculationTypes.MIN.value())) {
			double value = Double.MAX_VALUE;
			for (int j = transition.thresholdStartIndex; j < transition.recoveryStartIndex; j++)
				if (record.get(j) != null) value = Math.min(value, calculation.isUnsigned() ? Math.abs(this.device.translateValue(record, record.get(j))) : this.device.translateValue(record, record.get(j)));
			calculationValue = value;
		}
		else if (calculation.getCalcType().equals(CalculationTypes.MAX.value())) {
			double value = calculation.isUnsigned() ? 0. : -Double.MAX_VALUE;
			for (int j = transition.thresholdStartIndex; j < transition.recoveryStartIndex; j++)
				if (record.get(j) != null) value = Math.max(value, calculation.isUnsigned() ? Math.abs(this.device.translateValue(record, record.get(j))) : this.device.translateValue(record, record.get(j)));
			calculationValue = value;
		}
		else if (calculation.getCalcType().equals(CalculationTypes.AVG.value())) {
			double sum = 0;
			int skipCount = 0;
			for (int j = transition.thresholdStartIndex; j < transition.recoveryStartIndex; j++)
				if (record.get(j) != null) {
					sum += calculation.isUnsigned() ? Math.abs(this.device.translateValue(record, record.get(j))) : this.device.translateValue(record, record.get(j));
				}
				else
					skipCount++;
			calculationValue = sum / (double) (transition.thresholdSize - skipCount);
		}
		else if (calculation.getCalcType().equals(CalculationTypes.SIGMA.value())) {
			// https://en.wikipedia.org/wiki/Standard_deviation#Rapid_calculation_methods
			double avg = 0, q = 0, value = 0;
			int skipCount = 0;
			for (int j = transition.thresholdStartIndex; j < transition.recoveryStartIndex; j++)
				if (record.get(j) != null) {
					value = calculation.isUnsigned() ? Math.abs(this.device.translateValue(record, record.get(j))) : this.device.translateValue(record, record.get(j));
					double deltaAvg = value - avg;
					avg += deltaAvg / (j - transition.thresholdStartIndex - skipCount + 1);
					q += deltaAvg * (value - avg);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("value=%f  deltaAvg=%f  q=%f", value, deltaAvg, q));
				}
				else
					skipCount++;
			calculationValue = Math.sqrt(q / (transition.thresholdSize - skipCount - 1));
		}
		else if (calculation.getCalcType().equals(CalculationTypes.SUM.value())) {
			double sum = 0;
			for (int j = transition.thresholdStartIndex; j < transition.recoveryStartIndex; j++)
				sum += calculation.isUnsigned() ? Math.abs(this.device.translateValue(record, record.get(j))) : this.device.translateValue(record, record.get(j));
			calculationValue = sum;
		}
		else
			calculationValue = 0;
		// add to settlement record
		addNullableRaw((int) reverseTranslateValue(calculationValue), (long) record.getTime_ms(transition.thresholdEndIndex) * 10); // todo assign better to the time step of the extremum value in the threshold and recovery time
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("addRaw: timeStamp_ms=%f  calculationValue=%f  reverseTranslateValue=%f", record.getTime_ms(transition.recoveryStartIndex) //$NON-NLS-1$
				, calculationValue, reverseTranslateValue(calculationValue)));
	}

	/**
	 * add a data point to the settlement data, checks for minimum and maximum to define display range
	 * @param point
	 * @param time_100ns in 0.1 ms (divide by 10 to get ms)
	 */
	public synchronized boolean addNullableRaw(Integer point, long time_100ns) {
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

	public int getOrdinal() {
		return this.ordinal;
	}

	public void setOrdinal(int newOrdinal) {
		this.ordinal = newOrdinal;
	}

	public String getName() {
		return this.name;
	}

	public String getUnit() {
		return this.unit;
	}

	public void setUnit(String newUnit) {
		this.unit = newUnit;
	}

	public String getSymbol() {
		return this.symbol;
	}

	public void setSymbol(String newSymbol) {
		this.symbol = newSymbol;
	}

	/**
	 * get a reference to the record properies (offset, factor, ...)
	 * @return list containing the properties
	 */
	List<PropertyType> getProperties() {
		return this.properties;
	}

	/**
	 * replace the properties to enable channel/configuration switch
	 * @param newProperties
	 */
	public void setProperties(List<PropertyType> newProperties) {
		this.properties = new ArrayList<PropertyType>();
		for (PropertyType property : newProperties) {
			this.properties.add(property.clone());
		}
	}

	/**
	 * get property reference using given property type key (IDevice.OFFSET, ...)
	 * @param propertyKey
	 * @return PropertyType
	 */
	public PropertyType getProperty(String propertyKey) {
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
	public PropertyType createProperty(String propertyKey, DataTypes type, Object value) {
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

	public boolean isVisible() {
		return this.isVisible;
	}

	public void setVisible(boolean enabled) {
		this.isVisible = enabled;
	}

	public int getMaxValue() {
		return this.parent.isScopeMode ? this.scopeMax : this.maxValue == this.minValue ? this.maxValue + 100 : this.maxValue;
	}

	public int getMinValue() {
		return this.parent.isScopeMode ? this.scopeMin : this.minValue == this.maxValue ? this.minValue - 100 : this.minValue;
	}

	public int getSyncMaxValue() {
		return this.parent.isScopeMode ? this.scopeMax : this.syncMaxValue == this.syncMinValue ? this.syncMaxValue + 100 : this.syncMaxValue;
	}

	public int getSyncMinValue() {
		return this.parent.isScopeMode ? this.scopeMin : this.syncMinValue == this.syncMaxValue ? this.syncMinValue - 100 : this.syncMinValue;
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

		if (this.parent.isZoomMode) // record -> recordSet.isZoomMode
			tmpSize = this.findBestIndex(this.zoomTimeOffset + this.drawTimeWidth) - this.zoomOffset;
		else if (this.parent.isScopeMode) tmpSize = this.parent.scopeModeSize;

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
		if (this.parent.isZoomMode) {
			index = index + this.zoomOffset;
			index = index > (this.elementCount - 1) ? (this.elementCount - 1) : index;
			index = index < 0 ? 0 : index;
		}
		else if (this.parent.isScopeMode) {
			index = index + this.parent.scopeModeOffset;
			index = index > (this.elementCount - 1) ? (this.elementCount - 1) : index;
			index = index < 0 ? 0 : index;
		}
		else {
			index = index > (this.elementCount - 1) ? (this.elementCount - 1) : index;
			index = index < 0 ? 0 : index;
		}
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

	public boolean isPositionLeft() {
		return this.isPositionLeft;
	}

	public void setPositionLeft(boolean enabled) {
		this.isPositionLeft = enabled;
	}

	public Color getColor() {
		return this.color;
	}

	public String getRGB() {
		return String.format("%d, %d,%d", this.color.getRed(), this.color.getGreen(), this.color.getBlue());
	}

	public void setColor(Color newColor) {
		this.color = newColor;
	}

	public boolean isRoundOut() {
		return this.parent.isZoomMode ? false : this.isRoundOut;
	}

	public void setRoundOut(boolean enabled) {
		this.isRoundOut = enabled;
	}

	public boolean isStartpointZero() {
		return this.parent.isZoomMode ? false : this.isStartpointZero;
	}

	public void setStartpointZero(boolean enabled) {
		this.isStartpointZero = enabled;
	}

	public boolean isStartEndDefined() {
		return this.parent.isZoomMode ? true : this.isStartEndDefined;
	}

	/**
	 * sets the min-max values as displayed 4.0 - 200.5
	 * @param enabled
	 * @param newMinScaleValue
	 * @param newMaxScaleValue
	 */
	public void setStartEndDefined(boolean enabled, double newMinScaleValue, double newMaxScaleValue) {
		this.isStartEndDefined = enabled;
		if (enabled) {
			this.maxScaleValue = this.maxDisplayValue = newMaxScaleValue;
			this.minScaleValue = this.minDisplayValue = newMinScaleValue;
		}
		else {
			if (this.channelConfigKey == null || this.channelConfigKey.length() < 1) this.channelConfigKey = this.parent.getChannelConfigName();
			this.maxScaleValue = translateValue(this.maxValue / 1000.0);
			this.minScaleValue = translateValue(this.minValue / 1000.0);
		}
	}

	public void setMinScaleValue(double newMinScaleValue) {
		if (this.parent.isZoomMode)
			this.minZoomScaleValue = newMinScaleValue;
		else
			this.minScaleValue = newMinScaleValue;
	}

	public void setMaxScaleValue(double newMaxScaleValue) {
		if (this.parent.isZoomMode)
			this.maxZoomScaleValue = newMaxScaleValue;
		else
			this.maxScaleValue = newMaxScaleValue;
	}

	public int getLineWidth() {
		return this.lineWidth;
	}

	public void setLineWidth(int newLineWidth) {
		this.lineWidth = newLineWidth;
	}

	public int getLineStyle() {
		return this.lineStyle;
	}

	public void setLineStyle(int newLineStyle) {
		this.lineStyle = newLineStyle;
	}

	public int getNumberFormat() {
		return this.numberFormat;
	}

	public void setNumberFormat(int newNumberFormat) {
		this.numberFormat = newNumberFormat;
		switch (newNumberFormat) {
		case -1:
			final double delta = this.maxScaleValue - this.minScaleValue == 0 ? translateValue((this.maxValue - this.minValue) / 1000) : this.maxScaleValue - this.minScaleValue;
			final double maxValueAbs = translateValue(Math.abs(this.maxValue / 1000));
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format(Locale.getDefault(), "%s: %.0f - %.1f", this.name, maxValueAbs, delta));
			if (maxValueAbs < 100) {
				if (delta < 0.1)
					this.df.applyPattern("0.000"); //$NON-NLS-1$
				else if (delta <= 1)
					this.df.applyPattern("0.00"); //$NON-NLS-1$
				else if (delta < 100)
					this.df.applyPattern("0.0"); //$NON-NLS-1$
				else
					this.df.applyPattern("0"); //$NON-NLS-1$
			}
			else if (maxValueAbs < 500) {
				if (delta <= 0.1)
					this.df.applyPattern("0.00"); //$NON-NLS-1$
				else if (delta <= 1)
					this.df.applyPattern("0.0"); //$NON-NLS-1$
				else
					this.df.applyPattern("0"); //$NON-NLS-1$
			}
			else {
				if (delta <= 5)
					this.df.applyPattern("0.0"); //$NON-NLS-1$
				else
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
	 * @return the parent
	 */
	public RecordSet getParent() {
		return this.parent;
	}

	/**
	 * @param currentParent the parent to set
	 */
	@Deprecated
	public void setParentAndChannel(HistoRecordSet currentParent) {
		if (this.channelConfigKey == null || this.channelConfigKey.length() < 1) this.channelConfigKey = currentParent.getChannelConfigName();
		this.parent = currentParent;
	}

	/**
	 * @return the isDisplayable
	 */
	public boolean isDisplayable() {
		return this.isDisplayable;
	}

	/**
	 * @param enabled the isDisplayable to set
	 */
	public void setDisplayable(boolean enabled) {
		this.isDisplayable = enabled;
	}

	/**
	 * @return the isActive
	 */
	public boolean isActive() {
		return this.isActive;
	}

	/**
	 * set isActive value
	 */
	public void setActive(boolean newValue) {
		this.isActive = newValue;
	}

	/**
	 * @return the maxScaleValue
	 */
	public double getMaxScaleValue() {
		return this.parent.isZoomMode ? this.maxZoomScaleValue : this.maxScaleValue;
	}

	/**
	 * @return the minScaleValue
	 */
	public double getMinScaleValue() {
		return this.parent.isZoomMode ? this.minZoomScaleValue : this.minScaleValue;
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
		if (this.parent.isZoomMode) {
			return (this.timeStep_ms == null ? this.parent.timeStep_ms.getTime_ms(index + this.zoomOffset) : this.timeStep_ms.getTime_ms(index + this.zoomOffset)) - this.zoomTimeOffset;
		}
		else if (this.parent.isScopeMode) {
			return this.timeStep_ms == null ? this.parent.timeStep_ms.getTime_ms(index + this.parent.scopeModeOffset) - this.parent.timeStep_ms.getTime_ms(this.parent.scopeModeOffset)
					: this.timeStep_ms.getTime_ms(index + this.parent.scopeModeOffset) - this.timeStep_ms.getTime_ms(this.parent.scopeModeOffset);
		}
		else
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
	 * @return the zoomTimeOffset
	 */
	public int getZoomOffset() {
		return this.zoomOffset;
	}

	/**
	 * @return the zoomTimeOffset
	 */
	public double getZoomTimeOffset() {
		return this.zoomTimeOffset;
	}

	/**
	 * @return the zoomTimeOffset
	 */
	public double getDrawTimeOffset_ms() {
		double timeOffset_ms = 0;
		if (this.parent.isScopeMode) {
			if ((this.timeStep_ms != null && this.timeStep_ms.isConstant) || this.parent.timeStep_ms.isConstant) {
				timeOffset_ms = (this.timeStep_ms != null ? this.timeStep_ms.get(0) : this.parent.timeStep_ms.get(0)) * (this.elementCount - this.parent.scopeModeSize) / 10.0;
			}
			else {
				timeOffset_ms = this.timeStep_ms != null ? this.timeStep_ms.getTime_ms(this.elementCount - this.parent.scopeModeSize)
						: this.parent.timeStep_ms.getTime_ms(this.elementCount - this.parent.scopeModeSize);
			}
		}
		else if (this.parent.isZoomMode) {
			timeOffset_ms = this.zoomTimeOffset;
		}
		return timeOffset_ms;
	}

	/**
	 * @return the time in msec representing the segment to be displayed, without zooming this is the maximum time represented by the last data point time
	 */
	public double getDrawTimeWidth_ms() {
		if (this.elementCount > 0) {
			if (this.parent.isScopeMode) {
				if ((this.timeStep_ms != null && this.timeStep_ms.isConstant) || (this.parent.timeStep_ms != null && this.parent.timeStep_ms.isConstant)) {
					this.drawTimeWidth = (this.timeStep_ms != null ? this.timeStep_ms.get(0) : this.parent.timeStep_ms.get(0)) * this.parent.scopeModeSize / 10.0;
				}
				else {
					this.drawTimeWidth = this.timeStep_ms != null ? this.timeStep_ms.getDeltaTime(this.elementCount - 1 - this.parent.scopeModeSize, this.elementCount - 1)
							: this.parent.timeStep_ms.getDeltaTime(this.elementCount - 1 - this.parent.scopeModeSize, this.elementCount - 1);
				}
			}
			else if (!this.parent.isZoomMode) { // normal not manipulated view
				if ((this.timeStep_ms != null && this.timeStep_ms.isConstant) || (this.parent.timeStep_ms != null && this.parent.timeStep_ms.isConstant)) {
					this.drawTimeWidth = (this.timeStep_ms != null ? this.timeStep_ms.get(0) : this.parent.timeStep_ms.get(0)) * (this.elementCount - 1) / 10.0;
				}
				else {
					this.drawTimeWidth = (this.timeStep_ms != null ? this.timeStep_ms.lastElement() : this.parent.timeStep_ms.lastElement()) / 10.0;
				}
			}
		}
		return this.drawTimeWidth; // for this.parent.isZoomMode=true the width was calculated while setting the zoom bounds
	}

	/**
	 * @param newZoomTimeOffset the zoomTimeOffset to set
	 */
	public void setZoomTimeOffset(double newZoomTimeOffset) {
		this.zoomTimeOffset = newZoomTimeOffset;
	}

	/**
	 * @param newDrawTimeWidth the potential time width to be drawn
	 */
	public void setDrawTimeWidth(double newDrawTimeWidth) {
		this.drawTimeWidth = newDrawTimeWidth;
	}

	/**
	 * @return the decimal format used by this record
	 */
	public DecimalFormat getDecimalFormat() {
		if (this.numberFormat == -1) this.setNumberFormat(-1); // update the number format to actual automatic formating
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, this.isScaleSynced() + " - " + this.parent.getSyncMasterRecordOrdinal(this));
		return this.isScaleSynced() ? this.parent.get(this.parent.getSyncMasterRecordOrdinal(this)).df : this.df;
	}

	/**
	 * @return the keyName
	 */
	public String getKeyName() {
		return this.keyName;
	}

	/**
	 * @param newKeyName the keyName to set
	 */
	public void setKeyName(String newKeyName) {
		this.keyName = newKeyName;
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
	 * method to query time and value for display at a given index
	 * @param measurementPointIndex (differs from index if display width != measurement size)
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @return point time, value
	 */
	public Point getDisplayPoint(int measurementPointIndex, int xDisplayOffset, int yDisplayOffset) {
		// log.log(Level.OFF, " measurementPointIndex=" + measurementPointIndex + " value=" + (this.get(measurementPointIndex) / 1000.0) + "(" + (yDisplayOffset - Double.valueOf((this.get(measurementPointIndex)/1000.0 - (this.minDisplayValue*1/this.syncMasterFactor)) *
		// this.displayScaleFactorValue).intValue()) + ")");
		return new Point(xDisplayOffset + Double.valueOf(this.getTime_ms(measurementPointIndex) * this.displayScaleFactorTime).intValue(),
				yDisplayOffset - Double.valueOf(((this.get(measurementPointIndex) / 1000.0) - (this.minDisplayValue * 1 / this.syncMasterFactor)) * this.displayScaleFactorValue).intValue());
	}

	/**
	 * method to query time and value for display at a given index
	 * @param measurementPointIndex (differs from index if display width != measurement size)
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @return point time, value
	 */
	public Point getGPSDisplayPoint(int measurementPointIndex, int xDisplayOffset, int yDisplayOffset) {
		// log.log(Level.OFF, " measurementPointIndex=" + measurementPointIndex + " value=" + (this.get(measurementPointIndex) / 1000.0) + "(" + (yDisplayOffset - Double.valueOf((this.get(measurementPointIndex)/1000.0 - (this.minDisplayValue*1/this.syncMasterFactor)) *
		// this.displayScaleFactorValue).intValue()) + ")");
		int grad = this.get(measurementPointIndex) / 1000000;
		return new Point(xDisplayOffset + Double.valueOf(this.getTime_ms(measurementPointIndex) * this.displayScaleFactorTime).intValue(), yDisplayOffset - Double
				.valueOf((((grad + ((this.get(measurementPointIndex) / 1000000.0 - grad) / 0.60)) * 1000.0) - (this.minDisplayValue * 1 / this.syncMasterFactor)) * this.displayScaleFactorValue).intValue());
	}

	/**
	 * method to query x,y position of a display at a given horizontal display index 
	 * @param xPos
	 * @return point time, value
	 */
	public Point getDisplayEndPoint(int xPos) {
		return new Point(this.parent.drawAreaBounds.x + xPos, this.parent.drawAreaBounds.y + getVerticalDisplayPointValue(xPos));
	}

	/**
	* get the time in msec at given horizontal display position
	* @param xPos of the display point
	* @return time value in msec
	*/
	public double getHorizontalDisplayPointTime_ms(int xPos) {
		return this.drawTimeWidth * xPos / this.parent.drawAreaBounds.width;
	}

	/**
	* get the formatted time with unit at given position
	* @param xPos of the display point
	* @return string of time value in simple date format HH:ss:mm:SSS
	*/
	public String getHorizontalDisplayPointAsFormattedTimeWithUnit(int xPos) {
		return TimeLine.getFomatedTimeWithUnit(
				this.getHorizontalDisplayPointTime_ms(xPos) + this.getDrawTimeOffset_ms() + (this.settings != null && this.settings.isTimeFormatAbsolute() ? this.getStartTimeStamp() : 1292400000.0)); // 1292400000 == 1970-01-16 00:00:00.000
	}

	/**
	 * calculate best fit index in data vector from given display point relative to the (zoomed) display width
	 * @param xPos
	 * @return position integer value
	 */
	public int getHorizontalPointIndexFromDisplayPoint(int xPos) {
		return this.findBestIndex(getHorizontalDisplayPointTime_ms(xPos) + this.getDrawTimeOffset_ms());
	}

	/**
	 * query data value (not translated in device units) from a display position point 
	 * @param xPos
	 * @return displays yPos in pixel
	 */
	public int getVerticalDisplayPointValue(int xPos) {
		int pointPosY = 0;

		try {
			double tmpTimeValue = this.getHorizontalDisplayPointTime_ms(xPos) + this.getDrawTimeOffset_ms();
			int[] indexs = this.findBoundingIndexes(tmpTimeValue);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, tmpTimeValue + "; " + indexs[0] + "; " + indexs[1]); //$NON-NLS-1$ //$NON-NLS-2$
			if (super.size() > 0) {
				if (false) { // (this.getDevice().isGPSCoordinates(this)) {
					int grad0 = this.get(indexs[0]) / 1000000;
					if (indexs[0] == indexs[1]) {
						pointPosY = Double.valueOf(this.parent.drawAreaBounds.height
								- ((((grad0 + ((this.get(indexs[0]) / 1000000.0 - grad0) / 0.60)) * 1000.0) - (this.minDisplayValue * 1 / this.syncMasterFactor)) * this.displayScaleFactorValue)).intValue();
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
						pointPosY = Double.valueOf(this.parent.drawAreaBounds.height - (((super.get(indexs[0]) / 1000.0) - (this.minDisplayValue * 1 / this.syncMasterFactor)) * this.displayScaleFactorValue))
								.intValue();
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
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, xPos + " -> timeValue = " + TimeLine.getFomatedTime(tmpTimeValue) + " pointPosY = " + pointPosY); //$NON-NLS-1$ //$NON-NLS-2$

			// check yPos out of range, the graph might not visible within this area
			// if(pointPosY > this.parent.drawAreaBounds.height)
			// log.log(Level.WARNING, "pointPosY > drawAreaBounds.height");
			// if(pointPosY < 0)
			// log.log(Level.WARNING, "pointPosY < 0");
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
	public String getVerticalDisplayPointAsFormattedScaleValue(int yPos, Rectangle drawAreaBounds) {
		String displayPointValue;
		// scales are all synchronized in viewpoint of end values (min/max)
		// PropertyType syncProperty = this.parent.isCompareSet ? null : this.device.getMeasruementProperty(this.parent.parent.number, this.ordinal, MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value());
		// if (syncProperty != null && !syncProperty.getValue().equals(GDE.STRING_EMPTY)) {
		// Record syncRecord = this.parent.get(this.ordinal);
		// displayPointValue = syncRecord.df.format(Double.valueOf(syncRecord.minDisplayValue + ((syncRecord.maxDisplayValue - syncRecord.minDisplayValue) * (drawAreaBounds.height-yPos) / drawAreaBounds.height)));
		// }
		// else
		if (this.parent.isZoomMode)
			displayPointValue = this.df.format(Double.valueOf(this.minZoomScaleValue + ((this.maxZoomScaleValue - this.minZoomScaleValue) * (drawAreaBounds.height - yPos) / drawAreaBounds.height)));
		else
			displayPointValue = this.df.format(Double.valueOf(this.minScaleValue + ((this.maxScaleValue - this.minScaleValue) * (drawAreaBounds.height - yPos) / drawAreaBounds.height)));

		return displayPointValue;
	}

	/**
	 * get the scale value corresponding the vertical display point
	 * @param yPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
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
	public String getVerticalDisplayDeltaAsFormattedValue(int deltaPos, Rectangle drawAreaBounds) {
		String textValue;
		if (this.parent.isZoomMode || this.parent.isScopeMode)
			textValue = this.df.format(Double.valueOf((this.maxZoomScaleValue - this.minZoomScaleValue) * deltaPos / drawAreaBounds.height));
		else
			textValue = this.df.format(Double.valueOf((this.maxScaleValue - this.minScaleValue) * deltaPos / drawAreaBounds.height));

		return textValue;
	}

	/**
	 * get the slope value of two given points, unit depends on device configuration
	 * @param points describing the time difference (x) as well as the measurement difference (y)
	 * @return formated string of value
	 */
	public String getSlopeValue(Point points) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.STRING_EMPTY + points.toString());
		double measureDelta;
		if (this.parent.isZoomMode)
			measureDelta = (this.maxZoomScaleValue - this.minZoomScaleValue) * points.y / this.parent.drawAreaBounds.height;
		else
			measureDelta = (this.maxScaleValue - this.minScaleValue) * points.y / this.parent.drawAreaBounds.height;
		// double timeDelta = (1.0 * points.x * this.size() - 1) / drawAreaBounds.width * this.getTimeStep_ms() / 1000; //sec
		// this.drawTimeWidth * xPos / this.parent.drawAreaBounds.width;
		double timeDelta = this.drawTimeWidth * points.x / this.parent.drawAreaBounds.width / 1000; // sec
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "measureDelta = " + measureDelta + " timeDelta = " + timeDelta); //$NON-NLS-1$ //$NON-NLS-2$
		return new DecimalFormat("0.0").format(measureDelta / timeDelta); //$NON-NLS-1$
	}

	/**
	 * set the zoom bounds from display as record point offset and size, min/max scale values
	 * @param zoomBounds - where the start point offset is x,y and the area is width, height
	 */
	public void setZoomBounds(Rectangle zoomBounds) {
		this.zoomTimeOffset = this.getHorizontalDisplayPointTime_ms(zoomBounds.x) + this.getDrawTimeOffset_ms();
		if (this.zoomTimeOffset < 0) this.zoomTimeOffset = 0;
		this.zoomOffset = this.findBestIndex(this.zoomTimeOffset);
		this.drawTimeWidth = this.getHorizontalDisplayPointTime_ms(zoomBounds.width - 1);
		if (this.drawTimeWidth > this.getMaxTime_ms()) this.drawTimeWidth = this.getMaxTime_ms();
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.name + " zoomTimeOffset " + TimeLine.getFomatedTimeWithUnit(this.zoomTimeOffset) + " drawTimeWidth " //$NON-NLS-1$ //$NON-NLS-2$
				+ TimeLine.getFomatedTimeWithUnit(this.drawTimeWidth));

		this.tmpMinZoomScaleValue = this.getVerticalDisplayPointScaleValue(zoomBounds.y, this.parent.drawAreaBounds);
		this.tmpMaxZoomScaleValue = this.getVerticalDisplayPointScaleValue(zoomBounds.height + zoomBounds.y, this.parent.drawAreaBounds);
		this.minZoomScaleValue = tmpMinZoomScaleValue < this.minScaleValue ? this.minScaleValue : tmpMinZoomScaleValue;
		this.maxZoomScaleValue = tmpMaxZoomScaleValue > this.maxScaleValue ? this.maxScaleValue : tmpMaxZoomScaleValue;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.name + " - minZoomScaleValue = " + this.minZoomScaleValue + "  maxZoomScaleValue = " + this.maxZoomScaleValue); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @return the displayScaleFactorTime
	 */
	public double getDisplayScaleFactorTime() {
		return this.displayScaleFactorTime;
	}

	/**
	 * @param newDisplayScaleFactorTime the displayScaleFactorTime to set
	 */
	public void setDisplayScaleFactorTime(double newDisplayScaleFactorTime) {
		this.displayScaleFactorTime = newDisplayScaleFactorTime;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format(Locale.ENGLISH, "displayScaleFactorTime = %.3f", newDisplayScaleFactorTime)); //$NON-NLS-1$
	}

	/**
	 * @return the displayScaleFactorValue
	 */
	public double getDisplayScaleFactorValue() {
		return this.displayScaleFactorValue;
	}

	/**
	 * @param drawAreaHeight - used to calculate the displayScaleFactorValue to set
	 */
	public void setDisplayScaleFactorValue(int drawAreaHeight) {
		this.displayScaleFactorValue = (1.0 * drawAreaHeight) / (this.maxDisplayValue - this.minDisplayValue);
		if (this.parent.isOneOfSyncableRecord(this) && this.getFactor() / this.parent.get(this.parent.getSyncMasterRecordOrdinal(this)).getFactor() != 1) {
			this.syncMasterFactor = this.getFactor() / this.parent.get(this.parent.getSyncMasterRecordOrdinal(this)).getFactor();
			this.displayScaleFactorValue = this.displayScaleFactorValue * syncMasterFactor;
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER,
				String.format(Locale.ENGLISH, "drawAreaHeight = %d displayScaleFactorValue = %.3f (this.maxDisplayValue - this.minDisplayValue) = %.3f", drawAreaHeight, this.displayScaleFactorValue, //$NON-NLS-1$
						(this.maxDisplayValue - this.minDisplayValue)));

	}

	/**
	 * @param newMinDisplayValue the minDisplayValue to set
	 */
	public void setMinDisplayValue(double newMinDisplayValue) {
		if (false) { // todo (this.device.isGPSCoordinates(this)) {
			this.minDisplayValue = translateValue(newMinDisplayValue) * 1000;
		}
		else
			this.minDisplayValue = newMinDisplayValue;

		if (this.parent.isOneOfSyncableRecord(this)) {
			// todo find better solution for syncing records and histo settlements
			throw new IllegalStateException();
			// for (HistoSettlement tmpRecord : this.parent.scaleSyncedRecords.get(this.parent.getSyncMasterRecordOrdinal(this))) {
			// tmpRecord.minDisplayValue = this.minDisplayValue;
			// }
		}
	}

	/**
	 * @param newMaxDisplayValue the maxDisplayValue to set
	 */
	public void setMaxDisplayValue(double newMaxDisplayValue) {
		if (false) { // todo (this.device.isGPSCoordinates(this)) {
			this.maxDisplayValue = translateValue(newMaxDisplayValue) * 1000;
		}
		else
			this.maxDisplayValue = newMaxDisplayValue;

		if (this.parent.isOneOfSyncableRecord(this)) {
			// todo find better solution for syncing records and histo settlements
			throw new IllegalStateException();
			// for (HistoSettlement tmpRecord : this.parent.scaleSyncedRecords.get(this.parent.getSyncMasterRecordOrdinal(this))) {
			// tmpRecord.maxDisplayValue = this.maxDisplayValue;
			// }
		}
	}

	/**
	 * @return the minDisplayValue
	 */
	public double getMinDisplayValue() {
		return this.minDisplayValue;
	}

	/**
	 * @return the maxDisplayValue
	 */
	public double getMaxDisplayValue() {
		return this.maxDisplayValue;
	}

	/**
	 * @return the formated minDisplayValue as it is displayed in graphics window
	 */
	public String getFormatedMinDisplayValue() {
		return this.df.format(this.minDisplayValue);
	}

	/**
	 * @return the formated maxDisplayValue as it is displayed in graphics window
	 */
	public String getFormatedMaxDisplayValue() {
		return this.df.format(this.maxDisplayValue);
	}

	/**
	 * set min and max scale values for zoomed mode
	 * @param newMinZoomScaleValue
	 * @param newMaxZoomScaleValue
	 */
	public void setMinMaxZoomScaleValues(double newMinZoomScaleValue, double newMaxZoomScaleValue) {
		this.minZoomScaleValue = newMinZoomScaleValue;
		this.maxZoomScaleValue = newMaxZoomScaleValue;
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, this.name + " - minScaleValue/minZoomScaleValue = " + this.minScaleValue + "/" + newMinZoomScaleValue + " : maxScaleValue/maxZoomScaleValue = " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ this.maxScaleValue + "/" + newMaxZoomScaleValue); //$NON-NLS-1$
	}

	/**
	 * @return the isMeasurementMode
	 */
	public boolean isMeasurementMode() {
		return this.isMeasurementMode;
	}

	/**
	 * @param enabled the isMeasurementMode to set
	 */
	public void setMeasurementMode(boolean enabled) {
		this.isMeasurementMode = enabled;
	}

	/**
	 * @return the isDeltaMeasurementMode
	 */
	public boolean isDeltaMeasurementMode() {
		return this.isDeltaMeasurementMode;
	}

	/**
	 * @param enabled the isDeltaMeasurementMode to set
	 */
	public void setDeltaMeasurementMode(boolean enabled) {
		this.isDeltaMeasurementMode = enabled;
	}

	/**
	 * @return the parentName
	 */
	@Deprecated
	public String getChannelConfigKey() {
		if (this.channelConfigKey == null || this.channelConfigKey.length() < 1) this.channelConfigKey = this.parent.getChannelConfigName();

		return this.channelConfigKey;
	}

	/**
	 * @param newChannelConfigKey the channelConfigKey to set
	 */
	@Deprecated
	public void setChannelConfigKey(String newChannelConfigKey) {
		this.channelConfigKey = newChannelConfigKey;
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
	 * set data unsaved with a given reason
	 * @param reason
	 */
	public void setUnsaved(String reason) {
		this.parent.setUnsaved(reason);
	}

	/**
	 * @return the numberScaleTicks
	 */
	public int getNumberScaleTicks() {
		return this.numberScaleTicks;
	}

	/**
	 * @param newNumberScaleTicks the numberScaleTicks to set
	 */
	public void setNumberScaleTicks(int newNumberScaleTicks) {
		this.numberScaleTicks = newNumberScaleTicks;
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
	 * query if the record display scale is synced with an other record
	 * @return the isScaleSynced
	 */
	public boolean isScaleSynced() {
		return this.parent.isOneOfSyncableRecord(this);
	}

	/**
	 * @return true if the record represents a scale synchronize master record
	 */
	public boolean isScaleSyncMaster() {
		return this.parent.scaleSyncedRecords.containsKey(this.ordinal);
	}

	/**
	 * @return true if the record is the scale sync master
	 */
	public boolean isScaleVisible() {
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.name + " isScaleSyncMaster=" + isScaleSyncMaster() + " isOneOfSyncableRecord=" + this.parent.isOneOfSyncableRecord(this));
		return isScaleSyncMaster() ? this.parent.isOneSyncableVisible(this.ordinal) : !this.parent.isOneOfSyncableRecord(this) && this.isVisible && this.isDisplayable;
	}

	/**
	 * set min max values for scope view (recordSet.setScopeMode())
	 * @param newScopeMin the scopeMin to set
	 * @param newScopeMax the scopeMin to set
	 */
	public void setScopeMinMax(int newScopeMin, int newScopeMax) {
		this.scopeMin = newScopeMin;
		this.scopeMax = newScopeMax;
	}

	/**
	 * get the curve index until the curve will be drawn (for compare set with different time length curves)
	 * @return the drawLimit
	 */
	public double getCompareSetDrawLimit_ms() {
		return this.drawLimit_ms;
	}

	/**
	 * set the curve index until the curve will be drawn
	 * @param newDrawLimit the drawLimit to set
	 */
	public void setCompareSetDrawLimit_ms(double newDrawLimit) {
		this.drawLimit_ms = newDrawLimit;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return true if the record contained reasonable date which can be displayed
	 */
	public boolean hasReasonableData() {
		return this.realSize() > 0 && (this.minValue != this.maxValue || translateValue(this.maxValue / 1000.0) != 0.0);
	}

	/**
	 * query the dataType of this record
	 * @return
	 */
	public HistoSettlement.DataType getDataType() {
		return this.dataType == null ? DataType.DEFAULT : this.dataType;
	}

	/**
	 * set the dataType of this record by evaluating its name
	 * @return
	 */
	public void setDataType() {
		if (this.name.equalsIgnoreCase("Latitude") || this.name.contains("GPS_Lat") || this.name.equalsIgnoreCase("Breitengrad") || this.name.toLowerCase().contains("sirka"))
			this.dataType = HistoSettlement.DataType.GPS_LATITUDE;
		else if (this.name.equalsIgnoreCase("Longitude") || this.name.contains("GPS_Long") || this.name.equalsIgnoreCase("Längengrad") || this.name.equalsIgnoreCase("Laengengrad")
				|| this.name.toLowerCase().contains("delka"))
			this.dataType = HistoSettlement.DataType.GPS_LONGITUDE;
		else if ((this.name.toUpperCase().contains("GPS") || this.name.toUpperCase().contains("ABS"))
				&& (this.name.toLowerCase().contains("hoehe") || this.name.toLowerCase().contains("höhe") || this.name.toLowerCase().contains("height") || this.name.toLowerCase().contains("alt")))
			this.dataType = HistoSettlement.DataType.GPS_ALTITUDE;
		else if (this.name.toUpperCase().contains("GPS") && (this.name.toLowerCase().contains("speed") || this.name.toLowerCase().contains("geschw"))) this.dataType = HistoSettlement.DataType.SPEED;
	}

	/**
	 * set the dataType of this record
	 * @return
	 */
	public void setDataType(HistoSettlement.DataType newDataType) {
		this.dataType = newDataType;
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
		double newValue = 0;

		if (this.getParent().getChannelConfigNumber() == 3 && (this.getOrdinal() == 1 || this.getOrdinal() == 2)) { // 1=GPS-longitude 2=GPS-latitude
			// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			int grad = ((int) (value / 1000));
			double minuten = (value - (grad * 1000.0)) / 10.0;
			newValue = grad + minuten / 60.0;
		}
		else {
			newValue = (value - reduction) * factor + offset;
		}
		// if (log.isLoggable(Level.FINER))
		log.log(Level.FINER, "for " + this.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate settlement values to values represented.
	 * does not support settlements based on GPS-longitude or GPS-latitude.
	 * @return double of settlement dependent value
	 */
	private double reverseTranslateValue(double value) {
		double factor;
		double offset;
		double reduction;
		CalculationType calculation = this.settlement.getEvaluation().getCalculation();
		if (calculation.getCalcType().equals(CalculationTypes.RATIO.value()) || calculation.getCalcType().equals(CalculationTypes.RATIO_INVERSE.value())) {
			factor = .001; // internal representation in per mille
			offset = 0; // != 0 if a unit translation is required
			reduction = 0; // != 0 if a unit translation is required
		}
		else if (calculation.getCalcType().equals(CalculationTypes.COUNT.value())) {
			factor = 1; // internal representation in ea
			offset = 0; // != 0 if a unit translation is required
			reduction = 0; // != 0 if a unit translation is required
		}
		else {
			factor = this.getFactor(); // != 1 if a unit translation is required
			offset = this.getOffset(); // != 0 if a unit translation is required
			reduction = this.getReduction(); // != 0 if a unit translation is required
		}
		double newValue = 0;

		// if (this.getParent().getChannelConfigNumber() == 3 && (this.getOrdinal() == 1 || this.getOrdinal() == 2)) { // 1=GPS-longitude 2=GPS-latitude )
		// // 0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		// int grad = (int) value;
		// double minuten = (value - grad * 1.0) * 60.0;
		// newValue = (grad + minuten / 100.0) * 1000.0;
		// } else {
		newValue = (value - offset) / factor + reduction;
		// }
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + this.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

}
