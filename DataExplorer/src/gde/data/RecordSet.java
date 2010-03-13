/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.data;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import osde.log.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;

import osde.OSDE;
import osde.config.Settings;
import osde.device.DataTypes;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.device.PropertyType;
import osde.device.StatisticsType;
import osde.device.TriggerType;
import osde.exception.DataInconsitsentException;
import osde.io.LogViewReader;
import osde.io.OsdReaderWriter;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.tab.GraphicsWindow;
import osde.utils.CalculationThread;
import osde.utils.CellVoltageValues;
import osde.utils.StringHelper;
import osde.utils.TimeLine;

/**
 * RecordSet class holds all the data records for the configured measurement of a device
 * @author Winfried Brügmann
 */
public class RecordSet extends HashMap<String, Record> {
	final static String						$CLASS_NAME 									= RecordSet.class.getName();
	final static long							serialVersionUID							= 26031957;
	final static Logger						log														= Logger.getLogger(RecordSet.class.getName());
	final DecimalFormat						df														= new DecimalFormat("0.000");										//$NON-NLS-1$

	TimeSteps											timeStep_ms;

	String												name;																																					//1)Flugaufzeichnung, 2)Laden, 3)Entladen, ..
	final Channel									parent;
	String												header												= null;
	String[]											recordNames;																																	//Spannung, Strom, ..
	String[]											noneCalculationRecords 				= new String[0];																//records/measurements which are active or inactive
	String												description										= OSDE.STRING_EMPTY;
	boolean												isSaved												= false;																				//indicates if the record set is saved to file
	boolean												isRaw													= false;																				//indicates imported file with raw data, no translation at all
	boolean												isFromFile										= false;																				//indicates that this record set was created by loading data from file
	boolean												isRecalculation								= true;																					//indicates record is modified and need re-calculation
	int														fileDataSize									= 0; 																						//number of integer values per record
	int														fileDataBytes									= 0; 																						//number of bytes containing all records data 
	long													fileDataPointer								= 0; 																						//file pointer where the data of this record begins
	boolean												hasDisplayableData						= false;
	int														xScaleStep										= 0; 																						// steps in x direction to draw the curves, normally 1
	Rectangle											drawAreaBounds;																																// draw area in display pixel

	// sync enabled records
	Vector<String>								potentialSyncableRecords			= new Vector<String>();													//collection of record keys where scales might be synchronized
	Vector<String>								syncableRecords								= new Vector<String>();													//collection of potential syncable and displayable record keys
	boolean												isSyncableChecked							= false;
	boolean												isSyncRequested								= false;
	int														syncMin												= 0;
	int														syncMax												= 0;
	boolean												syncScalePositionLeft					= false;
	
	//for compare set x min/max and y max (time) might be different
	boolean												isCompareSet									= false;
//	int														maxSize												= 0;								//number of data point * time step = total time
	double												maxTime												= 0.0;							//compare set -> each record will have its own timeSteps_ms, 
																																									//so the biggest record in view point of time will define the time scale
	double												maxValue											= Integer.MIN_VALUE;
	double												minValue											= Integer.MAX_VALUE;														//min max value

	//zooming
	int														zoomLevel											= 0;																						//0 == not zoomed
	boolean												isZoomMode										= false;
	boolean												isScopeMode										= false;
	int														scopeModeOffset; 						// defines the offset in record pixel
	int														scopeModeSize;							// defines the number of record pixels to be displayed

	// measurement
	String												recordKeyMeasurement;

	public static final String		DESCRIPTION_TEXT_LEAD					= Messages.getString(MessageIds.OSDE_MSGT0129);

	public static final int				MAX_NAME_LENGTH								= 40;

	public static final String		TIME_STEP_MS									= "timeStep_ms";																//$NON-NLS-1$
	public static final String		TIME													= "time";																				//$NON-NLS-1$
	public static final String		TIME_GRID_TYPE								= "RecordSet_timeGridType";											//$NON-NLS-1$
	public static final String		TIME_GRID_COLOR								= "RecordSet_timeGridColor";										//$NON-NLS-1$
	public static final String		TIME_GRID_LINE_STYLE					= "RecordSet_timeGridLineStyle";								//$NON-NLS-1$
	public static final int				TIME_GRID_NONE								= 0;																						// no time grid
	public static final int				TIME_GRID_MAIN								= 1;																						// each main tickmark
	public static final int				TIME_GRID_MOD60								= 2;																						// each mod60 tickmark
	int														timeGridType									= TIME_GRID_NONE;
	Vector<Integer>								timeGrid											= new Vector<Integer>();												// contains the time grid position, updated from TimeLine.drawTickMarks
	Color													timeGridColor									= OpenSerialDataExplorer.COLOR_GREY;
	int														timeGridLineStyle							= SWT.LINE_DOT;

	@Deprecated
	public static final String		HORIZONTAL_GRID_RECORD				= "RecordSet_horizontalGridRecord";							//$NON-NLS-1$
	public static final String		HORIZONTAL_GRID_RECORD_ORDINAL= "RecordSet_horizontalGridRecordOrdinal";			//$NON-NLS-1$
	public static final String		HORIZONTAL_GRID_TYPE					= "RecordSet_horizontalGridType";								//$NON-NLS-1$
	public static final String		HORIZONTAL_GRID_COLOR					= "RecordSet_horizontalGridColor";							//$NON-NLS-1$
	public static final String		HORIZONTAL_GRID_LINE_STYLE		= "RecordSet_horizontalGridLineStyle";					//$NON-NLS-1$
	public static final int				HORIZONTAL_GRID_NONE					= 0;																						// no time grid
	public static final int				HORIZONTAL_GRID_EVERY					= 1;																						// each main tickmark
	public static final int				HORIZONTAL_GRID_SECOND				= 2;																						// each main tickmark
	int														horizontalGridType						= HORIZONTAL_GRID_NONE;
	Vector<Integer>								horizontalGrid								= new Vector<Integer>();												// contains the time grid position, updated from TimeLine.drawTickMarks
	Color													horizontalGridColor						= OpenSerialDataExplorer.COLOR_GREY;
	int														horizontalGridLineStyle				= SWT.LINE_DASH;
	int														horizontalGridRecordOrdinal		= -1;																						// recordNames[horizontalGridRecord]

	int[] 												voltageLimits									= CellVoltageValues.getVoltageLimits(); 			  // voltage limits for LiXx cells, initial LiPo
	public static final String		VOLTAGE_LIMITS								= "RecordSet_voltageLimits";										// each main tickmark //$NON-NLS-1$		
	
	boolean												isSyncRecordSelected					= false;
	public static final	String		SYNC_RECORD_SELECTED					= "Syncable_record_selected";
	
	private final String[]				propertyKeys									= new String[] { TIME_STEP_MS, HORIZONTAL_GRID_RECORD_ORDINAL, HORIZONTAL_GRID_RECORD, TIME_GRID_TYPE, TIME_GRID_LINE_STYLE, TIME_GRID_COLOR, HORIZONTAL_GRID_TYPE,
			HORIZONTAL_GRID_LINE_STYLE, HORIZONTAL_GRID_COLOR, VOLTAGE_LIMITS, SYNC_RECORD_SELECTED	};

	int														configuredDisplayable					= 0;																						// number of record which must be displayable before table calculation begins

	public final static String		UNSAVED_REASON_GRAPHICS				= Messages.getString(MessageIds.OSDE_MSGT0130);
	public final static String		UNSAVED_REASON_DATA						= Messages.getString(MessageIds.OSDE_MSGT0131);
	public final static String		UNSAVED_REASON_CONFIGURATION	= Messages.getString(MessageIds.OSDE_MSGT0132);
	Vector<String>								unsaveReasons									= new Vector<String>();
	int														changeCounter									= 0;																						// indicates change in general

	final OpenSerialDataExplorer	application;																																	// pointer to main application
	final Channels								channels;																																			// start point of data hierarchy
	final IDevice									device;

	/**
	 * record set data buffers according the size of given names array, where
	 * the name is the key to access the data buffer
	 * @param channelNumber the channel number to be used
	 * @param newName for the records like "1) Laden" 
	 * @param measurementNames array of the device supported measurement names
	 * @param newTimeStep_ms time in msec of device measures points
	 * @param isRawValue specified if dependent values has been calculated
	 * @param isFromFileValue specifies if the data are red from file and if not modified don't need to be saved
	 */
	public RecordSet(IDevice useDevice, int channelNumber, String newName, String[] measurementNames, double newTimeStep_ms, boolean isRawValue, boolean isFromFileValue) {
		super(measurementNames.length);
		this.channels = Channels.getInstance();
		this.device = useDevice;
		this.parent = this.channels.get(channelNumber);
		this.name = newName.length() <= RecordSet.MAX_NAME_LENGTH ? newName : newName.substring(0, 30);
		this.recordNames = measurementNames.clone();
		//this.timeStep_ms = new TimeSteps(this.get(0), newTimeStep_ms);
		this.application = OpenSerialDataExplorer.getInstance();
		this.isRaw = isRawValue;
		this.isFromFile = isFromFileValue;
		this.description = (this.device != null ? this.device.getName()+OSDE.STRING_MESSAGE_CONCAT : OSDE.STRING_EMPTY) 
			+ DESCRIPTION_TEXT_LEAD + StringHelper.getDateAndTime();
	}

