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

    Copyright (c) 2017 Thomas Eickert
****************************************************************************************/

package gde.data;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import gde.GDE;
import gde.config.Settings;
import gde.data.Record.DataType;
import gde.device.DataTypes;
import gde.device.IDevice;
import gde.device.ObjectFactory;
import gde.device.PropertyType;
import gde.device.StatisticsType;
import gde.log.Level;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

/**
 * Supports the GDE kernel record class and the history trail record class.
 * @author Thomas Eickert (USER)
 */
public abstract class CommonRecord extends AbstractRecord {
	private static final String		$CLASS_NAME								= CommonRecord.class.getName();
	private static final long			serialVersionUID					= 26031957;
	private static final Logger		log												= Logger.getLogger($CLASS_NAME);

	public final static String		NAME											= "_name";											// active means this measurement can be red //$NON-NLS-1$
																																													// from device, other wise its calculated
	public final static String		UNIT											= "_unit";											// active means this measurement can be red //$NON-NLS-1$
																																													// from device, other wise its calculated
	public final static String		SYMBOL										= "_symbol";										// active means this measurement can be red //$NON-NLS-1$
																																													// from device, other wise its calculated
	public final static String		IS_ACTIVE									= "_isActive";									// active means this measurement can be red //$NON-NLS-1$
																																													// from device, other wise its calculated
	public final static String		IS_DIPLAYABLE							= "_isDisplayable";							// true for all active records, true for //$NON-NLS-1$
																																													// passive records when data calculated
	public final static String		IS_VISIBLE								= "_isVisible";									// defines if data are displayed //$NON-NLS-1$
	public final static String		IS_POSITION_LEFT					= "_isPositionLeft";						// defines the side where the axis id //$NON-NLS-1$
																																													// displayed
	public final static String		COLOR											= "_color";											// defines which color is used to draw the //$NON-NLS-1$
																																													// curve
	public final static String		LINE_WITH									= "_lineWidth";									//$NON-NLS-1$
	public final static String		LINE_STYLE								= "_lineStyle";									//$NON-NLS-1$
	public final static String		IS_ROUND_OUT							= "_isRoundOut";								// defines if axis values are rounded //$NON-NLS-1$
	public final static String		IS_START_POINT_ZERO				= "_isStartpointZero";					// defines if axis value starts at zero //$NON-NLS-1$
	public final static String		IS_START_END_DEFINED			= "_isStartEndDefined";					// defines that explicit end values are //$NON-NLS-1$
																																													// defined for axis
	public final static String		NUMBER_FORMAT							= "_numberFormat";							//$NON-NLS-1$
	public final static String		MAX_VALUE									= "_maxValue";									//$NON-NLS-1$
	public final static String		DEFINED_MAX_VALUE					= "_defMaxValue";								// overwritten max value //$NON-NLS-1$
	public final static String		MIN_VALUE									= "_minValue";									//$NON-NLS-1$
	public final static String		DEFINED_MIN_VALUE					= "_defMinValue";								// overwritten min value //$NON-NLS-1$
	public final static String		DATA_TYPE									= "_dataType";									// data type of record //$NON-NLS-1$

	public final static String[]	propertyKeys							= new String[] { NAME,					//
			UNIT, SYMBOL, IS_ACTIVE, IS_DIPLAYABLE, IS_VISIBLE, IS_POSITION_LEFT,								//
			COLOR, LINE_WITH, LINE_STYLE, IS_ROUND_OUT, IS_START_POINT_ZERO,										//
			IS_START_END_DEFINED, NUMBER_FORMAT,																								//
			MAX_VALUE, DEFINED_MAX_VALUE, MIN_VALUE, DEFINED_MIN_VALUE, DATA_TYPE };

	public final static int				TYPE_AXIS_END_VALUES			= 0;														// defines axis end values types like isRoundout,
																																													// isStartpointZero, isStartEndDefined
	public final static int				TYPE_AXIS_NUMBER_FORMAT		= 1;														// defines axis scale values format
	public final static int				TYPE_AXIS_SCALE_POSITION	= 2;														// defines axis scale position left or right

	protected final Settings			settings									= Settings.getInstance();

// record fields
//
// this variables are used to make a record selfcontained within compare set
	String												channelConfigKey;																					// used as channelConfigKey
	String												keyName;
	/**
	 * for each measurement point in compare set, where time step of measurement points might be individual
	 */
	TimeSteps											timeStep_ms								= null;
	protected IDevice							device;
	/**
	 * is referencing the source position of the record ordinal relative to the initial
	 * device measurement configuration and used to find specific properties
	 */
	protected int									ordinal;

