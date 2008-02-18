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
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import osde.device.IDevice;
import osde.ui.OpenSerialDataExplorer;

/**
 * @author Winfried Brügmann
 * class record holds data points of one measurement or line or curve
 */
public class Record extends Vector<Integer> {
	static final long						serialVersionUID			= 26031957;
	private Logger							log										= Logger.getLogger(this.getClass().getName());

	private String							name;																																// MessgrößeX Höhe
	private String							unit;																																// Einheit m
	private String							symbol;																															// Symbol h
	private int									timeStep_ms;

	private double							factor								= 1.0;																				// offset + factor * x
	private double							offset								= 0;																					// offset + factor * x
	private boolean							isEmpty								= true;
	private int									maxValue							= Integer.MIN_VALUE;		 										  // max value of the curve
	private int									minValue							= Integer.MAX_VALUE;													// min value of the curve

	private RecordSet						parent;
	private String							channelConfigKey; 		// used as channelConfigKey
	private String							keyName;
	private boolean							isActive;
	private boolean							isDisplayable;
	private DecimalFormat				df;

	//TODO to be set by loaded graphic template
	private boolean							isVisible							= true;
	private boolean							isPositionLeft				= true;
	private Color								color									= OpenSerialDataExplorer.COLOR_BLUE;
	private int									lineWidth							= 1;
	private int									lineStyle							= new Integer(SWT.LINE_SOLID);
	private boolean							isRoundOut						= false;
	private boolean							isStartpointZero			= false;
	private boolean							isStartEndDefined			= false;
	private int									numberFormat					= 1;																					// 0 = 0000, 1 = 000.0, 2 = 00.00
	private double							maxScaleValue					= maxValue;																		// overwrite calculated boundaries
	private double							minScaleValue					= minValue;
	private double							maxZoomScaleValue		= maxScaleValue;
	private double							minZoomScaleValue		= minScaleValue;

	private double							displayScaleFactorTime;
	private double							displayScaleFactorValue;
	private double							minDisplayValue;									// min value in device units, correspond to draw area
	private double							maxDisplayValue;									// max value in device units, correspond to draw area

	// measurement
	private boolean							isMeasurementMode				= false;
	private boolean							isDeltaMeasurementMode	= false;
	
	private final IDevice				device;																															// record need to know its device to calculate data from raw

	public final static String	IS_ACTIVE							= "_isActive";																// active means this measurement can be red from device, other wise its calculated
	public final static String	IS_DIPLAYABLE					= "_isDisplayable";														// true for all active records, true for passive records when data calculated
	public final static String	IS_VISIBLE						= "_isVisible";																// defines if data are displayed 
	public final static String	IS_POSITION_LEFT			= "_isPositionLeft";													// defines the side where the axis id displayed 
	public final static String	COLOR									= "_color";																		// defines which color is used to draw the curve
	public final static String	LINE_WITH							= "_lineWidth";
	public final static String	LINE_STYLE						= "_lineStyle";
	public final static String	IS_ROUND_OUT					= "_isRoundOut";															// defines if axis values are rounded
	public final static String	IS_START_POINT_ZERO		= "_isStartpointZero";												// defines if axis value starts at zero
	public final static String	IS_START_END_DEFINED	= "_isStartEndDefined";												// defines that explicite end values are defined for axis
	public final static String	NUMBER_FORMAT					= "_numberFormat";
	public final static String	MAX_VALUE							= "_maxValue";
	public final static String	DEFINED_MAX_VALUE			= "_defMaxValue";															// overwritten max value
	public final static String	MIN_VALUE							= "_minValue";
	public final static String	DEFINED_MIN_VALUE			= "_defMinValue";															// overwritten min value