	/**
	 * special record set data buffers according the size of given names array, where
	 * the name is the key to access the data buffer used to hold compare able records (compare set) 
	 * @param useDevice the device
	 * @param newChannelName the channel name or configuration name
	 * @param newName for the records like "1) Laden" 
	 * @param newTimeStep_ms time in msec of device measures points
	 */
	public RecordSet(IDevice useDevice, String newChannelName, String newName, double newTimeStep_ms) {
		super();
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = null;
		this.device = useDevice;
		this.parent = null;
		this.name = newName.length() <= RecordSet.MAX_NAME_LENGTH ? newName : newName.substring(0, 30);
		this.recordNames = new String[0];
		//this.timeStep_ms = new TimeSteps(this.get(0), newTimeStep_ms);
		this.isRaw = true;
		this.isCompareSet = true;
	}

	/**
	 * copy constructor - used to copy a record set to another channel/configuration, 
	 * where the configuration coming from the device properties file
	 * @param recordSet
	 * @param channelConfigurationNumber
	 */
	private RecordSet(RecordSet recordSet, int channelConfigurationNumber) {
		super(recordSet); // hashmap

		this.device = recordSet.device; // this is a reference
		this.name = recordSet.name;
		this.application = recordSet.application;
		this.channels = recordSet.channels;
		this.parent = this.channels.get(channelConfigurationNumber);

		// check if there is a miss match of measurement names and correction required
		String[] oldRecordNames = recordSet.recordNames;
		if(log.isLoggable(Level.FINER)) {
			StringBuilder sb = new StringBuilder();
			for (String string : oldRecordNames) {
				sb.append(string).append(" "); //$NON-NLS-1$
			}
			log.log(Level.FINER, "oldRecordNames = "+ sb.toString()); //$NON-NLS-1$
		}
		String[] newRecordNames = this.device.getMeasurementNames(channelConfigurationNumber);
		if(log.isLoggable(Level.FINER)) {
			StringBuilder sb = new StringBuilder();
			for (String string : newRecordNames) {
				sb.append(string).append(" "); //$NON-NLS-1$
			}
			log.log(Level.FINER, "newRecordNames = "+ sb.toString()); //$NON-NLS-1$
		}
		//copy records while exchange record name (may change to other language!)
		for (int i = 0; i < newRecordNames.length; i++) {
			if (!oldRecordNames[i].equals(newRecordNames[i])) {
				// add the old record with new name
				this.put(newRecordNames[i], this.getRecord(oldRecordNames[i]).clone(newRecordNames[i]));
				// remove the old record
				this.remove(oldRecordNames[i]);
			}
		}
		this.recordNames = newRecordNames.clone();

		// update child records to new channel or configuration key and to the new parent
		for (int i = 0; i < this.recordNames.length; ++i) {
			Record tmpRecord = this.get(this.recordNames[i]);
			tmpRecord.setParent(this);

			tmpRecord.statistics = this.device.getMeasurementStatistic(channelConfigurationNumber, i);
			if (tmpRecord.statistics == null)
				System.out.println();
			TriggerType tmpTrigger = tmpRecord.statistics != null ? tmpRecord.statistics.getTrigger() : null;
			tmpRecord.triggerIsGreater = tmpTrigger != null ? tmpTrigger.isGreater() : null;
			tmpRecord.triggerLevel = tmpTrigger != null ? tmpTrigger.getLevel() : null;
			tmpRecord.minTriggerTimeSec = tmpTrigger != null ? tmpTrigger.getMinTimeSec() : null;

			//copy record properties if -> record properties available == name equal 
			if (recordSet.get(this.recordNames[i]) != null)
				tmpRecord.setProperties(recordSet.get(this.recordNames[i]).getProperties());
			else
				tmpRecord.setProperties(this.device.getProperties(channelConfigurationNumber, i));
		}

		this.timeStep_ms = recordSet.timeStep_ms.clone();
		this.description = recordSet.description;
		this.isSaved = false;
		this.isRaw = recordSet.isRaw;
		this.isFromFile = recordSet.isFromFile;
		this.drawAreaBounds = recordSet.drawAreaBounds;

		this.isCompareSet = recordSet.isCompareSet;

//		this.maxSize = recordSet.maxSize;
		this.maxTime = recordSet.maxTime;
		this.maxValue = recordSet.maxValue;
		this.minValue = recordSet.minValue;

		this.zoomLevel = recordSet.zoomLevel;
		this.isZoomMode = recordSet.isZoomMode;

		this.recordKeyMeasurement = recordSet.recordKeyMeasurement;

		this.timeGridType = recordSet.timeGridType;
		this.timeGrid = new Vector<Integer>(recordSet.timeGrid);
		this.timeGridColor = recordSet.timeGridColor;
		this.timeGridLineStyle = recordSet.timeGridLineStyle;

		this.horizontalGridType = recordSet.horizontalGridType;
		this.horizontalGrid = new Vector<Integer>(recordSet.horizontalGrid);
		this.horizontalGridColor = recordSet.horizontalGridColor;
		this.horizontalGridLineStyle = recordSet.horizontalGridLineStyle;
		this.horizontalGridRecordOrdinal = recordSet.horizontalGridRecordOrdinal;

		this.configuredDisplayable = recordSet.configuredDisplayable;
	}

	/**
	 * clone method used to move record sets to other configuration or channel
	 * @param channelConfiguationNumber 
	 */
	public RecordSet clone(int channelConfiguationNumber) {
		return new RecordSet(this, channelConfiguationNumber);
	}

	/**
	 * copy constructor - used to copy a record set to another channel/configuration, 
	 * wherer the configuration comming from the device properties file
	 * @param recordSet
	 * @param newChannelConfiguration
	 */
	private RecordSet(RecordSet recordSet, int dataIndex, boolean isFromBegin) {
		super(recordSet);

		this.device = recordSet.device; // this is a reference
		this.name = recordSet.name.length() < MAX_NAME_LENGTH ? recordSet.name + OSDE.STRING_UNDER_BAR : recordSet.name.substring(0, MAX_NAME_LENGTH - 1) + OSDE.STRING_UNDER_BAR;
		this.application = recordSet.application;
		this.channels = recordSet.channels;
		this.parent = recordSet.parent;

		if (recordSet.isSyncableChecked) {
			String syncRecordName = recordSet.getSyncableName();
			if (syncRecordName.length() > 5) { // " 1..2"
				this.remove(syncRecordName);
			}
		}

		this.recordNames = recordSet.getRecordNames().clone(); // copy record names without possible syncableName

		// update child records
		for (String recordKey : this.recordNames) {
			this.put(recordKey, this.get(recordKey).clone(dataIndex, isFromBegin));
		}

		if (recordSet.timeStep_ms != null && !recordSet.timeStep_ms.isConstant) { //time step vector must be updated as well
			this.timeStep_ms = recordSet.timeStep_ms.clone(dataIndex, isFromBegin);
		}
		else if (recordSet.timeStep_ms != null && recordSet.timeStep_ms.isConstant) { 
			this.timeStep_ms = recordSet.timeStep_ms.clone();
		}
		else {
			this.timeStep_ms = null;
		}

		
		this.description = recordSet.description;
		this.isSaved = false;
		this.isRaw = recordSet.isRaw;
		this.isFromFile = recordSet.isFromFile;
		this.drawAreaBounds = recordSet.drawAreaBounds;

		this.isCompareSet = recordSet.isCompareSet;

//		this.maxSize = recordSet.maxSize;
		this.maxTime = recordSet.maxTime;
		this.maxValue = recordSet.maxValue;
		this.minValue = recordSet.minValue;

		this.zoomLevel = 0;
		this.isZoomMode = false;

		this.recordKeyMeasurement = recordSet.recordKeyMeasurement;

		this.timeGridType = recordSet.timeGridType;
		this.timeGrid = new Vector<Integer>(recordSet.timeGrid);
		this.timeGridColor = recordSet.timeGridColor;
		this.timeGridLineStyle = recordSet.timeGridLineStyle;

		this.horizontalGridType = recordSet.horizontalGridType;
		this.horizontalGrid = new Vector<Integer>(recordSet.horizontalGrid);
		this.horizontalGridColor = recordSet.horizontalGridColor;
		this.horizontalGridLineStyle = recordSet.horizontalGridLineStyle;
		this.horizontalGridRecordOrdinal = recordSet.horizontalGridRecordOrdinal;

		this.configuredDisplayable = recordSet.configuredDisplayable;
		
		this.device.updateVisibilityStatus(this);
	}

	/**
	 * clone method re-writes data points of all records of this record set
	 * - if isFromBegin == true, the given index is the index where the record starts after this operation
	 * - if isFromBegin == false, the given index represents the last data point index of the records.
	 * @param dataIndex
	 * @param isFromBegin
	 * @return new created record set
	 */
	public RecordSet clone(int dataIndex, boolean isFromBegin) {
		return new RecordSet(this, dataIndex, isFromBegin);
	}

	/**
	 * remove the record as well as the key
	 */
	public void remove(String recordKey) {
		super.remove(recordKey);
		if (this.recordNames != null) this.removeRecordName(recordKey);
	}
	
