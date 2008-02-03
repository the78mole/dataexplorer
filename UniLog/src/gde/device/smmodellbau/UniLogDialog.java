package osde.device.smmodellbau;

import java.io.IOException;
import java.util.HashMap;
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

import osde.data.Channels;
import osde.device.DataCalculationType;
import osde.device.DeviceDialog;
import osde.device.MeasurementType;
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
	
	private final static int			WERTESAETZE_MAX					= 25920;
	private final static String[]					TIME_INTERVAL			= { " 1/16 s (-> 27 min)", 
																															" 1/8 s   (-> 54 min)", 
																															" 1/4 s   (-> 1:48 h)", 
																															" 1/2 s   (-> 3:36 h)", 
																															"   1 s    (-> 7:12 h)",
																															"   2 s    (-> 14:24 h)", 
																															"   5 s    (-> 36 h)", 
																															" 10 s   (-> 72 h)" };
	private final static String[]					RX_AUTO_START_MS	= { " 1,1 ms", " 1,2 ms", " 1,3 ms", " 1,4 ms", " 1,5 ms", " 1,6 ms", " 1,7 ms", " 1,8 ms", " 1,9 ms", " Rx an" };
	private final static String[]					CURRENT_SENSOR		= { " 40/80A ", "  150A ", "  400A ", "    20A " };
	private final static String[]					A1_MODUS					= { " Temperatur ", " Millivolt ", " Speed 250 ", " Speed 400 " };


	private Text													a3Unit;
	private Text													a2Unit;
	private Text													a2Symbol;
	private Text													a1Unit;
	private Text													a1Symbol;
	private CLabel												receiverVoltageUnit;
	private CLabel												receiverVoltageSymbol;
	private CLabel												etaUnit;
	private CLabel												etaSymbol;
	private CLabel												revolutionUnit;
	private CLabel												revolutionSymbol;
	private CLabel												slopeUnit;
	private CLabel												slopeSymbol;
	private CLabel												heightUnit;
	private CLabel												heightSymbol;
	private CLabel												voltagePerCellUnit;
	private CLabel												voltagePerCell;
	private CLabel												energyUnit;
	private CLabel												energySymbol;
	private CLabel												powerUnit;
	private CLabel												powerSymbol;
	private CLabel												capacityUnitLabel;
	private CLabel												capacitySignLabel;
	private CLabel												currentUnit;
	private CLabel												currentSymbol;
	private CLabel												voltageUnit;
	private CLabel												voltageSymbol;
	private CLabel												cellVoltageButton;
	private CLabel												etaButton;
	private CLabel												slopeButton;
	private CLabel												energyButton;
	private CLabel												powerButton;
	private Button												a3ValueButton;
	private Button												a1ValueButton;
	private Button												a2ValueButton;
	private Button												reveiverVoltageButton;
	private Button												revolutionButton;
	private Button												heightButton;
	private CLabel												capacityButton;
	private Button												currentButton;
	private Composite											mainTabComposite;
	private Button												voltageButton;
	private CTabItem											configTabItem;
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
	private Button												a23ExternModus;
	private CLabel												a3ModusLabel;
	private CLabel												a2ModusLabel;
	private CLabel												a3Label;
	private CLabel												a2Label;
	private Group													a2a3ModusGroup;
	private CLabel												axName, axUnit, axOffset, axFactor;
	private Text													a1Factor, a2Factor, a3Factor;
	private Text													a1Offset, a2Offset, a3Offset;
	private Text													a3Text;
	private Text													a2Text;
	private Text													a1Text;
	private CLabel												prop100WUnit, numCellLabel;
	private Button												a1UniLogModus;
	private Button												a23InternModus;
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
	private Button												setConfigButton;
	private CLabel												memUsagePercent;
	private Group													axModusGroup;
	private Group													powerGroup;
	private Text													prop100WInput, numCellInput;
	private CLabel												prop100WLabel;
	private Text													a3Symbol;
	private CCombo												timeIntervalCombo;
	private Group													dataRateGroup;
	private Composite											composite1;
	private Button												readAdjustmentButton;
	private CTabItem											baseConfigTabItem;
	private CTabFolder										configTabFolder;
	private final UniLogSerialPort				serialPort;																				// open/close port execute getData()....
	private final OpenSerialDataExplorer	application;																				// interaction with application instance
	private final Channels								channels;																					// interaction with channels, source of all records
	private final UniLog									device;																						// get device specific things, get serial port, ...
	
	private String statusText = "";
	private String serialNumber = "";
	private String unilogVersion = "";
	private String memoryUsedPercent = "0";
	private int sliderPosition = 50;
	private int prop100WValue = 3400;
	private int numCellValue = 12;
	private String[] configurationNames = new String[] {" Konfig 1"};
	private boolean isA1ModusAvailable = false;
	private String redDataSetsText = "0";
	private String numberReadErrorText = "0";
	private DataGathererThread				gatherThread;

	
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
		this.channels = Channels.getInstance();
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
								a2a3ModusGroup.addPaintListener(new PaintListener() {
									public void paintControl(PaintEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("powerGroup.paintControl, event=" + evt);
										String recordKey = device.getMeasurementNames()[12];
										MeasurementType measurement = device.getMeasurementDefinition(recordKey);
										a2ModusLabel.setText(measurement.getName());
										recordKey = device.getMeasurementNames()[13];
										measurement = device.getMeasurementDefinition(recordKey);
										a3ModusLabel.setText(measurement.getName());
									}
								});
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
					configTabItem = new CTabItem(configTabFolder, SWT.NONE);
					configTabItem.setText("Konfig 1");
					{
						mainTabComposite = new Composite(configTabFolder, SWT.NONE);
						configTabItem.setControl(mainTabComposite);
						mainTabComposite.setLayout(null);
						{
							powerGroup = new Group(mainTabComposite, SWT.NONE);
							powerGroup.setBounds(0, 0, 310, 291);
							powerGroup.setLayout(null);
							powerGroup.setText("Versorgung/Antrieb/Höhe");
							powerGroup.setToolTipText("Hier bitte alle Datenkanäle auswählen, die angezeigt werden sollen");
							powerGroup.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									if (log.isLoggable(Level.FINEST))  log.finest("powerGroup.paintControl, event="+evt);
									String recordKey = device.getMeasurementNames()[0];
									MeasurementType measurement = device.getMeasurementDefinition(recordKey);
									reveiverVoltageButton.setSelection(measurement.isActive());
									reveiverVoltageButton.setText(measurement.getName());
									receiverVoltageSymbol.setText(measurement.getSymbol());
									receiverVoltageUnit.setText(measurement.getUnit());

									recordKey = device.getMeasurementNames()[1];
									measurement = device.getMeasurementDefinition(recordKey);
									voltageButton.setSelection(measurement.isActive());
									voltageButton.setText(measurement.getName());
									voltageSymbol.setText(measurement.getSymbol());
									voltageUnit.setText(measurement.getUnit());
									
									recordKey = device.getMeasurementNames()[2];
									measurement = device.getMeasurementDefinition(recordKey);
									currentButton.setSelection(measurement.isActive());
									currentButton.setText(measurement.getName());
									currentSymbol.setText(measurement.getSymbol());
									currentUnit.setText(measurement.getUnit());
									
									// capacity, power, energy
									updateVoltageAndCurrentDependent(voltageButton.getSelection() && currentButton.getSelection());

									recordKey = device.getMeasurementNames()[7];
									measurement = device.getMeasurementDefinition(recordKey);
									revolutionButton.setSelection(measurement.isActive());
									revolutionButton.setText(measurement.getName());
									revolutionSymbol.setText(measurement.getSymbol());
									revolutionUnit.setText(measurement.getUnit());
									
									// n100W value, eta calculation 										
									updateVoltageCurrentRevolutionDependent(voltageButton.getSelection() && currentButton.getSelection() && revolutionButton.getSelection());
									
									recordKey = device.getMeasurementNames()[9];
									measurement = device.getMeasurementDefinition(recordKey);
									heightButton.setSelection(measurement.isActive());
									heightButton.setText(measurement.getName());
									heightSymbol.setText(measurement.getSymbol());
									heightUnit.setText(measurement.getUnit());
									
									updateHeightDependent(heightButton.getSelection());
								}
							});
							{
								reveiverVoltageButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
								reveiverVoltageButton.setBounds(50, 20, 132, 18);
								reveiverVoltageButton.setText("EmpfSpannung");
								reveiverVoltageButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("reveiverVoltageButton.widgetSelected, event="+evt);
										setConfigButton.setEnabled(true);
									}
								});
							}
							{
								receiverVoltageSymbol = new CLabel(powerGroup, SWT.NONE);
								receiverVoltageSymbol.setBounds(188, 18, 40, 20);
								receiverVoltageSymbol.setText("U");
							}
							{
								receiverVoltageUnit = new CLabel(powerGroup, SWT.NONE);
								receiverVoltageUnit.setBounds(228, 18, 40, 20);
								receiverVoltageUnit.setText("[V]");
							}
							{
								voltageButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
								voltageButton.setText("Spannung");
								voltageButton.setBounds(50, 40, 120, 18);
								voltageButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("voltageButton.widgetSelected, event="+evt);
										updateVoltageAndCurrentDependent(voltageButton.getSelection() && currentButton.getSelection());
										setConfigButton.setEnabled(true);
									}
								});
							}
							{
								voltageSymbol = new CLabel(powerGroup, SWT.NONE);
								voltageSymbol.setText("U");
								voltageSymbol.setBounds(190, 38, 40, 20);
							}
							{
								voltageUnit = new CLabel(powerGroup, SWT.NONE);
								voltageUnit.setText("[V]");
								voltageUnit.setBounds(230, 38, 40, 20);
							}
							{
								currentButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
								currentButton.setText("Strom");
								currentButton.setBounds(50, 60, 120, 18);
								currentButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("currentButton.widgetSelected, event="+evt);
										updateVoltageAndCurrentDependent(voltageButton.getSelection() && currentButton.getSelection());
										setConfigButton.setEnabled(true);
									}
								});
							}
							{
								currentSymbol = new CLabel(powerGroup, SWT.NONE);
								currentSymbol.setText(" I");
								currentSymbol.setBounds(190, 58, 40, 20);
							}
							{
								currentUnit = new CLabel(powerGroup, SWT.NONE);
								currentUnit.setText("[A]");
								currentUnit.setBounds(230, 58, 40, 20);
							}
							{
								capacityButton = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
								capacityButton.setText("Ladung");
								capacityButton.setBounds(64, 80, 120, 20);
							}
							{
								capacitySignLabel = new CLabel(powerGroup, SWT.NONE);
								capacitySignLabel.setText("C");
								capacitySignLabel.setBounds(190, 78, 40, 20);
							}
							{
								capacityUnitLabel = new CLabel(powerGroup, SWT.NONE);
								capacityUnitLabel.setText("[Ah]");
								capacityUnitLabel.setBounds(230, 78, 40, 20);
							}
							{
								powerButton = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
								powerButton.setText("Leistung");
								powerButton.setBounds(64, 100, 120, 20);
							}
							{
								powerSymbol = new CLabel(powerGroup, SWT.NONE);
								powerSymbol.setText("P");
								powerSymbol.setBounds(190, 98, 40, 20);
							}
							{
								powerUnit = new CLabel(powerGroup, SWT.NONE);
								powerUnit.setText("[W]");
								powerUnit.setBounds(230, 98, 40, 20);
							}
							{
								energyButton = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
								energyButton.setText("Energie");
								energyButton.setBounds(64, 120, 120, 20);
							}
							{
								energySymbol = new CLabel(powerGroup, SWT.NONE);
								energySymbol.setText("E");
								energySymbol.setBounds(190, 118, 40, 20);
							}
							{
								energyUnit = new CLabel(powerGroup, SWT.NONE);
								energyUnit.setText("[Wh]");
								energyUnit.setBounds(230, 118, 40, 20);
							}
							{
								cellVoltageButton = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
								cellVoltageButton.setText("Spannung/Zelle");
								cellVoltageButton.setBounds(64, 140, 120, 20);
							}
							{
								voltagePerCell = new CLabel(powerGroup, SWT.NONE);
								voltagePerCell.setText("Uc");
								voltagePerCell.setBounds(190, 138, 40, 20);
							}
							{
								voltagePerCellUnit = new CLabel(powerGroup, SWT.NONE);
								voltagePerCellUnit.setText("[V]");
								voltagePerCellUnit.setBounds(230, 138, 40, 20);
							}
							{
								numCellLabel = new CLabel(powerGroup, SWT.LEFT);
								numCellLabel.setBounds(64, 158, 118, 18);
								numCellLabel.setText("Anzahl Akkuzellen");
							}
							{
								numCellInput = new Text(powerGroup, SWT.LEFT);
								numCellInput.setBounds(187, 160, 40, 18);
								numCellInput.setText(" " + numCellValue);
								numCellInput.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("numCellInput.widgetSelected, event="+evt);
										storeAdjustmentsButton.setEnabled(true);
										prop100WValue = new Integer(prop100WInput.getText()).intValue();
										setConfigButton.setEnabled(true);
									}
								});
							}
							{
								revolutionButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
								revolutionButton.setBounds(50, 180, 140, 18);
								revolutionButton.setText("Drehzahl");
								revolutionButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("revolutionButton.widgetSelected, event="+evt);
										updateVoltageCurrentRevolutionDependent(voltageButton.getSelection() && currentButton.getSelection() && revolutionButton.getSelection());
										setConfigButton.setEnabled(true);
									}
								});
							}
							{
								revolutionSymbol = new CLabel(powerGroup, SWT.NONE);
								revolutionSymbol.setBounds(190, 178, 40, 20);
								revolutionSymbol.setText("rpm");
							}
							{
								revolutionUnit = new CLabel(powerGroup, SWT.NONE);
								revolutionUnit.setBounds(230, 178, 40, 20);
								revolutionUnit.setText("1/min");
							}
							{
								prop100WLabel = new CLabel(powerGroup, SWT.LEFT);
								prop100WLabel.setBounds(64, 198, 118, 18);
								prop100WLabel.setText("Propeller n100W");
							}
							{
								prop100WInput = new Text(powerGroup, SWT.LEFT);
								prop100WInput.setBounds(187, 200, 40, 18);
								prop100WInput.setText(" " + prop100WValue);
								prop100WInput.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("prop100WCombo.widgetSelected, event="+evt);
										storeAdjustmentsButton.setEnabled(true);
										prop100WValue = new Integer(prop100WInput.getText()).intValue();
										setConfigButton.setEnabled(true);
									}
								});
							}
							{
								prop100WUnit = new CLabel(powerGroup, SWT.NONE);
								prop100WUnit.setBounds(230, 198, 75, 20);
								prop100WUnit.setText("100W/min");
							}
							{
								etaButton = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
								etaButton.setBounds(64, 220, 108, 20);
								etaButton.setText("Wirkungsgrad");
							}
							{
								etaSymbol = new CLabel(powerGroup, SWT.NONE);
								etaSymbol.setBounds(190, 219, 40, 20);
								etaSymbol.setText("eta");
							}
							{
								etaUnit = new CLabel(powerGroup, SWT.NONE);
								etaUnit.setBounds(230, 218, 40, 20);
								etaUnit.setText("%");
							}
							{
								heightButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
								heightButton.setText("Höhe");
								heightButton.setBounds(50, 240, 120, 18);
								heightButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("heightButton.widgetSelected, event="+evt);
										updateHeightDependent(heightButton.getSelection());
										setConfigButton.setEnabled(true);
									}
								});
							}
							{
								heightSymbol = new CLabel(powerGroup, SWT.NONE);
								heightSymbol.setText("h");
								heightSymbol.setBounds(190, 238, 40, 20);
							}
							{
								heightUnit = new CLabel(powerGroup, SWT.NONE);
								heightUnit.setText("[m]");
								heightUnit.setBounds(230, 238, 40, 20);
							}
							{
								slopeButton = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
								slopeButton.setText("Steigrate");
								slopeButton.setBounds(64, 258, 120, 19);
							}
							{
								slopeSymbol = new CLabel(powerGroup, SWT.NONE);
								slopeSymbol.setText("V");
								slopeSymbol.setBounds(190, 258, 40, 20);
							}
							{
								slopeUnit = new CLabel(powerGroup, SWT.NONE);
								slopeUnit.setText("[m/s]");
								slopeUnit.setBounds(230, 258, 40, 20);
							}
						}
						{
							axModusGroup = new Group(mainTabComposite, SWT.NONE);
							axModusGroup.setLayout(null);
							axModusGroup.setText("A* Konfiguration");
							axModusGroup.setBounds(0, 291, 310, 175);
							axModusGroup.setToolTipText("HIer bitte die Konfiguration für die A* Ausgange festlegen");
							axModusGroup.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									if (log.isLoggable(Level.FINEST))  log.finest("axModusGroup.paintControl, event="+evt);
									String recordKey = device.getMeasurementNames()[11];
									MeasurementType measurement = device.getMeasurementDefinition(recordKey);
									a1ValueButton.setSelection(measurement.isActive());
									a1Text.setText(measurement.getName());
									a1Symbol.setText(measurement.getSymbol());
									a1Unit.setText(measurement.getUnit());
									a1Offset.setText(String.format("%.2f", measurement.getDataCalculation().getOffset()));
									a1Factor.setText(String.format("%.2f", measurement.getDataCalculation().getFactor()));
									
									recordKey = device.getMeasurementNames()[12];
									measurement = device.getMeasurementDefinition(recordKey);
									a2ValueButton.setSelection(measurement.isActive());
									a2Text.setText(measurement.getName());
									a2Symbol.setText(measurement.getSymbol());
									a2Unit.setText(measurement.getUnit());
									a2Offset.setText(String.format("%.2f", measurement.getDataCalculation().getOffset()));
									a2Factor.setText(String.format("%.2f", measurement.getDataCalculation().getFactor()));

									recordKey = device.getMeasurementNames()[13];
									measurement = device.getMeasurementDefinition(recordKey);
									a3ValueButton.setSelection(measurement.isActive());
									a3Text.setText(measurement.getName());
									a3Symbol.setText(measurement.getSymbol());
									a3Unit.setText(measurement.getUnit());
									a3Offset.setText(String.format("%.2f", measurement.getDataCalculation().getOffset()));
									a3Factor.setText(String.format("%.2f", measurement.getDataCalculation().getFactor()));
}
							});
							{
								a1UniLogModus = new Button(axModusGroup, SWT.PUSH | SWT.CENTER);
								a1UniLogModus.setBounds(7, 20, 290, 25);
								a1UniLogModus.setText("A1 Vorgabe aus UniLog Einstellung");
								a1UniLogModus.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("a1UniLogModus.widgetSelected, event="+evt);
										setConfigButton.setEnabled(true);
										try {
											if (!isA1ModusAvailable) {
												updateConfigurationValues(serialPort.readConfiguration());
											}
											a1Text.setText(A1_MODUS[a1ModusCombo.getSelectionIndex()]);
										}
										catch (Exception e) {
											application.openMessageDialog(e.getMessage());
										}
									}
								});
							}
							{
								axName = new CLabel(axModusGroup, SWT.LEFT);
								axName.setBounds(42, 48, 96, 20);
								axName.setText("Bezeichnung");
								axName.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 1, false, false));
							}
							{
								axUnit = new CLabel(axModusGroup, SWT.LEFT);
								axUnit.setBounds(161, 48, 42, 20);
								axUnit.setText("Einheit");
								axUnit.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 1, false, false));
							}
							{
								axOffset = new CLabel(axModusGroup, SWT.LEFT);
								axOffset.setBounds(206, 48, 46, 20);
								axOffset.setText("Offset");
								axOffset.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 1, false, false));
							}
							{
								axFactor = new CLabel(axModusGroup, SWT.LEFT);
								axFactor.setBounds(252, 48, 50, 20);
								axFactor.setText("Factor");
								axFactor.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 1, false, false));
							}
							{
								a1ValueButton = new Button(axModusGroup, SWT.CHECK | SWT.LEFT);
								a1ValueButton.setBounds(7, 67, 35, 18);
								a1ValueButton.setText("A1");
							}
							{
								a1Text = new Text(axModusGroup, SWT.BORDER);
								a1Text.setBounds(42, 67, 100, 18);
								a1Text.setText("Speed 250");
							}
							{
								a1Symbol = new Text(axModusGroup, SWT.CENTER | SWT.BORDER);
								a1Symbol.setBounds(142, 67, 30, 18);
								a1Symbol.setText("A1");
							}
							{
								a1Unit = new Text(axModusGroup, SWT.CENTER | SWT.BORDER);
								a1Unit.setBounds(172, 67, 30, 18);
								a1Unit.setText("°C");
								a1Unit.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 0, false, false));
							}
							{
								a1Offset = new Text(axModusGroup, SWT.BORDER);
								a1Offset.setBounds(202, 67, 50, 18);
								a1Offset.setText("0.0");
							}
							{
								a1Factor = new Text(axModusGroup, SWT.BORDER);
								a1Factor.setBounds(252, 67, 50, 18);
								a1Factor.setText("1.0");
							}
							{
								a2ValueButton = new Button(axModusGroup, SWT.CHECK | SWT.LEFT);
								a2ValueButton.setBounds(7, 87, 35, 18);
								a2ValueButton.setText("A2");
							}
							{
								a2Text = new Text(axModusGroup, SWT.BORDER);
								a2Text.setBounds(42, 87, 100, 18);
								a2Text.setText("servoImpuls");
							}
							{
								a2Symbol = new Text(axModusGroup, SWT.CENTER | SWT.BORDER);
								a2Symbol.setBounds(142, 87, 30, 18);
								a2Symbol.setText("A2");
							}
							{
								a2Unit = new Text(axModusGroup, SWT.CENTER | SWT.BORDER);
								a2Unit.setBounds(172, 87, 30, 18);
								a2Unit.setText("°C");
								a2Unit.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 0, false, false));
							}
							{
								a2Offset = new Text(axModusGroup, SWT.BORDER);
								a2Offset.setBounds(202, 87, 50, 18);
								a2Offset.setText("0.0");
							}
							{
								a2Factor = new Text(axModusGroup, SWT.BORDER);
								a2Factor.setBounds(252, 87, 50, 18);
								a2Factor.setText("1.0");
							}
							{
								a3ValueButton = new Button(axModusGroup, SWT.CHECK | SWT.LEFT);
								a3ValueButton.setBounds(7, 107, 35, 18);
								a3ValueButton.setText("A3");
							}
							{
								a3Text = new Text(axModusGroup, SWT.BORDER);
								a3Text.setBounds(42, 107, 100, 18);
								a3Text.setText("Temperatur");
							}
							{
								a3Symbol = new Text(axModusGroup, SWT.CENTER | SWT.BORDER);
								a3Symbol.setBounds(142, 107, 30, 18);
								a3Symbol.setText("A3");
							}
							{
								a3Unit = new Text(axModusGroup, SWT.CENTER | SWT.BORDER);
								a3Unit.setBounds(172, 107, 30, 18);
								a3Unit.setText("°C");
								a3Unit.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 0, false, false));
							}
							{
								a3Offset = new Text(axModusGroup, SWT.BORDER);
								a3Offset.setBounds(202, 107, 50, 18);
								a3Offset.setText("0.0");
							}
							{
								a3Factor = new Text(axModusGroup, SWT.BORDER);
								a3Factor.setBounds(252, 107, 50, 18);
								a3Factor.setText("1.0");
							}
							{
								a23InternModus = new Button(axModusGroup, SWT.PUSH | SWT.CENTER);
								a23InternModus.setBounds(7, 137, 146, 25);
								a23InternModus.setText("A2/3 Vorgabe intern");
								a23InternModus.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("a23InternModus.widgetSelected, event="+evt);
										setA23Defaults('I');
										setConfigButton.setEnabled(true);
									}
								});
							}
							{
								a23ExternModus = new Button(axModusGroup, SWT.PUSH | SWT.CENTER);
								a23ExternModus.setBounds(159, 137, 139, 26);
								a23ExternModus.setText("A2/3 Vorgabe extern");
								a23ExternModus.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										if (log.isLoggable(Level.FINEST))  log.finest("a23ExternModus.widgetSelected, event="+evt);
										setA23Defaults('E');
										setConfigButton.setEnabled(true);
									}
								});
							}
						}
						{
							setConfigButton = new Button(mainTabComposite, SWT.PUSH | SWT.CENTER);
							setConfigButton.setBounds(45, 477, 210, 25);
							setConfigButton.setText("Konfiguration speichern");
							setConfigButton.setEnabled(false);
							setConfigButton.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									if (log.isLoggable(Level.FINEST))  log.finest("setConfigButton.widgetSelected, event="+evt);
									collectAndUpdateConfiguration();
									device.store();
									setConfigButton.setEnabled(false);
								}
							});
						}
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
							dataReadGroup.setBounds(12, 12, 280, 204);
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
										gatherThread = new DataGathererThread(application, device, serialPort);
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
							loggingGroup.setBounds(12, 216, 281, 125);
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
							clearDataBufferGroup.setBounds(12, 409, 280, 64);
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
							liveDataCaptureGroup.setBounds(12, 340, 280, 63);
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
					FormData configTabFolderLData = new FormData();
					configTabFolderLData.width = 308;
					configTabFolderLData.height = 514;
					configTabFolderLData.left =  new FormAttachment(0, 1000, 0);
					configTabFolderLData.top =  new FormAttachment(0, 1000, 0);
					configTabFolderLData.right =  new FormAttachment(1000, 1000, 0);
					configTabFolderLData.bottom =  new FormAttachment(1000, 1000, 22);
					configTabFolder.setLayoutData(configTabFolderLData);
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
						if (setConfigButton.getEnabled()) {
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
		
		isA1ModusAvailable = true;
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

	/**
	 * load default values for A2 and A3 fields
	 * @param internExtern 'I' intern / 'E' external sensor
	 */
	private void setA23Defaults(int internExtern) {
		String[] a2Values;
		String[] a3Values;
		switch (internExtern) {
		case 'E': // extern
			a2Values = new String[] {"TemperaturA2", "A2", "°C", "0.0", "1.0"};
			a3Values = new String[] {"TemperaturA3", "A3", "°C", "0.0", "1.0"};
			break;
		case 'I':	// intern
		default:
			a2Values = new String[] {"ServoImpuls", "A2", "ms", "0.0", "1.0"};
			a3Values = new String[] {"TempIntern", "A3", "°C", "0.0", "1.0"};
			break;
		}
		a2Text.setText(a2Values[0]);
		a2Symbol.setText(a2Values[1]);
		a2Unit.setText(a2Values[2]);
		a2Offset.setText(a2Values[3]);
		a2Factor.setText(a2Values[4]);
		
		a3Text.setText(a3Values[0]);
		a3Symbol.setText(a3Values[1]);
		a3Unit.setText(a3Values[2]);
		a3Offset.setText(a3Values[3]);
		a3Factor.setText(a3Values[4]);
	}

	/**
	 * enable or disable voltage and current dependent measurement fields
	 * @param enabled true | false
	 */
	private void updateVoltageAndCurrentDependent(boolean enabled) {
		capacityButton.setEnabled(enabled);
		capacitySignLabel.setEnabled(enabled);
		capacityUnitLabel.setEnabled(enabled);
		powerButton.setEnabled(enabled);
		powerSymbol.setEnabled(enabled);
		powerUnit.setEnabled(enabled);
		energyButton.setEnabled(enabled);
		energySymbol.setEnabled(enabled);
		energyUnit.setEnabled(enabled);
		cellVoltageButton.setEnabled(enabled);
		voltagePerCell.setEnabled(enabled);
		voltagePerCellUnit.setEnabled(enabled);
	}
	
	/**
	 * enable voltage, current, revolution dependent measurement fields
	 * @param enabled
	 */
	private void updateVoltageCurrentRevolutionDependent(boolean enabled) {
		prop100WLabel.setEnabled(enabled);
		prop100WInput.setEnabled(enabled);
		prop100WUnit.setEnabled(enabled);
		etaButton.setEnabled(enabled);
		etaSymbol.setEnabled(enabled);
		etaUnit.setEnabled(enabled);
		if (enabled) {
			prop100WLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			prop100WInput.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			prop100WUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			etaButton.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			etaSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			etaUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		}
		else {
			prop100WLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			prop100WInput.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			prop100WUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			etaButton.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			etaSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			etaUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
		}
	}
	
	/**
	 * enable height measurement dependent fields
	 * @param enabled
	 */
	private void updateHeightDependent(boolean enabled) {
		slopeButton.setEnabled(enabled);
		slopeSymbol.setEnabled(enabled);
		slopeUnit.setEnabled(enabled);
	}
	
	/**
	 * collect all configuration relevant data and update device configuration
	 */
	private void collectAndUpdateConfiguration() {
		String recordKey = device.getMeasurementNames()[0];
		MeasurementType measurement = device.getMeasurementDefinition(recordKey);
		measurement.setActive(reveiverVoltageButton.getSelection());

		recordKey = device.getMeasurementNames()[1];
		measurement = device.getMeasurementDefinition(recordKey);
		measurement.setActive(voltageButton.getSelection());
		
		recordKey = device.getMeasurementNames()[2];
		measurement = device.getMeasurementDefinition(recordKey);
		measurement.setActive(currentButton.getSelection());

		recordKey = device.getMeasurementNames()[7];
		measurement = device.getMeasurementDefinition(recordKey);
		measurement.setActive(revolutionButton.getSelection());
		
		recordKey = device.getMeasurementNames()[9];
		measurement = device.getMeasurementDefinition(recordKey);
		measurement.setActive(heightButton.getSelection());
		
		recordKey = device.getMeasurementNames()[11];
		measurement = device.getMeasurementDefinition(recordKey);
		measurement.setActive(a1ValueButton.getSelection());
		measurement.setName(a1Text.getText());
		measurement.setSymbol(a1Symbol.getText());
		measurement.setUnit(a1Unit.getText());
		measurement.getDataCalculation().get(DataCalculationType.OFFSET).setValue(a1Offset.getText());
		measurement.getDataCalculation().get(DataCalculationType.FACTOR).setValue(a1Factor.getText());
		
		recordKey = device.getMeasurementNames()[12];
		measurement = device.getMeasurementDefinition(recordKey);
		measurement.setActive(a2ValueButton.getSelection());
		measurement.setName(a2Text.getText());
		measurement.setSymbol(a2Symbol.getText());
		measurement.setUnit(a2Unit.getText());
		measurement.getDataCalculation().get(DataCalculationType.OFFSET).setValue(a2Offset.getText());
		measurement.getDataCalculation().get(DataCalculationType.FACTOR).setValue(a2Factor.getText());

		recordKey = device.getMeasurementNames()[13];
		measurement = device.getMeasurementDefinition(recordKey);
		measurement.setActive(a3ValueButton.getSelection());
		measurement.setName(a3Text.getText());
		measurement.setSymbol(a3Symbol.getText());
		measurement.setUnit(a3Unit.getText());
		measurement.getDataCalculation().get(DataCalculationType.OFFSET).setValue(a3Offset.getText());
		measurement.getDataCalculation().get(DataCalculationType.FACTOR).setValue(a3Factor.getText());
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
		return numCellValue;
	}
	
	/**
	 * @return prop revolution at 100 W
	 */
	public int getPropN100Value() {
		return prop100WValue;
	}

}
