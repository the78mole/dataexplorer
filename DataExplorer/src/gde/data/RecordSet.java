/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.data;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.exception.DataInconsitsentException;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.StringHelper;
import osde.utils.TimeLine;

/**
 * DeviceRecords class holds all the data records for the configured measurement
 * @author Winfried Brügmann
 */
public class RecordSet extends HashMap<String, Record> {
	static final long							serialVersionUID			= 26031957;
	static Logger									log										= Logger.getLogger(RecordSet.class.getName());
	final DecimalFormat						df 										= new DecimalFormat("0.000");
	
	String												name;														// 1)Flugaufzeichnung, 2)Laden, 3)Entladen, ..
	final String									channelConfigName;
	String[]											recordNames;										// Spannung, Strom, ..
	double												timeStep_ms						= 0;			// Zeitbasis der Messpunkte
	String												recordSetDescription	= new SimpleDateFormat("HH:mm:ss").format(new Date().getTime());
	boolean												isSaved								= false;	// indicates if the record set is saved to file
	boolean												isRaw									= false;	// indicates imported file with raw data, no translation at all
	boolean												isFromFile						= false;	// indicates that this record set was created by loading data from file
	boolean												isRecalculation				= true;		// indicates record is modified and need re-calculation
	Rectangle											drawAreaBounds;
	
	Thread												dataTableCalcThread;
	Vector<Vector<Integer>>				dataTable;
	boolean												isTableDataCalculated = false;  // value to manage only one time calculation
	boolean												isTableDisplayable		= true;		// value to suppress table data calculation(live view)
	
	//in compare set x min/max and y max (time) might be different
	boolean												isCompareSet					= false;
	int														maxSize								= 0;			// number of data point * time step = total time
	double												maxValue							= -20000;
	double												minValue 							= 20000;	// min max value
	
	//zooming
	int 													zoomLevel 						= 0; 			// 0 == not zoomed
	boolean 											isZoomMode = false;
	int														recordZoomOffset;
	int														recordZoomSize;
	
	// measurement
	String 												recordKeyMeasurement;
	
	public static final int				MAX_NAME_LENGTH 			= 30;
	
	public static final String 		TIME_STEP_MS 					= "timeStep_ms";
	public static final String 		TIME 									= "time";
	public static final String		TIME_GRID_TYPE				= "RecordSet_timeGridType";
	public static final String		TIME_GRID_COLOR				= "RecordSet_timeGridColor";
	public static final String		TIME_GRID_LINE_STYLE	= "RecordSet_timeGridLineStyle";
	public static final int				TIME_GRID_NONE				= 0;		// no time grid
	public static final int				TIME_GRID_MAIN				= 1;		// each main tickmark
	public static final int				TIME_GRID_MOD60				= 2;		// each mod60 tickmark
	int														timeGridType					= TIME_GRID_NONE;
	Vector<Integer>								timeGrid 							= new Vector<Integer>();		// contains the time grid position, updated from TimeLine.drawTickMarks
	Color													timeGridColor					= OpenSerialDataExplorer.COLOR_GREY;
	int														timeGridLineStyle			= new Integer(SWT.LINE_DOT);
	
	public static final String		HORIZONTAL_GRID_RECORD			= "RecordSet_horizontalGridRecord";
	public static final String		HORIZONTAL_GRID_TYPE				= "RecordSet_horizontalGridType";
	public static final String		HORIZONTAL_GRID_COLOR				= "RecordSet_horizontalGridColor";
	public static final String		HORIZONTAL_GRID_LINE_STYLE	= "RecordSet_horizontalGridLineStyle";
	public static final int				HORIZONTAL_GRID_NONE				= 0;		// no time grid
	public static final int				HORIZONTAL_GRID_EVERY				= 1;		// each main tickmark
	public static final int				HORIZONTAL_GRID_SECOND			= 2;		// each main tickmark
	int														horizontalGridType					= HORIZONTAL_GRID_NONE;
	Vector<Integer>								horizontalGrid 							= new Vector<Integer>();		// contains the time grid position, updated from TimeLine.drawTickMarks
	Color													horizontalGridColor					= OpenSerialDataExplorer.COLOR_GREY;
	int														horizontalGridLineStyle			= new Integer(SWT.LINE_DASH);
	String												horizontalGridRecordKey			= "-";					// recordNames[horizontalGridRecord]
	
	private final String[] 				propertyKeys = new String[] {TIME_STEP_MS, HORIZONTAL_GRID_RECORD, TIME_GRID_TYPE, TIME_GRID_LINE_STYLE, TIME_GRID_COLOR, HORIZONTAL_GRID_TYPE, HORIZONTAL_GRID_LINE_STYLE, HORIZONTAL_GRID_COLOR};


