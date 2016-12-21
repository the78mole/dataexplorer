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

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import gde.data.TrailRecord.TrailType;
import gde.device.ChannelType;
import gde.device.EvaluationType;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.ScoreLabelTypes;
import gde.device.SettlementType;
import gde.device.StatisticsType;
import gde.device.TransitionType;
import gde.histocache.Entries;
import gde.histocache.EntryPoints;
import gde.histocache.HistoVault;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.Quantile;
import gde.utils.StringHelper;
import gde.utils.Quantile.Fixings;

/**
 * holds histo records for the configured measurements and for histo settlements.
 * the settlements support evaluations with calculations and transitions and simple properties for score values.
 * @author Thomas Eickert
 */
/**
 * @author Thomas Eickert
 */
public class HistoRecordSet extends RecordSet {
	final static String															$CLASS_NAME								= HistoRecordSet.class.getName();
	final static long																serialVersionUID					= -1580283867987273535L;
	final static Logger															log												= Logger.getLogger($CLASS_NAME);

	final static int																initialRecordCapacity			= 5555;																				
	final static int																initialSettlementCapacity	= 22;			

	private LinkedHashMap<String, HistoSettlement>	histoSettlements					= new LinkedHashMap<String, HistoSettlement>();

	private Integer[]																scorePoints;
	private HistoVault															truss;
	private long																		elapsedHistoRecordSet_ns;

	/**
	 * record set data buffers according the size of given names array, where the name is the key to access the data buffer
	 * @param truss holds the base data for the forthcoming history vault
	 * @param recordSetBaseName for the recordset from "1) Laden"  or "6) Channels [1234]"
	 * @param recordNames array of the device supported measurement and settlement names
	 * @param newTimeStep_ms time in msec of device measures points
	 */
	private HistoRecordSet(HistoVault truss, String recordSetBaseName, String[] recordNames) {
		super(DataExplorer.getInstance().getActiveDevice(), DataExplorer.getInstance().getActiveChannelNumber(), recordSetBaseName, recordNames,
				DataExplorer.getInstance().getActiveDevice().getTimeStep_ms(), true, true);
		this.truss = truss;
		ChannelType channelType = this.device.getDeviceConfiguration().getChannel(super.getChannelConfigNumber());
		for (SettlementType settlementType : channelType.getSettlement()) {
			HistoSettlement histoSettlement = new HistoSettlement(this.device, settlementType, this, initialSettlementCapacity);
			this.histoSettlements.put(settlementType.getName(), histoSettlement);
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, this.getName() + " HistoRecordSet(IDevice, int, Path, String[], double, boolean, boolean"); //$NON-NLS-1$
	}

	/**
	 * copy constructor - used to copy a record set during, where the configuration coming from the device properties file
	 * @param recordSet
	 * @param newChannelConfiguration
	 */
	@Deprecated
	protected HistoRecordSet(RecordSet recordSet, int dataIndex, boolean isFromBegin) {
		super(recordSet, dataIndex, isFromBegin);
	}

	/**
	 * clone method re-writes data points of all records of this record set - if isFromBegin == true, the given index is the index where the record starts after this operation - if isFromBegin == false, the given index represents the last data point index of the records.
	 * @param dataIndex
	 * @param isFromBegin
	 * @return new created record set
	 */
	@Override
	@Deprecated
	public HistoRecordSet clone(int dataIndex, boolean isFromBegin) {
		return new HistoRecordSet(this, dataIndex, isFromBegin);
	}

	/**
	 * clears the data points in all records and in timeStep.
	 * reduce initial capacity to zero.
	 * does not clear any fields in the recordSet, in the records or in timeStep. 
	 */
	public void cleanup() {
		//		this.histoSettlements.clear();
		//		this.histoScoregroups.clear();
		//		this.visibleAndDisplayableRecords.clear();
		//		this.visibleAndDisplayableRecords.trimToSize();
		//		this.allRecords.clear();
		//		this.allRecords.trimToSize();
		//		this.scaleSyncedRecords.clear();

		this.timeStep_ms.clear();
		this.timeStep_ms.trimToSize();
		for (Map.Entry<String, Record> entry : this.entrySet()) {
			entry.getValue().clear();
			entry.getValue().trimToSize();
		}
		//		super.clear();
	}

