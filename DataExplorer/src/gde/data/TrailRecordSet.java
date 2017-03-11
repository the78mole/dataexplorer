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

package gde.data;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import gde.device.ScoreGroupType;
import gde.device.ScoreLabelTypes;
import gde.device.SettlementType;
import gde.exception.DataInconsitsentException;
import gde.histocache.HistoVault;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;
import gde.utils.LocalizedDateTime;
import gde.utils.LocalizedDateTime.DateTimePattern;

/**
 * holds histo trail records for the configured measurements of a device supplemented by settlements and scores.
 * The display sequence is the linked hashmap sequence whereas the ordinals refer to the sequence of measurements + settlements + scoregroups.
 * @author Thomas Eickert
 */
public class TrailRecordSet extends RecordSet {
	private final static String								$CLASS_NAME							= TrailRecordSet.class.getName();
	private final static long									serialVersionUID				= -1580283867987273535L;
	private final static Logger								log											= Logger.getLogger($CLASS_NAME);

	private final static int									initialRecordCapacity		= 111;

	private final HistoGraphicsTemplate				template;																																// graphics template holds view configuration
	private final int[]												linkedOrdinals;																													// allows getting a trail record by ordinal without iterating the linked hashmap  

	private final List<Integer>								durations_mm						= new ArrayList<Integer>(initialRecordCapacity);
	private double														averageDuration_mm			= 0;
	private final List<String>								dataFilePath						= new ArrayList<String>(initialRecordCapacity);
	private final List<String>								dataChannelNumbers			= new ArrayList<String>(initialRecordCapacity);
	private final List<String>								dataRectifiedObjectKeys	= new ArrayList<String>(initialRecordCapacity);
	private final List<String>								dataRecordsetBaseNames	= new ArrayList<String>(initialRecordCapacity);
	private final List<String>								dataRecordSetOrdinals		= new ArrayList<String>(initialRecordCapacity);
	private final Map<Integer, List<String>>	dataTags								= new HashMap<>();

	public enum DisplayTag {
		FILE_NAME, DIRECTORY_NAME, BASE_PATH, CHANNEL_NUMBER, RECTIFIED_OBJECTKEY, RECORDSET_BASE_NAME;
		
		/**
		 * use this instead of values() to avoid repeatedly cloning actions.
		 */
		public final static DisplayTag						values[]				= values();

		public static DisplayTag fromOrdinal(int ordinal) {
			return DisplayTag.values[ordinal];
		}
	}
	
	public enum DataTag {
		FILE_PATH, CHANNEL_NUMBER, RECTIFIED_OBJECTKEY, RECORDSET_BASE_NAME, RECORDSET_ORDINAL};

