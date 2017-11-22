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
import gde.device.TrailTypes;
import gde.histo.cache.ExtendedVault;
import gde.log.Logger;

/**
 * Trail records containing measurement values.
 * @author Thomas Eickert (USER)
 */
public final class MeasurementTrail extends TrailRecord {
	private final static String		$CLASS_NAME				= MeasurementTrail.class.getName();
	private final static long			serialVersionUID	= 110124007964748556L;
	private final static Logger	log								= Logger.getLogger($CLASS_NAME);

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
	public boolean isAllowedBySetting() {
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

}
