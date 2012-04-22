/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.utils;

import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.log.Level;

import java.util.Date;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * helper class to calculate miscellaneous values based on GPS coordinates
 */
public class GPSHelper {
	private static Logger log = Logger.getLogger(GPSHelper.class.getName());
	final static double rad = Math.PI / 180;
	
	/**
	 * find the start index where GPS longitude and latitude has coordinate data
	 * @param recordSet
	 * @param recordOrdinalLatitude
	 * @param recordOrdinalLongitude
	 * @return
	 */
	public static synchronized int getStartIndexGPS(RecordSet recordSet, int recordOrdinalLatitude, int recordOrdinalLongitude) {
		int startIndexGPS = -1;
		//input records
		Record recordLatitude = recordSet.get(recordOrdinalLatitude);
		Record recordLongitude = recordSet.get(recordOrdinalLongitude);
		int recordSize = recordLatitude.realSize();

		if (recordSize >= 3 && recordLatitude.hasReasonableData() && recordLongitude.hasReasonableData()) {
			//check GPS latitude and longitude				
			startIndexGPS = 0;
			int i = 0;
			for (; i < recordSize; ++i) {
				if (recordLatitude.get(i) != 0 && recordLongitude.get(i) != 0) {
					startIndexGPS = i;
					++i;
					break;
				}
			}
		}

		return startIndexGPS;
	}

