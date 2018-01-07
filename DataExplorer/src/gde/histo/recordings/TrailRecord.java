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

import static java.util.logging.Level.FINER;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.stream.Collectors;

import gde.GDE;
import gde.config.Settings;
import gde.data.CommonRecord;
import gde.data.Record.DataType;
import gde.device.IChannelItem;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.device.TrailTypes;
import gde.device.resource.DeviceXmlResource;
import gde.histo.cache.ExtendedVault;
import gde.histo.datasources.HistoSet;
import gde.histo.recordings.TrailRecordSet.Outliers;
import gde.histo.ui.data.SummarySpots;
import gde.histo.utils.ElementaryQuantile;
import gde.histo.utils.Spot;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.utils.MathUtils;

/**
 * Hold histo data points of one measurement or settlement; score points are a third option.
 * A histo data point holds one aggregated value (e.g. max, avg, quantile).
 * Supports suites, i.e. records for multiple trails for combined curves.
 * @author Thomas Eickert
 */
public abstract class TrailRecord extends CommonRecord {
	private final static String		$CLASS_NAME					= TrailRecord.class.getName();
	private final static long			serialVersionUID		= 110124007964748556L;
	private final static Logger		log									= Logger.getLogger($CLASS_NAME);

	protected final static String	TRAIL_TEXT_ORDINAL	= "_trailTextOrdinal";					// reference to the selected trail //$NON-NLS-1$

	/**
	 * Collect input data for the trail record and subordinate objects.
	 * Support initial collection and collections after user input (e.g. trail type selection).
	 * @author Thomas Eickert (USER)
	 */
	final class RecordCollector {
		@SuppressWarnings("hiding")
		private final Logger log = Logger.getLogger(RecordCollector.class.getName());

		/**
		 * Set the data points for one single trail record.
		 * The record takes the selected trail type / score data from the trail record vault and populates its data.
		 * Support suites.
		 */
		synchronized void addVaults(TreeMap<Long, List<ExtendedVault>> histoVaults) {
			if (!getTrailSelector().isTrailSuite()) {
				histoVaults.values().stream().flatMap(Collection::stream).forEach(v -> {
					addElement(getVaultPoint(v, getTrailSelector().getTrailOrdinal()));
				});
			} else {
				setSuite(histoVaults.size());
				histoVaults.values().stream().flatMap(Collection::stream).forEach(v -> {
					addVaultToSuite(v);
				});
			}
			log.finer(() -> " " + getTrailSelector());
		}

		/**
		 * Take those data points from the histo vault which are assigned to the selected trail type.
		 * Supports trail suites.
		 */
		private void addVaultToSuite(ExtendedVault histoVault) {
			List<TrailTypes> suiteMembers = trailSelector.getTrailType().getSuiteMembers();

			if (trailSelector.getTrailType().isBoxPlot()) {
				for (int i = 0; i < trailSelector.getTrailType().getSuiteMembers().size(); i++) {
					suiteRecords.get(i).addElement(getVaultPoint(histoVault, suiteMembers.get(i).ordinal()));
				}
			} else {
				int tmpSummationFactor = 0;
				int masterPoint = 0; // this is the base value for adding or subtracting standard deviations

				for (int i = 0; i < suiteMembers.size(); i++) {
					Integer point = getVaultPoint(histoVault, suiteMembers.get(i).ordinal());
					if (point == null) {
						suiteRecords.get(i).addElement(null);
					} else {
						tmpSummationFactor = getSummationFactor(suiteMembers.get(i), tmpSummationFactor);
						if (tmpSummationFactor == 0)
							masterPoint = point; // use in the next iteration if summation is necessary, e.g. avg+2*sd
						else
							point = masterPoint + tmpSummationFactor * point * 2;

						suiteRecords.get(i).addElement(point);
					}
					if (log.isLoggable(FINER))
						log.log(FINER, String.format(" %s trail %3d  %s  %d minVal=%d maxVal=%d", getName(), trailSelector.getTrailOrdinal(), histoVault.getLogFilePath(), point, suiteRecords.get(i).getMinRecordValue(), suiteRecords.get(i).getMaxRecordValue()));
				}
			}
			log.log(FINER, " ", trailSelector);
		}

		/**
		 * Support adding / subtracting trail values from a preceding suite master record.
		 * @param trailType
		 * @param previousFactor the summation factor from the last iteration
		 * @return the alternating -1/+1 factor for summation trail types; 0 otherwise
		 */
		private int getSummationFactor(TrailTypes trailType, int previousFactor) {
			if (trailType.isAlienValue()) {
				return previousFactor == 0 ? -1 : previousFactor * -1;
			} else {
				return 0;
			}
		}

