/**
 * 
 */
package gde.junit;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;

import gde.GDE;
import gde.data.Channel;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.NotSupportedException;
import gde.io.CSVReaderWriter;
import gde.io.LogViewReader;
import gde.io.OsdReaderWriter;
import gde.ui.DataExplorer;
import gde.utils.FileUtils;
import gde.utils.OperatingSystemHelper;
import gde.utils.StringHelper;

/**
 * @author brueg
 *
 */
public class TestFileReaderWriter extends TestSuperClass {

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();		
	}

	/**
	 * test reading LOV files from LogView application directory and writes OSD files to %TEMP%\Write_1_OSD
	 * all consitent files must red without failures
	 */
	public final void testCsvReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		//this.devicePath = new File(this.tmpDir + "Write_0_OSD"); 
		this.devicePath = new File(this.settings.getDataFilePath());
		//this.devicePath = new File(this.settings.getDataFilePath() + GDE.FILE_SEPARATOR + "eStationBC6");

		try {
			List<File> files = FileUtils.getFileListing(this.devicePath);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".csv") && !file.getPath().toLowerCase().contains("csv2serialadapter")) {
					System.out.println("working with : " + file);
					try {
						HashMap<String, String> fileHeader = CSVReaderWriter.getHeader(';', file.getAbsolutePath());
						String fileDeviceName = fileHeader.get(GDE.DEVICE_NAME);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + fileDeviceName + " is not supported or in list of active devices");
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);
						
						setupDataChannels(device);
						fileHeader = CSVReaderWriter.evaluateType(';', fileHeader, deviceConfig);

						int channelConfigNumber = this.channels.getChannelNumber(fileHeader.get(GDE.CHANNEL_CONFIG_NAME));
						if (channelConfigNumber > device.getChannelCount()){
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
						
						String tmpDir1 = this.tmpDir + "Write_1_OSD" + GDE.FILE_SEPARATOR;
						new File(tmpDir1).mkdirs();
						String absolutFilePath = tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length()-4)+"_cvs.osd";
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
			List<File> files = FileUtils.getFileListing(new File(tmpDir1));

			for (File file : files) {
			String filePath = file.getAbsolutePath();
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

							String tmpDir2 = this.tmpDir + "Write_2_OSD" + GDE.FILE_SEPARATOR;
							new File(tmpDir2).mkdirs();
							String absolutFilePath = tmpDir2 + file.getName();
							absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_abs.csv";
							System.out.println("writing as   : " + absolutFilePath);
							CSVReaderWriter.write(';', activeChannel.getActiveRecordSet().getName(), absolutFilePath, false);

							absolutFilePath = tmpDir2 + file.getName();
							absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length() - 4) + "_raw.csv";
							System.out.println("writing as   : " + absolutFilePath);
							CSVReaderWriter.write(';', activeChannel.getActiveRecordSet().getName(), absolutFilePath, true);
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
	 * test reading OSD files from directories used by OSDE application and writes OSD files to %TEMP%\Write_1_OSD
	 * all consitent files must red without failures, 
	 * the written files might different due to code updates (add/change properties)
	 */
	public final void testOsdReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		//this.devicePath = new File(this.tmpDir + "Write_0_OSD"); 
		this.devicePath = new File(this.settings.getDataFilePath());
		//this.devicePath = new File(this.settings.getDataFilePath() + GDE.FILE_SEPARATOR + "UniLog");

		try {
			List<File> files = FileUtils.getFileListing(this.devicePath);

			for (File file : files) {
				String filePath = file.getAbsolutePath();
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
	 * all consitent files must red without failures
	 */
	public final void testLovReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		//this.devicePath = new File(this.tmpDir + "Write_0_OSD"); 
		String dataPath = this.settings.getDataFilePath();
		dataPath = dataPath.substring(0, dataPath.indexOf(DataExplorer.APPLICATION_NAME)) + "LogView";
		this.devicePath = new File(dataPath);
		//this.devicePath = new File("d:\\Documents\\LogView" + GDE.FILE_SEPARATOR + "UniLog");

		try {
			List<File> files = FileUtils.getFileListing(this.devicePath);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".lov")) {
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
								if (!recordSet.hasDisplayableData()) 
									recordSet.loadFileData(activeChannel.getFullQualifiedFileName(), false);
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
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length()-4)+"_lov.osd";
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
	public final void testReaderWriterOsd() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		try {
			String tmpDir1 = this.tmpDir + GDE.FILE_SEPARATOR + "Write_1_OSD" + GDE.FILE_SEPARATOR;
			List<File> files = FileUtils.getFileListing(new File(tmpDir1));

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
