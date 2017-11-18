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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import gde.GDE;
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
import gde.histo.cache.ExtendedVault;
import gde.histo.config.HistoGraphicsTemplate;
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
	private static final String												$CLASS_NAME					= TrailRecordSet.class.getName();
	private static final long													serialVersionUID		= -1580283867987273535L;
	@SuppressWarnings("hiding")
	private static final Logger												log									= Logger.getLogger($CLASS_NAME);

	public static final String												BASE_NAME_SEPARATOR	= " | ";

	/**
	 * Holds the view configuration.
	 */
	private final HistoGraphicsTemplate								template;

	private final List<Integer>												durations_mm				= new ArrayList<Integer>(INITIAL_RECORD_CAPACITY);

	private final TrailDataTags												dataTags						= new TrailDataTags();
	private final TrailRecordSynchronizer							synchronizer				= new TrailRecordSynchronizer(this);

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
		this.allRecords = new Vector<TrailRecord>();
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
					TrailRecord tmpRecord = new SettlementRecord(myIndex, settlement, newTrailRecordSet, INITIAL_RECORD_CAPACITY);
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
				TrailRecord tmpRecord = new MeasurementRecord(i, measurement, newTrailRecordSet, INITIAL_RECORD_CAPACITY); // ordinal starts at 0
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
					TrailRecord tmpRecord = new SettlementRecord(myIndex, settlement, newTrailRecordSet, INITIAL_RECORD_CAPACITY);
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
		newTrailRecordSet.syncScaleOfSyncableRecords();

		// setting all data in this create procedure and the synchronized keyword makes this method thread safe
		RecordingsCollector.defineTrailTypes(newTrailRecordSet);
		RecordingsCollector.addVaults(newTrailRecordSet);
		RecordingsCollector.setGpsLocationsTags(newTrailRecordSet);
		newTrailRecordSet.syncScaleOfSyncableRecords();

		return newTrailRecordSet;
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
	public int getHorizontalGridRecordOrdinal() {
		return this.horizontalGridRecordOrdinal;
	}

	/**
	 * Synchronize scales according device properties.
	 * Support settlements.
	 */
	@Override
	public void syncScaleOfSyncableRecords() {
		this.synchronizer.syncScales();
	}

	/**
	 * Update the collection of visible and displayable records in this record set.
	 * The sort order conforms to the record insertion order.
	 */
	@Override
	public void updateVisibleAndDisplayableRecordsForTable() {
		this.visibleAndDisplayableRecords.removeAllElements();
		this.allRecords.removeAllElements();

		// get by insertion order
		for (Map.Entry<String, AbstractRecord> entry : this.entrySet()) {
			final TrailRecord record = (TrailRecord) entry.getValue();
			if (record.isAllowedBySetting()) {
				record.setDisplayable(record.isActive() && record.hasReasonableData());

				if (record.isVisible() && record.isDisplayable()) // only selected records get displayed
					getVisibleAndDisplayableRecords().add(record);
				getDisplayRecords().add(record);
			}
		}
	}

	/**
	 * @return visible and display able records (p.e. to build the partial data table)
	 */
	@Override
	public Vector<TrailRecord> getVisibleAndDisplayableRecordsForTable() {
		return (Vector<TrailRecord>) (this.settings.isPartialDataTable() ? this.visibleAndDisplayableRecords : this.allRecords);
	}

	/**
	 * @return visible and displayable records (p.e. to build the partial data table)
	 */
	@Override
	public Vector<TrailRecord> getVisibleAndDisplayableRecords() {
		return (Vector<TrailRecord>) this.visibleAndDisplayableRecords;
	}

	/**
	 * @return all records for display
	 */
	@Override
	public Vector<TrailRecord> getDisplayRecords() {
		return (Vector<TrailRecord>) this.allRecords;
	}

	/**
	 * Update the scale values from sync record if visible
	 * and update referenced records to enable drawing of the curve, set min/max.
	 */
	@Override
	public void updateSyncRecordScale() {
		throw new UnsupportedOperationException("is not required for histo");
		// this.synchronizer.updateSyncRecordScale();
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
			if (record.getOrdinal() == this.getHorizontalGridRecordOrdinal()) displayRecords.add(record);
		}
		// add the scaleSyncMaster records to draw scale of this records first which sets the min/max display values
		for (TrailRecord record : this.getDisplayRecords()) {
			if (record.getOrdinal() != this.getHorizontalGridRecordOrdinal() && record.isScaleSyncMaster()) displayRecords.add(record);
		}
		// add all others
		for (TrailRecord record : this.getDisplayRecords()) {
			if (record.getOrdinal() != this.getHorizontalGridRecordOrdinal() && !record.isScaleSyncMaster()) displayRecords.add(record);
		}

		return displayRecords.toArray(new TrailRecord[displayRecords.size()]);
	}

	/**
	 * Set the sync max/min values for visible records inclusive referenced suite records.
	 * Update records to enable drawing of the curve.
	 */
	public void updateAllSyncScales() {
		this.synchronizer.updateAllSyncScales();
	}

	public void addVaultHeader(ExtendedVault histoVault) {
		int duration_mm = histoVault.getScorePoint(ScoreLabelTypes.DURATION_MM.ordinal());
		this.durations_mm.add(duration_mm);
		if (!this.timeStep_ms.addRaw(histoVault.getLogStartTimestamp_ms() * 10)) {
			log.warning(() -> String.format("Duplicate recordSet  startTimeStamp %,d  %s", histoVault.getLogStartTimestamp_ms(), histoVault.getLogFilePath())); //$NON-NLS-1$
		}

		this.dataTags.add(histoVault);
	}

	/**
	 * Save the histo graphics definition into template file.
	 */
	public void saveTemplate() {
		for (int i = 0; i < this.size(); ++i) {
			TrailRecord record = (TrailRecord) this.get(i);
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
				TrailRecord record = (TrailRecord) this.get(i);
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
					color = this.template.getProperty(AbstractRecordSet.HORIZONTAL_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
					r = Integer.parseInt(color.split(GDE.STRING_COMMA)[0].trim());
					g = Integer.parseInt(color.split(GDE.STRING_COMMA)[1].trim());
					b = Integer.parseInt(color.split(GDE.STRING_COMMA)[2].trim());
					this.setHorizontalGridColor(SWTResourceManager.getColor(r, g, b));
					this.setHorizontalGridLineStyle(Integer.parseInt(this.template.getProperty(AbstractRecordSet.HORIZONTAL_GRID_LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_DOT)));
					this.setHorizontalGridType(Integer.parseInt(this.template.getProperty(AbstractRecordSet.HORIZONTAL_GRID_TYPE, "0"))); //$NON-NLS-1$
					this.setHorizontalGridRecordOrdinal(record.getOrdinal()); // initial use top score trail record
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

	public Map<Integer, Vector<? extends AbstractRecord>> getScaleSyncedRecords() {
		return this.scaleSyncedRecords;
	}

	/**
	 * @return the Vector containing the slave records sync by the master name
	 */
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

}