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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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

	final static Logger	log						= Logger.getLogger(CellVoltageDisplay.class.getName());

	CLabel							textLabel;
	CLabel							actualDigitalLabel;
	Canvas							cellCanvas;
	Composite						fillRight;
	Composite						fillLeft;
	Composite						cellComposite;

	int									voltage;
	String							displayText1	= "Zelle ";
	String							displayText2	= " Spannung [ V ]";
	String							displayText		= this.displayText1 + "?" + this.displayText2;

	public CellVoltageDisplay(Composite cellVoltageMainComposite, int value) {
		super(cellVoltageMainComposite, SWT.BORDER);
		this.voltage = value;
		this.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
		GridLayout mainCompositeLayout = new GridLayout();
		mainCompositeLayout.makeColumnsEqualWidth = true;
		mainCompositeLayout.marginHeight = 0;
		mainCompositeLayout.marginWidth = 0;
		this.setLayout(mainCompositeLayout);
	}

	public void create() {
		{
			this.textLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			this.textLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 12, 1, false, false));
			this.textLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.textLabel.setText(this.displayText);
			GridData text1LData = new GridData();
			text1LData.horizontalAlignment = GridData.FILL;
			text1LData.grabExcessHorizontalSpace = true;
			this.textLabel.setLayoutData(text1LData);
		}
		{
			this.actualDigitalLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			this.actualDigitalLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.actualDigitalLabel.setText("0,00");
			this.actualDigitalLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 32, 0, false, false));
			GridData actualDigitalLabelLData = new GridData();
			actualDigitalLabelLData.horizontalAlignment = GridData.FILL;
			actualDigitalLabelLData.grabExcessHorizontalSpace = true;
			this.actualDigitalLabel.setLayoutData(actualDigitalLabelLData);
			//this.actualDigitalLabel.setBounds(0, 60, 50, 60);
		}
		{
			this.cellComposite = new Composite(this, SWT.NONE);
			GridData canvas1LData = new GridData();
			canvas1LData.horizontalAlignment = GridData.FILL;
			canvas1LData.grabExcessHorizontalSpace = true;
			canvas1LData.grabExcessVerticalSpace = true;
			canvas1LData.verticalAlignment = GridData.FILL;
			this.cellComposite.setLayoutData(canvas1LData);
			FillLayout canvas1Layout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
			canvas1Layout.marginHeight = 0;
			canvas1Layout.marginWidth = 0;
			this.cellComposite.setLayout(canvas1Layout);
			{
				this.fillLeft = new Composite(this.cellComposite, SWT.NONE);
				//				fillLeft.setVisible(false);
				//				fillLeft.setDragDetect(false);
				//				fillLeft.setEnabled(false);
				this.fillLeft.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			}
			{
				this.cellCanvas = new Canvas(this.cellComposite, SWT.BORDER);
				this.cellCanvas.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				//				this.cellCanvas.setDragDetect(false);
				this.cellCanvas.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						CellVoltageDisplay.log.fine("cellCanvas.paintControl, evt = " + evt);
						voltagePaintControl();
					}
				});
			}
			{
				this.fillRight = new Composite(this.cellComposite, SWT.NONE);
				//				fillRight.setEnabled(false);
				//				fillRight.setDragDetect(false);
				//				fillRight.setVisible(false);
				this.fillRight.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			}
		}
		this.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent evt) {
				CellVoltageDisplay.log.fine("mainComposite.paintControl, evt = " + evt);
				voltagePaintControl();
			}
		});
		this.layout();
	}

	/**
	 * @param newVoltage the voltage to set
	 */
	public void setVoltage(int cellNumber, int newVoltage) {
		this.displayText = this.displayText1 + cellNumber + this.displayText2;
		this.voltage = newVoltage;

		log.info("this = " + this.getSize());
		log.info("this.cellComposite = " + this.cellComposite.getSize());
		log.info("this.textLabel = " + this.textLabel.getSize());
		log.info("this.actualDigitalLabel = " + this.actualDigitalLabel.getSize());
		log.info("this.cellCanvas = " + this.cellCanvas.getSize());
		//this.update();
	}

	//	/**
	//	 * 
	//	 */
	//	public void update() {
	//		log.fine("CellVoltageDisplay.paintControl, voltage = " + this.voltage);
	//		Point mainSize = this.mainComposite.getSize();
	//		log.fine("mainSize = " + mainSize.toString());
	//		int width = mainSize.x;
	//		int height = mainSize.y;
	//		
	//		this.textLabel.setText(this.displayText);
	//		this.textLabel.setSize(width, 60);
	//				
	//		String valueText = String.format("%.2f", new Double(this.voltage / 1000.0)); 
	//		this.actualDigitalLabel.setText(valueText);
	//		this.actualDigitalLabel.setSize(width, 60);
	//		
	//		Rectangle rect = new Rectangle(0, 120, width, height-120);
	//		log.fine("cellCanvas = " + rect);
	//		this.cellCanvas.setBounds(rect);
	//		this.cellCanvas.redraw();
	//	}

	/**
	 * 
	 */
	void voltagePaintControl() {
		this.textLabel.setText(this.displayText);

		String valueText = String.format("%.2f", new Double(this.voltage / 1000.0));
		this.actualDigitalLabel.setText(valueText);

		Rectangle rect = CellVoltageDisplay.this.cellCanvas.getBounds();
		GC gc = SWTResourceManager.getGC(CellVoltageDisplay.this.cellCanvas, CellVoltageDisplay.this.displayText);

		int baseVoltage = 2500;
		//		int cellVoltageDelta = CellVoltageDisplay.this.parent.getVoltageDelta();
		//		if (cellVoltageDelta < 200 && cellVoltageDelta != 0) 
		//			baseVoltage = 1000;

		int height = rect.height; // 4,2 - 2  = 2,2 (max voltage - min voltage)

		Double delta = (4200.0 - CellVoltageDisplay.this.voltage) * (height - 20) / baseVoltage;
		int top = delta.intValue();

		rect = new Rectangle(0, top, rect.width, height);

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
