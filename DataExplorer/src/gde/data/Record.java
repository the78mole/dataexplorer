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
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import osde.device.DataTypes;
import osde.device.IDevice;
import osde.device.ObjectFactory;
import osde.device.PropertyType;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.StringHelper;

/**
 * @author Winfried Brügmann
 * class record holds data points of one measurement or line or curve
 */
public class Record extends Vector<Integer> {
	static final long						serialVersionUID			= 26031957;
	static final Logger					log										= Logger.getLogger(Record.class.getName());
	
	public static final String DELIMITER = "|-|";
	public static final String END_MARKER = "|:-:|";

	// this variables are used to make a record selfcontained within compare set
	String							channelConfigKey; 		// used as channelConfigKey
	String							keyName;
	double							timeStep_ms						= 0;			// time base of measurement points
	IDevice							device;

	RecordSet						parent;
	
	String							name;																																// MessgrößeX Höhe
	String							unit;																																// Einheit m
	String							symbol;																															// Symbol h
	boolean							isActive;
	boolean							isDisplayable;
	boolean							isVisible							= true;
	List<PropertyType>	properties						= new ArrayList<PropertyType>();	// offset, factor, reduction, ...
	boolean							isPositionLeft				= true;
	Color								color									= OpenSerialDataExplorer.COLOR_BLUE;
	int									lineWidth							= 1;
	int									lineStyle							= new Integer(SWT.LINE_SOLID);
	boolean							isRoundOut						= false;
	boolean							isStartpointZero			= false;
	boolean							isStartEndDefined			= false;
	DecimalFormat				df;
	int									numberFormat					= 1;																					// 0 = 0000, 1 = 000.0, 2 = 00.00
	int									maxValue							= Integer.MIN_VALUE;		 										  // max value of the curve
	int									minValue							= Integer.MAX_VALUE;													// min value of the curve
	double							maxScaleValue					= this.maxValue;																		// overwrite calculated boundaries
	double							minScaleValue					= this.minValue;
	double							maxZoomScaleValue		= this.maxScaleValue;
	double							minZoomScaleValue		= this.minScaleValue;

	double							displayScaleFactorTime;
	double							displayScaleFactorValue;
	double							minDisplayValue;									// min value in device units, correspond to draw area
	double							maxDisplayValue;									// max value in device units, correspond to draw area

	// measurement
	boolean							isMeasurementMode				= false;
	boolean							isDeltaMeasurementMode	= false;
	
	public final static String	NAME									= "_name";							// active means this measurement can be red from device, other wise its calculated
	public final static String	UNIT									= "_unit";							// active means this measurement can be red from device, other wise its calculated
	public final static String	SYMBOL								= "_symbol";						// active means this measurement can be red from device, other wise its calculated
	public final static String	IS_ACTIVE							= "_isActive";					// active means this measurement can be red from device, other wise its calculated
	public final static String	IS_DIPLAYABLE					= "_isDisplayable";			// true for all active records, true for passive records when data calculated
	public final static String	IS_VISIBLE						= "_isVisible";					// defines if data are displayed 
	public final static String	IS_POSITION_LEFT			= "_isPositionLeft";		// defines the side where the axis id displayed 
	public final static String	COLOR									= "_color";							// defines which color is used to draw the curve
	public final static String	LINE_WITH							= "_lineWidth";
	public final static String	LINE_STYLE						= "_lineStyle";
	public final static String	IS_ROUND_OUT					= "_isRoundOut";				// defines if axis values are rounded
	public final static String	IS_START_POINT_ZERO		= "_isStartpointZero";	// defines if axis value starts at zero
	public final static String	IS_START_END_DEFINED	= "_isStartEndDefined";	// defines that explicit end values are defined for axis
	public final static String	NUMBER_FORMAT					= "_numberFormat";
	public final static String	MAX_VALUE							= "_maxValue";
	public final static String	DEFINED_MAX_VALUE			= "_defMaxValue";				// overwritten max value
	public final static String	MIN_VALUE							= "_minValue";
	public final static String	DEFINED_MIN_VALUE			= "_defMinValue";				// overwritten min value
	
