/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.device.smmodellbau;

import java.util.Locale;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import osde.DE;
import osde.config.Settings;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.DeviceDialog;
import osde.device.smmodellbau.lipowatch.MessageIds;
import osde.log.Level;
import osde.messages.Messages;
import osde.ui.DataExplorer;
import osde.ui.SWTResourceManager;

/**
 * LiPoWatch device dialog class
 * @author Winfried Brügmann
 */
public class LiPoWatchDialog extends DeviceDialog {
	static final String						DEVICE_NAME									= "LiPoWatch";																																					//$NON-NLS-1$
	static final Logger						log													= Logger.getLogger(LiPoWatchDialog.class.getName());

	public final static int				MAX_DATA_RECORDS						= 25920;
	public final static int				FLASH_SIZE									= 524288;
	public final static int				FLASH_POSITION_DATA_BEGIN		= 0x100;
	public final static int				MAX_DATA_VALUES							= LiPoWatchDialog.FLASH_SIZE - 0x100;

	public final static String[]	TIME_INTERVAL								= { 
			"   1/4 s  (->     5 h)", //$NON-NLS-1$
			"   1/2 s  (->   10 h)", 	//$NON-NLS-1$
			"      1 s   (->   20 h)",//$NON-NLS-1$
			"      2 s   (->   40 h)",//$NON-NLS-1$
			"      5 s   (-> 100 h)", //$NON-NLS-1$
			"    10 s   (->  200 h)"};//$NON-NLS-1$
	public final static String[]	RX_AUTO_START_MS						= { " 1,1", " 1,2", " 1,3", " 1,4", " 1,5", " 1,6", " 1,7", " 1,8", " 1,9", " Rx on" }; //$NON-NLS-$
	public final static String[]	CELL_VOLTAGE_LIMITS					= { " 2.0", " 2.1", " 2.2", " 2.3", " 2.4", " 2.5", " 2.6", " 2.7", " 2.8", " 2.9", " 3.0", " 3.1", " 3.2", " 3.3", " 3.4", " 3.5", " 3.6", " 3.7"};	//$NON-NLS-$
	
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

	Group													impuleRegulationGroup;
	CLabel												cellTypeLabel;
	CLabel												voltageLimitLabel;
	CLabel												regulationTypeLabel;
	CCombo												cellTypeCombo;
	CCombo												voltageLevelRegulationCombo;
	CCombo												regulationTypeCombo;

	Button												storeConfigButton;
	Button												readConfigButton;

	CTabItem											displayTabItem;
	Composite											displayComposite;

	CTabItem											dataTabItem;
	Composite											dataMainComposite;
	Group													dataReadGroup;
	CLabel												clearMemoryLabel;
	Group													clearMemoryGroup;
	Button												readDataButton, stopReadDataButton;
	CLabel												dataSetLabel, redDataSetLabel, actualDataSetNumberLabel, actualDataSetNumber, readDataErrorLabel, numberReadErrorLabel, readLess4Label, numberLess4Label;
	ProgressBar										readDataProgressBar;

	Group													liveDataCaptureGroup, loggingGroup;
	Button												startLiveGatherButton, stopLiveGatherButton, startLoggingButton, stopLoggingButton;
	Button												clearMemoryButton;

	Button												closeButton;

	LiPoWatchDataGatherer					gatherThread;
	LiPoWatchLiveGatherer					liveThread;
	String												serialNumber								= "";																																											//$NON-NLS-1$
	String												lipoWatchVersion						= "";																																											//$NON-NLS-1$
	int														memoryUsed									= 0;
	String												memoryUsedPercent						= "0";																																											//$NON-NLS-1$
	int														timeIntervalPosition				= 0;
	int														measurementModus						= 0;
	boolean												isAutoStartVoltageDrop			= true;
	boolean												isAutStartRx								= false;
	boolean												isRxOn											= false;
	boolean												isAutoStartTime							= false;
	int														voltageLevelRegulationLimit	= 10;																																											//3.0 V
	int														impulsReductionType					= 1;																																												//soft
	int														cellType										= 0;																																												//LiPo
	int														rxAutoStartValue						= 0;
	int														timeAutoStart_sec						= 0;
	double												offsetA1										= 0.0;
	double												factorA1										= 1.0;

	String												numberRedDataSetsText				= "0";																																											//$NON-NLS-1$
	String												numberActualDataSetsText		= "0";																																											//$NON-NLS-1$
	String												numberReadErrorText					= "0";																																											//$NON-NLS-1$	
	String												numberLess4Text							= "0";																																											//$NON-NLS-1$	

