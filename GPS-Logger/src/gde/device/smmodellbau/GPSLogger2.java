/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2010,2011,2012,2013,2014 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import gde.device.DeviceConfiguration;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

/**
 * GPS-Logger2 device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class GPSLogger2 extends GPSLogger {
	//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
	//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=glideRatio;
	//SMGPS2 15=AccelerationX 16=AccelerationY 17=AccelerationZ 18=ENL 
	//Unilog 19=voltageUniLog 20=currentUniLog 21=powerUniLog 22=revolutionUniLog 23=voltageRxUniLog 24=heightUniLog 25=a1UniLog 26=a2UniLog 27=a3UniLog;
	//M-LINK 28=valAdd00 29=valAdd01 30=valAdd02 31=valAdd03 32=valAdd04 33=valAdd05 34=valAdd06 35=valAdd07 36=valAdd08 37=valAdd09 38=valAdd10 39=valAdd11 40=valAdd12 41=valAdd13 42=valAdd14;

	/**
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public GPSLogger2(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * @param deviceConfig
	 */
	public GPSLogger2(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

}
