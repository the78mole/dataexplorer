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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import osde.OSDE;
import osde.config.GraphicsTemplate;
import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.ChannelTypes;
import osde.device.DeviceConfiguration;
import osde.device.DeviceDialog;
import osde.device.IDevice;
import osde.exception.DeclinedException;
import osde.exception.NotSupportedFileFormatException;
import osde.io.CSVReaderWriter;
import osde.io.LogViewReader;
import osde.io.OsdReaderWriter;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.dialog.DeviceSelectionDialog;
import osde.ui.tab.GraphicsWindow;
import osde.utils.FileUtils;
import osde.utils.StringHelper;

/**
 * menu bar implementation class for the OpenSerialDataExplorer
 * @author Winfried Brügmann
 */
public class MenuBar {	
	final static Logger						log			= Logger.getLogger(MenuBar.class.getName());

	MenuItem											fileMenuItem;
	Menu													fileMenu;
	MenuItem											openFileMenuItem;
	MenuItem											historyFileMenuItem;
	MenuItem											toolBoxDeviceMenuItem, portMenuItem;
	MenuItem											aboutMenuItem;
	MenuItem											contentsMenuItem, webCheckMenuItem;
	Menu													helpMenu;
	MenuItem											helpMenuItem;
	MenuItem											recordCommentMenuItem, graphicsHeaderMenuItem;
	MenuItem											curveSelectionMenuItem;
	Menu													viewMenu;
	MenuItem											viewMenuItem;
	Menu													graphicsMenu;
	MenuItem											graphicsMenuItem, saveDefaultGraphicsTemplateItem, saveAsGraphicsTemplateItem, restoreGraphicsTemplateItem;
	MenuItem											csvExportMenuItem1, csvExportMenuItem2;
	MenuItem											nextDeviceMenuItem;
	MenuItem											prevDeviceMenuItem;
	MenuItem											selectDeviceMenuItem;
	Menu													deviceMenu;
	MenuItem											deviceMenuItem;
	MenuItem											copyTableMenuItem;
	MenuItem											copyGraphicMenuItem, activateZoomGraphicMenuItem, resetZoomGraphicMenuItem, panGraphicMenuItem;
	Menu													editMenu;
	MenuItem											editMenuItem;
	MenuItem											exitMenuItem;
	MenuItem											preferencesFileMenuItem;
	Menu													exportMenu;
	MenuItem											exportFileMenuItem;
	MenuItem											csvImportMenuItem1, csvImportMenuItem2;
	Menu													importMenu;
	MenuItem											importFileMenuItem;
	Menu													fileHistoryMenu;
	MenuItem											saveAsFileMenuItem;
	MenuItem											saveFileMenuItem;
	MenuItem											newFileMenuItem;
	
	int														iconSet = DeviceSerialPort.ICON_SET_OPEN_CLOSE; 
	
	final Menu										parent;
	final OpenSerialDataExplorer	application;
	final Channels								channels;
	Thread 												readerWriterThread;

	public MenuBar(OpenSerialDataExplorer currentApplication, Menu menuParent) {
		this.application = currentApplication;
		this.parent = menuParent;
		this.channels = Channels.getInstance();
	}

