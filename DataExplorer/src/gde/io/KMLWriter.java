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
import gde.utils.StringHelper;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Class to write KML XML files
 * @author Winfried Brügmann
 */
public class KMLWriter {
	static Logger							log					= Logger.getLogger(KMLWriter.class.getName());

	static final String				header			= "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\">" + GDE.LINE_SEPARATOR + GDE.LINE_SEPARATOR;	//$NON-NLS-1$

	static final String				position		= "<Document>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t<name>gx:%s</name>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t<description>%s</description>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t<LookAt>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<longitude>%.7f</longitude>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<latitude>%.7f</latitude>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<heading>%d</heading>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<tilt>%d</tilt>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<range>%d</range>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<gx:altitudeMode>relativeToGround</gx:altitudeMode>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t</LookAt>" + GDE.LINE_SEPARATOR;																																																				//$NON-NLS-1$

	static final String				leader			= "<Placemark>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t<name>gx:%s</name>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t<Style>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<LineStyle>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t<color>%s</color>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t\t<width>%d</width>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t</LineStyle>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t</Style>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t<LineString>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<extrude>%d</extrude>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<gx:altitudeMode>relativeToGround</gx:altitudeMode>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t\t<coordinates>" + GDE.LINE_SEPARATOR;																																																	//$NON-NLS-1$

	static final String				trailer			= "\t\t</coordinates>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "\t</LineString>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "</Placemark>" + GDE.LINE_SEPARATOR;																																																				//$NON-NLS-1$

	static final String				footer			= "</Document>" + GDE.LINE_SEPARATOR //$NON-NLS-1$
																						+ "</kml>" + GDE.LINE_SEPARATOR;																																																							//$NON-NLS-1$

	final static DataExplorer	application	= DataExplorer.getInstance();
	final static Channels			channels		= Channels.getInstance();

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
	public static void write(RecordSet recordSet, String filePath, final int ordinalLongitude, final int ordinalLatitude, final int ordinalHeight, final int ordinalVelocity,
			final boolean isHeightRelative) throws Exception {
		BufferedWriter writer = null;
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		StringBuilder sb = new StringBuilder();
		double height0 = 0;

		try {
			long startTime = new Date().getTime();
			if (KMLWriter.application.getStatusBar() != null) KMLWriter.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0138, new String[] { GDE.FILE_ENDING_KML, filePath }));
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8")); //$NON-NLS-1$
			IDevice device = DataExplorer.getInstance().getActiveDevice();

			writer.write(KMLWriter.header);

			// write data
			String[] recordNames = recordSet.getRecordNames();
			int realDataSize = recordSet.getRecordDataSize(true);
			int dataSize = recordSet.getRecordDataSize(false);
			int progressCycle = 0;
			if (KMLWriter.application.getStatusBar() != null) KMLWriter.application.setProgress(progressCycle, sThreadId);
			Record recordLongitude = recordSet.getRecord(recordNames[ordinalLongitude]);
			Record recordLatitude = recordSet.getRecord(recordNames[ordinalLatitude]);
			Record recordHeight = recordSet.getRecord(recordNames[ordinalHeight]);
			Record recordVelocity = recordSet.getRecord(recordNames[ordinalVelocity]);
			String velocityUnit = recordVelocity.getUnit();

			if (recordLongitude == null || recordLatitude == null || recordHeight == null)
				throw new Exception(Messages.getString(MessageIds.GDE_MSGE0005, new Object[] { Messages.getString(MessageIds.GDE_MSGT0599), recordSet.getChannelConfigName() }));

			boolean isPositionWritten = false;
			int i = 0;
			for (; i < realDataSize; i++) {
				if (recordLongitude.realGet(i) != 0 && recordLatitude.realGet(i) != 0) {
					if (!isPositionWritten) {
						// longitude, latitude, heading, tilt, range, lineColor, lineWidth, extrude
						writer.write(String.format(Locale.ENGLISH, KMLWriter.position, recordSet.getName(), recordSet.getRecordSetDescription(), device.translateValue(recordLongitude, recordLongitude.realGet(i) / 1000.0),
								device.translateValue(recordLongitude, recordLatitude.realGet(i) / 1000.0), -50, 70, 1000));
						isPositionWritten = true;

						height0 = isHeightRelative ? device.translateValue(recordHeight, recordHeight.realGet(i) / 1000.0) : 0;
						break;
					}
				}
			}
			int velocityRange = 0;
			int velocityAvg = (int) device.translateValue(recordVelocity, recordVelocity.getAvgValue()/1000.0);
			int velocityLowerLimit = 20;
			int velocityUpperLimit = 100;
			String withinLimitsColor = "ff0000ff"; //$NON-NLS-1$
			String lowerLimitColor = "ff00ff00"; //$NON-NLS-1$
			String upperLimitColor = "ff00ffff"; //$NON-NLS-1$

