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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
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

	private CLabel												a3ModusLabel;
	private CLabel												a2ModusLabel;
	private CLabel												a3Label;
	private CLabel												a2Label;
	private Group													a2a3ModusGroup;
	private CLabel												useConfigLabel;
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
	private Button												liveViewButton;
	private CLabel												orangeLabel;
	private CLabel												greenLabel;
	private CLabel												redLabel;
	private Composite											dataCaptureComposite;
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
	private UniLogConfigTab								configTab1, configTab2, configTab3, configTab4;

	private String												statusText					= "";
	private String												serialNumber				= "";
	private String												unilogVersion				= "";
	private String												memoryUsedPercent		= "0";
	private int														sliderPosition			= 50;
	private String[]											configurationNames	= new String[] { " Konfig 1" };
	private String												redDataSetsText			= "0";
	private String												numberReadErrorText	= "0";

	
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
				dialogShell.setSize(320, 572);
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
								statusGroup.setBounds(12, 7, 283, 65);
								{
									firmwareText = new CLabel(statusGroup, SWT.NONE);
									firmwareText.setText("Firmware :  ");
									firmwareText.setBounds(142, 15, 75, 22);
								}
								{
									firmwareVersionLabel = new CLabel(statusGroup, SWT.NONE);
									firmwareVersionLabel.setBounds(217, 15, 39, 22);
									firmwareVersionLabel.setText(unilogVersion);
								}
								{
									serialNumberText = new CLabel(statusGroup, SWT.NONE);
									serialNumberText.setText("S/N :");
									serialNumberText.setBounds(12, 15, 42, 22);
								}
								{
									snLabel = new CLabel(statusGroup, SWT.CENTER | SWT.EMBEDDED);
									snLabel.setBounds(48, 15, 88, 22);
									snLabel.setText(serialNumber);
								}
								{
									memoryUsageText = new CLabel(statusGroup, SWT.NONE);
									memoryUsageText.setText("Speicherbelegung");
									memoryUsageText.setBounds(12, 36, 134, 22);
								}
								{
									memUsagePercent = new CLabel(statusGroup, SWT.NONE);
									FormLayout statusLabelLayout = new FormLayout();
									memUsagePercent.setLayout(statusLabelLayout);
									memUsagePercent.setBounds(146, 36, 59, 22);
									memUsagePercent.setText(memoryUsedPercent);
								}
								{
									memUsageUnit = new CLabel(statusGroup, SWT.NONE);
									memUsageUnit.setText("[%]");
									memUsageUnit.setBounds(217, 36, 26, 22);
								}
							}
							{
								outletA1Group = new Group(composite1, SWT.NONE);
								outletA1Group.setLayout(null);
								outletA1Group.setText("A1 Modus");
								outletA1Group.setBounds(12, 386, 134, 64);
								{
									a1ModusCombo = new CCombo(outletA1Group, SWT.BORDER);
									a1ModusCombo.setItems(A1_MODUS);
									a1ModusCombo.select(2);
									a1ModusCombo.setEditable(false);
									a1ModusCombo.setBounds(16, 26, 100, 18);
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
								currentSensotGroup.setBounds(183, 127, 113, 49);
								{
									sensorCurrentCombo = new CCombo(currentSensotGroup, SWT.BORDER);
									sensorCurrentCombo.setBounds(17, 21, 74, 18);
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
								autoStartGroup.setBounds(12, 286, 284, 93);
								{
									currentTriggerButton = new Button(autoStartGroup, SWT.CHECK | SWT.LEFT);
									currentTriggerButton.setText("bei Stromschwelle");
									currentTriggerButton.setBounds(34, 20, 150, 18);
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
									timeTriggerButton.setBounds(34, 42, 150, 18);
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
									impulseTriggerButton.setBounds(34, 64, 150, 18);
									impulseTriggerButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											if (log.isLoggable(Level.FINEST))  log.finest("impulseTriggerButton.widgetSelected, event="+evt);
											storeAdjustmentsButton.setEnabled(true);
										}
									});
								}
								{
									currentTriggerCombo = new CCombo(autoStartGroup, SWT.BORDER);
									currentTriggerCombo.setBounds(184, 20, 60, 18);
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
									timeTriggerCombo.setBounds(184, 42, 60, 18);
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
									impulseTriggerCombo.setBounds(184, 64, 60, 18);
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
								motorPropGroup.setBounds(13, 187, 283, 89);
								{
									numberPolsButton = new Button(motorPropGroup, SWT.RADIO | SWT.LEFT);
									numberPolsButton.setText("Motorpole");
									numberPolsButton.setBounds(31, 20, 150, 18);
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
									motorPoleCombo.setBounds(181, 20, 50, 18);
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
									gearLabel.setBounds(31, 40, 150, 20);
									gearLabel.setText("Getriebeuntersetzung");
								}
								{
									gearFactorCombo = new Text(motorPropGroup, SWT.LEFT | SWT.BORDER);
									gearFactorCombo.setBounds(181, 42, 50, 18);		
									gearFactorCombo.setText(" 1.0  :  1");
									gearFactorCombo.setEditable(false);
									gearFactorCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
								}
								{
									gearRatioSlider = new Slider(motorPropGroup, SWT.VERTICAL);
									gearRatioSlider.setBounds(233, 38, 17, 26);
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
									numberPropButton.setBounds(31, 64, 150, 18);
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
									numbeProbCombo.setBounds(181, 64, 50, 18);
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
								dataRateGroup.setBounds(12, 127, 159, 49);
								{
									timeIntervalCombo = new CCombo(dataRateGroup, SWT.BORDER);
									timeIntervalCombo.setItems(TIME_INTERVAL);
									timeIntervalCombo.setBounds(20, 21, 122, 18);
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
								readAdjustmentButton.setBounds(12, 84, 281, 29);
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
								storeAdjustmentsButton.setBounds(12, 467, 281, 31);
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
							{
								a2a3ModusGroup = new Group(composite1, SWT.NONE);
								a2a3ModusGroup.setLayout(null);
								a2a3ModusGroup.setBounds(158, 386, 138, 64);
								a2a3ModusGroup.setText("A2, A3 Modus");
//								a2a3ModusGroup.addPaintListener(new PaintListener() {
//									public void paintControl(PaintEvent evt) {
//										if (log.isLoggable(Level.FINEST))  log.finest("powerGroup.paintControl, event=" + evt);
//										String recordKey = device.getMeasurementNames()[12];
//										MeasurementType measurement = device.getMeasurementDefinition(configName, recordKey);
//										a2ModusLabel.setText(measurement.getName());
//										recordKey = device.getMeasurementNames()[13];
//										measurement = device.getMeasurementDefinition(configName, recordKey);
//										a3ModusLabel.setText(measurement.getName());
//									}
//								});
								{
									a2Label = new CLabel(a2a3ModusGroup, SWT.NONE);
									a2Label.setText("A2 :");
									a2Label.setBounds(8, 15, 28, 20);
								}
								{
									a3Label = new CLabel(a2a3ModusGroup, SWT.NONE);
									a3Label.setBounds(8, 34, 28, 21);
									a3Label.setText("A3:");
								}
								{
									a2ModusLabel = new CLabel(a2a3ModusGroup, SWT.NONE);
									a2ModusLabel.setBounds(36, 15, 90, 19);
								}
								{
									a3ModusLabel = new CLabel(a2a3ModusGroup, SWT.NONE);
									a3ModusLabel.setBounds(36, 34, 90, 21);
								}
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
							dataReadGroup = new Group(dataMainComposite, SWT.NONE);
							dataReadGroup.setLayout(null);
							dataReadGroup.setBounds(14, 12, 280, 204);
							dataReadGroup.setText("Daten auslesen");
							dataReadGroup.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									if (log.isLoggable(Level.FINEST))  log.finest("dataReadGroup.paintControl, event="+evt);
									configurationNames = new String[device.getChannelCount()];
									for (int i = 0; i < configurationNames.length; i++) {
										configurationNames[i] = " " + device.getChannelName(i+1);
									}
								}
							});
							{
								useConfigLabel = new CLabel(dataReadGroup, SWT.NONE);
								useConfigLabel.setBounds(12, 18, 150, 20);
								useConfigLabel.setText("verwende Konfiguration");
								useConfigLabel.setToolTipText("Hier wird die Konfiguration gewählt, die den Datensäten zugeordnet werden soll");
							}
							{
								useConfigCombo = new CCombo(dataReadGroup, SWT.BORDER);
								useConfigCombo.setBounds(165, 20, 103, 20);
								useConfigCombo.setItems(configurationNames);
								useConfigCombo.select(0);
								useConfigCombo.setEditable(false);
								useConfigCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
								useConfigCombo.setToolTipText("Hier wird die Konfiguration gewählt, die den Datensäten zugeordnet werden soll");
								useConfigCombo.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("useConfigCombo.widgetSelected, event="+evt);
										readDataButton.setEnabled(true);
									}
								});
							}
							{
								readDataButton = new Button(dataReadGroup, SWT.PUSH | SWT.CENTER);
								readDataButton.setText("Start Daten auslesen");
								readDataButton.setBounds(10, 51, 260, 25);
								readDataButton.setEnabled(false);
								readDataButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("readDataButton.widgetSelected, event="+evt);
										gatherThread = new DataGathererThread(application, device, serialPort, useConfigCombo.getText());
										gatherThread.start();
										
										stopDataButton.setEnabled(true);
										startLoggingButton.setEnabled(false);
										stopLoggingButton.setEnabled(false);
										useConfigCombo.setEnabled(false);
										liveViewButton.setEnabled(false);
										clearMemoryButton.setEnabled(false);
									}
								});
							}
							{
								dataSetLabel = new CLabel(dataReadGroup, SWT.NONE);
								dataSetLabel.setBounds(25, 91, 180, 20);
								dataSetLabel.setText("gelesene Telegramme     :");
							}
							{
								redDataSetLabel = new CLabel(dataReadGroup, SWT.RIGHT);
								redDataSetLabel.setBounds(200, 91, 55, 20);
								redDataSetLabel.setText("0");
							}
							{
								readDataErrorLabel = new CLabel(dataReadGroup, SWT.NONE);
								readDataErrorLabel.setBounds(25, 111, 180, 20);
								readDataErrorLabel.setText("Datenübertragungsfehler  :");
							}
							{
								numberReadErrorLabel = new CLabel(dataReadGroup, SWT.RIGHT);
								numberReadErrorLabel.setBounds(200, 111, 55, 20);
								numberReadErrorLabel.setText("0");
							}
							{
								readDataProgressBar = new ProgressBar(dataReadGroup, SWT.NONE);
								readDataProgressBar.setBounds(10, 141, 260, 15);
								readDataProgressBar.setMinimum(0);
								readDataProgressBar.setMaximum(100);
							}
							{
								stopDataButton = new Button(dataReadGroup, SWT.PUSH | SWT.CENTER);
								stopDataButton.setBounds(10, 166, 260, 25);
								stopDataButton.setText("S T O P");
								stopDataButton.setEnabled(false);
							}
						}
						{
							loggingGroup = new Group(dataMainComposite, SWT.NONE);
							loggingGroup.setLayout(null);
							loggingGroup.setBounds(14, 221, 281, 125);
							loggingGroup.setText("Daten aufnehmen");
							{
								startLoggingButton = new Button(loggingGroup, SWT.PUSH | SWT.CENTER);
								startLoggingButton.setText("Start logging");
								startLoggingButton.setBounds(10, 20, 260, 25);
							}
							{
								dataCaptureComposite = new Composite(loggingGroup, SWT.BORDER);
								FillLayout dataCaptureCompositeLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
								dataCaptureComposite.setLayout(dataCaptureCompositeLayout);
								dataCaptureComposite.setBounds(50, 55, 185, 20);
								{
									redLabel = new CLabel(dataCaptureComposite, SWT.CENTER | SWT.EMBEDDED | SWT.BORDER);
									redLabel.setText("red");
								}
								{
									orangeLabel = new CLabel(dataCaptureComposite, SWT.CENTER | SWT.EMBEDDED | SWT.BORDER);
									orangeLabel.setText("orange");
								}
								{
									greenLabel = new CLabel(dataCaptureComposite, SWT.CENTER | SWT.EMBEDDED | SWT.BORDER);
									greenLabel.setText("green");
								}
							}
							{
								stopLoggingButton = new Button(loggingGroup, SWT.PUSH | SWT.CENTER);
								stopLoggingButton.setText("Stop logging");
								stopLoggingButton.setBounds(10, 85, 260, 25);
								stopLoggingButton.setEnabled(false);
							}
						}
						{
							clearDataBufferGroup = new Group(dataMainComposite, SWT.NONE);
							clearDataBufferGroup.setLayout(null);
							clearDataBufferGroup.setBounds(14, 421, 280, 64);
							clearDataBufferGroup.setText("Datenspeicher");
							{
								clearMemoryButton = new Button(clearDataBufferGroup, SWT.PUSH | SWT.CENTER);
								clearMemoryButton.setText("löschen");
								clearMemoryButton.setBounds(10, 24, 260, 25);
							}
						}
						{
							liveDataCaptureGroup = new Group(dataMainComposite, SWT.NONE);
							liveDataCaptureGroup.setLayout(null);
							liveDataCaptureGroup.setBounds(14, 352, 280, 63);
							liveDataCaptureGroup.setText("Datenabfrage");
							{
								liveViewButton = new Button(liveDataCaptureGroup, SWT.PUSH | SWT.CENTER);
								liveViewButton.setText("live Datenanzeige");
								liveViewButton.setBounds(10, 24, 260, 25);
							}
						}
					}
				} // end measurement configuration tab
				{
					FormData configTabFolderLData = new FormData(346, 515);
					configTabFolderLData.width = 346;
					configTabFolderLData.height = 515;
					configTabFolderLData.left =  new FormAttachment(0, 1000, 0);
					configTabFolderLData.top =  new FormAttachment(0, 1000, 0);
					configTabFolderLData.right =  new FormAttachment(1000, 1000, 0);
					configTabFolderLData.bottom =  new FormAttachment(1000, 1000, 0);
					configTabFolder.setLayoutData(configTabFolderLData);
					configTabFolder.setSize(346, 515);
				}
