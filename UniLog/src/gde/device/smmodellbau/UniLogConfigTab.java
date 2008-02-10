package osde.device.smmodellbau;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import osde.device.DataTypes;
import osde.device.MeasurementType;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.CalculationThread;


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
public class UniLogConfigTab extends org.eclipse.swt.widgets.Composite {

	{
		//Register as a resource user - SWTResourceManager will
		//handle the obtaining and disposing of resources
		SWTResourceManager.registerResourceUser(this);
	}
	
	private Logger												log	= Logger.getLogger(this.getClass().getName());
	
	private CLabel												receiverVoltageSymbol, receiverVoltageUnit;
	private CLabel voltageUnit;
	private CCombo currentUnit;
	private CLabel												revolutionSymbol, revolutionUnit;
	private CLabel												heightSymbol, heightUnit;
	private Button												a23ExternModus;
	private CLabel												axName, axUnit, axOffset, axFactor;
	private Button												a1UniLogModus;
	private Button												a23InternModus;
	private CLabel												prop100WUnit, numCellLabel;
	private Group													axModusGroup;
	private Group													powerGroup;
	private Text													prop100WInput, numCellInput;
	private Text													a1Unit;
	private CLabel												etaUnit;
	private CLabel												etaSymbol;
	private CLabel												slopeUnit;
	private CLabel												slopeSymbol;
	private CLabel												etaButton;
	private CLabel												slopeLabel;
	private Button												a3ValueButton;
	private Button												a1ValueButton;
	private Button												a2ValueButton;
	private Button												reveiverVoltageButton;
	private Button												revolutionButton;
	private Button												heightButton;
	private CLabel												capacityLabel;
	private Button												currentButton;
	private Button												voltageButton;
	private Text													a1Factor, a2Factor, a3Factor;
	private Text													a1Offset, a2Offset, a3Offset;
	private Text													a3Unit;
	private Text													a2Unit;
	private CLabel												voltagePerCellUnit;
	private CLabel												voltagePerCellSymbol;
	private CLabel												voltageSymbol;
	private CLabel												energyUnit;
	private CLabel												currentSymbol;
	private CLabel												energySymbol;
	private CLabel												powerUnit;
	private CLabel												powerSymbol;
	private CLabel												capacityUnit;
	private CLabel												capacitySymbol;
	private CLabel												voltagePerCellLabel;
	private CLabel												energyLabel;
	private CLabel												powerLabel;
	private Text													a3Text;
	private Text													a2Text;
	private Text													a1Text;
	private CLabel												prop100WLabel;
	private Button												setConfigButton;

	private boolean isA1ModusAvailable = false;
	private int prop100WValue = 3400;
	private int numCellValue = 12;

	private final String									configName; // tabName
	private final UniLogDialog						dialog;
	private CCombo regressionTime;
	private final UniLog									device;																						// get device specific things, get serial port, ...
	private final	OpenSerialDataExplorer  application;

//	/**
//	* Auto-generated main method to display this 
//	* org.eclipse.swt.widgets.Composite inside a new Shell.
//	*/
//	public static void main(String[] args) {
//		showGUI();
//	}
		
//	/**
//	* Auto-generated method to display this 
//	* org.eclipse.swt.widgets.Composite inside a new Shell.
//	*/
//	public static void showGUI() {
//		Display display = Display.getDefault();
//		Shell shell = new Shell(display);
//		UniLogConfigTab inst = new UniLogConfigTab(shell, SWT.NULL, null);
//		Point size = inst.getSize();
//		shell.setLayout(new FillLayout());
//		shell.layout();
//		if(size.x == 0 && size.y == 0) {
//			inst.pack();
//			shell.pack();
//		} else {
//			Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
//			shell.setSize(shellBounds.width, shellBounds.height);
//		}
//		shell.open();
//		while (!shell.isDisposed()) {
//			if (!display.readAndDispatch())
//				display.sleep();
//		}
//	}

