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
package osde.ui.dialog.edit;
import java.io.File;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import osde.OSDE;
import osde.config.Settings;
import osde.device.ChecksumTypes;
import osde.device.DataTypes;
import osde.device.DesktopType;
import osde.device.DeviceConfiguration;
import osde.device.FormatTypes;
import osde.device.ObjectFactory;
import osde.device.PropertyType;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.FileUtils;
import osde.utils.StringHelper;

/**
 * Dialog class enable to edit existing and create new device property files 
 * @author Winfried Brügmann
 */
public class DevicePropertiesEditor extends org.eclipse.swt.widgets.Dialog {
	final static Logger						log			= Logger.getLogger(DevicePropertiesEditor.class.getName());
	public final static int				widgetFontSize 										= OSDE.IS_LINUX ? 8 : 9;
	public final static String		widgetFontName										= OSDE.IS_WINDOWS ? "Microsoft Sans Serif" : "Sans Serif";


	Shell dialogShell;
	
	CTabFolder tabFolder;
	Label devicePropFileNamelabel;
	Text deviceFileNameText;
	Button fileSelectionButton, saveButton, closeButton;
	
	CTabItem deviceTabItem;
	Composite deviceComposite;
	Label deviceDescriptionlabel;
	Label deviceNameLabel, manufacturerLabel, manufURLabel, imageFileNameLabel, usageLabel, groupLabel;
	Text nameText, manufacturerText, manufURLText, imageFileNameText;
	Button usageButton, fileSelectButton;
	CCombo groupSelectionCombo;
	Composite deviceLabelComposite, devicePropsComposite;
	
	String devicePropertiesFileName = OSDE.STRING_EMPTY;
	String deviceName = OSDE.STRING_EMPTY;
	String manufacuturerURL = OSDE.STRING_EMPTY;
	String imageFileName = OSDE.STRING_EMPTY;
	String manufacturer = OSDE.STRING_EMPTY;
	boolean isDeviceUsed = false;
	String deviceGroup = OSDE.STRING_EMPTY; //TODO define device groups
	
	SeriaPortTypeTabItem serialPortTabItem;
	
	CTabItem timeBaseTabItem;
	Composite timeBaseComposite;
	Label timeBaseDescriptionLabel;
	Label	timeBaseNameLabel, timeBaseSymbolLabel, timeBaseUnitLabel, timeBaseTimeStepLabel;
	Text	timeBaseNameText, timeBaseSymbolText, timeBaseUnitText, timeBaseTimeStepText;

	double timeStep_ms = 0.0;
	
	CTabItem dataBlockTabItem;
	Composite dataBlockComposite;
	Label dataBlockDescriptionLabel;
	Label dataBlockFormatLabel, dataBlockSizeLabel, dataBlockCheckSumFormatLabel, dataBlockCheckSumLabel, dataBlockEndingLabel;
	CCombo dataBlockFormatCombo, dataBlockcheckSumFormatCombo, dataBlockCheckSumTypeCombo;
	Text dataBlockSizeText, dataBlockEndingText;
	Group dataBlockRequiredGroup, dataBlockOptionalGroup;
	Button dataBlockOptionalEnableButton;
	
	FormatTypes dataBlockFormat = FormatTypes.BINARY, dataBlockcheckSumFormat = FormatTypes.BINARY;
	int dataBlockSize = 30;
	ChecksumTypes dataBlockCheckSumType = ChecksumTypes.XOR;
	String dataBlockEnding = "0a0d";
	
	CTabItem modeStateTabItem, modeStateInnerTabItem;
	Composite modeStateComposite;
	Label modeStateDescriptionLabel;
	Button addButton;
	CTabFolder modeStateTabFolder;
	PropertyTypeTabItem modeStateItemComposite;
	
	CTabItem channelConfigurationTabItem;
	Composite channelConfigComposite;
	Label channelConfigDescriptionLabel;
	CTabFolder channelConfigInnerTabFolder;
	
	CTabItem destopTabItem;
	PropertyTypeTabItem desktopInnerTabItem1, desktopInnerTabItem2, desktopInnerTabItem3, desktopInnerTabItem4;
	Composite desktopComposite;
	Label destktopDescriptionLabel;	
	CTabFolder desktopTabFolder;
	
	//cross over fields
	DeviceConfiguration deviceConfig;
	final Settings settings;

