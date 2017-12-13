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
import java.util.Optional;
import java.util.stream.Collectors;

import gde.GDE;
import gde.config.Settings;
import gde.device.TrailDisplayType;
import gde.device.TrailTypes;
import gde.device.resource.DeviceXmlResource;
import gde.log.Logger;

/**
 * Handle the trail type assignment to a trailRecord.
 * @author Thomas Eickert (USER)
 */
public abstract class TrailSelector { // todo consider integrating the selector classes in the trail record  classes
	private static final String				$CLASS_NAME							= TrailSelector.class.getName();
	protected static final Logger			log											= Logger.getLogger($CLASS_NAME);

	protected final Settings					settings								= Settings.getInstance();
	protected final DeviceXmlResource	xmlResource							= DeviceXmlResource.getInstance();

	protected final TrailRecord				trailRecord;

	/**
	 * User selection from applicable trails, is saved in the graphics template
	 */
	protected int											trailTextSelectedIndex	= -1;
	/**
	 * The user may select one of these entries
	 */
	protected List<String>						applicableTrailsTexts;
	/**
	 * Maps all applicable trails in order to convert the user selection into a valid trail
	 */
	protected List<Integer>						applicableTrailsOrdinals;
	/**
	 * The ordinal numbers which hold the extremum values or alternatively the first trail in the channel item
	 */
	protected int[]										extremumOrdinals				= null;

	protected TrailSelector(TrailRecord trailRecord) {
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
		int displaySequence = Integer.MAX_VALUE;
		for (int i = 0; i < this.applicableTrailsOrdinals.size(); i++) {
			int tmpDisplaySequence = (TrailTypes.fromOrdinal(this.applicableTrailsOrdinals.get(i))).getDisplaySequence();
			if (tmpDisplaySequence < displaySequence) {
				displaySequence = tmpDisplaySequence;
				setTrailTextSelectedIndex(i);
			}
		}
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
		return getTrailType().isSuite();
	}

	public boolean isRangePlotSuite() {
		return getTrailType().isRangePlot();
	}

	public boolean isBoxPlotSuite() {
		return getTrailType().isBoxPlot();
	}

	/**
	 * @param replacementKey
	 * @return the replacement name of the specified key or an empty string if there is no key entry
	 */
	protected String getDeviceXmlReplacement(String replacementKey) {
		return replacementKey != null ? this.xmlResource.getReplacement(replacementKey) : GDE.STRING_EMPTY;
	}

	/**
	 * @return the ordinal numbers which hold the extremum values after outlier elimination
	 */
	public int[] getExtremumOrdinals() {
		if (extremumOrdinals == null) setExtremumOrdinals();
		return extremumOrdinals;
	}

	protected void setExtremumOrdinals() {
		int ordinalMin = -1, ordinalMax = -1;
		for (Integer trailOrdinal : this.applicableTrailsOrdinals) {
			if (trailOrdinal == TrailTypes.MIN.ordinal()) {
				ordinalMin = trailOrdinal;
			} else if (trailOrdinal == TrailTypes.MAX.ordinal()) {
				ordinalMax = trailOrdinal;
			}
		}
		if (ordinalMin == -1 || ordinalMax == -1) {
			for (Integer trailOrdinal : this.applicableTrailsOrdinals) {
				if (trailOrdinal == TrailTypes.Q0.ordinal()) {
					ordinalMin = trailOrdinal;
				} else if (trailOrdinal == TrailTypes.Q4.ordinal()) {
					ordinalMax = trailOrdinal;
				}
			}
		}
		if (ordinalMin != -1 && ordinalMax != -1) {
			extremumOrdinals = new int[] { ordinalMin, ordinalMax };
		} else {
			extremumOrdinals = new int[] { this.applicableTrailsOrdinals.get(0), this.applicableTrailsOrdinals.get(0) };
		}
	}

	public abstract void setApplicableTrails();

	protected void setApplicableSuiteTrails() {
		boolean isSmartStatistics = this.settings.isSmartStatistics();

		Optional<TrailDisplayType> trailDisplay = trailRecord.channelItem.getTrailDisplay();
		if (trailDisplay.isPresent()) {
			final List<TrailTypes> displayTrails;
			boolean hideAllTrails = trailDisplay.map(x -> x.isDiscloseAll()).orElse(false);
			if (hideAllTrails) {
				displayTrails = trailDisplay.map(x -> x.getExposed().stream().map(y -> y.getTrail()) //
						.filter(t -> t.isSuite()).filter(t -> t.isSmartStatistics() == isSmartStatistics) //
						.collect(Collectors.toList())).orElse(new ArrayList<TrailTypes>());
			} else {
				List<TrailTypes> disclosedTrails = trailDisplay.map(x -> x.getDisclosed().stream().map(y -> y.getTrail()) //
						.filter(t -> t.isSuite()).filter(t -> t.isSmartStatistics() == isSmartStatistics) //
						.collect(Collectors.toList())).orElse(new ArrayList<TrailTypes>());
				displayTrails = TrailTypes.getSuites().stream() //
						.filter(t -> !disclosedTrails.contains(t)).filter(t -> t.isSmartStatistics() == isSmartStatistics) //
						.collect(Collectors.toList());
			}
			for (TrailTypes suiteTrailType : displayTrails) {
				this.applicableTrailsOrdinals.add(suiteTrailType.ordinal());
				this.applicableTrailsTexts.add(suiteTrailType.getDisplayName().intern());
			}
		} else {
			for (TrailTypes suiteTrailType : TrailTypes.getSuites()) {
				if (suiteTrailType.isSmartStatistics() == isSmartStatistics) {
					this.applicableTrailsOrdinals.add(suiteTrailType.ordinal());
					this.applicableTrailsTexts.add(suiteTrailType.getDisplayName().intern());
				}
			}
		}
	}

}