	public UniLogConfigTab(Composite parent, UniLog device, String tabName) {
		super(parent, SWT.NONE);
		this.device = device;
		this.configName = tabName;
		this.dialog = device.getDialog();
		this.application = OpenSerialDataExplorer.getInstance();
		initGUI();
	}

	private void initGUI() {
		try {
			FillLayout thisLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
			this.setLayout(thisLayout);
			this.setSize(630, 320);
			{
				this.setLayout(null);
				{
					powerGroup = new Group(this, SWT.NONE);
					powerGroup.setBounds(5, 2, 299, 312);
					powerGroup.setLayout(null);
					powerGroup.setText("Versorgung/Antrieb/Höhe");
					powerGroup.setToolTipText("Hier bitte alle Datenkanäle auswählen, die angezeigt werden sollen");
					powerGroup.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							if (log.isLoggable(Level.FINEST))  log.finest("powerGroup.paintControl, event="+evt);
							String recordKey = device.getMeasurementNames(configName)[0];
							MeasurementType measurement = device.getMeasurement(configName, recordKey);
							reveiverVoltageButton.setSelection(measurement.isActive());
							reveiverVoltageButton.setText(measurement.getName());
							receiverVoltageSymbol.setText(measurement.getSymbol());
							receiverVoltageUnit.setText(measurement.getUnit());

							recordKey = device.getMeasurementNames(configName)[1];
							measurement = device.getMeasurement(configName, recordKey);
							voltageButton.setSelection(measurement.isActive());
							voltageButton.setText(measurement.getName());
							voltageSymbol.setText(measurement.getSymbol());
							voltageUnit.setText(measurement.getUnit());
							
							recordKey = device.getMeasurementNames(configName)[2];
							measurement = device.getMeasurement(configName, recordKey);
							currentButton.setSelection(measurement.isActive());
							currentButton.setText(measurement.getName());
							currentSymbol.setText(measurement.getSymbol());
							currentUnit.setText(" [" + measurement.getUnit() + "]");
							
							// capacity, power, energy
							updateVoltageAndCurrentDependent(voltageButton.getSelection() && currentButton.getSelection());
							
							// number cells voltagePerCell
							recordKey = device.getMeasurementNames(configName)[6];
							numCellValue = new Integer("" + device.getPropertyValue(configName, recordKey, UniLogDialog.NUMBER_CELLS));

							recordKey = device.getMeasurementNames(configName)[7];
							measurement = device.getMeasurement(configName, recordKey);
							revolutionButton.setSelection(measurement.isActive());
							revolutionButton.setText(measurement.getName());
							revolutionSymbol.setText(measurement.getSymbol());
							revolutionUnit.setText(measurement.getUnit());
							
							// n100W value, eta calculation 										
							updateVoltageCurrentRevolutionDependent(voltageButton.getSelection() && currentButton.getSelection() && revolutionButton.getSelection());
							recordKey = device.getMeasurementNames(configName)[8];
							prop100WValue = new Integer("" + device.getPropertyValue(configName, recordKey, UniLogDialog.PROP_N_100_WATT));
							
							recordKey = device.getMeasurementNames(configName)[9];
							measurement = device.getMeasurement(configName, recordKey);
							heightButton.setSelection(measurement.isActive());
							heightButton.setText(measurement.getName());
							heightSymbol.setText(measurement.getSymbol());
							heightUnit.setText(measurement.getUnit());
							
							updateHeightDependent(heightButton.getSelection());
						}
					});
					{
						reveiverVoltageButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
						reveiverVoltageButton.setBounds(25, 20, 132, 18);
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
						receiverVoltageSymbol.setBounds(165, 18, 40, 20);
						receiverVoltageSymbol.setText("U");
					}
					{
						receiverVoltageUnit = new CLabel(powerGroup, SWT.NONE);
						receiverVoltageUnit.setBounds(205, 18, 40, 20);
						receiverVoltageUnit.setText("[V]");
					}
					{
						voltageButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
						voltageButton.setText("Spannung");
						voltageButton.setBounds(25, 42, 120, 18);
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
						voltageSymbol.setBounds(165, 40, 40, 20);
					}
					{
						voltageUnit = new CLabel(powerGroup, SWT.NONE);
						voltageUnit.setText("[V]");
						voltageUnit.setBounds(205, 40, 40, 20);
					}
					{
						currentButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
						currentButton.setText("Strom");
						currentButton.setBounds(25, 64, 120, 18);
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
						currentSymbol.setBounds(165, 62, 30, 18);
					}
					{
						currentUnit = new CCombo(powerGroup, SWT.CENTER | SWT.BORDER);
						currentUnit.setItems(new String[] {" [mA]", "  [A]"});
						currentUnit.select(1);
						currentUnit.setBounds(203, 62, 57, 20);
						currentUnit.setToolTipText("Stromeinheit entsprechend dem eingestellten Faktor einstellen");
						currentUnit.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								System.out.println("currentUnit.widgetSelected, event="+evt);
								String measurementKey = device.getMeasurementNames(configName)[2]; //2=current
								switch (currentUnit.getSelectionIndex()) {
								case 0: // [mA]
									device.setFactor(configName, measurementKey, 0.001); //2=current
									device.setMeasurementUnit(configName, measurementKey, "mA");
									break;
								default: // [A]
									device.setFactor(configName, measurementKey, 1.0); //2=current
									device.setMeasurementUnit(configName, measurementKey, "A");
									break;
								}
							}
						});
					}
					{
						capacityLabel = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
						capacityLabel.setText("Ladung");
						capacityLabel.setBounds(39, 86, 120, 20);
					}
					{
						capacitySymbol = new CLabel(powerGroup, SWT.NONE);
						capacitySymbol.setText("C");
						capacitySymbol.setBounds(165, 84, 40, 20);
					}
					{
						capacityUnit = new CLabel(powerGroup, SWT.NONE);
						capacityUnit.setText("[Ah]");
						capacityUnit.setBounds(205, 84, 40, 20);
					}
					{
						powerLabel = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
						powerLabel.setText("Leistung");
						powerLabel.setBounds(39, 108, 120, 20);
					}
					{
						powerSymbol = new CLabel(powerGroup, SWT.NONE);
						powerSymbol.setText("P");
						powerSymbol.setBounds(165, 106, 40, 20);
					}
					{
						powerUnit = new CLabel(powerGroup, SWT.NONE);
						powerUnit.setText("[W]");
						powerUnit.setBounds(205, 106, 40, 20);
					}
					{
						energyLabel = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
						energyLabel.setText("Energie");
						energyLabel.setBounds(39, 130, 120, 20);
					}
					{
						energySymbol = new CLabel(powerGroup, SWT.NONE);
						energySymbol.setText("E");
						energySymbol.setBounds(165, 128, 40, 20);
					}
					{
						energyUnit = new CLabel(powerGroup, SWT.NONE);
						energyUnit.setText("[Wh]");
						energyUnit.setBounds(205, 128, 40, 20);
					}
					{
						voltagePerCellLabel = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
						voltagePerCellLabel.setText("Spannung/Zelle");
						voltagePerCellLabel.setBounds(39, 152, 120, 20);
					}
					{
						voltagePerCellSymbol = new CLabel(powerGroup, SWT.NONE);
						voltagePerCellSymbol.setText("Uc");
						voltagePerCellSymbol.setBounds(165, 150, 40, 20);
					}
					{
						voltagePerCellUnit = new CLabel(powerGroup, SWT.NONE);
						voltagePerCellUnit.setText("[V]");
						voltagePerCellUnit.setBounds(205, 150, 40, 20);
					}
					{
						numCellLabel = new CLabel(powerGroup, SWT.LEFT);
						numCellLabel.setBounds(39, 172, 118, 18);
						numCellLabel.setText("Anzahl Akkuzellen");
					}
					{
						numCellInput = new Text(powerGroup, SWT.LEFT);
						numCellInput.setBounds(165, 174, 40, 18);
						numCellInput.setText(" " + numCellValue); 
						numCellInput.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("numCellInput.keyReleased, event="+evt);
								if (evt.character == SWT.CR) {
									setConfigButton.setEnabled(true);
									numCellValue = new Integer(numCellInput.getText());
								}
							}
						});
					}
					{
						revolutionButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
						revolutionButton.setBounds(25, 196, 140, 18);
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
						revolutionSymbol.setBounds(165, 194, 40, 20);
						revolutionSymbol.setText("rpm");
					}
					{
						revolutionUnit = new CLabel(powerGroup, SWT.NONE);
						revolutionUnit.setBounds(205, 194, 40, 20);
						revolutionUnit.setText("1/min");
					}
					{
						prop100WLabel = new CLabel(powerGroup, SWT.LEFT);
						prop100WLabel.setBounds(39, 216, 118, 18);
						prop100WLabel.setText("Propeller n100W");
					}
					{
						prop100WInput = new Text(powerGroup, SWT.LEFT);
						prop100WInput.setBounds(165, 218, 40, 18);
						prop100WInput.setText(" " + prop100WValue);
						prop100WInput.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("prop100WInput.keyReleased, event="+evt);
								if (evt.character == SWT.CR) {
									setConfigButton.setEnabled(true);
									prop100WValue = new Integer(prop100WInput.getText());
								}
							}
						});
					}
					{
						prop100WUnit = new CLabel(powerGroup, SWT.NONE);
						prop100WUnit.setBounds(205, 216, 88, 20);
						prop100WUnit.setText("100W  * 1/min");
					}
					{
						etaButton = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
						etaButton.setBounds(39, 240, 108, 20);
						etaButton.setText("Wirkungsgrad");
					}
					{
						etaSymbol = new CLabel(powerGroup, SWT.NONE);
						etaSymbol.setBounds(165, 239, 40, 20);
						etaSymbol.setText("eta");
					}
					{
						etaUnit = new CLabel(powerGroup, SWT.NONE);
						etaUnit.setBounds(205, 238, 40, 20);
						etaUnit.setText("%");
					}
					{
						heightButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
						heightButton.setText("Höhe");
						heightButton.setBounds(25, 262, 120, 18);
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
						heightSymbol.setBounds(165, 260, 40, 20);
					}
					{
						heightUnit = new CLabel(powerGroup, SWT.NONE);
						heightUnit.setText("[m]");
						heightUnit.setBounds(205, 260, 40, 20);
					}
					{
						slopeLabel = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
						slopeLabel.setText("Steigrate");
						slopeLabel.setBounds(39, 282, 120, 19);
					}
					{
						slopeSymbol = new CLabel(powerGroup, SWT.NONE);
						slopeSymbol.setText("V");
						slopeSymbol.setBounds(165, 282, 40, 20);
					}
					{
						slopeUnit = new CLabel(powerGroup, SWT.NONE);
						slopeUnit.setText("[m/s]");
						slopeUnit.setBounds(205, 282, 40, 20);
					}
					{
						regressionTime = new CCombo(powerGroup, SWT.BORDER);
						regressionTime.setBounds(246, 284, 47, 18);
						regressionTime.setItems(new String[] {" 1 s", " 2 s", " 3 s", " 4 s", " 5 s", " 6 s", " 7 s", " 8 s", " 9 s", "10 s"});
						regressionTime.select(3);
						regressionTime.setToolTipText("Regressionszeit in Sekunden");
						regressionTime.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								System.out.println("regressionTime.widgetSelected, event="+evt);
								int regressionTime_sec = regressionTime.getSelectionIndex() + 1;
								String measurementKey = device.getMeasurementNames(configName)[10]; //10=slope
								device.setPropertyValue(configName, measurementKey, CalculationThread.REGRESSION_INTERVAL_SEC, DataTypes.INTEGER, regressionTime_sec);
							}
						});
					}
				}
				{
					axModusGroup = new Group(this, SWT.NONE);
					axModusGroup.setLayout(null);
					axModusGroup.setText("A* Konfiguration");
					axModusGroup.setBounds(313, 2, 310, 193);
					axModusGroup.setToolTipText("HIer bitte die Konfiguration für die A* Ausgange festlegen");
					axModusGroup.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							if (log.isLoggable(Level.FINEST))  log.finest("axModusGroup.paintControl, event="+evt);
							String recordKey = device.getMeasurementNames(configName)[11];
							MeasurementType measurement = device.getMeasurement(configName, recordKey);
							a1ValueButton.setSelection(measurement.isActive());
							a1Text.setText(measurement.getName());
							a1Unit.setText(measurement.getUnit());
							a1Offset.setText(String.format("%.2f", device.getOffset(configName, recordKey)));
							a1Factor.setText(String.format("%.2f", device.getFactor(configName, recordKey)));
							
							recordKey = device.getMeasurementNames(configName)[12];
							measurement = device.getMeasurement(configName, recordKey);
							a2ValueButton.setSelection(measurement.isActive());
							a2Text.setText(measurement.getName());
							a2Unit.setText(measurement.getUnit());
							a2Offset.setText(String.format("%.2f", device.getOffset(configName, recordKey)));
							a2Factor.setText(String.format("%.2f", device.getFactor(configName, recordKey)));

							recordKey = device.getMeasurementNames(configName)[13];
							measurement = device.getMeasurement(configName, recordKey);
							a3ValueButton.setSelection(measurement.isActive());
							a3Text.setText(measurement.getName());
							a3Unit.setText(measurement.getUnit());
							a3Offset.setText(String.format("%.2f", device.getOffset(configName, recordKey)));
							a3Factor.setText(String.format("%.2f", device.getFactor(configName, recordKey)));
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
										dialog.updateConfigurationValues(device.getSerialPort().readConfiguration());
									}
									a1Text.setText(UniLogDialog.A1_MODUS[dialog.getSelectionIndexA1ModusCombo()]);
								}
								catch (Exception e) {
									application.openMessageDialog(e.getMessage());
								}
							}
						});
					}
					{
						axName = new CLabel(axModusGroup, SWT.LEFT);
						axName.setBounds(42, 50, 96, 20);
						axName.setText("Bezeichnung");
						axName.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 1, false, false));
					}
					{
						axUnit = new CLabel(axModusGroup, SWT.LEFT);
						axUnit.setBounds(157, 50, 45, 20);
						axUnit.setText("Einheit");
						axUnit.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 1, false, false));
					}
					{
						axOffset = new CLabel(axModusGroup, SWT.LEFT);
						axOffset.setBounds(206, 50, 46, 20);
						axOffset.setText("Offset");
						axOffset.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 1, false, false));
					}
					{
						axFactor = new CLabel(axModusGroup, SWT.LEFT);
						axFactor.setBounds(252, 50, 50, 20);
						axFactor.setText("Factor");
						axFactor.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 1, false, false));
					}
					{
						a1ValueButton = new Button(axModusGroup, SWT.CHECK | SWT.LEFT);
						a1ValueButton.setBounds(7, 71, 35, 18);
						a1ValueButton.setText("A1");
						a1ValueButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a1ValueButton.widgetSelected, event="+evt);
								setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a1Text = new Text(axModusGroup, SWT.BORDER);
						a1Text.setBounds(42, 72, 120, 18);
						a1Text.setText("Speed 250");
						a1Text.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a1Text.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a1Unit = new Text(axModusGroup, SWT.CENTER | SWT.BORDER);
						a1Unit.setBounds(162, 72, 40, 18);
						a1Unit.setText("°C");
						a1Unit.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 0, false, false));
						a1Unit.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a1Unit.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a1Offset = new Text(axModusGroup, SWT.BORDER);
						a1Offset.setBounds(202, 72, 50, 18);
						a1Offset.setText("0.0");
						a1Offset.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a1Offset.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a1Factor = new Text(axModusGroup, SWT.BORDER);
						a1Factor.setBounds(252, 72, 50, 18);
						a1Factor.setText("1.0");
						a1Factor.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a1Factor.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a2ValueButton = new Button(axModusGroup, SWT.CHECK | SWT.LEFT);
						a2ValueButton.setBounds(7, 93, 35, 18);
						a2ValueButton.setText("A2");
						a2ValueButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a2ValueButton.widgetSelected, event="+evt);
								setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a2Text = new Text(axModusGroup, SWT.BORDER);
						a2Text.setBounds(42, 93, 120, 18);
						a2Text.setText("servoImpuls");
						a2Text.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a2Text.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a2Unit = new Text(axModusGroup, SWT.CENTER | SWT.BORDER);
						a2Unit.setBounds(162, 93, 40, 18);
						a2Unit.setText("°C");
						a2Unit.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 0, false, false));
						a2Unit.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a2Unit.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a2Offset = new Text(axModusGroup, SWT.BORDER);
						a2Offset.setBounds(202, 93, 50, 18);
						a2Offset.setText("0.0");
						a2Offset.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a2Offset.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a2Factor = new Text(axModusGroup, SWT.BORDER);
						a2Factor.setBounds(252, 93, 50, 18);
						a2Factor.setText("1.0");
						a2Factor.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a2Factor.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a3ValueButton = new Button(axModusGroup, SWT.CHECK | SWT.LEFT);
						a3ValueButton.setBounds(7, 115, 35, 18);
						a3ValueButton.setText("A3");
						a3ValueButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a3ValueButton.widgetSelected, event="+evt);
								setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a3Text = new Text(axModusGroup, SWT.BORDER);
						a3Text.setBounds(42, 115, 120, 18);
						a3Text.setText("Temperatur");
						a3Text.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a3Text.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a3Unit = new Text(axModusGroup, SWT.CENTER | SWT.BORDER);
						a3Unit.setBounds(162, 115, 40, 18);
						a3Unit.setText("°C");
						a3Unit.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 0, false, false));
						a3Unit.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a3Unit.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a3Offset = new Text(axModusGroup, SWT.BORDER);
						a3Offset.setBounds(202, 115, 50, 18);
						a3Offset.setText("0.0");
						a3Offset.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a3Offset.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a3Factor = new Text(axModusGroup, SWT.BORDER);
						a3Factor.setBounds(252, 115, 50, 18);
						a3Factor.setText("1.0");
						a3Factor.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a3Factor.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a23InternModus = new Button(axModusGroup, SWT.PUSH | SWT.CENTER);
						a23InternModus.setBounds(7, 153, 146, 25);
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
						a23ExternModus.setBounds(159, 153, 139, 26);
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
					setConfigButton = new Button(this, SWT.PUSH | SWT.CENTER);
					setConfigButton.setBounds(364, 235, 210, 42);
					setConfigButton.setText("Konfiguration speichern");
					setConfigButton.setEnabled(false);
					setConfigButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST))  log.finest("setConfigButton.widgetSelected, event="+evt);
							collectAndUpdateConfiguration();
							device.store();
							log.info(device.getChannel(1).toString());
							setConfigButton.setEnabled(false);
						}
					});
				}
			}
			this.layout();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		slopeLabel.setEnabled(enabled);
		slopeSymbol.setEnabled(enabled);
		slopeUnit.setEnabled(enabled);
		if (enabled) {
			slopeLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			slopeSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			slopeUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		}
		else {
			slopeLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			slopeSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			slopeUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
		}
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
			a2Values = new String[] {"Temperatur A2", "A2", "°C", "0.0", "1.0"};
			a3Values = new String[] {"Temperatur A3", "A3", "°C", "0.0", "1.0"};
			break;
		case 'I':	// intern
		default:
			a2Values = new String[] {"ServoImpuls", "A2", "ms", "0.0", "1.0"};
			a3Values = new String[] {"TempIntern", "A3", "°C", "0.0", "1.0"};
			break;
		}
		a2Text.setText(a2Values[0]);
		a2Unit.setText(a2Values[2]);
		a2Offset.setText(a2Values[3]);
		a2Factor.setText(a2Values[4]);
		
		a3Text.setText(a3Values[0]);
		a3Unit.setText(a3Values[2]);
		a3Offset.setText(a3Values[3]);
		a3Factor.setText(a3Values[4]);
	}

	/**
	 * enable or disable voltage and current dependent measurement fields
	 * @param enabled true | false
	 */
	private void updateVoltageAndCurrentDependent(boolean enabled) {
		capacityLabel.setEnabled(enabled);
		capacitySymbol.setEnabled(enabled);
		capacityUnit.setEnabled(enabled);
		powerLabel.setEnabled(enabled);
		powerUnit.setEnabled(enabled);
		powerSymbol.setEnabled(enabled);
		energyLabel.setEnabled(enabled);
		energyUnit.setEnabled(enabled);
		energySymbol.setEnabled(enabled);
		voltagePerCellLabel.setEnabled(enabled);
		voltagePerCellUnit.setEnabled(enabled);
		voltagePerCellSymbol.setEnabled(enabled);
		numCellLabel.setEnabled(enabled);
		numCellInput.setEnabled(enabled);
		if (enabled) {
			capacityLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			capacitySymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			capacityUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			powerLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			powerUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			powerSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			energyLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			energyUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			energySymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			voltagePerCellLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			voltagePerCellUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			voltagePerCellSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			numCellLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			numCellInput.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		}
		else {
			capacityLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			capacitySymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			capacityUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			powerLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			powerUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			powerSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			energyLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			energyUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			energySymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			voltagePerCellLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			voltagePerCellUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			voltagePerCellSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			numCellLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			numCellInput.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
		}
	}
	
	/**
	 * collect all configuration relevant data and update device configuration
	 */
	private void collectAndUpdateConfiguration() {
		String recordKey = device.getMeasurementNames(configName)[0];
		device.setMeasurementActive(configName, recordKey, reveiverVoltageButton.getSelection());

		recordKey = device.getMeasurementNames(configName)[1];
		device.setMeasurementActive(configName, recordKey, voltageButton.getSelection());
		
		recordKey = device.getMeasurementNames(configName)[2];
		device.setMeasurementActive(configName, recordKey, currentButton.getSelection());

		recordKey = device.getMeasurementNames(configName)[7];
		device.setMeasurementActive(configName, recordKey, revolutionButton.getSelection());
		
		recordKey = device.getMeasurementNames(configName)[9];
		device.setMeasurementActive(configName, recordKey, heightButton.getSelection());
		
		recordKey = device.getMeasurementNames(configName)[11];
		device.setMeasurementActive(configName, recordKey, a1ValueButton.getSelection());
		device.setMeasurementName(configName, recordKey, a1Text.getText());
		device.setMeasurementUnit(configName, recordKey, a1Unit.getText());
		device.setOffset(configName, recordKey, new Double(a1Offset.getText()));
		device.setFactor(configName, recordKey, new Double(a1Factor.getText()));
		
		recordKey = device.getMeasurementNames(configName)[12];
		device.setMeasurementActive(configName, recordKey, a2ValueButton.getSelection());
		device.setMeasurementName(configName, recordKey, a2Text.getText());
		device.setMeasurementUnit(configName, recordKey, a2Unit.getText());
		device.setOffset(configName, recordKey, new Double(a2Offset.getText()));
		device.setFactor(configName, recordKey, new Double(a2Factor.getText()));

		recordKey = device.getMeasurementNames(configName)[13];
		device.setMeasurementActive(configName, recordKey, a3ValueButton.getSelection());
		device.setMeasurementName(configName, recordKey, a3Text.getText());
		device.setMeasurementUnit(configName, recordKey, a3Unit.getText());
		device.setOffset(configName, recordKey, new Double(a3Offset.getText()));
		device.setFactor(configName, recordKey, new Double(a3Factor.getText()));
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

	/**
	 * @param isA1ModusAvailable the isA1ModusAvailable to set
	 */
	public void setA1ModusAvailable(boolean isA1ModusAvailable) {
		this.isA1ModusAvailable = isA1ModusAvailable;
	}
	
	/**
	 * @return set configuration button status, true(enabled) if configuration has been changed
	 */
	public boolean getConfigButtonStatus() {
		return this.setConfigButton.getEnabled();
	}

}
