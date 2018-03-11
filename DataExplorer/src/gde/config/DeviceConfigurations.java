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

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.xml.sax.SAXParseException;

import gde.GDE;
import gde.device.DeviceConfiguration;
import gde.histo.recordings.TrailRecordSet;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.StringHelper;

/**
 * Provide device lists and the selected device.
 * Moved from DeviceSelectionDialog with minimum changes.
 * @author Thomas Eickert (USER)
 */
public final class DeviceConfigurations {
	private static final String													$CLASS_NAME									= TrailRecordSet.class.getName();
	private static final Logger													log													= Logger.getLogger($CLASS_NAME);

	private final Settings															settings										= Settings.getInstance();

	private final TreeMap<String, DeviceConfiguration>	configs;
	private final Vector<String>												activeDevices;

	private DeviceConfiguration													selectedActiveDeviceConfig	= null;

	public DeviceConfigurations(String[] files) {
		this(files, "");
	}

	public DeviceConfigurations(String[] files, String activeDeviceName) {
		Objects.requireNonNull(activeDeviceName);
		Objects.requireNonNull(files);

		// wait until schema is setup
		while (this.settings.isXsdThreadAlive()) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				// ignore
			}
		}

		this.configs = new TreeMap<String, DeviceConfiguration>(String.CASE_INSENSITIVE_ORDER);
		this.activeDevices = new Vector<String>(2, 1);
		initialize(files, activeDeviceName);
	}

	public Set<String> keySet() {
		return this.configs.keySet();
	}

	/**
	 * Goes through the existing INI files and set active flagged devices into active devices list.
	 * Fills the DeviceConfigurations list.
	 */
	public void initialize(String[] files, String activeDeviceName) {
		DeviceConfiguration devConfig;

		for (int i = 0; files != null && i < files.length; i++) {
			try {
				// loop through all device properties XML and check if device used
				if (files[i].endsWith(GDE.FILE_ENDING_DOT_XML)) {
					String deviceKey = files[i].substring(0, files[i].length() - 4);
					devConfig = new DeviceConfiguration(this.settings.getDevicesPath() + GDE.FILE_SEPARATOR_UNIX + files[i]);
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
				log.log(Level.WARNING, files[i], e);
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
	public Set<String>  getValidLogExtentions() {
		Set<String> result = getImportExtentions();
		result.add(GDE.FILE_ENDING_DOT_OSD);
		return result;
	}

	/**
	 * @return the supported lowercase file extensions (e.g. '.bin') or an empty set
	 */
	public Set<String>  getImportExtentions() {
		Set<String> extentions = this.configs.values().parallelStream() //
				.map(c -> Arrays.asList(c.getDataBlockPreferredFileExtention().split(GDE.STRING_CSV_SEPARATOR))).flatMap(Collection::stream) //
				.map(s-> s.substring(s.lastIndexOf(GDE.STRING_DOT))).map(e-> e.toLowerCase()) //
				.collect(Collectors.toSet());
		return extentions;
	}
}
