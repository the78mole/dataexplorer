/*
Copyright (c) 2012, Martin FaltiÄ�ko, OndÅ™ej Vagner
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the Jetimodel s.r.o. nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL Martin FaltiÄ�ko, OndÅ™ej Vagner BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package gde.device.powerbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import gde.GDE;
import gde.messages.Messages;
import gde.ui.DataExplorer;

/**
 *
 * @author Martin Falticko
 */
public class TelemetryData {
	static Logger												log							= Logger.getLogger(TelemetryData.class.getName());
  static final Charset WINDOWS_1250 = Charset.forName("Windows-1250");
  static final Charset UTF_8 = Charset.forName("UTF-8");
  static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

	/** Typ dat - 8 bitu */
	public static final int							T_DATA8					= 0;
	/** Typ dat - 16 bitu */
	public static final int							T_DATA16				= 1;
	/** Typ dat - 24 bitu */
	public static final int							T_DATA24				= 4;
	/** Typ dat - Cas */
	public static final int							T_TIME					= 5;
	/** Typ dat - 32 bitu */
	public static final int							T_DATA32				= 8;
	/** Typ dat - GPS */
	public static final int							T_GPS						= 9;
	/** Typ dat - 37 bitu */
	public static final int							T_DATA37				= 12;
	/** Typ dat - event */
	public static final int							T_EVENT					= 16;

	/** start time stamp of the very first telemetryVar.TelemetryItem entry */
	long																startTimeStamp	= 0;

	public static Map<Integer, Integer>	idMap						= new HashMap<Integer, Integer>();

	/**
	 * @return Flag that the data is empty
	 */
	public boolean isEmpty() {
		return this.data.isEmpty();
	}

	public static class TimeVector extends Vector<Long> {
		private static final long	serialVersionUID	= 4860628311229067111L;

		long											firstTimeStamp		= 0;
		long											lastTimeStamp			= 0;
		long											minValue					= Long.MAX_VALUE;
		long											maxValue					= Long.MIN_VALUE;
		long											avgValue					= Long.MIN_VALUE;			// average value (avg = sum(xi)/n)
		long											sigmaValue				= Long.MIN_VALUE;			// sigma value of time steps
		long											maxTimeStamp			= 0;

		public TimeVector() {
			super();
		}

		@Override
		public synchronized boolean add(Long timeStamp) {
			boolean isAdded = false;
			if (timeStamp != 0) {
				if (this.lastTimeStamp != 0 && this.lastTimeStamp < timeStamp) {
					long delta = timeStamp - this.lastTimeStamp;
					//System.out.println("delta = " + delta);
					if (super.size() == 0) {
						this.minValue = this.maxValue = delta;
					}
					else {
						if (delta > this.maxValue)			{this.maxValue = delta; maxTimeStamp = this.lastTimeStamp-this.firstTimeStamp;}
						else if (delta < this.minValue) this.minValue = delta;
					}
					isAdded = super.add(delta);
				}
				else {
					this.firstTimeStamp = timeStamp;
				}
				this.lastTimeStamp = timeStamp;
			}
			return isAdded;
		}

		/**
		 * @return timeStamp max value occurrence
		 */
		public long getMaxValueTimeStamp() {
			return this.maxTimeStamp;
		}
		
		/**
		 * @return the avgValue
		 */
		public long getAvgValue() {
			this.setAvgValue();
			return this.avgValue;
		}

		/**
		 * calculates the avgValue
		 */
		public void setAvgValue() {
			synchronized (this) {
				if (this.size() >= 2) {
					long sum = 0;
					int zeroCount = 0;
					for (Long xi : this) {
						if (xi != 0) {
							sum += xi;
						}
						else {
							zeroCount++;
						}
					}
					this.avgValue = (this.size() - zeroCount) != 0 ? Long.valueOf(sum / (this.size() - zeroCount)).intValue() : 0;
				}
			}
		}

		/**
		 * @return the sigmaValue
		 */
		public long getSigmaValue() {
			this.setSigmaValue();
			return this.sigmaValue;
		}

		/**
		 * calculates the sigmaValue
		 */
		public void setSigmaValue() {
			synchronized (this) {
				if (super.size() >= 2) {
					long average = this.getAvgValue();
					double sumPoweredValues = 0;
					for (Long xi : this) {
						sumPoweredValues += Math.pow(xi - average, 2);
					}
					this.sigmaValue = Double.valueOf(Math.sqrt(sumPoweredValues / (this.size() - 1))).intValue();
				}
			}
		}

