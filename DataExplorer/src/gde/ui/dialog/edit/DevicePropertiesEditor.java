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

import javax.xml.bind.JAXBException;

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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.xml.sax.SAXParseException;

import osde.OSDE;
import osde.config.Settings;
import osde.device.ChecksumTypes;
import osde.device.DataTypes;
import osde.device.DesktopPropertyTypes;
import osde.device.DeviceConfiguration;
import osde.device.DeviceTypes;
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
public class DevicePropertiesEditor extends Composite {
	final static Logger					log												= Logger.getLogger(DevicePropertiesEditor.class.getName());
	public final static int			widgetFontSize						= OSDE.IS_LINUX ? 8 : 9;
	public final static String	widgetFontName						= OSDE.IS_WINDOWS ? "Microsoft Sans Serif" : "Sans Serif"; //$NON-NLS-1$ //$NON-NLS-2$
	
	private static DevicePropertiesEditor devicePropsEditor = null;
	private static Shell shell;

	CTabFolder									tabFolder;
	Label												devicePropFileNamelabel;
	Text												deviceFileNameText;
	Button											fileSelectionButton, saveButton, closeButton;

	CTabItem										deviceTabItem;
	Menu												popupMenu;
	ContextMenu									deviceContextMenu;
	Composite										deviceComposite;
	Label												deviceDescriptionlabel;
	Label												deviceNameLabel, manufacturerLabel, manufURLabel, imageFileNameLabel, usageLabel, groupLabel;
	Text												nameText, manufacturerText, manufURLText, imageFileNameText;
	Button											usageButton, fileSelectButton;
	CCombo											groupSelectionCombo;
	Composite										deviceLabelComposite, devicePropsComposite;

	String											devicePropertiesFileName	= OSDE.STRING_EMPTY;
	String											deviceName								= OSDE.STRING_EMPTY;
	String											manufacuturerURL					= OSDE.STRING_EMPTY;
	String											imageFileName							= OSDE.STRING_EMPTY;
	String											manufacturer							= OSDE.STRING_EMPTY;
	boolean											isDeviceUsed							= false;
	DeviceTypes									deviceGroup								= DeviceTypes.LOGGER;

	SeriaPortTypeTabItem				serialPortTabItem;

	CTabItem										timeBaseTabItem;
	Composite										timeBaseComposite;
	Label												timeBaseDescriptionLabel;
	Label												timeBaseNameLabel, timeBaseSymbolLabel, timeBaseUnitLabel, timeBaseTimeStepLabel;
	Text												timeBaseNameText, timeBaseSymbolText, timeBaseUnitText, timeBaseTimeStepText;

	double											timeStep_ms								= 0.0;

	CTabItem										dataBlockTabItem;
	Composite										dataBlockComposite;
	Label												dataBlockDescriptionLabel;
	Label												dataBlockFormatLabel, dataBlockSizeLabel, dataBlockCheckSumFormatLabel, dataBlockCheckSumLabel, dataBlockEndingLabel;
	CCombo											dataBlockFormatCombo, dataBlockcheckSumFormatCombo, dataBlockCheckSumTypeCombo;
	Text												dataBlockSizeText, dataBlockEndingText;
	Group												dataBlockRequiredGroup, dataBlockOptionalGroup;
	Button											dataBlockOptionalEnableButton;

	FormatTypes									dataBlockFormat						= FormatTypes.BINARY, dataBlockcheckSumFormat = FormatTypes.BINARY;
	int													dataBlockSize							= 30;
	ChecksumTypes								dataBlockCheckSumType			= ChecksumTypes.ADD;
	String											dataBlockEnding						= "0a0d";
	boolean											isDataBlockOptionalGroupEnabled = false;


	CTabItem										modeStateTabItem;
	Composite										modeStateComposite;
	Label												modeStateDescriptionLabel;
	Button											addButton;
	CTabFolder									stateTabFolder;
	PropertyTypeTabItem					modeStateItemComposite;

	CTabItem										channelConfigurationTabItem;
	Composite										channelConfigComposite;
	Label												channelConfigDescriptionLabel;
	CTabFolder									channelConfigInnerTabFolder;

	CTabItem										destopTabItem;
	PropertyTypeTabItem					desktopInnerTabItem1, desktopInnerTabItem2, desktopInnerTabItem3, desktopInnerTabItem4;
	Composite										desktopComposite;
	Label												desktopDescriptionLabel;
	CTabFolder									desktopTabFolder;

	//cross over fields
	DeviceConfiguration					deviceConfig;
	final Settings							settings;

