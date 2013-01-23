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
    
    Copyright (c) 2010,2011,2012,2013 Winfried Bruegmann
****************************************************************************************/
package gde.io;

import gde.GDE;
import gde.data.Channels;
import gde.data.ObjectData;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.GPSHelper;
import gde.utils.StringHelper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;

/**
 * Class to write KMz XML files
 * @author Winfried Brügmann
 */
public class KMZWriter {
	static Logger							log							= Logger.getLogger(KMZWriter.class.getName());
	
	static final String				ALTITUDE_ABSOLUTE					= "absolute";
	static final String				ALTITUDE_RELATIVE2GROUND	= "relativeToGround";
	static final String				ALTITUDE_CLAMP2GROUNDE		= "clampToGround";

	static final String				header					= "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\">" + GDE.LINE_SEPARATOR + GDE.LINE_SEPARATOR;	//$NON-NLS-1$

	static final String				position				= "<Document>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t<name>%s</name>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t<open>1</open>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t<description>%s</description>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t<LookAt>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<gx:TimeSpan>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t<begin>%sT%sZ</begin>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t<end>%sT%sZ</end>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t</gx:TimeSpan>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<longitude>%.7f</longitude>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<latitude>%.7f</latitude>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t<altitude>0</altitude>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<heading>%d</heading>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<tilt>%d</tilt>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<range>%d</range>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<altitudeMode>relativeToGround</altitudeMode>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t</LookAt>" + GDE.LINE_SEPARATOR;				//$NON-NLS-1$
	
