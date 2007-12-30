/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
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
	protected Logger												log					= Logger.getLogger(this.getClass().getName());

	protected static String									newLine			= System.getProperty("line.separator");

	protected RecordSet											recordSet;
	protected String												sourceRecordKey, targetRecordKey;
	protected final OpenSerialDataExplorer	apllication;
	protected boolean												threadStop	= false;

	/**
	 * constructor where calculation required parameters are given as parameter
	 * @param recordSet
	 * @param sourceRecordKey
	 * @param targetRecordKey
	 */
	public CalculationThread(RecordSet recordSet, String sourceRecordKey, String targetRecordKey) {
		super();
		this.recordSet = recordSet;
		this.sourceRecordKey = sourceRecordKey;
		this.targetRecordKey = targetRecordKey;
		this.apllication = OpenSerialDataExplorer.getInstance();
	}

	/**
	 * method which do the calculation
	 */
	public abstract void run();

	/**
	 * @param recordSet the recordSet to set
	 */
	public void setRecordSet(RecordSet recordSet) {
		this.recordSet = recordSet;
	}

	public void setThreadStop(boolean threadStop) {
		this.threadStop = threadStop;
	}

	/**
	 * @param sourceRecordKey the sourceRecordKey to set
	 */
	public void setSourceRecordKey(String sourceRecordKey) {
		this.sourceRecordKey = sourceRecordKey;
	}

	/**
	 * @param targetRecordKey the targetRecordKey to set
	 */
	public void setTargetRecordKey(String targetRecordKey) {
		this.targetRecordKey = targetRecordKey;
	}
}
