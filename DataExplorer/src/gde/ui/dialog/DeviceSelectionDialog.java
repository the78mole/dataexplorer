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

import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.serial.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Dialog class to select the device to be used
 * @author Winfried Brügmann
 */
public class DeviceSelectionDialog extends org.eclipse.swt.widgets.Dialog {
	private Logger																log							= Logger.getLogger(this.getClass().getName());
	private String																fileSep					= System.getProperty("file.separator");
	private Shell																	dialogShell;
	private CTabItem															cTabItem1;
	private Composite															composite2;
	private Text																	deviceText;
	private TableColumn														tableColumn1, tableColumn2, tableColumn3;
	private Table																	deviceTable;
	private Group																	deviceGroup;
	private Composite															composite1;
	private CTabItem															auswahlTabItem;
	private Button																closeButton;
	private Button																rtsCheckBox;
	private Button																dtrCheckBox;
	private CLabel																flowControlSelectLabel;
	private CLabel																dataBitsSelectLabel;
	private CLabel																paritySelectLabel;
	private CLabel																stopBitsSelectLabel;
	private CLabel																baudeSelectLabel;
	private Text																	rtsDescription;
	private Text																	dtrDescription;
	private Text																	parityDescription;
	private Text																	stopbitsDescription;
	private Text																	databbitsDescription;
	private Text																	flowcontrolDescription;
	private Text																	baudeDescription;
	private Group																	portSettings;
	private CCombo																portSelectCombo;
	private Text																	portDescription;
	private Group																	portGroup;
	private Button																voltagePerCellButton;
	private Button																analogTabButton;
	private Button																digitalTabButton;
	private Button																tableTabButton;
	private Group																	desktopTabsGroup;
	private Group																	group1;
	private Button																openToolBoxCheck;
	private Button																openPortCheck;
	private Text																	internetLinkText;
	private Text																	deviceTypeText;
	private Text																	herstellerText;
	private Text																	internetLinkDescription;
	private Text																	deviceTypeDescription;
	private Text																	deviceNameDescription;
	private Text																	herstellerDescription;
	private Canvas																deviceCanvas;
	private Text																	deviceGroupText;
	private Slider																deviceSlider;
	private CCombo																deviceSelectCombo;
	private CTabFolder														einstellungenTabFolder;

	private TreeMap<String, DeviceConfiguration>	deviceConfigurations;
	private Vector<String>												activeDevices;
	private final OpenSerialDataExplorer					application;
	private final Settings												settings;
	private String																activeDeviceName;
	private DeviceConfiguration										activeDeviceConfig;
	private IDevice																activeDevice;
	private Vector<String>												availablePorts	= null;

	public DeviceSelectionDialog(Shell parent, int style, final OpenSerialDataExplorer application) {
		super(parent, style);
		this.application = application;
		this.settings = Settings.getInstance();
		this.activeDeviceName = this.settings.getActiveDevice();

		try {
			initialize();
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			application.openMessageDialog("Es ist ein Fehler aufgetreten : " + e.getMessage());
		}
	}