	private final String[] propertyKeys = new String[] { NAME, UNIT, SYMBOL, IS_ACTIVE, IS_DIPLAYABLE, IS_VISIBLE, IS_POSITION_LEFT, COLOR, LINE_WITH, LINE_STYLE, 
			IS_ROUND_OUT, IS_START_POINT_ZERO, IS_START_END_DEFINED, NUMBER_FORMAT, MAX_VALUE, DEFINED_MAX_VALUE, MIN_VALUE, DEFINED_MIN_VALUE	};


	/**
	 * this constructor will create an vector to hold data points in case the initial capacity is > 0
	 * @param newName
	 * @param newUnit
	 * @param newSymbol
	 * @param isActiveValue
	 * @param newProperties (offset, factor, color, lineType, ...)
	 * @param initialCapacity
	 */
	public Record(String newName, String newSymbol, String newUnit, boolean isActiveValue, List<PropertyType> newProperties, int initialCapacity) {
		super(initialCapacity);
		this.name = newName;
		this.symbol = newSymbol;
		this.unit = newUnit;
		this.isActive = isActiveValue;
		this.isDisplayable = isActiveValue ? true : false;
		for (PropertyType property : newProperties) {
			this.properties.add(property.clone());
		}
		this.df = new DecimalFormat("0.0");
		this.numberFormat = 1;
		
		// special keys for compare set record are handled with put method
		//this.channelConfigKey;
		//this.keyName;
		this.device = OpenSerialDataExplorer.getInstance().getActiveDevice();
		this.timeStep_ms = this.device.getTimeStep_ms();
	}

	/**
	 * copy constructor
	 */
	private Record(Record record) {
		super(record);
		this.name = record.name;
		this.symbol = record.symbol;
		this.unit = record.unit;
		this.isActive = record.isActive;
		this.isDisplayable = record.isDisplayable;
		this.properties = new ArrayList<PropertyType>();
		for (PropertyType property : record.properties) {
			this.properties.add(property.clone());
		}
		this.maxValue = record.maxValue;
		this.minValue = record.minValue;
		this.df = (DecimalFormat) record.df.clone();
		this.numberFormat = record.numberFormat;
		this.isVisible = record.isVisible;
		this.isPositionLeft = record.isPositionLeft;
		this.color = new Color(record.color.getDevice(), record.color.getRGB());
		this.lineWidth = record.lineWidth;
		this.lineStyle = record.lineStyle;
		this.isRoundOut = record.isRoundOut;
		this.isStartpointZero = record.isStartpointZero;
		this.maxScaleValue = record.maxScaleValue;
		this.minScaleValue = record.minScaleValue;
		// handle special keys for compare set record
		this.channelConfigKey = record.channelConfigKey;
		this.keyName = record.keyName;
		this.timeStep_ms = record.timeStep_ms;
		this.device = record.device; // reference to device
	}

	/**
	 * overwritten clone method used to compare curves
	 */
	public Record clone() {
		return new Record(this);
	}

	/**
	 * clone method used to move records to other configuration, where measurement signature does not match the source
	 */
	public Record clone(String newName) {
		Record newRecord = new Record(this);
		newRecord.name = newName;
		return newRecord;
	}

	/**
	 * copy constructor
	 */
	private Record(Record record, int dataIndex, boolean isFromBegin) {
		//super(record); // vector
		this.parent = record.parent;
		this.parent.setZoomMode(false);
		this.name = record.name;
		this.symbol = record.symbol;
		this.unit = record.unit;
		this.isActive = record.isActive;
		this.isDisplayable = record.isDisplayable;
		this.properties = new ArrayList<PropertyType>();
		for (PropertyType property : record.properties) {
			this.properties.add(property.clone());
		}
		this.maxValue = 0;
		this.minValue = 0;
		this.clear();
		this.trimToSize();
		if (isFromBegin) {
			for (int i = dataIndex; i < record.realSize(); i++) {
				this.add(record.get(i).intValue());
			}
		}
		else {
			for (int i = 0; i < dataIndex; i++) {
				this.add(record.get(i).intValue());
			}
		}
		
		this.df = (DecimalFormat) record.df.clone();
		this.numberFormat = record.numberFormat;
		this.isVisible = record.isVisible;
		this.isPositionLeft = record.isPositionLeft;
		this.color = new Color(record.color.getDevice(), record.color.getRGB());
		this.lineWidth = record.lineWidth;
		this.lineStyle = record.lineStyle;
		this.isRoundOut = record.isRoundOut;
		this.isStartpointZero = record.isStartpointZero;
		this.maxScaleValue = this.maxValue;
		this.minScaleValue = this.minValue;
		// handle special keys for compare set record
		this.channelConfigKey = record.channelConfigKey;
		this.keyName = record.keyName;
		this.timeStep_ms = record.timeStep_ms;
		this.device = record.device; // reference to device
	}

