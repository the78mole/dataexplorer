/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.utils;

import java.util.logging.Logger;

import osde.data.RecordSet;
import osde.ui.OpenSerialDataExplorer;

/**
 * This abstract thread implementation calculates the slop of the height curve using several algorithm
 * @author Winfried Brügmann
 */
public abstract class CalculationThread extends Thread {
	private final static Logger											log					= Logger.getLogger(CalculationThread.class.getName());

	public static final String REGRESSION_TYPE 					= "regression_type"; //$NON-NLS-1$
	public static final String REGRESSION_TYPE_LINEAR 	= "regression_type_linear"; //$NON-NLS-1$
	public static final String REGRESSION_TYPE_CURVE 		= "regression_type_curve"; //$NON-NLS-1$
	public static final String REGRESSION_INTERVAL_SEC 	= "regression_interval_sec"; //$NON-NLS-1$

	protected RecordSet											recordSet;
	protected String												sourceRecordKey, targetRecordKey;
	protected int														calcInterval_sec = 10;
	protected final OpenSerialDataExplorer	application;
	protected boolean												threadStop	= false;

	/**
	 * constructor where calculation required parameters are given as parameter
	 * @param currentRecordSet
	 * @param inRecordKey
	 * @param outRecordKey
	 * @param calcIntervalSec
	 */
	public CalculationThread(RecordSet currentRecordSet, String inRecordKey, String outRecordKey, int calcIntervalSec) {
		super();
		this.recordSet = currentRecordSet;
		this.sourceRecordKey = inRecordKey;
		this.targetRecordKey = outRecordKey;
		this.calcInterval_sec = calcIntervalSec;
		this.application = OpenSerialDataExplorer.getInstance();
		log.finer(this.getClass().getSimpleName() + " instanciated");
	}

	/**
	 * method which do the calculation
	 */
	public abstract void run();

	/**
	 * @param newRecordSet the recordSet to set
	 */
	public void setRecordSet(RecordSet newRecordSet) {
		this.recordSet = newRecordSet;
	}

	public void setThreadStop(boolean doStop) {
		this.threadStop = doStop;
	}

	/**
	 * @param inRecordKey the sourceRecordKey to set
	 */
	public void setSourceRecordKey(String inRecordKey) {
		this.sourceRecordKey = inRecordKey;
	}

	/**
	 * @param outRecordKey the targetRecordKey to set
	 */
	public void setTargetRecordKey(String outRecordKey) {
		this.targetRecordKey = outRecordKey;
	}

	/**
	 * @return the calcInterval_sec
	 */
	public int getCalcInterval_sec() {
		return this.calcInterval_sec;
	}

	/**
	 * @param calcIntervalSec the calcInterval_sec to set
	 */
	public void setCalcInterval_sec(int calcIntervalSec) {
		this.calcInterval_sec = calcIntervalSec;
	}
}
