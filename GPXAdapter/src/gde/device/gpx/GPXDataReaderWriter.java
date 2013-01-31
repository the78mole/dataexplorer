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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013 Winfried Bruegmann
****************************************************************************************/
package gde.device.gpx;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.ChannelTypes;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.MissMatchDeviceException;
import gde.exception.NotSupportedFileFormatException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class to read and write geo points exchange format data 
 * @author Winfried Brügmann
 */
public class GPXDataReaderWriter {
	static Logger							log					= Logger.getLogger(GPXDataReaderWriter.class.getName());

	static String							lineSep			= GDE.LINE_SEPARATOR;
	static DecimalFormat			df3					= new DecimalFormat("0.000");														//$NON-NLS-1$
	static StringBuffer				sb;

	final static DataExplorer	application	= DataExplorer.getInstance();
	final static Channels			channels		= Channels.getInstance();

	/**
	 * read the selected CSV file and parse
	 * @param filePath
	 * @param device
	 * @param recordNameExtend
	 * @param channelConfigNumber
	 * @param isRaw
	 * @return record set created
	 * @throws NotSupportedFileFormatException 
	 * @throws MissMatchDeviceException 
	 * @throws IOException 
	 * @throws DataInconsitsentException 
	 * @throws DataTypeException 
	 */
	public static RecordSet read(String filePath, IDevice device, String recordNameExtend, Integer channelConfigNumber) throws NotSupportedFileFormatException, IOException, DataInconsitsentException,
			DataTypeException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		String line = GDE.STRING_STAR;
		Channel activeChannel = null;
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new File(filePath).lastModified()); //$NON-NLS-1$
		long inputFileSize = new File(filePath).length();
		int progressLineLength = Math.abs(device.getDataBlockSize(InputTypes.FILE_IO));
		int lineNumber = 0;
		int activeChannelConfigNumber = 1; // at least each device needs to have one channelConfig to place record sets
		String recordSetNameExtend = device.getRecordSetStemName();
		RecordSet channelRecordSet = null;

		try {
			if (channelConfigNumber == null)
				activeChannel = channels.getActiveChannel();
			else
				activeChannel = channels.get(channelConfigNumber);
			channelConfigNumber = channels.getActiveChannelNumber();

			if (activeChannel != null) {
				if (application.getStatusBar() != null) {
					application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0594) + filePath);
					application.setProgress(0, sThreadId);
				}
				activeChannelConfigNumber = activeChannel.getNumber();
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, device.getChannelCount() + " - data for channel = " + channelConfigNumber);

				String recordSetName = (activeChannel.size() + 1) + recordSetNameExtend; //$NON-NLS-1$

				int measurementSize = device.getNumberOfMeasurements(activeChannelConfigNumber);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "measurementSize = " + measurementSize); //$NON-NLS-1$

				int recordNumber = device.recordSetNumberFollowChannel() && activeChannel.getType() == ChannelTypes.TYPE_CONFIG ? activeChannel.getNextRecordSetNumber(channelConfigNumber) : activeChannel
						.getNextRecordSetNumber();
				recordSetName = recordNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + recordSetNameExtend;
				activeChannel.put(recordSetName, RecordSet.createRecordSet(recordSetName, application.getActiveDevice(), activeChannel.getNumber(), true, false));
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, recordSetName + " created for channel " + activeChannel.getName()); //$NON-NLS-1$
				activeChannel.setActiveRecordSet(recordSetName);
				channelRecordSet = activeChannel.get(recordSetName);
				recordSetName = channelRecordSet.getName(); // cut/correct length

				if (activeChannel.getType() == ChannelTypes.TYPE_CONFIG)
					activeChannel.applyTemplate(recordSetName, false);
				else
					activeChannel.applyTemplateBasics(recordSetName);

				if (application.getStatusBar() != null && activeChannel.getName().equals(channels.getActiveChannel().getName())) {
					channels.getActiveChannel().switchRecordSet(recordSetName);
				}

				parseInputXML(filePath, channelRecordSet);

				progressLineLength = progressLineLength > line.length() ? progressLineLength : line.length();
				int progress = (int) (lineNumber * 100 / (inputFileSize / progressLineLength));
				if (application.getStatusBar() != null && progress <= 90 && progress > application.getProgressPercentage() && progress % 10 == 0) {
					application.setProgress(progress, sThreadId);
				}

				if (application.getStatusBar() != null) application.setProgress(100, sThreadId);

				long startTimeStamp = (long) (new File(filePath).lastModified() - channelRecordSet.getMaxTime_ms());
				channelRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129)
						+ new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp));
				channelRecordSet.setStartTimeStamp(startTimeStamp);
				activeChannel.setFileDescription(dateTime.substring(0, 10) + activeChannel.getFileDescription().substring(10));
				channelRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129)
						+ new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new Date())); //$NON-NLS-1$

				channelRecordSet.checkAllDisplayable(); // raw import needs calculation of passive records
				device.updateVisibilityStatus(channelRecordSet, true);

				if (activeChannel.getActiveRecordSet() != null) {
					activeChannel.switchRecordSet(activeChannel.getActiveRecordSet().getName());
				}
			}
		}
		catch (FileNotFoundException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			application.openMessageDialog(e.getMessage());
		}
		catch (IOException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			application.openMessageDialog(e.getMessage());
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
			// check if previous records are available and needs to be displayed
			if (activeChannel != null && activeChannel.size() > 0) {
				String recordSetName = activeChannel.getFirstRecordSetName();
				activeChannel.setActiveRecordSet(recordSetName);
				device.updateVisibilityStatus(activeChannel.get(recordSetName), true);
				activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records
				if (application.getStatusBar() != null) activeChannel.switchRecordSet(recordSetName);
			}
			// now display the error message
			String msg = filePath + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGE0045, new Object[] { e.getMessage(), lineNumber });
			log.log(Level.WARNING, msg, e);
			application.openMessageDialog(msg);
		}
		finally {
			if (application.getStatusBar() != null) {
				application.setStatusMessage(GDE.STRING_EMPTY);
			}
		}

		return channelRecordSet;
	}

	public static void parseInputXML(final String localUnixFullQualifiedPath, final RecordSet activeRecordSet) throws ParserConfigurationException, SAXException, IOException {

		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(true);
		SAXParser saxParser = factory.newSAXParser();
		saxParser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
		saxParser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", "file:/resource/gpx.xsd");

		DefaultHandler handler = new DefaultHandler() {
			boolean isDescription = false;
			boolean isDescription2 = false;
			boolean isElevation = false;
			boolean isTime = false, isDateSet = false;
			boolean isNumSatelites = false;
			Boolean isExtensionFirstCalled = null;
			boolean isExtension = false;
			final int[] date = new int[3];
			final int[] time = new int[3];
			long timeStamp = 0, startTimeStamp = 0;
			final Map<String, String> tmpPoints = new LinkedHashMap<String, String>();
			final Vector<String> extensionNames = new Vector<String>();
			String extensionName = GDE.STRING_EMPTY;
			int[] points = new int[activeRecordSet.size()];
			int pointsIndex = 0;


			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

				//System.out.println("Start Element :" + qName);
				if (qName != null && qName.length() > 1) {
					if (qName.equalsIgnoreCase("text")) 
						isDescription = true; //<text>MikroKopter</text> 
					else if (qName.equalsIgnoreCase("desc")) 
						isDescription2 = true;//<desc>FC HW:2.1 SW:0.88e + NC HW:2.0 SW:0.28i</desc>
					
					// <trkpt lat="+41.0334244" lon="-73.5230532">	
					else if (qName.equalsIgnoreCase("trkpt")) {
						if (attributes.getLength() == 2) {
							tmpPoints.put("lat", attributes.getValue("lat")); //lat="+41.0334244"
							tmpPoints.put("lon", attributes.getValue("lon")); //lon="-73.5230532"
						}
					}
					else if (qName.equalsIgnoreCase("ele")) 
						isElevation = true;//<ele>12.863</ele>
					else if (qName.equalsIgnoreCase("time")) 
						isTime = true;//<time>2012-04-19T15:37:33Z</time>
					else if (qName.equalsIgnoreCase("sat")) 
						isNumSatelites = true;//<sat>10</sat>

					//<extensions>
					else if (qName.equalsIgnoreCase("extensions")) {
						isExtension  = true;
						if (isExtensionFirstCalled == null)
							isExtensionFirstCalled = true;
					}
					else if (isExtension) {
						extensionName = qName;
					}

					
				}

	
//				<extensions>
//				<Altimeter>252,' '</Altimeter>
//				<Variometer>89</Variometer>
//				<Course>297</Course>
//				<GroundSpeed>175</GroundSpeed>
//				<VerticalSpeed>508</VerticalSpeed>
//				<FlightTime>3</FlightTime>
//				<Voltage>15.8</Voltage>
//				<Current>68.9</Current>
//				<Capacity>76</Capacity>
//				<RCQuality>197</RCQuality>
//				<RCRSSI>0</RCRSSI>
//				<Compass>094,095</Compass>
//				<NickAngle>006</NickAngle>
//				<RollAngle>000</RollAngle>
//				<MagnetField>102</MagnetField>
//				<MagnetInclination>64,-4</MagnetInclination>
//				<MotorCurrent>24,91,143,97,157,88,0,0,0,0,0,0</MotorCurrent>
//				<BL_Temperature>25,27,20,27,26,24,0,0,0,0,0,0</BL_Temperature>
//				<AvaiableMotorPower>255</AvaiableMotorPower>
//				<FC_I2C_ErrorCounter>000</FC_I2C_ErrorCounter>
//				<AnalogInputs>21,12,24,760</AnalogInputs>
//				<NCFlag>0x82</NCFlag>
//				<Servo>153,128,0</Servo>
//				<WP>----,0,13,0</WP>
//				<FCFlags2>0xc3,0x18</FCFlags2>
//				<ErrorCode>000</ErrorCode>
//				<TargetBearing>090</TargetBearing>
//				<TargetDistance>12</TargetDistance>
//				<RCSticks>0,0,0,30,1,127,1,153,1,1,1,1</RCSticks>
//				<GPSSticks>-77,-14,0,'D'</GPSSticks>
//				</extensions>
				
			}

			@Override
			public void endElement(String uri, String localName, String qName) throws SAXException {
				//System.out.println("End Element :" + qName);
				if (qName.equalsIgnoreCase("trkpt")) {
					pointsIndex = 0;
					points[pointsIndex++] = (int) (Double.valueOf(tmpPoints.get("lat").replace("+", "").trim()) * 1000000); 
					points[pointsIndex++] = (int) (Double.valueOf(tmpPoints.get("lon").replace("+", "").trim()) * 1000000);  
					points[pointsIndex++] = Integer.valueOf(tmpPoints.get("ele").replace(".", "").trim()); 
					points[pointsIndex++] = Integer.valueOf(tmpPoints.get("sat").trim()) * 1000; 
					try {
						if (startTimeStamp == 0) startTimeStamp = timeStamp;
						System.out.println(""+(timeStamp - startTimeStamp)*1.0);
						activeRecordSet.addPoints(points, (timeStamp - startTimeStamp)*1.0);
					}
					catch (DataInconsitsentException e) {
						e.printStackTrace();
					}
				}
				else if (qName.equalsIgnoreCase("extensions")) {
					if (isExtensionFirstCalled) {
						for (String tmpExtensionName : extensionNames) {
							String[] values = tmpPoints.get(tmpExtensionName).split(GDE.STRING_COMMA);
							for (int i = 0; i < values.length && i < points.length-pointsIndex; i++) {
								activeRecordSet.getRecordNames()[pointsIndex] = tmpExtensionName + GDE.STRING_UNDER_BAR + (i+1);
								activeRecordSet.get(pointsIndex).setName(activeRecordSet.getRecordNames()[pointsIndex]);
								try {
									points[pointsIndex++] = Integer.valueOf(values[1].trim()) * 1000;
								}
								catch (NumberFormatException e) {
									// ignore and keep existing value
								} 
							}
						}
						isExtensionFirstCalled = false;
					}
					else if (!isExtensionFirstCalled) {
						for (String tmpExtensionName : extensionNames) {
							String[] values = tmpPoints.get(tmpExtensionName).split(GDE.STRING_COMMA);
							for (int i = 0; i < values.length && i < points.length-pointsIndex; i++) {
								try {
									points[pointsIndex++] = Integer.valueOf(values[1].trim()) * 1000;
								}
								catch (NumberFormatException e) {
									// ignore and keep existing value
								} 
							}
						}
					}
					
				}
			}

			@Override
			public void characters(char ch[], int start, int length) throws SAXException {
				System.out.println(new String(ch, start, length));
				if(isDescription) {
					activeRecordSet.setRecordSetDescription(activeRecordSet.getRecordSetDescription() + new String(ch, start, length));
					isDescription = false;
				}
				else if(isDescription2) {
					activeRecordSet.setRecordSetDescription(activeRecordSet.getRecordSetDescription() + GDE.LINE_SEPARATOR + new String(ch, start, length));
					isDescription2 = false;
				}
				else if (isElevation) {
					tmpPoints.put("ele", new String(ch, start, length)); //<ele>12.863</ele>
					isElevation = false;
				}
				else if (isTime) {
					String dateTime = new String(ch, start, length);//<time>2012-04-19T15:37:33Z</time>
					if (!isDateSet) {
						String strDate = dateTime.split("T")[0];
						date[0] = Integer.parseInt(strDate.substring(0, 4));
						date[1] = Integer.parseInt(strDate.substring(5, 7));
						date[2] = Integer.parseInt(strDate.substring(8, 10));
						isDateSet = true;
					}
					String strValueTime = dateTime.split("T|Z")[1];
					time[0] = Integer.parseInt(strValueTime.substring(0, 2));
					time[1] = Integer.parseInt(strValueTime.substring(3, 5));
					time[2] = Integer.parseInt(strValueTime.substring(6, 8));
					GregorianCalendar calendar = new GregorianCalendar(date[0], date[1] - 1, date[2], time[0], time[1], time[2]);
					timeStamp = calendar.getTimeInMillis() + (strValueTime.contains(GDE.STRING_DOT) ? Integer.parseInt(strValueTime.substring(strValueTime.indexOf(GDE.STRING_DOT) + 1)) : 0);
					isTime = false;
				}
				else if (isNumSatelites) {
					tmpPoints.put("sat", new String(ch, start, length)); //<sat>10</sat>
					isNumSatelites = false;
				}
				else if (isExtension) {
					extensionNames.add(extensionName);
					tmpPoints.put(extensionName, new String(ch, start, length)); //<MotorCurrent>24,91,143,97,157,88,0,0,0,0,0,0</MotorCurrent>
				}
			}
		};
		saxParser.parse(localUnixFullQualifiedPath, handler);

		return;
	}

}
