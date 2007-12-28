/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.ui.dialog;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
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
			dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					log.finest("dialogShell.widgetDisposed, event=" + evt);
					newValues[0] = new Double(minValueSelect.getText().replace(',', '.'));
					newValues[1] = new Double(maxValueSelect.getText().replace(',', '.'));
				}
			});
			dialogShell.addFocusListener(new FocusAdapter() {
				public void focusGained(FocusEvent evt) {
					log.finest("dialogShell.paintControl, event=" + evt);
					generateAndSetSelectionValues(maxValueSelect, oldMinMax[1], 20);
					generateAndSetSelectionValues(minValueSelect, oldMinMax[0], 20);
				}
			});
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
				canvas.setBackgroundImage(SWTResourceManager.getImage("osde/resource/SmallGraph.gif"));
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
				maxValueSelect.select(7);
				maxValueSelect.addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent evt) {
						log.finest("maxValueSelect.keyPressed, event=" + evt);
						if (evt.character == SWT.CR) {
							dialogShell.dispose();
						}
					}
				});
			}
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
				minValueSelect.addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent evt) {
						log.finest("maxValueSelect.keyPressed, event=" + evt);
						if (evt.character == SWT.CR) {
							dialogShell.dispose();
						}
					}
				});
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

	/**
	 * generates a new string array of size, where the given value is in the array center
	 * set the array to the combo and select the given value
	 * @param combo to be modified
	 * @param value to be used as center
	 * @param size of the array generated
	 */
	private void generateAndSetSelectionValues(final CCombo combo, final double value, int size) {
		size = size + (size % 2) + 1;
		String strDoubleValue = String.format("%.3f", value);
		int intValue = new Double(value).intValue();
		String[] newValues = new String[size];
		for (int i = intValue-size/2, j = size-1; i <= intValue+size/2; i++, j--) {
			newValues[j] = new Integer(i).toString();
		}
		combo.setItems(newValues);
		combo.setItem(10, strDoubleValue);
		combo.select(10);
	}

}
