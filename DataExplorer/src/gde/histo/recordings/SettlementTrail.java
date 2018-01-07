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

import java.util.Arrays;

import gde.data.Record.DataType;
import gde.device.SettlementType;
import gde.histo.cache.DataTypes;
import gde.histo.cache.ExtendedVault;
import gde.histo.datasources.HistoSet;

/**
 * Trail records containing settlement values.
 * @author Thomas Eickert (USER)
 */
public final class SettlementTrail extends TrailRecord {
	private final static long serialVersionUID = 110124007964748556L;

	/**
	 * @param newOrdinal
	 * @param settlementType
	 * @param parent
	 * @param initialCapacity
	 */
	public SettlementTrail(int newOrdinal, SettlementType settlementType, TrailRecordSet parent, int initialCapacity) {
		super(settlementType, newOrdinal, parent, initialCapacity);
		this.trailSelector = new SettlementTrailSelector(this);
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
		getTrailSelector().setApplicableTrails();
	}

	@Override
	public Integer getVaultPoint(ExtendedVault vault, int trailOrdinal) {
		return vault.getSettlementPoint(((SettlementType) this.channelItem).getSettlementId(), trailOrdinal);
	}

	@Override
	public boolean hasVaultOutliers(ExtendedVault vault) {
		return vault.hasSettlementOutliers(((SettlementType) this.channelItem).getSettlementId());
	}

	@Override
	public boolean hasVaultScraps(ExtendedVault vault) {
		return vault.hasSettlementScraps(((SettlementType) this.channelItem).getSettlementId());
	}

	@Override
	public double[] getVaultOutliers(ExtendedVault vault) {
		int[] points = vault.getSettlementOutlierPoints(((SettlementType) this.channelItem).getSettlementId());
		return Arrays.stream(points).mapToDouble(p -> HistoSet.decodeVaultValue(this, p / 1000.)).toArray();
	}

	@Override
	public double[] getVaultScraps(ExtendedVault vault) {
		int[] points = vault.getSettlementScrappedPoints(((SettlementType) this.channelItem).getSettlementId());
		return Arrays.stream(points).mapToDouble(p -> HistoSet.decodeVaultValue(this, p / 1000.)).toArray();
	}

	@Override
	public DataType getVaultDataType(ExtendedVault vault) {
		return DataTypes.toDataType(vault.getSettlementDataType(((SettlementType) this.channelItem).getSettlementId()));
	}


}
