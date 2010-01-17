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
import osde.log.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import osde.OSDE;
import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.menu.TabAreaContextMenu;

/**
 * Display window parent of digital displays
 * @author Winfried Brügmann
 */
public class DigitalWindow extends CTabItem {
	final static Logger							log	= Logger.getLogger(DigitalWindow.class.getName());

	Composite												digitalMainComposite;
	HashMap<String, DigitalDisplay>	displays;
	FillLayout 											digitalMainCompositeLayout = new FillLayout(SWT.HORIZONTAL);
	String 													info = Messages.getString(MessageIds.OSDE_MSGT0230);

	Color														surroundingBackground;

	final OpenSerialDataExplorer		application;
	final Channels									channels;
	final CTabFolder								displayTab;
	final Menu											popupmenu;
	final TabAreaContextMenu				contextMenu;
	
	RecordSet												oldRecordSet;
	Channel													oldChannel;
	String[] 												oldRecordsToDisplay;

	public DigitalWindow(CTabFolder currentDisplayTab, int style, int position) {
		super(currentDisplayTab, style, position);
		SWTResourceManager.registerResourceUser(this);
		this.displayTab = currentDisplayTab;
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.setFont(SWTResourceManager.getFont(this.application, 10, SWT.NORMAL));
		this.setText(Messages.getString(MessageIds.OSDE_MSGT0238));
			
		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new TabAreaContextMenu();
		this.surroundingBackground = Settings.getInstance().getDigitalSurroundingAreaBackground();
		
		this.displays = new HashMap<String, DigitalDisplay>(3);
	}

	public void create() {
		{
			this.digitalMainComposite = new Composite(this.displayTab, SWT.NONE);
			this.setControl(this.digitalMainComposite);
			this.digitalMainComposite.setBackground(this.surroundingBackground);
			this.digitalMainComposite.setLayout(null);
			this.digitalMainComposite.setMenu(this.popupmenu);
			this.digitalMainComposite.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINER, "digitalMainComposite.helpRequested " + evt); //$NON-NLS-1$
					OpenSerialDataExplorer.getInstance().openHelpDialog("", "HelpInfo_7.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.digitalMainComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.log(Level.FINE, "digitalMainComposite.paintControl, event=" + evt); //$NON-NLS-1$
					DigitalWindow.this.contextMenu.createMenu(DigitalWindow.this.popupmenu, TabAreaContextMenu.TYPE_SIMPLE);
					update(false);
				}
			});
			this.digitalMainComposite.layout();
		}
	}

	/**
	 * method to update the window with its children
	 */
	public void updateChilds() {
		RecordSet recordSet = this.channels.getActiveChannel().getActiveRecordSet();
		if (recordSet != null) { // channel does not have a record set yet
			String[] activeRecordKeys = recordSet.getActiveAndVisibleRecordNames();
			if (activeRecordKeys.length != this.displays.size())
				this.update(false);
			else
				for (String recordKey : activeRecordKeys) {
					DigitalDisplay display = this.displays.get(recordKey);
					if (display != null) display.getDigitalLabel().redraw();
				}
		}
	}

	/**
	 * method to update digital window by adding removing digital displays
	 */
	public void update(boolean forceUpdate) {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null && this.digitalMainComposite.isVisible()) {
			log.log(Level.FINE, OSDE.STRING_BLANK);
			RecordSet recordSet = activeChannel.getActiveRecordSet();
			// check if just created  or device switched or disabled
			if (recordSet != null && recordSet.getDevice().isDigitalTabRequested()) {
				String[] recordsToDisplay = recordSet.getActiveAndVisibleRecordNames();
		
				// if recordSet name signature changed new displays need to be created
				boolean isUpdateRequired = forceUpdate || this.oldRecordSet == null || !recordSet.getName().equals(this.oldRecordSet.getName())
				|| this.oldChannel == null  || !this.oldChannel.getName().equals(activeChannel.getName())
						|| (recordsToDisplay.length != this.oldRecordsToDisplay.length);
				log.log(Level.FINE, "isUpdateRequired = " + isUpdateRequired); //$NON-NLS-1$
				if (isUpdateRequired) {
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

	/**
	 * @param newInnerAreaBackground the innerAreaBackground to set
	 */
	public void setInnerAreaBackground(Color newInnerAreaBackground) {
		this.update(true);
	}

	/**
	 * @param newSurroundingBackground the surroundingAreaBackground to set
	 */
	public void setSurroundingAreaBackground(Color newSurroundingBackground) {
		this.digitalMainComposite.setBackground(newSurroundingBackground);
		this.surroundingBackground = newSurroundingBackground;
		this.digitalMainComposite.redraw();
	}
}
