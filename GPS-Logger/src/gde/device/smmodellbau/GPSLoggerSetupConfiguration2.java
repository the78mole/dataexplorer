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
    
    Copyright (c) 2010,2011,2012 Winfried Bruegmann
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
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

/**
 * class to implement SM GPS-Logger UniLog and Multiplex M-Link reverse link configuration panel 2
 */
public class GPSLoggerSetupConfiguration2 extends org.eclipse.swt.widgets.Composite {
	final static Logger			log									= Logger.getLogger(GPSLoggerSetupConfiguration2.class.getName());

	final GPSLoggerDialog		dialog;
	final DataExplorer		  application;

	Composite								fillerComposite;

	Group										unilogTelemtryAlarmsGroup;
	CLabel									addressLabel1UL, addressLabel2UL, addressLabel3UL, addressLabel4UL;
	Text										addressText1UL, addressText2UL, addressText3UL, addressText4UL;
	Slider									addressSlider1UL, addressSlider2UL, addressSlider3UL, addressSlider4UL;
	Button									currentButton, voltageStartButton, voltageButton, capacityButton;
	CCombo									currentCombo, voltageStartCombo, voltageCombo, capacityCombo;
	CLabel									currentLabel, voltageStartLabel, voltageLabel, capacityLabel;
	final int								fillerWidth					= 15;
	final int								buttonWidth					= 115;
	final int								comboWidth					= 83;

	Group										mLinkAddressesGroup;
	CLabel									addressLabel1, addressLabel2, addressLabel2max, addressLabel3, addressLabel3max, addressLabel4, addressLabel5, addressLabel6;
	Text										addressText1, addressText2, addressText2max, addressText3, addressText3max, addressText4, addressText5, addressText6;
	Slider									addressSlider1, addressSlider2, addressSlider2max, addressSlider3, addressSlider3max, addressSlider4, addressSlider5, addressSlider6;
	final int								labelWidth					= 115;
	final int								textWidth						= 25;
	final int								sliderWidth					= 125;
	final int								sliderMinimum				= 0;
	final int								sliderMaximum				= 26;
	final int								sliderIncrement			= 1;
	final String[]					sliderValues 				= {" 0", " 1", " 2", " 3", " 4", " 5", " 6", " 7", " 8", " 9", "10", "11", "12", "13", "14", "15", "- -"};

	final String[]					currentValues				= { "    1", "    5", "   10", "   15", "   20", "   30", "   40", "   50", "   60", "   70", "   80", "   90", "  100", "  150", "  200", "  250",
			"  300", "  350", "  400"							};
	final String[]					voltageStartValues	= { "  3.0", "  3.2", "  3.3", "  3.4", "  3.5", "  4.0", "  4.5", "  5.0", "  5.5", "  6.0", "  6.4", "  6.5", "  6.6", "  6.7", "  6.8", "  7.0",
			"  7.5", "  8.0", "  8.5", "  9.0", "  9.5", "  9.6", "  9.7", "  9.8", " 10.0", " 10.5", " 11.0", " 11.5", " 12.0", " 12.2", " 12.3", " 12.4", " 12.5", " 13.0", " 13.5", " 14.0", " 14.5",
			" 15.0", " 15.5", " 15.6", " 15.7", " 15.8", " 15.9", " 16.0", " 16.5", " 17.0", " 17.5", " 18.0", " 18.5", " 19.0", " 19.5", " 20.0", " 20.3", " 20.3", " 20.4", " 20.5", " 20.6", " 20.7",
			" 21.0", " 21.5", " 22.0", " 23.0", " 24.0", " 24.5", " 24.6", " 24.7", " 24.8", " 25.0", " 30.0", " 40.0", " 50.0", " 60.0" };

	final String[]					capacityValues 			= {"  250", "  500", "  750", " 1000", " 1250", " 1500", " 1750", " 2000", " 2250", " 2500", " 2750", " 3000", " 3250", " 3500", " 4000", " 5000", " 6000"};
	final SetupReaderWriter	configuration;

	/**
	 * constructor configuration panel 2
	 * @param parent
	* @param dialog
	* @param configuration
	*/
	public GPSLoggerSetupConfiguration2(Composite parent, int style, GPSLoggerDialog useDialog, SetupReaderWriter useConfiguration) {
		super(parent, style);
		SWTResourceManager.registerResourceUser(this);
		this.dialog = useDialog;
		this.configuration = useConfiguration;
		this.application = DataExplorer.getInstance();
		initGUI();
	}

