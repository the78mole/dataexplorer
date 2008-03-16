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

import java.util.HashMap;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.ui.OpenSerialDataExplorer;

/**
 * Display window parent of digital displays
 * @author Winfried Brügmann
 */
public class DigitalWindow {
	private Logger													log	= Logger.getLogger(this.getClass().getName());

	private Composite												digitalMainComposite;
	private TabItem													digitalTab;
	private HashMap<String, DigitalDisplay>	displays;
	private CLabel													infoText;

	private final Channels									channels;
	private final TabFolder									displayTab;
	private RecordSet												oldRecordSet;
	private Channel													oldChannel;

	public DigitalWindow(TabFolder displayTab) {
		this.displayTab = displayTab;
		this.channels = Channels.getInstance();
	}

	public void create() {
		digitalTab = new TabItem(displayTab, SWT.NONE);
		digitalTab.setText("Digital");
		displays = new HashMap<String, DigitalDisplay>(3);
		{
			digitalMainComposite = new Composite(displayTab, SWT.NONE);
			digitalTab.setControl(digitalMainComposite);
			FillLayout composite1Layout = new FillLayout(SWT.HORIZONTAL);
			digitalMainComposite.setLayout(composite1Layout);
			digitalMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.fine("digitalMainComposite.paintControl, event=" + evt);
					update();
				}
			});
			setActiveInfoText("");
			infoText.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.fine("infoText.paintControl, event=" + evt);
					update();
				}
			});
			
			digitalMainComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			digitalMainComposite.layout();
		}
	}

	/**
	 * create new info text
	 */
	private void setActiveInfoText(String info) {
		if (infoText == null || infoText.isDisposed()) {
			infoText = new CLabel(digitalMainComposite, SWT.LEFT);
			infoText.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			infoText.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
			infoText.setBounds(10, 10, 200, 30);
			infoText.setText(info);
		}
	}

	/**
	 * method to update the window with its children
	 */
	public void updateChilds() {
		RecordSet recordSet = channels.getActiveChannel().getActiveRecordSet();
		if (recordSet != null) { // channel does not have a record set yet
			String[] activeRecordKeys = recordSet.getActiveAndVisibleRecordNames();
			for (String recordKey : activeRecordKeys) {
				DigitalDisplay display = displays.get(recordKey);
				if (display != null) display.getDigitalLabel().redraw();
			}
		}
	}

	/**
	 * method to update digital window by adding removing digital displays
	 */
	public void update() {
		Channel activeChannel = channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			// check if just created  or device switched or disabled
			if (recordSet != null && recordSet.get(recordSet.getRecordNames()[0]).getDevice().isDigitalTabRequested()) {
				// if recordSet name signature changed new displays need to be created
				boolean isUpdateRequired = oldRecordSet == null || !recordSet.getName().equals(oldRecordSet.getName())
				|| oldChannel == null  || !oldChannel.getName().equals(activeChannel.getName());
				log.fine("isUpdateRequired = " + isUpdateRequired);
				if (isUpdateRequired) {
					if (!infoText.isDisposed()) infoText.dispose();
					// cleanup
					for (String recordKey : displays.keySet().toArray(new String[0])) {
						DigitalDisplay display = displays.get(recordKey);
						if (display != null) {
							display.dispose();
							displays.remove(recordKey);
						}
					}
					// add new
					for (String recordKey : recordSet.getActiveAndVisibleRecordNames()) {
						DigitalDisplay display = new DigitalDisplay(digitalMainComposite, recordKey, OpenSerialDataExplorer.getInstance().getActiveDevice());
						display.create();
						log.fine("created digital display for " + recordKey);
						displays.put(recordKey, display);
					}
					oldRecordSet = recordSet;
					oldChannel = activeChannel;
				}
			}
			else { // clean up after device switched
				for (String recordKey : displays.keySet().toArray(new String[0])) {
					DigitalDisplay display = displays.get(recordKey);
					if (display != null) {
						log.fine("clean child " + recordKey);
						display.dispose();
						displays.remove(recordKey);
					}
				}
				if (recordSet != null && !recordSet.get(recordSet.getRecordNames()[0]).getDevice().isDigitalTabRequested())
					setActiveInfoText("Die Anzeige ist ausgeschaltet!");
			}
			digitalMainComposite.layout();
		}
	}
}