	/**
	* Auto-generated main method to display this 
	* org.eclipse.swt.widgets.Dialog inside a new Shell.
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Rectangle displayBounds = display.getBounds();
			shell = new Shell(display, SWT.DIALOG_TRIM | SWT.MIN | SWT.APPLICATION_MODAL);
			DevicePropertiesEditor devicePropsEditor = DevicePropertiesEditor.getInstance();
			devicePropsEditor.open();
			Point size = devicePropsEditor.getSize();
			shell.setLayout(new FillLayout());
			shell.setText(Messages.getString(MessageIds.OSDE_MSGT0465));
			shell.setImage(SWTResourceManager.getImage("osde/resource/EditHot.gif")); //$NON-NLS-1$
			shell.setLocation(displayBounds.x < 0 ? -50-size.x : displayBounds.width+displayBounds.x-size.x-50, displayBounds.height-size.y-150);
			shell.layout();
			Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
			shell.setSize(shellBounds.width, shellBounds.height);
			shell.setMinimumSize(shellBounds.width, shellBounds.height);
			shell.open();
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static DevicePropertiesEditor getInstance() {
		if (devicePropsEditor == null) {
			devicePropsEditor = new DevicePropertiesEditor(shell, SWT.NONE);
		}
		return devicePropsEditor;
	}

	private DevicePropertiesEditor(Shell parent, int style) {
		super(parent, style);
		this.settings = Settings.getInstance();
	}

	public void open() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setLayout(null);
			this.setSize(640, 460);
			this.getShell().addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					DevicePropertiesEditor.log.log(Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
					if (DevicePropertiesEditor.this.deviceConfig != null && DevicePropertiesEditor.this.deviceConfig.isChangePropery()) {
						String msg = Messages.getString(MessageIds.OSDE_MSGT0469, new String[] {DevicePropertiesEditor.this.devicePropertiesFileName});
						if (OpenSerialDataExplorer.getInstance().openYesNoMessageDialog(DevicePropertiesEditor.this.getShell(), msg) == SWT.YES) {
							DevicePropertiesEditor.this.deviceConfig.storeDeviceProperties();
						}
					}
				}
			});
			{
				this.fileSelectionButton = new Button(this, SWT.PUSH | SWT.CENTER);
				this.fileSelectionButton.setText(" ... "); //$NON-NLS-1$
				this.fileSelectionButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
				this.fileSelectionButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0484));
				this.fileSelectionButton.setBounds(580, 10, 30, 20);
				this.fileSelectionButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "fileSelectionButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						if (DevicePropertiesEditor.this.deviceConfig != null && DevicePropertiesEditor.this.deviceConfig.isChangePropery()) {
							String msg = Messages.getString(MessageIds.OSDE_MSGT0469, new String[] {DevicePropertiesEditor.this.devicePropertiesFileName});
							if (OpenSerialDataExplorer.getInstance().openYesNoMessageDialog(DevicePropertiesEditor.this.getShell(), msg) == SWT.YES) {
								DevicePropertiesEditor.this.deviceConfig.storeDeviceProperties();
							}
						}
						FileDialog fileSelectionDialog = new FileDialog(DevicePropertiesEditor.this.getShell());
						fileSelectionDialog.setFilterPath(getDevicesPath());
						fileSelectionDialog.setText(Messages.getString(MessageIds.OSDE_MSGT0479));
						fileSelectionDialog.setFilterExtensions(new String[] { OSDE.FILE_ENDING_STAR_XML });
						fileSelectionDialog.setFilterNames(new String[] { Messages.getString(MessageIds.OSDE_MSGT0480) }); 
						fileSelectionDialog.open();
						String tmpFileName = fileSelectionDialog.getFileName();
						DevicePropertiesEditor.log.log(Level.INFO, "devicePropertiesFileName = " + tmpFileName); //$NON-NLS-1$

						try {
							DevicePropertiesEditor.this.deviceConfig = new DeviceConfiguration(getDevicesPath() + OSDE.FILE_SEPARATOR_UNIX + tmpFileName);

							DevicePropertiesEditor.this.devicePropertiesFileName = tmpFileName;
							DevicePropertiesEditor.this.deviceFileNameText.setText(DevicePropertiesEditor.this.devicePropertiesFileName);
							update();
						}
						catch (JAXBException e) {
							log.log(Level.WARNING, e.getMessage(), e);
							if (e.getLinkedException() instanceof SAXParseException) {
								SAXParseException spe = (SAXParseException) e.getLinkedException();
								openWarningMessageBox(Messages.getString(MessageIds.OSDE_MSGW0039, new String[] { spe.getSystemId().replace(OSDE.STRING_URL_BLANK, OSDE.STRING_BLANK), spe.getLocalizedMessage() }));
							}
						}
						catch (Exception e) {
							log.log(Level.WARNING, e.getMessage(), e);
						}
					}
				});
			}
			{
				this.deviceFileNameText = new Text(this, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
				this.deviceFileNameText.setBounds(120, 9, 450, 22);
				this.deviceFileNameText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
				this.deviceFileNameText.addKeyListener(new KeyAdapter() {
					public void keyReleased(KeyEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "deviceFileNameText.keyReleased, event=" + evt); //$NON-NLS-1$
						if (evt.keyCode == SWT.CR) {
							try {
								DevicePropertiesEditor.this.devicePropertiesFileName = DevicePropertiesEditor.this.deviceFileNameText.getText().trim();
								if (!DevicePropertiesEditor.this.devicePropertiesFileName.endsWith(OSDE.FILE_ENDING_DOT_XML)) {
									if (DevicePropertiesEditor.this.devicePropertiesFileName.lastIndexOf(".") != -1) {
										DevicePropertiesEditor.this.devicePropertiesFileName = DevicePropertiesEditor.this.devicePropertiesFileName.substring(0, DevicePropertiesEditor.this.devicePropertiesFileName.lastIndexOf(OSDE.STRING_DOT));
									}
									DevicePropertiesEditor.this.devicePropertiesFileName = DevicePropertiesEditor.this.devicePropertiesFileName + OSDE.FILE_ENDING_DOT_XML;
								}
								DevicePropertiesEditor.log.log(Level.INFO, "devicePropertiesFileName = " + DevicePropertiesEditor.this.devicePropertiesFileName); //$NON-NLS-1$

								if (!(new File(getDevicesPath() + OSDE.FILE_SEPARATOR_UNIX + DevicePropertiesEditor.this.devicePropertiesFileName)).exists()) {
									MessageBox okCancelMessageDialog = new MessageBox(DevicePropertiesEditor.this.getShell(), SWT.PRIMARY_MODAL | SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION);
									okCancelMessageDialog.setText(Messages.getString(MessageIds.OSDE_MSGT0465));
									okCancelMessageDialog.setMessage(Messages.getString(MessageIds.OSDE_MSGE0003) + DevicePropertiesEditor.this.devicePropertiesFileName + Messages.getString(MessageIds.OSDE_MSGT0481));
									if (SWT.OK == okCancelMessageDialog.open()) {
										if (FileUtils.extract(this.getClass(), "DeviceSample_" + DevicePropertiesEditor.this.settings.getLocale() + Settings.DEVICE_PROPERTIES_XSD_VERSION + OSDE.FILE_ENDING_DOT_XML, DevicePropertiesEditor.this.devicePropertiesFileName,//$NON-NLS-1$
												"resource/", getDevicesPath(), "555")) { //$NON-NLS-1$ //$NON-NLS-2$
											DevicePropertiesEditor.this.deviceConfig = new DeviceConfiguration(getDevicesPath() + OSDE.FILE_SEPARATOR_UNIX + DevicePropertiesEditor.this.devicePropertiesFileName);
										}
									}
								}
								else {
									DevicePropertiesEditor.this.deviceConfig = new DeviceConfiguration(getDevicesPath() + OSDE.FILE_SEPARATOR_UNIX + DevicePropertiesEditor.this.devicePropertiesFileName);
								}
								update();
							}
							catch (Throwable e) {
								DevicePropertiesEditor.log.log(Level.SEVERE, e.getMessage(), e);
							}
						}
					}
				});
			}
			{
				this.devicePropFileNamelabel = new Label(this, SWT.RIGHT);
				this.devicePropFileNamelabel.setText(Messages.getString(MessageIds.OSDE_MSGT0483));
				this.devicePropFileNamelabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
				this.devicePropFileNamelabel.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0484));
				this.devicePropFileNamelabel.setBounds(0, 12, 100, 16);
			}
			{
				this.closeButton = new Button(this, SWT.PUSH | SWT.CENTER);
				this.closeButton.setText(Messages.getString(MessageIds.OSDE_MSGT0485));
				this.closeButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
				this.closeButton.setBounds(338, 418, 250, 30);
				this.closeButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.getShell().dispose();
					}
				});
			}
			{
				this.saveButton = new Button(this, SWT.PUSH | SWT.CENTER);
				this.saveButton.setText(Messages.getString(MessageIds.OSDE_MSGT0486));
				this.saveButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
				this.saveButton.setBounds(50, 418, 250, 30);
				this.saveButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.deviceConfig.storeDeviceProperties();
					}
				});
			}
			{
				this.tabFolder = new CTabFolder(this, SWT.BORDER);
				this.tabFolder.setSimple(false);
				this.tabFolder.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
				this.popupMenu = new Menu(this.getShell(), SWT.POP_UP);
				(this.deviceContextMenu = new ContextMenu(this.popupMenu, this.tabFolder)).create();
				this.tabFolder.setMenu(this.popupMenu);
				{
					this.deviceTabItem = new CTabItem(this.tabFolder, SWT.NONE);
					this.deviceTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0487));
					this.deviceTabItem.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
					{
						this.deviceComposite = new Composite(this.tabFolder, SWT.NONE);
						this.deviceComposite.setLayout(null);
						this.deviceTabItem.setControl(this.deviceComposite);
						this.deviceComposite.setMenu(this.popupMenu);
						{
							this.deviceDescriptionlabel = new Label(this.deviceComposite, SWT.CENTER | SWT.WRAP);
							this.deviceDescriptionlabel.setText(Messages.getString(MessageIds.OSDE_MSGT0488));
							this.deviceDescriptionlabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
							this.deviceDescriptionlabel.setBounds(12, 8, 604, 50);
							this.deviceDescriptionlabel.setMenu(this.popupMenu);
						}
						{
							this.deviceLabelComposite = new Composite(this.deviceComposite, SWT.NONE);
							this.deviceLabelComposite.setLayout(null);
							this.deviceLabelComposite.setBounds(20, 70, 145, 190);
							this.deviceLabelComposite.setMenu(this.popupMenu);
							{
								this.deviceNameLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.deviceNameLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0489));
								this.deviceNameLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
								this.deviceNameLabel.setForeground(osde.ui.SWTResourceManager.getColor(SWT.COLOR_BLACK));
								this.deviceNameLabel.setBounds(0, 0, 137, 16);
								this.deviceNameLabel.setMenu(this.popupMenu);
							}
							{
								this.manufacturerLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.manufacturerLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0490));
								this.manufacturerLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
								this.manufacturerLabel.setBounds(0, 33, 137, 19);
								this.manufacturerLabel.setMenu(this.popupMenu);
							}
							{
								this.manufURLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.manufURLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0491));
								this.manufURLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
								this.manufURLabel.setBounds(0, 64, 137, 19);
								this.manufURLabel.setMenu(this.popupMenu);
							}
							{
								this.imageFileNameLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.imageFileNameLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0492));
								this.imageFileNameLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
								this.imageFileNameLabel.setBounds(0, 95, 137, 19);
								this.imageFileNameLabel.setMenu(this.popupMenu);
							}
							{
								this.usageLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.usageLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0493));
								this.usageLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
								this.usageLabel.setBounds(0, 126, 137, 19);
								this.usageLabel.setMenu(this.popupMenu);
							}
							{
								this.groupLabel = new Label(this.deviceLabelComposite, SWT.RIGHT);
								this.groupLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0494));
								this.groupLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
								this.groupLabel.setBounds(0, 157, 137, 16);
								this.groupLabel.setMenu(this.popupMenu);
							}
						}
						{
							this.devicePropsComposite = new Composite(this.deviceComposite, SWT.NONE);
							this.devicePropsComposite.setLayout(null);
							this.devicePropsComposite.setBounds(170, 70, 450, 190);
							this.devicePropsComposite.setMenu(this.popupMenu);
							this.devicePropsComposite.addPaintListener(new PaintListener() {
								public void paintControl(PaintEvent evt) {
									DevicePropertiesEditor.log.log(Level.FINEST, "devicePropsComposite.paintControl, event=" + evt); //$NON-NLS-1$
									DevicePropertiesEditor.this.deviceFileNameText.setText(DevicePropertiesEditor.this.devicePropertiesFileName);
									DevicePropertiesEditor.this.nameText.setText(DevicePropertiesEditor.this.deviceName);
									DevicePropertiesEditor.this.manufURLText.setText(DevicePropertiesEditor.this.manufacuturerURL);
									DevicePropertiesEditor.this.imageFileNameText.setText(DevicePropertiesEditor.this.imageFileName);
									DevicePropertiesEditor.this.manufacturerText.setText(DevicePropertiesEditor.this.manufacturer);
									DevicePropertiesEditor.this.usageButton.setSelection(DevicePropertiesEditor.this.isDeviceUsed);
									DevicePropertiesEditor.this.groupSelectionCombo.select(DevicePropertiesEditor.this.deviceGroup.ordinal());
								}
							});
							{
								this.nameText = new Text(this.devicePropsComposite, SWT.BORDER);
								this.nameText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
								this.nameText.setBounds(0, 0, 409, 22);
								this.nameText.setMenu(this.popupMenu);
								this.nameText.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										DevicePropertiesEditor.log.log(Level.FINEST, "nameText.keyReleased, event=" + evt); //$NON-NLS-1$
										DevicePropertiesEditor.this.deviceConfig.setName(DevicePropertiesEditor.this.deviceName = DevicePropertiesEditor.this.nameText.getText());
									}
								});
							}
							{
								this.manufacturerText = new Text(this.devicePropsComposite, SWT.BORDER);
								this.manufacturerText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
								this.manufacturerText.setBounds(0, 32, 409, 22);
								this.manufacturerText.setMenu(this.popupMenu);
								this.manufacturerText.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										DevicePropertiesEditor.log.log(Level.FINEST, "manufacturerText.keyReleased, event=" + evt); //$NON-NLS-1$
										DevicePropertiesEditor.this.deviceConfig.setManufacturer(DevicePropertiesEditor.this.manufacturer = DevicePropertiesEditor.this.manufacturerText.getText());
									}
								});
							}
							{
								this.manufURLText = new Text(this.devicePropsComposite, SWT.BORDER);
								this.manufURLText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
								this.manufURLText.setBounds(0, 64, 409, 22);
								this.manufURLText.setMenu(this.popupMenu);
								this.manufURLText.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										DevicePropertiesEditor.log.log(Level.FINEST, "manufURLText.keyReleased, event=" + evt); //$NON-NLS-1$
										DevicePropertiesEditor.this.deviceConfig.setManufacturerURL(DevicePropertiesEditor.this.manufacuturerURL = DevicePropertiesEditor.this.manufURLText.getText());
									}
								});
							}
							{
								this.imageFileNameText = new Text(this.devicePropsComposite, SWT.BORDER);
								this.imageFileNameText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
								this.imageFileNameText.setBounds(0, 95, 409, 22);
								this.imageFileNameText.setMenu(this.popupMenu);
								this.imageFileNameText.addKeyListener(new KeyAdapter() {
									public void keyReleased(KeyEvent evt) {
										DevicePropertiesEditor.log.log(Level.FINEST, "imageFileNameText.keyReleased, event=" + evt); //$NON-NLS-1$
										DevicePropertiesEditor.this.deviceConfig.setImageFileName(DevicePropertiesEditor.this.imageFileName = DevicePropertiesEditor.this.imageFileNameText.getText());
									}
								});
							}
							{
								this.usageButton = new Button(this.devicePropsComposite, SWT.CHECK);
								this.usageButton.setBounds(3, 126, 159, 22);
								this.usageButton.setMenu(this.popupMenu);
								this.usageButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										DevicePropertiesEditor.log.log(Level.FINEST, "usageButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (DevicePropertiesEditor.this.deviceConfig != null) {
											DevicePropertiesEditor.this.deviceConfig.setUsed(DevicePropertiesEditor.this.isDeviceUsed = usageButton.getSelection());
										}
									}
								});
							}
							{
								this.groupSelectionCombo = new CCombo(this.devicePropsComposite, SWT.BORDER);
								this.groupSelectionCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
								this.groupSelectionCombo.setItems(StringHelper.enumValues2StringArray(DeviceTypes.values()));
								this.groupSelectionCombo.setBounds(0, 154, 409, 22);
								this.groupSelectionCombo.setMenu(this.popupMenu);
								this.groupSelectionCombo.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										DevicePropertiesEditor.log.log(Level.FINEST, "groupSelectionCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
										if (DevicePropertiesEditor.this.deviceConfig != null) {
											DevicePropertiesEditor.this.deviceConfig.setDeviceGroup(DevicePropertiesEditor.this.deviceGroup = DeviceTypes.valueOf(DevicePropertiesEditor.this.groupSelectionCombo.getText()));
										}
									}
								});
							}
							{
								this.fileSelectButton = new Button(this.devicePropsComposite, SWT.PUSH | SWT.CENTER);
								this.fileSelectButton.setText(" ... "); //$NON-NLS-1$
								this.fileSelectButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0502));
								this.fileSelectButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
								this.fileSelectButton.setBounds(415, 96, 30, 20);
								this.fileSelectButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										FileDialog fileSelectionDialog = new FileDialog(DevicePropertiesEditor.this.getShell());
										fileSelectionDialog.setText("OpenSerialDataExplorer Device Image File"); //$NON-NLS-1$
										fileSelectionDialog.setFilterPath(getDevicesPath());
										fileSelectionDialog.setFilterExtensions(new String[] { OSDE.FILE_ENDING_STAR_JPG, OSDE.FILE_ENDING_STAR_GIF, OSDE.FILE_ENDING_STAR_PNG });
										fileSelectionDialog.setFilterNames(new String[] { Messages.getString(MessageIds.OSDE_MSGT0215), Messages.getString(MessageIds.OSDE_MSGT0214), Messages.getString(MessageIds.OSDE_MSGT0213) });
										fileSelectionDialog.open();
										DevicePropertiesEditor.this.imageFileName = fileSelectionDialog.getFileName();
										if (DevicePropertiesEditor.this.imageFileName != null && DevicePropertiesEditor.this.imageFileName.length() > 5) {
											DevicePropertiesEditor.this.imageFileNameText.setText(DevicePropertiesEditor.this.imageFileName);
											DevicePropertiesEditor.log.log(Level.INFO, "imageFileName = " + DevicePropertiesEditor.this.imageFileName); //$NON-NLS-1$
											if (DevicePropertiesEditor.this.deviceConfig != null) {
												DevicePropertiesEditor.this.deviceConfig.setImageFileName(DevicePropertiesEditor.this.imageFileName = DevicePropertiesEditor.this.imageFileNameText.getText());
											}
										}
									}
								});
							}
						}
					}
				}
				{
					this.serialPortTabItem = new SeriaPortTypeTabItem(this.tabFolder, SWT.CLOSE, 1, this.popupMenu);
				}
				{
					this.timeBaseTabItem = new CTabItem(this.tabFolder, SWT.NONE);
					this.timeBaseTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0495));
					this.timeBaseTabItem.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
					{
						this.timeBaseComposite = new Composite(this.tabFolder, SWT.NONE);
						this.timeBaseTabItem.setControl(this.timeBaseComposite);
						this.timeBaseComposite.setLayout(null);
						this.timeBaseComposite.setMenu(this.popupMenu);
						this.timeBaseComposite.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								DevicePropertiesEditor.log.log(Level.FINEST, "timeBaseComposite.paintControl, event=" + evt); //$NON-NLS-1$
								DevicePropertiesEditor.this.timeBaseTimeStepText.setText(String.format("%.1f", DevicePropertiesEditor.this.timeStep_ms)); //$NON-NLS-1$
							}
						});
						{
							this.timeBaseDescriptionLabel = new Label(this.timeBaseComposite, SWT.CENTER | SWT.WRAP);
							this.timeBaseDescriptionLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0496));
							this.timeBaseDescriptionLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
							this.timeBaseDescriptionLabel.setBounds(17, 12, 591, 71);
							this.timeBaseDescriptionLabel.setMenu(this.popupMenu);
						}
						{
							this.timeBaseNameLabel = new Label(this.timeBaseComposite, SWT.RIGHT);
							this.timeBaseNameLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0497));
							this.timeBaseNameLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
							this.timeBaseNameLabel.setBounds(142, 95, 150, 20);
							this.timeBaseNameLabel.setMenu(this.popupMenu);
						}
						{
							this.timeBaseNameText = new Text(this.timeBaseComposite, SWT.CENTER | SWT.BORDER);
							this.timeBaseNameText.setText(Messages.getString(MessageIds.OSDE_MSGT0271).split(OSDE.STRING_BLANK)[0]);
							this.timeBaseNameText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
							this.timeBaseNameText.setBounds(322, 94, 60, 20);
							this.timeBaseNameText.setEditable(false);
							this.timeBaseNameText.setMenu(this.popupMenu);
						}
						{
							this.timeBaseSymbolLabel = new Label(this.timeBaseComposite, SWT.RIGHT);
							this.timeBaseSymbolLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0498));
							this.timeBaseSymbolLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
							this.timeBaseSymbolLabel.setBounds(142, 125, 150, 20);
							this.timeBaseSymbolLabel.setMenu(this.popupMenu);
						}
						{
							this.timeBaseSymbolText = new Text(this.timeBaseComposite, SWT.CENTER | SWT.BORDER);
							this.timeBaseSymbolText.setText(Messages.getString(MessageIds.OSDE_MSGT0271).split(OSDE.STRING_BLANK)[1]);
							this.timeBaseSymbolText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
							this.timeBaseSymbolText.setBounds(322, 124, 60, 20);
							this.timeBaseSymbolText.setEditable(false);
							this.timeBaseSymbolText.setMenu(this.popupMenu);
						}
						{
							this.timeBaseUnitLabel = new Label(this.timeBaseComposite, SWT.RIGHT);
							this.timeBaseUnitLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0499));
							this.timeBaseUnitLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
							this.timeBaseUnitLabel.setBounds(142, 155, 150, 20);
							this.timeBaseUnitLabel.setMenu(this.popupMenu);
						}
						{
							this.timeBaseUnitText = new Text(this.timeBaseComposite, SWT.CENTER | SWT.BORDER);
							this.timeBaseUnitText.setText(Messages.getString(MessageIds.OSDE_MSGT0271).split(OSDE.STRING_BLANK)[3]);
							this.timeBaseUnitText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
							this.timeBaseUnitText.setBounds(322, 154, 60, 20);
							this.timeBaseUnitText.setEditable(false);
							this.timeBaseUnitText.setMenu(this.popupMenu);
						}
						{
							this.timeBaseTimeStepLabel = new Label(this.timeBaseComposite, SWT.RIGHT);
							this.timeBaseTimeStepLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0500));
							this.timeBaseTimeStepLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
							this.timeBaseTimeStepLabel.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0501));
							this.timeBaseTimeStepLabel.setBounds(142, 185, 150, 20);
							this.timeBaseTimeStepLabel.setMenu(this.popupMenu);
						}
						{
							this.timeBaseTimeStepText = new Text(this.timeBaseComposite, SWT.RIGHT | SWT.BORDER);
							this.timeBaseTimeStepText.setText("1000.0"); //$NON-NLS-1$
							this.timeBaseTimeStepText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
							this.timeBaseTimeStepText.setBounds(322, 184, 60, 20);
							this.timeBaseTimeStepText.setMenu(this.popupMenu);
							this.timeBaseTimeStepText.addVerifyListener(new VerifyListener() {
								public void verifyText(VerifyEvent evt) {
									DevicePropertiesEditor.log.log(Level.FINEST, "timeBaseTimeStepText.verifyText, event=" + evt); //$NON-NLS-1$
									evt.doit = StringHelper.verifyTypedInput(DataTypes.DOUBLE, evt.text);
								}
							});
							this.timeBaseTimeStepText.addKeyListener(new KeyAdapter() {
								public void keyReleased(KeyEvent evt) {
									DevicePropertiesEditor.log.log(Level.FINEST, "timeBaseTimeStepText.keyReleased, event=" + evt); //$NON-NLS-1$
									try {
										DevicePropertiesEditor.this.timeStep_ms = Double.parseDouble(DevicePropertiesEditor.this.timeBaseTimeStepText.getText().replace(OSDE.STRING_COMMA, OSDE.STRING_DOT));
										DevicePropertiesEditor.this.timeStep_ms = Double.parseDouble(String.format(Locale.ENGLISH, "%.1f", DevicePropertiesEditor.this.timeStep_ms)); //$NON-NLS-1$
										if (DevicePropertiesEditor.this.deviceConfig != null) DevicePropertiesEditor.this.deviceConfig.setTimeStep_ms(DevicePropertiesEditor.this.timeStep_ms);
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
					this.channelConfigurationTabItem = new CTabItem(this.tabFolder, SWT.NONE);
					this.channelConfigurationTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0503));
					this.channelConfigurationTabItem.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
					{
						this.channelConfigComposite = new Composite(this.tabFolder, SWT.NONE);
						this.channelConfigComposite.setLayout(null);
						this.channelConfigurationTabItem.setControl(this.channelConfigComposite);
						this.channelConfigComposite.setMenu(this.popupMenu);
						this.channelConfigComposite.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								DevicePropertiesEditor.log.log(Level.FINEST, "channleConfigComposite.paintControl, event=" + evt); //$NON-NLS-1$
								if (DevicePropertiesEditor.this.deviceConfig != null) {
									for (int i = 1; i <= DevicePropertiesEditor.this.deviceConfig.getChannelCount(); i++) {
										DevicePropertiesEditor.this.deviceConfig.getChannelTypes(i);
									}
								}
							}
						});
						{
							this.channelConfigDescriptionLabel = new Label(this.channelConfigComposite, SWT.CENTER | SWT.WRAP);
							this.channelConfigDescriptionLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0504));
							this.channelConfigDescriptionLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
							this.channelConfigDescriptionLabel.setBounds(12, 5, 602, 38);
							this.channelConfigDescriptionLabel.setMenu(this.popupMenu);
						}
						{
							this.channelConfigInnerTabFolder = new CTabFolder(this.channelConfigComposite, SWT.NONE | SWT.BORDER);
							this.channelConfigInnerTabFolder.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
							this.channelConfigInnerTabFolder.setBounds(0, 49, 626, 285);
							this.channelConfigInnerTabFolder.setMenu(this.popupMenu);
							{
								//initial channel TabItem
								new ChannelTypeTabItem(this.channelConfigInnerTabFolder, SWT.NONE, 0);
							}
							this.channelConfigInnerTabFolder.setSelection(0);
						}
					}
				}
				{
					this.destopTabItem = new CTabItem(this.tabFolder, SWT.NONE);
					this.destopTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0505));
					this.destopTabItem.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
					{
						this.desktopComposite = new Composite(this.tabFolder, SWT.NONE);
						this.desktopComposite.setLayout(null);
						this.destopTabItem.setControl(this.desktopComposite);
						this.desktopComposite.setMenu(this.popupMenu);
						//						desktopComposite.addPaintListener(new PaintListener() {
						//							public void paintControl(PaintEvent evt) {
						//								log.log(Level.FINEST, "desktopComposite.paintControl, event="+evt);
						//							}
						//						});
						{
							this.desktopDescriptionLabel = new Label(this.desktopComposite, SWT.CENTER | SWT.WRAP);
							this.desktopDescriptionLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0506));
							this.desktopDescriptionLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
							this.desktopDescriptionLabel.setBounds(12, 5, 602, 57);
							this.desktopDescriptionLabel.setMenu(this.popupMenu);
						}
						{
							this.desktopTabFolder = new CTabFolder(this.desktopComposite, SWT.BORDER);
							GridLayout appDesktopTabCompositeLayout = new GridLayout();
							appDesktopTabCompositeLayout.makeColumnsEqualWidth = true;
							this.desktopTabFolder.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
							this.desktopTabFolder.setLayout(appDesktopTabCompositeLayout);
							this.desktopTabFolder.setBounds(135, 68, 360, 196);
							{
								this.desktopInnerTabItem1 = new PropertyTypeTabItem(this.desktopTabFolder, SWT.NONE, DesktopPropertyTypes.TABLE_TAB.value());
							}
							{
								this.desktopInnerTabItem2 = new PropertyTypeTabItem(this.desktopTabFolder, SWT.NONE, DesktopPropertyTypes.DIGITAL_TAB.value());
							}
							{
								this.desktopInnerTabItem3 = new PropertyTypeTabItem(this.desktopTabFolder, SWT.NONE, DesktopPropertyTypes.ANALOG_TAB.value());
							}
							{
								this.desktopInnerTabItem4 = new PropertyTypeTabItem(this.desktopTabFolder, SWT.NONE, DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB.value());
							}
							this.desktopTabFolder.setSelection(0);
						}
					}
				}
				this.tabFolder.setSelection(0);
				this.tabFolder.setBounds(0, 45, 632, 360);
				this.tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
					public void restore(CTabFolderEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "tabFolder.restore, event=" + evt); //$NON-NLS-1$
						((CTabItem) evt.item).getControl();
					}

					public void close(CTabFolderEvent evt) {
						DevicePropertiesEditor.log.log(Level.INFO, "tabFolder.close, event=" + evt); //$NON-NLS-1$
						CTabItem tabItem = ((CTabItem) evt.item);
						if (tabItem.getText().equals(Messages.getString(MessageIds.OSDE_MSGT0470))) {
							tabItem.dispose();
							modeStateTabItem = null;
							if (DevicePropertiesEditor.this.deviceConfig != null) DevicePropertiesEditor.this.deviceConfig.removeStateType();
						}
						else if (tabItem.getText().equals(Messages.getString(MessageIds.OSDE_MSGT0512))) {
							tabItem.dispose();
							serialPortTabItem = null;
							if (DevicePropertiesEditor.this.deviceConfig != null) DevicePropertiesEditor.this.deviceConfig.removeSerialPortType();
						}
						else if (tabItem.getText().equals(Messages.getString(MessageIds.OSDE_MSGT0515))) {
							tabItem.dispose();
							dataBlockTabItem = null;
							if (DevicePropertiesEditor.this.deviceConfig != null) DevicePropertiesEditor.this.deviceConfig.removeDataBlockType();
						}
						if (DevicePropertiesEditor.this.deviceConfig != null) update();
					}
				});
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * create a new data block type and place it right after time base
	 */
	public void createDataBlockType() {
		for (int i = 1; i < this.tabFolder.getItemCount(); i++) {
			if (this.tabFolder.getItem(i).getText().equals(Messages.getString(MessageIds.OSDE_MSGT0495))) {
				this.dataBlockTabItem = new CTabItem(this.tabFolder, SWT.CLOSE, i + 1);
				break;
			}
		}
		//dataBlockTabItem = new CTabItem(tabFolder, SWT.CLOSE); //remove the leading comment to enable visual edit
		this.dataBlockTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0515));
		{
			this.dataBlockComposite = new Composite(this.tabFolder, SWT.NONE);
			this.dataBlockComposite.setLayout(null);
			this.dataBlockTabItem.setControl(this.dataBlockComposite);
			this.dataBlockComposite.setMenu(this.popupMenu);
			{
				this.dataBlockDescriptionLabel = new Label(this.dataBlockComposite, SWT.CENTER);
				this.dataBlockDescriptionLabel
						.setText(Messages.getString(MessageIds.OSDE_MSGT0516));
				this.dataBlockDescriptionLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
				this.dataBlockDescriptionLabel.setBounds(12, 5, 602, 51);
				this.dataBlockDescriptionLabel.setMenu(this.popupMenu);
			}
			{
				this.dataBlockRequiredGroup = new Group(this.dataBlockComposite, SWT.NONE);
				this.dataBlockRequiredGroup.setLayout(null);
				this.dataBlockRequiredGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0517));
				this.dataBlockRequiredGroup.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
				this.dataBlockRequiredGroup.setBounds(40, 80, 250, 170);
				this.dataBlockRequiredGroup.setMenu(this.popupMenu);
				this.dataBlockRequiredGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockRequiredGroup.paintControl, event=" + evt); //$NON-NLS-1$
						DevicePropertiesEditor.this.dataBlockFormatCombo.select(DevicePropertiesEditor.this.dataBlockFormat == FormatTypes.TEXT ? 0 : 1);
						DevicePropertiesEditor.this.dataBlockSizeText.setText(OSDE.STRING_EMPTY + DevicePropertiesEditor.this.dataBlockSize);
					}
				});
				{
					this.dataBlockFormatLabel = new Label(this.dataBlockRequiredGroup, SWT.RIGHT);
					this.dataBlockFormatLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0518));
					this.dataBlockFormatLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
					this.dataBlockFormatLabel.setBounds(19, 51, 85, 16);
					this.dataBlockFormatLabel.setMenu(this.popupMenu);
				}
				{
					this.dataBlockFormatCombo = new CCombo(this.dataBlockRequiredGroup, SWT.BORDER);
					this.dataBlockFormatCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
					this.dataBlockFormatCombo.setBounds(125, 49, 84, 21);
					this.dataBlockFormatCombo.setItems(new java.lang.String[] { "TEXT", "BINARY" });
					this.dataBlockFormatCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
					this.dataBlockFormatCombo.setLayout(null);
					this.dataBlockFormatCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockFormatCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockFormat = FormatTypes.valueOf(DevicePropertiesEditor.this.dataBlockFormatCombo.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockFormat(DevicePropertiesEditor.this.dataBlockFormat);
							}
						}
					});
				}
				{
					this.dataBlockSizeLabel = new Label(this.dataBlockRequiredGroup, SWT.RIGHT);
					this.dataBlockSizeLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0520));
					this.dataBlockSizeLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
					this.dataBlockSizeLabel.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0521));
					this.dataBlockSizeLabel.setBounds(19, 105, 85, 16);
					this.dataBlockSizeLabel.setMenu(this.popupMenu);
				}
				{
					this.dataBlockSizeText = new Text(this.dataBlockRequiredGroup, SWT.RIGHT | SWT.BORDER);
					this.dataBlockSizeText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
					this.dataBlockSizeText.setBounds(127, 103, 64, 22);
					this.dataBlockSizeText.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockSizeText.keyReleased, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockSize = Integer.parseInt(DevicePropertiesEditor.this.dataBlockSizeText.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockSize(DevicePropertiesEditor.this.dataBlockSize);
							}
						}
					});
					this.dataBlockSizeText.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockSizeText.verifyText, event=" + evt); //$NON-NLS-1$
							evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
						}
					});
				}
			}
			{
				this.dataBlockOptionalGroup = new Group(this.dataBlockComposite, SWT.NONE);
				this.dataBlockOptionalGroup.setLayout(null);
				this.dataBlockOptionalGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0522));
				this.dataBlockOptionalGroup.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
				this.dataBlockOptionalGroup.setBounds(330, 80, 250, 170);
				this.dataBlockOptionalGroup.setMenu(this.popupMenu);
				this.dataBlockOptionalGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockOptionalGroup.paintControl, event=" + evt); //$NON-NLS-1$
