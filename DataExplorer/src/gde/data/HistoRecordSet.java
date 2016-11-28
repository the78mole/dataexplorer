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

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import gde.GDE;
import gde.data.TrailRecord.TrailType;
import gde.device.ChannelType;
import gde.device.DeviceConfiguration;
import gde.device.EvaluationType;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.ScoreLabelTypes;
import gde.device.SettlementType;
import gde.device.StatisticsType;
import gde.device.TransitionType;
import gde.exception.DataInconsitsentException;
import gde.histocache.Entries;
import gde.histocache.EntryPoints;
import gde.histocache.HistoVault;
import gde.histocache.Point;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.Quantile;
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

	final static int																initialRecordCapacity			= 5555;														// vector capacity values are crucial for performance
	final static int																initialSettlementCapacity	= 22;															// vector capacity values are crucial for performance

	private LinkedHashMap<String, HistoSettlement>	histoSettlements					= new LinkedHashMap<>();					//todo a better solution would be a common base class for Record, HistoSettlement and HistoScoregroup or a common interface

	private Integer[]																scorePoints;
	private int																			recordSetNumber;
	private Path																		filePathOrigin;
	private int																			logChannelNumber;
	private String																	logObjectKey;
	private long																		elapsedHistoRecordSet_ns;
	private long																		elapsedHistoVaultWrite_ns;

	/**
	 * record set data buffers according the size of given names array, where the name is the key to access the data buffer
	 * @param uiDevice
	 * @param uiChannelNumber the channel number to be used
	 * @param newName for the recordset like "1) Laden" 
	 * @param filePathOrigin source file path
	 * @param recordNames array of the device supported measurement and settlement names
	 * @param newTimeStep_ms time in msec of device measures points
	 * @param isRawValue specified if dependent values has been calculated
	 * @param isFromFileValue specifies if the data are red from file and if not modified don't need to be saved
	 */
	private HistoRecordSet(IDevice uiDevice, int uiChannelNumber, String newName, int logChannelNumber, String logObjectKey, Path filePathOrigin, String[] recordNames, boolean isRawValue,
			boolean isFromFileValue) {
		super(uiDevice, uiChannelNumber, newName, recordNames, DataExplorer.getInstance().getActiveDevice().getTimeStep_ms(), isRawValue, isFromFileValue);
		this.recordSetNumber = Integer.parseInt(this.getName().split(Pattern.quote(GDE.STRING_RIGHT_PARENTHESIS_BLANK))[0]);
		this.filePathOrigin = filePathOrigin.toAbsolutePath();
		this.logChannelNumber = logChannelNumber;
		this.logObjectKey = logObjectKey;
		ChannelType channelType = ((DeviceConfiguration) this.device).getChannel(super.getChannelConfigNumber());
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
	 * does not clear any fields in the recordSet, the records or in timeStep. 
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
	 * @param uiDevice the instance of the device 
	 * @param uiChannelConfigNumber (number of the outlet or configuration)
	 * @param newName for the recordset like "1) Laden" 
	 * @param filePathOrigin source file path
	 * @param isRaw defines if the data needs translation using device specific properties
	 * @param isFromFile defines if a configuration change must be recorded to signal changes
	 * @return a record set containing all records (empty) as specified
	 */
	public static HistoRecordSet createRecordSet(String newName, int logChannelNumber, String logObjectNumber, Path filePathOrigin, boolean isRaw, boolean isFromFile) {
		String[] recordNames = DataExplorer.getInstance().getActiveDevice().getMeasurementNames(DataExplorer.getInstance().getActiveChannelNumber());
		HistoRecordSet newRecordSet = new HistoRecordSet(DataExplorer.getInstance().getActiveDevice(), DataExplorer.getInstance().getActiveChannelNumber(), newName, logChannelNumber, logObjectNumber,
				filePathOrigin, recordNames, isRaw, isFromFile);
		if (log.isLoggable(Level.FINE)) printRecordNames("createHistoRecordSet() " + newRecordSet.name + " - ", newRecordSet.getRecordNames()); //$NON-NLS-1$ //$NON-NLS-2$

		newRecordSet.timeStep_ms = new TimeSteps(DataExplorer.getInstance().getActiveDevice().getTimeStep_ms(), initialRecordCapacity);

		String[] recordSymbols = new String[recordNames.length];
		String[] recordUnits = new String[recordNames.length];
		for (int i = 0; i < recordNames.length; i++) {
			MeasurementType measurement = DataExplorer.getInstance().getActiveDevice().getMeasurement(DataExplorer.getInstance().getActiveChannelNumber(), i);
			recordSymbols[i] = measurement.getSymbol();
			recordUnits[i] = measurement.getUnit();
		}
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

	/**
	 * @param uiDevice
	 * @param uiChannelNumber the channel number to be used
	 * @param newName for the recordset like "1) Laden" 
	 * @param filePathOrigin source file path
	 * @param recordNames array of the device supported measurement and settlement names
	 * @param recordSymbols
	 * @param recordUnits
	 * @param timeStep_ms time in msec of device measures points
	 * @param isRaw defines if the data needs translation using device specific properties
	 * @param isFromFile defines if a configuration change must be recorded to signal changes
	 * @return a record set containing all records (empty) as specified
	 */
	public static HistoRecordSet createRecordSet(String newName, int logChannelNumber, String logObjectKey, Path filePathOrigin, String[] recordNames, String[] recordSymbols, String[] recordUnits,
			boolean isRaw, boolean isFromFile) {
		HistoRecordSet newRecordSet = new HistoRecordSet(DataExplorer.getInstance().getActiveDevice(), DataExplorer.getInstance().getActiveChannelNumber(), newName, logChannelNumber, logObjectKey,
				filePathOrigin, recordNames, isRaw, isFromFile);
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

	/**
	 * method to add a series of points to none calculation records (records active or inactive)
	 * @param points as int[], where the length must fit records.size()
	 * @throws DataInconsitsentException 
	 */
	@Deprecated
	@Override
	public synchronized void addNoneCalculationRecordsPoints(int[] points) throws DataInconsitsentException {
		throw new UnsupportedOperationException();
	}

	/**
	 * method to add a series of points to none calculation records (records active or inactive)
	 * @param points as int[], where the length must fit records.size()
	 * @param time_ms
	 * @throws DataInconsitsentException 
	 */
	@Deprecated
	@Override
	public synchronized void addNoneCalculationRecordsPoints(int[] points, double time_ms) throws DataInconsitsentException {
		throw new UnsupportedOperationException();
	}

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
		ChannelType channelType = ((DeviceConfiguration) this.device).getChannel(super.getChannelConfigNumber());
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
	 * @param nanoTimeSpan
	 */
	public void setElapsedHistoVaultWrite_ns(long nanoTimeSpan) {
		this.elapsedHistoVaultWrite_ns = nanoTimeSpan;
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("timeSpan=%,7d ms", TimeUnit.NANOSECONDS.toMillis(nanoTimeSpan))); //$NON-NLS-1$
	}

	/**
	 * @return
	 */
	public HistoVault getHistoVault() {
		return HistoVault.createHistoVault(this.filePathOrigin, this.filePathOrigin.toFile().lastModified(), this.recordSetNumber, this.getName(), this.getStartTimeStamp(), this.logChannelNumber,
				this.logObjectKey, getMeasurementsPoints(), getSettlementsPoints(), getScorePoints());
	}

	/**
	 * @return list of rows with index measurement type number. each row holds the points associated to the trail types with the ordinal as index.
	 */
	private Entries getMeasurementsPoints() {
		List<MeasurementType> channelMeasurements = device.getChannelMeasuremts(super.getChannelConfigNumber());
		//		HashMap<Integer, Integer[]> measurementsPoints = new HashMap<Integer, Integer[]>(channelMeasurements.size());
		Entries entries = new Entries();
		for (int i = 0; i < channelMeasurements.size(); i++) {
			MeasurementType measurementType = channelMeasurements.get(i);
			//			Integer[] trailTypePoints = new Integer[TrailType.getPrimitives().size()];
			//			measurementsPoints.put(i, trailTypePoints);
			EntryPoints entryPoints = new EntryPoints(measurementType.getName(), i);
			entries.getEntryPoints().add(entryPoints);

			Record record = this.get(measurementType.getName());
			StatisticsType measurementStatistics = measurementType.getStatistics();
			if (measurementStatistics != null && record.hasReasonableData()) {
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
					Point point = new Point();
					if (isTriggerLevel)
						entryPoints.addPoint(TrailType.REAL_AVG.name(), TrailType.REAL_AVG.ordinal(), record.getAvgValueTriggered());
					else if (isGpsCoordinates)
						entryPoints.addPoint(TrailType.REAL_AVG.name(), TrailType.REAL_AVG.ordinal(), record.getAvgValue());
					else
						entryPoints.addPoint(TrailType.REAL_AVG.name(), TrailType.REAL_AVG.ordinal(), triggerRefOrdinal < 0 ? record.getAvgValue() : (Integer) record.getAvgValueTriggered(triggerRefOrdinal));
				}
				if (measurementStatistics.isCountByTrigger() != null) {
					entryPoints.addPoint(TrailType.REAL_COUNT_TRIGGERED.name(), TrailType.REAL_COUNT_TRIGGERED.ordinal(), record.getTriggerRanges() != null ? record.getTriggerRanges().size() : 0);
				}
				if (measurementStatistics.isMin()) {
					if (isTriggerLevel)
						entryPoints.addPoint(TrailType.REAL_MIN.name(), TrailType.REAL_MIN.ordinal(), record.getMinValueTriggered());
					else if (triggerRefOrdinal < 0 || record.getMinValueTriggered(triggerRefOrdinal) != Integer.MAX_VALUE) if (isGpsCoordinates)
						entryPoints.addPoint(TrailType.REAL_MIN.name(), TrailType.REAL_MIN.ordinal(), record.getRealMinValue());
					else
						entryPoints.addPoint(TrailType.REAL_MIN.name(), TrailType.REAL_MIN.ordinal(), triggerRefOrdinal < 0 ? record.getRealMinValue() : (Integer) record.getMinValueTriggered(triggerRefOrdinal));
				}
				if (measurementStatistics.isMax()) {
					if (isTriggerLevel)
						entryPoints.addPoint(TrailType.REAL_MAX.name(), TrailType.REAL_MAX.ordinal(), record.getMaxValueTriggered());
					else if (triggerRefOrdinal < 0 || record.getMaxValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE) if (isGpsCoordinates)
						entryPoints.addPoint(TrailType.REAL_MAX.name(), TrailType.REAL_MAX.ordinal(), record.getRealMaxValue());
					else
						entryPoints.addPoint(TrailType.REAL_MAX.name(), TrailType.REAL_MAX.ordinal(), triggerRefOrdinal < 0 ? record.getRealMaxValue() : (Integer) record.getMaxValueTriggered(triggerRefOrdinal));
				}
				if (measurementStatistics.isSigma()) {
					if (isTriggerLevel)
						entryPoints.addPoint(TrailType.REAL_SD.name(), TrailType.REAL_SD.ordinal(), record.getSigmaValueTriggered());
					else
						entryPoints.addPoint(TrailType.REAL_SD.name(), TrailType.REAL_SD.ordinal(),
								triggerRefOrdinal < 0 ? (Integer) record.getSigmaValue() : (Integer) record.getSigmaValueTriggered(triggerRefOrdinal));
				}
				if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
					if (measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1)
						entryPoints.addPoint(TrailType.REAL_SUM_TRIGGERED.name(), TrailType.REAL_SUM_TRIGGERED.ordinal(), record.getSumTriggeredRange());
					else if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
						entryPoints.addPoint(TrailType.REAL_SUM_TRIGGERED.name(), TrailType.REAL_SUM_TRIGGERED.ordinal(), record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal()));
					}
					if (measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1 && measurementStatistics.getRatioRefOrdinal() != null) {
						Record referencedRecord = this.get(measurementStatistics.getRatioRefOrdinal().intValue());
						StatisticsType referencedStatistics = this.device.getMeasurementStatistic(this.getChannelConfigNumber(), measurementStatistics.getRatioRefOrdinal());
						if (referencedRecord != null && (referencedStatistics.isAvg() || referencedStatistics.isMax()))
							// todo trigger ratio is multiplied by 1000 (per mille)
							if (referencedStatistics.isAvg())
							entryPoints.addPoint(TrailType.REAL_AVG_RATIO_TRIGGERED.name(), TrailType.REAL_AVG_RATIO_TRIGGERED.ordinal(), (int) Math.round(referencedRecord.getAvgValue() * 1000.0 / record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal())));
							else if (referencedStatistics.isMax()) //
								entryPoints.addPoint(TrailType.REAL_MAX_RATIO_TRIGGERED.name(), TrailType.REAL_MAX_RATIO_TRIGGERED.ordinal(),
										(int) Math.round(referencedRecord.getMaxValue() * 1000.0 / record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal())));
					}
				}
				if (measurementStatistics.getTrigger() != null && measurementStatistics.getSumTriggerTimeText() != null && measurementStatistics.getSumTriggerTimeText().length() > 1) {
					entryPoints.addPoint(TrailType.REAL_TIME_SUM_TRIGGERED.name(), TrailType.REAL_TIME_SUM_TRIGGERED.ordinal(), record.getTimeSumTriggeredRange_ms());
				}
			}
			else {
				// todo stops here for 'Batt Low'
				// new UnsupportedOperationException();
			}
			entryPoints.addPoint(TrailType.REAL_FIRST.name(), TrailType.REAL_FIRST.ordinal(), record.get(0));
			entryPoints.addPoint(TrailType.REAL_LAST.name(), TrailType.REAL_LAST.ordinal(), record.get(record.size() - 1));
			if (settings.isQuantilesActive()) {
				Quantile quantile = new Quantile(record, this.isSampled() ? EnumSet.of(Fixings.IS_SAMPLE) : EnumSet.noneOf(Fixings.class));
				entryPoints.addPoint(TrailType.Q0.name(), TrailType.Q0.ordinal(), (int) quantile.getQuartile0());
				entryPoints.addPoint(TrailType.Q1.name(), TrailType.Q1.ordinal(), (int) quantile.getQuartile1());
				entryPoints.addPoint(TrailType.Q2.name(), TrailType.Q2.ordinal(), (int) quantile.getQuartile2());
				entryPoints.addPoint(TrailType.Q3.name(), TrailType.Q3.ordinal(), (int) quantile.getQuartile3());
				entryPoints.addPoint(TrailType.Q4.name(), TrailType.Q4.ordinal(), (int) quantile.getQuartile4());
				entryPoints.addPoint(TrailType.Q_25_PERMILLE.name(), TrailType.Q_25_PERMILLE.ordinal(), (int) quantile.getQuantile(.025));
				entryPoints.addPoint(TrailType.Q_975_PERMILLE.name(), TrailType.Q_975_PERMILLE.ordinal(), (int) quantile.getQuantile(.975));
				entryPoints.addPoint(TrailType.Q_LOWER_WHISKER.name(), TrailType.Q_LOWER_WHISKER.ordinal(), (int) quantile.getQuantileLowerWhisker());
				entryPoints.addPoint(TrailType.Q_UPPER_WHISKER.name(), TrailType.Q_UPPER_WHISKER.ordinal(), (int) quantile.getQuantileUpperWhisker());
			}

			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, record.getName() + " data " + entryPoints.toString()); //$NON-NLS-1$
		}
		return entries;
	}

	/**
	 * @return list of rows with index settlementId. each row holds the points associated to the trail types with the ordinal as index.
	 */
	private Entries getSettlementsPoints() {
		List<SettlementType> channelSettlements = ((DeviceConfiguration) device).getChannelSettlements(super.getChannelConfigNumber());
		//		HashMap<Integer, Integer[]> settlementsPoints = new HashMap<Integer, Integer[]>(channelSettlements.size());
		Entries entries = new Entries();
		for (int i = 0; i < channelSettlements.size(); i++) {
			SettlementType settlementType = channelSettlements.get(i);
			//			Integer[] trailTypePoints = new Integer[TrailType.getPrimitives().size()];
			//			settlementsPoints.put(settlementType.getSettlementId(), trailTypePoints);
			EntryPoints entryPoints = new EntryPoints(settlementType.getName(), i);
			entries.getEntryPoints().add(entryPoints);

			HistoSettlement record = this.histoSettlements.get(settlementType.getName());
			if (settlementType.getEvaluation() != null) { // evaluation and scores are choices
				EvaluationType settlementEvaluations = settlementType.getEvaluation();
				if (record.hasReasonableData()) {
					if (settlementEvaluations.isAvg()) entryPoints.addPoint(TrailType.REAL_AVG.name(), TrailType.REAL_AVG.ordinal(), record.getAvgValue());
					if (settlementEvaluations.isFirst()) entryPoints.addPoint(TrailType.REAL_FIRST.name(), TrailType.REAL_FIRST.ordinal(), record.get(0));
					if (settlementEvaluations.isLast()) entryPoints.addPoint(TrailType.REAL_LAST.name(), TrailType.REAL_LAST.ordinal(), record.get(record.size() - 1));
					if (settlementEvaluations.isMax()) entryPoints.addPoint(TrailType.REAL_MAX.name(), TrailType.REAL_MAX.ordinal(), record.getMaxValue());
					if (settlementEvaluations.isMin()) entryPoints.addPoint(TrailType.REAL_MIN.name(), TrailType.REAL_MIN.ordinal(), record.getMinValue());
					if (settlementEvaluations.isSigma()) entryPoints.addPoint(TrailType.REAL_SD.name(), TrailType.REAL_SD.ordinal(), record.getSigmaValue());
					if (settlementEvaluations.isSum()) entryPoints.addPoint(TrailType.REAL_SUM.name(), TrailType.REAL_SUM.ordinal(), record.getSumValue());
				}
				else {
					// todo check why UltraTrioPlus internal resistance has no elements
					//throw new UnsupportedOperationException();
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
		this.scorePoints[ScoreLabelTypes.ELAPSED_HISTO_VAULT_WRITE_MS.ordinal()] = (int) TimeUnit.NANOSECONDS.toMicros(this.elapsedHistoVaultWrite_ns); // do not multiply by 1000 as usual, this is the conversion from ns to ms

		EntryPoints entryPoints = new EntryPoints("All", 0);
		StringBuilder sb = new StringBuilder();
		for (ScoreLabelTypes scoreLabelTypes : EnumSet.allOf(ScoreLabelTypes.class)) {
			if (this.scorePoints[scoreLabelTypes.ordinal()] != null) {
				entryPoints.addPoint(scoreLabelTypes.toString(), scoreLabelTypes.ordinal(), this.scorePoints[scoreLabelTypes.ordinal()]);
				if (log.isLoggable(Level.FINE)) {
					sb.append(scoreLabelTypes.toString().toLowerCase()).append("=").append(this.scorePoints[scoreLabelTypes.ordinal()]).append(" ");
				}
			}
		}
		log.log(Level.SEVERE, entryPoints.toString()); //$NON-NLS-1$
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

}
