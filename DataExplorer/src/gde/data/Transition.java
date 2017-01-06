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

import java.util.logging.Logger;

import gde.GDE;
import gde.device.TransitionClassTypes;
import gde.device.TransitionType;
import gde.utils.StringHelper;

/**
 * holds the data for the trigger section identified in the data record.
 * the reference, threshold and recovery sections do not overlap.
 * @author Thomas Eickert
 */
public class Transition {
	final static String		$CLASS_NAME	= Transition.class.getName();
	final static Logger		log					= Logger.getLogger($CLASS_NAME);

	final int							referenceSize;
	final int							thresholdSize;
	final int							recoverySize;
	final Record					transitionRecord;
	final TransitionType	transitionType;

	final int							startIndex;
	final int							referenceStartIndex;
	final int							referenceEndIndex;
	final int							thresholdStartIndex;
	final int							thresholdEndIndex;
	final int							recoveryStartIndex;
	final int							recoveryEndIndex;
	//	final int							endIndex;

	private double				referenceExtremumValue;
	private double				thresholdExtremumValue;
	private double				recoveryExtremumValue;

	/**
	 * @param startIndex
	 * @param referenceSize
	 * @param recoveryStartIndex 
	 * @param thresholdSize
	 * @param recoveryStartIndex is less than zero in case of missing recovery phase 
	 * @param recoverySize
	 * @param transitionRecord
	 * @param transitionType
	 */
	public Transition(int startIndex, int referenceSize, int thresholdStartIndex, int thresholdSize, int recoveryStartIndex, int recoverySize, Record transitionRecord, TransitionType transitionType) {
		this.startIndex = startIndex;
		this.referenceStartIndex = thresholdStartIndex - referenceSize;
		this.referenceSize = referenceSize;
		this.referenceEndIndex = thresholdStartIndex - 1;
		this.thresholdStartIndex = thresholdStartIndex;
		this.thresholdSize = thresholdSize;
		this.recoveryStartIndex = recoveryStartIndex;
		this.recoverySize = recoverySize;
		this.transitionRecord = transitionRecord;
		this.transitionType = transitionType;

		if (this.recoveryStartIndex > 0) {
			this.thresholdEndIndex = recoveryStartIndex - 1;
			this.recoveryEndIndex = recoveryStartIndex + recoverySize - 1;
		}
		else {
			this.thresholdEndIndex = -1;
			this.recoveryEndIndex = -1;
		}
	}

	public double getTimeStamp(int index) {
		return this.transitionRecord.getTime_ms(index);
	}

	/**
	 * @return HH:mm:ss.SSS
	 */
	public String getFormatedDuration(int index) {
		return StringHelper.getFormatedDuration("HH:mm:ss.SSS", (long) this.transitionRecord.parent.timeStep_ms.getTime_ms(index)); //$NON-NLS-1$
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.transitionType.getTransitionId()).append(GDE.STRING_BLANK).append(GDE.STRING_BLANK);
		sb.append("threshold=").append(getFormatedDuration(this.thresholdStartIndex)).append(GDE.STRING_OR).append(this.thresholdExtremumValue).append(GDE.STRING_COMMA_BLANK);
		sb.append("isPeak=").append(isPeak()).append(GDE.STRING_COMMA_BLANK).append("isSlope=").append(isSlope()).append(GDE.STRING_COMMA_BLANK);
		sb.append("referenceStartIndex=").append(this.startIndex).append(GDE.STRING_COMMA_BLANK);
		sb.append("reference=").append(getFormatedDuration(this.startIndex)).append(GDE.STRING_OR).append(this.referenceExtremumValue).append(GDE.STRING_COMMA_BLANK);
		if (this.recoveryStartIndex > 0) {
			sb.append(GDE.STRING_COMMA_BLANK);
			sb.append("recovery=").append(getFormatedDuration(this.recoveryStartIndex)).append(GDE.STRING_OR).append(this.recoveryExtremumValue).append(GDE.STRING_COMMA_BLANK);
		}
		return sb.toString();
	}

	public boolean isSlope() {
		return this.transitionType.getClassType() == TransitionClassTypes.SLOPE;
	}

	public boolean isPeak() {
	return this.transitionType.getClassType() == TransitionClassTypes.PEAK;
}

	public boolean isPulse() {
	return this.transitionType.getClassType() == TransitionClassTypes.PULSE;
}

	/**
	 * @return the referenceExtremumValue
	 */
	public double getReferenceExtremumValue() {
		return referenceExtremumValue;
	}

	/**
	 * @param referenceExtremumValue the referenceExtremumValue to set
	 */
	public void setReferenceExtremumValue(double referenceExtremumValue) {
		this.referenceExtremumValue = referenceExtremumValue;
	}

	/**
	 * @return the thresholdExtremumValue
	 */
	public double getThresholdExtremumValue() {
		return thresholdExtremumValue;
	}

	/**
	 * @param thresholdExtremumValue the thresholdExtremumValue to set
	 */
	public void setThresholdExtremumValue(double thresholdExtremumValue) {
		this.thresholdExtremumValue = thresholdExtremumValue;
	}

	/**
	 * @return the recoveryExtremumValue
	 */
	public double getRecoveryExtremumValue() {
		return recoveryExtremumValue;
	}

	/**
	 * @param recoveryExtremumValue the recoveryExtremumValue to set
	 */
	public void setRecoveryExtremumValue(double recoveryExtremumValue) {
		this.recoveryExtremumValue = recoveryExtremumValue;
	}

}
