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
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
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
import osde.exception.ApplicationConfigurationException;
import osde.io.CSVReaderWriter;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.dialog.DeviceSelectionDialog;
import osde.ui.tab.GraphicsWindow;

/**
 * menu bar implementation class for the OpenSerialDataExplorer
 * @author Winfried Brügmann
 */
public class MenuBar {
	private Logger												log			= Logger.getLogger(this.getClass().getName());
	private final String									fileSep	= System.getProperty("file.separator");

	static Display												display;
	static Shell													shell;
	private MenuItem											fileMenuItem;
	private Menu													fileMenu;
	private MenuItem											openFileMenuItem;
	private MenuItem											historyFileMenuItem;
	private MenuItem											toolBoxDeviceMenuItem, portMenuItem;
	private MenuItem											aboutMenuItem;
	private MenuItem											contentsMenuItem, webCheckMenuItem;
	private Menu													helpMenu;
	private MenuItem											helpMenuItem;
	private MenuItem											graphicTabMenuItem, dataTableTabMenuItem, digitalTabMenuItem, analogTabMenuItem, recordSetCommentTabMenuItem, compareTabMenuItem;
	private MenuItem											recordCommentMenuItem;
	private MenuItem											curveSelectionMenuItem;
	private Menu													viewMenu;
	private MenuItem											viewMenuItem;
	private Menu													graphicsMenu;
	private MenuItem											graphicsMenuItem, saveDefaultGraphicsTemplateItem, saveGraphicsTemplateItem, restoreGraphicsTemplateItem;
	private MenuItem											csvExportMenuItem1, csvExportMenuItem2;
	private MenuItem											nextDeviceMenuItem;
	private MenuItem											prevDeviceMenuItem;
	private MenuItem											selectDeviceMenuItem;
	private Menu													deviceMenu;
	private MenuItem											deviceMenuItem;
	private MenuItem											copyTableMenuItem;
	private MenuItem											copyGraphicMenuItem, activateZoomGraphicMenuItem, resetZoomGraphicMenuItem, panGraphicMenuItem;
	private Menu													editMenu;
	private MenuItem											editMenuItem;
	private MenuItem											exitMenuItem;
	private MenuItem											preferencesFileMenuItem;
	private Menu													exportMenu;
	private MenuItem											exportFileMenuItem;
	private MenuItem											csvImportMenuItem1, csvImportMenuItem2;
	private Menu													importMenu;
	private MenuItem											importFileMenuItem;
	private Menu													fileHistoryMenu;
	private MenuItem											saveAsFileMenuItem;
	private MenuItem											saveFileMenuItem;
	private MenuItem											newFileMenuItem;
	private final Menu										parent;
	private final OpenSerialDataExplorer	application;
	private final Channels								channels;

	public MenuBar(OpenSerialDataExplorer application, Menu menuParent) {
		this.application = application;
		this.parent = menuParent;
		this.channels = Channels.getInstance();
	}

