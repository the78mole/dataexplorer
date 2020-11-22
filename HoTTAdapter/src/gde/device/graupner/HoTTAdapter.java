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
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2011,2012,2013,2014,2015,2016,2017,2018,2019,2020 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import com.sun.istack.Nullable;

import gde.Analyzer;
import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.ChannelPropertyTypes;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.MeasurementType;
import gde.device.graupner.HoTTbinReader.BinParser;
import gde.device.graupner.hott.MessageIds;
import gde.device.resource.DeviceXmlResource;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.histo.cache.VaultCollector;
import gde.histo.device.IHistoDevice;
import gde.histo.device.UniversalSampler;
import gde.histo.utils.PathUtils;
import gde.io.DataParser;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.CalculationThread;
import gde.utils.FileUtils;
import gde.utils.GPSHelper;
import gde.utils.LinearRegression;
import gde.utils.ObjectKeyCompliance;
import gde.utils.WaitTimer;

/**
 * Graupner HoTT device base class
 * @author Winfried Brügmann
 */
public class HoTTAdapter extends DeviceConfiguration implements IDevice, IHistoDevice {
	final static Logger		log																= Logger.getLogger(HoTTAdapter.class.getName());

	final static String		LOG_COUNT													= "LogCount";																																						//$NON-NLS-1$
	final static String		FILE_PATH													= "FilePath";																																						//$NON-NLS-1$
	final static String		SD_FORMAT													= "SD_FORMAT";																																					//$NON-NLS-1$
	final static String		DETECTED_SENSOR										= "DETECTED SENSOR";																																		//$NON-NLS-1$

	// HoTT sensor bytes 19200 Baud protocol
	static boolean				IS_SLAVE_MODE											= false;
	final static byte			SENSOR_TYPE_RECEIVER_19200				= (byte) (0x80 & 0xFF);
	final static byte			SENSOR_TYPE_VARIO_19200						= (byte) (0x89 & 0xFF);
	final static byte			SENSOR_TYPE_GPS_19200							= (byte) (0x8A & 0xFF);
	final static byte			SENSOR_TYPE_GENERAL_19200					= (byte) (0x8D & 0xFF);
	final static byte			SENSOR_TYPE_ELECTRIC_19200				= (byte) (0x8E & 0xFF);
	final static byte			SENSOR_TYPE_SPEED_CONTROL_19200		= (byte) (0x8C & 0xFF);
	final static byte			ANSWER_SENSOR_VARIO_19200					= (byte) (0x90 & 0xFF);
	final static byte			ANSWER_SENSOR_GPS_19200						= (byte) (0xA0 & 0xFF);
	final static byte			ANSWER_SENSOR_GENERAL_19200				= (byte) (0xD0 & 0xFF);
	final static byte			ANSWER_SENSOR_ELECTRIC_19200			= (byte) (0xE0 & 0xFF);
	final static byte			ANSWER_SENSOR_MOTOR_DRIVER_19200	= (byte) (0xC0 & 0xFF);

	// HoTT sensor bytes 115200 Baud protocol (actual no slave mode)
	// there is no real slave mode for this protocol
	final static byte			SENSOR_TYPE_RECEIVER_115200				= 0x34;
	final static byte			SENSOR_TYPE_VARIO_115200					= 0x37;
	final static byte			SENSOR_TYPE_GPS_115200						= 0x38;
	final static byte			SENSOR_TYPE_GENERAL_115200				= 0x35;
	final static byte			SENSOR_TYPE_ELECTRIC_115200				= 0x36;
	final static byte			SENSOR_TYPE_SPEED_CONTROL_115200	= 0x39;
	final static byte			SENSOR_TYPE_SERVO_POSITION_115200	= 0x40;
	final static byte			SENSOR_TYPE_PURPIL_POSITION_115200= 0x41;
	final static byte			SENSOR_TYPE_CONTROL_1_115200			= 0x42;
	final static byte			SENSOR_TYPE_CONTROL_2_115200			= 0x43;

	final static boolean	isSwitchS[]												= { false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false };
	final static boolean	isSwitchG[]												= { false, false, false, false, false, false, false, false };
	final static boolean	isSwitchL[]												= { false, false, false, false, false, false, false, false };

	final static int			QUERY_GAP_MS											= 30;

	public enum Sensor {
		RECEIVER(1, "Receiver", "RECEIVER") { //$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.RcvBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.RcvBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.RcvBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}
		},
		VARIO(2, "Vario", "VARIO") { //$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.VarBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.VarBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.VarBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}
		},
		GPS(3, "GPS", "GPS") { //$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.GpsBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.GpsBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.GpsBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}
		},
		GAM(4, "GAM", "GENERAL") { //$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.GamBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.GamBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.GamBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}
		},
		EAM(5, "EAM", "ELECTRIC") {//$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.EamBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.EamBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.EamBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}
		},
		ESC(7, "ESC", "?") { //$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.EscBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.EscBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.EscBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}
		},
		CHANNEL(6, "Channel", "N/A") { //$NON-NLS-1$
			@Override
			public BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader.ChnBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.ChnBinParser(pickerParameters, timeSteps_ms, buffers);
			}

