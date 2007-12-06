package osde.ui.dialog;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import osde.ui.SWTResourceManager;

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
public class AxisEndValuesDialog extends org.eclipse.swt.widgets.Dialog {
	private Logger log = Logger.getLogger(this.getClass().getName());

	private Shell			dialogShell;
	private Canvas		canvas;
	private CLabel		minValueLabel;
	private CLabel		maxValueLabel;
	private Button		okBbutton;
	private CCombo		maxValueSelect;
	private CCombo		minValueSelect;
	private double[]	newValues	= new double[2];

	/**
	* Auto-generated main method to display this 
	* org.eclipse.swt.widgets.Dialog inside a new Shell.
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			AxisEndValuesDialog inst = new AxisEndValuesDialog(shell, SWT.NULL);
			inst.open();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public AxisEndValuesDialog(Shell parent, int style) {
		super(parent, style);
	}

	public double[] open() {
		try {
			Shell parent = getParent();
			dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

			{
				//Register as a resource user - SWTResourceManager will
				//handle the obtaining and disposing of resources
				SWTResourceManager.registerResourceUser(dialogShell);
			}

			dialogShell.setLayout(new FormLayout());
			dialogShell.layout();
			dialogShell.pack();
			dialogShell.setSize(345, 272);
			dialogShell.setText("Achsenendwerte  min / max ");
			dialogShell.setLocation(100, 100);
			{
				FormData okBbuttonLData = new FormData();
				okBbuttonLData.width = 79;
				okBbuttonLData.height = 26;
				okBbuttonLData.left = new FormAttachment(0, 1000, 17);
				okBbuttonLData.top = new FormAttachment(0, 1000, 99);
				okBbutton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				okBbutton.setLayoutData(okBbuttonLData);
				okBbutton.setText("OK");
				okBbutton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.finest("okBbutton.widgetSelected, event=" + evt);
						newValues[0] = new Double(minValueSelect.getText().replace(',', '.'));
						newValues[1] = new Double(maxValueSelect.getText().replace(',', '.'));
						dialogShell.dispose();
					}
				});
			}
			{
				FormData canvasLData = new FormData();
				canvasLData.width = 200;
				canvasLData.height = 200;
				canvasLData.top = new FormAttachment(0, 1000, 18);
				canvasLData.right = new FormAttachment(1000, 1000, -24);
				canvasLData.bottom = new FormAttachment(1000, 1000, -21);
				canvasLData.left = new FormAttachment(0, 1000, 113);
				canvas = new Canvas(dialogShell, SWT.BORDER);
				canvas.setLayoutData(canvasLData);
				canvas.setBackgroundImage(SWTResourceManager.getImage("osde/resource/SmallGraph.jpg"));
			}
			{
				maxValueLabel = new CLabel(dialogShell, SWT.NONE);
				FormData maxValueLabelLData = new FormData();
				maxValueLabelLData.width = 89;
				maxValueLabelLData.height = 22;
				maxValueLabelLData.left = new FormAttachment(0, 1000, 12);
				maxValueLabelLData.top = new FormAttachment(0, 1000, 12);
				maxValueLabel.setLayoutData(maxValueLabelLData);
				maxValueLabel.setText("Maximalwert");
			}
			{
				FormData maxValueSelectLData = new FormData();
				maxValueSelectLData.width = 84;
				maxValueSelectLData.height = 17;
				maxValueSelectLData.left = new FormAttachment(0, 1000, 12);
				maxValueSelectLData.top = new FormAttachment(0, 1000, 38);
				maxValueSelect = new CCombo(dialogShell, SWT.BORDER);
				maxValueSelect.setLayoutData(maxValueSelectLData);
				maxValueSelect.setItems(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "10", "25", "50", "100" });
				maxValueSelect.select(7);			}
			{
				minValueLabel = new CLabel(dialogShell, SWT.NONE);
				FormData cLabel1LData = new FormData();
				cLabel1LData.width = 89;
				cLabel1LData.height = 22;
				cLabel1LData.left = new FormAttachment(0, 1000, 12);
				cLabel1LData.top = new FormAttachment(0, 1000, 169);
				minValueLabel.setLayoutData(cLabel1LData);
				minValueLabel.setText("Minimalwert");
			}
			{
				FormData minValueSelectLData = new FormData();
				minValueSelectLData.width = 84;
				minValueSelectLData.height = 17;
				minValueSelectLData.left = new FormAttachment(0, 1000, 12);
				minValueSelectLData.top = new FormAttachment(0, 1000, 195);
				minValueSelect = new CCombo(dialogShell, SWT.BORDER);
				minValueSelect.setLayoutData(minValueSelectLData);
				minValueSelect.setItems(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "10", "25", "50", "100" });
				minValueSelect.select(5);
			}
			dialogShell.setLocation(getParent().toDisplay(100, 100));
			dialogShell.open();
			Display display = dialogShell.getDisplay();
			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return newValues;
	}

}