	public void updateValues() {
		this.addressSlider1.setSelection(this.configuration.mLinkAddressVario);
		this.addressText1.setText(sliderValues[this.configuration.mLinkAddressVario]);
		this.addressSlider2.setSelection(this.configuration.mLinkAddressSpeed);
		this.addressText2.setText(sliderValues[this.configuration.mLinkAddressSpeed]);
		this.addressSlider2max.setSelection(this.configuration.mLinkAddressSpeedMax);
		this.addressText2max.setText(sliderValues[this.configuration.mLinkAddressSpeedMax]);
		this.addressSlider3.setSelection(this.configuration.mLinkAddressHeight);
		this.addressText3.setText(sliderValues[this.configuration.mLinkAddressHeight]);
		this.addressSlider3max.setSelection(this.configuration.mLinkAddressHeightMax);
		this.addressText3max.setText(sliderValues[this.configuration.mLinkAddressHeightMax]);
		this.addressSlider4.setSelection(this.configuration.mLinkAddressDistance);
		this.addressText4.setText(sliderValues[this.configuration.mLinkAddressDistance]);
		this.addressSlider5.setSelection(this.configuration.mLinkAddressDirection);
		this.addressText5.setText(sliderValues[this.configuration.mLinkAddressDirection]);
		this.addressSlider6.setSelection(this.configuration.mLinkAddressTripLength);
		this.addressText6.setText(sliderValues[this.configuration.mLinkAddressTripLength]);

		this.addressSlider1UL.setSelection(this.configuration.mLinkAddressVoltageUL);
		this.addressText1UL.setText(sliderValues[this.configuration.mLinkAddressVoltageUL]);
		this.addressSlider2UL.setSelection(this.configuration.mLinkAddressCurrentUL);
		this.addressText2UL.setText(sliderValues[this.configuration.mLinkAddressCurrentUL]);
		this.addressSlider3UL.setSelection(this.configuration.mLinkAddressRevolutionUL);
		this.addressText3UL.setText(sliderValues[this.configuration.mLinkAddressRevolutionUL]);
		this.addressSlider4UL.setSelection(this.configuration.mLinkAddressCapacityUL);
		this.addressText4UL.setText(sliderValues[this.configuration.mLinkAddressCapacityUL]);

		this.currentButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_CURRENT_UL) > 0);
		this.currentCombo.setText(String.format(Locale.ENGLISH, "%5d", this.configuration.currentUlAlarm)); //$NON-NLS-1$
		this.voltageStartButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_START_UL) > 0);
		this.voltageStartCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.voltageStartUlAlarm / 10.0)); //$NON-NLS-1$
		this.voltageButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_UL) > 0);
		this.voltageCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.voltageUlAlarm / 10.0)); //$NON-NLS-1$
		this.capacityButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_CAPACITY_UL) > 0);
		this.capacityCombo.setText(String.format(Locale.ENGLISH, "%5d", this.configuration.capacityUlAlarm)); //$NON-NLS-1$
	}

	void initGUI() {
		try {
			this.setLayout(new FormLayout());
			this.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINEST, "GPSLoggerSetupConfiguration2.helpRequested, event=" + evt); //$NON-NLS-1$
					GPSLoggerSetupConfiguration2.this.application.openHelpDialog(Messages.getString(MessageIds.GDE_MSGT2010), "HelpInfo.html#configuration");  //$NON-NLS-1$
				}
			});
			{
				this.mLinkAddressesGroup = new Group(this, SWT.NONE);
				RowLayout mLinkAddressesGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.mLinkAddressesGroup.setLayout(mLinkAddressesGroupLayout);
				FormData mLinkAddressesGroupLData = new FormData();
				mLinkAddressesGroupLData.top = new FormAttachment(0, 1000, 10);
				mLinkAddressesGroupLData.width = 290;
				mLinkAddressesGroupLData.height = 197;
				mLinkAddressesGroupLData.left = new FormAttachment(0, 1000, 15);
				this.mLinkAddressesGroup.setLayoutData(mLinkAddressesGroupLData);
				this.mLinkAddressesGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.mLinkAddressesGroup.setText(Messages.getString(MessageIds.GDE_MSGT2058));
				{
					this.addressLabel1 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressLabel1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel1LData = new RowData();
					addressLabel1LData.width = labelWidth;
					addressLabel1LData.height = 21;
					this.addressLabel1.setLayoutData(addressLabel1LData);
					this.addressLabel1.setText(Messages.getString(MessageIds.GDE_MSGT2059));
				}
				{
					this.addressText1 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText1LData = new RowData();
					addressText1LData.width = textWidth;
					addressText1LData.height = GDE.IS_MAC ? 16 : GDE.IS_LINUX ? 10 : 13;
					this.addressText1.setLayoutData(addressText1LData);
					this.addressText1.setText(sliderValues[this.configuration.mLinkAddressVario]);
				}
				{
					this.addressSlider1 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSlider1.setMinimum(sliderMinimum);
					this.addressSlider1.setMaximum(sliderMaximum);
					this.addressSlider1.setIncrement(sliderIncrement);
					RowData addressSlider1LData = new RowData();
					addressSlider1LData.width = sliderWidth;
					addressSlider1LData.height = 17;
					this.addressSlider1.setLayoutData(addressSlider1LData);
					this.addressSlider1.setSelection(this.configuration.mLinkAddressVario);
					this.addressSlider1.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider1.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressVario = GPSLoggerSetupConfiguration2.this.addressSlider1.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText1.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressVario]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel2 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressLabel2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel2LData = new RowData();
					addressLabel2LData.width = labelWidth;
					addressLabel2LData.height = 21;
					this.addressLabel2.setLayoutData(addressLabel2LData);
					this.addressLabel2.setText(Messages.getString(MessageIds.GDE_MSGT2060));
				}
				{
					this.addressText2 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText2LData = new RowData();
					addressText2LData.width = textWidth;
					addressText2LData.height = GDE.IS_MAC ? 16 : GDE.IS_LINUX ? 10 : 13;
					this.addressText2.setLayoutData(addressText2LData);
					this.addressText2.setText(sliderValues[this.configuration.mLinkAddressSpeed]);
				}
				{
					this.addressSlider2 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSlider2.setMinimum(sliderMinimum);
					this.addressSlider2.setMaximum(sliderMaximum);
					this.addressSlider2.setIncrement(sliderIncrement);
					RowData addressSlider2LData = new RowData();
					addressSlider2LData.width = sliderWidth;
					addressSlider2LData.height = 17;
					this.addressSlider2.setLayoutData(addressSlider2LData);
					this.addressSlider2.setSelection(this.configuration.mLinkAddressSpeed);
					this.addressSlider2.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider2.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressSpeed = GPSLoggerSetupConfiguration2.this.addressSlider2.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText2.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressSpeed]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel2max = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressLabel2max.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel2maxLData = new RowData();
					addressLabel2maxLData.width = labelWidth;
					addressLabel2maxLData.height = 21;
					this.addressLabel2max.setLayoutData(addressLabel2maxLData);
					this.addressLabel2max.setText(Messages.getString(MessageIds.GDE_MSGT2060) + "_max");
				}
				{
					this.addressText2max = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText2max.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText2maxLData = new RowData();
					addressText2maxLData.width = textWidth;
					addressText2maxLData.height = GDE.IS_MAC ? 16 : GDE.IS_LINUX ? 10 : 13;
					this.addressText2max.setLayoutData(addressText2maxLData);
					this.addressText2max.setText(sliderValues[this.configuration.mLinkAddressSpeedMax]);
				}
				{
					this.addressSlider2max = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSlider2max.setMinimum(sliderMinimum);
					this.addressSlider2max.setMaximum(sliderMaximum);
					this.addressSlider2max.setIncrement(sliderIncrement);
					RowData addressSlider2maxLData = new RowData();
					addressSlider2maxLData.width = sliderWidth;
					addressSlider2maxLData.height = 17;
					this.addressSlider2max.setLayoutData(addressSlider2maxLData);
					this.addressSlider2max.setSelection(this.configuration.mLinkAddressSpeedMax);
					this.addressSlider2max.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider2max.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressSpeedMax = GPSLoggerSetupConfiguration2.this.addressSlider2max.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText2max.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressSpeedMax]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel3 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressLabel3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel3LData = new RowData();
					addressLabel3LData.width = labelWidth;
					addressLabel3LData.height = 21;
					this.addressLabel3.setLayoutData(addressLabel3LData);
					this.addressLabel3.setText(Messages.getString(MessageIds.GDE_MSGT2061));
				}
				{
					this.addressText3 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3LData = new RowData();
					addressText3LData.width = textWidth;
					addressText3LData.height = GDE.IS_MAC ? 16 : GDE.IS_LINUX ? 10 : 13;
					this.addressText3.setLayoutData(addressText3LData);
					this.addressText3.setText(sliderValues[this.configuration.mLinkAddressHeight]);
				}
				{
					this.addressSlider3 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSlider3.setMinimum(sliderMinimum);
					this.addressSlider3.setMaximum(sliderMaximum);
					this.addressSlider3.setIncrement(sliderIncrement);
					RowData addressSlider3LData = new RowData();
					addressSlider3LData.width = sliderWidth;
					addressSlider3LData.height = 17;
					this.addressSlider3.setLayoutData(addressSlider3LData);
					this.addressSlider3.setSelection(this.configuration.mLinkAddressHeight);
					this.addressSlider3.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider3.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressHeight = GPSLoggerSetupConfiguration2.this.addressSlider3.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText3.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressHeight]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel3max = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressLabel3max.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel3maxLData = new RowData();
					addressLabel3maxLData.width = labelWidth;
					addressLabel3maxLData.height = 21;
					this.addressLabel3max.setLayoutData(addressLabel3maxLData);
					this.addressLabel3max.setText(Messages.getString(MessageIds.GDE_MSGT2061) + "_max");
				}
				{
					this.addressText3max = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText3max.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3maxLData = new RowData();
					addressText3maxLData.width = textWidth;
					addressText3maxLData.height = GDE.IS_MAC ? 16 : GDE.IS_LINUX ? 10 : 13;
					this.addressText3max.setLayoutData(addressText3maxLData);
					this.addressText3max.setText(sliderValues[this.configuration.mLinkAddressHeightMax]);
				}
				{
					this.addressSlider3max = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSlider3max.setMinimum(sliderMinimum);
					this.addressSlider3max.setMaximum(sliderMaximum);
					this.addressSlider3max.setIncrement(sliderIncrement);
					RowData addressSlider3maxLData = new RowData();
					addressSlider3maxLData.width = sliderWidth;
					addressSlider3maxLData.height = 17;
					this.addressSlider3max.setLayoutData(addressSlider3maxLData);
					this.addressSlider3max.setSelection(this.configuration.mLinkAddressHeightMax);
					this.addressSlider3max.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider3max.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressHeightMax = GPSLoggerSetupConfiguration2.this.addressSlider3max.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText3max.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressHeightMax]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel4 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressLabel4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel4LData = new RowData();
					addressLabel4LData.width = labelWidth;
					addressLabel4LData.height = 21;
					this.addressLabel4.setLayoutData(addressLabel4LData);
					this.addressLabel4.setText(Messages.getString(MessageIds.GDE_MSGT2062));
				}
				{
					this.addressText4 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText4LData = new RowData();
					addressText4LData.width = textWidth;
					addressText4LData.height = GDE.IS_MAC ? 16 : GDE.IS_LINUX ? 10 : 13;
					this.addressText4.setLayoutData(addressText4LData);
					this.addressText4.setText(sliderValues[this.configuration.mLinkAddressDistance]);
				}
				{
					this.addressSlider4 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSlider4.setMinimum(sliderMinimum);
					this.addressSlider4.setMaximum(sliderMaximum);
					this.addressSlider4.setIncrement(sliderIncrement);
					RowData addressSlider4LData = new RowData();
					addressSlider4LData.width = sliderWidth;
					addressSlider4LData.height = 17;
					this.addressSlider4.setLayoutData(addressSlider4LData);
					this.addressSlider4.setSelection(this.configuration.mLinkAddressDistance);
					this.addressSlider4.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider4.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressDistance = GPSLoggerSetupConfiguration2.this.addressSlider4.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText4.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressDistance]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel5 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressLabel5.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel5LData = new RowData();
					addressLabel5LData.width = labelWidth;
					addressLabel5LData.height = 21;
					this.addressLabel5.setLayoutData(addressLabel5LData);
					this.addressLabel5.setText(Messages.getString(MessageIds.GDE_MSGT2063));
				}
				{
					this.addressText5 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText5.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText5LData = new RowData();
					addressText5LData.width = textWidth;
					addressText5LData.height = GDE.IS_MAC ? 16 : GDE.IS_LINUX ? 10 : 13;
					this.addressText5.setLayoutData(addressText5LData);
					this.addressText5.setText(sliderValues[this.configuration.mLinkAddressDirection]);
				}
				{
					this.addressSlider5 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSlider5.setMinimum(sliderMinimum);
					this.addressSlider5.setMaximum(sliderMaximum);
					this.addressSlider5.setIncrement(sliderIncrement);
					RowData addressSlider5LData = new RowData();
					addressSlider5LData.width = sliderWidth;
					addressSlider5LData.height = 17;
					this.addressSlider5.setLayoutData(addressSlider5LData);
					this.addressSlider5.setSelection(this.configuration.mLinkAddressDirection);
					this.addressSlider5.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider5.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressDirection = GPSLoggerSetupConfiguration2.this.addressSlider5.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText5.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressDirection]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel6 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressLabel6.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel6LData = new RowData();
					addressLabel6LData.width = labelWidth;
					addressLabel6LData.height = 21;
					this.addressLabel6.setLayoutData(addressLabel6LData);
					this.addressLabel6.setText(Messages.getString(MessageIds.GDE_MSGT2064));
				}
				{
					this.addressText6 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText6.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText6LData = new RowData();
					addressText6LData.width = textWidth;
					addressText6LData.height = GDE.IS_MAC ? 16 : GDE.IS_LINUX ? 10 : 13;
					this.addressText6.setLayoutData(addressText6LData);
					this.addressText6.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength]);
				}
				{
					this.addressSlider6 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSlider6.setMinimum(sliderMinimum);
					this.addressSlider6.setMaximum(sliderMaximum);
					this.addressSlider6.setIncrement(sliderIncrement);
					RowData addressSlider6LData = new RowData();
					addressSlider6LData.width = sliderWidth;
					addressSlider6LData.height = 17;
					this.addressSlider6.setLayoutData(addressSlider6LData);
					this.addressSlider6.setSelection(this.configuration.mLinkAddressTripLength);
					this.addressSlider6.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider6.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength = GPSLoggerSetupConfiguration2.this.addressSlider6.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText6.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
			}
			{
				this.unilogTelemtryAlarmsGroup = new Group(this, SWT.NONE);
				RowLayout unilogTelemtryAlarmsGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.unilogTelemtryAlarmsGroup.setLayout(unilogTelemtryAlarmsGroupLayout);
				FormData unilogTelemtryAlarmsGroupLData = new FormData();
				unilogTelemtryAlarmsGroupLData.width = 290;
				unilogTelemtryAlarmsGroupLData.height = 200;
				unilogTelemtryAlarmsGroupLData.top = new FormAttachment(0, 1000, 230);
				unilogTelemtryAlarmsGroupLData.left = new FormAttachment(0, 1000, 15);
				this.unilogTelemtryAlarmsGroup.setLayoutData(unilogTelemtryAlarmsGroupLData);
				this.unilogTelemtryAlarmsGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.unilogTelemtryAlarmsGroup.setText(Messages.getString(MessageIds.GDE_MSGT2065));
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 280;
					fillerCompositeRA1LData.height = 2;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.addressLabel1UL = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.RIGHT);
					this.addressLabel1UL.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel1ULLData = new RowData();
					addressLabel1ULLData.width = labelWidth;
					addressLabel1ULLData.height = 21;
					this.addressLabel1UL.setLayoutData(addressLabel1ULLData);
					this.addressLabel1UL.setText(Messages.getString(MessageIds.GDE_MSGT2054));
				}
				{
					this.addressText1UL = new Text(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText1UL.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText1ULLData = new RowData();
					addressText1ULLData.width = textWidth;
					addressText1ULLData.height = GDE.IS_MAC ? 16 : GDE.IS_LINUX ? 10 : 13;
					this.addressText1UL.setLayoutData(addressText1ULLData);
					this.addressText1UL.setText(sliderValues[this.configuration.mLinkAddressVoltageUL]);
				}
				{
					this.addressSlider1UL = new Slider(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					this.addressSlider1UL.setMinimum(sliderMinimum);
					this.addressSlider1UL.setMaximum(sliderMaximum);
					this.addressSlider1UL.setIncrement(sliderIncrement);
					RowData addressSlider1ULLData = new RowData();
					addressSlider1ULLData.width = sliderWidth;
					addressSlider1ULLData.height = 17;
					this.addressSlider1UL.setLayoutData(addressSlider1ULLData);
					this.addressSlider1UL.setSelection(this.configuration.mLinkAddressVoltageUL);
					this.addressSlider1UL.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider1UL.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressVoltageUL = GPSLoggerSetupConfiguration2.this.addressSlider1UL.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText1UL.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressVoltageUL]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel2UL = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.RIGHT);
					this.addressLabel2UL.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel2ULLData = new RowData();
					addressLabel2ULLData.width = labelWidth;
					addressLabel2ULLData.height = 21;
					this.addressLabel2UL.setLayoutData(addressLabel2ULLData);
					this.addressLabel2UL.setText(Messages.getString(MessageIds.GDE_MSGT2066));
				}
				{
					this.addressText2UL = new Text(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText2UL.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText2ULLData = new RowData();
					addressText2ULLData.width = textWidth;
					addressText2ULLData.height = GDE.IS_MAC ? 16 : GDE.IS_LINUX ? 10 : 13;
					this.addressText2UL.setLayoutData(addressText2ULLData);
					this.addressText2UL.setText(sliderValues[this.configuration.mLinkAddressCurrentUL]);
				}
				{
					this.addressSlider2UL = new Slider(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					this.addressSlider2UL.setMinimum(sliderMinimum);
					this.addressSlider2UL.setMaximum(sliderMaximum);
					this.addressSlider2UL.setIncrement(sliderIncrement);
					RowData addressSlider2ULLData = new RowData();
					addressSlider2ULLData.width = sliderWidth;
					addressSlider2ULLData.height = 17;
					this.addressSlider2UL.setLayoutData(addressSlider2ULLData);
					this.addressSlider2UL.setSelection(this.configuration.mLinkAddressCurrentUL);
					this.addressSlider2UL.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider2UL.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressCurrentUL = GPSLoggerSetupConfiguration2.this.addressSlider2UL.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText2UL.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressCurrentUL]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel3UL = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.RIGHT);
					this.addressLabel3UL.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel3ULLData = new RowData();
					addressLabel3ULLData.width = labelWidth;
					addressLabel3ULLData.height = 21;
					this.addressLabel3UL.setLayoutData(addressLabel3ULLData);
					this.addressLabel3UL.setText(Messages.getString(MessageIds.GDE_MSGT2067));
				}
				{
					this.addressText3UL = new Text(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText3UL.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3ULLData = new RowData();
					addressText3ULLData.width = textWidth;
					addressText3ULLData.height = GDE.IS_MAC ? 16 : GDE.IS_LINUX ? 10 : 13;
					this.addressText3UL.setLayoutData(addressText3ULLData);
					this.addressText3UL.setText(sliderValues[this.configuration.mLinkAddressRevolutionUL]);
				}
				{
					this.addressSlider3UL = new Slider(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					this.addressSlider3UL.setMinimum(sliderMinimum);
					this.addressSlider3UL.setMaximum(sliderMaximum);
					this.addressSlider3UL.setIncrement(sliderIncrement);
					RowData addressSlider3ULLData = new RowData();
					addressSlider3ULLData.width = sliderWidth;
					addressSlider3ULLData.height = 17;
					this.addressSlider3UL.setLayoutData(addressSlider3ULLData);
					this.addressSlider3UL.setSelection(this.configuration.mLinkAddressRevolutionUL);
					this.addressSlider3UL.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider3UL.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressRevolutionUL = GPSLoggerSetupConfiguration2.this.addressSlider3UL.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText3UL.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressRevolutionUL]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel4UL = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.RIGHT);
					this.addressLabel4UL.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel4ULLData = new RowData();
					addressLabel4ULLData.width = labelWidth;
					addressLabel4ULLData.height = 21;
					this.addressLabel4UL.setLayoutData(addressLabel4ULLData);
					this.addressLabel4UL.setText(Messages.getString(MessageIds.GDE_MSGT2056));
				}
				{
					this.addressText4UL = new Text(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText4UL.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText4ULLData = new RowData();
					addressText4ULLData.width = textWidth;
					addressText4ULLData.height = GDE.IS_MAC ? 16 : GDE.IS_LINUX ? 10 : 13;
					this.addressText4UL.setLayoutData(addressText4ULLData);
					this.addressText4UL.setText(sliderValues[this.configuration.mLinkAddressCapacityUL]);
				}
				{
					this.addressSlider4UL = new Slider(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					this.addressSlider4UL.setMinimum(sliderMinimum);
					this.addressSlider4UL.setMaximum(sliderMaximum);
					this.addressSlider4UL.setIncrement(sliderIncrement);
					RowData addressSlider4ULLData = new RowData();
					addressSlider4ULLData.width = sliderWidth;
					addressSlider4ULLData.height = 17;
					this.addressSlider4UL.setLayoutData(addressSlider4ULLData);
					this.addressSlider4UL.setSelection(this.configuration.mLinkAddressCapacityUL);
					this.addressSlider4UL.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider4UL.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressCapacityUL = GPSLoggerSetupConfiguration2.this.addressSlider4UL.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText4UL.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressCapacityUL]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.currentButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData currentButtonLData = new RowData();
					currentButtonLData.width = buttonWidth;
					currentButtonLData.height = 21;
					this.currentButton.setLayoutData(currentButtonLData);
					this.currentButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentButton.setText(Messages.getString(MessageIds.GDE_MSGT2066));
					this.currentButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_CURRENT_UL) > 0);
					this.currentButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "currentButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration2.this.currentButton.getSelection()) {
								GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_CURRENT_UL;
							}
							else {
								GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_CURRENT_UL;
							}
							GPSLoggerSetupConfiguration2.this.currentButton.setSelection((GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_CURRENT_UL) > 0);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData currentCComboLData = new RowData();
					currentCComboLData.width = comboWidth;
					currentCComboLData.height = 17;
					this.currentCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.currentCombo.setLayoutData(currentCComboLData);
					this.currentCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentCombo.setItems(this.currentValues);
					this.currentCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentCombo.setText(String.format(Locale.ENGLISH, "%5d", this.configuration.currentUlAlarm)); //$NON-NLS-1$
					this.currentCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "currentCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration2.this.currentCombo.getText().trim());
							GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm < 1 ? 1
									: GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm > 400 ? 400 : GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm; 
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
					this.currentCombo.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent verifyevent) {
							log.log(java.util.logging.Level.FINEST, "currentCombo.verify, event=" + verifyevent); //$NON-NLS-1$
							verifyevent.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, verifyevent.text);
						}
					});
					this.currentCombo.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent keyevent) {
							log.log(java.util.logging.Level.FINEST, "currentCombo.keyReleased, event=" + keyevent); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration2.this.currentCombo.getText().trim());
							GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm < 1 ? 1
									: GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm > 400 ? 400 : GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm; 
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.currentLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER);
					RowData currentLabelLData = new RowData();
					currentLabelLData.width = 50;
					currentLabelLData.height = 22;
					this.currentLabel.setLayoutData(currentLabelLData);
					this.currentLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentLabel.setText(Messages.getString(MessageIds.GDE_MSGT2051));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.voltageStartButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageStartButtonLData = new RowData();
					voltageStartButtonLData.width = buttonWidth;
					voltageStartButtonLData.height = 21;
					this.voltageStartButton.setLayoutData(voltageStartButtonLData);
					this.voltageStartButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageStartButton.setText(Messages.getString(MessageIds.GDE_MSGT2052));
					this.voltageStartButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_START_UL) > 0);
					this.voltageStartButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageStartButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration2.this.voltageStartButton.getSelection()) {
								GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_VOLTAGE_START_UL;
							}
							else {
								GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_VOLTAGE_START_UL;
							}
							GPSLoggerSetupConfiguration2.this.voltageStartButton.setSelection((GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_START_UL) > 0);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData voltageStartCComboLData = new RowData();
					voltageStartCComboLData.width = comboWidth;
					voltageStartCComboLData.height = 17;
					this.voltageStartCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.voltageStartCombo.setLayoutData(voltageStartCComboLData);
					this.voltageStartCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageStartCombo.setItems(this.voltageStartValues);
					this.voltageStartCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.voltageStartUlAlarm / 10.0)); //$NON-NLS-1$
					this.voltageStartCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageStartCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm = (int) (Double.parseDouble(GPSLoggerSetupConfiguration2.this.voltageStartCombo.getText().trim().replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 10);
							GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm < 10 ? 10 
									: GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm > 600 ? 600 : GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm; 
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm = (int) (Double.parseDouble(GPSLoggerSetupConfiguration2.this.voltageStartCombo.getText().trim().replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 10);
							GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm < 10 ? 10 
									: GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm > 600 ? 600 : GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm; 
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.voltageStartLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData voltageStartLabelLData = new RowData();
					voltageStartLabelLData.width = 50;
					voltageStartLabelLData.height = 22;
					this.voltageStartLabel.setLayoutData(voltageStartLabelLData);
					this.voltageStartLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageStartLabel.setText(Messages.getString(MessageIds.GDE_MSGT2053));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.voltageButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageButtonLData = new RowData();
					voltageButtonLData.width = buttonWidth;
					voltageButtonLData.height = 21;
					this.voltageButton.setLayoutData(voltageButtonLData);
					this.voltageButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageButton.setText(Messages.getString(MessageIds.GDE_MSGT2054));
					this.voltageButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_UL) > 0);
					this.voltageButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration2.this.voltageButton.getSelection()) {
								GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_VOLTAGE_UL;
							}
							else {
								GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_VOLTAGE_UL;
							}
							GPSLoggerSetupConfiguration2.this.voltageButton.setSelection((GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_UL) > 0);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData voltageCComboLData = new RowData();
					voltageCComboLData.width = comboWidth;
					voltageCComboLData.height = 17;
					this.voltageCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.voltageCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageCombo.setLayoutData(voltageCComboLData);
					this.voltageCombo.setItems(this.voltageStartValues);
					this.voltageCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.voltageUlAlarm / 10.0)); //$NON-NLS-1$
					this.voltageCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm = (int) (Double.parseDouble(GPSLoggerSetupConfiguration2.this.voltageCombo.getText().trim().replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 10);
							GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm < 10 ? 10 
									: GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm > 600 ? 600 : GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm; 
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm = (int) (Double.parseDouble(GPSLoggerSetupConfiguration2.this.voltageCombo.getText().trim().replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 10);
							GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm < 10 ? 10 
									: GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm > 600 ? 600 : GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm; 
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData voltageLabelLData = new RowData();
					voltageLabelLData.width = 50;
					voltageLabelLData.height = 22;
					this.voltageLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					this.voltageLabel.setLayoutData(voltageLabelLData);
					this.voltageLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageLabel.setText(Messages.getString(MessageIds.GDE_MSGT2055));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = fillerWidth;
					fillerCompositeRA1LData.height = 20;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.capacityButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageRxULButtonLData = new RowData();
					voltageRxULButtonLData.width = buttonWidth;
					voltageRxULButtonLData.height = 21;
					this.capacityButton.setLayoutData(voltageRxULButtonLData);
					this.capacityButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.capacityButton.setText(Messages.getString(MessageIds.GDE_MSGT2056));
					this.capacityButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_CAPACITY_UL) > 0);
					this.capacityButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "voltageRxButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (GPSLoggerSetupConfiguration2.this.capacityButton.getSelection()) {
								GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms | SetupReaderWriter.TEL_ALARM_CAPACITY_UL;
							}
							else {
								GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms = GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms ^ SetupReaderWriter.TEL_ALARM_CAPACITY_UL;
							}
							GPSLoggerSetupConfiguration2.this.capacityButton.setSelection((GPSLoggerSetupConfiguration2.this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_CAPACITY_UL) > 0);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					RowData CCombo1LData = new RowData();
					CCombo1LData.width = comboWidth;
					CCombo1LData.height = 17;
					this.capacityCombo = new CCombo(this.unilogTelemtryAlarmsGroup, SWT.BORDER);
					this.capacityCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.capacityCombo.setLayoutData(CCombo1LData);
					this.capacityCombo.setItems(this.capacityValues);
					this.capacityCombo.setText(String.format(Locale.ENGLISH, "%5d", this.configuration.capacityUlAlarm)); //$NON-NLS-1$
					this.capacityCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "capacityCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration2.this.capacityCombo.getText().trim());
							GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm < 100 ? 100 
									: GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm > 30000 ? 30000 : GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm; 
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
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
							GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration2.this.capacityCombo.getText().trim());
							GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm < 100 ? 100 
									: GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm > 30000 ? 30000 : GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm; 
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.capacityLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData voltageRxULLabelLData = new RowData();
					voltageRxULLabelLData.width = 50;
					voltageRxULLabelLData.height = 22;
					this.capacityLabel.setLayoutData(voltageRxULLabelLData);
					this.capacityLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.capacityLabel.setText(Messages.getString(MessageIds.GDE_MSGT2057));
				}
			}
			this.layout();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
