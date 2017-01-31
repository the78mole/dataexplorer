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
package gde.data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.device.ChannelPropertyType;
import gde.device.ChannelPropertyTypes;
import gde.device.ChannelType;
import gde.device.IDevice;
import gde.device.TransitionClassTypes;
import gde.device.TransitionGroupType;
import gde.device.TransitionType;
import gde.device.TransitionValueTypes;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

/**
 * holds all transitions for the history recordSet.
 * the transitions are sorted by timestamp.
 * @author Thomas Eickert
 */
public class HistoTransitions {
	final static String															$CLASS_NAME	= HistoTransitions.class.getName();
	final static Logger															log					= Logger.getLogger($CLASS_NAME);

	private final DataExplorer											application	= DataExplorer.getInstance();
	private final Settings													settings		= Settings.getInstance();
	private final IDevice														device			= this.application.getActiveDevice();

	private final RecordSet													recordSet;

	/**
	 * hierarchical container for all transitions.
	 * holds only groups which actually have transitions.
	 * keys: transitionGroupId, thresholdStartTimestamp
	 */
	private Map<Integer, TreeMap<Long, Transition>>	transitionContainer;

	private SettlementDeque													referenceDeque;
	private SettlementDeque													thresholdDeque;
	private SettlementDeque													recoveryDeque;
	private TriggerState														previousTriggerState;
	private TriggerState														triggerState;
	private int																			transitionCount;

	private enum TriggerState {
		/**
		 * check measurements until the reference duration is satisfied and the measurement value goes beyond the trigger threshold  
		 */
		WAITING,
		/**
		 * threshold conditions (level and duration) apply to all threshold measurements.
		 * for peaks this phase includes a level hysteresis within the threshold duration: the measurement value must not exceed the recovery level.
		 */
		TRIGGERED,
		/**
		 * check measurements until the recovery duration is satisfied and the measurement value stays within the recovery threshold  
		 */
		RECOVERING;
	}

	/**
	 * provides the extremum value of the deque for reference purposes and the information if the deque has sufficient elements for the deque time period.
	 * ensures the maximum deque time period by removing elements from the front of the deque.
	 * expects elements to be added in chronological order.
	 * Does not support null measurement values.
	 * Resizable array implementation of the Deque interface.
	 * @author Thomas Eickert
	 */
	private class SettlementDeque extends ArrayDeque<Double> {

		private static final long	serialVersionUID	= 915484098600135376L;

		private static final int	capacitySurplus		= 11;									// plus 11 elements ensures sufficient elements for most cases

		private boolean						isMinimumExtremum;
		private long							timePeriod_100ns;
		private double						extremeValue;
		private boolean						isExhausted;
		private int								startIndex;

		private ArrayDeque<Long>	timeStampDeque;
		private List<Double>			sortedValues;

		/**
		 * be sure to call initialize before using the deque.
		 * @param numElements
		 * @param isMinimumExtremum true if the extreme value shall determine the minimum of all values since initialize / clear 
		 * @param timePeriod_100ns
		 * @param startIndex is the source record index at the initialize call
		 */
		SettlementDeque(int numElements, boolean isMinimumExtremum, long timePeriod_100ns) {
			super(numElements + SettlementDeque.capacitySurplus);
			this.isMinimumExtremum = isMinimumExtremum;
			this.timePeriod_100ns = timePeriod_100ns;
			this.timeStampDeque = new ArrayDeque<Long>(numElements);
			this.sortedValues = null;
			clear();
		}

		/*
		 * (non-Javadoc)
		 * ensures the maximum deque time period by removing elements from the front of the decque.
		 * 
		 * @see java.util.ArrayDeque#add(java.lang.Object)
		 */
		@Override
		@Deprecated
		public boolean add(Double translatedValue) {
			throw new UnsupportedOperationException();
		}

		public void addFirst(double translatedValue, long timeStamp_100ns) {
			this.addFirst(translatedValue);
			this.timeStampDeque.addFirst(timeStamp_100ns);
			ensureTimePeriod();
		}

		/*
		 * (non-Javadoc)
		 * ensures the maximum deque time period by removing elements from the front of the deque.
		 * 
		 * @see java.util.ArrayDeque#addFirst(java.lang.Object)
		 */
		public void addFirst(double translatedValue) {
			super.addFirst(translatedValue);
			this.extremeValue = this.isMinimumExtremum ? Math.min(this.extremeValue, translatedValue) : Math.max(this.extremeValue, translatedValue);
			this.sortedValues = null;
		}

		public void addLast(double translatedValue, long timeStamp_100ns) {
			this.addLast(translatedValue);
			this.timeStampDeque.addLast(timeStamp_100ns);
			ensureTimePeriod();
		}