	/**
	* Auto-generated main method to display this 
	* org.eclipse.swt.widgets.Dialog inside a new Shell.
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			DevicePropertiesEditor inst = new DevicePropertiesEditor(shell, SWT.NULL);
			inst.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public DevicePropertiesEditor(Shell parent, int style) {
		super(parent, style);
		this.settings = Settings.getInstance();
	}

	public void open() {
		try {
			Shell parent = getParent();
			dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

			{
				//Register as a resource user - SWTResourceManager will
				//handle the obtaining and disposing of resources
				SWTResourceManager.registerResourceUser(dialogShell);
			}
			
			
			dialogShell.setLayout(null);
			dialogShell.layout();
			dialogShell.pack();			
			dialogShell.setSize(640, 490);
			dialogShell.setText("Device Properties Editor");
			dialogShell.setImage(SWTResourceManager.getImage("osde/resource/EditHot.gif"));
			dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					log.log(Level.FINEST, "dialogShell.widgetDisposed, event="+evt);
					if (deviceConfig != null && deviceConfig.isChangePropery()) {
						String msg = "The content of file " + devicePropertiesFileName + " was changed, do you want to save chenges ?";
						if (OpenSerialDataExplorer.getInstance().openYesNoMessageDialog(dialogShell, msg) == SWT.YES) {
							deviceConfig.storeDeviceProperties();
						}
					}
				}
			});
			{
				fileSelectionButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				fileSelectionButton.setText(" ... ");
				fileSelectionButton.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
				fileSelectionButton.setToolTipText("push, to select an existing file ");
				fileSelectionButton.setBounds(580, 10, 30, 20);
				fileSelectionButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "fileSelectionButton.widgetSelected, event="+evt);
						if (deviceConfig != null && deviceConfig.isChangePropery()) {
							String msg = "The content of file " + devicePropertiesFileName + " was changed, do you want to save chenges ?";
							if (OpenSerialDataExplorer.getInstance().openYesNoMessageDialog(dialogShell, msg) == SWT.YES) {
								deviceConfig.storeDeviceProperties();
							}
						}
						FileDialog fileSelectionDialog = new FileDialog(dialogShell);
						fileSelectionDialog.setFilterPath(getDevicesPath());
						fileSelectionDialog.setText("OpenSerialDataExplorer DeviceProperties");
						fileSelectionDialog.setFilterExtensions(new String[] {"*.xml"});
						fileSelectionDialog.setFilterNames(new String[] {"DeviceProperties Files"});
						fileSelectionDialog.open();
						devicePropertiesFileName = fileSelectionDialog.getFileName();
						log.log(Level.INFO, "devicePropertiesFileName = "+devicePropertiesFileName);
						deviceFileNameText.setText(devicePropertiesFileName);
						
						try {
							deviceConfig = new DeviceConfiguration(getDevicesPath() + OSDE.FILE_SEPARATOR_UNIX + devicePropertiesFileName);
							
							update();
						}
						catch (Exception e) {
							log.log(Level.SEVERE, e.getMessage(), e);
						}
					}
				});
			}
			{
				deviceFileNameText = new Text(dialogShell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
				deviceFileNameText.setBounds(120, 9, 450, 22);
				deviceFileNameText.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
				deviceFileNameText.addKeyListener(new KeyAdapter() {
					public void keyReleased(KeyEvent evt) {
						log.log(Level.FINEST, "deviceFileNameText.keyReleased, event="+evt);
						if (evt.keyCode == SWT.CR) {
							try {
								devicePropertiesFileName = deviceFileNameText.getText().trim();
								if (!devicePropertiesFileName.endsWith(OSDE.FILE_ENDING_DOT_XML)) {
									if (devicePropertiesFileName.lastIndexOf(".") != -1) {
										devicePropertiesFileName = devicePropertiesFileName.substring(0, devicePropertiesFileName.lastIndexOf("."));
									}
									devicePropertiesFileName = devicePropertiesFileName + OSDE.FILE_ENDING_DOT_XML; 
								}
								log.log(Level.INFO, "devicePropertiesFileName = "+devicePropertiesFileName);
								
								if (!(new File(getDevicesPath() + OSDE.FILE_SEPARATOR_UNIX + devicePropertiesFileName)).exists()) {
									MessageBox okCancelMessageDialog = new MessageBox(dialogShell, SWT.PRIMARY_MODAL | SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION);
									okCancelMessageDialog.setText("Device Properties Editor");
									okCancelMessageDialog.setMessage(Messages.getString(MessageIds.OSDE_MSGE0003) + devicePropertiesFileName + "\nShould the file beeing created ?");
									if (SWT.OK == okCancelMessageDialog.open()) {
										if (FileUtils.extract(this.getClass(), "DeviceSample_" + settings.getLocale() + "_V08.xml", devicePropertiesFileName, "resource/", getDevicesPath(), "555")) {
											deviceConfig = new DeviceConfiguration(getDevicesPath() + OSDE.FILE_SEPARATOR_UNIX + devicePropertiesFileName);
										}
									}
								}
								else {
									deviceConfig = new DeviceConfiguration(getDevicesPath() + OSDE.FILE_SEPARATOR_UNIX + devicePropertiesFileName);
								}
								update();
							}
							catch (Throwable e) {
								log.log(Level.SEVERE, e.getMessage(), e);
							}
						}
					}
				});
			}
			{
				devicePropFileNamelabel = new Label(dialogShell, SWT.RIGHT);
				devicePropFileNamelabel.setText("FileName : ");
				devicePropFileNamelabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
				devicePropFileNamelabel.setToolTipText("specify a filename to be used for device property file to edit");
				devicePropFileNamelabel.setBounds(0, 12, 100, 16);
			}
			{
				closeButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				closeButton.setText("close");
				closeButton.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
				closeButton.setBounds(338, 418, 250, 30);
				closeButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "closeButton.widgetSelected, event="+evt);
						dialogShell.dispose();
					}
				});
			}
			{
				saveButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				saveButton.setText("save");
				saveButton.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
				saveButton.setBounds(50, 418, 250, 30);
				saveButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "saveButton.widgetSelected, event="+evt);
						deviceConfig.storeDeviceProperties();
					}
				});
			}
			{
				tabFolder = new CTabFolder(dialogShell, SWT.BORDER);
				tabFolder.setSimple(false);
				tabFolder.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
				{
					deviceTabItem = new CTabItem(tabFolder, SWT.NONE);
					deviceTabItem.setText("Device");
					deviceTabItem.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
					{
						deviceComposite = new Composite(tabFolder, SWT.NONE);
						deviceComposite.setLayout(null);
						deviceTabItem.setControl(deviceComposite);
						{
							deviceDescriptionlabel = new Label(deviceComposite, SWT.CENTER | SWT.WRAP);
							deviceDescriptionlabel.setText("This section describes the main parameter of any device.");
							deviceDescriptionlabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
							deviceDescriptionlabel.setBounds(12, 8, 604, 50);
						}
						{
							deviceLabelComposite = new Composite(deviceComposite, SWT.NONE);
							deviceLabelComposite.setLayout(null);
							deviceLabelComposite.setBounds(20, 70, 145, 190);
							{
								deviceNameLabel = new Label(deviceLabelComposite, SWT.RIGHT);
								deviceNameLabel.setText("Name");
								deviceNameLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
								deviceNameLabel.setForeground(osde.ui.SWTResourceManager.getColor(SWT.COLOR_BLACK));
								deviceNameLabel.setBounds(0, 0, 137, 16);
							}
							{
								manufacturerLabel = new Label(deviceLabelComposite, SWT.RIGHT);
								manufacturerLabel.setText("Manufacturer");
								manufacturerLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
								manufacturerLabel.setBounds(0, 33, 137, 19);
							}
							{
								manufURLabel = new Label(deviceLabelComposite, SWT.RIGHT);
								manufURLabel.setText("Manufactorer URL");
								manufURLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
								manufURLabel.setBounds(0, 64, 137, 19);
							}
							{
								imageFileNameLabel = new Label(deviceLabelComposite, SWT.RIGHT);
								imageFileNameLabel.setText("Image Filename");
								imageFileNameLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
								imageFileNameLabel.setBounds(0, 95, 137, 19);
							}
							{
								usageLabel = new Label(deviceLabelComposite, SWT.RIGHT);
								usageLabel.setText("usage");
								usageLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
								usageLabel.setBounds(0, 126, 137, 19);
							}
							{
								groupLabel = new Label(deviceLabelComposite, SWT.RIGHT);
								groupLabel.setText("Group ID");
								groupLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
								groupLabel.setBounds(0, 157, 137, 16);
							}
						}
						{
							devicePropsComposite = new Composite(deviceComposite, SWT.NONE);
							devicePropsComposite.setLayout(null);
							devicePropsComposite.setBounds(170, 70, 450, 190);
							devicePropsComposite.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									log.log(Level.FINEST, "devicePropsComposite.paintControl, event=" + evt);
									deviceFileNameText.setText(devicePropertiesFileName);
									nameText.setText(deviceName);
									manufURLText.setText(manufacuturerURL);
									imageFileNameText.setText(imageFileName);
									manufacturerText.setText(manufacturer);
									usageButton.setSelection(isDeviceUsed);
									groupSelectionCombo.setText(deviceGroup);
								}
							});
							{
								nameText = new Text(devicePropsComposite, SWT.BORDER);
								nameText.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
								nameText.setBounds(0, 0, 409, 22);
								nameText.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										log.log(Level.FINEST, "nameText.keyReleased, event=" + evt);
										deviceConfig.setName(deviceName = nameText.getText());
									}
								});
							}
							{
								manufacturerText = new Text(devicePropsComposite, SWT.BORDER);
								manufacturerText.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
								manufacturerText.setBounds(0, 32, 409, 22);
								manufacturerText.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										log.log(Level.FINEST, "manufacturerText.keyReleased, event=" + evt);
										deviceConfig.setManufacturer(manufacturer = manufacturerText.getText());
									}
								});
							}
							{
								manufURLText = new Text(devicePropsComposite, SWT.BORDER);
								manufURLText.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
								manufURLText.setBounds(0, 64, 409, 22);
								manufURLText.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										log.log(Level.FINEST, "manufURLText.keyReleased, event=" + evt);
										deviceConfig.setManufacturerURL(manufacuturerURL = manufURLText.getText());
									}
								});
							}
							{
								imageFileNameText = new Text(devicePropsComposite, SWT.BORDER);
								imageFileNameText.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
								imageFileNameText.setBounds(0, 95, 409, 22);
								imageFileNameText.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										log.log(Level.FINEST, "imageFileNameText.keyReleased, event=" + evt);
										deviceConfig.setImageFileName(imageFileName = imageFileNameText.getText());
									}
								});
							}
							{
								usageButton = new Button(devicePropsComposite, SWT.CHECK);
								usageButton.setBounds(3, 126, 159, 22);
							}
							{
								groupSelectionCombo = new CCombo(devicePropsComposite, SWT.BORDER);
								groupSelectionCombo.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
								groupSelectionCombo.setBounds(0, 154, 409, 22);
								groupSelectionCombo.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										log.log(Level.FINEST, "groupSelectionCombo.keyReleased, event=" + evt);
										deviceConfig.setDeviceGroup(deviceGroup = groupSelectionCombo.getText());
									}
								});
								groupSelectionCombo.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "groupSelectionCombo.widgetSelected, event=" + evt);
										deviceConfig.setDeviceGroup(deviceGroup = groupSelectionCombo.getText());
									}
								});
							}
							{
								fileSelectButton = new Button(devicePropsComposite, SWT.PUSH | SWT.CENTER);
								fileSelectButton.setText(" ... ");
								fileSelectButton.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
								fileSelectButton.setBounds(415, 96, 30, 20);
								fileSelectButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										FileDialog fileSelectionDialog = new FileDialog(dialogShell);
										fileSelectionDialog.setText("OpenSerialDataExplorer Device Image File");
										fileSelectionDialog.setFilterPath(getDevicesPath());
										fileSelectionDialog.setFilterExtensions(new String[] { "*.jpg", "*.gif", "*.png" });
										fileSelectionDialog.setFilterNames(new String[] { "JPEG compressed image", "Graphics Interchange Format image", "Portable Network Graphics image" });
										fileSelectionDialog.open();
										imageFileName = fileSelectionDialog.getFileName();
										log.log(Level.INFO, "imageFileName = " + imageFileName);
										deviceConfig.setImageFileName(imageFileName = imageFileNameText.getText());
									}
								});
							}
						}
					}
				}
				{
					serialPortTabItem = new SeriaPortTypeTabItem(tabFolder, SWT.CLOSE, 1);
				}
				{
					timeBaseTabItem = new CTabItem(tabFolder, SWT.NONE);
					timeBaseTabItem.setText("Time Base");
					timeBaseTabItem.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
					{
						timeBaseComposite = new Composite(tabFolder, SWT.NONE);
						timeBaseTabItem.setControl(timeBaseComposite);
						timeBaseComposite.setLayout(null);
						timeBaseComposite.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								log.log(Level.FINEST, "timeBaseComposite.paintControl, event="+evt);
								timeBaseTimeStepText.setText(String.format("%.1f", timeStep_ms));
							}
						});
						{
							timeBaseDescriptionLabel = new Label(timeBaseComposite, SWT.CENTER | SWT.WRAP);
							timeBaseDescriptionLabel.setText("Defines the time base used to display the data.\nA timeStep of -1 means, time comes from the device data within data block and might be not constant.");
							timeBaseDescriptionLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
							timeBaseDescriptionLabel.setBounds(17, 12, 591, 71);
						}
						{
							timeBaseNameLabel = new Label(timeBaseComposite, SWT.RIGHT);
							timeBaseNameLabel.setText("Name");
							timeBaseNameLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
							timeBaseNameLabel.setBounds(142, 95, 150, 20);
						}
						{
							timeBaseNameText = new Text(timeBaseComposite, SWT.CENTER | SWT.BORDER);
							timeBaseNameText.setText(Messages.getString(MessageIds.OSDE_MSGT0271).split(OSDE.STRING_BLANK)[0]);
							timeBaseNameText.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
							timeBaseNameText.setBounds(322, 94, 60, 20);
							timeBaseNameText.setEditable(false);
						}
						{
							timeBaseSymbolLabel = new Label(timeBaseComposite, SWT.RIGHT);
							timeBaseSymbolLabel.setText("Symbol");
							timeBaseSymbolLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
							timeBaseSymbolLabel.setBounds(142, 125, 150, 20);
						}
						{
							timeBaseSymbolText = new Text(timeBaseComposite, SWT.CENTER | SWT.BORDER);
							timeBaseSymbolText.setText(Messages.getString(MessageIds.OSDE_MSGT0271).split(OSDE.STRING_BLANK)[1]);
							timeBaseSymbolText.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
							timeBaseSymbolText.setBounds(322, 124, 60, 20);
							timeBaseSymbolText.setEditable(false);
						}
						{
							timeBaseUnitLabel = new Label(timeBaseComposite, SWT.RIGHT);
							timeBaseUnitLabel.setText("Unit");
							timeBaseUnitLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
							timeBaseUnitLabel.setBounds(142, 155, 150, 20);
						}
						{
							timeBaseUnitText = new Text(timeBaseComposite, SWT.CENTER | SWT.BORDER);
							timeBaseUnitText.setText(Messages.getString(MessageIds.OSDE_MSGT0271).split(OSDE.STRING_BLANK)[3]);
							timeBaseUnitText.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
							timeBaseUnitText.setBounds(322, 154, 60, 20);
							timeBaseUnitText.setEditable(false);
						}
						{
							timeBaseTimeStepLabel = new Label(timeBaseComposite, SWT.RIGHT);
							timeBaseTimeStepLabel.setText("time step");
							timeBaseTimeStepLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
							timeBaseTimeStepLabel.setToolTipText("time step has to be defined in milli seconds, if as floating value one decimal max");
							timeBaseTimeStepLabel.setBounds(142, 185, 150, 20);
						}
						{
							timeBaseTimeStepText = new Text(timeBaseComposite, SWT.RIGHT | SWT.BORDER);
							timeBaseTimeStepText.setText("1000.0");
							timeBaseTimeStepText.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
							timeBaseTimeStepText.setBounds(322, 184, 60, 20);
							timeBaseTimeStepText.addVerifyListener(new VerifyListener() {
								public void verifyText(VerifyEvent evt) {
									log.log(Level.FINEST, "timeBaseTimeStepText.verifyText, event="+evt);
									evt.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, evt.text);
								}
							});
							timeBaseTimeStepText.addKeyListener(new KeyAdapter() {
								public void keyReleased(KeyEvent evt) {
									log.log(Level.FINEST, "timeBaseTimeStepText.keyReleased, event="+evt);
									try { 
										timeStep_ms = Double.parseDouble(timeBaseTimeStepText.getText().replace(OSDE.STRING_COMMA, OSDE.STRING_DOT));
										timeStep_ms = Double.parseDouble(String.format(Locale.ENGLISH, "%.1f", timeStep_ms));
										if (deviceConfig != null) 
											deviceConfig.setTimeStep_ms(timeStep_ms);
									}
									catch (NumberFormatException e) {
										// ignore input
									}
								}
							});
						}
					}
				}
				{
					createDataBlockType();
				}
				{
					createStateTabItem();
				}
				{
					channelConfigurationTabItem = new CTabItem(tabFolder, SWT.NONE);
					channelConfigurationTabItem.setText("Channel/Configuration");
					channelConfigurationTabItem.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
					{
						channelConfigComposite = new Composite(tabFolder, SWT.NONE);
						channelConfigComposite.setLayout(null);
						channelConfigurationTabItem.setControl(channelConfigComposite);
						channelConfigComposite.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								log.log(Level.FINEST, "channleConfigComposite.paintControl, event="+evt);
								if (deviceConfig != null) {
									for (int i = 1; i <= deviceConfig.getChannelCount(); i++) {
										deviceConfig.getChannelTypes(i);
									}
								}
							}
						});
						{
							channelConfigDescriptionLabel = new Label(channelConfigComposite, SWT.CENTER | SWT.WRAP);
							channelConfigDescriptionLabel.setText("Defines miscelanious visualisation properties of the application desktop.\nProbably there are more than one measurements to be described here.");
							channelConfigDescriptionLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
							channelConfigDescriptionLabel.setBounds(12, 5, 602, 38);
						}
						{
							channelConfigInnerTabFolder = new CTabFolder(channelConfigComposite, SWT.NONE | SWT.BORDER);
							channelConfigInnerTabFolder.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
							channelConfigInnerTabFolder.setBounds(0, 49, 626, 285);
							{
								//initial channel TabItem
								new ChannelTypeTabItem(channelConfigInnerTabFolder, SWT.NONE, 0);
							}
							channelConfigInnerTabFolder.setSelection(0);
						}
					}
				}
				{
					destopTabItem = new CTabItem(tabFolder, SWT.NONE);
					destopTabItem.setText("Desktop");
					destopTabItem.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
					{
						desktopComposite = new Composite(tabFolder, SWT.NONE);
						desktopComposite.setLayout(null);
						destopTabItem.setControl(desktopComposite);
//						desktopComposite.addPaintListener(new PaintListener() {
//							public void paintControl(PaintEvent evt) {
//								log.log(Level.FINEST, "desktopComposite.paintControl, event="+evt);
//							}
//						});
						{
							destktopDescriptionLabel = new Label(desktopComposite, SWT.CENTER | SWT.WRAP);
							destktopDescriptionLabel.setText("This section defines miscelanious visualisation properties of the application desktop. \nAs example showing the data table contnet or should be updated and displyed, ....");
							destktopDescriptionLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
							destktopDescriptionLabel.setBounds(12, 5, 602, 57);
						}
						{
							desktopTabFolder = new CTabFolder(desktopComposite, SWT.BORDER);
							GridLayout appDesktopTabCompositeLayout = new GridLayout();
							appDesktopTabCompositeLayout.makeColumnsEqualWidth = true;
							desktopTabFolder.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
							desktopTabFolder.setLayout(appDesktopTabCompositeLayout);
							desktopTabFolder.setBounds(165, 68, 300, 196);
							{
								desktopInnerTabItem1 = new PropertyTypeTabItem(desktopTabFolder, SWT.NONE, "DataTable");
							}
							{
								desktopInnerTabItem2 = new PropertyTypeTabItem(desktopTabFolder, SWT.NONE, "Digital");
							}
							{
								desktopInnerTabItem3 = new PropertyTypeTabItem(desktopTabFolder, SWT.NONE, "Analog");
							}
							{
								desktopInnerTabItem4 = new PropertyTypeTabItem(desktopTabFolder, SWT.NONE, "Cellvoltage");
							}
							desktopTabFolder.setSelection(0);
						}
					}
				}
				tabFolder.setSelection(0);
				tabFolder.setBounds(0, 45, 632, 360);
				tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
					public void restore(CTabFolderEvent evt) {
						log.log(Level.FINEST, "tabFolder.restore, event="+evt);
						((CTabItem)evt.item).getControl();
					}
					public void close(CTabFolderEvent evt) {
						log.log(Level.FINEST, "tabFolder.close, event="+evt);
						CTabItem tabItem = ((CTabItem)evt.item);
						if (deviceConfig != null) {
							if (tabItem.getText().equals("State")) deviceConfig.removeStateType();
							else if (tabItem.getText().equals("Serial Port")) deviceConfig.removeSerialPortType();
							else if (tabItem.getText().equals("Data Block")) deviceConfig.removeDataBlockType();
						}
						tabItem.dispose();
						if(deviceConfig != null) 
							update();
					}
				});
			}
			dialogShell.setLocation(getParent().toDisplay(100, 100));
			dialogShell.open();
			Display display = dialogShell.getDisplay();
			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * create a new data block type and place it right after time base
	 */
	void createDataBlockType() {
		for (int i = 1; i < tabFolder.getItemCount(); i++) {
			if (tabFolder.getItem(i).getText().equals("Time Base")) {
				dataBlockTabItem = new CTabItem(tabFolder, SWT.CLOSE, i+1);
				break;
			}
		}
		//dataBlockTabItem = new CTabItem(tabFolder, SWT.CLOSE); //remove the leading comment to enable visual edit
		dataBlockTabItem.setText("Data Block");
		{
			dataBlockComposite = new Composite(tabFolder, SWT.NONE);
			dataBlockComposite.setLayout(null);
			dataBlockTabItem.setControl(dataBlockComposite);
			{
				dataBlockDescriptionLabel = new Label(dataBlockComposite, SWT.CENTER);
				dataBlockDescriptionLabel.setText("This describes the data as to be interpreted as input.\nNomally this is handeled by individual device plug-in.\nThe default values loaded here refers to TEXT input related to LogViews OpenFormat");
				dataBlockDescriptionLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
				dataBlockDescriptionLabel.setBounds(12, 5, 602, 51);
			}
			{
				dataBlockRequiredGroup = new Group(dataBlockComposite, SWT.NONE);
				dataBlockRequiredGroup.setLayout(null);
				dataBlockRequiredGroup.setText("Required Entries");
				dataBlockRequiredGroup.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
				dataBlockRequiredGroup.setBounds(40, 80, 250, 170);
				dataBlockRequiredGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.log(Level.FINEST, "dataBlockRequiredGroup.paintControl, event="+evt);
						dataBlockFormatCombo.select(dataBlockFormat == FormatTypes.TEXT ? 0 : 1);
						dataBlockSizeText.setText(OSDE.STRING_EMPTY + dataBlockSize);
					}
				});
				{
					dataBlockFormatLabel = new Label(dataBlockRequiredGroup, SWT.RIGHT);
					dataBlockFormatLabel.setText("format");
					dataBlockFormatLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
					dataBlockFormatLabel.setBounds(19, 51, 85, 16);
				}
				{
					dataBlockFormatCombo = new CCombo(dataBlockRequiredGroup, SWT.BORDER);
					dataBlockFormatCombo.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
					dataBlockFormatCombo.setBounds(125, 49, 84, 21);
					dataBlockFormatCombo.setItems(new java.lang.String[]{"TEXT","BINARY"});
					dataBlockFormatCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
					dataBlockFormatCombo.setLayout(null);
					dataBlockFormatCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "dataBlockFormatCombo.widgetSelected, event="+evt);
							dataBlockFormat = FormatTypes.valueOf(dataBlockFormatCombo.getText());
							if (deviceConfig != null) {
								deviceConfig.setDataBlockFormat(dataBlockFormat);
							}
						} 
					});
				}
				{
					dataBlockSizeLabel = new Label(dataBlockRequiredGroup, SWT.RIGHT);
					dataBlockSizeLabel.setText("size");
					dataBlockSizeLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
					dataBlockSizeLabel.setToolTipText("define size in bytes for BINARY, or value count for TEXT format");
					dataBlockSizeLabel.setBounds(19, 105, 85, 16);
				}
				{
					dataBlockSizeText = new Text(dataBlockRequiredGroup, SWT.RIGHT | SWT.BORDER);
					dataBlockSizeText.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
					dataBlockSizeText.setBounds(127, 103, 64, 22);
					dataBlockSizeText.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent evt) {
							log.log(Level.FINEST, "dataBlockSizeText.keyReleased, event="+evt);
							dataBlockSize = Integer.parseInt(dataBlockSizeText.getText());
							if (deviceConfig != null) {
								deviceConfig.setDataBlockSize(dataBlockSize);
							}
						}
					});
					dataBlockSizeText.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent evt) {
							log.log(Level.FINEST, "dataBlockSizeText.verifyText, event="+evt);
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
				}
			}
			{
				dataBlockOptionalGroup = new Group(dataBlockComposite, SWT.NONE);
				dataBlockOptionalGroup.setLayout(null);
				dataBlockOptionalGroup.setText("Optional Entries");
				dataBlockOptionalGroup.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
				dataBlockOptionalGroup.setBounds(330, 80, 250, 170);
				dataBlockOptionalGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.log(Level.FINEST, "dataBlockOptionalGroup.paintControl, event="+evt);
						if (deviceConfig != null && deviceConfig.getDataBlockCheckSumFormat() != null && deviceConfig.getDataBlockCheckSumType() != null && deviceConfig.getDataBlockEnding() != null) {
							dataBlockOptionalEnableButton.setSelection(true);
							dataBlockcheckSumFormatCombo.select(dataBlockcheckSumFormat == FormatTypes.TEXT ? 0 : 1);
							dataBlockCheckSumTypeCombo.select(dataBlockCheckSumType == ChecksumTypes.XOR ? 0 : 1);
							dataBlockEndingText.setText(dataBlockEnding);
						}
						else {
							dataBlockOptionalEnableButton.setSelection(false);
						}
					}
				});
				{
					dataBlockOptionalEnableButton = new Button(dataBlockOptionalGroup, SWT.CHECK | SWT.RIGHT);
					dataBlockOptionalEnableButton.setText("enable");
					dataBlockOptionalEnableButton.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
					dataBlockOptionalEnableButton.setBounds(143, 14, 92, 20);
					dataBlockOptionalEnableButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "dataBlockOptionalEnableButton.widgetSelected, event="+evt);
							enableDataBlockOptionalPart(dataBlockOptionalEnableButton.getSelection());
						}
					});
				}
				{
					dataBlockCheckSumFormatLabel = new Label(dataBlockOptionalGroup, SWT.RIGHT);
					dataBlockCheckSumFormatLabel.setText("checkSum format");
					dataBlockCheckSumFormatLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
					dataBlockCheckSumFormatLabel.setBounds(6, 46, 122, 20);
				}
				{
					dataBlockCheckSumLabel = new Label(dataBlockOptionalGroup, SWT.RIGHT);
					dataBlockCheckSumLabel.setText("checkSum");
					dataBlockCheckSumLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
					dataBlockCheckSumLabel.setBounds(6, 77, 122, 20);
				}
				{
					dataBlockEndingLabel = new Label(dataBlockOptionalGroup, SWT.RIGHT);
					dataBlockEndingLabel.setText("ending [bytes]");
					dataBlockEndingLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
					dataBlockEndingLabel.setBounds(6, 111, 122, 20);
				}
				{
					dataBlockcheckSumFormatCombo = new CCombo(dataBlockOptionalGroup, SWT.BORDER);
					dataBlockcheckSumFormatCombo.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
					dataBlockcheckSumFormatCombo.setItems(new java.lang.String[]{"TEXT","BINARY"});
					dataBlockcheckSumFormatCombo.setBounds(143, 44, 92, 20);
					dataBlockcheckSumFormatCombo.setEditable(false);
					dataBlockcheckSumFormatCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
					dataBlockcheckSumFormatCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "dataBlockceckSumFormatCombo.widgetSelected, event="+evt);
							dataBlockcheckSumFormat = FormatTypes.valueOf(dataBlockcheckSumFormatCombo.getText());
							if (deviceConfig != null) {
								deviceConfig.setDataBlockCheckSumFormat(dataBlockcheckSumFormat);
							}
						}
					});
				}
				{
					dataBlockCheckSumTypeCombo = new CCombo(dataBlockOptionalGroup, SWT.RIGHT | SWT.BORDER);
					dataBlockCheckSumTypeCombo.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
					dataBlockCheckSumTypeCombo.setItems(new String[] {"XOR", "ADD"});
					dataBlockCheckSumTypeCombo.setBounds(143, 74, 92, 20);
					dataBlockCheckSumTypeCombo.setEditable(false);
					dataBlockCheckSumTypeCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
					dataBlockCheckSumTypeCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "dataBlockCheckSumCombo.widgetSelected, event="+evt);
							dataBlockCheckSumType = ChecksumTypes.valueOf(dataBlockCheckSumTypeCombo.getText());
							if (deviceConfig != null) {
								deviceConfig.setDataBlockCheckSumType(dataBlockCheckSumType);
							}
						}
					});
				}
				{
					dataBlockEndingText = new Text(dataBlockOptionalGroup, SWT.BORDER);
					dataBlockEndingText.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
					dataBlockEndingText.setBounds(144, 109, 61, 20);
					dataBlockEndingText.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent evt) {
							log.log(Level.FINEST, "dataBlockEndingText.keyReleased, event="+evt);
							dataBlockEnding = dataBlockEndingText.getText();
							if (deviceConfig != null) {
								deviceConfig.setDataBlockEnding(StringHelper.convert2ByteArray(dataBlockEnding));
							}
						}
					});
					dataBlockEndingText.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent evt) {
							log.log(Level.FINEST, "dataBlockEndingText.verifyText, event="+evt);
							evt.doit = StringHelper.verifyHexAsString(evt.text);
						}
					});
				}
			}
		}
	}

	/**
	 * create a new mode state tabulator with one mode state entry
	 */
	void createStateTabItem() {
		modeStateTabItem = new CTabItem(tabFolder, SWT.CLOSE);
		modeStateTabItem.setText("State");
		modeStateTabItem.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
		{
			modeStateComposite = new Composite(tabFolder, SWT.NONE);
			modeStateComposite.setLayout(null);
			modeStateTabItem.setControl(modeStateComposite);
			modeStateComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.log(Level.FINEST, "modeStateComposite.paintControl, event="+evt);
					for (CTabItem child : modeStateTabFolder.getItems()) {
						((PropertyTypeTabItem)child).propertyTypeComposite.redraw();
					}
				}
			});
			{
				modeStateDescriptionLabel = new Label(modeStateComposite, SWT.LEFT);
				modeStateDescriptionLabel.setText("This section defines device processing states, like \n  \t1. charge\n  \t2. discharge, ...");
				modeStateDescriptionLabel.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
				modeStateDescriptionLabel.setBounds(165, 4, 449, 55);
			}
			{
				modeStateTabFolder = new CTabFolder(modeStateComposite, SWT.BORDER);
				modeStateTabFolder.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
				modeStateTabFolder.setBounds(165, 65, 300, 207);
				modeStateTabFolder.setSelection(0);
				{
					modeStateInnerTabItem = new PropertyTypeTabItem(modeStateTabFolder, SWT.CLOSE, "State " + modeStateTabFolder.getItemCount());
				}
				modeStateTabFolder.setSelection(0);
				modeStateTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
					public void close(CTabFolderEvent evt) {
						log.log(Level.FINEST, "modeStateTabFolder.close, event="+evt);
						if (deviceConfig != null) {
							int childIndex = Integer.parseInt(evt.item.toString().split(" |}")[2]) - 1;
							modeStateTabFolder.getChildren()[childIndex].dispose();
							deviceConfig.removeStateType(deviceConfig.getStateType().getProperty().get(childIndex));
						}
						evt.item.dispose();
					}
				});
			}
			{
				addButton = new Button(modeStateComposite, SWT.PUSH | SWT.CENTER);
				addButton.setText("add a new state");
				addButton.setFont(SWTResourceManager.getFont(widgetFontName, widgetFontSize, SWT.NORMAL, false, false));
				addButton.setBounds(165, 284, 300, 30);
				addButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "addButton.widgetSelected, event="+evt);
						{
							PropertyType property = new ObjectFactory().createPropertyType();
							property.setName("new mode state");
							property.setType(DataTypes.INTEGER);
							property.setValue(OSDE.STRING_EMPTY+modeStateTabFolder.getItemCount());
							property.setDescription("new mode state description");
							modeStateInnerTabItem = new PropertyTypeTabItem(desktopTabFolder, SWT.CLOSE, "State " + modeStateTabFolder.getItemCount(), property);
							deviceConfig.appendStateType(property);
							update();
						}
					}
				});
			}
		}
	}
	
	/**
	 * query the Devices path to open, query, save the device properties
	 * @return
	 */
	private String getDevicesPath() {
		String osname = System.getProperty("os.name", OSDE.STRING_EMPTY).toLowerCase(); //$NON-NLS-1$
		String applHomePath= "";
		if (osname.startsWith("windows")) { //$NON-NLS-1$
			applHomePath = (System.getenv("APPDATA") + OSDE.FILE_SEPARATOR_UNIX + "OpenSerialDataExplorer" + OSDE.FILE_SEPARATOR_UNIX).replace("\\", OSDE.FILE_SEPARATOR_UNIX); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		else if (osname.startsWith("linux") || osname.startsWith("mac")) { //$NON-NLS-1$ //$NON-NLS-2$
			applHomePath = System.getProperty("user.home") + OSDE.FILE_SEPARATOR_UNIX + ".OpenSerialDataExplorer" + OSDE.FILE_SEPARATOR_UNIX; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return applHomePath + "Devices";
	}

	/**
	 * update internal variables by device properties
	 */
	private void update() {
		//DeviceType begin
		deviceName = deviceConfig.getName();
		manufacturer = deviceConfig.getManufacturer();
		manufacuturerURL = deviceConfig.getManufacturerURL();
		imageFileName = deviceConfig.getImageFileName();
		isDeviceUsed = deviceConfig.isUsed();
		deviceGroup = deviceConfig.getDeviceGroup();
		devicePropsComposite.redraw();
		//DeviceType end
		
		//SerialPortType begin
		if (deviceConfig.getSerialPortType() == null && !serialPortTabItem.isDisposed()) {
			serialPortTabItem.dispose();
		}
		else if (deviceConfig.getSerialPortType() != null && serialPortTabItem.isDisposed()) {
				serialPortTabItem = new SeriaPortTypeTabItem(tabFolder, SWT.CLOSE, 1);			
		}
		if (deviceConfig.getSerialPortType() != null && !serialPortTabItem.isDisposed()) {
			serialPortTabItem.setDeviceConfig(deviceConfig);
		}
		//SerialPortType end
		
		//TimeBaseType begin
		timeStep_ms = deviceConfig.getTimeStep_ms();
		timeBaseComposite.redraw();
		//TimeBaseType end
		
		//DataBlockType begin
		if (deviceConfig.getDataBlockType() == null && !dataBlockTabItem.isDisposed()) {
			dataBlockTabItem.dispose();
		}
		else {
			if (deviceConfig.getDataBlockType() != null && dataBlockTabItem.isDisposed()) {
				createDataBlockType();		
			}
			if (deviceConfig.getDataBlockType() != null && !dataBlockTabItem.isDisposed()) {
				dataBlockFormat = deviceConfig.getDataBlockFormat();
				dataBlockSize = deviceConfig.getDataBlockSize();
				dataBlockRequiredGroup.redraw();

				if (deviceConfig.getDataBlockCheckSumFormat() != null && deviceConfig.getDataBlockCheckSumType() != null && deviceConfig.getDataBlockEnding() != null) {
					dataBlockcheckSumFormat = deviceConfig.getDataBlockCheckSumFormat();
					dataBlockCheckSumType = deviceConfig.getDataBlockCheckSumType();
					dataBlockEnding = StringHelper.convertHexInput(deviceConfig.getDataBlockEnding());
					enableDataBlockOptionalPart(true);				
				}
				else {
					enableDataBlockOptionalPart(false);			
				}
			}
		}
		//DataBlockType begin
		
		//StateType begin
		int modeStateCount = (deviceConfig.getStateType() == null) ? 0 : deviceConfig.getStateSize();
		if (deviceConfig.getStateType() == null || (modeStateCount == 0 && (deviceConfig.getStateType() != null && !modeStateTabFolder.isDisposed()))) {
			if (modeStateTabFolder != null && !modeStateTabFolder.isDisposed()) {
				for (Control child : modeStateTabFolder.getChildren()) {
					child.dispose();
				}
				modeStateTabFolder.getParent().dispose();
			}
			if (modeStateTabItem != null) modeStateTabItem.dispose();
		}
		else {
			if (deviceConfig.getStateType() != null && modeStateTabItem.isDisposed()) {
				createStateTabItem();
			}
			if (deviceConfig.getStateType() != null && !modeStateTabItem.isDisposed()) {
				if (modeStateCount > modeStateTabFolder.getItemCount()) {
					for (int i = modeStateTabFolder.getItemCount(); i < deviceConfig.getStateSize(); i++) {
						modeStateInnerTabItem = new PropertyTypeTabItem(modeStateTabFolder, SWT.CLOSE, "State " + modeStateTabFolder.getItemCount());
					}
				}
				else if (modeStateCount < modeStateTabFolder.getItemCount()) {
					CTabItem[] childs = modeStateTabFolder.getItems();
					for (int i = modeStateCount - 1; i < childs.length; i++) {
						((PropertyTypeTabItem) childs[i]).dispose();
					}
				}
				int index = 1;
				for (CTabItem child : modeStateTabFolder.getItems()) {
					((PropertyTypeTabItem) child).setProperty(deviceConfig.getStateProperty(index++), true, false, false, false);
				}
				modeStateComposite.redraw();
			}
		}
		//StateType end

		//ChannelType begin
		int channelTypeCount = deviceConfig.getChannelCount();
		int actualTabItemCount = channelConfigInnerTabFolder.getItemCount();
		if (channelTypeCount < actualTabItemCount) {
			for (int i = channelTypeCount; i < actualTabItemCount; i++) {
				ChannelTypeTabItem channelTabItem = (ChannelTypeTabItem)channelConfigInnerTabFolder.getItem(channelTypeCount);
				channelTabItem.dispose();
			}
		}
		else if (channelTypeCount > actualTabItemCount) {
			for (int i = actualTabItemCount; i < channelTypeCount; i++) {
				new ChannelTypeTabItem(channelConfigInnerTabFolder, SWT.CLOSE, i);
			}
		}
		for (int i = 0; i < channelTypeCount; i++) {
			ChannelTypeTabItem channelTabItem = (ChannelTypeTabItem)channelConfigInnerTabFolder.getItem(i);
			channelTabItem.setChannelType(deviceConfig, deviceConfig.getChannelType(i+1), (i+1));
		}
		//ChannelType end

		//DesktopType begin
		desktopInnerTabItem1.setProperty(deviceConfig.getDesktopProperty(DesktopType.TYPE_TABLE_TAB), false, false, false, true);
		desktopInnerTabItem2.setProperty(deviceConfig.getDesktopProperty(DesktopType.TYPE_DIGITAL_TAB), false, false, false, true);
		desktopInnerTabItem3.setProperty(deviceConfig.getDesktopProperty(DesktopType.TYPE_ANALOG_TAB), false, false, false, true);
		desktopInnerTabItem4.setProperty(deviceConfig.getDesktopProperty(DesktopType.TYPE_VOLTAGE_PER_CELL_TAB), false, false, false, true);
		desktopComposite.redraw();
		//DesktopType end
}

	/**
	 * enable or disable data block optional properties
	 */
	void enableDataBlockOptionalPart(boolean enable) {
		//dataBlockOptionalGroup.setEnabled(enable);
		dataBlockOptionalEnableButton.setText(enable ? "disable" : "enable");
		dataBlockCheckSumFormatLabel.setEnabled(enable);
		dataBlockcheckSumFormatCombo.setEnabled(enable);
		dataBlockCheckSumLabel.setEnabled(enable);
		dataBlockCheckSumTypeCombo.setEnabled(enable);
		dataBlockEndingLabel.setEnabled(enable);
		dataBlockEndingText.setEnabled(enable);
		dataBlockOptionalGroup.redraw();
//		if (!enable) {
//			deviceConfig.setDataBlockCheckSumFormat(dataBlockcheckSumFormat = null);;
//			deviceConfig.setDataBlockCheckSumType(dataBlockCheckSumType = null);
//			deviceConfig.setDataBlockEnding(StringHelper.convert2ByteArray(dataBlockEnding = "0"));
//		}
	}
}
