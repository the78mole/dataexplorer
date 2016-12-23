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
 
 Copyright (c) 2016 Thomas Eickert
****************************************************************************************/
package gde.data;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.device.ChannelPropertyTypes;
import gde.device.IDevice;
import gde.device.TransitionClassTypes;
import gde.device.TransitionType;
import gde.device.TransitionValueTypes;
import gde.log.Level;
import gde.utils.Quantile;
import gde.utils.Quantile.Fixings;

/**
 * holds all transitions for the history recordSet.
 * the transitions are identified by applying the transition type to the histo recordSet.
 * sorted by timestamp.
 * @author Thomas Eickert
 */
public class HistoTransitions extends Vector<Transition> {
	final static String				$CLASS_NAME				= HistoTransitions.class.getName();
	private static final long	serialVersionUID	= -3373317163399403911L;
	final static Logger				log								= Logger.getLogger($CLASS_NAME);

	private final IDevice			device;

	private final RecordSet		parent;
	private SettlementDeque		referenceDeque;
	private SettlementDeque		thresholdDeque;
	private SettlementDeque		recoveryDeque;
	private TriggerState			previousTriggerState;
	private TriggerState			triggerState;

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

		private static final long				serialVersionUID	= 915484098600135376L;
		private final SimpleDateFormat	timeFormat				= new SimpleDateFormat("HH:mm:ss.SSS");

		private boolean									isMinimumExtremum;
		private long										timePeriod_100ns;
		private double									extremeValue;
		private boolean									isExhausted;
		private int											startIndex;

		private ArrayDeque<Long>				timeStampDeque;
		private List<Double>						sortedValues;

