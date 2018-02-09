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

package gde.histo.recordings;

import static gde.histo.utils.ElementaryQuantile.BoxplotItems.LOWER_WHISKER;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.LQT;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.QUARTILE1;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.QUARTILE3;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.UPPER_WHISKER;
import static gde.histo.utils.ElementaryQuantile.BoxplotItems.UQT;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.WARNING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import gde.GDE;
import gde.config.Settings;
import gde.data.AbstractRecord;
import gde.data.AbstractRecordSet;
import gde.data.Record;
import gde.data.Record.DataType;
import gde.data.TimeSteps;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.device.ScoreGroupType;
import gde.device.ScoreLabelTypes;
import gde.device.SettlementType;
import gde.device.TrailTypes;
import gde.histo.cache.ExtendedVault;
import gde.histo.config.HistoGraphicsTemplate;
import gde.histo.datasources.HistoSet;
import gde.histo.gpslocations.GpsCluster;
import gde.histo.recordings.TrailDataTags.DataTag;
import gde.histo.recordings.TrailRecord.ChartTemplate;
import gde.histo.ui.AbstractChartComposite.AbstractChartData;
import gde.histo.ui.GraphicsComposite;
import gde.histo.ui.HistoExplorer;
import gde.histo.ui.HistoSummaryWindow;
import gde.histo.ui.SummaryComposite.SummaryLayout;
import gde.histo.ui.data.SummarySpots.OutlierWarning;
import gde.histo.utils.ElementaryQuantile;
import gde.histo.utils.GpsCoordinate;
import gde.histo.utils.UniversalQuantile;
import gde.log.Level;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

/**
 * Hold histo trail records for the configured measurements of a device supplemented by settlements and scores.
 * The display sequence is the linked hashmap sequence whereas the ordinals refer to the sequence of measurements + settlements +
 * scoregroups.
 * @author Thomas Eickert
 */
public final class TrailRecordSet extends AbstractRecordSet {
	@SuppressWarnings("hiding")
	private static final String	$CLASS_NAME					= TrailRecordSet.class.getName();
	private static final long		serialVersionUID		= -1580283867987273535L;
	@SuppressWarnings("hiding")
	private static final Logger	log									= Logger.getLogger($CLASS_NAME);

	public static final String	BASE_NAME_SEPARATOR	= " | ";

	/**
	 * Collect input data for the trail recordset and subordinate objects.
	 * Support initial collection and collections after user input (e.g. trail type selection).
	 * @author Thomas Eickert (USER)
	 */
	final class RecordingsCollector {
		@SuppressWarnings("hiding")
		private final Logger log = Logger.getLogger(RecordingsCollector.class.getName());

		/**
		 * Set time steps for the trail recordset and the data points for all trail records.
		 * Every record takes the selected trail type / score data from the history vault and populates its data.
		 */
		void addVaults() {
			for (Map.Entry<Long, List<ExtendedVault>> entry : pickedVaults.initialVaults.entrySet()) {
				for (ExtendedVault histoVault : entry.getValue()) {
					int duration_mm = histoVault.getScorePoint(ScoreLabelTypes.DURATION_MM.ordinal());
					durations_mm.add(duration_mm);
					if (!timeStep_ms.addRaw(histoVault.getLogStartTimestamp_ms() * 10)) {
						log.warning(() -> String.format("Duplicate recordSet  startTimeStamp %,d  %s", histoVault.getLogStartTimestamp_ms(), histoVault.getLogFilePath())); //$NON-NLS-1$
					}
					pickedVaults.addToIndex(histoVault);

					dataTags.add(histoVault);
				}
			}

			for (String recordName : recordNames) {
				TrailRecord trailRecord = get(recordName);
				trailRecord.initializeFromVaults(pickedVaults.initialVaults);
			}
		}

		/**
		 * Add GPS location tagging information based on the latitude / longitude median.
		 * Support asynchronous geocode fetches from the internet.
		 */
		void setGpsLocationsTags() {
			// locate the GPS coordinates records
			TrailRecord latitudeRecord = null, longitudeRecord = null;
			for (TrailRecord trailRecord : TrailRecordSet.this.getValues()) {
				if (trailRecord.getDataType() == DataType.GPS_LATITUDE)
					latitudeRecord = trailRecord;
				else if (trailRecord.getDataType() == DataType.GPS_LONGITUDE) longitudeRecord = trailRecord;
				if (latitudeRecord != null && longitudeRecord != null) break;
			}

			if (latitudeRecord != null && longitudeRecord != null) {
				// provide GPS coordinates for clustering which is the prerequisite for adding the location to dataGpsLocations
				GpsCluster gpsCluster = pickedVaults.defineGpsAverages(latitudeRecord, longitudeRecord);
				// populate the GPS locations list for subsequently filling the histo table
				if (gpsCluster.parallelStream().filter(Objects::nonNull).count() > 0) {
					Thread gpsLocationsThread = new Thread((Runnable) () -> setGpsLocationTags(gpsCluster), "setGpsLocationTags"); //$NON-NLS-1$
					try {
						gpsLocationsThread.start();
					} catch (RuntimeException e) {
						log.log(WARNING, e.getMessage(), e);
					}
				}
			}
		}

