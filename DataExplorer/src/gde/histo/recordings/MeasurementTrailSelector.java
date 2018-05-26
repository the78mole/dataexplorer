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

import gde.GDE;
import gde.device.IChannelItem;
import gde.device.MeasurementType;
import gde.device.StatisticsType;
import gde.device.TrailDisplayType;
import gde.device.TrailTypes;
import gde.device.TrailVisibilityType;

/**
 * Handle the trail type assignment to a trailRecord.
 * @author Thomas Eickert
 */
public final class MeasurementTrailSelector extends TrailSelector {

	private String	triggerScaleRawText	= GDE.STRING_EMPTY;
	private String	triggerScaleUnit		= GDE.STRING_EMPTY;

	public MeasurementTrailSelector(TrailRecord trailRecord) {
		super(trailRecord);
	}

	/**
	 * @param channelNumber is the 1-based device channel number
	 * @param channelItem is a measurement / settlement / scoregroup in the device channel
	 * @param recordName is the name of the data record which might differ from the device channel item name (e.g. Jeti)
	 * @param smartStatistics true selects the smart trail types
	 */
	public MeasurementTrailSelector(String deviceName, int channelNumber, IChannelItem channelItem, String recordName, boolean smartStatistics) {
		super(deviceName, channelNumber, channelItem, recordName, smartStatistics);
	}

	@Override
	protected void setApplicableTrails() {

		final boolean[] applicablePrimitiveTrails = getApplicablePrimitiveTrails();

		// build applicable trail type lists for display purposes
		this.applicableTrailsOrdinals = new ArrayList<Integer>();
		this.applicableTrailsTexts = new ArrayList<String>();
		for (int i = 0; i < applicablePrimitiveTrails.length; i++) {
			if (applicablePrimitiveTrails[i]) {
				this.applicableTrailsOrdinals.add(i);
				this.applicableTrailsTexts.add(TrailTypes.VALUES[i].getDisplayNameWithTriggerText(channelItem));
			}
		}

		setApplicableSuiteTrails();
		log.finer(() -> recordName + " texts " + this.applicableTrailsTexts);
		log.finer(() -> recordName + " ordinals " + this.applicableTrailsOrdinals);
	}

	/**
	 * Determine an array with index based on the trail primitives ordinal number.
	 * @return the array giving the information which trails are visible for the user
	 */
	private boolean[] getApplicablePrimitiveTrails() {
		final boolean[] applicablePrimitiveTrails;
		Optional<TrailDisplayType> trailDisplay = channelItem.getTrailDisplay();
		if (trailDisplay.map(TrailDisplayType::getDefaultTrail).map(TrailTypes::isSuite).orElse(false)) throw new UnsupportedOperationException(
				"suite trail must not be a device measurement default");

		boolean hideAllTrails = trailDisplay.map(TrailDisplayType::isDiscloseAll).orElse(false);
		if (hideAllTrails) {
			applicablePrimitiveTrails = new boolean[TrailTypes.getPrimitives().size()];
			trailDisplay.ifPresent(x -> x.getExposed().stream().map(TrailVisibilityType::getTrail) //
					.filter(t -> !t.isSuite()).filter(t -> t.isSmartStatistics() == smartStatistics) //
					.forEach(t -> applicablePrimitiveTrails[t.ordinal()] = true));
		} else {
			applicablePrimitiveTrails = getApplicableLegacyTrails();
			// set non-suite trail types : triggered values like count/sum are not supported
			TrailTypes.getPrimitives().stream() //
					.filter(t -> !t.isTriggered()).filter(t -> t.isSmartStatistics() == smartStatistics) //
					.forEach(t -> applicablePrimitiveTrails[t.ordinal()] = true);
			// set visible and reset hidden trails based on device settlement settings
			trailDisplay.ifPresent(x -> x.getDisclosed().stream().map(TrailVisibilityType::getTrail) //
					.filter(t -> !t.isSuite()).filter(t -> t.isSmartStatistics() == smartStatistics) //
					.forEach(t -> applicablePrimitiveTrails[t.ordinal()] = false));
		}

		// set at least one trail if no trail is applicable
		if (!hasTrueValues(applicablePrimitiveTrails))
			applicablePrimitiveTrails[trailDisplay.map(TrailDisplayType::getDefaultTrail).orElse(TrailTypes.getSubstitute()).ordinal()] = true;
		log.finer(() -> recordName + " data " + Arrays.toString(applicablePrimitiveTrails));
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
		if (!smartStatistics && ((MeasurementType) channelItem).getStatistics() != null) {
			StatisticsType measurementStatistics = ((MeasurementType) channelItem).getStatistics();
			if (measurementStatistics.getSumByTriggerRefOrdinal() != null) {
				applicablePrimitiveTrails[TrailTypes.REAL_SUM_TRIGGERED.ordinal()] = (measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1);
			}
			if (measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1 && measurementStatistics.getRatioRefOrdinal() != null) {
				applicablePrimitiveTrails[TrailTypes.REAL_MAX_RATIO_TRIGGERED.ordinal()] = true;
			}
			applicablePrimitiveTrails[TrailTypes.REAL_TIME_SUM_TRIGGERED.ordinal()] = (measurementStatistics.getTrigger() != null && measurementStatistics.getSumTriggerTimeText() != null && measurementStatistics.getSumTriggerTimeText().length() > 1);
			applicablePrimitiveTrails[TrailTypes.REAL_COUNT_TRIGGERED.ordinal()] = (measurementStatistics.isCountByTrigger() != null);
			// applicablePrimitiveTrails[TrailTypes.REAL_SUM.ordinal()] = false; // in settlements only
		}
		return applicablePrimitiveTrails;
	}