		/*
		 * (non-Javadoc)
		 * ensures the maximum deque time period by removing elements from the front of the decque.
		 * 
		 * @see java.util.ArrayDeque#addLast(java.lang.Object)
		 */
		@Override
		public void addLast(Double translatedValue) {
			super.addLast(translatedValue);
			this.extremeValue = this.isMinimumExtremum ? Math.min(this.extremeValue, translatedValue) : Math.max(this.extremeValue, translatedValue);
			this.sortedValues = null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#clear()
		 */
		@Override
		public void clear() {
			super.clear();
			this.timeStampDeque.clear();
			this.extremeValue = this.isMinimumExtremum ? Double.MAX_VALUE : -Double.MAX_VALUE;
			this.isExhausted = false;
			//  leave untouched because of initialize()  this.startIndex = -1;
			this.sortedValues = null;
		}

		/**
		 * @param newStartIndex is the source record index at the initialize call
		 */
		public void initialize(int newStartIndex) {
			clear();
			this.startIndex = newStartIndex;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#clone()
		 */
		@Override
		@Deprecated
		public SettlementDeque clone() {
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 * ensures the maximum deque time period by removing elements from the front of the decque.
		 * 
		 * @see java.util.ArrayDeque#offer(java.lang.Object)
		 */
		@Override
		@Deprecated
		public boolean offer(Double translatedValue) {
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 * ensures the maximum deque time period by removing elements from the front of the decque.
		 * 
		 * @see java.util.ArrayDeque#offerFirst(java.lang.Object)
		 */
		@Override
		@Deprecated
		public boolean offerFirst(Double translatedValue) {
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 * ensures the maximum deque time period by removing elements from the front of the decque.
		 * 
		 * @see java.util.ArrayDeque#offerLast(java.lang.Object)
		 */
		@Override
		@Deprecated
		public boolean offerLast(Double translatedValue) {
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#poll()
		 */
		@Override
		@Deprecated
		public Double poll() {
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#pollFirst()
		 */
		public Double pollFirst() {
			Double removedItem = super.pollFirst();
			this.timeStampDeque.pollFirst();
			setExtremeValue(removedItem);
			if (removedItem != null) this.isExhausted = false;
			this.sortedValues = null;
			return removedItem;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#pollLast()
		 */
		public Double pollLast() {
			Double removedItem = super.pollLast();
			this.timeStampDeque.pollLast();
			setExtremeValue(removedItem);
			if (removedItem != null) this.isExhausted = false;
			this.sortedValues = null;
			return removedItem;
		}

		/*
		 * (non-Javadoc)
		 * ensures the maximum deque time period by removing elements from the front of the decque.
		 * 
		 * @see java.util.ArrayDeque#push(java.lang.Object)
		 */
		@Override
		@Deprecated
		public void push(Double translatedValue) {
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#pop()
		 */
		@Override
		@Deprecated
		public Double pop() {
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#remove()
		 */
		@Override
		@Deprecated
		public Double remove() {
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#removeFirst()
		 */
		@Override
		@Deprecated
		public Double removeFirst() {
			// use pollFirst()
			throw new UnsupportedOperationException();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#removeLast()
		 */
		public Double removeLast() {
			// use pollLast()
			throw new UnsupportedOperationException();
		}

		/**
		 * inserts the specified element if the deque time period is not exceeded. 
		 * @param dequePair
		 * @return false if the timestamp is beyond the deque time period or if the element already exists in the deque. 
		 */
		public boolean tryAddLast(double translatedValue, long timeStamp_100ns) {
			if (this.timeStampDeque.peekFirst() + this.timePeriod_100ns >= timeStamp_100ns) {
				this.addLast(translatedValue, timeStamp_100ns);
				return true;
			}
			else {
				this.isExhausted = true;
				return false;
			}
		}

		/**
		 * moves all deque entries and leaves an cleared deque.
		 * @param settlementDeque
		 */
		public void addLastByMoving(SettlementDeque settlementDeque) {
			Iterator<Long> iteratorTimeStamp = settlementDeque.timeStampDeque.iterator();
			for (Iterator<Double> iterator = settlementDeque.iterator(); iterator.hasNext();) {
				this.addLast(iterator.next(), iteratorTimeStamp.next());
			}
			settlementDeque.clear();
		}

		/**
		 * moves entries until the deque time period is exceeded.
		 * leaves a reduced deque.
		 * @param settlementDeque
		 * @return the number of deque entries moved
		 */
		public int tryAddLastByMoving(SettlementDeque settlementDeque) {
			int movedCount = 0;
			Iterator<Long> iteratorTimeStamp = settlementDeque.timeStampDeque.iterator();
			for (Iterator<Double> iterator = settlementDeque.iterator(); iterator.hasNext();) {
				if (this.tryAddLast(iterator.next(), iteratorTimeStamp.next())) {
					iterator.remove();
					iteratorTimeStamp.remove();
					movedCount++;
				}
				else {
					break;
				}
			}
			if (settlementDeque.isEmpty()) settlementDeque.clear();
			return movedCount;
		}

		/**
		 * @return true if the time distance of the first and the last deque elements equals the deque time period.
		 */
		public boolean isFilledToCapacity() {
			return this.timeStampDeque.peekFirst() + this.timePeriod_100ns == this.timeStampDeque.peekLast();
		}

		/**
		 * @return true if the next add will be most likely beyond the current time window position. however, a smaller timestep than the last one may fit.
		 */
		public boolean isStuffed() {
			return this.isExhausted || isFilledToCapacity();
		}

		private void ensureTimePeriod() {
			this.isExhausted = false;
			if (!this.timeStampDeque.isEmpty()) {
				for (int i = 0; i < this.timeStampDeque.size(); i++) {
					if (this.timeStampDeque.peekFirst() + this.timePeriod_100ns < this.timeStampDeque.peekLast()) {
						this.pollFirst(); // polls timestampDeque also
						this.isExhausted = true;
					}
					else {
						break;
					}
				}
			}
		}

		public List<Double> getSortedValues() {
			if (this.sortedValues == null) {
				this.sortedValues = Arrays.asList(this.toArray(new Double[0]));
				Collections.sort(this.sortedValues);
			}
			return this.sortedValues;
		}

		/**
		 * please note: requires a sort over the full deque size.
		 * @param probabilityCutPoint as value between 0 and 1
		 * @return the quantileValue of the current deque contents
		 */
		public double getQuantileValue(double probabilityCutPoint) {
			// IS_SAMPLE
			final int realSize = this.getSortedValues().size();
			if (realSize > 0) {
				if (probabilityCutPoint >= 1. / (realSize + 1) && probabilityCutPoint < (double) realSize / (realSize + 1)) {
					double position = (realSize + 1) * probabilityCutPoint;
					return this.getSortedValues().get((int) position - 1) + (position - (int) position) * (this.getSortedValues().get((int) position) - this.getSortedValues().get((int) position - 1));
				}
				else if (probabilityCutPoint < 1. / (realSize + 1))
					return this.getSortedValues().get(0);
				else
					return this.getSortedValues().get(realSize - 1);
			}
			else
				throw new UnsupportedOperationException();
		}

		/**
		 * please note: requires a sort over the full deque size.
		 * @return NaN for empty deque or a leveled extremum value for comparisons, i.e. a more stable value than the extremum value  
		 */
		public double getBenchmarkValue() {
			if (Settings.getInstance().getMinmaxQuantileDistance() == 0.) {
				// bypass deque sort
				return this.extremeValue;
			}
			else {
				return this.isMinimumExtremum ? this.getQuantileValue(Settings.getInstance().getMinmaxQuantileDistance()) : this.getQuantileValue(1. - Settings.getInstance().getMinmaxQuantileDistance());
			}
		}

		/**
		 * please note: requires a sort over the full deque size.
		 * @return NaN for empty deque or a leveled value for security comparisons which shall ensure that the majority of the values did NOT pass a comparison level  
		 */
		public double getSecurityValue() {
			if (this.isEmpty()) {
				return Double.NaN;
			}
			else {
				return this.isMinimumExtremum ? this.getQuantileValue(1. - Settings.getInstance().getMinmaxQuantileDistance()) : this.getQuantileValue(Settings.getInstance().getMinmaxQuantileDistance());
			}
		}

		/**
		 * please note: performs a loop over the full deque size.
		 * @return the averageValue of the current deque contents
		 */
		public double getAverageValue() {
			double averageValue = 0.;
			int i = 0;
			for (double translatedValue : this) {
				averageValue += (translatedValue - averageValue) / ++i;
			}
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, String.format("averageValue=%f", averageValue)); //$NON-NLS-1$
			return averageValue;
		}

		private void setExtremeValue(double removedValue) {
			if (this.isEmpty()) {
				this.extremeValue = this.isMinimumExtremum ? Double.MAX_VALUE : -Double.MAX_VALUE;
			}
			else if (removedValue == this.extremeValue) {
				// avoid the calculation of a new extreme value if the neighbor value is identical
				if (removedValue != this.peekFirst()) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, String.format("removedValue=%f extremeValue=%f", removedValue, this.extremeValue)); //$NON-NLS-1$
					this.extremeValue = this.isMinimumExtremum ? getQuantileValue(0.) : getQuantileValue(1.);
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, String.format("extremeValue=%f", this.extremeValue)); //$NON-NLS-1$
				}
			}
		}

		public List<Double> getTranslatedValues() {
			return Arrays.asList(this.toArray(new Double[0]));
		}

		public List<Long> getTimestamps_100ns() {
			return Arrays.asList(this.timeStampDeque.toArray(new Long[0]));
		}

		public String getFormatedDuration(int index) {
			return StringHelper.getFormatedDuration("HH:mm:ss.SSS", this.getTimestamps_100ns().get(index) / 10); //$NON-NLS-1$
		}

		/**
		 * @return the isMinimumExtremum
		 */
		public boolean isMinimumExtremum() {
			return this.isMinimumExtremum;
		}

		/**
		 * @return the timePeriod_100ns
		 */
		public long getTimePeriod_100ns() {
			return this.timePeriod_100ns;
		}

		/**
		 * @return the extremeValue of the current deque contents
		 */
		public double getExtremeValue() {
			return this.extremeValue;
		}

		/**
		 * @return true if the last add provoked dropping at least one element from the front of the deque.
		 */
		public boolean isExhausted() {
			return this.isExhausted;
		}

		/**
		 * @return the startIndex
		 */
		public int getStartIndex() {
			return this.startIndex;
		}

	}

	public HistoTransitions(RecordSet recordSet) {
		this.recordSet = recordSet;
	}

	/**
	 * identify all transitions for the recordset and channel.
	 * remove transition duplicates or overlapping transitions in all transition groups.
	 * @param logChannelNumber
	 * @return false if no valid transition was identified
	 */
	public void add4Channel(int logChannelNumber) {
		this.transitionCount = 0;
		this.transitionContainer = new HashMap<Integer, TreeMap<Long, Transition>>();

		// step: determine all transitions for all transition types defined for the channel
		final ChannelType channelType = this.device.getDeviceConfiguration().getChannel(logChannelNumber);
		for (TransitionType transitionType : channelType.getTransitions().values()) {
			final TreeMap<Long, Transition> transitionsFromRecord = addFromRecord(this.recordSet.get(this.recordSet.getRecordNames()[transitionType.getRefOrdinal()]), transitionType);

			if (!transitionsFromRecord.isEmpty()) {
				this.transitionCount += transitionsFromRecord.size();
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("%d  transitionCount=%d", transitionType.getTransitionId(), this.transitionCount)); //$NON-NLS-1$

				// assign the transitions to all transition groups which have a mapping to this transition type 
				Iterable<TransitionGroupType> iterable = channelType.getTransitionGroups().values().stream()
						.filter(group -> group.getTransitionMapping().stream().anyMatch(mapping -> mapping.getTransitionId() == transitionType.getTransitionId()))::iterator;
				for (TransitionGroupType transitionGroupType : iterable) {
					if (!this.transitionContainer.containsKey(transitionGroupType.getTransitionGroupId())) {
						// build the container 
						TreeMap<Long, Transition> groupTransitions = new TreeMap<Long, Transition>();
						this.transitionContainer.put(transitionGroupType.getTransitionGroupId(), groupTransitions);
					}

					// add or merge the transitions
					final TreeMap<Long, Transition> selectedTransitions = this.transitionContainer.get(transitionGroupType.getTransitionGroupId());
					if (!selectedTransitions.isEmpty()) {
						// identify transitions with the same threshold startTimeStamp
						Set<Long> intersection = new HashSet<Long>(transitionsFromRecord.keySet()); // use the copy constructor because the keyset is only a view on the map
						intersection.retainAll(selectedTransitions.keySet());
						// discard transitions with the same timestamp
						for (long thresholdStartTimeStamp_ms : intersection) {
							// check which one of the conflicting transitions is inferior and remove it from its parent map
							final Entry<Long, Transition> transitionFromRecord = transitionsFromRecord.ceilingEntry(thresholdStartTimeStamp_ms);
							getInferiorTransition(selectedTransitions.ceilingEntry(thresholdStartTimeStamp_ms), transitionsFromRecord.ceilingEntry(thresholdStartTimeStamp_ms)).ifPresent(x -> {
								if (transitionFromRecord.equals(x))
									transitionsFromRecord.remove(thresholdStartTimeStamp_ms);
								else
									selectedTransitions.remove(thresholdStartTimeStamp_ms);
							});
						}
						if (!intersection.isEmpty()) {
							if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("%d  removals due to same timestamp:  intersectionSize=%d", transitionType.getTransitionId(), intersection.size())); //$NON-NLS-1$
						}
					}
					// merge the new transitions with existing transitions for the current group and class
					selectedTransitions.putAll(transitionsFromRecord);

					// eliminate transitions which overlap in the reference and threshold phases; prioritize shorter transitions because this will increase the probability for additional transitions
					List<Long> duplicates = new ArrayList<Long>();
					Entry<Long, Transition> previousTransitionEntry = null;
					for (Entry<Long, Transition> transitionEntry : selectedTransitions.entrySet()) {
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
					for (long timeStamp_ms : duplicates) {
						selectedTransitions.remove(timeStamp_ms);
					}
					if (!duplicates.isEmpty()) {
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("%d  removals due to general overlap:  duplicatesSize=%d", transitionType.getTransitionId(), duplicates.size())); //$NON-NLS-1$
					}
				} // for transitionGroupTypes
			} // transitionsFromRecord.isEmpty()
		} // for transitionTypes

	}

	/**
	 * prioritize transitions (peak is top priority, slope is last priority).
	 * the  second-order criterion is the reference + threshold duration (shorter is higher priority).
	 * @param entry1
	 * @param entry2
	 * @return the inferior transition in case of overlapping transitions
	 */
	private Optional<Entry<Long, Transition>> getInferiorTransition(Entry<Long, Transition> entry1, Entry<Long, Transition> entry2) {
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

	/**
	 * apply transitionType definitions to the measurement data.
	 * add all the transitions identified.
	 * measurements with null values are ignored.
	 * @param transitionRecord
	 * @param transitionType
	 * @return all transitions with the key thresholdStartTimestamp_ms
	 */
	private TreeMap<Long, Transition> addFromRecord(Record transitionRecord, TransitionType transitionType) {
		TreeMap<Long, Transition> transitionFromRecord = new TreeMap<Long, Transition>();

		final double translatedMaxValue = this.device.translateValue(transitionRecord, transitionRecord.getMaxValue());
		final double translatedMinValue = this.device.translateValue(transitionRecord, transitionRecord.getMinValue());
		final double absoluteDelta = Settings.getInstance().getAbsoluteTransitionLevel() * (translatedMaxValue - translatedMinValue);
		final double translatedThresholdValue = this.device.translateValue(transitionRecord, transitionType.getThresholdValue());
		final double translatedRecoveryValue = transitionType.getClassType() != TransitionClassTypes.SLOPE ? this.device.translateValue(transitionRecord, transitionType.getRecoveryValue()) : 0;

		final ChannelPropertyType channelProperty = this.device.getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.MINIMUM_TRANSITION_STEPS);
		final int minimumTransitionSteps = channelProperty != null && !channelProperty.getValue().isEmpty() ?  Integer.parseInt(channelProperty.getValue()) : 2;
		final int referenceDequeSize = (int) (transitionType.getReferenceTimeMsec() / this.recordSet.timeStep_ms.getAverageTimeStep_ms());
		final int thresholdDequeSize = (int) (transitionType.getThresholdTimeMsec() / this.recordSet.timeStep_ms.getAverageTimeStep_ms());
		final int recoveryDequeSize = (int) (transitionType.getClassType() != TransitionClassTypes.SLOPE ? transitionType.getRecoveryTimeMsec() / this.recordSet.timeStep_ms.getAverageTimeStep_ms() : 1);
		this.referenceDeque = new SettlementDeque(referenceDequeSize, transitionType.isGreater(), transitionType.getReferenceTimeMsec() * 10);
		this.thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), transitionType.getThresholdTimeMsec() * 10);
		this.recoveryDeque = new SettlementDeque(recoveryDequeSize, true, transitionType.getClassType() != TransitionClassTypes.SLOPE ? transitionType.getRecoveryTimeMsec() * 10 : 1); // initialize an empty deque just for avoiding a null reference
		this.triggerState = TriggerState.WAITING;
		this.referenceDeque.initialize(0);

		int descriptionCutPoint = transitionRecord.parent.description.indexOf("\r"); //$NON-NLS-1$
		if (descriptionCutPoint < 0) descriptionCutPoint = 11;
		log.log(Level.FINEST, transitionType.getTransitionId() + "  " + transitionRecord.parent.name + " " + " " + transitionRecord.parent.description.substring(0, descriptionCutPoint) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ String.format(" %s initialized: referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", transitionRecord.getName(), referenceDequeSize, thresholdDequeSize, recoveryDequeSize)); //$NON-NLS-1$
		if (transitionType.getClassType() == TransitionClassTypes.PEAK) {
			for (int i = 0; i < transitionRecord.size(); i++) {
				if (transitionRecord.realRealGet(i) == null) break;
				long timeStamp_100ns = (long) (transitionRecord.getTime_ms(i) * 10.);
				double translatedValue = this.device.translateValue(transitionRecord, transitionRecord.realRealGet(i));
				switch (this.triggerState) {
				case WAITING:
					this.previousTriggerState = this.triggerState;
					boolean isThresholdLevel = isBeyondThresholdLevel(absoluteDelta, transitionType, translatedValue, translatedThresholdValue);
					// check if the current value is beyond the threshold and if the majority of the reference values did not pass the threshold 
					if (isThresholdLevel && this.referenceDeque.isStuffed() && !isBeyondThresholdLevel(absoluteDelta, transitionType, this.referenceDeque.getSecurityValue(), translatedThresholdValue)) {
						this.triggerState = TriggerState.TRIGGERED;
						this.thresholdDeque.initialize(i);
						this.thresholdDeque.addLast(translatedValue, timeStamp_100ns);
						log.log(Level.FINER, Integer.toString(transitionType.getTransitionId()), this);
					}
					else { // continue waiting for trigger fire
						this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
					}
					break;
				case TRIGGERED:
					this.previousTriggerState = this.triggerState;
					boolean isRecovered = isBeyondRecoveryLevel(transitionType, translatedValue, translatedRecoveryValue);
					// minimumTransitionSteps prevents transitions provoked by jitters in the reference phase; should cover at least 2 'real' measurements
					// also check if only a minimum of the threshold values actually passed the recovery level (in order to tolerate a minimum of jitters)
					if (isRecovered && this.thresholdDeque.size() >= minimumTransitionSteps && !isBeyondRecoveryLevel(transitionType, this.thresholdDeque.getSecurityValue(), translatedRecoveryValue)) {
						this.triggerState = TriggerState.RECOVERING;
						this.recoveryDeque.initialize(i);
						this.recoveryDeque.addLast(translatedValue, timeStamp_100ns);
						log.log(Level.FINER, Integer.toString(transitionType.getTransitionId()), this);
					}
					else { // hysteresis
						boolean isThresholdTimeExceeded = !this.thresholdDeque.tryAddLast(translatedValue, timeStamp_100ns);
						if (isThresholdTimeExceeded) {
							this.triggerState = TriggerState.WAITING;
							log.log(Level.FINER, " isThresholdTimeExceeded ", this); //$NON-NLS-1$
							this.referenceDeque.addLastByMoving(this.thresholdDeque);
							// referenceStartIndex is not modified by jitters
							this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
						}
						else { // threshold phase continues
						}
					}
					break;
				case RECOVERING:
					this.previousTriggerState = this.triggerState;
					boolean isPersistentRecovery = isBeyondRecoveryLevel(transitionType, translatedValue, translatedRecoveryValue);
					boolean isRecoveryFinalized = !this.recoveryDeque.tryAddLast(translatedValue, timeStamp_100ns);
					if (isRecoveryFinalized) {
						// final check if the majority of the reference and recovery values did not pass the threshold and if only a minimum of the threshold values actually passed the recovery level
						if (!isBeyondThresholdLevel(absoluteDelta, transitionType, this.referenceDeque.getSecurityValue(), translatedThresholdValue) //
								&& !isBeyondRecoveryLevel(transitionType, this.thresholdDeque.getSecurityValue(), translatedRecoveryValue) //
								&& !isBeyondThresholdLevel(absoluteDelta, transitionType, this.recoveryDeque.getSecurityValue(), translatedThresholdValue)) {
							this.triggerState = TriggerState.WAITING;
							log.log(Level.FINER, Integer.toString(transitionType.getTransitionId()), this);
							Transition transition = new Transition(this.referenceDeque.startIndex, this.referenceDeque.size(), this.thresholdDeque.startIndex, this.thresholdDeque.size(),
									this.recoveryDeque.startIndex, this.recoveryDeque.size(), transitionRecord, transitionType);
							transitionFromRecord.put(transition.getThresholdStartTimeStamp_ms(), transition);
							log.log(Level.FINE, GDE.STRING_GREATER, transition);
							this.thresholdDeque.clear();
							this.referenceDeque.initialize(this.recoveryDeque.startIndex);
							this.referenceDeque.addLastByMoving(this.recoveryDeque);
							this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
						}
						else {
							if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, String.format("%d trigger security check provoked a fallback %s: translatedValue=%f  thresholdAverage=%f", //$NON-NLS-1$
									transitionType.getTransitionId(), this.thresholdDeque.getFormatedDuration(this.thresholdDeque.size() - 1), translatedValue, this.thresholdDeque.getAverageValue()));
							this.triggerState = TriggerState.WAITING;
							log.log(Level.FINER, Integer.toString(transitionType.getTransitionId()), this);
							this.referenceDeque.addLastByMoving(this.thresholdDeque);
							this.referenceDeque.addLastByMoving(this.recoveryDeque);
							// referenceStartIndex is not modified by jitters
						}
					}
					else if (!isRecoveryFinalized && isPersistentRecovery) { // go on with the recovery
					}
					else if (!isRecoveryFinalized && !isPersistentRecovery) {
						log.log(Level.FINER, " recovery level not stable ", this); //$NON-NLS-1$
						// try to extend the threshold time
						int removedCount = this.thresholdDeque.tryAddLastByMoving(this.recoveryDeque);
						if (removedCount > 0 && this.recoveryDeque.isEmpty()) {
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%d recovery jitters provoked a fallback %s: translatedValue=%f  thresholdAverage=%f", //$NON-NLS-1$
									transitionType.getTransitionId(), this.thresholdDeque.getFormatedDuration(this.thresholdDeque.size() - 1), translatedValue, this.thresholdDeque.getAverageValue()));
							// all recovery entries including the current entry are now in the threshold phase
							this.triggerState = TriggerState.TRIGGERED;
							// thresholdStartIndex is not modified by jitters;
						}
						else {
							// now the threshold time is exceeded and the current value does not fit in the recovery phase 
							this.triggerState = TriggerState.WAITING;
							this.referenceDeque.addLastByMoving(this.thresholdDeque);
							this.referenceDeque.addLastByMoving(this.recoveryDeque);
							// referenceStartIndex is not modified by jitters
						}
					}
					break;
				} // end switch
			} // end for
			// take the current transition even if the recovery time is not exhausted
			if (this.triggerState == TriggerState.RECOVERING) {
				// final check if the majority of the reference and recovery values did not pass the threshold and if only a minimum of the threshold values actually passed the recovery level
				if (!isBeyondThresholdLevel(absoluteDelta, transitionType, this.referenceDeque.getSecurityValue(), translatedThresholdValue) //
						&& !isBeyondRecoveryLevel(transitionType, this.thresholdDeque.getSecurityValue(), translatedRecoveryValue) //
						&& !isBeyondThresholdLevel(absoluteDelta, transitionType, this.recoveryDeque.getSecurityValue(), translatedThresholdValue)) {
					Transition transition = new Transition(this.referenceDeque.startIndex, this.referenceDeque.size(), this.thresholdDeque.startIndex, this.thresholdDeque.size(), this.recoveryDeque.startIndex,
							this.recoveryDeque.size(), transitionRecord, transitionType);
					transitionFromRecord.put(transition.getThresholdStartTimeStamp_ms(), transition);
					log.log(Level.FINE, GDE.STRING_GREATER, transition);
				}
			}
		} // end if isPeak

		else if (transitionType.getClassType() == TransitionClassTypes.PULSE) {
			for (int i = 0; i < transitionRecord.size(); i++) {
				if (transitionRecord.realRealGet(i) == null) break;
				long timeStamp_100ns = (long) (transitionRecord.getTime_ms(i) * 10.);
				double translatedValue = this.device.translateValue(transitionRecord, transitionRecord.realRealGet(i));
				switch (this.triggerState) {
				case WAITING:
					this.previousTriggerState = this.triggerState;
					boolean isThresholdLevel = isBeyondThresholdLevel(absoluteDelta, transitionType, translatedValue, translatedThresholdValue);
					// check if the current value is beyond the threshold and if the majority of the reference values did not pass the threshold 
					if (isThresholdLevel && this.referenceDeque.isStuffed() && !isBeyondThresholdLevel(absoluteDelta, transitionType, this.referenceDeque.getSecurityValue(), translatedThresholdValue)) {
						this.triggerState = TriggerState.TRIGGERED;
						this.thresholdDeque.initialize(i);
						this.thresholdDeque.addLast(translatedValue, timeStamp_100ns);
						log.log(Level.FINER, Integer.toString(transitionType.getTransitionId()), this);
					}
					else {
						this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
					}
					break;
				case TRIGGERED:
					this.previousTriggerState = this.triggerState;
					boolean isPersistentTrigger = isBeyondThresholdLevel(absoluteDelta, transitionType, translatedValue, translatedThresholdValue);
					boolean isInThresholdTime = this.thresholdDeque.tryAddLast(translatedValue, timeStamp_100ns);
					boolean isRecovered = isBeyondRecoveryLevel(transitionType, translatedValue, translatedRecoveryValue);
					if (!isInThresholdTime && isRecovered) {
						// check if only a minimum of the threshold values actually passed the recovery level (in order to tolerate a minimum of jitters)
						if (!isBeyondRecoveryLevel(transitionType, this.thresholdDeque.getSecurityValue(), translatedRecoveryValue)) {
							// threshold must conform perfectly to the threshold requirements which are level and MINIMUM time
							this.triggerState = TriggerState.RECOVERING;
							this.recoveryDeque.initialize(i);
							this.recoveryDeque.addLast(translatedValue, timeStamp_100ns);
							log.log(Level.FINER, Integer.toString(transitionType.getTransitionId()), this);
						}
						else {
							this.triggerState = TriggerState.WAITING;
							log.log(Level.FINER, " threshold SecurityValue ", this); //$NON-NLS-1$
							this.thresholdDeque.clear();
							this.referenceDeque.initialize(i);
							this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
						}
					}
					else if (!isInThresholdTime && isPersistentTrigger) {
						this.thresholdDeque.addLast(translatedValue, timeStamp_100ns);
					}
					else if (!isInThresholdTime && !isPersistentTrigger) { // no hysteresis 
						this.triggerState = TriggerState.WAITING;
						log.log(Level.FINER, " !isInThresholdTime && !isPersistentTrigger ", this); //$NON-NLS-1$
						this.thresholdDeque.clear();
						this.referenceDeque.initialize(i);
						this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
					}
					else if (isInThresholdTime && !isPersistentTrigger) { // no hysteresis 
						this.triggerState = TriggerState.WAITING;
						log.log(Level.FINER, " !isPersistentTrigger ", this); //$NON-NLS-1$
						this.thresholdDeque.clear();
						this.referenceDeque.initialize(i);
						this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
					}
					else {
						// currently within thresholdTime --- entry has been added to deque already
					}
					break;
				case RECOVERING:
					this.previousTriggerState = this.triggerState;
					boolean isPersistentRecovery = isBeyondRecoveryLevel(transitionType, translatedValue, translatedRecoveryValue);
					boolean isRecoveryFinalized = !this.recoveryDeque.tryAddLast(translatedValue, timeStamp_100ns);
					if (isRecoveryFinalized) {
						// final check if the majority of the reference and recovery values did not pass the threshold and if only a minimum of the threshold values actually passed the recovery level
						if (!isBeyondThresholdLevel(absoluteDelta, transitionType, this.referenceDeque.getSecurityValue(), translatedThresholdValue) //
								&& !isBeyondRecoveryLevel(transitionType, this.thresholdDeque.getSecurityValue(), translatedRecoveryValue) //
								&& !isBeyondThresholdLevel(absoluteDelta, transitionType, this.recoveryDeque.getSecurityValue(), translatedThresholdValue)) {
							this.triggerState = TriggerState.WAITING;
							log.log(Level.FINER, Integer.toString(transitionType.getTransitionId()), this);
							Transition transition = new Transition(this.referenceDeque.startIndex, this.referenceDeque.size(), this.thresholdDeque.startIndex, this.thresholdDeque.size(),
									this.recoveryDeque.startIndex, this.recoveryDeque.size(), transitionRecord, transitionType);
							transitionFromRecord.put(transition.getThresholdStartTimeStamp_ms(), transition);
							log.log(Level.FINE, GDE.STRING_GREATER, transition);
							this.thresholdDeque.clear();
							this.referenceDeque.initialize(this.recoveryDeque.getStartIndex());
							this.referenceDeque.addLastByMoving(this.recoveryDeque);
							this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
						}
						else {
							if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, String.format("%d trigger security check provoked a fallback %s: translatedValue=%f  thresholdAverage=%f", //$NON-NLS-1$
									transitionType.getTransitionId(), this.thresholdDeque.getFormatedDuration(this.thresholdDeque.size() - 1), translatedValue, this.thresholdDeque.getAverageValue()));
							this.triggerState = TriggerState.WAITING;
							log.log(Level.FINER, Integer.toString(transitionType.getTransitionId()), this);
							this.thresholdDeque.clear();
							this.referenceDeque.initialize(this.recoveryDeque.getStartIndex());
							this.referenceDeque.addLastByMoving(this.recoveryDeque);
							this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
						}
					}
					else if (!isRecoveryFinalized && !isPersistentRecovery) {
						this.triggerState = TriggerState.WAITING;
						log.log(Level.FINER, " !isPersistentRecovery " + transitionType.getTransitionId(), this); //$NON-NLS-1$
						this.thresholdDeque.clear();
						this.referenceDeque.initialize(this.recoveryDeque.getStartIndex());
						this.referenceDeque.addLastByMoving(this.recoveryDeque);
						this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
					}
					else {
						// go on recovering
					}
					break;
				} // end switch
			} // end for
			// take the current transition even if the recovery time is not exhausted
			if (this.triggerState == TriggerState.RECOVERING) {
				// final check if the majority of the reference and recovery values did not pass the threshold and if only a minimum of the threshold values actually passed the recovery level
				if (!isBeyondThresholdLevel(absoluteDelta, transitionType, this.referenceDeque.getSecurityValue(), translatedThresholdValue) //
						&& !isBeyondRecoveryLevel(transitionType, this.thresholdDeque.getSecurityValue(), translatedRecoveryValue) //
						&& !isBeyondThresholdLevel(absoluteDelta, transitionType, this.recoveryDeque.getSecurityValue(), translatedThresholdValue)) {
					Transition transition = new Transition(this.referenceDeque.startIndex, this.referenceDeque.size(), this.thresholdDeque.startIndex, this.thresholdDeque.size(), this.recoveryDeque.startIndex,
							this.recoveryDeque.size(), transitionRecord, transitionType);
					transitionFromRecord.put(transition.getThresholdStartTimeStamp_ms(), transition);
					log.log(Level.FINE, GDE.STRING_GREATER, transition);
				}
			}
			else if (this.triggerState == TriggerState.TRIGGERED) {
				log.log(Level.FINER,
						String.format("ends %s %s  referenceExtreme=%f ", this.triggerState, this.thresholdDeque.getFormatedDuration(this.thresholdDeque.size() - 1), this.referenceDeque.extremeValue) //$NON-NLS-1$
								+ this.thresholdDeque.getExtremeValue() + this.thresholdDeque.getTranslatedValues());
			}
		} // end isPulse

