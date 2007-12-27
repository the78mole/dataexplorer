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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import osde.data.Channels;
import osde.data.RecordSet;
import osde.ui.OpenSerialDataExplorer;

/**
 * @author Winfried Brügmann
 * display window parent of analog displays
 */
public class AnalogWindow {
	private Logger													log	= Logger.getLogger(this.getClass().getName());

	private TabItem													analogTab;
	private Composite												analogMainComposite;
	private HashMap<String, AnalogDisplay>	displays;

	private final Channels									channels;
	private final TabFolder									displayTab;
	private RecordSet												oldRecordSet;

	public AnalogWindow(TabFolder displayTab) {
		this.displayTab = displayTab;
		this.channels = Channels.getInstance();
	}

	public void create() {
		analogTab = new TabItem(displayTab, SWT.NONE);
		analogTab.setText("Analog");
		displays = new HashMap<String, AnalogDisplay>(3);
		{
			analogMainComposite = new Composite(displayTab, SWT.NONE);
			GridLayout analogWindowLayout = new GridLayout();
			analogTab.setControl(analogMainComposite);
			analogWindowLayout.numColumns = 2;
			analogWindowLayout.horizontalSpacing = 2;
			analogWindowLayout.verticalSpacing = 0;
			analogMainComposite.setLayout(analogWindowLayout);
			log.fine("digitalMainComposite " + analogMainComposite.getBounds().toString());
			analogMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.finest("analogMainComposite.paintControl, event=" + evt);
					log.fine("digitalMainComposite " + analogMainComposite.getBounds().toString());
					update();
				}
			});
			analogMainComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			//analogMainComposite.pack();
			analogMainComposite.layout();
		}
	}

	/**
	 * method to update digital window
	 */
	public synchronized void update() {
		RecordSet recordSet = channels.getActiveChannel().getActiveRecordSet();
		if (recordSet != null) { // just created  or device switched
			// if recordSet name signature changed new displays need to be created
			if (oldRecordSet == null || !recordSet.keySet().toString().equals(oldRecordSet.keySet().toString())) {
				oldRecordSet = recordSet;
				String[] activeRecordKeys = recordSet.getActiveRecordNames();
				for (String recordKey : activeRecordKeys) {
					AnalogDisplay display = new AnalogDisplay(analogMainComposite, recordKey, OpenSerialDataExplorer.getInstance().getActiveDevice());
					display.create();
					log.fine("created analog display for " + recordKey);
					displays.put(recordKey, display);
				}
				analogMainComposite.layout();
			}
		}
		else { // clean up after device switched
			for (String recordKey : displays.keySet().toArray(new String[0])) {
				log.fine("clean child " + recordKey);
				displays.get(recordKey).dispose();
				displays.remove(recordKey);
			}
			analogMainComposite.layout();
		}
	}
}
