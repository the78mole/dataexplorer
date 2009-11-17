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
package osde.device.bantam;

import java.util.logging.Level;
import java.util.logging.Logger;

import osde.data.Record;
import osde.data.RecordSet;
import osde.ui.OpenSerialDataExplorer;

/**
 * This class enables data calculation thread for device AkkuMaster C4
 * @author Winfried Brügmann
 */
public class CalculationThread extends Thread {
	final static Logger						log	= Logger.getLogger(CalculationThread.class.getName());

	String												recordKey;
	RecordSet											recordSet;
	final OpenSerialDataExplorer	application;

	/**
	 * constructor using the recordKey and recordSet for initialization
	 * @param useRecordKey as String
	 * @param useRecordSet as RecordSet
	 */
	public CalculationThread(String useRecordKey, RecordSet useRecordSet) {
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
	public CalculationThread(String name, String useRecordKey, RecordSet useRecordSet) {
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
		log.log(Level.FINE, "start data calculation for record = " + this.recordKey); //$NON-NLS-1$
		Record record = this.recordSet.get(this.recordKey);
		// 0=Spannung, 1=Strom, 2=Ladung, 3=Leistung, 4=Energie
		String[] recordNames = this.recordSet.getRecordNames();
		if (this.recordKey.equals(recordNames[3])) { // 3=Leistung P[W]=U[V]*I[A]
			Record recordVoltage = this.recordSet.get(recordNames[0]); // 0=Spannung
			Record recordCurrent = this.recordSet.get(recordNames[1]); // 1=Strom
			record.clear();
			for (int i = 0; i < recordVoltage.size(); i++) {
				record.add(Double.valueOf((recordVoltage.get(i) / 1000.0) * (recordCurrent.get(i) / 1000.0) * 1000).intValue());
				log.log(Level.FINEST, "adding value = " + record.get(i)); //$NON-NLS-1$
			}
			record.setDisplayable(true);
		}
		else if (this.recordKey.equals(recordNames[4])) { // 4=Energie E[Wh]=U[V]*I[A]*t[h]=U[V]*C[Ah]
			Record recordVoltage = this.recordSet.get(recordNames[0]); // 0=Spannung
			Record recordCharge = this.recordSet.get(recordNames[2]);  // 2=Ladung
			record.clear();
			for (int i = 0; i < recordVoltage.size(); i++) {
				record.add(Double.valueOf((recordVoltage.get(i) / 1000.0) * (recordCharge.get(i) / 1000.0)).intValue());
				log.log(Level.FINEST, "adding value = " + record.get(i)); //$NON-NLS-1$
			}
			record.setDisplayable(true);
		}
		else
			log.log(Level.WARNING, "only supported records are " + recordNames[3] + ", " + recordNames[4]); //$NON-NLS-1$ //$NON-NLS-2$

		//recordSet.updateDataTable();
		if (record.isVisible()) this.application.updateGraphicsWindow();
		log.log(Level.FINE, "finished data calculation for record = " + this.recordKey); //$NON-NLS-1$
	}
}
