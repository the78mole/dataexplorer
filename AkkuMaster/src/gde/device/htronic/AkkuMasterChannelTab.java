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

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
  * Implementation for one channel tab, this will be initialized according number of available channels
  * @author Winfried Brügmann
 */
public class AkkuMasterChannelTab {
	private Logger												log											= Logger.getLogger(this.getClass().getName());

	private AkkuMasterC4Dialog						parent;
	private String												name;
	private byte[]												channelSig;
	private String[]											aCapacity;
	private String[]											aCellCount;
	private String[]											aAkkuTyp;
	private String[]											aProgramm;
	private String[]											aChargeCurrent_mA;
	private String[]											aDischargeCurrent_mA;
	private String[]											aCycleCount;
	private String[]											aWaitTime_Min;
	private AkkuMasterC4SerialPort				serialPort;
	private Channel												channel;
	private Timer													timer;
	private TimerTask											timerTask;

	private CTabItem											channelTab;
	private Button												captureOnlyButton;
	private Group													programGroup;
	private Group													captureOnlyGroup;
	private CCombo												memoryNumberCombo;
	private CCombo												capacityMilliAh;
	private Group													akkuGroup;
	private CCombo												numberCycles;
	private Text													chargeCurrentText;
	private Button												stopAuzeichnungButton;
	private Button												startDataGatheringButton;
	private CCombo												waitTimeMin;
	private Text													memoryNumberText;
	private Text													waitTimeMinText;
	private Text													numberCyclesText;
	private CCombo												waitTimeDays;
	private Text													waitTimeDaysText;
	private CCombo												dischargeCurrent;
	private Text													dischargeCurrentText;
	private CCombo												chargeCurrent;
	private CCombo												program;
	private Text													programText;
	private Group													programTypeGroup;
	private Text													akkuTypeText;
	private CCombo												akkuType;
	private CCombo												countCells;
	private Text													countCellsText;
	private Text													capacityText;
	private Text													captureOnlyText;
	private Group													programCycleGroup;
	private Button												programmButton;
	private Composite											channelComposite;
	private boolean												isCaptureOnly						= true;
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
	 * constructor initialization of one channel tab
	 * @param name
	 * @param channel byte signature
	 * @param serialPort
	 * @param aCapacity
	 * @param aCellCount
	 * @param aAkkuTyp
	 * @param aProgramm
	 * @param aChargeCurrent_mA
	 * @param aDischargeCurrent_mA
	 * @param aCycleCount
	 * @param aWaitTime_Min
	 */
	public AkkuMasterChannelTab(AkkuMasterC4Dialog parent, String name, byte[] channelSig, AkkuMasterC4SerialPort serialPort, Channel channel, String[] aCapacity, String[] aCellCount,
			String[] aAkkuTyp, String[] aProgramm, String[] aChargeCurrent_mA, String[] aDischargeCurrent_mA, String[] aCycleCount, String[] aWaitTime_Min) {
		this.parent = parent;
		this.name = name;
		this.channelSig = channelSig;
		this.serialPort = serialPort;
		this.channel = channel;
		this.aCapacity = aCapacity;
		this.aCellCount = aCellCount;
		this.aAkkuTyp = aAkkuTyp;
		this.aProgramm = aProgramm;
		this.aChargeCurrent_mA = aChargeCurrent_mA;
		this.aDischargeCurrent_mA = aDischargeCurrent_mA;
		this.aCycleCount = aCycleCount;
		this.aWaitTime_Min = aWaitTime_Min;
		this.channels = Channels.getInstance();
		this.application = OpenSerialDataExplorer.getInstance();
	};

