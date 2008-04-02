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
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.PropertyType;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.CalculationThread;
import osde.utils.QuasiLinearRegression;

/**
 * Thread implementation to gather data from Picolariolog device
 * @author Winfied Brügmann
 */
public class DataGathererThread extends Thread {
	final static Logger				log							= Logger.getLogger(DataGathererThread.class.getName());

	OpenSerialDataExplorer		application;
	String[]									datagramNumbers;
	final String							RECORD_SET_NAME	= ") Flugaufzeichnung";
	final String							configKey;
	final PicolarioSerialPort	serialPort;
	final PicolarioDialog			dialog;
	final Picolario						device;
	CalculationThread					calculationThread;
	boolean										threadStop			= false;

	/**
	 * 
	 */
	public DataGathererThread(OpenSerialDataExplorer currentApplication, Picolario currentDevice, PicolarioSerialPort currentSerialPort, String[] useDatagramNumbers) {
		this.application = currentApplication;
		this.device = currentDevice;
		this.serialPort = currentSerialPort;
		this.dialog = currentDevice.getDialog();
		this.datagramNumbers = useDatagramNumbers;
		this.configKey = currentDevice.getChannelName(1);
	}

	/**
	 * method implements the data gathering from device and load data into the records
	 * if more than one available record set is created the status bar shows the status how many record sets are done
	 * this gives not a real feeling since the record sets may have big differences in number of available telegrams
	 */
	@SuppressWarnings("unchecked") // cast from Object to Vector<Integer>
	public void run() {
		boolean isPortOpenedByMe = false;
		try {
			DataGathererThread.log.fine("entry data gatherer");
			Channel channel = Channels.getInstance().getActiveChannel();
			String[] measurements = this.device.getMeasurementNames(channel.getConfigKey()); // 0=Spannung, 1=Höhe, 2=Steigrate
			String recordSetKey;

			this.dialog.resetDataSetsLabel();
			if (!this.serialPort.isConnected()) {
				this.serialPort.open();
				isPortOpenedByMe = true;
			}

			for (int j = 0; j < this.datagramNumbers.length && !this.threadStop; ++j) {
				this.dialog.resetTelegramLabel();
				this.dialog.setAlreadyRedDataSets(this.datagramNumbers[j]);
				HashMap<String, Object> data = this.serialPort.getData(new Integer(this.datagramNumbers[j]).intValue(), this.device, this.configKey);
				recordSetKey = (channel.size() + 1) + this.RECORD_SET_NAME;
				channel.put(recordSetKey, RecordSet.createRecordSet(this.configKey, recordSetKey, this.application.getActiveDevice(), true, false));
				DataGathererThread.log.fine(recordSetKey + " created");
				if (channel.getActiveRecordSet() == null) Channels.getInstance().getActiveChannel().setActiveRecordSet(recordSetKey);
				RecordSet recordSet = channel.get(recordSetKey);
				Vector<Integer> voltage = (Vector<Integer>) data.get(measurements[0]); // 0=Spannung
				Vector<Integer> height = (Vector<Integer>) data.get(measurements[1]); // 1=Höhe

				for (int i = 0; i < height.size(); i++) {
					int[] points = new int[recordSet.size() - 1];
					points[0] = voltage.get(i).intValue(); // Spannung, wie ausgelesen						
					points[1] = height.get(i).intValue(); // Höhe, wie ausgelesen
					//points[2] = 0; // Steigrate -> isCalculation

					recordSet.addPoints(points, false);
				}
				// start slope calculation
				PropertyType property = this.device.getMeasruementProperty(this.configKey, measurements[2], CalculationThread.REGRESSION_INTERVAL_SEC);
				int regressionInterval = property != null ? new Integer(property.getValue()) : 4;
				this.calculationThread = new QuasiLinearRegression(recordSet, measurements[1], measurements[2], regressionInterval);
				this.calculationThread.start();

				this.application.getMenuToolBar().addRecordSetName(recordSetKey);
				if (channel.getRecordSetNames().length <= 1 || this.dialog.isDoSwtichRecordSet()) channel.switchRecordSet(recordSetKey);

				// update the progress bar reading one after the other only
				channel.get(recordSetKey).setAllDisplayable();
			}// end for
			this.dialog.enableReadButtons();
			DataGathererThread.log.fine("exit data gatherer");

		}
		catch (Exception e) {
			DataGathererThread.log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog("Bei der seriellen Kommunikation mit dem angeschlossene Gerät gibt es Fehler !");
		}
		finally {
			if (isPortOpenedByMe) this.serialPort.close();
		}
	} // end of run()

	public void setThreadStop(boolean enable) {
		this.threadStop = enable;
		this.serialPort.setTransmitFinished(true);
	}

}