	/**
	 * goes through the existing INI files and set active flagged devices into active devices list
	 * @return DeviceConfiguration (if no device set it returns NULL 
	 * @throws FileNotFoundException 
	 */
	public void initialize() throws FileNotFoundException {
		
		File file = new File(settings.getDevicesPath());
		if (!file.exists()) throw new FileNotFoundException(settings.getDevicesPath());
		String[] files = file.list();
		DeviceConfiguration devConfig;
		deviceConfigurations = new TreeMap<String, DeviceConfiguration>(String.CASE_INSENSITIVE_ORDER);
		activeDevices = new Vector<String>(2, 1);

		for (int i = 0; files != null && i < files.length; i++) {
			try {
				// loop through all device properties XML and check if device used
				if (files[i].endsWith(".xml")) {
					String deviceKey = files[i].substring(0, files[i].length() - 4);
					devConfig = new DeviceConfiguration(settings.getDevicesPath() + fileSep + files[i]);
					if (devConfig.getName().equals(activeDeviceName)) { // define the active device after re-start
						activeDeviceConfig = devConfig;
					}
					// add the active once into the active device vector
					if(devConfig.isUsed())
						activeDevices.add(devConfig.getName());
					
					// store all device configurations in a map					
					String keyString;
					if (devConfig.getName() != null)
						keyString = devConfig.getName();
					else {
						devConfig.setName(deviceKey);
						keyString = deviceKey;
					}
					log.fine(deviceKey + " - " + keyString);
					deviceConfigurations.put(keyString, devConfig);
				}
			}
			catch (Exception e) {
				// ignore exception, but write to std out
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}

	public IDevice open() {
		try {
			Shell parent = getParent();
			dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

			//Register as a resource user - SWTResourceManager will handle the obtaining and disposing of resources
			SWTResourceManager.registerResourceUser(dialogShell);

			dialogShell.setImage(SWTResourceManager.getImage("osde/resource/DeviceSelection.gif"));
			dialogShell.setLayout(new FormLayout());
			dialogShell.setSize(579, 592);
			dialogShell.setSize(580, 592);
			dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					log.finest("dialogShell.widgetDisposed, event=" + evt);
					// update device configurations if required
					for (String deviceKey : deviceConfigurations.keySet().toArray(new String[0])) {
						DeviceConfiguration configuration = deviceConfigurations.get(deviceKey);
						if (configuration.isChangePropery()) log.fine(configuration.isChangePropery() + " update device properties for " + configuration.getName());
						configuration.storeDeviceProperties(); // stores only if is changed
					}
					// initialize selected device
					log.fine("activeDeviceConfig = " + activeDeviceConfig + " activeDevice = " + activeDevice + " application.getActiveDevice() = " + application.getActiveDevice());
					if (activeDeviceConfig != null && activeDevice != null) {
						if (!activeDevice.getName().equals(activeDeviceConfig.getName())) {// device changed
							settings.setActiveDevice(activeDeviceConfig.getName() + ";" + activeDeviceConfig.getManufacturer() + ";" + activeDeviceConfig.getPort());
							setupDevice();
						}
					}
					else{ // no device selected
						settings.setActiveDevice(Settings.EMPTY_SIGNATURE);
						application.setActiveDevice(null);
						activeDevice = null;
					}
						
					if (Thread.currentThread().getId() == application.getThreadId()) {
						handleAutoOpenAfterClose();
					}
					else {
						OpenSerialDataExplorer.display.asyncExec(new Runnable() {
							public void run() {
								handleAutoOpenAfterClose();
							}
						});
					}
					log.fine("disposed");
				}

				/**
				 * handle auto open functions
				 */
				private void handleAutoOpenAfterClose() {
					try {
						if (settings.isAutoOpenSerialPort()) {
							if (application.getActiveDevice() != null) {
								DeviceSerialPort serialPort = application.getActiveDevice().getSerialPort();
								if (serialPort != null && !serialPort.isConnected()) application.getActiveDevice().getSerialPort().open();
							}
							if (settings.isAutoOpenToolBox()) {
								application.openDeviceDialog();
							}
						}
					}
					catch (Exception e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						application.openMessageDialogAsync("Der serielle Port kann nicht geöffnet werden -> " + e.getClass().getCanonicalName() + " - " + e.getMessage() + "\n Bitte die Porteinstellung überprüfen.");
					}
				}
			});
			dialogShell.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
			{
				FormData composite1LData = new FormData();
				composite1LData.width = 558;
				composite1LData.height = 559;
				composite1LData.left =  new FormAttachment(0, 1000, 0);
				composite1LData.top =  new FormAttachment(0, 1000, 0);
				composite1LData.right =  new FormAttachment(1000, 1000, 0);
				composite1LData.bottom =  new FormAttachment(1000, 1000, -52);
				composite1 = new Composite(dialogShell, SWT.NONE);
				FillLayout composite1Layout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
				composite1.setLayout(composite1Layout);
				composite1.setLayoutData(composite1LData);
				{
					einstellungenTabFolder = new CTabFolder(composite1, SWT.BORDER);
					{
						cTabItem1 = new CTabItem(einstellungenTabFolder, SWT.NONE);
						cTabItem1.setText("Einstellungen");
						{
							composite2 = new Composite(einstellungenTabFolder, SWT.NONE);
							cTabItem1.setControl(composite2);
							composite2.setLayout(null);
							{
								group1 = new Group(composite2, SWT.NONE);
								group1.setLayout(null);
								group1.setText("Gerät");
								group1.setBounds(12, 9, 524, 250);
								{
									deviceSelectCombo = new CCombo(group1, SWT.FLAT | SWT.BORDER);
									deviceSelectCombo.setItems(new String[] {"-- no device selected --"});
									deviceSelectCombo.select(0);
									deviceSelectCombo.setBounds(12, 20, 375, 22);
									deviceSelectCombo.setEditable(false);
									deviceSelectCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									deviceSelectCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("deviceSelectCombo.widgetSelected, event=" + evt);
											// allow device switch only if port not connected
											if (application.getActiveDevice() == null || application.getActiveDevice().getSerialPort() != null && !application.getActiveDevice().getSerialPort().isConnected()) { // allow device switch only if port not connected
												activeDeviceName = deviceSelectCombo.getText();
												log.fine("activeName = " + activeDeviceName);
												activeDeviceConfig = deviceConfigurations.get(activeDeviceName);
												// if a device tool box is open, dispose it
												if (application.getActiveDevice() != null && !application.getDeviceDialog().isDisposed()) {
													application.getDeviceDialog().dispose();
												}
												updateDialogEntries();
												desktopTabsGroup.redraw();
											}
											else {
												application.openMessageDialog("Das Gerät kann nicht gewechselt werden, solange der serielle Port geöffnet ist!");
											}
										}
									});
								}
								{
									deviceSlider = new Slider(group1, SWT.HORIZONTAL);
									deviceSlider.setBounds(393, 18, 119, 25);
									deviceSlider.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									deviceSlider.setMinimum(0);
									deviceSlider.setMaximum(1);
									deviceSlider.setIncrement(1);
									deviceSlider.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("deviceSlider.widgetSelected, event=" + evt);
											// allow device switch only if port not connected
											if (application.getActiveDevice() == null || application.getActiveDevice().getSerialPort() != null && !application.getActiveDevice().getSerialPort().isConnected()) { // allow device switch only if port not connected
												int position = deviceSlider.getSelection();
												log.fine(" Position: " + position);
												if (activeDevices.size() > 0 && !activeDevices.get(position).equals(activeDeviceName)) {
													activeDeviceName = activeDevices.get(position);
													log.fine("activeName = " + activeDeviceName);
													activeDeviceConfig = deviceConfigurations.get(activeDeviceName);
													// if a device tool box is open, dispose it
													if (application.getDeviceDialog() != null && !application.getDeviceDialog().isDisposed()) {
														application.getDeviceDialog().dispose();
													}
													updateDialogEntries();
													desktopTabsGroup.redraw();
												}
											}
											else {
												application.openMessageDialog("Das Gerät kann nicht gewechselt werden, solange der serielle Port geöffnet ist!");
											}
										}
									});
								}
								{
									deviceGroupText = new Text(group1, SWT.MULTI | SWT.WRAP);
									deviceGroupText.setBounds(12, 48, 430, 16);
									deviceGroupText.setText("Bitte wählen Sie das Gerät, welches an Ihrem Rechner angeschlossen ist");
									deviceGroupText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
								}
								{
									deviceCanvas = new Canvas(group1, SWT.NONE);
									deviceCanvas.setBounds(12, 70, 227, 165);
									deviceCanvas.setBackgroundImage(new Image(dialogShell.getDisplay(), settings.getDevicesPath() + fileSep + "NoDevicePicture.jpg"));
								}
								{
									herstellerDescription = new Text(group1, SWT.MULTI | SWT.WRAP);
									herstellerDescription.setBounds(251, 70, 72, 16);
									herstellerDescription.setText("Hersteller:");
									herstellerDescription.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
									herstellerDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
								}
								{
									deviceNameDescription = new Text(group1, SWT.MULTI | SWT.WRAP);
									deviceNameDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									deviceNameDescription.setBounds(251, 92, 89, 16);
									deviceNameDescription.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
									deviceNameDescription.setText("Gerätename:");
								}
								{
									deviceTypeDescription = new Text(group1, SWT.MULTI | SWT.WRAP);
									deviceTypeDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									deviceTypeDescription.setBounds(251, 114, 31, 16);
									deviceTypeDescription.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
									deviceTypeDescription.setText("Typ:");
								}
								{
									internetLinkDescription = new Text(group1, SWT.MULTI | SWT.WRAP);
									internetLinkDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									internetLinkDescription.setBounds(251, 136, 55, 16);
									internetLinkDescription.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
									internetLinkDescription.setText("Internet:");
								}
								{
									herstellerText = new Text(group1, SWT.NONE);
									herstellerText.setBounds(358, 70, 154, 16);
									herstellerText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
								}
								{
									deviceText = new Text(group1, SWT.NONE);
									deviceText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									deviceText.setBounds(358, 92, 154, 16);
								}
								{
									deviceTypeText = new Text(group1, SWT.NONE);
									deviceTypeText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									deviceTypeText.setBounds(358, 114, 154, 16);
								}
								{
									internetLinkText = new Text(group1, SWT.NONE);
									internetLinkText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									internetLinkText.setBounds(358, 136, 154, 16);
								}
								{
									openPortCheck = new Button(group1, SWT.CHECK | SWT.LEFT);
									openPortCheck.setBounds(251, 176, 236, 16);
									openPortCheck.setText("nach dem schliessen Port öffnen");
									openPortCheck.setSelection(settings.isAutoOpenSerialPort());
									openPortCheck.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("openPortCheck.widgetSelected, event=" + evt);
											if (openPortCheck.getSelection())
												settings.setProperty(Settings.AUTO_OPEN_SERIAL_PORT, "true");
											else
												settings.setProperty(Settings.AUTO_OPEN_SERIAL_PORT, "false");
										}
									});
								}
								{
									openToolBoxCheck = new Button(group1, SWT.CHECK | SWT.LEFT);
									openToolBoxCheck.setBounds(251, 198, 186, 16);
									openToolBoxCheck.setText("automatisch ToolBox öffnen");
									openToolBoxCheck.setSelection(settings.isAutoOpenToolBox());
									openToolBoxCheck.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("openToolBoxCheck.widgetSelected, event=" + evt);
											if (openToolBoxCheck.getSelection())
												settings.setProperty(Settings.AUTO_OPEN_TOOL_BOX, "true");
											else
												settings.setProperty(Settings.AUTO_OPEN_TOOL_BOX, "false");
										}
									});
								}
							}
							{
								portGroup = new Group(composite2, SWT.NONE);
								portGroup.setLayout(null);
								portGroup.setText("Anschlussport");
								portGroup.setBounds(12, 265, 524, 58);
								portGroup.addPaintListener(new PaintListener() {
									public void paintControl(PaintEvent evt) {
										if (settings.isGlobalSerialPort()) {
											portDescription.setEnabled(false);
											portSelectCombo.setEnabled(false);
										}
										else {
											portDescription.setEnabled(true);
											portSelectCombo.setEnabled(true);
										}
									}
								});
								{
									portDescription = new Text(portGroup, SWT.NONE);
									portDescription.setText("Portbezeichnung");
									portDescription.setBounds(30, 29, 183, 18);
									portDescription.setToolTipText("Bitte wählen Sie den Port aus mit dem Gerät und Rechner verbunden sind");
									portDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
								}
								{
									portSelectCombo = new CCombo(portGroup, SWT.FLAT | SWT.BORDER);
									portSelectCombo.setBounds(249, 27, 205, 22);
									portSelectCombo.setEditable(false);
									portSelectCombo.setToolTipText("Bitte wählen Sie den Port aus mit dem Gerät und Rechner verbunden sind");
									portSelectCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									portSelectCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("portSelectCombo.widgetSelected, event=" + evt);
											activeDeviceConfig.setPort(portSelectCombo.getText());
											activeDeviceConfig.storeDeviceProperties();
											application.updateTitleBar(activeDeviceConfig.getName(), activeDeviceConfig.getPort());
										}
									});
								}
							}
							{
								portSettings = new Group(composite2, SWT.NONE);
								portSettings.setLayout(null);
								portSettings.setText("Anschlusseinstellungen");
								portSettings.setBounds(12, 409, 524, 115);
								{
									baudeDescription = new Text(portSettings, SWT.NONE);
									baudeDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									baudeDescription.setText("Baudrate");
									baudeDescription.setBounds(8, 21, 100, 16);
								}
								{
									stopbitsDescription = new Text(portSettings, SWT.NONE);
									stopbitsDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									stopbitsDescription.setText("Stoppbits");
									stopbitsDescription.setBounds(8, 65, 100, 16);
								}
								{
									flowcontrolDescription = new Text(portSettings, SWT.NONE);
									flowcontrolDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									flowcontrolDescription.setText("Flusskontrolle");
									flowcontrolDescription.setBounds(261, 21, 100, 16);
								}
								{
									databbitsDescription = new Text(portSettings, SWT.NONE);
									databbitsDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									databbitsDescription.setText("Datenbits");
									databbitsDescription.setBounds(8, 43, 100, 16);
								}
								{
									parityDescription = new Text(portSettings, SWT.NONE);
									parityDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									parityDescription.setBounds(8, 86, 100, 16);
									parityDescription.setText("Parität");
								}
								{
									dtrDescription = new Text(portSettings, SWT.NONE);
									dtrDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									dtrDescription.setText("DTR");
									dtrDescription.setBounds(261, 49, 105, 17);
								}
								{
									rtsDescription = new Text(portSettings, SWT.NONE);
									rtsDescription.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
									rtsDescription.setText("RTS");
									rtsDescription.setBounds(261, 72, 77, 17);
								}
								{
									baudeSelectLabel = new CLabel(portSettings, SWT.RIGHT);
									FillLayout cLabel1Layout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
									baudeSelectLabel.setLayout(cLabel1Layout);
									//baudeSelectLabel.setBackground(osde.COLOR_WHITE);
									baudeSelectLabel.setBounds(115, 23, 90, 19);
									//baudeSelectLabel.setEditable(false);
									baudeSelectLabel.setEnabled(false);
								}
								{
									stopBitsSelectLabel = new CLabel(portSettings, SWT.RIGHT);
									//stopBitsSelectLabel.setBackground(osde.COLOR_WHITE);
									stopBitsSelectLabel.setBounds(115, 67, 90, 19);
									//stopBitsSelectLabel.setEditable(false);
									stopBitsSelectLabel.setEnabled(false);
								}
								{
									paritySelectLabel = new CLabel(portSettings, SWT.RIGHT);
									//paritySelectLabel.setBackground(osde.COLOR_WHITE);
									paritySelectLabel.setBounds(115, 89, 90, 19);
									//paritySelectLabel.setEditable(false);
									paritySelectLabel.setEnabled(false);
								}
								{
									dataBitsSelectLabel = new CLabel(portSettings, SWT.RIGHT);
									//dataBitsSelectLabel.setBackground(osde.COLOR_WHITE);
									dataBitsSelectLabel.setBounds(115, 45, 90, 19);
									//dataBitsSelectLabel.setEditable(false);
									dataBitsSelectLabel.setEnabled(false);
								}
								{
									flowControlSelectLabel = new CLabel(portSettings, SWT.LEFT);
									//flowControlSelectLabel.setBackground(osde.COLOR_WHITE);
									flowControlSelectLabel.setBounds(372, 21, 90, 19);
									//flowControlSelectLabel.setEditable(false);
									flowControlSelectLabel.setEnabled(false);
								}
								{
									dtrCheckBox = new Button(portSettings, SWT.CHECK | SWT.LEFT);
									dtrCheckBox.setBounds(372, 49, 92, 17);
									dtrCheckBox.setText("On / Off");
									dtrCheckBox.setEnabled(false);
								}
								{
									rtsCheckBox = new Button(portSettings, SWT.CHECK | SWT.LEFT);
									rtsCheckBox.setBounds(372, 72, 102, 17);
									rtsCheckBox.setText("On / Off");
									rtsCheckBox.setEnabled(false);
								}
							}
							{
								desktopTabsGroup = new Group(composite2, SWT.NONE);
								desktopTabsGroup.setLayout(null);
								desktopTabsGroup.setBounds(12, 329, 524, 74);
								desktopTabsGroup.setText("Anzeigeeinstellungen");
								desktopTabsGroup.setToolTipText("Hier können pro Gerät die aktiven Anzeigetabulatoren ausgewählt werden");
								desktopTabsGroup.addPaintListener(new PaintListener() {
									public void paintControl(PaintEvent evt) {
										log.finest("desktopTabsGroup.paintControl, event="+evt);
										if (activeDeviceConfig != null) {
											tableTabButton.setSelection(activeDeviceConfig.isTableTabRequested());
											digitalTabButton.setSelection(activeDeviceConfig.isDigitalTabRequested());
											analogTabButton.setSelection(activeDeviceConfig.isAnalogTabRequested());
											voltagePerCellButton.setSelection(activeDeviceConfig.isVoltagePerCellTabRequested());
										}
									}
								});
								{
									tableTabButton = new Button(desktopTabsGroup, SWT.CHECK | SWT.LEFT);
									tableTabButton.setBounds(12, 30, 110, 25);
									tableTabButton.setText("Tabelle");
									tableTabButton.setToolTipText("Tabellenansicht ein-aus-schalten");
									tableTabButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("tableTabButton.widgetSelected, event="+evt);
											activeDeviceConfig.setTableTabRequested(tableTabButton.getSelection());
										}
									});
								}
								{
									digitalTabButton = new Button(desktopTabsGroup, SWT.CHECK | SWT.LEFT);
									digitalTabButton.setBounds(141, 30, 110, 25);
									digitalTabButton.setText("Digital");
									digitalTabButton.setToolTipText("Digitalanzeigeansicht ein- aus- schalten");
									digitalTabButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("digitalTabButton.widgetSelected, event="+evt);
											activeDeviceConfig.setDigitalTabRequested(digitalTabButton.getSelection());
										}
									});
								}
								{
									analogTabButton = new Button(desktopTabsGroup, SWT.CHECK | SWT.LEFT);
									analogTabButton.setBounds(272, 30, 110, 25);
									analogTabButton.setText("Analog");
									analogTabButton.setToolTipText("Analoganzeigeansicht ein- aus- schalten");
									analogTabButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("analogTabButton.widgetSelected, event="+evt);
											activeDeviceConfig.setAnalogTabRequested(analogTabButton.getSelection());
										}
									});
								}
								{
									voltagePerCellButton = new Button(desktopTabsGroup, SWT.CHECK | SWT.LEFT);
									voltagePerCellButton.setBounds(402, 30, 110, 25);
									voltagePerCellButton.setText("Spannung/Zelle");
									voltagePerCellButton.setToolTipText("Ansicht Spannung pro Akkuzelle  ein- aus- schalten");
									voltagePerCellButton.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.finest("cellVoltageButton.widgetSelected, event="+evt);
											activeDeviceConfig.setVoltagePerCellTabRequested(voltagePerCellButton.getSelection());
										}
									});
								}
							}
						}
					}
					{
						auswahlTabItem = new CTabItem(einstellungenTabFolder, SWT.NONE);
						auswahlTabItem.setText("Geräteauswahl, Portübersicht");
						{
							deviceGroup = new Group(einstellungenTabFolder, SWT.NONE);
							deviceGroup.setLayout(null);
							auswahlTabItem.setControl(deviceGroup);
							deviceGroup.setText("Bitte verwendete Geräte selektieren");
							{
								deviceTable = new Table(deviceGroup, SWT.MULTI | SWT.CHECK);
								deviceTable.setBounds(17, 40, 497, 401);
								deviceTable.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										TableItem item = (TableItem) evt.item;
										String deviceName = ((TableItem) evt.item).getText();
										DeviceConfiguration tmpDeviceConfiguration;
										if (item.getChecked()) {
											log.fine("add device = " + deviceName);
											tmpDeviceConfiguration = deviceConfigurations.get(deviceName);
											tmpDeviceConfiguration.setUsed(true);
											tmpDeviceConfiguration.storeDeviceProperties();
											deviceConfigurations.put(deviceName, tmpDeviceConfiguration);
											activeDevices.add(deviceName);
											if (activeDevices.size() == 1) {
												activeDeviceConfig = deviceConfigurations.get(deviceName);
											}
										}
										else {
											log.fine("remove device = " + deviceName);
											tmpDeviceConfiguration = deviceConfigurations.get(deviceName);
											tmpDeviceConfiguration.setUsed(false);
											tmpDeviceConfiguration.storeDeviceProperties();
											activeDevices.remove(deviceName);
											
											// the removed configuration is the active one
											if (activeDeviceConfig != null && activeDeviceConfig.getName().equals(deviceName)) {
												// take first available
												if (activeDevices.size() >  0) {
													activeDeviceConfig = deviceConfigurations.get(activeDevices.firstElement());
													activeDevice = getInstanceOfDevice();
												}
												else { // no device
													activeDeviceConfig = null;
													activeDevice = null;
												}
											}
										}
										updateDialogEntries();
									}
								});
								deviceTable.setLinesVisible(true);
								{
									tableColumn1 = new TableColumn(deviceTable, SWT.LEFT);
									tableColumn1.setText("Column1");
									tableColumn1.setWidth(200);
								}
								{
									tableColumn2 = new TableColumn(deviceTable, SWT.LEFT);
									tableColumn2.setText("Column2");
									tableColumn2.setWidth(180);
								}
								{
									tableColumn3 = new TableColumn(deviceTable, SWT.LEFT);
									tableColumn3.setText("Column3");
									tableColumn3.setWidth(90);
								}
								{
									deviceTable.removeAll();

									for (String deviceKey : deviceConfigurations.keySet()) {
										log.fine(deviceKey);
										DeviceConfiguration config = deviceConfigurations.get(deviceKey);

										TableItem item = new TableItem(deviceTable, SWT.NULL);
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
					einstellungenTabFolder.setSelection(0);
					einstellungenTabFolder.setBounds(0, 0, 559, 505);
				}
			}
			{
				closeButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				FormData closeButtonLData = new FormData();
				closeButtonLData.left =  new FormAttachment(0, 1000, 12);
				closeButtonLData.top =  new FormAttachment(0, 1000, 573);
				closeButtonLData.width = 529;
				closeButtonLData.height = 26;
				closeButton.setLayoutData(closeButtonLData);
				closeButton.setText("Schliessen");
				closeButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.fine("closeButton.widgetSelected, event=" + evt);
						dialogShell.dispose();
					}
				});
			}
			updateDialogEntries(); // update all the entries according active device configuration
			dialogShell.layout();
			dialogShell.pack();
			dialogShell.setSize(566, 644);
			dialogShell.setText("Geräteauswahl und Schnittstelleneinstellung");
			dialogShell.setLocation(getParent().toDisplay(100, 100));
			dialogShell.open();
			Display display = dialogShell.getDisplay();
			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			application.openMessageDialog("Es ist ein Fehler aufgetreten : " + e.getMessage());
		}
		return activeDevice;
	}

	/**
	 * update entries according configuration, this is called whenever a new device is selected
	 */
	private void updateDialogEntries() {
		// device selection
		log.fine(activeDevices.toString());
		String[] list = activeDevices.toArray(new String[activeDevices.size()]);
		Arrays.sort(list); // this sorts the list but not the vector
		activeDevices = new Vector<String>(list.length);
		for (String string : list) {
			activeDevices.add(string);
		}
		if (list.length > 0) {
			activeDeviceName = (activeDeviceConfig == null) ? "" : activeDeviceConfig.getName();
			deviceSelectCombo.setItems(list);
			deviceSelectCombo.setVisibleItemCount(activeDevices.size());
			deviceSelectCombo.select(activeDevices.indexOf(activeDeviceName));
			log.fine(activeDeviceName + " - " + activeDevices.indexOf(activeDeviceName));

			deviceSlider.setMaximum(activeDevices.size());
			deviceSlider.setSelection(activeDevices.indexOf(activeDeviceName));
			log.fine("activeDevices.size() " + activeDevices.size());
		}
		else { // no active device
			activeDeviceConfig = null;
			activeDeviceName = (activeDeviceConfig == null) ? "" : activeDeviceConfig.getName();
			deviceSelectCombo.setItems(new String[] {"---->>>>   kein Gerät gewählt"});
			deviceSelectCombo.setVisibleItemCount(1);
			deviceSelectCombo.select(0);
			log.fine("no active device");

			deviceSlider.setMaximum(1);
			deviceSlider.setIncrement(0);
			deviceSlider.setSelection(0);
			log.fine("activeDevices.size() = 0");
		}	

		// update all serial Port settings
		if (activeDeviceConfig == null) {
			log.fine("activeDeviceConfig == null -> no device selected as active");
			application.setActiveDevice(null);
			deviceCanvas.setBackgroundImage(new Image(dialogShell.getDisplay(), settings.getDevicesPath() + fileSep + "NoDevicePicture.jpg"));

			herstellerText.setText("---");
			deviceText.setText("---");
			deviceTypeText.setText("---");
			internetLinkText.setText("---");

			availablePorts = DeviceSerialPort.listConfiguredSerialPorts();
			portSelectCombo.setItems(availablePorts.toArray(new String[availablePorts.size()]));
			portSelectCombo.select(0);
			
			// com port adjustments group
			baudeSelectLabel.setText("0");
			dataBitsSelectLabel.setText("0");
			stopBitsSelectLabel.setText("0");
			paritySelectLabel.setText("0");
			flowControlSelectLabel.setText("0");
			dtrCheckBox.setSelection(false);
			rtsCheckBox.setSelection(false);
		}
		else {
			log.fine(settings.getDevicesPath() + activeDeviceConfig.getImageFileName());
			deviceCanvas.setBackgroundImage(new Image(dialogShell.getDisplay(), settings.getDevicesPath() + fileSep + activeDeviceConfig.getImageFileName()));

			herstellerText.setText(activeDeviceConfig.getManufacturer());
			deviceText.setText(activeDeviceConfig.getName());
			deviceTypeText.setText(activeDeviceConfig.getDeviceGroup());
			String link = activeDeviceConfig.getManufacturerURL() != null ? activeDeviceConfig.getManufacturerURL() : "????";
			internetLinkText.setText(link);

			availablePorts = DeviceSerialPort.listConfiguredSerialPorts();
			portSelectCombo.setItems(availablePorts.toArray(new String[availablePorts.size()]));
			portSelectCombo.select(availablePorts.indexOf(activeDeviceConfig.getPort()));

			// com port adjustments group
			baudeSelectLabel.setText(new Integer(activeDeviceConfig.getBaudeRate()).toString());
			dataBitsSelectLabel.setText(new Integer(activeDeviceConfig.getDataBits()).toString());
			stopBitsSelectLabel.setText(new Integer(activeDeviceConfig.getStopBits()).toString());
			paritySelectLabel.setText(new Integer(activeDeviceConfig.getParity()).toString());
			flowControlSelectLabel.setText(new Integer(activeDeviceConfig.getFlowCtrlMode()).toString());
			dtrCheckBox.setSelection(activeDeviceConfig.isDTR());
			rtsCheckBox.setSelection(activeDeviceConfig.isRTS());
		}
	}

	/**
	 * check if the configure com port matches system available
	 */
	public boolean checkPortSelection() {
		boolean matches = true;
		if (availablePorts == null) {
			availablePorts = DeviceSerialPort.listConfiguredSerialPorts();
		}

		if (settings.isGlobalSerialPort() && availablePorts.indexOf(settings.getSerialPort()) < 0) {
			application.openMessageDialog("Der für die Anwendung konfigurierte serielle Port steht am System nicht zur Verfügung, bitte wählen sie einen anderen");
			matches = false;
		}
		else if (!settings.isGlobalSerialPort() && (activeDeviceConfig != null && availablePorts.indexOf(activeDeviceConfig.getPort()) < 0)) {
			application.openMessageDialog("Der für das Gerät konfigurierte serielle Port steht am System nicht zur Verfügung, bitte wählen sie einen anderen");
			matches = false;
		}
		else {
			if (activeDeviceConfig != null) { //currently no device selected
				if (settings.isGlobalSerialPort()) activeDeviceConfig.setPort(settings.getSerialPort());
			}
		}
		return matches;
	}

	/**
	 * method to setup new device, this might called using this dialog or a menu item where device is switched 
	 */
	public void setupDevice() {
		if (isDeviceChanged()) {
//			if (!checkPortSelection()) {
//				if (settings.isGlobalSerialPort())
//					application.openSettingsDialog();
//				else
//					application.setActiveDevice(this.open());
//			}
//			else if (settings.getActiveDevice().startsWith("---")) {
//				application.setActiveDevice(this.open());
//			}
			activeDevice = this.getInstanceOfDevice();
			application.setActiveDevice(activeDevice);
			setupDataChannels(activeDevice);
			application.setupDataTableHeader();
			application.updateDigitalWindow();
			application.updateAnalogWindow();
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
				log.fine("setting up channels = " + i);

				Channel newChannel = new Channel(i, activeDevice.getChannelName(i), activeDevice.getChannelType(i));
				// do not allocate records to record set - newChannel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, activeConfig));
				channels.put(new Integer(i), newChannel);
				// do not call channel.applyTemplate here, there are no record sets
			}
			channels.switchChannel(1, ""); // set " 1 : Ausgang" as default after device switch and update
		}
	}

	/**
	 * calculates the new class name for the device
	 */
	@SuppressWarnings("unchecked")
	public IDevice getInstanceOfDevice() {
		IDevice newInst = null;
		String selectedDeviceName = activeDeviceConfig.getName().replace(" ", "").replace("-", "");
		String className = "osde.device." + activeDeviceConfig.getManufacturer().toLowerCase().replace(" ", "").replace("-", "") + "." + selectedDeviceName;
		try {
			//String className = "osde.device.DefaultDeviceDialog";
			log.fine("loading Class " + className);
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Class c = loader.loadClass(className);
			//Class c = Class.forName(className);
			Constructor constructor = c.getDeclaredConstructor(new Class[] { DeviceConfiguration.class });
			log.fine("constructor != null -> " + (constructor != null ? "true" : "false"));
			newInst = (IDevice) constructor.newInstance(new Object[] { activeDeviceConfig });

		}
		catch (NoClassDefFoundError e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			application.openMessageDialog("Die Geräteimplementierung wurde nicht gefunden - " + className);
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			application.openMessageDialog(e.getClass().getSimpleName() + " " + e.getMessage());
		}
		return newInst;
	}

	public CCombo getDeviceSelectionCombo(Composite parent) {
		return deviceSelectCombo;
	}

	public Canvas getDeviceCanvas(Composite parent) {
		return deviceCanvas;
	}

	public Text getHerstellerText(Composite parent) {
		return herstellerText;
	}

	public Group getPortSettings(Composite parent) {
		return portSettings;
	}

	public Table getDeviceTable(Composite parent) {
		return deviceTable;
	}

	public CCombo getDeviceSelectCombo() {
		return deviceSelectCombo;
	}

	public Vector<String> getActiveDevices() {
		return activeDevices;
	}

	public Slider getDeviceSlider() {
		return deviceSlider;
	}

	public DeviceConfiguration getActiveConfig() {
		return activeDeviceConfig;
	}

	/**
	 * @param activeConfig the activeConfig to set
	 */
	public void setActiveConfig(DeviceConfiguration activeConfig) {
		this.activeDeviceConfig = activeConfig;
	}

	/**
	 * @return the devices
	 */
	public TreeMap<String, DeviceConfiguration> getDevices() {
		return deviceConfigurations;
	}
	
	/**
	 * query if the device selection has changed
	 * @return true if device selection changed
	 */
	public boolean isDeviceChanged() {
		return (activeDeviceConfig != null || activeDevice != null && activeDevice != application.getActiveDevice());
	}
	
	/**
	 * check for data which need to be saved
	 */
	public boolean checkDataSaved() {
		boolean result = true;
		String unsaved = Channels.getInstance().checkRecordSetsSaved();
		if (unsaved.length() != 0) {
			String msg = "Die folgenden Datensätze sind nicht gesichert und gehen verloren" + unsaved.toString();
			if (application.openOkCancelMessageDialog(msg) == SWT.CANCEL) {
				result = false;
				log.fine("SWT.CANCEL");
			}
		}
		return result;
		
	}
}

