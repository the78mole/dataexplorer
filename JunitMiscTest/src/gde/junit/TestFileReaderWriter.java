/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011,2012,2013 Winfried Bruegmann
****************************************************************************************/
package gde.junit;

import gde.GDE;
import gde.data.Channel;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.gpx.GPXDataReaderWriter;
import gde.device.graupner.HoTTbinReader;
import gde.device.graupner.HoTTbinReader2;
import gde.device.jeti.JetiAdapter;
import gde.device.jeti.JetiDataReader;
import gde.exception.NotSupportedException;
import gde.io.CSVReaderWriter;
import gde.io.CSVSerialDataReaderWriter;
import gde.io.IGCReaderWriter;
import gde.io.LogViewReader;
import gde.io.NMEAReaderWriter;
import gde.io.OsdReaderWriter;
import gde.ui.dialog.IgcExportDialog;
import gde.utils.FileUtils;
import gde.utils.OperatingSystemHelper;
import gde.utils.StringHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

public class TestFileReaderWriter extends TestSuperClass {

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
	}

	/**
	 * test reading CSV files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testCsvReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".csv") 
						&& !(file.getPath().toLowerCase().contains("csv2serialadapter") 
								|| file.getPath().toLowerCase().contains("space pro") 
								|| file.getPath().toLowerCase().contains("asw")
								|| file.getPath().toLowerCase().contains("ash")
								|| file.getPath().toLowerCase().contains("spektrum")
								|| file.getPath().toLowerCase().contains("av4ms")
								|| file.getPath().toLowerCase().contains("akkumonitor")
								|| file.getPath().toLowerCase().contains("flightrecorder"))) {
					System.out.println("working with : " + file);
					
					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "ISO-8859-1")); //$NON-NLS-1$
						String line = reader.readLine();
						boolean isCVS2SerialFormat = line.startsWith(deviceConfig.getDataBlockLeader()) && line.contains(deviceConfig.getDataBlockSeparator().value());
						reader.close();
					
						if (!isCVS2SerialFormat) {
							HashMap<String, String> fileHeader = CSVReaderWriter.getHeader(';', file.getAbsolutePath());
							String fileDeviceName = fileHeader.get(GDE.DEVICE_NAME);
							deviceConfig = this.deviceConfigurations.get(fileDeviceName);
							if (deviceConfig == null) throw new NotSupportedException("device = " + fileDeviceName + " is not supported or in list of active devices");
							IDevice device = this.getInstanceOfDevice(deviceConfig);
							this.application.setActiveDeviceWoutUI(device);

							setupDataChannels(device);
							fileHeader = CSVReaderWriter.evaluateType(';', fileHeader, deviceConfig);

							int channelConfigNumber = this.channels.getChannelNumber(fileHeader.get(GDE.CHANNEL_CONFIG_NAME));
							if (channelConfigNumber > device.getChannelCount()) {
								channelConfigNumber = 1;
								fileHeader.put(GDE.CHANNEL_CONFIG_NAME, this.channels.get(1).getChannelConfigKey());
							}
							this.channels.setActiveChannelNumber(channelConfigNumber);
							Channel activeChannel = this.channels.getActiveChannel();
							activeChannel.setFileName(file.getAbsolutePath());
							activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
							activeChannel.setSaved(true);

							RecordSet recordSet = CSVReaderWriter.read(';', file.getAbsolutePath(), "csv test", fileHeader.get(GDE.CSV_DATA_TYPE).equals(GDE.CSV_DATA_TYPE_RAW));

							if (recordSet != null) {
								activeChannel.setActiveRecordSet(recordSet);
								activeChannel.applyTemplate(recordSet.getName(), true);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
							}
						}
						else { // CSV2SerialAdapter file
							IDevice device = this.getInstanceOfDevice(deviceConfig);
							this.application.setActiveDeviceWoutUI(device);

							setupDataChannels(device);

							this.channels.setActiveChannelNumber(1);
							Channel activeChannel = this.channels.getActiveChannel();
							activeChannel.setFileName(file.getAbsolutePath());
							activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
							activeChannel.setSaved(true);

							CSVSerialDataReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1, true);
							RecordSet recordSet = activeChannel.getActiveRecordSet();

							if (recordSet != null) {
								activeChannel.setActiveRecordSet(recordSet);
								activeChannel.applyTemplate(recordSet.getName(), true);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
							}
						}

						String tmpDir1 = this.tmpDir + "Write_1_OSD" + GDE.FILE_SEPARATOR;
						new File(tmpDir1).mkdirs();
						String absolutFilePath = tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_cvs.osd";
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading NMEA files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testNmeaReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".nmea")
						&& !(file.getPath().toLowerCase().contains("cappuccino") 
						|| file.getPath().toLowerCase().contains("space pro") 
						|| file.getPath().toLowerCase().contains("asw")
						|| file.getPath().toLowerCase().contains("ash"))) {
					System.out.println("working with : " + file);
					
					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						// GPS-Logger and similar file
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from NMEA file");
						activeChannel.setSaved(true);

						NMEAReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1);
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						String tmpDir1 = this.tmpDir + "Write_1_OSD" + GDE.FILE_SEPARATOR;
						new File(tmpDir1).mkdirs();
						String absolutFilePath = tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_nmea.osd";
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading UniLog2 NMEA files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testUniLog2NmeaReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(new File(this.dataPath.getAbsolutePath() + "/UniLog2/"), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".txt")) {
					System.out.println("working with : " + file);
					
					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						// GPS-Logger and similar file
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from TXT file");
						activeChannel.setSaved(true);

						NMEAReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1);
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						String tmpDir1 = this.tmpDir + "Write_1_OSD" + GDE.FILE_SEPARATOR;
						new File(tmpDir1).mkdirs();
						String absolutFilePath = tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_txt.osd";
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading UniLog2 NMEA files from device directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testMpxFlightRecorderNmeaReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(new File(this.dataPath.getAbsolutePath() + "/FlightRecorder/"), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".csv")) {
					System.out.println("working with : " + file);
					
					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						// GPS-Logger and similar file
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from TXT file");
						activeChannel.setSaved(true);

						NMEAReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1);
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						String tmpDir1 = this.tmpDir + "Write_1_OSD" + GDE.FILE_SEPARATOR;
						new File(tmpDir1).mkdirs();
						String absolutFilePath = tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_csv.osd";
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading OSD files %TEMP%\Write_1_OSD and writes OSD files to %TEMP%\Write_2_OSD
	 * all files must identical except time stamp
	 */
	public final void testOsdReaderCsvWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			String tmpDir1 = this.tmpDir + GDE.FILE_SEPARATOR + "Write_1_OSD" + GDE.FILE_SEPARATOR;
			List<File> files = FileUtils.getFileListing(new File(tmpDir1), 1);

			for (File file : files) {
				String filePath = file.getAbsolutePath().replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
				if (filePath.toLowerCase().endsWith(".osd")) {
					try {
						if (filePath.equals(OperatingSystemHelper.getLinkContainedFilePath(filePath))) {
							System.out.println("working with : " + file);
							HashMap<String, String> fileHeader = OsdReaderWriter.getHeader(file.getAbsolutePath());
							String fileDeviceName = fileHeader.get(GDE.DEVICE_NAME);
							DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
							IDevice device = this.getInstanceOfDevice(deviceConfig);
							this.application.setActiveDeviceWoutUI(device);

							setupDataChannels(device);

							OsdReaderWriter.read(file.getAbsolutePath());

							Channel activeChannel = this.channels.getActiveChannel();
							activeChannel.setFileName(file.getAbsolutePath());
							activeChannel.setFileDescription(fileHeader.get(GDE.FILE_COMMENT));
							activeChannel.setSaved(true);
							activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves

							for (String recordSetName : activeChannel.getRecordSetNames()) {
								RecordSet recordSet = activeChannel.get(recordSetName);
								if (recordSet != null) {
									activeChannel.setActiveRecordSet(recordSet);
									//device.makeInActiveDisplayable(recordSet);
									drawCurves(recordSet, 1024, 768);
								}
							}

							String absolutFilePath = tmpDir1 + file.getName();
							absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_abs.csv";
							System.out.println("writing as   : " + absolutFilePath);
							CSVReaderWriter.write(';', activeChannel.getActiveRecordSet() != null ? activeChannel.getActiveRecordSet().getName() : "DummyRecord", absolutFilePath, false);

							absolutFilePath = tmpDir1 + file.getName();
							absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_raw.csv";
							System.out.println("writing as   : " + absolutFilePath);
							CSVReaderWriter.write(';', activeChannel.getActiveRecordSet() != null ? activeChannel.getActiveRecordSet().getName() : "DummyRecord", absolutFilePath, true);
						}
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading OSD files from directories used by GDE application and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures, 
	 * the written files might different due to code updates (add/change properties)
	 */
	public final void testOsdReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				String filePath = file.getAbsolutePath().replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
				if (filePath.toLowerCase().endsWith(".osd")) {
					try {
						if (filePath.equals(OperatingSystemHelper.getLinkContainedFilePath(filePath))) {
							System.out.println("working with : " + file);
							HashMap<String, String> fileHeader = OsdReaderWriter.getHeader(file.getAbsolutePath());
							String fileDeviceName = fileHeader.get(GDE.DEVICE_NAME);
							if(this.legacyDeviceNames.get(fileDeviceName) != null) 
								fileDeviceName = this.legacyDeviceNames.get(fileDeviceName); 
							DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
							IDevice device = this.getInstanceOfDevice(deviceConfig);
							this.application.setActiveDeviceWoutUI(device);

							setupDataChannels(device);

							OsdReaderWriter.read(file.getAbsolutePath());

							Channel activeChannel = this.channels.getActiveChannel();
							activeChannel.setFileName(file.getAbsolutePath());
							activeChannel.setFileDescription(fileHeader.get(GDE.FILE_COMMENT));
							activeChannel.setSaved(true);
							//activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves

							for (String recordSetName : activeChannel.getRecordSetNames()) {
								RecordSet recordSet = activeChannel.get(recordSetName);
								if (recordSet != null) {
									if (!recordSet.hasDisplayableData()) recordSet.loadFileData(activeChannel.getFullQualifiedFileName(), false);
									activeChannel.setActiveRecordSet(recordSet);
									//device.makeInActiveDisplayable(recordSet);
									drawCurves(recordSet, 1024, 768);
								}
							}

							String tmpDir1 = this.tmpDir + "Write_1_OSD" + GDE.FILE_SEPARATOR;
							new File(tmpDir1).mkdirs();
							String absolutFilePath = tmpDir1 + file.getName();
							System.out.println("writing as   : " + absolutFilePath);
							OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
						}
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading LOV files from LogView application directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testLovReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".lov")
						&& !(file.getPath().toLowerCase().contains("spektrum")) ) {
					System.out.println("working with : " + file);
					try {
						HashMap<String, String> fileHeader = LogViewReader.getHeader(file.getAbsolutePath());
						String fileDeviceName = fileHeader.get(GDE.DEVICE_NAME);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);

						setupDataChannels(device);

						LogViewReader.read(file.getAbsolutePath());

						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(fileHeader.get(GDE.FILE_COMMENT));
						activeChannel.setSaved(true);
						//activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves

						for (String recordSetName : activeChannel.getRecordSetNames()) {
							//System.out.println("start drawing curves : " + recordSetName);
							RecordSet recordSet = activeChannel.get(recordSetName);
							if (recordSet != null) {
								if (!recordSet.hasDisplayableData()) recordSet.loadFileData(activeChannel.getFullQualifiedFileName(), false);
								//System.out.println("loaded FileData : " + recordSetName);
								activeChannel.setActiveRecordSet(recordSet);
								activeChannel.applyTemplate(recordSetName, true);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
							}
						}

						String tmpDir1 = this.tmpDir + "Write_1_OSD" + GDE.FILE_SEPARATOR;
						new File(tmpDir1).mkdirs();
						String absolutFilePath = tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_lov.osd";
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading GPX XML files from various application directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testGPXReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".gpx")) {
					System.out.println("working with : " + file);
					try {
						String deviceName = "GPXAdapter";
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);

						setupDataChannels(device);

						Channel activeChannel = this.channels.getActiveChannel();
						GPXDataReaderWriter.read(file.getAbsolutePath(), device, GDE.STRING_DOLLAR, activeChannel.getNumber());

						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setSaved(true);
						//activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves

						String tmpDir1 = this.tmpDir + "Write_1_OSD" + GDE.FILE_SEPARATOR;
						new File(tmpDir1).mkdirs();
						String absolutFilePath = tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_gpx.osd";
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	/**
	 * test reading GPX XML files from various application directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testIGCReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".igc")) {
					System.out.println("working with : " + file);
					try {
						String deviceName = "IGCAdapter";
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);

						setupDataChannels(device);

						Channel activeChannel = this.channels.getActiveChannel();
						IGCReaderWriter.read(file.getAbsolutePath(), device, GDE.STRING_DOLLAR, activeChannel.getNumber());

						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setSaved(true);
						//activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves

						String tmpDir1 = this.tmpDir + "Write_1_OSD" + GDE.FILE_SEPARATOR;
						new File(tmpDir1).mkdirs();
						String absolutFilePath = tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_igc.osd";
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading CSV files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testCSVAdapterWriterOsd() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			String tmpDir1 = this.tmpDir + GDE.FILE_SEPARATOR + "Write_1_OSD" + GDE.FILE_SEPARATOR;
			List<File> files = FileUtils.getFileListing(new File(tmpDir1), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".csv") && file.getName().toLowerCase().startsWith("LOG")) {
					System.out.println("working with : " + file);
					try {
						HashMap<String, String> fileHeader = OsdReaderWriter.getHeader(file.getAbsolutePath());
						String fileDeviceName = fileHeader.get(GDE.DEVICE_NAME);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);

						setupDataChannels(device);

						OsdReaderWriter.read(file.getAbsolutePath());

						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(fileHeader.get(GDE.FILE_COMMENT));
						activeChannel.setSaved(true);
						activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves

						for (String recordSetName : activeChannel.getRecordSetNames()) {
							RecordSet recordSet = activeChannel.get(recordSetName);
							if (recordSet != null) {
								activeChannel.setActiveRecordSet(recordSet);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
							}
						}

						String tmpDir2 = this.tmpDir + "Write_2_OSD" + GDE.FILE_SEPARATOR;
						new File(tmpDir2).mkdirs();
						String absolutFilePath = tmpDir2 + file.getName();
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading CSV(.txt) JLog2 files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testJlog2TxtReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			this.setDataPath(); //set the dataPath variable
			List<File> files = FileUtils.getFileListing(new File(this.dataPath.getAbsolutePath() + "/JLog2/"), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".txt") && !file.getAbsolutePath().toLowerCase().contains("config") 
						&& !file.getAbsolutePath().toLowerCase().contains("version") && !file.getAbsolutePath().toLowerCase().contains("info")) {
					System.out.println("working with : " + file);
					
					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "ISO-8859-1")); //$NON-NLS-1$
						String line = reader.readLine();
						boolean isCVS2SerialFormat = line.startsWith(deviceConfig.getDataBlockLeader()) && line.contains(deviceConfig.getDataBlockSeparator().value());
						reader.close();
					
						if (!isCVS2SerialFormat) {
							HashMap<String, String> fileHeader = CSVReaderWriter.getHeader(';', file.getAbsolutePath());
							String fileDeviceName = fileHeader.get(GDE.DEVICE_NAME);
							deviceConfig = this.deviceConfigurations.get(fileDeviceName);
							if (deviceConfig == null) throw new NotSupportedException("device = " + fileDeviceName + " is not supported or in list of active devices");
							IDevice device = this.getInstanceOfDevice(deviceConfig);
							this.application.setActiveDeviceWoutUI(device);

							setupDataChannels(device);
							fileHeader = CSVReaderWriter.evaluateType(';', fileHeader, deviceConfig);

							int channelConfigNumber = this.channels.getChannelNumber(fileHeader.get(GDE.CHANNEL_CONFIG_NAME));
							if (channelConfigNumber > device.getChannelCount()) {
								channelConfigNumber = 1;
								fileHeader.put(GDE.CHANNEL_CONFIG_NAME, this.channels.get(1).getChannelConfigKey());
							}
							this.channels.setActiveChannelNumber(channelConfigNumber);
							Channel activeChannel = this.channels.getActiveChannel();
							activeChannel.setFileName(file.getAbsolutePath());
							activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
							activeChannel.setSaved(true);

							RecordSet recordSet = CSVReaderWriter.read(';', file.getAbsolutePath(), "csv test", fileHeader.get(GDE.CSV_DATA_TYPE).equals(GDE.CSV_DATA_TYPE_RAW));

							if (recordSet != null) {
								activeChannel.setActiveRecordSet(recordSet);
								activeChannel.applyTemplate(recordSet.getName(), true);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
							}
						}
						else { // CSV2SerialAdapter file
							IDevice device = this.getInstanceOfDevice(deviceConfig);
							this.application.setActiveDeviceWoutUI(device);

							setupDataChannels(device);

							this.channels.setActiveChannelNumber(1);
							Channel activeChannel = this.channels.getActiveChannel();
							activeChannel.setFileName(file.getAbsolutePath());
							activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
							activeChannel.setSaved(true);

							CSVSerialDataReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1, true);
							RecordSet recordSet = activeChannel.getActiveRecordSet();

							if (recordSet != null) {
								activeChannel.setActiveRecordSet(recordSet);
								activeChannel.applyTemplate(recordSet.getName(), true);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
							}
						}

						String tmpDir1 = this.tmpDir + "Write_1_OSD" + GDE.FILE_SEPARATOR;
						new File(tmpDir1).mkdirs();
						String absolutFilePath = tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_txt.osd";
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading CSV(.txt) JLog2 files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testKosmikDatReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			this.setDataPath(); //set the dataPath variable
			List<File> files = FileUtils.getFileListing(new File(this.dataPath.getAbsolutePath() + "/Kosmik/"), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".dat")) {
					System.out.println("working with : " + file);
					
					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(2);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
						activeChannel.setSaved(true);

						CSVSerialDataReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 2, true);
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						String tmpDir1 = this.tmpDir + "Write_1_OSD" + GDE.FILE_SEPARATOR;
						new File(tmpDir1).mkdirs();
						String absolutFilePath = tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_dat.osd";
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading Graupner HoTT bin log files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testHoTTAdapterBinReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			String binDir = this.settings.getDataFilePath() + GDE.FILE_SEPARATOR + "HoTTAdapter" + GDE.FILE_SEPARATOR;
			List<File> files = FileUtils.getFileListing(new File(binDir), 2);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".bin")) {
					System.out.println("working with : " + file);
					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from NMEA file");
						activeChannel.setSaved(true);

						HoTTbinReader.read(file.getAbsolutePath());
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						String tmpDir1 = this.tmpDir + "Write_1_OSD" + GDE.FILE_SEPARATOR;
						new File(tmpDir1).mkdirs();
						String absolutFilePath = tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_bin.osd";
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading Graupner HoTT bin log files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testHoTTAdapter2BinReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			String binDir = this.settings.getDataFilePath() + GDE.FILE_SEPARATOR + "HoTTAdapter" + GDE.FILE_SEPARATOR;
			List<File> files = FileUtils.getFileListing(new File(binDir), 2);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".bin")) {
					System.out.println("working with : " + file);
					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR)) + "2";
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from NMEA file");
						activeChannel.setSaved(true);

						HoTTbinReader2.read(file.getAbsolutePath());
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						String tmpDir1 = this.tmpDir + "Write_1_OSD" + GDE.FILE_SEPARATOR;
						new File(tmpDir1).mkdirs();
						String absolutFilePath = tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_bin.osd";
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading CSV(.txt) JLog2 files in configured base directory (DataExplorer.properties and writes OSD files to %TEMP%\Write_1_OSD
	 * all files must identical except time stamp
	 */
	public final void testJetiAdapterReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			this.setDataPath(); //set the dataPath variable
			List<File> files = FileUtils.getFileListing(new File(this.dataPath.getAbsolutePath() + "/JetiAdapter/"), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG) || file.getAbsolutePath().toLowerCase().endsWith(GDE.FILE_ENDING_DOT_JML)) {
					System.out.println("working with : " + file);
					
					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1 + deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						//System.out.println("deviceName = " + deviceName);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");

						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);

						setupDataChannels(device);

						this.channels.setActiveChannelNumber(1);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from LOG file");
						activeChannel.setSaved(true);

						JetiDataReader.read(file.getAbsolutePath(), (JetiAdapter) device, "RecordSet", 1, true);
						RecordSet recordSet = activeChannel.getActiveRecordSet();

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName(), true);
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}

						String tmpDir1 = this.tmpDir + "Write_1_OSD" + GDE.FILE_SEPARATOR;
						new File(tmpDir1).mkdirs();
						String absolutFilePath = tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_log.osd";
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading data files from device directory check for GPS data content and writes IGC files to %TEMP%\Write_1_OSD
	 * all consistent files must red without failures
	 */
	public final void testGPSReaderIGCWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		this.setDataPath(); //set the dataPath variable

		try {
			List<File> files = FileUtils.getFileListing(this.dataPath, 2);

			IgcExportDialog igcExport = new IgcExportDialog();
			int ordinalLongitude = 1, ordinalLatitude = 0, ordinalAltitude = 2;
			RecordSet recordSet = null;
			
			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".nmea") || file.getAbsolutePath().toLowerCase().endsWith(".csv") || file.getAbsolutePath().toLowerCase().endsWith(".bin")) {
					
					try {
						//System.out.println("file.getPath() = " + file.getPath());
						String deviceName = file.getPath().substring(0, file.getPath().lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.substring(1+deviceName.lastIndexOf(GDE.FILE_SEPARATOR));
						deviceName = deviceName.contains(GDE.STRING_UNDER_BAR) ? deviceName.substring(1+deviceName.lastIndexOf(GDE.STRING_UNDER_BAR)) : deviceName;
						//System.out.println("deviceName = " + deviceName);
						if (deviceName.startsWith("NMEA") || deviceName.startsWith("GPS") || deviceName.startsWith("DataVario") || deviceName.startsWith("LinkVario") 
								|| deviceName.startsWith("HoTT")) {
						
							DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
							if (deviceConfig == null) throw new NotSupportedException("device = " + deviceName + " is not supported or in list of active devices");
	
							// GPS, GPS-Logger, WStech Varios, HoTTbiaries
							IDevice device = this.getInstanceOfDevice(deviceConfig);
							this.application.setActiveDeviceWoutUI(device);
	
							setupDataChannels(device);
	
							this.channels.setActiveChannelNumber(1);
							Channel activeChannel = this.channels.getActiveChannel();
							activeChannel.setFileName(file.getAbsolutePath());
							activeChannel.setFileDescription(StringHelper.getDateAndTime() + " - imported from GPS file");
							activeChannel.setSaved(true);
	
							if (file.getAbsolutePath().toLowerCase().endsWith(".nmea")) {
								recordSet = NMEAReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1);
								if (recordSet != null) {
									activeChannel.setActiveRecordSet(recordSet);
								}
								//NMEA, GPS-Logger
								ordinalLongitude = 1;
								ordinalLatitude = 0;
								ordinalAltitude = 2;
							}
							else if (file.getAbsolutePath().toLowerCase().endsWith(".csv") && (device.getName().startsWith("DataVario") || device.getName().startsWith("LinkVario"))) {
								recordSet = CSVSerialDataReaderWriter.read(file.getAbsolutePath(), device, "RecordSet", 1, true);
								if (recordSet != null) {
									activeChannel.setActiveRecordSet(recordSet);
								}
								if (!device.isActualRecordSetWithGpsData()) continue;
								//WStech
								ordinalLongitude = 7;
								ordinalLatitude = 8;
								ordinalAltitude = 9;
							}
							else if (file.getAbsolutePath().toLowerCase().endsWith(".bin")) {
								HoTTbinReader.read(file.getAbsolutePath());
								this.channels.setActiveChannelNumber(3);
								activeChannel = this.channels.getActiveChannel();
								activeChannel.setActiveRecordSet(activeChannel.getFirstRecordSetName());
								recordSet = activeChannel.getActiveRecordSet();
								if (recordSet != null) {
									activeChannel.setActiveRecordSet(recordSet);
								}
								else continue;
								if (!device.isActualRecordSetWithGpsData()) continue;
								//HoTT
								ordinalLongitude = 2;
								ordinalLatitude = 1;
								ordinalAltitude = 3;
							}
							
							System.out.println("working with : " + file);
							if (recordSet != null) {
								activeChannel.applyTemplate(recordSet.getName(), true);
								drawCurves(recordSet, 1024, 768);
							}
	
							String tmpDir1 = this.tmpDir + "Write_1_OSD" + GDE.FILE_SEPARATOR;
							new File(tmpDir1).mkdirs();
							String absolutFilePath = tmpDir1 + file.getName();
							absolutFilePath = absolutFilePath.trim().substring(0, absolutFilePath.lastIndexOf(GDE.STRING_DOT)) + GDE.FILE_ENDING_DOT_IGC;
							System.out.println("writing as   : " + absolutFilePath);
							igcExport.initializeValues(ordinalLongitude, ordinalLatitude, ordinalAltitude);
							IGCReaderWriter.write(device, absolutFilePath, igcExport.getHeader(), recordSet, ordinalLongitude, ordinalLatitude, ordinalAltitude, 487);
						}
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * test reading OSD files %TEMP%\Write_1_OSD and writes OSD files to %TEMP%\Write_2_OSD
	 * all files must identical except time stamp
	 */
	public final void testReaderWriterOsd() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			String tmpDir1 = this.tmpDir + GDE.FILE_SEPARATOR + "Write_1_OSD" + GDE.FILE_SEPARATOR;
			List<File> files = FileUtils.getFileListing(new File(tmpDir1), 1);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".osd")) {
					System.out.println("working with : " + file);
					try {
						HashMap<String, String> fileHeader = OsdReaderWriter.getHeader(file.getAbsolutePath());
						String fileDeviceName = fileHeader.get(GDE.DEVICE_NAME);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);

						setupDataChannels(device);

						OsdReaderWriter.read(file.getAbsolutePath());

						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						activeChannel.setFileDescription(fileHeader.get(GDE.FILE_COMMENT));
						activeChannel.setSaved(true);
						activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves

						for (String recordSetName : activeChannel.getRecordSetNames()) {
							RecordSet recordSet = activeChannel.get(recordSetName);
							if (recordSet != null) {
								activeChannel.setActiveRecordSet(recordSet);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
							}
						}

						String tmpDir2 = this.tmpDir + "Write_2_OSD" + GDE.FILE_SEPARATOR;
						new File(tmpDir2).mkdirs();
						String absolutFilePath = tmpDir2 + file.getName();
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), GDE.DATA_EXPLORER_FILE_VERSION_INT);
					}
					catch (Exception e) {
						e.printStackTrace();
						failures.put(file.getAbsolutePath(), e);
					}
				}
			}

		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.toString());
		}

		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}
}