//						if (DevicePropertiesEditor.this.deviceConfig != null && DevicePropertiesEditor.this.deviceConfig.getDataBlockCheckSumFormat() != null
//								&& DevicePropertiesEditor.this.deviceConfig.getDataBlockCheckSumType() != null && DevicePropertiesEditor.this.deviceConfig.getDataBlockEnding() != null) {
							DevicePropertiesEditor.this.dataBlockOptionalEnableButton.setSelection(isDataBlockOptionalGroupEnabled);
							DevicePropertiesEditor.this.dataBlockcheckSumFormatCombo.select(DevicePropertiesEditor.this.dataBlockcheckSumFormat == FormatTypes.TEXT ? 0 : 1);
							DevicePropertiesEditor.this.dataBlockCheckSumTypeCombo.select(DevicePropertiesEditor.this.dataBlockCheckSumType == ChecksumTypes.XOR ? 0 : 1);
							DevicePropertiesEditor.this.dataBlockEndingText.setText(DevicePropertiesEditor.this.dataBlockEnding);
//						}
//						else {
//							DevicePropertiesEditor.this.dataBlockOptionalEnableButton.setSelection(false);
//						}
					}
				});
				{
					this.dataBlockOptionalEnableButton = new Button(this.dataBlockOptionalGroup, SWT.CHECK | SWT.RIGHT);
					this.dataBlockOptionalEnableButton.setText(Messages.getString(MessageIds.OSDE_MSGT0478));
					this.dataBlockOptionalEnableButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
					this.dataBlockOptionalEnableButton.setBounds(143, 14, 92, 20);
					this.dataBlockOptionalEnableButton.setMenu(this.popupMenu);
					this.dataBlockOptionalEnableButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockOptionalEnableButton.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.isDataBlockOptionalGroupEnabled = DevicePropertiesEditor.this.dataBlockOptionalEnableButton.getSelection();
							enableDataBlockOptionalPart(DevicePropertiesEditor.this.isDataBlockOptionalGroupEnabled);
							if (deviceConfig != null) {
								if (DevicePropertiesEditor.this.isDataBlockOptionalGroupEnabled) {
									deviceConfig.setDataBlockCheckSumFormat(dataBlockcheckSumFormat);
									deviceConfig.setDataBlockCheckSumType(dataBlockCheckSumType);
									deviceConfig.setDataBlockEnding(StringHelper.convert2ByteArray(dataBlockEnding));
								}
								else {
									deviceConfig.setDataBlockCheckSumFormat(null);
									deviceConfig.setDataBlockCheckSumType(null);
									deviceConfig.setDataBlockEnding(null);
								}
							}
						}
					});
				}
				{
					this.dataBlockCheckSumFormatLabel = new Label(this.dataBlockOptionalGroup, SWT.RIGHT);
					this.dataBlockCheckSumFormatLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0466));
					this.dataBlockCheckSumFormatLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
					this.dataBlockCheckSumFormatLabel.setBounds(6, 46, 122, 20);
					this.dataBlockCheckSumFormatLabel.setEnabled(false);
					this.dataBlockCheckSumFormatLabel.setMenu(this.popupMenu);
				}
				{
					this.dataBlockCheckSumLabel = new Label(this.dataBlockOptionalGroup, SWT.RIGHT);
					this.dataBlockCheckSumLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0467));
					this.dataBlockCheckSumLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
					this.dataBlockCheckSumLabel.setBounds(6, 77, 122, 20);
					this.dataBlockCheckSumLabel.setEnabled(false);
					this.dataBlockCheckSumLabel.setMenu(this.popupMenu);
				}
				{
					this.dataBlockEndingLabel = new Label(this.dataBlockOptionalGroup, SWT.RIGHT);
					this.dataBlockEndingLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0468));
					this.dataBlockEndingLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
					this.dataBlockEndingLabel.setBounds(6, 111, 122, 20);
					this.dataBlockEndingLabel.setEnabled(false);
					this.dataBlockEndingLabel.setMenu(this.popupMenu);
				}
				{
					this.dataBlockcheckSumFormatCombo = new CCombo(this.dataBlockOptionalGroup, SWT.BORDER);
					this.dataBlockcheckSumFormatCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
					this.dataBlockcheckSumFormatCombo.setItems(new java.lang.String[] { "TEXT", "BINARY" });
					this.dataBlockcheckSumFormatCombo.setBounds(143, 44, 92, 20);
					this.dataBlockcheckSumFormatCombo.setEditable(false);
					this.dataBlockcheckSumFormatCombo.setEnabled(false);
					this.dataBlockcheckSumFormatCombo.select(1);
					this.dataBlockcheckSumFormatCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
					this.dataBlockcheckSumFormatCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockceckSumFormatCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockcheckSumFormat = FormatTypes.valueOf(DevicePropertiesEditor.this.dataBlockcheckSumFormatCombo.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockCheckSumFormat(DevicePropertiesEditor.this.dataBlockcheckSumFormat);
							}
						}
					});
				}
				{
					this.dataBlockCheckSumTypeCombo = new CCombo(this.dataBlockOptionalGroup, SWT.RIGHT | SWT.BORDER);
					this.dataBlockCheckSumTypeCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
					this.dataBlockCheckSumTypeCombo.setItems(new String[] { "XOR", "ADD" }); //$NON-NLS-1$ //$NON-NLS-2$
					this.dataBlockCheckSumTypeCombo.setBounds(143, 74, 92, 20);
					this.dataBlockCheckSumTypeCombo.setEditable(false);
					this.dataBlockCheckSumTypeCombo.setEnabled(false);
					this.dataBlockCheckSumTypeCombo.select(1);
					this.dataBlockCheckSumTypeCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
					this.dataBlockCheckSumTypeCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockCheckSumCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockCheckSumType = ChecksumTypes.valueOf(DevicePropertiesEditor.this.dataBlockCheckSumTypeCombo.getText());
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockCheckSumType(DevicePropertiesEditor.this.dataBlockCheckSumType);
							}
						}
					});
				}
				{
					this.dataBlockEndingText = new Text(this.dataBlockOptionalGroup, SWT.BORDER);
					this.dataBlockEndingText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
					this.dataBlockEndingText.setBounds(144, 109, 61, 20);
					this.dataBlockEndingText.setEnabled(false);
					this.dataBlockEndingText.addKeyListener(new KeyAdapter() {
						public void keyReleased(KeyEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockEndingText.keyReleased, event=" + evt); //$NON-NLS-1$
							DevicePropertiesEditor.this.dataBlockEnding = DevicePropertiesEditor.this.dataBlockEndingText.getText();
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.setDataBlockEnding(StringHelper.convert2ByteArray(DevicePropertiesEditor.this.dataBlockEnding));
							}
						}
					});
					this.dataBlockEndingText.addVerifyListener(new VerifyListener() {
						public void verifyText(VerifyEvent evt) {
							DevicePropertiesEditor.log.log(Level.FINEST, "dataBlockEndingText.verifyText, event=" + evt); //$NON-NLS-1$
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
	public void createStateTabItem() {
		int index = 0;
		for (CTabItem tabItem : this.tabFolder.getItems()) {
			if (tabItem.getText().equals(Messages.getString(MessageIds.OSDE_MSGT0503))) break;
			++index;
		}
		this.modeStateTabItem = new CTabItem(this.tabFolder, SWT.CLOSE, index);
		this.modeStateTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0470));
		this.modeStateTabItem.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
		{
			this.modeStateComposite = new Composite(this.tabFolder, SWT.NONE);
			this.modeStateComposite.setLayout(null);
			this.modeStateTabItem.setControl(this.modeStateComposite);
			this.modeStateComposite.setMenu(this.popupMenu);
			this.modeStateComposite.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					DevicePropertiesEditor.log.log(Level.FINEST, "modeStateComposite.paintControl, event=" + evt); //$NON-NLS-1$
					for (CTabItem child : DevicePropertiesEditor.this.stateTabFolder.getItems()) {
						((PropertyTypeTabItem) child).propertyTypeComposite.redraw();
					}
				}
			});
			{
				this.modeStateDescriptionLabel = new Label(this.modeStateComposite, SWT.LEFT);
				this.modeStateDescriptionLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0471));
				this.modeStateDescriptionLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
				this.modeStateDescriptionLabel.setBounds(165, 4, 449, 55);
				this.modeStateDescriptionLabel.setMenu(this.popupMenu);
			}
			{
				this.stateTabFolder = new CTabFolder(this.modeStateComposite, SWT.BORDER);
				this.stateTabFolder.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
				this.stateTabFolder.setBounds(165, 65, 300, 207);
				this.stateTabFolder.setSelection(0);
				{
						PropertyType property = new ObjectFactory().createPropertyType();
						property.setName(Messages.getString(MessageIds.OSDE_MSGT0473));
						property.setType(DataTypes.INTEGER);
						property.setValue(OSDE.STRING_EMPTY + DevicePropertiesEditor.this.stateTabFolder.getItemCount()+1);
						property.setDescription(Messages.getString(MessageIds.OSDE_MSGT0474));
						PropertyTypeTabItem tmpPropertyTypeTabItem = new PropertyTypeTabItem(DevicePropertiesEditor.this.stateTabFolder, SWT.CLOSE, Messages.getString(MessageIds.OSDE_MSGT0470));
						if (DevicePropertiesEditor.this.deviceConfig != null) {
							DevicePropertiesEditor.this.deviceConfig.appendStateType(property);
						}
						tmpPropertyTypeTabItem.setProperty(DevicePropertiesEditor.this.deviceConfig, property, true, false, false, false);
				}
				this.stateTabFolder.setSelection(0);
				this.stateTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
					public void close(CTabFolderEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "modeStateTabFolder.close, event=" + evt); //$NON-NLS-1$
						if (DevicePropertiesEditor.this.deviceConfig != null) {
							PropertyTypeTabItem tmpTabItem = (PropertyTypeTabItem)evt.item;
							int childIndex = DevicePropertiesEditor.this.deviceConfig.getStateType().getProperty().indexOf(tmpTabItem.propertyType);
							DevicePropertiesEditor.this.stateTabFolder.getChildren()[childIndex].dispose();
							DevicePropertiesEditor.this.deviceConfig.removeStateType(DevicePropertiesEditor.this.deviceConfig.getStateType().getProperty().get(childIndex));
						}
						evt.item.dispose();
					}
				});
			}
			{
				this.addButton = new Button(this.modeStateComposite, SWT.PUSH | SWT.CENTER);
				this.addButton.setText(Messages.getString(MessageIds.OSDE_MSGT0472));
				this.addButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL, false, false));
				this.addButton.setBounds(165, 284, 300, 30);
				this.addButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						DevicePropertiesEditor.log.log(Level.FINEST, "addButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						{
							PropertyType property = new ObjectFactory().createPropertyType();
							property.setName(Messages.getString(MessageIds.OSDE_MSGT0473));
							property.setType(DataTypes.INTEGER);
							property.setValue(OSDE.STRING_EMPTY + (DevicePropertiesEditor.this.stateTabFolder.getItemCount()+1));
							property.setDescription(Messages.getString(MessageIds.OSDE_MSGT0474));
							PropertyTypeTabItem tmpPropertyTypeTabItem = new PropertyTypeTabItem(DevicePropertiesEditor.this.stateTabFolder, SWT.CLOSE, Messages.getString(MessageIds.OSDE_MSGT0470));
							if (DevicePropertiesEditor.this.deviceConfig != null) {
								DevicePropertiesEditor.this.deviceConfig.appendStateType(property);
							}
							tmpPropertyTypeTabItem.setProperty(deviceConfig, property, true, false, false, false);
							DevicePropertiesEditor.this.stateTabFolder.setSelection(DevicePropertiesEditor.this.stateTabFolder.getItemCount()-1);
						}
					}
				});
			}
		}
	}

	/**
	 * create a new serial port tabulator
	 */
	public void createSerialPortTabItem() {
		this.serialPortTabItem = new SeriaPortTypeTabItem(this.tabFolder, SWT.CLOSE, 1, this.popupMenu);
	}

	/**
	 * query the Devices path to open, query, save the device properties
	 * @return
	 */
	private String getDevicesPath() {
		String osname = System.getProperty("os.name", OSDE.STRING_EMPTY).toLowerCase(); //$NON-NLS-1$
		String applHomePath = "";
		if (osname.startsWith("windows")) { //$NON-NLS-1$
			applHomePath = (System.getenv("APPDATA") + OSDE.FILE_SEPARATOR_UNIX + "OpenSerialDataExplorer" + OSDE.FILE_SEPARATOR_UNIX).replace("\\", OSDE.FILE_SEPARATOR_UNIX); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		else if (osname.startsWith("linux") || osname.startsWith("mac")) { //$NON-NLS-1$ //$NON-NLS-2$
			applHomePath = System.getProperty("user.home") + OSDE.FILE_SEPARATOR_UNIX + ".OpenSerialDataExplorer" + OSDE.FILE_SEPARATOR_UNIX; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return applHomePath + "Devices"; //$NON-NLS-1$
	}

	/**
	 * update internal variables by device properties
	 */
	public void update() {
		//DeviceType begin
		this.deviceName = this.deviceConfig.getName();
		this.manufacturer = this.deviceConfig.getManufacturer();
		this.manufacuturerURL = this.deviceConfig.getManufacturerURL();
		this.imageFileName = this.deviceConfig.getImageFileName();
		this.isDeviceUsed = this.deviceConfig.isUsed();
		this.deviceGroup = this.deviceConfig.getDeviceGroup();
		this.devicePropsComposite.redraw();
		//DeviceType end

		//SerialPortType begin
		if (this.deviceConfig.getSerialPortType() == null && this.serialPortTabItem != null && !this.serialPortTabItem.isDisposed()) {
			this.serialPortTabItem.dispose();
			this.serialPortTabItem = null;
		}
		else if (this.deviceConfig.getSerialPortType() != null && this.serialPortTabItem != null && this.serialPortTabItem.isDisposed()) {
			this.serialPortTabItem = new SeriaPortTypeTabItem(this.tabFolder, SWT.CLOSE, 1, this.popupMenu);
		}
		if (this.deviceConfig.getSerialPortType() != null && this.serialPortTabItem != null && !this.serialPortTabItem.isDisposed()) {
			this.serialPortTabItem.setDeviceConfig(this.deviceConfig);
		}
		//SerialPortType end

		//TimeBaseType begin
		this.timeStep_ms = this.deviceConfig.getTimeStep_ms();
		this.timeBaseComposite.redraw();
		//TimeBaseType end

		//DataBlockType begin
		if (this.deviceConfig.getDataBlockType() == null && this.dataBlockTabItem != null && !this.dataBlockTabItem.isDisposed()) {
			this.dataBlockTabItem.dispose();
			this.dataBlockTabItem = null;
		}
		else {
			if (this.deviceConfig.getDataBlockType() != null) {
				if ( this.dataBlockTabItem != null && this.dataBlockTabItem.isDisposed()) {
					createDataBlockType();
				}
				this.dataBlockFormat = this.deviceConfig.getDataBlockFormat();
				this.dataBlockSize = this.deviceConfig.getDataBlockSize();
				this.dataBlockRequiredGroup.redraw();

				if (this.deviceConfig.getDataBlockCheckSumFormat() != null && this.deviceConfig.getDataBlockCheckSumType() != null && this.deviceConfig.getDataBlockEnding() != null) {
					this.isDataBlockOptionalGroupEnabled = true;
					this.dataBlockcheckSumFormat = this.deviceConfig.getDataBlockCheckSumFormat();
					this.dataBlockCheckSumType = this.deviceConfig.getDataBlockCheckSumType();
					this.dataBlockEnding = StringHelper.convertHexInput(this.deviceConfig.getDataBlockEnding());
					enableDataBlockOptionalPart(true);
				}
				else {
					enableDataBlockOptionalPart(false);
				}
			}
		}
		//DataBlockType end

		//StateType begin
		int modeStateCount = (this.deviceConfig.getStateType() == null) ? 0 : this.deviceConfig.getStateSize();
		if (this.deviceConfig.getStateType() == null || (modeStateCount == 0 && (this.deviceConfig.getStateType() != null && !this.stateTabFolder.isDisposed()))) {
			if (this.modeStateTabItem != null) {
				this.modeStateTabItem.dispose();
				this.modeStateTabItem = null;
			}
		}
		else {
			if (this.deviceConfig.getStateType() != null && this.modeStateTabItem != null && this.modeStateTabItem.isDisposed()) {
				createStateTabItem();
			}
			if (this.deviceConfig.getStateType() != null && this.modeStateTabItem != null && !this.modeStateTabItem.isDisposed()) {
				if (modeStateCount > this.stateTabFolder.getItemCount()) {
					for (int i = this.stateTabFolder.getItemCount(); i < this.deviceConfig.getStateSize(); i++) {
						new PropertyTypeTabItem(this.stateTabFolder, SWT.CLOSE, Messages.getString(MessageIds.OSDE_MSGT0470) + this.stateTabFolder.getItemCount());
					}
				}
				else if (modeStateCount < this.stateTabFolder.getItemCount()) {
					CTabItem[] childs = this.stateTabFolder.getItems();
					for (int i = modeStateCount - 1; i < childs.length; i++) {
						childs[i].dispose();
						childs[i] = null;
					}
				}
				int index = 1;
				for (CTabItem child : this.stateTabFolder.getItems()) {
					((PropertyTypeTabItem) child).setProperty(this.deviceConfig, this.deviceConfig.getStateProperty(index++), true, false, false, false);
				}
				this.modeStateComposite.redraw();
			}
		}
		//StateType end

		//ChannelType begin
		int channelTypeCount = this.deviceConfig.getChannelCount();
		int actualTabItemCount = this.channelConfigInnerTabFolder.getItemCount();
		if (channelTypeCount < actualTabItemCount) {
			for (int i = channelTypeCount; i < actualTabItemCount; i++) {
				ChannelTypeTabItem channelTabItem = (ChannelTypeTabItem) this.channelConfigInnerTabFolder.getItem(channelTypeCount);
				channelTabItem.dispose();
				channelTabItem = null;
			}
		}
		else if (channelTypeCount > actualTabItemCount) {
			for (int i = actualTabItemCount; i < channelTypeCount; i++) {
				new ChannelTypeTabItem(this.channelConfigInnerTabFolder, SWT.CLOSE, i);
			}
		}
		for (int i = 0; i < channelTypeCount; i++) {
			ChannelTypeTabItem channelTabItem = (ChannelTypeTabItem) this.channelConfigInnerTabFolder.getItem(i);
			channelTabItem.setChannelType(this.deviceConfig, this.deviceConfig.getChannelType(i + 1), (i + 1));
		}
		//ChannelType end

		//DesktopType begin
		this.desktopInnerTabItem1.setProperty(this.deviceConfig, this.deviceConfig.getDesktopProperty(DesktopPropertyTypes.TABLE_TAB), false, false, false, true);
		this.desktopInnerTabItem2.setProperty(this.deviceConfig, this.deviceConfig.getDesktopProperty(DesktopPropertyTypes.DIGITAL_TAB), false, false, false, true);
		this.desktopInnerTabItem3.setProperty(this.deviceConfig, this.deviceConfig.getDesktopProperty(DesktopPropertyTypes.ANALOG_TAB), false, false, false, true);
		this.desktopInnerTabItem4.setProperty(this.deviceConfig, this.deviceConfig.getDesktopProperty(DesktopPropertyTypes.VOLTAGE_PER_CELL_TAB), false, false, false, true);
		this.desktopComposite.redraw();
		//DesktopType end
	}

	/**
	 * enable or disable data block optional properties
	 */
	void enableDataBlockOptionalPart(boolean enable) {
		this.dataBlockOptionalEnableButton.setText(enable ? Messages.getString(MessageIds.OSDE_MSGT0477) : Messages.getString(MessageIds.OSDE_MSGT0478));
		this.dataBlockCheckSumFormatLabel.setEnabled(enable);
		this.dataBlockcheckSumFormatCombo.setEnabled(enable);
		this.dataBlockCheckSumLabel.setEnabled(enable);
		this.dataBlockCheckSumTypeCombo.setEnabled(enable);
		this.dataBlockEndingLabel.setEnabled(enable);
		this.dataBlockEndingText.setEnabled(enable);
		if (enable) 
			this.dataBlockOptionalGroup.redraw();
	}
	
	public Menu getPopupMenu() {
		return this.popupMenu;
	}
	
	public void openWarningMessageBox(String errorMessage) {
  	MessageBox messageDialog = new MessageBox(shell, SWT.OK | SWT.ICON_WARNING);
  	if (errorMessage.contains(OSDE.STRING_SEMICOLON)) {
			String[] messages = errorMessage.split(OSDE.STRING_SEMICOLON);
			messageDialog.setText(messages[0]);
			messageDialog.setMessage(messages[1]);
		}
  	else {
  		messageDialog.setText(OSDE.OSDE_NAME_LONG);
			messageDialog.setMessage(errorMessage);
  	}
		messageDialog.open();

	}
}
