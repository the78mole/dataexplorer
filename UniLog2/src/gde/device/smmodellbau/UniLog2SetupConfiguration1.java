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
    
    Copyright (c) 2011 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import gde.GDE;
import gde.device.smmodellbau.unilog2.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

/**
 * class to implement SM UniLog2 basic configuration panel 1
 */
public class UniLog2SetupConfiguration1 extends org.eclipse.swt.widgets.Composite {
	final static Logger							log										= Logger.getLogger(UniLog2SetupConfiguration1.class.getName());

	final UniLog2Dialog							dialog;
	final DataExplorer							application;
	final UniLog2SetupReaderWriter	configuration;

	Group														commonAdjustmentsGroup, logStartStopGroup;
	CLabel													serialNumberLabel, firmwareLabel, dataRateLabel, currentSensorTypeLabel, propellerBladesLabel, motorPolsLabel, gearFactorLabel, varioTriggerLevelLabel,
			varioToneLabel;
	CLabel													limiterModusLabel, energyLimitLabel, autoStartCurrentUnitLabel, autoStartRxUnitLabel, autoStartTimeUnitLabel, a1ModusLabel, a2ModusLabel, a3ModusLabel, minMaxRxLabel,
			autoStopLabel;
	Text														serialNumberText, firmwareText, gearFactorText, varioTriggerLevelText, energyLimitText;
	Slider													gearFactorSlider, varioTriggerLevelSlider, energyLimitSlider;
	CCombo													dataRateCombo, currentSensorCombo, propBladesCombo, varioToneCombo, limiterModusCombo, minMaxRxCombo, autoStopCombo, autoStartCurrentCombo, autoStartRxCombo,
			autoStartTimeCombo;
	CCombo													a1ModusCombo, a2ModusCombo, a3ModusCombo;
	Button													autoStartCurrentButton, autoStartRxButton, autoStartTimeButton;

	final String[]									dataRateValues				= { " 50 Hz", " 20 Hz", " 10 Hz", "  5 Hz", "  2 Hz", "  1 Hz" };
	final String[]									currentSensorTypes		= { "  20 A", " 40/80 A", " 150 A", " 400 A" };
	final String[]									analogModi						= Messages.getString(MessageIds.GDE_MSGT2549).split(GDE.STRING_COMMA);
	final String[]									numberProbMotorPoles	= { " 1 / 2", " 2 / 4", " 3 / 6", " 4 / 8", " 5 / 10", " 6 / 12", " 7 / 14" };
	final String[]									varioThresholds				= { " 0.0", " 0.1", " 0.2", " 0.3", " 0.4", " 0.5", " 0.6", " 0.7", " 0.8", " 0.9", " 1.0", " 1.1", " 1.2", " 1.3", " 1.4", " 1.5", " 1.6",
			" 1.7", " 1.8", " 1.9", " 2.0", " 2.1", " 2.2", " 2.3", " 2.4", " 2.5", " 2.6", " 2.7", " 2.8", " 2.9", " 3.0", " 3.1", " 3.2", " 3.3", " 3.4", " 3.5", " 3.6", " 3.7", " 3.8", " 3.9", " 4.0",
			" 4.1", " 4.2", " 4.3", " 4.4", " 4.5", " 4.6", " 4.7", " 4.8", " 4.9", " 5.0" };
	final String[]									currentStartValues		= { "  1", "  2", "  3", "  4", "  5", "  6", "  7", "  8", "  9", " 10" };
	final String[]									rxStartValues					= { "  1.1", "  1.2", "  1.3", "  1.4", "  1.5", "  1.6", "  1.7", "  1.8", "  1.9", Messages.getString(MessageIds.GDE_MSGT2508) };
	final String[]									timeStartValues				= { "  5", " 10", " 15", " 20", " 25", " 30", " 35", " 40", " 45", " 50", " 55", " 60", " 65", " 70", " 75", " 80", " 85", " 90" };
	final String[]									voltageRxValues				= { "  3.00", "  3.25", "  3.50", "  3.75", "  4.00", "  4.25", "  4.50", "  4.75", "  4.80", "  4.85", "  4.90", "  4.95", "  5.00", "  5.05",
			"  5.10", "  5.15", "  5.20", "  5.25", "  5.50", "  6.00", "  6.25", "  6.50", "  6.75", "  7.00", "  7.25", "  7.50", "  7.75", "  8.00" };