		public long getMinValue() {
			return this.minValue;
		}

		public long getMaxValue() {
			return this.maxValue;
		}
	}

	static TimeVector	timeSteps;

	public TelemetryData() {
		TelemetryData.timeSteps = new TimeVector();
	}

	/** describe sensor in use */
	public static class TelemetrySensor implements Comparable<TelemetrySensor> {

		/** sensor ID */
		long									id;
		/** sensor name*/
		String								name;
		/** sensor data  */
		TreeSet<TelemetryVar>	variables	= new TreeSet<TelemetryVar>();

		/**
		 * @return sensor name
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * @return sensor identifier (Tx == 0)
		 */
		public long getId() {
			return this.id;
		}

		/**
		 * @param _id sensor ID
		 * @param _name sensor name
		 */
		public TelemetrySensor(long _id, String _name) {
			this.id = _id;
			this.name = _name;
		}

		/**
		 * add a new variable to the existing sensor
		 * @param v sensor variable
		 */
		public void addVariable(TelemetryVar v) {
			this.variables.add(new TelemetryVar(v));
		}

		/**
		 * Returns a list of all variables
		 * @return List of variable of all sensors
		 */
		public TreeSet<TelemetryVar> getVariables() {
			return this.variables;
		}

		/**
		 * Takes a link from the list of flight values
		 * @param param parameter
		 */
		public TelemetryVar getVar(int param) {
			for (TelemetryVar v : this.variables) {
				if (v.param == param) {
					return v;
				}
			}
			return null;
		}

		/**
		 * Compare two telemetry sensors
		 * @param o comparable value
		 * @return 1...greater
		 */
		@Override
		public int compareTo(TelemetrySensor o) {
			if (this.id > o.id) {
				return 1;
			}
			else if (this.id == o.id) {
				return 0;
			}
			else {
				return -1;
			}
		}
	};

	/**
	 * describe telemetry variable
	 */
	public static class TelemetryVar implements Comparable<TelemetryVar> {

		/** parameter number */
		int												param;
		/** parameter name */
		String										name;
		/** parameter unit */
		String										unit;
		/** list of telemetry values */
		ArrayList<TelemetryItem>	data;
		/** maximum and minimum values */
		double										maxValue	= 0.0, minValue = 0.0;
		/** time steps ms where data added **/
		TimeVector timeSteps;

		/**
		 * constructor telemetry variable
		 * @param _param parameter number
		 * @param _name parameter name
		 * @param _unit parameter unit
		 */
		TelemetryVar(int _param, String _name, String _unit) {
			this.param = _param;
			this.name = _name;
			this.unit = _unit;
			this.data = new ArrayList<TelemetryItem>();
			this.timeSteps = new TimeVector();
		}

		/**
		 * copy constructor
		 * @param e
		 */
		public TelemetryVar(TelemetryVar e) {
			this.param = e.param;
			this.name = e.name;
			this.unit = e.unit;
			this.data = new ArrayList<TelemetryItem>(e.data);
			this.timeSteps = new TimeVector();
		}

		/**
		 * Adds a new telemetry item
		 * @param i data
		 * @param skip if true without time stamp
		 */
		public void addItem(final TelemetryItem i, final boolean skip) {
			this.data.add(new TelemetryItem(i));
			if (!skip) {
				this.timeSteps.add(i.getTimestamp());
				TelemetryData.timeSteps.add(i.getTimestamp());
			}
		}

		/**
		 * Adds a new telemetry item at specified position
		 * @param index position
		 * @param i data
		 * @param skip if true without time stamp
		 */
		public void addItem(final int index, final TelemetryItem i, final boolean skip) {
			this.data.add(index, new TelemetryItem(i));
			if (!skip) {
				this.timeSteps.add(i.getTimestamp());
				TelemetryData.timeSteps.add(i.getTimestamp());
			}
		}

		/**
		 * @return timeSteps of telemetry item
		 */
		public TimeVector getTimeSteps() {
			return this.timeSteps;
		}
		
		/**
		 * @return unit of telemetry item
		 */
		public String getUnit() {
			return this.unit;
		}

