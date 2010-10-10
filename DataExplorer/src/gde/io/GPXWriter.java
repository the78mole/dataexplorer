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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Class to write KML XML files
 * @author Winfried Brügmann
 */
public class GPXWriter {
	static Logger					log			= Logger.getLogger(GPXWriter.class.getName());
	
	static final String header = "<?xml version=\"1.0\" standalone=\"yes\" ?>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "<gpx version=\"1.1\" creator=\"DataExplorer\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">" + GDE.LINE_SEPARATOR; //$NON-NLS-1$

	static final String leader = "<trk>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "<name>%s</name>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "<number>%d</number>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "<trkseg>" + GDE.LINE_SEPARATOR; //$NON-NLS-1$
	
	static final String trkpt = "<trkpt lat=\"%.7f\" lon=\"%.7f\">" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "<ele>%.0f</ele>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "<time>%sT%sZ</time>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "<name>t:%ss   v=%.1fkm/h   h=%.1fm</name>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "</trkpt>"; //$NON-NLS-1$

	static final String trailer ="</trkseg>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "</trk>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "</gpx>" + GDE.LINE_SEPARATOR; //$NON-NLS-1$

	final static DataExplorer			application	= DataExplorer.getInstance();
	final static Channels					channels		= Channels.getInstance();

	/**
	 * write GPX data
	 * @param recordSet
	 * @param filePath
	 * @param ordinalLongitude
	 * @param ordinalLatitude
	 * @param ordinalGPSHeight
	 * @param ordinalVelocity
	 * @param ordinalHeight
	 * @param isRelative
	 * @throws Exception
	 */
	public static void write(RecordSet recordSet, String filePath, int ordinalLongitude, int ordinalLatitude, int ordinalGPSHeight, int ordinalVelocity, int ordinalHeight, boolean isRelative) throws Exception {
		BufferedWriter writer = null;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		StringBuilder	sb = new StringBuilder();
		int gpsHeight0 = 0;
		
		try {
			long startTime = new Date().getTime();
			if (application.getStatusBar() != null) application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0138, new String[] {GDE.FILE_ENDING_GPX, filePath}));
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8")); //$NON-NLS-1$
			IDevice device = DataExplorer.getInstance().getActiveDevice();

			writer.write(header);
			writer.write(String.format(Locale.ENGLISH, leader, recordSet.getName(), 1));

			// write data
			String[] recordNames = recordSet.getRecordNames();
			int recordEntries = recordSet.getRecordDataSize(true);
			int progressCycle = 0;
			if (application.getStatusBar() != null) application.setProgress(progressCycle, sThreadId);
			Record recordLongitude = recordSet.getRecord(recordNames[ordinalLongitude]);
			Record recordLatitude = recordSet.getRecord(recordNames[ordinalLatitude]);
			Record recordGPSHeight = recordSet.getRecord(recordNames[ordinalGPSHeight]);
			Record recordVelocity = recordSet.getRecord(recordNames[ordinalVelocity]);
			Record recordHeight = recordSet.getRecord(recordNames[ordinalHeight]);
			
			//find date in description
			long date = new Date().getTime();
			try {
				String[] arrayDescription = recordSet.getRecordSetDescription().split(GDE.STRING_BLANK);
				int[] intDate = new int[3];
				for (String strDate : arrayDescription) {
					String[] tmp = strDate.replace(GDE.STRING_COMMA, GDE.STRING_EMPTY).replace(GDE.STRING_SEMICOLON, GDE.STRING_EMPTY).split(GDE.STRING_DASH);
					if (tmp.length == 3) {
						intDate = new int[] {Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]), Integer.parseInt(tmp[2])};
						break;
					}
				}
				int[] intTime = new int[3];
				for (String strTime : arrayDescription) {
					String[] tmp = strTime.replace(GDE.STRING_COMMA, GDE.STRING_EMPTY).replace(GDE.STRING_SEMICOLON, GDE.STRING_EMPTY).split(GDE.STRING_COLON);
					if (tmp.length == 3) {
						intTime = new int[] {Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]), Integer.parseInt(tmp[2])};
						break;
					}
				}
				GregorianCalendar calendar = new GregorianCalendar(intDate[0], intDate[1]-1, intDate[2], intTime[0], intTime[1], intTime[2]);
				date = calendar.getTimeInMillis();
			}
			catch (Exception e) {
				// ignore and use previous initialized time
			}

			if (recordLongitude == null || recordLatitude == null || recordGPSHeight == null)
				throw new Exception(Messages.getString(MessageIds.GDE_MSGE0005, new Object[] { Messages.getString(MessageIds.GDE_MSGT0599), recordSet.getChannelConfigName() }));

			boolean isHeight0Calculated = false;
			for (int i = 0; i < recordEntries; i++) {
				if (recordLongitude.get(i) != 0 && recordLatitude.get(i) != 0) {
					if (!isHeight0Calculated) {
						gpsHeight0 = isRelative ? (int)device.translateValue(recordGPSHeight, recordGPSHeight.get(i)/1000.0) : 0;
						isHeight0Calculated = true;
					}
					double height = device.translateValue(recordGPSHeight, recordGPSHeight.get(i) / 1000.0) - gpsHeight0;
					
					// add data entries, translate according device and measurement unit
					sb.append(String.format(Locale.ENGLISH, trkpt, 
							device.translateValue(recordLatitude, recordLatitude.get(i) / 1000.0),
							device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0),
							height < 0 ? 0 : height,
							new SimpleDateFormat("yyyy-MM-dd").format(date + i * 1000), //$NON-NLS-1$
							new SimpleDateFormat("HH:mm:ss").format(date + i * 1000), //$NON-NLS-1$
							recordSet.getTime(i)/1000/10,
							device.translateValue(recordVelocity, recordVelocity.get(i) / 1000.0), 
							device.translateValue(recordHeight, recordHeight.get(i) / 1000.0))).append(GDE.LINE_SEPARATOR);
	
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
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0006, new Object[] { GDE.FILE_ENDING_GPX, filePath, e.getMessage() }));
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
