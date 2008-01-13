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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import osde.device.DataCalculationType;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.menu.MenuToolBar;

/**
 * DeviceRecords class holds all the data records for the configured measurement
 * @author Winfried Brügmann
 */
public class RecordSet extends HashMap<String, Record> {
	private static final long							serialVersionUID			= 26031957;
	private static Logger									log										= Logger.getLogger(RecordSet.class.getName());

	private String												name;																																// 1)Flugaufzeichnung, 2)Laden, 3)Entladen, ..
	private final OpenSerialDataExplorer	application;																													// pointer to main application
	private final Channels								channels;
	private String[]											recordNames;																													// Spannung, Strom, ..
	private int														timeStep_ms						= 0;																						// Zeitbasis der Messpunkte
	private static final DateFormat				dateFormat						= new SimpleDateFormat("HH:mm:ss");
	private String												recordSetDescription	= dateFormat.format(new Date().getTime());
	private boolean												isSaved								= false;																				// indicates if the record set is saved to file
	private boolean												isRaw									= false;																				// indicates imported file with raw data, no translation at all
	private boolean												isFromFile						= false;																				// indicates that this record set was created by loading data from file
	private Rectangle											drawAreaBounds;
	
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

	
	// measure
	private boolean 											isMeasurementMode = false;
	private boolean 											isDeltaMeasurementMode = false;
	private String 												recordKeyMeasurement;

	/**
	 * data buffers according the size of given names array, where
	 * the name is the key to access the data buffer
	 * @param name for the records like "1) Laden" 
	 * @param recordNames string array of the device supported records
	 * @param timeStep_ms time in msec of device measures points
	 */
	public RecordSet(String name, String[] recordNames, int timeStep_ms, boolean isRaw, boolean isFromFile) {
		super();
		this.name = name;
		this.recordNames = recordNames;
		this.timeStep_ms = timeStep_ms;
		this.application = OpenSerialDataExplorer.getInstance();
		this.isRaw = isRaw;
		this.isFromFile = isFromFile;
		this.channels = Channels.getInstance();
	}

	/**
	 * data buffers according the size of given names array, where
	 * the name is the key to access the data buffer
	 * @param name for the records like "1) Laden" 
	 * @param timeStep_ms time in msec of device measures points
	 * @param isRaw
	 * @param isCompareSet
	 */
	public RecordSet(String name, int timeStep_ms, boolean isRaw, boolean isCompareSet) {
		super();
		this.name = name;
		this.recordNames = new String[0];
		this.timeStep_ms = timeStep_ms;
		this.application = OpenSerialDataExplorer.getInstance();
		this.isRaw = isRaw;
		this.isCompareSet = isCompareSet;
		this.channels = null;
	}

