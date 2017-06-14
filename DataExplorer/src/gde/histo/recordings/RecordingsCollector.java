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

import gde.config.HistoGraphicsTemplate;
import gde.data.HistoSet;
import gde.device.IDevice;
import gde.device.TrailTypes;
import gde.histo.cache.HistoVault;
import gde.histo.gpslocations.GpsCluster;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.GpsCoordinate;

/**
 * Collect input data for the trail recordset and subordinate objects.
 * Support initial collection and collections after user input (e.g. trail type selection).
 * @author Thomas Eickert (USER)
 */
public final class RecordingsCollector {
	private final static String				$CLASS_NAME	= RecordingsCollector.class.getName();
	private final static Logger				log					= Logger.getLogger($CLASS_NAME);

	private final static DataExplorer	application	= DataExplorer.getInstance();							// pointer to main application
	private final static IDevice			device			= application.getActiveDevice();

	/**
	 * rebuild data contents except building the records list and the synchronized scales data
	 */
	public static synchronized void refillRecordSet(TrailRecordSet trailRecordSet) {
		trailRecordSet.cleanup();
		addHistoVaults(trailRecordSet);
		setGpsLocationsTags(trailRecordSet);
		trailRecordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * Set time steps for the trail recordset and the data points for all displayable trail records.
	 * Every record takes the selected trail type / score data from the history vault and populates its data.
	 */
	public static void addHistoVaults(TrailRecordSet trailRecordSet) {
		for (Map.Entry<Long, List<HistoVault>> entry : HistoSet.getInstance().entrySet()) {
			for (HistoVault histoVault : entry.getValue()) {
				trailRecordSet.addVaultHeader(histoVault);

				for (String recordName : trailRecordSet.getRecordNames()) {
					add(histoVault, (TrailRecord) trailRecordSet.get(recordName));
				}
			}
		}
	}

	/**
	 * Set the data points for one single trail record.
	 * The record takes the selected trail type / score data from the trail record vault and populates its data.
	 * @param recordOrdinal
	 */
	public static synchronized void addHistoVaults(TrailRecordSet trailRecordSet, int recordOrdinal) {
		TrailRecord trailRecord = (TrailRecord) trailRecordSet.get(recordOrdinal);
		// the vault does hot hold data for non displayable records (= inactive records)
		if (trailRecord.isDisplayable()) {
			trailRecord.clear();
			for (Map.Entry<Long, List<HistoVault>> entry : HistoSet.getInstance().entrySet()) {
				for (HistoVault histoVault : entry.getValue()) {
					add(histoVault, trailRecord);
				}
			}
			trailRecordSet.syncScaleOfSyncableRecords();
		}
		else {
			if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, "access vault for non displayable record " + trailRecord.getName()); //$NON-NLS-1$
		}
	}