	/**
	 * clone method re-writes data points of all records of this record set
	 * - if isFromBegin == true, the given index is the index where the record starts after this operation
	 * - if isFromBegin == false, the given index represents the last data point index of the records.
	 * @param dataIndex
	 * @param isFromBegin
	 * @return new dreated record
	 */
	public Record clone(int dataIndex, boolean isFromBegin) {
		return new Record(this, dataIndex, isFromBegin);
	}
	
	/**
	 * add a data point to the record data, checks for minimum and maximum to define display range
	 * @param point
	 */
	public boolean add(int point) {
		if (super.size() == 0) {
			this.minValue = this.maxValue = point;
		}
		else {
			if (point > this.maxValue) this.maxValue = point;
			if (point < this.minValue) this.minValue = point;
		}
		if (log.isLoggable(Level.FINEST)) log.finest("adding point = " + point);
		return this.add(new Integer(point));
	}

	public String getName() {
		return this.name;
	}

	public void setName(String newName) {
		if (!this.name.equals(newName)) {
			this.parent.replaceRecordName(this.name, newName);
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
		newProperty.setValue("" + value);
		return newProperty;
	}

	public double getFactor() {
		double value = 1.0;
		PropertyType property = this.getProperty(IDevice.FACTOR);
		if (property != null)
			value = new Double(property.getValue().replace(',', '.')).doubleValue();
		else
			value = this.getDevice().getMeasurementFactor(this.getChannelConfigKey(), this.name);
		return value;
	}

	public void setFactor(double newValue) {
		PropertyType property = this.getProperty(IDevice.FACTOR);
		if (property != null)
			property.setValue(String.format("%.4f", newValue));
		else
			this.properties.add(this.createProperty(IDevice.FACTOR, DataTypes.DOUBLE, String.format("%.4f", newValue)));
	}

	public double getOffset() {
		double value = 0.0;
		PropertyType property = this.getProperty(IDevice.OFFSET);
		if (property != null)
			value = new Double(property.getValue().replace(',', '.')).doubleValue();
		else
			value = this.getDevice().getMeasurementOffset(this.getChannelConfigKey(), this.name);
		return value;
	}
	
	public void setOffset(double newValue) {
		PropertyType property = this.getProperty(IDevice.OFFSET);
		if (property != null)
			property.setValue(String.format("%.4f", newValue));
		else
			this.properties.add(this.createProperty(IDevice.OFFSET, DataTypes.DOUBLE, String.format("%.4f", newValue)));
	}

	public double getReduction() {
		double value = 0.0;
		PropertyType property = this.getProperty(IDevice.REDUCTION);
		if (property != null)
			value = new Double(property.getValue().replace(',', '.')).doubleValue();
		else {
			String strValue = (String)this.getDevice().getMeasurementPropertyValue(this.getChannelConfigKey(), this.name, IDevice.REDUCTION);
			if (strValue != null && strValue.length() > 0) value = new Double(strValue.trim().replace(',', '.')).doubleValue();
		}
		return value;
	}
	
	public void setReduction(double newValue) {
		PropertyType property = this.getProperty(IDevice.REDUCTION);
		if (property != null)
			property.setValue(String.format("%.4f", newValue));
		else
			this.properties.add(this.createProperty(IDevice.REDUCTION, DataTypes.DOUBLE, String.format("%.4f", newValue)));
	}

	public boolean isVisible() {
		return this.isVisible;
	}

	public void setVisible(boolean enabled) {
		this.isVisible = enabled;
	}

	public int getMaxValue() {
			return this.maxValue == this.minValue ? this.maxValue + 100 : this.maxValue;
	}

	public int getMinValue() {
			return this.minValue == this.maxValue ? this.minValue - 100 : this.minValue;
	}

	/**
	 * overwrites size method for zoom mode and not zoomed compare window
	 */
	public int size() {
		int tmpSize = super.size();
		
		if (this.parent.isZoomMode())
			tmpSize = this.parent.getRecordZoomSize();
		else if (this.parent.isCompareSet())
			tmpSize = this.parent.getRecordDataSize();
		
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
		return super.get(0);
	}
	
	public Integer getLast() {
		return super.get(super.size()-1);
	}

	/**
	 * overwrites vector get(int index) to enable zoom
	 * @param index
	 */
	public Integer get(int index) {
		int size = super.size();
		int currentIndex = index;
		if(this.parent.isZoomMode()) {
			currentIndex = currentIndex + this.parent.getRecordZoomOffset();
			currentIndex = currentIndex > (size-1) ? (size-1) : currentIndex;
			currentIndex = currentIndex < 0 ? 0 : currentIndex;
		}
		else {
			currentIndex = currentIndex > (size-1) ? (size-1) : currentIndex;
			currentIndex = currentIndex < 0 ? 0 : currentIndex;
		}
		return size != 0 ? super.get(currentIndex) : 0;
	}
	
	/**
	 * overwrites vector elementAt(int index) to enable zoom
	 * @param index
	 */
	public Integer elementAt(int index) {
		Integer value;
		if(this.parent.isZoomMode())
			value = super.elementAt(index + this.parent.getRecordZoomOffset());
		else
			value = super.elementAt(index);
		
		return value;
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
		return this.parent.isZoomMode() ? false : this.isRoundOut;
	}

	public void setRoundOut(boolean enabled) {
		this.isRoundOut = enabled;
	}

	public boolean isStartpointZero() {
		return this.parent.isZoomMode() ? false : this.isStartpointZero;
	}

	public void setStartpointZero(boolean enabled) {
		this.isStartpointZero = enabled;
	}

	public boolean isStartEndDefined() {
		return this.parent.isZoomMode() ? true : this.isStartEndDefined;
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
			this.maxScaleValue = newMaxScaleValue;
			this.minScaleValue = newMinScaleValue;
		}
		else {
			if (this.channelConfigKey == null || this.channelConfigKey.length() < 1)
				this.channelConfigKey = this.parent.getChannelConfigName();
			this.maxScaleValue = this.parent.getDevice().translateValue(this, this.maxValue/1000.0);
			this.minScaleValue = this.parent.getDevice().translateValue(this, this.minValue/1000.0);
		}
	}

