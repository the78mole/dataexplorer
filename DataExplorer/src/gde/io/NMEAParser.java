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

import gde.GDE;
import gde.device.CheckSumTypes;
import gde.device.IDevice;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

/**
 * Class to parse comma separated input line from a comma separated textual line which simulates serial data 
 * one data line consist of eg. $GPRMC,162614,A,5230.5900,N,01322.3900,E,10.0,90.0,131006,1.2,E,A*13
 * where $GPRMC describes a minimum NMEA sentence
 * All properties around the textual data in this line has to be specified in DataBlockType (type=TEXT, size number of values, separator=;, ...), refer to DeviceProperties_XY.XSD
 * @author Winfried Brügmann
 */
public class NMEAParser {
	static Logger					log			= Logger.getLogger(NMEAParser.class.getName());

	int									time_ms;
	long								lastTimeStamp = 0;
	int[]								values;
	Date								date;
	int									checkSum;
	String 							comment;
	int									year, month, day;
	
	final int						size;
	final int						timeFactor = 1000;
	final String				separator;
	final String				leader;
	final CheckSumTypes	checkSumType;
	final IDevice				device;
	final int						channelConfigNumber;
	
	
	public enum NMEA {
		
		
		GPRMC,
		GPGSA,
		GPGGA,
		GPVTG,
		//SM-Modellbau GPS-Logger
		SMGPS,
		MLINK,
		UNILOG,
		KOMMENTAR,
		COMMENT
	}

	/**
	 * constructor to construct a NMEA parser
	 * @param useLeaderChar , the leading character $
	 * @param useDeviceQualifier , the device qualifier, normally GP
	 * @param useSeparator , the separator character , 
	 * @param useCheckSum , exclusive OR
	 * @param useSize , size of the data points to be filled while parsing
	 */
	public NMEAParser(String useLeaderChar, String useSeparator, CheckSumTypes useCheckSum, int useSize, IDevice useDevice, int useChannelConfigNumber) {
		this.separator = useSeparator;
		this.leader = useLeaderChar;
		this.checkSumType = useCheckSum;
		this.size = useSize;
		this.values = new int[this.size];
		this.device = useDevice;
		this.channelConfigNumber = useChannelConfigNumber;
	}