		else if (transitionType.getClassType() == TransitionClassTypes.SLOPE) {
			for (int i = 0; i < transitionRecord.size(); i++) {
				if (transitionRecord.realRealGet(i) == null) break;
				long timeStamp_100ns = (long) (transitionRecord.getTime_ms(i) * 10.);
				double translatedValue = this.device.translateValue(transitionRecord, transitionRecord.realRealGet(i));
				switch (this.triggerState) {
				case WAITING:
					this.previousTriggerState = this.triggerState;
					boolean isThresholdLevel = isBeyondThresholdLevel(absoluteDelta, transitionType, translatedValue, translatedThresholdValue);
					// check if the current value is beyond the threshold and if the majority of the reference values did not pass the threshold 
					if (isThresholdLevel && this.referenceDeque.isStuffed() && !isBeyondThresholdLevel(absoluteDelta, transitionType, this.referenceDeque.getSecurityValue(), translatedThresholdValue)) {
						this.triggerState = TriggerState.TRIGGERED;
						this.thresholdDeque.initialize(i);
						this.thresholdDeque.addLast(translatedValue, timeStamp_100ns);
						log.log(Level.FINER, Integer.toString(transitionType.getTransitionId()), this);
					}
					else {
						this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
					}
					break;
				case TRIGGERED:
					this.previousTriggerState = this.triggerState;
					boolean isPersistentTrigger = isBeyondThresholdLevel(absoluteDelta, transitionType, translatedValue, translatedThresholdValue);
					boolean isInThresholdTime = this.thresholdDeque.tryAddLast(translatedValue, timeStamp_100ns);
					if (!isInThresholdTime) {
						// final check if the majority of the reference values did not pass the threshold and if the majority of the threshold values passed the threshold 
						if (!isBeyondThresholdLevel(absoluteDelta, transitionType, this.referenceDeque.getSecurityValue(), translatedThresholdValue)) {
							this.triggerState = TriggerState.WAITING;
							log.log(Level.FINE, Integer.toString(transitionType.getTransitionId()), this);
							Transition transition = new Transition(this.referenceDeque.startIndex, this.referenceDeque.size(), this.thresholdDeque.startIndex, this.thresholdDeque.size(),
									this.recoveryDeque.startIndex, this.recoveryDeque.size(), transitionRecord, transitionType);
							transitionFromRecord.put(transition.getThresholdStartTimeStamp_ms(), transition);
							log.log(Level.FINE, GDE.STRING_GREATER, transition);
							this.thresholdDeque.clear();
							this.referenceDeque.initialize(i);
							this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
							log.log(Level.FINE, Integer.toString(transitionType.getTransitionId()), this);
						}
						else {
							if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, String.format("%d trigger security check provoked a fallback %s: translatedValue=%f  thresholdAverage=%f", //$NON-NLS-1$
									transitionType.getTransitionId(), this.thresholdDeque.getFormatedDuration(this.thresholdDeque.size() - 1), translatedValue, this.thresholdDeque.getAverageValue()));
							this.triggerState = TriggerState.WAITING;
							log.log(Level.FINER, Integer.toString(transitionType.getTransitionId()), this);
							this.referenceDeque.initialize(i - this.thresholdDeque.size());
							this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
							this.thresholdDeque.clear();
						}
					}
					else if (isInThresholdTime && !isPersistentTrigger) { // threshold must conform perfectly to the threshold requirements which are level and time
						this.triggerState = TriggerState.WAITING;
						log.log(Level.FINE, " !isPersistentTrigger " + transitionType.getTransitionId(), this); //$NON-NLS-1$
						this.referenceDeque.initialize(i - this.thresholdDeque.size());
						this.referenceDeque.addLastByMoving(this.thresholdDeque);
					}
					else if (isInThresholdTime && isPersistentTrigger) {
						// go on and try to confirm the trigger
					}
					break;
				case RECOVERING: // is not relevant for slope
					break;
				} // end switch
			} // end for
			// do not take the last transition (differs from peak / pulse) because there is no recovery time
		} // end isSlope
		else {
			throw new UnsupportedOperationException();
		}

