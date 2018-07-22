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

    Copyright (c) 2011,2012,2013,2014,2015,2016,2017,2018 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.istack.internal.Nullable;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;

/**
 * Class to read Graupner HoTT binary data as saved on SD-Cards
 *
 * @author Winfried Brügmann
 */
public class HoTTbinReader {
	final static String													$CLASS_NAME									= HoTTbinReader.class.getName();
	final static Logger													log													= Logger.getLogger(HoTTbinReader.$CLASS_NAME);
	// 64 byte = 0.01 seconds for 40 seconds maximum sensor scan time (40 / 0.01 = 4000)
	protected static final int									LOG_RECORD_SCAN_START				= 4000;
	protected static final int									NUMBER_LOG_RECORDS_TO_SCAN	= 1500;
	protected static final int									NUMBER_LOG_RECORDS_MIN			= 7000;

	final static DataExplorer										application									= DataExplorer.getInstance();
	final static Channels												channels										= Channels.getInstance();

	static int																	dataBlockSize								= 64;
	static byte[]																buf;
	static byte[]																buf0, buf1, buf2, buf3, buf4, buf5, buf6, buf7, buf8, buf9, bufA, bufB, bufC, bufD;
	static long																	timeStep_ms;
	static int[]																pointsReceiver, pointsEAM, pointsVario, pointsGPS, pointsChannel, pointsESC;
	static int[]																pointsGAM;
	static RecordSet														recordSetReceiver, recordSetGAM, recordSetEAM, recordSetVario, recordSetGPS, recordSetChannel,
			recordSetESC;
	static int																	tmpVoltageRx								= 0;
	static int																	tmpTemperatureRx						= 0;
	static int																	tmpHeight										= 0;
	static int																	tmpTemperatureFet						= 0;
	static int																	tmpTemperatureExt						= 0;
	static int																	tmpVoltage									= 0;
	static int																	tmpCurrent									= 0;
	static int																	tmpRevolution								= 0;
	static int																	tmpClimb1										= 0;
	static int																	tmpClimb3										= 0;
	static int																	tmpClimb10									= 0;
	static int																	tmpVoltage1									= 0;
	static int																	tmpVoltage2									= 0;
	static int																	tmpCapacity									= 0;
	static int																	tmpVelocity									= 0;
	static int																	tmpLatitude									= 0;
	static int																	tmpLatitudeDelta						= 0;
	static double																latitudeTolerance						= 1;
	static long																	lastLatitudeTimeStep				= 0;
	static int																	tmpLongitude								= 0;
	static int																	tmpLongitudeDelta						= 0;
	static double																longitudeTolerance					= 1;
	static long																	lastLongitudeTimeStep				= 0;
	static int																	countLostPackages						= 0;
	static boolean															isJustParsed								= false;
	static boolean															isReceiverOnly							= false;
	static StringBuilder												sensorSignature;
	static boolean															isTextModusSignaled					= false;
	static int																	oldProtocolCount						= 0;
	static Vector<Byte>													blockSequenceCheck;

	static ReverseChannelPackageLossStatistics	lostPackages								= new ReverseChannelPackageLossStatistics();

	/**
	 * Individual settings for the SD Log container format ('GRAUPNER SD LOG').
	 * @author Thomas Eickert (USER)
	 */
	public static class SdLogFormat {
		private final String	starterText	= "GRAUPNER SD LOG";
		private final int			headerSize;
		private final int			footerSize;
		private final int			dataBlockSize;

		public SdLogFormat(int headerSize, int footerSize, int dataBlockSize) {
			this.headerSize = headerSize;
			this.footerSize = footerSize;
			this.dataBlockSize = dataBlockSize;
		}
	}

	/**
	 * Adapt a data input stream to a SD Log container format ('GRAUPNER SD LOG').
	 * Check the format basics.
	 * @author Thomas Eickert (USER)
	 */
	public static class SdLogInputStream extends FilterInputStream {

		private final SdLogFormat	sdLogFormat;
		private final long				payloadSize;
		private final long posMax;
	  private long pos = 0;
	  private long mark = 0;

		/**
		 * Create a <code>FilterInputStream</code> respecting the header and footer bytes of a SD log file.
		 * Check the file format.
		 * @param in is a markSupported input stream
		 * @param fileLength is the length of the file which is represented by the <code>in</code> stream
		 */
		public SdLogInputStream(FilterInputStream in, long fileLength, SdLogFormat sdLogFormat) throws DataTypeException, IOException {
			super(in);
			this.sdLogFormat = sdLogFormat;
			posMax = fileLength - sdLogFormat.footerSize - 1;
			payloadSize = fileLength - sdLogFormat.headerSize - sdLogFormat.footerSize;
			if (payloadSize < 0) throw new DataTypeException("file size less than header/footer size");

			if (payloadSize % sdLogFormat.dataBlockSize != 0) throw new DataTypeException("data block size does not match");

			if (!in.markSupported()) throw new DataTypeException("mark/reset not supported");

			byte[] buffer = skipHeader();
			if (!new String(buffer).startsWith(sdLogFormat.starterText)) throw new DataTypeException("starter text does not match");

			if (!verifyHoTTFormat()) throw new DataTypeException("data block counter does not match");
		}

		protected boolean verifyHoTTFormat() throws IOException {
			mark(1 + this.sdLogFormat.dataBlockSize * 4);
			byte[] buffer;
			boolean isHoTT = true;
			buffer = new byte[this.sdLogFormat.dataBlockSize];
			for (int i = 0; i < 4; i++) {
				read(buffer);
				if (buffer[0] != i + 1) {
					isHoTT = false;
					break;
				}
			}
			reset();
			return isHoTT;
		}

		@Override
		public synchronized int read() throws IOException {
			if (posMax - pos <= 0) return -1;
			int b = in.read();
			if (b >= 0) pos += 1;
			return b;
		}

		@Override
		public int read(byte b[]) throws IOException {
			return read(b, 0, b.length);
		}

		@Override
		public int read(byte b[], int off, int len) throws IOException {
			long r = posMax - pos;
			if (r <= 0) return -1;
			int i = in.read(b, off, len);
			if (i > r ) {
				i = (int) r;
			}
			if (i > 0) pos += i;
			return i;
		}

		@Override
		public synchronized long skip(long n) throws IOException {
			long i = in.skip(n);
			if (i > 0) {
				pos += i;
			}
			return i;
		}

		@Override
		public synchronized void mark(int readlimit) {
			in.mark(readlimit);
			mark = pos;
		}

		@Override
		public synchronized void reset() throws IOException {
			in.reset();
			pos = mark;
		}

		private byte[] skipHeader() throws IOException {
			byte[] buffer = new byte[sdLogFormat.headerSize];
			in.read(buffer);
			return buffer;
		}

		/**
		 * @return the number of bytes of the embedded payload data
		 */
		public long getPayloadSize() {
			return this.payloadSize;
		}

	}

	/**
	 * get data file info data
	 *
	 * @return hash map containing header data as string accessible by public header keys
	 * @throws IOException
	 * @throws DataTypeException
	 */
	public static HashMap<String, String> getFileInfo(File file) throws IOException, DataTypeException {
		HashMap<String, String> fileInfo = null;
		try (FilterInputStream data_in = new BufferedInputStream(new FileInputStream(file))) {
			fileInfo = HoTTbinReader.getFileInfo(data_in, file.getPath(), file.length());
		}
		return fileInfo;
	}

	/**
	 * @param data_in is the input stream which is consumed but not closed
	 * @param filePath
	 * @param fileLength
	 * @return a hash map containing header data as string accessible by public header keys
	 */
	@Nullable
	private static HashMap<String, String> getFileInfoLog(FilterInputStream data_in, String filePath, long fileLength)
			throws UnsupportedEncodingException, IOException {
		HashMap<String, String> fileInfo = new HashMap<String, String>();
		fileInfo.put(HoTTAdapter.SD_FORMAT, Boolean.toString(false));
		fileInfo.put(HoTTAdapter.FILE_PATH, filePath);

		data_in.reset();
		data_in.mark(999);
		byte[] buffer = new byte[64];
		data_in.read(buffer);

		// read header size
		String preHeader = new String(buffer);
		int indexOf = preHeader.indexOf("LOG DATA OFFSET : ");
		if (indexOf > 10) {
			int logDataOffset = Integer.valueOf(preHeader.substring(indexOf + 18, indexOf + 18 + 8));

			data_in.reset();

			StringBuilder sb = new StringBuilder();
			String line;
			BufferedReader reader = new BufferedReader(new InputStreamReader(data_in, "ISO-8859-1")); //$NON-NLS-1$
			while ((line = reader.readLine()) != null && sb.append(line).append(GDE.STRING_NEW_LINE).length() < logDataOffset) {
				if (line.contains(": ") && line.indexOf(GDE.STRING_COLON) > 5) {
					String key = line.split(": ")[0].trim();
					String value = null;
					try {
						value = line.split(": ")[1].trim();
					}
					catch (Exception e) {
						// ignore and skip this entry
					}
					if (value != null) {
						fileInfo.put(key, value);
						if (log.isLoggable(Level.FINE))
							log.log(Level.FINE, String.format("%16s : %s", line.split(": ")[0].trim(), fileInfo.get(line.split(": ")[0].trim())));
					}
				}
			}
			int sensorCount;
			sensorCount = fileInfo.get(HoTTAdapter.DETECTED_SENSOR) != null ? fileInfo.get(HoTTAdapter.DETECTED_SENSOR).split(GDE.STRING_COMMA).length - 1 : 0;
			fileInfo.put(HoTTAdapter.SENSOR_COUNT, GDE.STRING_EMPTY + sensorCount);
			fileInfo.put(HoTTAdapter.LOG_COUNT, GDE.STRING_EMPTY + ((fileLength - logDataOffset) / 78));
		}
		return fileInfo;
	}

