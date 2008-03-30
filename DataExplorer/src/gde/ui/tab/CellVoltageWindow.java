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
	private Logger													log	= Logger.getLogger(this.getClass().getName());

	private Composite												cellVoltageMainComposite, coverComposite;
	private TabItem													cellVoltageTab;
	private Vector<CellVoltageDisplay>			displays = new Vector<CellVoltageDisplay>();
	private CLabel													infoText;
	private String 													info = "Die Anzeige ist ausgeschaltet!";

	private final Channels									channels;
	private final TabFolder									displayTab;
	private RecordSet												oldRecordSet;
	private Channel													oldChannel;
	
	private int[]														values;
	private int 														voltageDelta = 0;

	public CellVoltageWindow(TabFolder displayTab) {
		this.displayTab = displayTab;
		this.channels = Channels.getInstance();
	}

	public void create() {
		cellVoltageTab = new TabItem(displayTab, SWT.NONE);
		cellVoltageTab.setText("Zellenspannung");
		{
			cellVoltageMainComposite = new Composite(displayTab, SWT.NONE);
			cellVoltageTab.setControl(cellVoltageMainComposite);
			cellVoltageMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.fine("cellVoltageMainComposite.paintControl, event=" + evt);
					Point mainSize = cellVoltageMainComposite.getSize();
					//log.info("mainSize = " + mainSize.toString());
					Rectangle bounds = new Rectangle(mainSize.x * 5/100, mainSize.y * 10/100
							, mainSize.x * 90/100, mainSize.y * 80/100);
					//log.info("cover bounds = " + bounds.toString());
					coverComposite.setBounds(bounds);
					update();
				}
			});
			setActiveInfoText(info);
			infoText.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.fine("infoText.paintControl, event=" + evt);
					Point mainSize = cellVoltageMainComposite.getSize();
					//log.info("mainSize = " + mainSize.toString());
					Rectangle bounds = new Rectangle(mainSize.x * 5/100, mainSize.y * 10/100
							, mainSize.x * 90/100, mainSize.y * 80/100);
					//log.info("cover bounds = " + bounds.toString());
					coverComposite.setBounds(bounds);
					update();
				}
			});
			
			coverComposite = new Composite(cellVoltageMainComposite, SWT.NONE | SWT.BORDER);
			FillLayout fillLayout = new FillLayout(SWT.HORIZONTAL);
			coverComposite.setLayout(fillLayout);
			
			cellVoltageMainComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			cellVoltageMainComposite.layout();
		}
	}

	/**
	 * create new info text
	 */
	private void setActiveInfoText(String info) {
		if (infoText == null || infoText.isDisposed()) {
			infoText = new CLabel(cellVoltageMainComposite, SWT.LEFT);
			infoText.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			infoText.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
			infoText.setBounds(10, 10, 200, 30);
			infoText.setText(info);
		}
	}

	/**
	 * method to update the window with its children
	 */
	public void updateChilds(int[] voltageValues) {
		voltageDelta = calculateVoltageDelta(voltageValues);
		RecordSet recordSet = channels.getActiveChannel().getActiveRecordSet();
		if (recordSet != null && voltageValues.length == displays.size()) { // channel does not have a record set yet
			for (int i=0; i<displays.size() && i<voltageValues.length; ++i) {
				displays.get(i).setVoltage(i+1, voltageValues[i]);
				log.info("setVoltage cell " + i);
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
		Channel activeChannel = channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			// check if just created  or device switched or disabled
			if (recordSet != null && recordSet.get(recordSet.getRecordNames()[0]).getDevice().isVoltagePerCellTabRequested()) {
				//int[] values = { 4180, 4150, 4190, 4200 }; // four voltage values
				
				//voltageDelta = calculateVoltageDelta(values);

				// if recordSet name signature changed new displays need to be created
				boolean isUpdateRequired = oldRecordSet == null || !recordSet.getName().equals(oldRecordSet.getName())
				|| oldChannel == null  || !oldChannel.getName().equals(activeChannel.getName());
						
				log.fine("isUpdateRequired = " + isUpdateRequired);
				if (isUpdateRequired) {
					// remove into text 
					if (!infoText.isDisposed()) infoText.dispose();
					// cleanup
					for (int i=0; i<displays.size(); ++i) {
						CellVoltageDisplay display = displays.firstElement();
						if (display != null) {
							if (!display.isDisposed()) display.dispose();
							displays.remove(display);
						}
					}
					// add new
					for (int i=0; values!=null && i<values.length; ++i) {
						int value = values[i];
						CellVoltageDisplay display = new CellVoltageDisplay(this, coverComposite, value, OpenSerialDataExplorer.getInstance().getActiveDevice());
						display.create();
						log.fine("created cellVoltage display for " + value);
						displays.add(display);
					}
					oldRecordSet = recordSet;
					oldChannel = activeChannel;
				}
				//updateChilds(new int[] { 4180, 4150, 4190, 4200 });
				//updateChilds(new int[] { 4100, 4150, 4190, 4200 });
				//updateChilds(new int[] { 2500, 4150, 4100, 3700 });
			}
			else { // clean up after device switched
				for (int i=0; i<displays.size(); ++i) {
					CellVoltageDisplay display = displays.firstElement();
					if (display != null) {
						if (!display.isDisposed()) display.dispose();
						displays.remove(display);
					}
				}
				if (recordSet != null && !recordSet.get(recordSet.getRecordNames()[0]).getDevice().isVoltagePerCellTabRequested()) {
					if (infoText.isDisposed()) setActiveInfoText(info);
					else infoText.setText(info);
				}
			}
			cellVoltageMainComposite.layout();
		}
	}

	/**
	 * @param values
	 */
	private int calculateVoltageDelta(int[] values) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;;
		for (int value : values) {
			if (value < min) min = value;
			else if (value > max) max = value;
		}
		return max - min;
	}

	/**
	 * @return the voltageDelta
	 */
	public int getVoltageDelta() {
		return voltageDelta;
	}
}
