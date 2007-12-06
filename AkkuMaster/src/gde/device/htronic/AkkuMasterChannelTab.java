package osde.device.htronic;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import osde.common.Channel;
import osde.common.Channels;
import osde.common.RecordSet;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

public class AkkuMasterChannelTab {
	private Logger												log											= Logger.getLogger(this.getClass().getName());

	private AkkuMasterC4Dialog						parent;
	private String												name;
	private byte[]												channelSig;
	private String[]											aCapacity;
	private String[]											aZellenZahl;
	private String[]											aAkkuTyp;
	private String[]											aProgramm;
	private String[]											aLadestromMilliA;
	private String[]											aEntladeStromMilliA;
	private String[]											aAnzahlWiederholungen;
	private String[]											aWarteZeitMin;
	private AkkuMasterC4SerialPort				serialPort;
	private Channel												channel;
	private Timer													timer;
	private TimerTask											timerTask;

	private CTabItem											kanal1Tab;
	private Button												direktStartButton;
	private Group													programmGroup;
	private Group													dataGatherOnlyGroup;
	private CCombo												memoryNumberCombo;
	private Button												memoryNumberButton;
	private CCombo												capacityMilliAh;
	private Group													akkuGroup;
	private CCombo												anzahlWiederholungen;
	private Text													ladeStromText;
	private Button												stopAuzeichnungButton;
	private Button												startAufzeichnungButton;
	private CCombo												wiederholungen;
	private CCombo												warteZeitMin;
	private Text													wiederholungSpeicher;
	private Text													warteZeitMinText;
	private Text													anzahlWiederholungText;
	private Button												programmwiederholungButton;
	private CCombo												waitTimeDays;
	private Text													waitTimeDaysText;
	private CCombo												entladeStromMilliA;
	private Text													entladeStromText;
	private CCombo												ladeStromMilliA;
	private CCombo												programm;
	private Text													programmText;
	private Group													programmtypGroup;
	private Text													akkuTypText;
	private CCombo												akkuTyp;
	private CCombo												zellenAnzahl;
	private Text													zellenAnzahlText;
	private Text													capacityText;
	private Text													direktStartText;
	private Group													programmwiederholungGroup;
	private Button												programmButton;
	private Composite											kanalKomposite;
	private boolean												isDirectStart						= true;
	private boolean												isCollectData						= false;
	private RecordSet											recordSet;
	private int														retryCounter						= 3;
	private long													timeStamp;
	private boolean												isChargeCurrentAdded		= false;
	private boolean												isDischargeCurrentAdded	= false;
	private boolean												isCollectDataStopped		= false;

	private final Channels								channels;
	private final OpenSerialDataExplorer	application;

	/**
	 * constructor
	 * @param name
	 * @param channel byte signature
	 * @param serial port
	 * @param aCapacity
	 * @param aZellenZahl
	 * @param aAkkuTyp
	 * @param aProgramm
	 * @param aLadestromMilliA
	 * @param aEntladeStromMilliA
	 * @param aAnzahlWiederholungen
	 * @param aWarteZeitMin
	 */
	public AkkuMasterChannelTab(AkkuMasterC4Dialog parent, String name, byte[] channelSig, AkkuMasterC4SerialPort serialPort, Channel channel, String[] aCapacity, String[] aZellenZahl,
			String[] aAkkuTyp, String[] aProgramm, String[] aLadestromMilliA, String[] aEntladeStromMilliA, String[] aAnzahlWiederholungen, String[] aWarteZeitMin) {
		this.parent = parent;
		this.name = name;
		this.channelSig = channelSig;
		this.serialPort = serialPort;
		this.channel = channel;
		this.aCapacity = aCapacity;
		this.aZellenZahl = aZellenZahl;
		this.aAkkuTyp = aAkkuTyp;
		this.aProgramm = aProgramm;
		this.aLadestromMilliA = aLadestromMilliA;
		this.aEntladeStromMilliA = aEntladeStromMilliA;
		this.aAnzahlWiederholungen = aAnzahlWiederholungen;
		this.aWarteZeitMin = aWarteZeitMin;
		this.channels = Channels.getInstance();
		this.application = OpenSerialDataExplorer.getInstance();
	};