		/**
		 * @return name of telemetry item
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * @return maximum value of telemetry item
		 */
		public double getMax() {
			return this.maxValue;
		}

		/**
		 * @return  minimum value of telemetry item
		 */
		public double getMin() {
			return this.minValue;
		}

		/**
		 * @return translated telemetry item to string
		 */
		@Override
		public String toString() {
			return this.name + " \t" + "[" + this.unit + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		/**
		 * Normalizes the time so that it starts from zero. Looks at max and min.
		 * Returns maximum time set
		 */
		public double normamlizeItems() {
			this.maxValue = 0.0;
			this.minValue = 0.0;
			double min = Double.POSITIVE_INFINITY;
			double max = Double.NEGATIVE_INFINITY;
			Collections.sort(this.data);
			long timeFINEset = 0;
			if (this.data.size() > 0) {
				//System.out.println(name);
				timeFINEset = this.data.get(0).getTimestamp();
				for (TelemetryItem row : this.data) {
					double val = row.getDouble();
					min = Math.min(val, min);
					max = Math.max(val, max);
					row.setTimestamp(row.getTimestamp() - timeFINEset);
				}
				this.maxValue = max;
				this.minValue = min;
				return this.data.get(this.data.size() - 1).timestamp / 1000.0;
			}
			return 0.0;
		}

		/**
		 * @param o telemetry variable
		 * @return Comparison of two telemetry variables
		 */
		@Override
		public int compareTo(TelemetryVar o) {
			return this.param - o.param;
		}

		/**
		 * @return parameter values
		 */
		public int getDecimals() {
			if (this.data.size() > 0) return this.data.get(0).getDecimals();

			return 0;
		}

		/**
		 * @return parameter type (1-15)
		 */
		public int getType() {
			if (this.data.size() > 0) return this.data.get(0).getType();

			return 0;
		}

		/**
		 * @return the value  approximated to the given time
		 */
		public double getDoubleAt(double time_ms) {
			if (this.data.size() == 0) {
				return 0;
			}
			else if (time_ms >= this.data.get(this.data.size() - 1).timestamp) {
				return this.data.get(this.data.size() - 1).getDouble();
			}
			else if (time_ms <= 0) {
				return this.data.get(0).getDouble();
			}
			else {
				//interpolates between the most recent time stamps
				for (int i = 0; i < this.data.size() - 1; i++) {
					TelemetryItem i1, i2;
					i1 = this.data.get(i);
					i2 = this.data.get(i + 1);
					if (i1.timestamp <= time_ms && i2.timestamp > time_ms) {
						if (i1.timestamp == i2.timestamp) {
							return i1.getDouble();
						}

						double interv = (time_ms - i1.timestamp) / (i2.timestamp - i1.timestamp);
						double delta = interv * (i2.getDouble() - i1.getDouble());
						return i1.getDouble() + delta;
					}
				}
				return 0;
			}
		}

		/**
		 * @param time
		 * @return potential course
		 */
		public double getDoubleCourseAt(double time) {
			time = time * 1000;
			if (this.data.size() == 0) {
				return 0;
			}
			else if (time >= this.data.get(this.data.size() - 1).timestamp) {
				return this.data.get(this.data.size() - 1).getDouble();
			}
			else if (time <= 0) {
				return this.data.get(0).getDouble();
			}

			//interpolates between the most recent time stamps
			for (int i = 0; i < this.data.size() - 1; i++) {
				TelemetryItem i1, i2;
				i1 = this.data.get(i);
				i2 = this.data.get(i + 1);
				if (i1.timestamp <= time && i2.timestamp > time) {
					if (i1.timestamp == i2.timestamp) {
						return i1.getDouble();
					}
					double interv = (time - i1.timestamp) / (i2.timestamp - i1.timestamp);
					double d1 = i1.getDouble();
					double d2 = i2.getDouble();
					if (Math.abs(d2 - d1) > 180) {
						return d1 + interv * (d2 - d1 + (d2 > d1 ? -360 : 360));
					}
					return d1 + interv * (d2 - d1);
				}
			}
			return 0;
		}

		/**
		 * @param time
		 * @return integer value matching given time
		 */
		public double getIntAt(double time_ms) {
			if (this.data.size() == 0) {
				return 0;
			}
			else if (time_ms >= this.data.get(this.data.size() - 1).timestamp) {
				return this.data.get(this.data.size() - 1).getInt();
			}
			else if (time_ms <= 0) {
				return this.data.get(0).getInt();
			}
			else {
				//interpolates between the most recent time stamps
				for (int i = 0; i < this.data.size(); i++) {
					TelemetryItem i1;
					i1 = this.data.get(i);
					if (i1.timestamp >= time_ms) {
						return i1.getInt();
					}
				}
				return 0;
			}
		}

		/**
		 * @return list of telemetry items
		 */
		public ArrayList<TelemetryItem> getItems() {
			return this.data;
		}

		/**
		 * @return the parameter
		 */
		public synchronized int getParam() {
			return this.param;
		}
	}

