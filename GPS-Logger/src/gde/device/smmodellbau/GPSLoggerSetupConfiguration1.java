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
import org.eclipse.swt.widgets.Text;

public class GPSLoggerSetupConfiguration1 extends org.eclipse.swt.widgets.Composite {
	final static Logger	log	= Logger.getLogger(GPSLoggerSetupConfiguration1.class.getName());

	Composite						fillerComposite;

	Group								gpsLoggerGroup;
	CLabel							serialNumberLabel, firmwareLabel, dataRatecLabel, startModusLabel, timeZoneLabel, varioLimitLabel, varioTonLabel, timeZoneUnitLabel, varioLimitUnitLabel;
	Text								serialNumberText, firmwareText;
	CCombo							dataRateCombo, startModusCombo, timeZoneCombo, varioLimitCombo, varioToneCombo;

	Group								gpsTelemertieGroup;
	Button							heightButton, velocityButton, distanceButton, pathLengthButton, voltageRxButton;
	CCombo							heightCombo, velocityCombo, distanceCombo, pathLengthCombo, voltageRxCombo;
	CLabel							heightLabel, velocityLabel, distanceLabel, pathLengthLabel, voltageRxLabel;

	Button saveButton;

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
		GPSLoggerSetupConfiguration1 inst = new GPSLoggerSetupConfiguration1(shell, SWT.NULL);
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

	public GPSLoggerSetupConfiguration1(org.eclipse.swt.widgets.Composite parent, int style) {
		super(parent, style);
		SWTResourceManager.registerResourceUser(this);
		initGUI();
	}