	protected AbstractRecordSet		parent;

	// core fields
	protected String							name;																											// measurement name Höhe
	String												unit;																											// unit [m]
	String												symbol;																										// symbol h
	String												description								= GDE.STRING_BLANK;							// only set if copied into compare set
	protected Boolean							isActive;
	boolean												isDisplayable;
	boolean												isVisible									= true;
	StatisticsType								statistics								= null;

	List<PropertyType>						properties								= new ArrayList<>();						// offset, factor, reduction, ...
	boolean												isPositionLeft						= true;
	Color													color											= DataExplorer.COLOR_BLACK;
	int														lineWidth									= 1;
	int														lineStyle									= SWT.LINE_SOLID;
	protected boolean							isRoundOut								= false;
	protected boolean							isStartpointZero					= false;
	protected boolean							isStartEndDefined					= false;
	protected DecimalFormat				df;
	protected int									numberFormat							= -1;														// -1 = automatic, 0 = 0000, 1 = 000.0, 2 = 00.00
	protected int									maxValue									= 0;														// max value of the curve
	protected int									minValue									= 0;														// min value of the curve
	protected double							maxScaleValue							= this.maxValue;								// overwrite calculated boundaries
	protected double							minScaleValue							= this.minValue;
	DataType											dataType									= Record.DataType.DEFAULT;

	// synchronize
	protected int									syncMaxValue							= 0;														// max value of the curve if synced
	protected int									syncMinValue							= 0;														// min value of the curve if synced

	int														numberScaleTicks					= 0;

	// display the record
	int														avgValue									= Integer.MIN_VALUE;						// avarage value (avg = sum(xi)/n)
	int														sigmaValue								= Integer.MIN_VALUE;						// sigma value of data, according a set trigger level if any
	double												displayScaleFactorTime;
	protected double							displayScaleFactorValue;
	protected double							syncMasterFactor					= 1.0;													// synchronized scale and different measurement factors
	protected double							minDisplayValue;																					// min value in device units, correspond to draw area
	protected double							maxDisplayValue;																					// max value in device units, correspond to draw area

	// measurement
	boolean												isMeasurementMode					= false;
	boolean												isDeltaMeasurementMode		= false;

// protected CommonRecord(int initialCapacity) {
// super(initialCapacity);
// }

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
	public CommonRecord(IDevice newDevice, int newOrdinal, String newName, String newSymbol, String newUnit, boolean isActiveValue,
			StatisticsType newStatistic, List<PropertyType> newProperties, int initialCapacity) {
		super(initialCapacity);
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, newName + " Record(IDevice, int, String, String, String, boolean, StatisticsType, List<PropertyType>, int)"); //$NON-NLS-1$
		this.device = newDevice;
		this.ordinal = newOrdinal;
		this.name = newName;
		this.symbol = newSymbol;
		this.unit = newUnit;
		this.isActive = isActiveValue;
		this.isDisplayable = isActiveValue ? true : false;
		this.statistics = newStatistic;

		this.initializeProperties(this, newProperties);

