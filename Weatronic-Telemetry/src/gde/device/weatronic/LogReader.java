package gde.device.weatronic;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.io.DataParser;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;
import gde.utils.StringHelper;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

public class LogReader {
	final static String				$CLASS_NAME		= LogReader.class.getName();
	final static Logger				log						= Logger.getLogger(LogReader.$CLASS_NAME);

	final static DataExplorer	application		= DataExplorer.getInstance();
	final static Channels			channels			= Channels.getInstance();

	static int								dataBlockSize	= 64;

	static int[]							points;
	static RecordSet					recordSet;
	static byte[]							buf_length = new byte[4];
	static byte[]							buf_log_record_tmp;
	static byte[]							buf_log_record;
	static byte[]							buf_header;
	static byte[]							buf_data;
	static long								timeStep_ms;
	static Map<Integer, Measurement> measurements = new HashMap<Integer, Measurement>();
	static Vector<Integer> measurementIds = new Vector<Integer>();
	static Map<Integer,DataItem> receivedDataItems = new HashMap<Integer,DataItem>();
	static Vector<Integer> unknownIds = new Vector<Integer>();



	public LogReader() {
		
	}
	
	public enum RecordType {
		UNKNOWN, DATA, HEADER, EVENT;
	}
	
	public class Record {
		protected int offset;
		
		public Record() {
			this.offset = 13;
		}
	}
	
	public long getTimeOffset(final int interval) {
		switch (interval) {
		//Smaller ID for shorter intervals
		case 0: // "1000Hz";
			return 1;
		case 1: // "500Hz";
			return 2;
		case 2: // "200Hz";
			return 5;
		case 3: // "100Hz";
			return 10;
		case 4: // "50Hz";
			return 20;
		case 5: // "20Hz";
			return 50;
		case 6: // "10Hz";
			return 100;
		case 7: // "5Hz";
			return 200;
		case 8: // "4Hz";
			return 250;
		case 9: // "2Hz";
			return 500;
		case 10: // "1Hz";
			return 1000;
		case 11: // "0.5Hz";
			return 2000;
		case 12: // "0.2Hz";
			return 2500;
		case 13: // "0.1Hz";
			return 10000;
		default:
			return 0;
		}
	}
	
	public enum DataType {
			SignedByte(43),
			UnsignedByte(52),
			SignedShort(44),
			UnsignedShort(53),
			SignedWord(45),
			UnsignedWord(54),
			Float(6),
			PaketGPS(20),
			PaketControlData(5),
			PaketServoData(4),
			PowerSupply(34),
			ControlIDPaket(17),
			SignedShort2(2),
			SignedShort3(3),
			UnknownSize(0);
			
			private final int value;
			private DataType(int v) {
				this.value = v;
			}
			public int value() {
				return this.value;
			}		
			public static DataType valueOf(int i) {
				for (DataType ds : DataType.values()) {
					if (ds.value == i)
						return ds;
				}
				return DataType.UnknownSize;
			}
	}
	
	public class Measurement {
		public static final double DBL_EPSILON = 2.220446049250313E-16d;
		String name;
		int id;
		DataType dataType;
		double factor;
		double offset;
		String unit;
		
		public Measurement(final byte[] buffer) {
			this.name = new String(buffer, 0, 64).trim();
			this.id = DataParser.parse2UnsignedShort(buffer,  64);
			this.dataType = DataType.valueOf(buffer[66]);
			byte[] bytes = new byte[8];
			System.arraycopy(buffer, 67, bytes, 0, 8);
			this.offset = DataParser.byte2Double(bytes, true)[0];
			System.arraycopy(buffer, 75, bytes, 0, 8);
			this.factor = DataParser.byte2Double(bytes, true)[0];
			this.factor = (this.factor-0.0) < DBL_EPSILON ? 1.0 : this.factor; //TODO check why are some factors so big??
			this.unit = new String(buffer, 83, 8).trim();
			System.out.println(String.format(Locale.ENGLISH, "%s[%s] factor=%f offset=%f Id=%d type=%d;", this.name, this.unit, this.factor, this.offset, this.id, this.dataType.value));
		}

		public String getName() {
			return name;
		}

