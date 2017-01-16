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
    
    Copyright (c) 2016 Thomas Eickert
****************************************************************************************/

package gde.data;

import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import gde.GDE;
import gde.config.HistoGraphicsTemplate;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.device.ScoreLabelTypes;
import gde.device.ScoregroupType;
import gde.device.SettlementType;
import gde.exception.DataInconsitsentException;
import gde.histocache.HistoVault;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

/**
 * holds histo trail records for the configured measurements of a device supplemented by settlements and scores.
 * The display sequence is the linked hashmap sequence whereas the ordinals refer to the sequence of measurements + settlements + scoregroups.
 * @author Thomas Eickert
 */
public class TrailRecordSet extends RecordSet {
	private final static String					$CLASS_NAME							= TrailRecordSet.class.getName();
	private final static long						serialVersionUID				= -1580283867987273535L;
	private final static Logger					log											= Logger.getLogger($CLASS_NAME);

	private final static int						initialRecordCapacity		= 111;
	private final String								isoDateTimeDelimiter		= "T";																					//$NON-NLS-1$

	private final HistoGraphicsTemplate	template;																																// graphics template holds view configuration
	private final int[]									linkedOrdinals;																													// allows getting a trail record by ordinal without iterating the linked hashmap  

	private final List<Integer>					durations_mm						= new ArrayList<Integer>(initialRecordCapacity);
	private double											averageDuration_mm			= 0;
	private final List<String>					fileNames								= new ArrayList<String>(initialRecordCapacity);
	private final List<String>					directoryNames					= new ArrayList<String>(initialRecordCapacity);
	private final List<String>					basePaths								= new ArrayList<String>(initialRecordCapacity);
	private final List<String>					logChannelNumbers				= new ArrayList<String>(initialRecordCapacity);
	private final List<String>					logRectifiedObjectKeys	= new ArrayList<String>(initialRecordCapacity);
	private final List<String>					logRecordsetBaseNames		= new ArrayList<String>(initialRecordCapacity);
	private final List<String>					logRecordSetOrdinals		= new ArrayList<String>(initialRecordCapacity);
	private final List<List<String>>		logTags									= new ArrayList<List<String>>();

