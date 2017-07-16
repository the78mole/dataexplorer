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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import gde.config.Settings;
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
import gde.histo.settlements.AmountEvaluator;
import gde.histo.settlements.CalculusEvaluator;
import gde.histo.settlements.FigureEvaluator;
import gde.histo.settlements.SettlementRecord;
import gde.histo.transitions.GroupTransitions;
import gde.histo.transitions.Transition;
import gde.histo.utils.UniversalQuantile;
import gde.log.Level;
import gde.ui.DataExplorer;

/**
 * Supports the data collection for a serializable histo vault.
 * @author Thomas Eickert
 */
public final class VaultCollector {
	final private static String	$CLASS_NAME	= VaultCollector.class.getName();
	final private static Logger	log					= Logger.getLogger($CLASS_NAME);

	private ExtendedVault				vault;

	private final DataExplorer	application	= DataExplorer.getInstance();
	private final Settings			settings		= Settings.getInstance();

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
	public VaultCollector(String objectDirectory, File file, int fileVersion, int logRecordSetSize, int logRecordSetOrdinal, String logRecordsetBaseName, String logDeviceName,
			long logStartTimestamp_ms, int logChannelNumber, String logObjectKey) {
		this.vault = new ExtendedVault(objectDirectory, file.toPath(), file.lastModified(), file.length(), fileVersion, logRecordSetSize, logRecordSetOrdinal, logRecordsetBaseName, logDeviceName,
				logStartTimestamp_ms, logChannelNumber, logObjectKey);
	}

	@Override
	public String toString() {
		return String.format("logChannelNumber=%d  logRecordSetOrdinal=%d  startTimestamp=%s  %s", //$NON-NLS-1$
				this.vault.getVaultDeviceName(), this.vault.getLogChannelNumber(), this.vault.getLogRecordSetOrdinal(), this.vault.getStartTimeStampFormatted(), this.vault.getLogFilePath());
	}

