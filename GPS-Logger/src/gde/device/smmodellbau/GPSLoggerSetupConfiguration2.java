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
import gde.ui.SWTResourceManager;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
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

public class GPSLoggerSetupConfiguration2 extends org.eclipse.swt.widgets.Composite {
	final static Logger	log	= Logger.getLogger(GPSLoggerSetupConfiguration2.class.getName());

	Composite						fillerComposite;

	Group								unilogTelemtryAlarmsGroup;
	Button							currentButton, voltageStartButton, voltageButton, distanceButton, voltageRxButton;
	CCombo							currentCombo, voltageStartCombo, voltageCombo, distanceCombo, voltageRxCombo;
	CLabel							currentLabel, voltageStartLabel, voltageLabel, distanceLabel, voltageRxLabel;

	Group								mLinkAddressesGroup;
	CLabel							addressLabel1, addressLabel2, addressLabel3, addressLabel4, addressLabel5, addressLabel6;
	Text								addressText1, addressText2, addressText3, addressText4, addressText5, addressText6;
	Slider							addressSlider1, addressSlider2, addressSlider3, addressSlider4, addressSlider5, addressSlider6;
	int									addressValue1	= 3, addressValue2 = 2, addressValue3 = 5, addressValue4 = 4, addressValue5 = 6, addressValue6 = 7;

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
		GPSLoggerSetupConfiguration2 inst = new GPSLoggerSetupConfiguration2(shell, SWT.NULL);
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
		inst.setSize(350, 482);
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
	}

	public GPSLoggerSetupConfiguration2(org.eclipse.swt.widgets.Composite parent, int style) {
		super(parent, style);
		SWTResourceManager.registerResourceUser(this);
		initGUI();
	}

	void initGUI() {
		try {
			this.setLayout(new FormLayout());
			//this.setSize(350, 451);
			{
				unilogTelemtryAlarmsGroup = new Group(this, SWT.NONE);
				RowLayout unilogTelemtryAlarmsGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				unilogTelemtryAlarmsGroup.setLayout(unilogTelemtryAlarmsGroupLayout);
				FormData unilogTelemtryAlarmsGroupLData = new FormData();
				unilogTelemtryAlarmsGroupLData.width = 310;
				unilogTelemtryAlarmsGroupLData.height = 159;
				unilogTelemtryAlarmsGroupLData.top =  new FormAttachment(0, 1000, 12);
				unilogTelemtryAlarmsGroupLData.left =  new FormAttachment(0, 1000, 15);
				unilogTelemtryAlarmsGroup.setLayoutData(unilogTelemtryAlarmsGroupLData);
				unilogTelemtryAlarmsGroup.setText("UniLog Telemetry Alarms");
				unilogTelemtryAlarmsGroup.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						System.out.println("unilogTelemtryAlarmsGroup.helpRequested, event="+evt);
						//TODO add your code for unilogTelemtryAlarmsGroup.helpRequested
					}
				});
				{
					fillerComposite = new Composite(unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 299;
					fillerCompositeRA1LData.height = 15;
					fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					fillerComposite = new Composite(unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 20;
					fillerCompositeRA1LData.height = 20;
					fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					currentButton = new Button(unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData currentButtonLData = new RowData();
					currentButtonLData.width = 134;
					currentButtonLData.height = 21;
					currentButton.setLayoutData(currentButtonLData);
					currentButton.setText("current");
					currentButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("currentButton.widgetSelected, event="+evt);
							//TODO add your code for currentButton.widgetSelected
						}
					});
				}
				{
					RowData currentCComboLData = new RowData();
					currentCComboLData.width = 83;
					currentCComboLData.height = 17;
					currentCombo = new CCombo(unilogTelemtryAlarmsGroup, SWT.BORDER);
					currentCombo.setLayoutData(currentCComboLData);
					currentCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("currentCCombo.widgetSelected, event="+evt);
							//TODO add your code for currentCCombo.widgetSelected
						}
					});
				}
				{
					currentLabel = new CLabel(unilogTelemtryAlarmsGroup, SWT.CENTER);
					RowData currentLabelLData = new RowData();
					currentLabelLData.width = 50;
					currentLabelLData.height = 22;
					currentLabel.setLayoutData(currentLabelLData);
					currentLabel.setText("A");
				}
				{
					fillerComposite = new Composite(unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 20;
					fillerCompositeRA1LData.height = 20;
					fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					voltageStartButton = new Button(unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageStartButtonLData = new RowData();
					voltageStartButtonLData.width = 134;
					voltageStartButtonLData.height = 21;
					voltageStartButton.setLayoutData(voltageStartButtonLData);
					voltageStartButton.setText("voltage start");
					voltageStartButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("voltageStartButton.widgetSelected, event="+evt);
							//TODO add your code for voltageStartButton.widgetSelected
						}
					});
				}
				{
					RowData voltageStartCComboLData = new RowData();
					voltageStartCComboLData.width = 83;
					voltageStartCComboLData.height = 17;
					voltageStartCombo = new CCombo(unilogTelemtryAlarmsGroup, SWT.BORDER);
					voltageStartCombo.setLayoutData(voltageStartCComboLData);
					voltageStartCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("voltageStartCCombo.widgetSelected, event="+evt);
							//TODO add your code for voltageStartCCombo.widgetSelected
						}
					});
				}
				{
					voltageStartLabel = new CLabel(unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData voltageStartLabelLData = new RowData();
					voltageStartLabelLData.width = 50;
					voltageStartLabelLData.height = 22;
					voltageStartLabel.setLayoutData(voltageStartLabelLData);
					voltageStartLabel.setText("V");
				}
				{
					fillerComposite = new Composite(unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 20;
					fillerCompositeRA1LData.height = 20;
					fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					voltageButton = new Button(unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageButtonLData = new RowData();
					voltageButtonLData.width = 134;
					voltageButtonLData.height = 21;
					voltageButton.setLayoutData(voltageButtonLData);
					voltageButton.setText("voltage");
					voltageButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("voltageButton.widgetSelected, event="+evt);
							//TODO add your code for voltageButton.widgetSelected
						}
					});
				}
				{
					RowData voltageCComboLData = new RowData();
					voltageCComboLData.width = 83;
					voltageCComboLData.height = 17;
					voltageCombo = new CCombo(unilogTelemtryAlarmsGroup, SWT.BORDER);
					voltageCombo.setLayoutData(voltageCComboLData);
					voltageCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("voltageCCombo.widgetSelected, event="+evt);
							//TODO add your code for voltageCCombo.widgetSelected
						}
					});
				}
				{
					RowData voltageLabelLData = new RowData();
					voltageLabelLData.width = 50;
					voltageLabelLData.height = 22;
					voltageLabel = new CLabel(unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					voltageLabel.setLayoutData(voltageLabelLData);
					voltageLabel.setText("V");
				}
				{
					fillerComposite = new Composite(unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 20;
					fillerCompositeRA1LData.height = 20;
					fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					distanceButton = new Button(unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData distanceULButtonLData = new RowData();
					distanceULButtonLData.width = 134;
					distanceULButtonLData.height = 21;
					distanceButton.setLayoutData(distanceULButtonLData);
					distanceButton.setText("distance");
					distanceButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("distanceULButton.widgetSelected, event="+evt);
							//TODO add your code for distanceULButton.widgetSelected
						}
					});
				}
				{
					RowData distanceULCComboLData = new RowData();
					distanceULCComboLData.width = 83;
					distanceULCComboLData.height = 17;
					distanceCombo = new CCombo(unilogTelemtryAlarmsGroup, SWT.BORDER);
					distanceCombo.setLayoutData(distanceULCComboLData);
					distanceCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("distanceULCCombo.widgetSelected, event="+evt);
							//TODO add your code for distanceULCCombo.widgetSelected
						}
					});
				}
				{
					distanceLabel = new CLabel(unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData distanceULLabelLData = new RowData();
					distanceULLabelLData.width = 50;
					distanceULLabelLData.height = 22;
					distanceLabel.setLayoutData(distanceULLabelLData);
					distanceLabel.setText("m");
				}
				{
					fillerComposite = new Composite(unilogTelemtryAlarmsGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 20;
					fillerCompositeRA1LData.height = 20;
					fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					voltageRxButton = new Button(unilogTelemtryAlarmsGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageRxULButtonLData = new RowData();
					voltageRxULButtonLData.width = 134;
					voltageRxULButtonLData.height = 21;
					voltageRxButton.setLayoutData(voltageRxULButtonLData);
					voltageRxButton.setText("voltage Rx");
					voltageRxButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("voltageRxULButton.widgetSelected, event="+evt);
							//TODO add your code for voltageRxULButton.widgetSelected
						}
					});
				}
				{
					RowData CCombo1LData = new RowData();
					CCombo1LData.width = 83;
					CCombo1LData.height = 17;
					voltageRxCombo = new CCombo(unilogTelemtryAlarmsGroup, SWT.BORDER);
					voltageRxCombo.setLayoutData(CCombo1LData);
					voltageRxCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("CCombo1.widgetSelected, event="+evt);
							//TODO add your code for CCombo1.widgetSelected
						}
					});
				}
				{
					voltageRxLabel = new CLabel(unilogTelemtryAlarmsGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData voltageRxULLabelLData = new RowData();
					voltageRxULLabelLData.width = 50;
					voltageRxULLabelLData.height = 22;
					voltageRxLabel.setLayoutData(voltageRxULLabelLData);
					voltageRxLabel.setText("V");
				}
			}
			{
				mLinkAddressesGroup = new Group(this, SWT.NONE);
				RowLayout mLinkAddressesGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				mLinkAddressesGroup.setLayout(mLinkAddressesGroupLayout);
				FormData mLinkAddressesGroupLData = new FormData();
				mLinkAddressesGroupLData.top =  new FormAttachment(0, 1000, 230);
				mLinkAddressesGroupLData.width = 310;
				mLinkAddressesGroupLData.height = 180;
				mLinkAddressesGroupLData.left =  new FormAttachment(0, 1000, 15);
				mLinkAddressesGroup.setLayoutData(mLinkAddressesGroupLData);
				mLinkAddressesGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				mLinkAddressesGroup.setText("M-Link Addresses");
				mLinkAddressesGroup.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						System.out.println("mLinkAddressesGroup.helpRequested, event="+evt);
						//TODO add your code for mLinkAddressesGroup.helpRequested
					}
				});
				{
					fillerComposite = new Composite(mLinkAddressesGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 296;
					fillerCompositeRALData.height = 13;
					fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					addressLabel1 = new CLabel(mLinkAddressesGroup, SWT.RIGHT);
					addressLabel1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel1LData = new RowData();
					addressLabel1LData.width = 91;
					addressLabel1LData.height = 21;
					addressLabel1.setLayoutData(addressLabel1LData);
					addressLabel1.setText("vario");
				}
				{
					addressText1 = new Text(mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					addressText1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText1LData = new RowData();
					addressText1LData.width = 41;
					addressText1LData.height = 13;
					addressText1.setLayoutData(addressText1LData);
					addressText1.setText(GDE.STRING_EMPTY + addressValue1);
				}
				{
					addressSlider1 = new Slider(mLinkAddressesGroup, SWT.NONE);
					addressSlider1.setMinimum(1);
					addressSlider1.setMaximum(26);
					addressSlider1.setIncrement(1);
					RowData addressSlider1LData = new RowData();
					addressSlider1LData.width = 134;
					addressSlider1LData.height = 17;
					addressSlider1.setLayoutData(addressSlider1LData);
					addressSlider1.setSelection(addressValue1);
					addressSlider1.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider1.widgetSelected, event=" + evt); //$NON-NLS-1$
							addressValue1 = addressSlider1.getSelection();
							addressText1.setText(GDE.STRING_EMPTY + addressValue1);
							//TODO writeTelemtryConfigurationButton.setEnabled(true);
						}
					});
				}
				{
					addressLabel2 = new CLabel(mLinkAddressesGroup, SWT.RIGHT);
					addressLabel2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel2LData = new RowData();
					addressLabel2LData.width = 91;
					addressLabel2LData.height = 21;
					addressLabel2.setLayoutData(addressLabel2LData);
					addressLabel2.setText("velocity");
				}
				{
					addressText2 = new Text(mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					addressText2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText2LData = new RowData();
					addressText2LData.width = 41;
					addressText2LData.height = 13;
					addressText2.setLayoutData(addressText2LData);
					addressText2.setText(GDE.STRING_EMPTY + addressValue2);
				}
				{
					addressSlider2 = new Slider(mLinkAddressesGroup, SWT.NONE);
					addressSlider2.setMinimum(1);
					addressSlider2.setMaximum(26);
					addressSlider2.setIncrement(1);
					RowData addressSlider2LData = new RowData();
					addressSlider2LData.width = 134;
					addressSlider2LData.height = 17;
					addressSlider2.setLayoutData(addressSlider2LData);
					addressSlider2.setSelection(addressValue2);
					addressSlider2.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider2.widgetSelected, event=" + evt); //$NON-NLS-1$
							addressValue2 = addressSlider2.getSelection();
							addressText2.setText(GDE.STRING_EMPTY + addressValue2);
							//TODO writeTelemtryConfigurationButton.setEnabled(true);
						}
					});
				}
				{
					addressLabel3 = new CLabel(mLinkAddressesGroup, SWT.RIGHT);
					addressLabel3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel3LData = new RowData();
					addressLabel3LData.width = 91;
					addressLabel3LData.height = 21;
					addressLabel3.setLayoutData(addressLabel3LData);
					addressLabel3.setText("direction");
				}
				{
					addressText3 = new Text(mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					addressText3.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText3LData = new RowData();
					addressText3LData.width = 41;
					addressText3LData.height = 13;
					addressText3.setLayoutData(addressText3LData);
					addressText3.setText(GDE.STRING_EMPTY + addressValue3);
				}
				{
					addressSlider3 = new Slider(mLinkAddressesGroup, SWT.NONE);
					addressSlider3.setMinimum(1);
					addressSlider3.setMaximum(26);
					addressSlider3.setIncrement(1);
					RowData addressSlider3LData = new RowData();
					addressSlider3LData.width = 134;
					addressSlider3LData.height = 17;
					addressSlider3.setLayoutData(addressSlider3LData);
					addressSlider3.setSelection(addressValue3);
					addressSlider3.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider3.widgetSelected, event=" + evt); //$NON-NLS-1$
							addressValue3 = addressSlider3.getSelection();
							addressText3.setText(GDE.STRING_EMPTY + addressValue3);
							//TODO writeTelemtryConfigurationButton.setEnabled(true);
						}
					});
				}
				{
					addressLabel4 = new CLabel(mLinkAddressesGroup, SWT.RIGHT);
					addressLabel4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel4LData = new RowData();
					addressLabel4LData.width = 91;
					addressLabel4LData.height = 21;
					addressLabel4.setLayoutData(addressLabel4LData);
					addressLabel4.setText("height");
				}
				{
					addressText4 = new Text(mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					addressText4.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText4LData = new RowData();
					addressText4LData.width = 41;
					addressText4LData.height = 13;
					addressText4.setLayoutData(addressText4LData);
					addressText4.setText(GDE.STRING_EMPTY + addressValue4);
				}
				{
					addressSlider4 = new Slider(mLinkAddressesGroup, SWT.NONE);
					addressSlider4.setMinimum(1);
					addressSlider4.setMaximum(26);
					addressSlider4.setIncrement(1);
					RowData addressSlider4LData = new RowData();
					addressSlider4LData.width = 134;
					addressSlider4LData.height = 17;
					addressSlider4.setLayoutData(addressSlider4LData);
					addressSlider4.setSelection(addressValue4);
					addressSlider4.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider4.widgetSelected, event=" + evt); //$NON-NLS-1$
							addressValue4 = addressSlider4.getSelection();
							addressText4.setText(GDE.STRING_EMPTY + addressValue4);
							//TODO writeTelemtryConfigurationButton.setEnabled(true);
						}
					});
				}
				{
					addressLabel5 = new CLabel(mLinkAddressesGroup, SWT.RIGHT);
					addressLabel5.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel5LData = new RowData();
					addressLabel5LData.width = 91;
					addressLabel5LData.height = 21;
					addressLabel5.setLayoutData(addressLabel5LData);
					addressLabel5.setText("distance");
				}
				{
					addressText5 = new Text(mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					addressText5.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText5LData = new RowData();
					addressText5LData.width = 41;
					addressText5LData.height = 13;
					addressText5.setLayoutData(addressText5LData);
					addressText5.setText(GDE.STRING_EMPTY + addressValue5);
				}
				{
					addressSlider5 = new Slider(mLinkAddressesGroup, SWT.NONE);
					addressSlider5.setMinimum(1);
					addressSlider5.setMaximum(26);
					addressSlider5.setIncrement(1);
					RowData addressSlider5LData = new RowData();
					addressSlider5LData.width = 134;
					addressSlider5LData.height = 17;
					addressSlider5.setLayoutData(addressSlider5LData);
					addressSlider5.setSelection(addressValue5);
					addressSlider5.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider5.widgetSelected, event=" + evt); //$NON-NLS-1$
							addressValue5 = addressSlider5.getSelection();
							addressText5.setText(GDE.STRING_EMPTY + addressValue5);
							//TODO writeTelemtryConfigurationButton.setEnabled(true);
						}
					});
				}
				{
					addressLabel6 = new CLabel(mLinkAddressesGroup, SWT.RIGHT);
					addressLabel6.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressLabel6LData = new RowData();
					addressLabel6LData.width = 91;
					addressLabel6LData.height = 21;
					addressLabel6.setLayoutData(addressLabel6LData);
					addressLabel6.setText("distance");
				}
				{
					addressText6 = new Text(mLinkAddressesGroup, SWT.CENTER | SWT.READ_ONLY | SWT.BORDER);
					addressText6.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					RowData addressText6LData = new RowData();
					addressText6LData.width = 41;
					addressText6LData.height = 13;
					addressText6.setLayoutData(addressText6LData);
					addressText6.setText(GDE.STRING_EMPTY + addressValue6);
				}
				{
					addressSlider6 = new Slider(mLinkAddressesGroup, SWT.NONE);
					addressSlider6.setMinimum(1);
					addressSlider6.setMaximum(26);
					addressSlider6.setIncrement(1);
					RowData addressSlider6LData = new RowData();
					addressSlider6LData.width = 134;
					addressSlider6LData.height = 17;
					addressSlider6.setLayoutData(addressSlider6LData);
					addressSlider6.setSelection(addressValue6);
					addressSlider6.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "addressSlider6.widgetSelected, event=" + evt); //$NON-NLS-1$
							addressValue6 = addressSlider6.getSelection();
							addressText6.setText(GDE.STRING_EMPTY + addressValue6);
							//TODO writeTelemtryConfigurationButton.setEnabled(true);
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