	/**
	 * 
	 */
	public void create() {
		{
			fileMenuItem = new MenuItem(parent, SWT.CASCADE);
			fileMenuItem.setText("Datei");
			fileMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.fine("fileMenuItem.helpRequested, event="+evt);
					application.openHelpDialog("OpenSerialDataExplorer", "HelpInfo_3.html");
				}
			});
			{
				fileMenu = new Menu(fileMenuItem);
				{
					newFileMenuItem = new MenuItem(fileMenu, SWT.PUSH);
					newFileMenuItem.setText("Neu");
					newFileMenuItem.setImage(SWTResourceManager.getImage("osde/resource/NewHot.gif"));
					newFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("newFileMenuItem.widgetSelected, event=" + evt);
							application.getDeviceSelectionDialog().setupDataChannels(application.getActiveDevice());
						}
					});
				}
				{
					openFileMenuItem = new MenuItem(fileMenu, SWT.PUSH);
					openFileMenuItem.setText("Öffnen");
					openFileMenuItem.setImage(SWTResourceManager.getImage("osde/resource/OpenHot.gif"));
					openFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("openFileMenuItem.widgetSelected, event=" + evt);
							//TODO implement data file format and set ending as file open dialog filter 
							application.openMessageDialog("Entschuldigung, ein Datenformat ist noch nicht implementiert! Benutze anstatt CVS \"raw\" Format.");
							importFileCVS("Import CSV raw", true, true);
						}
					});
				}
				{
					saveFileMenuItem = new MenuItem(fileMenu, SWT.PUSH);
					saveFileMenuItem.setText("Speichern");
					saveFileMenuItem.setImage(SWTResourceManager.getImage("osde/resource/SaveHot.gif"));
					saveFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("saveFileMenuItem.widgetSelected, event=" + evt);
							//TODO implement data file format and set ending as file save dialog filter 
							application.openMessageDialog("Entschuldigung, ein Datenformat ist noch nicht implementiert! Benutze anstatt CVS \"raw\" Format.");
							application.getMenuBar().exportFileCVS("Export CSV raw", true);
						}
					});
				}
				{
					saveAsFileMenuItem = new MenuItem(fileMenu, SWT.PUSH);
					saveAsFileMenuItem.setText("Speichern unter ...");
					saveAsFileMenuItem.setImage(SWTResourceManager.getImage("osde/resource/SaveAsHot.gif"));
					saveAsFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("saveAsFileMenuItem.widgetSelected, event=" + evt);
							//TODO implement data file format and set ending as file save dialog filter 
							application.openMessageDialog("Entschuldigung, ein Datenformat ist noch nicht implementiert! Benutze anstatt CVS \"raw\" Format.");
							application.getMenuBar().exportFileCVS("Export CSV raw", true);
						}
					});
				}
				{
					historyFileMenuItem = new MenuItem(fileMenu, SWT.CASCADE);
					historyFileMenuItem.setText("Historie");
					historyFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("historyFileMenuItem.widgetSelected, event=" + evt);
							//TODO add your code for historyFileMenuItem.widgetSelected
							application.openMessageDialog("Diese Implementierung fehlt noch :-(, Einträge werden aber schon angehängt");
						}
					});
					{
						fileHistoryMenu = new Menu(historyFileMenuItem);
						historyFileMenuItem.setMenu(fileHistoryMenu);
					}
				}
				{
					new MenuItem(fileMenu, SWT.SEPARATOR);
				}
				{
					importFileMenuItem = new MenuItem(fileMenu, SWT.CASCADE);
					importFileMenuItem.setText("Import");
					{
						importMenu = new Menu(importFileMenuItem);
						importFileMenuItem.setMenu(importMenu);
						{
							csvImportMenuItem1 = new MenuItem(importMenu, SWT.PUSH);
							csvImportMenuItem1.setText("CSV absolut");
							csvImportMenuItem1.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									log.finest("csvImportMenuItem.widgetSelected, event=" + evt);
									importFileCVS("Import CSV absolut", false, true);
								}
							});
						}
						{
							csvImportMenuItem2 = new MenuItem(importMenu, SWT.PUSH);
							csvImportMenuItem2.setText("CSV raw");
							csvImportMenuItem2.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									log.finest("csvImportMenuItem.widgetSelected, event=" + evt);
									importFileCVS("Import CSV raw", true, true);
								}
							});
						}
					}
				}
				{
					exportFileMenuItem = new MenuItem(fileMenu, SWT.CASCADE);
					exportFileMenuItem.setText("Export");
					{
						exportMenu = new Menu(exportFileMenuItem);
						exportFileMenuItem.setMenu(exportMenu);
						{
							csvExportMenuItem1 = new MenuItem(exportMenu, SWT.CASCADE);
							csvExportMenuItem1.setText("CSV absolut");
							csvExportMenuItem1.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									log.finest("csvExportMenuItem.widgetSelected, event=" + evt);
									exportFileCVS("Export CSV absolut", false);
								}
							});
						}
					}
					{
						csvExportMenuItem2 = new MenuItem(exportMenu, SWT.CASCADE);
						csvExportMenuItem2.setText("CSV raw");
						csvExportMenuItem2.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("csvExportMenuItem.widgetSelected, event=" + evt);
								exportFileCVS("Export CSV raw", true);
							}
						});
					}
				}
				{
					new MenuItem(fileMenu, SWT.SEPARATOR);
				}
				{
					preferencesFileMenuItem = new MenuItem(fileMenu, SWT.PUSH);
					preferencesFileMenuItem.setText("Einstellungen");
					preferencesFileMenuItem.setImage(SWTResourceManager.getImage("osde/resource/SettingsHot.gif"));
					preferencesFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("preferencesFileMenuItem.widgetSelected, event=" + evt);
							// check if other none modal dialog is open
							DeviceDialog deviceDialog = application.getDeviceDialog();
							if (deviceDialog == null || deviceDialog.isDisposed()) {
								application.openSettingsDialog();
								application.setStatusMessage("");
							}
							else
								application.setStatusMessage("Ein Gerätedialog geöffnet, ein Öffnen des Einstellungsdialoges ist zur Zeit nicht möglich !", SWT.COLOR_RED);
						}
					});
				}
				{
					new MenuItem(fileMenu, SWT.SEPARATOR);
				}
				{
					exitMenuItem = new MenuItem(fileMenu, SWT.PUSH);
					exitMenuItem.setText("Exit");
					exitMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("exitMenuItem.widgetSelected, event=" + evt);
							DeviceSelectionDialog deviceSelect = application.getDeviceSelectionDialog();
							if (deviceSelect.checkDataSaved()) {
								parent.getParent().dispose();
							}
						}
					});
				}
				fileMenuItem.setMenu(fileMenu);
			}
		}
		{
			editMenuItem = new MenuItem(parent, SWT.CASCADE);
			editMenuItem.setText("Bearbeiten");
			editMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.fine("editMenuItem.helpRequested, event="+evt);
					application.openHelpDialog("OpenSerialDataExplorer", "HelpInfo_31.html");
				}
			});
			{
				editMenu = new Menu(editMenuItem);
				editMenuItem.setMenu(editMenu);
				{
					activateZoomGraphicMenuItem = new MenuItem(editMenu, SWT.PUSH);
					activateZoomGraphicMenuItem.setText("Zoom Graphikfenster aktivieren");
					activateZoomGraphicMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ZoomHot.gif"));
					activateZoomGraphicMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("activateZoomGraphicMenuItem.widgetSelected, event=" + evt);
							application.setGraphicsMode(GraphicsWindow.MODE_ZOOM, true);
						}
					});
				}
				{
					resetZoomGraphicMenuItem = new MenuItem(editMenu, SWT.PUSH);
					resetZoomGraphicMenuItem.setText("Zoom Graphikfenster zurücksetzen");
					resetZoomGraphicMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ExpandHot.gif"));
					resetZoomGraphicMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("resetZoomGraphicMenuItem.widgetSelected, event=" + evt);
							application.setGraphicsMode(GraphicsWindow.MODE_RESET ,false);						}
					});
				}
				{
					panGraphicMenuItem = new MenuItem(editMenu, SWT.PUSH);
					panGraphicMenuItem.setText("Inhalt Graphikfenster verschieben");
					panGraphicMenuItem.setImage(SWTResourceManager.getImage("osde/resource/PanHot.gif"));
					panGraphicMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("panGraphicMenuItem.widgetSelected, event=" + evt);
							application.setGraphicsMode(GraphicsWindow.MODE_PAN, true);
						}
					});
				}
				{
					new MenuItem(editMenu, SWT.SEPARATOR);
				}
				{
					copyGraphicMenuItem = new MenuItem(editMenu, SWT.PUSH);
					copyGraphicMenuItem.setText("Kopiere Graphikfenster");
					copyGraphicMenuItem.setEnabled(false); //TODO enable after implementation
					copyGraphicMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("copyGraphicMenuItem.widgetSelected, event=" + evt);
//							Clipboard clipboard = new Clipboard(display);
//			        RTFTransfer rftTransfer = RTFTransfer.getInstance();
//			        clipboard.setContents(new String[]{"graphics copy"}, new Transfer[]{rftTransfer});
//			        clipboard.dispose();
						}
					});
				}
				{
					copyTableMenuItem = new MenuItem(editMenu, SWT.PUSH);
					copyTableMenuItem.setText("Kopiere Tabelle");
					copyTableMenuItem.setEnabled(false); //TODO enable after implementation
					copyTableMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("copyTableMenuItem.widgetSelected, event=" + evt);
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
			deviceMenuItem = new MenuItem(parent, SWT.CASCADE);
			deviceMenuItem.setText("Gerät");
			deviceMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.fine("deviceMenuItem.helpRequested, event="+evt);
					application.openHelpDialog("OpenSerialDataExplorer", "HelpInfo_32.html");
				}
			});
			{
				deviceMenu = new Menu(deviceMenuItem);
				deviceMenuItem.setMenu(deviceMenu);
				{
					toolBoxDeviceMenuItem = new MenuItem(deviceMenu, SWT.PUSH);
					toolBoxDeviceMenuItem.setText("Geräte ToolBox");
					toolBoxDeviceMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif"));
					toolBoxDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("toolBoxDeviceMenuItem.widgetSelected, event=" + evt);
							application.openDeviceDialog();
						}
					});
				}
				{
					new MenuItem(deviceMenu, SWT.SEPARATOR);
				}
				{
					portMenuItem = new MenuItem(deviceMenu, SWT.PUSH);
					portMenuItem.setText("Port öffnen");
					portMenuItem.setImage(SWTResourceManager.getImage("osde/resource/BulletHotRed.gif"));
					portMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("selectDeviceMenuItem.widgetSelected, event=" + evt);
							application.openCloseSerialPort();		
						}
					});
				}
				{
					new MenuItem(deviceMenu, SWT.SEPARATOR);
				}
				{
					selectDeviceMenuItem = new MenuItem(deviceMenu, SWT.PUSH);
					selectDeviceMenuItem.setText("Gerät auswählen");
					selectDeviceMenuItem.setImage(SWTResourceManager.getImage("osde/resource/DeviceSelectionHot.gif"));
					selectDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("selectDeviceMenuItem.widgetSelected, event=" + evt);
							DeviceSelectionDialog deviceSelection = application.getDeviceSelectionDialog();
							if (deviceSelection.checkDataSaved()) {
								deviceSelection.open();
							}
						}
					});
				}
				{
					prevDeviceMenuItem = new MenuItem(deviceMenu, SWT.PUSH);
					prevDeviceMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLefHot.gif"));
					prevDeviceMenuItem.setText("vorheriges Gerät");
					prevDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("prevDeviceMenuItem.widgetSelected, event=" + evt);
							if (application.getActiveDevice().getSerialPort() == null || !application.getActiveDevice().getSerialPort().isConnected()) { // allow device switch only if port noct connected
								DeviceConfiguration deviceConfig;
								DeviceSelectionDialog deviceSelect = application.getDeviceSelectionDialog();
								if (deviceSelect.checkDataSaved()) {
									int selection = deviceSelect.getActiveDevices().indexOf(deviceSelect.getActiveConfig().getName());
									int size = deviceSelect.getActiveDevices().size();
									if (selection > 0 && selection <= size) {
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(selection - 1));
									}
									else
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(size - 1));
									
									// if a device tool box is open, dispose it
									if (application.getDeviceDialog() != null && !application.getDeviceDialog().isDisposed()) {
										application.getDeviceDialog().dispose();
									}
									
									deviceSelect.setActiveConfig(deviceConfig);
									deviceSelect.setupDevice();
								}
							}
							else {
								application.openMessageDialog("Das Gerät kann nicht gewechselt werden, solange der serielle Port geöffnet ist!");
							}
						}
					});
				}
				{
					nextDeviceMenuItem = new MenuItem(deviceMenu, SWT.PUSH);
					nextDeviceMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRightHot.gif"));
					nextDeviceMenuItem.setText("nächstes Gerät");
					nextDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("nextDeviceMenuItem.widgetSelected, event=" + evt);
							if (application.getActiveDevice().getSerialPort() == null || !application.getActiveDevice().getSerialPort().isConnected()) { // allow device switch only if port noct connected
								DeviceConfiguration deviceConfig;
								DeviceSelectionDialog deviceSelect = application.getDeviceSelectionDialog();
								if (deviceSelect.checkDataSaved()) {
									int selection = deviceSelect.getActiveDevices().indexOf(deviceSelect.getActiveConfig().getName());
									int size = deviceSelect.getActiveDevices().size() - 1;
									if (selection >= 0 && selection < size)
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(selection + 1));
									else
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(0));

									// if a device tool box is open, dispose it
									if (application.getDeviceDialog() != null && !application.getDeviceDialog().isDisposed()) {
										application.getDeviceDialog().dispose();
									}
									
									deviceSelect.setActiveConfig(deviceConfig);
									deviceSelect.setupDevice();
								}
							}
							else {
								application.openMessageDialog("Das Gerät kann nicht gewechselt werden, solange der serielle Port geöffnet ist!");
							}
						}
					});
				}
			}
		}
		{
			graphicsMenuItem = new MenuItem(parent, SWT.CASCADE);
			graphicsMenuItem.setText("Graphikvorlagen");
			graphicsMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.fine("graphicsMenuItem.helpRequested, event="+evt);
					application.openHelpDialog("OpenSerialDataExplorer", "HelpInfo_33.html");
				}
			});
			{
				graphicsMenu = new Menu(graphicsMenuItem);
				graphicsMenuItem.setMenu(graphicsMenu);
				{
					saveDefaultGraphicsTemplateItem = new MenuItem(graphicsMenu, SWT.PUSH);
					saveDefaultGraphicsTemplateItem.setText("Graphikvorlage sichern");
					saveDefaultGraphicsTemplateItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("saveGraphicsTemplateItem.widgetSelected, event=" + evt);
							channels.getActiveChannel().saveTemplate();
						}
					});
				}
				{
					saveGraphicsTemplateItem = new MenuItem(graphicsMenu, SWT.PUSH);
					saveGraphicsTemplateItem.setText("Graphikvorlage sichern unter..");
					saveGraphicsTemplateItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("saveGraphicsTemplateItem.widgetSelected, event=" + evt);
							log.fine("templatePath = " + Settings.getInstance().getGraphicsTemplatePath());
							FileDialog fileDialog = application.openFileSaveDialog("Sichere GraphicsTemplate", new String[] { Settings.GRAPHICS_TEMPLATES_EXTENSION }, Settings.getInstance()
									.getGraphicsTemplatePath());
							Channel activeChannel = channels.getActiveChannel();
							GraphicsTemplate template = activeChannel.getTemplate();
							log.fine("templateFilePath = " + fileDialog.getFileName());
							template.setNewFileName(fileDialog.getFileName());
							activeChannel.saveTemplate();
						}
					});
				}
				{
					restoreGraphicsTemplateItem = new MenuItem(graphicsMenu, SWT.PUSH);
					restoreGraphicsTemplateItem.setText("Graphikvorlage laden");
					restoreGraphicsTemplateItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("restoreGraphicsTemplateItem.widgetSelected, event=" + evt);
							log.finest("saveGraphicsTemplateItem.widgetSelected, event=" + evt);
							FileDialog fileDialog = application.openFileOpenDialog("Lade GraphicsTemplate", new String[] { Settings.GRAPHICS_TEMPLATES_EXTENSION }, Settings.getInstance().getGraphicsTemplatePath());
							Channel activeChannel = channels.getActiveChannel();
							GraphicsTemplate template = activeChannel.getTemplate();
							template.setNewFileName(fileDialog.getFileName());
							log.fine("templateFilePath = " + fileDialog.getFileName());
							template.load();
							if (activeChannel.getActiveRecordSet() != null) {
								channels.getActiveChannel().applyTemplate(activeChannel.getActiveRecordSet().getName());
							}
						}
					});
				}
			}
		}
		{
			viewMenuItem = new MenuItem(parent, SWT.CASCADE);
			viewMenuItem.setText("Ansicht");
			viewMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.fine("viewMenuItem.helpRequested, event="+evt);
					application.openHelpDialog("OpenSerialDataExplorer", "HelpInfo_34.html");
				}
			});
			{
				viewMenu = new Menu(viewMenuItem);
				viewMenuItem.setMenu(viewMenu);
				{
					curveSelectionMenuItem = new MenuItem(viewMenu, SWT.CHECK);
					curveSelectionMenuItem.setText("Kurvenauswahl");
					curveSelectionMenuItem.setSelection(true);
					curveSelectionMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("kurveSelectionMenuItem.widgetSelected, event=" + evt);
							if (curveSelectionMenuItem.getSelection()) {
								application.setCurveSelectorEnabled(true);
							}
							else {
								application.setCurveSelectorEnabled(false);
							}
						}
					});
				}
				{
					recordCommentMenuItem = new MenuItem(viewMenu, SWT.CHECK);
					recordCommentMenuItem.setText("Datensatzkommentar");
					recordCommentMenuItem.setSelection(false);
					recordCommentMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("dataCommentMenuItem.widgetSelected, event=" + evt);
							if (recordCommentMenuItem.getSelection()) {
								application.setRecordCommentEnabled(true);
								application.updateDisplayTab();
							}
							else {
								application.setRecordCommentEnabled(false);
								application.updateDisplayTab();
							}
						}
					});
				}
				{
					new MenuItem(viewMenu, SWT.SEPARATOR);
				}
				{
					graphicTabMenuItem = new MenuItem(viewMenu, SWT.PUSH);
					graphicTabMenuItem.setText("Graphikansicht");
					graphicTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("graphicTabMenuItem.widgetSelected, event=" + evt);
							application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_GRAPHIC);
						}
					});
				}
				{
					dataTableTabMenuItem = new MenuItem(viewMenu, SWT.PUSH);
					dataTableTabMenuItem.setText("Tabellenansicht");
					dataTableTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("dataTableTabMenuItem.widgetSelected, event=" + evt);
							application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_DATA_TABLE);
						}
					});
				}
				{
					analogTabMenuItem = new MenuItem(viewMenu, SWT.PUSH);
					analogTabMenuItem.setText("Analoganzeige");
					analogTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("analogTabMenuItem.widgetSelected, event=" + evt);
							application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_ANALOG);
						}
					});
				}
				{
					digitalTabMenuItem = new MenuItem(viewMenu, SWT.PUSH);
					digitalTabMenuItem.setText("Zahlenanzeige");
					digitalTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("digitalTabMenuItem.widgetSelected, event=" + evt);
							application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_DIGITAL);
						}
					});
				}
				{
					recordSetCommentTabMenuItem = new MenuItem(viewMenu, SWT.PUSH);
					recordSetCommentTabMenuItem.setText("Datensatzkommentar");
					recordSetCommentTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("setCommentTabMenuItem.widgetSelected, event=" + evt);
							application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_COMMENT);
						}
					});
				}
				{
					compareTabMenuItem = new MenuItem(viewMenu, SWT.PUSH);
					compareTabMenuItem.setText("Kurvenvergleich");
					compareTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("compareTabMenuItem.widgetSelected, event=" + evt);
							application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_COMPARE);
						}
					});
				}
			}
		}
		{
			helpMenuItem = new MenuItem(parent, SWT.CASCADE);
			helpMenuItem.setText("Hilfe");
			helpMenuItem.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.fine("helpMenuItem.helpRequested, event="+evt);
					application.openHelpDialog("OpenSerialDataExplorer", "HelpInfo_34.html");
				}
			});
			{
				helpMenu = new Menu(helpMenuItem);
				{
					contentsMenuItem = new MenuItem(helpMenu, SWT.PUSH);
					contentsMenuItem.setText("Inhalt");
					contentsMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("contentsMenuItem.widgetSelected, event=" + evt);
							application.openHelpDialog("OpenSerialDataExplorer", "HelpInfo.html");
						}
					});
				}
				{
					webCheckMenuItem = new MenuItem(helpMenu, SWT.PUSH);
					webCheckMenuItem.setText("Versioncheck");
					webCheckMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("webCheckMenuItem.widgetSelected, event=" + evt);
							application.openWebBrowser("http://bruegmaenner.de/de/winfried/osde/Download.html");
						}
					});
				}
				{
					aboutMenuItem = new MenuItem(helpMenu, SWT.PUSH);
					aboutMenuItem.setText("About");
					aboutMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("aboutMenuItem.widgetSelected, event=" + evt);
							application.openAboutDialog();
						}
					});
				}
				helpMenuItem.setMenu(helpMenu);
			}
		}
	}

	/**
	 * add history file to history menu
	 * @param newFileName
	 */
	public void addSubHistoryMenuItem(String newFileName) {
		MenuItem historyImportMenuItem = new MenuItem(fileHistoryMenu, SWT.PUSH);
		historyImportMenuItem.setText(newFileName);
		historyImportMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				log.finest("historyImportMenuItem.widgetSelected, event=" + evt);
				// TODO implement 
			}
		});
	}

	/**
	 * handles the import of an CVS file
	 * @param dialogName
	 * @param isRaw
	 * @param isFromFile
	 */
	public void importFileCVS(String dialogName, boolean isRaw, boolean isFromFile) {
		try {
			IDevice activeDevice = application.getActiveDevice();
			if (activeDevice == null) throw new ApplicationConfigurationException("Vor dem Import bitte erst ein Gerät auswählen !");
			String fileSep = System.getProperty("file.separator");
			Settings deviceSetting = Settings.getInstance();
			String devicePath = application.getActiveDevice() != null ? fileSep + application.getActiveDevice().getName() : "";
			String path = deviceSetting.getDataFilePath() + devicePath + fileSep;
			FileDialog csvFileDialog = application.openFileOpenDialog(dialogName, new String[] { "*.csv" }, path);
			if (csvFileDialog.getFileName().length() > 4) {
				String csvFilePath = csvFileDialog.getFilterPath() + fileSep + csvFileDialog.getFileName();
				String fileName = csvFileDialog.getFileName();
				fileName = fileName.substring(0, fileName.indexOf('.'));
				addSubHistoryMenuItem(csvFileDialog.getFileName());

				CSVReaderWriter.read(deviceSetting.getListSeparator(), csvFilePath, fileName, isRaw);
			}
		}
		catch (Exception e) {
			application.openMessageDialog(e.getClass().getSimpleName() + " - " + e.getMessage());
		}
	}

	/**
	 * handles the export of an CVS file
	 * @param dialogName
	 * @param isRaw
	 */
	public void exportFileCVS(String dialogName, boolean isRaw) {
		try {
			Settings deviceSetting = Settings.getInstance();
			String devicePath = application.getActiveDevice() != null ? fileSep + application.getActiveDevice().getName() : "";
			String path = deviceSetting.getDataFilePath() + devicePath + fileSep;
			FileDialog csvFileDialog = application.openFileSaveDialog(dialogName, new String[] { "*.csv" }, path);
			if (csvFileDialog.getFileName().length() > 4) {
				Channel activeChannel = channels.getActiveChannel();
				if (activeChannel == null) throw new ApplicationConfigurationException("Es gibt keine Daten, die man sichern könnte ?");
				RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
				if (activeRecordSet == null) throw new ApplicationConfigurationException("Es gibt keine Daten, die man sichern könnte ?");
				String recordSetKey = activeRecordSet.getName();
				String csvFilePath = csvFileDialog.getFilterPath() + fileSep + csvFileDialog.getFileName();
				addSubHistoryMenuItem(csvFileDialog.getFileName());
				CSVReaderWriter.write(deviceSetting.getListSeparator(), recordSetKey, csvFilePath, isRaw);
			}
		}
		catch (Exception e) {
			application.openMessageDialog(e.getClass().getSimpleName() + " - " + e.getMessage());
		}
	}
	
	/**
	 * this function must only called by application which make secure to choose the right thread
	 * @param isOpenStatus
	 */
	public void setPortConnected(final boolean isOpenStatus) {
		if (isOpenStatus) {
			portMenuItem.setImage(SWTResourceManager.getImage("osde/resource/BulletHotGreen.gif"));
			portMenuItem.setText("Port schliessen");
		}
		else {
			if (!application.isDisposed()) {
				portMenuItem.setImage(SWTResourceManager.getImage("osde/resource/BulletHotRed.gif"));
				portMenuItem.setText("Port öffnen");
			}
		}
	}

	/**
	 * set selection of record comment window 
	 * @param selected
	 */
	public void setRecordCommentMenuItemSelection(boolean selected) {
		recordCommentMenuItem.setSelection(selected);
	}
}
