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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import osde.device.DataCalculationType;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.menu.DataToolBar;

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

	//in compare set x min/max and y max (time) might be different
	private boolean												isCompareSet					= false;
	private int														maxSize								= 0;																						// number of data point * time step = total time
	private double												maxValue							= -20000, minValue = 20000;										// min max value

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
			if (isChildOfActiveChannel()) {
				OpenSerialDataExplorer.display.asyncExec(new Runnable() {
					public void run() {
						application.updateGraphicsWindow();
					}
				});
				application.updateDigitalWindowChilds();
			}
		}
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
		final String recordSetKey = recordSetName;
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				DataToolBar dataToolBar = application.getDataToolBar();
				String[] recordSetNames = dataToolBar.updateRecordSetSelectCombo();
				channels.getActiveChannel().setActiveRecordSet(recordSetKey);
				//application.getGraphicsWindow().redrawGrahics();
				application.updateDataTable();
				application.updateDigitalWindow();
				for (int i = 0; i < recordSetNames.length; i++) {
					if (recordSetNames[i].equals(recordSetKey)) {
						dataToolBar.getRecordSelectCombo().select(i);
						log.fine("switching to record set " + recordSetKey + " - list position " + i);
					}
				}
				dataToolBar.updateRecordToolItems();
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
	 * @return the maxSize
	 */
	public int getMaxSize() {
		return maxSize;
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
}
