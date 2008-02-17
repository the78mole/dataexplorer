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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

import osde.data.Channels;
import osde.device.DeviceDialog;
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
	private Logger												log	= Logger.getLogger(this.getClass().getName());
	private Group channleConfigGroup;
	private Button stopLiveGatherButton;
	private Button editConfigButton;
	private Text memoryDeleteInfo;

	public final static int			WERTESAETZE_MAX							= 25920;
	public final static String[]					TIME_INTERVAL			= { " 1/16 s (-> 27 min)", 
																															" 1/8 s   (-> 54 min)", 
																															" 1/4 s   (-> 1:48 h)", 
																															" 1/2 s   (-> 3:36 h)", 
																															"   1 s    (-> 7:12 h)",
																															"   2 s    (-> 14:24 h)", 
																															"   5 s    (-> 36 h)", 
																															" 10 s   (-> 72 h)" };
	public final static String[]					RX_AUTO_START_MS	= { " 1,1 ms", " 1,2 ms", " 1,3 ms", " 1,4 ms", " 1,5 ms", " 1,6 ms", " 1,7 ms", " 1,8 ms", " 1,9 ms", " Rx an" };
	public final static String[]					CURRENT_SENSOR		= { " 40/80A ", "  150A ", "  400A ", "    20A " };
	public final static String[]					A1_MODUS					= { " Temperatur ", " Millivolt ", " Speed 250 ", " Speed 400 " };

	public final static String NUMBER_CELLS = "number_cells";
	public final static String PROP_N_100_WATT = "prop_n100W";

	private CTabItem											configTabItem1, configTabItem2, configTabItem3, configTabItem4;
	private CCombo												a1ModusCombo;
	private Group													outletA1Group;
	private CCombo												sensorCurrentCombo;
	private Group													currentSensotGroup;
	private CCombo												impulseTriggerCombo;
	private CCombo												timeTriggerCombo;
	private Button												numberPolsButton;
	private CCombo												currentTriggerCombo;
	private Button												impulseTriggerButton;
	private Button												timeTriggerButton;
	private Button												currentTriggerButton;
	private Group													autoStartGroup;
	private Text													gearFactorCombo;
	private CLabel												gearLabel;
	private CCombo												numbeProbCombo;
	private CCombo												motorPoleCombo;
	private Button												numberPropButton;
	private Group													motorPropGroup;
	private Button												storeAdjustmentsButton;

	private CCombo												useConfigCombo;
	private CLabel												firmwareVersionLabel;
	private CLabel												firmwareText;
	private CLabel												snLabel;
	private CLabel												serialNumberText;
	private CLabel												memUsageUnit;
	private CLabel												memoryUsageText;
	private Slider												gearRatioSlider;
	private Button												stopDataButton;
	private Group													liveDataCaptureGroup;
	private Button												clearMemoryButton;
	private Group													clearDataBufferGroup;
	private Button												startLiveGatherButton;
	private Button												stopLoggingButton;
	private Button												startLoggingButton;
	private Group													loggingGroup;
	private CLabel												numberReadErrorLabel;
	private CLabel												readDataErrorLabel;
	private CLabel												redDataSetLabel;
	private CLabel												dataSetLabel;
	private ProgressBar										readDataProgressBar;
	private Button												readDataButton;
	private Group													dataReadGroup;
	private Composite											dataMainComposite;
	private CTabItem											dataTabItem;
	private Group													statusGroup;
	private CLabel												memUsagePercent;
	private CCombo												timeIntervalCombo;
	private Group													dataRateGroup;
	private Composite											composite1;
	private Button												readAdjustmentButton;
	private CTabItem											baseConfigTabItem;
	private CTabFolder										configTabFolder;
	
	private final UniLogSerialPort				serialPort;			// open/close port execute getData()....
	private final OpenSerialDataExplorer	application;		// interaction with application instance
	private final UniLog									device;					// get device specific things, get serial port, ...
	private DataGathererThread						gatherThread;
	private LiveGathererThread						liveThread;
	private String 												liveRecordName;
	private UniLogConfigTab								configTab1, configTab2, configTab3, configTab4;

	private String												statusText					= "";
	private String												serialNumber				= "";
	private String												unilogVersion				= "";
	private String												memoryUsedPercent		= "0";
	private int														sliderPosition			= 50;
	private String[]											configurationNames	= new String[] { " Konfig 1" };
	private String												numberRedDataSetsText			= "0";
	private String												numberReadErrorText	= "0";
	private int														channelSelectionIndex = 0;
	
	/**
	* main method to test this dialog inside a shell 
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			UniLog device = new UniLog("c:\\Documents and Settings\\user\\Application Data\\OpenSerialDataExplorer\\Geraete\\Htronic Akkumaster C4.ini");
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
	 * @param device device specific class implementation
	 */
	public UniLogDialog(Shell parent, UniLog device) {
		super(parent);
		this.serialPort = device.getSerialPort();
		this.device = device;
		this.application = OpenSerialDataExplorer.getInstance();
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	public void open() {
		try {
			log.fine("dialogShell.isDisposed() " + ((dialogShell == null) ? "null" : dialogShell.isDisposed()));
			if (dialogShell == null || dialogShell.isDisposed()) {
				dialogShell = new Shell(new Shell(SWT.MODELESS), SWT.DIALOG_TRIM);
				dialogShell.setText("UniLog ToolBox");
				dialogShell.setImage(SWTResourceManager.getImage("osde/resource/Tools.gif"));
				SWTResourceManager.registerResourceUser(dialogShell);
				dialogShell.setLayout(new FormLayout());
				dialogShell.layout();
				dialogShell.pack();
				dialogShell.setSize(642, 400);
				{
					configTabFolder = new CTabFolder(dialogShell, SWT.NONE);
					{
						baseConfigTabItem = new CTabItem(configTabFolder, SWT.NONE);
						baseConfigTabItem.setText("Einstellung");
						{
							composite1 = new Composite(configTabFolder, SWT.NONE);
							baseConfigTabItem.setControl(composite1);
							composite1.setLayout(null);
							{
								statusGroup = new Group(composite1, SWT.NONE);
								statusGroup.setLayout(null);
								statusGroup.setText("Status");
								statusGroup.setBounds(12, 17, 602, 45);
								{
									firmwareText = new CLabel(statusGroup, SWT.NONE);
									firmwareText.setText("Firmware :  ");
									firmwareText.setBounds(146, 15, 79, 22);
								}
								{
									firmwareVersionLabel = new CLabel(statusGroup, SWT.NONE);
									firmwareVersionLabel.setBounds(225, 15, 71, 22);
									firmwareVersionLabel.setText(unilogVersion);
								}
								{
									serialNumberText = new CLabel(statusGroup, SWT.NONE);
									serialNumberText.setText("S/N :");
									serialNumberText.setBounds(12, 15, 42, 22);
								}
								{
									snLabel = new CLabel(statusGroup, SWT.CENTER | SWT.EMBEDDED);
									snLabel.setBounds(52, 15, 88, 22);
									snLabel.setText(serialNumber);
								}
								{
									memoryUsageText = new CLabel(statusGroup, SWT.NONE);
									memoryUsageText.setText("Speicherbelegung");
									memoryUsageText.setBounds(338, 15, 134, 22);
								}
								{
									memUsagePercent = new CLabel(statusGroup, SWT.NONE);
									FormLayout statusLabelLayout = new FormLayout();
									memUsagePercent.setLayout(statusLabelLayout);
									memUsagePercent.setBounds(472, 15, 59, 22);
									memUsagePercent.setText(memoryUsedPercent);
								}
								{
									memUsageUnit = new CLabel(statusGroup, SWT.NONE);
									memUsageUnit.setText("[%]");
									memUsageUnit.setBounds(543, 15, 26, 22);
								}
							}
							{
								outletA1Group = new Group(composite1, SWT.NONE);
								outletA1Group.setLayout(null);
								outletA1Group.setText("A1 Modus");
								outletA1Group.setBounds(337, 200, 277, 59);
								{
									a1ModusCombo = new CCombo(outletA1Group, SWT.BORDER);
									a1ModusCombo.setItems(A1_MODUS);
									a1ModusCombo.select(2);
									a1ModusCombo.setEditable(false);
									a1ModusCombo.setBounds(91, 24, 117, 20);
									a1ModusCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									a1ModusCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST))  log.finest("outletModusCombo.widgetSelected, event="+evt);
											storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
							}
							{
								currentSensotGroup = new Group(composite1, SWT.NONE);
								currentSensotGroup.setLayout(null);
								currentSensotGroup.setText("Stromsensor");
								currentSensotGroup.setBounds(337, 129, 277, 59);
								{
									sensorCurrentCombo = new CCombo(currentSensotGroup, SWT.BORDER);
									sensorCurrentCombo.setBounds(88, 23, 119, 20);
									sensorCurrentCombo.setItems(CURRENT_SENSOR);
									sensorCurrentCombo.select(2);
									sensorCurrentCombo.setEditable(false);
									sensorCurrentCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									sensorCurrentCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST))  log.finest("sensorCurrentCombo.widgetSelected, event="+evt);
											storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
							}
							{
								autoStartGroup = new Group(composite1, SWT.NONE);
								autoStartGroup.setLayout(null);
								autoStartGroup.setText("Logging Autostart");
								autoStartGroup.setBounds(12, 219, 300, 99);
								{
									currentTriggerButton = new Button(autoStartGroup, SWT.CHECK | SWT.LEFT);
									currentTriggerButton.setText("bei Stromschwelle");
									currentTriggerButton.setBounds(34, 22, 150, 18);
									currentTriggerButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST))  log.finest("currentTriggerButton.widgetSelected, event="+evt);
											storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									timeTriggerButton = new Button(autoStartGroup, SWT.CHECK | SWT.LEFT);
									timeTriggerButton.setText("Zeitschwelle");
									timeTriggerButton.setBounds(34, 46, 150, 18);
									timeTriggerButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST))  log.finest("timeTriggerButton.widgetSelected, event="+evt);
											storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									impulseTriggerButton = new Button(autoStartGroup, SWT.CHECK | SWT.LEFT);
									impulseTriggerButton.setText("RC-Signallänge");
									impulseTriggerButton.setBounds(34, 70, 150, 18);
									impulseTriggerButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST))  log.finest("impulseTriggerButton.widgetSelected, event="+evt);
											storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									currentTriggerCombo = new CCombo(autoStartGroup, SWT.BORDER);
									currentTriggerCombo.setBounds(200, 22, 80, 20);
									currentTriggerCombo.setItems(new String[] {"  1", "  2", "  3", "  4", "  5", "  6", "  7", "  8", "  9", " 10", "15", "20", "25", "30", "35", "40", "45", "50"});
									currentTriggerCombo.select(2);
									currentTriggerCombo.setEditable(false);
									currentTriggerCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									currentTriggerCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST))  log.finest("currentTriggerCombo.widgetSelected, event="+evt);
											storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									timeTriggerCombo = new CCombo(autoStartGroup, SWT.BORDER);
									timeTriggerCombo.setBounds(200, 46, 80, 20);
									timeTriggerCombo.setItems(new String[] {"  1", "  2", "  3", "  4", "  5", "  6", "  7", "  8", "  9", " 10", " 11", " 12", " 13", " 14", " 15", " 16", " 17", " 18", " 19", " 20", " 30", " 60", " 120" });
									timeTriggerCombo.select(16);
									timeTriggerCombo.setEditable(true);
									timeTriggerCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									timeTriggerCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST))  log.finest("timeTriggerCombo.widgetSelected, event="+evt);
											storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									impulseTriggerCombo = new CCombo(autoStartGroup, SWT.BORDER);
									impulseTriggerCombo.setBounds(200, 70, 80, 20);
									impulseTriggerCombo.setEditable(false);
									impulseTriggerCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									impulseTriggerCombo.setItems(RX_AUTO_START_MS);
									impulseTriggerCombo.select(4);
									impulseTriggerCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST))  log.finest("rcTriggerCombo.widgetSelected, event="+evt);
											storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
							}
							{
								motorPropGroup = new Group(composite1, SWT.NONE);
								motorPropGroup.setLayout(null);
								motorPropGroup.setText("Drehzahlsensor");
								motorPropGroup.setBounds(12, 115, 300, 96);
								{
									numberPolsButton = new Button(motorPropGroup, SWT.RADIO | SWT.LEFT);
									numberPolsButton.setText("Motorpole");
									numberPolsButton.setBounds(31, 20, 150, 20);
									numberPolsButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST))  log.finest("numberPolsButton.widgetSelected, event="+evt);
											storeAdjustmentsButton.setEnabled(true);
											if (numberPolsButton.getSelection()) {
												numbeProbCombo.setEnabled(false);
												motorPoleCombo.setEnabled(true);
											}
											else {
												numbeProbCombo.setEnabled(true);
												motorPoleCombo.setEnabled(false);
												numberPropButton.setSelection(false);
											}
										}
									});
								}
								{
									motorPoleCombo = new CCombo(motorPropGroup, SWT.BORDER);
									motorPoleCombo.setBounds(198, 20, 63, 20);
									motorPoleCombo.setItems(new String[] {"  2", "  4", "  6", "  8", " 10", " 12", " 14", " 16"});
									motorPoleCombo.select(6);
									motorPoleCombo.setEditable(false);
									motorPoleCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									motorPoleCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST))  log.finest("motorPoleCombo.widgetSelected, event="+evt);
											storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									gearLabel = new CLabel(motorPropGroup, SWT.LEFT);
									gearLabel.setBounds(31, 42, 150, 20);
									gearLabel.setText("Getriebeuntersetzung");
								}
								{
									gearFactorCombo = new Text(motorPropGroup, SWT.LEFT | SWT.BORDER);
									gearFactorCombo.setBounds(198, 44, 63, 20);		
									gearFactorCombo.setText(" 1.0  :  1");
									gearFactorCombo.setEditable(false);
									gearFactorCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
								}
								{
									gearRatioSlider = new Slider(motorPropGroup, SWT.VERTICAL);
									gearRatioSlider.setBounds(263, 39, 21, 30);
									gearRatioSlider.setMinimum(0);
									gearRatioSlider.setMaximum(100);
									gearRatioSlider.setSelection(sliderPosition);
									gearRatioSlider.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST))  log.finest("gearRatioSlider.widgetSelected, event="+evt);
											storeAdjustmentsButton.setEnabled(true);
											if (gearRatioSlider.getSelection() > sliderPosition) {
												//" 1.0  :  1"
												gearFactorCombo.setText(String.format(" %.1f  :  1", getGearRatio() - 0.1));
												++sliderPosition;
											}
											else {
												gearFactorCombo.setText(String.format(" %.1f  :  1", getGearRatio() + 0.1));
												--sliderPosition;
											}
										}
									});
								}
								{
									numberPropButton = new Button(motorPropGroup, SWT.RADIO | SWT.LEFT);
									numberPropButton.setText("Propellerblätter");
									numberPropButton.setBounds(31, 68, 150, 18);
									numberPropButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST))  log.finest("numberPropButton.widgetSelected, event="+evt);
											storeAdjustmentsButton.setEnabled(true);
											if (numberPropButton.getSelection()) {
												numbeProbCombo.setEnabled(true);
												motorPoleCombo.setEnabled(false);
											}
											else {
												numbeProbCombo.setEnabled(false);
												motorPoleCombo.setEnabled(true);
												numberPolsButton.setSelection(false);
											}
										}
									});
								}
								{
									numbeProbCombo = new CCombo(motorPropGroup, SWT.BORDER);
									numbeProbCombo.setBounds(198, 68, 63, 18);
									numbeProbCombo.setItems(new String[] {" 1", " 2", " 3", " 4"});
									numbeProbCombo.select(1);
									numbeProbCombo.setEditable(false);
									numbeProbCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									numbeProbCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST))  log.finest("numbeProbCombo.widgetSelected, event="+evt);
											storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
							}
							{
								dataRateGroup = new Group(composite1, SWT.NONE);
								dataRateGroup.setLayout(null);
								dataRateGroup.setText("Speicherrate");
								dataRateGroup.setBounds(337, 71, 277, 46);
								{
									timeIntervalCombo = new CCombo(dataRateGroup, SWT.BORDER);
									timeIntervalCombo.setItems(TIME_INTERVAL);
									timeIntervalCombo.setBounds(70, 17, 136, 20);
									timeIntervalCombo.select(1);
									timeIntervalCombo.setEditable(false);
									timeIntervalCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									timeIntervalCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST))  log.finest("timeRateCombo.widgetSelected, event="+evt);
											storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
							}
							{
								readAdjustmentButton = new Button(composite1, SWT.PUSH | SWT.FLAT | SWT.CENTER);
								readAdjustmentButton.setText("Einstellungen auslesen");
								readAdjustmentButton.setBounds(12, 74, 300, 29);
								//readAdjustmentButton.setBackground(SWTResourceManager.getColor(64, 128, 64));
								readAdjustmentButton.setForeground(SWTResourceManager.getColor(205, 227, 196));
								readAdjustmentButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("readAdjustmentButton.widgetSelected, event="+evt);
										try {
											updateConfigurationValues(serialPort.readConfiguration());
										}
										catch (Exception e) {
											application.openMessageDialog(e.getMessage());
										}
										
									}
								});
							}
							{
								storeAdjustmentsButton = new Button(composite1, SWT.PUSH | SWT.CENTER);
								storeAdjustmentsButton.setText("Einstellungen speichern");
								storeAdjustmentsButton.setBounds(335, 276, 281, 31);
								storeAdjustmentsButton.setEnabled(false);
								storeAdjustmentsButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("storeAdjustmentsButton.widgetSelected, event="+evt);
										try {
											if(serialPort.setConfiguration(buildUpdateBuffer())) {
												storeAdjustmentsButton.setEnabled(false);
											}
											else {
												storeAdjustmentsButton.setEnabled(true);
											}
										}
										catch (Exception e) {
											application.openMessageDialog(e.getMessage());
										}
									}
								});
							}
						}
					}
					configTabFolder.setSelection(0);
				}
				{ // begin measurement configuration tab
					if(device.getChannelCount() > 0) {
						configTabItem1 = new CTabItem(configTabFolder, SWT.NONE);
						configTabItem1.setText(device.getChannelName(1));
						configTab1 = new UniLogConfigTab(configTabFolder, device, device.getChannelName(1));
						configTabItem1.setControl(configTab1);
					}
					if(device.getChannelCount() > 1) {
						configTabItem2 = new CTabItem(configTabFolder, SWT.NONE);
						configTabItem2.setText(device.getChannelName(2));
						configTab2 = new UniLogConfigTab(configTabFolder, device, device.getChannelName(2));
						configTabItem2.setControl(configTab2);
					}
					if(device.getChannelCount() > 2) {
						configTabItem3 = new CTabItem(configTabFolder, SWT.NONE);
						configTabItem3.setText(device.getChannelName(3));
						configTab3 = new UniLogConfigTab(configTabFolder, device, device.getChannelName(3));
						configTabItem3.setControl(configTab3);
					}
					if(device.getChannelCount() > 3) {
						configTabItem4 = new CTabItem(configTabFolder, SWT.NONE);
						configTabItem4.setText(device.getChannelName(4));
						configTab4 = new UniLogConfigTab(configTabFolder, device, device.getChannelName(4));
						configTabItem4.setControl(configTab4);
					}
					
				} // end measurement configuration tab
				{ // begin data tab
					dataTabItem = new CTabItem(configTabFolder, SWT.NONE);
					dataTabItem.setText("Daten I/O");
					{
						dataMainComposite = new Composite(configTabFolder, SWT.NONE);
						dataMainComposite.setLayout(null);
						dataTabItem.setControl(dataMainComposite);
						{
							channleConfigGroup = new Group(dataMainComposite, SWT.NONE);
							channleConfigGroup.setLayout(null);
							channleConfigGroup.setBounds(18, 12, 290, 58);
							channleConfigGroup.setText("Kanalkonfiguration der Daten");
							{
								useConfigCombo = new CCombo(channleConfigGroup, SWT.BORDER);
								useConfigCombo.setBounds(24, 24, 140, 20);
								useConfigCombo.setItems(configurationNames);
								useConfigCombo.select(channelSelectionIndex);
								useConfigCombo.setEditable(false);
								useConfigCombo.setTextLimit(18);
								useConfigCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
								useConfigCombo.setToolTipText("Hier wird die Konfiguration gewählt, die den Datensäten zugeordnet werden soll");
								useConfigCombo.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("useConfigCombo.keyReleased, event="+evt);
										if (evt.character == SWT.CR) {
											String configName = useConfigCombo.getText();
											device.setChannelName(configName, channelSelectionIndex + 1);
											configurationNames[channelSelectionIndex] = configName;
											useConfigCombo.select(channelSelectionIndex);
											dataReadGroup.redraw();
											Channels.getInstance().get(channelSelectionIndex + 1).setName(" " + (channelSelectionIndex + 1) + " : " + configName);
											application.getMenuToolBar().updateChannelSelector();
											switch (channelSelectionIndex) {
											case 0: //configTab1
											configTabItem1.setText(configName);
											configTab1.setConfigName(configName);
											break;
											case 1: //configTab2
											configTabItem2.setText(configName);
											configTab2.setConfigName(configName);
											break;
											case 2: //configTab3												
											configTabItem3.setText(configName);
											configTab3.setConfigName(configName);
											break;
											case 3: //configTab4
											configTabItem4.setText(configName);
											configTab4.setConfigName(configName);
											break;
											}
											configTabFolder.redraw();
											editConfigButton.setEnabled(false);
											device.storeDeviceProperties();
										}
									}
								});
								useConfigCombo.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("useConfigCombo.widgetSelected, event="+evt);
										readDataButton.setEnabled(true);
										startLoggingButton.setEnabled(true);
										startLiveGatherButton.setEnabled(true);
										channelSelectionIndex = useConfigCombo.getSelectionIndex();
										useConfigCombo.select(channelSelectionIndex);
										editConfigButton.setEnabled(true);
										resetDataSetsLabel();
									}
								});
							}
							{
								editConfigButton = new Button(channleConfigGroup, SWT.PUSH | SWT.CENTER);
								editConfigButton.setBounds(172, 23, 106, 23);
								editConfigButton.setText("Name ändern");
								editConfigButton.setEnabled(false);
								editConfigButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("editConfigButton.widgetSelected, event="+evt);
										useConfigCombo.setEditable(true);
										editConfigButton.setEnabled(false);
									}
								});
							}
						}
						{
							dataReadGroup = new Group(dataMainComposite, SWT.NONE);
							dataReadGroup.setLayout(null);
							dataReadGroup.setBounds(18, 78, 290, 242);
							dataReadGroup.setText("Daten auslesen");
							dataReadGroup.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									if (log.isLoggable(Level.FINEST))  log.finest("dataReadGroup.paintControl, event="+evt);
									int index = useConfigCombo.getSelectionIndex();
									configurationNames = new String[device.getChannelCount()];
									for (int i = 0; i < configurationNames.length; i++) {
										configurationNames[i] = " " + device.getChannelName(i+1);
									}
									useConfigCombo.setItems(configurationNames);
									useConfigCombo.select(index);
								}
							});
							{
								readDataButton = new Button(dataReadGroup, SWT.PUSH | SWT.CENTER);
								readDataButton.setText("Start Daten auslesen");
								readDataButton.setBounds(11, 40, 260, 30);
								readDataButton.setEnabled(false);
								readDataButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("readDataButton.widgetSelected, event="+evt);
										String channelName = " " + (useConfigCombo.getSelectionIndex() + 1) + " : " + useConfigCombo.getText();
										gatherThread = new DataGathererThread(application, device, serialPort, channelName);
										gatherThread.start();
										
										readDataButton.setEnabled(false);
										editConfigButton.setEnabled(false);
										stopDataButton.setEnabled(true);
										startLoggingButton.setEnabled(false);
										stopLoggingButton.setEnabled(false);
										useConfigCombo.setEnabled(false);
										startLiveGatherButton.setEnabled(false);
										clearMemoryButton.setEnabled(false);
									}
								});
							}
							{
								dataSetLabel = new CLabel(dataReadGroup, SWT.NONE);
								dataSetLabel.setBounds(22, 85, 180, 20);
								dataSetLabel.setText("gelesene Telegramme     :");
							}
							{
								redDataSetLabel = new CLabel(dataReadGroup, SWT.RIGHT);
								redDataSetLabel.setBounds(205, 85, 55, 20);
								redDataSetLabel.setText("0");
							}
							{
								readDataErrorLabel = new CLabel(dataReadGroup, SWT.NONE);
								readDataErrorLabel.setBounds(22, 111, 180, 20);
								readDataErrorLabel.setText("Datenübertragungsfehler  :");
							}
							{
								numberReadErrorLabel = new CLabel(dataReadGroup, SWT.RIGHT);
								numberReadErrorLabel.setBounds(205, 111, 55, 20);
								numberReadErrorLabel.setText("0");
							}
							{
								readDataProgressBar = new ProgressBar(dataReadGroup, SWT.NONE);
								readDataProgressBar.setBounds(15, 158, 260, 15);
								readDataProgressBar.setMinimum(0);
								readDataProgressBar.setMaximum(100);
							}
							{
								stopDataButton = new Button(dataReadGroup, SWT.PUSH | SWT.CENTER);
								stopDataButton.setBounds(15, 194, 260, 30);
								stopDataButton.setText("S T O P");
								stopDataButton.setEnabled(false);
								stopDataButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										log.fine("stopDataButton.widgetSelected, event="+evt);
										if(gatherThread != null && gatherThread.isAlive()) {
											serialPort.setTransmitFinished(true);
											gatherThread.setThreadStop(true);
										}
										resetButtons();
									}
								});
							}
						}
						{
							liveDataCaptureGroup = new Group(dataMainComposite, SWT.NONE);
							liveDataCaptureGroup.setLayout(null);
							liveDataCaptureGroup.setBounds(328, 12, 280, 198);
							liveDataCaptureGroup.setText("Datenaufzeichnung");
							{
								startLiveGatherButton = new Button(liveDataCaptureGroup, SWT.PUSH | SWT.CENTER);
								startLiveGatherButton.setText("Start live Datenabfrage");
								startLiveGatherButton.setBounds(17, 26, 246, 30);
								startLiveGatherButton.setEnabled(false);
								startLiveGatherButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										log.fine("liveViewButton.widgetSelected, event=" + evt);
										try {
											String channelName = " " + (useConfigCombo.getSelectionIndex() + 1) + " : " + useConfigCombo.getText();
											liveThread = new LiveGathererThread(application, device, serialPort, channelName);
											liveRecordName = liveThread.startTimerThread();
											startLiveGatherButton.setEnabled(false);
											stopLiveGatherButton.setEnabled(true);
										}
										catch (Exception e) {
											log.log(Level.SEVERE, e.getMessage(), e);
											application.openMessageDialog("Bei der Livedatenabfrage ist eine Fehler aufgetreten !");
										}
									}
								});
							}
							{
								stopLiveGatherButton = new Button(liveDataCaptureGroup, SWT.PUSH | SWT.CENTER);
								stopLiveGatherButton.setBounds(17, 156, 246, 30);
								stopLiveGatherButton.setText("Stop live Datenabfrage");
								stopLiveGatherButton.setEnabled(false);
								stopLiveGatherButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										log.fine("stopLiveGatherButton.widgetSelected, event=" + evt);
										if (liveThread != null) {
											liveThread.stopTimerThread();
											liveThread.finalizeRecordSet(liveRecordName);
										}
										stopLiveGatherButton.setEnabled(false);
										startLiveGatherButton.setEnabled(true);
									}
								});
							}
						}
						{
							loggingGroup = new Group(liveDataCaptureGroup, SWT.NONE);
							loggingGroup.setLayout(null);
							loggingGroup.setBounds(25, 70, 228, 70);
							loggingGroup.setText("UniLog Datenaufnahme");
							{
								startLoggingButton = new Button(loggingGroup, SWT.PUSH | SWT.CENTER);
								startLoggingButton.setText("Start");
								startLoggingButton.setBounds(12, 27, 100, 30);
								startLoggingButton.setEnabled(false);
								startLoggingButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										log.fine("startLoggingButton.widgetSelected, event=" + evt);
										try {
											serialPort.startLogging();
											startLoggingButton.setEnabled(false);
											stopLoggingButton.setEnabled(true);
										}
										catch (Exception e) {
											log.log(Level.SEVERE, e.getMessage(), e);
											application.openMessageDialog("Es ist ein Fehler in der seriellen Kommunikation zum Gerät aufgetreten : " + e.getClass().getCanonicalName() + " - " + e.getMessage());
										}
									}
								});
							}
							{
								stopLoggingButton = new Button(loggingGroup, SWT.PUSH | SWT.CENTER);
								stopLoggingButton.setText("Stop");
								stopLoggingButton.setBounds(116, 27, 100, 30);
								stopLoggingButton.setEnabled(false);
								stopLoggingButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										log.fine("stopLoggingButton.widgetSelected, event=" + evt);
										try {
											serialPort.stopLogging();
											startLoggingButton.setEnabled(true);
											stopLoggingButton.setEnabled(false);
										}
										catch (Exception e) {
											log.log(Level.SEVERE, e.getMessage(), e);
											application.openMessageDialog("Es ist ein Fehler in der seriellen Kommunikation zum Gerät aufgetreten : " + e.getClass().getCanonicalName() + " - " + e.getMessage());
										}
									}
								});
							}
						}
						{
							clearDataBufferGroup = new Group(dataMainComposite, SWT.NONE);
							clearDataBufferGroup.setLayout(null);
							clearDataBufferGroup.setBounds(328, 216, 280, 104);
							clearDataBufferGroup.setText("Datenspeicher");
							{
								clearMemoryButton = new Button(clearDataBufferGroup, SWT.PUSH | SWT.CENTER);
								clearMemoryButton.setText("löschen");
								clearMemoryButton.setBounds(12, 60, 260, 30);
								clearMemoryButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										log.fine("clearMemoryButton.widgetSelected, event="+evt);
										try {
											clearMemoryButton.setEnabled(false);
											serialPort.clearMemory();
										}
										catch (Exception e) {
											log.log(Level.SEVERE, e.getMessage(), e);
											application.openMessageDialog("Bei der Löschoperation ist ein Fehler aufgetreten : " + e.getClass().getCanonicalName() + " - " + e.getMessage());
											e.printStackTrace();
										}
										clearMemoryButton.setEnabled(true);
									}
								});
							}
							{
								memoryDeleteInfo = new Text(clearDataBufferGroup, SWT.CENTER | SWT.WRAP);
								memoryDeleteInfo.setBounds(12, 22, 256, 34);
								memoryDeleteInfo.setText("Löschen wird erst bei der nächsten Datenaufnahme wirksam");
								memoryDeleteInfo.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
								memoryDeleteInfo.setEditable(false);
							}
						}
					}
				} // end measurement configuration tab
				{
					FormData configTabFolderLData = new FormData();
					configTabFolderLData.width = 626;
					configTabFolderLData.height = 355;
					configTabFolderLData.left =  new FormAttachment(0, 1000, 0);
					configTabFolderLData.top =  new FormAttachment(0, 1000, 0);
					configTabFolderLData.right =  new FormAttachment(1000, 1000, 0);
					configTabFolderLData.bottom =  new FormAttachment(1000, 1000, 0);
					configTabFolder.setLayoutData(configTabFolderLData);
				}
				dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						log.fine("dialogShell.widgetDisposed, event=" + evt);
						if (configTab1.getConfigButtonStatus() && configTab2.getConfigButtonStatus() && configTab3.getConfigButtonStatus() && configTab4.getConfigButtonStatus()) {
							String msg = "Eine Konfiguration wurde verändert, soll die geänderte Konfiguration abgespeichert werde ?";
							if (application.openYesNoMessageDialog(msg) == SWT.YES) {
								log.fine("SWT.YES");
								device.storeDeviceProperties();
							}
						}
					}
				});
				dialogShell.setLocation(getParent().toDisplay(100, 100));
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
	 * update the configuration tab with values red 
	 * @param readBuffer
	 */
	public void updateConfigurationValues(byte[] readBuffer) {
		//status field
		int memoryUsed = ((readBuffer[6] & 0xFF) << 8) + (readBuffer[7] & 0xFF);
		log.finer("memoryUsed = " + memoryUsed);

		unilogVersion = String.format("v%.2f", new Double(readBuffer[8] & 0xFF) / 100);
		log.finer("unilogVersion = " + unilogVersion);
		firmwareVersionLabel.setText(unilogVersion);

		int memoryDeleted = readBuffer[9] & 0xFF;
		int tmpMemoryUsed = 0;
		if (memoryDeleted > 0)
			tmpMemoryUsed = 0;
		else
			tmpMemoryUsed = memoryUsed;
		memoryUsedPercent = String.format("%.2f", tmpMemoryUsed * 100.0 / WERTESAETZE_MAX);
		log.finer("memoryUsedPercent = " + memoryUsedPercent + " (" + tmpMemoryUsed + "/" + WERTESAETZE_MAX + ")");
		memUsagePercent.setText(memoryUsedPercent);

		// timer interval
		int timeIntervalPosition = readBuffer[10] & 0xFF;
		log.finer("timeIntervalPosition = " + timeIntervalPosition + " timeInterval = " );
		timeIntervalCombo.select(timeIntervalPosition);

		// motor/prop
		boolean								isMotorPole							= false;
		boolean								isPropBlade							= false;
		int										countMotorPole					= 0;
		int										countPropBlade					= 0;
		if ((readBuffer[11] & 0x80) == 0) {
			isPropBlade = true;
		}
		else {
			isMotorPole = true;
		}
		countPropBlade = readBuffer[11] & 0x7F;
		countMotorPole = (readBuffer[11] & 0x7F) * 2;
		log.finer("isPropBlade = " + isPropBlade + " countPropBlade = " + countPropBlade);
		log.finer("isMotorPole = " + isMotorPole + " countMotorPole = " + countMotorPole);
		numberPolsButton.setSelection(isMotorPole);
		numberPropButton.setSelection(isPropBlade);
		//motorPoleCombo.setItems(new String[] {"2", "4", "6", "8", "10", "12", "14", "16"});
		motorPoleCombo.select(countMotorPole/2-1);
		//numbeProbCombo.setItems(new String[] {"1", "2", "3", "4"});
		numbeProbCombo.select(countPropBlade-1);
		if (isMotorPole) {
			numbeProbCombo.setEnabled(false);
			motorPoleCombo.setEnabled(true);
		}
		else {
			numbeProbCombo.setEnabled(true);
			motorPoleCombo.setEnabled(false);
		}

		boolean								isAutoStartCurrent			= false;
		int										currentAutoStart				= 0;
		if ((readBuffer[12] & 0x80) != 0) {
			isAutoStartCurrent = true;
		}
		currentAutoStart = readBuffer[12] & 0x7F;
		log.finer("isAutoStartCurrent = " + isAutoStartCurrent + " currentAutoStart = " + currentAutoStart);
		currentTriggerButton.setSelection(isAutoStartCurrent);
		currentTriggerCombo.select(currentAutoStart-1);

		boolean								isAutStartRx						= false;
		boolean								isRxOn									= false;
		int rxAutoStartValue = 0;
		if ((readBuffer[13] & 0x80) != 0) {
			isAutStartRx = true;
		}
		rxAutoStartValue = (readBuffer[13] & 0x7F); // 16 = 1.6 ms (value - 11 = position in RX_AUTO_START_MS)
		impulseTriggerCombo.select(rxAutoStartValue-11);
		impulseTriggerButton.setSelection(isAutStartRx);
		log.finer("isAutStartRx = " + isAutStartRx + " isRxOn = " + isRxOn + " rxAutoStartValue = " + rxAutoStartValue);

		boolean								isImpulseAutoStartTime					= false;
		int										impulseAutoStartTime_sec				= 0;
		if ((readBuffer[14] & 0x80) != 0) {
			isImpulseAutoStartTime = true;
		}
		impulseAutoStartTime_sec = readBuffer[14] & 0x7F;
		log.finer("isAutoStartTime = " + isImpulseAutoStartTime + " timeAutoStart_sec = " + impulseAutoStartTime_sec);
		timeTriggerButton.setSelection(isImpulseAutoStartTime);
		timeTriggerCombo.select(impulseAutoStartTime_sec + 1);
		timeTriggerCombo.setText(String.format("%4s", impulseAutoStartTime_sec));

		int currentSensorPosition = readBuffer[15] & 0xFF;
		log.finer("currentSensor = " + currentSensorPosition);
		sensorCurrentCombo.select(currentSensorPosition);

		serialNumber = "" + (((readBuffer[16] & 0xFF) << 8) + (readBuffer[17] & 0xFF));
		log.finer("serialNumber = " + serialNumber);
		snLabel.setText(serialNumber);


		int modusA1Position = (readBuffer[18] & 0xFF) <= 3 ? (readBuffer[18] & 0xFF) : 0;
		log.finer("modusA1 = " + modusA1Position);
		a1ModusCombo.select(modusA1Position);
		
		// skip 19, 20 not used
		
		double gearRatio = (readBuffer[21] & 0xFF) / 10.0;
		log.finer(String.format("gearRatio = %.1f", gearRatio));
		gearFactorCombo.setText(String.format(" %.1f  :  1", gearRatio));
		
		if (configTab1 != null) configTab1.setA1ModusAvailable(true); 
		if (configTab2 != null) configTab2.setA1ModusAvailable(true); 
		if (configTab3 != null) configTab3.setA1ModusAvailable(true); 
		if (configTab4 != null) configTab4.setA1ModusAvailable(true); 
	}
	
	public byte[] buildUpdateBuffer() {
		int checkSum = 0;
		byte[] updateBuffer = new byte[15];
		updateBuffer[0] = (byte) 0xC0;
		updateBuffer[1] = (byte) 0x03;
		checkSum = checkSum + (0xFF & updateBuffer[1]);
		updateBuffer[2] = (byte) 0x02; 
		checkSum = checkSum + (0xFF & updateBuffer[2]);
		
		updateBuffer[3] = (byte) timeIntervalCombo.getSelectionIndex();
		checkSum = checkSum + (0xFF & updateBuffer[3]);
		
		if (numberPolsButton.getSelection())  // isMotorPole
			updateBuffer[4] = (byte) ((motorPoleCombo.getSelectionIndex() + 1) | 0x80);
		else 
			updateBuffer[4] = (byte) ((numbeProbCombo.getSelectionIndex() + 1));
		checkSum = checkSum + (0xFF & updateBuffer[4]);
		
		if (currentTriggerButton.getSelection()) // isCurrentTriggerAutoStart
			updateBuffer[5] = (byte) ((currentTriggerCombo.getSelectionIndex() + 1) | 0x80);		
		else
			updateBuffer[5] = (byte) (currentTriggerCombo.getSelectionIndex() + 1);
		checkSum = checkSum + (0xFF & updateBuffer[5]);
		
		if (impulseTriggerButton.getSelection())
			if ((impulseTriggerCombo.getSelectionIndex()+1) ==  RX_AUTO_START_MS.length) // "RX an"
				updateBuffer[6] = (byte) 0x80;
			else
				updateBuffer[6] = (byte) ((impulseTriggerCombo.getSelectionIndex() | 0x80) + 11);
		else
			if ((impulseTriggerCombo.getSelectionIndex()+1) ==  RX_AUTO_START_MS.length) // "RX an"
				updateBuffer[6] = (byte) 0x00;
			else
				updateBuffer[6] = (byte) (impulseTriggerCombo.getSelectionIndex() + 11);
		checkSum = checkSum + (0xFF & updateBuffer[6]);
				
		if (timeTriggerButton.getSelection())
			updateBuffer[7] = (byte)((new Byte(timeTriggerCombo.getText().trim())) | 0x80);
		else
			updateBuffer[7] = new Byte(timeTriggerCombo.getText().trim());
		checkSum = checkSum + (0xFF & updateBuffer[7]);
		
		updateBuffer[8] = (byte) sensorCurrentCombo.getSelectionIndex();
		checkSum = checkSum + (0xFF & updateBuffer[8]);
		
		updateBuffer[9] = (byte) a1ModusCombo.getSelectionIndex();
		checkSum = checkSum + (0xFF & updateBuffer[9]);
		
		updateBuffer[10] = 0x00; // ignore 
		updateBuffer[11] = 0x00; // ignore
		
		double tempGearRatio = new Double(gearFactorCombo.getText().split(":")[0].trim()).doubleValue() * 10;
		updateBuffer[12] = (byte) tempGearRatio;
		checkSum = checkSum + (0xFF & updateBuffer[12]);
		
		updateBuffer[13] = 0x00; // ignore
		
		updateBuffer[14] = (byte) (checkSum % 256);
		
		if(log.isLoggable(Level.FINE)) {
			StringBuilder sb = new StringBuilder();
			sb.append("updateBuffer = [");
			for (int i = 0; i < updateBuffer.length; i++) {
				if (i == updateBuffer.length - 1)
					sb.append(String.format("%02X", updateBuffer[i]));
				else
					sb.append(String.format("%02X ", updateBuffer[i]));
			}
			sb.append("]");
			log.fine(sb.toString());
		}

		return updateBuffer;
	}
	/**
	 * @param memUsagePercent the memUsagePercent to set
	 */
	public void setStatusText(String newStatus) {
		this.statusText = newStatus;
		memUsagePercent.setText(this.statusText);
	}

	/**
	 * @return the readDataProgressBar
	 */
	public ProgressBar getReadDataProgressBar() {
		return readDataProgressBar;
	}

	/**
	 * set progress bar to value between 0 to 100
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
	 * query the rear ratio
	 * @return
	 */
	private double getGearRatio() {
		return new Double(gearFactorCombo.getText().split(":")[0].trim()).doubleValue();
	}

	
	
	public void updateDataGatherProgress(final int redTelegrams, final int numReadErrors) {
		numberRedDataSetsText = "" + redTelegrams;
		numberReadErrorText = "" + numReadErrors;
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				redDataSetLabel.setText(numberRedDataSetsText);
				numberReadErrorLabel.setText(numberReadErrorText);
			}
		});
	}
	
	/**
	 * function to reset counter labels
	 */
	public void resetDataSetsLabel() {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				numberRedDataSetsText = "0";
				numberReadErrorText = "0";
				redDataSetLabel.setText(numberRedDataSetsText);
				numberReadErrorLabel.setText(numberReadErrorText);
				readDataProgressBar.setSelection(0);
			}
		});
	}
	
	/**
	 * function to reset all the buttons, normally called after data gathering finished
	 */
	public void resetButtons() {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				readDataButton.setEnabled(false);
				editConfigButton.setEnabled(false);
				stopDataButton.setEnabled(false);
				startLoggingButton.setEnabled(false);
				stopLoggingButton.setEnabled(false);
				useConfigCombo.setEnabled(true);
				startLiveGatherButton.setEnabled(false);
				clearMemoryButton.setEnabled(true);
			}
		});
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
	
}
