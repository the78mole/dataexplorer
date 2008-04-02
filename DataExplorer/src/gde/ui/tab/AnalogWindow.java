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
import osde.ui.SWTResourceManager;

/**
 * Display window parent of analog displays
 * @author Winfried Brügmann
 */
public class AnalogWindow {
	final static Logger											log	= Logger.getLogger(AnalogWindow.class.getName());

	private TabItem													analogTab;
	private Composite												analogMainComposite;
	private HashMap<String, AnalogDisplay>	displays;
	private CLabel													infoText;
	private GridLayout 											analogMainCompositeLayout;
	private String 													info = "Die Anzeige ist ausgeschaltet!";

	private final Channels									channels;
	private final TabFolder									displayTab;
	private RecordSet												oldRecordSet;
	private Channel													oldChannel;
	private String[] 												oldRecordsToDisplay;

	public AnalogWindow(TabFolder currentDisplayTab) {
		this.displayTab = currentDisplayTab;
		this.channels = Channels.getInstance();
	}

	public void create() {
		this.analogTab = new TabItem(this.displayTab, SWT.NONE);
		this.analogTab.setText("Analog");
		SWTResourceManager.registerResourceUser(this.analogTab);
		
		this.displays = new HashMap<String, AnalogDisplay>(3);
		{
			this.analogMainComposite = new Composite(this.displayTab, SWT.NONE);
			this.analogMainCompositeLayout = new GridLayout();
			this.analogMainCompositeLayout.makeColumnsEqualWidth = true;
			this.analogMainCompositeLayout.numColumns = 2;
			this.analogTab.setControl(this.analogMainComposite);
			this.analogMainComposite.setLayout(null);
			log.fine("digitalMainComposite " + this.analogMainComposite.getBounds().toString());
			this.analogMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.fine("analogMainComposite.paintControl, event=" + evt);
					update();
				}
			});
			setActiveInfoText(this.info);
			this.infoText.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.fine("infoText.paintControl, event=" + evt);
					update();
				}
			});
			
			this.analogMainComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.analogMainComposite.layout();
		}
	}

	/**
	 * create new info text
	 */
	private void setActiveInfoText(String updatedInfo) {
		if (this.infoText == null || this.infoText.isDisposed()) {
			this.analogMainComposite.setLayout(null);
			this.infoText = new CLabel(this.analogMainComposite, SWT.LEFT);
			this.infoText.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.infoText.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
			this.infoText.setBounds(10, 10, 200, 30);
			this.infoText.setText(updatedInfo);
		}
	}

	/**
	 * method to update the window with its children
	 */
	public void updateChilds() {
		RecordSet recordSet = this.channels.getActiveChannel().getActiveRecordSet();
		if (recordSet != null) { // channel does not have a record set yet
			String[] activeRecordKeys = recordSet.getActiveAndVisibleRecordNames();
			for (String recordKey : activeRecordKeys) {
				AnalogDisplay display = this.displays.get(recordKey);
				if (display != null) display.getTacho().redraw();
			}
		}
	}

	/**
	 * method to update digital window
	 */
	public synchronized void update() {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			// check if just created  or device switched or disabled
			if (recordSet != null && recordSet.get(recordSet.getFirstRecordName()).getDevice().isAnalogTabRequested()) {
				String[] recordsToDisplay = recordSet.getActiveAndVisibleRecordNames();
				log.fine(activeChannel.getName());
				// if recordSet name signature changed new displays need to be created
				boolean isUpdateRequired = this.oldRecordSet == null || !recordSet.getName().equals(this.oldRecordSet.getName())
				|| this.oldChannel == null  || !this.oldChannel.getName().equals(activeChannel.getName())
						|| (recordsToDisplay.length != this.oldRecordsToDisplay.length);
				log.fine("isUpdateRequired = " + isUpdateRequired);
				if (isUpdateRequired) {
					// remove the info text
					if (!this.infoText.isDisposed()) this.infoText.dispose();
					// set the grid layout
					this.analogMainComposite.setLayout(this.analogMainCompositeLayout);
					// clean possible outdated displays
					for (String recordKey : this.displays.keySet().toArray(new String[0])) {
						AnalogDisplay display = this.displays.get(recordKey);
						if (display != null) {
							if (!display.isDisposed()) display.dispose();
							this.displays.remove(recordKey);
						}
					}
					// add new displays
					for (String recordKey : recordSet.getActiveAndVisibleRecordNames()) {
						AnalogDisplay display = new AnalogDisplay(this.analogMainComposite, recordKey, OpenSerialDataExplorer.getInstance().getActiveDevice());
						display.create();
						log.fine("created analog display for " + recordKey);
						this.displays.put(recordKey, display);
					}
					this.oldRecordSet = recordSet;
					this.oldRecordsToDisplay = recordsToDisplay;
				}
			}
			else { // clean up after device switched
				for (String recordKey : this.displays.keySet().toArray(new String[0])) {
					AnalogDisplay display = this.displays.get(recordKey);
					if (display != null) {
						log.fine("clean child " + recordKey);
						if (!display.isDisposed()) display.dispose();
						this.displays.remove(recordKey);
					}
				}
				if (recordSet != null && !recordSet.get(recordSet.getFirstRecordName()).getDevice().isAnalogTabRequested())
					if (this.infoText.isDisposed()) setActiveInfoText(this.info);
					else this.infoText.setText(this.info);
			}
			this.oldChannel = activeChannel;
			this.analogMainComposite.layout();
		}
	}
}
