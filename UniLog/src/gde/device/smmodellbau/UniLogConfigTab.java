package osde.device.smmodellbau;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
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

import osde.device.MeasurementType;
import osde.device.PropertyType;
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
public class UniLogConfigTab extends org.eclipse.swt.widgets.Composite {
	private Logger												log	= Logger.getLogger(this.getClass().getName());
	
	private CLabel												receiverVoltageSymbol, receiverVoltageUnit;
	private CLabel												voltageSymbol, voltageUnit;
	private CLabel												currentSymbol, currentUnit;
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
	private Text													a1Symbol;
	private CLabel												etaUnit;
	private CLabel												etaSymbol;
	private CLabel												slopeUnit;
	private CLabel												slopeSymbol;
	private CLabel												etaButton;
	private CLabel												slopeButton;
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
	private Text													a2Symbol;
	private CLabel												voltagePerCellUnit;
	private CLabel												voltagePerCell;
	private CLabel												energyUnit;
	private CLabel												energySymbol;
	private CLabel												powerUnit;
	private CLabel												powerSymbol;
	private CLabel												capacityUnitLabel;
	private CLabel												capacitySignLabel;
	private CLabel												cellVoltageLabel;
	private CLabel												energyLabel;
	private CLabel												powerLabel;
	private Text													a3Text;
	private Text													a2Text;
	private Text													a1Text;
	private CLabel												prop100WLabel;
	private Text													a3Symbol;
	private Button												setConfigButton;

	private boolean isA1ModusAvailable = false;
	private int prop100WValue = 3400;
	private int numCellValue = 12;