	/**
	 * add the tab to the dialog
	 */
	public void addChannelTab(CTabFolder tabFolder) {
		{
			kanal1Tab = new CTabItem(tabFolder, SWT.NONE);
			kanal1Tab.setText(name);
			{
				kanalKomposite = new Composite(tabFolder, SWT.NONE);
				kanal1Tab.setControl(kanalKomposite);
				kanalKomposite.setLayout(null);
				{
					dataGatherOnlyGroup = new Group(kanalKomposite, SWT.NONE);
					dataGatherOnlyGroup.setLayout(null);
					dataGatherOnlyGroup.setBounds(14, 8, 401, 82);
					{
						direktStartText = new Text(dataGatherOnlyGroup, SWT.MULTI | SWT.WRAP);
						direktStartText.setText("Mit dieser Funktion kann ein am Ladegerät gestarteter Vorgang aufgenommen werden");
						direktStartText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
						direktStartText.setBounds(51, 40, 315, 37);
					}
					{
						direktStartButton = new Button(dataGatherOnlyGroup, SWT.RADIO | SWT.LEFT);
						direktStartButton.setText("  Nur Datenaufnahme");
						direktStartButton.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
						direktStartButton.setBounds(0, 12, 310, 22);
						direktStartButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("direktStartButton.widgetSelected, event=" + evt);
								if (direktStartButton.getSelection()) {
									try {
										if (serialPort.isConnected())
											updateAdjustedValues();
										else {
											direktStartButton.setSelection(false);
											OpenSerialDataExplorer.getInstance().openMessageDialog("Erst den seriellen Port öffnen");
										}
									}
									catch (IOException e) {
										OpenSerialDataExplorer.getInstance().openMessageDialog("Das angeschlossene Gerät antwortet nicht auf dem seriellen Port!");
									}
									isDirectStart = true;
									programmButton.setSelection(false);
								}
							}
						});
					}
				}
				{
					programmGroup = new Group(kanalKomposite, SWT.NONE);
					programmGroup.setLayout(null);
					programmGroup.setBounds(12, 88, 403, 334);
					{
						programmButton = new Button(programmGroup, SWT.RADIO | SWT.LEFT);
						programmButton.setText("  Selbst konfiguriertes Programm");
						programmButton.setBounds(0, 10, 295, 21);
						programmButton.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
						programmButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("direktStartButton.widgetSelected, event=" + evt);
								if (programmButton.getSelection()) {
									try {
										if (serialPort.isConnected())
											updateAdjustedValues();
										else {
											programmButton.setSelection(false);
											OpenSerialDataExplorer.getInstance().openMessageDialog("Erst den seriellen Port öffnen");
										}
									}
									catch (IOException e) {
										OpenSerialDataExplorer.getInstance().openMessageDialog("Das angeschlossene Gerät antwortet nicht auf dem seriellen Port!");
									}
									isDirectStart = false;
									direktStartButton.setSelection(false);
								}
							}
						});
					}
					{
						akkuGroup = new Group(programmGroup, SWT.NONE);
						akkuGroup.setLayout(null);
						akkuGroup.setText("Akku");
						akkuGroup.setBounds(15, 51, 369, 67);
						{
							capacityText = new Text(akkuGroup, SWT.NONE);
							capacityText.setBounds(12, 20, 105, 18);
							capacityText.setText(" Kapazität [mAh]");
							capacityText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							capacityText.setEditable(false);
						}
						{
							capacityMilliAh = new CCombo(akkuGroup, SWT.NONE);
							capacityMilliAh.setItems(aCapacity);
							capacityMilliAh.setText(aCapacity[5]);
							capacityMilliAh.setBounds(12, 40, 105, 18);
						}
						{
							zellenAnzahlText = new Text(akkuGroup, SWT.NONE);
							zellenAnzahlText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							zellenAnzahlText.setBounds(130, 20, 105, 18);
							zellenAnzahlText.setText("  Zellenzahl");
							zellenAnzahlText.setEditable(false);
						}
						{
							zellenAnzahl = new CCombo(akkuGroup, SWT.NONE);
							zellenAnzahl.setBounds(130, 40, 105, 18);
							zellenAnzahl.setItems(aZellenZahl);
							zellenAnzahl.setText(aZellenZahl[3]);
							zellenAnzahl.setEditable(false);
							zellenAnzahl.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
						}
						{
							akkuTypText = new Text(akkuGroup, SWT.NONE);
							akkuTypText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							akkuTypText.setBounds(255, 20, 105, 18);
							akkuTypText.setText("   Akkutyp");
							akkuTypText.setDoubleClickEnabled(false);
							akkuTypText.setDragDetect(false);
							akkuTypText.setEditable(false);
						}
						{
							akkuTyp = new CCombo(akkuGroup, SWT.NONE);
							akkuTyp.setBounds(255, 40, 105, 18);
							akkuTyp.setItems(aAkkuTyp);
							akkuTyp.setText(aAkkuTyp[0]);
							akkuTyp.setEditable(false);
							akkuTyp.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
						}
					}
					{
						programmtypGroup = new Group(programmGroup, SWT.NONE);
						programmtypGroup.setBounds(15, 122, 369, 123);
						programmtypGroup.setText("Programmtyp");
						programmtypGroup.setLayout(null);
						{
							programmText = new Text(programmtypGroup, SWT.NONE);
							programmText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							programmText.setBounds(130, 20, 105, 18);
							programmText.setText("Programmname");
						}
						{
							programm = new CCombo(programmtypGroup, SWT.NONE);
							programm.setBounds(12, 40, 347, 18);
							programm.setItems(aProgramm);
							programm.setText(aProgramm[2]);
							programm.setEditable(false);
							programm.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
						}
						{
							ladeStromText = new Text(programmtypGroup, SWT.NONE);
							ladeStromText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							ladeStromText.setBounds(12, 70, 105, 18);
							ladeStromText.setText("  Ladestrom [mA]");
							ladeStromText.setEditable(false);
						}
						{
							ladeStromMilliA = new CCombo(programmtypGroup, SWT.NONE);
							ladeStromMilliA.setBounds(12, 93, 105, 18);
							ladeStromMilliA.setItems(aLadestromMilliA);
							ladeStromMilliA.setText(aLadestromMilliA[5]);
						}
						{
							entladeStromText = new Text(programmtypGroup, SWT.NONE);
							entladeStromText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							entladeStromText.setBounds(130, 70, 105, 18);
							entladeStromText.setDragDetect(false);
							entladeStromText.setDoubleClickEnabled(false);
							entladeStromText.setText("Entladestrom [mA]");
							entladeStromText.setEditable(false);
						}
						{
							entladeStromMilliA = new CCombo(programmtypGroup, SWT.NONE);
							entladeStromMilliA.setBounds(130, 93, 105, 18);
							entladeStromMilliA.setItems(aEntladeStromMilliA);
							entladeStromMilliA.setText(aEntladeStromMilliA[5]);
						}
						{
							waitTimeDaysText = new Text(programmtypGroup, SWT.NONE);
							waitTimeDaysText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							waitTimeDaysText.setBounds(255, 70, 105, 18);
							waitTimeDaysText.setText("Wartezeit [Min]");
							waitTimeDaysText.setEditable(false);
						}
						{
							waitTimeDays = new CCombo(programmtypGroup, SWT.NONE);
							waitTimeDays.setBounds(255, 93, 105, 18);
							waitTimeDays.setText("5");
							waitTimeDays.setEnabled(false);
						}
					}
					{
						programmwiederholungGroup = new Group(programmGroup, SWT.NONE);
						programmwiederholungGroup.setLayout(null);
						programmwiederholungGroup.setBounds(15, 250, 369, 75);
						programmwiederholungGroup.setText("Programmwiederholung");
						{
							anzahlWiederholungText = new Text(programmwiederholungGroup, SWT.NONE);
							anzahlWiederholungText.setBounds(23, 23, 60, 18);
							anzahlWiederholungText.setText("Anzahl");
							anzahlWiederholungText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							anzahlWiederholungText.setEditable(false);
							anzahlWiederholungText.setEnabled(false);
						}
						{
							anzahlWiederholungen = new CCombo(programmwiederholungGroup, SWT.NONE);
							anzahlWiederholungen.setBounds(12, 49, 89, 18);
							anzahlWiederholungen.setEditable(false);
							anzahlWiederholungen.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
							anzahlWiederholungen.setEnabled(false);
							anzahlWiederholungen.setItems(aAnzahlWiederholungen);
							anzahlWiederholungen.setText(aAnzahlWiederholungen[0]);
						}
						{
							warteZeitMinText = new Text(programmwiederholungGroup, SWT.NONE);
							warteZeitMinText.setBounds(128, 23, 90, 18);
							warteZeitMinText.setText("Wartezeit [Min]");
							warteZeitMinText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							warteZeitMinText.setEditable(false);
							warteZeitMinText.setEnabled(false);
						}
						{
							warteZeitMin = new CCombo(programmwiederholungGroup, SWT.NONE);
							warteZeitMin.setBounds(128, 49, 90, 18);
							warteZeitMin.setItems(aWarteZeitMin);
							warteZeitMin.setText(aWarteZeitMin[2]);
							warteZeitMin.setEnabled(false);
							warteZeitMin.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
						}
						{
							wiederholungSpeicher = new Text(programmwiederholungGroup, SWT.NONE);
							wiederholungSpeicher.setBounds(268, 23, 60, 18);
							wiederholungSpeicher.setText(" Speicher");
							wiederholungSpeicher.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							wiederholungSpeicher.setEditable(false);
							wiederholungSpeicher.setEnabled(false);
						}
						{
							wiederholungen = new CCombo(programmwiederholungGroup, SWT.NONE);
							wiederholungen.setBounds(259, 49, 83, 18);
							wiederholungen.setText("0");
							wiederholungen.setEditable(false);
							wiederholungen.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
							wiederholungen.setEnabled(false);
						}
					}
					{
						programmwiederholungButton = new Button(programmGroup, SWT.CHECK | SWT.LEFT);
						programmwiederholungButton.setBounds(1, 274, 14, 30);
						programmwiederholungButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("programmwiederholungButton.widgetSelected, event=" + evt);
								if (programmwiederholungButton.getSelection()) {
									anzahlWiederholungText.setEnabled(true);
									anzahlWiederholungen.setEnabled(true);
									warteZeitMinText.setEnabled(true);
									warteZeitMin.setEnabled(true);
									wiederholungSpeicher.setEnabled(true);
									wiederholungen.setEnabled(true);
								}
								else {
									anzahlWiederholungText.setEnabled(false);
									anzahlWiederholungen.setEnabled(false);
									warteZeitMinText.setEnabled(false);
									warteZeitMin.setEnabled(false);
									wiederholungSpeicher.setEnabled(false);
									wiederholungen.setEnabled(false);
								}
							};
						});
					}
					{
						memoryNumberCombo = new CCombo(programmGroup, SWT.NONE);
						memoryNumberCombo.setBounds(270, 30, 105, 18);
						memoryNumberCombo.setItems(new String[] { "0", "1", "2", "3", "4", "5", "6", "7" });
						memoryNumberCombo.select(1);
						memoryNumberCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("programNumber.widgetSelected, event=" + evt);
								//TODO implement gathering of data and update corresponding fields
								log.fine("selectionIndex = " + memoryNumberCombo.getSelectionIndex());
								//								try {
								//									serialPort.setMemoryNumberCycleCoundSleepTime(programNumberCombo.getSelectionIndex() + 1, 1, 25);
								//									serialPort.print(serialPort.getAdjustedValues(channelSig));
								//								}
								//								catch (IOException e) {
								//									System.err.println(e.getMessage());
								//								}
							}
						});
					}
					{
						memoryNumberButton = new Button(programmGroup, SWT.CHECK | SWT.LEFT);
						memoryNumberButton.setBounds(70, 30, 190, 18);
						memoryNumberButton.setText("Speichernummer verwenden");
						memoryNumberButton.setBackground(SWTResourceManager.getColor(225, 224, 228));
					}
				}
				{
					startAufzeichnungButton = new Button(kanalKomposite, SWT.CHECK | SWT.LEFT);
					startAufzeichnungButton.setBounds(12, 428, 139, 28);
					startAufzeichnungButton.setText("  Start Aufzeichnung");
					startAufzeichnungButton.setSelection(isCollectData);
					startAufzeichnungButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("startAufzeichnungButton.widgetSelected, event=" + evt);
							startAufzeichnungButton.setSelection(true);
							if (!isCollectData) {
								isCollectData = true;

								try {
									if (isDirectStart) {
										updateAdjustedValues();
									}
									else {
										int programNumber = new Integer(programm.getText().split(" ")[0]).intValue();
										int waitTime_days = 1; // new Integer(warteZeitTage.getText()).intValue();
										int accuTyp = new Integer(akkuTyp.getText().split(" ")[0]).intValue();
										int cellCount = new Integer(zellenAnzahl.getText().split(" ")[0]).intValue();
										int akkuCapacity = new Integer(capacityMilliAh.getText()).intValue();
										int dischargeCurrent_mA = new Integer(entladeStromMilliA.getText()).intValue();
										int chargeCurrent_mA = new Integer(ladeStromMilliA.getText()).intValue();
										log.fine(" programNumber = " + programNumber + " waitTime_days = " + waitTime_days + " accuTyp = " + accuTyp + " cellCount = " + cellCount + " akkuCapacity = " + akkuCapacity
												+ " dischargeCurrent_mA = " + dischargeCurrent_mA + " chargeCurrent_mA = " + chargeCurrent_mA);
										serialPort.writeNewProgram(channelSig, programNumber, waitTime_days, accuTyp, cellCount, akkuCapacity, dischargeCurrent_mA, chargeCurrent_mA);

										int memoryNumber = memoryNumberCombo.getSelectionIndex();
										log.fine("memoryNumber =" + memoryNumber);

										//TODO add checkbox "benutze Speichernummer"
										//serialPort.setMemoryNumberCycleCoundSleepTime(channelSig, memoryNumber, 1, 120);

										if (parent.getMaxCurrent() < parent.getActiveCurrent() + dischargeCurrent_mA || parent.getMaxCurrent() < parent.getActiveCurrent() + chargeCurrent_mA) {
											application.openMessageDialog("Der für das Gerät erlaubte Gesammtstrom würde mit den angegebenen Werten für Entladestrom = " + dischargeCurrent_mA
													+ " mA oder für den Ladestrom = " + chargeCurrent_mA + " mA überschritten, bitte korrigieren.");
											isCollectData = false;
											startAufzeichnungButton.setSelection(true);
											return;
										}
										else {
											serialPort.start(channelSig);
											serialPort.ok(channelSig);
										}
									}
									channels.switchChannel(channel.getName());
									// prepare timed data gatherer thread
									int delay = 0;
									int period = application.getActiveConfig().getTimeStep_ms(); // repeat every 10 sec.
									timer = new Timer();
									timerTask = new TimerTask() {
										private Logger	log						= Logger.getLogger(this.getClass().getName());
										String					recordSetKey	= ") nicht definiert";
										HashMap<String, Object>	data;	// [8]

										public void run() {
											/*
											 * [0] String Aktueller Prozessname 			"4 ) Laden" = AkkuMaster aktiv Laden
											 * [1] int 		Aktuelle Fehlernummer				"0" = kein Fehler
											 * [2] int		Aktuelle Akkuspannung 			[mV]
											 * [3] int 		Aktueller Prozesssstrom 		[mA] 	(laden/entladen)
											 * [4] int 		Aktuelle Prozesskapazität		[mAh] (laden/entladen)
											 * [5] int 		Errechnete Leistung					[mW]			
											 * [6] int		Errechnete Energie					[mWh]			
											 * [7] int		Prozesszeit									[msec]			
											 */
											try {
												data = serialPort.getData(channelSig, 0, null);
												// check for no error state
												log.fine("error state = " + data.get(AkkuMasterC4SerialPort.PROCESS_ERROR_NO));
												if (0 == (Integer)data.get(AkkuMasterC4SerialPort.PROCESS_ERROR_NO)) {
													String processName = ((String)data.get(AkkuMasterC4SerialPort.PROCESS_NAME)).split(" ")[1];
													log.fine("processName = " + processName);

													// check if device is ready for data capturing
													int processNumber = new Integer(((String)data.get(AkkuMasterC4SerialPort.PROCESS_NAME)).split(" ")[0]).intValue();
													if (processNumber == 1 || processNumber == 2) { // 1=Laden; 2=Entladen - AkkuMaster activ
														// check state change waiting to discharge to charge
														// check if a record set matching for re-use is available and prepare a new if required
														log.fine(channel.getName() + "=" + channel.size());
														if (channel.size() == 0 || !channel.getRecordSetNames()[channel.getRecordSetNames().length - 1].endsWith(processName) || (new Date().getTime() - timeStamp) > 30000
																|| isCollectDataStopped) {
															isCollectDataStopped = false;
															// record set does not exist or is outdated, build a new name and create
															recordSetKey = (channel.size() + 1) + ") " + processName;
															channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, application.getActiveConfig(), true, false));
															log.fine(recordSetKey + " created for channel " + channel.getName());
															if (channel.getActiveRecordSet() == null) Channels.getInstance().getActiveChannel().setActiveRecordSet(recordSetKey);
															channel.get(recordSetKey).setAllDisplayable();
															channel.applyTemplate(recordSetKey);
															// switch the active record set if the current record set is child of active channel
															if (channel.getName().equals(channels.getActiveChannel().getName())) {
																application.getDataToolBar().addRecordSetName(recordSetKey);
																channels.getActiveChannel().getActiveRecordSet().switchRecordSet(recordSetKey);
															}
															// update discharge / charge current display
															int actualCurrent = ((Integer)data.get(AkkuMasterC4SerialPort.PROCESS_CURRENT)).intValue();
															if (processName.equals("Laden")) {
																parent.addTotalChargeCurrent(actualCurrent);
																isChargeCurrentAdded = true;
															}
															else if (processName.equals("Entladen")) {
																parent.addTotalDischargeCurrent(actualCurrent);
																isDischargeCurrentAdded = true;
															}
															if (processName.equals("Laden") && isDischargeCurrentAdded) {
																parent.subtractTotalChargeCurrent(actualCurrent);
															}
															else if (processName.equals("Entladen") && isChargeCurrentAdded) {
																parent.subtractTotalDischargeCurrent(actualCurrent);
															}
														}
														else {
															recordSetKey = channel.size() + ") " + processName;
															log.fine("re-using " + recordSetKey);
														}
														timeStamp = new Date().getTime();

														// prepare the data for adding to record set
														recordSet = channel.get(recordSetKey);
														// build the point array according curves from record set
														int[] points = new int[recordSet.size()];

														points[0] = ((Integer)data.get(AkkuMasterC4SerialPort.PROCESS_VOLTAGE)).intValue(); //Spannung 	[mV]
														points[1] = ((Integer)data.get(AkkuMasterC4SerialPort.PROCESS_CURRENT)).intValue(); //Strom 			[mA]
														// display adaption * 1000  -  / 1000
														points[2] = ((Integer)data.get(AkkuMasterC4SerialPort.PROCESS_CAPACITY)).intValue() * 1000; //Kapazität	[mAh] 
														points[3] = ((Integer)data.get(AkkuMasterC4SerialPort.PROCESS_POWER)).intValue() / 1000; 		//Leistung		[mW]
														points[4] = ((Integer)data.get(AkkuMasterC4SerialPort.PROCESS_ENERGIE)).intValue() / 1000; 	//Energie		[mWh]
														log.fine(points[0] + " mV; " + points[1] + " mA; " + points[2] + " mAh; " + points[3] + " mW; " + points[4] + " mWh");

														recordSet.addPoints(points, true);
														application.updateDataTable();
														application.updateDigitalWindow();
													}
													else {
														// only the voltage can be displayed and updated
														//String voltage = new Double(new Integer(measuredData[2]) / 1000.0).toString(); // V
														//String current = new Double(new Integer(measuredData[3])).toString(); // mA
														//application.updateDigitalLabelText(new String[] { voltage, current });

														// enable switching records sets
														if (0 == retryCounter--) {
															stopTimer();
															isCollectData = false;
															log.fine("Timer stopped AkkuMaster inactiv");
															retryCounter = 3;
														}
													}
												}
												else { // some error state
													log.fine("canceling timer due to error");
													if (!isDirectStart) try {
														serialPort.stop(channelSig);
													}
													catch (IOException e) {
														e.printStackTrace();
													}
													isCollectData = false;
													stopTimer();
													application.openMessageDialog("Das angeschlossenen Gerät meldet eine Fehlerstatus, bitte überprüfen.");
												}
											}
											catch (IOException e) {
												e.printStackTrace();
											}
										}
									};
									timer.scheduleAtFixedRate(timerTask, delay, period);
								}
								catch (IOException e1) {
									application.openMessageDialog("Das angeschlossene Gerät antwortet nicht auf dem seriellen Port!");
								}
							}
						}

					});
				}
				{
					stopAuzeichnungButton = new Button(kanalKomposite, SWT.PUSH | SWT.CENTER);
					stopAuzeichnungButton.setBounds(207, 428, 193, 28);
					stopAuzeichnungButton.setText("S t o p");
					stopAuzeichnungButton.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					stopAuzeichnungButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("stopAuzeichnungButton.widgetSelected, event=" + evt);
							startAufzeichnungButton.setSelection(false);
							if (!isDirectStart) try {
								serialPort.stop(channelSig);
							}
							catch (IOException e) {
								e.printStackTrace();
							}
							isCollectData = false;
							isCollectDataStopped = true;
							stopTimer();
						}
					});
				}
			}
		}
	}

	/**
	 * @throws IOException
	 */
	private void updateAdjustedValues() throws IOException {
		// update channel tab with values red from device
		if (serialPort != null) {
			String[] configuration = serialPort.getConfiguration(channelSig);
			if(log.isLoggable(Level.FINER)) serialPort.print(configuration);
			if (!configuration[0].equals("0")) { // AkkuMaster somehow active
				programm.setText(aProgramm[new Integer(configuration[2].split(" ")[0]).intValue() - 1]);
				akkuTyp.setText(aAkkuTyp[new Integer(configuration[3].split(" ")[0]).intValue()]);
				zellenAnzahl.select(new Integer(configuration[4].split(" ")[0]).intValue() - 1);
				capacityMilliAh.setText(configuration[5].split(" ")[0]);
				entladeStromMilliA.setText(configuration[6].split(" ")[0]);
				ladeStromMilliA.setText(configuration[7].split(" ")[0]);
				waitTimeDays.setText(configuration[7].split(" ")[0]);

				String[] adjustments = serialPort.getAdjustedValues(channelSig);
				memoryNumberCombo.select(new Integer(adjustments[0].split(" ")[0]));
				wiederholungen.setText(adjustments[0].split(" ")[0]);
				if(log.isLoggable(Level.FINER)) serialPort.print(adjustments);
			}
		}
		else
			OpenSerialDataExplorer.getInstance().openMessageDialog("Erst den seriellen Port öffnen");
	}

	/**
	 * stop the timer task thread, this tops data capturing
	 */
	public void stopTimer() {
		if (timerTask != null) timerTask.cancel();
		if (timer != null) timer.cancel();
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				startAufzeichnungButton.setSelection(false);
			}
		});
	}
}
