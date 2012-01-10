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
    
    Copyright (c) 2012 Winfried Bruegmann
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
import gde.utils.GPSHelper;
import gde.utils.StringHelper;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Class to write IGC conform files
 * @author Winfried Brügmann
 */
public class IGCWriter {
	static Logger							log					= Logger.getLogger(IGCWriter.class.getName());

	final static DataExplorer	application	= DataExplorer.getInstance();
	final static Channels			channels		= Channels.getInstance();

	/**
	 * write the IGC header and way points
	 * @param device
	 * @param igcFilePath
	 * @param header
	 * @param recordSet
	 * @param ordinalLongitude
	 * @param ordinalLatitude
	 * @param ordinalAltitude
	 * @param startAltitude
	 * @param offsetUTC
	 * @throws Exception
	 */
	public static void write(IDevice device, String igcFilePath, StringBuilder header, RecordSet recordSet, final int ordinalLongitude, final int ordinalLatitude, final int ordinalAltitude,
			final int startAltitude, final int offsetUTC) throws Exception {
		BufferedWriter writer;
		StringBuilder content = new StringBuilder().append(header);
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		long startTime = new Date().getTime();

		try {
			if (IGCWriter.application.getStatusBar() != null) IGCWriter.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0138, new String[] { GDE.FILE_ENDING_IGC, igcFilePath }));

			if (recordSet != null) {
				int startIndex = GPSHelper.getStartIndexGPS(recordSet, ordinalLatitude, ordinalLongitude);
				Record recordLatitude = recordSet.get(ordinalLatitude);
				Record recordLongitude = recordSet.get(ordinalLongitude);
				Record recordAlitude = recordSet.get(ordinalAltitude);
				String latitudeNS = recordLatitude.get(startIndex) > 0 ? "N" : "S"; //$NON-NLS-1$ //$NON-NLS-2$
				String longitudeEW = recordLongitude.get(startIndex) > 0 ? "E" : "W"; //$NON-NLS-1$ //$NON-NLS-2$
				SimpleDateFormat sdf = new SimpleDateFormat("HHmmss"); //$NON-NLS-1$
				int offsetHeight = (int) (startAltitude - device.translateValue(recordAlitude, recordAlitude.get(startIndex) / 1000.0));
				String fixValidity = offsetHeight == 0 ? "A" : "V"; //$NON-NLS-1$ //$NON-NLS-2$

				long lastTimeStamp = -1, timeStamp;
				long recordSetStatTimeStamp = recordSet.getStartTimeStamp() - offsetUTC * 3600000;
				for (int i = startIndex; i < recordSet.get(ordinalLongitude).realSize(); i++) {
					// absolute time as recorded, needs to be converted into UTC
					timeStamp = recordSet.getTime(i) / 10 + recordSetStatTimeStamp;
					if ((timeStamp - lastTimeStamp) >= 950 || lastTimeStamp == -1) {
						content.append(String.format("B%s%07d%s%08d%s%s", sdf.format(timeStamp - 3600000), recordLatitude.get(i) / 10, latitudeNS, recordLongitude.get(i) / 10, longitudeEW, fixValidity)); //$NON-NLS-1$
						double altitude = device.translateValue(recordAlitude, recordAlitude.get(i) / 1000.0);
						if (altitude >= 0)
							content.append(String.format("%05.0f%05.0f\r\n", altitude + offsetHeight, altitude + offsetHeight)); //$NON-NLS-1$
						else
							content.append(String.format("-%04.0f%05.0f\r\n", altitude + offsetHeight, 0.0)); //$NON-NLS-1$

						lastTimeStamp = timeStamp;
					}
				}
			}

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(igcFilePath), "ISO-8859-1")); //$NON-NLS-1$
			writer.append(content.toString());
			writer.flush();
			writer.close();
			writer = null;
			//recordSet.setSaved(true);
			if (IGCWriter.application.getStatusBar() != null) IGCWriter.application.setProgress(100, sThreadId);
		}
		catch (Exception e) {
			IGCWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0007) + e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
		finally {
			if (IGCWriter.application.getStatusBar() != null) IGCWriter.application.setStatusMessage(GDE.STRING_EMPTY);
		}
		if (log.isLoggable(Level.TIME)) log.log(Level.TIME, "IGC file = " + igcFilePath + " written successfuly" //$NON-NLS-1$ //$NON-NLS-2$
				+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$
	}
}
