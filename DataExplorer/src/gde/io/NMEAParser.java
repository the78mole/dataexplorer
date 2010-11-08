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
    
    Copyright (c) 2008,2009,2010 Winfried Bruegmann
****************************************************************************************/
package gde.io;

import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;

import java.util.logging.Logger;

import gde.device.CheckSumTypes;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.utils.Checksum;

/**
 * Class to parse comma separated input line from a comma separated textual line which simulates serial data 
 * one data line consist of eg. $GPRMC,162614,A,5230.5900,N,01322.3900,E,10.0,90.0,131006,1.2,E,A*13
 * where $GPRMC describes a minimum NMEA sentence
 * All properties around the textual data in this line has to be specified in DataBlockType (type=TEXT, size number of values, separator=;, ...), refer to DeviceProperties_XY.XSD
 * @author Winfried Brügmann
 */
public class NMEAParser {
	static Logger					log			= Logger.getLogger(NMEAParser.class.getName());

	int									recordNumber;
	int									state;
	int									time_ms;
	int[]								values;
	int									checkSum;
	int									size;

	final int						timeFactor;
	final String				separator;
	final String				leader;
	final CheckSumTypes	checkSumType;
	
	
	public enum NMEA {
		
		
		GPRMC,
		GPGSA,
		GPGGA,
		GPVTG,
		KOMMENTAR,
		//SM-Modellbau GPS-Logger
		SMGPS,
		MLINK,
		UNILOG
	}

	public NMEAParser(String useLeaderChar, String useSeparator) {
		this.timeFactor = 1000;
		this.separator = useSeparator;
		this.leader = useLeaderChar;
		this.checkSumType = CheckSumTypes.XOR;
	}

