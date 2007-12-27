/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.ui.menu;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import osde.device.DeviceConfiguration;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.dialog.DeviceSelectionDialog;

/**
 * @author Winfried Brügmann
 * graphical menu tool bar class
 * (future items are: scaling icons, ...)
 */
public class MenuToolBar {
	private Logger												log	= Logger.getLogger(this.getClass().getName());

	private final OpenSerialDataExplorer	application;
	private CoolBar												menuCoolBar;
	private CoolItem											menuCoolItem;
	private ToolItem											toolBoxToolItem;
	private ToolItem											nextDeviceToolItem;
	private ToolItem											prevDeviceToolItem;
	private ToolItem											deviceToolItem;
	private ToolItem											separatorToolItem;
	private ToolItem											saveAsToolItem;
	private ToolItem											saveToolItem;
	private ToolItem											openToolItem;
	private ToolItem											newToolItem;
	private ToolBar												menuToolBar;
	private Composite											mainToolComposite;

	public MenuToolBar(OpenSerialDataExplorer parent, CoolBar menuCoolBar) {
		this.application = parent;
		this.menuCoolBar = menuCoolBar;
	}

	public void init() {
		menuCoolBar = new CoolBar(this.application, SWT.NONE);
		menuCoolBar.setSize(530, 191);
		create();
	}

