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
    
    Copyright (c) 2010 Winfried Bruegmann
****************************************************************************************/
package gde.device.smmodellbau;

import gde.GDE;
import gde.device.DataTypes;
import gde.device.smmodellbau.gpslogger.MessageIds;
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
import org.eclipse.swt.widgets.Text;

/**
 * class to implement SM GPS-Logger basic configuration panel 1
 */
public class GPSLoggerSetupConfiguration1 extends Composite {
	final static Logger			log							= Logger.getLogger(GPSLoggerSetupConfiguration1.class.getName());

	final GPSLoggerDialog		dialog;
	final DataExplorer		  application;

	Composite								fillerComposite;

	Group										gpsLoggerGroup;
	CLabel									serialNumberLabel, firmwareLabel, dataRatecLabel, startModusLabel, timeZoneLabel, varioLimitLabel, varioTonLabel, timeZoneUnitLabel, varioLimitUnitLabel;
	Text										serialNumberText, firmwareText;
	CCombo									dataRateCombo, startModusCombo, timeZoneCombo, varioLimitCombo, varioToneCombo;

	Group										gpsTelemertieGroup;
	Button									heightButton, velocityButton, distanceButton, tripLengthButton, voltageRxButton;
	CCombo									heightCombo, velocityCombo, distanceCombo, tripLengthCombo, voltageRxCombo;
	CLabel									heightLabel, velocityLabel, distanceLabel, tripLengthLabel, voltageRxLabel;

	final SetupReaderWriter	configuration;
	final String[]					dataRateValues	= Messages.getString(MessageIds.GDE_MSGT2020).split(GDE.STRING_COMMA);
	final String[]					startValues			= Messages.getString(MessageIds.GDE_MSGT2021).split(GDE.STRING_COMMA);
	final String[]					deltaUTC				= { " -12", " -11", " -10", " -9", " -8", " -7", " -6", " -5", " -4", " -3", " -2", " -1", " 0", " +1", " +2", " +3", " +4", " +5", " +6", " +7", " +8",
			" +9", " +10", " +11", " +12"			};
	final String[]					varioThresholds	= { " 0.0", " 0.1", " 0.2", " 0.3", " 0.4", " 0.5", " 0.6", " 0.7", " 0.8", " 0.9", " 1.0", " 1.1", " 1.2", " 1.3", " 1.4", " 1.5", " 1.6", " 1.7", " 1.8",
			" 1.9", " 2.0", " 2.1", " 2.2", " 2.3", " 2.4", " 2.5", " 2.6", " 2.7", " 2.8", " 2.9", " 3.0", " 3.1", " 3.2", " 3.3", " 3.4", " 3.5", " 3.6", " 3.7", " 3.8", " 3.9", " 4.0", " 4.1", " 4.2",
			" 4.3", " 4.4", " 4.5", " 4.6", " 4.7", " 4.8", " 4.9", " 5.0" };
	final String[]					varioTons				= Messages.getString(MessageIds.GDE_MSGT2022).split(GDE.STRING_COMMA);
	final String[]					heightValues		= {
			"   10", "   25", "   50", "   75", "  100", "  150", "  200", "  250", "  300", "  350", "  400", "  450", "  500", "  600", "  700", "  800", "  900", " 1000", " 1250", " 1500", " 2000", " 2500", " 3000", " 3500", " 4000" };																												//$NON-NLS-*$
	final String[]					speedValues			= {
			"   10", "   25", "   50", "   75", "  100", "  150", "  200", "  250", "  300", "  350", "  400", "  450", "  500", "  600", "  700", "  800", "  900", " 1000" };																																																											//$NON-NLS-*$
	final String[]					distanceValues	= {
			"   10", "   25", "   50", "   75", "  100", "  150", "  200", "  250", "  300", "  350", "  400", "  450", "  500", "  600", "  700", "  800", "  900", " 1000", " 1250", " 1500", " 2000", " 2500", " 3000", " 3500", " 4000", " 4500", " 5000" };																			//$NON-NLS-*$
	final String[]					tripValues			= {
			"  1.0", "  2.5", "  5.0", "  7.5", " 10.0", " 15.0", " 20.0", " 25.0", " 30.0", " 35.0", " 40.0", " 45.0", " 50.0", " 60.0", " 70.0", " 80.0", " 90.0", " 99.0" };																																																											//$NON-NLS-*$
	final String[]					voltageRxValues	= {
			"  3.00", "  3.25", "  3.50", "  3.75", "  4.00", "  4.25", "  4.50", "  4.75", "  4.80", "  4.85", "  4.90", "  4.95", "  5.00", "  5.05", "  5.10", "  5.15", "  5.20", "  5.25", "  5.50", "  6.00", "  6.25", "  6.50", "  6.75", "  7.00", "  7.25", "  7.50", "  7.75", "  8.00" }; //$NON-NLS-*$

