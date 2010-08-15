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
    
    Copyright (c) 2008,2009,2010 Winfried Bruegmann
****************************************************************************************/
package gde.ui.dialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Date;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.xml.sax.SAXParseException;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.NotSupportedException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.serial.DeviceSerialPort;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;

/**
 * Dialog class to select the device to be used
 * @author Winfried Brügmann
 */
public class DeviceSelectionDialog extends org.eclipse.swt.widgets.Dialog {
	final static Logger										log							= Logger.getLogger(DeviceSelectionDialog.class.getName());

	Shell																	dialogShell;
	CTabItem															cTabItem1;
	Composite															composite2;
	Label																	deviceText;
	TableColumn														tableColumn1, tableColumn2, tableColumn3;
	Table																	deviceTable;
	Group																	deviceGroup;
	Composite															composite1;
	CTabItem															auswahlTabItem;
	Button																closeButton;
	Button																rtsCheckBox;
	Button																dtrCheckBox;
	Label																	flowControlSelectLabel;
	Label																	dataBitsSelectLabel;
	Label																	paritySelectLabel;
	Label																	stopBitsSelectLabel;
	Label																	baudeSelectLabel;
	Label																	rtsDescription;
	Label																	dtrDescription;
	Label																	parityDescription;
	Label																	stopbitsDescription;
	Label																	dataBitsDescription;
	Label																	flowcontrolDescription;
	Label																	baudeDescription;
	Group																	portSettingsGroup;
	CCombo																portSelectCombo;
	Label																	portDescription;
	Group																	serialPortSelectionGroup;
	Button																voltagePerCellButton;
	Button																analogTabButton;
	Button																digitalTabButton;
	Button																tableTabButton;
	Group																	desktopTabsGroup;
	Group																	deviceSelectionGroup;
	Button																openToolBoxCheck;
	Button																openPortCheck;
	Label																	internetLinkText;
	Label																	deviceTypeText;
	Label																	manufacturerName;
	Label																	internetLinkDescription;
	Label																	deviceTypeDescription;
	Label																	deviceNameDescription;
	Label																	manufacturerDescription;
	Canvas																deviceCanvas;
	Label																	deviceGroupText;
	Slider																deviceSlider;
	CCombo																deviceSelectCombo;
	CTabFolder														settingsTabFolder;

	TreeMap<String, DeviceConfiguration>	deviceConfigurations;
	Vector<String>												activeDevices;
	final DataExplorer					application;
	final Settings												settings;
	String																activeDeviceName;
	DeviceConfiguration										selectedActiveDeviceConfig;
	Thread																listPortsThread;
	Vector<String>												availablePorts = new Vector<String>();
	boolean																isUpdateSerialPorts = true;


	public DeviceSelectionDialog(Shell parent, int style, final DataExplorer currentApplication) {
		super(parent, style);
		this.application = currentApplication;
		this.settings = Settings.getInstance();
		this.activeDeviceName = this.settings.getActiveDevice();

		try {
			initialize();
		}
		catch (FileNotFoundException e) {
			DeviceSelectionDialog.log.log(Level.SEVERE, e.getMessage(), e);
			currentApplication.openMessageDialog(this.dialogShell, Messages.getString(MessageIds.GDE_MSGE0007) + e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + e.getMessage());
		}
	}

	/**
	 * goes through the existing INI files and set active flagged devices into active devices list
	 * @throws FileNotFoundException 
	 */
	public void initialize() throws FileNotFoundException {

		File file = new File(this.settings.getDevicesPath());
		if (!file.exists()) throw new FileNotFoundException(this.settings.getDevicesPath());
		String[] files = file.list();
		DeviceConfiguration devConfig;
		this.deviceConfigurations = new TreeMap<String, DeviceConfiguration>(String.CASE_INSENSITIVE_ORDER);
		this.activeDevices = new Vector<String>(2, 1);

		//wait until schema is setup
		while (this.settings.isXsdThreadAlive()) {
			try {
				log.log(Level.INFO, "waiting for XSD thread");
				Thread.sleep(5);
			}
			catch (InterruptedException e) {
				//ignore
			}
		}
		
		//long startTime = new Date().getTime();
		for (int i = 0; files != null && i < files.length; i++) {
			try {
				// loop through all device properties XML and check if device used
				if (files[i].endsWith(".xml")) {
					String deviceKey = files[i].substring(0, files[i].length() - 4);
					devConfig = new DeviceConfiguration(this.settings.getDevicesPath() + GDE.FILE_SEPARATOR_UNIX + files[i]);
					if (devConfig.getName().equals(this.activeDeviceName) && devConfig.isUsed()) { // define the active device after re-start
						this.selectedActiveDeviceConfig = devConfig;
					}
					// add the active once into the active device vector
					if (devConfig.isUsed()) this.activeDevices.add(devConfig.getName());

					// store all device configurations in a map					
					String keyString;
					if (devConfig.getName() != null)
						keyString = devConfig.getName();
					else {
						devConfig.setName(deviceKey);
						keyString = deviceKey;
					}
					DeviceSelectionDialog.log.log(Level.FINE, deviceKey + GDE.STRING_MESSAGE_CONCAT + keyString);
					this.deviceConfigurations.put(keyString, devConfig);
				}
			}
			catch (JAXBException e) {
				DeviceSelectionDialog.log.log(Level.WARNING, e.getMessage(), e);
				if (e.getLinkedException() instanceof SAXParseException) {
					SAXParseException spe = (SAXParseException) e.getLinkedException();
					GDE.setInitError(Messages.getString(MessageIds.GDE_MSGW0038, new String[] { spe.getSystemId().replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK), spe.getLocalizedMessage() }));
				}
			}
			catch (Exception e) {
				DeviceSelectionDialog.log.log(Level.WARNING, e.getMessage(), e);
			}
		}
		DeviceSelectionDialog.log.log(Level.TIME, "device init time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime)));
		if (this.selectedActiveDeviceConfig == null) this.application.setActiveDevice(null);
	}

	public void open() {
		try {
			Shell parent = getParent();
			this.dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
			SWTResourceManager.registerResourceUser(this.dialogShell);
			this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/DeviceSelection.gif")); //$NON-NLS-1$
			this.dialogShell.setLayout(new FormLayout());
			this.dialogShell.setSize(579, 592);
			this.dialogShell.setSize(580, 592);
			this.dialogShell.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					DeviceSelectionDialog.log.log(Level.FINE, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
					DeviceSelectionDialog.this.application.openHelpDialog(GDE.STRING_EMPTY, "HelpInfo_2.html"); //$NON-NLS-1$
				}
			});
			this.dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					DeviceSelectionDialog.log.log(Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
					// update device configurations if required
					for (String deviceKey : DeviceSelectionDialog.this.deviceConfigurations.keySet().toArray(new String[0])) {
						DeviceConfiguration configuration = DeviceSelectionDialog.this.deviceConfigurations.get(deviceKey);
						if (configuration.isChangePropery()) {
							DeviceSelectionDialog.log.log(Level.FINE, configuration.isChangePropery() + " update device properties for " + configuration.getName()); //$NON-NLS-1$
							configuration.storeDeviceProperties(); // stores only if is changed
						}
					}
					// initialize selected device
					if (isDeviceChanged()) {
						setupDevice();
					}
					else if (DeviceSelectionDialog.this.selectedActiveDeviceConfig == null) { // no device selected
						DeviceSelectionDialog.this.application.setActiveDevice(null);
					}

					DataExplorer.display.asyncExec(new Runnable() {
						public void run() {
							handleAutoOpenAfterClose();
						}
					});
					DeviceSelectionDialog.log.log(Level.FINE, "disposed"); //$NON-NLS-1$
				}