	/**
	* Auto-generated main method to display this 
	* org.eclipse.swt.widgets.Composite inside a new Shell.
	*/
	public static void main(String[] args) {
		showGUI();
	}

	/**
	* Auto-generated method to display this 
	* org.eclipse.swt.widgets.Composite inside a new Shell.
	*/
	public static void showGUI() {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		UniLog2SetupConfiguration1 inst = new UniLog2SetupConfiguration1(shell, SWT.NULL);
		Point size = inst.getSize();
		shell.setLayout(new FillLayout());
		shell.layout();
		if (size.x == 0 && size.y == 0) {
			inst.pack();
			shell.pack();
		}
		else {
			Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
			shell.setSize(shellBounds.width, shellBounds.height);
		}
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
	}

	/**
	* Constructor matching the auto-generated method to display this 
	* org.eclipse.swt.widgets.Composite inside a new Shell.
	*/
	public UniLog2SetupConfiguration1(Composite parent, int style) {
		super(parent, style);
		this.dialog = null;
		this.configuration = null;
		this.application = null;
		initGUI();
	}

	/**
	 * constructor configuration panel 1
	 * @param parent
	 * @param style
	 * @param dialog
	 * @param configuration
	 */
	public UniLog2SetupConfiguration1(Composite parent, int style, UniLog2Dialog useDialog, UniLog2SetupReaderWriter useConfiguration) {
		super(parent, style);
		SWTResourceManager.registerResourceUser(this);
		this.dialog = useDialog;
		this.configuration = useConfiguration;
		this.application = DataExplorer.getInstance();
		initGUI();
		updateValues();
	}

	/**
	 * update values according to red configuration values
	 */
	public void updateValues() {
		this.serialNumberText.setText(GDE.STRING_EMPTY + this.configuration.serialNumber);
		this.firmwareText.setText(String.format(" %.2f", this.configuration.firmwareVersion / 100.0)); //$NON-NLS-1$

		this.dataRateCombo.select(this.configuration.dataRate);
		this.currentSensorCombo.select(this.configuration.currentSensorType);
		this.a1ModusCombo.select(this.configuration.modusA1);
		this.a2ModusCombo.select(this.configuration.modusA2);
		this.a3ModusCombo.select(this.configuration.modusA3);
		this.propBladesCombo.select(this.configuration.numberProb_MotorPole - 1);
		this.gearFactorSlider.setSelection(this.configuration.gearFactor - 100);
		this.gearFactorText.setText(String.format(Locale.ENGLISH, " %.2f", this.configuration.gearFactor / 100.0)); //$NON-NLS-1$
		this.varioTriggerLevelSlider.setSelection(this.configuration.varioThreshold);
		this.varioTriggerLevelText.setText(String.format(Locale.ENGLISH, " %.1f", this.configuration.varioThreshold / 10.0)); //$NON-NLS-1$
		this.varioToneCombo.select(this.configuration.varioTon);
		this.limiterModusCombo.select(this.configuration.limiterModus);
		this.energyLimitSlider.setSelection(this.configuration.energyLimit);
		this.energyLimitText.setText(GDE.STRING_BLANK + this.configuration.energyLimit);
		this.minMaxRxCombo.select(this.configuration.minMaxRx);

		this.autoStartCurrentButton.setSelection((this.configuration.startModus & 0x0001) > 0);
		this.autoStartRxButton.setSelection((this.configuration.startModus & 0x0002) > 0);
		this.autoStartTimeButton.setSelection((this.configuration.startModus & 0x0004) > 0);
		this.autoStartCurrentCombo.select(this.configuration.startCurrent - 1);
		this.autoStartRxCombo.select(this.configuration.startRx - 11);
		this.autoStartTimeCombo.select(this.configuration.startTime / 5 - 1);
		this.autoStopCombo.select(this.configuration.stopModus);
	}