	/**
	 * Make a full vault from the truss.
	 * @param recordSet
	 * @param scorePoints
	 */
	public void promoteTruss(RecordSet recordSet, Integer[] scorePoints) {
		if (recordSet.getRecordDataSize(true) > 0) {
			this.vault.logStartTimestampMs = recordSet.getStartTimeStamp();

			boolean isSampled = scorePoints[ScoreLabelTypes.TOTAL_READINGS.ordinal()] != null && scorePoints[ScoreLabelTypes.TOTAL_READINGS.ordinal()] > recordSet.getRecordDataSize(true);
			setMeasurementPoints(recordSet, isSampled);

			{
				GroupTransitions transitions = new GroupTransitions(recordSet);
				transitions.add4Channel(this.vault.logChannelNumber);

				LinkedHashMap<String, SettlementRecord> histoSettlements;
				if (transitions.getTransitionsCount() > 0) {
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
		List<MeasurementType> channelMeasurements = this.application.getActiveDevice().getChannelMeasuremts(this.vault.getLogChannelNumber());
		for (int i = 0; i < recordSet.getRecordNames().length; i++) {
			MeasurementType measurementType = channelMeasurements.get(i);
			Record record = recordSet.get(recordSet.getRecordNames()[i]);

			CompartmentType entryPoints = new CompartmentType(i, measurementType.getName(), DataTypes.fromDataType(record.getDataType()));
			this.vault.getMeasurements().put(i, entryPoints);
			entryPoints.setTrails(new HashMap<Integer, PointType>());
			Map<TrailTypes, Integer> trailPoints = new HashMap<>();

			if (!record.hasReasonableData()) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("no reasonable data for measurementType.getName()=%s %s", measurementType.getName(), this.vault.getLogFilePath())); //$NON-NLS-1$
			}
			else {
				StatisticsType measurementStatistics = measurementType.getStatistics();
				if (measurementStatistics != null) { // this creates trail types mismatch  :  && record.hasReasonableData()) {
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
	private LinkedHashMap<String, SettlementRecord> determineSettlements(RecordSet recordSet, GroupTransitions transitions) {
		LinkedHashMap<String, SettlementRecord> histoSettlements = new LinkedHashMap<String, SettlementRecord>();

		for (SettlementType settlementType : this.application.getActiveDevice().getDeviceConfiguration().getChannel(this.vault.logChannelNumber).getSettlements().values()) {
			if (settlementType.getEvaluation() != null) {
				final TransitionFigureType transitionFigureType = settlementType.getEvaluation().getTransitionFigure();
				final TransitionAmountType transitionAmountType = settlementType.getEvaluation().getTransitionAmount();
				final TransitionCalculusType calculationType = settlementType.getEvaluation().getTransitionCalculus();

				if (transitionFigureType != null) {
					SettlementRecord histoSettlement = new SettlementRecord(settlementType, recordSet, this.vault.logChannelNumber);
					FigureEvaluator evaluator = new FigureEvaluator(histoSettlement);
					for (Transition transition : transitions.get(transitionFigureType.getTransitionGroupId()).values()) {
						evaluator.addFromTransition(transition);
					}
					histoSettlements.put(settlementType.getName(), histoSettlement);
				}
				else if (transitionAmountType != null) {
					SettlementRecord histoSettlement = new SettlementRecord(settlementType, recordSet, this.vault.logChannelNumber);
					AmountEvaluator evaluator = new AmountEvaluator(histoSettlement);
					for (Transition transition : transitions.get(transitionAmountType.getTransitionGroupId()).values()) {
						evaluator.addFromTransition(transition);
					}
					histoSettlements.put(settlementType.getName(), histoSettlement);
				}
				else if (calculationType != null) {
					SettlementRecord histoSettlement = new SettlementRecord(settlementType, recordSet, this.vault.logChannelNumber);
					CalculusEvaluator evaluator = new CalculusEvaluator(histoSettlement);
					for (Transition transition : transitions.get(calculationType.getTransitionGroupId()).values()) {
						evaluator.addFromTransition(transition);
					}
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
		final IDevice device = this.application.getActiveDevice();

		int triggerRefOrdinal = -1;
		if (measurementStatistics.getTriggerRefOrdinal() != null) {
			int tmpOrdinal = measurementStatistics.getTriggerRefOrdinal().intValue();
			// if (record.isDisplayable()) { the record may be displayable later --- but note that calculating trigger ranges requires displayable true
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
					double translatedValue = device.translateDeltaValue(record, deltaValue / 1000.); // standard division by 1000 ensures correct reduction / offset
					trailPoints.put(TrailTypes.REAL_SUM_TRIGGERED, (int) (device.reverseTranslateValue(record, translatedValue) * 1000.));
				}
				else if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
					int deltaValue = record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal());
					double translatedValue = device.translateDeltaValue(record, deltaValue / 1000.); // standard division by 1000 ensures correct reduction / offset
					trailPoints.put(TrailTypes.REAL_SUM_TRIGGERED, (int) (device.reverseTranslateValue(record, translatedValue) * 1000.));
				}
			}
			if (measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1 && measurementStatistics.getRatioRefOrdinal() != null) {
				Record referencedRecord = recordSet.get(measurementStatistics.getRatioRefOrdinal().intValue());
				StatisticsType referencedStatistics = device.getMeasurementStatistic(this.vault.getLogChannelNumber(), measurementStatistics.getRatioRefOrdinal());
				if (referencedRecord != null && (referencedStatistics.isAvg() || referencedStatistics.isMax())) {
					if (referencedStatistics.isAvg()) {
						double ratio = device.translateValue(referencedRecord, referencedRecord.getAvgValueTriggered(measurementStatistics.getRatioRefOrdinal()) / 1000.)
								/ device.translateDeltaValue(record, record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal().intValue()) / 1000.);
						trailPoints.put(TrailTypes.REAL_MAX_RATIO_TRIGGERED, (int) (device.reverseTranslateValue(record, ratio) * 1000.)); // multiply by 1000 -> all ratios are internally stored multiplied by thousand
					}
					else if (referencedStatistics.isMax()) {
						double ratio = device.translateValue(referencedRecord, referencedRecord.getMaxValueTriggered(measurementStatistics.getRatioRefOrdinal()) / 1000.)
								/ device.translateDeltaValue(record, record.getSumTriggeredRange(measurementStatistics.getSumByTriggerRefOrdinal().intValue()) / 1000.);
						trailPoints.put(TrailTypes.REAL_MAX_RATIO_TRIGGERED, (int) (device.reverseTranslateValue(record, ratio) * 1000.)); // multiply by 1000 -> all ratios are internally stored multiplied by thousand
					}
				}
			}
		}
		if (measurementStatistics.getTrigger() != null && measurementStatistics.getSumTriggerTimeText() != null && measurementStatistics.getSumTriggerTimeText().length() > 1) {
			int timeSum_ms = record.getTimeSumTriggeredRange_ms();
			trailPoints.put(TrailTypes.REAL_TIME_SUM_TRIGGERED, (int) device.reverseTranslateValue(record, timeSum_ms)); // do not multiply by 1000 -> results in seconds
		}
		if (measurementStatistics.isCountByTrigger() != null) {
			int countValue = record.getTriggerRanges() != null ? record.getTriggerRanges().size() : 0;
			trailPoints.put(TrailTypes.REAL_COUNT_TRIGGERED, (int) (device.reverseTranslateValue(record, countValue) * 1000.)); // multiply by 1000 -> all counters are internally stored multiplied by thousand
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
		final IDevice device = this.application.getActiveDevice();

		trailPoints.put(TrailTypes.REAL_FIRST, record.elementAt(0));
		trailPoints.put(TrailTypes.REAL_LAST, record.elementAt(record.realSize() - 1));

		final ChannelPropertyType channelProperty = device.getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.OUTLIER_SIGMA);
		final double sigmaFactor = channelProperty.getValue() != null && !channelProperty.getValue().isEmpty() ? Double.parseDouble(channelProperty.getValue()) : SettlementRecord.OUTLIER_SIGMA_DEFAULT;
		final ChannelPropertyType channelProperty2 = device.getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.OUTLIER_RANGE_FACTOR);
		final double outlierFactor = channelProperty2.getValue() != null && !channelProperty2.getValue().isEmpty() ? Double.parseDouble(channelProperty2.getValue())
				: SettlementRecord.OUTLIER_RANGE_FACTOR_DEFAULT;
		// invoke translation because of GPS coordinates (decimal fraction range is x.0000 to x.5999 [recurring decimal])
		UniversalQuantile<Double> quantile = new UniversalQuantile<>(record.getTranslatedValues(), isSampled, sigmaFactor, outlierFactor);
		trailPoints.put(TrailTypes.AVG, (int) (device.reverseTranslateValue(record, quantile.getAvgFigure()) * 1000.));
		trailPoints.put(TrailTypes.MAX, (int) (device.reverseTranslateValue(record, quantile.getPopulationMaxFigure()) * 1000.));
		trailPoints.put(TrailTypes.MIN, (int) (device.reverseTranslateValue(record, quantile.getPopulationMinFigure()) * 1000.));
		trailPoints.put(TrailTypes.SD, (int) (device.reverseTranslateValue(record, quantile.getSigmaFigure()) * 1000.));
		trailPoints.put(TrailTypes.Q0, (int) (device.reverseTranslateValue(record, quantile.getQuartile0()) * 1000.));
		trailPoints.put(TrailTypes.Q1, (int) (device.reverseTranslateValue(record, quantile.getQuartile1()) * 1000.));
		trailPoints.put(TrailTypes.Q2, (int) (device.reverseTranslateValue(record, quantile.getQuartile2()) * 1000.));
		trailPoints.put(TrailTypes.Q3, (int) (device.reverseTranslateValue(record, quantile.getQuartile3()) * 1000.));
		trailPoints.put(TrailTypes.Q4, (int) (device.reverseTranslateValue(record, quantile.getQuartile4()) * 1000.));
		trailPoints.put(TrailTypes.Q_25_PERMILLE, (int) (device.reverseTranslateValue(record, quantile.getQuantile(.025)) * 1000.));
		trailPoints.put(TrailTypes.Q_975_PERMILLE, (int) (device.reverseTranslateValue(record, quantile.getQuantile(.975)) * 1000.));
		trailPoints.put(TrailTypes.Q_LOWER_WHISKER, (int) (device.reverseTranslateValue(record, quantile.getQuantileLowerWhisker()) * 1000.));
		trailPoints.put(TrailTypes.Q_UPPER_WHISKER, (int) (device.reverseTranslateValue(record, quantile.getQuantileUpperWhisker()) * 1000.));

		trailPoints.put(TrailTypes.FIRST, (int) (device.reverseTranslateValue(record, quantile.getFirstFigure()) * 1000.));
		trailPoints.put(TrailTypes.LAST, (int) (device.reverseTranslateValue(record, quantile.getLastFigure()) * 1000.));
		// trigger trail types sum are not supported for measurements
		trailPoints.put(TrailTypes.SUM, (int) (0 * 1000.));
		trailPoints.put(TrailTypes.COUNT, (int) (device.reverseTranslateValue(record, quantile.getSize()) * 1000.));

		return trailPoints;
	}

	/**
	 * Add the settlements points from the histo settlements to the vault.
	 * @param histoSettlements
	 */
	private void setSettlements(LinkedHashMap<String, SettlementRecord> histoSettlements) {
		final IDevice device = this.application.getActiveDevice();
		for (Entry<String, SettlementRecord> entry : histoSettlements.entrySet()) {
			SettlementRecord histoSettlement = entry.getValue();
			SettlementType settlementType = histoSettlement.getSettlement();
			CompartmentType entryPoints = new CompartmentType(settlementType.getSettlementId(), settlementType.getName(), DataTypes.DEFAULT);
			this.vault.getSettlements().put(settlementType.getSettlementId(), entryPoints);
			entryPoints.setTrails(new HashMap<Integer, PointType>());

			if (histoSettlement.realSize() != 0 && histoSettlement.hasReasonableData()) {
				entryPoints.addPoint(TrailTypes.REAL_FIRST, histoSettlement.elementAt(0));
				entryPoints.addPoint(TrailTypes.REAL_LAST, histoSettlement.elementAt(histoSettlement.realSize() - 1));

				final ChannelPropertyType channelProperty = device.getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.OUTLIER_SIGMA);
				final double sigmaFactor = channelProperty.getValue() != null && !channelProperty.getValue().isEmpty() ? Double.parseDouble(channelProperty.getValue())
						: SettlementRecord.OUTLIER_SIGMA_DEFAULT;
				final ChannelPropertyType channelProperty2 = device.getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.OUTLIER_RANGE_FACTOR);
				final double outlierFactor = channelProperty2.getValue() != null && !channelProperty2.getValue().isEmpty() ? Double.parseDouble(channelProperty2.getValue())
						: SettlementRecord.OUTLIER_RANGE_FACTOR_DEFAULT;
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
			log.log(Level.FINER, histoSettlement.getName() + " data ", this.vault.getSettlements()); //$NON-NLS-1$
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
				this.vault.getScores().put(scoreLabelTypes.ordinal(), new PointType(scoreLabelTypes.ordinal(), scoreLabelTypes.toString(), scorePoints[scoreLabelTypes.ordinal()]));
			}
		}
		log.log(Level.FINE, "scores ", this.vault.getScores()); //$NON-NLS-1$
	}

