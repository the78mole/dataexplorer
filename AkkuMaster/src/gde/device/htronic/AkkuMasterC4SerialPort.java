/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.device.htronic;

import gnu.io.NoSuchPortException;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;

/**
 * SSerial port implementation for AkkuMaster C4 device
 * @author Winfried Brügmann
 */
public class AkkuMasterC4SerialPort extends DeviceSerialPort {
	private Logger							log															= Logger.getLogger(this.getClass().getName());

	public static final String	PROCESS_NAME										= "Prozessname";																// "4 ) Laden" = AkkuMaster aktiv Laden
	public static final String	PROCESS_ERROR_NO								= "Fehlernummer";															// "0" = kein Fehler
	public static final String	PROCESS_VOLTAGE									= "Akkuspannung";															// [mV]
	public static final String	PROCESS_CURRENT									= "Prozesssstrom";															// [mA] 	(laden/entladen)
	public static final String	PROCESS_CAPACITY								= "Prozesskapazität";													// [mAh] (laden/entladen)
	public static final String	PROCESS_POWER										= "Leistung";																	// [mW]			
	public static final String	PROCESS_ENERGIE									= "Energie";																		// [mWh]			
	public static final String	PROCESS_TIME										= "Prozesszeit";																// [msec]		

	public static final String	VERSION_NUMBER									= "Versionsnummer";
	public static final String	VERSION_DATE										= "Datum";
	public static final String	VERSION_TYPE_CURRENT						= "Stromvariante";
	public static final String	VERSION_TYPE_FRONT							= "Frontplattenversion";

	public static final byte		channel_1[]											= new byte[] { 0x00 };
	public static final byte		channel_2[]											= new byte[] { 0x40 };
	public static final byte		channel_3[]											= new byte[] { (byte) 0x80 };
	public static final byte		channel_4[]											= new byte[] { (byte) 0xC0 };

	// data type command description
	private final byte					readVersion[]										= new byte[] { 0x16 };
	private final byte					readConfiguration[]							= new byte[] { 0x11 };													// Lese eingestellte Werte
	private final byte					readMeasuredValues[]						= new byte[] { 0x12 };													// Lese gemessene Werte
	private final byte					readAdjustedValues[]						= new byte[] { 0x21 };													// Lese eingestellte Werte zus. Parameter
	private final byte					setNewProgramm									= 0x14;																				// Schreibe Ladeparameter
	private final byte					setMomoryCycleSleep							= 0x24;																				// Schreibe Ladeparameterzusätzliche Parameter 
	private final byte					startProgram										= 0x15;																				// Start 
	private final byte					stopProgram											= 0x13;																				// Stop 
	private final byte					okStartProgram									= 0x19;																				// OK 

	// data type answer description
	private byte								version[]												= new byte[11];
	private byte								configuration[]									= new byte[14];
	private byte								measuredValues[]								= new byte[16];
	private byte								adjustedValues[]								= new byte[5];
	private byte								ok															= 0x00;

	// status AkkuMaster C4 device
	private final byte					stateWaiting										= 0x00;																				//OOH = warte auf Kommando
	private final byte					stateCharge											= 0x01;																				//OIH = Laden
	private final byte					stateDischarge									= 0x02;																				//02H = Entladen
	private final byte					stateKeepCharge									= 0x04;																				//04H = Erhaltungsladen
	private final byte					stateDeviceActive								= (byte) 0x80;																	//80H = Akkumaster aktiv

	// program numbers
	private final byte					programChargeOnly								= 0x01;																				//OIH = Nur laden
	private final byte					programDischargeOnly						= 0x02;																				//02H = Nur entladen
	private final byte					programDischargeCharge					= 0x03;																				//03H = entladen / laden
	private final byte					programChargeDischargeCharge		= 0x04;																				//04H = laden / entladen / laden
	private final byte					programDischargeChargeTwoTimes	= 0x05;																				//05H = 2 * entladen / laden
	private final byte					programFormUp										= 0x06;																				//06H = Formieren
	private final byte					programOverWinter								= 0x07;																				//07H = Überwintern
	private final byte					programRefresh									= 0x08;																				//08H = Auffrischen
	private final byte					programDiagnostic								= 0x09;																				//09H = Akkudiagnose