	/** Telemetry Record Item */
	public static class TelemetryItem implements Comparable<TelemetryItem> {

		/** data type(1-15) */
		private int		dataType;
		/** decimals */
		private int		decimals;
		/** value */
		private int		value;
		/** time stamp */
		private long	timestamp;

		/**
		 * Konstruktor
		 * @param type dataType
		 * @param dec decimals
		 * @param _value value
		 * @param _timestamp time stamp
		 */
		public TelemetryItem(int type, int dec, int _value, long _timestamp) {
			this.dataType = type;
			this.decimals = dec;
			this.value = _value;
			this.timestamp = _timestamp;
		}

		/**
		 * copy constructor
		 * @param i
		 */
		TelemetryItem(TelemetryItem i) {
			this.dataType = i.dataType;
			this.decimals = i.decimals;
			this.value = i.value;
			this.timestamp = i.timestamp;
		}

		/**
		 *
		 * @return data type
		 */
		public int getType() {
			return this.dataType;
		}

		/**
		 * @return value as double
		 */
		public double getDouble() {
			switch (this.dataType) {
			case T_DATA8:
			case T_DATA16:
			case T_DATA24:
			case T_DATA32:
			case T_DATA37:
				return this.value * Math.pow(10, -this.decimals);
			case T_GPS:
				double minute = (this.value & 0xFFFF) / 1000.0;
				double stupne = (this.value >> 16) & 0xFF;
				stupne = stupne + minute / 60.0;
				return stupne * (((this.decimals >> 1) & 1) == 1 ? -1 : 1);
			default:
				return 0.0;
			}
		}

		/**
		 * @return value as integer
		 */
		public double getInt() {
			switch (this.dataType) {
			case T_DATA8:
			case T_DATA16:
			case T_DATA24:
			case T_DATA32:
			case T_DATA37:
				return this.value * Math.pow(10, -this.decimals);
			case T_GPS:
				return this.value * Math.pow(10, -7);
//				int grad = ((int)(value / 1000));
//				double minuten = (value - (grad*1000.0))/10.0;
//				newValue = grad + minuten/60.0;

//				int grad = (int)value;
//				double minuten =  (value - grad*1.0) * 60.0;
//				newValue = (grad + minuten/100.0)*1000.0;


//				if (value > 0) {
//					int grad = this.value / 10000000;
//					double minutes = (this.value - grad * 10000000) * 60. / 10000000.;
//					//log.log(Level.OFF, String.format("%d %7.4f", grad, minutes));
//					return (grad + minutes / 100.0);
//				}
//				return 0.;
//				double stupne = (this.value >> 16) & 0xFF;
//				stupne = stupne + minute / 60.0;
//				return stupne * (((this.decimals >> 1) & 1) == 1 ? -1 : 1);
			default:
				return 0.0;
			}
		}

		/**
		 * @return Compare two telemetry items
		 */
		@Override
		public int compareTo(TelemetryItem o) {
			if (this.timestamp > o.timestamp) {
				return 1;
			}
			else if (this.timestamp == o.timestamp) {
				return 0;
			}
			else {
				return -1;
			}
		}

		/**
		 * @return time stamp as long
		 */
		public long getTimestamp() {
			return this.timestamp;
		}

		/**
		 * set time stamp
		 * @param l
		 */
		private void setTimestamp(long l) {
			this.timestamp = l;
		}

		/**
		 *
		 * @return Number of decimal places
		 */
		private int getDecimals() {
			return this.decimals;
		}
	}

