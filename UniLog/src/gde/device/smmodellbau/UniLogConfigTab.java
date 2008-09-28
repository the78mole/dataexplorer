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

import osde.OSDE;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.DataTypes;
import osde.device.MeasurementType;
import osde.device.PropertyType;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.CalculationThread;

/**
 * Configuration tab container, adjustment of all active and dependent measurements
 * @author Winfried Brügmann
 */
public class UniLogConfigTab extends org.eclipse.swt.widgets.Composite {

	{
		SWTResourceManager.registerResourceUser(this);
	}

	final static Logger						log									= Logger.getLogger(UniLogConfigTab.class.getName());

	CLabel												receiverVoltageSymbol, receiverVoltageUnit;
	CLabel												voltageUnit;
	CLabel												currentUnit;
	CLabel												revolutionSymbol, revolutionUnit;
	CLabel												heightSymbol, heightUnit;
	Button												a23ExternModus;
	CLabel												axName, axUnit, axOffset, axFactor;
	Button												a1UniLogModus;
	Button												a23InternModus;
	CLabel												prop100WUnit, numCellLabel;
	Group													axModusGroup;
	Group													powerGroup;
	Text													prop100WInput, numCellInput;
	Text													a1Unit;
	CLabel												etaUnit;
	CLabel												etaSymbol;
	CLabel												slopeUnit;
	CLabel												slopeSymbol;
	CLabel												etaButton;
	CLabel												slopeLabel;
	Button												a3Button;
	Button												a1Button;
	Button												a2Button;
	Button												reveiverVoltageButton;
	Button												revolutionButton;
	Button												heightButton;
	CLabel												capacityLabel;
	Button												currentButton;
	Button												currentInvertButton;
	CLabel												currentOffsetLabel;
	Text													currentOffset;
	Button												voltageButton;
	Text													a1Factor, a2Factor, a3Factor;
	Text													a1Offset, a2Offset, a3Offset;
	Text													a3Unit;
	Text													a2Unit;
	CLabel												voltagePerCellUnit;
	CLabel												voltagePerCellSymbol;
	CLabel												voltageSymbol;
	CLabel												energyUnit;
	CLabel												currentSymbol;
	CLabel												energySymbol;
	CLabel												powerUnit;
	CLabel												powerSymbol;
	CLabel												capacityUnit;
	CLabel												capacitySymbol;
	CLabel												voltagePerCellLabel;
	CLabel												energyLabel;
	CLabel												powerLabel;
	Text													a3Text;
	Text													a2Text;
	Text													a1Text;
	CLabel												prop100WLabel;
	Button												setConfigButton;

	// values manipulated by editing
	boolean												isActiveUe					= false;
	boolean												isActiveU						= false;
	boolean												isActiveI						= false;
	double												offsetCurrent				= 0.0;
	boolean												isActiveRPM					= false;
	boolean												isActiveHeight			= false;
	int														prop100WValue				= 3400;
	int														numCellValue				= 12;
	String												slopeTypeSelection	= CalculationThread.REGRESSION_TYPE_CURVE;
	int														slopeTimeSelection;
	boolean												isA1ModusAvailable	= false;
	boolean												isActiveA1					= false;
	boolean												isActiveA2					= false;
	boolean												isActiveA3					= false;
	String												nameA1							= "-"; //$NON-NLS-1$
	String												nameA2							= "-"; //$NON-NLS-1$
	String												nameA3							= "-"; //$NON-NLS-1$
	String												unitA1							= "-"; //$NON-NLS-1$
	String												unitA2							= "-"; //$NON-NLS-1$
	String												unitA3							= "-"; //$NON-NLS-1$
	double												offsetA1						= 0.0;
	double												offsetA2						= 0.0;
	double												offsetA3						= 0.0;
	double												factorA1						= 1.0;
	double												factorA2						= 1.0;
	double												factorA3						= 1.0;
	String												configName;					// tabName

	CLabel												calculationTypeLabel;
	CCombo												slopeCalculationTypeCombo;
	CCombo												regressionTime;

	final UniLogDialog						dialog;
	final UniLog									device;							// get device specific things, get serial port, ...
	final OpenSerialDataExplorer	application;
	final Channels								channels;

