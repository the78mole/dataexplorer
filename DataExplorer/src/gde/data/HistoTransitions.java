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
 
 Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
    					2016 Thomas Eickert
****************************************************************************************/
package gde.data;

import gde.config.Settings;
import gde.device.IDevice;
import gde.device.TransitionType;
import gde.log.Level;
import gde.ui.DataExplorer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * holds all transitions for the histo recordSet.
 * the transitions are identified by applying the transition type to the histo recordSet.
 * sorted by timestamp.
 * @author Thomas Eickert
 */
public class HistoTransitions extends Vector<Transition> {
	final static String					$CLASS_NAME				= HistoTransitions.class.getName();
	private static final long		serialVersionUID	= -3373317163399403911L;
	final static Logger					log								= Logger.getLogger($CLASS_NAME);

	private final DataExplorer	application				= DataExplorer.getInstance();
	private final Settings			settings					= Settings.getInstance();
	private final Channels			channels					= Channels.getInstance();
	private final IDevice				device;

	private final RecordSet			parent;

	private enum TriggerState {
		UNDEFINED,
		WAITING,
		TRIGGERED,
		RECOVERING;
	}

	class DequePair {
		long		timeStamp_100ns;
		double	translatedValue;

		DequePair(double translatedValue, long timeStamp_100ns) {
			this.translatedValue = translatedValue;
			this.timeStamp_100ns = timeStamp_100ns;
		}
	}

	/**
	 * provides the extremum value of the deque and the information if the deque has sufficient elements for the deque time period.
	 * ensures the maximum deque time period by removing elements from the front of the deque.
	 * expects elements to be added in chronological order.
	 * Does not support null measurement values.
	 * Resizable-array implementation of the Deque interface.
	 * @author Thomas Eickert
	 */
	private class SettlementDeque extends ArrayDeque<DequePair> {

		private static final long	serialVersionUID	= 915484098600135376L;
		private boolean				isGreater;
		private long					timePeriod_100ns;
		private double					extremeValue;
		private boolean				isExhausted;

		SettlementDeque(int numElements, boolean isMinimumExtremum, long timePeriod_100ns) {
			super(numElements);
			this.isGreater = isMinimumExtremum;
			this.timePeriod_100ns = timePeriod_100ns;
			this.extremeValue = this.isGreater ? Double.MAX_VALUE : -Double.MAX_VALUE;
			this.isExhausted = false;
		}

		/*
		 * (non-Javadoc)
		 * ensures the maximum deque time period by removing elements from the front of the decque.
		 * 
		 * @see java.util.ArrayDeque#add(java.lang.Object)
		 */
		public boolean add(DequePair newDequePair) {
			boolean bb = super.add(newDequePair);
			this.extremeValue = this.isGreater ? Math.min(this.extremeValue, newDequePair.translatedValue) : Math.max(this.extremeValue, newDequePair.translatedValue);
			if (bb)
				ensureTimePeriod();
			return bb;
		}

		/*
		 * (non-Javadoc)
		 * ensures the maximum deque time period by removing elements from the front of the decque.
		 * 
		 * @see java.util.ArrayDeque#addFirst(java.lang.Object)
		 */
		public void addFirst(DequePair newDequePair) {
			super.addFirst(newDequePair);
			this.extremeValue = this.isGreater ? Math.min(this.extremeValue, newDequePair.translatedValue) : Math.max(this.extremeValue, newDequePair.translatedValue);
			ensureTimePeriod();
		}

