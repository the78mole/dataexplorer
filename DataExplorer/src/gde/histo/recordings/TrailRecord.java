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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import gde.GDE;
import gde.data.AbstractRecord;
import gde.data.CommonRecord;
import gde.device.IChannelItem;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.device.TrailTypes;
import gde.device.resource.DeviceXmlResource;
import gde.histo.cache.ExtendedVault;
import gde.histo.datasources.HistoSet;
import gde.histo.utils.Spot;
import gde.histo.utils.UniversalQuantile;
import gde.log.Logger;
import gde.ui.DataExplorer;

/**
 * Hold histo data points of one measurement or settlement; score points are a third option.
 * A histo data point holds one aggregated value (e.g. max, avg, quantile).
 * Supports suites, i.e. records for multiple trails for combined curves.
 * @author Thomas Eickert
 */
public abstract class TrailRecord extends CommonRecord {
	private final static String				$CLASS_NAME					= TrailRecord.class.getName();
	private final static long					serialVersionUID		= 110124007964748556L;
	private final static Logger				log									= Logger.getLogger($CLASS_NAME);

	public final static String				TRAIL_TEXT_ORDINAL	= "_trailTextOrdinal";						// reference to the selected trail //$NON-NLS-1$

	protected final IChannelItem			channelItem;
	protected final TrailSelector			trailSelector;

	protected final DeviceXmlResource	xmlResource				= DeviceXmlResource.getInstance();

	/**
	 * If a suite trail is chosen the values are added to suite records and the trail record does not get values.
	 */
	protected final SuiteRecords			suiteRecords			= new SuiteRecords();
	/**
	 * The real maximum values of all vaults added to this record.
	 */
	protected final List<Integer>			vaultMaximums			= new ArrayList<>();
	/**
	 * The real minimum values of all vaults added to this record.
	 */
	protected final List<Integer>			vaultMinimums			= new ArrayList<>();

	protected Double									syncSummaryMax		= null;
	protected Double									syncSummaryMin		= null;

	protected double									factor						= Double.MIN_VALUE;
	protected double									offset						= Double.MIN_VALUE;
	protected double									reduction					= Double.MIN_VALUE;

	protected TrailRecord(IChannelItem channelItem, int newOrdinal, TrailRecordSet parentTrail, int initialCapacity) {
		super(DataExplorer.getInstance().getActiveDevice(), newOrdinal, channelItem.getName(), channelItem.getSymbol(), channelItem.getUnit(),
				channelItem.isActive(), null, channelItem.getProperty(), initialCapacity);
		this.channelItem = channelItem;
		super.parent = parentTrail;

		this.trailSelector = new TrailSelector(this);
		log.fine(() -> channelItem.toString());
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
		this.vaultMaximums.clear();
		this.vaultMinimums.clear();
		super.clear();
	}