	private final String									configName; // tabName
	private final UniLogDialog						dialog;
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
			this.setSize(310, 514);
			{
				this.setLayout(null);
				{
					powerGroup = new Group(this, SWT.NONE);
					powerGroup.setBounds(0, 0, 310, 291);
					powerGroup.setLayout(null);
					powerGroup.setText("Versorgung/Antrieb/Höhe");
					powerGroup.setToolTipText("Hier bitte alle Datenkanäle auswählen, die angezeigt werden sollen");
					powerGroup.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							if (log.isLoggable(Level.FINEST))  log.finest("powerGroup.paintControl, event="+evt);
							String recordKey = device.getMeasurementNames(configName)[0];
							MeasurementType measurement = device.getMeasurementDefinition(configName, recordKey);
							reveiverVoltageButton.setSelection(measurement.isActive());
							reveiverVoltageButton.setText(measurement.getName());
							receiverVoltageSymbol.setText(measurement.getSymbol());
							receiverVoltageUnit.setText(measurement.getUnit());

							recordKey = device.getMeasurementNames(configName)[1];
							measurement = device.getMeasurementDefinition(configName, recordKey);
							voltageButton.setSelection(measurement.isActive());
							voltageButton.setText(measurement.getName());
							voltageSymbol.setText(measurement.getSymbol());
							voltageUnit.setText(measurement.getUnit());
							
							recordKey = device.getMeasurementNames(configName)[2];
							measurement = device.getMeasurementDefinition(configName, recordKey);
							currentButton.setSelection(measurement.isActive());
							currentButton.setText(measurement.getName());
							currentSymbol.setText(measurement.getSymbol());
							currentUnit.setText(measurement.getUnit());
							
							// capacity, power, energy
							updateVoltageAndCurrentDependent(voltageButton.getSelection() && currentButton.getSelection());
							
							// number cells voltagePerCell
							recordKey = device.getMeasurementNames(configName)[6];
							numCellValue = (Integer)device.getPropertyValue(configName, recordKey, UniLogDialog.NUMBER_CELLS);

							recordKey = device.getMeasurementNames(configName)[7];
							measurement = device.getMeasurementDefinition(configName, recordKey);
							revolutionButton.setSelection(measurement.isActive());
							revolutionButton.setText(measurement.getName());
							revolutionSymbol.setText(measurement.getSymbol());
							revolutionUnit.setText(measurement.getUnit());
							
							// n100W value, eta calculation 										
							updateVoltageCurrentRevolutionDependent(voltageButton.getSelection() && currentButton.getSelection() && revolutionButton.getSelection());
							recordKey = device.getMeasurementNames(configName)[8];
							prop100WValue = (Integer)device.getPropertyValue(configName, recordKey, UniLogDialog.PROP_N_100_WATT);
							
							recordKey = device.getMeasurementNames(configName)[9];
							measurement = device.getMeasurementDefinition(configName, recordKey);
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
						capacityLabel = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
						capacityLabel.setText("Ladung");
						capacityLabel.setBounds(64, 80, 120, 20);
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
						powerLabel = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
						powerLabel.setText("Leistung");
						powerLabel.setBounds(64, 100, 120, 20);
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
						energyLabel = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
						energyLabel.setText("Energie");
						energyLabel.setBounds(64, 120, 120, 20);
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
						cellVoltageLabel = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
						cellVoltageLabel.setText("Spannung/Zelle");
						cellVoltageLabel.setBounds(64, 140, 120, 20);
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
						numCellInput.addKeyListener(new KeyAdapter() {
							public void keyPressed(KeyEvent evt) {
								if (evt.character == SWT.CR) {
									if (log.isLoggable(Level.FINEST))  log.finest("numCellInput.widgetSelected, event="+evt);
									dialog.enableStoreAdjustmentsButton(true);
									numCellValue = new Integer(numCellInput.getText().trim()).intValue();
									device.setPropertyValue(configName, device.getMeasurementNames(configName)[6], UniLogDialog.NUMBER_CELLS, PropertyType.Types.Integer, numCellValue); //6=votagePerCell
									setConfigButton.setEnabled(true);
								}
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
						prop100WInput.addKeyListener(new KeyAdapter() {
							public void keyPressed(KeyEvent evt) {
								if (evt.character == SWT.CR) {
									if (log.isLoggable(Level.FINEST))  log.finest("prop100WCombo.widgetSelected, event="+evt);
									dialog.enableStoreAdjustmentsButton(true);
									prop100WValue = new Integer(prop100WInput.getText().trim()).intValue();
									device.setPropertyValue(configName, device.getMeasurementNames(configName)[8], UniLogDialog.PROP_N_100_WATT, PropertyType.Types.Integer, prop100WValue); //8=efficiency
									setConfigButton.setEnabled(true);
								}
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
					axModusGroup = new Group(this, SWT.NONE);
					axModusGroup.setLayout(null);
					axModusGroup.setText("A* Konfiguration");
					axModusGroup.setBounds(0, 291, 310, 175);
					axModusGroup.setToolTipText("HIer bitte die Konfiguration für die A* Ausgange festlegen");
					axModusGroup.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							if (log.isLoggable(Level.FINEST))  log.finest("axModusGroup.paintControl, event="+evt);
							String recordKey = device.getMeasurementNames(configName)[11];
							MeasurementType measurement = device.getMeasurementDefinition(configName, recordKey);
							a1ValueButton.setSelection(measurement.isActive());
							a1Text.setText(measurement.getName());
							a1Symbol.setText(measurement.getSymbol());
							a1Unit.setText(measurement.getUnit());
							a1Offset.setText(String.format("%.2f", device.getOffset(configName, recordKey)));
							a1Factor.setText(String.format("%.2f", device.getFactor(configName, recordKey)));
							
							recordKey = device.getMeasurementNames(configName)[12];
							measurement = device.getMeasurementDefinition(configName, recordKey);
							a2ValueButton.setSelection(measurement.isActive());
							a2Text.setText(measurement.getName());
							a2Symbol.setText(measurement.getSymbol());
							a2Unit.setText(measurement.getUnit());
							a2Offset.setText(String.format("%.2f", device.getOffset(configName, recordKey)));
							a2Factor.setText(String.format("%.2f", device.getFactor(configName, recordKey)));

							recordKey = device.getMeasurementNames(configName)[13];
							measurement = device.getMeasurementDefinition(configName, recordKey);
							a3ValueButton.setSelection(measurement.isActive());
							a3Text.setText(measurement.getName());
							a3Symbol.setText(measurement.getSymbol());
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
						a1ValueButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a1ValueButton.widgetSelected, event="+evt);
								setConfigButton.setEnabled(true);
							}
						});
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
						a2ValueButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a2ValueButton.widgetSelected, event="+evt);
								setConfigButton.setEnabled(true);
							}
						});
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
						a3ValueButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a3ValueButton.widgetSelected, event="+evt);
								setConfigButton.setEnabled(true);
							}
						});
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
					setConfigButton = new Button(this, SWT.PUSH | SWT.CENTER);
					setConfigButton.setBounds(45, 477, 210, 25);
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
		slopeButton.setEnabled(enabled);
		slopeSymbol.setEnabled(enabled);
		slopeUnit.setEnabled(enabled);
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
		capacityLabel.setEnabled(enabled);
		capacitySignLabel.setEnabled(enabled);
		capacityUnitLabel.setEnabled(enabled);
		powerLabel.setEnabled(enabled);
		powerSymbol.setEnabled(enabled);
		powerUnit.setEnabled(enabled);
		energyLabel.setEnabled(enabled);
		energySymbol.setEnabled(enabled);
		energyUnit.setEnabled(enabled);
		cellVoltageLabel.setEnabled(enabled);
		voltagePerCell.setEnabled(enabled);
		voltagePerCellUnit.setEnabled(enabled);
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
	 * @return the isA1ModusAvailable
	 */
	public boolean isA1ModusAvailable() {
		return isA1ModusAvailable;
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
