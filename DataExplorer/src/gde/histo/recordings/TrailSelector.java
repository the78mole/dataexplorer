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
import java.util.Optional;
import java.util.stream.Collectors;

import gde.GDE;
import gde.config.Settings;
import gde.device.MeasurementType;
import gde.device.ScoreGroupType;
import gde.device.ScoreType;
import gde.device.SettlementType;
import gde.device.StatisticsType;
import gde.device.TrailDisplayType;
import gde.device.TrailTypes;
import gde.device.resource.DeviceXmlResource;
import gde.log.Logger;
import gde.ui.DataExplorer;

/**
 * Handle the trail type assignment to a trailRecord.
 * @author Thomas Eickert
 */
public final class TrailSelector {
	private final static String			$CLASS_NAME							= TrailSelector.class.getName();
	private final static Logger			log											= Logger.getLogger($CLASS_NAME);

	private final Settings					settings								= Settings.getInstance();
	private final DeviceXmlResource	xmlResource							= DeviceXmlResource.getInstance();

	private final TrailRecord				trailRecord;

	/**
	 * user selection from applicable trails, is saved in the graphics template
	 */
	private int											trailTextSelectedIndex	= -1;
	/**
	 * the user may select one of these entries
	 */
	private List<String>						applicableTrailsTexts;																		//
	/**
	 * maps all applicable trails in order to convert the user selection into a valid trail
	 */
	private List<Integer>						applicableTrailsOrdinals;

	public TrailSelector(TrailRecord trailRecord) {
		this.trailRecord = trailRecord;
	}

	@Override
	public String toString() {
		return String.format("selected trail for %22s type='%-11s' ordinal=%d", this.trailRecord.getName(), this.getTrailText(), this.getTrailTextSelectedIndex()); //$NON-NLS-1$
	}

	/**
	 * Select the most prioritized trail from the applicable trails.
	 */
	public void setMostApplicableTrailTextOrdinal() {
		if (this.trailRecord instanceof ScoregroupTrail) {
			setTrailTextSelectedIndex(0);
		} else {
			int displaySequence = Integer.MAX_VALUE;
			for (int i = 0; i < this.applicableTrailsOrdinals.size(); i++) {
				int tmpDisplaySequence = (TrailTypes.fromOrdinal(this.applicableTrailsOrdinals.get(i))).getDisplaySequence();
				if (tmpDisplaySequence < displaySequence) {
					displaySequence = tmpDisplaySequence;
					setTrailTextSelectedIndex(i);
				}
			}
		}
	}

