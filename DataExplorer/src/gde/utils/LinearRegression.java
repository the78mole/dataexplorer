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
 * @author Winfried Brügmann
 * this thread implementation calculates the slop of the height curve using linear regression
 * a time interval may be modified, intervals < 2 seconds may result in phase shifts near points of slope = 0
 * a point interval may be modified, but point interval must be modulo 2 and < interval
 * good for calculating average slope between selected curve points
 */
public class LinearRegression extends CalculationThread {
	private Logger	log	= Logger.getLogger(RecordSet.class.getName());

	/**
	 * @param dialog
	 */
	public LinearRegression() {
		super();
	}

	/**
	 * method which do the slope calculation on base of linear regression
	 */
	public void run() {
		if (recordSet == null) {
			log.severe("can not start slope calculation -> recordSet == null");
			return;
		}
		log.fine("start data calculation for record = " + RecordSet.SLOPE);

		Record record = recordSet.get(RecordSet.SLOPE);
		Record recordHeight = recordSet.get(RecordSet.HEIGHT);
		int time_ms = 50;
		int interval = 1000 / time_ms; // 1 sec / 0.05 sec 
		int pointInterval = 6;

		int modCounter = ((recordHeight.size() - (recordHeight.size() % interval)) - (interval - pointInterval)) / pointInterval;
		// fill mod interval + pontInterval / 2
		int counter = (recordHeight.size() % interval) + (pointInterval / 2);
		for (int i = 0; i < counter; i++) { // 0,5 sec
			record.add(0);
		}

		// calculate avg x
		double avgX = (interval - 1) * time_ms / 1000.0 / interval; // 9 * 0.05 / 10; --> 0,05 
		// (xi - avgX)*(xi - avgX)
		double ssXX = 0.0; // 10 sec = 0.053025;
		for (int i = 0; i < interval; i++) { // 0,5 sec
			ssXX = ssXX + (((0.05 * i) - avgX) * ((0.05 * i) - avgX));
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
				ssXY = ssXY + (((0.05 * i) - avgX) * ((recordHeight.get(i + counter)) - avgY));
			}
			ssXY = ssXY / interval;

			int slope = 0;
			// ad point over pointInterval only
			for (int i = 0; i < pointInterval; i++) { // 0,5 sec
				slope = new Double(ssXY / ssXX).intValue(); // slope = ssXY / ssXX;
				record.add(slope);
			}
			counter = counter + pointInterval;

			if (log.isLoggable(Level.FINEST)) log.finest("slope = " + slope + " counter = " + counter + " modCounter = " + modCounter);
			--modCounter;
		}
		// fill the rest of the curve to make equal lenght
		for (int i = counter - pointInterval; i < recordHeight.size(); i++) {
			record.add(0);
		}
		if (log.isLoggable(Level.FINEST)) log.fine("counter = " + counter + " modCounter = " + modCounter);

		record.setDisplayable(true);

		OpenSerialDataExplorer.getInstance().updateCurveSelectorTable();
		log.fine("finished data calculation for record = " + RecordSet.SLOPE);
	}

}