	/**
	 * Take those data points from the histo vault which are assigned to the selected trail type.
	 * Supports trail suites.
	 * @param histoVault
	 */
	private static void add(HistoVault histoVault, TrailRecord trailRecord) {
		//		if (this.trailRecordSuite == null) {
		//			super.addElement(null);
		//		}
		//		else
		if (!trailRecord.getTrailSelector().isTrailSuite()) {
			Integer point;
			if (trailRecord.isMeasurement())
				point = histoVault.getMeasurementPoint(trailRecord.getOrdinal(), trailRecord.getTrailSelector().getTrailOrdinal());
			else if (trailRecord.isSettlement())
				point = histoVault.getSettlementPoint(trailRecord.getSettlement().getSettlementId(), trailRecord.getTrailSelector().getTrailOrdinal());
			else if (trailRecord.isScoreGroup()) {
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST,
						String.format(" %s trail %3d  %s %s", trailRecord.getName(), trailRecord.getTrailSelector().getTrailOrdinal(), histoVault.getVaultFileName(), histoVault.getLogFilePath())); //$NON-NLS-1$
				point = histoVault.getScorePoint(trailRecord.getTrailSelector().getTrailOrdinal());
			}
			else
				throw new UnsupportedOperationException("length == 1"); //$NON-NLS-1$

			trailRecord.addElement(point);
			if (log.isLoggable(Level.FINEST))
				log.log(Level.FINEST, String.format(" %s trail %3d  %s %s %d", trailRecord.getName(), trailRecord.getTrailSelector().getTrailOrdinal(), histoVault.getLogFilePath(), point)); //$NON-NLS-1$
		}
		else {
			int masterPoint = 0; // this is the basis value for adding or subtracting standard deviations
			boolean summationSign = false; // false means subtract, true means add
			for (SuiteRecord suiteRecord : trailRecord.suiteManager.values()) {
				Integer point;
				if (trailRecord.isMeasurement())
					point = histoVault.getMeasurementPoint(trailRecord.getOrdinal(), suiteRecord.getTrailOrdinal());
				else if (trailRecord.isSettlement())
					point = histoVault.getSettlementPoint(trailRecord.getSettlement().getSettlementId(), suiteRecord.getTrailOrdinal());
				else if (trailRecord.isScoreGroup()) {
					point = histoVault.getScorePoint(trailRecord.getTrailSelector().getTrailOrdinal());
				}
				else
					throw new UnsupportedOperationException("length > 1"); //$NON-NLS-1$

				if (point != null) {
					if (trailRecord.getTrailSelector().isSuiteForSummation()) {
						point = summationSign ? masterPoint + 2 * point : masterPoint - 2 * point;
						summationSign = !summationSign; // toggle the add / subtract mode
					}
					else {
						masterPoint = point; // use in the next iteration if summation is necessary, e.g. avg+2*sd
						summationSign = false;
					}
					suiteRecord.addElement(point);
				}
				else {
					suiteRecord.addElement(point);
				}
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, String.format(" %s trail %3d  %s %s %d minVal=%d maxVal=%d", trailRecord.getName(), //$NON-NLS-1$
						trailRecord.getTrailSelector().getTrailOrdinal(), histoVault.getLogFilePath(), point, suiteRecord.getMinRecordValue(), suiteRecord.getMaxRecordValue()));
			}
		}
		log.log(Level.FINEST, trailRecord.getName());
	}

	/**
	 * Add GPS location tagging information based on the latitude / longitude median.
	 * Support asynchronous geocode fetches from the internet.
	 */
	public static void setGpsLocationsTags(TrailRecordSet trailRecordSet) {
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
			for (Map.Entry<Long, List<HistoVault>> entry : HistoSet.getInstance().entrySet()) {
				for (HistoVault histoVault : entry.getValue()) {
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
				}
				catch (RuntimeException e) {
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
			applyTemplateTrailData(trailRecordSet, trailRecord);
		}
	}

	/**
	 * Apply the data source information (= comboBox setting) from the graphics template definition to a record set.
	 * Look for trail type and score information in the histo graphics template.
	 * Take the prioritized trail type from applicable trails if no template setting is available.
	 * @param record
	 */
	private static  void applyTemplateTrailData(TrailRecordSet trailRecordSet, TrailRecord record) {
		int recordOrdinal = record.getOrdinal();
		HistoGraphicsTemplate template = trailRecordSet.getTemplate();
		boolean isValidTemplate = template != null && template.isAvailable();
		// histo properties
		if (isValidTemplate && template.getProperty(recordOrdinal + TrailRecord.TRAIL_TEXT_ORDINAL) != null) {
			record.getTrailSelector().setTrailTextSelectedIndex(Integer.parseInt(template.getProperty(recordOrdinal + TrailRecord.TRAIL_TEXT_ORDINAL)));
		}
		else { // the template is not a histo template or the property is a new measurement / settlement
			record.getTrailSelector().setMostApplicableTrailTextOrdinal();
		}
		if (record.getTrailSelector().getTrailTextSelectedIndex() < 0) {
			log.log(Level.INFO, String.format("%s : no trail types identified", record.getName())); //$NON-NLS-1$
		}
		else {
			if (log.isLoggable(Level.FINE))
				log.log(Level.FINE, String.format("%s : selected trail type=%s ordinal=%d", record.getName(), record.getTrailSelector().getTrailText(), record.getTrailSelector().getTrailTextSelectedIndex())); //$NON-NLS-1$
		}
	}

}