	/**
	 *
	 */
	public void setApplicableTrails4Scoregroup() {
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

	/**
	 *
	 */
	public void setApplicableTrails4Settlement() {
		Optional<TrailDisplayType> trailDisplay = trailRecord.channelItem.getTrailDisplay();
		if (trailDisplay.map(x -> x.getDefaultTrail()).map(x -> x.isSuite()).orElse(false)) throw new UnsupportedOperationException(
				"suite trail as a device settlement default");
		final boolean[] applicablePrimitiveTrails = getApplicablePrimitiveTrails4Settlement();

		// build applicable trail type lists for display purposes
		this.applicableTrailsOrdinals = new ArrayList<Integer>();
		this.applicableTrailsTexts = new ArrayList<String>();
		for (int i = 0; i < applicablePrimitiveTrails.length; i++) {
			if (applicablePrimitiveTrails[i]) {
				this.applicableTrailsOrdinals.add(i);
				this.applicableTrailsTexts.add(TrailTypes.VALUES[i].getDisplayName().intern());
			}
		}

		if (!trailDisplay.map(x -> x.isDiscloseAll()).orElse(false)) {
			// define trail suites which are applicable for display
			List<TrailTypes> exposed = trailDisplay.map(x -> x.getExposed().stream().map(y -> y.getTrail()).collect(Collectors.toList())).orElse(new ArrayList<TrailTypes>());
			// get an empty list if there is no trailDisplay tag in the device.xml for this settlement
			List<TrailTypes> disclosed = trailDisplay.map(x -> x.getDisclosed().stream().map(y -> y.getTrail()).collect(Collectors.toList())).orElse(new ArrayList<TrailTypes>());
			for (TrailTypes suiteTrailType : TrailTypes.getSuites()) {
				if ((suiteTrailType.isSmartStatistics() == this.settings.isSmartStatistics() || exposed.contains(suiteTrailType)) && !disclosed.contains(suiteTrailType)) {
					this.applicableTrailsOrdinals.add(suiteTrailType.ordinal());
					this.applicableTrailsTexts.add(suiteTrailType.getDisplayName().intern());
				}
			}
		}
		log.finer(() -> this.trailRecord.getName() + " texts " + this.applicableTrailsTexts); //$NON-NLS-1$
		log.finer(() -> this.trailRecord.getName() + " ordinals " + this.applicableTrailsOrdinals); //$NON-NLS-1$
	}

	/**
	 *
	 */
	public void setApplicableTrails4Measurement() {
		Optional<TrailDisplayType> trailDisplay = trailRecord.channelItem.getTrailDisplay();
		final boolean[] applicablePrimitiveTrails = getApplicablePrimitiveTrails4Measurement();

		// build applicable trail type lists for display purposes
		this.applicableTrailsOrdinals = new ArrayList<Integer>();
		this.applicableTrailsTexts = new ArrayList<String>();
		for (int i = 0; i < applicablePrimitiveTrails.length; i++) {
			if (applicablePrimitiveTrails[i]) {
				this.applicableTrailsOrdinals.add(i);
				this.applicableTrailsTexts.add(TrailTypes.VALUES[i].getDisplayNameWithTriggerText(trailRecord.channelItem).intern());
			}
		}

		if (!trailDisplay.map(x -> x.isDiscloseAll()).orElse(false)) {
			// define trail suites which are applicable for display
			List<TrailTypes> exposed = trailDisplay.map(x -> x.getExposed().stream().map(y -> y.getTrail()).collect(Collectors.toList())).orElse(new ArrayList<TrailTypes>());
			// get an empty list if there is no trailDisplay tag in the device.xml for this settlement
			List<TrailTypes> disclosed = trailDisplay.map(x -> x.getDisclosed().stream().map(y -> y.getTrail()).collect(Collectors.toList())).orElse(new ArrayList<TrailTypes>());
			for (TrailTypes suiteTrailType : TrailTypes.getSuites()) {
				if ((suiteTrailType.isSmartStatistics() == this.settings.isSmartStatistics() || exposed.contains(suiteTrailType)) && !disclosed.contains(suiteTrailType)) {
					this.applicableTrailsOrdinals.add(suiteTrailType.ordinal());
					this.applicableTrailsTexts.add(suiteTrailType.getDisplayName().intern());
				}
			}
		}
		log.finer(() -> this.trailRecord.getName() + " texts " + this.applicableTrailsTexts); //$NON-NLS-1$
		log.finer(() -> this.trailRecord.getName() + " ordinals " + this.applicableTrailsOrdinals); //$NON-NLS-1$
	}

	/**
	 * Determine an array with index based on the trail primitives ordinal number.
	 * @return the array giving the information which trails are visible for the user
	 */
	public boolean[] getApplicablePrimitiveTrails4Measurement() {
		final boolean[] applicablePrimitiveTrails;
		Optional<TrailDisplayType> trailDisplay = trailRecord.channelItem.getTrailDisplay();
		final boolean hideAllTrails = trailDisplay.map(x -> x.isDiscloseAll()).orElse(false);
		if (trailDisplay.map(x -> x.getDefaultTrail()).map(x -> x.isSuite()).orElse(false)) throw new UnsupportedOperationException(
				"suite trail as a device measurement default"); //$NON-NLS-1$

		if (!hideAllTrails) {
			applicablePrimitiveTrails = getApplicableLegacyTrails();
			// set non-suite trail types : triggered values like count/sum are not supported
			TrailTypes.getPrimitives().stream().filter(x -> !x.isTriggered() && x.isSmartStatistics() == this.settings.isSmartStatistics()).forEach(x -> applicablePrimitiveTrails[x.ordinal()] = true);
		} else
			applicablePrimitiveTrails = new boolean[TrailTypes.getPrimitives().size()];

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

	/**
	 * @return
	 */
	private boolean[] getApplicableLegacyTrails() {
		final boolean[] applicablePrimitiveTrails;
		applicablePrimitiveTrails = new boolean[TrailTypes.getPrimitives().size()];
		StatisticsType measurementStatistics = ((MeasurementType) trailRecord.channelItem).getStatistics();
		if (!this.settings.isSmartStatistics()) {
			if (measurementStatistics != null) {
				if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
					applicablePrimitiveTrails[TrailTypes.REAL_SUM_TRIGGERED.ordinal()] = (measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1);
					if (measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1 && measurementStatistics.getRatioRefOrdinal() != null) {
						StatisticsType referencedStatistics = DataExplorer.application.getActiveDevice().getMeasurementStatistic(this.trailRecord.getParentTrail().getChannelConfigNumber(), measurementStatistics.getRatioRefOrdinal());
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

	/**
	 * Determine an array with index based on the trail primitives ordinal number.
	 * @return the array giving the information which trails are visible for the user
	 */
	public boolean[] getApplicablePrimitiveTrails4Settlement() {
		boolean[] applicablePrimitiveTrails = new boolean[TrailTypes.getPrimitives().size()];
		Optional<TrailDisplayType> trailDisplay = trailRecord.channelItem.getTrailDisplay();

		// set quantile-based non-suite trail types : triggered value sum are CURRENTLY not supported
		final Boolean hideAllTrails = trailDisplay.map(x -> x.isDiscloseAll()).orElse(false);
		if (!hideAllTrails) {
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

	public Integer getTrailTextSelectedIndex() {
		return this.trailTextSelectedIndex;
	}

	/**
	 * Build the suite of trail records if the selection has changed.
	 * @param value position / index of the trail type in the current list of applicable trails
	 */
	public void setTrailTextSelectedIndex(int value) {
		this.trailTextSelectedIndex = value;
	}

	/**
	 * @return display text for the trail (may have been modified due to special texts for triggers)
	 */
	public String getTrailText() {
		return this.applicableTrailsTexts.size() == 0 ? GDE.STRING_EMPTY : this.applicableTrailsTexts.get(this.trailTextSelectedIndex);
	}

	public List<String> getApplicableTrailsTexts() {
		return this.applicableTrailsTexts;
	}

	public int getTrailOrdinal() {
		return this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex);
	}

	public TrailTypes getTrailType() {
		return TrailTypes.fromOrdinal(this.applicableTrailsOrdinals.get(this.trailTextSelectedIndex));
	}

	public boolean isTrailSuite() {
		return !(this.trailRecord instanceof ScoregroupTrail) ? getTrailType().isSuite() : false;
	}

	public boolean isRangePlotSuite() {
		return !(this.trailRecord instanceof ScoregroupTrail) ? getTrailType().isRangePlot() : false;
	}

	public boolean isBoxPlotSuite() {
		return !(this.trailRecord instanceof ScoregroupTrail) ? getTrailType().isBoxPlot() : false;
	}

	/**
	 * @param replacementKey
	 * @return the replacement name of the specified key or an empty string if there is no key entry
	 */
	private String getDeviceXmlReplacement(String replacementKey) {
		return replacementKey != null ? this.xmlResource.getReplacement(replacementKey) : GDE.STRING_EMPTY;
	}

	/**
	 * @return the ordinal numbers which hold the min / max values or return an empty array
	 */
	public int[] determineMinMaxScoreOrdinals() {
		List<ScoreType> scoreTypes = ((ScoreGroupType) trailRecord.channelItem).getScore();
		// find the score labels with a name containing min/max
		int index4Min = -1, index4Max = -1;
		for (int i = 0; i < this.applicableTrailsOrdinals.size(); i++) {
			if (scoreTypes.get(i).getLabel().name().contains("min")) index4Min = i;
			if (scoreTypes.get(i).getLabel().name().contains("max")) index4Max = i;
		}
		if (index4Min != -1 && index4Max != -1) {
			return new int[] { index4Min, index4Max };
		} else {
			return new int[0];
		}
	}
}