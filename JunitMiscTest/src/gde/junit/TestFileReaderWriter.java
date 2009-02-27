/**
 * 
 */
package osde.junit;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;

import osde.OSDE;
import osde.data.Channel;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.exception.NotSupportedException;
import osde.io.CSVReaderWriter;
import osde.io.LogViewReader;
import osde.io.OsdReaderWriter;
import osde.utils.FileUtils;
import osde.utils.StringHelper;

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
	 * test reading OSD files from directories used by OSDE application and writes OSD files to %TEMP%\Write_1_OSD
	 * all consitent files must red without failures, 
	 * the written files might different due to code updates (add/change properties)
	 */
	public final void testOsdReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		//this.devicePath = new File(this.tmpDir + "Write_0_OSD"); 
		this.devicePath = new File(this.settings.getDataFilePath());
		//this.devicePath = new File(this.settings.getDataFilePath() + OSDE.FILE_SEPARATOR + "UniLog");

		try {
			List<File> files = FileUtils.getFileListing(this.devicePath);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".osd")) {
					System.out.println("working with : " + file);
					try {
						HashMap<String, String> fileHeader = OsdReaderWriter.getHeader(file.getAbsolutePath());
						String fileDeviceName = fileHeader.get(OSDE.DEVICE_NAME);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);
						
						setupDataChannels(device);

						OsdReaderWriter.read(file.getAbsolutePath());

						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						this.channels.setFileDescription(fileHeader.get(OSDE.FILE_COMMENT));
						this.channels.setSaved(true);
						//activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves
						
						for (String recordSetName : activeChannel.getRecordSetNames()) {
							RecordSet recordSet = activeChannel.get(recordSetName);
							if (recordSet != null) {
								if (!recordSet.hasDisplayableData()) 
									recordSet.loadFileData(activeChannel.getFullQualifiedFileName(), false);
								activeChannel.setActiveRecordSet(recordSet);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
							}
						}

						String tmpDir1 = this.tmpDir + "Write_1_OSD" + OSDE.FILE_SEPARATOR;
						new File(tmpDir1).mkdirs();
						String absolutFilePath = tmpDir1 + file.getName();
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), 1);
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
	public final void testCsvReaderOsdWriter() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();

		//this.devicePath = new File(this.tmpDir + "Write_0_OSD"); 
		this.devicePath = new File(this.settings.getDataFilePath());
		//this.devicePath = new File(this.settings.getDataFilePath() + OSDE.FILE_SEPARATOR + "UniLog");

		try {
			List<File> files = FileUtils.getFileListing(this.devicePath);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".csv")) {
					System.out.println("working with : " + file);
					try {
						HashMap<String, String> fileHeader = CSVReaderWriter.getHeader(';', file.getAbsolutePath());
						String fileDeviceName = fileHeader.get(OSDE.DEVICE_NAME);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
						if (deviceConfig == null) throw new NotSupportedException("device = " + fileDeviceName + " is not supported or in list of active devices");
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);
						
						setupDataChannels(device);
						fileHeader = CSVReaderWriter.evaluateType(';', fileHeader, deviceConfig);

						int channelConfigNumber = this.channels.getChannelNumber(fileHeader.get(OSDE.CHANNEL_CONFIG_NAME));
						if (channelConfigNumber > device.getChannelCount()){
							channelConfigNumber = 1;
							fileHeader.put(OSDE.CHANNEL_CONFIG_NAME, this.channels.get(1).getConfigKey());
						}
						this.channels.setActiveChannelNumber(channelConfigNumber);
						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						this.channels.setFileDescription(StringHelper.getDateAndTime() + " - imported from CSV file");
						this.channels.setSaved(true);
						
						RecordSet recordSet = CSVReaderWriter.read(';', file.getAbsolutePath(), "csv test", fileHeader.get(OSDE.CSV_DATA_TYPE).equals(OSDE.CSV_DATA_TYPE_RAW));

						if (recordSet != null) {
							activeChannel.setActiveRecordSet(recordSet);
							activeChannel.applyTemplate(recordSet.getName());
							//device.makeInActiveDisplayable(recordSet);
							drawCurves(recordSet, 1024, 768);
						}
						
						String tmpDir1 = this.tmpDir + "Write_1_OSD" + OSDE.FILE_SEPARATOR;
						new File(tmpDir1).mkdirs();
						String absolutFilePath = tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length()-4)+"_cvs.osd";
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), 1);
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
			String tmpDir1 = this.tmpDir + OSDE.FILE_SEPARATOR + "Write_1_OSD" + OSDE.FILE_SEPARATOR;
			List<File> files = FileUtils.getFileListing(new File(tmpDir1));

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".osd")) {
					System.out.println("working with : " + file);
					try {
						HashMap<String, String> fileHeader = OsdReaderWriter.getHeader(file.getAbsolutePath());
						String fileDeviceName = fileHeader.get(OSDE.DEVICE_NAME);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);
						
						setupDataChannels(device);

						OsdReaderWriter.read(file.getAbsolutePath());

						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						this.channels.setFileDescription(fileHeader.get(OSDE.FILE_COMMENT));
						this.channels.setSaved(true);
						activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves
						
						for (String recordSetName : activeChannel.getRecordSetNames()) {
							RecordSet recordSet = activeChannel.get(recordSetName);
							if (recordSet != null) {
								activeChannel.setActiveRecordSet(recordSet);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
							}
						}

						String tmpDir2 = this.tmpDir + "Write_2_OSD" + OSDE.FILE_SEPARATOR;
						new File(tmpDir2).mkdirs();
						String absolutFilePath = tmpDir2 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length()-4)+"_abs.csv";
						System.out.println("writing as   : " + absolutFilePath);
						CSVReaderWriter.write(';', activeChannel.getActiveRecordSet().getName(), absolutFilePath, false);

						absolutFilePath = tmpDir2 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length()-4)+"_raw.csv";
						System.out.println("writing as   : " + absolutFilePath);
						CSVReaderWriter.write(';', activeChannel.getActiveRecordSet().getName(), absolutFilePath, true);
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
		this.devicePath = new File("d:\\Documents\\LogView");
		//this.devicePath = new File("d:\\Documents\\LogView" + OSDE.FILE_SEPARATOR + "UniLog");

		try {
			List<File> files = FileUtils.getFileListing(this.devicePath);

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".lov")) {
					System.out.println("working with : " + file);
					try {
						HashMap<String, String> fileHeader = LogViewReader.getHeader(file.getAbsolutePath());
						String fileDeviceName = fileHeader.get(OSDE.DEVICE_NAME);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);
						
						setupDataChannels(device);

						LogViewReader.read(file.getAbsolutePath());

						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						this.channels.setFileDescription(fileHeader.get(OSDE.FILE_COMMENT));
						this.channels.setSaved(true);
						//activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves
						
						for (String recordSetName : activeChannel.getRecordSetNames()) {
							RecordSet recordSet = activeChannel.get(recordSetName);
							if (recordSet != null) {
								if (!recordSet.hasDisplayableData()) 
									recordSet.loadFileData(activeChannel.getFullQualifiedFileName(), false);
								activeChannel.setActiveRecordSet(recordSet);
								activeChannel.applyTemplate(recordSetName);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
							}
						}

						String tmpDir1 = this.tmpDir + "Write_1_OSD" + OSDE.FILE_SEPARATOR;
						new File(tmpDir1).mkdirs();
						String absolutFilePath = tmpDir1 + file.getName();
						absolutFilePath = absolutFilePath.substring(0, absolutFilePath.length()-4)+"_lov.osd";
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), 1);
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
			String tmpDir1 = this.tmpDir + OSDE.FILE_SEPARATOR + "Write_1_OSD" + OSDE.FILE_SEPARATOR;
			List<File> files = FileUtils.getFileListing(new File(tmpDir1));

			for (File file : files) {
				if (file.getAbsolutePath().toLowerCase().endsWith(".osd")) {
					System.out.println("working with : " + file);
					try {
						HashMap<String, String> fileHeader = OsdReaderWriter.getHeader(file.getAbsolutePath());
						String fileDeviceName = fileHeader.get(OSDE.DEVICE_NAME);
						DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
						IDevice device = this.getInstanceOfDevice(deviceConfig);
						this.application.setActiveDeviceWoutUI(device);
						
						setupDataChannels(device);

						OsdReaderWriter.read(file.getAbsolutePath());

						Channel activeChannel = this.channels.getActiveChannel();
						activeChannel.setFileName(file.getAbsolutePath());
						this.channels.setFileDescription(fileHeader.get(OSDE.FILE_COMMENT));
						this.channels.setSaved(true);
						activeChannel.checkAndLoadData(); //perform this operation triggered by drawCurves
						
						for (String recordSetName : activeChannel.getRecordSetNames()) {
							RecordSet recordSet = activeChannel.get(recordSetName);
							if (recordSet != null) {
								activeChannel.setActiveRecordSet(recordSet);
								//device.makeInActiveDisplayable(recordSet);
								drawCurves(recordSet, 1024, 768);
							}
						}

						String tmpDir2 = this.tmpDir + "Write_2_OSD" + OSDE.FILE_SEPARATOR;
						new File(tmpDir2).mkdirs();
						String absolutFilePath = tmpDir2 + file.getName();
						System.out.println("writing as   : " + absolutFilePath);
						OsdReaderWriter.write(absolutFilePath, this.channels.getActiveChannel(), 1);
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