	public void parse(String[] inputLines) throws Exception {
		try {
			for (String inputLine : inputLines) {
				log.log(Level.FINER, "parser inputLine = " + inputLine); //$NON-NLS-1$
				if (!inputLine.startsWith(this.leader)) throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0046, new String[] { this.leader }));
				if (!inputLine.contains(separator)) throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0047, new String[] { inputLine, separator }));

				String[] strValues = inputLine.split(this.separator); // {$GPRMC,162614,A,5230.5900,N,01322.3900,E,10.0,90.0,131006,1.2,E,A*13}

				NMEA sentence = NMEA.valueOf(strValues[0].substring(1));
				switch (sentence) {
				case GPRMC: //Recommended Minimum Sentence C (RMC)
					parseRMC(strValues);
					break;
				case GPGGA: //Global Positioning System Fix Data (GGA)				
					parseGGA(strValues);
					break;
				case GPGSA: //Satellite status (GSA)
					parseGSA(strValues);
					break;
				case GPVTG: // Velocity made good (VTG)
					parseVTG(strValues);
					break;
				case SMGPS:
					parseSMGPS(strValues);
					break;
				case MLINK:
					parseMLINK(strValues);
					break;
				case UNILOG:
					parseUNILOG(strValues);
					break;
				case COMMENT:
				case KOMMENTAR:
					//$KOMMENTAR,Extra 300. Kuban Acht. Mit UniLog Daten.*
					//$KOMMENTAR,Trojan. Ein paar liegende Figuren. Volle M-Link Bestückung.*
					this.comment = strValues[1].trim();
					this.comment = this.comment.endsWith(GDE.STRING_STAR) ? this.comment.substring(0,this.comment.length()-1) : this.comment;

					break;
				default:
					log.log(Level.WARNING, "NMEA sentence = " + strValues[0].substring(1) + " actually not implementes!"); //$NON-NLS-1$
					break;
				}
				//0=latitude 1=longitude 2=altitude 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocityKnots 8=velocityKmh 9=height 10=climb 11=voltageRx 12=distanceTotal 13=distanceStart 14=directionStart 15=glideRatio
				//16=voltage 17=current 18=power 19=revolution 20=voltageRx 21=height 22=A1 23=A2 24=A3 25=? 26=? 28=? 29=? 30=?

			}
		}
		catch (NumberFormatException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			//do not re-throw and skip sentence set
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * parse the recommended minimum sentence RMC
	 * if a NumberFormatExeption in date and time parsing occurs, skip sentence set
	 * <ul>
	 * $GPRMC,HHMMSS.ss,A,BBBB.BBBB,b,LLLLL.LLLL,l,GG.G,RR.R,DDMMYY,M.M,m,F*PP
	 * $GPRMC,132045.100,A,4752.4904,N,01106.7063,E,1.29,267.55,170910,,,A*60
 	 * <li> RMC          Recommended Minimum sentence C						</li>
	 * <li> 123519       Fix taken at 12:35:19 UTC								</li>
	 * <li> A            Status A=active or V=Void.								</li> 		
	 * <li> 4807.038,N   Latitude 48 deg 07.038' N								</li>
	 * <li> 01131.000,E  Longitude 11 deg 31.000' E								</li>
	 * <li> 022.4        Speed over the ground in knots						</li>
	 * <li> 084.4        Track angle in degrees True							</li>
	 * <li> 230394       Date - 23rd of March 1994								</li>
	 * <li> 003.1,W      Magnetic Variation												</li>
	 * <li> *6A          The checksum data, always begins with *	</li>
	 * </ul>
	 * @param strValues
	 */
	void parseRMC(String[] strValues) {
		if (strValues[2].equals("A")) { // &&  Integer.parseInt(strValues[strValues.length-1].substring(2).trim()) == Checksum.XOR(this.values, 0, this.size)) {
			if (this.date == null) { 
					String strValueDate = strValues[9].trim();
					this.year = Integer.parseInt(strValueDate.substring(4));
					year = year > 50 ? year + 1900 : year + 2000;
					this.month = Integer.parseInt(strValueDate.substring(2, 4));
					this.day = Integer.parseInt(strValueDate.substring(0, 2));
			}
			String strValueTime = strValues[1].trim();
			int hour = Integer.parseInt(strValueTime.substring(0,2));
			int minute = Integer.parseInt(strValueTime.substring(2,4));
			int second = Integer.parseInt(strValueTime.substring(4,6));
			GregorianCalendar calendar = new GregorianCalendar(this.year, this.month, this.day, hour, minute, second);
			long timeStamp = calendar.getTimeInMillis() + (strValueTime.contains(GDE.STRING_DOT) ? Integer.parseInt(strValueTime.substring(strValueTime.indexOf(GDE.STRING_DOT)+1)) : 0);

			if (lastTimeStamp < timeStamp) {
				this.time_ms = (int)(this.lastTimeStamp == 0 ? 0 : this.time_ms + (timeStamp - this.lastTimeStamp));
				this.lastTimeStamp = timeStamp;
				this.date = calendar.getTime();
				
				int latitude, longitude, velocity;
				try {
					latitude = Integer.parseInt(strValues[3].trim().replace(GDE.STRING_DOT, GDE.STRING_EMPTY));
				}
				catch (NumberFormatException e) {
					latitude = 0;
				}
				try {
					longitude = Integer.parseInt(strValues[5].trim().replace(GDE.STRING_DOT, GDE.STRING_EMPTY));
				}
				catch (NumberFormatException e) {
					longitude = 0;
				}
				try {
					velocity = (int)(Double.parseDouble(strValues[7].trim())*1852.0);
				}
				catch (NumberFormatException e) {
					velocity = 0;
				}
				 
				//GPS 
				this.values[0] = latitude;
				this.values[1] = longitude;
				//this.values[2] = altitudeAbs;
				//this.values[3] = numSatelites;
				//this.values[4] = PDOP; 
				//this.values[5] = HDOP; 
				//this.values[6] = VDOP;
				this.values[7] = velocity;
				//GPS-Logger
				//this.values[8]  = altitudeRel;
				//this.values[9]  = climb;
				//this.values[10] = voltageRx;
				//this.values[11] = distanceTotal;
				//this.values[12] = distanceStart;
				//this.values[13] = directionStart;
				//this.values[14] = glideRatio;
				//Unilog
				//this.values[15] = voltageUniLog;
				//this.values[16] = currentUniLog;
				//this.values[17] = powerUniLog;
				//this.values[18] = revolutionUniLog;
				//this.values[19] = voltageRxUniLog;
				//this.values[20] = heightUniLog;
				//this.values[21] = a1UniLog;
				//this.values[22] = a2UniLog;
				//this.values[23] = a3UniLog;
				//M-LINK
				//this.values[24] = add00;
				//this.values[25] = add01;
				//this.values[26] = add02;
				//this.values[27] = add03;
				//this.values[28] = add04;
				//this.values[29] = add05;
				//this.values[30] = add06;
				//this.values[31] = add07;
				//this.values[32] = add08;
				//this.values[33] = add09;
				//this.values[34] = add10;
				//this.values[35] = add11;
				//this.values[36] = add12;
				//this.values[37] = add13;
				//this.values[38] = add14;
			}
		}
	}

	/**
	 * parse the Global Positioning System Fix Data (GGA)
	 * if a NumberFormatExeption in time parsing occurs, skip sentence set
	 * <ul>
	 * $GPGGA,HHMMSS.ss,BBBB.BBBB,b,LLLLL.LLLL,l,Q,NN,D.D,H.H,h,G.G,g,A.A,RRRR*PP
	 * $GPGGA,132045.100,4752.4904,N,01106.7063,E,1,10,0.89,543.4,M,47.8,M,,*68
	 * <li>
	 * <li> GGA          Global Positioning System Fix Data
	 * <li> 123519       Fix taken at 12:35:19 UTC
	 * <li> 4807.038,N   Latitude 48 deg 07.038' N
	 * <li> 01131.000,E  Longitude 11 deg 31.000' E
	 * <li> 1            Fix quality: 0 = invalid
	 * <li>                           1 = GPS fix (SPS)
	 * <li>                           2 = DGPS fix
	 * <li>                           3 = PPS fix
	 * <li> 													4 = Real Time Kinematic
	 * <li> 													5 = Float RTK
	 * <li> 													6 = estimated (dead reckoning) (2.3 feature)
	 * <li> 													7 = Manual input mode
	 * <li> 													8 = Simulation mode
	 * <li> 08           Number of satellites being tracked
	 * <li> 0.9          Horizontal dilution of position
	 * <li> 545.4,M      Altitude, Meters, above mean sea level
	 * <li> 46.9,M       Height of geoid (mean sea level) above WGS84
	 * <li>                  ellipsoid
	 * <li> (empty field) time in seconds since last DGPS update
	 * <li> (empty field) DGPS station ID number
	 * <li> *47          the checksum data, always begins with *
	 * </ul>
	 * @param strValues
	 */
	void parseGGA(String[] strValues) {
		if (Integer.parseInt(strValues[6].trim()) > 0 ) { //&&  Integer.parseInt(strValues[strValues.length-1].substring(1).trim()) == Checksum.XOR(this.values, 0, this.size)) {
			String strValueTime = strValues[1].trim();
			int hour = Integer.parseInt(strValueTime.substring(0,2));
			int minute = Integer.parseInt(strValueTime.substring(2,4));
			int second = Integer.parseInt(strValueTime.substring(4,6));
			GregorianCalendar calendar = new GregorianCalendar(this.year, this.month, this.day, hour, minute, second);
			long timeStamp = calendar.getTimeInMillis() + (strValueTime.contains(GDE.STRING_DOT) ? Integer.parseInt(strValueTime.substring(strValueTime.indexOf(GDE.STRING_DOT)+1)) : 0);

			if (lastTimeStamp == timeStamp) { // validate sentence  depends to same sentence set
				int latitude, longitude, numSatelites, altitudeAbs;
				try {
					latitude = this.values[0] == 0 ? Integer.parseInt(strValues[2].trim().replace(GDE.STRING_DOT, GDE.STRING_EMPTY)) : this.values[0];
				}
				catch (NumberFormatException e) {
					latitude = 0;
				}
				try {
					longitude = this.values[1] == 0 ? Integer.parseInt(strValues[4].trim().replace(GDE.STRING_DOT, GDE.STRING_EMPTY)) : this.values[1];
				}
				catch (NumberFormatException e) {
					longitude = 0;
				}
				try {
					numSatelites = Integer.parseInt(strValues[7].trim())*1000;
				}
				catch (NumberFormatException e) {
					numSatelites = 0;
				}
				try {
					altitudeAbs = (int)(Double.parseDouble(strValues[9].trim())*1000.0);
				}
				catch (NumberFormatException e) {
					altitudeAbs = 0;
				}
				 
				//GPS 
				this.values[0] = latitude;
				this.values[1] = longitude;
				this.values[2] = altitudeAbs;
				this.values[3] = numSatelites;
				//this.values[4] = PDOP; 
				//this.values[5] = HDOP; 
				//this.values[6] = VDOP;
				//this.values[7] = velocity;
				//GPS-Logger
				//this.values[8]  = altitudeRel;
				//this.values[9]  = climb;
				//this.values[10] = voltageRx;
				//this.values[11] = distanceTotal;
				//this.values[12] = distanceStart;
				//this.values[13] = directionStart;
				//this.values[14] = glideRatio;
				//Unilog
				//this.values[15] = voltageUniLog;
				//this.values[16] = currentUniLog;
				//this.values[17] = powerUniLog;
				//this.values[18] = revolutionUniLog;
				//this.values[19] = voltageRxUniLog;
				//this.values[20] = heightUniLog;
				//this.values[21] = a1UniLog;
				//this.values[22] = a2UniLog;
				//this.values[23] = a3UniLog;
				//M-LINK
				//this.values[24] = add00;
				//this.values[25] = add01;
				//this.values[26] = add02;
				//this.values[27] = add03;
				//this.values[28] = add04;
				//this.values[29] = add05;
				//this.values[30] = add06;
				//this.values[31] = add07;
				//this.values[32] = add08;
				//this.values[33] = add09;
				//this.values[34] = add10;
				//this.values[35] = add11;
				//this.values[36] = add12;
				//this.values[37] = add13;
				//this.values[38] = add14;
			}
		}
	}

	/**
	 * parse the satellites status sentence GSA
	 * <ul>
	 * $GPGSA,A,3,31,30,23,25,05,02,29,21,10,12,,,1.20,0.89,0.81*03
	 * <li> GSA      Satellite status
	 * <li> A        Auto selection of 2D or 3D fix (M = manual) 
	 * <li> 3        3D fix - values include: 1 = no fix
	 * <li>                                   2 = 2D fix
	 * <li>                                   3 = 3D fix
	 * <li> 04,05... PRNs of satellites used for fix (space for 12) 
	 * <li> 2.5      PDOP (dilution of precision) 
	 * <li> 1.3      Horizontal dilution of precision (HDOP) 
	 * <li> 2.1      Vertical dilution of precision (VDOP)
	 * <li> *39      the checksum data, always begins with *
	 * </ul>
	 * @param strValues
	 */
	void parseGSA(String[] strValues) {
		if (strValues[1].equals("A")) { // &&  Integer.parseInt(strValues[2].trim()) > 1 &&  Integer.parseInt(strValues[strValues.length-1].substring(1).trim()) == Checksum.XOR(this.values, 0, this.size)) {
			int PDOP, HDOP, VDOP;
			try {
				PDOP = (int)(Double.parseDouble(strValues[strValues.length-3].trim())*1000.0);
			}
			catch (NumberFormatException e) {
				PDOP = 0;
			}
			try {
				HDOP = (int)(Double.parseDouble(strValues[strValues.length-2].trim())*1000.0);
			}
			catch (NumberFormatException e) {
				HDOP = 0;
			}
			try {
				String value = strValues[strValues.length-1].trim();
				value = value.contains(GDE.STRING_STAR) ? value.substring(0, value.indexOf(GDE.STRING_STAR)) : value;
				VDOP = (int)(Double.parseDouble(value)*1000.0);
			}
			catch (NumberFormatException e) {
				VDOP = 0;
			}
			 
			//GPS 
			//this.values[0]  = latitude;
			//this.values[1]  = longitude;
			//this.values[2]  = altitudeAbs;
			//this.values[3]  = numSatelites;
		  this.values[4] = PDOP; 
			this.values[5] = HDOP; 
			this.values[6] = VDOP;
			//this.values[7] = velocity;
			//GPS-Logger
			//this.values[8]  = altitudeRel;
			//this.values[9]  = climb;
			//this.values[10] = voltageRx;
			//this.values[11] = distanceTotal;
			//this.values[12] = distanceStart;
			//this.values[13] = directionStart;
			//this.values[14] = glideRatio;
			//Unilog
			//this.values[15] = voltageUniLog;
			//this.values[16] = currentUniLog;
			//this.values[17] = powerUniLog;
			//this.values[18] = revolutionUniLog;
			//this.values[19] = voltageRxUniLog;
			//this.values[20] = heightUniLog;
			//this.values[21] = a1UniLog;
			//this.values[22] = a2UniLog;
			//this.values[23] = a3UniLog;
			//M-LINK
			//this.values[24] = add00;
			//this.values[25] = add01;
			//this.values[26] = add02;
			//this.values[27] = add03;
			//this.values[28] = add04;
			//this.values[29] = add05;
			//this.values[30] = add06;
			//this.values[31] = add07;
			//this.values[32] = add08;
			//this.values[33] = add09;
			//this.values[34] = add10;
			//this.values[35] = add11;
			//this.values[36] = add12;
			//this.values[37] = add13;
			//this.values[38] = add14;
		}
	}
	
	/**
	 * parse the Velocity made good sentence VTG
	 * <ul>
	 * $GPVTG,054.7,T,034.4,M,005.5,N,010.2,K*48
	 * $GPVTG,267.55,T,,M,1.29,N,2.38,K,A*3D
	 * <li>         VTG          Track made good and ground speed
	 * <li>         054.7,T      True track made good (degrees)
	 * <li>         034.4,M      Magnetic track made good
	 * <li>         005.5,N      Ground speed, knots
	 * <li>         010.2,K      Ground speed, Kilometers per hour
	 * <li>         *48          Checksum
	 * </ul>
	 * @param strValues
	 */
	void parseVTG(String[] strValues) {
		if (true) { // Integer.parseInt(strValues[strValues.length-1].substring(1).trim()) == Checksum.XOR(this.values, 0, this.size)) {			
			int velocity;
			try {
				velocity = (int)(Double.parseDouble(strValues[7].trim())*1000.0);
			}
			catch (NumberFormatException e) {
				try {
					velocity = (int)(Double.parseDouble(strValues[5].trim())*1852.0);
				}
				catch (NumberFormatException e1) {
					velocity = 0;
				}
			}
			 
			//GPS 
			//this.values[0]  = latitude;
			//this.values[1]  = longitude;
			//this.values[2]  = altitudeAbs;
			//this.values[3]  = numSatelites;
		  //this.values[4]  = PDOP (dilution of precision) 
			//this.values[5]  = HDOP (horizontal dilution of precision) 
			//this.values[6]  = VDOP (vertical dilution of precision)
			this.values[7] = velocity;
			//GPS-Logger
			//this.values[8]  = altitudeRel;
			//this.values[9]  = climb;
			//this.values[10] = voltageRx;
			//this.values[11] = distanceTotal;
			//this.values[12] = distanceStart;
			//this.values[13] = directionStart;
			//this.values[14] = glideRatio;
			//Unilog
			//this.values[15] = voltageUniLog;
			//this.values[16] = currentUniLog;
			//this.values[17] = powerUniLog;
			//this.values[18] = revolutionUniLog;
			//this.values[19] = voltageRxUniLog;
			//this.values[20] = heightUniLog;
			//this.values[21] = a1UniLog;
			//this.values[22] = a2UniLog;
			//this.values[23] = a3UniLog;
			//M-LINK
			//this.values[24] = add00;
			//this.values[25] = add01;
			//this.values[26] = add02;
			//this.values[27] = add03;
			//this.values[28] = add04;
			//this.values[29] = add05;
			//this.values[30] = add06;
			//this.values[31] = add07;
			//this.values[32] = add08;
			//this.values[33] = add09;
			//this.values[34] = add10;
			//this.values[35] = add11;
			//this.values[36] = add12;
			//this.values[37] = add13;
			//this.values[38] = add14;
		}
	}

	/**
	 * parse SM GPS sentence
	 * $SMGPS,-1.0 m,+0.21 m/s,4.85 VRx,0.00 km,0 m,270.0 °,1:-- (  0.0 km/h)*CE
	 * 1: Höhe relativ
	 * 2: Steigen
	 * 3: Rx Spannung
	 * 4: Strecke (gesamt zurückgelegt)
	 * 5: Entfernung vom Startpunkt Luftlinie
	 * 6: Richtung vom Startpunkt
	 * 7: Gleitzahl, in der Form „1:xx (yy.y km/h)“, dabei ist 1:xx die Gleitzahl, kein gültiger Wert ist „1:--“
	 * @param strValues
	 */
	void parseSMGPS(String[] strValues) {
		int altitudeRel, climb, voltageRx, distanceTotal, distanceStart, directionStart, glideRatio;
		try {
			altitudeRel = (int)(Double.parseDouble(strValues[1].trim().split(GDE.STRING_BLANK)[0])*1000.0);
		}
		catch (NumberFormatException e) {
			altitudeRel = 0;
		}
		try {
			climb = (int)(Double.parseDouble(strValues[2].trim().split(GDE.STRING_BLANK)[0])*1000.0);
		}
		catch (NumberFormatException e) {
			climb = 0;
		}
		try {
			voltageRx = (int)(Double.parseDouble(strValues[3].trim().split(GDE.STRING_BLANK)[0])*1000.0);
		}
		catch (NumberFormatException e) {
			voltageRx = 0;
		}
		try {
			distanceTotal = (int)(Double.parseDouble(strValues[4].trim().split(GDE.STRING_BLANK)[0])*1000.0);
		}
		catch (NumberFormatException e) {
			distanceTotal = 0;
		}
		try {
			distanceStart = (int)(Double.parseDouble(strValues[5].trim().split(GDE.STRING_BLANK)[0])*1000.0);
		}
		catch (NumberFormatException e) {
			distanceStart = 0;
		}
		try {
			directionStart = (int)(Double.parseDouble(strValues[6].trim().split(GDE.STRING_BLANK)[0])*1000.0);
		}
		catch (NumberFormatException e) {
			directionStart = 0;
		}
		try {
			glideRatio = (int)(Double.parseDouble(strValues[7].trim().split(GDE.STRING_BLANK)[0].substring(2))*1000.0);
		}
		catch (NumberFormatException e) {
			glideRatio = 0;
		}
		
		//GPS 
		//this.values[0]  = latitude;
		//this.values[1]  = longitude;
		//this.values[2]  = altitudeAbs;
		//this.values[3]  = numSatelites;
	  //this.values[4]  = PDOP (dilution of precision) 
		//this.values[5]  = HDOP (horizontal dilution of precision) 
		//this.values[6]  = VDOP (vertical dilution of precision)
		//this.values[7]  = velocity;
		this.values[8]  = altitudeRel;
		this.values[9]  = climb;
		this.values[10] = voltageRx;
		this.values[11] = distanceTotal;
		this.values[12] = distanceStart;
		this.values[13] = directionStart;
		this.values[14] = glideRatio;
		//Unilog
		//this.values[15] = voltageUniLog;
		//this.values[16] = currentUniLog;
		//this.values[17] = powerUniLog;
		//this.values[18] = revolutionUniLog;
		//this.values[19] = voltageRxUniLog;
		//this.values[20] = heightUniLog;
		//this.values[21] = a1UniLog;
		//this.values[22] = a2UniLog;
		//this.values[23] = a3UniLog;
		//M-LINK
		//this.values[24] = add00;
		//this.values[25] = add01;
		//this.values[26] = add02;
		//this.values[27] = add03;
		//this.values[28] = add04;
		//this.values[29] = add05;
		//this.values[30] = add06;
		//this.values[31] = add07;
		//this.values[32] = add08;
		//this.values[33] = add09;
		//this.values[34] = add10;
		//this.values[35] = add11;
		//this.values[36] = add12;
		//this.values[37] = add13;
		//this.values[38] = add14;
	}

	/**
	 * parse SM UNILOG sentence
	 * $UNILOG,11.31 V,0.00 A,0.0 W,0 rpm,0.00 VRx,0.9 m,---- °C (A1),1303 mAh (A2),13.9 °C (int)*30
	 * 1: Spannung
	 * 2: Strom
	 * 3: Leistung
	 * 4: Drehzahl
	 * 5: Rx Spannung
	 * 6: Höhe
	 * 7: Wert A1
	 * 8: Wert A2
	 * 9: Wert A3
	 * @param strValues
	 */
	void parseUNILOG(String[] strValues) {
		int voltageUniLog, currentUniLog, powerUniLog, revolutionUniLog, voltageRxUniLog, heightUniLog, a1UniLog, a2UniLog, a3UniLog;
		try {
			voltageUniLog = (int)(Double.parseDouble(strValues[1].trim().split(GDE.STRING_BLANK)[0])*1000.0);
		}
		catch (NumberFormatException e1) {
			voltageUniLog = 0;
		}
		try {
			currentUniLog = (int)(Double.parseDouble(strValues[2].trim().split(GDE.STRING_BLANK)[0])*1000.0);
		}
		catch (NumberFormatException e1) {
			currentUniLog = 0;
		}
		try {
			powerUniLog = (int)(Double.parseDouble(strValues[3].trim().split(GDE.STRING_BLANK)[0])*1000.0);
		}
		catch (NumberFormatException e1) {
			powerUniLog = 0;
		}
		try {
			revolutionUniLog = (int)(Double.parseDouble(strValues[4].trim().split(GDE.STRING_BLANK)[0])*1000.0);
		}
		catch (NumberFormatException e1) {
			revolutionUniLog = 0;
		}
		try {
			voltageRxUniLog = (int)(Double.parseDouble(strValues[5].trim().split(GDE.STRING_BLANK)[0])*1000.0);
		}
		catch (NumberFormatException e1) {
			voltageRxUniLog = 0;
		}
		try {
			heightUniLog = (int)(Double.parseDouble(strValues[6].trim().split(GDE.STRING_BLANK)[0])*1000.0);
		}
		catch (NumberFormatException e1) {
			heightUniLog = 0;
		}
		try {
			a1UniLog = (int)(Double.parseDouble(strValues[7].trim().split(GDE.STRING_BLANK)[0])*1000.0);
		}
		catch (NumberFormatException e) {
			a1UniLog = 0;
		}
		try {
			a2UniLog = (int)(Double.parseDouble(strValues[8].trim().split(GDE.STRING_BLANK)[0])*1000.0);
		}
		catch (NumberFormatException e) {
			a2UniLog = 0;
		}
		try {
			a3UniLog = (int)(Double.parseDouble(strValues[9].trim().split(GDE.STRING_BLANK)[0])*1000.0);
		}
		catch (NumberFormatException e) {
			a3UniLog = 0;
		}
		
		//GPS 
		//this.values[0]  = latitude;
		//this.values[1]  = longitude;
		//this.values[2]  = altitudeAbs;
		//this.values[3]  = numSatelites;
	  //this.values[4]  = PDOP (dilution of precision) 
		//this.values[5]  = HDOP (horizontal dilution of precision) 
		//this.values[6]  = VDOP (vertical dilution of precision)
		//this.values[7]  = velocity;
		//GPS-Logger
		//this.values[8]  = altitudeRel;
		//this.values[9]  = climb;
		//this.values[10] = voltageRx;
		//this.values[11] = distanceTotal;
		//this.values[12] = distanceStart;
		//this.values[13] = directionStart;
		//this.values[14] = glideRatio;
		//Unilog
		this.values[15] = voltageUniLog;
		this.values[16] = currentUniLog;
		this.values[17] = powerUniLog;
		this.values[18] = revolutionUniLog;
		this.values[19] = voltageRxUniLog;
		this.values[20] = heightUniLog;
		this.values[21] = a1UniLog;
		this.values[22] = a2UniLog;
		this.values[23] = a3UniLog;
		//M-LINK
		//this.values[24] = add00;
		//this.values[25] = add01;
		//this.values[26] = add02;
		//this.values[27] = add03;
		//this.values[28] = add04;
		//this.values[29] = add05;
		//this.values[30] = add06;
		//this.values[31] = add07;
		//this.values[32] = add08;
		//this.values[33] = add09;
		//this.values[34] = add10;
		//this.values[35] = add11;
		//this.values[36] = add12;
		//this.values[37] = add13;
		//this.values[38] = add14;
	}

	/**
	 * parse SM MLINK sentence
	 * $MLINK,0: 4.9 V,1: 100 % LQI,3: 0.2 m/s,4: 14.7 °C,5: 5.4 km/h,6: 261.4 °,7: 0 m,8: 2 m,9: 0.0 km,10: 0.0 A,11: 11.1 V,12: 0 rpm,13: 681 mAh,14: 1 m*19
	 * sentence declares only address correlation to measurement values
	 * @param strValues
	 */
	void parseMLINK(String[] strValues) {
		for (int i = 1; i < strValues.length && i < 15; i++) {
			try {
				String[] values = strValues[i].trim().split(" |:");
				int address = Integer.parseInt(values[0]);
				this.values[24+address] = (int)(Double.parseDouble(values[2])*1000.0);
				if (!this.device.getMeasurement(this.channelConfigNumber, 24+address).getUnit().equals(values[3])) {
					this.device.getMeasurement(this.channelConfigNumber, 24+address).setUnit(values[3].contains(GDE.STRING_STAR) ? values[3].substring(0, values[3].indexOf(GDE.STRING_STAR)) : values[3]);
				}
			}
			catch (NumberFormatException e) {
				// ignore and leave value unchanged
			}
		}
		// needs correlation with M-LINK configuration
		//GPS 
		//this.values[0]  = latitude;
		//this.values[1]  = longitude;
		//this.values[2]  = altitudeAbs;
		//this.values[3]  = numSatelites;
	  //this.values[4]  = PDOP (dilution of precision) 
		//this.values[5]  = HDOP (horizontal dilution of precision) 
		//this.values[6]  = VDOP (vertical dilution of precision)
		//this.values[7]  = velocity;
		//GPS-Logger
		//this.values[8]  = altitudeRel;
		//this.values[9]  = climb;
		//this.values[10] = voltageRx;
		//this.values[11] = distanceTotal;
		//this.values[12] = distanceStart;
		//this.values[13] = directionStart;
		//this.values[14] = glideRatio;
		//Unilog
		//this.values[15] = voltageUniLog;
		//this.values[16] = currentUniLog;
		//this.values[17] = powerUniLog;
		//this.values[18] = revolutionUniLog;
		//this.values[19] = voltageRxUniLog;
		//this.values[20] = heightUniLog;
		//this.values[21] = a1UniLog;
		//this.values[22] = a2UniLog;
		//this.values[23] = a3UniLog;
		//M-LINK
		//this.values[24] = add00;
		//this.values[25] = add01;
		//this.values[26] = add02;
		//this.values[27] = add03;
		//this.values[28] = add04;
		//this.values[29] = add05;
		//this.values[30] = add06;
		//this.values[31] = add07;
		//this.values[32] = add08;
		//this.values[33] = add09;
		//this.values[34] = add10;
		//this.values[35] = add11;
		//this.values[36] = add12;
		//this.values[37] = add13;
		//this.values[38] = add14;
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

	/**
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment != null ? this.comment : GDE.STRING_EMPTY;
	}
}
