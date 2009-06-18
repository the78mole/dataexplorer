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
package osde.ui.menu;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import osde.OSDE;
import osde.config.Settings;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;

/**
 * @author Winfried Brügmann
 * This class provides a context menu to selecet an image source and other image relevant functions
 */
public class ObjectImageContextMenu {
	final static Logger						log										= Logger.getLogger(ObjectImageContextMenu.class.getName());

	public static final String		OBJECT_IMAGE_PATH			= "OBJECT_IMAGE_PATH"; //$NON-NLS-1$
	public static final String		OBJECT_IMAGE_CHANGED	= "OBJECT_IMAGE_CHANGED"; //$NON-NLS-1$

	Menu													menu;

	final OpenSerialDataExplorer	application;

	public ObjectImageContextMenu() {
		this.application = OpenSerialDataExplorer.getInstance();
	}

	public void createMenu(Menu popupMenu) {
		this.menu = popupMenu;
		MenuItem newItem = new MenuItem(this.menu, SWT.PUSH);
		newItem.setText(Messages.getString(MessageIds.OSDE_MSGT0430));
		newItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ObjectImageContextMenu.log.log(Level.FINEST, "newItem action performed! " + e); //$NON-NLS-1$
				FileDialog imgFileDialog = ObjectImageContextMenu.this.application.openFileOpenDialog(Messages.getString(MessageIds.OSDE_MSGT0431), new String[] { OSDE.FILE_ENDING_STAR_JPG, OSDE.FILE_ENDING_STAR_PNG,
						OSDE.FILE_ENDING_STAR_GIF }, Settings.getInstance().getDataFilePath());
				String imgFilePath = imgFileDialog.getFilterPath();
				ObjectImageContextMenu.log.log(Level.FINE, "imgFilePath = " + imgFilePath); //$NON-NLS-1$
				if (imgFileDialog.getFileName().length() > 4) {
					ObjectImageContextMenu.this.menu.setData(ObjectImageContextMenu.OBJECT_IMAGE_CHANGED, true);
					ObjectImageContextMenu.this.menu.setData(ObjectImageContextMenu.OBJECT_IMAGE_PATH, imgFilePath + OSDE.FILE_SEPARATOR_UNIX + imgFileDialog.getFileName());
					ObjectImageContextMenu.this.application.getObjectDescriptionWindow().redrawImageCanvas();
				}
			}
		});
		MenuItem deleteItem = new MenuItem(this.menu, SWT.PUSH);
		deleteItem.setText(Messages.getString(MessageIds.OSDE_MSGT0432));
		deleteItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ObjectImageContextMenu.log.log(Level.FINEST, "deleteItem action performed! " + e); //$NON-NLS-1$
				ObjectImageContextMenu.this.menu.setData(ObjectImageContextMenu.OBJECT_IMAGE_CHANGED, true);
				ObjectImageContextMenu.this.menu.setData(ObjectImageContextMenu.OBJECT_IMAGE_PATH, null);
				ObjectImageContextMenu.this.application.getObjectDescriptionWindow().redrawImageCanvas();
			}
		});
	}
}