	void initGUI() {
		try {
			this.setLayout(new FormLayout());
			this.setSize(337, 520);
			{
				FormData gpsTelemertieGroupLData = new FormData();
				gpsTelemertieGroupLData.width = 248;
				gpsTelemertieGroupLData.height = 156;
				gpsTelemertieGroupLData.left =  new FormAttachment(0, 1000, 12);
				gpsTelemertieGroupLData.top =  new FormAttachment(0, 1000, 249);
				gpsTelemertieGroupLData.right =  new FormAttachment(1000, 1000, -12);
				gpsTelemertieGroup = new Group(this, SWT.NONE);
				RowLayout gpsTelemertieGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				gpsTelemertieGroup.setLayout(gpsTelemertieGroupLayout);
				gpsTelemertieGroup.setLayoutData(gpsTelemertieGroupLData);
				gpsTelemertieGroup.setText("Telemetry Alarms");
				gpsTelemertieGroup.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						System.out.println("gpsTelemertieGroup.helpRequested, event="+evt);
						//TODO add your code for gpsTelemertieGroup.helpRequested
					}
				});
				{
					fillerComposite = new Composite(gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 301;
					fillerCompositeRALData.height = 10;
					fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					fillerComposite = new Composite(gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 20;
					fillerCompositeRALData.height = 20;
					fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					heightButton = new Button(gpsTelemertieGroup, SWT.CHECK | SWT.LEFT);
					RowData heightButtonLData = new RowData();
					heightButtonLData.width = 109;
					heightButtonLData.height = 16;
					heightButton.setLayoutData(heightButtonLData);
					heightButton.setText("height");
					heightButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("heightButton.widgetSelected, event="+evt);
							//TODO add your code for heightButton.widgetSelected
						}
					});
				}
				{
					RowData heightCComboLData = new RowData();
					heightCComboLData.width = 70;
					heightCComboLData.height = 17;
					heightCombo = new CCombo(gpsTelemertieGroup, SWT.BORDER);
					heightCombo.setLayoutData(heightCComboLData);
					heightCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("heightCCombo.widgetSelected, event="+evt);
							//TODO add your code for heightCCombo.widgetSelected
						}
					});
				}
				{
					heightLabel = new CLabel(gpsTelemertieGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData heightLabelLData = new RowData();
					heightLabelLData.width = 89;
					heightLabelLData.height = 22;
					heightLabel.setLayoutData(heightLabelLData);
					heightLabel.setText("m");
				}
				{
					fillerComposite = new Composite(gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 20;
					fillerCompositeRALData.height = 20;
					fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					velocityButton = new Button(gpsTelemertieGroup, SWT.CHECK | SWT.LEFT);
					RowData velocityButtonLData = new RowData();
					velocityButtonLData.width = 109;
					velocityButtonLData.height = 16;
					velocityButton.setLayoutData(velocityButtonLData);
					velocityButton.setText("velocity");
					velocityButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("velocityButton.widgetSelected, event="+evt);
							//TODO add your code for velocityButton.widgetSelected
						}
					});
				}
				{
					RowData velocityCComboLData = new RowData();
					velocityCComboLData.width = 70;
					velocityCComboLData.height = 17;
					velocityCombo = new CCombo(gpsTelemertieGroup, SWT.BORDER);
					velocityCombo.setLayoutData(velocityCComboLData);
					velocityCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("velocityCCombo.widgetSelected, event="+evt);
							//TODO add your code for velocityCCombo.widgetSelected
						}
					});
				}
				{
					velocityLabel = new CLabel(gpsTelemertieGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData velocityLabelLData = new RowData();
					velocityLabelLData.width = 89;
					velocityLabelLData.height = 22;
					velocityLabel.setLayoutData(velocityLabelLData);
					velocityLabel.setText("km/h");
				}
				{
					fillerComposite = new Composite(gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 20;
					fillerCompositeRALData.height = 20;
					fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					distanceButton = new Button(gpsTelemertieGroup, SWT.CHECK | SWT.LEFT);
					RowData distanceButtonLData = new RowData();
					distanceButtonLData.width = 109;
					distanceButtonLData.height = 16;
					distanceButton.setLayoutData(distanceButtonLData);
					distanceButton.setText("distance");
					distanceButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("distanceButton.widgetSelected, event="+evt);
							//TODO add your code for distanceButton.widgetSelected
						}
					});
				}
				{
					RowData distanceCComboLData = new RowData();
					distanceCComboLData.width = 70;
					distanceCComboLData.height = 17;
					distanceCombo = new CCombo(gpsTelemertieGroup, SWT.BORDER);
					distanceCombo.setLayoutData(distanceCComboLData);
					distanceCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("distanceCCombo.widgetSelected, event="+evt);
							//TODO add your code for distanceCCombo.widgetSelected
						}
					});
				}
				{
					distanceLabel = new CLabel(gpsTelemertieGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData distanceLabelLData = new RowData();
					distanceLabelLData.width = 89;
					distanceLabelLData.height = 22;
					distanceLabel.setLayoutData(distanceLabelLData);
					distanceLabel.setText("m");
				}
				{
					fillerComposite = new Composite(gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 20;
					fillerCompositeRALData.height = 20;
					fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					pathLengthButton = new Button(gpsTelemertieGroup, SWT.CHECK | SWT.LEFT);
					RowData pathLengthButtonLData = new RowData();
					pathLengthButtonLData.width = 109;
					pathLengthButtonLData.height = 16;
					pathLengthButton.setLayoutData(pathLengthButtonLData);
					pathLengthButton.setText("path length");
					pathLengthButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("pathLengthButton.widgetSelected, event="+evt);
							//TODO add your code for pathLengthButton.widgetSelected
						}
					});
				}
				{
					RowData pathLengthCComboLData = new RowData();
					pathLengthCComboLData.width = 70;
					pathLengthCComboLData.height = 17;
					pathLengthCombo = new CCombo(gpsTelemertieGroup, SWT.BORDER);
					pathLengthCombo.setLayoutData(pathLengthCComboLData);
					pathLengthCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("pathLengthCCombo.widgetSelected, event="+evt);
							//TODO add your code for pathLengthCCombo.widgetSelected
						}
					});
				}
				{
					pathLengthLabel = new CLabel(gpsTelemertieGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData pathLengthLabelLData = new RowData();
					pathLengthLabelLData.width = 89;
					pathLengthLabelLData.height = 22;
					pathLengthLabel.setLayoutData(pathLengthLabelLData);
					pathLengthLabel.setText("km");
				}
				{
					fillerComposite = new Composite(gpsTelemertieGroup, SWT.NONE);
					RowData fillerCompositeRALData = new RowData();
					fillerCompositeRALData.width = 20;
					fillerCompositeRALData.height = 20;
					fillerComposite.setLayoutData(fillerCompositeRALData);
				}
				{
					voltageRxButton = new Button(gpsTelemertieGroup, SWT.CHECK | SWT.LEFT);
					RowData voltageRxButtonLData = new RowData();
					voltageRxButtonLData.width = 109;
					voltageRxButtonLData.height = 16;
					voltageRxButton.setLayoutData(voltageRxButtonLData);
					voltageRxButton.setText("voltageRx");
					voltageRxButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("voltageRxButton.widgetSelected, event="+evt);
							//TODO add your code for voltageRxButton.widgetSelected
						}
					});
				}
				{
					RowData voltageRxCComboLData = new RowData();
					voltageRxCComboLData.width = 70;
					voltageRxCComboLData.height = 17;
					voltageRxCombo = new CCombo(gpsTelemertieGroup, SWT.BORDER);
					voltageRxCombo.setLayoutData(voltageRxCComboLData);
					voltageRxCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("voltageRxCCombo.widgetSelected, event="+evt);
							//TODO add your code for voltageRxCCombo.widgetSelected
						}
					});
				}
				{
					voltageRxLabel = new CLabel(gpsTelemertieGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData voltageRxLabelLData = new RowData();
					voltageRxLabelLData.width = 89;
					voltageRxLabelLData.height = 22;
					voltageRxLabel.setLayoutData(voltageRxLabelLData);
					voltageRxLabel.setText("V");
				}
			}
			{
				FormData gpsLoggerGroupLData = new FormData();
				gpsLoggerGroupLData.width = 320;
				gpsLoggerGroupLData.height = 206;
				gpsLoggerGroupLData.left =  new FormAttachment(0, 1000, 12);
				gpsLoggerGroupLData.top =  new FormAttachment(0, 1000, 12);
				gpsLoggerGroupLData.right =  new FormAttachment(1000, 1000, -12);
				gpsLoggerGroup = new Group(this, SWT.NONE);
				RowLayout gpsLoggerGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				gpsLoggerGroup.setLayout(gpsLoggerGroupLayout);
				gpsLoggerGroup.setLayoutData(gpsLoggerGroupLData);
				gpsLoggerGroup.setText("Basic Setup");
				gpsLoggerGroup.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						System.out.println("gpsLoggerGroup.helpRequested, event="+evt);
						//TODO add your code for gpsLoggerGroup.helpRequested
					}
				});
				{
					fillerComposite = new Composite(gpsLoggerGroup, SWT.NONE);
					RowData fillerCompositeRA1LData = new RowData();
					fillerCompositeRA1LData.width = 299;
					fillerCompositeRA1LData.height = 12;
					fillerComposite.setLayoutData(fillerCompositeRA1LData);
				}
				{
					serialNumberLabel = new CLabel(gpsLoggerGroup, SWT.RIGHT);
					RowData serialNumberLabelLData = new RowData();
					serialNumberLabelLData.width = 146;
					serialNumberLabelLData.height = 22;
					serialNumberLabel.setLayoutData(serialNumberLabelLData);
					serialNumberLabel.setText("serial number");
				}
				{
					RowData serialNumberTextLData = new RowData();
					serialNumberTextLData.width = 83;
					serialNumberTextLData.height = 16;
					serialNumberText = new Text(gpsLoggerGroup, SWT.RIGHT | SWT.BORDER);
					serialNumberText.setLayoutData(serialNumberTextLData);
					serialNumberText.setEditable(false);
				}
				{
					firmwareLabel = new CLabel(gpsLoggerGroup, SWT.RIGHT);
					RowData firmwareLabelLData = new RowData();
					firmwareLabelLData.width = 146;
					firmwareLabelLData.height = 22;
					firmwareLabel.setLayoutData(firmwareLabelLData);
					firmwareLabel.setText("firmware");
				}
				{
					RowData firmwareTextLData = new RowData();
					firmwareTextLData.width = 83;
					firmwareTextLData.height = 16;
					firmwareText = new Text(gpsLoggerGroup, SWT.RIGHT | SWT.BORDER);
					firmwareText.setLayoutData(firmwareTextLData);
					firmwareText.setEditable(false);
				}
				{
					dataRatecLabel = new CLabel(gpsLoggerGroup, SWT.RIGHT);
					RowData dataRatecLabelLData = new RowData();
					dataRatecLabelLData.width = 146;
					dataRatecLabelLData.height = 22;
					dataRatecLabel.setLayoutData(dataRatecLabelLData);
					dataRatecLabel.setText("data rate");
				}
				{
					RowData dataRateCComboLData = new RowData();
					dataRateCComboLData.width = 66;
					dataRateCComboLData.height = 17;
					dataRateCombo = new CCombo(gpsLoggerGroup, SWT.BORDER);
					dataRateCombo.setLayoutData(dataRateCComboLData);
					dataRateCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("dataRateCCombo.widgetSelected, event="+evt);
							//TODO add your code for dataRateCCombo.widgetSelected
						}
					});
				}
				{
					startModusLabel = new CLabel(gpsLoggerGroup, SWT.RIGHT);
					RowData startModusLabelLData = new RowData();
					startModusLabelLData.width = 146;
					startModusLabelLData.height = 22;
					startModusLabel.setLayoutData(startModusLabelLData);
					startModusLabel.setText("start modus");
				}
				{
					RowData startModusCComboLData = new RowData();
					startModusCComboLData.width = 66;
					startModusCComboLData.height = 17;
					startModusCombo = new CCombo(gpsLoggerGroup, SWT.BORDER);
					startModusCombo.setLayoutData(startModusCComboLData);
					startModusCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("startModusCCombo.widgetSelected, event="+evt);
							//TODO add your code for startModusCCombo.widgetSelected
						}
					});
				}
				{
					RowData timeZoneLabelLData = new RowData();
					timeZoneLabelLData.width = 146;
					timeZoneLabelLData.height = 22;
					timeZoneLabel = new CLabel(gpsLoggerGroup, SWT.RIGHT);
					timeZoneLabel.setLayoutData(timeZoneLabelLData);
					timeZoneLabel.setText("time zone");
				}
				{
					RowData timeZoneCComboLData = new RowData();
					timeZoneCComboLData.width = 66;
					timeZoneCComboLData.height = 17;
					timeZoneCombo = new CCombo(gpsLoggerGroup, SWT.BORDER);
					timeZoneCombo.setLayoutData(timeZoneCComboLData);
					timeZoneCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("timeZoneCCombo.widgetSelected, event="+evt);
							//TODO add your code for timeZoneCCombo.widgetSelected
						}
					});
				}
				{
					timeZoneUnitLabel = new CLabel(gpsLoggerGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData timeZoneUnitLabelLData = new RowData();
					timeZoneUnitLabelLData.width = 79;
					timeZoneUnitLabelLData.height = 22;
					timeZoneUnitLabel.setLayoutData(timeZoneUnitLabelLData);
					timeZoneUnitLabel.setText("h");
				}
				{
					varioLimitLabel = new CLabel(gpsLoggerGroup, SWT.RIGHT);
					RowData varioLimitLabelLData = new RowData();
					varioLimitLabelLData.width = 146;
					varioLimitLabelLData.height = 22;
					varioLimitLabel.setLayoutData(varioLimitLabelLData);
					varioLimitLabel.setText("vario limit");
				}
				{
					RowData varioLimitCComboLData = new RowData();
					varioLimitCComboLData.width = 66;
					varioLimitCComboLData.height = 17;
					varioLimitCombo = new CCombo(gpsLoggerGroup, SWT.BORDER);
					varioLimitCombo.setLayoutData(varioLimitCComboLData);
					varioLimitCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("varioLimitCCombo.widgetSelected, event="+evt);
							//TODO add your code for varioLimitCCombo.widgetSelected
						}
					});
				}
				{
					varioLimitUnitLabel = new CLabel(gpsLoggerGroup, SWT.CENTER);
					RowData varioLimitUnitLabelLData = new RowData();
					varioLimitUnitLabelLData.width = 79;
					varioLimitUnitLabelLData.height = 22;
					varioLimitUnitLabel.setLayoutData(varioLimitUnitLabelLData);
					varioLimitUnitLabel.setText("m/s");
				}
				{
					varioTonLabel = new CLabel(gpsLoggerGroup, SWT.RIGHT);
					RowData varioTonLabelLData = new RowData();
					varioTonLabelLData.width = 146;
					varioTonLabelLData.height = 22;
					varioTonLabel.setLayoutData(varioTonLabelLData);
					varioTonLabel.setText("vario tone");
				}
				{
					RowData varioToneComboLData = new RowData();
					varioToneComboLData.width = 66;
					varioToneComboLData.height = 17;
					varioToneCombo = new CCombo(gpsLoggerGroup, SWT.BORDER);
					varioToneCombo.setLayoutData(varioToneComboLData);
					varioToneCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("varioToneCCombo.widgetSelected, event="+evt);
							//TODO add your code for varioToneCCombo.widgetSelected
						}
					});
				}
			}
			{
				saveButton = new Button(this, SWT.PUSH | SWT.CENTER);
				saveButton.setEnabled(false);
				saveButton.setText("save setup");
				FormData saveButtonLData = new FormData();
				saveButtonLData.width = 178;
				saveButtonLData.height = 31;
				saveButtonLData.left =  new FormAttachment(0, 1000, 80);
				saveButtonLData.bottom =  new FormAttachment(1000, 1000, -20);
				saveButtonLData.right =  new FormAttachment(1000, 1000, -80);
				saveButton.setLayoutData(saveButtonLData);
				saveButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						System.out.println("saveButton.widgetSelected, event="+evt);
						//TODO add your code for saveButton.widgetSelected
					}
				});
			}
			this.layout();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
