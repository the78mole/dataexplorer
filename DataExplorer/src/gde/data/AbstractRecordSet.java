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

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;

import gde.GDE;
import gde.config.Settings;
import gde.device.IDevice;
import gde.device.resource.DeviceXmlResource;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

/**
 * Hold records for a channel of a device and the time steps.
 * Support synchronizing the y axis scales.
 * @author Thomas Eickert (USER)
 */
public abstract class AbstractRecordSet extends LinkedHashMap<String, Record> {
	protected final static String								$CLASS_NAME											= AbstractRecordSet.class.getName();
	protected final static long									serialVersionUID								= 26031957;
	protected final static Logger								log															= Logger.getLogger($CLASS_NAME);

	public static final int											MAX_NAME_LENGTH									= 40;

	protected static final int									initialRecordCapacity						= 55;
	protected static final String								DESCRIPTION_TEXT_LEAD						= Messages.getString(MessageIds.GDE_MSGT0129);
	protected static final DeviceXmlResource		xmlResource											= DeviceXmlResource.getInstance();

	protected final DataExplorer								application;																																	// pointer to main application
	protected final Channels										channels;																																			// start point of data hierarchy
	protected final IDevice											device;
	protected final Settings										settings												= Settings.getInstance();
	protected final Channel											parent;

	protected String														name;																																					//1)Flugaufzeichnung, 2)Laden, 3)Entladen, ..
	protected TimeSteps													timeStep_ms;

	protected String														header													= null;
	protected String[]													recordNames;																																	//Spannung, Strom, ..
	protected String														description											= GDE.STRING_EMPTY;

	protected boolean														hasDisplayableData							= false;
	protected Rectangle													drawAreaBounds;																																// draw area in display pixel

	//display in data table
	protected Vector<Record>										visibleAndDisplayableRecords		= new Vector<Record>();												//collection of records visible and displayable
	protected Vector<Record>										allRecords											= new Vector<Record>();												//collection of all records
	// sync enabled records
	protected HashMap<Integer, Vector<Record>>	scaleSyncedRecords							= new HashMap<Integer, Vector<Record>>(2);		//collection of record keys where scales might be synchronized

	// measurement
	protected String														recordKeyMeasurement						= GDE.STRING_EMPTY;

	protected double														maxValue												= Integer.MIN_VALUE;
	protected double														minValue												= Integer.MAX_VALUE;													//min max value

	public static final String									TIME_STEP_MS										= "timeStep_ms";															//$NON-NLS-1$
	public static final String									START_TIME_STAMP								= "startTimeStamp";														//$NON-NLS-1$
	protected static final String								TIME														= "time";																			//$NON-NLS-1$
	protected static final String								TIME_GRID_TYPE									= "RecordSet_timeGridType";										//$NON-NLS-1$
	protected static final String								TIME_GRID_COLOR									= "RecordSet_timeGridColor";									//$NON-NLS-1$
	protected static final String								TIME_GRID_LINE_STYLE						= "RecordSet_timeGridLineStyle";							//$NON-NLS-1$
	public static final int											TIME_GRID_NONE									= 0;																					// no time grid
	public static final int											TIME_GRID_MAIN									= 1;																					// each main tickmark
	public static final int											TIME_GRID_MOD60									= 2;																					// each mod60 tickmark
	protected int																timeGridType										= TIME_GRID_NONE;
	protected Vector<Integer>										timeGrid												= new Vector<Integer>();											// contains the time grid position, updated from TimeLine.drawTickMarks
	protected Color															timeGridColor										= DataExplorer.COLOR_GREY;
	protected int																timeGridLineStyle								= SWT.LINE_DOT;

