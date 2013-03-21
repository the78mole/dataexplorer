package gde.device.kontronik;

import gde.GDE;
import gde.device.smmodellbau.JLog2;
import gde.device.smmodellbau.JLog2Configuration;
import gde.device.smmodellbau.JLog2Dialog;
import gde.device.smmodellbau.JLog2Configuration.Configuration;
import gde.ui.DataExplorer;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class KosmikConfiguration extends org.eclipse.swt.widgets.Composite {
	private Label motorRotorRpmLabel;
	private Label numMotorPolsLabel;
	private Label motorPinionLabel;
	private CCombo motorRotorRpmCombo;
	private CCombo kosmikVersionCombo;
	private CCombo temperatureUnitCombo;
	private Slider mainGearToothCountSlider;
	private Text mainGearToothCountText;
	private Text motorPinionText;
	private Slider motorPinionSlider;
	private CCombo numMotorPolsCombo;
	private Label kosmikVersionLabel;
	private Label temperatureUnitLabel;
	private Label mainGearToothCountLabel;

	final DataExplorer						application;
	final JLog2Dialog							dialog;
	final Kosmik										device;
	final String[]			oneTo50							= new String[50];

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
	public KosmikConfiguration(org.eclipse.swt.widgets.Composite parent, int style) {
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
				motorRotorRpmLabel = new Label(this, SWT.NONE);
				motorRotorRpmLabel.setText("Motor- Hauptrotordrehzahl");
				FormData rpmLabelLData = new FormData();
				rpmLabelLData.width = 272;
				rpmLabelLData.height = 18;
				rpmLabelLData.left =  new FormAttachment(0, 1000, 26);
				rpmLabelLData.top =  new FormAttachment(0, 1000, 38);
				motorRotorRpmLabel.setLayoutData(rpmLabelLData);
			}
			{
				motorRotorRpmCombo = new CCombo(this, SWT.BORDER);
				motorRotorRpmCombo.setItems(new String[] {"    Motor", "    Rotor"});
				FormData motorRotorRpmComboLData = new FormData();
				motorRotorRpmComboLData.width = 122;
				motorRotorRpmComboLData.height = 14;
				motorRotorRpmComboLData.left =  new FormAttachment(0, 1000, 307);
				motorRotorRpmComboLData.top =  new FormAttachment(0, 1000, 39);
				motorRotorRpmCombo.setLayoutData(motorRotorRpmComboLData);
				motorRotorRpmCombo.select(0);
				motorRotorRpmCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						System.out.println("motorRotorRpmCombo.widgetSelected, event="+evt);
						updateMotorRotor(motorRotorRpmCombo.getSelectionIndex());
					}
				});
			}
			{
				numMotorPolsLabel = new Label(this, SWT.NONE);
				numMotorPolsLabel.setText("Anzahl Motorpole");
				FormData numPolsLabelLData = new FormData();
				numPolsLabelLData.width = 272;
				numPolsLabelLData.height = 18;
				numPolsLabelLData.left =  new FormAttachment(0, 1000, 26);
				numPolsLabelLData.top =  new FormAttachment(0, 1000, 81);
				numMotorPolsLabel.setLayoutData(numPolsLabelLData);
			}
			{
				numMotorPolsCombo = new CCombo(this, SWT.BORDER);
				numMotorPolsCombo.setItems(new String[] {"    2", "    4", "    6", "    8", "   10", "   12", "   14", "   16", "   18"});
				FormData numMotorPolsComboLData = new FormData();
				numMotorPolsComboLData.width = 122;
				numMotorPolsComboLData.height = 14;
				numMotorPolsComboLData.left =  new FormAttachment(0, 1000, 307);
				numMotorPolsComboLData.top =  new FormAttachment(0, 1000, 82);
				numMotorPolsCombo.setLayoutData(numMotorPolsComboLData);
				numMotorPolsCombo.select(6);
				numMotorPolsCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						System.out.println("numMotorPolsCombo.widgetSelected, event="+evt);
						//TODO add your code for numMotorPolsCombo.widgetSelected
					}
				});
			}
			{
				motorPinionLabel = new Label(this, SWT.NONE);
				motorPinionLabel.setText("Motorritzel | Motorpinion tooth count:");
				FormData motorPinionLabelLData = new FormData();
				motorPinionLabelLData.width = 272;
				motorPinionLabelLData.height = 18;
				motorPinionLabelLData.left =  new FormAttachment(0, 1000, 26);
				motorPinionLabelLData.top =  new FormAttachment(0, 1000, 130);
				motorPinionLabel.setLayoutData(motorPinionLabelLData);
			}
			{
				motorPinionText = new Text(this, SWT.CENTER | SWT.BORDER);
				motorPinionText.setText("17");
				FormData motorPinionTextLData = new FormData();
				motorPinionTextLData.width = 25;
				motorPinionTextLData.height = 12;
				motorPinionTextLData.left =  new FormAttachment(0, 1000, 307);
				motorPinionTextLData.top =  new FormAttachment(0, 1000, 130);
				motorPinionText.setLayoutData(motorPinionTextLData);
				motorPinionText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent evt) {
						System.out.println("motorPinionText.keyPressed, event="+evt);
						//TODO add your code for motorPinionText.keyPressed
					}
				});
				motorPinionText.addVerifyListener(new VerifyListener() {
					public void verifyText(VerifyEvent evt) {
						System.out.println("motorPinionText.verifyText, event="+evt);
						//TODO add your code for motorPinionText.verifyText
					}
				});
			}
			{
				motorPinionSlider = new Slider(this, SWT.HORIZONTAL);
				FormData motorPinionSliderLData = new FormData();
				motorPinionSliderLData.width = 92;
				motorPinionSliderLData.height = 18;
				motorPinionSliderLData.left =  new FormAttachment(0, 1000, 340);
				motorPinionSliderLData.top =  new FormAttachment(0, 1000, 130);
				motorPinionSlider.setLayoutData(motorPinionSliderLData);
				motorPinionSlider.setMaximum(50);
				motorPinionSlider.setMinimum(10);
				motorPinionSlider.setSelection(16);
				motorPinionSlider.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						System.out.println("motorPinionSlider.widgetSelected, event="+evt);
						motorPinionText.setText(""+motorPinionSlider.getSelection());
					}
				});
				motorPinionText.setText(""+motorPinionSlider.getSelection());
			}
			{
				mainGearToothCountLabel = new Label(this, SWT.NONE);
				mainGearToothCountLabel.setText("Hauptrotorzähneanzahl | Main gear tooth count:");
				FormData mainGearToothCountLabelLData = new FormData();
				mainGearToothCountLabelLData.width = 272;
				mainGearToothCountLabelLData.height = 18;
				mainGearToothCountLabelLData.left =  new FormAttachment(0, 1000, 26);
				mainGearToothCountLabelLData.top =  new FormAttachment(0, 1000, 176);
				mainGearToothCountLabel.setLayoutData(mainGearToothCountLabelLData);
			}
			{
				mainGearToothCountText = new Text(this, SWT.CENTER | SWT.BORDER);
				mainGearToothCountText.setText("87");
				FormData mainGearToothCountTextLData = new FormData();
				mainGearToothCountTextLData.width = 25;
				mainGearToothCountTextLData.height = 12;
				mainGearToothCountTextLData.left =  new FormAttachment(0, 1000, 307);
				mainGearToothCountTextLData.top =  new FormAttachment(0, 1000, 174);
				mainGearToothCountText.setLayoutData(mainGearToothCountTextLData);
				mainGearToothCountText.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent evt) {
						System.out.println("mainGearToothCountText.keyPressed, event="+evt);
						//TODO add your code for mainGearToothCountText.keyPressed
					}
				});
				mainGearToothCountText.addVerifyListener(new VerifyListener() {
					public void verifyText(VerifyEvent evt) {
						System.out.println("mainGearToothCountText.verifyText, event="+evt);
						//TODO add your code for mainGearToothCountText.verifyText
					}
				});
			}
			{
				mainGearToothCountSlider = new Slider(this, SWT.HORIZONTAL);
				FormData mainGearToothCountSliderLData = new FormData();
				mainGearToothCountSliderLData.width = 92;
				mainGearToothCountSliderLData.height = 18;
				mainGearToothCountSliderLData.left =  new FormAttachment(0, 1000, 339);
				mainGearToothCountSliderLData.top =  new FormAttachment(0, 1000, 174);
				mainGearToothCountSlider.setLayoutData(mainGearToothCountSliderLData);
				mainGearToothCountSlider.setMaximum(130);
				mainGearToothCountSlider.setMinimum(30);
				mainGearToothCountSlider.setSelection(70);
				mainGearToothCountSlider.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						System.out.println("mainGearToothCountSlider.widgetSelected, event="+evt);
						mainGearToothCountText.setText(""+mainGearToothCountSlider.getSelection());
					}
				});
				mainGearToothCountText.setText(""+mainGearToothCountSlider.getSelection());
			}
			{
				temperatureUnitLabel = new Label(this, SWT.NONE);
				temperatureUnitLabel.setText("Temperatur(e):");
				FormData temperatureUnitLabelLData = new FormData();
				temperatureUnitLabelLData.width = 272;
				temperatureUnitLabelLData.height = 18;
				temperatureUnitLabelLData.left =  new FormAttachment(0, 1000, 26);
				temperatureUnitLabelLData.top =  new FormAttachment(0, 1000, 222);
				temperatureUnitLabel.setLayoutData(temperatureUnitLabelLData);
			}
			{
				temperatureUnitCombo = new CCombo(this, SWT.BORDER);
				temperatureUnitCombo.setItems(new String[] {"    Celsius", "   Fahrenheit"});
				FormData temperatureUnitComboLData = new FormData();
				temperatureUnitComboLData.width = 122;
				temperatureUnitComboLData.height = 14;
				temperatureUnitComboLData.left =  new FormAttachment(0, 1000, 307);
				temperatureUnitComboLData.top =  new FormAttachment(0, 1000, 223);
				temperatureUnitCombo.setLayoutData(temperatureUnitComboLData);
				temperatureUnitCombo.select(0);
				temperatureUnitCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						System.out.println("temperatureUnitCombo.widgetSelected, event="+evt);
						//TODO add your code for temperatureUnitCombo.widgetSelected
					}
				});
			}
			{
				kosmikVersionLabel = new Label(this, SWT.NONE);
				kosmikVersionLabel.setText("Kosmik Version:");
				FormData kosmikVersionLabelLData = new FormData();
				kosmikVersionLabelLData.width = 272;
				kosmikVersionLabelLData.height = 18;
				kosmikVersionLabelLData.left =  new FormAttachment(0, 1000, 26);
				kosmikVersionLabelLData.top =  new FormAttachment(0, 1000, 262);
				kosmikVersionLabel.setLayoutData(kosmikVersionLabelLData);
			}
			{
				kosmikVersionCombo = new CCombo(this, SWT.BORDER);
				kosmikVersionCombo.setItems(new String[] {"  <= 3.0.1", "  >= 3.0.2"});
				FormData kosmikVersionComboLData = new FormData();
				kosmikVersionComboLData.width = 122;
				kosmikVersionComboLData.height = 14;
				kosmikVersionComboLData.left =  new FormAttachment(0, 1000, 307);
				kosmikVersionComboLData.top =  new FormAttachment(0, 1000, 263);
				kosmikVersionCombo.setLayoutData(kosmikVersionComboLData);
				kosmikVersionCombo.select(1);
				kosmikVersionCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						System.out.println("kosmikVersionCombo.widgetSelected, event="+evt);
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
