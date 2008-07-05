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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import osde.messages.MessageIds;
import osde.messages.Messages;
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

	CLabel							cellTextLabel;
	CLabel							cellVoltageDigitalLabel;
	Canvas							cellCanvas;
	Composite						fillRight;
	Composite						fillLeft;
	Composite						cellComposite;

	int									voltage;
	String							displayText1	= Messages.getString(MessageIds.OSDE_MSGT0230);
	String							displayText2	= Messages.getString(MessageIds.OSDE_MSGT0231);
	String							displayText		= this.displayText1 + "?" + this.displayText2; //$NON-NLS-1$
	int 								lastTop = 0;
	int 								lastVoltageLevel = 0;

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
			this.cellTextLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			this.cellTextLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 12, 1, false, false)); //$NON-NLS-1$
			this.cellTextLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.cellTextLabel.setText(this.displayText);
			GridData text1LData = new GridData();
			text1LData.horizontalAlignment = GridData.FILL;
			text1LData.grabExcessHorizontalSpace = true;
			this.cellTextLabel.setLayoutData(text1LData);
		}
		{
			this.cellVoltageDigitalLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			this.cellVoltageDigitalLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.cellVoltageDigitalLabel.setText("0,00"); //$NON-NLS-1$
			this.cellVoltageDigitalLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 32, 0, false, false)); //$NON-NLS-1$
			GridData actualDigitalLabelLData = new GridData();
			actualDigitalLabelLData.horizontalAlignment = GridData.FILL;
			actualDigitalLabelLData.grabExcessHorizontalSpace = true;
			this.cellVoltageDigitalLabel.setLayoutData(actualDigitalLabelLData);
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
				this.fillLeft.setDragDetect(false);
				this.fillLeft.setEnabled(false);
				this.fillLeft.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			}
			{
				this.cellCanvas = new Canvas(this.cellComposite, SWT.NONE);
				this.cellCanvas.setBackground(OpenSerialDataExplorer.COLOR_GREY);
				this.cellCanvas.setDragDetect(false);
				this.cellCanvas.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						CellVoltageDisplay.log.fine("cellCanvas.paintControl, evt = " + evt); //$NON-NLS-1$
						voltagePaintControl();
					}
				});
			}
			{
				this.fillRight = new Composite(this.cellComposite, SWT.NONE);
				this.fillRight.setEnabled(false);
				this.fillRight.setDragDetect(false);
				this.fillRight.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			}
		}
//		this.addPaintListener(new PaintListener() {
//			public void paintControl(PaintEvent evt) {
//				CellVoltageDisplay.log.fine("mainComposite.paintControl, evt = " + evt);
//				CellVoltageDisplay.this.cellTextLabel.redraw();
//				CellVoltageDisplay.this.cellVoltageDigitalLabel.redraw();
//				CellVoltageDisplay.this.cellCanvas.redraw();
//			}
//		});
		this.layout();
	}

	/**
	 * @param newVoltage the voltage to set
	 */
	public void setVoltage(int cellNumber, int newVoltage) {
		this.layout(true);
		boolean isUpdateRequired = false;
		if (!this.displayText.equals(this.displayText1 + cellNumber + this.displayText2)) {
			this.displayText = this.displayText1 + cellNumber + this.displayText2;
			this.cellTextLabel.setText(this.displayText);
			isUpdateRequired = true;
		}
		if (this.voltage != newVoltage) {
			this.voltage = newVoltage;
			String valueText = String.format("%.2f", new Double(this.voltage / 1000.0)); //$NON-NLS-1$
			this.cellVoltageDigitalLabel.setText(valueText);
			isUpdateRequired = true;
		}

		if (isUpdateRequired) {
			Rectangle rect = this.cellCanvas.getClientArea();
			Point topHeight = calculateBarGraph(rect);
			if (this.lastVoltageLevel != checkVoltageLevel()) 
				this.cellComposite.redraw();
			else if (this.lastTop < topHeight.y)
				this.cellComposite.redraw(0, this.lastTop-1, rect.width-1, topHeight.x+1, true); 
			else
				this.cellComposite.redraw(0, topHeight.x-1, rect.width-1, this.lastTop+1, true); 
		}
	}

	/**
	 * 
	 */
	void voltagePaintControl() {
		this.cellTextLabel.setText(this.displayText);
		String valueText = String.format("%.2f", new Double(this.voltage / 1000.0)); //$NON-NLS-1$
		this.cellVoltageDigitalLabel.setText(valueText);

		Rectangle rect = this.cellCanvas.getClientArea();
		if (log.isLoggable(Level.FINE)) CellVoltageDisplay.log.fine("cellCanvas.getBounds = " + rect); //$NON-NLS-1$
		// using hashCode and size as qualifier will re-use the GC if only voltage values changed
		GC gc = SWTResourceManager.getGC(this.cellCanvas, this.cellCanvas.hashCode() + "_" + rect.width + "_" + rect.height); //$NON-NLS-1$ //$NON-NLS-2$
		if (log.isLoggable(Level.FINE)) log.fine(this.cellCanvas.hashCode() + "_" + rect.width + "_" + rect.height); //$NON-NLS-1$ //$NON-NLS-2$
		Point topHeight = calculateBarGraph(rect);
		rect = new Rectangle(0, topHeight.x, rect.width-1, topHeight.y);
		this.lastTop = topHeight.x;

		this.lastVoltageLevel = checkVoltageLevel();
		switch (this.lastVoltageLevel) {
		case 1: // < 2600 || > 4200
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_RED));
			break;
		default:
		case 2: //2600 - 4200
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_YELLOW));
			break;
		case 3: //== 4200
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_GREEN));
			break;
		}

		if (log.isLoggable(Level.FINE)) CellVoltageDisplay.log.fine("fillRectangle = " + rect); //$NON-NLS-1$
		gc.fillRectangle(1, topHeight.x+2, rect.width-1, topHeight.y-1);
		gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		gc.drawLine(1, topHeight.x, rect.width-1, topHeight.x);
		gc.drawRectangle(0, 0, rect.width, rect.height+topHeight.x);
	}

	/**
	 * @return 1 if voltage <2600 | >4200, 2 if voltage between 2600 and 4200, 3 for voltage == 4200
	 */
	int checkVoltageLevel() {
		int voltageLevel;
		if (CellVoltageDisplay.this.voltage < 2600 || CellVoltageDisplay.this.voltage > 4200) {
			voltageLevel = 1;
		}
		else if (CellVoltageDisplay.this.voltage >= 2600 && CellVoltageDisplay.this.voltage < 4200) {
			voltageLevel = 2;
		}
		else { // == 4200
			voltageLevel = 3;
		}
		return voltageLevel;
	}

	/**
	 * @param cellCanvasBounds
	 * @return top limit and botom line position within the cellCanvas bounds
	 */
	Point calculateBarGraph(Rectangle cellCanvasBounds) {
		Point topHeight = new Point(0,0);
		int baseVoltage = 2500;

		topHeight.y = cellCanvasBounds.height; // 4,2 - 2  = 2,2 (max voltage - min voltage)
		Double delta = (4200.0 - CellVoltageDisplay.this.voltage) * topHeight.y / baseVoltage;
		
		topHeight.x = delta.intValue();
		topHeight.y = topHeight.y-1-topHeight.x;
		if (log.isLoggable(Level.FINER)) log.finer(topHeight.toString());
		return topHeight;
	}
}
