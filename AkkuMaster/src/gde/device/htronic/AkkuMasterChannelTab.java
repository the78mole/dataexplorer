/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
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
import osde.exception.DataInconsitsentException;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
  * Implementation for one channel tab, this will be initialized according number of available channels
  * @author Winfried Brügmann
 */
public class AkkuMasterChannelTab {
	final static Logger						log												= Logger.getLogger(AkkuMasterChannelTab.class.getName());

	AkkuMasterC4Dialog						parent;
	String												name;
	byte[]												channelSig;
	String[]											aCapacity;
	String[]											aCellCount;
	String[]											aAkkuTyp;
	String[]											aProgramm;
	String[]											aChargeCurrent_mA;
	String[]											aDischargeCurrent_mA;
	AkkuMasterC4SerialPort				serialPort;
	Channel												channel;
	Timer													timer;
	TimerTask											timerTask;

	CTabItem											channelTab;
	Button												captureOnlyButton;
	Group													programGroup;
	Group													captureOnlyGroup;
	CCombo												memoryNumberCombo;
	CCombo												capacityMilliAh;
	Group													akkuGroup;
	Text													chargeCurrentText;
	Button												stopDataGatheringButton;
	Button												startDataGatheringButton;
	Text													memoryNumberText;
	CCombo												dischargeCurrent;
	Text													dischargeCurrentText;
	CCombo												chargeCurrent;
	CCombo												program;
	Text													programText;
	Group													programTypeGroup;
	Text													akkuTypeText;
	CCombo												akkuType;
	CCombo												countCells;
	Text													countCellsText;
	Text													capacityText;
	Text													captureOnlyText;
	Button												programmButton;
	Composite											channelComposite;
	boolean												isCaptureOnly							= false;
	boolean												isDefinedProgram					= false;
	boolean												isDataGatheringEnabled		= false;
	boolean												isStopButtonEnabled				= false;
	String												capacityMilliAhValue			= "0"; //$NON-NLS-1$
	int														countCellsValue						= 0;
	int														akkuTypeValue							= 0;
	int														programValue							= 0;
	String												chargeCurrentValue				= "0"; //$NON-NLS-1$
	String												dischargeCurrentValue			= "0"; //$NON-NLS-1$
	int														memoryNumberValue					= 1;

	boolean												isCollectData							= false;
	boolean												isGatheredRecordSetVisible	= true;
	RecordSet											recordSet;
	int														retryCounter							= 3;
	long													timeStamp;
	boolean												isChargeCurrentAdded			= false;
	boolean												isDischargeCurrentAdded		= false;
	boolean												isCollectDataStopped			= true;
	boolean												isMemorySelectionChanged	= false;
	String												recordSetKey							= Messages.getString(osde.messages.MessageIds.OSDE_MSGT0272);

	final Channels								channels;
	final OpenSerialDataExplorer	application;

	/**
	 * constructor initialization of one channel tab
	 * @param newName
	 * @param useChannel byte signature
	 * @param useSerialPort
	 * @param arrayCapacity
	 * @param arrayCellCount
	 * @param arrayAkkuTyp
	 * @param arrayProgramm
	 * @param arrayChargeCurrent_mA
	 * @param arrayDischargeCurrent_mA
	 */
	public AkkuMasterChannelTab(AkkuMasterC4Dialog useParent, String newName, byte[] useChannelSig, AkkuMasterC4SerialPort useSerialPort, Channel useChannel, String[] arrayCapacity,
			String[] arrayCellCount, String[] arrayAkkuTyp, String[] arrayProgramm, String[] arrayChargeCurrent_mA, String[] arrayDischargeCurrent_mA) {
		this.parent = useParent;
		this.name = newName;
		this.channelSig = useChannelSig;
		this.serialPort = useSerialPort;
		this.channel = useChannel;
		this.aCapacity = arrayCapacity;
		this.aCellCount = arrayCellCount;
		this.aAkkuTyp = arrayAkkuTyp;
		this.aProgramm = arrayProgramm;
		this.aChargeCurrent_mA = arrayChargeCurrent_mA;
		this.aDischargeCurrent_mA = arrayDischargeCurrent_mA;
		this.channels = Channels.getInstance();
		this.application = OpenSerialDataExplorer.getInstance();
	}

	/**
	 * add the tab to the dialog
	 */
	public void addChannelTab(CTabFolder tabFolder) {
		{
			this.channelTab = new CTabItem(tabFolder, SWT.NONE);
			this.channelTab.setText(this.name);
			{ // begin channel composite
				this.channelComposite = new Composite(tabFolder, SWT.NONE);
				this.channelTab.setControl(this.channelComposite);
				this.channelComposite.setLayout(null);
				this.channelComposite.addMouseTrackListener(this.parent.getDevice().getDialog().mouseTrackerEnterFadeOut);
				this.channelComposite.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.log(Level.FINEST, "channelComposite.widgetSelected, event=" + evt); //$NON-NLS-1$
						updateStartDataGatheringButton();
						updateStopDataGatheringButton();
					}
				});