	/**
	 * holds trail records for measurements, settlements and scores.
	 * @param useDevice the instance of the device 
	 * @param channelNumber the channel number to be used
	 * @param settingsRecordSet holds the settings for the curves
	 */
	public TrailRecordSet(IDevice useDevice, int channelNumber, String[] recordNames) {
		super(useDevice, channelNumber, "Trail", recordNames, -1, true, true);
		String deviceSignature = useDevice.getName() + GDE.STRING_UNDER_BAR + channelNumber;
		this.template = new HistoGraphicsTemplate(deviceSignature);
		this.linkedOrdinals = new int[recordNames.length];
		if (this.template != null) this.template.load();

		this.logTags.add(this.fileNames);
		this.logTags.add(this.directoryNames);
		this.logTags.add(this.basePaths);
		this.logTags.add(this.logChannelNumbers);
		this.logTags.add(this.logRectifiedObjectKeys);
		this.logTags.add(this.logRecordsetBaseNames);
		// for test only		this.logTags.add(this.logRecordSetOrdinals);

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
		this.linkedOrdinals = new int[isFromBegin ? recordSet.size() - dataIndex : dataIndex + 1];
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
	 * the trail records' display sequence (= LinkedHashMap sequence) supports pinning score / settlement records at the top based on device xml settings. 
	 * @param device the instance of the device 
	 * @param channelConfigNumber (number of the outlet or configuration)
	 * @return a trail record set containing all trail records (empty) as specified
	 */
	public static TrailRecordSet createRecordSet(IDevice device, int channelConfigNumber) {
		String[] names = device.getDeviceConfiguration().getMeasurementSettlementScoregroupNames(channelConfigNumber);
		TrailRecordSet newTrailRecordSet = new TrailRecordSet(device, channelConfigNumber, names);
		printRecordNames("createRecordSet() " + newTrailRecordSet.name + " - ", newTrailRecordSet.getRecordNames()); //$NON-NLS-1$ //$NON-NLS-2$
		newTrailRecordSet.timeStep_ms = new TimeSteps(-1, initialRecordCapacity);
		List<MeasurementType> channelMeasurements = device.getDeviceConfiguration().getChannelMeasuremts(channelConfigNumber);
		List<SettlementType> channelSettlements = device.getDeviceConfiguration().getChannel(channelConfigNumber).getSettlement();
		List<ScoregroupType> channelScoregroups = device.getDeviceConfiguration().getChannel(channelConfigNumber).getScoregroup();

		// display section 0: look for scores at the top - scores' ordinals start after measurements + settlements due to GraphicsTemplate compatibility
		for (int i = 0, myIndex = channelMeasurements.size() + channelSettlements.size(); i < channelScoregroups.size(); i++) { // myIndex is used as recordOrdinal
			ScoregroupType scoregroup = channelScoregroups.get(i);
			PropertyType topPlacementProperty = scoregroup.getProperty("histo_top_placement");
			if (topPlacementProperty != null ? Boolean.valueOf(topPlacementProperty.getValue()) : false) {
				TrailRecord tmpRecord = new TrailRecord(device, myIndex, scoregroup.getName(), scoregroup, newTrailRecordSet, scoregroup.getProperty().size());
				newTrailRecordSet.put(scoregroup.getName(), tmpRecord);
				tmpRecord.setColorDefaultsAndPosition(myIndex);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "added scoregroup record for " + scoregroup.getName() + " - " + myIndex); //$NON-NLS-1$
			}
			myIndex++;
		}
		// display section 1: look for settlements at the top - settlements' ordinals start after measurements due to GraphicsTemplate compatibility
		for (int i = 0, myIndex = channelMeasurements.size(); i < channelSettlements.size(); i++) { // myIndex is used as recordOrdinal
			SettlementType settlement = channelSettlements.get(i);
			PropertyType topPlacementProperty = settlement.getProperty("histo_top_placement");
			if (topPlacementProperty != null ? Boolean.valueOf(topPlacementProperty.getValue()) : false) {
				TrailRecord tmpRecord = new TrailRecord(device, myIndex, settlement.getName(), settlement, newTrailRecordSet, initialRecordCapacity);
				newTrailRecordSet.put(settlement.getName(), tmpRecord);
				tmpRecord.setColorDefaultsAndPosition(myIndex);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "added settlement record for " + settlement.getName() + " - " + myIndex); //$NON-NLS-1$
			}
			myIndex++;
		}
		// display section 2: all measurements
		for (int i = 0; i < channelMeasurements.size(); i++) {
			MeasurementType measurement = device.getMeasurement(channelConfigNumber, i);
			TrailRecord tmpRecord = new TrailRecord(device, i, measurement.getName(), measurement, newTrailRecordSet, initialRecordCapacity); // ordinal starts at 0
			newTrailRecordSet.put(measurement.getName(), tmpRecord);
			tmpRecord.setColorDefaultsAndPosition(i);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "added measurement record for " + measurement.getName() + " - " + i); //$NON-NLS-1$
		}
		// display section 3: take remaining settlements
		for (int i = 0, myIndex = channelMeasurements.size(); i < channelSettlements.size(); i++) { // myIndex is used as recordOrdinal
			SettlementType settlement = channelSettlements.get(i);
			PropertyType topPlacementProperty = settlement.getProperty("histo_top_placement");
			if (!(topPlacementProperty != null ? Boolean.valueOf(topPlacementProperty.getValue()) : false)) {
				TrailRecord tmpRecord = new TrailRecord(device, myIndex, settlement.getName(), settlement, newTrailRecordSet, initialRecordCapacity);
				newTrailRecordSet.put(settlement.getName(), tmpRecord);
				tmpRecord.setColorDefaultsAndPosition(myIndex);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "added settlement record for " + settlement.getName() + " - " + myIndex); //$NON-NLS-1$
			}
			myIndex++; // 
		}
		// display section 4: take remaining scores
		for (int i = 0, myIndex = channelMeasurements.size() + channelSettlements.size(); i < channelScoregroups.size(); i++) { // myIndex is used as recordOrdinal
			ScoregroupType scoregroup = channelScoregroups.get(i);
			PropertyType topPlacementProperty = scoregroup.getProperty("histo_top_placement");
			if (!(topPlacementProperty != null ? Boolean.valueOf(topPlacementProperty.getValue()) : false)) {
				TrailRecord tmpRecord = new TrailRecord(device, myIndex, scoregroup.getName(), scoregroup, newTrailRecordSet, scoregroup.getProperty().size());
				newTrailRecordSet.put(scoregroup.getName(), tmpRecord);
				tmpRecord.setColorDefaultsAndPosition(myIndex);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "added settlement record for " + scoregroup.getName() + " - " + myIndex); //$NON-NLS-1$
			}
			myIndex++; // 
		}
		newTrailRecordSet.syncScaleOfSyncableRecords();
		// build display sequence index array
		List<String> recordNameList = Arrays.asList(newTrailRecordSet.getRecordNames());
		Iterator<java.util.Map.Entry<String, Record>> iterator = newTrailRecordSet.entrySet().iterator();
		for (int i = 0; i < newTrailRecordSet.size(); i++) {
			newTrailRecordSet.linkedOrdinals[i] = recordNameList.indexOf(iterator.next().getKey());
		}
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
	 * clears the data points in all records and in the time steps.
	 * keeps initial capacities.
	 * does not clear any fields in the recordSet, the records or in timeStep. 
	 */
	public void cleanup() {
		super.timeStep_ms.clear();
		for (String recordName : super.getRecordNames()) {
			((TrailRecord) super.get(recordName)).clear();
		}
		this.durations_mm.clear();
		this.averageDuration_mm = 0;

		this.fileNames.clear();
		this.directoryNames.clear();
		this.basePaths.clear();
		this.logChannelNumbers.clear();
		this.logRectifiedObjectKeys.clear();
		this.logRecordSetOrdinals.clear();
		this.logRecordsetBaseNames.clear();
	}

	/**
	 * method to add a series of points to the associated records
	 * @param points as int[], where the length must fit records.size()
	 * @throws DataInconsitsentException 
	 */
	@Deprecated
	@Override
	public synchronized void addPoints(int[] points) throws DataInconsitsentException {
		throw new UnsupportedOperationException();
	}

	/**
	 * method to add a series of points to the associated records
	 * @param points as int[], where the length must fit records.size()
	 * @param time_ms
	 * @throws DataInconsitsentException 
	 */
	@Deprecated
	@Override
	public synchronized void addPoints(int[] points, double time_ms) throws DataInconsitsentException {
		throw new UnsupportedOperationException();
	}

	/**
	 * method to add a series of points to none calculation records (records active or inactive)
	 * @param points as int[], where the length must fit records.size()
	 * @throws DataInconsitsentException 
	 */
	@Deprecated
	@Override
	public synchronized void addNoneCalculationRecordsPoints(int[] points) throws DataInconsitsentException {
		throw new UnsupportedOperationException();
	}

	/**
	 * method to add a series of points to none calculation records (records active or inactive)
	 * @param points as int[], where the length must fit records.size()
	 * @param time_ms
	 * @throws DataInconsitsentException 
	 */
	@Deprecated
	@Override
	public synchronized void addNoneCalculationRecordsPoints(int[] points, double time_ms) throws DataInconsitsentException {
		throw new UnsupportedOperationException();
	}

	/**
	 * add a new time step to the time steps vector
	 * @param timeValue
	 */
	@Deprecated
	@Override
	public void addTimeStep_ms(double timeValue) {
		throw new UnsupportedOperationException();
	}

	/**
	 * set time steps for the trail recordset and the data points for all displayable trail records.
	 * every record takes the selected trail type / score data from the history vault and populates its data. 
	 */
	public void setPoints() {
		for (Map.Entry<Long, List<HistoVault>> entry : HistoSet.getInstance().entrySet()) {
			for (HistoVault histoVault : entry.getValue()) {
				int duration_mm = histoVault.getScorePoint(ScoreLabelTypes.DURATION_MM.ordinal());
				this.durations_mm.add(duration_mm);
				this.averageDuration_mm += (duration_mm - this.averageDuration_mm) / this.durations_mm.size();
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("recordSet  startTimeStamp %,d  -- entry.key %,d", histoVault.getLogStartTimestamp_ms(), entry.getKey())); //$NON-NLS-1$
				this.timeStep_ms.addRaw(histoVault.getLogStartTimestamp_ms() * 10);
				for (String recordName : this.getRecordNames()) {
					((TrailRecord) this.get(recordName)).add(histoVault);
				}
			}
		}
		syncScaleOfSyncableRecords();
	}

	/**
	 * query the size of record set child record 
	 * - normal record set will return the size of the data vector of first active in recordNames
	 * - zoomed set will return size of zoomOffset + zoomWith
	 * @param isReal false return the size of zoomOffset + zoomWith
	 * @return the size of data point to calculate the time unit
	 */
	@Override // trail suite support
	public int getRecordDataSize(boolean isReal) {
		int size = 0;
		if (isReal) {
			for (String recordKey : this.recordNames) {
				if (get(recordKey).isActive()) {
					size = ((TrailRecord) get(recordKey)).getTrailRecordSuite()[0].realSize();
					break;
				}
			}
		}
		else {
			for (String recordKey : this.recordNames) {
				if (get(recordKey).isActive()) {
					size = ((TrailRecord) get(recordKey)).getTrailRecordSuite()[0].realSize();
					break;
				}
			}
		}
		return size;
	}

	/**
	 * set the data points for one single trail record.
	 * the record takes the selected trail type / score data from the trail record vault and populates its data. 
	 * @param recordOrdinal
	 */
	public void setPoints(int recordOrdinal) {
		TrailRecord trailRecord = (TrailRecord) super.get(recordOrdinal);
		// the vault does hot hold data for non displayable records (= inactive records)
		if (trailRecord.isDisplayable) {
			trailRecord.clear();
			for (Map.Entry<Long, List<HistoVault>> entry : HistoSet.getInstance().entrySet()) {
				for (HistoVault histoVault : entry.getValue()) {
					trailRecord.add(histoVault);
				}
			}
			syncScaleOfSyncableRecords();
		}
		else {
			if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, "access vault for non displayable record " + trailRecord.getName()); //$NON-NLS-1$
		}
	}

	/**
	 * sets tagging information for the trail entries.
	 */
	public void setTags() {
		for (Map.Entry<Long, List<HistoVault>> entry : HistoSet.getInstance().entrySet()) {
			for (HistoVault histoVault : entry.getValue()) {
				this.fileNames.add(Paths.get(histoVault.getLogFilePath()).getFileName().toString());
				this.directoryNames.add(Paths.get(histoVault.getLogFilePath()).getParent().getFileName().toString().intern());
				this.basePaths.add(Paths.get(histoVault.getLogFilePath()).getParent().getParent().toString().intern());
				this.logChannelNumbers.add(String.valueOf(histoVault.getLogChannelNumber()).intern());
				this.logRectifiedObjectKeys.add(histoVault.getRectifiedObjectKey().intern());
				this.logRecordSetOrdinals.add(String.valueOf(histoVault.getLogRecordSetOrdinal()).intern());
				this.logRecordsetBaseNames.add(histoVault.getLogRecordsetBaseName().intern());
			}
		}
	}

	/**
	 * inform displayable trail records about the trail types which are allowed, set trail selection list and current trailType / score. 
	 * @param isLiveActive 
	 */
	public void defineTrailTypes() {
		String[] trailRecordNames = this.getRecordNames();
		for (int j = 0; j < trailRecordNames.length; j++) {
			TrailRecord trailRecord = ((TrailRecord) this.get(trailRecordNames[j]));
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
			this.template.setProperty(i + TrailRecord.TRAIL_TEXT_ORDINAL, String.valueOf(record.getTrailTextSelectedIndex()));
		}
		this.template.store();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "creating histo graphics template file in " + this.template.getCurrentFilePath()); //$NON-NLS-1$
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
		if (record.getTrailTextSelectedIndex() < 0) {
			log.log(Level.INFO, String.format("%s : no trail types identified", record.getName())); //$NON-NLS-1$
		}
		else {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("%s : selected trail type=%s ordinal=%d", record.getName(), record.getTrailText(), record.getTrailTextSelectedIndex())); //$NON-NLS-1$
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
			if (log.isLoggable(Level.OFF)) log.log(Level.OFF, "applied histo graphics template file " + this.template.getCurrentFilePath()); //$NON-NLS-1$
			if (doUpdateVisibilityStatus) {
				updateVisibleAndDisplayableRecordsForTable();
				updateVisibilityStatus(true);
			}
			// if (this.activeRecordSet != null && recordSet.getName().equals(this.activeRecordSet.name) && this.application.getMenuBar() != null) {
			this.application.updateGraphicsWindow(); // WBrueg histoGraphicsWindow
			// }
		}

	}

	/**
	 * check and update visibility status of all records according to the available histo data.
	 * at least an update of the graphics window should be included at the end of this method.
	 */
	public void updateVisibilityStatus(boolean includeReasonableDataCheck) {
		// WBrueg check if setting records to displayable is required for trailRecordSets: the current isActive settings should be valid for display --> remove this method
		int displayableCounter = 0;
		for (int i = 0; i < this.size(); ++i) {
			Record record = this.get(i);
			if (includeReasonableDataCheck) {
				// todo record.setDisplayable(record.isActive() || record.hasReasonableData()); // was initially: (record.hasReasonableData());
				//	if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " hasReasonableData " + record.hasReasonableData()); //$NON-NLS-1$
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
			if (syncProperty != null && !syncProperty.getValue().isEmpty()) {
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
	@Override // reason is:The data vector of trail records holding a record suite is empty -> (TrailRecord) record).getTrailRecordSuite().length > 1
	public Record[] getRecordsSortedForDisplay() {
		Vector<Record> displayRecords = new Vector<Record>();
		// add the record with horizontal grid
		for (Record record : this.values()) {
			log.log(Level.FINER, record.name);
			if (record.ordinal == this.horizontalGridRecordOrdinal) displayRecords.add(record);
		}
		// add the scaleSyncMaster records to draw scale of this records first which sets the min/max display values
		for (int i = 0; i < this.size(); ++i) {
			final Record record = this.get(i);
			if (record.ordinal != this.horizontalGridRecordOrdinal && record.isScaleSyncMaster()) displayRecords.add(record);
		}
		// add all others
		for (int i = 0; i < this.size(); ++i) {
			final Record record = this.get(i);
			if (record.ordinal != this.horizontalGridRecordOrdinal && !record.isScaleSyncMaster()) displayRecords.add(record);
		}

		return displayRecords.toArray(new TrailRecord[displayRecords.size()]);
	}

	/**
	 * @param sequenceNumber reflects the user sequence
	 * @return
	 */
	public TrailRecord getRecord(int sequenceNumber) {
		return (TrailRecord) super.get(this.linkedOrdinals[sequenceNumber]);
	}

	/**
	 * @return individual durations for all trails
	 */
	public List<Integer> getDurations_mm() {
		return this.durations_mm;
	}

	/**
	 * @return the average of the individual durations for all trails
	 	 */
	public double getAverageDuration_mm() {
		return this.averageDuration_mm;
	}

	/**
	 * update the collection of visible and displayable records in this record set for table view.
	 * the sort order conforms to the record insertion order.
	 */
	@Override // reason is display sequence independent from record names sequence (record ordinal)
	public void updateVisibleAndDisplayableRecordsForTable() {
		this.visibleAndDisplayableRecords.removeAllElements();
		this.allRecords.removeAllElements();
		// get by insertion order
		for (Map.Entry<String, Record> entry : this.entrySet()) {
			final TrailRecord record = (TrailRecord) entry.getValue();
			record.setDisplayable(record.isActive() && record.hasReasonableData());
			if (record.isVisible && record.isDisplayable) //only selected records get displayed
				this.visibleAndDisplayableRecords.add(record);
			if (record.isDisplayable) // only records with reasonable data get displayed
				this.allRecords.add(record);
		}
	}

	/**
	 * @return visible and displayable records (p.e. to build the partial data table) in record insertion order.
	 */
	@Override // reason is display sequence independent from record names sequence (record ordinal)
	public Vector<Record> getVisibleAndDisplayableRecordsForTable() {
		return this.settings.isPartialDataTable() ? this.visibleAndDisplayableRecords : this.allRecords;
	}

	/**
	 * get all tags for all recordsets / vaults.
	 * @param logTagOrdinal
	 * @return empty record name and tag description as a trail text replacement followed by the tag values
	 */
	public String[] getTagTableRow(int logTagOrdinal) {
		String[] dataTableRow = new String[this.get(0).size() + 2];

		if (!this.timeStep_ms.isEmpty()) {
			if (logTagOrdinal == 0)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0838);
			else if (logTagOrdinal == 1)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0839);
			else if (logTagOrdinal == 2)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0840);
			else if (logTagOrdinal == 3)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0841);
			else if (logTagOrdinal == 4)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0842);
			else if (logTagOrdinal == 5)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0844);
			else if (logTagOrdinal == 6)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0843); // for test only
			else
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0845);

			List<String> logTag = this.logTags.get(logTagOrdinal);
			if (this.settings.isXAxisReversed()) {
				for (int i = 0; i < this.timeStep_ms.size(); i++)
					dataTableRow[i + 2] = logTag.get(i);
			}
			else {
				for (int i = 0, j = this.timeStep_ms.size() - 1; i < this.timeStep_ms.size(); i++, j--)
					dataTableRow[i + 2] = logTag.get(j);
			}
		}
		return dataTableRow;
	}

	/**
	 * @return the logTags
	 */
	public List<List<String>> getLogTags() {
		return this.logTags;
	}

	/**
	 * @return the column headers starting with the first data column 
	 */
	public String[] getTableHeaderRow() {
		String[] headerRow = new String[this.timeStep_ms.size()];
		if (this.settings.isXAxisReversed()) {
			for (int i = 0; i < this.timeStep_ms.size(); i++) {
				StringBuilder sb = new StringBuilder();
				ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli((long) this.timeStep_ms.getTime_ms(i)), ZoneId.systemDefault());
				sb.append(zdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace(this.isoDateTimeDelimiter, GDE.STRING_BLANK));
				headerRow[i] = sb.toString();
			}
		}
		else {
			for (int i = 0, j = this.timeStep_ms.size() - 1; i < this.timeStep_ms.size(); i++, j--) {
				StringBuilder sb = new StringBuilder();
				ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli((long) this.timeStep_ms.getTime_ms(i)), ZoneId.systemDefault());
				sb.append(zdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace(this.isoDateTimeDelimiter, GDE.STRING_BLANK));
				headerRow[j] = sb.toString();
			}
		}
		return headerRow;
	}

	public long getFirstTimeStamp_ms() {
		return this.timeStep_ms.firstElement() / 10;
	}

	public long getLastTimeStamp_ms() {
		return this.timeStep_ms.lastElement() / 10;
	}
}