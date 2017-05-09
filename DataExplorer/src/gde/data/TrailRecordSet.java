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
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
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
import gde.device.TrailTypes;
import gde.exception.DataInconsitsentException;
import gde.histocache.HistoVault;
import gde.histoinventory.GeoCodes;
import gde.histoinventory.GpsCluster;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;
import gde.utils.GpsCoordinate;
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
	private final List<String>								dataGpsLocations				= new ArrayList<String>(initialRecordCapacity);
	private final Map<DataTag, List<String>>	dataTags								= new HashMap<>();

	public enum DisplayTag {
		FILE_NAME, DIRECTORY_NAME, BASE_PATH, CHANNEL_NUMBER, RECTIFIED_OBJECTKEY, RECORDSET_BASE_NAME, GPS_LOCATION;

		/**
		 * use this instead of values() to avoid repeatedly cloning actions.
		 */
		public final static DisplayTag values[] = values();

		public static DisplayTag fromOrdinal(int ordinal) {
			return DisplayTag.values[ordinal];
		}
	}

	public enum DataTag {
		FILE_PATH, CHANNEL_NUMBER, RECTIFIED_OBJECTKEY, RECORDSET_BASE_NAME, RECORDSET_ORDINAL, GPS_LOCATION
	};

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

		this.dataTags.put(DataTag.FILE_PATH, this.dataFilePath);
		this.dataTags.put(DataTag.CHANNEL_NUMBER, this.dataChannelNumbers);
		this.dataTags.put(DataTag.RECTIFIED_OBJECTKEY, this.dataRectifiedObjectKeys);
		this.dataTags.put(DataTag.RECORDSET_BASE_NAME, this.dataRecordsetBaseNames);
		this.dataTags.put(DataTag.RECORDSET_ORDINAL, this.dataRecordsetBaseNames);
		this.dataTags.put(DataTag.GPS_LOCATION, this.dataGpsLocations);

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
	public static synchronized TrailRecordSet createRecordSet(IDevice device, int channelConfigNumber) {
		String[] names = device.getDeviceConfiguration().getMeasurementSettlementScoregroupNames(channelConfigNumber);
		TrailRecordSet newTrailRecordSet = new TrailRecordSet(device, channelConfigNumber, names);
		printRecordNames("createRecordSet() " + newTrailRecordSet.name + " - ", newTrailRecordSet.getRecordNames()); //$NON-NLS-1$ //$NON-NLS-2$
		newTrailRecordSet.timeStep_ms = new TimeSteps(-1, initialRecordCapacity);
		List<MeasurementType> channelMeasurements = device.getDeviceConfiguration().getChannelMeasuremts(channelConfigNumber);
		LinkedHashMap<Integer, SettlementType> channelSettlements = device.getDeviceConfiguration().getChannel(channelConfigNumber).getSettlements();
		LinkedHashMap<Integer, ScoreGroupType> channelScoreGroups = device.getDeviceConfiguration().getChannel(channelConfigNumber).getScoreGroups();

		{// display section 0: look for scores at the top - scores' ordinals start after measurements + settlements due to GraphicsTemplate compatibility
			int myIndex = channelMeasurements.size() + channelSettlements.size();
			for (ScoreGroupType scoreGroup : channelScoreGroups.values()) {
				PropertyType topPlacementProperty = scoreGroup.getProperty("histo_top_placement"); //$NON-NLS-1$
				if (topPlacementProperty != null ? Boolean.valueOf(topPlacementProperty.getValue()) : false) {
					TrailRecord tmpRecord = new TrailRecord(device, myIndex, scoreGroup.getName(), scoreGroup, newTrailRecordSet, scoreGroup.getProperty().size());
					newTrailRecordSet.put(scoreGroup.getName(), tmpRecord);
					tmpRecord.setColorDefaultsAndPosition(myIndex);
					if (newTrailRecordSet.size() == 1) tmpRecord.setColor(SWTResourceManager.getColor(0, 0, 0)); //top score group entry, set color to black
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "added scoregroup record for " + scoreGroup.getName() + " - " + myIndex); //$NON-NLS-1$ //$NON-NLS-2$
				}
				myIndex++;
			}
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
		{// display section 2: all measurements
			for (int i = 0; i < channelMeasurements.size(); i++) {
				MeasurementType measurement = device.getMeasurement(channelConfigNumber, i);
				TrailRecord tmpRecord = new TrailRecord(device, i, measurement.getName(), measurement, newTrailRecordSet, initialRecordCapacity); // ordinal starts at 0
				newTrailRecordSet.put(measurement.getName(), tmpRecord);
				tmpRecord.setColorDefaultsAndPosition(i);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "added measurement record for " + measurement.getName() + " - " + i); //$NON-NLS-1$ //$NON-NLS-2$
			}
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
		{// display section 4: take remaining scores
			int myIndex = channelMeasurements.size() + channelSettlements.size();
			for (ScoreGroupType scoreGroup : channelScoreGroups.values()) {
				PropertyType topPlacementProperty = scoreGroup.getProperty("histo_top_placement"); //$NON-NLS-1$
				if (!(topPlacementProperty != null ? Boolean.valueOf(topPlacementProperty.getValue()) : false)) {
					TrailRecord tmpRecord = new TrailRecord(device, myIndex, scoreGroup.getName(), scoreGroup, newTrailRecordSet, scoreGroup.getProperty().size());
					newTrailRecordSet.put(scoreGroup.getName(), tmpRecord);
					tmpRecord.setColorDefaultsAndPosition(myIndex);
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "added scoregroup record for " + scoreGroup.getName() + " - " + myIndex); //$NON-NLS-1$ //$NON-NLS-2$
				}
				myIndex++; //
			}
		}
		newTrailRecordSet.syncScaleOfSyncableRecords();
		// build display sequence index array
		List<String> recordNameList = Arrays.asList(newTrailRecordSet.getRecordNames());
		Iterator<java.util.Map.Entry<String, Record>> iterator = newTrailRecordSet.entrySet().iterator();
		for (int i = 0; i < newTrailRecordSet.size(); i++) {
			newTrailRecordSet.linkedOrdinals[i] = recordNameList.indexOf(iterator.next().getKey());
		}

		// setting all data in this create procedure and the synchronized keyword makes this method thread safe
		newTrailRecordSet.defineTrailTypes();
		newTrailRecordSet.addHistoVaults();
		newTrailRecordSet.setGpsLocationsTags();
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
	 * rebuild data contents except building the records list and the synchronized scales data
	 */
	public synchronized void refillRecordSet() {
		this.cleanup();
		this.addHistoVaults();
		this.setGpsLocationsTags();
		this.syncScaleOfSyncableRecords();
	}

	/**
	 * clears the data points in all records and in the time steps.
	 * keeps initial capacities.
	 * does not clear any fields in the recordSet, the records or in timeStep.
	 */
	@Override
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
		this.dataGpsLocations.clear();
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
	 * set time steps for the trail recordset and the data points for all displayable trail records.
	 * every record takes the selected trail type / score data from the history vault and populates its data.
	 */
	private void addHistoVaults() {
		for (Map.Entry<Long, List<HistoVault>> entry : HistoSet.getInstance().entrySet()) {
			for (HistoVault histoVault : entry.getValue()) {
				{ // add values
					int duration_mm = histoVault.getScorePoint(ScoreLabelTypes.DURATION_MM.ordinal());
					this.durations_mm.add(duration_mm);
					this.averageDuration_mm += (duration_mm - this.averageDuration_mm) / this.durations_mm.size();
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("recordSet  startTimeStamp %,d  -- entry.key %,d", histoVault.getLogStartTimestamp_ms(), entry.getKey())); //$NON-NLS-1$
					this.timeStep_ms.addRaw(histoVault.getLogStartTimestamp_ms() * 10);
					for (String recordName : this.getRecordNames()) {
						((TrailRecord) this.get(recordName)).add(histoVault);
					}
				}
				{ // add data Tags
					this.dataFilePath.add(histoVault.getLogFilePath().intern());
					this.dataChannelNumbers.add(String.valueOf(histoVault.getLogChannelNumber()).intern());
					this.dataRectifiedObjectKeys.add(histoVault.getRectifiedObjectKey().intern());
					this.dataRecordsetBaseNames.add(histoVault.getLogRecordsetBaseName().intern());
					this.dataRecordSetOrdinals.add(String.valueOf(histoVault.getLogRecordSetOrdinal()).intern());
				}
			}
		}
	}

	/**
	 * set the data points for one single trail record.
	 * the record takes the selected trail type / score data from the trail record vault and populates its data.
	 * @param recordOrdinal
	 */
	public synchronized void setPoints(int recordOrdinal) {
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
	 * adds GPS location tagging information based on the latitude / longitude median.
	 * supports asynchronous geocode fetches from the internet.
	 */
	private void setGpsLocationsTags() {
		// locate the GPS coordinates records
		TrailRecord latitudeRecord = null, longitudeRecord = null;
		for (int i = 0; i < this.getRecordNames().length; i++) { // todo fill HistoVault's DataType and access via DataType without hard coded measurement names
			if (this.getRecordNames()[i].toLowerCase().contains("latitud")) //$NON-NLS-1$
				latitudeRecord = (TrailRecord) this.get(this.getRecordNames()[i]);
			else if (this.getRecordNames()[i].toLowerCase().contains("longitud")) //$NON-NLS-1$
				longitudeRecord = (TrailRecord) this.get(this.getRecordNames()[i]);
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
						gpsCluster.add(new GpsCoordinate(this.device.translateValue(latitudeRecord, latitudePoint / 1000.), this.device.translateValue(longitudeRecord, longitudePoint / 1000.)));
					else
						gpsCluster.add(null); // this keeps the sequence in parallel with the vaults sequence
				}
			}
			// populate the GPS locations list for subsequently filling the histo table
			if (gpsCluster.parallelStream().filter(Objects::nonNull).count() > 0) {
				Thread gpsLocationsThread = new Thread((Runnable) () -> setGpsLocationTags(gpsCluster, this.dataGpsLocations), "setGpsLocationTags"); //$NON-NLS-1$
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
	 * populate the GPS locations list if there are any GPS locations in this recordset.
	 * trigger refilling the histo table.
	 * @param gpsCluster holds the GPS coordinates and the assignment to clusters; null coordinates are allowed
	 * @param dataGpsLocations is an empty list or GPS location strings for all vaults in the correct sequence
	 */
	private synchronized void setGpsLocationTags(GpsCluster gpsCluster, List<String> dataGpsLocations) {
		long nanoTime = System.nanoTime();
		gpsCluster.setClusters();
		if (gpsCluster.size() > 0) {
			List<String> tmpGpsLocations = new ArrayList<String>();
			for (GpsCoordinate gpsCoordinate : gpsCluster) {
				// preserve the correct vaults sequence
				if (gpsCoordinate != null)
					tmpGpsLocations.add(GeoCodes.getLocation(gpsCluster.getAssignedClusters().get(gpsCoordinate).getCenter()));
				else
					tmpGpsLocations.add(GDE.STRING_EMPTY);
			}
			// fill the data tags only if there is at least one GPS coordinate
			if (tmpGpsLocations.parallelStream().filter(s -> !s.isEmpty()).count() > 0) dataGpsLocations.addAll(tmpGpsLocations);
			// refresh the histo table which might already have been painted without the GPS coordinates
			if (dataGpsLocations.size() > 0) {
				this.application.updateHistoTable(false);
				if (log.isLoggable(Level.FINER))
					log.log(Level.FINER, "fill in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + " ms!  GPS locations size=" + gpsCluster.getAssignedClusters().values().size()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * @param timestamp_ms
	 * @return the position of the timestep which is the closest to the timestamp
	 */
	public int getIndex(long timestamp_ms) {
		return this.timeStep_ms.getBestIndex(timestamp_ms, Comparator.reverseOrder());
	}

	/**
	 * @param index
	 * @return the dataTags
	 */
	public Map<DataTag, String> getDataTags(int index) {
		if (index >= 0) {
			HashMap<DataTag, String> dataTags4Index = new HashMap<>();
			for (java.util.Map.Entry<DataTag, List<String>> logTagEntry : this.dataTags.entrySet()) {
				if (logTagEntry.getValue().size() > 0) dataTags4Index.put(logTagEntry.getKey(), logTagEntry.getValue().get(index));
			}
			return dataTags4Index;
		}
		else
			return new HashMap<DataTag, String>();
	}

	/**
	 * inform displayable trail records about the trail types which are allowed, set trail selection list and current trailType / score.
	 */
	public void defineTrailTypes() {
		String[] trailRecordNames = this.getRecordNames();
		for (String trailRecordName : trailRecordNames) {
			TrailRecord trailRecord = ((TrailRecord) this.get(trailRecordName));
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
		return this.template;
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
				sb.append(GDE.STRING_NEW_LINE).append(syncRecordOrdinal).append(GDE.STRING_COLON);
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
	 * @return the record of the 0-based user sequence number
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
			if (record.isMeasurement() || (record.isSettlement() && this.settings.isDisplaySettlements()) || (record.isScoreGroup() && this.settings.isDisplayScores())) {
				record.setDisplayable(record.isActive() && record.hasReasonableData());
				if (record.isVisible && record.isDisplayable) //only selected records get displayed
					this.visibleAndDisplayableRecords.add(record);
				if (record.isDisplayable) // only records with reasonable data get displayed
					this.allRecords.add(record);
			}
		}
	}

	/**
	 * @return the tags which have been filled
	 */
	public EnumSet<DisplayTag> getActiveDisplayTags() {
		EnumSet<DisplayTag> activeDisplayTags = EnumSet.allOf(DisplayTag.class);
		if (this.dataTags.get(DataTag.GPS_LOCATION).size() != this.dataTags.get(DataTag.RECORDSET_BASE_NAME).size()) activeDisplayTags.remove(DisplayTag.GPS_LOCATION);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "activeDisplayTags.size()=", activeDisplayTags.size()); //$NON-NLS-1$
		return activeDisplayTags;
	}

	/**
	 * get all tags for all recordsets / vaults.
	 * please note that the GPS location tags are filled in asynchronously.
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
			else if (displayTag == DisplayTag.GPS_LOCATION) {
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0845);
			}
			else
				throw new UnsupportedOperationException();

			if (this.settings.isXAxisReversed()) {
				if (displayTag == DisplayTag.FILE_NAME)
					for (int i = 0; i < this.timeStep_ms.size(); i++)
						dataTableRow[i + 2] = Paths.get(this.dataTags.get(DataTag.FILE_PATH).get(i)).getFileName().toString();
				else if (displayTag == DisplayTag.DIRECTORY_NAME)
					for (int i = 0; i < this.timeStep_ms.size(); i++)
						dataTableRow[i + 2] = Paths.get(this.dataTags.get(DataTag.FILE_PATH).get(i)).getParent().getFileName().toString();
				else if (displayTag == DisplayTag.BASE_PATH)
					for (int i = 0; i < this.timeStep_ms.size(); i++)
						dataTableRow[i + 2] = Paths.get(this.dataTags.get(DataTag.FILE_PATH).get(i)).getParent().toString();
				else if (displayTag == DisplayTag.CHANNEL_NUMBER)
					for (int i = 0; i < this.timeStep_ms.size(); i++)
						dataTableRow[i + 2] = this.dataTags.get(DataTag.CHANNEL_NUMBER).get(i);
				else if (displayTag == DisplayTag.RECTIFIED_OBJECTKEY)
					for (int i = 0; i < this.timeStep_ms.size(); i++)
						dataTableRow[i + 2] = this.dataTags.get(DataTag.RECTIFIED_OBJECTKEY).get(i);
				else if (displayTag == DisplayTag.RECORDSET_BASE_NAME)
					for (int i = 0; i < this.timeStep_ms.size(); i++)
						dataTableRow[i + 2] = this.dataTags.get(DataTag.RECORDSET_BASE_NAME).get(i);
				else if (displayTag == DisplayTag.GPS_LOCATION) {
					for (int i = 0; i < this.timeStep_ms.size(); i++)
						dataTableRow[i + 2] = this.dataTags.get(DataTag.GPS_LOCATION).get(i);
				}
				else
					dataTableRow = null; // for test only
			}
			else {
				if (displayTag == DisplayTag.FILE_NAME)
					for (int i = 0, j = this.timeStep_ms.size() - 1; i < this.timeStep_ms.size(); i++, j--)
						dataTableRow[i + 2] = Paths.get(this.dataTags.get(DataTag.FILE_PATH).get(j)).getFileName().toString();
				else if (displayTag == DisplayTag.DIRECTORY_NAME)
					for (int i = 0, j = this.timeStep_ms.size() - 1; i < this.timeStep_ms.size(); i++, j--)
						dataTableRow[i + 2] = Paths.get(this.dataTags.get(DataTag.FILE_PATH).get(j)).getParent().getFileName().toString();
				else if (displayTag == DisplayTag.BASE_PATH)
					for (int i = 0, j = this.timeStep_ms.size() - 1; i < this.timeStep_ms.size(); i++, j--)
						dataTableRow[i + 2] = Paths.get(this.dataTags.get(DataTag.FILE_PATH).get(j)).getParent().toString();
				else if (displayTag == DisplayTag.CHANNEL_NUMBER)
					for (int i = 0, j = this.timeStep_ms.size() - 1; i < this.timeStep_ms.size(); i++, j--)
						dataTableRow[i + 2] = this.dataTags.get(DataTag.CHANNEL_NUMBER).get(j);
				else if (displayTag == DisplayTag.RECTIFIED_OBJECTKEY)
					for (int i = 0, j = this.timeStep_ms.size() - 1; i < this.timeStep_ms.size(); i++, j--)
						dataTableRow[i + 2] = this.dataTags.get(DataTag.RECTIFIED_OBJECTKEY).get(j);
				else if (displayTag == DisplayTag.RECORDSET_BASE_NAME)
					for (int i = 0, j = this.timeStep_ms.size() - 1; i < this.timeStep_ms.size(); i++, j--)
						dataTableRow[i + 2] = this.dataTags.get(DataTag.RECORDSET_BASE_NAME).get(j);
				else if (displayTag == DisplayTag.GPS_LOCATION) {
					for (int i = 0, j = this.timeStep_ms.size() - 1; i < this.timeStep_ms.size(); i++, j--)
						dataTableRow[i + 2] = this.dataTags.get(DataTag.GPS_LOCATION).get(j);
				}
				else
					dataTableRow = null; // for test only
			}
		}
		return dataTableRow;
	}

	/**
	 * @return the dataTags
	 */
	public Map<DataTag, List<String>> getDataTags() {
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

	public long getDisplayTimeStamp_ms(int index) {
		if (this.settings.isXAxisReversed()) {
			return this.timeStep_ms.get(index) / 10;
		}
		else {
			return this.timeStep_ms.get(this.timeStep_ms.size() - 1 - index) / 10;
		}
	}

}