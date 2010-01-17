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
package osde.ui.dialog.edit;

import osde.log.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.SWTResourceManager;

/**
 * Class to represent the context menu to enable adding different tab items
 * @author Winfried Brügmann
 */
public class ContextMenu {
	final static Logger					log												= Logger.getLogger(ContextMenu.class.getName());

	final Menu		 menu;
	final CTabFolder parentTabFolder;
	
	MenuItem addSerialPortTypeMenuItem, addDataBlockTypeMenuItem, addStateTypeMenuItem;

	public ContextMenu(Menu useMenu, CTabFolder parent) {
		menu = useMenu;
		parentTabFolder = parent;
	}	

	public void create() {
		SWTResourceManager.registerResourceUser(this.menu);
		menu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				log.log(Level.FINEST, "menuShown action performed! " + e); //$NON-NLS-1$
				addSerialPortTypeMenuItem.setEnabled(true);
				addDataBlockTypeMenuItem.setEnabled(true);
				addStateTypeMenuItem.setEnabled(true);
				for (CTabItem tabItem : parentTabFolder.getItems()) {
					if (tabItem.getText().equals(Messages.getString(MessageIds.OSDE_MSGT0510))) {
						addSerialPortTypeMenuItem.setEnabled(false);
					}
					else if (tabItem.getText().equals(Messages.getString(MessageIds.OSDE_MSGT0515))) {
						addDataBlockTypeMenuItem.setEnabled(false);
					}
					else if (tabItem.getText().equals(Messages.getString(MessageIds.OSDE_MSGT0470))) {
						addStateTypeMenuItem.setEnabled(false);
					}
				}
			}

			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		this.addSerialPortTypeMenuItem = new MenuItem(menu, SWT.PUSH);
		this.addSerialPortTypeMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0512));
		this.addSerialPortTypeMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "addSerialPortTypeMenuItem action performed! " + e); //$NON-NLS-1$
				DevicePropertiesEditor.getInstance().createSerialPortTabItem();
			}
		});
		new MenuItem(menu, SWT.SEPARATOR);
		this.addDataBlockTypeMenuItem = new MenuItem(menu, SWT.PUSH);
		this.addDataBlockTypeMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0513));
		this.addDataBlockTypeMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "addSerialPortTypeMenuItem action performed! " + e); //$NON-NLS-1$
				DevicePropertiesEditor.getInstance().createDataBlockType();
			}
		});
		new MenuItem(menu, SWT.SEPARATOR);
		this.addStateTypeMenuItem = new MenuItem(menu, SWT.PUSH);
		this.addStateTypeMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0514));
		this.addStateTypeMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "addStateTypeMenuItem action performed! " + e); //$NON-NLS-1$
				DevicePropertiesEditor.getInstance().createStateTabItem();
			}
		});
	}

}