	public boolean isConsistentDevice() {
		if (this.application.getActiveDevice() != null && !this.vault.getLogDeviceName().equals(this.application.getActiveDevice().getName())
				&& !(this.vault.logDeviceName.startsWith("HoTTViewer") && this.application.getActiveDevice().getName().equals("HoTTViewer"))) { // HoTTViewer V3 -> HoTTViewerAdapter //$NON-NLS-1$ //$NON-NLS-2$
			log.log(Level.INFO, String.format("%s candidate found for wrong device '%-11s' in %s  %s", this.vault.getLogFileExtension(), this.vault.getLogDeviceName(), this.vault.getLogFilePath(), //$NON-NLS-1$
					this.vault.getStartTimeStampFormatted()));
			return false;
		}
		else
			return true;
	}

	public boolean isConsistentChannel(List<Integer> channelMixConfigNumbers) {
		if (!channelMixConfigNumbers.contains(this.vault.logChannelNumber)) {
			log.log(Level.FINE,
					String.format("%s candidate for invalid channel  %d '%-11s' in %s  %s", this.vault.getLogFileExtension(), this.vault.getLogChannelNumber(), this.vault.getRectifiedObjectKey(), //$NON-NLS-1$
							this.vault.getLogFilePath(), this.vault.getStartTimeStampFormatted()));
			return false;
		}
		else
			return true;
	}