		this.df = new DecimalFormat("0.0"); //$NON-NLS-1$
	}

	/**
	 * copy constructor
	 */
	protected CommonRecord(CommonRecord record) {
		super(record);
	}

	/**
	 * initialize properties, at least all record will have as default a factor, an offset and a reduction property
	 * @param recordRef
	 * @param newProperties
	 */
	private void initializeProperties(CommonRecord recordRef, List<PropertyType> newProperties) {
		this.properties = this.properties != null ? this.properties : new ArrayList<PropertyType>(); // offset, factor, reduction, ...
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
			this.color = SWTResourceManager.getColor(0, 0, 255); // (SWT.COLOR_BLUE));
			break;
		case 1: // zweite Kurve
			this.color = SWTResourceManager.getColor(0, 128, 0); // SWT.COLOR_DARK_GREEN));
			break;
		case 2: // dritte Kurve
			this.color = SWTResourceManager.getColor(128, 0, 0); // (SWT.COLOR_DARK_RED));
			break;
		case 3: // vierte Kurve
			this.color = SWTResourceManager.getColor(255, 0, 255); // (SWT.COLOR_MAGENTA));
			break;
		case 4: // fünfte Kurve
			this.color = SWTResourceManager.getColor(64, 0, 64); // (SWT.COLOR_CYAN));
			break;
		case 5: // sechste Kurve
			this.color = SWTResourceManager.getColor(0, 128, 128); // (SWT.COLOR_DARK_YELLOW));
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
			this.color = SWTResourceManager.getColor(rand.nextInt() & 0xff, rand.nextInt() & 0xff, rand.nextInt() & 0xff); // (SWT.COLOR_GREEN));
			break;
		}
		// set position defaults
		if (recordOrdinal % 2 == 0) {
			this.setPositionLeft(true); // position left
		} else {
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

	@Override
	public abstract boolean add(Integer point);

	@Override
	public synchronized Integer set(int index, Integer point) {
		final String $METHOD_NAME = "set"; //$NON-NLS-1$
		if (super.size() == 0) {
			this.minValue = this.maxValue = point;
		} else {
			if (point > this.maxValue)
				this.maxValue = point;
			else if (point < this.minValue) this.minValue = point;
		}
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, this.name + " setting point = " + point); //$NON-NLS-1$
		if (log.isLoggable(Level.FINEST))
			log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, this.name + " minValue = " + this.minValue + " maxValue = " + this.maxValue); //$NON-NLS-1$ //$NON-NLS-2$
		return super.set(index, point);
	}

	@Override
	public int getOrdinal() {
		return this.ordinal;
	}

	public void setOrdinal(int newOrdinal) {
		this.ordinal = newOrdinal;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public String getSyncMasterName() {
		StringBuilder sb = new StringBuilder().append(this.name.split(GDE.STRING_BLANK)[0]);
		Map<Integer, Vector<? extends AbstractRecord>> syncedRecords = this.getAbstractParent().scaleSyncedRecords;
		if (syncedRecords.get(this.ordinal) != null && syncedRecords.get(this.ordinal).firstElement().getName().split(GDE.STRING_BLANK).length > 1) {
			sb.append(GDE.STRING_BLANK);
			String[] splitName = syncedRecords.get(this.ordinal).firstElement().getName().split(GDE.STRING_BLANK);
			sb.append(splitName.length > 1 ? splitName[1] : GDE.STRING_STAR);
			sb.append(GDE.STRING_DOT);
			sb.append(GDE.STRING_DOT);
			String trailer = GDE.STRING_STAR;
			for (AbstractRecord tmpRecord : syncedRecords.get(this.ordinal)) {
				if (tmpRecord.isDisplayable() && tmpRecord.realSize() > 1) trailer = tmpRecord.getName();
			}
			sb.append(trailer.split(GDE.STRING_BLANK).length > 1 ? trailer.split(GDE.STRING_BLANK)[1] : GDE.STRING_STAR);
		} else {
			sb.append(GDE.STRING_MESSAGE_CONCAT).append(syncedRecords.get(this.ordinal).lastElement().getName());
		}
		return sb.toString();
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
			if (propertyType.getName().equals(propertyKey)) {
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

	@Override
	public double getFactor() {
		double value = 1.0;
		PropertyType property = this.getProperty(IDevice.FACTOR);
		if (property != null)
			value = Double.valueOf(property.getValue()).doubleValue();
		else
			try {
				value = this.getDevice().getMeasurementFactor(this.getAbstractParent().parent.number, this.ordinal);
			} catch (RuntimeException e) {
				// log.log(Level.WARNING, this.name + " use default value for property " + IDevice.FACTOR); // log warning and use default value
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

	@Override
	public double getOffset() {
		double value = 0.0;
		PropertyType property = this.getProperty(IDevice.OFFSET);
		if (property != null)
			value = Double.valueOf(property.getValue()).doubleValue();
		else
			try {
				value = this.getDevice().getMeasurementOffset(this.getAbstractParent().parent.number, this.ordinal);
			} catch (RuntimeException e) {
				// log.log(Level.WARNING, this.name + " use default value for property " + IDevice.OFFSET); // log warning and use default value
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

	@Override
	public double getReduction() {
		double value = 0.0;
		PropertyType property = this.getProperty(IDevice.REDUCTION);
		if (property != null)
			value = Double.valueOf(property.getValue()).doubleValue();
		else {
			try {
				String strValue = (String) this.getDevice().getMeasurementPropertyValue(this.getAbstractParent().parent.number, this.ordinal, IDevice.REDUCTION);
				if (strValue != null && strValue.length() > 0) value = Double.valueOf(strValue.trim().replace(',', '.')).doubleValue();
			} catch (RuntimeException e) {
				// log.log(Level.WARNING, this.name + " use default value for property " + IDevice.REDUCTION); // log warning and use default value
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

	@Override
	public boolean isVisible() {
		return this.isVisible;
	}

	@Override
	public void setVisible(boolean enabled) {
		this.isVisible = enabled;
	}

	public abstract int getMaxValue();

	public abstract int getMinValue();

	public abstract int getSyncMaxValue();

	public abstract int getSyncMinValue();

	public int getRealMaxValue() {
		return this.maxValue;
	}

	public int getRealMinValue() {
		return this.minValue;
	}

	/**
	 * return the 'best fit' number of measurement points in dependency of zoomMode or scopeMode
	 */
	@Override
	public abstract int size();

	/**
	 * time calculation needs always the real size of the record
	 * @return real vector size
	 */
	@Override
	public int realSize() {
		return super.size();
	}

	public Integer getFirst() {
		return super.size() > 0 ? super.get(0) : 0;
	}

	public Integer getLast() {
		return super.size() > 0 ? super.get(super.size() - 1) : 0;
	}

	/**
	 * ET: throws NullPointerException if super.get(index) is null.
	 * in debugging mode, however, the expression 'super.size() != 0 ? super.get(index) : 0' evaluates to null which is correct.
	 * could not clarify the reason for the exception <<<
	 * @param index
	 */
	public Integer realGet(int index) {
		try {
			return super.size() != 0 ? super.get(index) : 0;
		} catch (ArrayIndexOutOfBoundsException e) {
			log.log(Level.WARNING, String.format("%s - %20s: size = %d - indesx = %d", this.parent.name, this.name, super.size(), index));
			return super.size() != 0 ? super.get(index - 1) : 0;
		}
	}

	@Override
	public boolean isPositionLeft() {
		return this.isPositionLeft;
	}

	@Override
	public void setPositionLeft(boolean enabled) {
		this.isPositionLeft = enabled;
	}

	@Override
	public Color getColor() {
		return this.color;
	}

	public String getRGB() {
		return String.format("%d, %d,%d", this.color.getRed(), this.color.getGreen(), this.color.getBlue());
	}

	@Override
	public void setColor(Color newColor) {
		this.color = newColor;
	}

	@Override
	public abstract boolean isRoundOut();

	@Override
	public abstract void setRoundOut(boolean enabled);

	@Override
	public abstract boolean isStartpointZero();

	@Override
	public abstract void setStartpointZero(boolean enabled);

	@Override
	public abstract boolean isStartEndDefined();

	/**
	 * sets the min-max values as displayed 4.0 - 200.5
	 * @param enabled
	 * @param newMinScaleValue
	 * @param newMaxScaleValue
	 */
	@Override
	public abstract void setStartEndDefined(boolean enabled, double newMinScaleValue, double newMaxScaleValue);

	public abstract void setMinScaleValue(double newMinScaleValue);

	public abstract void setMaxScaleValue(double newMaxScaleValue);

	@Override
	public int getLineWidth() {
		return this.lineWidth;
	}

	@Override
	public void setLineWidth(int newLineWidth) {
		this.lineWidth = newLineWidth;
	}

	@Override
	public int getLineStyle() {
		return this.lineStyle;
	}

	@Override
	public void setLineStyle(int newLineStyle) {
		this.lineStyle = newLineStyle;
	}

	@Override
	public int getNumberFormat() {
		return this.numberFormat;
	}

	@Override
	public abstract void setNumberFormat(int newNumberFormat);

	@Override
	public AbstractRecordSet getAbstractParent() {
		return this.parent;
	}

	/**
	 * @return the isDisplayable
	 */
	@Override
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
		return this.isActive == null || super.size() == 0 ? false : this.isActive;
	}

	/**
	 * set isActive value
	 */
	public void setActive(Boolean newValue) {
		this.isActive = newValue;
	}

	/**
	 * @return if record data represented by calculation
	 */
	public boolean isCalculation() {
		return this.isActive == null ? true : false;
	}

	/**
	 * @return the maxScaleValue
	 */
	@Override
	public abstract double getMaxScaleValue();

	/**
	 * @return the minScaleValue
	 */
	@Override
	public abstract double getMinScaleValue();

	/**
	 * return the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by the start date time of this record (set)
	 * @return time stamp of the date and time when the record starts
	 */
	public long getStartTimeStamp() {
		return this.timeStep_ms == null ? this.parent.timeStep_ms.getStartTimeStamp() : this.timeStep_ms.getStartTimeStamp();
	}

	/**
	 * query time step time in mills seconds at index
	 * @return time step in msec
	 */
	public abstract double getTime_ms(int index);

	/**
	 * query time step time in mills seconds at index
	 * @return time step in msec
	 */
	public double getLastTime_ms() {
		return this.timeStep_ms == null ? this.parent.timeStep_ms.lastElement() / 10.0 : this.timeStep_ms.lastElement() / 10.0;
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
		return this.timeStep_ms == null ? this.parent.getMaxTime_ms() : this.timeStep_ms.isConstant
				? this.timeStep_ms.getMaxTime_ms() * (this.elementCount - 1) : this.timeStep_ms.getMaxTime_ms();
	}

	/**
	 * Find the indexes in this time vector where the given time value is placed
	 * In case of the given time in in between two available measurement points both bounding indexes are returned,
	 * only in case where the given time matches an existing entry both indexes are equal.
	 * In cases where the returned indexes are not equal the related point x/y has to be interpolated.
	 * @param time_ms
	 * @return two index values around the given time
	 */
	private int[] findBoundingIndexes(double time_ms) {
		int[] indexs = this.timeStep_ms == null ? this.parent.timeStep_ms.findBoundingIndexes(time_ms) : this.timeStep_ms.findBoundingIndexes(time_ms);
		if (this.elementCount > 0) {
			indexs[0] = indexs[0] > this.elementCount - 1 ? this.elementCount - 1 : indexs[0];
			indexs[1] = indexs[1] > this.elementCount - 1 ? this.elementCount - 1 : indexs[1];
		}
		return indexs;
	}

	/**
	 * @return the decimal format used by this record
	 */
	public DecimalFormat getDecimalFormat() {
		if (this.numberFormat == -1) this.setNumberFormat(-1); // update the number format to actual automatic formating
		if (log.isLoggable(Level.FINE))
			log.log(Level.FINE, this.isScaleSynced() + " - " + this.getAbstractParent().getSyncMasterRecordOrdinal(getName()));
		return this.isScaleSynced() ? this.getAbstractParent().get(this.getAbstractParent().getSyncMasterRecordOrdinal(getName())).getRealDf() : this.df;
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
		if (this.device == null) this.device = this.getAbstractParent().getDevice();

		return this.device;
	}

	/**
	 * @param finalValue is the value to be displayed (without applying a factor or GPS coordinates fraction correction)
	 * @return the translated and decimal formatted value
	 */
	@Override
	public abstract String getFormattedScaleValue(double finalValue);

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
		AbstractRecordSet abstractParent = this.getAbstractParent();
		if (abstractParent.isOneOfSyncableRecord(getName()) && this.getFactor() / abstractParent.get(abstractParent.getSyncMasterRecordOrdinal(getName())).getFactor() != 1) {
			this.syncMasterFactor = this.getFactor() / abstractParent.get(abstractParent.getSyncMasterRecordOrdinal(getName())).getFactor();
			this.displayScaleFactorValue = this.displayScaleFactorValue * this.syncMasterFactor;
		}
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, String.format(Locale.ENGLISH, "drawAreaHeight = %d displayScaleFactorValue = %.3f (this.maxDisplayValue - this.minDisplayValue) = %.3f", //$NON-NLS-1$
					drawAreaHeight, this.displayScaleFactorValue, (this.maxDisplayValue - this.minDisplayValue)));

	}

	/**
	 * @param newMinDisplayValue the minDisplayValue to set
	 */
	public abstract void setMinDisplayValue(double newMinDisplayValue);

	/**
	 * @param newMaxDisplayValue the maxDisplayValue to set
	 */
	public abstract void setMaxDisplayValue(double newMaxDisplayValue);

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
	 * @return the isMeasurementMode
	 */
	@Override
	public boolean isMeasurementMode() {
		return this.isMeasurementMode;
	}

	/**
	 * @param enabled the isMeasurementMode to set
	 */
	@Override
	public void setMeasurementMode(boolean enabled) {
		this.isMeasurementMode = enabled;
	}

	/**
	 * @return the isDeltaMeasurementMode
	 */
	@Override
	public boolean isDeltaMeasurementMode() {
		return this.isDeltaMeasurementMode;
	}

	/**
	 * @param enabled the isDeltaMeasurementMode to set
	 */
	@Override
	public void setDeltaMeasurementMode(boolean enabled) {
		this.isDeltaMeasurementMode = enabled;
	}

	/**
	 * @return the parentName
	 */
	public String getChannelConfigKey() {
		if (this.channelConfigKey == null || this.channelConfigKey.length() < 1) this.channelConfigKey = this.parent.getChannelConfigName();

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
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "setMinMax :  " + newMin + "," + newMax); //$NON-NLS-1$ //$NON-NLS-2$
		this.maxValue = newMax;
		this.minValue = newMin;
	}

	/**
	 * @return the numberScaleTicks
	 */
	@Override
	public int getNumberScaleTicks() {
		return this.numberScaleTicks;
	}

	/**
	 * @param newNumberScaleTicks the numberScaleTicks to set
	 */
	@Override
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
	 * calculates the avgValue by discarding nulls and zeroes
	 */
	public void setAvgValue() {
		synchronized (this) {
			if (super.size() > 0) {
				long sum = 0;
				int zeroCount = 0;
				for (Integer xi : this) { // ET loops over all elements of the vector
					if (xi != null && xi != 0) {
						sum += xi;
					} else {
						zeroCount++;
					}
				}
				this.avgValue = (super.size() - zeroCount) != 0 ? Long.valueOf(sum / (super.size() - zeroCount)).intValue() : 0; // ET realSize corresponds to
																																																													// the looped elements
			}
		}
	}

	/**
	 * @return the sigmaValue by discarding nulls and zeroes
	 */
	public int getSigmaValue() {
		this.setSigmaValue();
		return this.sigmaValue;
	}

	/**
	 * calculates the sigmaValue by discarding nulls and zeroes
	 */
	public void setSigmaValue() {
		synchronized (this) {
			if (super.size() > 0) {
				double average = this.getAvgValue() / 1000.0;
				double sumPoweredValues = 0;
				int zeroCount = 0;
				for (Integer xi : this) { // ET loops over all elements of the vector
					if (xi != null && xi != 0) { // sigma is based on the same population as avg
						sumPoweredValues += Math.pow(xi / 1000.0 - average, 2);
					} else {
						zeroCount++;
					}
				}
				this.sigmaValue = (super.size() - zeroCount - 1) != 0
						? Double.valueOf(Math.sqrt(sumPoweredValues / (super.size() - zeroCount - 1)) * 1000).intValue() : 0; // ET realSize corresponds to the
																																																									// looped elements
			}
		}
	}

	/**
	 * query if the record display scale is synced with an other record
	 * @return the isScaleSynced
	 */
	public boolean isScaleSynced() {
		return this.getAbstractParent().isOneOfSyncableRecord(getName());
	}

	/**
	 * @return true if the record represents a scale synchronize master record
	 */
	public boolean isScaleSyncMaster() {
		return this.getAbstractParent().scaleSyncedRecords.containsKey(this.ordinal);
	}

	/**
	 * @return true if the record is the scale sync master
	 */
	@Override
	public boolean isScaleVisible() {
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, this.name + " isScaleSyncMaster=" + isScaleSyncMaster() + " isOneOfSyncableRecord=" + this.getAbstractParent().isOneOfSyncableRecord(getName()));
		return isScaleSyncMaster() ? this.getAbstractParent().isOneSyncableVisible(this.ordinal)
				: !this.getAbstractParent().isOneOfSyncableRecord(getName()) && this.isVisible && this.isDisplayable;
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
	public abstract boolean hasReasonableData();

	/**
	 * @return the dataType of this record
	 */
	@Override
	public Record.DataType getDataType() {
		return this.dataType == null ? DataType.DEFAULT : this.dataType;
	}

	/**
	 * set the dataType of this record by evaluating its name
	 */
	public void setDataType() {
		this.dataType = DataType.guess(this.name);
	}

	/**
	 * set the dataType of this record
	 */
	public void setDataType(Record.DataType newDataType) {
		this.dataType = newDataType;
	}

}