	/**
	 * Structure with telemetry sensor data 
	 */
	private TreeSet<TelemetrySensor>	data					= new TreeSet<TelemetrySensor>();
	/**
	 * Maximal time stamp - total count of milliseconds
	 */
	private double										maxTimestamp	= 0.0;

	private String										appVer			= "";														//$NON-NLS-1$
	private String										tcFwVer			= "";														//$NON-NLS-1$
	private String										scFwVer			= "";														//$NON-NLS-1$
	private String										modelName		= "";														//$NON-NLS-1$
	private boolean										isSensorTable = false;
	private boolean										isStartLogEntries = false;
	private long											startTime_ms;

	/**
	 * @return a list of loaded telemetry data
	 */
	public TreeSet<TelemetrySensor> getData() {
		return this.data;
	}

	/**
	 * query the name of the model contained in telemetry data
	 * @return
	 */
	public String getAppVer() {
		return this.appVer;
	}

	/**
	 * query the name of the model contained in telemetry data
	 * @return
	 */
	public String getTcFWVer() {
		return this.tcFwVer;
	}

	/**
	 * query the name of the model contained in telemetry data
	 * @return
	 */
	public String getScFWVer() {
		return this.scFwVer;
	}

	/**
	 * query the name of the model contained in telemetry data
	 * @return
	 */
	public String getModelName() {
		return this.modelName;
	}

	/**
	 * @return the sensor according to the given ID
	 */
	public TelemetrySensor getSensor(long id) {
		for (TelemetrySensor s : this.data) {
			if (s.id == id) {
				return s;
			}
		}
		return null;
	}

	/**
	   * @return Maximum time during flight
	   */
	public double getMaxTimestamp() {
		return this.maxTimestamp;
	}

	/**
	 * load data from file. Supported is *.log or *.jml
	 * @param file
	 */
	public boolean loadData(String file) {
		TelemetryData.idMap.clear();
		this.maxTimestamp = 0.0;
		int mid = file.lastIndexOf("."); //$NON-NLS-1$
		String ext = file.substring(mid + 1, file.length());
		if (ext.equalsIgnoreCase("log")) { //$NON-NLS-1$
			this.data.clear();
			if (!loadCSV(file)) {
				return false;
			}
		}
		else {
			DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGE2950, new String[] { file }));
			return false;
		}