	/**
	 * check all records of this record set are displayable
	 * @return true/false	
	 */
	public synchronized boolean checkAllRecordsDisplayable() {
		int displayableRecordEntries = 0;
		for (String recordKey : this.recordNames) {
			if (this.getRecord(recordKey).isDisplayable()) ++displayableRecordEntries;
			log.log(Level.FINER, recordKey + " isDiplayable = " + this.getRecord(recordKey).isDisplayable()); //$NON-NLS-1$
		}

		int targetDisplayable = this.configuredDisplayable == 0 ? this.getRecordNames().length : this.configuredDisplayable;
		log.log(Level.TIME, "targetDisplayable = " + targetDisplayable + " - displayableRecordEntries = " + displayableRecordEntries); //$NON-NLS-1$ //$NON-NLS-2$

		return displayableRecordEntries >= targetDisplayable;
	}

	/**
	 * returns a specific data vector selected by given key data name
	 * @param recordNameKey
	 * @return Vector<Integer>
	 */
	public Record getRecord(String recordNameKey) {
		return this.get(recordNameKey);
	}

	/**
	 * method to add a series of points to the associated records
	 * @param points as int[], where the length must fit records.size()
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addPoints(int[] points) throws DataInconsitsentException {
		final String $METHOD_NAME = "addPoints"; //$NON-NLS-1$
		if (points.length == this.size()) {
			for (int i = 0; i < points.length; i++) {
				this.getRecord(this.recordNames[i]).add(points[i]);
				
				// check if record synchronization is activated and update syncMin/syncMax
				if (this.syncableRecords.contains(this.recordNames[i])) {
					if (this.syncMin == 0 && this.syncMax == 0) {
						this.syncMin = points[i];
						this.syncMax = points[i];
					}
					else {
						if (points[i] < this.syncMin) 			this.syncMin = points[i];
						else if (points[i] > this.syncMax) 	this.syncMax = points[i];
					}
					if (this.isSyncRequested) this.updateSyncRecordScale();	
				}
			}
			if (log.isLoggable(Level.FINEST)) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < points.length; i++) {
					sb.append(points[i]).append(OSDE.STRING_BLANK);
				}
				log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, sb.toString());
			}
		}
		else
			throw new DataInconsitsentException(Messages.getString(MessageIds.OSDE_MSGE0035, new Object[] {this.getClass().getSimpleName(), $METHOD_NAME})); //$NON-NLS-1$
		
		this.hasDisplayableData = true;
	}

	/**
	 * method to add a series of points to the associated records
	 * @param points as int[], where the length must fit records.size()
	 * @param time_ms
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addPoints(int[] points, double time_ms) throws DataInconsitsentException {
		this.timeStep_ms.add(time_ms);
		this.addPoints(points);
	}

	/**
	 * method to add a series of points to none calculation records (records active or inactive)
	 * @param points as int[], where the length must fit records.size()
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addNoneCalculationRecordsPoints(int[] points) throws DataInconsitsentException {
		final String $METHOD_NAME = "addPoints"; //$NON-NLS-1$
		if (points.length == this.getNoneCalculationRecordNames().length) {
			for (int i = 0; i < points.length; i++) {
				this.getRecord(this.noneCalculationRecords[i]).add(points[i]);
			}
			if (log.isLoggable(Level.FINEST)) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < points.length; i++) {
					sb.append(points[i]).append(OSDE.STRING_BLANK);
				}
				log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, sb.toString());
			}
		}
		else
			throw new DataInconsitsentException(Messages.getString(MessageIds.OSDE_MSGE0036, new Object[] {this.getClass().getSimpleName(), $METHOD_NAME}));
		
		this.hasDisplayableData = true;
		}

	/**
	 * method to add a series of points to none calculation records (records active or inactive)
	 * @param points as int[], where the length must fit records.size()
	 * @param time_ms
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addNoneCalculationRecordsPoints(int[] points, double time_ms) throws DataInconsitsentException {
		this.timeStep_ms.add(time_ms);
		this.addNoneCalculationRecordsPoints(points);
	}

	/**
	 * get all calculated and formated data table points of a given index, decimal format is "0.000"
	 * @param index of the data points
	 * @return formatted values as string array including time
	 */
	public String[] getDataTableRow(int index) {
		int[] tmpValues = this.device.prepareDataTableRow(this, index);
		String[] strValues = new String[tmpValues.length];
		for (int i = 0; i < strValues.length; i++) {
			strValues[i] = this.df.format(tmpValues[i] / 1000.0);
		}
		return strValues;
	}

	/**
	 * @return a valid time step in msec for record sets from devices with constant time step between measurement points !
	 * For devices with none constant time step between measurement points it returns the average value.
	 * Do not use for calculation, use for logging purpose only.
	 */
	public double getAverageTimeStep_ms() {
		return this.timeStep_ms != null ? this.timeStep_ms.getAverageTimeStep_ms() : this.get(0).timeStep_ms != null ? this.get(0).timeStep_ms.getAverageTimeStep_ms() : -1.0;
	}

	/**
	 * @return the isConstant true if time step is a constant value between measurement points
	 */
	public boolean isTimeStepConstant() {
		return this.timeStep_ms.isConstant;
	}

	/**
	 * @return the timeSteps_ms
	 */
	public double getTime_ms(int index) {
		return this.timeStep_ms.getTime_ms(index);
	}

	/**
	 * @return the time information for index given
	 */
	public int getTime(int index) {
		return this.timeStep_ms.get(index);
	}

	/**
	 * add a new time step to the time steps vector
	 * @param timeValue
	 */
	public void addTimeStep_ms(double timeValue) {
		this.timeStep_ms.add(timeValue);
	}
	
	/**
	 * @return the maximum time of this record set, which should correspondence to the last entry in timeSteps
	 */
	public double getMaxTime_ms() {
		return this.timeStep_ms == null ? 0.0 : this.timeStep_ms.isConstant ? this.timeStep_ms.getMaxTime_ms() * (this.get(0).realSize()-1) : this.timeStep_ms.getMaxTime_ms();
	}

	/**
	 * method to get the sorted record names as array
	 * sorted according list in the device configuration (XML) file
	 * @return String[] containing record names 
	 */
	public String[] getRecordNames() {
		Vector<String> recordNamesVector = new Vector<String>();
		String syncPlaceholdername = this.getSyncableName();
		for (String recordName : this.recordNames) {
			if (!recordName.equals(syncPlaceholdername)) recordNamesVector.add(recordName);
		}
		return recordNamesVector.toArray(new String[0]).clone();
	}

	/**
	 * method to add an new record name 
	 */
	private void addRecordName(String newRecordName) {
		String[] newRecordNames = new String[this.recordNames.length + 1];
		System.arraycopy(this.recordNames, 0, newRecordNames, 0, this.recordNames.length);
		newRecordNames[this.recordNames.length] = newRecordName;
		this.recordNames = newRecordNames;
	}

	/**
	 * method to remove a record name 
	 */
	void removeRecordName(String deleteRecordName) {
		Vector<String> newRecordNames = new Vector<String>();
		for (String recordKey : this.recordNames) {
			if (!recordKey.equals(deleteRecordName)) newRecordNames.add(recordKey);
		}
		this.recordNames = newRecordNames.toArray(new String[0]);
	}

	/**
	 * replace a record name with a new one
	 * @param oldRecordName
	 * @param newRecordName
	 */
	public void replaceRecordName(String oldRecordName, String newRecordName) {
		for (int i = 0; i < this.recordNames.length; i++) {
			if (this.recordNames[i].equals(oldRecordName)) this.recordNames[i] = newRecordName;
		}
		if (this.get(newRecordName) == null) { // record may be created previously
			this.put(newRecordName, this.get(oldRecordName).clone(newRecordName));
			this.remove(oldRecordName);
		}
	}

	/**
	 * method to get the sorted record active names which are visible as string array
	 * @return String[] containing record names 
	 */
	public String[] getActiveAndVisibleRecordNames() {
		Vector<String> activeVisibleRecords = new Vector<String>();
		for (String recordKey : this.recordNames) {
			if (this.get(recordKey).isActive() && this.get(recordKey).isVisible()) activeVisibleRecords.add(recordKey);
		}
		return activeVisibleRecords.toArray(new String[0]);
	}

	/**
	 * method to get the sorted record active names which are visible as string array
	 * @return String[] containing record names 
	 */
	public String[] getVisibleRecordNames() {
		Vector<String> visibleRecords = new Vector<String>();
		for (String recordKey : this.recordNames) {
			if (this.get(recordKey).isVisible()) visibleRecords.add(recordKey);
		}
		return visibleRecords.toArray(new String[0]);
	}

	/**
	 * query number of visible and displayable records
	 * @return number of records visible and displayable (makeInActiveDisplayable)
	 */
	public int getNumberOfVisibleAndDisplayableRecords() {
		int visibleAndDisplayable = 0;
		for (String recordKey : this.recordNames) {
			if (this.get(recordKey).isVisible && this.get(recordKey).isDisplayable) visibleAndDisplayable++;
		}
		return visibleAndDisplayable;
	}

	/**
	 * query number of displayable records
	 * @return number of records displayable (makeInActiveDisplayable)
	 */
	public int getNumberOfDisplayableRecords() {
		int displayable = 0;
		for (String recordKey : this.recordNames) {
			if (this.get(recordKey).isDisplayable) displayable++;
		}
		return displayable;
	}

