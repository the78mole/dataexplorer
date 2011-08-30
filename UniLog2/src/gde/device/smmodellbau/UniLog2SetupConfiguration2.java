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
import gde.device.DataTypes;
import gde.device.smmodellbau.unilog2.MessageIds;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

import java.util.Locale;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

/**
 * class to implement SM UniLog2 and Multiplex M-Link reverse link configuration panel 2
 */
public class UniLog2SetupConfiguration2 extends org.eclipse.swt.widgets.Composite {
	final static Logger							log											= Logger.getLogger(UniLog2SetupConfiguration2.class.getName());

	final UniLog2Dialog							dialog;
	final DataExplorer							application;

	Composite												fillerComposite;

	Group														unilogTelemtryAlarmsGroup;
	CLabel													addressLabel1UL, addressLabel2UL, addressLabel3UL, addressLabel4UL;
	Text														addressText1UL, addressText2UL, addressText3UL, addressText4UL;
	Slider													addressSlider1UL, addressSlider2UL, addressSlider3UL, addressSlider4UL;
	Button													currentButton, voltageStartButton, voltageButton, capacityButton, heightButton, voltageRxButton, cellVoltageButton;
	CCombo													currentCombo, voltageStartCombo, voltageCombo, capacityCombo, heightCombo, voltageRxCombo, cellVoltageCombo;
	CLabel													currentLabel, voltageStartLabel, voltageLabel, capacityLabel, heightLabel, voltageRxLabel, cellVoltageLabel;
	CLabel													a1Label, a2Label, a3Label, c1Label, c2Label, c3Label, c4Label, c5Label, c6Label;
	CCombo													a1Combo, a2Combo, a3Combo, c1Combo, c2Combo, c3Combo, c4Combo, c5Combo, c6Combo;
	final int												fillerWidth							= 15;
	final int												buttonWidth							= 115;
	final int												comboWidth							= 83;

	Group														mLinkAddressesGroup;
	CLabel													addressVoltageLabel, addressCurrentLabel, addressRevolutionLabel, addressCapacityLabel, addressVarioLabel, addressHeightLabel;
	Text														addressVoltageText, addressCurrentText, addressRevolutionText, addressCapacityText, addressVarioText, addressHeightText;
	Slider													addressVoltageSlider, addressCurrentSlider, addressRevolutionSlider, addressCapacitySlider, addressVarioSlider, addressHeightSlider;
	final int												labelWidth							= 115;
	final int												textWidth								= 25;
	final int												sliderWidth							= 125;
	final int												sliderMinimum						= 0;
	final int												sliderMaximum						= 26;
	final int												sliderIncrement					= 1;
	final String[]									sliderValues						= { "  0", "  1", "  2", "  3", "  4", "  5", "  6", "  7", "  8", "  9", " 10", " 11", " 12", " 13", " 14", " 15", " - -" };

	final String[]									currentValues						= { "    1", "    5", "   10", "   15", "   20", "   30", "   40", "   50", "   60", "   70", "   80", "   90", "  100", "  150", "  200",
			"  250", "  300", "  350", "  400"									};
	final String[]									voltageStartValues			= { "  3.0", "  3.2", "  3.3", "  3.4", "  3.5", "  4.0", "  4.5", "  5.0", "  5.5", "  6.0", "  6.4", "  6.5", "  6.6", "  6.7", "  6.8",
			"  7.0", "  7.5", "  8.0", "  8.5", "  9.0", "  9.5", "  9.6", "  9.7", "  9.8", " 10.0", " 10.5", " 11.0", " 11.5", " 12.0", " 12.2", " 12.3", " 12.4", " 12.5", " 13.0", " 13.5", " 14.0",
			" 14.5", " 15.0", " 15.5", " 15.6", " 15.7", " 15.8", " 15.9", " 16.0", " 16.5", " 17.0", " 17.5", " 18.0", " 18.5", " 19.0", " 19.5", " 20.0", " 20.3", " 20.3", " 20.4", " 20.5", " 20.6",
			" 20.7", " 21.0", " 21.5", " 22.0", " 23.0", " 24.0", " 24.5", " 24.6", " 24.7", " 24.8", " 25.0", " 30.0", " 40.0", " 50.0", " 60.0" };

	final String[]									capacityValues					= { "  250", "  500", "  750", " 1000", " 1250", " 1500", " 1750", " 2000", " 2250", " 2500", " 2750", " 3000", " 3250", " 3500", " 4000",
			" 5000", " 6000"																		};
	final String[]									heightThresholds				= { "   10", "   25", "   50", "   75", "  100", "  150", "  200", "  250", "  300", "  350", "  400", "  450", "  500", "  600", "  700",
			"  800", "  900", " 1000", " 1250", " 1500", " 2000", " 2500", " 3000", " 3500", " 4000" };
	final String[]									voltageRxStartValues		= { "  3.00", "  3.25", "  3.50", "  3.75", "  4.00", "  4.25", "  4.50", "  4.75", "  4.80", "  4.85", "  4.90", "  4.95", "  5.00",
			"  5.05", "  5.10", "  5.15", "  5.20", "  5.25", "  5.50", "  6.00", "  6.25", "  6.50", "  6.75", "  7.00", "  7.25", "  7.50", "  7.75", "  8.00" };
	final String[]									cellVoltageStartValues	= new String[350];
	final UniLog2SetupReaderWriter	configuration;

