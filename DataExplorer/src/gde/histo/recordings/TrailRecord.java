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

import static gde.histo.datasources.HistoSet.SUMMARY_OUTLIER_RANGE_FACTOR;
import static gde.histo.datasources.HistoSet.SUMMARY_OUTLIER_SIGMA;
import static gde.histo.utils.UniversalQuantile.BoxplotItems.LOWER_WHISKER;
import static gde.histo.utils.UniversalQuantile.BoxplotItems.QUARTILE1;
import static gde.histo.utils.UniversalQuantile.BoxplotItems.QUARTILE3;
import static gde.histo.utils.UniversalQuantile.BoxplotItems.UPPER_WHISKER;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gde.GDE;
import gde.data.CommonRecord;
import gde.device.IChannelItem;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.device.TrailTypes;
import gde.device.resource.DeviceXmlResource;
import gde.histo.cache.ExtendedVault;
import gde.histo.datasources.HistoSet;
import gde.histo.ui.data.SummarySpots;
import gde.histo.ui.data.SummarySpots.OutlierWarning;
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
	private final static String					$CLASS_NAME					= TrailRecord.class.getName();
	private final static long						serialVersionUID		= 110124007964748556L;
	private final static Logger					log									= Logger.getLogger($CLASS_NAME);

	protected final static String				TRAIL_TEXT_ORDINAL	= "_trailTextOrdinal";						// reference to the selected trail //$NON-NLS-1$

	protected final DeviceXmlResource		xmlResource					= DeviceXmlResource.getInstance();

	protected final IChannelItem				channelItem;

	protected TrailSelector							trailSelector;

	/**
	 * If a suite trail is chosen the values are added to suite records and the trail record does not get values.
	 */
	protected final SuiteRecords				suiteRecords				= new SuiteRecords();

	// summary min/max only depend on the vault q0/q4 values; a synced record has synced min/max values in these fields
	protected Double										summaryMax					= null;
	protected Double										summaryMin					= null;

	protected double										factor							= Double.MIN_VALUE;
	protected double										offset							= Double.MIN_VALUE;
	protected double										reduction						= Double.MIN_VALUE;

	protected UniversalQuantile<Double>	quantile;
	protected SummarySpots							summarySpots;

	protected TrailRecord(IChannelItem channelItem, int newOrdinal, TrailRecordSet parentTrail, int initialCapacity) {
		super(DataExplorer.getInstance().getActiveDevice(), newOrdinal, channelItem.getName(), channelItem.getSymbol(), channelItem.getUnit(),
				channelItem.isActive(), null, channelItem.getProperty(), initialCapacity);
		this.channelItem = channelItem;
		super.parent = parentTrail;

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
		this.summarySpots = null;
		resetSummaryMinMax();
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
			double[] summaryExtrema = defineSummaryExtrema();
			hasReasonableData = !HistoSet.fuzzyEquals(summaryExtrema[0], summaryExtrema[1]) || !HistoSet.fuzzyEquals(summaryExtrema[0], 0.);
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

	public String getFormattedRangeValue(double finalValue, double comparisonValue) {
		return new TrailRecordFormatter(this).getRangeValue(finalValue, comparisonValue);
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

	public Double getSyncSummaryMax() {
		return summaryMax != null ? summaryMax : 0.;
	}

	public double getSyncSummaryMin() {
		return summaryMin != null ? summaryMin : 0.;
	}

	/**
	 * Determine and set the q0/q4 max/minValues for the summary window from this record.
	 */
	public void setSummaryMinMax() {
		double[] minMax = defineSummaryExtrema();
		if (minMax.length == 0) {
			resetSummaryMinMax();
		} else {
			setSummaryMinMax(minMax[0], minMax[1]);
		}
	}

	public void setSummaryMinMax(double newMin, double newMax) {
		summaryMin = newMin;
		summaryMax = newMax;
		log.finer(() -> getName() + " syncSummaryMin=" + newMin + " syncSummaryMax=" + newMax);
	}

	public void resetSummaryMinMax() {
		summaryMin = summaryMax = null;
	}

	protected double[] defineSummaryExtrema() { // todo consider caching this result
		TrailTypes trailType = getTrailSelector().getTrailType();
		if (trailType.isAlienValue()) {
			Collection<List<ExtendedVault>> vaults = getParent().getHistoVaults().values();

			Stream<Integer> alienPoints = vaults.parallelStream().flatMap(Collection::stream).map(v -> getVaultPoint(v, trailType.ordinal()));
			List<Double> decodedAliens = alienPoints.filter(Objects::nonNull).map(i -> HistoSet.decodeVaultValue(this, i / 1000.)).collect(Collectors.toList());
			if (decodedAliens.isEmpty()) {
				return new double[] { 0, 0 };
			} else {
				UniversalQuantile<Double> tmpQuantile = new UniversalQuantile<>(decodedAliens, true, //
						SUMMARY_OUTLIER_SIGMA, SUMMARY_OUTLIER_RANGE_FACTOR);

				double[] result = new double[] { tmpQuantile.getQuartile0(), tmpQuantile.getQuartile4() };
				log.finer(() -> getName() + " " + Arrays.toString(result) + "  outlier size=" + tmpQuantile.getOutliers().size());
				return result;
			}
		} else {
			Collection<List<ExtendedVault>> vaults = getParent().getHistoVaults().values();

			Stream<Integer> q0Points = vaults.parallelStream().flatMap(Collection::stream).map(v -> getVaultPoint(v, TrailTypes.Q0.ordinal()));
			List<Double> decodedQ0s = q0Points.filter(Objects::nonNull).map(i -> HistoSet.decodeVaultValue(this, i / 1000.)).collect(Collectors.toList());
			Stream<Integer> q4Points = vaults.parallelStream().flatMap(Collection::stream).map(v -> getVaultPoint(v, TrailTypes.Q4.ordinal()));
			List<Double> decodedQ4s = q4Points.filter(Objects::nonNull).map(i -> HistoSet.decodeVaultValue(this, i / 1000.)).collect(Collectors.toList());

			if (decodedQ0s.isEmpty() || decodedQ4s.isEmpty()) {
				return new double[] { 0, 0 };
			} else {
				UniversalQuantile<Double> minQuantile = new UniversalQuantile<>(decodedQ0s, true, //
						SUMMARY_OUTLIER_SIGMA, SUMMARY_OUTLIER_RANGE_FACTOR);
				UniversalQuantile<Double> maxQuantile = new UniversalQuantile<>(decodedQ4s, true, //
						SUMMARY_OUTLIER_SIGMA, SUMMARY_OUTLIER_RANGE_FACTOR);
				double[] result = new double[] { minQuantile.getQuartile0(), maxQuantile.getQuartile4() };
				log.finer(() -> getName() + " " + Arrays.toString(result) + "  max outlier size=" + maxQuantile.getOutliers().size() + "  min outlier size=" + minQuantile.getOutliers().size());
				return result;
			}
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
		quantile = new UniversalQuantile<>(getDecodedNotNullValues(), true, //
				SUMMARY_OUTLIER_SIGMA, SUMMARY_OUTLIER_RANGE_FACTOR);
	}

	public UniversalQuantile<Double> getQuantile() {
		if (quantile == null) defineQuantile();
		return quantile;
	}

	public SummarySpots getSummarySpots() {
		if (summarySpots == null) summarySpots = new SummarySpots(this);
		return summarySpots;
	}

	/**
	 * @return the tukey box plot arrays for the min/max trails or for score groups w/o min/max scores take the first score
	 */
	protected double[][] defineExtremumQuantiles() {
		int[] extremumOrdinals = this.trailSelector.getExtremumOrdinals();
		Collection<List<ExtendedVault>> vaults = this.getParent().getHistoVaults().values();

		Stream<Integer> pointMinimums = vaults.parallelStream().flatMap(List::stream).map(v -> getVaultPoint(v, extremumOrdinals[0]));
		List<Double> decodedMinimums = pointMinimums.filter(Objects::nonNull).map(i -> HistoSet.decodeVaultValue(this, i / 1000.)).collect(Collectors.toList());
		UniversalQuantile<Double> minQuantile = new UniversalQuantile<>(decodedMinimums, true);

		Stream<Integer> pointMaximums = vaults.parallelStream().flatMap(List::stream).map(v -> getVaultPoint(v, extremumOrdinals[1]));
		List<Double> decodedMaximums = pointMaximums.filter(Objects::nonNull).map(i -> HistoSet.decodeVaultValue(this, i / 1000.)).collect(Collectors.toList());
		UniversalQuantile<Double> maxQuantile = new UniversalQuantile<>(decodedMaximums, true);

		return new double[][] { minQuantile.getTukeyBoxPlot(), maxQuantile.getTukeyBoxPlot() };
	}

	/**
	 * @return the points for the q0/q4 trails; for score groups w/o min/max scores take the first score
	 */
	protected Integer[] getExtremumTrailPoints(ExtendedVault vault) {
		int[] extremumOrdinals = this.trailSelector.getExtremumOrdinals();
		return new Integer[] { getVaultPoint(vault, extremumOrdinals[0]), getVaultPoint(vault, extremumOrdinals[1]) };
	}

	/**
	 * @param logLimit is the maximum number of the most recent logs which is checked for warnings
	 * @return the array of warnings which may hold null values
	 */
	public OutlierWarning[] defineMinMaxWarning(int logLimit) {
		OutlierWarning minWarning = null;
		OutlierWarning maxWarning = null;

		double[][] minMaxQuantiles = defineExtremumQuantiles();
		double extremeMinOutlierLimit = minMaxQuantiles[0][QUARTILE1.ordinal()] - 3. * (minMaxQuantiles[0][QUARTILE3.ordinal()] - minMaxQuantiles[0][QUARTILE1.ordinal()]);
		double extremeMaxOutlierLimit = minMaxQuantiles[1][QUARTILE3.ordinal()] + 3. * (minMaxQuantiles[1][QUARTILE3.ordinal()] - minMaxQuantiles[1][QUARTILE1.ordinal()]);

		Collection<List<ExtendedVault>> vaults = this.getParent().getHistoVaults().values();
		Iterator<ExtendedVault> iterator = vaults.stream().flatMap(List::stream).iterator();

		int actualLimit = logLimit > 0 && logLimit < size() ? logLimit : size();
		for (int i = 0; i < actualLimit; i++) {
			ExtendedVault vault = iterator.next();
			Integer[] minMaxTrailPoints = getExtremumTrailPoints(vault);
			if (minMaxTrailPoints[0] == null || minMaxTrailPoints[1] == null) continue;

			double tmpMinValue = HistoSet.decodeVaultValue(this, minMaxTrailPoints[0] / 1000.0);
			if (HistoSet.fuzzyCompare(tmpMinValue, extremeMinOutlierLimit) < 0) {
				minWarning = OutlierWarning.FAR;
			} else if (minWarning == null && HistoSet.fuzzyCompare(tmpMinValue, minMaxQuantiles[0][LOWER_WHISKER.ordinal()]) < 0) {
				minWarning = OutlierWarning.CLOSE;
			}
			double tmpMaxValue = HistoSet.decodeVaultValue(this, minMaxTrailPoints[1] / 1000.);
			if (HistoSet.fuzzyCompare(tmpMaxValue, extremeMaxOutlierLimit) > 0) {
				maxWarning = OutlierWarning.FAR;
			} else if (maxWarning == null && HistoSet.fuzzyCompare(tmpMaxValue, minMaxQuantiles[1][UPPER_WHISKER.ordinal()]) > 0) {
				maxWarning = OutlierWarning.CLOSE;
			}
		}
		return new OutlierWarning[] { minWarning, maxWarning };
	}

}