		/**
		 * be sure to call initialize before using the deque.
		 * @param numElements
		 * @param isMinimumExtremum true if the extreme value shall determine the minimum of all values since initialize / clear 
		 * @param timePeriod_100ns
		 * @param startIndex is the source record index at the initialize call
		 */
		SettlementDeque(int numElements, boolean isMinimumExtremum, long timePeriod_100ns) {
			super(numElements);
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
			this.startIndex = -1;
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
		public SettlementDeque clone() {
			SettlementDeque cloned = (SettlementDeque) super.clone();
			cloned.timeStampDeque = this.timeStampDeque.clone();
			cloned.isMinimumExtremum = this.isMinimumExtremum;
			cloned.timePeriod_100ns = this.timePeriod_100ns;
			cloned.extremeValue = this.extremeValue;
			cloned.isExhausted = this.isExhausted;
			cloned.sortedValues = null;
			return cloned;
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
			if (this.timeStampDeque.peekFirst() + this.timePeriod_100ns > timeStamp_100ns) {
				this.addLast(translatedValue, timeStamp_100ns);
				return true;
			}
			else {
				this.isExhausted = true;
				return false;
			}
		}

		/**
		 * moves all deque entries and leaves an empty deque.
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
						this.pollFirst();
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
			if (probabilityCutPoint >= 1. / (realSize + 1) && probabilityCutPoint < (double) realSize / (realSize + 1)) {
				double position = (realSize + 1) * probabilityCutPoint;
				return this.getSortedValues().get((int) position - 1) + (position - (int) position) * (this.getSortedValues().get((int) position) - this.getSortedValues().get((int) position - 1));
			}
			else if (probabilityCutPoint < 1. / (realSize + 1))
				return this.getSortedValues().get(0);
			else
				return this.getSortedValues().get(realSize - 1);
		}

		/**
		 * please note: requires a sort over the full deque size.
		 * @return NaN for empty deque or a leveled extremum value for comparisons, i.e. a more stable value than the extremum value  
		 */
		public double getBenchmarkValue() {
			if (this.isEmpty()) {
				return Double.NaN;
			}
			else {
				if (Settings.getInstance().getReferenceQuantileDistance() == 0.) {
					// bypass deque sort
					return this.extremeValue;
				}
				else {
					return this.isMinimumExtremum ? this.getQuantileValue(Settings.getInstance().getReferenceQuantileDistance())
							: this.getQuantileValue(1. - Settings.getInstance().getReferenceQuantileDistance());
				}
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
				return this.isMinimumExtremum ? this.getQuantileValue(1. - Settings.getInstance().getReferenceQuantileDistance())
						: this.getQuantileValue(Settings.getInstance().getReferenceQuantileDistance());
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
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, String.format("averageValue=%f", averageValue));
			return averageValue;
		}

		private void setExtremeValue(double removedValue) {
			if (this.isEmpty()) {
				this.extremeValue = this.isMinimumExtremum ? Double.MAX_VALUE : -Double.MAX_VALUE;
			}
			else if (removedValue == this.extremeValue) {
				// avoid the calculation of a new extreme value if the neighbor value is identical
				if (removedValue != this.peekFirst()) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, String.format("removedValue=%f extremeValue=%f", removedValue, this.extremeValue));
					this.extremeValue = this.isMinimumExtremum ? getQuantileValue(0.) : getQuantileValue(1.);
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, String.format("extremeValue=%f", this.extremeValue));
				}
			}
		}

		public List<Double> getTranslatedValues() {
			return Arrays.asList(this.toArray(new Double[0]));
		}

		public List<Long> getTimestamps_100ns() {
			return Arrays.asList(this.timeStampDeque.toArray(new Long[0]));
		}

		public String getFormattedTime(int index) {
			return this.timeFormat.format(this.getTimestamps_100ns().get(index) / 10);

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

	public HistoTransitions(IDevice newDevice, RecordSet parent, int transitionTypeSize) {
		super(transitionTypeSize * 3); // let us assume we will identify 3 transitions per transitionType
		this.device = newDevice;
		this.parent = parent;
	}

	/**
	 * add a transition.
	 * @param point
	 */
	@Override
	public synchronized boolean add(Transition transition) {
		return super.add(transition);

	}

	/**
	 * apply transitionType definitions to the measurement data.
	 * add all the transitions identified.
	 * measurements with null values are ignored.
	 * @param transitionRecord
	 * @param transitionType
	 * @return false if no valid transition was identified
	 */
	public synchronized boolean addFromRecord(Record transitionRecord, TransitionType transitionType) {
		int mySize = this.size();
		final double lowerCutPoint = (1. - Settings.getInstance().getTransitionQuantileRange()) / 2.;
		final Quantile recordQuantile = new Quantile(transitionRecord, EnumSet.of(Fixings.IS_SAMPLE));
		final double absoluteDeltaOBS = (this.device.translateValue(transitionRecord, recordQuantile.getQuantile(1. - lowerCutPoint)) - this.device.translateValue(transitionRecord, recordQuantile.getQuantile(lowerCutPoint)));
		final double absoluteDelta = (this.device.translateValue(transitionRecord, transitionRecord.getMaxValue()) - this.device.translateValue(transitionRecord, transitionRecord.getMinValue()))
				* Settings.getInstance().getAbsoluteTransitionLevel();
//		log.log(Level.OFF, String.format("new/old=%f/%f  upper=%f/%d  lower=%f/%d", absoluteDelta, absoluteDeltaOBS, recordQuantile.getQuantile(1. - lowerCutPoint), transitionRecord.getMaxValue(), recordQuantile.getQuantile(lowerCutPoint),transitionRecord.getMinValue() ));

		final int minimumTransitionSteps = Integer.parseInt(this.device.getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.MINIMUM_TRANSITION_STEPS).getValue());
		final int referenceDequeSize = (int) (transitionType.getReferenceTimeMsec() / this.parent.timeStep_ms.getAverageTimeStep_ms()) + 11; // plus 11 elements ensures sufficient elements for most cases
		final int thresholdDequeSize = (int) (transitionType.getThresholdTimeMsec() / this.parent.timeStep_ms.getAverageTimeStep_ms()) + 11; // plus 11 elements ensures sufficient elements for most cases
		final int recoveryDequeSize = (int) (transitionType.getRecoveryTimeMsec() != null ? transitionType.getRecoveryTimeMsec() / this.parent.timeStep_ms.getAverageTimeStep_ms() + 11 : 1); // plus 11 elements ensures sufficient elements for most cases
		this.referenceDeque = new SettlementDeque(referenceDequeSize, transitionType.isGreater(), transitionType.getReferenceTimeMsec() * 10);
		this.thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), transitionType.getThresholdTimeMsec() * 10);
		this.recoveryDeque = new SettlementDeque(recoveryDequeSize, true,  transitionType.getRecoveryTimeMsec() != null ?  transitionType.getRecoveryTimeMsec() * 10 : 1); // initialize an empty deque just for avoiding a null reference
		this.triggerState = TriggerState.WAITING;
		this.referenceDeque.initialize(0);

		int descriptionCutPoint = transitionRecord.parent.description.indexOf("\r");
		if (descriptionCutPoint < 0) descriptionCutPoint = 11;
		log.log(Level.OFF, transitionType.getTransitionId() + "  " + transitionRecord.parent.name + " " + " " + transitionRecord.parent.description.substring(0, descriptionCutPoint)
				+ String.format(" %s initialized: referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", transitionRecord.getName(), referenceDequeSize, thresholdDequeSize, recoveryDequeSize)); //$NON-NLS-1$
		if (transitionType.getClassType() == TransitionClassTypes.PEAK) {
			for (int i = 0; i < transitionRecord.size(); i++) {
				if (transitionRecord.realRealGet(i) == null) break;
				long timeStamp_100ns = (long) (transitionRecord.getTime_ms(i) * 10.);
				double translatedValue = this.device.translateValue(transitionRecord, transitionRecord.realRealGet(i));
				switch (this.triggerState) {
				case WAITING:
					this.previousTriggerState = this.triggerState;
					boolean isThresholdLevel = isBeyondThresholdLevel(absoluteDelta, transitionType, translatedValue);
					// check if the current value is beyond the threshold and if the majority of the reference values did not pass the threshold 
					if (isThresholdLevel && this.referenceDeque.isStuffed() && !isBeyondThresholdLevel(absoluteDelta, transitionType, this.referenceDeque.getSecurityValue())) {
						this.triggerState = TriggerState.TRIGGERED;
						this.thresholdDeque.initialize(i);
						this.thresholdDeque.addLast(translatedValue, timeStamp_100ns);
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.toString());
					}
					else { // continue waiting for trigger fire
						this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
					}
					break;
				case TRIGGERED:
					this.previousTriggerState = this.triggerState;
					boolean isRecovered = isBeyondRecoveryLevel(transitionType, translatedValue);
					// minimumTransitionSteps prevents transitions provoked by jitters in the reference phase; should cover at least 2 'real' measurements
					// also check if only a minimum of the threshold values actually passed the recovery level (in order to tolerate a minimum of jitters)
					if (isRecovered && this.thresholdDeque.size() >= minimumTransitionSteps && !isBeyondRecoveryLevel(transitionType, this.thresholdDeque.getSecurityValue())) {
						this.triggerState = TriggerState.RECOVERING;
						this.recoveryDeque.initialize(i);
						this.recoveryDeque.addLast(translatedValue, timeStamp_100ns);
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.toString());
					}
					else { // hysteresis
						boolean isThresholdTimeExceeded = !this.thresholdDeque.tryAddLast(translatedValue, timeStamp_100ns);
						if (isThresholdTimeExceeded) { // never reached the recovery level
							this.triggerState = TriggerState.WAITING;
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.toString());
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
					boolean isPersistentRecovery = isBeyondRecoveryLevel(transitionType, translatedValue);
					boolean isRecoveryFinalized = !this.recoveryDeque.tryAddLast(translatedValue, timeStamp_100ns);
					if (isRecoveryFinalized) {
						// final check if the majority of the reference and recovery values did not pass the threshold and if only a minimum of the threshold values actually passed the recovery level
						if (!isBeyondThresholdLevel(absoluteDelta, transitionType, this.referenceDeque.getSecurityValue()) //
								&& !isBeyondRecoveryLevel(transitionType, this.thresholdDeque.getSecurityValue()) //
								&& !isBeyondThresholdLevel(absoluteDelta, transitionType, this.recoveryDeque.getSecurityValue())) {
							this.triggerState = TriggerState.WAITING;
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.toString());
							Transition transition = new Transition(this.referenceDeque.startIndex, this.referenceDeque.size(), this.thresholdDeque.startIndex, this.thresholdDeque.size(),
									this.recoveryDeque.startIndex, this.recoveryDeque.size(), transitionRecord, transitionType);
							transition.setReferenceExtremumValue(this.referenceDeque.getExtremeValue());
							transition.setThresholdExtremumValue(this.thresholdDeque.getExtremeValue());
							transition.setRecoveryExtremumValue(this.recoveryDeque.getExtremeValue());
							this.add(transition);
							log.log(Level.OFF, transition.toString());
							this.referenceDeque = this.recoveryDeque.clone();
							this.referenceDeque.initialize(this.recoveryDeque.getStartIndex());
							this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
							this.thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), transitionType.getThresholdTimeMsec() * 10);
							this.recoveryDeque = new SettlementDeque(recoveryDequeSize, transitionType.isGreater(), transitionType.getRecoveryTimeMsec() * 10);
						}
						else {
							if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, String.format("trigger security check provoked a fallback %s: translatedValue=%f  thresholdAverage=%f", //$NON-NLS-1$
									this.thresholdDeque.getFormattedTime(this.thresholdDeque.size() - 1), translatedValue, this.thresholdDeque.getAverageValue()));
							this.triggerState = TriggerState.WAITING;
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.toString());
							this.referenceDeque.addLastByMoving(this.thresholdDeque);
							this.referenceDeque.addLastByMoving(this.recoveryDeque);
							// referenceStartIndex is not modified by jitters
						}
					}
					else if (!isRecoveryFinalized && isPersistentRecovery) { // go on with the recovery
					}
					else if (!isRecoveryFinalized && !isPersistentRecovery) { // recovery level not stable
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.toString());
						// try to extend the threshold time
						int removedCount = this.thresholdDeque.tryAddLastByMoving(this.recoveryDeque);
						if (removedCount > 0 && this.recoveryDeque.isEmpty()) {
							if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, String.format("recovery jitters provoked a fallback %s: translatedValue=%f  thresholdAverage=%f", //$NON-NLS-1$
									this.thresholdDeque.getFormattedTime(this.thresholdDeque.size() - 1), translatedValue, this.thresholdDeque.getAverageValue()));
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
				if (!isBeyondThresholdLevel(absoluteDelta, transitionType, this.referenceDeque.getSecurityValue()) //
						&& !isBeyondRecoveryLevel(transitionType, this.thresholdDeque.getSecurityValue()) //
						&& !isBeyondThresholdLevel(absoluteDelta, transitionType, this.recoveryDeque.getSecurityValue())) {
					Transition transition = new Transition(this.referenceDeque.startIndex, this.referenceDeque.size(), this.thresholdDeque.startIndex, this.thresholdDeque.size(), this.recoveryDeque.startIndex,
							this.recoveryDeque.size(), transitionRecord, transitionType);
					transition.setReferenceExtremumValue(this.referenceDeque.extremeValue);
					transition.setThresholdExtremumValue(this.thresholdDeque.extremeValue);
					transition.setRecoveryExtremumValue(this.recoveryDeque.extremeValue);
					this.add(transition);
					log.log(Level.OFF, transition.toString());
				}
			}
		} // end if isPeak
		else if (transitionType.getClassType() == TransitionClassTypes.PULSE) { // todo not tested
			for (int i = 0; i < transitionRecord.size(); i++) {
				if (transitionRecord.realRealGet(i) == null) break;
				long timeStamp_100ns = (long) (transitionRecord.getTime_ms(i) * 10.);
				double translatedValue = this.device.translateValue(transitionRecord, transitionRecord.realRealGet(i));
				switch (this.triggerState) {
				case WAITING:
					this.previousTriggerState = this.triggerState;
					boolean isThresholdLevel = isBeyondThresholdLevel(absoluteDelta, transitionType, translatedValue);
					// check if the current value is beyond the threshold and if the majority of the reference values did not pass the threshold 
					if (isThresholdLevel && this.referenceDeque.isStuffed() && !isBeyondThresholdLevel(absoluteDelta, transitionType, this.referenceDeque.getSecurityValue())) {
						this.triggerState = TriggerState.TRIGGERED;
						this.thresholdDeque.initialize(i);
						this.thresholdDeque.addLast(translatedValue, timeStamp_100ns);
						if (log.isLoggable(Level.OFF)) log.log(Level.OFF, this.toString());
					}
					else {
						this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
					}
					break;
				case TRIGGERED:
					this.previousTriggerState = this.triggerState;
					boolean isPersistentTrigger = isBeyondThresholdLevel(absoluteDelta, transitionType, translatedValue);
					boolean isInThresholdTime = this.thresholdDeque.tryAddLast(translatedValue, timeStamp_100ns);
					boolean isRecovered = isBeyondRecoveryLevel(transitionType, translatedValue);
					if (!isInThresholdTime && isRecovered) {
						// check if only a minimum of the threshold values actually passed the recovery level (in order to tolerate a minimum of jitters)
						if (!isBeyondRecoveryLevel(transitionType, this.thresholdDeque.getSecurityValue())) {
							// threshold must conform perfectly to the threshold requirements which are level and MINIMUM time
							this.triggerState = TriggerState.RECOVERING;
							this.recoveryDeque.addLast(translatedValue, timeStamp_100ns);
							if (log.isLoggable(Level.OFF)) log.log(Level.OFF, this.toString());
						}
						else {
							this.triggerState = TriggerState.WAITING;
							if (log.isLoggable(Level.OFF)) log.log(Level.OFF, this.toString());
							this.thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), transitionType.getThresholdTimeMsec() * 10);
							this.referenceDeque.initialize(i);
							this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
						}
					}
					else if (!isInThresholdTime && isPersistentTrigger) {
						this.thresholdDeque.addLast(translatedValue, timeStamp_100ns);
					}
					else if (!isInThresholdTime && !isPersistentTrigger) { // no hysteresis 
						this.triggerState = TriggerState.WAITING;
						if (log.isLoggable(Level.OFF)) log.log(Level.OFF, this.toString());
						this.thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), transitionType.getThresholdTimeMsec() * 10);
						this.referenceDeque.initialize(i);
						this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
					}
					else if (isInThresholdTime && !isPersistentTrigger) { // no hysteresis 
						this.triggerState = TriggerState.WAITING;
						if (log.isLoggable(Level.OFF)) log.log(Level.OFF, this.toString());
						this.thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), transitionType.getThresholdTimeMsec() * 10);
						this.referenceDeque.initialize(i);
						this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
					}
					else {
						// currently within thresholdTime --- entry has been added to deque already
					}
					break;
				case RECOVERING:
					this.previousTriggerState = this.triggerState;
					boolean isPersistentRecovery = isBeyondRecoveryLevel(transitionType, translatedValue);
					boolean isRecoveryFinalized = !this.recoveryDeque.tryAddLast(translatedValue, timeStamp_100ns);
					if (isRecoveryFinalized) {
						// final check if the majority of the reference and recovery values did not pass the threshold and if only a minimum of the threshold values actually passed the recovery level
						if (!isBeyondThresholdLevel(absoluteDelta, transitionType, this.referenceDeque.getSecurityValue()) //
								&& !isBeyondRecoveryLevel(transitionType, this.thresholdDeque.getSecurityValue()) //
								&& !isBeyondThresholdLevel(absoluteDelta, transitionType, this.recoveryDeque.getSecurityValue())) {
							this.triggerState = TriggerState.WAITING;
							if (log.isLoggable(Level.OFF)) log.log(Level.OFF, this.toString());
							Transition transition = new Transition(this.referenceDeque.startIndex, this.referenceDeque.size(), this.thresholdDeque.startIndex, this.thresholdDeque.size(),
									this.recoveryDeque.startIndex, this.recoveryDeque.size(), transitionRecord, transitionType);
							transition.setReferenceExtremumValue(this.referenceDeque.getExtremeValue());
							transition.setThresholdExtremumValue(this.thresholdDeque.getExtremeValue());
							transition.setRecoveryExtremumValue(this.recoveryDeque.getExtremeValue());
							this.add(transition);
							log.log(Level.OFF, transition.toString());
							this.referenceDeque = this.recoveryDeque.clone();
							this.referenceDeque.initialize(this.recoveryDeque.getStartIndex());
							this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
							this.thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), transitionType.getThresholdTimeMsec() * 10);
							this.recoveryDeque = new SettlementDeque(recoveryDequeSize, transitionType.isGreater(), transitionType.getRecoveryTimeMsec() * 10);
						}
						else {
							if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, String.format("trigger security check provoked a fallback %s: translatedValue=%f  thresholdAverage=%f", //$NON-NLS-1$
									this.thresholdDeque.getFormattedTime(this.thresholdDeque.size() - 1), translatedValue, this.thresholdDeque.getAverageValue()));
							this.triggerState = TriggerState.WAITING;
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.toString());
							this.thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), transitionType.getThresholdTimeMsec() * 10);
							this.referenceDeque.initialize(i);
							this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
						}
					}
					else if (!isRecoveryFinalized && !isPersistentRecovery) {
						// currently within recovery time
						this.triggerState = TriggerState.WAITING;
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.toString());
						this.thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), transitionType.getThresholdTimeMsec() * 10);
						this.referenceDeque.initialize(i);
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
				if (!isBeyondThresholdLevel(absoluteDelta, transitionType, this.referenceDeque.getSecurityValue()) //
						&& !isBeyondRecoveryLevel(transitionType, this.thresholdDeque.getSecurityValue())//
						&& !isBeyondThresholdLevel(absoluteDelta, transitionType, this.recoveryDeque.getSecurityValue())) {
					Transition transition = new Transition(this.referenceDeque.startIndex, this.referenceDeque.size(), this.thresholdDeque.startIndex, this.thresholdDeque.size(), this.recoveryDeque.startIndex,
							this.recoveryDeque.size(), transitionRecord, transitionType);
					transition.setReferenceExtremumValue(this.referenceDeque.extremeValue);
					transition.setThresholdExtremumValue(this.thresholdDeque.extremeValue);
					transition.setRecoveryExtremumValue(this.recoveryDeque.extremeValue);
					this.add(transition);
					log.log(Level.OFF, transition.toString());
				}
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
					boolean isThresholdLevel = isBeyondThresholdLevel(absoluteDelta, transitionType, translatedValue);
					// check if the current value is beyond the threshold and if the majority of the reference values did not pass the threshold 
					if (isThresholdLevel && this.referenceDeque.isStuffed() && !isBeyondThresholdLevel(absoluteDelta, transitionType, this.referenceDeque.getSecurityValue())) {
						this.triggerState = TriggerState.TRIGGERED;
						this.thresholdDeque.initialize(i);
						this.thresholdDeque.addLast(translatedValue, timeStamp_100ns);
						if (log.isLoggable(Level.OFF)) log.log(Level.OFF, this.toString());
					}
					else {
						this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
					}
					break;
				case TRIGGERED:
					this.previousTriggerState = this.triggerState;
					boolean isPersistentTrigger = isBeyondThresholdLevel(absoluteDelta, transitionType, translatedValue);
					boolean isInThresholdTime = this.thresholdDeque.tryAddLast(translatedValue, timeStamp_100ns);
					if (!isInThresholdTime) {
						// final check if the majority of the reference values did not pass the threshold and if the majority of the threshold values passed the threshold 
						if (!isBeyondThresholdLevel(absoluteDelta, transitionType, this.referenceDeque.getSecurityValue())) {
							this.triggerState = TriggerState.WAITING;
							if (log.isLoggable(Level.OFF)) log.log(Level.OFF, this.toString());
							Transition transition = new Transition(this.referenceDeque.startIndex, this.referenceDeque.size(), this.thresholdDeque.startIndex, this.thresholdDeque.size(),
									this.recoveryDeque.startIndex, this.recoveryDeque.size(), transitionRecord, transitionType);
							transition.setReferenceExtremumValue(this.referenceDeque.getExtremeValue());
							transition.setThresholdExtremumValue(this.thresholdDeque.getExtremeValue());
							this.add(transition);
							log.log(Level.OFF, transition.toString());
							this.referenceDeque.initialize(i);
							this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
							this.thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), transitionType.getThresholdTimeMsec() * 10);
							if (log.isLoggable(Level.OFF)) log.log(Level.OFF, this.toString());
						}
						else {
							if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, String.format("trigger security check provoked a fallback %s: translatedValue=%f  thresholdAverage=%f", //$NON-NLS-1$
									this.thresholdDeque.getFormattedTime(this.thresholdDeque.size() - 1), translatedValue, this.thresholdDeque.getAverageValue()));
							this.triggerState = TriggerState.WAITING;
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.toString());
							this.referenceDeque.initialize(i - this.thresholdDeque.size());
							this.referenceDeque.addLastByMoving(this.thresholdDeque);
							this.thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), transitionType.getThresholdTimeMsec() * 10);
						}
					}
					else if (isInThresholdTime && !isPersistentTrigger) { // threshold must conform perfectly to the threshold requirements which are level and time
						this.triggerState = TriggerState.WAITING;
						if (log.isLoggable(Level.OFF)) log.log(Level.OFF, this.toString());
						this.referenceDeque.initialize(i - this.thresholdDeque.size());
						this.referenceDeque.addLastByMoving(this.thresholdDeque);
						this.thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), transitionType.getThresholdTimeMsec() * 10);
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

		return mySize != this.size();
	}

	@Override
	public synchronized String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s > %s %s:", this.previousTriggerState, this.triggerState, this.referenceDeque.getFormattedTime(this.referenceDeque.size() - 1)));
		sb.append(String.format(" referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", this.referenceDeque.size(), this.thresholdDeque.size(), this.recoveryDeque.size()))
				.append(GDE.STRING_NEW_LINE);
		sb.append(String.format("%38s reference bench=%.1f extreme=", "", this.referenceDeque.getBenchmarkValue()) + this.referenceDeque.getExtremeValue() + this.referenceDeque.getTranslatedValues());
		sb.append(GDE.STRING_BLANK_AT_BLANK + this.referenceDeque.getTimestamps_100ns()).append(GDE.STRING_NEW_LINE);
		sb.append(String.format("%38s threshold bench=%.1f extreme=", "", this.thresholdDeque.getBenchmarkValue()) + this.thresholdDeque.getExtremeValue() + this.thresholdDeque.getTranslatedValues());
		sb.append(GDE.STRING_BLANK_AT_BLANK + this.thresholdDeque.getTimestamps_100ns()).append(GDE.STRING_NEW_LINE);
		sb.append(String.format("%38s recovery  bench=%.1f extreme=", "", this.recoveryDeque.getBenchmarkValue()) + this.recoveryDeque.getExtremeValue() + this.recoveryDeque.getTranslatedValues());
		sb.append(GDE.STRING_BLANK_AT_BLANK + this.recoveryDeque.getTimestamps_100ns());
		return sb.toString();
	}

	/**
	 * @param absoluteDelta ensures for DELTA_FACTOR transitions a minimum delta between the translated value and the base value derived from the reference period
	 * @param transitionType
	 * @param translatedValue
	 * @return true if level exceeded (based on isGreater property)
	 */
	private boolean isBeyondThresholdLevel(double absoluteDelta, TransitionType transitionType, double translatedValue) {
		final double absoluteThresholdLevel;
		final boolean isBeyond;
		if (this.referenceDeque.isEmpty()) {
			isBeyond = false;
		}
		else if (transitionType.getValueType() == TransitionValueTypes.DELTA_FACTOR) {
			final double baseValue = this.referenceDeque.getBenchmarkValue();
			boolean minimumAbsoluteLevelExceeded = transitionType.isGreater() ? translatedValue > absoluteDelta + baseValue : translatedValue < baseValue - absoluteDelta;
			absoluteThresholdLevel = baseValue + transitionType.getThresholdValue() * baseValue;
			isBeyond = minimumAbsoluteLevelExceeded && (transitionType.isGreater() ? translatedValue > absoluteThresholdLevel : translatedValue < absoluteThresholdLevel);
		}
		else if (transitionType.getValueType() == TransitionValueTypes.DELTA_VALUE) {
			final double baseValue = this.referenceDeque.getBenchmarkValue();
			absoluteThresholdLevel = baseValue + transitionType.getThresholdValue() * 1000.;
			isBeyond = transitionType.isGreater() ? translatedValue > absoluteThresholdLevel : translatedValue < absoluteThresholdLevel;
		}
		else {
			absoluteThresholdLevel = transitionType.getThresholdValue() * 1000.;
			isBeyond = transitionType.isGreater() ? translatedValue > absoluteThresholdLevel : translatedValue < absoluteThresholdLevel;
		}
		return isBeyond;
	}

	private boolean isBeyondRecoveryLevel(TransitionType transitionType, double translatedValue) {
		final double absoluteRecoveryLevel;
		if (transitionType.getValueType() == TransitionValueTypes.DELTA_FACTOR) {
			final double baseValue = this.referenceDeque.getBenchmarkValue();
			absoluteRecoveryLevel = baseValue + transitionType.getRecoveryValue() * this.referenceDeque.getBenchmarkValue();
		}
		else if (transitionType.getValueType() == TransitionValueTypes.DELTA_VALUE) {
			final double baseValue = this.referenceDeque.getBenchmarkValue();
			absoluteRecoveryLevel = baseValue + transitionType.getRecoveryValue() * 1000.;
		}
		else {
			absoluteRecoveryLevel = transitionType.getRecoveryValue() * 1000.;
		}
		return transitionType.isGreater() ? translatedValue < absoluteRecoveryLevel : translatedValue > absoluteRecoveryLevel;
	}

	public List<Transition> getTransitions(Integer transitionId) {
		List<Transition> result = new ArrayList<>();
		for (Transition transition : this) {
			if (transition.transitionType.getTransitionId() == transitionId) {
				result.add(transition);
			}
		}
		return result;
	}

}