		/**
		 * Populate the GPS locations list if there are any GPS locations in this recordset.
		 * Trigger refilling the histo table.
		 * @param gpsCluster holds the GPS coordinates and the assignment to clusters; null coordinates are allowed
		 * @param dataGpsLocations is an empty list as INPUT or GPS location strings for all vaults in the correct sequence as OUTPUT
		 */
		private synchronized void setGpsLocationTags(GpsCluster gpsCluster) {
			long nanoTime = System.nanoTime();
			gpsCluster.setClusters();
			if (gpsCluster.size() > 0) {
				getDataTags().add(gpsCluster);
				// refresh the histo table which might already have been painted without the GPS coordinates
				if (getDataTags().getDataGpsLocations().size() > 0) {
					application.getPresentHistoExplorer().updateHistoTableWindow(false);
					log.finer(() -> "fill in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + " ms!  GPS locations size=" + gpsCluster.getAssignedClusters().values().size()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}

		/**
		 * Inform displayable trail records about the trail types which are allowed, set trail selection list and current trailType / score.
		 */
		void defineTrailTypes() {
			String[] trailRecordNames = getRecordNames();
			for (String trailRecordName : trailRecordNames) {
				TrailRecord trailRecord = get(trailRecordName);
				trailRecord.setApplicableTrailTypes();
				applyTemplateTrailData(trailRecord);
			}
		}

		/**
		 * Apply the data source information (= comboBox setting) from the graphics template definition to a record set.
		 * Look for trail type and score information in the histo graphics template.
		 * Take the prioritized trail type from applicable trails if no template setting is available.
		 * @param record
		 */
		private void applyTemplateTrailData(TrailRecord record) {
			TrailSelector trailSelector = record.getTrailSelector();
			if (template != null && template.isAvailable()) {
				String property = template.getProperty(record.getOrdinal() + Record.TRAIL_TEXT_ORDINAL);
				if (property != null) {
					int propertyValue = Integer.parseInt(property);
					if (propertyValue >= 0 && propertyValue < trailSelector.getApplicableTrailsTexts().size()) {
						trailSelector.setTrailTextSelectedIndex(propertyValue);
					} else { // the property value points to a trail which does not exist
						trailSelector.setMostApplicableTrailTextOrdinal();
					}
				} else { // the property might miss because of a new measurement / settlement
					trailSelector.setMostApplicableTrailTextOrdinal();
				}
			} else {
				trailSelector.setMostApplicableTrailTextOrdinal();
			}
			if (trailSelector.getTrailTextSelectedIndex() < 0) {
				log.info(() -> String.format("%s : no trail types identified" + record.getName())); //$NON-NLS-1$
			} else {
				log.log(FINER, "", trailSelector); //$NON-NLS-1$
			}
		}

	}

	public final class PickedVaults {
		@SuppressWarnings("hiding")
		private final Logger															log						= Logger.getLogger(PickedVaults.class.getName());

		/**
		 * Data source for this recordset.
		 * Sorted by recordSet startTimeStamp in reverse order.
		 */
		private final TreeMap<Long, List<ExtendedVault>>	initialVaults;
		/**
		 * Reference list for accessing vaults by index.
		 * Same set of vaults as {@code histoVaults}.
		 * Sorted by recordSet startTimeStamp in reverse order.
		 */
		private final List<ExtendedVault>									indexedVaults	= new ArrayList<>();
		// todo use indexedVaults instead of initialVaults in internal methods (performance)

		public PickedVaults(TreeMap<Long, List<ExtendedVault>> initialVaults) {
			this.initialVaults = initialVaults;
		}

		void clear() {
			indexedVaults.clear();
		}

		boolean addToIndex(ExtendedVault vault) {
			return indexedVaults.add(vault);
		}

		/**
		 * @return the vault at the timestep index position
		 */
		public ExtendedVault getVault(int index) {
			return indexedVaults.get(index);
		}

		/**
		 * @return a GPS location coordinates list
		 */
		GpsCluster defineGpsAverages(TrailRecord latitudeRecord, TrailRecord longitudeRecord) {
			GpsCluster gpsCluster = new GpsCluster();
			// provide GPS coordinates for clustering which is the prerequisite for adding the location to dataGpsLocations
			for (Map.Entry<Long, List<ExtendedVault>> entry : initialVaults.entrySet()) {
				for (ExtendedVault histoVault : entry.getValue()) {
					Integer latitudePoint = histoVault.getMeasurementPoint(latitudeRecord.getOrdinal(), TrailTypes.Q2.ordinal());
					Integer longitudePoint = histoVault.getMeasurementPoint(longitudeRecord.getOrdinal(), TrailTypes.Q2.ordinal());

					if (latitudePoint != null && longitudePoint != null) {
						gpsCluster.add(new GpsCoordinate(HistoSet.decodeVaultValue(latitudeRecord, latitudePoint / 1000.),
								HistoSet.decodeVaultValue(longitudeRecord, longitudePoint / 1000.)));
					} else {
						gpsCluster.add(null); // this keeps the sequence in parallel with the vaults sequence
					}
				}
			}
			return gpsCluster;
		}

		/**
		 * Determine the outliers of the category which is the most significant.
		 * This means: The result holds far outliers OR close outliers in case no far outliers are present.
		 * @param logLimit is the maximum number of the most recent logs which is checked for warnings
		 * @return the array of outliers warning objects which may hold null values
		 */
		public Outliers[] defineMinMaxWarning(String recordName, int logLimit) {
			Outliers minWarning = null;
			Outliers maxWarning = null;

			double[][] minMaxQuantiles = defineExtremumQuantiles(get(recordName));
			double minWhiskerLimit = minMaxQuantiles[0][QUARTILE1.ordinal()];
			double maxWhiskerLimit = minMaxQuantiles[1][QUARTILE3.ordinal()];
			double closeMinOutlierLimit = minMaxQuantiles[0][LOWER_WHISKER.ordinal()];
			double closeMaxOutlierLimit = minMaxQuantiles[1][UPPER_WHISKER.ordinal()];
			double farMinOutlierLimit = minMaxQuantiles[0][QUARTILE1.ordinal()] - 3. * 2. * minMaxQuantiles[0][LQT.ordinal()];
			double farMaxOutlierLimit = minMaxQuantiles[1][QUARTILE3.ordinal()] + 3. * 2. * minMaxQuantiles[1][UQT.ordinal()];
			int[] extremumIndices = get(recordName).trailSelector.getExtremumTrailsIndices();
			String[] selectText = get(recordName).trailSelector.getExtremumTrailsTexts();
			int warningLevel = settings.getWarningLevel();
			if (warningLevel == -1) return new Outliers[] { null, null };

			int actualLimit = logLimit >= 0 && logLimit < indexedVaults.size() ? logLimit : indexedVaults.size();
			Iterator<ExtendedVault> iterator = indexedVaults.stream().iterator();
			for (int i = 0; i < actualLimit; i++) {
				ExtendedVault vault = iterator.next();
				Integer[] minMaxTrailPoints = getExtremumTrailPoints(vault, get(recordName));
				if (minMaxTrailPoints[0] == null || minMaxTrailPoints[1] == null) continue;

				double tmpMinValue = HistoSet.decodeVaultValue(get(recordName), minMaxTrailPoints[0] / 1000.0);
				if (HistoSet.fuzzyCompare(tmpMinValue, farMinOutlierLimit) < 0) {
					if (minWarning == null || minWarning.getWarningType() == OutlierWarning.CLOSE || minWarning.getWarningType() == OutlierWarning.WHISKER) {
						// discard lower category warnings
						minWarning = new Outliers(OutlierWarning.FAR, farMinOutlierLimit, closeMinOutlierLimit, extremumIndices[0], selectText[0]);
					}
					minWarning.add(tmpMinValue, i);
				} else if (OutlierWarning.CLOSE.isIncluded(warningLevel) && HistoSet.fuzzyCompare(tmpMinValue, closeMinOutlierLimit) < 0) {
					if (minWarning == null || minWarning.getWarningType() == OutlierWarning.WHISKER) {
						minWarning = new Outliers(OutlierWarning.CLOSE, farMinOutlierLimit, closeMinOutlierLimit, extremumIndices[0], selectText[0]);
						minWarning.add(tmpMinValue, i);
					} else if (minWarning.getWarningType() == OutlierWarning.CLOSE) {
						minWarning.add(tmpMinValue, i);
					} else {
						; // discard close warning if far warnings are already present
					}
				} else if (OutlierWarning.WHISKER.isIncluded(warningLevel) && HistoSet.fuzzyCompare(tmpMinValue, minWhiskerLimit) < 0) {
					if (minWarning == null) {
						minWarning = new Outliers(OutlierWarning.WHISKER, farMinOutlierLimit, closeMinOutlierLimit, extremumIndices[0], selectText[0]);
						minWarning.add(tmpMinValue, i);
					} else if (minWarning.getWarningType() == OutlierWarning.WHISKER) {
						minWarning.add(tmpMinValue, i);
					} else {
						; // discard warning if far or close warnings are already present
					}
				}
				double tmpMaxValue = HistoSet.decodeVaultValue(get(recordName), minMaxTrailPoints[1] / 1000.);
				if (HistoSet.fuzzyCompare(tmpMaxValue, farMaxOutlierLimit) > 0) {
					if (maxWarning == null || maxWarning.getWarningType() == OutlierWarning.CLOSE || maxWarning.getWarningType() == OutlierWarning.WHISKER) {
						maxWarning = new Outliers(OutlierWarning.FAR, farMaxOutlierLimit, closeMaxOutlierLimit, extremumIndices[1], selectText[1]);
					}
					maxWarning.add(tmpMaxValue, i);
				} else if (OutlierWarning.CLOSE.isIncluded(warningLevel) && HistoSet.fuzzyCompare(tmpMaxValue, closeMaxOutlierLimit) > 0) {
					if (maxWarning == null || maxWarning.getWarningType() == OutlierWarning.WHISKER) {
						maxWarning = new Outliers(OutlierWarning.CLOSE, farMaxOutlierLimit, closeMaxOutlierLimit, extremumIndices[1], selectText[1]);
						maxWarning.add(tmpMaxValue, i);
					} else if (maxWarning.getWarningType() == OutlierWarning.CLOSE) {
						maxWarning.add(tmpMaxValue, i);
					}
				} else if (OutlierWarning.WHISKER.isIncluded(warningLevel) && HistoSet.fuzzyCompare(tmpMaxValue, maxWhiskerLimit) > 0) {
					if (maxWarning == null) {
						maxWarning = new Outliers(OutlierWarning.WHISKER, farMaxOutlierLimit, closeMaxOutlierLimit, extremumIndices[1], selectText[1]);
						maxWarning.add(tmpMaxValue, i);
					} else if (maxWarning.getWarningType() == OutlierWarning.WHISKER) {
						maxWarning.add(tmpMaxValue, i);
					}
				}
			}
			return new Outliers[] { minWarning, maxWarning };
		}

		/**
		 * @return the points for the q0/q4 respective min/max trails; for score groups w/o min/max scores take the first score
		 */
		protected Integer[] getExtremumTrailPoints(ExtendedVault vault, TrailRecord record) {
			int[] extremumOrdinals = record.trailSelector.getExtremumTrailsOrdinals();
			return new Integer[] { record.getVaultPoint(vault, extremumOrdinals[0]), record.getVaultPoint(vault, extremumOrdinals[1]) };
		}

		/**
		 * @return the extended tukey tolerance arrays for the min/max trails or for score groups w/o min/max scores take the first score
		 */
		protected double[][] defineExtremumQuantiles(TrailRecord record) {
			int[] extremumOrdinals = record.trailSelector.getExtremumTrailsOrdinals();
			Stream<Integer> pointMinimums = initialVaults.values().parallelStream().flatMap(List::stream).map(v -> record.getVaultPoint(v, extremumOrdinals[0]));
			List<Double> decodedMinimums = pointMinimums.filter(Objects::nonNull).map(i -> HistoSet.decodeVaultValue(record, i / 1000.)).collect(Collectors.toList());
			ElementaryQuantile<Double> minQuantile = new ElementaryQuantile<>(decodedMinimums, true);

			Stream<Integer> pointMaximums = initialVaults.values().parallelStream().flatMap(List::stream).map(v -> record.getVaultPoint(v, extremumOrdinals[1]));
			List<Double> decodedMaximums = pointMaximums.filter(Objects::nonNull).map(i -> HistoSet.decodeVaultValue(record, i / 1000.)).collect(Collectors.toList());
			ElementaryQuantile<Double> maxQuantile = new ElementaryQuantile<>(decodedMaximums, true);

			return new double[][] { minQuantile.getTukeyWithQuartileTolerances(), maxQuantile.getTukeyWithQuartileTolerances() };
		}

		/**
		 * @return the min/maxValues from the most recent logs
		 */
		double[] defineRecentStandardMinMax(String recordName, int limit) {
			TrailRecord record = get(recordName);
			double[] decodedMinMaxValues = new double[] { Double.MAX_VALUE, -Double.MAX_VALUE };

			initialVaults.values().stream().flatMap(Collection::stream).limit(limit).forEach(v -> {
				Integer tmpPoint;
				if ((tmpPoint = record.getVaultPoint(v, TrailTypes.MIN.ordinal())) != null)
					decodedMinMaxValues[0] = Math.min(decodedMinMaxValues[0], HistoSet.decodeVaultValue(record, tmpPoint / 1000.));
				if ((tmpPoint = record.getVaultPoint(v, TrailTypes.MAX.ordinal())) != null)
					decodedMinMaxValues[1] = Math.max(decodedMinMaxValues[1], HistoSet.decodeVaultValue(record, tmpPoint / 1000.));
			});

			if (decodedMinMaxValues[0] == Double.MAX_VALUE || decodedMinMaxValues[1] == -Double.MAX_VALUE) {
				return new double[] { 0., 0. };
			} else {
				return decodedMinMaxValues;
			}

		}

		/**
		 * @return the lower/upper values based on q0/q4
		 */
		double[] defineStandardExtrema(String recordName) {
			TrailRecord record = get(recordName);
			List<Double> decodedMinValues = new ArrayList<>();
			List<Double> decodedLowValues = new ArrayList<>();
			List<Double> decodedHighValues = new ArrayList<>();
			List<Double> decodedMaxValues = new ArrayList<>();
			initialVaults.values().stream().flatMap(Collection::stream).forEach(v -> {
				Integer tmpPoint;
				if ((tmpPoint = record.getVaultPoint(v, TrailTypes.MIN.ordinal())) != null)
					decodedMinValues.add(HistoSet.decodeVaultValue(record, tmpPoint / 1000.));
				if ((tmpPoint = record.getVaultPoint(v, TrailTypes.Q0.ordinal())) != null)
					decodedLowValues.add(HistoSet.decodeVaultValue(record, tmpPoint / 1000.));
				if ((tmpPoint = record.getVaultPoint(v, TrailTypes.Q4.ordinal())) != null)
					decodedHighValues.add(HistoSet.decodeVaultValue(record, tmpPoint / 1000.));
				if ((tmpPoint = record.getVaultPoint(v, TrailTypes.MAX.ordinal())) != null)
					decodedMaxValues.add(HistoSet.decodeVaultValue(record, tmpPoint / 1000.));
			});

			if (decodedLowValues.isEmpty() || decodedHighValues.isEmpty()) {
				return new double[] { 0., 0. };
			} else {
				double[] result = getExtrema(recordName, decodedLowValues, decodedHighValues);

				// corrections in cases when the whiskers are not within the scale
				double lowerWhisker = new ElementaryQuantile<>(decodedMinValues, true).getQuantileLowerWhisker();
				if (lowerWhisker < result[0]) {
					result[0] = Math.min(lowerWhisker, result[0]);
					log.log(Level.FINER, "lower corrected to ", lowerWhisker);
				}
				double upperWhisker = new ElementaryQuantile<>(decodedMaxValues, true).getQuantileUpperWhisker();
				if (upperWhisker > result[1]) {
					result[1] = Math.max(upperWhisker, result[1]);
					log.log(Level.FINER, "upper corrected to ", upperWhisker);
				}
				return result;
			}
		}

		private double[] getExtrema(String recordName, List<Double> decodedLowValues, List<Double> decodedHighValues) {
			ElementaryQuantile<Double> minQuantile = new ElementaryQuantile<>(decodedLowValues, true);
			ElementaryQuantile<Double> maxQuantile = new ElementaryQuantile<>(decodedHighValues, true);
			int scaleSpread = settings.getSummaryScaleSpread();
			double scaleMin = minQuantile.getExtremumFromRange(UniversalQuantile.INTER_QUARTILE_SIGMA_FACTOR, -scaleSpread);
			double scaleMax = maxQuantile.getExtremumFromRange(UniversalQuantile.INTER_QUARTILE_SIGMA_FACTOR, scaleSpread);
			double[] result = new double[] { Math.min(minQuantile.getQuantileLowerWhisker(), scaleMin),
					Math.max(maxQuantile.getQuantileUpperWhisker(), scaleMax) };
			log.finer(() -> recordName + " Quantile.Size=" + decodedLowValues.size() + "/" + decodedHighValues.size() + Arrays.toString(result));
			return result;
		}

		/**
		 * @return the lower/upper values from the most recent logs for trails with a different number range than the measurement values
		 */
		double[] defineRecentAlienMinMax(String recordName, TrailTypes trailType, int limit) {
			Stream<Integer> alienPoints = initialVaults.values().parallelStream().flatMap(Collection::stream) //
					.limit(limit).map(v -> get(recordName).getVaultPoint(v, trailType.ordinal()));
			DoubleSummaryStatistics decodedAliens = alienPoints.filter(Objects::nonNull).map(i -> HistoSet.decodeVaultValue(get(recordName), i / 1000.))//
					.collect(Collectors.summarizingDouble((Double::doubleValue)));
			if (decodedAliens.getCount() == 0) {
				return new double[] { 0., 0. };
			} else {
				return new double[] { decodedAliens.getMin(), decodedAliens.getMax() };
			}
		}

		/**
		 * @return the lower/upper values for trails with a different number range than the measurement values (e.g. SD, counters)
		 */
		double[] defineAlienExtrema(String recordName, TrailTypes trailType) {
			Stream<Integer> alienPoints = initialVaults.values().parallelStream().flatMap(Collection::stream).map(v -> get(recordName).getVaultPoint(v, trailType.ordinal()));
			List<Double> decodedAliens = alienPoints.filter(Objects::nonNull).map(i -> HistoSet.decodeVaultValue(get(recordName), i / 1000.)).collect(Collectors.toList());
			if (decodedAliens.isEmpty()) {
				return new double[] { 0., 0. };
			} else {
				return getExtrema(recordName, decodedAliens, decodedAliens);
			}
		}

		/**
		 * @return the min/maxValues from the most recent logs and all scoregroup members
		 */
		public double[] defineRecentScoreMinMax(String recordName, ScoreGroupType scoregroup, int limit) {
			double[] minMaxValues = new double[] { Double.MAX_VALUE, -Double.MAX_VALUE };

			List<Integer> scoreOrdinals = scoregroup.getScore().stream().map(s -> s.getTrailOrdinal()) //
					.collect(Collectors.toList());
			initialVaults.values().stream().flatMap(Collection::stream).limit(limit).forEach(v -> {
				// determine the min and max of all score entries in the score group of this vault
				Stream<Integer> scoregroupPoints = scoreOrdinals.stream().map(t -> get(recordName).getVaultPoint(v, t));
				DoubleSummaryStatistics stats = scoregroupPoints.map(i -> HistoSet.decodeVaultValue(get(recordName), i / 1000.)) //
						.collect(Collectors.summarizingDouble(Double::doubleValue));
				minValue = Math.min(minValue, stats.getMin());
				maxValue = Math.max(maxValue, stats.getMax());
			});

			if (minMaxValues[0] == -Double.MAX_VALUE || minMaxValues[1] == Double.MIN_VALUE) {
				return new double[] { 0., 0. };
			} else {
				return minMaxValues;
			}
		}

		/**
		 * @return the lower/upper values from all scoregroup members
		 */
		public double[] defineScoreExtrema(String recordName, ScoreGroupType scoregroup) {
			List<Double> decodedLowValues = new ArrayList<>();
			List<Double> decodedHighValues = new ArrayList<>();

			List<Integer> scoreOrdinals = scoregroup.getScore().stream().map(s -> s.getTrailOrdinal()) //
					.collect(Collectors.toList());
			initialVaults.values().stream().flatMap(Collection::stream).forEach(v -> {
				// determine the min and max of all score entries in the score group of this vault
				Stream<Integer> scoregroupPoints = scoreOrdinals.stream().map(t -> get(recordName).getVaultPoint(v, t));
				DoubleSummaryStatistics stats = scoregroupPoints.map(i -> HistoSet.decodeVaultValue(get(recordName), i / 1000.)) //
						.collect(Collectors.summarizingDouble(Double::doubleValue));
				decodedLowValues.add(stats.getMin());
				decodedHighValues.add(stats.getMax());
			});
			log.finer(() -> getName() + "  decodedMinimums=" + Arrays.toString(decodedLowValues.toArray()) + "  decodedMaximums=" + Arrays.toString(decodedHighValues.toArray()));

			return getExtrema(recordName, decodedLowValues, decodedHighValues);
		}

		@Override
		public String toString() {
			return "[initialVaults=" + this.initialVaults.size() + ", indexedVaults=" + this.indexedVaults.size() + "]";
		}

	}

	/**
	 * Outliers data related to a warning category.
	 * Please note:</br>
	 * Vaults may hold an additional set of exceptional outliers beyond the far outlier category.
	 * Those outliers are not element of the recordset and of this outliers object.
	 */
	public static final class Outliers {
		private final OutlierWarning	warningType;
		private final double					farLimit;
		private final double					closeLimit;
		private final List<Double>		decodedValues	= new ArrayList<>();
		private final List<Integer>		indices				= new ArrayList<>();
		private final int							selectIndex;
		private final String					selectText;

		public Outliers(OutlierWarning warningType, double farLimit, double closeLimit, int selectIndex, String selectText) {
			this.warningType = warningType;
			this.farLimit = farLimit;
			this.closeLimit = closeLimit;
			this.selectIndex = selectIndex;
			this.selectText = selectText;
		}

		public boolean add(double decodedValue, int index) {
			this.indices.add(index);
			return decodedValues.add(decodedValue);
		}

		public OutlierWarning getWarningType() {
			return this.warningType;
		}

		public double getFarLimit() {
			return this.farLimit;
		}

		public double getCloseLimit() {
			return this.closeLimit;
		}

		public List<Double> getDecodedValues() {
			return this.decodedValues;
		}

		public List<Integer> getIndices() {
			return this.indices;
		}

		public int getSelectIndex() {
			return this.selectIndex;
		}

		public String getSelectText() {
			return this.selectText;
		}

		@Override
		public String toString() {
			String values = Arrays.toString(this.decodedValues.toArray(new Double[0]));
			return this.warningType.toString() + " farLimit/closeLimit/values=" + this.farLimit + "/" + this.closeLimit + "/" + values;
		}
	}

	private final HistoExplorer					presentHistoExplorer	= DataExplorer.getInstance().getPresentHistoExplorer();

	/**
	 * Holds the view configuration.
	 */
	private final HistoGraphicsTemplate	template;

	private final PickedVaults					pickedVaults;

	private final List<Integer>					durations_mm					= new ArrayList<Integer>(INITIAL_RECORD_CAPACITY);
	private final TrailDataTags					dataTags							= new TrailDataTags();

	/**
	 * Hold trail records for measurements, settlements and scores.
	 * @param useDevice the instance of the device
	 * @param channelNumber the channel number to be used
	 * @param recordNames
	 * @param timeSteps
	 */
	private TrailRecordSet(IDevice useDevice, int channelNumber, String[] recordNames, TimeSteps timeSteps,
			TreeMap<Long, List<ExtendedVault>> pickedVaults) {
		super(useDevice, channelNumber, "Trail", recordNames, timeSteps); //$NON-NLS-1$
		String deviceSignature = useDevice.getName() + GDE.STRING_UNDER_BAR + channelNumber;
		this.pickedVaults = new PickedVaults(pickedVaults);
		this.template = new HistoGraphicsTemplate(deviceSignature);
		if (this.template != null) this.template.load();

		this.visibleAndDisplayableRecords = new Vector<TrailRecord>();
		this.displayRecords = new Vector<TrailRecord>();
		log.fine(() -> " TrailRecordSet(IDevice, int, RecordSet"); //$NON-NLS-1$
	}

	/**
	 * Create a trail record set containing records according the channel configuration which is loaded from device properties file.
	 * The trail records' display sequence (= LinkedHashMap sequence) supports pinning score / settlement records at the top
	 * based on device xml settings.
	 * @return a trail record set containing all trail records (empty) as specified
	 */
	public static synchronized TrailRecordSet createRecordSet(TreeMap<Long, List<ExtendedVault>> pickedVaults) {
		IDevice device = DataExplorer.application.getActiveDevice();
		int channelConfigNumber = DataExplorer.application.getActiveChannelNumber();
		String[] names = device.getDeviceConfiguration().getMeasurementSettlementScoregroupNames(channelConfigNumber);
		TimeSteps timeSteps = new TimeSteps(-1, INITIAL_RECORD_CAPACITY);

		TrailRecordSet newTrailRecordSet = new TrailRecordSet(device, channelConfigNumber, names, timeSteps, pickedVaults);
		printRecordNames("createRecordSet() " + newTrailRecordSet.getName() + " - ", newTrailRecordSet.getRecordNames()); //$NON-NLS-1$ //$NON-NLS-2$
		List<MeasurementType> channelMeasurements = device.getDeviceConfiguration().getChannelMeasuremts(channelConfigNumber);
		LinkedHashMap<Integer, SettlementType> channelSettlements = device.getDeviceConfiguration().getChannel(channelConfigNumber).getSettlements();
		LinkedHashMap<Integer, ScoreGroupType> channelScoreGroups = device.getDeviceConfiguration().getChannel(channelConfigNumber).getScoreGroups();

		{// display section 0: look for scores at the top - scores' ordinals start after measurements + settlements due to GraphicsTemplate
			// compatibility
			int myIndex = channelMeasurements.size() + channelSettlements.size();
			for (ScoreGroupType scoreGroup : channelScoreGroups.values()) {
				PropertyType topPlacementProperty = scoreGroup.getProperty("histo_top_placement"); //$NON-NLS-1$
				if (topPlacementProperty != null ? Boolean.parseBoolean(topPlacementProperty.getValue()) : false) {
					TrailRecord tmpRecord = new ScoregroupTrail(myIndex, scoreGroup, newTrailRecordSet, scoreGroup.getProperty().size());
					newTrailRecordSet.put(scoreGroup.getName(), tmpRecord);
					tmpRecord.setColorDefaultsAndPosition(myIndex);
					if (newTrailRecordSet.size() == 1) tmpRecord.setColor(SWTResourceManager.getColor(0, 0, 0)); // top score group entry, set color to black
					if (log.isLoggable(FINE)) log.log(FINE, "added scoregroup record for " + scoreGroup.getName() + " - " + myIndex); //$NON-NLS-1$ //$NON-NLS-2$
				}
				myIndex++;
			}
		}
		{// display section 1: look for settlements at the top - settlements' ordinals start after measurements due to GraphicsTemplate compatibility
			int myIndex = channelMeasurements.size(); // myIndex is used as recordOrdinal
			for (SettlementType settlement : channelSettlements.values()) {
				PropertyType topPlacementProperty = settlement.getProperty("histo_top_placement"); //$NON-NLS-1$
				if (topPlacementProperty != null ? Boolean.parseBoolean(topPlacementProperty.getValue()) : false) {
					TrailRecord tmpRecord = new SettlementTrail(myIndex, settlement, newTrailRecordSet, INITIAL_RECORD_CAPACITY);
					newTrailRecordSet.put(settlement.getName(), tmpRecord);
					tmpRecord.setColorDefaultsAndPosition(myIndex);
					if (log.isLoggable(FINE)) log.log(FINE, "added settlement record for " + settlement.getName() + " - " + myIndex); //$NON-NLS-1$ //$NON-NLS-2$
				}
				myIndex++;
			}
		}
		{// display section 2: all measurements
			for (int i = 0; i < channelMeasurements.size(); i++) {
				MeasurementType measurement = device.getMeasurement(channelConfigNumber, i);
				TrailRecord tmpRecord = new MeasurementTrail(i, measurement, newTrailRecordSet, INITIAL_RECORD_CAPACITY); // ordinal starts at 0
				newTrailRecordSet.put(measurement.getName(), tmpRecord);
				tmpRecord.setColorDefaultsAndPosition(i);
				if (log.isLoggable(FINE)) log.log(FINE, "added measurement record for " + measurement.getName() + " - " + i); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		{// display section 3: take remaining settlements
			int myIndex = channelMeasurements.size(); // myIndex is used as recordOrdinal
			for (SettlementType settlement : channelSettlements.values()) {
				PropertyType topPlacementProperty = settlement.getProperty("histo_top_placement"); //$NON-NLS-1$
				if (!(topPlacementProperty != null ? Boolean.parseBoolean(topPlacementProperty.getValue()) : false)) {
					TrailRecord tmpRecord = new SettlementTrail(myIndex, settlement, newTrailRecordSet, INITIAL_RECORD_CAPACITY);
					newTrailRecordSet.put(settlement.getName(), tmpRecord);
					tmpRecord.setColorDefaultsAndPosition(myIndex);
					if (log.isLoggable(FINE)) log.log(FINE, "added settlement record for " + settlement.getName() + " - " + myIndex); //$NON-NLS-1$ //$NON-NLS-2$
				}
				myIndex++; //
			}
		}
		{// display section 4: take remaining scores
			int myIndex = channelMeasurements.size() + channelSettlements.size();
			for (ScoreGroupType scoreGroup : channelScoreGroups.values()) {
				PropertyType topPlacementProperty = scoreGroup.getProperty("histo_top_placement"); //$NON-NLS-1$
				if (!(topPlacementProperty != null ? Boolean.parseBoolean(topPlacementProperty.getValue()) : false)) {
					TrailRecord tmpRecord = new ScoregroupTrail(myIndex, scoreGroup, newTrailRecordSet, scoreGroup.getProperty().size());
					newTrailRecordSet.put(scoreGroup.getName(), tmpRecord);
					tmpRecord.setColorDefaultsAndPosition(myIndex);
					if (log.isLoggable(FINE)) log.log(FINE, "added scoregroup record for " + scoreGroup.getName() + " - " + myIndex); //$NON-NLS-1$ //$NON-NLS-2$
				}
				myIndex++; //
			}
		}

		return newTrailRecordSet;
	}

	/**
	 * Rebuild data contents except building the records list.
	 */
	public synchronized void refillRecordSet() {
		cleanup();
		RecordingsCollector collector = new RecordingsCollector();
		collector.addVaults();
		collector.setGpsLocationsTags();
	}

	/**
	 * Rebuild the record based on a new trail selection.
	 */
	public synchronized void refillRecord(TrailRecord record, int trailTextIndex) {
		record.getTrailSelector().setTrailTextSelectedIndex(trailTextIndex);
		record.initializeFromVaults(this.pickedVaults.initialVaults);
	}

	/**
	 * Build data contents after building the records list.
	 */
	public synchronized void initializeFromVaults() {
		RecordingsCollector collector = new RecordingsCollector();
		collector.defineTrailTypes();
		collector.addVaults();
		collector.setGpsLocationsTags();
	}

	/**
	 * @param recordOrdinal
	 * @return the record based on ordinal
	 */
	@Override
	public TrailRecord get(int recordOrdinal) {
		return (TrailRecord) super.get(recordOrdinal);
	}

	@Override
	public TrailRecord get(Object recordName) {
		return (TrailRecord) super.get(recordName);
	}

	@Override
	public TrailRecord put(String recordName, AbstractRecord record) {
		return (TrailRecord) super.put(recordName, record);
	}

	/**
	 * @return the horizontalGridRecord ordinal
	 */
	@Override
	public int getValueGridRecordOrdinal() {
		return this.valueGridRecordOrdinal;
	}

	/**
	 * Synchronize scales according device properties.
	 * Support settlements.
	 */
	@Override
	public void syncScaleOfSyncableRecords() {
		this.scaleSyncedRecords.initSyncedScales(this);
	}

	/**
	 * Update referenced records to enable drawing of the curve, set min/max.
	 * Set the sync max/min values for visible records inclusive referenced suite records.
	 * Update the scale values from sync record if visible.
	 * @param graphicsComposite
	 */
	public synchronized void updateSyncGraphicsScale(GraphicsComposite graphicsComposite) {
		for (TrailRecord actualRecord : getVisibleAndDisplayableRecords()) {
			log.finer(() -> "set scale base value " + actualRecord.getName() + " isScaleSynced=" + actualRecord.isScaleSynced()); //$NON-NLS-1$
			graphicsComposite.getChartData(actualRecord).setSyncMaxMinValue();
		}

		for (Map.Entry<Integer, Vector<TrailRecord>> syncRecordsEntry : this.getScaleSyncedRecords().entrySet()) {
			boolean isAffected = false;
			int tmpMin = Integer.MAX_VALUE;
			int tmpMax = Integer.MIN_VALUE;
			for (TrailRecord syncRecord : syncRecordsEntry.getValue()) {
				if (syncRecord.isVisible() && syncRecord.isDisplayable()) {
					isAffected = true;
					tmpMin = Math.min(tmpMin, graphicsComposite.getChartData(syncRecord).getSyncMinValue());
					tmpMax = Math.max(tmpMax, graphicsComposite.getChartData(syncRecord).getSyncMaxValue());
					if (log.isLoggable(FINER)) log.log(FINER, syncRecord.getName() + " tmpMin  = " + tmpMin / 1000.0 + "; tmpMax  = " + tmpMax / 1000.0); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			// now we have the max/min values over all sync records of the current sync group
			for (TrailRecord syncRecord : syncRecordsEntry.getValue()) {
				graphicsComposite.getChartData(syncRecord).setSyncMinMax(tmpMin, tmpMax);
			}

			if (isAffected && log.isLoggable(FINER)) {
				log.log(FINER, this.get((int) syncRecordsEntry.getKey()).getSyncMasterName() + "; syncMin = " + tmpMin / 1000.0 + "; syncMax = " + tmpMax / 1000.0); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * Update the display records to enable drawing of the summary graphics.
	 * Needs not to check suite records because the summary max/min values comprise all suite members.
	 * @param summaryData holds all data required for painting with key recordName
	 */
	public synchronized void updateSyncSummaryScale(AbstractChartData summaryData) {
		int recencyLimit = settings.getWarningCount();
		for (TrailRecord actualRecord : getDisplayRecords()) {
			SummaryLayout summary = (SummaryLayout) summaryData.get(actualRecord.getName());
			summary.clear();
			summary.setSyncMinMax(recencyLimit);
			log.finer(() -> actualRecord.getName() + "   summaryMin = " + summary.getSyncMin() + "  summaryMax=" + summary.getSyncMax());
		}

		// update the min/max values for synced records
		for (java.util.Map.Entry<Integer, Vector<TrailRecord>> syncRecordsEntry : this.getScaleSyncedRecords().entrySet()) {
			boolean isAffected = false;
			double tmpMin = Double.MAX_VALUE;
			double tmpMax = -Double.MAX_VALUE;
			for (TrailRecord syncRecord : syncRecordsEntry.getValue()) {
				SummaryLayout summary = (SummaryLayout) summaryData.get(syncRecord.getName());
				// exclude records with special trails from synchronizing
				if (syncRecord.getTrailSelector().getTrailType().isAlienValue()) continue;

				if (summary.isSyncMinMaxDefined()) {
					isAffected = true;
					tmpMin = Math.min(tmpMin, summary.getSyncMin());
					tmpMax = Math.max(tmpMax, summary.getSyncMax());
					if (log.isLoggable(FINER)) log.log(FINER, syncRecord.getName() + " tmpMin  = " + tmpMin + "; tmpMax  = " + tmpMax);
				}
			}

			// now we have the max/min values over all sync records of the current sync group
			if (tmpMin == Double.MAX_VALUE || tmpMax == -Double.MAX_VALUE) {
				for (TrailRecord syncRecord : syncRecordsEntry.getValue()) {
					if (syncRecord.getTrailSelector().getTrailType().isAlienValue()) continue;
					SummaryLayout summary = (SummaryLayout) summaryData.get(syncRecord.getName());
					summary.resetSyncMinMax();
				}
			} else {
				for (TrailRecord syncRecord : syncRecordsEntry.getValue()) {
					if (syncRecord.getTrailSelector().getTrailType().isAlienValue()) continue;
					SummaryLayout summary = (SummaryLayout) summaryData.get(syncRecord.getName());
					summary.setSyncMinMax(tmpMin, tmpMax);
				}
			}
			if (isAffected && log.isLoggable(FINER)) {
				log.log(FINER, this.get((int) syncRecordsEntry.getKey()).getSyncMasterName() + "; syncMin = " + tmpMin + "; syncMax = " + tmpMax);
			}
		}
	}

	/**
	 * Update the displayable record information in this record set.
	 */
	public void setDisplayable() {
		for (TrailRecord record : this.getValues()) {
			record.setDisplayable();
		}
	}

	/**
	 * Update the collections of displayable records in this record set.
	 * The sort order conforms to the record insertion order.
	 */
	@Override
	public synchronized void updateVisibleAndDisplayableRecordsForTable() {
		this.visibleAndDisplayableRecords.removeAllElements();
		this.displayRecords.removeAllElements();

		// get by insertion order
		for (Map.Entry<String, AbstractRecord> entry : this.entrySet()) {
			final TrailRecord record = (TrailRecord) entry.getValue();
			if (record.isDisplayable()) {
				getDisplayRecords().add(record);
				if (record.isVisible()) // only selected records get displayed
					getVisibleAndDisplayableRecords().add(record);
			}
		}
	}

	/**
	 * @return visible and display able records (p.e. to build the partial data table)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Vector<TrailRecord> getVisibleAndDisplayableRecordsForTable() {
		return (Vector<TrailRecord>) (this.settings.isPartialDataTable() ? this.visibleAndDisplayableRecords : this.displayRecords);
	}

	/**
	 * @return visible and displayable records (p.e. to build the partial data table)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Vector<TrailRecord> getVisibleAndDisplayableRecords() {
		return (Vector<TrailRecord>) this.visibleAndDisplayableRecords;
	}

	/**
	 * @return all records for display
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Vector<TrailRecord> getDisplayRecords() {
		return (Vector<TrailRecord>) this.displayRecords;
	}

	/**
	 * Clears the data points in all records and in the time steps.
	 * Keeps initial capacities.
	 * Does not clear any fields in the recordSet, the records or in timeStep.
	 */
	public void cleanup() {
		super.timeStep_ms.clear();
		for (String recordName : super.getRecordNames()) {
			get(recordName).clear();
		}
		this.durations_mm.clear();
		this.pickedVaults.indexedVaults.clear();

		this.dataTags.clear();
	}

	/**
	 * Method to get the sorted record names as array for display purpose.
	 * Sorted according display requirement, grid record first, syncMasterRecords second, all remaining.
	 * @return all measurement records and settlement / score records based on display settings
	 */
	public synchronized TrailRecord[] getRecordsSortedForDisplay() {
		Vector<TrailRecord> resultRecords = new Vector<>();
		// add the record with horizontal grid
		for (TrailRecord record : this.getDisplayRecords()) {
			if (record.getOrdinal() == this.getValueGridRecordOrdinal()) resultRecords.add(record);
		}
		// add the scaleSyncMaster records to draw scale of this records first which sets the min/max display values
		for (TrailRecord record : this.getDisplayRecords()) {
			if (record.getOrdinal() != this.getValueGridRecordOrdinal() && record.isScaleSyncMaster()) resultRecords.add(record);
		}
		// add all others
		for (TrailRecord record : this.getDisplayRecords()) {
			if (record.getOrdinal() != this.getValueGridRecordOrdinal() && !record.isScaleSyncMaster()) resultRecords.add(record);
		}

		return resultRecords.toArray(new TrailRecord[resultRecords.size()]);
	}

	/**
	 * Save the histo graphics definition into template file.
	 */
	public void saveTemplate() {
		for (int i = 0; i < this.size(); ++i) {
			TrailRecord record = this.get(i);
			ChartTemplate recordTemplate = record.getTemplate();
			this.template.setProperty(i + Record.IS_VISIBLE, String.valueOf(recordTemplate.isVisible));
			this.template.setProperty(i + Record.IS_POSITION_LEFT, String.valueOf(recordTemplate.isPositionLeft));
			Color color = recordTemplate.color;
			this.template.setProperty(i + Record.COLOR, color.getRGB().red + GDE.STRING_COMMA + color.getRGB().green + GDE.STRING_COMMA + color.getRGB().blue);
			this.template.setProperty(i + Record.LINE_WITH, String.valueOf(recordTemplate.lineWidth));
			this.template.setProperty(i + Record.LINE_STYLE, String.valueOf(recordTemplate.lineStyle));
			this.template.setProperty(i + Record.IS_ROUND_OUT, String.valueOf(recordTemplate.isRoundOut));
			this.template.setProperty(i + Record.IS_START_POINT_ZERO, String.valueOf(recordTemplate.isStartpointZero));
			this.template.setProperty(i + Record.NUMBER_FORMAT, String.valueOf(recordTemplate.numberFormat));
			this.template.setProperty(i + Record.IS_START_END_DEFINED, String.valueOf(recordTemplate.isStartEndDefined));
			this.template.setProperty(i + Record.DEFINED_MAX_VALUE, String.valueOf(recordTemplate.maxScaleValue));
			this.template.setProperty(i + Record.DEFINED_MIN_VALUE, String.valueOf(recordTemplate.minScaleValue));

			this.template.setProperty(i + Record.TRAIL_TEXT_ORDINAL, String.valueOf(record.getTrailSelector().getTrailTextSelectedIndex()));
		}
		int[] chartWeights = presentHistoExplorer.getHistoSummaryTabItem().getChartWeights();
		for (int i = 0; i < chartWeights.length; i++) {
			this.template.setProperty(AbstractRecordSet.CHART_WEIGHT + i, String.valueOf(chartWeights[i]));
		}
		this.template.store();
		log.fine(() -> "creating histo graphics template file in " + this.template.getCurrentFilePath()); //$NON-NLS-1$
	}

	/**
	 * Apply the graphics template definition to a record set.
	 * @param doUpdateVisibilityStatus example: if the histo data do not hold data for this record it makes no sense to display the curve.
	 */
	public void applyTemplate(boolean doUpdateVisibilityStatus) {
		if (this.template != null && this.template.isAvailable()) {
			boolean isHorizontalGridOrdinalSet = false;
			for (int i = 0; i < this.size(); ++i) {
				TrailRecord record = this.get(i);
				ChartTemplate recordTemplate = record.getTemplate();
				recordTemplate.isVisible = Boolean.parseBoolean(this.template.getProperty(i + Record.IS_VISIBLE, "false"));
				recordTemplate.isPositionLeft = Boolean.parseBoolean(this.template.getProperty(i + Record.IS_POSITION_LEFT, "true"));
				int r, g, b;
				String color = this.template.getProperty(i + Record.COLOR, record.getRGB());
				r = Integer.parseInt(color.split(GDE.STRING_COMMA)[0].trim());
				g = Integer.parseInt(color.split(GDE.STRING_COMMA)[1].trim());
				b = Integer.parseInt(color.split(GDE.STRING_COMMA)[2].trim());
				recordTemplate.color = SWTResourceManager.getColor(r, g, b);
				recordTemplate.lineWidth = Integer.parseInt(this.template.getProperty(i + Record.LINE_WITH, "1"));
				recordTemplate.lineStyle = Integer.parseInt(this.template.getProperty(i + Record.LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_SOLID));
				recordTemplate.isRoundOut = Boolean.parseBoolean(this.template.getProperty(i + Record.IS_ROUND_OUT, "false"));
				recordTemplate.isStartpointZero = Boolean.parseBoolean(this.template.getProperty(i + Record.IS_START_POINT_ZERO, "false"));
				record.setStartEndDefined(Boolean.parseBoolean(this.template.getProperty(i + Record.IS_START_END_DEFINED, "false")), //
						Double.parseDouble(this.template.getProperty(i + Record.DEFINED_MIN_VALUE, "0")), //
						Double.parseDouble(this.template.getProperty(i + Record.DEFINED_MAX_VALUE, "0")));
				recordTemplate.numberFormat = Integer.parseInt(this.template.getProperty(i + Record.NUMBER_FORMAT, "-1"));
				// time grid
				// color = this.template.getProperty(RecordSet.TIME_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
				// r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
				// g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
				// b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
				// recordSet.setTimeGridColor(SWTResourceManager.getColor(r, g, b));
				// recordSet.setTimeGridLineStyle(Integer.valueOf(this.template.getProperty(RecordSet.TIME_GRID_LINE_STYLE, GDE.STRING_EMPTY +
				// SWT.LINE_DOT)).intValue());
				// recordSet.setTimeGridType(Integer.valueOf(this.template.getProperty(RecordSet.TIME_GRID_TYPE, "0")).intValue()); //$NON-NLS-1$
				if (!isHorizontalGridOrdinalSet && record.isVisible()) { // set curve grid to the first visible record
					color = this.template.getProperty(AbstractRecordSet.VALUE_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
					r = Integer.parseInt(color.split(GDE.STRING_COMMA)[0].trim());
					g = Integer.parseInt(color.split(GDE.STRING_COMMA)[1].trim());
					b = Integer.parseInt(color.split(GDE.STRING_COMMA)[2].trim());
					this.setValueGridColor(SWTResourceManager.getColor(r, g, b));
					this.setValueGridLineStyle(Integer.parseInt(this.template.getProperty(AbstractRecordSet.VALUE_GRID_LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_DOT)));
					this.setValueGridType(Integer.parseInt(this.template.getProperty(AbstractRecordSet.VALUE_GRID_TYPE, "0"))); //$NON-NLS-1$
					this.setValueGridRecordOrdinal(record.getOrdinal()); // initial use top score trail record
					isHorizontalGridOrdinalSet = true;
				}
			}
			if (Settings.getInstance().isSmartStatistics()) { // only smart statistics supports multiple charts
				int[] chartWeights = HistoSummaryWindow.DEFAULT_CHART_WEIGHTS.clone();
				for (int i = 0; i < chartWeights.length; i++) {
					chartWeights[i] = Integer.parseInt(this.template.getProperty(AbstractRecordSet.CHART_WEIGHT + i, String.valueOf(HistoSummaryWindow.DEFAULT_CHART_WEIGHTS[i])));
				}
				presentHistoExplorer.getHistoSummaryTabItem().setChartWeights(chartWeights);
			}
			// ET 29.06.2017 this.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
			log.fine(() -> "applied histo graphics template file " + this.template.getCurrentFilePath()); //$NON-NLS-1$
			if (doUpdateVisibilityStatus) {
				setDisplayable();
				updateVisibleAndDisplayableRecordsForTable();
			}
		}
	}

	public HistoGraphicsTemplate getTemplate() {
		return this.template;
	}

	/**
	 * @return the number of timesteps (equals the size of the trailrecords /suiterecords)
	 */
	public int getTimeStepSize() {
		return this.timeStep_ms.size();
	}

	/**
	 * @param timestamp_ms
	 * @return the position of the timestep which is the closest to the timestamp
	 */
	public int getIndex(long timestamp_ms) {
		return this.timeStep_ms.getBestIndex(timestamp_ms, Comparator.reverseOrder());
	}

	/**
	 * @return individual durations for all trails
	 */
	public List<Integer> getDurations_mm() {
		return this.durations_mm;
	}

	public TrailDataTags getDataTags() {
		return this.dataTags;
	}

	public String getDataTagText(int index, DataTag dataTag) {
		return this.dataTags.getText(index, dataTag);
	}

	public long getTopTimeStamp_ms() {
		return this.timeStep_ms.firstElement() / 10;
	}

	public long getLowestTimeStamp_ms() {
		return this.timeStep_ms.lastElement() / 10;
	}

	public long getDisplayTimeStamp_ms(int index) {
		if (this.settings.isXAxisReversed()) {
			return this.timeStep_ms.get(index) / 10;
		} else {
			return this.timeStep_ms.get(this.timeStep_ms.size() - 1 - index) / 10;
		}
	}

	@Override
	public int getChannelConfigNumber() {
		return this.parent.getNumber();
	}

	@SuppressWarnings("unchecked")
	public SyncedRecords<TrailRecord> getScaleSyncedRecords() {
		return (SyncedRecords<TrailRecord>) this.scaleSyncedRecords;
	}

	/**
	 * @return the Vector containing the slave records sync by the master name
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Vector<TrailRecord> getScaleSyncedRecords(int syncMasterRecordOrdinal) {
		return (Vector<TrailRecord>) this.scaleSyncedRecords.get(syncMasterRecordOrdinal);
	}

	PickedVaults getPickedVaults() {
		return this.pickedVaults;
	}

	/**
	 * @return the vault at the timestep index position
	 */
	public ExtendedVault getVault(int index) {
		return this.pickedVaults.indexedVaults.get(index);
	}

	@SuppressWarnings("unchecked")
	public Collection<TrailRecord> getValues() {
		return (Collection<TrailRecord>) (Collection<?>) values();
	}

}