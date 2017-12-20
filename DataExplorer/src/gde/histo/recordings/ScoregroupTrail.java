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

import gde.device.ScoreGroupType;
import gde.histo.cache.ExtendedVault;
import gde.histo.datasources.HistoSet;
import gde.histo.utils.UniversalQuantile;
import gde.log.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Trail records containing score values.
 * @author Thomas Eickert (USER)
 */
public final class ScoregroupTrail extends TrailRecord {
	private final static String	$CLASS_NAME				= ScoregroupTrail.class.getName();
	private final static long		serialVersionUID	= 110124007964748556L;
	private final static Logger	log								= Logger.getLogger($CLASS_NAME);

	/**
	 * @param newOrdinal
	 * @param scoreGroupType
	 * @param parent
	 * @param initialCapacity
	 */
	public ScoregroupTrail(int newOrdinal, ScoreGroupType scoreGroupType, TrailRecordSet parent, int initialCapacity) {
		super(scoreGroupType, newOrdinal, parent, initialCapacity);
		this.trailSelector = new ScoregroupTrailSelector(this);
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
		getTrailSelector().setApplicableTrails();
	}

	@Override
	public Integer getVaultPoint(ExtendedVault vault, int trailOrdinal) {
		return vault.getScorePoint(trailOrdinal);
	}

	/**
	 * @return true if the record or the suite contains reasonable data which can be displayed
	 */
	@Override // reason is trail record suites with a master record without point values and minValue/maxValue != 0 in case of empty records
	public boolean hasReasonableData() {
		return this.size() > 0 && this.minValue != Integer.MAX_VALUE && this.maxValue != Integer.MIN_VALUE //
				&& (summaryMin == null && summaryMax == null || !HistoSet.fuzzyEquals(summaryMin, summaryMax));
	}

	@Override
	protected double[] defineSummaryExtrema() {
		List<Double> decodedMinimums = new ArrayList<>();
		List<Double> decodedMaximums = new ArrayList<>();

		List<Integer> scoreOrdinals = getScoregroup().getScore().stream().map(s -> s.getTrailOrdinal()) //
				.collect(Collectors.toList());
		// todo check why parallelStream() in the next statement results in sporadic unmatched length of decodedMax/Min
		getParent().getHistoVaults().values().stream().flatMap(Collection::stream).forEach(v -> {
			// determine the min and max of all score entries in the score group of this vault
			Stream<Integer> scoregroupPoints = scoreOrdinals.stream().map(t -> getVaultPoint(v, t));
			DoubleSummaryStatistics stats = scoregroupPoints.map(i -> HistoSet.decodeVaultValue(this, i / 1000.)) //
					.collect(Collectors.summarizingDouble(Double::doubleValue));
			decodedMinimums.add(stats.getMin());
			decodedMaximums.add(stats.getMax());
		});
		log.finer(() -> getName() + "  decodedMinimums=" + Arrays.toString(decodedMinimums.toArray()) + "  decodedMaximums=" + Arrays.toString(decodedMaximums.toArray()));
		UniversalQuantile<Double> minQuantile = new UniversalQuantile<>(decodedMinimums, true, //
				SUMMARY_OUTLIER_SIGMA, SUMMARY_OUTLIER_RANGE_FACTOR);
		UniversalQuantile<Double> maxQuantile = new UniversalQuantile<>(decodedMaximums, true, //
				SUMMARY_OUTLIER_SIGMA, SUMMARY_OUTLIER_RANGE_FACTOR);

		double[] result = new double[] { minQuantile.getQuartile0(), maxQuantile.getQuartile4() };
		log.finer(() -> getName() + " " + Arrays.toString(result));
		return result;
	}

	@Override
	protected Integer[] getExtremumTrailPoints(ExtendedVault vault) {
		int[] extremumScoreOrdinals = this.trailSelector.getExtremumOrdinals();
		if (extremumScoreOrdinals[0] == extremumScoreOrdinals[1]) { // let's spare some processing time
			Integer vaultPoint = getVaultPoint(vault, extremumScoreOrdinals[0]);
			return new Integer[] { vaultPoint, vaultPoint };
		} else {
			return new Integer[] { getVaultPoint(vault, extremumScoreOrdinals[0]), getVaultPoint(vault, extremumScoreOrdinals[1]) };
		}
	}

}
