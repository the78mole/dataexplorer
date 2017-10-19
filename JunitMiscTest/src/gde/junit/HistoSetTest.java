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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import gde.config.Settings;
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
	 * Fills the histoset by reading all logs into all devices and all channels.
	 * uses cache data in a 2nd internal loop.
	 * ET 12.2016: Win PC elapsed times for DataFilesTestSamples\DataExplorer with 144 files resulting in 223 vaults (using a zipped cache):
	 *  - 1st run w/o vault cache 72 sec
	 *  - 2nd run with vault cache 23 sec
	 * ET 10.2017: Win PC elapsed times for DataFilesTestSamples\DataExplorer with 224 files resulting in 333 vaults (using a UN/zipped cache) including 73 bin files:
	 *  - 1st run w/o vault cache 230/225 sec (20,7 MBi cache)
	 *  - 2nd run with vault cache 56/56 sec (2,49 MBi cache)
	 */
	public void testBuildHistoSet4TestSamples() {
		System.out.println(String.format("Max Memory=%,11d   Total Memory=%,11d   Free Memory=%,11d", Runtime.getRuntime().maxMemory(), Runtime.getRuntime().totalMemory(),
				Runtime.getRuntime().freeMemory()));
		TreeMap<String, Exception> failures = new TreeMap<String, Exception>();

		HistoSet histoSet = HistoSet.getInstance();

		this.setDataPath(DataSource.TESTDATA, Paths.get(""));
		// >>> take one of these optional data sources for a smaller test portion <<<
		//		this.setDataPath(DataSource.INDIVIDUAL, Paths.get("C:\\_Java\\workspace\\DataFilesTestSamples\\DataExplorer", "_Thomas", "DataExplorer"));
		//		this.setDataPath(DataSource.INDIVIDUAL, Paths.get("C:\\_Java\\workspace\\DataFilesTestSamples\\DataExplorer", "_Winfried", "DataExplorer"));

		FileUtils.checkDirectoryAndCreate(this.dataPath.toString());
		List<String> objectKeyCandidates = Settings.getInstance().getObjectKeyCandidates(this.deviceConfigurations);
		objectKeyCandidates.add(""); // empty string for 'device oriented'

		List<Long> elapsed_sec = new ArrayList<>();
		this.settings.resetHistoCache();
		for (int i = 0; i < 2; i++) { // run twice for cache effects measuring
			long nanoTime = System.nanoTime();
			histoSet.initialize();
			try {
				for (Entry<String, DeviceConfiguration> deviceEntry : this.deviceConfigurations.entrySet()) {
					IDevice device = setDevice(deviceEntry.getKey());
					if (device != null) {
						setupDataChannels(device);
						System.out.println(this.application.getActiveDevice().getName());
						for (String objectKey : objectKeyCandidates) {
							setupDeviceChannelObject(deviceEntry.getKey(), 1, objectKey);
							Path sourcePath = objectKey.isEmpty() //
									? this.dataPath.toPath().resolve(this.application.getActiveDevice().getName()) //
									: this.dataPath.toPath().resolve(objectKey);
							for (Entry<Integer, Channel> channelEntry : this.channels.entrySet()) {
								this.channels.setActiveChannelNumber(channelEntry.getKey());

								histoSet.scan4Test(sourcePath, this.deviceConfigurations);
								int tmpSize = -histoSet.size();
								histoSet.rebuild4Test();
								tmpSize += histoSet.size();
								if (tmpSize > 0) {
									System.out.println(String.format("%40.44s channelNumber=%2d  histoSetSize==%,11d", //
											objectKey, this.channels.getActiveChannelNumber(), tmpSize));
								}
							}
						}
					}
				}
				elapsed_sec.add(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - nanoTime));
				System.out.println(String.format(" T O T A L            histoSetSize==%,11d  elapsed=%d", //
						histoSet.size(), elapsed_sec.get(elapsed_sec.size() - 1)));
			} catch (Exception e) {
				e.printStackTrace();
				fail(e.toString());
			}

		}
		for (int j = 0; j < elapsed_sec.size(); j++) {
			System.out.println(String.format("duration%d=%,5d", j, elapsed_sec.get(j)));
		}

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