	public void setMinScaleValue(double newMinScaleValue) {
		if (this.parent.isZoomMode())
			this.minZoomScaleValue = newMinScaleValue;
		else
			this.minScaleValue = newMinScaleValue;
	}

	public void setMaxScaleValue(double newMaxScaleValue) {
		if (this.parent.isZoomMode())
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
		case 0:
			this.df.applyPattern("0");
			break;
		case 1:
			this.df.applyPattern("0.0");
			break;
		default:
			this.df.applyPattern("0.00");
			break;
		case 3:
			this.df.applyPattern("0.000");
			break;
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
		return this.parent.isZoomMode() ? this.maxZoomScaleValue : this.maxScaleValue;
	}

	/**
	 * @return the minScaleValue
	 */
	public double getMinScaleValue() {
		return this.parent.isZoomMode() ? this.minZoomScaleValue : this.minScaleValue;
	}

	/** 
	 * qurey time step in milli seconds, this property is hold local to be independent
	 * @return time step in ms
	 */
	public double getTimeStep_ms() {
		if (this.timeStep_ms == 0)
			this.timeStep_ms = this.parent.getTimeStep_ms();
		return this.timeStep_ms;
	}

	/**
	 * set the time step in milli seconds, this property is hold local to be independent
	 * @param timeStep_ms the timeStep_ms to set
	 */
	void setTimeStep_ms(double newTimeStep_ms) {
		this.timeStep_ms = newTimeStep_ms;
	}

