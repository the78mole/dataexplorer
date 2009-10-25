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
import osde.device.DataTypes;
import osde.device.DesktopType;
import osde.device.DeviceConfiguration;
import osde.device.ObjectFactory;
import osde.device.PropertyType;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.SWTResourceManager;
import osde.utils.FileUtils;


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
	Label	_RTOCharDelayTimeLabel, _RTOExtraDelayTimeLabel, _WRTOCharDelayTimeLabel, _WTOExtraDelayTimeLabel;
	Text	_RTOCharDelayTimeText, _RTOExtraDelayTimeText, _WRTOCharDelayTimeText, _WTOExtraDelayTimeText;

	String portName = OSDE.STRING_EMPTY;
	int baudeRateIndex = 0;
	int dataBitsIndex = 0;
	int stopBitsIndex = 0;
	int parityIndex = 0;
	int flowControlIndex = 0;
	boolean isRTS = false;
	boolean isDTR = false;
	boolean useTimeOut = false;
	
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
	CCombo dataBlockFormatCombo, dataBlockceckSumFormatCombo;
	Text dataBlockSizeText, dataBlockCheckSumText, dataBlockEndingText;
	Group dataBlockRequiredGroup, dataBlockOptionalGroup;
	
	CTabItem modeStateTabItem, modeStateInnerTabItem;
	Composite modeStateComposite;
	Label modeStateDescriptionLabel;
	Button addButton;
	CTabFolder modeStateTabFolder;
	PropertyTypeComposite modeStateItemComposite;
	
	CTabItem channelConfigurationTabItem;
	Composite channleConfigComposite;
	Label channelConfigDescriptionLabel;
	
	CTabItem destopTabItem, desktopInnerTabItem1, desktopInnerTabItem2, desktopInnerTabItem3, desktopInnerTabItem4;
	Composite desktopComposite;
	Label destktopDescriptionLabel;	
	CTabFolder desktopTabFolder;
	PropertyTypeComposite tablePropertyComposite1, tablePropertyComposite2, tablePropertyComposite3, tablePropertyComposite4;
	
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
			dialogShell.setSize(640, 420);
			dialogShell.setText("Device Properties Editor");
			dialogShell.setImage(SWTResourceManager.getImage("osde/resource/EditHot.gif"));
			dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					log.log(Level.FINEST, "dialogShell.widgetDisposed, event="+evt);
					if (deviceConfig != null)
						deviceConfig.storeDeviceProperties();
					//TODO add your code for dialogShell.widgetDisposed
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
				closeButton.setBounds(336, 349, 250, 30);
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
				saveButton.setBounds(48, 349, 250, 30);
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
							deviceDescriptionlabel = new Label(deviceComposite, SWT.CENTER);
							deviceDescriptionlabel.setText("This section describes the main parameter of any device.");
							deviceDescriptionlabel.setBounds(12, 8, 604, 38);
						}
						{
							deviceLabelComposite = new Composite(deviceComposite, SWT.NONE);
							deviceLabelComposite.setLayout(null);
							deviceLabelComposite.setBounds(24, 52, 137, 190);
							{
								deviceNameLabel = new Label(deviceLabelComposite, SWT.NONE);
								deviceNameLabel.setText("Name");
								deviceNameLabel.setForeground(osde.ui.SWTResourceManager.getColor(SWT.COLOR_BLACK));
								deviceNameLabel.setBounds(0, 0, 137, 16);
							}
							{
								manufacturerLabel = new Label(deviceLabelComposite, SWT.NONE);
								manufacturerLabel.setText("Manufacturer");
								manufacturerLabel.setBounds(0, 33, 137, 19);
							}
							{
								manufURLabel = new Label(deviceLabelComposite, SWT.NONE);
								manufURLabel.setText("Manufactorer URL");
								manufURLabel.setBounds(0, 64, 137, 19);
							}
							{
								imageFileNameLabel = new Label(deviceLabelComposite, SWT.NONE);
								imageFileNameLabel.setText("Image Filename");
								imageFileNameLabel.setBounds(0, 95, 137, 19);
							}
							{
								usageLabel = new Label(deviceLabelComposite, SWT.NONE);
								usageLabel.setText("usage");
								usageLabel.setBounds(0, 126, 137, 19);
							}
							{
								groupLabel = new Label(deviceLabelComposite, SWT.NONE);
								groupLabel.setText("Group ID");
								groupLabel.setBounds(0, 157, 137, 16);
							}
						}
						{
							devicePropsComposite = new Composite(deviceComposite, SWT.NONE);
							devicePropsComposite.setLayout(null);
							devicePropsComposite.setBounds(173, 52, 482, 177);
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
					serialPortTabItem = new CTabItem(tabFolder, SWT.CLOSE);
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
							serialPortDescriptionLabel = new Label(serialPortComposite, SWT.CENTER);
							serialPortDescriptionLabel.setText("This optional field descibes the serial port configuration.\nIt can be removed.");
							serialPortDescriptionLabel.setBounds(5, 5, 605, 50);
						}
						{
							portNameLabel = new Label(serialPortComposite, SWT.RIGHT);
							portNameLabel.setText("port name");
							portNameLabel.setBounds(5, 55, 100, 20);
						}
						{
							portNameText = new Text(serialPortComposite, SWT.BORDER);
							portNameText.setBounds(140, 55, 180, 20);
						}
						{
							baudeRateLabel = new Label(serialPortComposite, SWT.RIGHT);
							baudeRateLabel.setText("baude rate");
							baudeRateLabel.setBounds(5, 80, 100, 20);
						}
						{
							baudeRateCombo = new CCombo(serialPortComposite, SWT.BORDER);
							baudeRateCombo.setItems(new String[] { "2400", "4800", "7200", "9600", "14400", "28800", "38400", "57600", "115200" });
							baudeRateCombo.setBounds(140, 80, 180, 20);
						}
						{
							dataBitsLabel = new Label(serialPortComposite, SWT.RIGHT);
							dataBitsLabel.setText("data bits");
							dataBitsLabel.setBounds(5, 105, 100, 20);
						}
						{
							dataBitsCombo = new CCombo(serialPortComposite, SWT.BORDER);
							dataBitsCombo.setItems(new String[] {"5", "6", "7", "8"});
							dataBitsCombo.setBounds(140, 105, 180, 20);
						}
						{
							stopBitsLabel = new Label(serialPortComposite, SWT.RIGHT);
							stopBitsLabel.setText("stop bits");
							stopBitsLabel.setBounds(5, 130, 100, 20);
						}
						{
							stopBitsCombo = new CCombo(serialPortComposite, SWT.BORDER);
							stopBitsCombo.setItems(new String[] { "STOPBITS_1", "STOPBITS_2", "STOPBITS_1_5" });
							stopBitsCombo.setBounds(140, 130, 180, 20);
						}
						{
							parityLabel = new Label(serialPortComposite, SWT.RIGHT);
							parityLabel.setText("parity");
							parityLabel.setBounds(5, 155, 100, 20);
						}
						{
							parityCombo = new CCombo(serialPortComposite, SWT.BORDER);
							parityCombo.setItems(new String[] { "PARITY_NONE", "PARITY_ODD", "PARITY_EVEN", "PARITY_MARK", "PARITY_SPACE" });
							parityCombo.setBounds(140, 155, 180, 20);
						}
						{
							flowControlLabel = new Label(serialPortComposite, SWT.RIGHT);
							flowControlLabel.setText("flow control");
							flowControlLabel.setBounds(5, 180, 100, 20);
						}
						{
							flowControlCombo = new CCombo(serialPortComposite, SWT.BORDER);
							flowControlCombo.setItems(new String[] { "FLOWCONTROL_NONE", "FLOWCONTROL_RTSCTS_IN", "FLOWCONTROL_RTSCTS_OUT", "FLOWCONTROL_XONXOFF_IN", "FLOWCONTROL_XONXOFF_OUT" });
							flowControlCombo.setBounds(140, 180, 180, 20);
						}
						{
							rtsLabel = new Label(serialPortComposite, SWT.RIGHT);
							rtsLabel.setText(" RTS");
							rtsLabel.setBounds(5, 205, 100, 20);
						}
						{
							isRTSButton = new Button(serialPortComposite, SWT.CHECK);
							isRTSButton.setBounds(140, 205, 180, 20);
						}
						{
							dtrLabel = new Label(serialPortComposite, SWT.RIGHT);
							dtrLabel.setText(" DTR");
							dtrLabel.setBounds(5, 230, 100, 20);
						}
						{
							isDTRButton = new Button(serialPortComposite, SWT.CHECK);
							isDTRButton.setBounds(140, 230, 180, 20);
						}
						{
							timeOutComposite = new Composite(serialPortComposite, SWT.BORDER);
							timeOutComposite.setLayout(null);
							timeOutComposite.setBounds(354, 57, 250, 207);
							timeOutComposite.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									log.log(Level.FINEST, "dialogShell.paintControl, event="+evt);
									timeOutButton.setSelection(useTimeOut);
									if (timeOutButton.getSelection()) {
										_RTOCharDelayTimeLabel.setEnabled(true);
										_RTOCharDelayTimeText.setEnabled(true);
										_RTOExtraDelayTimeLabel.setEnabled(true);
										_RTOExtraDelayTimeText.setEnabled(true);
										_WRTOCharDelayTimeLabel.setEnabled(true);
										_WRTOCharDelayTimeText.setEnabled(true);
										_WTOExtraDelayTimeLabel.setEnabled(true);
										_WTOExtraDelayTimeText.setEnabled(true);
									}
									else {
										_RTOCharDelayTimeLabel.setEnabled(false);
										_RTOCharDelayTimeText.setEnabled(false);
										_RTOExtraDelayTimeLabel.setEnabled(false);
										_RTOExtraDelayTimeText.setEnabled(false);
										_WRTOCharDelayTimeLabel.setEnabled(false);
										_WRTOCharDelayTimeText.setEnabled(false);
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
							}
							{
								_RTOExtraDelayTimeLabel = new Label(timeOutComposite, SWT.RIGHT);
								_RTOExtraDelayTimeLabel.setText("RTOExtraDelayTime");
								_RTOExtraDelayTimeLabel.setBounds(6, 118, 140, 20);
							}
							{
								_RTOExtraDelayTimeText = new Text(timeOutComposite, SWT.BORDER);
								_RTOExtraDelayTimeText.setBounds(162, 116, 70, 20);
							}
							{
								_WRTOCharDelayTimeLabel = new Label(timeOutComposite, SWT.RIGHT);
								_WRTOCharDelayTimeLabel.setText("WTOCharDelayTime");
								_WRTOCharDelayTimeLabel.setBounds(6, 148, 140, 20);
							}
							{
								_WRTOCharDelayTimeText = new Text(timeOutComposite, SWT.BORDER);
								_WRTOCharDelayTimeText.setBounds(162, 146, 70, 20);
							}
							{
								_WTOExtraDelayTimeLabel = new Label(timeOutComposite, SWT.RIGHT);
								_WTOExtraDelayTimeLabel.setText("WTOExtraDelayTime");
								_WTOExtraDelayTimeLabel.setBounds(6, 178, 140, 20);
							}
							{
								_WTOExtraDelayTimeText = new Text(timeOutComposite, SWT.BORDER);
								_WTOExtraDelayTimeText.setBounds(162, 176, 70, 20);
							}
							{
								timeOutDescriptionLabel = new Label(timeOutComposite, SWT.WRAP);
								timeOutDescriptionLabel.setText("Time out section describes Read and Write delay time. This delay and extra delay are only required in special purpose. ");
								timeOutDescriptionLabel.setBounds(6, 3, 232, 52);
							}
						}
					}
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
								System.out.println("timeBaseComposite.paintControl, event="+evt);
								timeBaseTimeStepText.setText(String.format("%.1f", timeStep_ms));
							}
						});
						{
							timeBaseDescriptionLabel = new Label(timeBaseComposite, SWT.LEFT | SWT.WRAP);
							timeBaseDescriptionLabel.setText("Defines the time base used to display the data. A timeStep of -1 means time comes from the device data within data block and might not constant. This is a must field definition. Only the time value given as floating point value needs to be edit.");
							timeBaseDescriptionLabel.setBounds(40, 5, 543, 54);
						}
						{
							timeBaseNameLabel = new Label(timeBaseComposite, SWT.RIGHT);
							timeBaseNameLabel.setText("Name");
							timeBaseNameLabel.setBounds(142, 95, 150, 20);
						}
						{
							timeBaseNameText = new Text(timeBaseComposite, SWT.CENTER | SWT.BORDER);
							timeBaseNameText.setText("Time");
							timeBaseNameText.setBounds(322, 94, 60, 20);
						}
						{
							timeBaseSymbolLabel = new Label(timeBaseComposite, SWT.RIGHT);
							timeBaseSymbolLabel.setText("Symbol");
							timeBaseSymbolLabel.setBounds(142, 125, 150, 20);
						}
						{
							timeBaseSymbolText = new Text(timeBaseComposite, SWT.CENTER | SWT.BORDER);
							timeBaseSymbolText.setText("U");
							timeBaseSymbolText.setBounds(322, 124, 60, 20);
						}
						{
							timeBaseUnitLabel = new Label(timeBaseComposite, SWT.RIGHT);
							timeBaseUnitLabel.setText("Unit");
							timeBaseUnitLabel.setBounds(142, 155, 150, 20);
						}
						{
							timeBaseUnitText = new Text(timeBaseComposite, SWT.CENTER | SWT.BORDER);
							timeBaseUnitText.setText("V");
							timeBaseUnitText.setBounds(322, 154, 60, 20);
						}
						{
							timeBaseTimeStepLabel = new Label(timeBaseComposite, SWT.RIGHT);
							timeBaseTimeStepLabel.setText("msec");
							timeBaseTimeStepLabel.setBounds(142, 185, 150, 20);
						}
						{
							timeBaseTimeStepText = new Text(timeBaseComposite, SWT.RIGHT | SWT.BORDER);
							timeBaseTimeStepText.setText("1000.0");
							timeBaseTimeStepText.setBounds(322, 184, 60, 20);
							timeBaseTimeStepText.addKeyListener(new KeyAdapter() {
								public void keyReleased(KeyEvent evt) {
									System.out.println("timeBaseTimeStepText.keyReleased, event="+evt);
									try { 
										String tmpTimeStep = timeBaseTimeStepText.getText();
										timeStep_ms = Double.parseDouble(tmpTimeStep);
									}
									catch (NumberFormatException e) {
										// ignore
									}
								}
							});
						}
					}
				}
				{
					dataBlockTabItem = new CTabItem(tabFolder, SWT.CLOSE);
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
							{
								dataBlockFormatLabel = new Label(dataBlockRequiredGroup, SWT.RIGHT);
								dataBlockFormatLabel.setText("format");
								dataBlockFormatLabel.setBounds(19, 51, 85, 16);
							}
							{
								dataBlockFormatCombo = new CCombo(dataBlockRequiredGroup, SWT.BORDER);
								dataBlockFormatCombo.setBounds(125, 49, 84, 21);
								dataBlockFormatCombo.setItems(new java.lang.String[]{"TEXT"," BINARY"});
								dataBlockFormatCombo.setLayout(null);
								dataBlockFormatCombo.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("dataBlockFormatCombo.widgetSelected, event="+evt);
										//TODO add your code for dataBlockFormatCombo.widgetSelected
									}
								});
							}
							{
								dataBlockSizeText = new Text(dataBlockRequiredGroup, SWT.RIGHT | SWT.BORDER);
								dataBlockSizeText.setText("30");
								dataBlockSizeText.setBounds(127, 103, 64, 22);
								dataBlockSizeText.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										System.out.println("dataBlockSizeText.keyReleased, event="+evt);
										//TODO add your code for dataBlockSizeText.keyReleased
									}
								});
								dataBlockSizeText.addVerifyListener(new VerifyListener() {
									public void verifyText(VerifyEvent evt) {
										System.out.println("dataBlockSizeText.verifyText, event="+evt);
										//TODO add your code for dataBlockSizeText.verifyText
									}
								});
							}
							{
								dataBlockSizeLabel = new Label(dataBlockRequiredGroup, SWT.RIGHT);
								dataBlockSizeLabel.setText("size");
								dataBlockSizeLabel.setBounds(19, 105, 85, 16);
							}
						}
						{
							dataBlockOptionalGroup = new Group(dataBlockComposite, SWT.NONE);
							dataBlockOptionalGroup.setLayout(null);
							dataBlockOptionalGroup.setText("Optional Entries");
							dataBlockOptionalGroup.setBounds(330, 80, 250, 170);
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
								dataBlockceckSumFormatCombo = new CCombo(dataBlockOptionalGroup, SWT.BORDER);
								dataBlockceckSumFormatCombo.setItems(new java.lang.String[]{"TEXT"," BINARY"});
								dataBlockceckSumFormatCombo.setBounds(144, 44, 99, 20);
								dataBlockceckSumFormatCombo.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										System.out.println("dataBlockceckSumFormatCombo.widgetSelected, event="+evt);
										//TODO add your code for dataBlockceckSumFormatCombo.widgetSelected
									}
								});
							}
							{
								dataBlockCheckSumText = new Text(dataBlockOptionalGroup, SWT.RIGHT | SWT.BORDER);
								dataBlockCheckSumText.setText("25");
								dataBlockCheckSumText.setBounds(144, 76, 61, 20);
								dataBlockCheckSumText.addVerifyListener(new VerifyListener() {
									public void verifyText(VerifyEvent evt) {
										System.out.println("dataBlockCheckSumText.verifyText, event="+evt);
										//TODO add your code for dataBlockCheckSumText.verifyText
									}
								});
								dataBlockCheckSumText.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										System.out.println("dataBlockCheckSumText.keyReleased, event="+evt);
										//TODO add your code for dataBlockCheckSumText.keyReleased
									}
								});
							}
							{
								dataBlockEndingText = new Text(dataBlockOptionalGroup, SWT.BORDER);
								dataBlockEndingText.setText("0a0d");
								dataBlockEndingText.setBounds(144, 109, 61, 20);
								dataBlockEndingText.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										System.out.println("dataBlockEndingText.keyReleased, event="+evt);
										//TODO add your code for dataBlockEndingText.keyReleased
									}
								});
								dataBlockEndingText.addVerifyListener(new VerifyListener() {
									public void verifyText(VerifyEvent evt) {
										System.out.println("dataBlockEndingText.verifyText, event="+evt);
										//TODO add your code for dataBlockEndingText.verifyText
									}
								});
							}
						}
					}
				}