	/**
	 * @return the point size of a single curve or a suite (calls realSize())
	 */
	@Override
	public synchronized int size() {
		return realSize();
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
			else
				this.factor = this.channelItem.getFactor();
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
			else
				this.offset = this.channelItem.getOffset();
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
			else
				this.reduction = this.channelItem.getReduction();
		}
		return this.reduction;
	}

	@Override // reason: formatting of values <= 100 with decimal places; define category based on maxValueAbs AND minValueAbs
	public void setNumberFormat(int newNumberFormat) {
		this.numberFormat = newNumberFormat;
		this.df = new TrailRecordFormatter(this).getDecimalFormat(newNumberFormat);

	}

	@Override
	public int getMaxValue() {
		return this.maxValue == this.minValue ? this.maxValue + 100 : this.maxValue;
	}

	@Override
	public int getMinValue() {
		return this.minValue == this.maxValue ? this.minValue - 100 : this.minValue;
	}

	@Override
	public double getMaxScaleValue() {
		return this.maxScaleValue;
	}

	@Override
	public void setMaxScaleValue(double newMaxScaleValue) {
		this.maxScaleValue = newMaxScaleValue;
	}

	@Override
	public double getMinScaleValue() {
		return this.minScaleValue;
	}

	@Override
	public void setMinScaleValue(double newMinScaleValue) {
		this.minScaleValue = newMinScaleValue;
	}

	@Override
	public void setSyncedMinMaxDisplayValues(double newMinValue, double newMaxValue) {
		this.minDisplayValue = RecordingsCollector.decodeVaultValue(this, newMinValue);
		this.maxDisplayValue = RecordingsCollector.decodeVaultValue(this, newMaxValue);

		if (this.getAbstractParent().isOneOfSyncableRecord(this.name)) {
			for (AbstractRecord tmpRecord : this.getAbstractParent().getScaleSyncedRecords(this.getAbstractParent().getSyncMasterRecordOrdinal(this.name))) {
				TrailRecord record = (TrailRecord) tmpRecord;
				record.minDisplayValue = this.minDisplayValue;
				record.maxDisplayValue = this.maxDisplayValue;
			}
		}
		log.fine(getName() + " data limit  -> yMinValue = " + newMinValue + "; yMaxValue = " + newMaxValue); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @return true if the record or the suite contains reasonable data which can be displayed
	 */
	@Override // reason is trail record suites with a master record without point values and minValue/maxValue != 0 in case of empty records
	public boolean hasReasonableData() {
		boolean hasReasonableData = false;
		if (this.suiteRecords.getSuiteLength() == 0) {
			hasReasonableData = this.realSize() > 0 && this.minValue != Integer.MAX_VALUE && this.maxValue != Integer.MIN_VALUE //
					&& (this.minValue != this.maxValue || RecordingsCollector.decodeVaultValue(this, this.maxValue / 1000.0) != 0.0);
		} else {
			for (SuiteRecord suiteRecord : this.suiteRecords.values()) {
				if (suiteRecord.size() > 0 && suiteRecord.getMinRecordValue() != Integer.MAX_VALUE && suiteRecord.getMaxRecordValue() != Integer.MIN_VALUE //
						&& (suiteRecord.getMinRecordValue() != suiteRecord.getMaxRecordValue() || RecordingsCollector.decodeVaultValue(this, suiteRecord.getMaxRecordValue() / 1000.0) != 0.0)) {
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
	 * @return true if the record is not suppressed by histo display settings which hide records
	 */
	public boolean isAllowedBySetting() {
		return false;
	}

	/**
	 * @return true if the record is the scale sync master and if the record is for display according to histo display settings
	 */
	@Override // reason are the histo display settings which hide records
	public boolean isScaleVisible() {
		return super.isScaleVisible();
	}

	@Override
	@Deprecated
	public double getTime_ms(int index) {
		throw new UnsupportedOperationException();
// return this.parent.timeStep_ms.getTime_ms(index);
	}

	@Override
	public double getDisplayScaleFactorValue() {
		return this.displayScaleFactorValue;
	}

	/**
	 * Take the current max/minValues form this record and recalculate the synced max/minValues.
	 * Support suites.
	 */
	public void setSyncMaxMinValue() {
		if (getTrailSelector().isTrailSuite()) {
			int suiteMaxValue = suiteRecords.getSuiteMaxValue();
			int suiteMinValue = suiteRecords.getSuiteMinValue();
			int tmpMaxValue = suiteMaxValue == suiteMinValue ? suiteMaxValue + 100 : suiteMaxValue;
			int tmpMinValue = suiteMaxValue == suiteMinValue ? suiteMinValue - 100 : suiteMinValue;
			syncMaxValue = (int) (tmpMaxValue * getSyncMasterFactor());
			syncMinValue = (int) (tmpMinValue * getSyncMasterFactor());
		} else {
			syncMaxValue = (int) (getMaxValue() * getSyncMasterFactor());
			syncMinValue = (int) (getMinValue() * getSyncMasterFactor());
		}
		log.finer(() -> getName() + "   syncMin = " + getSyncMinValue() + "; syncMax = " + getSyncMaxValue()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public int getSyncMinValue() {
		return this.syncMinValue == this.syncMaxValue ? this.syncMinValue - 100 : this.syncMinValue;
	}

	public void setSyncMinValue(int value) {
		this.syncMinValue = value;
	}

	@Override
	public int getSyncMaxValue() {
		return this.syncMaxValue == this.syncMinValue ? this.syncMaxValue + 100 : this.syncMaxValue;
	}

	public void setSyncMaxValue(int value) {
		this.syncMaxValue = value;
	}

	public double getSyncMasterFactor() {
		return this.syncMasterFactor;
	}

	@Deprecated // not supported
	@Override
	public boolean isRoundOut() {
		// return this.isRoundOut;
		return false;
	}

	@Deprecated // not supported
	@Override
	public void setRoundOut(boolean enabled) {
		// this.isRoundOut = enabled;
	}

	@Override
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
			this.maxScaleValue = RecordingsCollector.decodeVaultValue(this, this.maxValue / 1000.0);
			this.minScaleValue = RecordingsCollector.decodeVaultValue(this, this.minValue / 1000.0);
		}
	}

	/**
	 * @return the parent
	 */
	public TrailRecordSet getParentTrail() {
		return (TrailRecordSet) this.parent;
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
				result.add(new Spot<Double>(this.parent.getTime_ms(i), RecordingsCollector.decodeVaultValue(this, points.elementAt(i) / 1000.)));
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
		return this.getParentTrail().getIndex(timeStamp_ms);
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

	/**
	 * Defines new suite records from the selected trail and the suite master record.
	 * @param initialCapacity
	 */
	public void setSuite(int initialCapacity) {
		this.suiteRecords.clear();

		List<TrailTypes> suiteMembers = this.trailSelector.getTrailType().getSuiteMembers();
		for (int i = 0; i < suiteMembers.size(); i++) {
			this.suiteRecords.put(i, new SuiteRecord(suiteMembers.get(i).ordinal(), initialCapacity));
		}
	}

	public String getNameReplacement() {
		return getDeviceXmlReplacement(this.name);
	}

	/**
	 * @return the localized value of the label property from the device channel entry or an empty string.
	 */
	public String getLabel() {
		return getDeviceXmlReplacement(this.channelItem.getLabel());
	}

	public String getTableRowHeader() {
		return getUnit().length() > 0 ? (getNameReplacement() + GDE.STRING_BLANK_LEFT_BRACKET + getUnit() + GDE.STRING_RIGHT_BRACKET).intern()
				: getNameReplacement().intern();
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

	@Override
	public String getFormattedScaleValue(double finalValue) {
		return new TrailRecordFormatter(this).getScaleValue(finalValue);
	}

	/* (non-Javadoc)
	 * @see gde.data.AbstractRecord#getRealDf()
	 */
	@Override
	public DecimalFormat getRealDf() {
		return this.df;
	}

	/* (non-Javadoc)
	 * @see gde.data.AbstractRecord#setRealDf(java.text.DecimalFormat)
	 */
	@Override
	public void setRealDf(DecimalFormat realDf) {
		this.df = realDf;
	}

	public IChannelItem getChannelItem() {
		return this.channelItem;
	}

	/**
	 * Analyze device configuration entries to find applicable trail types.
	 * Build applicable trail type lists for display purposes.
	 * Use device settings trigger texts for trigger trail types and score labels for score trail types; message texts otherwise.
	 */
	public abstract void setApplicableTrailTypes();

	/**
	 * @param vault
	 * @param trailType is the requested trail type which may differ from the selected trail type (e.g. suites)
	 * @return the point value
	 */
	public abstract Integer getVaultPoint(ExtendedVault vault, TrailTypes trailType);

	@Override
	public int getSyncMasterRecordOrdinal() {
		final PropertyType syncProperty = getProperty(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value());
		if (syncProperty != null && !syncProperty.getValue().isEmpty()) {
			return Integer.parseInt(syncProperty.getValue());
		} else {
			return -1;
		}
	}

	public void addExtrema(Integer vaultMinValue, Integer vaultMaxValue) {
		this.vaultMaximums.add(vaultMaxValue);
		this.vaultMinimums.add(vaultMinValue);
	}

	/**
	 * Determine the extrema max/minValues from this record and recalculate the synced summary max/minValues.
	 */
	public void setSyncSummaryMinMax() {
		double[] minMax = determineSummaryMinMax();
		if (minMax.length == 0) {
			resetSyncSummaryMinMax();
		} else {
			double max = minMax[0] == minMax[1] ? minMax[1] * 1.1 : minMax[1];
			double min = minMax[0] == minMax[1] ? minMax[0] * 0.9 : minMax[0];
			setSyncSummaryMinMax(min, max);
		}
	}

	public void setSyncSummaryMinMax(double newMin, double newMax) {
		this.syncSummaryMin = newMin;
		this.syncSummaryMax = newMax;
		log.finer(() -> getName() + " syncSummaryMin=" + newMin + " syncSummaryMax=" + newMax);
	}

	public void resetSyncSummaryMinMax() {
		this.syncSummaryMin = this.syncSummaryMax = null;
	}

	/**
	 * Provide the extremum values based on all the vaults which have been added to this record up to now.
	 * @return the decoded extrema after removing outliers: {minExtremumValue, maxExtremumValue} or an empty array
	 */
	protected double[] determineSummaryMinMax() {
		if (vaultMaximums.isEmpty() || vaultMinimums.isEmpty()) return new double[0];

		List<Double> decodedMaximums = vaultMaximums.stream().map(i -> RecordingsCollector.decodeVaultValue(this, i / 1000.)).collect(Collectors.toList());
		List<Double> decodedMinimums = vaultMinimums.stream().map(i -> RecordingsCollector.decodeVaultValue(this, i / 1000.)).collect(Collectors.toList());

		UniversalQuantile<Double> maxQuantile = new UniversalQuantile<>(decodedMaximums, true, //
				HistoSet.SUMMARY_OUTLIER_SIGMA_DEFAULT, HistoSet.SUMMARY_OUTLIER_RANGE_FACTOR_DEFAULT);
		UniversalQuantile<Double> minQuantile = new UniversalQuantile<>(decodedMinimums, true, //
				HistoSet.SUMMARY_OUTLIER_SIGMA_DEFAULT, HistoSet.SUMMARY_OUTLIER_RANGE_FACTOR_DEFAULT);

		double[] result = new double[] { minQuantile.getQuartile0(), maxQuantile.getQuartile4() };
		log.finest(() -> getName() + " " + Arrays.toString(result));
		return result;
	}

}
