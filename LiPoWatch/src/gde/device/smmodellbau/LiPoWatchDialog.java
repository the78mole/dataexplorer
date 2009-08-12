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
package osde.device.smmodellbau;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import osde.OSDE;
import osde.config.Settings;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.DeviceDialog;
import osde.device.smmodellbau.lipowatch.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * LiPoWatch device dialog class
 * @author Winfried Brügmann
 */
public class LiPoWatchDialog extends DeviceDialog {
	static final String		DEVICE_NAME								= "LiPoWatch";
	static final Logger						log								= Logger.getLogger(LiPoWatchDialog.class.getName());
	
	public final static int				MAX_DATA_RECORDS					= 25920;
	public final static int				FLASH_SIZE								= 524288;
	public final static int				FLASH_POSITION_DATA_BEGIN	= 0x100;
	public final static int				MAX_DATA_VALUES						= FLASH_SIZE - 0x100;

	public final static String[]	TIME_INTERVAL							= { "   1/4 s  (->     5 h)", 
																															"   1/2 s  (->   10 h)", 
																															"      1 s   (->   20 h)",
																															"      2 s   (->   40 h)", 
																															"      5 s   (-> 100 h)", 
																															"    10 s   (->  200 h)"}; 
	public final static String[]	RX_AUTO_START_MS					= { " 1,1 ms", " 1,2 ms", " 1,3 ms", " 1,4 ms", " 1,5 ms", " 1,6 ms", " 1,7 ms", " 1,8 ms", " 1,9 ms", " Rx on" };	//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$

	//Shell dialogShell; // remove this later
	CTabFolder										mainTabFolder;
	CTabItem											configTabItem;
	Composite											configMainComosite;
	Group													statusGroup;
	CLabel												snLabel;
	CLabel												serialNumberText;
	CLabel												firmwareVersionLabel;
	CLabel												firmwareText;
	CLabel												memUsageUnit;
	CLabel												memoryUsageText;
	CLabel												memUsagePercent;
	
	Button readConfigButton, storeConfigButton, deleteMemoryButton, saveDisplayConfigButton;
	
	CCombo												measurementModusCombo;
	CLabel												measurementModusLabel;
	Group													measurementTypeGroup;
	Group													autoStartGroup;
	CCombo												impulseTriggerCombo;
	CCombo												timeTriggerCombo;
	Button												impulseTriggerButton;
	Button												timeTriggerButton;
	Button												voltageDropTriggerButton;

	Group													dataRateGroup;
	CCombo												timeIntervalCombo;

	Group impuleRegulationGroup;
	CLabel cellTypeLabel;
	CLabel voltageLimitLabel;
	CLabel regulationTypeLabel;
	CCombo cellTypeCombo;
	CCombo voltageLevelRegulationCombo;
	CCombo regulationTypeCombo;

	CTabItem displayTabItem;
	Composite displayComposite;
	Group voltageDisplayGroup;
	Button voltage1Button, voltage2Button, voltage3Button, voltage4Button, voltage5Button, voltage6Button, voltage7Button, voltage8Button, impulseButton;
	CLabel voltage1UnitLabel, voltage2UnitLabel, voltage3UnitLabel, voltage4UnitLabel, voltage5UnitLabel, voltage6UnitLabel, voltage7UnitLabel, voltage8UnitLabel, impulseUnitLabel;
	CLabel voltage1SymbolLabel, voltage2SymbolLabel, voltage3SymbolLabel, voltage4SymbolLabel, voltage5SymbolLabel, voltage6SymbolLabel, voltage7SymbolLabel, voltage8SymbolLabel, impulsSymbolLabel;
	Button voltage9Button, voltage10Button, voltage11Button, voltage12Button, voltage13Button, voltage14Button, voltage15Button, voltageTotalButton, temperatureButton;
	CLabel voltage9UnitLabel, voltage10UnitLabel, voltage11UnitLabel, voltage12UnitLabel, voltage13UnitLabel, voltage14UnitLabel, voltage15UnitLabel, voltageTotalUnitLabel, temperatureUnitLabel;
	CLabel voltage9SymbolLabel, voltage10SymbolLabel, voltage11SymbolLabel, voltage12SymbolLabel, voltage13SymbolLabel, voltage14SymbolLabel, voltage15SymbolLabel, voltageTotalSymbolLabel, temperatureSymbolLabel;

	CLabel a1OffsetLabel, a1FactorLabel;
	Text	a1OffsetText, a1FactorText;
	
	CTabItem dataTabItem;
	Composite dataMainComposite;
	Group dataReadGroup;
	Button readDataButton, stopReadDataButton;
	CLabel dataSetLabel, redDataSetLabel, actualDataSetNumberLabel, actualDataSetNumber, readDataErrorLabel, numberReadErrorLabel;
	ProgressBar readDataProgressBar;
	
	Group liveDataCaptureGroup, loggingGroup;
	Button startLiveGatherButton, stopLiveGatherButton, startLoggingButton, stopLoggingButton; 
	
	Button closeButton;
		
	LiPoWatchDataGatherer					gatherThread;
	LiPoWatchLiveGatherer					liveThread;
	String												serialNumber								= "";																																																														//$NON-NLS-1$
	String												lipoWatchVersion						= "";																																																														//$NON-NLS-1$
	int														memoryUsed									= 0;
	String												memoryUsedPercent						= "0";																																																														//$NON-NLS-1$
	int														timeIntervalPosition				= 0;
	int														measurementModus						= 0;
	boolean												isAutoStartVoltageDrop			= true;
	boolean												isAutStartRx								= false;
	boolean												isRxOn											= false;
	boolean												isAutoStartTime							= false;
	int														voltageLevelRegulationLimit	= 10;																																																														//3.0 V
	int														impulsReductionType					= 1;																																																															//soft
	int														cellType										= 0;																																																															//LiPo
	int														rxAutoStartValue						= 0;
	int														timeAutoStart_sec						= 0;
	double												offsetA1										= 0.0;
	double												factorA1										= 1.0;
	
	String												numberRedDataSetsText			= "0";																																																							//$NON-NLS-1$
	String												numberActualDataSetsText	= "0";																																																							//$NON-NLS-1$
	String												numberReadErrorText				= "0";																																																							//$NON-NLS-1$	

	final LiPoWatch								device;																							// get device specific things, get serial port, ...
	final LiPoWatchSerialPort			serialPort;																					// open/close port execute getData()....
	final OpenSerialDataExplorer	application;																				// interaction with application instance
	final Channels								channels;																						// interaction with channels, source of all records
	final Settings								settings;																						// application configuration settings
	
