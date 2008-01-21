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
package osde.device.renschler;

import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.CalculationThread;
import osde.utils.QuasiLinearRegression;

/**
 * Thread implementation to gather data from Picolariolog device
 * @author Winfied Brügmann
 */
public class DataGathererThread extends Thread {
	private Logger									log							= Logger.getLogger(this.getClass().getName());

	private OpenSerialDataExplorer		application;
	private String[]									datagramNumbers;
	private final String							RECORD_SET_NAME	= ") Flugaufzeichnung";
	private final PicolarioSerialPort	serialPort;
	private final PicolarioDialog			dialog;
	private final Picolario						device;
	private CalculationThread					calculationThread;
	private boolean										threadStop			= false;

	/**
	 * 
	 */
	public DataGathererThread(OpenSerialDataExplorer application, Picolario device, PicolarioSerialPort serialPort, String[] datagramNumbers) {
		this.application = application;
		this.device = device;
		this.serialPort = serialPort;
		this.dialog = device.getDialog();
		this.datagramNumbers = datagramNumbers;
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
			Channel channel = Channels.getInstance().getActiveChannel();
			String[] measurements = device.getMeasurementNames(); // 0=Spannung, 1=Höhe, 2=Steigrate
			String recordSetKey;

			dialog.resetDataSetsLabel();
			for (int j = 0; j < datagramNumbers.length && !threadStop; ++j) {
				dialog.resetTelegramLabel();
				dialog.setAlreadyRedDataSets(datagramNumbers[j]);
				HashMap<String, Object> data = serialPort.getData(null, new Integer(datagramNumbers[j]).intValue(), device);
				recordSetKey = (channel.size() + 1) + RECORD_SET_NAME;
				channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, application.getActiveDevice(), true, false));
				log.fine(recordSetKey + " created");
				if (channel.getActiveRecordSet() == null) Channels.getInstance().getActiveChannel().setActiveRecordSet(recordSetKey);
				RecordSet recordSet = channel.get(recordSetKey);
				Vector<Integer> voltage = (Vector<Integer>)data.get(measurements[0]); // 0=Spannung
				Vector<Integer> height = (Vector<Integer>)data.get(measurements[1]);	// 1=Höhe

				for (int i = 0; i < height.size(); i++) {
					int[] points = new int[recordSet.size() - 1];
					points[0] = voltage.get(i).intValue(); // Spannung, wie ausgelesen						
					points[1] = height.get(i).intValue(); // Höhe, wie ausgelesen
					//points[2] = 0; // Steigrate -> isCalculation

					recordSet.addPoints(points, false);
				}
				// start slope calculation
				calculationThread = new QuasiLinearRegression(recordSet, measurements[1], measurements[2]);
				calculationThread.start();

				application.getMenuToolBar().addRecordSetName(recordSetKey);
				if (channel.getRecordSetNames().length <= 1 || dialog.isDoSwtichRecordSet())
					channel.getActiveRecordSet().switchRecordSet(recordSetKey);

				// update the progress bar reading one after the other only
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
		this.serialPort.setTransmitFinished(true);
	}

}
