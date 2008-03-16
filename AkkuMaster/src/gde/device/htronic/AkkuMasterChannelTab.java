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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
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
	private Text													chargeCurrentText;
	private Button												stopDataGatheringButton;
	private Button												startDataGatheringButton;
	private Text													memoryNumberText;
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
	private Button												programmButton;
	private Composite											channelComposite;
	private boolean												isCaptureOnly						= false;
	private boolean 											isDefinedProgram				= false;
	private boolean												isDataGatheringEnabled	= false;
	private boolean												isStopButtonEnabled			= false;
	private String												capacityMilliAhValue		= "0";
	private int														countCellsValue					= 0;
	private int														akkuTypeValue 					= 0;
	private int														programValue 						= 0;
	private String												chargeCurrentValue			= "0";
	private String												dischargeCurrentValue		= "0";
	private int														memoryNumberValue 			= 1;
	
	private boolean												isCollectData						= false;
	private RecordSet											recordSet;
	private int														retryCounter						= 3;
	private long													timeStamp;
	private boolean												isChargeCurrentAdded		= false;
	private boolean												isDischargeCurrentAdded	= false;
	private boolean												isCollectDataStopped		= false;
	private boolean												isMemorySelectionChanged = false;
	private String												recordSetKey	= ") nicht definiert";


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
			{ // begin channel composite
				channelComposite = new Composite(tabFolder, SWT.NONE);
				channelTab.setControl(channelComposite);
				channelComposite.setLayout(null);
				channelComposite.addPaintListener( new PaintListener() {
					public void paintControl(PaintEvent evt) {
						startDataGatheringButton.setEnabled(isDataGatheringEnabled);
						stopDataGatheringButton.setEnabled(isStopButtonEnabled);
					}
				});
				
				{ // begin capture only group
					captureOnlyGroup = new Group(channelComposite, SWT.NONE);
					captureOnlyGroup.setLayout(null);
					captureOnlyGroup.setBounds(12, 8, 400, 80);
					captureOnlyGroup.addPaintListener( new PaintListener() {
						public void paintControl(PaintEvent evt) {
							captureOnlyButton.setSelection(isCaptureOnly);
						}
					});
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
										isCaptureOnly = true;
										isDefinedProgram = false;
										isDataGatheringEnabled = true;
										updateAdjustedValues();
									}
									catch (Exception e) {
										OpenSerialDataExplorer.getInstance().openMessageDialog("Bei der seriellen Kommunikation ist ein Fehler aufgetreten, bitte die Porteinstellung überprüfen. " 
												+ e.getClass().getCanonicalName() + " - " + e.getMessage());
									}
									captureOnlyButton.setSelection(isCaptureOnly);
									programmButton.setSelection(isDefinedProgram);
									startDataGatheringButton.setEnabled(isDataGatheringEnabled);
								}
							}
						});
					}
				} // end capture only group
				
				{ // begin program group
					programGroup = new Group(channelComposite, SWT.NONE);
					programGroup.setLayout(null);
					programGroup.setBounds(12, 95, 400, 250);
					programGroup.addPaintListener( new PaintListener() {
						public void paintControl(PaintEvent evt) {
							programmButton.setSelection(isDefinedProgram);
						}
					});
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
										isCaptureOnly = false;
										isDefinedProgram = true;
										isDataGatheringEnabled = true;
										updateAdjustedValues();
									}
									catch (Exception e) {
										OpenSerialDataExplorer.getInstance().openMessageDialog("Das angeschlossene Gerät antwortet nicht auf dem seriellen Port!");
									}
									captureOnlyButton.setSelection(isCaptureOnly);
									programmButton.setSelection(isDefinedProgram);
									startDataGatheringButton.setEnabled(isDataGatheringEnabled);
								}
							}
						});
					}
					{
						akkuGroup = new Group(programGroup, SWT.NONE);
						akkuGroup.setLayout(null);
						akkuGroup.setText("Akku");
						akkuGroup.setBounds(15, 40, 369, 67);
						akkuGroup.addPaintListener( new PaintListener() {
							public void paintControl(PaintEvent evt) {
								capacityMilliAh.setText(capacityMilliAhValue);
								countCells.select(countCellsValue);
								akkuType.setText(aAkkuTyp[akkuTypeValue]);
							}
						});
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
							capacityMilliAh.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									log.finest("capacityMilliAh.widgetSelected, event=" + evt);
									capacityMilliAhValue = capacityMilliAh.getText();
								}
							});
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
							countCells.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									log.finest("countCells.widgetSelected, event=" + evt);
									countCellsValue = countCells.getSelectionIndex();
								}
							});
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
							akkuType.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									log.finest("akkuType.widgetSelected, event=" + evt);
									akkuTypeValue = akkuType.getSelectionIndex();
								}
							});
						}
					}
					{
						programTypeGroup = new Group(programGroup, SWT.NONE);
						programTypeGroup.setBounds(15, 110, 369, 123);
						programTypeGroup.setText("Programmtyp");
						programTypeGroup.setLayout(null);
						programTypeGroup.addPaintListener( new PaintListener() {
							public void paintControl(PaintEvent evt) {
								program.setText(aProgramm[programValue]);
								chargeCurrent.setText(chargeCurrentValue);
								dischargeCurrent.setText(dischargeCurrentValue);
								memoryNumberCombo.select(memoryNumberValue);
							}
						});
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
									programValue = program.getSelectionIndex() + 1;
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
							chargeCurrent.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									log.finest("chargeCurrent.widgetSelected, event=" + evt);
									chargeCurrentValue = chargeCurrent.getText();
								}
							});
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
							dischargeCurrent.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									log.finest("dischargeCurrent.widgetSelected, event=" + evt);
									dischargeCurrentValue = dischargeCurrent.getText();
								}
							});
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
							memoryNumberCombo.setEditable(false);
							memoryNumberCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
							memoryNumberCombo.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									memoryNumberValue = memoryNumberCombo.getSelectionIndex();
									memoryNumberCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_CYAN));
									isMemorySelectionChanged = true;
								}
							});
						}
					}
				} // end program group
				
				{
					startDataGatheringButton = new Button(channelComposite, SWT.PUSH | SWT.CENTER);
					startDataGatheringButton.setBounds(12, 360, 190, 28);
					startDataGatheringButton.setText("S t a r t");
					startDataGatheringButton.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					startDataGatheringButton.setSelection(isCollectData);
					startDataGatheringButton.setEnabled(false);
					startDataGatheringButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("startAufzeichnungButton.widgetSelected, event=" + evt);
							isDataGatheringEnabled = false;
							isStopButtonEnabled = true;
							startDataGatheringButton.setEnabled(isDataGatheringEnabled);
							stopDataGatheringButton.setEnabled(isStopButtonEnabled);
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

										if (isMemorySelectionChanged) {
											int memoryNumber = memoryNumberCombo.getSelectionIndex();
											log.fine("memoryNumber =" + memoryNumber);
											serialPort.setMemoryNumberCycleCoundSleepTime(channelSig, memoryNumber, 2, 2000);
										}
										
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
										private Logger					log						= Logger.getLogger(this.getClass().getName());
										HashMap<String, Object>	data;																												// [8]

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
												if (0 == (Integer) data.get(AkkuMasterC4SerialPort.PROCESS_ERROR_NO)) {
													String processName = ((String) data.get(AkkuMasterC4SerialPort.PROCESS_NAME)).split(" ")[1];
													log.fine("processName = " + processName);

													// check if device is ready for data capturing
													int processNumber = new Integer(((String) data.get(AkkuMasterC4SerialPort.PROCESS_NAME)).split(" ")[0]).intValue();
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
															int actualCurrent = ((Integer) data.get(AkkuMasterC4SerialPort.PROCESS_CURRENT)).intValue();
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

														points[0] = new Integer((Integer) data.get(AkkuMasterC4SerialPort.PROCESS_VOLTAGE)).intValue(); //Spannung 	[mV]
														points[1] = new Integer((Integer) data.get(AkkuMasterC4SerialPort.PROCESS_CURRENT)).intValue(); //Strom 			[mA]
														// display adaption * 1000  -  / 1000
														points[2] = new Integer((Integer) data.get(AkkuMasterC4SerialPort.PROCESS_CAPACITY)).intValue() * 1000; //Kapazität	[mAh] 
														points[3] = new Integer((Integer) data.get(AkkuMasterC4SerialPort.PROCESS_POWER)).intValue() / 1000; //Leistung		[mW]
														points[4] = new Integer((Integer) data.get(AkkuMasterC4SerialPort.PROCESS_ENERGIE)).intValue() / 1000; //Energie		[mWh]
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
											catch (Exception e) {
												// exception is logged where it is thrown first log.log(Level.SEVERE, e.getMessage(), e);
												isCollectData = false;
												stopTimer();
												if (!parent.isDisposed()) application.openMessageDialog("Das angeschlossenen Gerät meldet einen Fehlerstatus, bitte überprüfen.");
											}
										}
									};
									timer.scheduleAtFixedRate(timerTask, delay, period);

								}
								catch (Exception e1) {
									application.openMessageDialog("Das angeschlossene Gerät antwortet nicht auf dem seriellen Port!");
								}
							}
						}

					});
				}
				{
					stopDataGatheringButton = new Button(channelComposite, SWT.PUSH | SWT.CENTER);
					stopDataGatheringButton.setBounds(225, 360, 190, 28);
					stopDataGatheringButton.setText("S t o p");
					stopDataGatheringButton.setEnabled(false);
					stopDataGatheringButton.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					stopDataGatheringButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("stopAuzeichnungButton.widgetSelected, event=" + evt);
							updateDialogAfterStop();
							if (!isCaptureOnly) try {
								serialPort.stop(channelSig);
							}
							catch (IOException e) {
								e.printStackTrace();
							}
							stopTimer();
							// hope this is the right record set
							RecordSet recordSet = channels.getActiveChannel().get(recordSetKey);
							if (recordSet != null) recordSet.setTableDisplayable(true); // enable table display after calculation
							application.updateDataTable();
						}
					});
				}
			} // end channel composite
		}
	}

	/**
	 * @throws IOException
	 */
	private void updateAdjustedValues() throws Exception {
		// update channel tab with values red from device
		if (!serialPort.isConnected()) 
			serialPort.open();
		String[] configuration = serialPort.getConfiguration(channelSig);
		if (log.isLoggable(Level.FINER)) serialPort.print(configuration);
		if (!configuration[0].equals("0")) { // AkkuMaster somehow active			
			programValue 						= new Integer(configuration[2].split(" ")[0]).intValue() - 1;
			program.setText(aProgramm[programValue]);
			
			akkuTypeValue 					= new Integer(configuration[3].split(" ")[0]).intValue();
			akkuType.setText(aAkkuTyp[akkuTypeValue]);
			
			countCellsValue					= new Integer(configuration[4].split(" ")[0]).intValue() - 1;
			countCells.select(countCellsValue);
			
			capacityMilliAhValue		= configuration[5].split(" ")[0];
			capacityMilliAh.setText(capacityMilliAhValue);
			
			chargeCurrentValue			= configuration[7].split(" ")[0];
			chargeCurrent.setText(chargeCurrentValue);
			
			dischargeCurrentValue		= configuration[6].split(" ")[0];
			dischargeCurrent.setText(dischargeCurrentValue);

			String[] adjustments = serialPort.getAdjustedValues(channelSig);
			memoryNumberValue 			= new Integer(adjustments[0].split(" ")[0]).intValue();
			memoryNumberCombo.select(memoryNumberValue);
			if (log.isLoggable(Level.FINER)) serialPort.print(adjustments);
		}
	}

	/**
	 * stop the timer task thread, this tops data capturing
	 */
	public void stopTimer() {
		if (timerTask != null) timerTask.cancel();
		if (timer != null) {
			timer.cancel();
			timer.purge();
		}
		
		isCollectData = false;
		isCollectDataStopped = true;
		
		if (Thread.currentThread().getId() == application.getThreadId()) {
			updateDialogAfterStop();
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					updateDialogAfterStop();
				}
			});
		}
	}

	/**
	 * updates dialog UI after stop timer operation
	 */
	private void updateDialogAfterStop() {
		isDataGatheringEnabled 		= false;
		isStopButtonEnabled				= false;
		isCaptureOnly							= false;
		isDefinedProgram					= false;
		isMemorySelectionChanged 	= false;
		startDataGatheringButton.setEnabled(isDataGatheringEnabled);
		stopDataGatheringButton.setEnabled(isStopButtonEnabled);

		captureOnlyButton.setSelection(isCaptureOnly);
		programmButton.setSelection(isDefinedProgram);
	}

	public boolean isDataColletionActive() {
		return isCollectData() && isCollectDataStopped();
	}

	/**
	 * @return the isCollectData
	 */
	public boolean isCollectData() {
		return isCollectData;
	}

	/**
	 * @return the isCollectDataStopped
	 */
	public boolean isCollectDataStopped() {
		return isCollectDataStopped;
	}
}
