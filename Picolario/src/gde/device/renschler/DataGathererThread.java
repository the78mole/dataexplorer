package osde.device.renschler;

import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import osde.common.Channel;
import osde.common.Channels;
import osde.common.RecordSet;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.StatusBar;
import osde.utils.CalculationThread;
import osde.utils.QuasiLinearRegression;

/**
 *
 */
public class DataGathererThread extends Thread {
	private Logger									log							= Logger.getLogger(this.getClass().getName());

	private OpenSerialDataExplorer	application;
	private String[]								datagramNumbers;
	private final String						RECORD_SET_NAME	= ") Flugaufzeichnung";
	private PicolarioSerialPort			serialPort;
	private PicolarioDialog					dialog;
	private CalculationThread				calculationThread;
	private boolean									threadStop			= false;

	/**
	 * 
	 */
	public DataGathererThread(OpenSerialDataExplorer application, PicolarioDialog dialog, PicolarioSerialPort serialPort, String[] datagramNumbers) {
		this.application = application;
		this.dialog = dialog;
		this.serialPort = serialPort;
		this.datagramNumbers = datagramNumbers;
		this.calculationThread = new QuasiLinearRegression(dialog);
	}

	/**
	 * method implements the data gathering from device and load data into the records
	 * if more than one available record set is created the status bar shows the status how many record sets are done
	 * this gives not a real feeling since the record sets may have big differences in number of available telegrams
	 */
	@SuppressWarnings("unchecked") // cast from Object to Vector<Integer>
	public void run() {
		try {
			log.fine("entry data gatherer");
			StatusBar statusBar = application.getStatusBar();
			int progressBarMacroSteps = 100 / datagramNumbers.length;
			statusBar.updateProgressbar(0);
			Channel channel = Channels.getInstance().getActiveChannel();
			String recordSetKey;

			for (int j = 0; j < datagramNumbers.length && !threadStop; ++j) {
				dialog.resetTelegramLabel();
				HashMap<String, Object> data = serialPort.getData(null, new Integer(datagramNumbers[j]).intValue(), dialog);
				recordSetKey = (channel.size() + 1) + RECORD_SET_NAME;
				channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, application.getActiveConfig(), true, false));
				log.fine(recordSetKey + " created");
				if (channel.getActiveRecordSet() == null) Channels.getInstance().getActiveChannel().setActiveRecordSet(recordSetKey);
				RecordSet recordSet = channel.get(recordSetKey);
				Vector<Integer> height = (Vector<Integer>)data.get(PicolarioSerialPort.HEIGHT);
				Vector<Integer> voltage = (Vector<Integer>)data.get(PicolarioSerialPort.VOLTAGE);

				for (int i = 0; i < height.size(); i++) {
					int[] points = new int[recordSet.size() - 1];
					points[0] = voltage.get(i).intValue(); // Spannung, wie ausgelesen						
					points[1] = height.get(i).intValue(); // Höhe, wie ausgelesen
					//points[2] = 0; // Steigrate

					recordSet.addPoints(points, false);
				}
				// start slope calculation
				calculationThread.setRecordSet(channel.get(recordSetKey));
				calculationThread.start();

				if (channel.getName().equals(Channels.getInstance().getActiveChannel().getName())) {
					application.getDataToolBar().addRecordSetName(recordSetKey);
					channel.getActiveRecordSet().switchRecordSet(recordSetKey);
				}

				// update the progress bar reading one after the other only
				statusBar.updateProgressbar(progressBarMacroSteps * (j + 1));
				channel.get(recordSetKey).setAllDisplayable();
				channel.applyTemplate(recordSetKey);
			}// end for
			dialog.enableReadButtons();
			log.fine("exit data gatherer");

		}
		catch (Exception e) {
			e.printStackTrace();
			application.openMessageDialog("Das angeschlossene Gerät antwortet nich auf dem seriellen Port");
		}
	} // end of run()

	public void setThreadStop(boolean threadStop) {
		this.threadStop = threadStop;
	}

}
