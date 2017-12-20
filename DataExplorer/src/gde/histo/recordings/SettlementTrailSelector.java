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
import java.util.Optional;

import gde.device.SettlementType;
import gde.device.TrailDisplayType;
import gde.device.TrailTypes;

/**
 * Handle the trail type assignment to a trailRecord.
 * @author Thomas Eickert
 */
public final class SettlementTrailSelector extends TrailSelector {
	public SettlementTrailSelector(TrailRecord trailRecord) {
		super(trailRecord);
	}

	@Override
	public void setApplicableTrails() {
		if (trailRecord.channelItem.getTrailDisplay().map(x -> x.getDefaultTrail()).map(x -> x.isSuite()).orElse(false))
			throw new UnsupportedOperationException("suite trail must not be a device settlement default");
		final boolean[] applicablePrimitiveTrails = getApplicablePrimitiveTrails();

		// build applicable trail type lists for display purposes
		this.applicableTrailsOrdinals = new ArrayList<Integer>();
		this.applicableTrailsTexts = new ArrayList<String>();
		for (int i = 0; i < applicablePrimitiveTrails.length; i++) {
			if (applicablePrimitiveTrails[i]) {
				this.applicableTrailsOrdinals.add(i);
				this.applicableTrailsTexts.add(TrailTypes.VALUES[i].getDisplayName().intern());
			}
		}

		setApplicableSuiteTrails();
		log.finer(() -> this.trailRecord.getName() + " texts " + this.applicableTrailsTexts); //$NON-NLS-1$
		log.finer(() -> this.trailRecord.getName() + " ordinals " + this.applicableTrailsOrdinals); //$NON-NLS-1$
	}

	/**
	 * Determine an array with index based on the trail primitives ordinal number.
	 * @return the array giving the information which trails are visible for the user
	 */
	public boolean[] getApplicablePrimitiveTrails() {
		boolean[] applicablePrimitiveTrails = new boolean[TrailTypes.getPrimitives().size()];
		Optional<TrailDisplayType> trailDisplay = trailRecord.channelItem.getTrailDisplay();

		// set quantile-based non-suite trail types : triggered value sum are CURRENTLY not supported
		if (!trailDisplay.map(x -> x.isDiscloseAll()).orElse(false)) {
			if (((SettlementType) trailRecord.channelItem).getEvaluation().getTransitionAmount() == null)
				TrailTypes.getPrimitives().stream().filter(x -> !x.isTriggered() && x.isSmartStatistics() == this.settings.isSmartStatistics()).forEach(x -> applicablePrimitiveTrails[x.ordinal()] = true);
			else
				throw new UnsupportedOperationException("TransitionAmount not implemented"); //$NON-NLS-1$
		}

		// set visible and reset hidden trails based on device settlement settings
		trailDisplay.ifPresent(x -> x.getExposed().stream().map(z -> z.getTrail()).filter(y -> !y.isSuite()).forEach(y -> applicablePrimitiveTrails[y.ordinal()] = true));
		trailDisplay.ifPresent(x -> x.getDisclosed().stream().map(z -> z.getTrail()).filter(y -> !y.isSuite()).forEach(y -> applicablePrimitiveTrails[y.ordinal()] = false));

		// set at least one trail if no trail is applicable
		boolean hasApplicablePrimitiveTrails = false;
		for (boolean value : applicablePrimitiveTrails) {
			hasApplicablePrimitiveTrails = value;
			if (hasApplicablePrimitiveTrails) break;
		}
		if (!hasApplicablePrimitiveTrails)
			applicablePrimitiveTrails[trailDisplay.map(x -> x.getDefaultTrail()).orElse(TrailTypes.getSubstitute()).ordinal()] = true;
		log.finer(() -> this.trailRecord.getName() + " data " + Arrays.toString(applicablePrimitiveTrails)); //$NON-NLS-1$
		return applicablePrimitiveTrails;
	}

}