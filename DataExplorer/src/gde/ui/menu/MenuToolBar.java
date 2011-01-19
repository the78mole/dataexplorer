/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011 Winfried Bruegmann
****************************************************************************************/
package gde.ui.menu;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.DeviceDialog;
import gde.device.IDevice;
import gde.io.FileHandler;
import gde.io.OsdReaderWriter;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.dialog.DeviceSelectionDialog;
import gde.ui.dialog.GoogleEarthCustomizingDialog;
import gde.ui.dialog.PrintSelectionDialog;
import gde.ui.tab.GraphicsComposite;
import gde.ui.tab.GraphicsWindow;
import gde.utils.ApplicationLauncher;
import gde.utils.FileUtils;
import gde.utils.ObjectKeyScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * Graphical menu tool bar class
 * (future items are: scaling icons, ...)
 * @author Winfried Brügmann
 */
public class MenuToolBar {
	final static Logger						log	= Logger.getLogger(MenuToolBar.class.getName());
	
	final String[]								SCOPE_VALUES					= Messages.getString(MessageIds.GDE_MSGT0196).split(GDE.STRING_SEMICOLON);
	StringBuffer									toolBarSizes 					= new StringBuffer();

	Point													toolSize, coolSize;
	CoolBar												coolBar;

	CoolItem											fileCoolItem;
	ToolBar												fileToolBar;
	ToolItem											copyToolItem, printToolItem, newToolItem, openToolItem, saveToolItem, saveAsToolItem, settingsToolItem;
	
	CoolItem											deviceObjectCoolItem;
	ToolBar												deviceObjectToolBar;
	ToolItem											deviceSelectToolItem, toolBoxToolItem;
	ToolItem											prevDeviceToolItem, nextDeviceToolItem;
	Composite											objectSelectComposite;
	CCombo												objectSelectCombo;
	Point													objectSelectSize = new Point(200, GDE.IS_LINUX ? 22 : 20);
	ToolItem											newObject, deleteObject, editObject;
	String												oldObjectKey = null;
	boolean												isObjectoriented = false;

	CoolItem											zoomCoolItem;
	ToolBar												zoomToolBar;
	ToolItem											zoomWindowItem, panItem, fitIntoItem, cutLeftItem, cutRightItem, scopePointsComboSep;
	Composite											scopePointsComposite;
	CCombo 												scopePointsCombo;
	Point													scopePointsComboSize = new Point(70, GDE.IS_LINUX ? 22 : 20);
	static final int							leadFill	= 4+(GDE.IS_WINDOWS == true ? 0 : 3);
	static final int							trailFill	= 4+(GDE.IS_WINDOWS == true ? 0 : 3);
	boolean												isScopePointsCombo = true;
	int														toolButtonHeight = 23;

	CoolItem											portCoolItem;
	ToolBar												portToolBar;
	ToolItem											portOpenCloseItem;
	int														iconSet = DeviceCommPort.ICON_SET_OPEN_CLOSE; 
	
	CoolItem											dataCoolItem;
	ToolBar												dataToolBar;
	ToolItem											nextChannel, prevChannel, prevRecord, nextRecord, separator, deleteRecord, editRecord;
	Composite											channelSelectComposite, recordSelectComposite;
	CCombo												channelSelectCombo, recordSelectCombo;
	Point													channelSelectSize = new Point(180, GDE.IS_LINUX ? 22 : 20);
	Point													recordSelectSize = new Point(260, GDE.IS_LINUX ? 22 : 20);

	CoolItem											helpCoolItem;
	ToolBar												helpToolBar;
	ToolItem											helpToolItem;
	
	CoolItem											googleEarthCoolItem;
	ToolBar												googleEarthToolBar;
	ToolItem											googleEarthToolItem, googleEarthConfigToolItem;
	
	final DataExplorer	application;
	final Channels								channels;
	final Settings								settings;
	final String									language;
	final FileHandler							fileHandler;

	public MenuToolBar(DataExplorer parent, CoolBar menuCoolBar) {
		this.application = parent;
		this.coolBar = menuCoolBar;
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
		this.language = this.settings.getLocale().getLanguage();
		this.fileHandler = new FileHandler();
	}

	public void init() {
		this.coolBar = new CoolBar(this.application, SWT.NONE);
		SWTResourceManager.registerResourceUser(this.coolBar);
		create();
	}

