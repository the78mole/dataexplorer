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
import java.math.BigInteger;
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
import org.eclipse.swt.layout.FormData;
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
import osde.device.ChecksumType;
import osde.device.DataTypes;
import osde.device.DesktopType;
import osde.device.DeviceConfiguration;
import osde.device.FlowControlType;
import osde.device.FormatType;
import osde.device.ObjectFactory;
import osde.device.ParityType;
import osde.device.PropertyType;
import osde.device.StopBitsType;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.FileUtils;
import osde.utils.StringHelper;


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
 * Dialog class enable to edit existing and create new device property files 
 * @author Winfried Brügmann
 */
public class DevicePropertiesEditor extends org.eclipse.swt.widgets.Dialog {
	private static final String	MODE_STATE_KEY	= "MODE_STATE_KEY";

	final static Logger						log			= Logger.getLogger(DevicePropertiesEditor.class.getName());

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
	
	CTabItem serialPortTabItem;
	Composite serialPortComposite, timeOutComposite;
	Label serialPortDescriptionLabel, timeOutDescriptionLabel;
	Label portNameLabel, baudeRateLabel, dataBitsLabel, stopBitsLabel, parityLabel, flowControlLabel, rtsLabel, dtrLabel, timeOutLabel;
	Text	portNameText;
	CCombo	baudeRateCombo, dataBitsCombo, stopBitsCombo, parityCombo, flowControlCombo;
	Button	isRTSButton, isDTRButton, timeOutButton;
	Label	_RTOCharDelayTimeLabel, _RTOExtraDelayTimeLabel, _WTOCharDelayTimeLabel, _WTOExtraDelayTimeLabel;
	Text	_RTOCharDelayTimeText, _RTOExtraDelayTimeText, _WTOCharDelayTimeText, _WTOExtraDelayTimeText;

	String portName = OSDE.STRING_EMPTY;
	int baudeRateIndex = 0;
	int dataBitsIndex = 0;
	int stopBitsIndex = 0;
	int parityIndex = 0;
	int flowControlIndex = 0;
	boolean isRTS = false;
	boolean isDTR = false;
	boolean useTimeOut = false;
	int RTOCharDelayTime = 0;
	int RTOExtraDelayTime = 0;
	int WTOCharDelayTime = 0;
	int WTOExtraDelayTime = 0;
	
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
	
	FormatType dataBlockFormat = FormatType.BINARY, dataBlockcheckSumFormat = FormatType.BINARY;
	int dataBlockSize = 30;
	ChecksumType dataBlockCheckSumType = ChecksumType.XOR;
	String dataBlockEnding = "0a0d";
	
	CTabItem modeStateTabItem, modeStateInnerTabItem;
	Composite modeStateComposite;
	Label modeStateDescriptionLabel;
	Button addButton;
	CTabFolder modeStateTabFolder;
	PropertyTypeComposite modeStateItemComposite;
	
	CTabItem channelConfigurationTabItem;
	Composite channleConfigComposite;
	Label channelConfigDescriptionLabel;
	CTabFolder channelConfigInnerTabFolder;
	CTabItem channelConfigTabItem;
	Composite channelConfigComposite;
	Label channelConfigLabel;
	CCombo channelConfigTypeCombo;
	Text channelConfigText;
	
	CTabFolder measurementsTabFolder;
	CTabItem measurementTabItem;
	Composite measurementsComposite;

	Label measurementNameLabel, measurementSymbolLabel, measurementUnitLabel, measurementEnableLabel;
	private Label propertyDescriptionLabel;
	private Button addMeasurementButton;
	private Composite statisticsComposite;
	private Text propertyDescriptionText;
	private Text propertyValueText;
	private Label propertyValueLabel;
	private CCombo propertyTypeCombo;
	private Label propertyTypeLabel;
	private CCombo propertyNameCombo;
	private Composite MeasurementPropertiesComposite;
	private Label propertyNameLabel;
	private Label label1;
	private CTabItem measurementPropertyTabItem;
	private CTabFolder measurementsPropertiesTabFolder;
	private CTabItem measurementStatisticsTabItem;
	private CTabItem measurementPropertiesTabItem;
	private CTabFolder channelConfigMeasurementPropertiesTabFolder;
	private Button channelConfigAddButton;
	Text measurementNameText, measurementSymbolText, measurementUnitText;
	Button measurementEnableButton;

	
	