	static final String[]			icons					= {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"};
	static final String				iconsdef 				= "\t<Style id=\"track-%s_n\">" + GDE.LINE_SEPARATOR
																						+ "\t\t<IconStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<scale>0.4</scale>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<heading>%.1f</heading>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<Icon>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<href>track.png</href>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</Icon>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<gx:headingMode>northUp</gx:headingMode>" + GDE.LINE_SEPARATOR
																						+ "\t\t</IconStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t<LabelStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<scale>0</scale>" + GDE.LINE_SEPARATOR
																						+ "\t\t</LabelStyle>" + GDE.LINE_SEPARATOR
																						+ "\t</Style>" + GDE.LINE_SEPARATOR
																						+ "\t<Style id=\"track-%s_h\">" + GDE.LINE_SEPARATOR
																						+ "\t\t<IconStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<scale>1.0</scale>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<heading>%.1f</heading>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<Icon>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<href>track.png</href>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</Icon>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<gx:headingMode>northUp</gx:headingMode>" + GDE.LINE_SEPARATOR
																						+ "\t\t</IconStyle>" + GDE.LINE_SEPARATOR
																						+ "\t</Style>" + GDE.LINE_SEPARATOR
																						+ "\t<StyleMap id=\"track-%s\">" + GDE.LINE_SEPARATOR
																						+ "\t\t<Pair>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<key>normal</key>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<styleUrl>#track-%s_n</styleUrl>" + GDE.LINE_SEPARATOR
																						+ "\t\t</Pair>" + GDE.LINE_SEPARATOR
																						+ "\t<Pair>" + GDE.LINE_SEPARATOR
																						+ "\t\t<key>highlight</key>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<styleUrl>#track-%s_h</styleUrl>" + GDE.LINE_SEPARATOR
																						+ "\t\t</Pair>" + GDE.LINE_SEPARATOR
																						+ "\t</StyleMap>" + GDE.LINE_SEPARATOR;
	
	static final String				statistics 			= "\t<Folder>" + GDE.LINE_SEPARATOR
																						+ "\t\t<name>%s</name>" + GDE.LINE_SEPARATOR
																						+ "\t\t<Snippet maxLines=\"0\"></Snippet>" + GDE.LINE_SEPARATOR
																						+ "\t\t<description><![CDATA[<table>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<tr><td><b>Distance</b> %.1f km </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<tr><td><b>Min Alt</b> %.0f meters </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<tr><td><b>Max Alt</b> %.0f%s meters </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<tr><td><b>Max Speed</b> %.1f km/hour </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<tr><td><b>Avg Speed</b> %.1f km/hour </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<tr><td><b>Start Time</b> %sT %sZ  </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<tr><td><b>End Time</b> %sT %sZ  </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t</table>]]></description>" + GDE.LINE_SEPARATOR
																						+ "\t\t<TimeSpan>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<begin>%sT%sZ</begin>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<end>%sT%sZ</end>" + GDE.LINE_SEPARATOR
																						+ "\t\t</TimeSpan>" + GDE.LINE_SEPARATOR
																						+ "\t</Folder>" + GDE.LINE_SEPARATOR;
	
	static final String				pointsLeader		= "\t<Folder>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<name>Data-Track</name>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<visibility>0</visibility>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<open>0</open>" + GDE.LINE_SEPARATOR;

	static final String				dataPoint				= "\t\t<Placemark>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<visibility>0</visibility>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<name>t:%ds   v=%.1fkm/h   h=%.0fm</name>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<Snippet maxLines=\"0\"></Snippet>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<description><![CDATA[<table>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<tr><td>Longitude: %.7f </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<tr><td>Latitude: %.7f </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<tr><td>Altitude: %.0f meters </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<tr><td>Speed: %.1f km/hour </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<tr><td>Heading: %.1f </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<tr><td>Time: %sT %sZ </td></tr>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</table>]]></description>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<LookAt>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<longitude>%.7f</longitude>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<latitude>%.7f</latitude>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<altitude>%.0f</altitude>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<heading>%.1f</heading>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<tilt>0</tilt>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<range>0</range>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</LookAt>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<TimeStamp><when>%sT%sZ</when></TimeStamp>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<styleUrl>#%s</styleUrl>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<Point>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<altitudeMode>%s</altitudeMode>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<coordinates>%.7f,%.7f,%.0f</coordinates>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</Point>" + GDE.LINE_SEPARATOR
																						+ "\t\t</Placemark>" + GDE.LINE_SEPARATOR;
	
	static final String				emptyStyleMap		= "\t\t<StyleMap id=\"none\">" + GDE.LINE_SEPARATOR
																					 	+ "\t\t\t<Pair>" + GDE.LINE_SEPARATOR
																					 	+ "\t\t\t\t<key>normal</key>" + GDE.LINE_SEPARATOR
																					 	+ "\t\t\t\t<styleUrl>#none_n</styleUrl>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</Pair>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<Pair>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<key>highlight</key>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<styleUrl>#none_n</styleUrl>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</Pair>" + GDE.LINE_SEPARATOR
																						+ "\t\t</StyleMap>" + GDE.LINE_SEPARATOR
																						+ "\t\t<Style id=\"none_n\">" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<IconStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<scale>1</scale>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<heading>0</heading>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<color>000000000</color>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<Icon>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t\t<href>track.png</href>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t</Icon>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<gx:headingMode>northUp</gx:headingMode>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</IconStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t<LabelStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t\t<scale>0</scale>" + GDE.LINE_SEPARATOR
																						+ "\t\t\t</LabelStyle>" + GDE.LINE_SEPARATOR
																						+ "\t\t</Style>" + GDE.LINE_SEPARATOR;

	static final String				pointsTrailer 	= "\t</Folder>" + GDE.LINE_SEPARATOR;
	
	
	
	static final String				speedHeader			= "\t<Folder>" + GDE.LINE_SEPARATOR
																						+ "\t\t<name>Speed-Track</name>" + GDE.LINE_SEPARATOR
																						+ "\t\t<open>0</open>" + GDE.LINE_SEPARATOR;
	
	static final String				speedLeader			= "\t\t\t<Placemark>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t<name>gx:%s</name>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t<Style>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t<LineStyle>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t\t<color>%s</color>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t\t<width>%d</width>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t</LineStyle>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t</Style>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t<LineString>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t<extrude>%d</extrude>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t<gx:altitudeMode>%s</gx:altitudeMode>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t\t<coordinates>" + GDE.LINE_SEPARATOR;																																																	//$NON-NLS-1$

	static final String				speedTrailer		= "\t\t\t\t</coordinates>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t</LineString>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t</Placemark>" + GDE.LINE_SEPARATOR;	//$NON-NLS-1$
	static final String				speedFooter		= "\t</Folder>" + GDE.LINE_SEPARATOR;
	
	static final String				footer					= "</Document>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "</kml>" + GDE.LINE_SEPARATOR;																																																							//$NON-NLS-1$

	final static DataExplorer	application	= DataExplorer.getInstance();
	final static Channels			channels		= Channels.getInstance();

	/**
	 * write data KMZ file containing one KML file
	 * @param kmzFilePath
	 * @param kmlFileName
	 * @param recordSet
	 * @param ordinalLongitude
	 * @param ordinalLatitude
	 * @param ordinalHeight
	 * @param ordinalVelocity
	 * @param ordinalSlope
	 * @param ordinalHeight
	 * @param ordinalTripLength
	 * @param ordinalAzimuth
	 * @param isHeightRelative
	 * @param isClampToGround
	 * @throws Exception
	 */
	public static void write(String kmzFilePath, String kmlFileName, RecordSet recordSet, final int ordinalLongitude, final int ordinalLatitude, final int ordinalHeight, final int ordinalVelocity, final int ordinalSlope, final int ordinalTripLength, final int ordinalAzimuth,
 final boolean isHeightRelative, boolean isClampToGround) throws Exception {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		StringBuilder sb = new StringBuilder();
		double height0 = 0.0, altitudeDelta = 0.0;
		long startTime = new Date().getTime();
		ZipOutputStream zipWriter = null;
		IDevice device = DataExplorer.getInstance().getActiveDevice();

		try {
			if (KMZWriter.application.getStatusBar() != null) KMZWriter.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0138, new String[] { GDE.FILE_ENDING_KMZ, kmzFilePath }));
			File targetFile = new File(kmzFilePath);
			if (targetFile.exists()) {
				if (!targetFile.delete()) log.log(Level.WARNING, kmzFilePath + " could not deleted!");
				if (!targetFile.createNewFile()) log.log(Level.WARNING, kmzFilePath + " could not created!");
			}
			zipWriter = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(targetFile)));
			zipWriter.putNextEntry(new ZipEntry(kmlFileName));

			zipWriter.write(KMZWriter.header.getBytes());

			// write data
			int realDataSize = recordSet.getRecordDataSize(true);
			int dataSize = recordSet.getRecordDataSize(false);
			int progressCycle = 0;
			if (KMZWriter.application.getStatusBar() != null) KMZWriter.application.setProgress(progressCycle, sThreadId);
			Record recordLongitude = recordSet.get(ordinalLongitude);
			Record recordLatitude = recordSet.get(ordinalLatitude);
			Record recordHeight = ordinalHeight < 0 ? null : recordSet.get(ordinalHeight);
			Record recordVelocity = ordinalVelocity < 0 ? null : recordSet.get(ordinalVelocity);
			Record recordSlope = ordinalSlope < 0 ? null : recordSet.get(ordinalSlope);
			String velocityUnit = recordVelocity != null ? recordVelocity.getUnit(): GDE.STRING_EMPTY;

			if (recordLongitude == null || recordLatitude == null)
				throw new Exception(Messages.getString(MessageIds.GDE_MSGE0005, new Object[] { Messages.getString(MessageIds.GDE_MSGT0599), recordSet.getChannelConfigName() }));
			if (recordHeight == null) {
				isClampToGround = true;
				altitudeDelta = 0.0;
			}
			else {
				altitudeDelta = Math.abs(device.translateValue(recordHeight, recordHeight.getMaxValue() / 1000.0) - device.translateValue(recordHeight, recordHeight.getMinValue() / 1000.0));
				isClampToGround = isClampToGround || altitudeDelta < 10;
			}
			String altitudeMode = isClampToGround ? ALTITUDE_CLAMP2GROUNDE : isHeightRelative ? ALTITUDE_RELATIVE2GROUND : ALTITUDE_ABSOLUTE;

			Record recordTripLength = ordinalTripLength < 0 ? null : recordSet.get(ordinalTripLength);
			Vector<Integer> recordAzimuth;
			try {
				if (ordinalAzimuth >= 0) 
					recordAzimuth = recordSet.get(ordinalAzimuth);
				else
					recordAzimuth = GPSHelper.calculateAzimuth(device, recordSet, ordinalLatitude, ordinalLongitude, ordinalHeight);
			}
			catch (Exception e) {
				recordAzimuth = GPSHelper.calculateAzimuth(device, recordSet, ordinalLatitude, ordinalLongitude, ordinalHeight);
			}
			//find date in description
			long date = new Date().getTime();
			try {
				String[] arrayDescription = recordSet.getRecordSetDescription().split(GDE.STRING_BLANK);
				int[] intDate = new int[3];
				for (String strDate : arrayDescription) {
					String[] tmp = strDate.replace(GDE.STRING_COMMA, GDE.STRING_DASH).replace(GDE.STRING_SEMICOLON, GDE.STRING_DASH).replace(GDE.STRING_RETURN, GDE.STRING_DASH).replace(GDE.STRING_NEW_LINE, GDE.STRING_DASH).split(GDE.STRING_DASH);
					if (tmp.length >= 3 && Character.isDigit(tmp[0].charAt(0)) && Character.isDigit(tmp[1].charAt(0)) && Character.isDigit(tmp[2].charAt(0))) {
						intDate = new int[] { Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]), Integer.parseInt(tmp[2]) };
						break;
					}
				}
				int[] intTime = new int[3];
				for (String strTime : arrayDescription) {
					String[] tmp = strTime.replace(GDE.STRING_COMMA, GDE.STRING_COLON).replace(GDE.STRING_SEMICOLON, GDE.STRING_COLON).replace(GDE.STRING_RETURN, GDE.STRING_COLON).replace(GDE.STRING_NEW_LINE, GDE.STRING_COLON).split(GDE.STRING_COLON);
					if (tmp.length >= 3 && Character.isDigit(tmp[0].charAt(0)) && Character.isDigit(tmp[1].charAt(0)) && Character.isDigit(tmp[2].charAt(0))) {
						intTime = new int[] { Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]), Integer.parseInt(tmp[2]) };
						break;
					}
				}
				GregorianCalendar calendar = new GregorianCalendar(intDate[0], intDate[1] - 1, intDate[2], intTime[0], intTime[1], intTime[2]);
				date = calendar.getTimeInMillis();
			}
			catch (Exception e) {
				// ignore and use previous initialized time
			}
			String dateString = new SimpleDateFormat("yyyy-MM-dd").format(date); //$NON-NLS-1$

			boolean isPositionWritten = false;
			double positionLongitude = device.translateValue(recordLongitude, recordLongitude.realGet(1) / 1000.0);
			double positionLatitude = device.translateValue(recordLongitude, recordLatitude.realGet(1) / 1000.0);
			int i = 0;
			for (; i < realDataSize; i++) {
				if (recordLongitude.realGet(i) != 0 && recordLatitude.realGet(i) != 0) {
					if (!isPositionWritten) {
						// longitude, latitude, heading, tilt, range, lineColor, lineWidth, extrude
						positionLongitude = device.translateValue(recordLongitude, recordLongitude.realGet(i) / 1000.0);
						positionLatitude = device.translateValue(recordLongitude, recordLatitude.realGet(i) / 1000.0);
						zipWriter.write(String.format(Locale.ENGLISH, KMZWriter.position, recordSet.getName(), recordSet.getRecordSetDescription(), 
								dateString, new SimpleDateFormat("HH:mm:ss").format(date), 
								dateString, new SimpleDateFormat("HH:mm:ss").format(date + recordSet.getTime_ms(recordLongitude.size() - 1)),
								positionLongitude, positionLatitude, -50, 70, 1000).getBytes());
						isPositionWritten = true;
						height0 = isHeightRelative && !isClampToGround && recordHeight != null ? device.translateValue(recordHeight, recordHeight.realGet(i) / 1000.0) : 0;
						break;
					}
				}
			}
			int startIndex = i;
			int velocityRange = 0;
			int velocityAvg = recordVelocity != null ? (int) device.translateValue(recordVelocity, recordVelocity.getAvgValue() / 1000.0) : 0;
			int velocityLowerLimit = 20;
			int velocityUpperLimit = 100;
			String withinLimitsColor = "ff0000ff"; //$NON-NLS-1$
			String lowerLimitColor = "ff00ff00"; //$NON-NLS-1$
			String upperLimitColor = "ff00ffff"; //$NON-NLS-1$

			if (KMZWriter.application.isObjectoriented()) {
				ObjectData object = KMZWriter.application.getObject();
				Properties properties = object.getProperties();
				if (properties != null) {
					double avgLimitFactor = 0.0;
					int lowerLimitVelocity = 0, upperLimitVelocity = 0;
					try {
						avgLimitFactor = Double.parseDouble(((String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR.value())).trim());
					}
					catch (Exception e) {
						// ignore
					}
					try {
						lowerLimitVelocity = Integer.parseInt(((String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_LOWER_LIMIT.value())).trim());
					}
					catch (Exception e) {
						// ignore
					}
					try {
						upperLimitVelocity = Integer.parseInt(((String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_UPPER_LIMIT.value())).trim());
					}
					catch (Exception e) {
						// ignore
					}
					velocityLowerLimit = (int) (avgLimitFactor != 0 ? velocityAvg / avgLimitFactor : lowerLimitVelocity != 0 ? lowerLimitVelocity : 0);
					velocityUpperLimit = (int) (avgLimitFactor != 0 ? velocityAvg * avgLimitFactor : upperLimitVelocity != 0 ? upperLimitVelocity : 500);

					int r, g, b;
					try {
						String color = (String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_WITHIN_LIMITS_COLOR.value());
						r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
						g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
						b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
						withinLimitsColor = String.format("ff%02x%02x%02x", b, g, r); //$NON-NLS-1$
					}
					catch (Exception e) {
						// ignore
					}
					try {
						String color = (String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_LOWER_LIMIT_COLOR.value());
						r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
						g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
						b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
						lowerLimitColor = String.format("ff%02x%02x%02x", b, g, r); //$NON-NLS-1$
					}
					catch (Exception e) {
						// ignore
					}
					try {
						String color = (String) properties.get(MeasurementPropertyTypes.GOOGLE_EARTH_UPPER_LIMIT_COLOR.value());
						r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
						g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
						b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
						upperLimitColor = String.format("ff%02x%02x%02x", b, g, r); //$NON-NLS-1$
					}
					catch (Exception e) {
						// ignore
					}
				}
			}
			else {
				PropertyType propertyAvg = recordVelocity == null ? null : recordVelocity.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalVelocity,
						MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR.value());
				PropertyType propertyLowerLimit = recordVelocity == null ? null : recordVelocity.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalVelocity,
						MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_LOWER_LIMIT.value());
				PropertyType propertyUpperLimit = recordVelocity == null ? null : recordVelocity.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalVelocity,
						MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_UPPER_LIMIT.value());
				velocityLowerLimit = (int) (propertyAvg != null ? velocityAvg / Double.valueOf(propertyAvg.getValue()) : propertyLowerLimit != null ? Integer.valueOf(propertyLowerLimit.getValue()) : 0);
				velocityUpperLimit = (int) (propertyAvg != null ? velocityAvg * Double.valueOf(propertyAvg.getValue()) : propertyUpperLimit != null ? Integer.valueOf(propertyUpperLimit.getValue()) : 500);

				PropertyType propertyWithinLimitsColor = recordVelocity == null ? null : recordVelocity.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalVelocity,
						MeasurementPropertyTypes.GOOGLE_EARTH_WITHIN_LIMITS_COLOR.value());
				PropertyType propertyLowerLimitColor = recordVelocity == null ? null : recordVelocity.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalVelocity,
						MeasurementPropertyTypes.GOOGLE_EARTH_LOWER_LIMIT_COLOR.value());
				PropertyType propertyUpperLimitColor = recordVelocity == null ? null : recordVelocity.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalVelocity,
						MeasurementPropertyTypes.GOOGLE_EARTH_UPPER_LIMIT_COLOR.value());
				String[] colorRGB = propertyWithinLimitsColor != null ? propertyWithinLimitsColor.getValue().split(GDE.STRING_COMMA) : new String[3];
				withinLimitsColor = propertyWithinLimitsColor != null ? String.format("ff%02x%02x%02x", Integer.parseInt(colorRGB[2]), Integer.parseInt(colorRGB[1]), Integer.parseInt(colorRGB[0])) : "ff0000ff"; //$NON-NLS-1$ //$NON-NLS-2$
				colorRGB = propertyLowerLimitColor != null ? propertyLowerLimitColor.getValue().split(GDE.STRING_COMMA) : new String[3];
				lowerLimitColor = propertyLowerLimitColor != null ? String.format("ff%02x%02x%02x", Integer.parseInt(colorRGB[2]), Integer.parseInt(colorRGB[1]), Integer.parseInt(colorRGB[0])) : "ff00ff00"; //$NON-NLS-1$ //$NON-NLS-2$
				colorRGB = propertyUpperLimitColor != null ? propertyUpperLimitColor.getValue().split(GDE.STRING_COMMA) : new String[3];
				upperLimitColor = propertyUpperLimitColor != null ? String.format("ff%02x%02x%02x", Integer.parseInt(colorRGB[2]), Integer.parseInt(colorRGB[1]), Integer.parseInt(colorRGB[0])) : "ff00ffff"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			String[] velocityColors = new String[] { lowerLimitColor, withinLimitsColor, upperLimitColor };

			//write track statistics
			double height = !isClampToGround && recordHeight != null ? device.translateValue(recordHeight, recordHeight.get(i) / 1000.0) - height0 : 0;
			zipWriter.write(String.format(Locale.ENGLISH, statistics, "Statistics", recordTripLength == null ? 0 : device.translateValue(recordTripLength, recordTripLength.getMaxValue() / 1000.0),
					height, recordHeight == null ? 0 : device.translateValue(recordHeight, recordHeight.getMaxValue() / 1000.0) - height0, 
					isHeightRelative ? "" : String.format("(%.0f)", (recordHeight == null ? 0.0f : device.translateValue(recordHeight, recordHeight.getMaxValue() / 1000.0) - height)),
					recordVelocity == null ? 0 : device.translateValue(recordVelocity, recordVelocity.getMaxValue() / 1000.0), recordVelocity == null ? 0 : device.translateValue(recordVelocity, recordVelocity.getAvgValue() / 1000.0), dateString,
					new SimpleDateFormat("HH:mm:ss").format(date), dateString, new SimpleDateFormat("HH:mm:ss").format(date + recordSet.getTime_ms(recordLongitude.size() - 1)), dateString,
					new SimpleDateFormat("HH:mm:ss").format(date), dateString, new SimpleDateFormat("HH:mm:ss").format(date + recordSet.getTime_ms(recordLongitude.size() - 1))).getBytes());

			//speed-track
			zipWriter.write(KMZWriter.speedHeader.getBytes());
			String initialPlacemarkName = Messages.getString(MessageIds.GDE_MSGT0604, new Object[] { velocityLowerLimit, velocityUnit });
			zipWriter.write(String.format(Locale.ENGLISH, KMZWriter.speedLeader, initialPlacemarkName, lowerLimitColor, 2, 0, altitudeMode).getBytes());
			long lastTimeStamp = -1, timeStamp;
			long recordSetStartTimeStamp = recordSet.getTime(recordSet.isZoomMode() ? 0 : startIndex) / 10;
			for (i = recordSet.isZoomMode() ? 0 : startIndex; isPositionWritten && i < dataSize; i++) {
				timeStamp = recordSet.getTime(i) / 10 + recordSetStartTimeStamp;
				if ((timeStamp - lastTimeStamp) >= 500 || lastTimeStamp == -1) {// write a point all ~1000 ms
					int velocity = recordVelocity == null ? 0 : (int) device.translateValue(recordVelocity, recordVelocity.get(i) / 1000.0);
					if (!((velocity < velocityLowerLimit && velocityRange == 0) || (velocity >= velocityLowerLimit && velocity <= velocityUpperLimit && velocityRange == 1) || (velocity > velocityUpperLimit && velocityRange == 2))) {
						velocityRange = switchColor(zipWriter, recordVelocity, velocity, velocityLowerLimit, velocityUpperLimit, velocityColors, velocityRange, velocityUnit, altitudeMode);
	
						//re-write last coordinates
//						height = device.translateValue(recordHeight, recordHeight.get(i - 1) / 1000.0) - height0;
						// add data entries, translate according device and measurement unit
//						sb.append(String.format(Locale.ENGLISH, "\t\t\t\t\t\t%.7f,", device.translateValue(recordLongitude, recordLongitude.get(i - 1) / 1000.0))) //$NON-NLS-1$
//								.append(String.format(Locale.ENGLISH, "%.7f,", device.translateValue(recordLatitude, recordLatitude.get(i - 1) / 1000.0))) //$NON-NLS-1$
//								.append(String.format(Locale.ENGLISH, "%.0f", height < 0 ? 0 : height)).append(GDE.LINE_SEPARATOR); //$NON-NLS-1$
	
						zipWriter.write(sb.toString().getBytes());
					}
					sb = new StringBuilder();
	
					height = recordHeight == null ? 0 : (device.translateValue(recordHeight, recordHeight.get(i) / 1000.0) - height0);
					// add data entries, translate according device and measurement unit
					sb.append(String.format(Locale.ENGLISH, "\t\t\t\t\t\t%.7f,", device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0))) //$NON-NLS-1$
							.append(String.format(Locale.ENGLISH, "%.7f,", device.translateValue(recordLatitude, recordLatitude.get(i) / 1000.0))) //$NON-NLS-1$
							.append(String.format(Locale.ENGLISH, "%.0f", height < 0 ? 0 : height)).append(GDE.LINE_SEPARATOR); //$NON-NLS-1$
	
					zipWriter.write(sb.toString().getBytes());
	
					if (KMZWriter.application.getStatusBar() != null && i % 50 == 0) KMZWriter.application.setProgress(((++progressCycle * 5000) / dataSize), sThreadId);
					KMZWriter.log.log(java.util.logging.Level.FINER, "data line = " + sb.toString()); //$NON-NLS-1$
					lastTimeStamp = timeStamp;
				}
			}
			zipWriter.write(KMZWriter.speedTrailer.getBytes());
			zipWriter.write(KMZWriter.speedFooter.getBytes());

			//data-track
			zipWriter.write(KMZWriter.pointsLeader.getBytes());
			lastTimeStamp = -1;
			for (i = recordSet.isZoomMode() ? 0 : startIndex; isPositionWritten && i < dataSize; i++) {
				timeStamp = recordSet.getTime(i) / 10 + recordSetStartTimeStamp;
				if ((timeStamp - lastTimeStamp) >= 500 || lastTimeStamp == -1) {// write a point all ~1000 ms
					double speed = recordVelocity == null ? 0 : device.translateValue(recordVelocity, recordVelocity.get(i) / 1000.0);
					double slope = recordSlope == null ? 0 : device.translateValue(recordSlope, recordSlope.get(i) / 1000.0);
					double slopeLast = i==0 ? slope : recordSlope == null ? 0 : device.translateValue(recordSlope, recordSlope.get(i-1) / 1000.0);
					boolean isSlope0 = speed > 2 && ((slope <= 0 && slopeLast > 0) || (slope > 0 && slopeLast <= 0) || slope == 0);
					height = recordHeight == null ? 0 : (device.translateValue(recordHeight, recordHeight.get(i) / 1000.0) - height0);
					zipWriter.write(String.format(Locale.ENGLISH, dataPoint, 
							recordSet.getTime(i) / 1000 / 10, speed, height, 
							device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0), device.translateValue(recordLatitude, recordLatitude.get(i) / 1000.0), height, speed, recordAzimuth.get(i) / 1000.0, 
							dateString, new SimpleDateFormat("HH:mm:ss").format(date + recordSet.getTime_ms(i)),
							device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0), device.translateValue(recordLatitude, recordLatitude.get(i) / 1000.0),	height, recordAzimuth.get(i) / 1000.0, 
							dateString, new SimpleDateFormat("HH:mm:ss").format(date + recordSet.getTime_ms(i)),
							isSlope0 ? getTrackIcon(recordAzimuth.get(i) / 1000.0) : "none", 
							altitudeMode,
							device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0), device.translateValue(recordLatitude, recordLatitude.get(i) / 1000.0), 
							height < 0 ? 0 : height).getBytes()); //correct visualization while height < 0
					
					lastTimeStamp = timeStamp;
				}
			}
			zipWriter.write(pointsTrailer.getBytes());
			
			//write track icons style definition
			for (int j = 0; j < icons.length; j++) {
				zipWriter.write(String.format(Locale.ENGLISH, KMZWriter.iconsdef, icons[j], (22.5*j), icons[j], (22.5*j), icons[j], icons[j], icons[j]).getBytes());
			}
			zipWriter.write(emptyStyleMap.getBytes());
			
			zipWriter.write(KMZWriter.footer.getBytes());
			
			zipWriter.closeEntry();
			
			// save the track icon
			ImageLoader imageLoader = new ImageLoader();
			imageLoader.data = new ImageData[] { SWTResourceManager.getImage("gde/resource/track.png").getImageData() };
			zipWriter.putNextEntry(new ZipEntry("track.png"));
			imageLoader.save(zipWriter, SWT.IMAGE_PNG);
			zipWriter.closeEntry();
			
			zipWriter.flush();
			zipWriter.close();
			zipWriter = null;
		}
		catch (IOException e) {
			KMZWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0006, new Object[] { GDE.FILE_ENDING_KMZ, kmzFilePath, e.getMessage() }));
		}
		catch (Exception e) {
			KMZWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0007) + e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
		finally {
			if (KMZWriter.application.getStatusBar() != null) KMZWriter.application.setStatusMessage(GDE.STRING_EMPTY);
			if (zipWriter != null) {
				try {
					zipWriter.close();
				}
				catch (IOException e) {
					log.log(Level.WARNING, e.getMessage(), e);
				}
				zipWriter = null;
			}
		}
		if (KMZWriter.application.getStatusBar() != null) {
			KMZWriter.application.setProgress(100, sThreadId);
			KMZWriter.application.setStatusMessage(GDE.STRING_EMPTY);
		}
		if (log.isLoggable(Level.TIME)) log.log(Level.TIME, "KML file = " + kmzFilePath + " written successfuly" //$NON-NLS-1$ //$NON-NLS-2$
				+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * return the fitting icon to the given azimut value
	 * @param tmpAzimuth
	 * @return
	 */
	private static String getTrackIcon(double tmpAzimuth) {
		String trackIcon = "track-" + icons[0];
		if (tmpAzimuth >= 348.75 && tmpAzimuth <= 360 || tmpAzimuth >= 0 && tmpAzimuth < 11.25)
			trackIcon = "track-" + icons[0];
		else if (tmpAzimuth >= 11.25 && tmpAzimuth < 33.75)
			trackIcon = "track-" + icons[1];
		else if (tmpAzimuth >= 33.75 && tmpAzimuth < 56.25)
			trackIcon = "track-" + icons[2];
		else if (tmpAzimuth >= 56.25 && tmpAzimuth < 78.75)
			trackIcon = "track-" + icons[3];
		else if (tmpAzimuth >= 78.75 && tmpAzimuth < 101.25)
			trackIcon = "track-" + icons[4];
		else if (tmpAzimuth >= 101.25 && tmpAzimuth < 123.75)
			trackIcon = "track-" + icons[5];
		else if (tmpAzimuth >= 123.75 && tmpAzimuth < 146.25)
			trackIcon = "track-" + icons[6];
		else if (tmpAzimuth >= 146.25 && tmpAzimuth < 168.75) 
			trackIcon = "track-" + icons[7];
		else if (tmpAzimuth >= 168.75 && tmpAzimuth < 191.25) 
			trackIcon = "track-" + icons[8];
		else if (tmpAzimuth >= 191.25 && tmpAzimuth < 213.75) 
			trackIcon = "track-" + icons[9];
		else if (tmpAzimuth >= 213.75 && tmpAzimuth < 236.25) 
			trackIcon = "track-" + icons[10];
		else if (tmpAzimuth >= 236.25 && tmpAzimuth < 258.75) 
			trackIcon = "track-" + icons[11];
		else if (tmpAzimuth >= 258.75 && tmpAzimuth < 281.25) 
			trackIcon = "track-" + icons[12];
		else if (tmpAzimuth >= 281.25 && tmpAzimuth < 303.75) 
			trackIcon = "track-" + icons[13];
		else if (tmpAzimuth >= 303.75 && tmpAzimuth < 326.25) 
			trackIcon = "track-" + icons[14];
		else if (tmpAzimuth >= 326.25 && tmpAzimuth < 348.75) 
			trackIcon = "track-" + icons[15];
		
		return trackIcon;
	}

	/**
		velocityRange = 0; // 0 = <= 30; 1 = <= 70; 2 = <= 90; 3 = > 120
	 * @param writer
	 * @param velocityRecord
	 * @param actualVelocity
	 * @param velocityLowerLimit
	 * @param velocityUpperLimit
	 * @param velocitColors {lowerLimitColor, withinLimitsColor, upperLimitColor) 
	 * @param velocityRange
	 * @param velocityUnit
	 * @param altitudeMode
	 * @return
	 * @throws IOException
	 */
	public static int switchColor(ZipOutputStream writer, Record velocityRecord, int actualVelocity, int velocityLowerLimit, int velocityUpperLimit, String[] velocitColors, int velocityRange,
			String velocityUnit, String altitudeMode) throws IOException {

		if (actualVelocity < velocityLowerLimit) {
			String placemarkName0 = Messages.getString(MessageIds.GDE_MSGT0604, new Object[] { velocityLowerLimit, velocityUnit });
			writer.write(KMZWriter.speedTrailer.getBytes());
			writer.write(String.format(Locale.ENGLISH, KMZWriter.speedLeader, placemarkName0, velocitColors[0], 2, 0, altitudeMode).getBytes());
			velocityRange = 0;
		}
		else if (actualVelocity >= velocityLowerLimit && actualVelocity <= velocityUpperLimit) {
			String placemarkName1 = Messages.getString(MessageIds.GDE_MSGT0605, new Object[] { velocityLowerLimit, velocityUpperLimit, velocityUnit });
			writer.write(KMZWriter.speedTrailer.getBytes());
			writer.write(String.format(Locale.ENGLISH, KMZWriter.speedLeader, placemarkName1, velocitColors[1], 2, 0, altitudeMode).getBytes());
			velocityRange = 1;
		}
		else if (actualVelocity > velocityUpperLimit) {
			String placemarkName2 = Messages.getString(MessageIds.GDE_MSGT0606, new Object[] { velocityUpperLimit, velocityUnit });
			writer.write(KMZWriter.speedTrailer.getBytes());
			writer.write(String.format(Locale.ENGLISH, KMZWriter.speedLeader, placemarkName2, velocitColors[2], 2, 0, altitudeMode).getBytes());
			velocityRange = 2;
		}
		return velocityRange;
	}

}
