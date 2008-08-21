package osde.device.smmodellbau;

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
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.DeviceDialog;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
/**
 * UniLog device dialog class
 * @author Winfried Brügmann
 */
public class UniLogDialog extends DeviceDialog {
	/**
	 * 
	 */
	private static final String	DEVICE_NAME	= "UniLog";

	final static Logger						log												= Logger.getLogger(UniLogDialog.class.getName());

	public final static int				WERTESAETZE_MAX						= 25920;
	public final static String[]	TIME_INTERVAL							= { " 1/16 s (-> 27 min)", " 1/8 s   (-> 54 min)", " 1/4 s   (-> 1:48 h)", " 1/2 s   (-> 3:36 h)", "   1 s    (-> 7:12 h)", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			"   2 s    (-> 14:24 h)", "   5 s    (-> 36 h)", " 10 s   (-> 72 h)" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	public final static String[]	RX_AUTO_START_MS					= { " 1,1 ms", " 1,2 ms", " 1,3 ms", " 1,4 ms", " 1,5 ms", " 1,6 ms", " 1,7 ms", " 1,8 ms", " 1,9 ms", " Rx on" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
	public final static String[]	CURRENT_SENSOR						= { " 40/80A ", "  150A ", "  400A ", "    20A " }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	public final static String[]	A1_MODUS									= { " Temperatur ", " Millivolt ", " Speed 250 ", " Speed 400 " }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	CTabItem											configTabItem1, configTabItem2, configTabItem3, configTabItem4;
	CCombo												a1ModusCombo;
	Group													outletA1Group;
	CCombo												sensorCurrentCombo;
	Group													currentSensotGroup;
	CCombo												impulseTriggerCombo;
	CCombo												timeTriggerCombo;
	Button												numberPolsButton;
	CCombo												currentTriggerCombo;
	Button												impulseTriggerButton;
	Button												timeTriggerButton;
	Button												currentTriggerButton;
	Group													autoStartGroup;
	Text													gearFactorCombo;
	CLabel												gearLabel;
	CCombo												numbeProbCombo;
	CCombo												motorPoleCombo;
	Button												numberPropButton;
	Group													motorPropGroup;
	Button												storeAdjustmentsButton;

	CCombo												useConfigCombo;
	CLabel												firmwareVersionLabel;
	CLabel												firmwareText;
	CLabel												snLabel;
	CLabel												serialNumberText;
	CLabel												memUsageUnit;
	CLabel												memoryUsageText;
	Slider												gearRatioSlider;
	Button												stopDataButton;
	Group													liveDataCaptureGroup;
	Button												clearMemoryButton;
	Group													clearDataBufferGroup;
	Button												startLiveGatherButton;
	Button												stopLoggingButton;
	Button												startLoggingButton;
	Group													loggingGroup;
	CLabel												numberReadErrorLabel;
	CLabel												readDataErrorLabel;
	CLabel												redDataSetLabel;
	CLabel												dataSetLabel;
	CLabel												actualDataSetNumberLabel;
	CLabel												actualDataSetNumber;
	ProgressBar										readDataProgressBar;
	Button												readDataButton;
	Group													dataReadGroup;
	Button												closeButton;
	Button												helpButton;
	Composite											dataMainComposite;
	CTabItem											dataTabItem;
	Group													statusGroup;
	CLabel												memUsagePercent;
	CCombo												timeIntervalCombo;
	Group													dataRateGroup;
	Composite											configMainComosite;
	Button												readAdjustmentButton;
	CTabItem											baseConfigTabItem;
	CTabFolder										deviceConfigTabFolder;

	Group													channleConfigGroup;
	Button												stopLiveGatherButton;
	Button												editConfigButton;
	Text													memoryDeleteInfo;

	final Settings								settings;
	final UniLogSerialPort				serialPort;									// open/close port execute getData()....
	final OpenSerialDataExplorer	application;								// interaction with application instance
	final UniLog									device;											// get device specific things, get serial port, ...
	DataGathererThread						gatherThread;
	LiveGathererThread						liveThread;
	UniLogConfigTab								configTab1, configTab2, configTab3, configTab4;

	String												statusText								= ""; //$NON-NLS-1$

	String												serialNumber							= ""; //$NON-NLS-1$
	String												unilogVersion							= ""; //$NON-NLS-1$
	int														memoryUsed								= 0;
	String												memoryUsedPercent					= "0"; //$NON-NLS-1$
	int														timeIntervalPosition			= -1;
	boolean												isMotorPole								= false;
	boolean												isPropBlade								= false;
	int														countMotorPole						= 0;
	int														countPropBlade						= 0;
	boolean												isAutoStartCurrent				= false;
	int														currentAutoStart					= 0;
	boolean												isAutStartRx							= false;
	boolean												isRxOn										= false;
	int														rxAutoStartValue					= 0;
	boolean												isImpulseAutoStartTime		= false;
	int														impulseAutoStartTime_sec	= 0;
	int														currentSensorPosition			= 0;
	int														modusA1Position						= 0;
	double												gearRatio									= 1.0;

	int														sliderPosition						= 50;
	String[]											configurationNames				= new String[] { " Konfig 1" }; //$NON-NLS-1$
	String												numberRedDataSetsText			= "0"; //$NON-NLS-1$
	String												numberActualDataSetsText	= "0"; //$NON-NLS-1$
	String												numberReadErrorText				= "0"; //$NON-NLS-1$
	int														channelSelectionIndex			= 0;

	/**
	* main method to test this dialog inside a shell 
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			UniLog device = new UniLog("c:\\Documents and Settings\\user\\Application Data\\OpenSerialDataExplorer\\Geraete\\Htronic Akkumaster C4.ini"); //$NON-NLS-1$
			UniLogDialog inst = new UniLogDialog(shell, device);
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
	public UniLogDialog(Shell parent, UniLog useDevice) {
		super(parent);
		this.serialPort = useDevice.getSerialPort();
		this.device = useDevice;
		this.application = OpenSerialDataExplorer.getInstance();
		this.settings = Settings.getInstance();
		RX_AUTO_START_MS[RX_AUTO_START_MS.length-1] = Messages.getString(MessageIds.OSDE_MSGT1400);
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	public void open() {
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue(); 
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			UniLogDialog.log.fine("dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialogShell == null || this.dialogShell.isDisposed()) {
				if (this.settings.isDeviceDialogsModal())
					this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
				else if (this.settings.isDeviceDialogsOnTop())
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
				else
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(this.dialogShell);
				this.dialogShell.setText(DEVICE_NAME + Messages.getString(osde.messages.MessageIds.OSDE_MSGT0273));
				this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				if (this.isAlphaEnabled) this.dialogShell.setAlpha(254);
				this.dialogShell.setLayout(null);
				this.dialogShell.layout();
				this.dialogShell.pack();
				this.dialogShell.setSize(642, 446);
				this.dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						UniLogDialog.log.fine("dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (UniLogDialog.this.configTab1.getConfigButtonStatus() && UniLogDialog.this.configTab2.getConfigButtonStatus() && UniLogDialog.this.configTab3.getConfigButtonStatus()
								&& UniLogDialog.this.configTab4.getConfigButtonStatus()) {
							String msg = Messages.getString(MessageIds.OSDE_MSGI1400);
							if (UniLogDialog.this.application.openYesNoMessageDialog(msg) == SWT.YES) {
								UniLogDialog.log.fine("SWT.YES"); //$NON-NLS-1$
								UniLogDialog.this.device.storeDeviceProperties();
								UniLogDialog.this.setClosePossible(true);
							}
							// check threads before close
							if (UniLogDialog.this.gatherThread != null && UniLogDialog.this.gatherThread.isAlive()) UniLogDialog.this.gatherThread.interrupt();
							if (UniLogDialog.this.liveThread != null && UniLogDialog.this.liveThread.isTimerRunning) {
								UniLogDialog.this.liveThread.stopTimerThread();
							}
						}
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						UniLogDialog.log.fine("dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						//application.openHelpDialog(DEVICE_NAME, "HelpInfo.html"); //$NON-NLS-1$
						int selection = UniLogDialog.this.deviceConfigTabFolder.getSelectionIndex();
						if (selection == 0)
							UniLogDialog.this.application.openHelpDialog(DEVICE_NAME, "HelpInfo.html#adjustment"); //$NON-NLS-1$
						else if (selection == UniLogDialog.this.deviceConfigTabFolder.getItemCount() - 1)
							UniLogDialog.this.application.openHelpDialog(DEVICE_NAME, "HelpInfo.html#data_io"); //$NON-NLS-1$
						else
							UniLogDialog.this.application.openHelpDialog(DEVICE_NAME, "HelpInfo.html#config_tab"); //$NON-NLS-1$
					}
				});
				this.dialogShell.addMouseTrackListener(new MouseTrackAdapter() {
					public void mouseEnter(MouseEvent evt) {
						log.finer("dialogShell.mouseEnter, event=" + evt); //$NON-NLS-1$
						fadeOutAplhaBlending(evt, UniLogDialog.this.getDialogShell().getClientArea(), 10, 10, 10, 15);
					}
					public void mouseHover(MouseEvent evt) {
						log.finest("dialogShell.mouseHover, event=" + evt); //$NON-NLS-1$
					}
					public void mouseExit(MouseEvent evt) {
						log.finer("dialogShell.mouseExit, event=" + evt); //$NON-NLS-1$
						fadeInAlpaBlending(evt, UniLogDialog.this.getDialogShell().getClientArea(), 10, 10, -10, 15);
					}
				});
				{
					this.helpButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					this.helpButton.setText(Messages.getString(osde.messages.MessageIds.OSDE_MSGT0280));
					this.helpButton.setBounds(31, 374, 259, 30);
					this.helpButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("helpButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLogDialog.this.application.openHelpDialog(DEVICE_NAME, "HelpInfo.html"); //$NON-NLS-1$
						}
					});
				}
				{
					this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					this.closeButton.setText(Messages.getString(osde.messages.MessageIds.OSDE_MSGT0188));
					this.closeButton.setBounds(342, 374, 260, 30);
					this.closeButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							dispose();
						}
					});
				}

				{ // begin tabs
					this.deviceConfigTabFolder = new CTabFolder(this.dialogShell, SWT.NONE);

					{// begin device configuration tab
						this.baseConfigTabItem = new CTabItem(this.deviceConfigTabFolder, SWT.NONE);
						this.baseConfigTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT1401));
						{
							this.configMainComosite = new Composite(this.deviceConfigTabFolder, SWT.NONE);
							this.baseConfigTabItem.setControl(this.configMainComosite);
							this.configMainComosite.setLayout(null);
							//this.configMainComosite.addMouseTrackListener(UniLogDialog.this.mouseTrackerEnterFadeOut);
							this.configMainComosite.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									UniLogDialog.log.finer("configMainComosite.paintControl " + evt); //$NON-NLS-1$
									if (UniLogDialog.this.timeIntervalPosition == -1) {
										int selection = new Double(UniLogDialog.this.device.getTimeStep_ms() / 62.5).intValue();
										switch (selection) {
										case 1: // 1/16 sec
											UniLogDialog.this.timeIntervalPosition = 0;
											break;
										case 2: // 1/8 sec
											UniLogDialog.this.timeIntervalPosition = 1;
											break;
										default:
										case 4: // 1/4 sec
											UniLogDialog.this.timeIntervalPosition = 2;
											break;
										case 8: // 1/2 sec
											UniLogDialog.this.timeIntervalPosition = 3;
											break;
										case 16: // 1 sec
											UniLogDialog.this.timeIntervalPosition = 4;
											break;
										case 32: // 2 sec
											UniLogDialog.this.timeIntervalPosition = 5;
											break;
										case 80: // 5 sec
											UniLogDialog.this.timeIntervalPosition = 6;
											break;
										case 160: // 10 sec
											UniLogDialog.this.timeIntervalPosition = 7;
											break;
										}
									}
									UniLogDialog.this.timeIntervalCombo.select(UniLogDialog.this.timeIntervalPosition);
								}
							});
							{
								this.statusGroup = new Group(this.configMainComosite, SWT.NONE);
								this.statusGroup.setLayout(null);
								this.statusGroup.setText(Messages.getString(MessageIds.OSDE_MSGT1402));
								this.statusGroup.setBounds(12, 17, 602, 45);
								this.statusGroup.addMouseTrackListener(UniLogDialog.this.mouseTrackerEnterFadeOut);
								{
									this.firmwareText = new CLabel(this.statusGroup, SWT.NONE);
									this.firmwareText.setText(Messages.getString(MessageIds.OSDE_MSGT1403));
									this.firmwareText.setBounds(146, 15, 79, 22);
								}
								{
									this.firmwareVersionLabel = new CLabel(this.statusGroup, SWT.NONE);
									this.firmwareVersionLabel.setBounds(225, 15, 71, 22);
									this.firmwareVersionLabel.setText(this.unilogVersion);
								}
								{
									this.serialNumberText = new CLabel(this.statusGroup, SWT.NONE);
									this.serialNumberText.setText(Messages.getString(MessageIds.OSDE_MSGT1404));
									this.serialNumberText.setBounds(12, 15, 42, 22);
								}
								{
									this.snLabel = new CLabel(this.statusGroup, SWT.CENTER | SWT.EMBEDDED);
									this.snLabel.setBounds(52, 15, 88, 22);
									this.snLabel.setText(this.serialNumber);
								}
								{
									this.memoryUsageText = new CLabel(this.statusGroup, SWT.NONE);
									this.memoryUsageText.setText(Messages.getString(MessageIds.OSDE_MSGT1405));
									this.memoryUsageText.setBounds(338, 15, 134, 22);
								}
								{
									this.memUsagePercent = new CLabel(this.statusGroup, SWT.NONE);
									FormLayout statusLabelLayout = new FormLayout();
									this.memUsagePercent.setLayout(statusLabelLayout);
									this.memUsagePercent.setBounds(472, 15, 59, 22);
									this.memUsagePercent.setText(this.memoryUsedPercent);
								}
								{
									this.memUsageUnit = new CLabel(this.statusGroup, SWT.NONE);
									this.memUsageUnit.setText(Messages.getString(MessageIds.OSDE_MSGT1406));
									this.memUsageUnit.setBounds(543, 15, 26, 22);
								}
							}
							{
								this.outletA1Group = new Group(this.configMainComosite, SWT.NONE);
								this.outletA1Group.setLayout(null);
								this.outletA1Group.setText(Messages.getString(MessageIds.OSDE_MSGT1407));
								this.outletA1Group.setBounds(337, 189, 277, 59);
								this.outletA1Group.addMouseTrackListener(UniLogDialog.this.mouseTrackerEnterFadeOut);
								{
									this.a1ModusCombo = new CCombo(this.outletA1Group, SWT.BORDER);
									this.a1ModusCombo.setItems(UniLogDialog.A1_MODUS);
									this.a1ModusCombo.select(2);
									this.a1ModusCombo.setEditable(false);
									this.a1ModusCombo.setBounds(91, 24, 117, 20);
									this.a1ModusCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									this.a1ModusCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("outletModusCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
							}
							{
								this.currentSensotGroup = new Group(this.configMainComosite, SWT.NONE);
								this.currentSensotGroup.setLayout(null);
								this.currentSensotGroup.setText(Messages.getString(MessageIds.OSDE_MSGT1408));
								this.currentSensotGroup.setBounds(337, 122, 277, 59);
								this.currentSensotGroup.addMouseTrackListener(UniLogDialog.this.mouseTrackerEnterFadeOut);
								{
									this.sensorCurrentCombo = new CCombo(this.currentSensotGroup, SWT.BORDER);
									this.sensorCurrentCombo.setBounds(88, 23, 119, 20);
									this.sensorCurrentCombo.setItems(UniLogDialog.CURRENT_SENSOR);
									this.sensorCurrentCombo.select(2);
									this.sensorCurrentCombo.setEditable(false);
									this.sensorCurrentCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									this.sensorCurrentCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("sensorCurrentCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
							}
							{
								this.autoStartGroup = new Group(this.configMainComosite, SWT.NONE);
								this.autoStartGroup.setLayout(null);
								this.autoStartGroup.setText(Messages.getString(MessageIds.OSDE_MSGT1409));
								this.autoStartGroup.setBounds(12, 226, 300, 99);
								this.autoStartGroup.addMouseTrackListener(UniLogDialog.this.mouseTrackerEnterFadeOut);
								{
									this.currentTriggerButton = new Button(this.autoStartGroup, SWT.CHECK | SWT.LEFT);
									this.currentTriggerButton.setText(Messages.getString(MessageIds.OSDE_MSGT1410));
									this.currentTriggerButton.setBounds(34, 22, 150, 18);
									this.currentTriggerButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("currentTriggerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									this.timeTriggerButton = new Button(this.autoStartGroup, SWT.CHECK | SWT.LEFT);
									this.timeTriggerButton.setText(Messages.getString(MessageIds.OSDE_MSGT1411));
									this.timeTriggerButton.setBounds(34, 46, 150, 18);
									this.timeTriggerButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("timeTriggerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									this.impulseTriggerButton = new Button(this.autoStartGroup, SWT.CHECK | SWT.LEFT);
									this.impulseTriggerButton.setText(Messages.getString(MessageIds.OSDE_MSGT1412));
									this.impulseTriggerButton.setBounds(34, 70, 150, 18);
									this.impulseTriggerButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("impulseTriggerButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									this.currentTriggerCombo = new CCombo(this.autoStartGroup, SWT.BORDER);
									this.currentTriggerCombo.setBounds(200, 22, 80, 20);
									this.currentTriggerCombo.setItems(new String[] { "  1", "  2", "  3", "  4", "  5", "  6", "  7", "  8", "  9", " 10", "15", "20", "25", "30", "35", "40", "45", "50" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$ //$NON-NLS-17$ //$NON-NLS-18$
									this.currentTriggerCombo.select(2);
									this.currentTriggerCombo.setEditable(false);
									this.currentTriggerCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									this.currentTriggerCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("currentTriggerCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									this.timeTriggerCombo = new CCombo(this.autoStartGroup, SWT.BORDER);
									this.timeTriggerCombo.setBounds(200, 46, 80, 20);
									this.timeTriggerCombo.setItems(new String[] { "  1", "  2", "  3", "  4", "  5", "  6", "  7", "  8", "  9", " 10", " 11", " 12", " 13", " 14", " 15", " 16", " 17", " 18", " 19", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$ //$NON-NLS-17$ //$NON-NLS-18$ //$NON-NLS-19$
											" 20", " 30", " 60", " 120" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
									this.timeTriggerCombo.select(16);
									this.timeTriggerCombo.setEditable(true);
									this.timeTriggerCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									this.timeTriggerCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("timeTriggerCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									this.impulseTriggerCombo = new CCombo(this.autoStartGroup, SWT.BORDER);
									this.impulseTriggerCombo.setBounds(200, 70, 80, 20);
									this.impulseTriggerCombo.setEditable(false);
									this.impulseTriggerCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									this.impulseTriggerCombo.setItems(UniLogDialog.RX_AUTO_START_MS);
									this.impulseTriggerCombo.select(4);
									this.impulseTriggerCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("rcTriggerCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
							}
							{
								this.motorPropGroup = new Group(this.configMainComosite, SWT.NONE);
								this.motorPropGroup.setLayout(null);
								this.motorPropGroup.setText(Messages.getString(MessageIds.OSDE_MSGT1413));
								this.motorPropGroup.setBounds(12, 117, 300, 96);
								this.motorPropGroup.addMouseTrackListener(UniLogDialog.this.mouseTrackerEnterFadeOut);
								{
									this.numberPolsButton = new Button(this.motorPropGroup, SWT.RADIO | SWT.LEFT);
									this.numberPolsButton.setText(Messages.getString(MessageIds.OSDE_MSGT1414));
									this.numberPolsButton.setBounds(31, 20, 150, 20);
									this.numberPolsButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("numberPolsButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
											if (UniLogDialog.this.numberPolsButton.getSelection()) {
												UniLogDialog.this.numbeProbCombo.setEnabled(false);
												UniLogDialog.this.motorPoleCombo.setEnabled(true);
											}
											else {
												UniLogDialog.this.numbeProbCombo.setEnabled(true);
												UniLogDialog.this.motorPoleCombo.setEnabled(false);
												UniLogDialog.this.numberPropButton.setSelection(false);
											}
										}
									});
								}
								{
									this.motorPoleCombo = new CCombo(this.motorPropGroup, SWT.BORDER);
									this.motorPoleCombo.setBounds(198, 20, 63, 20);
									this.motorPoleCombo.setItems(new String[] { "  2", "  4", "  6", "  8", " 10", " 12", " 14", " 16" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
									this.motorPoleCombo.select(6);
									this.motorPoleCombo.setEditable(false);
									this.motorPoleCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									this.motorPoleCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("motorPoleCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									this.gearLabel = new CLabel(this.motorPropGroup, SWT.LEFT);
									this.gearLabel.setBounds(31, 42, 150, 20);
									this.gearLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1415));
								}
								{
									this.gearFactorCombo = new Text(this.motorPropGroup, SWT.LEFT | SWT.BORDER);
									this.gearFactorCombo.setBounds(198, 44, 63, 20);
									this.gearFactorCombo.setText(" 1.0  :  1"); //$NON-NLS-1$
									this.gearFactorCombo.setEditable(false);
									this.gearFactorCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
								}
								{
									this.gearRatioSlider = new Slider(this.motorPropGroup, SWT.VERTICAL);
									this.gearRatioSlider.setBounds(263, 39, 21, 30);
									this.gearRatioSlider.setMinimum(0);
									this.gearRatioSlider.setMaximum(100);
									this.gearRatioSlider.setSelection(this.sliderPosition);
									this.gearRatioSlider.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("gearRatioSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
											if (UniLogDialog.this.gearRatioSlider.getSelection() > UniLogDialog.this.sliderPosition) {
												//" 1.0  :  1"
												UniLogDialog.this.gearFactorCombo.setText(String.format(" %.1f  :  1", getGearRatio() - 0.1)); //$NON-NLS-1$
												++UniLogDialog.this.sliderPosition;
											}
											else {
												UniLogDialog.this.gearFactorCombo.setText(String.format(" %.1f  :  1", getGearRatio() + 0.1)); //$NON-NLS-1$
												--UniLogDialog.this.sliderPosition;
											}
										}
									});
								}
								{
									this.numberPropButton = new Button(this.motorPropGroup, SWT.RADIO | SWT.LEFT);
									this.numberPropButton.setText(Messages.getString(MessageIds.OSDE_MSGT1416));
									this.numberPropButton.setBounds(31, 68, 150, 18);
									this.numberPropButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("numberPropButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
											if (UniLogDialog.this.numberPropButton.getSelection()) {
												UniLogDialog.this.numbeProbCombo.setEnabled(true);
												UniLogDialog.this.motorPoleCombo.setEnabled(false);
											}
											else {
												UniLogDialog.this.numbeProbCombo.setEnabled(false);
												UniLogDialog.this.motorPoleCombo.setEnabled(true);
												UniLogDialog.this.numberPolsButton.setSelection(false);
											}
										}
									});
								}
								{
									this.numbeProbCombo = new CCombo(this.motorPropGroup, SWT.BORDER);
									this.numbeProbCombo.setBounds(198, 68, 63, 18);
									this.numbeProbCombo.setItems(new String[] { " 1", " 2", " 3", " 4" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
									this.numbeProbCombo.select(1);
									this.numbeProbCombo.setEditable(false);
									this.numbeProbCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									this.numbeProbCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("numbeProbCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
							}
							{
								this.dataRateGroup = new Group(this.configMainComosite, SWT.NONE);
								this.dataRateGroup.setLayout(null);
								this.dataRateGroup.setText(Messages.getString(MessageIds.OSDE_MSGT1417));
								this.dataRateGroup.setBounds(337, 71, 277, 46);
								this.dataRateGroup.addMouseTrackListener(UniLogDialog.this.mouseTrackerEnterFadeOut);
								{
									this.timeIntervalCombo = new CCombo(this.dataRateGroup, SWT.BORDER);
									this.timeIntervalCombo.setItems(UniLogDialog.TIME_INTERVAL);
									this.timeIntervalCombo.setBounds(70, 17, 136, 20);
									this.timeIntervalCombo.select(1);
									this.timeIntervalCombo.setEditable(false);
									this.timeIntervalCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									this.timeIntervalCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("timeRateCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
											UniLogDialog.this.timeIntervalPosition = UniLogDialog.this.timeIntervalCombo.getSelectionIndex();
										}
									});
								}
							}
							{
								this.readAdjustmentButton = new Button(this.configMainComosite, SWT.PUSH | SWT.FLAT | SWT.CENTER);
								this.readAdjustmentButton.setText(Messages.getString(MessageIds.OSDE_MSGT1418));
								this.readAdjustmentButton.setBounds(12, 74, 300, 30);
								this.readAdjustmentButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("readAdjustmentButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										try {
											updateConfigurationValues(UniLogDialog.this.serialPort.readConfiguration());
										}
										catch (Exception e) {
											UniLogDialog.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0029, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
										}

									}
								});
							}
							{
								this.storeAdjustmentsButton = new Button(this.configMainComosite, SWT.PUSH | SWT.CENTER);
								this.storeAdjustmentsButton.setText(Messages.getString(MessageIds.OSDE_MSGT1419));
								this.storeAdjustmentsButton.setBounds(335, 275, 281, 30);
								this.storeAdjustmentsButton.setEnabled(false);
								this.storeAdjustmentsButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("storeAdjustmentsButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										try {
											if (UniLogDialog.this.serialPort.setConfiguration(buildUpdateBuffer())) {
												updateTimeStep_ms(UniLogDialog.this.timeIntervalPosition);
												UniLogDialog.this.storeAdjustmentsButton.setEnabled(false);
											}
											else {
												UniLogDialog.this.storeAdjustmentsButton.setEnabled(true);
											}
										}
										catch (Exception e) {
											UniLogDialog.this.application.openMessageDialog(e.getMessage());
										}
									}
								});
							}
						}
					} // end device configuration tab

					{ // begin measurement configuration tabs
						if (this.device.getChannelCount() > 0) {
							this.configTabItem1 = new CTabItem(this.deviceConfigTabFolder, SWT.NONE);
							this.configTabItem1.setText(this.device.getChannelName(1));
							this.configTab1 = new UniLogConfigTab(this.deviceConfigTabFolder, this.device, this.device.getChannelName(1));
							this.configTabItem1.setControl(this.configTab1);
						}
						if (this.device.getChannelCount() > 1) {
							this.configTabItem2 = new CTabItem(this.deviceConfigTabFolder, SWT.NONE);
							this.configTabItem2.setText(this.device.getChannelName(2));
							this.configTab2 = new UniLogConfigTab(this.deviceConfigTabFolder, this.device, this.device.getChannelName(2));
							this.configTabItem2.setControl(this.configTab2);
						}
						if (this.device.getChannelCount() > 2) {
							this.configTabItem3 = new CTabItem(this.deviceConfigTabFolder, SWT.NONE);
							this.configTabItem3.setText(this.device.getChannelName(3));
							this.configTab3 = new UniLogConfigTab(this.deviceConfigTabFolder, this.device, this.device.getChannelName(3));
							this.configTabItem3.setControl(this.configTab3);
						}
						if (this.device.getChannelCount() > 3) {
							this.configTabItem4 = new CTabItem(this.deviceConfigTabFolder, SWT.NONE);
							this.configTabItem4.setText(this.device.getChannelName(4));
							this.configTab4 = new UniLogConfigTab(this.deviceConfigTabFolder, this.device, this.device.getChannelName(4));
							this.configTabItem4.setControl(this.configTab4);
						}
					} // end measurement configuration tabs

					{ // begin data tab
						this.dataTabItem = new CTabItem(this.deviceConfigTabFolder, SWT.NONE);
						this.dataTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT1420));
						{
							this.dataMainComposite = new Composite(this.deviceConfigTabFolder, SWT.NONE);
							this.dataMainComposite.setLayout(null);
							this.dataTabItem.setControl(this.dataMainComposite);
							//this.dataMainComposite.addMouseTrackListener(UniLogDialog.this.mouseTrackerEnterFadeOut);
							{
								this.channleConfigGroup = new Group(this.dataMainComposite, SWT.NONE);
								this.channleConfigGroup.setLayout(null);
								this.channleConfigGroup.setBounds(14, 12, 290, 58);
								this.channleConfigGroup.setText(Messages.getString(MessageIds.OSDE_MSGT1421));
								this.channleConfigGroup.addMouseTrackListener(UniLogDialog.this.mouseTrackerEnterFadeOut);
								{
									this.useConfigCombo = new CCombo(this.channleConfigGroup, SWT.BORDER);
									this.useConfigCombo.setBounds(24, 24, 140, 20);
									this.useConfigCombo.setItems(this.configurationNames);
									this.useConfigCombo.select(this.channelSelectionIndex);
									this.useConfigCombo.setEditable(false);
									this.useConfigCombo.setTextLimit(18);
									this.useConfigCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									this.useConfigCombo.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT1422));
									this.useConfigCombo.addKeyListener(new KeyAdapter() {
										public void keyReleased(KeyEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("useConfigCombo.keyReleased, event=" + evt); //$NON-NLS-1$
											if (evt.character == SWT.CR) {
												String configName = UniLogDialog.this.useConfigCombo.getText().trim();
												UniLogDialog.this.device.setChannelName(configName, UniLogDialog.this.channelSelectionIndex + 1);
												UniLogDialog.this.configurationNames[UniLogDialog.this.channelSelectionIndex] = configName;
												UniLogDialog.this.useConfigCombo.select(UniLogDialog.this.channelSelectionIndex);
												UniLogDialog.this.dataReadGroup.redraw();
												Channels.getInstance().get(UniLogDialog.this.channelSelectionIndex + 1).setName(" " + (UniLogDialog.this.channelSelectionIndex + 1) + " : " + configName); //$NON-NLS-1$ //$NON-NLS-2$
												UniLogDialog.this.application.getMenuToolBar().updateChannelSelector();
												switch (UniLogDialog.this.channelSelectionIndex) {
												case 0: //configTab1
													UniLogDialog.this.configTabItem1.setText(configName);
													UniLogDialog.this.configTab1.setConfigName(configName);
													break;
												case 1: //configTab2
													UniLogDialog.this.configTabItem2.setText(configName);
													UniLogDialog.this.configTab2.setConfigName(configName);
													break;
												case 2: //configTab3												
													UniLogDialog.this.configTabItem3.setText(configName);
													UniLogDialog.this.configTab3.setConfigName(configName);
													break;
												case 3: //configTab4
													UniLogDialog.this.configTabItem4.setText(configName);
													UniLogDialog.this.configTab4.setConfigName(configName);
													break;
												}
												UniLogDialog.this.deviceConfigTabFolder.redraw();
												UniLogDialog.this.editConfigButton.setEnabled(false);
												UniLogDialog.this.device.storeDeviceProperties();
											}
										}
									});
									this.useConfigCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("useConfigCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.readDataButton.setEnabled(true);
											UniLogDialog.this.startLoggingButton.setEnabled(true);
											UniLogDialog.this.startLiveGatherButton.setEnabled(true);
											UniLogDialog.this.channelSelectionIndex = UniLogDialog.this.useConfigCombo.getSelectionIndex();
											UniLogDialog.this.useConfigCombo.select(UniLogDialog.this.channelSelectionIndex);
											UniLogDialog.this.editConfigButton.setEnabled(true);
											resetDataSetsLabel();
										}
									});
								}
								{
									this.editConfigButton = new Button(this.channleConfigGroup, SWT.PUSH | SWT.CENTER);
									this.editConfigButton.setBounds(172, 23, 106, 23);
									this.editConfigButton.setText(Messages.getString(MessageIds.OSDE_MSGT1423));
									this.editConfigButton.setEnabled(false);
									this.editConfigButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("editConfigButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											UniLogDialog.this.useConfigCombo.setEditable(true);
											UniLogDialog.this.editConfigButton.setEnabled(false);
										}
									});
								}
							}
							{
								this.dataReadGroup = new Group(this.dataMainComposite, SWT.NONE);
								this.dataReadGroup.setLayout(null);
								this.dataReadGroup.setBounds(14, 78, 290, 242);
								this.dataReadGroup.setText(Messages.getString(MessageIds.OSDE_MSGT1424));
								this.dataReadGroup.addPaintListener(new PaintListener() {
									public void paintControl(PaintEvent evt) {
										if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("dataReadGroup.paintControl, event=" + evt); //$NON-NLS-1$
										int index = UniLogDialog.this.useConfigCombo.getSelectionIndex();
										UniLogDialog.this.configurationNames = new String[UniLogDialog.this.device.getChannelCount()];
										for (int i = 0; i < UniLogDialog.this.configurationNames.length; i++) {
											UniLogDialog.this.configurationNames[i] = " " + UniLogDialog.this.device.getChannelName(i + 1); //$NON-NLS-1$
										}
										UniLogDialog.this.useConfigCombo.setItems(UniLogDialog.this.configurationNames);
										UniLogDialog.this.useConfigCombo.select(index);
									}
								});
								this.dataReadGroup.addMouseTrackListener(UniLogDialog.this.mouseTrackerEnterFadeOut);
								{
									this.readDataButton = new Button(this.dataReadGroup, SWT.PUSH | SWT.CENTER);
									this.readDataButton.setText(Messages.getString(MessageIds.OSDE_MSGT1425));
									this.readDataButton.setBounds(11, 40, 260, 30);
									this.readDataButton.setEnabled(true);
									this.readDataButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (UniLogDialog.log.isLoggable(Level.FINEST)) UniLogDialog.log.finest("readDataButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											String channelName = " " + (UniLogDialog.this.useConfigCombo.getSelectionIndex() + 1) + " : " + UniLogDialog.this.useConfigCombo.getText(); //$NON-NLS-1$ //$NON-NLS-2$
											UniLogDialog.this.gatherThread = new DataGathererThread(UniLogDialog.this.application, UniLogDialog.this.device, UniLogDialog.this.serialPort, channelName);
											UniLogDialog.this.gatherThread.start();
											UniLogDialog.this.setClosePossible(false);

											UniLogDialog.this.readDataButton.setEnabled(false);
											UniLogDialog.this.editConfigButton.setEnabled(false);
											UniLogDialog.this.stopDataButton.setEnabled(true);
											UniLogDialog.this.startLoggingButton.setEnabled(false);
											UniLogDialog.this.stopLoggingButton.setEnabled(false);
											UniLogDialog.this.useConfigCombo.setEnabled(false);
											UniLogDialog.this.startLiveGatherButton.setEnabled(false);
											UniLogDialog.this.clearMemoryButton.setEnabled(false);
										}
									});
								}
								{
									this.dataSetLabel = new CLabel(this.dataReadGroup, SWT.RIGHT);
									this.dataSetLabel.setBounds(22, 78, 180, 20);
									this.dataSetLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1426));
								}
								{
									this.redDataSetLabel = new CLabel(this.dataReadGroup, SWT.RIGHT);
									this.redDataSetLabel.setBounds(205, 78, 55, 20);
									this.redDataSetLabel.setText("0"); //$NON-NLS-1$
								}
								{
									this.actualDataSetNumberLabel = new CLabel(this.dataReadGroup, SWT.RIGHT);
									this.actualDataSetNumberLabel.setBounds(22, 101, 180, 20);
									this.actualDataSetNumberLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1427));
								}
								{
									this.actualDataSetNumber = new CLabel(this.dataReadGroup, SWT.RIGHT);
									this.actualDataSetNumber.setBounds(205, 101, 55, 20);
									this.actualDataSetNumber.setText("0"); //$NON-NLS-1$
								}
								{
									this.readDataErrorLabel = new CLabel(this.dataReadGroup, SWT.RIGHT);
									this.readDataErrorLabel.setBounds(22, 123, 180, 20);
									this.readDataErrorLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1428));
								}
								{
									this.numberReadErrorLabel = new CLabel(this.dataReadGroup, SWT.RIGHT);
									this.numberReadErrorLabel.setBounds(205, 123, 55, 20);
									this.numberReadErrorLabel.setText("0"); //$NON-NLS-1$
								}
								{
									this.readDataProgressBar = new ProgressBar(this.dataReadGroup, SWT.NONE);
									this.readDataProgressBar.setBounds(15, 158, 260, 15);
									this.readDataProgressBar.setMinimum(0);
									this.readDataProgressBar.setMaximum(100);
								}
								{
									this.stopDataButton = new Button(this.dataReadGroup, SWT.PUSH | SWT.CENTER);
									this.stopDataButton.setBounds(15, 194, 260, 30);
									this.stopDataButton.setText(Messages.getString(osde.messages.MessageIds.OSDE_MSGT0278));
									this.stopDataButton.setEnabled(false);
									this.stopDataButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											UniLogDialog.log.fine("stopDataButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											if (UniLogDialog.this.gatherThread != null && UniLogDialog.this.gatherThread.isAlive()) {
												UniLogDialog.this.gatherThread.setThreadStop(); // end serial communication
											}
											resetButtons();
										}
									});
								}
							}
							{
								this.liveDataCaptureGroup = new Group(this.dataMainComposite, SWT.NONE);
								this.liveDataCaptureGroup.setLayout(null);
								this.liveDataCaptureGroup.setBounds(324, 12, 284, 198);
								this.liveDataCaptureGroup.setText(Messages.getString(MessageIds.OSDE_MSGT1429));
								this.liveDataCaptureGroup.addMouseTrackListener(UniLogDialog.this.mouseTrackerEnterFadeOut);
								{
									this.startLiveGatherButton = new Button(this.liveDataCaptureGroup, SWT.PUSH | SWT.CENTER);
									this.startLiveGatherButton.setText(Messages.getString(MessageIds.OSDE_MSGT1430));
									this.startLiveGatherButton.setBounds(16, 26, 246, 30);
									this.startLiveGatherButton.setSize(260, 30);
									this.startLiveGatherButton.setEnabled(true);
									this.startLiveGatherButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											UniLogDialog.log.fine("liveViewButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											try {
												String channelName = " " + (UniLogDialog.this.useConfigCombo.getSelectionIndex() + 1) + " : " + UniLogDialog.this.useConfigCombo.getText(); //$NON-NLS-1$ //$NON-NLS-2$
												UniLogDialog.this.startLiveGatherButton.setEnabled(false);
												UniLogDialog.this.readDataButton.setEnabled(false);
												UniLogDialog.this.stopLiveGatherButton.setEnabled(true);
												UniLogDialog.this.useConfigCombo.setEnabled(false);
												UniLogDialog.this.editConfigButton.setEnabled(false);
												UniLogDialog.this.setClosePossible(false);
												UniLogDialog.this.liveThread = new LiveGathererThread(UniLogDialog.this.application, UniLogDialog.this.device, UniLogDialog.this.serialPort, channelName, UniLogDialog.this);
												UniLogDialog.this.liveThread.start();
											}
											catch (Exception e) {
												if (UniLogDialog.this.liveThread != null && UniLogDialog.this.liveThread.isTimerRunning) {
													UniLogDialog.this.liveThread.stopTimerThread();
													UniLogDialog.this.liveThread.interrupt();
												}
												UniLogDialog.this.application.updateGraphicsWindow();
												UniLogDialog.this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGE1401, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
												UniLogDialog.this.resetButtons();
											}
										}
									});
								}
								{
									this.loggingGroup = new Group(this.liveDataCaptureGroup, SWT.NONE);
									this.loggingGroup.setLayout(null);
									this.loggingGroup.setBounds(25, 70, 228, 70);
									this.loggingGroup.setText(Messages.getString(MessageIds.OSDE_MSGT1432));
									{
										this.startLoggingButton = new Button(this.loggingGroup, SWT.PUSH | SWT.CENTER);
										this.startLoggingButton.setText(Messages.getString(osde.messages.MessageIds.OSDE_MSGT0274));
										this.startLoggingButton.setBounds(12, 27, 100, 30);
										this.startLoggingButton.setEnabled(true);
										this.startLoggingButton.addSelectionListener(new SelectionAdapter() {
											public void widgetSelected(SelectionEvent evt) {
												UniLogDialog.log.fine("startLoggingButton.widgetSelected, event=" + evt); //$NON-NLS-1$
												try {
													UniLogDialog.this.setClosePossible(false);
													UniLogDialog.this.serialPort.startLogging();
													UniLogDialog.this.startLoggingButton.setEnabled(false);
													UniLogDialog.this.stopLoggingButton.setEnabled(true);
												}
												catch (Exception e) {
													UniLogDialog.log.log(Level.SEVERE, e.getMessage(), e);
													UniLogDialog.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0029, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
												}
											}
										});
									}
									{
										this.stopLoggingButton = new Button(this.loggingGroup, SWT.PUSH | SWT.CENTER);
										this.stopLoggingButton.setText(Messages.getString(osde.messages.MessageIds.OSDE_MSGT0275));
										this.stopLoggingButton.setBounds(116, 27, 100, 30);
										this.stopLoggingButton.setEnabled(false);
										this.stopLoggingButton.addSelectionListener(new SelectionAdapter() {
											public void widgetSelected(SelectionEvent evt) {
												UniLogDialog.log.fine("stopLoggingButton.widgetSelected, event=" + evt); //$NON-NLS-1$
												try {
													UniLogDialog.this.serialPort.stopLogging();
													UniLogDialog.this.startLoggingButton.setEnabled(true);
													UniLogDialog.this.stopLoggingButton.setEnabled(false);
													UniLogDialog.this.setClosePossible(true);
													if(!UniLogDialog.this.stopLiveGatherButton.getEnabled()) {
														UniLogDialog.this.readDataButton.setEnabled(true);
													}
												}
												catch (Exception e) {
													UniLogDialog.log.log(Level.SEVERE, e.getMessage(), e);
													UniLogDialog.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0029, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
												}
											}
										});
									}
								}
								{
									this.stopLiveGatherButton = new Button(this.liveDataCaptureGroup, SWT.PUSH | SWT.CENTER);
									this.stopLiveGatherButton.setBounds(17, 156, 246, 30);
									this.stopLiveGatherButton.setText(Messages.getString(MessageIds.OSDE_MSGT1433));
									this.stopLiveGatherButton.setEnabled(false);
									this.stopLiveGatherButton.setSize(260, 30);
									this.stopLiveGatherButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											UniLogDialog.log.fine("stopLiveGatherButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											if (UniLogDialog.this.liveThread != null) {
												UniLogDialog.this.liveThread.stopTimerThread();
												UniLogDialog.this.liveThread.interrupt();
												
												if (Channels.getInstance().getActiveChannel() != null) {
														RecordSet activeRecordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
														// active record set name == life gatherer record name
														UniLogDialog.this.liveThread.finalizeRecordSet(activeRecordSet.getName());
												}
											}
											UniLogDialog.this.stopLiveGatherButton.setEnabled(false);
											UniLogDialog.this.startLiveGatherButton.setEnabled(true);
											UniLogDialog.this.useConfigCombo.setEnabled(true);
											UniLogDialog.this.editConfigButton.setEnabled(true);
											UniLogDialog.this.setClosePossible(true);
											if(!UniLogDialog.this.stopLoggingButton.getEnabled()) {
												UniLogDialog.this.readDataButton.setEnabled(true);
											}
										}
									});
								}
							}

							{ // begin clearDataBufferGroup
								this.clearDataBufferGroup = new Group(this.dataMainComposite, SWT.NONE);
								this.clearDataBufferGroup.setLayout(null);
								this.clearDataBufferGroup.setBounds(324, 216, 284, 104);
								this.clearDataBufferGroup.setText(Messages.getString(MessageIds.OSDE_MSGT1434));
								this.clearDataBufferGroup.addMouseTrackListener(UniLogDialog.this.mouseTrackerEnterFadeOut);
								{
									this.clearMemoryButton = new Button(this.clearDataBufferGroup, SWT.PUSH | SWT.CENTER);
									this.clearMemoryButton.setText(Messages.getString(MessageIds.OSDE_MSGT1435));
									this.clearMemoryButton.setBounds(15, 60, 260, 30);
									this.clearMemoryButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											UniLogDialog.log.fine("clearMemoryButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											try {
												UniLogDialog.this.clearMemoryButton.setEnabled(false);
												UniLogDialog.this.serialPort.clearMemory();
											}
											catch (Exception e) {
												UniLogDialog.log.log(Level.SEVERE, e.getMessage(), e);
												UniLogDialog.this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGE1400, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
												e.printStackTrace();
											}
											UniLogDialog.this.clearMemoryButton.setEnabled(true);
										}
									});
								}
								{
									this.memoryDeleteInfo = new Text(this.clearDataBufferGroup, SWT.CENTER | SWT.WRAP);
									this.memoryDeleteInfo.setBounds(12, 22, 256, 34);
									this.memoryDeleteInfo.setText(Messages.getString(MessageIds.OSDE_MSGI1401));
									this.memoryDeleteInfo.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									this.memoryDeleteInfo.setEditable(false);
								}
							} // end clearDataBufferGroup
						}
					} // end data tab

					this.deviceConfigTabFolder.setBounds(0, 0, 634, 362);
					this.deviceConfigTabFolder.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							UniLogDialog.log.finest("configTabFolder.widgetSelected, event=" + evt); //$NON-NLS-1$
							int channelNumber = UniLogDialog.this.deviceConfigTabFolder.getSelectionIndex();
							if (channelNumber >= 1 && channelNumber <= 4) {
								String configKey = channelNumber + " : " + ((CTabItem) evt.item).getText(); //$NON-NLS-1$
								Channels channels = Channels.getInstance();
								Channel activeChannel = channels.getActiveChannel();
								if (activeChannel != null) {
									UniLogDialog.log.fine("activeChannel = " + activeChannel.getName() + " configKey = " + configKey); //$NON-NLS-1$ //$NON-NLS-2$
									RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
									if (activeRecordSet != null && !activeChannel.getName().trim().equals(configKey)) {
										int answer = UniLogDialog.this.application.openYesNoMessageDialog(Messages.getString(MessageIds.OSDE_MSGI1402));
										if (answer == SWT.YES) {
											String recordSetKey = activeRecordSet.getName();
											UniLogDialog.log.fine("move record set " + recordSetKey + " to configuration " + configKey); //$NON-NLS-1$ //$NON-NLS-2$
											channels.get(channelNumber).put(recordSetKey, activeRecordSet.clone(configKey.split(":")[1].trim())); //$NON-NLS-1$
											activeChannel.remove(recordSetKey);
											
											channels.switchChannel(channelNumber, recordSetKey);
											activeRecordSet = channels.get(channelNumber).get(recordSetKey);
											activeRecordSet.setRecalculationRequired();
											UniLogDialog.this.device.updateVisibilityStatus(activeRecordSet);
											UniLogDialog.this.device.makeInActiveDisplayable(activeRecordSet);
											UniLogDialog.this.application.updateDataTable();
										}
									}
								}
							}
						}
					});
				} // end tabs

				int index = Channels.getInstance().getActiveChannelNumber();
				this.deviceConfigTabFolder.setSelection(index < 1 || index > this.deviceConfigTabFolder.getChildren().length-2 ? 1 : index);
				//this.deviceConfigTabFolder.addMouseTrackListener(UniLogDialog.this.mouseTrackerEnterFadeOut);
				this.deviceConfigTabFolder.addMouseTrackListener(new MouseTrackAdapter() {
					public void mouseEnter(MouseEvent evt) {
						log.finer("deviceConfigTabFolder.mouseEnter, event=" + evt); //$NON-NLS-1$
						fadeOutAplhaBlending(evt, UniLogDialog.this.getDialogShell().getClientArea(), 10, 10, -10, -10);
					}
					public void mouseHover(MouseEvent evt) {
						log.finest("deviceConfigTabFolder.mouseHover, event=" + evt); //$NON-NLS-1$
					}
					public void mouseExit(MouseEvent evt) {
						log.finer("deviceConfigTabFolder.mouseExit, event=" + evt); //$NON-NLS-1$
						fadeInAlpaBlending(evt, UniLogDialog.this.getDialogShell().getClientArea(), 10, 10, -10, -10);
					}
				});
				
				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x/2-320, 100));
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
			UniLogDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * update the configuration tab with values red 
	 * @param readBuffer
	 */
	public void updateConfigurationValues(byte[] readBuffer) {
		//status field
		this.memoryUsed = ((readBuffer[6] & 0xFF) << 8) + (readBuffer[7] & 0xFF);
		UniLogDialog.log.finer("memoryUsed = " + this.memoryUsed); //$NON-NLS-1$

		this.unilogVersion = String.format("v%.2f", new Double(readBuffer[8] & 0xFF) / 100); //$NON-NLS-1$
		UniLogDialog.log.finer("unilogVersion = " + this.unilogVersion); //$NON-NLS-1$
		this.firmwareVersionLabel.setText(this.unilogVersion);

		int memoryDeleted = readBuffer[9] & 0xFF;
		int tmpMemoryUsed = 0;
		if (memoryDeleted > 0)
			tmpMemoryUsed = 0;
		else
			tmpMemoryUsed = this.memoryUsed;
		this.memoryUsedPercent = String.format("%.2f", tmpMemoryUsed * 100.0 / UniLogDialog.WERTESAETZE_MAX); //$NON-NLS-1$
		UniLogDialog.log.finer("memoryUsedPercent = " + this.memoryUsedPercent + " (" + tmpMemoryUsed + "/" + UniLogDialog.WERTESAETZE_MAX + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		this.memUsagePercent.setText(this.memoryUsedPercent);

		// timer interval
		this.timeIntervalPosition = readBuffer[10] & 0xFF;
		UniLogDialog.log.finer("timeIntervalPosition = " + this.timeIntervalPosition + " timeInterval = "); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.timeIntervalPosition != this.timeIntervalCombo.getSelectionIndex()) {
			this.timeIntervalCombo.select(this.timeIntervalPosition);
			updateTimeStep_ms(this.timeIntervalPosition);
		}

		// motor/prop
		this.isMotorPole = false;
		this.isPropBlade = false;
		this.countMotorPole = 0;
		this.countPropBlade = 0;
		if ((readBuffer[11] & 0x80) == 0) {
			this.isPropBlade = true;
		}
		else {
			this.isMotorPole = true;
		}
		this.countPropBlade = readBuffer[11] & 0x7F;
		this.countMotorPole = (readBuffer[11] & 0x7F) * 2;
		UniLogDialog.log.finer("isPropBlade = " + this.isPropBlade + " countPropBlade = " + this.countPropBlade); //$NON-NLS-1$ //$NON-NLS-2$
		UniLogDialog.log.finer("isMotorPole = " + this.isMotorPole + " countMotorPole = " + this.countMotorPole); //$NON-NLS-1$ //$NON-NLS-2$
		this.numberPolsButton.setSelection(this.isMotorPole);
		this.numberPropButton.setSelection(this.isPropBlade);
		//motorPoleCombo.setItems(new String[] {"2", "4", "6", "8", "10", "12", "14", "16"});
		this.motorPoleCombo.select(this.countMotorPole / 2 - 1);
		//numbeProbCombo.setItems(new String[] {"1", "2", "3", "4"});
		this.numbeProbCombo.select(this.countPropBlade - 1);
		if (this.isMotorPole) {
			this.numbeProbCombo.setEnabled(false);
			this.motorPoleCombo.setEnabled(true);
		}
		else {
			this.numbeProbCombo.setEnabled(true);
			this.motorPoleCombo.setEnabled(false);
		}

		this.isAutoStartCurrent = false;
		this.currentAutoStart = 0;
		if ((readBuffer[12] & 0x80) != 0) {
			this.isAutoStartCurrent = true;
		}
		this.currentAutoStart = readBuffer[12] & 0x7F;
		UniLogDialog.log.finer("isAutoStartCurrent = " + this.isAutoStartCurrent + " currentAutoStart = " + this.currentAutoStart); //$NON-NLS-1$ //$NON-NLS-2$
		this.currentTriggerButton.setSelection(this.isAutoStartCurrent);
		this.currentTriggerCombo.select(this.currentAutoStart - 1);

		this.isAutStartRx = false;
		this.isRxOn = false;
		this.rxAutoStartValue = 0;
		if ((readBuffer[13] & 0x80) != 0) {
			this.isAutStartRx = true;
		}
		this.rxAutoStartValue = (readBuffer[13] & 0x7F); // 16 = 1.6 ms (value - 11 = position in RX_AUTO_START_MS)
		this.impulseTriggerCombo.select(this.rxAutoStartValue - 11);
		this.impulseTriggerButton.setSelection(this.isAutStartRx);
		UniLogDialog.log.finer("isAutStartRx = " + this.isAutStartRx + " isRxOn = " + this.isRxOn + " rxAutoStartValue = " + this.rxAutoStartValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		this.isImpulseAutoStartTime = false;
		this.impulseAutoStartTime_sec = 0;
		if ((readBuffer[14] & 0x80) != 0) {
			this.isImpulseAutoStartTime = true;
		}
		this.impulseAutoStartTime_sec = readBuffer[14] & 0x7F;
		UniLogDialog.log.finer("isAutoStartTime = " + this.isImpulseAutoStartTime + " timeAutoStart_sec = " + this.impulseAutoStartTime_sec); //$NON-NLS-1$ //$NON-NLS-2$
		this.timeTriggerButton.setSelection(this.isImpulseAutoStartTime);
		this.timeTriggerCombo.select(this.impulseAutoStartTime_sec + 1);
		this.timeTriggerCombo.setText(String.format("%4s", this.impulseAutoStartTime_sec)); //$NON-NLS-1$

		this.currentSensorPosition = readBuffer[15] & 0xFF;
		UniLogDialog.log.finer("currentSensor = " + this.currentSensorPosition); //$NON-NLS-1$
		this.sensorCurrentCombo.select(this.currentSensorPosition);

		this.serialNumber = "" + (((readBuffer[16] & 0xFF) << 8) + (readBuffer[17] & 0xFF)); //$NON-NLS-1$
		UniLogDialog.log.finer("serialNumber = " + this.serialNumber); //$NON-NLS-1$
		this.snLabel.setText(this.serialNumber);

		this.modusA1Position = (readBuffer[18] & 0xFF) <= 3 ? (readBuffer[18] & 0xFF) : 0;
		UniLogDialog.log.finer("modusA1 = " + this.modusA1Position); //$NON-NLS-1$
		this.a1ModusCombo.select(this.modusA1Position);

		// skip 19, 20 not used

		this.gearRatio = (readBuffer[21] & 0xFF) / 10.0;
		UniLogDialog.log.finer(String.format("gearRatio = %.1f", this.gearRatio)); //$NON-NLS-1$
		this.gearFactorCombo.setText(String.format(" %.1f  :  1", this.gearRatio)); //$NON-NLS-1$

		if (this.configTab1 != null) this.configTab1.setA1ModusAvailable(true);
		if (this.configTab2 != null) this.configTab2.setA1ModusAvailable(true);
		if (this.configTab3 != null) this.configTab3.setA1ModusAvailable(true);
		if (this.configTab4 != null) this.configTab4.setA1ModusAvailable(true);
	}

	public byte[] buildUpdateBuffer() {
		int checkSum = 0;
		byte[] updateBuffer = new byte[15];
		updateBuffer[0] = (byte) 0xC0;
		updateBuffer[1] = (byte) 0x03;
		checkSum = checkSum + (0xFF & updateBuffer[1]);
		updateBuffer[2] = (byte) 0x02;
		checkSum = checkSum + (0xFF & updateBuffer[2]);

		updateBuffer[3] = (byte) this.timeIntervalCombo.getSelectionIndex();
		checkSum = checkSum + (0xFF & updateBuffer[3]);

		if (this.numberPolsButton.getSelection()) // isMotorPole
			updateBuffer[4] = (byte) ((this.motorPoleCombo.getSelectionIndex() + 1) | 0x80);
		else
			updateBuffer[4] = (byte) ((this.numbeProbCombo.getSelectionIndex() + 1));
		checkSum = checkSum + (0xFF & updateBuffer[4]);

		if (this.currentTriggerButton.getSelection()) // isCurrentTriggerAutoStart
			updateBuffer[5] = (byte) ((this.currentTriggerCombo.getSelectionIndex() + 1) | 0x80);
		else
			updateBuffer[5] = (byte) (this.currentTriggerCombo.getSelectionIndex() + 1);
		checkSum = checkSum + (0xFF & updateBuffer[5]);

		if (this.impulseTriggerButton.getSelection())
			if ((this.impulseTriggerCombo.getSelectionIndex() + 1) == UniLogDialog.RX_AUTO_START_MS.length) // "RX an"
				updateBuffer[6] = (byte) 0x80;
			else
				updateBuffer[6] = (byte) ((this.impulseTriggerCombo.getSelectionIndex() | 0x80) + 11);
		else if ((this.impulseTriggerCombo.getSelectionIndex() + 1) == UniLogDialog.RX_AUTO_START_MS.length) // "RX an"
			updateBuffer[6] = (byte) 0x00;
		else
			updateBuffer[6] = (byte) (this.impulseTriggerCombo.getSelectionIndex() + 11);
		checkSum = checkSum + (0xFF & updateBuffer[6]);

		if (this.timeTriggerButton.getSelection())
			updateBuffer[7] = (byte) ((new Byte(this.timeTriggerCombo.getText().trim())) | 0x80);
		else
			updateBuffer[7] = new Byte(this.timeTriggerCombo.getText().trim());
		checkSum = checkSum + (0xFF & updateBuffer[7]);

		updateBuffer[8] = (byte) this.sensorCurrentCombo.getSelectionIndex();
		checkSum = checkSum + (0xFF & updateBuffer[8]);

		updateBuffer[9] = (byte) this.a1ModusCombo.getSelectionIndex();
		checkSum = checkSum + (0xFF & updateBuffer[9]);

		updateBuffer[10] = 0x00; // ignore 
		updateBuffer[11] = 0x00; // ignore

		double tempGearRatio = new Double(this.gearFactorCombo.getText().split(":")[0].trim().replace(',', '.')).doubleValue() * 10; //$NON-NLS-1$
		updateBuffer[12] = (byte) tempGearRatio;
		checkSum = checkSum + (0xFF & updateBuffer[12]);

		updateBuffer[13] = 0x00; // ignore

		updateBuffer[14] = (byte) (checkSum % 256);

		if (UniLogDialog.log.isLoggable(Level.FINE)) {
			StringBuilder sb = new StringBuilder();
			sb.append("updateBuffer = ["); //$NON-NLS-1$
			for (int i = 0; i < updateBuffer.length; i++) {
				if (i == updateBuffer.length - 1)
					sb.append(String.format("%02X", updateBuffer[i])); //$NON-NLS-1$
				else
					sb.append(String.format("%02X ", updateBuffer[i])); //$NON-NLS-1$
			}
			sb.append("]"); //$NON-NLS-1$
			UniLogDialog.log.fine(sb.toString());
		}

		return updateBuffer;
	}

	/**
	 * @param newStatus the status text to set
	 */
	public void setStatusText(String newStatus) {
		this.statusText = newStatus;
		this.memUsagePercent.setText(this.statusText);
	}

	/**
	 * @return the readDataProgressBar
	 */
	public ProgressBar getReadDataProgressBar() {
		return this.readDataProgressBar;
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
				UniLogDialog.this.readDataProgressBar.setSelection(tmpValue);
			}
		});
	}