	// supportzed accu type
	private final byte					typeNC													= 0x00;																				//OOH = NC
	private final byte					typeNiMh												= 0x01;																				//OIH = NMH
	private final byte					typePB													= 0x02;																				//02H = PB

	//	// Zellenzahl:
	//	private byte							numberCells[]										= new byte[1];								//OIH = 1 Zellen . . . OCH = 12 Zellen
	//	// nominale Akku Kapazität:
	//	private byte							capacitiy[]											= new byte[2];								//1 bit=ImAh [400 . . 16000] (2A Variante)
	//	// Entladestrom / Ladestrom:
	//	private byte							current[]												= new byte[2];								//1 bit = 1 mA [40 . . 20001 (2A Variante)
	//
	//	// Wartezeit:
	//	private byte							latencyTime[]										= new byte[2];								//1 bit = 1 Minute
	//
	//	// Aktuelle Entladekapazität des Akkus
	//	private byte							dischargeCapacity[]							= new byte[2];								//1 bit= 1 mAh	
	//	// Aktuelle Ladekapazität des Akkus
	//	private byte							chargeCapacity[]								= new byte[2];								//1 bit= 1 mAh
	//	// Aktuelle Akkuspannung
	//	private byte							voltage[]												= new byte[2];								//1 bit = 0.2 mV
	//
	//	// Entladezeit Stunden
	//	private byte							dischargeTimeHours[]						= new byte[2];								//1 bit = 1 Stunde
	//	// Entladezeit Stunden
	//	private byte							dischargeTimeMinutes[]					= new byte[2];								//1 bit = 1 Minute
	//	// Entladezeit Stunden
	//	private byte							dischargeTimeSeconds[]					= new byte[2];								//1 bit = 1 Sekunde
	//
	//	// Entladezeit Stunden
	//	private byte							chargeTimeHours[]								= new byte[2];								//1 bit = 1 Stunde
	//	// Entladezeit Stunden
	//	private byte							chargeTimeMinutes[]							= new byte[2];								//1 bit = 1 Minute
	//	// Entladezeit Stunden
	//	private byte							chargeTimeSeconds[]							= new byte[2];								//1 bit = 1 Sekunde
	//
	//	// Anzahl Ladezyklen
	//	private byte							numberChargeCycles[]						= new byte[2];								//1 = 1. Ladezyklus
	//	// Verbleibende Wartezeit bis Formieren wiederholt wird
	//	private byte							formUpLatencyTime[]							= new byte[2];								//1 bit = 1 Minute

	public AkkuMasterC4SerialPort(DeviceConfiguration deviceConfig, OpenSerialDataExplorer application) throws NoSuchPortException {
		super(deviceConfig, application);
	}

	/**
	 * OK start program
	 * @throws IOException 
	 */
	public synchronized void ok(byte[] channel) throws IOException {
		byte[] command = new byte[1];
		command[0] = new Integer(okStartProgram + channel[0]).byteValue();

		this.write(command);
		byte[] answer = this.read(2, 5);

		if (answer[0] != command[0]) throw new IOException("command to answer missmatch");
		if (answer[1] != ok) throw new IOException("command OK failed");
	}

	/**
	 * start loaded program
	 * @throws IOException 
	 */
	public synchronized void start(byte[] channel) throws IOException {
		byte[] command = new byte[1];
		command[0] = new Integer(startProgram + channel[0]).byteValue();

		this.write(command);
		byte[] answer = this.read(2, 5);

		if (answer[0] != command[0]) throw new IOException("command to answer missmatch");
		if (answer[1] != ok) throw new IOException("command START failed");
	}

	/**
	 * stop loaded program
	 * @throws IOException 
	 */
	public synchronized void stop(byte[] channel) throws IOException {
		byte[] command = new byte[1];
		command[0] = new Integer(stopProgram + channel[0]).byteValue();

		this.write(command);
		byte[] answer = this.read(2, 5);

		if (answer[0] != command[0]) throw new IOException("command to answer missmatch");
		if (answer[1] != ok) throw new IOException("command STOP failed");
	}