		public int getId() {
			return id;
		}

		public DataType getDataType() {
			return dataType;
		}

		public int getDataSize() {
			return getSizeByType(dataType);
		}

		/**
		 * @param type
		 * @return the size in bytes to be used for a data type
		 */
		public int getSizeByType(DataType type) {
			switch (type) {
			case SignedByte:				/* 8Bit signed */
				return 1;
			case UnsignedByte:			/* 8Bit unsigned */
				return 1;
			case SignedShort:				/* 16Bit signed */
				return 2;
			case UnsignedShort:			/* 16Bit unsigned */
				return 2;
			case SignedWord:				/* 32Bit signed */
				return 4;
			case UnsignedWord:			/* 32Bit unsigned */
				return 4;
			case Float:							/* single float according IEEE754 */
				return 6;
			case PaketGPS:					/* 20 Bytes */
				return 20;
			case PaketControlData:	/* 16 12Bit values, 1.5Byte each, -2047 ... +2047 --> -100% ... +100% */
				return 24;
			case PaketServoData:		/* 16 12Bit values, 1.5Byte each, -2047 ... +2047 --> -200% ... +200% */
				return 24;
			case ControlIDPaket:		/* 16 16Bit values */
				return 32;
			case PowerSupply:				/* 64 Bytes */
				return 64;
			default://empty fields 2 bytes long
			case SignedShort2:
			case SignedShort3:
				return 2;
			}
		}

		public double getFactor() {
			return factor;
		}

		public double getOffset() {
			return offset;
		}

		public String getUnit() {
			return unit;
		}
	}
	
	public class RecordHeader extends Record {

		String modellName;
		int measurementCount;

		public RecordHeader(final byte[] buffer) {
			super(); //constructor will set offset
			this.modellName = new String(buffer, 0 + offset, 256); 
			this.measurementCount = DataParser.parse2UnsignedShort(buffer, 256 + offset);
			for (int i = 0; i < this.measurementCount; i++) {
				byte[] measurementBuffer = new byte[91];
				System.arraycopy(buffer, 258 + offset + i * measurementBuffer.length, measurementBuffer, 0, measurementBuffer.length);
				
				//due to special measurement blocks we need to add a second loop here
				DataType dataSize = DataType.valueOf(measurementBuffer[66]);
				final String measurementName = new String(measurementBuffer, 0, 64).trim();
				switch (dataSize) {
				case PaketGPS://GPS packet
					String[] packetGpsNames = {"_Latitude","_Longitude","_Speed","_Altitude","_Course","_isValid","_UTC"};
					for (int j = 0; j < packetGpsNames.length; j++) {
						String packetGpsName = measurementName + packetGpsNames[j];
						byte[] tempMeasurementBuffer = new byte[64];
						System.arraycopy(packetGpsName.getBytes(), 0, tempMeasurementBuffer, 0, packetGpsName.getBytes().length);
						System.arraycopy(tempMeasurementBuffer, 0, measurementBuffer, 0, 64);
					}
					Measurement measurement = new Measurement(measurementBuffer);
					LogReader.measurements.put(measurement.getId(), measurement);									
					LogReader.measurementIds.add(measurement.getId());
					break;
					
				case PowerSupply://Power Supply packet
					String[] powerSupplyNames = {"_Main_Status","_Main_Voltage","_Main_Current","_Main_InputVoltage","_Main_InputCurrent","_Main_MainVoltage","_Main_ReserveVoltage","_Main_InputTemperature"};
					for (int j = 0; j < powerSupplyNames.length; j++) {
						String powerSupplyName = measurementName + powerSupplyNames[j];
						byte[] tempMeasurementBuffer = new byte[64];
						System.arraycopy(powerSupplyName.getBytes(), 0, tempMeasurementBuffer, 0, powerSupplyName.getBytes().length);
						System.arraycopy(tempMeasurementBuffer, 0, measurementBuffer, 0, 64);
					}
					String[] mainCellMeasurementNames = {"_Cell%d_Status","_Cell%d_Voltage","_Cell%d_Current","_Cell%d_Capacity","_Cell%d_Temperature"};
					for (int cell = 1; cell <= 4; cell++) {
						for (int k = 0; k < mainCellMeasurementNames.length; k++) {
							String cellmeasurementName = measurementName + String.format(mainCellMeasurementNames[k], cell);
							byte[] tempMeasurementBuffer = new byte[64];
							System.arraycopy(cellmeasurementName.getBytes(), 0, tempMeasurementBuffer, 0, cellmeasurementName.getBytes().length);
							System.arraycopy(tempMeasurementBuffer, 0, measurementBuffer, 0, 64);
						}
					}
					measurement = new Measurement(measurementBuffer);
					LogReader.measurements.put(measurement.getId(), measurement);									
					LogReader.measurementIds.add(measurement.getId());
					break;
					
				default:
					measurement = new Measurement(measurementBuffer);
					LogReader.measurements.put(measurement.getId(), measurement);		
					LogReader.measurementIds.add(measurement.getId());
					break;
				}

			}
		}
			
