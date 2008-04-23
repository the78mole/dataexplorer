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

import java.io.FileNotFoundException;
import java.io.IOException;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import osde.config.GraphicsTemplate;
import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.DeviceDialog;
import osde.device.IDevice;
import osde.exception.DeclinedException;
import osde.exception.NotSupportedFileFormat;
import osde.io.CSVReaderWriter;
import osde.io.OsdReaderWriter;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.dialog.DeviceSelectionDialog;
import osde.ui.tab.GraphicsWindow;
import osde.utils.FileUtils;

/**
 * menu bar implementation class for the OpenSerialDataExplorer
 * @author Winfried Brügmann
 */
public class MenuBar {
	final static Logger						log			= Logger.getLogger(MenuBar.class.getName());
	final String									fileSep	= System.getProperty("file.separator");

	static Display								display;
	static Shell									shell;
	MenuItem											fileMenuItem;
	Menu													fileMenu;
	MenuItem											openFileMenuItem;
	MenuItem											historyFileMenuItem;
	MenuItem											toolBoxDeviceMenuItem, portMenuItem;
	MenuItem											aboutMenuItem;
	MenuItem											contentsMenuItem, webCheckMenuItem;
	Menu													helpMenu;
	MenuItem											helpMenuItem;
	MenuItem											graphicTabMenuItem, dataTableTabMenuItem, digitalTabMenuItem, analogTabMenuItem, cellVoltageTabMenuItem, recordSetCommentTabMenuItem, compareTabMenuItem;
	MenuItem											recordCommentMenuItem;
	MenuItem											curveSelectionMenuItem;
	Menu													viewMenu;
	MenuItem											viewMenuItem;
	Menu													graphicsMenu;
	MenuItem											graphicsMenuItem, saveDefaultGraphicsTemplateItem, saveGraphicsTemplateItem, restoreGraphicsTemplateItem;
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
			this.fileMenuItem.setText("Datei");
			this.fileMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.fine("fileMenuItem.helpRequested, event=" + evt);
					MenuBar.this.application.openHelpDialog("", "HelpInfo_3.html");
				}
			});
			{
				this.fileMenu = new Menu(this.fileMenuItem);
				this.fileMenu.addMenuListener(new MenuListener() {
					public void menuShown(MenuEvent evt) {
						MenuBar.log.finest("fileMenu.handleEvent, event=" + evt);
						MenuBar.this.updateSubHistoryMenuItem("");
					}
					public void menuHidden(MenuEvent evt) {
						log.finest("fileMenu.menuHidden " + evt);
					}
				});
				{
					this.newFileMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.newFileMenuItem.setText("Neu");
					this.newFileMenuItem.setImage(SWTResourceManager.getImage("osde/resource/NewHot.gif"));
					this.newFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("newFileMenuItem.widgetSelected, event=" + evt);
							if (MenuBar.this.application.getDeviceSelectionDialog().checkDataSaved()) {
								MenuBar.this.application.getDeviceSelectionDialog().setupDataChannels(MenuBar.this.application.getActiveDevice());
							}
						}
					});
				}
				{
					this.openFileMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.openFileMenuItem.setText("Öffnen");
					this.openFileMenuItem.setImage(SWTResourceManager.getImage("osde/resource/OpenHot.gif"));
					this.openFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("openFileMenuItem.widgetSelected, event=" + evt);
							if (MenuBar.this.application.getDeviceSelectionDialog().checkDataSaved()) {
								openOsdFileDialog("Öffne Datei ...");
							}
						}
					});
				}
				{
					this.saveFileMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.saveFileMenuItem.setText("Speichern");
					this.saveFileMenuItem.setImage(SWTResourceManager.getImage("osde/resource/SaveHot.gif"));
					this.saveFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("saveFileMenuItem.widgetSelected, event=" + evt);
							Channel activeChannel = MenuBar.this.channels.getActiveChannel();
							if (activeChannel != null) {
								if (MenuBar.this.channels.isSaved())
									MenuBar.this.saveOsdFile("OSD Datei - Speichern unter ...", "");
								else
									MenuBar.this.saveOsdFile("OSD Datei - Speichern", MenuBar.this.channels.getFullQualifiedFileName());
							}
						}
					});
				}
				{
					this.saveAsFileMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.saveAsFileMenuItem.setText("Speichern unter ...");
					this.saveAsFileMenuItem.setImage(SWTResourceManager.getImage("osde/resource/SaveAsHot.gif"));
					this.saveAsFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("saveAsFileMenuItem.widgetSelected, event=" + evt);
							MenuBar.this.saveOsdFile("OSD Datei - Speichern unter ...", "");
						}
					});
				}
				{
					this.historyFileMenuItem = new MenuItem(this.fileMenu, SWT.CASCADE);
					this.historyFileMenuItem.setText("Historie");
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
					this.importFileMenuItem.setText("Import");
					{
						this.importMenu = new Menu(this.importFileMenuItem);
						this.importFileMenuItem.setMenu(this.importMenu);
						{
							this.csvImportMenuItem1 = new MenuItem(this.importMenu, SWT.PUSH);
							this.csvImportMenuItem1.setText("CSV absolut");
							this.csvImportMenuItem1.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									MenuBar.log.finest("csvImportMenuItem.widgetSelected, event=" + evt);
									importFileCVS("Import CSV absolut", false);
								}
							});
						}
						{
							this.csvImportMenuItem2 = new MenuItem(this.importMenu, SWT.PUSH);
							this.csvImportMenuItem2.setText("CSV raw");
							this.csvImportMenuItem2.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									MenuBar.log.finest("csvImportMenuItem.widgetSelected, event=" + evt);
									importFileCVS("Import CSV raw", true);
								}
							});
						}
					}
				}
				{
					this.exportFileMenuItem = new MenuItem(this.fileMenu, SWT.CASCADE);
					this.exportFileMenuItem.setText("Export");
					{
						this.exportMenu = new Menu(this.exportFileMenuItem);
						this.exportFileMenuItem.setMenu(this.exportMenu);
						{
							this.csvExportMenuItem1 = new MenuItem(this.exportMenu, SWT.CASCADE);
							this.csvExportMenuItem1.setText("CSV absolut");
							this.csvExportMenuItem1.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									MenuBar.log.finest("csvExportMenuItem.widgetSelected, event=" + evt);
									exportFileCVS("Export CSV absolut", false);
								}
							});
						}
					}
					{
						this.csvExportMenuItem2 = new MenuItem(this.exportMenu, SWT.CASCADE);
						this.csvExportMenuItem2.setText("CSV raw");
						this.csvExportMenuItem2.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								MenuBar.log.finest("csvExportMenuItem.widgetSelected, event=" + evt);
								exportFileCVS("Export CSV raw", true);
							}
						});
					}
				}
				{
					new MenuItem(this.fileMenu, SWT.SEPARATOR);
				}
				{
					this.preferencesFileMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.preferencesFileMenuItem.setText("Einstellungen");
					this.preferencesFileMenuItem.setImage(SWTResourceManager.getImage("osde/resource/SettingsHot.gif"));
					this.preferencesFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("preferencesFileMenuItem.widgetSelected, event=" + evt);
							// check if other none modal dialog is open
							DeviceDialog deviceDialog = MenuBar.this.application.getDeviceDialog();
							if (deviceDialog == null || deviceDialog.isDisposed()) {
								MenuBar.this.application.openSettingsDialog();
								MenuBar.this.application.setStatusMessage("");
							}
							else
								MenuBar.this.application.setStatusMessage("Ein Gerätedialog geöffnet, ein Öffnen des Einstellungsdialoges ist zur Zeit nicht möglich !", SWT.COLOR_RED);
						}
					});
				}
				{
					new MenuItem(this.fileMenu, SWT.SEPARATOR);
				}
				{
					this.exitMenuItem = new MenuItem(this.fileMenu, SWT.PUSH);
					this.exitMenuItem.setText("Exit");
					this.exitMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("exitMenuItem.widgetSelected, event=" + evt);
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
			this.editMenuItem.setText("Bearbeiten");
			this.editMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.fine("editMenuItem.helpRequested, event=" + evt);
					MenuBar.this.application.openHelpDialog("", "HelpInfo_31.html");
				}
			});
			{
				this.editMenu = new Menu(this.editMenuItem);
				this.editMenuItem.setMenu(this.editMenu);
				{
					this.activateZoomGraphicMenuItem = new MenuItem(this.editMenu, SWT.PUSH);
					this.activateZoomGraphicMenuItem.setText("Zoom Graphikfenster aktivieren");
					this.activateZoomGraphicMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ZoomHot.gif"));
					this.activateZoomGraphicMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("activateZoomGraphicMenuItem.widgetSelected, event=" + evt);
							MenuBar.this.application.setGraphicsMode(GraphicsWindow.MODE_ZOOM, true);
						}
					});
				}
				{
					this.resetZoomGraphicMenuItem = new MenuItem(this.editMenu, SWT.PUSH);
					this.resetZoomGraphicMenuItem.setText("Zoom Graphikfenster zurücksetzen");
					this.resetZoomGraphicMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ExpandHot.gif"));
					this.resetZoomGraphicMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("resetZoomGraphicMenuItem.widgetSelected, event=" + evt);
							MenuBar.this.application.setGraphicsMode(GraphicsWindow.MODE_RESET, false);
						}
					});
				}
				{
					this.panGraphicMenuItem = new MenuItem(this.editMenu, SWT.PUSH);
					this.panGraphicMenuItem.setText("Inhalt Graphikfenster verschieben");
					this.panGraphicMenuItem.setImage(SWTResourceManager.getImage("osde/resource/PanHot.gif"));
					this.panGraphicMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("panGraphicMenuItem.widgetSelected, event=" + evt);
							MenuBar.this.application.setGraphicsMode(GraphicsWindow.MODE_PAN, true);
						}
					});
				}
				{
					new MenuItem(this.editMenu, SWT.SEPARATOR);
				}
				{
					this.copyGraphicMenuItem = new MenuItem(this.editMenu, SWT.PUSH);
					this.copyGraphicMenuItem.setText("Kopiere Graphikfenster");
					this.copyGraphicMenuItem.setEnabled(false); //TODO enable after implementation
					this.copyGraphicMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("copyGraphicMenuItem.widgetSelected, event=" + evt);
							//							Clipboard clipboard = new Clipboard(display);
							//			        RTFTransfer rftTransfer = RTFTransfer.getInstance();
							//			        clipboard.setContents(new String[]{"graphics copy"}, new Transfer[]{rftTransfer});
							//			        clipboard.dispose();
						}
					});
				}
				{
					this.copyTableMenuItem = new MenuItem(this.editMenu, SWT.PUSH);
					this.copyTableMenuItem.setText("Kopiere Tabelle");
					this.copyTableMenuItem.setEnabled(false); //TODO enable after implementation
					this.copyTableMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("copyTableMenuItem.widgetSelected, event=" + evt);
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
			this.deviceMenuItem.setText("Gerät");
			this.deviceMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.fine("deviceMenuItem.helpRequested, event=" + evt);
					MenuBar.this.application.openHelpDialog("", "HelpInfo_32.html");
				}
			});
			{
				this.deviceMenu = new Menu(this.deviceMenuItem);
				this.deviceMenuItem.setMenu(this.deviceMenu);
				{
					this.toolBoxDeviceMenuItem = new MenuItem(this.deviceMenu, SWT.PUSH);
					this.toolBoxDeviceMenuItem.setText("Geräte ToolBox");
					this.toolBoxDeviceMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif"));
					this.toolBoxDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("toolBoxDeviceMenuItem.widgetSelected, event=" + evt);
							MenuBar.this.application.openDeviceDialog();
						}
					});
				}
				{
					new MenuItem(this.deviceMenu, SWT.SEPARATOR);
				}
				{
					this.portMenuItem = new MenuItem(this.deviceMenu, SWT.PUSH);
					this.portMenuItem.setText("Port öffnen");
					this.portMenuItem.setImage(SWTResourceManager.getImage("osde/resource/BulletHotRed.gif"));
					this.portMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("selectDeviceMenuItem.widgetSelected, event=" + evt);
							MenuBar.this.application.openCloseSerialPort();
						}
					});
				}
				{
					new MenuItem(this.deviceMenu, SWT.SEPARATOR);
				}
				{
					this.selectDeviceMenuItem = new MenuItem(this.deviceMenu, SWT.PUSH);
					this.selectDeviceMenuItem.setText("Gerät auswählen");
					this.selectDeviceMenuItem.setImage(SWTResourceManager.getImage("osde/resource/DeviceSelectionHot.gif"));
					this.selectDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("selectDeviceMenuItem.widgetSelected, event=" + evt);
							DeviceSelectionDialog deviceSelection = MenuBar.this.application.getDeviceSelectionDialog();
							if (deviceSelection.checkDataSaved()) {
								deviceSelection.open();
							}
						}
					});
				}
				{
					this.prevDeviceMenuItem = new MenuItem(this.deviceMenu, SWT.PUSH);
					this.prevDeviceMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLefHot.gif"));
					this.prevDeviceMenuItem.setText("vorheriges Gerät");
					this.prevDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("prevDeviceMenuItem.widgetSelected, event=" + evt);
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
								MenuBar.this.application.openMessageDialog("Das Gerät kann nicht gewechselt werden, solange der serielle Port geöffnet ist!");
							}
						}
					});
				}
				{
					this.nextDeviceMenuItem = new MenuItem(this.deviceMenu, SWT.PUSH);
					this.nextDeviceMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRightHot.gif"));
					this.nextDeviceMenuItem.setText("nächstes Gerät");
					this.nextDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("nextDeviceMenuItem.widgetSelected, event=" + evt);
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
								MenuBar.this.application.openMessageDialog("Das Gerät kann nicht gewechselt werden, solange der serielle Port geöffnet ist!");
							}
						}
					});
				}
			}
		}
		{
			this.graphicsMenuItem = new MenuItem(this.parent, SWT.CASCADE);
			this.graphicsMenuItem.setText("Graphikvorlagen");
			this.graphicsMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.fine("graphicsMenuItem.helpRequested, event=" + evt);
					MenuBar.this.application.openHelpDialog("", "HelpInfo_33.html");
				}
			});
			{
				this.graphicsMenu = new Menu(this.graphicsMenuItem);
				this.graphicsMenuItem.setMenu(this.graphicsMenu);
				{
					this.saveDefaultGraphicsTemplateItem = new MenuItem(this.graphicsMenu, SWT.PUSH);
					this.saveDefaultGraphicsTemplateItem.setText("Graphikvorlage sichern");
					this.saveDefaultGraphicsTemplateItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("saveGraphicsTemplateItem.widgetSelected, event=" + evt);
							MenuBar.this.channels.getActiveChannel().saveTemplate();
						}
					});
				}
				{
					this.saveGraphicsTemplateItem = new MenuItem(this.graphicsMenu, SWT.PUSH);
					this.saveGraphicsTemplateItem.setText("Graphikvorlage sichern unter..");
					this.saveGraphicsTemplateItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("saveGraphicsTemplateItem.widgetSelected, event=" + evt);
							MenuBar.log.fine("templatePath = " + Settings.getInstance().getGraphicsTemplatePath());
							FileDialog fileDialog = MenuBar.this.application.openFileSaveDialog("Sichere GraphicsTemplate", new String[] { Settings.GRAPHICS_TEMPLATES_EXTENSION }, Settings.getInstance()
									.getGraphicsTemplatePath());
							Channel activeChannel = MenuBar.this.channels.getActiveChannel();
							GraphicsTemplate template = activeChannel.getTemplate();
							MenuBar.log.fine("templateFilePath = " + fileDialog.getFileName());
							template.setNewFileName(fileDialog.getFileName());
							activeChannel.saveTemplate();
						}
					});
				}
				{
					this.restoreGraphicsTemplateItem = new MenuItem(this.graphicsMenu, SWT.PUSH);
					this.restoreGraphicsTemplateItem.setText("Graphikvorlage laden");
					this.restoreGraphicsTemplateItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("restoreGraphicsTemplateItem.widgetSelected, event=" + evt);
							MenuBar.log.finest("saveGraphicsTemplateItem.widgetSelected, event=" + evt);
							FileDialog fileDialog = MenuBar.this.application.openFileOpenDialog("Lade GraphicsTemplate", new String[] { Settings.GRAPHICS_TEMPLATES_EXTENSION }, Settings.getInstance()
									.getGraphicsTemplatePath());
							Channel activeChannel = MenuBar.this.channels.getActiveChannel();
							GraphicsTemplate template = activeChannel.getTemplate();
							template.setNewFileName(fileDialog.getFileName());
							MenuBar.log.fine("templateFilePath = " + fileDialog.getFileName());
							template.load();
							if (activeChannel.getActiveRecordSet() != null) {
								MenuBar.this.channels.getActiveChannel().applyTemplate(activeChannel.getActiveRecordSet().getName());
							}
						}
					});
				}
			}
		}
		{
			this.viewMenuItem = new MenuItem(this.parent, SWT.CASCADE);
			this.viewMenuItem.setText("Ansicht");
			this.viewMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.fine("viewMenuItem.helpRequested, event=" + evt);
					MenuBar.this.application.openHelpDialog("", "HelpInfo_34.html");
				}
			});
			{
				this.viewMenu = new Menu(this.viewMenuItem);
				this.viewMenuItem.setMenu(this.viewMenu);
				{
					this.curveSelectionMenuItem = new MenuItem(this.viewMenu, SWT.CHECK);
					this.curveSelectionMenuItem.setText("Kurvenauswahl");
					this.curveSelectionMenuItem.setSelection(true);
					this.curveSelectionMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("kurveSelectionMenuItem.widgetSelected, event=" + evt);
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
					this.recordCommentMenuItem = new MenuItem(this.viewMenu, SWT.CHECK);
					this.recordCommentMenuItem.setText("Datensatzkommentar");
					this.recordCommentMenuItem.setSelection(false);
					this.recordCommentMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("dataCommentMenuItem.widgetSelected, event=" + evt);
							if (MenuBar.this.recordCommentMenuItem.getSelection()) {
								MenuBar.this.application.setRecordCommentEnabled(true);
								MenuBar.this.application.updateDisplayTab();
							}
							else {
								MenuBar.this.application.setRecordCommentEnabled(false);
								MenuBar.this.application.updateDisplayTab();
							}
						}
					});
				}
				{
					new MenuItem(this.viewMenu, SWT.SEPARATOR);
				}
				{
					this.graphicTabMenuItem = new MenuItem(this.viewMenu, SWT.PUSH);
					this.graphicTabMenuItem.setText("Graphikansicht");
					this.graphicTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("graphicTabMenuItem.widgetSelected, event=" + evt);
							MenuBar.this.application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_GRAPHIC);
						}
					});
				}
				{
					this.dataTableTabMenuItem = new MenuItem(this.viewMenu, SWT.PUSH);
					this.dataTableTabMenuItem.setText("Tabellenansicht");
					this.dataTableTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("dataTableTabMenuItem.widgetSelected, event=" + evt);
							MenuBar.this.application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_DATA_TABLE);
						}
					});
				}
				{
					this.analogTabMenuItem = new MenuItem(this.viewMenu, SWT.PUSH);
					this.analogTabMenuItem.setText("Analoganzeige");
					this.analogTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("analogTabMenuItem.widgetSelected, event=" + evt);
							MenuBar.this.application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_ANALOG);
						}
					});
				}
				{
					this.digitalTabMenuItem = new MenuItem(this.viewMenu, SWT.PUSH);
					this.digitalTabMenuItem.setText("Zahlenanzeige");
					this.digitalTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("digitalTabMenuItem.widgetSelected, event=" + evt);
							MenuBar.this.application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_DIGITAL);
						}
					});
				}
				{
					this.cellVoltageTabMenuItem = new MenuItem(this.viewMenu, SWT.PUSH);
					this.cellVoltageTabMenuItem.setText("Datensatzkommentar");
					this.cellVoltageTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("cellVoltageTabMenuItem.widgetSelected, event=" + evt);
							MenuBar.this.application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_CELL_VOLTAGE);
						}
					});
				}
				{
					this.compareTabMenuItem = new MenuItem(this.viewMenu, SWT.PUSH);
					this.compareTabMenuItem.setText("Kurvenvergleich");
					this.compareTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("compareTabMenuItem.widgetSelected, event=" + evt);
							MenuBar.this.application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_COMPARE);
						}
					});
				}
				{
					this.recordSetCommentTabMenuItem = new MenuItem(this.viewMenu, SWT.PUSH);
					this.recordSetCommentTabMenuItem.setText("Datensatzkommentar");
					this.recordSetCommentTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("setCommentTabMenuItem.widgetSelected, event=" + evt);
							MenuBar.this.application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_COMMENT);
						}
					});
				}
			}
		}
		{
			this.helpMenuItem = new MenuItem(this.parent, SWT.CASCADE);
			this.helpMenuItem.setText("Hilfe");
			this.helpMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					MenuBar.log.fine("helpMenuItem.helpRequested, event=" + evt);
					MenuBar.this.application.openHelpDialog("", "HelpInfo_34.html");
				}
			});
			{
				this.helpMenu = new Menu(this.helpMenuItem);
				{
					this.contentsMenuItem = new MenuItem(this.helpMenu, SWT.PUSH);
					this.contentsMenuItem.setText("Inhalt");
					this.contentsMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("contentsMenuItem.widgetSelected, event=" + evt);
							MenuBar.this.application.openHelpDialog("", "HelpInfo.html");
						}
					});
				}
				{
					this.webCheckMenuItem = new MenuItem(this.helpMenu, SWT.PUSH);
					this.webCheckMenuItem.setText("Versioncheck");
					this.webCheckMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("webCheckMenuItem.widgetSelected, event=" + evt);
							MenuBar.this.application.openWebBrowser("http://bruegmaenner.de/de/winfried/osde/Download.html");
						}
					});
				}
				{
					this.aboutMenuItem = new MenuItem(this.helpMenu, SWT.PUSH);
					this.aboutMenuItem.setText("About");
					this.aboutMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuBar.log.finest("aboutMenuItem.widgetSelected, event=" + evt);
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
	 * @param addDeviceFileName (device/filename.xyz)
	 */
	public void updateSubHistoryMenuItem(final String addDeviceFileName) {
		List<String> refFileHistory = Settings.getInstance().getFileHistory();
		if (addDeviceFileName != null && addDeviceFileName.length() > 4) {
			final String newhistoryEntry = addDeviceFileName.replace("\\", "\\\\"); // windows/filesep

			if (refFileHistory.indexOf(newhistoryEntry) > -1) { // fileName already exist
				refFileHistory.remove(newhistoryEntry);
			}
			refFileHistory.add(0, newhistoryEntry);
		}
		// clean up
		MenuItem[] menuItems = this.fileHistoryMenu.getItems();
		for (MenuItem menuItem : menuItems) {
			menuItem.dispose();
		}
		
		for (Iterator<String> iterator = refFileHistory.iterator(); iterator.hasNext();) {
			String fileReference = iterator.next();
			final MenuItem historyImportMenuItem = new MenuItem(this.fileHistoryMenu, SWT.PUSH);
			historyImportMenuItem.setText(fileReference);
			historyImportMenuItem.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					MenuBar.log.finest("historyImportMenuItem.widgetSelected, event=" + evt);
					String deviceFileName = historyImportMenuItem.getText();
					String fileType = deviceFileName.substring(deviceFileName.lastIndexOf('.')+1);
					if (fileType != null && fileType.length() > 2) {
						if (fileType.equalsIgnoreCase("OSD")) {
							MenuBar.log.info("opening file = " + Settings.getInstance().getDataFilePath() + MenuBar.this.fileSep + deviceFileName);
							openOsdFile(Settings.getInstance().getDataFilePath() + MenuBar.this.fileSep + deviceFileName);
						}
					}
					else {
						MenuBar.this.application.openMessageDialog("Die Datei kann auf Grund der Dateiendung nicht verarbeitet werden!");
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
	public void importFileCVS(String dialogName, final boolean isRaw) {
		IDevice activeDevice = this.application.getActiveDevice();
		if (activeDevice == null) {
			this.application.openMessageDialog("Vor dem Import bitte erst ein Gerät auswählen !");
			return;
		}
		Settings deviceSetting = Settings.getInstance();
		String devicePath = this.application.getActiveDevice() != null ? this.fileSep + this.application.getActiveDevice().getName() : "";
		String path = deviceSetting.getDataFilePath() + devicePath + this.fileSep;
		FileDialog csvFileDialog = this.application.openFileOpenDialog(dialogName, new String[] { "*.csv" }, path);
		if (csvFileDialog.getFileName().length() > 4) {
			final String csvFilePath = csvFileDialog.getFilterPath() + this.fileSep + csvFileDialog.getFileName();
			String fileName = csvFileDialog.getFileName();
			fileName = fileName.substring(0, fileName.indexOf('.'));

			final char listSeparator = deviceSetting.getListSeparator();
			final String recordSetNameExtend = fileName;
			try {
				CSVReaderWriter.read(listSeparator, csvFilePath, recordSetNameExtend, isRaw);
			}
			catch (Exception e) {
				log.log(Level.WARNING, e.getMessage(), e);
				MenuBar.this.application.openMessageDialog(e.getClass().getSimpleName() + " - " + e.getMessage());
			}
		}
	}

	/**
	 * handles the export of an CVS file
	 * @param dialogName
	 * @param isRaw
	 */
	public void exportFileCVS(final String dialogName, final boolean isRaw) {
		final Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel == null) {
			this.application.openMessageDialog("Es gibt keine Daten, die man sichern könnte ?");
			return;
		}
		RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
		if (activeRecordSet == null) {
			this.application.openMessageDialog("Es gibt keine Daten, die man sichern könnte ?");
			return;
		}

		Settings deviceSetting = Settings.getInstance();
		String devicePath = this.application.getActiveDevice() != null ? this.fileSep + this.application.getActiveDevice().getName() : "";
		String path = deviceSetting.getDataFilePath() + devicePath + this.fileSep;
		FileDialog csvFileDialog = this.application.openFileSaveDialog(dialogName, new String[] { "*.csv" }, path);
		String recordSetKey = activeRecordSet.getName();
		final String csvFilePath = csvFileDialog.getFilterPath() + this.fileSep + csvFileDialog.getFileName();

		if (csvFilePath.length() > 4) { // file name has a reasonable length
			if (FileUtils.checkFileExist(csvFilePath) && SWT.NO == this.application.openYesNoMessageDialog("Die Datei " + csvFilePath + " existiert bereits, soll die Datei überschrieben werden ?")) {
				return;
			}

			final char listSeparator = deviceSetting.getListSeparator();
			final String recordSetName = recordSetKey;
			this.readerWriterThread = new Thread() {
				public void run() {
					try {
						CSVReaderWriter.write(listSeparator, recordSetName, csvFilePath, isRaw);
					}
					catch (Exception e) {
						log.log(Level.WARNING, e.getMessage(), e);
						MenuBar.this.application.openMessageDialog(e.getClass().getSimpleName() + " - " + e.getMessage());
					}
				}
			};
			this.readerWriterThread.start();
		}
	}

	/**
	 * handles the import of an CVS file
	 * @param dialogName
	 * @param isRaw
	 */
	public void openOsdFileDialog(final String dialogName) {
		Settings deviceSetting = Settings.getInstance();
		String devicePath = this.application.getActiveDevice() != null ? this.fileSep + this.application.getActiveDevice().getName() : "";
		String path = this.application.getActiveDevice() != null ? deviceSetting.getDataFilePath() + devicePath + this.fileSep : deviceSetting.getDataFilePath();
		FileDialog openFileDialog = this.application.openFileOpenDialog(dialogName, new String[] { "*.osd" }, path);
		if (openFileDialog.getFileName().length() > 4) {
			final String openFilePath = openFileDialog.getFilterPath() + this.fileSep + openFileDialog.getFileName();
			String fileName = openFileDialog.getFileName();
			fileName = fileName.substring(0, fileName.indexOf('.'));

			openOsdFile(openFilePath);
		}
	}

	/**
	 * @param openFilePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NotSupportedFileFormat
	 * @throws DeclinedException
	 */
	void openOsdFile(final String openFilePath) {
		try {
			//check current device and switch if required
			String fileDeviceName = OsdReaderWriter.getDeviceName(openFilePath);
			String activeDeviceName = this.application.getActiveDevice().getName();
			if (!activeDeviceName.equals(fileDeviceName)) {
				String msg = "Das Gerät der ausgewählten Datei entspricht nicht dem aktiven Gerät. Soll auf das Gerät " + fileDeviceName + " umgeschaltet werden ?";
				if (SWT.NO == this.application.openYesNoMessageDialog(msg)) 
					throw new DeclinedException();
			}
			this.application.getDeviceSelectionDialog().setupDevice(fileDeviceName);

			this.readerWriterThread = new Thread() {
				public void run() {
					try {
						OsdReaderWriter.read(openFilePath);
					}
					catch (Exception e) {
						log.log(Level.WARNING, e.getMessage(), e);
						MenuBar.this.application.openMessageDialog(e.getClass().getSimpleName() + " - " + e.getMessage());
					}
				}
			};
			this.readerWriterThread.start();
			updateSubHistoryMenuItem(this.application.getActiveDevice().getName() + openFilePath.substring(openFilePath.lastIndexOf(this.fileSep)));
		}
		catch (DeclinedException e) {
			// ignore, user has declined device switch
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
			MenuBar.this.application.openMessageDialog(e.getClass().getSimpleName() + " - " + e.getMessage());
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
			this.application.openMessageDialog("Es gibt keine Daten, die man sichern könnte ?");
			return;
		}
		RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
		if (activeRecordSet == null) {
			this.application.openMessageDialog("Es gibt keine Daten, die man sichern könnte ?");
			return;
		}

		String filePath;
		FileDialog fileDialog;
		Settings deviceSetting = Settings.getInstance();
		String devicePath = this.application.getActiveDevice() != null ? this.fileSep + this.application.getActiveDevice().getName() : "";
		String path = deviceSetting.getDataFilePath() + devicePath + this.fileSep;
		if (fileName == null || fileName.length() < 5) {
			fileDialog = this.application.openFileSaveDialog(dialogName, new String[] { "*.osd" }, path);
			filePath = fileDialog.getFilterPath() + this.fileSep + fileDialog.getFileName();
		}
		else {
			filePath = path + fileName; // including ending ".osd"
		}

		if (filePath.length() > 4) { // file name has a reasonable length
			if (FileUtils.checkFileExist(filePath) && SWT.NO == this.application.openYesNoMessageDialog("Die Datei " + filePath + " existiert bereits, soll die Datei überschrieben werden ?")) {
				return;
			}

			final String useFilePath = filePath;
			this.readerWriterThread = new Thread() {
				public void run() {
					try {
						OsdReaderWriter.write(useFilePath, activeChannel);
					}
					catch (Exception e) {
						log.log(Level.WARNING, e.getMessage(), e);
						MenuBar.this.application.openMessageDialog(e.getClass().getSimpleName() + " - " + e.getMessage());
					}
				}
			};
			this.readerWriterThread.start();
			updateSubHistoryMenuItem(this.application.getActiveDevice().getName() + filePath.substring(filePath.lastIndexOf('/')));
		}
	}

	/**
	 * this function must only called by application which make secure to choose the right thread
	 * @param isOpenStatus
	 */
	public void setPortConnected(final boolean isOpenStatus) {
		if (isOpenStatus) {
			this.portMenuItem.setImage(SWTResourceManager.getImage("osde/resource/BulletHotGreen.gif"));
			this.portMenuItem.setText("Port schliessen");
		}
		else {
			if (!this.application.isDisposed()) {
				this.portMenuItem.setImage(SWTResourceManager.getImage("osde/resource/BulletHotRed.gif"));
				this.portMenuItem.setText("Port öffnen");
			}
		}
	}

	/**
	 * set selection of record comment window 
	 * @param selected
	 */
	public void setRecordCommentMenuItemSelection(boolean selected) {
		this.recordCommentMenuItem.setSelection(selected);
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
