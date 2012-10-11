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
    
    Copyright (c) 2008,2009,2010,2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.data;

import gde.GDE;
import gde.device.DataTypes;
import gde.device.IDevice;
import gde.device.ObjectFactory;
import gde.device.PropertyType;
import gde.device.StatisticsType;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;
import gde.utils.TimeLine;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * @author Winfried Brügmann
 * class record holds data points of one measurement or line or curve
 */
public class Record extends Vector<Integer> {
	final static String	$CLASS_NAME 					= Record.class.getName();
	final static long		serialVersionUID			= 26031957;
	final static Logger	log										= Logger.getLogger(Record.class.getName());
	
	public static final String DELIMITER 			= "|-|"; 		//$NON-NLS-1$
	public static final String END_MARKER 		= "|:-:|"; 	//$NON-NLS-1$

	// this variables are used to make a record selfcontained within compare set
	String							channelConfigKey; 								// used as channelConfigKey
	String							keyName;
	TimeSteps						timeStep_ms           = null; // timeStep_ms for each measurement point in compare set, where time step of measurement points might be individual
	IDevice							device;
	int									ordinal;	// ordinal is referencing the source position of the record relative to the initial 
																// device measurement configuration and used to find specific properties

	RecordSet						parent;
	String							name;																				// measurement name Höhe
	String							unit;																				// unit [m]
	String							symbol;																			// symbol h
	String							description						= GDE.STRING_BLANK;	// only set if copied into compare set 
	boolean							isActive;
	boolean							isDisplayable;
	boolean							isVisible							= true;
	StatisticsType			statistics						= null;
	Boolean							triggerIsGreater			= null;
	Integer							triggerLevel					= null;
	Integer							minTriggerTimeSec 		= null;
	static class TriggerRange {
		int in  = -1;
		int out = -1;
		public TriggerRange(int newIn, int newOut) { this.in = newIn; this.out = newOut; }
		public TriggerRange(int newIn) { this.in = newIn; }
		/**
		 * @return the in value
		 */
		public int getIn() {
			return this.in;
		}
		/**
		 * @param newIn the in value to set
		 */
		public void setIn(int newIn) {
			this.in = newIn;
		}
		/**
		 * @return the out value
		 */
		public int getOut() {
			return this.out;
		}
		/**
		 * @param newOut the out value to set
		 */
		public void setOut(int newOut) {
			this.out = newOut;
		}
		/**
		 * query if both values has been set
		 */
		public boolean isComplete() {
			return this.in >= 0 && this.out > 0 ? true : false;
		}
	}
	TriggerRange				tmpTriggerRange				= null;
	Vector<TriggerRange> triggerRanges				= null;
	List<PropertyType>	properties						= new ArrayList<PropertyType>();	// offset, factor, reduction, ...
	boolean							isPositionLeft				= true;
	Color								color									= DataExplorer.COLOR_BLACK;
	int									lineWidth							= 1;
	int									lineStyle							= SWT.LINE_SOLID;
	boolean							isRoundOut						= false;
	boolean							isStartpointZero			= false;
	boolean							isStartEndDefined			= false;
	DecimalFormat				df;
	int									numberFormat					= -1;													// -1 = automatic, 0 = 0000, 1 = 000.0, 2 = 00.00
	int									maxValue							= 0;		 										  // max value of the curve
	int									minValue							= 0;													// min value of the curve
	double							maxScaleValue					= this.maxValue;							// overwrite calculated boundaries
	double							minScaleValue					= this.minValue;

	//synchronize
	int									syncMaxValue					= 0;		 										  // max value of the curve if synced
	int									syncMinValue					= 0;													// min value of the curve if synced

	// scope view 
	int									scopeMin							= 0;													// min value of the curve within scope display area
	int									scopeMax							= 0;													// max value of the curve within scope display area

	// statistics trigger
	int									maxValueTriggered			= Integer.MIN_VALUE;		 			// max value of the curve, according a set trigger level if any
	int									minValueTriggered			= Integer.MAX_VALUE;					// min value of the curve, according a set trigger level if any
	int									avgValue							= Integer.MIN_VALUE;		 			// avarage value (avg = sum(xi)/n)
	int									sigmaValue						= Integer.MIN_VALUE;		 			// sigma value of data, according a set trigger level if any
	int									avgValueTriggered			= Integer.MIN_VALUE;		 			// avarage value (avg = sum(xi)/n)
	int									sigmaValueTriggered		= Integer.MIN_VALUE;		 			// sigma value of data, according a set trigger level if any
	
	double							drawLimit_ms					= Integer.MAX_VALUE;		// above this limit the record will not be drawn (compareSet with different records) 
	double							zoomTimeOffset	= 0;		// time where the zoom area begins
	int									zoomOffset			= 0;		// number of measurements point until zoom area begins approximation only
	double							drawTimeWidth		= 0;		// all or zoomed area time width
	double							tmpMaxZoomScaleValue	= this.maxScaleValue;
	double							tmpMinZoomScaleValue	= this.minScaleValue;
	double							maxZoomScaleValue			= this.maxScaleValue;
	double							minZoomScaleValue			= this.minScaleValue;
	int									numberScaleTicks			= 0;

	// display the record
	double							displayScaleFactorTime;
	double							displayScaleFactorValue;
	double							syncMasterFactor			= 1.0;			// synchronized scale and different measurement factors
	double							minDisplayValue;									// min value in device units, correspond to draw area
	double							maxDisplayValue;									// max value in device units, correspond to draw area

	//current drop, make curve capable to be smoothed
	boolean             isCurrentRecord 			= false;
	int             		dropStartIndex 				= 0;
	int             		dropEndIndex 					= 0;
	boolean             dropIndexWritten 			= true;

	// measurement
	boolean							isMeasurementMode				= false;
	boolean							isDeltaMeasurementMode	= false;
		
	public final static String	NAME									= "_name";							// active means this measurement can be red from device, other wise its calculated //$NON-NLS-1$
	public final static String	UNIT									= "_unit";							// active means this measurement can be red from device, other wise its calculated //$NON-NLS-1$
	public final static String	SYMBOL								= "_symbol";						// active means this measurement can be red from device, other wise its calculated //$NON-NLS-1$
	public final static String	IS_ACTIVE							= "_isActive";					// active means this measurement can be red from device, other wise its calculated //$NON-NLS-1$
	public final static String	IS_DIPLAYABLE					= "_isDisplayable";			// true for all active records, true for passive records when data calculated //$NON-NLS-1$
	public final static String	IS_VISIBLE						= "_isVisible";					// defines if data are displayed  //$NON-NLS-1$
	public final static String	IS_POSITION_LEFT			= "_isPositionLeft";		// defines the side where the axis id displayed  //$NON-NLS-1$
	public final static String	COLOR									= "_color";							// defines which color is used to draw the curve //$NON-NLS-1$
	public final static String	LINE_WITH							= "_lineWidth"; 				//$NON-NLS-1$
	public final static String	LINE_STYLE						= "_lineStyle"; 				//$NON-NLS-1$
	public final static String	IS_ROUND_OUT					= "_isRoundOut";				// defines if axis values are rounded //$NON-NLS-1$
	public final static String	IS_START_POINT_ZERO		= "_isStartpointZero";	// defines if axis value starts at zero //$NON-NLS-1$
	public final static String	IS_START_END_DEFINED	= "_isStartEndDefined";	// defines that explicit end values are defined for axis //$NON-NLS-1$
	public final static String	NUMBER_FORMAT					= "_numberFormat"; 			//$NON-NLS-1$
	public final static String	MAX_VALUE							= "_maxValue"; 					//$NON-NLS-1$
	public final static String	DEFINED_MAX_VALUE			= "_defMaxValue";				// overwritten max value //$NON-NLS-1$
	public final static String	MIN_VALUE							= "_minValue"; 					//$NON-NLS-1$
	public final static String	DEFINED_MIN_VALUE			= "_defMinValue";				// overwritten min value //$NON-NLS-1$
	
	public final static String[] propertyKeys = new String[] { NAME, UNIT, SYMBOL, IS_ACTIVE, IS_DIPLAYABLE, IS_VISIBLE, IS_POSITION_LEFT, COLOR, LINE_WITH, LINE_STYLE, 
			IS_ROUND_OUT, IS_START_POINT_ZERO, IS_START_END_DEFINED, NUMBER_FORMAT, MAX_VALUE, DEFINED_MAX_VALUE, MIN_VALUE, DEFINED_MIN_VALUE	};

	public final static int	TYPE_AXIS_END_VALUES			= 0;				// defines axis end values types like isRoundout, isStartpointZero, isStartEndDefined
	public final static int	TYPE_AXIS_NUMBER_FORMAT		= 1;				// defines axis scale values format
	public final static int	TYPE_AXIS_SCALE_POSITION	= 2;				// defines axis scale position left or right

	/**
	 * this constructor will create an vector to hold data points in case the initial capacity is > 0
	 * @param newDevice
	 * @param newOrdinal
	 * @param newName
	 * @param newUnit
	 * @param newSymbol
	 * @param isActiveValue
	 * @param newStatistic
	 * @param newProperties (offset, factor, color, lineType, ...)
	 * @param initialCapacity
	 */
	public Record(IDevice newDevice, int newOrdinal, String newName, String newSymbol, String newUnit, boolean isActiveValue, StatisticsType newStatistic, List<PropertyType> newProperties, int initialCapacity) {
		super(initialCapacity);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, newName + " Record(IDevice, int, String, String, String, boolean, StatisticsType, List<PropertyType>, int)"); //$NON-NLS-1$
		this.device = newDevice;
		this.ordinal = newOrdinal;
		this.name = newName;
		this.symbol = newSymbol;
		this.unit = newUnit;
		this.isActive = isActiveValue;
		this.isDisplayable = isActiveValue ? true : false;
		this.statistics = newStatistic;
		this.triggerIsGreater = (newStatistic != null && newStatistic.getTrigger() != null) ? newStatistic.getTrigger().isGreater() : null;
		this.triggerLevel = (newStatistic != null && newStatistic.getTrigger() != null) ? newStatistic.getTrigger().getLevel() : null;
		this.minTriggerTimeSec = (newStatistic != null && newStatistic.getTrigger() != null) ? newStatistic.getTrigger().getMinTimeSec() : null;
		this.initializeProperties(this, newProperties);
		this.df = new DecimalFormat("0.0"); //$NON-NLS-1$
		
