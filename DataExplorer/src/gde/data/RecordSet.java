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
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import osde.OSDE;
import osde.device.DataTypes;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.device.ObjectFactory;
import osde.device.PropertyType;
import osde.device.StatisticsType;
import osde.exception.DataInconsitsentException;
import osde.io.LogViewReader;
import osde.io.OsdReaderWriter;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.CalculationThread;
import osde.utils.LithiumBatteryValues;
import osde.utils.StringHelper;
import osde.utils.TimeLine;

/**
 * DeviceRecords class holds all the data records for the configured measurement
 * @author Winfried Brügmann
 */
public class RecordSet extends HashMap<String, Record> {
	final static String						$CLASS_NAME 									= RecordSet.class.getName();
	final static long							serialVersionUID							= 26031957;
	final static Logger						log														= Logger.getLogger(RecordSet.class.getName());
	final DecimalFormat						df														= new DecimalFormat("0.000");										//$NON-NLS-1$

	String												name;																																					//1)Flugaufzeichnung, 2)Laden, 3)Entladen, ..
	final String									channelConfigName;
	String												objectKey											= OSDE.STRING_EMPTY;
	String												header												= null;
	String[]											recordNames;																																	//Spannung, Strom, ..
	String[]											noneCalculationRecords 				= new String[0];																// records/measurements which are active or inactive
	double												timeStep_ms										= 0;																						//Zeitbasis der Messpunkte
	String												recordSetDescription					= DESCRIPTION_TEXT_LEAD + StringHelper.getDateAndTime();
	boolean												isSaved												= false;																				//indicates if the record set is saved to file
	boolean												isRaw													= false;																				//indicates imported file with raw data, no translation at all
	boolean												isFromFile										= false;																				//indicates that this record set was created by loading data from file
	boolean												isRecalculation								= true;																					//indicates record is modified and need re-calculation
	int														fileDataSize									= 0; 																						//number of integer values of all active/inactive records
	long													fileDataPointer								= 0; 																						// file pointer where the data of this record begins
	boolean												hasDisplayableData						= false;
	int														xScaleStep										= 0; 																						// steps in x direction to draw the curves, normally 1
	Rectangle											drawAreaBounds;

	// data table
	Thread												waitAllDisplayableThread;
	Thread												dataTableCalcThread;
	int[][]												dataTable;
	boolean												isTableDataCalculated					= false;																				//value to manage only one time calculation
	boolean												isTableDisplayable						= true;																					//value to suppress table data calculation(live view)

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
	int														maxSize												= 0;																						//number of data point * time step = total time
	double												maxValue											= 0;
	double												minValue											= 0;																						//min max value

	//zooming
	int														zoomLevel											= 0;																						//0 == not zoomed
	boolean												isZoomMode										= false;
	int														recordZoomOffset;
	int														recordZoomSize;

	// measurement
	String												recordKeyMeasurement;

	public static final String		DESCRIPTION_TEXT_LEAD					= Messages.getString(MessageIds.OSDE_MSGT0129);

	public static final int				MAX_NAME_LENGTH								= 30;

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
	int														timeGridLineStyle							= new Integer(SWT.LINE_DOT);

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
	int														horizontalGridLineStyle				= new Integer(SWT.LINE_DASH);
	String												horizontalGridRecordKey				= OSDE.STRING_DASH;															// recordNames[horizontalGridRecord]

	int[] 												voltageLimits									= LithiumBatteryValues.getVoltageLimits(); 			// voltage limits for LiXx cells, initial LiPo
	public static final String		VOLTAGE_LIMITS								= "RecordSet_voltageLimits";										// each main tickmark //$NON-NLS-1$		
	
	private final String[]				propertyKeys									= new String[] { TIME_STEP_MS, HORIZONTAL_GRID_RECORD_ORDINAL, HORIZONTAL_GRID_RECORD, TIME_GRID_TYPE, TIME_GRID_LINE_STYLE, TIME_GRID_COLOR, HORIZONTAL_GRID_TYPE,
			HORIZONTAL_GRID_LINE_STYLE, HORIZONTAL_GRID_COLOR, VOLTAGE_LIMITS	};

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
	 * data buffers according the size of given names array, where
	 * the name is the key to access the data buffer
	 * @param newChannelName the channel name or configuration name
	 * @param newName for the records like "1) Laden" 
	 * @param measurementNames array of the device supported measurement names
	 * @param newTimeStep_ms time in msec of device measures points
	 * @param isRawValue specified if dependent values has been calculated
	 * @param isFromFileValue specifies if the data are red from file and if not modified don't need to be saved
	 * @param initialCapacity the initial size of the data hash map
	 */
	public RecordSet(IDevice useDevice, String newChannelName, String newName, String[] measurementNames, double newTimeStep_ms, boolean isRawValue, boolean isFromFileValue, int initialCapacity) {
		super(initialCapacity);
		this.device = useDevice;
		this.channelConfigName = newChannelName;
		this.name = newName;
		this.recordNames = measurementNames.clone();
		this.timeStep_ms = newTimeStep_ms;
		this.application = OpenSerialDataExplorer.getInstance();
		this.isRaw = isRawValue;
		this.isFromFile = isFromFileValue;
		this.channels = Channels.getInstance();

		this.check4SyncableRecords();
	}

