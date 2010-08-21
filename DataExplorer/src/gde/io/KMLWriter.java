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
    
    Copyright (c) 2010 Winfried Bruegmann
****************************************************************************************/
package gde.io;

import gde.GDE;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Class to write KML XML files
 * @author Winfried Brügmann
 */
public class KMLWriter {
	static Logger					log			= Logger.getLogger(KMLWriter.class.getName());
	
	static final String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + GDE.LINE_SEPARATOR 
		+ "<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\">" + GDE.LINE_SEPARATOR  + GDE.LINE_SEPARATOR;

	static final String leader = "<Placemark>"  + GDE.LINE_SEPARATOR
		+ "\t<name>gx:%s</name>"  + GDE.LINE_SEPARATOR
	  + "\t<LookAt>" + GDE.LINE_SEPARATOR
    + "\t\t<longitude>%.7f</longitude>" + GDE.LINE_SEPARATOR
    + "\t\t<latitude>%.7f</latitude>" + GDE.LINE_SEPARATOR
    + "\t\t<heading>%d</heading>" + GDE.LINE_SEPARATOR
    + "\t\t<tilt>%d</tilt>" + GDE.LINE_SEPARATOR
    + "\t\t<range>%d</range>" + GDE.LINE_SEPARATOR
    + "\t\t<gx:altitudeMode>relativeToSeaFloor</gx:altitudeMode>" + GDE.LINE_SEPARATOR
    + "\t</LookAt>" + GDE.LINE_SEPARATOR
		+ "\t<Style>" + GDE.LINE_SEPARATOR
		+ "\t\t<LineStyle>" + GDE.LINE_SEPARATOR
		+ "\t\t\t<color>%s</color>" + GDE.LINE_SEPARATOR
		+ "\t\t\t<width>%d</width>" + GDE.LINE_SEPARATOR
		+ "\t\t</LineStyle>" + GDE.LINE_SEPARATOR
		+ "\t</Style>" + GDE.LINE_SEPARATOR
		+ "\t<LineString>" + GDE.LINE_SEPARATOR
    + "\t\t<extrude>%d</extrude>" + GDE.LINE_SEPARATOR
    + "\t\t<gx:altitudeMode>relativeToSeaFloor</gx:altitudeMode>" + GDE.LINE_SEPARATOR
    + "\t\t<coordinates>" + GDE.LINE_SEPARATOR;
	
	static final String trailer ="\t\t</coordinates>" + GDE.LINE_SEPARATOR
		+ "\t</LineString>" + GDE.LINE_SEPARATOR
		+ "</Placemark>" + GDE.LINE_SEPARATOR
		+ "</kml>" + GDE.LINE_SEPARATOR;

	final static DataExplorer			application	= DataExplorer.getInstance();
	final static Channels					channels		= Channels.getInstance();

	/**
	 * write data KML file
	 * @param recordSet
	 * @param filePath
	 * @param ordinalLongitude
	 * @param ordinalLatitude
	 * @param ordinalHeight
	 * @param isRelative
	 * @throws Exception
	 */
	public static void write(RecordSet recordSet, String filePath, int ordinalLongitude, int ordinalLatitude, int ordinalHeight, boolean isRelative) throws Exception {
		BufferedWriter writer = null;
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		StringBuilder	sb = new StringBuilder();
		double height0 = 0;
		
		try {
			long startTime = new Date().getTime();
			if (application.getStatusBar() != null) application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0138) + filePath);
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8")); //$NON-NLS-1$
			IDevice device = DataExplorer.getInstance().getActiveDevice();

			writer.write(header);

			// write data
			String[] recordNames = recordSet.getRecordNames();
			int recordEntries = recordSet.getRecordDataSize(true);
			int progressCycle = 0;
			if (application.getStatusBar() != null) application.setProgress(progressCycle, sThreadId);
			Record recordLongitude = recordSet.getRecord(recordNames[ordinalLongitude]);
			Record recordLatitude = recordSet.getRecord(recordNames[ordinalLatitude]);
			Record recordHeight = recordSet.getRecord(recordNames[ordinalHeight]);

			if (recordLongitude == null || recordLatitude == null || recordHeight == null)
				throw new Exception(Messages.getString(MessageIds.GDE_MSGE0005, new Object[] { "Longitude, Latitude, Height", recordSet.getChannelConfigName() }));

			boolean isLeaderWritten = false;
			for (int i = 0; i < recordEntries; i++) {
				if (recordLongitude.get(i) != 0 && recordLatitude.get(i) != 0) {
					if (!isLeaderWritten) {
						// longitude, latitude, heading, tilt, range, lineColor, lineWidth, extrude
						writer.write(String.format(Locale.ENGLISH, leader, recordSet.getName(),
								device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0), 
								device.translateValue(recordLongitude, recordLatitude.get(i) / 1000.0), 
								-50, 70, 1000, "ff00ff00", 2, 0));
						isLeaderWritten = true;
						
						height0 = isRelative ? device.translateValue(recordHeight, recordHeight.get(i) / 1000.0) : 0;
					}
					// add data entries, translate according device and measurement unit
					sb.append(String.format(Locale.ENGLISH, "\t\t\t%.7f,", device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0)))
						.append(String.format(Locale.ENGLISH, "%.7f,", device.translateValue(recordLatitude, recordLatitude.get(i) / 1000.0)))
						.append(String.format(Locale.ENGLISH, "%.0f", device.translateValue(recordHeight, recordHeight.get(i) / 1000.0) - height0)).append(GDE.LINE_SEPARATOR);
	
					writer.write(sb.toString());
					sb = new StringBuilder();

					if (application.getStatusBar() != null && i % 50 == 0) application.setProgress(((++progressCycle * 5000) / recordEntries), sThreadId);
					log.log(Level.FINE, "data line = " + sb.toString()); //$NON-NLS-1$
				}
			}
			sb = null;

			writer.write(trailer);

			log.log(Level.TIME, "KML file = " + filePath + " written successfuly" //$NON-NLS-1$ //$NON-NLS-2$
					+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$

			writer.flush();
			writer.close();
			writer = null;
			if (application.getStatusBar() != null) application.setProgress(100, sThreadId);
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0006, new Object[] { filePath }));
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0007) + e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
		finally {
			if (application.getStatusBar() != null) application.setStatusMessage(GDE.STRING_EMPTY);
			if (writer != null) {
				writer.close();
				writer = null;
			}
		}

	}

}
