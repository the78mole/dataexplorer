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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

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

	private final Channels									channels;
	private final TabFolder									displayTab;
	private RecordSet												oldRecordSet;

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
			log.fine("digitalMainComposite " + digitalMainComposite.getBounds().toString());
			digitalMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.finest("digitalMainComposite.paintControl, event=" + evt);
					update();
				}
			});
			digitalMainComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			digitalMainComposite.layout();
		}
	}

	/**
	 * method to update the window with its children
	 */
	public synchronized void updateChilds() {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				RecordSet recordSet = channels.getActiveChannel().getActiveRecordSet();
				if (recordSet != null) { // channel does not have a record set yet
					String[] activeRecordKeys = recordSet.getActiveRecordNames();
					for (String recordKey : activeRecordKeys) {
						DigitalDisplay display = displays.get(recordKey);
						if (display != null) display.getDigitalLabel().redraw();
					}
				}
			}
		});
	}

	/**
	 * method to update digital window
	 */
	public synchronized void update() {
		final RecordSet recordSet = channels.getActiveChannel().getActiveRecordSet();
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				if (recordSet != null) { // just created  or device switched
					// if recordSet name signature changed new displays need to be created
					if (oldRecordSet == null || !recordSet.keySet().toString().equals(oldRecordSet.keySet().toString())) {
						oldRecordSet = recordSet;
						String[] activeRecordKeys = recordSet.getActiveRecordNames();
						for (String recordKey : activeRecordKeys) {
							DigitalDisplay display = new DigitalDisplay(digitalMainComposite, recordKey, OpenSerialDataExplorer.getInstance().getActiveDevice());
							display.create();
							log.fine("created digital display for " + recordKey);
							displays.put(recordKey, display);
						}
						digitalMainComposite.layout();
					}
				}
				else { // clean up after device switched
					for (String recordKey : displays.keySet().toArray(new String[0])) {
						log.fine("clean child " + recordKey);
						displays.get(recordKey).dispose();
						displays.remove(recordKey);
					}
					digitalMainComposite.layout();
				}
			}
		});
	}
}
