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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import gde.device.IDevice;
import gde.device.TrailTypes;
import gde.histo.cache.ExtendedVault;
import gde.histo.config.HistoGraphicsTemplate;
import gde.histo.datasources.HistoSet;
import gde.histo.gpslocations.GpsCluster;
import gde.histo.utils.GpsCoordinate;
import gde.log.Level;
import gde.ui.DataExplorer;

/**
 * Collect input data for the trail recordset and subordinate objects.
 * Support initial collection and collections after user input (e.g. trail type selection).
 * @author Thomas Eickert (USER)
 */
public final class RecordingsCollector {
	private final static String				$CLASS_NAME	= RecordingsCollector.class.getName();
	private final static Logger				log					= Logger.getLogger($CLASS_NAME);

	private final static DataExplorer	application	= DataExplorer.getInstance();
	private final static HistoSet			histoSet		= HistoSet.getInstance();

	/**
	 * Rebuild data contents except building the records list.
	 */
	public static synchronized void refillRecordSet(TrailRecordSet trailRecordSet) {
		trailRecordSet.cleanup();
		addVaults(trailRecordSet);
		setGpsLocationsTags(trailRecordSet);
		trailRecordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * Set time steps for the trail recordset and the data points for all trail records.
	 * Every record takes the selected trail type / score data from the history vault and populates its data.
	 */
	public static void addVaults(TrailRecordSet trailRecordSet) {
		for (String recordName : trailRecordSet.getRecordNames()) {
			TrailRecord trailRecord = (TrailRecord) trailRecordSet.get(recordName);
			if (trailRecord.getTrailSelector().getTrailType().isSuite()) {
				trailRecord.setSuite(histoSet.size());
			}
		}
		for (Map.Entry<Long, List<ExtendedVault>> entry : histoSet.entrySet()) {
			for (ExtendedVault histoVault : entry.getValue()) {
				trailRecordSet.addVaultHeader(histoVault);

				for (String recordName : trailRecordSet.getRecordNames()) {
					addVault(histoVault, (TrailRecord) trailRecordSet.get(recordName));
				}
			}
		}
	}

	/**
	 * Set the data points for one single trail record.
	 * The record takes the selected trail type / score data from the trail record vault and populates its data.
	 * @param recordOrdinal
	 */
	public static synchronized void addVaults(TrailRecordSet trailRecordSet, int recordOrdinal) {
		TrailRecord trailRecord = (TrailRecord) trailRecordSet.get(recordOrdinal);

		trailRecord.clear();
		trailRecord.setSuite(histoSet.size());

		for (Map.Entry<Long, List<ExtendedVault>> entry : histoSet.entrySet()) {
			for (ExtendedVault histoVault : entry.getValue()) {
				addVault(histoVault, trailRecord);
			}
		}
		trailRecordSet.syncScaleOfSyncableRecords();
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, " " + trailRecord.getTrailSelector());
	}