	/**
	 * data buffers according the size of given names array, where
	 * the name is the key to access the data buffer
	 * @param newChannelName the channel name or configuration name
	 * @param newName for the records like "1) Laden" 
	 * @param measurementNames  array of the device supported measurement names
	 * @param newTimeStep_ms time in msec of device measures points
	 * @param isRawValue specified if dependent values has been calculated
	 * @param isFromFileValue specifies if the data are red from file and if not modified don't need to be saved
	 */
	public RecordSet(IDevice useDevice, String newChannelName, String newName, String[] measurementNames, double newTimeStep_ms, boolean isRawValue, boolean isFromFileValue) {
		super();
		this.device = useDevice;
		this.channelConfigName = newChannelName;
		this.name = newName;
		this.recordNames = measurementNames.clone();
		this.timeStep_ms = newTimeStep_ms;
		this.application = OpenSerialDataExplorer.getInstance();
		this.isRaw = isRawValue;
		this.isFromFile = isFromFileValue;
		this.channels = Channels.getInstance();

		this.check4SyncableRecords();
	}

	/**
	 * data buffers according the size of given names array, where
	 * the name is the key to access the data buffer
	 * @param useDevice the device
	 * @param newChannelName the channel name or configuration name
	 * @param newName for the records like "1) Laden" 
	 * @param newTimeStep_ms time in msec of device measures points
	 * @param isRawValue
	 * @param isCompareSetValue
	 */
	public RecordSet(IDevice useDevice, String newChannelName, String newName, double newTimeStep_ms, boolean isRawValue, boolean isCompareSetValue) {
		super();
		this.device = useDevice;
		this.channelConfigName = newChannelName;
		this.name = newName;
		this.recordNames = new String[0];
		this.timeStep_ms = newTimeStep_ms;
		this.application = OpenSerialDataExplorer.getInstance();
		this.isRaw = isRawValue;
		this.isCompareSet = isCompareSetValue;
		this.channels = null;

		this.check4SyncableRecords();
	}

	/**
	 * copy constructor - used to copy a record set to another channel/configuration, 
	 * wherer the configuration comming from the device properties file
	 * @param recordSet
	 * @param newChannelConfiguration
	 */
	private RecordSet(RecordSet recordSet, String newChannelConfiguration) {
		super(recordSet); // hashmap

		this.device = recordSet.device; // this is a reference
		this.name = recordSet.name;
		this.application = recordSet.application;
		this.channels = recordSet.channels;
		this.channelConfigName = newChannelConfiguration;

		// check if there is a miss match of measurement names and correction required
		String[] oldRecordNames = recordSet.recordNames;
		if(log.isLoggable(Level.FINER)) {
			StringBuilder sb = new StringBuilder();
			for (String string : oldRecordNames) {
				sb.append(string).append(" ");
			}
			log.log(Level.FINER, "oldRecordNames = "+ sb.toString());
		}
		String[] newRecordNames = this.device.getMeasurementNames(newChannelConfiguration);
		if(log.isLoggable(Level.FINER)) {
			StringBuilder sb = new StringBuilder();
			for (String string : newRecordNames) {
				sb.append(string).append(" ");
			}
			log.log(Level.FINER, "newRecordNames = "+ sb.toString());
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
			tmpRecord.setChannelConfigKey(newChannelConfiguration);
			tmpRecord.setParent(this);

			tmpRecord.statistics = this.device.getMeasurementStatistic(newChannelConfiguration, i);
			StatisticsType.Trigger tmpTrigger = tmpRecord.statistics.getTrigger();
			tmpRecord.triggerIsGreater = tmpTrigger != null ? tmpTrigger.isGreater() : null;
			tmpRecord.triggerLevel = tmpTrigger != null ? tmpTrigger.getLevel() : null;
			tmpRecord.minTriggerTimeSec = tmpTrigger != null ? tmpTrigger.getMinTimeSec() : null;

			//copy record properties if -> record properties available == name equal 
			if (recordSet.get(this.recordNames[i]) != null)
				tmpRecord.setProperties(recordSet.get(this.recordNames[i]).getProperties());
			else
				tmpRecord.setProperties(this.device.getProperties(newChannelConfiguration, i));
		}

		this.timeStep_ms = recordSet.timeStep_ms;
		this.recordSetDescription = recordSet.recordSetDescription;
		this.isSaved = false;
		this.isRaw = recordSet.isRaw;
		this.isFromFile = recordSet.isFromFile;
		this.drawAreaBounds = recordSet.drawAreaBounds;

		this.dataTable = null;
		this.isTableDataCalculated = false;
		this.isTableDisplayable = recordSet.isTableDisplayable;

		this.isCompareSet = recordSet.isCompareSet;

		this.maxSize = recordSet.maxSize;
		this.maxValue = recordSet.maxValue;
		this.minValue = recordSet.minValue;

		this.zoomLevel = recordSet.zoomLevel;
		this.isZoomMode = recordSet.isZoomMode;
		this.recordZoomOffset = recordSet.recordZoomOffset;
		this.recordZoomSize = recordSet.recordZoomSize;

		this.recordKeyMeasurement = recordSet.recordKeyMeasurement;

		this.timeGridType = recordSet.timeGridType;
		this.timeGrid = new Vector<Integer>(recordSet.timeGrid);
		this.timeGridColor = recordSet.timeGridColor;
		this.timeGridLineStyle = recordSet.timeGridLineStyle;

		this.horizontalGridType = recordSet.horizontalGridType;
		this.horizontalGrid = new Vector<Integer>(recordSet.horizontalGrid);
		this.horizontalGridColor = recordSet.horizontalGridColor;
		this.horizontalGridLineStyle = recordSet.horizontalGridLineStyle;
		this.horizontalGridRecordKey = recordSet.horizontalGridRecordKey;

		this.configuredDisplayable = recordSet.configuredDisplayable;

		this.check4SyncableRecords();
	}

