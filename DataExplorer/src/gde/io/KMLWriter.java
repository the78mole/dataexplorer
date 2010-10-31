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
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
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
	
	static final String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + GDE.LINE_SEPARATOR  //$NON-NLS-1$
		+ "<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\">" + GDE.LINE_SEPARATOR  + GDE.LINE_SEPARATOR; //$NON-NLS-1$

	static final String position = "<Document>"  + GDE.LINE_SEPARATOR //$NON-NLS-1$
	+ "\t<name>gx:%s</name>"  + GDE.LINE_SEPARATOR //$NON-NLS-1$
  + "\t<LookAt>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
  + "\t\t<longitude>%.7f</longitude>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
  + "\t\t<latitude>%.7f</latitude>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
  + "\t\t<heading>%d</heading>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
  + "\t\t<tilt>%d</tilt>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
  + "\t\t<range>%d</range>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
  + "\t\t<gx:altitudeMode>relativeToGround</gx:altitudeMode>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
  + "\t</LookAt>" + GDE.LINE_SEPARATOR; //$NON-NLS-1$
	
	static final String leader = "<Placemark>"  + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "\t<name>gx:%s</name>"  + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "\t<Style>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "\t\t<LineStyle>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "\t\t\t<color>%s</color>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "\t\t\t<width>%d</width>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "\t\t</LineStyle>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "\t</Style>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "\t<LineString>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
    + "\t\t<extrude>%d</extrude>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
    + "\t\t<gx:altitudeMode>relativeToGround</gx:altitudeMode>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
    + "\t\t<coordinates>" + GDE.LINE_SEPARATOR; //$NON-NLS-1$
	
	static final String trailer ="\t\t</coordinates>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "\t</LineString>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
  	+ "</Placemark>" + GDE.LINE_SEPARATOR; //$NON-NLS-1$
	
		static final String footer ="</Document>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
		+ "</kml>" + GDE.LINE_SEPARATOR; //$NON-NLS-1$

	final static DataExplorer			application	= DataExplorer.getInstance();
	final static Channels					channels		= Channels.getInstance();

	/**
	 * write data KML file
	 * @param recordSet
	 * @param filePath
	 * @param ordinalLongitude
	 * @param ordinalLatitude
	 * @param ordinalHeight
	 * @param ordinalVelocity
	 * @param isHeightRelative
	 * @throws Exception
	 */
	public static void write(RecordSet recordSet, String filePath, final int ordinalLongitude, final int ordinalLatitude, final int ordinalHeight, final int ordinalVelocity, final boolean isHeightRelative) throws Exception {
		BufferedWriter writer = null;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		StringBuilder	sb = new StringBuilder();
		double height0 = 0;
		
		try {
			long startTime = new Date().getTime();
			if (application.getStatusBar() != null) application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0138, new String[] {GDE.FILE_ENDING_KML, filePath}));
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8")); //$NON-NLS-1$
			IDevice device = DataExplorer.getInstance().getActiveDevice();

			writer.write(header);

			// write data
			String[] recordNames = recordSet.getRecordNames();
			int realDataSize = recordSet.getRecordDataSize(true);
			int dataSize = recordSet.getRecordDataSize(false);
			int progressCycle = 0;
			if (application.getStatusBar() != null) application.setProgress(progressCycle, sThreadId);
			Record recordLongitude = recordSet.getRecord(recordNames[ordinalLongitude]);
			Record recordLatitude = recordSet.getRecord(recordNames[ordinalLatitude]);
			Record recordHeight = recordSet.getRecord(recordNames[ordinalHeight]);
			Record recordVelocity = recordSet.getRecord(recordNames[ordinalVelocity]);
			String velocityUnit = recordVelocity.getUnit();

			if (recordLongitude == null || recordLatitude == null || recordHeight == null)
				throw new Exception(Messages.getString(MessageIds.GDE_MSGE0005, new Object[] { Messages.getString(MessageIds.GDE_MSGT0599), recordSet.getChannelConfigName() })); //$NON-NLS-1$

			boolean isPositionWritten = false;
			int i = 0;
			for (; i < realDataSize; i++) {
				if (recordLongitude.realGet(i) != 0 && recordLatitude.realGet(i) != 0) {
					if (!isPositionWritten) {
						// longitude, latitude, heading, tilt, range, lineColor, lineWidth, extrude
						writer.write(String.format(Locale.ENGLISH, position, recordSet.getName(),
								device.translateValue(recordLongitude, recordLongitude.realGet(i) / 1000.0), 
								device.translateValue(recordLongitude, recordLatitude.realGet(i) / 1000.0), 
								-50, 70, 1000)); 
						isPositionWritten = true;
						
						height0 = isHeightRelative ? device.translateValue(recordHeight, recordHeight.realGet(i) / 1000.0) : 0;
						break;
					}
				}
			}
			int velocityRange = 0; 
			int velocityAvg = recordVelocity.getAvgValue()/1000;
			PropertyType propertyAvg = recordVelocity.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalVelocity, MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR.value());
			PropertyType propertyLowerLimit = recordVelocity.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalVelocity, MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_LOWER_LIMIT.value());
			PropertyType propertyUpperLimit = recordVelocity.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalVelocity, MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_UPPER_LIMIT.value());
			int velocityLowerLimit = (int) (propertyAvg != null ? velocityAvg/Double.valueOf(propertyAvg.getValue()) : propertyLowerLimit != null ? Integer.valueOf(propertyLowerLimit.getValue()) : 0);
			int velocityUpperLimit = (int) (propertyAvg != null ? velocityAvg*Double.valueOf(propertyAvg.getValue()) : propertyLowerLimit != null ? Integer.valueOf(propertyUpperLimit.getValue()) : 500);;			
			String initialPlacemarkName = Messages.getString(MessageIds.GDE_MSGT0604, new Object[] {velocityLowerLimit, velocityUnit});
			writer.write(String.format(Locale.ENGLISH, leader, initialPlacemarkName, "ff0000ff", 2, 0));//$NON-NLS-1$		
			for (i = recordSet.isZoomMode() ? 0 : i; isPositionWritten && i < dataSize; i++) {
				
				int velocity = (int)device.translateValue(recordVelocity, recordVelocity.get(i) / 1000.0);
				if (!((velocity < velocityLowerLimit && velocityRange == 0) || (velocity >= velocityLowerLimit && velocity <= velocityUpperLimit && velocityRange == 1) || (velocity > velocityUpperLimit && velocityRange == 2))) {
					velocityRange = switchColor(writer, recordVelocity, velocity, velocityLowerLimit, velocityUpperLimit, velocityRange, velocityUnit);

					//re-write last coordinates
					double height = device.translateValue(recordHeight, recordHeight.get(i-1) / 1000.0) - height0;
					// add data entries, translate according device and measurement unit
					sb.append(String.format(Locale.ENGLISH, "\t\t\t%.7f,", device.translateValue(recordLongitude, recordLongitude.get(i-1) / 1000.0))) //$NON-NLS-1$
						.append(String.format(Locale.ENGLISH, "%.7f,", device.translateValue(recordLatitude, recordLatitude.get(i-1) / 1000.0))) //$NON-NLS-1$
						.append(String.format(Locale.ENGLISH, "%.0f", height < 0 ? 0 : height)).append(GDE.LINE_SEPARATOR); //$NON-NLS-1$

					writer.write(sb.toString());
				}

				double height = device.translateValue(recordHeight, recordHeight.get(i) / 1000.0) - height0;
				// add data entries, translate according device and measurement unit
				sb.append(String.format(Locale.ENGLISH, "\t\t\t%.7f,", device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0))) //$NON-NLS-1$
					.append(String.format(Locale.ENGLISH, "%.7f,", device.translateValue(recordLatitude, recordLatitude.get(i) / 1000.0))) //$NON-NLS-1$
					.append(String.format(Locale.ENGLISH, "%.0f", height < 0 ? 0 : height)).append(GDE.LINE_SEPARATOR); //$NON-NLS-1$

				writer.write(sb.toString());

				if (application.getStatusBar() != null && i % 50 == 0) application.setProgress(((++progressCycle * 5000) / dataSize), sThreadId);
				log.log(Level.FINER, "data line = " + sb.toString()); //$NON-NLS-1$
				sb = new StringBuilder();
			}
			writer.write(trailer);

			writer.write(footer);

			log.log(Level.TIME, "KML file = " + filePath + " written successfuly" //$NON-NLS-1$ //$NON-NLS-2$
					+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$

			writer.flush();
			writer.close();
			writer = null;
			if (application.getStatusBar() != null) application.setProgress(100, sThreadId);
		}
		catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0006, new Object[] { GDE.FILE_ENDING_KML, filePath, e.getMessage() }));
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

	/**
		velocityRange = 0; // 0 = <= 30; 1 = <= 70; 2 = <= 90; 3 = > 120
	 * @param writer
	 * @param velocityRecord
	 * @param actualVelocity
	 * @param velocityRange
	 * @param velocityUnit
	 * @return
	 * @throws IOException
	 */
	public static int switchColor(BufferedWriter writer, Record velocityRecord, int actualVelocity, int velocityLowerLimit, int velocityUpperLimit, int velocityRange, String velocityUnit)
			throws IOException {

		if (actualVelocity < velocityLowerLimit) {
			String placemarkName0 = Messages.getString(MessageIds.GDE_MSGT0604, new Object[] { velocityLowerLimit, velocityUnit });
			writer.write(trailer);
			writer.write(String.format(Locale.ENGLISH, leader, placemarkName0, "ff0000ff", 2, 0));//$NON-NLS-1$		
			velocityRange = 0;
		}
		else if (actualVelocity >= velocityLowerLimit && actualVelocity <= velocityUpperLimit) {
			String placemarkName1 = Messages.getString(MessageIds.GDE_MSGT0605, new Object[] { velocityLowerLimit, velocityUpperLimit, velocityUnit });
			writer.write(trailer);
			writer.write(String.format(Locale.ENGLISH, leader, placemarkName1, "ff00ff00", 2, 0));//$NON-NLS-1$		
			velocityRange = 1;
		}
		else if (actualVelocity > velocityUpperLimit) {
			String placemarkName2 = Messages.getString(MessageIds.GDE_MSGT0606, new Object[] { velocityUpperLimit, velocityUnit });
			writer.write(trailer);
			writer.write(String.format(Locale.ENGLISH, leader, placemarkName2, "ff00ffff", 2, 0));//$NON-NLS-1$		
			velocityRange = 2;
		}
		return velocityRange;
	}

}
