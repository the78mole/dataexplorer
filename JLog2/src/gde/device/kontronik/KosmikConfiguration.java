package gde.device.kontronik;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DataTypes;
import gde.device.smmodellbau.JLog2Configuration;
import gde.device.smmodellbau.JLog2Dialog;
import gde.log.Level;
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

public class KosmikConfiguration extends Composite {
	final static Logger						log									= Logger.getLogger(JLog2Configuration.class.getName());
	
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
		if(size.x == 0 && size.y == 0) {
			inst.pack();
			shell.pack();
		} else {
			Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
			shell.setSize(shellBounds.width, shellBounds.height);
		}
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
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
			this.setSize(458, 334);
			{
				motorRotorRpmLabel = new CLabel(this, SWT.NONE);
				motorRotorRpmLabel.setText("Motor- Hauptrotordrehzahl");
				FormData rpmLabelLData = new FormData();
				rpmLabelLData.width = 272;
				rpmLabelLData.height = 18;
				rpmLabelLData.left =  new FormAttachment(0, 1000, 25);
				rpmLabelLData.top =  new FormAttachment(0, 1000, 40);
				motorRotorRpmLabel.setLayoutData(rpmLabelLData);
			}
			{
				motorRotorRpmCombo = new CCombo(this, SWT.BORDER);
				motorRotorRpmCombo.setItems(new String[] {"    Motor", "    Rotor"});
				FormData motorRotorRpmComboLData = new FormData();
				motorRotorRpmComboLData.width = 121;
				motorRotorRpmComboLData.height = 18;
				motorRotorRpmComboLData.left =  new FormAttachment(0, 1000, 300);
				motorRotorRpmComboLData.top =  new FormAttachment(0, 1000, 40);
				motorRotorRpmCombo.setLayoutData(motorRotorRpmComboLData);
				motorRotorRpmCombo.select(0);
				motorRotorRpmCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "motorRotorRpmCombo.widgetSelected, event="+evt);
						updateMotorRotor(motorRotorRpmCombo.getSelectionIndex());
					}
				});
			}
			{
				numMotorPolsLabel = new CLabel(this, SWT.NONE);
				numMotorPolsLabel.setText("Anzahl Motorpole");
				FormData numPolsLabelLData = new FormData();
				numPolsLabelLData.width = 272;
				numPolsLabelLData.height = 18;
				numPolsLabelLData.left =  new FormAttachment(0, 1000, 25);
				numPolsLabelLData.top =  new FormAttachment(0, 1000, 80);
				numMotorPolsLabel.setLayoutData(numPolsLabelLData);
			}
			{
				numMotorPolsCombo = new CCombo(this, SWT.BORDER);
				numMotorPolsCombo.setItems(new String[] {"    2", "    4", "    6", "    8", "   10", "   12", "   14", "   16", "   18"});
				FormData numMotorPolsComboLData = new FormData();
				numMotorPolsComboLData.width = 121;
				numMotorPolsComboLData.height = 18;
				numMotorPolsComboLData.left =  new FormAttachment(0, 1000, 300);
				numMotorPolsComboLData.top =  new FormAttachment(0, 1000, 80);
				numMotorPolsCombo.setLayoutData(numMotorPolsComboLData);
				numMotorPolsCombo.select(6);
				numMotorPolsCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "numMotorPolsCombo.widgetSelected, event="+evt);
						if (motorRotorRpmCombo.getSelectionIndex() == 0) { //motor RPM
							Channel activeChannel = Channels.getInstance().getActiveChannel();
							if (activeChannel != null) {
								RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
								if (activeRecordSet != null) {
									double factor = 2.0 / ((numMotorPolsCombo.getSelectionIndex() + 1) * 2);
									activeRecordSet.get(0).setFactor(factor);
									device.setMeasurementFactor(activeChannel.getNumber(), 0, factor);
								}
							}
						}
						else {
							Channel activeChannel = Channels.getInstance().getActiveChannel();
							if (activeChannel != null) {
								RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
								if (activeRecordSet != null) {
									double factor = 2.0 / ((numMotorPolsCombo.getSelectionIndex() + 1) * 2);
									double gearFactor = Integer.valueOf(mainGearToothCountText.getText().trim()) / Integer.valueOf(motorPinionText.getText().trim());
									factor /= gearFactor;
									activeRecordSet.get(0).setFactor(factor);
									device.setMeasurementFactor(activeChannel.getNumber(), 0, factor);
								}
							}

						}
					}
				});
			}
			{
				motorPinionLabel = new CLabel(this, SWT.NONE);
				motorPinionLabel.setText("Motorritzel | Motorpinion tooth count:");
				FormData motorPinionLabelLData = new FormData();
				motorPinionLabelLData.width = 272;
				motorPinionLabelLData.height = 18;
				motorPinionLabelLData.left =  new FormAttachment(0, 1000, 25);
				motorPinionLabelLData.top =  new FormAttachment(0, 1000, 120);
				motorPinionLabel.setLayoutData(motorPinionLabelLData);
			}
			{
				motorPinionText = new Text(this, SWT.CENTER | SWT.BORDER);
				motorPinionText.setText("17");
				FormData motorPinionTextLData = new FormData();
				motorPinionTextLData.width = 23;
				motorPinionTextLData.height = 18;
				motorPinionTextLData.left =  new FormAttachment(0, 1000, 300);
				motorPinionTextLData.top =  new FormAttachment(0, 1000, 120);
				motorPinionText.setLayoutData(motorPinionTextLData);
				motorPinionText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "motorPinionText.keyPressed, event="+evt);
						motorPinionSlider.setSelection(Integer.valueOf(motorPinionText.getText().trim()) + motorPinionSlider.getMinimum());
					}
				});
				motorPinionText.addVerifyListener(new VerifyListener() {
					public void verifyText(VerifyEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "motorPinionText.verifyText, event="+evt);
						evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
					}
				});
			}
			{
				motorPinionSlider = new Slider(this, SWT.HORIZONTAL | SWT.BORDER);
				FormData motorPinionSliderLData = new FormData();
				motorPinionSliderLData.width = 100;
				motorPinionSliderLData.height = 18;
				motorPinionSliderLData.left =  new FormAttachment(0, 1000, 340);
				motorPinionSliderLData.top =  new FormAttachment(0, 1000, 120);
				motorPinionSlider.setLayoutData(motorPinionSliderLData);
				motorPinionSlider.setMaximum(50);
				motorPinionSlider.setMinimum(10);
				motorPinionSlider.setSelection(16);
				motorPinionSlider.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "motorPinionSlider.widgetSelected, event="+evt);
						motorPinionText.setText(""+motorPinionSlider.getSelection());
					}
				});
				motorPinionText.setText(""+motorPinionSlider.getSelection());
			}
			{
				mainGearToothCountLabel = new CLabel(this, SWT.NONE);
				mainGearToothCountLabel.setText("Hauptrotorzähneanzahl | Main gear tooth count:");
				FormData mainGearToothCountLabelLData = new FormData();
				mainGearToothCountLabelLData.width = 272;
				mainGearToothCountLabelLData.height = 18;
				mainGearToothCountLabelLData.left =  new FormAttachment(0, 1000, 25);
				mainGearToothCountLabelLData.top =  new FormAttachment(0, 1000, 160);
				mainGearToothCountLabel.setLayoutData(mainGearToothCountLabelLData);
			}
			{
				mainGearToothCountText = new Text(this, SWT.CENTER | SWT.BORDER);
				mainGearToothCountText.setText("87");
				FormData mainGearToothCountTextLData = new FormData();
				mainGearToothCountTextLData.width = 23;
				mainGearToothCountTextLData.height = 18;
				mainGearToothCountTextLData.left =  new FormAttachment(0, 1000, 300);
				mainGearToothCountTextLData.top =  new FormAttachment(0, 1000, 160);
				mainGearToothCountText.setLayoutData(mainGearToothCountTextLData);
				mainGearToothCountText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "mainGearToothCountText.keyPressed, event="+evt);
						mainGearToothCountSlider.setSelection(Integer.valueOf(mainGearToothCountText.getText().trim()) + mainGearToothCountSlider.getMinimum());
					}
				});
				mainGearToothCountText.addVerifyListener(new VerifyListener() {
					public void verifyText(VerifyEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "mainGearToothCountText.verifyText, event="+evt);
						evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
					}
				});
			}
			{
				mainGearToothCountSlider = new Slider(this, SWT.HORIZONTAL | SWT.BORDER);
				FormData mainGearToothCountSliderLData = new FormData();
				mainGearToothCountSliderLData.width = 100;
				mainGearToothCountSliderLData.height = 18;
				mainGearToothCountSliderLData.left =  new FormAttachment(0, 1000, 340);
				mainGearToothCountSliderLData.top =  new FormAttachment(0, 1000, 160);
				mainGearToothCountSlider.setLayoutData(mainGearToothCountSliderLData);
				mainGearToothCountSlider.setMaximum(130);
				mainGearToothCountSlider.setMinimum(30);
				mainGearToothCountSlider.setSelection(70);
				mainGearToothCountSlider.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "mainGearToothCountSlider.widgetSelected, event="+evt);
						mainGearToothCountText.setText(""+mainGearToothCountSlider.getSelection());
					}
				});
				mainGearToothCountText.setText(""+mainGearToothCountSlider.getSelection());
			}
			{
				temperatureUnitLabel = new CLabel(this, SWT.NONE);
				temperatureUnitLabel.setText("Temperatur(e):");
				FormData temperatureUnitLabelLData = new FormData();
				temperatureUnitLabelLData.width = 272;
				temperatureUnitLabelLData.height = 18;
				temperatureUnitLabelLData.left =  new FormAttachment(0, 1000, 25);
				temperatureUnitLabelLData.top =  new FormAttachment(0, 1000, 200);
				temperatureUnitLabel.setLayoutData(temperatureUnitLabelLData);
			}
			{
				temperatureUnitCombo = new CCombo(this, SWT.BORDER);
				temperatureUnitCombo.setItems(new String[] {"    Celsius", "   Fahrenheit"});
				FormData temperatureUnitComboLData = new FormData();
				temperatureUnitComboLData.width = 121;
				temperatureUnitComboLData.height = 18;
				temperatureUnitComboLData.left =  new FormAttachment(0, 1000, 300);
				temperatureUnitComboLData.top =  new FormAttachment(0, 1000, 200);
				temperatureUnitCombo.setLayoutData(temperatureUnitComboLData);
				temperatureUnitCombo.select(0);
				temperatureUnitCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "temperatureUnitCombo.widgetSelected, event="+evt);
						//TODO add your code for temperatureUnitCombo.widgetSelected
					}
				});
			}
			{
				kosmikVersionLabel = new CLabel(this, SWT.NONE);
				kosmikVersionLabel.setText("Kosmik Version:");
				FormData kosmikVersionLabelLData = new FormData();
				kosmikVersionLabelLData.width = 272;
				kosmikVersionLabelLData.height = 18;
				kosmikVersionLabelLData.left =  new FormAttachment(0, 1000, 25);
				kosmikVersionLabelLData.top =  new FormAttachment(0, 1000, 240);
				kosmikVersionLabel.setLayoutData(kosmikVersionLabelLData);
			}
			{
				kosmikVersionCombo = new CCombo(this, SWT.BORDER);
				kosmikVersionCombo.setItems(new String[] {"  <= 3.0.1", "  >= 3.0.2"});
				FormData kosmikVersionComboLData = new FormData();
				kosmikVersionComboLData.width = 121;
				kosmikVersionComboLData.height = 18;
				kosmikVersionComboLData.left =  new FormAttachment(0, 1000, 300);
				kosmikVersionComboLData.top =  new FormAttachment(0, 1000, 240);
				kosmikVersionCombo.setLayoutData(kosmikVersionComboLData);
				kosmikVersionCombo.select(1);
				kosmikVersionCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "kosmikVersionCombo.widgetSelected, event="+evt);
						//TODO add your code for kosmikVersionCombo.widgetSelected
					}
				});
			}
			this.layout();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	protected void updateMotorRotor(int selectionIndex) {
		motorPinionLabel.setEnabled(selectionIndex == 1);
		motorPinionText.setEnabled(selectionIndex == 1);
		motorPinionSlider.setEnabled(selectionIndex == 1);
		mainGearToothCountLabel.setEnabled(selectionIndex == 1);
		mainGearToothCountText.setEnabled(selectionIndex == 1);
		mainGearToothCountSlider.setEnabled(selectionIndex == 1);
	}

}
