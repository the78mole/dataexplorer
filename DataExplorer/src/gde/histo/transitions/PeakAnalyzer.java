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

import java.util.TreeMap;
import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.TransitionType;
import gde.log.Level;
import gde.ui.DataExplorer;

/**
 * Analyze a record for transitions defined in the device channel settings.
 * @author Thomas Eickert (USER)
 */
public final class PeakAnalyzer extends AbstractAnalyzer {
	final static String			$CLASS_NAME	= PeakAnalyzer.class.getName();
	final static Logger			log					= Logger.getLogger($CLASS_NAME);

	private final RecordSet	recordSet;

	public PeakAnalyzer(RecordSet recordSet) {
		this.recordSet = recordSet;
	}

	/**
	 * Apply transitionType definitions to the measurement data.
	 * Add all the transitions identified.
	 * Measurements with null values are ignored.
	 * @param record
	 * @param transitionType
	 * @return all transitions with the key thresholdStartTimestamp_ms
	 */
	public TreeMap<Long, Transition> findTransitions(Record record, TransitionType transitionType) {
		this.triggerState = TriggerState.WAITING;
		initializeDeques(record, transitionType);

		return findPeakTransitions(record, transitionType);
	}

	/**
	 * @param record
	 * @param transitionType
	 * @return all transitions with the key thresholdStartTimestamp_ms
	 */
	private TreeMap<Long, Transition> findPeakTransitions(Record record, TransitionType transitionType) {
		TreeMap<Long, Transition> transitions;
		IDevice device = DataExplorer.application.getActiveDevice();

		transitions = new TreeMap<Long, Transition>();
		LevelChecker levelChecker = new LevelChecker(record, transitionType);
		int samplingTimespan_ms = Settings.getInstance().getSamplingTimespan_ms();
		for (int i = 0; i < record.realSize(); i++) {
			if (record.elementAt(i) == null) break;
			if (log.isLoggable(Level.FINER) && i > 0 && samplingTimespan_ms * 2 <= (long) (record.getTime_ms(i) - record.getTime_ms(i - 1)))
				log.log(Level.FINER, String.format("timestamps with distance >= 2 * samplingPeriod: %,d %,d", (int) record.getTime_ms(i), (int) record.getTime_ms(i - 1)));
			long timeStamp_100ns = (long) (record.getTime_ms(i) * 10.);
			double translatedValue = device.translateValue(record, record.elementAt(i) / 1000.);
			switch (this.triggerState) {
			case WAITING:
				this.previousTriggerState = this.triggerState;
				boolean isThresholdLevel = levelChecker.isBeyondThresholdLevel(translatedValue);
				// check if the current value is beyond the threshold and if the majority of the reference values did not pass the threshold
				if (isThresholdLevel && !this.referenceDeque.isAddableInTimePeriod(timeStamp_100ns) && !levelChecker.isBeyondThresholdLevel(this.referenceDeque.getSecurityValue())) {
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
				boolean isRecovered = levelChecker.isBeyondRecoveryLevel(translatedValue);
				// minimumTransitionSteps prevents transitions provoked by jitters in the reference phase; should cover at least 2 'real' measurements
				// also check if only a minimum of the threshold values actually passed the recovery level (in order to tolerate a minimum of jitters)
				if (isRecovered && this.thresholdDeque.size() >= 2 && this.thresholdDeque.getDuration_ms() >= transitionType.getPeakMinimumTimeMsec().orElse(0)
						&& !levelChecker.isBeyondRecoveryLevel(this.thresholdDeque.getSecurityValue())) {
					this.triggerState = TriggerState.RECOVERING;
					this.recoveryDeque.initialize(i);
					this.recoveryDeque.addLast(translatedValue, timeStamp_100ns);
					log.log(Level.FINER, Integer.toString(transitionType.getTransitionId()), this);
				}
				else if (!this.thresholdDeque.isAddableInTimePeriod(timeStamp_100ns)) {
					this.triggerState = TriggerState.WAITING;
					log.log(Level.FINER, " isThresholdTimeExceeded ", this); //$NON-NLS-1$
					this.referenceDeque.addLastByMoving(this.thresholdDeque);
					// referenceStartIndex is not modified by jitters
					this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
				}
				else { // threshold phase continues
					this.thresholdDeque.addLast(translatedValue, timeStamp_100ns);
				}
				break;
			case RECOVERING:
				this.previousTriggerState = this.triggerState;
				boolean isPersistentRecovery = levelChecker.isBeyondRecoveryLevel(translatedValue);
				if (!this.recoveryDeque.isAddableInTimePeriod(timeStamp_100ns)) {
					// final check if the majority of the reference and recovery values did not pass the threshold and if only a minimum of the threshold values actually passed the recovery level
					if (!levelChecker.isBeyondThresholdLevel(this.referenceDeque.getSecurityValue()) //
							&& !levelChecker.isBeyondRecoveryLevel(this.thresholdDeque.getSecurityValue()) //
							&& !levelChecker.isBeyondThresholdLevel(this.recoveryDeque.getSecurityValue())) {
						this.triggerState = TriggerState.WAITING;
						Transition transition = new Transition(this.referenceDeque.startIndex, this.referenceDeque.size(), this.thresholdDeque.startIndex, this.thresholdDeque.size(),
								this.recoveryDeque.startIndex, this.recoveryDeque.size(), record, transitionType);
						transitions.put(transition.getThresholdStartTimeStamp_ms(), transition);
						log.log(Level.FINE, GDE.STRING_GREATER, transition);
						log.log(Level.FINER, Integer.toString(transitionType.getTransitionId()), this);
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
						this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
					}
				}
				else if (isPersistentRecovery) { // go on with the recovery
					this.recoveryDeque.addLast(translatedValue, timeStamp_100ns);
				}
				else {
					log.log(Level.FINER, " recovery level not stable ", this); //$NON-NLS-1$
					// try to extend the threshold time
					int removedCount = this.thresholdDeque.tryAddLastByMoving(this.recoveryDeque);
					if (removedCount > 0 && this.recoveryDeque.isEmpty()) {
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%d recovery jitters provoked a fallback %s: translatedValue=%f  thresholdAverage=%f", //$NON-NLS-1$
								transitionType.getTransitionId(), this.thresholdDeque.getFormatedDuration(this.thresholdDeque.size() - 1), translatedValue, this.thresholdDeque.getAverageValue()));
						// all recovery entries including the current entry are now in the threshold phase
						this.triggerState = TriggerState.TRIGGERED;
						// thresholdStartIndex is not modified by jitters;
						this.thresholdDeque.addLast(translatedValue, timeStamp_100ns);
					}
					else {
						// now the threshold time is exceeded and the current value does not fit in the recovery phase
						this.triggerState = TriggerState.WAITING;
						this.referenceDeque.addLastByMoving(this.thresholdDeque);
						this.referenceDeque.addLastByMoving(this.recoveryDeque);
						// referenceStartIndex is not modified by jitters
						this.referenceDeque.addLast(translatedValue, timeStamp_100ns);
					}
				}
				break;
			} // end switch
		} // end for
		// take the current transition even if the recovery time is not exhausted
		if (this.triggerState == TriggerState.RECOVERING) {
			// final check if the majority of the reference and recovery values did not pass the threshold and if only a minimum of the threshold values actually passed the recovery level
			if (!levelChecker.isBeyondThresholdLevel(this.referenceDeque.getSecurityValue()) //
					&& !levelChecker.isBeyondRecoveryLevel(this.thresholdDeque.getSecurityValue()) //
					&& !levelChecker.isBeyondThresholdLevel(this.recoveryDeque.getSecurityValue())) {
				Transition transition = new Transition(this.referenceDeque.startIndex, this.referenceDeque.size(), this.thresholdDeque.startIndex, this.thresholdDeque.size(), this.recoveryDeque.startIndex,
						this.recoveryDeque.size(), record, transitionType);
				transitions.put(transition.getThresholdStartTimeStamp_ms(), transition);
				log.log(Level.FINE, GDE.STRING_GREATER, transition);
			}
		}
		return transitions;
	}

	/**
	 * @param transitionRecord
	 * @param transitionType
	 */
	public void initializeDeques(Record transitionRecord, TransitionType transitionType) {
		final int referenceDequeSize = (int) (transitionType.getReferenceTimeMsec() / this.recordSet.getAverageTimeStep_ms());
		final int thresholdDequeSize = (int) (transitionType.getThresholdTimeMsec() / this.recordSet.getAverageTimeStep_ms());
		final int recoveryDequeSize = (int) (transitionType.getRecoveryTimeMsec().orElseThrow(() -> new UnsupportedOperationException("recovery time. transitionID=" + transitionType.getTransitionId())) //$NON-NLS-1$
				/ this.recordSet.getAverageTimeStep_ms());
		this.referenceDeque = new SettlementDeque(referenceDequeSize, transitionType.isGreater(), transitionType.getReferenceTimeMsec() * 10);
		this.thresholdDeque = new SettlementDeque(thresholdDequeSize, !transitionType.isGreater(), transitionType.getThresholdTimeMsec() * 10);
		this.recoveryDeque = new SettlementDeque(recoveryDequeSize, transitionType.isGreater(), transitionType.getRecoveryTimeMsec().orElse(null) * 10);
		this.referenceDeque.initialize(0);

		int descriptionCutPoint = transitionRecord.getParent().getDescription().indexOf("\r"); //$NON-NLS-1$
		if (descriptionCutPoint < 0) descriptionCutPoint = 11;
		log.log(Level.FINEST, transitionType.getTransitionId() + "  " + transitionRecord.getParent().getName() + " " + " " //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				+ transitionRecord.getParent().getDescription().substring(0, descriptionCutPoint)
				+ String.format(" %s initialized: referenceDequeSize=%d  thresholdDequeSize=%d  recoveryDequeSize=%d", transitionRecord.getName(), referenceDequeSize, thresholdDequeSize, recoveryDequeSize)); //$NON-NLS-1$
	}

}