			@Override
			public BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers) {
				return new HoTTbinReader2.ChnBinParser(pickerParameters, points, timeSteps_ms, buffers);
			}
		};

		private final String				value;
		private final String				detectedName;
		private final int						channelNumber;
		public static final Sensor	VALUES[]	= values();	// use this to avoid cloning if calling values()

		private Sensor(int channelNumber, String value, String detectedName) {
			this.value = value;
			this.detectedName = detectedName;
			this.channelNumber = channelNumber;
		}

		public String value() {
			return this.value;
		}

		/**
		 * Takes the parsing input objects in order to avoid parsing method parameters for better performance.
		 * @param points is the output object
		 * @param timeSteps_ms is the wrapper object holding the current timestep
		 * @param buffers are the required input buffers for parsing (the first dimension corresponds to the buffers count)
		 */
		public abstract BinParser createBinParser(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers);

		/**
		 * Parse for HoTTAdapter2.
		 * Takes the parsing input objects in order to avoid parsing method parameters for better performance.
		 * @param timeSteps_ms is the wrapper object holding the current timestep
		 * @param buffers are the required input buffers for parsing (the first dimension corresponds to the buffers count)
		 */
		public abstract BinParser createBinParser2(PickerParameters pickerParameters, long[] timeSteps_ms, byte[][] buffers);

		/**
		 * Parse in situ for HoTTAdapter2 without the need to migrate the points.
		 * Takes the parsing input objects as well as the parsing output object.
		 * @param points is the output object which might be on single commonly used object for all parser subclasses
		 * @param timeSteps_ms is the wrapper object holding the current timestep
		 * @param buffers are the required input buffers for parsing (the first dimension corresponds to the buffers count)
		 */
		public abstract BinParser createBinParser2(PickerParameters pickerParameters, int[] points, long[] timeSteps_ms, byte[][] buffers);

		public static Sensor fromOrdinal(int ordinal) {
			return Sensor.VALUES[ordinal];
		}

		@Nullable
		public static Sensor fromSensorByte(byte sensorByte) {
			Sensor sensor = null;
			switch (sensorByte) {
			case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
				sensor = Sensor.VARIO;
				break;
			case HoTTAdapter.SENSOR_TYPE_GPS_19200:
				sensor = Sensor.GPS;
				break;
			case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
				sensor = Sensor.GAM;
				break;
			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
				sensor = Sensor.EAM;
				break;
			case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
				sensor = Sensor.ESC;
				break;
			default:
				//
				break;
			}
			return sensor;
		}

		@Nullable
		public static Sensor fromChannelNumber(int channelNumber) {
			for (Sensor sensor : Sensor.VALUES) {
				if (channelNumber == sensor.channelNumber) {
					return sensor;
				}
			}
			return null;
		}

		/**
		 * @param value is the name in the sensor signature
		 * @return the sensor found (ignoring casing)
		 */
		@Nullable
		public static Sensor fromValue(String value) {
			for (Sensor sensor : Sensor.VALUES) {
				if (value.equalsIgnoreCase(sensor.value)) {
					return sensor;
				}
			}
			return null;
		}

		/**
		 * @param detectedName is the name in the same pattern as the DETECTED SENSOR entry in a *.log file (e.g. RECEIVER,VARIO,GPS,GENERAL,)
		 * @return the sensor found (ignoring casing)
		 */
		@Nullable
		public static Sensor fromDetectedName(String detectedName) {
			for (Sensor sensor : Sensor.VALUES) {
				if (detectedName.equalsIgnoreCase(sensor.detectedName)) {
					return sensor;
				}
			}
			return null;
		}

		public static EnumSet<Sensor> getSetFromSignature(String sensorSignature) {
			EnumSet<Sensor> sensors = sensorSignature.isEmpty() ? EnumSet.noneOf(Sensor.class)
					: Arrays.stream(sensorSignature.split(GDE.STRING_COMMA)).map(Sensor::fromValue).filter(Objects::nonNull) //
							.collect(Collectors.toCollection(() -> EnumSet.noneOf(Sensor.class)));
			sensors.add(RECEIVER); // always present
			sensors.remove(CHANNEL); // not a real sensor
			return sensors;
		}

		public static EnumSet<Sensor> getSetFromDetected(String detectedSensors) {
			EnumSet<Sensor> sensors = detectedSensors.isEmpty() ? EnumSet.noneOf(Sensor.class)
					: Arrays.stream(detectedSensors.split(GDE.STRING_COMMA)).map(Sensor::fromDetectedName).filter(Objects::nonNull) //
							.collect(Collectors.toCollection(() -> EnumSet.noneOf(Sensor.class)));
			sensors.add(RECEIVER); // always present
			sensors.remove(CHANNEL); // not a real sensor
			return sensors;
		}

		public static StringBuilder getSetAsSignature(EnumSet<Sensor> sensors) {
			EnumSet<Sensor> tmpSensors = sensors.clone();
			tmpSensors.add(RECEIVER);
			return new StringBuilder(tmpSensors.stream().map(s -> s.value).collect(Collectors.joining(GDE.STRING_COMMA)));
		}

		public static String getSetAsDetected(EnumSet<Sensor> sensors) {
			EnumSet<Sensor> tmpSensors = sensors.clone();
			tmpSensors.add(RECEIVER);
			return tmpSensors.stream().map(s -> s.detectedName).collect(Collectors.joining(GDE.STRING_COMMA));
		}

		/**
		 * @param sensors is a subset of the sensor entries
		 * @return a boolean array with a length of all sensor entries. It holds true values for the sensors in the {@code sensors} set.
		 */
		public static boolean[] getActiveSensors(EnumSet<Sensor> sensors) {
			boolean[] activeSensors = new boolean[VALUES.length];
			for (int i = 0; i < VALUES.length; i++) {
				Sensor sensor = VALUES[i];
				activeSensors[i] = sensors.contains(sensor);
			}
			return activeSensors;
		}

		/**
		 * @param sensors is a subset of the sensor entries
		 * @return a set with bits 0 to 31 representing the sensor type ordinal numbers (true if the sensor type is active)
		 */
		public static BitSet getSensors(EnumSet<Sensor> sensors) {
			BitSet sensorBitSet = new BitSet();
			for (Sensor sensor : sensors) {
				sensorBitSet.set(sensor.ordinal());
			}
			return sensorBitSet;
		}

		/**
		 * @return channel numbers of the sensors including the receiver and 'channels'
		 */
		public static Set<Integer> getChannelNumbers(EnumSet<Sensor> sensors) {
			Set<Integer> channelNumbers = new HashSet<Integer>();
			channelNumbers.add(RECEIVER.channelNumber); // always present
			channelNumbers.add(CHANNEL.channelNumber); // always present
			for (Sensor sensor : sensors) {
				channelNumbers.add(sensor.channelNumber);
			}
			return channelNumbers;
		}

		public int getChannelNumber() {
			return this.channelNumber;
		}

	};

	// protocol definitions
	public enum Protocol {
		TYPE_19200_V3("19200 V3"), TYPE_19200_V4("19200 V4"), TYPE_115200("115200"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		private final String value;

		private Protocol(String v) {
			this.value = v;
		}

		public String value() {
			return this.value;
		}

		public static Protocol fromValue(String v) {
			for (Protocol c : Protocol.values()) {
				if (c.value.equals(v)) {
					return c;
				}
			}
			throw new IllegalArgumentException(v);
		}

		public static String[] valuesAsStingArray() {
			StringBuilder sb = new StringBuilder();
			for (Protocol protocol : Protocol.values()) {
				sb.append(protocol.value).append(GDE.STRING_SEMICOLON);
			}
			return sb.toString().split(GDE.STRING_SEMICOLON);
		}
	}

	/**
	 * Parameter object replacing some HoTTAdapter* fields used by other classes.
	 * Supports threadsafe access to these parameters.
	 * todo check if this class is required after making the readers threadsafe
	 * @author Thomas Eickert (USER)
	 */
	public static final class PickerParameters {
		final Analyzer					analyzer;

		final PackageLossDeque	reverseChannelPackageLossCounter;

		boolean									isChannelsChannelEnabled			= false;
		boolean									isFilterEnabled								= true;
		boolean									isFilterTextModus							= true;
		boolean									isChannelPercentEnabled				= true;
		int											altitudeClimbSensorSelection						= 0;
		
		boolean									isTolerateSignChangeLatitude	= false;
		boolean									isTolerateSignChangeLongitude	= false;
		double									latitudeToleranceFactor				= 90.0;
		double									longitudeToleranceFactor			= 25.0;

		public PickerParameters(Analyzer analyzer) {
			this.analyzer = analyzer;
			this.reverseChannelPackageLossCounter = new PackageLossDeque(100);
		}

		/**
		 * Shallow copy constructor.
		 * New package loss counter!
		 */
		PickerParameters(PickerParameters that) {
			this.analyzer = that.analyzer;
			this.reverseChannelPackageLossCounter = new PackageLossDeque(100);

			this.isChannelsChannelEnabled = that.isChannelsChannelEnabled;
			this.isFilterEnabled = that.isFilterEnabled;
			this.isFilterTextModus = that.isFilterTextModus;
			this.isChannelPercentEnabled = that.isChannelPercentEnabled;
			this.altitudeClimbSensorSelection = that.altitudeClimbSensorSelection;
			
			this.isTolerateSignChangeLatitude = that.isTolerateSignChangeLatitude;
			this.isTolerateSignChangeLongitude = that.isTolerateSignChangeLongitude;
			this.latitudeToleranceFactor = that.latitudeToleranceFactor;
			this.longitudeToleranceFactor = that.longitudeToleranceFactor;
		}

		/**
		 * Collect the settings relevant for the values inserted in the histo vault.
		 * @return the settings which determine the measurement values returned by the reader
		 */
		public String getReaderSettingsCsv() {
			final String d = GDE.STRING_CSV_SEPARATOR;
			return isFilterEnabled + d + altitudeClimbSensorSelection;
			//return isFilterEnabled + d + isTolerateSignChangeLatitude + d + isTolerateSignChangeLongitude + d + latitudeToleranceFactor + d + longitudeToleranceFactor;
		}

		@Override
		public String toString() {
			return "PickerParameters [analyzer.channels=" + this.analyzer.getChannels() + ", isChannelsChannelEnabled=" + this.isChannelsChannelEnabled + ", isFilterEnabled=" + this.isFilterEnabled + ", isFilterTextModus=" + this.isFilterTextModus + ", altitudeClimbSensorSelection=" + this.altitudeClimbSensorSelection + "]";
		}


	}

	final DataExplorer								application;
	final Channels										channels;
	final Settings										settings;
	final HoTTAdapterDialog						dialog;
	final HoTTAdapterSerialPort				serialPort;

	protected final PickerParameters	pickerParameters	= new PickerParameters(Analyzer.getInstance());

	protected List<String>						importExtentions	= null;

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public HoTTAdapter(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.hott.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
		this.serialPort = this.application != null ? new HoTTAdapterSerialPort(this, this.application) : new HoTTAdapterSerialPort(this, null);
		this.dialog = new HoTTAdapterDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			String toolTipText = HoTTAdapter.getImportToolTip();
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, toolTipText, toolTipText);
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}

		setPickerParameters();
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public HoTTAdapter(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.graupner.hott.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
		this.serialPort = this.application != null ? new HoTTAdapterSerialPort(this, this.application) : new HoTTAdapterSerialPort(this, null);
		this.dialog = new HoTTAdapterDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			String toolTipText = HoTTAdapter.getImportToolTip();
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, toolTipText, toolTipText);
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}

		setPickerParameters();
	}

	private void setPickerParameters() {
		this.pickerParameters.isChannelsChannelEnabled = this.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL) != null && this.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL).getValue() != "" //$NON-NLS-1$
				? Boolean.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.ENABLE_CHANNEL).getValue()) : false;
		this.pickerParameters.isFilterEnabled = this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER) != null && this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER).getValue() != "" //$NON-NLS-1$
				? Boolean.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.ENABLE_FILTER).getValue()) : true;
		this.pickerParameters.isFilterTextModus = this.getChannelProperty(ChannelPropertyTypes.TEXT_MODE) != null && this.getChannelProperty(ChannelPropertyTypes.TEXT_MODE).getValue() != "" //$NON-NLS-1$
				? Boolean.parseBoolean(this.getChannelProperty(ChannelPropertyTypes.TEXT_MODE).getValue()) : false;
		try {
			this.pickerParameters.altitudeClimbSensorSelection = this.getChannelProperty(ChannelPropertyTypes.SENSOR_ALT_CLIMB) != null && this.getChannelProperty(ChannelPropertyTypes.SENSOR_ALT_CLIMB).getValue() != null //$NON-NLS-1$
					? Integer.parseInt(this.getChannelProperty(ChannelPropertyTypes.SENSOR_ALT_CLIMB).getValue()) : 0;
		}
		catch (NumberFormatException e) {
			this.pickerParameters.altitudeClimbSensorSelection = 0;
		}
		
		//TODO unused in future releases
		this.pickerParameters.isTolerateSignChangeLatitude = this.getMeasruementProperty(3, 1, MeasurementPropertyTypes.TOLERATE_SIGN_CHANGE.value()) != null
				? Boolean.parseBoolean(this.getMeasruementProperty(3, 1, MeasurementPropertyTypes.TOLERATE_SIGN_CHANGE.value()).getValue()) : false;
		this.pickerParameters.isTolerateSignChangeLongitude = this.getMeasruementProperty(3, 2, MeasurementPropertyTypes.TOLERATE_SIGN_CHANGE.value()) != null
				? Boolean.parseBoolean(this.getMeasruementProperty(3, 2, MeasurementPropertyTypes.TOLERATE_SIGN_CHANGE.value()).getValue()) : false;
		this.pickerParameters.latitudeToleranceFactor = this.getMeasurementPropertyValue(3, 1, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().length() > 0
				? Double.parseDouble(this.getMeasurementPropertyValue(3, 1, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString()) : 90.0;
		this.pickerParameters.longitudeToleranceFactor = this.getMeasurementPropertyValue(3, 2, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().length() > 0
				? Double.parseDouble(this.getMeasurementPropertyValue(3, 2, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString()) : 25.0;
	}

	/**
	 * Collect the settings relevant for the values inserted in the histo vault.
	 * @return the settings which determine the measurement values returned by the reader
	 */
	@Override
	public String getReaderSettingsCsv() {
		return this.pickerParameters.getReaderSettingsCsv();
	}

	/**
	 * @return the serialPort
	 */
	@Override
	public HoTTAdapterSerialPort getCommunicationPort() {
		return this.serialPort;
	}

	/**
	 * load the mapping exist between lov file configuration keys and GDE keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	@Override
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// ...
		return lov2osdMap;
	}

	/**
	 * convert record LogView config data to GDE config keys into records section
	 * @param header reference to header data, contain all key value pairs
	 * @param lov2osdMap reference to the map where the key mapping
	 * @param channelNumber
	 * @return converted configuration data
	 */
	@Override
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// ...
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device
	 */
	@Override
	public int getLovDataByteSize() {
		return 0; // sometimes first 4 bytes give the length of data + 4 bytes for number
	}

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * this method is more usable for real logger, where data can be stored and converted in one block
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException
	 */
	@Override
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		// LogView doesn't support HoTT sensor logfiles
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte array with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;
		int tmpHeight, tmpClimb3, tmpClimb10, tmpCapacity, tmpRevolution, tmpVoltage, tmpCurrent, tmpCellVoltage, tmpVoltage1, tmpVoltage2, tmpLatitude, tmpLongitude, tmpPackageLoss, tmpVoltageRx,
				tmpTemperatureRx;

		switch (this.serialPort.protocolType) {
		case TYPE_19200_V3:
			switch (dataBuffer[1]) {
			case HoTTAdapter.SENSOR_TYPE_RECEIVER_19200:
				if (dataBuffer.length == 17) {
					// 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx
					points[0] = 0; // seams not part of live data ?? (dataBuffer[15] & 0xFF) * 1000;
					points[1] = (dataBuffer[9] & 0xFF) * 1000;
					points[2] = (dataBuffer[5] & 0xFF) * 1000;
					points[3] = DataParser.parse2Short(dataBuffer, 11) * 1000;
					points[4] = (dataBuffer[13] & 0xFF) * -1000;
					points[5] = (dataBuffer[9] & 0xFF) * -1000;
					points[6] = (dataBuffer[6] & 0xFF) * 1000;
					points[7] = (dataBuffer[7] & 0xFF) * 1000;
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
				if (dataBuffer.length == 31) {
					// 0=RXSQ, 1=Altitude, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
					points[0] = (dataBuffer[15] & 0xFF) * 1000;
					points[1] = DataParser.parse2Short(dataBuffer, 16) * 1000;
					points[2] = DataParser.parse2Short(dataBuffer, 22) * 1000;
					points[3] = DataParser.parse2Short(dataBuffer, 24) * 1000;
					points[4] = DataParser.parse2Short(dataBuffer, 26) * 1000;
					points[5] = (dataBuffer[8] & 0xFF) * 1000;
					points[6] = (dataBuffer[5] & 0xFF) * 1000;
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GPS_19200:
				if (dataBuffer.length == 40) {
					// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
					points[0] = (dataBuffer[15] & 0xFF) * 1000;
					points[1] = DataParser.parse2Short(dataBuffer, 20) * 10000 + DataParser.parse2Short(dataBuffer, 22);
					points[1] = dataBuffer[19] == 1 ? -1 * points[1] : points[1];
					points[2] = DataParser.parse2Short(dataBuffer, 25) * 10000 + DataParser.parse2Short(dataBuffer, 27);
					points[2] = dataBuffer[24] == 1 ? -1 * points[2] : points[2];
					points[3] = DataParser.parse2Short(dataBuffer, 31) * 1000;
					points[4] = DataParser.parse2Short(dataBuffer, 33) * 1000;
					points[5] = (dataBuffer[35] & 0xFF) * 1000;
					points[6] = DataParser.parse2Short(dataBuffer, 17) * 1000;
					points[7] = DataParser.parse2Short(dataBuffer, 29) * 1000;
					points[8] = (dataBuffer[16] & 0xFF) * 1000;
					points[9] = 0;
					points[10] = (dataBuffer[8] & 0xFF) * 1000;
					points[11] = (dataBuffer[5] & 0xFF) * 1000;
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
				if (dataBuffer.length == 48) {
					// 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
					points[0] = (dataBuffer[15] & 0xFF) * 1000;
					points[1] = DataParser.parse2Short(dataBuffer, 40) * 1000;
					points[2] = DataParser.parse2Short(dataBuffer, 38) * 1000;
					points[3] = DataParser.parse2Short(dataBuffer, 42) * 1000;
					points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
					points[5] = 0; // 5=Balance
					for (int j = 0; j < 6; j++) {
						points[j + 6] = (dataBuffer[16 + j] & 0xFF) * 1000;
						if (points[j + 6] > 0) {
							maxVotage = points[j + 6] > maxVotage ? points[j + 6] : maxVotage;
							minVotage = points[j + 6] < minVotage ? points[j + 6] : minVotage;
						}
					}
					// calculate balance on the fly
					points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0);
					points[12] = DataParser.parse2Short(dataBuffer, 31) * 1000;
					points[13] = DataParser.parse2Short(dataBuffer, 33) * 1000;
					points[14] = DataParser.parse2Short(dataBuffer, 35) * 1000;
					points[15] = (dataBuffer[37] & 0xFF) * 1000;
					points[16] = DataParser.parse2Short(dataBuffer, 29) * 1000;
					points[17] = DataParser.parse2Short(dataBuffer, 22) * 1000;
					points[18] = DataParser.parse2Short(dataBuffer, 24) * 1000;
					points[19] = (dataBuffer[26] & 0xFF) * 1000;
					points[20] = (dataBuffer[27] & 0xFF) * 1000;
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
				if (dataBuffer.length == 51) {
					// 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Altitude, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2
					points[0] = (dataBuffer[15] & 0xFF) * 1000;
					points[1] = DataParser.parse2Short(dataBuffer, 40) * 1000;
					points[2] = DataParser.parse2Short(dataBuffer, 38) * 1000;
					points[3] = DataParser.parse2Short(dataBuffer, 42) * 1000;
					points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
					points[5] = 0; // 5=Balance
					for (int j = 0; j < 14; j++) {
						points[j + 6] = (dataBuffer[16 + j] & 0xFF) * 1000;
						if (points[j + 6] > 0) {
							maxVotage = points[j + 6] > maxVotage ? points[j + 6] : maxVotage;
							minVotage = points[j + 6] < minVotage ? points[j + 6] : minVotage;
						}
					}
					// calculate balance on the fly
					points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0);
					points[20] = DataParser.parse2Short(dataBuffer, 36) * 1000;
					points[21] = DataParser.parse2Short(dataBuffer, 44) * 1000;
					points[22] = (dataBuffer[46] & 0xFF) * 1000;
					points[23] = DataParser.parse2Short(dataBuffer, 30) * 1000;
					points[24] = DataParser.parse2Short(dataBuffer, 32) * 1000;
					points[25] = (dataBuffer[34] & 0xFF) * 1000;
					points[26] = (dataBuffer[35] & 0xFF) * 1000;
				}
				break;
			}
			break;

		case TYPE_19200_V4:
			switch (dataBuffer[1]) {
			case HoTTAdapter.SENSOR_TYPE_RECEIVER_19200:
				if (dataBuffer.length >= 17) {
					// 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx, 8=VoltageRxMin
					tmpPackageLoss = DataParser.parse2Short(dataBuffer, 11);
					tmpVoltageRx = (dataBuffer[6] & 0xFF);
					tmpTemperatureRx = (dataBuffer[7] & 0xFF);
					if (!this.pickerParameters.isFilterEnabled || tmpPackageLoss > -1 && tmpVoltageRx > -1 && tmpVoltageRx < 100 && tmpTemperatureRx < 120) {
						points[0] = 0; // seams not part of live data ?? (dataBuffer[15] & 0xFF) * 1000;
						points[1] = (dataBuffer[9] & 0xFF) * 1000;
						points[2] = (dataBuffer[5] & 0xFF) * 1000;
						points[3] = tmpPackageLoss * 1000;
						points[4] = (dataBuffer[13] & 0xFF) * -1000;
						points[5] = (dataBuffer[8] & 0xFF) * -1000;
						points[6] = tmpVoltageRx * 1000;
						points[7] = tmpTemperatureRx * 1000;
						points[8] = (dataBuffer[10] & 0xFF) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
				if (dataBuffer.length == 57) {
					// 0=RXSQ, 1=Altitude, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
					points[0] = (dataBuffer[9] & 0xFF) * 1000;
					tmpHeight = DataParser.parse2Short(dataBuffer, 16);
					if (!this.pickerParameters.isFilterEnabled || tmpHeight > 10 && tmpHeight < 5000) {
						points[1] = tmpHeight * 1000;
						points[2] = DataParser.parse2Short(dataBuffer, 22) * 1000;
					}
					tmpClimb3 = DataParser.parse2Short(dataBuffer, 24);
					tmpClimb10 = DataParser.parse2Short(dataBuffer, 26);
					if (!this.pickerParameters.isFilterEnabled || tmpClimb3 > 20000 && tmpClimb10 > 20000 && tmpClimb3 < 40000 && tmpClimb10 < 40000) {
						points[3] = tmpClimb3 * 1000;
						points[4] = tmpClimb10 * 1000;
					}
					points[5] = (dataBuffer[6] & 0xFF) * 1000;
					points[6] = (dataBuffer[7] & 0xFF) * 1000;
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GPS_19200:
				if (dataBuffer.length == 57) {
					// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
					tmpLatitude = DataParser.parse2Short(dataBuffer, 20);
					tmpLongitude = DataParser.parse2Short(dataBuffer, 25);
					tmpHeight = DataParser.parse2Short(dataBuffer, 31);
					tmpClimb3 = dataBuffer[35] & 0xFF;
					if (!this.pickerParameters.isFilterEnabled || (tmpLatitude == tmpLongitude || tmpLatitude > 0) && tmpHeight > 10 && tmpHeight < 5000 && tmpClimb3 > 80) {
						points[0] = (dataBuffer[9] & 0xFF) * 1000;
						points[1] = DataParser.parse2Short(dataBuffer, 20) * 10000 + DataParser.parse2Short(dataBuffer, 22);
						points[1] = dataBuffer[19] == 1 ? -1 * points[1] : points[1];
						points[2] = tmpLongitude * 10000 + DataParser.parse2Short(dataBuffer, 27);
						points[2] = dataBuffer[24] == 1 ? -1 * points[2] : points[2];
						points[3] = tmpHeight * 1000;
						points[4] = DataParser.parse2Short(dataBuffer, 33) * 1000;
						points[5] = tmpClimb3 * 1000;
						points[6] = DataParser.parse2Short(dataBuffer, 17) * 1000;
						points[7] = DataParser.parse2Short(dataBuffer, 29) * 1000;
						points[8] = (dataBuffer[38] & 0xFF) * 1000;
						points[9] = 0;
						points[10] = (dataBuffer[6] & 0xFF) * 1000;
						points[11] = (dataBuffer[7] & 0xFF) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
				if (dataBuffer.length == 57) {
					// 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
					tmpVoltage = DataParser.parse2Short(dataBuffer, 40);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 42);
					tmpHeight = DataParser.parse2Short(dataBuffer, 33);
					tmpClimb3 = dataBuffer[37] & 0xFF;
					tmpVoltage1 = DataParser.parse2Short(dataBuffer, 22);
					tmpVoltage2 = DataParser.parse2Short(dataBuffer, 24);
					if (!this.pickerParameters.isFilterEnabled || tmpClimb3 > 80 && tmpHeight > 10 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600 && tmpCapacity >= points[3] / 1000) {
						points[0] = (dataBuffer[9] & 0xFF) * 1000;
						points[1] = tmpVoltage * 1000;
						points[2] = DataParser.parse2Short(dataBuffer, 38) * 1000;
						points[3] = tmpCapacity * 1000;
						points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
						if (tmpVoltage > 0) {
							for (int j = 0; j < 6; j++) {
								tmpCellVoltage = (dataBuffer[16 + j] & 0xFF);
								points[j + 6] = tmpCellVoltage > 0 ? tmpCellVoltage * 1000 : points[j + 6];
								if (points[j + 6] > 0) {
									maxVotage = points[j + 6] > maxVotage ? points[j + 6] : maxVotage;
									minVotage = points[j + 6] < minVotage ? points[j + 6] : minVotage;
								}
							}
							// calculate balance on the fly
							points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 10;
						}
						points[12] = DataParser.parse2Short(dataBuffer, 31) * 1000;
						points[13] = tmpHeight * 1000;
						points[14] = DataParser.parse2Short(dataBuffer, 35) * 1000;
						points[15] = tmpClimb3 * 1000;
						points[16] = DataParser.parse2Short(dataBuffer, 29) * 1000;
						points[17] = tmpVoltage1 * 1000;
						points[18] = tmpVoltage2 * 1000;
						points[19] = (dataBuffer[26] & 0xFF) * 1000;
						points[20] = (dataBuffer[27] & 0xFF) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
				if (dataBuffer.length == 57) {
					// 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Altitude, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2, 27=Revolution
					tmpVoltage = DataParser.parse2Short(dataBuffer, 40);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 42);
					tmpHeight = DataParser.parse2Short(dataBuffer, 36);
					tmpClimb3 = dataBuffer[46] & 0xFF;
					tmpVoltage1 = DataParser.parse2Short(dataBuffer, 30);
					tmpVoltage2 = DataParser.parse2Short(dataBuffer, 32);
					if (!this.pickerParameters.isFilterEnabled || tmpClimb3 > 80 && tmpHeight > 10 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600 && tmpCapacity >= points[3] / 1000) {
						points[0] = (dataBuffer[9] & 0xFF) * 1000;
						points[1] = tmpVoltage * 1000;
						points[2] = DataParser.parse2Short(dataBuffer, 38) * 1000;
						points[3] = tmpCapacity * 1000;
						points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
						if (tmpVoltage > 0) {
							for (int j = 0; j < 14; j++) {
								tmpCellVoltage = (dataBuffer[16 + j] & 0xFF);
								points[j + 6] = tmpCellVoltage > 0 ? tmpCellVoltage * 1000 : points[j + 6];
								if (points[j + 6] > 0) {
									maxVotage = points[j + 6] > maxVotage ? points[j + 6] : maxVotage;
									minVotage = points[j + 6] < minVotage ? points[j + 6] : minVotage;
								}
							}
							// calculate balance on the fly
							points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 10;
						}
						points[20] = tmpHeight * 1000;
						points[21] = DataParser.parse2Short(dataBuffer, 44) * 1000;
						points[22] = tmpClimb3 * 1000;
						points[23] = tmpVoltage1 * 1000;
						points[24] = tmpVoltage2 * 1000;
						points[25] = (dataBuffer[34] & 0xFF) * 1000;
						points[26] = (dataBuffer[35] & 0xFF) * 1000;
						points[27] = DataParser.parse2Short(dataBuffer, 47) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
				if (dataBuffer.length == 57) {
					// 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature
					points[0] = (dataBuffer[9] & 0xFF) * 1000;
					tmpVoltage = DataParser.parse2Short(dataBuffer, 16);
					tmpCurrent = DataParser.parse2Short(dataBuffer, 24);
					tmpRevolution = DataParser.parse2Short(dataBuffer, 28);
					if (!this.pickerParameters.isFilterEnabled || tmpVoltage > -1 && tmpVoltage < 1000 && tmpCurrent < 2550 && tmpRevolution > -1 && tmpRevolution < 2000) {
						points[1] = tmpVoltage * 1000;
						points[2] = tmpCurrent * 1000;
						points[3] = DataParser.parse2Short(dataBuffer, 20) * 1000;
						points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
						points[5] = tmpRevolution * 1000;
						points[6] = ((dataBuffer[35] & 0xFF) - 20) * 1000;
					}
				}
				break;
			}
			break;

		case TYPE_115200:
			switch (dataBuffer[0]) {
			case HoTTAdapter.SENSOR_TYPE_RECEIVER_115200:
				if (dataBuffer.length >= 21) {
					// 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx
					tmpPackageLoss = DataParser.parse2Short(dataBuffer, 12);
					tmpVoltageRx = dataBuffer[15] & 0xFF;
					tmpTemperatureRx = (DataParser.parse2Short(dataBuffer, 10) + 20);
					if (!this.pickerParameters.isFilterEnabled || tmpPackageLoss > -1 && tmpVoltageRx > -1 && tmpVoltageRx < 100 && tmpTemperatureRx < 100) {
						this.pickerParameters.reverseChannelPackageLossCounter.add((dataBuffer[5] & 0xFF) == 0 && (dataBuffer[4] & 0xFF) == 0 ? 0 : 1);
						points[0] = this.pickerParameters.reverseChannelPackageLossCounter.getPercentage() * 1000;// (dataBuffer[16] & 0xFF) * 1000;
						points[1] = (dataBuffer[17] & 0xFF) * 1000;
						points[2] = (dataBuffer[14] & 0xFF) * 1000;
						points[3] = tmpPackageLoss * 1000;
						points[4] = (dataBuffer[5] & 0xFF) * -1000;
						points[5] = (dataBuffer[4] & 0xFF) * -1000;
						points[6] = tmpVoltageRx * 1000;
						points[7] = tmpTemperatureRx * 1000;
						points[8] = (dataBuffer[10] & 0xFF) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
				if (dataBuffer.length >= 25) {
					// 0=RXSQ, 1=Altitude, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
					points[0] = (dataBuffer[3] & 0xFF) * 1000;
					tmpHeight = DataParser.parse2Short(dataBuffer, 10) + 500;
					if (!this.pickerParameters.isFilterEnabled || tmpHeight > 10 && tmpHeight < 5000) {
						points[1] = tmpHeight * 1000;
						points[2] = (DataParser.parse2Short(dataBuffer, 16) + 30000) * 1000;
					}
					tmpClimb3 = DataParser.parse2Short(dataBuffer, 18) + 30000;
					tmpClimb10 = DataParser.parse2Short(dataBuffer, 20) + 30000;
					if (!this.pickerParameters.isFilterEnabled || tmpClimb3 > 20000 && tmpClimb10 > 20000 && tmpClimb3 < 40000 && tmpClimb10 < 40000) {
						points[3] = tmpClimb3 * 1000;
						points[4] = tmpClimb10 * 1000;
					}
					points[5] = dataBuffer[4] * 1000;
					points[6] = (dataBuffer[5] + 20) * 1000;
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GPS_115200:
				if (dataBuffer.length >= 34) {
					// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
					tmpLatitude = DataParser.parse2Short(dataBuffer, 16);
					tmpLongitude = DataParser.parse2Short(dataBuffer, 20);
					tmpHeight = DataParser.parse2Short(dataBuffer, 14) + 500;
					tmpClimb3 = dataBuffer[30] + 120;
					if (!this.pickerParameters.isFilterEnabled || (tmpLatitude == tmpLongitude || tmpLatitude > 0) && tmpHeight > 10 && tmpHeight < 5000 && tmpClimb3 > 80) {
						points[0] = (dataBuffer[3] & 0xFF) * 1000;
						points[1] = tmpLatitude * 10000 + DataParser.parse2Short(dataBuffer, 18);
						points[1] = dataBuffer[26] == 1 ? -1 * points[1] : points[1];
						points[2] = tmpLongitude * 10000 + DataParser.parse2Short(dataBuffer, 22);
						points[2] = dataBuffer[27] == 1 ? -1 * points[2] : points[2];
						points[3] = tmpHeight * 1000;
						points[4] = (DataParser.parse2Short(dataBuffer, 28) + 30000) * 1000;
						points[5] = tmpClimb3 * 1000;
						points[6] = DataParser.parse2Short(dataBuffer, 10) * 1000;
						points[7] = DataParser.parse2Short(dataBuffer, 12) * 1000;
						points[8] = DataParser.parse2Short(dataBuffer, 24) * 500;
						points[9] = 0;
						points[10] = dataBuffer[4] * 1000;
						points[11] = (dataBuffer[5] + 20) * 1000;
						points[12] = dataBuffer[32];
						try {
							points[13] = Integer.valueOf(String.format("%c", dataBuffer[33])) * 1000;
						} catch (NumberFormatException e1) {
							// ignore;
						}
						/*
						 * dataBuffer[34] * 2 = HomeDirection
						 * dataBuffer[35] = acceleration x-direction
						 * dataBuffer[36] = acceleration y-direction
						 * dataBuffer[37] = acceleration z-direction
						 * dataBuffer[38,39] = gyro x //39 SM GPS-Logger ENL
						 * dataBuffer[40,41] = gyro y
						 * dataBuffer[42,43] = gyro z
						 * dataBuffer[44] = vibration
						 * dataBuffer[45] = free
						 * dataBuffer[46] = free
						 * dataBuffer[47] = free
						 * dataBuffer[48] = version
						 * dataBuffer[49,50] = CRC
						 */
						points[14] = (dataBuffer[1] & 0x0F) * 1000; // inverse event
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
				if (dataBuffer.length >= 49) {
					// 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
					tmpVoltage = DataParser.parse2Short(dataBuffer, 36);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 38);
					tmpHeight = DataParser.parse2Short(dataBuffer, 32) + 500;
					tmpClimb3 = dataBuffer[44] + 120;
					tmpVoltage1 = DataParser.parse2Short(dataBuffer, 22);
					tmpVoltage2 = DataParser.parse2Short(dataBuffer, 24);
					if (!this.pickerParameters.isFilterEnabled || tmpClimb3 > 80 && tmpHeight > 10 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600 && tmpCapacity >= points[3] / 1000) {
						points[0] = (dataBuffer[3] & 0xFF) * 1000;
						points[1] = tmpVoltage * 1000;
						points[2] = DataParser.parse2Short(dataBuffer, 34) * 1000;
						points[3] = tmpCapacity * 1000;
						points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
						if (tmpVoltage > 0) {
							for (int i = 0, j = 0; i < 6; i++, j += 2) {
								tmpCellVoltage = DataParser.parse2Short(dataBuffer, j + 10);
								points[i + 6] = tmpCellVoltage > 0 ? tmpCellVoltage * 500 : points[i + 6];
								if (points[i + 6] > 0) {
									maxVotage = points[i + 6] > maxVotage ? points[i + 6] : maxVotage;
									minVotage = points[i + 6] < minVotage ? points[i + 6] : minVotage;
								}
							}
							// calculate balance on the fly
							points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 10;
						}
						points[12] = DataParser.parse2Short(dataBuffer, 30) * 1000;
						points[13] = tmpHeight * 1000;
						points[14] = (DataParser.parse2Short(dataBuffer, 42) + 30000) * 1000;
						points[15] = tmpClimb3 * 1000;
						points[16] = DataParser.parse2Short(dataBuffer, 40) * 1000;
						points[17] = tmpVoltage1 * 1000;
						points[18] = tmpVoltage2 * 1000;
						points[19] = (DataParser.parse2Short(dataBuffer, 26) + 20) * 1000;
						points[20] = (DataParser.parse2Short(dataBuffer, 28) + 20) * 1000;
					}
				}
				break;

			case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
				if (dataBuffer.length >= 60) {
					// 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Altitude, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2
					tmpVoltage = DataParser.parse2Short(dataBuffer, 50);
					tmpCapacity = DataParser.parse2Short(dataBuffer, 52);
					tmpHeight = DataParser.parse2Short(dataBuffer, 46) + 500;
					tmpClimb3 = dataBuffer[56] + 120;
					tmpVoltage1 = DataParser.parse2Short(dataBuffer, 38);
					tmpVoltage2 = DataParser.parse2Short(dataBuffer, 40);
					if (!this.pickerParameters.isFilterEnabled || tmpClimb3 > 80 && tmpHeight > 10 && tmpHeight < 5000 && Math.abs(tmpVoltage1) < 600 && Math.abs(tmpVoltage2) < 600 && tmpCapacity >= points[3] / 1000) {
						points[0] = (dataBuffer[3] & 0xFF) * 1000;
						points[1] = DataParser.parse2Short(dataBuffer, 50) * 1000;
						points[2] = DataParser.parse2Short(dataBuffer, 48) * 1000;
						points[3] = tmpCapacity * 1000;
						points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
						if (tmpVoltage > 0) {
							for (int i = 0, j = 0; i < 14; i++, j += 2) {
								tmpCellVoltage = DataParser.parse2Short(dataBuffer, j + 10);
								points[i + 6] = tmpCellVoltage > 0 ? tmpCellVoltage * 500 : points[i + 6];
								if (points[i + 6] > 0) {
									maxVotage = points[i + 6] > maxVotage ? points[i + 6] : maxVotage;
									minVotage = points[i + 6] < minVotage ? points[i + 6] : minVotage;
								}
							}
							// calculate balance on the fly
							points[5] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 10;
						}
						points[20] = tmpHeight * 1000;
						points[21] = (DataParser.parse2Short(dataBuffer, 54) + 30000) * 1000;
						points[22] = (dataBuffer[46] + 120) * 1000;
						points[23] = tmpVoltage1 * 1000;
						points[24] = tmpVoltage2 * 1000;
						points[25] = (DataParser.parse2Short(dataBuffer, 42) + 20) * 1000;
						points[26] = (DataParser.parse2Short(dataBuffer, 44) + 20) * 1000;
						points[27] = DataParser.parse2Short(dataBuffer, 58) * 1000;
					}
				}
				break;
			case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
				if (dataBuffer.length >= 34) {
					// 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature
					points[0] = (dataBuffer[3] & 0xFF) * 1000;
					tmpVoltage = DataParser.parse2Short(dataBuffer, 10);
					tmpCurrent = DataParser.parse2Short(dataBuffer, 14);
					tmpRevolution = DataParser.parse2Short(dataBuffer, 18);
					if (!this.pickerParameters.isFilterEnabled || tmpVoltage > -1 && tmpVoltage < 1000 && tmpCurrent < 2550 && tmpRevolution > -1 && tmpRevolution < 2000) {
						points[1] = tmpVoltage * 1000;
						points[2] = tmpCurrent * 1000;
						points[3] = DataParser.parse2Short(dataBuffer, 22) * 1000;
						points[4] = Double.valueOf(points[1] / 1000.0 * points[2]).intValue(); // power U*I [W];
						points[5] = tmpRevolution * 1000;
						points[6] = DataParser.parse2Short(dataBuffer, 24) * 1000;
						// points[7] = dataBuffer[19] * 1000;
					}
				}
				break;
			case HoTTAdapter.SENSOR_TYPE_SERVO_POSITION_115200:
				if (dataBuffer.length >= 74) {
					//log.log(Level.OFF, StringHelper.byte2Hex2CharString(dataBuffer, dataBuffer.length));
					StringBuffer sb = new StringBuffer();
					for (int i = 0, j = 0; i < 16; i++, j+=2) {
						sb.append(String.format("%2d = %4d; ", i+1, DataParser.parse2Short(dataBuffer, 8 + j) / 16 + 50));					
					}
					log.log(Level.OFF, sb.toString());
				}
				break;
			}
			break;
		}
		return points;
	}

	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * since this is a long term operation the progress bar should be updated to signal business to user
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException
	 */
	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		int[] points = new int[recordSet.size()];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 1;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
		int index = 0;
		for (int i = 0; i < recordDataSize; i++) {
			index = i * dataBufferSize + timeStampBufferSize;
			if (HoTTAdapter.log.isLoggable(java.util.logging.Level.FINER)) HoTTAdapter.log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + index); //$NON-NLS-1$

			for (int j = 0; j < points.length; j++) {
				points[j] = (((dataBuffer[0 + (j * 4) + index] & 0xff) << 24) + ((dataBuffer[1 + (j * 4) + index] & 0xff) << 16) + ((dataBuffer[2 + (j * 4) + index] & 0xff) << 8)
						+ ((dataBuffer[3 + (j * 4) + index] & 0xff) << 0));
			}

			recordSet.addPoints(points,
					(((dataBuffer[0 + (i * 4)] & 0xff) << 24) + ((dataBuffer[1 + (i * 4)] & 0xff) << 16) + ((dataBuffer[2 + (i * 4)] & 0xff) << 8) + ((dataBuffer[3 + (i * 4)] & 0xff) << 0)) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * Add record data points from file stream to each measurement.
	 * It is possible to add only none calculation records if makeInActiveDisplayable calculates the rest.
	 * Do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * Reduces memory and cpu load by taking measurement samples every x ms based on device setting |histoSamplingTime| .
	 * @param recordSet is the target object holding the records (curves) which include measurement curves and calculated curves
	 * @param dataBuffer holds rows for each time step (i = recordDataSize) with measurement data (j = recordNamesLength equals the number of measurements)
	 * @param recordDataSize is the number of time steps
	 */
	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, int[] maxPoints, int[] minPoints, Analyzer analyzer) throws DataInconsitsentException {
		if (maxPoints.length != minPoints.length || maxPoints.length == 0) throw new DataInconsitsentException("number of max/min points differs: " + maxPoints.length + "/" + minPoints.length); //$NON-NLS-1$

		int recordTimespan_ms = 10;
		UniversalSampler histoRandomSample = UniversalSampler.createSampler(recordSet.getChannelConfigNumber(), maxPoints, minPoints, recordTimespan_ms, analyzer);
		int[] points = histoRandomSample.getPoints();
		IntBuffer intBuffer = ByteBuffer.wrap(dataBuffer).asIntBuffer(); // no performance penalty compared to familiar bit shifting solution
		for (int i = 0, pointsLength = points.length; i < recordDataSize; i++) {
			for (int j = 0, iOffset = i * pointsLength + recordDataSize; j < pointsLength; j++) {
				points[j] = intBuffer.get(j + iOffset);
			}
			int timeStep_ms = intBuffer.get(i) / 10;
			if (histoRandomSample.capturePoints(timeStep_ms)) recordSet.addPoints(points, timeStep_ms);
		}
		recordSet.syncScaleOfSyncableRecords();
		if (log.isLoggable(Level.FINE)) log.log(Level.INFO, String.format("%s processed: %,9d", recordSet.getChannelConfigName(), recordDataSize)); //$NON-NLS-1$
	}

	/**
	 * @return true if the device supports a native file import for histo purposes
	 */
	@Override
	public boolean isHistoImportSupported() {
		return this.getClass().equals(HoTTAdapter.class) && !this.getClass().equals(HoTTAdapterD.class) && !this.getClass().equals(HoTTAdapterM.class) && !this.getClass().equals(HoTTAdapterX.class)
				&& !this.getClass().equals(HoTTViewer.class);
	}

	private void setSupportedImportExtentions() {
		if (isHistoImportSupported()) {
			this.importExtentions = Arrays.stream(getDataBlockPreferredFileExtention().split(GDE.REGEX_FILE_EXTENTION_SEPARATION)) //
					.map(s -> s.substring(s.lastIndexOf(GDE.CHAR_DOT))).map(e -> e.toLowerCase()) //
					.collect(Collectors.toList());
		} else {
			this.importExtentions = new ArrayList<>();
		}
	}

	/**
	 * @return the device's native file extentions if the device supports histo imports (e.g. '.bin' or '.log')
	 */
	@Override
	public List<String> getSupportedImportExtentions() {
		if (this.importExtentions == null) setSupportedImportExtentions();
		return this.importExtentions;
	}

	/**
	 * Extended consumer supporting exceptions.
	 */
	@FunctionalInterface
	interface CheckedConsumer<T> {
		void accept(T t) throws DataInconsitsentException, IOException, DataTypeException;
	}

	/**
	 * create recordSet and add record data size points from binary file to each measurement.
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest.
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data.
	 * collects life data if device setting |isLiveDataActive| is true.
	 * reduces memory and cpu load by taking measurement samples every x ms based on device setting |histoSamplingTime| .
	 * @param inputStream for loading the log data
	 * @param truss references the requested vault for feeding with the results (vault might be without measurements, settlements and scores)
	 */
	@Override
	public void getRecordSetFromImportFile(Supplier<InputStream> inputStream, VaultCollector truss, Analyzer analyzer)
			throws DataInconsitsentException, IOException, DataTypeException {
		String fileEnding = PathUtils.getFileExtention(truss.getVault().getLoadFilePath());
		if (GDE.FILE_ENDING_DOT_BIN.equals(fileEnding)) {
			HoTTbinHistoReader histoReader = new HoTTbinHistoReader(new PickerParameters(analyzer));
			histoReader.read(inputStream, truss);
		} else if (GDE.FILE_ENDING_DOT_LOG.equals(fileEnding)) {
			// todo implement HoTTlogHistoReader
		} else {
			throw new UnsupportedOperationException(truss.getVault().getLoadFilePath());
		}
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			int channel = recordSet.getChannelConfigNumber();
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				int ordinal = record.getOrdinal();
				// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb, 5=Velocity, 6=Distance, 7=Direction, 8=TripDistance, 9=VoltageRx, 10=TemperatureRx
				if (channel == 1 && ordinal >= 0 && ordinal <= 5) { // Receiver
					dataTableRow[index + 1] = String.format("%.0f", (record.realGet(rowIndex) / 1000.0)); //$NON-NLS-1$
				}
				else if (channel == 6 && ordinal == 22) { //Channels warning
					dataTableRow[index + 1] = record.realGet(rowIndex) == 0
							? GDE.STRING_EMPTY
							: String.format("'%c'", ((record.realGet(rowIndex) / 1000) + 64));
				}
				else {
					dataTableRow[index + 1] = record.getFormattedTableValue(rowIndex);
				}
				++index;
			}
		} catch (RuntimeException e) {
			HoTTAdapter.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double translateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
		double newValue = 0;

		if (record.getAbstractParent().getChannelConfigNumber() == 3 && (record.getOrdinal() == 1 || record.getOrdinal() == 2)) { // 1=GPS-longitude 2=GPS-latitude
			// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			int grad = ((int) (value / 1000));
			double minuten = (value - (grad * 1000.0)) / 10.0;
			newValue = grad + minuten / 60.0;
		}
		else if (record.getAbstractParent().getChannelConfigNumber() == 6 && (record.getOrdinal() >= 3 && record.getOrdinal() <= 18)) {
			if (this.pickerParameters.isChannelPercentEnabled) {
				if (!record.getUnit().equals("%")) record.setUnit("%");
				factor = 0.250;
				reduction = 1500.0;
				newValue = (value - reduction) * factor;
			}
			else {
				if (!record.getUnit().equals("µsec")) record.setUnit("µsec");
				newValue = (value - reduction) * factor + offset;
			}
		}
		// ET logic differs compared to prepareDataTableRow for getChannelConfigNumber() == 1 (Receiver)
		else {
			newValue = (value - reduction) * factor + offset;
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
		double newValue = 0;

		if ((record.getOrdinal() == 1 || record.getOrdinal() == 2) && record.getAbstractParent().getChannelConfigNumber() == 3) { // 1=GPS-longitude 2=GPS-latitude )
			// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			int grad = (int) value;
			double minuten = (value - grad * 1.0) * 60.0;
			newValue = (grad + minuten / 100.0) * 1000.0;
		} 
		else if (record.getAbstractParent().getChannelConfigNumber() == 6 && (record.getOrdinal() >= 3 && record.getOrdinal() <= 18)) {
			if (this.pickerParameters.isChannelPercentEnabled) {
				if (!record.getUnit().equals("%")) record.setUnit("%");
				factor = 0.250;
				reduction = 1500.0;
				newValue = value / factor + reduction;
			}
			else {
				if (!record.getUnit().equals("µsec")) record.setUnit("µsec");
				newValue = (value - reduction) * factor;
			}
		}
		else {
			newValue = (value - offset) / factor + reduction;
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * check and update visibility status of all records according the available device configuration
	 * this function must have only implementation code if the device implementation supports different configurations
	 * where some curves are hided for better overview
	 * example: if device supports voltage, current and height and no sensors are connected to voltage and current
	 * it makes less sense to display voltage and current curves, if only height has measurement data
	 * at least an update of the graphics window should be included at the end of this method
	 */
	@Override
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int channelConfigNumber = recordSet.getChannelConfigNumber();
		int displayableCounter = 0;
		boolean configChanged = this.isChangePropery();
		Record record;

		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			if (HoTTAdapter.log.isLoggable(java.util.logging.Level.FINE))
				HoTTAdapter.log.log(java.util.logging.Level.FINE, record.getName() + " = " + DeviceXmlResource.getInstance().getReplacement(this.getMeasurementNames(channelConfigNumber)[i])); //$NON-NLS-1$

			MeasurementType measurement = this.getMeasurement(channelConfigNumber, i);
			if (record.isActive() && record.isActive() != measurement.isActive()) { //corrected values from older OSD might be overwritten p.e. VoltageRx_min
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				if (HoTTAdapter.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapter.log.log(java.util.logging.Level.FINE, "switch " + record.getName() + " to " + measurement.isActive()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (includeReasonableDataCheck) {
				record.setDisplayable(measurement.isActive() && record.hasReasonableData());
				if (HoTTAdapter.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapter.log.log(java.util.logging.Level.FINE, record.getName() + " hasReasonableData " + record.hasReasonableData()); //$NON-NLS-1$
			}

			if (record.isActive() && record.isDisplayable()) {
				++displayableCounter;
				if (HoTTAdapter.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapter.log.log(java.util.logging.Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
			}
		}
		if (HoTTAdapter.log.isLoggable(java.util.logging.Level.FINE)) HoTTAdapter.log.log(java.util.logging.Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
		this.setChangePropery(configChanged); // reset configuration change indicator to previous value, do not vote automatic configuration change at all
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread,
	 * target is to make sure all data point not coming from device directly are available and can be displayed
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		calculateInactiveRecords(recordSet);
		recordSet.syncScaleOfSyncableRecords();
		this.updateVisibilityStatus(recordSet, true);
		this.application.updateStatisticsData();
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 */
	@Override
	public void calculateInactiveRecords(RecordSet recordSet) {
		if (recordSet.getChannelConfigNumber() == 3) { // 1=GPS-longitude 2=GPS-latitude 3=Altitude
			// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			Record recordLatitude = recordSet.get(1);
			Record recordLongitude = recordSet.get(2);
			Record recordAlitude = recordSet.get(3);
			if (recordLatitude.hasReasonableData() && recordLongitude.hasReasonableData() && recordAlitude.hasReasonableData()) {
				int recordSize = recordLatitude.realSize();
				int startAltitude = recordAlitude.get(0); // using this as start point might be sense less if the GPS data has no 3D-fix
				// check GPS latitude and longitude
				int indexGPS = 0;
				int i = 0;
				for (; i < recordSize; ++i) {
					if (recordLatitude.get(i) != 0 && recordLongitude.get(i) != 0) {
						indexGPS = i;
						++i;
						break;
					}
				}
				startAltitude = recordAlitude.get(indexGPS); // set initial altitude to enable absolute altitude calculation

				GPSHelper.calculateTripLength(this, recordSet, 1, 2, 3, startAltitude, 7, 9);
				// GPSHelper.calculateLabs(this, recordSet, 1, 2, 7, 9, 6);
			}
		}
	}

	/**
	 * @return the dialog
	 */
	@Override
	public HoTTAdapterDialog getDialog() {
		return this.dialog;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION };
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data
	 */
	@Override
	public void open_closeCommPort() {
		switch (this.application.getMenuBar().getSerialPortIconSet()) {
		case DeviceCommPort.ICON_SET_IMPORT_CLOSE:
			importDeviceData();
			break;

		case DeviceCommPort.ICON_SET_START_STOP:
			this.serialPort.isInterruptedByUser = true;
			break;
		}
	}

	/**
	 * import device specific *.bin data files
	 */
	protected void importDeviceData() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT2400), "LogData"); //$NON-NLS-1$

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					HoTTAdapter.this.application.setPortConnected(true);
					boolean isInitialSwitched = false;

					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						if (!selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_BIN) && !selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
							log.log(Level.WARNING, String.format("skip selectedImportFile %s since it has not a supported file ending", selectedImportFile));
						}
						HoTTAdapter.log.log(java.util.logging.Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > MIN_FILENAME_LENGTH) {
							// String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT) - 4, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));

							String directoryName = ObjectKeyCompliance.getUpcomingObjectKey(Paths.get(selectedImportFile));
							if (!directoryName.isEmpty()) ObjectKeyCompliance.createObjectKey(directoryName);

							try {
								// use a copy of the picker parameters to avoid changes by the reader
								if (selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_BIN)) {
									HoTTbinReader.read(selectedImportFile, new PickerParameters(HoTTAdapter.this.pickerParameters));
								}
								else if (selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
									HoTTlogReader.read(selectedImportFile, new PickerParameters(HoTTAdapter.this.pickerParameters));
								}
								if (!isInitialSwitched) {
									if (HoTTAdapter.this.application.getActiveChannel().getActiveRecordSet() == null) {
										Channel selectedChannel = Settings.getInstance().isFirstRecordSetChoice() ? HoTTAdapter.this.channels.get(1) : HoTTAdapter.this.application.getActiveChannel();
										HoTTbinReader.channels.switchChannel(selectedChannel.getName());
									} else {
										String recordSetType = HoTTAdapter.this.application.getActiveChannel().getActiveRecordSet().getName().split(Pattern.quote("["))[0].split(Pattern.quote(")"))[1];
										Channel selectedChannel = Settings.getInstance().isFirstRecordSetChoice() ? HoTTAdapter.this.channels.get(1) : HoTTAdapter.this.application.getActiveChannel();

										// find the latest recordSet just imported by this method
										if (Settings.getInstance().isFirstRecordSetChoice()) {
											String lastCurrentNumber = "";
											String lastNameMatch = null;
											for (String tmpName : selectedChannel.getRecordSetNames()) {
												String currentNumber = tmpName.split(Pattern.quote(")"))[0];
												if (!currentNumber.equals(lastCurrentNumber)) {
													lastCurrentNumber = currentNumber;
													lastNameMatch = tmpName;
												}
											}
											HoTTbinReader.channels.switchChannel(selectedChannel.getName());
											selectedChannel.switchRecordSet(lastNameMatch);
										} else {
											String lastNameMatch = null;
											for (String tmpName : selectedChannel.getRecordSetNames()) {
												if (tmpName.contains(recordSetType)) lastNameMatch = tmpName;
											}
											HoTTbinReader.channels.switchChannel(selectedChannel.getName());
											selectedChannel.switchRecordSet(lastNameMatch);
										}
									}
									isInitialSwitched = true;
								}
								WaitTimer.delay(500);
							} catch (Exception e) {
								HoTTAdapter.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
							}
						}
					}
				} finally {
					HoTTAdapter.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}

	/**
	 * import device specific *.bin data files
	 * @param filePath
	 */
	@Override
	public void importDeviceData(Path filePath) {
		if (!this.application.getDeviceSelectionDialog().checkDataSaved()) return;

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					boolean isInitialSwitched = false;
					HoTTAdapter.this.application.setPortConnected(true);

					if (filePath.getFileName().toString().length() > MIN_FILENAME_LENGTH) {
						for (Channel channel : HoTTAdapter.this.channels.values()) {
							channel.clear();
						}

						try {
							// use a copy of the picker parameters to avoid changes by the reader
							if (HoTTAdapter.this.getClass().equals(HoTTAdapter.class))
								HoTTbinReader.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
							else if (HoTTAdapter.this.getClass().equals(HoTTAdapter2.class))
								HoTTbinReader2.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
							else if (HoTTAdapter.this.getClass().equals(HoTTAdapter2M.class))
								HoTTbinReader2.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
							else if (HoTTAdapter.this.getClass().equals(HoTTAdapterD.class))
								HoTTbinReaderD.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
							else if (HoTTAdapter.this.getClass().equals(HoTTAdapterM.class))
								HoTTbinReader.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
							else if (HoTTAdapter.this.getClass().equals(HoTTAdapterX.class))
								HoTTbinReaderX.read(filePath.toString(), new PickerParameters(HoTTAdapter.this.pickerParameters));
							else
								throw new UnsupportedOperationException();

							if (!isInitialSwitched) {
								Channel selectedChannel = Settings.getInstance().isFirstRecordSetChoice() ? HoTTAdapter.this.channels.get(1) : HoTTAdapter.this.application.getActiveChannel();
								if (HoTTAdapter.this.getClass().equals(HoTTAdapter.class) || HoTTAdapter.this.getClass().equals(HoTTAdapterM.class) || HoTTAdapter.this.getClass().equals(HoTTAdapterX.class)) {
									HoTTbinReader.channels.switchChannel(selectedChannel.getName());
								} else if (HoTTAdapter.this.getClass().equals(HoTTAdapter2.class) || HoTTAdapter.this.getClass().equals(HoTTAdapter2M.class)) {
									HoTTbinReader.channels.switchChannel(selectedChannel.getName());
									selectedChannel.switchRecordSet(HoTTbinReader2.recordSet.getName());
								} else if (HoTTAdapter.this.getClass().equals(HoTTAdapterD.class)) {
									HoTTbinReader.channels.switchChannel(selectedChannel.getName());
									selectedChannel.switchRecordSet(HoTTbinReaderD.recordSet.getName());
								} else
									throw new UnsupportedOperationException();
								isInitialSwitched = true;
							}

							WaitTimer.delay(500);
						} catch (Exception e) {
							HoTTAdapter.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
						}
					}
				} finally {
					HoTTAdapter.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}

	/**
	 * update the file export menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	public void updateFileExportMenu(Menu exportMenue) {
		MenuItem convertKMZ3DRelativeItem;
		MenuItem convertKMZDAbsoluteItem;
		// MenuItem convertGPXItem;
		// MenuItem convertGPXGarminItem;

		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0732))) {
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertKMZ3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT2405));
			convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					HoTTAdapter.log.log(java.util.logging.Level.FINEST, "convertKMZ3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2406));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					HoTTAdapter.log.log(java.util.logging.Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2407));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					HoTTAdapter.log.log(java.util.logging.Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
				}
			});

			// convertGPXItem = new MenuItem(exportMenue, SWT.PUSH);
			// convertGPXItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0728));
			// convertGPXItem.addListener(SWT.Selection, new Listener() {
			// public void handleEvent(Event e) {
			// log.log(java.util.logging.Level.FINEST, "convertGPXItem action performed! " + e); //$NON-NLS-1$
			// export2GPX(false);
			// }
			// });
			//
			// convertGPXGarminItem = new MenuItem(exportMenue, SWT.PUSH);
			// convertGPXGarminItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0729));
			// convertGPXGarminItem.addListener(SWT.Selection, new Listener() {
			// public void handleEvent(Event e) {
			// log.log(java.util.logging.Level.FINEST, "convertGPXGarminItem action performed! " + e); //$NON-NLS-1$
			// export2GPX(true);
			// }
			// });
		}
	}

	/**
	 * update the file import menu by adding new entry to import device specific files
	 * @param importMenue
	 */
	public void updateFileImportMenu(Menu importMenue) {
		MenuItem importDeviceLogItem;

		if (importMenue.getItem(importMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {
			new MenuItem(importMenue, SWT.SEPARATOR);

			importDeviceLogItem = new MenuItem(importMenue, SWT.PUSH);
			String[] messageParams = new String[GDE.MOD1.length + 1];
			System.arraycopy(GDE.MOD1, 0, messageParams, 1, GDE.MOD1.length);
			messageParams[0] = this.getDeviceConfiguration().getDataBlockPreferredFileExtention();
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT2416, messageParams));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT2416));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					HoTTAdapter.log.log(java.util.logging.Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					importDeviceData();
				}
			});
		}
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE
	 */
	public void export2KMZ3D(int type) {
		// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGT2403), 2, 1, 3, 6, 5, 9, -1, type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param isGarminExtension
	 */
	public void export2GPX(final boolean isGarminExtension) {
		// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		if (isGarminExtension)
			new FileHandler().exportFileGPX(Messages.getString(gde.messages.MessageIds.GDE_MSGT0730), 1, 2, 3, 6, -1, -1, -1, -1, new int[] { -1, -1, -1 });
		else
			new FileHandler().exportFileGPX(Messages.getString(gde.messages.MessageIds.GDE_MSGT0730), 1, 2, 3, 6, -1, -1, -1, -1, new int[0]);
	}

	/**
	 * query if the given record is longitude or latitude of GPS data, such data needs translation for display as graph
	 * @param record
	 * @return true if the record is a latitude or longitude record
	 */
	@Override
	public boolean isGPSCoordinates(Record record) {
		if (this.application.getActiveChannelNumber() == 3) {
			// 0=RXSQ, 1=Latitude, 2=Longitude
			return record.getOrdinal() == 1 || record.getOrdinal() == 2;
		}
		return false;
	}

	/**
	 * @return the translated latitude and longitude to IGC latitude {DDMMmmmN/S, DDDMMmmmE/W} for GPS devices only
	 */
	@Override
	public String translateGPS2IGC(RecordSet recordSet, int index, char fixValidity, int startAltitude, int offsetAltitude) {
		// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		Record recordLatitude = recordSet.get(1);
		Record recordLongitude = recordSet.get(2);
		Record gpsAlitude = recordSet.get(3);

		return String.format("%02d%05d%s%03d%05d%s%c%05.0f%05.0f", //$NON-NLS-1$
				recordLatitude.get(index) / 1000000, Double.valueOf(recordLatitude.get(index) % 1000000 / 10.0 + 0.5).intValue(), recordLatitude.get(index) > 0 ? "N" : "S", //$NON-NLS-1$ //$NON-NLS-2$
				recordLongitude.get(index) / 1000000, Double.valueOf(recordLongitude.get(index) % 1000000 / 10.0 + 0.5).intValue(), recordLongitude.get(index) > 0 ? "E" : "W", //$NON-NLS-1$ //$NON-NLS-2$
				fixValidity, (this.translateValue(gpsAlitude, gpsAlitude.get(index) / 1000.0) + offsetAltitude), (this.translateValue(gpsAlitude, gpsAlitude.get(index) / 1000.0) + offsetAltitude));
	}

	/**
	 * query if the actual record set of this device contains GPS data to enable KML export to enable google earth visualization
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public boolean isActualRecordSetWithGpsData() {
		boolean containsGPSdata = false;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null && activeChannel.getNumber() == 3) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
				containsGPSdata = activeRecordSet.get(1).hasReasonableData() && activeRecordSet.get(2).hasReasonableData();
			}
		}
		return containsGPSdata;
	}

	/**
	 * export a file of the actual channel/record set
	 * @return full qualified file path depending of the file ending type
	 */
	@Override
	public String exportFile(String fileEndingType, boolean isExport2TmpDir) {
		String exportFileName = GDE.STRING_EMPTY;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && fileEndingType.contains(GDE.FILE_ENDING_KMZ)) {
				// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
				final int additionalMeasurementOrdinal = this.getGPS2KMZMeasurementOrdinal();
				exportFileName = new FileHandler().exportFileKMZ(2, 1, 3, additionalMeasurementOrdinal, 5, 9, -1, true, isExport2TmpDir);
			}
		}
		return exportFileName;
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
		// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		if (this.kmzMeasurementOrdinal == null) // keep usage as initial supposed and use speed measurement ordinal
			return 6;

		return this.kmzMeasurementOrdinal;
	}

	/**
	 * @param isFilterEnabled the isFilterEnabled to set
	 */
	public synchronized void setFilterProperties(boolean isFilterEnabled) {
		this.pickerParameters.isFilterEnabled = isFilterEnabled;
	}

	/**
	 * @param isTextModusFilterEnabled the isTextModusFilterEnabled to set
	 */
	public synchronized void setTextModusFilter(boolean isTextModusFilterEnabled) {
		this.pickerParameters.isFilterTextModus = isTextModusFilterEnabled;
	}

	/**
	 * @param isChannelPercentEnabled the isChannelPercentEnabled to set
	 */
	public synchronized void setChannelPercent(boolean isChannelPercentEnabled) {
		this.pickerParameters.isChannelPercentEnabled = isChannelPercentEnabled;
	}
	
	/**
	 * @param altitudeClimbComboSelectionIndex the altitudeClimbSensorSelection index to set
	 */
	public synchronized void setAltitudeClimbSelectionProperties(int altitudeClimbComboSelectionIndex) {
		this.pickerParameters.altitudeClimbSensorSelection = altitudeClimbComboSelectionIndex;
	}

	/**
	 * @return the tooltip text for the import menu bar button
	 */
	public static String getImportToolTip() {
		DeviceConfiguration hoTTConfiguration = Analyzer.getInstance().getDeviceConfigurations().get("HoTTAdapter");
		String fileExtentions = hoTTConfiguration != null ? hoTTConfiguration.getDataBlockPreferredFileExtention() : GDE.STRING_QUESTION_MARK;
		return Messages.getString(MessageIds.GDE_MSGT2404, new Object[] { fileExtentions });
	}

	/**
	 * @param isChannelEnabled the isChannelEnabled to set
	 */
	public synchronized void setChannelEnabledProperty(boolean isChannelEnabled) {
		this.pickerParameters.isChannelsChannelEnabled = isChannelEnabled;
	}

	/**
	 * This function allows to register a custom CTabItem to the main application tab folder to display device
	 * specific curve calculated from point combinations or other specific dialog
	 * As default the function should return null which stands for no device custom tab item.
	 */
	@Override
	public CTabItem getUtilityDeviceTabItem() {
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if (HoTTAdapter.this.isMdlTabRequested()) {
					CTabItem mdlTabItem = HoTTAdapter.this.getMdlTabItem();
					if (mdlTabItem != null) {
						DataExplorer.getInstance().registerCustomTabItem(mdlTabItem);
					}
				}
			}
		});

		return new FileTransferTabItem(this.application.getTabFolder(), SWT.NONE, this.application.getTabFolder().getItemCount(), this, this.serialPort);
	}

	/**
	 * query if the MDL decoder tab item can be displayed
	 * @return the value of the property, if property does not exist return false (default behavior of Boolean)
	 */
	public boolean isMdlTabRequested() {
		boolean rc = true;
		try {
			String className = "de.treichels.hott.mdlviewer.swt.Launcher";//$NON-NLS-1$
			// log.log(Level.OFF, "loading Class " + className); //$NON-NLS-1$
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Class<?> c = loader.loadClass(className);
			// Class c = Class.forName(className);
			Constructor<?> constructor = c.getDeclaredConstructor();
			if (constructor != null) {
				constructor.newInstance();
			} else {
				HoTTAdapter.log.log(java.util.logging.Level.OFF, "de.treichels.hott.mdlviewer.swt.Launcher can not be loaded"); //$NON-NLS-1$
				rc = false;
			}
		} catch (final Throwable t) {
			HoTTAdapter.log.log(java.util.logging.Level.OFF, "de.treichels.hott.mdlviewer.swt.Launcher can not be loaded"); //$NON-NLS-1$
			rc = false;
		}
		return rc;
	}

	/**
	 * This function allows to register a CTabItem to to display MDL content converted to HTML
	 */
	public CTabItem getMdlTabItem() {
		Object inst = null;
		try {
			String className = "de.treichels.hott.mdlviewer.swt.MdlTabItem";//$NON-NLS-1$
			HoTTAdapter.log.log(java.util.logging.Level.OFF, "loading Class " + className); //$NON-NLS-1$
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Class<?> c = loader.loadClass(className);
			Constructor<?> constructor = c.getDeclaredConstructor(new Class[] { CTabFolder.class, int.class, int.class });
			HoTTAdapter.log.log(java.util.logging.Level.OFF, "constructor != null -> " + (constructor != null ? "true" : "false")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (constructor != null) {

				// set directory where to start search for mdl files
				System.setProperty("log.dir", Settings.getLogFilePath().substring(0, Settings.getLogFilePath().lastIndexOf(GDE.CHAR_FILE_SEPARATOR_UNIX))); //$NON-NLS-1$
				HoTTAdapter.log.log(java.util.logging.Level.OFF, "log.dir =  " + System.getProperty("log.dir")); //$NON-NLS-1$ //$NON-NLS-2$
				System.setProperty("mdl.dir", Settings.getInstance().getDataFilePath());//$NON-NLS-1$
				HoTTAdapter.log.log(java.util.logging.Level.OFF, "mdl.dir =  " + System.getProperty("mdl.dir")); //$NON-NLS-1$ //$NON-NLS-2$
				URL url = GDE.class.getProtectionDomain().getCodeSource().getLocation();
				System.setProperty("program.dir", url.getFile().substring(0, url.getPath().lastIndexOf(DataExplorer.class.getSimpleName())));//$NON-NLS-1$
				HoTTAdapter.log.log(java.util.logging.Level.OFF, "program.dir =  " + System.getProperty("program.dir")); //$NON-NLS-1$ //$NON-NLS-2$
				System.setProperty("template.dir", "");// load from classpath //$NON-NLS-1$ //$NON-NLS-2$
				HoTTAdapter.log.log(java.util.logging.Level.OFF, "template.dir =  " + System.getProperty("template.dir")); //$NON-NLS-1$ //$NON-NLS-2$

				inst = constructor.newInstance(new Object[] { this.application.getTabFolder(), SWT.NONE, this.application.getTabFolder().getItemCount() });
			}
		} catch (final Throwable t) {
			t.printStackTrace();
		}
		if (HoTTAdapter.log.isLoggable(java.util.logging.Level.OFF) && inst != null) HoTTAdapter.log.log(java.util.logging.Level.OFF, "loading TabItem " + ((CTabItem) inst).getText()); //$NON-NLS-1$

		if (inst != null) ((CTabItem) inst).setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + (GDE.IS_LINUX ? 3 : 1), SWT.NORMAL));
		return (CTabItem) inst;
	}

	/**
	 * calculate labs based on Rx dbm and based on distance from start point
	 * HoTTAdapterD
	 * //5=Rx_dbm, 109=SmoothedRx_dbm, 110=DiffRx_dbm, 111=LapsRx_dbm
	 * //15=DistanceStart, 112=DiffDistance, 113=LapsDistance
	 * @param recordSet
	 * @param channelNumber
	 * @param ordinalSourceRx_dbm
	 * @param ordinalSmoothRx_dbm
	 * @param ordinalDiffRx_dbm
	 * @param ordinalLabsRx_dbm
	 * @param ordinalSourceDist
	 * @param ordinalDiffDist
	 * @param ordinalLapsDistance
	 */
	protected void runLabsCalculation(final RecordSet recordSet, final int channelNumber, final int ordinalSourceRx_dbm, final int ordinalSmoothRx_dbm, final int ordinalDiffRx_dbm,
			final int ordinalLabsRx_dbm, final int ordinalSourceDist, final int ordinalDiffDist, final int ordinalLapsDistance) {
		// laps calculation init begin
		Record recordSourceRx_dbm = recordSet.get(ordinalSourceRx_dbm);
		Record recordSmoothRx_dbm = recordSet.get(ordinalSmoothRx_dbm);
		Record recordDiffRx_dbm = recordSet.get(ordinalDiffRx_dbm);
		Record recordLapsRx_dbm = recordSet.get(ordinalLabsRx_dbm);
		Record recordDistanceStart = recordSet.get(ordinalSourceDist);
		Record recordDiffDistance = recordSet.get(ordinalDiffDist);
		Record recordLapsDistance = recordSet.get(ordinalLapsDistance);
		// adjustable variables
		int absorptionLevel = 70;
		long filterStartTime = 0;// wait 15 seconds before starting lab counting
		long filterMaxTime = 300000;// 300 seconds = 5 min window for lab counting
		long filterLapMinTime_ms = 5000; // 5 seconds time minimum time space between laps
		int filterMinDeltaRxDbm = 3;
		int filterMinDeltaDist = 20;
		if (this.getMeasurementPropertyValue(channelNumber, ordinalLabsRx_dbm, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().length() > 0) {
			try {
				absorptionLevel = Integer.valueOf(this.getMeasurementPropertyValue(channelNumber, ordinalSmoothRx_dbm, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().trim());
			} catch (NumberFormatException e) {
				// ignore and use intial value
			}
			try {
				filterStartTime = 1000 * Integer.valueOf(this.getMeasurementPropertyValue(channelNumber, ordinalDiffRx_dbm, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().trim());
			} catch (NumberFormatException e) {
				// ignore and use intial value
			}
			try {
				filterMaxTime = 1000 * Integer.valueOf(this.getMeasurementPropertyValue(channelNumber, ordinalSourceRx_dbm, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().trim());
			} catch (NumberFormatException e) {
				// ignore and use intial value
			}
			try {
				filterLapMinTime_ms = 1000 * Integer.valueOf(this.getMeasurementPropertyValue(channelNumber, ordinalLabsRx_dbm, MeasurementPropertyTypes.FILTER_FACTOR.value()).toString().trim());
			} catch (NumberFormatException e) {
				// ignore and use intial value
			}
			try {
				filterMinDeltaRxDbm = Integer.valueOf(this.getMeasurementPropertyValue(channelNumber, ordinalDiffRx_dbm, MeasurementPropertyTypes.NONE_SPECIFIED.value()).toString().trim());
			} catch (NumberFormatException e) {
				// ignore and use intial value
			}
			try {
				filterMinDeltaDist = Integer.valueOf(this.getMeasurementPropertyValue(channelNumber, ordinalDiffDist, MeasurementPropertyTypes.NONE_SPECIFIED.value()).toString().trim());
			} catch (NumberFormatException e) {
				// ignore and use intial value
			}
		}
		if (recordSourceRx_dbm != null && recordSmoothRx_dbm != null && recordDiffRx_dbm != null && recordLapsRx_dbm != null) {
			// temporary variables
			double lastLapTimeStamp_ms = 0;
			int lapTime = 0;
			int lastRxDbmValue = 0;
			int lapCount = 0;
			int lastRxdbm = 0;
			boolean isLapEvent = false;
			int localRxDbmMin = 0;

			// prepare smoothed Rx dbm
			for (int i = 0; i < recordSourceRx_dbm.realSize(); ++i) {
				if (recordSourceRx_dbm.get(i) == 0)
					recordSmoothRx_dbm.set(i, lastRxdbm);
				else
					recordSmoothRx_dbm.set(i, (lastRxdbm * absorptionLevel + recordSourceRx_dbm.get(i)) / (absorptionLevel + 1));
				lastRxdbm = recordSmoothRx_dbm.get(i);

			}
			// smooth and calculate differentiation
			CalculationThread thread = new LinearRegression(recordSet, recordSmoothRx_dbm.getName(), recordDiffRx_dbm.getName(), 2);
			thread.start();
			try {
				thread.join();
			} catch (InterruptedException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}

			for (int i = 0; i < recordDiffRx_dbm.realSize(); ++i) {
				if (recordDiffRx_dbm.getTime_ms(i) > filterStartTime && recordDiffRx_dbm.getTime_ms(i) < (filterStartTime + filterMaxTime)) { //check start time before starting lab counting

					if ((recordDiffRx_dbm.getTime_ms(i) - lastLapTimeStamp_ms) > filterLapMinTime_ms) { // check minimal time between lap events

						if ((recordSmoothRx_dbm.get(i) / 1000 - localRxDbmMin) > filterMinDeltaRxDbm) { // check minimal Rx dbm difference

							if (lastRxDbmValue > 0 && recordDiffRx_dbm.get(i) <= 0) { // lap event detected
								isLapEvent = true;
								if (lastLapTimeStamp_ms != 0) {
									log.log(Level.FINE, String.format("Lap time in sec %03.1f", (recordSet.getTime_ms(i) - lastLapTimeStamp_ms) / 1000.0)); //$NON-NLS-1$
									lapTime = (int) (recordSet.getTime_ms(i) - lastLapTimeStamp_ms);
								}
								lastLapTimeStamp_ms = recordSet.getTime_ms(i);
								recordLapsRx_dbm.set(i, lapTime);
								if (lapTime != 0) {
									if (lapCount % 2 == 0) {
										recordSet.setRecordSetDescription(recordSet.getRecordSetDescription() + String.format(Locale.ENGLISH, "\n%02d  %.1f sec", ++lapCount, lapTime / 1000.0)); //$NON-NLS-1$
									} else {
										recordSet.setRecordSetDescription(recordSet.getRecordSetDescription() + String.format(Locale.ENGLISH, "  -   %02d  %.1f sec", ++lapCount, lapTime / 1000.0)); //$NON-NLS-1$
									}
								}
								if (isLapEvent && lapTime == 0) { // first lap start
									recordLapsRx_dbm.set(i, (int) filterLapMinTime_ms / 2);
								}

								localRxDbmMin = 0; // reset local min value of Rx dbm
							} // end lap event detected
							else if (lapTime == 0)
								if (isLapEvent)
									recordLapsRx_dbm.set(i, (int) filterLapMinTime_ms / 2);
								else
									recordLapsRx_dbm.set(i, (int) filterLapMinTime_ms);
							else
								recordLapsRx_dbm.set(i, lapTime);
						} // end check minimal Rx dbm difference
						else if (lapTime == 0)
							if (isLapEvent)
								recordLapsRx_dbm.set(i, (int) filterLapMinTime_ms / 2);
							else
								recordLapsRx_dbm.set(i, (int) filterLapMinTime_ms);
						else
							recordLapsRx_dbm.set(i, lapTime);
					} // end check minimal time between lap events
					else if (lapTime == 0) {
						if (isLapEvent)
							recordLapsRx_dbm.set(i, (int) filterLapMinTime_ms / 2);
						else
							recordLapsRx_dbm.set(i, (int) filterLapMinTime_ms);
					}
					else {
						try {
							recordLapsRx_dbm.set(i, lapTime);
						}
						catch (ArrayIndexOutOfBoundsException e) {
							log.log(Level.SEVERE, String.format("index %d out of range in recordLapsRx_dbm with length %d", i, recordLapsRx_dbm.realSize()));
						}
					}

					// find a local minimal value of Rx dbm
					if (lastRxDbmValue < 0 && recordDiffRx_dbm.get(i) >= 0) { // local minimum Rx dbm detected
						if (recordSmoothRx_dbm.get(i) / 1000 < localRxDbmMin) localRxDbmMin = recordSmoothRx_dbm.get(i) / 1000;
					}
				} // end check start time before starting lab counting
				else if (recordDiffRx_dbm.getTime_ms(i) > (filterStartTime + filterMaxTime))
					recordLapsRx_dbm.set(i, 0);
				else
					recordLapsRx_dbm.set(i, lapTime);

				lastRxDbmValue = recordDiffRx_dbm.get(i);
			}
			// labs calculation end
		}
		if (recordDistanceStart != null && recordDistanceStart.hasReasonableData() && recordDiffDistance != null && recordLapsDistance != null) {
			// temporary variables
			double lastLapTimeStamp_ms = 0;
			int lapTime = 0;
			int lastDistanceValue = 0;
			int lapCount = 0;
			boolean isLapEvent = false;
			int localDistMax = 0;

			// smooth and calculate differentiation
			CalculationThread thread = new LinearRegression(recordSet, recordDistanceStart.getName(), recordDiffDistance.getName(), 4);
			thread.start();
			try {
				thread.join();
			} catch (InterruptedException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}

			for (int i = 0; i < recordDiffDistance.realSize(); ++i) {
				if (recordDiffDistance.getTime_ms(i) > filterStartTime && recordDiffDistance.getTime_ms(i) < (filterStartTime + filterMaxTime)) { //check start time before starting lab counting

					if ((recordDiffDistance.getTime_ms(i) - lastLapTimeStamp_ms) > filterLapMinTime_ms) { // check minimal time between lap events

						if ((localDistMax - recordDistanceStart.get(i) / 1000) > filterMinDeltaDist) { // check minimal distance difference

							if (lastDistanceValue < 0 && recordDiffDistance.get(i) >= 0) { // lap event detected
								isLapEvent = true;
								if (lastLapTimeStamp_ms != 0) {
									log.log(Level.FINE, String.format("Lap time in sec %03.1f", (recordSet.getTime_ms(i) - lastLapTimeStamp_ms) / 1000.0)); //$NON-NLS-1$
									lapTime = (int) (recordSet.getTime_ms(i) - lastLapTimeStamp_ms);
								}
								lastLapTimeStamp_ms = recordSet.getTime_ms(i);
								recordLapsDistance.set(i, lapTime);
								if (lapTime != 0) {
									if (lapCount % 2 == 0) {
										recordSet.setRecordSetDescription(recordSet.getRecordSetDescription() + String.format(Locale.ENGLISH, "\n%02d  %.1f sec", ++lapCount, lapTime / 1000.0)); //$NON-NLS-1$
									} else {
										recordSet.setRecordSetDescription(recordSet.getRecordSetDescription() + String.format(Locale.ENGLISH, "  -   %02d  %.1f sec", ++lapCount, lapTime / 1000.0)); //$NON-NLS-1$
									}
								}
								if (isLapEvent && lapTime == 0) // first lap start
									recordLapsDistance.set(i, (int) filterLapMinTime_ms / 2);

								localDistMax = 0; // reset local distance maximum
							} // end lap event detected
							else if (lapTime == 0)
								if (isLapEvent)
									recordLapsDistance.set(i, (int) filterLapMinTime_ms / 2);
								else
									recordLapsDistance.set(i, (int) filterLapMinTime_ms);
							else
								recordLapsDistance.set(i, lapTime);
						} // end check minimal distance difference
						else if (lapTime == 0)
							if (isLapEvent)
								recordLapsDistance.set(i, (int) filterLapMinTime_ms / 2);
							else
								recordLapsDistance.set(i, (int) filterLapMinTime_ms);
						else
							recordLapsDistance.set(i, lapTime);
					} // end check minimal time between lap events
					else if (lapTime == 0)
						if (isLapEvent)
							recordLapsDistance.set(i, (int) filterLapMinTime_ms / 2);
						else
							recordLapsDistance.set(i, (int) filterLapMinTime_ms);
					else
						recordLapsDistance.set(i, lapTime);

					// find local distance maximum
					if (lastDistanceValue > 0 && recordDiffDistance.get(i) <= 0) { // local maximum distance detected
						if (recordDistanceStart.get(i) / 1000 > localDistMax) localDistMax = recordDistanceStart.get(i) / 1000;
					}
				} // end check start time before starting lab counting
				else if (recordDiffDistance.getTime_ms(i) > (filterStartTime + filterMaxTime))
					recordLapsDistance.set(i, 0);
				else
					recordLapsDistance.set(i, lapTime);

				lastDistanceValue = recordDiffDistance.get(i);
			}
		}
		recordSet.setSaved(true); // adding description will set unsaved reason
	}

	public PickerParameters getPickerParameters() {
		return this.pickerParameters;
	}

	/**
	 * @param sensorSignature is a csv list of valid sensor values (i.e. sensor names)
	 * @return an integer value with bits 0 to 31 representing the sensor type ordinal numbers (true if the sensor type is active)
	 */
	@Override
	public BitSet getActiveSensors(String sensorSignature) {
		EnumSet<Sensor> sensors = Sensor.getSetFromSignature(sensorSignature);
		return Sensor.getSensors(sensors);
	}
	
	/**
	 * get the measurement ordinal of altitude, speed and trip length
	 * @return empty integer array if device does not fulfill complete requirement
	 */
	@Override
	public int[] getAtlitudeTripSpeedOrdinals() { 
		switch (this.application.getActiveChannelNumber()) {
		case 3: //GPS = 3; 0=RXSQ, 1=Latitude, 2=Longitude, 3=Altitude, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=Distance, 8=Direction, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
			return new int[] { 3, 9, 6 };
		default:
			return new int[0];
		}
	}  
}