	void initGUI() {
		SWTResourceManager.registerResourceUser(this);
		try {
			this.setLayout(new FormLayout());
			//this.setSize(320, 467); // required for form editor
			{
				this.logStartStopGroup = new Group(this, SWT.NONE);
				RowLayout logStartStopGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.logStartStopGroup.setLayout(logStartStopGroupLayout);
				FormData logStartStopGroupLData = new FormData();
				logStartStopGroupLData.width = 290;
				logStartStopGroupLData.height = 96;
				logStartStopGroupLData.left = new FormAttachment(0, 1000, 12);
				logStartStopGroupLData.top = new FormAttachment(0, 1000, 340);
				this.logStartStopGroup.setLayoutData(logStartStopGroupLData);
				this.logStartStopGroup.setText(Messages.getString(MessageIds.GDE_MSGT2526));
				{
					this.autoStartCurrentButton = new Button(this.logStartStopGroup, SWT.CHECK);
					RowData startByCurrentButtonLData = new RowData();
					startByCurrentButtonLData.width = 135;
					startByCurrentButtonLData.height = 19;
					this.autoStartCurrentButton.setLayoutData(startByCurrentButtonLData);
					this.autoStartCurrentButton.setText(Messages.getString(MessageIds.GDE_MSGT2527));
					this.autoStartCurrentButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "autoStartCurrentButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration1.this.autoStartCurrentButton.getSelection()) {
								UniLog2SetupConfiguration1.this.configuration.startModus = (short) (UniLog2SetupConfiguration1.this.configuration.startModus | UniLog2SetupReaderWriter.AUTO_START_CURRENT);
							}
							else {
								UniLog2SetupConfiguration1.this.configuration.startModus = (short) (UniLog2SetupConfiguration1.this.configuration.startModus ^ UniLog2SetupReaderWriter.AUTO_START_CURRENT);
							}
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.autoStartCurrentCombo = new CCombo(this.logStartStopGroup, SWT.BORDER);
					RowData currentTriggerComboLData = new RowData();
					currentTriggerComboLData.width = 84;
					currentTriggerComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.autoStartCurrentCombo.setLayoutData(currentTriggerComboLData);
					this.autoStartCurrentCombo.setItems(this.currentStartValues);
					this.autoStartCurrentCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "autoStartCurrentCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.startCurrent = (short) (UniLog2SetupConfiguration1.this.autoStartCurrentCombo.getSelectionIndex() + 1);
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.autoStartCurrentUnitLabel = new CLabel(this.logStartStopGroup, SWT.NONE);
					RowData currentTriggerUnitLabelLData = new RowData();
					currentTriggerUnitLabelLData.width = 49;
					currentTriggerUnitLabelLData.height = 20;
					this.autoStartCurrentUnitLabel.setLayoutData(currentTriggerUnitLabelLData);
					this.autoStartCurrentUnitLabel.setText("[A]"); //$NON-NLS-1$
				}
				{
					this.autoStartRxButton = new Button(this.logStartStopGroup, SWT.CHECK | SWT.LEFT);
					RowData rxTriggerButtonLData = new RowData();
					rxTriggerButtonLData.width = 135;
					rxTriggerButtonLData.height = 16;
					this.autoStartRxButton.setLayoutData(rxTriggerButtonLData);
					this.autoStartRxButton.setText(Messages.getString(MessageIds.GDE_MSGT2528));
					this.autoStartRxButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "autoStartRxButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration1.this.autoStartRxButton.getSelection()) {
								UniLog2SetupConfiguration1.this.configuration.startModus = (short) (UniLog2SetupConfiguration1.this.configuration.startModus | UniLog2SetupReaderWriter.AUTO_START_RX);
							}
							else {
								UniLog2SetupConfiguration1.this.configuration.startModus = (short) (UniLog2SetupConfiguration1.this.configuration.startModus ^ UniLog2SetupReaderWriter.AUTO_START_RX);
							}
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.autoStartRxCombo = new CCombo(this.logStartStopGroup, SWT.BORDER);
					RowData rxTriggerComboLData = new RowData();
					rxTriggerComboLData.width = 84;
					rxTriggerComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.autoStartRxCombo.setLayoutData(rxTriggerComboLData);
					this.autoStartRxCombo.setItems(this.rxStartValues);
					this.autoStartRxCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "autoStartRxCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.startRx = (short) (UniLog2SetupConfiguration1.this.autoStartRxCombo.getSelectionIndex() + 11);
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.autoStartRxUnitLabel = new CLabel(this.logStartStopGroup, SWT.NONE);
					RowData rxTriggerUnitLabelLData = new RowData();
					rxTriggerUnitLabelLData.width = 49;
					rxTriggerUnitLabelLData.height = 20;
					this.autoStartRxUnitLabel.setLayoutData(rxTriggerUnitLabelLData);
					this.autoStartRxUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2529));
				}
				{
					this.autoStartTimeButton = new Button(this.logStartStopGroup, SWT.CHECK | SWT.LEFT);
					RowData timeTriggerButtonLData = new RowData();
					timeTriggerButtonLData.width = 135;
					timeTriggerButtonLData.height = 16;
					this.autoStartTimeButton.setLayoutData(timeTriggerButtonLData);
					this.autoStartTimeButton.setText(Messages.getString(MessageIds.GDE_MSGT2530));
					this.autoStartTimeButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "autoStartTimeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration1.this.autoStartTimeButton.getSelection()) {
								UniLog2SetupConfiguration1.this.configuration.startModus = (short) (UniLog2SetupConfiguration1.this.configuration.startModus | UniLog2SetupReaderWriter.AUTO_START_TIME);
							}
							else {
								UniLog2SetupConfiguration1.this.configuration.startModus = (short) (UniLog2SetupConfiguration1.this.configuration.startModus ^ UniLog2SetupReaderWriter.AUTO_START_TIME);
							}
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.autoStartTimeCombo = new CCombo(this.logStartStopGroup, SWT.BORDER);
					RowData timeTriggerComboLData = new RowData();
					timeTriggerComboLData.width = 84;
					timeTriggerComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.autoStartTimeCombo.setLayoutData(timeTriggerComboLData);
					this.autoStartTimeCombo.setItems(this.timeStartValues);
					this.autoStartTimeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "autoStartTimeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.startTime = (short) ((UniLog2SetupConfiguration1.this.autoStartTimeCombo.getSelectionIndex() + 1) * 5);
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.autoStartTimeUnitLabel = new CLabel(this.logStartStopGroup, SWT.NONE);
					RowData timeTriggerUnitLabelLData = new RowData();
					timeTriggerUnitLabelLData.width = 49;
					timeTriggerUnitLabelLData.height = 20;
					this.autoStartTimeUnitLabel.setLayoutData(timeTriggerUnitLabelLData);
					this.autoStartTimeUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2531));
				}
				{
					this.autoStopLabel = new CLabel(this.logStartStopGroup, SWT.NONE);
					RowData autoStopLabelLData = new RowData();
					autoStopLabelLData.width = 135;
					autoStopLabelLData.height = 20;
					this.autoStopLabel.setLayoutData(autoStopLabelLData);
					this.autoStopLabel.setText(Messages.getString(MessageIds.GDE_MSGT2532));
				}
				{
					this.autoStopCombo = new CCombo(this.logStartStopGroup, SWT.BORDER);
					this.autoStopCombo.setItems(Messages.getString(MessageIds.GDE_MSGT2519).split(GDE.STRING_COMMA));
					RowData autoStopComboLData = new RowData();
					autoStopComboLData.width = 84;
					autoStopComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.autoStopCombo.setLayoutData(autoStopComboLData);
					this.autoStopCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "autoStopCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.stopModus = (short) (UniLog2SetupConfiguration1.this.autoStopCombo.getSelectionIndex());
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
			}
			{
				this.commonAdjustmentsGroup = new Group(this, SWT.NONE);
				RowLayout commonAdjustmentsGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.commonAdjustmentsGroup.setLayout(commonAdjustmentsGroupLayout);
				FormData commonAdjustmentsGroupLData = new FormData();
				commonAdjustmentsGroupLData.width = 290;
				commonAdjustmentsGroupLData.height = 307;
				commonAdjustmentsGroupLData.left = new FormAttachment(0, 1000, 12);
				commonAdjustmentsGroupLData.top = new FormAttachment(0, 1000, 10);
				this.commonAdjustmentsGroup.setLayoutData(commonAdjustmentsGroupLData);
				this.commonAdjustmentsGroup.setText(Messages.getString(MessageIds.GDE_MSGT2533));
				{
					this.serialNumberLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData serialNumberLabelLData = new RowData();
					serialNumberLabelLData.width = 99;
					serialNumberLabelLData.height = 20;
					this.serialNumberLabel.setLayoutData(serialNumberLabelLData);
					this.serialNumberLabel.setText(Messages.getString(MessageIds.GDE_MSGT2534));
				}
				{
					this.serialNumberText = new Text(this.commonAdjustmentsGroup, SWT.RIGHT | SWT.BORDER);
					RowData serialNumberTextLData = new RowData();
					serialNumberTextLData.width = 49;
					serialNumberTextLData.height = GDE.IS_MAC ? 16 : 13;
					this.serialNumberText.setLayoutData(serialNumberTextLData);
					this.serialNumberText.setEditable(false);
				}
				{
					this.firmwareLabel = new CLabel(this.commonAdjustmentsGroup, SWT.RIGHT);
					RowData firmwareLabelLData = new RowData();
					firmwareLabelLData.width = 73;
					firmwareLabelLData.height = 20;
					this.firmwareLabel.setLayoutData(firmwareLabelLData);
					this.firmwareLabel.setText(Messages.getString(MessageIds.GDE_MSGT2535));
				}
				{
					this.firmwareText = new Text(this.commonAdjustmentsGroup, SWT.BORDER | SWT.RIGHT);
					RowData firmwareTextLData = new RowData();
					firmwareTextLData.width = 39;
					firmwareTextLData.height = GDE.IS_MAC ? 16 : 13;
					this.firmwareText.setLayoutData(firmwareTextLData);
					this.firmwareText.setEditable(false);
				}
				{
					this.dataRateLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData dataRateLabelLData = new RowData();
					dataRateLabelLData.width = 135;
					dataRateLabelLData.height = 20;
					this.dataRateLabel.setLayoutData(dataRateLabelLData);
					this.dataRateLabel.setText(Messages.getString(MessageIds.GDE_MSGT2536));
				}
				{
					this.dataRateCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					this.dataRateCombo.setItems(this.dataRateValues);
					RowData dataRateComboLData = new RowData();
					dataRateComboLData.width = 84;
					dataRateComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.dataRateCombo.setLayoutData(dataRateComboLData);
					this.dataRateCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "dataRateCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.dataRate = (short) UniLog2SetupConfiguration1.this.dataRateCombo.getSelectionIndex();
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.currentSensorTypeLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData currentSensorTypeLabelLData = new RowData();
					currentSensorTypeLabelLData.width = 135;
					currentSensorTypeLabelLData.height = 20;
					this.currentSensorTypeLabel.setLayoutData(currentSensorTypeLabelLData);
					this.currentSensorTypeLabel.setText(Messages.getString(MessageIds.GDE_MSGT2537));
				}
				{
					this.currentSensorCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					RowData currentSensorComboLData = new RowData();
					currentSensorComboLData.width = 84;
					currentSensorComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.currentSensorCombo.setLayoutData(currentSensorComboLData);
					this.currentSensorCombo.setItems(this.currentSensorTypes);
					this.currentSensorCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "currentSensorCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.currentSensorType = (short) UniLog2SetupConfiguration1.this.currentSensorCombo.getSelectionIndex();
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.a1ModusLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData a1ModusLabelLData = new RowData();
					a1ModusLabelLData.width = 135;
					a1ModusLabelLData.height = 20;
					this.a1ModusLabel.setLayoutData(a1ModusLabelLData);
					this.a1ModusLabel.setText(Messages.getString(MessageIds.GDE_MSGT2538));
				}
				{
					this.a1ModusCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					this.a1ModusCombo.setItems(this.analogModi);
					RowData a1ModusComboLData = new RowData();
					a1ModusComboLData.width = 105;
					a1ModusComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.a1ModusCombo.setLayoutData(a1ModusComboLData);
					this.a1ModusCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "a1ModusCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.modusA1 = (short) UniLog2SetupConfiguration1.this.a1ModusCombo.getSelectionIndex();
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.a2ModusLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData a2ModusLabelLData = new RowData();
					a2ModusLabelLData.width = 135;
					a2ModusLabelLData.height = 20;
					this.a2ModusLabel.setLayoutData(a2ModusLabelLData);
					this.a2ModusLabel.setText(Messages.getString(MessageIds.GDE_MSGT2539));
				}
				{
					this.a2ModusCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					this.a2ModusCombo.setItems(this.analogModi);
					RowData a2ModusComboLData = new RowData();
					a2ModusComboLData.width = 105;
					a2ModusComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.a2ModusCombo.setLayoutData(a2ModusComboLData);
					this.a2ModusCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "a2ModusCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.modusA2 = (short) UniLog2SetupConfiguration1.this.a2ModusCombo.getSelectionIndex();
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.a3ModusLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData a3ModusLabelLData = new RowData();
					a3ModusLabelLData.width = 135;
					a3ModusLabelLData.height = 20;
					this.a3ModusLabel.setLayoutData(a3ModusLabelLData);
					this.a3ModusLabel.setText(Messages.getString(MessageIds.GDE_MSGT2540));
				}
				{
					this.a3ModusCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					this.a3ModusCombo.setItems(this.analogModi);
					RowData a3ModusComboLData = new RowData();
					a3ModusComboLData.width = 105;
					a3ModusComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.a3ModusCombo.setLayoutData(a3ModusComboLData);
					this.a3ModusCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "a3ModusCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.modusA3 = (short) UniLog2SetupConfiguration1.this.a3ModusCombo.getSelectionIndex();
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.propellerBladesLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData propellerBladesLabelLData = new RowData();
					propellerBladesLabelLData.width = 135;
					propellerBladesLabelLData.height = 20;
					this.propellerBladesLabel.setLayoutData(propellerBladesLabelLData);
					this.propellerBladesLabel.setText(Messages.getString(MessageIds.GDE_MSGT2541));
				}
				{
					this.propBladesCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					RowData propBladesComboLData = new RowData();
					propBladesComboLData.width = 60;
					propBladesComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.propBladesCombo.setLayoutData(propBladesComboLData);
					this.propBladesCombo.setItems(this.numberProbMotorPoles);
					this.propBladesCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "propBladesCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.numberProb_MotorPole = (short) (UniLog2SetupConfiguration1.this.propBladesCombo.getSelectionIndex() + 1);
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.motorPolsLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData motorPolsLabelLData = new RowData();
					motorPolsLabelLData.width = 76;
					motorPolsLabelLData.height = 20;
					this.motorPolsLabel.setLayoutData(motorPolsLabelLData);
					this.motorPolsLabel.setText(Messages.getString(MessageIds.GDE_MSGT2542));
				}
				{
					this.gearFactorLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData gearFactorLabelLData = new RowData();
					gearFactorLabelLData.width = 135;
					gearFactorLabelLData.height = 20;
					this.gearFactorLabel.setLayoutData(gearFactorLabelLData);
					this.gearFactorLabel.setText(Messages.getString(MessageIds.GDE_MSGT2543));
				}
				{
					this.gearFactorText = new Text(this.commonAdjustmentsGroup, SWT.CENTER | SWT.BORDER);
					RowData gearFactorTextLData = new RowData();
					gearFactorTextLData.width = 35;
					gearFactorTextLData.height = GDE.IS_MAC ? 16 : 13;
					this.gearFactorText.setLayoutData(gearFactorTextLData);
				}
				{
					RowData gearFactorSliderLData = new RowData();
					gearFactorSliderLData.width = 101;
					gearFactorSliderLData.height = 18;
					this.gearFactorSlider = new Slider(this.commonAdjustmentsGroup, SWT.NONE);
					this.gearFactorSlider.setLayoutData(gearFactorSliderLData);
					this.gearFactorSlider.setMinimum(0);
					this.gearFactorSlider.setMaximum(910);
					this.gearFactorSlider.setIncrement(1);
					this.gearFactorSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "gearFactorSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.gearFactor = (short) (UniLog2SetupConfiguration1.this.gearFactorSlider.getSelection() + 100);
							UniLog2SetupConfiguration1.this.gearFactorText.setText(String.format(Locale.ENGLISH, " %.2f", (UniLog2SetupConfiguration1.this.configuration.gearFactor / 100.0))); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioTriggerLevelLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData varioTriggerLevelLabelLData = new RowData();
					varioTriggerLevelLabelLData.width = 135;
					varioTriggerLevelLabelLData.height = 20;
					this.varioTriggerLevelLabel.setLayoutData(varioTriggerLevelLabelLData);
					this.varioTriggerLevelLabel.setText(Messages.getString(MessageIds.GDE_MSGT2544));
				}
				{
					this.varioTriggerLevelText = new Text(this.commonAdjustmentsGroup, SWT.CENTER | SWT.BORDER);
					RowData varioTriggerLevelTextLData = new RowData();
					varioTriggerLevelTextLData.width = 35;
					varioTriggerLevelTextLData.height = GDE.IS_MAC ? 16 : 13;
					this.varioTriggerLevelText.setLayoutData(varioTriggerLevelTextLData);
				}
				{
					RowData varioTriggerLevelSliderLData = new RowData();
					varioTriggerLevelSliderLData.width = 101;
					varioTriggerLevelSliderLData.height = 18;
					this.varioTriggerLevelSlider = new Slider(this.commonAdjustmentsGroup, SWT.NONE);
					this.varioTriggerLevelSlider.setLayoutData(varioTriggerLevelSliderLData);
					this.varioTriggerLevelSlider.setMinimum(0);
					this.varioTriggerLevelSlider.setMaximum(60);
					this.varioTriggerLevelSlider.setIncrement(1);
					this.varioTriggerLevelSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "gearFactorSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.varioThreshold = (short) UniLog2SetupConfiguration1.this.varioTriggerLevelSlider.getSelection();
							UniLog2SetupConfiguration1.this.varioTriggerLevelText.setText(UniLog2SetupConfiguration1.this.varioThresholds[UniLog2SetupConfiguration1.this.configuration.varioThreshold]);
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioToneLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData varioToneLabelLData = new RowData();
					varioToneLabelLData.width = 135;
					varioToneLabelLData.height = 20;
					this.varioToneLabel.setLayoutData(varioToneLabelLData);
					this.varioToneLabel.setText(Messages.getString(MessageIds.GDE_MSGT2545));
				}
				{
					this.varioToneCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER | SWT.CENTER);
					RowData varioToneComboLData = new RowData();
					varioToneComboLData.width = 84;
					varioToneComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.varioToneCombo.setLayoutData(varioToneComboLData);
					this.varioToneCombo.setItems(Messages.getString(MessageIds.GDE_MSGT2522).split(GDE.STRING_COMMA));
					this.varioToneCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "varioToneCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.varioTon = (short) (UniLog2SetupConfiguration1.this.varioToneCombo.getSelectionIndex());
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.limiterModusLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData limiterModusLabelLData = new RowData();
					limiterModusLabelLData.width = 135;
					limiterModusLabelLData.height = 20;
					this.limiterModusLabel.setLayoutData(limiterModusLabelLData);
					this.limiterModusLabel.setText(Messages.getString(MessageIds.GDE_MSGT2546));
				}
				{
					this.limiterModusCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER | SWT.CENTER);
					RowData limiterModusComboLData = new RowData();
					limiterModusComboLData.width = 84;
					limiterModusComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.limiterModusCombo.setLayoutData(limiterModusComboLData);
					this.limiterModusCombo.setItems(Messages.getString(MessageIds.GDE_MSGT2521).split(GDE.STRING_COMMA));
					this.limiterModusCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "limiterModusCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.limiterModus = (short) (UniLog2SetupConfiguration1.this.limiterModusCombo.getSelectionIndex());
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.energyLimitLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData energyLimitLabelLData = new RowData();
					energyLimitLabelLData.width = 135;
					energyLimitLabelLData.height = 20;
					this.energyLimitLabel.setLayoutData(energyLimitLabelLData);
					this.energyLimitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2547));
				}
				{
					this.energyLimitText = new Text(this.commonAdjustmentsGroup, SWT.CENTER | SWT.BORDER);
					RowData energyLimitTextLData = new RowData();
					energyLimitTextLData.width = GDE.IS_MAC ? 39 : 35;
					energyLimitTextLData.height = GDE.IS_MAC ? 16 : 13;
					this.energyLimitText.setLayoutData(energyLimitTextLData);
				}
				{
					RowData energyLimitSliderLData = new RowData();
					energyLimitSliderLData.width = 101;
					energyLimitSliderLData.height = 18;
					this.energyLimitSlider = new Slider(this.commonAdjustmentsGroup, SWT.BORDER);
					this.energyLimitSlider.setLayoutData(energyLimitSliderLData);
					this.energyLimitSlider.setMinimum(0);
					this.energyLimitSlider.setMaximum(2010);
					this.energyLimitSlider.setIncrement(1);
					this.energyLimitSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "energyLimitSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.energyLimit = (short) UniLog2SetupConfiguration1.this.energyLimitSlider.getSelection();
							UniLog2SetupConfiguration1.this.energyLimitText.setText(GDE.STRING_EMPTY + UniLog2SetupConfiguration1.this.configuration.energyLimit);
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.minMaxRxLabel = new CLabel(this.commonAdjustmentsGroup, SWT.NONE);
					RowData minMaxRxLabelLData = new RowData();
					minMaxRxLabelLData.width = 135;
					minMaxRxLabelLData.height = 20;
					this.minMaxRxLabel.setLayoutData(minMaxRxLabelLData);
					this.minMaxRxLabel.setText(Messages.getString(MessageIds.GDE_MSGT2548));
				}
				{
					this.minMaxRxCombo = new CCombo(this.commonAdjustmentsGroup, SWT.BORDER);
					RowData minMaxRxComboLData = new RowData();
					minMaxRxComboLData.width = 84;
					minMaxRxComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.minMaxRxCombo.setLayoutData(minMaxRxComboLData);
					this.minMaxRxCombo.setItems(Messages.getString(MessageIds.GDE_MSGT2519).split(GDE.STRING_COMMA));
					this.minMaxRxCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "minMaxRxCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration1.this.configuration.minMaxRx = (short) (UniLog2SetupConfiguration1.this.minMaxRxCombo.getSelectionIndex());
							UniLog2SetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
			}
			this.layout();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

}