		public String getModellName() {
			return modellName;
		}

		public int getMeasurementCount() {
			return measurementCount > LogReader.measurements.size() ? measurementCount : LogReader.measurements.size();
		}

		public Map<Integer, Measurement> getMeasurements() {
			return measurements;
		}
	}
	
	public class ConfigSection {
		int count;
		int interval;
		Vector<Integer> ids = new Vector<Integer>();
		int lengthBytes = 0;
		
		public ConfigSection(final byte[] buffer, final int offset) {
			this.count = DataParser.parse2UnsignedShort(buffer, 0+offset);
			System.out.print(String.format("config sections %3d, ", this.count));
			this.interval = (buffer[2+offset] & 0xFF);
			System.out.print(String.format("interval %3d, IDs ", this.interval));			
			this.lengthBytes = 3 + this.count * 2;
			for (int i = 0; i < this.count; i++) {
				System.out.print(String.format("%d ", DataParser.parse2UnsignedShort(buffer, 3 + offset + i * 2)));			
				this.ids.add(DataParser.parse2UnsignedShort(buffer, 3 + offset + i * 2));
			}
			System.out.println();
		}

		public int getId(final int index) {
			return ids.get(index);
		}

		public Vector<Integer> getIds() {
			return ids;
		}

		public int getCount() {
			return count;
		}

		public int getInterval() {
			return interval;
		}

		public int getLength() {
			return lengthBytes;
		}
	}
	
	public class DataItem {
		final Measurement measurement;
		final DataType dataType;
		int value;
		
		int longitude;
		int latitude;
		int speed;
		int altitude;
		int course;
		int isValid;
		long timeStam_ms_utc;
		
		int status;
		int voltage;
		int current;
		int inVoltage;
		int inCurrent;
		int mainVoltage;
		int reserveVoltage;
		int inTemperature;
		
		int cell1_Status;
		int cell1_Voltage;
		int cell1_Current;
		int cell1_Capacity;
		int cell1_Temperature;				
		int cell2_Status;
		int cell2_Voltage;
		int cell2_Current;
		int cell2_Capacity;
		int cell2_Temperature;				
		int cell3_Status;
		int cell3_Voltage;
		int cell3_Current;
		int cell3_Capacity;
		int cell3_Temperature;				
		int cell4_Status;
		int cell4_Voltage;
		int cell4_Current;
		int cell4_Capacity;
		int cell4_Temperature;				

		long dataSectionTimeStamp_ms;
		
