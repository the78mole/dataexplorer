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

import gde.device.MeasurementType;
import gde.histo.cache.ExtendedVault;

/**
 * Trail records containing measurement values.
 * @author Thomas Eickert (USER)
 */
public final class MeasurementTrail extends TrailRecord {
	private final static long		serialVersionUID	= 110124007964748556L;

	/**
	 * @param newOrdinal
	 * @param measurementType
	 * @param parent
	 * @param initialCapacity
	 */
	public MeasurementTrail(int newOrdinal, MeasurementType measurementType, TrailRecordSet parent, int initialCapacity) {
		super(measurementType, newOrdinal, parent, initialCapacity);
		this.trailSelector = new MeasurementTrailSelector(this);
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
		getTrailSelector().setApplicableTrails();
	}

	@Override
	public Integer getVaultPoint(ExtendedVault vault, int trailOrdinal) {
		return vault.getMeasurementPoint(this.getOrdinal(), trailOrdinal);
	}

}