	/**
	 * Select the trail record (or the suite if applicable) if the selection has changed.
	 * @param value position / index of the trail type in the current list of applicable trails
	 */
	@Override // reason is trigger texts
	public void setTrailTextSelectedIndex(int value) {
		this.trailTextSelectedIndex = value;
		setTriggerScaleTexts(getTrailType());
	}

	protected void setTriggerScaleTexts(TrailTypes trailType) {
		if (!smartStatistics || ((MeasurementType) channelItem).getStatistics() == null) {
			StatisticsType measurementStatistics = ((MeasurementType) channelItem).getStatistics();
			if (trailType == TrailTypes.REAL_SUM_TRIGGERED //
					&& measurementStatistics.getSumTriggerText() != null && measurementStatistics.getSumTriggerText().length() > 1 && measurementStatistics.getSumByTriggerRefOrdinal() != null) {
				this.triggerScaleRawText = measurementStatistics.getSumTriggerText();
				this.triggerScaleUnit = channelItem.getUnit();
			} else if ((trailType == TrailTypes.REAL_MAX_RATIO_TRIGGERED) //
					&& measurementStatistics.getRatioText() != null && measurementStatistics.getRatioText().length() > 1 && measurementStatistics.getRatioRefOrdinal() != null) {
				this.triggerScaleRawText = measurementStatistics.getRatioText();
				this.triggerScaleUnit = GDE.STRING_SLASH + channelItem.getUnit();
			} else if (trailType == TrailTypes.REAL_TIME_SUM_TRIGGERED //
					&& measurementStatistics.getTrigger() != null && measurementStatistics.getSumTriggerTimeText() != null && measurementStatistics.getSumTriggerTimeText().length() > 1) {
				this.triggerScaleRawText = measurementStatistics.getSumTriggerTimeText();
				this.triggerScaleUnit = "sec";
			} else if (trailType == TrailTypes.REAL_COUNT_TRIGGERED //
					&& measurementStatistics.isCountByTrigger() != null) {
				this.triggerScaleRawText = measurementStatistics.getCountTriggerText();
				this.triggerScaleUnit = "1";
			}
		} else {
			this.triggerScaleRawText = GDE.STRING_EMPTY;
			this.triggerScaleUnit = GDE.STRING_EMPTY;
		}
	}

	/**
	 * Trigger values (e.g. text_trigger_motor_sum) require individual scale texts.
	 * @return the scale text prior to XML replacement
	 */
	public String getTriggerScaleRawText() {
		return this.triggerScaleRawText;
	}

	/**
	 * Trigger values (e.g. text_trigger_count) require individual scale units.
	 * @return the scale unit text or the denominator in case of a trigger ratio (prefixed by '/')
	 */
	public String getTriggerScaleUnit() {
		return this.triggerScaleUnit;
	}
}