	/**
	 * method to get the sorted record displayable names as string array
	 * @return String[] containing record names 
	 */
	public String[] getDisplayableRecordNames() {
		Vector<String> displayableRecords = new Vector<String>();
		for (String recordKey : this.recordNames) {
			if (this.get(recordKey).isDisplayable()) displayableRecords.add(recordKey);
		}
		return displayableRecords.toArray(new String[0]);
	}

	/**
	 * method to get the sorted record active names as string array
	 * @return String[] containing record names 
	 */
	public String[] getActiveRecordNames() {
		Vector<String> activeRecords = new Vector<String>();
		for (String recordKey : this.recordNames) {
			if (this.get(recordKey).isActive()) activeRecords.add(recordKey);
		}
		return activeRecords.toArray(new String[0]);
	}

	/**
	 * method to get the sorted active or in active record names as string array
	 *  - records which does not have inactive or active flag are calculated from active or inactive
	 *  - all records not calculated may have the active status and must be stored
	 * @return String[] containing record names 
	 */
	public String[] getNoneCalculationRecordNames() {
		if (this.noneCalculationRecords.length == 0) {
			Vector<String> tmpCalculationRecords = new Vector<String>();
			String[] deviceMeasurements = this.device.getMeasurementNames(this.parent.number);
			// record names may not match device measurements, but device measurements might be more then existing records
			for (int i = 0; i < deviceMeasurements.length && i < this.size(); ++i) { 
				MeasurementType measurement = this.device.getMeasurement(this.parent.number, i);
				if (!measurement.isCalculation()) { // active or inactive 
					tmpCalculationRecords.add(this.recordNames[i]);
				}
			}
			this.noneCalculationRecords = tmpCalculationRecords.toArray(new String[0]);
		}
		return this.noneCalculationRecords;
	}

	/**
	 *	clear the record set compare view 
	 */
	public void clear() {
		super.clear();
		this.recordNames = new String[0];
		this.timeStep_ms = null;
//		this.maxSize = 0;
		this.maxTime = 0.0;
		this.maxValue = -20000;
		this.minValue = 20000;
		this.resetZoomAndMeasurement();
	}

	public String getName() {
		return this.name;
	}

	public void setName(String newName) {
		this.name = newName;
	}