	public DecimalFormat getDecimalFormat() {
		return this.df;
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
	 * set the device as fallback for data point calculation, this property is hold local to be independent
	 * @param device the device to set
	 */
	void setDevice(IDevice useDevice) {
		this.device = useDevice;
	}
	
	/**
	 * method to query time and value for display at a given index
	 * @param index
	 * @param scaledIndex (may differ from index if display width << number of points)
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @return point time, value
	 */
	public Point getDisplayPoint(int index, int scaledIndex, int xDisplayOffset, int yDisplayOffset) {
		Point returnPoint = new Point(0,0);
		returnPoint.x = new Double((xDisplayOffset + (this.getTimeStep_ms() * index) * this.displayScaleFactorTime)).intValue();
		returnPoint.y = new Double(yDisplayOffset - 1 - ((this.get(scaledIndex) / 1000.0) - this.minDisplayValue) * this.displayScaleFactorValue).intValue();
		return returnPoint;
	}

	/**
	 * query data value (not translated in device units) from a display position point 
	 * @param xPos
	 * @param drawAreaBounds
	 * @return displays yPos in pixel
	 */
	public int getDisplayPointDataValue(int xPos, Rectangle drawAreaBounds) {
		int scaledIndex = this.size() * xPos / drawAreaBounds.width;
		scaledIndex = this.parent.getRecordZoomOffset() + scaledIndex >= this.realSize() ? this.realSize() - this.parent.getRecordZoomOffset() -1 : scaledIndex;
		if (log.isLoggable(Level.FINER))log.finer("scaledIndex = " + scaledIndex);
		int pointY = new Double(drawAreaBounds.height - ((this.get(scaledIndex) / 1000.0) - this.minDisplayValue) * this.displayScaleFactorValue).intValue();
		pointY = pointY < 0 ? 0 : pointY;
		pointY = pointY >= drawAreaBounds.height ? drawAreaBounds.height-1 : pointY;
		if (log.isLoggable(Level.FINER))log.finer("pointY = " + pointY);
		return pointY;
	}
	
	/**
	 * get the value corresponding the display point (needs translate)
	 * @param yPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	public String getDisplayPointValueString(int yPos, Rectangle drawAreaBounds) {
		String displayPointValue;
		if(this.parent.isZoomMode())
			displayPointValue = this.df.format(new Double(this.minZoomScaleValue +  ((this.maxZoomScaleValue - this.minZoomScaleValue) * (drawAreaBounds.height-yPos) / drawAreaBounds.height)));
		else
			displayPointValue = this.df.format(new Double(this.minScaleValue +  ((this.maxScaleValue - this.minScaleValue) * (drawAreaBounds.height-yPos) / drawAreaBounds.height)));
		
		return displayPointValue;
	}

	/**
	 * get the value corresponding the display point (needs translate)
	 * @param yPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	public double getDisplayPointValue(int yPos, Rectangle drawAreaBounds) {
		double value;
		if(this.parent.isZoomMode())
			value = this.minZoomScaleValue + ((this.maxZoomScaleValue - this.minZoomScaleValue) * yPos) / drawAreaBounds.height;
		else
			value = this.minScaleValue + ((this.maxScaleValue - this.minScaleValue) * yPos) / drawAreaBounds.height;
		
		return value;
	}

	/**
	 * get the value corresponding the display point (needs translate)
	 * @param deltaPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	public String getDisplayDeltaValue(int deltaPos, Rectangle drawAreaBounds) {
		String textValue;
		if(this.parent.isZoomMode())
			textValue = this.df.format(new Double((this.maxZoomScaleValue - this.minZoomScaleValue) * deltaPos / drawAreaBounds.height));
		else
			textValue = this.df.format(new Double((this.maxScaleValue - this.minScaleValue) * deltaPos / drawAreaBounds.height));
	
		return textValue;
	}
	
	/**
	 * get the slope value of two given points, unit depends on device configuration
	 * @param points describing the time difference (x) as well as the measurement difference (y)
	 * @param drawAreaBounds
	 * @return string of value
	 */
	public String getSlopeValue(Point points, Rectangle drawAreaBounds) {
		if(log.isLoggable(Level.FINE)) log.fine("" + points.toString());
		double measureDelta;
		if(this.parent.isZoomMode())
			measureDelta = (this.maxZoomScaleValue - this.minZoomScaleValue) * points.y / drawAreaBounds.height;
		else
			measureDelta = (this.maxScaleValue - this.minScaleValue) * points.y / drawAreaBounds.height;
		double timeDelta = 1.0 * points.x * this.size() / (drawAreaBounds.width-1) * this.getTimeStep_ms() / 1000; //sec
		if(log.isLoggable(Level.FINE)) log.fine("measureDelta = " + measureDelta + " timeDelta = " + timeDelta);
		return this.df.format(measureDelta / timeDelta);
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
		if (log.isLoggable(Level.FINER)) log.finer(String.format("displayScaleFactorTime = %.3f", newDisplayScaleFactorTime));
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
		if (log.isLoggable(Level.FINER)) log.finer(String.format("displayScaleFactorValue = %.3f (this.maxDisplayValue - this.minDisplayValue) = %.3f", this.displayScaleFactorValue, (this.maxDisplayValue - this.minDisplayValue)));

	}

	/**
	 * @param newMinDisplayValue the minDisplayValue to set
	 */
	public void setMinDisplayValue(double newMinDisplayValue) {
		this.minDisplayValue = newMinDisplayValue;
	}

	/**
	 * @param newMaxDisplayValue the maxDisplayValue to set
	 */
	public void setMaxDisplayValue(double newMaxDisplayValue) {
		this.maxDisplayValue = newMaxDisplayValue;
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
	 * set min and max scale values for zoomed mode
	 * @param newMinZoomScaleValue
	 * @param newMaxZoomScaleValue
	 */
	public void setMinMaxZoomScaleValues(double newMinZoomScaleValue, double newMaxZoomScaleValue) {
		this.minZoomScaleValue				= newMinZoomScaleValue;
		this.maxZoomScaleValue				= newMaxZoomScaleValue;
		if (log.isLoggable(Level.FINE)) log.fine(this.name + " - minScaleValue/minZoomScaleValue = " + this.minScaleValue + "/"  + newMinZoomScaleValue + " : maxScaleValue/maxZoomScaleValue = " + this.maxScaleValue + "/"  + newMaxZoomScaleValue);
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
	}

	/**
	 * set new the min-max-values after external re-calculation
	 */
	public void setMinMax(int newMin, int newMax) {
		this.maxValue = newMax;
		this.minValue = newMin;
	}
	
	/**
	 * get all record properties in serialized form
	 * @return serializedRecordProperties
	 */
	public String getSerializeProperties() {
		StringBuilder sb = new StringBuilder();
		sb.append(NAME).append("=").append(this.name).append(DELIMITER);
		sb.append(UNIT).append("=").append(this.unit).append(DELIMITER);
		sb.append(SYMBOL).append("=").append(this.symbol).append(DELIMITER);
		sb.append(IS_ACTIVE).append("=").append(this.isActive).append(DELIMITER);
		sb.append(IS_DIPLAYABLE).append("=").append(this.isDisplayable).append(DELIMITER);
		sb.append(IS_VISIBLE).append("=").append(this.isVisible).append(DELIMITER);
		sb.append(MAX_VALUE).append("=").append(this.maxValue).append(DELIMITER);
		sb.append(MIN_VALUE).append("=").append(this.minValue).append(DELIMITER);
		for (PropertyType property : this.properties) {
			sb.append(property.getName()).append("_").append(property.getType()).append("=").append(property.getValue()).append(DELIMITER);
		}
		sb.append(DEFINED_MAX_VALUE).append("=").append(this.maxScaleValue).append(DELIMITER);
		sb.append(DEFINED_MIN_VALUE).append("=").append(this.minScaleValue).append(DELIMITER);
		sb.append(IS_POSITION_LEFT).append("=").append(this.isPositionLeft).append(DELIMITER);
		sb.append(COLOR).append("=").append(this.color.getRed()).append(",").append(this.color.getGreen()).append(",").append(this.color.getBlue()).append(DELIMITER);
		sb.append(LINE_WITH).append("=").append(this.lineWidth).append(DELIMITER);
		sb.append(LINE_STYLE).append("=").append(this.lineStyle).append(DELIMITER);
		sb.append(IS_ROUND_OUT).append("=").append(this.isRoundOut).append(DELIMITER);
		sb.append(IS_START_POINT_ZERO).append("=").append(this.isStartpointZero).append(DELIMITER);
		sb.append(IS_START_END_DEFINED).append("=").append(this.isStartEndDefined).append(DELIMITER);
		sb.append(NUMBER_FORMAT).append("=").append(this.numberFormat).append(DELIMITER);
		return sb.substring(0, sb.lastIndexOf(Record.DELIMITER)) + Record.END_MARKER;
	}
	
	/**
	 * set all record properties by given serialized form
	 * @param serializedRecordProperties
	 */
	public void setSerializedProperties(String serializedRecordProperties) {
		HashMap<String, String> recordProps = StringHelper.splitString(serializedRecordProperties, DELIMITER, this.propertyKeys);
		String tmpValue = null;
				
		tmpValue = recordProps.get(UNIT);
		if (tmpValue!=null && tmpValue.length() > 0) this.unit =  tmpValue.trim();
		tmpValue = recordProps.get(SYMBOL);
		if (tmpValue!=null && tmpValue.length() > 0) this.symbol =  tmpValue.trim();
		tmpValue = recordProps.get(IS_ACTIVE);
		if (tmpValue!=null && tmpValue.length() > 0) this.isActive =  new Boolean(tmpValue.trim()).booleanValue();
		tmpValue = recordProps.get(IS_DIPLAYABLE);
		if (tmpValue!=null && tmpValue.length() > 0) this.isDisplayable =  new Boolean(tmpValue.trim()).booleanValue();
		tmpValue = recordProps.get(IS_VISIBLE);
		if (tmpValue!=null && tmpValue.length() > 0) this.isVisible =  new Boolean(tmpValue.trim()).booleanValue();
		tmpValue = recordProps.get(IS_POSITION_LEFT);
		if (tmpValue!=null && tmpValue.length() > 0) this.isPositionLeft =  new Boolean(tmpValue.trim()).booleanValue();
		tmpValue = recordProps.get(IS_DIPLAYABLE);
		if (tmpValue!=null && tmpValue.length() > 0) this.isDisplayable =  new Boolean(tmpValue.trim()).booleanValue();
		tmpValue = recordProps.get(COLOR);
		if (tmpValue!=null && tmpValue.length() > 5) this.color = SWTResourceManager.getColor(new Integer(tmpValue.split(",")[0]), new Integer(tmpValue.split(",")[1]), new Integer(tmpValue.split(",")[2]));
		tmpValue = recordProps.get(LINE_WITH);
		if (tmpValue!=null && tmpValue.length() > 0) this.lineWidth =  new Integer(tmpValue.trim()).intValue();
		tmpValue = recordProps.get(LINE_STYLE);
		if (tmpValue!=null && tmpValue.length() > 0) this.lineStyle =  new Integer(tmpValue.trim()).intValue();
		tmpValue = recordProps.get(IS_ROUND_OUT);
		if (tmpValue!=null && tmpValue.length() > 0) this.isRoundOut =  new Boolean(tmpValue.trim()).booleanValue();
		tmpValue = recordProps.get(IS_START_POINT_ZERO);
		if (tmpValue!=null && tmpValue.length() > 0) this.isStartpointZero =  new Boolean(tmpValue.trim()).booleanValue();
		tmpValue = recordProps.get(IS_START_END_DEFINED);
		if (tmpValue!=null && tmpValue.length() > 0) this.isStartEndDefined =  new Boolean(tmpValue.trim()).booleanValue();
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
		if (tmpValue!=null && tmpValue.length() > 0 && !this.name.equalsIgnoreCase(tmpValue)) {
			this.setName(tmpValue.trim()); // might replace the record set key as well
		}
}
	
	/**
	 * set the device specific properties for this record
	 * @param serializedProperties
	 */
	public void setSerializedDeviceSpecificProperties(String serializedProperties) {
		HashMap<String, String> recordDeviceProps = StringHelper.splitString(serializedProperties, DELIMITER, this.getDevice().getUsedPropertyKeys());
		Iterator<String> iterator = recordDeviceProps.keySet().iterator();
	
		if (iterator.hasNext()) {
			this.properties = new ArrayList<PropertyType>(); // offset, factor, reduction, ...
			while (iterator.hasNext()) {
				String propName = iterator.next();
				String prop = recordDeviceProps.get(propName);
				PropertyType tmpProperty = new ObjectFactory().createPropertyType();
				tmpProperty.setName(propName);
				String type = prop.split("=")[0].substring(1);
				if (type != null && type.length() > 3) tmpProperty.setType(DataTypes.fromValue(type));
				String value = prop.split("=")[1];
				if (value != null && value.length() > 0) tmpProperty.setValue(value.trim());
				this.properties.add(tmpProperty.clone());
			}
		}
	}
	
	/**
	 * set data unsaved with a given reason
	 * @param reason
	 */
	public void setUnsaved(String reason) {
		this.parent.setUnsaved(reason);
	}
}

