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
	private int									maxValue							= -20000;																			// max value of the curve
	private int									minValue							= 20000;																			// min value of the curve

	private RecordSet						parent;
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
	private double							maxDisplayValue				= maxValue;																		// overwrite calculated boundaries
	private double							minDisplayValue				= minValue;

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
		this.maxDisplayValue = record.maxDisplayValue;
		this.minDisplayValue = record.minDisplayValue;
		this.device = record.device;
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
	}

	public Integer[] get() {
		return (Integer[]) this.toArray(new Integer[2]);
	}

	public Integer get(int position) {
		return this.elementAt(position).intValue();
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
		return isRoundOut;
	}

	public void setRoundOut(boolean isRoundOut) {
		this.isRoundOut = isRoundOut;
	}

	public boolean isStartpointZero() {
		return isStartpointZero;
	}

	public void setStartpointZero(boolean isStartpointZero) {
		this.isStartpointZero = isStartpointZero;
	}

	public boolean isStartEndDefined() {
		return isStartEndDefined;
	}

	/**
	 * sets the min-max values as displayed 4.0 - 200.5
	 * @param isStartEndDefined
	 * @param newMinDisplayValue
	 * @param newMaxDisplayValue
	 */
	public void setStartEndDefined(boolean isStartEndDefined, double newMinDisplayValue, double newMaxDisplayValue) {
		this.isStartEndDefined = isStartEndDefined;
		if (isStartEndDefined) {
			this.maxDisplayValue = newMaxDisplayValue;
			this.minDisplayValue = newMinDisplayValue;
		}
		else {
			this.maxDisplayValue = maxValue;
			this.minDisplayValue = minValue;
		}
	}

	public void setMinDisplayValue(double newMinDisplayValue) {
		this.minDisplayValue = newMinDisplayValue;
	}

	public void setMaxDisplayValue(double newMaxDisplayValue) {
		this.maxDisplayValue = newMaxDisplayValue;
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
	 * @return the newMaxValue
	 */
	public double getDefinedMaxValue() {
		return maxDisplayValue;
	}

	/**
	 * @return the newMinValue
	 */
	public double getDefinedMinValue() {
		return minDisplayValue;
	}

	/**
	 * @param maxValue the maxValue to set
	 */
	public void setMaxValue(int maxValue) {
		this.maxValue = maxValue;
	}

	/**
	 * @param minValue the minValue to set
	 */
	public void setMinValue(int minValue) {
		this.minValue = minValue;
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

}