	/**
	 * Take those data points from the histo vault which are assigned to the selected trail type.
	 * Supports trail suites.
	 * @param histoVault
	 */
	private static void addVault(ExtendedVault histoVault, TrailRecord trailRecord) {
		TrailSelector trailSelector = trailRecord.getTrailSelector();
		if (!trailSelector.isTrailSuite()) {
			trailRecord.addElement(histoVault.getPoint(trailRecord, trailSelector.getTrailType()));
		} else {
			TrailTypes suiteTrailType = trailSelector.getTrailType();
			if (!suiteTrailType.isRangePlot()) {
				SuiteRecords suiteRecords = trailRecord.getSuiteRecords();
				for (int i = 0; i < suiteTrailType.getSuiteMembers().size(); i++) {
					suiteRecords.get(i).addElement(histoVault.getPoint(trailRecord, suiteTrailType.getSuiteMembers().get(i)));
				}
			} else {
				int tmpSummationFactor = 0;
				int masterPoint = 0; // this is the base value for adding or subtracting standard deviations

				SuiteRecords suiteRecords = trailRecord.getSuiteRecords();
				for (int i = 0; i < suiteTrailType.getSuiteMembers().size(); i++) {
					Integer point = histoVault.getPoint(trailRecord, suiteTrailType.getSuiteMembers().get(i));
					if (point == null) {
						suiteRecords.get(i).addElement(null);
					} else {
						tmpSummationFactor = getSummationFactor(suiteTrailType.getSuiteMembers().get(i), tmpSummationFactor);
						if (tmpSummationFactor == 0)
							masterPoint = point; // use in the next iteration if summation is necessary, e.g. avg+2*sd
						else
							point = masterPoint + tmpSummationFactor * point * 2;

						suiteRecords.get(i).addElement(point);
					}
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format(" %s trail %3d  %s  %d minVal=%d maxVal=%d", trailRecord.getName(), //$NON-NLS-1$
							trailSelector.getTrailOrdinal(), histoVault.getLogFilePath(), point, suiteRecords.get(i).getMinRecordValue(), suiteRecords.get(i).getMaxRecordValue()));
				}
			}
		}
		log.log(Level.FINER, " " , trailSelector);
	}

	/**
	 * Support adding / subtracting trail values from a preceding suite master record.
	 * @param trailType
	 * @param previousFactor the summation factor from the last iteration
	 * @return the alternating -1/+1 factor for summation trail types; 0 otherwise
	 */
	private static int getSummationFactor(TrailTypes trailType, int previousFactor) {
		if (trailType.isForSummation()) {
			return previousFactor == 0 ? -1 : previousFactor * -1;
		} else {
			return 0;
		}
	}

	/**
	 * Add GPS location tagging information based on the latitude / longitude median.
	 * Support asynchronous geocode fetches from the internet.
	 */
	public static void setGpsLocationsTags(TrailRecordSet trailRecordSet) {
		IDevice device = DataExplorer.application.getActiveDevice();
		String[] recordNames = trailRecordSet.getRecordNames();
		// locate the GPS coordinates records
		TrailRecord latitudeRecord = null, longitudeRecord = null;
		for (String recordName : recordNames) { // todo fill HistoVault's DataType and access via DataType without hard coded measurement names
			if (recordName.toLowerCase().contains("latitud")) //$NON-NLS-1$
				latitudeRecord = (TrailRecord) trailRecordSet.get(recordName);
			else if (recordName.toLowerCase().contains("longitud")) //$NON-NLS-1$
				longitudeRecord = (TrailRecord) trailRecordSet.get(recordName);
			if (latitudeRecord != null && longitudeRecord != null) break;
		}

		GpsCluster gpsCluster = new GpsCluster();
		if (latitudeRecord != null && longitudeRecord != null) {
			// provide GPS coordinates for clustering which is the prerequisite for adding the location to dataGpsLocations
			for (Map.Entry<Long, List<ExtendedVault>> entry : histoSet.entrySet()) {
				for (ExtendedVault histoVault : entry.getValue()) {
					Integer latitudePoint = histoVault.getMeasurementPoint(latitudeRecord.getOrdinal(), TrailTypes.Q2.ordinal());
					Integer longitudePoint = histoVault.getMeasurementPoint(longitudeRecord.getOrdinal(), TrailTypes.Q2.ordinal());

					if (latitudePoint != null && longitudePoint != null)
						gpsCluster.add(new GpsCoordinate(device.translateValue(latitudeRecord, latitudePoint / 1000.), device.translateValue(longitudeRecord, longitudePoint / 1000.)));
					else
						gpsCluster.add(null); // this keeps the sequence in parallel with the vaults sequence
				}
			}
			// populate the GPS locations list for subsequently filling the histo table
			if (gpsCluster.parallelStream().filter(Objects::nonNull).count() > 0) {
				Thread gpsLocationsThread = new Thread((Runnable) () -> setGpsLocationTags(gpsCluster, trailRecordSet), "setGpsLocationTags"); //$NON-NLS-1$
				try {
					gpsLocationsThread.start();
				} catch (RuntimeException e) {
					log.log(Level.WARNING, e.getMessage(), e);
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
	private static synchronized void setGpsLocationTags(GpsCluster gpsCluster, TrailRecordSet trailRecordSet) {
		long nanoTime = System.nanoTime();
		gpsCluster.setClusters();
		if (gpsCluster.size() > 0) {
			trailRecordSet.getDataTags().add(gpsCluster);
			// refresh the histo table which might already have been painted without the GPS coordinates
			if (trailRecordSet.getDataTags().getDataGpsLocations().size() > 0) {
				RecordingsCollector.application.updateHistoTable(false);
				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, "fill in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + " ms!  GPS locations size=" + gpsCluster.getAssignedClusters().values().size()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * Inform displayable trail records about the trail types which are allowed, set trail selection list and current trailType / score.
	 */
	public static void defineTrailTypes(TrailRecordSet trailRecordSet) {
		String[] trailRecordNames = trailRecordSet.getRecordNames();
		for (String trailRecordName : trailRecordNames) {
			TrailRecord trailRecord = ((TrailRecord) trailRecordSet.get(trailRecordName));
			trailRecord.getTrailSelector().setApplicableTrailTypes();
			applyTemplateTrailData(trailRecord);
		}
	}

	/**
	 * Apply the data source information (= comboBox setting) from the graphics template definition to a record set.
	 * Look for trail type and score information in the histo graphics template.
	 * Take the prioritized trail type from applicable trails if no template setting is available.
	 * @param record
	 */
	private static void applyTemplateTrailData(TrailRecord record) {
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
			log.log(Level.INFO, String.format("%s : no trail types identified", record.getName())); //$NON-NLS-1$
		} else {
			log.log(Level.FINER, "", trailSelector); //$NON-NLS-1$
		}
	}

}
