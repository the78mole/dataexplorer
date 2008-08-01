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
package osde.ui.dialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import osde.OSDE;
import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.exception.NotSupportedException;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Dialog class to select the device to be used
 * @author Winfried Brügmann
 */
public class DeviceSelectionDialog extends org.eclipse.swt.widgets.Dialog {
	final static Logger											log	= Logger.getLogger(DeviceSelectionDialog.class.getName());
	final static String											fileSep = Settings.FILE_SEP;
	
	Shell																	dialogShell;
	CTabItem															cTabItem1;
	Composite															composite2;
	Text																	deviceText;
	TableColumn														tableColumn1, tableColumn2, tableColumn3;
	Table																	deviceTable;
	Group																	deviceGroup;
	Composite															composite1;
	CTabItem															auswahlTabItem;
	Button																closeButton;
	Button																rtsCheckBox;
	Button																dtrCheckBox;
	CLabel																flowControlSelectLabel;
	CLabel																dataBitsSelectLabel;
	CLabel																paritySelectLabel;
	CLabel																stopBitsSelectLabel;
	CLabel																baudeSelectLabel;
	Text																	rtsDescription;
	Text																	dtrDescription;
	Text																	parityDescription;
	Text																	stopbitsDescription;
	Text																	databbitsDescription;
	Text																	flowcontrolDescription;
	Text																	baudeDescription;
	Group																	portSettings;
	CCombo																portSelectCombo;
	Text																	portDescription;
	Group																	portGroup;
	Button																voltagePerCellButton;
	Button																analogTabButton;
	Button																digitalTabButton;
	Button																tableTabButton;
	Group																	desktopTabsGroup;
	Group																	group1;
	Button																openToolBoxCheck;
	Button																openPortCheck;
	Text																	internetLinkText;
	Text																	deviceTypeText;
	Text																	manufacturerName;
	Text																	internetLinkDescription;
	Text																	deviceTypeDescription;
	Text																	deviceNameDescription;
	Text																	manufacturerDescription;
	Canvas																deviceCanvas;
	Text																	deviceGroupText;
	Slider																deviceSlider;
	CCombo																deviceSelectCombo;
	CTabFolder														settingsTabFolder;

	TreeMap<String, DeviceConfiguration>	deviceConfigurations;
	Vector<String>												activeDevices;
	final OpenSerialDataExplorer					application;
	final Settings												settings;
	String																activeDeviceName;
	DeviceConfiguration										selectedActiveDeviceConfig;
	Vector<String>												availablePorts	= new Vector<String>();
	Thread																listPortsThread;

	public DeviceSelectionDialog(Shell parent, int style, final OpenSerialDataExplorer currentApplication) {
		super(parent, style);
		this.application = currentApplication;
		this.settings = Settings.getInstance();
		this.activeDeviceName = this.settings.getActiveDevice();

		try {
			initialize();
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			currentApplication.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGE0007) + e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage());
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

		for (int i = 0; files != null && i < files.length; i++) {
			try {
				// loop through all device properties XML and check if device used
				if (files[i].endsWith(".xml")) {
					String deviceKey = files[i].substring(0, files[i].length() - 4);
					devConfig = new DeviceConfiguration(this.settings.getDevicesPath() + fileSep + files[i]);
					if (devConfig.getName().equals(this.activeDeviceName) && devConfig.isUsed()) { // define the active device after re-start
						this.selectedActiveDeviceConfig = devConfig;
					}
					// add the active once into the active device vector
					if(devConfig.isUsed())
						this.activeDevices.add(devConfig.getName());
					
					// store all device configurations in a map					
					String keyString;
					if (devConfig.getName() != null)
						keyString = devConfig.getName();
					else {
						devConfig.setName(deviceKey);
						keyString = deviceKey;
					}
					log.fine(deviceKey + OSDE.STRING_MESSAGE_CONCAT + keyString);
					this.deviceConfigurations.put(keyString, devConfig);
				}
			}
			catch (Exception e) {
				// ignore exception, but write to std out
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}
		if (this.selectedActiveDeviceConfig == null) this.application.setActiveDevice(null);
	}

