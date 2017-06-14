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

package gde.histo.cache;

import java.io.File;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Logger;

import gde.data.HistoSettlement;
import gde.data.HistoTransitions;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.ChannelPropertyType;
import gde.device.ChannelPropertyTypes;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.ScoreLabelTypes;
import gde.device.SettlementType;
import gde.device.StatisticsType;
import gde.device.TrailTypes;
import gde.device.TransitionAmountType;
import gde.device.TransitionCalculusType;
import gde.device.TransitionFigureType;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.UniversalQuantile;

/**
 * Supports the data collection for a serializable histo vault.
 * @author Thomas Eickert
 */
public final class VaultCollector {
	final private static String	$CLASS_NAME	= VaultCollector.class.getName();
	final private static Logger	log					= Logger.getLogger($CLASS_NAME);

	private HistoVault					histoVault;

	private final DataExplorer	application	= DataExplorer.getInstance();
	private final IDevice				device			= this.application.getActiveDevice();

	/**
	 * @param newVault is taken as the new collector target
	 */
	public VaultCollector(HistoVault newVault) {
		this.histoVault = newVault;
	}

	/**
	 * @param objectDirectory validated object key
	 * @param file is the bin origin file (not a link file)
	 * @param fileVersion is the version of the log origin file
	 * @param logRecordSetSize is the number of recordsets in the log origin file
	 * @param logRecordsetBaseName the base name without recordset number
	 */
	public VaultCollector(String objectDirectory, File file, int fileVersion, int logRecordSetSize, String logRecordsetBaseName) {
		this(objectDirectory, file, fileVersion, logRecordSetSize, 0, logRecordsetBaseName, "native", file.lastModified(), DataExplorer.getInstance().getActiveChannelNumber(), //$NON-NLS-1$
				objectDirectory);
	}

	/**
	 * @param objectDirectory validated object key
	 * @param file is the log origin file (not a link file)
	 * @param fileVersion is the version of the log origin file
	 * @param logRecordSetSize is the number of recordsets in the log origin file
	 * @param logRecordSetOrdinal identifies multiple recordsets in one single file (0-based)
	 * @param logRecordsetBaseName the base name without recordset number
	 * @param logStartTimestamp_ms of the log or recordset
	 * @param logDeviceName
	 * @param logChannelNumber may differ from UI settings in case of channel mix
	 * @param logObjectKey may differ from UI settings (empty in OSD files, validated parent path for bin files)
	 */
	public VaultCollector(String objectDirectory, File file, int fileVersion, int logRecordSetSize, int logRecordSetOrdinal, String logRecordsetBaseName, String logDeviceName, long logStartTimestamp_ms,
			int logChannelNumber, String logObjectKey) {
		this.histoVault = new HistoVault(objectDirectory, file.toPath(), file.lastModified(), file.length(), fileVersion, logRecordSetSize, logRecordSetOrdinal, logRecordsetBaseName, logDeviceName,
				logStartTimestamp_ms, logChannelNumber, logObjectKey);
	}

	/**
	 * Makes a full vault from the truss.
	 * @param recordSet
	 * @param scorePoints
	 */
	public void promoteTruss(RecordSet recordSet, Integer[] scorePoints) {
		if (recordSet.getRecordDataSize(true) > 0) {
			this.histoVault.logStartTimestampMs = recordSet.getStartTimeStamp();

			boolean isSampled = scorePoints[ScoreLabelTypes.TOTAL_READINGS.ordinal()] != null && scorePoints[ScoreLabelTypes.TOTAL_READINGS.ordinal()] > recordSet.getRecordDataSize(true);
			setMeasurementPoints(recordSet, isSampled);

			{
				HistoTransitions transitions = new HistoTransitions(recordSet);
				transitions.add4Channel(this.histoVault.logChannelNumber);

				LinkedHashMap<String, HistoSettlement> histoSettlements;
				if (transitions.getTransitionCount() > 0) { // todo implement evaluations w/o transitions
					histoSettlements = determineSettlements(recordSet, transitions);
					setSettlements(histoSettlements);
				}
			}

			complementScorePoints(recordSet, scorePoints);
			setScorePoints(scorePoints);
		}
	}

