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

    Copyright (c) 2017,2018 Thomas Eickert
****************************************************************************************/

package gde.config;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.xml.sax.SAXParseException;

import gde.DataAccess;
import gde.GDE;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.StringHelper;

/**
 * Provide device lists and the selected device.
 * Holds only devices with the usage flag in the device configuration xml file.
 * Not threadsafe due to device shallow copies and JAXB un-/marshallers.
 * @author Thomas Eickert (USER)
 */
public final class DeviceConfigurations {
	private static final String													$CLASS_NAME									= DeviceConfigurations.class.getName();
	private static final Logger													log													= Logger.getLogger($CLASS_NAME);

	private final TreeMap<String, DeviceConfiguration>	configs;
	private final Vector<String>												activeDevices;

	private DeviceConfiguration													selectedActiveDeviceConfig	= null;

	public DeviceConfigurations() {
		this.configs = new TreeMap<String, DeviceConfiguration>(String.CASE_INSENSITIVE_ORDER);
		this.activeDevices = new Vector<String>(11, 11);
	}

	/**
	 * @return the device names of devices with the usage flag in the device configuration xml file
	 */
	public Set<String> deviceNames() {
		return this.configs.keySet();
	}

	/**
	 * Goes through the existing XML files and set active flagged devices into active devices list.
	 * Fills the DeviceConfigurations list.
	 */
	public void initialize(Settings settings, DataAccess dataAccess) {
		String activeDeviceName = settings.getActiveDevice();
		Objects.requireNonNull(activeDeviceName);

		DeviceConfiguration devConfig;
		for (String file : dataAccess.getDeviceFolderList()) {
			try {
				// loop through all device properties XML and check if device used
				if (file.endsWith(GDE.FILE_ENDING_DOT_XML)) {
					String deviceKey = file.substring(0, file.length() - 4);
					devConfig = new DeviceConfiguration(Paths.get(Settings.DEVICE_PROPERTIES_DIR_NAME, file), settings);
					if (devConfig.getName().equals(activeDeviceName) && devConfig.isUsed()) { // define the active device after re-start
						selectedActiveDeviceConfig = devConfig;
					}
					// add the active once into the active device vector
					if (devConfig.isUsed() && !this.activeDevices.contains(devConfig.getName())) this.activeDevices.add(devConfig.getName());

					// store all device configurations in a map
					String keyString;
					if (devConfig.getName() != null)
						keyString = devConfig.getName();
					else {
						devConfig.setName(deviceKey);
						keyString = deviceKey;
					}
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, deviceKey + GDE.STRING_MESSAGE_CONCAT + keyString);
					this.configs.put(keyString, devConfig);
				}
			} catch (JAXBException e) {
				log.log(Level.WARNING, file, e);
				if (e.getLinkedException() instanceof SAXParseException) {
					SAXParseException spe = (SAXParseException) e.getLinkedException();
					GDE.setInitError(Messages.getString(MessageIds.GDE_MSGW0038, new String[] {
							spe.getSystemId().replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK), spe.getLocalizedMessage() }));
				}
			} catch (Exception e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}
		log.log(Level.TIME, "device init time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime)));
	}

	public TreeMap<String, DeviceConfiguration> getAllConfigurations() { // todo replace with getters / putters etc
		return this.configs;
	}

	public boolean contains(String deviceName) {
		return this.configs.containsKey(deviceName);
	}

	public DeviceConfiguration get(String deviceName) {
		return this.configs.get(deviceName);
	}

	public DeviceConfiguration put(String deviceName, DeviceConfiguration tmpDeviceConfiguration) {
		return this.configs.put(deviceName, tmpDeviceConfiguration);
	}

	public DeviceConfiguration remove(String deviceName) {
		return this.configs.remove(deviceName);
	}

	/**
	 * @return the names of the devices with the usage property
	 */
	public Vector<String> getActiveDevices() {
		return this.activeDevices;
	}

	/**
	 * @return null or a device selected by name
	 */
	public DeviceConfiguration getSelectedActiveDeviceConfig() {
		return this.selectedActiveDeviceConfig;
	}

	/**
	 * @return the supported lowercase file extensions (e.g. '.bin') or an empty set
	 */
	public Set<String> getValidLogExtentions() {
		Set<String> result = getImportExtentions();
		result.add(GDE.FILE_ENDING_DOT_OSD);
		return result;
	}

	/**
	 * @return the supported lowercase file extensions (e.g. '.bin') or an empty set
	 */
	public Set<String> getImportExtentions() {
		Set<String> extentions = this.configs.values().parallelStream() //
				.map(c -> Arrays.asList(c.getDataBlockPreferredFileExtention().split(GDE.REGEX_FILE_EXTENTION_SEPARATION))).flatMap(Collection::stream) //
				.map(s -> s.substring(s.lastIndexOf(GDE.STRING_DOT))).map(e -> e.toLowerCase()) //
				.collect(Collectors.toSet());
		return extentions;
	}

	/**
	 * @return a map with key device name and device
	 */
	public HashMap<String, IDevice> getAsDevices(Collection<String> deviceNames) {
		HashMap<String, IDevice> existingDevices = new HashMap<>();
		for (String deviceName : deviceNames) {
			existingDevices.put(deviceName, configs.get(deviceName).getAsDevice());
		}
		log.log(Level.FINE, "Selected      size=", existingDevices.size());
		return existingDevices;
	}

	/**
	 * @param deviceName is the name entry in the device configuration xml file
	 * @return the cashed device configuration
	 */
	public DeviceConfiguration getConfiguration(String deviceName) {
		return this.configs.get(deviceName);
	}

}
