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

    Copyright (c) 2017,2018 Thomas Eickert
****************************************************************************************/

package gde.histo.cache;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import gde.Analyzer;
import gde.GDE;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.ScoreLabelTypes;
import gde.device.SettlementType;
import gde.device.StatisticsType;
import gde.device.TrailTypes;
import gde.device.TransitionAmountType;
import gde.device.TransitionCalculusType;
import gde.device.TransitionFigureType;
import gde.histo.datasources.AbstractSourceDataSet.SourceDataSet;
import gde.histo.datasources.HistoSet;
import gde.histo.device.IHistoDevice;
import gde.histo.settlements.AmountEvaluator;
import gde.histo.settlements.CalculusEvaluator;
import gde.histo.settlements.FigureEvaluator;
import gde.histo.settlements.SettlementRecord;
import gde.histo.transitions.GroupTransitions;
import gde.histo.transitions.Transition;
import gde.histo.transitions.TransitionCollector;
import gde.histo.transitions.TransitionTableMapper.SettlementRecords;
import gde.histo.utils.UniversalQuantile;
import gde.log.Level;
import gde.log.Logger;

/**
 * Supports the data collection for a serializable histo vault.
 * @author Thomas Eickert
 */
public final class VaultCollector {
	final private static String	$CLASS_NAME	= VaultCollector.class.getName();
	final private static Logger	log					= Logger.getLogger($CLASS_NAME);

	private final IDevice				device			= Analyzer.getInstance().getActiveDevice();

	private ExtendedVault				vault;
	private SourceDataSet				sourceDataSet;

	/**
	 * @param objectDirectory validated object key
	 * @param sourcePath is the bin origin file (not a link file)
	 * @param fileVersion is the version of the log origin file
	 * @param logRecordSetSize is the number of recordsets in the log origin file
	 * @param logRecordsetBaseName the base name without recordset number
	 * @param providesReaderSettings true is a file reader capable to deliver different measurement values based on device settings
	 */
	public VaultCollector(String objectDirectory, Path sourcePath, int fileVersion, int logRecordSetSize, String logRecordsetBaseName, //
			boolean providesReaderSettings) {
		this(objectDirectory, sourcePath, fileVersion, logRecordSetSize, 0, logRecordsetBaseName, "native", sourcePath.toFile().lastModified(),
				Analyzer.getInstance().getActiveChannel().getNumber(), objectDirectory, providesReaderSettings);
	}

	/**
	 * @param objectDirectory validated object key
	 * @param sourcePath is the log origin file (not a link file)
	 * @param fileVersion is the version of the log origin file
	 * @param logRecordSetSize is the number of recordsets in the log origin file
	 * @param logRecordSetOrdinal identifies multiple recordsets in one single file (0-based)
	 * @param logRecordsetBaseName the base name without recordset number
	 * @param logStartTimestamp_ms of the log or recordset
	 * @param logDeviceName
	 * @param logChannelNumber may differ from UI settings in case of channel mix
	 * @param logObjectKey may differ from UI settings (empty in OSD files, validated parent path for bin files)
	 * @param providesReaderSettings true is a file reader capable to deliver different measurement values based on device settings
	 */
	public VaultCollector(String objectDirectory, Path sourcePath, int fileVersion, int logRecordSetSize, int logRecordSetOrdinal,
			String logRecordsetBaseName, String logDeviceName, long logStartTimestamp_ms, int logChannelNumber, String logObjectKey, //
			boolean providesReaderSettings) {
		String readerSettings = providesReaderSettings && Analyzer.getInstance().getActiveDevice() instanceof IHistoDevice
				? ((IHistoDevice) Analyzer.getInstance().getActiveDevice()).getReaderSettingsCsv() : GDE.STRING_EMPTY;
		File file = sourcePath.toFile();
		this.vault = new ExtendedVault(objectDirectory, sourcePath, file.lastModified(), file.length(), fileVersion, logRecordSetSize,
				logRecordSetOrdinal, logRecordsetBaseName, logDeviceName, logStartTimestamp_ms, logChannelNumber, logObjectKey, //
				readerSettings);
	}

