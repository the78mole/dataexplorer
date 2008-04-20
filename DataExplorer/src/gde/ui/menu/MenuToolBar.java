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

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.DeviceDialog;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.dialog.DeviceSelectionDialog;
import osde.ui.tab.GraphicsWindow;

/**
 * Graphical menu tool bar class
 * (future items are: scaling icons, ...)
 * @author Winfried Brügmann
 */
public class MenuToolBar {
	final static Logger						log	= Logger.getLogger(MenuToolBar.class.getName());

	Point													size;
	Composite											recordSelectComposite;
	Composite											channelSelectComposite;
	CoolItem											dataCoolItem;
	ToolBar												portToolBar;
	CoolItem											portCoolItem;
	ToolBar												deviceToolBar;
	CoolItem											deviceCoolItem;

	CoolBar												coolBar;
	CoolItem											menuCoolItem;
	ToolItem											toolBoxToolItem;
	ToolItem											nextDeviceToolItem;
	ToolItem											prevDeviceToolItem;
	ToolItem											deviceSelectToolItem;
	ToolItem											saveAsToolItem;
	ToolItem											settingsToolItem;
	ToolItem											saveToolItem;
	ToolItem											openToolItem;
	ToolItem											newToolItem;
	ToolBar												fileToolBar;
	ToolItem											fitIntoItem;
	ToolItem											panItem;
	ToolItem											zoomWindowItem;
	ToolBar												zoomToolBar;
	CoolItem											zoomCoolItem;

	ToolItem											portOpenCloseItem;
	Composite											dataBarComposite;
	ToolItem											nextChannel, prevChannel, prevRecord, nextRecord, deleteRecord, editRecord;
	CCombo												channelSelectCombo, recordSelectCombo;
	ToolBar												channelToolBar, recordToolBar;
	ToolItem											separator;

	final OpenSerialDataExplorer	application;
	final Channels								channels;

	public MenuToolBar(OpenSerialDataExplorer parent, CoolBar menuCoolBar) {
		this.application = parent;
		this.coolBar = menuCoolBar;
		this.channels = Channels.getInstance();
	}

	public void init() {
		this.coolBar = new CoolBar(this.application, SWT.NONE);
		SWTResourceManager.registerResourceUser(this.coolBar);
		this.coolBar.setSize(800, 100);
		create();
	}

