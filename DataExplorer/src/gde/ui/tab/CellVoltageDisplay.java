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
package osde.ui.tab;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Child display class displaying cell voltage display
 * @author Winfried Brügmann
 */
public class CellVoltageDisplay extends Composite {

	{
//Register as a resource user - SWTResourceManager will
//handle the obtaining and disposing of resources
		SWTResourceManager.registerResourceUser(this);
	}

	final static Logger			log	= Logger.getLogger(CellVoltageDisplay.class.getName());

	final CellVoltageWindow parent;
	Composite				mainComposite;
	CLabel					textLabel;
	CLabel					actualDigitalLabel;
	Canvas				cellCanvas;

	int							voltage;
	String					displayText1 = "Zelle ";
	String					displayText2 = " Spannung [ V ]";
	String					displayText = this.displayText1 + "?" + this.displayText2;

	public CellVoltageDisplay(CellVoltageWindow asParent, Composite cellVoltageMainComposite, int value) {
		super(cellVoltageMainComposite, SWT.BORDER);
		this.parent = asParent;
		this.voltage = value;
		this.mainComposite = this;
		this.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
		GridData blockCellLayoutData = new GridData();
		blockCellLayoutData.grabExcessVerticalSpace = true;
		blockCellLayoutData.grabExcessHorizontalSpace = true;
		blockCellLayoutData.verticalAlignment = GridData.FILL;
		blockCellLayoutData.horizontalAlignment = GridData.FILL;
		this.setLayoutData(blockCellLayoutData);
		this.setLayout(null);
	}

	public void create() {
		{
			this.textLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			this.textLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 12, 1, false, false));
			this.textLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.textLabel.setText(this.displayText);
			this.textLabel.setBounds(0, 0, 50, 60);
		}
		{
			this.actualDigitalLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			this.actualDigitalLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.actualDigitalLabel.setText("0,00");
			this.actualDigitalLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 32, 0, false, false));
			this.actualDigitalLabel.setBounds(0, 60, 50, 60);
		}
		{
			this.cellCanvas = new Canvas(this, SWT.NONE);
			this.cellCanvas.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.cellCanvas.setBounds(0, 120, 50, 140);
			this.cellCanvas.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.fine("cellCanvas.paintControl, evt = " + evt);
					voltagePaintControl();
				}
			});
		}
//		this.addPaintListener(new PaintListener() {
//			public void paintControl(PaintEvent evt) {
//				log.fine("mainComposite.paintControl, evt = " + evt);
//				voltagePaintControl();
//			}
//		});
		this.layout();
	}

	/**
	 * @param newVoltage the voltage to set
	 */
	public void setVoltage(int cellNumber, int newVoltage) {
		this.displayText = this.displayText1 + cellNumber + this.displayText2;
		this.voltage = newVoltage;
		this.update();
	}

	/**
	 * 
	 */
	public void update() {
		log.fine("CellVoltageDisplay.paintControl, voltage = " + this.voltage);
		Point mainSize = this.mainComposite.getSize();
		log.fine("mainSize = " + mainSize.toString());
		int width = mainSize.x;
		int height = mainSize.y;
		
		this.textLabel.setText(this.displayText);
		this.textLabel.setSize(width, 60);
				
		String valueText = String.format("%.2f", new Double(this.voltage / 1000.0)); 
		this.actualDigitalLabel.setText(valueText);
		this.actualDigitalLabel.setSize(width, 60);
		
		Rectangle rect = new Rectangle(0, 120, width, height-120);
		log.fine("cellCanvas = " + rect);
		this.cellCanvas.setBounds(rect);
		this.cellCanvas.redraw();
	}

	/**
	 * 
	 */
	void voltagePaintControl() {
		Rectangle rect = CellVoltageDisplay.this.cellCanvas.getBounds();
		GC gc = SWTResourceManager.getGC(CellVoltageDisplay.this.cellCanvas, CellVoltageDisplay.this.displayText);

		int baseVoltage = 2500;
//		int cellVoltageDelta = CellVoltageDisplay.this.parent.getVoltageDelta();
//		if (cellVoltageDelta < 200 && cellVoltageDelta != 0) 
//			baseVoltage = 1000;

		int height = rect.height; // 4,2 - 2  = 2,2 (max voltage - min voltage)

		Double delta = (4200.0 - CellVoltageDisplay.this.voltage) * (height-20) / baseVoltage;
		int top = delta.intValue();

		rect = new Rectangle(0, top, rect.width, height);
		log.fine("fill rect = " + rect.toString() + " parent.getVoltageDelta() = " + CellVoltageDisplay.this.parent.getVoltageDelta());

		if (CellVoltageDisplay.this.voltage < 2600 || CellVoltageDisplay.this.voltage > 4200)
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_RED));
		else if (CellVoltageDisplay.this.voltage >= 2600 && CellVoltageDisplay.this.voltage < 4200)
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_YELLOW));
		else
			// == 4200
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_GREEN));

		gc.fillRectangle(rect);
	}
}
