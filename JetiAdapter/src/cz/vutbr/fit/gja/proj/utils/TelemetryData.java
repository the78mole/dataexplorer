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
package cz.vutbr.fit.gja.proj.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.swt.SWT;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import gde.GDE;
import gde.device.jeti.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.TimeLine;

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
	 * Vrati priznak, jestli jsou data naplnena
	 * @return Priznak, ze data nejsou prazdna
	 */
	public boolean isEmpty() {
		return this.data.isEmpty();
	}

	public static class TimeVector extends Vector<Long> {
		private static final long	serialVersionUID	= 4860628311229067111L;

		long											lastTimeStamp			= 0;
		long											minValue					= Long.MAX_VALUE;
		long											maxValue					= Long.MIN_VALUE;
		long											avgValue					= Long.MIN_VALUE;			// avarage value (avg = sum(xi)/n)
		long											sigmaValue				= Long.MIN_VALUE;			// sigma value of time steps

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
						if (delta > this.maxValue)			this.maxValue = delta;
						else if (delta < this.minValue) this.minValue = delta;
					}
					isAdded = super.add(delta);
				}
				this.lastTimeStamp = timeStamp;
			}
			return isAdded;
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

	/** Info o pouzitem senzoru */
	public static class TelemetrySensor implements Comparable<TelemetrySensor> {

		/** ID senzoru */
		long									id;
		/** Nazev senzoru */
		String								name;
		/** Promenne senzoru, udaje, ktere jsou pouzite */
		TreeSet<TelemetryVar>	variables	= new TreeSet<TelemetryVar>();

		/**
		 * Nazev senzoru
		 * @return Nazev senzoru
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
		 * Konstruktor
		 * @param _id ID senzoru
		 * @param _name Nazev senzoru
		 */
		public TelemetrySensor(long _id, String _name) {
			this.id = _id;
			this.name = _name;
		}

		/**
		 * Vlozi novou promennou k existujisimu senzoru
		 * @param v promenna senzoru
		 */
		public void addVariable(TelemetryVar v) {
			this.variables.add(new TelemetryVar(v));
		}

		/*
		 * Vrati seznam vsech promennych
		 * @return Seznam promennych daneho senzoru
		 */
		public TreeSet<TelemetryVar> getVariables() {
			return this.variables;
		}

		/**
		 * Ziska odkaz ze seznamu letovych hodnot
		 * @param param parametr
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
		 * Porovnani dvou telem. senzoru
		 * @param o Porovnavana hodnota
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
	 * Info o zobrazene promenne
	 */
	public static class TelemetryVar implements Comparable<TelemetryVar> {

		/** Cislo parametru*/
		int												param;
		/** Jmeno parametru */
		String										name;
		/** Jednotka promenne parametru */
		String										unit;
		/** Casosberna data prirazena k danemu parametru */
		ArrayList<TelemetryItem>	data;
		/**Maximalni a minimalni hodnoty */
		double										maxValue	= 0.0, minValue = 0.0;

		/**
		 * Konstruktor
		 * @param _param cislo parametru
		 * @param _name jmeno parametru
		 * @param _unit jednotka
		 */
		TelemetryVar(int _param, String _name, String _unit) {
			this.param = _param;
			this.name = _name;
			this.unit = _unit;
			this.data = new ArrayList<TelemetryItem>();
		}

		/**
		 * Kopirovaci konstruktor
		 * @param e
		 */
		public TelemetryVar(TelemetryVar e) {
			this.param = e.param;
			this.name = e.name;
			this.unit = e.unit;
			this.data = new ArrayList<TelemetryItem>(e.data);
		}

		/**
		 * Prida novou polozku do casosbernych dat
		 * @param i data
		 */
		public void addItem(final TelemetryItem i, final boolean skip) {
			this.data.add(new TelemetryItem(i));
			if (!skip) TelemetryData.timeSteps.add(i.getTimestamp());
		}

		/**
		 * Prida novou polozku do casosbernych dat
		 * @param i data
		 */
		public void addItem(final int index, final TelemetryItem i, final boolean skip) {
			this.data.add(index, new TelemetryItem(i));
			if (!skip) TelemetryData.timeSteps.add(i.getTimestamp());
		}

		/**
		 * @return Jednotka promenne
		 */
		public String getUnit() {
			return this.unit;
		}

		/**
		 * @return Jmeno promenne
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * @return Maximalni hodnota promenne
		 */
		public double getMax() {
			return this.maxValue;
		}

		/**
		 * @return Minimalni hodnota promenne
		 */
		public double getMin() {
			return this.minValue;
		}

		/**
		 * Prepis na retezec
		 * @return
		 */
		@Override
		public String toString() {
			return this.name + " \t" + "[" + this.unit + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		/*
		 * Normalizuje cas, tak aby zacinal od nuly.Vyhleda max a min.
		 * Vraci maximalni dosazeny cas
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
		 * Porovnani dvou promennych
		 * @param o
		 * @return
		 */
		@Override
		public int compareTo(TelemetryVar o) {
			return this.param - o.param;
		}

		/**
		 *
		 * @return Pocet desetinnych mist
		 */
		public int getDecimals() {
			if (this.data.size() > 0) return this.data.get(0).getDecimals();

			return 0;
		}

		/**
		 *
		 * @return Typ promenne (1-15)
		 */
		public int getType() {
			if (this.data.size() > 0) return this.data.get(0).getType();

			return 0;
		}

		/**
		 *
		 * @return Hodnota double aproximovana v zadanem case
		 */
		public double getDoubleAt(double time) {
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
			else {
				//interpoluje mezi nejblizsimi casovymi znackami
				for (int i = 0; i < this.data.size() - 1; i++) {
					TelemetryItem i1, i2;
					i1 = this.data.get(i);
					i2 = this.data.get(i + 1);
					if (i1.timestamp <= time && i2.timestamp > time) {
						if (i1.timestamp == i2.timestamp) {
							return i1.getDouble();
						}

						double interv = (time - i1.timestamp) / (i2.timestamp - i1.timestamp);
						return i1.getDouble() + interv * (i2.getDouble() - i1.getDouble());
					}
				}
				return 0;
			}
		}

		/**
		 * Vrati uhlovy kurz v zadanem casu
		 * @param time cas
		 * @return vypocteny cas
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

			//interpoluje mezi nejblizsimi casovymi znackami
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
		 * Vrati hodnotu tyu Int v zadanem case
		 * @param time
		 * @return
		 */
		public int getIntAt(double time) {
			time = time * 1000;
			if (this.data.size() == 0) {
				return 0;
			}
			else if (time >= this.data.get(this.data.size() - 1).timestamp) {
				return this.data.get(this.data.size() - 1).getInt();
			}
			else if (time <= 0) {
				return this.data.get(0).getInt();
			}
			else {
				//interpoluje mezi nejblizsimi casovymi znackami
				for (int i = 0; i < this.data.size(); i++) {
					TelemetryItem i1;
					i1 = this.data.get(i);
					if (i1.timestamp >= time) {
						return i1.getInt();
					}
				}
				return 0;
			}
		}

		/**
		 *
		 * @return Seznam casosbernych dat
		 */
		public ArrayList<TelemetryItem> getItems() {
			return this.data;
		}

		/**
		 * @return the param
		 */
		public synchronized int getParam() {
			return this.param;
		}
	}

	/** Polozka zaznamu telemetrie */
	public static class TelemetryItem implements Comparable<TelemetryItem> {

		/** Typ dat(1-15) */
		private int		dataType;
		/** Pocet deset. mist */
		private int		decimals;
		/** Hodnota */
		private int		value;
		/** casove razitko od zacatku letu */
		private long	timestamp;

		/**
		 * Konstruktor
		 * @param type Typ
		 * @param dec pocet des. mist
		 * @param _value hodnota
		 * @param _timestamp casove razitko
		 */
		public TelemetryItem(int type, int dec, int _value, long _timestamp) {
			this.dataType = type;
			this.decimals = dec;
			this.value = _value;
			this.timestamp = _timestamp;
		}

		/**
		 * Kopirovaci konstruktor
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
		 * @return Zjisti typ dat
		 */
		public int getType() {
			return this.dataType;
		}

		/**
		 * Vrati udaj typu Double
		 * @return aktualni hodnota
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
		 * Vrati udaj typu Int
		 */
		public int getInt() {
			return this.value;
		}

		/**
		 *
		 * @return Porovnani dvou objektu
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
		 *
		 * @return casove razitko  vsekundach
		 */
		public long getTimestamp() {
			return this.timestamp;
		}

		/**
		 * Nastavi casove razitko
		 * @param l
		 */
		private void setTimestamp(long l) {
			this.timestamp = l;
		}

		/**
		 *
		 * @return Pocet desetinnych mist
		 */
		private int getDecimals() {
			return this.decimals;
		}
	}

	/**
	 * Struktura s telemetrickymi udaji
	 */
	private TreeSet<TelemetrySensor>	data					= new TreeSet<TelemetrySensor>();
	/**
	 * Maximalni casova znacka - celkovy pocet milisekund zaznamu
	 */
	private double										maxTimestamp	= 0.0;

	private String										modelName			= "";														//$NON-NLS-1$

	/**
	 * Vrati seznam s nactenymi telemetrickymi daty
	 * @return
	 */
	public TreeSet<TelemetrySensor> getData() {
		return this.data;
	}

	/**
	 * query the name of the model contained in telemetry data
	 * @return
	 */
	public String getModelName() {
		return this.modelName;
	}

	/**
	 * Vrati senzor podle zadaneho ID
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
	   *
	   * @return Maximalni cas behem letu
	   */
	public double getMaxTimestamp() {
		return this.maxTimestamp;
	}

	/**
	 * Nacte data ze souboru. Pozna, jestli se jedna o *.log nebo *.xml
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
		else if (ext.equalsIgnoreCase("jml")) { //$NON-NLS-1$
			this.data.clear();
			if (!loadJML(file)) {
				return false;
			}
		}
		else {
			DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGE2900, new String[] { file }));
			//JOptionPane.showMessageDialog(null, "Error: " + "NeznÃ¡mÃ¡ koncovka souboru", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		//Nyni prepocita vsechny polozky a cas. udaje
		for (TelemetrySensor s : this.data) {
			for (TelemetryVar d : s.variables) {
				this.maxTimestamp = Math.max(this.maxTimestamp, d.normamlizeItems());
			}
		}
		return true;
	}

	/**
	 * Nahraje soubor jml, ktery je zalozeny na XML
	 * @param file nazev souboru
	 * @return
	 */
	boolean loadJML(String filename) {
		try {
			File file = new File(filename);

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();

			NodeList sensors = doc.getElementsByTagName("dataStorage"); //$NON-NLS-1$

			for (int s = 0; s < sensors.getLength(); s++) {
				Node sensor = sensors.item(s);

				if (sensor.getNodeType() == Node.ELEMENT_NODE) {
					Element fstElmnt = (Element) sensor;
					long ID = Long.parseLong(fstElmnt.getAttribute("dataStorageID")); //$NON-NLS-1$
					//Vlozim novy sensor
					TelemetrySensor tel = new TelemetrySensor(ID, "-"); //$NON-NLS-1$
					this.data.add(tel);
					//Projdu atributy
					NodeList elements = fstElmnt.getElementsByTagName("attrDescription"); //$NON-NLS-1$
					for (int i = 0; i < elements.getLength(); i++) {
						Node var = elements.item(i);
						if (var.getNodeType() == Node.ELEMENT_NODE) {
							Element varElem = (Element) var;
							int varId = Integer.parseInt(varElem.getAttribute("attrID")); //$NON-NLS-1$
							String name = varElem.getAttribute("name"); //$NON-NLS-1$
							String unit = varElem.getAttribute("units"); //$NON-NLS-1$
							TelemetryVar telvar = new TelemetryVar(varId, name, unit);
							//Vlozim promennou telemetrie
							tel.addVariable(telvar);
						}
					}

					//Projdu data k danemu cidlu
					elements = fstElmnt.getElementsByTagName("entity"); //$NON-NLS-1$
					for (int i = 0; i < elements.getLength(); i++) {
						Node var = elements.item(i);
						if (var.getNodeType() == Node.ELEMENT_NODE) {
							Element varElem = (Element) var;
							String row = String.valueOf(ID) + ";" + varElem.getAttribute("plainData"); //$NON-NLS-1$ //$NON-NLS-2$
							String rowData[] = row.split(";"); //$NON-NLS-1$
							if (rowData.length > 2) {
								//Prehodim ID a timestamp
								String tmp = rowData[1];
								rowData[1] = rowData[0];
								rowData[0] = tmp;
								parseLineParams(rowData);
							}
						}
					}
				}
				int progress = s * 100 / (sensors.getLength() * 2 / 3);
				if (progress <= 90 && progress > GDE.getUiNotification().getProgressPercentage() && progress % 10 == 0) {
					GDE.getUiNotification().setProgress(progress);
					try {
						Thread.sleep(2);
					}
					catch (Exception e) {
						// ignore
					}
				}

			}
			return true;
		}
		catch (Exception e) {
			this.getData().clear();
			TelemetryData.log.log(Level.SEVERE, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGE2901, new String[] { filename }));
			//JOptionPane.showMessageDialog(null, "Chyba JML, " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	/**
	 * Nahraje soubor CSV. Vraci info o uspechu
	 */
	boolean loadCSV(String file) {
		int line = 0;
		try {
			// Open the file that is the first
			// command line parameter
			FileInputStream fis = new FileInputStream(file);
			InputStreamReader in = new InputStreamReader(fis, "ISO-8859-1"); //$NON-NLS-1$

			long inputFileSize = new File(file).length();
			int progressLineLength = 0;

			//FileInputStream fstream = new FileInputStream(file);
			// Get the object of DataInputStream
			//DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(in);

			String strLine;
			//Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				line++;
				//strLine = strLine.trim();
				//Prvni znak - komentar?
				if (strLine.startsWith("#")) { //$NON-NLS-1$
					this.modelName = strLine.substring(1).trim();
					continue;
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

				String arr[] = strLine.replace("|", ";").split(";"); //$NON-NLS-1$
				if (arr != null && arr.length > 0) {
					parseLineParams(arr);
				}
			}
			//Close the input stream
			in.close();
			return true;
		}
		catch (Exception e) {//Catch exception if any
			this.getData().clear();
			TelemetryData.log.log(Level.SEVERE, e.getMessage(), e);
			DataExplorer.getInstance().openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGE2902, new String[] { file, String.valueOf(line) }));
			//JOptionPane.showMessageDialog(null, "Chyba na Å™Ã¡dku " + String.valueOf(line) + ", " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	void loadActual() {
		/*int prepMsg_TxExtRxEnable(uchar *msg)
		{
		int len=0;
		unsigned short ui;
		// 3E 02 14 00 0F 16 41 01 00 01 42 01 00 01 47 01 00 01 ( 2B- CRC)
		  msg[len++]=0x3E;
		  msg[len++]=0x02;
		  msg[len++]=0x00; // dÃ©lka (doplnÃ­ se pozdÄ›ji)
		  msg[len++]=0x00; // vyÅ¡Å¡Ã­ bajt dÃ©lky
		  msg[len++]=0x0F; // counter
		  msg[len++]=0x16;
		  msg[len++]=0x41; msg[len++]=0x01; msg[len++]=0x00; msg[len++]=0x01;
		  msg[len++]=0x42; msg[len++]=0x01; msg[len++]=0x00; msg[len++]=0x01;
		  msg[len++]=0x47; msg[len++]=0x01; msg[len++]=0x00; msg[len++]=0x01;
		  msg[2]=len+2; // Ãºprava bajtu s dÃ©lkou
		  ui=getCrc16(msg,len);
		  msg[len++]=LOBYTE(ui);
		  msg[len++]=HIBYTE(ui);
		  return(len);
		}*/
	}

	/**
	 * Projede zadany seznam retezcu - ziskany radek
	 */
	void parseLineParams(String params[]) {
		final int ST_TIME = 0;
		final int ST_DEVICE_ID = 1;
		final int ST_PARAM_NUM = 2;
		final int ST_DATATYPE = 3;
		final int ST_DECIMALS = 4;
		final int ST_VALUE = 5;
		final int ST_LABEL = 6;
		final int ST_UNIT = 7;

		int state = ST_TIME;
		long timestamp = 0;
		long deviceId = 0;
		int paramId = 0;
		int dataType = 0;
		int decimals = 0;
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
					state = ST_LABEL;
				}
				else {
					state = ST_DATATYPE;
				}
				break;
			case ST_LABEL:
				label = param;
				//Vlozeni noveho cidla a ukonceni nacitani radku
				if (timestamp == 0 && paramId == 0) {
					if (TelemetryData.log.isLoggable(Level.FINE)) TelemetryData.log.log(Level.FINE, "adding sensor " + label);
					TelemetrySensor sensor = new TelemetrySensor(deviceId, label);
					this.data.add(sensor);
					return;
				}
				//Nyni nacitam popisek parametru
				state = ST_UNIT;
				break;
			case ST_UNIT:
				unit = param;
				TelemetryVar var = new TelemetryVar(paramId, label, unit);
				TelemetrySensor s = this.getSensor(deviceId);
				if (s != null) {
					if (TelemetryData.log.isLoggable(Level.FINE)) TelemetryData.log.log(Level.FINE, String.format("add variable %s[%s]", var.name, unit));
					s.addVariable(var);
				}
				//Vypadne z funkce
				return;
			case ST_DATATYPE:
				dataType = Integer.parseInt(param);
				state = ST_DECIMALS;
				break;
			case ST_DECIMALS:
				decimals = Integer.parseInt(param);
				state = ST_VALUE;
				break;
			case ST_VALUE:
				long val = 0;
				try {
					val = Long.parseLong(param);
				}
				catch (NumberFormatException e) {
					if (param.length() > 3 && this.startTimeStamp > 0) {
						// String value, for instance an alarm
						String utf8String = new String(param.getBytes(ISO_8859_1));
						String message = TimeLine.getFomatedTimeWithUnit(timestamp) + " - " + utf8String;
						TelemetryData.log.log(Level.WARNING, message);
						GDE.getUiNotification().setStatusMessage(message, SWT.COLOR_RED);
						// Alarm:  Capacity
						deviceId = 0; //force this even if there is a real Tx ID to enable sort in JetiDataReader
						TelemetrySensor sensor = this.getSensor(deviceId);
						if (sensor == null) {
							if (TelemetryData.log.isLoggable(Level.FINE)) TelemetryData.log.log(Level.FINE, "adding sensor " + label);
							sensor = new TelemetrySensor(deviceId, label);
							this.data.add(sensor);
						}
						label = utf8String;
						paramId = getParameterIdByName(sensor, label);
						dataType = TelemetryData.T_DATA8;
						if (TelemetryData.log.isLoggable(Level.FINE))
							TelemetryData.log.log(Level.FINE, "deviceId = " + deviceId + ", paramId = " + paramId + ", untit = " + unit + ", dataType = " + dataType + ", state = " + state + ", decimals = "
									+ decimals);
						TelemetryVar par = sensor.getVar(paramId);
						if (par == null) {
							par = new TelemetryVar(paramId, label, unit);
							if (TelemetryData.log.isLoggable(Level.FINE)) TelemetryData.log.log(Level.FINE, "add variable " + par.name);
							sensor.addVariable(par);
							TelemetryItem item = new TelemetryItem(dataType, decimals, 0, this.startTimeStamp);
							if (TelemetryData.log.isLoggable(Level.FINE)) TelemetryData.log.log(Level.FINE, "add sensor variable value " + par.getName() + "=" + item.getInt());
							par = sensor.getVar(paramId); //read again since copy constructor was used during addVariable
							par.addItem(item, true);
						}

						//add old value, just before new event value in respect of time
						par.addItem(new TelemetryItem(dataType, decimals, par.getItems().get(par.getItems().size() - 1).getInt(), timestamp - 5), true);
						par.addItem(new TelemetryItem(dataType, decimals, (int) (par.maxValue += 1), timestamp), true);
						if (TelemetryData.log.isLoggable(Level.FINE)) {
							TelemetryData.log.log(Level.FINE, "add sensor variable value " + par.getName() + "=" + par.getItems().get(par.getItems().size() - 1).getInt());
							TelemetryData.log.log(Level.FINE, "add sensor variable value count " + par.getItems().size());
						}
					}
					state = ST_PARAM_NUM;
					break;
				}
				//Pokusi se vlozit novy zaznam
				int intval = 0;
				if (dataType == TelemetryData.T_DATA16)
					intval = (short) val;
				else if (dataType == TelemetryData.T_DATA8)
					intval = (byte) val;
				else
					intval = (int) val;
				if (TelemetryData.log.isLoggable(Level.FINE))
					TelemetryData.log.log(Level.FINE, "deviceId = " + deviceId + ", paramId = " + paramId + ", untit = " + unit + ", dataType = " + dataType + ", state = " + state + ", decimals = " + decimals);
				TelemetryItem item = new TelemetryItem(dataType, decimals, intval, timestamp);
				TelemetrySensor sen = this.getSensor(deviceId);
				if (sen != null) {
					TelemetryVar par = sen.getVar(paramId);
					if (par != null) {
						if (TelemetryData.log.isLoggable(Level.FINE)) TelemetryData.log.log(Level.FINE, "add sensor variable value " + par.name + "=" + item.value);
						par.addItem(item, false);
						if (this.startTimeStamp == 0) {
							if (TelemetryData.log.isLoggable(Level.FINE)) TelemetryData.log.log(Level.FINE, "set startTimeStamp = " + timestamp);
							this.startTimeStamp = timestamp;
						}
					}
				}
				state = ST_PARAM_NUM;
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
