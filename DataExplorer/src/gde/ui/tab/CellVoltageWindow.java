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

import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.ui.OpenSerialDataExplorer;

/**
 * Display window parent of cellVoltage displays
 * @author Winfried Brügmann
 */
public class CellVoltageWindow {
	final static Logger											log	= Logger.getLogger(CellVoltageWindow.class.getName());

	Composite												cellVoltageMainComposite, coverComposite;
	TabItem													cellVoltageTab;
	Vector<CellVoltageDisplay>			displays = new Vector<CellVoltageDisplay>();
	CLabel													infoText;
	String 													info = "Die Anzeige ist ausgeschaltet!";

	final Channels									channels;
	final TabFolder									displayTab;
	RecordSet												oldRecordSet = null;
	Channel													oldChannel = null;
	
	int[]														voltageValues = new int[0];
	int 														voltageDelta = 0;
	Point 													displayCompositeSize = new Point(0,0);

	public CellVoltageWindow(TabFolder currentDisplayTab) {
		this.displayTab = currentDisplayTab;
		this.channels = Channels.getInstance();
	}

	public void create() {
		this.cellVoltageTab = new TabItem(this.displayTab, SWT.NONE);
		this.cellVoltageTab.setText("Zellenspannung");
		{
			this.cellVoltageMainComposite = new Composite(this.displayTab, SWT.NONE);
			this.cellVoltageTab.setControl(this.cellVoltageMainComposite);
			this.cellVoltageMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.fine("cellVoltageMainComposite.paintControl, event=" + evt);
					Point mainSize = CellVoltageWindow.this.cellVoltageMainComposite.getSize();
					//log.info("mainSize = " + mainSize.toString());
					Rectangle bounds = new Rectangle(mainSize.x * 5/100, mainSize.y * 10/100
							, mainSize.x * 90/100, mainSize.y * 80/100);
					//log.info("cover bounds = " + bounds.toString());
					CellVoltageWindow.this.coverComposite.setBounds(bounds);
					update();
				}
			});
			setActiveInfoText(this.info);
			this.infoText.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.fine("infoText.paintControl, event=" + evt);
					Point mainSize = CellVoltageWindow.this.cellVoltageMainComposite.getSize();
					//log.info("mainSize = " + mainSize.toString());
					Rectangle bounds = new Rectangle(mainSize.x * 5/100, mainSize.y * 10/100
							, mainSize.x * 90/100, mainSize.y * 80/100);
					//log.info("cover bounds = " + bounds.toString());
					CellVoltageWindow.this.coverComposite.setBounds(bounds);
					update();
				}
			});
			
			this.coverComposite = new Composite(this.cellVoltageMainComposite, SWT.NONE | SWT.BORDER);
			FillLayout fillLayout = new FillLayout(SWT.HORIZONTAL);
			this.coverComposite.setLayout(fillLayout);
			
			this.cellVoltageMainComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.cellVoltageMainComposite.layout();
		}
	}

	/**
	 * create new info text
	 */
	private void setActiveInfoText(String updateInfo) {
		if (this.infoText == null || this.infoText.isDisposed()) {
			this.infoText = new CLabel(this.cellVoltageMainComposite, SWT.LEFT);
			this.infoText.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.infoText.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
			this.infoText.setBounds(10, 10, 200, 30);
			this.infoText.setText(updateInfo);
		}
	}

	/**
	 * method to update the window with its children
	 */
	public void updateChilds(int[] newVoltageValues) {
		this.voltageValues = newVoltageValues;
		this.voltageDelta = calculateVoltageDelta(newVoltageValues);
		RecordSet recordSet = this.channels.getActiveChannel().getActiveRecordSet();
		if (recordSet != null && this.voltageValues.length == this.displays.size()) { // channel does not have a record set yet
			for (int i=0; i<this.voltageValues.length; ++i) {
				this.displays.get(i).setVoltage(i+1, this.voltageValues[i]);
				log.fine("setVoltage cell " + i + " - " + this.voltageValues[i]);
			}
		}
		else {
			update();
		}
	}

	/**
	 * method to update cellVoltage window by adding removing cellVoltage displays
	 */
	public void update() {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			// check if just created  or device switched or disabled
			if (recordSet != null && recordSet.getDevice().isVoltagePerCellTabRequested()) {
				//int[] values = { 4180, 4150, 4190, 4200 }; // four voltage values
				
				//voltageDelta = calculateVoltageDelta(values);

				// if recordSet name signature changed new displays need to be created
				boolean isUpdateRequired = this.oldRecordSet == null || !recordSet.getName().equals(this.oldRecordSet.getName())
				|| this.oldChannel == null  || !this.oldChannel.getName().equals(activeChannel.getName())
						|| this.displays.size() != this.voltageValues.length;
						
				log.fine("isUpdateRequired = " + isUpdateRequired);
				if (isUpdateRequired) {
					// remove into text 
					if (!this.infoText.isDisposed()) this.infoText.dispose();
					// cleanup
					while (this.displays.size() > 0) {
						CellVoltageDisplay display = this.displays.lastElement();
						if (display != null) {
							if (!display.isDisposed()) display.dispose();
							this.displays.remove(display);
						}
					}
					// add new
					for (int i=0; this.voltageValues!=null && i<this.voltageValues.length; ++i) {
						int value = this.voltageValues[i];
						CellVoltageDisplay display = new CellVoltageDisplay(this, this.coverComposite, value);
						display.create();
						display.redraw();
						log.fine("created cellVoltage display for " + value);
						this.displays.add(display);
					}
					this.oldRecordSet = recordSet;
					this.oldChannel = activeChannel;
				}
				//updateChilds(new int[] { 4180, 4150, 4190, 4200 });
				//updateChilds(new int[] { 4100, 4150, 4190, 4200 });
				//updateChilds(new int[] { 2500, 4150, 4100, 3700 });
			}
			else { // clean up after device switched
				while (this.displays.size() > 0) {
					CellVoltageDisplay display = this.displays.lastElement();
					if (display != null) {
						if (!display.isDisposed()) display.dispose();
						this.displays.remove(display);
					}
				}
				if (recordSet != null && !recordSet.getDevice().isVoltagePerCellTabRequested()) {
					if (this.infoText.isDisposed()) setActiveInfoText(this.info);
					else this.infoText.setText(this.info);
				}
			}
			this.cellVoltageMainComposite.layout();
		}
	}

	/**
	 * @param newValues
	 */
	private int calculateVoltageDelta(int[] newValues) {
		int min = newValues[0];
		int max = newValues[0];
		for (int value : newValues) {
			if (value < min) min = value;
			else if (value > max) max = value;
		}
		return max - min;
	}

	/**
	 * @return the voltageDelta
	 */
	public int getVoltageDelta() {
		return this.voltageDelta;
	}

	/**
	 * @return the displayCompositeSize
	 */
	public Point getDisplayCompositeSize() {
		return this.displayCompositeSize;
	}
}
