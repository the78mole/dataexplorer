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

    Copyright (c) 2017,2018 Thomas Eickert
****************************************************************************************/

package gde.histo.recordings;

import gde.data.Record.DataType;
import gde.device.ScoreGroupType;
import gde.histo.cache.ExtendedVault;
import gde.log.Logger;

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

	@Override
	public boolean hasReasonableData() {
		return true;
	}

	@Override
	public double[] defineRecentMinMax(int limit ) {
		return getParent().getPickedVaults().defineRecentScoreMinMax(getName(), getScoregroup(), limit);
	}

	@Override
	public double[] defineExtrema() {
		return getParent().getPickedVaults().defineScoreExtrema(getName(), getScoregroup());
	}

	@Override
	protected Integer[] getExtremumTrailPoints(ExtendedVault vault) {
		int[] extremumScoreOrdinals = this.trailSelector.getExtremumTrailsOrdinals();
		if (extremumScoreOrdinals[0] == extremumScoreOrdinals[1]) { // let's spare some processing time
			Integer vaultPoint = getVaultPoint(vault, extremumScoreOrdinals[0]);
			return new Integer[] { vaultPoint, vaultPoint };
		} else {
			return new Integer[] { getVaultPoint(vault, extremumScoreOrdinals[0]), getVaultPoint(vault, extremumScoreOrdinals[1]) };
		}
	}

	@Override
	public boolean hasVaultOutliers(ExtendedVault vault) {
		return false;
	}

	@Override
	public boolean hasVaultScraps(ExtendedVault vault) {
		return false;
	}

	@Override
	public double[] getVaultScraps(ExtendedVault vault) {
		return new double[0];
	}

	@Override
	public double[] getVaultOutliers(ExtendedVault vault) {
		return new double[0];
	}

	@Override
	public DataType getVaultDataType(ExtendedVault vault) {
		return DataType.DEFAULT;
	}

}
