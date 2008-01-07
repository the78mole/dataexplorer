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

import java.util.Arrays;
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
import osde.device.DeviceSerialPort;
import osde.device.IDevice;
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
/**
 * Graphical menu tool bar class
 * (future items are: scaling icons, ...)
 * @author Winfried Brügmann
 */
public class MenuToolBar {
	private Logger												log	= Logger.getLogger(this.getClass().getName());
	
	private Composite recordSelectComposite;
	private Composite channelSelectComposite;
	private CoolItem dataCoolItem;
	private ToolBar portToolBar;
	private CoolItem portCoolItem;
	private ToolBar deviceToolBar;
	private CoolItem deviceCoolItem;

	private CoolBar												coolBar;
	private CoolItem											menuCoolItem;
	private ToolItem											toolBoxToolItem;
	private ToolItem											nextDeviceToolItem;
	private ToolItem											prevDeviceToolItem;
	private ToolItem											deviceSelectToolItem;
	private ToolItem											saveAsToolItem;
	private ToolItem											saveToolItem;
	private ToolItem											openToolItem;
	private ToolItem											newToolItem;
	private ToolBar												fileToolBar;
	private ToolItem											fitIntoItem;
	private ToolItem											resizeItem;
	private ToolItem											zoomWindowItem;
	private ToolBar												zoomToolBar;
	private CoolItem											zoomCoolItem;
	
	private ToolItem portOpenCloseItem;
	private Composite											dataBarComposite;
	private ToolItem											nextChannel, prevChannel, prevRecord, nextRecord, deleteRecord, editRecord;
	private CCombo												channelSelectCombo, recordSelectCombo;
	private ToolBar												channelToolBar, recordToolBar;
	@SuppressWarnings("unused")
	private ToolItem											separator;

	private final OpenSerialDataExplorer	application;
	private final Channels								channels;



	public MenuToolBar(OpenSerialDataExplorer parent, CoolBar menuCoolBar) {
		this.application = parent;
		this.coolBar = menuCoolBar;
		this.channels = Channels.getInstance();
	}

	public void init() {
		coolBar = new CoolBar(this.application, SWT.NONE);
		SWTResourceManager.registerResourceUser(coolBar);
		coolBar.setSize(796, 100);
		create();
	}

