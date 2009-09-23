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

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Display window parent of analog displays
 * @author Winfried Brügmann
 */
public class AnalogWindow {
	final static Logger											log	= Logger.getLogger(AnalogWindow.class.getName());

	CTabItem												analogTab;
	Composite												analogMainComposite;
	HashMap<String, AnalogDisplay>	displays;
	CLabel													infoText;
	GridLayout 											analogMainCompositeLayout;
	String 													info = Messages.getString(MessageIds.OSDE_MSGT0230);

	final OpenSerialDataExplorer		application;
	final Channels									channels;
	final CTabFolder								displayTab;
	RecordSet												oldRecordSet;
	Channel													oldChannel;
	String[] 												oldRecordsToDisplay;

	public AnalogWindow(CTabFolder currentDisplayTab) {
		this.displayTab = currentDisplayTab;
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();
	}

	public void create() {
		this.analogTab = new CTabItem(this.displayTab, SWT.NONE);
		SWTResourceManager.registerResourceUser(this.analogTab);
		this.analogTab.setFont(SWTResourceManager.getFont(this.application, 10, SWT.NORMAL));
		this.analogTab.setText(Messages.getString(MessageIds.OSDE_MSGT0231));
		
		this.displays = new HashMap<String, AnalogDisplay>(3);
		{
			this.analogMainComposite = new Composite(this.displayTab, SWT.NONE);
			this.analogMainCompositeLayout = new GridLayout();
			this.analogMainCompositeLayout.makeColumnsEqualWidth = true;
			this.analogMainCompositeLayout.numColumns = 2;
			this.analogTab.setControl(this.analogMainComposite);
			this.analogMainComposite.setLayout(null);
			log.log(Level.FINE, "digitalMainComposite " + this.analogMainComposite.getBounds().toString()); //$NON-NLS-1$
			this.analogMainComposite.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINER, "analogMainComposite.helpRequested " + evt); //$NON-NLS-1$
					OpenSerialDataExplorer.getInstance().openHelpDialog("", "HelpInfo_8.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.analogMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.log(Level.FINE, "analogMainComposite.paintControl, event=" + evt); //$NON-NLS-1$
					update();
				}
			});
			setActiveInfoText(this.info);
			this.infoText.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.log(Level.FINE, "infoText.paintControl, event=" + evt); //$NON-NLS-1$
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
				if (display != null) {
					display.checkTachoNeedlePosition();
				}
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
			if (recordSet != null && recordSet.getDevice().isAnalogTabRequested()) {
				String[] recordsToDisplay = recordSet.getActiveAndVisibleRecordNames();
				log.log(Level.FINE, activeChannel.getName());
				// if recordSet name signature changed new displays need to be created
				boolean isUpdateRequired = this.oldRecordSet == null || !recordSet.getName().equals(this.oldRecordSet.getName())
				|| this.oldChannel == null  || !this.oldChannel.getName().equals(activeChannel.getName())
						|| (recordsToDisplay.length != this.oldRecordsToDisplay.length);
				log.log(Level.FINE, "isUpdateRequired = " + isUpdateRequired); //$NON-NLS-1$
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
						log.log(Level.FINE, "created analog display for " + recordKey); //$NON-NLS-1$
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
						log.log(Level.FINE, "clean child " + recordKey); //$NON-NLS-1$
						if (!display.isDisposed()) display.dispose();
						this.displays.remove(recordKey);
					}
				}
				if (recordSet != null && !recordSet.getDevice().isAnalogTabRequested())
					if (this.infoText.isDisposed()) setActiveInfoText(this.info);
					else this.infoText.setText(this.info);
			}
			this.oldChannel = activeChannel;
			this.analogMainComposite.layout();
		}
	}
	
	/**
	 * create visible tab window content as image
	 * @return image with content
	 */
	public Image getContentAsImage() {
		Rectangle bounds = this.analogMainComposite.getClientArea();
		Image tabContentImage = new Image(OpenSerialDataExplorer.display, bounds.width, bounds.height);
		GC imageGC = new GC(tabContentImage);
		this.analogMainComposite.print(imageGC);
		imageGC.dispose();

		return tabContentImage;
	}
}