//				{
//					channelConfigurationTabItem = new CTabItem(tabFolder, SWT.NONE);
//					channelConfigurationTabItem.setText("Channel/Configuration");
//					{
//						channleConfigComposite = new Composite(tabFolder, SWT.NONE);
//						channleConfigComposite.setLayout(null);
//						channelConfigurationTabItem.setControl(channleConfigComposite);
//						{
//							channelConfigDescriptionLabel = new Label(channleConfigComposite, SWT.CENTER);
//							channelConfigDescriptionLabel.setText("Defines miscelanious visualisation properties of the application desktop.\nProbably there are more than one measurements to be described here.");
//							channelConfigDescriptionLabel.setBounds(5, 5, 616, 49);
//						}
//					}
//				}
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
								tablePropertyComposite1.redraw();
								tablePropertyComposite2.redraw();
								tablePropertyComposite3.redraw();
								tablePropertyComposite4.redraw();
							}
						});
						{
							destktopDescriptionLabel = new Label(desktopComposite, SWT.CENTER);
							destktopDescriptionLabel.setText("Defines miscelanious visualisation properties of the application desktop. \nIf the data table contnet should be updated and displyed, ....");
							destktopDescriptionLabel.setBounds(5, 5, 611, 51);
						}
						{
							desktopTabFolder = new CTabFolder(desktopComposite, SWT.BORDER);
							GridLayout appDesktopTabCompositeLayout = new GridLayout();
							appDesktopTabCompositeLayout.makeColumnsEqualWidth = true;
							desktopTabFolder.setLayout(appDesktopTabCompositeLayout);
							desktopTabFolder.setBounds(165, 65, 300, 160);
							{
								desktopInnerTabItem1 = new CTabItem(desktopTabFolder, SWT.NONE);
								desktopInnerTabItem1.setText("DataTable");
								{
									tablePropertyComposite1 = new PropertyTypeComposite(desktopTabFolder, SWT.NONE);
									desktopInnerTabItem1.setControl(tablePropertyComposite1);
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
							modeStateDescriptionLabel = new Label(modeStateComposite, SWT.CENTER);
							modeStateDescriptionLabel.setText("Defines device processing states, like 1 charge, 2, discharge, ...");
							modeStateDescriptionLabel.setBounds(5, 5, 615, 55);
						}
						{
							modeStateTabFolder = new CTabFolder(modeStateComposite, SWT.BORDER);
							modeStateTabFolder.setBounds(165, 65, 300, 170);
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
									System.out.println("modeStateTabFolder.close, event="+evt);
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
							addButton.setBounds(165, 237, 300, 30);
							addButton.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent evt) {
									System.out.println("addButton.widgetSelected, event="+evt);
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
				tabFolder.setSelection(0);
				tabFolder.setBounds(0, 40, 632, 300);
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
		
		portName = deviceConfig.getPortString();
		baudeRateIndex = getSelectionIndex(baudeRateCombo, ""+deviceConfig.getBaudeRate());
		dataBitsIndex = getSelectionIndex(dataBitsCombo, ""+deviceConfig.getDataBits());
		stopBitsIndex = deviceConfig.getStopBits() - 1;
		parityIndex = deviceConfig.getParity();
		flowControlIndex = deviceConfig.getFlowCtrlMode();
		isRTS = deviceConfig.isRTS();
		isDTR = deviceConfig.isDTR();
		serialPortComposite.redraw();
		
		timeStep_ms = deviceConfig.getTimeStep_ms();
		timeBaseComposite.redraw();
		
		tablePropertyComposite1.update(deviceConfig.getDesktopProperty(DesktopType.TYPE_TABLE_TAB), false, false, true);
		tablePropertyComposite1.setParents(deviceConfig, null, deviceConfig.getDesktopType());
		tablePropertyComposite2.update(deviceConfig.getDesktopProperty(DesktopType.TYPE_DIGITAL_TAB), false, false, true);
		tablePropertyComposite2.setParents(deviceConfig, null, deviceConfig.getDesktopType());
		tablePropertyComposite3.update(deviceConfig.getDesktopProperty(DesktopType.TYPE_ANALOG_TAB), false, false, true);
		tablePropertyComposite3.setParents(deviceConfig, null, deviceConfig.getDesktopType());
		tablePropertyComposite4.update(deviceConfig.getDesktopProperty(DesktopType.TYPE_VOLTAGE_PER_CELL_TAB), false, false, true);
		tablePropertyComposite4.setParents(deviceConfig, null, deviceConfig.getDesktopType());
		desktopComposite.redraw();
		
		if (deviceConfig.getModeStateSize() > modeStateTabFolder.getItemCount()) {
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
		else if (deviceConfig.getModeStateSize() < modeStateTabFolder.getItemCount()) {
			Control[] childs = modeStateTabFolder.getChildren();
			for (int i = deviceConfig.getModeStateSize()-1; i < childs.length; i++) {
				childs[i].dispose();
			}
		}

		int index = 1;
		for (Control child : modeStateTabFolder.getChildren()) {
			((PropertyTypeComposite)child).update(deviceConfig.getModeStateProperty(index++), true, false, false);
			((PropertyTypeComposite)child).setParents(deviceConfig, deviceConfig.getModeStateType(), null);
		}
		modeStateComposite.redraw();
	}
}
