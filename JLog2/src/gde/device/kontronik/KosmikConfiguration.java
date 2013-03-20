package gde.device.kontronik;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.SWT;


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
	private Label rpmLabel;
	private Label numPolsLabel;
	private Label motorPinionLabel;
	private Label kosmikVersionLabel;
	private Label temperatureUnitLabel;
	private Label mainGearToothCountLabel;

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

	public KosmikConfiguration(org.eclipse.swt.widgets.Composite parent, int style) {
		super(parent, style);
		initGUI();
	}

	private void initGUI() {
		try {
			FormLayout thisLayout = new FormLayout();
			this.setLayout(new FormLayout());
			this.setSize(458, 429);
			{
				kosmikVersionLabel = new Label(this, SWT.NONE);
				kosmikVersionLabel.setText("Kosmik Version:");
				FormData kosmikVersionLabelLData = new FormData();
				kosmikVersionLabelLData.width = 96;
				kosmikVersionLabelLData.height = 16;
				kosmikVersionLabelLData.left =  new FormAttachment(0, 1000, 26);
				kosmikVersionLabelLData.top =  new FormAttachment(0, 1000, 268);
				kosmikVersionLabel.setLayoutData(kosmikVersionLabelLData);
			}
			{
				temperatureUnitLabel = new Label(this, SWT.NONE);
				temperatureUnitLabel.setText("Temperatur(e):");
				FormData temperatureUnitLabelLData = new FormData();
				temperatureUnitLabelLData.width = 89;
				temperatureUnitLabelLData.height = 16;
				temperatureUnitLabelLData.left =  new FormAttachment(0, 1000, 26);
				temperatureUnitLabelLData.top =  new FormAttachment(0, 1000, 223);
				temperatureUnitLabel.setLayoutData(temperatureUnitLabelLData);
			}
			{
				mainGearToothCountLabel = new Label(this, SWT.NONE);
				mainGearToothCountLabel.setText("Hauptrotorzähneanzahl | Main gear tooth count:");
				FormData mainGearToothCountLabelLData = new FormData();
				mainGearToothCountLabelLData.width = 277;
				mainGearToothCountLabelLData.height = 22;
				mainGearToothCountLabelLData.left =  new FormAttachment(0, 1000, 26);
				mainGearToothCountLabelLData.top =  new FormAttachment(0, 1000, 169);
				mainGearToothCountLabel.setLayoutData(mainGearToothCountLabelLData);
			}
			{
				motorPinionLabel = new Label(this, SWT.NONE);
				motorPinionLabel.setText("Motorritzel | Motorpinion tooth count:");
				FormData motorPinionLabelLData = new FormData();
				motorPinionLabelLData.width = 210;
				motorPinionLabelLData.height = 16;
				motorPinionLabelLData.left =  new FormAttachment(0, 1000, 26);
				motorPinionLabelLData.top =  new FormAttachment(0, 1000, 122);
				motorPinionLabel.setLayoutData(motorPinionLabelLData);
			}
			{
				numPolsLabel = new Label(this, SWT.NONE);
				numPolsLabel.setText("Anzahl Motorpole");
				FormData numPolsLabelLData = new FormData();
				numPolsLabelLData.width = 104;
				numPolsLabelLData.height = 16;
				numPolsLabelLData.left =  new FormAttachment(0, 1000, 26);
				numPolsLabelLData.top =  new FormAttachment(0, 1000, 76);
				numPolsLabel.setLayoutData(numPolsLabelLData);
			}
			{
				rpmLabel = new Label(this, SWT.NONE);
				rpmLabel.setText("Motor- Hauptrotordrehzahl");
				FormData rpmLabelLData = new FormData();
				rpmLabelLData.width = 155;
				rpmLabelLData.height = 16;
				rpmLabelLData.left =  new FormAttachment(0, 1000, 26);
				rpmLabelLData.top =  new FormAttachment(0, 1000, 39);
				rpmLabel.setLayoutData(rpmLabelLData);
			}
			this.layout();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