		//Now all the items and times, data
		for (TelemetrySensor s : this.data) {
			for (TelemetryVar d : s.variables) {
				this.maxTimestamp = Math.max(this.maxTimestamp, d.normamlizeItems());
			}
		}
		return true;
	}

	/**
	 * uploads a CSV file. 
	 * @return info on success
	 */
	boolean loadCSV(String file) {
		int line = 0;
		try {
			FileInputStream fis = new FileInputStream(file);
			InputStreamReader in = new InputStreamReader(fis, "ISO-8859-1"); //$NON-NLS-1$

			long inputFileSize = new File(file).length();
			int progressLineLength = 0;

			BufferedReader br = new BufferedReader(in);

			String strLine;
			while ((strLine = br.readLine()) != null) {
				line++;
				strLine = strLine.trim();
				/*First character - commentary?
				#AppVer=1.95
				#TCfwVer=1.21
				#SCfwVer=1.4
				#Model=X-Perience Pro@0x5F33C7FF
				#SensorsTable
				*/
				if (strLine.length() == 0 || strLine.startsWith("#")) { //$NON-NLS-1$
					if (strLine.startsWith("#AppVer"))
						this.appVer = strLine.substring(8).trim();
					else if (strLine.startsWith("#TCfwVer"))
						this.tcFwVer = strLine.substring(9).trim();
					else if (strLine.startsWith("#SCfwVer"))
						this.scFwVer = strLine.substring(9).trim();
					else if (strLine.startsWith("#Model")) {						
						this.modelName = strLine.substring(7).trim().split("@")[0];
					}
					else if (strLine.startsWith("#SensorsTable")) {
						this.isSensorTable = true;
						this.isStartLogEntries = false;
						log.log(Level.FINER, "SensorsTable");
					}
					else if (strLine.startsWith("#Time")) {
						this.startTime_ms = Long.parseLong(strLine.substring(8).trim(), 16) * 1000;
						this.isSensorTable = false;
						this.isStartLogEntries = true;
						//log.log(Level.OFF, "Time = " + StringHelper.getFormatedTime("YYYY-MM-dd hh:mm:ss.SSS", startTime_ms));
					}
					continue;
				}
				
				if (isSensorTable) //patch time stamp 0 to enable adding sensor
					strLine = "000000000;" + strLine;
				else if (isStartLogEntries && strLine.startsWith(":")) {
					strLine = String.format("%09d;%s", this.startTime_ms + Integer.parseInt(strLine.substring(1, 3)) * 1000, strLine.substring(4));
				}

				progressLineLength = progressLineLength > strLine.length() ? progressLineLength : strLine.length();
				int progress = (int) (line * 100 / (inputFileSize / progressLineLength));
				if (progress <= 90 && progress > GDE.getUiNotification().getProgressPercentage() && progress % 10 == 0) {
					GDE.getUiNotification().setProgress(progress);
					try {
						Thread.sleep(2);
					}
					catch (Exception e) {
						// ignore
					}
				}

				List<String> array = new ArrayList<String>();
				array.addAll(Arrays.asList(strLine.replace("|", ";").split(";")));
				if (array != null && array.size() > 0) {
					if (array.size() == 4) { //only sensors/variables may have 4 entries in array while missing a unit 
						log.log(Level.WARNING, String.format("Sensor data unknown! - %s", array));
						array.add(GDE.STRING_EMPTY);
						continue;
					}
					//if (!array.get(0).equals("000000000")) //print sensor measurements
					//	log.log(Level.OFF, array.toString());
					parseLineParams(array.toArray(new String[5]));
				}
			}
			in.close();
			return true;
		}
		catch (Exception e) {//Catch exception if any
			this.getData().clear();
			TelemetryData.log.log(Level.SEVERE, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGE2952, new String[] { file, String.valueOf(line) }));
			return false;
		}
	}

	/**
	 * parse string array into parameter
	 */
	void parseLineParams(String params[]) {
		final int ST_TIME = 0;
		final int ST_DEVICE_ID = 1;
		final int ST_PARAM_NUM = 2;
		final int ST_START_INDEX = 3;
		final int ST_DECIMALS = 4;
		final int ST_VALUES = 5;
		final int ST_SENSOR = 6;
		final int ST_LABEL = 7;
		final int ST_UNIT = 8;

		int state = ST_TIME;
		long timestamp = 0;
		long deviceId = 0;
		int paramId = 0;
		int dataType = 0;
		int decimals = 0;
		String sensor = ""; //$NON-NLS-1$
		String label = ""; //$NON-NLS-1$
		String unit = ""; //$NON-NLS-1$
		if (params == null) {
			return;
		}
		for (String param : params) {
			switch (state) {
			case ST_TIME:
				timestamp = Long.parseLong(param);
				state = ST_DEVICE_ID;
				break;
			case ST_DEVICE_ID:
				//device id sometimes ;42020    1; LiVa or | 0   0| Alarm: Cap..
				try {
					deviceId = Long.parseLong(!(param.startsWith(" ") && param.endsWith("0"))  ? param.replace(' ', '0') : param);
				}
				catch (NumberFormatException e) {
					log.log(Level.WARNING, "skip | param = " + param);
				}
				state = ST_PARAM_NUM;
				break;
			case ST_PARAM_NUM:
				paramId = Integer.parseInt(param);
				if (timestamp == 0) {
					state = ST_SENSOR;
				}
				else {
					state = ST_START_INDEX;
				}
				break;
			case ST_SENSOR:
				sensor = param;
				//Insert a new sensor and exit the queue
				if (timestamp == 0 && paramId == 0) {
					if (TelemetryData.log.isLoggable(Level.FINER)) TelemetryData.log.log(Level.OFF, "adding sensor " + sensor);
					TelemetrySensor telemetrySensor = new TelemetrySensor(deviceId, sensor);
					this.data.add(telemetrySensor);
				}
				//call the parameter label
				state = ST_LABEL;
				break;
			case ST_LABEL:
				label = param;
				//call the parameter unit
				state = ST_UNIT;
				break;
			case ST_UNIT:
				unit = param;
				TelemetryVar var = new TelemetryVar(paramId, label, unit);
				TelemetrySensor s = this.getSensor(deviceId);
				if (s != null) {
					if (TelemetryData.log.isLoggable(Level.FINER)) TelemetryData.log.log(Level.OFF, String.format("%s %03d add variable %s[%s] ID=%d", s.getName(), deviceId, var.name, unit, paramId));
					s.addVariable(var);
				}
				//no function
				return;
			case ST_START_INDEX:
				state = ST_VALUES;
				break;
			case ST_DECIMALS:
				state = ST_VALUES;
				break;
			case ST_VALUES:
				int val = 0;
				try {
					if (param.startsWith("0x")) {
						switch (param.length()) {
						case 10: //GPS coordinate
							//log.log(Level.OFF, paramId + " param = " + param);
							dataType = TelemetryData.T_GPS;
							val = Integer.valueOf(param.substring(2), 16);
							break;

						case 6: //GPS other value
						default:
							dataType = TelemetryData.T_DATA16;
							val = Integer.valueOf(param.substring(2), 16).shortValue();
							break;
						}
					}
					else {
						//paramId += 1;				//skip for first GOE coordinate
						state = ST_VALUES;
						break;
					}
						
				}
				catch (Exception e) {
					//TODO
					log.log(Level.SEVERE, "Failed parsing " + param);
				}
				//Try to insert a new record
				TelemetrySensor sen = this.getSensor(deviceId);
				if (sen != null) {
					TelemetryVar par = sen.getVar(paramId);
					if (par != null) {
						switch (par.getUnit()) {
						case "m":
						case "A":
						case "m/s":
							decimals=3;
							break;
							
						case "V":
						case "km/h":
						case "dbm":
						case "g":
							decimals=2;
							break;
							
						case "°C":
						case "hPa":
							decimals=1;
							break;

						default:
							if (dataType == TelemetryData.T_GPS) {
								par.unit = "°";
								String parameterName = par.getName().toLowerCase();
								if (parameterName.startsWith("long") || parameterName.startsWith("läng")) { //longitude längengrad
									decimals = 1;
								}
								else 
									decimals=0;
							} 
							else
								decimals=0;
							break;
						}
						if (TelemetryData.log.isLoggable(Level.FINER) && deviceId == 200 && paramId >= 20) //GPS-Logger Höhe
							log.log(Level.OFF, String.format("TelemetryData: deviceId=%03d, paramId=%02d, value=%d, decimals=%d timeStamp=%d", deviceId, paramId, val, decimals, timestamp));
						TelemetryItem item = new TelemetryItem(dataType, decimals, val, timestamp);

						if (TelemetryData.log.isLoggable(Level.FINER)) TelemetryData.log.log(Level.FINER, "add sensor variable value " + par.name + "=" + item.value);
//						if (deviceId == 200 && paramId == 01)
//							log.log(Level.OFF, String.format("TelemetryData: deviceId=%03d, paramId=%02d, name= %s, untit='%s', value=%d, timeStamp=%d", deviceId, paramId, par.name, par.unit, val, timestamp));
						par.addItem(item, false);
						if (this.startTimeStamp == 0) {
							if (TelemetryData.log.isLoggable(Level.FINER)) TelemetryData.log.log(Level.OFF, "set startTimeStamp = " + timestamp);
							this.startTimeStamp = timestamp;
						}
					}
				}
				paramId += dataType == TelemetryData.T_GPS ? 2 : 1;				
				state = ST_VALUES;
				break;
			}
		}
	}

	public static int getParameterIdByName(final TelemetrySensor sensor, final String label) {
		int labelParamId = 0;
		for (int i = 0; i < label.length(); i++) {
			labelParamId += label.charAt(i);
		}
		if (TelemetryData.idMap.get(labelParamId) == null) {
			int newPartamId = sensor.getVariables().size() + 1;
			TelemetryData.idMap.put(labelParamId, newPartamId);
		}
		return TelemetryData.idMap.get(labelParamId);
	}

	public long getMinTimeStep() {
		return TelemetryData.timeSteps.getMinValue();
	}

	public long getMaxTimeStep() {
		return TelemetryData.timeSteps.getMaxValue();
	}

	public long getSigmaTimeStep() {
		return TelemetryData.timeSteps.getSigmaValue();
	}

	public long getAvgTimeStep() {
		return TelemetryData.timeSteps.getAvgValue();
	}

}
