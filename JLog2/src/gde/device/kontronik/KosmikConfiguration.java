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
    
    Copyright (c) 2013 Winfried Bruegmann
****************************************************************************************/
package gde.device.kontronik;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DataTypes;
import gde.device.smmodellbau.JLog2Configuration;
import gde.device.smmodellbau.JLog2Dialog;
import gde.device.smmodellbau.jlog2.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

/**
 * composite containing configuration items to adapt displayed values to connected motor 
 * respective motor with gear ratio for rotor or propeller revolutions per minute
 */
public class KosmikConfiguration extends Composite {
	final static Logger	log			= Logger.getLogger(JLog2Configuration.class.getName());

	private CLabel			kosmikHeaderLabel;
	private CLabel			motorRotorRpmLabel;
	private CLabel			numMotorPolsLabel;
	private CLabel			motorPinionLabel;
	private CCombo			motorRotorRpmCombo;
	private CCombo			kosmikVersionCombo;
	private CCombo			temperatureUnitCombo;
	private Slider			mainGearToothCountSlider;
	private Text				mainGearToothCountText;
	private Text				motorPinionText;
	private Slider			motorPinionSlider;
	private CCombo			numMotorPolsCombo;
	private CLabel			kosmikVersionLabel;
	private CLabel			temperatureUnitLabel;
	private CLabel			mainGearToothCountLabel;

	final DataExplorer	application;
	final JLog2Dialog		dialog;
	final Kosmik				device;
	final String[]			oneTo50	= new String[50];

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
		KosmikConfiguration inst = new KosmikConfiguration(shell, SWT.NULL);
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
	 * default constructor to be used by Kosmik plug-in
	 * @param parent
	 * @param style
	 * @param useDialog
	 * @param useDevice
	 */
	public KosmikConfiguration(Composite parent, int style, JLog2Dialog useDialog, Kosmik useDevice) {
		super(parent, style);
		this.application = DataExplorer.getInstance();
		this.dialog = useDialog;
		this.device = useDevice;

		for (int i = 0; i < this.oneTo50.length; i++) {
			this.oneTo50[i] = GDE.STRING_EMPTY + (i + 1);
		}
		initGUI();
	}

	/**
	 * constructor to be used by Kosmik plug-in
	 * @param parent
	 * @param style
	 */
	public KosmikConfiguration(Composite parent, int style) {
		super(parent, style);
		for (int i = 0; i < this.oneTo50.length; i++) {
			this.oneTo50[i] = GDE.STRING_EMPTY + (i + 1);
		}
		this.application = null;
		this.dialog = null;
		this.device = null;
		initGUI();
	}