			if (KMLWriter.application.isObjectoriented()) {
				ObjectData object = KMLWriter.application.getObject();
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
				PropertyType propertyAvg = recordVelocity.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalVelocity,	MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_AVG_LIMIT_FACTOR.value());
				PropertyType propertyLowerLimit = recordVelocity.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalVelocity, MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_LOWER_LIMIT.value());
				PropertyType propertyUpperLimit = recordVelocity.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalVelocity, MeasurementPropertyTypes.GOOGLE_EARTH_VELOCITY_UPPER_LIMIT.value());
				velocityLowerLimit = (int) (propertyAvg != null ? velocityAvg / Double.valueOf(propertyAvg.getValue()) : propertyLowerLimit != null ? Integer.valueOf(propertyLowerLimit.getValue()) : 0);
				velocityUpperLimit = (int) (propertyAvg != null ? velocityAvg * Double.valueOf(propertyAvg.getValue()) : propertyLowerLimit != null ? Integer.valueOf(propertyUpperLimit.getValue()) : 500);

				PropertyType propertyWithinLimitsColor = recordVelocity.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalVelocity, MeasurementPropertyTypes.GOOGLE_EARTH_WITHIN_LIMITS_COLOR.value());
				PropertyType propertyLowerLimitColor = recordVelocity.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalVelocity, MeasurementPropertyTypes.GOOGLE_EARTH_LOWER_LIMIT_COLOR.value());
				PropertyType propertyUpperLimitColor = recordVelocity.getDevice().getMeasruementProperty(recordSet.getChannelConfigNumber(), ordinalVelocity, MeasurementPropertyTypes.GOOGLE_EARTH_UPPER_LIMIT_COLOR.value());
				String[] colorRGB = propertyWithinLimitsColor != null ? propertyWithinLimitsColor.getValue().split(GDE.STRING_COMMA) : new String[3];
				withinLimitsColor = propertyWithinLimitsColor != null ? String.format("ff%02x%02x%02x", Integer.parseInt(colorRGB[2]), Integer.parseInt(colorRGB[1]), Integer.parseInt(colorRGB[0])) : "ff0000ff"; //$NON-NLS-1$ //$NON-NLS-2$
				colorRGB = propertyWithinLimitsColor != null ? propertyLowerLimitColor.getValue().split(GDE.STRING_COMMA) : new String[3];
				lowerLimitColor = propertyLowerLimitColor != null ? String.format("ff%02x%02x%02x", Integer.parseInt(colorRGB[2]), Integer.parseInt(colorRGB[1]), Integer.parseInt(colorRGB[0])) : "ff00ff00"; //$NON-NLS-1$ //$NON-NLS-2$
				colorRGB = propertyWithinLimitsColor != null ? propertyUpperLimitColor.getValue().split(GDE.STRING_COMMA) : new String[3];
				upperLimitColor = propertyUpperLimitColor != null ? String.format("ff%02x%02x%02x", Integer.parseInt(colorRGB[2]), Integer.parseInt(colorRGB[1]), Integer.parseInt(colorRGB[0])) : "ff00ffff"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			String[] velocityColors = new String[] { lowerLimitColor, withinLimitsColor, upperLimitColor };

			String initialPlacemarkName = Messages.getString(MessageIds.GDE_MSGT0604, new Object[] { velocityLowerLimit, velocityUnit });
			writer.write(String.format(Locale.ENGLISH, KMLWriter.leader, initialPlacemarkName, lowerLimitColor, 2, 0));
			for (i = recordSet.isZoomMode() ? 0 : i; isPositionWritten && i < dataSize; i++) {

				int velocity = (int) device.translateValue(recordVelocity, recordVelocity.get(i) / 1000.0);
				if (!((velocity < velocityLowerLimit && velocityRange == 0) || (velocity >= velocityLowerLimit && velocity <= velocityUpperLimit && velocityRange == 1) || (velocity > velocityUpperLimit && velocityRange == 2))) {
					velocityRange = switchColor(writer, recordVelocity, velocity, velocityLowerLimit, velocityUpperLimit, velocityColors, velocityRange, velocityUnit);

					//re-write last coordinates
					double height = device.translateValue(recordHeight, recordHeight.get(i - 1) / 1000.0) - height0;
					// add data entries, translate according device and measurement unit
					sb.append(String.format(Locale.ENGLISH, "\t\t\t%.7f,", device.translateValue(recordLongitude, recordLongitude.get(i - 1) / 1000.0))) //$NON-NLS-1$
							.append(String.format(Locale.ENGLISH, "%.7f,", device.translateValue(recordLatitude, recordLatitude.get(i - 1) / 1000.0))) //$NON-NLS-1$
							.append(String.format(Locale.ENGLISH, "%.0f", height < 0 ? 0 : height)).append(GDE.LINE_SEPARATOR); //$NON-NLS-1$

					writer.write(sb.toString());
				}

				double height = device.translateValue(recordHeight, recordHeight.get(i) / 1000.0) - height0;
				// add data entries, translate according device and measurement unit
				sb.append(String.format(Locale.ENGLISH, "\t\t\t%.7f,", device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0))) //$NON-NLS-1$
						.append(String.format(Locale.ENGLISH, "%.7f,", device.translateValue(recordLatitude, recordLatitude.get(i) / 1000.0))) //$NON-NLS-1$
						.append(String.format(Locale.ENGLISH, "%.0f", height < 0 ? 0 : height)).append(GDE.LINE_SEPARATOR); //$NON-NLS-1$

				writer.write(sb.toString());

				if (KMLWriter.application.getStatusBar() != null && i % 50 == 0) KMLWriter.application.setProgress(((++progressCycle * 5000) / dataSize), sThreadId);
				KMLWriter.log.log(java.util.logging.Level.FINER, "data line = " + sb.toString()); //$NON-NLS-1$
				sb = new StringBuilder();
			}
			writer.write(KMLWriter.trailer);

			writer.write(KMLWriter.footer);

			KMLWriter.log.log(Level.TIME, "KML file = " + filePath + " written successfuly" //$NON-NLS-1$ //$NON-NLS-2$
					+ "write time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));//$NON-NLS-1$ //$NON-NLS-2$

			writer.flush();
			writer.close();
			writer = null;
			if (KMLWriter.application.getStatusBar() != null) KMLWriter.application.setProgress(100, sThreadId);
		}
		catch (IOException e) {
			KMLWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0006, new Object[] { GDE.FILE_ENDING_KML, filePath, e.getMessage() }));
		}
		catch (Exception e) {
			KMLWriter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			throw new Exception(Messages.getString(MessageIds.GDE_MSGE0007) + e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
		finally {
			if (KMLWriter.application.getStatusBar() != null) KMLWriter.application.setStatusMessage(GDE.STRING_EMPTY);
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
	 * @param velocityLowerLimit
	 * @param velocityUpperLimit
	 * @param velocitColors {lowerLimitColor, withinLimitsColor, upperLimitColor) 
	 * @param velocityRange
	 * @param velocityUnit
	 * @return
	 * @throws IOException
	 */
	public static int switchColor(BufferedWriter writer, Record velocityRecord, int actualVelocity, int velocityLowerLimit, int velocityUpperLimit, String[] velocitColors, int velocityRange,
			String velocityUnit) throws IOException {

		if (actualVelocity < velocityLowerLimit) {
			String placemarkName0 = Messages.getString(MessageIds.GDE_MSGT0604, new Object[] { velocityLowerLimit, velocityUnit });
			writer.write(KMLWriter.trailer);
			writer.write(String.format(Locale.ENGLISH, KMLWriter.leader, placemarkName0, velocitColors[0], 2, 0));
			velocityRange = 0;
		}
		else if (actualVelocity >= velocityLowerLimit && actualVelocity <= velocityUpperLimit) {
			String placemarkName1 = Messages.getString(MessageIds.GDE_MSGT0605, new Object[] { velocityLowerLimit, velocityUpperLimit, velocityUnit });
			writer.write(KMLWriter.trailer);
			writer.write(String.format(Locale.ENGLISH, KMLWriter.leader, placemarkName1, velocitColors[1], 2, 0));
			velocityRange = 1;
		}
		else if (actualVelocity > velocityUpperLimit) {
			String placemarkName2 = Messages.getString(MessageIds.GDE_MSGT0606, new Object[] { velocityUpperLimit, velocityUnit });
			writer.write(KMLWriter.trailer);
			writer.write(String.format(Locale.ENGLISH, KMLWriter.leader, placemarkName2, velocitColors[2], 2, 0));
			velocityRange = 2;
		}
		return velocityRange;
	}

}