	/**
	 * constructor configuration panel 1
	 * @param parent
	 * @param style
	 * @param dialog
	 * @param configuration
	 */
	public GPSLoggerSetupConfiguration1(Composite parent, int style, GPSLoggerDialog useDialog, SetupReaderWriter useConfiguration) {
		super(parent, style);
		SWTResourceManager.registerResourceUser(this);
		this.dialog = useDialog;
		this.configuration = useConfiguration;
		this.application = DataExplorer.getInstance();
		initGUI();
		updateValues();
	}

	public void updateValues() {
		this.serialNumberText.setText(GDE.STRING_EMPTY + this.configuration.serialNumber);
		this.firmwareText.setText(String.format(" %.2f", this.configuration.firmwareVersion / 100.0)); //$NON-NLS-1$

		this.dataRateCombo.select(this.configuration.datarate);
		this.startModusCombo.select(this.configuration.startmodus);
		this.timeZoneCombo.select(this.configuration.timeZone + 12);
		this.varioLimitCombo.select(this.configuration.varioThreshold);
		this.varioToneCombo.select(this.configuration.varioTon);

		this.heightButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_HEIGHT) > 0);
		this.heightCombo.setText(String.format("%5d", this.configuration.heightAlarm)); //$NON-NLS-1$
		this.velocityButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_SPEED) > 0);
		this.velocityCombo.setText(String.format("%5d", this.configuration.speedAlarm)); //$NON-NLS-1$
		this.distanceButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_DISTANCE) > 0);
		this.distanceCombo.setText(String.format("%5d", this.configuration.distanceAlarm)); //$NON-NLS-1$
		this.tripLengthButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_TRIP_LENGTH) > 0);
		this.tripLengthCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.tripLengthAlarm / 10.0)); //$NON-NLS-1$
		this.voltageRxButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_RX) > 0);
		this.voltageRxCombo.setText(String.format(Locale.ENGLISH, "%6.2f", this.configuration.voltageRxAlarm / 100.0)); //$NON-NLS-1$
	}

	void initGUI() {
		try {
			this.setLayout(new FormLayout());
			this.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					System.out.println("GPSLoggerSetupConfiguration1.helpRequested, event=" + evt); //$NON-NLS-1$
					GPSLoggerSetupConfiguration1.this.application.openHelpDialog(Messages.getString(MessageIds.GDE_MSGT2010), "HelpInfo.html#configuration");  //$NON-NLS-1$
				}
			});
			{
				this.gpsLoggerGroup = new Group(this, SWT.NONE);
				FormData gpsLoggerGroupLData = new FormData();
				gpsLoggerGroupLData.left = new FormAttachment(0, 1000, 15);
				gpsLoggerGroupLData.top = new FormAttachment(0, 1000, 12);
				gpsLoggerGroupLData.width = 290;
				gpsLoggerGroupLData.height = 210;
				RowLayout gpsLoggerGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.gpsLoggerGroup.setLayout(gpsLoggerGroupLayout);
				this.gpsLoggerGroup.setLayoutData(gpsLoggerGroupLData);
				this.gpsLoggerGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.gpsLoggerGroup.setText(Messages.getString(MessageIds.GDE_MSGT2031));
				{
					this.fillerComposite = new Composite(this.gpsLoggerGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 280;
					fillerCompositeRA1LData.height = 12;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.serialNumberLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData serialNumberLabelLData = new RowData();
					serialNumberLabelLData.width = 130;
					serialNumberLabelLData.height = 22;
					this.serialNumberLabel.setLayoutData(serialNumberLabelLData);
					this.serialNumberLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.serialNumberLabel.setText(Messages.getString(MessageIds.GDE_MSGT2032));
				}
				{
					this.serialNumberText = new Text(this.gpsLoggerGroup, SWT.RIGHT | SWT.BORDER);
					RowData serialNumberTextLData = new RowData();
					serialNumberTextLData.width = 83;
					serialNumberTextLData.height = 16;
					this.serialNumberText.setLayoutData(serialNumberTextLData);
					this.serialNumberText.setEditable(false);
					this.serialNumberText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.serialNumberText.setText(GDE.STRING_EMPTY + this.configuration.serialNumber);
				}
				{
					this.firmwareLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData firmwareLabelLData = new RowData();
					firmwareLabelLData.width = 130;
					firmwareLabelLData.height = 22;
					this.firmwareLabel.setLayoutData(firmwareLabelLData);
					this.firmwareLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.firmwareLabel.setText(Messages.getString(MessageIds.GDE_MSGT2033));
				}
				{
					this.firmwareText = new Text(this.gpsLoggerGroup, SWT.RIGHT | SWT.BORDER);
					RowData firmwareTextLData = new RowData();
					firmwareTextLData.width = 83;
					firmwareTextLData.height = 16;
					this.firmwareText.setLayoutData(firmwareTextLData);
					this.firmwareText.setEditable(false);
					this.firmwareText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.firmwareText.setText(String.format(" %.2f", this.configuration.firmwareVersion / 100.0)); //$NON-NLS-1$
				}
				{
					this.dataRatecLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData dataRatecLabelLData = new RowData();
					dataRatecLabelLData.width = 130;
					dataRatecLabelLData.height = 22;
					this.dataRatecLabel.setLayoutData(dataRatecLabelLData);
					this.dataRatecLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.dataRatecLabel.setText(Messages.getString(MessageIds.GDE_MSGT2034));
				}
				{
					this.dataRateCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData dataRateCComboLData = new RowData();
					dataRateCComboLData.width = 84;
					dataRateCComboLData.height = 17;
					this.dataRateCombo.setLayoutData(dataRateCComboLData);
					this.dataRateCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.dataRateCombo.setItems(this.dataRateValues);
					this.dataRateCombo.select(this.configuration.datarate);
					this.dataRateCombo.setEditable(false);
					this.dataRateCombo.setBackground(DataExplorer.COLOR_WHITE);
					this.dataRateCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "dataRateCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.datarate = GPSLoggerSetupConfiguration1.this.dataRateCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.startModusLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData startModusLabelLData = new RowData();
					startModusLabelLData.width = 130;
					startModusLabelLData.height = 22;
					this.startModusLabel.setLayoutData(startModusLabelLData);
					this.startModusLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.startModusLabel.setText(Messages.getString(MessageIds.GDE_MSGT2035));
				}
				{
					this.startModusCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData startModusCComboLData = new RowData();
					startModusCComboLData.width = 84;
					startModusCComboLData.height = 17;
					this.startModusCombo.setLayoutData(startModusCComboLData);
					this.startModusCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.startModusCombo.setItems(this.startValues);
					this.startModusCombo.select(this.configuration.startmodus);
					this.startModusCombo.setEditable(false);
					this.startModusCombo.setBackground(DataExplorer.COLOR_WHITE);
					this.startModusCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "startModusCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.startmodus = GPSLoggerSetupConfiguration1.this.startModusCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.timeZoneLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData timeZoneLabelLData = new RowData();
					timeZoneLabelLData.width = 130;
					timeZoneLabelLData.height = 22;
					this.timeZoneLabel.setLayoutData(timeZoneLabelLData);
					this.timeZoneLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.timeZoneLabel.setText(Messages.getString(MessageIds.GDE_MSGT2036));
				}
				{
					this.timeZoneCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData timeZoneCComboLData = new RowData();
					timeZoneCComboLData.width = 66;
					timeZoneCComboLData.height = 17;
					this.timeZoneCombo.setLayoutData(timeZoneCComboLData);
					this.timeZoneCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.timeZoneCombo.setItems(this.deltaUTC);
					this.timeZoneCombo.select(this.configuration.timeZone + 12);
					this.timeZoneCombo.setEditable(false);
					this.timeZoneCombo.setBackground(DataExplorer.COLOR_WHITE);
					this.timeZoneCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "timeZoneCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.timeZone = (short) (GPSLoggerSetupConfiguration1.this.timeZoneCombo.getSelectionIndex() - 12);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.timeZoneUnitLabel = new CLabel(this.gpsLoggerGroup, SWT.CENTER);
					RowData timeZoneUnitLabelLData = new RowData();
					timeZoneUnitLabelLData.width = 79;
					timeZoneUnitLabelLData.height = 22;
					this.timeZoneUnitLabel.setLayoutData(timeZoneUnitLabelLData);
					this.timeZoneUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.timeZoneUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2037));
				}
				{
					this.varioLimitLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData varioLimitLabelLData = new RowData();
					varioLimitLabelLData.width = 130;
					varioLimitLabelLData.height = 22;
					this.varioLimitLabel.setLayoutData(varioLimitLabelLData);
					this.varioLimitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioLimitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2038));
				}
				{
					this.varioLimitCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER | SWT.RIGHT);
					RowData varioLimitCComboLData = new RowData();
					varioLimitCComboLData.width = 66;
					varioLimitCComboLData.height = 17;
					this.varioLimitCombo.setLayoutData(varioLimitCComboLData);
					this.varioLimitCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioLimitCombo.setItems(this.varioThresholds);
					this.varioLimitCombo.select(this.configuration.varioThreshold);
					this.varioLimitCombo.setEditable(false);
					this.varioLimitCombo.setBackground(DataExplorer.COLOR_WHITE);
					this.varioLimitCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "varioLimitCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.varioThreshold = GPSLoggerSetupConfiguration1.this.varioLimitCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioLimitUnitLabel = new CLabel(this.gpsLoggerGroup, SWT.CENTER);
					RowData varioLimitUnitLabelLData = new RowData();
					varioLimitUnitLabelLData.width = 79;
					varioLimitUnitLabelLData.height = 22;
					this.varioLimitUnitLabel.setLayoutData(varioLimitUnitLabelLData);
					this.varioLimitUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioLimitUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2039));
				}
				{
					this.varioTonLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData varioTonLabelLData = new RowData();
					varioTonLabelLData.width = 130;
					varioTonLabelLData.height = 22;
					this.varioTonLabel.setLayoutData(varioTonLabelLData);
					this.varioTonLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioTonLabel.setText(Messages.getString(MessageIds.GDE_MSGT2040));
				}
				{
					this.varioToneCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData varioToneComboLData = new RowData();
					varioToneComboLData.width = 66;
					varioToneComboLData.height = 17;
					this.varioToneCombo.setLayoutData(varioToneComboLData);
					this.varioToneCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioToneCombo.setItems(this.varioTons);
					this.varioToneCombo.select(this.configuration.varioTon);
					this.varioToneCombo.setEditable(false);
					this.varioToneCombo.setBackground(DataExplorer.COLOR_WHITE);
					this.varioToneCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "varioToneCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.varioTon = GPSLoggerSetupConfiguration1.this.varioToneCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
			}
			{
				FormData gpsTelemertieGroupLData = new FormData();
				gpsTelemertieGroupLData.left = new FormAttachment(0, 1000, 16);
				gpsTelemertieGroupLData.top = new FormAttachment(0, 1000, 260);
				gpsTelemertieGroupLData.width = 290;
				gpsTelemertieGroupLData.height = 150;
				this.gpsTelemertieGroup = new Group(this, SWT.NONE);
				RowLayout gpsTelemertieGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.gpsTelemertieGroup.setLayout(gpsTelemertieGroupLayout);
				this.gpsTelemertieGroup.setLayoutData(gpsTelemertieGroupLData);
				this.gpsTelemertieGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.gpsTelemertieGroup.setText(Messages.getString(MessageIds.GDE_MSGT2041));
				{
					this.fillerComposite = new Composite(this.gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 280;
					fillerCompositeRALData.height = 10;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 20;
					fillerCompositeRALData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.heightButton = new Button(this.gpsTelemertieGroup, SWT.CHECK | SWT.LEFT);
					RowData heightButtonLData = new RowData();
					heightButtonLData.width = 95;
					heightButtonLData.height = 16;
					this.heightButton.setLayoutData(heightButtonLData);
					this.heightButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.heightButton.setText(Messages.getString(MessageIds.GDE_MSGT2030));
					this.heightButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_HEIGHT) > 0);
					this.heightButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "heightButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration1.this.heightButton.getSelection()) {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_HEIGHT;
							}
							else {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_HEIGHT;
							}
							GPSLoggerSetupConfiguration1.this.heightButton.setSelection((GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_HEIGHT) > 0);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.heightCombo = new CCombo(this.gpsTelemertieGroup, SWT.BORDER);
					RowData heightCComboLData = new RowData();
					heightCComboLData.width = 70;
					heightCComboLData.height = 17;
					this.heightCombo.setLayoutData(heightCComboLData);
					this.heightCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.heightCombo.setItems(this.heightValues);
					this.heightCombo.setText(String.format("%5d", this.configuration.heightAlarm)); //$NON-NLS-1$
					this.heightCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "heightCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.heightAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.heightCombo.getText().trim());
							GPSLoggerSetupConfiguration1.this.configuration.heightAlarm = GPSLoggerSetupConfiguration1.this.configuration.heightAlarm < 10 ? 10
									: GPSLoggerSetupConfiguration1.this.configuration.heightAlarm > 4000 ? 4000 : GPSLoggerSetupConfiguration1.this.configuration.heightAlarm; 
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.heightCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "heightCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.heightCombo.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "heightCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.heightAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.heightCombo.getText().trim());
							GPSLoggerSetupConfiguration1.this.configuration.heightAlarm = GPSLoggerSetupConfiguration1.this.configuration.heightAlarm < 10 ? 10
									: GPSLoggerSetupConfiguration1.this.configuration.heightAlarm > 4000 ? 4000 : GPSLoggerSetupConfiguration1.this.configuration.heightAlarm; 
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.heightLabel = new CLabel(this.gpsTelemertieGroup, SWT.CENTER);
					RowData heightLabelLData = new RowData();
					heightLabelLData.width = 89;
					heightLabelLData.height = 22;
					this.heightLabel.setLayoutData(heightLabelLData);
					this.heightLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.heightLabel.setText(Messages.getString(MessageIds.GDE_MSGT2042));
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 20;
					fillerCompositeRALData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.velocityButton = new Button(this.gpsTelemertieGroup, SWT.CHECK | SWT.LEFT);
					RowData velocityButtonLData = new RowData();
					velocityButtonLData.width = 95;
					velocityButtonLData.height = 16;
					this.velocityButton.setLayoutData(velocityButtonLData);
					this.velocityButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.velocityButton.setText(Messages.getString(MessageIds.GDE_MSGT2043));
					this.velocityButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_SPEED) > 0);
					this.velocityButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "velocityButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration1.this.heightButton.getSelection()) {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_SPEED;
							}
							else {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_SPEED;
							}
							GPSLoggerSetupConfiguration1.this.velocityButton.setSelection((GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_SPEED) > 0);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.velocityCombo = new CCombo(this.gpsTelemertieGroup, SWT.BORDER);
					RowData velocityCComboLData = new RowData();
					velocityCComboLData.width = 70;
					velocityCComboLData.height = 17;
					this.velocityCombo.setLayoutData(velocityCComboLData);
					this.velocityCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.velocityCombo.setItems(this.speedValues);
					this.velocityCombo.setText(String.format("%5d", this.configuration.speedAlarm)); //$NON-NLS-1$
					this.velocityCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "velocityCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.speedAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.velocityCombo.getText().trim());
							GPSLoggerSetupConfiguration1.this.configuration.speedAlarm = GPSLoggerSetupConfiguration1.this.configuration.speedAlarm < 10 ? 10
									: GPSLoggerSetupConfiguration1.this.configuration.speedAlarm > 1000 ? 1000 : GPSLoggerSetupConfiguration1.this.configuration.speedAlarm; 
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.velocityCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "velocityCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.velocityCombo.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "velocityCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.speedAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.velocityCombo.getText().trim());
							GPSLoggerSetupConfiguration1.this.configuration.speedAlarm = GPSLoggerSetupConfiguration1.this.configuration.speedAlarm < 10 ? 10
									: GPSLoggerSetupConfiguration1.this.configuration.speedAlarm > 1000 ? 1000 : GPSLoggerSetupConfiguration1.this.configuration.speedAlarm; 
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.velocityLabel = new CLabel(this.gpsTelemertieGroup, SWT.CENTER);
					RowData velocityLabelLData = new RowData();
					velocityLabelLData.width = 89;
					velocityLabelLData.height = 22;
					this.velocityLabel.setLayoutData(velocityLabelLData);
					this.velocityLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.velocityLabel.setText(Messages.getString(MessageIds.GDE_MSGT2044));
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 20;
					fillerCompositeRALData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.distanceButton = new Button(this.gpsTelemertieGroup, SWT.CHECK | SWT.LEFT);
					RowData distanceButtonLData = new RowData();
					distanceButtonLData.width = 95;
					distanceButtonLData.height = 16;
					this.distanceButton.setLayoutData(distanceButtonLData);
					this.distanceButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.distanceButton.setText(Messages.getString(MessageIds.GDE_MSGT2045));
					this.distanceButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_DISTANCE) > 0);
					this.distanceButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "distanceButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration1.this.heightButton.getSelection()) {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_DISTANCE;
							}
							else {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_DISTANCE;
							}
							GPSLoggerSetupConfiguration1.this.distanceButton.setSelection((GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_DISTANCE) > 0);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.distanceCombo = new CCombo(this.gpsTelemertieGroup, SWT.BORDER);
					RowData distanceCComboLData = new RowData();
					distanceCComboLData.width = 70;
					distanceCComboLData.height = 17;
					this.distanceCombo.setLayoutData(distanceCComboLData);
					this.distanceCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.distanceCombo.setItems(this.distanceValues);
					this.distanceCombo.setText(String.format("%5d", this.configuration.distanceAlarm)); //$NON-NLS-1$
					this.distanceCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "distanceCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.distanceAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.distanceCombo.getText().trim());
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.distanceCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "distanceCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.distanceCombo.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "distanceCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.distanceAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.distanceCombo.getText().trim());
							GPSLoggerSetupConfiguration1.this.configuration.distanceAlarm = GPSLoggerSetupConfiguration1.this.configuration.distanceAlarm < 10 ? 10
									: GPSLoggerSetupConfiguration1.this.configuration.distanceAlarm > 5000 ? 5000 : GPSLoggerSetupConfiguration1.this.configuration.distanceAlarm; 
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.distanceLabel = new CLabel(this.gpsTelemertieGroup, SWT.CENTER);
					RowData distanceLabelLData = new RowData();
					distanceLabelLData.width = 89;
					distanceLabelLData.height = 22;
					this.distanceLabel.setLayoutData(distanceLabelLData);
					this.distanceLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.distanceLabel.setText(Messages.getString(MessageIds.GDE_MSGT2046));
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 20;
					fillerCompositeRALData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.tripLengthButton = new Button(this.gpsTelemertieGroup, SWT.CHECK | SWT.LEFT);
					RowData pathLengthButtonLData = new RowData();
					pathLengthButtonLData.width = 95;
					pathLengthButtonLData.height = 16;
					this.tripLengthButton.setLayoutData(pathLengthButtonLData);
					this.tripLengthButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tripLengthButton.setText(Messages.getString(MessageIds.GDE_MSGT2047));
					this.tripLengthButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_TRIP_LENGTH) > 0);
					this.tripLengthButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "pathLengthButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration1.this.heightButton.getSelection()) {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_TRIP_LENGTH;
							}
							else {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_TRIP_LENGTH;
							}
							GPSLoggerSetupConfiguration1.this.tripLengthButton.setSelection((GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_TRIP_LENGTH) > 0);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData pathLengthCComboLData = new RowData();
					pathLengthCComboLData.width = 70;
					pathLengthCComboLData.height = 17;
					this.tripLengthCombo = new CCombo(this.gpsTelemertieGroup, SWT.BORDER);
					this.tripLengthCombo.setLayoutData(pathLengthCComboLData);
					this.tripLengthCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tripLengthCombo.setItems(this.tripValues);
					this.tripLengthCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.tripLengthAlarm / 10.0)); //$NON-NLS-1$
					this.tripLengthCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "tripLengthCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm = (int) (Double.parseDouble(GPSLoggerSetupConfiguration1.this.tripLengthCombo.getText().trim().replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 10);
							GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm = GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm < 1 ? 1
									: GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm > 999 ? 999 : GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm; 
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.distanceCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "tripLengthCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.distanceCombo.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "tripLengthCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm = (int) (Double.parseDouble(GPSLoggerSetupConfiguration1.this.tripLengthCombo.getText().trim().replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 10);
							GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm = GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm < 1 ? 1
									: GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm > 999 ? 999 : GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm; 
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.tripLengthLabel = new CLabel(this.gpsTelemertieGroup, SWT.CENTER);
					RowData pathLengthLabelLData = new RowData();
					pathLengthLabelLData.width = 89;
					pathLengthLabelLData.height = 22;
					this.tripLengthLabel.setLayoutData(pathLengthLabelLData);
					this.tripLengthLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tripLengthLabel.setText(Messages.getString(MessageIds.GDE_MSGT2048));
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 20;
					fillerCompositeRALData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.voltageRxButton = new Button(this.gpsTelemertieGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageRxButtonLData = new RowData();
					voltageRxButtonLData.width = 95;
					voltageRxButtonLData.height = 16;
					this.voltageRxButton.setLayoutData(voltageRxButtonLData);
					this.voltageRxButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageRxButton.setText(Messages.getString(MessageIds.GDE_MSGT2049));
					this.voltageRxButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_RX) > 0);
					this.voltageRxButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageRxButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration1.this.heightButton.getSelection()) {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_VOLTAGE_RX;
							}
							else {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_VOLTAGE_RX;
							}
							GPSLoggerSetupConfiguration1.this.voltageRxButton.setSelection((GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_RX) > 0);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.voltageRxCombo = new CCombo(this.gpsTelemertieGroup, SWT.BORDER);
					RowData voltageRxCComboLData = new RowData();
					voltageRxCComboLData.width = 70;
					voltageRxCComboLData.height = 17;
					this.voltageRxCombo.setLayoutData(voltageRxCComboLData);
					this.voltageRxCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageRxCombo.setItems(this.voltageRxValues);
					this.voltageRxCombo.setText(String.format(Locale.ENGLISH, "%6.2f", this.configuration.voltageRxAlarm / 100.0)); //$NON-NLS-1$
					this.voltageRxCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageRxCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm = (int) (Double.parseDouble(GPSLoggerSetupConfiguration1.this.voltageRxCombo.getText().trim().replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 100);
							GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm = GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm < 300 ? 300
									: GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm > 800 ? 800 : GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm; 
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.voltageRxCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "voltageRxCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, verifyevent.text);
						}
					});
					this.voltageRxCombo.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "voltageRxCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm = (int) (Double.parseDouble(GPSLoggerSetupConfiguration1.this.voltageRxCombo.getText().trim().replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 100);
							GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm = GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm < 300 ? 300
									: GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm > 800 ? 800 : GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm; 
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.voltageRxLabel = new CLabel(this.gpsTelemertieGroup, SWT.CENTER);
					RowData voltageRxLabelLData = new RowData();
					voltageRxLabelLData.width = 89;
					voltageRxLabelLData.height = 22;
					this.voltageRxLabel.setLayoutData(voltageRxLabelLData);
					this.voltageRxLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageRxLabel.setText(Messages.getString(MessageIds.GDE_MSGT2050));
				}
			}
			this.layout();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