		public DataItem(final byte[] buffer, final int offset, final Measurement measurement, final long dataSectionTimeStemp) {
			this.measurement = measurement;
			this.dataType = measurement.getDataType();
			this.dataSectionTimeStamp_ms = dataSectionTimeStemp;
			switch (this.dataType) {
			case SignedByte:				/* 8Bit signed */
				this.value = buffer[offset];
				break;
			case UnsignedByte:			/* 8Bit unsigned */
				this.value = buffer[offset] & 0xFF;
				break;
			case SignedShort:				/* 16Bit signed */
				this.value = DataParser.parse2Short(buffer, offset);
				break;
			case UnsignedShort:			/* 16Bit unsigned */
				this.value = DataParser.parse2UnsignedShort(buffer, offset);
				break;
			case SignedWord:				/* 32Bit signed */
				this.value = DataParser.parse2Int(buffer, offset);
				break;
			case UnsignedWord:			/* 32Bit unsigned */
				this.value = DataParser.parse2Int(buffer, offset); 
				break;
			case Float:							/* single float according IEEE754 */
				System.out.println("Float data item received, can not handle");
				break;
			case PaketGPS:					/* 20 Bytes */
				//"4_Latitude","4_Longitude","2_Speed","2_Altitude","2_Course","1_isValid","4+1_UTC"
				latitude = DataParser.parse2Int(buffer, offset);
				longitude = DataParser.parse2Int(buffer, 4+offset);
				speed = DataParser.parse2Short(buffer, 8+offset);
				altitude = DataParser.parse2Short(buffer, 10+offset);
				course = DataParser.parse2UnsignedShort(buffer, 12+offset);
				isValid = (buffer[14+offset] & 0x01);
				timeStam_ms_utc = DataParser.getUInt32(buffer, 15+offset) + (buffer[19] & 0xFF);
				break;
			case PaketControlData:	/* 16 12Bit values, 1.5Byte each, -2047 ... +2047 --> -100% ... +100% */
				//24 bytes
				break;
			case PaketServoData:		/* 16 12Bit values, 1.5Byte each, -2047 ... +2047 --> -200% ... +200% */
				//24 bytes
				break;
			case ControlIDPaket:		/* 16 16Bit values */
				//32 bytes
				break;
			case PowerSupply:				/* 64 Bytes */
				//"_Main_Status","_Main_Voltage","_Main_Current","_Main_InputVoltage","_Main_InputCurrent","_Main_MainVoltage","_Main_ReserveVoltage","_Main_InputTemperature"
				status = DataParser.parse2Int(buffer, offset);; //2560033
				voltage = DataParser.parse2Short(buffer, 4+offset); //3.744
				current = DataParser.parse2Short(buffer, 6+offset);
				inVoltage = DataParser.parse2Short(buffer, 8+offset);
				inCurrent = DataParser.parse2Short(buffer, 10+offset);
				mainVoltage = DataParser.parse2Short(buffer, 12+offset);
				reserveVoltage = DataParser.parse2Short(buffer, 14+offset);
				inTemperature = DataParser.parse2Short(buffer, 16+offset);
				
				//"_Cell%d_Status","_Cell%d_Voltage","_Cell%d_Current","_Cell%d_Capacity","_Cell%d_Temperature"				
				cell1_Status = DataParser.parse2Int(buffer, 20+offset); //1110272
				cell1_Voltage = DataParser.parse2Short(buffer, 24+offset);
				cell1_Current = DataParser.parse2Short(buffer, 26+offset);
				cell1_Capacity = DataParser.parse2Short(buffer, 28+offset);
				cell1_Temperature = buffer[30+offset];	
				
				cell2_Status = DataParser.parse2Int(buffer, 31+offset);
				cell2_Voltage = DataParser.parse2Short(buffer, 35+offset);
				cell2_Current = DataParser.parse2Short(buffer, 37+offset);
				cell2_Capacity = DataParser.parse2Short(buffer, 39+offset);
				cell2_Temperature = buffer[41+offset];		
				
				cell3_Status = DataParser.parse2Int(buffer, 42+offset);
				cell3_Voltage = DataParser.parse2Short(buffer, 46+offset);
				cell3_Current = DataParser.parse2Short(buffer, 48+offset);
				cell3_Capacity = DataParser.parse2Short(buffer, 50+offset);
				cell3_Temperature = buffer[52+offset];		
				
				cell4_Status = DataParser.parse2Int(buffer, 53+offset);
				cell4_Voltage = DataParser.parse2Short(buffer, 57+offset);
				cell4_Current = DataParser.parse2Short(buffer, 59+offset);
				cell4_Capacity = DataParser.parse2Short(buffer, 61+offset);
				cell4_Temperature = buffer[63+offset];				
				break;
			default://empty fields 2 bytes long
			case SignedShort2:
			case SignedShort3:
				this.value = DataParser.parse2Short(buffer, offset);
				break;
			}
		}

