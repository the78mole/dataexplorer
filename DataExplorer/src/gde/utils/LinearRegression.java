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

/**
 * This thread implementation calculates the slop of the height curve using linear regression
 * a time interval may be modified, intervals < 2 seconds may result in phase shifts near points of slope = 0
 * a point interval may be modified, but point interval must be modulo 2 and < interval
 * good for calculating average slope between selected curve points
 * @author Winfried Brügmann
 */
public class LinearRegression extends CalculationThread {
	final static Logger	logLin	= Logger.getLogger(LinearRegression.class.getName());

	/**
	 * constructor where calculation required parameters are given as parameter
	 * @param currentRecordSet
	 * @param inRecordKey
	 * @param outRecordKey
	 * @param calcIntervalSec
	 */
	public LinearRegression(RecordSet currentRecordSet, String inRecordKey, String outRecordKey, int calcIntervalSec) {
		super(currentRecordSet, inRecordKey, outRecordKey, calcIntervalSec);
	}

	/**
	 * method which do the slope calculation on base of linear regression
	 */
	@Override
	public void run() {
		if (this.recordSet == null || this.sourceRecordKey == null || this.targetRecordKey == null) {
			LinearRegression.logLin.warning("Die Steigung kann icht berechnet werden -> recordSet == null || sourceRecordKey == null || targetRecordKey == null");
			return;
		}
		LinearRegression.logLin.fine("start data calculation for record = " + this.targetRecordKey);

		Record record = this.recordSet.get(this.targetRecordKey);
		Record recordHeight = this.recordSet.get(this.sourceRecordKey);
		int time_ms = this.recordSet.getTimeStep_ms();
		int pointsPerInterval = this.calcInterval_sec * 1000 / time_ms; // 4000ms/50ms/point -> 80 points per interval
		int pointInterval = 2;

		int modCounter = ((recordHeight.size() - (recordHeight.size() % pointsPerInterval)) - (pointsPerInterval - pointInterval)) / pointInterval;
		// fill mod interval + pontInterval / 2
		int counter = (recordHeight.size() % pointsPerInterval) + (pointInterval / 2);
		for (int i = 0; i < counter; i++) { // 0,5 sec
			record.add(0);
		}

		// calculate avg x
		double avgX = (pointsPerInterval - 1) * time_ms / 1000.0 / pointsPerInterval; // 9 * 0.05 / 10; --> 0,05 
		// (xi - avgX)*(xi - avgX)
		double ssXX = 0.0; // 10 sec = 0.053025;
		for (int i = 0; i < pointsPerInterval; i++) { // 0,5 sec
			ssXX = ssXX + (((0.05 * i) - avgX) * ((0.05 * i) - avgX));
		}
		ssXX = ssXX / pointsPerInterval;
		if (LinearRegression.logLin.isLoggable(Level.FINEST)) LinearRegression.logLin.finest("avgX = " + avgX + " ssXX = " + ssXX);

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
				ssXY = ssXY + (((0.05 * i) - avgX) * ((recordHeight.get(i + counter)) - avgY));
			}
			ssXY = ssXY / pointsPerInterval;

			int slope = 0;
			// ad point over pointInterval only
			for (int i = 0; i < pointInterval; i++) { // 0,5 sec
				slope = new Double(ssXY / ssXX).intValue(); // slope = ssXY / ssXX;
				record.add(slope);
			}
			counter = counter + pointInterval;

			if (LinearRegression.logLin.isLoggable(Level.FINEST)) LinearRegression.logLin.finest("slope = " + slope + " counter = " + counter + " modCounter = " + modCounter);
			--modCounter;
		}
		// fill the rest of the curve to make equal lenght
		for (int i = counter - pointInterval; i < recordHeight.size(); i++) {
			record.add(0);
		}
		if (LinearRegression.logLin.isLoggable(Level.FINEST)) LinearRegression.logLin.fine("counter = " + counter + " modCounter = " + modCounter);

		record.setDisplayable(true);
		if (record.isVisible()) this.application.updateGraphicsWindow();

		OpenSerialDataExplorer.getInstance().updateCurveSelectorTable();
		LinearRegression.logLin.fine("finished data calculation for record = " + this.targetRecordKey);
	}

}
