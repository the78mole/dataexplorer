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

import java.text.DateFormat;
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
import osde.ui.OpenSerialDataExplorer;
import osde.utils.TimeLine;

/**
 * DeviceRecords class holds all the data records for the configured measurement
 * @author Winfried Brügmann
 */
public class RecordSet extends HashMap<String, Record> {
	private static final long							serialVersionUID			= 26031957;
	private static Logger									log										= Logger.getLogger(RecordSet.class.getName());
	
	private String												name;																																// 1)Flugaufzeichnung, 2)Laden, 3)Entladen, ..
	private final String									channelName;
	private final OpenSerialDataExplorer	application;																													// pointer to main application
	private final Channels								channels;
	private String[]											recordNames;																													// Spannung, Strom, ..
	private int														timeStep_ms						= 0;																						// Zeitbasis der Messpunkte
	private static final DateFormat				dateFormat						= new SimpleDateFormat("HH:mm:ss");
	private String												recordSetDescription	= dateFormat.format(new Date().getTime());
	private boolean												isSaved								= false;																				// indicates if the record set is saved to file
	private boolean												isRaw									= false;																				// indicates imported file with raw data, no translation at all
	private boolean												isFromFile						= false;																				// indicates that this record set was created by loading data from file
	private boolean												isRecalculation				= false;																				// indicates record is modified and need re-calculation
	private Rectangle											drawAreaBounds;
	private final DecimalFormat						df = new DecimalFormat("0.000");

	private Thread												dataTableCalcThread;
	private Vector<Vector<Integer>>				dataTable;
	private boolean												isTableDataCalculated = false;  // value to manage only one time calculation
	private boolean												isTableDisplayable		= true;		// value to suppress table data calculation(live view)
	
	//in compare set x min/max and y max (time) might be different
	private boolean												isCompareSet					= false;
	private int														maxSize								= 0;																						// number of data point * time step = total time
	private double												maxValue							= -20000;
	private double												minValue 							= 20000;										// min max value
	
	//zooming
	private int 													zoomLevel 						= 0; // 0 == not zoomed
	private boolean 											isZoomMode = false;
	private int														recordZoomOffset;
	private int														recordZoomSize;
	
	// measurement
	private String 												recordKeyMeasurement;
	
	public static final String 						TIME 									= "time";
	public static final String						TIME_GRID_STATE				= "RecordSet_timeGridState";
	public static final String						TIME_GRID_COLOR				= "RecordSet_timeGridColor";
	public static final String						TIME_GRID_LINE_STYLE	= "RecordSet_timeGridLineStyle";
	public static final int								TIME_GRID_NONE				= 0;																						// no time grid
	public static final int								TIME_GRID_MAIN				= 1;																						// each main tickmark
	public static final int								TIME_GRID_MOD60				= 2;																						// each mod60 tickmark
	private int														timeGridType					= TIME_GRID_NONE;
	private Vector<Integer>								timeGrid 							= new Vector<Integer>();												// contains the time grid position, updated from TimeLine.drawTickMarks
	private Color													timeGridColor					= OpenSerialDataExplorer.COLOR_GREY;
	private int														timeGridLineStyle			= new Integer(SWT.LINE_DOT);
	
	public static final String						HORIZONTAL_GRID_RECORD			= "RecordSet_horizontalGridRecord";
	public static final String						HORIZONTAL_GRID_STATE				= "RecordSet_horizontalGridState";
	public static final String						HORIZONTAL_GRID_COLOR				= "RecordSet_horizontalGridColor";
	public static final String						HORIZONTAL_GRID_LINE_STYSLE	= "RecordSet_horizontalGridLineStyle";
	public static final int								HORIZONTAL_GRID_NONE				= 0;																	// no time grid
	public static final int								HORIZONTAL_GRID_EVERY				= 1;																	// each main tickmark
	public static final int								HORIZONTAL_GRID_SECOND			= 2;																	// each main tickmark
	private int														horizontalGridType					= HORIZONTAL_GRID_NONE;
	private Vector<Integer>								horizontalGrid 							= new Vector<Integer>();							// contains the time grid position, updated from TimeLine.drawTickMarks
	private Color													horizontalGridColor					= OpenSerialDataExplorer.COLOR_GREY;
	private int														horizontalGridLineStyle			= new Integer(SWT.LINE_DASH);
	private String												horizontalGridRecordKey			= "-";																	// recordNames[horizontalGridRecord]

