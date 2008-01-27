package osde.device.smmodellbau;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.rowset.serial.SerialArray;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
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
import osde.device.DeviceSerialPort;
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
	
	private final static String[]	TIME_INTERVAL						= { "1/16 s (-> 27 min)", "1/8 s   (-> 54 min)", "1/4 s   (-> 1:48 h)", "1/2 s   (-> 3:36 h)", "1 s      (-> 7:12 h)", "2 s      (-> 14:24 h)",
		"5 s      (-> 36 h)", "10 s    (-> 72 h)"				};
	private final static String[]	RX_AUTO_START_MS				= { "1,1 ms", "1,2 ms", "1,3 ms", "1,4 ms", "1,5 ms", "1,6 ms", "1,7 ms", "1,8 ms", "1,9 ms", "Rx an" };
	private final static String[]	CURRENT_SENSOR					= { "40/80A", "150A", "400A", "  20A" };
	private final static String[] A1_MODUS = {" Temperatur ", " Millivolt ", " Speed 250 ", " Speed 400 "};


	private CLabel												tempA3UnitLabel;
	private CLabel												tempA2UnitLabel;
	private CLabel												tempA2SignLabel;
	private CLabel												tempA1UnitLabel;
	private CLabel												tempA1SignLabel;
	private CLabel												tempInternUnitLabel;
	private CLabel												tempInternSignLabel;
	private CLabel												impulseUnitLabel;
	private CLabel												impulseSignLabel;
	private CLabel												receiverVoltageUnitLabel;
	private CLabel												receiverVoltageSignLabel;
	private CLabel												speedUnitLabel;
	private CLabel												speedSignLabel;
	private CLabel												etaUnitLabel;
	private CLabel												etaSignLabel;
	private CLabel												rpmUnitLabel;
	private CLabel												rpmSignLabel;
	private CLabel												slopeUnitLabel;
	private CLabel												slopeSignLabel;
	private CLabel												heightUnitLabel;
	private CLabel												heightSignLabel;
	private CLabel												cellVoltageUnitLabel;
	private CLabel												cellVoltageLabel;
	private CLabel												energyUnitLabel;
	private CLabel												engerySignLabel;
	private CLabel												powerUnitLabel;
	private CLabel												powerSignLabel;
	private CLabel												capacityUnitLabel;
	private CLabel												capacitySignLabel;
	private CLabel												currentUnitLabel;
	private CLabel												currentSignLabel;
	private CLabel												voltageUnitLabel;
	private CLabel												voltageSignLabel;
	private Button												cellVoltageButton;
	private Button												etaButton;
	private Button												slopeButton;
	private Button												energyButton;
	private Button												powerButton;
	private Button												tempA3Button;
	private Button												tempA2Button;
	private Button												tempA1Button;
	private Button												speedButton;
	private Button												tempInternButton;
	private Button												reveiverVoltageButton;
	private Button												impulseButton;
	private Button												rpmButton;
	private Button												heightButton;
	private Button												capacityButton;
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
	private Text gearFactorCombo;
	private CLabel gearLabel;
	private CCombo												numbeProbCombo;
	private CCombo												motorPoleCombo;
	private Button												numberPropButton;
	private Group													motorPropGroup;

	private Button												storeAdjustmentsButton;
	private CLabel firmwareVersionLabel;
	private CLabel firmwareText;
	private CLabel snLabel;
	private CLabel serialNumberText;
	private CLabel memUsageUnit;
	private CLabel memoryUsageText;
	private Slider gearRatioSlider;
	private Button stopDataButton;
	private Group liveDataCaptureGroup;
	private Button clearMemoryButton;
	private Group clearDataBufferGroup;
	private Button liveViewButton;
	private CLabel orangeLabel;
	private CLabel greenLabel;
	private CLabel redLabel;
	private Composite dataCaptureComposite;
	private Button stopLoggingButton;
	private Button startLoggingButton;
	private Group loggingGroup;
	private CLabel numberReadErrorLabel;
	private CLabel readDataErrorLabel;
	private CLabel redDataSetLabel;
	private CLabel dataSetLabel;
	private ProgressBar readDataProgressBar;
	private Button readDataButton;
	private Group dataReadGroup;
	private Composite dataMainComposite;
	private CTabItem dataTabItem;
	private Group statusGroup;
	private Button setConfigButton;
	private CLabel memUsagePercent;
	private Group													temperatureGroup;
	private Group													receiverGroup;
	private Group													airPressureGroup;
	private Group													powerGroup;
	private Text prop100WCombo;
	private CLabel												propRpm100WLabel;
	private CLabel												tempA3SignLabel;
	private CCombo												timeIntervalCombo;
	private Group													dataRateGroup;
	private Composite											composite1;
	private Button												readAdjustmentButton;
	private CTabItem											baseConfigTabItem;
	private CTabFolder										configTabFolder;
	@SuppressWarnings("unused")
	private final UniLogSerialPort				serialPort;																				// open/close port execute getData()....
	@SuppressWarnings("unused")
	private final OpenSerialDataExplorer	application;																				// interaction with application instance
	@SuppressWarnings("unused")
	private final Channels								channels;																					// interaction with channels, source of all records
	private final UniLog									device;																						// get device specific things, get serial port, ...
	
	private String statusText = "";

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
				SWTResourceManager.registerResourceUser(dialogShell);
				dialogShell.setLayout(new FormLayout());
				dialogShell.layout();
				dialogShell.pack();
				dialogShell.setSize(317, 540);
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
									firmwareVersionLabel = new CLabel(statusGroup, SWT.NONE);
									firmwareVersionLabel.setBounds(217, 15, 39, 22);
								}
								{
									firmwareText = new CLabel(statusGroup, SWT.NONE);
									firmwareText.setText("Firmware :  ");
									firmwareText.setBounds(142, 15, 75, 22);
								}
								{
									snLabel = new CLabel(statusGroup, SWT.CENTER | SWT.EMBEDDED);
									snLabel.setBounds(48, 15, 88, 22);
								}
								{
									serialNumberText = new CLabel(statusGroup, SWT.NONE);
									serialNumberText.setText("S/N :");
									serialNumberText.setBounds(12, 15, 42, 22);
								}
								{
									memUsageUnit = new CLabel(statusGroup, SWT.NONE);
									memUsageUnit.setText("[%]");
									memUsageUnit.setBounds(217, 36, 26, 22);
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
//									RowData statusLabelLData = new RowData();
//									statusLabelLData.width = 271;
//									statusLabelLData.height = 22;
//									memUsagePercent.setLayoutData(statusLabelLData);
									memUsagePercent.setText(this.statusText);
								}
							}
							{
								outletA1Group = new Group(composite1, SWT.NONE);
								outletA1Group.setLayout(null);
								outletA1Group.setText("A1 Modus");
								outletA1Group.setBounds(142, 383, 151, 49);
								{
									a1ModusCombo = new CCombo(outletA1Group, SWT.RIGHT);
									a1ModusCombo.setItems(new String[] {" Temperatur ", " Millivolt ", " Speed 250 ", " Speed 400 "});
									a1ModusCombo.select(2);
									a1ModusCombo.setEditable(false);
									a1ModusCombo.setBounds(8, 21, 128, 18);
									a1ModusCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									a1ModusCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											System.out.println("outletModusCombo.widgetSelected, event="+evt);
											//TODO add your code for outletModusCombo.widgetSelected
										}
									});
								}
							}
							{
								currentSensotGroup = new Group(composite1, SWT.NONE);
								currentSensotGroup.setLayout(null);
								currentSensotGroup.setText("Stromsensor");
								currentSensotGroup.setBounds(12, 383, 118, 49);
								{
									sensorCurrentCombo = new CCombo(currentSensotGroup, SWT.RIGHT);
									sensorCurrentCombo.setBounds(12, 20, 94, 18);
									sensorCurrentCombo.setItems(new String[] { " 40/80A ", "  150A ", "  400A ", "    20A" });
									sensorCurrentCombo.select(2);
									sensorCurrentCombo.setEditable(false);
									sensorCurrentCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									sensorCurrentCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											System.out.println("sensorCurrentCombo.widgetSelected, event="+evt);
											//TODO add your code for sensorCurrentCombo.widgetSelected
										}
									});
								}
							}
							{
								autoStartGroup = new Group(composite1, SWT.NONE);
								autoStartGroup.setLayout(null);
								autoStartGroup.setText("Autostart");
								autoStartGroup.setBounds(12, 291, 281, 87);
								{
									currentTriggerButton = new Button(autoStartGroup, SWT.RADIO | SWT.LEFT);
									currentTriggerButton.setText("bei Stromschwelle");
									currentTriggerButton.setBounds(20, 20, 150, 18);
									currentTriggerButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											System.out.println("currentTriggerButton.widgetSelected, event="+evt);
											//TODO add your code for currentTriggerButton.widgetSelected
										}
									});
								}
								{
									timeTriggerButton = new Button(autoStartGroup, SWT.RADIO | SWT.LEFT);
									timeTriggerButton.setText("Zeitschwelle");
									timeTriggerButton.setBounds(20, 42, 150, 18);
									timeTriggerButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											System.out.println("timeTriggerButton.widgetSelected, event="+evt);
											//TODO add your code for timeTriggerButton.widgetSelected
										}
									});
								}
								{
									impulseTriggerButton = new Button(autoStartGroup, SWT.RADIO | SWT.LEFT);
									impulseTriggerButton.setText("RC-Signallänge");
									impulseTriggerButton.setBounds(20, 64, 150, 18);
									impulseTriggerButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											System.out.println("impulseTriggerButton.widgetSelected, event="+evt);
											//TODO add your code for impulseTriggerButton.widgetSelected
										}
									});
								}
								{
									currentTriggerCombo = new CCombo(autoStartGroup, SWT.NONE);
									currentTriggerCombo.setBounds(170, 20, 60, 18);
									currentTriggerCombo.setItems(new String[] {"  1", "  2", "  3", "  4", "  5", "  6", "  7", "  8", "  9", " 10"});
									currentTriggerCombo.select(2);
									currentTriggerCombo.setEditable(false);
									currentTriggerCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									currentTriggerCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											System.out.println("currentTriggerCombo.widgetSelected, event="+evt);
											//TODO add your code for currentTriggerCombo.widgetSelected
										}
									});
								}
								{
									timeTriggerCombo = new CCombo(autoStartGroup, SWT.NONE);
									timeTriggerCombo.setBounds(170, 42, 60, 18);
									timeTriggerCombo.setEditable(false);
									timeTriggerCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									timeTriggerCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											System.out.println("timeTriggerCombo.widgetSelected, event="+evt);
											//TODO add your code for timeTriggerCombo.widgetSelected
										}
									});
								}
								{
									impulseTriggerCombo = new CCombo(autoStartGroup, SWT.NONE);
									impulseTriggerCombo.setBounds(170, 64, 60, 18);
									impulseTriggerCombo.setEditable(false);
									impulseTriggerCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									impulseTriggerCombo.setItems(RX_AUTO_START_MS);
									impulseTriggerCombo.select(4);
									impulseTriggerCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											System.out.println("rcTriggerCombo.widgetSelected, event="+evt);
											//TODO add your code for rcTriggerCombo.widgetSelected
										}
									});
								}
							}
							{
								motorPropGroup = new Group(composite1, SWT.NONE);
								motorPropGroup.setLayout(null);
								motorPropGroup.setText("Motor/Propeller/Getriebe");
								motorPropGroup.setBounds(12, 169, 281, 116);
								{
									numberPolsButton = new Button(motorPropGroup, SWT.RADIO | SWT.LEFT);
									numberPolsButton.setText("Motorpole");
									numberPolsButton.setBounds(20, 20, 150, 18);
									numberPolsButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											System.out.println("numberPolsButton.widgetSelected, event="+evt);
											//TODO add your code for numberPolsButton.widgetSelected
										}
									});
								}
								{
									motorPoleCombo = new CCombo(motorPropGroup, SWT.RIGHT);
									motorPoleCombo.setBounds(170, 20, 50, 18);
									motorPoleCombo.setItems(new String[] {"2", "4", "6", "8", "10", "12", "14", "16"});
									motorPoleCombo.select(6);
									motorPoleCombo.setEditable(false);
									motorPoleCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									motorPoleCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											System.out.println("motorPoleCombo.widgetSelected, event="+evt);
											//TODO add your code for motorPoleCombo.widgetSelected
										}
									});
								}
								{
									gearLabel = new CLabel(motorPropGroup, SWT.LEFT);
									gearLabel.setBounds(20, 40, 150, 20);
									gearLabel.setText("Getriebeuntersetzung");
								}
								{
									gearFactorCombo = new Text(motorPropGroup, SWT.RIGHT);
									gearFactorCombo.setBounds(170, 42, 50, 18);									//gearFactorCombo.setItems(new String[] {".8", ".9", "1.0", "1.1", "1.2"});
									gearFactorCombo.setText(" 1.0 : 1 ");
									gearFactorCombo.setEditable(false);
									gearFactorCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
								}
								{
									gearRatioSlider = new Slider(motorPropGroup, SWT.VERTICAL);
									gearRatioSlider.setBounds(222, 38, 17, 26);
									gearRatioSlider.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											System.out.println("gearRatioSlider.widgetSelected, event="+evt);
											//TODO
										}
									});
								}
								{
									numberPropButton = new Button(motorPropGroup, SWT.RADIO | SWT.LEFT);
									numberPropButton.setText("Propellerblätter");
									numberPropButton.setBounds(20, 64, 150, 18);
									numberPropButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											System.out.println("numberPropButton.widgetSelected, event="+evt);
											//TODO add your code for numberPropButton.widgetSelected
										}
									});
								}
								{
									numbeProbCombo = new CCombo(motorPropGroup, SWT.NONE);
									numbeProbCombo.setBounds(170, 64, 50, 18);
									numbeProbCombo.setItems(new String[] {"1", "2", "3", "4"});
									numbeProbCombo.select(1);
									numbeProbCombo.setEditable(false);
									numbeProbCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									numbeProbCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											System.out.println("numbeProbCombo.widgetSelected, event="+evt);
											//TODO add your code for numbeProbCombo.widgetSelected
										}
									});
								}
								{
									propRpm100WLabel = new CLabel(motorPropGroup, SWT.NONE);
									propRpm100WLabel.setBounds(20, 84, 149, 20);
									propRpm100WLabel.setText("Propeller n100W - Wert");
								}
								{
									prop100WCombo = new Text(motorPropGroup, SWT.NONE);
									prop100WCombo.setBounds(170, 86, 73, 18);
									prop100WCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											System.out.println("prop100WCombo.widgetSelected, event="+evt);
											//TODO add your code for prop100WCombo.widgetSelected
										}
									});
								}
							}
							{
								dataRateGroup = new Group(composite1, SWT.NONE);
								dataRateGroup.setLayout(null);
								dataRateGroup.setText("Speicherrate");
								dataRateGroup.setBounds(12, 119, 281, 45);
								{
									timeIntervalCombo = new CCombo(dataRateGroup, SWT.NONE);
									timeIntervalCombo.setItems(TIME_INTERVAL);
									timeIntervalCombo.setBounds(75, 20, 138, 18);
									timeIntervalCombo.select(1);
									timeIntervalCombo.setEditable(false);
									timeIntervalCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									timeIntervalCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											System.out.println("timeRateCombo.widgetSelected, event="+evt);
											//TODO add your code for timeRateCombo.widgetSelected
										}
									});
								}
							}
							{
								readAdjustmentButton = new Button(composite1, SWT.PUSH | SWT.CENTER);
								readAdjustmentButton.setText("Einstellungen auslesen");
								readAdjustmentButton.setBounds(12, 84, 281, 29);
								readAdjustmentButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("readAdjustmentButton.widgetSelected, event="+evt);
										try {
											serialPort.readConfiguration();
											updateConfigTab();
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
								storeAdjustmentsButton.setBounds(12, 442, 281, 31);
								storeAdjustmentsButton.setEnabled(false);
								storeAdjustmentsButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("storeAdjustmentsButton.widgetSelected, event="+evt);
										//TODO add your code for storeAdjustmentsButton.widgetSelected
									}
								});
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
							powerGroup.setBounds(0, 0, 305, 185);
							powerGroup.setLayout(null);
							powerGroup.setText("Versorgung/Antrieb");
							//powerGroup.setBackground(SWTResourceManager.getColor(128, 128, 225));
							{
								voltageButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
								voltageButton.setText("Spannung");
								voltageButton.setBounds(50, 20, 120, 18);
								voltageButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("voltageButton.widgetSelected, event="+evt);
										//TODO add your code for voltageButton.widgetSelected
									}
								});
							}
							{
								voltageSignLabel = new CLabel(powerGroup, SWT.NONE);
								voltageSignLabel.setText("U");
								voltageSignLabel.setBounds(190, 18, 40, 20);
							}
							{
								voltageUnitLabel = new CLabel(powerGroup, SWT.NONE);
								voltageUnitLabel.setText("[V]");
								voltageUnitLabel.setBounds(230, 18, 40, 20);
							}
							{
								currentButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
								currentButton.setText("Strom");
								currentButton.setBounds(50, 40, 120, 18);
								currentButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("currentButton.widgetSelected, event="+evt);
										//TODO add your code for currentButton.widgetSelected
									}
								});
							}
							{
								currentSignLabel = new CLabel(powerGroup, SWT.NONE);
								currentSignLabel.setText(" I");
								currentSignLabel.setBounds(190, 38, 40, 20);
							}
							{
								currentUnitLabel = new CLabel(powerGroup, SWT.NONE);
								currentUnitLabel.setText("[A]");
								currentUnitLabel.setBounds(230, 38, 40, 20);
							}
							{
								capacityButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
								capacityButton.setText("Ladung");
								capacityButton.setBounds(50, 60, 120, 18);
								capacityButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("capacityButton.widgetSelected, event="+evt);
										//TODO add your code for capacityButton.widgetSelected
									}
								});
							}
							{
								capacitySignLabel = new CLabel(powerGroup, SWT.NONE);
								capacitySignLabel.setText("C");
								capacitySignLabel.setBounds(190, 58, 40, 20);
							}
							{
								capacityUnitLabel = new CLabel(powerGroup, SWT.NONE);
								capacityUnitLabel.setText("[Ah]");
								capacityUnitLabel.setBounds(230, 58, 40, 20);
							}
							{
								powerButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
								powerButton.setText("Leistung");
								powerButton.setBounds(50, 80, 120, 18);
								powerButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("powerButton.widgetSelected, event="+evt);
										//TODO add your code for powerButton.widgetSelected
									}
								});
							}
							{
								powerSignLabel = new CLabel(powerGroup, SWT.NONE);
								powerSignLabel.setText("P");
								powerSignLabel.setBounds(190, 78, 40, 20);
							}
							{
								powerUnitLabel = new CLabel(powerGroup, SWT.NONE);
								powerUnitLabel.setText("[W]");
								powerUnitLabel.setBounds(230, 78, 40, 20);
							}
							{
								energyButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
								energyButton.setText("Energie");
								energyButton.setBounds(50, 100, 120, 18);
								energyButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("energyButton.widgetSelected, event="+evt);
										//TODO add your code for energyButton.widgetSelected
									}
								});
							}
							{
								engerySignLabel = new CLabel(powerGroup, SWT.NONE);
								engerySignLabel.setText("E");
								engerySignLabel.setBounds(190, 98, 40, 20);
							}
							{
								energyUnitLabel = new CLabel(powerGroup, SWT.NONE);
								energyUnitLabel.setText("[Wh]");
								energyUnitLabel.setBounds(230, 98, 40, 20);
							}
							{
								cellVoltageButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
								cellVoltageButton.setText("Spannung/Zelle");
								cellVoltageButton.setBounds(50, 120, 120, 18);
								cellVoltageButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("cellVoltageButton.widgetSelected, event="+evt);
										//TODO add your code for cellVoltageButton.widgetSelected
									}
								});
							}
							{
								cellVoltageLabel = new CLabel(powerGroup, SWT.NONE);
								cellVoltageLabel.setText("Uc");
								cellVoltageLabel.setBounds(190, 118, 40, 20);
							}
							{
								cellVoltageUnitLabel = new CLabel(powerGroup, SWT.NONE);
								cellVoltageUnitLabel.setText("[V]");
								cellVoltageUnitLabel.setBounds(230, 118, 40, 20);
							}
							{
								rpmButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
								rpmButton.setBounds(50, 140, 120, 18);
								rpmButton.setText("Drehzahl");
								rpmButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("rpmButton.widgetSelected, event="+evt);
										//TODO add your code for rpmButton.widgetSelected
									}
								});
							}
							{
								rpmSignLabel = new CLabel(powerGroup, SWT.NONE);
								rpmSignLabel.setBounds(190, 138, 40, 20);
								rpmSignLabel.setText("rpm");
							}
							{
								rpmUnitLabel = new CLabel(powerGroup, SWT.NONE);
								rpmUnitLabel.setBounds(230, 138, 40, 20);
								rpmUnitLabel.setText("1/min");
							}
							{
								etaButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
								etaButton.setBounds(50, 160, 120, 18);
								etaButton.setText("Wirkungsgrad");
								etaButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("etaButton.widgetSelected, event="+evt);
										//TODO add your code for etaButton.widgetSelected
									}
								});
							}
							{
								etaSignLabel = new CLabel(powerGroup, SWT.NONE);
								etaSignLabel.setBounds(190, 158, 40, 20);
								etaSignLabel.setText("eta");
							}
							{
								etaUnitLabel = new CLabel(powerGroup, SWT.NONE);
								etaUnitLabel.setBounds(230, 158, 40, 20);
								etaUnitLabel.setText("%");
							}
						}
						{
							airPressureGroup = new Group(mainTabComposite, SWT.NONE);
							airPressureGroup.setLayout(null);
							airPressureGroup.setText("Dynamik");
							airPressureGroup.setBounds(0, 185, 305, 85);
							{
								heightButton = new Button(airPressureGroup, SWT.CHECK | SWT.LEFT);
								heightButton.setText("Höhe");
								heightButton.setBounds(50, 20, 120, 18);
								heightButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("heightButton.widgetSelected, event="+evt);
										//TODO add your code for heightButton.widgetSelected
									}
								});
							}
							{
								heightSignLabel = new CLabel(airPressureGroup, SWT.NONE);
								heightSignLabel.setText("h");
								heightSignLabel.setBounds(190, 18, 40, 20);
							}
							{
								heightUnitLabel = new CLabel(airPressureGroup, SWT.NONE);
								heightUnitLabel.setText("[m]");
								heightUnitLabel.setBounds(230, 18, 40, 20);
							}
							{
								slopeButton = new Button(airPressureGroup, SWT.CHECK | SWT.LEFT);
								slopeButton.setText("Steigrate");
								slopeButton.setBounds(50, 40, 120, 18);
								slopeButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("slopeButton.widgetSelected, event="+evt);
										//TODO add your code for slopeButton.widgetSelected
									}
								});
							}
							{
								slopeSignLabel = new CLabel(airPressureGroup, SWT.NONE);
								slopeSignLabel.setText("V");
								slopeSignLabel.setBounds(190, 38, 40, 20);
							}
							{
								slopeUnitLabel = new CLabel(airPressureGroup, SWT.NONE);
								slopeUnitLabel.setText("[m/s]");
								slopeUnitLabel.setBounds(230, 38, 40, 20);
							}
							{
								speedButton = new Button(airPressureGroup, SWT.CHECK | SWT.LEFT);
								speedButton.setBounds(50, 60, 130, 18);
								speedButton.setText("Geschwindigkeit A1");
								speedButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("speedButton.widgetSelected, event="+evt);
										//TODO add your code for speedButton.widgetSelected
									}
								});
							}
							{
								speedSignLabel = new CLabel(airPressureGroup, SWT.NONE);
								speedSignLabel.setBounds(190, 58, 40, 20);
								speedSignLabel.setText("V");
							}
							{
								speedUnitLabel = new CLabel(airPressureGroup, SWT.NONE);
								speedUnitLabel.setBounds(230, 58, 60, 20);
								speedUnitLabel.setText("[km/Std]");
							}
						}
						{
							receiverGroup = new Group(mainTabComposite, SWT.NONE);
							receiverGroup.setLayout(null);
							receiverGroup.setText("Empfänger");
							receiverGroup.setBounds(0, 270, 305, 65);
							{
								reveiverVoltageButton = new Button(receiverGroup, SWT.CHECK | SWT.LEFT);
								reveiverVoltageButton.setBounds(50, 20, 120, 18);
								reveiverVoltageButton.setText("ServoImpuls");
								reveiverVoltageButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("reveiverVoltageButton.widgetSelected, event="+evt);
										//TODO add your code for reveiverVoltageButton.widgetSelected
									}
								});
							}
							{
								receiverVoltageSignLabel = new CLabel(receiverGroup, SWT.NONE);
								receiverVoltageSignLabel.setBounds(190, 18, 40, 20);
								receiverVoltageSignLabel.setText("U");
							}
							{
								receiverVoltageUnitLabel = new CLabel(receiverGroup, SWT.NONE);
								receiverVoltageUnitLabel.setBounds(230, 18, 40, 20);
								receiverVoltageUnitLabel.setText("[V]");
							}
							{
								impulseButton = new Button(receiverGroup, SWT.CHECK | SWT.LEFT);
								impulseButton.setBounds(50, 40, 120, 18);
								impulseButton.setText("SpannungEmpf");
								impulseButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("impulseButton.widgetSelected, event="+evt);
										//TODO add your code for impulseButton.widgetSelected
									}
								});
							}
							{
								impulseSignLabel = new CLabel(receiverGroup, SWT.NONE);
								impulseSignLabel.setBounds(190, 38, 40, 20);
								impulseSignLabel.setText("Si");
							}
							{
								impulseUnitLabel = new CLabel(receiverGroup, SWT.NONE);
								impulseUnitLabel.setBounds(230, 38, 40, 20);
								impulseUnitLabel.setText("[ms]");
							}
						}
						{
							temperatureGroup = new Group(mainTabComposite, SWT.NONE);
							temperatureGroup.setLayout(null);
							temperatureGroup.setText("Temperatur");
							temperatureGroup.setBounds(0, 335, 305, 105);
							{
								tempInternButton = new Button(temperatureGroup, SWT.CHECK | SWT.LEFT);
								tempInternButton.setBounds(50, 20, 120, 18);
								tempInternButton.setText("Temp intern");
								tempInternButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("tempInternButton.widgetSelected, event="+evt);
										//TODO add your code for tempInternButton.widgetSelected
									}
								});
							}
							{
								tempInternSignLabel = new CLabel(temperatureGroup, SWT.NONE);
								tempInternSignLabel.setBounds(190, 18, 40, 20);
								tempInternSignLabel.setText("Ti");
							}
							{
								tempInternUnitLabel = new CLabel(temperatureGroup, SWT.NONE);
								tempInternUnitLabel.setBounds(230, 18, 40, 20);
								tempInternUnitLabel.setText("°C");
							}
							{
								tempA1Button = new Button(temperatureGroup, SWT.CHECK | SWT.LEFT);
								tempA1Button.setBounds(50, 40, 120, 18);
								tempA1Button.setText("Temperatur A1");
								tempA1Button.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("tempA1Button.widgetSelected, event="+evt);
										//TODO add your code for tempA1Button.widgetSelected
									}
								});
							}
							{
								tempA1SignLabel = new CLabel(temperatureGroup, SWT.NONE);
								tempA1SignLabel.setBounds(190, 38, 40, 20);
								tempA1SignLabel.setText("Ta1");
							}
							{
								tempA1UnitLabel = new CLabel(temperatureGroup, SWT.NONE);
								tempA1UnitLabel.setBounds(230, 38, 40, 20);
								tempA1UnitLabel.setText("°C");
							}
							{
								tempA2Button = new Button(temperatureGroup, SWT.CHECK | SWT.LEFT);
								tempA2Button.setBounds(50, 60, 120, 18);
								tempA2Button.setText("Temperatur A2");
								tempA2Button.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("tempA2Button.widgetSelected, event="+evt);
										//TODO add your code for tempA2Button.widgetSelected
									}
								});
							}
							{
								tempA2SignLabel = new CLabel(temperatureGroup, SWT.NONE);
								tempA2SignLabel.setBounds(190, 58, 40, 20);
								tempA2SignLabel.setText("Ta2");
							}
							{
								tempA2UnitLabel = new CLabel(temperatureGroup, SWT.NONE);
								tempA2UnitLabel.setBounds(230, 58, 40, 20);
								tempA2UnitLabel.setText("°C");
							}
							{
								tempA3Button = new Button(temperatureGroup, SWT.CHECK | SWT.LEFT);
								tempA3Button.setBounds(50, 80, 120, 18);
								tempA3Button.setText("Temperatur A3");
								tempA3Button.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("tempA3Button.widgetSelected, event="+evt);
										//TODO add your code for tempA3Button.widgetSelected
									}
								});
							}
							{
								tempA3SignLabel = new CLabel(temperatureGroup, SWT.NONE);
								tempA3SignLabel.setBounds(190, 78, 40, 20);
								tempA3SignLabel.setText("Ta3");
							}
							{
								tempA3UnitLabel = new CLabel(temperatureGroup, SWT.NONE);
								tempA3UnitLabel.setBounds(230, 78, 40, 20);
								tempA3UnitLabel.setText("°C");
							}
						}
						{
							setConfigButton = new Button(mainTabComposite, SWT.PUSH | SWT.CENTER);
							setConfigButton.setBounds(45, 449, 210, 25);
							setConfigButton.setText("Konfiguration speichern");
							setConfigButton.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									System.out.println("setConfigButton.widgetSelected, event="+evt);
									//TODO add your code for setConfigButton.widgetSelected
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
							dataReadGroup.setBounds(12, 12, 280, 175);
							dataReadGroup.setText("Daten auslesen");
							{
								readDataButton = new Button(dataReadGroup, SWT.PUSH | SWT.CENTER);
								readDataButton.setText("Start Daten auslesen");
								readDataButton.setBounds(10, 20, 260, 25);
								readDataButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("readDataButton.widgetSelected, event="+evt);
										//TODO add your code for readDataButton.widgetSelected
									}
								});
							}
							{
								dataSetLabel = new CLabel(dataReadGroup, SWT.NONE);
								dataSetLabel.setBounds(25, 60, 180, 20);
								dataSetLabel.setText("gelesene Telegramme     :");
							}
							{
								redDataSetLabel = new CLabel(dataReadGroup, SWT.RIGHT);
								redDataSetLabel.setBounds(200, 60, 55, 20);
								redDataSetLabel.setText("0");
							}
							{
								readDataErrorLabel = new CLabel(dataReadGroup, SWT.NONE);
								readDataErrorLabel.setBounds(25, 80, 180, 20);
								readDataErrorLabel.setText("Datenübertragungsfehler  :");
							}
							{
								numberReadErrorLabel = new CLabel(dataReadGroup, SWT.RIGHT);
								numberReadErrorLabel.setBounds(200, 80, 55, 20);
								numberReadErrorLabel.setText("0");
							}
							{
								readDataProgressBar = new ProgressBar(dataReadGroup, SWT.NONE);
								readDataProgressBar.setBounds(10, 110, 260, 15);
								readDataProgressBar.setMinimum(0);
								readDataProgressBar.setMaximum(100);
							}
							{
								stopDataButton = new Button(dataReadGroup, SWT.PUSH | SWT.CENTER);
								stopDataButton.setBounds(10, 135, 260, 25);
								stopDataButton.setText("S T O P");
							}
						}
						{
							loggingGroup = new Group(dataMainComposite, SWT.NONE);
							loggingGroup.setLayout(null);
							loggingGroup.setBounds(12, 191, 281, 133);
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
									greenLabel = new CLabel(dataCaptureComposite, SWT.CENTER | SWT.EMBEDDED | SWT.BORDER);
									greenLabel.setText("green");
								}
								{
									orangeLabel = new CLabel(dataCaptureComposite, SWT.CENTER | SWT.EMBEDDED | SWT.BORDER);
									orangeLabel.setText("orange");
								}
							}
							{
								stopLoggingButton = new Button(loggingGroup, SWT.PUSH | SWT.CENTER);
								stopLoggingButton.setText("Stop logging");
								stopLoggingButton.setBounds(10, 85, 260, 25);
							}
						}
						{
							clearDataBufferGroup = new Group(dataMainComposite, SWT.NONE);
							clearDataBufferGroup.setLayout(null);
							clearDataBufferGroup.setBounds(12, 400, 280, 75);
							clearDataBufferGroup.setText("Datenspeicher");
							{
								clearMemoryButton = new Button(clearDataBufferGroup, SWT.PUSH | SWT.CENTER);
								clearMemoryButton.setText("löschen");
								clearMemoryButton.setBounds(10, 25, 260, 25);
							}
						}
						{
							liveDataCaptureGroup = new Group(dataMainComposite, SWT.NONE);
							liveDataCaptureGroup.setLayout(null);
							liveDataCaptureGroup.setBounds(12, 330, 280, 75);
							liveDataCaptureGroup.setText("Datenabfrage");
							{
								liveViewButton = new Button(liveDataCaptureGroup, SWT.PUSH | SWT.CENTER);
								liveViewButton.setText("live Datenanzeige");
								liveViewButton.setBounds(10, 25, 260, 25);
							}
						}
					}
				} // end measurement configuration tab
				{
					FormData configTabFolderLData = new FormData();
					configTabFolderLData.width = 305;
					configTabFolderLData.height = 484;
					configTabFolderLData.left =  new FormAttachment(0, 1000, 0);
					configTabFolderLData.top =  new FormAttachment(0, 1000, 0);
					configTabFolderLData.right =  new FormAttachment(1000, 1000, 0);
					configTabFolderLData.bottom =  new FormAttachment(1000, 1000, 0);
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
						//TODO check if some thing to do before exiting
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

	private void updateConfigTab() {
		snLabel.setText("" + serialPort.getSerialNumber());
		firmwareVersionLabel.setText("v" + serialPort.getUnilogVersion());
		memUsagePercent.setText(serialPort.getMemoryUsedPercent());
		timeIntervalCombo.select(serialPort.getTimeIntervalPosition());
		numberPolsButton.setSelection(serialPort.isMotorPole());
		numberPropButton.setSelection(serialPort.isPropBlade());
		motorPoleCombo.select(serialPort.getCountMotorPole()/2-1);
		numbeProbCombo.select(serialPort.getCountPropBlade()/2-1);
		//TODo disable motorPole/propb
		gearFactorCombo.setText(" " + serialPort.getGearRatio() + " : 1 ");
		currentButton.setSelection(serialPort.isAutoStartCurrent());
		currentTriggerCombo.select(serialPort.getCurrentAutoStart()-1);
		timeTriggerButton.setSelection(serialPort.isImpulseAutoStartTime());
		timeTriggerCombo.select(serialPort.getTimeIntervalPosition());
		impulseButton.setSelection(serialPort.isAutStartRx());
		impulseTriggerCombo.select(serialPort.getImpulseAutoStartTime_sec()-11);
		sensorCurrentCombo.select(serialPort.getCurrentSensorPosition());
		a1ModusCombo.select(serialPort.getModusA1Position());
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
	public void setReadDataProgressBar(int value) {
		value = value < 0 ? 0 : value;
		value = value > 100 ? 100 : value;
		this.readDataProgressBar.setSelection(value);
	}
}
