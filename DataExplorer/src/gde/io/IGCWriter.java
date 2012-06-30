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
			final int startAltitude) throws Exception {
		BufferedWriter writer;
		StringBuilder content = new StringBuilder().append(header);
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		long startTime = new Date().getTime();

		try {
			if (IGCWriter.application.getStatusBar() != null) IGCWriter.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0138, new String[] { GDE.FILE_ENDING_IGC, igcFilePath }));

			if (recordSet != null) {
				int startIndex = GPSHelper.getStartIndexGPS(recordSet, ordinalLatitude, ordinalLongitude);
				Record recordAlitude = recordSet.get(ordinalAltitude);
				SimpleDateFormat sdf = new SimpleDateFormat("HHmmss"); //$NON-NLS-1$
				int offsetHeight = (int) (startAltitude - device.translateValue(recordAlitude, recordAlitude.get(startIndex) / 1000.0));
				char fixValidity = offsetHeight == 0 ? 'A' : 'V'; //$NON-NLS-1$ //$NON-NLS-2$
				long lastTimeStamp = -1, timeStamp;
				long recordSetStartTimeStamp = recordSet.getStartTimeStamp();
				log.log(Level.TIME, "start time stamp = " + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", recordSetStartTimeStamp));
				
				for (int i = startIndex; startIndex >= 0 && i < recordSet.get(ordinalLongitude).realSize(); i++) {
					// absolute time as recorded, needs to be converted into UTC
					timeStamp = recordSet.getTime(i) / 10 + recordSetStartTimeStamp;
					if ((timeStamp - lastTimeStamp) >= 950 || lastTimeStamp == -1) {
						content.append(String.format("B%s%s\r\n", sdf.format(timeStamp), device.translateGPS2IGC(recordSet, i, fixValidity, startAltitude, offsetHeight))); //$NON-NLS-1$

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
		catch (RuntimeException e) {
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