	/**
	 * this constructor will create an vector to hold data points in case the initial capacity is > 0
	 * @param name
	 * @param unit
	 * @param symbol
	 * @param factor
	 * @param initialCapacity
	 */
	public Record(String name, String symbol, String unit, boolean isActive, double offset, double factor, int timeStep_ms, int initialCapacity) {
		super(initialCapacity);
		this.name = name;
		this.symbol = symbol;
		this.unit = unit;
		this.offset = offset;
		this.factor = factor;
		this.isActive = isActive;
		this.timeStep_ms = timeStep_ms;
		this.isDisplayable = isActive ? true : false;
		this.df = new DecimalFormat("0.0");
		this.device = OpenSerialDataExplorer.getInstance().getActiveDevice();
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
		this.offset = record.offset;
		this.factor = record.factor;
		this.timeStep_ms = record.timeStep_ms;
		this.isDisplayable = record.isDisplayable;
		this.maxValue = record.maxValue;
		this.minValue = record.minValue;
		this.df = (DecimalFormat) record.df.clone();
		this.isVisible = record.isVisible;
		this.isPositionLeft = record.isPositionLeft;
		this.color = record.color;
		this.lineWidth = record.lineWidth;
		this.lineStyle = record.lineStyle;
		this.isRoundOut = record.isRoundOut;
		this.isStartpointZero = record.isStartpointZero;
		this.numberFormat = record.numberFormat;
		this.maxScaleValue = record.maxScaleValue;
		this.minScaleValue = record.minScaleValue;
		this.device = record.device;
		this.channelConfigKey = record.channelConfigKey;
		log.fine("channelConfigKey = " + this.channelConfigKey);
	}

	/**
	 * overwritten clone method used to compare curves
	 */
	public Record clone() {
		return new Record(this);
	}

	/**
	 * add a data point to the record data, checks for minimum and maximum to define display range
	 * @param point
	 */
	public synchronized void add(int point) {
		if (isEmpty) {
			minValue = maxValue = point;
			isEmpty = false;
		}
		else {
			if (point > maxValue) maxValue = point;
			if (point < minValue) minValue = point;
		}
		if (log.isLoggable(Level.FINEST)) log.finest("adding point = " + point);
		this.add(new Integer(point));
		this.parent.dataTableAddPoint(this.getName(), this.size()-1, point);
	}

	public String getName() {
		return name;
	}

	public String getUnit() {
		return unit;
	}

	public String getSymbol() {
		return symbol;
	}

	public double getFactor() {
		return factor;
	}

	public double getOffset() {
		return offset;
	}