	public void parse(String inputLine) throws DevicePropertiesInconsistenceException, NumberFormatException {
		try {
			if(!inputLine.startsWith(this.leader)) 
				throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0046, new String[] {this.leader}));
			if(!inputLine.contains(separator)) 
				throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0047, new String[] {inputLine, separator})); 
			
			String[] strValues = inputLine.split(this.separator); // {$GPRMC,162614,A,5230.5900,N,01322.3900,E,10.0,90.0,131006,1.2,E,A*13}
			
			switch (NMEA.valueOf(strValues[0].substring(1))) {
			case GPRMC: //Recommended Minimum Sentence C (RMC)
				this.size = 12;
				//$GPRMC,HHMMSS,A,BBBB.BBBB,b,LLLLL.LLLL,l,GG.G,RR.R,DDMMYY,M.M,m,F*PP
//		     RMC          Recommended Minimum sentence C
//		     123519       Fix taken at 12:35:19 UTC
//		     A            Status A=active or V=Void.
//		     4807.038,N   Latitude 48 deg 07.038' N
//		     01131.000,E  Longitude 11 deg 31.000' E
//		     022.4        Speed over the ground in knots
//		     084.4        Track angle in degrees True
//		     230394       Date - 23rd of March 1994
//		     003.1,W      Magnetic Variation
//		     *6A          The checksum data, always begins with *
				
				break;
			case GPGSA:
//		     GSA      Satellite status
//		     A        Auto selection of 2D or 3D fix (M = manual) 
//		     3        3D fix - values include: 1 = no fix
//		                                       2 = 2D fix
//		                                       3 = 3D fix
//		     04,05... PRNs of satellites used for fix (space for 12) 
//		     2.5      PDOP (dilution of precision) 
//		     1.3      Horizontal dilution of precision (HDOP) 
//		     2.1      Vertical dilution of precision (VDOP)
//		     *39      the checksum data, always begins with *

				
				break;
			case GPGGA: //Global Positioning System Fix Data (GGA) 
				this.size = 13;
				//$GPGGA,HHMMSS.ss,BBBB.BBBB,b,LLLLL.LLLL,l,Q,NN,D.D,H.H,h,G.G,g,A.A,RRRR*PP
//		     GGA          Global Positioning System Fix Data
//		     123519       Fix taken at 12:35:19 UTC
//		     4807.038,N   Latitude 48 deg 07.038' N
//		     01131.000,E  Longitude 11 deg 31.000' E
//		     1            Fix quality: 0 = invalid
//		                               1 = GPS fix (SPS)
//		                               2 = DGPS fix
//		                               3 = PPS fix
//					       4 = Real Time Kinematic
//					       5 = Float RTK
//		                               6 = estimated (dead reckoning) (2.3 feature)
//					       7 = Manual input mode
//					       8 = Simulation mode
//		     08           Number of satellites being tracked
//		     0.9          Horizontal dilution of position
//		     545.4,M      Altitude, Meters, above mean sea level
//		     46.9,M       Height of geoid (mean sea level) above WGS84
//		                      ellipsoid
//		     (empty field) time in seconds since last DGPS update
//		     (empty field) DGPS station ID number
//		     *47          the checksum data, always begins with *
				
				break;
			case GPVTG: // Velocity made good (VTG)
				//$GPVTG,054.7,T,034.4,M,005.5,N,010.2,K*48
//        VTG          Track made good and ground speed
//        054.7,T      True track made good (degrees)
//        034.4,M      Magnetic track made good
//        005.5,N      Ground speed, knots
//        010.2,K      Ground speed, Kilometers per hour
//        *48          Checksum
				
				break;
			case SMGPS:
				
				break;
			case MLINK:
				
				break;
			case UNILOG:
				
				break;
			case KOMMENTAR:
				
				break;
			default:
				log.log(Level.WARNING, "NMEA sentence = " + strValues[0].substring(1) + " actually not implementes!"); //$NON-NLS-1$
				break;
			}
			this.values = new int[this.size];
			log.log(Level.FINER, "parser inputLine = " + inputLine); //$NON-NLS-1$
			if (strValues.length-4 != this.size)  throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0048, new String[] {inputLine}));
			
			String strValue = strValues[0].trim().substring(1);
			this.recordNumber = Integer.parseInt(strValue);
			
			strValue = strValues[1].trim();
			this.state = Integer.parseInt(strValue);

			strValue = strValues[2].trim();
			time_ms = Integer.parseInt(strValue) * this.timeFactor; // Seconds * 1000 = msec
			
			for (int i = 0; i < this.size; i++) { 
				strValue = strValues[i+3].trim();
				try {
					long tmpValue = strValue.length() > 0 ? Long.parseLong(strValue) : 0;
					if (tmpValue < Integer.MAX_VALUE/1000 && tmpValue > Integer.MIN_VALUE/1000)
						this.values[i] = (int) (tmpValue*1000); // enable 3 positions after decimal place
					else // needs special processing within IDevice.translateValue(), IDevice.reverseTranslateValue()
						if (tmpValue < Integer.MAX_VALUE || tmpValue > Integer.MIN_VALUE) {
							this.values[i] = (int) tmpValue;
						}
						else {
							this.values[i] = (int) (tmpValue/1000);
						}
				}
				catch (NumberFormatException e) {
					this.values[i] = 0;
				}
			}

			strValue = strValues[this.values.length].trim();
			int checkSum = Integer.parseInt(strValue);
			boolean isValid = true;
			if (checkSumType != null) {
				switch (checkSumType) {
				case ADD:
					isValid = checkSum == Checksum.ADD(this.values, 0, this.size);
					break;
				case XOR:
					isValid = checkSum == Checksum.XOR(this.values, 0, this.size);
					break;
				case OR:
					isValid = checkSum == Checksum.OR(this.values, 0, this.size);
					break;
				case AND:
					isValid = checkSum == Checksum.AND(this.values, 0, this.size);
					break;
				}
			}
			if (!isValid) {
				DevicePropertiesInconsistenceException e = new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0049, new String[] {strValue})); 
				log.log(Level.WARNING, e.getMessage(), e);
				throw e;
			}
		}
		catch (NumberFormatException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * @return the recordNumber
	 */
	public int getRecordNumber() {
		return recordNumber;
	}

	/**
	 * @return the state
	 */
	public int getState() {
		return state;
	}

	/**
	 * @return the time
	 */
	public long getTime_ms() {
		return time_ms;
	}

	/**
	 * @return the values
	 */
	public int[] getValues() {
		return values;
	}
}