	/**
	 * Get data file info data.
	 * Set the HoTTbinReader.sensorSignature.
	 * @param data_in is the input stream which is consumed but not closed.
	 * @return a hash map containing header data as string accessible by public header keys
	 */
	public static HashMap<String, String> getFileInfo(FilterInputStream data_in, String filePath, long fileLength) throws IOException, DataTypeException {
		final HashMap<String, String> fileInfo;

		data_in.mark(999);
		byte[] buffer = new byte[64];
		data_in.read(buffer);

		if (filePath.endsWith(GDE.FILE_ENDING_BIN) && new String(buffer).startsWith("GRAUPNER SD LOG")) {
			// begin evaluate for HoTTAdapterX files containing normal HoTT V4 sensor data
			try {
				SdLogFormat sdLogFormat = new SdLogFormat(HoTTbinReaderX.headerSize, HoTTbinReaderX.footerSize, 64);
				SdLogInputStream sdLogInputStream = new SdLogInputStream(data_in, fileLength, sdLogFormat);
				fileInfo = getFileInfo(sdLogInputStream, filePath, fileLength);
			} catch (DataTypeException e) {
				// includes diverse format exceptions
			}
			HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2410));
			throw new DataTypeException(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2410));
		}
		else if (filePath.endsWith(GDE.FILE_ENDING_LOG) && new String(buffer).startsWith("FILE TAG IDVER")) {
			fileInfo = getFileInfoLog(data_in, filePath, fileLength);
			if (fileInfo.get(HoTTAdapter.DETECTED_SENSOR) != null)
				HoTTbinReader.sensorSignature.append(fileInfo.get(HoTTAdapter.DETECTED_SENSOR).substring(9));
		}
		else if (filePath.endsWith(GDE.FILE_ENDING_LOG)) {
			HoTTbinReader.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGW0021));
			throw new DataTypeException(Messages.getString(MessageIds.GDE_MSGW0021));
		} else { // *.log do not need a sensor scan.
			long numberLogs = (fileLength / 64);
			if (numberLogs < NUMBER_LOG_RECORDS_MIN) {
				HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2406));
			}
			else if (numberLogs < LOG_RECORD_SCAN_START + NUMBER_LOG_RECORDS_TO_SCAN) {
				HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2407));
				throw new IOException(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2407));
			}
			fileInfo = getFileInfoBin(data_in, filePath, fileLength);
		}
		return fileInfo;
	}

	private static HashMap<String, String> getFileInfoBin(FilterInputStream data_in, String filePath, long fileLength) throws IOException {
		for (int i = 0; i < HoTTAdapter.isSensorType.length; i++) {
			HoTTAdapter.isSensorType[i] = false;
		}

		HashMap<String, String> fileInfo;
		int sensorCount = 0;
		HoTTbinReader.sensorSignature = new StringBuilder().append(GDE.STRING_LEFT_BRACKET).append(HoTTAdapter.Sensor.RECEIVER.name()).append(GDE.STRING_COMMA);

		fileInfo = new HashMap<String, String>();
		fileInfo.put(HoTTAdapter.SD_FORMAT, Boolean.toString(data_in instanceof SdLogInputStream));
		fileInfo.put(HoTTAdapter.FILE_PATH, filePath);

		data_in.reset();
		byte[] buffer = new byte[64];
		data_in.read(buffer);
		long position = (fileLength / 2) - ((NUMBER_LOG_RECORDS_TO_SCAN * 64) / 2);
		position = position - position % 64;
		if (position <= 0) {
			sensorCount = 1;
		}
		else {
			position = position <= 64 ? 64 : position;
			data_in.skip(position - 64);
			for (int i = 0; i < NUMBER_LOG_RECORDS_TO_SCAN && data_in.available() >= 64; i++) {
				data_in.read(buffer);
				if (HoTTbinReader.log.isLoggable(Level.FINER)) {
					HoTTbinReader.log.log(Level.FINER, StringHelper.byte2Hex4CharString(buffer, buffer.length));
					HoTTbinReader.log.log(Level.FINER, String.format("SensorByte  %02X", buffer[7]));
				}

				switch (buffer[7]) {
				case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
					if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.VARIO.ordinal()] == false) HoTTbinReader.sensorSignature.append(HoTTAdapter.Sensor.VARIO.name()).append(GDE.STRING_COMMA);
					HoTTAdapter.isSensorType[HoTTAdapter.Sensor.VARIO.ordinal()] = true;
					break;
				case HoTTAdapter.SENSOR_TYPE_GPS_19200:
					if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GPS.ordinal()] == false) HoTTbinReader.sensorSignature.append(HoTTAdapter.Sensor.GPS.name()).append(GDE.STRING_COMMA);
					HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GPS.ordinal()] = true;
					break;
				case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
					if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()] == false) HoTTbinReader.sensorSignature.append(HoTTAdapter.Sensor.GAM.name()).append(GDE.STRING_COMMA);
					HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()] = true;
					break;
				case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
					if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.EAM.ordinal()] == false) HoTTbinReader.sensorSignature.append(HoTTAdapter.Sensor.EAM.name()).append(GDE.STRING_COMMA);
					HoTTAdapter.isSensorType[HoTTAdapter.Sensor.EAM.ordinal()] = true;
					break;
				case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
					if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()] == false) HoTTbinReader.sensorSignature.append(HoTTAdapter.Sensor.ESC.name()).append(GDE.STRING_COMMA);
					HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()] = true;
					break;
				}
			}
			for (boolean element : HoTTAdapter.isSensorType) {
				if (element == true)
					++sensorCount;
			}
		}
		fileInfo.put(HoTTAdapter.SENSOR_COUNT, GDE.STRING_EMPTY + sensorCount);
		fileInfo.put(HoTTAdapter.LOG_COUNT, GDE.STRING_EMPTY + (fileLength / 64));
		HoTTbinReader.sensorSignature.deleteCharAt(HoTTbinReader.sensorSignature.length() - 1).append(GDE.STRING_RIGHT_BRACKET);
		if (HoTTbinReader.log.isLoggable(Level.FINE))
			for (Entry<String, String> entry : fileInfo.entrySet()) {
				HoTTbinReader.log.log(Level.FINE, entry.getKey() + " = " + entry.getValue());
				HoTTbinReader.log.log(Level.FINE, Paths.get(filePath).getFileName().toString() + " - " + "sensor count = " + sensorCount);
			}
		return fileInfo;
	}

	// 0=Rx dbm to Strength lookup table
	static int[] lookup = new int[] { 95, 95, 95, 95, 95, 95, 95, 95, 95, 95, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 85, 85, 85, 85, 85, 80, 75, 70, 65, 60, 55, 50, 45, 40, 35, 30, 30, 25, 25, 20, 20,
			20, 15, 15, 10, 10, 5, 5, 5, 5, 5, 5, 0, 0, 0, 0, 0, 0, 0 };

	/**
	 * convert from Rx dbm to strength using lookup table
	 *
	 * @param inValue
	 * @return
	 */
	static int convertRxDbm2Strength(int inValue) {
		if (inValue >= 40 && inValue < HoTTbinReader.lookup.length + 40) {
			return HoTTbinReader.lookup[inValue - 40];
		}
		else if (inValue < 40)
			return 100;
		else
			return 0;
	}

	/**
	 * read complete file data and display the first found record set
	 *
	 * @param filePath
	 * @throws Exception
	 */
	public static synchronized void read(String filePath) throws Exception {
		HashMap<String, String> header = getFileInfo(new File(filePath));

		if (Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) <= 1) {
			HoTTbinReader.isReceiverOnly = Integer.parseInt(header.get(HoTTAdapter.SENSOR_COUNT)) == 0;
			readSingle(new File(filePath));
		} else {
			readMultiple(new File(filePath));
		}
	}

	/**
	 * read log data according to version 0
	 *
	 * @param file
	 * @throws IOException
	 * @throws DataInconsitsentException
	 */
	static void readSingle(File file) throws IOException, DataInconsitsentException {
		final String $METHOD_NAME = "readSingle";
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		HoTTAdapter device = (HoTTAdapter) HoTTbinReader.application.getActiveDevice();
		int recordSetNumber = HoTTbinReader.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		HoTTbinReader.recordSetReceiver = null; // 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=UminRx 9=Event Rx
		HoTTbinReader.recordSetGAM = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		HoTTbinReader.recordSetEAM = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2, 27=Revolution
		HoTTbinReader.recordSetVario = null; // 0=RXSQ, 1=Height, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx 7=Event Vario
		HoTTbinReader.recordSetGPS = null; // 0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		HoTTbinReader.recordSetChannel = null; // 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16
		HoTTbinReader.recordSetESC = null; // 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature
		HoTTbinReader.pointsReceiver = new int[10];
		HoTTbinReader.pointsGAM = new int[26];
		HoTTbinReader.pointsEAM = new int[31];
		HoTTbinReader.pointsVario = new int[8];
		HoTTbinReader.pointsVario[2] = 100000;
		HoTTbinReader.pointsGPS = new int[15];
		HoTTbinReader.pointsChannel = new int[23];
		HoTTbinReader.pointsESC = new int[14];
		HoTTbinReader.timeStep_ms = 0;
		HoTTbinReader.dataBlockSize = 64;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		HoTTbinReader.buf0 = new byte[30];
		HoTTbinReader.buf1 = null;
		HoTTbinReader.buf2 = null;
		HoTTbinReader.buf3 = null;
		HoTTbinReader.buf4 = null;
		int version = -1;
		HoTTAdapter.reverseChannelPackageLossCounter.clear();
		HoTTbinReader.lostPackages.clear();
		HoTTbinReader.countLostPackages = 0;
		HoTTbinReader.isJustParsed = false;
		HoTTbinReader.isTextModusSignaled = false;
		boolean isWrongDataBlockNummerSignaled = false;
		int countPackageLoss = 0;
		long numberDatablocks = fileSize / HoTTbinReader.dataBlockSize;
		long startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks);
		numberDatablocks = HoTTbinReader.isReceiverOnly && !HoTTAdapter.isChannelsChannelEnabled ? numberDatablocks/10 : numberDatablocks;
		String date = StringHelper.getDate();
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		MenuToolBar menuToolBar = HoTTbinReader.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		GDE.getUiNotification().setProgress(0);

		try {
			HoTTAdapter.recordSets.clear();
			// receiver data are always contained
			// check if recordSetReceiver initialized, transmitter and receiver
			// data always present, but not in the same data rate and signals
			channel = HoTTbinReader.channels.get(1);
			channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
			recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.RECEIVER.value() + recordSetNameExtend;
			HoTTbinReader.recordSetReceiver = RecordSet.createRecordSet(recordSetName, device, 1, true, true, true);
			channel.put(recordSetName, HoTTbinReader.recordSetReceiver);
			HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.RECEIVER.value(), HoTTbinReader.recordSetReceiver);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (GDE.isWithUi()) {
				channel.applyTemplate(recordSetName, false);
			}
			// recordSetReceiver initialized and ready to add data

			if (HoTTAdapter.isChannelsChannelEnabled) {
				// channel data are always contained
				// check if recordSetChannel initialized, transmitter and
				// receiver data always present, but not in the same data rate
				// and signals
				channel = HoTTbinReader.channels.get(6);
				channel.setFileDescription(HoTTbinReader.application.isObjectoriented() ? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
				recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.CHANNEL.value() + recordSetNameExtend;
				HoTTbinReader.recordSetChannel = RecordSet.createRecordSet(recordSetName, device, 6, true, true, true);
				channel.put(recordSetName, HoTTbinReader.recordSetChannel);
				HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.CHANNEL.value(), HoTTbinReader.recordSetChannel);
				tmpRecordSet = channel.get(recordSetName);
				tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT
						+ Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
				tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
				if (GDE.isWithUi()) {
					channel.applyTemplate(recordSetName, false);
				}
				// recordSetChannel initialized and ready to add data
			}

			// read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTbinReader.log.isLoggable(Level.FINE) && i % 10 == 0) {
					HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
					HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if (!HoTTAdapter.isFilterTextModus || (HoTTbinReader.buf[6] & 0x01) == 0) { // switch into text modus
					if (HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { // buf 3, 4, tx,rx
						if (HoTTbinReader.log.isLoggable(Level.FINE)) HoTTbinReader.log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", HoTTbinReader.buf[7], HoTTbinReader.buf[33]));

						HoTTAdapter.reverseChannelPackageLossCounter.add(1);
						HoTTbinReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
						// create and fill sensor specific data record sets
						if (HoTTbinReader.log.isLoggable(Level.FINER)) HoTTbinReader.log.logp(Level.FINER, HoTTbinReader.$CLASS_NAME, $METHOD_NAME,
								StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

						// fill receiver data
						if (HoTTbinReader.buf[33] == 0 && (HoTTbinReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader.buf, 40) >= 0) {
							parseAddReceiver(HoTTbinReader.buf);
						}
						if (HoTTAdapter.isChannelsChannelEnabled) {
							parseAddChannel(HoTTbinReader.buf);
						}
						if (HoTTbinReader.isReceiverOnly && !HoTTAdapter.isChannelsChannelEnabled) {
							for (int j = 0; j < 9; j++) {
								data_in.read(HoTTbinReader.buf);
								HoTTbinReader.timeStep_ms += 10;
							}
						}
						// fill data block 0 receiver voltage an temperature
						if (HoTTbinReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader.buf, 0) != 0) {
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf0, 0, HoTTbinReader.buf0.length);
						}
						HoTTbinReader.timeStep_ms += 10;// add default time step from device of 10 msec
						// log.log(Level.OFF, "sensor type ID = " + StringHelper.byte2Hex2CharString(new byte[] {(byte) (HoTTbinReader.buf[7] & 0xFF)}, 1));
						if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4) { // expected data block number
							switch ((byte) (HoTTbinReader.buf[7] & 0xFF)) {
							case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
							case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
								if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.VARIO.ordinal()]) {
									// check if recordSetVario initialized, transmitter and receiver data always present, but not in the same data rate and signals
									if (HoTTbinReader.recordSetVario == null) {
										channel = HoTTbinReader.channels.get(2);
										channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
											: date);
									recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK
											+ HoTTAdapter.Sensor.VARIO.value() + recordSetNameExtend;
										HoTTbinReader.recordSetVario = RecordSet.createRecordSet(recordSetName, device, 2, true, true, true);
										channel.put(recordSetName, HoTTbinReader.recordSetVario);
										HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.VARIO.value(), HoTTbinReader.recordSetVario);
										tmpRecordSet = channel.get(recordSetName);
									  tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT
											+ Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
										tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
										if (GDE.isWithUi()) {
											channel.applyTemplate(recordSetName, false);
										}
									}
									// recordSetVario initialized and ready to add data
									// fill data block 1 to 2
									if (HoTTbinReader.buf[33] == 1) {
										HoTTbinReader.buf1 = new byte[30];
										System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf1, 0, HoTTbinReader.buf1.length);
									}
									if (HoTTbinReader.buf[33] == 2) {
										HoTTbinReader.buf2 = new byte[30];
										System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf2, 0, HoTTbinReader.buf2.length);
									}
									if (HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null) {
										version = parseAddVario(version, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2);
										HoTTbinReader.buf1 = HoTTbinReader.buf2 = null;
									}
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GPS_115200:
							case HoTTAdapter.SENSOR_TYPE_GPS_19200:
								if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GPS.ordinal()]) {
									// check if recordSetReceiver initialized, transmitter and receiver data always present, but not in the same data rate as signals
									if (HoTTbinReader.recordSetGPS == null) {
										channel = HoTTbinReader.channels.get(3);
										channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
											: date);
										recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GPS.value() + recordSetNameExtend;
										HoTTbinReader.recordSetGPS = RecordSet.createRecordSet(recordSetName, device, 3, true, true, true);
										channel.put(recordSetName, HoTTbinReader.recordSetGPS);
										HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GPS.value(), HoTTbinReader.recordSetGPS);
										tmpRecordSet = channel.get(recordSetName);
										tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
										tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
										if (GDE.isWithUi()) {
											channel.applyTemplate(recordSetName, false);
										}
									}
									// recordSetGPS initialized and ready to add data
									// fill data block 1 to 3
									if (HoTTbinReader.buf1 == null && HoTTbinReader.buf[33] == 1) {
										HoTTbinReader.buf1 = new byte[30];
										System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf1, 0, HoTTbinReader.buf1.length);
									}
									if (HoTTbinReader.buf2 == null && HoTTbinReader.buf[33] == 2) {
										HoTTbinReader.buf2 = new byte[30];
										System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf2, 0, HoTTbinReader.buf2.length);
									}
									if (HoTTbinReader.buf3 == null && HoTTbinReader.buf[33] == 3) {
										HoTTbinReader.buf3 = new byte[30];
										System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf3, 0, HoTTbinReader.buf3.length);
									}
									if (HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null && HoTTbinReader.buf3 != null) {
										parseAddGPS(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3);
										HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = null;
									}
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
							case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
								if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()]) {
									// check if recordSetGeneral initialized, transmitter and receiver data always present, but not in the same data rate and signals
									if (HoTTbinReader.recordSetGAM == null) {
										channel = HoTTbinReader.channels.get(4);
										channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
											: date);
										recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GAM.value() + recordSetNameExtend;
										HoTTbinReader.recordSetGAM = RecordSet.createRecordSet(recordSetName, device, 4, true, true, true);
										channel.put(recordSetName, HoTTbinReader.recordSetGAM);
										HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GAM.value(), HoTTbinReader.recordSetGAM);
										tmpRecordSet = channel.get(recordSetName);
										tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
										tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
										if (GDE.isWithUi()) {
											channel.applyTemplate(recordSetName, false);
										}
									}
									// recordSetGeneral initialized and ready to add data
									// fill data block 1 to 4
									if (HoTTbinReader.buf1 == null && HoTTbinReader.buf[33] == 1) {
										HoTTbinReader.buf1 = new byte[30];
										System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf1, 0, HoTTbinReader.buf1.length);
									}
									if (HoTTbinReader.buf2 == null && HoTTbinReader.buf[33] == 2) {
										HoTTbinReader.buf2 = new byte[30];
										System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf2, 0, HoTTbinReader.buf2.length);
									}
									if (HoTTbinReader.buf3 == null && HoTTbinReader.buf[33] == 3) {
										HoTTbinReader.buf3 = new byte[30];
										System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf3, 0, HoTTbinReader.buf3.length);
									}
									if (HoTTbinReader.buf4 == null && HoTTbinReader.buf[33] == 4) {
										HoTTbinReader.buf4 = new byte[30];
										System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf4, 0, HoTTbinReader.buf4.length);
									}
									if (HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null && HoTTbinReader.buf3 != null && HoTTbinReader.buf4 != null) {
										parseAddGAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
										HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
									}
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
							case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
								if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.EAM.ordinal()]) {
									// check if recordSetGeneral initialized, transmitter and receiver data always present, but not in the same data rate and signals
									if (HoTTbinReader.recordSetEAM == null) {
										channel = HoTTbinReader.channels.get(5);
										channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
											: date);
										recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.EAM.value() + recordSetNameExtend;
										HoTTbinReader.recordSetEAM = RecordSet.createRecordSet(recordSetName, device, 5, true, true, true);
										channel.put(recordSetName, HoTTbinReader.recordSetEAM);
										HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.EAM.value(), HoTTbinReader.recordSetEAM);
										tmpRecordSet = channel.get(recordSetName);
										tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
										tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
										if (GDE.isWithUi()) {
											channel.applyTemplate(recordSetName, false);
										}
									}
									// recordSetElectric initialized and ready to add data
									// fill data block 1 to 4
									if (HoTTbinReader.buf1 == null && HoTTbinReader.buf[33] == 1) {
										HoTTbinReader.buf1 = new byte[30];
										System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf1, 0, HoTTbinReader.buf1.length);
									}
									if (HoTTbinReader.buf2 == null && HoTTbinReader.buf[33] == 2) {
										HoTTbinReader.buf2 = new byte[30];
										System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf2, 0, HoTTbinReader.buf2.length);
									}
									if (HoTTbinReader.buf3 == null && HoTTbinReader.buf[33] == 3) {
										HoTTbinReader.buf3 = new byte[30];
										System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf3, 0, HoTTbinReader.buf3.length);
									}
									if (HoTTbinReader.buf4 == null && HoTTbinReader.buf[33] == 4) {
										HoTTbinReader.buf4 = new byte[30];
										System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf4, 0, HoTTbinReader.buf4.length);
									}
									if (HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null && HoTTbinReader.buf3 != null && HoTTbinReader.buf4 != null) {
										parseAddEAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
										HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
									}
								}
								break;

							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
							case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
								if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()]) {
									// check if recordSetMotorDriver initialized, transmitter and receiver data always present, but not in the same data rate and signals
									if (HoTTbinReader.recordSetESC == null) {
										channel = HoTTbinReader.channels.get(7);
										channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
											? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey()
											: date);
										recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ESC.value() + recordSetNameExtend;
										HoTTbinReader.recordSetESC = RecordSet.createRecordSet(recordSetName, device, 7, true, true, true);
										channel.put(recordSetName, HoTTbinReader.recordSetESC);
										HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.ESC.value(), HoTTbinReader.recordSetESC);
										tmpRecordSet = channel.get(recordSetName);
										tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
										tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
										if (GDE.isWithUi()) {
											channel.applyTemplate(recordSetName, false);
										}
									}
									// recordSetMotorDriver initialized and ready to add data
									// fill data block 1 to 4
									if (HoTTbinReader.buf1 == null && HoTTbinReader.buf[33] == 1) {
										HoTTbinReader.buf1 = new byte[30];
										System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf1, 0, HoTTbinReader.buf1.length);
									}
									if (HoTTbinReader.buf2 == null && HoTTbinReader.buf[33] == 2) {
										HoTTbinReader.buf2 = new byte[30];
										System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf2, 0, HoTTbinReader.buf2.length);
									}
									if (HoTTbinReader.buf3 == null && HoTTbinReader.buf[33] == 3) {
										HoTTbinReader.buf3 = new byte[30];
										System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf3, 0, HoTTbinReader.buf3.length);
									}
									if (HoTTbinReader.buf1 != null && HoTTbinReader.buf2 != null && HoTTbinReader.buf3 != null) {
										parseAddESC(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3);
										HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
									}
								}
								break;
							}
						}
						else {
							if (!isWrongDataBlockNummerSignaled) {
								application.openMessageDialogAsync(String.format("Datenblocknummer ausserhalb des Bereichs von 0 bis 5 (%d)", buf[33]));
								isWrongDataBlockNummerSignaled = true;
							}
							HoTTbinReader.isJustParsed = true;
						}
						if (i % progressIndicator == 0) GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));

						if (HoTTbinReader.isJustParsed && HoTTbinReader.countLostPackages > 0) {
							HoTTbinReader.lostPackages.add(HoTTbinReader.countLostPackages);
							HoTTbinReader.countLostPackages = 0;
							HoTTbinReader.isJustParsed = false;
						}
					}
					else { // skip empty block, but add time step
						if (HoTTbinReader.log.isLoggable(Level.FINE)) HoTTbinReader.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

						HoTTAdapter.reverseChannelPackageLossCounter.add(0);
						HoTTbinReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;

						++countPackageLoss; // add up lost packages in telemetry
						// data
						++HoTTbinReader.countLostPackages;
						// HoTTbinReader.pointsReceiver[0] = (int) (countPackageLoss*100.0 / ((HoTTbinReader.timeStep_ms+10) / 10.0)*1000.0);

						if (HoTTAdapter.isChannelsChannelEnabled) {
							parseAddChannel(HoTTbinReader.buf);
						}

						HoTTbinReader.timeStep_ms += 10;
						// reset buffer to avoid mixing data >> 20 Jul 14, not any longer required due to protocol change requesting next sensor data block
						// HoTTbinReader.buf1 = HoTTbinReader.buf2 = HoTTbinReader.buf3 = HoTTbinReader.buf4 = null;
					}
				}
				else if (!HoTTbinReader.isTextModusSignaled) {
					HoTTbinReader.isTextModusSignaled = true;
					HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			String packageLossPercentage = HoTTbinReader.recordSetReceiver.getRecordDataSize(true) > 0
					? String.format("%.1f", (countPackageLoss / HoTTbinReader.recordSetReceiver.getTime_ms(HoTTbinReader.recordSetReceiver.getRecordDataSize(true) - 1) * 1000))
					: "100";
			HoTTbinReader.recordSetReceiver.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] { countPackageLoss, packageLossPercentage, HoTTbinReader.lostPackages.getStatistics() })
					+ HoTTbinReader.sensorSignature);
			HoTTbinReader.log.logp(Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReader.log.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " //$NON-NLS-1$
					+ StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$

			if (GDE.isWithUi()) {
				for (RecordSet recordSet : HoTTAdapter.recordSets.values()) {
					device.makeInActiveDisplayable(recordSet);

					// write filename after import to record description
					recordSet.descriptionAppendFilename(file.getName());
				}

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				GDE.getUiNotification().setProgress(100);
			}
		}
		finally {
			data_in.close();
			data_in = null;
		}
	}

	/**
	 * compose the record set extend to give capability to identify source of
	 * this record set
	 *
	 * @param file
	 * @return
	 */
	protected static String getRecordSetExtend(File file) {
		return getRecordSetExtend(file.getName());
	}

	/**
	 * compose the record set extend to give capability to identify source of
	 * this record set
	 *
	 * @param fileName
	 * @return
	 */
	protected static String getRecordSetExtend(String fileName) {
		String recordSetNameExtend = GDE.STRING_EMPTY;
		if (fileName.contains(GDE.STRING_UNDER_BAR)) {
			try {
				Integer.parseInt(fileName.substring(0, fileName.lastIndexOf(GDE.STRING_UNDER_BAR)));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.lastIndexOf(GDE.STRING_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (fileName.substring(0, fileName.lastIndexOf(GDE.STRING_UNDER_BAR)).length() <= 8)
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.lastIndexOf(GDE.STRING_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		else {
			try {
				Integer.parseInt(fileName.substring(0, 4));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, 4) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (fileName.substring(0, fileName.length()).length() <= 8 + 4) recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.length() - 4) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		return recordSetNameExtend;
	}

	/**
	 * read log data according to version 0
	 *
	 * @param file
	 * @param data_in
	 * @throws IOException
	 * @throws DataInconsitsentException
	 */
	static void readMultiple(File file) throws IOException, DataInconsitsentException {
		final String $METHOD_NAME = "readMultiple";
		long startTime = System.nanoTime() / 1000000;
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		HoTTAdapter device = (HoTTAdapter) HoTTbinReader.application.getActiveDevice();
		int recordSetNumber = HoTTbinReader.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		HoTTbinReader.recordSetReceiver = null; // 0=RF_RXSQ, 1=RXSQ, 2=Strength, 3=PackageLoss, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=UminRx
		HoTTbinReader.recordSetGAM = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6, 12=Revolution, 13=Altitude, 14=Climb, 15=Climb3, 16=FuelLevel, 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		HoTTbinReader.recordSetEAM = null; // 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance, 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height, 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1, 26=Temperature 2, 27=Revolution
		HoTTbinReader.recordSetVario = null; // 0=RXSQ, 1=Height, 2=Climb 1, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx
		HoTTbinReader.recordSetGPS = null; // 0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripDistance, 10=VoltageRx, 11=TemperatureRx
		HoTTbinReader.recordSetChannel = null; // 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16 19=PowerOff 20=BattLow 21=Reset 22=Warning
		HoTTbinReader.recordSetESC = null; // 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperaure
		HoTTbinReader.pointsReceiver = new int[10];
		HoTTbinReader.pointsGAM = new int[26];
		HoTTbinReader.pointsEAM = new int[31];
		HoTTbinReader.pointsVario = new int[8];
		HoTTbinReader.pointsVario[2] = 100000;
		HoTTbinReader.pointsGPS = new int[15];
		HoTTbinReader.pointsChannel = new int[23];
		HoTTbinReader.pointsESC = new int[14];
		HoTTbinReader.timeStep_ms = 0;
		HoTTbinReader.dataBlockSize = 64;
		HoTTbinReader.buf = new byte[HoTTbinReader.dataBlockSize];
		HoTTbinReader.buf0 = new byte[30];
		HoTTbinReader.buf1 = new byte[30];
		HoTTbinReader.buf2 = new byte[30];
		HoTTbinReader.buf3 = new byte[30];
		HoTTbinReader.buf4 = new byte[30];
		byte actualSensor = -1, lastSensor = -1;
		int logCountVario = 0, logCountGPS = 0, logCountGeneral = 0, logCountElectric = 0, logCountSpeedControl = 0;
		HoTTAdapter.reverseChannelPackageLossCounter.clear();
		HoTTbinReader.lostPackages.clear();
		HoTTbinReader.countLostPackages = 0;
		HoTTbinReader.isJustParsed = false;
		HoTTbinReader.isTextModusSignaled = false;
		int countPackageLoss = 0;
		long numberDatablocks = fileSize / HoTTbinReader.dataBlockSize;
		long startTimeStamp_ms = HoTTbinReader.getStartTimeStamp(file.getName(), file.lastModified(), numberDatablocks);
		String date = StringHelper.getDate();
		String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(startTimeStamp_ms); //$NON-NLS-1$
		RecordSet tmpRecordSet;
		MenuToolBar menuToolBar = HoTTbinReader.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		GDE.getUiNotification().setProgress(0);

		try {
			HoTTAdapter.recordSets.clear();
			// receiver data are always contained
			// check if recordSetReceiver initialized, transmitter and receiver
			// data always present, but not in the same data rate and signals
			channel = HoTTbinReader.channels.get(1);
			channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
					? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
			recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.RECEIVER.value() + recordSetNameExtend;
			HoTTbinReader.recordSetReceiver = RecordSet.createRecordSet(recordSetName, device, 1, true, true, true);
			channel.put(recordSetName, HoTTbinReader.recordSetReceiver);
			HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.RECEIVER.value(), HoTTbinReader.recordSetReceiver);
			tmpRecordSet = channel.get(recordSetName);
			tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
			tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
			if (GDE.isWithUi()) {
				channel.applyTemplate(recordSetName, false);
			}
			// recordSetReceiver initialized and ready to add data
			// channel data are always contained
			if (HoTTAdapter.isChannelsChannelEnabled) {
				// check if recordSetChannel initialized, transmitter and
				// receiver data always present, but not in the same data rate
				// and signals
				channel = HoTTbinReader.channels.get(6);
				channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
						? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
				recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.CHANNEL.value() + recordSetNameExtend;
				HoTTbinReader.recordSetChannel = RecordSet.createRecordSet(recordSetName, device, 6, true, true, true);
				channel.put(recordSetName, HoTTbinReader.recordSetChannel);
				HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.CHANNEL.value(), HoTTbinReader.recordSetChannel);
				tmpRecordSet = channel.get(recordSetName);
				tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
				tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
				if (GDE.isWithUi()) {
					channel.applyTemplate(recordSetName, false);
				}
				// recordSetChannel initialized and ready to add data
			}

			// read all the data blocks from the file and parse
			for (int i = 0; i < numberDatablocks; i++) {
				data_in.read(HoTTbinReader.buf);
				if (HoTTbinReader.log.isLoggable(Level.FINEST) && i % 10 == 0) {
					HoTTbinReader.log.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(HoTTbinReader.buf.length));
					HoTTbinReader.log.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(HoTTbinReader.buf, HoTTbinReader.buf.length));
				}

				if (!HoTTAdapter.isFilterTextModus || (HoTTbinReader.buf[6] & 0x01) == 0) { // switch into text modus
					if (HoTTbinReader.buf[33] >= 0 && HoTTbinReader.buf[33] <= 4 && HoTTbinReader.buf[3] != 0 && HoTTbinReader.buf[4] != 0) { // buf 3, 4, tx,rx
						if (HoTTbinReader.log.isLoggable(Level.FINE)) HoTTbinReader.log.log(Level.FINE, String.format("Sensor %x Blocknummer : %d", HoTTbinReader.buf[7], HoTTbinReader.buf[33]));

						HoTTAdapter.reverseChannelPackageLossCounter.add(1);
						HoTTbinReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;
						// create and fill sensor specific data record sets
						if (HoTTbinReader.log.isLoggable(Level.FINEST)) HoTTbinReader.log.logp(Level.FINEST, HoTTbinReader.$CLASS_NAME, $METHOD_NAME,
								StringHelper.byte2Hex2CharString(new byte[] { HoTTbinReader.buf[7] }, 1) + GDE.STRING_MESSAGE_CONCAT + StringHelper.printBinary(HoTTbinReader.buf[7], false));

						// fill receiver data
						if (HoTTbinReader.buf[33] == 0 && (HoTTbinReader.buf[38] & 0x80) != 128 && DataParser.parse2Short(HoTTbinReader.buf, 40) >= 0) {
							parseAddReceiver(HoTTbinReader.buf);
						}
						if (HoTTAdapter.isChannelsChannelEnabled) {
							parseAddChannel(HoTTbinReader.buf);
						}

						HoTTbinReader.timeStep_ms += 10;// add default time step from log record of 10 msec

						// detect sensor switch
						if (actualSensor == -1)
							lastSensor = actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);
						else
							actualSensor = (byte) (HoTTbinReader.buf[7] & 0xFF);

						if (actualSensor != lastSensor) {
							// write data just after sensor switch
							if (logCountVario >= 3 || logCountGPS >= 4 || logCountGeneral >= 5 || logCountElectric >= 5 || logCountSpeedControl >= 5) {
								switch (lastSensor) {
								case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
								case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
									if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.VARIO.ordinal()]) {
										// check if recordSetVario initialized, transmitter and receiver data always
										// present, but not in the same data rate as signals
										if (HoTTbinReader.recordSetVario == null) {
											channel = HoTTbinReader.channels.get(2);
											channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
													? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
											recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.VARIO.value() + recordSetNameExtend;
											HoTTbinReader.recordSetVario = RecordSet.createRecordSet(recordSetName, device, 2, true, true, true);
											channel.put(recordSetName, HoTTbinReader.recordSetVario);
											HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.VARIO.value(), HoTTbinReader.recordSetVario);
											tmpRecordSet = channel.get(recordSetName);
											tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
											tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
											if (GDE.isWithUi()) {
												channel.applyTemplate(recordSetName, false);
											}
										}
										// recordSetVario initialized and ready to add data
										parseAddVario(1, HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_GPS_115200:
								case HoTTAdapter.SENSOR_TYPE_GPS_19200:
									if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GPS.ordinal()]) {
										// check if recordSetReceiver initialized, transmitter and receiver
										// data always present, but not in the same data rate as signals
										if (HoTTbinReader.recordSetGPS == null) {
											channel = HoTTbinReader.channels.get(3);
											channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
													? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
											recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GPS.value() + recordSetNameExtend;
											HoTTbinReader.recordSetGPS = RecordSet.createRecordSet(recordSetName, device, 3, true, true, true);
											channel.put(recordSetName, HoTTbinReader.recordSetGPS);
											HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GPS.value(), HoTTbinReader.recordSetGPS);
											tmpRecordSet = channel.get(recordSetName);
											tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
											tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
											if (GDE.isWithUi()) {
												channel.applyTemplate(recordSetName, false);
											}
										}
										// recordSetGPS initialized and ready to add data
										parseAddGPS(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
								case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
									if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.GAM.ordinal()]) {
										// check if recordSetGeneral initialized, transmitter and receiver
										// data always present, but not in the same data rate as signals
										if (HoTTbinReader.recordSetGAM == null) {
											channel = HoTTbinReader.channels.get(4);
											channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
													? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
											recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.GAM.value() + recordSetNameExtend;
											HoTTbinReader.recordSetGAM = RecordSet.createRecordSet(recordSetName, device, 4, true, true, true);
											channel.put(recordSetName, HoTTbinReader.recordSetGAM);
											HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.GAM.value(), HoTTbinReader.recordSetGAM);
											tmpRecordSet = channel.get(recordSetName);
											tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
											tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
											if (GDE.isWithUi()) {
												channel.applyTemplate(recordSetName, false);
											}
										}
										// recordSetGeneral initialized and ready to add data
										parseAddGAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
								case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
									if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.EAM.ordinal()]) {
										// check if recordSetGeneral initialized, transmitter and receiver
										// data always present, but not in the same data rate as signals
										if (HoTTbinReader.recordSetEAM == null) {
											channel = HoTTbinReader.channels.get(5);
											channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
													? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
											recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.EAM.value() + recordSetNameExtend;
											HoTTbinReader.recordSetEAM = RecordSet.createRecordSet(recordSetName, device, 5, true, true, true);
											channel.put(recordSetName, HoTTbinReader.recordSetEAM);
											HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.EAM.value(), HoTTbinReader.recordSetEAM);
											tmpRecordSet = channel.get(recordSetName);
											tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
											tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
											if (GDE.isWithUi()) {
												channel.applyTemplate(recordSetName, false);
											}
										}
										// recordSetElectric initialized and ready to add data
										parseAddEAM(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3, HoTTbinReader.buf4);
									}
									break;

								case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
								case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
									if (HoTTAdapter.isSensorType[HoTTAdapter.Sensor.ESC.ordinal()]) {
										// check if recordSetGeneral initialized, transmitter and receiver
										// data always present, but not in the same data rate as signals
										if (HoTTbinReader.recordSetESC == null) {
											channel = HoTTbinReader.channels.get(7);
											channel.setFileDescription(HoTTbinReader.application.isObjectoriented()
													? date + GDE.STRING_BLANK + HoTTbinReader.application.getObjectKey() : date);
											recordSetName = recordSetNumber + GDE.STRING_RIGHT_PARENTHESIS_BLANK + HoTTAdapter.Sensor.ESC.value() + recordSetNameExtend;
											HoTTbinReader.recordSetESC = RecordSet.createRecordSet(recordSetName, device, 7, true, true, true);
											channel.put(recordSetName, HoTTbinReader.recordSetESC);
											HoTTAdapter.recordSets.put(HoTTAdapter.Sensor.ESC.value(), HoTTbinReader.recordSetESC);
											tmpRecordSet = channel.get(recordSetName);
											tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
											tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
											if (GDE.isWithUi()) {
												channel.applyTemplate(recordSetName, false);
											}
										}
										// recordSetElectric initialized and ready to add data
										parseAddESC(HoTTbinReader.buf0, HoTTbinReader.buf1, HoTTbinReader.buf2, HoTTbinReader.buf3);
									}
									break;
								}
							}

							if (HoTTbinReader.log.isLoggable(Level.FINE))
								HoTTbinReader.log.logp(Level.FINE, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "logCountVario = " + logCountVario + " logCountGPS = "
									+ logCountGPS + " logCountGeneral = " + logCountGeneral + " logCountElectric = " + logCountElectric + " logCountMotorDriver = " + logCountSpeedControl);
							lastSensor = actualSensor;
							logCountVario = logCountGPS = logCountGeneral = logCountElectric = logCountSpeedControl = 0;
						}

						switch (lastSensor) {
						case HoTTAdapter.SENSOR_TYPE_VARIO_115200:
						case HoTTAdapter.SENSOR_TYPE_VARIO_19200:
							++logCountVario;
							break;
						case HoTTAdapter.SENSOR_TYPE_GPS_115200:
						case HoTTAdapter.SENSOR_TYPE_GPS_19200:
							++logCountGPS;
							break;
						case HoTTAdapter.SENSOR_TYPE_GENERAL_115200:
						case HoTTAdapter.SENSOR_TYPE_GENERAL_19200:
							++logCountGeneral;
							break;
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_115200:
						case HoTTAdapter.SENSOR_TYPE_ELECTRIC_19200:
							++logCountElectric;
							break;
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_115200:
						case HoTTAdapter.SENSOR_TYPE_SPEED_CONTROL_19200:
							++logCountSpeedControl;
							break;
						}

						// fill data block 0 to 4
						if (HoTTbinReader.buf[33] == 0 && DataParser.parse2Short(HoTTbinReader.buf, 0) != 0) {
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf0, 0, HoTTbinReader.buf0.length);
						}
						else if (HoTTbinReader.buf[33] == 1) {
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf1, 0, HoTTbinReader.buf1.length);
						}
						else if (HoTTbinReader.buf[33] == 2) {
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf2, 0, HoTTbinReader.buf2.length);
						}
						else if (HoTTbinReader.buf[33] == 3) {
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf3, 0, HoTTbinReader.buf3.length);
						}
						else if (HoTTbinReader.buf[33] == 4) {
							System.arraycopy(HoTTbinReader.buf, 34, HoTTbinReader.buf4, 0, HoTTbinReader.buf4.length);
						}

						// if (HoTTbinReader.blockSequenceCheck.size() > 1) {
						// if(HoTTbinReader.blockSequenceCheck.get(0) -
						// HoTTbinReader.blockSequenceCheck.get(1) > 1 &&
						// HoTTbinReader.blockSequenceCheck.get(0) -
						// HoTTbinReader.blockSequenceCheck.get(1) < 4)
						// ++HoTTbinReader.oldProtocolCount;
						// HoTTbinReader.blockSequenceCheck.remove(0);
						// }
						// HoTTbinReader.blockSequenceCheck.add(HoTTbinReader.buf[33]);

						if (i % progressIndicator == 0)
							GDE.getUiNotification().setProgress((int) (i * 100 / numberDatablocks));

						if (HoTTbinReader.isJustParsed && HoTTbinReader.countLostPackages > 0) {
							HoTTbinReader.lostPackages.add(HoTTbinReader.countLostPackages);
							HoTTbinReader.countLostPackages = 0;
							HoTTbinReader.isJustParsed = false;
						}
					}
					else { // tx,rx == 0
						if (HoTTbinReader.log.isLoggable(Level.FINE)) HoTTbinReader.log.log(Level.FINE, "-->> Found tx=rx=0 dBm");

						HoTTAdapter.reverseChannelPackageLossCounter.add(0);
						HoTTbinReader.pointsReceiver[0] = HoTTAdapter.reverseChannelPackageLossCounter.getPercentage() * 1000;

						++countPackageLoss; // add up lost packages in telemetry
						// data
						++HoTTbinReader.countLostPackages;
						// HoTTbinReader.pointsReceiver[0] = (int) (countPackageLoss*100.0 / ((HoTTbinReader2.timeStep_ms+10) / 10.0)*1000.0);

						if (HoTTAdapter.isChannelsChannelEnabled) {
							parseAddChannel(HoTTbinReader.buf);
						}

						HoTTbinReader.timeStep_ms += 10;
						// reset buffer to avoid mixing data
						// logCountVario = logCountGPS = logCountGeneral = logCountElectric = logCountSpeedControl = 0;
					}
				}
				else if (!HoTTbinReader.isTextModusSignaled) {
					HoTTbinReader.isTextModusSignaled = true;
					HoTTbinReader.application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2404));
				}
			}
			// if (HoTTbinReader.oldProtocolCount > 2) {
			// application.openMessageDialogAsync(Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGW2405,
			// new Object[] { HoTTbinReader.oldProtocolCount }));
			// }
			String packageLossPercentage = HoTTbinReader.recordSetReceiver.getRecordDataSize(true) > 0
					? String.format("%.1f", (countPackageLoss / HoTTbinReader.recordSetReceiver.getTime_ms(HoTTbinReader.recordSetReceiver.getRecordDataSize(true) - 1) * 1000)) : "100";
			HoTTbinReader.recordSetReceiver.setRecordSetDescription(tmpRecordSet.getRecordSetDescription()
					+ Messages.getString(gde.device.graupner.hott.MessageIds.GDE_MSGI2404, new Object[] { countPackageLoss, packageLossPercentage, HoTTbinReader.lostPackages.getStatistics() })
					+ HoTTbinReader.sensorSignature);
			HoTTbinReader.log.logp(Level.WARNING, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "skipped number receiver data due to package loss = " + countPackageLoss); //$NON-NLS-1$
			HoTTbinReader.log.logp(Level.TIME, HoTTbinReader.$CLASS_NAME, $METHOD_NAME, "read time = " //$NON-NLS-1$
					+ StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$

			if (GDE.isWithUi()) {
				for (RecordSet recordSet : HoTTAdapter.recordSets.values()) {
					device.makeInActiveDisplayable(recordSet);
					device.updateVisibilityStatus(recordSet, true);

					// write filename after import to record description
					recordSet.descriptionAppendFilename(file.getName());
				}

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				GDE.getUiNotification().setProgress(100);
			}
		}
		finally {
			data_in.close();
			data_in = null;
		}
	}

	// ET splitting the parseAddxyz methods was preferred against copying the
	// code. so this specific code is not scattered over two classes.
	/**
	 * parse the buffered data from buffer and add points to record set
	 *
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddReceiver(byte[] _buf) throws DataInconsitsentException {
		HoTTbinReader.parse4Receiver(_buf);
		HoTTbinReader.recordSetReceiver.addPoints(HoTTbinReader.pointsReceiver, HoTTbinReader.timeStep_ms);
	}

	/**
	 * parse the buffered data from buffer
	 *
	 * @param _buf
	 */
	protected static void parse4Receiver(byte[] _buf) {
		// 0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx,
		// 6=VoltageRx, 7=TemperatureRx, 8=VoltageRx_min
		HoTTbinReader.tmpVoltageRx = (_buf[35] & 0xFF);
		HoTTbinReader.tmpTemperatureRx = (_buf[36] & 0xFF);
		HoTTbinReader.pointsReceiver[1] = (_buf[38] & 0xFF) * 1000;
		HoTTbinReader.pointsReceiver[3] = DataParser.parse2Short(_buf, 40) * 1000;
		if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.tmpVoltageRx > -1 && HoTTbinReader.tmpVoltageRx < 100 && HoTTbinReader.tmpTemperatureRx < 120) {
			HoTTbinReader.pointsReceiver[2] = (convertRxDbm2Strength(_buf[4] & 0xFF)) * 1000;
			HoTTbinReader.pointsReceiver[4] = (_buf[3] & 0xFF) * -1000;
			HoTTbinReader.pointsReceiver[5] = (_buf[4] & 0xFF) * -1000;
			HoTTbinReader.pointsReceiver[6] = (_buf[35] & 0xFF) * 1000;
			HoTTbinReader.pointsReceiver[7] = (_buf[36] & 0xFF) * 1000;
			HoTTbinReader.pointsReceiver[8] = (_buf[39] & 0xFF) * 1000;
		}
		if ((_buf[32] & 0x40) > 0 || (_buf[32] & 0x25) > 0 && HoTTbinReader.tmpTemperatureRx >= 70) // T = 70 - 20 = 50 lowest temperature warning
			HoTTbinReader.pointsReceiver[9] = (_buf[32] & 0x65) * 1000; // warning E,V,T only
		else
			HoTTbinReader.pointsReceiver[9] = 0;

		// printByteValues(_timeStep_ms, _buf);
		// if (HoTTbinReader.pointsReceiver[3] > 2000000)
		// System.out.println();
	}

	/**
	 * parse the buffered data from buffer and add points to record set
	 *
	 * @param _buf
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddChannel(byte[] _buf) throws DataInconsitsentException {
		HoTTbinReader.parse4Channel(_buf);
		HoTTbinReader.recordSetChannel.addPoints(HoTTbinReader.pointsChannel, HoTTbinReader.timeStep_ms);
	}

	/**
	 * parse the buffered data from buffer
	 *
	 * @param _buf
	 */
	protected static void parse4Channel(byte[] _buf) {
		// 0=FreCh, 1=Tx, 2=Rx, 3=Ch 1, 4=Ch 2 .. 18=Ch 16, 19=PowerOff, 20=BattLow, 21=Reset, 22=warning
		HoTTbinReader.pointsChannel[0] = (_buf[1] & 0xFF) * 1000;
		HoTTbinReader.pointsChannel[1] = (_buf[3] & 0xFF) * -1000;
		HoTTbinReader.pointsChannel[2] = (_buf[4] & 0xFF) * -1000;
		HoTTbinReader.pointsChannel[3] = (DataParser.parse2UnsignedShort(_buf, 8) / 2) * 1000;
		HoTTbinReader.pointsChannel[4] = (DataParser.parse2UnsignedShort(_buf, 10) / 2) * 1000;
		HoTTbinReader.pointsChannel[5] = (DataParser.parse2UnsignedShort(_buf, 12) / 2) * 1000;
		HoTTbinReader.pointsChannel[6] = (DataParser.parse2UnsignedShort(_buf, 14) / 2) * 1000;
		HoTTbinReader.pointsChannel[7] = (DataParser.parse2UnsignedShort(_buf, 16) / 2) * 1000;
		HoTTbinReader.pointsChannel[8] = (DataParser.parse2UnsignedShort(_buf, 18) / 2) * 1000;
		HoTTbinReader.pointsChannel[9] = (DataParser.parse2UnsignedShort(_buf, 20) / 2) * 1000;
		HoTTbinReader.pointsChannel[10] = (DataParser.parse2UnsignedShort(_buf, 22) / 2) * 1000;
		HoTTbinReader.pointsChannel[19] = (_buf[50] & 0x01) * 100000;
		HoTTbinReader.pointsChannel[20] = (_buf[50] & 0x02) * 50000;
		HoTTbinReader.pointsChannel[21] = (_buf[50] & 0x04) * 25000;
		if (_buf[32] > 0 && _buf[32] < 27) {
			HoTTbinReader.pointsChannel[22] = _buf[32] * 1000; // warning
			log.log(Level.FINE, String.format("Warning %d occured at %s", HoTTbinReader.pointsChannel[22] / 1000, StringHelper.getFormatedTime("HH:mm:ss.SSS", HoTTbinReader.timeStep_ms + HoTTbinReader.recordSetChannel.getStartTimeStamp())));
		}
		else
			HoTTbinReader.pointsChannel[22] = 0;

		if (_buf[5] == 0x00) { // channel 9-12
			HoTTbinReader.pointsChannel[11] = (DataParser.parse2UnsignedShort(_buf, 24) / 2) * 1000;
			HoTTbinReader.pointsChannel[12] = (DataParser.parse2UnsignedShort(_buf, 26) / 2) * 1000;
			HoTTbinReader.pointsChannel[13] = (DataParser.parse2UnsignedShort(_buf, 28) / 2) * 1000;
			HoTTbinReader.pointsChannel[14] = (DataParser.parse2UnsignedShort(_buf, 30) / 2) * 1000;
			if (HoTTbinReader.pointsChannel[15] == 0) {
				HoTTbinReader.pointsChannel[15] = 1500 * 1000;
				HoTTbinReader.pointsChannel[16] = 1500 * 1000;
				HoTTbinReader.pointsChannel[17] = 1500 * 1000;
				HoTTbinReader.pointsChannel[18] = 1500 * 1000;
			}
		}
		else { // channel 13-16
			HoTTbinReader.pointsChannel[15] = (DataParser.parse2UnsignedShort(_buf, 24) / 2) * 1000;
			HoTTbinReader.pointsChannel[16] = (DataParser.parse2UnsignedShort(_buf, 26) / 2) * 1000;
			HoTTbinReader.pointsChannel[17] = (DataParser.parse2UnsignedShort(_buf, 28) / 2) * 1000;
			HoTTbinReader.pointsChannel[18] = (DataParser.parse2UnsignedShort(_buf, 30) / 2) * 1000;
			if (HoTTbinReader.pointsChannel[11] == 0) {
				HoTTbinReader.pointsChannel[11] = 1500 * 1000;
				HoTTbinReader.pointsChannel[12] = 1500 * 1000;
				HoTTbinReader.pointsChannel[13] = 1500 * 1000;
				HoTTbinReader.pointsChannel[14] = 1500 * 1000;
			}
		}

		// printByteValues(_timeStep_ms, _buf);
	}

	/**
	 * parse the buffered data from buffer 0 to 2 and add points to record set
	 *
	 * @param sdLogVersion
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @throws DataInconsitsentException
	 */
	protected static int parseAddVario(int sdLogVersion, byte[] _buf0, byte[] _buf1, byte[] _buf2) throws DataInconsitsentException {
		HoTTbinReader.parse4Vario(sdLogVersion, _buf0, _buf1, _buf2);
		if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && HoTTbinReader.tmpClimb10 < 40000 && HoTTbinReader.tmpClimb10 > 20000)) {
			HoTTbinReader.recordSetVario.addPoints(HoTTbinReader.pointsVario, HoTTbinReader.timeStep_ms);
		}
		return sdLogVersion;
	}

	/**
	 * parse the buffered data from buffer 0 to 2
	 *
	 * @param sdLogVersion
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 */
	protected static int parse4Vario(int sdLogVersion, byte[] _buf0, byte[] _buf1, byte[] _buf2) {
		// 0=RXSQ, 1=Height, 2=Climb, 3=Climb 3, 4=Climb 10, 5=VoltageRx, 6=TemperatureRx 7=EventVario
		HoTTbinReader.pointsVario[0] = (_buf0[4] & 0xFF) * 1000;
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf1, 2);
		HoTTbinReader.tmpClimb10 = DataParser.parse2UnsignedShort(_buf2, 2);
		if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && HoTTbinReader.tmpClimb10 < 40000 && HoTTbinReader.tmpClimb10 > 20000)) {
			HoTTbinReader.pointsVario[1] = HoTTbinReader.tmpHeight * 1000;
			// pointsVarioMax = DataParser.parse2Short(buf1, 4) * 1000;
			// pointsVarioMin = DataParser.parse2Short(buf1, 6) * 1000;
			HoTTbinReader.pointsVario[2] = DataParser.parse2UnsignedShort(_buf1, 8) * 1000;
			HoTTbinReader.pointsVario[3] = DataParser.parse2UnsignedShort(_buf2, 0) * 1000;
			HoTTbinReader.pointsVario[4] = HoTTbinReader.tmpClimb10 * 1000;
			HoTTbinReader.pointsVario[5] = (_buf0[1] & 0xFF) * 1000;
			HoTTbinReader.pointsVario[6] = (_buf0[2] & 0xFF) * 1000;
		}
		HoTTbinReader.pointsVario[7] = (_buf1[1] & 0x3F) * 1000; // inverse event

		HoTTbinReader.isJustParsed = true;
		return sdLogVersion;
	}

	/**
	 * detect the SD Log Version V3 or V4
	 *
	 * @param _buf1
	 * @param _buf2
	 * @return
	 */
	protected static int getSdLogVerion(byte[] _buf1, byte[] _buf2) {
		// printByteValues(1, _buf1);
		// printByteValues(2, _buf2);
		if (HoTTbinReader.log.isLoggable(Level.FINER))
			HoTTbinReader.log.log(Level.FINER, "version = " + (((DataParser.parse2UnsignedShort(_buf1[3], _buf2[4]) - 30000) < -100) ? 4 : 3));
		return ((DataParser.parse2UnsignedShort(_buf1[3], _buf2[4]) - 30000) < -100) ? 4 : 3;
	}

	/**
	 * parse the buffered data from buffer 0 to 3 and add points to record set
	 *
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddGPS(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3) throws DataInconsitsentException {
		HoTTbinReader.parse4GPS(_buf0, _buf1, _buf2, _buf3);
		if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpClimb1 > 10000 && HoTTbinReader.tmpClimb3 > 30 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 4500)) {
			HoTTbinReader.recordSetGPS.addPoints(HoTTbinReader.pointsGPS, HoTTbinReader.timeStep_ms);
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 3
	 *
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 */
	protected static void parse4GPS(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3) {
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf2, 8);
		HoTTbinReader.tmpClimb1 = DataParser.parse2UnsignedShort(_buf3, 0);
		HoTTbinReader.tmpClimb3 = (_buf3[2] & 0xFF);
		HoTTbinReader.tmpVelocity = DataParser.parse2Short(_buf1, 4) * 1000;
		HoTTbinReader.pointsGPS[0] = (_buf0[4] & 0xFF) * 1000;
		if (!HoTTAdapter.isFilterEnabled || (HoTTbinReader.tmpClimb1 > 10000 && HoTTbinReader.tmpClimb3 > 30 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 4500)) {
			// 0=RXSQ, 1=Latitude, 2=Longitude, 3=Height, 4=Climb 1, 5=Climb 3, 6=Velocity, 7=DistanceStart, 8=DirectionStart, 9=TripLength, 10=VoltageRx, 11=TemperatureRx 12=satellites 13=GPS-fix 14=EventGPS
			HoTTbinReader.pointsGPS[6] = HoTTAdapter.isFilterEnabled && HoTTbinReader.tmpVelocity > 2000000 ? HoTTbinReader.pointsGPS[6] : HoTTbinReader.tmpVelocity;

			HoTTbinReader.tmpLatitude = DataParser.parse2Short(_buf1, 7) * 10000 + DataParser.parse2Short(_buf1[9], _buf2[0]);
			if (!HoTTAdapter.isTolerateSignChangeLatitude) HoTTbinReader.tmpLatitude = _buf1[6] == 1 ? -1 * HoTTbinReader.tmpLatitude : HoTTbinReader.tmpLatitude;
			HoTTbinReader.tmpLatitudeDelta = Math.abs(HoTTbinReader.tmpLatitude - HoTTbinReader.pointsGPS[1]);
			HoTTbinReader.tmpLatitudeDelta = HoTTbinReader.tmpLatitudeDelta > 400000 ? HoTTbinReader.tmpLatitudeDelta - 400000 : HoTTbinReader.tmpLatitudeDelta;
			HoTTbinReader.latitudeTolerance = HoTTbinReader.pointsGPS[6] / 1000.0 * (HoTTbinReader.timeStep_ms - HoTTbinReader.lastLatitudeTimeStep) / HoTTAdapter.latitudeToleranceFactor;
			HoTTbinReader.latitudeTolerance = HoTTbinReader.latitudeTolerance > 0 ? HoTTbinReader.latitudeTolerance : 5;

			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.pointsGPS[1] == 0 || HoTTbinReader.tmpLatitudeDelta <= HoTTbinReader.latitudeTolerance) {
				HoTTbinReader.lastLatitudeTimeStep = HoTTbinReader.timeStep_ms;
				HoTTbinReader.pointsGPS[1] = HoTTbinReader.tmpLatitude;
			}
			else {
				if (HoTTbinReader.log.isLoggable(Level.FINE)) HoTTbinReader.log.log(Level.FINE,
						StringHelper.getFormatedTime("HH:mm:ss:SSS", HoTTbinReader.timeStep_ms - GDE.ONE_HOUR_MS) + " Lat " + HoTTbinReader.tmpLatitude + " - " + HoTTbinReader.tmpLatitudeDelta);
			}

			HoTTbinReader.tmpLongitude = DataParser.parse2Short(_buf2, 2) * 10000 + DataParser.parse2Short(_buf2, 4);
			if (!HoTTAdapter.isTolerateSignChangeLongitude) HoTTbinReader.tmpLongitude = _buf2[1] == 1 ? -1 * HoTTbinReader.tmpLongitude : HoTTbinReader.tmpLongitude;
			HoTTbinReader.tmpLongitudeDelta = Math.abs(HoTTbinReader.tmpLongitude - HoTTbinReader.pointsGPS[2]);
			HoTTbinReader.tmpLongitudeDelta = HoTTbinReader.tmpLongitudeDelta > 400000 ? HoTTbinReader.tmpLongitudeDelta - 400000 : HoTTbinReader.tmpLongitudeDelta;
			HoTTbinReader.longitudeTolerance = HoTTbinReader.pointsGPS[6] / 1000.0 * (HoTTbinReader.timeStep_ms - HoTTbinReader.lastLongitudeTimeStep) / HoTTAdapter.longitudeToleranceFactor;
			HoTTbinReader.longitudeTolerance = HoTTbinReader.longitudeTolerance > 0 ? HoTTbinReader.longitudeTolerance : 5;

			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.pointsGPS[2] == 0 || HoTTbinReader.tmpLongitudeDelta <= HoTTbinReader.longitudeTolerance) {
				HoTTbinReader.lastLongitudeTimeStep = HoTTbinReader.timeStep_ms;
				HoTTbinReader.pointsGPS[2] = HoTTbinReader.tmpLongitude;
			}
			else {
				if (HoTTbinReader.log.isLoggable(Level.FINE)) HoTTbinReader.log.log(Level.FINE, StringHelper.getFormatedTime("HH:mm:ss:SSS", HoTTbinReader.timeStep_ms - GDE.ONE_HOUR_MS) + " Long "
						+ HoTTbinReader.tmpLongitude + " - " + HoTTbinReader.tmpLongitudeDelta + " - " + HoTTbinReader.longitudeTolerance);
			}

			HoTTbinReader.pointsGPS[3] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsGPS[4] = HoTTbinReader.tmpClimb1 * 1000;
			HoTTbinReader.pointsGPS[5] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsGPS[7] = DataParser.parse2Short(_buf2, 6) * 1000;
			HoTTbinReader.pointsGPS[8] = (_buf1[3] & 0xFF) * 1000;
			HoTTbinReader.pointsGPS[9] = 0;
			HoTTbinReader.pointsGPS[10] = (_buf0[1] & 0xFF) * 1000;
			HoTTbinReader.pointsGPS[11] = (_buf0[2] & 0xFF) * 1000;
			HoTTbinReader.pointsGPS[12] = (_buf3[3] & 0xFF) * 1000;
			try {
				HoTTbinReader.pointsGPS[13] = Integer.valueOf(String.format("%c", _buf3[4])) * 1000;
			}
			catch (NumberFormatException e1) {
				// ignore;
			}
		}
		HoTTbinReader.pointsGPS[14] = (_buf1[1] & 0x0F) * 1000; // inverse event

		HoTTbinReader.isJustParsed = true;
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 *
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddGAM(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) throws DataInconsitsentException {
		HoTTbinReader.parse4GAM(_buf0, _buf1, _buf2, _buf3, _buf4);
		if (!HoTTAdapter.isFilterEnabled
				|| (HoTTbinReader.tmpClimb3 > 30 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600 && Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
			HoTTbinReader.recordSetGAM.addPoints(HoTTbinReader.pointsGAM, HoTTbinReader.timeStep_ms);
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4
	 *
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 */
	protected static void parse4GAM(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) {
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf3, 0);
		HoTTbinReader.tmpClimb3 = (_buf3[4] & 0xFF);
		HoTTbinReader.tmpVoltage1 = DataParser.parse2Short(_buf1[9], _buf2[0]);
		HoTTbinReader.tmpVoltage2 = DataParser.parse2Short(_buf2, 1);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
		// 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance,
		// 6=CellVoltage 1, 7=CellVoltage 2 .... 11=CellVoltage 6,
		// 12=Revolution, 13=Height, 14=Climb, 15=Climb3, 16=FuelLevel,
		// 17=Voltage 1, 18=Voltage 2, 19=Temperature 1, 20=Temperature 2
		// 21=Speed, 22=LowestCellVoltage, 23=LowestCellNumber, 24=Pressure, 24=Event
		HoTTbinReader.pointsGAM[0] = (_buf0[4] & 0xFF) * 1000;
		if (!HoTTAdapter.isFilterEnabled
				|| (HoTTbinReader.tmpClimb3 > 30 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600 && Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			HoTTbinReader.pointsGAM[1] = DataParser.parse2Short(_buf3, 7) * 1000;
			HoTTbinReader.pointsGAM[2] = DataParser.parse2Short(_buf3, 5) * 1000;
			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.recordSetGAM.getRecordDataSize(true) <= 20
					|| (HoTTbinReader.tmpCapacity != 0 && Math.abs(HoTTbinReader.tmpCapacity) <= (HoTTbinReader.pointsGAM[3] / 1000 + HoTTbinReader.pointsGAM[1] / 1000 * HoTTbinReader.pointsGAM[2] / 1000 / 2500 + 2))) {
				HoTTbinReader.pointsGAM[3] = HoTTbinReader.tmpCapacity * 1000;
			}
			else {
				HoTTbinReader.log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - " + (HoTTbinReader.pointsGAM[3] / 1000)
						+ " + " + (HoTTbinReader.pointsGAM[1] / 1000 * HoTTbinReader.pointsGAM[2] / 1000 / 2500 + 2));
			}
			HoTTbinReader.pointsGAM[4] = Double.valueOf(HoTTbinReader.pointsGAM[1] / 1000.0 * HoTTbinReader.pointsGAM[2]).intValue();
			HoTTbinReader.pointsGAM[5] = 0;
			for (int j = 0; j < 6; j++) {
				HoTTbinReader.pointsGAM[j + 6] = (_buf1[3 + j] & 0xFF) * 1000;
				if (HoTTbinReader.pointsGAM[j + 6] > 0) {
					maxVotage = HoTTbinReader.pointsGAM[j + 6] > maxVotage ? HoTTbinReader.pointsGAM[j + 6] : maxVotage;
					minVotage = HoTTbinReader.pointsGAM[j + 6] < minVotage ? HoTTbinReader.pointsGAM[j + 6] : minVotage;
				}
			}
			HoTTbinReader.pointsGAM[5] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
			HoTTbinReader.pointsGAM[12] = DataParser.parse2Short(_buf2, 8) * 1000;
			HoTTbinReader.pointsGAM[13] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsGAM[14] = DataParser.parse2UnsignedShort(_buf3, 2) * 1000;
			HoTTbinReader.pointsGAM[15] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsGAM[16] = DataParser.parse2Short(_buf2, 6) * 1000;
			HoTTbinReader.pointsGAM[17] = HoTTbinReader.tmpVoltage1 * 1000;
			HoTTbinReader.pointsGAM[18] = HoTTbinReader.tmpVoltage2 * 1000;
			HoTTbinReader.pointsGAM[19] = (_buf2[3] & 0xFF) * 1000;
			HoTTbinReader.pointsGAM[20] = (_buf2[4] & 0xFF) * 1000;
			// 21=Speed, 22=LowestCellVoltage, 23=LowestCellNumber, 24=Pressure, 24=Event
			HoTTbinReader.pointsGAM[21] = DataParser.parse2Short(_buf4, 1) * 1000; // Speed [km/h
			HoTTbinReader.pointsGAM[22] = (_buf4[3] & 0xFF) * 1000; // lowest cell voltage 124 = 2.48 V
			HoTTbinReader.pointsGAM[23] = (_buf4[4] & 0xFF) * 1000; // cell number lowest cell voltage
			HoTTbinReader.pointsGAM[24] = (_buf4[8] & 0xFF) * 1000; // Pressure
		}
		if ((_buf1[1] & 0xFF) + ((_buf1[2] & 0x7F) << 8) != 0)
		HoTTbinReader.pointsGAM[25] = ((_buf1[1] & 0xFF) + ((_buf1[2] & 0x7F) << 8)) * 1000; //inverse event

		HoTTbinReader.isJustParsed = true;
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 *
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddEAM(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) throws DataInconsitsentException {
		HoTTbinReader.parse4EAM(_buf0, _buf1, _buf2, _buf3, _buf4);
		if (!HoTTAdapter.isFilterEnabled
				|| (HoTTbinReader.tmpClimb3 > 30 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600 && Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
			HoTTbinReader.recordSetEAM.addPoints(HoTTbinReader.pointsEAM, HoTTbinReader.timeStep_ms);
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4
	 *
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @param _buf3
	 * @param _buf4
	 */
	protected static void parse4EAM(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3, byte[] _buf4) {
		HoTTbinReader.tmpHeight = DataParser.parse2Short(_buf3, 3);
		HoTTbinReader.tmpClimb3 = (_buf4[3] & 0xFF);
		HoTTbinReader.tmpVoltage1 = DataParser.parse2Short(_buf2, 7);
		HoTTbinReader.tmpVoltage2 = DataParser.parse2Short(_buf2[9], _buf3[0]);
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf3[9], _buf4[0]);
		// 0=RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Balance,
		// 6=CellVoltage 1, 7=CellVoltage 2 .... 19=CellVoltage 14, 20=Height,
		// 21=Climb 1, 22=Climb 3, 23=Voltage 1, 24=Voltage 2, 25=Temperature 1,
		// 26=Temperature 2 27=RPM 28=MotorTime 29=Speed 30=Event
		HoTTbinReader.pointsEAM[0] = (_buf0[4] & 0xFF) * 1000;
		if (!HoTTAdapter.isFilterEnabled
				|| (HoTTbinReader.tmpClimb3 > 30 && HoTTbinReader.tmpHeight > 10 && HoTTbinReader.tmpHeight < 5000 && Math.abs(HoTTbinReader.tmpVoltage1) < 600 && Math.abs(HoTTbinReader.tmpVoltage2) < 600)) {
			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			HoTTbinReader.pointsEAM[1] = DataParser.parse2Short(_buf3, 7) * 1000;
			HoTTbinReader.pointsEAM[2] = DataParser.parse2Short(_buf3, 5) * 1000;
			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.recordSetEAM.getRecordDataSize(true) <= 20
					|| Math.abs(HoTTbinReader.tmpCapacity) <= (HoTTbinReader.pointsEAM[3] / 1000 + HoTTbinReader.pointsEAM[1] / 1000 * HoTTbinReader.pointsEAM[2] / 1000 / 2500 + 2)) {
				HoTTbinReader.pointsEAM[3] = HoTTbinReader.tmpCapacity * 1000;
			}
			else {
				HoTTbinReader.log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - " + (HoTTbinReader.pointsEAM[3] / 1000)
						+ " + " + (HoTTbinReader.pointsEAM[1] / 1000 * HoTTbinReader.pointsEAM[2] / 1000 / 2500 + 2));
			}
			HoTTbinReader.pointsEAM[4] = Double.valueOf(HoTTbinReader.pointsEAM[1] / 1000.0 * HoTTbinReader.pointsEAM[2]).intValue(); // power U*I [W];
			HoTTbinReader.pointsEAM[5] = 0; // 5=Balance
			for (int j = 0; j < 7; j++) {
				HoTTbinReader.pointsEAM[j + 6] = (_buf1[3 + j] & 0xFF) * 1000;
				if (HoTTbinReader.pointsEAM[j + 6] > 0) {
					maxVotage = HoTTbinReader.pointsEAM[j + 6] > maxVotage ? HoTTbinReader.pointsEAM[j + 6] : maxVotage;
					minVotage = HoTTbinReader.pointsEAM[j + 6] < minVotage ? HoTTbinReader.pointsEAM[j + 6] : minVotage;
				}
			}
			for (int j = 0; j < 7; j++) {
				HoTTbinReader.pointsEAM[j + 13] = (_buf2[j] & 0xFF) * 1000;
				if (HoTTbinReader.pointsEAM[j + 13] > 0) {
					maxVotage = HoTTbinReader.pointsEAM[j + 13] > maxVotage ? HoTTbinReader.pointsEAM[j + 13] : maxVotage;
					minVotage = HoTTbinReader.pointsEAM[j + 13] < minVotage ? HoTTbinReader.pointsEAM[j + 13] : minVotage;
				}
			}
			// calculate balance on the fly
			HoTTbinReader.pointsEAM[5] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? (maxVotage - minVotage) * 10 : 0;
			HoTTbinReader.pointsEAM[20] = HoTTbinReader.tmpHeight * 1000;
			HoTTbinReader.pointsEAM[21] = DataParser.parse2UnsignedShort(_buf4, 1) * 1000;
			HoTTbinReader.pointsEAM[22] = HoTTbinReader.tmpClimb3 * 1000;
			HoTTbinReader.pointsEAM[23] = HoTTbinReader.tmpVoltage1 * 1000;
			HoTTbinReader.pointsEAM[24] = HoTTbinReader.tmpVoltage2 * 1000;
			HoTTbinReader.pointsEAM[25] = (_buf3[1] & 0xFF) * 1000;
			HoTTbinReader.pointsEAM[26] = (_buf3[2] & 0xFF) * 1000;
			HoTTbinReader.pointsEAM[27] = DataParser.parse2Short(_buf4, 4) * 1000; // revolution
			HoTTbinReader.pointsEAM[28] = ((_buf4[6] & 0xFF) * 60 + (_buf4[7] & 0xFF)) * 1000; // motor time
			HoTTbinReader.pointsEAM[29] = DataParser.parse2Short(_buf4, 8) * 1000; // speed
		}
		HoTTbinReader.pointsEAM[30] = ((_buf1[1] & 0xFF) + ((_buf1[2] & 0x7F) << 8)) * 1000; // inverse event

		HoTTbinReader.isJustParsed = true;
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 *
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 * @throws DataInconsitsentException
	 */
	protected static void parseAddESC(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3) throws DataInconsitsentException {
		HoTTbinReader.parse4ESC(_buf0, _buf1, _buf2, _buf3);
		if (!HoTTAdapter.isFilterEnabled
				|| HoTTbinReader.tmpVoltage > 0 && HoTTbinReader.tmpVoltage < 1000 && HoTTbinReader.tmpCurrent < 4000 && HoTTbinReader.tmpCurrent > -10 && HoTTbinReader.tmpRevolution > -1
						&& HoTTbinReader.tmpRevolution < 20000 && !(HoTTbinReader.pointsESC[6] != 0 && HoTTbinReader.pointsESC[6] / 1000 - HoTTbinReader.tmpTemperatureFet > 20)) {
			HoTTbinReader.recordSetESC.addPoints(HoTTbinReader.pointsESC, HoTTbinReader.timeStep_ms);
		}
	}

	/**
	 * parse the buffered data from buffer 0 to 4 and add points to record set
	 *
	 * @param _buf0
	 * @param _buf1
	 * @param _buf2
	 */
	protected static void parse4ESC(byte[] _buf0, byte[] _buf1, byte[] _buf2, byte[] _buf3) {
		// 0=RF_RXSQ, 1=Voltage, 2=Current, 3=Capacity, 4=Power, 5=Revolution, 6=Temperature1, 7=Temperature2
		// 8=Voltage_min, 9=Current_max, 10=Revolution_max, 11=Temperature1_max, 12=Temperature2_max 13=Event
		HoTTbinReader.tmpVoltage = DataParser.parse2Short(_buf1, 3);
		HoTTbinReader.tmpCurrent = DataParser.parse2Short(_buf2, 1);
		HoTTbinReader.pointsESC[0] = (_buf0[4] & 0xFF) * 1000;
		HoTTbinReader.tmpCapacity = DataParser.parse2Short(_buf1, 7);
		HoTTbinReader.tmpRevolution = DataParser.parse2Short(_buf2, 5);
		HoTTbinReader.tmpTemperatureFet = _buf1[9] - 20;
		if (!HoTTAdapter.isFilterEnabled
				|| HoTTbinReader.tmpVoltage > 0 && HoTTbinReader.tmpVoltage < 1000 && HoTTbinReader.tmpCurrent < 4000 && HoTTbinReader.tmpCurrent > -10 && HoTTbinReader.tmpRevolution > -1
						&& HoTTbinReader.tmpRevolution < 20000 && !(HoTTbinReader.pointsESC[6] != 0 && HoTTbinReader.pointsESC[6] / 1000 - HoTTbinReader.tmpTemperatureFet > 20)) {
			HoTTbinReader.pointsESC[1] = HoTTbinReader.tmpVoltage * 1000;
			HoTTbinReader.pointsESC[2] = HoTTbinReader.tmpCurrent * 1000;
			HoTTbinReader.pointsESC[4] = Double.valueOf(HoTTbinReader.pointsESC[1] / 1000.0 * HoTTbinReader.pointsESC[2]).intValue();
			if (!HoTTAdapter.isFilterEnabled || HoTTbinReader.recordSetESC.getRecordDataSize(true) <= 20
					|| (HoTTbinReader.tmpCapacity != 0 && Math.abs(HoTTbinReader.tmpCapacity) <= (HoTTbinReader.pointsESC[3] / 1000 + HoTTbinReader.tmpVoltage * HoTTbinReader.tmpCurrent / 2500 + 2))) {
				HoTTbinReader.pointsESC[3] = HoTTbinReader.tmpCapacity * 1000;
			}
			else {
				if (HoTTbinReader.tmpCapacity != 0) HoTTbinReader.log.log(Level.FINE, StringHelper.getFormatedTime("mm:ss.SSS", HoTTbinReader.timeStep_ms) + " - " + HoTTbinReader.tmpCapacity + " - "
						+ (HoTTbinReader.pointsESC[3] / 1000) + " + " + (HoTTbinReader.tmpVoltage * HoTTbinReader.tmpCurrent / 2500 + 2));
			}
			HoTTbinReader.pointsESC[5] = HoTTbinReader.tmpRevolution * 1000;
			HoTTbinReader.pointsESC[6] = HoTTbinReader.tmpTemperatureFet * 1000;

			HoTTbinReader.pointsESC[7] = (_buf2[9] - 20) * 1000;
			// 8=Voltage_min, 9=Current_max, 10=Revolution_max,
			// 11=Temperature1_max, 12=Temperature2_max
			HoTTbinReader.pointsESC[8] = DataParser.parse2Short(_buf1, 5) * 1000;
			HoTTbinReader.pointsESC[9] = DataParser.parse2Short(_buf2, 3) * 1000;
			HoTTbinReader.pointsESC[10] = DataParser.parse2Short(_buf2, 7) * 1000;
			HoTTbinReader.pointsESC[11] = (_buf2[0] - 20) * 1000;
			HoTTbinReader.pointsESC[12] = (_buf3[0] - 20) * 1000;
		}
		if ((_buf1[1] & 0xFF) != 0)
		HoTTbinReader.pointsESC[13] = (_buf1[1] & 0xFF) * 1000; //inverse event

		HoTTbinReader.isJustParsed = true;
	}

	static void printByteValues(long millisec, byte[] buffer) {
		StringBuilder sb = new StringBuilder().append(StringHelper.getFormatedTime("mm:ss:SSS", millisec)).append(" : ");
		for (int i = 0; buffer != null && i < buffer.length; i++) {
			sb.append("(").append(i).append(")").append(buffer[i]).append(GDE.STRING_BLANK);
		}
		HoTTbinReader.log.log(Level.FINE, sb.toString());
	}

	static void printShortValues(long millisec, byte[] buffer) {
		StringBuilder sb = new StringBuilder().append(StringHelper.getFormatedTime("mm:ss:SSS", millisec)).append(" : ");
		for (int i = 0; buffer != null && i < buffer.length - 1; i++) {
			sb.append("(").append(i).append(")").append(DataParser.parse2Short(buffer, i)).append(GDE.STRING_BLANK);
		}
		HoTTbinReader.log.log(Level.FINE, sb.toString());
	}

	/**
	 * @return get a rectified start timestamp if the files's last modified timestamp does not correspond with the filename
	 */
	protected static long getStartTimeStamp(String fileName, long fileLastModified, long numberDatablocks) {
		final long startTimeStamp;
		log.log(Level.FINE, "name=", fileName); //$NON-NLS-1$
		Pattern hoTTNamePattern = Pattern.compile("\\d{4}\\_\\d{4}-\\d{1,2}-\\d{1,2}"); //$NON-NLS-1$
		Matcher hoTTMatcher = hoTTNamePattern.matcher(fileName);
		if (hoTTMatcher.find()) {
			String logName = hoTTMatcher.group();

			String[] strValueLogName = logName.split(GDE.STRING_UNDER_BAR);
			int logCounter = Integer.parseInt(strValueLogName[0]);

			String[] strValueDate = strValueLogName[1].split(GDE.STRING_DASH);
			int year = Integer.parseInt(strValueDate[0]);
			int month = Integer.parseInt(strValueDate[1]);
			int day = Integer.parseInt(strValueDate[2]);

			// check if the zoned date is equal
			GregorianCalendar lastModifiedDate = new GregorianCalendar();
			lastModifiedDate.setTimeInMillis(fileLastModified - numberDatablocks * 10);
			if (year == lastModifiedDate.get(Calendar.YEAR) && month == lastModifiedDate.get(Calendar.MONTH) + 1 && day == lastModifiedDate.get(Calendar.DAY_OF_MONTH)) {
				startTimeStamp = lastModifiedDate.getTimeInMillis();
				log.log(Level.FINE, "lastModified=" + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", fileLastModified) + " " + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", lastModifiedDate.getTimeInMillis())); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
			else {
				// spread the logCounter into steps of 10 minutes
				GregorianCalendar fileNameDate = new GregorianCalendar(year, month - 1, day, 0, 0, 0);
				fileNameDate.add(Calendar.MINUTE, 10 * (logCounter % (24 * 6)));
				startTimeStamp = fileNameDate.getTimeInMillis();
				log.log(Level.FINE, "fileNameDate=" + StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", fileNameDate.getTimeInMillis())); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		else {
			startTimeStamp = fileLastModified;
		}
		return startTimeStamp;
	}

	/**
	 * check if file time stamp match log internal recorded start time and replace if required
	 * @param formattedLogStartTime
	 * @param fileStartTimeStamp_ms
	 * @return
	 */
	protected static long getStartTimeStamp(String formattedLogStartTime, long fileStartTimeStamp_ms) {
		long startTimeStamp_ms = fileStartTimeStamp_ms;
		String formattedFileStartTimeStamp = StringHelper.getFormatedTime("YYYY-MM-dd HH:mm:ss.SSS", fileStartTimeStamp_ms);

		if (!formattedFileStartTimeStamp.contains(formattedLogStartTime)) { // LOG START TIME : 15:08:28

			int year = Integer.parseInt(formattedFileStartTimeStamp.substring(0, 4));
			int month = Integer.parseInt(formattedFileStartTimeStamp.substring(5, 7));
			int day = Integer.parseInt(formattedFileStartTimeStamp.substring(8, 10));

			int hour = Integer.parseInt(formattedLogStartTime.substring(0, 2));
			int minute = Integer.parseInt(formattedLogStartTime.substring(3, 5));
			int second = Integer.parseInt(formattedLogStartTime.substring(6, 8));

			GregorianCalendar calendar = new GregorianCalendar(year, month - 1, day, hour, minute, second);
			fileStartTimeStamp_ms = calendar.getTimeInMillis();
		}
		return startTimeStamp_ms;
	}
}
