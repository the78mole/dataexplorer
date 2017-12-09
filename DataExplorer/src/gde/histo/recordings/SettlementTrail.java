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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gde.device.SettlementType;
import gde.device.TrailTypes;
import gde.histo.cache.ExtendedVault;
import gde.histo.datasources.HistoSet;
import gde.histo.utils.UniversalQuantile;
import gde.log.Logger;

/**
 * Trail records containing settlement values.
 * @author Thomas Eickert (USER)
 */
public final class SettlementTrail extends TrailRecord {
	private final static String		$CLASS_NAME				= SettlementTrail.class.getName();
	private final static long			serialVersionUID	= 110124007964748556L;
	final static Logger		log								= Logger.getLogger($CLASS_NAME);
	/**
	 * @param newOrdinal
	 * @param settlementType
	 * @param parent
	 * @param initialCapacity
	 */
	public SettlementTrail(int newOrdinal, SettlementType settlementType, TrailRecordSet parent, int initialCapacity) {
		super(settlementType, newOrdinal, parent, initialCapacity);
	}

	public SettlementType getSettlement() {
		return (SettlementType) this.channelItem;
	}

	@Override
	protected boolean isAllowedBySetting() {
		return this.settings.isDisplaySettlements();
	}

	@Override
	public boolean isScaleVisible() {
		return this.settings.isDisplaySettlements() && super.isScaleVisible();
	}

	@Override
	public void setApplicableTrailTypes() {
		getTrailSelector().setApplicableTrails4Settlement();
	}

	@Override
	public Integer getVaultPoint(ExtendedVault vault, int trailOrdinal) {
		return vault.getSettlementPoint(this.getOrdinal(), trailOrdinal);
	}

	@Override
	public double[][] determineMinMaxQuantiles() {
		Collection<List<ExtendedVault>> vaults = this.getParentTrail().getHistoVaults().values();

		Stream<Integer> pointMinimums = vaults.parallelStream().flatMap(List::stream).map(v -> getVaultPoint(v, TrailTypes.MIN.ordinal()));
		List<Double> decodedMinimums = pointMinimums.map(i -> HistoSet.decodeVaultValue(this, i / 1000.)).collect(Collectors.toList());
		UniversalQuantile<Double> minQuantile = new UniversalQuantile<>(decodedMinimums, true);

		Stream<Integer> pointMaximums = vaults.parallelStream().flatMap(List::stream).map(v -> getVaultPoint(v, TrailTypes.MAX.ordinal()));
		List<Double> decodedMaximums = pointMaximums.map(i -> HistoSet.decodeVaultValue(this, i / 1000.)).collect(Collectors.toList());
		UniversalQuantile<Double> maxQuantile = new UniversalQuantile<>(decodedMaximums, true);

		return new double[][] { minQuantile.getTukeyBoxPlot(), maxQuantile.getTukeyBoxPlot() };
	}

}