	/**
	 * check all records of this record set are displayable
	 * @return true/false	
	 */
	public synchronized boolean checkAllRecordsDisplayable() {
		boolean areDisplayavle = false;
		int displayableRecordEntries = 0;
		for (String recordKey : this.getRecordNames()) {
			if (this.getRecord(recordKey).isDisplayable()) ++displayableRecordEntries;
		}
		log.fine("displayableRecordEntries=" + displayableRecordEntries);

		if (displayableRecordEntries == this.getRecordNames().length) {
			areDisplayavle = true;
		}
		return areDisplayavle;
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
	 */
	public synchronized void addPoints(int[] points, boolean doUpdate) {
		for (int i = 0; i < points.length; i++) {
			this.getRecord(recordNames[i]).add((new Integer(points[i])).intValue());
		}
		if (doUpdate) {
			if (isChildOfActiveChannel() && this.equals(channels.getActiveChannel().getActiveRecordSet())) {
				OpenSerialDataExplorer.display.asyncExec(new Runnable() {
					public void run() {
						application.updateGraphicsWindow();
					}
				});
				application.updateDataTable();
				application.updateDigitalWindowChilds();
			}
		}
	}
	
	/**
	 * get all calculated and formated data points of a given index
	 * @param index of the data points
	 * @param df decimal formatting information
	 * @return string array including time
	 */
	public String[] getDataPoints(int index, DecimalFormat df) {
		IDevice device = this.get(this.recordNames[0]).getDevice();
		String[] values = new String[this.size() + 1];
		values[0] = df.format(new Double(index * this.getTimeStep_ms() / 1000.0));
		for (int i = 1; i < values.length; i++) {
			Record record = this.getRecord(this.recordNames[i-1]);
			values[i] = df.format(new Double(device.translateValue(record.getName(), record.get(index) / 1000.0)));
		}
		return values;
	}

	public int getTimeStep_ms() {
		return timeStep_ms;
	}

	/**
	 * method to get the sorted record names as array
	 * sorted according list in the device ini file
	 * @return String[] containing record names 
	 */
	public String[] getRecordNames() {
		return recordNames;
	}

	/**
	 * method to add an new record name 
	 */
	public void addRecordName(String newRecordName) {
		String[] newRecordNames = new String[recordNames.length + 1];
		System.arraycopy(recordNames, 0, newRecordNames, 0, recordNames.length);
		newRecordNames[recordNames.length] = newRecordName;
		recordNames = newRecordNames;
	}

	/**
	 * method to get the sorted record active names as array
	 * @return String[] containing record names 
	 */
	public String[] getActiveRecordNames() {
		Vector<String> activeRecords = new Vector<String>();
		for (String recordKey : recordNames) {
			if (this.get(recordKey).isActive()) activeRecords.add(recordKey);
		}
		return activeRecords.toArray(new String[1]);
	}

	public void clear() {
		super.clear();
		recordNames = new String[0];
		timeStep_ms = 0;
		maxSize = 0;
		maxValue = -20000;
		minValue = 20000;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * method to create a record set with given name "1) Laden" containing records according the active device configuration
	 * @param recordName
	 * @param device 
	 */
	public static RecordSet createRecordSet(String recordName, IDevice device, boolean isRaw, boolean isFromFile) {
		// assume all channels have the same size
		String[] recordNames = device.getMeasurementNames();
		RecordSet newRecordSet = new RecordSet(recordName, recordNames, device.getTimeStep_ms(), isRaw, isFromFile);
		for (int i = 0; i < recordNames.length; i++) {
			String recordKey = recordNames[i];
			MeasurementType measurement = device.getMeasurementDefinition(recordKey);
			DataCalculationType dataCalculation = measurement.getDataCalculation();
			Record tmpRecord = new Record(measurement.getName(), measurement.getSymbol(), measurement.getUnit(), measurement.isActive(), dataCalculation.getOffset(), dataCalculation.getFactor(), device
					.getTimeStep_ms(), 5);

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
		if (log.isLoggable(Level.FINE)) {
			for (String key : recordNames) {
				log.fine(newRecordSet.get(key).getName());
			}
		}
		return newRecordSet;
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
		Channels channels = Channels.getInstance();
		RecordSet uiRecordSet = channels.getActiveChannel().get(this.name);
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
		return record;
	}

	/**
	 * switch the record set according selection and set applications active channel
	 * @param recordSetName p.e. "1) Laden"
	 */
	public void switchRecordSet(String recordSetName) {
		log.finest("entry - " + recordSetName);
		this.resetAllModes();
		final String recordSetKey = recordSetName;
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				MenuToolBar menuToolBar = application.getMenuToolBar();
				String[] recordSetNames = menuToolBar.updateRecordSetSelectCombo();
				channels.getActiveChannel().setActiveRecordSet(recordSetKey);
				//application.getGraphicsWindow().redrawGrahics();
				application.updateDataTable();
				application.updateDigitalWindow();
				for (int i = 0; i < recordSetNames.length; i++) {
					if (recordSetNames[i].equals(recordSetKey)) {
						menuToolBar.getRecordSelectCombo().select(i);
						log.fine("switching to record set " + recordSetKey + " - list position " + i);
					}
				}
				menuToolBar.updateRecordToolItems();
			}
		});
	}

	/**
	 * @param timeStep_ms the timeStep_ms to set
	 */
	public void setTimeStep_ms(int timeStep_ms) {
		this.timeStep_ms = timeStep_ms;
	}

	/**
	 * @return the isSaved
	 */
	public boolean isSaved() {
		return isSaved;
	}

	/**
	 * @param isSaved the isSaved to set
	 */
	public void setSaved(boolean isSaved) {
		this.isSaved = isSaved;
	}

	/**
	 * @return the isRaw
	 */
	public boolean isRaw() {
		return isRaw;
	}

	/**
	 * @return the recordSetDescription
	 */
	public String getRecordSetDescription() {
		return recordSetDescription;
	}

	/**
	 * @param recordSetDescription the recordSetDescription to set
	 */
	public void setRecordSetDescription(String recordSetDescription) {
		this.recordSetDescription = recordSetDescription;
	}

	/**
	 * check if all records from this record set are displayable, starts calcualation if not
	 */
	public void checkAllDisplayable() {
		application.getDevice().makeInActiveDisplayable(this);
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
		return isFromFile;
	}

	/**
	 * method to refresh data table, graphics canvas and curve selection table
	 */
	public void refreshAll() {
		application.updateGraphicsWindow();
		application.updateDataTable();
		application.updateDigitalWindow();
	}

	/**
	 * query the size of record set for time unit calculation
	 * - compare set not zoomed will return the size of the largest record
	 * - normal record set will return the size of the data vector
	 * - zoomed set will return size of zoomOffset + zoomWith
	 * @return the size of data point to calculate the time unit
	 */
	public int getSize() {
		if (this.isZoomMode)
			return this.recordZoomOffset + this.recordZoomSize;
		else if (this.isCompareSet)
			return maxSize;
		else
			return this.get(this.recordNames[0]).realSize();
	}

	/**
	 * @param maxSize the maxSize to set
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	/**
	 * @return the maxValue
	 */
	public double getMaxValue() {
		return maxValue;
	}

