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
import java.util.List;

import gde.device.ScoreGroupType;
import gde.device.ScoreType;

/**
 * Handle the trail type assignment to a trailRecord.
 * @author Thomas Eickert
 */
public final class ScoregroupTrailSelector extends TrailSelector {
	public ScoregroupTrailSelector(TrailRecord trailRecord) {
		super(trailRecord);
	}

	@Override
	public void setMostApplicableTrailTextOrdinal() {
		setTrailTextSelectedIndex(0);
	}

	@Override
	public void setApplicableTrails() {
		// build applicable trail type lists for display purposes
		this.applicableTrailsOrdinals = new ArrayList<Integer>();
		this.applicableTrailsTexts = new ArrayList<String>();
		// if (this.trailRecord.channelItem != null) {
		List<ScoreType> scoreTypes = ((ScoreGroupType) trailRecord.channelItem).getScore();
		for (int i = 0; i < scoreTypes.size(); i++) {
			this.applicableTrailsOrdinals.add(scoreTypes.get(i).getTrailOrdinal());
			this.applicableTrailsTexts.add(getDeviceXmlReplacement(scoreTypes.get(i).getValue()));
		}
		log.finer(() -> this.trailRecord.getName() + " score "); //$NON-NLS-1$
		// }
	}

	@Override
	protected void setExtremumOrdinals() {
		List<ScoreType> scoreTypes = ((ScoreGroupType) trailRecord.channelItem).getScore();
		// find the score labels with a name containing min/max
		int index4Min = -1, index4Max = -1;
		for (int i = 0; i < this.applicableTrailsOrdinals.size(); i++) {
			if (scoreTypes.get(i).getLabel().name().contains("MIN")) index4Min = i;
			if (scoreTypes.get(i).getLabel().name().contains("MAX")) index4Max = i;
		}
		if (index4Min != -1 && index4Max != -1) {
			extremumOrdinals = new int[] { index4Min, index4Max };
		} else {
			extremumOrdinals = new int[] { 0, 0 };
		}
	}

	@Override
	public boolean isTrailSuite() {
		return false;
	}

	@Override
	public boolean isRangePlotSuite() {
		return false;
	}

	@Override
	public boolean isBoxPlotSuite() {
		return false;
	}

}