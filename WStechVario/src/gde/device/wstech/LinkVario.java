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

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.Messages;
import gde.serial.DeviceSerialPort;
import gde.ui.DataExplorer;

/**
 * Class to implement WSTech DataVario device properties extending the CSV2SerialAdapter class
 * @author Winfried Brügmann
 */
public class LinkVario extends DataVario {
	
	final DataExplorer		application;
	final VarioDialog		dialog;

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public LinkVario(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.wstech.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.dialog = new VarioDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_IMPORT_CLOSE);
			updateFileMenu(this.application.getMenuBar().getExportMenu());
		}
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public LinkVario(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.wstech.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.application = DataExplorer.getInstance();
		this.dialog = new VarioDialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceSerialPort.ICON_SET_IMPORT_CLOSE);
			updateFileMenu(this.application.getMenuBar().getExportMenu());
		}
	}
	
	/**
	 * This function allows to register a custom CTabItem to the main application tab folder to display device 
	 * specific curve calculated from point combinations or other specific dialog
	 * As default the function should return null which stands for no device custom tab item.  
	 */
	public CTabItem getCustomTabItem() {
		return new VarioToolTabItem(this.application.getTabFolder(), SWT.NONE, this.application.getTabFolder().getItemCount(), this, false);
	}
	
	
	/**
	 * update the file menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	public void updateFileMenu(Menu exportMenue) {
		MenuItem											convertKLM3DRelativeItem;
		MenuItem											convertKLM3DAbsoluteItem;
		MenuItem											convert2GPXRelativeItem;
		MenuItem											convert2GPXAbsoluteItem;
		
		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertKLM3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKLM3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT1895));
			convertKLM3DRelativeItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ContextMenu.log.log(Level.FINEST, "convertKLM3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KML3D(DataVario.HEIGHT_RELATIVE);
				}
			});

			convertKLM3DAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKLM3DAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT1896));
			convertKLM3DAbsoluteItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ContextMenu.log.log(Level.FINEST, "convertKLM3DAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KML3D(DataVario.HEIGHT_ABSOLUTE);
				}
			});

			convert2GPXRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convert2GPXRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT1897));
			convert2GPXRelativeItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ContextMenu.log.log(Level.FINEST, "convert2GPXRelativeItem action performed! " + e); //$NON-NLS-1$
					export2GPX(DataVario.HEIGHT_RELATIVE);
				}
			});

			convert2GPXAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convert2GPXAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT1898));
			convert2GPXAbsoluteItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ContextMenu.log.log(Level.FINEST, "convert2GPXAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2GPX(DataVario.HEIGHT_ABSOLUTE);
				}
			});
		}
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DataVario.HEIGHT_RELATIVE | DataVario.HEIGHT_ABSOLUTE
	 */
	public void export2KML3D(int type) {
		//ordinalLongitude, ordinalLatitude, ordinalGPSHeight, inRelative
		new FileHandler().exportFileKML("export KML file with GPS height", 7, 8, 9, type == DataVario.HEIGHT_RELATIVE);
	}
	
	/**
	 * exports the actual displayed data set to GPX file format
	 * @param type DataVario.HEIGHT_RELATIVE | DataVario.HEIGHT_ABSOLUTE
	 */
	public void export2GPX(int type) {
		//ordinalLongitude, ordinalLatitude, ordinalGPSHeight, ordinalVelocity, ordinalHeight, inRelative
		new FileHandler().exportFileGPX("export GPX file", 7, 8, 9, 10, 1, type == DataVario.HEIGHT_RELATIVE);
	}
}