	/**
	* Auto-generated main method to display this 
	* osde.device.DeviceDialog inside a new Shell.
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			LiPoWatchDialog inst = new LiPoWatchDialog(shell, null);
			inst.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public LiPoWatchDialog(Shell parent, LiPoWatch useDevice) {
		super(parent);
		serialPort = useDevice.getSerialPort();
		device = useDevice;
		application = OpenSerialDataExplorer.getInstance();
		channels = Channels.getInstance();
		settings = Settings.getInstance();
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	@Override
	public void open() {
		try {
			shellAlpha = Settings.getInstance().getDialogAlphaValue();
			isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			log.log(Level.FINE, "dialogShell.isDisposed() " + ((dialogShell == null) ? "null" : dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (dialogShell == null || dialogShell.isDisposed()) {
				if (settings.isDeviceDialogsModal())
					dialogShell = new Shell(application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
				else if (settings.isDeviceDialogsOnTop())
					dialogShell = new Shell(application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
				else
					dialogShell = new Shell(application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(dialogShell);
				if (isAlphaEnabled) dialogShell.setAlpha(254);
				dialogShell.setText("LiPoWatch" + Messages.getString(osde.messages.MessageIds.OSDE_MSGT0273)); //$NON-NLS-1$
				dialogShell.setImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				dialogShell.setSize(550, 401);
				dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 175, 100));
				dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						log.log(Level.FINE, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (saveDisplayConfigButton.getEnabled()) {
							String msg = Messages.getString(MessageIds.OSDE_MSGI1500);
							if (application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								log.log(Level.FINE, "SWT.YES"); //$NON-NLS-1$
								device.storeDeviceProperties();
								setClosePossible(true);
							}
							// check threads before close
							if (gatherThread != null && gatherThread.isAlive()) gatherThread.interrupt();
							if (liveThread != null && liveThread.isTimerRunning) {
								liveThread.stopTimerThread();
							}
						}
					}
				});
				dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						log.log(Level.FINE, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						application.openHelpDialog("LiPoWatch", "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				// enable fade in/out alpha blending (do not fade-in on top)
//				dialogShell.addMouseTrackListener(new MouseTrackAdapter() {
//					public void mouseEnter(MouseEvent evt) {
//						log.log(Level.FINER, "dialogShell.mouseEnter, event=" + evt); //$NON-NLS-1$
//						fadeOutAplhaBlending(evt, getDialogShell().getClientArea(), 10, 10, 10, 15);
//					}
//
//					public void mouseHover(MouseEvent evt) {
//						log.log(Level.FINEST, "dialogShell.mouseHover, event=" + evt); //$NON-NLS-1$
//					}
//
//					public void mouseExit(MouseEvent evt) {
//						log.log(Level.FINER, "dialogShell.mouseExit, event=" + evt); //$NON-NLS-1$
//						fadeInAlpaBlending(evt, getDialogShell().getClientArea(), 10, 10, -10, 15);
//					}
//				});
				{
					mainTabFolder = new CTabFolder(dialogShell, SWT.NONE);
					{
						configTabItem = new CTabItem(mainTabFolder, SWT.NONE);
						configTabItem.setText("Configuration");
						{
							configMainComosite = new Composite(mainTabFolder, SWT.NONE);
							configTabItem.setControl(configMainComosite);
							configMainComosite.setLayout(null);
							configMainComosite.setToolTipText("F1 (Linux, Strg+F1) for help");
							configMainComosite.addPaintListener(new PaintListener() {		
								@Override
								public void paintControl(PaintEvent evt) {
									log.log(Level.FINEST, "configMainComosite.addPaintListener, event=" + evt); //$NON-NLS-1$
									snLabel.setText(serialNumber);
									firmwareVersionLabel.setText(lipoWatchVersion);
									memUsagePercent.setText(memoryUsedPercent);
									timeIntervalCombo.select(timeIntervalPosition);
									measurementModusCombo.select(measurementModus);
									voltageDropTriggerButton.setSelection(isAutoStartVoltageDrop);
									timeTriggerButton.setSelection(isAutoStartTime);
									impulseTriggerButton.setSelection(isAutStartRx);
									regulationTypeCombo.select(impulsReductionType);
									voltageLevelRegulationCombo.select(voltageLevelRegulationLimit);
									cellTypeCombo.select(cellType);
								}
							});
							{
								statusGroup = new Group(configMainComosite, SWT.NONE);
								statusGroup.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
								statusGroup.setLayout(null);
								statusGroup.setText("Status");
								statusGroup.setBounds(9, 4, 518, 45);
								{
									serialNumberText = new CLabel(statusGroup, SWT.NONE);
									serialNumberText.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									serialNumberText.setText("S/N:");
									serialNumberText.setBounds(12, 15, 40, 22);
									serialNumberText.setToolTipText("serial number of connected LiPoWatch");
								}
								{
									snLabel = new CLabel(statusGroup, SWT.CENTER | SWT.EMBEDDED);
									snLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									snLabel.setBounds(52, 15, 88, 22);
									snLabel.setText(serialNumber);
								}
								{
									firmwareText = new CLabel(statusGroup, SWT.NONE);
									firmwareText.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									firmwareText.setText("Firmware:");
									firmwareText.setBounds(141, 15, 79, 22);
									firmwareText.setToolTipText("firmware level of connected LiPoWatch");
								}
								{
									firmwareVersionLabel = new CLabel(statusGroup, SWT.NONE);
									firmwareVersionLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									firmwareVersionLabel.setBounds(221, 15, 71, 22);
									firmwareVersionLabel.setText(lipoWatchVersion);
								}
								{
									memoryUsageText = new CLabel(statusGroup, SWT.NONE);
									memoryUsageText.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									memoryUsageText.setText("Speicherbelegung");
									memoryUsageText.setBounds(293, 15, 124, 22);
								}
								{
									memUsagePercent = new CLabel(statusGroup, SWT.RIGHT);
									memUsagePercent.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									memUsagePercent.setBounds(418, 15, 59, 22);
									memUsagePercent.setText(memoryUsedPercent);
								}
								{
									memUsageUnit = new CLabel(statusGroup, SWT.NONE);
									memUsageUnit.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									memUsageUnit.setText("[%]");
									memUsageUnit.setBounds(478, 15, 26, 22);
								}
							} // end status group

							{
								autoStartGroup = new Group(configMainComosite, SWT.NONE);
								autoStartGroup.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
								autoStartGroup.setText("Logging Autostart");
								autoStartGroup.setBounds(12, 141, 247, 107);
								{
									voltageDropTriggerButton = new Button(autoStartGroup, SWT.CHECK | SWT.RIGHT);
									voltageDropTriggerButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									voltageDropTriggerButton.setText("voltage drop trigger ");
									voltageDropTriggerButton.setToolTipText("startet die Aufnahme bei sinkender Spannungslage");
									voltageDropTriggerButton.setSelection(isAutoStartVoltageDrop);
									voltageDropTriggerButton.setBounds(16, 24, 129, 20);
									voltageDropTriggerButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "currentTriggerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											isAutoStartVoltageDrop = voltageDropTriggerButton.getSelection();
											if (isAutoStartVoltageDrop) {
												impulseTriggerButton.setSelection(isAutStartRx = false);
												timeTriggerButton.setSelection(isAutoStartTime = false);
											}
											storeConfigButton.setEnabled(true);
										}
									});
								}
								{
									timeTriggerButton = new Button(autoStartGroup, SWT.CHECK | SWT.RIGHT);
									timeTriggerButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									timeTriggerButton.setText("time trigger [sec] ");
									timeTriggerButton.setToolTipText("startet die Aufnahme nach der eingestellten Zeit");
									timeTriggerButton.setSelection(isAutStartRx);
									timeTriggerButton.setBounds(16, 52, 129, 20);
									timeTriggerButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "timeTriggerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											isAutoStartTime = timeTriggerButton.getSelection();
											if (isAutoStartTime) {
												impulseTriggerButton.setSelection(isAutStartRx = false);
												voltageDropTriggerButton.setSelection(isAutoStartVoltageDrop = false);
											}
											storeConfigButton.setEnabled(true);
										}
									});
								}
								{
									timeTriggerCombo = new CCombo(autoStartGroup, SWT.BORDER);
									timeTriggerCombo.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									timeTriggerCombo.setBounds(154, 50, 80, 20);
									timeTriggerCombo.setItems(new String[] { " 15", " 20", " 25", " 30", " 60", " 90" }); //$NON-NLS-1$
									timeTriggerCombo.select(3);
									timeTriggerCombo.setEditable(true);
									timeTriggerCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									timeTriggerCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "timeTriggerCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											timeAutoStart_sec = Integer.parseInt(timeTriggerCombo.getText().trim());
											storeConfigButton.setEnabled(true);
										}
									});
									timeTriggerCombo.addKeyListener(new KeyAdapter() {
										public void keyReleased(KeyEvent evt) {
											log.log(Level.FINEST, "timeTriggerCombo.keyReleased, event=" + evt);
											if (evt.keyCode == SWT.CR) {
												int value = Integer.parseInt(timeTriggerCombo.getText().trim());
												if (value < 15)
													timeTriggerCombo.setText(" " + (value = 15));
												else if (value > 90) timeTriggerCombo.setText(" " + (value = 90));
												
												int timeSelect = 0;
												for (; timeSelect < timeTriggerCombo.getItemCount(); ++timeSelect){
													if (value <= Integer.parseInt(timeTriggerCombo.getItems()[timeSelect].trim())) break;
												}
												timeTriggerCombo.select(timeSelect);
												storeConfigButton.setEnabled(true);
											}
										}
									});
								}
								{
									impulseTriggerButton = new Button(autoStartGroup, SWT.CHECK | SWT.RIGHT);
									impulseTriggerButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									impulseTriggerButton.setText("impulse trigger [ms] ");
									impulseTriggerButton.setToolTipText("startet die Aufnahme bei der eingestellten Impulslängen");
									impulseTriggerButton.setSelection(isAutoStartTime);
									impulseTriggerButton.setBounds(16, 78, 129, 20);
									impulseTriggerButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "impulseTriggerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											isAutStartRx = impulseTriggerButton.getSelection();
											if (isAutStartRx) {
												timeTriggerButton.setSelection(isAutoStartTime = false);
												voltageDropTriggerButton.setSelection(isAutoStartVoltageDrop = false);
											}
											storeConfigButton.setEnabled(true);
										}
									});
								}
								{
									impulseTriggerCombo = new CCombo(autoStartGroup, SWT.BORDER);
									impulseTriggerCombo.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									impulseTriggerCombo.setBounds(154, 76, 80, 20);
									impulseTriggerCombo.setEditable(false);
									impulseTriggerCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									impulseTriggerCombo.setItems(LiPoWatchDialog.RX_AUTO_START_MS);
									impulseTriggerCombo.select(4);
									impulseTriggerCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "rcTriggerCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											rxAutoStartValue = impulseTriggerCombo.getSelectionIndex() + 11;
											storeConfigButton.setEnabled(true);
										}
									});
								}
							} // end autoStartGroup
							{
								dataRateGroup = new Group(configMainComosite, SWT.NONE);
								dataRateGroup.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
								dataRateGroup.setText("Speicherrate");
								dataRateGroup.setBounds(12, 89, 247, 45);
								dataRateGroup.setToolTipText("adjust data rate used for data logging");
								dataRateGroup.addMouseTrackListener(mouseTrackerEnterFadeOut);
								{
									timeIntervalCombo = new CCombo(dataRateGroup, SWT.BORDER);
									timeIntervalCombo.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									timeIntervalCombo.setItems(TIME_INTERVAL);
									timeIntervalCombo.setBounds(56, 17, 156, 20);
									timeIntervalCombo.select(1);
									timeIntervalCombo.setEditable(false);
									timeIntervalCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									timeIntervalCombo.setToolTipText("adjust data rate used for data logging");
									timeIntervalCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "timeRateCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											timeIntervalPosition = timeIntervalCombo.getSelectionIndex();
											storeConfigButton.setEnabled(true);
										}
									});
								}
							} // end dataRateGroup
							{
								readConfigButton = new Button(configMainComosite, SWT.PUSH | SWT.CENTER);
								readConfigButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
								readConfigButton.setText("Read Configuration");
								readConfigButton.setBounds(130, 55, 273, 30);
								readConfigButton.setToolTipText("Liest die Konfiguration aus dem LiPoWatch");
								readConfigButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "readConfigButton.widgetSelected, event=" + evt);
										try {
											updateConfigurationValues(LiPoWatchDialog.this.serialPort.readConfiguration());
										}
										catch (Exception e) {
											LiPoWatchDialog.this.application.openMessageDialog(LiPoWatchDialog.this.getDialogShell(), Messages.getString(osde.messages.MessageIds.OSDE_MSGE0029, new Object[] {
													e.getClass().getSimpleName(), e.getMessage() }));
										}
									}
								});
							}
							{
								storeConfigButton = new Button(configMainComosite, SWT.PUSH | SWT.CENTER);
								storeConfigButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
								storeConfigButton.setText("Save Configuration");
								storeConfigButton.setBounds(130, 254, 273, 30);
								storeConfigButton.setToolTipText("speicher die eingestellte Konfiguration ");
								storeConfigButton.setEnabled(false);
								storeConfigButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "storeConfigButton.widgetSelected, event=" + evt);
										try {
											if (serialPort.setConfiguration(buildUpdateBuffer())) {
												updateTimeStep_ms(timeIntervalPosition);
												storeConfigButton.setEnabled(false);
											}
											else {
												storeConfigButton.setEnabled(true);
											}
										}
										catch (Exception e) {
											application.openMessageDialog(getDialogShell(), e.getMessage());
										}
									}
								});
							}
							{
								impuleRegulationGroup = new Group(configMainComosite, SWT.NONE);
								impuleRegulationGroup.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
								impuleRegulationGroup.setLayout(null);
								impuleRegulationGroup.setText("Impuls regulation");
								impuleRegulationGroup.setBounds(278, 141, 248, 107);
								{
									regulationTypeLabel = new CLabel(impuleRegulationGroup, SWT.NONE);
									regulationTypeLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									regulationTypeLabel.setText("type");
									regulationTypeLabel.setBounds(14, 21, 79, 20);
									regulationTypeLabel.setToolTipText("type of slow down regulation at adjusted trigger level");
								}
								{
									voltageLimitLabel = new CLabel(impuleRegulationGroup, SWT.NONE);
									voltageLimitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									voltageLimitLabel.setText("voltage limit");
									voltageLimitLabel.setBounds(14, 49, 79, 20);
									voltageLimitLabel.setToolTipText("voltage limit when regulation begins");
								}
								{
									cellTypeLabel = new CLabel(impuleRegulationGroup, SWT.NONE);
									cellTypeLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									cellTypeLabel.setText("cell type");
									cellTypeLabel.setBounds(14, 75, 79, 20);
								}
								{
									regulationTypeCombo = new CCombo(impuleRegulationGroup, SWT.BORDER | SWT.CENTER);
									regulationTypeCombo.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									regulationTypeCombo.setItems(new String[] { "  off", "  soft", "  hard" });
									regulationTypeCombo.setBounds(130, 23, 100, 20);
									regulationTypeCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "regulationTypeCombo.widgetSelected, event=" + evt);
											impulsReductionType = regulationTypeCombo.getSelectionIndex();
											storeConfigButton.setEnabled(true);
										}
									});
								}
								{
									voltageLevelRegulationCombo = new CCombo(impuleRegulationGroup, SWT.BORDER);
									voltageLevelRegulationCombo.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									voltageLevelRegulationCombo.setItems(new String[] { " 2.0 V", " 2.1 V", " 2.2 V", " 2.3 V", " 2.4 V", " 2.5 V", " 2.6 V", " 2.7 V", " 2.8 V", " 2.9 V", " 3.0 V", " 3.1 V", " 3.2 V",
											" 3.3 V", " 3.4 V", " 3.5 V", " 3.6 V", " 3.7 V" });
									voltageLevelRegulationCombo.setBounds(130, 51, 100, 20);
									voltageLevelRegulationCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "voltageLimitCombo.widgetSelected, event=" + evt);
											voltageLevelRegulationLimit = voltageLevelRegulationCombo.getSelectionIndex();
											storeConfigButton.setEnabled(true);
										}
									});
								}
								{
									cellTypeCombo = new CCombo(impuleRegulationGroup, SWT.BORDER);
									cellTypeCombo.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									cellTypeCombo.setBounds(130, 77, 100, 20);
									cellTypeCombo.setItems(new java.lang.String[] { "  LiPo", "  LiFe" });
									cellTypeCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "cellTypeCombo.widgetSelected, event=" + evt);
											cellType = cellTypeCombo.getSelectionIndex();
											storeConfigButton.setEnabled(true);
										}
									});
								}
							} // end impuleRegulationGroup
							{
								measurementTypeGroup = new Group(configMainComosite, SWT.NONE);
								measurementTypeGroup.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
								measurementTypeGroup.setLayout(null);
								measurementTypeGroup.setText("Measurement");
								measurementTypeGroup.setBounds(278, 89, 247, 45);
								measurementTypeGroup.setToolTipText("define if voltage will be measured absolute (4, 8 12..) or relative  (4, 4, 4)");
								{
									measurementModusLabel = new CLabel(measurementTypeGroup, SWT.NONE);
									measurementModusLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									measurementModusLabel.setText("modus");
									measurementModusLabel.setBounds(14, 18, 72, 20);
									measurementModusLabel.setToolTipText("define if voltage will be measured absolute (4, 8 12..) or relative  (4, 4, 4)");
								}
								{
									measurementModusCombo = new CCombo(measurementTypeGroup, SWT.BORDER);
									measurementModusCombo.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									measurementModusCombo.setBounds(93, 18, 137, 20);
									measurementModusCombo.setItems(new java.lang.String[] { " relative", " absolute" });
									measurementModusCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "measurementTypeCombo.widgetSelected, event=" + evt);
											measurementModus = measurementModusCombo.getSelectionIndex();
											storeConfigButton.setEnabled(true);
										}
									});
								}
							}
						}
						{
							displayTabItem = new CTabItem(mainTabFolder, SWT.NONE);
							displayTabItem.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
							displayTabItem.setText("Graphics");
							displayTabItem.setToolTipText("configure what to be displayed");
							{
								displayComposite = new Composite(mainTabFolder, SWT.NONE);
								displayComposite.setLayout(null);
								displayTabItem.setControl(displayComposite);
								{
									voltageDisplayGroup = new Group(displayComposite, SWT.NONE);
									voltageDisplayGroup.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									voltageDisplayGroup.setText("Cell voltage");
									voltageDisplayGroup.setLayout(null);
									voltageDisplayGroup.setBounds(17, 0, 506, 221);
									voltageDisplayGroup.setToolTipText("select cell voltage to be displayed\nmeasurements with zero data are greyed");
									// right side
									{
										voltage1Button = new Button(voltageDisplayGroup, SWT.CHECK | SWT.LEFT);
										voltage1Button.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage1Button.setText("voltage 1");
										voltage1Button.setBounds(50, 19, 100, 22);
										voltage1Button.addSelectionListener(new SelectionAdapter() {
											public void widgetSelected(SelectionEvent evt) {
												log.log(Level.FINEST, "voltage1Button.widgetSelected, event=" + evt);
												//TODO add your code for voltage1Button.widgetSelected
											}
										});
									}
									{
										voltage1SymbolLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage1SymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage1SymbolLabel.setText("U1");
										voltage1SymbolLabel.setBounds(150, 19, 30, 22);
									}
									{
										voltage1UnitLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage1UnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage1UnitLabel.setText("[V]");
										voltage1UnitLabel.setBounds(180, 18, 30, 22);
									}
									{
										voltage2Button = new Button(voltageDisplayGroup, SWT.CHECK | SWT.LEFT);
										voltage2Button.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage2Button.setText("voltage 2");
										voltage2Button.setBounds(50, 44, 100, 22);
										voltage2Button.setToolTipText("cell voltage");
										voltage2Button.addKeyListener(new KeyAdapter() {
											public void keyReleased(KeyEvent evt) {
												log.log(Level.FINEST, "voltage2Button.keyReleased, event=" + evt);
												//TODO add your code for voltage2Button.keyReleased
											}
										});
									}
									{
										voltage2SymbolLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage2SymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage2SymbolLabel.setText("U2");
										voltage2SymbolLabel.setBounds(150, 44, 30, 22);
									}
									{
										voltage2UnitLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage2UnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage2UnitLabel.setText("[V]");
										voltage2UnitLabel.setBounds(180, 43, 30, 22);
									}
									{
										voltage3Button = new Button(voltageDisplayGroup, SWT.CHECK | SWT.LEFT);
										voltage3Button.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage3Button.setText("voltage 3");
										voltage3Button.setBounds(50, 69, 100, 22);
										voltage3Button.setToolTipText("cell voltage");
										voltage3Button.addKeyListener(new KeyAdapter() {
											public void keyReleased(KeyEvent evt) {
												log.log(Level.FINEST, "voltage3Button.keyReleased, event=" + evt);
												//TODO add your code for voltage3Button.keyReleased
											}
										});
									}
									{
										voltage3SymbolLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage3SymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage3SymbolLabel.setText("U3");
										voltage3SymbolLabel.setBounds(150, 69, 30, 22);
									}
									{
										voltage3UnitLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage3UnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage3UnitLabel.setText("[V]");
										voltage3UnitLabel.setBounds(180, 68, 30, 22);
									}
									{
										voltage4Button = new Button(voltageDisplayGroup, SWT.CHECK | SWT.LEFT);
										voltage4Button.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage4Button.setText("voltage 4");
										voltage4Button.setBounds(50, 94, 100, 22);
										voltage4Button.setToolTipText("cell voltage");
										voltage4Button.addKeyListener(new KeyAdapter() {
											public void keyReleased(KeyEvent evt) {
												log.log(Level.FINEST, "voltage4Button.keyReleased, event=" + evt);
												//TODO add your code for voltage4Button.keyReleased
											}
										});
									}
									{
										voltage4SymbolLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage4SymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage4SymbolLabel.setText("U4");
										voltage4SymbolLabel.setBounds(150, 94, 30, 22);
									}
									{
										voltage4UnitLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage4UnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage4UnitLabel.setText("[V]");
										voltage4UnitLabel.setBounds(180, 93, 30, 22);
									}
									{
										voltage5Button = new Button(voltageDisplayGroup, SWT.CHECK | SWT.LEFT);
										voltage5Button.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage5Button.setText("voltage 5");
										voltage5Button.setBounds(50, 119, 100, 22);
										voltage5Button.setToolTipText("cell voltage");
										voltage5Button.addKeyListener(new KeyAdapter() {
											public void keyReleased(KeyEvent evt) {
												log.log(Level.FINEST, "voltage5Button.keyReleased, event=" + evt);
												//TODO add your code for voltage5Button.keyReleased
											}
										});
									}
									{
										voltage5SymbolLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage5SymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage5SymbolLabel.setText("U5");
										voltage5SymbolLabel.setBounds(150, 119, 30, 22);
									}
									{
										voltage5UnitLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage5UnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage5UnitLabel.setText("[V]");
										voltage5UnitLabel.setBounds(180, 118, 30, 22);
									}
									{
										voltage6Button = new Button(voltageDisplayGroup, SWT.CHECK | SWT.LEFT);
										voltage6Button.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage6Button.setText("voltage 6");
										voltage6Button.setBounds(50, 144, 100, 22);
										voltage6Button.setToolTipText("cell voltage");
										voltage6Button.addKeyListener(new KeyAdapter() {
											public void keyReleased(KeyEvent evt) {
												log.log(Level.FINEST, "voltage6Button.keyReleased, event=" + evt);
												//TODO add your code for voltage6Button.keyReleased
											}
										});
									}
									{
										voltage6SymbolLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage6SymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage6SymbolLabel.setText("U6");
										voltage6SymbolLabel.setBounds(150, 144, 30, 22);
									}
									{
										voltage6UnitLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage6UnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage6UnitLabel.setText("[V]");
										voltage6UnitLabel.setBounds(180, 143, 30, 22);
									}
									{
										voltage7Button = new Button(voltageDisplayGroup, SWT.CHECK | SWT.LEFT);
										voltage7Button.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage7Button.setText("voltage 7");
										voltage7Button.setBounds(50, 169, 100, 22);
										voltage7Button.setToolTipText("cell voltage");
										voltage7Button.addKeyListener(new KeyAdapter() {
											public void keyReleased(KeyEvent evt) {
												log.log(Level.FINEST, "voltage7Button.keyReleased, event=" + evt);
												//TODO add your code for voltage7Button.keyReleased
											}
										});
									}
									{
										voltage7SymbolLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage7SymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage7SymbolLabel.setText("U7");
										voltage7SymbolLabel.setBounds(150, 169, 30, 22);
									}
									{
										voltage7UnitLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage7UnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage7UnitLabel.setText("[V]");
										voltage7UnitLabel.setBounds(180, 168, 30, 22);
									}
									{
										voltage8Button = new Button(voltageDisplayGroup, SWT.CHECK | SWT.LEFT);
										voltage8Button.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage8Button.setText("voltage 8");
										voltage8Button.setBounds(50, 194, 100, 22);
										voltage8Button.setToolTipText("cell voltage");
										voltage8Button.addKeyListener(new KeyAdapter() {
											public void keyReleased(KeyEvent evt) {
												log.log(Level.FINEST, "voltage8Button.keyReleased, event=" + evt);
												//TODO add your code for voltage8Button.keyReleased
											}
										});
									}
									{
										voltage8SymbolLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage8SymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage8SymbolLabel.setText("U8");
										voltage8SymbolLabel.setBounds(150, 194, 30, 22);
									}
									{
										voltage8UnitLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage8UnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage8UnitLabel.setText("[V]");
										voltage8UnitLabel.setBounds(180, 193, 29, 22);
									}
									{
										voltage9Button = new Button(voltageDisplayGroup, SWT.CHECK | SWT.LEFT);
										voltage9Button.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage9Button.setText("voltage 9");
										voltage9Button.setBounds(300, 19, 100, 22);
										voltage9Button.setToolTipText("cell voltage");
										voltage9Button.addKeyListener(new KeyAdapter() {
											public void keyReleased(KeyEvent evt) {
												log.log(Level.FINEST, "voltage9Button.keyReleased, event=" + evt);
												//TODO add your code for voltage9Button.keyReleased
											}
										});
									}
									{
										voltage9SymbolLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage9SymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage9SymbolLabel.setText("U9");
										voltage9SymbolLabel.setBounds(400, 19, 30, 22);
									}
									{
										voltage9UnitLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage9UnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage9UnitLabel.setText("[V]");
										voltage9UnitLabel.setBounds(430, 18, 30, 22);
									}
									{
										voltage10Button = new Button(voltageDisplayGroup, SWT.CHECK | SWT.LEFT);
										voltage10Button.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage10Button.setText("voltage 10");
										voltage10Button.setBounds(300, 44, 100, 22);
										voltage10Button.setToolTipText("cell voltage");
										voltage10Button.addKeyListener(new KeyAdapter() {
											public void keyReleased(KeyEvent evt) {
												log.log(Level.FINEST, "voltage10Button.keyReleased, event=" + evt);
												//TODO add your code for voltage10Button.keyReleased
											}
										});
									}
									{
										voltage10SymbolLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage10SymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage10SymbolLabel.setText("U10");
										voltage10SymbolLabel.setBounds(400, 44, 30, 22);
									}
									{
										voltage10UnitLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage10UnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage10UnitLabel.setText("[V]");
										voltage10UnitLabel.setBounds(430, 43, 30, 22);
									}
									{
										voltage11Button = new Button(voltageDisplayGroup, SWT.CHECK | SWT.LEFT);
										voltage11Button.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage11Button.setText("voltage 11");
										voltage11Button.setBounds(300, 69, 100, 22);
										voltage11Button.setToolTipText("cell voltage");
										voltage11Button.addKeyListener(new KeyAdapter() {
											public void keyReleased(KeyEvent evt) {
												log.log(Level.FINEST, "voltage11Button.keyReleased, event=" + evt);
												//TODO add your code for voltage11Button.keyReleased
											}
										});
									}
									{
										voltage11SymbolLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage11SymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage11SymbolLabel.setText("U11");
										voltage11SymbolLabel.setBounds(400, 69, 30, 22);
									}
									{
										voltage11UnitLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage11UnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage11UnitLabel.setText("[V]");
										voltage11UnitLabel.setBounds(430, 68, 30, 22);
									}
									{
										voltage12Button = new Button(voltageDisplayGroup, SWT.CHECK | SWT.LEFT);
										voltage12Button.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage12Button.setText("voltage 12");
										voltage12Button.setBounds(300, 94, 100, 22);
										voltage12Button.setToolTipText("cell voltage");
										voltage12Button.addKeyListener(new KeyAdapter() {
											public void keyReleased(KeyEvent evt) {
												log.log(Level.FINEST, "voltage12Button.keyReleased, event=" + evt);
												//TODO add your code for voltage12Button.keyReleased
											}
										});
									}
									{
										voltage12SymbolLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage12SymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage12SymbolLabel.setText("U12");
										voltage12SymbolLabel.setBounds(400, 94, 30, 22);
									}
									{
										voltage12UnitLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage12UnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage12UnitLabel.setText("[V]");
										voltage12UnitLabel.setBounds(430, 93, 30, 22);
									}
									{
										voltage13Button = new Button(voltageDisplayGroup, SWT.CHECK | SWT.LEFT);
										voltage13Button.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage13Button.setText("voltage 13");
										voltage13Button.setBounds(300, 119, 100, 22);
										voltage13Button.setToolTipText("cell voltage");
										voltage13Button.addKeyListener(new KeyAdapter() {
											public void keyReleased(KeyEvent evt) {
												log.log(Level.FINEST, "voltage13Button.keyReleased, event=" + evt);
												//TODO add your code for voltage13Button.keyReleased
											}
										});
									}
									{
										voltage13SymbolLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage13SymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage13SymbolLabel.setText("U13");
										voltage13SymbolLabel.setBounds(400, 119, 30, 22);
									}
									{
										voltage13UnitLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage13UnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage13UnitLabel.setText("[V]");
										voltage13UnitLabel.setBounds(430, 118, 30, 22);
									}
									{
										voltage14Button = new Button(voltageDisplayGroup, SWT.CHECK | SWT.LEFT);
										voltage14Button.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage14Button.setText("voltage 14");
										voltage14Button.setBounds(300, 144, 100, 22);
										voltage14Button.setToolTipText("cell voltage");
										voltage14Button.addKeyListener(new KeyAdapter() {
											public void keyReleased(KeyEvent evt) {
												log.log(Level.FINEST, "voltage14Button.keyReleased, event=" + evt);
												//TODO add your code for voltage14Button.keyReleased
											}
										});
									}
									{
										voltage14SymbolLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage14SymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage14SymbolLabel.setText("U14");
										voltage14SymbolLabel.setBounds(400, 144, 30, 22);
									}
									{
										voltage14UnitLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage14UnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage14UnitLabel.setText("[V]");
										voltage14UnitLabel.setBounds(430, 143, 30, 22);
									}
									{
										voltage15Button = new Button(voltageDisplayGroup, SWT.CHECK | SWT.LEFT);
										voltage15Button.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage15Button.setText("voltage 15");
										voltage15Button.setBounds(300, 169, 95, 22);
										voltage15Button.setToolTipText("cell voltage");
										voltage15Button.addKeyListener(new KeyAdapter() {
											public void keyReleased(KeyEvent evt) {
												log.log(Level.FINEST, "voltage15Button.keyReleased, event=" + evt);
												//TODO add your code for voltage15Button.keyReleased
											}
										});
									}
									{
										voltage15SymbolLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage15SymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage15SymbolLabel.setText("U15");
										voltage15SymbolLabel.setBounds(400, 169, 30, 22);
									}
									{
										voltage15UnitLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltage15UnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltage15UnitLabel.setText("[V]");
										voltage15UnitLabel.setBounds(430, 168, 30, 22);
									}
									{
										voltageTotalButton = new Button(voltageDisplayGroup, SWT.CHECK | SWT.LEFT);
										voltageTotalButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltageTotalButton.setText("voltage t");
										voltageTotalButton.setBounds(300, 194, 95, 22);
										voltageTotalButton.setToolTipText("total battery voltage");
										voltageTotalButton.addKeyListener(new KeyAdapter() {
											public void keyReleased(KeyEvent evt) {
												log.log(Level.FINEST, "voltageTotalButton.keyReleased, event=" + evt);
												//TODO add your code for voltageTotalButton.keyReleased
											}
										});
									}
									{
										voltageTotalSymbolLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltageTotalSymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltageTotalSymbolLabel.setText("Ut");
										voltageTotalSymbolLabel.setBounds(400, 194, 30, 22);
									}
									{
										voltageTotalUnitLabel = new CLabel(voltageDisplayGroup, SWT.NONE);
										voltageTotalUnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										voltageTotalUnitLabel.setText("[V]");
										voltageTotalUnitLabel.setBounds(430, 193, 30, 22);
									}
								}
								{
									impulseButton = new Button(displayComposite, SWT.CHECK | SWT.LEFT);
									impulseButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									impulseButton.setText(" Impulse");
									impulseButton.setBounds(24, 232, 100, 22);
									impulseButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "impulseButton.widgetSelected, event=" + evt);
											//TODO add your code for impulseButton.widgetSelected
										}
									});
								}
								{
									impulsSymbolLabel = new CLabel(displayComposite, SWT.NONE);
									impulsSymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									impulsSymbolLabel.setText("SI");
									impulsSymbolLabel.setBounds(130, 232, 24, 22);
								}
								{
									impulseUnitLabel = new CLabel(displayComposite, SWT.NONE);
									impulseUnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									impulseUnitLabel.setText("[us]");
									impulseUnitLabel.setBounds(154, 231, 29, 22);
								}
								{
									temperatureButton = new Button(displayComposite, SWT.CHECK | SWT.LEFT);
									temperatureButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									temperatureButton.setText("Temperature");
									temperatureButton.setBounds(24, 257, 100, 22);
									temperatureButton.setToolTipText("intern/extern temperature");
									temperatureButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "temperatureButton.widgetSelected, event=" + evt);
											//TODO add your code for temperatureButton.widgetSelected
										}
									});
								}
								{
									temperatureSymbolLabel = new CLabel(displayComposite, SWT.NONE);
									temperatureSymbolLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									temperatureSymbolLabel.setText("A1");
									temperatureSymbolLabel.setBounds(130, 257, 24, 22);
								}
								{
									temperatureUnitLabel = new CLabel(displayComposite, SWT.NONE);
									temperatureUnitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									temperatureUnitLabel.setText("[°C]");
									temperatureUnitLabel.setBounds(154, 256, 30, 22);
								}
								{
									a1OffsetLabel = new CLabel(displayComposite, SWT.LEFT);
									a1OffsetLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									a1OffsetLabel.setBounds(194, 238, 46, 20);
									a1OffsetLabel.setText("offset");
									a1OffsetLabel.setToolTipText("offset to adapt to sensor");
								}
								{
									a1FactorLabel = new CLabel(displayComposite, SWT.LEFT);
									a1FactorLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									a1FactorLabel.setBounds(244, 238, 48, 20);
									a1FactorLabel.setText("factor");
									a1FactorLabel.setToolTipText("factor to adapt to sensor");
								}
								{
									a1OffsetText = new Text(displayComposite, SWT.BORDER);
									a1OffsetText.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									a1OffsetText.setBounds(190, 258, 48, 20);
									a1OffsetText.addKeyListener(new KeyAdapter() {
										public void keyReleased(KeyEvent evt) {
											log.log(Level.FINEST, "a1Offset.keyReleased, event=" + evt); //$NON-NLS-1$
											try {
												offsetA1 = new Double(a1OffsetText.getText().trim().replace(',', '.'));
												if (evt.character == SWT.CR) checkUpdateAnalog();
											}
											catch (Exception e) {
												application.openMessageDialog(LiPoWatchDialog.this.getDialogShell(), Messages.getString(osde.messages.MessageIds.OSDE_MSGE0030, new Object[] { e.getClass().getSimpleName(),
														e.getMessage() }));
											}
										}
									});
								}
								{
									a1FactorText = new Text(displayComposite, SWT.BORDER);
									a1FactorText.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									a1FactorText.setBounds(240, 258, 48, 20);
									a1FactorText.addKeyListener(new KeyAdapter() {
										public void keyReleased(KeyEvent evt) {
											log.log(Level.FINEST, "a1Factor.keyReleased, event=" + evt); //$NON-NLS-1$
											saveDisplayConfigButton.setEnabled(true);
											try {
												factorA1 = new Double(a1FactorText.getText().trim().replace(',', '.'));
												if (evt.character == SWT.CR) checkUpdateAnalog();
											}
											catch (Exception e) {
												application.openMessageDialog(LiPoWatchDialog.this.getDialogShell(), Messages.getString(osde.messages.MessageIds.OSDE_MSGE0030, new Object[] { e.getClass().getSimpleName(),
														e.getMessage() }));
											}
										}
									});
								}
								{
									saveDisplayConfigButton = new Button(displayComposite, SWT.PUSH | SWT.CENTER);
									saveDisplayConfigButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									saveDisplayConfigButton.setText("save as default configuration");
									saveDisplayConfigButton.setToolTipText("save display configuration as default to device configuration file");
									saveDisplayConfigButton.setBounds(307, 245, 215, 30);
									saveDisplayConfigButton.setEnabled(false);
									saveDisplayConfigButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "saveConfigButton.widgetSelected, event=" + evt);
											//TODO add your code for saveConfigButton.widgetSelected
										}

										public void widgetDefaultSelected(SelectionEvent evt) {
											saveConfigButtonWidgetDefaultSelected(evt);
										}
									});
								}
							}
						}
						{
							dataTabItem = new CTabItem(mainTabFolder, SWT.NONE);
							//dataTabItem.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
							dataTabItem.setText("Data I/O");
							dataTabItem.setToolTipText("Gather data from device, start/stop (life) data logging");
							{
								dataMainComposite = new Composite(mainTabFolder, SWT.NONE);
								dataMainComposite.setLayout(null);
								dataTabItem.setControl(dataMainComposite);
								{
									dataReadGroup = new Group(dataMainComposite, SWT.NONE);
									dataReadGroup.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									dataReadGroup.setBounds(12, 12, 253, 266);
									dataReadGroup.setText("Daten auslesen");
									{
										readDataButton = new Button(dataReadGroup, SWT.PUSH | SWT.CENTER);
										readDataButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										readDataButton.setText("Start Daten auslesen");
										readDataButton.setBounds(11, 30, 230, 30);
										readDataButton.setEnabled(true);
										readDataButton.setToolTipText("Startet den Ausleseprozess");
										readDataButton.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												log.log(Level.FINEST, "readDataButton.widgetSelected, event=" + evt); //$NON-NLS-1$
												gatherThread = new LiPoWatchDataGatherer(application, device, serialPort);
												try {
													gatherThread.start();
												}
												catch (RuntimeException e) {
													log.log(Level.WARNING, e.getMessage(), e);
												}
												setClosePossible(false);

												readDataButton.setEnabled(false);
												stopReadDataButton.setEnabled(true);
												startLoggingButton.setEnabled(false);
												stopLoggingButton.setEnabled(false);
												startLiveGatherButton.setEnabled(false);
												deleteMemoryButton.setEnabled(false);
											}
										});
									}
									{
										dataSetLabel = new CLabel(dataReadGroup, SWT.RIGHT);
										dataSetLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										dataSetLabel.setBounds(13, 83, 164, 20);
										dataSetLabel.setText("red telegrams  :");
									}
									{
										redDataSetLabel = new CLabel(dataReadGroup, SWT.RIGHT);
										redDataSetLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										redDataSetLabel.setBounds(183, 83, 55, 20);
										redDataSetLabel.setText("0"); //$NON-NLS-1$
									}
									{
										actualDataSetNumberLabel = new CLabel(dataReadGroup, SWT.RIGHT);
										actualDataSetNumberLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										actualDataSetNumberLabel.setBounds(13, 111, 164, 20);
										actualDataSetNumberLabel.setText("current data set number  :");
									}
									{
										actualDataSetNumber = new CLabel(dataReadGroup, SWT.RIGHT);
										actualDataSetNumber.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										actualDataSetNumber.setBounds(183, 111, 55, 20);
										actualDataSetNumber.setText("0"); //$NON-NLS-1$
									}
									{
										readDataErrorLabel = new CLabel(dataReadGroup, SWT.RIGHT);
										readDataErrorLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										readDataErrorLabel.setBounds(13, 139, 164, 20);
										readDataErrorLabel.setText("data transmission errors  :");
									}
									{
										numberReadErrorLabel = new CLabel(dataReadGroup, SWT.RIGHT);
										numberReadErrorLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										numberReadErrorLabel.setBounds(183, 139, 55, 20);
										numberReadErrorLabel.setText("0"); //$NON-NLS-1$
									}
									{
										readDataProgressBar = new ProgressBar(dataReadGroup, SWT.NONE);
										readDataProgressBar.setBounds(15, 175, 226, 15);
										readDataProgressBar.setMinimum(0);
										readDataProgressBar.setMaximum(100);
									}
									{
										stopReadDataButton = new Button(dataReadGroup, SWT.PUSH | SWT.CENTER);
										stopReadDataButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										stopReadDataButton.setBounds(15, 212, 226, 30);
										stopReadDataButton.setText(Messages.getString(osde.messages.MessageIds.OSDE_MSGT0278));
										stopReadDataButton.setEnabled(false);
										stopReadDataButton.setToolTipText("Stoppt einen laufenden Ausleseprozess");
										stopReadDataButton.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												log.log(Level.FINE, "stopDataButton.widgetSelected, event=" + evt); //$NON-NLS-1$
												if (gatherThread != null && gatherThread.isAlive()) {
													gatherThread.setThreadStop(); // end serial communication
												}
												resetButtons();
											}
										});
									}
								}
								{
									liveDataCaptureGroup = new Group(dataMainComposite, SWT.NONE);
									liveDataCaptureGroup.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									liveDataCaptureGroup.setBounds(273, 12, 253, 230);
									liveDataCaptureGroup.setText("Datenaufzeichnung");
									liveDataCaptureGroup.setEnabled(false);
									{
										startLiveGatherButton = new Button(liveDataCaptureGroup, SWT.PUSH | SWT.CENTER);
										startLiveGatherButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										startLiveGatherButton.setText("Start life data gathering");
										startLiveGatherButton.setBounds(16, 29, 225, 30);
										startLiveGatherButton.setEnabled(true);
										startLiveGatherButton.setToolTipText("Start life Datenanzeige");
										startLiveGatherButton.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												log.log(Level.FINE, "liveViewButton.widgetSelected, event=" + evt); //$NON-NLS-1$
												try {
													startLiveGatherButton.setEnabled(false);
													readDataButton.setEnabled(false);
													stopLiveGatherButton.setEnabled(true);
													setClosePossible(false);
													liveThread = new LiPoWatchLiveGatherer(application, device, serialPort, LiPoWatchDialog.this);
													try {
														liveThread.start();
													}
													catch (RuntimeException e) {
														log.log(Level.WARNING, e.getMessage(), e);
													}
												}
												catch (Exception e) {
													if (liveThread != null && liveThread.isTimerRunning) {
														liveThread.stopTimerThread();
														liveThread.interrupt();
													}
													application.updateGraphicsWindow();
													application.openMessageDialog(getDialogShell(), e.getClass().getSimpleName() + e.getMessage());
													resetButtons();
												}
											}
										});
									}
									{
										loggingGroup = new Group(liveDataCaptureGroup, SWT.NONE);
										loggingGroup.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										loggingGroup.setBounds(16, 67, 225, 112);
										loggingGroup.setText("LiPoWatch Logging");
										{
											startLoggingButton = new Button(loggingGroup, SWT.PUSH | SWT.CENTER);
											startLoggingButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
											startLoggingButton.setText("Start");
											startLoggingButton.setBounds(17, 27, 196, 30);
											startLoggingButton.setEnabled(true);
											startLoggingButton.setToolTipText("Startet eine LiPoWatch Aufzeichnung");
											startLoggingButton.addSelectionListener(new SelectionAdapter() {
												@Override
												public void widgetSelected(SelectionEvent evt) {
													log.log(Level.FINE, "startLoggingButton.widgetSelected, event=" + evt); //$NON-NLS-1$
													try {
														setClosePossible(false);
														serialPort.startLogging();
														startLoggingButton.setEnabled(false);
														stopLoggingButton.setEnabled(true);
													}
													catch (Exception e) {
														log.log(Level.SEVERE, e.getMessage(), e);
														application.openMessageDialog(getDialogShell(), Messages.getString(osde.messages.MessageIds.OSDE_MSGE0029, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
													}
												}
											});
										}
										{
											stopLoggingButton = new Button(loggingGroup, SWT.PUSH | SWT.CENTER);
											stopLoggingButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
											stopLoggingButton.setText("Stop");
											stopLoggingButton.setBounds(17, 69, 196, 30);
											stopLoggingButton.setEnabled(false);
											stopLoggingButton.setToolTipText("Stoppt einen laufende LiPoWatch Aufzeichnung");
											stopLoggingButton.addSelectionListener(new SelectionAdapter() {
												@Override
												public void widgetSelected(SelectionEvent evt) {
													log.log(Level.FINE, "stopLoggingButton.widgetSelected, event=" + evt); //$NON-NLS-1$
													try {
														serialPort.stopLogging();
														startLoggingButton.setEnabled(true);
														stopLoggingButton.setEnabled(false);
														setClosePossible(true);
														if (!stopLiveGatherButton.getEnabled()) {
															readDataButton.setEnabled(true);
														}
													}
													catch (Exception e) {
														log.log(Level.SEVERE, e.getMessage(), e);
														application.openMessageDialog(getDialogShell(), Messages.getString(osde.messages.MessageIds.OSDE_MSGE0029, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
													}
												}
											});
										}
									}
									{
										stopLiveGatherButton = new Button(liveDataCaptureGroup, SWT.PUSH | SWT.CENTER);
										stopLiveGatherButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										stopLiveGatherButton.setBounds(17, 189, 224, 30);
										stopLiveGatherButton.setText("Stop life data gathering");
										stopLiveGatherButton.setEnabled(false);
										stopLiveGatherButton.setToolTipText("Stoppt eine life Datenanzeige");
										stopLiveGatherButton.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												log.log(Level.FINE, "stopLiveGatherButton.widgetSelected, event=" + evt); //$NON-NLS-1$
												if (liveThread != null) {
													liveThread.stopTimerThread();
													liveThread.interrupt();

													if (Channels.getInstance().getActiveChannel() != null) {
														RecordSet activeRecordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
														// active record set name == life gatherer record name
														liveThread.finalizeRecordSet(activeRecordSet.getName());
													}
												}
												stopLiveGatherButton.setEnabled(false);
												startLiveGatherButton.setEnabled(true);
												setClosePossible(true);
												if (!stopLoggingButton.getEnabled()) {
													readDataButton.setEnabled(true);
												}
											}
										});
									}
								}
								{
									deleteMemoryButton = new Button(dataMainComposite, SWT.PUSH | SWT.CENTER);
									deleteMemoryButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									deleteMemoryButton.setText("Delete Memory");
									deleteMemoryButton.setBounds(272, 248, 255, 30);
									deleteMemoryButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "deleteMemoryButton.widgetSelected, event=" + evt);
											try {
												deleteMemoryButton.setEnabled(false);
												serialPort.clearMemory();
											}
											catch (Exception e) {
												log.log(Level.SEVERE, e.getMessage(), e);
												application.openMessageDialog(getDialogShell(), Messages.getString(MessageIds.OSDE_MSGE1500, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
												e.printStackTrace();
											}
											deleteMemoryButton.setEnabled(true);
										}
									});
								}

							}
						}
					}
					
					closeButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
					closeButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
					closeButton.setText("Schliessen");
					closeButton.setBounds(132, 327, 273, 31);
					closeButton.setToolTipText("Schliesst den Gerätedialog ");
					closeButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINE, "closeButton.widgetSelected, event=" + evt);
							dispose();
						}
					});

					mainTabFolder.setBounds(0, 0, 542, 317);
					mainTabFolder.setSelection(0);
					mainTabFolder.addMouseTrackListener(new MouseTrackAdapter() {
						@Override
						public void mouseEnter(MouseEvent evt) {
							log.log(Level.FINER, "deviceConfigTabFolder.mouseEnter, event=" + evt); //$NON-NLS-1$
							fadeOutAplhaBlending(evt, mainTabFolder.getClientArea(), 10, 10, -10, -10);
						}

						@Override
						public void mouseHover(MouseEvent evt) {
							log.log(Level.FINEST, "deviceConfigTabFolder.mouseHover, event=" + evt); //$NON-NLS-1$
							fadeOutAplhaBlending(evt, mainTabFolder.getClientArea(), 10, 10, -10, -10);
						}

						@Override
						public void mouseExit(MouseEvent evt) {
							log.log(Level.FINER, "deviceConfigTabFolder.mouseExit, event=" + evt); //$NON-NLS-1$
							fadeInAlpaBlending(evt, mainTabFolder.getClientArea(), 10, 10, -10, -10);
						}
					});
				}
				dialogShell.open();
			}
			else {
				dialogShell.setVisible(true);
				dialogShell.setActive();
			}
			Display display = dialogShell.getDisplay();
			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	private void saveConfigButtonWidgetDefaultSelected(SelectionEvent evt) {
		log.log(Level.FINEST, "saveConfigButton.widgetDefaultSelected, event="+evt);
		//TODO add your code for saveConfigButton.widgetDefaultSelected
	}

	/**
	 * updates the analog record descriptors according input fields
	 * attention: set new record name replaces the record, setName() must the last operation in sequence
	 */
	public void checkUpdateAnalog() {
		if (channels.getActiveChannel() != null) {
			RecordSet activeRecordSet = channels.getActiveChannel().getActiveRecordSet();
			if (activeRecordSet != null) {
				// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
				// 11=a1Value
				activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setActive(temperatureButton.getSelection());
				//activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setVisible(temperatureButton.getSelection());
				activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setDisplayable(temperatureButton.getSelection());
				activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setOffset(new Double(a1OffsetText.getText().trim().replace(',', '.')));
				activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setFactor(new Double(a1FactorText.getText().trim().replace(',', '.')));
				
				application.updateGraphicsWindow();
				activeRecordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
			}
		}
		saveDisplayConfigButton.setEnabled(true);
	}