		private void printValue(final long dataSectionTimeStamp) {
			switch (this.dataType) {
			case PaketGPS:					/* 20 Bytes */
				//"4_Latitude","4_Longitude","2_Speed","2_Altitude","2_Course","1_isValid","4+1_UTC"
				System.out.print(String.format(Locale.ENGLISH, "%.6f°;%.6f°;%.1fkn;%.1fm;%.2f°;%d;%d;", latitude / 6000000.0f, longitude / 6000000.0f, speed / 10.0f, altitude / 10.0f, course / 100.0f, isValid, timeStam_ms_utc));

				if (this.dataSectionTimeStamp_ms + 1500 < dataSectionTimeStamp ) {
					latitude = longitude = speed = altitude = course = isValid = 0;
					timeStam_ms_utc = 0L;
				}
				break;
			case PowerSupply:				/* 64 Bytes */
				//"_Main_Status","_Main_Voltage","_Main_Current","_Main_InputVoltage","_Main_InputCurrent","_Main_MainVoltage","_Main_ReserveVoltage","_Main_InputTemperature"
				System.out.print(String.format(Locale.ENGLISH, "%d;%.3fV;%.3fA;%.3fV;%.3fA;%.3fV;%.3fV;%d°C;", status, voltage / 1000.0f, current / 1000.0f, inVoltage / 1000.0f, inCurrent / 1000.0f, mainVoltage / 1000.0f, reserveVoltage / 1000.0f, inTemperature));
				
				//"_Cell%d_Status","_Cell%d_Voltage","_Cell%d_Current","_Cell%d_Capacity","_Cell%d_Temperature"				
				System.out.print(String.format(Locale.ENGLISH, "%d;%.3fV;%.3fA;%.3fAh;%d°C;", cell1_Status, cell1_Voltage / 1000.0f, cell1_Current / 1000.0f, cell1_Capacity / 1000.0f, cell1_Temperature));
				System.out.print(String.format(Locale.ENGLISH, "%d;%.3fV;%.3fA;%.3fAh;%d°C;", cell2_Status, cell2_Voltage / 1000.0f, cell2_Current / 1000.0f, cell2_Capacity / 1000.0f, cell2_Temperature));
				System.out.print(String.format(Locale.ENGLISH, "%d;%.3fV;%.3fA;%.3fAh;%d°C;", cell3_Status, cell3_Voltage / 1000.0f, cell3_Current / 1000.0f, cell3_Capacity / 1000.0f, cell3_Temperature));
				System.out.print(String.format(Locale.ENGLISH, "%d;%.3fV;%.3fA;%.3fAh;%d°C;", cell4_Status, cell4_Voltage / 1000.0f, cell4_Current / 1000.0f, cell4_Capacity / 1000.0f, cell4_Temperature));

				if (this.dataSectionTimeStamp_ms + 1500 < dataSectionTimeStamp ) {
					status = voltage = current = inVoltage = inCurrent = mainVoltage = reserveVoltage = inTemperature = 0;
					cell1_Status = cell1_Voltage = cell1_Current = cell1_Capacity = cell1_Temperature = 0;
					cell2_Status = cell2_Voltage = cell2_Current = cell2_Capacity = cell2_Temperature = 0;
					cell3_Status = cell3_Voltage = cell3_Current = cell3_Capacity = cell3_Temperature = 0;
					cell4_Status = cell4_Voltage = cell4_Current = cell4_Capacity = cell4_Temperature = 0;
				}
				break;
			default:
				String format = "%." + getDecimalPoints(measurement.factor) + "f%s;";
				System.out.print(String.format(Locale.ENGLISH, format, this.value * measurement.factor + measurement.offset, measurement.getUnit()));

				if (this.dataSectionTimeStamp_ms + 1500 < dataSectionTimeStamp ) {
					this.value = 0;
				}
				break;
			}
		}
		
		public int getDecimalPoints(final double factor) {
			if (factor >= 1.0) {
				return 1;
			}
			//Basically number of leading zeroes, e.g. 3 for 0.005
			return (int) Math.ceil(Math.abs(Math.log10(factor)));
		}
		
		public Measurement getMeasurement() {
			return this.measurement;
		}
	}
	
	public class DataSection {

		int length;
		int interval;
		int readBytes;
		
