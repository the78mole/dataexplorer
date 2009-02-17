/**
 * 
 */
package osde.junit;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;
import osde.OSDE;
import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.log.LogFormatter;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;

/**
 * @author brueg
 *
 */
public class TestSuperClass extends TestCase {
	Logger																rootLogger;

	OpenSerialDataExplorer								application	= OpenSerialDataExplorer.getInstance();
	Channels															channels		= Channels.getInstance();
	Settings															settings		= Settings.getInstance();
	TreeMap<String, DeviceConfiguration>	deviceConfigurations;
	Vector<String>												activeDevices;
	File																	devicePath;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		Handler ch = new ConsoleHandler();
		LogFormatter lf = new LogFormatter();
		this.rootLogger = Logger.getLogger("");

		// clean up all handlers from outside
		Handler[] handlers = this.rootLogger.getHandlers();
		for (int index = 0; index < handlers.length; index++) {
			this.rootLogger.removeHandler(handlers[index]);
		}
		this.rootLogger.setLevel(Level.WARNING);
		this.rootLogger.addHandler(ch);
		ch.setFormatter(lf);
		ch.setLevel(Level.WARNING);

		Thread.currentThread().setContextClassLoader(OSDE.getClassLoader());

		this.initialize();

		this.devicePath = new File(this.settings.getDataFilePath());
	}

	/**
	 * goes through the existing device properties files and set active flagged devices into active devices list
	 * @throws FileNotFoundException 
	 */
	public void initialize() throws FileNotFoundException {

		File file = new File(this.settings.getDevicesPath());
		if (!file.exists()) throw new FileNotFoundException(this.settings.getDevicesPath());
		String[] files = file.list();
		DeviceConfiguration devConfig;
		this.deviceConfigurations = new TreeMap<String, DeviceConfiguration>(String.CASE_INSENSITIVE_ORDER);
		this.activeDevices = new Vector<String>(2, 1);

		for (int i = 0; files != null && i < files.length; i++) {
			try {
				// loop through all device properties XML and check if device used
				if (files[i].endsWith(".xml")) {
					String deviceKey = files[i].substring(0, files[i].length() - 4);
					devConfig = new DeviceConfiguration(this.settings.getDevicesPath() + OSDE.FILE_SEPARATOR_UNIX + files[i]);

					// store all device configurations in a map					
					String keyString;
					if (devConfig.getName() != null)
						keyString = devConfig.getName();
					else {
						devConfig.setName(deviceKey);
						keyString = deviceKey;
					}
					//log.log(Level.FINE, deviceKey + OSDE.STRING_MESSAGE_CONCAT + keyString);
					this.deviceConfigurations.put(keyString, devConfig);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				fail(e.toString());
			}
		}
	}

	/**
	 * calculates the new class name for the device
	 */
	@SuppressWarnings("unchecked")//$NON-NLS-1$
	protected IDevice getInstanceOfDevice(DeviceConfiguration selectedActiveDeviceConfig) {
		IDevice newInst = null;
		String selectedDeviceName = selectedActiveDeviceConfig.getName().replace(OSDE.STRING_BLANK, OSDE.STRING_EMPTY).replace(OSDE.STRING_DASH, OSDE.STRING_EMPTY);
		//selectedDeviceName = selectedDeviceName.substring(0, 1).toUpperCase() + selectedDeviceName.substring(1);
		String className = "osde.device." + selectedActiveDeviceConfig.getManufacturer().toLowerCase().replace(OSDE.STRING_BLANK, OSDE.STRING_EMPTY).replace(OSDE.STRING_DASH, OSDE.STRING_EMPTY) + "." + selectedDeviceName; //$NON-NLS-1$
		try {
			//String className = "osde.device.DefaultDeviceDialog";
			//log.log(Level.FINE, "loading Class " + className); //$NON-NLS-1$
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Class c = loader.loadClass(className);
			//Class c = Class.forName(className);
			Constructor constructor = c.getDeclaredConstructor(new Class[] { String.class });
			//log.log(Level.FINE, "constructor != null -> " + (constructor != null ? "true" : "false")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (constructor != null) {
				newInst = (IDevice) constructor.newInstance(new Object[] { selectedActiveDeviceConfig.getPropertiesFileName() });
			}
			else
				throw new NoClassDefFoundError(Messages.getString(MessageIds.OSDE_MSGE0016));

		}
		catch (NoClassDefFoundError e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return newInst;
	}

	/**
	 * this will setup empty channels for the device
	 * @param activeDevice (IDevice is the abstract type)
	 */
	protected void setupDataChannels(IDevice activeDevice) {
		// cleanup existing channels and record sets
		this.channels.cleanup();

		if (activeDevice != null) {
			String[] channelNames = new String[activeDevice.getChannelCount()];
			// buildup new structure  - set up the channels
			for (int i = 1; i <= activeDevice.getChannelCount(); i++) {
				Channel newChannel = new Channel(i, activeDevice.getChannelName(i), activeDevice.getChannelType(i));
				this.channels.put(new Integer(i), newChannel);
				channelNames[i - 1] = i + " : " + activeDevice.getChannelName(i);
			}
			this.channels.setChannelNames(channelNames);
		}
	}

}
