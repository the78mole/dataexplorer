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
 * Loads the active flagged device configurations.
 * References the settings and the roaming data access.
 * Tracks the active device with its channels and the active channel.
 * @author Thomas Eickert (USER)
 */
public abstract class Analyzer {
	private static final String	$CLASS_NAME	= Analyzer.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	protected static Analyzer		analyzer;

	public static Analyzer getInstance() {
		if (analyzer == null) {
			if (GDE.EXECUTION_ENV != null) {
				analyzer = null; // todo
			} else if (!GDE.isWithUi()) {
				analyzer = new TestAnalyzer();
			} else {
				analyzer = new Explorer();
			}
			// synchronize now to avoid a performance penalty in case of frequent getInstance calls
			synchronized (analyzer) {
				analyzer.initialize();
			}
		}
		return analyzer;
	}

	protected final Settings				settings;
	protected final DataAccess			dataAccess;
	protected DeviceConfigurations	deviceConfigurations				= null;
	protected Thread								deviceConfigurationsThread	= null;

	protected IDevice								activeDevice								= null;
	protected Channels							channels										= null;

	protected Analyzer() {
		this.settings = Settings.getInstance();
		if (GDE.EXECUTION_ENV != null) {
			this.dataAccess = null; // todo
		} else if (!GDE.isWithUi()) {
			this.dataAccess = DataAccess.getInstance();
		} else {
			this.dataAccess = DataAccess.getInstance();
		}

		this.deviceConfigurationsThread = new Thread("loadDeviceConfigurations") {
			@Override
			public void run() {
				log.log(Level.FINE, "deviceConfigurationsThread    started");
				Settings.getInstance();
				File file = new File(Settings.getDevicesPath());
				if (file.exists()) Analyzer.this.deviceConfigurations = new DeviceConfigurations(file.list(), Settings.getInstance().getActiveDevice());
				log.log(Level.TIME, "deviceConfigurationsThread time =", new SimpleDateFormat("ss:SSS").format(new Date().getTime() - GDE.StartTime));
			}
		};
	}

	protected void initialize() {
		this.deviceConfigurationsThread.start();
	}

	/**
	 * Hybrid singleton copy constructor.
	 * Caution: NO deep copy for device and deviceConfigurations.
	 */
	protected Analyzer(Analyzer that) {
		this.settings = new Settings(that.settings);
		this.channels = new Channels(that.channels);

		this.activeDevice = that.activeDevice;
		this.dataAccess = that.dataAccess.clone();
		if (this.deviceConfigurations == null) {
			throw new UnsupportedOperationException("clone is requested befor the configurations are loaded");
		} else {
			this.deviceConfigurations = that.deviceConfigurations;
		}
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
				File file = new File(Settings.getDevicesPath());
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
	 * Do not clean or rebuild the channels.
	 */
	public void setActiveDevice(IDevice device) {
		this.activeDevice = device;
	}

	/**
	 * @return the settings clone
	 */
	public Settings getSettings() {
		return this.settings;
	}

	/**
	 * Support multiple threads with different analyzer instances.
	 * Use this if analyzer updates are not required or apply to the current thread only.
	 * Be aware of the cloning performance impact.
	 * @return the shallow / deep clone instance ({@link gde.Analyzer#Analyzer(Analyzer)})
	 */
	@Override
	public abstract Analyzer clone();

	public void joinDeviceConfigurationsThread() {
		try {
			while (this.deviceConfigurationsThread == null) {
				Thread.sleep(22);
			}
			this.deviceConfigurationsThread.join();
		} catch (InterruptedException e) {
			log.log(Level.SEVERE, "deviceConfigurationsThread.run()", e);
		}
	}

	@Override
	public String toString() {
		String deviceName = this.activeDevice != null ? this.activeDevice.getName() : "null";
		int channelsSize = this.channels != null ? this.channels.size() : 0;
		int channelNumber = this.getActiveChannel() != null ? this.getActiveChannel().getNumber() : -1;
		return "Analyzer [activeDevice=" + deviceName + ", deviceConfigurationsSize=" + this.deviceConfigurations.getAllConfigurations().size() //
				+ ", channelNumber=" + channelNumber + ", objectKey=" + this.getSettings().getActiveObjectKey() + ", channelsSize=" + channelsSize + "]";
	}
}