	/**
	 * add the tab to the dialog
	 */
	public void addChannelTab(CTabFolder tabFolder) {
		{
			channelTab = new CTabItem(tabFolder, SWT.NONE);
			channelTab.setText(name);
			{
				channelComposite = new Composite(tabFolder, SWT.NONE);
				channelTab.setControl(channelComposite);
				channelComposite.setLayout(null);
				{
					captureOnlyGroup = new Group(channelComposite, SWT.NONE);
					captureOnlyGroup.setLayout(null);
					captureOnlyGroup.setBounds(14, 8, 401, 82);
					{
						captureOnlyText = new Text(captureOnlyGroup, SWT.MULTI | SWT.WRAP);
						captureOnlyText.setText("Mit dieser Funktion kann ein am Ladegerät gestarteter Vorgang aufgenommen werden");
						captureOnlyText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
						captureOnlyText.setBounds(51, 40, 315, 37);
					}
					{
						captureOnlyButton = new Button(captureOnlyGroup, SWT.RADIO | SWT.LEFT);
						captureOnlyButton.setText("  Nur Datenaufnahme");
						captureOnlyButton.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
						captureOnlyButton.setBounds(12, 15, 310, 22);
						captureOnlyButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("captureOnlyButton.widgetSelected, event=" + evt);
								if (captureOnlyButton.getSelection()) {
									try {
										if (serialPort.isConnected()) {
											updateAdjustedValues();
											startDataGatheringButton.setEnabled(true);
										}
										else {
											captureOnlyButton.setSelection(false);
											OpenSerialDataExplorer.getInstance().openMessageDialog("Erst den seriellen Port öffnen");
										}
									}
									catch (IOException e) {
										OpenSerialDataExplorer.getInstance().openMessageDialog("Das angeschlossene Gerät antwortet nicht auf dem seriellen Port!");
									}
									isCaptureOnly = true;
									programmButton.setSelection(false);
								}
							}
						});
					}
				}
				{
					programGroup = new Group(channelComposite, SWT.NONE);
					programGroup.setLayout(null);
					programGroup.setBounds(12, 95, 403, 325);
					{
						programmButton = new Button(programGroup, SWT.RADIO | SWT.LEFT);
						programmButton.setText("  Selbst konfiguriertes Programm");
						programmButton.setBounds(12, 15, 295, 21);
						programmButton.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
						programmButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("programmButton.widgetSelected, event=" + evt);
								if (programmButton.getSelection()) {
									try {
										if (serialPort.isConnected()) {
											updateAdjustedValues();
											startDataGatheringButton.setEnabled(true);
										}
										else {
											programmButton.setSelection(false);
											OpenSerialDataExplorer.getInstance().openMessageDialog("Erst den seriellen Port öffnen");
										}
									}
									catch (IOException e) {
										OpenSerialDataExplorer.getInstance().openMessageDialog("Das angeschlossene Gerät antwortet nicht auf dem seriellen Port!");
									}
									isCaptureOnly = false;
									captureOnlyButton.setSelection(false);
								}
							}
						});
					}
					{
						akkuGroup = new Group(programGroup, SWT.NONE);
						akkuGroup.setLayout(null);
						akkuGroup.setText("Akku");
						akkuGroup.setBounds(15, 40, 369, 67);
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
							countCellsText = new Text(akkuGroup, SWT.NONE);
							countCellsText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							countCellsText.setBounds(130, 20, 105, 18);
							countCellsText.setText("  Zellenzahl");
							countCellsText.setEditable(false);
						}
						{
							countCells = new CCombo(akkuGroup, SWT.NONE);
							countCells.setBounds(130, 40, 105, 18);
							countCells.setItems(aCellCount);
							countCells.setText(aCellCount[3]);
							countCells.setEditable(false);
							countCells.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
						}
						{
							akkuTypeText = new Text(akkuGroup, SWT.NONE);
							akkuTypeText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							akkuTypeText.setBounds(255, 20, 105, 18);
							akkuTypeText.setText("   Akkutyp");
							akkuTypeText.setDoubleClickEnabled(false);
							akkuTypeText.setDragDetect(false);
							akkuTypeText.setEditable(false);
						}
						{
							akkuType = new CCombo(akkuGroup, SWT.NONE);
							akkuType.setBounds(255, 40, 105, 18);
							akkuType.setItems(aAkkuTyp);
							akkuType.setText(aAkkuTyp[0]);
							akkuType.setEditable(false);
							akkuType.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
						}
					}
					{
						programTypeGroup = new Group(programGroup, SWT.NONE);
						programTypeGroup.setBounds(15, 110, 369, 123);
						programTypeGroup.setText("Programmtyp");
						programTypeGroup.setLayout(null);
						{
							programText = new Text(programTypeGroup, SWT.NONE);
							programText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							programText.setBounds(130, 20, 105, 18);
							programText.setText("Programmname");
						}
						{
							program = new CCombo(programTypeGroup, SWT.NONE);
							program.setBounds(12, 40, 347, 18);
							program.setItems(aProgramm);
							program.select(2);
							program.setEditable(false);
							program.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
							program.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									log.finest("program.widgetSelected, event=" + evt);
									int cycleType = program.getSelectionIndex() + 1;
									switch (cycleType) {
									case 5:
										enableProgramCycle(true);
										break;

									default:
										enableProgramCycle(false);
										break;
									}
								}
							});
						}
						{
							chargeCurrentText = new Text(programTypeGroup, SWT.NONE);
							chargeCurrentText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							chargeCurrentText.setBounds(12, 70, 105, 18);
							chargeCurrentText.setText("  Ladestrom [mA]");
							chargeCurrentText.setEditable(false);
						}
						{
							chargeCurrent = new CCombo(programTypeGroup, SWT.NONE);
							chargeCurrent.setBounds(12, 93, 105, 18);
							chargeCurrent.setItems(aChargeCurrent_mA);
							chargeCurrent.setText(aChargeCurrent_mA[5]);
						}
						{
							dischargeCurrentText = new Text(programTypeGroup, SWT.NONE);
							dischargeCurrentText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							dischargeCurrentText.setBounds(130, 70, 105, 18);
							dischargeCurrentText.setDragDetect(false);
							dischargeCurrentText.setDoubleClickEnabled(false);
							dischargeCurrentText.setText("Entladestrom [mA]");
							dischargeCurrentText.setEditable(false);
						}
						{
							dischargeCurrent = new CCombo(programTypeGroup, SWT.NONE);
							dischargeCurrent.setBounds(130, 93, 105, 18);
							dischargeCurrent.setItems(aDischargeCurrent_mA);
							dischargeCurrent.setText(aDischargeCurrent_mA[5]);
						}
						{
							memoryNumberText = new Text(programTypeGroup, SWT.NONE);
							memoryNumberText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							memoryNumberText.setBounds(255, 70, 105, 18);
							memoryNumberText.setText("Speicher No");
							memoryNumberText.setEditable(false);
						}
						{
							memoryNumberCombo = new CCombo(programTypeGroup, SWT.NONE);
							memoryNumberCombo.setBounds(255, 93, 105, 18);
							memoryNumberCombo.setItems(new String[] { "0", "1", "2", "3", "4", "5", "6", "7" });
							memoryNumberCombo.select(1);
							memoryNumberCombo.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									//TODO check if values can be updated using the selected memory
								}
							});
						}
					}
					{
						programCycleGroup = new Group(programGroup, SWT.NONE);
						programCycleGroup.setLayout(null);
						programCycleGroup.setBounds(15, 240, 369, 75);
						programCycleGroup.setText("Programmwiederholung");
						{
							numberCyclesText = new Text(programCycleGroup, SWT.NONE);
							numberCyclesText.setBounds(23, 23, 60, 18);
							numberCyclesText.setText("Anzahl");
							numberCyclesText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							numberCyclesText.setEditable(false);
						}
						{
							numberCycles = new CCombo(programCycleGroup, SWT.NONE);
							numberCycles.setBounds(12, 49, 89, 18);
							numberCycles.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
							numberCycles.setItems(aCycleCount);
							numberCycles.select(0);
						}
						{
							waitTimeMinText = new Text(programCycleGroup, SWT.NONE);
							waitTimeMinText.setBounds(128, 23, 90, 18);
							waitTimeMinText.setText("Wartezeit [Min]");
							waitTimeMinText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							waitTimeMinText.setEditable(false);
						}
						{
							waitTimeMin = new CCombo(programCycleGroup, SWT.NONE);
							waitTimeMin.setBounds(128, 49, 90, 18);
							waitTimeMin.setItems(aWaitTime_Min);
							waitTimeMin.select(2);
							waitTimeMin.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
						}
						{
							waitTimeDaysText = new Text(programCycleGroup, SWT.NONE);
							waitTimeDaysText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							waitTimeDaysText.setBounds(268, 23, 60, 18);
							waitTimeDaysText.setText("Wartezeit [Tage]");
							waitTimeDaysText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							waitTimeDaysText.setEditable(false);
						}
						{
							waitTimeDays = new CCombo(programCycleGroup, SWT.NONE);
							waitTimeDays.setBounds(259, 49, 83, 18);
							waitTimeDays.setText("1");
							waitTimeDays.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
						}
						enableProgramCycle(false);
					}
				}
				{
					startDataGatheringButton = new Button(channelComposite, SWT.PUSH | SWT.CENTER);
					startDataGatheringButton.setBounds(12, 428, 190, 28);
					startDataGatheringButton.setText("S t a r t");
					startDataGatheringButton.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					startDataGatheringButton.setSelection(isCollectData);
					startDataGatheringButton.setEnabled(false);
					startDataGatheringButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("startAufzeichnungButton.widgetSelected, event=" + evt);
							startDataGatheringButton.setEnabled(false);
							stopAuzeichnungButton.setEnabled(true);
							if (!isCollectData) {
								isCollectData = true;

								try {
									if (isCaptureOnly) {
										updateAdjustedValues();
									}
									else {
										int programNumber = new Integer(program.getText().split(" ")[0]).intValue();
										int waitTime_days = 1; // new Integer(warteZeitTage.getText()).intValue();
										int accuTyp = new Integer(akkuType.getText().split(" ")[0]).intValue();
										int cellCount = new Integer(countCells.getText().split(" ")[0]).intValue();
										int akkuCapacity = new Integer(capacityMilliAh.getText()).intValue();
										int dischargeCurrent_mA = new Integer(dischargeCurrent.getText()).intValue();
										int chargeCurrent_mA = new Integer(chargeCurrent.getText()).intValue();
										log.fine(" programNumber = " + programNumber + " waitTime_days = " + waitTime_days + " accuTyp = " + accuTyp + " cellCount = " + cellCount + " akkuCapacity = " + akkuCapacity
												+ " dischargeCurrent_mA = " + dischargeCurrent_mA + " chargeCurrent_mA = " + chargeCurrent_mA);
										serialPort.writeNewProgram(channelSig, programNumber, waitTime_days, accuTyp, cellCount, akkuCapacity, dischargeCurrent_mA, chargeCurrent_mA);

										int memoryNumber = memoryNumberCombo.getSelectionIndex();
										log.fine("memoryNumber =" + memoryNumber);

										if (parent.getMaxCurrent() < parent.getActiveCurrent() + dischargeCurrent_mA || parent.getMaxCurrent() < parent.getActiveCurrent() + chargeCurrent_mA) {
											application.openMessageDialog("Der für das Gerät erlaubte Gesammtstrom würde mit den angegebenen Werten für Entladestrom = " + dischargeCurrent_mA
													+ " mA oder für den Ladestrom = " + chargeCurrent_mA + " mA überschritten, bitte korrigieren.");
											isCollectData = false;
											startDataGatheringButton.setSelection(true);
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
									int period = application.getActiveDevice().getTimeStep_ms(); // repeat every 10 sec.
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
												data = serialPort.getData(channelSig, 0, null, "");
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
															channel.put(recordSetKey, RecordSet.createRecordSet(name.trim(), recordSetKey, application.getActiveDevice(), true, false));
															log.fine(recordSetKey + " created for channel " + channel.getName());
															if (channel.getActiveRecordSet() == null) channel.setActiveRecordSet(recordSetKey);
															recordSet = channel.get(recordSetKey);
															recordSet.setTableDisplayable(false); // suppress table calc + display 
															recordSet.setAllDisplayable();
															channel.applyTemplate(recordSetKey);
															// switch the active record set if the current record set is child of active channel
															if (channel.getName().equals(channels.getActiveChannel().getName())) {
																application.getMenuToolBar().addRecordSetName(recordSetKey);
																channels.getActiveChannel().switchRecordSet(recordSetKey);
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

														points[0] = new Integer((Integer)data.get(AkkuMasterC4SerialPort.PROCESS_VOLTAGE)).intValue(); //Spannung 	[mV]
														points[1] = new Integer((Integer)data.get(AkkuMasterC4SerialPort.PROCESS_CURRENT)).intValue(); //Strom 			[mA]
														// display adaption * 1000  -  / 1000
														points[2] = new Integer((Integer)data.get(AkkuMasterC4SerialPort.PROCESS_CAPACITY)).intValue() * 1000; //Kapazität	[mAh] 
														points[3] = new Integer((Integer)data.get(AkkuMasterC4SerialPort.PROCESS_POWER)).intValue() / 1000; 		//Leistung		[mW]
														points[4] = new Integer((Integer)data.get(AkkuMasterC4SerialPort.PROCESS_ENERGIE)).intValue() / 1000; 	//Energie		[mWh]
														log.fine(points[0] + " mV; " + points[1] + " mA; " + points[2] + " mAh; " + points[3] + " mW; " + points[4] + " mWh");

														recordSet.addPoints(points, false); // updates data table and digital windows
														application.updateGraphicsWindow();
														application.updateDigitalWindowChilds();
														application.updateAnalogWindowChilds();
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
													if (!isCaptureOnly) try {
														serialPort.stop(channelSig);
													}
													catch (IOException e) {
														log.log(Level.SEVERE, e.getMessage(), e);
													}
													isCollectData = false;
													stopTimer();
													application.openMessageDialog("Das angeschlossenen Gerät meldet einen Fehlerstatus, bitte überprüfen.");
												}
											}
											catch (IOException e) {
												log.log(Level.SEVERE, e.getMessage(), e);
												isCollectData = false;
												stopTimer();
												if (!parent.isDisposed())
													application.openMessageDialog("Das angeschlossenen Gerät meldet einen Fehlerstatus, bitte überprüfen.");
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
					stopAuzeichnungButton = new Button(channelComposite, SWT.PUSH | SWT.CENTER);
					stopAuzeichnungButton.setBounds(225, 428, 190, 28);
					stopAuzeichnungButton.setText("S t o p");
					stopAuzeichnungButton.setEnabled(false);
					stopAuzeichnungButton.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					stopAuzeichnungButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("stopAuzeichnungButton.widgetSelected, event=" + evt);
							startDataGatheringButton.setEnabled(false);
							captureOnlyButton.setSelection(false);
							programmButton.setSelection(false);
							stopAuzeichnungButton.setEnabled(false);
							if (!isCaptureOnly) try {
								serialPort.stop(channelSig);
							}
							catch (IOException e) {
								e.printStackTrace();
							}
							isCollectData = false;
							isCollectDataStopped = true;
							stopTimer();
							// hope this is the right record set
							channels.getActiveChannel().getActiveRecordSet().setTableDisplayable(true); // enable table display after calculation
							application.updateDataTable();
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
				program.setText(aProgramm[new Integer(configuration[2].split(" ")[0]).intValue() - 1]);
				akkuType.setText(aAkkuTyp[new Integer(configuration[3].split(" ")[0]).intValue()]);
				countCells.select(new Integer(configuration[4].split(" ")[0]).intValue() - 1);
				capacityMilliAh.setText(configuration[5].split(" ")[0]);
				dischargeCurrent.setText(configuration[6].split(" ")[0]);
				chargeCurrent.setText(configuration[7].split(" ")[0]);
				waitTimeDays.setText(configuration[7].split(" ")[0]);

				String[] adjustments = serialPort.getAdjustedValues(channelSig);
				memoryNumberCombo.select(new Integer(adjustments[0].split(" ")[0]));
				numberCycles.setText(adjustments[1].split(" ")[0]);
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
		if (Thread.currentThread().getId() == application.getThreadId()) {
			startDataGatheringButton.setEnabled(false);
			captureOnlyButton.setSelection(false);
			programmButton.setSelection(false);
			stopAuzeichnungButton.setEnabled(false);
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					startDataGatheringButton.setEnabled(false);
					captureOnlyButton.setSelection(false);
					programmButton.setSelection(false);
					stopAuzeichnungButton.setEnabled(false);
				}
			});
		}
	}

	/**
	 * enalbe program cycle group
	 */
	private void enableProgramCycle(boolean value) {
		numberCyclesText.setEnabled(value);
		numberCycles.setEnabled(value);
		waitTimeMinText.setEnabled(value);
		waitTimeMin.setEnabled(value);
		waitTimeDaysText.setEnabled(value);
		waitTimeDays.setEnabled(value);
	}
}
