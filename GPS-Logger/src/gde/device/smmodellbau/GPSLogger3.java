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
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.
    
    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2021,2022 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import gde.device.DeviceConfiguration;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

/**
 * GPS-Logger2 device class, used as template for new device implementations
 * @author Winfried Brügmann
 */
public class GPSLogger3 extends GPSLogger2 {
	//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
	//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track;
	//SMGPS2 15=AccelerationX 16=AccelerationY 17=AccelerationZ 18=ENL 19=Impulse
	//CH1-UniLog
	//Unilog 20=voltage_UL 21=current_UL 22=power_UL 23=revolution_UL 24=voltageRx_UL 25=altitude_UL 26=a1_UL 27=a2_UL 27=a3_UL;
	//M-LINK 28=valAdd00 29=valAdd01 30=valAdd02 31=valAdd03 32=valAdd04 33=valAdd05 34=valAdd06 35=valAdd07 36=valAdd08 37=valAdd09 38=valAdd10 39=valAdd11 40=valAdd12 41=valAdd13 42=valAdd14;
	//CH2-UniLog2
	//Unilog2 20=voltage_UL 21=current_UL2 22=capacity_UL2 23=power_UL2 24=energy_UL2 25=balance_UL 26=cellVoltage1 27=cellVolt2_ul 28=cellVolltage3_UL 29=cellVoltage4_UL 30=cellVoltage5_UL 31=cellVoltage6_UL 32=revolution_UL 33=a1_UL 34=a2_UL 35=a3_UL 36=temp_UL;
	//M-LINK 37=valAdd00 38=valAdd01 39=valAdd02 40=valAdd03 41=valAdd04 42=valAdd05 43=valAdd06 44=valAdd07 45=valAdd08 46=valAdd09 47=valAdd10 48=valAdd11 49=valAdd12 50=valAdd13 51=valAdd14;
	//begin FW1.26
	//GPS 		0=latitude 1=longitude 2=altitudeGPS 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
	//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=azimuth/track 15=GlideRatio 16=SpeedGlideRatio;
	//SMGPS2 17=AccelerationX 18=AccelerationY 19=AccelerationZ 20=ENL 21=Impulse 22=AirSpeed 23=pressure static 24=pressure TEK 25=climb TEK
	//CH1-UniLog
	//Unilog 26=voltage_UL 27=current_UL 28=power_UL 29=revolution_UL 30=voltageRx_UL 31=altitude_UL 32=a1_UL 33=a2_UL 34=a3_UL;
	//M-LINK 35=valAdd00 36=valAdd01 37=valAdd02 38=valAdd03 39=valAdd04 40=valAdd05 41=valAdd06 42=valAdd07 43=valAdd08 44=valAdd09 45=valAdd10 46=valAdd11 47=valAdd12 48=valAdd13 49=valAdd14;
	//CH2-UniLog2
	//Unilog2 26=voltage_UL 27=current_UL2 28=capacity_UL2 29=power_UL2 30=energy_UL2 31=balance_UL 32=cellVoltage1 33=cellVolt2_ul 34=cellVolltage3_UL 35=cellVoltage4_UL 36=cellVoltage5_UL 37=cellVoltage6_UL 38=revolution_UL 39=a1_UL 40=a2_UL 41=a3_UL 42=temp_UL;
	//M-LINK 43=valAdd00 44=valAdd01 45=valAdd02 46=valAdd03 47=valAdd04 48=valAdd05 49=valAdd06 50=valAdd07 51=valAdd08 52=valAdd09 53=valAdd10 54=valAdd11 55=valAdd12 56=valAdd13 57=valAdd14;

	/**
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public GPSLogger3(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * @param deviceConfig
	 */
	public GPSLogger3(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

}
