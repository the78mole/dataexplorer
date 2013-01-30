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
import gde.exception.DevicePropertiesInconsistenceException;
import gde.exception.MissMatchDeviceException;
import gde.exception.NotSupportedFileFormatException;
import gde.io.DataParser;
import gde.io.NMEAParser.NMEA;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

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
	static Logger					log			= Logger.getLogger(GPXDataReaderWriter.class.getName());

	static String					lineSep	= GDE.LINE_SEPARATOR;
	static DecimalFormat	df3			= new DecimalFormat("0.000"); //$NON-NLS-1$
	static StringBuffer		sb;
	
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
	public static RecordSet read(String filePath, IDevice device, String recordNameExtend, Integer channelConfigNumber) throws NotSupportedFileFormatException, IOException, DataInconsitsentException, DataTypeException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		String line = GDE.STRING_STAR;
		BufferedReader reader; // to read the data
		Channel activeChannel = null;
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new File(filePath).lastModified()); //$NON-NLS-1$
		long inputFileSize = new File(filePath).length();
		int progressLineLength = Math.abs(device.getDataBlockSize(InputTypes.FILE_IO));
		boolean isOutdated = false;
		int lineNumber = 0;
		int activeChannelConfigNumber = 1; // at least each device needs to have one channelConfig to place record sets
		String recordSetNameExtend = device.getRecordSetStemName();
		RecordSet channelRecordSet = null;
		int lastRecordSetNumberOffset = 0;
		Vector<RecordSet> createdRecordSets = new Vector<RecordSet>(1);

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
				