	/**
	 * method to create a histo record set containing records according the device channel configuration
	 * which are loaded from device properties file.
	 * @param truss holds the base data for the forthcoming history vault
	 * @return a record set containing all records (empty) as specified
	 */
	public static HistoRecordSet createRecordSet(HistoVault truss) {
		String[] recordNames = DataExplorer.getInstance().getActiveDevice().getMeasurementNames(DataExplorer.getInstance().getActiveChannelNumber());
		HistoRecordSet newRecordSet = new HistoRecordSet(truss, truss.getLogRecordsetBaseName(), recordNames);
		if (log.isLoggable(Level.FINE)) printRecordNames("createHistoRecordSet() " + newRecordSet.name + " - ", newRecordSet.getRecordNames()); //$NON-NLS-1$ //$NON-NLS-2$

		newRecordSet.timeStep_ms = new TimeSteps(DataExplorer.getInstance().getActiveDevice().getTimeStep_ms(), initialRecordCapacity);

		for (int i = 0; i < recordNames.length; i++) {
			MeasurementType measurement = DataExplorer.getInstance().getActiveDevice().getMeasurement(DataExplorer.getInstance().getActiveChannelNumber(), i);
			Record tmpRecord = new Record(DataExplorer.getInstance().getActiveDevice(), i, recordNames[i], measurement.getSymbol(), measurement.getUnit(), measurement.isActive(),
					measurement.getStatistics(), measurement.getProperty(), initialRecordCapacity);
			tmpRecord.setColorDefaultsAndPosition(i);
			newRecordSet.put(recordNames[i], tmpRecord);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("added record for %s - %d", recordNames[i], newRecordSet.size())); //$NON-NLS-1$
		}
		newRecordSet.syncScaleOfSyncableRecords();
		return newRecordSet;
	}

	/**
	 * @param objectDirectory validated object key
	 * @param truss holds the base data for the forthcoming history vault
	 * @param recordNames array of the device supported measurement and settlement names
	 * @param recordSymbols
	 * @param recordUnits
	 * @return a record set containing all records (empty) as specified
	 */
	public static HistoRecordSet createRecordSet(HistoVault truss, String[] recordNames, String[] recordSymbols, String[] recordUnits) {
		HistoRecordSet newRecordSet = new HistoRecordSet(truss, truss.getLogRecordsetBaseName(), recordNames);
		if (log.isLoggable(Level.FINE)) printRecordNames("createHistoRecordSet() " + newRecordSet.name + " - ", newRecordSet.getRecordNames()); //$NON-NLS-1$ //$NON-NLS-2$

		newRecordSet.timeStep_ms = new TimeSteps(DataExplorer.getInstance().getActiveDevice().getTimeStep_ms(), initialRecordCapacity);

		for (int i = 0; i < recordNames.length; i++) {
			MeasurementType measurement = DataExplorer.getInstance().getActiveDevice().getMeasurement(DataExplorer.getInstance().getActiveChannelNumber(), i);
			Record tmpRecord = new Record(DataExplorer.getInstance().getActiveDevice(), i, recordNames[i], recordSymbols[i], recordUnits[i], measurement.isActive(), measurement.getStatistics(),
					measurement.getProperty(), initialRecordCapacity);
			tmpRecord.setColorDefaultsAndPosition(i);
			newRecordSet.put(recordNames[i], tmpRecord);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("added record for %s - %d", recordNames[i], newRecordSet.size())); //$NON-NLS-1$
		}
		newRecordSet.syncScaleOfSyncableRecords();
		return newRecordSet;
	}

	@Deprecated
	public static RecordSet createRecordSet(String recordSetName, IDevice device, int channelConfigNumber, boolean isRaw, boolean isFromFile) {
		return null;
	}

	@Deprecated
	public static RecordSet createRecordSet(String recordSetName, IDevice device, int channelConfigNumber, String[] recordNames, String[] recordSymbols, String[] recordUnits, double timeStep_ms,
			boolean isRaw, boolean isFromFile) {
		return null;
	}

	//	/**
	//	 * method to add a series of points to none calculation records (records active or inactive)
	//	 * @param points as int[], where the length must fit records.size()
	//	 * @throws DataInconsitsentException 
	//	 */
	//	@Deprecated
	//	@Override
	//	public synchronized void addNoneCalculationRecordsPoints(int[] points) throws DataInconsitsentException {
	//		throw new UnsupportedOperationException();
	//	}
	//
	//	/**
	//	 * method to add a series of points to none calculation records (records active or inactive)
	//	 * @param points as int[], where the length must fit records.size()
	//	 * @param time_ms
	//	 * @throws DataInconsitsentException 
	//	 */
	//	@Deprecated
	//	@Override
	//	public synchronized void addNoneCalculationRecordsPoints(int[] points, double time_ms) throws DataInconsitsentException {
	//		throw new UnsupportedOperationException();
	//	}
	//
	/**
	 * add a new time step to the time steps vector
	 * @param timeValue
	 */
	@Deprecated
	@Override
	public void addTimeStep_ms(double timeValue) {
		throw new UnsupportedOperationException();
	}

	/**
	 * returns a specific data vector selected by given key data name
	 * @param recordNameKey
	 * @return Vector<Integer>
	 */
	public HistoSettlement getSettlement(String recordNameKey) {
		return this.histoSettlements.get(recordNameKey);
	}

	public int getSyncMasterRecordOrdinal(HistoSettlement histoSettlement) {
		// todo find better solution for syncing records and histo settlements
		throw new IllegalStateException();
	}

	public boolean isOneOfSyncableRecord(HistoSettlement histoSettlement) {
		// todo find better solution for syncing records and histo settlements
		return false;
	}

	/**
	 * replaces existing settlements by new ones based on channel settlements and transitions.
	 * populates settlements according to evaluation rules.
	 */
	public void addSettlements() {
		ChannelType channelType = this.device.getDeviceConfiguration().getChannel(super.getChannelConfigNumber());
		HistoTransitions transitions = new HistoTransitions(this.device, this);
		for (TransitionType transitionType : channelType.getTransition()) {
			transitions.addFromRecord(this.get(this.recordNames[transitionType.getRefOrdinal()]), transitionType);
		}
		if (transitions.size() == 0) { // todo implement evaluations w/o transitions
		}
		else {
			for (SettlementType settlementType : channelType.getSettlement()) {
				if (settlementType.getEvaluation() != null) {
					HistoSettlement histoSettlement = this.histoSettlements.get(settlementType.getName());
					Integer transitionId = settlementType.getEvaluation().getCalculation().getTransitionId(); // todo decide if evaluations without calculation are useful
					histoSettlement.clear();
					histoSettlement.addFromTransitions(transitions, channelType.getTransitionById(transitionId));
				}
			}
		}
	}

	public boolean isSampled() {
		return (this.scorePoints[ScoreLabelTypes.TOTAL_READINGS.ordinal()] != null && this.scorePoints[ScoreLabelTypes.SAMPLED_READINGS.ordinal()] != null
				&& this.scorePoints[ScoreLabelTypes.TOTAL_READINGS.ordinal()] > this.scorePoints[ScoreLabelTypes.SAMPLED_READINGS.ordinal()]);
	}

	/**
	 * @param nanoTimeSpan
	 */
	public void setElapsedHistoRecordSet_ns(long nanoTimeSpan) {
		this.elapsedHistoRecordSet_ns = nanoTimeSpan;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("timeSpan=%,7d ms", TimeUnit.NANOSECONDS.toMillis(nanoTimeSpan))); //$NON-NLS-1$
	}

	/**
	 * get points for measurements which are included in the recordset.
	 * @return list of rows with index measurement type number. each row holds the points associated to the trail types with the ordinal as index.
	 */
	private Entries getMeasurementsPoints() {
		List<MeasurementType> channelMeasurements = this.device.getChannelMeasuremts(super.getChannelConfigNumber());
		Entries entries = new Entries();

		for (int i = 0; i < this.recordNames.length; i++) {
			MeasurementType measurementType = channelMeasurements.get(i);
			EntryPoints entryPoints = new EntryPoints(i, measurementType.getName());
			entries.getEntryPoints().add(entryPoints);

			Record record = this.get(this.recordNames[i]);
			if (record == null) {
				if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, String.format("no record found for measurementType.getName()=%s %s", measurementType.getName(), this.truss.getLogFilePath())); //$NON-NLS-1$
			}
			else {
				StatisticsType measurementStatistics = measurementType.getStatistics();
				if (measurementStatistics != null) { // todo this creates trail types mismatch  :  && record.hasReasonableData()) {
					int triggerRefOrdinal = -1;
					if (measurementStatistics.getTriggerRefOrdinal() != null) {
						int tmpOrdinal = measurementStatistics.getTriggerRefOrdinal().intValue();
						if (record != null && record.isDisplayable()) {
							triggerRefOrdinal = tmpOrdinal;
						}
					}
					boolean isTriggerLevel = measurementStatistics.getTrigger() != null;
					boolean isGpsCoordinates = this.device.isGPSCoordinates(record);
					if (measurementStatistics.isAvg()) {
						if (isTriggerLevel)
							entryPoints.addPoint(TrailType.REAL_AVG.ordinal(), TrailType.REAL_AVG.name(), record.getAvgValueTriggered() != Integer.MIN_VALUE ? record.getAvgValueTriggered() : 0);
						else if (triggerRefOrdinal < 0 || record.getAvgValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE) //
							if (isGpsCoordinates)
							entryPoints.addPoint(TrailType.REAL_AVG.ordinal(), TrailType.REAL_AVG.name(), record.getAvgValue());
							else
							entryPoints.addPoint(TrailType.REAL_AVG.ordinal(), TrailType.REAL_AVG.name(), triggerRefOrdinal < 0 ? record.getAvgValue() : (Integer) record.getAvgValueTriggered(triggerRefOrdinal));
					}
					if (measurementStatistics.isMax()) {
						if (isTriggerLevel)
							entryPoints.addPoint(TrailType.REAL_MAX.ordinal(), TrailType.REAL_MAX.name(), record.getMaxValueTriggered());
						else if (triggerRefOrdinal < 0 || record.getMaxValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE) //
							if (isGpsCoordinates)
							entryPoints.addPoint(TrailType.REAL_MAX.ordinal(), TrailType.REAL_MAX.name(), record.getRealMaxValue());
							else
							entryPoints.addPoint(TrailType.REAL_MAX.ordinal(), TrailType.REAL_MAX.name(), triggerRefOrdinal < 0 ? record.getRealMaxValue() : (Integer) record.getMaxValueTriggered(triggerRefOrdinal));
					}
					if (measurementStatistics.isMin()) {
						if (isTriggerLevel)
							entryPoints.addPoint(TrailType.REAL_MIN.ordinal(), TrailType.REAL_MIN.name(), record.getMinValueTriggered());
						else if (triggerRefOrdinal < 0 || record.getMinValueTriggered(triggerRefOrdinal) != Integer.MAX_VALUE) //
							if (isGpsCoordinates)
							entryPoints.addPoint(TrailType.REAL_MIN.ordinal(), TrailType.REAL_MIN.name(), record.getRealMinValue());
							else
							entryPoints.addPoint(TrailType.REAL_MIN.ordinal(), TrailType.REAL_MIN.name(), triggerRefOrdinal < 0 ? record.getRealMinValue() : (Integer) record.getMinValueTriggered(triggerRefOrdinal));
					}
					if (measurementStatistics.isSigma()) {
						if (isTriggerLevel)
							entryPoints.addPoint(TrailType.REAL_SD.ordinal(), TrailType.REAL_SD.name(), record.getSigmaValueTriggered());
						else if (triggerRefOrdinal < 0 || record.getSigmaValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE) //
							entryPoints.addPoint(TrailType.REAL_SD.ordinal(), TrailType.REAL_SD.name(),
									triggerRefOrdinal < 0 ? (Integer) record.getSigmaValue() : (Integer) record.getSigmaValueTriggered(triggerRefOrdinal));
					}
					if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
						if (measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1)
							entryPoints.addPoint(TrailType.REAL_SUM_TRIGGERED.ordinal(), TrailType.REAL_SUM_TRIGGERED.name(), record.getSumTriggeredRange());
						else if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
							entryPoints.addPoint(TrailType.REAL_SUM_TRIGGERED.ordinal(), TrailType.REAL_SUM_TRIGGERED.name(), record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal()));
						}
						if (measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1 && measurementStatistics.getRatioRefOrdinal() != null) {
							Record referencedRecord = this.get(measurementStatistics.getRatioRefOrdinal().intValue());
							StatisticsType referencedStatistics = this.device.getMeasurementStatistic(this.getChannelConfigNumber(), measurementStatistics.getRatioRefOrdinal());
							if (referencedRecord != null && (referencedStatistics.isAvg() || referencedStatistics.isMax()))
								// todo trigger ratio is multiplied by 1000 (per mille)
								if (referencedStatistics.isAvg())
								entryPoints.addPoint(TrailType.REAL_AVG_RATIO_TRIGGERED.ordinal(), TrailType.REAL_AVG_RATIO_TRIGGERED.name(), (int) Math.round(referencedRecord.getAvgValue() * 1000.0 / record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal())));
								else if (referencedStatistics.isMax()) //
									entryPoints.addPoint(TrailType.REAL_MAX_RATIO_TRIGGERED.ordinal(), TrailType.REAL_MAX_RATIO_TRIGGERED.name(),
											(int) Math.round(referencedRecord.getMaxValue() * 1000.0 / record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal())));
						}
					}
					if (measurementStatistics.getTrigger() != null && measurementStatistics.getSumTriggerTimeText() != null && measurementStatistics.getSumTriggerTimeText().length() > 1) {
						entryPoints.addPoint(TrailType.REAL_TIME_SUM_TRIGGERED.ordinal(), TrailType.REAL_TIME_SUM_TRIGGERED.name(), record.getTimeSumTriggeredRange_ms());
					}
					if (measurementStatistics.isCountByTrigger() != null) {
						entryPoints.addPoint(TrailType.REAL_COUNT_TRIGGERED.ordinal(), TrailType.REAL_COUNT_TRIGGERED.name(), record.getTriggerRanges() != null ? record.getTriggerRanges().size() : 0);
					}
				}
				else {
					// these trail types might act as default trails
					for (TrailType trailType : TrailType.getSubstitutes()) {
						if (trailType == TrailType.REAL_LAST)
							entryPoints.addPoint(TrailType.REAL_LAST.ordinal(), TrailType.REAL_LAST.name(), record.realRealGet(record.size() - 1));
						else if (trailType == TrailType.REAL_FIRST)
							entryPoints.addPoint(TrailType.REAL_FIRST.ordinal(), TrailType.REAL_FIRST.name(), record.realRealGet(0));
						else
							throw new UnsupportedOperationException(record.getName());
					}
				}
				if (record.size() != 0) {
					Quantile quantile = new Quantile(record, this.isSampled() ? EnumSet.of(Fixings.IS_SAMPLE) : EnumSet.noneOf(Fixings.class));
					entryPoints.addPoint(TrailType.Q0.ordinal(), TrailType.Q0.name(), (int) quantile.getQuartile0());
					entryPoints.addPoint(TrailType.Q1.ordinal(), TrailType.Q1.name(), (int) quantile.getQuartile1());
					entryPoints.addPoint(TrailType.Q2.ordinal(), TrailType.Q2.name(), (int) quantile.getQuartile2());
					entryPoints.addPoint(TrailType.Q3.ordinal(), TrailType.Q3.name(), (int) quantile.getQuartile3());
					entryPoints.addPoint(TrailType.Q4.ordinal(), TrailType.Q4.name(), (int) quantile.getQuartile4());
					entryPoints.addPoint(TrailType.Q_25_PERMILLE.ordinal(), TrailType.Q_25_PERMILLE.name(), (int) quantile.getQuantile(.025));
					entryPoints.addPoint(TrailType.Q_975_PERMILLE.ordinal(), TrailType.Q_975_PERMILLE.name(), (int) quantile.getQuantile(.975));
					entryPoints.addPoint(TrailType.Q_LOWER_WHISKER.ordinal(), TrailType.Q_LOWER_WHISKER.name(), (int) quantile.getQuantileLowerWhisker());
					entryPoints.addPoint(TrailType.Q_UPPER_WHISKER.ordinal(), TrailType.Q_UPPER_WHISKER.name(), (int) quantile.getQuantileUpperWhisker());
				}
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, record.getName() + " data " + entryPoints.toString()); //$NON-NLS-1$
			}
		}
		return entries;
	}

	/**
	 * @return list of rows with index settlementId. each row holds the points associated to the trail types with the ordinal as index.
	 */
	private Entries getSettlementsPoints() {
		List<SettlementType> channelSettlements = this.device.getDeviceConfiguration().getChannelSettlements(super.getChannelConfigNumber());
		//		HashMap<Integer, Integer[]> settlementsPoints = new HashMap<Integer, Integer[]>(channelSettlements.size());
		Entries entries = new Entries();
		for (int i = 0; i < channelSettlements.size(); i++) {
			SettlementType settlementType = channelSettlements.get(i);
			//			Integer[] trailTypePoints = new Integer[TrailType.getPrimitives().size()];
			//			settlementsPoints.put(settlementType.getSettlementId(), trailTypePoints);
			EntryPoints entryPoints = new EntryPoints(i, settlementType.getName());
			entries.getEntryPoints().add(entryPoints);

			HistoSettlement record = this.histoSettlements.get(settlementType.getName());
			if (record.size() > 0 && settlementType.getEvaluation() != null) {
				EvaluationType settlementEvaluations = settlementType.getEvaluation();
				if (record.hasReasonableData()) {
					if (settlementEvaluations.isAvg()) entryPoints.addPoint(TrailType.REAL_AVG.ordinal(), TrailType.REAL_AVG.name(), record.getAvgValue());
					if (settlementEvaluations.isMax()) entryPoints.addPoint(TrailType.REAL_MAX.ordinal(), TrailType.REAL_MAX.name(), record.getMaxValue());
					if (settlementEvaluations.isMin()) entryPoints.addPoint(TrailType.REAL_MIN.ordinal(), TrailType.REAL_MIN.name(), record.getMinValue());
					if (settlementEvaluations.isSigma()) entryPoints.addPoint(TrailType.REAL_SD.ordinal(), TrailType.REAL_SD.name(), record.getSigmaValue());
					if (settlementEvaluations.isSum()) entryPoints.addPoint(TrailType.REAL_SUM.ordinal(), TrailType.REAL_SUM.name(), record.getSumValue());
				}
				else {
					// these trail types might act as default trails
					for (TrailType trailType : TrailType.getSubstitutes()) {
						if (trailType == TrailType.REAL_LAST)
							entryPoints.addPoint(TrailType.REAL_LAST.ordinal(), TrailType.REAL_LAST.name(), record.realRealGet(record.size() - 1));
						else if (trailType == TrailType.REAL_FIRST)
							entryPoints.addPoint(TrailType.REAL_FIRST.ordinal(), TrailType.REAL_FIRST.name(), record.realRealGet(0));
						else
							throw new UnsupportedOperationException(record.getName());
					}
				}
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, record.getName() + " data " + entries.toString()); //$NON-NLS-1$
		}
		return entries;
	}

	/**
	 * @return points associated to the scores with the label ordinal as index.
	 */
	private EntryPoints getScorePoints() {
		// values are multiplied by 1000 as this is the convention for internal values in order to avoid rounding errors for values below 1.0 (0.5 -> 0)
		this.scorePoints[ScoreLabelTypes.DURATION_MM.ordinal()] = (int) (this.getMaxTime_ms() / 60000. * 1000. + .5);
		this.scorePoints[ScoreLabelTypes.AVERAGE_TIME_STEP_MS.ordinal()] = (int) (this.timeStep_ms.getAverageTimeStep_ms() * 1000.);
		this.scorePoints[ScoreLabelTypes.MAXIMUM_TIME_STEP_MS.ordinal()] = (int) (this.timeStep_ms.getMaximumTimeStep_ms() * 1000.);
		this.scorePoints[ScoreLabelTypes.MINIMUM_TIME_STEP_MS.ordinal()] = (int) (this.timeStep_ms.getMinimumTimeStep_ms() * 1000.);
		this.scorePoints[ScoreLabelTypes.SIGMA_TIME_STEP_MS.ordinal()] = (int) (this.timeStep_ms.getSigmaTimeStep_ms() * 1000.);
		this.scorePoints[ScoreLabelTypes.SAMPLED_READINGS.ordinal()] = this.getRecordDataSize(true);
		this.scorePoints[ScoreLabelTypes.ELAPSED_HISTO_RECORD_SET_MS.ordinal()] = (int) TimeUnit.NANOSECONDS.toMicros(this.elapsedHistoRecordSet_ns); // do not multiply by 1000 as usual, this is the conversion from ns to ms

		EntryPoints entryPoints = new EntryPoints(0, "All"); //$NON-NLS-1$
		for (ScoreLabelTypes scoreLabelTypes : EnumSet.allOf(ScoreLabelTypes.class)) {
			if (this.scorePoints[scoreLabelTypes.ordinal()] != null) {
				entryPoints.addPoint(scoreLabelTypes.ordinal(), scoreLabelTypes.toString(), this.scorePoints[scoreLabelTypes.ordinal()]);
			}
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, entryPoints.toString());
		return entryPoints;
	}

	public void setScorePoints(Integer[] scorePoints) {
		this.scorePoints = scorePoints;
	}

	@Deprecated // does not set the recordSetNumber
	@Override
	public void setName(String newName) {
		super.setName(newName);
	}

	public String getStartTimeStampFormatted() {
		return StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", this.getStartTimeStamp()); //$NON-NLS-1$
	}

}