		/**
		 * Set the most specific common datatype of all vault entries.</br>
		 * Pls note that the vaults may come from mixed sources (OSD files, native files) and that
		 * device settings or rules for determining the datatype might have changed in the meantime.
		 */
		public void setUnifiedDataType(TreeMap<Long, List<ExtendedVault>> histoVaults) {
			List<DataType> dataTypes = histoVaults.values().parallelStream().flatMap(Collection::stream).map(v -> getVaultDataType(v)) //
					.filter(Objects::nonNull).distinct().limit(2).collect(Collectors.toList());
			DataType tmpDataType = dataTypes.size() == 1 ? dataTypes.get(0) : DataType.DEFAULT;

			if (tmpDataType == DataType.DEFAULT) {
				DataType guess = DataType.guess(TrailRecord.this.getName());
				if (guess != null) tmpDataType = guess;
			}
			TrailRecord.this.setDataType(tmpDataType);
		}
	}

	/**
	 * Data for the life cycle of a graphics composite drawing.
	 */
	public final class Graphics { // todo class might be better independent from TrailRecord

		private int[] numberTickMarks;

		public int[] getNumberTickMarks() {
			return this.numberTickMarks;
		}

		public void setNumberTickMarks(int[] numberTickMarks) {
			this.numberTickMarks = numberTickMarks;
		}

	}

	/**
	 * Data for the life cycle of a summary composite drawing.
	 */
	public final class Summary { // todo class might be better independent from TrailRecord
		private final Logger				log			= Logger.getLogger(Summary.class.getName());

		private final SummarySpots	summarySpots;

		// summary min/max only depend on the vault q0/q4 values; a synced record has synced min/max values in these fields
		Double											syncMax	= null;
		Double											syncMin	= null;

		private Outliers[]					warningMinMaxValues;

		public Summary(SummarySpots summarySpots) {
			this.summarySpots = summarySpots;
		}

		public void clear() {
			warningMinMaxValues = null;
			resetSyncMinMax();
		}

		/**
		 * Determine and set the q0/q4 max/minValues for the summary window from this record.
		 */
		public void setSyncMinMax(int recencyLimit) {
			double[] minMax = defineExtrema();
			double[] recentMinMax = defineRecentMinMax(recencyLimit);
			if (minMax.length == 0) {
				resetSyncMinMax();
			} else {
				setSyncMinMax(Math.min(minMax[0], recentMinMax[0]), Math.max(minMax[1], recentMinMax[1]));
			}
		}

		public double defineScaleMin() { // todo consider caching
			log.finer(() -> "'" + getName() + "'  syncSummaryMin=" + getSyncMin() + " syncSummaryMax=" + getSyncMax());
			return MathUtils.floorStepwise(getSyncMin(), getSyncMax() - getSyncMin());
		}

		public double defineScaleMax() { // todo consider caching
			return MathUtils.ceilStepwise(getSyncMax(), getSyncMax() - getSyncMin());
		}

		public Outliers[] getMinMaxWarning() {
			if (warningMinMaxValues == null) {
				warningMinMaxValues = getParent().getPickedVaults().defineMinMaxWarning(getName(), Settings.getInstance().getWarningCount());
			}
			return warningMinMaxValues;
		}

		/**
		 * @return true if the record or the suite contains reasonable min max data
		 */
		public boolean hasReasonableMinMax() {
			return syncMin == null && syncMax == null || !HistoSet.fuzzyEquals(syncMin, syncMax);
		}

		public void setSyncMinMax(double newMin, double newMax) {
			syncMin = newMin;
			syncMax = newMax;
			log.finer(() -> getName() + " syncSummaryMin=" + newMin + " syncSummaryMax=" + newMax);
		}

		public void resetSyncMinMax() {
			syncMin = syncMax = null;
		}

		private Double getSyncMax() {
			return syncMax != null ? syncMax : 0.;
		}

		private double getSyncMin() {
			return syncMin != null ? syncMin : 0.;
		}

		public SummarySpots getSummarySpots() {
			return summarySpots;
		}

	}

	protected final DeviceXmlResource			xmlResource		= DeviceXmlResource.getInstance();

	protected final IChannelItem					channelItem;

	/**
	 * If a suite trail is chosen the values are added to suite records and the trail record does not get values.
	 */
	protected final SuiteRecords					suiteRecords	= new SuiteRecords();

	protected double											factor				= Double.MIN_VALUE;
	protected double											offset				= Double.MIN_VALUE;
	protected double											reduction			= Double.MIN_VALUE;

	protected TrailSelector								trailSelector;
	protected ElementaryQuantile<Double>	quantile;
	protected Graphics										graphics;
	protected Summary											summary;