				{ // begin capture only group
					this.captureOnlyGroup = new Group(this.channelComposite, SWT.NONE);
					this.captureOnlyGroup.setLayout(null);
					this.captureOnlyGroup.setBounds(12, 8, 400, 80);
					this.captureOnlyGroup.addMouseTrackListener(this.parent.getDevice().getDialog().mouseTrackerEnterFadeOut);
					this.captureOnlyGroup.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							log.log(Level.FINEST, "captureOnlyGroup.widgetSelected, event=" + evt); //$NON-NLS-1$
							updateCaptureOnlyButton();
						}
					});
					{
						this.captureOnlyText = new Text(this.captureOnlyGroup, SWT.MULTI | SWT.WRAP);
						this.captureOnlyText.setText(Messages.getString(MessageIds.OSDE_MSGT1173));
						this.captureOnlyText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
						this.captureOnlyText.setBounds(51, 40, 315, 37);
					}
					{
						this.captureOnlyButton = new Button(this.captureOnlyGroup, SWT.RADIO | SWT.LEFT);
						this.captureOnlyButton.setText(Messages.getString(MessageIds.OSDE_MSGT1174)); 
						this.captureOnlyButton.setFont(SWTResourceManager.getFont(this.captureOnlyButton, SWT.BOLD));
						this.captureOnlyButton.setBounds(12, 15, 310, 22);
						this.captureOnlyButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "captureOnlyButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								if (getCaptureOnlyButtonSelection()) {
									try {
										setCaptureOnly(true);
										setDefinedProgram(false);
										setDataGatheringEnabled(true);
										updateAdjustedValues();
									}
									catch (Exception e) {
										OpenSerialDataExplorer.getInstance().openMessageDialog(
												Messages.getString(osde.messages.MessageIds.OSDE_MSGE0022, new Object[] {e.getClass().getSimpleName(), e.getMessage() } )); 
									}
									updateCaptureOnlyButton();
									updateProgramButton();
									updateStartDataGatheringButton();
								}
							}
						});
					}
				} // end capture only group

				{ // begin program group
					this.programGroup = new Group(this.channelComposite, SWT.NONE);
					this.programGroup.setLayout(null);
					this.programGroup.setBounds(12, 95, 400, 250);
					this.programGroup.addMouseTrackListener(this.parent.getDevice().getDialog().mouseTrackerEnterFadeOut);
					this.programGroup.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							log.log(Level.FINEST, "programGroup.widgetSelected, event=" + evt); //$NON-NLS-1$
							updateProgramButton();
						}
					});
					{
						this.programmButton = new Button(this.programGroup, SWT.RADIO | SWT.LEFT);
						this.programmButton.setText(Messages.getString(MessageIds.OSDE_MSGT1175));
						this.programmButton.setBounds(12, 15, 295, 21);
						this.programmButton.setFont(SWTResourceManager.getFont(this.captureOnlyButton, SWT.BOLD));
						this.programmButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "programmButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								if (getProgramButtonSelection()) {
									try {
										setCaptureOnly(false);
										setDefinedProgram(true);
										setDataGatheringEnabled(true);
										updateAdjustedValues();
									}
									catch (Exception e) {
										OpenSerialDataExplorer.getInstance().openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0026));
									}
									updateCaptureOnlyButton();
									updateProgramButton();
									updateStartDataGatheringButton();
								}
							}
						});
					}
					{
						this.akkuGroup = new Group(this.programGroup, SWT.NONE);
						this.akkuGroup.setLayout(null);
						this.akkuGroup.setText(Messages.getString(MessageIds.OSDE_MSGT1176));
						this.akkuGroup.setBounds(15, 40, 369, 67);
						this.akkuGroup.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								log.log(Level.FINEST, "akkuGroup.widgetSelected, event=" + evt); //$NON-NLS-1$
								updateCapacityMilliAhText();
								updateCountCellSelection();
								updateAkkuType();
							}
						});
						{
							this.capacityText = new Text(this.akkuGroup, SWT.NONE);
							this.capacityText.setBounds(12, 20, 105, 18);
							this.capacityText.setText(Messages.getString(MessageIds.OSDE_MSGT1177));
							this.capacityText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.capacityText.setEditable(false);
						}
						{
							this.capacityMilliAh = new CCombo(this.akkuGroup, SWT.NONE);
							this.capacityMilliAh.setItems(this.aCapacity);
							this.capacityMilliAh.setText(this.aCapacity[5]);
							this.capacityMilliAh.setBounds(12, 40, 105, 18);
							this.capacityMilliAh.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "capacityMilliAh.widgetSelected, event=" + evt); //$NON-NLS-1$
									updateCapacityMilliAhValue();
								}
							});
						}
						{
							this.countCellsText = new Text(this.akkuGroup, SWT.NONE);
							this.countCellsText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.countCellsText.setBounds(130, 20, 105, 18);
							this.countCellsText.setText(Messages.getString(MessageIds.OSDE_MSGT1178));
							this.countCellsText.setEditable(false);
						}
						{
							this.countCells = new CCombo(this.akkuGroup, SWT.NONE);
							this.countCells.setBounds(130, 40, 105, 18);
							this.countCells.setItems(this.aCellCount);
							this.countCells.setText(this.aCellCount[3]);
							this.countCells.setEditable(false);
							this.countCells.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
							this.countCells.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "countCells.widgetSelected, event=" + evt); //$NON-NLS-1$
									updateCellCountValue();
								}
							});
						}
						{
							this.akkuTypeText = new Text(this.akkuGroup, SWT.NONE);
							this.akkuTypeText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.akkuTypeText.setBounds(255, 20, 105, 18);
							this.akkuTypeText.setText(Messages.getString(MessageIds.OSDE_MSGT1179));
							this.akkuTypeText.setDoubleClickEnabled(false);
							this.akkuTypeText.setDragDetect(false);
							this.akkuTypeText.setEditable(false);
						}
						{
							this.akkuType = new CCombo(this.akkuGroup, SWT.NONE);
							this.akkuType.setBounds(255, 40, 105, 18);
							this.akkuType.setItems(this.aAkkuTyp);
							this.akkuType.setText(this.aAkkuTyp[0]);
							this.akkuType.setEditable(false);
							this.akkuType.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
							this.akkuType.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "akkuType.widgetSelected, event=" + evt); //$NON-NLS-1$
									updateAkkuTypeValue();
								}
							});
						}
					}
					{
						this.programTypeGroup = new Group(this.programGroup, SWT.NONE);
						this.programTypeGroup.setBounds(15, 110, 369, 123);
						this.programTypeGroup.setText(Messages.getString(MessageIds.OSDE_MSGT1180));
						this.programTypeGroup.setLayout(null);
						this.programTypeGroup.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								log.log(Level.FINEST, "programTypeGroup.widgetSelected, event=" + evt); //$NON-NLS-1$
								updateProgramText();
								updateChargeCurrentText();
								updateDichargeCurrentText();
								updateMemoryNumberSelection();
							}
						});
						{
							this.programText = new Text(this.programTypeGroup, SWT.NONE);
							this.programText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.programText.setBounds(130, 20, 105, 18);
							this.programText.setText(Messages.getString(MessageIds.OSDE_MSGT1181)); 
						}
						{
							this.program = new CCombo(this.programTypeGroup, SWT.NONE);
							this.program.setBounds(12, 40, 347, 18);
							this.program.setItems(this.aProgramm);
							this.program.select(2);
							this.program.setEditable(false);
							this.program.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
							this.program.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "program.widgetSelected, event=" + evt); //$NON-NLS-1$
									updateProgramSelectionValue();
								}
							});
						}
						{
							this.chargeCurrentText = new Text(this.programTypeGroup, SWT.NONE);
							this.chargeCurrentText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.chargeCurrentText.setBounds(12, 70, 105, 18);
							this.chargeCurrentText.setText(Messages.getString(MessageIds.OSDE_MSGT1182));
							this.chargeCurrentText.setEditable(false);
						}
						{
							this.chargeCurrent = new CCombo(this.programTypeGroup, SWT.NONE);
							this.chargeCurrent.setBounds(12, 93, 105, 18);
							this.chargeCurrent.setItems(this.aChargeCurrent_mA);
							this.chargeCurrent.setText(this.aChargeCurrent_mA[5]);
							this.chargeCurrent.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "chargeCurrent.widgetSelected, event=" + evt); //$NON-NLS-1$
									updateChargeCurrentValue();
								}
							});
						}
						{
							this.dischargeCurrentText = new Text(this.programTypeGroup, SWT.NONE);
							this.dischargeCurrentText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.dischargeCurrentText.setBounds(130, 70, 105, 18);
							this.dischargeCurrentText.setDragDetect(false);
							this.dischargeCurrentText.setDoubleClickEnabled(false);
							this.dischargeCurrentText.setText(Messages.getString(MessageIds.OSDE_MSGT1183));
							this.dischargeCurrentText.setEditable(false);
						}
						{
							this.dischargeCurrent = new CCombo(this.programTypeGroup, SWT.NONE);
							this.dischargeCurrent.setBounds(130, 93, 105, 18);
							this.dischargeCurrent.setItems(this.aDischargeCurrent_mA);
							this.dischargeCurrent.setText(this.aDischargeCurrent_mA[5]);
							this.dischargeCurrent.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "dischargeCurrent.widgetSelected, event=" + evt); //$NON-NLS-1$
									updateDischargeCurrentValue();
								}
							});
						}
						{
							this.memoryNumberText = new Text(this.programTypeGroup, SWT.NONE);
							this.memoryNumberText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.memoryNumberText.setBounds(255, 70, 105, 18);
							this.memoryNumberText.setText(Messages.getString(MessageIds.OSDE_MSGT1184)); 
							this.memoryNumberText.setEditable(false);
						}
						{
							this.memoryNumberCombo = new CCombo(this.programTypeGroup, SWT.NONE);
							this.memoryNumberCombo.setBounds(255, 93, 105, 18);
							this.memoryNumberCombo.setItems(new String[] { "0", "1", "2", "3", "4", "5", "6", "7" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
							this.memoryNumberCombo.select(1);
							this.memoryNumberCombo.setEditable(false);
							this.memoryNumberCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
							this.memoryNumberCombo.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "memoryNumberCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
									updateMemoryNumberValue();
									setMemorySelectionChanged(true);
								}
							});
						}
					}
				} // end program group

				{
					this.startDataGatheringButton = new Button(this.channelComposite, SWT.PUSH | SWT.CENTER);
					this.startDataGatheringButton.setBounds(12, 360, 190, 28);
					this.startDataGatheringButton.setText(Messages.getString(osde.messages.MessageIds.OSDE_MSGT0277));
					this.startDataGatheringButton.setFont(SWTResourceManager.getFont(this.captureOnlyButton, SWT.BOLD));
					this.startDataGatheringButton.setSelection(this.isCollectData);
					this.startDataGatheringButton.setEnabled(false);
					this.startDataGatheringButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "startAufzeichnungButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							setDataGatheringEnabled(false);
							setStopButtonEnabled(true);
							updateStartDataGatheringButton();
							updateStopDataGatheringButton();
							if (!isCollectData()) {
								setCollectData(true);

								try {
									if (isCaptureOnly()) {
										updateAdjustedValues();
									}
									else {
										int programNumber = getProgramNumber();
										int waitTime_days = 1; // new Integer(warteZeitTage.getText()).intValue();
										int accuTyp = getAkkuType();
										int cellCount = getCellCount();
										int akkuCapacity = getAkkuCapacity();
										int dischargeCurrent_mA = getDischargeCurrent();
										int chargeCurrent_mA = getChargeCurrent();
										log.log(Level.FINE, " programNumber = " + programNumber + " waitTime_days = " + waitTime_days + " accuTyp = " + accuTyp + " cellCount = " + cellCount //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
												+ " akkuCapacity = " + akkuCapacity + " dischargeCurrent_mA = " + dischargeCurrent_mA + " chargeCurrent_mA = " + chargeCurrent_mA); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
										AkkuMasterChannelTab.this.serialPort.writeNewProgram(AkkuMasterChannelTab.this.channelSig, programNumber, waitTime_days, accuTyp, cellCount, akkuCapacity, dischargeCurrent_mA, chargeCurrent_mA);

										if (isMemorySelectionChanged()) {
											int memoryNumber = getMemoryNumberSelectionIndex();
											log.log(Level.FINE, "memoryNumber =" + memoryNumber); //$NON-NLS-1$
											AkkuMasterChannelTab.this.serialPort.setMemoryNumberCycleCountSleepTime(AkkuMasterChannelTab.this.channelSig, memoryNumber, 2, 2000);
										}

										if (AkkuMasterChannelTab.this.parent.getMaxCurrent() < AkkuMasterChannelTab.this.parent.getActiveCurrent() + dischargeCurrent_mA || AkkuMasterChannelTab.this.parent.getMaxCurrent() < AkkuMasterChannelTab.this.parent.getActiveCurrent() + chargeCurrent_mA) {
											AkkuMasterChannelTab.this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGW1100, new Object[] { dischargeCurrent_mA, chargeCurrent_mA } ));
											setCollectData(false);
											setStartDataGatheringSelection(true);
											return;
										}

										AkkuMasterChannelTab.this.serialPort.start(AkkuMasterChannelTab.this.channelSig);
										AkkuMasterChannelTab.this.serialPort.ok(AkkuMasterChannelTab.this.channelSig);
									}
									AkkuMasterChannelTab.this.channels.switchChannel(AkkuMasterChannelTab.this.channel.getName());
									// prepare timed data gatherer thread
									int delay = 0;
									int period = AkkuMasterChannelTab.this.application.getActiveDevice().getTimeStep_ms().intValue(); // repeat every 10 sec.
									setTimer(new Timer());
									setTimerTask(new TimerTask() {
										HashMap<String, Object>	data; // [8]

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
												this.data = AkkuMasterChannelTab.this.serialPort.getData(AkkuMasterChannelTab.this.channelSig);
												// check for no error state
												log.log(Level.FINE, "error state = " + this.data.get(AkkuMasterC4SerialPort.PROCESS_ERROR_NO)); //$NON-NLS-1$
												if (0 == (Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_ERROR_NO)) {
													String processName = ((String) this.data.get(AkkuMasterC4SerialPort.PROCESS_NAME)).split(" ")[1].trim(); //$NON-NLS-1$
													log.log(Level.FINE, "processName = " + processName); //$NON-NLS-1$

													// check if device is ready for data capturing
													int processNumber = new Integer(((String) this.data.get(AkkuMasterC4SerialPort.PROCESS_NAME)).split(" ")[0]).intValue(); //$NON-NLS-1$
													if (processNumber == 1 || processNumber == 2) { // 1=Laden; 2=Entladen - AkkuMaster activ
														// check state change waiting to discharge to charge
														// check if a record set matching for re-use is available and prepare a new if required
														log.log(Level.FINE, AkkuMasterChannelTab.this.channel.getName() + "=" + AkkuMasterChannelTab.this.channel.size()); //$NON-NLS-1$
														if (AkkuMasterChannelTab.this.channel.size() == 0 
																|| !AkkuMasterChannelTab.this.channel.getRecordSetNames()[AkkuMasterChannelTab.this.channel.getRecordSetNames().length - 1].endsWith(" " + processName) //$NON-NLS-1$
																|| (new Date().getTime() - getTimeStamp()) > 30000 || isCollectDataStopped()) {
															setCollectDataStopped(false);
															// record set does not exist or is outdated, build a new name and create
															AkkuMasterChannelTab.this.recordSetKey = AkkuMasterChannelTab.this.channel.getNextRecordSetNumber() + ") " + processName; //$NON-NLS-1$
															AkkuMasterChannelTab.this.channel.put(AkkuMasterChannelTab.this.recordSetKey, RecordSet.createRecordSet(getName().trim(), AkkuMasterChannelTab.this.recordSetKey, AkkuMasterChannelTab.this.application.getActiveDevice(), true, false));
															AkkuMasterChannelTab.this.channel.applyTemplateBasics(AkkuMasterChannelTab.this.recordSetKey);
															log.log(Level.FINE, AkkuMasterChannelTab.this.recordSetKey + " created for channel " + AkkuMasterChannelTab.this.channel.getName()); //$NON-NLS-1$
															if (AkkuMasterChannelTab.this.channel.getActiveRecordSet() == null) AkkuMasterChannelTab.this.channel.setActiveRecordSet(AkkuMasterChannelTab.this.recordSetKey);
															AkkuMasterChannelTab.this.recordSet = AkkuMasterChannelTab.this.channel.get(AkkuMasterChannelTab.this.recordSetKey);
															AkkuMasterChannelTab.this.recordSet.setTableDisplayable(false); // suppress table calc + display 
															AkkuMasterChannelTab.this.recordSet.setAllDisplayable();
															AkkuMasterChannelTab.this.channel.applyTemplate(AkkuMasterChannelTab.this.recordSetKey);
															// switch the active record set if the current record set is child of active channel
															if (AkkuMasterChannelTab.this.channel.getName().equals(AkkuMasterChannelTab.this.channels.getActiveChannel().getName())) {
																AkkuMasterChannelTab.this.application.getMenuToolBar().addRecordSetName(AkkuMasterChannelTab.this.recordSetKey);
																AkkuMasterChannelTab.this.channels.getActiveChannel().switchRecordSet(AkkuMasterChannelTab.this.recordSetKey);
															}
															// update discharge / charge current display
															int actualCurrent = ((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_CURRENT)).intValue();
															if (processName.trim().equalsIgnoreCase(Messages.getString(MessageIds.OSDE_MSGT1126).trim())) {
																AkkuMasterChannelTab.this.parent.addTotalChargeCurrent(actualCurrent);
																setChargeCurrentAdded(true);
															}
															else if (processName.trim().equalsIgnoreCase(Messages.getString(MessageIds.OSDE_MSGT1127).trim())) {
																AkkuMasterChannelTab.this.parent.addTotalDischargeCurrent(actualCurrent);
																setDischargeCurrentAdded(true);
															}
															if (processName.trim().equalsIgnoreCase(Messages.getString(MessageIds.OSDE_MSGT1126).trim()) && isDischargeCurrentAdded()) { 
																AkkuMasterChannelTab.this.parent.subtractTotalChargeCurrent(actualCurrent);
															}
															else if (processName.trim().equalsIgnoreCase(Messages.getString(MessageIds.OSDE_MSGT1127).trim()) && isChargeCurrentAdded()) {
																AkkuMasterChannelTab.this.parent.subtractTotalDischargeCurrent(actualCurrent);
															}
														}
														else {
															log.log(Level.FINE, "re-using " + AkkuMasterChannelTab.this.recordSetKey); //$NON-NLS-1$
														}
														setTimeStamp();

														// prepare the data for adding to record set
														// build the point array according curves from record set
														int[] points = new int[AkkuMasterChannelTab.this.recordSet.size()];

														points[0] = new Integer((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_VOLTAGE)).intValue(); //Spannung 	[mV]
														points[1] = new Integer((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_CURRENT)).intValue(); //Strom 			[mA]
														// display adaption * 1000  -  / 1000
														points[2] = new Integer((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_CAPACITY)).intValue() * 1000; //Kapazität	[mAh] 
														points[3] = new Integer((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_POWER)).intValue() / 1000; //Leistung		[mW]
														points[4] = new Integer((Integer) this.data.get(AkkuMasterC4SerialPort.PROCESS_ENERGIE)).intValue() / 1000; //Energie		[mWh]
														log.log(Level.FINE, points[0] + " mV; " + points[1] + " mA; " + points[2] + " mAh; " + points[3] + " mW; " + points[4] + " mWh"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

														AkkuMasterChannelTab.this.isGatheredRecordSetVisible = AkkuMasterChannelTab.this.recordSetKey.equals(AkkuMasterChannelTab.this.channels.getActiveChannel().getActiveRecordSet().getName());						
														if (AkkuMasterChannelTab.this.isGatheredRecordSetVisible) {
															AkkuMasterChannelTab.this.recordSet.addPoints(points, false); // updates data table and digital windows
															AkkuMasterChannelTab.this.application.updateGraphicsWindow();
															AkkuMasterChannelTab.this.application.updateStatisticsData();
															AkkuMasterChannelTab.this.application.updateDataTable(AkkuMasterChannelTab.this.recordSetKey);
															AkkuMasterChannelTab.this.application.updateDigitalWindowChilds();
															AkkuMasterChannelTab.this.application.updateAnalogWindowChilds();
														}
													}
													else {
														// enable switching records sets
														if (0 == (setRetryCounter(getRetryCounter() - 1))) {
															stopTimer();
															log.log(Level.FINE, "Timer stopped AkkuMaster inactiv"); //$NON-NLS-1$
															setRetryCounter(3);
														}
													}
												}
												else { // some error state
													log.log(Level.FINE, "canceling timer due to error"); //$NON-NLS-1$
													if (!isCaptureOnly()) try {
														AkkuMasterChannelTab.this.serialPort.stop(AkkuMasterChannelTab.this.channelSig);
													}
													catch (IOException e) {
														log.log(Level.SEVERE, e.getMessage(), e);
													}
													setCollectData(false);
													stopTimer();
													AkkuMasterChannelTab.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0027));
												}
											}
											catch (DataInconsitsentException e) {
												// exception is logged where it is thrown first 
												log.log(Level.SEVERE, e.getMessage(), e);
												setCollectData(false);
												stopTimer();
												if (!AkkuMasterChannelTab.this.parent.isDisposed()) AkkuMasterChannelTab.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0028, new Object[] {e.getClass().getSimpleName(), e.getMessage()} ));
											}
											catch (Exception e) {
												// exception is logged where it is thrown first 
												log.log(Level.SEVERE, e.getMessage(), e);
												setCollectData(false);
												stopTimer();
												if (!AkkuMasterChannelTab.this.parent.isDisposed()) AkkuMasterChannelTab.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0029, new Object[] {e.getClass().getSimpleName(), e.getMessage()} ));
											}
										}
									});
									getTimer().scheduleAtFixedRate(getTimerTask(), delay, period);

								}
								catch (Exception e1) {
									setStopButtonEnabled(false);
									setStartDataGatheringSelection(true);
									AkkuMasterChannelTab.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0026));
								}
							}
						}

					});
				}
				{
					this.stopDataGatheringButton = new Button(this.channelComposite, SWT.PUSH | SWT.CENTER);
					this.stopDataGatheringButton.setBounds(225, 360, 190, 28);
					this.stopDataGatheringButton.setText(Messages.getString(osde.messages.MessageIds.OSDE_MSGT0278));
					this.stopDataGatheringButton.setEnabled(false);
					this.stopDataGatheringButton.setFont(SWTResourceManager.getFont(this.captureOnlyButton, SWT.BOLD));
					this.stopDataGatheringButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "stopAuzeichnungButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							updateDialogAfterStop();
							if (!isCaptureOnly()) try {
								AkkuMasterChannelTab.this.serialPort.stop(AkkuMasterChannelTab.this.channelSig);
							}
							catch (Exception e) {
								e.printStackTrace();
							}
							stopTimer();
							// hope this is the right record set
							AkkuMasterChannelTab.this.recordSet = AkkuMasterChannelTab.this.channels.getActiveChannel().get(AkkuMasterChannelTab.this.recordSetKey);
							if (AkkuMasterChannelTab.this.recordSet != null) AkkuMasterChannelTab.this.recordSet.setTableDisplayable(true); // enable table display after calculation
							AkkuMasterChannelTab.this.application.updateStatisticsData();
							AkkuMasterChannelTab.this.application.updateDataTable(AkkuMasterChannelTab.this.recordSet.getName());
						}
					});
				}
			} // end channel composite
		}
	}

	/**
	 * @throws IOException
	 */
	void updateAdjustedValues() throws Exception {
		// update channel tab with values red from device
		if (!this.serialPort.isConnected()) this.serialPort.open();
		String[] configuration = this.serialPort.getConfiguration(this.channelSig);
		if (log.isLoggable(Level.FINER)) this.serialPort.print(configuration);
		if (!configuration[0].equals("0")) { // AkkuMaster somehow active			 //$NON-NLS-1$
			this.programValue = new Integer(configuration[2].split(" ")[0]).intValue() - 1; //$NON-NLS-1$
			this.program.setText(this.aProgramm[this.programValue]);

			this.akkuTypeValue = new Integer(configuration[3].split(" ")[0]).intValue(); //$NON-NLS-1$
			this.akkuType.setText(this.aAkkuTyp[this.akkuTypeValue]);

			this.countCellsValue = new Integer(configuration[4].split(" ")[0]).intValue() - 1; //$NON-NLS-1$
			this.countCells.select(this.countCellsValue);

			this.capacityMilliAhValue = configuration[5].split(" ")[0]; //$NON-NLS-1$
			this.capacityMilliAh.setText(this.capacityMilliAhValue);

			this.chargeCurrentValue = configuration[7].split(" ")[0]; //$NON-NLS-1$
			this.chargeCurrent.setText(this.chargeCurrentValue);

			this.dischargeCurrentValue = configuration[6].split(" ")[0]; //$NON-NLS-1$
			this.dischargeCurrent.setText(this.dischargeCurrentValue);

			String[] adjustments = this.serialPort.getAdjustedValues(this.channelSig);
			this.memoryNumberValue = new Integer(adjustments[0].split(" ")[0]).intValue(); //$NON-NLS-1$
			this.memoryNumberCombo.select(this.memoryNumberValue);
			if (log.isLoggable(Level.FINER)) this.serialPort.print(adjustments);
		}
	}

	/**
	 * stop the timer task thread, this tops data capturing
	 */
	public void stopTimer() {
		if (this.timerTask != null) this.timerTask.cancel();
		if (this.timer != null) {
			this.timer.cancel();
			this.timer.purge();
		}

		this.isCollectData = false;
		this.isCollectDataStopped = true;

		if (Thread.currentThread().getId() == AkkuMasterChannelTab.this.application.getThreadId()) {
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
	void updateDialogAfterStop() {
		this.isDataGatheringEnabled = false;
		this.isStopButtonEnabled = false;
		this.isCaptureOnly = false;
		this.isDefinedProgram = false;
		this.isMemorySelectionChanged = false;
		this.startDataGatheringButton.setEnabled(this.isDataGatheringEnabled);
		this.stopDataGatheringButton.setEnabled(this.isStopButtonEnabled);

		this.captureOnlyButton.setSelection(this.isCaptureOnly);
		this.programmButton.setSelection(this.isDefinedProgram);
	}

	public boolean isDataColletionActive() {
		return isCollectData() || !isCollectDataStopped();
	}

	/**
	 * @return the isCollectData
	 */
	public boolean isCollectData() {
		return this.isCollectData;
	}

	/**
	 * @return the isCollectDataStopped
	 */
	public boolean isCollectDataStopped() {
		return this.isCollectDataStopped;
	}

	/**
	 * update the StartDataGatheringButton with the actual value of isDataGatheringEnabled
	 */
	void updateStartDataGatheringButton() {
		this.startDataGatheringButton.setEnabled(this.isDataGatheringEnabled);
	}

	/**
	 * update the StopDataGatheringButton with the actual value of isDataGatheringEnabled
	 */
	void updateStopDataGatheringButton() {
		this.stopDataGatheringButton.setEnabled(this.isStopButtonEnabled);
	}

	/**
	 * update the CaptureOnlyButton with the actual value of isCaptureOnly
	 */
	void updateCaptureOnlyButton() {
		this.captureOnlyButton.setSelection(this.isCaptureOnly);
	}

	/**
	 * 
	 */
	void updateProgramButton() {
		this.programmButton.setSelection(this.isDefinedProgram);
	}

	/**
	 * @param enabled the isCaptureOnly to set
	 */
	public void setCaptureOnly(boolean enabled) {
		this.isCaptureOnly = enabled;
	}

	/**
	 * @param enabled the isDefinedProgram to set
	 */
	public void setDefinedProgram(boolean enabled) {
		this.isDefinedProgram = enabled;
	}

	/**
	 * @param enabled the isDataGatheringEnabled to set
	 */
	public void setDataGatheringEnabled(boolean enabled) {
		this.isDataGatheringEnabled = enabled;
	}

	/**
	 * @param enabled the isStopButtonEnabled to set
	 */
	public void setStopButtonEnabled(boolean enabled) {
		this.isStopButtonEnabled = enabled;
	}

	/**
	 * @return
	 */
	boolean getCaptureOnlyButtonSelection() {
		return this.captureOnlyButton.getSelection();
	}

	/**
	 * @return
	 */
	boolean getProgramButtonSelection() {
		return this.programmButton.getSelection();
	}

	/**
	 * 
	 */
	void updateCapacityMilliAhText() {
		this.capacityMilliAh.setText(this.capacityMilliAhValue);
	}

	/**
	 * 
	 */
	void updateCountCellSelection() {
		this.countCells.select(this.countCellsValue);
	}

	/**
	 * 
	 */
	void updateAkkuType() {
		this.akkuType.setText(this.aAkkuTyp[this.akkuTypeValue]);
	}

	/**
	 * 
	 */
	void updateCapacityMilliAhValue() {
		this.capacityMilliAhValue = this.capacityMilliAh.getText();
	}

	/**
	 * 
	 */
	void updateCellCountValue() {
		this.countCellsValue = this.countCells.getSelectionIndex();
	}

	/**
	 * 
	 */
	void updateAkkuTypeValue() {
		this.akkuTypeValue = this.akkuType.getSelectionIndex();
	}

	/**
	 * 
	 */
	void updateProgramText() {
		this.program.setText(this.aProgramm[this.programValue]);
	}

	/**
	 * 
	 */
	void updateChargeCurrentText() {
		this.chargeCurrent.setText(this.chargeCurrentValue);
	}

	/**
	 * 
	 */
	void updateDichargeCurrentText() {
		this.dischargeCurrent.setText(this.dischargeCurrentValue);
	}

	/**
	 * 
	 */
	void updateMemoryNumberSelection() {
		this.memoryNumberCombo.select(this.memoryNumberValue);
	}

	/**
	 * 
	 */
	void updateProgramSelectionValue() {
		this.programValue = this.program.getSelectionIndex() + 1;
	}

	/**
	 * 
	 */
	void updateChargeCurrentValue() {
		this.chargeCurrentValue = this.chargeCurrent.getText();
	}

	/**
	 * 
	 */
	void updateDischargeCurrentValue() {
		this.dischargeCurrentValue = this.dischargeCurrent.getText();
	}

	/**
	 * 
	 */
	void updateMemoryNumberValue() {
		this.memoryNumberValue = this.memoryNumberCombo.getSelectionIndex();
		this.memoryNumberCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_CYAN));
	}

	/**
	 * @param enabled the isMemorySelectionChanged to set
	 */
	void setMemorySelectionChanged(boolean enabled) {
		this.isMemorySelectionChanged = enabled;
	}

	/**
	 * @param enabled the isCollectData to set
	 */
	void setCollectData(boolean enabled) {
		this.isCollectData = enabled;
	}

	/**
	 * @return the isCaptureOnly
	 */
	boolean isCaptureOnly() {
		return this.isCaptureOnly;
	}

	/**
	 * @return
	 */
	int getProgramNumber() {
		return new Integer(this.program.getText().split(" ")[0]).intValue(); //$NON-NLS-1$
	}

	/**
	 * @return
	 */
	int getAkkuType() {
		return new Integer(this.akkuType.getText().split(" ")[0]).intValue(); //$NON-NLS-1$
	}

	/**
	 * @return
	 */
	int getCellCount() {
		return new Integer(this.countCells.getText().split(" ")[0]).intValue(); //$NON-NLS-1$
	}

	/**
	 * @return
	 */
	int getAkkuCapacity() {
		return new Integer(this.capacityMilliAh.getText()).intValue();
	}

	/**
	 * @return
	 */
	int getDischargeCurrent() {
		return new Integer(this.dischargeCurrent.getText()).intValue();
	}

	/**
	 * @return
	 */
	int getChargeCurrent() {
		return new Integer(this.chargeCurrent.getText()).intValue();
	}

	/**
	 * @return the isMemorySelectionChanged
	 */
	boolean isMemorySelectionChanged() {
		return this.isMemorySelectionChanged;
	}

	/**
	 * @return
	 */
	int getMemoryNumberSelectionIndex() {
		return this.memoryNumberCombo.getSelectionIndex();
	}

	/**
	 * @param enabled
	 */
	void setStartDataGatheringSelection(boolean enabled) {
		this.startDataGatheringButton.setSelection(enabled);
	}

	/**
	 * @param newTimer the timer to set
	 */
	public void setTimer(Timer newTimer) {
		this.timer = newTimer;
	}

	/**
	 * @return the timer
	 */
	public Timer getTimer() {
		return this.timer;
	}

	/**
	 * @param newTimerTask the timerTask to set
	 */
	void setTimerTask(TimerTask newTimerTask) {
		this.timerTask = newTimerTask;
	}

	/**
	 * set timeStamp using Date().getTime()
	 */
	public void setTimeStamp() {
		this.timeStamp = new Date().getTime();
	}

	/**
	 * @return the timeStamp
	 */
	public long getTimeStamp() {
		return this.timeStamp;
	}

	/**
	 * @param enabled the isCollectDataStopped to set
	 */
	void setCollectDataStopped(boolean enabled) {
		this.isCollectDataStopped = enabled;
	}

	/**
	 * @return the name
	 */
	String getName() {
		return this.name;
	}

	/**
	 * @return the timerTask
	 */
	public TimerTask getTimerTask() {
		return this.timerTask;
	}

	/**
	 * @param isAdded the isChargeCurrentAdded to set
	 */
	public void setChargeCurrentAdded(boolean isAdded) {
		this.isChargeCurrentAdded = isAdded;
	}

	/**
	 * @return the isChargeCurrentAdded
	 */
	public boolean isChargeCurrentAdded() {
		return this.isChargeCurrentAdded;
	}

	/**
	 * @param isAdded the isDischargeCurrentAdded to set
	 */
	public void setDischargeCurrentAdded(boolean isAdded) {
		this.isDischargeCurrentAdded = isAdded;
	}

	/**
	 * @return the isDischargeCurrentAdded
	 */
	public boolean isDischargeCurrentAdded() {
		return this.isDischargeCurrentAdded;
	}

	/**
	 * @param newRetryCounter the retryCounter to set
	 */
	public int setRetryCounter(int newRetryCounter) {
		return this.retryCounter = newRetryCounter;
	}

	/**
	 * @return the retryCounter
	 */
	public int getRetryCounter() {
		return this.retryCounter;
	}
}
