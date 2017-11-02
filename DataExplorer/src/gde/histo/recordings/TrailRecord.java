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

package gde.histo.recordings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import gde.GDE;
import gde.data.Record;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.device.ScoreGroupType;
import gde.device.SettlementType;
import gde.device.TrailTypes;
import gde.device.resource.DeviceXmlResource;
import gde.histo.utils.Spot;
import gde.log.Logger;

/**
 * Hold histo data points of one measurement or settlement; score points are a third option.
 * A histo data point holds one aggregated value (e.g. max, avg, quantile).
 * Supports suites, i.e. records for multiple trails for combined curves.
 * @author Thomas Eickert
 */
public final class TrailRecord extends Record {
	private final static String		$CLASS_NAME					= TrailRecord.class.getName();
	private final static long			serialVersionUID		= 110124007964748556L;
	private final static Logger		log									= Logger.getLogger($CLASS_NAME);

	public final static String		TRAIL_TEXT_ORDINAL	= "_trailTextOrdinal";						// reference to the selected trail //$NON-NLS-1$

	private final TrailRecordSet	parentTrail;
	private final MeasurementType	measurementType;																			// measurement / settlement / scoregroup are options
	private final SettlementType	settlementType;																				// measurement / settlement / scoregroup are options
	private final ScoreGroupType	scoreGroupType;																				// measurement / settlement / scoregroup are options
	private final TrailSelector		trailSelector;

	private final SuiteRecords		suiteRecords;

	private double								factor							= Double.MIN_VALUE;
	private double								offset							= Double.MIN_VALUE;
	private double								reduction						= Double.MIN_VALUE;

	final DeviceXmlResource				xmlResource					= DeviceXmlResource.getInstance();

	/**
	 * Create a vector for a measurementType to hold data points.
	 * @param newDevice
	 * @param newOrdinal
	 * @param measurementType
	 * @param parentTrail
	 */
	public TrailRecord(IDevice newDevice, int newOrdinal, String newName, MeasurementType measurementType, TrailRecordSet parentTrail,
			int initialCapacity) {
		super(newDevice, newOrdinal, newName, measurementType.getSymbol(), measurementType.getUnit(), measurementType.isActive(), null,
				measurementType.getProperty(), initialCapacity);
		log.fine(() -> measurementType.getName() + " TrailRecord(IDevice newDevice, int newOrdinal, MeasurementType measurementType, TrailRecordSet parentTrail)"); //$NON-NLS-1$
		this.parentTrail = parentTrail;
		super.parent = null; // we currently have no common abstract class of Record and TrailRecord or no common interface of Record and TrailRecord
		this.measurementType = measurementType;
		this.settlementType = null;
		this.scoreGroupType = null;

		this.trailSelector = new TrailSelector(this);
		this.suiteRecords = new SuiteRecords();
	}

	/**
	 * Create a vector for a settlementType to hold data points.
	 * @param newDevice
	 * @param newOrdinal
	 * @param settlementType
	 * @param parentTrail
	 */
	public TrailRecord(IDevice newDevice, int newOrdinal, String newName, SettlementType settlementType, TrailRecordSet parentTrail,
			int initialCapacity) {
		super(newDevice, newOrdinal, newName, settlementType.getSymbol(), settlementType.getUnit(), settlementType.isActive(), null,
				settlementType.getProperty(), initialCapacity);
		log.fine(() -> settlementType.getName() + " TrailRecord(IDevice newDevice, int newOrdinal, SettlementType settlementType, TrailRecordSet parentTrail)"); //$NON-NLS-1$
		this.parentTrail = parentTrail;
		super.parent = null;
		this.measurementType = null;
		this.settlementType = settlementType;
		this.scoreGroupType = null;

		this.trailSelector = new TrailSelector(this);
		this.suiteRecords = new SuiteRecords();
	}

	/**
	 * Create a vector for a scoregroupType to hold all scores of a scoregroup.
	 * The scores are not related to time steps.
	 * @param newDevice
	 * @param newOrdinal
	 * @param scoregroupType
	 * @param parentTrail
	 */
	public TrailRecord(IDevice newDevice, int newOrdinal, String newName, ScoreGroupType scoregroupType, TrailRecordSet parentTrail,
			int initialCapacity) {
		super(newDevice, newOrdinal, newName, scoregroupType.getSymbol(), scoregroupType.getUnit(), scoregroupType.isActive(), null,
				scoregroupType.getProperty(), initialCapacity);
		log.fine(() -> scoregroupType.getName() + " TrailRecord(IDevice newDevice, int newOrdinal, ScoregroupType scoregroupType, TrailRecordSet parentTrail)"); //$NON-NLS-1$
		this.parentTrail = parentTrail;
		super.parent = null;
		this.measurementType = null;
		this.settlementType = null;
		this.scoreGroupType = scoregroupType;

		this.trailSelector = new TrailSelector(this);
		this.suiteRecords = new SuiteRecords();
	}