	protected TrailRecord(IChannelItem channelItem, int newOrdinal, TrailRecordSet parentTrail, int initialCapacity) {
		super(DataExplorer.getInstance().getActiveDevice(), newOrdinal, channelItem.getName(), channelItem.getSymbol(), channelItem.getUnit(),
				channelItem.isActive(), null, channelItem.getProperty(), initialCapacity);
		this.channelItem = channelItem;
		this.parent = parentTrail;

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
		this.quantile = null;
		this.graphics = null;
		this.summary = null;
		super.clear();
	}

	/**
	 * @return the point size of a single curve or a suite
	 */
	@Override
	public synchronized int size() {
		return this.trailSelector.isTrailSuite() ? this.suiteRecords.realSize() : super.realSize();
	}

	/**
	 * @return the point size of a single curve or a suite element (calls realSize())
	 */
	@Override
	public int realSize() {
		return super.realSize();
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
	public double getMinScaleValue() {
		return this.minScaleValue;
	}

	@Override
	public void setMinMaxScaleValue(double minScaleValue, double maxScaleValue) {
		this.minScaleValue = minScaleValue;
		this.maxScaleValue = maxScaleValue;
		log.fine(() -> getName() + " minScaleValue = " + minScaleValue + "; maxScaleValue = " + minScaleValue); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void setSyncedMinMaxDisplayValues(double newMinValue, double newMaxValue) {
		this.minDisplayValue = HistoSet.decodeVaultValue(this, newMinValue);
		this.maxDisplayValue = HistoSet.decodeVaultValue(this, newMaxValue);

		if (this.getParent().isOneOfSyncableRecord(this.name)) {
			for (TrailRecord record : this.getParent().getScaleSyncedRecords(this.getParent().getSyncMasterRecordOrdinal(this.name))) {
				record.minDisplayValue = this.minDisplayValue;
				record.maxDisplayValue = this.maxDisplayValue;
			}
		}
		log.fine(getName() + getName() + " yMinValue = " + newMinValue + "; yMaxValue = " + newMaxValue); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @return true if the record or the suite contains reasonable data which can be displayed
	 */
	@Override
	public boolean hasReasonableData() {
		boolean hasReasonableData = false;
		if (this.size() > 0) {
			double[] extrema = defineExtrema();
			hasReasonableData = !HistoSet.fuzzyEquals(extrema[0], extrema[1]) || !HistoSet.fuzzyEquals(extrema[0], 0.);
			log.log(Level.FINE, name, hasReasonableData);
		}
		return hasReasonableData;
	}

	/**
	 * @return true if the record is active and has data values
	 */
	@Override // reason is size for suite records
	public boolean isActive() {
		return this.isActive == null || this.size() == 0 ? false : this.isActive;
	}

	/**
	 * @return true if the record is not suppressed by histo display settings which hide records
	 */
	protected abstract boolean isAllowedBySetting();

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
		log.finer(() -> getName() + "  syncMin = " + getSyncMinValue() + "; syncMax = " + getSyncMaxValue()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public int getSyncMinValue() {
		return this.syncMinValue == this.syncMaxValue ? this.syncMinValue - 100 : this.syncMinValue;
	}

	@Override
	public int getSyncMaxValue() {
		return this.syncMaxValue == this.syncMinValue ? this.syncMaxValue + 100 : this.syncMaxValue;
	}

	public double getSyncMasterFactor() {
		return this.syncMasterFactor;
	}

	/**
	 ** @return always false as trail records do not use this mechanism
	 ** @see gde.data.CommonRecord#isRoundOut()
	 */
	@Deprecated // not supported
	@Override
	public boolean isRoundOut() {
		return false;
	}

	/**
	 * @return the template value which is passed through
	 */
	public boolean isRealRoundOut() {
		return this.isRoundOut;
	}

	@Override
	public void setRoundOut(boolean enabled) {
		this.isRoundOut = enabled;
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
			this.maxScaleValue = HistoSet.decodeVaultValue(this, this.maxValue / 1000.0);
			this.minScaleValue = HistoSet.decodeVaultValue(this, this.minValue / 1000.0);
		}
	}

	/**
	 * @return the parent
	 */
	public TrailRecordSet getParent() {
		return (TrailRecordSet) this.parent;
	}

	/**
	 * @param timeStamp1_ms is a timeStamp fitting exactly to an index
	 * @param timeStamp2_ms is a timeStamp fitting exactly to an index
	 * @return startIndex, inclusive, and endIndex, inclusive.<br/>
	 *         If the timestamps are equal, the returned values are the same.
	 */
	public int[] defineRangeIndices(long timeStamp1_ms, long timeStamp2_ms) {
		int index1 = this.getIndex(timeStamp1_ms);
		int index2 = this.getIndex(timeStamp2_ms);
		return new int[] { Math.min(index1, index2), Math.max(index1, index2) };
	}

	/**
	 * Supports suites.
	 * @param fromIndex inclusive
	 * @param toIndex exclusive
	 * @return the range of spots (timestamps_ms, aggregated translated value) in timestamp desc order.<br/>
	 *         If fromIndex and toIndex are equal, the returned list is empty.
	 */
	public List<Spot<Double>> getSubPoints(int fromIndex, int toIndex) {
		int recordSize = toIndex - fromIndex;
		List<Spot<Double>> result = new ArrayList<>(recordSize);

		Vector<Integer> points = this.getPoints();
		for (int i = fromIndex; i < toIndex; i++) {
			if (points.elementAt(i) != null) {
				result.add(new Spot<Double>(this.parent.getTime_ms(i), HistoSet.decodeVaultValue(this, points.elementAt(i) / 1000.)));
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
		return this.getParent().getIndex(timeStamp_ms);
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

	@Override
	public DecimalFormat getRealDf() {
		return this.df;
	}

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
	 * @param trailOrdinal is the requested trail ordinal number which may differ from the selected trail type (e.g. suites)
	 * @return the point value
	 */
	public abstract Integer getVaultPoint(ExtendedVault vault, int trailOrdinal);

	@Override
	public int getSyncMasterRecordOrdinal() {
		final PropertyType syncProperty = getProperty(MeasurementPropertyTypes.SCALE_SYNC_REF_ORDINAL.value());
		if (syncProperty != null && !syncProperty.getValue().isEmpty()) {
			return Integer.parseInt(syncProperty.getValue());
		} else {
			return -1;
		}
	}

	protected double[] defineRecentMinMax(int limit) {
		TrailTypes trailType = getTrailSelector().getTrailType();
		if (trailType.isAlienValue()) {
			return getParent().getPickedVaults().defineRecentAlienMinMax(getName(), trailType, limit);
		} else {
			return getParent().getPickedVaults().defineRecentStandardMinMax(getName(), limit);
		}
	}

	protected double[] defineExtrema() { // todo consider caching this result
		TrailTypes trailType = getTrailSelector().getTrailType();
		if (trailType.isAlienValue()) {
			return getParent().getPickedVaults().defineAlienExtrema(getName(), trailType);
		} else {
			return getParent().getPickedVaults().defineStandardExtrema(getName());
		}
	}

	/**
	 * @return all the decoded record values including nulls
	 */
	protected List<Double> getDecodedNotNullValues() {
		List<Double> decodedValues = new ArrayList<>();

		final Vector<Integer> record;
		if (this.trailSelector.isTrailSuite()) {
			record = this.suiteRecords.get(this.trailSelector.getTrailType().getSuiteMasterIndex());
		} else {
			record = this;
		}

		for (Integer value : record) { // loops without calling the overridden getter
			if (value != null) decodedValues.add(HistoSet.decodeVaultValue(this, value / 1000.));
		}
		return decodedValues;
	}

	protected void defineQuantile() {
		quantile = new ElementaryQuantile<>(getDecodedNotNullValues(), true);
		log.finest(() -> name + " size=" + quantile.getSize() + " UpperWhisker=" + quantile.getQuantileUpperWhisker());
	}

	public ElementaryQuantile<Double> getQuantile() {
		if (quantile == null) defineQuantile();
		return quantile;
	}

	/**
	 * @return the points for the q0/q4 trails; for score groups w/o min/max scores take the first score
	 */
	protected Integer[] getExtremumTrailPoints(ExtendedVault vault) {
		int[] extremumOrdinals = this.trailSelector.getExtremumTrailsOrdinals();
		return new Integer[] { getVaultPoint(vault, extremumOrdinals[0]), getVaultPoint(vault, extremumOrdinals[1]) };
	}

	/**
	 * (Re)Build the data contents.
	 */
	public synchronized void initializeFromVaults(TreeMap<Long, List<ExtendedVault>> histoVaults) {
		clear();
		RecordCollector collector = new RecordCollector();
		collector.addVaults(histoVaults);
		collector.setUnifiedDataType(histoVaults);
	}

	public Graphics getGraphics() {
		if (graphics == null) {
			graphics = new Graphics();
		}
		return this.graphics;
	}

	public Summary getSummary() {
		if (summary == null) {
			summary = new Summary(new SummarySpots(this));
		}
		return this.summary;
	}

	/**
	 * @return false if the record does not exist or has no outliers
	 */
	public abstract boolean hasVaultOutliers(ExtendedVault vault);

	public abstract double[] getVaultScraps(ExtendedVault vault);

	public abstract double[] getVaultOutliers(ExtendedVault vault);

	public abstract boolean hasVaultScraps(ExtendedVault vault);

	public abstract DataType getVaultDataType(ExtendedVault vault);
}
