/**************************************************************************************
  	This file is part of DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DataExplorer.  If not, see <http://www.gnu.org/licenses/>.

    Copyright (c) 2010 Winfried Bruegmann
****************************************************************************************/
package gde.device.wstech;

import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.tab.GraphicsWindow;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * @author Winfried Brügmann
 * This class provides a context menu to tabulator area, curve graphics, compare window, etc. and enable selection of background color, ...
 */
public class ContextMenu {
	final static Logger						log						= Logger.getLogger(ContextMenu.class.getName());

	public final static int				TYPE_GRAPHICS	= GraphicsWindow.TYPE_NORMAL;
	public final static int				TYPE_COMPARE	= GraphicsWindow.TYPE_COMPARE;
	public final static int				TYPE_SIMPLE		= 2;
	public final static int				TYPE_TABLE		= 3;

	final DataExplorer	application;
	final VarioToolTabItem varioToolTabItem;

	MenuItem											headerItem;
	MenuItem											setupLoadItem;
	MenuItem											setupSaveItem;
	MenuItem											separatorView;
	MenuItem											convertKLM3DGPSItem;
	MenuItem											separatorCopy;
	MenuItem											convertKLM3DBaroItem;
	MenuItem											convert2GPXItem;
	MenuItem											convert2SkyplotterItem;

	public ContextMenu(VarioToolTabItem useVarioToolTabItem) {
		this.application = DataExplorer.getInstance();
		this.varioToolTabItem = useVarioToolTabItem;
	}

	public void createMenu(Menu popupMenu) {
//		popupMenu.addMenuListener(new MenuListener() {
//			@Override
//			public void menuShown(MenuEvent e) {
//				int tabSelectionIndex = ContextMenu.this.application.getTabSelectionIndex();
//				if (tabSelectionIndex == 0) {
//					//					TabAreaContextMenu.this.curveSelectionItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().curveSelectionMenuItem.getSelection());
//					//					TabAreaContextMenu.this.displayGraphicsHeaderItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().graphicsHeaderMenuItem.getSelection());
//					//					TabAreaContextMenu.this.displayGraphicsCommentItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().recordCommentMenuItem.getSelection());
//				}
//			}
//
//			@Override
//			public void menuHidden(MenuEvent e) {
//			}
//		});
		this.headerItem = new MenuItem(popupMenu, SWT.NONE);
		this.headerItem.setText(Messages.getString(MessageIds.GDE_MSGT1894) + varioToolTabItem.getFirmwareVersion());

		this.separatorView = new MenuItem(popupMenu, SWT.SEPARATOR);

		this.setupLoadItem = new MenuItem(popupMenu, SWT.PUSH);
		this.setupLoadItem.setText(Messages.getString(MessageIds.GDE_MSGT1892));
		this.setupLoadItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ContextMenu.log.log(Level.FINEST, "curveSelectionItem action performed! " + e); //$NON-NLS-1$
				varioToolTabItem.loadSetup();
			}
		});
		this.setupSaveItem = new MenuItem(popupMenu, SWT.PUSH);
		this.setupSaveItem.setText(Messages.getString(MessageIds.GDE_MSGT1893));
		this.setupSaveItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ContextMenu.log.log(Level.FINEST, "toggleViewGraphicsHeaderItem action performed! " + e); //$NON-NLS-1$
				varioToolTabItem.saveSetup();
			}
		});

		this.separatorView = new MenuItem(popupMenu, SWT.SEPARATOR);

		this.convertKLM3DGPSItem = new MenuItem(popupMenu, SWT.PUSH);
		this.convertKLM3DGPSItem.setText(Messages.getString(MessageIds.GDE_MSGT1895));
		this.convertKLM3DGPSItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ContextMenu.log.log(Level.FINEST, "copyPrintImageItem action performed! " + e); //$NON-NLS-1$
				varioToolTabItem.convert2KML3D(DataVario.GPS_HEIGHT);
			}
		});
		this.convertKLM3DBaroItem = new MenuItem(popupMenu, SWT.PUSH);
		this.convertKLM3DBaroItem.setText(Messages.getString(MessageIds.GDE_MSGT1896));
		this.convertKLM3DBaroItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ContextMenu.log.log(Level.FINEST, "outherAreaColorItem action performed! " + e); //$NON-NLS-1$
				varioToolTabItem.convert2KML3D(DataVario.BARO_HEIGHT);
			}
		});
		
		this.separatorCopy = new MenuItem(popupMenu, SWT.SEPARATOR);

		this.convert2GPXItem = new MenuItem(popupMenu, SWT.PUSH);
		this.convert2GPXItem.setText(Messages.getString(MessageIds.GDE_MSGT1897));
		this.convert2GPXItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ContextMenu.log.log(Level.FINEST, "innerAreaColorItem action performed! " + e); //$NON-NLS-1$
				varioToolTabItem.convert2GPX();
			}
		});
		this.convert2SkyplotterItem = new MenuItem(popupMenu, SWT.PUSH);
		this.convert2SkyplotterItem.setText(Messages.getString(MessageIds.GDE_MSGT1898));
		this.convert2SkyplotterItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ContextMenu.log.log(Level.FINEST, "borderColorItem action performed! " + e); //$NON-NLS-1$
				varioToolTabItem.convert2Skylotter();
			}
		});
	}
}