	@Override
	public String toString() {
		return String.format("logChannelNumber=%d  logRecordSetOrdinal=%d  logObjectKey=%s  startTimestamp=%s  %s", vault.getLogChannelNumber(), vault.getLogRecordSetOrdinal(), vault.getLogObjectKey(), vault.getStartTimeStampFormatted(), vault.getLoadFilePath());
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
				GroupTransitions transitions = TransitionCollector.defineTransitions(recordSet, this.vault.logChannelNumber);
				SettlementRecords histoSettlements = determineSettlements(recordSet, transitions);
				setSettlements(histoSettlements);
			}

			scorePoints[ScoreLabelTypes.DURATION_MM.ordinal()] = (int) (recordSet.getMaxTime_ms() / 60000. * 1000. + .5);
			scorePoints[ScoreLabelTypes.AVERAGE_TIME_STEP_MS.ordinal()] = (int) (recordSet.getAverageTimeStep_ms() * 1000.);
			scorePoints[ScoreLabelTypes.MAXIMUM_TIME_STEP_MS.ordinal()] = (int) (recordSet.getMaximumTimeStep_ms() * 1000.);
			scorePoints[ScoreLabelTypes.MINIMUM_TIME_STEP_MS.ordinal()] = (int) (recordSet.getMinimumTimeStep_ms() * 1000.);
			scorePoints[ScoreLabelTypes.SIGMA_TIME_STEP_MS.ordinal()] = (int) (recordSet.getSigmaTimeStep_ms() * 1000.);
			scorePoints[ScoreLabelTypes.SAMPLED_READINGS.ordinal()] = recordSet.getRecordDataSize(true);

