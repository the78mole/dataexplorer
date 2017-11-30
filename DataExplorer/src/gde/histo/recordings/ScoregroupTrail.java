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

import gde.device.ScoreGroupType;
import gde.device.TrailTypes;
import gde.histo.cache.ExtendedVault;
import gde.histo.datasources.HistoSet;
import gde.histo.utils.UniversalQuantile;
import gde.log.Logger;

/**
 * Trail records containing score values.
 * @author Thomas Eickert (USER)
 */
public final class ScoregroupTrail extends TrailRecord {
	private final static String		$CLASS_NAME				= ScoregroupTrail.class.getName();
	private final static long			serialVersionUID	= 110124007964748556L;
	private final static Logger		log								= Logger.getLogger($CLASS_NAME);

	/**
	 * The score values of all vaults added to this record.
	 */
	protected final List<Integer>	vaultValues				= new ArrayList<>();

	/**
	 * @param newOrdinal
	 * @param scoreGroupType
	 * @param parent
	 * @param initialCapacity
	 */
	public ScoregroupTrail(int newOrdinal, ScoreGroupType scoreGroupType, TrailRecordSet parent, int initialCapacity) {
		super(scoreGroupType, newOrdinal, parent, initialCapacity);
	}

	public ScoreGroupType getScoregroup() {
		return (ScoreGroupType) this.channelItem;
	}

	@Override
	protected boolean isAllowedBySetting() {
		return this.settings.isDisplayScores();
	}

	@Override
	public boolean isScaleVisible() {
		return this.settings.isDisplayScores() && super.isScaleVisible();
	}

	@Override
	public void setApplicableTrailTypes() {
		getTrailSelector().setApplicableTrails4Scoregroup();
	}

	@Override
	public Integer getVaultPoint(ExtendedVault vault, TrailTypes trailType) {
		return vault.getScorePoint(trailType.ordinal());
	}

	@Override
	protected double[] determineSummaryMinMax() {
		if (vaultValues.isEmpty()) return new double[0];

		// both lists hold identical values because there are no max/min values but only one value per vault
		List<Double> decodedValues = vaultValues.stream().map(i -> RecordingsCollector.decodeVaultValue(this, i / 1000.)).collect(Collectors.toList());

		UniversalQuantile<Double> quantile = new UniversalQuantile<>(decodedValues, true, //
				HistoSet.SUMMARY_OUTLIER_SIGMA_DEFAULT, HistoSet.SUMMARY_OUTLIER_RANGE_FACTOR_DEFAULT);

		double[] result = new double[] { quantile.getQuartile0(), quantile.getQuartile4() };
		log.finest(() -> getName() + " " + Arrays.toString(result));
		return result;
	}

	@Override
	public void addSummaryPoints(ExtendedVault histoVault) {
		Integer point = getVaultPoint(histoVault, this.getTrailSelector().getTrailType());
		if (point != null) this.vaultValues.add(point);
	}

	@Override
	public void clear() {
		this.vaultValues.clear();
		super.clear();
	}

}
