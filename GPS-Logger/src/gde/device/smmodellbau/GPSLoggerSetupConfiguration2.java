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
    
    Copyright (c) 2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
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
	
	final int								fillerWidth					= 15;
	final int								fillerHeight				= 25;
	final int								buttonWidth					= 115;
	final int								comboWidth					= 83;

	Group										mLinkAddressesGroup;
	static Group						mLinkGroupStatic;
	CLabel									addressLabel1, addressLabel2, addressLabel2max, addressLabel3, addressLabel3max, addressLabel4, addressLabel5, addressLabel6, addressLabel7, addressLabel8, addressLabel9, addressLabel10, addressLabel11, addressLabel12, addressLabel13;
	Text										addressText1, addressText2, addressText2max, addressText3, addressText3max, addressText4, addressText5, addressText6, addressText7, addressText8, addressText9, addressText10, addressText11, addressText12, addressText13;
	Slider									addressSlider1, addressSlider2, addressSlider2max, addressSlider3, addressSlider3max, addressSlider4, addressSlider5, addressSlider6, addressSlider7, addressSlider8, addressSlider9, addressSlider10, addressSlider11, addressSlider12, addressSlider13;

	Group										jetiExGroup;
	static Group						jetiExGroupStatic;
	Button									measurement1, measurement2, measurement3, measurement4, measurement5, measurement6, measurement7, measurement8, measurement9, measurement10, measurement11,
			measurement12, measurement13, measurement14, measurement15, measurement16, measurement17, measurement18, measurement19, measurement20, measurement21, measurement22, measurement23, measurement24, measurement25, measurement26, measurement27, measurement28, measurement29, measurement30;
	final String[]					jetiValueNames					= Messages.getString(MessageIds.GDE_MSGT2083).split(GDE.STRING_COMMA);
	CLabel									jetiExSelectionLabel;

	Group										unilogTelemtryAlarmsGroup;
	static Group						unilogTelemtryAlarmsGroupStatic;
	Button									currentButton, voltageStartButton, voltageButton, capacityButton;
	CCombo									currentCombo, voltageStartCombo, voltageCombo, capacityCombo;
	CLabel									currentLabel, voltageStartLabel, voltageLabel, capacityLabel, jetiUniLogAlarmsLabel;
	
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
		
		this.addressSlider6.setSelection(this.configuration.mLinkAddressFlightDirection);
		this.addressText6.setText(sliderValues[this.configuration.mLinkAddressFlightDirection]);
		this.addressSlider7.setSelection(this.configuration.mLinkAddressDirectionRel);
		this.addressText7.setText(sliderValues[this.configuration.mLinkAddressDirectionRel]);
		
		this.addressSlider8.setSelection(this.configuration.mLinkAddressTripLength);
		this.addressText8.setText(sliderValues[this.configuration.mLinkAddressTripLength]);

		this.addressSlider9.setSelection(this.configuration.mLinkAddressIntHeight);
		this.addressText9.setText(sliderValues[this.configuration.mLinkAddressIntHeight]);

		this.addressSlider10.setSelection(this.configuration.mLinkAddressIntHeight);
		this.addressText10.setText(sliderValues[this.configuration.mLinkAddressIntHeight]);
		this.addressSlider11.setSelection(this.configuration.mLinkAddressIntHeight);
		this.addressText11.setText(sliderValues[this.configuration.mLinkAddressIntHeight]);
		this.addressSlider12.setSelection(this.configuration.mLinkAddressIntHeight);
		this.addressText12.setText(sliderValues[this.configuration.mLinkAddressIntHeight]);
		this.addressSlider13.setSelection(this.configuration.mLinkAddressIntHeight);
		this.addressText13.setText(sliderValues[this.configuration.mLinkAddressIntHeight]);