				/**
				 * handle auto open functions
				 */
				void handleAutoOpenAfterClose() {
					try {
						if (DeviceSelectionDialog.this.settings.isAutoOpenSerialPort()) {
							while (DeviceSelectionDialog.this.listPortsThread.isAlive()) {
								Thread.sleep(500);
							}
							if (DeviceSelectionDialog.this.application.getActiveDevice() != null) {
								DeviceSerialPort serialPort = DeviceSelectionDialog.this.application.getActiveDevice().getSerialPort();
								if (serialPort != null && !serialPort.isConnected()) DeviceSelectionDialog.this.application.getActiveDevice().openCloseSerialPort();
							}
						}
						if (DeviceSelectionDialog.this.settings.isAutoOpenToolBox()) {
							DeviceSelectionDialog.this.application.openDeviceDialog();
						}
					}
					catch (Exception e) {
						DeviceSelectionDialog.log.log(Level.SEVERE, e.getMessage(), e);
						DeviceSelectionDialog.this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT
								+ e.getMessage() }));
					}
				}
			});
			this.dialogShell.setBackground(DataExplorer.COLOR_LIGHT_GREY);
			{
				FormData composite1LData = new FormData();
				composite1LData.width = 558;
				composite1LData.height = 559;
				composite1LData.left = new FormAttachment(0, 1000, 0);
				composite1LData.top = new FormAttachment(0, 1000, 0);
				composite1LData.right = new FormAttachment(1000, 1000, 0);
				composite1LData.bottom = new FormAttachment(1000, 1000, -52);
				this.composite1 = new Composite(this.dialogShell, SWT.NONE);
				FillLayout composite1Layout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.composite1.setLayout(composite1Layout);
				this.composite1.setLayoutData(composite1LData);
				{
					this.settingsTabFolder = new CTabFolder(this.composite1, SWT.BORDER);
					this.settingsTabFolder.setSimple(false);
					{
						this.cTabItem1 = new CTabItem(this.settingsTabFolder, SWT.NONE);
						this.cTabItem1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.cTabItem1.setText(Messages.getString(MessageIds.GDE_MSGT0154));
						{
							this.composite2 = new Composite(this.settingsTabFolder, SWT.NONE);
							this.cTabItem1.setControl(this.composite2);
							this.composite2.setLayout(null);
							{
								this.deviceSelectionGroup = new Group(this.composite2, SWT.NONE);
								this.deviceSelectionGroup.setLayout(null);
								this.deviceSelectionGroup.setText(Messages.getString(MessageIds.GDE_MSGT0155));
								this.deviceSelectionGroup.setBounds(12, 9, 524, 250);
								{
									this.deviceSelectCombo = new CCombo(this.deviceSelectionGroup, SWT.BORDER);
									this.deviceSelectCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.deviceSelectCombo.setItems(new String[] { Messages.getString(MessageIds.GDE_MSGT0156) });
									this.deviceSelectCombo.setBounds(12, GDE.IS_MAC_COCOA ? 15 : 20, 375, GDE.IS_LINUX ? 22 : 20);
									this.deviceSelectCombo.setEditable(false);
									this.deviceSelectCombo.setBackground(DataExplorer.COLOR_WHITE);
									this.deviceSelectCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											DeviceSelectionDialog.log.log(Level.FINEST, "deviceSelectCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											// allow device switch only if port not connected
											if (DeviceSelectionDialog.this.application.getActiveDevice() == null 
													|| (DeviceSelectionDialog.this.application.getActiveDevice() != null && DeviceSelectionDialog.this.application.getActiveDevice().getSerialPort() == null)
													|| (DeviceSelectionDialog.this.application.getActiveDevice() != null && DeviceSelectionDialog.this.application.getActiveDevice().getSerialPort() != null && !DeviceSelectionDialog.this.application.getActiveDevice().getSerialPort().isConnected())) {
												DeviceSelectionDialog.this.activeDeviceName = DeviceSelectionDialog.this.deviceSelectCombo.getText();
												DeviceSelectionDialog.log.log(Level.FINE, "activeName = " + DeviceSelectionDialog.this.activeDeviceName); //$NON-NLS-1$
												DeviceSelectionDialog.this.selectedActiveDeviceConfig = DeviceSelectionDialog.this.deviceConfigurations.get(DeviceSelectionDialog.this.activeDeviceName);
												// if a device tool box is open, dispose it
												if (DeviceSelectionDialog.this.application.getActiveDevice() != null && !DeviceSelectionDialog.this.application.getDeviceDialog().isDisposed()) {
													DeviceSelectionDialog.this.application.getDeviceDialog().dispose();
												}
												updateDialogEntries();
											}
											else {
												DeviceSelectionDialog.this.application.openMessageDialog(DeviceSelectionDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGW0017));
											}
										}
									});
								}
								{
									this.deviceSlider = new Slider(this.deviceSelectionGroup, SWT.HORIZONTAL);
									this.deviceSlider.setBounds(393, GDE.IS_WINDOWS ? 18 : GDE.IS_MAC_COCOA ? 3 : 16, 119, GDE.IS_WINDOWS ? 22 : 28);
									this.deviceSlider.setBackground(DataExplorer.COLOR_LIGHT_GREY);
									this.deviceSlider.setMinimum(0);
									this.deviceSlider.setMaximum(1);
									this.deviceSlider.setIncrement(1);
									this.deviceSlider.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											DeviceSelectionDialog.log.log(Level.FINEST, "deviceSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
											// allow device switch only if port not connected
											if (DeviceSelectionDialog.this.application.getActiveDevice() == null 
													|| (DeviceSelectionDialog.this.application.getActiveDevice() != null && DeviceSelectionDialog.this.application.getActiveDevice().getSerialPort() == null)
													|| (DeviceSelectionDialog.this.application.getActiveDevice() != null && DeviceSelectionDialog.this.application.getActiveDevice().getSerialPort() != null && !DeviceSelectionDialog.this.application.getActiveDevice().getSerialPort().isConnected())) {
												int position = DeviceSelectionDialog.this.deviceSlider.getSelection();
												DeviceSelectionDialog.log.log(Level.FINE, " Position: " + position); //$NON-NLS-1$
												if (DeviceSelectionDialog.this.activeDevices.size() > 0 && !DeviceSelectionDialog.this.activeDevices.get(position).equals(DeviceSelectionDialog.this.activeDeviceName)) {
													DeviceSelectionDialog.this.activeDeviceName = DeviceSelectionDialog.this.activeDevices.get(position);
													DeviceSelectionDialog.log.log(Level.FINE, "activeName = " + DeviceSelectionDialog.this.activeDeviceName); //$NON-NLS-1$
													DeviceSelectionDialog.this.selectedActiveDeviceConfig = DeviceSelectionDialog.this.deviceConfigurations.get(DeviceSelectionDialog.this.activeDeviceName);
													// if a device tool box is open, dispose it
													if (DeviceSelectionDialog.this.application.getDeviceDialog() != null && !DeviceSelectionDialog.this.application.getDeviceDialog().isDisposed()) {
														DeviceSelectionDialog.this.application.getDeviceDialog().dispose();
													}
													updateDialogEntries();
												}
											}
											else {
												DeviceSelectionDialog.this.application.openMessageDialog(DeviceSelectionDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGW0017));
											}
										}
									});
								}
								{
									this.deviceGroupText = new Label(this.deviceSelectionGroup, SWT.WRAP);
									this.deviceGroupText.setBounds(12, 48, 430, 16);
									this.deviceGroupText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.deviceGroupText.setText(Messages.getString(MessageIds.GDE_MSGI0026));
									this.deviceGroupText.setBackground(DataExplorer.COLOR_LIGHT_GREY);
								}
								{
									this.deviceCanvas = new Canvas(this.deviceSelectionGroup, SWT.BORDER);
									this.deviceCanvas.setBounds(12, 70, 227, 165);
									this.deviceCanvas.setBackgroundImage(SWTResourceManager.getImage("gde/resource/NoDevicePicture.jpg")); //$NON-NLS-1$
								}
								{
									this.manufacturerDescription = new Label(this.deviceSelectionGroup, SWT.WRAP);
									this.manufacturerDescription.setBounds(251, 70, 101, 16);
									this.manufacturerDescription.setText(Messages.getString(MessageIds.GDE_MSGT0157));
									this.manufacturerDescription.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.manufacturerDescription.setBackground(DataExplorer.COLOR_LIGHT_GREY);
								}
								{
									this.deviceNameDescription = new Label(this.deviceSelectionGroup, SWT.WRAP);
									this.deviceNameDescription.setBackground(DataExplorer.COLOR_LIGHT_GREY);
									this.deviceNameDescription.setBounds(251, 92, 101, 16);
									this.deviceNameDescription.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.deviceNameDescription.setText(Messages.getString(MessageIds.GDE_MSGT0158));
								}
								{
									this.deviceTypeDescription = new Label(this.deviceSelectionGroup, SWT.WRAP);
									this.deviceTypeDescription.setBackground(DataExplorer.COLOR_LIGHT_GREY);
									this.deviceTypeDescription.setBounds(251, 114, 101, 16);
									this.deviceTypeDescription.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.deviceTypeDescription.setText(Messages.getString(MessageIds.GDE_MSGT0159));
								}
								{
									this.internetLinkDescription = new Label(this.deviceSelectionGroup, SWT.WRAP);
									this.internetLinkDescription.setBackground(DataExplorer.COLOR_LIGHT_GREY);
									this.internetLinkDescription.setBounds(251, 136, 101, 16);
									this.internetLinkDescription.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.internetLinkDescription.setText(Messages.getString(MessageIds.GDE_MSGT0160));
								}
								{
									this.manufacturerName = new Label(this.deviceSelectionGroup, SWT.NONE);
									this.manufacturerName.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.manufacturerName.setBounds(358, 70, 154, 16);
									this.manufacturerName.setBackground(DataExplorer.COLOR_LIGHT_GREY);
								}
								{
									this.deviceText = new Label(this.deviceSelectionGroup, SWT.NONE);
									this.deviceText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.deviceText.setBackground(DataExplorer.COLOR_LIGHT_GREY);
									this.deviceText.setBounds(358, 92, 154, 16);
								}
								{
									this.deviceTypeText = new Label(this.deviceSelectionGroup, SWT.NONE);
									this.deviceTypeText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.deviceTypeText.setBackground(DataExplorer.COLOR_LIGHT_GREY);
									this.deviceTypeText.setBounds(358, 114, 154, 16);
								}
								{
									this.internetLinkText = new Label(this.deviceSelectionGroup, SWT.NONE);
									this.internetLinkText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.internetLinkText.setBackground(DataExplorer.COLOR_LIGHT_GREY);
									this.internetLinkText.setBounds(358, 136, 154, 16);
								}
								{
									this.openPortCheck = new Button(this.deviceSelectionGroup, SWT.CHECK | SWT.LEFT);
									this.openPortCheck.setBounds(251, 176, 261, 16);
									this.openPortCheck.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.openPortCheck.setText(Messages.getString(MessageIds.GDE_MSGT0161));
									this.openPortCheck.setSelection(this.settings.isAutoOpenSerialPort());
									this.openPortCheck.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											DeviceSelectionDialog.log.log(Level.FINEST, "openPortCheck.widgetSelected, event=" + evt); //$NON-NLS-1$
											if (DeviceSelectionDialog.this.openPortCheck.getSelection()) {
												DeviceSelectionDialog.this.settings.setProperty(Settings.AUTO_OPEN_SERIAL_PORT, "true"); //$NON-NLS-1$
												DeviceSelectionDialog.this.application.openMessageDialog(DeviceSelectionDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGI0037));
											}
											else
												DeviceSelectionDialog.this.settings.setProperty(Settings.AUTO_OPEN_SERIAL_PORT, "false"); //$NON-NLS-1$
										}
									});
								}
								{
									this.openToolBoxCheck = new Button(this.deviceSelectionGroup, SWT.CHECK | SWT.LEFT);
									this.openToolBoxCheck.setBounds(251, 198, 261, 16);
									this.openToolBoxCheck.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.openToolBoxCheck.setText(Messages.getString(MessageIds.GDE_MSGT0162));
									this.openToolBoxCheck.setSelection(this.settings.isAutoOpenToolBox());
									this.openToolBoxCheck.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											DeviceSelectionDialog.log.log(Level.FINEST, "openToolBoxCheck.widgetSelected, event=" + evt); //$NON-NLS-1$
											if (DeviceSelectionDialog.this.openToolBoxCheck.getSelection()) {
												DeviceSelectionDialog.this.settings.setProperty(Settings.AUTO_OPEN_TOOL_BOX, "true"); //$NON-NLS-1$
												DeviceSelectionDialog.this.application.openMessageDialog(DeviceSelectionDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGI0038));
											}
											else
												DeviceSelectionDialog.this.settings.setProperty(Settings.AUTO_OPEN_TOOL_BOX, "false"); //$NON-NLS-1$
										}
									});
								}
							}
							{
								this.serialPortSelectionGroup = new Group(this.composite2, SWT.NONE);
								this.serialPortSelectionGroup.setLayout(null);
								this.serialPortSelectionGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.serialPortSelectionGroup.setText(Messages.getString(MessageIds.GDE_MSGT0163));
								this.serialPortSelectionGroup.setBounds(12, 265, 524, 58);
								{
									this.portDescription = new Label(this.serialPortSelectionGroup, SWT.NONE);
									this.portDescription.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.portDescription.setText(Messages.getString(MessageIds.GDE_MSGT0164));
									this.portDescription.setBounds(30, GDE.IS_MAC_COCOA ? 19 : 29, 150, 18);
									this.portDescription.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0165));
									this.portDescription.setBackground(DataExplorer.COLOR_LIGHT_GREY);
								}
								{
									this.portSelectCombo = new CCombo(this.serialPortSelectionGroup, SWT.FLAT | SWT.BORDER);
									this.portSelectCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.portSelectCombo.setBounds(185, GDE.IS_MAC_COCOA ? 17 : 27, 320, GDE.IS_LINUX ? 22 : 20);
									this.portSelectCombo.setEditable(false);
									this.portSelectCombo.setText(Messages.getString(MessageIds.GDE_MSGT0199));
									this.portSelectCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0165));
									this.portSelectCombo.setBackground(DataExplorer.COLOR_WHITE);
									this.portSelectCombo.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											DeviceSelectionDialog.log.log(Level.FINEST, "portSelectCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											DeviceSelectionDialog.this.selectedActiveDeviceConfig.setPort(DeviceSelectionDialog.this.portSelectCombo.getText().trim().split(GDE.STRING_BLANK)[0]);
											DeviceSelectionDialog.this.selectedActiveDeviceConfig.storeDeviceProperties();
											DeviceSelectionDialog.this.application.updateTitleBar(DeviceSelectionDialog.this.application.getObjectKey(), DeviceSelectionDialog.this.selectedActiveDeviceConfig.getName(),
													DeviceSelectionDialog.this.selectedActiveDeviceConfig.getPort());
										}
									});
								}
							}
							{
								this.portSettingsGroup = new Group(this.composite2, SWT.NONE);
								this.portSettingsGroup.setLayout(null);
								this.portSettingsGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.portSettingsGroup.setText(Messages.getString(MessageIds.GDE_MSGT0166));
								this.portSettingsGroup.setBounds(12, 409, 524, 115);
								this.portSettingsGroup.addPaintListener(new PaintListener() {
									public void paintControl(PaintEvent evt) {
										DeviceSelectionDialog.log.log(Level.FINEST, "portSettingsGroup.paintControl, event=" + evt); //$NON-NLS-1$
										if (DeviceSelectionDialog.this.portSettingsGroup.getEnabled()) {
											DeviceSelectionDialog.this.baudeDescription.setEnabled(true);
											DeviceSelectionDialog.this.baudeDescription.setEnabled(true);
											DeviceSelectionDialog.this.dataBitsDescription.setEnabled(true);
											DeviceSelectionDialog.this.stopbitsDescription.setEnabled(true);
											DeviceSelectionDialog.this.parityDescription.setEnabled(true);
											DeviceSelectionDialog.this.flowcontrolDescription.setEnabled(true);
											DeviceSelectionDialog.this.dtrDescription.setEnabled(true);
											DeviceSelectionDialog.this.rtsDescription.setEnabled(true);
											DeviceSelectionDialog.this.baudeSelectLabel.setEnabled(true);
											DeviceSelectionDialog.this.dataBitsSelectLabel.setEnabled(true);
											DeviceSelectionDialog.this.stopBitsSelectLabel.setEnabled(true);
											DeviceSelectionDialog.this.paritySelectLabel.setEnabled(true);
											DeviceSelectionDialog.this.flowControlSelectLabel.setEnabled(true);
										}
										else {
											DeviceSelectionDialog.this.baudeDescription.setEnabled(false);
											DeviceSelectionDialog.this.baudeDescription.setEnabled(false);
											DeviceSelectionDialog.this.dataBitsDescription.setEnabled(false);
											DeviceSelectionDialog.this.stopbitsDescription.setEnabled(false);
											DeviceSelectionDialog.this.parityDescription.setEnabled(false);
											DeviceSelectionDialog.this.flowcontrolDescription.setEnabled(false);
											DeviceSelectionDialog.this.dtrDescription.setEnabled(false);
											DeviceSelectionDialog.this.rtsDescription.setEnabled(false);
											DeviceSelectionDialog.this.baudeSelectLabel.setEnabled(false);
											DeviceSelectionDialog.this.dataBitsSelectLabel.setEnabled(false);
											DeviceSelectionDialog.this.stopBitsSelectLabel.setEnabled(false);
											DeviceSelectionDialog.this.paritySelectLabel.setEnabled(false);
											DeviceSelectionDialog.this.flowControlSelectLabel.setEnabled(false);
										}
								}
								});
								{
									this.baudeDescription = new Label(this.portSettingsGroup, SWT.NONE);
									this.baudeDescription.setBackground(DataExplorer.COLOR_LIGHT_GREY);
									this.baudeDescription.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.baudeDescription.setText(Messages.getString(MessageIds.GDE_MSGT0167));
									this.baudeDescription.setBounds(8, GDE.IS_MAC_COCOA ? 11 : 21, 100, 16);
								}
								{
									this.stopbitsDescription = new Label(this.portSettingsGroup, SWT.NONE);
									this.stopbitsDescription.setBackground(DataExplorer.COLOR_LIGHT_GREY);
									this.stopbitsDescription.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.stopbitsDescription.setText(Messages.getString(MessageIds.GDE_MSGT0168));
									this.stopbitsDescription.setBounds(8, GDE.IS_MAC_COCOA ? 55 : 65, 100, 16);
								}
								{
									this.flowcontrolDescription = new Label(this.portSettingsGroup, SWT.NONE);
									this.flowcontrolDescription.setBackground(DataExplorer.COLOR_LIGHT_GREY);
									this.flowcontrolDescription.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.flowcontrolDescription.setText(Messages.getString(MessageIds.GDE_MSGT0169));
									this.flowcontrolDescription.setBounds(261, GDE.IS_MAC_COCOA ? 11 : 21, 100, 16);
								}
								{
									this.dataBitsDescription = new Label(this.portSettingsGroup, SWT.NONE);
									this.dataBitsDescription.setBackground(DataExplorer.COLOR_LIGHT_GREY);
									this.dataBitsDescription.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.dataBitsDescription.setText(Messages.getString(MessageIds.GDE_MSGT0170));
									this.dataBitsDescription.setBounds(8, GDE.IS_MAC_COCOA ? 33 : 43, 100, 16);
								}
								{
									this.parityDescription = new Label(this.portSettingsGroup, SWT.NONE);
									this.parityDescription.setBackground(DataExplorer.COLOR_LIGHT_GREY);
									this.parityDescription.setBounds(8, GDE.IS_MAC_COCOA ? 76 : 86, 100, 16);
									this.parityDescription.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.parityDescription.setText(Messages.getString(MessageIds.GDE_MSGT0171));
								}
								{
									this.dtrDescription = new Label(this.portSettingsGroup, SWT.NONE);
									this.dtrDescription.setBackground(DataExplorer.COLOR_LIGHT_GREY);
									this.dtrDescription.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.dtrDescription.setText(Messages.getString(MessageIds.GDE_MSGT0172));
									this.dtrDescription.setBounds(261, GDE.IS_MAC_COCOA ? 39 : 49, 105, 17);
								}
								{
									this.rtsDescription = new Label(this.portSettingsGroup, SWT.NONE);
									this.rtsDescription.setBackground(DataExplorer.COLOR_LIGHT_GREY);
									this.rtsDescription.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.rtsDescription.setText(Messages.getString(MessageIds.GDE_MSGT0173));
									this.rtsDescription.setBounds(261, GDE.IS_MAC_COCOA ? 62 : 72, 77, 17);
								}
								{
									this.baudeSelectLabel = new Label(this.portSettingsGroup, SWT.RIGHT);
									this.baudeSelectLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.baudeSelectLabel.setBounds(115, GDE.IS_MAC_COCOA ? 13 : 23, 90, 19);
								}
								{
									this.stopBitsSelectLabel = new Label(this.portSettingsGroup, SWT.RIGHT);
									this.stopBitsSelectLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.stopBitsSelectLabel.setBounds(115, GDE.IS_MAC_COCOA ? 57 : 67, 90, 19);
								}
								{
									this.paritySelectLabel = new Label(this.portSettingsGroup, SWT.RIGHT);
									this.paritySelectLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.paritySelectLabel.setBounds(115, GDE.IS_MAC_COCOA ? 79 : 89, 90, 19);
								}
								{
									this.dataBitsSelectLabel = new Label(this.portSettingsGroup, SWT.RIGHT);
									this.dataBitsSelectLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.dataBitsSelectLabel.setBounds(115, GDE.IS_MAC_COCOA ? 35 : 45, 90, 19);
								}
								{
									this.flowControlSelectLabel = new Label(this.portSettingsGroup, SWT.LEFT);
									this.flowControlSelectLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.flowControlSelectLabel.setBounds(372, GDE.IS_MAC_COCOA ? 11 : 21, 90, 19);
								}
								{
									this.dtrCheckBox = new Button(this.portSettingsGroup, SWT.CHECK | SWT.LEFT);
									this.dtrCheckBox.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.dtrCheckBox.setBounds(372, GDE.IS_MAC_COCOA ? 39 : 49, 92, 17);
									this.dtrCheckBox.setText(Messages.getString(MessageIds.GDE_MSGT0174));
									this.dtrCheckBox.setEnabled(false);
								}
								{
									this.rtsCheckBox = new Button(this.portSettingsGroup, SWT.CHECK | SWT.LEFT);
									this.rtsCheckBox.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.rtsCheckBox.setBounds(372, GDE.IS_MAC_COCOA ? 62 : 72, 102, 17);
									this.rtsCheckBox.setText(Messages.getString(MessageIds.GDE_MSGT0175));
									this.rtsCheckBox.setEnabled(false);
								}
							}
							{
								this.desktopTabsGroup = new Group(this.composite2, SWT.NONE);
								this.desktopTabsGroup.setLayout(null);
								this.desktopTabsGroup.setBounds(12, 329, 524, 74);
								this.desktopTabsGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.desktopTabsGroup.setText(Messages.getString(MessageIds.GDE_MSGT0176));
								this.desktopTabsGroup.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0177));
								{
									this.tableTabButton = new Button(this.desktopTabsGroup, SWT.CHECK | SWT.LEFT);
									this.tableTabButton.setBounds(12, GDE.IS_MAC_COCOA ? 20 : 30, 105, 25);
									this.tableTabButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.tableTabButton.setText(Messages.getString(MessageIds.GDE_MSGT0178));
									this.tableTabButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0179));
									this.tableTabButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											DeviceSelectionDialog.log.log(Level.FINEST, "tableTabButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											DeviceSelectionDialog.this.selectedActiveDeviceConfig.setTableTabRequested(DeviceSelectionDialog.this.tableTabButton.getSelection());
											DeviceSelectionDialog.this.application.setDataTableTabItemVisible(DeviceSelectionDialog.this.selectedActiveDeviceConfig.isTableTabRequested());
}
									});
								}
								{
									this.digitalTabButton = new Button(this.desktopTabsGroup, SWT.CHECK | SWT.LEFT);
									this.digitalTabButton.setBounds(141, GDE.IS_MAC_COCOA ? 20 : 30, 110, 25);
									this.digitalTabButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.digitalTabButton.setText(Messages.getString(MessageIds.GDE_MSGT0180));
									this.digitalTabButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0181));
									this.digitalTabButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											DeviceSelectionDialog.log.log(Level.FINEST, "digitalTabButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											DeviceSelectionDialog.this.selectedActiveDeviceConfig.setDigitalTabRequested(DeviceSelectionDialog.this.digitalTabButton.getSelection());
											DeviceSelectionDialog.this.application.setDigitalTabItemVisible(DeviceSelectionDialog.this.selectedActiveDeviceConfig.isDigitalTabRequested());
										}
									});
								}
								{
									this.analogTabButton = new Button(this.desktopTabsGroup, SWT.CHECK | SWT.LEFT);
									this.analogTabButton.setBounds(272, GDE.IS_MAC_COCOA ? 20 : 30, 110, 25);
									this.analogTabButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.analogTabButton.setText(Messages.getString(MessageIds.GDE_MSGT0182));
									this.analogTabButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0183));
									this.analogTabButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											DeviceSelectionDialog.log.log(Level.FINEST, "analogTabButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											DeviceSelectionDialog.this.selectedActiveDeviceConfig.setAnalogTabRequested(DeviceSelectionDialog.this.analogTabButton.getSelection());
											DeviceSelectionDialog.this.application.setAnalogTabItemVisible(DeviceSelectionDialog.this.selectedActiveDeviceConfig.isAnalogTabRequested());
										}
									});
								}
								{
									this.voltagePerCellButton = new Button(this.desktopTabsGroup, SWT.CHECK | SWT.LEFT);
									this.voltagePerCellButton.setBounds(402, GDE.IS_MAC_COCOA ? 20 : 30, 110, 25);
									this.voltagePerCellButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.voltagePerCellButton.setText(Messages.getString(MessageIds.GDE_MSGT0184));
									this.voltagePerCellButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0185));
									this.voltagePerCellButton.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											DeviceSelectionDialog.log.log(Level.FINEST, "cellVoltageButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											DeviceSelectionDialog.this.selectedActiveDeviceConfig.setVoltagePerCellTabRequested(DeviceSelectionDialog.this.voltagePerCellButton.getSelection());
											DeviceSelectionDialog.this.application.setCellVoltageTabItemVisible(DeviceSelectionDialog.this.selectedActiveDeviceConfig.isVoltagePerCellTabRequested());
										}
									});
								}
							}
						}
					}
					{
						this.auswahlTabItem = new CTabItem(this.settingsTabFolder, SWT.NONE);
						this.auswahlTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.auswahlTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0186));
						{
							this.deviceGroup = new Group(this.settingsTabFolder, SWT.NONE);
							this.deviceGroup.setLayout(null);
							this.auswahlTabItem.setControl(this.deviceGroup);
							this.deviceGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.deviceGroup.setText(Messages.getString(MessageIds.GDE_MSGT0187));
							{
								this.deviceTable = new Table(this.deviceGroup, SWT.MULTI | SWT.CHECK | SWT.BORDER);
								this.deviceTable.setBounds(17, 40, 497, 401);
								this.deviceTable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
								this.deviceTable.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										TableItem item = (TableItem) evt.item;
										String deviceName = ((TableItem) evt.item).getText();
										DeviceConfiguration tmpDeviceConfiguration;
										if (item.getChecked()) {
											DeviceSelectionDialog.log.log(Level.FINE, "add device = " + deviceName); //$NON-NLS-1$
											tmpDeviceConfiguration = DeviceSelectionDialog.this.deviceConfigurations.get(deviceName);
											tmpDeviceConfiguration.setUsed(true);
											//tmpDeviceConfiguration.storeDeviceProperties();
											DeviceSelectionDialog.this.deviceConfigurations.put(deviceName, tmpDeviceConfiguration);
											if (!DeviceSelectionDialog.this.activeDevices.contains(deviceName)) DeviceSelectionDialog.this.activeDevices.add(deviceName);
											if (DeviceSelectionDialog.this.activeDevices.size() == 1) {
												DeviceSelectionDialog.this.selectedActiveDeviceConfig = DeviceSelectionDialog.this.deviceConfigurations.get(deviceName);
											}
										}
										else {
											DeviceSelectionDialog.log.log(Level.FINE, "remove device = " + deviceName); //$NON-NLS-1$
											tmpDeviceConfiguration = DeviceSelectionDialog.this.deviceConfigurations.get(deviceName);
											tmpDeviceConfiguration.setUsed(false);
											//tmpDeviceConfiguration.storeDeviceProperties();
											if (DeviceSelectionDialog.this.activeDevices.contains(deviceName)) DeviceSelectionDialog.this.activeDevices.remove(deviceName);

											// the removed configuration is the active one
											if (DeviceSelectionDialog.this.selectedActiveDeviceConfig != null && DeviceSelectionDialog.this.selectedActiveDeviceConfig.getName().equals(deviceName)) {
												// take first available
												if (DeviceSelectionDialog.this.activeDevices.size() > 0) {
													DeviceSelectionDialog.this.selectedActiveDeviceConfig = DeviceSelectionDialog.this.deviceConfigurations.get(DeviceSelectionDialog.this.activeDevices.firstElement());
												}
												else { // no device
													DeviceSelectionDialog.this.selectedActiveDeviceConfig = null;
												}
											}
										}
										updateDialogEntries();
									}
								});
								this.deviceTable.setLinesVisible(true);
								{
									this.tableColumn1 = new TableColumn(this.deviceTable, SWT.LEFT);
									this.tableColumn1.setText("Column1"); //$NON-NLS-1$
									this.tableColumn1.setWidth(200);
								}
								{
									this.tableColumn2 = new TableColumn(this.deviceTable, SWT.LEFT);
									this.tableColumn2.setText("Column2"); //$NON-NLS-1$
									this.tableColumn2.setWidth(180);
								}
								{
									this.tableColumn3 = new TableColumn(this.deviceTable, SWT.LEFT);
									this.tableColumn3.setText("Column3"); //$NON-NLS-1$
									this.tableColumn3.setWidth(90);
								}
								{
									updateDeviceSelectionTable();
								}
							}
						}
					}
					this.settingsTabFolder.setSelection(0);
					this.settingsTabFolder.setBounds(0, 0, 559, 505);
				}
			}
			{
				this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				FormData closeButtonLData = new FormData();
				closeButtonLData.left = new FormAttachment(0, 1000, 12);
				closeButtonLData.top = new FormAttachment(0, 1000, 573);
				closeButtonLData.width = 529;
				closeButtonLData.height = 26;
				this.closeButton.setLayoutData(closeButtonLData);
				this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.closeButton.setText(Messages.getString(MessageIds.GDE_MSGT0188));
				this.closeButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						DeviceSelectionDialog.log.log(Level.FINE, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						DeviceSelectionDialog.this.dialogShell.dispose();
					}
				});
			}
			initializeUI(); // update all the entries according active device configuration			
			updateAvailablePorts();
			this.dialogShell.layout();
			this.dialogShell.pack();
			this.dialogShell.setSize(566, 644);
			this.dialogShell.setText(Messages.getString(MessageIds.GDE_MSGT0189));
			this.dialogShell.setLocation(getParent().toDisplay(100, 10));
			this.dialogShell.open();
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			DeviceSelectionDialog.log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(this.dialogShell, Messages.getString(MessageIds.GDE_MSGE0007) + e.getMessage());
		}
	}

	/**
	 * 
	 */
	private void updateDeviceSelectionTable() {
		if (!this.isDisposed()) {
			this.deviceTable.removeAll();
			for (String deviceKey : this.deviceConfigurations.keySet()) {
				DeviceSelectionDialog.log.log(Level.FINE, deviceKey);
				DeviceConfiguration config = this.deviceConfigurations.get(deviceKey);

				TableItem item = new TableItem(this.deviceTable, SWT.NULL);
				item.setText(new String[] { config.getName(), config.getManufacturer(), config.getSerialPortType() != null ? config.getPort() : GDE.STRING_MESSAGE_CONCAT });
				if (config.isUsed()) {
					item.setChecked(true);
				}
				else {
					item.setChecked(false);
				}
			}
		}
	}

	/**
	 * update entries according configuration, this is called whenever a new device is selected
	 */
	void updateDialogEntries() {
		if (!this.isDisposed()) {
			// device selection
			DeviceSelectionDialog.log.log(Level.FINE, "active devices " + this.activeDevices.toString()); //$NON-NLS-1$
			String[] list = this.activeDevices.toArray(new String[this.activeDevices.size()]);
			Arrays.sort(list); // this sorts the list but not the vector
			//get sorted devices list and activeDevices array in sync
			this.activeDevices.removeAllElements();
			for (String device : list) {
				this.activeDevices.add(device);
			}
			if (list.length > 0) {
				this.activeDeviceName = (this.selectedActiveDeviceConfig == null) ? this.activeDevices.firstElement() : this.selectedActiveDeviceConfig.getName();
				this.deviceSelectCombo.setItems(list);
				this.deviceSelectCombo.setVisibleItemCount(this.activeDevices.size() + 1);
				this.deviceSelectCombo.select(this.activeDevices.indexOf(this.activeDeviceName));
				DeviceSelectionDialog.log.log(Level.FINE, this.activeDeviceName + GDE.STRING_MESSAGE_CONCAT + this.activeDevices.indexOf(this.activeDeviceName));

				this.deviceSlider.setMaximum(this.activeDevices.size());
				this.deviceSlider.setSelection(this.activeDevices.indexOf(this.activeDeviceName));
				DeviceSelectionDialog.log.log(Level.FINE, "activeDevices.size() " + this.activeDevices.size()); //$NON-NLS-1$
				if (this.activeDevices.size() > 1)
					this.application.enableDeviceSwitchButtons(true);
				else
					this.application.enableDeviceSwitchButtons(false);

				this.selectedActiveDeviceConfig = this.deviceConfigurations.get(this.activeDeviceName);
			}
			else { // no active device
				this.selectedActiveDeviceConfig = null;
				this.activeDeviceName = (this.selectedActiveDeviceConfig == null) ? GDE.STRING_EMPTY : this.selectedActiveDeviceConfig.getName();
				this.deviceSelectCombo.setItems(new String[] { Messages.getString(MessageIds.GDE_MSGT0190) });
				this.deviceSelectCombo.setVisibleItemCount(1);
				this.deviceSelectCombo.select(0);
				DeviceSelectionDialog.log.log(Level.FINE, "no active device"); //$NON-NLS-1$

				this.deviceSlider.setMaximum(1);
				this.deviceSlider.setIncrement(0);
				this.deviceSlider.setSelection(0);
				DeviceSelectionDialog.log.log(Level.FINE, "activeDevices.size() = 0"); //$NON-NLS-1$
				this.application.updateTitleBar(this.application.getObjectKey(), Settings.EMPTY, Settings.EMPTY);
			}
			// update all serial Port settings
			if (this.selectedActiveDeviceConfig == null) {
				DeviceSelectionDialog.log.log(Level.FINE, "activeDeviceConfig == null -> no device selected as active"); //$NON-NLS-1$
				this.application.setActiveDevice(null);
				this.deviceCanvas.setBackgroundImage(SWTResourceManager.getImage("gde/resource/NoDevicePicture.jpg")); //$NON-NLS-1$

				this.manufacturerName.setText(Settings.EMPTY);
				this.deviceText.setText(Settings.EMPTY);
				this.deviceTypeText.setText(Settings.EMPTY);
				this.internetLinkText.setText(Settings.EMPTY);

				this.portSelectCombo.setItems(StringHelper.prepareSerialPortList(DeviceSelectionDialog.this.availablePorts));
				this.portSelectCombo.select(0);

				// com port adjustments group
				this.baudeSelectLabel.setText("0"); //$NON-NLS-1$
				this.dataBitsSelectLabel.setText("0"); //$NON-NLS-1$
				this.stopBitsSelectLabel.setText("0"); //$NON-NLS-1$
				this.paritySelectLabel.setText("0"); //$NON-NLS-1$
				this.flowControlSelectLabel.setText("0"); //$NON-NLS-1$
				this.dtrCheckBox.setSelection(false);
				this.rtsCheckBox.setSelection(false);
			}
			else {
				IDevice selectedDevice = getInstanceOfDevice();
				if (selectedDevice != null) {
					log.log(Level.FINE, this.settings.getDevicesPath() + this.selectedActiveDeviceConfig.getImageFileName());
					this.deviceCanvas.setBackgroundImage(SWTResourceManager.getImage(selectedDevice, "resource/" + this.selectedActiveDeviceConfig.getImageFileName()));
					this.manufacturerName.setText(this.selectedActiveDeviceConfig.getManufacturer());
					this.deviceText.setText(this.selectedActiveDeviceConfig.getName());
					this.deviceTypeText.setText(this.selectedActiveDeviceConfig.getDeviceGroup().name());
					String link = this.selectedActiveDeviceConfig.getManufacturerURL() != null ? this.selectedActiveDeviceConfig.getManufacturerURL() : Messages.getString(MessageIds.GDE_MSGT0191);
					this.internetLinkText.setText(link);
					if (this.deviceConfigurations.get(activeDeviceName).getSerialPortType() != null) {
						if (!this.serialPortSelectionGroup.getEnabled() || !this.portSettingsGroup.getEnabled()) {
							enableSerialPortEntries(true);
						}
						// serial port adjustments group
						this.baudeSelectLabel.setText(this.selectedActiveDeviceConfig.getBaudeRate().toString());
						this.dataBitsSelectLabel.setText(GDE.STRING_BLANK + this.selectedActiveDeviceConfig.getDataBits().toString().split(GDE.STRING_UNDER_BAR)[1]);
						this.stopBitsSelectLabel.setText(GDE.STRING_BLANK + this.selectedActiveDeviceConfig.getStopBits().toString().split(GDE.STRING_UNDER_BAR)[1]);
						this.paritySelectLabel.setText(GDE.STRING_BLANK + this.selectedActiveDeviceConfig.getParity().toString().split(GDE.STRING_UNDER_BAR)[1]);
						this.flowControlSelectLabel.setText(GDE.STRING_BLANK + this.selectedActiveDeviceConfig.getFlowCtrlMode().toString().split(GDE.STRING_UNDER_BAR)[1]);
						this.dtrCheckBox.setSelection(this.selectedActiveDeviceConfig.isDTR());
						this.rtsCheckBox.setSelection(this.selectedActiveDeviceConfig.isRTS());
					}
					else {
						if (this.serialPortSelectionGroup.getEnabled() || this.portSettingsGroup.getEnabled()) {
							enableSerialPortEntries(false);
						}
						this.baudeSelectLabel.setText(GDE.STRING_MESSAGE_CONCAT);
						this.dataBitsSelectLabel.setText(GDE.STRING_MESSAGE_CONCAT);
						this.stopBitsSelectLabel.setText(GDE.STRING_MESSAGE_CONCAT);
						this.paritySelectLabel.setText(GDE.STRING_MESSAGE_CONCAT);
						this.flowControlSelectLabel.setText(GDE.STRING_MESSAGE_CONCAT);
						this.dtrCheckBox.setSelection(false);
						this.rtsCheckBox.setSelection(false);
					}
					if (this.availablePorts != null && this.availablePorts.size() > 0) {
						this.portSelectCombo.setItems(StringHelper.prepareSerialPortList(this.availablePorts));
						int index = DeviceSelectionDialog.this.availablePorts.indexOf(this.selectedActiveDeviceConfig.getPort());
						if (index > -1) {
							this.portSelectCombo.select(index);
						}
						else {
							this.portSelectCombo.setText(Messages.getString(MessageIds.GDE_MSGT0197));
						}
					}
					else {
						this.portSelectCombo.setItems(new String[0]);
						this.portSelectCombo.setText(Messages.getString(MessageIds.GDE_MSGT0199));
					}

					DeviceSelectionDialog.this.tableTabButton.setSelection(DeviceSelectionDialog.this.selectedActiveDeviceConfig.isTableTabRequested());
					DeviceSelectionDialog.this.digitalTabButton.setSelection(DeviceSelectionDialog.this.selectedActiveDeviceConfig.isDigitalTabRequested());
					DeviceSelectionDialog.this.analogTabButton.setSelection(DeviceSelectionDialog.this.selectedActiveDeviceConfig.isAnalogTabRequested());
					DeviceSelectionDialog.this.voltagePerCellButton.setSelection(DeviceSelectionDialog.this.selectedActiveDeviceConfig.isVoltagePerCellTabRequested());

					this.application.updateTitleBar(this.application.getObjectKey(), this.selectedActiveDeviceConfig.getName(), this.selectedActiveDeviceConfig.getPort());
					
					setupDevice();
				}
			}
		}
	}

	/**
	 * enable or disable serial port related groups
	 * @param enable true | false
	 */
	private void enableSerialPortEntries(boolean enable) {
		this.isUpdateSerialPorts = enable;	
		this.serialPortSelectionGroup.setEnabled(enable);
		this.portDescription.setEnabled(enable);
		this.portSelectCombo.setEnabled(enable);
		this.portSettingsGroup.setEnabled(enable);
		this.portSettingsGroup.redraw();
	}

	/**
	 * check if the configure serial port matches system available
	 */
	public boolean checkPortSelection() {
		boolean matches = true;
		if (this.availablePorts == null) {
			this.availablePorts = DeviceSerialPort.listConfiguredSerialPorts(false, DeviceSelectionDialog.this.settings.getSerialPortBlackList(), DeviceSelectionDialog.this.settings.getSerialPortWhiteList());
		}

		if (this.settings.isGlobalSerialPort() && this.availablePorts.indexOf(this.settings.getSerialPort()) < 0) {
			this.application.openMessageDialog(this.dialogShell, Messages.getString(MessageIds.GDE_MSGW0018));
			matches = false;
		}
		else if (!this.settings.isGlobalSerialPort() && (this.selectedActiveDeviceConfig != null && this.availablePorts.indexOf(this.selectedActiveDeviceConfig.getPort()) < 0)) {
			this.application.openMessageDialog(this.dialogShell, Messages.getString(MessageIds.GDE_MSGW0019));
			matches = false;
		}
		else {
			if (this.selectedActiveDeviceConfig != null) { //currently no device selected
				if (this.settings.isGlobalSerialPort()) this.selectedActiveDeviceConfig.setPort(this.settings.getSerialPort());
			}
		}
		return matches;
	}

	/**
	 * method to setup new device, this might called using this dialog or a menu item where device is switched 
	 * @throws NotSupportedException 
	 */
	public void setupDevice(String newDeviceName) throws NotSupportedException {
		int selection = this.activeDevices.indexOf(newDeviceName);
		if (selection != -1 || getDevices().keySet().contains(newDeviceName)) {
			this.activeDevices.add(newDeviceName);
			DeviceConfiguration tmpDeviceConfig = this.getDevices().get(newDeviceName);
			tmpDeviceConfig.setUsed(true);
			setActiveConfig(tmpDeviceConfig);
			setupDevice();
		}
		else {
			String msg = Messages.getString(MessageIds.GDE_MSGI0027, new Object[] { newDeviceName });
			NotSupportedException e = new NotSupportedException(msg);
			DeviceSelectionDialog.log.log(Level.WARNING, e.getMessage(), e);
			throw e;
		}

	}

	/**
	 * method to setup new device, this might called using this dialog or a menu item where device is switched 
	 */
	public void setupDevice() {
		IDevice activeDevice = this.application.getActiveDevice();
		// check if any thing to clean up
		if (activeDevice != null) { // there is an previous active device
			if (activeDevice.getDialog() != null && !activeDevice.getDialog().isDisposed()) {
				activeDevice.getDialog().dispose();
			}
		}
		// cleanup menuBar for device specific entries
		this.application.getMenuBar().cleanup();
		
		// prepare every thing for the new device
		if ((activeDevice = getInstanceOfDevice()) != null) {
			this.application.setActiveDevice(activeDevice);
			this.application.setDataTableTabItemVisible(activeDevice.isTableTabRequested());
			this.application.setDigitalTabItemVisible(activeDevice.isDigitalTabRequested());
			this.application.setAnalogTabItemVisible(activeDevice.isAnalogTabRequested());
			this.application.setCellVoltageTabItemVisible(activeDevice.isVoltagePerCellTabRequested());
			this.application.setUtilGraphicsWindowVisible(activeDevice.isUtilityGraphicsRequested());
			this.application.registerCustomTabItem(activeDevice.getCustomTabItem());
			setupDataChannels(activeDevice);
			this.application.setupDataTableHeader();
			this.application.updateDigitalWindow();
			this.application.updateAnalogWindow();
			this.application.setCellVoltageWindowOrdinal(activeDevice.getCellVoltageOrdinals());
			this.application.updateCellVoltageWindow();
			//this.application.updateCompareWindow();
			this.application.updateFileCommentWindow();
		}
	}

	/**
	 * this will setup empty channels for the device
	 * @param activeDevice (IDevice is the abstract type)
	 */
	public void setupDataChannels(IDevice activeDevice) {
		Channels channels = Channels.getInstance();
		// cleanup existing channels and record sets
		channels.cleanup();

		if (activeDevice != null) {
			// buildup new structure  - set up the channels
			for (int i = 1; i <= activeDevice.getChannelCount(); i++) {
				DeviceSelectionDialog.log.log(Level.FINE, "setting up channels = " + i); //$NON-NLS-1$

				Channel newChannel = new Channel(activeDevice.getChannelName(i), activeDevice.getChannelTypes(i));
				// do not allocate records to record set - newChannel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, activeConfig));
				channels.put(Integer.valueOf(i), newChannel);
				// do not call channel.applyTemplate here, there are no record sets
			}
			channels.switchChannel(1, GDE.STRING_EMPTY); // set " 1 : Ausgang" as default after device switch and update
		}
		this.application.setProgress(0, null);
		this.application.updateGraphicsWindow();
	}

	/**
	 * calculates the new class name for the device
	 */
	public IDevice getInstanceOfDevice() {
		IDevice newInst = null;
		String selectedDeviceName = this.selectedActiveDeviceConfig.getDeviceImplName().replace(GDE.STRING_BLANK, GDE.STRING_EMPTY).replace(GDE.STRING_DASH, GDE.STRING_EMPTY);
		//selectedDeviceName = selectedDeviceName.substring(0, 1).toUpperCase() + selectedDeviceName.substring(1);
		String className = selectedDeviceName.contains(GDE.STRING_DOT) ? selectedDeviceName  // full qualified
				: "gde.device." + this.selectedActiveDeviceConfig.getManufacturer().toLowerCase().replace(GDE.STRING_BLANK, GDE.STRING_EMPTY).replace(GDE.STRING_DASH, GDE.STRING_EMPTY) + "." + selectedDeviceName; //$NON-NLS-1$
		try {
			//String className = "gde.device.DefaultDeviceDialog";
			DeviceSelectionDialog.log.log(Level.FINE, "loading Class " + className); //$NON-NLS-1$
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Class<?> c = loader.loadClass(className);
			//Class c = Class.forName(className);
			Constructor<?> constructor = c.getDeclaredConstructor(new Class[] { DeviceConfiguration.class });
			DeviceSelectionDialog.log.log(Level.FINE, "constructor != null -> " + (constructor != null ? "true" : "false")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (constructor != null) {
				newInst = (IDevice) constructor.newInstance(new Object[] { this.selectedActiveDeviceConfig });
			}
			else
				throw new ClassNotFoundException(Messages.getString(MessageIds.GDE_MSGE0016, new String[] {className}));

		}
		catch (Exception e) {
			DeviceSelectionDialog.log.log(Level.SEVERE, e.getMessage(), e);
			String msg = e.getClass().getSimpleName() + GDE.STRING_BLANK + e.getMessage() + GDE.LINE_SEPARATOR + Messages.getString(MessageIds.GDE_MSGE0040, new String[] {className});
			this.application.openMessageDialog(this.dialogShell, msg);
			
			// in-activate and remove failed device (XML) from potential devices list 
			this.selectedActiveDeviceConfig.setUsed(false);
			this.selectedActiveDeviceConfig.storeDeviceProperties();
			this.activeDevices.remove(selectedDeviceName);
			this.deviceConfigurations.remove(selectedDeviceName);
			if (this.activeDevices.size() > 0) {
				this.selectedActiveDeviceConfig = deviceConfigurations.get(this.activeDevices.firstElement());
			}
			else {
				this.selectedActiveDeviceConfig = null;
			}
			updateDeviceSelectionTable();
			updateDialogEntries();
		}
		return newInst;
	}

	public Vector<String> getActiveDevices() {
		return this.activeDevices;
	}

	public Slider getDeviceSlider() {
		return this.deviceSlider;
	}

	public DeviceConfiguration getActiveConfig() {
		return this.selectedActiveDeviceConfig;
	}

	/**
	 * @param activeConfig the activeConfig to set
	 */
	public void setActiveConfig(DeviceConfiguration activeConfig) {
		this.selectedActiveDeviceConfig = activeConfig;
	}

	/**
	 * @return the devices
	 */
	public TreeMap<String, DeviceConfiguration> getDevices() {
		return this.deviceConfigurations;
	}

	/**
	 * query if the device selection has changed
	 * @return true if device selection changed
	 */
	public boolean isDeviceChanged() {
		IDevice activeDevice = this.application.getActiveDevice();
		boolean isFirstConfigSelected = this.selectedActiveDeviceConfig != null && activeDevice == null; // new configuration selected, but no active device
		@SuppressWarnings("unused")
		boolean isLastConfigDeselected = this.selectedActiveDeviceConfig == null && activeDevice != null; // all configurations deselected, but a device is still selected
		boolean isDeviceSwitched = this.selectedActiveDeviceConfig != null && activeDevice != null && !this.selectedActiveDeviceConfig.getName().equals(activeDevice.getName());
		DeviceSelectionDialog.log.log(Level.FINE, GDE.STRING_EMPTY + (isFirstConfigSelected || isDeviceSwitched));
		return (isFirstConfigSelected || isDeviceSwitched);
	}

	/**
	 * check for data which need to be saved
	 */
	public boolean checkDataSaved() {
		boolean result = true;
		String unsaved = Channels.getInstance().checkRecordSetsSaved();
		if (unsaved.length() != 0) {
			String msg = Messages.getString(MessageIds.GDE_MSGW0020) + unsaved.toString();
			if (this.application.openOkCancelMessageDialog(msg) == SWT.CANCEL) {
				result = false;
				DeviceSelectionDialog.log.log(Level.FINE, "SWT.CANCEL"); //$NON-NLS-1$
			}
		}
		return result;

	}

	/**
	 * query the available serial ports and update the serialPortGroup combo
	 */
	void updateAvailablePorts() {
		// execute independent from dialog UI
		this.listPortsThread = new Thread() {
			@Override
			public void run() {
				try {
					while (!DeviceSelectionDialog.this.dialogShell.isDisposed()) {
						if (DeviceSelectionDialog.this.isUpdateSerialPorts) {
							DeviceSelectionDialog.this.availablePorts = DeviceSerialPort.listConfiguredSerialPorts(DeviceSelectionDialog.this.settings.doPortAvailabilityCheck(), DeviceSelectionDialog.this.settings
									.isSerialPortBlackListEnabled() ? DeviceSelectionDialog.this.settings.getSerialPortBlackList() : GDE.STRING_EMPTY, DeviceSelectionDialog.this.settings
									.isSerialPortWhiteListEnabled() ? DeviceSelectionDialog.this.settings.getSerialPortWhiteList() : new Vector<String>());
							if (DeviceSelectionDialog.this.dialogShell != null && !DeviceSelectionDialog.this.dialogShell.isDisposed()) {
								DataExplorer.display.syncExec(new Runnable() {
									public void run() {
										if (!DeviceSelectionDialog.this.dialogShell.isDisposed() && DeviceSelectionDialog.this.selectedActiveDeviceConfig != null) {
											if (DeviceSelectionDialog.this.availablePorts != null && DeviceSelectionDialog.this.availablePorts.size() > 0) {
												DeviceSelectionDialog.this.portSelectCombo.setItems(StringHelper.prepareSerialPortList(DeviceSelectionDialog.this.availablePorts));
												int index = DeviceSelectionDialog.this.availablePorts.indexOf(DeviceSelectionDialog.this.selectedActiveDeviceConfig.getPort());
												if (index > -1) {
													DeviceSelectionDialog.this.portSelectCombo.select(index);
												}
												else {
													DeviceSelectionDialog.this.portSelectCombo.setText(Messages.getString(MessageIds.GDE_MSGT0197));
												}
											}
											else {
												DeviceSelectionDialog.this.portSelectCombo.setItems(new String[0]);
												DeviceSelectionDialog.this.portSelectCombo.setText(Messages.getString(MessageIds.GDE_MSGT0198));
											}
										}
									}
								});
							}
						}
						try {
							Thread.sleep(2500);
						}
						catch (InterruptedException e) {
							// ignore
						}
					}
				}
				catch (Throwable t) {
					SettingsDialog.log.log(Level.WARNING, t.getMessage(), t);
				}
			}
		};
		this.listPortsThread.start();
	}

	/**
	 * query the number of configured active devices 
	 * @return number of active devices
	 */
	public int getNumberOfActiveDevices() {
		return this.activeDevices.size();
	}

	/**
	 * @return the dialogShell visible status
	 */
	public boolean isDisposed() {
		return dialogShell != null && dialogShell.isDisposed();
	}

	/**
	 * initialize button states etc. of the dialog UI
	 */
	private void initializeUI() {
		if (DeviceSelectionDialog.this.deviceSelectCombo.getSelectionIndex() < 0 && DeviceSelectionDialog.this.deviceSelectCombo.getItemCount() > 0) {
			updateDialogEntries();
		}
		
		if (DeviceSelectionDialog.this.settings.isGlobalSerialPort()  
				|| !DeviceSelectionDialog.this.serialPortSelectionGroup.getEnabled()) {
			DeviceSelectionDialog.this.portDescription.setEnabled(false);
			DeviceSelectionDialog.this.portSelectCombo.setEnabled(false);
		}
		else {
			DeviceSelectionDialog.this.portDescription.setEnabled(true);
			DeviceSelectionDialog.this.portSelectCombo.setEnabled(true);
		}
	}
}
