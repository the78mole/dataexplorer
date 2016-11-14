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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
    					2016 Thomas Eickert
****************************************************************************************/

package gde.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import gde.GDE;
import gde.config.GraphicsTemplate;
import gde.config.HistoGraphicsTemplate;
import gde.config.Settings;
import gde.data.TrailRecord.TrailType;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.device.SettlementType;
import gde.device.StatisticsType;
import gde.exception.DataInconsitsentException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

/**
 * holds histo trail records for the configured measurements of a device supplemented by settlements.
 * @author Thomas Eickert
 */
public class TrailRecordSet extends RecordSet {
	final static String									$CLASS_NAME						= TrailRecordSet.class.getName();
	final static long										serialVersionUID			= -1580283867987273535L;
	final static Logger									log										= Logger.getLogger($CLASS_NAME);

	final static int										initialRecordCapacity	= 111;														// vector capacity values are crucial for performance

	private final HistoGraphicsTemplate	template;																								// graphics template holds view configuration

	/**
	 * holds trails records for measurements and settlements.
	 * @param useDevice the instance of the device 
	 * @param channelNumber the channel number to be used
	 * @param settingsRecordSet holds the settings for the curves
	 */
	public TrailRecordSet(IDevice useDevice, int channelNumber, String[] recordNames) {
		super(useDevice, channelNumber, "Trail", recordNames, -1, true, true);
		String deviceSignature = useDevice.getName() + GDE.STRING_UNDER_BAR + channelNumber;
		this.template = new HistoGraphicsTemplate(deviceSignature);
		if (this.template != null) this.template.load();

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, " TrailRecordSet(IDevice, int, RecordSet"); //$NON-NLS-1$
	}

	/**
	 * copy constructor - used to copy a trail record set where the configuration comes from the device properties file.
	 * @param recordSet
	 * @param newChannelConfiguration
	 */
	@Deprecated
	private TrailRecordSet(TrailRecordSet recordSet, int dataIndex, boolean isFromBegin) {
		super(recordSet, dataIndex, isFromBegin);
		this.template = null;
	}

	/**
	 * clone method re-writes data points of all records of this record set.
	 * @param dataIndex
	 * @param isFromBegin - if true, the given index is the index where the record starts after this operation.  - if false, the given index represents the last data point index of the records
	 * @return new created trail record set
	 */
	@Override
	@Deprecated
	public TrailRecordSet clone(int dataIndex, boolean isFromBegin) {
		return new TrailRecordSet(this, dataIndex, isFromBegin);
	}

	/**
	 * create a trail record set containing records according the channel configuration which is loaded from device properties file.
	 * @param device the instance of the device 
	 * @param channelConfigNumber (number of the outlet or configuration)
	 * @return a trail record set containing all trail records (empty) as specified
	 */
	public static TrailRecordSet createRecordSet(IDevice device, int channelConfigNumber) {
		DeviceConfiguration deviceConfiguration = (DeviceConfiguration) device;
		String[] names = deviceConfiguration.getMeasurementSettlementScoregroupNames(channelConfigNumber);
		if (names.length == 0) { // simple check for valid device and record names, as fall back use the config from the first channel/configuration
			names = deviceConfiguration.getMeasurementSettlementScoregroupNames(channelConfigNumber = 1);
		}
		TrailRecordSet newTrailRecordSet = new TrailRecordSet(device, channelConfigNumber, names);
		printRecordNames("createRecordSet() " + newTrailRecordSet.name + " - ", newTrailRecordSet.getRecordNames()); //$NON-NLS-1$ //$NON-NLS-2$
		newTrailRecordSet.timeStep_ms = new TimeSteps(-1, initialRecordCapacity);
		List<SettlementType> channelSettlements = deviceConfiguration.getChannel(channelConfigNumber).getSettlement();
		List<MeasurementType> channelMeasuremts = deviceConfiguration.getChannelMeasuremts(channelConfigNumber);

		// TODO dieses Zusammenmischen in den Sections 1 - 3 aufgeben - stattdessen ein Override von getRecordsSortedForDisplay
		// display section 1: look for settlements at the top - settlements' ordinals start after measurements due to GraphicsTemplate compatibility
		for (int i = 0, myIndex = channelMeasuremts.size(); i < channelSettlements.size(); i++) { // myIndex is used as recordOrdinal
			SettlementType settlement = channelSettlements.get(i);
			PropertyType topPlacementProperty = settlement.getProperty("histo_top_placement");
			if (topPlacementProperty != null ? Boolean.valueOf(topPlacementProperty.getValue()) : false) {
				TrailRecord tmpRecord = new TrailRecord(device, myIndex, settlement.getName(), settlement, newTrailRecordSet, initialRecordCapacity);
				newTrailRecordSet.put(settlement.getName(), tmpRecord);
				tmpRecord.setColorDefaultsAndPosition(myIndex);
				// if (log.isLoggable(Level.FINE))
				log.log(Level.SEVERE, "added settlement record for " + settlement.getName() + " - " + myIndex); //$NON-NLS-1$
			}
			myIndex++;
		}
		// display section 2: all measurements
		for (int i = 0; i < channelMeasuremts.size(); i++) {
			MeasurementType measurement = device.getMeasurement(channelConfigNumber, i);
			TrailRecord tmpRecord = new TrailRecord(device, i, measurement.getName(), measurement, newTrailRecordSet, initialRecordCapacity); // ordinal starts at 0
			newTrailRecordSet.put(measurement.getName(), tmpRecord);
			tmpRecord.setColorDefaultsAndPosition(i);
			// if (log.isLoggable(Level.FINE))
			log.log(Level.SEVERE, "added measurement record for " + measurement.getName() + " - " + i); //$NON-NLS-1$
		}
		// display section 3: take remaining settlements
		for (int i = 0, myIndex = channelMeasuremts.size(); i < channelSettlements.size(); i++) { // myIndex is used as recordOrdinal
			SettlementType settlement = channelSettlements.get(i);
			PropertyType topPlacementProperty = settlement.getProperty("histo_top_placement");
			if (!(topPlacementProperty != null ? Boolean.valueOf(topPlacementProperty.getValue()) : false)) {
				TrailRecord tmpRecord = new TrailRecord(device, myIndex, settlement.getName(), settlement, newTrailRecordSet, initialRecordCapacity);
				newTrailRecordSet.put(settlement.getName(), tmpRecord);
				tmpRecord.setColorDefaultsAndPosition(myIndex);
				// if (log.isLoggable(Level.FINE))
				log.log(Level.SEVERE, "added settlement record for " + settlement.getName() + " - " + myIndex); //$NON-NLS-1$
			}
			myIndex++; // 
		}
		newTrailRecordSet.syncScaleOfSyncableRecords();
		return newTrailRecordSet;
	}

	/**
	 * method to create a record set with given name "1) Laden" containing records according the device channle/configuration
	 * which are loaded from device properties file
	 * @param recordSetName the name of the record set
	 * @param device the instance of the device 
	 * @param channelConfigNumber (number of the outlet or configuration)
	 * @param isRaw defines if the data needs translation using device specific properties
	 * @param isFromFile defines if a configuration change must be recorded to signal changes
	 * @return a record set containing all records (empty) as specified
	 */
	@Deprecated
	public static RecordSet createRecordSet(String recordSetName, IDevice device, int channelConfigNumber, boolean isRaw, boolean isFromFile) {
		throw new UnsupportedOperationException();
	}

	/**
	 * method to create a record set with given name "1) Laden" containing records according the given record names, symbols and units
	 * active status as well as statistics and properties are used from device properties
	 * @param recordSetName the name of the record set
	 * @param device the instance of the device 
	 * @param channelConfigNumber (name of the outlet or configuration)
	 * @param recordNames array of names to be used for created records
	 * @param recordSymbols array of symbols to be used for created records
	 * @param recordUnits array of units to be used for created records
	 * @param timeStep_ms 
	 * @param isRaw defines if the data needs translation using device specific properties
	 * @param isFromFile defines if a configuration change must be recorded to signal changes
	 * @return a record set containing all records (empty) as specified
	 */
	@Deprecated
	public static RecordSet createRecordSet(String recordSetName, IDevice device, int channelConfigNumber, String[] recordNames, String[] recordSymbols, String[] recordUnits, double timeStep_ms,
			boolean isRaw, boolean isFromFile) {
		throw new UnsupportedOperationException();
	}

	/**
	 * add a series of measurement and settlement points to the associated records.
	 * @param points, where the length must fit to the size of this recordset.
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addNullablePoints(Integer[] points) throws DataInconsitsentException {
		final String $METHOD_NAME = "addNullablePoints"; //$NON-NLS-1$
		if (points.length == super.size()) {
			for (int i = 0; i < points.length; i++) {
				super.getRecord(super.recordNames[i]).add(points[i]);
			}
			if (log.isLoggable(Level.FINEST)) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < points.length; i++) {
					sb.append(points[i]).append(GDE.STRING_BLANK);
				}
				log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, sb.toString());
			}
		}
		else
			throw new DataInconsitsentException(Messages.getString(MessageIds.GDE_MSGE0035, new Object[] { this.getClass().getSimpleName(), $METHOD_NAME, points.length, super.size() })); // $NON-NLS-1$

		super.hasDisplayableData = true;
	}

	/**
	 * clears and fills the aggregated values into the trailRecords associated with the measurement or settlement.
	 * takes time steps and data from all histo record sets for the current channel.
	 * takes only those aggregated values which are assigned to the selected trail type.
	 * @param histoSet
	 */
	public void addHisto(HistoSet histoSet) {
		super.timeStep_ms.clear();
		for (String recordName : super.getRecordNames()) {
			((TrailRecord) super.get(recordName)).clear();
		}
		for (Map.Entry<Long, List<HistoRecordSet>> entry : histoSet.entrySet()) {
			for (HistoRecordSet histoRecordSet : entry.getValue()) {
				if (log.isLoggable(Level.OFF)) log.log(Level.OFF, String.format("recordSet  startTimeStamp %,d  -- entry.key %,d", histoRecordSet.getStartTimeStamp(), entry.getKey())); //$NON-NLS-1$
				super.timeStep_ms.addRaw(histoRecordSet.getStartTimeStamp() * 10);
				for (String recordName : super.getRecordNames()) {
					((TrailRecord) super.get(recordName)).addHisto(histoRecordSet);
				}
			}
		}
		syncScaleOfSyncableRecords();
	}

	/**
	 * inform trail records about the trail types which are allowed, set trail selection list and current trailType / score. 
	 * @param isLiveActive 
	 */
	public void defineTrailTypes() {
		String[] trailRecordNames = super.getRecordNames();
		for (int j = 0; j < trailRecordNames.length; j++) {
			TrailRecord trailRecord = ((TrailRecord) super.get(trailRecordNames[j]));
			trailRecord.setApplicableTrailTypes();
			applyTemplateTrailData(trailRecord.getOrdinal());
		}
	}

	/**
	 * save the histo graphics definition into template file
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
			// smooth current drop
			// this.template.setProperty(RecordSet.SMOOTH_AT_CURRENT_DROP, Boolean.toString(recordSet.isSmoothAtCurrentDrop()));
			// recordSet.setSmoothAtCurrentDrop(Boolean.valueOf(this.template.getProperty(RecordSet.SMOOTH_AT_CURRENT_DROP, "false"))); //$NON-NLS-1$
			// smooth voltage curve
			// this.template.setProperty(RecordSet.SMOOTH_VOLTAGE_CURVE, Boolean.toString(recordSet.isSmoothAtCurrentDrop()));
			// recordSet.setSmoothVoltageCurve(Boolean.valueOf(this.template.getProperty(RecordSet.SMOOTH_VOLTAGE_CURVE, "false"))); //$NON-NLS-1$
			// time grid
			// color = this.getColorTimeGrid();
			// rgb = color.getRGB().red + GDE.STRING_COMMA + color.getRGB().green + GDE.STRING_COMMA + color.getRGB().blue;
			// this.template.setProperty(RecordSet.TIME_GRID_COLOR, rgb);
			// this.template.setProperty(RecordSet.TIME_GRID_LINE_STYLE, Integer.valueOf(recordSet.getLineStyleTimeGrid()).toString());
			// this.template.setProperty(RecordSet.TIME_GRID_TYPE, Integer.valueOf(recordSet.getTimeGridType()).toString());
			// // curve grid
			// color = this.getHorizontalGridColor();
			// rgb = color.getRGB().red + GDE.STRING_COMMA + color.getRGB().green + GDE.STRING_COMMA + color.getRGB().blue;
			// this.template.setProperty(RecordSet.HORIZONTAL_GRID_COLOR, rgb);
			// this.template.setProperty(RecordSet.HORIZONTAL_GRID_LINE_STYLE, Integer.valueOf(recordSet.getHorizontalGridLineStyle()).toString());
			// this.template.setProperty(RecordSet.HORIZONTAL_GRID_TYPE, Integer.valueOf(recordSet.getHorizontalGridType()).toString());
			// if (recordSet.get(recordSet.getHorizontalGridRecordOrdinal()) != null) {
			// this.template.setProperty(RecordSet.HORIZONTAL_GRID_RECORD_ORDINAL, GDE.STRING_EMPTY + recordSet.getHorizontalGridRecordOrdinal());
			// }
			// histo properties
			this.template.setProperty(i + TrailRecord.TRAIL_TEXT_ORDINAL, String.valueOf(record.getTrailOrdinal()));
		}
		this.template.store();
		// if (log.isLoggable(Level.FINE))
		log.log(Level.SEVERE, "creating histo graphics template file in " + this.template.getCurrentFilePath()); //$NON-NLS-1$
	}

	/**
	 * apply the data source information (= comboBox setting) from the graphics template definition to a record set.
	 * look for trail type and score information in the histo graphics template.
	 * take the prioritized trail type from applicable trails if no template setting is available.
	 * @param recordOrdinal
	 */
	public void applyTemplateTrailData(int recordOrdinal) {
		boolean isValidTemplate = this.template != null && this.template.isAvailable();
		TrailRecord record = (TrailRecord) this.get(recordOrdinal);
		// histo properties
		if (isValidTemplate && this.template.getProperty(recordOrdinal + TrailRecord.TRAIL_TEXT_ORDINAL) != null) {
			record.setTrailTextSelectedIndex(Integer.parseInt(this.template.getProperty(recordOrdinal + TrailRecord.TRAIL_TEXT_ORDINAL)));
		}
		else { // the template is not a histo template or the property is a new measurement / settlement
			record.setMostApplicableTrailTextOrdinal();
		}
		if (record.getTrailTextSelectedIndex() < 0 ) {
			log.log(Level.INFO, String.format("%s : no trail types identified", record.getName())); //$NON-NLS-1$
		} else {
		// if (log.isLoggable(Level.FINE))
		log.log(Level.SEVERE, String.format("%s : selected trail type=%s ordinal=%d", record.getName(), record.getTrailText(), record.getTrailOrdinal())); //$NON-NLS-1$
		}
	}

	/**
	 * apply the graphics template definition to a record set
	 * @param doUpdateVisibilityStatus example: if the histo data do not hold data for this record it makes no sense to display the curve.
	 */
	public void applyTemplate(boolean doUpdateVisibilityStatus) {
		if (this.template != null && this.template.isAvailable()) {
			for (int i = 0; i < this.size(); ++i) {
				TrailRecord record = (TrailRecord) this.get(i);
				record.setVisible(Boolean.parseBoolean(this.template.getProperty(i + Record.IS_VISIBLE, "true"))); //$NON-NLS-1$
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
				// smooth current drop
				// recordSet.setSmoothAtCurrentDrop(Boolean.valueOf(this.template.getProperty(RecordSet.SMOOTH_AT_CURRENT_DROP, "false"))); //$NON-NLS-1$
				// smooth voltage curve
				// recordSet.setSmoothVoltageCurve(Boolean.valueOf(this.template.getProperty(RecordSet.SMOOTH_VOLTAGE_CURVE, "false"))); //$NON-NLS-1$
				// time grid
				// color = this.template.getProperty(RecordSet.TIME_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
				// r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
				// g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
				// b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
				// recordSet.setTimeGridColor(SWTResourceManager.getColor(r, g, b));
				// recordSet.setTimeGridLineStyle(Integer.valueOf(this.template.getProperty(RecordSet.TIME_GRID_LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_DOT)).intValue());
				// recordSet.setTimeGridType(Integer.valueOf(this.template.getProperty(RecordSet.TIME_GRID_TYPE, "0")).intValue()); //$NON-NLS-1$
				// curve grid
				// color = this.template.getProperty(RecordSet.HORIZONTAL_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
				// r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
				// g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
				// b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
				// recordSet.setHorizontalGridColor(SWTResourceManager.getColor(r, g, b));
				// recordSet.setHorizontalGridLineStyle(Integer.valueOf(this.template.getProperty(RecordSet.HORIZONTAL_GRID_LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_DOT)).intValue());
				// recordSet.setHorizontalGridType(Integer.valueOf(this.template.getProperty(RecordSet.HORIZONTAL_GRID_TYPE, "0")).intValue()); //$NON-NLS-1$
				// recordSet.setHorizontalGridRecordOrdinal(Integer.valueOf(this.template.getProperty(RecordSet.HORIZONTAL_GRID_RECORD_ORDINAL, "-1")).intValue()); //$NON-NLS-1$
				// histo properties
			}
			this.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
			// if (log.isLoggable(Level.FINE))
			log.log(Level.SEVERE, "applied histo graphics template file " + this.template.getCurrentFilePath()); //$NON-NLS-1$
			if (doUpdateVisibilityStatus)

			{
				updateVisibilityStatus(true);
			}
			// if (this.activeRecordSet != null && recordSet.getName().equals(this.activeRecordSet.name) && this.application.getMenuBar() != null) {
			this.application.updateGraphicsWindow();
			// }
		}

	}

	/**
	 * check and update visibility status of all records according to the available histo data.
	 * at least an update of the graphics window should be included at the end of this method.
	 */
	public void updateVisibilityStatus(boolean includeReasonableDataCheck) {
		int channelConfigNumber = this.getChannelConfigNumber();
		int displayableCounter = 0;
		Record record;

		for (int i = 0; i < this.size(); ++i) {
			record = this.get(i);
			if (includeReasonableDataCheck) {
				record.setDisplayable(record.isActive() || record.hasReasonableData()); // TODO was initially: (record.hasReasonableData());
				if (log.isLoggable(Level.SEVERE)) log.log(Level.SEVERE, record.getName() + " hasReasonableData " + record.hasReasonableData()); //$NON-NLS-1$
			}

			if (record.isActive() && record.isDisplayable()) {
				++displayableCounter;
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
			}
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		this.setConfiguredDisplayable(displayableCounter);
	}

	/**
	 * synchronize scales according device properties.
	 * support settlements.
	 */
	@Override // reason is access to getFactor, getUnit etc. via this.device.getMeasruementProperty
	public void syncScaleOfSyncableRecords() {
		this.scaleSyncedRecords.clear();
		for (int i = 0; i < this.size() && !this.isCompareSet; i++) {
			// ET overridden method implemented final PropertyType syncProperty = this.isUtilitySet ? this.get(i).getProperty(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value()) : this.device.getMeasruementProperty(this.parent.number, i, MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value());
			final TrailRecord tmpRecord = (TrailRecord) this.get(i);
			final PropertyType syncProperty = tmpRecord.getProperty(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value());
			if (syncProperty != null && !syncProperty.getValue().equals(GDE.STRING_EMPTY)) {
				final int syncMasterRecordOrdinal = Integer.parseInt(syncProperty.getValue());
				if (syncMasterRecordOrdinal >= 0) {
					if (this.scaleSyncedRecords.get(syncMasterRecordOrdinal) == null) {
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "add syncMaster " + this.get(syncMasterRecordOrdinal).name);
						this.scaleSyncedRecords.put(syncMasterRecordOrdinal, new Vector<Record>());
						this.scaleSyncedRecords.get(syncMasterRecordOrdinal).add(this.get(syncMasterRecordOrdinal));
						this.get(syncMasterRecordOrdinal).syncMinValue = Integer.MAX_VALUE;
						this.get(syncMasterRecordOrdinal).syncMaxValue = Integer.MIN_VALUE;
					}
					if (!this.isRecordContained(syncMasterRecordOrdinal, tmpRecord)) {
						if (Math.abs(i - syncMasterRecordOrdinal) >= this.scaleSyncedRecords.get(syncMasterRecordOrdinal).size())
							this.scaleSyncedRecords.get(syncMasterRecordOrdinal).add(tmpRecord);
						else
							// sort while add
							this.scaleSyncedRecords.get(syncMasterRecordOrdinal).add(Math.abs(i - syncMasterRecordOrdinal), tmpRecord);

						this.syncMasterSlaveRecords(this.get(syncMasterRecordOrdinal), Record.TYPE_AXIS_END_VALUES);
						this.syncMasterSlaveRecords(this.get(syncMasterRecordOrdinal), Record.TYPE_AXIS_NUMBER_FORMAT);
						this.syncMasterSlaveRecords(this.get(syncMasterRecordOrdinal), Record.TYPE_AXIS_SCALE_POSITION);
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "add " + tmpRecord.name);
					}
				}
			}
		}
		if (log.isLoggable(Level.FINE)) {
			StringBuilder sb = new StringBuilder();
			for (Integer syncRecordOrdinal : this.scaleSyncedRecords.keySet()) {
				sb.append("\n").append(syncRecordOrdinal).append(GDE.STRING_COLON);
				for (Record tmpRecord : this.scaleSyncedRecords.get(syncRecordOrdinal)) {
					sb.append(tmpRecord.name).append(GDE.STRING_SEMICOLON);
				}
			}
			log.log(Level.FINE, sb.toString());
		}
	}

	/**
	 * method to get the sorted record names as array for display purpose
	 * sorted according display requirement, grid record first, syncMasterRecords second, all remaining.
	 * support trail suites.
	 * @return Record[] containing records
	 */
	@Override // reason is: The data vector of trail records holding a record suite is empty -> (TrailRecord) record).getTrailRecordSuite().length > 1
	public Record[] getRecordsSortedForDisplay() {
		Vector<Record> displayRecords = new Vector<Record>();
		// add the record with horizontal grid
		for (Record record : this.values()) {
			if ((record.size() > 0 || (((TrailRecord) record).getTrailRecordSuite() != null && ((TrailRecord) record).getTrailRecordSuite().length > 1)) && record.ordinal == this.horizontalGridRecordOrdinal) displayRecords.add(record);
		}
		// add the scaleSyncMaster records to draw scale of this records first which sets the min/max display values
		for (int i = 0; i < this.size(); ++i) {
			final Record record = this.get(i);
			if ((record.size() > 0 || (((TrailRecord) record).getTrailRecordSuite() != null && ((TrailRecord) record).getTrailRecordSuite().length > 1)) && record.ordinal != this.horizontalGridRecordOrdinal && record.isScaleSyncMaster())
				displayRecords.add(record);
		}
		// add all others
		for (int i = 0; i < this.size(); ++i) {
			final Record record = this.get(i);
			if ((record.size() > 0 || (((TrailRecord) record).getTrailRecordSuite() != null && ((TrailRecord) record).getTrailRecordSuite().length > 1)) && record.ordinal != this.horizontalGridRecordOrdinal && !record.isScaleSyncMaster())
				displayRecords.add(record);
		}

		return displayRecords.toArray(new TrailRecord[displayRecords.size()]);
	}

}