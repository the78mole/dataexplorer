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
import java.util.stream.Collectors;

import gde.device.MeasurementType;
import gde.device.TrailTypes;
import gde.histo.cache.ExtendedVault;
import gde.histo.datasources.HistoSet;
import gde.histo.utils.UniversalQuantile;
import gde.log.Logger;

/**
 * Trail records containing measurement values.
 * @author Thomas Eickert (USER)
 */
public final class MeasurementTrail extends TrailRecord {
	private final static String		$CLASS_NAME				= MeasurementTrail.class.getName();
	private final static long			serialVersionUID	= 110124007964748556L;
	private final static Logger		log								= Logger.getLogger($CLASS_NAME);

	/**
	 * The real maximum values of all vaults added to this record.
	 */
	protected final List<Integer>	vaultMaximums			= new ArrayList<>();
	/**
	 * The real minimum values of all vaults added to this record.
	 */
	protected final List<Integer>	vaultMinimums			= new ArrayList<>();

	/**
	 * @param newOrdinal
	 * @param measurementType
	 * @param parent
	 * @param initialCapacity
	 */
	public MeasurementTrail(int newOrdinal, MeasurementType measurementType, TrailRecordSet parent, int initialCapacity) {
		super(measurementType, newOrdinal, parent, initialCapacity);
	}

	public MeasurementType getMeasurement() {
		return (MeasurementType) this.channelItem;
	}

	@Override
	protected boolean isAllowedBySetting() {
		return true;
	}

	@Override // for clarity only
	public boolean isScaleVisible() {
		return super.isScaleVisible();
	}

	@Override
	public void setApplicableTrailTypes() {
		getTrailSelector().setApplicableTrails4Measurement();
	}

	@Override
	public Integer getVaultPoint(ExtendedVault vault, TrailTypes trailType) {
		return vault.getMeasurementPoint(this.getOrdinal(), trailType.ordinal());
	}

	@Override
	public void addSummaryPoints(ExtendedVault histoVault) {
		Integer point = getVaultPoint(histoVault, TrailTypes.MAX);
		if (point != null) {
			this.vaultMaximums.add(point);
			this.vaultMinimums.add(getVaultPoint(histoVault, TrailTypes.MIN));
		}
	}

	@Override
	public void clear() {
		this.vaultMaximums.clear();
		this.vaultMinimums.clear();
		super.clear();
	}

	@Override
	protected double[] determineSummaryMinMax() {
		if (vaultMaximums.isEmpty() || vaultMinimums.isEmpty()) return new double[0];

		List<Double> decodedMaximums = vaultMaximums.stream().map(i -> RecordingsCollector.decodeVaultValue(this, i / 1000.)).collect(Collectors.toList());
		List<Double> decodedMinimums = vaultMinimums.stream().map(i -> RecordingsCollector.decodeVaultValue(this, i / 1000.)).collect(Collectors.toList());

		UniversalQuantile<Double> maxQuantile = new UniversalQuantile<>(decodedMaximums, true, //
				HistoSet.SUMMARY_OUTLIER_SIGMA_DEFAULT, HistoSet.SUMMARY_OUTLIER_RANGE_FACTOR_DEFAULT);
		UniversalQuantile<Double> minQuantile = new UniversalQuantile<>(decodedMinimums, true, //
				HistoSet.SUMMARY_OUTLIER_SIGMA_DEFAULT, HistoSet.SUMMARY_OUTLIER_RANGE_FACTOR_DEFAULT);

		double[] result = new double[] { minQuantile.getQuartile0(), maxQuantile.getQuartile4() };
		log.finest(() -> getName() + " " + Arrays.toString(result) + "  max outlier size=" + maxQuantile.getOutliers().size() + "  min outlier size=" + minQuantile.getOutliers().size());
		return result;
	}

}
