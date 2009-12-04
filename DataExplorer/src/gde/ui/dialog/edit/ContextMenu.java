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
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Decorations;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import osde.messages.MessageIds;
import osde.messages.Messages;

/**
 * Class to represent the context menu to enable adding different tab items
 * @author Winfried Brügmann
 */
public class ContextMenu {
	final static Logger					log												= Logger.getLogger(ContextMenu.class.getName());

	final Menu		 menu;
	final CTabItem parentTabItem;
	
	MenuItem curveSelectionItem, displayGraphicsHeaderItem;

	public ContextMenu(Menu useMenu, CTabItem parent) {
		menu = useMenu;
		parentTabItem = parent;
	}	

	public void create() {
		menu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				//				int tabSelectionIndex = application.getTabSelectionIndex();
				//				if (tabSelectionIndex == 0) {
				//					TabAreaContextMenu.this.curveSelectionItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().curveSelectionMenuItem.getSelection());
				//					TabAreaContextMenu.this.displayGraphicsHeaderItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().graphicsHeaderMenuItem.getSelection());
				//					TabAreaContextMenu.this.displayGraphicsCommentItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().recordCommentMenuItem.getSelection());
				//				}
			}

			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		this.curveSelectionItem = new MenuItem(menu, SWT.CHECK);
		this.curveSelectionItem.setText(Messages.getString(MessageIds.OSDE_MSGT0040));
		this.curveSelectionItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "curveSelectionItem action performed! " + e); //$NON-NLS-1$
				boolean selection = curveSelectionItem.getSelection();
			}
		});
		this.displayGraphicsHeaderItem = new MenuItem(menu, SWT.CHECK);
		this.displayGraphicsHeaderItem.setText(Messages.getString(MessageIds.OSDE_MSGT0041));
		this.displayGraphicsHeaderItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				log.log(Level.FINEST, "toggleViewGraphicsHeaderItem action performed! " + e); //$NON-NLS-1$
				boolean selection = displayGraphicsHeaderItem.getSelection();
			}
		});
	}

}
