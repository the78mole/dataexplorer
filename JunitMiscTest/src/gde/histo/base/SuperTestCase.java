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

    Copyright (c) 2018 Thomas Eickert
 ****************************************************************************************/

package gde.histo.base;

import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import gde.GDE;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.messages.MessageIds;
import gde.messages.Messages;

/**
 * Provide a Data Explorer instance with settings and all device configurations.
 * Support switching devices, channels, objects and settings including data path.
 * @author Thomas Eickert (USER)
 */
public class SuperTestCase extends HistoTestCase {

	public static <K, V> Map.Entry<K, V> entry(K key, V value) {
		return new AbstractMap.SimpleEntry<>(key, value);
	}

	protected HashMap<String, String>	legacyDeviceNames	= new HashMap<String, String>(2);

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.initialize();

		// add this two renamed device plug-ins to the list of legacy devices
		this.legacyDeviceNames.put("GPSLogger", "GPS-Logger");
		this.legacyDeviceNames.put("QuadroControl", "QC-Copter");
		this.legacyDeviceNames.put("PichlerP60", "PichlerP60 50W");
}

	/**
	 * Goes through the existing device properties files and set active flagged devices into active devices list.
	 */
	public void initialize() throws FileNotFoundException {
		String deviceoriented = Messages.getString(MessageIds.GDE_MSGT0200).split(GDE.STRING_SEMICOLON)[0];
		this.settings.setObjectList(settings.getObjectList(), Arrays.asList(settings.getObjectList()).indexOf(deviceoriented));

		this.settings.setPartialDataTable(false);
		this.settings.setTimeFormat("relativ");
	}

	/**
	 * calculates the new class name for the device
	 */
	protected IDevice getInstanceOfDevice(DeviceConfiguration selectedActiveDeviceConfig) {
		IDevice newInst = null;
		String selectedDeviceName = selectedActiveDeviceConfig.getDeviceImplName().replace(GDE.STRING_BLANK, GDE.STRING_EMPTY).replace(GDE.STRING_DASH, GDE.STRING_EMPTY);
		// selectedDeviceName = selectedDeviceName.substring(0, 1).toUpperCase() + selectedDeviceName.substring(1);
		String className = selectedDeviceName.contains(GDE.STRING_DOT) ? selectedDeviceName // full qualified
				: "gde.device." + selectedActiveDeviceConfig.getManufacturer().toLowerCase().replace(GDE.STRING_BLANK, GDE.STRING_EMPTY).replace(GDE.STRING_DASH, GDE.STRING_EMPTY) + "." + selectedDeviceName;
		try {
			// String className = "gde.device.DefaultDeviceDialog";
			// log.log(Level.FINE, "loading Class " + className); //$NON-NLS-1$
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Class<?> c = loader.loadClass(className);
			// Class c = Class.forName(className);
			Constructor<?> constructor = c.getDeclaredConstructor(new Class[] { String.class });
			// log.log(Level.FINE, "constructor != null -> " + (constructor != null ? "true" : "false"));
			if (constructor != null) {
				newInst = (IDevice) constructor.newInstance(new Object[] { selectedActiveDeviceConfig.getPropertiesFileName() });
			} else
				throw new NoClassDefFoundError(Messages.getString(MessageIds.GDE_MSGE0016));

		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return newInst;
	}

	/**
	 * Emulate user GUI settings required for history tabs.
	 * @param fileDeviceName
	 * @param activeChannelNumber
	 * @param activeObjectKey is an object key which exists in the settings or an empty string for deviceoriented
	 */
	protected void setDeviceChannelObject(String fileDeviceName, int activeChannelNumber, String activeObjectKey) {
		// device : from setDevice
		if (this.legacyDeviceNames.get(fileDeviceName) != null) fileDeviceName = this.legacyDeviceNames.get(fileDeviceName);
		if (fileDeviceName.toLowerCase().contains("charger308duo") || fileDeviceName.toLowerCase().contains("charger308duo")) {
			System.out.println("skip fileDeviceName=" + fileDeviceName);
		}
		DeviceConfiguration deviceConfig = this.application.getDeviceConfigurations().get(fileDeviceName);
		if (deviceConfig == null) new UnsupportedOperationException("deviceConfig == null");
		IDevice device = this.getInstanceOfDevice(deviceConfig);

		this.application.setEnvironmentWoutUI(settings, device, activeChannelNumber);

		// object : in Settings only - not in channel because not used in histo
		this.settings.setActiveObjectKey(activeObjectKey);
	}

	/**
	 * Emulate user GUI settings required for history tabs.
	 * @param device
	 * @param activeChannelNumber
	 * @param activeObjectKey is an object key which exists in the settings or an empty string for deviceoriented
	 */
	protected void setDeviceChannelObject(IDevice device, int activeChannelNumber, String activeObjectKey) {
		this.application.setEnvironmentWoutUI(settings, device, activeChannelNumber);

		// object : in Settings only - not in channel because not used in histo
		this.settings.setActiveObjectKey(activeObjectKey);
	}
}
