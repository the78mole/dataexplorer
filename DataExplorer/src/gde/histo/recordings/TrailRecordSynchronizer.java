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

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import gde.GDE;
import gde.data.Record;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.histo.ui.CurveSurvey;
import gde.log.Level;

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
		HashMap<Integer, Vector<Record>> scaleSyncedRecords = this.trailRecordSet.getScaleSyncedRecords();
		scaleSyncedRecords.clear();

		for (int i = 0; i < this.trailRecordSet.size(); i++) {
			final TrailRecord tmpRecord = (TrailRecord) this.trailRecordSet.get(i);
			final PropertyType syncProperty = tmpRecord.getProperty(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value());
			if (syncProperty != null && !syncProperty.getValue().isEmpty()) {
				final int syncMasterRecordOrdinal = Integer.parseInt(syncProperty.getValue());
				if (syncMasterRecordOrdinal >= 0) {
					TrailRecord syncMasterRecord = (TrailRecord) this.trailRecordSet.get(syncMasterRecordOrdinal);
					if (scaleSyncedRecords.get(syncMasterRecordOrdinal) == null) {
						scaleSyncedRecords.put(syncMasterRecordOrdinal, new Vector<Record>());
						scaleSyncedRecords.get(syncMasterRecordOrdinal).add(syncMasterRecord);
						syncMasterRecord.setSyncMinValue(Integer.MAX_VALUE);
						syncMasterRecord.setSyncMaxValue(Integer.MIN_VALUE);
						if (log.isLoggable(Level.FINER))
							log.log(Level.FINER, "add syncMaster " + syncMasterRecord.getName() + " syncMinValue=" + syncMasterRecord.getSyncMinValue() + " syncMaxValue=" + syncMasterRecord.getSyncMaxValue());
					}
					if (!this.trailRecordSet.isRecordContained(syncMasterRecordOrdinal, tmpRecord)) {
						if (Math.abs(i - syncMasterRecordOrdinal) >= scaleSyncedRecords.get(syncMasterRecordOrdinal).size())
							scaleSyncedRecords.get(syncMasterRecordOrdinal).add(tmpRecord);
						else
							// sort while add
							scaleSyncedRecords.get(syncMasterRecordOrdinal).add(Math.abs(i - syncMasterRecordOrdinal), tmpRecord);

						this.trailRecordSet.syncMasterSlaveRecords(syncMasterRecord, Record.TYPE_AXIS_END_VALUES);
						this.trailRecordSet.syncMasterSlaveRecords(syncMasterRecord, Record.TYPE_AXIS_NUMBER_FORMAT);
						this.trailRecordSet.syncMasterSlaveRecords(syncMasterRecord, Record.TYPE_AXIS_SCALE_POSITION);
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "add " + tmpRecord.getName()); //$NON-NLS-1$
					}
				}
			}
		}
		if (log.isLoggable(Level.FINE)) {
			StringBuilder sb = new StringBuilder();
			for (Integer syncRecordOrdinal : scaleSyncedRecords.keySet()) {
				sb.append(GDE.STRING_NEW_LINE).append(syncRecordOrdinal).append(GDE.STRING_COLON);
				for (Record tmpRecord : scaleSyncedRecords.get(syncRecordOrdinal)) {
					sb.append(tmpRecord.getName()).append(GDE.STRING_SEMICOLON);
				}
			}
			log.log(Level.FINE, sb.toString());
		}
	}

	/**
	 * Set the sync max/min values for visible records inclusive referenced suite records.
	 * Update records to enable drawing of the curve.
	 */
	public void updateAllSyncScales() {
		for (Record record : this.trailRecordSet.getRecordsSortedForDisplay()) {
			TrailRecord actualRecord = (TrailRecord) record;
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, actualRecord.getName() + "   isVisible=" + actualRecord.isVisible() + " isDisplayable=" + actualRecord.isDisplayable() //$NON-NLS-1$ //$NON-NLS-2$
					+ " isScaleSynced=" + actualRecord.isScaleSynced()); //$NON-NLS-1$

			if (actualRecord.isVisible() && actualRecord.isDisplayable()) {
				if (!actualRecord.getTrailSelector().isTrailSuite()) {
					actualRecord.setSyncMinValue((int) (actualRecord.getMinValue() * actualRecord.getSyncMasterFactor()));
					actualRecord.setSyncMaxValue((int) (actualRecord.getMaxValue() * actualRecord.getSyncMasterFactor()));
				}
				else {
					actualRecord.setSyncMinValue((int) (actualRecord.getSuiteMinValue() * actualRecord.getSyncMasterFactor()));
					actualRecord.setSyncMaxValue((int) (actualRecord.getSuiteMaxValue() * actualRecord.getSyncMasterFactor()));
				}
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, actualRecord.getName() + "   syncMin = " + actualRecord.getSyncMinValue() + "; syncMax = " + actualRecord.getSyncMaxValue()); //$NON-NLS-1$ //$NON-NLS-2$
			}
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
		for (Map.Entry<Integer, Vector<Record>> syncRecordsEntry : this.trailRecordSet.getScaleSyncedRecords().entrySet()) {
			boolean isAffected = false;

			int syncRecordOrdinal = syncRecordsEntry.getKey();
			int tmpMin = Integer.MAX_VALUE;
			int tmpMax = Integer.MIN_VALUE;
			for (Record syncRecord : syncRecordsEntry.getValue()) {
				if (syncRecord.isVisible() && syncRecord.isDisplayable()) {
					tmpMin = Math.min(tmpMin, syncRecord.getSyncMinValue());
					tmpMax = Math.max(tmpMax, syncRecord.getSyncMaxValue());
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, syncRecord.getName() + " tmpMin  = " + tmpMin / 1000.0 + "; tmpMax  = " + tmpMax / 1000.0); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			// now we have the max/min values over all sync records of the current sync group
			for (Record syncRecord : this.trailRecordSet.getScaleSyncedRecords().get(syncRecordOrdinal)) {
				((TrailRecord) syncRecord).setSyncMinValue(tmpMin);
				((TrailRecord) syncRecord).setSyncMaxValue(tmpMax);
			}

			if (isAffected && log.isLoggable(Level.FINER))
				log.log(Level.FINER, this.trailRecordSet.get(syncRecordOrdinal).getSyncMasterName() + "; syncMin = " + tmpMin / 1000.0 + "; syncMax = " + tmpMax / 1000.0); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

}