	public void create() {
		{
			menuCoolItem = new CoolItem(menuCoolBar, SWT.NONE);
			{
				mainToolComposite = new Composite(menuCoolBar, SWT.NONE);
				Point connectSize = mainToolComposite.computeSize(SWT.DEFAULT, 34);
				Point connectCoolSize = mainToolComposite.computeSize(connectSize.x, connectSize.y);
				menuCoolItem.setPreferredSize(connectCoolSize);
				menuCoolItem.setMinimumSize(connectCoolSize);
				menuCoolItem.setSize(connectCoolSize);
				menuCoolItem.setControl(mainToolComposite);
				RowLayout composite1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				mainToolComposite.setLayout(composite1Layout);
				{
					menuToolBar = new ToolBar(mainToolComposite, SWT.FLAT);
					{
						newToolItem = new ToolItem(menuToolBar, SWT.PUSH);
						newToolItem.setToolTipText("Löscht die aktuellen Aufzeichnungen und legt einen leeren Datensatz an");
						newToolItem.setImage(SWTResourceManager.getImage("osde/resource/New.gif"));
						newToolItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("newToolItem.widgetSelected, event=" + evt);
								application.getDeviceSelectionDialog().setupDataChannels(application.getActiveDevice());
								application.updateDataTable();
								application.updateDigitalWindow();
							}
						});
					}
					{
						openToolItem = new ToolItem(menuToolBar, SWT.NONE);
						openToolItem.setToolTipText("Verwirft den aktuellen Datensatz und  ersetz ihn durch einen dem Dateiinhalt entsprechenden");
						openToolItem.setImage(SWTResourceManager.getImage("osde/resource/Open.gif"));
						openToolItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("openToolItem.widgetSelected, event=" + evt);
								//TODO add your code for openToolItem.widgetSelected
							}
						});
					}
					{
						saveToolItem = new ToolItem(menuToolBar, SWT.NONE);
						saveToolItem.setToolTipText("Sichert die aktuellen Aufzeichnungen in eine Datei");
						saveToolItem.setImage(SWTResourceManager.getImage("osde/resource/Save.gif"));
						saveToolItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("saveToolItem.widgetSelected, event=" + evt);
								//TODO add your code for saveToolItem.widgetSelected
							}
						});
					}
					{
						saveAsToolItem = new ToolItem(menuToolBar, SWT.NONE);
						saveAsToolItem.setToolTipText("Sichert die aktuellen Aufzeichnungen unter einem anzugebenden Namen");
						saveAsToolItem.setImage(SWTResourceManager.getImage("osde/resource/SaveAs.gif"));
						saveAsToolItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("saveAsToolItem.widgetSelected, event=" + evt);
								//TODO add your code for saveAsToolItem.widgetSelected
							}
						});
					}
					{
						separatorToolItem = new ToolItem(menuToolBar, SWT.SEPARATOR);
						separatorToolItem.setEnabled(false);
						separatorToolItem.setWidth(5);
						separatorToolItem.setImage(SWTResourceManager.getImage("osde/resource/SeparatorHorizontal.gif"));
					}
					{
						deviceToolItem = new ToolItem(menuToolBar, SWT.NONE);
						deviceToolItem.setToolTipText("Geräteauswahl mit Einstellungen");
						deviceToolItem.setImage(SWTResourceManager.getImage("osde/resource/DeviceSelection.gif"));
						deviceToolItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("deviceToolItem.widgetSelected, event=" + evt);
								application.setActiveDevice(application.getDeviceSelectionDialog().open());
							}
						});
					}
					{
						prevDeviceToolItem = new ToolItem(menuToolBar, SWT.NONE);
						prevDeviceToolItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLeft.gif"));
						prevDeviceToolItem.setToolTipText("Schalte zum verhergehenden Gerät");
						prevDeviceToolItem.setWidth(20);
						prevDeviceToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLefHot.gif"));
						prevDeviceToolItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("prevDeviceToolItem.widgetSelected, event=" + evt);
								// allow device switch only if port not connected
								if (application.getActiveDevice() == null || application.getActiveDevice().getSerialPort() != null && !application.getActiveDevice().getSerialPort().isConnected()) {
									DeviceConfiguration deviceConfig;
									DeviceSelectionDialog deviceSelect = application.getDeviceSelectionDialog();
									int selection = deviceSelect.getActiveDevices().indexOf(deviceSelect.getActiveConfig().getName());
									int size = deviceSelect.getActiveDevices().size();
									if (selection > 0 && selection <= size) {
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(selection - 1));
									}
									else
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(size - 1));

									if (application.getDeviceDialog() != null && !application.getDeviceDialog().isDisposed()) {
										application.getDeviceDialog().dispose();
									}
									deviceSelect.setActiveConfig(deviceConfig);
									if (!deviceSelect.checkPortSelection()) application.setActiveDevice(application.getDeviceSelectionDialog().open());
									deviceSelect.setupDevice();
								}
								else {
									application.openMessageDialog("Das Gerät kann nicht gewechselt werden, solange der serielle Port geöffnet ist!");
								}
							}
						});
					}
					{
						nextDeviceToolItem = new ToolItem(menuToolBar, SWT.NONE);
						nextDeviceToolItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRight.gif"));
						nextDeviceToolItem.setToolTipText("Schalte zum nachfolgenden Gerät");
						nextDeviceToolItem.setText("");
						nextDeviceToolItem.setWidth(20);
						nextDeviceToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRightHot.gif"));
						nextDeviceToolItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("nextDeviceToolItem.widgetSelected, event=" + evt);
								// allow device switch only if port not connected
								if (application.getActiveDevice() == null || application.getActiveDevice().getSerialPort() != null && !application.getActiveDevice().getSerialPort().isConnected()) {
									DeviceConfiguration deviceConfig;
									DeviceSelectionDialog deviceSelect = application.getDeviceSelectionDialog();
									int selection = deviceSelect.getActiveDevices().indexOf(deviceSelect.getActiveConfig().getName());
									int size = deviceSelect.getActiveDevices().size() - 1;
									if (selection >= 0 && selection < size)
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(selection + 1));
									else
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(0));

									if (application.getDeviceDialog() != null && !application.getDeviceDialog().isDisposed()) {
										application.getDeviceDialog().dispose();
									}
									deviceSelect.setActiveConfig(deviceConfig);
									if (!deviceSelect.checkPortSelection()) application.setActiveDevice(application.getDeviceSelectionDialog().open());
									deviceSelect.setupDevice();
								}
								else {
									application.openMessageDialog("Das Gerät kann nicht gewechselt werden, solange der serielle Port geöffnet ist!");
								}
							}
						});
					}
					{
						toolBoxToolItem = new ToolItem(menuToolBar, SWT.NONE);
						toolBoxToolItem.setImage(SWTResourceManager.getImage("osde/resource/Tools.gif"));
						toolBoxToolItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("toolBoxToolItem.widgetSelected, event=" + evt);
								if (application.getDeviceDialog() != null) {
									application.getDeviceDialog().open();
								}
								else {
									application.setActiveDevice(application.getDeviceSelectionDialog().open());
								}
							}
						});
					}
					{
						separatorToolItem = new ToolItem(menuToolBar, SWT.SEPARATOR);
						separatorToolItem.setWidth(5);
					}
					{
						new ToolItem(menuToolBar, SWT.NONE);
					}
					menuToolBar.pack();
				}
			}

		}
	}

}