	/**
	 * update the configuration tab with values red 
	 * @param readBuffer
	 */
	public void updateConfigurationValues(byte[] readBuffer) {
		
		//int length = (readBuffer[0] & 0x7F);    // höchstes Bit steht für Einstellungen, sonst Daten
		//log.log(Level.INFO, "length = " + length); //$NON-NLS-1$

		//status field
		//Speichernummer = (CLng(Asc(Mid(strResult, 9, 1))) * 256 * 256 * 256 + CLng(Asc(Mid(strResult, 8, 1))) * 256 * 256 + CLng(Asc(Mid(strResult, 7, 1))) * 256 + Asc(Mid(strResult, 6, 1)))
		memoryUsed = ((readBuffer[8] & 0xFF) << 24) + ((readBuffer[7] & 0xFF) << 16) + ((readBuffer[6] & 0xFF) << 8) + (readBuffer[7] & 0xFF);
		log.log(Level.INFO, "memoryUsed = " + memoryUsed); //$NON-NLS-1$

		//Seriennummer = CLng(Asc(Mid(strResult, 11, 1))) * 256 + Asc(Mid(strResult, 10, 1))
		serialNumber = "" + (((readBuffer[10] & 0xFF) << 8) + (readBuffer[9] & 0xFF)); //$NON-NLS-1$
		log.log(Level.INFO, "serialNumber = " + serialNumber); //$NON-NLS-1$
		snLabel.setText(serialNumber);
		
		//LiPoWatch_Version = CLng(Asc(Mid(strResult, 12, 1)))
		lipoWatchVersion = String.format(Locale.ENGLISH, "v%.2f", new Double((readBuffer[11] & 0xFF) / 100)); //$NON-NLS-1$
		log.log(Level.INFO, "unilogVersion = " + lipoWatchVersion); //$NON-NLS-1$
		firmwareVersionLabel.setText(lipoWatchVersion);

		//Speicher_geloescht = CLng(Asc(Mid(strResult, 13, 1)))
		int memoryDeleted = readBuffer[12] & 0xFF;
		int tmpMemoryUsed = 0;
		if (memoryDeleted > 0)
			tmpMemoryUsed = 0;
		else
			tmpMemoryUsed = memoryUsed;
		memoryUsedPercent = String.format("%.2f", tmpMemoryUsed * 100.0 / MAX_DATA_VALUES); //$NON-NLS-1$
		log.log(Level.INFO, "memoryUsedPercent = " + memoryUsedPercent + " (" + tmpMemoryUsed + "/" + MAX_DATA_RECORDS + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		memUsagePercent.setText(memoryUsedPercent);

		// timer interval
		//Speicherrate_Box.Value = CLng(Asc(Mid(strResult, 14, 1)))
		timeIntervalPosition = readBuffer[13] & 0xFF;
		log.log(Level.INFO, "timeIntervalPosition = " + timeIntervalPosition); //$NON-NLS-1$
		timeIntervalCombo.select(timeIntervalPosition);
		updateTimeStep_ms(timeIntervalPosition);
		
		//Modus_Spannung_Box.Value = CLng(Asc(Mid(strResult, 15, 1)))
		measurementModus = readBuffer[14] & 0xFF;
		log.log(Level.INFO, "measurementModus(relative, absolute) = " + measurementModus); //$NON-NLS-1$
		measurementModusCombo.select(measurementModus);
	
		isAutoStartTime = false;
		timeAutoStart_sec = 0;
		if ((readBuffer[15] & 0x80) != 0) {
			isAutoStartTime = true;
		}
		//Autostart_Zeit_Box.Value = CLng(Asc(Mid(strResult, 16, 1))) And &H7F
		timeAutoStart_sec = readBuffer[15] & 0x7F;
		log.log(Level.INFO, "isAutoStartTime = " + isAutoStartTime + " timeAutoStart_sec = " + timeAutoStart_sec); //$NON-NLS-1$ //$NON-NLS-2$
		timeTriggerButton.setSelection(isAutoStartTime);
		int timeSelect = 0;
		for (; timeSelect < timeTriggerCombo.getItemCount(); ++timeSelect){
			if (timeAutoStart_sec >= Integer.parseInt(timeTriggerCombo.getItems()[timeSelect].trim())) break;
		}
		timeTriggerCombo.select(timeSelect);
		timeTriggerCombo.setText(String.format("%4s", timeAutoStart_sec)); //$NON-NLS-1$

		//Spannungsschwelle_Box.Value = CLng(Asc(Mid(strResult, 17, 1))) - 20
		voltageLevelRegulationLimit = (readBuffer[16] & 0xFF)-20;
		log.log(Level.INFO, "voltageLevelRegulationLimit = " + voltageLevelRegulationLimit); //$NON-NLS-1$
		voltageLevelRegulationCombo.select(voltageLevelRegulationLimit);

		isAutStartRx = false;
		isRxOn = false;
		rxAutoStartValue = 0;
		//If (CLng(Asc(Mid(strResult, 18, 1))) And &H80) = 0 Then
		if ((readBuffer[17] & 0x80) != 0) {
			isAutStartRx = true;
		}
		//Startimpuls_Box.Value = (CLng(Asc(Mid(strResult, 18, 1))) And &H7F) - 11  ' Wert wird in us geteilt durch 100 gespeichert
		rxAutoStartValue = (readBuffer[17] & 0x7F); // 16 = 1.6 ms (value - 11 = position in RX_AUTO_START_MS)
		impulseTriggerCombo.select(rxAutoStartValue - 11);
		impulseTriggerButton.setSelection(isAutStartRx);
		log.log(Level.INFO, "isAutStartRx = " + isAutStartRx + " isRxOn = " + isRxOn + " rxAutoStartValue = " + rxAutoStartValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		//Abregelung_Box.Value = CLng(Asc(Mid(strResult, 19, 1)))
		impulsReductionType = (readBuffer[18] & 0x7F);
		log.log(Level.INFO, "impulsReductionType = " + impulsReductionType); //$NON-NLS-1$
		regulationTypeCombo.select(impulsReductionType);

		isAutoStartVoltageDrop = false;
		//If CLng(Asc(Mid(strResult, 20, 1))) = 0 Then
		if ((readBuffer[19] & 0xFF) != 0) {
			isAutoStartVoltageDrop = true;
		}
		log.log(Level.INFO, "isAutoStartVoltageDrop = " + isAutoStartVoltageDrop); //$NON-NLS-1$ //$NON-NLS-2$
		voltageDropTriggerButton.setSelection(isAutoStartVoltageDrop);	
		
		//Zellentyp_Box.Value = CLng(Asc(Mid(strResult, 21, 1)))
		cellType = readBuffer[20] & 0xFF;
		log.log(Level.INFO, "cellType = " + cellType ); 
		cellTypeCombo.select(cellType);
		
	}

	public byte[] buildUpdateBuffer() {
		int checkSum = 0;
		byte[] updateBuffer = new byte[21];
		updateBuffer[0] = (byte) 0xC0;
		updateBuffer[1] = (byte) 0x04; // update buffer ID
		checkSum = checkSum + (0xFF & updateBuffer[1]);
		updateBuffer[2] = (byte) 0x11; //17 length
		checkSum = checkSum + (0xFF & updateBuffer[2]);
		updateBuffer[3] = (byte) 0x02; // update order
		checkSum = checkSum + (0xFF & updateBuffer[3]);
		
		updateBuffer[4] = (byte) 0x00; // memory number
		updateBuffer[5] = (byte) 0x00;
		updateBuffer[6] = (byte) 0x00;
		updateBuffer[7] = (byte) 0x00;	
		updateBuffer[8] = (byte) 0x00; // serial number
		updateBuffer[9] = (byte) 0x00;	
		updateBuffer[10] = (byte) 0x00; // firmware
		updateBuffer[11] = (byte) 0x00; // memory cleared

		updateBuffer[12] = (byte) this.timeIntervalCombo.getSelectionIndex(); // time step
		checkSum = checkSum + (0xFF & updateBuffer[12]);

		updateBuffer[13] = (byte) measurementModusCombo.getSelectionIndex(); // measurement modus, relative, absolute
		checkSum = checkSum + (0xFF & updateBuffer[13]);
		
		if (this.timeTriggerButton.getSelection())
			updateBuffer[14] = (byte) ((new Byte(this.timeTriggerCombo.getText().trim())) | 0x80);
		else {
			int autoStartZeit = Integer.parseInt(this.timeTriggerCombo.getText().trim());
			autoStartZeit = autoStartZeit - (autoStartZeit % 5);
			updateBuffer[14] = (byte) (autoStartZeit & 0x7F);
		}
		checkSum = checkSum + (0xFF & updateBuffer[14]);
		
		updateBuffer[15] = (byte) (voltageLevelRegulationCombo.getSelectionIndex() + 20); // voltage level to start impules regulation
		checkSum = checkSum + (0xFF & updateBuffer[15]);

		if (this.impulseTriggerButton.getSelection()) {
			if ((this.impulseTriggerCombo.getSelectionIndex() + 1) == RX_AUTO_START_MS.length) // "RX an"
				updateBuffer[16] = (byte) 0x80;
			else
				updateBuffer[16] = (byte) ((this.impulseTriggerCombo.getSelectionIndex() | 0x80) + 11);
		}
		else if ((this.impulseTriggerCombo.getSelectionIndex() + 1) == RX_AUTO_START_MS.length) // "RX an"
			updateBuffer[16] = (byte) 0x00;
		else
			updateBuffer[16] = (byte) (this.impulseTriggerCombo.getSelectionIndex() + 11);
		checkSum = checkSum + (0xFF & updateBuffer[16]);

		updateBuffer[17] = (byte) regulationTypeCombo.getSelectionIndex(); // regulation type off, soft, hard
		checkSum = checkSum + (0xFF & updateBuffer[17]);
		
		updateBuffer[18] = (byte) (voltageDropTriggerButton.getSelection() ? 0x01 : 0x00); // voltage drop trigger
		checkSum = checkSum + (0xFF & updateBuffer[18]);

		updateBuffer[19] = (byte) this.cellTypeCombo.getSelectionIndex(); // cell type
		checkSum = checkSum + (0xFF & updateBuffer[19]);

		updateBuffer[20] = (byte) (checkSum % 256);

		if (log.isLoggable(Level.INFO)) {
			StringBuilder sb = new StringBuilder();
			sb.append("updateBuffer = ["); //$NON-NLS-1$
			for (int i = 0; i < updateBuffer.length; i++) {
				if (i == updateBuffer.length - 1)
					sb.append(String.format("%02X", updateBuffer[i])); //$NON-NLS-1$
				else
					sb.append(String.format("%02X ", updateBuffer[i])); //$NON-NLS-1$
			}
			sb.append("]"); //$NON-NLS-1$
			log.log(Level.INFO, sb.toString());
		}

		return updateBuffer;
	}

	/**
	 * function to reset counter labels
	 */
	public void resetDataSetsLabel() {
		if (Thread.currentThread().getId() == application.getThreadId()) {
			numberRedDataSetsText = "0"; //$NON-NLS-1$
			numberActualDataSetsText = "0"; //$NON-NLS-1$
			numberReadErrorText = "0"; //$NON-NLS-1$
			redDataSetLabel.setText(numberRedDataSetsText);
			actualDataSetNumber.setText(numberActualDataSetsText);
			numberReadErrorLabel.setText(numberReadErrorText);
			readDataProgressBar.setSelection(0);
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					numberRedDataSetsText = "0"; //$NON-NLS-1$
					numberActualDataSetsText = "0"; //$NON-NLS-1$
					numberReadErrorText = "0"; //$NON-NLS-1$
					redDataSetLabel.setText(numberRedDataSetsText);
					actualDataSetNumber.setText(numberActualDataSetsText);
					numberReadErrorLabel.setText(numberReadErrorText);
					readDataProgressBar.setSelection(0);
				}
			});
		}
	}

	/**
	 * function to reset all the buttons, normally called after data gathering finished
	 */
	public void resetButtons() {
//		if (Thread.currentThread().getId() == application.getThreadId()) {
//			editConfigButton.setEnabled(false);
//			useConfigCombo.setEnabled(true);
//
//			readDataButton.setEnabled(true);
//			stopDataButton.setEnabled(false);
//
//			startLoggingButton.setEnabled(true);
//			stopLoggingButton.setEnabled(false);
//
//			startLiveGatherButton.setEnabled(true);
//			stopLiveGatherButton.setEnabled(false);
//
//			deleteMemoryButton.setEnabled(true);
//			setClosePossible(true);
//		}
//		else {
//			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
//				public void run() {
//					readDataButton.setEnabled(true);
//					stopReadDataButton.setEnabled(false);
//
//					startLoggingButton.setEnabled(true);
//					stopLoggingButton.setEnabled(false);
//
//					startLiveGatherButton.setEnabled(true);
//					stopLiveGatherButton.setEnabled(false);
//
//					deleteMemoryButton.setEnabled(true);
//					setClosePossible(true);
//				}
//			});
//		}
	}

	/**
	 * update the used timeStep_ms, this is used for all type of calculations
	 * @param timeIntervalIndex the index of the TIME_INTERVAL array
	 */
	void updateTimeStep_ms(int timeIntervalIndex) {
		switch (timeIntervalIndex) {
		default:
		case 0: // 1/4 sec
			device.setTimeStep_ms(1000.0 / 4);
			break;
		case 1: // 1/2 sec
			device.setTimeStep_ms(1000.0 / 2);
			break;
		case 2: // 1 sec
			device.setTimeStep_ms(1000.0 * 1);
			break;
		case 3: // 2 sec
			device.setTimeStep_ms(1000.0 * 2);
			break;
		case 4: // 5 sec
			device.setTimeStep_ms(1000.0 * 5);
			break;
		case 5: // 10 sec
			device.setTimeStep_ms(1000.0 * 10);
			break;
		}
		device.storeDeviceProperties();
	}

	/**
	 * set progress bar to value between 0 to 100, called out of thread
	 * @param value
	 */
	public void setReadDataProgressBar(final int value) {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				int tmpValue = value < 0 ? 0 : value;
				tmpValue = value > 100 ? 100 : value;
				//LiPoWatchDialog.readDataProgressBar.setSelection(tmpValue);
			}
		});
	}
	
	private Button getCloseButton(Composite parent) {
		if(closeButton == null) {
		}
		return closeButton;
	}

}
