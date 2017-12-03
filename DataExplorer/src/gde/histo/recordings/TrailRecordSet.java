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

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.OFF;
import static java.util.logging.Level.WARNING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import gde.GDE;
import gde.config.Settings;
import gde.data.AbstractRecord;
import gde.data.AbstractRecordSet;
import gde.data.Record;
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
import gde.histo.utils.GpsCoordinate;
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
	public final class RecordingsCollector {
		@SuppressWarnings("hiding")
		private final Logger log = Logger.getLogger(RecordingsCollector.class.getName());

		RecordingsCollector() {
			super();
		}

		/**
		 * Set time steps for the trail recordset and the data points for all trail records.
		 * Every record takes the selected trail type / score data from the history vault and populates its data.
		 */
		public void addVaults() {
			for (Map.Entry<Long, List<ExtendedVault>> entry : histoVaults.entrySet()) {
				for (ExtendedVault histoVault : entry.getValue()) {
					addVaultHeader(histoVault);
				}
			}

			for (String recordName : getRecordNames()) {
				addVaults(recordName);
			}
		}

		private void addVaultHeader(ExtendedVault histoVault) {
			int duration_mm = histoVault.getScorePoint(ScoreLabelTypes.DURATION_MM.ordinal());
			durations_mm.add(duration_mm);
			if (!timeStep_ms.addRaw(histoVault.getLogStartTimestamp_ms() * 10)) {
				log.warning(() -> String.format("Duplicate recordSet  startTimeStamp %,d  %s", histoVault.getLogStartTimestamp_ms(), histoVault.getLogFilePath())); //$NON-NLS-1$
			}

			dataTags.add(histoVault);
		}

		/**
		 * Set the data points for one single trail record.
		 * The record takes the selected trail type / score data from the trail record vault and populates its data.
		 * @param recordName
		 */
		private synchronized void addVaults(String recordName) {
			TrailRecord trailRecord = get(recordName);

			boolean isSmartStatistics = Settings.getInstance().isSmartStatistics();

			trailRecord.clear();
			if (!trailRecord.getTrailSelector().isTrailSuite()) {
				for (Map.Entry<Long, List<ExtendedVault>> entry : histoVaults.entrySet()) {
					for (ExtendedVault histoVault : entry.getValue()) {
						trailRecord.addElement(trailRecord.getVaultPoint(histoVault, trailRecord.getTrailSelector().getTrailType()));
						if (isSmartStatistics) {
							trailRecord.addSummaryPoints(histoVault);
						}
					}
				}
			} else {
				trailRecord.setSuite(histoVaults.size());
				for (Map.Entry<Long, List<ExtendedVault>> entry : histoVaults.entrySet()) {
					for (ExtendedVault histoVault : entry.getValue()) {
						addVaultToSuite(histoVault, trailRecord);
						if (isSmartStatistics) {
							trailRecord.addSummaryPoints(histoVault);
						}
					}
				}
			}
			log.finer(() -> " " + trailRecord.getTrailSelector());
		}

		/**
		 * Take those data points from the histo vault which are assigned to the selected trail type.
		 * Supports trail suites.
		 */
		private void addVaultToSuite(ExtendedVault histoVault, TrailRecord trailRecord) {
			TrailSelector trailSelector = trailRecord.getTrailSelector();
			List<TrailTypes> suiteMembers = trailSelector.getTrailType().getSuiteMembers();

			if (!trailSelector.getTrailType().isRangePlot()) {
				SuiteRecords suiteRecords = trailRecord.getSuiteRecords();
				for (int i = 0; i < trailSelector.getTrailType().getSuiteMembers().size(); i++) {
					suiteRecords.get(i).addElement(trailRecord.getVaultPoint(histoVault, suiteMembers.get(i)));
				}
			} else {
				int tmpSummationFactor = 0;
				int masterPoint = 0; // this is the base value for adding or subtracting standard deviations

				SuiteRecords suiteRecords = trailRecord.getSuiteRecords();
				for (int i = 0; i < suiteMembers.size(); i++) {
					Integer point = trailRecord.getVaultPoint(histoVault, suiteMembers.get(i));
					if (point == null) {
						suiteRecords.get(i).addElement(null);
					} else {
						tmpSummationFactor = getSummationFactor(suiteMembers.get(i), tmpSummationFactor);
						if (tmpSummationFactor == 0)
							masterPoint = point; // use in the next iteration if summation is necessary, e.g. avg+2*sd
						else
							point = masterPoint + tmpSummationFactor * point * 2;

						suiteRecords.get(i).addElement(point);
					}
					if (log.isLoggable(FINER)) log.log(FINER, String.format(" %s trail %3d  %s  %d minVal=%d maxVal=%d", trailRecord.getName(), //$NON-NLS-1$
							trailSelector.getTrailOrdinal(), histoVault.getLogFilePath(), point, suiteRecords.get(i).getMinRecordValue(), suiteRecords.get(i).getMaxRecordValue()));
				}
			}
			log.log(FINER, " ", trailSelector);
		}

		/**
		 * Support adding / subtracting trail values from a preceding suite master record.
		 * @param trailType
		 * @param previousFactor the summation factor from the last iteration
		 * @return the alternating -1/+1 factor for summation trail types; 0 otherwise
		 */
		private int getSummationFactor(TrailTypes trailType, int previousFactor) {
			if (trailType.isAlienValue()) {
				return previousFactor == 0 ? -1 : previousFactor * -1;
			} else {
				return 0;
			}
		}

		/**
		 * Add GPS location tagging information based on the latitude / longitude median.
		 * Support asynchronous geocode fetches from the internet.
		 */
		void setGpsLocationsTags() {
			String[] recordNames = getRecordNames();
			// locate the GPS coordinates records
			TrailRecord latitudeRecord = null, longitudeRecord = null;
			for (String recordName : recordNames) { // todo fill HistoVault's DataType and access via DataType without hard coded measurement names
				if (recordName.toLowerCase().contains("latitud")) //$NON-NLS-1$
					latitudeRecord = get(recordName);
				else if (recordName.toLowerCase().contains("longitud")) //$NON-NLS-1$
					longitudeRecord = get(recordName);
				if (latitudeRecord != null && longitudeRecord != null) break;
			}

			GpsCluster gpsCluster = new GpsCluster();
			if (latitudeRecord != null && longitudeRecord != null) {
				// provide GPS coordinates for clustering which is the prerequisite for adding the location to dataGpsLocations
				for (Map.Entry<Long, List<ExtendedVault>> entry : histoVaults.entrySet()) {
					for (ExtendedVault histoVault : entry.getValue()) {
						Integer latitudePoint = histoVault.getMeasurementPoint(latitudeRecord.getOrdinal(), TrailTypes.Q2.ordinal());
						Integer longitudePoint = histoVault.getMeasurementPoint(longitudeRecord.getOrdinal(), TrailTypes.Q2.ordinal());

						if (latitudePoint != null && longitudePoint != null) {
							// todo why division by 1000?
							gpsCluster.add(new GpsCoordinate(HistoSet.decodeVaultValue(latitudeRecord, latitudePoint / 1000.),
									HistoSet.decodeVaultValue(longitudeRecord, longitudePoint / 1000.)));
						} else {
							gpsCluster.add(null); // this keeps the sequence in parallel with the vaults sequence
						}
					}
				}
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
					application.updateHistoTableWindow(false);
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
			HistoGraphicsTemplate template = record.getParentTrail().getTemplate();
			TrailSelector trailSelector = record.getTrailSelector();
			if (template != null && template.isAvailable()) {
				String property = template.getProperty(record.getOrdinal() + TrailRecord.TRAIL_TEXT_ORDINAL);
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

	/**
	 * Holds the view configuration.
	 */
	private final HistoGraphicsTemplate								template;

	private final List<Integer>												durations_mm	= new ArrayList<Integer>(INITIAL_RECORD_CAPACITY);
	private final TrailDataTags												dataTags			= new TrailDataTags();

	/**
	 * Data source for this recordset.
	 */
	private final TreeMap<Long, List<ExtendedVault>>	histoVaults;

	public enum DataTag {
		LINK_PATH, FILE_PATH, CHANNEL_NUMBER, RECTIFIED_OBJECTKEY, RECORDSET_BASE_NAME, RECORDSET_ORDINAL, GPS_LOCATION
	};

	/**
	 * Hold trail records for measurements, settlements and scores.
	 * @param useDevice the instance of the device
	 * @param channelNumber the channel number to be used
	 * @param recordNames
	 * @param timeSteps
	 */
	private TrailRecordSet(IDevice useDevice, int channelNumber, String[] recordNames, TimeSteps timeSteps,
			TreeMap<Long, List<ExtendedVault>> histoVaults) {
		super(useDevice, channelNumber, "Trail", recordNames, timeSteps); //$NON-NLS-1$
		String deviceSignature = useDevice.getName() + GDE.STRING_UNDER_BAR + channelNumber;
		this.histoVaults = histoVaults;
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
	public static synchronized TrailRecordSet createRecordSet(TreeMap<Long, List<ExtendedVault>> histoVaults) {
		IDevice device = DataExplorer.application.getActiveDevice();
		int channelConfigNumber = DataExplorer.application.getActiveChannelNumber();
		String[] names = device.getDeviceConfiguration().getMeasurementSettlementScoregroupNames(channelConfigNumber);
		TimeSteps timeSteps = new TimeSteps(-1, INITIAL_RECORD_CAPACITY);

		TrailRecordSet newTrailRecordSet = new TrailRecordSet(device, channelConfigNumber, names, timeSteps, histoVaults);
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
		syncScaleOfSyncableRecords();
	}

	/**
	 * Build data contents after building the records list.
	 */
	public synchronized void initializeFromVaults() {
		// setting all data in this create procedure and the synchronized keyword makes this method thread safe
		RecordingsCollector collector = new RecordingsCollector();
		collector.defineTrailTypes();
		collector.addVaults();
		collector.setGpsLocationsTags();
		syncScaleOfSyncableRecords();
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
		this.scaleSyncedRecords.initSyncedScales();
	}

	/**
	 * Update referenced records to enable drawing of the curve, set min/max.
	 * Set the sync max/min values for visible records inclusive referenced suite records.
	 * Update the scale values from sync record if visible.
	 */
	public void updateSyncRecordScale() {
		for (TrailRecord actualRecord : getVisibleAndDisplayableRecords()) {
			log.finer(() -> "set scale base value " + actualRecord.getName() + " isScaleSynced=" + actualRecord.isScaleSynced()); //$NON-NLS-1$
			actualRecord.setSyncMaxMinValue();
		}

		for (Map.Entry<Integer, Vector<TrailRecord>> syncRecordsEntry : this.getScaleSyncedRecords().entrySet()) {
			boolean isAffected = false;
			int tmpMin = Integer.MAX_VALUE;
			int tmpMax = Integer.MIN_VALUE;
			for (TrailRecord syncRecord : syncRecordsEntry.getValue()) {
				if (syncRecord.isVisible() && syncRecord.isDisplayable()) {
					isAffected = true;
					tmpMin = Math.min(tmpMin, syncRecord.getSyncMinValue());
					tmpMax = Math.max(tmpMax, syncRecord.getSyncMaxValue());
					if (log.isLoggable(FINER)) log.log(FINER, syncRecord.getName() + " tmpMin  = " + tmpMin / 1000.0 + "; tmpMax  = " + tmpMax / 1000.0); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			// now we have the max/min values over all sync records of the current sync group
			for (TrailRecord syncRecord : syncRecordsEntry.getValue()) {
				syncRecord.setSyncMinMax(tmpMin, tmpMax);
			}

			if (isAffected && log.isLoggable(FINER)) {
				log.log(FINER, this.get((int) syncRecordsEntry.getKey()).getSyncMasterName() + "; syncMin = " + tmpMin / 1000.0 + "; syncMax = " + tmpMax / 1000.0); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * Update referenced records to enable drawing of the summary graphics.
	 * Set the sync vaults max/min values.
	 * Update the scale values from sync record.</br>
	 * Take all records but build sync information only for display records.
	 * Needs not to check suite records because the summary max/min values comprise all suite members.
	 */
	public void updateSyncSummary() {
		{ // reset hidden records
			List<TrailRecord> hiddenRecords = new ArrayList<>(this.getValues());
			hiddenRecords.removeAll(getDisplayRecords());
			for (TrailRecord actualRecord : hiddenRecords) {
				actualRecord.resetSyncSummaryMinMax();
			}
		}
		for (TrailRecord actualRecord : getDisplayRecords()) {
			actualRecord.setSyncSummaryMinMax();
			log.finer(() -> actualRecord.getName() + "   syncMin = " + actualRecord.syncSummaryMin + "; syncMax = " + actualRecord.syncSummaryMax); //$NON-NLS-1$ //$NON-NLS-2$
		}

		for (java.util.Map.Entry<Integer, Vector<TrailRecord>> syncRecordsEntry : this.getScaleSyncedRecords().entrySet()) {
			if (syncRecordsEntry.getValue().size() <= 1) continue; // keep the sync summary min/max values

			boolean isAffected = false;
			double tmpMin = Double.MAX_VALUE;
			double tmpMax = -Double.MAX_VALUE;
			for (TrailRecord syncRecord : syncRecordsEntry.getValue()) {
				if (syncRecord.syncSummaryMax != null && syncRecord.syncSummaryMin != null) {
					isAffected = true;
					tmpMin = Math.min(tmpMin, syncRecord.syncSummaryMin);
					tmpMax = Math.max(tmpMax, syncRecord.syncSummaryMax);
					if (log.isLoggable(FINER)) log.log(FINER, syncRecord.getName() + " tmpMin  = " + tmpMin + "; tmpMax  = " + tmpMax); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			// now we have the max/min values over all sync records of the current sync group
			if (tmpMin == Double.MAX_VALUE || tmpMax == -Double.MAX_VALUE) {
				for (TrailRecord syncRecord : syncRecordsEntry.getValue()) {
					syncRecord.resetSyncSummaryMinMax();
				}
			} else {
				for (TrailRecord syncRecord : syncRecordsEntry.getValue()) {
					syncRecord.setSyncSummaryMinMax(tmpMin, tmpMax);
				}
			}
			if (isAffected && log.isLoggable(OFF)) {
				log.log(OFF, this.get((int) syncRecordsEntry.getKey()).getSyncMasterName() + "; syncMin = " + tmpMin + "; syncMax = " + tmpMax); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * Update the collection of visible and displayable records in this record set.
	 * The sort order conforms to the record insertion order.
	 */
	@Override
	public void updateVisibleAndDisplayableRecordsForTable() {
		this.visibleAndDisplayableRecords.removeAllElements();
		this.displayRecords.removeAllElements();

		// get by insertion order
		for (Map.Entry<String, AbstractRecord> entry : this.entrySet()) {
			final TrailRecord record = (TrailRecord) entry.getValue();
			if (record.isAllowedBySetting()) {
				if (record.isActive() && record.hasReasonableData()) {
					record.setDisplayable(true);
					getDisplayRecords().add(record);
					if (record.isVisible()) // only selected records get displayed
						getVisibleAndDisplayableRecords().add(record);
				}
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
			((TrailRecord) super.get(recordName)).clear();
		}
		this.durations_mm.clear();

		this.dataTags.clear();
	}

	/**
	 * Method to get the sorted record names as array for display purpose.
	 * Sorted according display requirement, grid record first, syncMasterRecords second, all remaining.
	 * @return all measurement records and settlement / score records based on display settings
	 */
	public TrailRecord[] getRecordsSortedForDisplay() {
		Vector<TrailRecord> displayRecords = new Vector<>();
		// add the record with horizontal grid
		for (TrailRecord record : this.getDisplayRecords()) {
			if (record.getOrdinal() == this.getValueGridRecordOrdinal()) displayRecords.add(record);
		}
		// add the scaleSyncMaster records to draw scale of this records first which sets the min/max display values
		for (TrailRecord record : this.getDisplayRecords()) {
			if (record.getOrdinal() != this.getValueGridRecordOrdinal() && record.isScaleSyncMaster()) displayRecords.add(record);
		}
		// add all others
		for (TrailRecord record : this.getDisplayRecords()) {
			if (record.getOrdinal() != this.getValueGridRecordOrdinal() && !record.isScaleSyncMaster()) displayRecords.add(record);
		}

		return displayRecords.toArray(new TrailRecord[displayRecords.size()]);
	}

	/**
	 * Save the histo graphics definition into template file.
	 */
	public void saveTemplate() {
		for (int i = 0; i < this.size(); ++i) {
			TrailRecord record = this.get(i);
			this.template.setProperty(i + Record.IS_VISIBLE, String.valueOf(record.isVisible()));
			this.template.setProperty(i + Record.IS_POSITION_LEFT, String.valueOf(record.isPositionLeft()));
			Color color = record.getColor();
			String rgb = color.getRGB().red + GDE.STRING_COMMA + color.getRGB().green + GDE.STRING_COMMA + color.getRGB().blue;
			this.template.setProperty(i + Record.COLOR, rgb);
			this.template.setProperty(i + Record.LINE_WITH, String.valueOf(record.getLineWidth()));
			this.template.setProperty(i + Record.LINE_STYLE, String.valueOf(record.getLineStyle()));
			this.template.setProperty(i + Record.IS_ROUND_OUT, String.valueOf(record.isRoundOut()));
			this.template.setProperty(i + Record.IS_START_POINT_ZERO, String.valueOf(record.isStartpointZero()));
			this.template.setProperty(i + Record.NUMBER_FORMAT, String.valueOf(record.getNumberFormat()));
			this.template.setProperty(i + Record.IS_START_END_DEFINED, String.valueOf(record.isStartEndDefined()));
			this.template.setProperty(i + Record.DEFINED_MAX_VALUE, String.valueOf(record.getMaxScaleValue()));
			this.template.setProperty(i + Record.DEFINED_MIN_VALUE, String.valueOf(record.getMinScaleValue()));
			this.template.setProperty(i + TrailRecord.TRAIL_TEXT_ORDINAL, String.valueOf(record.getTrailSelector().getTrailTextSelectedIndex()));
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
				record.setVisible(Boolean.parseBoolean(this.template.getProperty(i + Record.IS_VISIBLE, "false"))); //$NON-NLS-1$
				record.setPositionLeft(Boolean.parseBoolean(this.template.getProperty(i + Record.IS_POSITION_LEFT, "true"))); //$NON-NLS-1$
				int r, g, b;
				String color = this.template.getProperty(i + Record.COLOR, record.getRGB());
				r = Integer.parseInt(color.split(GDE.STRING_COMMA)[0].trim());
				g = Integer.parseInt(color.split(GDE.STRING_COMMA)[1].trim());
				b = Integer.parseInt(color.split(GDE.STRING_COMMA)[2].trim());
				record.setColor(SWTResourceManager.getColor(r, g, b));
				record.setLineWidth(Integer.parseInt(this.template.getProperty(i + Record.LINE_WITH, "1"))); //$NON-NLS-1$
				record.setLineStyle(Integer.parseInt(this.template.getProperty(i + Record.LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_SOLID)));
				record.setRoundOut(Boolean.parseBoolean(this.template.getProperty(i + Record.IS_ROUND_OUT, "false"))); //$NON-NLS-1$
				record.setStartpointZero(Boolean.parseBoolean(this.template.getProperty(i + Record.IS_START_POINT_ZERO, "false"))); //$NON-NLS-1$
				record.setStartEndDefined(Boolean.parseBoolean(this.template.getProperty(i + Record.IS_START_END_DEFINED, "false")), //$NON-NLS-1$
						Double.parseDouble(this.template.getProperty(i + Record.DEFINED_MIN_VALUE, "0")) //$NON-NLS-1$
						, Double.parseDouble(this.template.getProperty(i + Record.DEFINED_MAX_VALUE, "0"))); //$NON-NLS-1$
				record.setNumberFormat(Integer.parseInt(this.template.getProperty(i + Record.NUMBER_FORMAT, "-1"))); //$NON-NLS-1$
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
			// ET 29.06.2017 this.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
			log.fine(() -> "applied histo graphics template file " + this.template.getCurrentFilePath()); //$NON-NLS-1$
			if (doUpdateVisibilityStatus) {
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

	public long getLeftmostTimeStamp_ms() {
		return this.timeStep_ms.firstElement() / 10;
	}

	public long getRightmostTimeStamp_ms() {
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

	/**
	 * @param recordKey the key which record should be measured
	 * @return true if the record isMeasurementMode or isDeltaMeasurementMode
	 */
	public boolean isSurveyMode(String recordKey) {
		TrailRecord record = this.get(recordKey);
		if (record != null) {
			return record.isMeasurementMode() || record.isDeltaMeasurementMode();
		} else
			return false;
	}

	public TreeMap<Long, List<ExtendedVault>> getHistoVaults() {
		return this.histoVaults;
	}

	@SuppressWarnings("unchecked")
	public Collection<TrailRecord> getValues() {
		return (Collection<TrailRecord>) (Collection<?>) values();
	}

}