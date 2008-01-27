package osde.device.smmodellbau;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOError;
import java.io.IOException;
import java.util.HashMap;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.device.DeviceConfiguration;
import osde.device.DeviceSerialPort;
import osde.device.IDevice;
import osde.exception.CheckSumMissmatchExcption;
import osde.ui.StatusBar;
import osde.utils.Checksum;

/**
 * UniLog serial port implementation class, just copied from Sample projectS
 * @author Winfried Brügmann
 */
public class UniLogSerialPort extends DeviceSerialPort {
	private static Logger					log											= Logger.getLogger(UniLogSerialPort.class.getName());

	private final static byte[]		COMMAND_QUERY_STATE			= { 0x54 };																																															// 'T' auf die Bereitschaft vom UniLog warten
	private final static byte[]		COMMAND_RESET						= { 0x72 };																																															// 'r' "Reset" zum UniLog schicken (damit sendet der UniLog die Daten von Anfang an)
	private final static byte[]		COMMAND_READ_DATA				= { 0x6C };																																															// 'l' "Lesen" zum UniLog schicken (der UniLog schickt dann einen Datensatz)
	private final static byte[]		COMMAND_REPEAT					= { 0x77 };																																															// 'w' "Wiederholen": UniLog schickt den selben Datensatz nochmal
	private final static byte[]		COMMAND_PREPARE_DELETE	= { 0x78, 0x79, 0x31 };																																									// "xy1"
	private final static byte[]		COMMAND_DELETE					= { (byte) 0xC0, 0x03, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06 };
	private final static byte[]		COMMAND_QUERY_CONFIG		= { (byte) 0xC0, 0x03, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04 };
	private final static byte[]		COMMAND_PREPARE_SET_CONFIG = { 0x78, 0x79, (byte)0xA7 }; // "xyz"
	private static byte[]					COMMAND_SET_CONFIG 			= { (byte)0xC0 , 0x03, 0x02 };

	private final static byte			DATA_STATE_WAITING			= 0x57;																																																	// 'W' heisst UniLog ist da, muss aber noch den Flash absuchen nach dem Speicherende
	private final static byte			DATA_STATE_READY				= 0x46;																																																	// 'F' heisst UniLog ist fertig zum Datensenden
	private final static byte			DATA_STATE_DELETED			= 0x6A;																																																	// 'j' "Speicher im UniLog gelöscht."

	private final static int			WERTESAETZE_MAX					= 25920;
	private final static int			DATENSATZ_BYTES					= 24;																																																		//TODO exchange with deviceConfig.get()

	public final static int				A1_MODUS_TEMPERATUR			= 0;
	public final static int				A1_MODUS_MILLIVOLT			= 1;
	public final static int				A1_MODUS_PITOT_250			= 2;
	public final static int				A1_MODUS_PITOT_450			= 3;

	private String								unilogVersion;
	private int 									timeIntervalPosition = 0;
	private int										memoryUsed;
	private String								memoryUsedPercent;
	private boolean								isMotorPole							= false;
	private boolean								isPropBlade							= false;
	private int										countMotorPole					= 0;
	private int										countPropBlade					= 0;
	private boolean								isAutoStartCurrent			= false;
	private int										currentAutoStart				= 0;
	private boolean								isAutStartRx						= false;
	private boolean								isRxOn									= false;
	private boolean								isImpulseAutoStartTime					= false;
	private int										impulseAutoStartTime_sec				= 0;
	private int										currentSensorPosition;
	private int										serialNumber;
	private int										modusA1Position;
	private boolean								isLimiter								= false;
	private int										limiter									= 0;
	private double								gearRatio;

