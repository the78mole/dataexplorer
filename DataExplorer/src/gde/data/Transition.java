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

import gde.GDE;
import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.PropertyType;
import gde.device.SettlementType;
import gde.device.StatisticsType;
import gde.device.TransitionType;

import java.util.ArrayDeque;
import java.util.logging.Logger;

/**
 * holds the data record and the indices for the identified trigger sections.
 * the sections reference, threshold and recovery do not overlap.
 * @author Thomas Eickert
 */
public class Transition {
	final static String	$CLASS_NAME	= Transition.class.getName();
	final static Logger	log			= Logger.getLogger($CLASS_NAME);

	final int				startIndex;
	final int				referenceSize;
	final int				thresholdSize;
	final int				recoverySize;
	final Record			transitionRecord;
	final TransitionType	transitionType;
	
	final int				referenceEndIndex;
	final int				thresholdStartIndex;
	final int				thresholdEndIndex;
	final int				recoveryStartIndex;
	final int				endIndex;

	public Transition(int startIndex, int referenceSize, int thresholdSize, int recoverySize, Record transitionRecord, TransitionType transitionType) {
		this.startIndex = startIndex;
		this.referenceSize = referenceSize;
		this.thresholdSize = thresholdSize;
		this.recoverySize = recoverySize;
		this.transitionRecord = transitionRecord;
		this.transitionType = transitionType;

		this.referenceEndIndex = startIndex + referenceSize - 1;
		this.thresholdStartIndex = startIndex + referenceSize;
		this.thresholdEndIndex = startIndex + referenceSize + thresholdSize - 1;
		this.recoveryStartIndex = startIndex + referenceSize + thresholdSize;
		this.endIndex = startIndex + referenceSize + thresholdSize + recoverySize - 1;
	}

	public double getTimeStamp(int transitionIndex) {
		return this.transitionRecord.getTime_ms(this.startIndex + transitionIndex);
	}


}