	public boolean isVisible() {
		return isVisible;
	}

	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}

	public int getMaxValue() {
			return maxValue;
	}

	public int getMinValue() {
			return minValue;
	}

	/**
	 * overwrites size method for zoom mode
	 */
	public int size() {
		return this.parent.isZoomMode() ? this.parent.getRecordZoomSize() : super.size();
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
		if(parent.isZoomMode()) {
			index = index + parent.getRecordZoomOffset();
			index = index > (size-1) ? (size-1) : index;
			index = index < 0 ? 0 : index;
		}
		else {
			index = index > (size-1) ? (size-1) : index;
			index = index < 0 ? 0 : index;
		}
		return size != 0 ? super.get(index) : 0;
	}
	
	/**
	 * overwrites vector elementAt(int index) to enable zoom
	 * @param index
	 */
	public Integer elementAt(int index) {
		if(parent.isZoomMode())
			return super.elementAt(index + parent.getRecordZoomOffset());
		else
			return super.elementAt(index);
	}
	
	public boolean isPositionLeft() {
		return isPositionLeft;
	}

	public void setPositionLeft(boolean isPositionLeft) {
		this.isPositionLeft = isPositionLeft;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public boolean isRoundOut() {
		return parent.isZoomMode() ? false : isRoundOut;
	}

	public void setRoundOut(boolean isRoundOut) {
		this.isRoundOut = isRoundOut;
	}

	public boolean isStartpointZero() {
		return parent.isZoomMode() ? false : isStartpointZero;
	}

	public void setStartpointZero(boolean isStartpointZero) {
		this.isStartpointZero = isStartpointZero;
	}

	public boolean isStartEndDefined() {
		return parent.isZoomMode() ? true : isStartEndDefined;
	}

	/**
	 * sets the min-max values as displayed 4.0 - 200.5
	 * @param isStartEndDefined
	 * @param newMinScaleValue
	 * @param newMaxScaleValue
	 */
	public void setStartEndDefined(boolean isStartEndDefined, double newMinScaleValue, double newMaxScaleValue) {
		this.isStartEndDefined = isStartEndDefined;
		if (isStartEndDefined) {
			this.maxScaleValue = newMaxScaleValue;
			this.minScaleValue = newMinScaleValue;
		}
		else {
			String channelConfigKey = this.getParent().getChannelName();
			this.maxScaleValue = device.translateValue(channelConfigKey, this.name, maxValue/1000);
			this.minScaleValue = device.translateValue(channelConfigKey, this.name, minValue/1000);
		}
	}

	public void setMinScaleValue(double newMinScaleValue) {
		if (parent.isZoomMode())
			this.minZoomScaleValue = newMinScaleValue;
		else
			this.minScaleValue = newMinScaleValue;
	}

	public void setMaxScaleValue(double newMaxScaleValue) {
		if (parent.isZoomMode())
			this.maxZoomScaleValue = newMaxScaleValue;
		else
			this.maxScaleValue = newMaxScaleValue;
	}

	public int getLineWidth() {
		return lineWidth;
	}

	public void setLineWidth(int lineWidth) {
		this.lineWidth = lineWidth;
	}

	public int getLineStyle() {
		return lineStyle;
	}

	public void setLineStyle(int lineStyle) {
		this.lineStyle = lineStyle;
	}

	public int getNumberFormat() {
		return numberFormat;
	}

	public void setNumberFormat(int numberFormat) {
		this.numberFormat = numberFormat;
		switch (numberFormat) {
		case 0:
			df.applyPattern("0");
			break;
		case 1:
			df.applyPattern("0.0");
			break;
		default:
			df.applyPattern("0.00");
			break;
		case 3:
			df.applyPattern("0.000");
			break;
		}
	}

	/**
	 * @return the parent
	 */
	public RecordSet getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(RecordSet parent) {
		this.channelConfigKey = parent.getChannelName();
		this.parent = parent;
	}

	/**
	 * @return the isDisplayable
	 */
	public boolean isDisplayable() {
		return isDisplayable;
	}

	/**
	 * @param isDisplayable the isDisplayable to set
	 */
	public void setDisplayable(boolean isDisplayable) {
		this.isDisplayable = isDisplayable;
	}

	/**
	 * @return the isActive
	 */
	public boolean isActive() {
		return isActive;
	}

	/**
	 * @return the maxScaleValue
	 */
	public double getMaxScaleValue() {
		return parent.isZoomMode() ? this.maxZoomScaleValue : this.maxScaleValue;
	}

	/**
	 * @return the minScaleValue
	 */
	public double getMinScaleValue() {
		return parent.isZoomMode() ? this.minZoomScaleValue : this.minScaleValue;
	}

	public int getTimeStep_ms() {
		return timeStep_ms;
	}

	public DecimalFormat getDecimalFormat() {
		return df;
	}

	/**
	 * @return the keyName
	 */
	public String getKeyName() {
		return keyName;
	}

	/**
	 * @param keyName the keyName to set
	 */
	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

	/**
	 * @return the device
	 */
	public IDevice getDevice() {
		return device;
	}
	
	/**
	 * method to query time and value for display at a given index
	 * @param index
	 * @param scaledIndex (may differ from index if display width << number of points)
	 * @param xDisplayO
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @return point time, value
	 */
	public Point getDisplayPoint(int index, int scaledIndex, int xDisplayOffset, int yDisplayOffset) {
		Point returnPoint = new Point(0,0);
		returnPoint.x = new Double((xDisplayOffset + (this.timeStep_ms * index) * this.displayScaleFactorTime)).intValue();
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
		scaledIndex = parent.getRecordZoomOffset() + scaledIndex >= this.realSize() ? this.realSize() - parent.getRecordZoomOffset() -1 : scaledIndex;
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
		if(parent.isZoomMode())
			return df.format(new Double(this.minZoomScaleValue +  ((this.maxZoomScaleValue - this.minZoomScaleValue) * (drawAreaBounds.height-yPos) / drawAreaBounds.height)));
		else
			return df.format(new Double(this.minScaleValue +  ((this.maxScaleValue - this.minScaleValue) * (drawAreaBounds.height-yPos) / drawAreaBounds.height)));
	}

	/**
	 * get the value corresponding the display point (needs translate)
	 * @param yPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	public double getDisplayPointValue(int yPos, Rectangle drawAreaBounds) {
		if(parent.isZoomMode())
			return this.minZoomScaleValue + ((this.maxZoomScaleValue - this.minZoomScaleValue) * yPos) / drawAreaBounds.height;
		else
			return this.minScaleValue + ((this.maxScaleValue - this.minScaleValue) * yPos) / drawAreaBounds.height;
	}

	/**
	 * get the value corresponding the display point (needs translate)
	 * @param deltaPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	public String getDisplayDeltaValue(int deltaPos, Rectangle drawAreaBounds) {
		if(parent.isZoomMode())
			return df.format(new Double((this.maxZoomScaleValue - this.minZoomScaleValue) * deltaPos / drawAreaBounds.height));
		else
			return df.format(new Double((this.maxScaleValue - this.minScaleValue) * deltaPos / drawAreaBounds.height));
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
		if(parent.isZoomMode())
			measureDelta = (this.maxZoomScaleValue - this.minZoomScaleValue) * points.y / drawAreaBounds.height;
		else
			measureDelta = (this.maxScaleValue - this.minScaleValue) * points.y / drawAreaBounds.height;
		double timeDelta = 1.0 * points.x * this.size() / (drawAreaBounds.width-1) * this.getTimeStep_ms() / 1000; //sec
		if(log.isLoggable(Level.FINE)) log.fine("measureDelta = " + measureDelta + " timeDelta = " + timeDelta);
		return df.format(measureDelta / timeDelta);
	}
	
	/**
	 * @return the displayScaleFactorTime
	 */
	public double getDisplayScaleFactorTime() {
		return displayScaleFactorTime;
	}

	/**
	 * @param displayScaleFactorTime the displayScaleFactorTime to set
	 */
	public void setDisplayScaleFactorTime(double displayScaleFactorTime) {
		this.displayScaleFactorTime = displayScaleFactorTime;
		if (log.isLoggable(Level.FINER)) log.finer(String.format("displayScaleFactorTime = %.3f", displayScaleFactorTime));
	}

	/**
	 * @return the displayScaleFactorValue
	 */
	public double getDisplayScaleFactorValue() {
		return displayScaleFactorValue;
	}

	/**
	 * @param displayScaleFactorValue the displayScaleFactorValue to set
	 */
	public void setDisplayScaleFactorValue(int drawAreaHeight) {
		this.displayScaleFactorValue = (1.0 * drawAreaHeight) / (this.maxDisplayValue - this.minDisplayValue);
		if (log.isLoggable(Level.FINER)) log.finer(String.format("displayScaleFactorValue = %.3f (this.maxDisplayValue - this.minDisplayValue) = %.3f", displayScaleFactorValue, (this.maxDisplayValue - this.minDisplayValue)));

	}

	/**
	 * @param minDisplayValue the minDisplayValue to set
	 */
	public void setMinDisplayValue(double minDisplayValue) {
		this.minDisplayValue = minDisplayValue;
	}

	/**
	 * @param maxDisplayValue the maxDisplayValue to set
	 */
	public void setMaxDisplayValue(double maxDisplayValue) {
		this.maxDisplayValue = maxDisplayValue;
	}

	/**
	 * @return the minDisplayValue
	 */
	public double getMinDisplayValue() {
		return minDisplayValue;
	}

	/**
	 * @return the maxDisplayValue
	 */
	public double getMaxDisplayValue() {
		return maxDisplayValue;
	}

	/**
	 * set min and max scale values for zoomed mode
	 * @param minZoomScaleValue
	 * @param maxZoomScaleValue
	 */
	public void setMinMaxZoomScaleValues(double minZoomScaleValue, double maxZoomScaleValue) {
		this.minZoomScaleValue				= minZoomScaleValue;
		this.maxZoomScaleValue				= maxZoomScaleValue;
		if (log.isLoggable(Level.FINE)) log.fine(this.name + " - minScaleValue/minZoomScaleValue = " + minScaleValue + "/"  + minZoomScaleValue + " : maxScaleValue/maxZoomScaleValue = " + maxScaleValue + "/"  + maxZoomScaleValue);
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
	 * @return the parentName
	 */
	public String getChannelConfigKey() {
		return channelConfigKey;
	}

	/**
	 * @param channelConfigKey the channelConfigKey to set
	 */
	public void setChannelConfigKey(String channelConfigKey) {
		this.channelConfigKey = channelConfigKey;
	}
}