//				if (application.getStatusBar() != null) {
//					channels.switchChannel(activeChannel.getNumber(), GDE.STRING_EMPTY);
//					application.getMenuToolBar().updateChannelSelector();
//					activeChannel = channels.getActiveChannel();
//				}
				String recordSetName = (activeChannel.size() + 1) + recordSetNameExtend; //$NON-NLS-1$

				//now get all data   $1;1;0; 14780;  598;  1000;  8838;  0002
				//$channelConfigNumber;stateNumber;timeStepSeconds;firstIntValue;secondIntValue;.....;checkSumIntValue;
				int measurementSize = device.getNumberOfMeasurements(activeChannelConfigNumber);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "measurementSize = " + measurementSize);  //$NON-NLS-1$
				

					try {
						channelConfigNumber = device.recordSetNumberFollowChannel() ? data.channelConfigNumber : channelConfigNumber;
						activeChannel = channels.get(channelConfigNumber);
						if (channelConfigNumber > device.getChannelCount()) 
							continue; //skip data if not configured
						
						if (log.isLoggable(Level.FINE)) log.log(Level.FINE, device.getChannelCount() + " - data for channel = " + channelConfigNumber + " state = " + data.state);
						
						recordSetNameExtend = device.getStateType().getProperty().get(data.state - 1).getName(); // state name
						if (recordNameExtend.length() > 0) {
							recordSetNameExtend = recordSetNameExtend + GDE.STRING_BLANK + GDE.STRING_LEFT_BRACKET + recordNameExtend + GDE.STRING_RIGHT_BRACKET;
						}
						channelRecordSet = activeChannel.get(activeChannel.getLastActiveRecordSetName());
					}
					catch (Exception e) {
						throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0044, new Object[] {data.state, filePath, device.getPropertiesFileName()})); 
					}

					// check if a record set matching for re-use is available and prepare a new if required
					if (activeChannel.size() == 0 || channelRecordSet == null || !recordSetName.endsWith(GDE.STRING_BLANK + recordSetNameExtend) || lastRecordSetNumberOffset != data.recordSetNumberOffset) {
						//record set does not exist or is outdated, build a new name and create, in case of ChannelTypes.TYPE_CONFIG try sync with channel number
						if (lastRecordSetNumberOffset != data.recordSetNumberOffset && channelRecordSet != null) {
							if (channelRecordSet.get(0).size() < 3) {
								channelRecordSet = activeChannel.get(recordSetName);
								activeChannel.remove(recordSetName);
								createdRecordSets.remove(channelRecordSet);
								log.log(Level.WARNING, filePath + " - remove record set with < 3 data points, last line number = " + (lineNumber - 1)); //$NON-NLS-1$
								activeChannel.put(recordSetName, RecordSet.createRecordSet(recordSetName, application.getActiveDevice(), activeChannel.getNumber(), true, false));
								createdRecordSets.add(activeChannel.get(recordSetName));
								recordSetName = channelRecordSet.getName(); // cut/correct length
								
								if (activeChannel.getType() == ChannelTypes.TYPE_CONFIG)
									activeChannel.applyTemplate(recordSetName, false);
								else 
									activeChannel.applyTemplateBasics(recordSetName);
							}
							else {
								channelRecordSet.checkAllDisplayable(); // raw import needs calculation of passive records
								if (activeChannel.getType() == ChannelTypes.TYPE_CONFIG)
									activeChannel.applyTemplate(recordSetName, false);
								else 
									activeChannel.applyTemplateBasics(recordSetName);
								device.updateVisibilityStatus(activeChannel.get(recordSetName), true);
							}
						}
						else {
							int recordNumber = device.recordSetNumberFollowChannel() && activeChannel.getType() == ChannelTypes.TYPE_CONFIG ? activeChannel.getNextRecordSetNumber(channelConfigNumber) : activeChannel.getNextRecordSetNumber();
							recordSetName = recordNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + recordSetNameExtend;
							activeChannel.put(recordSetName, RecordSet.createRecordSet(recordSetName, application.getActiveDevice(), activeChannel.getNumber(), true, false));
							if (log.isLoggable(Level.FINE)) log.log(Level.FINE, recordSetName + " created for channel " + activeChannel.getName()); //$NON-NLS-1$
							activeChannel.setActiveRecordSet(recordSetName);
							channelRecordSet = activeChannel.get(recordSetName);
							createdRecordSets.add(channelRecordSet);
							recordSetName = channelRecordSet.getName(); // cut/correct length
							
							if (activeChannel.getType() == ChannelTypes.TYPE_CONFIG)
								activeChannel.applyTemplate(recordSetName, false);
							else 
								activeChannel.applyTemplateBasics(recordSetName);
							
							if (application.getStatusBar() != null && activeChannel.getName().equals(channels.getActiveChannel().getName())) {
								channels.getActiveChannel().switchRecordSet(recordSetName);
							}
						}
						

						try {
							isOutdated = Integer.parseInt(dateTime.split(GDE.STRING_DASH)[0]) <= 2000;
						}
						catch (Exception e) {
							// ignore and state as not outdated
						}

						// make all records displayable while absolute data
						String[] recordNames = device.getMeasurementNames(activeChannel.getNumber());
						lastRecordSetNumberOffset = data.recordSetNumberOffset;
				
					//add data only if 
					if (data.time_ms - lastTimeStamp >= 0) 
							channelRecordSet.addPoints(data.values, data.time_ms);
					
						data.setTimeResetEnabled(true);
						lastTimeStamp = data.time_ms;
					}
					progressLineLength = progressLineLength > line.length() ? progressLineLength : line.length();
					int progress = (int) (lineNumber*100/(inputFileSize/progressLineLength));
					if (application.getStatusBar() != null && progress <= 90 && progress > application.getProgressPercentage() && progress % 10 == 0) 	{
						application.setProgress(progress, sThreadId);
					}

				}
				if (application.getStatusBar() != null) 	application.setProgress(100, sThreadId);

				Iterator<RecordSet> iterator = createdRecordSets.iterator();
				while (iterator.hasNext()) {
					RecordSet tmpRecordSet = iterator.next();
					if (tmpRecordSet.get(0).realSize() < 3) {
						channels.get(tmpRecordSet.getChannelConfigNumber()).remove(recordSetName);
						log.log(Level.WARNING, filePath + " - remove record set " + tmpRecordSet.getName() + " with < 3 data points, last line number = " + (lineNumber - 1)); //$NON-NLS-1$
						iterator.remove();
					}
					else {
						if (!isOutdated) {
							long startTimeStamp = (long) (new File(filePath).lastModified() - tmpRecordSet.getMaxTime_ms());
							tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT	+ Messages.getString(MessageIds.GDE_MSGT0129) + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp));
							tmpRecordSet.setStartTimeStamp(startTimeStamp);
							activeChannel.setFileDescription(dateTime.substring(0, 10) + activeChannel.getFileDescription().substring(10));
						}
						else {
							tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT	+ Messages.getString(MessageIds.GDE_MSGT0129) + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new Date())); //$NON-NLS-1$
						}
						tmpRecordSet.checkAllDisplayable(); // raw import needs calculation of passive records
						device.updateVisibilityStatus(tmpRecordSet, true);
					}
				}
				
				if (createdRecordSets.size() == 1) {
					activeChannel.setActiveRecordSet(createdRecordSets.firstElement());

					if (application.getStatusBar() != null) 
						activeChannel.switchRecordSet(createdRecordSets.firstElement().getName());
				}
				else {
					if (activeChannel.getActiveRecordSet() != null) {
						activeChannel.switchRecordSet(activeChannel.getActiveRecordSet().getName());
					}
				}

				reader.close();
				reader = null;
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
			String msg = filePath + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGE0045, new Object[] {e.getMessage(), lineNumber});
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

	public static String parseInputXML(final String localUnixFullQualifiedPath) {
		final StringBuilder sb = new StringBuilder();
		

		try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {

				boolean isPropertyValueViosSet = false;
				String actualStorageVolume = "";

				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

					// System.out.println("Start Element :" + qName);
					if (qName != null && qName.length() > 1 && attributes != null && attributes.getLength() >= 1) {
						if (qName.equalsIgnoreCase("storage-subsystem")) {
							if (attributes.getValue("name") == null	|| !(attributes.getValue("name").equals("cloud-storage-subsystem") && attributes.getValue("ansi-t10-id").equals("3258"))) {
								sb.append("Error : 'storage-subsystem' must have attribute name=\"cloud-storage-subsystem\" and ansi-t10-id=\"3258\", please correct!\n");
							}
						}
						if (qName.equalsIgnoreCase("storage-allocation-pool")) {
							// check for <storage-allocation-pool name="Server Pool - Data Disk Stg Pool"
							sb.append("Error : 'storage-allocation-pool' will be handeled by underlaying workflow, please remove!\n");
						}
						if (qName.equalsIgnoreCase("storage-volume")) {
							// <storage-volume storage-pool="Server Pool - Storage Pool" ...
							// <storage-volume storage-pool="Server Pool - Data Disk Stg Pool" ...
							actualStorageVolume = attributes.getValue("volume-id");
						}
					}
				}

			};
			saxParser.parse(localUnixFullQualifiedPath, handler);

		} catch (Exception e) {
			e.printStackTrace();
			sb.append(e.getMessage());
		}

		return sb.toString();
	}

}
