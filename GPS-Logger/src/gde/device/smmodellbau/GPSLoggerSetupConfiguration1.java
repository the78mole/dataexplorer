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
    
    Copyright (c) 2010,2011,2012,2013,2014,2015,2016,2017 Winfried Bruegmann
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

	Composite								fillerComposite, addonComposite;

	Group										gpsLoggerGroup;
	CLabel									serialNumberLabel,firmwareLabel,dataRatecLabel,startModusLabel,stopModusLabel,timeZoneLabel,timeAutoLabel,varioLimitClimbLabel,varioLimitSinkLabel,varioTonLabel,timeZoneUnitLabel,varioLimitUnitLabel,modusIgcLabel,modusDistanceLabel,telemetryTypeLabel,varioFactorLabel,minMaxRxLabel;
	Text										serialNumberText, firmwareText;
	CCombo									dataRateCombo, startModusCombo, stopModusCombo, timeZoneCombo, timeAutoCombo, varioLimitClimbCombo, varioLimitSinkCombo, varioToneCombo, modusIgcCombo, modusDistanceCombo, telemetryTypeCombo, varioFactorCombo, minMaxRxCombo;

	Group										gpsTelemertieGroup;
	Button									heightButton, velocityButton, distanceMaxButton, distanceMinButton, tripLengthButton, voltageRxButton;
	CCombo									heightCombo, velocityCombo, distanceMaxCombo, distanceMinCombo, tripLengthCombo, voltageRxCombo;
	CLabel									heightLabel, velocityLabel, distanceMaxLabel, distanceMinLabel, tripLengthLabel, voltageRxLabel;

	final SetupReaderWriter	configuration;
	final String[]					dataRateValues	= Messages.getString(MessageIds.GDE_MSGT2020).split(GDE.STRING_COMMA);
	final String[]					startValues			= Messages.getString(MessageIds.GDE_MSGT2021).split(GDE.STRING_COMMA);
	final String[]					stopValues			= Messages.getString(MessageIds.GDE_MSGT2072).split(GDE.STRING_COMMA);
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
	final String[]					igcModes				= Messages.getString(MessageIds.GDE_MSGT2069).split(GDE.STRING_COMMA);
	final String[]					distanceModes		= Messages.getString(MessageIds.GDE_MSGT2071).split(GDE.STRING_COMMA);
	final String[]					telemetrieTypes	= { " - - - ", " Futaba", " JR DMSS", " HoTT", " JetiDuplex", " M-Link", " FrSky" };
	final String[]					varioFactors;

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
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 41; i++) {
			sb.append(String.format(Locale.ENGLISH, " %2.1f ,", 1 + i / 10.0));
		}
		this.varioFactors = sb.toString().split(GDE.STRING_COMMA);
		initGUI();
		updateValues();
	}

	public void updateValues() {
		this.serialNumberText.setText(GDE.STRING_EMPTY + this.configuration.serialNumber);
		this.firmwareText.setText(String.format(" %.2f", this.configuration.firmwareVersion / 100.0)); //$NON-NLS-1$

		this.dataRateCombo.select(this.configuration.datarate);
		this.startModusCombo.select(this.configuration.startModus);
		this.stopModusCombo.select(this.configuration.stopModus);
		this.timeZoneCombo.select(this.configuration.timeZone + 12);
		this.timeAutoCombo.select(this.configuration.daylightSavingModus);
		this.varioLimitClimbCombo.select(this.configuration.varioThreshold);
		this.varioLimitSinkCombo.select(this.configuration.varioThresholdSink);
		this.varioToneCombo.select(this.configuration.varioTon);
		this.varioFactorCombo.select(this.configuration.varioFactor);
		this.modusIgcCombo.select(this.configuration.modusIGC);
		this.modusDistanceCombo.select(this.configuration.modusDistance);
		this.telemetryTypeCombo.select(this.configuration.telemetryType);
		this.minMaxRxCombo.select(this.configuration.rxControl);

		this.heightButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_HEIGHT) > 0);
		this.heightCombo.setText(String.format("%5d", this.configuration.heightAlarm)); //$NON-NLS-1$
		this.velocityButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_SPEED) > 0);
		this.velocityCombo.setText(String.format("%5d", this.configuration.speedAlarm)); //$NON-NLS-1$
		this.distanceMaxButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_DISTANCE_MAX) > 0);
		this.distanceMinButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_DISTANCE_MIN) > 0);
		this.distanceMaxCombo.setText(String.format("%5d", this.configuration.distanceMaxAlarm)); //$NON-NLS-1$
		this.tripLengthButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_TRIP_LENGTH) > 0);
		this.tripLengthCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.tripLengthAlarm / 10.0)); //$NON-NLS-1$
		this.voltageRxButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_RX) > 0);
		this.voltageRxCombo.setText(String.format(Locale.ENGLISH, "%6.2f", this.configuration.voltageRxAlarm / 100.0)); //$NON-NLS-1$

		this.changeVisibility();
	}
	
	private void changeVisibility() {
		if (GPSLoggerSetupConfiguration2.mLinkGroupStatic != null && !GPSLoggerSetupConfiguration2.mLinkGroupStatic.isDisposed()) {
			GPSLoggerSetupConfiguration2.mLinkGroupStatic.setVisible(false);
		}
		if (GPSLoggerSetupConfiguration2.jetiExGroupStatic != null && !GPSLoggerSetupConfiguration2.jetiExGroupStatic.isDisposed()) {
			GPSLoggerSetupConfiguration2.jetiExGroupStatic.setVisible(false);
		}
		if (GPSLoggerSetupConfiguration2.unilogTelemtryAlarmsGroupStatic != null && !GPSLoggerSetupConfiguration2.unilogTelemtryAlarmsGroupStatic.isDisposed()) {
			GPSLoggerSetupConfiguration2.unilogTelemtryAlarmsGroupStatic.setVisible(false);
		}

		if (this.gpsLoggerGroup != null && !this.gpsLoggerGroup.isDisposed()) {
		//0=invalid, 1=Futaba, 2=JR DMSS, 3=HoTT, 4=JetiDuplex, 5=M-Link, 6=FrSky
			switch (this.telemetryTypeCombo.getSelectionIndex()) {
			case 5: //M-Link
				if (GPSLoggerSetupConfiguration2.mLinkGroupStatic != null) {
					GPSLoggerSetupConfiguration2.mLinkGroupStatic.setVisible(true);
				}
				break;
			case 4: //JetiDuplex
				if (GPSLoggerSetupConfiguration2.jetiExGroupStatic != null) {
					GPSLoggerSetupConfiguration2.jetiExGroupStatic.setVisible(true);
					GPSLoggerSetupConfiguration2.unilogTelemtryAlarmsGroupStatic.setVisible(true);				}
				break;
			default:
				break;
			}
		}
	}

	void initGUI() {
		try {
			this.setLayout(new FormLayout());
			this.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINEST, "GPSLoggerSetupConfiguration1.helpRequested, event=" + evt); //$NON-NLS-1$
					GPSLoggerSetupConfiguration1.this.application.openHelpDialog(Messages.getString(MessageIds.GDE_MSGT2010), "HelpInfo.html#configuration");  //$NON-NLS-1$
				}
			});
			{
				this.gpsLoggerGroup = new Group(this, SWT.NONE);
				FormData gpsLoggerGroupLData = new FormData();
				gpsLoggerGroupLData.left = new FormAttachment(0, 1000, 10);
				gpsLoggerGroupLData.top = new FormAttachment(0, 1000, 5);
				gpsLoggerGroupLData.width = 290;
				gpsLoggerGroupLData.height = GDE.IS_MAC ? 355 : 360;
				RowLayout gpsLoggerGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.gpsLoggerGroup.setLayout(gpsLoggerGroupLayout);
				this.gpsLoggerGroup.setLayoutData(gpsLoggerGroupLData);
				this.gpsLoggerGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.gpsLoggerGroup.setText(Messages.getString(MessageIds.GDE_MSGT2031));
//				{
//					this.fillerComposite = new Composite(this.gpsLoggerGroup, SWT.NONE);
//					RowData fillerCompositeRA1LData = new RowData();
//					fillerCompositeRA1LData.width = 280;
//					fillerCompositeRA1LData.height = 2;
//					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
//				}
				{
					this.telemetryTypeLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData telemetryTypeLabelLData = new RowData();
					telemetryTypeLabelLData.width = 130;
					telemetryTypeLabelLData.height = 20;
					this.telemetryTypeLabel.setLayoutData(telemetryTypeLabelLData);
					this.telemetryTypeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.telemetryTypeLabel.setText(Messages.getString(MessageIds.GDE_MSGT2075));
				}
				{
					this.telemetryTypeCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData telemetryTypeComboLData = new RowData();
					telemetryTypeComboLData.width = 130;
					telemetryTypeComboLData.height = 17;
					this.telemetryTypeCombo.setLayoutData(telemetryTypeComboLData);
					this.telemetryTypeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.telemetryTypeCombo.setItems(this.telemetrieTypes);
					this.telemetryTypeCombo.select(this.configuration.telemetryType);
					this.telemetryTypeCombo.setEditable(false);
					this.telemetryTypeCombo.setBackground(DataExplorer.COLOR_WHITE);
					this.telemetryTypeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "telemetryTypeCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.telemetryType = GPSLoggerSetupConfiguration1.this.telemetryTypeCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							changeVisibility();
						}
					});
				}
				{
					this.serialNumberLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData serialNumberLabelLData = new RowData();
					serialNumberLabelLData.width = 130;
					serialNumberLabelLData.height = 20;
					this.serialNumberLabel.setLayoutData(serialNumberLabelLData);
					this.serialNumberLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.serialNumberLabel.setText(Messages.getString(MessageIds.GDE_MSGT2032));
				}
				{
					this.serialNumberText = new Text(this.gpsLoggerGroup, SWT.CENTER | SWT.BORDER);
					RowData serialNumberTextLData = new RowData();
					serialNumberTextLData.width = 67;
					serialNumberTextLData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 12;
					this.serialNumberText.setLayoutData(serialNumberTextLData);
					this.serialNumberText.setEditable(false);
					this.serialNumberText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.serialNumberText.setText(GDE.STRING_EMPTY + this.configuration.serialNumber);
				}
				{
					this.firmwareLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData firmwareLabelLData = new RowData();
					firmwareLabelLData.width = 130;
					firmwareLabelLData.height = 20;
					this.firmwareLabel.setLayoutData(firmwareLabelLData);
					this.firmwareLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.firmwareLabel.setText(Messages.getString(MessageIds.GDE_MSGT2033));
				}
				{
					this.firmwareText = new Text(this.gpsLoggerGroup, SWT.CENTER | SWT.BORDER);
					RowData firmwareTextLData = new RowData();
					firmwareTextLData.width = 67;
					firmwareTextLData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 12;
					this.firmwareText.setLayoutData(firmwareTextLData);
					this.firmwareText.setEditable(false);
					this.firmwareText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.firmwareText.setText(String.format(" %.2f", this.configuration.firmwareVersion / 100.0)); //$NON-NLS-1$
				}
				{
					this.dataRatecLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData dataRatecLabelLData = new RowData();
					dataRatecLabelLData.width = 130;
					dataRatecLabelLData.height = 20;
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
						@Override
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
					startModusLabelLData.height = 20;
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
					this.startModusCombo.select(this.configuration.startModus);
					this.startModusCombo.setEditable(false);
					this.startModusCombo.setBackground(DataExplorer.COLOR_WHITE);
					this.startModusCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "startModusCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.startModus = GPSLoggerSetupConfiguration1.this.startModusCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.stopModusLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData stopModusLabelLData = new RowData();
					stopModusLabelLData.width = 130;
					stopModusLabelLData.height = 20;
					this.stopModusLabel.setLayoutData(stopModusLabelLData);
					this.stopModusLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.stopModusLabel.setText(Messages.getString(MessageIds.GDE_MSGT2026));
				}
				{
					this.stopModusCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData stopModusCComboLData = new RowData();
					stopModusCComboLData.width = 84;
					stopModusCComboLData.height = 17;
					this.stopModusCombo.setLayoutData(stopModusCComboLData);
					this.stopModusCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.stopModusCombo.setItems(this.stopValues);
					this.stopModusCombo.select(this.configuration.stopModus);
					this.stopModusCombo.setEditable(false);
					this.stopModusCombo.setBackground(DataExplorer.COLOR_WHITE);
					this.stopModusCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "stopModusCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.stopModus = GPSLoggerSetupConfiguration1.this.stopModusCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.timeZoneLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData timeZoneLabelLData = new RowData();
					timeZoneLabelLData.width = 130;
					timeZoneLabelLData.height = 20;
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
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "timeZoneCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.timeZone = (short) (GPSLoggerSetupConfiguration1.this.timeZoneCombo.getSelectionIndex() - 12);
							GPSLoggerSetupConfiguration1.this.dialog.device.setUTCdelta(GPSLoggerSetupConfiguration1.this.configuration.timeZone);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.timeZoneUnitLabel = new CLabel(this.gpsLoggerGroup, SWT.CENTER);
					RowData timeZoneUnitLabelLData = new RowData();
					timeZoneUnitLabelLData.width = 79;
					timeZoneUnitLabelLData.height = 20;
					this.timeZoneUnitLabel.setLayoutData(timeZoneUnitLabelLData);
					this.timeZoneUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.timeZoneUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2037));
				}
				{
					this.timeAutoLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData timeZoneLabelLData = new RowData();
					timeZoneLabelLData.width = 130;
					timeZoneLabelLData.height = 20;
					this.timeAutoLabel.setLayoutData(timeZoneLabelLData);
					this.timeAutoLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.timeAutoLabel.setText(Messages.getString(MessageIds.GDE_MSGT2073));
				}
				{
					this.timeAutoCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData timeZoneCComboLData = new RowData();
					timeZoneCComboLData.width = 66;
					timeZoneCComboLData.height = 17;
					this.timeAutoCombo.setLayoutData(timeZoneCComboLData);
					this.timeAutoCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.timeAutoCombo.setItems(Messages.getString(MessageIds.GDE_MSGT2070).split(GDE.STRING_COMMA));
					this.timeAutoCombo.select(this.configuration.daylightSavingModus);
					this.timeAutoCombo.setEditable(false);
					this.timeAutoCombo.setBackground(DataExplorer.COLOR_WHITE);
					this.timeAutoCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "timeAutoCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.timeZone = (short) (GPSLoggerSetupConfiguration1.this.timeAutoCombo.getSelectionIndex());
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioLimitClimbLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData varioLimitLabelLData = new RowData();
					varioLimitLabelLData.width = 130;
					varioLimitLabelLData.height = 20;
					this.varioLimitClimbLabel.setLayoutData(varioLimitLabelLData);
					this.varioLimitClimbLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioLimitClimbLabel.setText(Messages.getString(MessageIds.GDE_MSGT2038));
				}
				{
					this.varioLimitClimbCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER | SWT.RIGHT);
					RowData varioLimitCComboLData = new RowData();
					varioLimitCComboLData.width = 66;
					varioLimitCComboLData.height = 17;
					this.varioLimitClimbCombo.setLayoutData(varioLimitCComboLData);
					this.varioLimitClimbCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioLimitClimbCombo.setItems(this.varioThresholds);
					this.varioLimitClimbCombo.select(this.configuration.varioThreshold);
					this.varioLimitClimbCombo.setEditable(false);
					this.varioLimitClimbCombo.setBackground(DataExplorer.COLOR_WHITE);
					this.varioLimitClimbCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "varioLimitCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.varioThreshold = GPSLoggerSetupConfiguration1.this.varioLimitClimbCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioLimitUnitLabel = new CLabel(this.gpsLoggerGroup, SWT.CENTER);
					RowData varioLimitUnitLabelLData = new RowData();
					varioLimitUnitLabelLData.width = 79;
					varioLimitUnitLabelLData.height = 20;
					this.varioLimitUnitLabel.setLayoutData(varioLimitUnitLabelLData);
					this.varioLimitUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioLimitUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2039));
				}
				{
					this.varioLimitSinkLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData varioLimitLabelLData = new RowData();
					varioLimitLabelLData.width = 130;
					varioLimitLabelLData.height = 20;
					this.varioLimitSinkLabel.setLayoutData(varioLimitLabelLData);
					this.varioLimitSinkLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioLimitSinkLabel.setText(Messages.getString(MessageIds.GDE_MSGT2074));
				}
				{
					this.varioLimitSinkCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER | SWT.RIGHT);
					RowData varioLimitCComboLData = new RowData();
					varioLimitCComboLData.width = 66;
					varioLimitCComboLData.height = 17;
					this.varioLimitSinkCombo.setLayoutData(varioLimitCComboLData);
					this.varioLimitSinkCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioLimitSinkCombo.setItems(this.varioThresholds);
					this.varioLimitSinkCombo.select(this.configuration.varioThreshold);
					this.varioLimitSinkCombo.setEditable(false);
					this.varioLimitSinkCombo.setBackground(DataExplorer.COLOR_WHITE);
					this.varioLimitSinkCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "varioLimitSinkCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.varioThresholdSink = GPSLoggerSetupConfiguration1.this.varioLimitSinkCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioLimitUnitLabel = new CLabel(this.gpsLoggerGroup, SWT.CENTER);
					RowData varioLimitUnitLabelLData = new RowData();
					varioLimitUnitLabelLData.width = 79;
					varioLimitUnitLabelLData.height = 20;
					this.varioLimitUnitLabel.setLayoutData(varioLimitUnitLabelLData);
					this.varioLimitUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioLimitUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2039));
				}
				{
					this.varioTonLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData varioTonLabelLData = new RowData();
					varioTonLabelLData.width = 130;
					varioTonLabelLData.height = 20;
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
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "varioToneCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.varioTon = GPSLoggerSetupConfiguration1.this.varioToneCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.varioFactorLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData varioFactorLabelLData = new RowData();
					varioFactorLabelLData.width = 130;
					varioFactorLabelLData.height = 20;
					this.varioFactorLabel.setLayoutData(varioFactorLabelLData);
					this.varioFactorLabel.setText(Messages.getString(MessageIds.GDE_MSGT2078));
					this.varioFactorLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.varioFactorCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER | SWT.CENTER);
					RowData varioFactorComboLData = new RowData();
					varioFactorComboLData.width = 84;
					varioFactorComboLData.height = GDE.IS_MAC ? 17 : 14;
					this.varioFactorCombo.setLayoutData(varioFactorComboLData);
					this.varioFactorCombo.setItems(this.varioFactors);
					this.varioFactorCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.varioFactorCombo.setEditable(false);
					this.varioFactorCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.varioFactorCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration1.log.log(Level.FINEST, "varioFactorCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.varioFactor = (short) (GPSLoggerSetupConfiguration1.this.varioFactorCombo.getSelectionIndex());
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.modusIgcLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData modusIgcLabelLData = new RowData();
					modusIgcLabelLData.width = 130;
					modusIgcLabelLData.height = 20;
					this.modusIgcLabel.setLayoutData(modusIgcLabelLData);
					this.modusIgcLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.modusIgcLabel.setText(Messages.getString(MessageIds.GDE_MSGT2068));
				}
				{
					this.modusIgcCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData modusIgcComboLData = new RowData();
					modusIgcComboLData.width = 66;
					modusIgcComboLData.height = 17;
					this.modusIgcCombo.setLayoutData(modusIgcComboLData);
					this.modusIgcCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.modusIgcCombo.setItems(this.igcModes);
					this.modusIgcCombo.select(this.configuration.modusIGC);
					this.modusIgcCombo.setEditable(false);
					this.modusIgcCombo.setBackground(DataExplorer.COLOR_WHITE);
					this.modusIgcCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "varioToneCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.modusIGC = GPSLoggerSetupConfiguration1.this.modusIgcCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.modusDistanceLabel = new CLabel(this.gpsLoggerGroup, SWT.RIGHT);
					RowData modusDistanceLabelLData = new RowData();
					modusDistanceLabelLData.width = 130;
					modusDistanceLabelLData.height = 20;
					this.modusDistanceLabel.setLayoutData(modusDistanceLabelLData);
					this.modusDistanceLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.modusDistanceLabel.setText(Messages.getString(MessageIds.GDE_MSGT2027));
				}
				{
					this.modusDistanceCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData modusDistanceComboLData = new RowData();
					modusDistanceComboLData.width = 66;
					modusDistanceComboLData.height = 17;
					this.modusDistanceCombo.setLayoutData(modusDistanceComboLData);
					this.modusDistanceCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.modusDistanceCombo.setItems(this.distanceModes);
					this.modusDistanceCombo.select(this.configuration.modusDistance);
					this.modusDistanceCombo.setEditable(false);
					this.modusDistanceCombo.setBackground(DataExplorer.COLOR_WHITE);
					this.modusDistanceCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "modusDistanceCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.modusDistance = GPSLoggerSetupConfiguration1.this.modusDistanceCombo.getSelectionIndex();
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.minMaxRxLabel = new CLabel(this.gpsLoggerGroup, SWT.NONE);
					RowData minMaxRxLabelLData = new RowData();
					minMaxRxLabelLData.width = 130;
					minMaxRxLabelLData.height = 20;
					this.minMaxRxLabel.setLayoutData(minMaxRxLabelLData);
					this.minMaxRxLabel.setText(Messages.getString(MessageIds.GDE_MSGT2080));
					this.minMaxRxLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					this.minMaxRxCombo = new CCombo(this.gpsLoggerGroup, SWT.BORDER);
					RowData minMaxRxComboLData = new RowData();
					minMaxRxComboLData.width = 104;
					minMaxRxComboLData.height = GDE.IS_MAC ? 18 : 14;
					this.minMaxRxCombo.setLayoutData(minMaxRxComboLData);
					this.minMaxRxCombo.setItems(Messages.getString(MessageIds.GDE_MSGT2081).split(GDE.STRING_COMMA));
					this.minMaxRxCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.minMaxRxCombo.setEditable(false);
					this.minMaxRxCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					this.minMaxRxCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration1.log.log(Level.FINEST, "minMaxRxCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.rxControl = (short) (GPSLoggerSetupConfiguration1.this.minMaxRxCombo.getSelectionIndex());
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
			}
			{
				FormData gpsTelemertieGroupLData = new FormData();
				gpsTelemertieGroupLData.left = new FormAttachment(0, 1000, 10);
				gpsTelemertieGroupLData.top = new FormAttachment(0, 1000, 390);
				gpsTelemertieGroupLData.width = 290;
				gpsTelemertieGroupLData.height = 143;
				this.gpsTelemertieGroup = new Group(this, SWT.NONE);
				RowLayout gpsTelemertieGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.gpsTelemertieGroup.setLayout(gpsTelemertieGroupLayout);
				this.gpsTelemertieGroup.setLayoutData(gpsTelemertieGroupLData);
				this.gpsTelemertieGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.gpsTelemertieGroup.setText(Messages.getString(MessageIds.GDE_MSGT2041));
				{
					this.fillerComposite = new Composite(this.gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 275;
					fillerCompositeRALData.height = 5;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 15;
					fillerCompositeRALData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.heightButton = new Button(this.gpsTelemertieGroup, SWT.CHECK | SWT.LEFT);
					RowData heightButtonLData = new RowData();
					heightButtonLData.width = 100;
					heightButtonLData.height = 16;
					this.heightButton.setLayoutData(heightButtonLData);
					this.heightButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.heightButton.setText(Messages.getString(MessageIds.GDE_MSGT2030));
					this.heightButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_HEIGHT) > 0);
					this.heightButton.addSelectionListener(new SelectionAdapter() {
						@Override
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
						@Override
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
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "heightCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration1.this.configuration.heightAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.heightCombo.getText().trim());
								GPSLoggerSetupConfiguration1.this.configuration.heightAlarm = GPSLoggerSetupConfiguration1.this.configuration.heightAlarm < 10 ? 10
										: GPSLoggerSetupConfiguration1.this.configuration.heightAlarm > 4000 ? 4000 : GPSLoggerSetupConfiguration1.this.configuration.heightAlarm; 
								GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.heightLabel = new CLabel(this.gpsTelemertieGroup, SWT.CENTER);
					RowData heightLabelLData = new RowData();
					heightLabelLData.width = 89;
					heightLabelLData.height = 20;
					this.heightLabel.setLayoutData(heightLabelLData);
					this.heightLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.heightLabel.setText(Messages.getString(MessageIds.GDE_MSGT2042));
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 15;
					fillerCompositeRALData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.velocityButton = new Button(this.gpsTelemertieGroup, SWT.CHECK | SWT.LEFT);
					RowData velocityButtonLData = new RowData();
					velocityButtonLData.width = 100;
					velocityButtonLData.height = 16;
					this.velocityButton.setLayoutData(velocityButtonLData);
					this.velocityButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.velocityButton.setText(Messages.getString(MessageIds.GDE_MSGT2043));
					this.velocityButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_SPEED) > 0);
					this.velocityButton.addSelectionListener(new SelectionAdapter() {
						@Override
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
						@Override
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
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "velocityCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration1.this.configuration.speedAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.velocityCombo.getText().trim());
								GPSLoggerSetupConfiguration1.this.configuration.speedAlarm = GPSLoggerSetupConfiguration1.this.configuration.speedAlarm < 10 ? 10
										: GPSLoggerSetupConfiguration1.this.configuration.speedAlarm > 1000 ? 1000 : GPSLoggerSetupConfiguration1.this.configuration.speedAlarm; 
								GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.velocityLabel = new CLabel(this.gpsTelemertieGroup, SWT.CENTER);
					RowData velocityLabelLData = new RowData();
					velocityLabelLData.width = 89;
					velocityLabelLData.height = 20;
					this.velocityLabel.setLayoutData(velocityLabelLData);
					this.velocityLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.velocityLabel.setText(Messages.getString(MessageIds.GDE_MSGT2044));
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 15;
					fillerCompositeRALData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.distanceMaxButton = new Button(this.gpsTelemertieGroup, SWT.CHECK | SWT.LEFT);
					RowData distanceButtonLData = new RowData();
					distanceButtonLData.width = 100;
					distanceButtonLData.height = 16;
					this.distanceMaxButton.setLayoutData(distanceButtonLData);
					this.distanceMaxButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.distanceMaxButton.setText(Messages.getString(MessageIds.GDE_MSGT2045));
					this.distanceMaxButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_DISTANCE_MAX) > 0);
					this.distanceMaxButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "distanceButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration1.this.heightButton.getSelection()) {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_DISTANCE_MAX;
							}
							else {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_DISTANCE_MAX;
							}
							GPSLoggerSetupConfiguration1.this.distanceMaxButton.setSelection((GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_DISTANCE_MAX) > 0);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.distanceMaxCombo = new CCombo(this.gpsTelemertieGroup, SWT.BORDER);
					RowData distanceCComboLData = new RowData();
					distanceCComboLData.width = 70;
					distanceCComboLData.height = 17;
					this.distanceMaxCombo.setLayoutData(distanceCComboLData);
					this.distanceMaxCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.distanceMaxCombo.setItems(this.distanceValues);
					this.distanceMaxCombo.setText(String.format("%5d", this.configuration.distanceMaxAlarm)); //$NON-NLS-1$
					this.distanceMaxCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "distanceCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.distanceMaxAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.distanceMaxCombo.getText().trim());
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.distanceMaxCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "distanceCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.distanceMaxCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "distanceCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration1.this.configuration.distanceMaxAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.distanceMaxCombo.getText().trim());
								GPSLoggerSetupConfiguration1.this.configuration.distanceMaxAlarm = GPSLoggerSetupConfiguration1.this.configuration.distanceMaxAlarm < 10 ? 10
										: GPSLoggerSetupConfiguration1.this.configuration.distanceMaxAlarm > 5000 ? 5000 : GPSLoggerSetupConfiguration1.this.configuration.distanceMaxAlarm; 
								GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.distanceMaxLabel = new CLabel(this.gpsTelemertieGroup, SWT.CENTER);
					RowData distanceLabelLData = new RowData();
					distanceLabelLData.width = 89;
					distanceLabelLData.height = 20;
					this.distanceMaxLabel.setLayoutData(distanceLabelLData);
					this.distanceMaxLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.distanceMaxLabel.setText(Messages.getString(MessageIds.GDE_MSGT2046));
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 15;
					fillerCompositeRALData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.distanceMinButton = new Button(this.gpsTelemertieGroup, SWT.CHECK | SWT.LEFT);
					RowData distanceButtonLData = new RowData();
					distanceButtonLData.width = 100;
					distanceButtonLData.height = 16;
					this.distanceMinButton.setLayoutData(distanceButtonLData);
					this.distanceMinButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.distanceMinButton.setText(Messages.getString(MessageIds.GDE_MSGT2082));
					this.distanceMinButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_DISTANCE_MIN) > 0);
					this.distanceMinButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "distanceMinButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration1.this.distanceMinButton.getSelection()) {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_DISTANCE_MIN;
							}
							else {
								GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_DISTANCE_MIN;
							}
							GPSLoggerSetupConfiguration1.this.distanceMinButton.setSelection((GPSLoggerSetupConfiguration1.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_DISTANCE_MIN) > 0);
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.distanceMinCombo = new CCombo(this.gpsTelemertieGroup, SWT.BORDER);
					RowData distanceCComboLData = new RowData();
					distanceCComboLData.width = 70;
					distanceCComboLData.height = 17;
					this.distanceMinCombo.setLayoutData(distanceCComboLData);
					this.distanceMinCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.distanceMinCombo.setItems(this.distanceValues);
					this.distanceMinCombo.setText(String.format("%5d", this.configuration.distanceMinAlarm)); //$NON-NLS-1$
					this.distanceMinCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "distanceMinCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.distanceMinAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.distanceMinCombo.getText().trim());
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.distanceMinCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "distanceMinCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.distanceMinCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "distanceMinCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration1.this.configuration.distanceMinAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration1.this.distanceMinCombo.getText().trim());
								GPSLoggerSetupConfiguration1.this.configuration.distanceMinAlarm = GPSLoggerSetupConfiguration1.this.configuration.distanceMinAlarm < 0 ? 0
										: GPSLoggerSetupConfiguration1.this.configuration.distanceMinAlarm > 5000 ? 5000 : GPSLoggerSetupConfiguration1.this.configuration.distanceMinAlarm; 
								GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.distanceMinLabel = new CLabel(this.gpsTelemertieGroup, SWT.CENTER);
					RowData distanceLabelLData = new RowData();
					distanceLabelLData.width = 89;
					distanceLabelLData.height = 20;
					this.distanceMinLabel.setLayoutData(distanceLabelLData);
					this.distanceMinLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.distanceMinLabel.setText(Messages.getString(MessageIds.GDE_MSGT2046));
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 15;
					fillerCompositeRALData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.tripLengthButton = new Button(this.gpsTelemertieGroup, SWT.CHECK | SWT.LEFT);
					RowData pathLengthButtonLData = new RowData();
					pathLengthButtonLData.width = 100;
					pathLengthButtonLData.height = 16;
					this.tripLengthButton.setLayoutData(pathLengthButtonLData);
					this.tripLengthButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tripLengthButton.setText(Messages.getString(MessageIds.GDE_MSGT2047));
					this.tripLengthButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_TRIP_LENGTH) > 0);
					this.tripLengthButton.addSelectionListener(new SelectionAdapter() {
						@Override
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
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "tripLengthCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm = (int) (Double.parseDouble(GPSLoggerSetupConfiguration1.this.tripLengthCombo.getText().trim().replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 10);
							GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm = GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm < 1 ? 1
									: GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm > 999 ? 999 : GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm; 
							GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.distanceMaxCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(Level.FINEST, "tripLengthCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.distanceMaxCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "tripLengthCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm = (int) (Double.parseDouble(GPSLoggerSetupConfiguration1.this.tripLengthCombo.getText().trim().replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 10);
								GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm = GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm < 1 ? 1
										: GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm > 999 ? 999 : GPSLoggerSetupConfiguration1.this.configuration.tripLengthAlarm; 
								GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.tripLengthLabel = new CLabel(this.gpsTelemertieGroup, SWT.CENTER);
					RowData pathLengthLabelLData = new RowData();
					pathLengthLabelLData.width = 89;
					pathLengthLabelLData.height = 20;
					this.tripLengthLabel.setLayoutData(pathLengthLabelLData);
					this.tripLengthLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tripLengthLabel.setText(Messages.getString(MessageIds.GDE_MSGT2048));
				}
				{
					this.fillerComposite = new Composite(this.gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 15;
					fillerCompositeRALData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					this.voltageRxButton = new Button(this.gpsTelemertieGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageRxButtonLData = new RowData();
					voltageRxButtonLData.width = 100;
					voltageRxButtonLData.height = 16;
					this.voltageRxButton.setLayoutData(voltageRxButtonLData);
					this.voltageRxButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageRxButton.setText(Messages.getString(MessageIds.GDE_MSGT2049));
					this.voltageRxButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_RX) > 0);
					this.voltageRxButton.addSelectionListener(new SelectionAdapter() {
						@Override
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
						@Override
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
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(Level.FINEST, "voltageRxCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							try {
								GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm = (int) (Double.parseDouble(GPSLoggerSetupConfiguration1.this.voltageRxCombo.getText().trim().replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 100);
								GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm = GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm < 300 ? 300
										: GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm > 800 ? 800 : GPSLoggerSetupConfiguration1.this.configuration.voltageRxAlarm; 
								GPSLoggerSetupConfiguration1.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.voltageRxLabel = new CLabel(this.gpsTelemertieGroup, SWT.CENTER);
					RowData voltageRxLabelLData = new RowData();
					voltageRxLabelLData.width = 89;
					voltageRxLabelLData.height = 20;
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
