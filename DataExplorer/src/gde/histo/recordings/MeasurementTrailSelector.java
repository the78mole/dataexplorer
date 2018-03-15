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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import gde.device.MeasurementType;
import gde.device.StatisticsType;
import gde.device.TrailDisplayType;
import gde.device.TrailTypes;
import gde.device.TrailVisibilityType;
import gde.ui.DataExplorer;

/**
 * Handle the trail type assignment to a trailRecord.
 * @author Thomas Eickert
 */
public final class MeasurementTrailSelector extends TrailSelector {

	public MeasurementTrailSelector(TrailRecord trailRecord) {
		super(trailRecord);
	}

	@Override
	public void setApplicableTrails() {

		final boolean[] applicablePrimitiveTrails = getApplicablePrimitiveTrails();

		// build applicable trail type lists for display purposes
		this.applicableTrailsOrdinals = new ArrayList<Integer>();
		this.applicableTrailsTexts = new ArrayList<String>();
		for (int i = 0; i < applicablePrimitiveTrails.length; i++) {
			if (applicablePrimitiveTrails[i]) {
				this.applicableTrailsOrdinals.add(i);
				this.applicableTrailsTexts.add(TrailTypes.VALUES[i].getDisplayNameWithTriggerText(trailRecord.channelItem).intern());
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
	private boolean[] getApplicablePrimitiveTrails() {
		final boolean[] applicablePrimitiveTrails;
		Optional<TrailDisplayType> trailDisplay = trailRecord.channelItem.getTrailDisplay();
		if (trailDisplay.map(TrailDisplayType::getDefaultTrail).map(TrailTypes::isSuite).orElse(false)) throw new UnsupportedOperationException(
				"suite trail must not be a device measurement default");

		boolean hideAllTrails = trailDisplay.map(TrailDisplayType::isDiscloseAll).orElse(false);
		if (hideAllTrails) {
			applicablePrimitiveTrails = new boolean[TrailTypes.getPrimitives().size()];
			trailDisplay.ifPresent(x -> x.getExposed().stream().map(TrailVisibilityType::getTrail) //
					.filter(t -> !t.isSuite()).filter(t -> t.isSmartStatistics() == trailRecord.getParent().isSmartStatistics()) //
					.forEach(t -> applicablePrimitiveTrails[t.ordinal()] = true));
		} else {
			applicablePrimitiveTrails = getApplicableLegacyTrails();
			// set non-suite trail types : triggered values like count/sum are not supported
			TrailTypes.getPrimitives().stream() //
					.filter(t -> !t.isTriggered()).filter(t -> t.isSmartStatistics() == trailRecord.getParent().isSmartStatistics()) //
					.forEach(t -> applicablePrimitiveTrails[t.ordinal()] = true);
			// set visible and reset hidden trails based on device settlement settings
			trailDisplay.ifPresent(x -> x.getDisclosed().stream().map(TrailVisibilityType::getTrail) //
					.filter(t -> !t.isSuite()).filter(t -> t.isSmartStatistics() == trailRecord.getParent().isSmartStatistics()) //
					.forEach(t -> applicablePrimitiveTrails[t.ordinal()] = false));
		}

		// set at least one trail if no trail is applicable
		if (!hasTrueValues(applicablePrimitiveTrails))
			applicablePrimitiveTrails[trailDisplay.map(TrailDisplayType::getDefaultTrail).orElse(TrailTypes.getSubstitute()).ordinal()] = true;
		log.finer(() -> this.trailRecord.getName() + " data " + Arrays.toString(applicablePrimitiveTrails)); //$NON-NLS-1$
		return applicablePrimitiveTrails;
	}

	private boolean hasTrueValues(boolean[] values) {
		for (boolean value : values) {
			if (value) return true;
		}
		return false;
	}

	/**
	 * @return an array corresponding to the primitive trail types
	 *         (true if the entry is a legacy compound trail and appears in the device channel item)
	 */
	private boolean[] getApplicableLegacyTrails() {
		final boolean[] applicablePrimitiveTrails;
		applicablePrimitiveTrails = new boolean[TrailTypes.getPrimitives().size()];
		StatisticsType measurementStatistics = ((MeasurementType) trailRecord.channelItem).getStatistics();
		if (trailRecord.getParent().isSmartStatistics()) {
			if (measurementStatistics != null) {
				if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
					applicablePrimitiveTrails[TrailTypes.REAL_SUM_TRIGGERED.ordinal()] = (measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1);
					if (measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1 && measurementStatistics.getRatioRefOrdinal() != null) {
						StatisticsType referencedStatistics = DataExplorer.application.getActiveDevice().getMeasurementStatistic(this.trailRecord.getParent().getChannelConfigNumber(), measurementStatistics.getRatioRefOrdinal());
						applicablePrimitiveTrails[TrailTypes.REAL_AVG_RATIO_TRIGGERED.ordinal()] = referencedStatistics.isAvg();
						applicablePrimitiveTrails[TrailTypes.REAL_MAX_RATIO_TRIGGERED.ordinal()] = referencedStatistics.isMax();
					}
				}
				applicablePrimitiveTrails[TrailTypes.REAL_TIME_SUM_TRIGGERED.ordinal()] = (measurementStatistics.getTrigger() != null && measurementStatistics.getSumTriggerTimeText() != null && measurementStatistics.getSumTriggerTimeText().length() > 1);
				applicablePrimitiveTrails[TrailTypes.REAL_COUNT_TRIGGERED.ordinal()] = (measurementStatistics.isCountByTrigger() != null);
			}
			// applicablePrimitiveTrails[TrailTypes.REAL_SUM.ordinal()] = false; // in settlements only
		}
		return applicablePrimitiveTrails;
	}

}