		return transitionFromRecord;
	}

	@Override
	public synchronized String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s > %s ", this.previousTriggerState, this.triggerState)); //$NON-NLS-1$
		sb.append(this.previousTriggerState == TriggerState.WAITING && !this.thresholdDeque.isEmpty() ? this.referenceDeque.getFormatedDuration(this.referenceDeque.size() - 1)
				: this.previousTriggerState == TriggerState.TRIGGERED && !this.thresholdDeque.isEmpty() ? this.thresholdDeque.getFormatedDuration(this.thresholdDeque.size() - 1)
						: this.previousTriggerState == TriggerState.RECOVERING && !this.recoveryDeque.isEmpty() ? this.recoveryDeque.getFormatedDuration(this.recoveryDeque.size() - 1) : "null"); //$NON-NLS-1$
		sb.append(String.format(": referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", this.referenceDeque.size(), this.thresholdDeque.size(), this.recoveryDeque.size())) //$NON-NLS-1$
				.append(GDE.STRING_NEW_LINE);
		sb.append(String.format("%38s reference bench=%.1f extreme=", "", this.referenceDeque.getBenchmarkValue()) + this.referenceDeque.getExtremeValue() + this.referenceDeque.getTranslatedValues()); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(GDE.STRING_BLANK_AT_BLANK + this.referenceDeque.getTimestamps_100ns()).append(GDE.STRING_NEW_LINE);
		sb.append(String.format("%38s threshold bench=%.1f extreme=", "", this.thresholdDeque.getBenchmarkValue()) + this.thresholdDeque.getExtremeValue() + this.thresholdDeque.getTranslatedValues()); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(GDE.STRING_BLANK_AT_BLANK + this.thresholdDeque.getTimestamps_100ns());
		if (!this.recoveryDeque.isEmpty()) {
			sb.append(GDE.STRING_NEW_LINE);
			sb.append(String.format("%38s recovery  bench=%.1f extreme=", "", this.recoveryDeque.getBenchmarkValue()) + this.recoveryDeque.getExtremeValue() + this.recoveryDeque.getTranslatedValues()); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(GDE.STRING_BLANK_AT_BLANK + this.recoveryDeque.getTimestamps_100ns());
		}
		return sb.toString();
	}

	/**
	 * @param absoluteDelta ensures for DELTA_FACTOR transitions a minimum delta between the translated value and the base value derived from the reference period
	 * @param transitionType
	 * @param translatedValue
	 * @return true if level exceeded (based on isGreater property)
	 */
	private boolean isBeyondThresholdLevel(double absoluteDelta, TransitionType transitionType, double translatedValue, double translatedThresholdValue) {
		final boolean isBeyond;
		if (this.referenceDeque.isEmpty()) {
			isBeyond = false;
		}
		else if (transitionType.getValueType() == TransitionValueTypes.DELTA_FACTOR) {
			final double baseValue = this.referenceDeque.getBenchmarkValue();
			if (transitionType.isGreater())
				isBeyond = translatedValue > baseValue + absoluteDelta && translatedValue > baseValue + transitionType.getThresholdValue() * Math.abs(baseValue);
			else
				isBeyond = translatedValue < baseValue - absoluteDelta && translatedValue < baseValue + transitionType.getThresholdValue() * Math.abs(baseValue);
		}
		else if (transitionType.getValueType() == TransitionValueTypes.DELTA_VALUE) {
			final double baseValue = this.referenceDeque.getBenchmarkValue();
			if (transitionType.isGreater())
				isBeyond = translatedValue > baseValue + translatedThresholdValue;
			else
				isBeyond = translatedValue < baseValue + translatedThresholdValue;
		}
		else if (transitionType.getValueType() == TransitionValueTypes.UPPER_THRESHOLD)
			isBeyond = translatedValue > translatedThresholdValue;
		else if (transitionType.getValueType() == TransitionValueTypes.LOWER_THRESHOLD)
			isBeyond = translatedValue < translatedThresholdValue;
		else
			throw new UnsupportedOperationException();
		return isBeyond;
	}

	private boolean isBeyondRecoveryLevel(TransitionType transitionType, double translatedValue, double translatedRecoveryValue) {
		final boolean isBeyond;
		if (transitionType.getValueType() == TransitionValueTypes.DELTA_FACTOR) {
			final double baseValue = this.referenceDeque.getBenchmarkValue();
			isBeyond = transitionType.isGreater() ? translatedValue < baseValue + transitionType.getRecoveryValue() * baseValue : translatedValue > baseValue + transitionType.getRecoveryValue() * baseValue;
		}
		else if (transitionType.getValueType() == TransitionValueTypes.DELTA_VALUE) {
			final double baseValue = this.referenceDeque.getBenchmarkValue();
			isBeyond = transitionType.isGreater() ? translatedValue < baseValue + translatedRecoveryValue : translatedValue > baseValue + translatedRecoveryValue;
		}
		else {
			isBeyond = transitionType.isGreater() ? translatedValue < translatedRecoveryValue : translatedValue > translatedRecoveryValue;
		}
		return isBeyond;
	}

	public TreeMap<Long, Transition> getTransitions(int transitionGroupId) {
		if (this.transitionContainer.containsKey(transitionGroupId)) {
			return this.transitionContainer.get(transitionGroupId);
		}
		else {
			return new TreeMap<Long, Transition>();
		}
	}

	public Map<Integer, TreeMap<Long, Transition>> getTransitionContainer() {
		return this.transitionContainer;
	}

	/**
	 * @return the transitionCount
	 */
	public int getTransitionCount() {
		return this.transitionCount;
	}

}