		this.isCurrentRecord = this.unit.equalsIgnoreCase("A") && this.symbol.toUpperCase().contains("I");
	}

	/**
	 * copy constructor
	 */
	private Record(Record record) {
		super(record);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.name + " Record(Record)"); //$NON-NLS-1$
		this.parent = record.parent;
		this.ordinal = record.ordinal;
		this.name = record.name;
		this.symbol = record.symbol;
		this.unit = record.unit;
		this.timeStep_ms = record.timeStep_ms == null ? record.parent.timeStep_ms.clone() : record.timeStep_ms.clone();
		this.drawTimeWidth = record.drawTimeWidth;
		this.isActive = record.isActive;
		this.isDisplayable = record.isDisplayable;
		this.statistics = record.statistics;
		this.triggerIsGreater = record.triggerIsGreater;
		this.triggerLevel = record.triggerLevel;
		this.minTriggerTimeSec = record.minTriggerTimeSec;
		this.initializeProperties(record, record.properties);
		this.maxValue = record.maxValue;
		this.minValue = record.minValue;
		this.df = (DecimalFormat) record.df.clone();
		this.numberFormat = record.numberFormat;
		this.isVisible = record.isVisible;
		this.isPositionLeft = record.isPositionLeft;
		this.color = SWTResourceManager.getColor(record.color.getRGB().red, record.color.getRGB().green, record.color.getRGB().blue);
		this.lineWidth = record.lineWidth;
		this.lineStyle = record.lineStyle;
		this.isRoundOut = record.isRoundOut;
		this.isStartpointZero = record.isStartpointZero;
		this.isStartEndDefined = record.isStartEndDefined;
		this.maxScaleValue = record.maxScaleValue;
		this.minScaleValue = record.minScaleValue;
		this.isCurrentRecord = record.isCurrentRecord;

		// handle special keys for compare set record
		this.channelConfigKey = record.channelConfigKey;
		this.keyName = record.keyName;
		this.device = record.device; // reference to device	
	}

	/**
	 * overwritten clone method used to compare curves
	 */
	@Override
	public synchronized Record clone() {
		super.clone();
		return new Record(this);
	}

	/**
	 * clone method used to move records to other configuration, where measurement signature does not match the source
	 * called within RecordSet.clone(newChannelConfiguration) only!
	 */
	public Record clone(String newName) {
		Record newRecord = new Record(this);
		newRecord.name = newName;
		if (newRecord.parent.timeStep_ms != null) { //record.clone() have copied timeSteps where the parent record set 
			newRecord.timeStep_ms = null; // route to parent timeSteps
		}
		return newRecord;
	}

	/**
	 * copy constructor
	 */
	private Record(Record record, int dataIndex, boolean isFromBegin) {
		//super(record); // vector
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, record.name + " Record(Record, int, boolean)"); //$NON-NLS-1$
		this.parent = record.parent;
		this.parent.setZoomMode(false);
		this.ordinal = record.ordinal;
		this.name = record.name;
		this.symbol = record.symbol;
		this.unit = record.unit;
		this.isActive = record.isActive;
		this.isDisplayable = record.isDisplayable;
		this.statistics = record.statistics;
		this.triggerIsGreater = record.triggerIsGreater;
		this.triggerLevel = record.triggerLevel;
		this.minTriggerTimeSec = record.minTriggerTimeSec;
		this.initializeProperties(record, record.properties);
		this.maxValue = 0;
		this.minValue = 0;
		this.isCurrentRecord = record.isCurrentRecord;
		this.clear();
		this.trimToSize();
		
		if (isFromBegin) { 
			for (int i = dataIndex; i < record.realSize(); i++) {
				this.add(record.realGet(i).intValue());
			}
		}
		else {
			for (int i = 0; i < dataIndex; i++) {
				this.add(record.realGet(i).intValue());
			}
		}
		
		if (record.timeStep_ms != null && !record.timeStep_ms.isConstant) { //time step vector must be updated as well
			this.timeStep_ms = record.timeStep_ms.clone(dataIndex, isFromBegin);
		}
		else if (record.timeStep_ms != null && record.timeStep_ms.isConstant) { 
			this.timeStep_ms = record.timeStep_ms.clone();
		}
		else {
			this.timeStep_ms = null; //refer to parent time steps
		}

		this.drawTimeWidth = this.getMaxTime_ms(); 
		
		this.df = (DecimalFormat) record.df.clone();
		this.numberFormat = record.numberFormat;
		this.isVisible = record.isVisible;
		this.isPositionLeft = record.isPositionLeft;
		this.color = SWTResourceManager.getColor(record.color.getRGB().red, record.color.getRGB().green, record.color.getRGB().blue);
		this.lineWidth = record.lineWidth;
		this.lineStyle = record.lineStyle;
		this.isRoundOut = record.isRoundOut;
		this.isStartpointZero = record.isStartpointZero;
		this.maxScaleValue = this.maxValue;
		this.minScaleValue = this.minValue;
		
		this.drawTimeWidth = record.getMaxTime_ms();
		// handle special keys for compare set record
		this.channelConfigKey = record.channelConfigKey;
		this.keyName = record.keyName;
		this.device = record.device; // reference to device
	}

	/**
	 * clone method re-writes data points of all records of this record set
	 * - if isFromBegin == true, the given index is the index where the record starts after this operation
	 * - if isFromBegin == false, the given index represents the last data point index of the records.
	 * @param dataIndex
	 * @param isFromBegin
	 * @return new created record
	 */
	public Record clone(int dataIndex, boolean isFromBegin) {
		return new Record(this, dataIndex, isFromBegin);
	}

	/**
	 * initialize properties, at least all record will have as default a factor, an offset and a reduction property
	 * @param recordRef
	 * @param newProperties
	 */
	private void initializeProperties(Record recordRef, List<PropertyType> newProperties) {
		this.properties = this.properties != null ? this.properties : new ArrayList<PropertyType>();	// offset, factor, reduction, ...
		for (PropertyType property : newProperties) {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%20s - %s = %s", recordRef.name, property.getName(), property.getValue()));
			this.properties.add(property.clone());
		}
	}

	/**
	 * Method to initialize color and scale position defaults
	 * @param recordOrdinal
	 */
	public void setColorDefaultsAndPosition(int recordOrdinal) {
		// set color defaults
		switch (recordOrdinal) {
		case 0: // erste Kurve
			this.color = SWTResourceManager.getColor(0, 0, 255); //(SWT.COLOR_BLUE));
			break;
		case 1: // zweite Kurve
			this.color = SWTResourceManager.getColor(0, 128, 0); //SWT.COLOR_DARK_GREEN));
			break;
		case 2: // dritte Kurve
			this.color = SWTResourceManager.getColor(128, 0, 0); //(SWT.COLOR_DARK_RED));
			break;
		case 3: // vierte Kurve
			this.color = SWTResourceManager.getColor(255, 0, 255); //(SWT.COLOR_MAGENTA));
			break;
		case 4: // fünfte Kurve
			this.color = SWTResourceManager.getColor(64, 0, 64); //(SWT.COLOR_CYAN));
			break;
		case 5: // sechste Kurve
			this.color = SWTResourceManager.getColor(0, 128, 128); //(SWT.COLOR_DARK_YELLOW));
			break;
		case 6: // Kurve
			this.color = SWTResourceManager.getColor(128, 128, 0);
			break;
		case 7: // Kurve
			this.color = SWTResourceManager.getColor(128, 0, 128);
			break;
		case 8: // Kurve
			this.color = SWTResourceManager.getColor(0, 128, 255);
			break;
		case 9: // Kurve
			this.color = SWTResourceManager.getColor(128, 255, 0);
			break;
		case 10: // Kurve
			this.color = SWTResourceManager.getColor(255, 0, 128);
			break;
		case 11: // Kurve
			this.color = SWTResourceManager.getColor(0, 64, 128);
			break;
		case 12: // Kurve
			this.color = SWTResourceManager.getColor(64, 128, 0);
			break;
		case 13: // Kurve
			this.color = SWTResourceManager.getColor(128, 0, 64);
			break;
		case 14: // Kurve
			this.color = SWTResourceManager.getColor(128, 64, 0);
			break;
		case 15: // Kurve
			this.color = SWTResourceManager.getColor(0, 128, 64);
			break;
		default:
			Random rand = new Random();
			this.color = SWTResourceManager.getColor(rand.nextInt()&0xff, rand.nextInt()&0xff, rand.nextInt()&0xff); //(SWT.COLOR_GREEN));
			break;
		}
		// set position defaults
		if (recordOrdinal % 2 == 0) {
			this.setPositionLeft(true); //position left
		}
		else {
			this.setPositionLeft(false); // position right
		}
	}
	
	/**
	 * add a data point to the record data, checks for minimum and maximum to define display range
	 * @param point
	 */
	public synchronized boolean add(Integer point, double useTimeStep_ms) {
		if (this.timeStep_ms != null) this.timeStep_ms.add(useTimeStep_ms);
		return this.add(point);
	}
	
	/**
	 * add a data point to the record data, checks for minimum and maximum to define display range
	 * @param point
	 */
	@Override
	public synchronized boolean add(Integer point) {
		final String $METHOD_NAME = "add"; //$NON-NLS-1$
		if (super.size() == 0) {
			this.minValue = this.maxValue = point;
		}
		else {
			if (point > this.maxValue)			this.maxValue = point;
			else if (point < this.minValue) this.minValue = point;
		}
		if(log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, this.name + " adding point = " + point); //$NON-NLS-1$
		if(log.isLoggable(Level.FINEST)) log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, this.name + " minValue = " + this.minValue + " maxValue = " + this.maxValue); //$NON-NLS-1$ //$NON-NLS-2$
		
		//add shadow data points to detect current drops to nearly zero
		if (this.isCurrentRecord && super.size() > 5 ) {
			int index = super.size();
			//check value is close to zero and there should be a delta to the actual max value, we could have very low values which should be skipped
			if (this.maxValue > 200 && point < (this.maxValue >> 2)) {
				if (this.dropStartIndex == 0) {
					this.dropStartIndex = index; //reduce run in slope and reduce index by one measurement
				}
				else if (!this.dropIndexWritten) { // run into another drop while previous one is not handled
					this.parent.currentDropShadow.add(new Integer[]{this.dropStartIndex-2, index-2});
					this.dropStartIndex = index; //reduce run in slope and reduce index by one measurement					
					this.dropIndexWritten = true;
				}
				this.dropEndIndex = index + ((index - this.dropStartIndex) * 4);
			}
			else { // normal data point
				if (index > this.dropEndIndex && this.dropStartIndex != 0) {
					this.parent.currentDropShadow.add(new Integer[]{this.dropStartIndex-2, this.dropEndIndex});
					this.dropStartIndex = 0;
					this.dropIndexWritten = true;
				}
				else if (this.dropStartIndex != 0) {
					this.dropIndexWritten = false;
				}
			}
		}
		return super.add(point);
	}
	
	@Override
	public synchronized Integer set(int index, Integer point) {
		final String $METHOD_NAME = "set"; //$NON-NLS-1$
		if (super.size() == 0) {
			this.minValue = this.maxValue = point;
		}
		else {
			if (point > this.maxValue)			this.maxValue = point;
			else if (point < this.minValue) this.minValue = point;
		}
		if(log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, this.name + " setting point = " + point); //$NON-NLS-1$
		if(log.isLoggable(Level.FINEST)) log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, this.name + " minValue = " + this.minValue + " maxValue = " + this.maxValue); //$NON-NLS-1$ //$NON-NLS-2$
		return super.set(index, point);
	}

	public int getOrdinal() {
		return this.ordinal;
	}

	public void setOrdinal(int newOrdinal) {
		this.ordinal = newOrdinal;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getSyncMasterName() {
		StringBuilder sb = new StringBuilder().append(this.name.split(GDE.STRING_BLANK)[0]);
		if (this.parent.scaleSyncedRecords.get(this.ordinal) != null 
				&& this.parent.scaleSyncedRecords.get(this.ordinal).firstElement().name.split(GDE.STRING_BLANK).length > 1) {
			sb.append(GDE.STRING_BLANK).append(this.parent.scaleSyncedRecords.get(this.ordinal).firstElement().name.split(GDE.STRING_BLANK).length > 1 ? this.parent.scaleSyncedRecords.get(this.ordinal).firstElement().name.split(GDE.STRING_BLANK)[1] : GDE.STRING_STAR).append(GDE.STRING_DOT);
			sb.append(GDE.STRING_DOT);
			String trailer = GDE.STRING_STAR;
			for (Record tmpRecord : this.parent.scaleSyncedRecords.get(this.ordinal)) {
				if (tmpRecord.isDisplayable && tmpRecord.realSize() > 1) trailer = tmpRecord.name;
			}
			sb.append(trailer.split(GDE.STRING_BLANK).length > 1 ? trailer.split(GDE.STRING_BLANK)[1] : GDE.STRING_STAR);
		} 
		else {
			sb.append(GDE.STRING_BLANK).append(GDE.STRING_STAR);
		}
		return sb.toString();
	}

	public void setName(String newName) {
		if (newName != null && newName.length() > 1 && !this.name.equals(newName)) {
			this.parent.replaceRecordName(this, newName);
			this.name = newName;
		}
	}
	
	public String getUnit() {
		return this.unit;
	}

	public void setUnit(String newUnit) {
		this.unit = newUnit;
	}

	public String getSymbol() {
		return this.symbol;
	}

	public void setSymbol(String newSymbol) {
		this.symbol = newSymbol;
	}
	
	/**
	 * get a reference to the record properies (offset, factor, ...)
	 * @return list containing the properties
	 */
	List<PropertyType> getProperties() {
		return this.properties;
	}

	/**
	 * replace the properties to enable channel/configuration switch
	 * @param newProperties
	 */
	public void setProperties(List<PropertyType> newProperties) {
		this.properties = new ArrayList<PropertyType>();
		for (PropertyType property : newProperties) {
			this.properties.add(property.clone());
		}
	}
	
	/**
	 * get property reference using given property type key (IDevice.OFFSET, ...)
	 * @param propertyKey
	 * @return PropertyType
	 */
	public PropertyType getProperty(String propertyKey) {
		PropertyType property = null;
		for (PropertyType propertyType : this.properties) {
			if(propertyType.getName().equals(propertyKey)) {
				property = propertyType;
				break;
			}
		}
		return property;
	}
	
	/**
	 * create a property and return the reference
	 * @param propertyKey
	 * @param type
	 * @return created property with associated propertyKey
	 */
	public PropertyType createProperty(String propertyKey, DataTypes type, Object value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(propertyKey);
		newProperty.setType(type);
		newProperty.setValue(GDE.STRING_EMPTY + value);
		this.properties.add(newProperty);
		return newProperty;
	}

	public double getFactor() {
		double value = 1.0;
		PropertyType property = this.getProperty(IDevice.FACTOR);
		if (property != null)
			value = new Double(property.getValue()).doubleValue();
		else
			try {
				value = this.getDevice().getMeasurementFactor(this.parent.parent.number, this.ordinal);
			}
			catch (RuntimeException e) {
				//log.log(Level.WARNING, this.name + " use default value for property " + IDevice.FACTOR); // log warning and use default value
			}
		return value;
	}

	public void setFactor(double newValue) {
		PropertyType property = this.getProperty(IDevice.FACTOR);
		if (property != null)
			property.setValue(String.format("%.4f", newValue)); //$NON-NLS-1$
		else
			this.createProperty(IDevice.FACTOR, DataTypes.DOUBLE, String.format(Locale.ENGLISH, "%.4f", newValue)); //$NON-NLS-1$
	}

	public double getOffset() {
		double value = 0.0;
		PropertyType property = this.getProperty(IDevice.OFFSET);
		if (property != null)
			value = new Double(property.getValue()).doubleValue();
		else
			try {
				value = this.getDevice().getMeasurementOffset(this.parent.parent.number, this.ordinal);
			}
			catch (RuntimeException e) {
				//log.log(Level.WARNING, this.name + " use default value for property " + IDevice.OFFSET); // log warning and use default value
			}
		return value;
	}
	
	public void setOffset(double newValue) {
		PropertyType property = this.getProperty(IDevice.OFFSET);
		if (property != null)
			property.setValue(String.format("%.4f", newValue)); //$NON-NLS-1$
		else
			this.createProperty(IDevice.OFFSET, DataTypes.DOUBLE, String.format(Locale.ENGLISH, "%.4f", newValue)); //$NON-NLS-1$
	}

	public double getReduction() {
		double value = 0.0;
		PropertyType property = this.getProperty(IDevice.REDUCTION);
		if (property != null)
			value = new Double(property.getValue()).doubleValue();
		else {
			try {
				String strValue = (String)this.getDevice().getMeasurementPropertyValue(this.parent.parent.number, this.ordinal, IDevice.REDUCTION);
				if (strValue != null && strValue.length() > 0) value = new Double(strValue.trim().replace(',', '.')).doubleValue();
			}
			catch (RuntimeException e) {
				//log.log(Level.WARNING, this.name + " use default value for property " + IDevice.REDUCTION); // log warning and use default value
			}
		}
		return value;
	}
	
	public void setReduction(double newValue) {
		PropertyType property = this.getProperty(IDevice.REDUCTION);
		if (property != null)
			property.setValue(String.format("%.4f", newValue)); //$NON-NLS-1$
		else
			this.createProperty(IDevice.REDUCTION, DataTypes.DOUBLE, String.format(Locale.ENGLISH, "%.4f", newValue)); //$NON-NLS-1$
	}

	public boolean isVisible() {
		return this.isVisible;
	}

	public void setVisible(boolean enabled) {
		this.isVisible = enabled;
	}

	public int getMaxValue() {
		return this.parent.isScopeMode ? this.scopeMax : 
				this.maxValue == this.minValue ? this.maxValue + 100 : 
					this.maxValue;
	}

	public int getMinValue() {
		return this.parent.isScopeMode ? this.scopeMin : 
				this.minValue == this.maxValue ? this.minValue - 100 : 
					this.minValue;
	}

	public int getSyncMaxValue() {
		return this.parent.isScopeMode ? this.scopeMax : 
				this.syncMaxValue == this.syncMinValue ? this.syncMaxValue + 100 : 
					this.syncMaxValue;
	}

	public int getSyncMinValue() {
		return this.parent.isScopeMode ? this.scopeMin : 
				this.syncMinValue == this.syncMaxValue ? this.syncMinValue - 100 : 
					this.syncMinValue;
	}

	public int getRealMaxValue() {
		return this.maxValue;
	}

	public int getRealMinValue() {
		return this.minValue;
	}

	public int getMaxValueTriggered() {
		synchronized (this) {
			if (this.tmpTriggerRange == null) this.evaluateMinMax();
			return this.maxValueTriggered;
		}
	}

	public int getMinValueTriggered() {
		synchronized (this) {
			if (this.tmpTriggerRange == null) this.evaluateMinMax();
			return this.minValueTriggered;
		}
	}

	/**
	 * @return the minTriggerTimeSec
	 */
	public Integer getMinTriggerTimeSec() {
		if (this.minTriggerTimeSec == null) {
			this.minTriggerTimeSec = this.parent.get(this.statistics.getTriggerRefOrdinal().intValue()).getMinTriggerTimeSec();
		}
		return this.minTriggerTimeSec;
	}

	/**
	 * evaluate min and max value within range according trigger configuration
	 * while building vector of trigger range definitions as pre-requisite of avg and sigma calculation
	 */
	@SuppressWarnings("unchecked") // clone triggerRanges to be able to modify by time filter
	 void evaluateMinMax() {
		synchronized (this) {
			if (this.triggerRanges == null && this.isDisplayable && this.triggerIsGreater != null && this.triggerLevel != null) {
				int deviceTriggerlevel = Double.valueOf(this.device.reverseTranslateValue(this, this.triggerLevel / 1000.0) * 1000).intValue();
				for (int i = 0; i < this.realSize(); ++i) {
					int point = this.realGet(i);
					if (this.triggerIsGreater) { // point value must above trigger level
						if (point > deviceTriggerlevel) {
							if (this.tmpTriggerRange == null) {
								if (this.triggerRanges == null) {
									this.triggerRanges = new Vector<TriggerRange>();
									this.minValueTriggered = this.maxValueTriggered = point;
								}
								this.tmpTriggerRange = new TriggerRange(i);
							}
							else {
								if (point > this.maxValueTriggered) this.maxValueTriggered = point;
								if (point < this.minValueTriggered) this.minValueTriggered = point;
							}
						}
						else {
							if (this.triggerRanges != null && this.tmpTriggerRange != null) {
								this.tmpTriggerRange.setOut(i);
								this.triggerRanges.add(this.tmpTriggerRange);
								this.tmpTriggerRange = null;
							}
						}
					}
					else { // point value must below trigger level
						if (point < deviceTriggerlevel) {
							if (this.tmpTriggerRange == null) {
								if (this.triggerRanges == null) {
									this.triggerRanges = new Vector<TriggerRange>();
									this.minValueTriggered = this.maxValueTriggered = point;
								}
								this.tmpTriggerRange = new TriggerRange(i);
							}
							else {
								if (point > this.maxValueTriggered) this.maxValueTriggered = point;
								if (point < this.minValueTriggered) this.minValueTriggered = point;
							}
						}
						else {
							if (this.triggerRanges != null) {
								this.tmpTriggerRange.setOut(i);
								this.triggerRanges.add(this.tmpTriggerRange);
								this.tmpTriggerRange = null;
							}
						}
					}
				}
			}
			if (log.isLoggable(Level.FINE)) {
				if (this.triggerRanges != null) {
					for (TriggerRange range : this.triggerRanges) {
						log.log(Level.FINE,
								this.name
										+ " trigger range = " + range.in + "(" + TimeLine.getFomatedTime(this.getTime_ms(range.in)) + "), " + range.out + "(" + TimeLine.getFomatedTime(this.getTime_ms(range.out)) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
					}
				}
				else
					log.log(Level.FINE, this.name + " triggerRanges = null"); //$NON-NLS-1$
			}
			if (this.triggerRanges != null) {
				// evaluate trigger ranges to meet minTimeSec requirement 
				for (TriggerRange range : (Vector<TriggerRange>) this.triggerRanges.clone()) {
					if ((this.getTime_ms(range.out) - this.getTime_ms(range.in)) < this.getMinTriggerTimeSec() * 1000) this.triggerRanges.remove(range);
				}
			}
			if (log.isLoggable(Level.FINE)) {
				if (this.triggerRanges != null) {
					for (TriggerRange range : this.triggerRanges) {
						log.log(Level.FINE,
								this.name
										+ " trigger range = " + range.in + "(" + TimeLine.getFomatedTime(this.getTime_ms(range.out)) + "), " + range.out + "(" + TimeLine.getFomatedTime(this.getTime_ms(range.in)) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
					}
				}
				else
					log.log(Level.FINE, this.name + " triggerRanges = null"); //$NON-NLS-1$
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.name + " minTriggered = " + this.minValueTriggered + " maxTriggered = " + this.maxValueTriggered); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * get/calcualte max value by referenced triggered other measurement
	 * @param referencedMeasurementOrdinal
	 * @return maximum value according trigger specification of referenced measurement
	 */
	public int getMaxValueTriggered(int referencedMeasurementOrdinal) {
		synchronized (this) {
			if (this.tmpTriggerRange == null || this.triggerRanges == null) {
				this.triggerRanges = this.parent.get(referencedMeasurementOrdinal).getTriggerRanges();
			}
			if (this.maxValueTriggered == Integer.MIN_VALUE) this.setMinMaxValueTriggered();
			return this.maxValueTriggered;
		}
	}

	/**
	 * get/calcualte max value by referenced triggered other measurement
	 * @param referencedMeasurementOrdinal
	 * @return minimum value according trigger specification of referenced measurement
	 */
	public int getMinValueTriggered(int referencedMeasurementOrdinal) {
		synchronized (this) {
			if (this.tmpTriggerRange == null || this.triggerRanges == null) {
				this.triggerRanges = this.parent.get(referencedMeasurementOrdinal).getTriggerRanges();
			}
			if (this.minValueTriggered == Integer.MAX_VALUE) this.setMinMaxValueTriggered();
			return this.minValueTriggered;
		}
	}
	
	void setMinMaxValueTriggered() {
		synchronized (this) {
			if (this.triggerRanges != null) {
				for (TriggerRange range : this.triggerRanges) {
					for (int i = range.in; i < range.out; i++) {
						int point = this.realGet(i);
						if (point > this.maxValueTriggered) this.maxValueTriggered = point;
						if (point < this.minValueTriggered) this.minValueTriggered = point;
					}
				}
			}
		}
	}
	
	/**
	 * return the 'best fit' number of measurement points in dependency of zoomMode or scopeMode
	 */
  @Override
	public synchronized int size() {
		int tmpSize = elementCount;
		
		if (this.parent.isZoomMode) // record -> recordSet.isZoomMode
			tmpSize = this.findBestIndex(this.zoomTimeOffset + this.drawTimeWidth) - this.zoomOffset;
		else if (this.parent.isScopeMode)
			tmpSize = this.parent.scopeModeSize;
		
		return tmpSize;
  }
	
	/**
	 * time calculation needs always the real size of the record
	 * @return real vector size 
	 */
	public int realSize() {
		return super.size();
	}
	
	public Integer getFirst() {
		return super.size() > 0 ? super.get(0) : 0;
	}
	
	public Integer getLast() {
		return super.size() > 0 ? super.get(super.size()-1) : 0;
	}

	/**
	 * overwrites vector get(int index) to enable zoom
	 * @param index
	 */
	@Override
	public synchronized Integer get(int index) {
		if (this.parent.isZoomMode) {
			index = index + this.zoomOffset;
			index = index > (elementCount - 1) ? (elementCount - 1) : index;
			index = index < 0 ? 0 : index;
		}
		else if (this.parent.isScopeMode) {
			index = index + this.parent.scopeModeOffset;
			index = index > (elementCount - 1) ? (elementCount - 1) : index;
			index = index < 0 ? 0 : index;
		}
		else {
			index = index > (elementCount - 1) ? (elementCount - 1) : index;
			index = index < 0 ? 0 : index;
		}
		//log.log(Level.INFO, "index=" + index);
		if (elementCount != 0) {
			if (!this.parent.isCompareSet && this.parent.isSmoothAtCurrentDrop) {
				for (Integer[] dropArea :  this.parent.currentDropShadow) {
					if (dropArea[0] <= index && dropArea[1] >= index) {
						int dropStartValue = super.get(dropArea[0]);
						int dropEndValue = super.get(dropArea[1]);
						double dropDeltaValue = (double)(dropEndValue - dropStartValue) / (dropArea[1] - dropArea[0]);
						return (int) (dropStartValue + dropDeltaValue * (index - dropArea[0]));
					}
				}
			}

			return super.get(index);
		}
		return 0;
	}

	/**
	 * 
	 * @param index
	 */
	public Integer realGet(int index) {
		return super.size() != 0 ? super.get(index) : 0;
	}

	public boolean isPositionLeft() {
		return this.isPositionLeft;
	}

	public void setPositionLeft(boolean enabled) {
		this.isPositionLeft = enabled;
	}

	public Color getColor() {
		return this.color;
	}

	public void setColor(Color newColor) {
		this.color = newColor;
	}

	public boolean isRoundOut() {
		return this.parent.isZoomMode ? false : this.isRoundOut;
	}

	public void setRoundOut(boolean enabled) {
		this.isRoundOut = enabled;
	}

	public boolean isStartpointZero() {
		return this.parent.isZoomMode ? false : this.isStartpointZero;
	}

	public void setStartpointZero(boolean enabled) {
		this.isStartpointZero = enabled;
	}

	public boolean isStartEndDefined() {
		return this.parent.isZoomMode ? true : this.isStartEndDefined;
	}

	/**
	 * sets the min-max values as displayed 4.0 - 200.5
	 * @param enabled
	 * @param newMinScaleValue
	 * @param newMaxScaleValue
	 */
	public void setStartEndDefined(boolean enabled, double newMinScaleValue, double newMaxScaleValue) {
		this.isStartEndDefined = enabled;
		if (enabled) {
			this.maxScaleValue = this.maxDisplayValue = newMaxScaleValue;
			this.minScaleValue = this.minDisplayValue = newMinScaleValue;
		}
		else {
			if (this.channelConfigKey == null || this.channelConfigKey.length() < 1)
				this.channelConfigKey = this.parent.getChannelConfigName();
			this.maxScaleValue = this.parent.getDevice().translateValue(this, this.maxValue/1000.0);
			this.minScaleValue = this.parent.getDevice().translateValue(this, this.minValue/1000.0);
		}
	}

	public void setMinScaleValue(double newMinScaleValue) {
		if (this.parent.isZoomMode)
			this.minZoomScaleValue = newMinScaleValue;
		else
			this.minScaleValue = newMinScaleValue;
	}

	public void setMaxScaleValue(double newMaxScaleValue) {
		if (this.parent.isZoomMode)
			this.maxZoomScaleValue = newMaxScaleValue;
		else
			this.maxScaleValue = newMaxScaleValue;
	}

	public int getLineWidth() {
		return this.lineWidth;
	}

	public void setLineWidth(int newLineWidth) {
		this.lineWidth = newLineWidth;
	}

	public int getLineStyle() {
		return this.lineStyle;
	}

	public void setLineStyle(int newLineStyle) {
		this.lineStyle = newLineStyle;
	}

	public int getNumberFormat() {
		return this.numberFormat;
	}

	public void setNumberFormat(int newNumberFormat) {
		this.numberFormat = newNumberFormat;
		switch (newNumberFormat) {
		case -1:
			double delta = this.maxScaleValue - this.minScaleValue;
			if (this.maxScaleValue <= 100) {
				if (delta <= 0.1)
					this.df.applyPattern("0.000"); //$NON-NLS-1$
				else if (delta <= 1)
					this.df.applyPattern("0.00"); //$NON-NLS-1$
				else if (delta <= 100)
					this.df.applyPattern("0.0"); //$NON-NLS-1$
				else
					this.df.applyPattern("0"); //$NON-NLS-1$
			}
			else if (this.maxScaleValue <= 500) {
				if (delta <= 0.1)
					this.df.applyPattern("0.00"); //$NON-NLS-1$
				else if (delta <= 1)
					this.df.applyPattern("0.0"); //$NON-NLS-1$
				else
					this.df.applyPattern("0"); //$NON-NLS-1$
			}
			else {
				if (delta <= 5)
					this.df.applyPattern("0.0"); //$NON-NLS-1$
				else
					this.df.applyPattern("0"); //$NON-NLS-1$
			}
			break;
		case 0:
			this.df.applyPattern("0"); //$NON-NLS-1$
			break;
		case 1:
			this.df.applyPattern("0.0"); //$NON-NLS-1$
			break;
		case 2:
		default:
			this.df.applyPattern("0.00"); //$NON-NLS-1$
			break;
		case 3:
			this.df.applyPattern("0.000"); //$NON-NLS-1$
			break;
		}
		RecordSet compareSet = DataExplorer.getInstance().getCompareSet();
		if (compareSet != null && compareSet.size() > 0) {
			this.parent.syncMasterSlaveRecords(this, Record.TYPE_AXIS_NUMBER_FORMAT);
		}
	}

	/**
	 * @return the parent
	 */
	public RecordSet getParent() {
		return this.parent;
	}

	/**
	 * @param currentParent the parent to set
	 */
	public void setParent(RecordSet currentParent) {
		if (this.channelConfigKey == null || this.channelConfigKey.length() < 1)
			this.channelConfigKey = currentParent.getChannelConfigName();
		this.parent = currentParent;
	}

	/**
	 * @return the isDisplayable
	 */
	public boolean isDisplayable() {
		return this.isDisplayable;
	}

	/**
	 * @param enabled the isDisplayable to set
	 */
	public void setDisplayable(boolean enabled) {
		this.isDisplayable = enabled;
	}

	/**
	 * @return the isActive
	 */
	public boolean isActive() {
		return this.isActive;
	}

	/**
	 * set isActive value
	 */
	public void setActive(boolean newValue) {
		this.isActive = newValue;
	}

	/**
	 * @return the maxScaleValue
	 */
	public double getMaxScaleValue() {
		return this.parent.isZoomMode ? this.maxZoomScaleValue : this.maxScaleValue;
	}

	/**
	 * @return the minScaleValue
	 */
	public double getMinScaleValue() {
		return this.parent.isZoomMode ? this.minZoomScaleValue : this.minScaleValue;
	}

	/** 
	 * query time step time in mills seconds at index
	 * @return time step in msec
	 */
	public double getTime_ms(int index) {
		if(this.parent.isZoomMode) {
			return (this.timeStep_ms == null ? this.parent.timeStep_ms.getTime_ms(index+this.zoomOffset) : this.timeStep_ms.getTime_ms(index+this.zoomOffset)) - this.zoomTimeOffset;
		}
		else if (this.parent.isScopeMode) {
			return this.timeStep_ms == null ? this.parent.timeStep_ms.getTime_ms(index+this.parent.scopeModeOffset) - this.parent.timeStep_ms.getTime_ms(this.parent.scopeModeOffset) 
					: this.timeStep_ms.getTime_ms(index+this.parent.scopeModeOffset) - this.timeStep_ms.getTime_ms(this.parent.scopeModeOffset);
		}
		else 
			return this.timeStep_ms == null ? this.parent.timeStep_ms.getTime_ms(index) : this.timeStep_ms.getTime_ms(index);
	}

	/** 
	 * query time step time in mills seconds at index
	 * @return time step in msec
	 */
	public double getLastTime_ms() {
		return this.timeStep_ms == null ? this.parent.timeStep_ms.lastElement()/10.0 : this.timeStep_ms.lastElement()/10.0;
	}

	/** 
	 * query time step in mills seconds, this property is hold local to be independent (compare window)
	 * @return time step in msec
	 */
	public double getAverageTimeStep_ms() {
		return this.timeStep_ms == null ? this.parent.getAverageTimeStep_ms() : this.timeStep_ms.getAverageTimeStep_ms();
	}

	/**
	 * set the time step in milli seconds, this property is hold local to be independent
	 * @param timeStep_ms the timeStep_ms to set
	 */
	void setTimeStep_ms(double newTimeStep_ms) {
		this.timeStep_ms = new TimeSteps(newTimeStep_ms);
	}
	
	/**
	 * @return the maximum time of this record, which should correspondence to the last entry in timeSteps
	 */
	public double getMaxTime_ms() {
		return this.timeStep_ms == null ? this.parent.getMaxTime_ms() : this.timeStep_ms.isConstant ? this.timeStep_ms.getMaxTime_ms()*(this.elementCount-1) : this.timeStep_ms.getMaxTime_ms();
	}

	/**
	 * Find the indexes in this time vector where the given time value is placed
	 * In case of the given time in in between two available measurement points both bounding indexes are returned, 
	 * only in case where the given time matches an existing entry both indexes are equal.
	 * In cases where the returned indexes are not equal the related point x/y has to be interpolated.
	 * @param time_ms
	 * @return two index values around the given time 
	 */
	public int[] findBoundingIndexes(double time_ms) {
		int[] indexs = this.timeStep_ms == null ? this.parent.timeStep_ms.findBoundingIndexes(time_ms) : this.timeStep_ms.findBoundingIndexes(time_ms);
		if (this.elementCount > 0) {
			indexs[0] = indexs[0] > this.elementCount - 1 ? this.elementCount - 1 : indexs[0];
			indexs[1] = indexs[1] > this.elementCount - 1 ? this.elementCount - 1 : indexs[1];
		}
		return indexs;
	}

	/**
	 * find the index closest to given time in msec
	 * @param time_ms
	 * @return index nearest to given time
	 */
	public int findBestIndex(double time_ms) {
		int index = this.timeStep_ms == null ? this.parent.timeStep_ms.findBestIndex(time_ms) : this.timeStep_ms.findBestIndex(time_ms);
		return index > this.elementCount-1 ? this.elementCount-1 : index;
	}

	/**
	 * @return the zoomTimeOffset
	 */
	public int getZoomOffset() {
		return this.zoomOffset;
	}
	
	/**
	 * @return the zoomTimeOffset
	 */
	public double getZoomTimeOffset() {
		return this.zoomTimeOffset;
	}
	
	/**
	 * @return the zoomTimeOffset
	 */
	public double getDrawTimeOffset_ms() {
		double timeOffset_ms = 0;
		if (this.parent.isScopeMode) {
			if ((this.timeStep_ms != null && this.timeStep_ms.isConstant) || this.parent.timeStep_ms.isConstant) {
				timeOffset_ms = (this.timeStep_ms != null ? this.timeStep_ms.get(0) : this.parent.timeStep_ms.get(0)) * (this.elementCount-this.parent.scopeModeSize) / 10.0;
			}
			else {
				timeOffset_ms = this.timeStep_ms != null ? this.timeStep_ms.getTime_ms(this.elementCount-this.parent.scopeModeSize) : this.parent.timeStep_ms.getTime_ms(this.elementCount-this.parent.scopeModeSize);
			}
		}
		else if(this.parent.isZoomMode) {
			timeOffset_ms = this.zoomTimeOffset;
		}
		return timeOffset_ms;
	}

	/**
	 * @return the time in msec representing the segment to be displayed, without zooming this is the maximum time represented by the last data point time
	 */
	public double getDrawTimeWidth_ms() {
		if (this.elementCount > 0) {
			if (this.parent.isScopeMode) {
				if ((this.timeStep_ms != null && this.timeStep_ms.isConstant) || (this.parent.timeStep_ms != null && this.parent.timeStep_ms.isConstant)) {
					this.drawTimeWidth = (this.timeStep_ms != null ? this.timeStep_ms.get(0) : this.parent.timeStep_ms.get(0)) * this.parent.scopeModeSize / 10.0;
				}
				else {
					this.drawTimeWidth = this.timeStep_ms != null ? this.timeStep_ms.getDeltaTime(this.elementCount - 1 - this.parent.scopeModeSize, this.elementCount - 1) : this.parent.timeStep_ms
							.getDeltaTime(this.elementCount - 1 - this.parent.scopeModeSize, this.elementCount - 1);
				}
			}
			else if (!this.parent.isZoomMode) { // normal not manipulated view
				if ((this.timeStep_ms != null && this.timeStep_ms.isConstant) || (this.parent.timeStep_ms != null && this.parent.timeStep_ms.isConstant)) {
					this.drawTimeWidth = (this.timeStep_ms != null ? this.timeStep_ms.get(0) : this.parent.timeStep_ms.get(0)) * (this.elementCount - 1) / 10.0;
				}
				else {
					this.drawTimeWidth = (this.timeStep_ms != null ? this.timeStep_ms.lastElement() : this.parent.timeStep_ms.lastElement()) / 10.0;
				}
			}
		}
		return this.drawTimeWidth; // for this.parent.isZoomMode=true the width was calculated while setting the zoom bounds
	}

	/**
	 * @param newZoomTimeOffset the zoomTimeOffset to set
	 */
	public void setZoomTimeOffset(double newZoomTimeOffset) {
			this.zoomTimeOffset = newZoomTimeOffset;
	}

	/**
	 * @param newDrawTimeWidth the potential time width to be drawn
	 */
	public void setDrawTimeWidth(double newDrawTimeWidth) {
			this.drawTimeWidth = newDrawTimeWidth;
	}
	
	/**
	 * @return the decimal format used by this record
	 */
	public DecimalFormat getDecimalFormat() {
		if(this.numberFormat == -1) this.setNumberFormat(-1); // update the number format to actual automatic formating
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, this.isScaleSynced() + " - " + this.parent.getSyncMasterRecordOrdinal(this));
		return this.isScaleSynced() ? this.parent.get(this.parent.getSyncMasterRecordOrdinal(this)).df : this.df;
	}

	/**
	 * @return the keyName
	 */
	public String getKeyName() {
		return this.keyName;
	}

	/**
	 * @param newKeyName the keyName to set
	 */
	public void setKeyName(String newKeyName) {
		this.keyName = newKeyName;
	}

	/**
	 * get the device to calculate or retrieve measurement properties, this property is hold local to be independent
	 * @return the device
	 */
	public IDevice getDevice() {
		if (this.device == null)
			this.device = this.parent.getDevice();
		
		return this.device;
	}
	
	/**
	 * method to query time and value for display at a given index
	 * @param measurementPointIndex (differs from index if display width != measurement size)
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @return point time, value
	 */
	public Point getDisplayPoint(int measurementPointIndex, int xDisplayOffset, int yDisplayOffset) {
		//log.log(Level.OFF, " measurementPointIndex=" + measurementPointIndex + " value=" + (this.get(measurementPointIndex) / 1000.0) + "(" + (yDisplayOffset - Double.valueOf((this.get(measurementPointIndex)/1000.0 - (this.minDisplayValue*1/this.syncMasterFactor)) * this.displayScaleFactorValue).intValue()) + ")");
		return new Point(
			xDisplayOffset + Double.valueOf(this.getTime_ms(measurementPointIndex) * this.displayScaleFactorTime).intValue(), 
			yDisplayOffset - Double.valueOf(((this.get(measurementPointIndex)/1000.0) - (this.minDisplayValue*1/this.syncMasterFactor)) * this.displayScaleFactorValue).intValue());
	}
	
	/**
	 * method to query time and value for display at a given index
	 * @param measurementPointIndex (differs from index if display width != measurement size)
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @return point time, value
	 */
	public Point getGPSDisplayPoint(int measurementPointIndex, int xDisplayOffset, int yDisplayOffset) {
		//log.log(Level.OFF, " measurementPointIndex=" + measurementPointIndex + " value=" + (this.get(measurementPointIndex) / 1000.0) + "(" + (yDisplayOffset - Double.valueOf((this.get(measurementPointIndex)/1000.0 - (this.minDisplayValue*1/this.syncMasterFactor)) * this.displayScaleFactorValue).intValue()) + ")");
		int grad = this.get(measurementPointIndex)/1000000;
		return new Point(
			xDisplayOffset + Double.valueOf(this.getTime_ms(measurementPointIndex) * this.displayScaleFactorTime).intValue(), 
			yDisplayOffset - Double.valueOf((((grad + ((this.get(measurementPointIndex)/1000000.0-grad)/0.60)) * 1000.0) - (this.minDisplayValue*1/this.syncMasterFactor)) * this.displayScaleFactorValue).intValue());
	}
	
	/**
	 * method to query x,y position of a display at a given horizontal display index 
	 * @param xPos
	 * @return point time, value
	 */
	public Point getDisplayEndPoint(int xPos) {
		return new Point(this.parent.drawAreaBounds.x + xPos, this.parent.drawAreaBounds.y + getVerticalDisplayPointValue(xPos));
	}

	/**
	* get the time in msec at given horizontal display position
	* @param xPos of the display point
	* @return time value in msec
	*/
	public double getHorizontalDisplayPointTime_ms(int xPos) {
		return this.drawTimeWidth * xPos / this.parent.drawAreaBounds.width;
	}

	/**
	* get the formatted time with unit at given position
	* @param xPos of the display point
	* @return string of time value in simple date format HH:ss:mm:SSS
	*/
	public String getHorizontalDisplayPointAsFormattedTimeWithUnit(int xPos) {
		return TimeLine.getFomatedTimeWithUnit(this.getHorizontalDisplayPointTime_ms(xPos) + this.getDrawTimeOffset_ms()); 
	}

	/**
	 * calculate best fit index in data vector from given display point relative to the (zoomed) display width
	 * @param xPos
	 * @return position integer value
	 */
	public int getHorizontalPointIndexFromDisplayPoint(int xPos) {
		return this.findBestIndex(getHorizontalDisplayPointTime_ms(xPos) + this.getDrawTimeOffset_ms());
	}

	/**
	 * query data value (not translated in device units) from a display position point 
	 * @param xPos
	 * @return displays yPos in pixel
	 */
	public int getVerticalDisplayPointValue(int xPos) {
		int pointPosY = 0;
		
		try {
			double tmpTimeValue = this.getHorizontalDisplayPointTime_ms(xPos) + this.getDrawTimeOffset_ms();
			int[] indexs = this.findBoundingIndexes(tmpTimeValue);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, tmpTimeValue + "; " + indexs[0] + "; " + indexs[1]); //$NON-NLS-1$ //$NON-NLS-2$
			if (super.size() > 0) {
				if (this.getDevice().isGPSCoordinates(this)) {
					int grad0 = this.get(indexs[0])/1000000;
					if (indexs[0] == indexs[1]) {
						pointPosY = Double.valueOf(this.parent.drawAreaBounds.height - ((((grad0 + ((this.get(indexs[0])/1000000.0-grad0)/0.60)) * 1000.0) - (this.minDisplayValue*1/this.syncMasterFactor)) * this.displayScaleFactorValue)).intValue();
					}
					else {
						int grad1 = this.get(indexs[1])/1000000;
						double deltaValueY = (grad1 + ((this.get(indexs[1])/1000000.0-grad1)/0.60)) - (grad0 + ((this.get(indexs[0])/1000000.0-grad0)/0.60));
						double deltaTimeIndex01 = this.timeStep_ms != null ? this.timeStep_ms.getTime_ms(indexs[1]) - this.timeStep_ms.getTime_ms(indexs[0]) : this.parent.timeStep_ms.getTime_ms(indexs[1]) - this.parent.timeStep_ms.getTime_ms(indexs[0]);
						double xPosDeltaTime2Index0 = tmpTimeValue - (this.timeStep_ms != null ? this.timeStep_ms.getTime_ms(indexs[0]) : this.parent.timeStep_ms.getTime_ms(indexs[0]));
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "deltyValueY = " + deltaValueY + " deltaTime = " + deltaTimeIndex01 + " deltaTimeValue = " + xPosDeltaTime2Index0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						pointPosY = Double.valueOf(this.parent.drawAreaBounds.height - ((((grad0 + ((this.get(indexs[0])/1000000.0-grad0)/0.60)) + (xPosDeltaTime2Index0 / deltaTimeIndex01 * deltaValueY)) * 1000.0) - (this.minDisplayValue * 1 / this.syncMasterFactor))	* this.displayScaleFactorValue).intValue();
					}
				}
				else {
					if (indexs[0] == indexs[1]) {
						pointPosY = Double.valueOf(this.parent.drawAreaBounds.height - (((super.get(indexs[0]) / 1000.0) - (this.minDisplayValue * 1 / this.syncMasterFactor)) * this.displayScaleFactorValue)).intValue();
					}
					else {
						int deltaValueY = super.get(indexs[1]) - super.get(indexs[0]);
						double deltaTimeIndex01 = this.timeStep_ms != null ? this.timeStep_ms.getTime_ms(indexs[1]) - this.timeStep_ms.getTime_ms(indexs[0]) : this.parent.timeStep_ms.getTime_ms(indexs[1]) - this.parent.timeStep_ms.getTime_ms(indexs[0]);
						double xPosDeltaTime2Index0 = tmpTimeValue - (this.timeStep_ms != null ? this.timeStep_ms.getTime_ms(indexs[0]) : this.parent.timeStep_ms.getTime_ms(indexs[0]));
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "deltyValueY = " + deltaValueY + " deltaTime = " + deltaTimeIndex01 + " deltaTimeValue = " + xPosDeltaTime2Index0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						pointPosY = Double.valueOf(this.parent.drawAreaBounds.height - (((super.get(indexs[0]) + (xPosDeltaTime2Index0 / deltaTimeIndex01 * deltaValueY)) / 1000.0) - (this.minDisplayValue * 1 / this.syncMasterFactor)) * this.displayScaleFactorValue).intValue();
					}
				}
			}
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, xPos + " -> timeValue = " + TimeLine.getFomatedTime(tmpTimeValue) + " pointPosY = " + pointPosY); //$NON-NLS-1$ //$NON-NLS-2$
			
			//check yPos out of range, the graph might not visible within this area
//		if(pointPosY > this.parent.drawAreaBounds.height) 
//			log.log(Level.WARNING, "pointPosY > drawAreaBounds.height");
//		if(pointPosY < 0) 
//			log.log(Level.WARNING, "pointPosY < 0");
		}
		catch (RuntimeException e) {
			log.log(Level.WARNING, e.getMessage() + " xPos = " + xPos, e); //$NON-NLS-1$
		}
		return pointPosY > this.parent.drawAreaBounds.height ? this.parent.drawAreaBounds.height : pointPosY < 0 ? 0 : pointPosY;
	}
	
	/**
	 * get the formatted scale value corresponding the vertical display point
	 * @param yPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	public String getVerticalDisplayPointAsFormattedScaleValue(int yPos, Rectangle drawAreaBounds) {
		String displayPointValue;
		//scales are all synchronized in viewpoint of end values (min/max)
		//PropertyType syncProperty = this.parent.isCompareSet ? null : this.device.getMeasruementProperty(this.parent.parent.number, this.ordinal, MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value());
		//if (syncProperty != null && !syncProperty.getValue().equals(GDE.STRING_EMPTY)) {
		//	Record syncRecord = this.parent.get(this.ordinal);
		//		displayPointValue = syncRecord.df.format(new Double(syncRecord.minDisplayValue +  ((syncRecord.maxDisplayValue - syncRecord.minDisplayValue) * (drawAreaBounds.height-yPos) / drawAreaBounds.height)));
		//}	
		//else 
		if(this.parent.isZoomMode)
			displayPointValue = this.df.format(new Double(this.minZoomScaleValue +  ((this.maxZoomScaleValue - this.minZoomScaleValue) * (drawAreaBounds.height-yPos) / drawAreaBounds.height)));
		else
			displayPointValue = this.df.format(new Double(this.minScaleValue +  ((this.maxScaleValue - this.minScaleValue) * (drawAreaBounds.height-yPos) / drawAreaBounds.height)));
		
		return displayPointValue;
	}

	/**
	 * get the scale value corresponding the vertical display point
	 * @param yPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	public double getVerticalDisplayPointScaleValue(int yPos, Rectangle drawAreaBounds) {
		double value;
		if(this.parent.isZoomMode || this.parent.isScopeMode)
			value = this.minZoomScaleValue + ((this.maxZoomScaleValue - this.minZoomScaleValue) * yPos) / drawAreaBounds.height;
		else
			value = this.minScaleValue + ((this.maxScaleValue - this.minScaleValue) * yPos) / drawAreaBounds.height;
		
		return value;
	}

	/**
	 * get the value corresponding the display point
	 * @param deltaPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	public String getVerticalDisplayDeltaAsFormattedValue(int deltaPos, Rectangle drawAreaBounds) {
		String textValue;
		if(this.parent.isZoomMode || this.parent.isScopeMode)
			textValue = this.df.format(new Double((this.maxZoomScaleValue - this.minZoomScaleValue) * deltaPos / drawAreaBounds.height));
		else
			textValue = this.df.format(new Double((this.maxScaleValue - this.minScaleValue) * deltaPos / drawAreaBounds.height));
	
		return textValue;
	}
	
	/**
	 * get the slope value of two given points, unit depends on device configuration
	 * @param points describing the time difference (x) as well as the measurement difference (y)
	 * @return formated string of value
	 */
	public String getSlopeValue(Point points) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, GDE.STRING_EMPTY + points.toString());
		double measureDelta;
		if(this.parent.isZoomMode)
			measureDelta = (this.maxZoomScaleValue - this.minZoomScaleValue) * points.y / this.parent.drawAreaBounds.height;
		else
			measureDelta = (this.maxScaleValue - this.minScaleValue) * points.y / this.parent.drawAreaBounds.height;
		//double timeDelta = (1.0 * points.x * this.size() - 1) / drawAreaBounds.width * this.getTimeStep_ms() / 1000; //sec
		//this.drawTimeWidth * xPos / this.parent.drawAreaBounds.width;
		double timeDelta = this.drawTimeWidth * points.x / this.parent.drawAreaBounds.width / 1000; //sec
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "measureDelta = " + measureDelta + " timeDelta = " + timeDelta); //$NON-NLS-1$ //$NON-NLS-2$
		return new DecimalFormat("0.0").format(measureDelta / timeDelta); //$NON-NLS-1$
	}

	/**
	 * set the zoom bounds from display as record point offset and size, min/max scale values
	 * @param zoomBounds - where the start point offset is x,y and the area is width, height
	 */
	public void setZoomBounds(Rectangle zoomBounds) {
		this.zoomTimeOffset = this.getHorizontalDisplayPointTime_ms(zoomBounds.x) + this.getDrawTimeOffset_ms();
		if (this.zoomTimeOffset < 0) this.zoomTimeOffset = 0;
		this.zoomOffset = this.findBestIndex(this.zoomTimeOffset);
		this.drawTimeWidth = this.getHorizontalDisplayPointTime_ms(zoomBounds.width-1);
		if (this.drawTimeWidth > this.getMaxTime_ms()) this.drawTimeWidth = this.getMaxTime_ms();
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.name + " zoomTimeOffset " + TimeLine.getFomatedTimeWithUnit(this.zoomTimeOffset) + " drawTimeWidth "  + TimeLine.getFomatedTimeWithUnit(this.drawTimeWidth)); //$NON-NLS-1$ //$NON-NLS-2$

		this.tmpMinZoomScaleValue = this.getVerticalDisplayPointScaleValue(zoomBounds.y, this.parent.drawAreaBounds);
		this.tmpMaxZoomScaleValue = this.getVerticalDisplayPointScaleValue(zoomBounds.height + zoomBounds.y, this.parent.drawAreaBounds);
		this.minZoomScaleValue = tmpMinZoomScaleValue < this.minScaleValue ? this.minScaleValue : tmpMinZoomScaleValue;
		this.maxZoomScaleValue = tmpMaxZoomScaleValue > this.maxScaleValue ? this.maxScaleValue : tmpMaxZoomScaleValue;
		if (log.isLoggable(Level.OFF)) log.log(Level.OFF, this.name + " - minZoomScaleValue = " + this.minZoomScaleValue + "  maxZoomScaleValue = " + this.maxZoomScaleValue); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @return the displayScaleFactorTime
	 */
	public double getDisplayScaleFactorTime() {
		return this.displayScaleFactorTime;
	}

	/**
	 * @param newDisplayScaleFactorTime the displayScaleFactorTime to set
	 */
	public void setDisplayScaleFactorTime(double newDisplayScaleFactorTime) {
		this.displayScaleFactorTime = newDisplayScaleFactorTime;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format(Locale.ENGLISH, "displayScaleFactorTime = %.3f", newDisplayScaleFactorTime)); //$NON-NLS-1$
	}

	/**
	 * @return the displayScaleFactorValue
	 */
	public double getDisplayScaleFactorValue() {
		return this.displayScaleFactorValue;
	}

	/**
	 * @param drawAreaHeight - used to calculate the displayScaleFactorValue to set
	 */
	public void setDisplayScaleFactorValue(int drawAreaHeight) {
		this.displayScaleFactorValue = (1.0 * drawAreaHeight) / (this.maxDisplayValue - this.minDisplayValue);
		if (this.parent.isOneOfSyncableRecord(this) && this.getFactor() / this.parent.get(this.parent.getSyncMasterRecordOrdinal(this)).getFactor() != 1) {
			this.syncMasterFactor = this.getFactor() / this.parent.get(this.parent.getSyncMasterRecordOrdinal(this)).getFactor();
			this.displayScaleFactorValue = this.displayScaleFactorValue * syncMasterFactor;			
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format(Locale.ENGLISH, "drawAreaHeight = %d displayScaleFactorValue = %.3f (this.maxDisplayValue - this.minDisplayValue) = %.3f", drawAreaHeight, this.displayScaleFactorValue, (this.maxDisplayValue - this.minDisplayValue))); //$NON-NLS-1$

	}

	/**
	 * @param newMinDisplayValue the minDisplayValue to set
	 */
	public void setMinDisplayValue(double newMinDisplayValue) {
		if (this.device.isGPSCoordinates(this)) {
			this.minDisplayValue = this.device.translateValue(this, newMinDisplayValue)*1000;
		}
		else
			this.minDisplayValue = newMinDisplayValue;
		
		if (this.parent.isOneOfSyncableRecord(this)) {
			for (Record tmpRecord : this.parent.scaleSyncedRecords.get(this.parent.getSyncMasterRecordOrdinal(this))) {
				tmpRecord.minDisplayValue = this.minDisplayValue;
			}
		}
	}

	/**
	 * @param newMaxDisplayValue the maxDisplayValue to set
	 */
	public void setMaxDisplayValue(double newMaxDisplayValue) {
		if (this.device.isGPSCoordinates(this)) {
			this.maxDisplayValue = this.device.translateValue(this, newMaxDisplayValue)*1000;
		}
		else
			this.maxDisplayValue = newMaxDisplayValue;
		
		if (this.parent.isOneOfSyncableRecord(this)) {
			for (Record tmpRecord : this.parent.scaleSyncedRecords.get(this.parent.getSyncMasterRecordOrdinal(this))) {
				tmpRecord.maxDisplayValue = this.maxDisplayValue;
			}
		}
	}

	/**
	 * @return the minDisplayValue
	 */
	public double getMinDisplayValue() {
		return this.minDisplayValue;
	}

	/**
	 * @return the maxDisplayValue
	 */
	public double getMaxDisplayValue() {
		return this.maxDisplayValue;
	}

	/**
	 * @return the formated minDisplayValue as it is displayed in graphics window
	 */
	public String getFormatedMinDisplayValue() {
		return this.df.format(this.minDisplayValue);
	}

	/**
	 * @return the formated maxDisplayValue as it is displayed in graphics window
	 */
	public String getFormatedMaxDisplayValue() {
		return this.df.format(this.maxDisplayValue);
	}

	/**
	 * set min and max scale values for zoomed mode
	 * @param newMinZoomScaleValue
	 * @param newMaxZoomScaleValue
	 */
	public void setMinMaxZoomScaleValues(double newMinZoomScaleValue, double newMaxZoomScaleValue) {
		this.minZoomScaleValue				= newMinZoomScaleValue;
		this.maxZoomScaleValue				= newMaxZoomScaleValue;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.name + " - minScaleValue/minZoomScaleValue = " + this.minScaleValue + "/"  + newMinZoomScaleValue + " : maxScaleValue/maxZoomScaleValue = " + this.maxScaleValue + "/"  + newMaxZoomScaleValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * @return the isMeasurementMode
	 */
	public boolean isMeasurementMode() {
		return this.isMeasurementMode;
	}

	/**
	 * @param enabled the isMeasurementMode to set
	 */
	public void setMeasurementMode(boolean enabled) {
		this.isMeasurementMode = enabled;
	}

	/**
	 * @return the isDeltaMeasurementMode
	 */
	public boolean isDeltaMeasurementMode() {
		return this.isDeltaMeasurementMode;
	}

	/**
	 * @param enabled the isDeltaMeasurementMode to set
	 */
	public void setDeltaMeasurementMode(boolean enabled) {
		this.isDeltaMeasurementMode = enabled;
	}

	/**
	 * @return the parentName
	 */
	public String getChannelConfigKey() {
		if (this.channelConfigKey == null || this.channelConfigKey.length() < 1)
			this.channelConfigKey = this.parent.getChannelConfigName();

		return this.channelConfigKey;
	}

	/**
	 * @param newChannelConfigKey the channelConfigKey to set
	 */
	public void setChannelConfigKey(String newChannelConfigKey) {
		this.channelConfigKey = newChannelConfigKey;
	}

	/**
	 * reset the min-max-values to enable new settings after re-calculation
	 */
	public void resetMinMax() {
		this.maxValue = 0;
		this.minValue = 0;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.name);
	}

	/**
	 * set new the min-max-values after external re-calculation
	 */
	public void setMinMax(int newMin, int newMax) {
		this.maxValue = newMax;
		this.minValue = newMin;
	}
	
	/**
	 * reset all variables to enable re-calcualation of statistics
	 */
	public void resetStatiticCalculationBase() {
		synchronized (this) {
			this.maxValueTriggered = Integer.MIN_VALUE;
			this.minValueTriggered = Integer.MAX_VALUE;
			this.avgValue = Integer.MIN_VALUE;
			this.sigmaValue = Integer.MIN_VALUE;
			this.avgValueTriggered = Integer.MIN_VALUE;
			this.sigmaValueTriggered = Integer.MIN_VALUE;
			this.triggerRanges = null;
			this.tmpTriggerRange = null;
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.name);
		}
	}
	
	/**
	 * get all record properties in serialized form
	 * @return serializedRecordProperties
	 */
	public String getSerializeProperties() {
		StringBuilder sb = new StringBuilder();
		sb.append(NAME).append(GDE.STRING_EQUAL).append(this.name).append(DELIMITER);
		sb.append(UNIT).append(GDE.STRING_EQUAL).append(this.unit).append(DELIMITER);
		sb.append(SYMBOL).append(GDE.STRING_EQUAL).append(this.symbol).append(DELIMITER);
		sb.append(IS_ACTIVE).append(GDE.STRING_EQUAL).append(this.isActive).append(DELIMITER);
		sb.append(IS_DIPLAYABLE).append(GDE.STRING_EQUAL).append(this.isDisplayable).append(DELIMITER);
		sb.append(IS_VISIBLE).append(GDE.STRING_EQUAL).append(this.isVisible).append(DELIMITER);
		sb.append(MAX_VALUE).append(GDE.STRING_EQUAL).append(this.maxValue).append(DELIMITER);
		sb.append(MIN_VALUE).append(GDE.STRING_EQUAL).append(this.minValue).append(DELIMITER);
		for (PropertyType property : this.properties) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, this.name + " - " + property.getName() + " = " + property.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(property.getName()).append(GDE.STRING_UNDER_BAR).append(property.getType()).append(GDE.STRING_EQUAL).append(property.getValue()).append(DELIMITER);
		}
		sb.append(DEFINED_MAX_VALUE).append(GDE.STRING_EQUAL).append(this.maxScaleValue).append(DELIMITER);
		sb.append(DEFINED_MIN_VALUE).append(GDE.STRING_EQUAL).append(this.minScaleValue).append(DELIMITER);
		sb.append(IS_POSITION_LEFT).append(GDE.STRING_EQUAL).append(this.isPositionLeft).append(DELIMITER);
		sb.append(COLOR).append(GDE.STRING_EQUAL).append(this.color.getRed()).append(GDE.STRING_COMMA).append(this.color.getGreen()).append(GDE.STRING_COMMA).append(this.color.getBlue()).append(DELIMITER);
		sb.append(LINE_WITH).append(GDE.STRING_EQUAL).append(this.lineWidth).append(DELIMITER);
		sb.append(LINE_STYLE).append(GDE.STRING_EQUAL).append(this.lineStyle).append(DELIMITER);
		sb.append(IS_ROUND_OUT).append(GDE.STRING_EQUAL).append(this.isRoundOut).append(DELIMITER);
		sb.append(IS_START_POINT_ZERO).append(GDE.STRING_EQUAL).append(this.isStartpointZero).append(DELIMITER);
		sb.append(IS_START_END_DEFINED).append(GDE.STRING_EQUAL).append(this.isStartEndDefined).append(DELIMITER);
		sb.append(NUMBER_FORMAT).append(GDE.STRING_EQUAL).append(this.numberFormat).append(DELIMITER);
		return sb.substring(0, sb.lastIndexOf(Record.DELIMITER)) + Record.END_MARKER;
	}
	
	/**
	 * set all record properties by given serialized form
	 * @param serializedRecordProperties
	 */
	public void setSerializedProperties(String serializedRecordProperties) {
		HashMap<String, String> recordProps = StringHelper.splitString(serializedRecordProperties, DELIMITER, Record.propertyKeys);
		String tmpValue = null;
				
		tmpValue = recordProps.get(UNIT);
		if (tmpValue!=null && tmpValue.length() > 0) this.unit =  tmpValue.trim();
		tmpValue = recordProps.get(SYMBOL);
		if (tmpValue!=null && tmpValue.length() > 0) this.symbol =  tmpValue.trim();
		tmpValue = recordProps.get(IS_ACTIVE);
		if (tmpValue!=null && tmpValue.length() > 0) this.isActive =  Boolean.valueOf(tmpValue.trim());
		tmpValue = recordProps.get(IS_DIPLAYABLE);
		if (tmpValue!=null && tmpValue.length() > 0) this.isDisplayable =  Boolean.valueOf(tmpValue.trim());
		tmpValue = recordProps.get(IS_VISIBLE);
		if (tmpValue!=null && tmpValue.length() > 0) this.isVisible =  Boolean.valueOf(tmpValue.trim());
		tmpValue = recordProps.get(IS_POSITION_LEFT);
		if (tmpValue!=null && tmpValue.length() > 0) this.isPositionLeft =  Boolean.valueOf(tmpValue.trim());
		tmpValue = recordProps.get(IS_DIPLAYABLE);
		if (tmpValue!=null && tmpValue.length() > 0) this.isDisplayable =  Boolean.valueOf(tmpValue.trim());
		tmpValue = recordProps.get(COLOR);
		if (tmpValue!=null && tmpValue.length() > 5) this.color = SWTResourceManager.getColor(new Integer(tmpValue.split(GDE.STRING_COMMA)[0]), new Integer(tmpValue.split(GDE.STRING_COMMA)[1]), new Integer(tmpValue.split(GDE.STRING_COMMA)[2]));
		tmpValue = recordProps.get(LINE_WITH);
		if (tmpValue!=null && tmpValue.length() > 0) this.lineWidth =  new Integer(tmpValue.trim()).intValue();
		tmpValue = recordProps.get(LINE_STYLE);
		if (tmpValue!=null && tmpValue.length() > 0) this.lineStyle =  new Integer(tmpValue.trim()).intValue();
		tmpValue = recordProps.get(IS_ROUND_OUT);
		if (tmpValue!=null && tmpValue.length() > 0) this.isRoundOut =  Boolean.valueOf(tmpValue.trim());
		tmpValue = recordProps.get(IS_START_POINT_ZERO);
		if (tmpValue!=null && tmpValue.length() > 0) this.isStartpointZero =  Boolean.valueOf(tmpValue.trim());
		tmpValue = recordProps.get(IS_START_END_DEFINED);
		if (tmpValue!=null && tmpValue.length() > 0) this.isStartEndDefined =  Boolean.valueOf(tmpValue.trim());
		tmpValue = recordProps.get(NUMBER_FORMAT);
		if (tmpValue!=null && tmpValue.length() > 0) this.setNumberFormat(new Integer(tmpValue.trim()).intValue());
		tmpValue = recordProps.get(MAX_VALUE);
		if (tmpValue!=null && tmpValue.length() > 0) this.maxValue =  new Integer(tmpValue.trim()).intValue();
		tmpValue = recordProps.get(MIN_VALUE);
		if (tmpValue!=null && tmpValue.length() > 0) this.minValue =  new Integer(tmpValue.trim()).intValue();
		tmpValue = recordProps.get(DEFINED_MAX_VALUE);
		if (tmpValue!=null && tmpValue.length() > 0) this.maxScaleValue =  new Double(tmpValue.trim()).doubleValue();
		tmpValue = recordProps.get(DEFINED_MIN_VALUE);
		if (tmpValue!=null && tmpValue.length() > 0) this.minScaleValue =  new Double(tmpValue.trim()).doubleValue();

		tmpValue =  recordProps.get(NAME);
		if (tmpValue!=null && tmpValue.length() > 0 && !this.name.trim().equalsIgnoreCase(tmpValue.trim())) {
			this.setName(tmpValue.trim()); // might replace the record set key as well
		}
}
	
	/**
	 * set the device specific properties for this record
	 * @param serializedProperties
	 */
	public void setSerializedDeviceSpecificProperties(String serializedProperties) {
		HashMap<String, String> recordDeviceProps = StringHelper.splitString(serializedProperties, DELIMITER, this.getDevice().getUsedPropertyKeys());
		StringBuilder sb = new StringBuilder().append("update: "); //$NON-NLS-1$
		if (log.isLoggable(Level.FINE)) sb.append(this.name).append(GDE.STRING_MESSAGE_CONCAT);
		
		// each record loaded from a file updates properties instead of using the default initialized in constructor
		for (Entry<String, String> entry : recordDeviceProps.entrySet()) {
			for (PropertyType defaultProperty : this.properties) {
				if(defaultProperty.getName().equalsIgnoreCase(entry.getKey())) {
					String prop = entry.getValue();
					String type = prop.split(GDE.STRING_EQUAL)[0].substring(1);
					DataTypes dataType = type != null ? DataTypes.fromValue(type) : DataTypes.STRING;
					String value = prop.split(GDE.STRING_EQUAL)[1];
					if (value != null && value.length() > 0 && defaultProperty.getType().equals(dataType)) {
						defaultProperty.setValue(value.trim());
						if (log.isLoggable(Level.FINE)) sb.append(entry.getKey()).append(" = ").append(value); //$NON-NLS-1$
					}
				}
			}
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, sb.toString());
	}
	
	/**
	 * set data unsaved with a given reason
	 * @param reason
	 */
	public void setUnsaved(String reason) {
		this.parent.setUnsaved(reason);
	}

	/**
	 * @return the numberScaleTicks
	 */
	public int getNumberScaleTicks() {
		return this.numberScaleTicks;
	}

	/**
	 * @param newNumberScaleTicks the numberScaleTicks to set
	 */
	public void setNumberScaleTicks(int newNumberScaleTicks) {
		this.numberScaleTicks = newNumberScaleTicks;
	}

	/**
	 * @return the avgValue
	 */
	public int getAvgValue() {
		this.setAvgValue();
		return this.avgValue;
	}
	
	/**
	 * get/calcualte avg value by configuraed trigger
	 * @return average value according trigger specification
	 */
	public int getAvgValueTriggered() {
		synchronized (this) {
			if (this.triggerRanges == null) {
				this.evaluateMinMax();
			}
			this.setAvgValueTriggered();
			return this.avgValueTriggered;
		}
	}
	
	/**
	 * get/calcualte avg value by referenced triggered other measurement
	 * @param referencedMeasurementOrdinal
	 * @return average value according trigger specification of referenced measurement
	 */
	public int getAvgValueTriggered(int referencedMeasurementOrdinal) {
		synchronized (this) {
			if (this.triggerRanges == null) {
				this.triggerRanges = this.parent.get(referencedMeasurementOrdinal).getTriggerRanges();
			}
			this.setAvgValueTriggered();
			return this.avgValueTriggered;
		}
	}
	
	/**
	 * calculates the avgValue
	 */
	public void setAvgValue() {
		synchronized (this) {
			if (this.size() >= 2) {
				long sum = 0;
				int zeroCount = 0;
				for (Integer xi : this) {
					if (xi != 0) {
						sum += xi;
					}
					else {
						zeroCount++;
					}
				}
				this.avgValue = (this.size() - zeroCount) != 0 ? Long.valueOf(sum / (this.size() - zeroCount)).intValue() : 0;
			}
		}
	}
	
	/**
	 * calculates the avgValue using trigger ranges
	 */
	public void setAvgValueTriggered() {
		synchronized (this) {
			long sum = 0;
			int numPoints = 0;
			StringBuilder sb = new StringBuilder();
			if (this.triggerRanges != null) {
				for (TriggerRange range : this.triggerRanges) {
					for (int i = range.in; i < range.out; i++) {
						sum += this.get(i);
						if (log.isLoggable(Level.FINER)) sb.append(this.realGet(i) / 1000.0).append(", "); //$NON-NLS-1$
						numPoints++;
					}
					if (log.isLoggable(Level.FINER)) sb.append("\n"); //$NON-NLS-1$
				}
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, sb.toString());
				this.avgValueTriggered = numPoints > 0 ? Long.valueOf(sum / numPoints).intValue() : 0;
			}
		}
	}

	/**
	 * @return the sigmaValue
	 */
	public int getSigmaValue() {
		this.setSigmaValue();
		return this.sigmaValue;
	}
	
	/**
	 * get/calcualte avg value by trigger configuration
	 * @return sigma value according trigger specification
	 */
	public int getSigmaValueTriggered() {
		synchronized (this) {
			if (this.triggerRanges == null) {
				this.evaluateMinMax();
			}
			if (this.sigmaValueTriggered == Integer.MIN_VALUE) this.setSigmaValueTriggered();
			return this.sigmaValueTriggered;
		}
	}
	
	/**
	 * get/calculate avg value by referenced triggered other measurement
	 * @param referencedMeasurementOrdinal
	 * @return sigma value according trigger specification of referenced measurement
	 */
	public int getSigmaValueTriggered(int referencedMeasurementOrdinal) {
		synchronized (this) {
			if (this.triggerRanges == null) {
				this.triggerRanges = this.parent.get(referencedMeasurementOrdinal).getTriggerRanges();
			}
			if (this.sigmaValueTriggered == Integer.MIN_VALUE) this.setSigmaValueTriggered();
			return this.sigmaValueTriggered;
		}
	}

	/**
	 * calculates the sigmaValue 
	 */
	public void setSigmaValue() {
		synchronized (this) {
			if (super.size() >= 2) {
				double average = this.getAvgValue() / 1000.0;
				double sumPoweredValues = 0;
				for (Integer xi : this) {
					sumPoweredValues += Math.pow(xi / 1000.0 - average, 2);
				}
				this.sigmaValue = Double.valueOf(Math.sqrt(sumPoweredValues / (this.realSize() - 1)) * 1000).intValue();
			}
		}
	}

	/**
	 * calculates the sigmaValue using trigger ranges
	 */
	public void setSigmaValueTriggered() {
		synchronized (this) {
			double average = this.getAvgValueTriggered() / 1000.0;
			double sumPoweredDeviations = 0;
			int numPoints = 0;
			if (this.triggerRanges != null) {
				for (TriggerRange range : this.triggerRanges) {
					for (int i = range.in; i < range.out; i++) {
						sumPoweredDeviations += Math.pow(this.realGet(i) / 1000.0 - average, 2);
						numPoints++;
					}
				}
				this.sigmaValueTriggered = Double.valueOf(Math.sqrt(sumPoweredDeviations / (numPoints - 1)) * 1000).intValue();
			}
		}
	}
	
	/**
	 * get/calcualte sum of values by configured trigger
	 * @return sum value according trigger range specification of referenced measurement
	 */
	public synchronized int getSumTriggeredRange() {
		if (this.triggerRanges == null)  {
			this.evaluateMinMax();
		}
		return this.calculateSum();
	}
	
	/**
	 * get/calcualte sum of values by configuraed trigger
	 * @param referencedMeasurementOrdinal
	 * @return sum value according trigger range specification of referenced measurement
	 */
	public synchronized int getSumTriggeredRange(int referencedMeasurementOrdinal) {
		if (this.triggerRanges == null)  {
			this.triggerRanges = this.parent.get(referencedMeasurementOrdinal).getTriggerRanges();
		}
		return this.calculateSum();
	}
	
	/**
	 * calculate sum of min/max delta of each trigger range
	 */
	int calculateSum() {
		synchronized (this) {
			int sum = 0;
			int min = 0, max = 0;
			if (this.triggerRanges != null) {
				for (TriggerRange range : this.triggerRanges) {
					for (int i = range.in; i < range.out; i++) {
						if (i == range.in)
							min = max = this.realGet(i);
						else {
							int point = this.realGet(i);
							if (point > max) max = point;
							if (point < min) min = point;
						}
					}
					sum += this.device.translateValue(this, (max - min));
				}
			}
			return sum;
		}
	}
	
	/**
	 * get/calcualte sum of time by configured trigger
	 * @return sum value according trigger range specification of referenced measurement
	 */
	public String getTimeSumTriggeredRange() {
		synchronized (this) {
			if (this.triggerRanges == null) {
				this.evaluateMinMax();
			}
			return this.calculateTimeSum();
		}
	}
	
	/**
	 * get/calcualte sum of time by configuraed trigger
	 * @param referencedMeasurementOrdinal
	 * @return sum value according trigger range specification of referenced measurement
	 */
	public String getTimeSumTriggeredRange(int referencedMeasurementOrdinal) {
		synchronized (this) {
			if (this.triggerRanges == null) {
				this.triggerRanges = this.parent.get(referencedMeasurementOrdinal).getTriggerRanges();
			}
			return this.calculateTimeSum();
		}
	}
	
	/**
	 * calculate sum of min/max delta of each trigger range
	 */
	String calculateTimeSum() {
		synchronized (this) {
			double sum = 0;
			if (this.triggerRanges != null) {
				for (TriggerRange range : this.triggerRanges) {
					sum += (this.getTime_ms(range.out) - this.getTime_ms(range.in));
				}
			}
			return TimeLine.getFomatedTimeWithUnit(sum);
		}
	}
	
	/**
	 * @return the triggerRanges
	 */
	public Vector<TriggerRange> getTriggerRanges() {
		synchronized (this) {
			this.evaluateMinMax();
			return this.triggerRanges;
		}
	}

	/**
	 * query if the record display scale is synced with an other record
	 * @return the isScaleSynced
	 */
	public boolean isScaleSynced() {
		return this.parent.isOneOfSyncableRecord(this);
	}

	/**
	 * @return true if the record represents a scale synchronize master record
	 */
	public boolean isScaleSyncMaster() {
		return this.parent.scaleSyncedRecords.containsKey(this.ordinal);
	}
	
	/**
	 * @return true if the record is the scale sync master
	 */
	public boolean isScaleVisible() {
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.name + " isScaleSyncMaster=" + isScaleSyncMaster() + " isOneOfSyncableRecord=" + this.parent.isOneOfSyncableRecord(this));
		return isScaleSyncMaster() ? this.parent.isOneSyncableVisible(this.ordinal) : !this.parent.isOneOfSyncableRecord(this) && this.isVisible && this.isDisplayable;
	}
	
	/**
	 * set min max values for scope view (recordSet.setScopeMode())
	 * @param newScopeMin the scopeMin to set
	 * @param newScopeMax the scopeMin to set
	 */
	public void setScopeMinMax(int newScopeMin, int newScopeMax) {
		this.scopeMin = newScopeMin;
		this.scopeMax = newScopeMax;
	}
	
	/**
	 * get the curve index until the curve will be drawn (for compare set with different time length curves)
	 * @return the drawLimit
	 */
	public double getCompareSetDrawLimit_ms() {
		return this.drawLimit_ms;
	}

	/**
	 * set the curve index until the curve will be drawn
	 * @param newDrawLimit the drawLimit to set
	 */
	public void setCompareSetDrawLimit_ms(double newDrawLimit) {
		this.drawLimit_ms = newDrawLimit;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * @return true if the record contained reasonable date which can be displayed
	 */
	public boolean hasReasonableData() {
		return this.minValue != this.maxValue || device.translateValue(this, this.maxValue/1000.0) != 0.0;
	}
}

