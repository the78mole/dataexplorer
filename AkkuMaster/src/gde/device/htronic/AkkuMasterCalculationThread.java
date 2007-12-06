package osde.device.htronic;

import java.util.logging.Level;
import java.util.logging.Logger;

import osde.common.Record;
import osde.common.RecordSet;
import osde.ui.OpenSerialDataExplorer;

/**
 * this class enables data calculation thread
 */
public class AkkuMasterCalculationThread extends Thread {
	private Logger												log	= Logger.getLogger(this.getClass().getName());

	private String												recordKey;
	private RecordSet											recordSet;
	private final OpenSerialDataExplorer	application;

	/**
	 * 
	 */
	public AkkuMasterCalculationThread(String recordKey, RecordSet recordsSet) {
		this.recordKey = recordKey;
		this.recordSet = recordsSet;
		this.application = OpenSerialDataExplorer.getInstance();
	}

	/**
	 * @param name
	 */
	public AkkuMasterCalculationThread(String name, String recordKey, RecordSet recordsSet) {
		super(name);
		this.recordKey = recordKey;
		this.recordSet = recordsSet;
		this.application = OpenSerialDataExplorer.getInstance();
	}

	/**
	 * method which do the calculation
	 */
	public void run() {
		log.fine("start data calculation for record = " + recordKey);
		Record record = recordSet.get(recordKey);
		//		values[5] = new Integer(new Integer(values[2]).intValue() * new Integer(values[3]).intValue()).toString(); // Errechnete Leistung	[mW]
		//		values[6] = new Integer(new Integer(values[2]).intValue() * new Integer(values[4]).intValue()).toString(); // Errechnete Energie	[mWh]
		if (recordKey.equals(RecordSet.POWER)) {
			Record recordVoltage = recordSet.get(RecordSet.VOLTAGE);
			Record recordCurrent = recordSet.get(RecordSet.CURRENT);
			for (int i = 0; i < recordVoltage.size(); i++) {
				record.add(new Double((recordVoltage.get(i) / 1000.0) * (recordCurrent.get(i) / 1000.0) * 1000).intValue());
				if (log.isLoggable(Level.FINEST)) log.finest("adding value = " + record.get(i));
			}
			record.setDisplayable(true);
		}
		else if (recordKey.equals(RecordSet.ENERGY)) {
			Record recordVoltage = recordSet.get(RecordSet.VOLTAGE);
			Record recordCharge = recordSet.get(RecordSet.CHARGE);
			for (int i = 0; i < recordVoltage.size(); i++) {
				record.add(new Double((recordVoltage.get(i) / 1000.0) * (recordCharge.get(i) / 1000.0) * 1000).intValue());
				if (log.isLoggable(Level.FINEST)) log.finest("adding value = " + record.get(i));
			}
			record.setDisplayable(true);
		}
		else
			log.warning("only supported records are " + RecordSet.POWER + ", " + RecordSet.ENERGY);

		//recordSet.updateDataTable();
		application.updateGraphicsWindow();
		log.fine("finished data calculation for record = " + recordKey);
	}
}
