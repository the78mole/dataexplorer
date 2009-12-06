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

import java.util.logging.Level;
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
		menu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				log.log(Level.FINEST, "menuShown action performed! " + e); //$NON-NLS-1$
				for (CTabItem tabItem : parentTabFolder.getItems()) {
//					if (tabItem instanceof SeriaPortTypeTabItem) {
//						addSerialPortTypeMenuItem.setEnabled(false);
//					}
				}
			}

			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		this.addSerialPortTypeMenuItem = new MenuItem(menu, SWT.CHECK);
		this.addSerialPortTypeMenuItem.setText("add SerialPortType");
		this.addSerialPortTypeMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "addSerialPortTypeMenuItem action performed! " + e); //$NON-NLS-1$
				boolean selection = addSerialPortTypeMenuItem.getSelection();
			}
		});
		this.addDataBlockTypeMenuItem = new MenuItem(menu, SWT.CHECK);
		this.addDataBlockTypeMenuItem.setText("add DataBlockType");
		this.addDataBlockTypeMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "addSerialPortTypeMenuItem action performed! " + e); //$NON-NLS-1$
				boolean selection = addDataBlockTypeMenuItem.getSelection();
			}
		});
		this.addStateTypeMenuItem = new MenuItem(menu, SWT.CHECK);
		this.addStateTypeMenuItem.setText("add StateType");
		this.addStateTypeMenuItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "addStateTypeMenuItem action performed! " + e); //$NON-NLS-1$
				boolean selection = addStateTypeMenuItem.getSelection();
			}
		});
		new MenuItem(menu, SWT.SEPARATOR);
	}

}
