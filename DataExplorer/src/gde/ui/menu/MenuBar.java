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
import osde.device.IDevice;
import osde.exception.ApplicationConfigurationException;
import osde.io.CSVReaderWriter;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.dialog.DeviceSelectionDialog;

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
	private MenuItem											toolBoxDeviceMenuItem;
	private MenuItem											aboutMenuItem;
	private MenuItem											contentsMenuItem;
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
	private MenuItem											copyGraphicMenuItem;
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
			{
				fileMenu = new Menu(fileMenuItem);
				{
					newFileMenuItem = new MenuItem(fileMenu, SWT.PUSH);
					newFileMenuItem.setText("Neu");
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
					preferencesFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("preferencesFileMenuItem.widgetSelected, event=" + evt);
							application.openSettingsDialog();
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
							parent.getParent().dispose();
						}
					});
				}
				fileMenuItem.setMenu(fileMenu);
			}
		}
		{
			editMenuItem = new MenuItem(parent, SWT.CASCADE);
			editMenuItem.setText("Bearbeiten");
			{
				editMenu = new Menu(editMenuItem);
				editMenuItem.setMenu(editMenu);
				{
					copyGraphicMenuItem = new MenuItem(editMenu, SWT.PUSH);
					copyGraphicMenuItem.setText("Kopiere Graphikfenster");
					copyGraphicMenuItem.setEnabled(false); //TODO enable after implementation
				}
				{
					copyTableMenuItem = new MenuItem(editMenu, SWT.PUSH);
					copyTableMenuItem.setText("Kopiere Tabelle");
					copyTableMenuItem.setEnabled(false); //TODO enable after implementation
				}
			}
		}
		{
			deviceMenuItem = new MenuItem(parent, SWT.CASCADE);
			deviceMenuItem.setText("Gerät");
			{
				deviceMenu = new Menu(deviceMenuItem);
				deviceMenuItem.setMenu(deviceMenu);
				{
					toolBoxDeviceMenuItem = new MenuItem(deviceMenu, SWT.PUSH);
					toolBoxDeviceMenuItem.setText("Geräte ToolBox");
					toolBoxDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("toolBoxDeviceMenuItem.widgetSelected, event=" + evt);
							application.openDeviceDialog();
						}
					});
				}
				{
					selectDeviceMenuItem = new MenuItem(deviceMenu, SWT.PUSH);
					selectDeviceMenuItem.setText("Gerät auswählen");
					selectDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("selectDeviceMenuItem.widgetSelected, event=" + evt);
							DeviceSelectionDialog deviceSelect = application.getDeviceSelectionDialog();
							if (deviceSelect.checkDataSaved()) {
								application.setActiveDevice(deviceSelect.open());
							}
						}
					});
				}
				{
					prevDeviceMenuItem = new MenuItem(deviceMenu, SWT.PUSH);
					prevDeviceMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowLeftGreen.gif"));
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
									if (application.getDeviceDialog() != null && !application.getDeviceDialog().isDisposed()) {
										application.getDeviceDialog().dispose();
									}
									deviceSelect.setActiveConfig(deviceConfig);
									if (!deviceSelect.checkPortSelection()) application.setActiveDevice(application.getDeviceSelectionDialog().open());
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
					nextDeviceMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowRightGreen.gif"));
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

									if (application.getDeviceDialog() != null && !application.getDeviceDialog().isDisposed()) {
										application.getDeviceDialog().dispose();
									}
									deviceSelect.setActiveConfig(deviceConfig);
									if (!deviceSelect.checkPortSelection()) application.setActiveDevice(application.getDeviceSelectionDialog().open());
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
			{
				viewMenu = new Menu(viewMenuItem);
				viewMenuItem.setMenu(viewMenu);
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
				{
					new MenuItem(viewMenu, SWT.SEPARATOR);
				}
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
			}
		}
		{
			helpMenuItem = new MenuItem(parent, SWT.CASCADE);
			helpMenuItem.setText("Hilfe");
			{
				helpMenu = new Menu(helpMenuItem);
				{
					contentsMenuItem = new MenuItem(helpMenu, SWT.PUSH);
					contentsMenuItem.setText("Inhalt");
					contentsMenuItem.setEnabled(false);
					contentsMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("contentsMenuItem.widgetSelected, event=" + evt);
							//TODO add your code for contentsMenuItem.widgetSelected
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
				addSubHistoryMenuItem(csvFileDialog.getFileName());

				CSVReaderWriter.read(deviceSetting.getListSeparator(), csvFilePath, dialogName, isRaw);
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

}