		public DataSection(final byte[] buffer, int offset, final Vector<ConfigSection> configSections, final RecordHeader recordHeader, final long sectionTimeStamp) {
			this.length = DataParser.parse2Int(buffer, 0 + offset);
			this.interval = (buffer[4+offset] & 0xFF);
//			System.out.println(String.format("data sections %3d, interval %3d", this.length, this.interval));		
			readBytes = 5;
				
			for (ConfigSection configSection : configSections) {
				if (configSection.getInterval() == this.interval) {
					for (Integer id : configSection.ids) {
						Measurement measurement = recordHeader.getMeasurements().get(id);
						if (measurement != null && (offset + 5 + measurement.getDataSize()) <= buffer.length) {
							receivedDataItems.put(id, new DataItem(buffer, offset+5, measurement, sectionTimeStamp));
							offset += measurement.getDataSize();
							readBytes += measurement.getDataSize();
						}
						else {
							if (!unknownIds.contains(id)) {
								unknownIds.add(id);
							}
							offset += 2; //default width used for unknown measurements
						}
					}
				}
			}
		}
		
		public int getReadBytes() {
			return readBytes;
		}
		
//		public int getSectionLength() {
//			return length + 3;
//		}
		
		public int getLength() {
			return length;
		}

		public int getInterval() {
			return interval;
		}
		
	}
	
	/**
	 * RecordData is a class to handle log records of type data
	 */
	public class RecordData extends Record {
		
		Vector<ConfigSection> configSections = new Vector<LogReader.ConfigSection>();
		//Map<Integer, ConfigSection> configSections = new HashMap<Integer, ConfigSection>();
		//Map<Integer, Boolean> valueReceived = new HashMap<Integer, Boolean>();
		int configSectionCount;
		int shortestInerterval = 13; //0.1Hz
		int measurementCount = 0;
		
		DataSection dataSection;
		
		public RecordData(final byte[] buffer, final RecordHeader recordHeader, final long timeStamp_ms) {
			super();
			
			configSectionCount = buffer[0 + offset];
//			System.out.println("configSectionCount = " + configSectionCount);
			int configSectionBytes = 0;
			for (int section = 0; section < configSectionCount; section++) {
				ConfigSection configSection = new ConfigSection(buffer, 1 + offset + configSectionBytes);
				shortestInerterval = Math.min(shortestInerterval, configSection.getInterval());
				//this.configSections.put(configSection.getInterval(), configSection);	
				this.configSections.add(configSection);	
				//this.valueReceived.put(configSection.getInterval(), false);			
				configSectionBytes += configSection.getLength();
				measurementCount += configSection.getCount();
			}
			//System.out.println(String.format("header measurement count = %d, config section measurement count = %d", recordHeader.getMeasurementCount(), measurementCount));
			this.offset = configSectionBytes + 4 + 8 + 1 + 1; //length, timeStamp, recordType, configSectionCount
	
			long timeOffset = getTimeOffset(shortestInerterval);
			int count = 0;
			while (this.offset < (buffer.length-2)) { // record contains measurement data
				//System.out.println("\n -> offset = " + this.offset + " buffer.length-2 = " + (buffer.length-2));
				DataSection tmpDataSection = new DataSection(buffer, offset, configSections, recordHeader, timeStamp_ms);
				
				//print values after receiving data section with shortest interval
				if (tmpDataSection.interval == shortestInerterval) {
//					String timeStamp = StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS;", timeStamp_ms + timeOffset*count++);
//					if (timeStamp.equals("2015-10-03 15:38:08.342;"))
//						System.out.println(timeStamp);
					System.out.print(StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS;", timeStamp_ms + timeOffset*count++));
	
					Iterator<Integer> iteratorMeasurementIds = LogReader.measurementIds.iterator();
					
					while (iteratorMeasurementIds.hasNext()) {
						int measurementId = iteratorMeasurementIds.next();
						DataItem dataItem = receivedDataItems.get(measurementId);
						if (dataItem == null) { //data not received 
							switch (LogReader.measurements.get(measurementId).getDataType()) {
							case PaketGPS:
								for (int i = 0; i < 7; i++) {
									System.out.print("-;");
								}
								break;
							case PowerSupply:
								for (int i = 0; i < 28; i++) {
									System.out.print("-;");
								}
								break;
	
							default:
								System.out.print("-;");
								break;
							}					
						}
						else {
							dataItem.printValue(timeStamp_ms);
						}					
					}
					System.out.println();
				}

				
//				if (tmpDataSection.getLength() != tmpDataSection.getReadBytes())
//					System.out.println("tmpDataSection.getLength() != tmpDataSection.getReadBytes()");
				offset += tmpDataSection.getLength(); //tmpDataSection.getReadBytes();
			}
		}
		
	}
	
