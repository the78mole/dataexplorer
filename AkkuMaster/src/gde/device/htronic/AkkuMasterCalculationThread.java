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
	final static Logger						log	= Logger.getLogger(AkkuMasterCalculationThread.class.getName());

	String												recordKey;
	RecordSet											recordSet;
	final OpenSerialDataExplorer	application;

	/**
	 * constructor using the recordKey and recordSet for initialization
	 * @param useRecordKey as String
	 * @param useRecordSet as RecordSet
	 */
	public AkkuMasterCalculationThread(String useRecordKey, RecordSet useRecordSet) {
		this.recordKey = useRecordKey;
		this.recordSet = useRecordSet;
		this.application = OpenSerialDataExplorer.getInstance();
	}

	/**
	 * constructor using the recordKey and recordSet for initialization, a thread name can be given
	 * @param name
	 * @param useRecordKey as String
	 * @param useRecordSet as RecordSet
	 */
	public AkkuMasterCalculationThread(String name, String useRecordKey, RecordSet useRecordSet) {
		super(name);
		this.recordKey = useRecordKey;
		this.recordSet = useRecordSet;
		this.application = OpenSerialDataExplorer.getInstance();
	}

	/**
	 * method which do the calculation
	 */
	@Override
	public void run() {
		AkkuMasterCalculationThread.log.fine("start data calculation for record = " + this.recordKey);
		Record record = this.recordSet.get(this.recordKey);
		String[] measurements = this.recordSet.getDevice().getMeasurementNames(this.recordSet.getChannelName()); // 0=Spannung, 1=Strom, 2=Ladung, 3=Leistung, 4=Energie
		//		values[5] = new Integer(new Integer(values[2]).intValue() * new Integer(values[3]).intValue()).toString(); // Errechnete Leistung	[mW]
		//		values[6] = new Integer(new Integer(values[2]).intValue() * new Integer(values[4]).intValue()).toString(); // Errechnete Energie	[mWh]
		if (this.recordKey.equals(measurements[3])) { // 3=Leistung
			Record recordVoltage = this.recordSet.get(measurements[0]); // 0=Spannung
			Record recordCurrent = this.recordSet.get(measurements[1]); // 1=Strom
			for (int i = 0; i < recordVoltage.size(); i++) {
				record.add(new Double((recordVoltage.get(i) / 1000.0) * (recordCurrent.get(i) / 1000.0) * 1000).intValue());
				if (AkkuMasterCalculationThread.log.isLoggable(Level.FINEST)) AkkuMasterCalculationThread.log.finest("adding value = " + record.get(i));
			}
			record.setDisplayable(true);
		}
		else if (this.recordKey.equals(measurements[4])) { // 4=Energie
			Record recordVoltage = this.recordSet.get(measurements[0]); // 0=Spannung
			Record recordCharge = this.recordSet.get(measurements[2]); // 2=Ladung
			for (int i = 0; i < recordVoltage.size(); i++) {
				record.add(new Double((recordVoltage.get(i) / 1000.0) * (recordCharge.get(i) / 1000.0) * 1000.0).intValue());
				if (AkkuMasterCalculationThread.log.isLoggable(Level.FINEST)) AkkuMasterCalculationThread.log.finest("adding value = " + record.get(i));
			}
			record.setDisplayable(true);
		}
		else
			AkkuMasterCalculationThread.log.warning("only supported records are " + measurements[3] + ", " + measurements[4]);

		//recordSet.updateDataTable();
		this.application.updateGraphicsWindow();
		AkkuMasterCalculationThread.log.fine("finished data calculation for record = " + this.recordKey);
	}
}