	/**
	 * Add the measurements points from the records to the vault.
	 * @param recordSet
	 * @param isSampled
	 */
	private void setMeasurementPoints(RecordSet recordSet, boolean isSampled) {
		List<MeasurementType> channelMeasurements = this.device.getChannelMeasuremts(this.histoVault.getLogChannelNumber());
		for (int i = 0; i < recordSet.getRecordNames().length; i++) {
			MeasurementType measurementType = channelMeasurements.get(i);
			Record record = recordSet.get(recordSet.getRecordNames()[i]);

			CompartmentType entryPoints = new CompartmentType(i, measurementType.getName(), DataTypes.fromDataType(record.getDataType()));
			this.histoVault.getMeasurements().put(i, entryPoints);
			entryPoints.setTrails(new HashMap<Integer, PointType>());
			Map<TrailTypes, Integer> trailPoints = new HashMap<>();

			if (record == null) {
				if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, String.format("no record found for measurementType.getName()=%s %s", measurementType.getName(), this.histoVault.getLogFilePath())); //$NON-NLS-1$
			}
			else if (!record.hasReasonableData()) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("no reasonable data for measurementType.getName()=%s %s", measurementType.getName(), this.histoVault.getLogFilePath())); //$NON-NLS-1$
			}
			else {
				StatisticsType measurementStatistics = measurementType.getStatistics();
				if (measurementStatistics != null) { // todo this creates trail types mismatch  :  && record.hasReasonableData()) {
					trailPoints.putAll(getTrailStatisticsPoints(record, measurementStatistics, recordSet));
				}
				trailPoints.putAll(getTrailPoints(record, isSampled));
			}

			for (Entry<TrailTypes, Integer> entry : trailPoints.entrySet()) {
				entryPoints.addPoint(entry.getKey(), entry.getValue());
			}
			log.log(Level.FINER, record.getName() + " data ", entryPoints); //$NON-NLS-1$
		}
	}

	/**
	 * @param recordSet
	 * @param transitions
	 * @return the calculated settlements calculated from transitions.
	 */
	private LinkedHashMap<String, HistoSettlement> determineSettlements(RecordSet recordSet, HistoTransitions transitions) {
		LinkedHashMap<String, HistoSettlement> histoSettlements = new LinkedHashMap<String, HistoSettlement>();

		for (SettlementType settlementType : this.device.getDeviceConfiguration().getChannel(this.histoVault.logChannelNumber).getSettlements().values()) {
			if (settlementType.getEvaluation() != null) {
				final TransitionFigureType transitionFigureType = settlementType.getEvaluation().getTransitionFigure();
				final TransitionAmountType transitionAmountType = settlementType.getEvaluation().getTransitionAmount();
				final TransitionCalculusType calculationType = settlementType.getEvaluation().getTransitionCalculus();

				if (transitionFigureType != null) {
					HistoSettlement histoSettlement = new HistoSettlement(settlementType, recordSet, this.histoVault.logChannelNumber);
					histoSettlement.addFromTransitions(transitions.getTransitions(transitionFigureType.getTransitionGroupId()).values());
					histoSettlements.put(settlementType.getName(), histoSettlement);
				}
				else if (transitionAmountType != null) {
					HistoSettlement histoSettlement = new HistoSettlement(settlementType, recordSet, this.histoVault.logChannelNumber);
					histoSettlement.addFromTransitions(transitions.getTransitions(transitionAmountType.getTransitionGroupId()).values());
					histoSettlements.put(settlementType.getName(), histoSettlement);
				}
				else if (calculationType != null) {
					HistoSettlement histoSettlement = new HistoSettlement(settlementType, recordSet, this.histoVault.logChannelNumber);
					histoSettlement.addFromTransitions(transitions.getTransitions(calculationType.getTransitionGroupId()).values());
					histoSettlements.put(settlementType.getName(), histoSettlement);
				}
				else {
					throw new UnsupportedOperationException();
				}
			}
		}

		return histoSettlements;
	}

	/**
	 * @param record
	 * @param measurementStatistics
	 * @param recordSet
	 * @return the list of point values for each trail type marked as 'real'
	 */
	private Map<TrailTypes, Integer> getTrailStatisticsPoints(Record record, StatisticsType measurementStatistics, RecordSet recordSet) {
		final Map<TrailTypes, Integer> trailPoints = new HashMap<>();

		int triggerRefOrdinal = -1;
		if (measurementStatistics.getTriggerRefOrdinal() != null) {
			int tmpOrdinal = measurementStatistics.getTriggerRefOrdinal().intValue();
			//todo	if (record.isDisplayable()) { the record may be displayable later --- but note that calculating trigger ranges requires displayable true
			triggerRefOrdinal = tmpOrdinal;
			//								}
		}
		boolean isTriggerLevel = measurementStatistics.getTrigger() != null;
		if (measurementStatistics.isAvg()) {
			if (isTriggerLevel)
				trailPoints.put(TrailTypes.REAL_AVG, record.getAvgValueTriggered() != Integer.MIN_VALUE ? record.getAvgValueTriggered() : 0);
			else if (triggerRefOrdinal < 0 || record.getAvgValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE) //
				trailPoints.put(TrailTypes.REAL_AVG, triggerRefOrdinal < 0 ? record.getAvgValue() : (Integer) record.getAvgValueTriggered(triggerRefOrdinal));
		}
		else {
			trailPoints.put(TrailTypes.REAL_AVG, record.getAvgValue());
		}
		if (measurementStatistics.isMax()) {
			if (isTriggerLevel)
				trailPoints.put(TrailTypes.REAL_MAX, record.getMaxValueTriggered());
			else if (triggerRefOrdinal < 0 || record.getMaxValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE) //
				trailPoints.put(TrailTypes.REAL_MAX, triggerRefOrdinal < 0 ? record.getRealMaxValue() : (Integer) record.getMaxValueTriggered(triggerRefOrdinal));
		}
		else {
			trailPoints.put(TrailTypes.REAL_MAX, record.getRealMaxValue());
		}
		if (measurementStatistics.isMin()) {
			if (isTriggerLevel)
				trailPoints.put(TrailTypes.REAL_MIN, record.getMinValueTriggered());
			else if (triggerRefOrdinal < 0 || record.getMinValueTriggered(triggerRefOrdinal) != Integer.MAX_VALUE) //
				trailPoints.put(TrailTypes.REAL_MIN, triggerRefOrdinal < 0 ? record.getRealMinValue() : (Integer) record.getMinValueTriggered(triggerRefOrdinal));
		}
		else {
			trailPoints.put(TrailTypes.REAL_MIN, record.getRealMinValue());
		}
		if (measurementStatistics.isSigma()) {
			if (isTriggerLevel)
				trailPoints.put(TrailTypes.REAL_SD, record.getSigmaValueTriggered());
			else if (triggerRefOrdinal < 0 || record.getSigmaValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE) //
				trailPoints.put(TrailTypes.REAL_SD, triggerRefOrdinal < 0 ? (Integer) record.getSigmaValue() : (Integer) record.getSigmaValueTriggered(triggerRefOrdinal));
		}
		else {
			trailPoints.put(TrailTypes.REAL_SD, record.getSigmaValue());
		}
		if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
			if (measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1) {
				if (isTriggerLevel) {
					int deltaValue = record.getSumTriggeredRange();
					double translatedValue = this.device.translateDeltaValue(record, deltaValue / 1000.); // standard division by 1000 ensures correct reduction / offset
					trailPoints.put(TrailTypes.REAL_SUM_TRIGGERED, (int) (this.device.reverseTranslateValue(record, translatedValue) * 1000.));
				}
				else if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
					int deltaValue = record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal());
					double translatedValue = this.device.translateDeltaValue(record, deltaValue / 1000.); // standard division by 1000 ensures correct reduction / offset
					trailPoints.put(TrailTypes.REAL_SUM_TRIGGERED, (int) (this.device.reverseTranslateValue(record, translatedValue) * 1000.));
				}
			}
			if (measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1 && measurementStatistics.getRatioRefOrdinal() != null) {
				Record referencedRecord = recordSet.get(measurementStatistics.getRatioRefOrdinal().intValue());
				StatisticsType referencedStatistics = this.device.getMeasurementStatistic(this.histoVault.getLogChannelNumber(), measurementStatistics.getRatioRefOrdinal());
				if (referencedRecord != null && (referencedStatistics.isAvg() || referencedStatistics.isMax())) {
					if (referencedStatistics.isAvg()) {
						double ratio = this.device.translateValue(referencedRecord, referencedRecord.getAvgValueTriggered(measurementStatistics.getRatioRefOrdinal()) / 1000.)
								/ this.device.translateDeltaValue(record, record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal().intValue()) / 1000.);
						trailPoints.put(TrailTypes.REAL_MAX_RATIO_TRIGGERED, (int) (this.device.reverseTranslateValue(record, ratio) * 1000.)); // multiply by 1000 -> all ratios are internally stored multiplied by thousand
					}
					else if (referencedStatistics.isMax()) {
						double ratio = this.device.translateValue(referencedRecord, referencedRecord.getMaxValueTriggered(measurementStatistics.getRatioRefOrdinal()) / 1000.)
								/ this.device.translateDeltaValue(record, record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal().intValue()) / 1000.);
						trailPoints.put(TrailTypes.REAL_MAX_RATIO_TRIGGERED, (int) (this.device.reverseTranslateValue(record, ratio) * 1000.)); // multiply by 1000 -> all ratios are internally stored multiplied by thousand
					}
				}
			}
		}
		if (measurementStatistics.getTrigger() != null && measurementStatistics.getSumTriggerTimeText() != null && measurementStatistics.getSumTriggerTimeText().length() > 1) {
			int timeSum_ms = record.getTimeSumTriggeredRange_ms();
			trailPoints.put(TrailTypes.REAL_TIME_SUM_TRIGGERED, (int) this.device.reverseTranslateValue(record, timeSum_ms)); // do not multiply by 1000 -> results in seconds
		}
		if (measurementStatistics.isCountByTrigger() != null) {
			int countValue = record.getTriggerRanges() != null ? record.getTriggerRanges().size() : 0;
			trailPoints.put(TrailTypes.REAL_COUNT_TRIGGERED, (int) (this.device.reverseTranslateValue(record, countValue) * 1000.)); // multiply by 1000 -> all counters are internally stored multiplied by thousand
		}

		return trailPoints;
	}

	/**
	 * @param record
	 * @param isSampled
	 * @return the list of point values for each standard trail type
	 */
	private Map<TrailTypes, Integer> getTrailPoints(Record record, boolean isSampled) {
		final Map<TrailTypes, Integer> trailPoints = new HashMap<>();

		trailPoints.put(TrailTypes.REAL_FIRST, record.elementAt(0));
		trailPoints.put(TrailTypes.REAL_LAST, record.elementAt(record.size() - 1));

		final ChannelPropertyType channelProperty = this.device.getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.OUTLIER_SIGMA);
		final double sigmaFactor = channelProperty.getValue() != null && !channelProperty.getValue().isEmpty() ? Double.parseDouble(channelProperty.getValue()) : HistoSettlement.outlierSigmaDefault;
		final ChannelPropertyType channelProperty2 = this.device.getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.OUTLIER_RANGE_FACTOR);
		final double outlierFactor = channelProperty2.getValue() != null && !channelProperty2.getValue().isEmpty() ? Double.parseDouble(channelProperty2.getValue())
				: HistoSettlement.outlierRangeFactorDefault;
		// invoke translation because of GPS coordinates (decimal fraction range is x.0000 to x.5999 [recurring decimal])
		UniversalQuantile<Double> quantile = new UniversalQuantile<>(record.getTranslatedValues(), isSampled, sigmaFactor, outlierFactor);
		trailPoints.put(TrailTypes.AVG, (int) (this.device.reverseTranslateValue(record, quantile.getAvgFigure()) * 1000.));
		trailPoints.put(TrailTypes.MAX, (int) (this.device.reverseTranslateValue(record, quantile.getPopulationMaxFigure()) * 1000.));
		trailPoints.put(TrailTypes.MIN, (int) (this.device.reverseTranslateValue(record, quantile.getPopulationMinFigure()) * 1000.));
		trailPoints.put(TrailTypes.SD, (int) (this.device.reverseTranslateValue(record, quantile.getSigmaFigure()) * 1000.));
		trailPoints.put(TrailTypes.Q0, (int) (this.device.reverseTranslateValue(record, quantile.getQuartile0()) * 1000.));
		trailPoints.put(TrailTypes.Q1, (int) (this.device.reverseTranslateValue(record, quantile.getQuartile1()) * 1000.));
		trailPoints.put(TrailTypes.Q2, (int) (this.device.reverseTranslateValue(record, quantile.getQuartile2()) * 1000.));
		trailPoints.put(TrailTypes.Q3, (int) (this.device.reverseTranslateValue(record, quantile.getQuartile3()) * 1000.));
		trailPoints.put(TrailTypes.Q4, (int) (this.device.reverseTranslateValue(record, quantile.getQuartile4()) * 1000.));
		trailPoints.put(TrailTypes.Q_25_PERMILLE, (int) (this.device.reverseTranslateValue(record, quantile.getQuantile(.025)) * 1000.));
		trailPoints.put(TrailTypes.Q_975_PERMILLE, (int) (this.device.reverseTranslateValue(record, quantile.getQuantile(.975)) * 1000.));
		trailPoints.put(TrailTypes.Q_LOWER_WHISKER, (int) (this.device.reverseTranslateValue(record, quantile.getQuantileLowerWhisker()) * 1000.));
		trailPoints.put(TrailTypes.Q_UPPER_WHISKER, (int) (this.device.reverseTranslateValue(record, quantile.getQuantileUpperWhisker()) * 1000.));

		trailPoints.put(TrailTypes.FIRST, (int) (this.device.reverseTranslateValue(record, quantile.getFirstFigure()) * 1000.));
		trailPoints.put(TrailTypes.LAST, (int) (this.device.reverseTranslateValue(record, quantile.getLastFigure()) * 1000.));
		// trigger trail types sum are not supported for measurements
		trailPoints.put(TrailTypes.SUM, (int) (0 * 1000.));
		trailPoints.put(TrailTypes.COUNT, (int) (this.device.reverseTranslateValue(record, quantile.getSize()) * 1000.));

		return trailPoints;
	}

	/**
	 * Add the settlements points from the histo settlements to the vault.
	 * @param histoSettlements
	 */
	private void setSettlements(LinkedHashMap<String, HistoSettlement> histoSettlements) {
		for (Entry<String, HistoSettlement> entry : histoSettlements.entrySet()) {
			HistoSettlement histoSettlement = entry.getValue();
			SettlementType settlementType = histoSettlement.getSettlement();
			CompartmentType entryPoints = new CompartmentType(settlementType.getSettlementId(), settlementType.getName(), DataTypes.DEFAULT);
			this.histoVault.getSettlements().put(settlementType.getSettlementId(), entryPoints);
			entryPoints.setTrails(new HashMap<Integer, PointType>());

			if (histoSettlement.realSize() != 0 && histoSettlement.hasReasonableData()) {
				entryPoints.addPoint(TrailTypes.REAL_FIRST, histoSettlement.elementAt(0));
				entryPoints.addPoint(TrailTypes.REAL_LAST, histoSettlement.elementAt(histoSettlement.realSize() - 1));

				final ChannelPropertyType channelProperty = this.device.getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.OUTLIER_SIGMA);
				final double sigmaFactor = channelProperty.getValue() != null && !channelProperty.getValue().isEmpty() ? Double.parseDouble(channelProperty.getValue()) : HistoSettlement.outlierSigmaDefault;
				final ChannelPropertyType channelProperty2 = this.device.getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.OUTLIER_RANGE_FACTOR);
				final double outlierFactor = channelProperty2.getValue() != null && !channelProperty2.getValue().isEmpty() ? Double.parseDouble(channelProperty2.getValue())
						: HistoSettlement.outlierRangeFactorDefault;
				UniversalQuantile<Double> quantile = new UniversalQuantile<>(histoSettlement.getTranslatedValues(), true, sigmaFactor, outlierFactor);

				entryPoints.addPoint(TrailTypes.REAL_AVG, (int) (histoSettlement.reverseTranslateValue(quantile.getAvgFigure()) * 1000.));
				entryPoints.addPoint(TrailTypes.REAL_MAX, (int) (histoSettlement.reverseTranslateValue(quantile.getPopulationMaxFigure()) * 1000.));
				entryPoints.addPoint(TrailTypes.REAL_MIN, (int) (histoSettlement.reverseTranslateValue(quantile.getPopulationMinFigure()) * 1000.));
				entryPoints.addPoint(TrailTypes.REAL_SD, (int) (histoSettlement.reverseTranslateValue(quantile.getSigmaFigure()) * 1000.));
				entryPoints.addPoint(TrailTypes.REAL_SUM, (int) (histoSettlement.reverseTranslateValue(quantile.getSumFigure()) * 1000.));
				entryPoints.addPoint(TrailTypes.REAL_COUNT, (int) (histoSettlement.reverseTranslateValue(quantile.getSize() * 1000) * 1000.));

				entryPoints.addPoint(TrailTypes.AVG, (int) (histoSettlement.reverseTranslateValue(quantile.getAvgFigure()) * 1000.));
				entryPoints.addPoint(TrailTypes.MAX, (int) (histoSettlement.reverseTranslateValue(quantile.getPopulationMaxFigure()) * 1000.));
				entryPoints.addPoint(TrailTypes.MIN, (int) (histoSettlement.reverseTranslateValue(quantile.getPopulationMinFigure()) * 1000.));
				entryPoints.addPoint(TrailTypes.SD, (int) (histoSettlement.reverseTranslateValue(quantile.getSigmaFigure()) * 1000.));
				entryPoints.addPoint(TrailTypes.Q0, (int) (histoSettlement.reverseTranslateValue(quantile.getQuartile0()) * 1000.));
				entryPoints.addPoint(TrailTypes.Q1, (int) (histoSettlement.reverseTranslateValue(quantile.getQuartile1()) * 1000.));
				entryPoints.addPoint(TrailTypes.Q2, (int) (histoSettlement.reverseTranslateValue(quantile.getQuartile2()) * 1000.));
				entryPoints.addPoint(TrailTypes.Q3, (int) (histoSettlement.reverseTranslateValue(quantile.getQuartile3()) * 1000.));
				entryPoints.addPoint(TrailTypes.Q4, (int) (histoSettlement.reverseTranslateValue(quantile.getQuartile4()) * 1000.));
				entryPoints.addPoint(TrailTypes.Q_25_PERMILLE, (int) (histoSettlement.reverseTranslateValue(quantile.getQuantile(.025)) * 1000.));
				entryPoints.addPoint(TrailTypes.Q_975_PERMILLE, (int) (histoSettlement.reverseTranslateValue(quantile.getQuantile(.975)) * 1000.));
				entryPoints.addPoint(TrailTypes.Q_LOWER_WHISKER, (int) (histoSettlement.reverseTranslateValue(quantile.getQuantileLowerWhisker()) * 1000.));
				entryPoints.addPoint(TrailTypes.Q_UPPER_WHISKER, (int) (histoSettlement.reverseTranslateValue(quantile.getQuantileUpperWhisker()) * 1000.));

				entryPoints.addPoint(TrailTypes.FIRST, (int) (histoSettlement.reverseTranslateValue(quantile.getFirstFigure()) * 1000.));
				entryPoints.addPoint(TrailTypes.LAST, (int) (histoSettlement.reverseTranslateValue(quantile.getLastFigure()) * 1000.));
				entryPoints.addPoint(TrailTypes.SUM, (int) (histoSettlement.reverseTranslateValue(quantile.getSumFigure()) * 1000.));
				entryPoints.addPoint(TrailTypes.COUNT, (int) (histoSettlement.reverseTranslateValue(quantile.getSize()) * 1000.));
			}
			log.log(Level.FINER, histoSettlement.getName() + " data ", this.histoVault.getSettlements()); //$NON-NLS-1$
		}
	}

	/**
	 * Fill in score points calculated from recordset data.
	 * @param recordSet
	 * @param scorePoints
	 */
	private void complementScorePoints(RecordSet recordSet, Integer[] scorePoints) {
		scorePoints[ScoreLabelTypes.DURATION_MM.ordinal()] = (int) (recordSet.getMaxTime_ms() / 60000. * 1000. + .5);
		scorePoints[ScoreLabelTypes.AVERAGE_TIME_STEP_MS.ordinal()] = (int) (recordSet.getAverageTimeStep_ms() * 1000.);
		scorePoints[ScoreLabelTypes.MAXIMUM_TIME_STEP_MS.ordinal()] = (int) (recordSet.getMaximumTimeStep_ms() * 1000.);
		scorePoints[ScoreLabelTypes.MINIMUM_TIME_STEP_MS.ordinal()] = (int) (recordSet.getMinimumTimeStep_ms() * 1000.);
		scorePoints[ScoreLabelTypes.SIGMA_TIME_STEP_MS.ordinal()] = (int) (recordSet.getSigmaTimeStep_ms() * 1000.);
		scorePoints[ScoreLabelTypes.SAMPLED_READINGS.ordinal()] = recordSet.getRecordDataSize(true);
	}

	/**
	 * Add the score points to the vault.
	 * @param scorePoints
	 */
	private void setScorePoints(Integer[] scorePoints) {
		for (ScoreLabelTypes scoreLabelTypes : EnumSet.allOf(ScoreLabelTypes.class)) {
			if (scorePoints[scoreLabelTypes.ordinal()] != null) {
				this.histoVault.getScores().put(scoreLabelTypes.ordinal(), new PointType(scoreLabelTypes.ordinal(), scoreLabelTypes.toString(), scorePoints[scoreLabelTypes.ordinal()]));
			}
		}
		log.log(Level.FINE, "scores ", this.histoVault.getScores()); //$NON-NLS-1$
	}

	/**
	 * @return the name (SHA1 checksum length 40)
	 */
	public String getVaultName() {
		return this.histoVault.getVaultName();
	}

	public String getVaultDeviceName() {
		return this.histoVault.getVaultDeviceName();
	}

	public int getVaultChannelNumber() {
		return this.histoVault.getVaultChannelNumber();
	}

	public String getLogFilePath() {
		return this.histoVault.getLogFilePath();
	}

	public int getLogRecordSetOrdinal() {
		return this.histoVault.getLogRecordSetOrdinal();
	}

	public String getLogRecordsetBaseName() {
		return this.histoVault.getLogRecordsetBaseName();
	}

	public String getLogDeviceName() {
		return this.histoVault.getLogDeviceName();
	}

	public int getLogChannelNumber() {
		return this.histoVault.getLogChannelNumber();
	}

	public String getLogObjectKey() {
		return this.histoVault.getLogObjectKey();
	}

	public long getLogStartTimestamp_ms() {
		return this.histoVault.getLogStartTimestamp_ms();
	}

	/**
	 * @return yyyy-MM-dd HH:mm:ss
	 */
	public String getStartTimeStampFormatted() {
		return this.histoVault.getStartTimeStampFormatted();
	}

	public Path getVaultFileName() {
		return this.histoVault.getVaultFileName();
	}

	/**
	 * @return the non-validated object key or alternatively (if empty) the non-validated object directory
	 */
	public String getRectifiedObjectKey() {
		return this.histoVault.getRectifiedObjectKey();
	}

	/**
	 * @return the validated object key
	 */
	public Optional<String> getValidatedObjectKey() {
		return this.histoVault.getValidatedObjectKey();
	}

	/**
	 * @return boolean value to verify if given object key is valid
	 */
	public boolean isValidObjectKey(final String objectKey) {
		return this.histoVault.isValidObjectKey(objectKey);
	}

	/**
	 * @return the log file path
	 */
	public Path getLogFileAsPath() {
		return this.histoVault.getLogFileAsPath();
	}

	public HistoVault getVault() {
		return this.histoVault;
	}

}