	/**
	 * method to create a record set with given name "1) Laden" containing records according the device channle/configuration
	 * which are loaded from device properties file
	 * @param recordSetName the name of the record set
	 * @param device the instance of the device 
	 * @param channelNumber (name of the outlet or configuration)
	 * @param isRaw defines if the data needs translation using device specific properties
	 * @param isFromFile defines if a configuration change must be recorded to signal changes
	 * @return a record set containing all records (empty) as specified
	 */
	public static RecordSet createRecordSet(String recordSetName, IDevice device, int channelNumber, boolean isRaw, boolean isFromFile) {
		recordSetName = recordSetName.length() <= RecordSet.MAX_NAME_LENGTH ? recordSetName : recordSetName.substring(0, RecordSet.MAX_NAME_LENGTH);

		String[] recordNames = device.getMeasurementNames(channelNumber);
		if (recordNames.length == 0) { // simple check for valid device and record names, as fall back use the config from the first channel/configuration
			recordNames = device.getMeasurementNames(channelNumber = 1);
		}
		String [] recordSymbols = new String[recordNames.length];
		String [] recordUnits = new String[recordNames.length];
		for (int i = 0; i < recordNames.length; i++) {
			MeasurementType measurement = device.getMeasurement(channelNumber, i);
			recordSymbols[i] = measurement.getSymbol();
			recordUnits[i] = measurement.getUnit();
		}			
		return createRecordSet(recordSetName, device, channelNumber, recordNames, recordSymbols, recordUnits, device.getTimeStep_ms(), isRaw, isFromFile);
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
	public static RecordSet createRecordSet(String recordSetName, IDevice device, int channelConfigNumber, String[] recordNames, String[] recordSymbols, String[] recordUnits, double timeStep_ms, boolean isRaw, boolean isFromFile) {
		RecordSet newRecordSet = new RecordSet(device, channelConfigNumber, recordSetName, recordNames, timeStep_ms, isRaw, isFromFile);
		if (log.isLoggable(Level.FINE)) printRecordNames("createRecordSet() " + newRecordSet.name + " - " , newRecordSet.getRecordNames()); //$NON-NLS-1$ //$NON-NLS-2$

		newRecordSet.timeStep_ms = new TimeSteps(device.getTimeStep_ms());

		for (int i = 0; i < recordNames.length; i++) {
			MeasurementType measurement = device.getMeasurement(channelConfigNumber, i);
			Record tmpRecord = new Record(device, i, recordNames[i], recordSymbols[i], recordUnits[i], measurement.isActive(), measurement.getStatistics(), measurement.getProperty(), 5);
			tmpRecord.setColorDefaultsAndPosition(i);
			newRecordSet.put(recordNames[i], tmpRecord);
			log.log(Level.FINER, "added record for " + recordNames[i]); //$NON-NLS-1$
		}
		
		// check and update object key
		OpenSerialDataExplorer application = OpenSerialDataExplorer.getInstance();
		if (application != null && application.isObjectoriented()) {
			Channel activeChannel = Channels.getInstance().getActiveChannel();
			if (activeChannel != null && !activeChannel.getObjectKey().equals(Settings.getInstance().getActiveObject())) {
				activeChannel.setObjectKey(Settings.getInstance().getActiveObject());
			}
		}

		return newRecordSet;
	}

	/**
	 * print record names array
	 * @param recordNames
	 */
	static void printRecordNames(String methodName, String[] recordNames) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < recordNames.length; ++i) {
			sb.append(recordNames[i]).append(OSDE.STRING_MESSAGE_CONCAT);
		}
		sb.delete(sb.length() - 3, sb.length());
		log.logp(Level.FINE, $CLASS_NAME, methodName, sb.toString());
	}

	/**
	 * calculate the scale axis position as function of available axis at the given side
	 */
	public int getAxisPosition(String recordKey, boolean isLeft) {
		int value = -1;
		if (isLeft) {
			for (String recordName : this.recordNames) {
				Record tmpRecord = this.get(recordName);
				if (tmpRecord.isPositionLeft && tmpRecord.isVisible && (tmpRecord.isDisplayable && !tmpRecord.isScaleSynced || tmpRecord.isSyncPlaceholder)) ++value;
				if (recordName.equals(recordKey)) break;
			}
		}
		else {
			for (String recordName : this.recordNames) {
				log.log(Level.FINER, "record name = " + recordName); //$NON-NLS-1$
				Record tmpRecord = this.get(recordName);
				if (!tmpRecord.isPositionLeft && tmpRecord.isVisible && (tmpRecord.isDisplayable && !tmpRecord.isScaleSynced || tmpRecord.isSyncPlaceholder)) ++value;
				if (recordName.equals(recordKey)) break;
			}
		}
		return value;
	}

	/**
	 * calculate number of records with axis position is left
	 */
	public int getNumberVisibleWithAxisPosLeft() {
		int value = 0;
		for (String recordKey : this.recordNames) {
			Record record = this.get(recordKey);
			if (record.isVisible && record.isDisplayable && record.isPositionLeft) ++value;
		}
		return value;
	}

	/**
	 * check if this record set is one of the just active channel in UI
	 */
	public boolean isChildOfActiveChannel() {
		boolean isChild = false;
		//Channels channels = Channels.getInstance();
		RecordSet uiRecordSet = this.channels.getActiveChannel().get(this.name);
		if (uiRecordSet == this) isChild = true;
		return isChild;
	}

	/**
	 * overwrites default HashMap method
	 */
	public Record put(String key, Record record) {
		super.put(key, record);
		Record newRecord = this.get(key);
		//for compare set record following properties has to be checked at the point where
		newRecord.keyName = key;
		newRecord.parent = this;

		// add key to recordNames[] in case of TYPE_COMPARE_SET, keep ordinal to enable translate value
		if (this.isCompareSet) {
			this.addRecordName(key);
			newRecord.name = key;
			
			if (this.realSize() > 1) { 
				// keep the color of first added record
				newRecord.setColorDefaultsAndPosition(this.realSize());
				// synchronize scale format
				newRecord.numberFormat = this.get(0).numberFormat;
				newRecord.df = (DecimalFormat)this.get(0).df.clone();
			}
			// set all scales of compare set to left
			newRecord.isPositionLeft = true;
		}

		return newRecord;
	}
	
	/**
	 * get record based on ordinal
	 */
	public Record get(int recordOrdinal) {
		return this.get(this.recordNames[recordOrdinal]);
	}

	/**
	 * Set the time step in msec for record sets of devices with constant time step with a positive value.
	 * A negative value signals none-constant time steps for this record set.
	 * This method has to be called BEFORE adding data points and will set the time step if called the first time only, 
	 * if timeStep_ms needs to be changed for some reason use setNewTimeStep_ms
	 * @param newTimeStep_ms the timeStep_ms to set
	 */
	public void setTimeStep_ms(double newTimeStep_ms) {		
		this.timeStep_ms = this.timeStep_ms == null || this.timeStep_ms.size() == 0 ? new TimeSteps(newTimeStep_ms) : this.timeStep_ms;
	}

	/**
	 * Set the time step in msec for record sets of devices with constant time step with a positive value.
	 * A negative value signals none-constant time steps for this record set.
	 * @param newTimeStep_ms the timeStep_ms to set
	 */
	public void setNewTimeStep_ms(double newTimeStep_ms) {		
		this.timeStep_ms = new TimeSteps(newTimeStep_ms);
	}

	/**
	 * @return the isSaved
	 */
	public boolean isSaved() {
		return this.isSaved;
	}

	/**
	 * @param newValue the isSaved to set
	 */
	public void setSaved(boolean newValue) {
		if (newValue) this.unsaveReasons = new Vector<String>();
		this.isSaved = newValue;
	}

	/**
	 * set data unsaved with a given reason RecordSet.UNSAVED_REASON_*
	 * no influence for compare set
	 * @param unsavedReason
	 */
	public void setUnsaved(String unsavedReason) {
		if (!this.isCompareSet) {
			this.changeCounter++;
			this.isSaved = false;
			if (!this.unsaveReasons.contains(unsavedReason)) {
				this.unsaveReasons.add(unsavedReason);
			}
		}
	}

	/**
	 * @return the isRaw
	 */
	public boolean isRaw() {
		return this.isRaw;
	}

	/**
	 * @return the recordSetDescription
	 */
	public String getRecordSetDescription() {
		return this.description;
	}

	/**
	 * @param newRecordSetDescription the recordSetDescription to set
	 */
	public void setRecordSetDescription(String newRecordSetDescription) {
		this.description = newRecordSetDescription;
	}

	/**
	 * check if all records from this record set are displayable, starts calculation if required by calling makeInActiveDisplayable()
	 */
	public void checkAllDisplayable() {
		this.application.getActiveDevice().makeInActiveDisplayable(this);
		this.isRecalculation = false;
		
		// start a low prio tread to load other record set data
		Thread dataLoadThread = new Thread(new Runnable() {
      public void run() {
      	CalculationThread ct = RecordSet.this.device.getCalculationThread();
      	try {
    			Thread.sleep(1000);
      		while (ct != null && ct.isAlive()) {
      			log.log(Level.FINER, "CalculationThread isAlive"); //$NON-NLS-1$
      			Thread.sleep(1000);
      		}
				}
				catch (InterruptedException e) {
				}
				Channel activeChannel = RecordSet.this.channels.getActiveChannel();
				if (activeChannel != null) 
					activeChannel.checkAndLoadData();
			}
		});
		try {
			dataLoadThread.setPriority(Thread.NORM_PRIORITY - 2);
			dataLoadThread.start();
		}
		catch (RuntimeException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}

	}

	/**
	 * set all records from this record to displayable
	 */
	public void setAllDisplayable() {
		String syncableRecordKey = this.getSyncableName();
		for (String recordKey : this.recordNames) {
			if (!recordKey.equals(syncableRecordKey)) {
				this.get(recordKey).setDisplayable(true);
			}
		}
	}

	/**
	 * force enable all records to be displayed and active if none calculation, this is used for unknown data import or unknown configurations
	 */
	public void setAllVisibleAndDisplayable() {
		String syncableRecordKey = this.getSyncableName();
		for (String recordKey : this.recordNames) {
			if (!recordKey.equals(syncableRecordKey)) {
				Record record = this.get(recordKey);
				record.setVisible(true);
				record.setDisplayable(true);
			}
		}
		for (String recordKey : this.getNoneCalculationRecordNames()) {
			this.get(recordKey).setActive(true);
		}
	}

	/**
	 * @return the isFromFile, this flag indicates to call the make allInActiveDisplayable 
	 * - the handling might be different if data captured directly from device
	 */
	public boolean isFromFile() {
		return this.isFromFile;
	}

	/**
	 * query the size of record set child record 
	 * - normal record set will return the size of the data vector of first active in recordNames
	 * - zoomed set will return size of zoomOffset + zoomWith
	 * @return the size of data point to calculate the time unit
	 */
	public int getRecordDataSize(boolean isReal) {
		int size = 0;
		if (isReal) {
			for (String recordKey : this.recordNames) {
				if (get(recordKey).isActive()) {
					size = get(recordKey).realSize();
					break;
				}
			}
		}
		else {
			for (String recordKey : this.recordNames) {
				if (get(recordKey).isActive()) {
					size = get(recordKey).size();
					break;
				}
			}
		}
		return size;
	}

	/**
	 * set maximum time in msec relating to the record defining the time scale of a compare set
	 * @param newMaxTime the maxSize to set
	 */
	public void setCompareSetMaxScaleTime_ms(double newMaxTime) {
		this.maxTime = newMaxTime;
	}

	/**
	 * get maximum time in msec relating to the record defining the time scale of a compare set
	 */
	public double getCompareSetMaxScaleTime_ms() {
		return this.maxTime;
	}

	/**
	 * query maximum display scale value of a compare set
	 * @return the maxValue
	 */
	public double getMaxValue() {
		return this.maxValue;
	}

	/**
	 * set maximum display scale value of a compare set
	 * @param newMaxValue the maxValue to set
	 */
	public void setMaxValue(double newMaxValue) {
		this.maxValue = newMaxValue;
	}

	/**
	 * query minimum display scale value of a compare set
	 * @return the minValue
	 */
	public double getMinValue() {
		return this.minValue;
	}

	/**
	 * set minimum display scale value of a compare set
	 * @param newMinValue the minValue to set
	 */
	public void setMinValue(double newMinValue) {
		this.minValue = newMinValue;
	}

	/**
	 * @return the isCompareSet
	 */
	public boolean isCompareSet() {
		return this.isCompareSet;
	}

	/**
	 * @return the curveBounds, this is the area where curves are drawn
	 */
	public Rectangle getDrawAreaBounds() {
		return this.drawAreaBounds;
	}

	/**
	 * define the area where curves are drawn (clipping, image)
	 * @param newDrawAreaBounds the curveBounds to set
	 */
	public void setDrawAreaBounds(Rectangle newDrawAreaBounds) {
		this.drawAreaBounds = newDrawAreaBounds;
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
	 * @return the isZoomMode
	 */
	public boolean isZoomMode() {
		return this.isZoomMode;
	}

	/**
	 * set the mouse tracker in graphics window active for zoom window selection
	 * @param zoomModeEnabled the isZoomMode to set
	 */
	public void setZoomMode(boolean zoomModeEnabled) {
		if (!zoomModeEnabled) {
			this.resetMeasurement();
			if (this.recordNames.length != 0) { // check existens of records, a compare set may have no records
				// iterate children and reset min/max values
				for (String recordName : this.keySet()) {
					Record record = this.get(recordName);
					record.zoomOffset = 0;
					record.zoomTimeOffset = 0.0;
					record.drawTimeWidth = record.getMaxTime_ms();
					//log.log(Level.INFO, this.name + "this.getMaxTime_ms() = " + record.drawTimeWidth);
					record.minZoomScaleValue	= record.minScaleValue;
					record.maxZoomScaleValue	= record.maxScaleValue;
					log.log(Level.FINER, this.name + " zoomTimeOffset " + TimeLine.getFomatedTimeWithUnit(record.zoomTimeOffset) + " drawTimeWidth "  + TimeLine.getFomatedTimeWithUnit(record.drawTimeWidth));
				}
			}
		}
		else {
			for (String recordName : this.keySet()) {
				Record record = this.get(recordName);
				record.minZoomScaleValue = record.minScaleValue;
				record.maxZoomScaleValue = record.maxScaleValue;
			}
		}
		this.isZoomMode = zoomModeEnabled;
		this.isScopeMode = false;
	}

	/**
	 * reset the record set in viewpoint of measurement and zooming
	 */
	public void resetZoomAndMeasurement() {
		this.setZoomMode(false);
		this.setMeasurementMode(this.recordKeyMeasurement, false);
		this.setDeltaMeasurementMode(this.recordKeyMeasurement, false);
	}

	/**
	 * reset the record set in viewpoint of measurement
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
	 * set the zoom bounds from display to child records as record point offset and size
	 * @param newDisplayZoomBounds - where the start point offset is x,y and the area is width, height
	 */
	public void setDisplayZoomBounds(Rectangle newDisplayZoomBounds) {
		// iterate children 
		for (String recordKey : this.keySet()) {
			this.get(recordKey).setZoomBounds(newDisplayZoomBounds);
		}
	}

	/**
	 * set the scope size to record set to enable to display only the last size records point (scope mode)
	 * recordZoomOffset must be calculated each graphics refresh
	 * @param newScopeSize number of points shown
	 */
	public void setScopeSizeRecordPoints(int newScopeSize) {
		this.isScopeMode = true;
		this.scopeModeSize = newScopeSize;
	}
	
	/**
	 * query if the record set is in scope mode
	 */
	public boolean isScopeMode() {
		return this.isScopeMode;
	}
	
	/**
	 * query actual scope mode offset in record points
	 * @return scopeModeOffset
	 */
	public int getScopeModeOffset() {
		return this.scopeModeOffset;
	}
	
	/**
	 * set a new scope mode offset in record points
	 * @param newScopeModeOffset
	 */
	public void setScopeModeOffset(int newScopeModeOffset) {
		this.scopeModeOffset = newScopeModeOffset;
	}

	/**
	 * @return the scope mode size in record points
	 */
	public int getScopeModeSize() {
		return this.scopeModeSize;
	}

	/**
	 * @return the display start time in msec
	 */
	public double getStartTime() {
		double startTime = 0;
		if (this.isZoomMode) {
			startTime = this.get(0).zoomTimeOffset;
		}
		else if (this.isScopeMode) {
			startTime = this.timeStep_ms.getTime_ms(this.scopeModeOffset+1);
		}
		return startTime;
	}

	/**
	 * @return the isPanMode, panning is only possible in zoom mode
	 */
	public boolean isPanMode() {
		return this.isZoomMode;
	}

	public void shift(int xPercent, int yPercent) {
		// iterate children and set min/max values
		for (String recordKey : this.recordNames) {
			Record record = this.get(recordKey);
			double xShift_ms = record.drawTimeWidth * xPercent / 100;
			if (record.zoomTimeOffset + xShift_ms <= 0) {
				record.zoomOffset = 0;
				record.zoomTimeOffset = 0.0;
			}
			else if (record.zoomTimeOffset + record.drawTimeWidth + xShift_ms > record.getMaxTime_ms()) {
				record.zoomTimeOffset = record.getMaxTime_ms() - record.drawTimeWidth;
				record.zoomOffset = record.findBestIndex(record.zoomTimeOffset);
			}
			else {
				record.zoomTimeOffset = record.zoomTimeOffset + xShift_ms;
				record.zoomOffset = record.findBestIndex(record.zoomTimeOffset);
			}
			
			double yShift = (record.getMaxScaleValue() - record.getMinScaleValue()) * yPercent / 100;
			record.minZoomScaleValue = record.getMinScaleValue() + yShift;
			record.maxZoomScaleValue = record.getMaxScaleValue() + yShift;
		}
	}

	/**
	 * @return the gridType
	 */
	public int getTimeGridType() {
		return this.timeGridType;
	}

	/**
	 * @param gridType the gridType to set
	 */
	public void setTimeGridType(int gridType) {
		this.timeGridType = gridType;
	}

	/**
	 * @return the timeGrid
	 */
	public Vector<Integer> getTimeGrid() {
		return this.timeGrid;
	}

	/**
	 * @param newTimeGrid the timeGrid to set
	 */
	public void setTimeGrid(Vector<Integer> newTimeGrid) {
		this.timeGrid = new Vector<Integer>(newTimeGrid);
	}

	/**
	 * @return the colorTimeGrid
	 */
	public Color getColorTimeGrid() {
		return this.timeGridColor;
	}

	/**
	 * @param colorTimeGrid the colorTimeGrid to set
	 */
	public void setTimeGridColor(Color colorTimeGrid) {
		this.timeGridColor = colorTimeGrid;
	}

	/**
	 * @return the lineStyleTimeGrid
	 */
	public int getLineStyleTimeGrid() {
		return this.timeGridLineStyle;
	}

	/**
	 * @param lineStyleTimeGrid the lineStyleTimeGrid to set
	 */
	public void setTimeGridLineStyle(int lineStyleTimeGrid) {
		this.timeGridLineStyle = lineStyleTimeGrid;
	}

	/**
	 * @return the configuredDisplayable
	 */
	public int getConfiguredDisplayable() {
		return this.configuredDisplayable;
	}

	/**
	 * @param newConfiguredDisplayableNumber the configuredDisplayable to set
	 */
	public void setConfiguredDisplayable(int newConfiguredDisplayableNumber) {
		log.log(Level.FINE, "configuredDisplayable = " + newConfiguredDisplayableNumber); //$NON-NLS-1$
		this.configuredDisplayable = newConfiguredDisplayableNumber;
	}
	
	/**
	 * @return the channe/configuration number
	 */
	public int getChannelConfigNumber() {
		return this.parent.number;
	}

	/**
	 * @return the channel (or) configuration name
	 */
	public String getChannelConfigName() {
		return this.parent.channelConfigName;
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
	 * get the record name to decide for horizontal grid lines 
	 * @param isSyncRecordIncluded while building the horizontal grid vector the scale of the syncRecord must be used
	 * @return the horizontalGridRecord
	 */
	public String getHorizontalGridRecordName(boolean isSyncRecordIncluded) {
		String gridRecordName = this.horizontalGridRecordOrdinal == -1 || this.horizontalGridRecordOrdinal > this.getRecordNames().length-1
		? OSDE.STRING_DASH : this.getRecordNames()[this.horizontalGridRecordOrdinal];
		if (this.isCompareSet) {
			gridRecordName = this.realSize() == 0 ? OSDE.STRING_DASH : this.getFirstRecordName();
			log.log(Level.FINE, "gridRecordName = " + gridRecordName);
		}
		if (this.isSyncRequested && isSyncRecordIncluded) {
			for (String syncableRecordName : this.syncableRecords) {
				if (gridRecordName.equals(syncableRecordName)) {
					gridRecordName = this.getSyncableName();
				}
			}
		}			
		return gridRecordName;
	}

	/**
	 * @return the horizontalGridRecord ordinal
	 */
	public int getHorizontalGridRecordOrdinal() {
		return this.horizontalGridRecordOrdinal;
	}

	/**
	 * @param newHorizontalGridRecordOrdinal of the horizontal grid record name to set
	 */
	public void setHorizontalGridRecordOrdinal(int newHorizontalGridRecordOrdinal) {
		this.horizontalGridRecordOrdinal = newHorizontalGridRecordOrdinal;
	}

	/**
	 * @return the isRecalculation
	 */
	public boolean isRecalculation() {
		return this.isRecalculation;
	}

	@Deprecated
	public int getRecordNamesLength() {
		return (this.getSyncableName().equals("") ? this.recordNames.length : this.recordNames.length-1);
	}

	public String getFirstRecordName() {
		return this.recordNames[0];
	}

	/**
	 * set if a recalculation of depending calculated records are required
	 */
	public void setRecalculationRequired() {
		this.isRecalculation = true;
		for (int i = 0; i < this.device.getMeasurementNames(this.parent.number).length && i < this.getRecordNames().length; ++i) {
			if (this.device.getMeasurement(this.parent.number, i).isCalculation()) {
				this.get(i).resetMinMax();
			}
			this.get(i).resetStatiticCalculationBase();
		}
	}

	/**
	 * @return the device
	 */
	public IDevice getDevice() {
		return this.device != null ? this.device : this.application.getActiveDevice();
	}

	/**
	 * get all record properties in serialized form
	 * @return serializedRecordProperties
	 */
	public String getSerializeProperties() {
		StringBuilder sb = new StringBuilder();

		sb.append(TIME_STEP_MS).append(OSDE.STRING_EQUAL).append(this.timeStep_ms.isConstant ? this.getAverageTimeStep_ms() : -1).append(Record.DELIMITER);

		sb.append(TIME_GRID_TYPE).append(OSDE.STRING_EQUAL).append(this.timeGridType).append(Record.DELIMITER);
		sb.append(TIME_GRID_LINE_STYLE).append(OSDE.STRING_EQUAL).append(this.timeGridLineStyle).append(Record.DELIMITER);
		sb.append(TIME_GRID_COLOR).append(OSDE.STRING_EQUAL).append(this.timeGridColor.getRed()).append(OSDE.STRING_COMMA).append(this.timeGridColor.getGreen()).append(OSDE.STRING_COMMA).append(
				this.timeGridColor.getBlue()).append(Record.DELIMITER);

		sb.append(HORIZONTAL_GRID_RECORD_ORDINAL).append(OSDE.STRING_EQUAL).append(this.horizontalGridRecordOrdinal).append(Record.DELIMITER);
		sb.append(HORIZONTAL_GRID_TYPE).append(OSDE.STRING_EQUAL).append(this.horizontalGridType).append(Record.DELIMITER);
		sb.append(HORIZONTAL_GRID_LINE_STYLE).append(OSDE.STRING_EQUAL).append(this.horizontalGridLineStyle).append(Record.DELIMITER);
		sb.append(HORIZONTAL_GRID_COLOR).append(OSDE.STRING_EQUAL).append(this.horizontalGridColor.getRed()).append(OSDE.STRING_COMMA).append(this.horizontalGridColor.getGreen())
				.append(OSDE.STRING_COMMA).append(this.horizontalGridColor.getBlue()).append(Record.DELIMITER);
		
		sb.append(SYNC_RECORD_SELECTED).append(OSDE.STRING_EQUAL).append(this.isSyncRecordSelected).append(Record.DELIMITER);

		sb.append(VOLTAGE_LIMITS).append(OSDE.STRING_EQUAL);
		for (int value : this.voltageLimits) {
			sb.append(value).append(OSDE.STRING_COMMA);
		}	
		sb.deleteCharAt(sb.length()-1);

		return sb.toString().endsWith(Record.DELIMITER) ? sb.substring(0, sb.lastIndexOf(Record.DELIMITER)) : sb.toString();
	}

	/**
	 * set all record properties by given serialized form
	 * @param serializedRecordSetProperties
	 */
	public void setDeserializedProperties(String serializedRecordSetProperties) {
		HashMap<String, String> recordSetProps = StringHelper.splitString(serializedRecordSetProperties, Record.DELIMITER, this.propertyKeys);
		String tmpValue = null;
		try {
			tmpValue = recordSetProps.get(TIME_STEP_MS);
			if (tmpValue != null && tmpValue.length() > 0) this.timeStep_ms = new TimeSteps(Double.parseDouble(tmpValue.trim()));
			
			tmpValue = recordSetProps.get(TIME_GRID_TYPE);
			if (tmpValue != null && tmpValue.length() > 0) this.timeGridType = new Integer(tmpValue.trim()).intValue();
			tmpValue = recordSetProps.get(TIME_GRID_LINE_STYLE);
			if (tmpValue != null && tmpValue.length() > 0) this.timeGridLineStyle = new Integer(tmpValue.trim()).intValue();
			tmpValue = recordSetProps.get(TIME_GRID_COLOR);
			if (tmpValue != null && tmpValue.length() > 5)
				this.timeGridColor = SWTResourceManager.getColor(new Integer(tmpValue.split(OSDE.STRING_COMMA)[0]), new Integer(tmpValue.split(OSDE.STRING_COMMA)[1]), new Integer(tmpValue
						.split(OSDE.STRING_COMMA)[2]));

			// begin depreciated
			tmpValue = recordSetProps.get(HORIZONTAL_GRID_RECORD);
			if (tmpValue != null && tmpValue.length() > 0) {
				for (String recordKey : this.recordNames) {
					if (recordKey.equals(tmpValue)) {
						this.horizontalGridRecordOrdinal = this.getRecord(recordKey).ordinal;
						break;
					}
				}
			}
			// end depreciated
			tmpValue = recordSetProps.get(HORIZONTAL_GRID_RECORD_ORDINAL);
			if (tmpValue != null && tmpValue.length() > 0) {
				try {
					this.horizontalGridRecordOrdinal = new Integer(tmpValue.trim());
				}
				catch (Exception e) {
					this.horizontalGridRecordOrdinal = -1; 
				}
			}
			tmpValue = recordSetProps.get(HORIZONTAL_GRID_TYPE);
			if (tmpValue != null && tmpValue.length() > 0) this.horizontalGridType = new Integer(tmpValue.trim()).intValue();
			tmpValue = recordSetProps.get(HORIZONTAL_GRID_LINE_STYLE);
			if (tmpValue != null && tmpValue.length() > 0) this.horizontalGridLineStyle = new Integer(tmpValue.trim()).intValue();
			tmpValue = recordSetProps.get(HORIZONTAL_GRID_COLOR);
			if (tmpValue != null && tmpValue.length() > 5)
				this.horizontalGridColor = SWTResourceManager.getColor(new Integer(tmpValue.split(OSDE.STRING_COMMA)[0]), new Integer(tmpValue.split(OSDE.STRING_COMMA)[1]), new Integer(tmpValue
						.split(OSDE.STRING_COMMA)[2]));
				
			tmpValue = recordSetProps.get(SYNC_RECORD_SELECTED);
			if (tmpValue != null && tmpValue.length() > 0) this.isSyncRecordSelected = Boolean.valueOf(tmpValue.trim());
			
			tmpValue = recordSetProps.get(VOLTAGE_LIMITS);
			if (tmpValue != null && tmpValue.length() > 0) {
				String[] strVoltageValues = tmpValue.trim().split(OSDE.STRING_COMMA);
				for (int i = 0; i < strVoltageValues.length && i < this.voltageLimits.length; i++) {
					this.voltageLimits[i] = new Integer(strVoltageValues[i].trim());
				}
			}
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
			this.application.openMessageDialogAsync(Messages.getString(MessageIds.OSDE_MSGE0002) + OSDE.STRING_NEW_LINE + e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
	}

	/**
	 * query if the record set is zoomed and the zoomed data extract starts at first data point
	 * @return true if zoom is active and starts at left edge of curve
	 */
	public boolean isCutLeftEdgeEnabled() {
		try {
			return this.application.isRecordSetVisible(GraphicsWindow.TYPE_NORMAL) && this.isZoomMode && (this.get(0).zoomTimeOffset == 0);
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * query if the record set is zoomed and the zoomed data extract ends at last data point
	 * @return true if zoom is active and starts at right edge of curve
	 */
	public boolean isCutRightEdgeEnabled() {
		Record tmpRecord;
		try {
			tmpRecord = this.get(0);
		}
		catch (Exception e) {
			return false;
		}
		return this.application.isRecordSetVisible(GraphicsWindow.TYPE_NORMAL) && this.isZoomMode && (tmpRecord.zoomTimeOffset + tmpRecord.drawTimeWidth >= tmpRecord.getMaxTime_ms());
	}

	/**
	 * @return the unsaveReasons
	 */
	public Vector<String> getUnsaveReasons() {
		return this.unsaveReasons;
	}

	/**
	 * @return the changeCounter
	 */
	public int getChangeCounter() {
		return this.changeCounter;
	}

	/**
	 * find syncable record names, checking for 
	 * <ol>
	 * <li>equal first word in record names "CellVoltage" of CellVoltage 1"
	 * <li>equal measurement units (from device properties)
	 * </ol>
	 * do not care about symbols
	 */
	void check4SyncableRecords() {
		this.potentialSyncableRecords = new Vector<String>();

		for (int i = 0; i < (this.syncableRecords.size() > 0 ? this.recordNames.length-1 : this.recordNames.length); i++) {
			if (this.recordNames.length > 1 
					&& i > 0 
					&& !this.recordNames[i].contains("..") //$NON-NLS-1$ CellVoltage 1..2
					&& this.recordNames[i - 1].split(" ")[0].equals(this.recordNames[i].split(" ")[0]) //$NON-NLS-1$ //$NON-NLS-2$
					&& this.device.getMeasurement(this.parent.number, i - 1).getUnit().equals(this.device.getMeasurement(this.parent.number, i).getUnit())) {
				if (this.potentialSyncableRecords.isEmpty()) {
					this.potentialSyncableRecords.add(this.recordNames[i - 1]);
				}
				this.potentialSyncableRecords.add(this.recordNames[i]);
			}
		}

		log.log(Level.FINER, this.potentialSyncableRecords.toString());
	}

	/**
	 * find syncable and displayable record names and place the record names in a vector 
	 * @param forceRenew - true will recreate syncableRecordsVector
	 */
	public boolean isSyncableDisplayableRecords(boolean forceRenew) {
		if (forceRenew || !this.isSyncableChecked) {
			String oldSyncName = this.getSyncableName();
			if (this.containsKey(oldSyncName)) {
				this.removeRecordName(oldSyncName);
				this.remove(oldSyncName);
			}
			this.syncableRecords = new Vector<String>();
			for (String syncableRecordKey : this.potentialSyncableRecords) {
				Record record = this.get(syncableRecordKey);
				if (record != null && record.isDisplayable && record.size() > 2) this.syncableRecords.add(syncableRecordKey);
			}

			if (!this.syncableRecords.isEmpty()) {
				//create a new record with syncableName to hold the data for drawing the scale
				String syncRecName = this.getSyncableName();
				String symbol = this.get(this.syncableRecords.firstElement()).getSymbol() + ".." + this.syncableRecords.lastElement().split(" ")[1]; //$NON-NLS-1$ //$NON-NLS-2$
				String unit = this.get(this.syncableRecords.firstElement()).getUnit();
				List<PropertyType> properties = new ArrayList<PropertyType>(this.device.getProperties(this.parent.number, this.get(this.syncableRecords.firstElement()).ordinal));
				DeviceConfiguration.addProperty(properties, IDevice.OFFSET, DataTypes.DOUBLE, this.get(this.syncableRecords.firstElement()).getOffset());
				DeviceConfiguration.addProperty(properties, IDevice.FACTOR, DataTypes.DOUBLE, this.get(this.syncableRecords.firstElement()).getFactor());
				DeviceConfiguration.addProperty(properties, IDevice.REDUCTION, DataTypes.DOUBLE, this.get(this.syncableRecords.firstElement()).getReduction());
				Record tmpRecord = new Record(this.device, this.realSize(), syncRecName, symbol, unit, false, new StatisticsType(), properties, 0);
				tmpRecord.isSyncPlaceholder = true;
				tmpRecord.isPositionLeft = this.get(this.syncableRecords.firstElement()).isPositionLeft; // use fist sync record for scale position
				tmpRecord.isVisible = this.isSyncRecordSelected;
				tmpRecord.df = new DecimalFormat("0.00"); //$NON-NLS-1$
				this.put(syncRecName, tmpRecord);
				this.addRecordName(syncRecName);
				this.setSyncRequested(this.isSyncRecordSelected, false);

				this.isSyncableChecked = true;
				log.log(Level.FINER, "syncableRecords = " + this.syncableRecords.toString()); //$NON-NLS-1$
			}
		}

		return !this.syncableRecords.isEmpty();
	}

	/**
	 * method to query syncable record name stem
	 * @return syncable record name stem or empty sting
	 */
	public String getSyncableName() {
		return this.syncableRecords.isEmpty() ? OSDE.STRING_EMPTY : this.syncableRecords.firstElement() + ".." + this.syncableRecords.lastElement().split(OSDE.STRING_BLANK)[1]; //$NON-NLS-1$
	}

	public void syncScaleOfSyncableRecords() {
		// find min/max
		for (String syncableRecordKey : this.syncableRecords) {
			Record record = this.get(syncableRecordKey);
			if (record != null) {
				int tmpMin = record.getMinValue();
				int tmpMax = record.getMaxValue();
				log.log(Level.FINE, syncableRecordKey + " tmpMin = " + tmpMin / 1000.0 + "; tmpMax = " + tmpMax / 1000.0); //$NON-NLS-1$ //$NON-NLS-2$
				if (this.syncMin == 0 && this.syncMax == 0) {
					this.syncMin = tmpMin;
					this.syncMax = tmpMax;
				}
				else {
					if (tmpMin < this.syncMin) this.syncMin = tmpMin;
					if (tmpMax > this.syncMax) this.syncMax = tmpMax;
				}
			}
		}
		log.log(Level.FINE, "syncMin = " + this.syncMin / 1000.0 + "; syncMax = " + this.syncMax / 1000.0); //$NON-NLS-1$ //$NON-NLS-2$

		updateSyncRecordScale();
	}

	/**
	 * update the scale values from sync record 
	 * and update referenced records to enable drawing of curve, set min/max
	 */
	void updateSyncRecordScale() {
		Record syncPlaceholderRecord = this.get(this.getSyncableName());
		if (syncPlaceholderRecord != null) {
			syncPlaceholderRecord.setMinMax(this.syncMin, this.syncMax);
			syncPlaceholderRecord.setScopeMinMax(this.syncMin, this.syncMax);
			syncPlaceholderRecord.minDisplayValue = syncPlaceholderRecord.minZoomScaleValue = this.syncMin / 1000.0;
			syncPlaceholderRecord.maxDisplayValue = syncPlaceholderRecord.maxZoomScaleValue = this.syncMax / 1000.0;

			// update referenced record to enable drawing of curve, set min/max
			for (String syncableRecordKey : this.syncableRecords) {
				Record record = this.get(syncableRecordKey);
				if (record != null) {
					record.minDisplayValue = this.syncMin / 1000.0;
					record.maxDisplayValue = this.syncMax / 1000.0;
					record.setScaleSynced(true);
				}
			}
		}
	}

	/**
	 * update the display scale values, only called for records which have synced scale
	 * setting display values directly requires to have the scale drawn previously, this will set adapted display values
	 */
	public void updateSyncedScaleValues() {
		Record tmpRecord = this.get(this.getSyncableName());
		if (tmpRecord != null) {
			for (String syncedRecordName : this.syncableRecords) {
				Record record = this.get(syncedRecordName);
				if (record != null) {
					record.minDisplayValue = tmpRecord.minDisplayValue;
					record.maxDisplayValue = tmpRecord.maxDisplayValue;
				}
			}
		}
	}

	/**
	 * @return true only if both isSyncableSynced && isOneSyncableVisible are true
	 */
	public boolean isSyncableSynced() {
		return this.isSyncRequested && isOneSyncableVisible();
	}

	/**
	 * query if one of the syncable records is switched visible
	 * @return true if one of the syncable records is visible
	 */
	public boolean isOneSyncableVisible() {
		boolean isOneSynceableVisible = false;
		for (String syncableRecordKey : this.potentialSyncableRecords) {
			Record record = this.get(syncableRecordKey);
			if (record != null && record.isVisible) {
				isOneSynceableVisible = true;
				break;
			}
		}
		return isOneSynceableVisible;
	}

	/**
	 * synchronize the scales of all syncable records
	 * @param enable the isSyncableSynced to set
	 * @param countAsChange 
	 */
	public void setSyncRequested(boolean enable, boolean countAsChange) {
		this.isSyncRequested = enable;
 
		if (countAsChange)
			this.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
		
		if (enable) {
			this.syncScaleOfSyncableRecords();
		}
		else {
			for (String recordName : this.syncableRecords) {
				this.get(recordName).setScaleSynced(false);
			}
		}
		this.get(this.getSyncableName()).isVisible = this.isSyncRequested();
	}

	/**
	 * overwritten size method to care about added sync record (placeholder)
	 */
	public int size() {
		return this.syncableRecords.isEmpty() ? super.size() : super.size() - 1;
	}
	
	public int realSize() {
		return super.size();
	}

	/**
	 * @return the isSyncRequested
	 */
	public boolean isSyncRequested() {
		return this.isSyncRequested;
	}

	/**
	 * @return the syncableRecords
	 */
	public Vector<String> getSyncableRecords() {
		return this.syncableRecords;
	}
	
	/**
	 * query if the given record key is one of syncable records
	 * @param queryRecordKey the record key to be used for the query
	 * @return true if syncable records contains queryRecordKey
	 */
	public boolean isOneOfSyncableRecord(String queryRecordKey) {
		return this.syncableRecords.contains(queryRecordKey);
	}

	/**
	 * @return the fileDataSize, number of integer values per record
	 */
	public int getFileDataSize() {
		return this.fileDataSize;
	}

	/**
	 * @return the fileDataPointer, file pointer where the data of this record begins
	 */
	public long getFileDataPointer() {
		return this.fileDataPointer;
	}

	/**
	 * @return the fileDataBytes, number of bytes containing all records data 
	 */
	public int getFileDataBytesSize() {
		return this.fileDataBytes;
	}


	/**
	 * @param newFileDataPointer the file data pointer to set, seek point to read data
	 * @param newFileRecordDataSize the file record size to set
	 * @param newFileRecordSetDataBytes the file data bytes to set
	 */
	public void setFileDataPointerAndSize(long newFileDataPointer, int newFileRecordDataSize, int newFileRecordSetDataBytes) {
		this.fileDataPointer = newFileDataPointer;
		this.fileDataSize = newFileRecordDataSize;
		this.fileDataBytes = newFileRecordSetDataBytes;
	}

	/**
	 * @return the hasDisplayableData
	 */
	public boolean hasDisplayableData() {
		return this.hasDisplayableData;
	}

	/**
	 * @param enable the hasDisplayableData to set
	 */
	public void setDisplayableData(boolean enable) {
		this.hasDisplayableData = enable;
	}
	
	/**
	 * load data from file
	 */
	public void loadFileData(String fullQualifiedFileName, boolean doShowProgress) {
		try {
			if (this.fileDataSize != 0 && this.fileDataPointer != 0) {
				if 			(fullQualifiedFileName.endsWith(OSDE.FILE_ENDING_OSD)) OsdReaderWriter.readRecordSetsData(this, fullQualifiedFileName, doShowProgress);
				else if (fullQualifiedFileName.endsWith(OSDE.FILE_ENDING_LOV)) LogViewReader.readRecordSetsData(this, fullQualifiedFileName, doShowProgress);
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(),e);
			this.application.openMessageDialog(e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage()); 
		}
	}

	/**
	 * @return the xScale
	 */
	public int getXScale() {
		return this.xScaleStep;
	}

	/**
	 * @param value the x scale step to set
	 */
	public void setXScale(int value) {
		this.xScaleStep = value;
	}

	/**
	 * @return the voltageLimits for LiXx cells if enabled
	 */
	public int[] getVoltageLimits() {
		return this.voltageLimits;
	}

	/**
	 * set the voltage limits to set for battery cells if enabled
	 */
	public void setVoltageLimits() {
		this.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
		this.voltageLimits = CellVoltageValues.getVoltageLimits();
	}

	/**
	 * @param enable the isScopeMode 
	 */
	public void setScopeMode(boolean enable) {
		this.isScopeMode = enable;
		if (enable) {
			// iterate children and set min/max values
			for (String recordKey : this.recordNames) {
				//StringBuilder sb = new StringBuilder();
				Record record = this.get(recordKey);
				if (record.isVisible && record.isDisplayable){
					int min = 0, max = 0, value;
					for (int i = this.scopeModeOffset; i < record.realSize(); i++) {
						value = record.realGet(i);
						if (i == this.scopeModeOffset) 
							min = max = value;
						else {
							if 			(value > max) max = value;
							else if (value < min) min = value;						
						}
						//sb.append(value).append(", ");
					}	
					//log.log(Level.INFO, sb.toString());
					log.log(Level.FINE, record.getName() + ": scopeMin = " + min / 1000.0 + "; scopeMax = " + max / 1000.0); //$NON-NLS-1$ //$NON-NLS-2$
					record.setScopeMinMax(min, max);
				}
			}
			if (this.isSyncRequested) {
				this.syncMin = this.syncMax = 0;
				this.syncScaleOfSyncableRecords();
			}
		}
	}

	/**
	 * @param enable the isSyncRecordSelected to set
	 */
	public void setSyncRecordSelected(boolean enable) {
		this.isSyncRecordSelected = enable;
	}
	
	/**
	 * synchronize the scale number format for compare set only
	 * @param sourceRecordKey
	 * @param newNumberFormat
	 */
	public void syncScaleNumberFormat(String sourceRecordKey, int newNumberFormat) {
		if (this.isCompareSet) {
			Record srcRecord = this.get(sourceRecordKey);
			for (String tmpRecordKey : this.keySet()) {
				Record tmpRecord = this.get(tmpRecordKey);
				if (!tmpRecordKey.equals(sourceRecordKey)) {
					tmpRecord.df = (DecimalFormat)srcRecord.df.clone();
				}
			}
		}
	}
}
