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

    Copyright (c) 2018 Thomas Eickert
****************************************************************************************/

package gde;

import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.device.IDevice;
import gde.log.Logger;

/**
 * Kernel for analyzing logging data.
 * Use this for unit test without integrated UI.
 * @author Thomas Eickert (USER)
 */
public class TestAnalyzer extends Analyzer {
	private static final String	$CLASS_NAME	= TestAnalyzer.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	TestAnalyzer() {
		super();
	}

	private TestAnalyzer(TestAnalyzer analyzer) {
		super(analyzer);
	}

	public void initiateUnitTestEnvironment(IDevice device, Channels channels, String objectKey) {
		// device :
		this.activeDevice = device;

		// channel : from setupDataChannels
		this.channels = Channels.getInstance();
		this.channels.cleanup();
		String[] channelNames = new String[device.getChannelCount()];
		// buildup new structure - set up the channels
		for (int i = 1; i <= device.getChannelCount(); i++) {
			Channel newChannel = new Channel(device.getChannelNameReplacement(i), device.getChannelTypes(i));
			// newChannel.setObjectKey(this.application.getObjectKey()); now in application.selectObjectKey
			this.channels.put(Integer.valueOf(i), newChannel);
			channelNames[i - 1] = i + " : " + device.getChannelNameReplacement(i);
		}
		this.channels.setChannelNames(channelNames);

		if (!Settings.getInstance().getActiveObjectKey().equals(objectKey)) {
			String[] objectKeys = Settings.getInstance().getObjectList();
			for (String objectKey2 : objectKeys) {
				if (objectKey.equals(objectKey2)) {
					this.channels.getActiveChannel().setObjectKey(objectKey);
				}
			}
		}
	}

	public void setEnvironmentWoutUI(Settings newSettings, IDevice device, int channelNumber) { // todo new settings was set into a field variable
		if (!device.equals(this.activeDevice)) {
			// device :
			this.activeDevice = device;

			// channels : from setupDataChannels
			// objectKey : is not set because histo does not use it
			this.channels = Channels.getInstance();
			this.channels.cleanup();
			String[] channelNames = new String[device.getChannelCount()];
			// buildup new structure - set up the channels
			for (int i = 1; i <= device.getChannelCount(); i++) {
				Channel newChannel = new Channel(device.getChannelNameReplacement(i), device.getChannelTypes(i));
				// newChannel.setObjectKey(objectKey);
				this.channels.put(Integer.valueOf(i), newChannel);
				channelNames[i - 1] = i + " : " + device.getChannelNameReplacement(i);
			}
			this.channels.setChannelNames(channelNames);
		}

		// channel :
		this.channels.setActiveChannelNumber(channelNumber);
	}

	@Override
	public void setActiveDevice(IDevice device) {
		if (device != null) {
			this.activeDevice = device;
		}
	}

	@Override
	public TestAnalyzer clone() {
		return new TestAnalyzer(this);
	}

}