	/**
	 * 
	 */
	public void create() {
		{
			this.fileMenuItem = new MenuItem(this.parent, SWT.CASCADE);
			this.fileMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0001));
			this.fileMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.fine("fileMenuItem.helpRequested, event=" + evt); //$NON-NLS-1$
					MenuBar.this.application.openHelpDialog(OSDE.STRING_EMPTY, "HelpInfo_3.html"); //$NON-NLS-1$
				}
			});
			{
				this.fileMenu = new Menu(this.fileMenuItem);
				this.fileMenu.addMenuListener(new MenuListener() {
					public void menuShown(MenuEvent evt) {
						MenuBar.log.finest("fileMenu.handleEvent, event=" + evt); //$NON-NLS-1$
						MenuBar.this.updateSubHistoryMenuItem(OSDE.STRING_EMPTY); //$NON-NLS-1$
					}
					public void menuHidden(MenuEvent evt) {
						log.finest("fileMenu.menuHidden " + evt); //$NON-NLS-1$
					}
				});
				{
					this.newFileMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.newFileMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0002)); //$NON-NLS-1$
					this.newFileMenuItem.setImage(SWTResourceManager.getImage("osde/resource/NewHot.gif")); //$NON-NLS-1$
					this.newFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("newFileMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.application.getDeviceSelectionDialog().checkDataSaved()) {
								MenuBar.this.application.getDeviceSelectionDialog().setupDataChannels(MenuBar.this.application.getActiveDevice());
							}
						}
					});
				}
				{
					this.openFileMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.openFileMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0003)); 
					this.openFileMenuItem.setImage(SWTResourceManager.getImage("osde/resource/OpenHot.gif")); //$NON-NLS-1$
					this.openFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("openFileMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							openFileDialog(Messages.getString(MessageIds.OSDE_MSGT0004));
						}
					});
				}
				{
					this.saveFileMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.saveFileMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0005));
					this.saveFileMenuItem.setImage(SWTResourceManager.getImage("osde/resource/SaveHot.gif")); //$NON-NLS-1$
					this.saveFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("saveFileMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							Channel activeChannel = MenuBar.this.channels.getActiveChannel();
							if (activeChannel != null) {
								if (!activeChannel.isSaved())
									MenuBar.this.saveOsdFile(Messages.getString(MessageIds.OSDE_MSGT0006), OSDE.STRING_EMPTY);  //$NON-NLS-2$
								else
									MenuBar.this.saveOsdFile(Messages.getString(MessageIds.OSDE_MSGT0007), activeChannel.getFileName()); 
							}
						}
					});
				}
				{
					this.saveAsFileMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.saveAsFileMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0008));
					this.saveAsFileMenuItem.setImage(SWTResourceManager.getImage("osde/resource/SaveAsHot.gif")); //$NON-NLS-1$
					this.saveAsFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("saveAsFileMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.saveOsdFile(Messages.getString(MessageIds.OSDE_MSGT0006), OSDE.STRING_EMPTY); //$NON-NLS-1$
						}
					});
				}
				{
					this.historyFileMenuItem = new MenuItem(this.fileMenu, SWT.CASCADE);
					this.historyFileMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0009));
					{
						this.fileHistoryMenu = new Menu(this.historyFileMenuItem);
						this.historyFileMenuItem.setMenu(this.fileHistoryMenu);
					}
				}
				{
					new MenuItem(this.fileMenu, SWT.SEPARATOR);
				}
				{
					this.importFileMenuItem = new MenuItem(this.fileMenu, SWT.CASCADE);
					this.importFileMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0010)); 
					{
						this.importMenu = new Menu(this.importFileMenuItem);
						this.importFileMenuItem.setMenu(this.importMenu);
						{
							this.csvImportMenuItem1 = new MenuItem(this.importMenu, SWT.PUSH);
							this.csvImportMenuItem1.setText(Messages.getString(MessageIds.OSDE_MSGT0011));
							this.csvImportMenuItem1.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									MenuBar.log.finest("csvImportMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
									importFileCSV(Messages.getString(MessageIds.OSDE_MSGT0012), false);
								}
							});
						}
						{
							this.csvImportMenuItem2 = new MenuItem(this.importMenu, SWT.PUSH);
							this.csvImportMenuItem2.setText(Messages.getString(MessageIds.OSDE_MSGT0013));
							this.csvImportMenuItem2.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									MenuBar.log.finest("csvImportMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
									importFileCSV(Messages.getString(MessageIds.OSDE_MSGT0014), true);
								}
							});
						}
					}
				}
				{
					this.exportFileMenuItem = new MenuItem(this.fileMenu, SWT.CASCADE);
					this.exportFileMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0015));
					{
						this.exportMenu = new Menu(this.exportFileMenuItem);
						this.exportFileMenuItem.setMenu(this.exportMenu);
						{
							this.csvExportMenuItem1 = new MenuItem(this.exportMenu, SWT.CASCADE);
							this.csvExportMenuItem1.setText(Messages.getString(MessageIds.OSDE_MSGT0016));
							this.csvExportMenuItem1.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									MenuBar.log.finest("csvExportMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
									exportFileCSV(Messages.getString(MessageIds.OSDE_MSGT0017), false);
								}
							});
						}
					}
					{
						this.csvExportMenuItem2 = new MenuItem(this.exportMenu, SWT.CASCADE);
						this.csvExportMenuItem2.setText(Messages.getString(MessageIds.OSDE_MSGT0018));
						this.csvExportMenuItem2.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								MenuBar.log.finest("csvExportMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
								exportFileCSV(Messages.getString(MessageIds.OSDE_MSGT0019), true); 
							}
						});
					}
				}
				{
					new MenuItem(this.fileMenu, SWT.SEPARATOR);
				}
				{
					this.preferencesFileMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.preferencesFileMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0020));
					this.preferencesFileMenuItem.setImage(SWTResourceManager.getImage("osde/resource/SettingsHot.gif")); //$NON-NLS-1$
					this.preferencesFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("preferencesFileMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							// check if other none modal dialog is open
							DeviceDialog deviceDialog = MenuBar.this.application.getDeviceDialog();
							if (deviceDialog == null || deviceDialog.isDisposed()) {
								MenuBar.this.application.openSettingsDialog();
								MenuBar.this.application.setStatusMessage(OSDE.STRING_EMPTY); //$NON-NLS-1$
							}
							else
								MenuBar.this.application.setStatusMessage(Messages.getString(MessageIds.OSDE_MSGI0001), SWT.COLOR_RED); 
						}
					});
				}
				{
					new MenuItem(this.fileMenu, SWT.SEPARATOR);
				}
				{
					this.exitMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.exitMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0021));
					this.exitMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("exitMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							DeviceSelectionDialog deviceSelect = MenuBar.this.application.getDeviceSelectionDialog();
							if (deviceSelect.checkDataSaved()) {
								MenuBar.this.parent.getParent().dispose();
							}
						}
					});
				}
				this.fileMenuItem.setMenu(this.fileMenu);
			}
		}
		{
			this.editMenuItem = new MenuItem(this.parent, SWT.CASCADE);
			this.editMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0022));
			this.editMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.fine("editMenuItem.helpRequested, event=" + evt); //$NON-NLS-1$
					MenuBar.this.application.openHelpDialog(OSDE.STRING_EMPTY, "HelpInfo_31.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			{
				this.editMenu = new Menu(this.editMenuItem);
				this.editMenuItem.setMenu(this.editMenu);
				{
					this.activateZoomGraphicMenuItem = new MenuItem(this.editMenu, SWT.PUSH);
					this.activateZoomGraphicMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0023));
					this.activateZoomGraphicMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ZoomHot.gif")); //$NON-NLS-1$
					this.activateZoomGraphicMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("activateZoomGraphicMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.setGraphicsMode(GraphicsWindow.MODE_ZOOM, true);
						}
					});
				}
				{
					this.resetZoomGraphicMenuItem = new MenuItem(this.editMenu, SWT.PUSH);
					this.resetZoomGraphicMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0024));
					this.resetZoomGraphicMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ExpandHot.gif")); //$NON-NLS-1$
					this.resetZoomGraphicMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("resetZoomGraphicMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.setGraphicsMode(GraphicsWindow.MODE_RESET, false);
						}
					});
				}
				{
					this.panGraphicMenuItem = new MenuItem(this.editMenu, SWT.PUSH);
					this.panGraphicMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0025));
					this.panGraphicMenuItem.setImage(SWTResourceManager.getImage("osde/resource/PanHot.gif")); //$NON-NLS-1$
					this.panGraphicMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("panGraphicMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.setGraphicsMode(GraphicsWindow.MODE_PAN, true);
						}
					});
				}
				{
					new MenuItem(this.editMenu, SWT.SEPARATOR);
				}
				{
					this.copyGraphicMenuItem = new MenuItem(this.editMenu, SWT.PUSH);
					this.copyGraphicMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0026));
					this.copyGraphicMenuItem.setEnabled(false); //TODO enable after implementation
					this.copyGraphicMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("copyGraphicMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							//							Clipboard clipboard = new Clipboard(display);
							//			        RTFTransfer rftTransfer = RTFTransfer.getInstance();
							//			        clipboard.setContents(new String[]{"graphics copy"}, new Transfer[]{rftTransfer});
							//			        clipboard.dispose();
						}
					});
				}
				{
					this.copyTableMenuItem = new MenuItem(this.editMenu, SWT.PUSH);
					this.copyTableMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0027));
					this.copyTableMenuItem.setEnabled(false); //TODO enable after implementation
					this.copyTableMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("copyTableMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							//							Clipboard clipboard = new Clipboard(display);
							//			        TextTransfer transfer = TextTransfer.getInstance();
							//			        clipboard.setContents(new String[]{"graphics copy"}, new Transfer[]{rftTransfer});
							//			        clipboard.dispose();						}
						}
					});
				}
			}
		}
		{
			this.deviceMenuItem = new MenuItem(this.parent, SWT.CASCADE);
			this.deviceMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0028));
			this.deviceMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.fine("deviceMenuItem.helpRequested, event=" + evt); //$NON-NLS-1$
					MenuBar.this.application.openHelpDialog(OSDE.STRING_EMPTY, "HelpInfo_32.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			{
				this.deviceMenu = new Menu(this.deviceMenuItem);
				this.deviceMenuItem.setMenu(this.deviceMenu);
				{
					this.toolBoxDeviceMenuItem = new MenuItem(this.deviceMenu, SWT.PUSH);
					this.toolBoxDeviceMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0029));
					this.toolBoxDeviceMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
					this.toolBoxDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("toolBoxDeviceMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.openDeviceDialog();
						}
					});
				}
				{
					new MenuItem(this.deviceMenu, SWT.SEPARATOR);
				}
				{
					this.portMenuItem = new MenuItem(this.deviceMenu, SWT.PUSH);
					this.portMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0049));
					this.portMenuItem.setImage(SWTResourceManager.getImage("osde/resource/BulletHotRed.gif")); //$NON-NLS-1$
					this.portMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("selectDeviceMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							IDevice activeDevice = MenuBar.this.application.getActiveDevice();
							if(activeDevice != null) activeDevice.openCloseSerialPort();
						}
					});
				}
				{
					new MenuItem(this.deviceMenu, SWT.SEPARATOR);
				}
				{
					this.selectDeviceMenuItem = new MenuItem(this.deviceMenu, SWT.PUSH);
					this.selectDeviceMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0030));
					this.selectDeviceMenuItem.setImage(SWTResourceManager.getImage("osde/resource/DeviceSelectionHot.gif")); //$NON-NLS-1$
					this.selectDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("selectDeviceMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							DeviceSelectionDialog deviceSelection = MenuBar.this.application.getDeviceSelectionDialog();
							if (deviceSelection.checkDataSaved()) {
								deviceSelection.open();
							}
						}
					});
				}
				{
					this.prevDeviceMenuItem = new MenuItem(this.deviceMenu, SWT.PUSH);
					this.prevDeviceMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLefHot.gif")); //$NON-NLS-1$
					this.prevDeviceMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0031)); 
					this.prevDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("prevDeviceMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.application.getActiveDevice().getSerialPort() == null || !MenuBar.this.application.getActiveDevice().getSerialPort().isConnected()) { // allow device switch only if port noct connected
								DeviceConfiguration deviceConfig;
								DeviceSelectionDialog deviceSelect = MenuBar.this.application.getDeviceSelectionDialog();
								if (deviceSelect.checkDataSaved()) {
									int selection = deviceSelect.getActiveDevices().indexOf(deviceSelect.getActiveConfig().getName());
									int size = deviceSelect.getActiveDevices().size();
									if (selection > 0 && selection <= size) {
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(selection - 1));
									}
									else
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(size - 1));

									// if a device tool box is open, dispose it
									if (MenuBar.this.application.getDeviceDialog() != null && !MenuBar.this.application.getDeviceDialog().isDisposed()) {
										MenuBar.this.application.getDeviceDialog().dispose();
									}

									deviceSelect.setActiveConfig(deviceConfig);
									deviceSelect.setupDevice();
								}
							}
							else {
								MenuBar.this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0002)); 
							}
						}
					});
				}
				{
					this.nextDeviceMenuItem = new MenuItem(this.deviceMenu, SWT.PUSH);
					this.nextDeviceMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRightHot.gif")); //$NON-NLS-1$
					this.nextDeviceMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0032)); 
					this.nextDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("nextDeviceMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.application.getActiveDevice().getSerialPort() == null || !MenuBar.this.application.getActiveDevice().getSerialPort().isConnected()) { // allow device switch only if port noct connected
								DeviceConfiguration deviceConfig;
								DeviceSelectionDialog deviceSelect = MenuBar.this.application.getDeviceSelectionDialog();
								if (deviceSelect.checkDataSaved()) {
									int selection = deviceSelect.getActiveDevices().indexOf(deviceSelect.getActiveConfig().getName());
									int size = deviceSelect.getActiveDevices().size() - 1;
									if (selection >= 0 && selection < size)
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(selection + 1));
									else
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(0));

									// if a device tool box is open, dispose it
									if (MenuBar.this.application.getDeviceDialog() != null && !MenuBar.this.application.getDeviceDialog().isDisposed()) {
										MenuBar.this.application.getDeviceDialog().dispose();
									}

									deviceSelect.setActiveConfig(deviceConfig);
									deviceSelect.setupDevice();
								}
							}
							else {
								MenuBar.this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0002)); 
							}
						}
					});
				}
			}
		}
		{
			this.graphicsMenuItem = new MenuItem(this.parent, SWT.CASCADE);
			this.graphicsMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0033));
			this.graphicsMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.fine("graphicsMenuItem.helpRequested, event=" + evt); //$NON-NLS-1$
					MenuBar.this.application.openHelpDialog(OSDE.STRING_EMPTY, "HelpInfo_33.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			{
				this.graphicsMenu = new Menu(this.graphicsMenuItem);
				this.graphicsMenuItem.setMenu(this.graphicsMenu);
				{
					this.saveDefaultGraphicsTemplateItem = new MenuItem(this.graphicsMenu, SWT.PUSH);
					this.saveDefaultGraphicsTemplateItem.setText(Messages.getString(MessageIds.OSDE_MSGT0034));
					this.saveDefaultGraphicsTemplateItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("saveGraphicsTemplateItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.channels.getActiveChannel().saveTemplate();
						}
					});
				}
				{
					this.saveAsGraphicsTemplateItem = new MenuItem(this.graphicsMenu, SWT.PUSH);
					this.saveAsGraphicsTemplateItem.setText(Messages.getString(MessageIds.OSDE_MSGT0035)); 
					this.saveAsGraphicsTemplateItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("saveGraphicsTemplateItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.log.fine("templatePath = " + Settings.getInstance().getGraphicsTemplatePath()); //$NON-NLS-1$
							Channel activeChannel = MenuBar.this.channels.getActiveChannel();
							if (activeChannel != null) {
								GraphicsTemplate template = activeChannel.getTemplate();
								FileDialog fileDialog = MenuBar.this.application.openFileSaveDialog(Messages.getString(MessageIds.OSDE_MSGT0036), new String[] { Settings.GRAPHICS_TEMPLATES_EXTENSION }, Settings.getInstance() 
										.getGraphicsTemplatePath(), template.getDefaultFileName());
								MenuBar.log.fine("templateFilePath = " + fileDialog.getFileName()); //$NON-NLS-1$
								template.setNewFileName(fileDialog.getFileName());
								activeChannel.saveTemplate();
							}
						}
					});
				}
				{
					this.restoreGraphicsTemplateItem = new MenuItem(this.graphicsMenu, SWT.PUSH);
					this.restoreGraphicsTemplateItem.setText(Messages.getString(MessageIds.OSDE_MSGT0037));
					this.restoreGraphicsTemplateItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("restoreGraphicsTemplateItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							FileDialog fileDialog = MenuBar.this.application.openFileOpenDialog(Messages.getString(MessageIds.OSDE_MSGT0038), new String[] { Settings.GRAPHICS_TEMPLATES_EXTENSION }, Settings.getInstance() 
									.getGraphicsTemplatePath());
							Channel activeChannel = MenuBar.this.channels.getActiveChannel();
							GraphicsTemplate template = activeChannel.getTemplate();
							template.setNewFileName(fileDialog.getFileName());
							MenuBar.log.fine("templateFilePath = " + fileDialog.getFileName()); //$NON-NLS-1$
							template.load();
							if (activeChannel.getActiveRecordSet() != null) {
								activeChannel.applyTemplate(activeChannel.getActiveRecordSet().getName());
								activeChannel.getActiveRecordSet().setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
							}
						}
					});
				}
			}
		}
		{
			this.viewMenuItem = new MenuItem(this.parent, SWT.CASCADE);
			this.viewMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0039)); 
			this.viewMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.fine("viewMenuItem.helpRequested, event=" + evt); //$NON-NLS-1$
					MenuBar.this.application.openHelpDialog(OSDE.STRING_EMPTY, "HelpInfo_34.html"); //$NON-NLS-1$
				}
			});
			{
				this.viewMenu = new Menu(this.viewMenuItem);
				this.viewMenuItem.setMenu(this.viewMenu);
				{
					this.curveSelectionMenuItem = new MenuItem(this.viewMenu, SWT.CHECK);
					this.curveSelectionMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0040)); 
					this.curveSelectionMenuItem.setSelection(true);
					this.curveSelectionMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("kurveSelectionMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.curveSelectionMenuItem.getSelection()) {
								MenuBar.this.application.setCurveSelectorEnabled(true);
							}
							else {
								MenuBar.this.application.setCurveSelectorEnabled(false);
							}
						}
					});
				}
				{
					new MenuItem(this.viewMenu, SWT.SEPARATOR);
				}
				{
					this.graphicsHeaderMenuItem = new MenuItem(this.viewMenu, SWT.CHECK);
					this.graphicsHeaderMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0041));
					this.graphicsHeaderMenuItem.setSelection(false);
					this.graphicsHeaderMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("graphicsHeaderMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.graphicsHeaderMenuItem.getSelection()) {
								MenuBar.this.application.enableGraphicsHeader(true);
								MenuBar.this.application.updateDisplayTab();
							}
							else {
								MenuBar.this.application.enableGraphicsHeader(false);
								MenuBar.this.application.updateDisplayTab();
							}
						}
					});
				}
				{
					new MenuItem(this.viewMenu, SWT.SEPARATOR);
				}
				{
					this.recordCommentMenuItem = new MenuItem(this.viewMenu, SWT.CHECK);
					this.recordCommentMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0042)); 
					this.recordCommentMenuItem.setSelection(false);
					this.recordCommentMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("recordCommentMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuBar.this.recordCommentMenuItem.getSelection()) {
								MenuBar.this.application.enableRecordSetComment(true);
								MenuBar.this.application.updateDisplayTab();
							}
							else {
								MenuBar.this.application.enableRecordSetComment(false);
								MenuBar.this.application.updateDisplayTab();
							}
						}
					});
				}
			}
		}
		{
			this.helpMenuItem = new MenuItem(this.parent, SWT.CASCADE);
			this.helpMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0043)); 
			this.helpMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.fine("helpMenuItem.helpRequested, event=" + evt); //$NON-NLS-1$
					MenuBar.this.application.openHelpDialog(OSDE.STRING_EMPTY, "HelpInfo_34.html"); //$NON-NLS-1$
				}
			});
			{
				this.helpMenu = new Menu(this.helpMenuItem);
				{
					this.contentsMenuItem = new MenuItem(this.helpMenu, SWT.PUSH);
					this.contentsMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0044)); 
					this.contentsMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("contentsMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.openHelpDialog(OSDE.STRING_EMPTY, "HelpInfo.html");  //$NON-NLS-1$
						}
					});
				}
				{
					this.webCheckMenuItem = new MenuItem(this.helpMenu, SWT.PUSH);
					this.webCheckMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0045)); 
					this.webCheckMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("webCheckMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.openWebBrowser(Messages.getString(MessageIds.OSDE_MSGT0046));
						}
					});
				}
				{
					this.aboutMenuItem = new MenuItem(this.helpMenu, SWT.PUSH);
					this.aboutMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0047)); 
					this.aboutMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("aboutMenuItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuBar.this.application.openAboutDialog();
						}
					});
				}
				this.helpMenuItem.setMenu(this.helpMenu);
			}
		}
	}

	/**
	 * update file history while add history file to history menu
	 * @param fullQualifiedFileName (/home/device/filename.osd)
	 */
	public void updateSubHistoryMenuItem(String fullQualifiedFileName) {
		List<String> refFileHistory = Settings.getInstance().getFileHistory();
		if (fullQualifiedFileName != null && fullQualifiedFileName.length() > 4) {
			final String newhistoryEntry = fullQualifiedFileName.replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX);

			if (refFileHistory.indexOf(newhistoryEntry) > -1) { // fileName already exist
				refFileHistory.remove(newhistoryEntry);
			}
			refFileHistory.add(0, newhistoryEntry);
		}
		// clean up the menu entries
		MenuItem[] menuItems = this.fileHistoryMenu.getItems();
		for (MenuItem menuItem : menuItems) {
			menuItem.dispose();
		}
		// fill with refreshed data
		for (Iterator<String> iterator = refFileHistory.iterator(); iterator.hasNext();) {
			String fullQualifiedFileReference = iterator.next();
			String shortFileReference = fullQualifiedFileReference.substring(fullQualifiedFileReference.lastIndexOf('/') + 1);
			final MenuItem historyImportMenuItem = new MenuItem(this.fileHistoryMenu, SWT.PUSH);
			historyImportMenuItem.setText(shortFileReference);
			historyImportMenuItem.setData(shortFileReference, fullQualifiedFileReference);
			historyImportMenuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					MenuBar.log.finest("historyImportMenuItem.widgetSelected, event=" + evt);//$NON-NLS-1$
					String fileName = (String) historyImportMenuItem.getData(historyImportMenuItem.getText());
					String fileType = fileName.substring(fileName.lastIndexOf('.') + 1);
					if (fileType != null && fileType.length() > 2) {
						MenuBar.log.fine("opening file = " + fileName);//$NON-NLS-1$
						if (fileType.equalsIgnoreCase(OSDE.FILE_ENDING_OSD)) { 
							openOsdFile(fileName);
						}
						else if (fileType.equalsIgnoreCase(OSDE.FILE_ENDING_LOV)) { 
							openLovFile(fileName);
						}
						else {
							MenuBar.this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0003)); 
						}
					}
				}
			});
		}
	}

	/**
	 * handles the import of an CVS file
	 * @param dialogName
	 * @param isRaw
	 */
	public void importFileCSV(String dialogName, final boolean isRaw) {
		IDevice activeDevice = this.application.getActiveDevice();
		if (activeDevice == null) {
			this.application.openMessageDialog(Messages.getString(OSDE.FILE_ENDING_STAR_CSV));
			return;
		}
		Settings deviceSetting = Settings.getInstance();
		String devicePath = this.application.getActiveDevice() != null ? OSDE.FILE_SEPARATOR_UNIX + this.application.getActiveDevice().getName() : OSDE.STRING_EMPTY;
		String path = deviceSetting.getDataFilePath() + devicePath + OSDE.FILE_SEPARATOR_UNIX;
		FileDialog csvFileDialog = this.application.openFileOpenDialog(dialogName, new String[] { OSDE.FILE_ENDING_STAR_CSV }, path);
		if (csvFileDialog.getFileName().length() > 4) {
			final String csvFilePath = csvFileDialog.getFilterPath() + OSDE.FILE_SEPARATOR_UNIX + csvFileDialog.getFileName();
			String fileName = csvFileDialog.getFileName();
			fileName = fileName.substring(0, fileName.indexOf('.'));

			final char listSeparator = deviceSetting.getListSeparator();
			final String recordSetNameExtend = fileName;
			try {
				CSVReaderWriter.read(listSeparator, csvFilePath, recordSetNameExtend, isRaw);
			}
			catch (Exception e) {
				log.log(Level.WARNING, e.getMessage(), e);
				MenuBar.this.application.openMessageDialog(e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage());
			}
		}
	}

	/**
	 * handles the export of an CVS file
	 * @param dialogName
	 * @param isRaw
	 */
	public void exportFileCSV(final String dialogName, final boolean isRaw) {
		final Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel == null) {
			this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0005)); 
			return;
		}
		RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
		if (activeRecordSet == null) {
			this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0005)); 
			return;
		}

		Settings deviceSetting = Settings.getInstance();
		String devicePath = this.application.getActiveDevice() != null ? OSDE.FILE_SEPARATOR_UNIX + this.application.getActiveDevice().getName() : OSDE.STRING_EMPTY;
		String path = deviceSetting.getDataFilePath() + devicePath + OSDE.FILE_SEPARATOR_UNIX;
		FileDialog csvFileDialog = this.application.openFileSaveDialog(dialogName, new String[] { OSDE.FILE_ENDING_STAR_CSV }, path, getFileNameProposal()); 
		String recordSetKey = activeRecordSet.getName();
		final String csvFilePath = csvFileDialog.getFilterPath() + OSDE.FILE_SEPARATOR_UNIX + csvFileDialog.getFileName();

		if (csvFilePath.length() > 4) { // file name has a reasonable length
			if (FileUtils.checkFileExist(csvFilePath) && SWT.NO == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0007, new Object[]{csvFilePath}))) { 
				return;
			}

			final char listSeparator = deviceSetting.getListSeparator();
			final String recordSetName = recordSetKey;
			this.readerWriterThread = new Thread() {
				public void run() {
					try {
						MenuBar.this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));
						CSVReaderWriter.write(listSeparator, recordSetName, csvFilePath, isRaw);
					}
					catch (Exception e) {
						log.log(Level.WARNING, e.getMessage(), e);
						MenuBar.this.application.openMessageDialog(e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage());
					}
					finally {
						MenuBar.this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
					}
				}
			};
			this.readerWriterThread.start();
		}
	}

	/**
	 * @return
	 */
	String getFileNameProposal() {
		String fileName = OSDE.STRING_EMPTY;
		if (Settings.getInstance().getUsageDateAsFileNameLeader()) {
			fileName = StringHelper.getDate() + OSDE.STRING_UNDER_BAR;
		}
		if (Settings.getInstance().getUsageObjectKeyInFileName() && Channels.getInstance().getActiveChannel() != null && Channels.getInstance().getActiveChannel().getActiveRecordSet() != null) {
			fileName = fileName + Channels.getInstance().getActiveChannel().getActiveRecordSet().getObjectKey() + OSDE.STRING_UNDER_BAR;
		}
		return fileName;
	}

	/**
	 * handles the file dialog to open OpenSerialData or LogView file
	 * @param dialogName
	 */
	public void openFileDialog(final String dialogName) {
		if (this.application.getDeviceSelectionDialog().checkDataSaved()) {
			Settings deviceSetting = Settings.getInstance();
			String devicePath = this.application.getActiveDevice() != null ? OSDE.FILE_SEPARATOR_UNIX + this.application.getActiveDevice().getName() : OSDE.STRING_EMPTY;
			String path = this.application.getActiveDevice() != null ? deviceSetting.getDataFilePath() + devicePath + OSDE.FILE_SEPARATOR_UNIX : deviceSetting.getDataFilePath();
			FileDialog openFileDialog = this.application.openFileOpenDialog(dialogName, new String[] { OSDE.FILE_ENDING_STAR_OSD, OSDE.FILE_ENDING_STAR_LOV }, path); 
			if (openFileDialog.getFileName().length() > 4) {
				String openFilePath = (openFileDialog.getFilterPath() + OSDE.FILE_SEPARATOR_UNIX + openFileDialog.getFileName()).replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX);

				if (openFilePath.toLowerCase().endsWith(OSDE.FILE_ENDING_OSD)) 
					openOsdFile(openFilePath);
				else if (openFilePath.toLowerCase().endsWith(OSDE.FILE_ENDING_LOV))
					openLovFile(openFilePath);
				else
					this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0008) + openFilePath); 
			}
		}
	}

	/**
	 * open a OpenSerialData file and load data into a cleaned device/channel
	 * @param openFilePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 * @throws DeclinedException
	 */
	public void openOsdFile(final String openFilePath) {
		try {
			//check current device and switch if required
			String fileDeviceName = OsdReaderWriter.getHeader(openFilePath).get(OSDE.DEVICE_NAME);
			String activeDeviceName = this.application.getActiveDevice().getName();
			if (!activeDeviceName.equals(fileDeviceName)) { // new device in file
				String msg = Messages.getString(MessageIds.OSDE_MSGI0009, new Object[]{fileDeviceName}); 
				if (SWT.NO == this.application.openYesNoMessageDialog(msg)) 
					return;			
				this.application.getDeviceSelectionDialog().setupDevice(fileDeviceName);				
			}
			
			String recordSetPropertys = OsdReaderWriter.getHeader(openFilePath).get("1 "+OSDE.RECORD_SET_NAME); //$NON-NLS-1$
			String channelConfigName = OsdReaderWriter.getRecordSetProperties(recordSetPropertys).get(OSDE.CHANNEL_CONFIG_NAME);
			// channel/configuration type is outlet
			boolean isChannelTypeOutlet = this.channels.getActiveChannel().getType() == ChannelTypes.TYPE_OUTLET.ordinal();
			if(isChannelTypeOutlet) {
				String[] splitChannel = channelConfigName.split(" ");
				int channelNumber = 1;
				try {
					channelNumber = splitChannel.length == 2 ? new Integer(splitChannel[1]) : (
						splitChannel.length > 2 ? new Integer(splitChannel[0]) : 1);
				}
				catch (NumberFormatException e) {// ignore
				}
				// at this point we have a channel/config ordinal
				Channel channel = this.channels.get(channelNumber);
				if (channel.size() > 0) { // check for records to be exchanged
					int answer = this.application.openOkCancelMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0010, new Object[]{channel.getConfigKey()})); 
					if (answer != SWT.OK) 
						return;				
				}
				// clean existing channel record sets for new data
				for (String recordSetKey : channel.getRecordSetNames()) {
					if (recordSetKey != null && recordSetKey.length() > 3) channel.remove(recordSetKey);
				}
			}
			else
				this.application.getDeviceSelectionDialog().setupDevice(fileDeviceName);				

			this.readerWriterThread = new Thread() {
				public void run() {
					try {
						OsdReaderWriter.read(openFilePath);
					}
					catch (Exception e) {
						log.log(Level.WARNING, e.getMessage(), e);
						MenuBar.this.application.openMessageDialog(e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage());
					}
				}
			};
			this.readerWriterThread.start();
			updateSubHistoryMenuItem(openFilePath);
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
			MenuBar.this.application.openMessageDialog(e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
	}

	/**
	 * handles the save as functionality
	 * @param dialogName
	 * @param fileName
	 */
	public void saveOsdFile(final String dialogName, final String fileName) {
		final Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel == null) {
			this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0011)); 
			return;
		}
		RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
		if (activeRecordSet == null) {
			this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0011));
			return;
		}

		String filePath;
		FileDialog fileDialog;
		Settings deviceSetting = Settings.getInstance();
		String devicePath = this.application.getActiveDevice() != null ? OSDE.FILE_SEPARATOR_UNIX + this.application.getActiveDevice().getName() : OSDE.STRING_EMPTY; 
		String path = deviceSetting.getDataFilePath() + devicePath;
		if (!FileUtils.checkDirectoryAndCreate(path)) {
			this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0012, new Object[] { path })); 
		}
		if (fileName == null || fileName.length() < 5 || fileName.equals(getFileNameProposal())) {
			fileDialog = this.application.openFileSaveDialog(dialogName, new String[] { OSDE.FILE_ENDING_STAR_OSD }, path + OSDE.FILE_SEPARATOR_UNIX, getFileNameProposal()); 
			filePath = fileDialog.getFilterPath() + OSDE.FILE_SEPARATOR_UNIX + fileDialog.getFileName();
		}
		else {
			filePath = path + OSDE.FILE_SEPARATOR_UNIX + fileName; // including ending ".osd"
		}

		if (filePath.length() > 4 && !filePath.endsWith(getFileNameProposal())) { // file name has a reasonable length
			while (filePath.toLowerCase().endsWith(OSDE.FILE_ENDING_DOT_OSD) || filePath.toLowerCase().endsWith(OSDE.FILE_ENDING_DOT_LOV)){ 
				filePath = filePath.substring(0, filePath.lastIndexOf('.'));
			}
			filePath = (filePath + OSDE.FILE_ENDING_DOT_OSD).replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX); //$NON-NLS-1$
			if (FileUtils.checkFileExist(filePath) && SWT.NO == this.application.openYesNoMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0013, new Object[]{filePath}))) { 
				return;
			}
			// rename existing file to *.bak
			FileUtils.renameFile(filePath, OSDE.FILE_ENDING_BAK);
			
			final String useFilePath = filePath;
			this.readerWriterThread = new Thread() {
				public void run() {
					try {
						MenuBar.this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_WAIT));
						OsdReaderWriter.write(useFilePath, activeChannel, 1);
					}
					catch (Exception e) {
						log.log(Level.WARNING, e.getMessage(), e);
						MenuBar.this.application.openMessageDialog(e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage());
					}
					finally {
						MenuBar.this.application.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_ARROW));
					}
				}
			};
			this.readerWriterThread.start();
			updateSubHistoryMenuItem(filePath);
			activeChannel.setFileName(filePath.replace(OSDE.FILE_SEPARATOR_WINDOWS, OSDE.FILE_SEPARATOR_UNIX));
			activeChannel.setSaved(true);
		}
	}

	/**
	 * open a LogView Data file and load data into a cleaned device/channel
	 * @param openFilePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NotSupportedFileFormatException
	 * @throws DeclinedException
	 */
	public void openLovFile(final String openFilePath) {
		try {
			//check current device and switch if required
			HashMap<String, String> lovHeader = LogViewReader.getHeader(openFilePath);
			String fileDeviceName = lovHeader.get(OSDE.DEVICE_NAME);
			String activeDeviceName = this.application.getActiveDevice().getName();
			if (!activeDeviceName.equals(fileDeviceName)) { // new device in file
				String msg = Messages.getString(MessageIds.OSDE_MSGI0014, new Object[]{fileDeviceName});
				if (SWT.NO == this.application.openYesNoMessageDialog(msg)) 
					return;			
				this.application.getDeviceSelectionDialog().setupDevice(fileDeviceName);				
			}
			
			int channelNumber = new Integer(lovHeader.get(OSDE.CHANNEL_CONFIG_NUMBER)).intValue();
			IDevice activeDevice = this.application.getActiveDevice();
			String channelType = ChannelTypes.values()[activeDevice.getChannelType(channelNumber)].name();
			String channelConfigName = activeDevice.getChannelName(channelNumber);
			log.fine("channelConfigName = " + channelConfigName + " (" + OSDE.CHANNEL_CONFIG_TYPE + channelType + "; " + OSDE.CHANNEL_CONFIG_NUMBER + channelNumber + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			Channel channel = this.channels.get(this.channels.getChannelNumber(channelConfigName));
			
			if(channel != null && this.channels.getActiveChannel() != null && this.channels.getActiveChannel().getType() == ChannelTypes.TYPE_OUTLET.ordinal()) {
				if (this.channels.getActiveChannelNumber() != this.channels.getChannelNumber(channelConfigName)) {
					int answer = this.application.openOkCancelMessageDialog(Messages.getString(MessageIds.OSDE_MSGI0006, new Object[] {channelConfigName}));
					if (answer != SWT.OK) 
						return;				
					
					// clean existing channel for new data, if channel does not exist ignore, 
					// this will be covered by the reader by creating a new channel
					for (String recordSetKey : channel.getRecordSetNames()) {
						if (recordSetKey != null && recordSetKey.length() > 3) channel.remove(recordSetKey);
					}
				}
			}
			else
				this.application.getDeviceSelectionDialog().setupDevice(fileDeviceName);				

			this.readerWriterThread = new Thread() {
				public void run() {
					try {
						LogViewReader.read(openFilePath);
					}
					catch (Exception e) {
						log.log(Level.WARNING, e.getMessage(), e);
						MenuBar.this.application.openMessageDialog(e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage());
					}
				}
			};
			this.readerWriterThread.start();
			updateSubHistoryMenuItem(openFilePath);
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
			MenuBar.this.application.openMessageDialog(e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
	}

	/**
	 * this function must only called by application which make secure to choose the right thread
	 * @param isOpenStatus
	 */
	public void setPortConnected(final boolean isOpenStatus) {
		if (!this.application.isDisposed()) {
			switch (this.iconSet) {
			case 1: // DeviceSerialPort.ICON_SET_START_STOP
				if (isOpenStatus) {
					this.portMenuItem.setImage(SWTResourceManager.getImage("osde/resource/RectangleHotRed.gif")); //$NON-NLS-1$
					this.portMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0071)); //$NON-NLS-1$
				}
				else {
					this.portMenuItem.setImage(SWTResourceManager.getImage("osde/resource/TriangleGreen.gif")); //$NON-NLS-1$
					this.portMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0070)); //$NON-NLS-1$
				}
				break;
			case 0: // DeviceSerialPort.ICON_SET_OPEN_CLOSE
			default:
				if (isOpenStatus) {
					this.portMenuItem.setImage(SWTResourceManager.getImage("osde/resource/BulletHotGreen.gif")); //$NON-NLS-1$
					this.portMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0048)); //$NON-NLS-1$
				}
				else {
					this.portMenuItem.setImage(SWTResourceManager.getImage("osde/resource/BulletHotRed.gif")); //$NON-NLS-1$
					this.portMenuItem.setText(Messages.getString(MessageIds.OSDE_MSGT0049)); //$NON-NLS-1$
				}
				break;
			}
		}
	}
	
	/**
	 * method to switch icon set by active device
	 * @param newIconSet
	 */
	public void setSerialPortIconSet(int newIconSet) {
		this.iconSet = newIconSet;
		this.setPortConnected(false);
	}

	/**
	 * set selection of record comment window 
	 * @param selected
	 */
	public void setRecordCommentMenuItemSelection(boolean selected) {
		this.recordCommentMenuItem.setSelection(selected);
	}
	
	/**
	 * set selection of record comment window 
	 * @param selected
	 */
	public void setGraphicsHeaderMenuItemSelection(boolean selected) {
		this.graphicsHeaderMenuItem.setSelection(selected);
	}
	
	/**
	 * set the state of device switch menu
	 * @param enabled
	 */
	public void enableDeviceSwitchButtons(boolean enabled) {
		this.prevDeviceMenuItem.setEnabled(enabled);
		this.nextDeviceMenuItem.setEnabled(enabled);
	}

}
