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
import osde.io.CSVReaderWriter;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.dialog.DeviceSelectionDialog;

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class MenuBar {
	private Logger												log	= Logger.getLogger(this.getClass().getName());

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
	private MenuItem											debugLogMenuItem;
	private MenuItem											serialDiagnosticMenuItem;
	private MenuItem											graphicTabMenuItem, dataTableTabMenuItem, digitalTabMenuItem, analogTabMenuItem, recordSetCommentTabMenuItem, compareTabMenuItem;
	private MenuItem											recordCommentMenuItem;
	private MenuItem											kurveSelectionMenuItem;
	private Menu													windowMenu;
	private MenuItem											windowMenuItem;
	private MenuItem											editCommentsToolsMenuItem;
	private Menu													toolsMenu, graphicsMenu;
	private MenuItem											toolsMenuItem, graphicsMenuItem, saveDefaultGraphicsTemplateItem, saveGraphicsTemplateItem, restoreGraphicsTemplateItem;
	private MenuItem											csvExportMenuItem1, csvExportMenuItem2;
	private MenuItem											nextDeviceMenuItem;
	private MenuItem											prevDeviceMenuItem;
	private MenuItem											selectDeviceMenuItem;
	private Menu													deviceMenu;
	private MenuItem											deviceMenuItem;
	private MenuItem											copyTabelleBearbeitenMenuItem;
	private MenuItem											copyGrafikBearbeitenMenuItem;
	private Menu													bearbeitenMenu;
	private MenuItem											bearbeitenMenuItem;
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
							application.updateDataTable();
							application.updateDigitalWindow();
						}
					});
				}
				{
					openFileMenuItem = new MenuItem(fileMenu, SWT.PUSH);
					openFileMenuItem.setText("Öffnen");
					openFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("openFileMenuItem.widgetSelected, event=" + evt);
							//TODO add your code for openFileMenuItem.widgetSelected
						}
					});
				}
				{
					saveFileMenuItem = new MenuItem(fileMenu, SWT.PUSH);
					saveFileMenuItem.setText("Speichern");
					saveFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("saveFileMenuItem.widgetSelected, event=" + evt);
							//TODO add your code for saveFileMenuItem.widgetSelected
						}
					});
				}
				{
					saveAsFileMenuItem = new MenuItem(fileMenu, SWT.PUSH);
					saveAsFileMenuItem.setText("Speichern unter ...");
				}
				{
					historyFileMenuItem = new MenuItem(fileMenu, SWT.CASCADE);
					historyFileMenuItem.setText("Historie");
					historyFileMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("historyFileMenuItem.widgetSelected, event=" + evt);
							//TODO add your code for historyFileMenuItem.widgetSelected
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
									try {
										log.finest("csvImportMenuItem.widgetSelected, event=" + evt);
										Channel channel = channels.getActiveChannel();
										String fileSep = System.getProperty("file.separator");
										Settings deviceSetting = Settings.getInstance();
										String path = deviceSetting.getDataFilePath() + fileSep + application.getActiveDevice().getName();
										FileDialog csvFileDialog = application.openFileOpenDialog("Import CSV absolut", new String[] { "*.csv" }, path);
										if (csvFileDialog.getFileName().length() > 4) {
											String csvFilePath = csvFileDialog.getFilterPath() + fileSep + csvFileDialog.getFileName();
											addSubHistoryMenuItem(csvFileDialog.getFileName());
											String recordSetKey = "1) CSV Import";
											if (channel.getActiveRecordSet() != null) {
												recordSetKey = (channel.size() + 1) + ") CSV Import";
											}
											channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, application.getActiveDevice(), false, true));
											channel.applyTemplate(recordSetKey);
											CSVReaderWriter.read(deviceSetting.getListSeparator(), csvFilePath, channel.get(recordSetKey), false);
											channel.setActiveRecordSet(recordSetKey);
											channel.getActiveRecordSet().switchRecordSet(recordSetKey);
											//application.getGraphicsWindow().redrawGrahics();
											application.updateDataTable();
											application.updateDigitalWindowChilds();
										}
									}
									catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							});
						}
						{
							csvImportMenuItem2 = new MenuItem(importMenu, SWT.PUSH);
							csvImportMenuItem2.setText("CSV raw");
							csvImportMenuItem2.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									try {
										log.finest("csvImportMenuItem.widgetSelected, event=" + evt);
										String fileSep = System.getProperty("file.separator");
										Channel channel = channels.getActiveChannel();
										Settings deviceSetting = Settings.getInstance();
										String path = deviceSetting.getDataFilePath() + fileSep + application.getActiveDevice().getName();
										FileDialog csvFileDialog = application.openFileOpenDialog("Import CSV raw", new String[] { "*.csv" }, path);
										if (csvFileDialog.getFileName().length() > 4) {
											String csvFilePath = csvFileDialog.getFilterPath() + fileSep + csvFileDialog.getFileName();
											addSubHistoryMenuItem(csvFileDialog.getFileName());
											String recordSetKey = "1) CSV Import";
											if (channel.getActiveRecordSet() != null) {
												recordSetKey = (channel.size() + 1) + ") CSV Import";
											}
											channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, application.getActiveDevice(), true, true));
											channel.applyTemplate(recordSetKey);
											CSVReaderWriter.read(deviceSetting.getListSeparator(), csvFilePath, channel.get(recordSetKey), true);
											channels.getActiveChannel().setActiveRecordSet(recordSetKey);
											channel.getActiveRecordSet().switchRecordSet(recordSetKey);
											//application.getGraphicsWindow().redrawGrahics();
											channel.get(recordSetKey).checkAllDisplayable(); // raw import needs calculation of passive records
											application.updateDataTable();
											application.updateDigitalWindowChilds();
										}
									}
									catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
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
									String fileSep = System.getProperty("file.separator");
									Settings deviceSetting = Settings.getInstance();
									String path = deviceSetting.getDataFilePath() + fileSep + application.getActiveDevice().getName();
									FileDialog csvFileDialog = application.openFileSaveDialog("Export CSV absolut", new String[] { "*.csv" }, path);
									if (csvFileDialog.getFileName().length() > 4) {
										Channel activeChannel = channels.getActiveChannel();
										String recordSetKey = activeChannel.getActiveRecordSet().getName();
										String csvFilePath = csvFileDialog.getFilterPath() + System.getProperty("file.separator") + csvFileDialog.getFileName();
										addSubHistoryMenuItem(csvFileDialog.getFileName());
										CSVReaderWriter.write(deviceSetting.getListSeparator(), recordSetKey, csvFilePath, false);
									}
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
								String fileSep = System.getProperty("file.separator");
								Settings deviceSetting = Settings.getInstance();
								String path = deviceSetting.getDataFilePath() + fileSep + application.getActiveDevice().getName();
								FileDialog csvFileDialog = application.openFileSaveDialog("Export CSV raw", new String[] { "*.csv" }, path);
								if (csvFileDialog.getFileName().length() > 4) {
									Channel activeChannel = channels.getActiveChannel();
									String recordSetKey = activeChannel.getActiveRecordSet().getName();
									String csvFilePath = csvFileDialog.getFilterPath() + fileSep + csvFileDialog.getFileName();
									addSubHistoryMenuItem(csvFileDialog.getFileName());
									CSVReaderWriter.write(deviceSetting.getListSeparator(), recordSetKey, csvFilePath, true);
								}
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
			bearbeitenMenuItem = new MenuItem(parent, SWT.CASCADE);
			bearbeitenMenuItem.setText("Bearbeiten");
			{
				bearbeitenMenu = new Menu(bearbeitenMenuItem);
				bearbeitenMenuItem.setMenu(bearbeitenMenu);
				{
					copyGrafikBearbeitenMenuItem = new MenuItem(bearbeitenMenu, SWT.PUSH);
					copyGrafikBearbeitenMenuItem.setText("Kopiere Grafikfenster");
					copyGrafikBearbeitenMenuItem.setEnabled(false);
				}
				{
					copyTabelleBearbeitenMenuItem = new MenuItem(bearbeitenMenu, SWT.PUSH);
					copyTabelleBearbeitenMenuItem.setText("Kopiere Tabelle");
					copyTabelleBearbeitenMenuItem.setEnabled(false);
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
					selectDeviceMenuItem = new MenuItem(deviceMenu, SWT.PUSH);
					selectDeviceMenuItem.setText("Gerät auswählen");
					selectDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("selectDeviceMenuItem.widgetSelected, event=" + evt);
							application.setActiveDevice(application.getDeviceSelectionDialog().open());
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
					nextDeviceMenuItem = new MenuItem(deviceMenu, SWT.PUSH);
					nextDeviceMenuItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowRightGreen.gif"));
					nextDeviceMenuItem.setText("nächstes Gerät");
					nextDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("nextDeviceMenuItem.widgetSelected, event=" + evt);
							if (application.getActiveDevice().getSerialPort() == null || !application.getActiveDevice().getSerialPort().isConnected()) { // allow device switch only if port noct connected
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
					toolBoxDeviceMenuItem = new MenuItem(deviceMenu, SWT.PUSH);
					toolBoxDeviceMenuItem.setText("Geräte ToolBox");
					toolBoxDeviceMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("toolBoxDeviceMenuItem.widgetSelected, event=" + evt);
							application.setActiveDevice(application.getDeviceSelectionDialog().open());
						}
					});
				}
			}
		}
		{
			toolsMenuItem = new MenuItem(parent, SWT.CASCADE);
			toolsMenuItem.setText("Tools");
			toolsMenuItem.setEnabled(false);
			{
				toolsMenu = new Menu(toolsMenuItem);
				toolsMenuItem.setMenu(toolsMenu);
				{
					editCommentsToolsMenuItem = new MenuItem(toolsMenu, SWT.PUSH);
					editCommentsToolsMenuItem.setText("Kommentartext");
					editCommentsToolsMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("editCommentsToolsMenuItem.widgetSelected, event=" + evt);
							//TODO add your code for editCommentsToolsMenuItem.widgetSelected
						}
					});
				}
			}
		}
		{
			graphicsMenuItem = new MenuItem(parent, SWT.CASCADE);
			graphicsMenuItem.setText("Graphik");
			{
				graphicsMenu = new Menu(graphicsMenuItem);
				graphicsMenuItem.setMenu(graphicsMenu);
				{
					saveDefaultGraphicsTemplateItem = new MenuItem(graphicsMenu, SWT.PUSH);
					saveDefaultGraphicsTemplateItem.setText("Grafikvorlage sichern");
					saveDefaultGraphicsTemplateItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("saveGraphicsTemplateItem.widgetSelected, event=" + evt);
							channels.getActiveChannel().saveTemplate();
						}
					});
				}
				{
					saveGraphicsTemplateItem = new MenuItem(graphicsMenu, SWT.PUSH);
					saveGraphicsTemplateItem.setText("Grafikvorlage sichern unter..");
					saveGraphicsTemplateItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("saveGraphicsTemplateItem.widgetSelected, event=" + evt);
							log.fine("templatePath = " + Settings.getInstance().getGraphicsTemplatePath());
							FileDialog fileDialog = application.openFileSaveDialog("Sichere GraphicsTemplate", new String[] { Settings.GRAPHICS_TEMPLATE_EXTENSION }, Settings.getInstance()
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
					restoreGraphicsTemplateItem.setText("Grafikvorlage laden");
					restoreGraphicsTemplateItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("restoreGraphicsTemplateItem.widgetSelected, event=" + evt);
							log.finest("saveGraphicsTemplateItem.widgetSelected, event=" + evt);
							FileDialog fileDialog = application.openFileOpenDialog("Lade GraphicsTemplate", new String[] { Settings.GRAPHICS_TEMPLATE_EXTENSION }, Settings.getInstance().getGraphicsTemplatePath());
							Channel activeChannel = channels.getActiveChannel();
							GraphicsTemplate template = activeChannel.getTemplate();
							template.setNewFileName(fileDialog.getFileName());
							log.fine("templateFilePath = " + fileDialog.getFileName());
							template.load();
							if (activeChannel.getActiveRecordSet() != null) {
							channels.getActiveChannel().applyTemplate(activeChannel.getActiveRecordSet().getName());
							application.updateGraphicsWindow();
							}
						}
					});
				}
			}
		}
		{
			windowMenuItem = new MenuItem(parent, SWT.CASCADE);
			windowMenuItem.setText("Ansicht");
			{
				windowMenu = new Menu(windowMenuItem);
				windowMenuItem.setMenu(windowMenu);
				{
					graphicTabMenuItem = new MenuItem(windowMenu, SWT.PUSH);
					graphicTabMenuItem.setText("Grafikansicht");
					graphicTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("graphicTabMenuItem.widgetSelected, event=" + evt);
							application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_GRAPHIC);
						}
					});
				}
				{
					dataTableTabMenuItem = new MenuItem(windowMenu, SWT.PUSH);
					dataTableTabMenuItem.setText("Tabellenansicht");
					dataTableTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("dataTableTabMenuItem.widgetSelected, event=" + evt);
							application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_DATA_TABLE);
						}
					});
				}
				{
					analogTabMenuItem = new MenuItem(windowMenu, SWT.PUSH);
					analogTabMenuItem.setText("Analoganzeige");
					analogTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("analogTabMenuItem.widgetSelected, event=" + evt);
							application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_ANALOG);
						}
					});
				}
				{
					digitalTabMenuItem = new MenuItem(windowMenu, SWT.PUSH);
					digitalTabMenuItem.setText("Zahlenanzeige");
					digitalTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("digitalTabMenuItem.widgetSelected, event=" + evt);
							application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_DIGITAL);
						}
					});
				}
				{
					recordSetCommentTabMenuItem = new MenuItem(windowMenu, SWT.PUSH);
					recordSetCommentTabMenuItem.setText("Datensatzkommentar");
					recordSetCommentTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("setCommentTabMenuItem.widgetSelected, event=" + evt);
							application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_COMMENT);
						}
					});
				}
				{
					compareTabMenuItem = new MenuItem(windowMenu, SWT.PUSH);
					compareTabMenuItem.setText("Kurvenvergleich");
					compareTabMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("compareTabMenuItem.widgetSelected, event=" + evt);
							application.switchDisplayTab(OpenSerialDataExplorer.TAB_INDEX_COMPARE);
						}
					});
				}
				{
					new MenuItem(windowMenu, SWT.SEPARATOR);
				}
				{
					kurveSelectionMenuItem = new MenuItem(windowMenu, SWT.CHECK);
					kurveSelectionMenuItem.setText("Kurvenauswahl");
					kurveSelectionMenuItem.setSelection(true);
					kurveSelectionMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("kurveSelectionMenuItem.widgetSelected, event=" + evt);
							if (kurveSelectionMenuItem.getSelection()) {
								application.setCurveSelectorEnabled(true);
							}
							else {
								application.setCurveSelectorEnabled(false);
							}
						}
					});
				}
				{
					recordCommentMenuItem = new MenuItem(windowMenu, SWT.CHECK);
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
					new MenuItem(windowMenu, SWT.SEPARATOR);
				}
				{
					serialDiagnosticMenuItem = new MenuItem(windowMenu, SWT.PUSH);
					serialDiagnosticMenuItem.setText("Schnittstellendiagnose");
					serialDiagnosticMenuItem.setEnabled(false);
					serialDiagnosticMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("serialDiagnosticMenuItem.widgetSelected, event=" + evt);
							//TODO add your code for serialDiagnosticMenuItem.widgetSelected
						}
					});
				}
				{
					debugLogMenuItem = new MenuItem(windowMenu, SWT.PUSH);
					debugLogMenuItem.setText("Diagnoselog");
					debugLogMenuItem.setEnabled(false);
					debugLogMenuItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("debugLogMenuItem.widgetSelected, event=" + evt);
							//TODO add your code for debugLogMenuItem.widgetSelected
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

}
