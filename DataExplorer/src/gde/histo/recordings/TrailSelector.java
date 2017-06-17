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
import java.util.logging.Logger;
import java.util.stream.Collectors;

import gde.GDE;
import gde.config.Settings;
import gde.device.MeasurementType;
import gde.device.ScoreGroupType;
import gde.device.SettlementType;
import gde.device.StatisticsType;
import gde.device.TrailTypes;
import gde.device.resource.DeviceXmlResource;
import gde.log.Level;
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
	private final MeasurementType		measurementType;																					// measurement / settlement / scoregroup are options
	private final SettlementType		settlementType;																						// measurement / settlement / scoregroup are options
	private final ScoreGroupType		scoreGroupType;																						// measurement / settlement / scoregroup are options

	private int											trailTextSelectedIndex	= -1;															// user selection from applicable trails, is saved in the graphics template
	private List<String>						applicableTrailsTexts;																		// the user may select one of these entries
	private List<Integer>						applicableTrailsOrdinals;																	// maps all applicable trails in order to convert the user selection into a valid trail

	public TrailSelector(TrailRecord trailRecord) {
		this.trailRecord = trailRecord;

		this.measurementType = trailRecord.getMeasurement();
		this.settlementType = trailRecord.getSettlement();
		this.scoreGroupType = trailRecord.getScoregroup();
	}

	/**
	 * Select the most prioritized trail from the applicable trails.
	 */
	public void setMostApplicableTrailTextOrdinal() {
		if (this.scoreGroupType != null) {
			setTrailTextSelectedIndex(0);
		}
		else {
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
	 * Build applicable trail type lists and textIndex for display purposes for one single trail.
	 * @param trailOrdinal
	 */
	public void setApplicableTrailTypes(int trailOrdinal) {
		this.applicableTrailsOrdinals = new ArrayList<Integer>(1);
		this.applicableTrailsOrdinals.add(trailOrdinal);
		this.applicableTrailsTexts = new ArrayList<String>(1);
		this.applicableTrailsTexts.add(TrailTypes.fromOrdinal(trailOrdinal).getDisplayName());
		this.trailTextSelectedIndex = 0;
	}

	/**
	 * Analyze device configuration entries to find applicable trail types.
	 * Build applicable trail type lists for display purposes.
	 * Use device settings trigger texts for trigger trail types and score labels for score trail types; message texts otherwise.
	 */
	public void setApplicableTrailTypes() {
		final boolean[] applicablePrimitiveTrails;
		// step 1: analyze device entries to find applicable primitive trail types
		if (this.measurementType != null) {
			final boolean hideAllTrails = this.measurementType.getTrailDisplay().map(x -> x.isDiscloseAll()).orElse(false);
			if (this.measurementType.getTrailDisplay().map(x -> x.getDefaultTrail()).map(x -> x.isSuite()).orElse(false))
				throw new UnsupportedOperationException("suite trail as a device measurement default"); //$NON-NLS-1$

			if (!hideAllTrails) {
				applicablePrimitiveTrails = getApplicableLegacyTrails();
				// set non-suite trail types : triggered values like count/sum are not supported
				TrailTypes.getPrimitives().stream().filter(x -> !x.isTriggered() && x.isSmartStatistics() == this.settings.isSmartStatistics()).forEach(x -> applicablePrimitiveTrails[x.ordinal()] = true);
			}
			else
				applicablePrimitiveTrails = new boolean[TrailTypes.getPrimitives().size()];

			// set visible and reset hidden trails based on device settlement settings
			this.measurementType.getTrailDisplay().ifPresent(x -> x.getExposed().stream().filter(y -> !y.getTrail().isSuite()).forEach(y -> applicablePrimitiveTrails[y.getTrail().ordinal()] = true));
			this.measurementType.getTrailDisplay().ifPresent(x -> x.getDisclosed().stream().filter(y -> !y.getTrail().isSuite()).forEach(y -> applicablePrimitiveTrails[y.getTrail().ordinal()] = false));

			// set at least one trail if no trail is applicable
			boolean hasApplicablePrimitiveTrails = false;
			for (boolean value : applicablePrimitiveTrails) {
				hasApplicablePrimitiveTrails = value;
				if (hasApplicablePrimitiveTrails) break;
			}
			if (!hasApplicablePrimitiveTrails) applicablePrimitiveTrails[this.measurementType.getTrailDisplay().map(x -> x.getDefaultTrail()).orElse(TrailTypes.getSubstitute()).ordinal()] = true;
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.trailRecord.getName() + " data " + Arrays.toString(applicablePrimitiveTrails)); //$NON-NLS-1$

			// build applicable trail type lists for display purposes
			this.applicableTrailsOrdinals = new ArrayList<Integer>();
			this.applicableTrailsTexts = new ArrayList<String>();
			for (int i = 0; i < applicablePrimitiveTrails.length; i++) {
				if (applicablePrimitiveTrails[i]) {
					this.applicableTrailsOrdinals.add(i);
					if (TrailTypes.values[i].isTriggered()) {
						if (TrailTypes.values[i].equals(TrailTypes.REAL_COUNT_TRIGGERED)) {
							this.applicableTrailsTexts.add(getDeviceXmlReplacement(getStatistics().getCountTriggerText()));
						}
						else if (TrailTypes.values[i].equals(TrailTypes.REAL_SUM_TRIGGERED)) {
							this.applicableTrailsTexts.add(getDeviceXmlReplacement(getStatistics().getSumTriggerText()));
						}
						else if (TrailTypes.values[i].equals(TrailTypes.REAL_TIME_SUM_TRIGGERED)) {
							this.applicableTrailsTexts.add(getDeviceXmlReplacement(getStatistics().getSumTriggerTimeText()));
						}
						else if (TrailTypes.values[i].equals(TrailTypes.REAL_AVG_RATIO_TRIGGERED)) {
							this.applicableTrailsTexts.add(getDeviceXmlReplacement(getStatistics().getRatioText()));
						}
						else if (TrailTypes.values[i].equals(TrailTypes.REAL_MAX_RATIO_TRIGGERED)) {
							this.applicableTrailsTexts.add(getDeviceXmlReplacement(getStatistics().getRatioText()));
						}
						else
							throw new UnsupportedOperationException("TrailTypes.isTriggered"); //$NON-NLS-1$
					}
					else {
						this.applicableTrailsTexts.add(TrailTypes.values[i].getDisplayName().intern());
					}
				}
			}

			if (!this.measurementType.getTrailDisplay().map(x -> x.isDiscloseAll()).orElse(false)) {
				// define trail suites which are applicable for display
				List<TrailTypes> exposed = this.measurementType.getTrailDisplay().map(x -> x.getExposed().stream().map(y -> y.getTrail()).collect(Collectors.toList())).orElse(new ArrayList<TrailTypes>());
				List<TrailTypes> disclosed = this.measurementType.getTrailDisplay().map(x -> x.getDisclosed().stream().map(y -> y.getTrail()).collect(Collectors.toList())).orElse(new ArrayList<TrailTypes>());
				for (TrailTypes suiteTrailType : TrailTypes.getSuites()) {
					if ((suiteTrailType.isSmartStatistics() == this.settings.isSmartStatistics() || exposed.contains(suiteTrailType)) && !disclosed.contains(suiteTrailType)) {
						this.applicableTrailsOrdinals.add(suiteTrailType.ordinal());
						this.applicableTrailsTexts.add(suiteTrailType.getDisplayName().intern());
					}
				}
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.trailRecord.getName() + " texts " + this.applicableTrailsTexts); //$NON-NLS-1$
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.trailRecord.getName() + " ordinals " + this.applicableTrailsOrdinals); //$NON-NLS-1$
		}

		else if (this.settlementType != null) {
			if (this.settlementType.getTrailDisplay().map(x -> x.getDefaultTrail()).map(x -> x.isSuite()).orElse(false))
				throw new UnsupportedOperationException("suite trail as a device settlement default"); //$NON-NLS-1$

			applicablePrimitiveTrails = getApplicableSettlementTrails();

			if (!this.settlementType.getTrailDisplay().map(x -> x.isDiscloseAll()).orElse(false)) {
				// define trail suites which are applicable for display
				List<TrailTypes> exposed = this.settlementType.getTrailDisplay() //
						.map(x -> x.getExposed().stream().map(y -> y.getTrail()).collect(Collectors.toList())) // collect all the trails from the collection of exposed TrailVisibility members
						.orElse(new ArrayList<TrailTypes>()); // 																									get an empty list if there is no trailDisplay tag in the device.xml for this settlement
				List<TrailTypes> disclosed = this.settlementType.getTrailDisplay().map(x -> x.getDisclosed().stream().map(y -> y.getTrail()).collect(Collectors.toList())).orElse(new ArrayList<TrailTypes>());
				for (TrailTypes suiteTrailType : TrailTypes.getSuites()) {
					if ((suiteTrailType.isSmartStatistics() == this.settings.isSmartStatistics() || exposed.contains(suiteTrailType)) && !disclosed.contains(suiteTrailType)) {
						this.applicableTrailsOrdinals.add(suiteTrailType.ordinal());
						this.applicableTrailsTexts.add(suiteTrailType.getDisplayName().intern());
					}
				}
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.trailRecord.getName() + " texts " + this.applicableTrailsTexts); //$NON-NLS-1$
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.trailRecord.getName() + " ordinals " + this.applicableTrailsOrdinals); //$NON-NLS-1$
		}
		else if (this.scoreGroupType != null) {
			applicablePrimitiveTrails = new boolean[0]; // not required

			// build applicable trail type lists for display purposes
			this.applicableTrailsOrdinals = new ArrayList<Integer>();
			this.applicableTrailsTexts = new ArrayList<String>();
			if (this.scoreGroupType != null) {
				for (int i = 0; i < this.scoreGroupType.getScore().size(); i++) {
					this.applicableTrailsOrdinals.add(this.scoreGroupType.getScore().get(i).getLabel().ordinal());
					this.applicableTrailsTexts.add(getDeviceXmlReplacement(this.scoreGroupType.getScore().get(i).getValue()));
				}
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.trailRecord.getName() + " score "); //$NON-NLS-1$
			}
		}
		else {
			throw new UnsupportedOperationException(" >>> no trails found <<< "); //$NON-NLS-1$
		}
	}

	/**
	 * @return
	 */
	private boolean[] getApplicableLegacyTrails() {
		final boolean[] applicablePrimitiveTrails;
		applicablePrimitiveTrails = new boolean[TrailTypes.getPrimitives().size()];
		StatisticsType measurementStatistics = getStatistics();
		if (!this.settings.isSmartStatistics()) {
			if (measurementStatistics != null) {
				if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
					applicablePrimitiveTrails[TrailTypes.REAL_SUM_TRIGGERED.ordinal()] = (measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1);
					if (measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1 && measurementStatistics.getRatioRefOrdinal() != null) {
						StatisticsType referencedStatistics = DataExplorer.application.getActiveDevice().getMeasurementStatistic(this.trailRecord.getParentTrail().getChannelConfigNumber(),
								measurementStatistics.getRatioRefOrdinal());
						applicablePrimitiveTrails[TrailTypes.REAL_AVG_RATIO_TRIGGERED.ordinal()] = referencedStatistics.isAvg();
						applicablePrimitiveTrails[TrailTypes.REAL_MAX_RATIO_TRIGGERED.ordinal()] = referencedStatistics.isMax();
					}
				}
				applicablePrimitiveTrails[TrailTypes.REAL_TIME_SUM_TRIGGERED.ordinal()] = (measurementStatistics.getTrigger() != null && measurementStatistics.getSumTriggerTimeText() != null
						&& measurementStatistics.getSumTriggerTimeText().length() > 1);
				applicablePrimitiveTrails[TrailTypes.REAL_COUNT_TRIGGERED.ordinal()] = (measurementStatistics.isCountByTrigger() != null);
			}
			// applicablePrimitiveTrails[TrailTypes.REAL_SUM.ordinal()] = false; // in settlements only
		}
		return applicablePrimitiveTrails;
	}

	/**
	 * @param applicablePrimitiveTrails
	 */
	private boolean[] getApplicableSettlementTrails() {
		boolean[] applicablePrimitiveTrails = new boolean[TrailTypes.getPrimitives().size()];

		// set quantile-based non-suite trail types : triggered value sum are CURRENTLY not supported
		if (!this.settlementType.getTrailDisplay().map(x -> x.isDiscloseAll()).orElse(false)) {
			if (this.settlementType.getEvaluation().getTransitionAmount() == null)
				TrailTypes.getPrimitives().stream().filter(x -> !x.isTriggered() && x.isSmartStatistics() == this.settings.isSmartStatistics()).forEach(x -> applicablePrimitiveTrails[x.ordinal()] = true);
			else
				throw new UnsupportedOperationException("TransitionAmount not implemented"); //$NON-NLS-1$
		}

		// set visible non-suite trails based on device settlement settings
		this.settlementType.getTrailDisplay()
				.ifPresent(x -> x.getExposed().stream().map(z -> z.getTrail()) //
						.filter(o -> !o.isSuite()) //
						.forEach(y -> applicablePrimitiveTrails[y.ordinal()] = true));

		// reset hidden non-suite trails based on device settlement settings
		this.settlementType.getTrailDisplay().ifPresent(x -> x.getDisclosed().stream().map(z -> z.getTrail()).filter(o -> !o.isSuite()).forEach(y -> applicablePrimitiveTrails[y.ordinal()] = false));

		// set at least one trail if no trail is applicable
		boolean hasApplicablePrimitiveTrails = false;
		for (boolean value : applicablePrimitiveTrails) {
			hasApplicablePrimitiveTrails = value;
			if (hasApplicablePrimitiveTrails) break;
		}
		if (!hasApplicablePrimitiveTrails) applicablePrimitiveTrails[this.settlementType.getTrailDisplay().map(x -> x.getDefaultTrail()).orElse(TrailTypes.getSubstitute()).ordinal()] = true;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.trailRecord.getName() + " data " + Arrays.toString(applicablePrimitiveTrails)); //$NON-NLS-1$

		// build applicable trail type lists for display purposes
		this.applicableTrailsOrdinals = new ArrayList<Integer>();
		this.applicableTrailsTexts = new ArrayList<String>();
		for (int i = 0; i < applicablePrimitiveTrails.length; i++) {
			if (applicablePrimitiveTrails[i]) {
				this.applicableTrailsOrdinals.add(i);
				this.applicableTrailsTexts.add(TrailTypes.values[i].getDisplayName().intern());
			}
		}
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
		return this.scoreGroupType == null ? getTrailType().isSuite() : false;
	}

	public boolean isRangePlotSuite() {
		return this.scoreGroupType == null ? getTrailType().isRangePlot() : false;
	}

	public boolean isBoxPlotSuite() {
		return this.scoreGroupType == null ? getTrailType().isBoxPlot() : false;
	}

	public StatisticsType getStatistics() {
		return this.measurementType.getStatistics();
	}

	/**
	 * @param replacementKey
	 * @return the replacement name of the specified key or an empty string if there is no key entry
	 */
	private String getDeviceXmlReplacement(String replacementKey) {
		return replacementKey != null ? this.xmlResource.getReplacement(replacementKey) : GDE.STRING_EMPTY;
	}

}