//				dialogShell.addFocusListener(new FocusAdapter() {
//					public void focusGained(FocusEvent evt) {
//						log.fine("dialogShell.focusGained, event=" + evt);
//						// this is only placed in the focus listener as hint, do not forget place this query 
//						if (!serialPort.isConnected()) {
//							application.openMessageDialog("Der serielle Port ist nicht geöffnet!");
//						}
//					}
//				});
				dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						log.fine("dialogShell.widgetDisposed, event=" + evt);
						if (configTab1.getConfigButtonStatus()) {
							//TODO meherere config tabs jede config abfragen
							String msg = "Eine Konfiguration wurde verändert, soll die geänderte Konfiguration abgespeichert werde ?";
							if (application.openYesNoMessageDialog(msg) == SWT.YES) {
								log.fine("SWT.YES");
								device.store();
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
		gearFactorCombo.setText(String.format(" %.1f  :  1", gearRatio + 0.1));
		
		configTab1.setA1ModusAvailable(true); //TODO more config tabs
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
		redDataSetsText = "" + redTelegrams;
		numberReadErrorText = "" + numReadErrors;
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				redDataSetLabel.setText(redDataSetsText);
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
				redDataSetsText = "0";
				numberReadErrorText = "0";
				redDataSetLabel.setText(redDataSetsText);
				numberReadErrorLabel.setText(numberReadErrorText);
			}
		});
	}
	
	/**
	 * function to enable all the read data read buttons, normally called after data gathering finished
	 */
	public void enableReadButtons() {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				stopDataButton.setEnabled(false);
				startLoggingButton.setEnabled(true);
				stopLoggingButton.setEnabled(false);
				useConfigCombo.setEnabled(true);
				liveViewButton.setEnabled(true);
				clearMemoryButton.setEnabled(true);
			}
		});
	}
	
	/**
	 * @return number of cells
	 */
	public int getNumCell() {
		return this.configTab1.getNumCell();
	}
	
	/**
	 * @return prop revolution at 100 W
	 */
	public int getPropN100Value() {
		return this.configTab1.getPropN100Value();
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
