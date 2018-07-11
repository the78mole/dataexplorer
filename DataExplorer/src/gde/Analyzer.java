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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.sun.istack.internal.Nullable;

import gde.config.DeviceConfigurations;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.device.IDevice;
import gde.log.Level;
import gde.log.Logger;

/**
 * Kernel for analyzing logging data.
 * Loads all device configurations.
 * References the roaming data access.
 * Tracks the active device, its channels and the active channel.
 * @author Thomas Eickert (USER)
 */
public abstract class Analyzer {
	private static final String	$CLASS_NAME	= Analyzer.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	protected static Analyzer		analyzer;

	public static Analyzer getInstance() {
		if (analyzer == null) {
			if (GDE.display != null) {
				analyzer = new Explorer();
			} else if (GDE.EXECUTION_ENV == null) {
				analyzer = new TestAnalyzer();
			} else {
				// todo
			}
		}
		return analyzer;
	}

	protected final DataAccess			dataAccess;
	protected DeviceConfigurations	deviceConfigurations				= null;
	protected Thread								deviceConfigurationsThread	= null;

	protected IDevice								activeDevice								= null;
	protected Channels							channels										= null;

	protected Analyzer() {
		if (GDE.display != null) {
			this.dataAccess = DataAccess.getInstance();
		} else if (GDE.EXECUTION_ENV == null) {
			this.dataAccess = DataAccess.getInstance();
		} else {
			this.dataAccess = null; // todo
		}

		this.deviceConfigurationsThread = new Thread("loadDeviceConfigurations") {
			@Override
			public void run() {
				log.log(Level.OFF, "deviceConfigurationsThread    started");
				File file = new File(Settings.getInstance().getDevicesPath());
				if (file.exists()) Analyzer.this.deviceConfigurations = new DeviceConfigurations(file.list(), Settings.getInstance().getActiveDevice());
				log.log(Level.TIME, "deviceConfigurationsThread time =", new SimpleDateFormat("ss:SSS").format(new Date().getTime() - GDE.StartTime));
			}
		};
		this.deviceConfigurationsThread.start();
	}

	/**
	 * @return the roaming data sources support
	 */
	public DataAccess getDataAccess() {
		return this.dataAccess;
	}

	public DeviceConfigurations getDeviceConfigurations() {
		if (this.deviceConfigurations == null) {
			if (this.isDeviceConfigurationsThreadAlive()) {
				try {
					this.deviceConfigurationsThread.join();
				} catch (InterruptedException e) {
				}
			} else {
				File file = new File(Settings.getInstance().getDevicesPath());
				this.deviceConfigurations = new DeviceConfigurations(file.list());
			}
		}
		return this.deviceConfigurations;
	}

	/**
	 * @return false if the thread is null or finished
	 */
	private boolean isDeviceConfigurationsThreadAlive() {
		return this.deviceConfigurationsThread != null ? this.deviceConfigurationsThread.isAlive() : false;
	}

	@Nullable
	public Channels getChannels() {
		return this.channels;
	}

	@Nullable
	public Channel getActiveChannel() {
		return this.channels != null ? this.channels.getActiveChannel() : null;
	}

	@Nullable
	public IDevice getActiveDevice() {
		return this.activeDevice;
	}

	/**
	 * set the active device in main settings
	 * @param device
	 */
	public void setActiveDevice(IDevice device) {
		this.activeDevice = device;
	}

	/**
	 * @return true if the GUI was not initialized
	 */
	public boolean isWithUi() {
		return this instanceof Explorer;
	}

}
