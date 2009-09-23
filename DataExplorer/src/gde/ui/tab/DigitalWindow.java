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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Display window parent of digital displays
 * @author Winfried Brügmann
 */
public class DigitalWindow {
	final static Logger							log	= Logger.getLogger(DigitalWindow.class.getName());

	Composite												digitalMainComposite;
	CTabItem												digitalTab;
	HashMap<String, DigitalDisplay>	displays;
	CLabel													infoText;
	FillLayout 											digitalMainCompositeLayout;
	String 													info = Messages.getString(MessageIds.OSDE_MSGT0230);

	final OpenSerialDataExplorer		application;
	final Channels									channels;
	final CTabFolder								displayTab;
	RecordSet												oldRecordSet;
	Channel													oldChannel;
	String[] 												oldRecordsToDisplay;

	public DigitalWindow(CTabFolder currentDisplayTab) {
		this.displayTab = currentDisplayTab;
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();
	}

	public void create() {
		this.digitalTab = new CTabItem(this.displayTab, SWT.NONE);
		SWTResourceManager.registerResourceUser(this.digitalTab);
		this.digitalTab.setFont(SWTResourceManager.getFont(this.application, 10, SWT.NORMAL));
		this.digitalTab.setText(Messages.getString(MessageIds.OSDE_MSGT0238));
		
		this.displays = new HashMap<String, DigitalDisplay>(3);
		{
			this.digitalMainComposite = new Composite(this.displayTab, SWT.NONE);
			this.digitalTab.setControl(this.digitalMainComposite);
			this.digitalMainCompositeLayout = new FillLayout(SWT.HORIZONTAL);
			this.digitalMainComposite.setLayout(null);
			this.digitalMainComposite.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINER, "digitalMainComposite.helpRequested " + evt); //$NON-NLS-1$
					OpenSerialDataExplorer.getInstance().openHelpDialog("", "HelpInfo_7.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.digitalMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.log(Level.FINE, "digitalMainComposite.paintControl, event=" + evt); //$NON-NLS-1$
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
			this.infoText.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINER, "infoText.helpRequested " + evt); //$NON-NLS-1$
					OpenSerialDataExplorer.getInstance().openHelpDialog("", "HelpInfo_7.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			
			this.digitalMainComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.digitalMainComposite.layout();
		}
	}

	/**
	 * create new info text
	 */
	private void setActiveInfoText(String udateInfo) {
		if (this.infoText == null || this.infoText.isDisposed()) {
			this.digitalMainComposite.setLayout(null);
			this.infoText = new CLabel(this.digitalMainComposite, SWT.LEFT);
			this.infoText.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.infoText.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
			this.infoText.setBounds(10, 10, 200, 30);
			this.infoText.setText(udateInfo);
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
				DigitalDisplay display = this.displays.get(recordKey);
				if (display != null) display.getDigitalLabel().redraw();
			}
		}
	}

	/**
	 * method to update digital window by adding removing digital displays
	 */
	public void update() {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			// check if just created  or device switched or disabled
			if (recordSet != null && recordSet.getDevice().isDigitalTabRequested()) {
				String[] recordsToDisplay = recordSet.getActiveAndVisibleRecordNames();
		
				// if recordSet name signature changed new displays need to be created
				boolean isUpdateRequired = this.oldRecordSet == null || !recordSet.getName().equals(this.oldRecordSet.getName())
				|| this.oldChannel == null  || !this.oldChannel.getName().equals(activeChannel.getName())
						|| (recordsToDisplay.length != this.oldRecordsToDisplay.length);
				log.log(Level.FINE, "isUpdateRequired = " + isUpdateRequired); //$NON-NLS-1$
				if (isUpdateRequired) {
					// remove the info text 
					if (!this.infoText.isDisposed()) this.infoText.dispose();
					// set layout 
					this.digitalMainComposite.setLayout(this.digitalMainCompositeLayout);
					// cleanup
					for (String recordKey : this.displays.keySet().toArray(new String[0])) {
						DigitalDisplay display = this.displays.get(recordKey);
						if (display != null) {
							if (!display.isDisposed()) display.dispose();
							this.displays.remove(recordKey);
						}
					}
					// add new
					for (String recordKey : recordSet.getActiveAndVisibleRecordNames()) {
						DigitalDisplay display = new DigitalDisplay(this.application, this.digitalMainComposite, recordKey, OpenSerialDataExplorer.getInstance().getActiveDevice());
						display.create();
						log.log(Level.FINE, "created digital display for " + recordKey); //$NON-NLS-1$
						this.displays.put(recordKey, display);
					}
					this.oldRecordSet = recordSet;
					this.oldRecordsToDisplay = recordsToDisplay;
				}
			}
			else { // clean up after device switched
				for (String recordKey : this.displays.keySet().toArray(new String[0])) {
					DigitalDisplay display = this.displays.get(recordKey);
					if (display != null) {
						log.log(Level.FINE, "clean child " + recordKey); //$NON-NLS-1$
						if (!display.isDisposed()) display.dispose();
						this.displays.remove(recordKey);
					}
				}
				if (recordSet != null && !recordSet.getDevice().isDigitalTabRequested())
					if (this.infoText.isDisposed()) setActiveInfoText(this.info);
					else this.infoText.setText(this.info);
			}
			this.oldChannel = activeChannel;
			this.digitalMainComposite.layout();
		}
	}
	
	/**
	 * create visible tab window content as image
	 * @return image with content
	 */
	public Image getContentAsImage() {
		Rectangle bounds = this.digitalMainComposite.getClientArea();
		Image tabContentImage = new Image(OpenSerialDataExplorer.display, bounds.width, bounds.height);
		GC imageGC = new GC(tabContentImage);
		this.digitalMainComposite.print(imageGC);
		imageGC.dispose();

		return tabContentImage;
	}
}