	public void create() {
		{ // begin file cool item
			this.menuCoolItem = new CoolItem(this.coolBar, SWT.LEFT | SWT.SHADOW_OUT);
			{ // begin file tool bar
				this.fileToolBar = new ToolBar(this.coolBar, SWT.NONE);
				this.menuCoolItem.setControl(this.fileToolBar);
				//fileToolBar.setSize(116, 29);
				{
					this.newToolItem = new ToolItem(this.fileToolBar, SWT.PUSH);
					this.newToolItem.setToolTipText("Löscht die aktuellen Aufzeichnungen und legt einen leeren Datensatz an");
					this.newToolItem.setImage(SWTResourceManager.getImage("osde/resource/New.gif"));
					this.newToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/NewHot.gif"));
					this.newToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuToolBar.log.finest("newToolItem.widgetSelected, event=" + evt);
							MenuToolBar.this.application.getDeviceSelectionDialog().setupDataChannels(MenuToolBar.this.application.getActiveDevice());
						}
					});
				}
				{
					this.openToolItem = new ToolItem(this.fileToolBar, SWT.NONE);
					this.openToolItem.setToolTipText("Verwirft den aktuellen Datensatz und  öffnet eine neue Datei mit neuem Inhalt");
					this.openToolItem.setImage(SWTResourceManager.getImage("osde/resource/Open.gif"));
					this.openToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/OpenHot.gif"));
					this.openToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuToolBar.log.finest("openToolItem.widgetSelected, event=" + evt);
							//TODO check if other data unsaved 
							MenuToolBar.this.application.getMenuBar().openFile("Öffne Datei ...");
						}
					});
				}
				{
					this.saveToolItem = new ToolItem(this.fileToolBar, SWT.NONE);
					this.saveToolItem.setToolTipText("Sichert die aktuellen Aufzeichnungen in eine Datei");
					this.saveToolItem.setImage(SWTResourceManager.getImage("osde/resource/Save.gif"));
					this.saveToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/SaveHot.gif"));
					this.saveToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuToolBar.log.finest("saveToolItem.widgetSelected, event=" + evt);
							Channel activeChannel = MenuToolBar.this.channels.getActiveChannel();
							if (activeChannel != null) {
								if (MenuToolBar.this.channels.isSaved())
									MenuToolBar.this.application.getMenuBar().saveOsdFile("OSD Datei - Speichern unter ...", "");
								else
									MenuToolBar.this.application.getMenuBar().saveOsdFile("OSD Datei - Speichern", MenuToolBar.this.channels.getFullQualifiedFileName());
							}
						}
					});
				}
				{
					this.saveAsToolItem = new ToolItem(this.fileToolBar, SWT.NONE);
					this.saveAsToolItem.setToolTipText("Sichert die aktuellen Aufzeichnungen unter einem anzugebenden Namen");
					this.saveAsToolItem.setImage(SWTResourceManager.getImage("osde/resource/SaveAs.gif"));
					this.saveAsToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/SaveAsHot.gif"));
					this.saveAsToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuToolBar.log.finest("saveAsToolItem.widgetSelected, event=" + evt);
							MenuToolBar.this.application.getMenuBar().saveOsdFile("OSD Datei - Speichern unter ...", "");
						}
					});
				}
				{
					this.settingsToolItem = new ToolItem(this.fileToolBar, SWT.NONE);
					this.settingsToolItem.setToolTipText("Sichert die aktuellen Aufzeichnungen unter einem anzugebenden Namen");
					this.settingsToolItem.setImage(SWTResourceManager.getImage("osde/resource/Settings.gif"));
					this.settingsToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/SettingsHot.gif"));
					this.settingsToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuToolBar.log.finest("saveAsToolItem.widgetSelected, event=" + evt);
							// check if other none modal dialog is open
							DeviceDialog deviceDialog = MenuToolBar.this.application.getDeviceDialog();
							if (deviceDialog == null || deviceDialog.isDisposed()) {
								MenuToolBar.this.application.openSettingsDialog();
								MenuToolBar.this.application.setStatusMessage("");
							}
							else
								MenuToolBar.this.application.setStatusMessage("Ein Gerätedialog geöffnet, ein Öffnen des Einstellungsdialoges ist zur Zeit nicht möglich !", SWT.COLOR_RED);
						}
					});
				}
			} // end file tool bar
			this.fileToolBar.pack();
			this.size = this.fileToolBar.getSize();
			this.menuCoolItem.setSize(this.size);
			this.menuCoolItem.setPreferredSize(this.size);
			this.menuCoolItem.setMinimumSize(this.size);
			MenuToolBar.log.fine("fileToolBar.size = " + this.size);
		} // end file cool item

		{ // begin device cool item
			this.deviceCoolItem = new CoolItem(this.coolBar, SWT.LEFT | SWT.SHADOW_OUT);
			{ // begin device tool bar
				this.deviceToolBar = new ToolBar(this.coolBar, SWT.NONE);
				this.deviceCoolItem.setControl(this.deviceToolBar);
				//deviceToolBar.setSize(120, 29);
				{
					this.deviceSelectToolItem = new ToolItem(this.deviceToolBar, SWT.NONE);
					this.deviceSelectToolItem.setToolTipText("Geräteauswahl mit Einstellungen");
					this.deviceSelectToolItem.setImage(SWTResourceManager.getImage("osde/resource/DeviceSelection.gif"));
					this.deviceSelectToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/DeviceSelectionHot.gif"));
					this.deviceSelectToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuToolBar.log.finest("deviceToolItem.widgetSelected, event=" + evt);
							DeviceSelectionDialog deviceSelection = MenuToolBar.this.application.getDeviceSelectionDialog();
							if (deviceSelection.checkDataSaved()) {
								deviceSelection.open();
							}
						}
					});
				}
				{
					this.prevDeviceToolItem = new ToolItem(this.deviceToolBar, SWT.NONE);
					this.prevDeviceToolItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLeft.gif"));
					this.prevDeviceToolItem.setToolTipText("Schalte zum verhergehenden Gerät");
					this.prevDeviceToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLefHot.gif"));
					this.prevDeviceToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuToolBar.log.finest("prevDeviceToolItem.widgetSelected, event=" + evt);
							// allow device switch only if port not connected
							if (MenuToolBar.this.application.getActiveDevice() == null || MenuToolBar.this.application.getActiveDevice().getSerialPort() != null
									&& !MenuToolBar.this.application.getActiveDevice().getSerialPort().isConnected()) {
								DeviceConfiguration deviceConfig;
								DeviceSelectionDialog deviceSelect = MenuToolBar.this.application.getDeviceSelectionDialog();
								if (deviceSelect.checkDataSaved()) {
									int selection = deviceSelect.getActiveDevices().indexOf(deviceSelect.getActiveConfig().getName());
									int tmpSize = deviceSelect.getActiveDevices().size();
									if (selection > 0 && selection <= tmpSize) {
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(selection - 1));
									}
									else
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(tmpSize - 1));

									// if a device tool box is open, dispose it
									if (MenuToolBar.this.application.getDeviceDialog() != null && !MenuToolBar.this.application.getDeviceDialog().isDisposed()) {
										MenuToolBar.this.application.getDeviceDialog().dispose();
									}

									deviceSelect.setActiveConfig(deviceConfig);
									deviceSelect.setupDevice();
								}
							}
							else {
								MenuToolBar.this.application.openMessageDialog("Das Gerät kann nicht gewechselt werden, solange der serielle Port geöffnet ist!");
							}
						}
					});
				}
				{
					this.nextDeviceToolItem = new ToolItem(this.deviceToolBar, SWT.NONE);
					this.nextDeviceToolItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRight.gif"));
					this.nextDeviceToolItem.setToolTipText("Schalte zum nachfolgenden Gerät");
					this.nextDeviceToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRightHot.gif"));
					this.nextDeviceToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuToolBar.log.finest("nextDeviceToolItem.widgetSelected, event=" + evt);
							// allow device switch only if port not connected
							if (MenuToolBar.this.application.getActiveDevice() == null || MenuToolBar.this.application.getActiveDevice().getSerialPort() != null
									&& !MenuToolBar.this.application.getActiveDevice().getSerialPort().isConnected()) {
								DeviceConfiguration deviceConfig;
								DeviceSelectionDialog deviceSelect = MenuToolBar.this.application.getDeviceSelectionDialog();
								if (deviceSelect.checkDataSaved()) {
									int selection = deviceSelect.getActiveDevices().indexOf(deviceSelect.getActiveConfig().getName());
									int tmpSize = deviceSelect.getActiveDevices().size() - 1;
									if (selection >= 0 && selection < tmpSize)
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(selection + 1));
									else
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(0));

									// if a device tool box is open, dispose it
									if (MenuToolBar.this.application.getDeviceDialog() != null && !MenuToolBar.this.application.getDeviceDialog().isDisposed()) {
										MenuToolBar.this.application.getDeviceDialog().dispose();
									}

									deviceSelect.setActiveConfig(deviceConfig);
									deviceSelect.setupDevice();
								}
							}
							else {
								MenuToolBar.this.application.openMessageDialog("Das Gerät kann nicht gewechselt werden, solange der serielle Port geöffnet ist!");
							}
						}
					});
				}
				{
					this.toolBoxToolItem = new ToolItem(this.deviceToolBar, SWT.NONE);
					this.toolBoxToolItem.setImage(SWTResourceManager.getImage("osde/resource/ToolBox.gif"));
					this.toolBoxToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif"));
					this.toolBoxToolItem.setToolTipText("Gerätedialog öffnen");
					this.toolBoxToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuToolBar.log.finest("toolBoxToolItem.widgetSelected, event=" + evt);
							if (MenuToolBar.this.application.getDeviceDialog() != null) {
								MenuToolBar.this.application.getDeviceDialog().open();
							}
							else {
								MenuToolBar.this.application.getDeviceSelectionDialog().open();
							}
						}
					});
				}
			} // end device tool bar
			this.deviceToolBar.pack();
			this.size = this.deviceToolBar.getSize();
			this.deviceCoolItem.setSize(this.size);
			this.deviceCoolItem.setPreferredSize(this.size);
			this.deviceCoolItem.setMinimumSize(this.size);
		} // end device cool item
		{ // begin zoom cool item
			this.zoomCoolItem = new CoolItem(this.coolBar, SWT.LEFT | SWT.SHADOW_OUT);
			{ // begin zoom tool bar
				this.zoomToolBar = new ToolBar(this.coolBar, SWT.NONE);
				this.zoomCoolItem.setControl(this.zoomToolBar);
				//zoomToolBar.setSize(90, 29);
				{
					this.zoomWindowItem = new ToolItem(this.zoomToolBar, SWT.NONE);
					this.zoomWindowItem.setImage(SWTResourceManager.getImage("osde/resource/Zoom.gif"));
					this.zoomWindowItem.setHotImage(SWTResourceManager.getImage("osde/resource/ZoomHot.gif"));
					this.zoomWindowItem.setToolTipText("Ausschnitt vergrößern");
					this.zoomWindowItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							if (MenuToolBar.log.isLoggable(Level.FINEST)) MenuToolBar.log.finest("zoomWindowItem.widgetSelected, event=" + evt);
							MenuToolBar.this.application.setGraphicsMode(GraphicsWindow.MODE_ZOOM, true);
						}
					});
				}
				{
					this.panItem = new ToolItem(this.zoomToolBar, SWT.NONE);
					this.panItem.setImage(SWTResourceManager.getImage("osde/resource/Pan.gif"));
					this.panItem.setHotImage(SWTResourceManager.getImage("osde/resource/PanHot.gif"));
					this.panItem.setToolTipText("Verschieben");
					this.panItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							if (MenuToolBar.log.isLoggable(Level.FINEST)) MenuToolBar.log.finest("resizeItem.widgetSelected, event=" + evt);
							MenuToolBar.this.application.setGraphicsMode(GraphicsWindow.MODE_PAN, true);
						}
					});
				}
				{
					this.fitIntoItem = new ToolItem(this.zoomToolBar, SWT.NONE);
					this.fitIntoItem.setImage(SWTResourceManager.getImage("osde/resource/Expand.gif"));
					this.fitIntoItem.setHotImage(SWTResourceManager.getImage("osde/resource/ExpandHot.gif"));
					this.fitIntoItem.setToolTipText("Auf Ursprungsgröße einpassen");
					this.fitIntoItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							if (MenuToolBar.log.isLoggable(Level.FINEST)) MenuToolBar.log.finest("fitIntoItem.widgetSelected, event=" + evt);
							MenuToolBar.this.application.setGraphicsMode(GraphicsWindow.MODE_RESET, false);
						}
					});
				}
			} // end zoom tool bar
			this.zoomToolBar.pack();
			this.size = this.zoomToolBar.getSize();
			this.zoomCoolItem.setSize(this.size);
			this.zoomCoolItem.setPreferredSize(this.size);
			this.zoomCoolItem.setMinimumSize(this.size);
		} // end zoom cool item

		{ // begin port cool item
			this.portCoolItem = new CoolItem(this.coolBar, SWT.LEFT | SWT.SHADOW_OUT);
			{
				this.portToolBar = new ToolBar(this.coolBar, SWT.NONE);
				this.portCoolItem.setControl(this.portToolBar);
				//portToolBar.setSize(129, 29);
				{
					this.portOpenCloseItem = new ToolItem(this.portToolBar, SWT.NONE);
					this.portOpenCloseItem.setToolTipText("Seriellen Port Öffnen, um eine Datenaufnahme zu ermöglichen");
					this.portOpenCloseItem.setImage(SWTResourceManager.getImage("osde/resource/PortOpen.gif"));
					this.portOpenCloseItem.setDisabledImage(SWTResourceManager.getImage("osde/resource/PortOpenDisabled.gif"));
					this.portOpenCloseItem.setHotImage(SWTResourceManager.getImage("osde/resource/PortOpenHot.gif"));
					this.portOpenCloseItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							MenuToolBar.log.finest("portOpenCloseItem.widgetSelected, event=" + evt);
							MenuToolBar.this.application.openCloseSerialPort();
						}
					});
				}
			}
			this.portToolBar.pack();
			this.size = this.portToolBar.getSize();
			this.portCoolItem.setSize(this.size);
			this.portCoolItem.setPreferredSize(this.size);
			this.portCoolItem.setMinimumSize(this.size);
		} // end port cool item

		{ // begin data cool item (channel select, record select)
			this.dataCoolItem = new CoolItem(this.coolBar, SWT.LEFT | SWT.SHADOW_OUT);
			{
				this.dataBarComposite = new Composite(this.coolBar, SWT.NONE);
				RowLayout composite1Layout1 = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.dataBarComposite.setLayout(composite1Layout1);
				this.dataCoolItem.setControl(this.dataBarComposite);
				{
					this.channelSelectComposite = new Composite(this.dataBarComposite, SWT.NONE);
					RowData composite1LData = new RowData();
					composite1LData.width = 150;
					composite1LData.height = 24;
					RowLayout composite1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					this.channelSelectComposite.setLayout(composite1Layout);
					this.channelSelectComposite.setLayoutData(composite1LData);
					{
						this.channelSelectCombo = new CCombo(this.channelSelectComposite, SWT.BORDER | SWT.LEFT);
						this.channelSelectCombo.setItems(new String[] { " 1 : Ausgang" }); // " 2 : Ausgang", " 3 : Ausgang", "" 4 : Ausgang"" });
						this.channelSelectCombo.select(0);
						this.channelSelectCombo.setToolTipText("Wählen Sie einen Ausgang/eine Konfiguration aus der angezeigt werden soll");
						RowData channelSelectComboLData = new RowData();
						channelSelectComboLData.width = 140;
						channelSelectComboLData.height = 18;
						this.channelSelectCombo.setLayoutData(channelSelectComboLData);
						this.channelSelectCombo.setEditable(false);
						this.channelSelectCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
						this.channelSelectCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								MenuToolBar.log.finest("kanalCombo.widgetSelected, event=" + evt);
								MenuToolBar.this.channels.switchChannel(MenuToolBar.this.channelSelectCombo.getText());
							}
						});
					}
					{
						this.channelToolBar = new ToolBar(this.dataBarComposite, SWT.FLAT);
						{
							this.prevChannel = new ToolItem(this.channelToolBar, SWT.NONE);
							this.prevChannel.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLeft.gif"));
							this.prevChannel.setToolTipText("Ausgang/Konfiguration zurück");
							this.prevChannel.setEnabled(false);
							this.prevChannel.setHotImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLefHot.gif"));
							this.prevChannel.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									MenuToolBar.log.finest("prevChannel.widgetSelected, event=" + evt);
									int selectionIndex = MenuToolBar.this.channelSelectCombo.getSelectionIndex();
									if (selectionIndex > 0) MenuToolBar.this.channelSelectCombo.select(selectionIndex - 1);
									if (selectionIndex == 1) MenuToolBar.this.prevChannel.setEnabled(false);
									selectionIndex = MenuToolBar.this.channelSelectCombo.getSelectionIndex();
									MenuToolBar.this.nextChannel.setEnabled(true);
									MenuToolBar.this.channels.switchChannel(MenuToolBar.this.channelSelectCombo.getText());
								}
							});
						}
						{
							this.nextChannel = new ToolItem(this.channelToolBar, SWT.NONE);
							this.nextChannel.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRight.gif"));
							this.nextChannel.setToolTipText("Ausgang/Konfiguration vor");
							this.nextChannel.setEnabled(false);
							this.nextChannel.setHotImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRightHot.gif"));
							this.nextChannel.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									MenuToolBar.log.finest("nextChannel.widgetSelected, event=" + evt);
									int selectionIndex = MenuToolBar.this.channelSelectCombo.getSelectionIndex();
									int maxIndex = MenuToolBar.this.channelSelectCombo.getItemCount() - 1;
									if (maxIndex <= 0) {
										MenuToolBar.this.nextChannel.setEnabled(false);
										MenuToolBar.this.prevChannel.setEnabled(false);
									}
									else {
										if (selectionIndex < maxIndex) MenuToolBar.this.channelSelectCombo.select(selectionIndex + 1);
										if (selectionIndex == maxIndex - 1) MenuToolBar.this.nextChannel.setEnabled(false);
										MenuToolBar.this.prevChannel.setEnabled(true);
									}
									MenuToolBar.this.channels.switchChannel(MenuToolBar.this.channelSelectCombo.getText());
								}
							});
						}
					}
					this.channelToolBar.pack();
					{
						RowData composite2LData = new RowData();
						composite2LData.width = 250;
						composite2LData.height = 24;
						this.recordSelectComposite = new Composite(this.dataBarComposite, SWT.NONE);
						RowLayout composite2Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.recordSelectComposite.setLayout(composite2Layout);
						this.recordSelectComposite.setLayoutData(composite2LData);
						{
							this.recordSelectCombo = new CCombo(this.recordSelectComposite, SWT.BORDER);
							FormLayout aufnahmeComboLayout = new FormLayout();
							this.recordSelectCombo.setLayout(aufnahmeComboLayout);
							this.recordSelectCombo.setItems(new String[] { " " }); // "2) Flugaufzeichnung", "3) laden" });
							this.recordSelectCombo.setToolTipText("Wählen Sie einen Datensatz aus, der angezeigt werden soll");
							this.recordSelectCombo.setTextLimit(30);
							RowData recordSelectComboLData = new RowData();
							recordSelectComboLData.width = 240;
							recordSelectComboLData.height = 18;
							this.recordSelectCombo.setLayoutData(recordSelectComboLData);
							this.recordSelectCombo.setEditable(false);
							this.recordSelectCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
							this.recordSelectCombo.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									MenuToolBar.log.finest("recordSelectCombo.widgetSelected, event=" + evt);
									Channel activeChannel = MenuToolBar.this.channels.getActiveChannel();
									if (activeChannel != null) activeChannel.switchRecordSet(MenuToolBar.this.recordSelectCombo.getText());
								}
							});
							this.recordSelectCombo.addKeyListener(new KeyAdapter() {
								public void keyPressed(KeyEvent evt) {
									MenuToolBar.log.finest("recordSelectCombo.keyPressed, event=" + evt);
									if (evt.character == SWT.CR) {
										Channel activeChannel = MenuToolBar.this.channels.getActiveChannel();
										if (activeChannel != null) {
											String oldRecordSetName = activeChannel.getActiveRecordSet().getName();
											String newRecordSetName = MenuToolBar.this.recordSelectCombo.getText();
											MenuToolBar.log.fine("newRecordSetName = " + newRecordSetName);
											String[] recordSetNames = MenuToolBar.this.recordSelectCombo.getItems();
											for (int i = 0; i < recordSetNames.length; i++) {
												if (recordSetNames[i].equals(oldRecordSetName)) recordSetNames[i] = newRecordSetName;
											}
											MenuToolBar.this.recordSelectCombo.setEditable(false);
											MenuToolBar.this.recordSelectCombo.setItems(recordSetNames);
											RecordSet recordSet = MenuToolBar.this.channels.getActiveChannel().get(oldRecordSetName);
											recordSet.setName(newRecordSetName);
											activeChannel.put(newRecordSetName, recordSet);
											activeChannel.remove(oldRecordSetName);
											activeChannel.getRecordSetNames();
											MenuToolBar.this.channels.getActiveChannel().switchRecordSet(newRecordSetName);
										}
									}
								}
							});
						}
						this.recordSelectComposite.pack();
					}
					{
						this.recordToolBar = new ToolBar(this.dataBarComposite, SWT.FLAT);
						{
							this.prevRecord = new ToolItem(this.recordToolBar, SWT.NONE);
							this.prevRecord.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLeft.gif"));
							this.prevRecord.setToolTipText("vorhergehender Datensatz");
							this.prevRecord.setEnabled(false);
							this.prevRecord.setHotImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLefHot.gif"));
							this.prevRecord.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									MenuToolBar.log.finest("prevRecord.widgetSelected, event=" + evt);
									int selectionIndex = MenuToolBar.this.recordSelectCombo.getSelectionIndex();
									if (selectionIndex > 0) MenuToolBar.this.recordSelectCombo.select(selectionIndex - 1);
									if (selectionIndex == 1) MenuToolBar.this.prevRecord.setEnabled(false);
									MenuToolBar.this.nextRecord.setEnabled(true);
									MenuToolBar.this.channels.getActiveChannel().switchRecordSet(MenuToolBar.this.recordSelectCombo.getText());
								}
							});
						}
						{
							this.nextRecord = new ToolItem(this.recordToolBar, SWT.NONE);
							this.nextRecord.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRight.gif"));
							this.nextRecord.setToolTipText("nächster Datensatz");
							this.nextRecord.setEnabled(false);
							this.nextRecord.setHotImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRightHot.gif"));
							this.nextRecord.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									MenuToolBar.log.finest("nextRecord.widgetSelected, event=" + evt);
									int selectionIndex = MenuToolBar.this.recordSelectCombo.getSelectionIndex();
									int maxIndex = MenuToolBar.this.recordSelectCombo.getItemCount() - 1;
									if (maxIndex <= 0) {
										MenuToolBar.this.nextRecord.setEnabled(false);
										MenuToolBar.this.prevRecord.setEnabled(false);
									}
									else {
										if (selectionIndex < maxIndex) MenuToolBar.this.recordSelectCombo.select(selectionIndex + 1);
										if (selectionIndex == maxIndex - 1) MenuToolBar.this.nextRecord.setEnabled(false);
										MenuToolBar.this.prevRecord.setEnabled(true);
									}
									MenuToolBar.this.channels.getActiveChannel().switchRecordSet(MenuToolBar.this.recordSelectCombo.getText());
								}
							});
						}
						this.recordToolBar.pack();
					}
					{
						this.separator = new ToolItem(this.recordToolBar, SWT.SEPARATOR);
					}
					{
						this.deleteRecord = new ToolItem(this.recordToolBar, SWT.NONE);
						this.deleteRecord.setImage(SWTResourceManager.getImage("osde/resource/DeleteHot.gif"));
						this.deleteRecord.setToolTipText("löscht den aktiven Datensatz");
						this.deleteRecord.setHotImage(SWTResourceManager.getImage("osde/resource/DeleteHot.gif"));
						this.deleteRecord.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								MenuToolBar.log.finest("deleteAufnahme.widgetSelected, event=" + evt);
								Channel activeChannel = MenuToolBar.this.channels.getActiveChannel();
								if (activeChannel != null) {
									RecordSet recordSet = activeChannel.getActiveRecordSet();
									if (recordSet != null) {
										String deleteRecordSetName = recordSet.getName();
										// before deletion set new active record set
										String newRecorKey = null;
										int selectionIndex = MenuToolBar.this.recordSelectCombo.getSelectionIndex();
										if ((selectionIndex - 1) > 0)
											newRecorKey = MenuToolBar.this.recordSelectCombo.getItem(selectionIndex - 1);
										else if ((selectionIndex - 1) == 0 && MenuToolBar.this.recordSelectCombo.getItemCount() > 2)
											newRecorKey = MenuToolBar.this.recordSelectCombo.getItem(selectionIndex + 1);
										if (newRecorKey != null) activeChannel.setActiveRecordSet(newRecorKey);
										// ready for deletion
										activeChannel.get(deleteRecordSetName).clear();
										activeChannel.remove(deleteRecordSetName);
										MenuToolBar.log.fine("deleted " + deleteRecordSetName);
										updateRecordSetSelectCombo();
										MenuToolBar.this.application.updateDataTable();
									}
								}
							}
						});
					}
					{
						this.editRecord = new ToolItem(this.recordToolBar, SWT.NONE);
						this.editRecord.setImage(SWTResourceManager.getImage("osde/resource/EditHot.gif"));
						this.editRecord.setToolTipText("umbenennen des aktiven Datensatznamen");
						this.editRecord.setHotImage(SWTResourceManager.getImage("osde/resource/EditHot.gif"));
						this.editRecord.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								MenuToolBar.log.finest("editAufnahme.widgetSelected, event=" + evt);
								MenuToolBar.this.recordSelectCombo.setEditable(true);
								MenuToolBar.this.recordSelectCombo.setFocus();
								// begin here text can be edited
							}
						});
					}
					this.channelSelectComposite.pack();
				}
			}
			this.dataBarComposite.pack();
			int height = this.size.y + 2;
			this.size = this.dataBarComposite.getSize();
			MenuToolBar.log.fine("pre dataBarComposite.size = " + this.size);
			this.dataBarComposite.setSize(this.size.x, height);
			this.size = this.dataBarComposite.getSize();
			MenuToolBar.log.fine("post dataBarComposite.size = " + this.size);
			this.dataCoolItem.setSize(this.size);
			this.dataCoolItem.setPreferredSize(this.size);
			this.dataCoolItem.setMinimumSize(this.size);
		} // end record cool item
	}

	/**
	 * add record set entry to record set select combo
	 * @param newRecordSetName
	 */
	public void addRecordSetName(String newRecordSetName) {
		final String recordSetKey = newRecordSetName;
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				Vector<String> newRecordSetItems = new Vector<String>(MenuToolBar.this.recordSelectCombo.getItems().length);
				String[] recordSetNames = MenuToolBar.this.recordSelectCombo.getItems();
				int index = MenuToolBar.this.recordSelectCombo.getSelectionIndex();
				for (String element : recordSetNames) {
					if (element.length() > 3) newRecordSetItems.add(element);
				}
				newRecordSetItems.add(recordSetKey);
				MenuToolBar.this.recordSelectCombo.setItems(newRecordSetItems.toArray(new String[1]));
				MenuToolBar.this.recordSelectCombo.select(index);
				updateRecordSetSelectCombo();
				updateChannelToolItems();
			}
		});
	}

	/**
	 * updates the channel select combo according the active channel
	 */
	public void updateChannelSelector() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			doUpdateChannelSelector();
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					doUpdateChannelSelector();
				}
			});
		}
	}

	/**
	 * execute the channel selector update
	 */
	void doUpdateChannelSelector() {
		int activeChannelNumber = 0;
		if (this.channels.size() > 0) {
			String[] channelNames = new String[this.channels.size()];
			String activeChannelName = this.channels.getActiveChannel().getName();
			for (int i = 0; i < channelNames.length; i++) {
				channelNames[i] = this.channels.get(i + 1).getName();
				if (channelNames[i].equals(activeChannelName)) activeChannelNumber = i;
			}
			this.channels.setChannelNames(channelNames);
			this.channelSelectCombo.setItems(channelNames); //new String[] { "K1: Kanal 1" }); // "K2: Kanal 2", "K3: Kanal 3", "K4: Kanal 4" });
		}
		else { // no channel
			this.channelSelectCombo.setItems(new String[] { "" });
		}
		this.channelSelectCombo.select(activeChannelNumber); // kanalCombo.setText("K1: Kanal 1");
		updateChannelToolItems();
	}

	/**
	 * updates the record set select combo according the active record set
	 */
	public String[] updateRecordSetSelectCombo() {
		final String[] recordSetNames = this.channels.getActiveChannel().getRecordSetNames();
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			doUpdateRecordSetSelectCombo(recordSetNames);
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					doUpdateRecordSetSelectCombo(recordSetNames);
				}
			});
		}
		return recordSetNames;
	}

	/**
	 * @param recordSetNames
	 */
	void doUpdateRecordSetSelectCombo(final String[] recordSetNames) {
		if (recordSetNames != null && recordSetNames.length > 0 && recordSetNames[0] != null) {
			Channel activeChannel = this.channels.getActiveChannel();
			String activeRecord = activeChannel.getActiveRecordSet() != null ? activeChannel.getActiveRecordSet().getName() : recordSetNames[0];
			this.recordSelectCombo.setItems(recordSetNames); //new String[] { "1) Datensatz" }); // "2) Flugaufzeichnung", "3) laden" });
			for (int i = 0; i < recordSetNames.length; i++) {
				if (recordSetNames[i].equals(activeRecord)) this.recordSelectCombo.select(i); // aufnahmeCombo.setText("1) Datensatz");
			}
		}
		else {
			this.recordSelectCombo.setItems(new String[0]);
			this.recordSelectCombo.setText("");
		}
		updateRecordToolItems();
		this.application.updateGraphicsWindow();
	}

	/**
	 * updates the netxtRecord , prevRecord tool items
	 */
	public void updateRecordToolItems() {
		int numberRecords = this.channels.getActiveChannel().getRecordSetNames().length;
		if (numberRecords <= 1) {
			this.nextRecord.setEnabled(false);
			this.prevRecord.setEnabled(false);
		}
		else {
			int index = this.recordSelectCombo.getSelectionIndex();
			int maxIndex = this.recordSelectCombo.getItemCount() - 1;
			if (numberRecords == 2 && index == 0) {
				this.nextRecord.setEnabled(true);
				this.prevRecord.setEnabled(false);
			}
			else if (numberRecords == 2 && index == 1) {
				this.nextRecord.setEnabled(false);
				this.prevRecord.setEnabled(true);
			}
			if (numberRecords >= 2 && index == 0) {
				this.nextRecord.setEnabled(true);
				this.prevRecord.setEnabled(false);
			}
			else if (numberRecords >= 2 && index == maxIndex) {
				this.nextRecord.setEnabled(false);
				this.prevRecord.setEnabled(true);
			}
			else {
				this.nextRecord.setEnabled(true);
				this.prevRecord.setEnabled(true);
			}
		}
	}

	/**
	 * 
	 */
	void doUpdateChannelToolItems() {
		int numberChannels = this.channels.size();
		if (numberChannels <= 1) {
			this.nextChannel.setEnabled(false);
			this.prevChannel.setEnabled(false);
		}
		else {
			int index = this.channelSelectCombo.getSelectionIndex();
			int maxIndex = this.channelSelectCombo.getItemCount() - 1;
			if (numberChannels == 2 && index == 0) {
				this.nextChannel.setEnabled(true);
				this.prevChannel.setEnabled(false);
			}
			else if (numberChannels == 2 && index == 1) {
				this.nextChannel.setEnabled(false);
				this.prevChannel.setEnabled(true);
			}
			if (numberChannels >= 2 && index == 0) {
				this.nextChannel.setEnabled(true);
				this.prevChannel.setEnabled(false);
			}
			else if (numberChannels >= 2 && index == maxIndex) {
				this.nextChannel.setEnabled(false);
				this.prevChannel.setEnabled(true);
			}
			else {
				this.nextChannel.setEnabled(true);
				this.prevChannel.setEnabled(true);
			}
		}
	}

	/**
	 * updates the netxtChannel , prevChannel tool items
	 */
	public void updateChannelToolItems() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			doUpdateChannelToolItems();
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					doUpdateChannelToolItems();
				}
			});
		}
	}

	/**
	 * this function must only called by application which make secure to choose the right thread
	 * @param isOpenStatus
	 */
	public void setPortConnected(final boolean isOpenStatus) {
		if (isOpenStatus) {
			this.portOpenCloseItem.setDisabledImage(SWTResourceManager.getImage("osde/resource/PortCloseDisabled.gif"));
			this.portOpenCloseItem.setHotImage(SWTResourceManager.getImage("osde/resource/PortClose.gif"));
			this.portOpenCloseItem.setImage(SWTResourceManager.getImage("osde/resource/PortCloseHot.gif"));
			//portOpenCloseLabel.setText("Port schliessen");
		}
		else {
			if (!this.application.isDisposed()) {
				this.portOpenCloseItem.setDisabledImage(SWTResourceManager.getImage("osde/resource/PortOpenDisabled.gif"));
				this.portOpenCloseItem.setHotImage(SWTResourceManager.getImage("osde/resource/PortOpenHot.gif"));
				this.portOpenCloseItem.setImage(SWTResourceManager.getImage("osde/resource/PortOpen.gif"));
				//portOpenCloseLabel.setText("Port öffnen         ");
			}
		}
	}

	public CCombo getChannelSelectCombo() {
		return this.channelSelectCombo;
	}

	public CCombo getRecordSelectCombo() {
		return this.recordSelectCombo;
	}

	public void enableDeviceSwitchButtons(boolean enabled) {
		this.prevDeviceToolItem.setEnabled(enabled);
		this.nextDeviceToolItem.setEnabled(enabled);
		updateChannelSelector();
	}

	@SuppressWarnings("unused")
	private void initGUI() {
		try {
			{
				this.application.setSize(800, 200);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