	public void open() {
		try {
			Shell parent = getParent();
			this.dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);

			//Register as a resource user - SWTResourceManager will handle the obtaining and disposing of resources
			SWTResourceManager.registerResourceUser(this.dialogShell);

			this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/DeviceSelection.gif")); //$NON-NLS-1$
			this.dialogShell.setLayout(new FormLayout());
			this.dialogShell.setSize(579, 592);
			this.dialogShell.setSize(580, 592);
			this.dialogShell.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.fine("dialogShell.helpRequested, event="+evt); //$NON-NLS-1$
					DeviceSelectionDialog.this.application.openHelpDialog(OSDE.STRING_EMPTY, "HelpInfo_2.html"); //$NON-NLS-1$
				}
			});
			this.dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					log.finest("dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
					// update device configurations if required
					for (String deviceKey : DeviceSelectionDialog.this.deviceConfigurations.keySet().toArray(new String[0])) {
						DeviceConfiguration configuration = DeviceSelectionDialog.this.deviceConfigurations.get(deviceKey);
						if (configuration.isChangePropery()) {
							log.fine(configuration.isChangePropery() + " update device properties for " + configuration.getName()); //$NON-NLS-1$
							configuration.storeDeviceProperties(); // stores only if is changed
						}
					}
					// initialize selected device
					if (isDeviceChanged()) {
						setupDevice();
					}
					else if(DeviceSelectionDialog.this.selectedActiveDeviceConfig == null){ // no device selected
						DeviceSelectionDialog.this.application.setActiveDevice(null);
					}
						
					if (Thread.currentThread().getId() == DeviceSelectionDialog.this.application.getThreadId()) {
						handleAutoOpenAfterClose();
					}
					else {
						OpenSerialDataExplorer.display.asyncExec(new Runnable() {
							public void run() {
								handleAutoOpenAfterClose();
							}
						});
					}
					log.fine("disposed"); //$NON-NLS-1$
				}

				/**
				 * handle auto open functions
				 */
				void handleAutoOpenAfterClose() {
					try {
						if (DeviceSelectionDialog.this.settings.isAutoOpenSerialPort()) {
							if (DeviceSelectionDialog.this.application.getActiveDevice() != null) {
								DeviceSerialPort serialPort = DeviceSelectionDialog.this.application.getActiveDevice().getSerialPort();
								if (serialPort != null && !serialPort.isConnected()) DeviceSelectionDialog.this.application.getActiveDevice().getSerialPort().open();
							}
							if (DeviceSelectionDialog.this.settings.isAutoOpenToolBox()) {
								DeviceSelectionDialog.this.application.openDeviceDialog();
							}
						}
					}
					catch (Exception e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						DeviceSelectionDialog.this.application.openMessageDialogAsync(Messages.getString(MessageIds.OSDE_MSGE0015, new Object[] {e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage()}));
					}
				}
			});
			this.dialogShell.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
			{
				FormData composite1LData = new FormData();
				composite1LData.width = 558;
				composite1LData.height = 559;
				composite1LData.left =  new FormAttachment(0, 1000, 0);
				composite1LData.top =  new FormAttachment(0, 1000, 0);
				composite1LData.right =  new FormAttachment(1000, 1000, 0);
				composite1LData.bottom =  new FormAttachment(1000, 1000, -52);
				this.composite1 = new Composite(this.dialogShell, SWT.NONE);
				FillLayout composite1Layout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.composite1.setLayout(composite1Layout);
				this.composite1.setLayoutData(composite1LData);
				{
					this.settingsTabFolder = new CTabFolder(this.composite1, SWT.BORDER);
					{
						this.cTabItem1 = new CTabItem(this.settingsTabFolder, SWT.NONE);
						this.cTabItem1.setText(Messages.getString(MessageIds.OSDE_MSGT0154));
						{
							this.composite2 = new Composite(this.settingsTabFolder, SWT.NONE);
							this.cTabItem1.setControl(this.composite2);
							this.composite2.setLayout(null);
							{
								this.group1 = new Group(this.composite2, SWT.NONE);
								this.group1.setLayout(null);
								this.group1.setText(Messages.getString(MessageIds.OSDE_MSGT0155));
								this.group1.setBounds(12, 9, 524, 250);
								{
									this.deviceSelectCombo = new CCombo(this.group1, SWT.FLAT | SWT.BORDER);
									this.deviceSelectCombo.setItems(new String[] {Messages.getString(MessageIds.OSDE_MSGT0156)});
									this.deviceSelectCombo.select(0);
									this.deviceSelectCombo.setBounds(12, 20, 375, 22);
									this.deviceSelectCombo.setEditable(false);
									this.deviceSelectCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									this.deviceSelectCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("deviceSelectCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											// allow device switch only if port not connected
											if (DeviceSelectionDialog.this.application.getActiveDevice() == null || DeviceSelectionDialog.this.application.getActiveDevice().getSerialPort() != null && !DeviceSelectionDialog.this.application.getActiveDevice().getSerialPort().isConnected()) { // allow device switch only if port not connected
												DeviceSelectionDialog.this.activeDeviceName = DeviceSelectionDialog.this.deviceSelectCombo.getText();
												log.fine("activeName = " + DeviceSelectionDialog.this.activeDeviceName); //$NON-NLS-1$
												DeviceSelectionDialog.this.selectedActiveDeviceConfig = DeviceSelectionDialog.this.deviceConfigurations.get(DeviceSelectionDialog.this.activeDeviceName);
												// if a device tool box is open, dispose it
												if (DeviceSelectionDialog.this.application.getActiveDevice() != null && !DeviceSelectionDialog.this.application.getDeviceDialog().isDisposed()) {
													DeviceSelectionDialog.this.application.getDeviceDialog().dispose();
												}
												updateDialogEntries();
												DeviceSelectionDialog.this.desktopTabsGroup.redraw();
											}
											else {
												DeviceSelectionDialog.this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGW0017));
											}
										}
									});
								}
								{
									this.deviceSlider = new Slider(this.group1, SWT.HORIZONTAL);
									this.deviceSlider.setBounds(393, 18, 119, 25);
									this.deviceSlider.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									this.deviceSlider.setMinimum(0);
									this.deviceSlider.setMaximum(1);
									this.deviceSlider.setIncrement(1);
									this.deviceSlider.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("deviceSlider.widgetSelected, event=" + evt); //$NON-NLS-1$
											// allow device switch only if port not connected
											if (DeviceSelectionDialog.this.application.getActiveDevice() == null || DeviceSelectionDialog.this.application.getActiveDevice().getSerialPort() != null && !DeviceSelectionDialog.this.application.getActiveDevice().getSerialPort().isConnected()) { // allow device switch only if port not connected
												int position = DeviceSelectionDialog.this.deviceSlider.getSelection();
												log.fine(" Position: " + position); //$NON-NLS-1$
												if (DeviceSelectionDialog.this.activeDevices.size() > 0 && !DeviceSelectionDialog.this.activeDevices.get(position).equals(DeviceSelectionDialog.this.activeDeviceName)) {
													DeviceSelectionDialog.this.activeDeviceName = DeviceSelectionDialog.this.activeDevices.get(position);
													log.fine("activeName = " + DeviceSelectionDialog.this.activeDeviceName); //$NON-NLS-1$
													DeviceSelectionDialog.this.selectedActiveDeviceConfig = DeviceSelectionDialog.this.deviceConfigurations.get(DeviceSelectionDialog.this.activeDeviceName);
													// if a device tool box is open, dispose it
													if (DeviceSelectionDialog.this.application.getDeviceDialog() != null && !DeviceSelectionDialog.this.application.getDeviceDialog().isDisposed()) {
														DeviceSelectionDialog.this.application.getDeviceDialog().dispose();
													}
													updateDialogEntries();
													DeviceSelectionDialog.this.desktopTabsGroup.redraw();
												}
											}
											else {
												DeviceSelectionDialog.this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGW0017));
											}
										}
									});
								}
								{
									this.deviceGroupText = new Text(this.group1, SWT.MULTI | SWT.WRAP);
									this.deviceGroupText.setBounds(12, 48, 430, 16);
									this.deviceGroupText.setText(Messages.getString(MessageIds.OSDE_MSGI0026));
									this.deviceGroupText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
								}
								{
									this.deviceCanvas = new Canvas(this.group1, SWT.NONE);
									this.deviceCanvas.setBounds(12, 70, 227, 165);
									this.deviceCanvas.setBackgroundImage(new Image(this.dialogShell.getDisplay(), this.settings.getDevicesPath() + fileSep + "NoDevicePicture.jpg")); //$NON-NLS-1$
								}
								{
									this.manufacturerDescription = new Text(this.group1, SWT.MULTI | SWT.WRAP);
									this.manufacturerDescription.setBounds(251, 70, 72, 16);
									this.manufacturerDescription.setText(Messages.getString(MessageIds.OSDE_MSGT0157));
									this.manufacturerDescription.setFont(SWTResourceManager.getFont(this.manufacturerDescription, SWT.BOLD));
									this.manufacturerDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
								}
								{
									this.deviceNameDescription = new Text(this.group1, SWT.MULTI | SWT.WRAP);
									this.deviceNameDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									this.deviceNameDescription.setBounds(251, 92, 89, 16);
									this.deviceNameDescription.setFont(SWTResourceManager.getFont(this.manufacturerDescription, SWT.BOLD));
									this.deviceNameDescription.setText(Messages.getString(MessageIds.OSDE_MSGT0158));
								}
								{
									this.deviceTypeDescription = new Text(this.group1, SWT.MULTI | SWT.WRAP);
									this.deviceTypeDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									this.deviceTypeDescription.setBounds(251, 114, 31, 16);
									this.deviceTypeDescription.setFont(SWTResourceManager.getFont(this.manufacturerDescription, SWT.BOLD));
									this.deviceTypeDescription.setText(Messages.getString(MessageIds.OSDE_MSGT0159));
								}
								{
									this.internetLinkDescription = new Text(this.group1, SWT.MULTI | SWT.WRAP);
									this.internetLinkDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									this.internetLinkDescription.setBounds(251, 136, 55, 16);
									this.internetLinkDescription.setFont(SWTResourceManager.getFont(this.manufacturerDescription, SWT.BOLD));
									this.internetLinkDescription.setText(Messages.getString(MessageIds.OSDE_MSGT0160));
								}
								{
									this.manufacturerName = new Text(this.group1, SWT.NONE);
									this.manufacturerName.setBounds(358, 70, 154, 16);
									this.manufacturerName.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
								}
								{
									this.deviceText = new Text(this.group1, SWT.NONE);
									this.deviceText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									this.deviceText.setBounds(358, 92, 154, 16);
								}
								{
									this.deviceTypeText = new Text(this.group1, SWT.NONE);
									this.deviceTypeText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									this.deviceTypeText.setBounds(358, 114, 154, 16);
								}
								{
									this.internetLinkText = new Text(this.group1, SWT.NONE);
									this.internetLinkText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									this.internetLinkText.setBounds(358, 136, 154, 16);
								}
								{
									this.openPortCheck = new Button(this.group1, SWT.CHECK | SWT.LEFT);
									this.openPortCheck.setBounds(251, 176, 236, 16);
									this.openPortCheck.setText(Messages.getString(MessageIds.OSDE_MSGT0161));
									this.openPortCheck.setSelection(this.settings.isAutoOpenSerialPort());
									this.openPortCheck.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("openPortCheck.widgetSelected, event=" + evt); //$NON-NLS-1$
											if (DeviceSelectionDialog.this.openPortCheck.getSelection())
												DeviceSelectionDialog.this.settings.setProperty(Settings.AUTO_OPEN_SERIAL_PORT, "true"); //$NON-NLS-1$
											else
												DeviceSelectionDialog.this.settings.setProperty(Settings.AUTO_OPEN_SERIAL_PORT, "false"); //$NON-NLS-1$
										}
									});
								}
								{
									this.openToolBoxCheck = new Button(this.group1, SWT.CHECK | SWT.LEFT);
									this.openToolBoxCheck.setBounds(251, 198, 186, 16);
									this.openToolBoxCheck.setText(Messages.getString(MessageIds.OSDE_MSGT0162));
									this.openToolBoxCheck.setSelection(this.settings.isAutoOpenToolBox());
									this.openToolBoxCheck.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("openToolBoxCheck.widgetSelected, event=" + evt); //$NON-NLS-1$
											if (DeviceSelectionDialog.this.openToolBoxCheck.getSelection())
												DeviceSelectionDialog.this.settings.setProperty(Settings.AUTO_OPEN_TOOL_BOX, "true"); //$NON-NLS-1$
											else
												DeviceSelectionDialog.this.settings.setProperty(Settings.AUTO_OPEN_TOOL_BOX, "false"); //$NON-NLS-1$
										}
									});
								}
							}
							{
								this.portGroup = new Group(this.composite2, SWT.NONE);
								this.portGroup.setLayout(null);
								this.portGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0163));
								this.portGroup.setBounds(12, 265, 524, 58);
								this.portGroup.addPaintListener(new PaintListener() {
									public void paintControl(PaintEvent evt) {
										log.finest("portGroup.addPaintListener, event=" + evt); //$NON-NLS-1$
										if (DeviceSelectionDialog.this.settings.isGlobalSerialPort()) {
											DeviceSelectionDialog.this.portDescription.setEnabled(false);
											DeviceSelectionDialog.this.portSelectCombo.setEnabled(false);
										}
										else {
											DeviceSelectionDialog.this.portDescription.setEnabled(true);
											DeviceSelectionDialog.this.portSelectCombo.setEnabled(true);
											DeviceSelectionDialog.this.portSelectCombo.setItems(DeviceSelectionDialog.this.availablePorts.toArray(new String[DeviceSelectionDialog.this.availablePorts.size()]));
											int portIndex = 0;
											if (DeviceSelectionDialog.this.selectedActiveDeviceConfig != null && DeviceSelectionDialog.this.availablePorts.size() > 0) {
												portIndex = DeviceSelectionDialog.this.availablePorts.indexOf(DeviceSelectionDialog.this.selectedActiveDeviceConfig.getPort());
												
												if (portIndex < 0) portIndex = 0; // port not found in the list
											  DeviceSelectionDialog.this.portSelectCombo.select(portIndex);
											}
										}
									}
								});
								{
									this.portDescription = new Text(this.portGroup, SWT.NONE);
									this.portDescription.setText(Messages.getString(MessageIds.OSDE_MSGT0164));
									this.portDescription.setBounds(30, 29, 183, 18);
									this.portDescription.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0165));
									this.portDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
								}
								{
									this.portSelectCombo = new CCombo(this.portGroup, SWT.FLAT | SWT.BORDER);
									this.portSelectCombo.setBounds(249, 27, 205, 22);
									this.portSelectCombo.setEditable(false);
									this.portSelectCombo.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0165));
									this.portSelectCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									this.portSelectCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("portSelectCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											DeviceSelectionDialog.this.selectedActiveDeviceConfig.setPort(DeviceSelectionDialog.this.portSelectCombo.getText());
											DeviceSelectionDialog.this.selectedActiveDeviceConfig.storeDeviceProperties();
											DeviceSelectionDialog.this.application.updateTitleBar(DeviceSelectionDialog.this.selectedActiveDeviceConfig.getName(), DeviceSelectionDialog.this.selectedActiveDeviceConfig.getPort());
										}
									});
								}
							}
							{
								this.portSettings = new Group(this.composite2, SWT.NONE);
								this.portSettings.setLayout(null);
								this.portSettings.setText(Messages.getString(MessageIds.OSDE_MSGT0166));
								this.portSettings.setBounds(12, 409, 524, 115);
								{
									this.baudeDescription = new Text(this.portSettings, SWT.NONE);
									this.baudeDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									this.baudeDescription.setText(Messages.getString(MessageIds.OSDE_MSGT0167));
									this.baudeDescription.setBounds(8, 21, 100, 16);
								}
								{
									this.stopbitsDescription = new Text(this.portSettings, SWT.NONE);
									this.stopbitsDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									this.stopbitsDescription.setText(Messages.getString(MessageIds.OSDE_MSGT0168));
									this.stopbitsDescription.setBounds(8, 65, 100, 16);
								}
								{
									this.flowcontrolDescription = new Text(this.portSettings, SWT.NONE);
									this.flowcontrolDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									this.flowcontrolDescription.setText(Messages.getString(MessageIds.OSDE_MSGT0169));
									this.flowcontrolDescription.setBounds(261, 21, 100, 16);
								}
								{
									this.databbitsDescription = new Text(this.portSettings, SWT.NONE);
									this.databbitsDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									this.databbitsDescription.setText(Messages.getString(MessageIds.OSDE_MSGT0170));
									this.databbitsDescription.setBounds(8, 43, 100, 16);
								}
								{
									this.parityDescription = new Text(this.portSettings, SWT.NONE);
									this.parityDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									this.parityDescription.setBounds(8, 86, 100, 16);
									this.parityDescription.setText(Messages.getString(MessageIds.OSDE_MSGT0171));
								}
								{
									this.dtrDescription = new Text(this.portSettings, SWT.NONE);
									this.dtrDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									this.dtrDescription.setText(Messages.getString(MessageIds.OSDE_MSGT0172));
									this.dtrDescription.setBounds(261, 49, 105, 17);
								}
								{
									this.rtsDescription = new Text(this.portSettings, SWT.NONE);
									this.rtsDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									this.rtsDescription.setText(Messages.getString(MessageIds.OSDE_MSGT0173));
									this.rtsDescription.setBounds(261, 72, 77, 17);
								}
								{
									this.baudeSelectLabel = new CLabel(this.portSettings, SWT.RIGHT);
									FillLayout cLabel1Layout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
									this.baudeSelectLabel.setLayout(cLabel1Layout);
									this.baudeSelectLabel.setBounds(115, 23, 90, 19);
									this.baudeSelectLabel.setEnabled(false);
								}
								{
									this.stopBitsSelectLabel = new CLabel(this.portSettings, SWT.RIGHT);
									this.stopBitsSelectLabel.setBounds(115, 67, 90, 19);
									this.stopBitsSelectLabel.setEnabled(false);
								}
								{
									this.paritySelectLabel = new CLabel(this.portSettings, SWT.RIGHT);
									this.paritySelectLabel.setBounds(115, 89, 90, 19);
									this.paritySelectLabel.setEnabled(false);
								}
								{
									this.dataBitsSelectLabel = new CLabel(this.portSettings, SWT.RIGHT);
									this.dataBitsSelectLabel.setBounds(115, 45, 90, 19);
									this.dataBitsSelectLabel.setEnabled(false);
								}
								{
									this.flowControlSelectLabel = new CLabel(this.portSettings, SWT.LEFT);
									this.flowControlSelectLabel.setBounds(372, 21, 90, 19);
									this.flowControlSelectLabel.setEnabled(false);
								}
								{
									this.dtrCheckBox = new Button(this.portSettings, SWT.CHECK | SWT.LEFT);
									this.dtrCheckBox.setBounds(372, 49, 92, 17);
									this.dtrCheckBox.setText(Messages.getString(MessageIds.OSDE_MSGT0174));
									this.dtrCheckBox.setEnabled(false);
								}
								{
									this.rtsCheckBox = new Button(this.portSettings, SWT.CHECK | SWT.LEFT);
									this.rtsCheckBox.setBounds(372, 72, 102, 17);
									this.rtsCheckBox.setText(Messages.getString(MessageIds.OSDE_MSGT0175));
									this.rtsCheckBox.setEnabled(false);
								}
							}
							{
								this.desktopTabsGroup = new Group(this.composite2, SWT.NONE);
								this.desktopTabsGroup.setLayout(null);
								this.desktopTabsGroup.setBounds(12, 329, 524, 74);
								this.desktopTabsGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0176));
								this.desktopTabsGroup.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0177));
								this.desktopTabsGroup.addPaintListener(new PaintListener() {
									public void paintControl(PaintEvent evt) {
										log.finest("desktopTabsGroup.paintControl, event="+evt); //$NON-NLS-1$
										if (DeviceSelectionDialog.this.selectedActiveDeviceConfig != null) {
											DeviceSelectionDialog.this.tableTabButton.setSelection(DeviceSelectionDialog.this.selectedActiveDeviceConfig.isTableTabRequested());
											DeviceSelectionDialog.this.digitalTabButton.setSelection(DeviceSelectionDialog.this.selectedActiveDeviceConfig.isDigitalTabRequested());
											DeviceSelectionDialog.this.analogTabButton.setSelection(DeviceSelectionDialog.this.selectedActiveDeviceConfig.isAnalogTabRequested());
											DeviceSelectionDialog.this.voltagePerCellButton.setSelection(DeviceSelectionDialog.this.selectedActiveDeviceConfig.isVoltagePerCellTabRequested());
										}
										else {
											DeviceSelectionDialog.this.tableTabButton.setSelection(false);
											DeviceSelectionDialog.this.digitalTabButton.setSelection(false);
											DeviceSelectionDialog.this.analogTabButton.setSelection(false);
											DeviceSelectionDialog.this.voltagePerCellButton.setSelection(false);
										}
									}
								});
								{
									this.tableTabButton = new Button(this.desktopTabsGroup, SWT.CHECK | SWT.LEFT);
									this.tableTabButton.setBounds(12, 30, 110, 25);
									this.tableTabButton.setText(Messages.getString(MessageIds.OSDE_MSGT0178));
									this.tableTabButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0179));
									this.tableTabButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("tableTabButton.widgetSelected, event="+evt); //$NON-NLS-1$
											DeviceSelectionDialog.this.selectedActiveDeviceConfig.setTableTabRequested(DeviceSelectionDialog.this.tableTabButton.getSelection());
										}
									});
								}
								{
									this.digitalTabButton = new Button(this.desktopTabsGroup, SWT.CHECK | SWT.LEFT);
									this.digitalTabButton.setBounds(141, 30, 110, 25);
									this.digitalTabButton.setText(Messages.getString(MessageIds.OSDE_MSGT0180));
									this.digitalTabButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0181));
									this.digitalTabButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("digitalTabButton.widgetSelected, event="+evt); //$NON-NLS-1$
											DeviceSelectionDialog.this.selectedActiveDeviceConfig.setDigitalTabRequested(DeviceSelectionDialog.this.digitalTabButton.getSelection());
										}
									});
								}
								{
									this.analogTabButton = new Button(this.desktopTabsGroup, SWT.CHECK | SWT.LEFT);
									this.analogTabButton.setBounds(272, 30, 110, 25);
									this.analogTabButton.setText(Messages.getString(MessageIds.OSDE_MSGT0182));
									this.analogTabButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0183));
									this.analogTabButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("analogTabButton.widgetSelected, event="+evt); //$NON-NLS-1$
											DeviceSelectionDialog.this.selectedActiveDeviceConfig.setAnalogTabRequested(DeviceSelectionDialog.this.analogTabButton.getSelection());
										}
									});
								}
								{
									this.voltagePerCellButton = new Button(this.desktopTabsGroup, SWT.CHECK | SWT.LEFT);
									this.voltagePerCellButton.setBounds(402, 30, 110, 25);
									this.voltagePerCellButton.setText(Messages.getString(MessageIds.OSDE_MSGT0184));
									this.voltagePerCellButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0185));
									this.voltagePerCellButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("cellVoltageButton.widgetSelected, event="+evt); //$NON-NLS-1$
											DeviceSelectionDialog.this.selectedActiveDeviceConfig.setVoltagePerCellTabRequested(DeviceSelectionDialog.this.voltagePerCellButton.getSelection());
										}
									});
								}
							}
						}
					}
					{
						this.auswahlTabItem = new CTabItem(this.settingsTabFolder, SWT.NONE);
						this.auswahlTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0186));
						{
							this.deviceGroup = new Group(this.settingsTabFolder, SWT.NONE);
							this.deviceGroup.setLayout(null);
							this.auswahlTabItem.setControl(this.deviceGroup);
							this.deviceGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0187));
							{
								this.deviceTable = new Table(this.deviceGroup, SWT.MULTI | SWT.CHECK);
								this.deviceTable.setBounds(17, 40, 497, 401);
								this.deviceTable.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										TableItem item = (TableItem) evt.item;
										String deviceName = ((TableItem) evt.item).getText();
										DeviceConfiguration tmpDeviceConfiguration;
										if (item.getChecked()) {
											log.fine("add device = " + deviceName); //$NON-NLS-1$
											tmpDeviceConfiguration = DeviceSelectionDialog.this.deviceConfigurations.get(deviceName);
											tmpDeviceConfiguration.setUsed(true);
											//tmpDeviceConfiguration.storeDeviceProperties();
											DeviceSelectionDialog.this.deviceConfigurations.put(deviceName, tmpDeviceConfiguration);
											if (!DeviceSelectionDialog.this.activeDevices.contains(deviceName))
												DeviceSelectionDialog.this.activeDevices.add(deviceName);
											if (DeviceSelectionDialog.this.activeDevices.size() == 1) {
												DeviceSelectionDialog.this.selectedActiveDeviceConfig = DeviceSelectionDialog.this.deviceConfigurations.get(deviceName);
											}
										}
										else {
											log.fine("remove device = " + deviceName); //$NON-NLS-1$
											tmpDeviceConfiguration = DeviceSelectionDialog.this.deviceConfigurations.get(deviceName);
											tmpDeviceConfiguration.setUsed(false);
											//tmpDeviceConfiguration.storeDeviceProperties();
											if (DeviceSelectionDialog.this.activeDevices.contains(deviceName))
												DeviceSelectionDialog.this.activeDevices.remove(deviceName);
											
											// the removed configuration is the active one
											if (DeviceSelectionDialog.this.selectedActiveDeviceConfig != null && DeviceSelectionDialog.this.selectedActiveDeviceConfig.getName().equals(deviceName)) {
												// take first available
												if (DeviceSelectionDialog.this.activeDevices.size() >  0) {
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
									this.deviceTable.removeAll();

									for (String deviceKey : this.deviceConfigurations.keySet()) {
										log.fine(deviceKey);
										DeviceConfiguration config = this.deviceConfigurations.get(deviceKey);

										TableItem item = new TableItem(this.deviceTable, SWT.NULL);
										item.setText(new String[] { config.getName(), config.getManufacturer(), config.getPort() });
										if (new Boolean(config.isUsed())) {
											item.setChecked(true);
										}
										else {
											item.setChecked(false);
										}
									}
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
				closeButtonLData.left =  new FormAttachment(0, 1000, 12);
				closeButtonLData.top =  new FormAttachment(0, 1000, 573);
				closeButtonLData.width = 529;
				closeButtonLData.height = 26;
				this.closeButton.setLayoutData(closeButtonLData);
				this.closeButton.setText(Messages.getString(MessageIds.OSDE_MSGT0188));
				this.closeButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.fine("closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						DeviceSelectionDialog.this.dialogShell.dispose();
					}
				});
			}
			updateDialogEntries(); // update all the entries according active device configuration			
			updateAvailablePorts();
			this.dialogShell.layout();
			this.dialogShell.pack();
			this.dialogShell.setSize(566, 644);
			this.dialogShell.setText(Messages.getString(MessageIds.OSDE_MSGT0189));
			this.dialogShell.setLocation(getParent().toDisplay(100, 100));
			this.dialogShell.open();
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGE0007) + e.getMessage());
		}
	}

	/**
	 * update entries according configuration, this is called whenever a new device is selected
	 */
	void updateDialogEntries() {
		//updateAvailablePorts();
		// device selection
		log.fine("active devices " + this.activeDevices.toString()); //$NON-NLS-1$
		String[] list = this.activeDevices.toArray(new String[this.activeDevices.size()]);
		Arrays.sort(list); // this sorts the list but not the vector
		//get sorted devices list and activeDevices array in sync
		this.activeDevices.removeAllElements();
		for (String device : list) {
			this.activeDevices.add(device);
		}

		if (list.length > 0) {
			this.activeDeviceName = (this.selectedActiveDeviceConfig == null) ? OSDE.STRING_EMPTY : this.selectedActiveDeviceConfig.getName();
			this.deviceSelectCombo.setItems(list);
			this.deviceSelectCombo.setVisibleItemCount(this.activeDevices.size());
			this.deviceSelectCombo.select(this.activeDevices.indexOf(this.activeDeviceName));
			log.fine(this.activeDeviceName + OSDE.STRING_MESSAGE_CONCAT + this.activeDevices.indexOf(this.activeDeviceName));

			this.deviceSlider.setMaximum(this.activeDevices.size());
			this.deviceSlider.setSelection(this.activeDevices.indexOf(this.activeDeviceName));
			log.fine("activeDevices.size() " + this.activeDevices.size()); //$NON-NLS-1$
			if (this.activeDevices.size() > 1) 	this.application.enableDeviceSwitchButtons(true);
			else																this.application.enableDeviceSwitchButtons(false);

		}
		else { // no active device
			this.selectedActiveDeviceConfig = null;
			this.activeDeviceName = (this.selectedActiveDeviceConfig == null) ? OSDE.STRING_EMPTY : this.selectedActiveDeviceConfig.getName();
			this.deviceSelectCombo.setItems(new String[] {Messages.getString(MessageIds.OSDE_MSGT0190)});
			this.deviceSelectCombo.setVisibleItemCount(1);
			this.deviceSelectCombo.select(0);
			log.fine("no active device"); //$NON-NLS-1$

			this.deviceSlider.setMaximum(1);
			this.deviceSlider.setIncrement(0);
			this.deviceSlider.setSelection(0);
			log.fine("activeDevices.size() = 0"); //$NON-NLS-1$
			this.application.updateTitleBar(Settings.EMPTY, Settings.EMPTY);
		}	

		// update all serial Port settings
		if (this.selectedActiveDeviceConfig == null) {
			log.fine("activeDeviceConfig == null -> no device selected as active"); //$NON-NLS-1$
			this.application.setActiveDevice(null);
			this.deviceCanvas.setBackgroundImage(new Image(this.dialogShell.getDisplay(), this.settings.getDevicesPath() + fileSep + "NoDevicePicture.jpg")); //$NON-NLS-1$

			this.manufacturerName.setText(Settings.EMPTY);
			this.deviceText.setText(Settings.EMPTY);
			this.deviceTypeText.setText(Settings.EMPTY);
			this.internetLinkText.setText(Settings.EMPTY);

			this.portSelectCombo.setItems(this.availablePorts.toArray(new String[this.availablePorts.size()]));
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
			log.fine(this.settings.getDevicesPath() + this.selectedActiveDeviceConfig.getImageFileName());
			this.deviceCanvas.setBackgroundImage(new Image(this.dialogShell.getDisplay(), this.settings.getDevicesPath() + fileSep + this.selectedActiveDeviceConfig.getImageFileName()));

			this.manufacturerName.setText(this.selectedActiveDeviceConfig.getManufacturer());
			this.deviceText.setText(this.selectedActiveDeviceConfig.getName());
			this.deviceTypeText.setText(this.selectedActiveDeviceConfig.getDeviceGroup());
			String link = this.selectedActiveDeviceConfig.getManufacturerURL() != null ? this.selectedActiveDeviceConfig.getManufacturerURL() : Messages.getString(MessageIds.OSDE_MSGT0191);
			this.internetLinkText.setText(link);

			this.portSelectCombo.setItems(this.availablePorts.toArray(new String[this.availablePorts.size()]));
			int portIndex = this.availablePorts.indexOf(this.selectedActiveDeviceConfig.getPort()); // -1 means not available
			if(portIndex < 0 && this.availablePorts.size() > 0) {
				this.selectedActiveDeviceConfig.setPort(this.availablePorts.firstElement());
				this.selectedActiveDeviceConfig.setPort(this.availablePorts.firstElement());
			}
			this.portSelectCombo.select(this.availablePorts.indexOf(this.selectedActiveDeviceConfig.getPort()));

			// com port adjustments group
			this.baudeSelectLabel.setText(new Integer(this.selectedActiveDeviceConfig.getBaudeRate()).toString());
			this.dataBitsSelectLabel.setText(new Integer(this.selectedActiveDeviceConfig.getDataBits()).toString());
			this.stopBitsSelectLabel.setText(new Integer(this.selectedActiveDeviceConfig.getStopBits()).toString());
			this.paritySelectLabel.setText(new Integer(this.selectedActiveDeviceConfig.getParity()).toString());
			this.flowControlSelectLabel.setText(new Integer(this.selectedActiveDeviceConfig.getFlowCtrlMode()).toString());
			this.dtrCheckBox.setSelection(this.selectedActiveDeviceConfig.isDTR());
			this.rtsCheckBox.setSelection(this.selectedActiveDeviceConfig.isRTS());

			this.application.updateTitleBar(this.selectedActiveDeviceConfig.getName(), this.selectedActiveDeviceConfig.getPort());
		}
	}

	/**
	 * check if the configure com port matches system available
	 */
	public boolean checkPortSelection() {
		boolean matches = true;
		if (this.availablePorts == null) {
			this.availablePorts = DeviceSerialPort.listConfiguredSerialPorts();
		}

		if (this.settings.isGlobalSerialPort() && this.availablePorts.indexOf(this.settings.getSerialPort()) < 0) {
			this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGW0018));
			matches = false;
		}
		else if (!this.settings.isGlobalSerialPort() && (this.selectedActiveDeviceConfig != null && this.availablePorts.indexOf(this.selectedActiveDeviceConfig.getPort()) < 0)) {
			this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGW0019));
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
		int selection = this.getActiveDevices().indexOf(newDeviceName);
		if (selection != -1 && this.getDevices().containsKey(newDeviceName)) {
			this.setActiveConfig(this.getDevices().get(this.getActiveDevices().get(selection)));
			this.setupDevice();
		}
		else {
			String msg = Messages.getString(MessageIds.OSDE_MSGI0027, new Object[] { newDeviceName });
			NotSupportedException e = new NotSupportedException(msg);
			log.log(Level.WARNING, e.getMessage(), e);
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
		// prepare every thing for the new device
		activeDevice = this.getInstanceOfDevice();
		this.application.setActiveDevice(activeDevice);
		setupDataChannels(activeDevice);
		this.application.setupDataTableHeader();
		this.application.updateDigitalWindow();
		this.application.updateAnalogWindow();
		this.application.updateCellVoltageWindow();
		this.application.updateCompareWindow();
		this.application.updateFileCommentWindow();
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
				log.fine("setting up channels = " + i); //$NON-NLS-1$

				Channel newChannel = new Channel(i, activeDevice.getChannelName(i), activeDevice.getChannelType(i));
				// do not allocate records to record set - newChannel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, activeConfig));
				channels.put(new Integer(i), newChannel);
				// do not call channel.applyTemplate here, there are no record sets
			}
			channels.switchChannel(1, OSDE.STRING_EMPTY); // set " 1 : Ausgang" as default after device switch and update
		}
		this.application.updateGraphicsWindow();
	}

	/**
	 * calculates the new class name for the device
	 */
	@SuppressWarnings("unchecked") //$NON-NLS-1$
	public IDevice getInstanceOfDevice() {
		IDevice newInst = null;
		String selectedDeviceName = this.selectedActiveDeviceConfig.getName().replace(OSDE.STRING_BLANK, OSDE.STRING_EMPTY).replace(OSDE.STRING_DASH, OSDE.STRING_EMPTY);
		//selectedDeviceName = selectedDeviceName.substring(0, 1).toUpperCase() + selectedDeviceName.substring(1);
		String className = "osde.device." + this.selectedActiveDeviceConfig.getManufacturer().toLowerCase().replace(OSDE.STRING_BLANK, OSDE.STRING_EMPTY).replace(OSDE.STRING_DASH, OSDE.STRING_EMPTY) + "." + selectedDeviceName; //$NON-NLS-1$
		try {
			//String className = "osde.device.DefaultDeviceDialog";
			log.fine("loading Class " + className); //$NON-NLS-1$
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Class c = loader.loadClass(className);
			//Class c = Class.forName(className);
			Constructor constructor = c.getDeclaredConstructor(new Class[] { DeviceConfiguration.class });
			log.fine("constructor != null -> " + (constructor != null ? "true" : "false")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (constructor != null) {
				newInst = (IDevice) constructor.newInstance(new Object[] { this.selectedActiveDeviceConfig });
			}
			else throw new NoClassDefFoundError(Messages.getString(MessageIds.OSDE_MSGE0016));

		}
		catch (NoClassDefFoundError e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(e.getMessage() + className);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialog(e.getClass().getSimpleName() + OSDE.STRING_BLANK + e.getMessage());
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
		@SuppressWarnings("unused") //$NON-NLS-1$
		boolean isLastConfigDeselected = this.selectedActiveDeviceConfig == null && activeDevice != null; // all configurations deselected, but a device is still selected
		boolean isDeviceSwitched = this.selectedActiveDeviceConfig != null && activeDevice != null && !this.selectedActiveDeviceConfig.getName().equals(activeDevice.getName());
		log.fine(OSDE.STRING_EMPTY + (isFirstConfigSelected || isDeviceSwitched));
		return (isFirstConfigSelected || isDeviceSwitched);
	}
	
	/**
	 * check for data which need to be saved
	 */
	public boolean checkDataSaved() {
		boolean result = true;
		String unsaved = Channels.getInstance().checkRecordSetsSaved();
		if (unsaved.length() != 0) {
			String msg = Messages.getString(MessageIds.OSDE_MSGW0020) + unsaved.toString();
			if (this.application.openOkCancelMessageDialog(msg) == SWT.CANCEL) {
				result = false;
				log.fine("SWT.CANCEL"); //$NON-NLS-1$
			}
		}
		return result;
		
	}
	
	private void updateAvailablePorts() {
		// execute independent from dialog UI
		this.listPortsThread = new Thread() {
			public void run() {
				while (!DeviceSelectionDialog.this.dialogShell.isDisposed()) {
					log.fine("updateAvailablePorts() - entry"); //$NON-NLS-1$
					DeviceSelectionDialog.this.availablePorts = DeviceSerialPort.listConfiguredSerialPorts();
					if (DeviceSelectionDialog.this.availablePorts != null && DeviceSelectionDialog.this.availablePorts.size() > 0) {
						OpenSerialDataExplorer.display.asyncExec(new Runnable() {
							public void run() {
								if (!DeviceSelectionDialog.this.dialogShell.isDisposed()) 
									DeviceSelectionDialog.this.portGroup.redraw();
							}
						});
					}
					log.fine("updateAvailablePorts() - exit"); //$NON-NLS-1$
					try {
						Thread.sleep(2500);
					}
					catch (InterruptedException e) {
						// ignore
					}
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
}

