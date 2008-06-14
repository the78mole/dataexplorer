/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.ui.dialog;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
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
 * Dialog class to adjust the measurement scale end values
 * @author Winfried Brügmann
 */
public class AxisEndValuesDialog extends org.eclipse.swt.widgets.Dialog {
	final static Logger log = Logger.getLogger(AxisEndValuesDialog.class.getName());

	Shell			dialogShell;
	Canvas		canvas;
	CLabel		minValueLabel;
	CLabel		maxValueLabel;
	Button		okBbutton;
	CCombo		maxValueSelect;
	CCombo		minValueSelect;
	boolean 	isInit = false;
	double[]	newValues	= new double[2];

	/**
	* Auto-generated main method to display this 
	* org.eclipse.swt.widgets.Dialog inside a new Shell.
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			AxisEndValuesDialog inst = new AxisEndValuesDialog(shell, SWT.NULL);
			double[] oldMinMax = {7.0, 1.0};
			System.out.println("newMinMax = " + inst.open( oldMinMax ).toString());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public AxisEndValuesDialog(Shell parent, int style) {
		super(parent, style);
	}

	public double[] open(final double[] oldMinMax) {
		try {
			this.isInit = true;
			Shell parent = getParent();
			this.dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			SWTResourceManager.registerResourceUser(this.dialogShell);
			this.dialogShell.setLayout(new FormLayout());
			this.dialogShell.layout();
			this.dialogShell.pack();
			this.dialogShell.setSize(345, 272);
			this.dialogShell.setText("Achsenendwerte  min / max ");
			this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/Measure.gif"));
			this.dialogShell.setLocation(100, 100);
			this.dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					log.finest("dialogShell.widgetDisposed, event=" + evt);
					AxisEndValuesDialog.this.newValues[0] = new Double(AxisEndValuesDialog.this.minValueSelect.getText().replace(',', '.'));
					AxisEndValuesDialog.this.newValues[1] = new Double(AxisEndValuesDialog.this.maxValueSelect.getText().replace(',', '.'));
				}
			});
			this.dialogShell.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					if (AxisEndValuesDialog.this.isInit) {
						log.info("paintControl.paintControl, event=" + evt);
						generateAndSetSelectionValues(AxisEndValuesDialog.this.maxValueSelect, oldMinMax[1], 20);
						generateAndSetSelectionValues(AxisEndValuesDialog.this.minValueSelect, oldMinMax[0], 20);
					}
					AxisEndValuesDialog.this.isInit = false;
				}
			});
			{
				FormData okBbuttonLData = new FormData();
				okBbuttonLData.width = 79;
				okBbuttonLData.height = 26;
				okBbuttonLData.left = new FormAttachment(0, 1000, 17);
				okBbuttonLData.top = new FormAttachment(0, 1000, 99);
				this.okBbutton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				this.okBbutton.setLayoutData(okBbuttonLData);
				this.okBbutton.setText("OK");
				this.okBbutton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.finest("okBbutton.widgetSelected, event=" + evt);
						AxisEndValuesDialog.this.dialogShell.dispose();
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
				this.canvas = new Canvas(this.dialogShell, SWT.BORDER);
				this.canvas.setLayoutData(canvasLData);
				this.canvas.setBackgroundImage(SWTResourceManager.getImage("osde/resource/SmallGraph.gif"));
			}
			{
				this.maxValueLabel = new CLabel(this.dialogShell, SWT.NONE);
				FormData maxValueLabelLData = new FormData();
				maxValueLabelLData.width = 89;
				maxValueLabelLData.height = 22;
				maxValueLabelLData.left = new FormAttachment(0, 1000, 12);
				maxValueLabelLData.top = new FormAttachment(0, 1000, 12);
				this.maxValueLabel.setLayoutData(maxValueLabelLData);
				this.maxValueLabel.setText("Maximalwert");
			}
			{
				FormData maxValueSelectLData = new FormData();
				maxValueSelectLData.width = 84;
				maxValueSelectLData.height = 17;
				maxValueSelectLData.left = new FormAttachment(0, 1000, 12);
				maxValueSelectLData.top = new FormAttachment(0, 1000, 38);
				this.maxValueSelect = new CCombo(this.dialogShell, SWT.BORDER);
				this.maxValueSelect.setLayoutData(maxValueSelectLData);
				this.maxValueSelect.setItems(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "10", "25", "50", "100" });
				this.maxValueSelect.select(7);
				this.maxValueSelect.addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent evt) {
						log.finest("maxValueSelect.keyPressed, event=" + evt);
						if (evt.character == SWT.CR) {
							AxisEndValuesDialog.this.dialogShell.dispose();
						}
					}
				});
			}
			{
				this.minValueLabel = new CLabel(this.dialogShell, SWT.NONE);
				FormData cLabel1LData = new FormData();
				cLabel1LData.width = 89;
				cLabel1LData.height = 22;
				cLabel1LData.left = new FormAttachment(0, 1000, 12);
				cLabel1LData.top = new FormAttachment(0, 1000, 169);
				this.minValueLabel.setLayoutData(cLabel1LData);
				this.minValueLabel.setText("Minimalwert");
			}
			{
				FormData minValueSelectLData = new FormData();
				minValueSelectLData.width = 84;
				minValueSelectLData.height = 17;
				minValueSelectLData.left = new FormAttachment(0, 1000, 12);
				minValueSelectLData.top = new FormAttachment(0, 1000, 195);
				this.minValueSelect = new CCombo(this.dialogShell, SWT.BORDER);
				this.minValueSelect.setLayoutData(minValueSelectLData);
				this.minValueSelect.setItems(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "10", "25", "50", "100" });
				this.minValueSelect.select(5);
				this.minValueSelect.addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent evt) {
						log.finest("maxValueSelect.keyPressed, event=" + evt);
						if (evt.character == SWT.CR) {
							AxisEndValuesDialog.this.dialogShell.dispose();
						}
					}
				});
			}
			this.dialogShell.setLocation(getParent().toDisplay(100, 100));
			this.dialogShell.open();
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return this.newValues;
	}

	/**
	 * generates a new string array of size, where the given value is in the array center
	 * set the array to the combo and select the given value
	 * @param combo to be modified
	 * @param value to be used as center
	 * @param size of the array generated
	 */
	void generateAndSetSelectionValues(final CCombo combo, final double value, int size) {
		int tmpSize = size + (size % 2) + 1;
		String strDoubleValue = String.format("%.3f", value);
		int intValue = new Double(value).intValue();
		String[] tmpValues = new String[tmpSize];
		for (int i = intValue-tmpSize/2, j = tmpSize-1; i <= intValue+tmpSize/2; i++, j--) {
			tmpValues[j] = new Integer(i).toString();
		}
		combo.setItems(tmpValues);
		combo.setItem(10, strDoubleValue);
		combo.select(10);
	}

}
