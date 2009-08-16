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
	public final static String[]	RX_AUTO_START_MS					= { " 1,1", " 1,2", " 1,3", " 1,4", " 1,5", " 1,6", " 1,7", " 1,8", " 1,9", " Rx on" };	//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
	public final static String[] CELL_VOLTAGE_LIMITS = new String[] { " 2.0", " 2.1", " 2.2", " 2.3", " 2.4", " 2.5", " 2.6", " 2.7", " 2.8", " 2.9", " 3.0", " 3.1", " 3.2",
		" 3.3", " 3.4", " 3.5", " 3.6", " 3.7" };


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
	
	Button storeConfigButton;
	Button readConfigButton;

	CTabItem displayTabItem;
	Composite displayComposite;

	CTabItem dataTabItem;
	Composite dataMainComposite;
	Group dataReadGroup;
	CLabel clearMemoryLabel;
	Group clearMemoryGroup;
	Button readDataButton, stopReadDataButton;
	CLabel dataSetLabel, redDataSetLabel, actualDataSetNumberLabel, actualDataSetNumber, readDataErrorLabel, numberReadErrorLabel;
	ProgressBar readDataProgressBar;
	
	Group liveDataCaptureGroup, loggingGroup;
	Button startLiveGatherButton, stopLiveGatherButton, startLoggingButton, stopLoggingButton; 
	Button	clearMemoryButton;
	
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
				dialogShell.setSize(509, 394);
				dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 175, 100));
				dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						log.log(Level.FINE, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (storeConfigButton.getEnabled()) {
							String msg = Messages.getString(MessageIds.OSDE_MSGI1500);
							if (application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								log.log(Level.FINE, "SWT.YES"); //$NON-NLS-1$
								//device.storeDeviceProperties();
								setClosePossible(true);
							}
							// check threads before close
							if (gatherThread != null && gatherThread.isAlive()) {
								gatherThread.interrupt();
							}
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
				dialogShell.addMouseTrackListener(new MouseTrackAdapter() {
					public void mouseEnter(MouseEvent evt) {
						log.log(Level.FINER, "dialogShell.mouseEnter, event=" + evt); //$NON-NLS-1$
						fadeOutAplhaBlending(evt, getDialogShell().getClientArea(), 20, 20, 20, 25);
					}

					public void mouseHover(MouseEvent evt) {
						log.log(Level.FINEST, "dialogShell.mouseHover, event=" + evt); //$NON-NLS-1$
					}

					public void mouseExit(MouseEvent evt) {
						log.log(Level.FINER, "dialogShell.mouseExit, event=" + evt); //$NON-NLS-1$
						fadeInAlpaBlending(evt, getDialogShell().getClientArea(), 20, 20, -20, 25);
					}
				});
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
								statusGroup.setBounds(12, 4, 473, 45);
								{
									serialNumberText = new CLabel(statusGroup, SWT.NONE);
									serialNumberText.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									serialNumberText.setText("S/N:");
									serialNumberText.setBounds(6, 17, 40, 22);
									serialNumberText.setToolTipText("serial number of connected LiPoWatch");
								}
								{
									snLabel = new CLabel(statusGroup, SWT.CENTER | SWT.EMBEDDED);
									snLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									snLabel.setBounds(46, 17, 65, 22);
									snLabel.setText(serialNumber);
								}
								{
									firmwareText = new CLabel(statusGroup, SWT.NONE);
									firmwareText.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									firmwareText.setText("Firmware:");
									firmwareText.setBounds(115, 17, 70, 22);
									firmwareText.setToolTipText("firmware level of connected LiPoWatch");
								}
								{
									firmwareVersionLabel = new CLabel(statusGroup, SWT.NONE);
									firmwareVersionLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									firmwareVersionLabel.setBounds(185, 17, 69, 22);
									firmwareVersionLabel.setText(lipoWatchVersion);
								}
								{
									memoryUsageText = new CLabel(statusGroup, SWT.NONE);
									memoryUsageText.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									memoryUsageText.setText("Speicherbelegung");
									memoryUsageText.setBounds(255, 17, 124, 22);
								}
								{
									memUsagePercent = new CLabel(statusGroup, SWT.RIGHT);
									memUsagePercent.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									memUsagePercent.setBounds(380, 17, 59, 22);
									memUsagePercent.setText(memoryUsedPercent);
								}
								{
									memUsageUnit = new CLabel(statusGroup, SWT.NONE);
									memUsageUnit.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									memUsageUnit.setText("[%]");
									memUsageUnit.setBounds(442, 17, 26, 22);
								}
							} // end status group

							{
								autoStartGroup = new Group(configMainComosite, SWT.NONE);
								autoStartGroup.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
								autoStartGroup.setText("Logging Autostart");
								autoStartGroup.setBounds(12, 136, 232, 107);
								{
									voltageDropTriggerButton = new Button(autoStartGroup, SWT.CHECK | SWT.RIGHT);
									voltageDropTriggerButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									voltageDropTriggerButton.setText("voltage drop trigger ");
									voltageDropTriggerButton.setToolTipText("startet die Aufnahme bei sinkender Spannungslage");
									voltageDropTriggerButton.setSelection(isAutoStartVoltageDrop);
									voltageDropTriggerButton.setBounds(16, 24, 139, 20);
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
									timeTriggerButton.setBounds(16, 52, 139, 20);
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
									timeTriggerCombo.setBounds(159, 52, 59, 20);
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
									impulseTriggerButton.setBounds(16, 78, 139, 20);
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
									impulseTriggerCombo.setBounds(159, 78, 59, 20);
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
								dataRateGroup.setBounds(14, 89, 232, 45);
								dataRateGroup.setToolTipText("adjust data rate used for data logging");
								dataRateGroup.addMouseTrackListener(mouseTrackerEnterFadeOut);
								{
									timeIntervalCombo = new CCombo(dataRateGroup, SWT.BORDER);
									timeIntervalCombo.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									timeIntervalCombo.setItems(TIME_INTERVAL);
									timeIntervalCombo.setBounds(49, 17, 133, 20);
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
								readConfigButton.setBounds(135, 56, 232, 30);
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
								storeConfigButton.setBounds(135, 249, 229, 30);
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
								impuleRegulationGroup.setBounds(252, 136, 232, 107);
								{
									regulationTypeLabel = new CLabel(impuleRegulationGroup, SWT.RIGHT);
									regulationTypeLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									regulationTypeLabel.setText("type");
									regulationTypeLabel.setBounds(14, 21, 110, 20);
									regulationTypeLabel.setToolTipText("type of slow down regulation at adjusted trigger level");
								}
								{
									voltageLimitLabel = new CLabel(impuleRegulationGroup, SWT.RIGHT);
									voltageLimitLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									voltageLimitLabel.setText("voltage limit [V]");
									voltageLimitLabel.setBounds(14, 49, 110, 20);
									voltageLimitLabel.setToolTipText("voltage limit when regulation begins");
								}
								{
									cellTypeLabel = new CLabel(impuleRegulationGroup, SWT.RIGHT);
									cellTypeLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									cellTypeLabel.setText("cell type");
									cellTypeLabel.setBounds(14, 75, 110, 20);
								}
								{
									regulationTypeCombo = new CCombo(impuleRegulationGroup, SWT.BORDER | SWT.CENTER);
									regulationTypeCombo.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									regulationTypeCombo.setItems(new String[] { "  off", "  soft", "  hard" });
									regulationTypeCombo.setBounds(134, 23, 69, 20);
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
									voltageLevelRegulationCombo.setItems(CELL_VOLTAGE_LIMITS);
									voltageLevelRegulationCombo.setBounds(134, 51, 69, 20);
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
									cellTypeCombo.setBounds(134, 77, 69, 20);
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
								measurementTypeGroup.setBounds(252, 89, 232, 45);
								measurementTypeGroup.setToolTipText("define if voltage will be measured absolute (4, 8 12..) or relative  (4, 4, 4)");
								{
									measurementModusLabel = new CLabel(measurementTypeGroup, SWT.RIGHT);
									measurementModusLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									measurementModusLabel.setText("modus");
									measurementModusLabel.setBounds(14, 17, 72, 20);
									measurementModusLabel.setToolTipText("define if voltage will be measured absolute (4, 8 12..) or relative  (4, 4, 4)");
								}
								{
									measurementModusCombo = new CCombo(measurementTypeGroup, SWT.BORDER);
									measurementModusCombo.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
									measurementModusCombo.setBounds(96, 17, 104, 20);
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
									dataReadGroup.setBounds(12, 12, 241, 263);
									dataReadGroup.setText("Daten auslesen");
									{
										readDataButton = new Button(dataReadGroup, SWT.PUSH | SWT.CENTER);
										readDataButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										readDataButton.setText("Start Daten auslesen");
										readDataButton.setBounds(11, 24, 218, 30);
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
												readConfigButton.setEnabled(false);
												storeConfigButton.setEnabled(false);
												readDataButton.setEnabled(false);
												stopReadDataButton.setEnabled(true);
												startLoggingButton.setEnabled(false);
												stopLoggingButton.setEnabled(false);
												startLiveGatherButton.setEnabled(false);
												clearMemoryButton.setEnabled(false);
												closeButton.setEnabled(false);
											}
										});
									}
									{
										dataSetLabel = new CLabel(dataReadGroup, SWT.RIGHT);
										dataSetLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										dataSetLabel.setBounds(4, 74, 164, 20);
										dataSetLabel.setText("red measurements  :");
									}
									{
										redDataSetLabel = new CLabel(dataReadGroup, SWT.RIGHT);
										redDataSetLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										redDataSetLabel.setBounds(174, 74, 55, 20);
										redDataSetLabel.setText("0"); //$NON-NLS-1$
									}
									{
										actualDataSetNumberLabel = new CLabel(dataReadGroup, SWT.RIGHT);
										actualDataSetNumberLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										actualDataSetNumberLabel.setBounds(4, 106, 164, 20);
										actualDataSetNumberLabel.setText("current data set number  :");
									}
									{
										actualDataSetNumber = new CLabel(dataReadGroup, SWT.RIGHT);
										actualDataSetNumber.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										actualDataSetNumber.setBounds(174, 106, 55, 20);
										actualDataSetNumber.setText("0"); //$NON-NLS-1$
									}
									{
										readDataErrorLabel = new CLabel(dataReadGroup, SWT.RIGHT);
										readDataErrorLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										readDataErrorLabel.setBounds(4, 138, 164, 20);
										readDataErrorLabel.setText("data transmission errors  :");
									}
									{
										numberReadErrorLabel = new CLabel(dataReadGroup, SWT.RIGHT);
										numberReadErrorLabel.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										numberReadErrorLabel.setBounds(174, 138, 55, 20);
										numberReadErrorLabel.setText("0"); //$NON-NLS-1$
									}
									{
										readDataProgressBar = new ProgressBar(dataReadGroup, SWT.NONE);
										readDataProgressBar.setBounds(11, 183, 218, 20);
										readDataProgressBar.setMinimum(0);
										readDataProgressBar.setMaximum(100);
									}
									{
										stopReadDataButton = new Button(dataReadGroup, SWT.PUSH | SWT.CENTER);
										stopReadDataButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										stopReadDataButton.setBounds(11, 222, 218, 30);
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
									liveDataCaptureGroup.setBounds(259, 12, 228, 164);
									liveDataCaptureGroup.setText("Datenaufzeichnung");
									liveDataCaptureGroup.setEnabled(false);
									{
										startLiveGatherButton = new Button(liveDataCaptureGroup, SWT.PUSH | SWT.CENTER);
										startLiveGatherButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
										startLiveGatherButton.setText("Start life data gathering");
										startLiveGatherButton.setBounds(12, 24, 202, 30);
										startLiveGatherButton.setEnabled(true);
										startLiveGatherButton.setToolTipText("Start life Datenanzeige");
										startLiveGatherButton.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												log.log(Level.FINE, "liveViewButton.widgetSelected, event=" + evt); //$NON-NLS-1$
												try {
													readConfigButton.setEnabled(false);
													storeConfigButton.setEnabled(false);
													startLiveGatherButton.setEnabled(false);
													readDataButton.setEnabled(false);
													stopReadDataButton.setEnabled(false);
													stopLiveGatherButton.setEnabled(true);
													clearMemoryButton.setEnabled(false);
													closeButton.setEnabled(false);
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
										loggingGroup.setBounds(12, 56, 202, 63);
										loggingGroup.setText("LiPoWatch Logging");
										{
											startLoggingButton = new Button(loggingGroup, SWT.PUSH | SWT.CENTER);
											startLoggingButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
											startLoggingButton.setText("Start");
											startLoggingButton.setBounds(12, 21, 70, 30);
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
											stopLoggingButton.setBounds(94, 21, 82, 30);
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
										stopLiveGatherButton.setBounds(12, 126, 202, 30);
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
												readConfigButton.setEnabled(true);
												storeConfigButton.setEnabled(false);
												readDataButton.setEnabled(true);
												stopReadDataButton.setEnabled(false);
												startLiveGatherButton.setEnabled(true);
												stopLiveGatherButton.setEnabled(false);
												clearMemoryButton.setEnabled(true);
												closeButton.setEnabled(true);
												setClosePossible(true);
												if (!stopLoggingButton.getEnabled()) {
													readDataButton.setEnabled(true);
												}
											}
										});
									}
								}
								{
									clearMemoryGroup = new Group(dataMainComposite, SWT.NONE);
									clearMemoryGroup.setLayout(null);
									clearMemoryGroup.setText("Data memory");
									clearMemoryGroup.setBounds(261, 178, 226, 97);
									{
										clearMemoryButton = new Button(clearMemoryGroup, SWT.PUSH | SWT.CENTER);
										clearMemoryButton.setFont(SWTResourceManager.getFont(application,application.getWidgetFontSize(),SWT.NORMAL));
										clearMemoryButton.setText("Delete Memory");
										clearMemoryButton.setBounds(12, 56, 202, 31);
										clearMemoryButton.setToolTipText("data will deleted just before next  data logging begins");
										clearMemoryButton.addSelectionListener(new SelectionAdapter() {
											public void widgetSelected(SelectionEvent evt) {
												log.log(Level.FINEST, "deleteMemoryButton.widgetSelected, event=" + evt);
												try {
													clearMemoryButton.setEnabled(false);
													serialPort.clearMemory();
												}
												catch (Exception e) {
													log.log(Level.SEVERE, e.getMessage(), e);
													application.openMessageDialog(getDialogShell(), Messages.getString(MessageIds.OSDE_MSGE1500, new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
													e.printStackTrace();
												}
												clearMemoryButton.setEnabled(true);
											}
										});
									}
									{
										clearMemoryLabel = new CLabel(clearMemoryGroup, SWT.CENTER | SWT.EMBEDDED);
										clearMemoryLabel.setText("clear memory will be performed\njust before starting new logging");
										clearMemoryLabel.setBounds(12, 16, 202, 40);
									}
								}

							}
						}
					}
					
					closeButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
					closeButton.setFont(SWTResourceManager.getFont(application, application.getWidgetFontSize(), SWT.NORMAL));
					closeButton.setText("Schliessen");
					closeButton.setBounds(127, 318, 273, 31);
					closeButton.setToolTipText("Schliesst den Gerätedialog ");
					closeButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINE, "closeButton.widgetSelected, event=" + evt);
							dispose();
						}
					});

					mainTabFolder.setBounds(0, 0, 501, 312);
					mainTabFolder.setSelection(0);
