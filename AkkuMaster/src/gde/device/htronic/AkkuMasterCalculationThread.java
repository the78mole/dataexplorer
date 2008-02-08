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
package osde.device.htronic;

import java.util.logging.Level;
import java.util.logging.Logger;

import osde.data.Record;
import osde.data.RecordSet;
import osde.ui.OpenSerialDataExplorer;

/**
 * This class enables data calculation thread for device AkkuMaster C4
 * @author Winfried Brügmann
 */
public class AkkuMasterCalculationThread extends Thread {
	private Logger												log	= Logger.getLogger(this.getClass().getName());

	private String												recordKey;
	private RecordSet											recordSet;
	private final OpenSerialDataExplorer	application;

	/**
	 * constructor using the recordKey and recordSet for initialization
	 * @param recordKey as String
	 * @param recordSet as RecordSet
	 */
	public AkkuMasterCalculationThread(String recordKey, RecordSet recordSet) {
		this.recordKey = recordKey;
		this.recordSet = recordSet;
		this.application = OpenSerialDataExplorer.getInstance();
	}

	/**
	 * constructor using the recordKey and recordSet for initialization, a thread name can be given
	 * @param name
	 * @param recordKey as String
	 * @param recordSet as RecordSet
	 */
	public AkkuMasterCalculationThread(String name, String recordKey, RecordSet recordSet) {
		super(name);
		this.recordKey = recordKey;
		this.recordSet = recordSet;
		this.application = OpenSerialDataExplorer.getInstance();
	}

	/**
	 * method which do the calculation
	 */
	public void run() {
		log.fine("start data calculation for record = " + recordKey);
		Record record = recordSet.get(recordKey);
		String[] measurements = record.getDevice().getMeasurementNames(recordSet.getChannelName()); // 0=Spannung, 1=Strom, 2=Ladung, 3=Leistung, 4=Energie
		//		values[5] = new Integer(new Integer(values[2]).intValue() * new Integer(values[3]).intValue()).toString(); // Errechnete Leistung	[mW]
		//		values[6] = new Integer(new Integer(values[2]).intValue() * new Integer(values[4]).intValue()).toString(); // Errechnete Energie	[mWh]
		if (recordKey.equals(measurements[3])) {									// 3=Leistung
			Record recordVoltage = recordSet.get(measurements[0]);	// 0=Spannung
			Record recordCurrent = recordSet.get(measurements[1]);	// 1=Strom
			for (int i = 0; i < recordVoltage.size(); i++) {
				record.add(new Double((recordVoltage.get(i) / 1000.0) * (recordCurrent.get(i) / 1000.0) * 1000).intValue());
				if (log.isLoggable(Level.FINEST)) log.finest("adding value = " + record.get(i));
			}
			record.setDisplayable(true);
		}
		else if (recordKey.equals(measurements[4])) {							// 4=Energie
			Record recordVoltage = recordSet.get(measurements[0]);	// 0=Spannung
			Record recordCharge = recordSet.get(measurements[2]);		// 2=Ladung
			for (int i = 0; i < recordVoltage.size(); i++) {
				record.add(new Double((recordVoltage.get(i) / 1000.0) * (recordCharge.get(i) / 1000.0) * 1000).intValue());
				if (log.isLoggable(Level.FINEST)) log.finest("adding value = " + record.get(i));
			}
			record.setDisplayable(true);
		}
		else
			log.warning("only supported records are " + measurements[3] + ", " + measurements[4]);

		//recordSet.updateDataTable();
		application.updateGraphicsWindow();
		log.fine("finished data calculation for record = " + recordKey);
	}
}