	/**
	 * panel tab describing a configuration
	 * @param parent
	 * @param useDevice
	 * @param tabName
	 */
	public UniLogConfigTab(Composite parent, UniLog useDevice, String tabName) {
		super(parent, SWT.NONE);
		this.device = useDevice;
		this.configName = tabName;
		this.dialog = useDevice.getDialog();
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();
		initEditable();
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
					this.powerGroup = new Group(this, SWT.NONE);
					this.powerGroup.setBounds(5, 2, 299, 331);
					this.powerGroup.setLayout(null);
					this.powerGroup.setText(Messages.getString(MessageIds.OSDE_MSGT1436));
					this.powerGroup.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT1437));
					this.powerGroup.addMouseTrackListener(this.device.getDialog().mouseTrackerEnterFadeOut);
					this.powerGroup.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("powerGroup.paintControl, event=" + evt); //$NON-NLS-1$
							initEditable();
							
							MeasurementType measurement = UniLogConfigTab.this.device.getMeasurement(UniLogConfigTab.this.configName, 0); // 0=VoltageReceiver
							UniLogConfigTab.this.reveiverVoltageButton.setSelection(UniLogConfigTab.this.isActiveUe);
							UniLogConfigTab.this.reveiverVoltageButton.setText(measurement.getName());
							UniLogConfigTab.this.receiverVoltageSymbol.setText(measurement.getSymbol());
							UniLogConfigTab.this.receiverVoltageUnit.setText("[" + measurement.getUnit() + "]"); //$NON-NLS-1$ //$NON-NLS-2$

							measurement = UniLogConfigTab.this.device.getMeasurement(UniLogConfigTab.this.configName, 1); // 1=Voltage
							UniLogConfigTab.this.voltageButton.setSelection(UniLogConfigTab.this.isActiveU);
							UniLogConfigTab.this.voltageButton.setText(measurement.getName());
							UniLogConfigTab.this.voltageSymbol.setText(measurement.getSymbol());
							UniLogConfigTab.this.voltageUnit.setText("[" + measurement.getUnit() + "]"); //$NON-NLS-1$ //$NON-NLS-2$

							measurement = UniLogConfigTab.this.device.getMeasurement(UniLogConfigTab.this.configName, 2); // 2=current
							UniLogConfigTab.this.currentButton.setSelection(UniLogConfigTab.this.isActiveI);
							UniLogConfigTab.this.currentButton.setText(measurement.getName());
							UniLogConfigTab.this.currentSymbol.setText(" " + measurement.getSymbol()); //$NON-NLS-1$
							UniLogConfigTab.this.currentUnit.setText("[" + measurement.getUnit() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
							UniLogConfigTab.this.currentOffset.setText(String.format("%.3f", UniLogConfigTab.this.offsetCurrent)); //$NON-NLS-1$

							measurement = UniLogConfigTab.this.device.getMeasurement(UniLogConfigTab.this.configName, 3); // 3=charge/capacity
							UniLogConfigTab.this.capacityLabel.setText(measurement.getName());
							UniLogConfigTab.this.capacitySymbol.setText(measurement.getSymbol());
							UniLogConfigTab.this.capacityUnit.setText("[" + measurement.getUnit() + "]"); //$NON-NLS-1$ //$NON-NLS-2$

							measurement = UniLogConfigTab.this.device.getMeasurement(UniLogConfigTab.this.configName, 4); // 4=power
							UniLogConfigTab.this.powerLabel.setText(measurement.getName());
							UniLogConfigTab.this.powerSymbol.setText(measurement.getSymbol());
							UniLogConfigTab.this.powerUnit.setText("[" + measurement.getUnit() + "]"); //$NON-NLS-1$ //$NON-NLS-2$

							measurement = UniLogConfigTab.this.device.getMeasurement(UniLogConfigTab.this.configName, 5); // 5=energy
							UniLogConfigTab.this.energyLabel.setText(measurement.getName());
							UniLogConfigTab.this.energySymbol.setText(measurement.getSymbol());
							UniLogConfigTab.this.energyUnit.setText("[" + measurement.getUnit() + "]"); //$NON-NLS-1$ //$NON-NLS-2$

							// capacity
							updateStateCurrentDependent(UniLogConfigTab.this.currentButton.getSelection());

							// capacity, power, energy
							updateStateVoltageAndCurrentDependent(UniLogConfigTab.this.voltageButton.getSelection() && UniLogConfigTab.this.currentButton.getSelection());

							// number cells voltagePerCell
							measurement = UniLogConfigTab.this.device.getMeasurement(UniLogConfigTab.this.configName, 6);  // 6=voltagePerCell/cell
							UniLogConfigTab.this.voltagePerCellLabel.setText(measurement.getName());
							UniLogConfigTab.this.voltagePerCellSymbol.setText(measurement.getSymbol());
							UniLogConfigTab.this.voltagePerCellUnit.setText("[" + measurement.getUnit() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
							UniLogConfigTab.this.numCellInput.setText(" " + UniLogConfigTab.this.numCellValue); //$NON-NLS-1$

							measurement = UniLogConfigTab.this.device.getMeasurement(UniLogConfigTab.this.configName, 7); //7=revolution
							UniLogConfigTab.this.revolutionButton.setSelection(UniLogConfigTab.this.isActiveRPM);
							UniLogConfigTab.this.revolutionButton.setText(measurement.getName());
							UniLogConfigTab.this.revolutionSymbol.setText(measurement.getSymbol());
							UniLogConfigTab.this.revolutionUnit.setText("[" + measurement.getUnit() + "]"); //$NON-NLS-1$ //$NON-NLS-2$

							measurement = UniLogConfigTab.this.device.getMeasurement(UniLogConfigTab.this.configName, 8); // 8=efficience
							UniLogConfigTab.this.etaButton.setText(measurement.getName());
							UniLogConfigTab.this.etaSymbol.setText(measurement.getSymbol());
							UniLogConfigTab.this.etaUnit.setText("[" + measurement.getUnit() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
							UniLogConfigTab.this.prop100WInput.setText(" " + UniLogConfigTab.this.prop100WValue); //$NON-NLS-1$

							// n100W value, eta calculation 										
							updateStateVoltageCurrentRevolutionDependent(UniLogConfigTab.this.voltageButton.getSelection() && UniLogConfigTab.this.currentButton.getSelection()
									&& UniLogConfigTab.this.revolutionButton.getSelection());

							measurement = UniLogConfigTab.this.device.getMeasurement(UniLogConfigTab.this.configName, 9); // 9=height
							UniLogConfigTab.this.heightButton.setSelection(UniLogConfigTab.this.isActiveHeight);
							UniLogConfigTab.this.heightButton.setText(measurement.getName());
							UniLogConfigTab.this.heightSymbol.setText(measurement.getSymbol());
							UniLogConfigTab.this.heightUnit.setText("[" + measurement.getUnit() + "]"); //$NON-NLS-1$ //$NON-NLS-2$

							measurement = UniLogConfigTab.this.device.getMeasurement(UniLogConfigTab.this.configName, 10); // 10=slope
							UniLogConfigTab.this.slopeLabel.setText(measurement.getName());
							UniLogConfigTab.this.slopeSymbol.setText(measurement.getSymbol());
							UniLogConfigTab.this.slopeUnit.setText("[" + measurement.getUnit() + "]"); //$NON-NLS-1$ //$NON-NLS-2$

							updateHeightDependent(UniLogConfigTab.this.heightButton.getSelection());

							UniLogConfigTab.this.regressionTime.select(UniLogConfigTab.this.slopeTimeSelection - 1);

							UniLogConfigTab.this.slopeCalculationTypeCombo.select(UniLogConfigTab.this.slopeTypeSelection.equals(CalculationThread.REGRESSION_TYPE_CURVE) ? 1 : 0);
						}
					});
					{
						this.reveiverVoltageButton = new Button(this.powerGroup, SWT.CHECK | SWT.LEFT);
						this.reveiverVoltageButton.setBounds(23, 20, 132, 18);
						this.reveiverVoltageButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("reveiverVoltageButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								UniLogConfigTab.this.isActiveUe = UniLogConfigTab.this.reveiverVoltageButton.getSelection();
								if (UniLogConfigTab.this.channels.getActiveChannel() != null) {
									RecordSet activeRecordSet = UniLogConfigTab.this.channels.getActiveChannel().getActiveRecordSet();
									if (activeRecordSet != null) {
										// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
										activeRecordSet.get(activeRecordSet.getRecordNames()[0]).setActive(UniLogConfigTab.this.isActiveUe);
										activeRecordSet.get(activeRecordSet.getRecordNames()[0]).setDisplayable(UniLogConfigTab.this.reveiverVoltageButton.getSelection());
										UniLogConfigTab.this.application.updateGraphicsWindow();
									}
								}
								UniLogConfigTab.this.setConfigButton.setEnabled(true);
							}
						});
					}
					{
						this.receiverVoltageSymbol = new CLabel(this.powerGroup, SWT.NONE);
						this.receiverVoltageSymbol.setBounds(158, 18, 40, 20);
					}
					{
						this.receiverVoltageUnit = new CLabel(this.powerGroup, SWT.NONE);
						this.receiverVoltageUnit.setBounds(198, 18, 40, 20);
					}
					{
						this.voltageButton = new Button(this.powerGroup, SWT.CHECK | SWT.LEFT);
						this.voltageButton.setBounds(23, 42, 120, 18);
						this.voltageButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("voltageButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								UniLogConfigTab.this.isActiveU = UniLogConfigTab.this.voltageButton.getSelection();
								updateStateVoltageAndCurrentDependent(UniLogConfigTab.this.isActiveU && UniLogConfigTab.this.isActiveI);
								updateStateVoltageCurrentRevolutionDependent(UniLogConfigTab.this.voltageButton.getSelection() && UniLogConfigTab.this.currentButton.getSelection()
										&& UniLogConfigTab.this.revolutionButton.getSelection());
								if (UniLogConfigTab.this.channels.getActiveChannel() != null) {
									RecordSet activeRecordSet = UniLogConfigTab.this.channels.getActiveChannel().getActiveRecordSet();
									if (activeRecordSet != null) {
										// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
										activeRecordSet.get(activeRecordSet.getRecordNames()[1]).setActive(UniLogConfigTab.this.voltageButton.getSelection());
										activeRecordSet.get(activeRecordSet.getRecordNames()[1]).setDisplayable(UniLogConfigTab.this.voltageButton.getSelection());
										UniLogConfigTab.this.application.updateGraphicsWindow();
									}
								}
								UniLogConfigTab.this.setConfigButton.setEnabled(true);
							}
						});
					}
					{
						this.voltageSymbol = new CLabel(this.powerGroup, SWT.NONE);
						this.voltageSymbol.setBounds(158, 40, 40, 20);
					}
					{
						this.voltageUnit = new CLabel(this.powerGroup, SWT.NONE);
						this.voltageUnit.setBounds(198, 40, 40, 20);
					}
					{
						this.currentButton = new Button(this.powerGroup, SWT.CHECK | SWT.LEFT);
						this.currentButton.setBounds(23, 64, 120, 18);
						this.currentButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("currentButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								updateStateCurrentDependent(UniLogConfigTab.this.isActiveI = UniLogConfigTab.this.currentButton.getSelection());
								updateStateVoltageAndCurrentDependent(UniLogConfigTab.this.voltageButton.getSelection() && UniLogConfigTab.this.currentButton.getSelection());
								updateStateVoltageCurrentRevolutionDependent(UniLogConfigTab.this.voltageButton.getSelection() && UniLogConfigTab.this.currentButton.getSelection()
										&& UniLogConfigTab.this.revolutionButton.getSelection());
								if (UniLogConfigTab.this.channels.getActiveChannel() != null) {
									RecordSet activeRecordSet = UniLogConfigTab.this.channels.getActiveChannel().getActiveRecordSet();
									if (activeRecordSet != null) {
										// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
										activeRecordSet.get(activeRecordSet.getRecordNames()[2]).setActive(UniLogConfigTab.this.currentButton.getSelection());
										activeRecordSet.get(activeRecordSet.getRecordNames()[2]).setDisplayable(UniLogConfigTab.this.currentButton.getSelection());
										UniLogConfigTab.this.application.updateGraphicsWindow();
									}
								}
								UniLogConfigTab.this.setConfigButton.setEnabled(true);
							}
						});
					}
					{
						this.currentSymbol = new CLabel(this.powerGroup, SWT.NONE);
						this.currentSymbol.setBounds(158, 62, 30, 18);
					}
					{
						this.currentUnit = new CLabel(this.powerGroup, SWT.NONE);
						this.currentUnit.setBounds(198, 61, 20, 20);
					}
					{
						this.currentInvertButton = new Button(this.powerGroup, SWT.PUSH | SWT.CENTER);
						this.currentInvertButton.setBounds(220, 62, 25, 22);
						this.currentInvertButton.setText(Messages.getString(MessageIds.OSDE_MSGT1438));
						this.currentInvertButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT1439));
						this.currentInvertButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("currentButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								Channel activeChannel = UniLogConfigTab.this.channels.getActiveChannel();
								if (activeChannel != null && activeChannel.getActiveRecordSet() != null) {
									String[] recordKeys = activeChannel.getActiveRecordSet().getRecordNames();
									Record currentRecord = activeChannel.getActiveRecordSet().get(recordKeys[2]);
									UniLogConfigTab.this.device.invertRecordData(currentRecord);
									UniLogConfigTab.this.application.updateGraphicsWindow();
									UniLogConfigTab.this.application.updateDataTable();
									UniLogConfigTab.this.application.updateDigitalWindow();
									UniLogConfigTab.this.application.updateAnalogWindow();
								}
							}
						});
					}
					{
						this.currentOffsetLabel = new CLabel(this.powerGroup, SWT.LEFT);
						this.currentOffsetLabel.setBounds(247, 44, 46, 18);
						this.currentOffsetLabel.setFont(SWTResourceManager.getFont(this.currentOffsetLabel, SWT.NORMAL));
						this.currentOffsetLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1440));
					}
					{
						this.currentOffset = new Text(this.powerGroup, SWT.BORDER);
						this.currentOffset.setBounds(245, 63, 50, 20);
						this.currentOffset.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT1441));
						this.currentOffset.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("currentOffset.keyReleased, event=" + evt); //$NON-NLS-1$
								try {
									UniLogConfigTab.this.offsetCurrent = new Double(UniLogConfigTab.this.currentOffset.getText().trim().replace(',', '.')).doubleValue();
									if (evt.character == SWT.CR) checkUpdateAnalog();
								}
								catch (Exception e) {
									UniLogConfigTab.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0030, new Object[] {e.getClass().getSimpleName(), e.getMessage() } ));
								}
							}
						});
					}
					{
						this.capacityLabel = new CLabel(this.powerGroup, SWT.CHECK | SWT.LEFT);
						this.capacityLabel.setBounds(37, 86, 120, 20);
					}
					{
						this.capacitySymbol = new CLabel(this.powerGroup, SWT.NONE);
						this.capacitySymbol.setBounds(158, 84, 40, 20);
					}
					{
						this.capacityUnit = new CLabel(this.powerGroup, SWT.NONE);
						this.capacityUnit.setBounds(198, 84, 40, 20);
					}
					{
						this.powerLabel = new CLabel(this.powerGroup, SWT.CHECK | SWT.LEFT);
						this.powerLabel.setBounds(37, 108, 120, 20);
					}
					{
						this.powerSymbol = new CLabel(this.powerGroup, SWT.NONE);
						this.powerSymbol.setBounds(158, 106, 40, 20);
					}
					{
						this.powerUnit = new CLabel(this.powerGroup, SWT.NONE);
						this.powerUnit.setBounds(198, 106, 40, 20);
					}
					{
						this.energyLabel = new CLabel(this.powerGroup, SWT.CHECK | SWT.LEFT);
						this.energyLabel.setBounds(37, 130, 120, 20);
					}
					{
						this.energySymbol = new CLabel(this.powerGroup, SWT.NONE);
						this.energySymbol.setBounds(158, 128, 40, 20);
					}
					{
						this.energyUnit = new CLabel(this.powerGroup, SWT.NONE);
						this.energyUnit.setBounds(198, 128, 40, 20);
					}
					{
						this.voltagePerCellLabel = new CLabel(this.powerGroup, SWT.CHECK | SWT.LEFT);
						this.voltagePerCellLabel.setBounds(37, 152, 120, 20);
					}
					{
						this.voltagePerCellSymbol = new CLabel(this.powerGroup, SWT.NONE);
						this.voltagePerCellSymbol.setBounds(158, 150, 40, 20);
					}
					{
						this.voltagePerCellUnit = new CLabel(this.powerGroup, SWT.NONE);
						this.voltagePerCellUnit.setBounds(198, 150, 40, 20);
					}
					{
						this.numCellLabel = new CLabel(this.powerGroup, SWT.LEFT);
						this.numCellLabel.setBounds(37, 172, 118, 18);
						this.numCellLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1442));
					}
					{
						this.numCellInput = new Text(this.powerGroup, SWT.LEFT | SWT.BORDER);
						this.numCellInput.setBounds(158, 173, 40, 20);
						this.numCellInput.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT1443));
						this.numCellInput.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("numCellInput.keyReleased, event=" + evt); //$NON-NLS-1$
								try {
									if (evt.character == SWT.CR) {
										UniLogConfigTab.this.setConfigButton.setEnabled(true);
										UniLogConfigTab.this.numCellValue = new Integer(UniLogConfigTab.this.numCellInput.getText().trim());
										UniLogConfigTab.this.numCellInput.setText(" " + UniLogConfigTab.this.numCellValue); //$NON-NLS-1$
										// update propeller n100W value too, if user has changed, but not hit enter 
										UniLogConfigTab.this.prop100WValue = new Integer(UniLogConfigTab.this.prop100WInput.getText().trim());
										UniLogConfigTab.this.prop100WInput.setText(" " + UniLogConfigTab.this.prop100WValue); //$NON-NLS-1$
										if (UniLogConfigTab.this.channels.getActiveChannel() != null) {
											RecordSet recordSet = UniLogConfigTab.this.channels.getActiveChannel().getActiveRecordSet();
											if (recordSet != null) {
												Record record = recordSet.get(recordSet.getRecordNames()[6]);
												PropertyType property = record.getProperty(UniLog.NUMBER_CELLS);
												if (property != null) {
													property.setValue(UniLogConfigTab.this.numCellValue);
												}
												else {
													record.createProperty(UniLog.NUMBER_CELLS, DataTypes.INTEGER, UniLogConfigTab.this.numCellValue);
												}
												// update propeller n100W value too, if user has changed, but not hit enter 
												record = recordSet.get(recordSet.getRecordNames()[8]);
												property = record.getProperty(UniLog.PROP_N_100_WATT);
												if (property != null) {
													property.setValue(UniLogConfigTab.this.prop100WValue);
												}
												else {
													record.createProperty(UniLog.PROP_N_100_WATT, DataTypes.INTEGER, UniLogConfigTab.this.prop100WValue);
												}

												recordSet.setRecalculationRequired();
												UniLogConfigTab.this.device.makeInActiveDisplayable(recordSet);
												UniLogConfigTab.this.application.updateGraphicsWindow();
												UniLogConfigTab.this.application.updateDataTable();
												recordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
											}
										}
									}
								}
								catch (Exception e) {
									UniLogConfigTab.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0030, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
								}
							}
						});
					}
					{
						this.revolutionButton = new Button(this.powerGroup, SWT.CHECK | SWT.LEFT);
						this.revolutionButton.setBounds(23, 196, 135, 18);
						this.revolutionButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("revolutionButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								UniLogConfigTab.this.isActiveRPM = UniLogConfigTab.this.revolutionButton.getSelection();
								updateStateVoltageCurrentRevolutionDependent(UniLogConfigTab.this.isActiveU && UniLogConfigTab.this.isActiveI	&& UniLogConfigTab.this.isActiveRPM);
								if (UniLogConfigTab.this.channels.getActiveChannel() != null) {
									RecordSet activeRecordSet = UniLogConfigTab.this.channels.getActiveChannel().getActiveRecordSet();
									if (activeRecordSet != null) {
										// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
										activeRecordSet.get(activeRecordSet.getRecordNames()[7]).setActive(UniLogConfigTab.this.revolutionButton.getSelection());
										activeRecordSet.get(activeRecordSet.getRecordNames()[7]).setDisplayable(UniLogConfigTab.this.revolutionButton.getSelection());
										UniLogConfigTab.this.application.updateGraphicsWindow();
									}
								}
								UniLogConfigTab.this.setConfigButton.setEnabled(true);
							}
						});
					}
					{
						this.revolutionSymbol = new CLabel(this.powerGroup, SWT.NONE);
						this.revolutionSymbol.setBounds(158, 194, 40, 20);
					}
					{
						this.revolutionUnit = new CLabel(this.powerGroup, SWT.NONE);
						this.revolutionUnit.setBounds(198, 194, 50, 20);
					}
					{
						this.prop100WLabel = new CLabel(this.powerGroup, SWT.LEFT);
						this.prop100WLabel.setBounds(37, 216, 118, 18);
						this.prop100WLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1444));
					}
					{
						this.prop100WInput = new Text(this.powerGroup, SWT.LEFT | SWT.BORDER);
						this.prop100WInput.setBounds(158, 217, 40, 20);
						this.prop100WInput.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT1445));
						this.prop100WInput.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("prop100WInput.keyReleased, event=" + evt); //$NON-NLS-1$
								try {
									if (evt.character == SWT.CR) {
										UniLogConfigTab.this.setConfigButton.setEnabled(true);
										UniLogConfigTab.this.prop100WValue = new Integer(UniLogConfigTab.this.prop100WInput.getText().trim());
										UniLogConfigTab.this.prop100WInput.setText(" " + UniLogConfigTab.this.prop100WValue); //$NON-NLS-1$
										// update number cells too, if user has changed, but not hit enter 
										UniLogConfigTab.this.numCellValue = new Integer(UniLogConfigTab.this.numCellInput.getText().trim());
										UniLogConfigTab.this.numCellInput.setText(" " + UniLogConfigTab.this.numCellValue); //$NON-NLS-1$
										if (UniLogConfigTab.this.channels.getActiveChannel() != null) {
											RecordSet recordSet = UniLogConfigTab.this.channels.getActiveChannel().getActiveRecordSet();
											if (recordSet != null) {
												Record record = recordSet.get(recordSet.getRecordNames()[8]);
												PropertyType property = record.getProperty(UniLog.PROP_N_100_WATT);
												if (property != null) {
													property.setValue(UniLogConfigTab.this.prop100WValue);
												}
												else {
													record.createProperty(UniLog.PROP_N_100_WATT, DataTypes.INTEGER, UniLogConfigTab.this.prop100WValue);
												}
												// update number cells too, if user has changed, but not hit enter 
												record = recordSet.get(recordSet.getRecordNames()[6]);
												property = record.getProperty(UniLog.NUMBER_CELLS);
												if (property != null) {
													property.setValue(UniLogConfigTab.this.numCellValue);
												}
												else {
													record.createProperty(UniLog.NUMBER_CELLS, DataTypes.INTEGER, UniLogConfigTab.this.numCellValue);
												}
												
												recordSet.setRecalculationRequired();
												UniLogConfigTab.this.device.makeInActiveDisplayable(recordSet);
												UniLogConfigTab.this.application.updateGraphicsWindow();
												UniLogConfigTab.this.application.updateDataTable();
												recordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
											}
										}
									}
								}
								catch (Exception e) {
									UniLogConfigTab.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0030, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
								}
							}
						});
					}
					{
						this.prop100WUnit = new CLabel(this.powerGroup, SWT.NONE);
						this.prop100WUnit.setBounds(198, 216, 88, 20);
						this.prop100WUnit.setText(Messages.getString(MessageIds.OSDE_MSGT1446));
					}
					{
						this.etaButton = new CLabel(this.powerGroup, SWT.CHECK | SWT.LEFT);
						this.etaButton.setBounds(37, 240, 108, 20);
					}
					{
						this.etaSymbol = new CLabel(this.powerGroup, SWT.NONE);
						this.etaSymbol.setBounds(158, 239, 40, 20);
					}
					{
						this.etaUnit = new CLabel(this.powerGroup, SWT.NONE);
						this.etaUnit.setBounds(198, 238, 40, 20);
					}
					{
						this.heightButton = new Button(this.powerGroup, SWT.CHECK | SWT.LEFT);
						this.heightButton.setBounds(23, 262, 120, 18);
						this.heightButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("heightButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								updateHeightDependent(UniLogConfigTab.this.isActiveHeight = UniLogConfigTab.this.heightButton.getSelection());
								if (UniLogConfigTab.this.channels.getActiveChannel() != null) {
									RecordSet activeRecordSet = UniLogConfigTab.this.channels.getActiveChannel().getActiveRecordSet();
									if (activeRecordSet != null) {
										// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
										activeRecordSet.get(activeRecordSet.getRecordNames()[9]).setActive(UniLogConfigTab.this.heightButton.getSelection());
										activeRecordSet.get(activeRecordSet.getRecordNames()[9]).setDisplayable(UniLogConfigTab.this.heightButton.getSelection());
										UniLogConfigTab.this.application.updateGraphicsWindow();
									}
								}
								UniLogConfigTab.this.setConfigButton.setEnabled(true);
							}
						});
					}
					{
						this.heightSymbol = new CLabel(this.powerGroup, SWT.NONE);
						this.heightSymbol.setBounds(158, 260, 40, 20);
					}
					{
						this.heightUnit = new CLabel(this.powerGroup, SWT.NONE);
						this.heightUnit.setBounds(198, 260, 40, 20);
					}
					{
						this.slopeLabel = new CLabel(this.powerGroup, SWT.CHECK | SWT.LEFT);
						this.slopeLabel.setBounds(37, 282, 120, 19);
					}
					{
						this.slopeSymbol = new CLabel(this.powerGroup, SWT.NONE);
						this.slopeSymbol.setBounds(158, 282, 40, 20);
					}
					{
						this.slopeUnit = new CLabel(this.powerGroup, SWT.NONE);
						this.slopeUnit.setBounds(198, 282, 40, 20);
					}
					{
						this.calculationTypeLabel = new CLabel(this.powerGroup, SWT.NONE);
						this.calculationTypeLabel.setBounds(48, 304, 79, 20);
						this.calculationTypeLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1447));
					}
					{
						this.slopeCalculationTypeCombo = new CCombo(this.powerGroup, SWT.BORDER);
						this.slopeCalculationTypeCombo.setBounds(133, 304, 97, 20);
						this.slopeCalculationTypeCombo.setItems(new String[] { " " + Messages.getString(MessageIds.OSDE_MSGT1479), " " + Messages.getString(MessageIds.OSDE_MSGT1480) });
						this.slopeCalculationTypeCombo.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT1448));
						this.slopeCalculationTypeCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("slopeCalculationTypeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								if (UniLogConfigTab.this.slopeCalculationTypeCombo.getSelectionIndex() == 1)
									UniLogConfigTab.this.slopeTypeSelection = CalculationThread.REGRESSION_TYPE_CURVE;
								else
									UniLogConfigTab.this.slopeTypeSelection = CalculationThread.REGRESSION_TYPE_LINEAR;

								RecordSet recordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
								if (recordSet != null) {
									Record record = recordSet.get(recordSet.getRecordNames()[10]);
									PropertyType property = record.getProperty(CalculationThread.REGRESSION_TYPE);
									if (property != null) {
										property.setValue(UniLogConfigTab.this.slopeTypeSelection);
									}
									else {
										record.createProperty(CalculationThread.REGRESSION_TYPE, DataTypes.STRING, UniLogConfigTab.this.slopeTypeSelection);
									}
									recordSet.setRecalculationRequired();
									UniLogConfigTab.this.device.makeInActiveDisplayable(recordSet);
									UniLogConfigTab.this.application.updateDataTable();
									recordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
								}
								UniLogConfigTab.this.setConfigButton.setEnabled(true);
							}
						});
					}
					{
						this.regressionTime = new CCombo(this.powerGroup, SWT.BORDER);
						this.regressionTime.setBounds(232, 304, 61, 20);
						this.regressionTime.setItems(new String[] { " 1 s", " 2 s", " 3 s", " 4 s", " 5 s", " 6 s", " 7 s", " 8 s", " 9 s", "10 s", "11 s", "12 s", "13 s", "14 s", "15 s", "16 s", "17 s", "18 s", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$ //$NON-NLS-17$ //$NON-NLS-18$
								"19 s", "20 s", "21 s", "22 s", "23 s", "24 s", "25 s", "26 s", "27 s", "28 s", "29 s", "30 s" }); //$NON-NLS-1$ //$NON-NLS-2$
						this.regressionTime.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT1449));
						this.regressionTime.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("regressionTime.widgetSelected, event=" + evt); //$NON-NLS-1$
								UniLogConfigTab.this.slopeTimeSelection = UniLogConfigTab.this.regressionTime.getSelectionIndex() + 1;
								RecordSet recordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
								if (recordSet != null) {
									Record record = recordSet.get(recordSet.getRecordNames()[10]);
									PropertyType property = record.getProperty(CalculationThread.REGRESSION_INTERVAL_SEC);
									if (property != null) {
										property.setValue(UniLogConfigTab.this.slopeTimeSelection);
									}
									else {
										record.createProperty(CalculationThread.REGRESSION_INTERVAL_SEC, DataTypes.INTEGER, UniLogConfigTab.this.slopeTimeSelection);
									}	
									recordSet.setRecalculationRequired();
									UniLogConfigTab.this.device.makeInActiveDisplayable(recordSet);
									UniLogConfigTab.this.application.updateDataTable();
									recordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
								}
								UniLogConfigTab.this.setConfigButton.setEnabled(true);
							}
						});
					}
				}
				{
					this.axModusGroup = new Group(this, SWT.NONE);
					this.axModusGroup.setLayout(null);
					this.axModusGroup.setText(Messages.getString(MessageIds.OSDE_MSGT1450));
					this.axModusGroup.setBounds(313, 2, 310, 193);
					this.axModusGroup.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT1451));
					this.axModusGroup.addMouseTrackListener(this.device.getDialog().mouseTrackerEnterFadeOut);
					this.axModusGroup.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("axModusGroup.paintControl, event=" + evt); //$NON-NLS-1$
							UniLogConfigTab.this.a1Button.setSelection(UniLogConfigTab.this.isActiveA1);
							UniLogConfigTab.this.a1Text.setText(UniLogConfigTab.this.nameA1);
							UniLogConfigTab.this.a1Unit.setText("[" + UniLogConfigTab.this.unitA1 + "]"); //$NON-NLS-1$ //$NON-NLS-2$
							UniLogConfigTab.this.a1Offset.setText(String.format("%.3f", UniLogConfigTab.this.offsetA1)); //$NON-NLS-1$
							UniLogConfigTab.this.a1Factor.setText(String.format("%.3f", UniLogConfigTab.this.factorA1)); //$NON-NLS-1$

							UniLogConfigTab.this.a2Button.setSelection(UniLogConfigTab.this.isActiveA2);
							UniLogConfigTab.this.a2Text.setText(UniLogConfigTab.this.nameA2);
							UniLogConfigTab.this.a2Unit.setText("[" + UniLogConfigTab.this.unitA2 + "]"); //$NON-NLS-1$ //$NON-NLS-2$
							UniLogConfigTab.this.a2Offset.setText(String.format("%.3f", UniLogConfigTab.this.offsetA2)); //$NON-NLS-1$
							UniLogConfigTab.this.a2Factor.setText(String.format("%.3f", UniLogConfigTab.this.factorA2)); //$NON-NLS-1$

							UniLogConfigTab.this.a3Button.setSelection(UniLogConfigTab.this.isActiveA3);
							UniLogConfigTab.this.a3Text.setText(UniLogConfigTab.this.nameA3);
							UniLogConfigTab.this.a3Unit.setText("[" + UniLogConfigTab.this.unitA3 + "]"); //$NON-NLS-1$ //$NON-NLS-2$
							UniLogConfigTab.this.a3Offset.setText(String.format("%.3f", UniLogConfigTab.this.offsetA3)); //$NON-NLS-1$
							UniLogConfigTab.this.a3Factor.setText(String.format("%.3f", UniLogConfigTab.this.factorA3)); //$NON-NLS-1$
						}
					});
					{
						this.a1UniLogModus = new Button(this.axModusGroup, SWT.PUSH | SWT.CENTER);
						this.a1UniLogModus.setBounds(7, 20, 290, 25);
						this.a1UniLogModus.setText(Messages.getString(MessageIds.OSDE_MSGT1452));
						this.a1UniLogModus.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a1UniLogModus.widgetSelected, event=" + evt); //$NON-NLS-1$
								try {
									if (!UniLogConfigTab.this.isA1ModusAvailable) {
										UniLogConfigTab.this.dialog.updateConfigurationValues(UniLogConfigTab.this.device.getSerialPort().readConfiguration());
									}
									UniLogConfigTab.this.a1Text.setText(UniLogDialog.A1_MODUS[UniLogConfigTab.this.dialog.getSelectionIndexA1ModusCombo()]);
									checkUpdateAnalog();
								}
								catch (Exception e) {
									UniLogConfigTab.this.application.openMessageDialog(e.getMessage());
								}
							}
						});
					}
					{
						this.axName = new CLabel(this.axModusGroup, SWT.LEFT);
						this.axName.setBounds(40, 50, 116, 18);
						this.axName.setText(Messages.getString(MessageIds.OSDE_MSGT1453));
						this.axName.setFont(SWTResourceManager.getFont(this.axName, SWT.NORMAL));
					}
					{
						this.axUnit = new CLabel(this.axModusGroup, SWT.LEFT);
						this.axUnit.setBounds(158, 50, 50, 18);
						this.axUnit.setText(Messages.getString(MessageIds.OSDE_MSGT1454));
						this.axUnit.setFont(SWTResourceManager.getFont(this.axName, SWT.NORMAL));
					}
					{
						this.axOffset = new CLabel(this.axModusGroup, SWT.LEFT);
						this.axOffset.setBounds(209, 50, 46, 20);
						this.axOffset.setText(Messages.getString(MessageIds.OSDE_MSGT1455));
						this.axOffset.setFont(SWTResourceManager.getFont(this.axName, SWT.NORMAL));
					}
					{
						this.axFactor = new CLabel(this.axModusGroup, SWT.LEFT);
						this.axFactor.setBounds(257, 50, 48, 20);
						this.axFactor.setText(Messages.getString(MessageIds.OSDE_MSGT1456));
						this.axFactor.setFont(SWTResourceManager.getFont(this.axName, SWT.NORMAL));
					}
					{
						this.a1Button = new Button(this.axModusGroup, SWT.CHECK | SWT.LEFT);
						this.a1Button.setBounds(4, 72, 36, 20);
						this.a1Button.setText(Messages.getString(MessageIds.OSDE_MSGT1457));
						this.a1Button.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a1ValueButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								UniLogConfigTab.this.isActiveA1 = UniLogConfigTab.this.a1Button.getSelection();
								checkUpdateAnalog();
							}
						});
					}
					{
						this.a1Text = new Text(this.axModusGroup, SWT.BORDER);
						this.a1Text.setBounds(42, 72, 116, 20);
						this.a1Text.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT1458));
						this.a1Text.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a1Text.keyReleased, event=" + evt); //$NON-NLS-1$
								UniLogConfigTab.this.nameA1 = UniLogConfigTab.this.a1Text.getText().trim();
								if (evt.character == SWT.CR) checkUpdateAnalog();
							}
						});
					}
					{
						this.a1Unit = new Text(this.axModusGroup, SWT.CENTER | SWT.BORDER);
						this.a1Unit.setBounds(160, 72, 45, 20);
						this.a1Unit.setFont(SWTResourceManager.getFont(this.axName, SWT.NORMAL));
						this.a1Unit.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a1Unit.keyReleased, event=" + evt); //$NON-NLS-1$
								UniLogConfigTab.this.unitA1 = UniLogConfigTab.this.a1Unit.getText().replace('[', ' ').replace(']', ' ').trim();
								if (evt.character == SWT.CR) checkUpdateAnalog();
							}
						});
					}
					{
						this.a1Offset = new Text(this.axModusGroup, SWT.BORDER);
						this.a1Offset.setBounds(207, 72, 48, 20);
						this.a1Offset.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a1Offset.keyReleased, event=" + evt); //$NON-NLS-1$
								try {
									UniLogConfigTab.this.offsetA1 = new Double(UniLogConfigTab.this.a1Offset.getText().trim().replace(',', '.'));
									if (evt.character == SWT.CR) checkUpdateAnalog();
								}
								catch (Exception e) {
									UniLogConfigTab.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0030, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
								}
							}
						});
					}
					{
						this.a1Factor = new Text(this.axModusGroup, SWT.BORDER);
						this.a1Factor.setBounds(257, 72, 48, 20);
						this.a1Factor.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a1Factor.keyReleased, event=" + evt); //$NON-NLS-1$
								try {
									UniLogConfigTab.this.factorA1 = new Double(UniLogConfigTab.this.a1Factor.getText().trim().replace(',', '.'));
									if (evt.character == SWT.CR) checkUpdateAnalog();
								}
								catch (Exception e) {
									UniLogConfigTab.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0030, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
								}
							}
						});
					}
					{
						this.a2Button = new Button(this.axModusGroup, SWT.CHECK | SWT.LEFT);
						this.a2Button.setBounds(4, 93, 36, 20);
						this.a2Button.setText(Messages.getString(MessageIds.OSDE_MSGT1459));
						this.a2Button.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a2ValueButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								UniLogConfigTab.this.isActiveA2 = UniLogConfigTab.this.a2Button.getSelection();
								checkUpdateAnalog();
							}
						});
					}
					{
						this.a2Text = new Text(this.axModusGroup, SWT.BORDER);
						this.a2Text.setBounds(42, 93, 116, 20);
						this.a2Text.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT1460));
						this.a2Text.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a2Text.keyReleased, event=" + evt); //$NON-NLS-1$
								UniLogConfigTab.this.nameA2 = UniLogConfigTab.this.a2Text.getText().trim();
								if (evt.character == SWT.CR) checkUpdateAnalog();
							}
						});
					}
					{
						this.a2Unit = new Text(this.axModusGroup, SWT.CENTER | SWT.BORDER);
						this.a2Unit.setBounds(160, 93, 45, 20);
						this.a2Unit.setFont(SWTResourceManager.getFont(this.axName, SWT.NORMAL));
						this.a2Unit.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a2Unit.keyReleased, event=" + evt); //$NON-NLS-1$
								UniLogConfigTab.this.unitA2 = UniLogConfigTab.this.a2Unit.getText().replace('[', ' ').replace(']', ' ').trim();
								if (evt.character == SWT.CR) checkUpdateAnalog();
							}
						});
					}
					{
						this.a2Offset = new Text(this.axModusGroup, SWT.BORDER);
						this.a2Offset.setBounds(207, 93, 48, 20);
						this.a2Offset.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a2Offset.keyReleased, event=" + evt); //$NON-NLS-1$
								try {
									UniLogConfigTab.this.offsetA2 = new Double(UniLogConfigTab.this.a2Offset.getText().trim().replace(',', '.'));
									if (evt.character == SWT.CR) checkUpdateAnalog();
								}
								catch (Exception e) {
									UniLogConfigTab.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0030, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
								}
							}
						});
					}
					{
						this.a2Factor = new Text(this.axModusGroup, SWT.BORDER);
						this.a2Factor.setBounds(257, 93, 48, 20);
						this.a2Factor.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a2Factor.keyReleased, event=" + evt); //$NON-NLS-1$
								try {
									UniLogConfigTab.this.factorA2 = new Double(UniLogConfigTab.this.a2Factor.getText().trim().replace(',', '.'));
									if (evt.character == SWT.CR) checkUpdateAnalog();
								}
								catch (Exception e) {
									UniLogConfigTab.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0030, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
								}
							}
						});
					}
					{
						this.a3Button = new Button(this.axModusGroup, SWT.CHECK | SWT.LEFT);
						this.a3Button.setBounds(4, 115, 36, 20);
						this.a3Button.setText(Messages.getString(MessageIds.OSDE_MSGT1461));
						this.a3Button.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a3ValueButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								UniLogConfigTab.this.isActiveA3 = UniLogConfigTab.this.a3Button.getSelection();
								checkUpdateAnalog();
							}
						});
					}
					{
						this.a3Text = new Text(this.axModusGroup, SWT.BORDER);
						this.a3Text.setBounds(42, 115, 116, 20);
						this.a3Text.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT1462));
						this.a3Text.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a3Text.keyReleased, event=" + evt); //$NON-NLS-1$
								UniLogConfigTab.this.nameA3 = UniLogConfigTab.this.a3Text.getText().trim();
								if (evt.character == SWT.CR) checkUpdateAnalog();
							}
						});
					}
					{
						this.a3Unit = new Text(this.axModusGroup, SWT.CENTER | SWT.BORDER);
						this.a3Unit.setBounds(160, 115, 45, 20);
						this.a3Unit.setFont(SWTResourceManager.getFont(this.axName, SWT.NORMAL));
						this.a3Unit.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a3Unit.keyReleased, event=" + evt); //$NON-NLS-1$
								UniLogConfigTab.this.unitA3 = UniLogConfigTab.this.a3Unit.getText().replace('[', ' ').replace(']', ' ').trim();
								if (evt.character == SWT.CR) checkUpdateAnalog();
							}
						});
					}
					{
						this.a3Offset = new Text(this.axModusGroup, SWT.BORDER);
						this.a3Offset.setBounds(207, 115, 48, 20);
						this.a3Offset.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a3Offset.keyReleased, event=" + evt); //$NON-NLS-1$
								try {
									UniLogConfigTab.this.offsetA3 = new Double(UniLogConfigTab.this.a3Offset.getText().trim().replace(',', '.'));
									if (evt.character == SWT.CR) checkUpdateAnalog();
								}
								catch (Exception e) {
									UniLogConfigTab.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0030, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
								}
							}
						});
					}
					{
						this.a3Factor = new Text(this.axModusGroup, SWT.BORDER);
						this.a3Factor.setBounds(257, 115, 48, 20);
						this.a3Factor.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a3Factor.keyReleased, event=" + evt); //$NON-NLS-1$
								try {
									UniLogConfigTab.this.factorA3 = new Double(UniLogConfigTab.this.a3Factor.getText().trim().replace(',', '.'));
									if (evt.character == SWT.CR) checkUpdateAnalog();
								}
								catch (Exception e) {
									UniLogConfigTab.this.application.openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0030, new Object[] { e.getClass().getSimpleName(), e.getMessage() } ));
								}
							}
						});
					}
					{
						this.a23InternModus = new Button(this.axModusGroup, SWT.PUSH | SWT.CENTER);
						this.a23InternModus.setBounds(7, 153, 146, 25);
						this.a23InternModus.setText(Messages.getString(MessageIds.OSDE_MSGT1463));
						this.a23InternModus.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a23InternModus.widgetSelected, event=" + evt); //$NON-NLS-1$
								setA23Defaults('I');
								checkUpdateAnalog();
							}
						});
					}
					{
						this.a23ExternModus = new Button(this.axModusGroup, SWT.PUSH | SWT.CENTER);
						this.a23ExternModus.setBounds(159, 153, 139, 26);
						this.a23ExternModus.setText(Messages.getString(MessageIds.OSDE_MSGT1464));
						this.a23ExternModus.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("a23ExternModus.widgetSelected, event=" + evt); //$NON-NLS-1$
								setA23Defaults('E');
								checkUpdateAnalog();
							}
						});
					}
				}
				{
					this.setConfigButton = new Button(this, SWT.PUSH | SWT.CENTER);
					this.setConfigButton.setBounds(320, 250, 295, 30);
					this.setConfigButton.setText(Messages.getString(MessageIds.OSDE_MSGT1465));
					this.setConfigButton.setEnabled(false);
					this.setConfigButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							if (UniLogConfigTab.log.isLoggable(Level.FINEST)) UniLogConfigTab.log.finest("setConfigButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							collectAndUpdateConfiguration();
							UniLogConfigTab.this.setConfigButton.setEnabled(false);
						}
					});
				}
			}
			this.layout();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * enable voltage, current, revolution dependent measurement fields
	 * @param enabled
	 */
	void updateStateVoltageCurrentRevolutionDependent(boolean enabled) {
		if (this.channels.getActiveChannel() != null) {
			RecordSet activeRecordSet = this.channels.getActiveChannel().getActiveRecordSet();
			if (activeRecordSet != null) {
				// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
				activeRecordSet.get(activeRecordSet.getRecordNames()[8]).setDisplayable(enabled);
				activeRecordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
			}
		}
		this.prop100WLabel.setEnabled(enabled);
		this.prop100WInput.setEnabled(enabled);
		this.prop100WUnit.setEnabled(enabled);
		this.etaButton.setEnabled(enabled);
		this.etaSymbol.setEnabled(enabled);
		this.etaUnit.setEnabled(enabled);
		if (enabled) {
			this.prop100WLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.prop100WInput.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.prop100WUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.etaButton.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.etaSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.etaUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		}
		else {
			this.prop100WLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.prop100WInput.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.prop100WUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.etaButton.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.etaSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.etaUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
		}
	}

	/**
	 * enable height measurement dependent fields
	 * @param enabled
	 */
	void updateHeightDependent(boolean enabled) {
		if (this.channels.getActiveChannel() != null) {
			RecordSet activeRecordSet = this.channels.getActiveChannel().getActiveRecordSet();
			if (activeRecordSet != null) {
				// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
				activeRecordSet.get(activeRecordSet.getRecordNames()[10]).setDisplayable(enabled);
				activeRecordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
			}
		}
		this.slopeLabel.setEnabled(enabled);
		this.slopeSymbol.setEnabled(enabled);
		this.slopeUnit.setEnabled(enabled);
		if (enabled) {
			this.slopeLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.slopeSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.slopeUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.calculationTypeLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.slopeCalculationTypeCombo.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.regressionTime.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		}
		else {
			this.slopeLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.slopeSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.slopeUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.calculationTypeLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.slopeCalculationTypeCombo.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.regressionTime.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
		}
	}

	/**
	 * load default values for A2 and A3 fields
	 * @param internExtern 'I' intern / 'E' external sensor
	 */
	void setA23Defaults(int internExtern) {
		String[] a2Values;
		String[] a3Values;
		switch (internExtern) {
		case 'E': // extern
			a2Values = new String[] { Messages.getString(MessageIds.OSDE_MSGT1466), Messages.getString(MessageIds.OSDE_MSGT1467), Messages.getString(MessageIds.OSDE_MSGT1468), "0.0", "1.0" }; //$NON-NLS-1$ //$NON-NLS-2$
			a3Values = new String[] { Messages.getString(MessageIds.OSDE_MSGT1469), Messages.getString(MessageIds.OSDE_MSGT1470), Messages.getString(MessageIds.OSDE_MSGT1471), "0.0", "1.0" }; //$NON-NLS-1$ //$NON-NLS-2$
			break;
		case 'I': // intern
		default:
			a2Values = new String[] { Messages.getString(MessageIds.OSDE_MSGT1472), Messages.getString(MessageIds.OSDE_MSGT1473), Messages.getString(MessageIds.OSDE_MSGT1474), "0.0", "1.0" }; //$NON-NLS-1$ //$NON-NLS-2$
			a3Values = new String[] { Messages.getString(MessageIds.OSDE_MSGT1475), Messages.getString(MessageIds.OSDE_MSGT1476), Messages.getString(MessageIds.OSDE_MSGT1477), "0.0", "1.0" }; //$NON-NLS-1$ //$NON-NLS-2$
			break;
		}
		this.a2Text.setText(this.nameA2 = a2Values[0]);
		this.a2Unit.setText(this.unitA2 = a2Values[2]);
		this.a2Offset.setText(""+(this.offsetA2 = new Double(a2Values[3]))); //$NON-NLS-1$
		this.a2Factor.setText(""+(this.factorA2 = new Double(a2Values[4]))); //$NON-NLS-1$

		this.a3Text.setText(this.nameA3 = a3Values[0]);
		this.a3Unit.setText(this.unitA3 = a3Values[2]);
		this.a3Offset.setText(""+(this.offsetA3 = new Double(a3Values[3]))); //$NON-NLS-1$
		this.a3Factor.setText(""+(this.factorA3 = new Double(a3Values[4]))); //$NON-NLS-1$
	}

	/**
	 * enable or disable current (and time) dependent measurement fields
	 * @param enabled true | false
	 */
	void updateStateCurrentDependent(boolean enabled) {
		if (this.channels.getActiveChannel() != null) {
			RecordSet activeRecordSet = this.channels.getActiveChannel().getActiveRecordSet();
			if (activeRecordSet != null) {
				// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
				activeRecordSet.get(activeRecordSet.getRecordNames()[3]).setDisplayable(enabled);
				activeRecordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
			}
		}
		this.capacityLabel.setEnabled(enabled);
		this.capacitySymbol.setEnabled(enabled);
		this.capacityUnit.setEnabled(enabled);
		if (enabled) {
			this.capacityLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.capacitySymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.capacityUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		}
		else {
			this.capacityLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.capacitySymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.capacityUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
		}
	}
	
	/**
	 * enable or disable voltage and current dependent measurement fields
	 * @param enabled true | false
	 */
	void updateStateVoltageAndCurrentDependent(boolean enabled) {
		if (this.channels.getActiveChannel() != null) {
			RecordSet activeRecordSet = this.channels.getActiveChannel().getActiveRecordSet();
			if (activeRecordSet != null) {
				// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
				activeRecordSet.get(activeRecordSet.getRecordNames()[4]).setDisplayable(enabled);
				activeRecordSet.get(activeRecordSet.getRecordNames()[5]).setDisplayable(enabled);
				activeRecordSet.get(activeRecordSet.getRecordNames()[6]).setDisplayable(enabled);
				activeRecordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
			}
		}
		this.powerLabel.setEnabled(enabled);
		this.powerUnit.setEnabled(enabled);
		this.powerSymbol.setEnabled(enabled);
		this.energyLabel.setEnabled(enabled);
		this.energyUnit.setEnabled(enabled);
		this.energySymbol.setEnabled(enabled);
		this.voltagePerCellLabel.setEnabled(enabled);
		this.voltagePerCellUnit.setEnabled(enabled);
		this.voltagePerCellSymbol.setEnabled(enabled);
		this.numCellLabel.setEnabled(enabled);
		this.numCellInput.setEnabled(enabled);
		if (enabled) {
			this.powerLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.powerUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.powerSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.energyLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.energyUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.energySymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.voltagePerCellLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.voltagePerCellUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.voltagePerCellSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.numCellLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
			this.numCellInput.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		}
		else {
			this.powerLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.powerUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.powerSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.energyLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.energyUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.energySymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.voltagePerCellLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.voltagePerCellUnit.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.voltagePerCellSymbol.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.numCellLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
			this.numCellInput.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_INACTIVE_FOREGROUND));
		}
	}

	/**
	 * collect all configuration relevant data and update device configuration
	 */
	void collectAndUpdateConfiguration() {
		MeasurementType measurement = this.device.getMeasurement(this.configName, 0); // 0=voltageReceiver
		measurement.setActive(this.reveiverVoltageButton.getSelection());

		measurement = this.device.getMeasurement(this.configName, 1); // 1=voltage
		measurement.setActive(this.voltageButton.getSelection());

		measurement = this.device.getMeasurement(this.configName, 2); // 2=current
		measurement.setActive(this.currentButton.getSelection());
		measurement.setOffset(new Double(this.currentOffset.getText().replace(',', '.').trim()));

		this.device.setMeasurementPropertyValue(this.configName, 6, UniLog.NUMBER_CELLS, DataTypes.INTEGER, this.numCellValue);

		measurement = this.device.getMeasurement(this.configName, 7); //7=revolution
		measurement.setActive(this.revolutionButton.getSelection());

		this.device.setMeasurementPropertyValue(this.configName, 8, UniLog.PROP_N_100_WATT, DataTypes.INTEGER, this.prop100WValue);// 8=efficiency

		measurement = this.device.getMeasurement(this.configName, 9); // 9=height
		measurement.setActive(this.heightButton.getSelection());

		// 10=slope
		this.device.setMeasurementPropertyValue(this.configName, 10, CalculationThread.REGRESSION_TYPE, DataTypes.STRING, this.slopeTypeSelection);
		this.device.setMeasurementPropertyValue(this.configName, 10, CalculationThread.REGRESSION_INTERVAL_SEC, DataTypes.INTEGER, this.slopeTimeSelection);
		
		measurement = this.device.getMeasurement(this.configName, 11); // 11=A1
		measurement.setActive(this.a1Button.getSelection());
		measurement.setName(this.a1Text.getText().trim());
		measurement.setUnit(this.a1Unit.getText().replace('[', ' ').replace(']', ' ').trim());
		measurement.setOffset(new Double(this.a1Offset.getText().trim().replace(',', '.').trim()));
		measurement.setFactor(new Double(this.a1Factor.getText().trim().replace(',', '.').trim()));

		measurement = this.device.getMeasurement(this.configName, 12); // 12=A2
		measurement.setActive(this.a2Button.getSelection());
		measurement.setName(this.a2Text.getText().trim());
		measurement.setUnit(this.a2Unit.getText().replace('[', ' ').replace(']', ' ').trim());
		measurement.setOffset(new Double(this.a2Offset.getText().trim().replace(',', '.').trim()));
		measurement.setFactor(new Double(this.a2Factor.getText().trim().replace(',', '.').trim()));

		measurement = this.device.getMeasurement(this.configName, 13); // 13=A3
		measurement.setActive(this.a3Button.getSelection());
		measurement.setName(this.a3Text.getText().trim());
		measurement.setUnit(this.a3Unit.getText().replace('[', ' ').replace(']', ' ').trim());
		measurement.setOffset(new Double(this.a3Offset.getText().trim().replace(',', '.').trim()));
		measurement.setFactor(new Double(this.a3Factor.getText().trim().replace(',', '.').trim()));

		this.device.setChangePropery(true);
		this.device.storeDeviceProperties();
	}

	/**
	 * @param enable the isA1ModusAvailable to set
	 */
	public void setA1ModusAvailable(boolean enable) {
		this.isA1ModusAvailable = enable;
	}

	/**
	 * @return set configuration button status, true(enabled) if configuration has been changed
	 */
	public boolean getConfigButtonStatus() {
		return this.setConfigButton.getEnabled();
	}

	/**
	 * @param newConfigName the configName to set
	 */
	public void setConfigName(String newConfigName) {
		this.configName = newConfigName;
	}

	/**
	 * retrieve initial values from device properties file for editable fields
	 */
	void initEditable() {
		MeasurementType measurement;
		PropertyType property = null;
		Record record = null;
		RecordSet recordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
		if (recordSet != null) { // load all data from record set
			String[] recordKeys = recordSet.getRecordNames();
			
			this.isActiveUe = recordSet.get(recordKeys[0]).isActive();
			this.isActiveU = recordSet.get(recordKeys[1]).isActive();
			this.isActiveI = recordSet.get(recordKeys[2]).isActive();
			
			this.offsetCurrent = recordSet.get(recordKeys[2]).getOffset();
			
			property = recordSet.get(recordKeys[6]).getProperty(UniLog.NUMBER_CELLS);
			this.numCellValue = property != null ? new Integer(property.getValue()) : 4;
			
			this.isActiveRPM = recordSet.get(recordKeys[7]).isActive();
			
			property = recordSet.get(recordKeys[8]).getProperty(UniLog.PROP_N_100_WATT);
			this.prop100WValue = property != null ? new Integer(property.getValue()) : 10000;
			
			this.isActiveHeight = recordSet.get(recordKeys[9]).isActive();

			property = recordSet.get(recordKeys[10]).getProperty(CalculationThread.REGRESSION_TYPE);
			this.slopeTypeSelection = property != null ? property.getValue() : CalculationThread.REGRESSION_TYPE_CURVE;
			property = recordSet.get(recordKeys[10]).getProperty(CalculationThread.REGRESSION_INTERVAL_SEC);
			this.slopeTimeSelection = property != null ? new Integer(property.getValue()) : 10;

			record = recordSet.get(recordKeys[11]);
			this.isActiveA1 = record.isActive();
			this.nameA1 = record.getName();
			this.unitA1 = record.getUnit();
			this.offsetA1 = record.getOffset();
			this.factorA1 = record.getFactor();

			record = recordSet.get(recordKeys[12]);
			this.isActiveA2 = record.isActive();
			this.nameA2 = record.getName();
			this.unitA2 = record.getUnit();
			this.offsetA2 = record.getOffset();
			this.factorA2 = record.getFactor();

			record = recordSet.get(recordKeys[13]);
			this.isActiveA3 = record.isActive();
			this.nameA3 = record.getName();
			this.unitA3 = record.getUnit();
			this.offsetA3 = record.getOffset();
			this.factorA3 = record.getFactor();
		}
		else { // no active record, load data from device properties
			measurement = this.device.getMeasurement(this.configName, 0);
			this.isActiveUe = measurement.isActive();

			measurement = this.device.getMeasurement(this.configName, 1);
			this.isActiveU = measurement.isActive();

			measurement = this.device.getMeasurement(this.configName, 2);
			this.isActiveI = measurement.isActive();
			this.offsetCurrent = measurement.getOffset();

			property = this.device.getMeasruementProperty(this.configName, 6, UniLog.NUMBER_CELLS);
			this.numCellValue = property != null ? new Integer(property.getValue()) : 4;

			measurement = this.device.getMeasurement(this.configName, 7);
			this.isActiveRPM = measurement.isActive();

			property = this.device.getMeasruementProperty(this.configName, 8, UniLog.PROP_N_100_WATT);
			this.prop100WValue = property != null ? new Integer(property.getValue()) : 10000;

			measurement = this.device.getMeasurement(this.configName, 9);
			this.isActiveHeight = measurement.isActive();

			property = this.device.getMeasruementProperty(this.configName, 10, CalculationThread.REGRESSION_TYPE);
			this.slopeTypeSelection = property != null ? property.getValue() : CalculationThread.REGRESSION_TYPE_CURVE;
			property = this.device.getMeasruementProperty(this.configName, 10, CalculationThread.REGRESSION_INTERVAL_SEC);
			this.slopeTimeSelection = property != null ? new Integer(property.getValue()) : 10;

			if (this.nameA1.equals(OSDE.STRING_DASH)) {
				measurement = this.device.getMeasurement(this.configName, 11);
				this.isActiveA1 = measurement.isActive();
				this.nameA1 = measurement.getName();
				this.unitA1 = measurement.getUnit();
				this.offsetA1 = this.device.getMeasurementOffset(this.configName, 11);
				this.factorA1 = this.device.getMeasurementFactor(this.configName, 11);
			}
			if (this.nameA2.equals(OSDE.STRING_DASH)) {
				measurement = this.device.getMeasurement(this.configName, 12);
				this.isActiveA2 = measurement.isActive();
				this.nameA2 = measurement.getName();
				this.unitA2 = measurement.getUnit();
				this.offsetA2 = this.device.getMeasurementOffset(this.configName, 12);
				this.factorA2 = this.device.getMeasurementFactor(this.configName, 12);
			}
			if (this.nameA3.equals(OSDE.STRING_DASH)) {
				measurement = this.device.getMeasurement(this.configName, 13);
				this.isActiveA3 = measurement.isActive();
				this.nameA3 = measurement.getName();
				this.unitA3 = measurement.getUnit();
				this.offsetA3 = this.device.getMeasurementOffset(this.configName, 13);
				this.factorA3 = this.device.getMeasurementFactor(this.configName, 13);
			}
		}
	}

	/**
	 * updates the analog record descriptors according input fields
	 * attention: set new record name replaces the record, setName() must the last operation in sequence
	 */
	void checkUpdateAnalog() {
		if (this.channels.getActiveChannel() != null) {
			RecordSet activeRecordSet = this.channels.getActiveChannel().getActiveRecordSet();
			if (activeRecordSet != null) {
				// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
				// 2=current
				activeRecordSet.get(activeRecordSet.getRecordNames()[2]).setOffset(new Double(this.currentOffset.getText().trim().replace(',', '.')));

				// 11=a1Value
				activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setActive(this.a1Button.getSelection());
				//activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setVisible(this.a1Button.getSelection());
				activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setDisplayable(this.a1Button.getSelection());
				activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setName(this.a1Text.getText().trim());
				activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setUnit(this.a1Unit.getText().replace('[', ' ').replace(']', ' ').trim());
				activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setOffset(new Double(this.a1Offset.getText().trim().replace(',', '.')));
				activeRecordSet.get(activeRecordSet.getRecordNames()[11]).setFactor(new Double(this.a1Factor.getText().trim().replace(',', '.')));
				// 12=a2Value
				activeRecordSet.get(activeRecordSet.getRecordNames()[12]).setActive(this.a2Button.getSelection());
				//activeRecordSet.get(activeRecordSet.getRecordNames()[12]).setVisible(this.a2Button.getSelection());
				activeRecordSet.get(activeRecordSet.getRecordNames()[12]).setDisplayable(this.a2Button.getSelection());
				activeRecordSet.get(activeRecordSet.getRecordNames()[12]).setName(this.a2Text.getText().trim());
				activeRecordSet.get(activeRecordSet.getRecordNames()[12]).setUnit(this.a2Unit.getText().replace('[', ' ').replace(']', ' ').trim());
				activeRecordSet.get(activeRecordSet.getRecordNames()[12]).setOffset(new Double(this.a2Offset.getText().trim().replace(',', '.')));
				activeRecordSet.get(activeRecordSet.getRecordNames()[12]).setFactor(new Double(this.a2Factor.getText().trim().replace(',', '.')));
				// 13=a3Value
				activeRecordSet.get(activeRecordSet.getRecordNames()[13]).setActive(this.a3Button.getSelection());
				//activeRecordSet.get(activeRecordSet.getRecordNames()[13]).setVisible(this.a3Button.getSelection());
				activeRecordSet.get(activeRecordSet.getRecordNames()[13]).setDisplayable(this.a3Button.getSelection());
				activeRecordSet.get(activeRecordSet.getRecordNames()[13]).setName(this.a3Text.getText().trim());
				activeRecordSet.get(activeRecordSet.getRecordNames()[13]).setUnit(this.a3Unit.getText().replace('[', ' ').replace(']', ' ').trim());
				activeRecordSet.get(activeRecordSet.getRecordNames()[13]).setOffset(new Double(this.a3Offset.getText().trim().replace(',', '.')));
				activeRecordSet.get(activeRecordSet.getRecordNames()[13]).setFactor(new Double(this.a3Factor.getText().trim().replace(',', '.')));
				activeRecordSet.get(activeRecordSet.getRecordNames()[13]).setName(this.a3Text.getText().trim());
				
				UniLogConfigTab.this.application.updateGraphicsWindow();
				activeRecordSet.setUnsaved(RecordSet.UNSAVED_REASON_CONFIGURATION);
			}
		}
		this.setConfigButton.setEnabled(true);
		this.powerGroup.redraw();
		this.axModusGroup.redraw();
	}
}
