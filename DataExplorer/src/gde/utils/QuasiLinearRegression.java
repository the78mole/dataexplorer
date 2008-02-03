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

import java.util.logging.Level;
import java.util.logging.Logger;

import osde.data.Record;
import osde.data.RecordSet;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.StatusBar;

/**
 * This thread implementation calculates the slop of the height curve using quasi linear regression
 * none critical regarding time interval and no phase shift
 * @author Winfried Brügmann
 */
public class QuasiLinearRegression extends CalculationThread {
	private Logger	log	= Logger.getLogger(this.getClass().getName());

	/**
	 * constructor where calculation required parameters are given as parameter
	 * @param recordSet
	 * @param sourceRecordKey
	 * @param targetRecordKey
	 */
	public QuasiLinearRegression(RecordSet recordSet, String sourceRecordKey, String targetRecordKey, int calcInterval_sec) {
		super(recordSet, sourceRecordKey, targetRecordKey, calcInterval_sec);
	}


	/**
	 * method which do the slope calculation on base of linear regression
	 */
	public void run() {
		if (recordSet == null || sourceRecordKey == null || targetRecordKey == null) {
			log.warning("Die Steigung kann icht berechnet werden -> recordSet == null || sourceRecordKey == null || targetRecordKey == null");
			return;
		}
		log.fine("start data calculation for record = " + targetRecordKey);

		Record record = recordSet.get(targetRecordKey);
		Record recordHeight = recordSet.get(sourceRecordKey);
		StatusBar statusBar = application.getStatusBar();
		statusBar.setMessageAsync(statusMessage);

		int time_ms = recordSet.getTimeStep_ms();
		int interval = calcInterval_sec; // 4000 / time_ms; // 1 sec / 0.05 sec -> 20 points/sec -> 4000/50 -> 8 sec interval time
		int pointInterval = 2;

		int modCounter = ((recordHeight.size() - (recordHeight.size() % interval)) - (interval - pointInterval)) / pointInterval;
		// fill mod interval + pontInterval / 2
		int counter = (recordHeight.size() % interval) + (pointInterval / 2);
		int padding = interval / 2 - pointInterval;
		// padding data points which does not fit into interval
		for (int i = 0; i < counter+padding; i++) { // 0,5 sec
			record.add(0);
		}
		// prepare progress status
		int progInterval = maxCalcProgressPercent / modCounter;

		// calculate avg x
		double avgX = 0; //(interval-1) * time_ms / 1000.0 / interval; // 9 * 0.05 / 10; --> 0,05 
		for (int i = 0; i < interval; i++) {
			avgX = avgX + (1 / 0.05 * i);
		}
		avgX = avgX / interval;

		// (xi - avgX)*(xi - avgX)
		double ssXX = 0.0; // 10 sec = 0.053025;
		for (int i = 0; i < interval; i++) { // 0,5 sec
			ssXX = ssXX + (((1 / 0.05 * i) - avgX) * ((1 / 0.05 * i) - avgX));
		}
		ssXX = ssXX / interval;
		if (log.isLoggable(Level.FINEST)) log.finest("avgX = " + avgX + " ssXX = " + ssXX);

		--modCounter;
		while (modCounter > 0 && !threadStop) {
			// calculate avg y
			double avgY = 0.0;
			for (int i = 0; i < interval; i++) { // 0,5 sec
				avgY = avgY + (recordHeight.get(i + counter));
			}
			avgY = avgY / interval;

			// (yi - avgY)
			double sumYi_avgY = 0.0;
			for (int i = 0; i < interval; i++) { // 0,5 sec
				sumYi_avgY = sumYi_avgY + ((recordHeight.get(i + counter)) - avgY);
			}
			sumYi_avgY = sumYi_avgY / interval;

			// (xi - avgX)*(yi - avgY)
			double ssXY = 0.0;
			for (int i = 0; i < interval; i++) { // 0,5 sec
				ssXY = ssXY + (((1 / 0.05 * i) - avgX) * ((recordHeight.get(i + counter)) - avgY));
			}
			ssXY = ssXY / interval;

			int slope = 0;
			// ad point over pointInterval only
			for (int i = 0; i < pointInterval; i++) { // 0,5 sec
				slope = new Double(ssXY / ssXX / 2).intValue(); // slope = ssXY / ssXX / 2; 2*0.5 = 1
				record.add(slope * 1000);
			}
			counter = counter + pointInterval;

			if (log.isLoggable(Level.FINEST)) log.finest("slope = " + slope + " counter = " + counter + " modCounter = " + modCounter);
			--modCounter;
			statusBar.setProgressAsync(maxCalcProgressPercent - modCounter * progInterval);
		}
		// pad the rest of the curve to make equal size
		for (int i = record.size()-1; i < recordHeight.size(); i++) {
			record.add(0);
		}
		if (log.isLoggable(Level.FINEST)) log.fine("counter = " + counter + " modCounter = " + modCounter);

		record.setDisplayable(true);
		statusBar.setProgressAsync(maxCalcProgressPercent);
		statusBar.setMessageAsync("");

		OpenSerialDataExplorer.getInstance().updateCurveSelectorTable();
		log.fine("finished data calculation for record = " + targetRecordKey);
	}

}