	/**
	 * @param maxValue the maxValue to set
	 */
	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}

	/**
	 * @return the minValue
	 */
	public double getMinValue() {
		return minValue;
	}

	/**
	 * @param minValue the minValue to set
	 */
	public void setMinValue(double minValue) {
		this.minValue = minValue;
	}

	/**
	 * @return the isCompareSet
	 */
	public boolean isCompareSet() {
		return isCompareSet;
	}

	/**
	 * @return the the zoom level
	 */
	public int getZoomLevel() {
		return zoomLevel;
	}

	/**
	 * @param newZoomLevel
	 */
	public void setZoomLevel(int newZoomLevel) {
		this.zoomLevel = newZoomLevel;
		if (newZoomLevel == 0) {
			this.resetAllModes();
		}
	}

	/**
	 * @return the curveBounds, this is the area where curves are drawn
	 */
	public Rectangle getDrawAreaBounds() {
		return drawAreaBounds;
	}

	/**
	 * define the area where curves are drawn (clipping, image)
	 * @param drawAreaBounds the curveBounds to set
	 */
	public void setDrawAreaBounds(Rectangle drawAreaBounds) {
		this.drawAreaBounds = drawAreaBounds;
	}

	/**
	 * @param isMeasurementMode the boolean value to set
	 * @param recordKey the key which record should be measured
	 */
	public void setMeasurementMode(boolean isMeasurementMode, String recordKey) {
		this.isMeasurementMode = isMeasurementMode;
		this.recordKeyMeasurement = recordKey;
		if (isMeasurementMode) {
			this.isZoomMode = false;
			this.isDeltaMeasurementMode = false;
		}
	}

	/**
	 * @param isDeltaMeasurementMode the boolean value to set
	 * @param recordKey the key which record should be measured
	 */
	public void setDeltaMeasurementMode(boolean isDeltaMeasurementMode, String recordKey) {
		this.isDeltaMeasurementMode = isDeltaMeasurementMode;
		this.recordKeyMeasurement = recordKey;
		if (isMeasurementMode) {
			this.isZoomMode = false;
			this.isMeasurementMode = false;
		}
	}
	/**
	 * @return the isMeasurementMode
	 */
	public boolean isMeasurementMode() {
		return isMeasurementMode;
	}

	/**
	 * @param isMeasurementMode the isMeasurementMode to set
	 */
	public void setMeasurementMode(boolean isMeasurementMode) {
		this.isMeasurementMode = isMeasurementMode;
	}

	/**
	 * @return the isDeltaMeasurementMode
	 */
	public boolean isDeltaMeasurementMode() {
		return isDeltaMeasurementMode;
	}

	/**
	 * @param isDeltaMeasurementMode the isDeltaMeasurementMode to set
	 */
	public void setDeltaMeasurementMode(boolean isDeltaMeasurementMode) {
		this.isDeltaMeasurementMode = isDeltaMeasurementMode;
	}

	/**
	 * @return the isZoomMode
	 */
	public boolean isZoomMode() {
		return isZoomMode;
	}

	/**
	 * set the mouse tracker in graphics window active for zoom window selection
	 * @param isZoomMode the isZoomMode to set
	 */
	public void setZoomMode(boolean isZoomMode) {
		this.isZoomMode = isZoomMode;
	}

	/**
	 * @return the recordKeyMeasurement
	 */
	public String getRecordKeyMeasurement() {
		return recordKeyMeasurement;
	}
	
	/**
	 * reset all context adjusted modes to default values
	 */
	public void resetAllModes() {
		this.isZoomMode = false;
		this.isMeasurementMode = false;
		this.isDeltaMeasurementMode = false;
		this.recordZoomSize = this.get(this.recordNames[0]).size();
	}

	/**
	 * @param zoomBounds, where the start point offset is x,y and the area is width, height
	 */
	public void setZoomOffsetAndWidth(Rectangle zoomBounds) {
		this.recordZoomOffset = this.getPointIndexFromDisplayPoint(zoomBounds.x);
		this.recordZoomSize = this.getPointIndexFromDisplayPoint(zoomBounds.width);
		// iterate childs and set min/max values
		for (int i = 0; i < this.recordNames.length; i++) {
			Record record = this.get(this.recordNames[i]);
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
	 * @param drawAreaBounds
	 * @return position integer value
	 */
	public int getPointIndexFromDisplayPoint(int xPos) {
		return new Double(1.0 * xPos * this.get(this.recordNames[0]).realSize() / this.drawAreaBounds.width).intValue();
	}
	
	/**
	 * get the formatted time at given position
	 * @param xPos of the display point
	 * @param drawAreaBounds
	 * @return string of time value in simple date format HH:ss:mm:SSS
	 */
	public String getDisplayPointTime(int xPos) {
		String time = "0";
		long time_ms = this.getPointIndexFromDisplayPoint(xPos) * this.getTimeStep_ms();
		if (time_ms >= 0)
		{
			long	lSeconds = time_ms / 1000;
			time_ms %= 1000;
			long	lMinutes = lSeconds / 60;
			lSeconds %= 60;
			long	lHours = lMinutes / 60;
			lMinutes %= 60;
			time = String.format("%02d:%02d:%02d:%03d", lHours, lMinutes, lSeconds, time_ms);
		}
		return time;
	}

	public int getStartTime() {
		return this.isZoomMode ? this.recordZoomOffset * this.timeStep_ms : 0;
	}
}