	/**
	 * clone method used to move record sets to other configuration or channel
	 * @param newChannelConfiguration 
	 */
	public RecordSet clone(String newChannelConfiguration) {
		return new RecordSet(this, newChannelConfiguration);
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
		this.channelConfigName = recordSet.channelConfigName;
		this.recordNames = recordSet.recordNames.clone();

		// update child records
		for (String recordKey : this.keySet()) {
			this.put(recordKey, this.get(recordKey).clone(dataIndex, isFromBegin));
		}

		this.timeStep_ms = recordSet.timeStep_ms;
		this.recordSetDescription = recordSet.recordSetDescription;
		this.isSaved = false;
		this.isRaw = recordSet.isRaw;
		this.isFromFile = recordSet.isFromFile;
		this.drawAreaBounds = recordSet.drawAreaBounds;

		this.dataTable = null; // table calculation will create array
		this.isTableDataCalculated = false;
		this.isTableDisplayable = true;

		this.isCompareSet = recordSet.isCompareSet;

		this.maxSize = recordSet.maxSize;
		this.maxValue = recordSet.maxValue;
		this.minValue = recordSet.minValue;

		this.zoomLevel = 0;
		this.isZoomMode = false;
		this.recordZoomOffset = 0;
		this.recordZoomSize = super.size();

		this.recordKeyMeasurement = recordSet.recordKeyMeasurement;

		this.timeGridType = recordSet.timeGridType;
		this.timeGrid = new Vector<Integer>(recordSet.timeGrid);
		this.timeGridColor = recordSet.timeGridColor;
		this.timeGridLineStyle = recordSet.timeGridLineStyle;

		this.horizontalGridType = recordSet.horizontalGridType;
		this.horizontalGrid = new Vector<Integer>(recordSet.horizontalGrid);
		this.horizontalGridColor = recordSet.horizontalGridColor;
		this.horizontalGridLineStyle = recordSet.horizontalGridLineStyle;
		this.horizontalGridRecordKey = recordSet.horizontalGridRecordKey;

		this.configuredDisplayable = recordSet.configuredDisplayable;

		this.check4SyncableRecords();
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
		log.log(Level.FINE, "targetDisplayable = " + targetDisplayable + " - displayableRecordEntries = " + displayableRecordEntries); //$NON-NLS-1$ //$NON-NLS-2$

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
	 * @param doUpdate to manage display update
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addPoints(int[] points, boolean doUpdate) throws DataInconsitsentException {
		final String $METHOD_NAME = "addPoints";
		if (points.length == this.size()) {
			for (int i = 0; i < points.length; i++) {
				this.getRecord(this.recordNames[i]).add(points[i]);
				
				// check if record synchronisation is activated and update syncMin/syncMax
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
			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < points.length; i++) {
					sb.append(points[i]).append(OSDE.STRING_BLANK);
				}
				log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, sb.toString());
			}
			if (doUpdate) {
				if (isChildOfActiveChannel() && this.equals(this.channels.getActiveChannel().getActiveRecordSet())) {
					this.application.updateGraphicsWindow();
					this.application.updateDigitalWindowChilds();
					this.application.updateAnalogWindowChilds();
				}
			}
		}
		else
			throw new DataInconsitsentException(Messages.getString(MessageIds.OSDE_MSGE0035, new Object[] {this.getClass().getSimpleName(), $METHOD_NAME})); //$NON-NLS-1$
		