	/**
	 * RecordEvent is a class to handle all record of the log file of type event
	 */
	public class RecordEvent extends Record {
		
		byte[] eventData;

		public RecordEvent(byte[] buffer) {
			super();
			this.eventData = buffer;
		}
		
	}

	public class LogRecord {
		int length;
		long timeStamp_ms;
		RecordType recordType;
		RecordEvent recordEvent;
		RecordData recordData;
		RecordHeader recordHeader;
		short crc;
		
		public LogRecord( final byte[] buffer, final RecordHeader header) {
			this.recordHeader = header;
			this.length = DataParser.parse2Int(buffer, 0);
			this.timeStamp_ms = DataParser.parse2Long(buffer, 4);
			//System.out.println(StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", this.timeStamp_ms));
			this.recordType = RecordType.values()[buffer[12]];
			switch (this.recordType) {
			case HEADER: //RecordType.HEADER
//				System.out.println("RecordType.HEADER");
				recordHeader = new RecordHeader(buffer);
				break;
			case DATA: //RecordType.DATA
//				System.out.println("RecordType.DATA");
				recordData = new RecordData(buffer, header, this.timeStamp_ms);
				break;
			case EVENT: //RecordType.EVENT
				System.out.println("RecordType.EVENT");
				//TODO structure of record type event is unknown 
				recordEvent = new RecordEvent(buffer);
				break;

			default:
				System.out.println("RecordType_UNKNOWN");
				//TODO unknown record type occurrence, description needed
				break;
			}

		}

		public int getLength() {
			return length;
		}

		public long getTimeStamp_ms() {
			return timeStamp_ms;
		}

		public RecordType getRecordType() {
			return recordType;
		}

		public RecordEvent getRecordEvent() {
			return recordEvent;
		}

		public RecordData getRecordData() {
			return recordData;
		}

		public RecordHeader getRecordHeader() {
			return recordHeader;
		}

		public short getCrc() {
			return crc;
		}
	}
	
