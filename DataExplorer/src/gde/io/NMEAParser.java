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
    
    Copyright (c) 2010,2011 Winfried Bruegmann
****************************************************************************************/
package gde.io;

import gde.GDE;
import gde.device.CheckSumTypes;
import gde.device.IDevice;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.Checksum;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Class to parse comma separated input line from a comma separated textual line which simulates serial data 
 * one data line consist of eg. $GPRMC,162614,A,5230.5900,N,01322.3900,E,10.0,90.0,131006,1.2,E,A*13
 * where $GPRMC describes a minimum NMEA sentence
 * All properties around the textual data in this line has to be specified in DataBlockType (type=TEXT, size number of values, separator=;, ...), refer to DeviceProperties_XY.XSD
 * @author Winfried Brügmann
 */
public class NMEAParser {
	private static final Logger	log												= Logger.getLogger(NMEAParser.class.getName());
	private final String				$CLASS_NAME								= "NMEAParser.";
	private static final String	STRING_SENTENCE_SPLITTER	= " |:";																				//$NON-NLS-1$

	int													time_ms;
	long												lastTimeStamp							= 0;
	int[]												values;
	Date												date;
	short												timeOffsetUTC							= 0;
	int													checkSum;
	String											comment;
	int													year, month, day;
	int													numGSVsentence 						= 1; //check if GSV sentences are in sync
	int													numSattelites							= 0;
	Vector<String>							missingImpleWarned				= new Vector<String>();

	final int										size;
	final String								separator;
	final String								leader;
	final CheckSumTypes					checkSumType;
	final IDevice								device;
	final int										channelConfigNumber;
	
	int lineNumber = 0;

	public enum NMEA {
		//NMEA sentences
		GPRMC, GPGSA, GPGGA, GPVTG, GPGSV, GPRMB, GPGLL, GPZDA, 
		//additional SM-Modellbau GPS-Logger NMEA sentences
		GPSETUP, SETUP, SMGPS, MLINK, UNILOG, KOMMENTAR, COMMENT
	}

	/**
	 * constructor to construct a NMEA parser
	 * @param useLeaderChar , the leading character $
	 * @param useDeviceQualifier , the device qualifier, normally GP
	 * @param useSeparator , the separator character , 
	 * @param useCheckSum , exclusive OR
	 * @param useSize , size of the data points to be filled while parsing
	 */
	public NMEAParser(String useLeaderChar, String useSeparator, CheckSumTypes useCheckSum, int useSize, IDevice useDevice, int useChannelConfigNumber, short useTimeOffsetUTC) {
		this.separator = useSeparator;
		this.leader = useLeaderChar;
		this.checkSumType = useCheckSum;
		this.size = useSize;
		this.values = new int[this.size];
		this.device = useDevice;
		this.channelConfigNumber = useChannelConfigNumber;
		this.timeOffsetUTC = useTimeOffsetUTC;
	}