	/**
	 * write new program to given memory number
	 * Komando, Programm-Nummer, Wartezeit bis Formieren wiederholt wird, Akku Typ, Zellenzahl, Nominale Kapazität des Akkus, Entladestrom, Ladestrom
	 * @throws IOException 
	 */
	public synchronized void writeNewProgram(byte[] channel, int programNumber, int waitTime_days, int akkuTyp, int cellCount, int akkuCapacity, int dischargeCurrent_mA, int chargeCurrent_mA)
			throws IOException {
		byte[] command = new byte[11];
		command[0] = new Integer(setNewProgramm + channel[0]).byteValue();
		command[1] = new Integer(programNumber).byteValue();
		command[2] = new Integer(waitTime_days).byteValue();
		command[3] = new Integer(akkuTyp).byteValue();
		command[4] = new Integer(cellCount).byteValue();

		command[5] = new Integer(((akkuCapacity >> 8) & 0xFF)).byteValue();
		command[6] = new Integer(akkuCapacity & 0xFF).byteValue();

		command[7] = new Integer(((dischargeCurrent_mA >> 8) & 0xFF)).byteValue();
		command[8] = new Integer(dischargeCurrent_mA & 0xFF).byteValue();

		command[9] = new Integer(((chargeCurrent_mA >> 8) & 0xFF)).byteValue();
		command[10] = new Integer(chargeCurrent_mA & 0xFF).byteValue();

		this.write(command);
		byte[] answer = this.read(2, 5);

		if (answer[0] != command[0]) throw new IOException("command to answer missmatch");
		if (answer[1] != ok) throw new IOException("command WRITE NEW PROGRAM failed");
	}

