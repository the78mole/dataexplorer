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
    
    Copyright (c) 2008,2009,2010,2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.device.jeti;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.InputTypes;
import gde.device.MeasurementType;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.exception.MissMatchDeviceException;
import gde.exception.NotSupportedFileFormatException;
import gde.io.CSVSerialDataReaderWriter;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;

import cz.vutbr.fit.gja.proj.utils.TelemetryData;
import cz.vutbr.fit.gja.proj.utils.TelemetryData.TelemetrySensor;

/**
 * Class to read Jeti sensor data using the TelemetryData class provided by Martin Falticko
 * @author Winfried Brügmann
 */
public class JetiDataReader {
	static Logger							log					= Logger.getLogger(CSVSerialDataReaderWriter.class.getName());

	static String							lineSep			= GDE.LINE_SEPARATOR;
	static DecimalFormat			df3					= new DecimalFormat("0.000");																	//$NON-NLS-1$
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
	public static RecordSet read(String filePath, JetiAdapter device, String recordNameExtend, Integer channelConfigNumber, boolean isRaw) throws NotSupportedFileFormatException, IOException,
			DataInconsitsentException, DataTypeException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		String line = GDE.STRING_STAR;
		RecordSet recordSet = null;
		Channel activeChannel = null;
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new File(filePath).lastModified()); //$NON-NLS-1$
		long inputFileSize = new File(filePath).length();
		int progressLineLength = Math.abs(device.getDataBlockSize(InputTypes.FILE_IO));
		boolean isOutdated = false;
		int lineNumber = 0;
		int activeChannelConfigNumber = 1; // at least each device needs to have one channelConfig to place record sets
		String recordSetNameExtend = device.getRecordSetStemName();
		double time_ms = 0, timeStep_ms = 0;

		try {
			if (channelConfigNumber == null)
				activeChannel = channels.getActiveChannel();
			else
				activeChannel = channels.get(channelConfigNumber);

			if (activeChannel != null) {
				if (application.getStatusBar() != null) {
					application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0594) + filePath);
					application.setProgress(0, sThreadId);
				}
				activeChannelConfigNumber = activeChannel.getNumber();

				if (application.getStatusBar() != null) {
					channels.switchChannel(activeChannel.getNumber(), GDE.STRING_EMPTY);
					application.getMenuToolBar().updateChannelSelector();
					activeChannel = channels.getActiveChannel();
				}
				String recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$

				//now get all data   $1;1;0; 14780;  598;  1000;  8838;  0002
				//$recordSetNumber;stateNumber;timeStepSeconds;firstIntValue;secondIntValue;.....;checkSumIntValue;
				int measurementSize = device.getNumberOfMeasurements(activeChannelConfigNumber);
				int dataBlockSize = device.getDataBlockSize(InputTypes.FILE_IO); // measurements size must not match data block size, there are some measurements which are result of calculation			
				log.log(Level.FINE, "measurementSize = " + measurementSize + "; dataBlockSize = " + dataBlockSize); //$NON-NLS-1$ //$NON-NLS-2$
				if (measurementSize < Math.abs(dataBlockSize)) throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0041, new String[] { filePath }));

				TelemetryData data = new TelemetryData();
				if (data.loadData(filePath)) {
					TreeSet<TelemetrySensor> recordSetData = data.getData();
					//System.out.println("Modell name = " + data.getModelName());
					//TODO check for objectKey
					//System.out.println("max time = " + data.getMaxTimestamp());

					int maxHit = 0, numValues = 0;
					Map<Integer, Integer> valuesMap = new HashMap<Integer, Integer>();
					for (TelemetrySensor telemetrySensor : recordSetData) {
						for (TelemetryData.TelemetryVar dataVar : telemetrySensor.getVariables()) {
							if (dataVar.getItems().size() > 0) {
								if (valuesMap.containsKey(dataVar.getItems().size())) {
									valuesMap.put(dataVar.getItems().size(), valuesMap.get(dataVar.getItems().size()) + 1);
									maxHit = Math.max(maxHit, valuesMap.get(dataVar.getItems().size()));
								}
								else
									valuesMap.put(dataVar.getItems().size(), 1);
							}
							System.out.println(String.format("%10s [%s]", dataVar.getName(), dataVar.getUnit()));
						}
					}
					Integer[] occurrence = valuesMap.values().toArray(new Integer[1]);
					Arrays.sort(occurrence);
					for (Integer key : valuesMap.keySet()) {
						if (valuesMap.get(key) == occurrence[occurrence.length - 1]) {
							numValues = key;
							break;
						}
					}
					//System.out.println("best fit # values = " + numValues);
					timeStep_ms = data.getMaxTimestamp() * 1000 / numValues;

					try {

						recordSetNameExtend = device.getStateType().getProperty().get(0).getName(); // state name
						if (recordNameExtend.length() > 0) {
							if (data.getModelName().length() > 0)
								recordSetNameExtend = recordSetNameExtend + GDE.STRING_BLANK + GDE.STRING_LEFT_BRACKET + data.getModelName() + GDE.STRING_RIGHT_BRACKET;
							else
								recordSetNameExtend = recordSetNameExtend + GDE.STRING_BLANK + GDE.STRING_LEFT_BRACKET + recordNameExtend + GDE.STRING_RIGHT_BRACKET;
						}
					}
					catch (Exception e) {
						throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0044, new Object[] { 1, filePath, device.getPropertiesFileName() }));
					}

					//prepare new record set now
					recordSetName = (activeChannel.size() + 1) + ") " + recordSetNameExtend; //$NON-NLS-1$

					//String[] recordNames = device.getMeasurementNames(activeChannel.getNumber());
					//adapt record names and units to current telemetry sensors
					int index = 0;
					Vector<String> vecRecordNames = new Vector<String>();
					Map<Integer, Record.DataType> mapRecordType = new HashMap<Integer, Record.DataType>();
					for (TelemetrySensor telemetrySensor : recordSetData) {
						boolean actualgps = false;
						for (TelemetryData.TelemetryVar dataVar : telemetrySensor.getVariables()) {
							String newReordName = dataVar.getName();
							while (vecRecordNames.contains(newReordName)) { //check for duplicated record names and update to make unique
								newReordName = newReordName + "'";
							}
							vecRecordNames.add(newReordName);
							System.out.println("add new record name = " + newReordName);
							device.setMeasurementName(activeChannelConfigNumber, index, dataVar.getName());
							device.setMeasurementUnit(activeChannelConfigNumber, index, dataVar.getUnit());
							if (dataVar.getType() == (TelemetryData.T_GPS) && (dataVar.getDecimals() & 1) == 0) {
								actualgps = true;
								mapRecordType.put(index, Record.DataType.GPS_LATITUDE);
							}
							if (dataVar.getType() == (TelemetryData.T_GPS) && (dataVar.getDecimals() & 1) == 1) {
								actualgps = true;
								mapRecordType.put(index, Record.DataType.GPS_LONGITUDE);
							}
							if (actualgps && dataVar.getUnit().contains("°") && dataVar.getParam() == 10) {
								mapRecordType.put(index, Record.DataType.GPS_AZIMUTH);
							}
							if ((dataVar.getName().toUpperCase().contains("GPS") || dataVar.getName().toUpperCase().contains("ABS"))
									&& (dataVar.getName().toLowerCase().contains("hoehe") || dataVar.getName().toLowerCase().contains("höhe") || dataVar.getName().toLowerCase().contains("height") || dataVar.getName()
											.toLowerCase().contains("alt"))) //dataVar.getParam()==4
							{
								mapRecordType.put(index, Record.DataType.GPS_ALTITUDE);
							}
							//System.out.println(dataVar.getParam());
							++index;
						}
					}

					//build up the record set with variable number of records just fit the sensor data
					String[] recordNames = vecRecordNames.toArray(new String[0]);
					String[] recordSymbols = new String[recordNames.length];
					String[] recordUnits = new String[recordNames.length];
					for (int i = 0; i < recordNames.length; i++) {
						MeasurementType measurement = device.getMeasurement(activeChannelConfigNumber, i);
						recordSymbols[i] = measurement.getSymbol();
						recordUnits[i] = measurement.getUnit();
					}
					recordSet = RecordSet.createRecordSet(recordSetName, device, activeChannelConfigNumber, recordNames, recordSymbols, recordUnits, device.getTimeStep_ms(), isRaw, true);
					//set record data type which are not default
					for (Entry<Integer, Record.DataType> entry : mapRecordType.entrySet()) {
						recordSet.get(entry.getKey().intValue()).setDataType(entry.getValue());
					}
					recordSetName = recordSet.getName(); // cut/correct length of recordSetName

					//correct time if needed
					try {
						isOutdated = Integer.parseInt(dateTime.split(GDE.STRING_DASH)[0]) <= 2000;
					}
					catch (Exception e) {
						// ignore and state as not outdated
					}

					activeChannel.put(recordSetName, recordSet);

					index = 0;
					//System.out.println(device.getMeasurementNames(activeChannelConfigNumber).length + " - " + recordSet.size());
					int[] points = new int[recordNames.length];
					for (int i = 0; i < numValues; i++) {
						for (TelemetrySensor telemetrySensor : recordSetData) {
							for (TelemetryData.TelemetryVar dataVar : telemetrySensor.getVariables()) {
								//System.out.print(String.format("%s ", dataVar.getName()));
								points[index++] = (int) (dataVar.getDoubleAt(time_ms / 1000) * (dataVar.getType() == TelemetryData.T_GPS ? 1000000 : 1000));
								//System.out.print(String.format("%3.2f ", (points[index-1]/1000.0)));
							}
						}
						recordSet.addPoints(points, time_ms);
						time_ms += timeStep_ms;
						index = 0;
						//System.out.println();
					}
					progressLineLength = progressLineLength > line.length() ? progressLineLength : line.length();
					int progress = (int) (lineNumber * 100 / (inputFileSize / progressLineLength));
					if (application.getStatusBar() != null && progress % 5 == 0) application.setProgress(progress, sThreadId);

					if (application.getStatusBar() != null) application.setProgress(100, sThreadId);

					activeChannel.setActiveRecordSet(recordSetName);
					activeChannel.applyTemplate(recordSetName, true);
					device.updateVisibilityStatus(activeChannel.get(recordSetName), true);
					if (!isOutdated) {
						long startTimeStamp = (long) (new File(filePath).lastModified() - activeChannel.get(recordSetName).getMaxTime_ms());
						activeChannel.get(recordSetName).setRecordSetDescription(
								device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp));
						activeChannel.get(recordSetName).setStartTimeStamp(startTimeStamp);
						activeChannel.setFileDescription(dateTime.substring(0, 10) + activeChannel.getFileDescription().substring(10));
					}
					else {
						activeChannel.get(recordSetName).setRecordSetDescription(
								device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new Date())); //$NON-NLS-1$
					}
					activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records
					if (application.getStatusBar() != null) activeChannel.switchRecordSet(recordSetName);
				}
			}
		}
		catch (Exception e) {
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

		return recordSet;
	}
}