		this.hasDisplayableData = true;
	}

	/**
	 * method to add a series of points to none calculation records (records active or inactive)
	 * @param points as int[], where the length must fit records.size()
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addPoints(int[] points) throws DataInconsitsentException {
		final String $METHOD_NAME = "addPoints";
		if (points.length == this.noneCalculationRecords.length) {
			for (int i = 0; i < points.length; i++) {
				this.getRecord(this.noneCalculationRecords[i]).add(points[i]);
			}
			if (log.isLoggable(Level.FINE)) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < points.length; i++) {
					sb.append(points[i]).append(OSDE.STRING_BLANK);
				}
				log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, sb.toString());
			}
		}
		else
			throw new DataInconsitsentException(Messages.getString(MessageIds.OSDE_MSGE0036, new Object[] {this.getClass().getSimpleName(), $METHOD_NAME}));
		
		this.hasDisplayableData = true;
	}

	/**
	 * add a row to the data table where all values are integers multiplied with 1000 to enable 3 decimals
	 * @param dataTableRow
	 */
	public void addDataTableRow(int[] dataTableRow) {
		int numCols = this.size()+1;
		if (numCols == dataTableRow.length) {
			int numRows = getRecordDataSize(true);
			int[][] tmpDataTable = new int[numRows + 1][numCols]; // [existing rows + 1][size+time]
			for (int i = 0; i < numRows; i++) {
				System.arraycopy(this.dataTable[i], 0, tmpDataTable[i], 0, numCols);
			}
			System.arraycopy(this.dataTable[numRows], 0, dataTableRow, 0, numCols);
		}
		else {
			log.log(Level.WARNING, "no data table row added -> numCols != dataTableRow.length");
		}
	}

	/**
	 * query the number of calculated rows of the data table
	 * @return number of rows of the data table
	 */
	public int getNumberDataTableRows() {
		return this.dataTable.length;
	}

	/**
	 * query the record index by given string, if -1 is returned the given name is not found as record name
	 * better use the ordinal of an record
	 * @param recordName
	 * @return record number if valid, else -1
	 */
	@Deprecated
	public int getRecordIndex(String recordName) {
		int searchedNumber = -1;
		for (int i = 0; i < this.recordNames.length; ++i) {
			if (this.recordNames[i].equals(recordName)) {
				searchedNumber = i;
				break;
			}
		}
		return searchedNumber;
	}

	/**
	 * get all calculated and formated data table points of a given index, decimal format is "0.000"
	 * @param index of the data points
	 * @return string array including time
	 */
	public String[] getDataTableRow(int index) {
		int[] tmpValues = this.dataTable[index];
		String[] strValues = new String[tmpValues.length];
		for (int i = 0; i < strValues.length; i++) {
			strValues[i] = this.df.format(tmpValues[i] / 1000.0);
		}
		return strValues;
		//return this.dataTable[index];
	}

	public double getTimeStep_ms() {
		return this.timeStep_ms;
	}

	/**
	 * method to get the sorted record names as array
	 * sorted according list in the device configuration (XML) file
	 * @return String[] containing record names 
	 */
	public String[] getRecordNames() {
		return this.recordNames.clone();
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
			String[] deviceMeasurements = this.device.getMeasurementNames(this.channelConfigName);
			for (int i = 0; i < deviceMeasurements.length; ++i) { // record names may not match device measurements
				MeasurementType measurement = this.device.getMeasurement(this.channelConfigName, i);
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
		this.timeStep_ms = 0;
		this.maxSize = 0;
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

	public String getObjectKey() {
		return this.objectKey;
	}

	public void setObjectKey(String newObjectkey) {
		this.objectKey = newObjectkey;
	}

	/**
	 * method to create a record set with given name "1) Laden" containing records according the active device configuration
	 * @param channelKey (name of the outlet or configuration)
	 * @param recordName
	 * @param device 
	 */
	public static RecordSet createRecordSet(String channelKey, String recordName, IDevice device, boolean isRaw, boolean isFromFile) {
		recordName = recordName.length() <= RecordSet.MAX_NAME_LENGTH ? recordName : recordName.substring(0, RecordSet.MAX_NAME_LENGTH);

		String[] recordNames = device.getMeasurementNames(channelKey);
		if (recordNames.length == 0) { // simple check for valid record names
			channelKey = Channels.getInstance().getChannelNames()[0].split(OSDE.STRING_COLON)[1].trim();
			recordNames = device.getMeasurementNames(channelKey);
		}
		RecordSet newRecordSet = new RecordSet(device, channelKey, recordName, recordNames, device.getTimeStep_ms(), isRaw, isFromFile, recordNames.length);

		for (int i = 0; i < recordNames.length; i++) {
			MeasurementType measurement = device.getMeasurement(channelKey, i);
			Record tmpRecord = new Record(device, i, measurement.getName(), measurement.getSymbol(), measurement.getUnit(), measurement.isActive(), measurement.getStatistics(), measurement.getProperty(), 5);

			// set color defaults
			switch (i) {
			case 0: // erste Kurve
				tmpRecord.setColor(new Color(Display.getCurrent(), 0, 0, 255)); //(SWT.COLOR_BLUE));
				break;
			case 1: // zweite Kurve
				tmpRecord.setColor(new Color(Display.getCurrent(), 0, 128, 0)); //SWT.COLOR_DARK_GREEN));
				break;
			case 2: // dritte Kurve
				tmpRecord.setColor(new Color(Display.getCurrent(), 128, 0, 0)); //(SWT.COLOR_DARK_RED));
				break;
			case 3: // vierte Kurve
				tmpRecord.setColor(new Color(Display.getCurrent(), 255, 0, 255)); //(SWT.COLOR_MAGENTA));
				break;
			case 4: // fünfte Kurve
				tmpRecord.setColor(new Color(Display.getCurrent(), 64, 0, 64)); //(SWT.COLOR_CYAN));
				break;
			case 5: // erste Kurve
				tmpRecord.setColor(new Color(Display.getCurrent(), 0, 128, 128)); //(SWT.COLOR_DARK_YELLOW));
				break;
			case 6: // erste Kurve
				tmpRecord.setColor(new Color(Display.getCurrent(), 128, 128, 0));
				break;
			case 7: // erste Kurve
				tmpRecord.setColor(new Color(Display.getCurrent(), 128, 0, 128));
				break;
			case 8: // erste Kurve
				tmpRecord.setColor(new Color(Display.getCurrent(), 0, 128, 255));
				break;
			case 9: // erste Kurve
				tmpRecord.setColor(new Color(Display.getCurrent(), 128, 255, 0));
				break;
			case 10: // erste Kurve
				tmpRecord.setColor(new Color(Display.getCurrent(), 255, 0, 128));
				break;
			case 11: // erste Kurve
				tmpRecord.setColor(new Color(Display.getCurrent(), 0, 64, 128));
				break;
			case 12: // erste Kurve
				tmpRecord.setColor(new Color(Display.getCurrent(), 64, 128, 0));
				break;
			case 13: // erste Kurve
				tmpRecord.setColor(new Color(Display.getCurrent(), 128, 0, 64));
				break;
			case 14: // erste Kurve
				tmpRecord.setColor(new Color(Display.getCurrent(), 128, 64, 0));
				break;
			case 15: // erste Kurve
				tmpRecord.setColor(new Color(Display.getCurrent(), 0, 128, 64));
				break;
			default:
				tmpRecord.setColor(new Color(Display.getCurrent(), 128, 255, 128)); //(SWT.COLOR_GREEN));
				break;
			}
			// set position defaults
			if (i % 2 == 0) {
				tmpRecord.setPositionLeft(true); //position left
				//				tmpRecord.setPositionNumber(x / 2);
			}
			else {
				tmpRecord.setPositionLeft(false); // position right
				//				tmpRecord.setPositionNumber(x / 2);
			}
			newRecordSet.put(recordNames[i], tmpRecord);
			log.log(Level.FINE, "added record for " + recordNames[i]); //$NON-NLS-1$
		}

		if (log.isLoggable(Level.FINE)) printRecordNames("createRecordSet", newRecordSet.getRecordNames()); //$NON-NLS-1$
		return newRecordSet;
	}

	/**
	 * print record names array
	 * @param recordNames
	 */
	static void printRecordNames(String methodName, String[] recordNames) {
		StringBuilder sb = new StringBuilder().append(methodName + OSDE.STRING_BLANK + OSDE.STRING_NEW_LINE);
		for (int i = 0; i < recordNames.length; ++i) {
			sb.append(recordNames[i]).append(OSDE.STRING_MESSAGE_CONCAT);
		}
		sb.delete(sb.length() - 3, sb.length());
		log.log(Level.FINE, sb.toString());
	}

	/**
	 * calculate the scale axis position as function of available axis at the given side
	 */
	public int getAxisPosition(String recordKey, boolean isLeft) {
		int value = -1;
		if (isLeft) {
			for (String recordName : getRecordNames()) {
				Record tmpRecord = this.get(recordName);
				if (tmpRecord.isPositionLeft && tmpRecord.isVisible && (tmpRecord.isDisplayable && !tmpRecord.isScaleSynced || tmpRecord.isSyncPlaceholder)) ++value;
				if (recordName.equals(recordKey)) break;
			}
		}
		else {
			for (String recordName : getRecordNames()) {
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
		for (String recordKey : this.getRecordNames()) {
			Record record = this.get(recordKey);
			if (record.isVisible && record.isDisplayable && record.isPositionLeft) ++value;
		}
		return value;
	}

	/**
	 * check if this record set is one of the just active channel in UI
	 */
	private boolean isChildOfActiveChannel() {
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
		newRecord.setKeyName(key);
		newRecord.setParent(this);

		// add key to recordNames[] in case of TYPE_COMPARE_SET
		if (this.isCompareSet) this.addRecordName(key);

		return newRecord;
	}

	/**
	 * @param newTimeStep_ms the timeStep_ms to set
	 */
	public void setTimeStep_ms(double newTimeStep_ms) {
		this.timeStep_ms = newTimeStep_ms;
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
	 * @param unsavedReason
	 */
	public void setUnsaved(String unsavedReason) {
		this.changeCounter++;
		this.isSaved = false;
		if (!this.unsaveReasons.contains(unsavedReason)) {
			this.unsaveReasons.add(unsavedReason);
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
		return this.recordSetDescription;
	}

	/**
	 * @param newRecordSetDescription the recordSetDescription to set
	 */
	public void setRecordSetDescription(String newRecordSetDescription) {
		this.recordSetDescription = newRecordSetDescription;
	}

	/**
	 * check if all records from this record set are displayable, starts calcualation if not
	 */
	public void checkAllDisplayable() {
		this.application.getActiveDevice().makeInActiveDisplayable(this);
		this.isRecalculation = false;
		
		// start a low prio tread to load other record set data
		Thread dataLoadThread = new Thread(new Runnable() {
      public void run() {
      	CalculationThread ct = RecordSet.this.device.getCalculationThread();
      	try {
      		while (ct != null && ct.isAlive()) {
      			log.log(Level.FINER, "CalculationThread isAlive");
      			Thread.sleep(1000);
      		}
				}
				catch (InterruptedException e) {
				}
				Channel activeChannel = RecordSet.this.channels.getActiveChannel();
				if (activeChannel != null) activeChannel.checkAndLoadData();
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
		for (String recordKey : this.recordNames) {
			this.get(recordKey).setDisplayable(true);
		}
	}

	/**
	 * force enable all records to be displayed and active if none calculation, this is used for unknown data import or unknown configurations
	 */
	public void setAllVisibleAndDisplayable() {
		for (String recordKey : this.recordNames) {
			Record record = this.get(recordKey);
			record.setVisible(record.isActive());
			record.setDisplayable(!this.device.getMeasurement(this.channelConfigName, record.ordinal).isCalculation());
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
	 * - compare set not zoomed will return the size of the largest record
	 * - normal record set will return the size of the data vector of first active in recordNames
	 * - zoomed set will return size of zoomOffset + zoomWith
	 * @return the size of data point to calculate the time unit
	 */
	public int getRecordDataSize(boolean isReal) {
		int size = 0;
		if (this.isCompareSet) {
			size = this.isZoomMode ? this.recordZoomSize : this.maxSize;
		}
		else {
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
		}
		return size;
	}

	/**
	 * set maximum size of data points of a compare set
	 * @param newMaxSize the maxSize to set
	 */
	public void setMaxSize(int newMaxSize) {
		this.maxSize = newMaxSize;
	}

	/**
	 * get maximum size of data points of a compare set
	 */
	public int getMaxSize() {
		return this.maxSize;
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
		if (!this.isZoomMode) {
			this.resetMeasurement();
			if (this.recordNames.length != 0) { // check existens of records, a compare set may have no records
				this.recordZoomSize = this.isCompareSet ? this.getMaxSize() : this.get(this.recordNames[0]).realSize();
				// iterate children and reset min/max values
				for (int i = 0; i < this.recordNames.length; i++) {
					Record record = this.get(this.recordNames[i]);
					record.setMinMaxZoomScaleValues(record.getMinScaleValue(), record.getMaxScaleValue());
				}
			}
		}
		if (!zoomModeEnabled) { // reset
			this.recordZoomOffset = 0;
		}
		this.isZoomMode = zoomModeEnabled;
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
	 * @param zoomBounds - where the start point offset is x,y and the area is width, height
	 */
	public void setZoomOffsetAndWidth(Rectangle zoomBounds) {
		this.recordZoomOffset = this.getPointIndexFromDisplayPoint(zoomBounds.x) + this.recordZoomOffset;
		this.recordZoomSize = this.getPointIndexFromDisplayPoint(zoomBounds.width);
		// iterate children and set min/max values
		for (String recordKey : this.recordNames) {
			Record record = this.get(recordKey);
			double minZoomScaleValue = record.getDisplayPointValue(zoomBounds.y, this.drawAreaBounds);
			double maxZoomScaleValue = record.getDisplayPointValue(zoomBounds.height + zoomBounds.y, this.drawAreaBounds);
			record.setMinMaxZoomScaleValues(minZoomScaleValue, maxZoomScaleValue);
		}
	}

	public int getRecordZoomOffset() {
		return this.recordZoomOffset;
	}

	public int getRecordZoomSize() {
		return this.recordZoomSize;
	}

	/**
	 * calculate index in data vector from given display point
	 * @param xPos
	 * @return position integer value
	 */
	public int getPointIndexFromDisplayPoint(int xPos) {
		return new Double(1.0 * xPos * this.getRecordDataSize(false) / this.drawAreaBounds.width).intValue();
	}

	/**
	 * get the formatted time at given position
	 * @param xPos of the display point
	 * @return string of time value in simple date format HH:ss:mm:SSS
	 */
	public String getDisplayPointTime(int xPos) {
		return TimeLine.getFomatedTimeWithUnit(new Double((this.getPointIndexFromDisplayPoint(xPos) + this.recordZoomOffset) * this.getTimeStep_ms()).intValue());
	}

	public double getStartTime() {
		return this.isZoomMode ? this.recordZoomOffset * this.timeStep_ms : 0;
	}

	/**
	 * @return the isPanMode
	 */
	public boolean isPanMode() {
		return this.recordZoomOffset != 0 || this.isZoomMode;
	}

	public void shift(int xPercent, int yPercent) {
		int xShift = new Double(1.0 * this.recordZoomSize * xPercent / 100).intValue();
		if (this.recordZoomOffset + xShift <= 0)
			this.recordZoomOffset = 0;
		else if (this.recordZoomOffset + this.recordZoomSize + xShift > this.get(this.recordNames[0]).realSize())
			this.recordZoomOffset = this.get(this.recordNames[0]).realSize() - this.recordZoomSize;
		else
			this.recordZoomOffset = this.recordZoomOffset + xShift;

		// iterate children and set min/max values
		for (String recordKey : this.recordNames) {
			Record record = this.get(recordKey);
			double yShift = (record.getMaxScaleValue() - record.getMinScaleValue()) * yPercent / 100;
			double minZoomScaleValue = record.getMinScaleValue() + yShift;
			double maxZoomScaleValue = record.getMaxScaleValue() + yShift;
			record.setMinMaxZoomScaleValues(minZoomScaleValue, maxZoomScaleValue);
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
	 * @return the channel (or) configuration name
	 */
	public String getChannelConfigName() {
		return this.channelConfigName;
	}

	/**
	 * @return the boolean value true if all table data are calculated and table can be displayed
	 */
	public boolean isTableDataCalculated() {
		return this.isTableDataCalculated;
	}

	/**
	 * @param newValue - boolean value if the table need to be calculated before it can be displayed
	 */
	public void setTableDataCalculated(boolean newValue) {
		if (!newValue) this.dataTable = null;
		this.isTableDataCalculated = newValue;
	}

	/**
	 * @return the boolean value if table can be displayed or displaying is suppressed
	 */
	public boolean isTableDisplayable() {
		return this.isTableDisplayable;
	}

	/**
	 * @param newValue the boolean value if table can be displayed or displaying is suppressed (live view)
	 */
	public void setTableDisplayable(boolean newValue) {
		this.isTableDisplayable = newValue;
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
	 * @return the horizontalGridRecord
	 */
	public String getHorizontalGridRecordName() {
		return this.horizontalGridRecordKey;
	}

	/**
	 * @param newHorizontalGridRecordOrdinal of the horizontal grid record name to set
	 */
	public void setHorizontalGridRecordKey(int newHorizontalGridRecordOrdinal) {
		for (String recordKey : this.recordNames) {
			if (this.get(recordKey).ordinal == newHorizontalGridRecordOrdinal) {
				this.horizontalGridRecordKey = recordKey;
				break;
			}
		}
	}

	/**
	 * @param newHorizontalGridRecordName of the horizontal grid record name to set
	 */
	public void setHorizontalGridRecordKey(String newHorizontalGridRecordName) {
		this.horizontalGridRecordKey = newHorizontalGridRecordName;
	}

	/**
	 * starts a thread executing the dataTable entries
	 */
	public void calculateDataTable() {

		this.dataTableCalcThread = new Thread() {
			final String[]	recordKeys	= getRecordNames();
			final String		sThreadId		= String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$

			public void run() {
				log.log(Level.FINE, "entry data table calculation, threadId = " + this.sThreadId); //$NON-NLS-1$
				//RecordSet.this.application.setStatusMessage(Messages.getString(MessageIds.OSDE_MSGT0133));

				int numberRecords = getRecordNamesLength();
				int recordEntries = getRecordDataSize(true);

				int maxWaitCounter = 10;
				int sleepTime = numberRecords * recordEntries / 200;
				while (!checkAllRecordsDisplayable() && maxWaitCounter > 0) {
					try {
						log.log(Level.FINE, "waiting for all records displayable"); //$NON-NLS-1$
						Thread.sleep(sleepTime);
						--maxWaitCounter;
						if (maxWaitCounter == 0) return;
					}
					catch (InterruptedException e) {
						log.log(Level.SEVERE, e.getMessage(), e);
					}
				}
				log.log(Level.FINE, "all records displayable now, create table, threadId = " + this.sThreadId); //$NON-NLS-1$

				// calculate record set internal data table
				if (log.isLoggable(Level.FINE)) printRecordNames("calculateDataTable", this.recordKeys); //$NON-NLS-1$
				if (!isTableDataCalculated()) {
					log.log(Level.FINE, "start build table entries, threadId = " + this.sThreadId); //$NON-NLS-1$

					long startTime = System.currentTimeMillis();
					RecordSet.this.dataTable = new int[recordEntries][numberRecords+1];
					//RecordSet.this.dataTable = new String[recordEntries][numberRecords+1];
					for (int i = 0; i < recordEntries; i++) {
						RecordSet.this.dataTable[i][0] = new Double(getTimeStep_ms() * i).intValue();					
						//RecordSet.this.dataTable[i][0] = String.format("%.3f", (getTimeStep_ms() * i));
					}
					RecordSet.this.device.prepareDataTable(RecordSet.this, RecordSet.this.dataTable);
					log.log(Level.FINE, "table calcualation time = " + StringHelper.getFormatedTime("ss:SSS", (System.currentTimeMillis() - startTime)));
					
					setTableDataCalculated(true);
					log.log(Level.FINE, "end build table entries, threadId = " + this.sThreadId); //$NON-NLS-1$
				}

				// recall the table update function all prerequisites are checked
				RecordSet.this.application.updateDataTable(RecordSet.this.getName());
			}
		};
		if (!this.dataTableCalcThread.isAlive() && !this.isTableDataCalculated) {
			try {
				this.dataTableCalcThread.start();
			}
			catch (RuntimeException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}

	/**
	 * @return the isRecalculation
	 */
	public boolean isRecalculation() {
		return this.isRecalculation;
	}

	public int getRecordNamesLength() {
		return this.recordNames.length;
	}

	public String getFirstRecordName() {
		return this.recordNames[0];
	}

	/**
	 * set if a recalculation of depending calculated records are required
	 */
	public void setRecalculationRequired() {
		this.isRecalculation = true;
		this.setTableDataCalculated(false);
		for (int i = 0; i < this.device.getMeasurementNames(this.channelConfigName).length; ++i) {
			if (this.device.getMeasurement(this.channelConfigName, i).isCalculation()) this.get(this.getRecordNames()[i]).resetMinMax();

			this.get(this.getRecordNames()[i]).resetStatiticCalculationBase();
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

		sb.append(TIME_STEP_MS).append(OSDE.STRING_EQUAL).append(this.timeStep_ms).append(Record.DELIMITER);

		sb.append(TIME_GRID_TYPE).append(OSDE.STRING_EQUAL).append(this.timeGridType).append(Record.DELIMITER);
		sb.append(TIME_GRID_LINE_STYLE).append(OSDE.STRING_EQUAL).append(this.timeGridLineStyle).append(Record.DELIMITER);
		sb.append(TIME_GRID_COLOR).append(OSDE.STRING_EQUAL).append(this.timeGridColor.getRed()).append(OSDE.STRING_COMMA).append(this.timeGridColor.getGreen()).append(OSDE.STRING_COMMA).append(
				this.timeGridColor.getBlue()).append(Record.DELIMITER);

		sb.append(HORIZONTAL_GRID_RECORD_ORDINAL).append(OSDE.STRING_EQUAL).append(this.get(this.horizontalGridRecordKey)!= null ? this.get(this.horizontalGridRecordKey).ordinal : -1).append(Record.DELIMITER);
		sb.append(HORIZONTAL_GRID_TYPE).append(OSDE.STRING_EQUAL).append(this.horizontalGridType).append(Record.DELIMITER);
		sb.append(HORIZONTAL_GRID_LINE_STYLE).append(OSDE.STRING_EQUAL).append(this.horizontalGridLineStyle).append(Record.DELIMITER);
		sb.append(HORIZONTAL_GRID_COLOR).append(OSDE.STRING_EQUAL).append(this.horizontalGridColor.getRed()).append(OSDE.STRING_COMMA).append(this.horizontalGridColor.getGreen())
				.append(OSDE.STRING_COMMA).append(this.horizontalGridColor.getBlue()).append(Record.DELIMITER);
		
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
			if (tmpValue != null && tmpValue.length() > 0) this.timeStep_ms = new Double(tmpValue.trim()).doubleValue();

			tmpValue = recordSetProps.get(TIME_GRID_TYPE);
			if (tmpValue != null && tmpValue.length() > 0) this.timeGridType = new Integer(tmpValue.trim()).intValue();
			tmpValue = recordSetProps.get(TIME_GRID_LINE_STYLE);
			if (tmpValue != null && tmpValue.length() > 0) this.timeGridLineStyle = new Integer(tmpValue.trim()).intValue();
			tmpValue = recordSetProps.get(TIME_GRID_COLOR);
			if (tmpValue != null && tmpValue.length() > 5)
				this.timeGridColor = SWTResourceManager.getColor(new Integer(tmpValue.split(OSDE.STRING_COMMA)[0]), new Integer(tmpValue.split(OSDE.STRING_COMMA)[1]), new Integer(tmpValue
						.split(OSDE.STRING_COMMA)[2]));

			tmpValue = recordSetProps.get(HORIZONTAL_GRID_RECORD);
			if (tmpValue != null && tmpValue.length() > 0) this.horizontalGridRecordKey = tmpValue.trim();
			tmpValue = recordSetProps.get(HORIZONTAL_GRID_RECORD_ORDINAL);
			if (tmpValue != null && tmpValue.length() > 0) {
				try {
					int index = new Integer(tmpValue.trim());
					this.horizontalGridRecordKey = (index >= 0 && index < this.recordNames.length) ? this.recordNames[new Integer(tmpValue.trim())] : "";
				}
				catch (Exception e) {
					this.horizontalGridRecordKey = "";
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
		return this.isZoomMode && (this.recordZoomOffset == 0);
	}

	/**
	 * query if the record set is zoomed and the zoomed data extract ends at last data point
	 * @return true if zoom is active and starts at right edge of curve
	 */
	public boolean isCutRightEdgeEnabled() {
		return this.isZoomMode && (this.recordZoomOffset + this.recordZoomSize >= this.get(this.getFirstRecordName()).realSize() - 1);
	}

	public String getHeader() {
		return this.header != null ? this.header : this.name;
	}

	public void setHeader(String newHeader) {
		this.header = newHeader;
		this.application.updateHeaderText(newHeader);
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
	 * find syncable record names, checking for equal first word "CellVoltage 1"
	 * and check for equal unit using device measurement
	 */
	void check4SyncableRecords() {
		this.potentialSyncableRecords = new Vector<String>();

		for (int j = 0; j < this.recordNames.length; j++) {
			if (j > 0 && this.recordNames[j - 1].split(" ")[0].equals(this.recordNames[j].split(" ")[0]) //$NON-NLS-1$ //$NON-NLS-2$
					&& this.device.getMeasurement(this.channelConfigName, j - 1).getUnit().equals(this.device.getMeasurement(this.channelConfigName, j).getUnit())) {
				if (this.potentialSyncableRecords.isEmpty()) {
					this.potentialSyncableRecords.add(this.recordNames[j - 1]);
				}
				this.potentialSyncableRecords.add(this.recordNames[j]);
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
				List<PropertyType> properties = this.device.getProperties(this.channelConfigName, this.get(this.syncableRecords.firstElement()).ordinal);
				addProperty(properties, IDevice.OFFSET, DataTypes.DOUBLE, this.get(this.syncableRecords.firstElement()).getOffset());
				addProperty(properties, IDevice.FACTOR, DataTypes.DOUBLE, this.get(this.syncableRecords.firstElement()).getFactor());
				addProperty(properties, IDevice.REDUCTION, DataTypes.DOUBLE, this.get(this.syncableRecords.firstElement()).getReduction());
				Record tmpRecord = new Record(this.device, this.realSize()+1, syncRecName, symbol, unit, false, new StatisticsType(), properties, 0);
				this.put(syncRecName, tmpRecord);
				tmpRecord.isSyncPlaceholder = true;
				tmpRecord.isPositionLeft = this.get(this.syncableRecords.firstElement()).isPositionLeft; // use fist sync record for scale position
				tmpRecord.isVisible = false;
				tmpRecord.df = new DecimalFormat("0.00"); //$NON-NLS-1$
				this.addRecordName(syncRecName);

				this.isSyncableChecked = true;
				log.log(Level.FINER, "syncableRecords = " + this.syncableRecords.toString());
			}
		}

		return !this.syncableRecords.isEmpty();
	}

	/**
	 * method to query syncable record name stem
	 * @return syncable record name stem or empty sting
	 */
	public String getSyncableName() {
		return this.syncableRecords.isEmpty() ? "" : this.syncableRecords.firstElement() + ".." + this.syncableRecords.lastElement().split(" ")[1]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		Record tmpRecord = this.get(this.getSyncableName());
		if (tmpRecord != null) {
			tmpRecord.setMinMax(this.syncMin, this.syncMax);

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
	 * @param properties
	 * @param propertyKey
	 * @param type
	 * @param value
	 */
	private void addProperty(List<PropertyType> properties, String propertyKey, DataTypes type, double value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(propertyKey);
		newProperty.setType(type);
		newProperty.setValue(OSDE.STRING_EMPTY + value);
		properties.add(newProperty);
	}

	/**
	 * @return true onlx if both isSyncableSynced && isOneSyncableVisible are true
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
	 * @param newValue the isSyncableSynced to set
	 */
	public void setSyncRequested(boolean newValue) {
		this.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
		this.isSyncRequested = newValue;
		if (newValue) {
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
	 * @return 
	 */
	public boolean isOneOfSyncableRecord(String queryRecordKey) {
		return this.syncableRecords.contains(queryRecordKey);
	}

	/**
	 * @return the fileDataSize
	 */
	public int getFileDataSize() {
		return this.fileDataSize;
	}

	/**
	 * @param newFileDataSize the fileDataSize to set
	 */
	public void setFileDataSize(int newFileDataSize) {
		this.fileDataSize = newFileDataSize;
	}

	/**
	 * @return the fileDataPointer
	 */
	public long getFileDataPointer() {
		return this.fileDataPointer;
	}

	/**
	 * @param newFileDataPointer the fileDataPointer to set
	 */
	public void setFileDataPointer(long newFileDataPointer) {
		this.fileDataPointer = newFileDataPointer;
	}

	/**
	 * @param newFileDataPointer the fileDataPointer to set
	 * @param newFileDataSize the fileDataSize to set
	 */
	public void setFileDataPointerAndSize(long newFileDataPointer, int newFileDataSize) {
		this.fileDataPointer = newFileDataPointer;
		this.fileDataSize = newFileDataSize;
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
	public void loadFileData(String fullQualifiedFileName) {
		try {
			if (this.fileDataSize != 0 && this.fileDataPointer != 0) {
				if 			(fullQualifiedFileName.endsWith(OSDE.FILE_ENDING_OSD)) OsdReaderWriter.readRecordSetsData(this, fullQualifiedFileName, true);
				else if (fullQualifiedFileName.endsWith(OSDE.FILE_ENDING_LOV)) LogViewReader.readRecordSetsData(this, fullQualifiedFileName, true);
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
	 * @param newVoltageLimits the voltageLimits to set for LiXx cells if enabled
	 */
	public void setVoltageLimits() {
		this.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
		this.voltageLimits = LithiumBatteryValues.getVoltageLimits();
	}
}
