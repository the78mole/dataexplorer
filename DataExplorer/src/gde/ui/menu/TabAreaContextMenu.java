/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.ui.menu;

import osde.log.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.DataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.tab.GraphicsWindow;

/**
 * @author Winfried Brügmann
 * This class provides a context menu to tabulator area, curve graphics, compare window, etc. and enable selection of background color, ...
 */
public class TabAreaContextMenu {
	final static Logger						log	= Logger.getLogger(TabAreaContextMenu.class.getName());
	
	public final static int				TYPE_GRAPHICS = GraphicsWindow.TYPE_NORMAL;
	public final static int				TYPE_COMPARE = GraphicsWindow.TYPE_COMPARE;
	public final static int				TYPE_SIMPLE = 2;
	public final static int				TYPE_TABLE = 3;
	
	final DataExplorer	application;

	MenuItem											curveSelectionItem;
	MenuItem											displayGraphicsHeaderItem;
	MenuItem											displayGraphicsCommentItem;
	MenuItem											separatorView;
	MenuItem											copyTabItem;
	MenuItem											copyPrintImageItem;
	MenuItem											separatorCopy;
	MenuItem											outherAreaColorItem;
	MenuItem											innerAreaColorItem;
	MenuItem											borderColorItem;
	boolean												isCreated = false;

	public TabAreaContextMenu() {
		this.application = DataExplorer.getInstance();
	}

	public void createMenu(Menu popupMenu, int type) {
		popupMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				int tabSelectionIndex = TabAreaContextMenu.this.application.getTabSelectionIndex();
				if (tabSelectionIndex == 0) {
					TabAreaContextMenu.this.curveSelectionItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().curveSelectionMenuItem.getSelection());
					TabAreaContextMenu.this.displayGraphicsHeaderItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().graphicsHeaderMenuItem.getSelection());
					TabAreaContextMenu.this.displayGraphicsCommentItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().recordCommentMenuItem.getSelection());
				}
			}
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		if (!isCreated) {
			if (type == TYPE_GRAPHICS) { // -1 as index mean initialization phase
				this.curveSelectionItem = new MenuItem(popupMenu, SWT.CHECK);
				this.curveSelectionItem.setText(Messages.getString(MessageIds.DE_MSGT0040));
				this.curveSelectionItem.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "curveSelectionItem action performed! " + e); //$NON-NLS-1$
						boolean selection = TabAreaContextMenu.this.curveSelectionItem.getSelection();
						TabAreaContextMenu.this.application.setCurveSelectorEnabled(selection);
						TabAreaContextMenu.this.application.getMenuBar().curveSelectionMenuItem.setSelection(selection);
					}
				});
				this.displayGraphicsHeaderItem = new MenuItem(popupMenu, SWT.CHECK);
				this.displayGraphicsHeaderItem.setText(Messages.getString(MessageIds.DE_MSGT0041));
				this.displayGraphicsHeaderItem.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "toggleViewGraphicsHeaderItem action performed! " + e); //$NON-NLS-1$
						boolean selection = TabAreaContextMenu.this.displayGraphicsHeaderItem.getSelection();
						TabAreaContextMenu.this.application.getMenuBar().graphicsHeaderMenuItem.setSelection(selection);
						TabAreaContextMenu.this.application.enableGraphicsHeader(selection);
						TabAreaContextMenu.this.application.updateDisplayTab();
					}
				});
				this.displayGraphicsCommentItem = new MenuItem(popupMenu, SWT.CHECK);
				this.displayGraphicsCommentItem.setText(Messages.getString(MessageIds.DE_MSGT0042));
				this.displayGraphicsCommentItem.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "toggleViewGraphicsCommentItem action performed! " + e); //$NON-NLS-1$
						boolean selection = TabAreaContextMenu.this.displayGraphicsCommentItem.getSelection();
						TabAreaContextMenu.this.application.getMenuBar().recordCommentMenuItem.setSelection(selection);
						TabAreaContextMenu.this.application.enableRecordSetComment(selection);
						TabAreaContextMenu.this.application.updateDisplayTab();
					}
				});
				this.separatorView = new MenuItem(popupMenu, SWT.SEPARATOR);
			}

			this.copyTabItem = new MenuItem(popupMenu, SWT.PUSH);
			this.copyTabItem.setText(Messages.getString(MessageIds.DE_MSGT0026));
			this.copyTabItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					TabAreaContextMenu.log.log(Level.FINEST, "copyTabItem action performed! " + e); //$NON-NLS-1$
					TabAreaContextMenu.this.application.copyTabContentAsImage();
				}
			});
			if (type == TYPE_GRAPHICS || type == TYPE_COMPARE) {
				this.copyPrintImageItem = new MenuItem(popupMenu, SWT.PUSH);
				this.copyPrintImageItem.setText(Messages.getString(MessageIds.DE_MSGT0027));
				this.copyPrintImageItem.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "copyPrintImageItem action performed! " + e); //$NON-NLS-1$
						TabAreaContextMenu.this.application.copyTabContentAsImage();
					}
				});
			}
			this.separatorCopy = new MenuItem(popupMenu, SWT.SEPARATOR);

			this.outherAreaColorItem = new MenuItem(popupMenu, SWT.PUSH);
			this.outherAreaColorItem.setText(Messages.getString(MessageIds.DE_MSGT0462));
			this.outherAreaColorItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					TabAreaContextMenu.log.log(Level.FINEST, "outherAreaColorItem action performed! " + e); //$NON-NLS-1$
					RGB rgb = TabAreaContextMenu.this.application.openColorDialog();
					if (rgb != null) {
						TabAreaContextMenu.this.application.setSurroundingBackground(TabAreaContextMenu.this.application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
					}
				}
			});
			this.innerAreaColorItem = new MenuItem(popupMenu, SWT.PUSH);
			this.innerAreaColorItem.setText(Messages.getString(MessageIds.DE_MSGT0463));
			this.innerAreaColorItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					TabAreaContextMenu.log.log(Level.FINEST, "innerAreaColorItem action performed! " + e); //$NON-NLS-1$
					RGB rgb = TabAreaContextMenu.this.application.openColorDialog();
					if (rgb != null) {
						TabAreaContextMenu.this.application.setInnerAreaBackground(TabAreaContextMenu.this.application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
					}
				}
			});
			if (type == TYPE_GRAPHICS || type == TYPE_COMPARE) {
				this.borderColorItem = new MenuItem(popupMenu, SWT.PUSH);
				this.borderColorItem.setText(Messages.getString(MessageIds.DE_MSGT0464));
				this.borderColorItem.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "borderColorItem action performed! " + e); //$NON-NLS-1$
						RGB rgb = TabAreaContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							TabAreaContextMenu.this.application.setBorderColor(TabAreaContextMenu.this.application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
						}
					}
				});
			}
			isCreated = true;
		}
	}
}