	/**
	 * constructor of default implementation
	 * @param deviceConfig - required by super class to initialize the serial communication port
	 * @param statusBar - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 * @throws NoSuchPortException
	 */
	public UniLogSerialPort(DeviceConfiguration deviceConfig, StatusBar statusBar) throws NoSuchPortException {
		super(deviceConfig, statusBar);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @param channel signature if device has more than one or required by device
	 * @return map containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public HashMap<String, Object> getData(byte[] channel, int recordNumber, IDevice dialog) throws IOException {
		// TODO add some sample code here
		return null;
	}

	//
	public void setConfiguration() throws Exception {
		int checkSum = 0;		
		try {
			this.open();
			// check device connected
			if (this.checkConnectionStatus()) {
				// check data ready for read operation
				if (this.checkDataReady()) {
					this.write(COMMAND_PREPARE_SET_CONFIG);
					checkSum = checkSum + 0x03 + 0x02;

/*
    If IsNull(Speicherrate_Box.Value) Then
        Speicherrate_Box.Value = 2
    End If
    Ser.WriteHex = Speicherrate_Box.Value
    Checksumme = Checksumme + Speicherrate_Box.Value
    
    If IsNull(Blattzahl_Auswahl.Value) Then
        Blattzahl_Auswahl.Value = 2
    End If
    If Option_Pole.Value = True Then
        Ser.WriteHex = CInt(Blattzahl_Auswahl.Value) Or &H80
        Checksumme = Checksumme + (CInt(Blattzahl_Auswahl.Value) Or &H80)
    Else
        Ser.WriteHex = Blattzahl_Auswahl.Value
        Checksumme = Checksumme + Blattzahl_Auswahl.Value
    End If
    
    If IsNull(CheckBox_Autostart.Value) Then
        CheckBox_Autostart.Value = False
    End If
    If CheckBox_Autostart.Value = True Then
        Ser.WriteHex = CInt(Autostart_Strom_Box.Value) Or &H80
        Checksumme = Checksumme + (CInt(Autostart_Strom_Box.Value) Or &H80)
    Else
        Ser.WriteHex = Autostart_Strom_Box
        Checksumme = Checksumme + Autostart_Strom_Box
    End If
    
    If IsNull(CheckBox_Empfaengersteuerung.Value) Then
        CheckBox_Empfaengersteuerung.Value = False
    End If
    If CheckBox_Empfaengersteuerung.Value = True Then
        If Startimpuls_Box.Text = "Rx an" Then
            Ser.WriteHex = &H80
            Checksumme = Checksumme + &H80
        Else
            Ser.WriteHex = (CInt(Startimpuls_Box.Value) Or &H80) + 11
            Checksumme = Checksumme + (CInt(Startimpuls_Box.Value) Or &H80) + 11
        End If
    Else
        If Startimpuls_Box.Text = "Rx an" Then
            Ser.WriteHex = 0
            Checksumme = Checksumme + 0
        Else
            Ser.WriteHex = (Startimpuls_Box + 11)
            Checksumme = Checksumme + (Startimpuls_Box + 11)
        End If
    End If
   
    Autostart_Zeit_Box = Autostart_Zeit_Box - Autostart_Zeit_Box Mod 5
    If IsNull(CheckBox_Autostart_s.Value) Then
        CheckBox_Autostart_s.Value = False
    End If
    If CheckBox_Autostart_s.Value = True Then
        Ser.WriteHex = CInt(Autostart_Zeit_Box.Value) Or &H80
        Checksumme = Checksumme + (CInt(Autostart_Zeit_Box.Value) Or &H80)
    Else
        Ser.WriteHex = Autostart_Zeit_Box
        Checksumme = Checksumme + Autostart_Zeit_Box
    End If

    Ser.WriteHex = (CInt(Stromsensor_Box.Value))
    Checksumme = Checksumme + (CInt(Stromsensor_Box.Value))
       
    Ser.WriteHex = (CInt(A1_Modus_Box.Value))
    Checksumme = Checksumme + (CInt(A1_Modus_Box.Value))
       
    If IsNull(CheckBox_Limiter.Value) Then
        CheckBox_Limiter.Value = False
    End If
    If CheckBox_Limiter.Value = True Then
        Ser.WriteHex = CInt((Limiter_Wert_Box.Value And &HFF00) / 256) Or &H80
        Ser.WriteHex = CInt(Limiter_Wert_Box.Value And &HFF)
        Checksumme = Checksumme + (CInt((Limiter_Wert_Box.Value And &HFF00) / 256) Or &H80) + CInt(Limiter_Wert_Box.Value And &HFF)
    Else
        Ser.WriteHex = CInt((Limiter_Wert_Box.Value And &HFF00) / 256)
        Ser.WriteHex = Limiter_Wert_Box.Value And &HFF
        Checksumme = Checksumme + CInt((Limiter_Wert_Box.Value And &HFF00) / 256) + (Limiter_Wert_Box.Value And &HFF)
    End If
    
    If IsNull(Getriebe_Faktor_Box.Value) Then
        Getriebe_Faktor_Box.Value = 10
    End If
    Ser.WriteHex = Getriebe_Faktor_Auswahl.Value
    Checksumme = Checksumme + Getriebe_Faktor_Auswahl.Value
    
    Ser.WriteHex = &H0                  'Platzhalter für Erweiterungen
    
    Checksumme = Checksumme Mod 256     'ist nur ein Byte im UniLog
    Ser.WriteHex = Checksumme
    
    If (Ser.ReadLine = "j") Then
        Status_Box.Value = "Einstellungen gesetzt."
        Einstellungen_setzen.BackColor = &HFFFFC0
    Else
        Status_Box.Value = "Fehler beim Übertragen."
    End If

 */					
				}
				else
					throw new IOException("Daten im Gerät sind nicht bereit zum Abholen.");
			}
			else
				throw new IOException("Gerät ist nicht angeschlossen oder nicht bereit.");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			this.close();
		}
	}
	
	/**
	 * query the configuration information from UniLog
	 * @throws Exception
	 */
	public void readConfiguration() throws Exception {
		int checkSum = 0;
		int checkSumLast2Bytes = 0;
		try {
			this.open();

			// check device connected
			if (this.checkConnectionStatus()) {
				// check data ready for read operation
				if (this.checkDataReady()) {

					this.write(COMMAND_QUERY_CONFIG);
					byte[] readBuffer = this.read(24, 5);

					// verify checksum
					checkSum = Checksum.ADD(readBuffer, 2) + 1;
					log.finer("checkSum = " + checkSum);
					checkSumLast2Bytes = getCheckSum(readBuffer);
					log.finer("checkSumLast2Bytes = " + checkSumLast2Bytes);

					if (checkSum == checkSumLast2Bytes) {
						// valid data set -> set values
						memoryUsed = ((readBuffer[6] & 0xFF) << 8) + (readBuffer[7] & 0xFF);
						log.finer("memoryUsed = " + memoryUsed);

						unilogVersion = String.format("%.2f", new Double(readBuffer[8] & 0xFF) / 100);
						log.finer("unilogVersion = " + unilogVersion);

						int memoryDeleted = readBuffer[9] & 0xFF;
						int tmpMemoryUsed = 0;
						if (memoryDeleted > 0)
							tmpMemoryUsed = 0;
						else
							tmpMemoryUsed = memoryUsed;
						memoryUsedPercent = String.format("%.2f", tmpMemoryUsed * 100.0 / WERTESAETZE_MAX);
						log.finer("memoryUsedPercent = " + memoryUsedPercent);

						timeIntervalPosition = readBuffer[10] & 0xFF;
						log.finer("timeIntervalPosition = " + timeIntervalPosition + " timeInterval = " );

						if ((readBuffer[11] & 0x80) == 0) {
							isPropBlade = true;
							countPropBlade = readBuffer[11] & 0x7F;
						}
						else {
							isMotorPole = true;
							countMotorPole = (readBuffer[11] & 0x7F) * 2;
						}
						log.finer("isPropBlade = " + isPropBlade + " countPropBlade = " + countPropBlade);
						log.finer("isMotorPole = " + isMotorPole + " countMotorPole = " + countMotorPole);

						if ((readBuffer[12] & 0x80) != 0) {
							isAutoStartCurrent = true;
							currentAutoStart = readBuffer[12] & 0x7F;
						}
						log.finer("isAutoStartCurrent = " + isAutoStartCurrent + " currentAutoStart = " + currentAutoStart);

						int rxAutoStart_us = 0;
						if ((readBuffer[13] & 0x80) != 0) {
							isAutStartRx = true;
							if ((readBuffer[13] & 0x7F) == 0)
								isRxOn = true;
							else
								rxAutoStart_us = (readBuffer[13] & 0x7F) * 1000; // 16 = 1.6 ms (value - 11 = position in RX_AUTO_START_MS)
						}
						log.finer("isAutStartRx = " + isAutStartRx + " isRxOn = " + isRxOn + " rxAutoStart_us = " + rxAutoStart_us);

						if ((readBuffer[14] & 0x80) != 0) {
							isImpulseAutoStartTime = true;
							impulseAutoStartTime_sec = readBuffer[14] & 0x7F;
						}
						log.finer("isAutoStartTime = " + isImpulseAutoStartTime + " timeAutoStart_sec = " + impulseAutoStartTime_sec);

						currentSensorPosition = readBuffer[15] & 0xFF;
						log.finer("currentSensor = " + currentSensorPosition);

						serialNumber = ((readBuffer[16] & 0xFF) << 8) + (readBuffer[17] & 0xFF);
						log.finer("serialNumber = " + serialNumber);

						modusA1Position = (readBuffer[18] & 0xFF) <= 3 ? (readBuffer[18] & 0xFF) : 0;
						log.finer("modusA1 = " + modusA1Position);

						if ((readBuffer[19] & 0x80) != 0) {
							isLimiter = true;
							limiter = (((readBuffer[19] & 0xFF) << 8) + (readBuffer[20] & 0xFF)) & 0x7FFF;
						}
						log.finer("isLimiter = " + isLimiter + " limiter = " + limiter);

						gearRatio = (readBuffer[21] & 0xFF) / 10.0;
						log.finer(String.format("gearRatio = %.1f", gearRatio));
					}
					else
						throw new CheckSumMissmatchExcption("Die Checksumme ist fehlerhaft - " + checkSum + " / " + checkSumLast2Bytes);
				}
				else
					throw new IOException("Daten im Gerät sind nicht bereit zum Abholen.");
			}
			else
				throw new IOException("Gerät ist nicht angeschlossen oder nicht bereit.");
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		finally {
			this.close();
		}
	}

	/**
	 * query if UniLog is connected and capable for communication
	 * @return true/false
	 * @throws IOException
	 */
	public boolean checkConnectionStatus() throws IOException {
		boolean isConnect = false;

		this.write(COMMAND_QUERY_STATE);

		byte[] buffer = this.read(1, 2);

		if ((buffer[0] & DATA_STATE_WAITING) == DATA_STATE_WAITING || (buffer[0] & DATA_STATE_READY) == DATA_STATE_READY) { 
			isConnect = true;
		}

		return isConnect;
	}

	/**
	 * check if UniLog is capable to send data
	 * @return
	 * @throws Exception
	 */
	public boolean checkDataReady() throws Exception {
		boolean isReady = false;

		this.write(COMMAND_QUERY_STATE); 

		byte[] buffer = this.read(1, 2);

		if ((buffer[0] & DATA_STATE_READY) == DATA_STATE_READY) { 
			isReady = true;
		}

		return isReady;
	}

	/**
	 * @param readBuffer
	 * @return
	 */
	private int getCheckSum(byte[] readBuffer) {
		return ((readBuffer[DATENSATZ_BYTES - 2] & 0xFF) << 8) + (readBuffer[DATENSATZ_BYTES - 1] & 0xFF);
	}

	/**
	 * @return the memoryUsed
	 */
	public int getMemoryUsed() {
		return memoryUsed;
	}

	/**
	 * @param memoryUsed the memoryUsed to set
	 */
	public void setMemoryUsed(int memoryUsed) {
		this.memoryUsed = memoryUsed;
	}

	/**
	 * @return the memoryUsedPercent
	 */
	public String getMemoryUsedPercent() {
		return memoryUsedPercent;
	}

	/**
	 * @param memoryUsedPercent the memoryUsedPercent to set
	 */
	public void setMemoryUsedPercent(String memoryUsedPercent) {
		this.memoryUsedPercent = memoryUsedPercent;
	}

	/**
	 * @return the isMotorPole
	 */
	public boolean isMotorPole() {
		return isMotorPole;
	}

	/**
	 * @param isMotorPole the isMotorPole to set
	 */
	public void setMotorPole(boolean isMotorPole) {
		this.isMotorPole = isMotorPole;
	}

	/**
	 * @return the isPropBlade
	 */
	public boolean isPropBlade() {
		return isPropBlade;
	}

	/**
	 * @param isPropBlade the isPropBlade to set
	 */
	public void setPropBlade(boolean isPropBlade) {
		this.isPropBlade = isPropBlade;
	}

	/**
	 * @return the countMotorPole
	 */
	public int getCountMotorPole() {
		return countMotorPole;
	}

	/**
	 * @param countMotorPole the countMotorPole to set
	 */
	public void setCountMotorPole(int countMotorPole) {
		this.countMotorPole = countMotorPole;
	}

	/**
	 * @return the countPropBlade
	 */
	public int getCountPropBlade() {
		return countPropBlade;
	}

	/**
	 * @param countPropBlade the countPropBlade to set
	 */
	public void setCountPropBlade(int countPropBlade) {
		this.countPropBlade = countPropBlade;
	}

	/**
	 * @return the isAutoStartCurrent
	 */
	public boolean isAutoStartCurrent() {
		return isAutoStartCurrent;
	}

	/**
	 * @param isAutoStartCurrent the isAutoStartCurrent to set
	 */
	public void setAutoStartCurrent(boolean isAutoStartCurrent) {
		this.isAutoStartCurrent = isAutoStartCurrent;
	}

	/**
	 * @return the currentAutoStart
	 */
	public int getCurrentAutoStart() {
		return currentAutoStart;
	}

	/**
	 * @param currentAutoStart the currentAutoStart to set
	 */
	public void setCurrentAutoStart(int currentAutoStart) {
		this.currentAutoStart = currentAutoStart;
	}

	/**
	 * @return the isAutStartRx
	 */
	public boolean isAutStartRx() {
		return isAutStartRx;
	}

	/**
	 * @param isAutStartRx the isAutStartRx to set
	 */
	public void setAutStartRx(boolean isAutStartRx) {
		this.isAutStartRx = isAutStartRx;
	}

	/**
	 * @return the isRxOn
	 */
	public boolean isRxOn() {
		return isRxOn;
	}

	/**
	 * @param isRxOn the isRxOn to set
	 */
	public void setRxOn(boolean isRxOn) {
		this.isRxOn = isRxOn;
	}

	/**
	 * @return the isAutoStartTime
	 */
	public boolean isImpulseAutoStartTime() {
		return isImpulseAutoStartTime;
	}

	/**
	 * @param isAutoStartTime the isAutoStartTime to set
	 */
	public void setImpulseAutoStartTime(boolean isAutoStartTime) {
		this.isImpulseAutoStartTime = isAutoStartTime;
	}

	/**
	 * @return the timeAutoStart_sec
	 */
	public int getImpulseAutoStartTime_sec() {
		return impulseAutoStartTime_sec;
	}

	/**
	 * @param timeAutoStart_sec the timeAutoStart_sec to set
	 */
	public void setImpulseAutoStartTime_sec(int timeAutoStart_sec) {
		this.impulseAutoStartTime_sec = timeAutoStart_sec;
	}

	/**
	 * @return the currentSensor
	 */
	public int getCurrentSensorPosition() {
		return currentSensorPosition;
	}

	/**
	 * @param currentSensor the currentSensor to set
	 */
	public void setCurrentSensorPosition(int currentSensor) {
		this.currentSensorPosition = currentSensor;
	}

	/**
	 * @return the serialNumber
	 */
	public int getSerialNumber() {
		return serialNumber;
	}

	/**
	 * @param serialNumber the serialNumber to set
	 */
	public void setSerialNumber(int serialNumber) {
		this.serialNumber = serialNumber;
	}

	/**
	 * @return the modusA1
	 */
	public int getModusA1Position() {
		return modusA1Position;
	}

	/**
	 * @param modusA1 the modusA1 to set
	 */
	public void setModusA1Position(int modusA1) {
		this.modusA1Position = modusA1;
	}

	/**
	 * @return the isLimiter
	 */
	public boolean isLimiter() {
		return isLimiter;
	}

	/**
	 * @param isLimiter the isLimiter to set
	 */
	public void setLimiter(boolean isLimiter) {
		this.isLimiter = isLimiter;
	}

	/**
	 * @return the limiter
	 */
	public int getLimiter() {
		return limiter;
	}

	/**
	 * @param limiter the limiter to set
	 */
	public void setLimiter(int limiter) {
		this.limiter = limiter;
	}

	/**
	 * @return the gearRatio
	 */
	public double getGearRatio() {
		return gearRatio;
	}

	/**
	 * @param gearRatio the gearRatio to set
	 */
	public void setGearRatio(double gearRatio) {
		this.gearRatio = gearRatio;
	}

	/**
	 * @return the unilogVersion
	 */
	public String getUnilogVersion() {
		return unilogVersion;
	}

	/**
	 * @return the timeIntervalPosition
	 */
	public int getTimeIntervalPosition() {
		return timeIntervalPosition;
	}

	/**
	 * @param timeIntervalPosition the timeIntervalPosition to set
	 */
	public void setTimeIntervalPosition(int timeIntervalPosition) {
		this.timeIntervalPosition = timeIntervalPosition;
	}
}