	/**
	 * query the rear ratio
	 * @return
	 */
	double getGearRatio() {
		return new Double(this.gearFactorCombo.getText().split(":")[0].trim().replace(',', '.')).doubleValue(); //$NON-NLS-1$
	}

	/**
	 * update the counter number in the dialog, called out of thread
	 * @param redTelegrams
	 * @param numReadErrors
	 */
	public void updateDataGatherProgress(final int redTelegrams, final int numberRecordSet, final int numReadErrors, final int progress) {
		this.numberRedDataSetsText = "" + redTelegrams; //$NON-NLS-1$
		this.numberActualDataSetsText = "" + numberRecordSet; //$NON-NLS-1$
		this.numberReadErrorText = "" + numReadErrors; //$NON-NLS-1$
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				int tmpValue = progress < 0 ? 0 : progress;
				tmpValue = progress > 100 ? 100 : progress;
				UniLogDialog.this.readDataProgressBar.setSelection(tmpValue);
				UniLogDialog.this.redDataSetLabel.setText(UniLogDialog.this.numberRedDataSetsText);
				UniLogDialog.this.actualDataSetNumber.setText(UniLogDialog.this.numberActualDataSetsText);
				UniLogDialog.this.numberReadErrorLabel.setText(UniLogDialog.this.numberReadErrorText);
			}
		});
	}

	/**
	 * function to reset counter labels
	 */
	public void resetDataSetsLabel() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.numberRedDataSetsText = "0"; //$NON-NLS-1$
			this.numberActualDataSetsText = "0"; //$NON-NLS-1$
			this.numberReadErrorText = "0"; //$NON-NLS-1$
			this.redDataSetLabel.setText(this.numberRedDataSetsText);
			this.actualDataSetNumber.setText(this.numberActualDataSetsText);
			this.numberReadErrorLabel.setText(this.numberReadErrorText);
			this.readDataProgressBar.setSelection(0);
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					UniLogDialog.this.numberRedDataSetsText = "0"; //$NON-NLS-1$
					UniLogDialog.this.numberActualDataSetsText = "0"; //$NON-NLS-1$
					UniLogDialog.this.numberReadErrorText = "0"; //$NON-NLS-1$
					UniLogDialog.this.redDataSetLabel.setText(UniLogDialog.this.numberRedDataSetsText);
					UniLogDialog.this.actualDataSetNumber.setText(UniLogDialog.this.numberActualDataSetsText);
					UniLogDialog.this.numberReadErrorLabel.setText(UniLogDialog.this.numberReadErrorText);
					UniLogDialog.this.readDataProgressBar.setSelection(0);
				}
			});
		}
	}

	/**
	 * function to reset all the buttons, normally called after data gathering finished
	 */
	public void resetButtons() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.editConfigButton.setEnabled(false);
			this.useConfigCombo.setEnabled(true);
			
			this.readDataButton.setEnabled(true);
			this.stopDataButton.setEnabled(false);
			
			this.startLoggingButton.setEnabled(true);
			this.stopLoggingButton.setEnabled(false);
			
			this.startLiveGatherButton.setEnabled(true);
			this.stopLiveGatherButton.setEnabled(false);
			
			this.clearMemoryButton.setEnabled(true);
			this.setClosePossible(true);
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					UniLogDialog.this.editConfigButton.setEnabled(false);
					UniLogDialog.this.useConfigCombo.setEnabled(true);
					
					UniLogDialog.this.readDataButton.setEnabled(true);
					UniLogDialog.this.stopDataButton.setEnabled(false);
					
					UniLogDialog.this.startLoggingButton.setEnabled(true);
					UniLogDialog.this.stopLoggingButton.setEnabled(false);
					
					UniLogDialog.this.startLiveGatherButton.setEnabled(true);
					UniLogDialog.this.stopLiveGatherButton.setEnabled(false);
					
					UniLogDialog.this.clearMemoryButton.setEnabled(true);
					UniLogDialog.this.setClosePossible(true);
				}
			});
		}
	}

	/**
	 * enable the button to store the configuration
	 * @param enabled
	 */
	public void enableStoreAdjustmentsButton(boolean enabled) {
		this.storeAdjustmentsButton.setEnabled(enabled);
	}

	/**
	 * query the selection index of the A1 modus type
	 * @return index position of the A1 modus array
	 */
	public int getSelectionIndexA1ModusCombo() {
		return this.a1ModusCombo.getSelectionIndex();
	}

	/**
	 * update the used timeStep_ms, this is used for all type of calculations
	 * @param timeIntervalIndex the index of the TIME_INTERVAL array
	 */
	void updateTimeStep_ms(int timeIntervalIndex) {
		switch (timeIntervalIndex) {
		case 0: // 1/16 sec
			this.device.setTimeStep_ms(1000.0 / 16);
			break;
		case 1: // 1/8 sec
			this.device.setTimeStep_ms(1000.0 / 8);
			break;
		default:
		case 2: // 1/4 sec
			this.device.setTimeStep_ms(1000.0 / 4);
			break;
		case 3: // 1/2 sec
			this.device.setTimeStep_ms(1000.0 / 2);
			break;
		case 4: // 1 sec
			this.device.setTimeStep_ms(1000.0 * 1);
			break;
		case 5: // 2 sec
			this.device.setTimeStep_ms(1000.0 * 2);
			break;
		case 6: // 5 sec
			this.device.setTimeStep_ms(1000.0 * 5);
			break;
		case 7: // 10 sec
			this.device.setTimeStep_ms(1000.0 * 10);
			break;
		}
		this.device.storeDeviceProperties();
	}
}