//					mainTabFolder.addMouseTrackListener(new MouseTrackAdapter() {
//						@Override
//						public void mouseEnter(MouseEvent evt) {
//							log.log(Level.FINER, "deviceConfigTabFolder.mouseEnter, event=" + evt); //$NON-NLS-1$
//							fadeOutAplhaBlending(evt, mainTabFolder.getClientArea(), 10, 10, -10, 0);
//						}
//
//						@Override
//						public void mouseHover(MouseEvent evt) {
//							log.log(Level.FINEST, "deviceConfigTabFolder.mouseHover, event=" + evt); //$NON-NLS-1$
//						}
//
//						@Override
//						public void mouseExit(MouseEvent evt) {
//							log.log(Level.FINER, "deviceConfigTabFolder.mouseExit, event=" + evt); //$NON-NLS-1$
//							fadeInAlpaBlending(evt, mainTabFolder.getClientArea(), 10, 10, -10, -10);
//						}
//					});
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
	
	/**
	 * analog connection propertis ahas to be adjusted manually if required
	 * updates the analog record descriptors according input fields
	 * attention: set new record name replaces the record, setName() must the last operation in sequence
	 */
//	public void checkUpdateAnalog() {
//		if (channels.getActiveChannel() != null) {
//			RecordSet activeRecordSet = channels.getActiveChannel().getActiveRecordSet();
//			if (activeRecordSet != null) {
//				// 0=total voltage, 1=ServoImpuls on, 2=ServoImpulse off, 3=temperature, 4=cell voltage, 5=cell voltage, 6=cell voltage, .... 
//				activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setActive(temperatureButton.getSelection());
//				//activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setVisible(temperatureButton.getSelection());
//				activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setDisplayable(temperatureButton.getSelection());
//				activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setOffset(new Double(a1OffsetText.getText().trim().replace(',', '.')));
//				activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setFactor(new Double(a1FactorText.getText().trim().replace(',', '.')));
//				
//				application.updateGraphicsWindow();
//				activeRecordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
//			}
//		}
//		saveDisplayConfigButton.setEnabled(true);
//	}

	/**
	 * update the configuration tab with values red 
	 * @param readBuffer
	 */
	public void updateConfigurationValues(byte[] readBuffer) {
		
		//int length = (readBuffer[0] & 0x7F);    // höchstes Bit steht für Einstellungen, sonst Daten
		//log.log(Level.INFO, "length = " + length); //$NON-NLS-1$

		//status field
		//Speichernummer = (CLng(Asc(Mid(strResult, 9, 1))) * 256 * 256 * 256 + CLng(Asc(Mid(strResult, 8, 1))) * 256 * 256 + CLng(Asc(Mid(strResult, 7, 1))) * 256 + Asc(Mid(strResult, 6, 1)))
		memoryUsed = ((readBuffer[8] & 0xFF) << 24) + ((readBuffer[7] & 0xFF) << 16) + ((readBuffer[6] & 0xFF) << 8) + (readBuffer[5] & 0xFF);
		log.log(Level.FINE, "memoryUsed = " + memoryUsed); //$NON-NLS-1$

		//Seriennummer = CLng(Asc(Mid(strResult, 11, 1))) * 256 + Asc(Mid(strResult, 10, 1))
		serialNumber = "" + (((readBuffer[10] & 0xFF) << 8) + (readBuffer[9] & 0xFF)); //$NON-NLS-1$
		log.log(Level.FINE, "serialNumber = " + serialNumber); //$NON-NLS-1$
		snLabel.setText(serialNumber);
		
		//LiPoWatch_Version = CLng(Asc(Mid(strResult, 12, 1)))
		lipoWatchVersion = String.format(Locale.ENGLISH, "v%.2f", new Double((readBuffer[11] & 0xFF) / 100)); //$NON-NLS-1$
		log.log(Level.FINE, "unilogVersion = " + lipoWatchVersion); //$NON-NLS-1$
		firmwareVersionLabel.setText(lipoWatchVersion);

		//Speicher_geloescht = CLng(Asc(Mid(strResult, 13, 1)))
		int memoryDeleted = readBuffer[12] & 0xFF;
		int tmpMemoryUsed = 0;
		if (memoryDeleted > 0)
			tmpMemoryUsed = 0;
		else
			tmpMemoryUsed = memoryUsed;
		memoryUsedPercent = String.format("%.2f", tmpMemoryUsed * 100.0 / MAX_DATA_VALUES); //$NON-NLS-1$
		log.log(Level.FINE, "memoryUsedPercent = " + memoryUsedPercent + " (" + tmpMemoryUsed + "/" + MAX_DATA_RECORDS + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		memUsagePercent.setText(memoryUsedPercent);

		// timer interval
		//Speicherrate_Box.Value = CLng(Asc(Mid(strResult, 14, 1)))
		timeIntervalPosition = readBuffer[13] & 0xFF;
		log.log(Level.FINE, "timeIntervalPosition = " + timeIntervalPosition); //$NON-NLS-1$
		timeIntervalCombo.select(timeIntervalPosition);
		updateTimeStep_ms(timeIntervalPosition);
		
		//Modus_Spannung_Box.Value = CLng(Asc(Mid(strResult, 15, 1)))
		measurementModus = readBuffer[14] & 0xFF;
		log.log(Level.FINE, "measurementModus(relative, absolute) = " + measurementModus); //$NON-NLS-1$
		measurementModusCombo.select(measurementModus);
	
		isAutoStartTime = false;
		timeAutoStart_sec = 0;
		if ((readBuffer[15] & 0x80) != 0) {
			isAutoStartTime = true;
		}
		//Autostart_Zeit_Box.Value = CLng(Asc(Mid(strResult, 16, 1))) And &H7F
		timeAutoStart_sec = readBuffer[15] & 0x7F;
		log.log(Level.FINE, "isAutoStartTime = " + isAutoStartTime + " timeAutoStart_sec = " + timeAutoStart_sec); //$NON-NLS-1$ //$NON-NLS-2$
		timeTriggerButton.setSelection(isAutoStartTime);
		int timeSelect = 0;
		for (; timeSelect < timeTriggerCombo.getItemCount(); ++timeSelect){
			if (timeAutoStart_sec >= Integer.parseInt(timeTriggerCombo.getItems()[timeSelect].trim())) break;
		}
		timeTriggerCombo.select(timeSelect);
		timeTriggerCombo.setText(String.format("%4s", timeAutoStart_sec)); //$NON-NLS-1$

		//Spannungsschwelle_Box.Value = CLng(Asc(Mid(strResult, 17, 1))) - 20
		voltageLevelRegulationLimit = (readBuffer[16] & 0xFF)-20;
		log.log(Level.FINE, "voltageLevelRegulationLimit = " + voltageLevelRegulationLimit); //$NON-NLS-1$
		voltageLevelRegulationCombo.select(voltageLevelRegulationLimit);

		//If (CLng(Asc(Mid(strResult, 18, 1))) And &H80) = 0 Then CheckBox_Empfaengersteuerung.Value = False
		isAutStartRx = (readBuffer[17] & 0x80) != 0;
		impulseTriggerButton.setSelection(isAutStartRx);
		
		//If (CLng(Asc(Mid(strResult, 18, 1))) And &H7F) = 0 Then Startimpuls_Box.Text = "Rx an"
		isRxOn = (readBuffer[17] & 0x7F) == 0;		
		if (isRxOn) {
			rxAutoStartValue = (readBuffer[17] & 0x7F) + 9; // 9 = Rx on 
		}
		else {
			//Startimpuls_Box.Value = (CLng(Asc(Mid(strResult, 18, 1))) And &H7F) - 11  ' Wert wird in us geteilt durch 100 gespeichert
			rxAutoStartValue = (readBuffer[17] & 0x7F)-11; // 16 = 1.6 ms (value - 11 = position in RX_AUTO_START_MS)
		}
		impulseTriggerCombo.select(rxAutoStartValue);
		log.log(Level.FINE, "isAutStartRx = " + isAutStartRx + " isRxOn = " + isRxOn + " rxAutoStartValue = " + rxAutoStartValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		//Abregelung_Box.Value = CLng(Asc(Mid(strResult, 19, 1)))
		impulsReductionType = (readBuffer[18] & 0x7F);
		log.log(Level.FINE, "impulsReductionType = " + impulsReductionType); //$NON-NLS-1$
		regulationTypeCombo.select(impulsReductionType);

		isAutoStartVoltageDrop = false;
		//If CLng(Asc(Mid(strResult, 20, 1))) = 0 Then
		if ((readBuffer[19] & 0xFF) != 0) {
			isAutoStartVoltageDrop = true;
		}
		log.log(Level.FINE, "isAutoStartVoltageDrop = " + isAutoStartVoltageDrop); //$NON-NLS-1$ //$NON-NLS-2$
		voltageDropTriggerButton.setSelection(isAutoStartVoltageDrop);	
		
		//Zellentyp_Box.Value = CLng(Asc(Mid(strResult, 21, 1)))
		cellType = readBuffer[20] & 0xFF;
		log.log(Level.FINE, "cellType = " + cellType ); 
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
		if (Thread.currentThread().getId() == application.getThreadId()) {
			readConfigButton.setEnabled(true);
			storeConfigButton.setEnabled(false);

			readDataButton.setEnabled(true);
			stopReadDataButton.setEnabled(false);

			startLoggingButton.setEnabled(true);
			stopLoggingButton.setEnabled(false);

			startLiveGatherButton.setEnabled(true);
			stopLiveGatherButton.setEnabled(false);

			clearMemoryButton.setEnabled(true);
			closeButton.setEnabled(true);
			setClosePossible(true);
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					readConfigButton.setEnabled(true);
					storeConfigButton.setEnabled(false);

					readDataButton.setEnabled(true);
					stopReadDataButton.setEnabled(false);

					startLoggingButton.setEnabled(true);
					stopLoggingButton.setEnabled(false);

					startLiveGatherButton.setEnabled(true);
					stopLiveGatherButton.setEnabled(false);

					clearMemoryButton.setEnabled(true);
					closeButton.setEnabled(true);
					setClosePossible(true);
				}
			});
		}
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
				readDataProgressBar.setSelection(tmpValue);
			}
		});
	}
	
	/**
	 * update the counter number in the dialog, called out of thread
	 * @param redTelegrams
	 * @param numberRecordSet
	 * @param numReadErrors
	 * @param memoryUsed
	 */
	public void updateDataGatherProgress(final int redTelegrams, final int numberRecordSet, final int numReadErrors, final int memoryUsed) {
		this.numberRedDataSetsText = "" + redTelegrams; //$NON-NLS-1$
		this.numberActualDataSetsText = "" + numberRecordSet; //$NON-NLS-1$
		this.numberReadErrorText = "" + numReadErrors; //$NON-NLS-1$
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				int progress = redTelegrams * 100 / memoryUsed;
				int tmpValue = progress < 0 ? 0 : progress;
				tmpValue = progress > 100 ? 100 : progress;
				readDataProgressBar.setSelection(tmpValue);
				redDataSetLabel.setText(numberRedDataSetsText);
				actualDataSetNumber.setText(numberActualDataSetsText);
				numberReadErrorLabel.setText(numberReadErrorText);
			}
		});
	}

}
