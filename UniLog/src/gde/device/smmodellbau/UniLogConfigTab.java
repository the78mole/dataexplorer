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
import osde.device.PropertyType;
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
	private CLabel 												voltageUnit;
	private CLabel 												currentUnit;
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
	private Button												a3Button;
	private Button												a1Button;
	private Button												a2Button;
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

	private String												configName; // tabName
	private final UniLogDialog						dialog;
	private CLabel calculationTypeLabel;
	private CCombo slopeCalculationTypeCombo;
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
			this.setSize(630, 340);
			{
				this.setLayout(null);
				{
					powerGroup = new Group(this, SWT.NONE);
					powerGroup.setBounds(5, 2, 299, 331);
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
							receiverVoltageUnit.setText("[" + measurement.getUnit() + "]");

							recordKey = device.getMeasurementNames(configName)[1];
							measurement = device.getMeasurement(configName, recordKey);
							voltageButton.setSelection(measurement.isActive());
							voltageButton.setText(measurement.getName());
							voltageSymbol.setText(measurement.getSymbol());
							voltageUnit.setText("[" + measurement.getUnit() + "]");
							
							recordKey = device.getMeasurementNames(configName)[2];
							measurement = device.getMeasurement(configName, recordKey);
							currentButton.setSelection(measurement.isActive());
							currentButton.setText(measurement.getName());
							currentSymbol.setText(" " + measurement.getSymbol());
							currentUnit.setText("[" + measurement.getUnit() + "]");
							
							recordKey = device.getMeasurementNames(configName)[3];
							measurement = device.getMeasurement(configName, recordKey);
							capacityLabel.setText(measurement.getName());
							capacitySymbol.setText(measurement.getSymbol());
							capacityUnit.setText("[" + measurement.getUnit() + "]");

							recordKey = device.getMeasurementNames(configName)[4];
							measurement = device.getMeasurement(configName, recordKey);
							powerLabel.setText(measurement.getName());
							powerSymbol.setText(measurement.getSymbol());
							powerUnit.setText("[" + measurement.getUnit() + "]");

							recordKey = device.getMeasurementNames(configName)[5];
							measurement = device.getMeasurement(configName, recordKey);
							energyLabel.setText(measurement.getName());
							energySymbol.setText(measurement.getSymbol());
							energyUnit.setText("[" + measurement.getUnit() + "]");

							// capacity, power, energy
							updateStateVoltageAndCurrentDependent(voltageButton.getSelection() && currentButton.getSelection());
							
							// number cells voltagePerCell
							recordKey = device.getMeasurementNames(configName)[6];
							measurement = device.getMeasurement(configName, recordKey);
							voltagePerCellLabel.setText(measurement.getName());
							voltagePerCellSymbol.setText(measurement.getSymbol());
							voltagePerCellUnit.setText("[" + measurement.getUnit() + "]");
							PropertyType property = device.getMeasruementProperty(configName, recordKey, UniLogDialog.NUMBER_CELLS);
							numCellValue = property != null ? new Integer(property.getValue()) : 4;
							numCellInput.setText(" " + numCellValue);

							recordKey = device.getMeasurementNames(configName)[7];
							measurement = device.getMeasurement(configName, recordKey);
							revolutionButton.setSelection(measurement.isActive());
							revolutionButton.setText(measurement.getName());
							revolutionSymbol.setText(measurement.getSymbol());
							revolutionUnit.setText("[" + measurement.getUnit() + "]");
							
							recordKey = device.getMeasurementNames(configName)[8];
							measurement = device.getMeasurement(configName, recordKey);
							etaButton.setText(measurement.getName());
							etaSymbol.setText(measurement.getSymbol());
							etaUnit.setText("[" + measurement.getUnit() + "]");
							property = device.getMeasruementProperty(configName, recordKey, UniLogDialog.PROP_N_100_WATT);
							prop100WValue = property != null ? new Integer(property.getValue()) : 10000;
							prop100WInput.setText(" " + prop100WValue);

							
							// n100W value, eta calculation 										
							updateStateVoltageCurrentRevolutionDependent(voltageButton.getSelection() && currentButton.getSelection() && revolutionButton.getSelection());
							
							recordKey = device.getMeasurementNames(configName)[9];
							measurement = device.getMeasurement(configName, recordKey);
							heightButton.setSelection(measurement.isActive());
							heightButton.setText(measurement.getName());
							heightSymbol.setText(measurement.getSymbol());
							heightUnit.setText("[" + measurement.getUnit() + "]");
							
							recordKey = device.getMeasurementNames(configName)[10];
							measurement = device.getMeasurement(configName, recordKey);
							slopeLabel.setText(measurement.getName());
							slopeSymbol.setText(measurement.getSymbol());
							slopeUnit.setText("[" + measurement.getUnit() + "]");

							updateHeightDependent(heightButton.getSelection());

							PropertyType typeSelection = device.getMeasruementProperty(configName, device.getMeasurementNames(configName)[10], CalculationThread.REGRESSION_INTERVAL_SEC);
							int intSelection;
							if (typeSelection == null)  intSelection = 4;
							else intSelection = new Integer(typeSelection.getValue());
							regressionTime.select(intSelection-1);

							typeSelection = device.getMeasruementProperty(configName, device.getMeasurementNames(configName)[10], CalculationThread.REGRESSION_TYPE);
							if (typeSelection == null)  intSelection = 1;
							else {
								String selectionKey = typeSelection.getValue();
								if(selectionKey.equals(CalculationThread.REGRESSION_TYPE_CURVE)) intSelection = 1; // quasilinear
								else intSelection = 0; // linear
							}
							slopeCalculationTypeCombo.select(intSelection);
						}
					});
					{
						reveiverVoltageButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
						reveiverVoltageButton.setBounds(23, 20, 132, 18);
						reveiverVoltageButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("reveiverVoltageButton.widgetSelected, event="+evt);
								setConfigButton.setEnabled(true);
							}
						});
					}
					{
						receiverVoltageSymbol = new CLabel(powerGroup, SWT.NONE);
						receiverVoltageSymbol.setBounds(158, 18, 40, 20);
					}
					{
						receiverVoltageUnit = new CLabel(powerGroup, SWT.NONE);
						receiverVoltageUnit.setBounds(198, 18, 40, 20);
					}
					{
						voltageButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
						voltageButton.setBounds(23, 42, 120, 18);
						voltageButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("voltageButton.widgetSelected, event="+evt);
								updateStateVoltageAndCurrentDependent(voltageButton.getSelection() && currentButton.getSelection());
								updateStateVoltageCurrentRevolutionDependent(voltageButton.getSelection() && currentButton.getSelection() && revolutionButton.getSelection());
								setConfigButton.setEnabled(true);
							}
						});
					}
					{
						voltageSymbol = new CLabel(powerGroup, SWT.NONE);
						voltageSymbol.setBounds(158, 40, 40, 20);
					}
					{
						voltageUnit = new CLabel(powerGroup, SWT.NONE);
						voltageUnit.setBounds(198, 40, 40, 20);
					}
					{
						currentButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
						currentButton.setBounds(23, 64, 120, 18);
						currentButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("currentButton.widgetSelected, event="+evt);
								updateStateVoltageAndCurrentDependent(voltageButton.getSelection() && currentButton.getSelection());
								updateStateVoltageCurrentRevolutionDependent(voltageButton.getSelection() && currentButton.getSelection() && revolutionButton.getSelection());
								setConfigButton.setEnabled(true);
							}
						});
					}
					{
						currentSymbol = new CLabel(powerGroup, SWT.NONE);
						currentSymbol.setBounds(158, 62, 30, 18);
					}
					{
						currentUnit = new CLabel(powerGroup, SWT.NONE);
						currentUnit.setBounds(198, 62, 62, 20);
					}
					{
						capacityLabel = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
						capacityLabel.setBounds(37, 86, 120, 20);
					}
					{
						capacitySymbol = new CLabel(powerGroup, SWT.NONE);
						capacitySymbol.setBounds(158, 84, 40, 20);
					}
					{
						capacityUnit = new CLabel(powerGroup, SWT.NONE);
						capacityUnit.setBounds(198, 84, 40, 20);
					}
					{
						powerLabel = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
						powerLabel.setBounds(37, 108, 120, 20);
					}
					{
						powerSymbol = new CLabel(powerGroup, SWT.NONE);
						powerSymbol.setBounds(158, 106, 40, 20);
					}
					{
						powerUnit = new CLabel(powerGroup, SWT.NONE);
						powerUnit.setBounds(198, 106, 40, 20);
					}
					{
						energyLabel = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
						energyLabel.setBounds(37, 130, 120, 20);
					}
					{
						energySymbol = new CLabel(powerGroup, SWT.NONE);
						energySymbol.setBounds(158, 128, 40, 20);
					}
					{
						energyUnit = new CLabel(powerGroup, SWT.NONE);
						energyUnit.setBounds(198, 128, 40, 20);
					}
					{
						voltagePerCellLabel = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
						voltagePerCellLabel.setBounds(37, 152, 120, 20);
					}
					{
						voltagePerCellSymbol = new CLabel(powerGroup, SWT.NONE);
						voltagePerCellSymbol.setBounds(158, 150, 40, 20);
					}
					{
						voltagePerCellUnit = new CLabel(powerGroup, SWT.NONE);
						voltagePerCellUnit.setBounds(198, 150, 40, 20);
					}
					{
						numCellLabel = new CLabel(powerGroup, SWT.LEFT);
						numCellLabel.setBounds(37, 172, 118, 18);
						numCellLabel.setText("Anzahl Akkuzellen");
					}
					{
						numCellInput = new Text(powerGroup, SWT.LEFT | SWT.BORDER);
						numCellInput.setBounds(158, 173, 40, 20);
						numCellInput.setToolTipText("Hier die Anzahl der Akkuzellen einsetzen");
						numCellInput.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("numCellInput.keyReleased, event="+evt);
								if (evt.character == SWT.CR) {
									setConfigButton.setEnabled(true);
									numCellValue = new Integer(numCellInput.getText().trim());
									numCellInput.setText(" " + numCellValue); 
									device.setMeasurementPropertyValue(configName, device.getMeasurementNames(configName)[6], UniLogDialog.NUMBER_CELLS, DataTypes.INTEGER, numCellValue);
								}
							}
						});
					}
					{
						revolutionButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
						revolutionButton.setBounds(23, 196, 135, 18);
						revolutionButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("revolutionButton.widgetSelected, event="+evt);
								updateStateVoltageCurrentRevolutionDependent(voltageButton.getSelection() && currentButton.getSelection() && revolutionButton.getSelection());
								setConfigButton.setEnabled(true);
							}
						});
					}
					{
						revolutionSymbol = new CLabel(powerGroup, SWT.NONE);
						revolutionSymbol.setBounds(158, 194, 40, 20);
					}
					{
						revolutionUnit = new CLabel(powerGroup, SWT.NONE);
						revolutionUnit.setBounds(198, 194, 40, 20);
					}
					{
						prop100WLabel = new CLabel(powerGroup, SWT.LEFT);
						prop100WLabel.setBounds(37, 216, 118, 18);
						prop100WLabel.setText("Propeller n100W");
					}
					{
						prop100WInput = new Text(powerGroup, SWT.LEFT | SWT.BORDER);
						prop100WInput.setBounds(158, 217, 40, 20);
						prop100WInput.setToolTipText("Hier die Derhzahl des Propellers bei 100 Watt einsetzen");
						prop100WInput.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("prop100WInput.keyReleased, event="+evt);
								if (evt.character == SWT.CR) {
									setConfigButton.setEnabled(true);
									prop100WValue = new Integer(prop100WInput.getText().trim());
									prop100WInput.setText(" " + prop100WValue);
									device.setMeasurementPropertyValue(configName, device.getMeasurementNames(configName)[8], UniLogDialog.PROP_N_100_WATT, DataTypes.INTEGER, prop100WValue);
								}
							}
						});
					}
					{
						prop100WUnit = new CLabel(powerGroup, SWT.NONE);
						prop100WUnit.setBounds(198, 216, 88, 20);
						prop100WUnit.setText("100W  * 1/min");
					}
					{
						etaButton = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
						etaButton.setBounds(37, 240, 108, 20);
					}
					{
						etaSymbol = new CLabel(powerGroup, SWT.NONE);
						etaSymbol.setBounds(158, 239, 40, 20);
					}
					{
						etaUnit = new CLabel(powerGroup, SWT.NONE);
						etaUnit.setBounds(198, 238, 40, 20);
					}
					{
						heightButton = new Button(powerGroup, SWT.CHECK | SWT.LEFT);
						heightButton.setBounds(23, 262, 120, 18);
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
						heightSymbol.setBounds(158, 260, 40, 20);
					}
					{
						heightUnit = new CLabel(powerGroup, SWT.NONE);
						heightUnit.setBounds(198, 260, 40, 20);
					}
					{
						slopeLabel = new CLabel(powerGroup, SWT.CHECK | SWT.LEFT);
						slopeLabel.setBounds(37, 282, 120, 19);
					}
					{
						slopeSymbol = new CLabel(powerGroup, SWT.NONE);
						slopeSymbol.setBounds(158, 282, 40, 20);
					}
					{
						slopeUnit = new CLabel(powerGroup, SWT.NONE);
						slopeUnit.setBounds(198, 282, 40, 20);
					}
					{
						calculationTypeLabel = new CLabel(powerGroup, SWT.NONE);
						calculationTypeLabel.setBounds(48, 304, 79, 20);
						calculationTypeLabel.setText("Berechnung");
					}
					{
						slopeCalculationTypeCombo = new CCombo(powerGroup, SWT.BORDER);
						slopeCalculationTypeCombo.setBounds(133, 304, 97, 20);
						slopeCalculationTypeCombo.setItems(new String[] {" "+ CalculationThread.REGRESSION_TYPE_LINEAR, " " + CalculationThread.REGRESSION_TYPE_CURVE });
						slopeCalculationTypeCombo.setToolTipText("Hier den Berechnungstyp einstellen");
						slopeCalculationTypeCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("slopeCalculationTypeCombo.widgetSelected, event="+evt);
								String calcType;
								if (slopeCalculationTypeCombo.getSelectionIndex() == 1) calcType = CalculationThread.REGRESSION_TYPE_CURVE;
								else calcType = CalculationThread.REGRESSION_TYPE_LINEAR;
								String measurementKey = device.getMeasurementNames(configName)[10]; //10=slope
								device.setMeasurementPropertyValue(configName, measurementKey, CalculationThread.REGRESSION_TYPE, DataTypes.STRING, calcType);
								collectAndUpdateConfiguration();
								//device.storeDeviceProperties();
								setConfigButton.setEnabled(true);
							}
						});
					}
					{
						regressionTime = new CCombo(powerGroup, SWT.BORDER);
						regressionTime.setBounds(232, 304, 61, 20);
						regressionTime.setItems(new String[] {" 1 s", " 2 s", " 3 s", " 4 s", " 5 s", " 6 s", " 7 s", " 8 s", " 9 s", "10 s", "11 s", "12 s", "13 s", "14 s", "15 s", "16 s", "17 s", "18 s", "19 s", "20 s"});
						regressionTime.setToolTipText("Hier die Regressionszeit in Sekunden einstellen");
						regressionTime.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("regressionTime.widgetSelected, event="+evt);
								int regressionTime_sec = regressionTime.getSelectionIndex() + 1;
								String measurementKey = device.getMeasurementNames(configName)[10]; //10=slope
								device.setMeasurementPropertyValue(configName, measurementKey, CalculationThread.REGRESSION_INTERVAL_SEC, DataTypes.INTEGER, regressionTime_sec);
								collectAndUpdateConfiguration();
								//device.storeDeviceProperties();
								setConfigButton.setEnabled(true);
							}
						});
					}
				}
				{
					axModusGroup = new Group(this, SWT.NONE);
					axModusGroup.setLayout(null);
					axModusGroup.setText("A* Konfiguration");
					axModusGroup.setBounds(313, 2, 310, 193);
					axModusGroup.setToolTipText("Hier bitte die Konfiguration für die A* Ausgange festlegen");
					axModusGroup.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							if (log.isLoggable(Level.FINEST))  log.finest("axModusGroup.paintControl, event="+evt);
							String recordKey = device.getMeasurementNames(configName)[11];
							MeasurementType measurement = device.getMeasurement(configName, recordKey);
							a1Button.setSelection(measurement.isActive());
							a1Text.setText(measurement.getName());
							a1Unit.setText("[" + measurement.getUnit() + "]");
							a1Offset.setText(String.format("%.2f", device.getMeasurementOffset(configName, recordKey)));
							a1Factor.setText(String.format("%.2f", device.getMeasurementFactor(configName, recordKey)));
							
							recordKey = device.getMeasurementNames(configName)[12];
							measurement = device.getMeasurement(configName, recordKey);
							a2Button.setSelection(measurement.isActive());
							a2Text.setText(measurement.getName());
							a2Unit.setText("[" + measurement.getUnit() + "]");
							a2Offset.setText(String.format("%.2f", device.getMeasurementOffset(configName, recordKey)));
							a2Factor.setText(String.format("%.2f", device.getMeasurementFactor(configName, recordKey)));

							recordKey = device.getMeasurementNames(configName)[13];
							measurement = device.getMeasurement(configName, recordKey);
							a3Button.setSelection(measurement.isActive());
							a3Text.setText(measurement.getName());
							a3Unit.setText("[" + measurement.getUnit() + "]");
							a3Offset.setText(String.format("%.2f", device.getMeasurementOffset(configName, recordKey)));
							a3Factor.setText(String.format("%.2f", device.getMeasurementFactor(configName, recordKey)));
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
						axName.setBounds(47, 50, 116, 18);
						axName.setText("Bezeichnung");
						axName.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 1, false, false));
					}
					{
						axUnit = new CLabel(axModusGroup, SWT.LEFT);
						axUnit.setBounds(160, 50, 45, 20);
						axUnit.setText("Einheit");
						axUnit.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 1, false, false));
					}
					{
						axOffset = new CLabel(axModusGroup, SWT.LEFT);
						axOffset.setBounds(209, 50, 46, 20);
						axOffset.setText("Offset");
						axOffset.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 1, false, false));
					}
					{
						axFactor = new CLabel(axModusGroup, SWT.LEFT);
						axFactor.setBounds(255, 50, 50, 20);
						axFactor.setText("Factor");
						axFactor.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 1, false, false));
					}
					{
						a1Button = new Button(axModusGroup, SWT.CHECK | SWT.LEFT);
						a1Button.setBounds(4, 71, 41, 18);
						a1Button.setText("A1");
						a1Button.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a1ValueButton.widgetSelected, event="+evt);
								setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a1Text = new Text(axModusGroup, SWT.BORDER);
						a1Text.setBounds(49, 72, 116, 18);
						a1Text.setToolTipText("Name vom A1 Ausgang");
						a1Text.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a1Text.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a1Unit = new Text(axModusGroup, SWT.CENTER | SWT.BORDER);
						a1Unit.setBounds(165, 72, 40, 18);
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
						a1Offset.setBounds(205, 72, 50, 18);
						a1Offset.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a1Offset.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a1Factor = new Text(axModusGroup, SWT.BORDER);
						a1Factor.setBounds(255, 72, 50, 18);
						a1Factor.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a1Factor.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a2Button = new Button(axModusGroup, SWT.CHECK | SWT.LEFT);
						a2Button.setBounds(4, 93, 41, 18);
						a2Button.setText("A2");
						a2Button.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a2ValueButton.widgetSelected, event="+evt);
								setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a2Text = new Text(axModusGroup, SWT.BORDER);
						a2Text.setBounds(49, 93, 116, 18);
						a2Text.setToolTipText("Name vom A2 Ausgang");
						a2Text.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a2Text.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a2Unit = new Text(axModusGroup, SWT.CENTER | SWT.BORDER);
						a2Unit.setBounds(165, 93, 40, 18);
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
						a2Offset.setBounds(205, 93, 50, 18);
						a2Offset.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a2Offset.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a2Factor = new Text(axModusGroup, SWT.BORDER);
						a2Factor.setBounds(255, 93, 50, 18);
						a2Factor.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a2Factor.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a3Button = new Button(axModusGroup, SWT.CHECK | SWT.LEFT);
						a3Button.setBounds(4, 115, 41, 18);
						a3Button.setText("A3");
						a3Button.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a3ValueButton.widgetSelected, event="+evt);
								setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a3Text = new Text(axModusGroup, SWT.BORDER);
						a3Text.setBounds(49, 115, 116, 18);
						a3Text.setToolTipText("Name vom A3 Ausgang");
						a3Text.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a3Text.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a3Unit = new Text(axModusGroup, SWT.CENTER | SWT.BORDER);
						a3Unit.setBounds(165, 115, 40, 18);
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
						a3Offset.setBounds(205, 115, 50, 18);
						a3Offset.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (log.isLoggable(Level.FINEST))  log.finest("a3Offset.keyReleased, event="+evt);
								if (evt.character == SWT.CR) setConfigButton.setEnabled(true);
							}
						});
					}
					{
						a3Factor = new Text(axModusGroup, SWT.BORDER);
						a3Factor.setBounds(255, 115, 50, 18);
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
					setConfigButton.setBounds(366, 247, 210, 42);
					setConfigButton.setText("Konfiguration speichern");
					setConfigButton.setEnabled(false);
					setConfigButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST))  log.finest("setConfigButton.widgetSelected, event="+evt);
							collectAndUpdateConfiguration();
							device.storeDeviceProperties();
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
	private void updateStateVoltageCurrentRevolutionDependent(boolean enabled) {
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
			calculationTypeLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			slopeCalculationTypeCombo.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			regressionTime.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		}
		else {
			slopeLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			slopeSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			slopeUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			calculationTypeLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			slopeCalculationTypeCombo.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			regressionTime.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
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
			a2Values = new String[] {"ServoImpuls", "A2", "µs", "0.0", "1.0"};
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
	private void updateStateVoltageAndCurrentDependent(boolean enabled) {
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
		String measurementKey = device.getMeasurementNames(configName)[0];
		MeasurementType measurement = device.getMeasurement(configName, measurementKey);
		measurement.setActive(reveiverVoltageButton.getSelection());

		measurementKey = device.getMeasurementNames(configName)[1];
		measurement = device.getMeasurement(configName, measurementKey);
		measurement.setActive(voltageButton.getSelection());
		
		measurementKey = device.getMeasurementNames(configName)[2];
		measurement = device.getMeasurement(configName, measurementKey);
		measurement.setActive(currentButton.getSelection());

		measurementKey = device.getMeasurementNames(configName)[7];
		measurement = device.getMeasurement(configName, measurementKey);
		measurement.setActive(revolutionButton.getSelection());
		
		measurementKey = device.getMeasurementNames(configName)[9];
		measurement = device.getMeasurement(configName, measurementKey);
		measurement.setActive(heightButton.getSelection());
		
		measurementKey = device.getMeasurementNames(configName)[11];
		measurement = device.getMeasurement(configName, measurementKey);
		measurement.setActive(a1Button.getSelection());
		measurement.setName(a1Text.getText().trim());
		measurement.setUnit(a1Unit.getText().replace('[', ' ').replace(']', ' ').trim());
		measurement.setOffset(new Double(a1Offset.getText().replace(',', '.').trim()));
		measurement.setFactor(new Double(a1Factor.getText().replace(',', '.').trim()));
		
		measurementKey = device.getMeasurementNames(configName)[12];
		measurement = device.getMeasurement(configName, measurementKey);
		measurement.setActive(a2Button.getSelection());
		measurement.setName(a2Text.getText().trim());
		measurement.setUnit(a2Unit.getText().replace('[', ' ').replace(']', ' ').trim());
		measurement.setOffset(new Double(a2Offset.getText().replace(',', '.').trim()));
		measurement.setFactor(new Double(a2Factor.getText().replace(',', '.').trim()));

		measurementKey = device.getMeasurementNames(configName)[13];
		measurement = device.getMeasurement(configName, measurementKey);
		measurement.setActive(a3Button.getSelection());
		measurement.setName(a3Text.getText().trim());
		measurement.setUnit(a3Unit.getText().replace('[', ' ').replace(']', ' ').trim());
		measurement.setOffset(new Double(a3Offset.getText().replace(',', '.').trim()));
		measurement.setFactor(new Double(a3Factor.getText().replace(',', '.').trim()));
		
		device.setChangePropery(true);
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

	/**
	 * @param configName the configName to set
	 */
	public void setConfigName(String configName) {
		this.configName = configName;
	}

}