	private void initGUI() {
		try {
			this.setLayout(new FormLayout());
			this.setSize(665, 334);
			{
				this.kosmikHeaderLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
				FormData kosmikHeaderLabelLData = new FormData();
				kosmikHeaderLabelLData.left = new FormAttachment(0, 1000, 10);
				kosmikHeaderLabelLData.top = new FormAttachment(0, 1000, 10);
				kosmikHeaderLabelLData.width = this.getClientArea().width - 20;
				kosmikHeaderLabelLData.height = 18;
				this.kosmikHeaderLabel.setLayoutData(kosmikHeaderLabelLData);
				this.kosmikHeaderLabel.setText(Messages.getString(MessageIds.GDE_MSGW2804));
			}
			{
				this.motorRotorRpmLabel = new CLabel(this, SWT.NONE);
				this.motorRotorRpmLabel.setText(Messages.getString(MessageIds.GDE_MSGW2805));
				FormData rpmLabelLData = new FormData();
				rpmLabelLData.width = 272;
				rpmLabelLData.height = 18;
				rpmLabelLData.left = new FormAttachment(0, 1000, 125);
				rpmLabelLData.top = new FormAttachment(0, 1000, 40);
				this.motorRotorRpmLabel.setLayoutData(rpmLabelLData);
			}
			{
				this.motorRotorRpmCombo = new CCombo(this, SWT.BORDER);
				this.motorRotorRpmCombo.setItems(new String[] { "  Motor", "Propeller/Rotor" }); //$NON-NLS-1$ //$NON-NLS-2$
				FormData motorRotorRpmComboLData = new FormData();
				motorRotorRpmComboLData.width = 80;
				motorRotorRpmComboLData.height = 18;
				motorRotorRpmComboLData.left = new FormAttachment(0, 1000, 400);
				motorRotorRpmComboLData.top = new FormAttachment(0, 1000, 40);
				this.motorRotorRpmCombo.setLayoutData(motorRotorRpmComboLData);
				this.motorRotorRpmCombo.select(0);
				this.motorRotorRpmCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (KosmikConfiguration.log.isLoggable(java.util.logging.Level.FINEST)) KosmikConfiguration.log.log(java.util.logging.Level.FINEST, "motorRotorRpmCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
						updateMotorRotor(KosmikConfiguration.this.motorRotorRpmCombo.getSelectionIndex());
					}
				});
			}
			{
				this.numMotorPolsLabel = new CLabel(this, SWT.NONE);
				this.numMotorPolsLabel.setText(Messages.getString(MessageIds.GDE_MSGW2808));
				FormData numPolsLabelLData = new FormData();
				numPolsLabelLData.width = 272;
				numPolsLabelLData.height = 18;
				numPolsLabelLData.left = new FormAttachment(0, 1000, 125);
				numPolsLabelLData.top = new FormAttachment(0, 1000, 80);
				this.numMotorPolsLabel.setLayoutData(numPolsLabelLData);
			}
			{
				this.numMotorPolsCombo = new CCombo(this, SWT.BORDER);
				this.numMotorPolsCombo.setItems(new String[] { "    2", "    4", "    6", "    8", "   10", "   12", "   14", "   16", "   18" });
				FormData numMotorPolsComboLData = new FormData();
				numMotorPolsComboLData.width = 80;
				numMotorPolsComboLData.height = 18;
				numMotorPolsComboLData.left = new FormAttachment(0, 1000, 400);
				numMotorPolsComboLData.top = new FormAttachment(0, 1000, 80);
				this.numMotorPolsCombo.setLayoutData(numMotorPolsComboLData);
				this.numMotorPolsCombo.select(6);
				this.numMotorPolsCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (KosmikConfiguration.log.isLoggable(java.util.logging.Level.FINEST)) KosmikConfiguration.log.log(java.util.logging.Level.FINEST, "numMotorPolsCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
						if (KosmikConfiguration.this.motorRotorRpmCombo.getSelectionIndex() == 0) { //motor RPM
							Channel activeChannel = Channels.getInstance().getActiveChannel();
							if (activeChannel != null) {
								RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
								if (activeRecordSet != null) {
									double factor = 2.0 / ((KosmikConfiguration.this.numMotorPolsCombo.getSelectionIndex() + 1) * 2);
									activeRecordSet.get(0).setFactor(factor);
									KosmikConfiguration.this.device.setMeasurementFactor(activeChannel.getNumber(), 0, factor);

									KosmikConfiguration.this.device.updateVisibilityStatus(activeRecordSet, false);
									KosmikConfiguration.this.dialog.enableSaveButton(true);
								}
							}
						}
						else {
							Channel activeChannel = Channels.getInstance().getActiveChannel();
							if (activeChannel != null) {
								RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
								if (activeRecordSet != null) {
									double factor = 2.0 / ((KosmikConfiguration.this.numMotorPolsCombo.getSelectionIndex() + 1) * 2);
									double gearFactor = Integer.valueOf(KosmikConfiguration.this.mainGearToothCountText.getText().trim()) / Integer.valueOf(KosmikConfiguration.this.motorPinionText.getText().trim());
									factor /= gearFactor;
									activeRecordSet.get(0).setFactor(factor);
									KosmikConfiguration.this.device.setMeasurementFactor(activeChannel.getNumber(), 0, factor);

									KosmikConfiguration.this.device.updateVisibilityStatus(activeRecordSet, false);
									KosmikConfiguration.this.dialog.enableSaveButton(true);
								}
							}

						}
					}
				});
			}
			{
				this.motorPinionLabel = new CLabel(this, SWT.NONE);
				this.motorPinionLabel.setText(Messages.getString(MessageIds.GDE_MSGW2806));
				FormData motorPinionLabelLData = new FormData();
				motorPinionLabelLData.width = 272;
				motorPinionLabelLData.height = 18;
				motorPinionLabelLData.left = new FormAttachment(0, 1000, 125);
				motorPinionLabelLData.top = new FormAttachment(0, 1000, 120);
				this.motorPinionLabel.setLayoutData(motorPinionLabelLData);
			}
			{
				this.motorPinionText = new Text(this, SWT.CENTER | SWT.BORDER);
				this.motorPinionText.setText("17"); //$NON-NLS-1$
				FormData motorPinionTextLData = new FormData();
				motorPinionTextLData.width = 23;
				motorPinionTextLData.height = 18;
				motorPinionTextLData.left = new FormAttachment(0, 1000, 400);
				motorPinionTextLData.top = new FormAttachment(0, 1000, 120);
				this.motorPinionText.setLayoutData(motorPinionTextLData);
				this.motorPinionText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent evt) {
						if (KosmikConfiguration.log.isLoggable(java.util.logging.Level.FINEST)) KosmikConfiguration.log.log(java.util.logging.Level.FINEST, "motorPinionText.keyPressed, event=" + evt); //$NON-NLS-1$
						KosmikConfiguration.this.motorPinionSlider.setSelection(Integer.valueOf(KosmikConfiguration.this.motorPinionText.getText().trim())
								+ KosmikConfiguration.this.motorPinionSlider.getMinimum());
						KosmikConfiguration.this.dialog.enableSaveButton(true);
					}
				});
				this.motorPinionText.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent evt) {
						if (KosmikConfiguration.log.isLoggable(java.util.logging.Level.FINEST)) KosmikConfiguration.log.log(java.util.logging.Level.FINEST, "motorPinionText.verifyText, event=" + evt); //$NON-NLS-1$
						evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
					}
				});
			}
			{
				this.motorPinionSlider = new Slider(this, SWT.HORIZONTAL | SWT.BORDER);
				FormData motorPinionSliderLData = new FormData();
				motorPinionSliderLData.width = 100;
				motorPinionSliderLData.height = 18;
				motorPinionSliderLData.left = new FormAttachment(0, 1000, 440);
				motorPinionSliderLData.top = new FormAttachment(0, 1000, 120);
				this.motorPinionSlider.setLayoutData(motorPinionSliderLData);
				this.motorPinionSlider.setMaximum(50);
				this.motorPinionSlider.setMinimum(10);
				this.motorPinionSlider.setSelection(16);
				this.motorPinionSlider.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (KosmikConfiguration.log.isLoggable(java.util.logging.Level.FINEST)) KosmikConfiguration.log.log(java.util.logging.Level.FINEST, "motorPinionSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
						KosmikConfiguration.this.motorPinionText.setText(GDE.STRING_EMPTY + KosmikConfiguration.this.motorPinionSlider.getSelection());
						KosmikConfiguration.this.dialog.enableSaveButton(true);
					}
				});
				this.motorPinionText.setText(GDE.STRING_EMPTY + this.motorPinionSlider.getSelection());
			}
			{
				this.mainGearToothCountLabel = new CLabel(this, SWT.NONE);
				this.mainGearToothCountLabel.setText(Messages.getString(MessageIds.GDE_MSGW2807));
				FormData mainGearToothCountLabelLData = new FormData();
				mainGearToothCountLabelLData.width = 272;
				mainGearToothCountLabelLData.height = 18;
				mainGearToothCountLabelLData.left = new FormAttachment(0, 1000, 125);
				mainGearToothCountLabelLData.top = new FormAttachment(0, 1000, 160);
				this.mainGearToothCountLabel.setLayoutData(mainGearToothCountLabelLData);
			}
			{
				this.mainGearToothCountText = new Text(this, SWT.CENTER | SWT.BORDER);
				this.mainGearToothCountText.setText("87");
				FormData mainGearToothCountTextLData = new FormData();
				mainGearToothCountTextLData.width = 23;
				mainGearToothCountTextLData.height = 18;
				mainGearToothCountTextLData.left = new FormAttachment(0, 1000, 400);
				mainGearToothCountTextLData.top = new FormAttachment(0, 1000, 160);
				this.mainGearToothCountText.setLayoutData(mainGearToothCountTextLData);
				this.mainGearToothCountText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent evt) {
						if (KosmikConfiguration.log.isLoggable(java.util.logging.Level.FINEST)) KosmikConfiguration.log.log(java.util.logging.Level.FINEST, "mainGearToothCountText.keyPressed, event=" + evt); //$NON-NLS-1$
						KosmikConfiguration.this.mainGearToothCountSlider.setSelection(Integer.valueOf(KosmikConfiguration.this.mainGearToothCountText.getText().trim())
								+ KosmikConfiguration.this.mainGearToothCountSlider.getMinimum());
						KosmikConfiguration.this.dialog.enableSaveButton(true);
					}
				});
				this.mainGearToothCountText.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent evt) {
						if (KosmikConfiguration.log.isLoggable(java.util.logging.Level.FINEST)) KosmikConfiguration.log.log(java.util.logging.Level.FINEST, "mainGearToothCountText.verifyText, event=" + evt); //$NON-NLS-1$
						evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
					}
				});
			}
			{
				this.mainGearToothCountSlider = new Slider(this, SWT.HORIZONTAL | SWT.BORDER);
				FormData mainGearToothCountSliderLData = new FormData();
				mainGearToothCountSliderLData.width = 100;
				mainGearToothCountSliderLData.height = 18;
				mainGearToothCountSliderLData.left = new FormAttachment(0, 1000, 440);
				mainGearToothCountSliderLData.top = new FormAttachment(0, 1000, 160);
				this.mainGearToothCountSlider.setLayoutData(mainGearToothCountSliderLData);
				this.mainGearToothCountSlider.setMaximum(130);
				this.mainGearToothCountSlider.setMinimum(30);
				this.mainGearToothCountSlider.setSelection(70);
				this.mainGearToothCountSlider.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (KosmikConfiguration.log.isLoggable(java.util.logging.Level.FINEST))
							KosmikConfiguration.log.log(java.util.logging.Level.FINEST, "mainGearToothCountSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
						KosmikConfiguration.this.mainGearToothCountText.setText(GDE.STRING_EMPTY + KosmikConfiguration.this.mainGearToothCountSlider.getSelection());
						KosmikConfiguration.this.dialog.enableSaveButton(true);
					}
				});
				this.mainGearToothCountText.setText(GDE.STRING_EMPTY + this.mainGearToothCountSlider.getSelection());
			}
			{
				this.temperatureUnitLabel = new CLabel(this, SWT.NONE);
				this.temperatureUnitLabel.setText("Temperatur(e):"); //$NON-NLS-1$
				FormData temperatureUnitLabelLData = new FormData();
				temperatureUnitLabelLData.width = 272;
				temperatureUnitLabelLData.height = 18;
				temperatureUnitLabelLData.left = new FormAttachment(0, 1000, 125);
				temperatureUnitLabelLData.top = new FormAttachment(0, 1000, 200);
				this.temperatureUnitLabel.setLayoutData(temperatureUnitLabelLData);
			}
			{
				this.temperatureUnitCombo = new CCombo(this, SWT.BORDER);
				this.temperatureUnitCombo.setItems(new String[] { "    Celsius", "   Fahrenheit" });
				FormData temperatureUnitComboLData = new FormData();
				temperatureUnitComboLData.width = 80;
				temperatureUnitComboLData.height = 18;
				temperatureUnitComboLData.left = new FormAttachment(0, 1000, 400);
				temperatureUnitComboLData.top = new FormAttachment(0, 1000, 200);
				this.temperatureUnitCombo.setLayoutData(temperatureUnitComboLData);
				this.temperatureUnitCombo.select(0);
				this.temperatureUnitCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (KosmikConfiguration.log.isLoggable(java.util.logging.Level.FINEST)) KosmikConfiguration.log.log(java.util.logging.Level.FINEST, "temperatureUnitCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
						Channel activeChannel = Channels.getInstance().getActiveChannel();
						if (activeChannel != null) {
							RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
							if (activeRecordSet != null) {
								double factor = 1.0;
								double offset = 0.0;
								if (KosmikConfiguration.this.temperatureUnitCombo.getSelectionIndex() == 1) {
									factor = 1.8018;
									offset = 32.0;
									activeRecordSet.get(6).setUnit("°F"); //$NON-NLS-1$
									KosmikConfiguration.this.device.setMeasurementUnit(activeChannel.getNumber(), 6, "°F"); //$NON-NLS-1$
									activeRecordSet.get(10).setUnit("°F"); //$NON-NLS-1$
									KosmikConfiguration.this.device.setMeasurementUnit(activeChannel.getNumber(), 10, "°F"); //$NON-NLS-1$
								}
								else {
									activeRecordSet.get(6).setUnit("°C"); //$NON-NLS-1$
									KosmikConfiguration.this.device.setMeasurementUnit(activeChannel.getNumber(), 6, "°C"); //$NON-NLS-1$
									activeRecordSet.get(10).setUnit("°C"); //$NON-NLS-1$
									KosmikConfiguration.this.device.setMeasurementUnit(activeChannel.getNumber(), 10, "°C"); //$NON-NLS-1$
								}
								activeRecordSet.get(6).setFactor(factor);
								KosmikConfiguration.this.device.setMeasurementFactor(activeChannel.getNumber(), 6, factor);
								activeRecordSet.get(6).setOffset(offset);
								KosmikConfiguration.this.device.setMeasurementOffset(activeChannel.getNumber(), 6, offset);
								activeRecordSet.get(10).setFactor(factor);
								KosmikConfiguration.this.device.setMeasurementFactor(activeChannel.getNumber(), 10, factor);
								activeRecordSet.get(10).setOffset(offset);
								KosmikConfiguration.this.device.setMeasurementOffset(activeChannel.getNumber(), 10, offset);

								KosmikConfiguration.this.device.updateVisibilityStatus(activeRecordSet, false);
								KosmikConfiguration.this.dialog.enableSaveButton(true);
							}
							else {
								double factor = 1.0;
								double offset = 0.0;
								if (KosmikConfiguration.this.temperatureUnitCombo.getSelectionIndex() == 1) {
									factor = 1.8018;
									offset = 32.0;
									KosmikConfiguration.this.device.setMeasurementUnit(activeChannel.getNumber(), 6, "°F"); //$NON-NLS-1$
									KosmikConfiguration.this.device.setMeasurementUnit(activeChannel.getNumber(), 10, "°F"); //$NON-NLS-1$
								}
								else {
									KosmikConfiguration.this.device.setMeasurementUnit(activeChannel.getNumber(), 5, "°C"); //$NON-NLS-1$
									KosmikConfiguration.this.device.setMeasurementUnit(activeChannel.getNumber(), 9, "°C"); //$NON-NLS-1$
								}
								KosmikConfiguration.this.device.setMeasurementFactor(activeChannel.getNumber(), 6, factor);
								KosmikConfiguration.this.device.setMeasurementOffset(activeChannel.getNumber(), 6, offset);
								KosmikConfiguration.this.device.setMeasurementFactor(activeChannel.getNumber(), 10, factor);
								KosmikConfiguration.this.device.setMeasurementOffset(activeChannel.getNumber(), 10, offset);

								KosmikConfiguration.this.dialog.enableSaveButton(true);
							}
						}
					}
				});
			}
			{
				this.kosmikVersionLabel = new CLabel(this, SWT.NONE);
				this.kosmikVersionLabel.setText("Kosmik Version:"); //$NON-NLS-1$
				FormData kosmikVersionLabelLData = new FormData();
				kosmikVersionLabelLData.width = 272;
				kosmikVersionLabelLData.height = 18;
				kosmikVersionLabelLData.left = new FormAttachment(0, 1000, 125);
				kosmikVersionLabelLData.top = new FormAttachment(0, 1000, 240);
				this.kosmikVersionLabel.setLayoutData(kosmikVersionLabelLData);
			}
			{
				this.kosmikVersionCombo = new CCombo(this, SWT.BORDER);
				this.kosmikVersionCombo.setItems(new String[] { "  <= 3.1", "  >= 3.2" }); //$NON-NLS-1$ //$NON-NLS-2$
				FormData kosmikVersionComboLData = new FormData();
				kosmikVersionComboLData.width = 80;
				kosmikVersionComboLData.height = 18;
				kosmikVersionComboLData.left = new FormAttachment(0, 1000, 400);
				kosmikVersionComboLData.top = new FormAttachment(0, 1000, 240);
				this.kosmikVersionCombo.setLayoutData(kosmikVersionComboLData);
				this.kosmikVersionCombo.select(1);
				this.kosmikVersionCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (KosmikConfiguration.log.isLoggable(java.util.logging.Level.FINEST)) KosmikConfiguration.log.log(java.util.logging.Level.FINEST, "kosmikVersionCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
						Channel activeChannel = Channels.getInstance().getActiveChannel();
						if (activeChannel != null) {
							KosmikConfiguration.this.device.setMeasurementActive(activeChannel.getNumber(), 6, KosmikConfiguration.this.kosmikVersionCombo.getSelectionIndex() == 1);
							KosmikConfiguration.this.dialog.enableSaveButton(true);
						}
					}
				});
			}
			this.layout();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * switch enable/disable fields depending on motor rotor configuration
	 */
	protected void updateMotorRotor(int selectionIndex) {
		this.motorPinionLabel.setEnabled(selectionIndex == 1);
		this.motorPinionText.setEnabled(selectionIndex == 1);
		this.motorPinionSlider.setEnabled(selectionIndex == 1);
		this.mainGearToothCountLabel.setEnabled(selectionIndex == 1);
		this.mainGearToothCountText.setEnabled(selectionIndex == 1);
		this.mainGearToothCountSlider.setEnabled(selectionIndex == 1);
	}

}