	@Deprecated
	protected static final String								HORIZONTAL_GRID_RECORD					= "RecordSet_horizontalGridRecord";						//$NON-NLS-1$
	protected static final String								HORIZONTAL_GRID_RECORD_ORDINAL	= "RecordSet_horizontalGridRecordOrdinal";		//$NON-NLS-1$
	protected static final String								HORIZONTAL_GRID_TYPE						= "RecordSet_horizontalGridType";							//$NON-NLS-1$
	protected static final String								HORIZONTAL_GRID_COLOR						= "RecordSet_horizontalGridColor";						//$NON-NLS-1$
	protected static final String								HORIZONTAL_GRID_LINE_STYLE			= "RecordSet_horizontalGridLineStyle";				//$NON-NLS-1$
	public static final int											HORIZONTAL_GRID_NONE						= 0;																					// no time grid
	public static final int											HORIZONTAL_GRID_EVERY						= 1;																					// each main tickmark
	public static final int											HORIZONTAL_GRID_SECOND					= 2;																					// each main tickmark
	protected int																horizontalGridType							= HORIZONTAL_GRID_NONE;
	protected Vector<Integer>										horizontalGrid									= new Vector<Integer>();											// contains the time grid position, updated from TimeLine.drawTickMarks
	protected Color															horizontalGridColor							= DataExplorer.COLOR_GREY;
	protected int																horizontalGridLineStyle					= SWT.LINE_DASH;
	protected int																horizontalGridRecordOrdinal			= -1;																					// recordNames[horizontalGridRecord]

	/**
	 * Special record set data buffers according the size of given names array, where the name is the key to access the data buffer used to hold compare able records (compare set).
	 * @param application
	 * @param useDevice the device
	 * @param newName for the records like "1) Laden"
	 * @param recordNames
	 */
	protected AbstractRecordSet(DataExplorer application, IDevice useDevice, String newName, String[] recordNames) {
		super();
		this.application = application;
		this.channels = null;
		this.device = useDevice;

		this.parent = null;
		this.name = newName;
		this.recordNames = recordNames;
	}

	/**
	 * Record set data buffers according the size of given names array, where the name is the key to access the data buffer.
	 * @param useDevice
	 * @param channelNumber the channel number to be used
	 * @param newName for the records like "1) Laden"
	 * @param measurementNames array of the device supported measurement names
	 * @param newTimeStep_ms time in msec of device measures points
	 */
	protected AbstractRecordSet(IDevice useDevice, int channelNumber, String newName, String[] measurementNames, double newTimeStep_ms) {
		super(measurementNames.length);
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.device = useDevice;

		this.parent = this.channels.get(channelNumber);
		this.name = newName.length() <= RecordSet.MAX_NAME_LENGTH ? newName : newName.substring(0, 30);

		this.recordNames = measurementNames.clone();
		//this.timeStep_ms = new TimeSteps(this.get(0), newTimeStep_ms);
		this.description = (this.device != null ? this.device.getName() + GDE.STRING_MESSAGE_CONCAT : GDE.STRING_EMPTY) + DESCRIPTION_TEXT_LEAD + StringHelper.getDateAndTime();
	}

	/**
	 * Copy constructor - used to copy a record set to another channel/configuration, where the configuration coming from the device properties file.
	 * @param recordSet
	 * @param channelConfigurationNumber
	 */
	protected AbstractRecordSet(AbstractRecordSet recordSet, int channelConfigurationNumber) {
		super(recordSet);
		this.application = recordSet.application;
		this.channels = recordSet.channels;
		this.device = recordSet.device;

		this.parent = this.channels.get(channelConfigurationNumber);
		this.name = recordSet.name;

	}

	/**
	 * Copy constructor - used to copy a record set during graphics cut mode.
	 * @param recordSet
	 * @param recordNames
	 */
	protected AbstractRecordSet(AbstractRecordSet recordSet, String[] recordNames) {
		super(recordSet);
		this.application = recordSet.application;
		this.channels = recordSet.channels;
		this.device = recordSet.device;

		this.parent = recordSet.parent;
		this.name = recordSet.name.length() < MAX_NAME_LENGTH ? recordSet.name + GDE.STRING_UNDER_BAR : recordSet.name.substring(0, MAX_NAME_LENGTH - 1) + GDE.STRING_UNDER_BAR;

		this.recordNames = recordNames;
	}

