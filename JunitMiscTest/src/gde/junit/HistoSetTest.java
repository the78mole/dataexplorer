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

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017 Winfried Bruegmann
									2016,2017 Thomas Eickert
****************************************************************************************/
package gde.junit;

import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import gde.data.Channel;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.histo.datasources.HistoSet;
import gde.utils.FileUtils;

public class HistoSetTest extends TestSuperClass { // TODO for junit tests in general it may be better to choose another directory structure: http://stackoverflow.com/a/2388285
	private final static String	$CLASS_NAME	= HistoSetTest.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
		log.setLevel(Level.INFO);
		log.setUseParentHandlers(true);
	}

	/**
	 * fills histoSet by reading all logs into all devices and all channels.
	 * uses cache data.
	 * ET Win PC elapsed times for DataFilesTestSamples\DataExplorer with 144 files resulting in 223 vaults (using a zipped cache):
	 *  - 1st run w/o vault cache 72 sec
	 *  - 2nd run with vault cache 23 sec
	 */
	public void testBuildHistoSet4TestSamples() {
		System.out
				.println(String.format("Max Memory=%,11d   Total Memory=%,11d   Free Memory=%,11d", Runtime.getRuntime().maxMemory(), Runtime.getRuntime().totalMemory(), Runtime.getRuntime().freeMemory()));
		TreeMap<String, Exception> failures = new TreeMap<String, Exception>();

		HistoSet histoSet = HistoSet.getInstance();

		this.setDataPath(DataSource.TESTDATA, Paths.get(""));
		// >>> take one of these optional data sources for a smaller test portion <<<
		//		this.setDataPath(DataSource.INDIVIDUAL, Paths.get("C:\\_Java\\workspace\\DataFilesTestSamples\\DataExplorer", "_Thomas", "DataExplorer"));
		//		this.setDataPath(DataSource.INDIVIDUAL, Paths.get("C:\\_Java\\workspace\\DataFilesTestSamples\\DataExplorer", "_Winfried", "DataExplorer"));

		FileUtils.checkDirectoryAndCreate(this.dataPath.toString());
		histoSet.initialize();
		try {
			for (Entry<String, DeviceConfiguration> deviceEntry : this.deviceConfigurations.entrySet()) {
				IDevice device = setDevice(deviceEntry.getKey());
				setupDataChannels(device);
				setupDeviceChannelObject(deviceEntry.getKey(), 1, "");
				for (Entry<Integer, Channel> channelEntry : this.channels.entrySet()) {
					this.channels.setActiveChannelNumber(channelEntry.getKey());
					histoSet.rebuild4Test();
					System.out.println(String.format("%33.44s  channelNumber=%2d  histoSetSize==%,11d", this.application.getActiveDevice().getName(), this.channels.getActiveChannelNumber(), histoSet.size()));
				}
			}

			histoSet.setHistoFilePaths4Test(this.dataPath.toPath(), 5, this.deviceConfigurations);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}

		System.out.println(String.format("%,11d files processed from  %s", histoSet.getUnsuppressedTrusses().size(), this.dataPath.getAbsolutePath()));
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
		String deviceName = fileDeviceName;
		if (this.legacyDeviceNames.get(fileDeviceName) != null) deviceName = this.legacyDeviceNames.get(fileDeviceName);
		if (deviceName.toLowerCase().contains("hottviewer") || deviceName.toLowerCase().contains("mpu")) return null; // iCharger308DUO gde.device.UsbPortType missing
		DeviceConfiguration deviceConfig = this.deviceConfigurations.get(deviceName);
		if (deviceConfig == null) return null;
		IDevice device = this.getInstanceOfDevice(deviceConfig);
		this.application.setActiveDeviceWoutUI(device);
		return device;
	}

}