	public void parse(Vector<String> inputLines, int lastLineNumber) throws Exception {
		final String $METHOD_NAME = "parse()";
		try {
			int indexRMC = 0;
			for (; indexRMC < inputLines.size(); ++indexRMC) {
				if (inputLines.elementAt(indexRMC).indexOf("RMC", 1) > -1) {
					this.lineNumber = lastLineNumber - inputLines.size() + indexRMC + 1;
					parse(inputLines.elementAt(indexRMC));
					inputLines.remove(indexRMC);
					break;
				}
			}

			for (int i = 0; i < inputLines.size(); ++i) {
				String inputLine = inputLines.elementAt(i);
				this.lineNumber = lastLineNumber - inputLines.size() + (i<indexRMC ? i : i+1);
				parse(inputLine);
			}
		}
		catch (NumberFormatException e) {
			log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "line number " + this.lineNumber + GDE.STRING_MESSAGE_CONCAT + e.getMessage(), e);
			//do not re-throw and skip sentence set
		}
		catch (Exception e) {
			log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "line number " + this.lineNumber + GDE.STRING_MESSAGE_CONCAT + e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * parse the input line string
	 * @param inputLine
	 * @throws DevicePropertiesInconsistenceException
	 * @throws Exception
	 */
	private void parse(String inputLine) throws DevicePropertiesInconsistenceException, Exception {
		final String $METHOD_NAME = "parse()";
		log.log(Level.FINER, "parser inputLine = " + inputLine); //$NON-NLS-1$
		if (!inputLine.startsWith(this.leader)) 
			throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0046, new Object[] { this.leader, this.lineNumber }));
		if (!inputLine.contains(this.separator)) 
			throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0047, new Object[] { inputLine, this.separator, this.lineNumber }));

		if (isChecksumOK(inputLine)) {

			String[] strValues = inputLine.split(this.separator); // {$GPRMC,162614,A,5230.5900,N,01322.3900,E,10.0,90.0,131006,1.2,E,A*13}

			try {
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
				case GPGSV: // Satellites in view (GSV)
					parseGSV(strValues);
					break;
				case GPRMB: // Recommended minimum navigation information (RMB)
					parseRMB(strValues);
					break;
				case GPGLL: // Geographic Latitude and Longitude (GLL)
					parseGLL(strValues);
					break;
				case GPZDA: // Data and Time (ZDA)
					parseZDA(strValues);
					break;
				case SMGPS:
					if (this.values.length >=15) parseSMGPS(strValues);
					break;
				case UNILOG:
					if (this.values.length >=24) parseUNILOG(strValues);
					break;
				case MLINK:
					if (this.values.length >=39) parseMLINK(strValues);
					break;
				case COMMENT:
				case KOMMENTAR:
					//$KOMMENTAR,Extra 300. Kuban Acht. Mit UniLog Daten.*
					//$KOMMENTAR,Trojan. Ein paar liegende Figuren. Volle M-Link Bestückung.*
					this.comment = strValues[1].trim();
					this.comment = this.comment.endsWith(GDE.STRING_STAR) ? this.comment.substring(0, this.comment.length() - 1) : this.comment;

					break;
				case GPSETUP:
					//not yet implemented, future
					break;
				case SETUP: // setup SM GPS-Logger firmware 1.00
//							try {
//								byte[] buffer = StringHelper.convert2ByteArray(strValues[1]);
//								this.timeOffsetUTC = (short) ((buffer[7] << 8) + (buffer[6] & 0x00FF));
//								this.timeOffsetUTC = this.timeOffsetUTC > 12 ? 12 : this.timeOffsetUTC < -12 ? -12 : this.timeOffsetUTC;
//							}
//							catch (Exception e) {
//								log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "line number " + this.lineNumber + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
//							}
					break;
				}
				//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
				//GPS 		8=altitudeRel 9=climb 10=magneticVariation 11=tripLength 12=distance 13=azimuth
				//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=glideRatio;
				//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
				//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
			}
			catch (Exception e) {
				if (e instanceof IllegalArgumentException && e.getMessage().contains("No enum")) { //$NON-NLS-1$
					if (!missingImpleWarned.contains(strValues[0].substring(1))) {
						log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, "line number " + this.lineNumber + " - NMEA sentence = " + strValues[0].substring(1) + " actually not implementes!"); //$NON-NLS-1$ //$NON-NLS-2$
						missingImpleWarned.add(strValues[0].substring(1));
					}
				}
				else {
					throw e;
				}
			}
		}
	}

	/**
	 * check sentence checkSum against last two bytes hex value
	 * checksum is build of exclusive or between $ and *
	 * @param strValues
	 */
	boolean isChecksumOK(String sentence) {
		boolean isOK = true;
		try {
			String hexCheckSum = sentence.trim().substring(sentence.indexOf(GDE.STRING_STAR) + 1);
			if (hexCheckSum.length() == 2) {
				int checkSum = Integer.parseInt(hexCheckSum, 16);
				String subSentence = sentence.substring(1, sentence.indexOf(GDE.STRING_STAR));
				isOK = checkSum == Checksum.XOR(subSentence.toCharArray());
				if (!isOK) 
					log.logp(Level.WARNING, $CLASS_NAME, "parse()", String.format("line number %d : checkSum 0x%s missmatch 0x%X in %s!", this.lineNumber, hexCheckSum, Checksum.XOR(subSentence.getBytes()), subSentence)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		catch (Exception e) {
			log.logp(Level.WARNING, $CLASS_NAME, "isChecksumOK()", "line number " + this.lineNumber + GDE.STRING_BLANK + e.getClass().getSimpleName() + GDE.STRING_BLANK + e.getMessage() + " in " + sentence); //$NON-NLS-1$
		}
		return isOK;
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
		if (strValues[2].equals("A") || strValues[2].equals("V")) { //$NON-NLS-1$
			if (this.date == null) {
				String strValueDate = strValues[9].trim();
				this.year = Integer.parseInt(strValueDate.substring(4));
				this.year = this.year > 50 ? this.year + 1900 : this.year + 2000;
				this.month = Integer.parseInt(strValueDate.substring(2, 4));
				this.day = Integer.parseInt(strValueDate.substring(0, 2));
			}
			String strValueTime = strValues[1].trim();
			int hour = Integer.parseInt(strValueTime.substring(0, 2)) + this.timeOffsetUTC;
			int minute = Integer.parseInt(strValueTime.substring(2, 4));
			int second = Integer.parseInt(strValueTime.substring(4, 6));
			GregorianCalendar calendar = new GregorianCalendar(this.year, this.month - 1, this.day, hour, minute, second);
			long timeStamp = calendar.getTimeInMillis() + (strValueTime.contains(GDE.STRING_DOT) ? Integer.parseInt(strValueTime.substring(strValueTime.indexOf(GDE.STRING_DOT) + 1)) : 0);

			if (this.lastTimeStamp < timeStamp) {
				this.time_ms = (int) (this.lastTimeStamp == 0 ? 0 : this.time_ms + (timeStamp - this.lastTimeStamp));
				this.lastTimeStamp = timeStamp;
				this.date = calendar.getTime();
				log.log(Level.FINE, new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(this.date)); //$NON-NLS-1$);

				int latitude, longitude, velocity, magneticVariation;
				try {
					latitude = Integer.parseInt(strValues[3].trim().replace(GDE.STRING_DOT, GDE.STRING_EMPTY));
					latitude = strValues[4].trim().equalsIgnoreCase("N") ? latitude : -1 * latitude; 
				}
				catch (Exception e) {
					latitude = this.values[0];
				}
				try {
					longitude = Integer.parseInt(strValues[5].trim().replace(GDE.STRING_DOT, GDE.STRING_EMPTY));
					longitude = strValues[6].trim().equalsIgnoreCase("E") ? longitude : -1 * longitude; 
				}
				catch (Exception e) {
					longitude = this.values[1];
				}
				try {
					velocity = (int) (Double.parseDouble(strValues[7].trim()) * 1852.0);
				}
				catch (Exception e) {
					velocity = this.values[7];
				}
				try {
					magneticVariation = strValues[10].trim().length() > 0 ? (int) (Double.parseDouble(strValues[10].trim()) * 1000.0) : this.values[10];
				}
				catch (Exception e) {
					magneticVariation = this.values[8];
				}

				//GPS 
				this.values[0] = latitude;
				this.values[1] = longitude;
				//this.values[2]  = altitudeAbs;
				//this.values[3]  = numSatelites;
				//this.values[4]  = PDOP (dilution of precision) 
				//this.values[5]  = HDOP (horizontal dilution of precision) 
				//this.values[6]  = VDOP (vertical dilution of precision)
				this.values[7] = velocity;
				this.values[8] = magneticVariation; // SM GPS-Logger -> altitudeRel;
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
		if (Integer.parseInt(strValues[6].trim()) > 0) {
			String strValueTime = strValues[1].trim();
			int hour = Integer.parseInt(strValueTime.substring(0, 2)) + this.timeOffsetUTC;
			int minute = Integer.parseInt(strValueTime.substring(2, 4));
			int second = Integer.parseInt(strValueTime.substring(4, 6));
			GregorianCalendar calendar = new GregorianCalendar(this.year, this.month - 1, this.day, hour, minute, second);
			long timeStamp = calendar.getTimeInMillis() + (strValueTime.contains(GDE.STRING_DOT) ? Integer.parseInt(strValueTime.substring(strValueTime.indexOf(GDE.STRING_DOT) + 1)) : 0);

			if (this.lastTimeStamp == timeStamp) { // validate sentence  depends to same sentence set
				int latitude, longitude, numSatelites, altitudeAbs;
				try {
					if (this.values[0] == 0) {
						latitude = Integer.parseInt(strValues[2].trim().replace(GDE.STRING_DOT, GDE.STRING_EMPTY));
						latitude = strValues[3].trim().equalsIgnoreCase("N") ? latitude : -1 * latitude;
					} 
					else
						latitude = this.values[0];
				}
				catch (Exception e) {
					latitude = this.values[0];
				}
				try {
					if (this.values[1] == 0) {
						longitude = Integer.parseInt(strValues[4].trim().replace(GDE.STRING_DOT, GDE.STRING_EMPTY));
						longitude = strValues[5].trim().equalsIgnoreCase("E") ? longitude : -1 * longitude;
					} 
					else
						longitude = this.values[1];
				}
				catch (Exception e) {
					longitude = this.values[1];
				}
				try {
					numSatelites = Integer.parseInt(strValues[7].trim()) * 1000;
				}
				catch (Exception e) {
					numSatelites = this.values[3];
				}
				try {
					altitudeAbs = (int) (Double.parseDouble(strValues[9].trim()) * 1000.0);
				}
				catch (Exception e) {
					altitudeAbs = this.values[2];
				}

				//GPS 
				this.values[0] = latitude;
				this.values[1] = longitude;
				this.values[2] = altitudeAbs;
				this.values[3] = numSatelites;
				//this.values[4]  = PDOP (dilution of precision) 
				//this.values[5]  = HDOP (horizontal dilution of precision) 
				//this.values[6]  = VDOP (vertical dilution of precision)
				//this.values[7]  = velocity;
				//this.values[8]  = magneticVariation; // SM GPS-Logger -> altitudeRel;
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
		if (strValues[1].equals("A") || strValues[1].equals("M")) { //$NON-NLS-1$ //$NON-NLS-2$
			int PDOP = this.values[4], HDOP = this.values[5], VDOP = this.values[6];
			try {
				PDOP = (int) (Double.parseDouble(strValues[strValues.length - 3].trim()) * 1000.0);
			}
			catch (Exception e) {
				//ignore and leave value unchanged
			}
			try {
				HDOP = (int) (Double.parseDouble(strValues[strValues.length - 2].trim()) * 1000.0);
			}
			catch (Exception e) {
				//ignore and leave value unchanged
			}
			try {
				String value = strValues[strValues.length - 1].trim();
				value = value.contains(GDE.STRING_STAR) ? value.substring(0, value.indexOf(GDE.STRING_STAR)) : value;
				VDOP = (int) (Double.parseDouble(value) * 1000.0);
			}
			catch (Exception e) {
				//ignore and leave value unchanged
			}

			//GPS 
			//this.values[0]  = latitude;
			//this.values[1]  = longitude;
			//this.values[2]  = altitudeAbs;
			//this.values[3]  = numSatelites;
			this.values[4] = PDOP; // (dilution of precision) 
			this.values[5] = HDOP; // (horizontal dilution of precision) 
			this.values[6] = VDOP; // (vertical dilution of precision)
			//this.values[7]  = velocity;
			//this.values[8]  = magneticVariation; // SM GPS-Logger -> altitudeRel;
		}
	}

	/**
	 * parse the GSV - Satellites in view
	 * <ul>
	 * $GPGSV,2,1,08,01,40,083,46,02,17,308,41,12,07,344,39,14,22,228,45*75
	 * <li> GSV          Satellites in view
	 * <li> 2            Number of sentences for full data
	 * <li> 1            sentence 1 of 2
	 * <li> 08           Number of satellites in view
	 * <li> 01           Satellite PRN number
	 * <li> 40           Elevation, degrees
	 * <li> 083          Azimuth, degrees
	 * <li> 46           SNR - higher is better   
	 * <li> 			for up to 4 satellites per sentence
	 * <li> *75      the checksum data, always begins with *
	 * </ul>
	 * @param strValues
	 */
	void parseGSV(String[] strValues) {
		if (!(GDE.STRING_EMPTY+numGSVsentence).equals(strValues[1]) || (GDE.STRING_EMPTY+numGSVsentence).equals(strValues[2])) { 
			int numSentence = 1;
			int actualSentence = 0;
			int actualNumSattelites = 0;
			try {
				numSentence = Integer.parseInt(strValues[1]);
				actualSentence = Integer.parseInt(strValues[2]);
				actualNumSattelites = Integer.parseInt(strValues[3]) * 1000;
				if (numSentence >= numGSVsentence && actualSentence <= numGSVsentence && (numSattelites == 0 || numSattelites == actualNumSattelites)){ // in synch
					numGSVsentence = actualSentence == numSentence ? 1 : numSentence; // reset after reading last sentence of set
					numSattelites = actualNumSattelites;
					this.values[3]  = actualNumSattelites;
				}
				else {
					numGSVsentence = 1;
					log.log(Level.WARNING, "GSV sentences out of sync, skip and reset!");
					return;
				}
			}
			catch (Exception e) {
				//ignore and leave value unchanged
			}
			//passed sentence sync check
			try {
				for (int i = 0; i < 4 && (7 + 4*i) < strValues.length; i++) { //up to 4 satellites per sentence
					int numSattelite = Integer.parseInt(strValues[4 + 4*i]);
					int elevationDegrees = Integer.parseInt(strValues[5 + 4*i]);
					int azimuthDegrees = Integer.parseInt(strValues[6 + 4*i]);
					int signalNoiseRation;
					if (strValues[7 + 4*i].contains(GDE.STRING_STAR)) {
						String tmpValue = strValues[7 + 4*i].substring(0, strValues[7 + 4*i].indexOf(GDE.STRING_STAR));
						signalNoiseRation = tmpValue.length() > 0 ? Integer.parseInt(tmpValue) : 0;
					}
					else 
						signalNoiseRation = Integer.parseInt(strValues[7 + 4*i]);
					log.log(Level.WARNING, "numSattelite = " + numSattelite + " elevation = " + elevationDegrees + " azimuth = " + azimuthDegrees + " signalNoiseRation = " + signalNoiseRation);
				}
			}
			catch (Exception e) {
				//ignore and leave value unchanged
			}

			//GPS 
			//this.values[0]  = latitude;
			//this.values[1]  = longitude;
			//this.values[2]  = altitudeAbs;
			//this.values[3]  = numSatelites;
			//this.values[4] = PDOP; // (dilution of precision) 
			//this.values[5] = HDOP; // (horizontal dilution of precision) 
			//this.values[6] = VDOP; // (vertical dilution of precision)
			//this.values[7]  = velocity;

			//this.values[8]  = altitudeRel;
			//this.values[9]  = climb;
			//this.values[10] = magneticVariation; // SM GPS-Logger -> voltageRx;
			//this.values[11] = distanceTotal;
			//this.values[12] = distanceStart;
			//this.values[13] = directionStart;
		}
	}

	/**
	 * parse the velocity made good sentence VTG
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
		if (true) {
			int velocity;
			try {
				velocity = (int) (Double.parseDouble(strValues[7].trim()) * 1000.0);
			}
			catch (Exception e) {
				try {
					velocity = (int) (Double.parseDouble(strValues[5].trim()) * 1852.0);
				}
				catch (Exception e1) {
					velocity = this.values[7];
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
			//this.values[8]  = magneticVariation; // SM GPS-Logger -> altitudeRel;
		}
	}

	/**
	 	 * parse the GLL - Geographic Latitude and Longitude
	 	 * <ul>
	 	 * $GPGLL,4916.45,N,12311.12,W,225444,A,*1D
	 	 * <li> GLL          Geographic position, Latitude and Longitude
	 	 * <li> 4916.46,N    Latitude 49 deg. 16.45 min. North
	 	 * <li> 12311.12,W   Longitude 123 deg. 11.12 min. West
	 	 * <li> 225444       Fix taken at 22:54:44 UTC
	 	 * <li> A            Data Active or V (void)
	 	 * <li> *iD          checksum data
	 	 * </ul>
	 	 * @param strValues
	 	 */
	void parseGLL(String[] strValues) {
		if (strValues[6].equals("A")) { //$NON-NLS-1$ //$NON-NLS-2$
			if (this.date == null) {
				Calendar calendar = new GregorianCalendar();
				this.year = calendar.get(Calendar.YEAR);
				this.month = calendar.get(Calendar.MONTH)+1;
				this.day = calendar.get(Calendar.DATE);
			}
			String strValueTime = strValues[5].trim();
			int hour = Integer.parseInt(strValueTime.substring(0, 2)) + this.timeOffsetUTC;
			int minute = Integer.parseInt(strValueTime.substring(2, 4));
			int second = Integer.parseInt(strValueTime.substring(4, 6));
			GregorianCalendar calendar = new GregorianCalendar(this.year, this.month - 1, this.day, hour, minute, second);
			long timeStamp = calendar.getTimeInMillis() + (strValueTime.contains(GDE.STRING_DOT) ? Integer.parseInt(strValueTime.substring(strValueTime.indexOf(GDE.STRING_DOT) + 1)) : 0);

			if (this.lastTimeStamp < timeStamp) {
				this.time_ms = (int) (this.lastTimeStamp == 0 ? 0 : this.time_ms + (timeStamp - this.lastTimeStamp));
				this.lastTimeStamp = timeStamp;
				this.date = calendar.getTime();
				log.log(Level.FINE, new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(this.date)); //$NON-NLS-1$);

				int latitude, longitude;
				try {
					if (this.values[0] == 0) {
						latitude = Integer.parseInt(strValues[2].trim().replace(GDE.STRING_DOT, GDE.STRING_EMPTY));
						latitude = strValues[3].trim().equalsIgnoreCase("N") ? latitude : -1 * latitude;
					} 
					else
						latitude = this.values[0];
				}
				catch (Exception e) {
					latitude = this.values[0];
				}
				try {
					if (this.values[1] == 0) {
						longitude = Integer.parseInt(strValues[4].trim().replace(GDE.STRING_DOT, GDE.STRING_EMPTY));
						longitude = strValues[5].trim().equalsIgnoreCase("E") ? longitude : -1 * longitude;
					} 
					else
						longitude = this.values[1];
				}
				catch (Exception e) {
					longitude = this.values[1];
				}

				//GPS 
				this.values[0] = latitude;
				this.values[1] = longitude;
				//this.values[2]  = altitudeAbs;
				//this.values[3]  = numSatelites;
				//this.values[4] = PDOP; // (dilution of precision) 
				//this.values[5] = HDOP; // (horizontal dilution of precision) 
				//this.values[6] = VDOP; // (vertical dilution of precision)
				//this.values[7]  = velocity;
				//this.values[8]  = magneticVariation; // SM GPS-Logger -> altitudeRel;
			}
		}
	}

	/**
	 * parse ZDA - Data and Time
	 * $GPZDA,hhmmss.ss,dd,mm,yyyy,xx,yy*CC
	 * $GPZDA,201530.00,04,07,2002,00,00*60
	 * <ul>
	 * <li>         ZDA					Data and Time
	 * <li>         hhmmss    	HrMinSec(UTC)
	 * <li>         dd,mm,yyy 	Day,Month,Year
	 * <li>         xx        	local zone hours -13..13
	 * <li>         yy        	local zone minutes 0..59    	 								
	 * <li>         *CC       	checksum
	 * </ul>
	 * @param strValues
	 */
	void parseZDA(String[] strValues) {
		if (this.date == null) {
			String strValueDate = strValues[9].trim();
			this.year = Integer.parseInt(strValueDate.substring(4));
			this.year = this.year > 50 ? this.year + 1900 : this.year + 2000;
			this.month = Integer.parseInt(strValueDate.substring(2, 4));
			this.day = Integer.parseInt(strValueDate.substring(0, 2));
		}
		String strValueTime = strValues[1].trim();
		int hour = Integer.parseInt(strValueTime.substring(0, 2)) + this.timeOffsetUTC;
		int minute = Integer.parseInt(strValueTime.substring(2, 4));
		int second = Integer.parseInt(strValueTime.substring(4, 6));
		GregorianCalendar calendar = new GregorianCalendar(this.year, this.month - 1, this.day, hour, minute, second);
		long timeStamp = calendar.getTimeInMillis() + (strValueTime.contains(GDE.STRING_DOT) ? Integer.parseInt(strValueTime.substring(strValueTime.indexOf(GDE.STRING_DOT) + 1)) : 0);

		if (this.lastTimeStamp < timeStamp) {
			this.time_ms = (int) (this.lastTimeStamp == 0 ? 0 : this.time_ms + (timeStamp - this.lastTimeStamp));
			this.lastTimeStamp = timeStamp;
			this.date = calendar.getTime();
			log.log(Level.FINE, new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(this.date)); //$NON-NLS-1$);
		}
	}

	/**
	 * parse RMB - Recommended minimum navigation information
	 * $GPZDA,hhmmss.ss,dd,mm,yyyy,xx,yy*CC
	 * $GPRMB,A,0.66,L,003,004,4917.24,N,12309.57,W,001.3,052.5,000.5,V*20
	 * <ul>
	 * <li>         RMB          Recommended minimum navigation information
	 * <li>         A            Data status A = OK, V = Void (warning)
	 * <li>         0.66,L       Cross-track error (nautical miles, 9.99 max), steer Left to correct (or R = right)
	 * <li>         003          Origin waypoint ID
	 * <li>         004          Destination waypoint ID  	 	
	 * <li>         4917.24,N    Destination waypoint latitude 49 deg. 17.24 min. N
	 * <li>         12309.57,W   Destination waypoint longitude 123 deg. 09.57 min. W		
	 * <li>         001.3        Range to destination, nautical miles (999.9 max)	    
	 * <li>         052.5        True bearing to destination		
	 * <li>         000.5        Velocity towards destination, knots	
	 * <li>         V            Arrival alarm  A = arrived, V = not arrived	
	 * <li>         *20          checksum
	 * </ul>
	 * @param strValues
	 */
	void parseRMB(String[] strValues) {
		if (strValues[1].equals("A")) { //$NON-NLS-1$
			int velocity;
			try {
				velocity = this.values[7] == 0 ? (int) (Double.parseDouble(strValues[12].trim()) * 1852.0) : this.values[7];
			}
			catch (Exception e) {
				velocity = this.values[7];
			}

			//GPS 
			//this.values[0]  = latitude;
			//this.values[1]  = longitude;
			//this.values[2]  = altitudeAbs;
			//this.values[3]  = numSatelites;
			//this.values[4] = PDOP; // (dilution of precision) 
			//this.values[5] = HDOP; // (horizontal dilution of precision) 
			//this.values[6] = VDOP; // (vertical dilution of precision)
			this.values[7]  = velocity;
			//this.values[8]  = magneticVariation; // SM GPS-Logger -> altitudeRel;
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
		final String STRING_GLIDE_RATIO_UNIT = "m/1"; //$NON-NLS-1$
		for (int i = 0; i < strValues.length && i < 7; i++) {
			try {
				String[] values = strValues[i + 1].trim().split(NMEAParser.STRING_SENTENCE_SPLITTER);
				if (i != 6) {
					this.values[8 + i] = (int) (Double.parseDouble(values[0]) * 1000.0);
					if (!this.device.getMeasurement(this.channelConfigNumber, 8 + i).getUnit().equals(values[1])) {
						this.device.getMeasurement(this.channelConfigNumber, 8 + i).setUnit(values[1].contains(GDE.STRING_STAR) ? values[1].substring(0, values[1].indexOf(GDE.STRING_STAR)) : values[1]);
					}
				}
				else {
					this.values[8 + i] = (int) (Double.parseDouble(values[1]) * 1000.0);
					if (!this.device.getMeasurement(this.channelConfigNumber, 8 + i).getUnit().equals(STRING_GLIDE_RATIO_UNIT)) {
						this.device.getMeasurement(this.channelConfigNumber, 8 + i).setUnit(STRING_GLIDE_RATIO_UNIT);
					}
				}
			}
			catch (Exception e) {
				// ignore and leave value unchanged
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
		//this.values[7]  = velocity;
		//SMGPS
		//this.values[8]  = altitudeRel;
		//this.values[9]  = climb;
		//this.values[10] = voltageRx;
		//this.values[11] = distanceTotal;
		//this.values[12] = distanceStart;
		//this.values[13] = directionStart;
		//this.values[14] = glideRatio;
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
		for (int i = 0; i < strValues.length && i < 9; i++) {
			try {
				String[] values = strValues[i + 1].trim().split(GDE.STRING_BLANK);
				this.values[15 + i] = (int) (Double.parseDouble(values[0]) * 1000.0);
				if (!this.device.getMeasurement(this.channelConfigNumber, 15 + i).getUnit().equals(values[1])) {
					this.device.getMeasurement(this.channelConfigNumber, 15 + i).setUnit(values[1].contains(GDE.STRING_STAR) ? values[1].substring(0, values[1].indexOf(GDE.STRING_STAR)) : values[1]);
				}
			}
			catch (Exception e) {
				// ignore and leave value unchanged
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
		//this.values[7]  = velocity;
		//SMGPS
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
				String[] values = strValues[i].trim().split(NMEAParser.STRING_SENTENCE_SPLITTER);
				int address = Integer.parseInt(values[0]);
				this.values[24 + address] = (int) (Double.parseDouble(values[2]) * 1000.0);
				if (!this.device.getMeasurement(this.channelConfigNumber, 24 + address).getUnit().equals(values[3])) {
					this.device.getMeasurement(this.channelConfigNumber, 24 + address).setUnit(values[3].contains(GDE.STRING_STAR) ? values[3].substring(0, values[3].indexOf(GDE.STRING_STAR)) : values[3]);
				}
			}
			catch (Exception e) {
				// ignore and leave value unchanged
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
		//this.values[7]  = velocity;
		//SMGPS
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
		return this.time_ms;
	}

	/**
	 * @return the values
	 */
	public int[] getValues() {
		return this.values;
	}

	/**
	 * @return the date
	 */
	public Date getDate() {
		return this.date;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return this.comment != null ? this.comment : GDE.STRING_EMPTY;
	}
}