	public void create() {
		{ // begin file cool item
			menuCoolItem = new CoolItem(coolBar, SWT.NONE);
			menuCoolItem.setSize(116, 29);
			menuCoolItem.setPreferredSize(new org.eclipse.swt.graphics.Point(116, 29));
			menuCoolItem.setMinimumSize(new org.eclipse.swt.graphics.Point(116, 29));
			{ // begin file tool bar
				fileToolBar = new ToolBar(coolBar, SWT.NONE);
				menuCoolItem.setControl(fileToolBar);
				fileToolBar.setSize(116, 29);
				{
					newToolItem = new ToolItem(fileToolBar, SWT.PUSH);
					newToolItem.setToolTipText("Löscht die aktuellen Aufzeichnungen und legt einen leeren Datensatz an");
					newToolItem.setImage(SWTResourceManager.getImage("osde/resource/New.gif"));
					newToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/NewHot.gif"));
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
					openToolItem = new ToolItem(fileToolBar, SWT.NONE);
					openToolItem.setToolTipText("Verwirft den aktuellen Datensatz und  öffnet eine neue Datei mit neuem Inhalt");
					openToolItem.setImage(SWTResourceManager.getImage("osde/resource/Open.gif"));
					openToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/OpenHot.gif"));
					openToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("openToolItem.widgetSelected, event=" + evt);
							//TODO add your code for openToolItem.widgetSelected
							application.openMessageDialog("Entschuldigung, ein Datenformat ist noch nicht implementiert! ");
						}
					});
				}
				{
					saveToolItem = new ToolItem(fileToolBar, SWT.NONE);
					saveToolItem.setToolTipText("Sichert die aktuellen Aufzeichnungen in eine Datei");
					saveToolItem.setImage(SWTResourceManager.getImage("osde/resource/Save.gif"));
					saveToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/SaveHot.gif"));
					saveToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("saveToolItem.widgetSelected, event=" + evt);
							//TODO add your code for saveToolItem.widgetSelected
							application.openMessageDialog("Entschuldigung, ein Datenformat ist noch nicht implementiert! ");
						}
					});
				}
				{
					saveAsToolItem = new ToolItem(fileToolBar, SWT.NONE);
					saveAsToolItem.setToolTipText("Sichert die aktuellen Aufzeichnungen unter einem anzugebenden Namen");
					saveAsToolItem.setImage(SWTResourceManager.getImage("osde/resource/SaveAs.gif"));
					saveAsToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/SaveAsHot.gif"));
					saveAsToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("saveAsToolItem.widgetSelected, event=" + evt);
							//TODO add your code for saveAsToolItem.widgetSelected
							application.openMessageDialog("Entschuldigung, ein Datenformat ist noch nicht implementiert! ");
						}
					});
				}
			} // end file tool bar
		} // end file cool item
		
		{ // begin device cool item
			deviceCoolItem = new CoolItem(coolBar, SWT.NONE);
			deviceCoolItem.setSize(120, 29);
			deviceCoolItem.setPreferredSize(new org.eclipse.swt.graphics.Point(120, 29));
			deviceCoolItem.setMinimumSize(new org.eclipse.swt.graphics.Point(120, 29));
			{  // begin device tool bar
				deviceToolBar = new ToolBar(coolBar, SWT.NONE);
				deviceCoolItem.setControl(deviceToolBar);
				deviceToolBar.setSize(120, 29);
				{
					deviceSelectToolItem = new ToolItem(deviceToolBar, SWT.NONE);
					deviceSelectToolItem.setToolTipText("Geräteauswahl mit Einstellungen");
					deviceSelectToolItem.setImage(SWTResourceManager.getImage("osde/resource/DeviceSelection.gif"));
					deviceSelectToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/DeviceSelectionHot.gif"));
					deviceSelectToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("deviceToolItem.widgetSelected, event=" + evt);
							DeviceSelectionDialog deviceSelect = application.getDeviceSelectionDialog();
							if (deviceSelect.checkDataSaved()) {
								application.setActiveDevice(deviceSelect.open());
							}
						}
					});
				}
				{
					prevDeviceToolItem = new ToolItem(deviceToolBar, SWT.NONE);
					prevDeviceToolItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLeft.gif"));
					prevDeviceToolItem.setToolTipText("Schalte zum verhergehenden Gerät");
					prevDeviceToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLefHot.gif"));
					prevDeviceToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("prevDeviceToolItem.widgetSelected, event=" + evt);
							// allow device switch only if port not connected
							if (application.getActiveDevice() == null || application.getActiveDevice().getSerialPort() != null && !application.getActiveDevice().getSerialPort().isConnected()) {
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
					nextDeviceToolItem = new ToolItem(deviceToolBar, SWT.NONE);
					nextDeviceToolItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRight.gif"));
					nextDeviceToolItem.setToolTipText("Schalte zum nachfolgenden Gerät");
					nextDeviceToolItem.setText("");
					//nextDeviceToolItem.setWidth(20);
					nextDeviceToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRightHot.gif"));
					nextDeviceToolItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("nextDeviceToolItem.widgetSelected, event=" + evt);
							// allow device switch only if port not connected
							if (application.getActiveDevice() == null || application.getActiveDevice().getSerialPort() != null && !application.getActiveDevice().getSerialPort().isConnected()) {
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
				{
					toolBoxToolItem = new ToolItem(deviceToolBar, SWT.NONE);
					toolBoxToolItem.setImage(SWTResourceManager.getImage("osde/resource/ToolBox.gif"));
					toolBoxToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif"));
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
			}  // end device tool bar
		} // end device cool item
		{ // begin zoom cool item
			zoomCoolItem = new CoolItem(coolBar, SWT.NONE);
			zoomCoolItem.setSize(90, 29);
			zoomCoolItem.setPreferredSize(new org.eclipse.swt.graphics.Point(90, 29));
			zoomCoolItem.setMinimumSize(new org.eclipse.swt.graphics.Point(90, 29));
			{ // begin zoom tool bar
				zoomToolBar = new ToolBar(coolBar, SWT.NONE);
				zoomCoolItem.setControl(zoomToolBar);
				zoomToolBar.setSize(90, 29);
				{
					zoomWindowItem = new ToolItem(zoomToolBar, SWT.NONE);
					zoomWindowItem.setImage(SWTResourceManager.getImage("osde/resource/Zoom.gif"));
					zoomWindowItem.setHotImage(SWTResourceManager.getImage("osde/resource/ZoomHot.gif"));
					zoomWindowItem.setToolTipText("Ausschnitt vergrößern");
					zoomWindowItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							if(log.isLoggable(Level.FINEST)) log.finest("zoomWindowItem.widgetSelected, event="+evt);
							application.setZoomMode(true);
						}
					});
				}
				{
					resizeItem = new ToolItem(zoomToolBar, SWT.NONE);
					resizeItem.setImage(SWTResourceManager.getImage("osde/resource/Resize.gif"));
					resizeItem.setHotImage(SWTResourceManager.getImage("osde/resource/ResizeHot.gif"));
					resizeItem.setToolTipText("Größe verändern");
					resizeItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							if(log.isLoggable(Level.FINEST)) log.finest("resizeItem.widgetSelected, event="+evt);
							//TODO add your code for resizeItem.widgetSelected
							application.openMessageDialog("Entschuldigung, diese Methode ist noch nicht implementiert! ");
						}
					});
				}
				{
					fitIntoItem = new ToolItem(zoomToolBar, SWT.NONE);
					fitIntoItem.setImage(SWTResourceManager.getImage("osde/resource/Expand.gif"));
					fitIntoItem.setHotImage(SWTResourceManager.getImage("osde/resource/ExpandHot.gif"));
					fitIntoItem.setToolTipText("Auf Ursprungsgröße einpassen");
					fitIntoItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							if(log.isLoggable(Level.FINEST)) log.finest("fitIntoItem.widgetSelected, event="+evt);
							application.setZoomMode(false);
							RecordSet recordSet = channels.getActiveChannel().getActiveRecordSet();
							if (recordSet!= null) recordSet.setZoomLevel(0);
							application.updateGraphicsWindow();
						}
					});
				}
			}  // end zoom tool bar
		} // end zoom cool item
		
		{ // begin port cool item
			portCoolItem = new CoolItem(coolBar, SWT.NONE);
			portCoolItem.setSize(129, 29);
			portCoolItem.setPreferredSize(new org.eclipse.swt.graphics.Point(129, 29));
			portCoolItem.setMinimumSize(new org.eclipse.swt.graphics.Point(129, 29));
			{
				portToolBar = new ToolBar(coolBar, SWT.NONE);
				portCoolItem.setControl(portToolBar);
				portToolBar.setSize(129, 29);
				{
					portOpenCloseItem = new ToolItem(portToolBar, SWT.NONE);
					portOpenCloseItem.setToolTipText("Seriellen Port Öffnen, um eine Datenaufnahme zu ermöglichen");
					portOpenCloseItem.setImage(SWTResourceManager.getImage("osde/resource/PortOpen.gif"));
					portOpenCloseItem.setDisabledImage(SWTResourceManager.getImage("osde/resource/PortOpenDisabled.gif"));
					portOpenCloseItem.setHotImage(SWTResourceManager.getImage("osde/resource/PortOpenHot.gif"));
					portOpenCloseItem.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("portOpenCloseItem.widgetSelected, event=" + evt);
							openCloseSerialPort();
						}
					});
				}
			}
		} // end port cool item
		
		{ // begin data cool item (channel select, record select)
			dataCoolItem = new CoolItem(coolBar, SWT.NONE);
			dataCoolItem.setSize(551, 29);
			dataCoolItem.setPreferredSize(new org.eclipse.swt.graphics.Point(551, 29));
			dataCoolItem.setMinimumSize(new org.eclipse.swt.graphics.Point(551, 29));
			{
				dataBarComposite = new Composite(coolBar, SWT.NONE);
				Point connectSize = dataBarComposite.computeSize(551, 29);
				Point connectCoolSize = dataBarComposite.computeSize(connectSize.x, connectSize.y);
				dataCoolItem.setSize(connectCoolSize);
				dataCoolItem.setMinimumSize(connectCoolSize);
				dataCoolItem.setPreferredSize(connectCoolSize);
				dataCoolItem.setControl(dataBarComposite);
				RowLayout composite1Layout1 = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				dataBarComposite.setLayout(composite1Layout1);
				{
					RowData composite1LData = new RowData();
					composite1LData.width = 104;
					composite1LData.height = 23;
					channelSelectComposite = new Composite(dataBarComposite, SWT.NONE);
					RowLayout composite1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					channelSelectComposite.setLayout(composite1Layout);
					channelSelectComposite.setLayoutData(composite1LData);
					{
						channelSelectCombo = new CCombo(channelSelectComposite, SWT.FLAT | SWT.BORDER);
						channelSelectCombo.setItems(new String[] { "K1: Kanal 1" }); // "K2: Kanal 2", "K3: Kanal 3", "K4: Kanal 4" });
						channelSelectCombo.select(0); // kanalCombo.setText("K1: Kanal 1");
						channelSelectCombo.setToolTipText("Wählen Sie einen Kanal aus der angezeigt werden soll");
						channelSelectCombo.setEditable(false);
						channelSelectCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
						channelSelectCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("kanalCombo.widgetSelected, event=" + evt);
								channels.switchChannel(channelSelectCombo.getText());
							}
						});
					}
				}

				{
					channelToolBar = new ToolBar(dataBarComposite, SWT.FLAT);
					{
						prevChannel = new ToolItem(channelToolBar, SWT.NONE);
						prevChannel.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLeft.gif"));
						prevChannel.setToolTipText("Kanal zurück");
						prevChannel.setEnabled(false);
						prevChannel.setHotImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLefHot.gif"));
						prevChannel.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("prevChannel.widgetSelected, event=" + evt);
								int selectionIndex = channelSelectCombo.getSelectionIndex();
								if (selectionIndex > 0) channelSelectCombo.select(selectionIndex - 1);
								if (selectionIndex == 1) prevChannel.setEnabled(false);
								selectionIndex = channelSelectCombo.getSelectionIndex();
								nextChannel.setEnabled(true);
								channels.switchChannel(channelSelectCombo.getText());
							}
						});
					}
					{
						nextChannel = new ToolItem(channelToolBar, SWT.NONE);
						nextChannel.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRight.gif"));
						nextChannel.setToolTipText("Kanal vor");
						nextChannel.setEnabled(false);
						nextChannel.setHotImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRightHot.gif"));
						nextChannel.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("nextChannel.widgetSelected, event=" + evt);
								int selectionIndex = channelSelectCombo.getSelectionIndex();
								int maxIndex = channelSelectCombo.getItemCount() - 1;
								if (maxIndex <= 0) {
									nextChannel.setEnabled(false);
									prevChannel.setEnabled(false);
								}
								else {
									if (selectionIndex < maxIndex) channelSelectCombo.select(selectionIndex + 1);
									if (selectionIndex == maxIndex - 1) nextChannel.setEnabled(false);
									prevChannel.setEnabled(true);
								}
								channels.switchChannel(channelSelectCombo.getText());
							}
						});
					}
				}
				{
					RowData composite2LData = new RowData();
					composite2LData.width = 250;
					composite2LData.height = 23;
					recordSelectComposite = new Composite(dataBarComposite, SWT.NONE);
					RowLayout composite2Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					recordSelectComposite.setLayout(composite2Layout);
					recordSelectComposite.setLayoutData(composite2LData);
					{
						recordSelectCombo = new CCombo(recordSelectComposite, SWT.FLAT | SWT.BORDER);
						FormLayout aufnahmeComboLayout = new FormLayout();
						recordSelectCombo.setLayout(aufnahmeComboLayout);
						recordSelectCombo.setItems(new String[] { " " }); // "2) Flugaufzeichnung", "3) laden" });
						recordSelectCombo.setToolTipText("Wählen Sie einen Datensatz aus, der angezeigt werden soll");
						recordSelectCombo.setTextLimit(30);
						RowData aufnahmeComboLData = new RowData();
						aufnahmeComboLData.width = 240;
						aufnahmeComboLData.height = 18;
						recordSelectCombo.setLayoutData(aufnahmeComboLData);
						recordSelectCombo.setEditable(false);
						recordSelectCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
						recordSelectCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("recordSelectCombo.widgetSelected, event=" + evt);
								channels.getActiveChannel().getActiveRecordSet().switchRecordSet(recordSelectCombo.getText());
							}
						});
						recordSelectCombo.addKeyListener(new KeyAdapter() {
							public void keyPressed(KeyEvent evt) {
								log.finest("recordSelectCombo.keyPressed, event=" + evt);
								if (evt.character == SWT.CR) {
									Channel activeChannel = channels.getActiveChannel();
									String oldRecordSetName = activeChannel.getActiveRecordSet().getName();
									String newRecordSetName = recordSelectCombo.getText();
									log.fine("newRecordSetName = " + newRecordSetName);
									String[] recordSetNames = recordSelectCombo.getItems();
									for (int i = 0; i < recordSetNames.length; i++) {
										if (recordSetNames[i].equals(oldRecordSetName)) recordSetNames[i] = newRecordSetName;
									}
									recordSelectCombo.setEditable(false);
									recordSelectCombo.setItems(recordSetNames);
									RecordSet recordSet = channels.getActiveChannel().get(oldRecordSetName);
									recordSet.setName(newRecordSetName);
									activeChannel.put(newRecordSetName, recordSet);
									activeChannel.remove(oldRecordSetName);
									activeChannel.getRecordSetNames();
									channels.getActiveChannel().getActiveRecordSet().switchRecordSet(newRecordSetName);
								}
							}
						});
					}
				}
				{
					recordToolBar = new ToolBar(dataBarComposite, SWT.FLAT);
					{
						prevRecord = new ToolItem(recordToolBar, SWT.NONE);
						prevRecord.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLeft.gif"));
						prevRecord.setToolTipText("vorhergehender Datensatz");
						prevRecord.setEnabled(false);
						prevRecord.setHotImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLefHot.gif"));
						prevRecord.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("prevRecord.widgetSelected, event=" + evt);
								int selectionIndex = recordSelectCombo.getSelectionIndex();
								if (selectionIndex > 0) recordSelectCombo.select(selectionIndex - 1);
								if (selectionIndex == 1) prevRecord.setEnabled(false);
								nextRecord.setEnabled(true);
								channels.getActiveChannel().getActiveRecordSet().switchRecordSet(recordSelectCombo.getText());
							}
						});
					}
					{
						nextRecord = new ToolItem(recordToolBar, SWT.NONE);
						nextRecord.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRight.gif"));
						nextRecord.setToolTipText("nächster Datensatz");
						nextRecord.setEnabled(false);
						nextRecord.setHotImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRightHot.gif"));
						nextRecord.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("nextRecord.widgetSelected, event=" + evt);
								int selectionIndex = recordSelectCombo.getSelectionIndex();
								int maxIndex = recordSelectCombo.getItemCount() - 1;
								if (maxIndex <= 0) {
									nextRecord.setEnabled(false);
									prevRecord.setEnabled(false);
								}
								else {
									if (selectionIndex < maxIndex) recordSelectCombo.select(selectionIndex + 1);
									if (selectionIndex == maxIndex - 1) nextRecord.setEnabled(false);
									prevRecord.setEnabled(true);
								}
								channels.getActiveChannel().getActiveRecordSet().switchRecordSet(recordSelectCombo.getText());
							}
						});
					}
				}
				{
					separator = new ToolItem(recordToolBar, SWT.SEPARATOR);
				}
				{
					deleteRecord = new ToolItem(recordToolBar, SWT.NONE);
					deleteRecord.setImage(SWTResourceManager.getImage("osde/resource/DeleteHot.gif"));
					deleteRecord.setToolTipText("löscht den aktiven Datensatz");
					deleteRecord.setHotImage(SWTResourceManager.getImage("osde/resource/DeleteHot.gif"));
					deleteRecord.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("deleteAufnahme.widgetSelected, event=" + evt);
							String deleteRecordSetName = channels.getActiveChannel().getActiveRecordSet().getName();
							channels.getActiveChannel().get(deleteRecordSetName).clear();
							channels.getActiveChannel().remove(deleteRecordSetName);
							log.fine("deleted " + deleteRecordSetName);
							updateRecordSetSelectCombo();
						}
					});
				}
				{
					editRecord = new ToolItem(recordToolBar, SWT.NONE);
					editRecord.setImage(SWTResourceManager.getImage("osde/resource/EditHot.gif"));
					editRecord.setToolTipText("umbenennen des aktiven Datensatznamen");
					editRecord.setHotImage(SWTResourceManager.getImage("osde/resource/EditHot.gif"));
					editRecord.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("editAufnahme.widgetSelected, event=" + evt);
							recordSelectCombo.setEditable(true);
							recordSelectCombo.setFocus();
							// begin here text can be edited
						}
					});
				}
			}
		} // end record cool item
	}
	
	/**
	 * method toggle open close serial port
	 */
	private void openCloseSerialPort() {
		IDevice device = application.getActiveDevice();
		DeviceSerialPort serialPort = device.getSerialPort();
		if (serialPort != null) {
			if (!serialPort.isConnected()) {
				try {
					serialPort.open();
				}
				catch (Exception e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					application.openMessageDialog("Der serielle Port kann nicht geöffnet werden -> " + e.getClass().getSimpleName() + " : " + e.getMessage());
				}
			}
			else {
				serialPort.close();
			}
		}
	}

	/**
	 * add record set entry to record set select combo
	 */
	public void addRecordSetName(String newRecordSetName) {
		final String recordSetKey = newRecordSetName;
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				Vector<String> newRecordSetItems = new Vector<String>(recordSelectCombo.getItems().length);
				String[] recordSetNames = recordSelectCombo.getItems();
				for (int i = 0; i < recordSetNames.length; i++) {
					if (recordSetNames[i].length() > 3) newRecordSetItems.add(recordSetNames[i]);
				}
				newRecordSetItems.add(recordSetKey);
				recordSelectCombo.setItems(newRecordSetItems.toArray(new String[1]));
				updateChannelToolItems();
			}
		});

	}

	/**
	 * updates the channel select combo according the active channel
	 */
	public synchronized void updateChannelSelector() {
		if (channels.size() > 0) {
			String[] channelNames = new String[channels.size()];
			for (int i = 0; i < channelNames.length; i++) {
				channelNames[i] = "K" + (i + 1) + ": Kanal " + (i + 1);
			}
			channelSelectCombo.setItems(channelNames); //new String[] { "K1: Kanal 1" }); // "K2: Kanal 2", "K3: Kanal 3", "K4: Kanal 4" });
		}
		channelSelectCombo.select(0); // kanalCombo.setText("K1: Kanal 1");
		updateChannelToolItems();
		application.updateGraphicsWindow();
		application.updateDataTable();
	}

	/**
	 * updates the record set select combo according the active record set
	 */
	public String[] updateRecordSetSelectCombo() {
		String[] recordSetNames = channels.getActiveChannel().getRecordSetNames();
		Arrays.sort(recordSetNames);
		if (recordSetNames != null && recordSetNames[0] != null) {
			recordSelectCombo.setItems(recordSetNames); //new String[] { "1) Datensatz" }); // "2) Flugaufzeichnung", "3) laden" });
			recordSelectCombo.select(0); // aufnahmeCombo.setText("1) Datensatz");
		}
		else {
			recordSelectCombo.setItems(new String[0]);
			recordSelectCombo.setText("");
		}
		updateRecordToolItems();
		application.updateGraphicsWindow();

		return recordSetNames;
	}

	/**
	 * updates the netxtRecord , prevRecord tool items
	 */
	public void updateRecordToolItems() {
		int numberRecords = channels.getActiveChannel().getRecordSetNames().length;
		if (numberRecords <= 1) {
			nextRecord.setEnabled(false);
			prevRecord.setEnabled(false);
		}
		else {
			int index = recordSelectCombo.getSelectionIndex();
			int maxIndex = recordSelectCombo.getItemCount() - 1;
			if (numberRecords == 2 && index == 0) {
				nextRecord.setEnabled(true);
				prevRecord.setEnabled(false);
			}
			else if (numberRecords == 2 && index == 1) {
				nextRecord.setEnabled(false);
				prevRecord.setEnabled(true);
			}
			if (numberRecords >= 2 && index == 0) {
				nextRecord.setEnabled(true);
				prevRecord.setEnabled(false);
			}
			else if (numberRecords >= 2 && index == maxIndex) {
				nextRecord.setEnabled(false);
				prevRecord.setEnabled(true);
			}
			else {
				nextRecord.setEnabled(true);
				prevRecord.setEnabled(true);
			}
		}
	}

	/**
	 * updates the netxtChannel , prevChannel tool items
	 */
	public void updateChannelToolItems() {
		int numberChannels = channels.size();
		if (numberChannels <= 1) {
			nextChannel.setEnabled(false);
			prevChannel.setEnabled(false);
		}
		else {
			int index = channelSelectCombo.getSelectionIndex();
			int maxIndex = channelSelectCombo.getItemCount() - 1;
			if (numberChannels == 2 && index == 0) {
				nextChannel.setEnabled(true);
				prevChannel.setEnabled(false);
			}
			else if (numberChannels == 2 && index == 1) {
				nextChannel.setEnabled(false);
				prevChannel.setEnabled(true);
			}
			if (numberChannels >= 2 && index == 0) {
				nextChannel.setEnabled(true);
				prevChannel.setEnabled(false);
			}
			else if (numberChannels >= 2 && index == maxIndex) {
				nextChannel.setEnabled(false);
				prevChannel.setEnabled(true);
			}
			else {
				nextChannel.setEnabled(true);
				prevChannel.setEnabled(true);
			}
		}
	}

	public void setPortConnected(boolean isOpenStatus) {
		if (isOpenStatus) {
			portOpenCloseItem.setDisabledImage(SWTResourceManager.getImage("osde/resource/PortCloseDisabled.gif"));
			portOpenCloseItem.setHotImage(SWTResourceManager.getImage("osde/resource/PortClose.gif"));
			portOpenCloseItem.setImage(SWTResourceManager.getImage("osde/resource/PortCloseHot.gif"));
			//portOpenCloseLabel.setText("Port schliessen");
		}
		else {
			if (!application.isDisposed()) {
				portOpenCloseItem.setDisabledImage(SWTResourceManager.getImage("osde/resource/PortOpenDisabled.gif"));
				portOpenCloseItem.setHotImage(SWTResourceManager.getImage("osde/resource/PortOpenHot.gif"));
				portOpenCloseItem.setImage(SWTResourceManager.getImage("osde/resource/PortOpen.gif"));
				//portOpenCloseLabel.setText("Port öffnen         ");
			}
		}
	}

	public CCombo getChannelSelectCombo() {
		return channelSelectCombo;
	}

	public CCombo getRecordSelectCombo() {
		return recordSelectCombo;
	}

}