	CTabItem destopTabItem, desktopInnerTabItem1, desktopInnerTabItem2, desktopInnerTabItem3, desktopInnerTabItem4;
	Composite desktopComposite;
	Label destktopDescriptionLabel;	
	CTabFolder desktopTabFolder;
	PropertyTypeComposite tablePropertyComposite, tablePropertyComposite2, tablePropertyComposite3, tablePropertyComposite4;
	
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
							catch (Exception e) {
								log.log(Level.SEVERE, e.getMessage(), e);
							}
						}
					}
				});
			}
			{
				devicePropFileNamelabel = new Label(dialogShell, SWT.RIGHT);
				devicePropFileNamelabel.setText("FileName : ");
				devicePropFileNamelabel.setToolTipText("specify a filename to be used for device property file to edit");
				devicePropFileNamelabel.setBounds(0, 12, 100, 16);
			}
			{
				closeButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				closeButton.setText("close");
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
				{
					deviceTabItem = new CTabItem(tabFolder, SWT.NONE);
					deviceTabItem.setText("Device");
					{
						deviceComposite = new Composite(tabFolder, SWT.NONE);
						deviceComposite.setLayout(null);
						deviceTabItem.setControl(deviceComposite);
						{
							deviceDescriptionlabel = new Label(deviceComposite, SWT.CENTER | SWT.WRAP);
							deviceDescriptionlabel.setText("This section describes the main parameter of any device.");
							deviceDescriptionlabel.setBounds(12, 8, 604, 50);
						}
						{
							deviceLabelComposite = new Composite(deviceComposite, SWT.NONE);
							deviceLabelComposite.setLayout(null);
							deviceLabelComposite.setBounds(20, 70, 145, 190);
							{
								deviceNameLabel = new Label(deviceLabelComposite, SWT.RIGHT);
								deviceNameLabel.setText("Name");
								deviceNameLabel.setForeground(osde.ui.SWTResourceManager.getColor(SWT.COLOR_BLACK));
								deviceNameLabel.setBounds(0, 0, 137, 16);
							}
							{
								manufacturerLabel = new Label(deviceLabelComposite, SWT.RIGHT);
								manufacturerLabel.setText("Manufacturer");
								manufacturerLabel.setBounds(0, 33, 137, 19);
							}
							{
								manufURLabel = new Label(deviceLabelComposite, SWT.RIGHT);
								manufURLabel.setText("Manufactorer URL");
								manufURLabel.setBounds(0, 64, 137, 19);
							}
							{
								imageFileNameLabel = new Label(deviceLabelComposite, SWT.RIGHT);
								imageFileNameLabel.setText("Image Filename");
								imageFileNameLabel.setBounds(0, 95, 137, 19);
							}
							{
								usageLabel = new Label(deviceLabelComposite, SWT.RIGHT);
								usageLabel.setText("usage");
								usageLabel.setBounds(0, 126, 137, 19);
							}
							{
								groupLabel = new Label(deviceLabelComposite, SWT.RIGHT);
								groupLabel.setText("Group ID");
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
					createSeriaPortType();
				}
				{
					timeBaseTabItem = new CTabItem(tabFolder, SWT.NONE);
					timeBaseTabItem.setText("Time Base");
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
							timeBaseDescriptionLabel.setBounds(17, 12, 591, 71);
						}
						{
							timeBaseNameLabel = new Label(timeBaseComposite, SWT.RIGHT);
							timeBaseNameLabel.setText("Name");
							timeBaseNameLabel.setBounds(142, 95, 150, 20);
						}
						{
							timeBaseNameText = new Text(timeBaseComposite, SWT.CENTER | SWT.BORDER);
							timeBaseNameText.setText(Messages.getString(MessageIds.OSDE_MSGT0271).split(OSDE.STRING_BLANK)[0]);
							timeBaseNameText.setBounds(322, 94, 60, 20);
							timeBaseNameText.setEditable(false);
						}
						{
							timeBaseSymbolLabel = new Label(timeBaseComposite, SWT.RIGHT);
							timeBaseSymbolLabel.setText("Symbol");
							timeBaseSymbolLabel.setBounds(142, 125, 150, 20);
						}
						{
							timeBaseSymbolText = new Text(timeBaseComposite, SWT.CENTER | SWT.BORDER);
							timeBaseSymbolText.setText(Messages.getString(MessageIds.OSDE_MSGT0271).split(OSDE.STRING_BLANK)[1]);
							timeBaseSymbolText.setBounds(322, 124, 60, 20);
							timeBaseSymbolText.setEditable(false);
						}
						{
							timeBaseUnitLabel = new Label(timeBaseComposite, SWT.RIGHT);
							timeBaseUnitLabel.setText("Unit");
							timeBaseUnitLabel.setBounds(142, 155, 150, 20);
						}
						{
							timeBaseUnitText = new Text(timeBaseComposite, SWT.CENTER | SWT.BORDER);
							timeBaseUnitText.setText(Messages.getString(MessageIds.OSDE_MSGT0271).split(OSDE.STRING_BLANK)[3]);
							timeBaseUnitText.setBounds(322, 154, 60, 20);
							timeBaseUnitText.setEditable(false);
						}
						{
							timeBaseTimeStepLabel = new Label(timeBaseComposite, SWT.RIGHT);
							timeBaseTimeStepLabel.setText("time step");
							timeBaseTimeStepLabel.setBounds(142, 185, 150, 20);
						}
						{
							timeBaseTimeStepText = new Text(timeBaseComposite, SWT.RIGHT | SWT.BORDER);
							timeBaseTimeStepText.setText("1000.0");
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
					channelConfigurationTabItem = new CTabItem(tabFolder, SWT.NONE);
					channelConfigurationTabItem.setText("Channel/Configuration");
					{
						channleConfigComposite = new Composite(tabFolder, SWT.NONE);
						channleConfigComposite.setLayout(null);
						channelConfigurationTabItem.setControl(channleConfigComposite);
						{
							channelConfigDescriptionLabel = new Label(channleConfigComposite, SWT.CENTER | SWT.WRAP);
							channelConfigDescriptionLabel.setText("Defines miscelanious visualisation properties of the application desktop.\nProbably there are more than one measurements to be described here.");
							channelConfigDescriptionLabel.setBounds(12, 5, 602, 38);
						}
						{
							channelConfigInnerTabFolder = new CTabFolder(channleConfigComposite, SWT.CLOSE | SWT.BORDER);
							channelConfigInnerTabFolder.setBounds(0, 49, 626, 285);
							{
								channelConfigTabItem = new CTabItem(channelConfigInnerTabFolder, SWT.NONE);
								channelConfigTabItem.setText(" 1");
								{
									channelConfigComposite = new Composite(channelConfigInnerTabFolder, SWT.NONE);
									channelConfigTabItem.setControl(channelConfigComposite);
									channelConfigComposite.setLayout(null);
									{
										channelConfigTypeCombo = new CCombo(channelConfigComposite, SWT.BORDER);
										channelConfigTypeCombo.setBounds(6, 9, 121, 19);
									}
									{
										channelConfigText = new Text(channelConfigComposite, SWT.BORDER);
										channelConfigText.setText("Outlet");
										channelConfigText.setBounds(147, 9, 128, 19);
									}
									{
										channelConfigLabel = new Label(channelConfigComposite, SWT.CENTER);
										channelConfigLabel.setText("complete definitions before adding new");
										channelConfigLabel.setBounds(289, 9, 279, 19);
									}
									{
										measurementsTabFolder = new CTabFolder(channelConfigComposite, SWT.CLOSE | SWT.BORDER);
										measurementsTabFolder.setBounds(0, 34, 622, 225);
										{
											measurementTabItem = new CTabItem(measurementsTabFolder, SWT.NONE);
											measurementTabItem.setText(" 1");
											{
												measurementsComposite = new Composite(measurementsTabFolder, SWT.NONE);
												measurementsComposite.setLayout(null);
												measurementTabItem.setControl(measurementsComposite);
												{
													measurementNameLabel = new Label(measurementsComposite, SWT.RIGHT);
													measurementNameLabel.setText("name");
													measurementNameLabel.setBounds(10, 37, 60, 20);
												}
												{
													measurementNameText = new Text(measurementsComposite, SWT.BORDER);
													measurementNameText.setBounds(80, 37, 145, 20);
												}
												{
													measurementSymbolLabel = new Label(measurementsComposite, SWT.RIGHT);
													measurementSymbolLabel.setText("symbol");
													measurementSymbolLabel.setBounds(10, 62, 60, 20);
												}
												{
													measurementSymbolText = new Text(measurementsComposite, SWT.BORDER);
													measurementSymbolText.setBounds(80, 62, 145, 20);
												}
												{
													measurementUnitLabel = new Label(measurementsComposite, SWT.RIGHT);
													measurementUnitLabel.setText("unit");
													measurementUnitLabel.setBounds(10, 87, 60, 20);
												}
												{
													measurementUnitText = new Text(measurementsComposite, SWT.BORDER);
													measurementUnitText.setBounds(80, 87, 145, 20);
												}
												{
													measurementEnableLabel = new Label(measurementsComposite, SWT.RIGHT);
													measurementEnableLabel.setText("active");
													measurementEnableLabel.setBounds(10, 112, 60, 20);
												}
												{
													measurementEnableButton = new Button(measurementsComposite, SWT.CHECK);
													measurementEnableButton.setBounds(82, 112, 145, 20);
												}
												{
													channelConfigMeasurementPropertiesTabFolder = new CTabFolder(measurementsComposite, SWT.BORDER);
													channelConfigMeasurementPropertiesTabFolder.setBounds(237, 0, 379, 199);
													{
														measurementPropertiesTabItem = new CTabItem(channelConfigMeasurementPropertiesTabFolder, SWT.NONE);
														measurementPropertiesTabItem.setShowClose(true);
														measurementPropertiesTabItem.setText("Properties");
														{
															measurementsPropertiesTabFolder = new CTabFolder(channelConfigMeasurementPropertiesTabFolder, SWT.NONE);
															measurementPropertiesTabItem.setControl(measurementsPropertiesTabFolder);
															{
																measurementPropertyTabItem = new CTabItem(measurementsPropertiesTabFolder, SWT.NONE);
																measurementPropertyTabItem.setShowClose(true);
																measurementPropertyTabItem.setText("Property");
																{
																	MeasurementPropertiesComposite = new PropertyTypeComposite(measurementsPropertiesTabFolder, SWT.NONE);
																	measurementPropertyTabItem.setControl(MeasurementPropertiesComposite);
//																	composite1.setLayout(null);
//																	{
//																		propertyNameLabel = new Label(composite1, SWT.RIGHT);
//																		propertyNameLabel.setText("name");
//																		propertyNameLabel.setBounds(5, 5, 85, 20);
//																	}
//																	{
//																		propertyNameCombo = new CCombo(composite1, SWT.BORDER);
//																		propertyNameCombo.setBounds(100, 5, 150, 20);
//																	}
//																	{
//																		propertyTypeLabel = new Label(composite1, SWT.RIGHT);
//																		propertyTypeLabel.setText("type");
//																		propertyTypeLabel.setBounds(5, 30, 85, 20);
//																	}
//																	{
//																		propertyTypeCombo = new CCombo(composite1, SWT.BORDER);
//																		propertyTypeCombo.setBounds(100, 30, 150, 20);
//																	}
//																	{
//																		propertyValueLabel = new Label(composite1, SWT.RIGHT);
//																		propertyValueLabel.setText("value");
//																		propertyValueLabel.setBounds(5, 55, 85, 20);
//																	}
//																	{
//																		propertyValueText = new Text(composite1, SWT.BORDER);
//																		propertyValueText.setBounds(100, 55, 150, 20);
//																	}
//																	{
//																		propertyDescriptionLabel = new Label(composite1, SWT.RIGHT);
//																		propertyDescriptionLabel.setText("description");
//																		propertyDescriptionLabel.setBounds(5, 80, 85, 20);
//																	}
//																	{
//																		propertyDescriptionText = new Text(composite1, SWT.BORDER);
//																		propertyDescriptionText.setText("describe the sense of the property");
//																		propertyDescriptionText.setBounds(100, 81, 265, 48);
//																	}
																}
															}
															measurementsPropertiesTabFolder.setSelection(0);
														}
													}
													{
														measurementStatisticsTabItem = new CTabItem(channelConfigMeasurementPropertiesTabFolder, SWT.NONE);
														measurementStatisticsTabItem.setText("Statistics");
														{
															statisticsComposite = new StatisticsComposite(channelConfigMeasurementPropertiesTabFolder);
															measurementStatisticsTabItem.setControl(statisticsComposite);

														}
													}
													channelConfigMeasurementPropertiesTabFolder.setSelection(0);
												}
												{
													label1 = new Label(measurementsComposite, SWT.NONE);
													label1.setText("measurement");
													label1.setBounds(10, 8, 120, 20);
												}
												{
													addMeasurementButton = new Button(measurementsComposite, SWT.PUSH | SWT.CENTER);
													addMeasurementButton.setText("+");
													addMeasurementButton.setBounds(182, 7, 40, 20);
												}
											}
										}
										measurementsTabFolder.setSelection(0);
									}
									{
										channelConfigAddButton = new Button(channelConfigComposite, SWT.PUSH | SWT.CENTER);
										channelConfigAddButton.setText("+");
										channelConfigAddButton.setBounds(574, 9, 42, 19);
										channelConfigAddButton.setToolTipText("add a new channel or configuration, this will inherit all definitions from precessor");
										channelConfigAddButton.setSize(40, 20);
									}
								}
							}
							channelConfigInnerTabFolder.setSelection(0);
						}
					}
				}
				{
					destopTabItem = new CTabItem(tabFolder, SWT.NONE);
					destopTabItem.setText("Application Desktop");
					{
						desktopComposite = new Composite(tabFolder, SWT.NONE);
						desktopComposite.setLayout(null);
						destopTabItem.setControl(desktopComposite);
						desktopComposite.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								log.log(Level.FINEST, "desktopComposite.paintControl, event="+evt);
								tablePropertyComposite.redraw();
								tablePropertyComposite2.redraw();
								tablePropertyComposite3.redraw();
								tablePropertyComposite4.redraw();
							}
						});
						{
							destktopDescriptionLabel = new Label(desktopComposite, SWT.CENTER | SWT.WRAP);
							destktopDescriptionLabel.setText("This section defines miscelanious visualisation properties of the application desktop. \nAs example showing the data table contnet or should be updated and displyed, ....");
							destktopDescriptionLabel.setBounds(12, 5, 602, 57);
						}
						{
							desktopTabFolder = new CTabFolder(desktopComposite, SWT.BORDER);
							GridLayout appDesktopTabCompositeLayout = new GridLayout();
							appDesktopTabCompositeLayout.makeColumnsEqualWidth = true;
							desktopTabFolder.setLayout(appDesktopTabCompositeLayout);
							desktopTabFolder.setBounds(165, 68, 300, 196);
							{
								desktopInnerTabItem1 = new CTabItem(desktopTabFolder, SWT.NONE);
								desktopInnerTabItem1.setText("DataTable");
								{
									tablePropertyComposite = new PropertyTypeComposite(desktopTabFolder, SWT.NONE);
									desktopInnerTabItem1.setControl(tablePropertyComposite);
								}
							}
							{
								desktopInnerTabItem2 = new CTabItem(desktopTabFolder, SWT.NONE);
								desktopInnerTabItem2.setText("Digital");
								{
									tablePropertyComposite2 = new PropertyTypeComposite(desktopTabFolder, SWT.NONE);
									desktopInnerTabItem2.setControl(tablePropertyComposite2);
								}
							}
							{
								desktopInnerTabItem3 = new CTabItem(desktopTabFolder, SWT.NONE);
								desktopInnerTabItem3.setText("Analog");
								{
									tablePropertyComposite3 = new PropertyTypeComposite(desktopTabFolder, SWT.NONE);
									desktopInnerTabItem3.setControl(tablePropertyComposite3);
								}
							}
							{
								desktopInnerTabItem4 = new CTabItem(desktopTabFolder, SWT.NONE);
								desktopInnerTabItem4.setText("Cellvoltage");
								{
									tablePropertyComposite4 = new PropertyTypeComposite(desktopTabFolder, SWT.NONE);
									desktopInnerTabItem4.setControl(tablePropertyComposite4);
								}
							}
							desktopTabFolder.setSelection(0);
						}
					}
				}
				{
					createInitialModeStateTabItem();
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
							if (tabItem.getText().equals("Mode State")) deviceConfig.removeModeStateType();
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

//	/**
//	 * 
//	 */
//	void createStatisticsEditor(CTabFolder useChannelConfigMeasurementPropertiesTabFolder) {
//		statisticsComposite = new Composite(useChannelConfigMeasurementPropertiesTabFolder, SWT.V_SCROLL);
//		statisticsComposite.setLayout(null);
//		measurementStatisticsTabItem.setControl(statisticsComposite);
//		{
//			staisticsMinButton = new Button(statisticsComposite, SWT.CHECK | SWT.RIGHT);
//			staisticsMinButton.setText("minimum");
//			staisticsMinButton.setBounds(10, 10, 90, 20);
//		}
//		{
//			statisticsAvgButton = new Button(statisticsComposite, SWT.CHECK | SWT.RIGHT);
//			statisticsAvgButton.setText("average");
//			statisticsAvgButton.setBounds(10, 35, 90, 20);
//		}
//		{
//			statisticsMaxButton = new Button(statisticsComposite, SWT.CHECK | SWT.RIGHT);
//			statisticsMaxButton.setText("maximum");
//			statisticsMaxButton.setBounds(10, 60, 90, 20);
//		}
//		{
//			statisticsSigmaButton = new Button(statisticsComposite, SWT.CHECK | SWT.RIGHT);
//			statisticsSigmaButton.setText("sigma");
//			statisticsSigmaButton.setBounds(10, 85, 90, 20);
//		}
//		{
//			triggerRefOrdinalButton = new Button(statisticsComposite, SWT.CHECK | SWT.RIGHT);
//			triggerRefOrdinalButton.setText("triggerRefOrdinal");
//			triggerRefOrdinalButton.setBounds(125, 10, 169, 20);
//			triggerRefOrdinalButton.setToolTipText("references the measurement ordinal where trigger level is set in case of trigger is defined (0=VoltageReceiver;1=Voltage,2=Current, ...)");
//		}
//		{
//			triggerRefOrdinalCombo = new CCombo(statisticsComposite, SWT.BORDER);
//			triggerRefOrdinalCombo.setBounds(298, 8, 54, 20);
//		}
//		{
//			sumByTriggerRefOrdinalButton = new Button(statisticsComposite, SWT.CHECK | SWT.RIGHT);
//			sumByTriggerRefOrdinalButton.setText("sumByTriggerRefOrdinal");
//			sumByTriggerRefOrdinalButton.setBounds(125, 30, 169, 21);
//		}
//		{
//			sumByTriggerRefOrdinalCombo = new CCombo(statisticsComposite, SWT.BORDER);
//			sumByTriggerRefOrdinalCombo.setBounds(297, 31, 54, 19);
//		}
//	}

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
				dataBlockDescriptionLabel.setBounds(12, 5, 602, 51);
			}
			{
				dataBlockRequiredGroup = new Group(dataBlockComposite, SWT.NONE);
				dataBlockRequiredGroup.setLayout(null);
				dataBlockRequiredGroup.setText("Required Entries");
				dataBlockRequiredGroup.setBounds(40, 80, 250, 170);
				dataBlockRequiredGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.log(Level.FINEST, "dataBlockRequiredGroup.paintControl, event="+evt);
						dataBlockFormatCombo.select(dataBlockFormat == FormatType.TEXT ? 0 : 1);
						dataBlockSizeText.setText(OSDE.STRING_EMPTY + dataBlockSize);
					}
				});
				{
					dataBlockFormatLabel = new Label(dataBlockRequiredGroup, SWT.RIGHT);
					dataBlockFormatLabel.setText("format");
					dataBlockFormatLabel.setBounds(19, 51, 85, 16);
				}
				{
					dataBlockFormatCombo = new CCombo(dataBlockRequiredGroup, SWT.BORDER);
					dataBlockFormatCombo.setBounds(125, 49, 84, 21);
					dataBlockFormatCombo.setItems(new java.lang.String[]{"TEXT","BINARY"});
					dataBlockFormatCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
					dataBlockFormatCombo.setLayout(null);
					dataBlockFormatCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "dataBlockFormatCombo.widgetSelected, event="+evt);
							dataBlockFormat = FormatType.valueOf(dataBlockFormatCombo.getText());
							if (deviceConfig != null) {
								deviceConfig.setDataBlockFormat(dataBlockFormat);
							}
						} 
					});
				}
				{
					dataBlockSizeLabel = new Label(dataBlockRequiredGroup, SWT.RIGHT);
					dataBlockSizeLabel.setText("size");
					dataBlockSizeLabel.setToolTipText("define size in bytes for BINARY, or value count for TEXT format");
					dataBlockSizeLabel.setBounds(19, 105, 85, 16);
				}
				{
					dataBlockSizeText = new Text(dataBlockRequiredGroup, SWT.RIGHT | SWT.BORDER);
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
				dataBlockOptionalGroup.setBounds(330, 80, 250, 170);
				dataBlockOptionalGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.log(Level.FINEST, "dataBlockOptionalGroup.paintControl, event="+evt);
						if (deviceConfig != null && deviceConfig.getDataBlockCheckSumFormat() != null && deviceConfig.getDataBlockCheckSumType() != null && deviceConfig.getDataBlockEnding() != null) {
							dataBlockOptionalEnableButton.setSelection(true);
							dataBlockcheckSumFormatCombo.select(dataBlockcheckSumFormat == FormatType.TEXT ? 0 : 1);
							dataBlockCheckSumTypeCombo.select(dataBlockCheckSumType == ChecksumType.XOR ? 0 : 1);
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
					dataBlockCheckSumFormatLabel.setBounds(6, 46, 122, 20);
				}
				{
					dataBlockCheckSumLabel = new Label(dataBlockOptionalGroup, SWT.RIGHT);
					dataBlockCheckSumLabel.setText("checkSum");
					dataBlockCheckSumLabel.setBounds(6, 77, 122, 20);
				}
				{
					dataBlockEndingLabel = new Label(dataBlockOptionalGroup, SWT.RIGHT);
					dataBlockEndingLabel.setText("ending [bytes]");
					dataBlockEndingLabel.setBounds(6, 111, 122, 20);
				}
				{
					dataBlockcheckSumFormatCombo = new CCombo(dataBlockOptionalGroup, SWT.BORDER);
					dataBlockcheckSumFormatCombo.setItems(new java.lang.String[]{"TEXT","BINARY"});
					dataBlockcheckSumFormatCombo.setBounds(143, 44, 92, 20);
					dataBlockcheckSumFormatCombo.setEditable(false);
					dataBlockcheckSumFormatCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
					dataBlockcheckSumFormatCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "dataBlockceckSumFormatCombo.widgetSelected, event="+evt);
							dataBlockcheckSumFormat = FormatType.valueOf(dataBlockcheckSumFormatCombo.getText());
							if (deviceConfig != null) {
								deviceConfig.setDataBlockCheckSumFormat(dataBlockcheckSumFormat);
							}
						}
					});
				}
				{
					dataBlockCheckSumTypeCombo = new CCombo(dataBlockOptionalGroup, SWT.RIGHT | SWT.BORDER);
					dataBlockCheckSumTypeCombo.setItems(new String[] {"XOR", "ADD"});
					dataBlockCheckSumTypeCombo.setBounds(143, 74, 92, 20);
					dataBlockCheckSumTypeCombo.setEditable(false);
					dataBlockCheckSumTypeCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
					dataBlockCheckSumTypeCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "dataBlockCheckSumCombo.widgetSelected, event="+evt);
							dataBlockCheckSumType = ChecksumType.valueOf(dataBlockCheckSumTypeCombo.getText());
							if (deviceConfig != null) {
								deviceConfig.setDataBlockCheckSumType(dataBlockCheckSumType);
							}
						}
					});
				}
				{
					dataBlockEndingText = new Text(dataBlockOptionalGroup, SWT.BORDER);
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
	 * create a new serial port type tabulator item
	 */
	CTabItem createSeriaPortType() {
		serialPortTabItem = new CTabItem(tabFolder, SWT.CLOSE, 1);
		serialPortTabItem.setText("Serial Port");
		serialPortTabItem.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent evt) {
				log.log(Level.FINEST, "serialPortTabItem.widgetDisposed, event="+evt);
				if (deviceConfig != null) {
					deviceConfig.removeSerialPortType();
				}
			}
		});
		{
			serialPortComposite = new Composite(tabFolder, SWT.NONE);
			serialPortComposite.setLayout(null);
			serialPortTabItem.setControl(serialPortComposite);
			serialPortComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.log(Level.FINEST, "serialPortComposite.paintControl, event=" + evt);
					portNameText.setText(portName);
					baudeRateCombo.select(baudeRateIndex);
					dataBitsCombo.select(dataBitsIndex);
					stopBitsCombo.select(stopBitsIndex);
					parityCombo.select(parityIndex);
					flowControlCombo.select(flowControlIndex);
					isRTSButton.setSelection(isRTS);
					isDTRButton.setSelection(isDTR);
				}
			});
			{
				serialPortDescriptionLabel = new Label(serialPortComposite, SWT.CENTER | SWT.WRAP);
				serialPortDescriptionLabel.setText("This optional section descibes the serial port configuration.\nFor devices where the data comes from file instead through serial communication, it can be removed.");
				serialPortDescriptionLabel.setBounds(12, 6, 602, 56);
			}
			{
				portNameLabel = new Label(serialPortComposite, SWT.RIGHT);
				portNameLabel.setText("port name");
				portNameLabel.setBounds(5, 74, 100, 20);
			}
			{
				portNameText = new Text(serialPortComposite, SWT.BORDER);
				portNameText.setBounds(141, 76, 180, 20);
				portNameText.setEditable(false);
			}
			{
				baudeRateLabel = new Label(serialPortComposite, SWT.RIGHT);
				baudeRateLabel.setText("baude rate");
				baudeRateLabel.setBounds(5, 99, 100, 20);
			}
			{
				baudeRateCombo = new CCombo(serialPortComposite, SWT.BORDER);
				baudeRateCombo.setItems(new String[] { "2400", "4800", "7200", "9600", "14400", "28800", "38400", "57600", "115200" });
				baudeRateCombo.setBounds(142, 101, 180, 20);
				baudeRateCombo.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "baudeRateCombo.widgetSelected, event=" + evt);
						if (deviceConfig != null) {
							deviceConfig.setBaudeRate(new BigInteger(baudeRateCombo.getText()));
						}
						baudeRateIndex = baudeRateCombo.getSelectionIndex();
					}
				});
			}
			{
				dataBitsLabel = new Label(serialPortComposite, SWT.RIGHT);
				dataBitsLabel.setText("data bits");
				dataBitsLabel.setBounds(5, 124, 100, 20);
			}
			{
				dataBitsCombo = new CCombo(serialPortComposite, SWT.BORDER);
				dataBitsCombo.setItems(new String[] {"5", "6", "7", "8"});
				dataBitsCombo.setBounds(142, 126, 180, 20);
				dataBitsCombo.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "dataBitsCombo.widgetSelected, event="+evt);
						if (deviceConfig != null) {
							deviceConfig.setDataBits(new BigInteger(dataBitsCombo.getText()));
						}
						dataBitsIndex = dataBitsCombo.getSelectionIndex();
					}
				});
			}
			{
				stopBitsLabel = new Label(serialPortComposite, SWT.RIGHT);
				stopBitsLabel.setText("stop bits");
				stopBitsLabel.setBounds(5, 149, 100, 20);
			}
			{
				stopBitsCombo = new CCombo(serialPortComposite, SWT.BORDER);
				stopBitsCombo.setItems(new String[] { "STOPBITS_1", "STOPBITS_2", "STOPBITS_1_5" });
				stopBitsCombo.setBounds(142, 151, 180, 20);
				stopBitsCombo.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "stopBitsCombo.widgetSelected, event="+evt);
						if (deviceConfig != null) {
							deviceConfig.setStopBits(StopBitsType.values()[stopBitsCombo.getSelectionIndex()]);
						}
						stopBitsIndex = stopBitsCombo.getSelectionIndex();
					}
				});
			}
			{
				parityLabel = new Label(serialPortComposite, SWT.RIGHT);
				parityLabel.setText("parity");
				parityLabel.setBounds(5, 174, 100, 20);
			}
			{
				parityCombo = new CCombo(serialPortComposite, SWT.BORDER);
				parityCombo.setItems(new String[] { "PARITY_NONE", "PARITY_ODD", "PARITY_EVEN", "PARITY_MARK", "PARITY_SPACE" });
				parityCombo.setBounds(142, 176, 180, 20);
				parityCombo.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "parityCombo.widgetSelected, event="+evt);
						if (deviceConfig != null) {
							deviceConfig.setParity(ParityType.values()[parityCombo.getSelectionIndex()]);
						}
						parityIndex = parityCombo.getSelectionIndex();
					}
				});
			}
			{
				flowControlLabel = new Label(serialPortComposite, SWT.RIGHT);
				flowControlLabel.setText("flow control");
				flowControlLabel.setBounds(5, 199, 100, 20);
			}
			{
				flowControlCombo = new CCombo(serialPortComposite, SWT.BORDER);
				flowControlCombo.setItems(new String[] { "FLOWCONTROL_NONE", "FLOWCONTROL_RTSCTS_IN", "FLOWCONTROL_RTSCTS_OUT", "FLOWCONTROL_XONXOFF_IN", "FLOWCONTROL_XONXOFF_OUT" });
				flowControlCombo.setBounds(142, 201, 180, 20);
				flowControlCombo.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "flowControlCombo.widgetSelected, event="+evt);
						if (deviceConfig != null) {
							deviceConfig.setFlowCtrlMode(FlowControlType.values()[flowControlCombo.getSelectionIndex()]);
						}
						flowControlIndex = flowControlCombo.getSelectionIndex();
					}
				});
			}
			{
				rtsLabel = new Label(serialPortComposite, SWT.RIGHT);
				rtsLabel.setText(" RTS");
				rtsLabel.setBounds(5, 224, 100, 20);
			}
			{
				isRTSButton = new Button(serialPortComposite, SWT.CHECK);
				isRTSButton.setBounds(142, 224, 180, 20);
				isRTSButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "isRTSButton.widgetSelected, event="+evt);
						if (deviceConfig != null) {
							deviceConfig.setIsRTS(isRTSButton.getSelection());
						}
						isRTS = isRTSButton.getSelection();
					}
				});
			}
			{
				dtrLabel = new Label(serialPortComposite, SWT.RIGHT);
				dtrLabel.setText(" DTR");
				dtrLabel.setBounds(5, 249, 100, 20);
			}
			{
				isDTRButton = new Button(serialPortComposite, SWT.CHECK);
				isDTRButton.setBounds(142, 249, 180, 20);
				isDTRButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "isDTRButton.widgetSelected, event="+evt);
						if (deviceConfig != null) {
							deviceConfig.setIsDTR(isDTRButton.getSelection());
						}
						isDTR = isDTRButton.getSelection();
					}
				});
			}
			{
				timeOutComposite = new Composite(serialPortComposite, SWT.BORDER);
				timeOutComposite.setLayout(null);
				timeOutComposite.setBounds(356, 78, 250, 207);
				timeOutComposite.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.log(Level.FINEST, "dialogShell.paintControl, event="+evt);
						_RTOCharDelayTimeText.setText(""+RTOCharDelayTime);
						_RTOExtraDelayTimeText.setText(""+RTOExtraDelayTime);
						_WTOCharDelayTimeText.setText(""+WTOCharDelayTime);
						_WTOExtraDelayTimeText.setText(""+WTOExtraDelayTime);
						
						timeOutButton.setSelection(useTimeOut);
						if (timeOutButton.getSelection()) {
							_RTOCharDelayTimeLabel.setEnabled(true);
							_RTOCharDelayTimeText.setEnabled(true);
							_RTOExtraDelayTimeLabel.setEnabled(true);
							_RTOExtraDelayTimeText.setEnabled(true);
							_WTOCharDelayTimeLabel.setEnabled(true);
							_WTOCharDelayTimeText.setEnabled(true);
							_WTOExtraDelayTimeLabel.setEnabled(true);
							_WTOExtraDelayTimeText.setEnabled(true);
						}
						else {
							_RTOCharDelayTimeLabel.setEnabled(false);
							_RTOCharDelayTimeText.setEnabled(false);
							_RTOExtraDelayTimeLabel.setEnabled(false);
							_RTOExtraDelayTimeText.setEnabled(false);
							_WTOCharDelayTimeLabel.setEnabled(false);
							_WTOCharDelayTimeText.setEnabled(false);
							_WTOExtraDelayTimeLabel.setEnabled(false);
							_WTOExtraDelayTimeText.setEnabled(false);
						}
					}
				});
				{
					timeOutLabel = new Label(timeOutComposite, SWT.RIGHT);
					timeOutLabel.setText("specify time out");
					timeOutLabel.setBounds(6, 58, 140, 20);
				}
				{
					timeOutButton = new Button(timeOutComposite, SWT.CHECK);
					timeOutButton.setBounds(161, 56, 70, 20);
					timeOutButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "timeOutButton.widgetSelected, event="+evt);
							useTimeOut = timeOutButton.getSelection();
							if (useTimeOut) {
								if (deviceConfig != null) {
									RTOCharDelayTime = deviceConfig.getRTOCharDelayTime();
									RTOExtraDelayTime = deviceConfig.getRTOExtraDelayTime();
									WTOCharDelayTime = deviceConfig.getWTOCharDelayTime();
									WTOExtraDelayTime = deviceConfig.getWTOExtraDelayTime();
								}
								else {
									RTOCharDelayTime = 0;
									RTOExtraDelayTime = 0;
									WTOCharDelayTime = 0;
									WTOExtraDelayTime = 0;
								}
							}
							else {
								if (deviceConfig != null) {
									deviceConfig.removeSerialPortTimeOut();
								}
									RTOCharDelayTime = 0;
									RTOExtraDelayTime = 0;
									WTOCharDelayTime = 0;
									WTOExtraDelayTime = 0;
							}
							timeOutComposite.redraw();
						}
					});
				}
				{
					_RTOCharDelayTimeLabel = new Label(timeOutComposite, SWT.RIGHT);
					_RTOCharDelayTimeLabel.setText("RTOCharDelayTime");
					_RTOCharDelayTimeLabel.setBounds(6, 88, 140, 20);
				}
				{
					_RTOCharDelayTimeText = new Text(timeOutComposite, SWT.BORDER);
					_RTOCharDelayTimeText.setBounds(162, 86, 70, 20);
					_RTOCharDelayTimeText.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent evt) {
							log.log(Level.FINEST, "_RTOCharDelayTimeText.verifyText, event="+evt);
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
					_RTOCharDelayTimeText.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent evt) {
							log.log(Level.FINEST, "_RTOCharDelayTimeText.keyReleased, event="+evt);
							RTOCharDelayTime = Integer.parseInt(_RTOCharDelayTimeText.getText());
							if(deviceConfig != null) {
								deviceConfig.setRTOCharDelayTime(RTOCharDelayTime);
							}
						}
					});
				}
				{
					_RTOExtraDelayTimeLabel = new Label(timeOutComposite, SWT.RIGHT);
					_RTOExtraDelayTimeLabel.setText("RTOExtraDelayTime");
					_RTOExtraDelayTimeLabel.setBounds(6, 118, 140, 20);
				}
				{
					_RTOExtraDelayTimeText = new Text(timeOutComposite, SWT.BORDER);
					_RTOExtraDelayTimeText.setBounds(162, 116, 70, 20);
					_RTOExtraDelayTimeText.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent evt) {
							log.log(Level.FINEST, "_RTOExtraDelayTimeText.verifyText, event="+evt);
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
					_RTOExtraDelayTimeText.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent evt) {
							log.log(Level.FINEST, "_RTOExtraDelayTimeText.keyReleased, event="+evt);
							RTOExtraDelayTime = Integer.parseInt(_RTOExtraDelayTimeText.getText());
							if(deviceConfig != null) {
								deviceConfig.setRTOExtraDelayTime(RTOExtraDelayTime);
							}
						}
					});
				}
				{
					_WTOCharDelayTimeLabel = new Label(timeOutComposite, SWT.RIGHT);
					_WTOCharDelayTimeLabel.setText("WTOCharDelayTime");
					_WTOCharDelayTimeLabel.setBounds(6, 148, 140, 20);
				}
				{
					_WTOCharDelayTimeText = new Text(timeOutComposite, SWT.BORDER);
					_WTOCharDelayTimeText.setBounds(162, 146, 70, 20);
					_WTOCharDelayTimeText.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent evt) {
							log.log(Level.FINEST, "_WRTOCharDelayTimeText.verifyText, event="+evt);
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
					_WTOCharDelayTimeText.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent evt) {
							log.log(Level.FINEST, "_WRTOCharDelayTimeText.keyReleased, event="+evt);
							WTOCharDelayTime = Integer.parseInt(_WTOCharDelayTimeText.getText());
							if(deviceConfig != null) {
								deviceConfig.setWTOCharDelayTime(WTOCharDelayTime);
							}
						}
					});
				}
				{
					_WTOExtraDelayTimeLabel = new Label(timeOutComposite, SWT.RIGHT);
					_WTOExtraDelayTimeLabel.setText("WTOExtraDelayTime");
					_WTOExtraDelayTimeLabel.setBounds(6, 178, 140, 20);
				}
				{
					_WTOExtraDelayTimeText = new Text(timeOutComposite, SWT.BORDER);
					_WTOExtraDelayTimeText.setBounds(162, 176, 70, 20);
					_WTOExtraDelayTimeText.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent evt) {
							log.log(Level.FINEST, "_WTOExtraDelayTimeText.verifyText, event="+evt);
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
					_WTOExtraDelayTimeText.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent evt) {
							log.log(Level.FINEST, "_WTOExtraDelayTimeText.keyReleased, event="+evt);
							WTOExtraDelayTime = Integer.parseInt(_WTOExtraDelayTimeText.getText());
							if(deviceConfig != null) {
								deviceConfig.setWTOExtraDelayTime(WTOExtraDelayTime);
							}
						}
					});
				}
				{
					timeOutDescriptionLabel = new Label(timeOutComposite, SWT.WRAP);
					timeOutDescriptionLabel.setText("Time out section describes Read and Write delay time. This delay and extra delay are only required in special purpose. ");
					timeOutDescriptionLabel.setBounds(6, 3, 232, 52);
				}
			}
		}
		return serialPortTabItem;
	}

	/**
	 * create a new mode state tabulator with one mode state entry
	 */
	void createInitialModeStateTabItem() {
		modeStateTabItem = new CTabItem(tabFolder, SWT.CLOSE);
		modeStateTabItem.setText("Mode State");
		{
			modeStateComposite = new Composite(tabFolder, SWT.NONE);
			modeStateComposite.setLayout(null);
			modeStateTabItem.setControl(modeStateComposite);
			modeStateComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.log(Level.FINEST, "modeStateComposite.paintControl, event="+evt);
					for (Control child : modeStateTabFolder.getChildren()) {
						((PropertyTypeComposite)child).redraw();
					}
				}
			});
			{
				modeStateDescriptionLabel = new Label(modeStateComposite, SWT.LEFT);
				modeStateDescriptionLabel.setText("This section defines device processing states, like \n  \t1. charge\n  \t2. discharge, ...");
				modeStateDescriptionLabel.setBounds(165, 4, 449, 55);
			}
			{
				modeStateTabFolder = new CTabFolder(modeStateComposite, SWT.BORDER);
				modeStateTabFolder.setBounds(165, 65, 300, 207);
				modeStateTabFolder.setSelection(0);
				{
					modeStateInnerTabItem = new CTabItem(modeStateTabFolder, SWT.NONE);
					modeStateInnerTabItem.setText("ModeState " + modeStateTabFolder.getItemCount());
					modeStateInnerTabItem.setShowClose(true);
					modeStateInnerTabItem.setData(MODE_STATE_KEY, modeStateTabFolder.getItemCount());
					{
						modeStateItemComposite = new PropertyTypeComposite(modeStateTabFolder, SWT.NONE);
						modeStateInnerTabItem.setControl(modeStateItemComposite);
					}
				}
				modeStateTabFolder.setSelection(0);
				modeStateTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
					public void close(CTabFolderEvent evt) {
						log.log(Level.FINEST, "modeStateTabFolder.close, event="+evt);
						if (deviceConfig != null) {
							int childIndex = Integer.parseInt(evt.item.toString().split(" |}")[2]) - 1;
							modeStateTabFolder.getChildren()[childIndex].dispose();
							deviceConfig.removeModeStateType(deviceConfig.getModeStateType().getProperty().get(childIndex));
						}
						evt.item.dispose();
					}
				});
			}
			{
				addButton = new Button(modeStateComposite, SWT.PUSH | SWT.CENTER);
				addButton.setText("add a new state");
				addButton.setBounds(165, 284, 300, 30);
				addButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "addButton.widgetSelected, event="+evt);
						{
							modeStateInnerTabItem = new CTabItem(modeStateTabFolder, SWT.NONE);
							modeStateInnerTabItem.setText("ModeState " + modeStateTabFolder.getItemCount());
							modeStateInnerTabItem.setShowClose(true);
							{
								modeStateItemComposite = new PropertyTypeComposite(modeStateTabFolder, SWT.NONE);
								modeStateInnerTabItem.setControl(modeStateItemComposite);
								PropertyType property = new ObjectFactory().createPropertyType();
								property.setName("new mode state");
								property.setType(DataTypes.INTEGER);
								property.setValue(OSDE.STRING_EMPTY+modeStateTabFolder.getItemCount());
								property.setDescription("new mode state description");
								deviceConfig.appendModeStateType(property);
								update();
							}
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
	 * search the index of a given string within the items of a combo box items
	 * @param useCombo
	 * @param searchString
	 * @return
	 */
	private int getSelectionIndex(CCombo useCombo, String searchString) {
		int searchIndex = 0;
		for (String item : useCombo.getItems()) {
			if (item.equals(searchString)) break;
			++searchIndex;
		}
		return searchIndex;
	}

	/**
	 * update internal variables by device properties
	 */
	private void update() {
		deviceName = deviceConfig.getName();
		manufacturer = deviceConfig.getManufacturer();
		manufacuturerURL = deviceConfig.getManufacturerURL();
		imageFileName = deviceConfig.getImageFileName();
		isDeviceUsed = deviceConfig.isUsed();
		deviceGroup = deviceConfig.getDeviceGroup();
		devicePropsComposite.redraw();
		
		if (deviceConfig.getSerialPortType() == null && !serialPortTabItem.isDisposed()) {
			serialPortTabItem.dispose();
		}
		else {
			if (deviceConfig.getSerialPortType() != null && serialPortTabItem.isDisposed()) {
				createSeriaPortType();			
			}
			if (deviceConfig.getSerialPortType() != null && !serialPortTabItem.isDisposed()) {
				String tmpPortString = OSDE.IS_WINDOWS ? "COM1" : OSDE.IS_LINUX ? "/dev/ttyS0" : OSDE.IS_MAC ? "/dev/tty.usbserial" : "COMx";
				deviceConfig.setPort(tmpPortString);
				portName = tmpPortString;
				baudeRateIndex = getSelectionIndex(baudeRateCombo, "" + deviceConfig.getBaudeRate());
				dataBitsIndex = getSelectionIndex(dataBitsCombo, "" + deviceConfig.getDataBits());
				stopBitsIndex = deviceConfig.getStopBits() - 1;
				parityIndex = deviceConfig.getParity();
				flowControlIndex = deviceConfig.getFlowCtrlMode();
				isRTS = deviceConfig.isRTS();
				isDTR = deviceConfig.isDTR();
				
				if(deviceConfig.getSerialPortType().getTimeOut() != null) {
					timeOutButton.setSelection(useTimeOut = true);
				}
				else {
					timeOutButton.setSelection(useTimeOut = false);
				}
				RTOCharDelayTime = deviceConfig.getRTOCharDelayTime();
				RTOExtraDelayTime = deviceConfig.getRTOExtraDelayTime();
				WTOCharDelayTime = deviceConfig.getWTOCharDelayTime();
				WTOExtraDelayTime = deviceConfig.getWTOExtraDelayTime();
				timeOutComposite.redraw();
		
				serialPortComposite.redraw();
			}
		}
		
		timeStep_ms = deviceConfig.getTimeStep_ms();
		timeBaseComposite.redraw();
		
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
		
		
		tablePropertyComposite.update(deviceConfig.getDesktopProperty(DesktopType.TYPE_TABLE_TAB), false, false, true);
		tablePropertyComposite.setParents(deviceConfig, null, deviceConfig.getDesktopType());
		tablePropertyComposite2.update(deviceConfig.getDesktopProperty(DesktopType.TYPE_DIGITAL_TAB), false, false, true);
		tablePropertyComposite2.setParents(deviceConfig, null, deviceConfig.getDesktopType());
		tablePropertyComposite3.update(deviceConfig.getDesktopProperty(DesktopType.TYPE_ANALOG_TAB), false, false, true);
		tablePropertyComposite3.setParents(deviceConfig, null, deviceConfig.getDesktopType());
		tablePropertyComposite4.update(deviceConfig.getDesktopProperty(DesktopType.TYPE_VOLTAGE_PER_CELL_TAB), false, false, true);
		tablePropertyComposite4.setParents(deviceConfig, null, deviceConfig.getDesktopType());
		desktopComposite.redraw();
		
		int modeStateCount = (deviceConfig.getModeStateType() == null) ? 0 : deviceConfig.getModeStateSize();
		if (deviceConfig.getModeStateType() == null || (modeStateCount == 0 && (deviceConfig.getModeStateType() != null && !modeStateTabFolder.isDisposed()))) {
			if (modeStateTabFolder != null && !modeStateTabFolder.isDisposed()) {
				for (Control child : modeStateTabFolder.getChildren()) {
					child.dispose();
				}
				modeStateTabFolder.getParent().dispose();
			}
			if (modeStateTabItem != null) modeStateTabItem.dispose();
		}
		else {
			if (deviceConfig.getModeStateType() != null && modeStateTabItem.isDisposed()) {
				createInitialModeStateTabItem();
			}
			if (deviceConfig.getModeStateType() != null && !modeStateTabItem.isDisposed()) {
				if (modeStateCount > modeStateTabFolder.getItemCount()) {
					for (int i = modeStateTabFolder.getItemCount(); i < deviceConfig.getModeStateSize(); i++) {
						modeStateInnerTabItem = new CTabItem(modeStateTabFolder, SWT.NONE);
						modeStateInnerTabItem.setText("ModeState " + modeStateTabFolder.getItemCount());
						modeStateInnerTabItem.setShowClose(true);
						modeStateInnerTabItem.setData(MODE_STATE_KEY, modeStateTabFolder.getItemCount());
						{
							modeStateItemComposite = new PropertyTypeComposite(modeStateTabFolder, SWT.NONE);
							modeStateInnerTabItem.setControl(modeStateItemComposite);
						}
					}
				}
				else if (modeStateCount < modeStateTabFolder.getItemCount()) {
					Control[] childs = modeStateTabFolder.getChildren();
					for (int i = modeStateCount - 1; i < childs.length; i++) {
						((PropertyTypeComposite) childs[i]).dispose();
					}
				}
				int index = 1;
				for (Control child : modeStateTabFolder.getChildren()) {
					((PropertyTypeComposite) child).update(deviceConfig.getModeStateProperty(index++), true, false, false);
					((PropertyTypeComposite) child).setParents(deviceConfig, deviceConfig.getModeStateType(), null);
				}
				modeStateComposite.redraw();
			}
		}
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
		if (!enable) {
			deviceConfig.setDataBlockCheckSumFormat(dataBlockcheckSumFormat = null);;
			deviceConfig.setDataBlockCheckSumType(dataBlockCheckSumType = null);
			deviceConfig.setDataBlockEnding(StringHelper.convert2ByteArray(dataBlockEnding = "0"));
		}
	}
}