	/**
	 * set memory number, cycle counter, sleep time
	 * 24H we Kommando: Schreibe Ladeparameterzusätzliche Parameter
	 * YYH byte Speicher-Nummer (OOH = Speicher 0 . . .  07H = Speicher 7)
	 * YYH byte Anzahl Wiederholungen (1 bit = 1 Wiederholung [2 . . 9])
	 * YYYYH word Wartezeit (1 bit = 1 Minute [0 . . 43200])
	 * @throws IOException 
	 */
	public synchronized void setMemoryNumberCycleCoundSleepTime(byte[] channel, int memoryNumber, int cycleCount, int sleepTime_min) throws IOException {
		byte[] command = new byte[5];
		command[0] = new Integer(setMomoryCycleSleep + channel[0]).byteValue();
		command[1] = new Integer(memoryNumber).byteValue();
		command[2] = new Integer(cycleCount).byteValue();
		command[3] = new Integer(((sleepTime_min >> 8) & 0xFF)).byteValue();
		command[4] = new Integer(sleepTime_min & 0xFF).byteValue();

		this.write(command);
		byte[] answer = this.read(2, 5);

		if (answer[0] != setMomoryCycleSleep) throw new IOException("command to answer missmatch");
		if (answer[1] != ok) throw new IOException("command setMemoryNumberCycleCoundSleepTime failed");
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * [0] String Aktueller Prozessname 			"4 ) Laden" = AkkuMaster aktiv Laden
	 * [1] int 		Aktuelle Fehlernummer				"0" = kein Fehler
	 * [2] int		Aktuelle Akkuspannung 			[mV]
	 * [3] int 		Aktueller Prozesssstrom 		[mA] 	(laden/entladen)
	 * [4] int 		Aktuelle Prozesskapazität		[mAh] (laden/entladen)
	 * [5] int 		Errechnete Leistung					[mW]			
	 * [6] int		Errechnete Energie					[mWh]			
	 * [7] int		Prozesszeit									[msec]			
	 * @param channel signature if device has more than one or required by device
	 * @return map containing gathered data - this can individual specified per device
	 * @throws Exception 
	 */
	public synchronized HashMap<String, Object> getData(byte[] channel, int recordNumber, IDevice dialog, String channelConfigKey) throws Exception {
		boolean isActive = true;
		HashMap<String, Object> values = new HashMap<String, Object>(7);
		boolean isPortOpenedByMe = false;
		try {
			if (!this.isConnected) {
				this.open();
				isPortOpenedByMe = true;
			}

			String[] configuration = getConfiguration(channel);
			String[] measurements = getMeasuredValues(channel);

			values.put(PROCESS_NAME, configuration[0].split(" ")[0]); // AkkuMaster aktiv
			values.put(PROCESS_ERROR_NO, new Integer(configuration[1].split(" ")[0])); // Aktuelle Fehlernummer
			values.put(PROCESS_VOLTAGE, new Integer(measurements[2].split(" ")[0])); // Aktuelle Akkuspannung

			switch (new Integer(configuration[0].split(" ")[0])) {
			case 1:
				values.put(PROCESS_NAME, configuration[0].split(" ")[0] + " Laden");
				values.put(PROCESS_CURRENT, new Integer(configuration[7].split(" ")[0])); // eingestellter Ladestrom
				values.put(PROCESS_CAPACITY, new Integer(measurements[1].split(" ")[0])); // Aktuelle Ladekapazität
				break;
			case 2:
				values.put(PROCESS_NAME, configuration[0].split(" ")[0] + " Entladen");
				values.put(PROCESS_CURRENT, new Integer(configuration[6].split(" ")[0])); // eingestellter Entladestrom
				values.put(PROCESS_CAPACITY, new Integer(measurements[0].split(" ")[0])); // Aktuelle Entladekapazität
				break;
			case 3:
				values.put(PROCESS_NAME, configuration[0].split(" ")[0] + " Erhaltungsladen");
				values.put(PROCESS_CAPACITY, new Integer("0"));
				break;
			default:
				isActive = false;
				values.put(PROCESS_NAME, configuration[0].split(" ")[0] + " AkkuMaster_inactiv");
				values.put(PROCESS_CAPACITY, new Integer("0"));
				break;
			}

			if (isActive) {
				int voltage = (Integer) values.get(PROCESS_VOLTAGE);
				values.put(PROCESS_POWER, new Integer(voltage * (Integer) values.get(PROCESS_CURRENT))); // Errechnete Leistung	[mW]
				values.put(PROCESS_ENERGIE, new Integer(voltage * (Integer) values.get(PROCESS_CAPACITY))); // Errechnete Energie	[mWh]
			}

		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			if (isPortOpenedByMe) this.close();
		}
		return values;
	}

	public void setStatus() throws IOException {
		this.write(readConfiguration);
		this.configuration = this.read(14, 5);
	}

	/**
	 * 	Antwort: Lese eingestellte Werte
	 *	[0] Status des Akku-Master
	 *	[1] Fehlernummer
	 *  [2] Programm-Nummer
	 *  [3] Akku Typ
	 *	[4] Zellenzahl
	 *	[5] Nominale Kapazität des Akkus
	 *	[6] eingestellter Entladestrom
	 *	[7] eingestellter Ladestrom
	 *	[8] eingestellte Wartezeit bis Formieren wiederholt wird
	 * @throws IOException 
	 * @return String[] containing described values
	 */
	public synchronized String[] getConfiguration(byte[] channel) throws IOException {
		String[] configStrings = new String[9];
		byte readConfigOfChannel[] = new byte[1];

		int a = (int) readConfiguration[0];
		int b = (int) channel[0];
		readConfigOfChannel[0] = new Integer(a + b).byteValue();
		this.write(readConfigOfChannel);
		this.configuration = this.read(14, 5);

		if (this.configuration[0] != readConfigOfChannel[0]) throw new IOException("command to answer missmatch");

		// status
		byte state = (byte) (this.configuration[1] - stateDeviceActive);
		if (state == stateWaiting)
			configStrings[0] = "0 Akkumaster_inaktiv"; // warte auf Kommando
		else if (state == stateCharge)
			configStrings[0] = "1 Laden";
		else if (state == stateDischarge)
			configStrings[0] = "2 Entladen";
		else if (state == stateKeepCharge)
			configStrings[0] = "3 Erhaltungsladen";
		else
			configStrings[0] = "8 Akkumaster_aktiv"; // Pausentimer ?

		// error number 
		configStrings[1] = this.configuration[2] + " = Fehlernummer";

		// program number
		byte program = this.configuration[3];
		if (program == programChargeOnly)
			configStrings[2] = "1 Laden";
		else if (program == programDischargeOnly)
			configStrings[2] = "2 Entladen";
		else if (program == programDischargeCharge)
			configStrings[2] = "3 Entladen-Laden";
		else if (program == programChargeDischargeCharge)
			configStrings[2] = "4 Laden-Entladen-Laden";
		else if (program == programDischargeChargeTwoTimes)
			configStrings[2] = "5 Entladen-Laden-Entladen-Laden";
		else if (program == programFormUp)
			configStrings[2] = "6 Formieren";
		else if (program == programOverWinter)
			configStrings[2] = "7 Überwintern";
		else if (program == programRefresh)
			configStrings[2] = "8 Auffrischen";
		else if (program == programDiagnostic)
			configStrings[2] = "9 Akkudiagnose";
		else
			configStrings[2] = "Unbekannt";

		// Akku-Typ:
		byte accuTyp = this.configuration[4];
		if (accuTyp == typeNC)
			configStrings[3] = "0 NickelCadmium";
		else if (accuTyp == typeNiMh)
			configStrings[3] = "1 NickelMetallHydrid";
		else if (accuTyp == typePB)
			configStrings[3] = "2 Blei";
		else
			configStrings[3] = "Unbekannt";

		// Zellenzahl:
		configStrings[4] = this.configuration[5] + " Zellen";

		// nominale Akku Kapazität:
		int accuCapacity = ((int) this.configuration[6] & 0xFF) << 8;
		accuCapacity += ((int) this.configuration[7] & 0xFF) << 0;
		configStrings[5] = accuCapacity + " mAh"; // (2A Variante)

		// Entladestrom 
		int current = ((int) this.configuration[8] & 0xFF) << 8;
		current += ((int) this.configuration[9] & 0xFF) << 0;
		configStrings[6] = current + " mA Entladestrom"; // (2A Variante)

		// Ladestrom
		current = ((int) this.configuration[10] & 0xFF) << 8;
		current += ((int) this.configuration[11] & 0xFF) << 0;
		configStrings[7] = current + " mA Ladestrom"; // (2A Variante)

		// Wartezeit:
		int latencyTime = ((int) this.configuration[12] & 0xFF) << 8;
		latencyTime += ((int) this.configuration[13] & 0xFF) << 0;
		configStrings[8] = latencyTime + " Minute"; //1 bit = 1 Minute

		return configStrings;
	}

	/**
	 * 	Antwort: Lese eingestellte Werte zus. Parameter
	 *	[0] ausgewählte Speichernummer
	 *	[1] Anzahl einaestellter Wiederholunaen
	 *  [2] tatsächlicher Ladestrom
	 * @throws IOException 
	 * @return String[] containing described values
	 */
	public synchronized String[] getAdjustedValues(byte[] channel) throws IOException {
		String[] adjustments = new String[3];
		byte readAdjustmentsOfChannel[] = new byte[1];

		int a = (int) readAdjustedValues[0];
		int b = (int) channel[0];
		readAdjustmentsOfChannel[0] = new Integer(a + b).byteValue();
		this.write(readAdjustmentsOfChannel);
		this.adjustedValues = this.read(5, 5);

		if (this.adjustedValues[0] != readAdjustmentsOfChannel[0]) throw new IOException("command to answer missmatch");

		// ausgewählte Speichernummer
		adjustments[0] = this.adjustedValues[1] + " ausgewählte Speichernummer";

		// Anzahl einaestellter Wiederholungen
		adjustments[1] = this.adjustedValues[2] + " Anzahl eingestellter Wiederholungen";

		// tatsächlicher Ladestrom
		int current = ((int) this.adjustedValues[3] & 0xFF) << 8;
		current += ((int) this.adjustedValues[4] & 0xFF) << 0;
		adjustments[2] = current + " mA Ladestrom";

		return adjustments;
	}

	/**
	 * Antwort: Lese gemessene Werte
	 * [0] Aktuelle Entladekapazität des Akkus
	 * [1] Aktuelle Ladekapazität des Akkus
	 * [2] Aktuelle Akkuspannung
	 * [3] Entladezeit Stunden
	 * [4] Entladezeit Minuten
	 * [5] Entladezeit Sekunden 
	 * [6] Ladezeit Stunden
	 * [7] Ladezeit Minuten
	 * [8] Ladezeit Sekunden
	 * [9] Anzahl Ladezyklen
	 * [10] Verbleibende Wartezeit bis Formieren wiederholt wird
	 * @return String[]
	 * @throws IOException 
	 */
	public synchronized String[] getMeasuredValues(byte[] channel) throws IOException {
		String[] measurements = new String[11];
		byte readValuesOfChannel[] = new byte[1];

		int a = (int) readMeasuredValues[0];
		int b = (int) channel[0];
		readValuesOfChannel[0] = new Integer(a + b).byteValue();
		this.write(readValuesOfChannel);
		this.measuredValues = this.read(16, 5);

		if (this.measuredValues[0] != readValuesOfChannel[0]) throw new IOException("command to answer missmatch");

		// Aktuelle Entladekapazität des Akkus
		int current = ((int) this.measuredValues[1] & 0xFF) << 8;
		current += ((int) this.measuredValues[2] & 0xFF) << 0;
		measurements[0] = current + " mAh Entladekapazität";
		//Aktuelle Ladekapazität des Akkus
		current = ((int) this.measuredValues[3] & 0xFF) << 8;
		current += ((int) this.measuredValues[4] & 0xFF) << 0;
		measurements[1] = current + " mAh Ladekapazität";
		// Aktuelle Akkuspannung
		int voltage = ((int) this.measuredValues[5] & 0xFF) << 8;
		voltage += ((int) this.measuredValues[6] & 0xFF) << 0;
		measurements[2] = voltage * 10 / 2 + " mV Spannung";

		// Entladezeit Stunden
		measurements[3] = this.measuredValues[7] + " Std Entladezeit";
		// Entladezeit Minuten
		measurements[4] = this.measuredValues[8] + " Min Entladezeit";
		// Entladezeit Sekunden
		measurements[5] = this.measuredValues[9] + " Sec Entladezeit";
		// Ladezeit Stunden
		measurements[6] = this.measuredValues[10] + " Std Ladezeit";
		// Ladezeit Minuten
		measurements[7] = this.measuredValues[11] + " Min Ladezeit";
		// Ladezeit Sekunden
		measurements[8] = this.measuredValues[12] + " Sec Ladezeit";

		// Anzahl Ladezyklen
		measurements[9] = this.measuredValues[13] + " Ladecyclen";

		// Verbleibende Wartezeit bis Formieren wiederholt wird
		int latencyCycleTime = ((int) this.measuredValues[14] & 0xFF) << 8;
		latencyCycleTime += ((int) this.measuredValues[15] & 0xFF) << 0;
		measurements[10] = latencyCycleTime + " Minuten verbleibende Wartezeit bis Formieren wiederholt wird";

		return measurements;
	}

	/**
	 * Antwort: Lese Version
	 * [0] Versionsnummer
	 * [1] Datum 
	 * [2] Stromvariante
	 * [3] Frontplattenversion
	 * @return String[] containing described values
	 * @throws Exception 
	 */
	public synchronized HashMap<String, Object> getVersion() throws Exception {
		HashMap<String, Object> result = new HashMap<String, Object>(4);
		try {
			if (!this.isConnected) {
				this.open();
			}

			this.write(readVersion);
			this.version = this.read(11, 5);

			// Versionsnummer der Software
			String versionsNummer = new Integer(version[1]).toString();
			// Versionsindex der Software
			String versionsIndex = new Integer(version[2]).toString();
			result.put(VERSION_NUMBER, versionsNummer + "." + versionsIndex);

			// Datum der Software Tag
			String day = new Integer(version[3]).toString();
			// Datum der Software Monat
			String month = new Integer(version[4]).toString();
			int iYear = ((int) version[5] & 0xFF) << 8;
			iYear += ((int) version[6] & 0xFF) << 0;
			// Datum der Software Jahr
			String year = new Integer(iYear).toString();
			result.put(VERSION_DATE, day + "." + month + "." + year);

			// Stromvariante OOH = 0,5A Variante; 01 H = 2A Variante
			if (version[7] == 0x00)
				result.put(VERSION_TYPE_CURRENT, "0,5A");
			else if (version[7] == 0x01)
				result.put(VERSION_TYPE_CURRENT, "2,0A");
			else
				result.put(VERSION_TYPE_CURRENT, "?,?A");

			// Frontplattenvariante 0OH = 6 Tasten; 01 H = 4 Tasten (turned around ?)
			if (version[8] == 0x00)
				result.put(VERSION_TYPE_FRONT, "4 Tasten");
			else if (version[8] == 0x01)
				result.put(VERSION_TYPE_FRONT, "6 Tasten");
			else
				result.put(VERSION_TYPE_FRONT, "? Tasten");

		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			// could not secure all timer threads are stopped
			//if (isPortOpenedByMe) this.close();
		}
		return result;
	}

	/**
	 * log string array
	 * @param array
	 */
	public void print(String[] array) {
		for (String string : array) {
			if (string != null)
				log.fine(string);
			else
				log.fine("no data");
		}
	}

}
