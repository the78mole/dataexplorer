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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.ui.OpenSerialDataExplorer;

/**
 * Display window parent of analog displays
 * @author Winfried Brügmann
 */
public class AnalogWindow {
	private Logger													log	= Logger.getLogger(this.getClass().getName());

	private TabItem													analogTab;
	private Composite												analogMainComposite;
	private HashMap<String, AnalogDisplay>	displays;
	private CLabel													infoText;

	private final Channels									channels;
	private final TabFolder									displayTab;
	private RecordSet												oldRecordSet;
	private Channel													oldChannel;
	private String[] 												oldRecordsToDisplay;

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
			analogWindowLayout.makeColumnsEqualWidth = true;
			analogWindowLayout.numColumns = 2;
			analogTab.setControl(analogMainComposite);
			analogMainComposite.setLayout(analogWindowLayout);
			log.fine("digitalMainComposite " + analogMainComposite.getBounds().toString());
			analogMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.fine("analogMainComposite.paintControl, event=" + evt);
					update();
				}
			});
			setActiveInfoText("");
			
			analogMainComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			analogMainComposite.layout();
		}
	}

	/**
	 * create new info text
	 */
	private void setActiveInfoText(String info) {
		if (infoText == null || infoText.isDisposed()) {
			infoText = new CLabel(analogMainComposite, SWT.LEFT);
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
				AnalogDisplay display = displays.get(recordKey);
				if (display != null) display.redraw();
			}
		}
	}

	/**
	 * method to update digital window
	 */
	public synchronized void update() {
		Channel activeChannel = channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			// check if just created  or device switched or disabled
			if (recordSet != null && recordSet.get(recordSet.getRecordNames()[0]).getDevice().isAnalogTabRequested()) {
				String[] recordsToDisplay = recordSet.getActiveAndVisibleRecordNames();
		
				// if recordSet name signature changed new displays need to be created
				boolean isUpdateRequired = oldRecordSet == null || !recordSet.getName().equals(oldRecordSet.getName())
				|| oldChannel == null  || !oldChannel.getName().equals(activeChannel.getName())
						|| (recordsToDisplay.length != oldRecordsToDisplay.length);
				log.fine("isUpdateRequired = " + isUpdateRequired);
				if (isUpdateRequired) {
					if (!infoText.isDisposed()) infoText.dispose();
					// clean
					for (String recordKey : displays.keySet().toArray(new String[0])) {
						AnalogDisplay display = displays.get(recordKey);
						if (display != null) {
							display.dispose();
							displays.remove(recordKey);
						}
					}
					// add new displays
					for (String recordKey : recordSet.getActiveAndVisibleRecordNames()) {
						AnalogDisplay display = new AnalogDisplay(analogMainComposite, recordKey, OpenSerialDataExplorer.getInstance().getActiveDevice());
						display.create();
						log.fine("created analog display for " + recordKey);
						displays.put(recordKey, display);
					}
					oldRecordSet = recordSet;
					oldChannel = activeChannel;
					oldRecordsToDisplay = recordsToDisplay;
				}
			}
			else { // clean up after device switched
				for (String recordKey : displays.keySet().toArray(new String[0])) {
					AnalogDisplay display = displays.get(recordKey);
					if (display != null) {
						log.fine("clean child " + recordKey);
						display.dispose();
						displays.remove(recordKey);
					}
				}
				if (recordSet != null && !recordSet.get(recordSet.getRecordNames()[0]).getDevice().isAnalogTabRequested())
					setActiveInfoText("Die Anzeige ist ausgeschaltet!");
			}
			analogMainComposite.layout();
		}
	}
}
