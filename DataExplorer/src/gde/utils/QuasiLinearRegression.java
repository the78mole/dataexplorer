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
package osde.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.ui.OpenSerialDataExplorer;

/**
 * This thread implementation calculates the slop of the height curve using quasi linear regression
 * none critical regarding time interval and no phase shift
 * @author Winfried Brügmann
 */
public class QuasiLinearRegression extends CalculationThread {
	final static Logger	logQl	= Logger.getLogger(QuasiLinearRegression.class.getName());

	/**
	 * constructor where calculation required parameters are given as parameter
	 * @param currentRecordSet
	 * @param inRecordKey
	 * @param outRecordKey
	 * @param calcIntervalSec
	 */
	public QuasiLinearRegression(RecordSet currentRecordSet, String inRecordKey, String outRecordKey, int calcIntervalSec) {
		super(currentRecordSet, inRecordKey, outRecordKey, calcIntervalSec);
	}

	/**
	 * method which do the slope calculation on base of linear regression
	 */
	@Override
	public void run() {
		if (this.recordSet == null || this.sourceRecordKey == null || this.targetRecordKey == null) {
			QuasiLinearRegression.logQl.warning("Die Steigung kann nicht berechnet werden -> recordSet == null || sourceRecordKey == null || targetRecordKey == null");
			return;
		}
		synchronized (REGRESSION_INTERVAL_SEC) {
			QuasiLinearRegression.logQl.fine("start data calculation for record = " + this.targetRecordKey);

			Record record = this.recordSet.get(this.targetRecordKey);
			record.clear();
			Record recordHeight = this.recordSet.get(this.sourceRecordKey);
			double time_ms = this.recordSet.getTimeStep_ms();
			int pointsPerInterval = new Double(this.calcInterval_sec * 1000.0 / time_ms).intValue(); // 4000ms/50ms/point -> 80 points per interval
			int pointInterval = 2;
			int modCounter = ((recordHeight.size() - (recordHeight.size() % pointsPerInterval)) - (pointsPerInterval - pointInterval)) / pointInterval;
			// fill mod interval + pontInterval / 2
			int counter = (recordHeight.size() % pointsPerInterval) + (pointInterval / 2);
			int padding = pointsPerInterval / 2 - pointInterval;
			// padding data points which does not fit into interval
			for (int i = 0; i < counter + padding; i++) { // 0,5 sec
				record.add(0);
			}
			// calculate avg x
			double avgX = 0; //(interval-1) * time_ms / 1000.0 / interval; // 9 * 0.05 / 10; --> 0,05 
			for (int i = 0; i < pointsPerInterval; i++) {
				avgX = avgX + (1 / 0.05 * i);
			}
			avgX = avgX / pointsPerInterval;
			// (xi - avgX)*(xi - avgX)
			double ssXX = 0.0; // 10 sec = 0.053025;
			for (int i = 0; i < pointsPerInterval; i++) { // 0,5 sec
				ssXX = ssXX + (((1 / 0.05 * i) - avgX) * ((1 / 0.05 * i) - avgX));
			}
			ssXX = ssXX / pointsPerInterval;
			if (QuasiLinearRegression.logQl.isLoggable(Level.FINEST)) QuasiLinearRegression.logQl.finest("avgX = " + avgX + " ssXX = " + ssXX);
			--modCounter;
			while (modCounter > 0 && !this.threadStop) {
				// calculate avg y
				double avgY = 0.0;
				for (int i = 0; i < pointsPerInterval; i++) { // 0,5 sec
					avgY = avgY + (recordHeight.get(i + counter));
				}
				avgY = avgY / pointsPerInterval;

				// (yi - avgY)
				double sumYi_avgY = 0.0;
				for (int i = 0; i < pointsPerInterval; i++) { // 0,5 sec
					sumYi_avgY = sumYi_avgY + ((recordHeight.get(i + counter)) - avgY);
				}
				sumYi_avgY = sumYi_avgY / pointsPerInterval;

				// (xi - avgX)*(yi - avgY)
				double ssXY = 0.0;
				for (int i = 0; i < pointsPerInterval; i++) { // 0,5 sec
					ssXY = ssXY + (((1 / 0.05 * i) - avgX) * ((recordHeight.get(i + counter)) - avgY));
				}
				ssXY = ssXY / pointsPerInterval;

				int slope = 0;
				// ad point over pointInterval only
				for (int i = 0; i < pointInterval; i++) { // 0,5 sec
					slope = new Double(ssXY / ssXX / 2).intValue(); // slope = ssXY / ssXX / 2; 2*0.5 = 1
					record.add(slope * 1000);
				}
				counter = counter + pointInterval;

				if (QuasiLinearRegression.logQl.isLoggable(Level.FINEST)) QuasiLinearRegression.logQl.finest("slope = " + slope + " counter = " + counter + " modCounter = " + modCounter);
				--modCounter;
			}
			// pad the rest of the curve to make equal size
			for (int i = record.size() - 1; i < recordHeight.size() - 1; i++) {
				record.add(0);
			}
			if (QuasiLinearRegression.logQl.isLoggable(Level.FINEST)) QuasiLinearRegression.logQl.fine("counter = " + counter + " modCounter = " + modCounter);
			if (this.recordSet.get(this.sourceRecordKey).isDisplayable()) record.setDisplayable(true); // depending record influence
			if (this.recordSet.getName().equals(Channels.getInstance().getActiveChannel().getActiveRecordSet().getName()) && record.isVisible()) {
				this.application.updateGraphicsWindow();
			}
			
			OpenSerialDataExplorer.getInstance().updateCurveSelectorTable();
			QuasiLinearRegression.logQl.fine("finished data calculation for record = " + this.targetRecordKey);
		}
	}

}
