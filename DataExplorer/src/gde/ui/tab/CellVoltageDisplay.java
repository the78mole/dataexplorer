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
package osde.ui.tab;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import osde.device.IDevice;
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

	private Logger					log	= Logger.getLogger(this.getClass().getName());

	private final CellVoltageWindow parent;
	private Composite				mainComposite;
	private CLabel					textLabel;
	private CLabel					actualDigitalLabel;
	private Canvas				cellCanvas;

	private int							voltage;
	private String					displayText1 = "Zelle ";
	private String					displayText2 = " Spannung [ V ]";
	private String					displayText = displayText1 + "?" + displayText2;

	public CellVoltageDisplay(CellVoltageWindow myParent, Composite cellVoltageMainComposite, int value, IDevice device) {
		super(cellVoltageMainComposite, SWT.BORDER);
		this.parent = myParent;
		//this.setLayout(null);
		//this.setSize(202, 395);
		this.voltage = value;
		this.mainComposite = this;
		this.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
//		this.addPaintListener(new PaintListener() {
//			public void paintControl(PaintEvent evt) {
//				pack(true);
//			}
//		});
	}

	public void create() {
		{
			textLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			textLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 12, 1, false, false));
			textLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			textLabel.setText(displayText);
			textLabel.setBounds(0, 0, 50, 60);
		}
		{
			actualDigitalLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			actualDigitalLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			actualDigitalLabel.setText("0,00");
			actualDigitalLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 32, 0, false, false));
			actualDigitalLabel.setBounds(0, 60, 50, 60);
		}
		{
			cellCanvas = new Canvas(this, SWT.NONE);
			cellCanvas.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			cellCanvas.setBounds(0, 120, 50, 140);
		cellCanvas.addPaintListener(new PaintListener() {
		public void paintControl(PaintEvent evt) {
			Rectangle rect = cellCanvas.getBounds();
			GC gc = SWTResourceManager.getGC(cellCanvas, displayText);
			
			int baseVoltage = 2200;
			if (parent.getVoltageDelta() < 200) 
				baseVoltage = 200;
			
			int height = rect.height; // 4,2 - 2  = 2,2 (max voltage - min voltage)
			
			Double delta = (4200.0 - voltage) * height / baseVoltage;
			int top = delta.intValue();
			
			
			rect = new Rectangle(0, top, rect.width, height);
			log.info("fill rect = " + rect.toString() + " parent.getVoltageDelta() = " + parent.getVoltageDelta());
			
			if (voltage < 2600 || voltage > 4200)
				gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_RED));
			else if (voltage >= 2600 && voltage < 4200)
				gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_YELLOW));
			else // == 4200
				gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_GREEN));
			
			gc.fillRectangle(rect);
		}
	});
		}
		this.layout();
	}

	/**
	 * @param voltage the voltage to set
	 */
	public void setVoltage(int cellNumber, int voltage) {
		this.displayText = displayText1 + cellNumber + displayText2;
		this.voltage = voltage;
		this.update();
	}

	/**
	 * 
	 */
	public void update() {
		log.fine("CellVoltageDisplay.paintControl, voltage = " + voltage);
		Point mainSize = mainComposite.getSize();
		log.info("mainSize = " + mainSize.toString());
		int width = mainSize.x;
		int height = mainSize.y;
		
		textLabel.setText(displayText);
		textLabel.setSize(width, 60);
				
		String valueText = String.format("%.2f", new Double(voltage / 1000.0)); 
		actualDigitalLabel.setText(valueText);
		actualDigitalLabel.setSize(width, 60);
		
		Rectangle rect = new Rectangle(0, 120, width, height-120);
		log.info("cellCanvas = " + rect);
		cellCanvas.setBounds(rect);
	}
}