	@Override
	@Deprecated
	public synchronized Record clone() {
		throw new UnsupportedOperationException("clone"); //$NON-NLS-1$
	}

	@Override
	@Deprecated
	public Record clone(String newName) {
		throw new UnsupportedOperationException("clone"); //$NON-NLS-1$
	}

	@Override
	@Deprecated
	public Record clone(int dataIndex, boolean isFromBegin) {
		throw new UnsupportedOperationException("clone"); //$NON-NLS-1$
	}

	@Override
	@Deprecated
	public Point getDisplayPoint(int measurementPointIndex, int xDisplayOffset, int yDisplayOffset) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public Point getGPSDisplayPoint(int measurementPointIndex, int xDisplayOffset, int yDisplayOffset) {
		throw new UnsupportedOperationException();
	}

	@Deprecated
	@Override
	public synchronized boolean add(Integer point) {
		throw new UnsupportedOperationException(" " + point); //$NON-NLS-1$
	}

	/**
	 * Add a data point to the record and set minimum and maximum.
	 * @param point
	 */
	@Override
	public synchronized void addElement(Integer point) {
		if (point == null) {
			if (this.isEmpty()) {
				this.minValue = Integer.MAX_VALUE;
				this.maxValue = Integer.MIN_VALUE;
			}
		} else {
			if (this.isEmpty())
				this.minValue = this.maxValue = point;
			else {
				if (point > this.maxValue) this.maxValue = point;
				if (point < this.minValue) this.minValue = point;
			}
		}
		super.addElement(point);
		log.finer(() -> this.name + " adding point = " + point); //$NON-NLS-1$
		log.finest(() -> this.name + " minValue = " + this.minValue + " maxValue = " + this.maxValue); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	@Deprecated
	public synchronized Integer set(int index, Integer point) {
		throw new UnsupportedOperationException(" " + index + " " + point); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Clear the record subordinate objects.
	 */
	@Override
	public void clear() {
		this.suiteRecords.clear();
		super.clear();
	}

	/**
	 * @return the point size of a single curve or a suite
	 */
	@Override
	public int realSize() {
		return this.trailSelector.isTrailSuite() ? this.suiteRecords.realSize() : super.realSize();
	}

	@Override // reason is translateValue which accesses the device for offset etc.
	public double getFactor() {
		if (this.factor == Double.MIN_VALUE) {
			this.factor = 1.0;
			PropertyType property = this.getProperty(IDevice.FACTOR);
			if (property != null)
				this.factor = Double.parseDouble(property.getValue());
			else if (this.scoreGroupType != null)
				this.factor = this.scoreGroupType.getFactor();
			else if (this.settlementType != null)
				this.factor = this.settlementType.getFactor();
			else if (this.measurementType != null) {
				this.factor = this.measurementType.getFactor();
			} else
				try {
					this.factor = this.getDevice().getMeasurementFactor(this.parentTrail.getChannelConfigNumber(), this.ordinal);
				} catch (RuntimeException e) {
					// log.log(Level.WARNING, this.name + " use default value for property " + IDevice.FACTOR); // log warning and use default value
				}
		}
		return this.factor;
	}

	@Override // reason is translateValue which accesses the device for offset etc.
	public double getOffset() {
		if (this.offset == Double.MIN_VALUE) {
			this.offset = 0.0;
			PropertyType property = this.getProperty(IDevice.OFFSET);
			if (property != null)
				this.offset = Double.parseDouble(property.getValue());
			else if (this.scoreGroupType != null)
				this.offset = this.scoreGroupType.getOffset();
			else if (this.settlementType != null)
				this.offset = this.settlementType.getOffset();
			else if (this.measurementType != null)
				this.offset = this.measurementType.getOffset();
			else
				try {
					this.offset = this.getDevice().getMeasurementOffset(this.parentTrail.getChannelConfigNumber(), this.ordinal);
				} catch (RuntimeException e) {
					// log.log(Level.WARNING, this.name + " use default value for property " + IDevice.OFFSET); // log warning and use default value
				}
		}
		return this.offset;
	}

	@Override // reason is translateValue which accesses the device for offset etc.
	public double getReduction() {
		if (this.reduction == Double.MIN_VALUE) {
			this.reduction = 0.0;
			PropertyType property = this.getProperty(IDevice.REDUCTION);
			if (property != null)
				this.reduction = Double.parseDouble(property.getValue());
			else if (this.scoreGroupType != null)
				this.reduction = this.scoreGroupType.getReduction();
			else if (this.settlementType != null)
				this.reduction = this.settlementType.getReduction();
			else if (this.measurementType != null)
				this.reduction = this.measurementType.getReduction();
			else
				try {
					String strValue = (String) this.getDevice().getMeasurementPropertyValue(this.parentTrail.getChannelConfigNumber(), this.ordinal, IDevice.REDUCTION);
					if (strValue != null && strValue.length() > 0) this.reduction = Double.parseDouble(strValue.trim().replace(',', '.'));
				} catch (RuntimeException e) {
					// log.log(Level.WARNING, this.name + " use default value for property " + IDevice.REDUCTION); // log warning and use default value
				}
		}
		return this.reduction;
	}

	@Override // reason: formatting of values <= 100 with decimal places; define category based on maxValueAbs AND minValueAbs
	public void setNumberFormat(int newNumberFormat) {
		this.numberFormat = newNumberFormat;
		this.df = new TrailRecordFormatter(this).getDecimalFormat(newNumberFormat);

	}

	@Override // reason is missing scope mode
	public int getMaxValue() {
		return this.maxValue == this.minValue ? this.maxValue + 100 : this.maxValue;
	}

	@Override // reason is missing scope mode
	public int getMinValue() {
		return this.minValue == this.maxValue ? this.minValue - 100 : this.minValue;
	}

	@Override // reason is missing zoom mode
	public double getMaxScaleValue() {
		return this.maxScaleValue;
	}

	@Override // reason is missing zoom mode
	public void setMaxScaleValue(double newMaxScaleValue) {
		this.maxScaleValue = newMaxScaleValue;
	}

	@Override // reason is missing zoom mode
	public double getMinScaleValue() {
		return this.minScaleValue;
	}

	@Override // reason is missing zoom mode
	public void setMinScaleValue(double newMinScaleValue) {
		this.minScaleValue = newMinScaleValue;
	}

	/**
	 * @return true if the record or the suite contains reasonable data which can be displayed
	 */
	@Override // reason is trail record suites with a master record without point values and minValue/maxValue != 0 in case of empty records
	public boolean hasReasonableData() {
		boolean hasReasonableData = false;
		if (this.suiteRecords.getSuiteLength() == 0) {
			hasReasonableData = this.realSize() > 0 && this.minValue != Integer.MAX_VALUE && this.maxValue != Integer.MIN_VALUE && (this.minValue != this.maxValue || this.device.translateValue(this, this.maxValue / 1000.0) != 0.0);
		} else {
			for (SuiteRecord suiteRecord : this.suiteRecords.values()) {
				if (suiteRecord.size() > 0 && suiteRecord.getMinRecordValue() != Integer.MAX_VALUE && suiteRecord.getMaxRecordValue() != Integer.MIN_VALUE && (suiteRecord.getMinRecordValue() != suiteRecord.getMaxRecordValue() || this.device.translateValue(this, suiteRecord.getMaxRecordValue() / 1000.0) != 0.0)) {
					hasReasonableData = true;
					break;
				}
			}
		}
		return hasReasonableData;
	}

	/**
	 * @return true if the record is active and has data values
	 */
	@Override // reason is size for suite records
	public boolean isActive() {
		return this.isActive == null || this.realSize() == 0 ? false : this.isActive;
	}

	/**
	 * @return true if the record is the scale sync master and if the record is for display according to histo display settings
	 */
	@Override // reason are the histo display settings which hide records
	public boolean isScaleVisible() {
		boolean isValidDisplayRecord = this.isMeasurement() || (this.isSettlement() && this.settings.isDisplaySettlements()) || (this.isScoreGroup() && this.settings.isDisplayScores());
		return isValidDisplayRecord && super.isScaleVisible();
	}

	@Override
	@Deprecated
	public String getFormattedMeasureValue(int index) {
		throw new UnsupportedOperationException();
	}

	/**
	 * find the index closest to given time in msec
	 * @param time_ms
	 * @return index nearest to given time
	 */
	@Override
	@Deprecated // replaced by gde.data.TrailRecordSet.getIndex(long)
	public int findBestIndex(double time_ms) {
		throw new UnsupportedOperationException();
	}

	/**
	 * get the time in msec at given horizontal display position
	 * @param xPos of the display point
	 * @return time value in msec
	 */
	@Override
	@Deprecated // replaced by gde.utils.HistoTimeLine.getTimestamp(int)
	public double getHorizontalDisplayPointTime_ms(int xPos) {
		throw new UnsupportedOperationException();
	}

	/**
	 * get the formatted time with unit at given position
	 * @param xPos of the display point
	 * @return string of time value in simple date format HH:ss:mm:SSS
	 */
	@Override
	@Deprecated // replaced by gde.utils.HistoTimeLine.getTimestamp(int)
	public String getHorizontalDisplayPointAsFormattedTimeWithUnit(int xPos) {
		throw new UnsupportedOperationException();
	}

	/**
	 * calculate best fit index in data vector from given display point relative to the (zoomed) display width
	 * @param xPos
	 * @return position integer value
	 */
	@Override
	@Deprecated // replaced by gde.data.TrailRecordSet.getIndex(gde.utils.HistoTimeLine.getTimestamp(int))
	public int getHorizontalPointIndexFromDisplayPoint(int xPos) {
		throw new UnsupportedOperationException();
	}

	/**
	 * query data value (not translated in device units) from a display position point
	 * @param xPos
	 * @return displays yPos in pixel
	 */
	@Override
	@Deprecated // replaced by gde.data.TrailRecord.getVerticalDisplayPos
	public int getVerticalDisplayPointValue(int xPos) {
		throw new UnsupportedOperationException();
	}

	/**
	 * get the formatted scale value corresponding the vertical display point
	 * @param yPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	@Override
	@Deprecated // replaced by gde.data.TrailRecord.getVerticalDisplayPointAsFormattedScaleValue(int)
	public String getVerticalDisplayPointAsFormattedScaleValue(int yPos, Rectangle drawAreaBounds) {
		throw new UnsupportedOperationException();
	}

	/**
	 * get the scale value corresponding the vertical display point
	 * @param yPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	@Override
	@Deprecated // replaced by gde.data.TrailRecord.getVerticalDisplayPos
	public double getVerticalDisplayPointScaleValue(int yPos, Rectangle drawAreaBounds) {
		throw new UnsupportedOperationException();
	}

	/**
	 * get the value corresponding the display point
	 * @param deltaPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	@Override
	@Deprecated // replaced by gde.data.TrailRecord.getVerticalDisplayPos
	public String getVerticalDisplayDeltaAsFormattedValue(int deltaPos, Rectangle drawAreaBounds) {
		throw new UnsupportedOperationException();
	}

	/**
	 * get the slope value of two given points, unit depends on device configuration
	 * @param points describing the time difference (x) as well as the measurement difference (y)
	 * @return formated string of value
	 */
	@Override
	@Deprecated // replaced by getBoundedSlopeValue
	public String getSlopeValue(Point points) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getDisplayScaleFactorValue() {
		return this.displayScaleFactorValue;
	}

	@Override // reason is missing scope mode
	public int getSyncMinValue() {
		return this.syncMinValue == this.syncMaxValue ? this.syncMinValue - 100 : this.syncMinValue;
	}

	public void setSyncMinValue(int syncMinValue) {
		this.syncMinValue = syncMinValue;
	}

	@Override // reason is missing scope mode
	public int getSyncMaxValue() {
		return this.syncMaxValue == this.syncMinValue ? this.syncMaxValue + 100 : this.syncMaxValue;
	}

	public void setSyncMaxValue(int syncMaxValue) {
		this.syncMaxValue = syncMaxValue;
	}

	public double getSyncMasterFactor() {
		return this.syncMasterFactor;
	}

	public TrailRecordSet getParentTrail() {
		return this.parentTrail;
	}

	@Override // reason is missing zoom mode
	public boolean isRoundOut() {
		return this.isRoundOut;
	}

	@Override
	public void setRoundOut(boolean enabled) {
		this.isRoundOut = enabled;
	}

	@Override // reason is missing zoom mode
	public boolean isStartpointZero() {
		return this.isStartpointZero;
	}

	@Override
	public void setStartpointZero(boolean enabled) {
		this.isStartpointZero = enabled;
	}

	@Override // reason is missing zoom mode
	public boolean isStartEndDefined() {
		return this.isStartEndDefined;
	}

	/**
	 * sets the min-max values as displayed 4.0 - 200.5
	 * @param enabled
	 * @param newMinScaleValue
	 * @param newMaxScaleValue
	 */
	@Override // reason is unused channelConfigKey
	public void setStartEndDefined(boolean enabled, double newMinScaleValue, double newMaxScaleValue) {
		this.isStartEndDefined = enabled;
		if (enabled) {
			this.maxScaleValue = this.maxDisplayValue = newMaxScaleValue;
			this.minScaleValue = this.minDisplayValue = newMinScaleValue;
		} else {
			this.maxScaleValue = this.parentTrail.getDevice().translateValue(this, this.maxValue / 1000.0);
			this.minScaleValue = this.parentTrail.getDevice().translateValue(this, this.minValue / 1000.0);
		}
	}

	public boolean isMeasurement() {
		return this.measurementType != null;
	}

	public boolean isSettlement() {
		return this.settlementType != null;
	}

	public boolean isScoreGroup() {
		return this.scoreGroupType != null;
	}

	public MeasurementType getMeasurement() {
		return this.measurementType;
	}

	public SettlementType getSettlement() {
		return this.settlementType;
	}

	public ScoreGroupType getScoregroup() {
		return this.scoreGroupType;
	}

	/**
	 * Supports suites.
	 * @param timeStamp1_ms
	 * @param timeStamp2_ms
	 * @return the portion of the timestamps_ms and aggregated translated values between fromIndex, inclusive, and toIndex, exclusive. (If
	 *         fromIndex and toIndex are equal, the returned list is empty.)
	 */
	public List<Spot<Double>> getSubPoints(long timeStamp1_ms, long timeStamp2_ms) {
		int index1 = this.getIndex(timeStamp1_ms);
		int index2 = this.getIndex(timeStamp2_ms);
		int fromIndex = Math.min(index1, index2);
		int toIndex = Math.max(index1, index2) + 1;

		int recordSize = toIndex - fromIndex;
		List<Spot<Double>> result = new ArrayList<>(recordSize);

		Vector<Integer> points = this.getPoints();
		for (int i = fromIndex; i < toIndex; i++) {
			if (points.elementAt(i) != null) {
				result.add(new Spot<Double>(this.parentTrail.getTime_ms(i), this.device.translateValue(this, points.elementAt(i) / 1000.)));
			}
		}
		log.finer(() -> Arrays.toString(result.toArray()));
		return result;
	}

	/**
	 * @param timeStamp_ms
	 * @return the index fitting exactly to the timeStamp
	 */
	public int getIndex(long timeStamp_ms) {
		return this.parentTrail.getIndex(timeStamp_ms);
	}

	/**
	 * @return the uncloned point values of the record or suite master
	 */
	public Vector<Integer> getPoints() {
		final Vector<Integer> points;
		if (!this.trailSelector.isTrailSuite())
			points = this;
		else
			points = this.suiteRecords.get(this.trailSelector.getTrailType().getSuiteMasterIndex());

		return points;
	}

	public int getSuiteMaxValue() {
		return this.suiteRecords.getSuiteMaxValue() == this.suiteRecords.getSuiteMinValue() ? this.suiteRecords.getSuiteMaxValue() + 100
				: this.suiteRecords.getSuiteMaxValue();
	}

	public int getSuiteMinValue() {
		return this.suiteRecords.getSuiteMaxValue() == this.suiteRecords.getSuiteMinValue() ? this.suiteRecords.getSuiteMinValue() - 100
				: this.suiteRecords.getSuiteMinValue();
	}

	/**
	 * Defines new suite records from the selected trail and the suite master record.
	 * @param initialCapacity
	 */
	public void setSuite(int initialCapacity) {
		this.suiteRecords.clear();

		List<TrailTypes> suiteMembers = this.trailSelector.getTrailType().getSuiteMembers();
		for (int i = 0; i < suiteMembers.size(); i++)
			this.suiteRecords.put(i, new SuiteRecord(suiteMembers.get(i).ordinal(), initialCapacity));
	}

	public String getNameReplacement() {
		return getDeviceXmlReplacement(this.name);
	}

	/**
	 * @return the localized value of the label property from the device channel entry or an empty string.
	 */
	public String getLabel() {
		log.finest(() -> String.format("started")); //$NON-NLS-1$
		String label = this.measurementType != null ? this.measurementType.getLabel() : this.settlementType != null ? this.settlementType.getLabel()
				: this.scoreGroupType.getLabel();
		return getDeviceXmlReplacement(label);
	}

	/**
	 * @param replacementKey
	 * @return the replacement name of the specified key or an empty string if there is no key entry
	 */
	private String getDeviceXmlReplacement(String replacementKey) {
		return replacementKey != null ? this.xmlResource.getReplacement(replacementKey) : GDE.STRING_EMPTY;
	}

	public TrailSelector getTrailSelector() {
		return this.trailSelector;
	}

	public SuiteRecords getSuiteRecords() {
		return this.suiteRecords;
	}

}
