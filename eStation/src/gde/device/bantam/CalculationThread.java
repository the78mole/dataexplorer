/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020 Winfried Bruegmann
****************************************************************************************/
package gde.device.bantam;

import gde.log.Level;
import java.util.logging.Logger;

import gde.data.Record;
import gde.data.RecordSet;
import gde.ui.DataExplorer;

/**
 * This class enables data calculation thread for device AkkuMaster C4
 * @author Winfried Brügmann
 */
public class CalculationThread extends Thread {
	final static Logger						log	= Logger.getLogger(CalculationThread.class.getName());

	String												recordKey;
	RecordSet											recordSet;
	final DataExplorer	application;

	/**
	 * constructor using the recordKey and recordSet for initialization
	 * @param useRecordKey as String
	 * @param useRecordSet as RecordSet
	 */
	public CalculationThread(String useRecordKey, RecordSet useRecordSet) {
		super("calculation");
		this.recordKey = useRecordKey;
		this.recordSet = useRecordSet;
		this.application = DataExplorer.getInstance();
	}

	/**
	 * method which do the calculation
	 */
	@Override
	public void run() {
		log.log(Level.FINE, "start data calculation for record = " + this.recordKey); //$NON-NLS-1$
		Record record = this.recordSet.get(this.recordKey);
		// 0=Spannung, 1=Strom, 2=Ladung, 3=Leistung, 4=Energie
		if (this.recordKey.equals(3)) { // 3=Leistung P[W]=U[V]*I[A]
			Record recordVoltage = this.recordSet.get(0); // 0=Spannung
			Record recordCurrent = this.recordSet.get(1); // 1=Strom
			record.clear();
			for (int i = 0; i < recordVoltage.size(); i++) {
				record.add(Double.valueOf((recordVoltage.realGet(i) / 1000.0) * (recordCurrent.realGet(i) / 1000.0) * 1000).intValue());
				log.log(Level.FINEST, "adding value = " + record.realGet(i)); //$NON-NLS-1$
			}
			record.setDisplayable(true);
		}
		else if (this.recordKey.equals(4)) { // 4=Energie E[Wh]=U[V]*I[A]*t[h]=U[V]*C[Ah]
			Record recordVoltage = this.recordSet.get(0); // 0=Spannung
			Record recordCharge = this.recordSet.get(2);  // 2=Ladung
			record.clear();
			for (int i = 0; i < recordVoltage.size(); i++) {
				record.add(Double.valueOf((recordVoltage.realGet(i) / 1000.0) * (recordCharge.realGet(i) / 1000.0)).intValue());
				log.log(Level.FINEST, "adding value = " + record.realGet(i)); //$NON-NLS-1$
			}
			record.setDisplayable(true);
		}
		else
			log.log(Level.WARNING, "only supported records are " + this.recordSet.get(3).getName() + ", " + this.recordSet.get(4).getName()); //$NON-NLS-1$ //$NON-NLS-2$

		//recordSet.updateDataTable();
		if (record.isVisible()) this.application.updateGraphicsWindow();
		log.log(Level.FINE, "finished data calculation for record = " + this.recordKey); //$NON-NLS-1$
	}
}