	/**
	 * Method to get the sorted record names as array, use it for logging or debugging purpose only.
	 * Sorted according list in the device configuration (XML) file.
	 * @return the record names
	 */
	public String[] getRecordNames() {
		return this.recordNames.clone();
	}

	/**
	 * @param recordNameKey
	 * @return a specific data vector selected by given key data name
	 */
	public Record getRecord(String recordNameKey) {
		return this.get(recordNameKey);
	}

	/**
	 * Update the collection of visible and displayable records in this record set for table view
	 */
	public abstract void updateVisibleAndDisplayableRecordsForTable();

	/**
	 * @return visible and display able records (p.e. to build the partial data table)
	 */
	public Vector<Record> getVisibleAndDisplayableRecordsForTable() {
		return this.settings.isPartialDataTable() ? this.visibleAndDisplayableRecords : this.allRecords;
	}

	/**
	 * @return visible and displayable records (p.e. to build the partial data table)
	 */
	public Vector<Record> getVisibleAndDisplayableRecords() {
		return this.visibleAndDisplayableRecords;
	}

	/**
	 * @return all records for display
	 */
	public Vector<Record> getDisplayRecords() {
		return this.allRecords;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String newName) {
		this.name = newName;
	}

	/**
	 * Print record names array into the log.
	 * @param recordNames
	 */
	protected static void printRecordNames(String methodName, String[] recordNames) {
		StringBuilder sb = new StringBuilder();
		for (String recordName : recordNames) {
			sb.append(recordName).append(GDE.STRING_MESSAGE_CONCAT);
		}
		sb.delete(sb.length() - 3, sb.length());
		if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, methodName, sb.toString());
	}

	/**
	 * @param recordKey
	 * @param isLeft
	 * @return the scale axis position as function of available axis at the given side
	 */
	public int getAxisPosition(String recordKey, boolean isLeft) {
		int value = -1;
		if (isLeft) {
			for (String recordName : this.recordNames) {
				Record tmpRecord = this.get(recordName);
				if (tmpRecord.isPositionLeft && tmpRecord.isScaleVisible()) ++value;
				if (recordName.equals(recordKey)) break;
			}
		}
		else {
			for (String recordName : this.recordNames) {
				log.log(Level.FINER, "record name = " + recordName); //$NON-NLS-1$
				Record tmpRecord = this.get(recordName);
				if (!tmpRecord.isPositionLeft && tmpRecord.isScaleVisible()) ++value;
				if (recordName.equals(recordKey)) break;
			}
		}
		return value;
	}

	/**
	 * @param recordOrdinal
	 * @return the record based on ordinal
	 */
	public Record get(int recordOrdinal) {
		try {
			return this.get(this.recordNames[recordOrdinal]);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return super.size() > 0 ? this.get(0) : null;
		}
	}

	/**
	 * @param index
	 * @return the time value
	 */
	public double getTime_ms(int index) {
		return this.timeStep_ms.getTime_ms(index);
	}

	/**
	 * @return the maxValue
	 */
	public double getMaxValue() {
		return this.maxValue;
	}

	/**
	 * @param newMaxValue the maxValue to set
	 */
	public void setMaxValue(double newMaxValue) {
		this.maxValue = newMaxValue;
	}

	/**
	 * @return the minValue
	 */
	public double getMinValue() {
		return this.minValue;
	}

	/**
	 * @param newMinValue the minValue to set
	 */
	public void setMinValue(double newMinValue) {
		this.minValue = newMinValue;
	}

	/**
	 * @param recordKey the key which record should be measured
	 * @param enabled the boolean value to set
	 */
	public void setMeasurementMode(String recordKey, boolean enabled) {
		Record record = this.get(recordKey);
		if (record != null) {
			record.setMeasurementMode(enabled);
			if (enabled) {
				Record oldRecord = this.get(this.recordKeyMeasurement);
				if (oldRecord != null && !oldRecord.equals(record)) {
					oldRecord.setMeasurementMode(false);
					oldRecord.setDeltaMeasurementMode(false);
				}
				this.recordKeyMeasurement = recordKey;
				record.setDeltaMeasurementMode(false);
			}
		}
	}

	/**
	 * @param recordKey the key which record should be measured
	 * @param enabled the boolean value to set
	 */
	public void setDeltaMeasurementMode(String recordKey, boolean enabled) {
		Record record = this.get(recordKey);
		if (record != null) {
			record.setDeltaMeasurementMode(enabled);
			if (enabled) {
				Record oldRecord = this.get(this.recordKeyMeasurement);
				if (oldRecord != null && !oldRecord.equals(record)) {
					oldRecord.setMeasurementMode(false);
					oldRecord.setDeltaMeasurementMode(false);
				}
				this.recordKeyMeasurement = recordKey;
				record.setMeasurementMode(false);
			}
		}
	}

	/**
	 * Clear measurement modes if any.
	 */
	public void clearMeasurementModes() {
		Record record = this.get(this.recordKeyMeasurement);
		if (record != null) {
			record.setMeasurementMode(false);
			record.setDeltaMeasurementMode(false);
		}
	}

	/**
	 * @param recordKey the key which record should be measured
	 * @return the isMeasurementMode
	 */
	public boolean isMeasurementMode(String recordKey) {
		return this.get(recordKey) != null ? this.get(recordKey).isMeasurementMode() : false;
	}

	/**
	 * @param recordKey the key which record should be measured
	 * @return the isDeltaMeasurementMode
	 */
	public boolean isDeltaMeasurementMode(String recordKey) {
		return this.get(recordKey) != null ? this.get(recordKey).isDeltaMeasurementMode() : false;
	}

	/**
	 * Reset the record set in viewpoint of measurement.
	 */
	public void resetMeasurement() {
		this.setMeasurementMode(this.recordKeyMeasurement, false);
		this.setDeltaMeasurementMode(this.recordKeyMeasurement, false);
	}

	/**
	 * @return the recordKeyMeasurement
	 */
	public String getRecordKeyMeasurement() {
		return this.recordKeyMeasurement;
	}

	/**
	 * @return the curveBounds, this is the area where curves are drawn
	 */
	public Rectangle getDrawAreaBounds() {
		return this.drawAreaBounds;
	}

	/**
	 * Define the area where curves are drawn (clipping, image).
	 * @param newDrawAreaBounds the curveBounds to set
	 */
	public void setDrawAreaBounds(Rectangle newDrawAreaBounds) {
		this.drawAreaBounds = newDrawAreaBounds;
	}

	/**
	 * @return the channel/configuration number
	 */
	public int getChannelConfigNumber() {
		return this.parent != null ? this.parent.number : 1; //compare set does not have a parent
	}

	/**
	 * @return the channel (or) configuration name
	 */
	public String getChannelConfigName() {
		return this.parent != null ? this.parent.channelConfigName : GDE.STRING_EMPTY;
	}

	/**
	 * @return the horizontalGridRecord ordinal
	 */
	public abstract int getHorizontalGridRecordOrdinal();

	/**
	 * @param newHorizontalGridRecordOrdinal of the horizontal grid record name to set
	 */
	public void setHorizontalGridRecordOrdinal(int newHorizontalGridRecordOrdinal) {
		if (newHorizontalGridRecordOrdinal >= this.size()) newHorizontalGridRecordOrdinal = 0;
		this.horizontalGridRecordOrdinal = this.isOneOfSyncableRecord(this.get(newHorizontalGridRecordOrdinal)) ? this.getSyncMasterRecordOrdinal(this.get(newHorizontalGridRecordOrdinal))
				: newHorizontalGridRecordOrdinal;
	}

	/**
	 * @return the horizontalGridType
	 */
	public int getHorizontalGridType() {
		return this.horizontalGridType;
	}

	/**
	 * @param newHorizontalGridType the horizontalGridType to set
	 */
	public void setHorizontalGridType(int newHorizontalGridType) {
		this.horizontalGridType = newHorizontalGridType;
	}

	/**
	 * @return the horizontalGrid
	 */
	public Vector<Integer> getHorizontalGrid() {
		return this.horizontalGrid;
	}

	/**
	 * @param newHorizontalGrid the horizontalGrid to set
	 */
	public void setHorizontalGrid(Vector<Integer> newHorizontalGrid) {
		this.horizontalGrid = new Vector<Integer>(newHorizontalGrid);
	}

	/**
	 * @return the horizontalGridColor
	 */
	public Color getHorizontalGridColor() {
		return this.horizontalGridColor;
	}

	/**
	 * @param newHorizontalGridColor the horizontalGridColor to set
	 */
	public void setHorizontalGridColor(Color newHorizontalGridColor) {
		this.horizontalGridColor = newHorizontalGridColor;
	}

	/**
	 * @return the horizontalGridLineStyle
	 */
	public int getHorizontalGridLineStyle() {
		return this.horizontalGridLineStyle;
	}

	/**
	 * @param newHorizontalGridLineStyle the horizontalGridLineStyle to set
	 */
	public void setHorizontalGridLineStyle(int newHorizontalGridLineStyle) {
		this.horizontalGridLineStyle = newHorizontalGridLineStyle;
	}

	/**
	 * @return the device
	 */
	public IDevice getDevice() {
		return this.device != null ? this.device : this.application.getActiveDevice();
	}

	/**
	 * For devices with none constant time step between measurement points it returns the average value.
	 * Do not use for calculation, use for logging purpose only.
	 * @return a valid time step in msec for record sets from devices with constant time step between measurement points !
	 */
	public abstract double getAverageTimeStep_ms();

		/**
	 * @param syncMasterRecordOrdinal
	 * @param tmpRecord
	 * @return true if the scaleSyncedRecords vector contains the given record not using equivalent entries, like the Vector.contains() method
	 */
	public boolean isRecordContained(int syncMasterRecordOrdinal, Record tmpRecord) {
		final String $METHOD_NAME = "isRecordContained";
		boolean isContained = false;
		synchronized (this.scaleSyncedRecords) {
			if (this.scaleSyncedRecords.get(syncMasterRecordOrdinal) != null) {
				for (Record tempRecord : this.scaleSyncedRecords.get(syncMasterRecordOrdinal)) {
					if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "compare " + tempRecord.name + " with " + tmpRecord.name);
					if (tempRecord.name.equals(tmpRecord.name)) {
						isContained = true;
						break;
					}
				}
			}
		}
		return isContained;
	}

	/**
	 * Update the scale values from sync record if visible and update referenced records to enable drawing of curve, set min/max.
	 */
	public abstract void updateSyncRecordScale();

	/**
	 * Synchronize scales according device properties.
	 */
	public abstract void syncScaleOfSyncableRecords();

	/**
	 * Synchronize scale properties of master and slave scale synchronized records.
	 */
	public void syncMasterSlaveRecords(Record syncInputRecord, int type) {
		for (Integer syncRecordOrdinal : this.scaleSyncedRecords.keySet()) {
			if (this.isRecordContained(syncRecordOrdinal, syncInputRecord)) {
				switch (type) {
				case Record.TYPE_AXIS_END_VALUES:
					boolean tmpIsRoundout = syncInputRecord.isRoundOut;
					boolean tmpIsStartpointZero = syncInputRecord.isStartpointZero;
					boolean tmpIsStartEndDefined = syncInputRecord.isStartEndDefined;
					double minScaleValue = syncInputRecord.minScaleValue;
					double maxScaleValue = syncInputRecord.maxScaleValue;
					for (Record tmpRecord : this.scaleSyncedRecords.get(syncRecordOrdinal)) {
						synchronized (tmpRecord) {
							tmpRecord.isRoundOut = tmpIsRoundout;
							tmpRecord.isStartpointZero = tmpIsStartpointZero;
							tmpRecord.isStartEndDefined = tmpIsStartEndDefined;
							tmpRecord.minScaleValue = minScaleValue;
							tmpRecord.maxScaleValue = maxScaleValue;
						}
						//log.log(Level.OFF, String.format("%s minScaleValue=%.2f maxScaleValue=%.2f", tmpRecord.getName(), tmpRecord.minScaleValue, tmpRecord.maxScaleValue));
					}
					break;
				case Record.TYPE_AXIS_NUMBER_FORMAT:
					DecimalFormat tmpDf = syncInputRecord.df;
					int numberFormat = syncInputRecord.numberFormat;
					for (Record tmpRecord : this.scaleSyncedRecords.get(syncRecordOrdinal)) {
						synchronized (tmpRecord) {
							tmpRecord.df = (DecimalFormat) tmpDf.clone();
							tmpRecord.numberFormat = numberFormat;
						}
					}
					break;
				case Record.TYPE_AXIS_SCALE_POSITION:
					boolean tmpIsPositionLeft = syncInputRecord.isPositionLeft;
					for (Record tmpRecord : this.scaleSyncedRecords.get(syncRecordOrdinal)) {
						synchronized (tmpRecord) {
							tmpRecord.isPositionLeft = tmpIsPositionLeft;
						}
					}
					break;
				}
			}
		}
	}

	/**
	 * @return true only if both isSyncableSynced && isOneSyncableVisible are true
	 */
	public boolean hasSynchronizedRecords() {
		return !this.scaleSyncedRecords.isEmpty();
	}

	/**
	 * @return true if one of the syncable records is visible
	 */
	public boolean isOneSyncableVisible() {
		for (Integer syncRecordOrdinal : this.scaleSyncedRecords.keySet()) {
			for (Record tmpRecord : this.scaleSyncedRecords.get(syncRecordOrdinal)) {
				if (tmpRecord != null && tmpRecord.isVisible) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @param syncMasterOrdinal the record name of the sync master record
	 * @return true if one of the syncable records is visible
	 */
	public boolean isOneSyncableVisible(int syncMasterOrdinal) {
		for (Record tmpRecord : this.scaleSyncedRecords.get(syncMasterOrdinal)) {
			if (tmpRecord != null && tmpRecord.isVisible && tmpRecord.isDisplayable) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the Vector containing the slave records sync by the master name
	 */
	public Vector<Record> getScaleSyncedRecords(int syncMasterRecordOrdinal) {
		return this.scaleSyncedRecords.get(syncMasterRecordOrdinal);
	}

	public int realSize() {
		return super.size();
	}

	/**
	 * @param queryRecord the record key to be used for the query
	 * @return true if syncable records contains queryRecordKey
	 */
	public boolean isOneOfSyncableRecord(Record queryRecord) {
		for (Integer syncRecordOrdinal : this.scaleSyncedRecords.keySet()) {
			if (this.isRecordContained(syncRecordOrdinal, queryRecord)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param queryRecord the record key to be used for the query
	 * @return the synchronization master record name
	 */
	public int getSyncMasterRecordOrdinal(Record queryRecord) {
		for (Integer syncRecordOrdinal : this.scaleSyncedRecords.keySet()) {
			if (this.isRecordContained(syncRecordOrdinal, queryRecord)) {
				return syncRecordOrdinal;
			}
		}
		return -1;
	}

	public String getDescription() {
		return this.description;
	}

}
