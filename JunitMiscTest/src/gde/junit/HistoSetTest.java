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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
									2016 Thomas Eickert
****************************************************************************************/
package gde.junit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.naming.OperationNotSupportedException;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.HistoRecordSet;
import gde.data.HistoSet;
import gde.data.HistoSet.RebuildStep;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.graupner.HoTTbinHistoReader;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.NotSupportedFileFormatException;
import gde.histocache.HistoVault;
import gde.io.HistoOsdReaderWriter;
import gde.io.OsdReaderWriter;
import gde.ui.dialog.SettingsDialog;
import gde.utils.FileUtils;

public class HistoSetTest extends TestSuperClass { // TODO for junit tests in general it may be better to choose another directory structure: http://stackoverflow.com/a/2388285
	private final static String	$CLASS_NAME	= HistoSetTest.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	private static final double	DELTA				= 1e-13;
	private static int					count				= 0;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
		log.setLevel(Level.INFO);
		log.setUseParentHandlers(true);
	}

	private List<String> getProblemFileNames() {
		ArrayList<String> problemFileNames = new ArrayList<String>();
		//		problemFileNames.add("2016-02-12_T-Rex 250_.osd"); // header has java.io.UTFDataFormatException: malformed input around byte 8
		//		problemFileNames.add("2015-05-14_T-Rex 250 Kanaele.osd"); // RecordSet #1 consists of 86 records whereas the recordSetDataPointer value of the next recordSet allows 85 records only
		return problemFileNames;
	}

	public void testBuildHistoSet() {
		TreeMap<String, Exception> failures = new TreeMap<String, Exception>();

		this.setDataPath(DataSource.TESTDATA, Paths.get(""));
		String fileRootDir = this.dataPath.getAbsolutePath();

		FileUtils.checkDirectoryAndCreate(fileRootDir);
		String[] directories = new File(fileRootDir).list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		System.out.println(Arrays.toString(directories));

		for (int i = 0; i < directories.length; i++) {
			this.setDataPath(DataSource.TESTDATA, Paths.get(directories[i]));
			setupDeviceChannelObject(directories[i], 1, "");
			for (Entry<Integer, Channel> channelEntry : this.channels.entrySet()) {
				setupDeviceChannelObject(directories[i], channelEntry.getKey(), "");
				try {
					HistoSet.getInstance().rebuild4Screening(RebuildStep.A_HISTOSET, false);
				}
				catch (Exception e) {
					e.printStackTrace();
					failures.put("file.getAbsolutePath()", e);
				}
			}
		}

		System.out.println(String.format("%,11d files processed from  %s", HistoSetTest.count, fileRootDir));
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Exception> failure : failures.entrySet()) {
			sb.append(failure).append("\n");
		}
		//			System.out.println(sb);
		if (failures.size() > 0) fail(sb.toString());
	}

	public void testBuildHistoSet4TestSamples() {
		System.out
				.println(String.format("Max Memory=%,11d   Total Memory=%,11d   Free Memory=%,11d", Runtime.getRuntime().maxMemory(), Runtime.getRuntime().totalMemory(), Runtime.getRuntime().freeMemory()));
		List<String> problemFileNames = getProblemFileNames();
		TreeMap<String, Exception> failures = new TreeMap<String, Exception>();

		HistoSet histoSet = HistoSet.getInstance();
		
		this.setDataPath(DataSource.TESTDATA, Paths.get(""));
		String fileRootDir = this.dataPath.getAbsolutePath();
		// >>> take one of these optional data sources for the test <<<
		//		Path dirPath = Paths.get(fileRootDir, "_Thomas", "DataExplorer");  // use with empty datafilepath in DataExplorer.properties
		//		Path dirPath = Paths.get(fileRootDir, "_Winfried", "DataExplorer"); // use with empty datafilepath in DataExplorer.properties
		Path dirPath = FileSystems.getDefault().getPath(fileRootDir); // takes all Files from datafilepath in DataExplorer.properties or from DataFilesTestSamples if empty datafilepath

		FileUtils.checkDirectoryAndCreate(dirPath.toString());
			histoSet.initialize();
		try {
			histoSet.setHistoFilePaths4Test(dirPath, 11);

			for (Entry<String, DeviceConfiguration> deviceEntry : this.deviceConfigurations.entrySet()) {
				IDevice device = setDevice(deviceEntry.getKey());
				setupDataChannels(device);
				setupDeviceChannelObject(deviceEntry.getKey(), 1, "");
				for (Entry<Integer, Channel> channelEntry : this.channels.entrySet()) {
					this.channels.setActiveChannelNumber(channelEntry.getKey());
					histoSet.rebuild4Test();
					System.out.println(String.format("Device=%3.33s  channelNumber=%2d  histoSetSize==%,11d", this.application.getActiveDevice().getName(), this.channels.getActiveChannelNumber(), histoSet.size()));
				}
			}
		}
		catch (Exception  e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.toString());
		}

		System.out.println(String.format("%,11d files processed from  %s", HistoSetTest.count, dirPath.toString()));
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Exception> failure : failures.entrySet()) {
			sb.append(failure).append("\n");
		}
		//		System.out.println(sb);
		if (failures.size() > 0) fail(sb.toString());
	}

	public void testReadBinOsdFiles() {
		System.out
				.println(String.format("Max Memory=%,11d   Total Memory=%,11d   Free Memory=%,11d", Runtime.getRuntime().maxMemory(), Runtime.getRuntime().totalMemory(), Runtime.getRuntime().freeMemory()));
		List<String> problemFileNames = getProblemFileNames();
		TreeMap<String, Exception> failures = new TreeMap<String, Exception>();

		this.setDataPath(DataSource.TESTDATA, Paths.get(""));
		String fileRootDir = this.dataPath.getAbsolutePath();
		// >>> take one of these optional data sources for the test <<<
		//		Path dirPath = Paths.get(fileRootDir, "_Thomas", "DataExplorer");  // use with empty datafilepath in DataExplorer.properties
		//		Path dirPath = Paths.get(fileRootDir, "_Winfried", "DataExplorer"); // use with empty datafilepath in DataExplorer.properties
		Path dirPath = FileSystems.getDefault().getPath(fileRootDir); // takes all Files from datafilepath in DataExplorer.properties or from DataFilesTestSamples if empty datafilepath

		FileUtils.checkDirectoryAndCreate(dirPath.toString());
		try {
			List<File> files = FileUtils.getFileListing(dirPath.toFile(), 11);

			for (File file : files) {
				if (!problemFileNames.contains(file.getName())) {
					if (file.getName().endsWith(GDE.FILE_ENDING_DOT_BIN)) {
						HistoSetTest.count++;
						int maxTime_sec = 0;
						String fileDeviceName = "HoTTAdapter";
						IDevice device = setDevice(fileDeviceName);
						setupDataChannels(device);
						for (Entry<Integer, Channel> channelEntry : this.channels.entrySet()) {
							this.channels.setActiveChannelNumber(channelEntry.getKey());
							try {
								String objectDirectory = this.settings.getValidatedObjectKey(file.toPath().getParent().getFileName().toString()).orElse(GDE.STRING_EMPTY).intern();
								HistoVault truss = HistoVault.createTruss(objectDirectory, file);
								HistoRecordSet recordSet = HoTTbinHistoReader.read(truss);
								maxTime_sec = recordSet.getMaxTime_ms() / 1000 > maxTime_sec ? (int) recordSet.getMaxTime_ms() / 1000 : maxTime_sec;
								System.out.println(String.format("binFile processed      channel=%d  MaxTime_sec=%,9d  Bytes=%,11d %s", this.channels.getActiveChannelNumber(), (int) recordSet.getMaxTime_ms() / 1000,
										file.length(), file.toPath().toAbsolutePath().toString()));
							}
							catch (Exception e) {
								e.printStackTrace();
								failures.put(file.getAbsolutePath(), e);
							}
						}
						if (maxTime_sec < 60 && !failures.containsKey(file.getAbsolutePath())) {
							System.out.println(String.format("WARNING: binFile too small  MaxTime_sec= %,6d  Bytes=%,11d %s", maxTime_sec, file.length(), file.getAbsolutePath()));
							failures.put(file.getAbsolutePath(),
									new OperationNotSupportedException(device.getName() + String.format(" WARNING: binFile too small  MaxTime_sec= %,6d  Bytes=%,11d", maxTime_sec, file.length())));
						}

					}
					else if (file.getName().endsWith(GDE.FILE_ENDING_DOT_OSD)) {
						HistoSetTest.count++;
						HashMap<String, String> header = null;
						try {
							header = HistoOsdReaderWriter.getHeader(file.getAbsolutePath());
						}
						catch (Exception e) {
							System.out.println(file.getAbsolutePath());
							e.printStackTrace();
							failures.put(file.getAbsolutePath(), e);
						}
						if (header != null) {
							IDevice device = setDevice(header.get(GDE.DEVICE_NAME));
							if (device == null) {
								failures.put(file.getAbsolutePath(), new OperationNotSupportedException(">" + header.get(GDE.DEVICE_NAME) + "<  device error: probably missing device XML"));
							}
							else {
								int maxTime_sec = 0;
								setupDataChannels(device);
								for (Entry<Integer, Channel> channelEntry : this.channels.entrySet()) {
									this.channels.setActiveChannelNumber(channelEntry.getKey());
									try {
										String objectDirectory = this.settings.getValidatedObjectKey(file.toPath().getParent().getFileName().toString()).orElse(GDE.STRING_EMPTY).intern();
										HistoVault truss = HistoVault.createTruss(objectDirectory, file.toPath(), file.lastModified(), 0, file.getName(), objectDirectory, file.lastModified(),
												this.application.getActiveChannelNumber(), objectDirectory);
										// todo HistoRecordSet recordSet = HistoOsdReaderWriter.readHisto(truss);
//										System.out.println(String.format("osdFile processed  channel=%d  MaxTime_sec=%,9d  Bytes=%,11d %s", this.channels.getActiveChannelNumber(), (int) recordSet.getMaxTime_ms() / 1000,
//												file.length(), file.toPath().toAbsolutePath().toString()));
									}
									catch (Exception e) {
										System.out.println(file.getAbsolutePath());
										e.printStackTrace();
										failures.put(file.getAbsolutePath(), e);
									}
								}
								if (maxTime_sec <= 0 && !failures.containsKey(file.getAbsolutePath())) {
									failures.put(file.getAbsolutePath(), new OperationNotSupportedException("WARNING: maxTime_sec = 0"));
								}
							}
						}
					}
				}
			}
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.toString());
		}

		System.out.println(String.format("%,11d files processed from  %s", HistoSetTest.count, dirPath.toString()));
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Exception> failure : failures.entrySet()) {
			sb.append(failure).append("\n");
		}
		//		System.out.println(sb);
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * @param fileDeviceName
	 * @return
	 */
	private IDevice setDevice(String fileDeviceName) {
		if (this.legacyDeviceNames.get(fileDeviceName) != null) fileDeviceName = this.legacyDeviceNames.get(fileDeviceName);
		if (fileDeviceName.toLowerCase().contains("hottviewer") || fileDeviceName.toLowerCase().contains("mpu")) return null; // iCharger308DUO gde.device.UsbPortType missing
		DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
		if (deviceConfig == null) return null;
		IDevice device = this.getInstanceOfDevice(deviceConfig);
		this.application.setActiveDeviceWoutUI(device);
		return device;
	}

}
