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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import gde.GDE;
import gde.data.AbstractRecord;
import gde.data.Record;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.histo.ui.CurveSurvey;
import gde.log.Logger;

/**
 * Scale synchronization for measurements, settlements and scores.
 * @author Thomas Eickert (USER)
 */
public final class TrailRecordSynchronizer {
	private final static String		$CLASS_NAME	= CurveSurvey.class.getName();
	private final static Logger		log					= Logger.getLogger($CLASS_NAME);

	private final TrailRecordSet	trailRecordSet;

	public TrailRecordSynchronizer(TrailRecordSet trailRecordSet) {
		this.trailRecordSet = trailRecordSet;
	}

	/**
	 * Synchronize scales according device properties.
	 * Support settlements.
	 */
	public void syncScales() {
		Map<Integer, Vector<? extends AbstractRecord>> scaleSyncedRecords = this.trailRecordSet.getScaleSyncedRecords();
		scaleSyncedRecords.clear();

		for (int i = 0; i < this.trailRecordSet.size(); i++) {
			final TrailRecord tmpRecord = (TrailRecord) this.trailRecordSet.get(i);
			final PropertyType syncProperty = tmpRecord.getProperty(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value());
			if (syncProperty != null && !syncProperty.getValue().isEmpty()) {
				final int syncMasterRecordOrdinal = Integer.parseInt(syncProperty.getValue());
				if (syncMasterRecordOrdinal >= 0) {
					TrailRecord syncMasterRecord = (TrailRecord) this.trailRecordSet.get(syncMasterRecordOrdinal);
					if (scaleSyncedRecords.get(syncMasterRecordOrdinal) == null) {
						scaleSyncedRecords.put(syncMasterRecordOrdinal, new Vector<TrailRecord>());
						((Vector<TrailRecord>) scaleSyncedRecords.get(syncMasterRecordOrdinal)).add(syncMasterRecord);
						syncMasterRecord.setSyncMinValue(Integer.MAX_VALUE);
						syncMasterRecord.setSyncMaxValue(Integer.MIN_VALUE);
						log.finer(() -> "add syncMaster " + syncMasterRecord.getName() + " syncMinValue=" + syncMasterRecord.getSyncMinValue() + " syncMaxValue=" + syncMasterRecord.getSyncMaxValue());
					}
					if (!this.trailRecordSet.isRecordContained(syncMasterRecordOrdinal, tmpRecord.getName())) {
						if (Math.abs(i - syncMasterRecordOrdinal) >= scaleSyncedRecords.get(syncMasterRecordOrdinal).size())
							((Vector<TrailRecord>) scaleSyncedRecords.get(syncMasterRecordOrdinal)).add(tmpRecord);
						else
							// sort while add
							((Vector<TrailRecord>) scaleSyncedRecords.get(syncMasterRecordOrdinal)).add(Math.abs(i - syncMasterRecordOrdinal), tmpRecord);

						this.trailRecordSet.syncMasterSlaveRecords(syncMasterRecord, Record.TYPE_AXIS_END_VALUES);
						this.trailRecordSet.syncMasterSlaveRecords(syncMasterRecord, Record.TYPE_AXIS_NUMBER_FORMAT);
						this.trailRecordSet.syncMasterSlaveRecords(syncMasterRecord, Record.TYPE_AXIS_SCALE_POSITION);
						log.finer(() -> "add " + tmpRecord.getName()); //$NON-NLS-1$
					}
				}
			}
		}
		if (log.isLoggable(FINE)) {
			StringBuilder sb = new StringBuilder();
			for (Integer syncRecordOrdinal : scaleSyncedRecords.keySet()) {
				sb.append(GDE.STRING_NEW_LINE).append(syncRecordOrdinal).append(GDE.STRING_COLON);
				for (AbstractRecord tmpRecord : scaleSyncedRecords.get(syncRecordOrdinal)) {
					sb.append(tmpRecord.getName()).append(GDE.STRING_SEMICOLON);
				}
			}
			log.log(FINE, sb.toString());
		}
	}

	/**
	 * Set the sync max/min values for visible records inclusive referenced suite records.
	 * Update records to enable drawing of the curve.
	 */
	public void updateAllSyncScales() {
		for (TrailRecord actualRecord : this.trailRecordSet.getRecordsSortedForDisplay()) {
			log.finer(() -> actualRecord.getName() + "   isVisible=" + actualRecord.isVisible() + " isDisplayable=" + actualRecord.isDisplayable() //$NON-NLS-1$ //$NON-NLS-2$
					+ " isScaleSynced=" + actualRecord.isScaleSynced()); //$NON-NLS-1$
			actualRecord.setSyncMaxMinValue();
		}

		// the same procedure is required for those records which are synchronized
		updateSyncRecordScale();
		;
	}

	/**
	 * Update the scale values from sync record if visible.
	 * Update referenced records to enable drawing of the curve, set min/max.
	 */
	public void updateSyncRecordScale() {
		for (Entry<Integer, Vector<? extends AbstractRecord>> syncRecordsEntry : this.trailRecordSet.getScaleSyncedRecords().entrySet()) {
			boolean isAffected = false;

			int syncRecordOrdinal = syncRecordsEntry.getKey();
			int tmpMin = Integer.MAX_VALUE;
			int tmpMax = Integer.MIN_VALUE;
			for (AbstractRecord tmpRecord : syncRecordsEntry.getValue()) {
				TrailRecord syncRecord = (TrailRecord) tmpRecord;
				if (syncRecord.isVisible() && syncRecord.isDisplayable()) {
					tmpMin = Math.min(tmpMin, syncRecord.getSyncMinValue());
					tmpMax = Math.max(tmpMax, syncRecord.getSyncMaxValue());
					if (log.isLoggable(FINER)) log.log(FINER, syncRecord.getName() + " tmpMin  = " + tmpMin / 1000.0 + "; tmpMax  = " + tmpMax / 1000.0); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			// now we have the max/min values over all sync records of the current sync group
			for (AbstractRecord tmpRecord : this.trailRecordSet.getScaleSyncedRecords().get(syncRecordOrdinal)) {
				TrailRecord syncRecord = (TrailRecord) tmpRecord;
				syncRecord.setSyncMinValue(tmpMin);
				syncRecord.setSyncMaxValue(tmpMax);
			}

			if (isAffected && log.isLoggable(FINER)) {
				TrailRecord record = (TrailRecord) this.trailRecordSet.get(syncRecordOrdinal);
				log.log(FINER, record.getSyncMasterName() + "; syncMin = " + tmpMin / 1000.0 + "; syncMax = " + tmpMax / 1000.0); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

}
