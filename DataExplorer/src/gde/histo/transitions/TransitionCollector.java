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

package gde.histo.transitions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import gde.data.RecordSet;
import gde.device.ChannelType;
import gde.device.TransitionClassTypes;
import gde.device.TransitionGroupType;
import gde.device.TransitionType;
import gde.log.Level;
import gde.ui.DataExplorer;

/**
 * Collect the transitions from a recordset.
 * @author Thomas Eickert (USER)
 */
public final class TransitionCollector {
	private final static String	$CLASS_NAME	= TransitionCollector.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * Identify all transitions for the recordset and channel.
	 * Take all transition types defined for the channel.
	 * remove transition duplicates or overlapping transitions in all transition groups.
	 * @param logChannelNumber
	 */
	public static Map<Integer, TreeMap<Long, Transition>> add4Channel(RecordSet recordSet, int logChannelNumber) {
		Map<Integer, TreeMap<Long, Transition>> transitionContainer = new HashMap<>();

		final ChannelType channelType = DataExplorer.application.getActiveDevice().getDeviceConfiguration().getChannel(logChannelNumber);
		for (TransitionType transitionType : channelType.getTransitions().values()) {
			final TreeMap<Long, Transition> transitionsFromRecord = findTransitions(recordSet, transitionType);
			if (!transitionsFromRecord.isEmpty()) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("%d  transitionCount=%d", transitionType.getTransitionId(), 999)); //$NON-NLS-1$

				// assign the transitions to all transition groups which have a mapping to this transition type
				Iterable<TransitionGroupType> iterable = channelType.getTransitionGroups().values().stream()
						.filter(group -> group.getTransitionMapping().stream().anyMatch(mapping -> mapping.getTransitionId() == transitionType.getTransitionId()))::iterator;
				for (TransitionGroupType transitionGroupType : iterable) {
					if (!transitionContainer.containsKey(transitionGroupType.getTransitionGroupId())) {
						// build the container
						transitionContainer.put(transitionGroupType.getTransitionGroupId(), new TreeMap<Long, Transition>());
					}
					final TreeMap<Long, Transition> groupTransitions = transitionContainer.get(transitionGroupType.getTransitionGroupId());

					// merge the new transitions with existing transitions for the current group and class
					groupTransitions.putAll(getSuperiorTransitions(transitionsFromRecord, groupTransitions));

					// eliminate duplicate transitions
					List<Long> duplicates = getDuplicates(groupTransitions);
					for (long timeStamp_ms : duplicates) {
						groupTransitions.remove(timeStamp_ms);
					}
					if (!duplicates.isEmpty()) {
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("%d  removals due to general overlap:  duplicatesSize=%d", transitionType.getTransitionId(), duplicates.size())); //$NON-NLS-1$
					}
				}
			}
		}

		return transitionContainer;
	}

	/**
	 * @param recordSet
	 * @param transitionType
	 * @return
	 */
	private static TreeMap<Long, Transition> findTransitions(RecordSet recordSet, TransitionType transitionType) {
		final TreeMap<Long, Transition> transitionsFromRecord;

		if (transitionType.getClassType() == TransitionClassTypes.PEAK) {
			PeakAnalyzer histoTransitions = new PeakAnalyzer(recordSet);
			transitionsFromRecord = histoTransitions.findTransitions(recordSet.get(recordSet.getRecordNames()[transitionType.getRefOrdinal()]), transitionType);
		}
		else if (transitionType.getClassType() == TransitionClassTypes.PULSE) {
			PulseAnalyzer histoTransitions = new PulseAnalyzer(recordSet);
			transitionsFromRecord = histoTransitions.findTransitions(recordSet.get(recordSet.getRecordNames()[transitionType.getRefOrdinal()]), transitionType);
		}
		else if (transitionType.getClassType() == TransitionClassTypes.SLOPE) {
			SlopeAnalyzer histoTransitions = new SlopeAnalyzer(recordSet);
			transitionsFromRecord = histoTransitions.findTransitions(recordSet.get(recordSet.getRecordNames()[transitionType.getRefOrdinal()]), transitionType);
		}
		else {
			throw new UnsupportedOperationException();
		}

		return transitionsFromRecord;
	}

	/**
	 * Prioritize shorter transitions because this will increase the probability for additional transitions.
	 * @param groupTransitions
	 * @return transitions which overlap in the reference and threshold phases
	 */
	private static List<Long> getDuplicates(final TreeMap<Long, Transition> groupTransitions) {
		List<Long> duplicates;
		duplicates = new ArrayList<Long>();
		Entry<Long, Transition> previousTransitionEntry = null;
		for (Entry<Long, Transition> transitionEntry : groupTransitions.entrySet()) {
			if (previousTransitionEntry != null) {
				Entry<Long, Transition> inferiorTransitionEntry = getInferiorTransition(previousTransitionEntry, transitionEntry).orElse(null);
				if (inferiorTransitionEntry == null)
					previousTransitionEntry = transitionEntry;
				else if (previousTransitionEntry.equals(inferiorTransitionEntry)) {
					duplicates.add(previousTransitionEntry.getKey());
					previousTransitionEntry = transitionEntry;
				}
				else {
					duplicates.add(transitionEntry.getKey());
					// keep previousTransitionEntry for the next overlap check
				}
			}
			else {
				previousTransitionEntry = transitionEntry;
			}
		}
		return duplicates;
	}

	/**
	 * @param transitionsFromRecord
	 * @param groupTransitions
	 * @return the merged transitions with existing transitions for the current group and class
	 */
	private static TreeMap<Long, Transition> getSuperiorTransitions(final TreeMap<Long, Transition> transitionsFromRecord, final TreeMap<Long, Transition> groupTransitions) {
		final TreeMap<Long, Transition> newTransitions;
		newTransitions = new TreeMap<>(transitionsFromRecord);
		if (!groupTransitions.isEmpty()) {
			// identify transitions with the same threshold startTimeStamp
			Set<Long> intersection = new HashSet<Long>(newTransitions.keySet()); // use the copy constructor because the keyset is only a view on the map
			intersection.retainAll(groupTransitions.keySet());
			// discard transitions with the same timestamp
			for (long thresholdStartTimeStamp_ms : intersection) {
				// check which one of the conflicting transitions is inferior and remove it from its parent map
				final Entry<Long, Transition> newTransition = newTransitions.ceilingEntry(thresholdStartTimeStamp_ms);
				getInferiorTransition(groupTransitions.ceilingEntry(thresholdStartTimeStamp_ms), transitionsFromRecord.ceilingEntry(thresholdStartTimeStamp_ms)).ifPresent(x -> {
					if (newTransition.equals(x))
						newTransitions.remove(thresholdStartTimeStamp_ms);
					else
						groupTransitions.remove(thresholdStartTimeStamp_ms);
				});
			}
			if (!intersection.isEmpty()) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("removals due to same timestamp:  intersectionSize=%d", intersection.size())); //$NON-NLS-1$
			}
		}
		return newTransitions;
	}

	/**
	 * Prioritize transitions (peak is top priority, slope is last priority).
	 * The second-order criterion is the reference + threshold duration (shorter is higher priority).
	 * @param entry1
	 * @param entry2
	 * @return the inferior transition in case of overlapping transitions
	 */
	private static Optional<Entry<Long, Transition>> getInferiorTransition(Entry<Long, Transition> entry1, Entry<Long, Transition> entry2) {
		Optional<Entry<Long, Transition>> inferiorTransition = Optional.empty();

		final Transition transition2 = entry2.getValue();
		final Transition transition1 = entry1.getValue();
		final boolean isOverlap = transition2.getReferenceStartTimeStamp_ms() < transition1.getThresholdEndTimeStamp_ms()
				&& transition1.getReferenceStartTimeStamp_ms() < transition2.getThresholdEndTimeStamp_ms(); // !start2.after(end1) && !start1.after(end2)
		final boolean isTransition1Prioritized = (transition1.isPeak() && !transition2.isPeak()) || (transition1.isPulse() && !transition2.isPeak() && !transition2.isPulse());
		final boolean isTransition2Prioritized = (transition2.isPeak() && !transition1.isPeak()) || (transition2.isPulse() && !transition1.isPeak() && !transition1.isPulse());

		if (isOverlap) {
			if (isTransition1Prioritized) {
				inferiorTransition = Optional.of(entry2);
			}
			else if (isTransition2Prioritized) {
				inferiorTransition = Optional.of(entry1);
			}
			else {
				final long transition1Duration = transition1.getThresholdEndTimeStamp_ms() - transition1.getReferenceStartTimeStamp_ms();
				final long transition2Duration = transition2.getThresholdEndTimeStamp_ms() - transition2.getReferenceStartTimeStamp_ms();
				if (transition1Duration < transition2Duration) {
					inferiorTransition = Optional.of(entry2);
				}
				else if (transition2Duration < transition1Duration) {
					inferiorTransition = Optional.of(entry1);
				}
				else {
					// no criterion found
					inferiorTransition = Optional.of(entry1);
				}
			}
		}

		inferiorTransition.ifPresent(x -> log.log(Level.FINER, "discarded due to " + (isTransition1Prioritized || isTransition2Prioritized ? "class:  " : "duration:  "), x.getValue())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return inferiorTransition;
	}

}