			setScorePoints(scorePoints);
		}
	}

	/**
	 * Add the measurements points from the records to the vault.
	 * @param recordSet
	 * @param isSampled
	 */
	private void setMeasurementPoints(RecordSet recordSet, boolean isSampled) {
		List<MeasurementType> channelMeasurements = device.getChannelMeasuremts(this.vault.getLogChannelNumber());
		for (int i = 0; i < recordSet.getRecordNames().length; i++) {
			MeasurementType measurementType = channelMeasurements.get(i);
			Record record = recordSet.get(recordSet.getRecordNames()[i]);

			CompartmentType entryPoints = new CompartmentType(i, measurementType.getName(), DataTypes.fromDataType(record.getDataType()));
			this.vault.getMeasurements().put(i, entryPoints);
			entryPoints.setTrails(new HashMap<Integer, PointType>());

			if (!record.hasReasonableData()) {
				log.fine(() -> String.format("no reasonable data for measurementType.getName()=%s %s", measurementType.getName(), this.vault.getLoadFilePath())); //$NON-NLS-1$
			} else {
				if (measurementType.getStatistics() != null) { // this creates trail types mismatch : && record.hasReasonableData()) {
					setTrailStatisticsPoints(entryPoints, record, recordSet);
				}
				setTrailPoints(entryPoints, record, isSampled);
			}
			log.finer(() -> record.getName() + " data " + entryPoints); //$NON-NLS-1$
		}
	}

	/**
	 * @param recordSet
	 * @param transitions
	 * @return the calculated settlements calculated from transitions (key is the name of the settlementType)
	 */
	private SettlementRecords determineSettlements(RecordSet recordSet, GroupTransitions transitions) {
		SettlementRecords histoSettlements = new SettlementRecords();

		for (SettlementType settlementType : device.getDeviceConfiguration().getChannel(this.vault.logChannelNumber).getSettlements().values()) {
			if (settlementType.getEvaluation() != null) {
				SettlementRecord record = new SettlementRecord(settlementType, recordSet, this.vault.logChannelNumber);
				histoSettlements.put(settlementType.getName(), record);
				if (transitions.isEmpty()) continue;

				final TransitionFigureType transitionFigureType = settlementType.getEvaluation().getTransitionFigure();
				final TransitionAmountType transitionAmountType = settlementType.getEvaluation().getTransitionAmount();
				final TransitionCalculusType calculationType = settlementType.getEvaluation().getTransitionCalculus();
				if (transitionFigureType != null) {
					FigureEvaluator evaluator = new FigureEvaluator(record);
					for (Transition transition : transitions.get(transitionFigureType.getTransitionGroupId()).values()) {
						evaluator.addFromTransition(transition);
					}
				} else if (transitionAmountType != null) {
					AmountEvaluator evaluator = new AmountEvaluator(record);
					for (Transition transition : transitions.get(transitionAmountType.getTransitionGroupId()).values()) {
						evaluator.addFromTransition(transition);
					}
				} else if (calculationType != null) {
					CalculusEvaluator evaluator = new CalculusEvaluator(record);
					for (Transition transition : transitions.get(calculationType.getTransitionGroupId()).values()) {
						evaluator.addFromTransition(transition);
					}
				} else {
					throw new UnsupportedOperationException();
				}
			}
		}
		return histoSettlements;
	}

	/**
	 * Set the list of point values for trail types marked as 'real'.
	 * @param entryPoints is the target object
	 * @param record
	 * @param recordSet
	 */
	private void setTrailStatisticsPoints(CompartmentType entryPoints, Record record, RecordSet recordSet) {
		StatisticsType measurementStatistics = this.device.getChannelMeasuremts(this.vault.getLogChannelNumber()).get(record.getOrdinal()).getStatistics();

		entryPoints.addPoint(TrailTypes.REAL_FIRST, transmuteValue(record, record.elementAt(0)));
		entryPoints.addPoint(TrailTypes.REAL_LAST, transmuteValue(record, record.elementAt(record.realSize() - 1)));

		int triggerRefOrdinal = -1;
		if (measurementStatistics.getTriggerRefOrdinal() != null) {
			int tmpOrdinal = measurementStatistics.getTriggerRefOrdinal().intValue();
			// if (record.isDisplayable()) { the record may be displayable later --- but note that calculating trigger ranges requires displayable true
			triggerRefOrdinal = tmpOrdinal;
			// }
		}
		boolean isTriggerLevel = measurementStatistics.getTrigger() != null;

		if (measurementStatistics.isAvg()) {
			if (isTriggerLevel) {
				int dd = record.getAvgValueTriggered() != Integer.MIN_VALUE ? record.getAvgValueTriggered() : 0;
				entryPoints.addPoint(TrailTypes.REAL_AVG, transmuteValue(record, dd));
			} else if (triggerRefOrdinal < 0) {
				entryPoints.addPoint(TrailTypes.REAL_AVG, transmuteValue(record, record.getAvgValue()));
			} else if (record.getAvgValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE) {
				entryPoints.addPoint(TrailTypes.REAL_AVG, transmuteValue(record, record.getAvgValueTriggered(triggerRefOrdinal)));
			}
		} else {
			entryPoints.addPoint(TrailTypes.REAL_AVG, transmuteValue(record, record.getAvgValue()));
		}

		if (measurementStatistics.isMax()) {
			if (isTriggerLevel) {
				entryPoints.addPoint(TrailTypes.REAL_MAX, transmuteValue(record, record.getMaxValueTriggered()));
			} else if (triggerRefOrdinal < 0) {
				entryPoints.addPoint(TrailTypes.REAL_MAX, transmuteValue(record, record.getRealMaxValue()));
			} else if (record.getMaxValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE) {
				entryPoints.addPoint(TrailTypes.REAL_MAX, transmuteValue(record, record.getMaxValueTriggered(triggerRefOrdinal)));
			}
		} else {
			entryPoints.addPoint(TrailTypes.REAL_MAX, transmuteValue(record, record.getRealMaxValue()));
		}

		if (measurementStatistics.isMin()) {
			if (isTriggerLevel) {
				entryPoints.addPoint(TrailTypes.REAL_MIN, transmuteValue(record, record.getMinValueTriggered()));
			} else if (triggerRefOrdinal < 0) {
				entryPoints.addPoint(TrailTypes.REAL_MIN, transmuteValue(record, record.getRealMinValue()));
			} else if (record.getMinValueTriggered(triggerRefOrdinal) != Integer.MAX_VALUE) {
				entryPoints.addPoint(TrailTypes.REAL_MIN, transmuteValue(record, record.getMinValueTriggered(triggerRefOrdinal)));
			}
		} else {
			entryPoints.addPoint(TrailTypes.REAL_MIN, transmuteValue(record, record.getRealMinValue()));
		}

		if (measurementStatistics.isSigma()) {
			if (isTriggerLevel) {
				entryPoints.addPoint(TrailTypes.REAL_SD, transmuteDelta(record, record.getSigmaValueTriggered()));
			} else if (triggerRefOrdinal < 0) {
				entryPoints.addPoint(TrailTypes.REAL_SD, transmuteDelta(record, record.getSigmaValue()));
			} else if (record.getSigmaValueTriggered(triggerRefOrdinal) != Integer.MIN_VALUE) {
				entryPoints.addPoint(TrailTypes.REAL_SD, transmuteDelta(record, record.getSigmaValueTriggered(triggerRefOrdinal)));
			}
		} else {
			entryPoints.addPoint(TrailTypes.REAL_SD, transmuteDelta(record, record.getSigmaValue()));
		}

		Integer refOrdinal = measurementStatistics.getSumByTriggerRefOrdinal();
		if (refOrdinal != null) {
			int referencedSumTriggeredRange = record.getSumTriggeredRange(refOrdinal);
			double summarizedValue = device.translateDeltaValue(record, referencedSumTriggeredRange / 1000.);
			log.finer(() -> String.format("%s -> summarizedValue = %d", record.getName(), (int)summarizedValue));
			if (measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1 && summarizedValue > 0.) {
				// Warning: Sum values do not sum correctly in case of offset != 0. Reason is summing up the offset multiple times.
				if (isTriggerLevel) {
					entryPoints.addPoint(TrailTypes.REAL_SUM_TRIGGERED, transmuteDelta(record, record.getSumTriggeredRange()));
				} else {
					entryPoints.addPoint(TrailTypes.REAL_SUM_TRIGGERED, transmuteDelta(record, referencedSumTriggeredRange));
				}
			}

			if (summarizedValue > 0.) { //while summarized value is zero it does not make sense to calculate ratio
				Integer ratioRefOrdinal = measurementStatistics.getRatioRefOrdinal();
				if (ratioRefOrdinal != null && measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1) {
					Record referencedRecord = recordSet.get(ratioRefOrdinal.intValue());
					if (referencedRecord != null) {
						final double summarizedReferencedValue = device.translateDeltaValue(referencedRecord, referencedRecord.getSumTriggeredRange(refOrdinal) / 1000.0);
						log.finer(() -> String.format("summarizedReferencedValue = %d, ratioRefOrdinal = %d", (int) summarizedReferencedValue, ratioRefOrdinal));
						if (summarizedReferencedValue > 0.) {
							double ratio = summarizedReferencedValue / summarizedValue;
							// multiply by 1000 -> all ratios are internally stored multiplied by thousand
							entryPoints.addPoint(TrailTypes.REAL_MAX_RATIO_TRIGGERED, transmuteScalar(record, (int) (ratio * 1000.)));
						}
					}
				} 
			}
		}

		String sumTriggerTimeText = measurementStatistics.getSumTriggerTimeText();
		if (measurementStatistics.getTrigger() != null && sumTriggerTimeText != null && sumTriggerTimeText.length() > 1) {
			// omitting the multiplication by 1000 results in seconds
			entryPoints.addPoint(TrailTypes.REAL_TIME_SUM_TRIGGERED, transmuteScalar(record, record.getTimeSumTriggeredRange_ms()));
		}
		if (measurementStatistics.isCountByTrigger() != null) {
			int countValue = record.getTriggerRanges() != null ? record.getTriggerRanges().size() : 0;
			// all counters are internally stored multiplied by thousand
			entryPoints.addPoint(TrailTypes.REAL_COUNT_TRIGGERED, transmuteScalar(record, countValue * 1000));
		}

		// all counters are internally stored multiplied by thousand
		entryPoints.addPoint(TrailTypes.REAL_COUNT, transmuteScalar(record, record.realSize() * 1000));
	}

	/**
	 * Provides an encoded histo vault value which can be decoded by applying the factor, offset and reduction.
	 * @return a record value (point) transformed into the device independent histo vault value (point)
	 */
	private int transmuteValue(Record record, int value) {
		return encodeMeasurementValue(record, device.translateValue(record, value / 1000.));
	}

	/**
	 * Provides an encoded histo vault value which can be decoded by applying the factor, offset and reduction.
	 * @return a record delta value transformed into the device independent histo vault value
	 */
	private int transmuteDelta(Record record, int deltaValue) {
		return encodeMeasurementValue(record, device.translateDeltaValue(record, deltaValue / 1000.));
	}

	/**
	 * Provides an encoded histo vault value which can be decoded by applying the factor, offset and reduction.
	 * @return a scalar (e.g. ratio) transformed into the device independent histo vault value
	 */
	private int transmuteScalar(Record record, int scalar) {
		return encodeMeasurementValue(record, scalar / 1000.);
	}

	/**
	 * Set the list of point values for standard trail types.
	 * @param entryPoints is the target object
	 * @param record
	 * @param isSampled true indicates that the record values are a sample from the original values
	 */
	private void setTrailPoints(CompartmentType entryPoints, Record record, boolean isSampled) {
		UniversalQuantile<Double> quantile = new UniversalQuantile<>(record.getTranslatedValues(), true, true, false);
		if (!quantile.getOutliers().isEmpty()) {
			entryPoints.setOutlierPoints(//
					quantile.getOutliers().stream().distinct().mapToInt(v -> encodeMeasurementValue(record, v)));
			log.log(Level.FINE, record.getName() + "  outliers=" + Arrays.toString(quantile.getOutliers().toArray()));
		}
		if (!quantile.getConstantScraps().isEmpty()) {
			entryPoints.setScrappedPoints( //
					quantile.getConstantScraps().stream().distinct().mapToInt(v -> encodeMeasurementValue(record, v)));
			log.log(Level.FINE, record.getName() + "  scrappedValues=", Arrays.toString(quantile.getConstantScraps().toArray()));
		}

		entryPoints.addPoint(TrailTypes.AVG, encodeMeasurementValue(record, quantile.getAvgFigure()));
		entryPoints.addPoint(TrailTypes.MAX, encodeMeasurementValue(record, quantile.getPopulationMaxFigure()));
		entryPoints.addPoint(TrailTypes.MIN, encodeMeasurementValue(record, quantile.getPopulationMinFigure()));
		entryPoints.addPoint(TrailTypes.SD, encodeMeasurementValue(record, quantile.getSigmaFigure()));
		entryPoints.addPoint(TrailTypes.Q0, encodeMeasurementValue(record, quantile.getQuartile0()));
		entryPoints.addPoint(TrailTypes.Q1, encodeMeasurementValue(record, quantile.getQuartile1()));
		entryPoints.addPoint(TrailTypes.Q2, encodeMeasurementValue(record, quantile.getQuartile2()));
		entryPoints.addPoint(TrailTypes.Q3, encodeMeasurementValue(record, quantile.getQuartile3()));
		entryPoints.addPoint(TrailTypes.Q4, encodeMeasurementValue(record, quantile.getQuartile4()));
		entryPoints.addPoint(TrailTypes.Q_25_PERMILLE, encodeMeasurementValue(record, quantile.getQuantile(.025)));
		entryPoints.addPoint(TrailTypes.Q_975_PERMILLE, encodeMeasurementValue(record, quantile.getQuantile(.975)));
		entryPoints.addPoint(TrailTypes.Q_LOWER_WHISKER, encodeMeasurementValue(record, quantile.getQuantileLowerWhisker()));
		entryPoints.addPoint(TrailTypes.Q_UPPER_WHISKER, encodeMeasurementValue(record, quantile.getQuantileUpperWhisker()));

		entryPoints.addPoint(TrailTypes.FIRST, encodeMeasurementValue(record, quantile.getFirstFigure()));
		entryPoints.addPoint(TrailTypes.LAST, encodeMeasurementValue(record, quantile.getLastFigure()));
		// trigger trail types sum are not supported for measurements
		entryPoints.addPoint(TrailTypes.SUM, 0);
		entryPoints.addPoint(TrailTypes.COUNT, encodeMeasurementValue(record, quantile.getSize()));
	}

	/**
	 * Add the settlements points from the histo settlements to the vault.
	 * @param histoSettlements
	 */
	private void setSettlements(SettlementRecords histoSettlements) {
		for (Entry<String, SettlementRecord> entry : histoSettlements.entrySet()) {
			SettlementRecord record = entry.getValue();
			SettlementType settlementType = record.getSettlement();
			CompartmentType entryPoints = new CompartmentType(settlementType.getSettlementId(), settlementType.getName(), DataTypes.DEFAULT);
			this.vault.getSettlements().put(settlementType.getSettlementId(), entryPoints);
			entryPoints.setTrails(new HashMap<Integer, PointType>());

			if (record.realSize() != 0 && record.hasReasonableData()) {
				entryPoints.addPoint(TrailTypes.REAL_FIRST, record.elementAt(0));
				entryPoints.addPoint(TrailTypes.REAL_LAST, record.elementAt(record.realSize() - 1));

				UniversalQuantile<Double> quantile = new UniversalQuantile<>(record.getTranslatedValues(), true, true, false);
				if (!quantile.getOutliers().isEmpty()) {
					entryPoints.setOutlierPoints(//
							quantile.getOutliers().stream().distinct().mapToInt(v -> encodeSettlementValue(record, v)));
					log.log(Level.FINE, record.getName() + "  outliers=" + Arrays.toString(quantile.getOutliers().toArray()));
				}
				if (!quantile.getConstantScraps().isEmpty()) {
					entryPoints.setScrappedPoints( //
							quantile.getConstantScraps().stream().distinct().mapToInt(v -> encodeSettlementValue(record, v)));
					log.log(Level.FINE, record.getName() + "  scrappedValues=", Arrays.toString(quantile.getConstantScraps().toArray()));
				}

				entryPoints.addPoint(TrailTypes.REAL_AVG, encodeSettlementValue(record, quantile.getAvgFigure()));
				entryPoints.addPoint(TrailTypes.REAL_MAX, encodeSettlementValue(record, quantile.getPopulationMaxFigure()));
				entryPoints.addPoint(TrailTypes.REAL_MIN, encodeSettlementValue(record, quantile.getPopulationMinFigure()));
				entryPoints.addPoint(TrailTypes.REAL_SD, encodeSettlementValue(record, quantile.getSigmaFigure()));
				entryPoints.addPoint(TrailTypes.REAL_SUM, encodeSettlementValue(record, quantile.getSumFigure()));
				entryPoints.addPoint(TrailTypes.REAL_COUNT, encodeSettlementValue(record, quantile.getSize()));

				entryPoints.addPoint(TrailTypes.AVG, encodeSettlementValue(record, quantile.getAvgFigure()));
				entryPoints.addPoint(TrailTypes.MAX, encodeSettlementValue(record, quantile.getPopulationMaxFigure()));
				entryPoints.addPoint(TrailTypes.MIN, encodeSettlementValue(record, quantile.getPopulationMinFigure()));
				entryPoints.addPoint(TrailTypes.SD, encodeSettlementValue(record, quantile.getSigmaFigure()));
				entryPoints.addPoint(TrailTypes.Q0, encodeSettlementValue(record, quantile.getQuartile0()));
				entryPoints.addPoint(TrailTypes.Q1, encodeSettlementValue(record, quantile.getQuartile1()));
				entryPoints.addPoint(TrailTypes.Q2, encodeSettlementValue(record, quantile.getQuartile2()));
				entryPoints.addPoint(TrailTypes.Q3, encodeSettlementValue(record, quantile.getQuartile3()));
				entryPoints.addPoint(TrailTypes.Q4, encodeSettlementValue(record, quantile.getQuartile4()));
				entryPoints.addPoint(TrailTypes.Q_25_PERMILLE, encodeSettlementValue(record, quantile.getQuantile(.025)));
				entryPoints.addPoint(TrailTypes.Q_975_PERMILLE, encodeSettlementValue(record, quantile.getQuantile(.975)));
				entryPoints.addPoint(TrailTypes.Q_LOWER_WHISKER, encodeSettlementValue(record, quantile.getQuantileLowerWhisker()));
				entryPoints.addPoint(TrailTypes.Q_UPPER_WHISKER, encodeSettlementValue(record, quantile.getQuantileUpperWhisker()));

				entryPoints.addPoint(TrailTypes.FIRST, encodeSettlementValue(record, quantile.getFirstFigure()));
				entryPoints.addPoint(TrailTypes.LAST, encodeSettlementValue(record, quantile.getLastFigure()));
				entryPoints.addPoint(TrailTypes.SUM, encodeSettlementValue(record, quantile.getSumFigure()));
				entryPoints.addPoint(TrailTypes.COUNT, encodeSettlementValue(record, quantile.getSize()));
			}
			log.finer(() -> record.getName() + " data " + this.vault.getSettlements()); //$NON-NLS-1$
		}
	}

	/**
	 * Add the score points to the vault.
	 * @param scorePoints
	 */
	private void setScorePoints(Integer[] scorePoints) {
		for (ScoreLabelTypes scoreLabelTypes : EnumSet.allOf(ScoreLabelTypes.class)) {
			Integer scoreValue = scorePoints[scoreLabelTypes.ordinal()];
			if (scoreValue != null) {
				PointType pointType = new PointType(scoreLabelTypes.ordinal(), scoreLabelTypes.toString(), scoreValue);
				this.vault.getScores().put(scoreLabelTypes.ordinal(), pointType);
			}
		}
		log.fine(() -> "scores " + this.vault.getScores()); //$NON-NLS-1$
	}

	private int encodeMeasurementValue(Record record, double value) {
		// todo harmonize encodeVaultValue methods later
		return (int) (HistoSet.encodeVaultValue(record, value) * 1000.);
	}

	/**
	 * Function to translate values represented into normalized settlement values.
	 * Does not support settlements based on GPS-longitude or GPS-latitude.
	 * @return double of settlement dependent value
	 */
	private int encodeSettlementValue(SettlementRecord record, double value) {
		// todo harmonize encodeVaultValue methods later
		// todo support settlements based on GPS-longitude or GPS-latitude with a base class common for Record and SettlementRecord
		double newValue = (value - record.getOffset()) / record.getFactor() + record.getReduction();
		return (int) (newValue * 1000.);
	}

	public ExtendedVault getVault() {
		return this.vault;
	}

	public void setSourceDataSet(SourceDataSet sourceDataSet) {
		this.sourceDataSet = sourceDataSet;
	}

	public SourceDataSet getSourceDataSet() {
		return this.sourceDataSet;
	}

}