		/*
		 * (non-Javadoc)
		 * ensures the maximum deque time period by removing elements from the front of the decque.
		 * 
		 * @see java.util.ArrayDeque#addLast(java.lang.Object)
		 */
		public void addLast(DequePair newDequePair) {
			super.addLast(newDequePair);
			this.extremeValue = this.isGreater ? Math.min(this.extremeValue, newDequePair.translatedValue) : Math.max(this.extremeValue, newDequePair.translatedValue);
			ensureTimePeriod();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#clear()
		 */
		public void clear() {
			super.clear();
			this.extremeValue = this.isGreater ? Double.MAX_VALUE : -Double.MAX_VALUE;
			this.isExhausted = false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#clone()
		 */
		public SettlementDeque clone() {
			SettlementDeque cloned = (SettlementDeque) super.clone();
			cloned.isGreater = this.isGreater;
			cloned.timePeriod_100ns = this.timePeriod_100ns;
			cloned.extremeValue = this.extremeValue;
			cloned.isExhausted = this.isExhausted;
			return cloned;
		}

		/*
		 * (non-Javadoc)
		 * ensures the maximum deque time period by removing elements from the front of the decque.
		 * 
		 * @see java.util.ArrayDeque#offer(java.lang.Object)
		 */
		public boolean offer(DequePair newDequePair) {
			boolean bb = super.offer(newDequePair);
			this.extremeValue = this.isGreater ? Math.min(this.extremeValue, newDequePair.translatedValue) : Math.max(this.extremeValue, newDequePair.translatedValue);
			if (bb)
				ensureTimePeriod();
			return bb;
		}

		/*
		 * (non-Javadoc)
		 * ensures the maximum deque time period by removing elements from the front of the decque.
		 * 
		 * @see java.util.ArrayDeque#offerFirst(java.lang.Object)
		 */
		public boolean offerFirst(DequePair newDequePair) {
			boolean bb = super.offerFirst(newDequePair);
			this.extremeValue = this.isGreater ? Math.min(this.extremeValue, newDequePair.translatedValue) : Math.max(this.extremeValue, newDequePair.translatedValue);
			return bb ? ensureTimePeriod() : false;
		}

		/*
		 * (non-Javadoc)
		 * ensures the maximum deque time period by removing elements from the front of the decque.
		 * 
		 * @see java.util.ArrayDeque#offerLast(java.lang.Object)
		 */
		public boolean offerLast(DequePair newDequePair) {
			boolean bb = super.offerLast(newDequePair);
			this.extremeValue = this.isGreater ? Math.min(this.extremeValue, newDequePair.translatedValue) : Math.max(this.extremeValue, newDequePair.translatedValue);
			if (bb)
				ensureTimePeriod();
			return bb;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#poll()
		 */
		public DequePair poll() {
			DequePair polled = super.poll();
			this.extremeValue = getExtremeValue(polled);
			if (polled != null)
				this.isExhausted = false;
			return polled;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#pollFirst()
		 */
		public DequePair pollFirst() {
			DequePair polled = super.pollFirst();
			this.extremeValue = getExtremeValue(polled);
			if (polled != null)
				this.isExhausted = false;
			return polled;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#pollLast()
		 */
		public DequePair pollLast() {
			DequePair polled = super.pollLast();
			this.extremeValue = getExtremeValue(polled);
			if (polled != null)
				this.isExhausted = false;
			return polled;
		}

		/*
		 * (non-Javadoc)
		 * ensures the maximum deque time period by removing elements from the front of the decque.
		 * 
		 * @see java.util.ArrayDeque#push(java.lang.Object)
		 */
		public void push(DequePair newDequePair) {
			super.push(newDequePair);
			this.extremeValue = this.isGreater ? Math.min(this.extremeValue, newDequePair.translatedValue) : Math.max(this.extremeValue, newDequePair.translatedValue);
			ensureTimePeriod();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#pop()
		 */
		public DequePair pop() {
			DequePair popped = super.pop();
			this.extremeValue = getExtremeValue(popped);
			if (popped != null)
				this.isExhausted = false;
			return popped;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#remove()
		 */
		public DequePair remove() {
			DequePair removed = super.remove();
			this.extremeValue = getExtremeValue(removed);
			if (removed != null)
				this.isExhausted = false;
			return removed;
		}

		/**
		 * Removes a single instance of the specified element from this deque.
		 * @param dequePair
		 * @return
		 */
		public boolean remove(DequePair dequePair) {
			boolean bb = super.remove(dequePair);
			this.extremeValue = getExtremeValue(dequePair);
			if (bb)
				this.isExhausted = false;
			return bb;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#removeFirst()
		 */
		public DequePair removeFirst() {
			DequePair removed = super.removeFirst();
			this.extremeValue = getExtremeValue(removed);
			if (removed != null)
				this.isExhausted = false;
			return removed;
		}

		/**
		 * Removes the first occurrence of the specified element in this deque (when traversing the deque from head to tail).
		 * @param dequePair
		 * @return
		 */
		public boolean removeFirstOccurrence(DequePair dequePair) {
			boolean bb = super.removeFirstOccurrence(dequePair);
			this.extremeValue = getExtremeValue(dequePair);
			if (bb)
				this.isExhausted = false;
			return bb;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.ArrayDeque#removeLast()
		 */
		public DequePair removeLast() {
			DequePair removed = super.removeLast();
			this.extremeValue = getExtremeValue(removed);
			if (removed != null)
				this.isExhausted = false;
			return removed;
		}

		/**
		 * Removes the last occurrence of the specified element in this deque (when traversing the deque from head to tail).
		 * @param dequePair
		 * @return
		 */
		public boolean removeLastOccurrence(DequePair dequePair) {
			boolean bb = super.removeLastOccurrence(dequePair);
			this.extremeValue = getExtremeValue(dequePair);
			if (bb)
				this.isExhausted = false;
			return bb;
		}

		/**
		 * Retrieves and removes the elements of this deque.
		 * @param dequePairs
		 * @return elements which were removed successfully.
		 */
		public ArrayList<DequePair> removeList(ArrayList<DequePair> dequePairs) {
			ArrayList<DequePair> removedPairs = new ArrayList<DequePair>();
			for (DequePair dequePair : dequePairs) {
				if (super.remove(dequePair)) {
					removedPairs.add(dequePair);
				}
			}
			this.extremeValue = getExtremeValue(removedPairs);
			if (removedPairs.size() > 0)
				this.isExhausted = false;
			return removedPairs;
		}

		/**
		 * inserts the specified element at the end of this deque if the deque time period will not be exceeded. 
		 * @param dequePair
		 * @return false if the timestamp is beyond the deque time period or if the element already exists in the deque. 
		 */
		public boolean tryAdd(DequePair dequePair) {
			if (this.peekFirst().timeStamp_100ns + this.timePeriod_100ns > dequePair.timeStamp_100ns) {
				return this.add(dequePair);
			} else {
				this.isExhausted = true;
				return false;
			}
		}

		/**
		 * @return true if the time distance of the first and the last deque elements equals the deque time period.
		 */
		public boolean isFilledToCapacity() {
			return this.peekFirst().timeStamp_100ns + this.timePeriod_100ns == this.peekLast().timeStamp_100ns;
		}

		/**
		 * indicates if the next add will be most likely beyond the current time window position.
		 * however, a smaller timestep than the last one may fit.
		 * @return true if the deque elements cover the whole deque time period.
		 */
		public boolean isStuffed() {
			return this.isExhausted || isFilledToCapacity();
		}

		private boolean ensureTimePeriod() {
			this.isExhausted = false;
			ArrayList<DequePair> removedList = new ArrayList<DequePair>();
			for (; this.peekFirst().timeStamp_100ns + this.timePeriod_100ns < this.peekLast().timeStamp_100ns;) {
				removedList.add(this.removeFirst());
				this.isExhausted = true;
			}
			return this.isExhausted;
		}

		private double getExtremeValue(DequePair removedPair) {
			boolean isExtremeValueRemoved = removedPair.translatedValue == this.extremeValue;
			if (this.size() == 0 || isExtremeValueRemoved) {
				if (log.isLoggable(Level.FINEST))
					log.log(Level.FINEST, String.format("translatedValue=%f extremeValue=%f", removedPair.translatedValue, this.extremeValue));
				double extremeValue = this.isGreater ? Double.MAX_VALUE : -Double.MAX_VALUE;
				for (DequePair pair : this) {
					extremeValue = this.isGreater ? Math.min(extremeValue, pair.translatedValue) : Math.max(extremeValue, pair.translatedValue);
				}
				if (log.isLoggable(Level.FINEST))
					log.log(Level.FINEST, String.format("extremeValue=%f", this.extremeValue));
				return extremeValue;
			} else
				return this.extremeValue;
		}

		private double getExtremeValue(ArrayList<DequePair> removedPairs) {
			boolean isExtremeValueRemoved = false;
			for (DequePair dequePair : removedPairs) {
				if (dequePair.translatedValue == this.extremeValue) {
					isExtremeValueRemoved = true;
					break;
				}
			}
			if (this.size() == 0 || isExtremeValueRemoved) { // at least one element holding the extremum value was removed: check all entries if the extremum is still present
				if (log.isLoggable(Level.FINEST))
					log.log(Level.FINEST, String.format("extremeValue=%f", this.extremeValue));
				double extremeValue = this.isGreater ? Double.MAX_VALUE : -Double.MAX_VALUE;
				for (DequePair pair : this) {
					extremeValue = this.isGreater ? Math.min(extremeValue, pair.translatedValue) : Math.max(extremeValue, pair.translatedValue);
				}
				if (log.isLoggable(Level.FINEST))
					log.log(Level.FINEST, String.format("extremeValue=%f", this.extremeValue));
				return extremeValue;
			} else
				return this.extremeValue;
		}

		public ArrayList<Double> getTranslatedValues() {
			ArrayList<Double> values = new ArrayList<Double>();
			for (DequePair dequePair : this) {
				values.add(dequePair.translatedValue);
			}
			return values;
		}

		public ArrayList<Long> getTimestamps_ms() {
			ArrayList<Long> values = new ArrayList<Long>();
			for (DequePair dequePair : this) {
				values.add(dequePair.timeStamp_100ns * 10);
			}
			return values;
		}
	}

	public HistoTransitions(IDevice newDevice, RecordSet parent) {
		super(10, 11);
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
		SettlementDeque referenceDeque = null;
		SettlementDeque thresholdDeque = null;
		SettlementDeque recoveryDeque = null;
		int mySize = this.size();

		int referenceDequeSize = (int) (transitionType.getReferenceTimeMsec() / this.parent.timeStep_ms.getAverageTimeStep_ms()) + 11; // plus 11 elements ensures sufficient elements for most cases
		int thresholdDequeSize = (int) (transitionType.getThresholdTimeMsec() / this.parent.timeStep_ms.getAverageTimeStep_ms()) + 11; // plus 11 elements ensures sufficient elements for most cases
		int recoveryDequeSize = (int) (transitionType.getRecoveryTimeMsec() / this.parent.timeStep_ms.getAverageTimeStep_ms()) + 11; // plus 11 elements ensures sufficient elements for most cases
		// if (log.isLoggable(Level.FINE))
		log.log(Level.SEVERE, transitionRecord.parent.name + " " + transitionRecord.parent.description
				+ String.format("%s initialized: referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", transitionRecord.getName(), referenceDequeSize, thresholdDequeSize, recoveryDequeSize)); //$NON-NLS-1$
		if (transitionType.isPercent()) {
			TriggerState triggerState = TriggerState.UNDEFINED;
			for (int i = 0; i < transitionRecord.size(); i++) {
				if (transitionRecord.get(i) == null)
					break;
				long timeStamp_100ns = (long) (transitionRecord.getTime_ms(i) * 10.);
				Double translatedValue = this.device.translateValue(transitionRecord, transitionRecord.get(i));
				switch (triggerState) {
				case UNDEFINED:
					if (recoveryDeque == null) {
						referenceDeque = new SettlementDeque(referenceDequeSize, transitionType.isGreater(), transitionType.getReferenceTimeMsec() * 10);
					} else { // take from previous trigger
						referenceDeque = recoveryDeque.clone(); // TODO take only the number of elements which are required
					}
					thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), transitionType.getThresholdTimeMsec() * 10);
					recoveryDeque = new SettlementDeque(recoveryDequeSize, transitionType.isGreater(), transitionType.getRecoveryTimeMsec() * 10);
					if (translatedValue == null) {
						
					} else {
						triggerState = TriggerState.WAITING;
						referenceDeque.add(new DequePair(translatedValue, timeStamp_100ns));
					}
					break;
				case WAITING:
					boolean isInitiated = isPercentageCrossed(transitionType, transitionType.getTriggerLevel(), referenceDeque.extremeValue, translatedValue, true);
					if (isInitiated) { // trigger has fired right now
						triggerState = TriggerState.TRIGGERED;
						thresholdDeque.add(new DequePair(translatedValue, timeStamp_100ns));
						// if (log.isLoggable(Level.FINE))
						log.log(Level.SEVERE, String.format("%s: referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", triggerState, referenceDeque.size(), thresholdDeque.size(), recoveryDeque.size())); //$NON-NLS-1$
					} else { // continue waiting for trigger fire
						referenceDeque.add(new DequePair(translatedValue, timeStamp_100ns));
					}
					break;
				case TRIGGERED:
					boolean isRecovering = isPercentageCrossed(transitionType, transitionType.getRecoveryLevel(), referenceDeque.extremeValue, translatedValue, false);
					if (isRecovering) { // force recovery if threshold time has expired
						triggerState = TriggerState.RECOVERING;
						recoveryDeque.add(new DequePair(translatedValue, timeStamp_100ns));
					} else if (!thresholdDeque.tryAdd(new DequePair(translatedValue, timeStamp_100ns))) { // threshold time has expired
						log.log(Level.SEVERE, String.format("%s > WAITING: referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", triggerState, referenceDeque.size(), thresholdDeque.size(), recoveryDeque.size())); //$NON-NLS-1$
						log.log(Level.SEVERE, "referenceDeque " + referenceDeque.getTranslatedValues() + " @ " + referenceDeque.getTimestamps_ms()); //$NON-NLS-1$
						log.log(Level.SEVERE, "thresholdDeque " + thresholdDeque.getTranslatedValues() + " @ " + thresholdDeque.getTimestamps_ms()); //$NON-NLS-1$
						log.log(Level.SEVERE, "recoveryDeque " + recoveryDeque.getTranslatedValues() + " @ " + recoveryDeque.getTimestamps_ms()); //$NON-NLS-1$
						triggerState = TriggerState.WAITING;
						for (DequePair dequePair : thresholdDeque) {
							referenceDeque.add(dequePair);
						}
						thresholdDeque.clear();
						referenceDeque.add(new DequePair(translatedValue, timeStamp_100ns));
					}
					break;
				case RECOVERING:
					boolean isRecoveryFinalized;
					boolean isPersistentRecovery = isPercentageCrossed(transitionType, transitionType.getRecoveryLevel(), referenceDeque.extremeValue, translatedValue, false);
					boolean isThresholdTimeExceeded = timeStamp_100ns - thresholdDeque.getFirst().timeStamp_100ns >= transitionType.getThresholdTimeMsec() * 10;
					if (isPersistentRecovery) {
						isRecoveryFinalized = !recoveryDeque.tryAdd(new DequePair(translatedValue, timeStamp_100ns));
					} else if (isThresholdTimeExceeded) {
						isRecoveryFinalized = !recoveryDeque.tryAdd(new DequePair(translatedValue, timeStamp_100ns));
						if (!isRecoveryFinalized) { // recovery must conform perfectly to the recovery requirements which are level and time
							log.log(Level.SEVERE, String.format("%s > WAITING: referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", triggerState, referenceDeque.size(), thresholdDeque.size(), recoveryDeque.size())); //$NON-NLS-1$
							log.log(Level.SEVERE, "referenceDeque " + referenceDeque.getTranslatedValues() + " @ " + referenceDeque.getTimestamps_ms()); //$NON-NLS-1$
							log.log(Level.SEVERE, "thresholdDeque " + thresholdDeque.getTranslatedValues() + " @ " + thresholdDeque.getTimestamps_ms()); //$NON-NLS-1$
							log.log(Level.SEVERE, "recoveryDeque " + recoveryDeque.getTranslatedValues() + " @ " + recoveryDeque.getTimestamps_ms()); //$NON-NLS-1$
							triggerState = TriggerState.WAITING;
							for (DequePair dequePair : thresholdDeque) {
								referenceDeque.add(dequePair);
							}
							thresholdDeque.clear();
							for (DequePair dequePair : recoveryDeque) {
								referenceDeque.add(dequePair);
							}
							recoveryDeque.clear();
						}
						// if (log.isLoggable(Level.FINE))
						log.log(Level.SEVERE, String.format("%s: referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", triggerState, referenceDeque.size(), thresholdDeque.size(), recoveryDeque.size())); //$NON-NLS-1$
					} else { // try to extend the threshold time, interpret entries as jitter
						log.log(Level.SEVERE, String.format("%s > TRIGGERED: referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d  translatedValue=%f", triggerState, referenceDeque.size(), thresholdDeque.size(), recoveryDeque.size(), translatedValue)); //$NON-NLS-1$
						log.log(Level.SEVERE, "referenceDeque " + referenceDeque.getTranslatedValues() + " @ " + referenceDeque.getTimestamps_ms()); //$NON-NLS-1$
						log.log(Level.SEVERE, "thresholdDeque " + thresholdDeque.getTranslatedValues() + " @ " + thresholdDeque.getTimestamps_ms()); //$NON-NLS-1$
						log.log(Level.SEVERE, "recoveryDeque " + recoveryDeque.getTranslatedValues() + " @ " + recoveryDeque.getTimestamps_ms()); //$NON-NLS-1$
						ArrayList<DequePair> movedPairs = new ArrayList<DequePair>();
						for (DequePair dequePair : recoveryDeque) {
							if (thresholdDeque.tryAdd(dequePair)) {
								movedPairs.add(dequePair);
							} else {
								break;
							}
						}
						recoveryDeque.removeList(movedPairs);
						if (recoveryDeque.size() <= 0 & thresholdDeque.tryAdd(new DequePair(translatedValue, timeStamp_100ns))) { // threshold time is not exceeded up to now
							triggerState = TriggerState.TRIGGERED;
							isRecoveryFinalized = false;
							// if (log.isLoggable(Level.FINE))
							log.log(Level.SEVERE, String.format("%s: referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", triggerState, referenceDeque.size(), thresholdDeque.size(), recoveryDeque.size())); //$NON-NLS-1$
						} else { // threshold time is exceeded: jitters have been accepted
							isRecoveryFinalized = !recoveryDeque.tryAdd(new DequePair(translatedValue, timeStamp_100ns));
						}
					}
					if (isRecoveryFinalized) {
						log.log(Level.SEVERE, String.format("%s finalized: referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", triggerState, referenceDeque.size(), thresholdDeque.size(), recoveryDeque.size())); //$NON-NLS-1$
						log.log(Level.SEVERE, "referenceDeque " + referenceDeque.getTranslatedValues() + " @ " + referenceDeque.getTimestamps_ms()); //$NON-NLS-1$
						log.log(Level.SEVERE, "thresholdDeque " + thresholdDeque.getTranslatedValues() + " @ " + thresholdDeque.getTimestamps_ms()); //$NON-NLS-1$
						log.log(Level.SEVERE, "recoveryDeque " + recoveryDeque.getTranslatedValues() + " @ " + recoveryDeque.getTimestamps_ms()); //$NON-NLS-1$
						triggerState = TriggerState.UNDEFINED;
						Transition transition = new Transition(i - referenceDeque.size() - thresholdDeque.size()
								- recoveryDeque.size(), referenceDeque.size(), thresholdDeque.size(), recoveryDeque.size(), transitionRecord, transitionType);
						this.add(transition);
					}
					break;
				} // end switch
			} // end for
		} else {
			TriggerState triggerState = TriggerState.UNDEFINED;
			for (int i = 0; i < transitionRecord.size(); i++) {
				if (transitionRecord.get(i) == null)
					break;
				long timeStamp_100ns = (long) (transitionRecord.getTime_ms(i) * 10.);
				double translatedValue = this.device.translateValue(transitionRecord, transitionRecord.get(i));
				switch (triggerState) {
				case UNDEFINED:
					if (recoveryDeque == null) {
						referenceDeque = new SettlementDeque(referenceDequeSize, transitionType.isGreater(), transitionType.getReferenceTimeMsec() * 10);
						thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), Long.MAX_VALUE); // allow unlimited trigger length
						recoveryDeque = new SettlementDeque(recoveryDequeSize, transitionType.isGreater(), transitionType.getRecoveryTimeMsec() * 10);
						triggerState = TriggerState.WAITING;
						referenceDeque.add(new DequePair(translatedValue, timeStamp_100ns));
					} else { // take from previous trigger
						referenceDeque = recoveryDeque.clone(); // TODO take only the number of elements which are required
						thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), Long.MAX_VALUE); // allow unlimited trigger length
						recoveryDeque = new SettlementDeque(recoveryDequeSize, transitionType.isGreater(), transitionType.getRecoveryTimeMsec() * 10);
						// initialize for next trigger
						boolean isInitiatedFromRecovery = referenceDeque.isFilledToCapacity()
								&& isLevelCrossed(transitionType, transitionType.getTriggerLevel(), translatedValue, true);
						if (isInitiatedFromRecovery) { // trigger has fired right now
							triggerState = TriggerState.TRIGGERED;
							thresholdDeque.add(new DequePair(translatedValue, timeStamp_100ns));
						} else {
							triggerState = TriggerState.WAITING;
							referenceDeque.add(new DequePair(translatedValue, timeStamp_100ns));
						}
					}
					// if (log.isLoggable(Level.FINE))
					log.log(Level.SEVERE, String.format("%s: referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", triggerState, referenceDeque.size(), thresholdDeque.size(), recoveryDeque.size())); //$NON-NLS-1$
					break;
				case WAITING:
					boolean isTriggered = isLevelCrossed(transitionType, transitionType.getTriggerLevel(), translatedValue, true);
					if (isTriggered) {
						triggerState = TriggerState.TRIGGERED;
						thresholdDeque.add(new DequePair(translatedValue, timeStamp_100ns));
						// if (log.isLoggable(Level.FINE))
						log.log(Level.SEVERE, String.format("%s: referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", triggerState, referenceDeque.size(), thresholdDeque.size(), recoveryDeque.size())); //$NON-NLS-1$
					} else {
						referenceDeque.add(new DequePair(translatedValue, timeStamp_100ns));
					}
					break;
				case TRIGGERED:
					boolean isPersistentTrigger = !isLevelCrossed(transitionType, transitionType.getRecoveryLevel(), translatedValue, false);
					boolean isInThresholdTime = thresholdDeque.add(new DequePair(translatedValue, timeStamp_100ns));
					;
					if (!isPersistentTrigger && !isInThresholdTime) {
						triggerState = TriggerState.RECOVERING;
						recoveryDeque.add(new DequePair(translatedValue, timeStamp_100ns));
						// if (log.isLoggable(Level.FINE))
						log.log(Level.SEVERE, String.format("%s: referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", triggerState, referenceDeque.size(), thresholdDeque.size(), recoveryDeque.size())); //$NON-NLS-1$
					} else if (!isPersistentTrigger && isInThresholdTime) { // threshold must conform perfectly to the threshold requirements which are level and time
						triggerState = TriggerState.WAITING;
						for (DequePair dequePair : thresholdDeque) {
							referenceDeque.add(dequePair);
						}
						thresholdDeque.clear();
						recoveryDeque.clear();
						// if (log.isLoggable(Level.FINE))
						log.log(Level.SEVERE, String.format("%s: referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", triggerState, referenceDeque.size(), thresholdDeque.size(), recoveryDeque.size())); //$NON-NLS-1$
					}
					break;
				case RECOVERING:
					DequePair newPair = new DequePair(translatedValue, timeStamp_100ns);
					boolean isPersistentRecovery = isLevelCrossed(transitionType, transitionType.getRecoveryLevel(), translatedValue, false);
					boolean isInRecoveryTime = recoveryDeque.tryAdd(newPair);
					if (!isPersistentRecovery && isInRecoveryTime) { // re-activate trigger as recovery time has not expired, interpret entry as jitter
						triggerState = TriggerState.TRIGGERED;
						for (DequePair dequePair : recoveryDeque) {
							thresholdDeque.add(dequePair);
						}
						recoveryDeque.clear();
						// if (log.isLoggable(Level.FINE))
						log.log(Level.SEVERE, String.format("%s: referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", triggerState, referenceDeque.size(), thresholdDeque.size(), recoveryDeque.size())); //$NON-NLS-1$
					} else if (!isInRecoveryTime) { // recovery was successful: record trigger data and test for new trigger just as in waiting state
						triggerState = TriggerState.UNDEFINED;
						Transition transition = new Transition(i - referenceDeque.size() - thresholdDeque.size()
								- recoveryDeque.size(), referenceDeque.size(), thresholdDeque.size(), recoveryDeque.size(), transitionRecord, transitionType);
						this.add(transition);
					}
				}
				break;
			}
		}
		return mySize != this.size();
	}

	/**
	 * @param levelValue percentage
	 * @param baseValue the absolute level is calculated based on this value
	 * @param translatedValue
	 * @param isThresholdValue false inverts the result (threshold checks are based on isGreater value). 
	 * @return true level exceeded
	 */
	private boolean isPercentageCrossed(TransitionType transitionType, int levelValue, double baseValue, double translatedValue, boolean isThreshold) {
		boolean bb;
		bb = transitionType.isGreater() ? translatedValue >= (100 + levelValue) * baseValue / 100. : translatedValue <= (100 - levelValue) * baseValue / 100.;
		return isThreshold ? bb : !bb;
	}

	/**
	 * @param levelValue
	 * @param translatedValue
	 * @param isThresholdValue false inverts the result (threshold checks are based on isGreater value). 
	 * @return true level exceeded
	 */
	private boolean isLevelCrossed(TransitionType transitionType, int levelValue, double translatedValue, boolean isThreshold) {
		boolean bb;
		bb = transitionType.isGreater() ? translatedValue >= levelValue / 1000 : translatedValue <= levelValue / 1000;
		return isThreshold ? bb : !bb;
	}

	public List<Transition> getTransitions(Integer transitionId) {
		List<Transition> result = new ArrayList<Transition>();
		for (Transition transition : this) {
			if (transition.transitionType.getTransitionId() == transitionId) {
				result.add(transition);
			}
		}
		return result;
	}

}
