package osde.utils;

import java.util.logging.Logger;

import osde.data.RecordSet;
import osde.device.DeviceDialog;
import osde.ui.OpenSerialDataExplorer;

/**
 * this abstract thread implementation calculates the slop of the height curve using several algorithm
 */
public abstract class CalculationThread extends Thread {
	protected Logger												log					= Logger.getLogger(this.getClass().getName());

	protected static String									newLine			= System.getProperty("line.separator");

	protected DeviceDialog			dialog;
	protected RecordSet											recordSet;
	protected final OpenSerialDataExplorer	apllication;
	protected boolean												threadStop	= false;

	/**
	 * 
	 */
	public CalculationThread(DeviceDialog dialog) {
		this.dialog = dialog;
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
}