	/**
	 * holds trail records for measurements, settlements and scores.
	 * @param useDevice the instance of the device 
	 * @param channelNumber the channel number to be used
	 * @param recordNames
	 */
	public TrailRecordSet(IDevice useDevice, int channelNumber, String[] recordNames) {
		super(useDevice, channelNumber, "Trail", recordNames, -1, true, true); //$NON-NLS-1$
		String deviceSignature = useDevice.getName() + GDE.STRING_UNDER_BAR + channelNumber;
		this.template = new HistoGraphicsTemplate(deviceSignature);
		this.linkedOrdinals = new int[recordNames.length];
		if (this.template != null) this.template.load();

		this.dataTags.put(DataTag.FILE_PATH.ordinal(), this.dataFilePath);
		this.dataTags.put(DataTag.CHANNEL_NUMBER.ordinal(), this.dataChannelNumbers);
		this.dataTags.put(DataTag.RECTIFIED_OBJECTKEY.ordinal(), this.dataRectifiedObjectKeys);
		this.dataTags.put(DataTag.RECORDSET_BASE_NAME.ordinal(), this.dataRecordsetBaseNames);
		this.dataTags.put(DataTag.RECORDSET_ORDINAL.ordinal(), this.dataRecordsetBaseNames);

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
		LinkedHashMap<Integer, SettlementType> channelSettlements = device.getDeviceConfiguration().getChannel(channelConfigNumber).getSettlements();
		LinkedHashMap<Integer, ScoreGroupType> channelScoreGroups = device.getDeviceConfiguration().getChannel(channelConfigNumber).getScoreGroups();

		// display section 0: look for scores at the top - scores' ordinals start after measurements + settlements due to GraphicsTemplate compatibility
		for (int i = 0, myIndex = channelMeasurements.size() + channelSettlements.size(); i < channelScoreGroups.size(); i++) { // myIndex is used as recordOrdinal
			ScoreGroupType scoregroup = channelScoreGroups.get(i);
			PropertyType topPlacementProperty = scoregroup.getProperty("histo_top_placement"); //$NON-NLS-1$
			if (topPlacementProperty != null ? Boolean.valueOf(topPlacementProperty.getValue()) : false) {
				TrailRecord tmpRecord = new TrailRecord(device, myIndex, scoregroup.getName(), scoregroup, newTrailRecordSet, scoregroup.getProperty().size());
				newTrailRecordSet.put(scoregroup.getName(), tmpRecord);
				tmpRecord.setColorDefaultsAndPosition(myIndex);
				if (i == 0) //top score group entry, set color to black
					tmpRecord.setColor(SWTResourceManager.getColor(0, 0, 0));
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "added scoregroup record for " + scoregroup.getName() + " - " + myIndex); //$NON-NLS-1$ //$NON-NLS-2$
			}
			myIndex++;
		}
		{// display section 1: look for settlements at the top - settlements' ordinals start after measurements due to GraphicsTemplate compatibility
			int myIndex = channelMeasurements.size(); // myIndex is used as recordOrdinal
			for (SettlementType settlement : channelSettlements.values()) {
				PropertyType topPlacementProperty = settlement.getProperty("histo_top_placement"); //$NON-NLS-1$
				if (topPlacementProperty != null ? Boolean.valueOf(topPlacementProperty.getValue()) : false) {
					TrailRecord tmpRecord = new TrailRecord(device, myIndex, settlement.getName(), settlement, newTrailRecordSet, initialRecordCapacity);
					newTrailRecordSet.put(settlement.getName(), tmpRecord);
					tmpRecord.setColorDefaultsAndPosition(myIndex);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "added settlement record for " + settlement.getName() + " - " + myIndex); //$NON-NLS-1$ //$NON-NLS-2$
				}
				myIndex++;
			}
		}
		// display section 2: all measurements
		for (int i = 0; i < channelMeasurements.size(); i++) {
			MeasurementType measurement = device.getMeasurement(channelConfigNumber, i);
			TrailRecord tmpRecord = new TrailRecord(device, i, measurement.getName(), measurement, newTrailRecordSet, initialRecordCapacity); // ordinal starts at 0
			newTrailRecordSet.put(measurement.getName(), tmpRecord);
			tmpRecord.setColorDefaultsAndPosition(i);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "added measurement record for " + measurement.getName() + " - " + i); //$NON-NLS-1$ //$NON-NLS-2$
		}
		{// display section 3: take remaining settlements
			int myIndex = channelMeasurements.size(); // myIndex is used as recordOrdinal
			for (SettlementType settlement : channelSettlements.values()) {
				PropertyType topPlacementProperty = settlement.getProperty("histo_top_placement"); //$NON-NLS-1$
				if (!(topPlacementProperty != null ? Boolean.valueOf(topPlacementProperty.getValue()) : false)) {
					TrailRecord tmpRecord = new TrailRecord(device, myIndex, settlement.getName(), settlement, newTrailRecordSet, initialRecordCapacity);
					newTrailRecordSet.put(settlement.getName(), tmpRecord);
					tmpRecord.setColorDefaultsAndPosition(myIndex);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "added settlement record for " + settlement.getName() + " - " + myIndex); //$NON-NLS-1$ //$NON-NLS-2$
				}
				myIndex++; // 
			}
		}
		// display section 4: take remaining scores
		for (int i = 0, myIndex = channelMeasurements.size() + channelSettlements.size(); i < channelScoreGroups.size(); i++) { // myIndex is used as recordOrdinal
			ScoreGroupType scoregroup = channelScoreGroups.get(i);
			PropertyType topPlacementProperty = scoregroup.getProperty("histo_top_placement"); //$NON-NLS-1$
			if (!(topPlacementProperty != null ? Boolean.valueOf(topPlacementProperty.getValue()) : false)) {
				TrailRecord tmpRecord = new TrailRecord(device, myIndex, scoregroup.getName(), scoregroup, newTrailRecordSet, scoregroup.getProperty().size());
				newTrailRecordSet.put(scoregroup.getName(), tmpRecord);
				tmpRecord.setColorDefaultsAndPosition(myIndex);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "added settlement record for " + scoregroup.getName() + " - " + myIndex); //$NON-NLS-1$ //$NON-NLS-2$
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

		this.dataFilePath.clear();
		this.dataChannelNumbers.clear();
		this.dataRectifiedObjectKeys.clear();
		this.dataRecordsetBaseNames.clear();
		this.dataRecordSetOrdinals.clear();
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
					size = ((TrailRecord) get(recordKey)).getTrailRecordSuite()[0].size();
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
	public void setDataTags() {
		for (Map.Entry<Long, List<HistoVault>> entry : HistoSet.getInstance().entrySet()) {
			for (HistoVault histoVault : entry.getValue()) {
				this.dataFilePath.add(histoVault.getLogFilePath().intern());
				this.dataChannelNumbers.add(String.valueOf(histoVault.getLogChannelNumber()).intern());
				this.dataRectifiedObjectKeys.add(histoVault.getRectifiedObjectKey().intern());
				this.dataRecordsetBaseNames.add(histoVault.getLogRecordsetBaseName().intern());
				this.dataRecordSetOrdinals.add(String.valueOf(histoVault.getLogRecordSetOrdinal()).intern());
			}
		}
	}

	/**
	 * @param timestamp_ms is a time stamp from the timestep range
	 * @return the position of the timestep which is smaller or equal to the timestamp or -1 if not found 
	 */
	public int getIndex(long timestamp_ms) {
		for (int i = 0; i < this.timeStep_ms.size(); i++) {
			long tmpTimestep_ms = this.timeStep_ms.get(i) / 10;
			if (tmpTimestep_ms <= timestamp_ms) return i;
		}
		return -1;
	}

	/**
	 * @return the dataTags
	 */
	public Map<Integer, String> getDataTags(long timestamp_ms) {
		int index = getIndex(timestamp_ms);
		if (index >= 0) {
			HashMap<Integer, String> dataTags4Index = new HashMap<Integer, String>();
			for (java.util.Map.Entry<Integer, List<String>> logTagEntry : this.dataTags.entrySet()) {
				dataTags4Index.put(logTagEntry.getKey(), logTagEntry.getValue().get(index));
			}
			return dataTags4Index;
		}
		else
			return new HashMap<Integer, String>();
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
				// recordSet.setTimeGridLineStyle(Integer.valueOf(this.template.getProperty(RecordSet.TIME_GRID_LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_DOT)).intValue());
				// recordSet.setTimeGridType(Integer.valueOf(this.template.getProperty(RecordSet.TIME_GRID_TYPE, "0")).intValue()); //$NON-NLS-1$
				if (!isHorizontalGridOrdinalSet && record.isVisible) { // set curve grid to the first visible record
					color = this.template.getProperty(RecordSet.HORIZONTAL_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
					r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
					g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
					b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
					this.setHorizontalGridColor(SWTResourceManager.getColor(r, g, b));
					this.setHorizontalGridLineStyle(Integer.valueOf(this.template.getProperty(RecordSet.HORIZONTAL_GRID_LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_DOT)).intValue());
					this.setHorizontalGridType(Integer.valueOf(this.template.getProperty(RecordSet.HORIZONTAL_GRID_TYPE, "0")).intValue()); //$NON-NLS-1$
					this.setHorizontalGridRecordOrdinal(record.ordinal); // initial use top score trail record
					isHorizontalGridOrdinalSet = true;
				}
			}
			this.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "applied histo graphics template file " + this.template.getCurrentFilePath()); //$NON-NLS-1$
			if (doUpdateVisibilityStatus) {
				updateVisibleAndDisplayableRecordsForTable();
			}
			this.application.updateHistoGraphicsWindow(true);
		}

	}

	//	/**
	//	 * check and update visibility status of all records according to the available histo data.
	//	 * at least an update of the graphics window should be included at the end of this method.
	//	 */
	//	public void updateVisibilityStatus(boolean includeReasonableDataCheck) {
	//		int displayableCounter = 0;
	//		for (int i = 0; i < this.size(); ++i) {
	//			Record record = this.get(i);
	//			if (includeReasonableDataCheck) {
	//				// todo record.setDisplayable(record.isActive() || record.hasReasonableData()); // was initially: (record.hasReasonableData());
	//				//	if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.getName() + " hasReasonableData " + record.hasReasonableData()); //$NON-NLS-1$
	//			}
	//
	//			if (record.isActive() && record.isDisplayable()) {
	//				++displayableCounter;
	//				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
	//			}
	//		}
	//		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
	//		this.setConfiguredDisplayable(displayableCounter);
	//	}

	public HistoGraphicsTemplate getTemplate() {
		return template;
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
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "add syncMaster " + this.get(syncMasterRecordOrdinal).name); //$NON-NLS-1$
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
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "add " + tmpRecord.name); //$NON-NLS-1$
					}
				}
			}
		}
		if (log.isLoggable(Level.FINE)) {
			StringBuilder sb = new StringBuilder();
			for (Integer syncRecordOrdinal : this.scaleSyncedRecords.keySet()) {
				sb.append("\n").append(syncRecordOrdinal).append(GDE.STRING_COLON); //$NON-NLS-1$
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
	 * @return all measurement records and settlement / score records based on display settings 
	 */
	@Override // reasons: 1. Harmonize display records collections  2. The data vector of trail records holding a record suite is empty -> (TrailRecord) record).getTrailRecordSuite().length > 1
	public Record[] getRecordsSortedForDisplay() {
		Vector<Record> displayRecords = new Vector<Record>();
		// add the record with horizontal grid
		for (Record record : this.getDisplayRecords()) {
			if (record.ordinal == this.horizontalGridRecordOrdinal) displayRecords.add(record);
		}
		// add the scaleSyncMaster records to draw scale of this records first which sets the min/max display values
		for (Record record : this.getDisplayRecords()) {
			if (record.ordinal != this.horizontalGridRecordOrdinal && record.isScaleSyncMaster()) displayRecords.add(record);
		}
		// add all others
		for (Record record : this.getDisplayRecords()) {
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
	 * update the collection of visible and displayable records in this record set.
	 * the sort order conforms to the record insertion order.
	 */
	@Override // reason is display sequence independent from record names sequence (record ordinal)
	public void updateVisibleAndDisplayableRecordsForTable() {
		this.visibleAndDisplayableRecords.removeAllElements();
		this.allRecords.removeAllElements();
		// get by insertion order
		for (Map.Entry<String, Record> entry : this.entrySet()) {
			final TrailRecord record = (TrailRecord) entry.getValue();
			if (record.isMeasurement() || (record.isSettlement() && this.settings.isDisplaySettlements()) || (record.isScoregroup() && this.settings.isDisplayScores())) {
				record.setDisplayable(record.isActive() && record.hasReasonableData());
				if (record.isVisible && record.isDisplayable) //only selected records get displayed
					this.visibleAndDisplayableRecords.add(record);
				if (record.isDisplayable) // only records with reasonable data get displayed
					this.allRecords.add(record);
			}
		}
	}

	/**
	 * get all tags for all recordsets / vaults.
	 * @param displayTag
	 * @return empty record name and display tag description as a trail text replacement followed by the tag values
	 */
	public String[] getTableTagRow(DisplayTag displayTag) {
		String[] dataTableRow = new String[this.timeStep_ms.size() + 2];

		if (!this.timeStep_ms.isEmpty()) {
			if (displayTag == DisplayTag.FILE_NAME)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0838);
			else if (displayTag == DisplayTag.DIRECTORY_NAME)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0839);
			else if (displayTag == DisplayTag.BASE_PATH)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0840);
			else if (displayTag == DisplayTag.CHANNEL_NUMBER)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0841);
			else if (displayTag == DisplayTag.RECTIFIED_OBJECTKEY)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0842);
			else if (displayTag == DisplayTag.RECORDSET_BASE_NAME)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0844);
			else
				throw new UnsupportedOperationException();

			List<String> logTag = this.dataTags.get(displayTag);
			if (this.settings.isXAxisReversed()) {
				if (displayTag == DisplayTag.FILE_NAME)
					for (int i = 0; i < this.timeStep_ms.size(); i++)
						dataTableRow[i + 2] = Paths.get(this.dataTags.get(DataTag.FILE_PATH.ordinal()).get(i)).getFileName().toString();
				else if (displayTag == DisplayTag.DIRECTORY_NAME)
					for (int i = 0; i < this.timeStep_ms.size(); i++)
						dataTableRow[i + 2] = Paths.get(this.dataTags.get(DataTag.FILE_PATH.ordinal()).get(i)).getParent().getFileName().toString();
				else if (displayTag == DisplayTag.BASE_PATH)
					for (int i = 0; i < this.timeStep_ms.size(); i++)
						dataTableRow[i + 2] = Paths.get(this.dataTags.get(DataTag.FILE_PATH.ordinal()).get(i)).getParent().getParent().getFileName().toString();
				else if (displayTag == DisplayTag.CHANNEL_NUMBER)
					for (int i = 0; i < this.timeStep_ms.size(); i++)
						dataTableRow[i + 2] = this.dataTags.get(DataTag.CHANNEL_NUMBER.ordinal()).get(i);
				else if (displayTag == DisplayTag.RECTIFIED_OBJECTKEY)
					for (int i = 0; i < this.timeStep_ms.size(); i++)
						dataTableRow[i + 2] = this.dataTags.get(DataTag.RECTIFIED_OBJECTKEY.ordinal()).get(i);
				else if (displayTag == DisplayTag.RECORDSET_BASE_NAME)
					for (int i = 0; i < this.timeStep_ms.size(); i++)
						dataTableRow[i + 2] = this.dataTags.get(DataTag.RECORDSET_BASE_NAME.ordinal()).get(i);
				else
					dataTableRow = null; // for test only
			}
			else {
				if (displayTag == DisplayTag.FILE_NAME)
					for (int i = 0, j = this.timeStep_ms.size() - 1; i < this.timeStep_ms.size(); i++, j--)
						dataTableRow[i + 2] = Paths.get(this.dataTags.get(DataTag.FILE_PATH.ordinal()).get(j)).getFileName().toString();
				else if (displayTag == DisplayTag.DIRECTORY_NAME)
					for (int i = 0, j = this.timeStep_ms.size() - 1; i < this.timeStep_ms.size(); i++, j--)
						dataTableRow[i + 2] = Paths.get(this.dataTags.get(DataTag.FILE_PATH.ordinal()).get(j)).getParent().getFileName().toString();
				else if (displayTag == DisplayTag.BASE_PATH)
					for (int i = 0, j = this.timeStep_ms.size() - 1; i < this.timeStep_ms.size(); i++, j--)
						dataTableRow[i + 2] = Paths.get(this.dataTags.get(DataTag.FILE_PATH.ordinal()).get(j)).getParent().getParent().getFileName().toString();
				else if (displayTag == DisplayTag.CHANNEL_NUMBER)
					for (int i = 0, j = this.timeStep_ms.size() - 1; i < this.timeStep_ms.size(); i++, j--)
						dataTableRow[i + 2] = this.dataTags.get(DataTag.CHANNEL_NUMBER.ordinal()).get(j);
				else if (displayTag == DisplayTag.RECTIFIED_OBJECTKEY)
					for (int i = 0, j = this.timeStep_ms.size() - 1; i < this.timeStep_ms.size(); i++, j--)
						dataTableRow[i + 2] = this.dataTags.get(DataTag.RECTIFIED_OBJECTKEY.ordinal()).get(j);
				else if (displayTag == DisplayTag.RECORDSET_BASE_NAME)
					for (int i = 0, j = this.timeStep_ms.size() - 1; i < this.timeStep_ms.size(); i++, j--)
						dataTableRow[i + 2] = this.dataTags.get(DataTag.RECORDSET_BASE_NAME.ordinal()).get(j);
				else
					dataTableRow = null; // for test only
			}
		}
		return dataTableRow;
	}

	/**
	 * @return the dataTags
	 */
	public Map<Integer, List<String>> getDataTags() {
		return this.dataTags;
	}

	/**
	 * @return the column headers starting with the first data column 
	 */
	public String[] getTableHeaderRow() {
		String[] headerRow = new String[this.timeStep_ms.size()];
		if (this.settings.isXAxisReversed()) {
			for (int i = 0; i < this.timeStep_ms.size(); i++) {
				StringBuilder sb = new StringBuilder();
				sb.append(LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmmss, this.timeStep_ms.getTime_ms(i)));
				headerRow[i] = sb.toString();
			}
		}
		else {
			for (int i = 0, j = this.timeStep_ms.size() - 1; i < this.timeStep_ms.size(); i++, j--) {
				StringBuilder sb = new StringBuilder();
				sb.append(LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmmss, this.timeStep_ms.getTime_ms(i)));
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