	int														configuredDisplayable = 0;  // number of record which must be displayable before table calculation begins

	final OpenSerialDataExplorer	application;				// pointer to main application
	final Channels								channels;						// start point of data hierarchy
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
		this.dataTable = new Vector<Vector<Integer>>(); 
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
		this.dataTable = new Vector<Vector<Integer>>(); 
	}

	/**
	 * data buffers according the size of given names array, where
	 * the name is the key to access the data buffer
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
		this.dataTable = new Vector<Vector<Integer>>(); 
	}

	/**
	 * copy constructor - used to copy a record set to another channel/configuration
	 * @param recordSet
	 * @param newChannelConfiguration
	 */
	private RecordSet(RecordSet recordSet, String newChannelConfiguration) {
		super(recordSet);

		this.device = recordSet.device; // this is a reference
		this.name = recordSet.name;
		this.application = recordSet.application;
		this.channels = recordSet.channels;
		this.channelConfigName = newChannelConfiguration;
				
		// update child records to new channel or configuration key and to the new parent
		for (String recordKey : this.keySet()) {
			Record tmpRecord = this.get(recordKey);
			tmpRecord.setChannelConfigKey(newChannelConfiguration);
			tmpRecord.setParent(this);
			tmpRecord.replaceProperties(this.device.getProperties(newChannelConfiguration, recordKey));
		}
		
		// check if there is a miss match of measurement names and correction required
		String[] oldRecordNames = recordSet.recordNames;
		String[] newRecordNames = this.device.getMeasurementNames(newChannelConfiguration);
		for (int i = 0; i < newRecordNames.length; i++) {
			if (!oldRecordNames[i].equals(newRecordNames[i])){
				// add the old record with new name
				this.put(newRecordNames[i], this.getRecord(oldRecordNames[i]).clone(newRecordNames[i]));
				// remove the old record
				this.remove(oldRecordNames[i]);
			}
		}
		this.recordNames = newRecordNames.clone();

		this.timeStep_ms = recordSet.timeStep_ms;
		this.recordSetDescription = recordSet.recordSetDescription;
		this.isSaved = recordSet.isSaved;
		this.isRaw = recordSet.isRaw;
		this.isFromFile = recordSet.isFromFile;
		this.drawAreaBounds = recordSet.drawAreaBounds;

		this.dataTable = new Vector<Vector<Integer>>(recordSet.dataTable);
		this.isTableDataCalculated = recordSet.isTableDataCalculated;
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
	}
	
	/**
	 * overwritten clone method used to move record sets to other configuration or channel
	 * @param newChannelConfiguration 
	 */
	public RecordSet clone(String newChannelConfiguration) {
		return new RecordSet(this, newChannelConfiguration);
	}

	/**
	 * check all records of this record set are displayable
	 * @return true/false	
	 */
	public synchronized boolean checkAllRecordsDisplayable() {
		boolean allDisplayable = false;
		int displayableRecordEntries = 0;
		for (String recordKey : this.recordNames) {
			if (this.getRecord(recordKey).isDisplayable()) ++displayableRecordEntries;
			log.fine(recordKey + " isDiplayable = " + this.getRecord(recordKey).isDisplayable());
		}

		int targetDisplayable = this.configuredDisplayable == 0 ? this.getRecordNames().length : this.configuredDisplayable; 
		log.fine("targetDisplayable = " + targetDisplayable + " - displayableRecordEntries = " + displayableRecordEntries);

		if (displayableRecordEntries == targetDisplayable) {
			allDisplayable = true;
		}
		return allDisplayable;
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
		if (points.length == this.size()) {
			for (int i = 0; i < points.length; i++) {
				Record record = this.getRecord(this.recordNames[i]);
				record.add((new Integer(points[i])).intValue());
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
			throw new DataInconsitsentException("RecordSet.addPoints - points.length != recordNames.length");
	}
	
	/**
	 * add a data point at specified index to the data table
	 * @param recordkey
	 * @param index
	 * @param value
	 */
	public void dataTableAddPoint(String recordkey, int index, int value) {
		log.fine(recordkey + " - " + index + " - " + value);
		if (recordkey.equals(RecordSet.TIME)) {
			Vector<Integer> dataTableRow = new Vector<Integer>(this.size() + 1); // time as well 
			if (value != 0) dataTableRow.add(value);
			else 						dataTableRow.add(new Double(this.getRecordDataSize() * this.getTimeStep_ms()).intValue());
			for (int i=0; i<this.recordNames.length; ++i) {
				dataTableRow.add(0);
			}
			this.dataTable.add(dataTableRow);
		}
		else {
			int columnIndex = getRecordIndex(recordkey) + 1; // + time column
			Vector<Integer> tableRow = this.dataTable.get(index);
			if (tableRow != null) {
				tableRow.set(columnIndex, new Double(this.device.translateValue(this.get(recordkey), value)).intValue());
			}
			else log.log(Level.WARNING, "add time point before adding other values !");
		}
	}

	/**
	 * add a row to the data table where all values are integers multiplied with 1000 to enable 3 decimals
	 * @param dataTableRow
	 */
	public void dataTableAddRow(Vector<Integer> dataTableRow) {
		this.dataTable.add(dataTableRow);
	}
	
	/**
	 * query the number of calculated rows of the data table
	 * @return number of rows of the data table
	 */
	public int getNumberDataTableRows() {
		return this.dataTable.size();
	}
	
	/**
	 * query the record index by given string, if 0 is returned the given name is not found as record name
	 * @param recordName
	 * @return record number
	 */
	public int getRecordIndex(String recordName) {
		int searchedNumber = -1;
		for (int i=0; i<this.recordNames.length; ++i) {
			if (this.recordNames[i].equals(recordName)) {
				searchedNumber = i;
				break;
			}
		}
		return searchedNumber;
	}

	/**
	 * get all calculated and formated data points of a given index, decimal format is "0.000"
	 * @param index of the data points
	 * @return string array including time
	 */
	public String[] getDataPoints(int index) {
		Vector<String> dfValues = new Vector<String>();
		for (Integer integer : this.dataTable.get(index)) {
			dfValues.add(this.df.format(integer/1000.0));
		}
		return dfValues.toArray(new String[this.size() + 1]);
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
		this.recordNames = newRecordNames.clone();
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
		Vector<String> calculationRecords = new Vector<String>();
		for (String recordKey : this.recordNames) {
			MeasurementType measurement = this.device.getMeasurement(this.channelConfigName, recordKey);
			if (!measurement.isCalculation()) { // active or inactive 
				calculationRecords.add(recordKey);
			}
		}
		return calculationRecords.toArray(new String[0]);
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

	/**
	 * method to create a record set with given name "1) Laden" containing records according the active device configuration
	 * @param channelKey (name of the outlet or configuration)
	 * @param recordName
	 * @param device 
	 */
	public static RecordSet createRecordSet(String channelKey, String recordName, IDevice device, boolean isRaw, boolean isFromFile) {
		recordName = recordName.length() <= RecordSet.MAX_NAME_LENGTH ? recordName : recordName.substring(0, RecordSet.MAX_NAME_LENGTH);
		
		String[] recordNames = device.getMeasurementNames(channelKey);
		RecordSet newRecordSet = new RecordSet(device, channelKey, recordName, recordNames, device.getTimeStep_ms(), isRaw, isFromFile, recordNames.length);

		for (int i = 0; i < recordNames.length; i++) {
			String recordKey = recordNames[i];
			MeasurementType measurement = device.getMeasurement(channelKey, recordKey);
			Record tmpRecord = new Record(measurement.getName(), measurement.getSymbol(), measurement.getUnit(), measurement.isActive(), measurement.getProperty(), 5);

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
			if (log.isLoggable(Level.FINE)) log.fine("added record for " + recordNames[i]);
		}
		
		if(log.isLoggable(Level.FINE)) printRecordNames("createRecordSet", newRecordSet.getRecordNames());
		return newRecordSet;
	}

	/**
	 * print record names array
	 * @param recordNames
	 */
	static void printRecordNames(String methodName, String[] recordNames) {
		StringBuilder sb = new StringBuilder().append(methodName + " " + "\n");
		for (int i=0; i<recordNames.length; ++i){
			sb.append(recordNames[i]).append(" - ");
		}
		sb.delete(sb.length()-3, sb.length());
		log.info(sb.toString());
	}

	/**
	 * calculate the scale axis position as function of available axis at the given side
	 */
	public int getAxisPosition(String recordKey, boolean isLeft) {
		int value = -1;
		if (isLeft) {
			for (String recordName : getRecordNames()) {
				Record tmpRecord = this.get(recordName);
				if (tmpRecord.isPositionLeft() && tmpRecord.isVisible() && tmpRecord.isDisplayable()) ++value;
				if (recordName.equals(recordKey)) break;
			}
		}
		else {
			for (String recordName : getRecordNames()) {
				Record tmpRecord = this.get(recordName);
				if (!tmpRecord.isPositionLeft() && tmpRecord.isVisible() && tmpRecord.isDisplayable()) ++value;
				if (recordName.equals(recordKey)) break;
			}
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
		super.put(key, record.clone());
		Record newRecord = this.get(key);
		//for compare set record following properties has to be checked at the point where
		newRecord.setKeyName(key);
		newRecord.setParent(this);
		
		// add key to recordNames[] in case of TYPE_COMPARE_SET
		if(this.isCompareSet)
			this.addRecordName(key);
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
		this.isSaved = newValue;
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
	}

	/**
	 * check if all records from this record set are displayable, starts calcualation if not
	 */
	public void setAllDisplayable() {
		for (String recordKey : this.recordNames) {
			this.get(recordKey).setDisplayable(true);
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
	public int getRecordDataSize() {
		int size = 0;
		if (this.isCompareSet) {
			size = this.isZoomMode ? this.recordZoomSize : this.maxSize;
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
		return new Double(1.0 * xPos * this.getRecordDataSize() / this.drawAreaBounds.width).intValue();
	}
	
	/**
	 * get the formatted time at given position
	 * @param xPos of the display point
	 * @return string of time value in simple date format HH:ss:mm:SSS
	 */
	public String getDisplayPointTime(int xPos) {
		return TimeLine.getFomatedTime(new Double((this.getPointIndexFromDisplayPoint(xPos) + this.recordZoomOffset) * this.getTimeStep_ms()).intValue());
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
		else if(this.recordZoomOffset + this.recordZoomSize + xShift > this.get(this.recordNames[0]).realSize())
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
		log.fine("configuredDisplayable = " + newConfiguredDisplayableNumber);
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
		if (!newValue) this.dataTable = new Vector<Vector<Integer>>(); 
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
	 * @param newHorizontalGridRecordKey the horizontal grid record name to set
	 */
	public void setHorizontalGridRecordKey(String newHorizontalGridRecordKey) {
		this.horizontalGridRecordKey = newHorizontalGridRecordKey;
	}
	
	/**
	 * starts a thread executing the dataTable entries
	 */
	public void calculateDataTable() {

		this.dataTableCalcThread = new Thread() {
			final Logger									logger				= Logger.getLogger(Thread.class.getName());
			final OpenSerialDataExplorer 	application2 	= OpenSerialDataExplorer.getInstance(); 
			final String[] 								recordKeys 		= getRecordNames();
			
			public void run() {
				if (this.logger.isLoggable(Level.FINE)) this.logger.fine("entry data table calculation");
				this.application2.setStatusMessage(" -> berechne Datentabelle");

				int numberRecords = getRecordNamesLength();
				int recordEntries = getRecordDataSize();
				int progress = this.application2.getProgressPercentage();

				int maxWaitCounter = 10;
				int sleepTime = numberRecords*recordEntries/200;
				while (!checkAllRecordsDisplayable() && maxWaitCounter > 0) {
					try {
						this.logger.fine("waiting for all records displayable");
						Thread.sleep(sleepTime);
						--maxWaitCounter;
						if (maxWaitCounter == 0) return;
					}
					catch (InterruptedException e) {
						this.logger.log(Level.SEVERE, e.getMessage(), e);
					}
					this.application2.setProgress(progress+=2);
				}
				if (this.logger.isLoggable(Level.FINE)) this.logger.fine("all records displayable now, create table");

				// calculate record set internal data table
				if (this.logger.isLoggable(Level.FINE)) printRecordNames("calculateDataTable", this.recordKeys);
				if (!isTableDataCalculated()) {
					if (this.logger.isLoggable(Level.FINE)) this.logger.fine("start build table entries");
					double progressInterval = (60.0 - progress) / recordEntries;

					for (int i = 0; i < recordEntries; i++) {
						this.application2.setProgress(new Double(i * progressInterval + progress).intValue());
						Vector<Integer> dataTableRow = new Vector<Integer>(numberRecords + 1); // time as well 
						dataTableRow.add(new Double(getTimeStep_ms() * i).intValue());
						for (String recordKey : this.recordKeys) {
							dataTableRow.add(new Double(1000.0 * RecordSet.this.device.translateValue(get(recordKey), get(recordKey).get(i) / 1000.0)).intValue());
						}
						dataTableAddRow(dataTableRow);
					}
					setTableDataCalculated(true);
					if (this.logger.isLoggable(Level.FINE)) this.logger.fine("end build table entries");
				}
				if (this.logger.isLoggable(Level.FINE)) this.logger.fine("exit data table calculation");
				this.application2.updateDataTable();  // recall the table update function all prerequisites are checked
			}
		};
		this.dataTableCalcThread.start();
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
	 * @param newRecalculationValue the isRecalculation to set
	 */
	public void setRecalculationRequired() {
		this.isRecalculation = true;
		this.setTableDataCalculated(false);
		for (String recordKey : this.recordNames) {
			if (this.device.getMeasurement(this.channelConfigName, recordKey).isCalculation())
				this.get(recordKey).resetMinMax();
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

		sb.append(TIME_STEP_MS).append("=").append(this.timeStep_ms).append(Record.DELIMITER);

		sb.append(TIME_GRID_TYPE).append("=").append(this.timeGridType).append(Record.DELIMITER);
		sb.append(TIME_GRID_LINE_STYLE).append("=").append(this.timeGridLineStyle).append(Record.DELIMITER);
		sb.append(TIME_GRID_COLOR).append("=").append(this.timeGridColor.getRed()).append(",").append(this.timeGridColor.getGreen()).append(",").append(this.timeGridColor.getBlue()).append(Record.DELIMITER);

		sb.append(HORIZONTAL_GRID_RECORD).append("=").append(this.horizontalGridRecordKey).append(Record.DELIMITER);
		sb.append(HORIZONTAL_GRID_TYPE).append("=").append(this.horizontalGridType).append(Record.DELIMITER);
		sb.append(HORIZONTAL_GRID_LINE_STYLE).append("=").append(this.horizontalGridLineStyle).append(Record.DELIMITER);
		sb.append(HORIZONTAL_GRID_COLOR).append("=").append(this.horizontalGridColor.getRed()).append(",").append(this.horizontalGridColor.getGreen()).append(",").append(this.horizontalGridColor.getBlue()).append(Record.DELIMITER);

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
			if (tmpValue!=null && tmpValue.length() > 0) this.timeStep_ms = new Double(tmpValue.trim()).doubleValue();

			tmpValue = recordSetProps.get(TIME_GRID_TYPE);
			if (tmpValue!=null && tmpValue.length() > 0) this.timeGridType = new Integer(tmpValue.trim()).intValue();
			tmpValue = recordSetProps.get(TIME_GRID_LINE_STYLE);
			if (tmpValue!=null && tmpValue.length() > 0) this.timeGridLineStyle = new Integer(tmpValue.trim()).intValue();
			tmpValue = recordSetProps.get(TIME_GRID_COLOR);
			if (tmpValue!=null && tmpValue.length() > 5) this.timeGridColor = SWTResourceManager.getColor(new Integer(tmpValue.split(",")[0]), new Integer(tmpValue.split(",")[1]), new Integer(tmpValue.split(",")[2]));
			
			tmpValue = recordSetProps.get(HORIZONTAL_GRID_RECORD);
			if (tmpValue!=null && tmpValue.length() > 0) this.horizontalGridRecordKey = tmpValue.trim();
			tmpValue = recordSetProps.get(HORIZONTAL_GRID_TYPE);
			if (tmpValue!=null && tmpValue.length() > 0) this.horizontalGridType = new Integer(tmpValue.trim()).intValue();
			tmpValue = recordSetProps.get(HORIZONTAL_GRID_LINE_STYLE);
			if (tmpValue!=null && tmpValue.length() > 0) this.horizontalGridLineStyle = new Integer(tmpValue.trim()).intValue();
			tmpValue = recordSetProps.get(HORIZONTAL_GRID_COLOR);
			if (tmpValue!=null && tmpValue.length() > 5) this.horizontalGridColor = SWTResourceManager.getColor(new Integer(tmpValue.split(",")[0]), new Integer(tmpValue.split(",")[1]), new Integer(tmpValue.split(",")[2]));
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
			this.application.openMessageDialogAsync("Beim laden der Datensatzeigenschaften ist ein Fehler aufgetreten ! \n" + e.getClass().getSimpleName() + " - " + e.getMessage());
		}
	}
	
	/**
	 * query if the record set is zoomed and the zoomed data extract starts at first data point
	 * @return
	 */
	public boolean isCutLeftEdgeEnabled() {
		return this.isZoomMode && (this.recordZoomOffset == 0);
	}
	
	/**
	 * query if the record set is zoomed and the zoomed data extract ends at last data point
	 * @return
	 */
	public boolean isCutRightEdgeEnabled() {
		return this.isZoomMode && (this.recordZoomOffset + this.recordZoomSize == this.getRecordDataSize());
	}

}