	/**
	 * compose the record set extend to give capability to identify source of this record set
	 * @param file
	 * @return
	 */
	protected static String getRecordSetExtend(File file) {
		String recordSetNameExtend = GDE.STRING_EMPTY;
		if (file.getName().contains(GDE.STRING_UNDER_BAR)) {
			try {
				Integer.parseInt(file.getName().substring(0, file.getName().lastIndexOf(GDE.STRING_UNDER_BAR)));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, file.getName().lastIndexOf(GDE.STRING_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (file.getName().substring(0, file.getName().lastIndexOf(GDE.STRING_UNDER_BAR)).length() <= 8)
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, file.getName().lastIndexOf(GDE.STRING_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		else {
			try {
				Integer.parseInt(file.getName().substring(0, 4));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, 4) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (file.getName().substring(0, file.getName().length()).length() <= 8 + 4)
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + file.getName().substring(0, file.getName().length() - 4) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		return recordSetNameExtend;
	}

	/**
	 * read complete file data and display the first found record set
	 * @param filePath
	 * @throws Exception 
	 */
	public static synchronized void read(String filePath) throws Exception {
		final String $METHOD_NAME = "read";
		long startTime = System.nanoTime() / 1000000;
		File file = new File(filePath);
		FileInputStream file_input = new FileInputStream(file);
		DataInputStream data_in = new DataInputStream(file_input);
		long fileSize = file.length();
		long readByteCount = 0;
		WeatronicAdapter device = (WeatronicAdapter) LogReader.application.getActiveDevice();
		int recordSetNumber = LogReader.channels.get(1).maxSize() + 1;
		String recordSetName = GDE.STRING_EMPTY;
		String recordSetNameExtend = getRecordSetExtend(file);
		Channel channel = null;
		boolean isInitialSwitched = false;
		long numberDatablocks = fileSize / LogReader.dataBlockSize;
		LogReader logReader = new LogReader();
		LogRecord logRecord = null; //records red from file
		LogReader.measurementIds.clear();
		LogReader.receivedDataItems.clear();
		LogReader.unknownIds.clear();
		
		long timeStamp = 0;
		
		long startTimeStamp_ms = 0;

		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		MenuToolBar menuToolBar = LogReader.application.getMenuToolBar();
		int progressIndicator = (int) (numberDatablocks / 30);
		if (menuToolBar != null) LogReader.application.setProgress(0, sThreadId);

		try {
			int logRecordCount = 0;
			while (readByteCount < fileSize) {
				buf_length = new byte[4];
				data_in.read(buf_length);
				final int logRecordLength = DataParser.parse2Int(buf_length, 0);
//				System.out.println("logRecordLength = " + logRecordLength);
				buf_log_record = new byte[logRecordLength];
				System.arraycopy(buf_length, 0, buf_log_record, 0, 4);
				readByteCount += 4;
				readByteCount += data_in.read(buf_log_record, 4, logRecordLength-4);
				//TODO CRC check should occur at first
				//boolean isOK = Checksum.CRC16CCITT(buf_log_record, 0, buf_log_record.length-2) == DataParser.parse2UnsignedShort(buf_log_record, buf_log_record.length-2);
				//System.out.println("CRC = " + isOK);
				timeStamp = DataParser.parse2Long(buf_log_record, 4);
				//System.out.println(StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", timeStamp));
				RecordType recordType = RecordType.values()[buf_log_record[12]];
				if (startTimeStamp_ms == 0 && recordType == RecordType.DATA) startTimeStamp_ms = timeStamp;
				
				logRecord = logReader.new LogRecord(buf_log_record, (logRecord == null ? null : logRecord.getRecordHeader()));
				if (menuToolBar != null) LogReader.application.setProgress((int) (readByteCount*100/fileSize), sThreadId);
				logRecordCount +=1;
			}
			System.out.println("logRecordCount = " + logRecordCount);
			System.out.print("unknown Ids = ");
			for (Integer unknownId : LogReader.unknownIds) {
				System.out.print(unknownId + ", ");
			}
			System.out.println();
			

			//read all the data blocks from the file and parse
			for (int i = 0; i < 0; i++) {
				data_in.read(LogReader.buf_length);
				if (LogReader.log.isLoggable(Level.FINE) && i % 10 == 0) {
					LogReader.log.logp(Level.FINE, LogReader.$CLASS_NAME, $METHOD_NAME, StringHelper.fourDigitsRunningNumber(LogReader.buf_length.length));
				}
				if (LogReader.log.isLoggable(Level.FINE)) {
					LogReader.log.logp(Level.FINE, LogReader.$CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex4CharString(LogReader.buf_length, LogReader.buf_length.length));
				}

				LogReader.recordSet.addPoints(LogReader.points, LogReader.timeStep_ms);

				if (menuToolBar != null && i % progressIndicator == 0) LogReader.application.setProgress((int) (i * 100 / numberDatablocks), sThreadId);
			}

			LogReader.log.logp(Level.TIME, LogReader.$CLASS_NAME, $METHOD_NAME, "read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

			if (menuToolBar != null && LogReader.recordSet != null) {
				LogReader.application.setProgress(99, sThreadId);
				if (!isInitialSwitched) {
					LogReader.channels.switchChannel(channel.getName());
					channel.switchRecordSet(recordSetName);
				}
				else {
					device.makeInActiveDisplayable(LogReader.recordSet);
				}
				device.updateVisibilityStatus(LogReader.recordSet, true);

				//write filename after import to record description
				LogReader.recordSet.descriptionAppendFilename(file.getName());

				menuToolBar.updateChannelSelector();
				menuToolBar.updateRecordSetSelectCombo();
				LogReader.application.setProgress(100, sThreadId);
			}
		}
		finally {
			data_in.close();
			data_in = null;
		}

	}

}