	final LiPoWatch								device;																																																								// get device specific things, get serial port, ...
	final LiPoWatchSerialPort			serialPort;																																																						// open/close port execute getData()....
	final DataExplorer	application;																																																						// interaction with application instance
	final Channels								channels;																																																							// interaction with channels, source of all records
	final Settings								settings;																																																							// application configuration settings

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
		}
		catch (Exception e) {
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
		this.serialPort = useDevice.getSerialPort();
		this.device = useDevice;
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	@Override
	public void open() {
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue();
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			LiPoWatchDialog.log.log(Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialogShell == null || this.dialogShell.isDisposed()) {
				if (this.settings.isDeviceDialogsModal())
					this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
				else if (this.settings.isDeviceDialogsOnTop())
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
				else
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(this.dialogShell);
				if (this.isAlphaEnabled) this.dialogShell.setAlpha(254);
				this.dialogShell.setText("LiPoWatch" + Messages.getString(osde.messages.MessageIds.DE_MSGT0273)); //$NON-NLS-1$
				this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.setSize(509, 394);
				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 175, 100));
				this.dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						LiPoWatchDialog.log.log(Level.FINE, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (LiPoWatchDialog.this.storeConfigButton.getEnabled()) {
							String msg = Messages.getString(MessageIds.DE_MSGI1600);
							if (LiPoWatchDialog.this.application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								LiPoWatchDialog.log.log(Level.FINE, "SWT.YES"); //$NON-NLS-1$
								//device.storeDeviceProperties(); // only used for configurable analog input 
								setClosePossible(true);
							}
							// check threads before close
							if (LiPoWatchDialog.this.gatherThread != null && LiPoWatchDialog.this.gatherThread.isAlive()) {
								LiPoWatchDialog.this.gatherThread.interrupt();
							}
							if (LiPoWatchDialog.this.liveThread != null && LiPoWatchDialog.this.liveThread.isTimerRunning) {
								LiPoWatchDialog.this.liveThread.stopTimerThread();
							}
						}
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						LiPoWatchDialog.log.log(Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						LiPoWatchDialog.this.application.openHelpDialog("LiPoWatch", "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				// enable fade in/out alpha blending (do not fade-in on top)
				this.dialogShell.addMouseTrackListener(new MouseTrackAdapter() {
					@Override
					public void mouseEnter(MouseEvent evt) {
						LiPoWatchDialog.log.log(Level.FINER, "dialogShell.mouseEnter, event=" + evt); //$NON-NLS-1$
						fadeOutAplhaBlending(evt, getDialogShell().getClientArea(), 20, 20, 20, 25);
					}

					@Override
					public void mouseHover(MouseEvent evt) {
						LiPoWatchDialog.log.log(Level.FINEST, "dialogShell.mouseHover, event=" + evt); //$NON-NLS-1$
					}

					@Override
					public void mouseExit(MouseEvent evt) {
						LiPoWatchDialog.log.log(Level.FINER, "dialogShell.mouseExit, event=" + evt); //$NON-NLS-1$
						fadeInAlpaBlending(evt, getDialogShell().getClientArea(), 20, 20, -20, 25);
					}
				});
				{
					this.mainTabFolder = new CTabFolder(this.dialogShell, SWT.NONE);
					{
						this.configTabItem = new CTabItem(this.mainTabFolder, SWT.NONE);
						this.configTabItem.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.configTabItem.setText(Messages.getString(MessageIds.DE_MSGT1608));
						{
							this.configMainComosite = new Composite(this.mainTabFolder, SWT.NONE);
							this.configTabItem.setControl(this.configMainComosite);
							this.configMainComosite.setLayout(null);
							this.configMainComosite.setToolTipText(Messages.getString(MessageIds.DE_MSGT1609));
							this.configMainComosite.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
							{
								this.statusGroup = new Group(this.configMainComosite, SWT.NONE);
								this.statusGroup.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.statusGroup.setLayout(null);
								this.statusGroup.setText(Messages.getString(MessageIds.DE_MSGT1610));
								this.statusGroup.setBounds(12, 4, 473, 45);
								this.statusGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								{
									this.serialNumberText = new CLabel(this.statusGroup, SWT.NONE);
									this.serialNumberText.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.serialNumberText.setText(Messages.getString(MessageIds.DE_MSGT1611));
									this.serialNumberText.setBounds(6, DE.IS_MAC_COCOA ? 2 : 17, 40, 22);
									this.serialNumberText.setToolTipText(Messages.getString(MessageIds.DE_MSGT1612));
								}
								{
									this.snLabel = new CLabel(this.statusGroup, SWT.CENTER | SWT.EMBEDDED);
									this.snLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.snLabel.setBounds(46, DE.IS_MAC_COCOA ? 2 : 17, 65, 22);
									this.snLabel.setText(this.serialNumber);
								}
								{
									this.firmwareText = new CLabel(this.statusGroup, SWT.NONE);
									this.firmwareText.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.firmwareText.setText(Messages.getString(MessageIds.DE_MSGT1613));
									this.firmwareText.setBounds(115, DE.IS_MAC_COCOA ? 2 : 17, 70, 22);
									this.firmwareText.setToolTipText(Messages.getString(MessageIds.DE_MSGT1614));
								}
								{
									this.firmwareVersionLabel = new CLabel(this.statusGroup, SWT.NONE);
									this.firmwareVersionLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.firmwareVersionLabel.setBounds(185, DE.IS_MAC_COCOA ? 2 : 17, 69, 22);
									this.firmwareVersionLabel.setText(this.lipoWatchVersion);
								}
								{
									this.memoryUsageText = new CLabel(this.statusGroup, SWT.NONE);
									this.memoryUsageText.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.memoryUsageText.setText(Messages.getString(MessageIds.DE_MSGT1615));
									this.memoryUsageText.setBounds(255, DE.IS_MAC_COCOA ? 2 : 17, 124, 22);
								}
								{
									this.memUsagePercent = new CLabel(this.statusGroup, SWT.RIGHT);
									this.memUsagePercent.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.memUsagePercent.setBounds(380, DE.IS_MAC_COCOA ? 2 : 17, 59, 22);
									this.memUsagePercent.setText(this.memoryUsedPercent);
								}
								{
									this.memUsageUnit = new CLabel(this.statusGroup, SWT.NONE);
									this.memUsageUnit.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.memUsageUnit.setText(Messages.getString(MessageIds.DE_MSGT1616));
									this.memUsageUnit.setBounds(442, DE.IS_MAC_COCOA ? 2 : 17, 26, 22);
								}
							} // end status group

							{
								this.autoStartGroup = new Group(this.configMainComosite, SWT.NONE);
								this.autoStartGroup.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.autoStartGroup.setText(Messages.getString(MessageIds.DE_MSGT1617));
								this.autoStartGroup.setBounds(12, 136, 232, 107);
								this.autoStartGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								{
									this.voltageDropTriggerButton = new Button(this.autoStartGroup, SWT.CHECK | SWT.RIGHT);
									this.voltageDropTriggerButton.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.voltageDropTriggerButton.setText(Messages.getString(MessageIds.DE_MSGT1618));
									this.voltageDropTriggerButton.setToolTipText(Messages.getString(MessageIds.DE_MSGT1619));
									this.voltageDropTriggerButton.setSelection(this.isAutoStartVoltageDrop);
									this.voltageDropTriggerButton.setBounds(16, DE.IS_MAC_COCOA ? 9 : 24, 139, 20);
									this.voltageDropTriggerButton.addSelectionListener(new SelectionAdapter() {

										@Override
										public void widgetSelected(SelectionEvent evt) {
											LiPoWatchDialog.log.log(Level.FINEST, "currentTriggerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											LiPoWatchDialog.this.isAutoStartVoltageDrop = LiPoWatchDialog.this.voltageDropTriggerButton.getSelection();
											if (LiPoWatchDialog.this.isAutoStartVoltageDrop) {
												LiPoWatchDialog.this.impulseTriggerButton.setSelection(LiPoWatchDialog.this.isAutStartRx = false);
												LiPoWatchDialog.this.timeTriggerButton.setSelection(LiPoWatchDialog.this.isAutoStartTime = false);
											}
											LiPoWatchDialog.this.storeConfigButton.setEnabled(true);
										}
									});
								}
								{
									this.timeTriggerButton = new Button(this.autoStartGroup, SWT.CHECK | SWT.RIGHT);
									this.timeTriggerButton.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.timeTriggerButton.setText(Messages.getString(MessageIds.DE_MSGT1620));
									this.timeTriggerButton.setToolTipText(Messages.getString(MessageIds.DE_MSGT1621));
									this.timeTriggerButton.setSelection(this.isAutStartRx);
									this.timeTriggerButton.setBounds(16, DE.IS_MAC_COCOA ? 37 : 52, 139, 20);
									this.timeTriggerButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											LiPoWatchDialog.log.log(Level.FINEST, "timeTriggerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											LiPoWatchDialog.this.isAutoStartTime = LiPoWatchDialog.this.timeTriggerButton.getSelection();
											if (LiPoWatchDialog.this.isAutoStartTime) {
												LiPoWatchDialog.this.impulseTriggerButton.setSelection(LiPoWatchDialog.this.isAutStartRx = false);
												LiPoWatchDialog.this.voltageDropTriggerButton.setSelection(LiPoWatchDialog.this.isAutoStartVoltageDrop = false);
											}
											LiPoWatchDialog.this.storeConfigButton.setEnabled(true);
										}
									});
								}
								{
									this.timeTriggerCombo = new CCombo(this.autoStartGroup, SWT.BORDER);
									this.timeTriggerCombo.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.timeTriggerCombo.setBounds(159, DE.IS_MAC_COCOA ? 37 : 52, 59, DE.IS_LINUX ? 22 : 20);
									this.timeTriggerCombo.setItems(new String[] { " 15", " 20", " 25", " 30", " 60", " 90" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
									this.timeTriggerCombo.select(3);
									this.timeTriggerCombo.setEditable(true);
									this.timeTriggerCombo.setBackground(DataExplorer.COLOR_WHITE);
									this.timeTriggerCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											LiPoWatchDialog.log.log(Level.FINEST, "timeTriggerCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											LiPoWatchDialog.this.timeAutoStart_sec = Integer.parseInt(LiPoWatchDialog.this.timeTriggerCombo.getText().trim());
											LiPoWatchDialog.this.storeConfigButton.setEnabled(true);
										}
									});
									this.timeTriggerCombo.addKeyListener(new KeyAdapter() {
										@Override
										public void keyReleased(KeyEvent evt) {
											LiPoWatchDialog.log.log(Level.FINEST, "timeTriggerCombo.keyReleased, event=" + evt); //$NON-NLS-1$
											if (evt.keyCode == SWT.CR) {
												int value = Integer.parseInt(LiPoWatchDialog.this.timeTriggerCombo.getText().trim());
												if (value < 15)
													LiPoWatchDialog.this.timeTriggerCombo.setText(" " + (value = 15)); //$NON-NLS-1$
												else if (value > 90) LiPoWatchDialog.this.timeTriggerCombo.setText(" " + (value = 90)); //$NON-NLS-1$

												int timeSelect = 0;
												for (; timeSelect < LiPoWatchDialog.this.timeTriggerCombo.getItemCount(); ++timeSelect) {
													if (value <= Integer.parseInt(LiPoWatchDialog.this.timeTriggerCombo.getItems()[timeSelect].trim())) break;
												}
												LiPoWatchDialog.this.timeTriggerCombo.select(timeSelect);
												LiPoWatchDialog.this.storeConfigButton.setEnabled(true);
											}
										}
									});
								}
								{
									this.impulseTriggerButton = new Button(this.autoStartGroup, SWT.CHECK | SWT.RIGHT);
									this.impulseTriggerButton.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.impulseTriggerButton.setText(Messages.getString(MessageIds.DE_MSGT1622));
									this.impulseTriggerButton.setToolTipText(Messages.getString(MessageIds.DE_MSGT1623));
									this.impulseTriggerButton.setSelection(this.isAutoStartTime);
									this.impulseTriggerButton.setBounds(16, DE.IS_MAC_COCOA ? 63 : 78, 139, 20);
									this.impulseTriggerButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											LiPoWatchDialog.log.log(Level.FINEST, "impulseTriggerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											LiPoWatchDialog.this.isAutStartRx = LiPoWatchDialog.this.impulseTriggerButton.getSelection();
											if (LiPoWatchDialog.this.isAutStartRx) {
												LiPoWatchDialog.this.timeTriggerButton.setSelection(LiPoWatchDialog.this.isAutoStartTime = false);
												LiPoWatchDialog.this.voltageDropTriggerButton.setSelection(LiPoWatchDialog.this.isAutoStartVoltageDrop = false);
											}
											LiPoWatchDialog.this.storeConfigButton.setEnabled(true);
										}
									});
								}
								{
									this.impulseTriggerCombo = new CCombo(this.autoStartGroup, SWT.BORDER);
									this.impulseTriggerCombo.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.impulseTriggerCombo.setBounds(159, DE.IS_MAC_COCOA ? 63 : 78, 59, DE.IS_LINUX ? 22 : 20);
									this.impulseTriggerCombo.setEditable(false);
									this.impulseTriggerCombo.setBackground(DataExplorer.COLOR_WHITE);
									this.impulseTriggerCombo.setItems(LiPoWatchDialog.RX_AUTO_START_MS);
									this.impulseTriggerCombo.select(4);
									this.impulseTriggerCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											LiPoWatchDialog.log.log(Level.FINEST, "rcTriggerCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											LiPoWatchDialog.this.rxAutoStartValue = LiPoWatchDialog.this.impulseTriggerCombo.getSelectionIndex() + 11;
											LiPoWatchDialog.this.storeConfigButton.setEnabled(true);
										}
									});
								}
							} // end autoStartGroup
							{
								this.dataRateGroup = new Group(this.configMainComosite, SWT.NONE);
								this.dataRateGroup.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.dataRateGroup.setText(Messages.getString(MessageIds.DE_MSGT1624));
								this.dataRateGroup.setBounds(14, 89, 232, 45);
								this.dataRateGroup.setToolTipText(Messages.getString(MessageIds.DE_MSGT1625));
								this.dataRateGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								{
									this.timeIntervalCombo = new CCombo(this.dataRateGroup, SWT.BORDER);
									this.timeIntervalCombo.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.timeIntervalCombo.setItems(LiPoWatchDialog.TIME_INTERVAL);
									this.timeIntervalCombo.setBounds(49, DE.IS_MAC_COCOA ? 2 : 17, 133, DE.IS_LINUX ? 22 : 20);
									this.timeIntervalCombo.select(1);
									this.timeIntervalCombo.setEditable(false);
									this.timeIntervalCombo.setBackground(DataExplorer.COLOR_WHITE);
									this.timeIntervalCombo.setToolTipText(Messages.getString(MessageIds.DE_MSGT1625));
									this.timeIntervalCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											LiPoWatchDialog.log.log(Level.FINEST, "timeRateCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											LiPoWatchDialog.this.timeIntervalPosition = LiPoWatchDialog.this.timeIntervalCombo.getSelectionIndex();
											LiPoWatchDialog.this.storeConfigButton.setEnabled(true);
										}
									});
								}
							} // end dataRateGroup
							{
								this.readConfigButton = new Button(this.configMainComosite, SWT.PUSH | SWT.CENTER);
								this.readConfigButton.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.readConfigButton.setText(Messages.getString(MessageIds.DE_MSGT1627));
								this.readConfigButton.setBounds(135, 56, 232, 30);
								this.readConfigButton.setToolTipText(Messages.getString(MessageIds.DE_MSGT1628));
								this.readConfigButton.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								this.readConfigButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										LiPoWatchDialog.log.log(Level.FINEST, "readConfigButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										try {
											updateConfigurationValues(LiPoWatchDialog.this.serialPort.readConfiguration());
										}
										catch (Exception e) {
											LiPoWatchDialog.this.application.openMessageDialog(LiPoWatchDialog.this.getDialogShell(), Messages.getString(osde.messages.MessageIds.DE_MSGE0029, new Object[] {
													e.getClass().getSimpleName(), e.getMessage() }));
										}
									}
								});
							}
							{
								this.storeConfigButton = new Button(this.configMainComosite, SWT.PUSH | SWT.CENTER);
								this.storeConfigButton.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.storeConfigButton.setText(Messages.getString(MessageIds.DE_MSGT1629));
								this.storeConfigButton.setBounds(135, 249, 229, 30);
								this.storeConfigButton.setToolTipText(Messages.getString(MessageIds.DE_MSGT1630));
								this.storeConfigButton.setEnabled(false);
								this.storeConfigButton.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								this.storeConfigButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										LiPoWatchDialog.log.log(Level.FINEST, "storeConfigButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										try {
											if (LiPoWatchDialog.this.serialPort.setConfiguration(buildUpdateBuffer())) {
												updateTimeStep_ms(LiPoWatchDialog.this.timeIntervalPosition);
												LiPoWatchDialog.this.storeConfigButton.setEnabled(false);
											}
											else {
												LiPoWatchDialog.this.storeConfigButton.setEnabled(true);
											}
										}
										catch (Exception e) {
											LiPoWatchDialog.this.application.openMessageDialog(getDialogShell(), e.getMessage());
										}
									}
								});
							}
							{
								this.impuleRegulationGroup = new Group(this.configMainComosite, SWT.NONE);
								this.impuleRegulationGroup.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.impuleRegulationGroup.setLayout(null);
								this.impuleRegulationGroup.setText(Messages.getString(MessageIds.DE_MSGT1631));
								this.impuleRegulationGroup.setBounds(252, 136, 232, 107);
								this.impuleRegulationGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								{
									this.regulationTypeLabel = new CLabel(this.impuleRegulationGroup, SWT.RIGHT);
									this.regulationTypeLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.regulationTypeLabel.setText(Messages.getString(MessageIds.DE_MSGT1632));
									this.regulationTypeLabel.setBounds(14, DE.IS_MAC_COCOA ? 6 : 21, 110, 20);
									this.regulationTypeLabel.setToolTipText(Messages.getString(MessageIds.DE_MSGT1633));
								}
								{
									this.voltageLimitLabel = new CLabel(this.impuleRegulationGroup, SWT.RIGHT);
									this.voltageLimitLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.voltageLimitLabel.setText(Messages.getString(MessageIds.DE_MSGT1634));
									this.voltageLimitLabel.setBounds(14, DE.IS_MAC_COCOA ? 34 : 49, 110, 20);
									this.voltageLimitLabel.setToolTipText(Messages.getString(MessageIds.DE_MSGT1635));
								}
								{
									this.cellTypeLabel = new CLabel(this.impuleRegulationGroup, SWT.RIGHT);
									this.cellTypeLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.cellTypeLabel.setText(Messages.getString(MessageIds.DE_MSGT1636));
									this.cellTypeLabel.setBounds(14, DE.IS_MAC_COCOA ? 60 : 75, 110, 20);
								}
								{
									this.regulationTypeCombo = new CCombo(this.impuleRegulationGroup, SWT.BORDER | SWT.CENTER);
									this.regulationTypeCombo.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.regulationTypeCombo.setItems(Messages.getString(MessageIds.DE_MSGT1637).split(DE.STRING_SEMICOLON));
									this.regulationTypeCombo.setBounds(134, DE.IS_MAC_COCOA ? 7 : 23, 69, DE.IS_LINUX ? 22 : 20);
									this.regulationTypeCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											LiPoWatchDialog.log.log(Level.FINEST, "regulationTypeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											LiPoWatchDialog.this.impulsReductionType = LiPoWatchDialog.this.regulationTypeCombo.getSelectionIndex();
											LiPoWatchDialog.this.storeConfigButton.setEnabled(true);
										}
									});
								}
								{
									this.voltageLevelRegulationCombo = new CCombo(this.impuleRegulationGroup, SWT.BORDER);
									this.voltageLevelRegulationCombo.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.voltageLevelRegulationCombo.setItems(LiPoWatchDialog.CELL_VOLTAGE_LIMITS);
									this.voltageLevelRegulationCombo.setBounds(134, DE.IS_MAC_COCOA ? 36 : 51, 69, DE.IS_LINUX ? 22 : 20);
									this.voltageLevelRegulationCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											LiPoWatchDialog.log.log(Level.FINEST, "voltageLimitCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											LiPoWatchDialog.this.voltageLevelRegulationLimit = LiPoWatchDialog.this.voltageLevelRegulationCombo.getSelectionIndex();
											LiPoWatchDialog.this.storeConfigButton.setEnabled(true);
										}
									});
								}
								{
									this.cellTypeCombo = new CCombo(this.impuleRegulationGroup, SWT.BORDER);
									this.cellTypeCombo.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.cellTypeCombo.setBounds(134, DE.IS_MAC_COCOA ? 62 : 77, 69, DE.IS_LINUX ? 22 : 20);
									this.cellTypeCombo.setItems(Messages.getString(MessageIds.DE_MSGT1640).split(DE.STRING_SEMICOLON));
									this.cellTypeCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											LiPoWatchDialog.log.log(Level.FINEST, "cellTypeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											LiPoWatchDialog.this.cellType = LiPoWatchDialog.this.cellTypeCombo.getSelectionIndex();
											LiPoWatchDialog.this.storeConfigButton.setEnabled(true);
										}
									});
								}
							} // end impuleRegulationGroup
							{
								this.measurementTypeGroup = new Group(this.configMainComosite, SWT.NONE);
								this.measurementTypeGroup.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.measurementTypeGroup.setLayout(null);
								this.measurementTypeGroup.setText(Messages.getString(MessageIds.DE_MSGT1642));
								this.measurementTypeGroup.setBounds(252, 89, 232, 45);
								this.measurementTypeGroup.setToolTipText(Messages.getString(MessageIds.DE_MSGT1643));
								this.measurementTypeGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								{
									this.measurementModusLabel = new CLabel(this.measurementTypeGroup, SWT.RIGHT);
									this.measurementModusLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.measurementModusLabel.setText(Messages.getString(MessageIds.DE_MSGT1644));
									this.measurementModusLabel.setBounds(14, DE.IS_MAC_COCOA ? 2 : 17, 72, 20);
									this.measurementModusLabel.setToolTipText(Messages.getString(MessageIds.DE_MSGT1643));
								}
								{
									this.measurementModusCombo = new CCombo(this.measurementTypeGroup, SWT.BORDER);
									this.measurementModusCombo.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.measurementModusCombo.setBounds(96, DE.IS_MAC_COCOA ? 2 : 17, 104, DE.IS_LINUX ? 22 : 20);
									this.measurementModusCombo.setItems(Messages.getString(MessageIds.DE_MSGT1646).split(DE.STRING_SEMICOLON));
									this.measurementModusCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											LiPoWatchDialog.log.log(Level.FINEST, "measurementTypeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											LiPoWatchDialog.this.measurementModus = LiPoWatchDialog.this.measurementModusCombo.getSelectionIndex();
											LiPoWatchDialog.this.storeConfigButton.setEnabled(true);
										}
									});
								}
							}
						}
						{
							this.dataTabItem = new CTabItem(this.mainTabFolder, SWT.NONE);
							this.dataTabItem.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.dataTabItem.setText(Messages.getString(MessageIds.DE_MSGT1648));
							this.dataTabItem.setToolTipText(Messages.getString(MessageIds.DE_MSGT1649));
							{
								this.dataMainComposite = new Composite(this.mainTabFolder, SWT.NONE);
								this.dataMainComposite.setLayout(null);
								this.dataTabItem.setControl(this.dataMainComposite);
								this.dataMainComposite.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
								{
									this.dataReadGroup = new Group(this.dataMainComposite, SWT.NONE);
									this.dataReadGroup.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.dataReadGroup.setBounds(12, 12, 241, 263);
									this.dataReadGroup.setText(Messages.getString(MessageIds.DE_MSGT1650));
									this.dataReadGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
									{
										this.readDataButton = new Button(this.dataReadGroup, SWT.PUSH | SWT.CENTER);
										this.readDataButton.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.readDataButton.setText(Messages.getString(MessageIds.DE_MSGT1651));
										this.readDataButton.setBounds(11, 24, 218, 30);
										this.readDataButton.setEnabled(true);
										this.readDataButton.setToolTipText(Messages.getString(MessageIds.DE_MSGT1651));
										this.readDataButton.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												LiPoWatchDialog.log.log(Level.FINEST, "readDataButton.widgetSelected, event=" + evt); //$NON-NLS-1$
												LiPoWatchDialog.this.gatherThread = new LiPoWatchDataGatherer(LiPoWatchDialog.this.application, LiPoWatchDialog.this.device, LiPoWatchDialog.this.serialPort);
												try {
													LiPoWatchDialog.this.gatherThread.start();
												}
												catch (RuntimeException e) {
													LiPoWatchDialog.log.log(Level.WARNING, e.getMessage(), e);
												}
												setClosePossible(false);
												LiPoWatchDialog.this.readConfigButton.setEnabled(false);
												LiPoWatchDialog.this.storeConfigButton.setEnabled(false);
												LiPoWatchDialog.this.readDataButton.setEnabled(false);
												LiPoWatchDialog.this.stopReadDataButton.setEnabled(true);
												LiPoWatchDialog.this.startLoggingButton.setEnabled(false);
												LiPoWatchDialog.this.stopLoggingButton.setEnabled(false);
												LiPoWatchDialog.this.startLiveGatherButton.setEnabled(false);
												LiPoWatchDialog.this.clearMemoryButton.setEnabled(false);
												LiPoWatchDialog.this.closeButton.setEnabled(false);
											}
										});
									}
									{
										this.dataSetLabel = new CLabel(this.dataReadGroup, SWT.RIGHT);
										this.dataSetLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.dataSetLabel.setBounds(5, DE.IS_MAC_COCOA ? 59 : 74, 165, 20);
										this.dataSetLabel.setText(Messages.getString(MessageIds.DE_MSGT1653));
									}
									{
										this.redDataSetLabel = new CLabel(this.dataReadGroup, SWT.RIGHT);
										this.redDataSetLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.redDataSetLabel.setBounds(175, DE.IS_MAC_COCOA ? 59 : 74, 55, 20);
										this.redDataSetLabel.setText("0"); //$NON-NLS-1$
									}
									{
										this.actualDataSetNumberLabel = new CLabel(this.dataReadGroup, SWT.RIGHT);
										this.actualDataSetNumberLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.actualDataSetNumberLabel.setBounds(5, DE.IS_MAC_COCOA ? 81 : 96, 190, 20);
										this.actualDataSetNumberLabel.setText(Messages.getString(MessageIds.DE_MSGT1654));
									}
									{
										this.actualDataSetNumber = new CLabel(this.dataReadGroup, SWT.RIGHT);
										this.actualDataSetNumber.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.actualDataSetNumber.setBounds(200, DE.IS_MAC_COCOA ? 81 : 96, 30, 20);
										this.actualDataSetNumber.setText("0"); //$NON-NLS-1$
									}
									{
										this.readDataErrorLabel = new CLabel(this.dataReadGroup, SWT.RIGHT);
										this.readDataErrorLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.readDataErrorLabel.setBounds(5, DE.IS_MAC_COCOA ? 103 : 118, 190, 20);
										this.readDataErrorLabel.setText(Messages.getString(MessageIds.DE_MSGT1655));
									}
									{
										this.numberReadErrorLabel = new CLabel(this.dataReadGroup, SWT.RIGHT);
										this.numberReadErrorLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.numberReadErrorLabel.setBounds(200, DE.IS_MAC_COCOA ? 103 : 118, 30, 20);
										this.numberReadErrorLabel.setText("0"); //$NON-NLS-1$
									}
									{
										this.readLess4Label = new CLabel(this.dataReadGroup, SWT.RIGHT);
										this.readLess4Label.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.readLess4Label.setBounds(5, DE.IS_MAC_COCOA ? 125 : 140, 190, 20);
										this.readLess4Label.setText(Messages.getString(MessageIds.DE_MSGT1659));
									}
									{
										this.numberLess4Label = new CLabel(this.dataReadGroup, SWT.RIGHT);
										this.numberLess4Label.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.numberLess4Label.setBounds(200, DE.IS_MAC_COCOA ? 125 : 140, 30, 20);
										this.numberLess4Label.setText("0"); //$NON-NLS-1$
									}
									{
										this.readDataProgressBar = new ProgressBar(this.dataReadGroup, SWT.NONE);
										this.readDataProgressBar.setBounds(11, DE.IS_MAC_COCOA ? 169 : 183, 218, 20);
										this.readDataProgressBar.setMinimum(0);
										this.readDataProgressBar.setMaximum(100);
									}
									{
										this.stopReadDataButton = new Button(this.dataReadGroup, SWT.PUSH | SWT.CENTER);
										this.stopReadDataButton.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.stopReadDataButton.setBounds(11, DE.IS_MAC_COCOA ? 207 : 222, 218, 30);
										this.stopReadDataButton.setText(Messages.getString(osde.messages.MessageIds.DE_MSGT0278));
										this.stopReadDataButton.setEnabled(false);
										this.stopReadDataButton.setToolTipText(Messages.getString(MessageIds.DE_MSGT1656));
										this.stopReadDataButton.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												LiPoWatchDialog.log.log(Level.FINE, "stopDataButton.widgetSelected, event=" + evt); //$NON-NLS-1$
												if (LiPoWatchDialog.this.gatherThread != null && LiPoWatchDialog.this.gatherThread.isAlive()) {
													LiPoWatchDialog.this.gatherThread.setThreadStop(); // end serial communication
												}
												resetButtons();
											}
										});
									}
								}
								{
									this.liveDataCaptureGroup = new Group(this.dataMainComposite, SWT.NONE);
									this.liveDataCaptureGroup.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.liveDataCaptureGroup.setBounds(259, 12, 228, 164);
									this.liveDataCaptureGroup.setText(Messages.getString(MessageIds.DE_MSGT1657));
									this.liveDataCaptureGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
									{
										this.startLiveGatherButton = new Button(this.liveDataCaptureGroup, SWT.PUSH | SWT.CENTER);
										this.startLiveGatherButton.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.startLiveGatherButton.setText(Messages.getString(MessageIds.DE_MSGT1658));
										this.startLiveGatherButton.setBounds(12, DE.IS_MAC_COCOA ? 9 : 24, 202, 30);
										this.startLiveGatherButton.setEnabled(true);
										this.startLiveGatherButton.setToolTipText(Messages.getString(MessageIds.DE_MSGT1658));
										this.startLiveGatherButton.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												LiPoWatchDialog.log.log(Level.FINE, "liveViewButton.widgetSelected, event=" + evt); //$NON-NLS-1$
												try {
													LiPoWatchDialog.this.readConfigButton.setEnabled(false);
													LiPoWatchDialog.this.storeConfigButton.setEnabled(false);
													LiPoWatchDialog.this.startLiveGatherButton.setEnabled(false);
													LiPoWatchDialog.this.readDataButton.setEnabled(false);
													LiPoWatchDialog.this.stopReadDataButton.setEnabled(false);
													LiPoWatchDialog.this.stopLiveGatherButton.setEnabled(true);
													LiPoWatchDialog.this.clearMemoryButton.setEnabled(false);
													LiPoWatchDialog.this.closeButton.setEnabled(false);
													setClosePossible(false);
													LiPoWatchDialog.this.liveThread = new LiPoWatchLiveGatherer(LiPoWatchDialog.this.application, LiPoWatchDialog.this.device, LiPoWatchDialog.this.serialPort,
															LiPoWatchDialog.this);
													try {
														LiPoWatchDialog.this.liveThread.start();
													}
													catch (RuntimeException e) {
														LiPoWatchDialog.log.log(Level.WARNING, e.getMessage(), e);
													}
												}
												catch (Exception e) {
													if (LiPoWatchDialog.this.liveThread != null && LiPoWatchDialog.this.liveThread.isTimerRunning) {
														LiPoWatchDialog.this.liveThread.stopTimerThread();
														LiPoWatchDialog.this.liveThread.interrupt();
													}
													LiPoWatchDialog.this.application.updateGraphicsWindow();
													LiPoWatchDialog.this.application.openMessageDialog(getDialogShell(), e.getClass().getSimpleName() + e.getMessage());
													resetButtons();
												}
											}
										});
										{
											this.loggingGroup = new Group(this.liveDataCaptureGroup, SWT.NONE);
											this.loggingGroup.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
											this.loggingGroup.setBounds(12, 56, 202, 63);
											this.loggingGroup.setText(Messages.getString(MessageIds.DE_MSGT1660));
											this.loggingGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
											{
												this.startLoggingButton = new Button(this.loggingGroup, SWT.PUSH | SWT.CENTER);
												this.startLoggingButton.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
												this.startLoggingButton.setText(Messages.getString(MessageIds.DE_MSGT1661));
												this.startLoggingButton.setBounds(12, DE.IS_MAC_COCOA ? 6 : 21, 70, 30);
												this.startLoggingButton.setEnabled(true);
												this.startLoggingButton.setToolTipText(Messages.getString(MessageIds.DE_MSGT1662));
												this.startLoggingButton.addSelectionListener(new SelectionAdapter() {
													@Override
													public void widgetSelected(SelectionEvent evt) {
														LiPoWatchDialog.log.log(Level.FINE, "startLoggingButton.widgetSelected, event=" + evt); //$NON-NLS-1$
														try {
															setClosePossible(false);
															LiPoWatchDialog.this.serialPort.startLogging();
															LiPoWatchDialog.this.startLoggingButton.setEnabled(false);
															LiPoWatchDialog.this.stopLoggingButton.setEnabled(true);
														}
														catch (Exception e) {
															LiPoWatchDialog.log.log(Level.SEVERE, e.getMessage(), e);
															LiPoWatchDialog.this.application.openMessageDialog(getDialogShell(), Messages.getString(osde.messages.MessageIds.DE_MSGE0029, new Object[] {
																	e.getClass().getSimpleName(), e.getMessage() }));
														}
													}
												});
											}
											{
												this.stopLoggingButton = new Button(this.loggingGroup, SWT.PUSH | SWT.CENTER);
												this.stopLoggingButton.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
												this.stopLoggingButton.setText(Messages.getString(MessageIds.DE_MSGT1663));
												this.stopLoggingButton.setBounds(94, DE.IS_MAC_COCOA ? 6 : 21, 82, 30);
												this.stopLoggingButton.setEnabled(false);
												this.stopLoggingButton.setToolTipText(Messages.getString(MessageIds.DE_MSGT1664));
												this.stopLoggingButton.addSelectionListener(new SelectionAdapter() {
													@Override
													public void widgetSelected(SelectionEvent evt) {
														LiPoWatchDialog.log.log(Level.FINE, "stopLoggingButton.widgetSelected, event=" + evt); //$NON-NLS-1$
														try {
															LiPoWatchDialog.this.serialPort.stopLogging();
															LiPoWatchDialog.this.startLoggingButton.setEnabled(true);
															LiPoWatchDialog.this.stopLoggingButton.setEnabled(false);
															setClosePossible(true);
															if (!LiPoWatchDialog.this.stopLiveGatherButton.getEnabled()) {
																LiPoWatchDialog.this.readDataButton.setEnabled(true);
															}
														}
														catch (Exception e) {
															LiPoWatchDialog.log.log(Level.SEVERE, e.getMessage(), e);
															LiPoWatchDialog.this.application.openMessageDialog(getDialogShell(), Messages.getString(osde.messages.MessageIds.DE_MSGE0029, new Object[] {
																	e.getClass().getSimpleName(), e.getMessage() }));
														}
													}
												});
											}
										}
									}
									{
										this.stopLiveGatherButton = new Button(this.liveDataCaptureGroup, SWT.PUSH | SWT.CENTER);
										this.stopLiveGatherButton.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.stopLiveGatherButton.setBounds(12, DE.IS_MAC_COCOA ? 111 : 126, 202, 30);
										this.stopLiveGatherButton.setText(Messages.getString(MessageIds.DE_MSGT1665));
										this.stopLiveGatherButton.setEnabled(false);
										this.stopLiveGatherButton.setToolTipText(Messages.getString(MessageIds.DE_MSGT1666));
										this.stopLiveGatherButton.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												LiPoWatchDialog.log.log(Level.FINE, "stopLiveGatherButton.widgetSelected, event=" + evt); //$NON-NLS-1$
												if (LiPoWatchDialog.this.liveThread != null) {
													LiPoWatchDialog.this.liveThread.stopTimerThread();
													LiPoWatchDialog.this.liveThread.interrupt();

													if (Channels.getInstance().getActiveChannel() != null) {
														RecordSet activeRecordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
														// active record set name == live gatherer record name
														LiPoWatchDialog.this.liveThread.finalizeRecordSet(activeRecordSet.getName());
													}
												}
												LiPoWatchDialog.this.readConfigButton.setEnabled(true);
												LiPoWatchDialog.this.storeConfigButton.setEnabled(false);
												LiPoWatchDialog.this.readDataButton.setEnabled(true);
												LiPoWatchDialog.this.stopReadDataButton.setEnabled(false);
												LiPoWatchDialog.this.startLiveGatherButton.setEnabled(true);
												LiPoWatchDialog.this.stopLiveGatherButton.setEnabled(false);
												LiPoWatchDialog.this.clearMemoryButton.setEnabled(true);
												LiPoWatchDialog.this.closeButton.setEnabled(true);
												setClosePossible(true);
												if (!LiPoWatchDialog.this.stopLoggingButton.getEnabled()) {
													LiPoWatchDialog.this.readDataButton.setEnabled(true);
												}
											}
										});
									}
								}
								{
									this.clearMemoryGroup = new Group(this.dataMainComposite, SWT.NONE);
									this.clearMemoryGroup.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.clearMemoryGroup.setLayout(null);
									this.clearMemoryGroup.setText(Messages.getString(MessageIds.DE_MSGT1667));
									this.clearMemoryGroup.setBounds(261, 178, 226, 97);
									this.clearMemoryGroup.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
									{
										this.clearMemoryButton = new Button(this.clearMemoryGroup, SWT.PUSH | SWT.CENTER);
										this.clearMemoryButton.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.clearMemoryButton.setText(Messages.getString(MessageIds.DE_MSGT1668));
										this.clearMemoryButton.setBounds(12, DE.IS_MAC_COCOA ? 41 : 56, 202, 31);
										this.clearMemoryButton.setToolTipText(Messages.getString(MessageIds.DE_MSGT1603));
										this.clearMemoryButton.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												LiPoWatchDialog.log.log(Level.FINEST, "deleteMemoryButton.widgetSelected, event=" + evt); //$NON-NLS-1$
												try {
													LiPoWatchDialog.this.clearMemoryButton.setEnabled(false);
													LiPoWatchDialog.this.serialPort.clearMemory();
												}
												catch (Exception e) {
													LiPoWatchDialog.log.log(Level.SEVERE, e.getMessage(), e);
													LiPoWatchDialog.this.application.openMessageDialog(getDialogShell(), Messages.getString(MessageIds.DE_MSGE1600,
															new Object[] { e.getClass().getSimpleName(), e.getMessage() }));
													e.printStackTrace();
												}
												LiPoWatchDialog.this.clearMemoryButton.setEnabled(true);
											}
										});
									}
									{
										this.clearMemoryLabel = new CLabel(this.clearMemoryGroup, SWT.CENTER | SWT.EMBEDDED);
										this.clearMemoryLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.clearMemoryLabel.setText(Messages.getString(MessageIds.DE_MSGT1604));
										this.clearMemoryLabel.setBounds(12, DE.IS_MAC_COCOA ? 1 : 16, 202, 40);
									}
								}

							}
						}
					}

					this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					this.closeButton.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.closeButton.setText(Messages.getString(MessageIds.DE_MSGT1605));
					this.closeButton.setBounds(110, 318, 280, 31);
					this.closeButton.setToolTipText(Messages.getString(MessageIds.DE_MSGT1606));
					this.closeButton.addMouseTrackListener(this.mouseTrackerEnterFadeOut);
					this.closeButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							LiPoWatchDialog.log.log(Level.FINE, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							dispose();
						}
					});

					this.mainTabFolder.setBounds(0, 0, 501, 312);
					this.mainTabFolder.setSelection(0);
					
					initialize();
				}
				this.dialogShell.open();
			}
			else {
				this.dialogShell.setVisible(true);
				this.dialogShell.setActive();
			}
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			LiPoWatchDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	//	/**
	//	 * analog connection properties to be adjusted manually if required
	//	 * updates the analog record descriptors according input fields
	//	 * attention: set new record name replaces the record, setName() must the last operation in sequence
	//	 */
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


		//status field
		this.memoryUsed = ((readBuffer[8] & 0xFF) << 24) + ((readBuffer[7] & 0xFF) << 16) + ((readBuffer[6] & 0xFF) << 8) + (readBuffer[5] & 0xFF);
		LiPoWatchDialog.log.log(Level.FINE, "memoryUsed = " + this.memoryUsed); //$NON-NLS-1$

		//serial number
		this.serialNumber = "" + (((readBuffer[10] & 0xFF) << 8) + (readBuffer[9] & 0xFF)); //$NON-NLS-1$
		LiPoWatchDialog.log.log(Level.FINE, "serialNumber = " + this.serialNumber); //$NON-NLS-1$
		this.snLabel.setText(this.serialNumber);

		//firmware version
		this.lipoWatchVersion = String.format(Locale.ENGLISH, "v%.2f", (readBuffer[11] & 0xFF) / 100.0); //$NON-NLS-1$
		LiPoWatchDialog.log.log(Level.FINE, "unilogVersion = " + this.lipoWatchVersion); //$NON-NLS-1$
		this.firmwareVersionLabel.setText(this.lipoWatchVersion);

		//memory delete flag
		int memoryDeleted = readBuffer[12] & 0xFF;
		int tmpMemoryUsed = 0;
		if (memoryDeleted > 0)
			tmpMemoryUsed = 0;
		else
			tmpMemoryUsed = this.memoryUsed;
		this.memoryUsedPercent = String.format("%.2f", tmpMemoryUsed * 100.0 / LiPoWatchDialog.MAX_DATA_VALUES); //$NON-NLS-1$
		LiPoWatchDialog.log.log(Level.FINE, "memoryUsedPercent = " + this.memoryUsedPercent + " (" + tmpMemoryUsed + "/" + LiPoWatchDialog.MAX_DATA_RECORDS + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		this.memUsagePercent.setText(this.memoryUsedPercent);

		// timer interval
		this.timeIntervalPosition = readBuffer[13] & 0xFF;
		LiPoWatchDialog.log.log(Level.FINE, "timeIntervalPosition = " + this.timeIntervalPosition); //$NON-NLS-1$
		this.timeIntervalCombo.select(this.timeIntervalPosition);
		updateTimeStep_ms(this.timeIntervalPosition);

		// voltage modus absolute/relative
		this.measurementModus = readBuffer[14] & 0xFF;
		LiPoWatchDialog.log.log(Level.FINE, "measurementModus(relative, absolute) = " + this.measurementModus); //$NON-NLS-1$
		this.measurementModusCombo.select(this.measurementModus);

		// auto start time
		this.isAutoStartTime = false;
		this.timeAutoStart_sec = 0;
		if ((readBuffer[15] & 0x80) != 0) {
			this.isAutoStartTime = true;
		}
		this.timeAutoStart_sec = readBuffer[15] & 0x7F;
		LiPoWatchDialog.log.log(Level.FINE, "isAutoStartTime = " + this.isAutoStartTime + " timeAutoStart_sec = " + this.timeAutoStart_sec); //$NON-NLS-1$ //$NON-NLS-2$
		this.timeTriggerButton.setSelection(this.isAutoStartTime);
		int timeSelect = 0;
		for (; timeSelect < this.timeTriggerCombo.getItemCount(); ++timeSelect) {
			if (this.timeAutoStart_sec >= Integer.parseInt(this.timeTriggerCombo.getItems()[timeSelect].trim())) break;
		}
		this.timeTriggerCombo.select(timeSelect);
		this.timeTriggerCombo.setText(String.format("%4s", this.timeAutoStart_sec)); //$NON-NLS-1$

		// auto start voltage limit
		this.voltageLevelRegulationLimit = (readBuffer[16] & 0xFF) - 20;
		LiPoWatchDialog.log.log(Level.FINE, "voltageLevelRegulationLimit = " + this.voltageLevelRegulationLimit); //$NON-NLS-1$
		this.voltageLevelRegulationCombo.select(this.voltageLevelRegulationLimit);

		// auto start rx signal
		this.isAutStartRx = (readBuffer[17] & 0x80) != 0;
		this.impulseTriggerButton.setSelection(this.isAutStartRx);

		
		this.isRxOn = (readBuffer[17] & 0x7F) == 0;
		if (this.isRxOn) {// auto start rx impulse "Rx on"
			this.rxAutoStartValue = (readBuffer[17] & 0x7F) + 9; // 9 = Rx on 
		}
		else {// auto start rx impulse length	
			this.rxAutoStartValue = (readBuffer[17] & 0x7F) - 11; // 16 = 1.6 ms 
		}
		this.impulseTriggerCombo.select(this.rxAutoStartValue);
		LiPoWatchDialog.log.log(Level.FINE, "isAutStartRx = " + this.isAutStartRx + " isRxOn = " + this.isRxOn + " rxAutoStartValue = " + this.rxAutoStartValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// auto start voltage drop
		this.impulsReductionType = (readBuffer[18] & 0x7F);
		LiPoWatchDialog.log.log(Level.FINE, "impulsReductionType = " + this.impulsReductionType); //$NON-NLS-1$
		this.regulationTypeCombo.select(this.impulsReductionType);
		this.isAutoStartVoltageDrop = false;
		if ((readBuffer[19] & 0xFF) != 0) {
			this.isAutoStartVoltageDrop = true;
		}
		LiPoWatchDialog.log.log(Level.FINE, "isAutoStartVoltageDrop = " + this.isAutoStartVoltageDrop); //$NON-NLS-1$ 
		this.voltageDropTriggerButton.setSelection(this.isAutoStartVoltageDrop);

		// cell type
		this.cellType = readBuffer[20] & 0xFF;
		LiPoWatchDialog.log.log(Level.FINE, "cellType = " + this.cellType); //$NON-NLS-1$
		this.cellTypeCombo.select(this.cellType);

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

		updateBuffer[13] = (byte) this.measurementModusCombo.getSelectionIndex(); // measurement modus, relative, absolute
		checkSum = checkSum + (0xFF & updateBuffer[13]);

		if (this.timeTriggerButton.getSelection())
			updateBuffer[14] = (byte) ((new Byte(this.timeTriggerCombo.getText().trim())) | 0x80);
		else {
			int autoStartZeit = Integer.parseInt(this.timeTriggerCombo.getText().trim());
			autoStartZeit = autoStartZeit - (autoStartZeit % 5);
			updateBuffer[14] = (byte) (autoStartZeit & 0x7F);
		}
		checkSum = checkSum + (0xFF & updateBuffer[14]);

		updateBuffer[15] = (byte) (this.voltageLevelRegulationCombo.getSelectionIndex() + 20); // voltage level to start impules regulation
		checkSum = checkSum + (0xFF & updateBuffer[15]);

		if (this.impulseTriggerButton.getSelection()) {
			if ((this.impulseTriggerCombo.getSelectionIndex() + 1) == LiPoWatchDialog.RX_AUTO_START_MS.length) // "RX an"
				updateBuffer[16] = (byte) 0x80;
			else
				updateBuffer[16] = (byte) ((this.impulseTriggerCombo.getSelectionIndex() | 0x80) + 11);
		}
		else if ((this.impulseTriggerCombo.getSelectionIndex() + 1) == LiPoWatchDialog.RX_AUTO_START_MS.length) // "RX an"
			updateBuffer[16] = (byte) 0x00;
		else
			updateBuffer[16] = (byte) (this.impulseTriggerCombo.getSelectionIndex() + 11);
		checkSum = checkSum + (0xFF & updateBuffer[16]);

		updateBuffer[17] = (byte) this.regulationTypeCombo.getSelectionIndex(); // regulation type off, soft, hard
		checkSum = checkSum + (0xFF & updateBuffer[17]);

		updateBuffer[18] = (byte) (this.voltageDropTriggerButton.getSelection() ? 0x01 : 0x00); // voltage drop trigger
		checkSum = checkSum + (0xFF & updateBuffer[18]);

		updateBuffer[19] = (byte) this.cellTypeCombo.getSelectionIndex(); // cell type
		checkSum = checkSum + (0xFF & updateBuffer[19]);

		updateBuffer[20] = (byte) (checkSum % 256);

		if (LiPoWatchDialog.log.isLoggable(Level.FINE)) {
			StringBuilder sb = new StringBuilder();
			sb.append("updateBuffer = ["); //$NON-NLS-1$
			for (int i = 0; i < updateBuffer.length; i++) {
				if (i == updateBuffer.length - 1)
					sb.append(String.format("%02X", updateBuffer[i])); //$NON-NLS-1$
				else
					sb.append(String.format("%02X ", updateBuffer[i])); //$NON-NLS-1$
			}
			sb.append("]"); //$NON-NLS-1$
			LiPoWatchDialog.log.log(Level.FINE, sb.toString());
		}

		return updateBuffer;
	}

	/**
	 * function to reset counter labels
	 */
	public void resetDataSetsLabel() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.numberRedDataSetsText = "0"; //$NON-NLS-1$
			this.numberActualDataSetsText = "0"; //$NON-NLS-1$
			this.numberReadErrorText = "0"; //$NON-NLS-1$
			this.numberLess4Text = "0"; //$NON-NLS-1$
			this.redDataSetLabel.setText(this.numberRedDataSetsText);
			this.actualDataSetNumber.setText(this.numberActualDataSetsText);
			this.numberReadErrorLabel.setText(this.numberReadErrorText);
			this.numberLess4Label.setText(this.numberLess4Text);
			this.readDataProgressBar.setSelection(0);
		}
		else {
			DataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					LiPoWatchDialog.this.numberRedDataSetsText = "0"; //$NON-NLS-1$
					LiPoWatchDialog.this.numberActualDataSetsText = "0"; //$NON-NLS-1$
					LiPoWatchDialog.this.numberReadErrorText = "0"; //$NON-NLS-1$
					LiPoWatchDialog.this.redDataSetLabel.setText(LiPoWatchDialog.this.numberRedDataSetsText);
					LiPoWatchDialog.this.actualDataSetNumber.setText(LiPoWatchDialog.this.numberActualDataSetsText);
					LiPoWatchDialog.this.numberReadErrorLabel.setText(LiPoWatchDialog.this.numberReadErrorText);
					LiPoWatchDialog.this.numberLess4Label.setText(LiPoWatchDialog.this.numberLess4Text);
					LiPoWatchDialog.this.readDataProgressBar.setSelection(0);
				}
			});
		}
	}

	/**
	 * function to reset all the buttons, normally called after data gathering finished
	 */
	public void resetButtons() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.readConfigButton.setEnabled(true);
			this.storeConfigButton.setEnabled(false);

			this.readDataButton.setEnabled(true);
			this.stopReadDataButton.setEnabled(false);

			this.startLoggingButton.setEnabled(true);
			this.stopLoggingButton.setEnabled(false);

			this.startLiveGatherButton.setEnabled(true);
			this.stopLiveGatherButton.setEnabled(false);

			this.clearMemoryButton.setEnabled(true);
			this.closeButton.setEnabled(true);
			setClosePossible(true);
		}
		else {
			DataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					LiPoWatchDialog.this.readConfigButton.setEnabled(true);
					LiPoWatchDialog.this.storeConfigButton.setEnabled(false);

					LiPoWatchDialog.this.readDataButton.setEnabled(true);
					LiPoWatchDialog.this.stopReadDataButton.setEnabled(false);

					LiPoWatchDialog.this.startLoggingButton.setEnabled(true); 
					LiPoWatchDialog.this.stopLoggingButton.setEnabled(false);

					LiPoWatchDialog.this.startLiveGatherButton.setEnabled(true);
					LiPoWatchDialog.this.stopLiveGatherButton.setEnabled(false);

					LiPoWatchDialog.this.clearMemoryButton.setEnabled(true);
					LiPoWatchDialog.this.closeButton.setEnabled(true);
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
			this.device.setTimeStep_ms(1000.0 / 4);
			break;
		case 1: // 1/2 sec
			this.device.setTimeStep_ms(1000.0 / 2);
			break;
		case 2: // 1 sec
			this.device.setTimeStep_ms(1000.0 * 1);
			break;
		case 3: // 2 sec
			this.device.setTimeStep_ms(1000.0 * 2);
			break;
		case 4: // 5 sec
			this.device.setTimeStep_ms(1000.0 * 5);
			break;
		case 5: // 10 sec
			this.device.setTimeStep_ms(1000.0 * 10);
			break;
		}
		this.device.storeDeviceProperties();
	}

	/**
	 * set progress bar to value between 0 to 100, called out of thread
	 * @param value
	 */
	public void setReadDataProgressBar(final int value) {
		DataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				int tmpValue = value < 0 ? 0 : value;
				tmpValue = value > 100 ? 100 : value;
				LiPoWatchDialog.this.readDataProgressBar.setSelection(tmpValue);
			}
		});
	}

	/**
	 * update the counter number in the dialog, called out of thread
	 * @param redTelegrams
	 * @param numberRecordSet
	 * @param numReadErrors
	 * @param numLess4Measurements
	 * @param memoryUsed
	 */
	public void updateDataGatherProgress(final int redTelegrams, final int numberRecordSet, final int numReadErrors, final int numLess4Measurements, final int memoryUsed) {
		this.numberRedDataSetsText = "" + redTelegrams; //$NON-NLS-1$
		this.numberActualDataSetsText = "" + numberRecordSet; //$NON-NLS-1$
		this.numberReadErrorText = "" + numReadErrors; //$NON-NLS-1$
		this.numberLess4Text = "" + numLess4Measurements; //$NON-NLS-1$
		DataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				int progress = redTelegrams * 100 / memoryUsed;
				int tmpValue = progress < 0 ? 0 : progress;
				tmpValue = progress > 100 ? 100 : progress;
				LiPoWatchDialog.this.readDataProgressBar.setSelection(tmpValue);
				LiPoWatchDialog.this.redDataSetLabel.setText(LiPoWatchDialog.this.numberRedDataSetsText);
				LiPoWatchDialog.this.actualDataSetNumber.setText(LiPoWatchDialog.this.numberActualDataSetsText);
				LiPoWatchDialog.this.numberReadErrorLabel.setText(LiPoWatchDialog.this.numberReadErrorText);
				LiPoWatchDialog.this.numberLess4Label.setText(LiPoWatchDialog.this.numberLess4Text);
			}
		});
	}

	/**
	 * initialize buttons settings
	 */
	private void initialize() {
		LiPoWatchDialog.this.snLabel.setText(LiPoWatchDialog.this.serialNumber);
		LiPoWatchDialog.this.firmwareVersionLabel.setText(LiPoWatchDialog.this.lipoWatchVersion);
		LiPoWatchDialog.this.memUsagePercent.setText(LiPoWatchDialog.this.memoryUsedPercent);
		LiPoWatchDialog.this.timeIntervalCombo.select(LiPoWatchDialog.this.timeIntervalPosition);
		LiPoWatchDialog.this.measurementModusCombo.select(LiPoWatchDialog.this.measurementModus);
		LiPoWatchDialog.this.voltageDropTriggerButton.setSelection(LiPoWatchDialog.this.isAutoStartVoltageDrop);
		LiPoWatchDialog.this.timeTriggerButton.setSelection(LiPoWatchDialog.this.isAutoStartTime);
		LiPoWatchDialog.this.impulseTriggerButton.setSelection(LiPoWatchDialog.this.isAutStartRx);
		LiPoWatchDialog.this.regulationTypeCombo.select(LiPoWatchDialog.this.impulsReductionType);
		LiPoWatchDialog.this.voltageLevelRegulationCombo.select(LiPoWatchDialog.this.voltageLevelRegulationLimit);
		LiPoWatchDialog.this.cellTypeCombo.select(LiPoWatchDialog.this.cellType);
	}
}