//		this.addressSlider1UL.setSelection(this.configuration.mLinkAddressVoltageUL);
//		this.addressText1UL.setText(sliderValues[this.configuration.mLinkAddressVoltageUL]);
//		this.addressSlider2UL.setSelection(this.configuration.mLinkAddressCurrentUL);
//		this.addressText2UL.setText(sliderValues[this.configuration.mLinkAddressCurrentUL]);
//		this.addressSlider3UL.setSelection(this.configuration.mLinkAddressRevolutionUL);
//		this.addressText3UL.setText(sliderValues[this.configuration.mLinkAddressRevolutionUL]);
//		this.addressSlider4UL.setSelection(this.configuration.mLinkAddressCapacityUL);
//		this.addressText4UL.setText(sliderValues[this.configuration.mLinkAddressCapacityUL]);

		this.currentButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_CURRENT_UL) > 0);
		this.currentCombo.setText(String.format(Locale.ENGLISH, "%5d", this.configuration.currentUlAlarm)); //$NON-NLS-1$
		this.voltageStartButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_START_UL) > 0);
		this.voltageStartCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.voltageStartUlAlarm / 10.0)); //$NON-NLS-1$
		this.voltageButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_VOLTAGE_UL) > 0);
		this.voltageCombo.setText(String.format(Locale.ENGLISH, "%5.1f", this.configuration.voltageUlAlarm / 10.0)); //$NON-NLS-1$
		this.capacityButton.setSelection((this.configuration.telemetryAlarms & SetupReaderWriter.TEL_ALARM_CAPACITY_UL) > 0);
		this.capacityCombo.setText(String.format(Locale.ENGLISH, "%5d", this.configuration.capacityUlAlarm)); //$NON-NLS-1$
		
		this.measurement1.setSelection((this.configuration.jetiExMask  & 0x00000002) == 0);
		this.measurement2.setSelection((this.configuration.jetiExMask  & 0x00000004) == 0);
		this.measurement3.setSelection((this.configuration.jetiExMask  & 0x00000008) == 0);
		this.measurement4.setSelection((this.configuration.jetiExMask  & 0x00000010) == 0);
		this.measurement5.setSelection((this.configuration.jetiExMask  & 0x00000020) == 0);
		this.measurement6.setSelection((this.configuration.jetiExMask  & 0x00000040) == 0);
		this.measurement7.setSelection((this.configuration.jetiExMask  & 0x00000080) == 0);
		this.measurement8.setSelection((this.configuration.jetiExMask  & 0x00000100) == 0);
		this.measurement9.setSelection((this.configuration.jetiExMask  & 0x00000200) == 0);
		this.measurement10.setSelection((this.configuration.jetiExMask & 0x00000400) == 0);
		this.measurement11.setSelection((this.configuration.jetiExMask & 0x00000800) == 0);
		this.measurement12.setSelection((this.configuration.jetiExMask & 0x00001000) == 0);
		this.measurement13.setSelection((this.configuration.jetiExMask & 0x00002000) == 0);
		this.measurement14.setSelection((this.configuration.jetiExMask & 0x00004000) == 0);
		this.measurement15.setSelection((this.configuration.jetiExMask & 0x00008000) == 0);
		this.measurement16.setSelection((this.configuration.jetiExMask & 0x00010000) == 0);
		this.measurement17.setSelection((this.configuration.jetiExMask & 0x00020000) == 0);
		this.measurement18.setSelection((this.configuration.jetiExMask & 0x00040000) == 0);
		this.measurement19.setSelection((this.configuration.jetiExMask & 0x00080000) == 0);
		this.measurement20.setSelection((this.configuration.jetiExMask & 0x00100000) == 0);
		this.measurement21.setSelection((this.configuration.jetiExMask & 0x00200000) == 0);
		this.measurement22.setSelection((this.configuration.jetiExMask & 0x00400000) == 0);
		this.measurement23.setSelection((this.configuration.jetiExMask & 0x00800000) == 0);
		this.measurement24.setSelection((this.configuration.jetiExMask & 0x01000000) == 0);
		this.measurement25.setSelection((this.configuration.jetiExMask & 0x02000000) == 0);
		this.measurement26.setSelection((this.configuration.jetiExMask & 0x04000000) == 0);
		this.measurement27.setSelection((this.configuration.jetiExMask & 0x08000000) == 0);
		this.measurement28.setSelection((this.configuration.jetiExMask & 0x10000000) == 0);
		this.measurement29.setSelection((this.configuration.jetiExMask & 0x20000000) == 0);
		this.measurement30.setSelection((this.configuration.jetiExMask & 0x40000000) == 0);
		this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
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
				GPSLoggerSetupConfiguration2.mLinkGroupStatic = this.mLinkAddressesGroup;
				RowLayout mLinkAddressesGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.mLinkAddressesGroup.setLayout(mLinkAddressesGroupLayout);
				FormData mLinkAddressesGroupLData = new FormData();
				mLinkAddressesGroupLData.top = new FormAttachment(0, 1000, 2);
				mLinkAddressesGroupLData.width = 290;
				mLinkAddressesGroupLData.bottom = new FormAttachment(1000, 1000, -5);
				mLinkAddressesGroupLData.left = new FormAttachment(0, 1000, 15);
				this.mLinkAddressesGroup.setLayoutData(mLinkAddressesGroupLData);
				this.mLinkAddressesGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.mLinkAddressesGroup.setText(Messages.getString(MessageIds.GDE_MSGT2058));
				{
					this.addressLabel1 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressLabel1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel1LData = new RowData();
					addressLabel1LData.width = labelWidth;
					addressLabel1LData.height = 20;
					this.addressLabel1.setLayoutData(addressLabel1LData);
					this.addressLabel1.setText(Messages.getString(MessageIds.GDE_MSGT2059));
				}
				{
					this.addressText1 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText1LData = new RowData();
					addressText1LData.width = textWidth;
					addressText1LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressText1.setLayoutData(addressText1LData);
					this.addressText1.setText(sliderValues[this.configuration.mLinkAddressVario]);
					this.addressText1.setEditable(false);
					this.addressText1.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
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
					addressLabel2LData.height = 20;
					this.addressLabel2.setLayoutData(addressLabel2LData);
					this.addressLabel2.setText(Messages.getString(MessageIds.GDE_MSGT2060));
				}
				{
					this.addressText2 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText2LData = new RowData();
					addressText2LData.width = textWidth;
					addressText2LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressText2.setLayoutData(addressText2LData);
					this.addressText2.setText(sliderValues[this.configuration.mLinkAddressSpeed]);
					this.addressText2.setEditable(false);
					this.addressText2.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
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
					addressLabel2maxLData.height = 20;
					this.addressLabel2max.setLayoutData(addressLabel2maxLData);
					this.addressLabel2max.setText(Messages.getString(MessageIds.GDE_MSGT2060) + "_max");
				}
				{
					this.addressText2max = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText2max.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText2maxLData = new RowData();
					addressText2maxLData.width = textWidth;
					addressText2maxLData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressText2max.setLayoutData(addressText2maxLData);
					this.addressText2max.setText(sliderValues[this.configuration.mLinkAddressSpeedMax]);
					this.addressText2max.setEditable(false);
					this.addressText2max.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
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
					addressLabel3LData.height = 20;
					this.addressLabel3.setLayoutData(addressLabel3LData);
					this.addressLabel3.setText(Messages.getString(MessageIds.GDE_MSGT2061));
				}
				{
					this.addressText3 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3LData = new RowData();
					addressText3LData.width = textWidth;
					addressText3LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressText3.setLayoutData(addressText3LData);
					this.addressText3.setText(sliderValues[this.configuration.mLinkAddressHeight]);
					this.addressText3.setEditable(false);
					this.addressText3.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
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
					addressLabel3maxLData.height = 20;
					this.addressLabel3max.setLayoutData(addressLabel3maxLData);
					this.addressLabel3max.setText(Messages.getString(MessageIds.GDE_MSGT2061) + "_max");
				}
				{
					this.addressText3max = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText3max.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3maxLData = new RowData();
					addressText3maxLData.width = textWidth;
					addressText3maxLData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressText3max.setLayoutData(addressText3maxLData);
					this.addressText3max.setText(sliderValues[this.configuration.mLinkAddressHeightMax]);
					this.addressText3max.setEditable(false);
					this.addressText3max.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
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
					addressLabel4LData.height = 20;
					this.addressLabel4.setLayoutData(addressLabel4LData);
					this.addressLabel4.setText(Messages.getString(MessageIds.GDE_MSGT2062));
				}
				{
					this.addressText4 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText4LData = new RowData();
					addressText4LData.width = textWidth;
					addressText4LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressText4.setLayoutData(addressText4LData);
					this.addressText4.setText(sliderValues[this.configuration.mLinkAddressDistance]);
					this.addressText4.setEditable(false);
					this.addressText4.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
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
					addressLabel5LData.height = 20;
					this.addressLabel5.setLayoutData(addressLabel5LData);
					this.addressLabel5.setText(Messages.getString(MessageIds.GDE_MSGT2063));
				}
				{
					this.addressText5 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText5.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText5LData = new RowData();
					addressText5LData.width = textWidth;
					addressText5LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressText5.setLayoutData(addressText5LData);
					this.addressText5.setText(sliderValues[this.configuration.mLinkAddressDirection]);
					this.addressText5.setEditable(false);
					this.addressText5.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
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
					addressLabel6LData.height = 20;
					this.addressLabel6.setLayoutData(addressLabel6LData);
					this.addressLabel6.setText(Messages.getString(MessageIds.GDE_MSGT2028));
				}
				{
					this.addressText6 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText6.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText6LData = new RowData();
					addressText6LData.width = textWidth;
					addressText6LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressText6.setLayoutData(addressText6LData);
					this.addressText6.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressFlightDirection]);
					this.addressText6.setEditable(false);
					this.addressText6.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
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
					this.addressSlider6.setSelection(this.configuration.mLinkAddressFlightDirection);
					this.addressSlider6.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider6.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressFlightDirection = GPSLoggerSetupConfiguration2.this.addressSlider6.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText6.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressFlightDirection]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel7 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressLabel7.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel7LData = new RowData();
					addressLabel7LData.width = labelWidth;
					addressLabel7LData.height = 20;
					this.addressLabel7.setLayoutData(addressLabel7LData);
					this.addressLabel7.setText(Messages.getString(MessageIds.GDE_MSGT2029));
				}
				{
					this.addressText7 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText7.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText7LData = new RowData();
					addressText7LData.width = textWidth;
					addressText7LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressText7.setLayoutData(addressText7LData);
					this.addressText7.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressDirectionRel]);
					this.addressText7.setEditable(false);
					this.addressText7.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressSlider7 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSlider7.setMinimum(sliderMinimum);
					this.addressSlider7.setMaximum(sliderMaximum);
					this.addressSlider7.setIncrement(sliderIncrement);
					RowData addressSlider7LData = new RowData();
					addressSlider7LData.width = sliderWidth;
					addressSlider7LData.height = 17;
					this.addressSlider7.setLayoutData(addressSlider7LData);
					this.addressSlider7.setSelection(this.configuration.mLinkAddressDirectionRel);
					this.addressSlider7.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider7.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressDirectionRel = GPSLoggerSetupConfiguration2.this.addressSlider7.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText7.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressDirectionRel]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel8 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressLabel8.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel8LData = new RowData();
					addressLabel8LData.width = labelWidth;
					addressLabel8LData.height = 20;
					this.addressLabel8.setLayoutData(addressLabel8LData);
					this.addressLabel8.setText(Messages.getString(MessageIds.GDE_MSGT2064));
				}
				{
					this.addressText8 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText8.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText8LData = new RowData();
					addressText8LData.width = textWidth;
					addressText8LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressText8.setLayoutData(addressText8LData);
					this.addressText8.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength]);
					this.addressText8.setEditable(false);
					this.addressText8.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressSlider8 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSlider8.setMinimum(sliderMinimum);
					this.addressSlider8.setMaximum(sliderMaximum);
					this.addressSlider8.setIncrement(sliderIncrement);
					RowData addressSlider8LData = new RowData();
					addressSlider8LData.width = sliderWidth;
					addressSlider8LData.height = 17;
					this.addressSlider8.setLayoutData(addressSlider8LData);
					this.addressSlider8.setSelection(this.configuration.mLinkAddressTripLength);
					this.addressSlider8.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider8.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength = GPSLoggerSetupConfiguration2.this.addressSlider8.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText8.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel9 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressLabel9.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel9LData = new RowData();
					addressLabel9LData.width = labelWidth;
					addressLabel9LData.height = 20;
					this.addressLabel9.setLayoutData(addressLabel9LData);
					this.addressLabel9.setText(Messages.getString(MessageIds.GDE_MSGT2076));
				}
				{
					this.addressText9 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText9.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText9LData = new RowData();
					addressText9LData.width = textWidth;
					addressText9LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressText9.setLayoutData(addressText9LData);
					this.addressText9.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressIntHeight]);
					this.addressText9.setEditable(false);
					this.addressText9.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressSlider9 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSlider9.setMinimum(sliderMinimum);
					this.addressSlider9.setMaximum(sliderMaximum);
					this.addressSlider9.setIncrement(sliderIncrement);
					RowData addressSlider9LData = new RowData();
					addressSlider9LData.width = sliderWidth;
					addressSlider9LData.height = 17;
					this.addressSlider9.setLayoutData(addressSlider9LData);
					this.addressSlider9.setSelection(this.configuration.mLinkAddressTripLength);
					this.addressSlider9.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider9.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength = GPSLoggerSetupConfiguration2.this.addressSlider9.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText9.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel10 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressLabel10.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel10LData = new RowData();
					addressLabel10LData.width = labelWidth;
					addressLabel10LData.height = 20;
					this.addressLabel10.setLayoutData(addressLabel10LData);
					this.addressLabel10.setText(Messages.getString(MessageIds.GDE_MSGT2084));
				}
				{
					this.addressText10 =new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText10.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText10LData = new RowData();
					addressText10LData.width = textWidth;
					addressText10LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressText10.setLayoutData(addressText10LData);
					this.addressText10.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressIntHeight]);
					this.addressText10.setEditable(false);
					this.addressText10.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressSlider10 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSlider10.setMinimum(sliderMinimum);
					this.addressSlider10.setMaximum(sliderMaximum);
					this.addressSlider10.setIncrement(sliderIncrement);
					RowData addressSlider10LData = new RowData();
					addressSlider10LData.width = sliderWidth;
					addressSlider10LData.height = 17;
					this.addressSlider10.setLayoutData(addressSlider10LData);
					this.addressSlider10.setSelection(this.configuration.mLinkAddressTripLength);
					this.addressSlider10.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider10.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength = GPSLoggerSetupConfiguration2.this.addressSlider10.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText10.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel11 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressLabel11.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel11LData = new RowData();
					addressLabel11LData.width = labelWidth;
					addressLabel11LData.height = 20;
					this.addressLabel11.setLayoutData(addressLabel11LData);
					this.addressLabel11.setText(Messages.getString(MessageIds.GDE_MSGT2085));
				}
				{
					this.addressText11 =new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText11.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText11LData = new RowData();
					addressText11LData.width = textWidth;
					addressText11LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressText11.setLayoutData(addressText11LData);
					this.addressText11.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressIntHeight]);
					this.addressText11.setEditable(false);
					this.addressText11.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressSlider11 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSlider11.setMinimum(sliderMinimum);
					this.addressSlider11.setMaximum(sliderMaximum);
					this.addressSlider11.setIncrement(sliderIncrement);
					RowData addressSlider11LData = new RowData();
					addressSlider11LData.width = sliderWidth;
					addressSlider11LData.height = 17;
					this.addressSlider11.setLayoutData(addressSlider11LData);
					this.addressSlider11.setSelection(this.configuration.mLinkAddressTripLength);
					this.addressSlider11.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider11.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength = GPSLoggerSetupConfiguration2.this.addressSlider11.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText11.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel12 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressLabel12.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel12LData = new RowData();
					addressLabel12LData.width = labelWidth;
					addressLabel12LData.height = 20;
					this.addressLabel12.setLayoutData(addressLabel12LData);
					this.addressLabel12.setText(Messages.getString(MessageIds.GDE_MSGT2086));
				}
				{
					this.addressText12 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText12.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText12LData = new RowData();
					addressText12LData.width = textWidth;
					addressText12LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressText12.setLayoutData(addressText12LData);
					this.addressText12.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressIntHeight]);
					this.addressText12.setEditable(false);
					this.addressText12.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressSlider12 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSlider12.setMinimum(sliderMinimum);
					this.addressSlider12.setMaximum(sliderMaximum);
					this.addressSlider12.setIncrement(sliderIncrement);
					RowData addressSlider12LData = new RowData();
					addressSlider12LData.width = sliderWidth;
					addressSlider12LData.height = 17;
					this.addressSlider12.setLayoutData(addressSlider12LData);
					this.addressSlider12.setSelection(this.configuration.mLinkAddressTripLength);
					this.addressSlider12.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider12.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength = GPSLoggerSetupConfiguration2.this.addressSlider12.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText12.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.addressLabel13 = new CLabel(this.mLinkAddressesGroup, SWT.RIGHT);
					this.addressLabel13.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel13LData = new RowData();
					addressLabel13LData.width = labelWidth;
					addressLabel13LData.height = 20;
					this.addressLabel13.setLayoutData(addressLabel13LData);
					this.addressLabel13.setText(Messages.getString(MessageIds.GDE_MSGT2087));
				}
				{
					this.addressText13 = new Text(this.mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					this.addressText13.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText13LData = new RowData();
					addressText13LData.width = textWidth;
					addressText13LData.height = GDE.IS_MAC ? 14 : GDE.IS_LINUX ? 10 : 13;
					this.addressText13.setLayoutData(addressText13LData);
					this.addressText13.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressIntHeight]);
					this.addressText13.setEditable(false);
					this.addressText13.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				}
				{
					this.addressSlider13 = new Slider(this.mLinkAddressesGroup, SWT.NONE);
					this.addressSlider13.setMinimum(sliderMinimum);
					this.addressSlider13.setMaximum(sliderMaximum);
					this.addressSlider13.setIncrement(sliderIncrement);
					RowData addressSlider13LData = new RowData();
					addressSlider13LData.width = sliderWidth;
					addressSlider13LData.height = 17;
					this.addressSlider13.setLayoutData(addressSlider13LData);
					this.addressSlider13.setSelection(this.configuration.mLinkAddressTripLength);
					this.addressSlider13.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider13.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength = GPSLoggerSetupConfiguration2.this.addressSlider13.getSelection();
							GPSLoggerSetupConfiguration2.this.addressText13.setText(sliderValues[GPSLoggerSetupConfiguration2.this.configuration.mLinkAddressTripLength]);
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
			}
			{
				this.jetiExGroup = new Group(this, SWT.NONE);
				GPSLoggerSetupConfiguration2.jetiExGroupStatic = this.jetiExGroup;
				RowLayout mLinkAddressesGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.jetiExGroup.setLayout(mLinkAddressesGroupLayout);
				FormData mLinkAddressesGroupLData = new FormData();
				mLinkAddressesGroupLData.top = new FormAttachment(0, 1000, 5);
				mLinkAddressesGroupLData.left = new FormAttachment(0, 1000, 15);
				mLinkAddressesGroupLData.width = 290;
				mLinkAddressesGroupLData.height = GDE.IS_MAC ? 355 : 360;
				this.jetiExGroup.setLayoutData(mLinkAddressesGroupLData);
				this.jetiExGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.jetiExGroup.setText("Jeti EX ");
				{
					this.fillerComposite = new Composite(this.mLinkAddressesGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 280;
					fillerCompositeRA1LData.height = 2;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.measurement1 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement1.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement1.setText(this.jetiValueNames[1]);
					this.measurement1.setEnabled(false);
					this.measurement1.setSelection(true);
					//this.measurement1.addSelectionListener(new SelectionAdapter() {
					//	@Override
					//	public void widgetSelected(SelectionEvent evt) {
					//		log.log(Level.FINEST, "measurement1.widgetSelected, event=" + evt); //$NON-NLS-1$
					//		GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = measurement1.getSelection() ? configuration.jetiExMask & 0xFFFFFFFD : configuration.jetiExMask + 0x00000002;
					//		GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
					//	}
					//});
				}
				{
					this.measurement2 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement2.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement2.setText(this.jetiValueNames[2]);
					this.measurement2.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement2.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement2.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFFFB
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000004;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement3 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement3.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement3.setText(this.jetiValueNames[3]);
					this.measurement3.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement3.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement3.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFFF7
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000008;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement4 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement4.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement4.setText(this.jetiValueNames[4]);
					this.measurement4.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement4.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement4.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFFEF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000010;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				//Höhe, Vario, Drehzahl, Energie, Leistung, Lufdruck, Impuls Ein, Impuls Aus, Zelle Min, Zelle Min#, A1, A2, A3, Temp int, Höhengeweinn
				{
					this.measurement5 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement5.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement5.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement5.setText(this.jetiValueNames[5]);
					this.measurement5.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement5.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement5.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFFDF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000020;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement6 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement6.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement6.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement6.setText(this.jetiValueNames[6]);
					this.measurement6.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement6.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement6.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFFBF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000040;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement7 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement7.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement7.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement7.setText(this.jetiValueNames[7]);
					this.measurement7.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement7.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement7.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFF7F
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000080;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement8 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement8.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement8.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement8.setText(this.jetiValueNames[8]);
					this.measurement8.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement8.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement8.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFEFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000100;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement9 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement9.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement9.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement9.setText(this.jetiValueNames[9]);
					this.measurement9.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement9.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement9.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFDFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000200;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement10 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement10.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement10.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement10.setText(this.jetiValueNames[10]);
					this.measurement10.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement10.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement10.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFFBFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000400;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement11 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement11.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement11.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement11.setText(this.jetiValueNames[11]);
					this.measurement11.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement11.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement11.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFF7FF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00000800;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement12 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement12.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement12.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement12.setText(this.jetiValueNames[12]);
					this.measurement12.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement12.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement12.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFEFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00001000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement13 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement13.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement13.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement13.setText(this.jetiValueNames[13]);
					this.measurement13.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement13.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement13.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFDFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00002000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement14 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement14.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement14.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement14.setText(this.jetiValueNames[14]);
					this.measurement14.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement14.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement14.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFFBFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00004000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement15 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement15.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement15.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement15.setText(this.jetiValueNames[15]);
					this.measurement15.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement15.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement15.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFF7FFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00008000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement16 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement16.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement16.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement16.setText(this.jetiValueNames[16]);
					this.measurement16.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement16.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement16.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFEFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00010000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement17 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement17.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement17.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement17.setText(this.jetiValueNames[17]);
					this.measurement17.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement17.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement17.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFDFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00020000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement18 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement18.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement18.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement18.setText(this.jetiValueNames[18]);
					this.measurement18.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement18.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement18.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFFBFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00040000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement19 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement19.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement19.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement19.setText(this.jetiValueNames[19]);
					this.measurement19.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement19.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement19.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFF7FFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00080000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement20 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement20.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement20.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement20.setText(this.jetiValueNames[20]);
					this.measurement20.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement20.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement20.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFEFFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00100000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement21 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement21.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement21.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement21.setText(this.jetiValueNames[21]);
					this.measurement21.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement21.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement21.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFDFFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00200000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement22 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement22.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement22.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement22.setText(this.jetiValueNames[22]);
					this.measurement22.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement22.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement22.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFFBFFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00400000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement23 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement23.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement23.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement23.setText(this.jetiValueNames[23]);
					this.measurement23.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement23.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement23.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFF7FFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x00800000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement24 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement24.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement24.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement24.setText(this.jetiValueNames[24]);
					this.measurement24.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement24.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement24.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFEFFFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x01000000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement25 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement25.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement25.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement25.setText(this.jetiValueNames[25]);
					this.measurement25.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement25.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement25.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFDFFFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x02000000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement26 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement26.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement26.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement26.setText(this.jetiValueNames[26]);
					this.measurement26.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement26.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement26.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xFBFFFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x04000000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement27 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement27.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement27.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement27.setText(this.jetiValueNames[27]);
					this.measurement27.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement27.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement27.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xF7FFFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x08000000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement28 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement28.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement28.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement28.setText(this.jetiValueNames[28]);
					this.measurement28.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement28.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement28.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xEFFFFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x10000000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement29 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement29.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement29.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement29.setText(this.jetiValueNames[29]);
					this.measurement29.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement29.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement29.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xDFFFFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x20000000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.measurement30 = new Button(this.jetiExGroup, SWT.CHECK | SWT.LEFT);
					RowData analogAlarm1DirectionButtonLData = new RowData();
					analogAlarm1DirectionButtonLData.width = 142;
					analogAlarm1DirectionButtonLData.height = 19;
					this.measurement30.setLayoutData(analogAlarm1DirectionButtonLData);
					this.measurement30.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.measurement30.setText(this.jetiValueNames[30]);
					this.measurement30.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							GPSLoggerSetupConfiguration2.log.log(java.util.logging.Level.FINEST, "measurement30.widgetSelected, event=" + evt); //$NON-NLS-1$
							GPSLoggerSetupConfiguration2.this.configuration.jetiExMask = GPSLoggerSetupConfiguration2.this.measurement30.getSelection() ? GPSLoggerSetupConfiguration2.this.configuration.jetiExMask & 0xBFFFFFFF
									: GPSLoggerSetupConfiguration2.this.configuration.jetiExMask + 0x40000000;
							GPSLoggerSetupConfiguration2.this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
							GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
						}
					});
				}
				{
					this.jetiExSelectionLabel = new CLabel(this.jetiExGroup, SWT.CENTER);
					this.jetiExSelectionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData jetiExSelectionLData = new RowData();
					jetiExSelectionLData.width = 285;
					jetiExSelectionLData.height = 25;
					this.jetiExSelectionLabel.setLayoutData(jetiExSelectionLData);
					this.jetiExSelectionLabel.setText(Messages.getString(MessageIds.GDE_MSGT2088, new Object[] {GPSLoggerSetupConfiguration2.this.configuration.getJetiMeasurementCount()}));
				}
			}
			{
				this.unilogTelemtryAlarmsGroup = new Group(this, SWT.NONE);
				GPSLoggerSetupConfiguration2.unilogTelemtryAlarmsGroupStatic = this.unilogTelemtryAlarmsGroup;
				RowLayout unilogTelemtryAlarmsGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.unilogTelemtryAlarmsGroup.setLayout(unilogTelemtryAlarmsGroupLayout);
				FormData unilogTelemtryAlarmsGroupLData = new FormData();
				unilogTelemtryAlarmsGroupLData.width = 290;
				unilogTelemtryAlarmsGroupLData.height = 143;
				unilogTelemtryAlarmsGroupLData.top = new FormAttachment(0, 1000, 390);
				unilogTelemtryAlarmsGroupLData.left = new FormAttachment(0, 1000, 15);
				this.unilogTelemtryAlarmsGroup.setLayoutData(unilogTelemtryAlarmsGroupLData);
				this.unilogTelemtryAlarmsGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.unilogTelemtryAlarmsGroup.setText(Messages.getString(MessageIds.GDE_MSGT2065));
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = fillerWidth;
					fillerCompositeRA1LData.height = fillerHeight;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.currentButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData currentButtonLData = new RowData();
					currentButtonLData.width = buttonWidth;
					currentButtonLData.height = 20;
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
							try {
								GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration2.this.currentCombo.getText().trim());
								GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm < 1 ? 1
										: GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm > 400 ? 400 : GPSLoggerSetupConfiguration2.this.configuration.currentUlAlarm; 
								GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.currentLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER);
					RowData currentLabelLData = new RowData();
					currentLabelLData.width = 50;
					currentLabelLData.height = 20;
					this.currentLabel.setLayoutData(currentLabelLData);
					this.currentLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.currentLabel.setText(Messages.getString(MessageIds.GDE_MSGT2051));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = fillerWidth;
					fillerCompositeRA1LData.height = fillerHeight;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.voltageStartButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageStartButtonLData = new RowData();
					voltageStartButtonLData.width = buttonWidth;
					voltageStartButtonLData.height = 20;
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
							try {
								GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm = (int) (Double.parseDouble(GPSLoggerSetupConfiguration2.this.voltageStartCombo.getText().trim().replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 10);
								GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm < 10 ? 10 
										: GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm > 600 ? 600 : GPSLoggerSetupConfiguration2.this.configuration.voltageStartUlAlarm; 
								GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					this.voltageStartLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData voltageStartLabelLData = new RowData();
					voltageStartLabelLData.width = 50;
					voltageStartLabelLData.height = 20;
					this.voltageStartLabel.setLayoutData(voltageStartLabelLData);
					this.voltageStartLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageStartLabel.setText(Messages.getString(MessageIds.GDE_MSGT2053));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = fillerWidth;
					fillerCompositeRA1LData.height = fillerHeight;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.voltageButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageButtonLData = new RowData();
					voltageButtonLData.width = buttonWidth;
					voltageButtonLData.height = 20;
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
							try {
								GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm = (int) (Double.parseDouble(GPSLoggerSetupConfiguration2.this.voltageCombo.getText().trim().replace(GDE.STRING_COMMA, GDE.STRING_DOT)) * 10);
								GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm < 10 ? 10 
										: GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm > 600 ? 600 : GPSLoggerSetupConfiguration2.this.configuration.voltageUlAlarm; 
								GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// ignore illegal char
							}
						}
					});
				}
				{
					RowData voltageLabelLData = new RowData();
					voltageLabelLData.width = 50;
					voltageLabelLData.height = 20;
					this.voltageLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					this.voltageLabel.setLayoutData(voltageLabelLData);
					this.voltageLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.voltageLabel.setText(Messages.getString(MessageIds.GDE_MSGT2055));
				}
				{
					this.fillerComposite = new Composite(this.unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = fillerWidth;
					fillerCompositeRA1LData.height = fillerHeight;
					this.fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					this.capacityButton = new Button(this.unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageRxULButtonLData = new RowData();
					voltageRxULButtonLData.width = buttonWidth;
					voltageRxULButtonLData.height = 20;
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
							try {
								GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm = (short) Integer.parseInt(GPSLoggerSetupConfiguration2.this.capacityCombo.getText().trim());
								GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm = GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm < 100 ? 100 
										: GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm > 30000 ? 30000 : GPSLoggerSetupConfiguration2.this.configuration.capacityUlAlarm; 
								GPSLoggerSetupConfiguration2.this.dialog.enableSaveConfigurationButton(true);
							}
							catch (NumberFormatException e) {
								// illegal char
							}
						}
					});
				}
				{
					this.capacityLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData voltageRxULLabelLData = new RowData();
					voltageRxULLabelLData.width = 50;
					voltageRxULLabelLData.height = 20;
					this.capacityLabel.setLayoutData(voltageRxULLabelLData);
					this.capacityLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.capacityLabel.setText(Messages.getString(MessageIds.GDE_MSGT2057));
				}
				{
					this.jetiUniLogAlarmsLabel = new CLabel(this.unilogTelemtryAlarmsGroup, SWT.CENTER);
					this.jetiUniLogAlarmsLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData jetiUniLogAlarmsLData = new RowData();
					jetiUniLogAlarmsLData.width = 285;
					jetiUniLogAlarmsLData.height = 30;
					this.jetiUniLogAlarmsLabel.setLayoutData(jetiUniLogAlarmsLData);
					this.jetiUniLogAlarmsLabel.setText(Messages.getString(MessageIds.GDE_MSGT2089));
				}
			}
			this.layout();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