	/**
	 * calculate values of relative altitude, trip length, distance from start point, azimuth and direction from start point
	 * @param device
	 * @param recordSet
	 * @param recordOrdinalLatitude - input, will be checked for reasonable data
	 * @param recordOrdinalLongitude - input, will be checked for reasonable data
	 * @param recordOrdinalAltitude - input, might contain zero values, which result in zero values for relative altitude and climb/slope values
	 * @param startAltitude - input, needed to calculate the distance from start point 
	 * @param recordOrdinalTripLength - output, depends on input latitude, longitude and altitude
	 * @param recordOrdinalDistance - output, depends on input latitude, longitude and altitude
	 * @param recordOrdinalAzimuth - output, depends on input latitude, longitude (will be smoothed to make somehow interpretable)
	 * @param recordOrdinalDirectionStart - output, depends on input latitude, longitude 
	 */
	public static synchronized void calculateValues(IDevice device, RecordSet recordSet, int recordOrdinalLatitude, int recordOrdinalLongitude, int recordOrdinalAltitude, int startAltitude,
			int recordOrdinalTripLength, int recordOrdinalDistance, int recordOrdinalAzimuth, int recordOrdinalDirectionStart) {
		double lastTripLength = 0;
		long startTime = new Date().getTime();

		try {
			//input records
			Record recordLatitude = recordSet.get(recordOrdinalLatitude);
			Record recordLongitude = recordSet.get(recordOrdinalLongitude);
			Record recordAlitude = recordSet.get(recordOrdinalAltitude);
			int recordSize = recordLatitude.realSize();

			if (recordSize >= 3 && recordLatitude.hasReasonableData() && recordLongitude.hasReasonableData()) {
				//output records
				Record recordTripLength = recordSet.get(recordOrdinalTripLength);
				Record recordDistance = recordSet.get(recordOrdinalDistance);
				Record recordAzimuth = recordSet.get(recordOrdinalAzimuth);
				Record recordDirection = recordSet.get(recordOrdinalDirectionStart);
				recordTripLength.clear();
				recordDistance.clear();
				recordAzimuth.clear();
				recordDirection.clear();
				
				//check GPS latitude and longitude				
				int indexGPS = 0;
				int i = 0;
				for (; i < recordSize; ++i) {
					if (recordLatitude.get(i) != 0 && recordLongitude.get(i) != 0) {
						indexGPS = i;
						++i;
						break;
					}
					recordTripLength.add(0);
					recordDistance.add(0);
					recordDirection.add(0);
					recordAzimuth.add(0);
					recordDirection.add(0);
				}
				recordTripLength.add(0);
				recordDistance.add(0);

				int lastLongitude = recordLongitude.get(indexGPS);
				int startLongitude = recordLongitude.get(indexGPS);
				double phi_start_rad = device.translateValue(recordLatitude, recordLatitude.get(indexGPS) / 1000.0) * rad;
				double lambda_start = device.translateValue(recordLongitude, lastLongitude / 1000.0);

				double phi_A_rad = phi_start_rad;
				double lambda_A = lambda_start;
				
				int indexMovement = 0;

				recordTripLength.add(0);
				recordDistance.add(0);
				recordDirection.add(0);
				
				for (; i < recordSize; ++i) {
					double phi_B_rad = device.translateValue(recordLatitude, recordLatitude.get(i) / 1000.0) * rad;
					double lambda_B = device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0);

					double prod_start = (Math.sin(phi_start_rad) * Math.sin(phi_B_rad)) + (Math.cos(phi_start_rad) * Math.cos(phi_B_rad) * Math.cos((lambda_B - lambda_start) * rad));
					prod_start = prod_start > 1.0 ? 1.0 : prod_start < -1.0 ? -1.0 : prod_start;

					double zeta_start_rad = Math.acos(prod_start);
					zeta_start_rad = zeta_start_rad <= 0.0 ? 0.0 : zeta_start_rad >= Math.PI ? Math.PI : zeta_start_rad;
					double zeta_start = zeta_start_rad / rad;

					double prod = (Math.sin(phi_A_rad) * Math.sin(phi_B_rad)) + (Math.cos(phi_A_rad) * Math.cos(phi_B_rad) * Math.cos((lambda_B - lambda_A) * rad));
					prod = prod > 1.0 ? 1.0 : prod < -1.0 ? -1.0 : prod;

					double zeta_rad = Math.acos(prod);
					zeta_rad = zeta_rad <= 0.0 ? 0.0 : zeta_rad >= Math.PI ? Math.PI : zeta_rad;
					double zeta = zeta_rad / rad;

					double powDeltaHeight = Math.pow((recordAlitude.get(i - 1) - recordAlitude.get(i)) / 1000.0, 2);
					double powOrthodrome = Math.pow((zeta * (40041000.0 / 360.0)), 2);
					double deltaTrip = Math.sqrt(powOrthodrome + powDeltaHeight);
					recordTripLength.add((int) (lastTripLength + deltaTrip));//[km}];

					powDeltaHeight = Math.pow((recordAlitude.get(i) - startAltitude) / 1000.0, 2); // alternatively the relative altitude could be used here
					powOrthodrome = Math.pow(((zeta_start * 40041000 / 360)), 2);
					recordDistance.add((int) (Math.sqrt(powOrthodrome + powDeltaHeight) * 1000.0)); //[km}];

					double prod_alpha_start = zeta_start <= 0.0 ? -1.0 : zeta_start >= Math.PI ? Math.PI : (Math.sin(phi_B_rad) - (Math.sin(phi_start_rad) * Math.cos(zeta_start_rad)))
							/ (Math.cos(phi_start_rad) * Math.sin(zeta_start_rad));
					double alpha_start = Math.acos(prod_alpha_start < -1.0 ? -1.0 : prod_alpha_start > 1.0 ? 1.0 : prod_alpha_start) / rad;
					alpha_start = startLongitude > recordLongitude.get(i) ? 360.0 - alpha_start : alpha_start;
					recordDirection.add((int) (alpha_start * 1000.0));

					double prod_alpha = zeta_rad <= 0.0 ? -1.0 : zeta_rad >= Math.PI ? Math.PI : (Math.sin(phi_B_rad) - (Math.sin(phi_A_rad) * Math.cos(zeta_rad))) / (Math.cos(phi_A_rad) * Math.sin(zeta_rad));
					double alpha = Math.acos(prod_alpha < -1.0 ? -1.0 : prod_alpha > 1.0 ? 1.0 : prod_alpha) / rad;
					alpha = lastLongitude > recordLongitude.get(i) ? 360.0 - alpha : alpha;

					recordAzimuth.add((int) (alpha * 1000.0));

					//make more insensitive for azimuth dither around 0/360 
					int deltaLongitude = Math.abs(lastLongitude - recordLongitude.get(i));
					int deltaDistance = Math.abs(recordDistance.get(i - 1) - recordDistance.get(i));
					if (i != 1 && ((deltaLongitude <= 2 && deltaTrip < 0.5) || deltaDistance < 5)) {
						if (i-indexGPS > 3) {
							recordAzimuth.set(i - 1, ((recordAzimuth.get(i - 1) + recordAzimuth.get(i - 2) + recordAzimuth.get(i - 3)) / 3));
						}
						else
							recordAzimuth.set(i - 1, recordAzimuth.get(i - 1));
					}
					else
						lastLongitude = recordLongitude.get(i);

					if (log.isLoggable(Level.FINER))
						log.log(Level.FINER, String.format("deltaLongitude = %d; deltaDistance = %d; Kurswinkel = %7.3f; %7.3f", deltaLongitude, deltaDistance, alpha, (recordAzimuth.get(i - 1) / 1000.0)));

					phi_A_rad = phi_B_rad;
					lambda_A = lambda_B;

					lastTripLength = lastTripLength + deltaTrip;
					
					if (indexMovement == 0 && recordDistance.get(i) > 1500) 
						indexMovement = i;
				}
				recordAzimuth.add(recordAzimuth.getLast());
				
				int azimuth = recordAzimuth.get(indexMovement);
				int direction = recordDirection.get(indexMovement);
				for (i = 0; i < indexMovement; i++) {
					recordAzimuth.set(i, azimuth);
					recordDirection.set(i, direction);
				}
			}
		}
		catch (RuntimeException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		if(log.isLoggable(Level.TIME)) log.log(Level.TIME, "calcualation time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$	
	}

	/**
	 * calculate values of relative altitude, trip length, distance from start point, azimuth and direction from start point
	 * @param device
	 * @param recordSet
	 * @param recordOrdinalLatitude - input, will be checked for reasonable data
	 * @param recordOrdinalLongitude - input, will be checked for reasonable data
	 * @param recordOrdinalAltitude - input, might contain zero values, which result in zero values for relative altitude and climb/slope values
	 * @param startAltitude - input, needed to calculate the distance from start point 
	 * @param recordOrdinalDistance - input, needed to start of movement 
	 * @param recordOrdinalTripLength - output, depends on input latitude, longitude and altitude
	 */
	public static synchronized void calculateTripLength(IDevice device, RecordSet recordSet, int recordOrdinalLatitude, int recordOrdinalLongitude, int recordOrdinalAltitude, int startAltitude,
			int recordOrdinalDistance, int recordOrdinalTripLength) {
		double lastTripLength = 0;
		long startTime = new Date().getTime();

		try {
			//input records
			Record recordLatitude = recordSet.get(recordOrdinalLatitude);
			Record recordLongitude = recordSet.get(recordOrdinalLongitude);
			Record recordAlitude = recordSet.get(recordOrdinalAltitude);
			Record recordDistance = recordSet.get(recordOrdinalDistance);
			int recordSize = recordLatitude.realSize();

			if (recordSize >= 3 && recordLatitude.hasReasonableData() && recordLongitude.hasReasonableData()) {
				//output records
				Record recordTripLength = recordSet.get(recordOrdinalTripLength);
				recordTripLength.clear();
				
				//check GPS latitude and longitude				
				int indexGPS = 0;
				int i = 0;
				for (; i < recordSize; ++i) {
					if (recordLatitude.get(i) != 0 && recordLongitude.get(i) != 0) {
						indexGPS = i;
						++i;
						break;
					}
					recordTripLength.add(0);
				}
				recordTripLength.add(0);

				int lastLongitude = recordLongitude.get(indexGPS);
				int startLongitude = recordLongitude.get(indexGPS);
				double phi_start_rad = device.translateValue(recordLatitude, recordLatitude.get(indexGPS) / 1000.0) * rad;
				double lambda_start = device.translateValue(recordLongitude, lastLongitude / 1000.0);

				double phi_A_rad = phi_start_rad;
				double lambda_A = lambda_start;
				
				int indexMovement = 0;
				
				for (; i < recordSize; ++i) {
					double phi_B_rad = device.translateValue(recordLatitude, recordLatitude.get(i) / 1000.0) * rad;
					double lambda_B = device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0);

					double prod_start = (Math.sin(phi_start_rad) * Math.sin(phi_B_rad)) + (Math.cos(phi_start_rad) * Math.cos(phi_B_rad) * Math.cos((lambda_B - lambda_start) * rad));
					prod_start = prod_start > 1.0 ? 1.0 : prod_start < -1.0 ? -1.0 : prod_start;

					double zeta_start_rad = Math.acos(prod_start);
					zeta_start_rad = zeta_start_rad <= 0.0 ? 0.0 : zeta_start_rad >= Math.PI ? Math.PI : zeta_start_rad;
					double zeta_start = zeta_start_rad / rad;

					double prod = (Math.sin(phi_A_rad) * Math.sin(phi_B_rad)) + (Math.cos(phi_A_rad) * Math.cos(phi_B_rad) * Math.cos((lambda_B - lambda_A) * rad));
					prod = prod > 1.0 ? 1.0 : prod < -1.0 ? -1.0 : prod;

					double zeta_rad = Math.acos(prod);
					zeta_rad = zeta_rad <= 0.0 ? 0.0 : zeta_rad >= Math.PI ? Math.PI : zeta_rad;
					double zeta = zeta_rad / rad;

					double powDeltaHeight = Math.pow((recordAlitude.get(i - 1) - recordAlitude.get(i)) / 1000.0, 2);
					double powOrthodrome = Math.pow((zeta * (40041000.0 / 360.0)), 2);
					double deltaTrip = Math.sqrt(powOrthodrome + powDeltaHeight);
					recordTripLength.add((int) (lastTripLength + deltaTrip));//[km}];

					powDeltaHeight = Math.pow((recordAlitude.get(i) - startAltitude) / 1000.0, 2); // alternatively the relative altitude could be used here
					powOrthodrome = Math.pow(((zeta_start * 40041000 / 360)), 2);

					double prod_alpha_start = zeta_start <= 0.0 ? -1.0 : zeta_start >= Math.PI ? Math.PI : (Math.sin(phi_B_rad) - (Math.sin(phi_start_rad) * Math.cos(zeta_start_rad)))
							/ (Math.cos(phi_start_rad) * Math.sin(zeta_start_rad));
					double alpha_start = Math.acos(prod_alpha_start < -1.0 ? -1.0 : prod_alpha_start > 1.0 ? 1.0 : prod_alpha_start) / rad;
					alpha_start = startLongitude > recordLongitude.get(i) ? 360.0 - alpha_start : alpha_start;

					double prod_alpha = zeta_rad <= 0.0 ? -1.0 : zeta_rad >= Math.PI ? Math.PI : (Math.sin(phi_B_rad) - (Math.sin(phi_A_rad) * Math.cos(zeta_rad))) / (Math.cos(phi_A_rad) * Math.sin(zeta_rad));
					double alpha = Math.acos(prod_alpha < -1.0 ? -1.0 : prod_alpha > 1.0 ? 1.0 : prod_alpha) / rad;
					alpha = lastLongitude > recordLongitude.get(i) ? 360.0 - alpha : alpha;

					lastLongitude = recordLongitude.get(i);

					phi_A_rad = phi_B_rad;
					lambda_A = lambda_B;

					lastTripLength = lastTripLength + deltaTrip;
					
					if (indexMovement == 0 && recordDistance.get(i) > 1500) 
						indexMovement = i;
				}
				for (int j = recordTripLength.realSize(); j < recordSize; j++) {
					recordTripLength.add(recordTripLength.get(recordTripLength.realSize()-1));
				}
			}
		}
		catch (RuntimeException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		if(log.isLoggable(Level.TIME)) log.log(Level.TIME, "calcualation time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$	
	}

	/**
	 * calculate missing azimuth data as Vector, attention this is no Record, using a record would require to put it to record set!
	 * @param device
	 * @param recordSet
	 * @param recordOrdinalLatitude
	 * @param recordOrdinalLongitude
	 * @param recordOrdinalAltitude
	 * @return
	 */
	public static synchronized Vector<Integer> calculateAzimuth(IDevice device, RecordSet recordSet, int recordOrdinalLatitude, int recordOrdinalLongitude, int recordOrdinalAltitude ) {
		Vector<Integer> recordAzimuth = new Vector<Integer>();
		long startTime = new Date().getTime();
		
		try {
			//input records
			Record recordLatitude = recordSet.get(recordOrdinalLatitude);
			Record recordLongitude = recordSet.get(recordOrdinalLongitude);
			Record recordAltitude = recordSet.get(recordOrdinalAltitude);
			int recordSize = recordLatitude.realSize();

			if (recordSize >= 3 && recordLatitude.hasReasonableData() && recordLongitude.hasReasonableData()) {
				//check GPS latitude and longitude				
				int indexGPS = 0;
				int i = 0;
				for (; i < recordSize; ++i) {
					if (recordLatitude.get(i) != 0 && recordLongitude.get(i) != 0) {
						indexGPS = i;
						++i;
						break;
					}
					recordAzimuth.add(0);
				}

				int lastLongitude = recordLongitude.get(indexGPS);

				double phi_A_rad = device.translateValue(recordLatitude, recordLatitude.get(indexGPS) / 1000.0) * rad;;
				double lambda_A = device.translateValue(recordLongitude, lastLongitude / 1000.0);
				int indexMovement = 0;

				for (; i < recordSize; ++i) {
					double phi_B_rad = device.translateValue(recordLatitude, recordLatitude.get(i) / 1000.0) * rad;
					double lambda_B = device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0);

					double prod = (Math.sin(phi_A_rad) * Math.sin(phi_B_rad)) + (Math.cos(phi_A_rad) * Math.cos(phi_B_rad) * Math.cos((lambda_B - lambda_A) * rad));
					prod = prod > 1.0 ? 1.0 : prod < -1.0 ? -1.0 : prod;

					double zeta_rad = Math.acos(prod);
					zeta_rad = zeta_rad <= 0.0 ? 0.0 : zeta_rad >= Math.PI ? Math.PI : zeta_rad;
					double prod_alpha = zeta_rad <= 0.0 ? -1.0 : zeta_rad >= Math.PI ? Math.PI : (Math.sin(phi_B_rad) - (Math.sin(phi_A_rad) * Math.cos(zeta_rad))) / (Math.cos(phi_A_rad) * Math.sin(zeta_rad));
					double alpha = Math.acos(prod_alpha < -1.0 ? -1.0 : prod_alpha > 1.0 ? 1.0 : prod_alpha) / rad;
					alpha = lastLongitude > recordLongitude.get(i) ? 360.0 - alpha : alpha;

					recordAzimuth.add((int) (alpha * 1000.0));

					phi_A_rad = phi_B_rad;
					lambda_A = lambda_B;
					lastLongitude = recordLongitude.get(i);

					if (indexMovement == 0) {
						double zeta = zeta_rad / rad;
						double powDeltaHeight = Math.pow((recordAltitude.get(i - 1) - recordAltitude.get(i)) / 1000.0, 2);
						double powOrthodrome = Math.pow((zeta * (40041000.0 / 360.0)), 2);
						if((int) (Math.sqrt(powOrthodrome + powDeltaHeight) * 1000.0) > 1500) 
						indexMovement = i;
					}
				}
				recordAzimuth.add(recordAzimuth.get(recordAzimuth.size()-1));
				
				int azimuth = recordAzimuth.get(indexMovement);
				for (i = 0; i < indexMovement; i++) {
					recordAzimuth.set(i, azimuth);
				}
			}
		}
		catch (RuntimeException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		
		if(log.isLoggable(Level.TIME)) log.log(Level.TIME, "calcualation time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$	
		return recordAzimuth;
	}
}