	public void create() {
		//long startTime = new Date().getTime();
		{ // begin file cool item
			this.fileCoolItem = new CoolItem(this.coolBar, SWT.NONE);
			{ // begin file tool bar
				this.fileToolBar = new ToolBar(this.coolBar, SWT.NONE);
				this.fileCoolItem.setControl(this.fileToolBar);
				{
					this.newToolItem = new ToolItem(this.fileToolBar, SWT.NONE);
					this.newToolItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0050));
					this.newToolItem.setImage(SWTResourceManager.getImage("gde/resource/New.gif")); //$NON-NLS-1$
					this.newToolItem.setHotImage(SWTResourceManager.getImage("gde/resource/NewHot.gif")); //$NON-NLS-1$
					this.newToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "newToolItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuToolBar.this.application.getDeviceSelectionDialog().checkDataSaved()) {
								MenuToolBar.this.application.getDeviceSelectionDialog().setupDataChannels(MenuToolBar.this.application.getActiveDevice());
							}
						}
					});
				}
				{
					this.openToolItem = new ToolItem(this.fileToolBar, SWT.NONE);
					this.openToolItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0051)); //$NON-NLS-1$
					this.openToolItem.setImage(SWTResourceManager.getImage("gde/resource/Open.gif")); //$NON-NLS-1$
					this.openToolItem.setHotImage(SWTResourceManager.getImage("gde/resource/OpenHot.gif")); //$NON-NLS-1$
					this.openToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "openToolItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuToolBar.this.fileHandler.openFileDialog(Messages.getString(MessageIds.GDE_MSGT0004));
						}
					});
				}
				{
					this.saveToolItem = new ToolItem(this.fileToolBar, SWT.NONE);
					this.saveToolItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0053));
					this.saveToolItem.setImage(SWTResourceManager.getImage("gde/resource/Save.gif")); //$NON-NLS-1$
					this.saveToolItem.setHotImage(SWTResourceManager.getImage("gde/resource/SaveHot.gif")); //$NON-NLS-1$
					this.saveToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "saveToolItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							Channel activeChannel = MenuToolBar.this.channels.getActiveChannel();
							if (activeChannel != null) {
								if (!activeChannel.isSaved())
									MenuToolBar.this.fileHandler.saveOsdFile(Messages.getString(MessageIds.GDE_MSGT0006), GDE.STRING_EMPTY);
								else
									MenuToolBar.this.fileHandler.saveOsdFile(Messages.getString(MessageIds.GDE_MSGT0007), activeChannel.getFileName());
							}
						}
					});
				}
				{
					this.saveAsToolItem = new ToolItem(this.fileToolBar, SWT.NONE);
					this.saveAsToolItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0054));
					this.saveAsToolItem.setImage(SWTResourceManager.getImage("gde/resource/SaveAs.gif")); //$NON-NLS-1$
					this.saveAsToolItem.setHotImage(SWTResourceManager.getImage("gde/resource/SaveAsHot.gif")); //$NON-NLS-1$
					this.saveAsToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "saveAsToolItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuToolBar.this.fileHandler.saveOsdFile(Messages.getString(MessageIds.GDE_MSGT0006), GDE.STRING_EMPTY);
						}
					});
				}
				{
					this.settingsToolItem = new ToolItem(this.fileToolBar, SWT.NONE);
					this.settingsToolItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0055));
					this.settingsToolItem.setImage(SWTResourceManager.getImage("gde/resource/Settings.gif")); //$NON-NLS-1$
					this.settingsToolItem.setHotImage(SWTResourceManager.getImage("gde/resource/SettingsHot.gif")); //$NON-NLS-1$
					this.settingsToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "settingsToolItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							// check if other none modal dialog is open
							DeviceDialog deviceDialog = MenuToolBar.this.application.getDeviceDialog();
							if (deviceDialog == null || deviceDialog.isDisposed()) {
								MenuToolBar.this.application.openSettingsDialog();
								MenuToolBar.this.application.setStatusMessage(GDE.STRING_EMPTY);
							}
							else
								MenuToolBar.this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGW0002), SWT.COLOR_RED);
						}
					});
				}
				{
					this.copyToolItem = new ToolItem(this.fileToolBar, SWT.NONE);
					this.copyToolItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0073));
					this.copyToolItem.setImage(SWTResourceManager.getImage("gde/resource/Copy.gif")); //$NON-NLS-1$
					this.copyToolItem.setHotImage(SWTResourceManager.getImage("gde/resource/CopyHot.gif")); //$NON-NLS-1$
					this.copyToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "copyToolItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuToolBar.this.application.copyTabContentAsImage();
						}
					});
				}
				{
					this.printToolItem = new ToolItem(this.fileToolBar, SWT.NONE);
					this.printToolItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0074));
					this.printToolItem.setImage(SWTResourceManager.getImage("gde/resource/Print.gif")); //$NON-NLS-1$
					this.printToolItem.setHotImage(SWTResourceManager.getImage("gde/resource/PrintHot.gif")); //$NON-NLS-1$
					this.printToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "printToolItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							new PrintSelectionDialog(DataExplorer.shell, SWT.NULL).open();
						}
					});
				}
				this.toolSize = this.fileToolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				this.fileToolBar.setSize(this.toolSize);
				log.log(Level.FINE, "fileToolBar.size = " + this.toolSize); //$NON-NLS-1$
			} // end file tool bar
			this.fileCoolItem.setSize(this.toolSize.x, this.toolSize.y);
			//this.fileCoolItem.setPreferredSize(this.size);
			this.fileCoolItem.setMinimumSize(this.toolSize.x, this.toolSize.y);
			this.toolBarSizes.append(this.toolSize.x).append(GDE.STRING_COLON).append(this.toolSize.y).append(GDE.STRING_SEMICOLON);
			
			// set height used for selection combos
			this.toolButtonHeight = this.settingsToolItem.getBounds().height;
		} // end file cool item

		{ // begin device cool item
			this.deviceObjectCoolItem = new CoolItem(this.coolBar, SWT.NONE);
			{ // begin device tool bar
				this.deviceObjectToolBar = new ToolBar(this.coolBar, SWT.NONE);
				this.deviceObjectCoolItem.setControl(this.deviceObjectToolBar);
				{
					this.deviceSelectToolItem = new ToolItem(this.deviceObjectToolBar, SWT.NONE);
					this.deviceSelectToolItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0057));
					this.deviceSelectToolItem.setImage(SWTResourceManager.getImage("gde/resource/DeviceSelection.gif")); //$NON-NLS-1$
					this.deviceSelectToolItem.setHotImage(SWTResourceManager.getImage("gde/resource/DeviceSelectionHot.gif")); //$NON-NLS-1$
					this.deviceSelectToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "deviceToolItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							DeviceSelectionDialog deviceSelection = MenuToolBar.this.application.getDeviceSelectionDialog();
							if (deviceSelection.checkDataSaved()) {
								deviceSelection.open();
							}
						}
					});
				}
				{
					this.prevDeviceToolItem = new ToolItem(this.deviceObjectToolBar, SWT.NONE);
					this.prevDeviceToolItem.setImage(SWTResourceManager.getImage("gde/resource/ArrowWhiteGreenFieldLeft.gif")); //$NON-NLS-1$
					this.prevDeviceToolItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0058));
					this.prevDeviceToolItem.setHotImage(SWTResourceManager.getImage("gde/resource/ArrowWhiteGreenFieldLefHot.gif")); //$NON-NLS-1$
					this.prevDeviceToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "prevDeviceToolItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							// allow device switch only if port not connected
							if (MenuToolBar.this.application.getActiveDevice() == null 
									|| (MenuToolBar.this.application.getActiveDevice() != null && MenuToolBar.this.application.getActiveDevice().getCommunicationPort() == null)
									|| (MenuToolBar.this.application.getActiveDevice() != null && MenuToolBar.this.application.getActiveDevice().getCommunicationPort() != null && !MenuToolBar.this.application.getActiveDevice().getCommunicationPort().isConnected())) {
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
								MenuToolBar.this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGW0017));
							}
						}
					});
				}
				{
					this.nextDeviceToolItem = new ToolItem(this.deviceObjectToolBar, SWT.NONE);
					this.nextDeviceToolItem.setImage(SWTResourceManager.getImage("gde/resource/ArrowWhiteGreenFieldRight.gif")); //$NON-NLS-1$
					this.nextDeviceToolItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0059));
					this.nextDeviceToolItem.setHotImage(SWTResourceManager.getImage("gde/resource/ArrowWhiteGreenFieldRightHot.gif")); //$NON-NLS-1$
					this.nextDeviceToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "nextDeviceToolItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							// allow device switch only if port not connected
							if (MenuToolBar.this.application.getActiveDevice() == null 
									|| (MenuToolBar.this.application.getActiveDevice() != null && MenuToolBar.this.application.getActiveDevice().getCommunicationPort() == null)
									|| (MenuToolBar.this.application.getActiveDevice() != null && MenuToolBar.this.application.getActiveDevice().getCommunicationPort() != null && !MenuToolBar.this.application.getActiveDevice().getCommunicationPort().isConnected())) {
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
								MenuToolBar.this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGW0030));
							}
						}
					});
				}
				{
					this.toolBoxToolItem = new ToolItem(this.deviceObjectToolBar, SWT.NONE);
					this.toolBoxToolItem.setImage(SWTResourceManager.getImage("gde/resource/ToolBox.gif")); //$NON-NLS-1$
					this.toolBoxToolItem.setHotImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
					this.toolBoxToolItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0060));
					this.toolBoxToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "toolBoxToolItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuToolBar.this.application.getDeviceDialog() != null) {
								MenuToolBar.this.application.getDeviceDialog().open();
							}
							else {
								MenuToolBar.this.application.getDeviceSelectionDialog().open();
							}
						}
					});
				}
				{
					ToolItem objectSelectComboSep = new ToolItem(this.deviceObjectToolBar, SWT.SEPARATOR);
					{
						this.objectSelectComposite = new Composite(this.deviceObjectToolBar, SWT.NONE);
						this.objectSelectComposite.setLayout(null);
						this.objectSelectCombo = new CCombo(this.objectSelectComposite, SWT.BORDER | SWT.LEFT | SWT.READ_ONLY);
						this.objectSelectCombo.setFont(SWTResourceManager.getFont(this.application, GDE.IS_LINUX ? 9 : GDE.IS_MAC ? 12 : 10, SWT.NORMAL));
						this.objectSelectCombo.setItems(this.settings.getObjectList()); // "device-oriented", "ASW-27", "AkkuSubC_1"" });
						this.objectSelectCombo.select(this.settings.getActiveObjectIndex());
						this.isObjectoriented = this.settings.getActiveObjectIndex() > 0;
						this.objectSelectCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0201));
						this.objectSelectCombo.setEditable(false);
						this.objectSelectCombo.setBackground(DataExplorer.COLOR_WHITE);
						this.objectSelectCombo.setVisibleItemCount(this.objectSelectCombo.getItemCount()+1);
						this.objectSelectCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "objectSelectCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								MenuToolBar.this.application.checkSaveObjectData();
								int selectionIndex = MenuToolBar.this.objectSelectCombo.getSelectionIndex();
								if (selectionIndex != 0) {
									MenuToolBar.this.editObject.setEnabled(true);
									MenuToolBar.this.deleteObject.setEnabled(true);
									checkChannelForObjectKeyMissmatch(selectionIndex, MenuToolBar.this.objectSelectCombo.getText());
									MenuToolBar.this.isObjectoriented = true;
									
									MenuToolBar.this.application.setObjectDescriptionTabVisible(true);
								}
								else { // device oriented
									MenuToolBar.this.editObject.setEnabled(false);
									MenuToolBar.this.deleteObject.setEnabled(false);
									checkChannelForObjectKeyMissmatch(selectionIndex, GDE.STRING_EMPTY);
									MenuToolBar.this.isObjectoriented = false;
									MenuToolBar.this.application.setObjectDescriptionTabVisible(false);
								}
								MenuToolBar.this.settings.setObjectList(MenuToolBar.this.objectSelectCombo.getItems(), selectionIndex);
								//MenuToolBar.this.application.getDeviceSelectionDialog().setupDataChannels(MenuToolBar.this.application.getActiveDevice());
								MenuToolBar.this.application.updateObjectDescriptionWindow();
								MenuToolBar.this.application.updateTitleBar(MenuToolBar.this.getActiveObjectKey(), MenuToolBar.this.application.getActiveDevice().getName(), MenuToolBar.this.application.getActiveDevice().getPort());
							}
						});
						this.objectSelectCombo.addKeyListener(new KeyAdapter() {
							public void keyPressed(KeyEvent evt) {
								log.log(Level.FINEST, "recordSelectCombo.keyPressed, event=" + evt); //$NON-NLS-1$
								if (evt.character == SWT.CR) {
									MenuToolBar.this.objectSelectCombo.setEditable(false);
									MenuToolBar.this.deviceObjectToolBar.setFocus();
									String newObjKey = MenuToolBar.this.objectSelectCombo.getText();
									log.log(Level.FINE, "newObjKey = " + newObjKey); //$NON-NLS-1$
									if (newObjKey.length() >= 1) {
										String[] tmpObjects = MenuToolBar.this.objectSelectCombo.getItems();
										int selectionIndex = 0;
										if (MenuToolBar.this.oldObjectKey == null) { // new object key
											for (; selectionIndex < tmpObjects.length; selectionIndex++) {
												if (tmpObjects[selectionIndex].equals(GDE.STRING_EMPTY)) {
													tmpObjects[selectionIndex] = newObjKey;
													break;
												}
											}
											checkChannelForObjectKeyMissmatch(selectionIndex, newObjKey);
										}
										else { //rename object key
											log.log(Level.FINE, "oldObjectKey = " + MenuToolBar.this.oldObjectKey); //$NON-NLS-1$
											if (MenuToolBar.this.oldObjectKey.length() >= 1) {
												checkChannelForObjectKeyMissmatch(selectionIndex, newObjKey);

												// query if new object key should be used to modify all existing data files with the new corrected one
												int answer = MenuToolBar.this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0048, new String[] {MenuToolBar.this.oldObjectKey, newObjKey}));
												if (answer == SWT.YES) {
													OsdReaderWriter.updateObjectKey(MenuToolBar.this.oldObjectKey, newObjKey);
													MenuToolBar.this.application.updateCurrentObjectData(newObjKey);
												}
												
												//query for old directory deletion
												answer = MenuToolBar.this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGW0031));
												if (answer == SWT.YES) 
													FileUtils.deleteDirectory(MenuToolBar.this.settings.getDataFilePath() + GDE.FILE_SEPARATOR_UNIX  + MenuToolBar.this.oldObjectKey);
												
												//replace modified object key
												for (; selectionIndex < tmpObjects.length; selectionIndex++) {
													if (tmpObjects[selectionIndex].equals(MenuToolBar.this.oldObjectKey)) {
														tmpObjects[selectionIndex] = newObjKey;
														break;
													}
												}
												MenuToolBar.this.oldObjectKey = null;
											}
										}
										MenuToolBar.this.setObjectList(tmpObjects, newObjKey);
										
										if (selectionIndex >= 1) {
											MenuToolBar.this.deleteObject.setEnabled(true);
											MenuToolBar.this.editObject.setEnabled(true);
											MenuToolBar.this.isObjectoriented = true;
											//MenuToolBar.this.activeObjectKey = MenuToolBar.this.objectSelectCombo.getItem(selectionIndex);
										}
										MenuToolBar.this.application.updateObjectDescriptionWindow();
										new ObjectKeyScanner(newObjKey).start();
									}
									else { // undefined newObjectKey
										Vector<String> tmpObjectKeys = new Vector<String>();
										for (String objectKey : MenuToolBar.this.objectSelectCombo.getItems()) {
											if (objectKey.length() >=1 ) tmpObjectKeys.add(objectKey);
										}
										MenuToolBar.this.objectSelectCombo.setItems(tmpObjectKeys.toArray(new String[1]));
										MenuToolBar.this.objectSelectCombo.select(MenuToolBar.this.isObjectoriented ? 1 : 0);
										MenuToolBar.this.application.setObjectDescriptionTabVisible(MenuToolBar.this.isObjectoriented);
										MenuToolBar.this.application.updateObjectDescriptionWindow();
									}
									MenuToolBar.this.application.updateTitleBar(MenuToolBar.this.getActiveObjectKey(), MenuToolBar.this.application.getActiveDevice().getName(), MenuToolBar.this.application.getActiveDevice().getPort());
								}
							}
						});		
						this.objectSelectCombo.setSize(this.objectSelectSize);
						this.objectSelectComposite.setSize(this.objectSelectSize.x+leadFill+trailFill, this.toolButtonHeight);
						this.objectSelectCombo.setLocation(leadFill, GDE.IS_MAC ? 1 : (this.toolButtonHeight - this.objectSelectSize.y) / 2 - 1);
					}
					objectSelectComboSep.setWidth(this.objectSelectComposite.getSize().x);
					objectSelectComboSep.setControl(this.objectSelectComposite);
				}
				{
					this.newObject = new ToolItem(this.deviceObjectToolBar, SWT.NONE);
					this.newObject.setImage(SWTResourceManager.getImage("gde/resource/NewObj.gif")); //$NON-NLS-1$
					this.newObject.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0202));
					this.newObject.setHotImage(SWTResourceManager.getImage("gde/resource/NewObjHot.gif")); //$NON-NLS-1$
					this.newObject.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "newObject.widgetSelected, event=" + evt); //$NON-NLS-1$
							Vector<String> tmpObjects = new Vector<String>();
							for (String tmpObject : MenuToolBar.this.settings.getObjectList()) {
								tmpObjects.add(tmpObject);
							}
							tmpObjects.add(GDE.STRING_EMPTY);
							MenuToolBar.this.application.setObjectDescriptionTabVisible(true);
							MenuToolBar.this.objectSelectCombo.setItems(tmpObjects.toArray(new String[1])); // "None", "ASW-27", "AkkuSubC_1", "" });
							MenuToolBar.this.objectSelectCombo.select(tmpObjects.size() - 1);
							MenuToolBar.this.objectSelectCombo.setEditable(true);
							MenuToolBar.this.objectSelectCombo.setFocus();
							// begin here text can be edited -> key listener
						}
					});
				}
				{
					this.deleteObject = new ToolItem(this.deviceObjectToolBar, SWT.NONE);
					this.deleteObject.setImage(SWTResourceManager.getImage("gde/resource/RemObj.gif")); //$NON-NLS-1$
					this.deleteObject.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0203));
					this.deleteObject.setHotImage(SWTResourceManager.getImage("gde/resource/RemObjHot.gif")); //$NON-NLS-1$
					if (this.objectSelectCombo.getItemCount() == 1)	MenuToolBar.this.deleteObject.setEnabled(false);
					this.deleteObject.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "deleteObject.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuToolBar.this.objectSelectCombo.getSelectionIndex() != 0) {
								Vector<String> tmpObjects = new Vector<String>();
								for (String tmpObject : MenuToolBar.this.objectSelectCombo.getItems()) {
									tmpObjects.add(tmpObject);
								}
								int currentIndex = MenuToolBar.this.objectSelectCombo.getSelectionIndex();
								String delObjectKey = tmpObjects.elementAt(currentIndex);
								tmpObjects.remove(currentIndex);
								MenuToolBar.this.objectSelectCombo.setItems(tmpObjects.toArray(new String[1])); // "None", "ASW-27", "AkkuSubC_1", "" });
								currentIndex = currentIndex >= 2 ? currentIndex - 1 : tmpObjects.size() > 1 ? 1 : 0;
								MenuToolBar.this.objectSelectCombo.select(currentIndex);
								FileUtils.deleteDirectory(MenuToolBar.this.settings.getDataFilePath() + GDE.FILE_SEPARATOR_UNIX + delObjectKey);
								if (currentIndex == 0) {
									MenuToolBar.this.deleteObject.setEnabled(false);
									MenuToolBar.this.editObject.setEnabled(false);
									MenuToolBar.this.isObjectoriented = false;
								}
								MenuToolBar.this.settings.setObjectList(tmpObjects.toArray(new String[1]), currentIndex);
								
								MenuToolBar.this.application.setObjectDescriptionTabVisible(MenuToolBar.this.isObjectoriented);
								MenuToolBar.this.application.updateObjectDescriptionWindow();
							}
							MenuToolBar.this.application.updateTitleBar(MenuToolBar.this.getActiveObjectKey(), MenuToolBar.this.application.getActiveDevice().getName(), MenuToolBar.this.application.getActiveDevice().getPort());
						}
					});
				}
				{
					this.editObject = new ToolItem(this.deviceObjectToolBar, SWT.NONE);
					this.editObject.setImage(SWTResourceManager.getImage("gde/resource/EditObj.gif")); //$NON-NLS-1$
					this.editObject.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0204));
					this.editObject.setHotImage(SWTResourceManager.getImage("gde/resource/EditObjHot.gif")); //$NON-NLS-1$
					if (this.objectSelectCombo.getItemCount() == 1)	MenuToolBar.this.editObject.setEnabled(false);
					this.editObject.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "editObject.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (MenuToolBar.this.objectSelectCombo.getSelectionIndex() != 0) {
								MenuToolBar.this.oldObjectKey = MenuToolBar.this.objectSelectCombo.getItems()[MenuToolBar.this.objectSelectCombo.getSelectionIndex()];
								MenuToolBar.this.objectSelectCombo.setEditable(true);
								MenuToolBar.this.objectSelectCombo.setFocus();
								// begin here text can be edited -> key listener
							}
						}
					});
				}
				this.toolSize = this.deviceObjectToolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				this.deviceObjectToolBar.setSize(this.toolSize);
				log.log(Level.FINE, "deviceToolBar.size = " + this.toolSize); //$NON-NLS-1$
			} // end device tool bar
			this.deviceObjectCoolItem.setSize(this.toolSize.x, this.toolSize.y);
			//this.deviceCoolItem.setPreferredSize(this.size);
			this.deviceObjectCoolItem.setMinimumSize(this.toolSize.x, this.toolSize.y);
			this.toolBarSizes.append(this.toolSize.x).append(GDE.STRING_COLON).append(this.toolSize.y).append(GDE.STRING_SEMICOLON);
		} // end device cool item
		
		{ // begin zoom cool item
			this.zoomCoolItem = new CoolItem(this.coolBar, SWT.NONE);
			{ // begin zoom tool bar
				this.zoomToolBar = new ToolBar(this.coolBar, SWT.NONE);
				this.zoomCoolItem.setControl(this.zoomToolBar);
				{
					this.zoomWindowItem = new ToolItem(this.zoomToolBar, SWT.NONE);
					this.zoomWindowItem.setImage(SWTResourceManager.getImage("gde/resource/Zoom.gif")); //$NON-NLS-1$
					this.zoomWindowItem.setHotImage(SWTResourceManager.getImage("gde/resource/ZoomHot.gif")); //$NON-NLS-1$
					this.zoomWindowItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0061));
					this.zoomWindowItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "zoomWindowItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuToolBar.this.application.setGraphicsMode(GraphicsComposite.MODE_ZOOM, true);
							MenuToolBar.this.scopePointsCombo.setEnabled(false);
						}
					});
				}
				{
					this.panItem = new ToolItem(this.zoomToolBar, SWT.NONE);
					this.panItem.setImage(SWTResourceManager.getImage("gde/resource/Pan.gif")); //$NON-NLS-1$
					this.panItem.setHotImage(SWTResourceManager.getImage("gde/resource/PanHot.gif")); //$NON-NLS-1$
					this.panItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0062));
					this.panItem.setEnabled(false);
					this.panItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "resizeItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuToolBar.this.application.setGraphicsMode(GraphicsComposite.MODE_PAN, true);
						}
					});
				}
				{
					this.cutLeftItem = new ToolItem(this.zoomToolBar, SWT.NONE);
					this.cutLeftItem.setImage(SWTResourceManager.getImage("gde/resource/CutLeft.gif")); //$NON-NLS-1$
					this.cutLeftItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0063));
					this.cutLeftItem.setEnabled(false);
					this.cutLeftItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "cutLeftItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuToolBar.this.application.setCutModeActive(true, false);
						}
					});
				}
				{
					this.cutRightItem = new ToolItem(this.zoomToolBar, SWT.NONE);
					this.cutRightItem.setImage(SWTResourceManager.getImage("gde/resource/CutRight.gif")); //$NON-NLS-1$
					this.cutRightItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0064));
					this.cutRightItem.setEnabled(false);
					this.cutRightItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "cutRightItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuToolBar.this.application.setCutModeActive(false, true);						}
					});
				}
				{
					this.fitIntoItem = new ToolItem(this.zoomToolBar, SWT.NONE);
					this.fitIntoItem.setImage(SWTResourceManager.getImage("gde/resource/Expand.gif")); //$NON-NLS-1$
					this.fitIntoItem.setHotImage(SWTResourceManager.getImage("gde/resource/ExpandHot.gif")); //$NON-NLS-1$
					this.fitIntoItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0065));
					this.fitIntoItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "fitIntoItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuToolBar.this.application.setGraphicsMode(GraphicsComposite.MODE_RESET, false);
						}
					});
				}
				{
					this.scopePointsComboSep = new ToolItem(this.zoomToolBar, SWT.SEPARATOR);
					{
						this.scopePointsComposite = new Composite(this.zoomToolBar, SWT.NONE);
						this.scopePointsComposite.setLayout(null);
						this.scopePointsCombo = new CCombo(this.scopePointsComposite, SWT.BORDER | SWT.LEFT | SWT.READ_ONLY);
						this.scopePointsCombo.setFont(SWTResourceManager.getFont(this.application, GDE.IS_LINUX ? 9 : GDE.IS_MAC ? 12 : 10, SWT.NORMAL));
						this.scopePointsCombo.setItems(SCOPE_VALUES);
						this.scopePointsCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
						this.scopePointsCombo.select(0);
						this.scopePointsCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0072));
						this.scopePointsCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "scopePointsCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								try {
									new Integer(MenuToolBar.this.scopePointsCombo.getText().trim());
									MenuToolBar.this.application.setGraphicsMode(GraphicsComposite.MODE_SCOPE, true);
									MenuToolBar.this.zoomWindowItem.setEnabled(false);
								}
								catch(Exception e) {
									MenuToolBar.this.application.setGraphicsMode(GraphicsComposite.MODE_RESET, false);
								}
							}
						});
						//this.scopePointsComboSize.x = SWTResourceManager.getGC(this.scopePointsCombo.getDisplay()).stringExtent("00000000").x;
						this.scopePointsCombo.setSize(this.scopePointsComboSize);
						this.scopePointsComposite.setSize(this.scopePointsComboSize.x+leadFill+trailFill, this.toolButtonHeight);
						this.scopePointsCombo.setLocation(leadFill, GDE.IS_MAC ? 1 : (this.toolButtonHeight - this.objectSelectSize.y) / 2 - 1);
					}					
					this.scopePointsComboSep.setWidth(this.scopePointsComposite.getSize().x);
					this.scopePointsComboSep.setControl(this.scopePointsComposite);
				}
				this.toolSize = this.zoomToolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				this.zoomToolBar.setSize(this.toolSize);
				log.log(Level.FINE, "zoomToolBar.size = " + this.toolSize); //$NON-NLS-1$
			} // end zoom tool bar
			this.zoomCoolItem.setSize(this.toolSize.x, this.toolSize.y);
			//this.zoomCoolItem.setPreferredSize(this.size);
			this.zoomCoolItem.setMinimumSize(this.toolSize.x, this.toolSize.y);
			this.toolBarSizes.append(this.toolSize.x).append(GDE.STRING_COLON).append(this.toolSize.y).append(GDE.STRING_SEMICOLON);
		} // end zoom cool item

		{ // begin port cool item
			this.portCoolItem = new CoolItem(this.coolBar, SWT.NONE);
			{
				this.portToolBar = new ToolBar(this.coolBar, SWT.NONE);
				this.portCoolItem.setControl(this.portToolBar);
				{
					this.portOpenCloseItem = new ToolItem(this.portToolBar, SWT.NONE);
					this.portOpenCloseItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0066));
					this.portOpenCloseItem.setImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/PortOpen.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setDisabledImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/PortOpenDisabled.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setHotImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/PortOpenHot.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "portOpenCloseItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							IDevice activeDevice = MenuToolBar.this.application.getActiveDevice();
							if(activeDevice != null) {
								activeDevice.open_closeCommPort();
								if (activeDevice.getCommunicationPort() != null) {
									if (activeDevice.getCommunicationPort().isConnected()) {
										MenuToolBar.this.portOpenCloseItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0067));
									}
									else {
										MenuToolBar.this.portOpenCloseItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0066));
									}
								}
							}
						}
					});
				}
				//this.portToolBar.pack();
				this.toolSize = this.portToolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				this.portToolBar.setSize(this.toolSize);
				log.log(Level.FINE, "portToolBar.size = " + this.toolSize); //$NON-NLS-1$
			}
			this.portCoolItem.setSize(this.toolSize.x, this.toolSize.y);
			//this.portCoolItem.setPreferredSize(this.size);
			this.portCoolItem.setMinimumSize(this.toolSize.x, this.toolSize.y);
			this.toolBarSizes.append(this.toolSize.x).append(GDE.STRING_COLON).append(this.toolSize.y).append(GDE.STRING_SEMICOLON);
		} // end port cool item
		
		{ // begin data cool item (channel select, record select)
			this.dataCoolItem = new CoolItem(this.coolBar, SWT.NONE);
			{
				this.dataToolBar = new ToolBar(this.coolBar, SWT.NONE);
				this.dataCoolItem.setControl(this.dataToolBar);
				{
					ToolItem channelSelectComboSep = new ToolItem(this.dataToolBar, SWT.SEPARATOR);
					{
						this.channelSelectComposite = new Composite(this.dataToolBar, SWT.NONE);
						this.channelSelectComposite.setLayout(null);
						this.channelSelectCombo = new CCombo(this.channelSelectComposite, SWT.BORDER | SWT.LEFT | SWT.READ_ONLY);
						this.channelSelectCombo.setFont(SWTResourceManager.getFont(this.application, GDE.IS_LINUX ? 9 : GDE.IS_MAC ? 12 : 10, SWT.NORMAL));
						this.channelSelectCombo.setItems(new String[] { " 1 : Ausgang" }); // " 2 : Ausgang", " 3 : Ausgang", "" 4 : Ausgang"" }); //$NON-NLS-1$
						this.channelSelectCombo.select(0);
						this.channelSelectCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0075));
						this.channelSelectCombo.setEditable(false);
						this.channelSelectCombo.setBackground(DataExplorer.COLOR_WHITE);
						this.channelSelectCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "kanalCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								MenuToolBar.this.channels.switchChannel(MenuToolBar.this.channelSelectCombo.getText());
							}
						});
						this.channelSelectCombo.setSize(this.channelSelectSize);
						this.channelSelectComposite.setSize(this.channelSelectSize.x+leadFill+trailFill, this.toolButtonHeight);
						this.channelSelectCombo.setLocation(leadFill, GDE.IS_MAC ? 1 : (this.toolButtonHeight - this.objectSelectSize.y) / 2 - 1);
					}
					channelSelectComboSep.setWidth(this.channelSelectComposite.getSize().x);
					channelSelectComboSep.setControl(this.channelSelectComposite);
				}
				{
					this.prevChannel = new ToolItem(this.dataToolBar, SWT.NONE);
					this.prevChannel.setImage(SWTResourceManager.getImage("gde/resource/ArrowWhiteGreenFieldLeft.gif")); //$NON-NLS-1$
					this.prevChannel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0076));
					this.prevChannel.setEnabled(false);
					this.prevChannel.setHotImage(SWTResourceManager.getImage("gde/resource/ArrowWhiteGreenFieldLefHot.gif")); //$NON-NLS-1$
					this.prevChannel.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "prevChannel.widgetSelected, event=" + evt); //$NON-NLS-1$
							int selectionIndex = MenuToolBar.this.channelSelectCombo.getSelectionIndex();
							if (selectionIndex > 0) MenuToolBar.this.channelSelectCombo.select(selectionIndex - 1);
							if (selectionIndex == 1) MenuToolBar.this.prevChannel.setEnabled(false);
							MenuToolBar.this.nextChannel.setEnabled(true);
							MenuToolBar.this.channels.switchChannel(MenuToolBar.this.channelSelectCombo.getText());
						}
					});
				}
				{
					this.nextChannel = new ToolItem(this.dataToolBar, SWT.NONE);
					this.nextChannel.setImage(SWTResourceManager.getImage("gde/resource/ArrowWhiteGreenFieldRight.gif")); //$NON-NLS-1$
					this.nextChannel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0077));
					this.nextChannel.setEnabled(false);
					this.nextChannel.setHotImage(SWTResourceManager.getImage("gde/resource/ArrowWhiteGreenFieldRightHot.gif")); //$NON-NLS-1$
					this.nextChannel.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "nextChannel.widgetSelected, event=" + evt); //$NON-NLS-1$
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
				{
					ToolItem recordSelectComboSep = new ToolItem(this.dataToolBar, SWT.SEPARATOR);
					{
						this.recordSelectComposite = new Composite(this.dataToolBar, SWT.NONE);
						this.recordSelectComposite.setLayout(null);
						this.recordSelectCombo = new CCombo(this.recordSelectComposite, SWT.BORDER | SWT.LEFT);
						this.recordSelectCombo.setFont(SWTResourceManager.getFont(this.application, GDE.IS_LINUX ? 9 : GDE.IS_MAC ? 12 : 10, SWT.NORMAL));
						this.recordSelectCombo.setItems(new String[] { GDE.STRING_BLANK }); // later "2) Flugaufzeichnung", "3) laden" });
						this.recordSelectCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0078));
						this.recordSelectCombo.setTextLimit(RecordSet.MAX_NAME_LENGTH);
						//this.recordSelectSize.x = SWTResourceManager.getGC(this.recordSelectCombo.getDisplay()).stringExtent("012345678901234567890123456789012345678901234567890".substring(0, RecordSet.MAX_NAME_LENGTH)).x;
						this.recordSelectCombo.setEditable(false);
						this.recordSelectCombo.setBackground(DataExplorer.COLOR_WHITE);
						this.recordSelectCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "recordSelectCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								Channel activeChannel = MenuToolBar.this.channels.getActiveChannel();
								if (activeChannel != null) activeChannel.switchRecordSet(MenuToolBar.this.recordSelectCombo.getText());
							}
						});
						this.recordSelectCombo.addKeyListener(new KeyAdapter() {
							public void keyPressed(KeyEvent evt) {
								log.log(Level.FINEST, "recordSelectCombo.keyPressed, event=" + evt); //$NON-NLS-1$
								if (evt.character == SWT.CR) {
									Channel activeChannel = MenuToolBar.this.channels.getActiveChannel();
									if (activeChannel != null) {
										String oldRecordSetName = activeChannel.getActiveRecordSet().getName();
										String newRecordSetName = MenuToolBar.this.recordSelectCombo.getText();
										log.log(Level.FINE, "newRecordSetName = " + newRecordSetName); //$NON-NLS-1$
										String[] recordSetNames = MenuToolBar.this.recordSelectCombo.getItems();
										for (int i = 0; i < recordSetNames.length; i++) {
											if (recordSetNames[i].equals(oldRecordSetName)) recordSetNames[i] = newRecordSetName;
										}
										//MenuToolBar.this.recordSelectCombo.setEditable(false);
										MenuToolBar.this.recordSelectCombo.setItems(recordSetNames);
										RecordSet recordSet = MenuToolBar.this.channels.getActiveChannel().get(oldRecordSetName);
										recordSet.setName(newRecordSetName);
										recordSet.setUnsaved(RecordSet.UNSAVED_REASON_DATA);
										activeChannel.put(newRecordSetName, recordSet);
										activeChannel.remove(oldRecordSetName);
										activeChannel.getRecordSetNames();
										MenuToolBar.this.channels.getActiveChannel().switchRecordSet(newRecordSetName);
									}
								}
							}
						});
						this.recordSelectCombo.setSize(this.recordSelectSize);
						this.recordSelectComposite.setSize(this.recordSelectSize.x+leadFill+trailFill, this.toolButtonHeight);
						this.recordSelectCombo.setLocation(leadFill, GDE.IS_MAC ? 1 : (this.toolButtonHeight - this.objectSelectSize.y) / 2 - 1);
					}
					recordSelectComboSep.setWidth(this.recordSelectComposite.getSize().x);
					recordSelectComboSep.setControl(this.recordSelectComposite);
				}
				{
					this.prevRecord = new ToolItem(this.dataToolBar, SWT.NONE);
					this.prevRecord.setImage(SWTResourceManager.getImage("gde/resource/ArrowWhiteGreenFieldLeft.gif")); //$NON-NLS-1$
					this.prevRecord.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0079));
					this.prevRecord.setEnabled(false);
					this.prevRecord.setHotImage(SWTResourceManager.getImage("gde/resource/ArrowWhiteGreenFieldLefHot.gif")); //$NON-NLS-1$
					this.prevRecord.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "prevRecord.widgetSelected, event=" + evt); //$NON-NLS-1$
							int selectionIndex = MenuToolBar.this.recordSelectCombo.getSelectionIndex();
							if (selectionIndex > 0) MenuToolBar.this.recordSelectCombo.select(selectionIndex - 1);
							if (selectionIndex == 1) MenuToolBar.this.prevRecord.setEnabled(false);
							MenuToolBar.this.nextRecord.setEnabled(true);
							MenuToolBar.this.channels.getActiveChannel().switchRecordSet(MenuToolBar.this.recordSelectCombo.getText());
						}
					});
				}
				{
					this.nextRecord = new ToolItem(this.dataToolBar, SWT.NONE);
					this.nextRecord.setImage(SWTResourceManager.getImage("gde/resource/ArrowWhiteGreenFieldRight.gif")); //$NON-NLS-1$
					this.nextRecord.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0080));
					this.nextRecord.setEnabled(false);
					this.nextRecord.setHotImage(SWTResourceManager.getImage("gde/resource/ArrowWhiteGreenFieldRightHot.gif")); //$NON-NLS-1$
					this.nextRecord.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "nextRecord.widgetSelected, event=" + evt); //$NON-NLS-1$
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
				{
					this.separator = new ToolItem(this.dataToolBar, SWT.SEPARATOR);
				}
				{
					this.deleteRecord = new ToolItem(this.dataToolBar, SWT.NONE);
					this.deleteRecord.setImage(SWTResourceManager.getImage("gde/resource/Delete.gif")); //$NON-NLS-1$
					this.deleteRecord.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0081));
					this.deleteRecord.setHotImage(SWTResourceManager.getImage("gde/resource/DeleteHot.gif")); //$NON-NLS-1$
					this.deleteRecord.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "deleteRecord.widgetSelected, event=" + evt); //$NON-NLS-1$
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
									log.log(Level.FINE, "deleted " + deleteRecordSetName); //$NON-NLS-1$
									String[] recordSetNames = updateRecordSetSelectCombo();
									if (recordSetNames.length > 0 && recordSetNames[0] != null && recordSetNames[0].length() > 1) {
										activeChannel.switchRecordSet(recordSetNames[0]);
									}
									else {
										// only update viewable
										MenuToolBar.this.application.cleanHeaderAndCommentInGraphicsWindow();
										MenuToolBar.this.application.updateAllTabs(true);
									}
								}
							}
						}
					});
				}
				{
					this.editRecord = new ToolItem(this.dataToolBar, SWT.NONE);
					this.editRecord.setImage(SWTResourceManager.getImage("gde/resource/Edit.gif")); //$NON-NLS-1$
					this.editRecord.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0082));
					this.editRecord.setHotImage(SWTResourceManager.getImage("gde/resource/EditHot.gif")); //$NON-NLS-1$
					this.editRecord.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "editAufnahme.widgetSelected, event=" + evt); //$NON-NLS-1$
							MenuToolBar.this.recordSelectCombo.setEditable(true);
							MenuToolBar.this.recordSelectCombo.setFocus();
							// begin here text can be edited
						}
					});
				}
				this.toolSize = this.dataToolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				this.dataToolBar.setSize(this.toolSize);
				log.log(Level.FINE, "dataToolBar.size = " + this.toolSize); //$NON-NLS-1$
			}
			this.dataCoolItem.setSize(this.toolSize.x, this.toolSize.y);
			//this.dataCoolItem.setPreferredSize(this.size);
			this.dataCoolItem.setMinimumSize(this.toolSize.x, this.toolSize.y);
			this.toolBarSizes.append(this.toolSize.x).append(GDE.STRING_COLON).append(this.toolSize.y).append(GDE.STRING_SEMICOLON);
		}
		
		{ // begin google earth cool item
			this.googleEarthCoolItem = new CoolItem(this.coolBar, SWT.NONE);
			{ // begin file tool bar
				this.googleEarthToolBar = new ToolBar(this.coolBar, SWT.NONE);
				this.googleEarthCoolItem.setControl(this.googleEarthToolBar);
				{
					this.googleEarthToolItem = new ToolItem(this.googleEarthToolBar, SWT.NONE);
					this.googleEarthToolItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0229));
					this.googleEarthToolItem.setImage(SWTResourceManager.getImage("gde/resource/Earth.gif")); //$NON-NLS-1$
					this.googleEarthToolItem.setHotImage(SWTResourceManager.getImage("gde/resource/EarthHot.gif")); //$NON-NLS-1$
					this.googleEarthToolItem.setDisabledImage(SWTResourceManager.getImage("gde/resource/EarthDisabled.gif")); //$NON-NLS-1$
					this.googleEarthToolItem.setEnabled(false);
					this.googleEarthToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "googleEarthToolItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							ApplicationLauncher launcher;
							if (GDE.IS_MAC)
								launcher = new ApplicationLauncher("Google Earth", "Google Earth", GDE.STRING_MAC_APP_BASE_PATH + "Google Earth" + GDE.STRING_MAC_DOT_APP); //$NON-NLS-1$ //$NON-NLS-2$
							else if (GDE.IS_LINUX)
								launcher = new ApplicationLauncher("googleearth", "googleearth", Messages.getString(MessageIds.GDE_MSGT0601)); //$NON-NLS-1$
							else //GDE.IS_WINDOWS
								launcher = new ApplicationLauncher("googleearth.exe", "Google Earth.kmlfile", Messages.getString(MessageIds.GDE_MSGT0600)); //$NON-NLS-1$

							if (launcher.isLaunchable()) {
								String kmlFilePath = MenuToolBar.this.application.getActiveDevice().exportFile(GDE.FILE_ENDING_KML, true);
								List<String> argumentList = new ArrayList<String>();
								argumentList.add(kmlFilePath);
								launcher.execute(argumentList);
							}
						}
					});
				}
				{
					this.googleEarthConfigToolItem = new ToolItem(this.googleEarthToolBar, SWT.NONE);
					this.googleEarthConfigToolItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0281));
					this.googleEarthConfigToolItem.setImage(SWTResourceManager.getImage("gde/resource/EarthConfig.gif")); //$NON-NLS-1$
					this.googleEarthConfigToolItem.setHotImage(SWTResourceManager.getImage("gde/resource/EarthConfigHot.gif")); //$NON-NLS-1$
					this.googleEarthConfigToolItem.setDisabledImage(SWTResourceManager.getImage("gde/resource/EarthConfigDisabled.gif")); //$NON-NLS-1$
					this.googleEarthConfigToolItem.setEnabled(false);
					this.googleEarthConfigToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "googleEarthConfigToolItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							new GoogleEarthCustomizingDialog(application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL).open();
						}
					});
				}
			}
			this.toolSize = this.googleEarthToolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			this.googleEarthCoolItem.setSize(this.toolSize.x, this.toolSize.y);
			//this.googleEarthCoolItem.setPreferredSize(this.size);
			this.googleEarthCoolItem.setMinimumSize(this.toolSize.x, this.toolSize.y);
			this.toolBarSizes.append(this.toolSize.x).append(GDE.STRING_COLON).append(this.toolSize.y).append(GDE.STRING_SEMICOLON);
		} // end google earth cool item

		{ // begin help cool item
			this.helpCoolItem = new CoolItem(this.coolBar, SWT.NONE);
			{ // begin file tool bar
				this.helpToolBar = new ToolBar(this.coolBar, SWT.NONE);
				this.helpCoolItem.setControl(this.helpToolBar);
				{
					this.helpToolItem = new ToolItem(this.helpToolBar, SWT.NONE);
					this.helpToolItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0228));
					this.helpToolItem.setImage(SWTResourceManager.getImage("gde/resource/Question.gif")); //$NON-NLS-1$
					this.helpToolItem.setHotImage(SWTResourceManager.getImage("gde/resource/QuestionHot.gif")); //$NON-NLS-1$
					this.helpToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "helpToolItem.widgetSelected, event=" + evt); //$NON-NLS-1$
								if (MenuToolBar.this.application.getActiveDevice().getDialog() != null && !MenuToolBar.this.application.getActiveDevice().getDialog().isDisposed()) {
									MenuToolBar.this.application.getActiveDevice().getDialog().getDialogShell().notifyListeners(SWT.Help, new Event());
								}
								else {
									for (CTabItem tabItem : MenuToolBar.this.application.getTabFolder().getItems()) {
										if (!tabItem.isDisposed()&& tabItem.getControl().isVisible()) {
											if (tabItem.getControl().isListening(SWT.Help)) {
												tabItem.getControl().notifyListeners(SWT.Help, new Event());
												break;
											}
											else if (tabItem instanceof GraphicsWindow) {
												((GraphicsWindow)tabItem).getGraphicsComposite().notifyListeners(SWT.Help, new Event());
											}
											else if (tabItem.getText().endsWith("Tool")) { //DataVarioTool, LinkVarioTool //$NON-NLS-1$
												if (MenuToolBar.this.application.getActiveDevice() != null && MenuToolBar.this.application.getActiveDevice().isUtilityDeviceTabRequested()) {
													try {
														MenuToolBar.this.application.openHelpDialog(FileUtils.getJarFileNameOfDevice(MenuToolBar.this.application.getActiveDevice().getDeviceConfiguration()), "HelpInfo.html");//$NON-NLS-1$
													}
													catch (Exception e) {
														//ignore
													} 	
												}
											}
										}
									}
								}
						}
					});
				}
			}
			this.toolSize = this.helpToolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			this.helpCoolItem.setSize(this.toolSize.x, this.toolSize.y);
			//this.helpCoolItem.setPreferredSize(this.size);
			this.helpCoolItem.setMinimumSize(this.toolSize.x, this.toolSize.y);
			this.toolBarSizes.append(this.toolSize.x).append(GDE.STRING_COLON).append(this.toolSize.y).append(GDE.STRING_SEMICOLON);
		} // end help cool item
		
		// set the focus controlled to an item which has no slection capability
		this.deviceObjectToolBar.setFocus();
	}

	/**
	 * add record set entry to record set select combo
	 * @param newRecordSetName
	 */
	public void addRecordSetName(String newRecordSetName) {
		final String recordSetKey = newRecordSetName;
		DataExplorer.display.asyncExec(new Runnable() {
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
			DataExplorer.display.asyncExec(new Runnable() {
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
			this.channelSelectCombo.setItems(new String[] { GDE.STRING_EMPTY });
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
			DataExplorer.display.asyncExec(new Runnable() {
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
			this.recordSelectCombo.setText(GDE.STRING_EMPTY);
		}
		updateRecordToolItems();
	}

	/**
	 * updates the netxtRecord , prevRecord tool items
	 */
	public void updateRecordToolItems() {
		if (this.recordSelectCombo.isEnabled()) {
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
	}

	/**
	 * 
	 */
	void doUpdateChannelToolItems() {
		if (this.channelSelectCombo.isEnabled()) {
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
	}

	/**
	 * updates the netxtChannel , prevChannel tool items
	 */
	public void updateChannelToolItems() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			doUpdateChannelToolItems();
		}
		else {
			DataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					doUpdateChannelToolItems();
				}
			});
		}
	}

	/**
	 * this function must only called by application which make secure to choose the right thread
	 * @param isPortOpen
	 */
	public void setPortConnected(final boolean isPortOpen) {
		if (!this.application.isDisposed()) {
			switch (this.iconSet) {
			case 0: // DeviceSerialPort.ICON_SET_OPEN_CLOSE
			default:
				if (isPortOpen) {
					this.portOpenCloseItem.setDisabledImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/PortCloseDisabled.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setHotImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/PortClose.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/PortCloseHot.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0067));
				}
				else {
					this.portOpenCloseItem.setDisabledImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/PortOpenDisabled.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setHotImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/PortOpenHot.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/PortOpen.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0066));
				}
				break;
			case 1: // DeviceSerialPort.ICON_SET_START_STOP
				if (isPortOpen) {
					this.portOpenCloseItem.setDisabledImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/StopGatherDisabled.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setHotImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/StopGatherHot.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/StopGather.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0069));
				}
				else {
					this.portOpenCloseItem.setDisabledImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/StartGatherDisabled.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setHotImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/StartGather.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/StartGatherHot.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0068));
				}
				break;
			case 2: // DeviceSerialPort.ICON_SET_IMPORT_CLOSE
				if (isPortOpen) {
					this.portOpenCloseItem.setDisabledImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/ImportActiveDisabled.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setHotImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/ImportActiveHot.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/ImportActive.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0069));
				}
				else {
					this.portOpenCloseItem.setDisabledImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/ImportDataDisabled.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setHotImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/ImportData.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setImage(SWTResourceManager.getImage("gde/resource/" + this.language + "/ImportDataHot.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.portOpenCloseItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0068));
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

	public void enableChannelActions(boolean enabled) {
		this.prevChannel.setEnabled(enabled);
		this.nextChannel.setEnabled(enabled);
		this.channelSelectCombo.setEnabled(enabled);
		updateChannelSelector();
	}

	public void enableRecordSetActions(boolean enabled) {
		this.prevRecord.setEnabled(enabled);
		this.nextRecord.setEnabled(enabled);
		this.deleteRecord.setEnabled(enabled);
		this.editRecord.setEnabled(enabled);
		this.recordSelectCombo.setEnabled(enabled);
		updateRecordSetSelectCombo();
	}

	/**
	 * enable pan button in zoomed mode
	 * @param enable 
	 */
	public void enablePanButton(boolean enable) {
		this.panItem.setEnabled(enable);
	}
	
	/**
	 * enable or disable tool bar buttons
	 * @param enableLeft
	 * @param enableRight
	 */
	public void enableCutButtons(boolean enableLeft, boolean enableRight) {
		this.cutLeftItem.setEnabled(enableLeft);
		this.cutRightItem.setEnabled(enableRight);
	}

	/**
	 * query the last point of measurement to be displayed
	 * return -1 in case of inactive or all 
	 * @return sizeLastPoints
	 */
	public int getScopeModeLevelValue() {
		int sizeLastPoints = -1;
		try {
			sizeLastPoints = new Integer(MenuToolBar.this.scopePointsCombo.getText().trim());
		}
		catch (NumberFormatException e) {
			// ignore and return -1
		}
		return sizeLastPoints;
	}

	/**
	 * reset the zoom tool bar to default state
	 */
	public void resetZoomToolBar() {
		this.zoomWindowItem.setEnabled(true);
		this.panItem.setEnabled(false);
		this.cutLeftItem.setEnabled(false);
		this.cutRightItem.setEnabled(false);
		this.scopePointsCombo.setEnabled(this.isScopePointsCombo);
		this.scopePointsCombo.select(0);
//		Channel activeChannel = this.channels.getActiveChannel();
//		if (activeChannel != null) {
//			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
//			if (activeRecordSet != null && activeRecordSet.isSyncRequested()) {
//				activeRecordSet.setSyncRequested(true, false);
//			}
//		}
	}
	
	/**
	 * get the coolbar sizes as string
	 * INITIAL_COOLBAR_SIZES = (GDE.IS_WINDOWS == true ? "161:29;136:29;1143:29;145:29;1295:29" : "174:35;146:35;904:35;146:35;1078:35");
	 */
	public String getCoolBarSizes() {
			return this.toolBarSizes.toString();		
	}

	/**
	 * check the object key selected against an eventually existing and ask for replacement
	 * @param actualSelectionIndex
	 * @param newObjectKey
	 * @return actualSelectionIndex
	 */
	public int checkChannelForObjectKeyMissmatch(int actualSelectionIndex, String newObjectKey) {
		Channel activeChannel = MenuToolBar.this.channels.getActiveChannel();
		if (activeChannel != null) {
			String channelObjKey = activeChannel.getObjectKey();

			// check if selected key matches the existing object key or is new for this channel
			if (!newObjectKey.equals(channelObjKey)) { // channel has a key
				int answer = this.application.getActiveRecordSet() == null ? SWT.YES : (MenuToolBar.this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGT0205, new Object[] { channelObjKey, newObjectKey })));
				if (answer == SWT.YES) { //replace existing objectkey in channel
					activeChannel.setObjectKey(newObjectKey);
					String updateFileDescription = activeChannel.getFileDescription();
					if (channelObjKey.length() > 1 && updateFileDescription.contains(channelObjKey)) {
						if (newObjectKey.length() > 1) {
							updateFileDescription = updateFileDescription.substring(0, updateFileDescription.indexOf(channelObjKey)) + newObjectKey
									+ updateFileDescription.substring(updateFileDescription.indexOf(channelObjKey) + channelObjKey.length());
						}
						else { // newObjectKey = ""
							updateFileDescription = updateFileDescription.substring(0, updateFileDescription.indexOf(channelObjKey) - 1)
									+ updateFileDescription.substring(updateFileDescription.indexOf(channelObjKey) + channelObjKey.length());
						}
					}
					else if (newObjectKey.length() > 1) {
						updateFileDescription = updateFileDescription + GDE.STRING_BLANK + newObjectKey;
					}
					activeChannel.setFileDescription(updateFileDescription);
				}
				// do not exchange the object key in the channel/configuration, but keep the selector switch to enable new data load
			}
		}
		return actualSelectionIndex;
	}

	/**
	 * @param actualSelectionIndex
	 * @param newObjectKey
	 */
	public void selectObjectKey(int actualSelectionIndex, String newObjectKey) {
		boolean isContained = false;
		int searchSelectionIndex = 0;
		String[] objectKeys = MenuToolBar.this.objectSelectCombo.getItems();
		for(; searchSelectionIndex < objectKeys.length; ++searchSelectionIndex) {
			if (newObjectKey.equals(objectKeys[searchSelectionIndex])) {
					MenuToolBar.this.objectSelectCombo.select(searchSelectionIndex);
					MenuToolBar.this.settings.setObjectList(MenuToolBar.this.settings.getObjectList(), searchSelectionIndex);
					isContained = true;
					break;
			}
		}
		if (!isContained && searchSelectionIndex > actualSelectionIndex) { // channel contains a key which does not exist in the list
			Vector<String> tmpObjects = new Vector<String>();
			for (String tmpObject : MenuToolBar.this.settings.getObjectList()) {
				tmpObjects.add(tmpObject);
			}
			tmpObjects.add(newObjectKey);
			MenuToolBar.this.settings.setObjectList(tmpObjects.toArray(new String[1]), newObjectKey);
			MenuToolBar.this.objectSelectCombo.setItems(MenuToolBar.this.settings.getObjectList());
			MenuToolBar.this.objectSelectCombo.select(MenuToolBar.this.settings.getActiveObjectIndex());
		}
		this.isObjectoriented = this.objectSelectCombo.getSelectionIndex() > 0;
		this.application.setObjectDescriptionTabVisible(this.isObjectoriented);
		this.application.updateObjectDescriptionWindow();
	}

	public void selectObjectKeyDeviceOriented() {
		this.objectSelectCombo.select(0);
		this.isObjectoriented = this.objectSelectCombo.getSelectionIndex() > 0;
	}

	/**
	 * @return the isObjectoriented
	 */
	public boolean isObjectoriented() {
		return this.isObjectoriented;
	}

	/**
	 * @return the activeObjectKey
	 */
	public String getActiveObjectKey() {
		return this.objectSelectCombo.getText();
	}
	
	/**
	 * update the object select combo switch to the channel related settings
	 */
	public void updateObjectSelector() {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null && activeChannel.size() > 0 && !activeChannel.getObjectKey().equals(GDE.STRING_EMPTY)) {
			this.selectObjectKey(this.objectSelectCombo.getSelectionIndex(), activeChannel.getObjectKey());
		}
		else if (activeChannel != null && activeChannel.size() == 0) { // startup
			this.objectSelectCombo.select(this.settings.getActiveObjectIndex());
		}
		else {
			this.selectObjectKeyDeviceOriented();
		}
		this.isObjectoriented = this.objectSelectCombo.getSelectionIndex() > 0;
	}
	
	/**
	 * set a new object key list from outside (object key scanner)
	 */
	public void setObjectList(String[] newObjectKeyList, String newObjectKey) {
		this.settings.setObjectList(newObjectKeyList, newObjectKey);
		this.objectSelectCombo.setItems(this.settings.getObjectList());
		this.objectSelectCombo.select(this.settings.getActiveObjectIndex());
		this.objectSelectCombo.setVisibleItemCount(this.objectSelectCombo.getItemCount()+1);
		this.updateObjectSelector();
	}
	
	public String[] getObjectKeyList() {
		return this.objectSelectCombo.getItems();
	}

	/**
	 * enable or disable the ScopePointsCombo
	 * @param enabled true will enable the scopePointCombo
	 */
	public void enableScopePointsCombo(boolean enabled) {
		this.isScopePointsCombo = enabled;
		this.scopePointsCombo.setEnabled(enabled);
	}
	
	/**
	 * switch enable the Google Earth tool item, if enabled KML export with Google Earth launch could be executed
	 * this functions queries the actual device with actual record set for GPS data
	 */
	public void updateGoogleEarthToolItem() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.googleEarthToolItem.setEnabled(this.application.getActiveDevice().isActualRecordSetWithGpsData());
			this.googleEarthConfigToolItem.setEnabled(this.application.getActiveDevice().isActualRecordSetWithGpsData());
		}
		else {
			DataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					MenuToolBar.this.googleEarthToolItem.setEnabled(MenuToolBar.this.application.getActiveDevice().isActualRecordSetWithGpsData());
					MenuToolBar.this.googleEarthConfigToolItem.setEnabled(MenuToolBar.this.application.getActiveDevice().isActualRecordSetWithGpsData());
				}
			});
		}
	}
}