	/**
	 * constructor configuration panel 2
	 * @param parent
	* @param dialog
	* @param configuration
	*/
	public UniLog2SetupConfiguration2(Composite parent, int style, UniLog2Dialog useDialog, UniLog2SetupReaderWriter useConfiguration) {
		super(parent, style);
		SWTResourceManager.registerResourceUser(this);
		this.dialog = useDialog;
		this.configuration = useConfiguration;
		this.application = DataExplorer.getInstance();
		for (int i = 0, value = 100; i < 350; i++) {
			this.cellVoltageStartValues[i] = String.format("    %.2f", ((value += 1) / 100.0)); //$NON-NLS-1$
		}
		initGUI();
	}

	public void updateValues() {
		this.currentButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_CURRENT) > 0);
		this.currentCombo.setText(String.format(Locale.ENGLISH, " %5d", this.configuration.currentAlarm)); //$NON-NLS-1$
		this.voltageStartButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE_START) > 0);
		this.voltageStartCombo.setText(String.format(Locale.ENGLISH, " %5.1f", this.configuration.voltageStartAlarm / 10.0)); //$NON-NLS-1$
		this.voltageButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
		this.voltageCombo.setText(String.format(Locale.ENGLISH, " %5.1f", this.configuration.voltageAlarm / 10.0)); //$NON-NLS-1$
		this.capacityButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_CAPACITY) > 0);
		this.capacityCombo.setText(String.format(Locale.ENGLISH, " %5d", this.configuration.capacityAlarm)); //$NON-NLS-1$
		this.heightButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_HEIGHT) > 0);
		this.heightCombo.setText(String.format(Locale.ENGLISH, " %5d", this.configuration.heightAlarm)); //$NON-NLS-1$
		this.voltageRxButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE_RX) > 0);
		this.voltageRxCombo.setText(String.format(Locale.ENGLISH, "  %5.2f", (this.configuration.voltageRxAlarm / 100.0))); //$NON-NLS-1$
		this.cellVoltageButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE_CELL) > 0);
		this.cellVoltageCombo.setText(String.format(Locale.ENGLISH, "  %5.2f", (this.configuration.cellVoltageAlarm / 100.0))); //$NON-NLS-1$

		this.addressVoltageSlider.setSelection(this.configuration.mLinkAddressVoltage);
		this.addressVoltageText.setText(this.sliderValues[this.configuration.mLinkAddressVoltage]);
		this.addressCurrentSlider.setSelection(this.configuration.mLinkAddressCurrent);
		this.addressCurrentText.setText(this.sliderValues[this.configuration.mLinkAddressCurrent]);
		this.addressRevolutionSlider.setSelection(this.configuration.mLinkAddressRevolution);
		this.addressRevolutionText.setText(this.sliderValues[this.configuration.mLinkAddressRevolution]);
		this.addressCapacitySlider.setSelection(this.configuration.mLinkAddressCapacity);
		this.addressCapacityText.setText(this.sliderValues[this.configuration.mLinkAddressCapacity]);
		this.addressVarioSlider.setSelection(this.configuration.mLinkAddressVario);
		this.addressVarioText.setText(this.sliderValues[this.configuration.mLinkAddressVario]);
		this.addressHeightSlider.setSelection(this.configuration.mLinkAddressHeight);
		this.addressHeightText.setText(this.sliderValues[this.configuration.mLinkAddressHeight]);

		this.a1Combo.select(this.configuration.mLinkAddressA1);
		this.a2Combo.select(this.configuration.mLinkAddressA2);
		this.a3Combo.select(this.configuration.mLinkAddressA3);

		this.c1Combo.select(this.configuration.mLinkAddressCell1);
		this.c2Combo.select(this.configuration.mLinkAddressCell2);
		this.c3Combo.select(this.configuration.mLinkAddressCell3);
		this.c4Combo.select(this.configuration.mLinkAddressCell4);
		this.c5Combo.select(this.configuration.mLinkAddressCell5);
		this.c6Combo.select(this.configuration.mLinkAddressCell6);
	}

	void initGUI() {
		try {
			this.setLayout(new FormLayout());
			this.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINEST, "GPSLoggerSetupConfiguration2.helpRequested, event=" + evt); //$NON-NLS-1$
					UniLog2SetupConfiguration2.this.application.openHelpDialog(Messages.getString(MessageIds.GDE_MSGT2510), "HelpInfo.html#configuration"); //$NON-NLS-1$
				}
			});
			{
				this.unilogTelemtryAlarmsGroup = new Group(this, SWT.NONE);
				RowLayout unilogTelemtryAlarmsGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.unilogTelemtryAlarmsGroup.setLayout(unilogTelemtryAlarmsGroupLayout);
				FormData unilogTelemtryAlarmsGroupLData = new FormData();
				unilogTelemtryAlarmsGroupLData.top = new FormAttachment(0, 1000, 10);
				unilogTelemtryAlarmsGroupLData.left = new FormAttachment(0, 1000, 15);
				unilogTelemtryAlarmsGroupLData.width = 290;
				unilogTelemtryAlarmsGroupLData.height = 175;
				this.unilogTelemtryAlarmsGroup.setLayoutData(unilogTelemtryAlarmsGroupLData);
				this.unilogTelemtryAlarmsGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.unilogTelemtryAlarmsGroup.setText(Messages.getString(MessageIds.GDE_MSGT2565));
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 280;
					fillerCompositeRA1LData.height = 2;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.currentButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData currentButtonLData = new RowData();
					currentButtonLData.width = this.buttonWidth;
					currentButtonLData.height = 19;
					this.currentButton.setLayoutData(currentButtonLData);
					this.currentButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentButton.setText(Messages.getString(MessageIds.GDE_MSGT2566));
					this.currentButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_CURRENT) > 0);
					this.currentButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "currentButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.currentButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_CURRENT);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_CURRENT);
							}
							UniLog2SetupConfiguration2.this.currentButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_CURRENT) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData currentCComboLData = new RowData();
					currentCComboLData.width = this.comboWidth;
					currentCComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.currentCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.currentCombo.setLayoutData(currentCComboLData);
					this.currentCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentCombo.setItems(this.currentValues);
					this.currentCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentCombo.setText(String.format(Locale.ENGLISH, "%5d", this.configuration.currentAlarm)); //$NON-NLS-1$
					this.currentCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "currentCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.currentAlarm = (short) Integer.parseInt(UniLog2SetupConfiguration2.this.currentCombo.getText().trim());
							UniLog2SetupConfiguration2.this.configuration.currentAlarm = UniLog2SetupConfiguration2.this.configuration.currentAlarm < 1 ? 1
									: UniLog2SetupConfiguration2.this.configuration.currentAlarm > 400 ? 400 : UniLog2SetupConfiguration2.this.configuration.currentAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.currentCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "currentCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.currentCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "currentCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.currentAlarm = (short) Integer.parseInt(UniLog2SetupConfiguration2.this.currentCombo.getText().trim());
							UniLog2SetupConfiguration2.this.configuration.currentAlarm = UniLog2SetupConfiguration2.this.configuration.currentAlarm < 1 ? 1
									: UniLog2SetupConfiguration2.this.configuration.currentAlarm > 400 ? 400 : UniLog2SetupConfiguration2.this.configuration.currentAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.currentLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER);
					RowData currentLabelLData = new RowData();
					currentLabelLData.width = 50;
					currentLabelLData.height = 19;
					this.currentLabel.setLayoutData(currentLabelLData);
					this.currentLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentLabel.setText(Messages.getString(MessageIds.GDE_MSGT2551));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.voltageStartButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageStartButtonLData = new RowData();
					voltageStartButtonLData.width = this.buttonWidth;
					voltageStartButtonLData.height = 19;
					this.voltageStartButton.setLayoutData(voltageStartButtonLData);
					this.voltageStartButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageStartButton.setText(Messages.getString(MessageIds.GDE_MSGT2552));
					this.voltageStartButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE_START) > 0);
					this.voltageStartButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageStartButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.voltageStartButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE_START);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE_START);
							}
							UniLog2SetupConfiguration2.this.voltageStartButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE_START) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData voltageStartCComboLData = new RowData();
					voltageStartCComboLData.width = this.comboWidth;
					voltageStartCComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.voltageStartCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.voltageStartCombo.setLayoutData(voltageStartCComboLData);
					this.voltageStartCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageStartCombo.setItems(this.voltageStartValues);
					this.voltageStartCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.voltageStartAlarm / 10.0)); //$NON-NLS-1$
					this.voltageStartCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageStartCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.voltageStartCombo.getText().trim()
									.replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 10);
							UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm = UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm < 10 ? 10
									: UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.voltageStartCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "voltageStartCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, verifyevent.text);
						}
					});
					this.voltageStartCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "voltageStartCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.voltageStartCombo.getText().trim()
									.replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 10);
							UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm = UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm < 10 ? 10
									: UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.voltageStartAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.voltageStartLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData voltageStartLabelLData = new RowData();
					voltageStartLabelLData.width = 50;
					voltageStartLabelLData.height = 19;
					this.voltageStartLabel.setLayoutData(voltageStartLabelLData);
					this.voltageStartLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageStartLabel.setText(Messages.getString(MessageIds.GDE_MSGT2553));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.voltageButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageButtonLData = new RowData();
					voltageButtonLData.width = this.buttonWidth;
					voltageButtonLData.height = 19;
					this.voltageButton.setLayoutData(voltageButtonLData);
					this.voltageButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageButton.setText(Messages.getString(MessageIds.GDE_MSGT2554));
					this.voltageButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
					this.voltageButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.voltageButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE);
							}
							UniLog2SetupConfiguration2.this.voltageButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData voltageCComboLData = new RowData();
					voltageCComboLData.width = this.comboWidth;
					voltageCComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.voltageCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.voltageCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageCombo.setLayoutData(voltageCComboLData);
					this.voltageCombo.setItems(this.voltageStartValues);
					this.voltageCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.voltageAlarm / 10.0)); //$NON-NLS-1$
					this.voltageCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.voltageAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.voltageCombo.getText().trim()
									.replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 10);
							UniLog2SetupConfiguration2.this.configuration.voltageAlarm = UniLog2SetupConfiguration2.this.configuration.voltageAlarm < 10 ? 10
									: UniLog2SetupConfiguration2.this.configuration.voltageAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.voltageAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.voltageCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "voltageCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, verifyevent.text);
						}
					});
					this.voltageCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "voltageCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.voltageAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.voltageCombo.getText().trim()
									.replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 10);
							UniLog2SetupConfiguration2.this.configuration.voltageAlarm = UniLog2SetupConfiguration2.this.configuration.voltageAlarm < 10 ? 10
									: UniLog2SetupConfiguration2.this.configuration.voltageAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.voltageAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData voltageLabelLData = new RowData();
					voltageLabelLData.width = 50;
					voltageLabelLData.height = 19;
					this.voltageLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					this.voltageLabel.setLayoutData(voltageLabelLData);
					this.voltageLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageLabel.setText(Messages.getString(MessageIds.GDE_MSGT2555));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.capacityButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageRxULButtonLData = new RowData();
					voltageRxULButtonLData.width = this.buttonWidth;
					voltageRxULButtonLData.height = 19;
					this.capacityButton.setLayoutData(voltageRxULButtonLData);
					this.capacityButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.capacityButton.setText(Messages.getString(MessageIds.GDE_MSGT2556));
					this.capacityButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_CAPACITY) > 0);
					this.capacityButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageRxButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.capacityButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_CAPACITY);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_CAPACITY);
							}
							UniLog2SetupConfiguration2.this.capacityButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_CAPACITY) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.capacityCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.capacityCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData capacityComboLData = new RowData();
					capacityComboLData.width = this.comboWidth;
					capacityComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.capacityCombo.setLayoutData(capacityComboLData);
					this.capacityCombo.setItems(this.capacityValues);
					this.capacityCombo.setText(String.format(Locale.ENGLISH, "%5d", this.configuration.capacityAlarm)); //$NON-NLS-1$
					this.capacityCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "capacityCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.capacityAlarm = (short) Integer.parseInt(UniLog2SetupConfiguration2.this.capacityCombo.getText().trim());
							UniLog2SetupConfiguration2.this.configuration.capacityAlarm = UniLog2SetupConfiguration2.this.configuration.capacityAlarm < 100 ? 100
									: UniLog2SetupConfiguration2.this.configuration.capacityAlarm > 30000 ? 30000 : UniLog2SetupConfiguration2.this.configuration.capacityAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.capacityCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "capacityCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.capacityCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "capacityCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.capacityAlarm = (short) Integer.parseInt(UniLog2SetupConfiguration2.this.capacityCombo.getText().trim());
							UniLog2SetupConfiguration2.this.configuration.capacityAlarm = UniLog2SetupConfiguration2.this.configuration.capacityAlarm < 100 ? 100
									: UniLog2SetupConfiguration2.this.configuration.capacityAlarm > 30000 ? 30000 : UniLog2SetupConfiguration2.this.configuration.capacityAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.capacityLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData voltageRxULLabelLData = new RowData();
					voltageRxULLabelLData.width = 50;
					voltageRxULLabelLData.height = 19;
					this.capacityLabel.setLayoutData(voltageRxULLabelLData);
					this.capacityLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.capacityLabel.setText(Messages.getString(MessageIds.GDE_MSGT2557));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.heightButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData heightButtonLData = new RowData();
					heightButtonLData.width = this.buttonWidth;
					heightButtonLData.height = 19;
					this.heightButton.setLayoutData(heightButtonLData);
					this.heightButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.heightButton.setText(Messages.getString(MessageIds.GDE_MSGT2567));
					this.heightButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
					this.heightButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "heightButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.heightButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE);
							}
							UniLog2SetupConfiguration2.this.heightButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData heightCComboLData = new RowData();
					heightCComboLData.width = this.comboWidth;
					heightCComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.heightCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.heightCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.heightCombo.setLayoutData(heightCComboLData);
					this.heightCombo.setItems(this.heightThresholds);
					this.heightCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "heightCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.heightAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.heightCombo.getText().trim()
									.replace(GDE.STRING_COMMA, GDE.STRING_DOT)));
							UniLog2SetupConfiguration2.this.configuration.heightAlarm = UniLog2SetupConfiguration2.this.configuration.heightAlarm < 10 ? 10
									: UniLog2SetupConfiguration2.this.configuration.heightAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.heightAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.heightCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "heightCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, verifyevent.text);
						}
					});
					this.heightCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "heightCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.heightAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.heightCombo.getText().trim()
									.replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 10);
							UniLog2SetupConfiguration2.this.configuration.heightAlarm = UniLog2SetupConfiguration2.this.configuration.heightAlarm < 10 ? 10
									: UniLog2SetupConfiguration2.this.configuration.heightAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.heightAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData heightLabelLData = new RowData();
					heightLabelLData.width = 50;
					heightLabelLData.height = 19;
					this.heightLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					this.heightLabel.setLayoutData(heightLabelLData);
					this.heightLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.heightLabel.setText(Messages.getString(MessageIds.GDE_MSGT2555));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.voltageRxButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageRxButtonLData = new RowData();
					voltageRxButtonLData.width = this.buttonWidth;
					voltageRxButtonLData.height = 19;
					this.voltageRxButton.setLayoutData(voltageRxButtonLData);
					this.voltageRxButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageRxButton.setText(Messages.getString(MessageIds.GDE_MSGT2568));
					this.voltageRxButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
					this.voltageRxButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageRxButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.voltageRxButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE);
							}
							UniLog2SetupConfiguration2.this.voltageRxButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData voltageRxCComboLData = new RowData();
					voltageRxCComboLData.width = this.comboWidth;
					voltageRxCComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.voltageRxCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.voltageRxCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageRxCombo.setLayoutData(voltageRxCComboLData);
					this.voltageRxCombo.setItems(this.voltageRxStartValues);
					this.voltageRxCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageRxCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.voltageRxCombo.getText().trim()
									.replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 100);
							UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm = UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm < 10 ? 10
									: UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.voltageRxCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "voltageRxCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, verifyevent.text);
						}
					});
					this.voltageRxCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "voltageRxCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.voltageRxCombo.getText().trim()
									.replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 100);
							UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm = UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm < 10 ? 10
									: UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.voltageRxAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData voltageRxLabelLData = new RowData();
					voltageRxLabelLData.width = 50;
					voltageRxLabelLData.height = 19;
					this.voltageRxLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					this.voltageRxLabel.setLayoutData(voltageRxLabelLData);
					this.voltageRxLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageRxLabel.setText(Messages.getString(MessageIds.GDE_MSGT2555));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = this.fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.cellVoltageButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData cellVoltageButtonLData = new RowData();
					cellVoltageButtonLData.width = this.buttonWidth;
					cellVoltageButtonLData.height = 19;
					this.cellVoltageButton.setLayoutData(cellVoltageButtonLData);
					this.cellVoltageButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.cellVoltageButton.setText(Messages.getString(MessageIds.GDE_MSGT2569));
					this.cellVoltageButton.setSelection((this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
					this.cellVoltageButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "cellVoltageButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (UniLog2SetupConfiguration2.this.cellVoltageButton.getSelection()) {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms | UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE);
							}
							else {
								UniLog2SetupConfiguration2.this.configuration.telemetryAlarms = (short) (UniLog2SetupConfiguration2.this.configuration.telemetryAlarms ^ UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE);
							}
							UniLog2SetupConfiguration2.this.cellVoltageButton.setSelection((UniLog2SetupConfiguration2.this.configuration.telemetryAlarms & UniLog2SetupReaderWriter.TEL_ALARM_VOLTAGE) > 0);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData cellVoltageCComboLData = new RowData();
					cellVoltageCComboLData.width = this.comboWidth;
					cellVoltageCComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.cellVoltageCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.cellVoltageCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.cellVoltageCombo.setLayoutData(cellVoltageCComboLData);
					this.cellVoltageCombo.setItems(this.cellVoltageStartValues);
					this.cellVoltageCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.cellVoltageAlarm / 10.0)); //$NON-NLS-1$
					this.cellVoltageCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "cellVoltageCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.cellVoltageCombo.getText().trim()
									.replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 100);
							UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm = UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm < 10 ? 10
									: UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.cellVoltageCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "cellVoltageCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, verifyevent.text);
						}
					});
					this.cellVoltageCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "cellVoltageCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm = (short) (Double.parseDouble(UniLog2SetupConfiguration2.this.cellVoltageCombo.getText().trim()
									.replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 100);
							UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm = UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm < 10 ? 10
									: UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm > 600 ? 600 : UniLog2SetupConfiguration2.this.configuration.cellVoltageAlarm;
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData cellVoltageLabelLData = new RowData();
					cellVoltageLabelLData.width = 50;
					cellVoltageLabelLData.height = 19;
					this.cellVoltageLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					this.cellVoltageLabel.setLayoutData(cellVoltageLabelLData);
					this.cellVoltageLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.cellVoltageLabel.setText(Messages.getString(MessageIds.GDE_MSGT2555));
				}
			}
			{
				this.mLinkAddressesGroup = new Group(this, SWT.NONE);
				RowLayout mLinkAddressesGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.mLinkAddressesGroup.setLayout(mLinkAddressesGroupLayout);
				FormData mLinkAddressesGroupLData = new FormData();
				mLinkAddressesGroupLData.top = new FormAttachment(0, 1000, 210);
				mLinkAddressesGroupLData.left = new FormAttachment(0, 1000, 15);
				mLinkAddressesGroupLData.width = 290;
				mLinkAddressesGroupLData.height = 225;
				this.mLinkAddressesGroup.setLayoutData(mLinkAddressesGroupLData);
				this.mLinkAddressesGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.mLinkAddressesGroup.setText(Messages.getString(MessageIds.GDE_MSGT2558));
				{
					this.fillerComposite = new Composite(this.mLinkAddressesGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 280;
					fillerCompositeRA1LData.height = 2;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.addressVoltageLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressVoltageLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel1LData = new RowData();
					addressLabel1LData.width = this.labelWidth;
					addressLabel1LData.height = 21;
					this.addressVoltageLabel.setLayoutData(addressLabel1LData);
					this.addressVoltageLabel.setText(Messages.getString(MessageIds.GDE_MSGT2559));
				}
				{
					this.addressVoltageText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressVoltageText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText1LData = new RowData();
					addressText1LData.width = this.textWidth;
					addressText1LData.height = 13;
					this.addressVoltageText.setLayoutData(addressText1LData);
					this.addressVoltageText.setText(this.sliderValues[this.configuration.mLinkAddressVario]);
				}
				{
					this.addressVoltageSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressVoltageSlider.setMinimum(this.sliderMinimum);
					this.addressVoltageSlider.setMaximum(this.sliderMaximum);
					this.addressVoltageSlider.setIncrement(this.sliderIncrement);
					RowData addressSlider1LData = new RowData();
					addressSlider1LData.width = this.sliderWidth;
					addressSlider1LData.height = 17;
					this.addressVoltageSlider.setLayoutData(addressSlider1LData);
					this.addressVoltageSlider.setSelection(this.configuration.mLinkAddressVario);
					this.addressVoltageSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "addressSlider1.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressVoltage = (byte) UniLog2SetupConfiguration2.this.addressVoltageSlider.getSelection();
							UniLog2SetupConfiguration2.this.addressVoltageText.setText(UniLog2SetupConfiguration2.this.sliderValues[UniLog2SetupConfiguration2.this.configuration.mLinkAddressVoltage]);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressCurrentLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressCurrentLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel2LData = new RowData();
					addressLabel2LData.width = this.labelWidth;
					addressLabel2LData.height = 21;
					this.addressCurrentLabel.setLayoutData(addressLabel2LData);
					this.addressCurrentLabel.setText(Messages.getString(MessageIds.GDE_MSGT2560));
				}
				{
					this.addressCurrentText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressCurrentText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText2LData = new RowData();
					addressText2LData.width = this.textWidth;
					addressText2LData.height = 13;
					this.addressCurrentText.setLayoutData(addressText2LData);
					this.addressCurrentText.setText(this.sliderValues[this.configuration.mLinkAddressA1]);
				}
				{
					this.addressCurrentSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressCurrentSlider.setMinimum(this.sliderMinimum);
					this.addressCurrentSlider.setMaximum(this.sliderMaximum);
					this.addressCurrentSlider.setIncrement(this.sliderIncrement);
					RowData addressSlider2LData = new RowData();
					addressSlider2LData.width = this.sliderWidth;
					addressSlider2LData.height = 17;
					this.addressCurrentSlider.setLayoutData(addressSlider2LData);
					this.addressCurrentSlider.setSelection(this.configuration.mLinkAddressA1);
					this.addressCurrentSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "addressSlider2.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCurrent = (byte) UniLog2SetupConfiguration2.this.addressCurrentSlider.getSelection();
							UniLog2SetupConfiguration2.this.addressCurrentText.setText(UniLog2SetupConfiguration2.this.sliderValues[UniLog2SetupConfiguration2.this.configuration.mLinkAddressCurrent]);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressRevolutionLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressRevolutionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel2maxLData = new RowData();
					addressLabel2maxLData.width = this.labelWidth;
					addressLabel2maxLData.height = 21;
					this.addressRevolutionLabel.setLayoutData(addressLabel2maxLData);
					this.addressRevolutionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2561));
				}
				{
					this.addressRevolutionText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressRevolutionText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText2maxLData = new RowData();
					addressText2maxLData.width = this.textWidth;
					addressText2maxLData.height = 13;
					this.addressRevolutionText.setLayoutData(addressText2maxLData);
				}
				{
					this.addressRevolutionSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressRevolutionSlider.setMinimum(this.sliderMinimum);
					this.addressRevolutionSlider.setMaximum(this.sliderMaximum);
					this.addressRevolutionSlider.setIncrement(this.sliderIncrement);
					RowData addressSlider2maxLData = new RowData();
					addressSlider2maxLData.width = this.sliderWidth;
					addressSlider2maxLData.height = 17;
					this.addressRevolutionSlider.setLayoutData(addressSlider2maxLData);
					//this.addressSlider2max.setSelection(this.configuration.mLinkAddressZ);
					this.addressRevolutionSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "addressSlider3.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressRevolution = (byte) UniLog2SetupConfiguration2.this.addressRevolutionSlider.getSelection();
							UniLog2SetupConfiguration2.this.addressRevolutionText.setText(UniLog2SetupConfiguration2.this.sliderValues[UniLog2SetupConfiguration2.this.configuration.mLinkAddressRevolution]);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressCapacityLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressCapacityLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel3LData = new RowData();
					addressLabel3LData.width = this.labelWidth;
					addressLabel3LData.height = 21;
					this.addressCapacityLabel.setLayoutData(addressLabel3LData);
					this.addressCapacityLabel.setText(Messages.getString(MessageIds.GDE_MSGT2562));
				}
				{
					this.addressCapacityText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressCapacityText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3LData = new RowData();
					addressText3LData.width = this.textWidth;
					addressText3LData.height = 13;
					this.addressCapacityText.setLayoutData(addressText3LData);
					this.addressCapacityText.setText(this.sliderValues[this.configuration.mLinkAddressHeight]);
				}
				{
					this.addressCapacitySlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressCapacitySlider.setMinimum(this.sliderMinimum);
					this.addressCapacitySlider.setMaximum(this.sliderMaximum);
					this.addressCapacitySlider.setIncrement(this.sliderIncrement);
					RowData addressSlider3LData = new RowData();
					addressSlider3LData.width = this.sliderWidth;
					addressSlider3LData.height = 17;
					this.addressCapacitySlider.setLayoutData(addressSlider3LData);
					this.addressCapacitySlider.setSelection(this.configuration.mLinkAddressHeight);
					this.addressCapacitySlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "addressSlider3.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCapacity = (byte) UniLog2SetupConfiguration2.this.addressCapacitySlider.getSelection();
							UniLog2SetupConfiguration2.this.addressCapacityText.setText(UniLog2SetupConfiguration2.this.sliderValues[UniLog2SetupConfiguration2.this.configuration.mLinkAddressCapacity]);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressVarioLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressVarioLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel3maxLData = new RowData();
					addressLabel3maxLData.width = this.labelWidth;
					addressLabel3maxLData.height = 21;
					this.addressVarioLabel.setLayoutData(addressLabel3maxLData);
					this.addressVarioLabel.setText(Messages.getString(MessageIds.GDE_MSGT2563));
				}
				{
					this.addressVarioText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressVarioText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3maxLData = new RowData();
					addressText3maxLData.width = this.textWidth;
					addressText3maxLData.height = 13;
					this.addressVarioText.setLayoutData(addressText3maxLData);
				}
				{
					this.addressVarioSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressVarioSlider.setMinimum(this.sliderMinimum);
					this.addressVarioSlider.setMaximum(this.sliderMaximum);
					this.addressVarioSlider.setIncrement(this.sliderIncrement);
					RowData addressSlider3maxLData = new RowData();
					addressSlider3maxLData.width = this.sliderWidth;
					addressSlider3maxLData.height = 17;
					this.addressVarioSlider.setLayoutData(addressSlider3maxLData);
					//this.addressSlider3max.setSelection(this.configuration.mLinkAddressHeightMax);
					this.addressVarioSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "addressSlider3max.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressVario = (byte) UniLog2SetupConfiguration2.this.addressVarioSlider.getSelection();
							UniLog2SetupConfiguration2.this.addressVarioText.setText(UniLog2SetupConfiguration2.this.sliderValues[UniLog2SetupConfiguration2.this.configuration.mLinkAddressVario]);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressHeightLabel = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressHeightLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel3maxLData = new RowData();
					addressLabel3maxLData.width = this.labelWidth;
					addressLabel3maxLData.height = 21;
					this.addressHeightLabel.setLayoutData(addressLabel3maxLData);
					this.addressHeightLabel.setText(Messages.getString(MessageIds.GDE_MSGT2564));
				}
				{
					this.addressHeightText = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressHeightText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3maxLData = new RowData();
					addressText3maxLData.width = this.textWidth;
					addressText3maxLData.height = 13;
					this.addressHeightText.setLayoutData(addressText3maxLData);
				}
				{
					this.addressHeightSlider = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressHeightSlider.setMinimum(this.sliderMinimum);
					this.addressHeightSlider.setMaximum(this.sliderMaximum);
					this.addressHeightSlider.setIncrement(this.sliderIncrement);
					RowData addressSlider3maxLData = new RowData();
					addressSlider3maxLData.width = this.sliderWidth;
					addressSlider3maxLData.height = 17;
					this.addressHeightSlider.setLayoutData(addressSlider3maxLData);
					//this.addressSlider3max.setSelection(this.configuration.mLinkAddressHeightMax);
					this.addressHeightSlider.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "addressSlider3max.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressHeight = (byte) UniLog2SetupConfiguration2.this.addressHeightSlider.getSelection();
							UniLog2SetupConfiguration2.this.addressHeightText.setText(UniLog2SetupConfiguration2.this.sliderValues[UniLog2SetupConfiguration2.this.configuration.mLinkAddressHeight]);
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.fillerComposite = new Composite(this.mLinkAddressesGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 280;
					fillerCompositeRA1LData.height = 4;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.a1Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData a1LabelLData = new RowData();
					a1LabelLData.width = 43;
					a1LabelLData.height = 19;
					this.a1Label.setLayoutData(a1LabelLData);
					this.a1Label.setText("A1"); //$NON-NLS-1$
				}
				{
					this.a1Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData a1ComboLData = new RowData();
					a1ComboLData.width = 40;
					a1ComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.a1Combo.setLayoutData(a1ComboLData);
					this.a1Combo.setItems(this.sliderValues);
					this.a1Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "a1Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressA1 = (byte) UniLog2SetupConfiguration2.this.a1Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.a2Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData a2LabelLData = new RowData();
					a2LabelLData.width = 43;
					a2LabelLData.height = 19;
					this.a2Label.setLayoutData(a2LabelLData);
					this.a2Label.setText("A2"); //$NON-NLS-1$
				}
				{
					this.a2Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData a2ComboLData = new RowData();
					a2ComboLData.width = 40;
					a2ComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.a2Combo.setLayoutData(a2ComboLData);
					this.a2Combo.setItems(this.sliderValues);
					this.a2Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "a1Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressA2 = (byte) UniLog2SetupConfiguration2.this.a2Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.a3Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData a3LabelLData = new RowData();
					a3LabelLData.width = 43;
					a3LabelLData.height = 19;
					this.a3Label.setLayoutData(a3LabelLData);
					this.a3Label.setText("A3"); //$NON-NLS-1$
				}
				{
					this.a3Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData a3ComboLData = new RowData();
					a3ComboLData.width = 40;
					a3ComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.a3Combo.setLayoutData(a3ComboLData);
					this.a3Combo.setItems(this.sliderValues);
					this.a3Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "a1Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressA3 = (byte) UniLog2SetupConfiguration2.this.a3Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.c1Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData c1LabelLData = new RowData();
					c1LabelLData.width = 43;
					c1LabelLData.height = 19;
					this.c1Label.setLayoutData(c1LabelLData);
					this.c1Label.setText(Messages.getString(MessageIds.GDE_MSGT2570));
				}
				{
					this.c1Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData c1ComboLData = new RowData();
					c1ComboLData.width = 40;
					c1ComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.c1Combo.setLayoutData(c1ComboLData);
					this.c1Combo.setItems(this.sliderValues);
					this.c1Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "c1Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCell1 = (byte) UniLog2SetupConfiguration2.this.c1Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.c2Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData c2LabelLData = new RowData();
					c2LabelLData.width = 43;
					c2LabelLData.height = 19;
					this.c2Label.setLayoutData(c2LabelLData);
					this.c2Label.setText(Messages.getString(MessageIds.GDE_MSGT2571));
				}
				{
					this.c2Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData c2ComboLData = new RowData();
					c2ComboLData.width = 40;
					c2ComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.c2Combo.setLayoutData(c2ComboLData);
					this.c2Combo.setItems(this.sliderValues);
					this.c2Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "a1Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCell2 = (byte) UniLog2SetupConfiguration2.this.c2Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.c3Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData c3LabelLData = new RowData();
					c3LabelLData.width = 43;
					c3LabelLData.height = 19;
					this.c3Label.setLayoutData(c3LabelLData);
					this.c3Label.setText(Messages.getString(MessageIds.GDE_MSGT2572));
				}
				{
					this.c3Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData c3ComboLData = new RowData();
					c3ComboLData.width = 40;
					c3ComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.c3Combo.setLayoutData(c3ComboLData);
					this.c3Combo.setItems(this.sliderValues);
					this.c3Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "c3Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCell3 = (byte) UniLog2SetupConfiguration2.this.c3Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.c4Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData c4LabelLData = new RowData();
					c4LabelLData.width = 43;
					c4LabelLData.height = 19;
					this.c4Label.setLayoutData(c4LabelLData);
					this.c4Label.setText(Messages.getString(MessageIds.GDE_MSGT2573));
				}
				{
					this.c4Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData c4ComboLData = new RowData();
					c4ComboLData.width = 40;
					c4ComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.c4Combo.setLayoutData(c4ComboLData);
					this.c4Combo.setItems(this.sliderValues);
					this.c4Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "c4Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCell4 = (byte) UniLog2SetupConfiguration2.this.c4Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.c5Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData c5LabelLData = new RowData();
					c5LabelLData.width = 43;
					c5LabelLData.height = 19;
					this.c5Label.setLayoutData(c5LabelLData);
					this.c5Label.setText(Messages.getString(MessageIds.GDE_MSGT2574));
				}
				{
					this.c5Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData c5ComboLData = new RowData();
					c5ComboLData.width = 40;
					c5ComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.c5Combo.setLayoutData(c5ComboLData);
					this.c5Combo.setItems(this.sliderValues);
					this.c5Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "a1Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCell5 = (byte) UniLog2SetupConfiguration2.this.c5Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.c6Label = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					RowData c6LabelLData = new RowData();
					c6LabelLData.width = 43;
					c6LabelLData.height = 19;
					this.c6Label.setLayoutData(c6LabelLData);
					this.c6Label.setText(Messages.getString(MessageIds.GDE_MSGT2575));
				}
				{
					this.c6Combo = new CCombo(this.mLinkAddressesGroup, SWT.BORDER);
					RowData c6ComboLData = new RowData();
					c6ComboLData.width = 40;
					c6ComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.c6Combo.setLayoutData(c6ComboLData);
					this.c6Combo.setItems(this.sliderValues);
					this.c6Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "a1Combo.widgetSelected, event=" + evt); //$NON-NLS-1$
							UniLog2SetupConfiguration2.this.configuration.mLinkAddressCell6 = (byte) UniLog2SetupConfiguration2.this.c6Combo.getSelectionIndex();
							UniLog2SetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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

}