	private int														configuredDisplayable = 0;  // number of record which must be displayable before table calculation begins
	
	/**
	 * data buffers according the size of given names array, where
	 * the name is the key to access the data buffer
	 * @param newChannelName the channel name or configuration name
	 * @param newName for the records like "1) Laden" 
	 * @param newRecordNames string array of the device supported records
	 * @param newTimeStep_ms time in msec of device measures points
	 * @param isRawValue specified if dependent values has been calculated
	 * @param isFromFileValue specifies if the data are red from file and if not modified don't need to be saved
	 * @param initialCapacity thie initial size of the data hash map
	 */
	public RecordSet(String newChannelName, String newName, String[] newRecordNames, int newTimeStep_ms, boolean isRawValue, boolean isFromFileValue, int initialCapacity) {
		super(initialCapacity);
		this.channelName = newChannelName;
		this.name = newName;
		this.recordNames = newRecordNames.clone();
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
	 * @param newRecordNames string array of the device supported records
	 * @param newTimeStep_ms time in msec of device measures points
	 * @param isRawValue specified if dependent values has been calculated
	 * @param isFromFileValue specifies if the data are red from file and if not modified don't need to be saved
	 */
	public RecordSet(String newChannelName, String newName, String[] newRecordNames, int newTimeStep_ms, boolean isRawValue, boolean isFromFileValue) {
		super();
		this.channelName = newChannelName;
		this.name = newName;
		this.recordNames = newRecordNames.clone();
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
	public RecordSet(String newChannelName, String newName, int newTimeStep_ms, boolean isRawValue, boolean isCompareSetValue) {
		super();
		this.channelName = newChannelName;
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
	 * copy constructor
	 * @param recordSet
	 * @param newChannelName
	 */
	private RecordSet(RecordSet recordSet, String newChannelName) {
		super(recordSet);

		this.name = recordSet.name;
		this.application = recordSet.application;
		this.channels = recordSet.channels;
		this.channelName = newChannelName;
		
		for (String recordKey : this.keySet()) {
			this.get(recordKey).setChannelConfigKey(newChannelName);
		}
		String[] oldRecordNames = recordSet.recordNames;
		String[] newRecordNames = recordSet.get(recordSet.getFirstRecordName()).getDevice().getMeasurementNames(newChannelName);
		for (int i = 0; i < newRecordNames.length; i++) {
			if (!oldRecordNames[i].equals(newRecordNames[i])){
				// add the old record with new name
				this.put(newRecordNames[i], this.getRecord(oldRecordNames[i]).clone(newRecordNames[i]));
				// remove the old record
				this.remove(oldRecordNames[i]);
			}
		}
		// update record names
		this.recordNames = 	newRecordNames.clone();
		
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
	 * @param newChannelName according the new configuration
	 */
	public RecordSet clone(String newChannelName) {
		return new RecordSet(this, newChannelName);
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
	 * @throws Exception 
	 */
	public synchronized void addPoints(int[] points, boolean doUpdate) throws Exception {
		if (points.length == this.recordNames.length) {
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
			throw new Exception("RecordSet.addPoints - points.length != recordNames.length");
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
			else 						dataTableRow.add(this.get(this.recordNames[0]).size() * this.getTimeStep_ms());
			for (int i=0; i<this.recordNames.length; ++i) {
				dataTableRow.add(0);
			}
			this.dataTable.add(dataTableRow);
		}
		else {
			IDevice device = this.get(this.recordNames[0]).getDevice();
			int columnIndex = getRecordIndex(recordkey) + 1; // + time column
			Vector<Integer> tableRow = this.dataTable.get(index);
			if (tableRow != null) {
				tableRow.set(columnIndex, new Double(device.translateValue(this.getChannelName(), recordkey, value)).intValue());
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

	public int getTimeStep_ms() {
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
		Vector<String> activeRecords = new Vector<String>();
		for (String recordKey : this.recordNames) {
			if (this.get(recordKey).isActive() && this.get(recordKey).isVisible()) activeRecords.add(recordKey);
		}
		return activeRecords.toArray(new String[1]);
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
		this.reset();
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
		// assume all channels have the same size
		String[] recordNames = device.getMeasurementNames(channelKey).clone();
		RecordSet newRecordSet = new RecordSet(channelKey, recordName, recordNames, device.getTimeStep_ms(), isRaw, isFromFile, 30);
		for (int i = 0; i < recordNames.length; i++) {
			String recordKey = recordNames[i];
			MeasurementType measurement = device.getMeasurement(channelKey, recordKey);
			Record tmpRecord = new Record(measurement.getName(), measurement.getSymbol(), measurement.getUnit(), measurement.isActive(), measurement.getOffset(), 
					measurement.getFactor(), device.getTimeStep_ms(), 5);

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
				if (this.get(recordName).isPositionLeft() && this.get(recordName).isVisible()) ++value;
				if (recordName.equals(recordKey)) break;
			}
		}
		else {
			for (String recordName : getRecordNames()) {
				if (!this.get(recordName).isPositionLeft() && this.get(recordName).isVisible()) ++value;
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
		super.put(key, record);
		record.setKeyName(key);
		record.setParent(this);
		
		// add key to recordNames[] in case of TYPE_COMPARE_SET
		if(this.isCompareSet)
			this.addRecordName(key);
		return record;
	}

	/**
	 * @param newTimeStep_ms the timeStep_ms to set
	 */
	public void setTimeStep_ms(int newTimeStep_ms) {
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
		this.application.getDevice().makeInActiveDisplayable(this);
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
	 * query the size of record set for time unit calculation
	 * - compare set not zoomed will return the size of the largest record
	 * - normal record set will return the size of the data vector
	 * - zoomed set will return size of zoomOffset + zoomWith
	 * @return the size of data point to calculate the time unit
	 */
	public int getSize() {
		int size;
		if (this.isCompareSet)
			size = this.isZoomMode ? this.recordZoomSize : this.maxSize;
		else
			size = this.get(this.recordNames[0]).size();
		
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
	 * @return the the zoom level
	 */
	public int getZoomLevel() {
		return this.zoomLevel;
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
			this.recordZoomOffset = 0;
			
			if (this.recordNames.length != 0) { // check existens of records, a compare set may have no records
				this.recordZoomSize = this.isCompareSet ? this.getMaxSize() : this.get(this.recordNames[0]).realSize();
				// iterate children and reset min/max values
				for (int i = 0; i < this.recordNames.length; i++) {
					Record record = this.get(this.recordNames[i]);
					record.setMinMaxZoomScaleValues(record.getMinScaleValue(), record.getMaxScaleValue());
				}
			}
		}
		this.isZoomMode = zoomModeEnabled;
	}

	/**
	 * reset the record set in viewpoint of measurement and zooming
	 */
	public void reset() {
		this.setZoomMode(false);
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
		return new Double(1.0 * xPos * this.get(this.recordNames[0]).size() / this.drawAreaBounds.width).intValue();
	}
	
	/**
	 * get the formatted time at given position
	 * @param xPos of the display point
	 * @return string of time value in simple date format HH:ss:mm:SSS
	 */
	public String getDisplayPointTime(int xPos) {
		return TimeLine.getFomatedTime((this.getPointIndexFromDisplayPoint(xPos) + this.recordZoomOffset) * this.getTimeStep_ms());
	}

	public int getStartTime() {
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
	 * @return the channelName
	 */
	public String getChannelName() {
		return this.channelName;
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

				String channelConfigKey = getChannelName();
				int numberRecords = getRecordNamesLength();
				int recordEntries = get(getFirstRecordName()).size();
				int progress = this.application2.getProgressPercentage();

				int maxWaitCounter = 10;
				int sleepTime = numberRecords*recordEntries/100;
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

					IDevice device = get(getFirstRecordName()).getDevice();
					for (int i = 0; i < recordEntries; i++) {
						this.application2.setProgress(new Double(i * progressInterval + progress).intValue());
						Vector<Integer> dataTableRow = new Vector<Integer>(numberRecords + 1); // time as well 
						dataTableRow.add(getTimeStep_ms() * i);
						for (String recordKey : this.recordKeys) {
							dataTableRow.add(new Double(1000.0 * device.translateValue(channelConfigKey, recordKey, get(recordKey).get(i) / 1000.0)).intValue());
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
	public void setRecalculation(boolean newRecalculationValue) {
		this.isRecalculation = newRecalculationValue;
		IDevice device = this.get(this.recordNames[0]).getDevice();
		String channelConfigKey = this.get(this.recordNames[0]).getChannelConfigKey();
		for (String recordKey : this.recordNames) {
			if (device.getMeasurement(channelConfigKey, recordKey).isCalculation())
				this.get(recordKey).resetMinMax();
		}
	}
}
