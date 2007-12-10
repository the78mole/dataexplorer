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

import osde.config.DeviceConfiguration;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.menu.DataToolBar;

/**
 * DeviceRecords holds all the data records for the configured measurement
 * units, size is according the device configured record elements (measured and
 * calculated)
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

	// some constants definitions
	public static final String						VOLTAGE								= "Spannung";
	public static final String						HEIGHT								= "Höhe";
	public static final String						SLOPE									= "Steigrate";
	public static final String						CURRENT								= "Strom";
	public static final String						CHARGE								= "Ladung";
	public static final String						POWER									= "Leistung";
	public static final String						ENERGY								= "Energie";

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
	 * @param recordNames string array of the device supported records
	 * @param timeStep_ms time in msec of device measures points
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
	 * 
	 * @param dataName
	 * @return Vector<Integer>
	 */
	public Record getRecord(String recordNameKey) {
		return this.get(recordNameKey);
	}

	/**
	 * method to add a series of points to the associated records
	 * @param in[] points, where the length must fit records.size()
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
				//application.updateDigitalLabelText(new String[] { new Double(points[0] / 1000.0).toString(), new Integer(points[1]).toString() });
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
	 * @param deviceConfig 
	 */
	public static RecordSet createRecordSet(String recordName, DeviceConfiguration deviceConfig, boolean isRaw, boolean isFromFile) {
		int size = deviceConfig.getNumberRecords(); // all channels must have the same size, use channel1
		String[] recordNames = new String[size];
		for (int i = 1; i <= deviceConfig.getNumberRecords(); i++) {
			recordNames[i - 1] = (String) deviceConfig.getConfiguredRecords().get(DeviceConfiguration.MEASUREMENT + i);
			if (log.isLoggable(Level.FINE)) log.fine("- " + recordNames[i - 1]);
		}

		RecordSet newRecordSet = new RecordSet(recordName, recordNames, deviceConfig.getTimeStep_ms(), isRaw, isFromFile);
		for (int k = 1; k <= recordNames.length; k++) {
			Record tmpRecord = new Record(recordNames[k - 1], (String) deviceConfig.getConfiguredRecords().get(DeviceConfiguration.MEASUREMENT_UNIT + k), (String) deviceConfig.getConfiguredRecords().get(
					DeviceConfiguration.MEASUREMENT_SYMBOL + k), (int) deviceConfig.getTimeStep_ms(), (Integer) deviceConfig.getConfiguredRecords().get(DeviceConfiguration.MEASUREMENT_FACTOR + k),
					(Integer) deviceConfig.getConfiguredRecords().get(DeviceConfiguration.MEASUREMENT_OFFSET + k), (Integer) deviceConfig.getConfiguredRecords().get(
							DeviceConfiguration.MEASUREMENT_GAUGE_MAX + k), (Boolean) deviceConfig.getConfiguredRecords().get(DeviceConfiguration.MEASUREMENT_IS_ACTIVE + k), 5);

			int x = k - 1;

			// set color defaults
			switch (x) {
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
			default:
				tmpRecord.setColor(new Color(Display.getCurrent(), 0, 255, 0)); //(SWT.COLOR_GREEN));
				break;
			}
			// set position defaults
			if (x % 2 == 0) {
				tmpRecord.setPositionLeft(true); //position left
				//				tmpRecord.setPositionNumber(x / 2);
			}
			else {
				tmpRecord.setPositionLeft(false); // position right
				//				tmpRecord.setPositionNumber(x / 2);
			}
			newRecordSet.put(recordNames[k - 1], tmpRecord);
			if (log.isLoggable(Level.FINE)) log.fine("added record for " + recordNames[k - 1]);
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
	 * @param channelText assuming p.e. "1) Laden"
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
		application.getDeviceDialog().makeInActiveDisplayable(this);
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