	public boolean isConsistentStartTimeStamp() {
		long minStartTimeStamp_ms = LocalDate.now().minusMonths(this.settings.getRetrospectMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
		return isConsistentStartTimeStamp(minStartTimeStamp_ms);
	}

	public boolean isConsistentStartTimeStamp(long minStartTimeStamp_ms) {
		if (this.vault.getLogStartTimestamp_ms() < minStartTimeStamp_ms) {
			log.log(Level.FINE, String.format("%s candidate out of time range      '%-11s' in %s  %s", this.vault.getLogFileExtension(), this.vault.getRectifiedObjectKey(), //$NON-NLS-1$
					this.vault.getLogFilePath(), this.vault.getStartTimeStampFormatted()));
			return false;
		}
		else
			return true;
	}

	public boolean isConsistentObjectKey() {
		boolean isValidObject = false;
		boolean isValidatedObjectKey = this.settings.getValidatedObjectKey(this.application.getObjectKey()).isPresent();
		if (this.application.getActiveObject() != null && !isValidatedObjectKey) {
			log.log(Level.INFO, String.format("%s candidate found for empty object '%-11s' in %s  %s", this.vault.getLogFileExtension(), this.vault.getLogObjectKey(), //$NON-NLS-1$
					this.vault.getLogFilePath(), this.vault.getStartTimeStampFormatted()));
			isValidObject = this.settings.getFilesWithoutObject();
		}
		else if (this.application.getActiveObject() != null && !isValidatedObjectKey) {
			log.log(Level.INFO, String.format("%s candidate found for wrong object '%-11s' in %s  %s", this.vault.getLogFileExtension(), this.vault.getRectifiedObjectKey(), //$NON-NLS-1$
					this.vault.getLogFilePath(), this.vault.getStartTimeStampFormatted()));
			isValidObject = this.settings.getFilesWithOtherObject();
		}
		else if (this.application.getActiveObject() == null || isValidatedObjectKey) {
			log.log(Level.FINE, String.format("%s candidate found for object       '%-11s' in %s  %s", this.vault.getLogFileExtension(), this.vault.getRectifiedObjectKey(), //$NON-NLS-1$
					this.vault.getLogFilePath(), this.vault.getStartTimeStampFormatted()));
			isValidObject = true;
		}
		return isValidObject;
	}

	public ExtendedVault getVault() {
		return this.vault;
	}

}
