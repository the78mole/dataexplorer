/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channels;
import gde.device.DeviceDialog;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.ParameterConfigControl;
import gde.ui.ParameterHeaderControl;
import gde.ui.SWTResourceManager;

import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Graupner Ultra Duo Plus setup dialog
 * @author Winfried Brügmann
 */
public class UltraDuoPlusDialog extends DeviceDialog {
	final static Logger						log									= Logger.getLogger(UltraDuoPlusDialog.class.getName());
	static final String						DEVICE_JAR_NAME			= "UltramatUDP";

	private Text userNameText, memoryNameText;
	private Composite baseDeviceSetupComposite, baseDeviceSetupComposite1, baseDeviceSetupComposite2;
	private ScrolledComposite scollableDeviceComposite, scollableDeviceComposite1, scollableDeviceComposite2;
	private CTabItem channelTabItem2;
	private CTabItem channelTabItem1;
	private CTabItem baseSetupTabItem;
	private CTabFolder channelBaseDataTabFolder;
	private CLabel userLabel;
	private Button restoreButton;
	private Button backupButton;
	private Button closeButton;
	private Button helpButton;
	private CTabItem setupTabItem;
	private CTabItem memorySetupTabItem;
	private CTabFolder TabFolder;
	private Composite memoryComposite;
	private Composite memoryBoundsComposite, memorySelectComposite;
	private CLabel memorySelectLabel;
	private CLabel cellTypeSelectLabel, memoryNameLable;
	private ScrolledComposite scrolledMemoryComposite;
	private CCombo memoryCombo, cellTypeCombo;

	Composite											boundsComposite;

	final Ultramat								device;						// get device specific things, get serial port, ...
	final UltramatSerialPort			serialPort;				// open/close port execute getData()....
	final Channels								channels;					// interaction with channels, source of all records
	final Settings								settings;					// application configuration settings
	
	//input/display values
	String deviceUserName = "no name";
	int[] channelValues1 = new int[] {1, 1, 10, 1, 0, 1, 1, 1, 120, 200, 26, 3, 11, 8, 23, 1};
	int[] channelValues2 = new int[] {1, 1, 10, 1};
	int[] memoryValues = new int[]{3, 4, 3200, 9, 5, 1, 320, 0, 1, 550, 50, 105, 3, 4200, 5, 0, 3200, 3000, 50, 100, 1250, 1, 1, 10, 10, 3800, 0};
	int lastMemorySelectionIndex = 0;
	int memorySelectHeight = 750;
	ParameterConfigControl[] memoryParameters = new ParameterConfigControl[28];
	//boolean array which entry was red, which was already written, while closing the dialog all red entries which are changed and not written must be transfered
	boolean[] readCheck = new boolean[3+40];
	boolean[] change = new boolean[3+40];
	boolean[] write = new boolean[3+40];
	
//	private Shell dialogShell;
//
//	/**
//	* Auto-generated main method to display this 
//	* org.eclipse.swt.widgets.Dialog inside a new Shell.
//	*/
//	public static void main(String[] args) {
//		try {
//			Display display = Display.getDefault();
//			Shell shell = new Shell(display);
//			UltraDuoPlusDialog inst = new UltraDuoPlusDialog(shell, SWT.NULL);
//			inst.open();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public UltraDuoPlusDialog(Shell parent, int style) {
//		super(parent);
//		serialPort = null;
//		device = null;
//		channels = Channels.getInstance();
//		settings = Settings.getInstance();
//	}

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public UltraDuoPlusDialog(Shell parent, Ultramat useDevice) {
		super(parent);
		serialPort = useDevice.getCommunicationPort();
		device = useDevice;
		channels = Channels.getInstance();
		settings = Settings.getInstance();
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	@Override
	public void open() {
		try {
			if (serialPort != null && !serialPort.isConnected()) {
				try {
					serialPort.open();
					serialPort.write(UltramatSerialPort.RESET_BEGIN);
					deviceUserName = serialPort.readDeviceUserName();
					readCheck[0] = true;
					channelValues1 = serialPort.readChannelData(1);
					readCheck[1] = true;
					channelValues2 = serialPort.readChannelData(2);
					readCheck[2] = true;
				}
				catch (Exception e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(null, Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
					this.application.getDeviceSelectionDialog().open();
					return;
				}
			}
			else {
				log.log(java.util.logging.Level.SEVERE, "serial port == null");
				application.openMessageDialog(null, Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
				this.application.getDeviceSelectionDialog().open();
				return;
			}
			
			log.log(Level.FINE, "dialogShell.isDisposed() " + ((dialogShell == null) ? "null" : dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (dialogShell == null || dialogShell.isDisposed()) {
				if (settings.isDeviceDialogsModal())
					dialogShell = new Shell(application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
				else if (settings.isDeviceDialogsOnTop())
					dialogShell = new Shell(application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
				else
					dialogShell = new Shell(application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(dialogShell);
				dialogShell.setLayout(new FormLayout());
				dialogShell.setText(device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				dialogShell.layout();
				dialogShell.pack();
				dialogShell.setSize(600, 650);
				dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						log.log(Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						application.openHelpDialog(DEVICE_JAR_NAME, "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (serialPort != null && serialPort.isConnected()) {
							try {
								serialPort.write(UltramatSerialPort.RESET_END);
							}
							catch (IOException e) {
								// ignore
							}
							finally {
								serialPort.close();
							}
						}
					}
				});
				{
					boundsComposite = new Composite(dialogShell, SWT.NONE);
					FormData boundsCompositeLData = new FormData();
					boundsCompositeLData.left =  new FormAttachment(0, 1000, 0);
					boundsCompositeLData.right =  new FormAttachment(1000, 1000, 0);
					boundsCompositeLData.top =  new FormAttachment(0, 1000, 0);
					boundsCompositeLData.bottom =  new FormAttachment(1000, 1000, 0);
					boundsCompositeLData.width = 553;
					boundsCompositeLData.height = 573;
					boundsComposite.setLayoutData(boundsCompositeLData);
					boundsComposite.setLayout(new FormLayout());
					{
						userLabel = new CLabel(boundsComposite, SWT.NONE);
						userLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						userLabel.setText("Geräte-/Benutzer-Name  :");
						FormData userLabelLData = new FormData();
						userLabelLData.left =  new FormAttachment(0, 1000, 12);
						userLabelLData.top =  new FormAttachment(0, 1000, 7);
						userLabelLData.width = 209;
						userLabelLData.height = 22;
						userLabel.setLayoutData(userLabelLData);
					}
					{
						userNameText = new Text(boundsComposite, SWT.SINGLE | SWT.BORDER);
						userNameText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						userNameText.setText(deviceUserName);
						FormData userNameTextLData = new FormData();
						userNameTextLData.width = 120;
						userNameTextLData.height = 16;
						userNameTextLData.left =  new FormAttachment(0, 1000, 245);
						userNameTextLData.top =  new FormAttachment(0, 1000, 7);
						userNameText.setLayoutData(userNameTextLData);
						userNameText.addVerifyListener(new VerifyListener() {	
							@Override
							public void verifyText(VerifyEvent evt) {
								evt.doit = evt.text.length() <= 16;
							}
						});
					}
					{
						TabFolder = new CTabFolder(boundsComposite, SWT.BORDER);
						{
							setupTabItem = new CTabItem(TabFolder, SWT.NONE);
							setupTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							setupTabItem.setText("Geräteeinstellungen");
							{
								channelBaseDataTabFolder = new CTabFolder(TabFolder, SWT.NONE);
								setupTabItem.setControl(channelBaseDataTabFolder);
								{
									baseSetupTabItem = new CTabItem(channelBaseDataTabFolder, SWT.NONE);
									baseSetupTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									baseSetupTabItem.setText("Basis-Setup");
									{
										scollableDeviceComposite = new ScrolledComposite(channelBaseDataTabFolder, SWT.BORDER | SWT.V_SCROLL);
										baseSetupTabItem.setControl(scollableDeviceComposite);
										FillLayout composite2Layout = new FillLayout();
										scollableDeviceComposite.setLayout(composite2Layout);
										{
											baseDeviceSetupComposite = new Composite(scollableDeviceComposite, SWT.NONE);
											FillLayout composite1Layout = new FillLayout(SWT.VERTICAL);
											baseDeviceSetupComposite.setLayout(composite1Layout);
											new ParameterHeaderControl(baseDeviceSetupComposite, "Parametername    ", 150, "Wert", 50, "Wertebereich, Einheit", 150, 20);
											new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 4, "Temp Scale", "0=°C, 1=°F", false, 0, 11, 1);
											new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 5, "Button Sound", "0=On, 1=Off", false, 0, 11, 1);
											new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 6, "Languages", "0=En, 1=De, 2=Fr, 3=It", false, 0, 13, 1);
											new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 7, "PC setup", "0=DISABLE, 1=ENABLE", false, 0, 11, 1);
											new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 8, "Supply Voltage", "120 ~ 150 (12.0 ~ 15.0V)", true, 60, 85, 2);
											new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 9, "Supply Current", "50 ~ 400 (5 ~ 40A)", true, 50, 410, 1);
											new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 10, "Time Setup – day", "1 ~ 31", false, 1, 41, 1);
											new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 11, "Time Setup – month", "1 ~ 12", false, 1, 22, 1);
											new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 12, "Time Setup – Year", "0 ~ 99", false, 0, 109, 1);
											new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 13, "Time Setup – Hour", "0 ~ 12", false, 0, 22, 1);
											new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 14, "Time Setup – minute", "0 ~ 59", false, 0, 69, 1);
											new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 15, "Time Setup – Format", "0=12H, 1=24H", false, 0, 11, 1);
										}
										scollableDeviceComposite.setContent(baseDeviceSetupComposite);
										baseDeviceSetupComposite.setSize(550, 390);
										scollableDeviceComposite.addControlListener(new ControlListener() {

											@Override
											public void controlResized(ControlEvent evt) {
												log.log(Level.FINEST, "baseDeviceSetupComposite.controlResized, event=" + evt);
												baseDeviceSetupComposite.setSize(scollableDeviceComposite.getClientArea().width, 390);
											}

											@Override
											public void controlMoved(ControlEvent evt) {
												log.log(Level.FINEST, "baseDeviceSetupComposite.controlMoved, event=" + evt);
												baseDeviceSetupComposite.setSize(scollableDeviceComposite.getClientArea().width, 390);
											}
										});
									}
								}
								{
									channelTabItem1 = new CTabItem(channelBaseDataTabFolder, SWT.NONE);
									channelTabItem1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									channelTabItem1.setText("Geräteausgang 1");
									{										

										scollableDeviceComposite1 = new ScrolledComposite(channelBaseDataTabFolder, SWT.BORDER | SWT.V_SCROLL);
										channelTabItem1.setControl(scollableDeviceComposite1);
										FillLayout composite2Layout = new FillLayout();
										scollableDeviceComposite1.setLayout(composite2Layout);
										{
											baseDeviceSetupComposite1 = new Composite(scollableDeviceComposite1, SWT.NONE);
											FillLayout composite1Layout = new FillLayout(SWT.VERTICAL);
											baseDeviceSetupComposite1.setLayout(composite1Layout);
											new ParameterHeaderControl(baseDeviceSetupComposite1, "Parametername    ", 150, "Wert", 50, "Wertebereich, Einheit", 150, 20);
											new ParameterConfigControl(baseDeviceSetupComposite1, channelValues1, 0, "Finish Sound Time", "0=OFF, 1=5s, 2=15s, 3=1m, 4=ON", false, 0, 14, 1);
											new ParameterConfigControl(baseDeviceSetupComposite1, channelValues1, 1, "Finish Melody", "1 ~ 10", false, 1, 20, 1);
											new ParameterConfigControl(baseDeviceSetupComposite1, channelValues1, 2, "LCD Contrast", "1 ~ 15", false, 1, 25, 1);
											new ParameterConfigControl(baseDeviceSetupComposite1, channelValues1, 3, "Power On Display", "0=MOVE, 1=LAST", false, 0, 11, 1);
										}
										scollableDeviceComposite1.setContent(baseDeviceSetupComposite1);
										baseDeviceSetupComposite1.setSize(550, 150);
										scollableDeviceComposite1.addControlListener(new ControlListener() {

											@Override
											public void controlResized(ControlEvent evt) {
												log.log(Level.FINEST, "baseDeviceSetupComposite1.controlResized, event=" + evt);
												baseDeviceSetupComposite1.setSize(scollableDeviceComposite1.getClientArea().width, 150);
												System.out.println("baseDeviceSetupComposite1 " + baseDeviceSetupComposite1.getBounds());
											}

											@Override
											public void controlMoved(ControlEvent evt) {
												log.log(Level.FINEST, "baseDeviceSetupComposite1.controlMoved, event=" + evt);
												baseDeviceSetupComposite1.setSize(scollableDeviceComposite1.getClientArea().width, 150);
											}
										});
																		}
								}
								{
									channelTabItem2 = new CTabItem(channelBaseDataTabFolder, SWT.NONE);
									channelTabItem2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									channelTabItem2.setText("Geräteausgang 2");
									{
										scollableDeviceComposite2 = new ScrolledComposite(channelBaseDataTabFolder, SWT.BORDER | SWT.V_SCROLL);
										channelTabItem2.setControl(scollableDeviceComposite2);
										FillLayout composite2Layout = new FillLayout();
										scollableDeviceComposite2.setLayout(composite2Layout);
										{
											baseDeviceSetupComposite2 = new Composite(scollableDeviceComposite2, SWT.NONE);
											FillLayout composite1Layout = new FillLayout(SWT.VERTICAL);
											baseDeviceSetupComposite2.setLayout(composite1Layout);
											new ParameterHeaderControl(baseDeviceSetupComposite2, "Parametername    ", 150, "Wert", 50, "Wertebereich, Einheit", 150, 20);
											new ParameterConfigControl(baseDeviceSetupComposite2, channelValues2, 0, "Finish Sound Time", "0=OFF, 1=5s, 2=15s, 3=1m, 4=ON", false, 0, 14, 1);
											new ParameterConfigControl(baseDeviceSetupComposite2, channelValues2, 1, "Finish Melody", "1 ~ 10", false, 1, 20, 1);
											new ParameterConfigControl(baseDeviceSetupComposite2, channelValues2, 2, "LCD Contrast", "1 ~ 15", false, 1, 25, 1);
											new ParameterConfigControl(baseDeviceSetupComposite2, channelValues2, 3, "Power On Display", "0=MOVE, 1=LAST", false, 0, 11, 1);
										}
										scollableDeviceComposite2.setContent(baseDeviceSetupComposite2);
										baseDeviceSetupComposite.setSize(550, 150);
										scollableDeviceComposite2.addControlListener(new ControlListener() {

											@Override
											public void controlResized(ControlEvent evt) {
												log.log(Level.FINEST, "baseDeviceSetupComposite2.controlResized, event=" + evt);
												baseDeviceSetupComposite2.setSize(scollableDeviceComposite2.getClientArea().width, 150);
												System.out.println("baseDeviceSetupComposite2 " + baseDeviceSetupComposite2.getBounds());
											}

											@Override
											public void controlMoved(ControlEvent evt) {
												log.log(Level.FINEST, "baseDeviceSetupComposite2.controlMoved, event=" + evt);
												baseDeviceSetupComposite2.setSize(scollableDeviceComposite2.getClientArea().width, 150);
											}
										});
									}
								}
								channelBaseDataTabFolder.setSelection(0);
							}
						}
						{
							memorySetupTabItem = new CTabItem(TabFolder, SWT.NONE);
							memorySetupTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							memorySetupTabItem.setText("Batteriespeicher");
							{
								memoryBoundsComposite = new Composite(TabFolder, SWT.NONE); 
								memorySetupTabItem.setControl(memoryBoundsComposite);
								memoryBoundsComposite.setLayout(new FormLayout());
								{
									memorySelectComposite = new Composite(memoryBoundsComposite, SWT.NONE);
									FormData memorySelectLData = new FormData();
									memorySelectLData.height = 55;
									memorySelectLData.left =  new FormAttachment(0, 1000, 0);
									memorySelectLData.right =  new FormAttachment(1000, 1000, 0);
									memorySelectLData.top =  new FormAttachment(0, 1000, 0);
									memorySelectComposite.setLayoutData(memorySelectLData);
									RowLayout composite2Layout = new RowLayout(SWT.HORIZONTAL);
									memorySelectComposite.setLayout(composite2Layout);
									memorySelectComposite.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
									{
										memorySelectLabel = new CLabel(memorySelectComposite, SWT.RIGHT);
										RowData memorySelectLabelLData = new RowData();
										memorySelectLabelLData.width = 120;
										memorySelectLabelLData.height = 20;
										memorySelectLabel.setLayoutData(memorySelectLabelLData);
										memorySelectLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										memorySelectLabel.setText("Speichernummer :");
										memorySelectLabel.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
									}
									{
										String[] memoryNames = new String[60];
										for (int i = 1; i <= 60; i++) {
											memoryNames[i-1] = String.format("%02d", i);
										}
										memoryCombo = new CCombo(memorySelectComposite, SWT.BORDER);
										memoryCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										memoryCombo.setItems(memoryNames);
										memoryCombo.setVisibleItemCount(10);
										RowData memoryComboLData = new RowData();
										memoryComboLData.width = 40;
										memoryComboLData.height = 16;
										memoryCombo.setLayoutData(memoryComboLData);
										memoryCombo.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												log.log(Level.OFF, "memoryCombo.widgetSelected, event=" + evt);
												try {
													if(lastMemorySelectionIndex != memoryCombo.getSelectionIndex()) {
														//TODO write memory if changed
														int memoryNumber = memoryCombo.getSelectionIndex();
														memoryNameText.setText(serialPort.readMemoryName(memoryNumber+1));
														memoryValues = serialPort.readMemorySetup(memoryNumber+1);
														System.out.println(memoryValues[0]);
														cellTypeCombo.select(memoryValues[0]);
														lastMemorySelectionIndex = memoryNumber;
													}
												}
												catch (IOException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												}
												catch (TimeOutException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												}
											}
										});
									}
									{
										memoryNameLable = new CLabel(memorySelectComposite, SWT.RIGHT);
										RowData memoryNameLableLData = new RowData();
										memoryNameLableLData.width = 110;
										memoryNameLableLData.height = 20;
										memoryNameLable.setLayoutData(memoryNameLableLData);
										memoryNameLable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										memoryNameLable.setText("Speichername :");
										memoryNameLable.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
									}
									{
										memoryNameText = new Text(memorySelectComposite, SWT.SINGLE | SWT.BORDER);
										memoryNameText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										RowData memoryNameTextLData = new RowData();
										memoryNameTextLData.width = 120;
										memoryNameTextLData.height = 16;
										memoryNameText.setLayoutData(memoryNameTextLData);
										memoryNameText.addVerifyListener(new VerifyListener() {	
											@Override
											public void verifyText(VerifyEvent evt) {
												evt.doit = evt.text.length() <= 16;
											}
										});
									}
									{
										cellTypeSelectLabel = new CLabel(memorySelectComposite, SWT.RIGHT);
										RowData cellTypeSelectLabelLData = new RowData();
										cellTypeSelectLabelLData.width = 100;
										cellTypeSelectLabelLData.height = 20;
										cellTypeSelectLabel.setLayoutData(cellTypeSelectLabelLData);
										cellTypeSelectLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										cellTypeSelectLabel.setText("  Zellentype : ");
										cellTypeSelectLabel.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
									}
									{
										String[] cellTypeNames = new String[] {"NiCd", "NiMh", "LiIo", "LiPo", "LiFe", "Pb"};
										cellTypeCombo = new CCombo(memorySelectComposite, SWT.BORDER);
										cellTypeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										cellTypeCombo.setItems(cellTypeNames);
										cellTypeCombo.setVisibleItemCount(10);
										RowData cellTypeComboLData = new RowData();
										cellTypeComboLData.width = 60;
										cellTypeComboLData.height = 16;
										cellTypeCombo.setLayoutData(cellTypeComboLData);
										cellTypeCombo.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												log.log(Level.OFF, "cellTypeCombo.widgetSelected, event=" + evt);
												if(lastMemorySelectionIndex != cellTypeCombo.getSelectionIndex()) {
													memoryNameText.setText(deviceUserName);

													//TODO write memory if changed
													memoryValues[0] = cellTypeCombo.getSelectionIndex();
													//update memory parameter table to reflect not edit able parameters for selected cell type
													switch(memoryValues[0]) {
														case 0: //NiCd
														case 1: //NiMh
															memoryParameters[7] = memoryParameters[7] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 7, "Delta peak", "0 ~ 25mV", false, 0, 35, 1) : memoryParameters[7];
															memoryParameters[8] = memoryParameters[8] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 8, "Pre peak delay", "1 ~ 20min", false, 1, 30, 1) : memoryParameters[8];
															memoryParameters[9] = memoryParameters[9] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 9, "Trickle current", "0 ~ 550mA (550=OFF)", false, 0, 560, 1) : memoryParameters[9];
															memoryParameters[12] = memoryParameters[12] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 12, "Re-peak cycle", "1 ~ 5", false, 0, 15, 1) : memoryParameters[12];
															memoryParameters[14] = memoryParameters[14] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 14, "Re-peak delay", "1 ~ 30min", false, 1, 40, 1) : memoryParameters[14];
															memoryParameters[15] = memoryParameters[15] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 15, "Flat Limit check", "0=OFF, 1=ON", false, 0, 11, 1) : memoryParameters[15];
															memoryParameters[20] = memoryParameters[20] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 20, "NiMh matched voltage", "1100 ~ 1300", true, 10, 40, 10, 1000) : memoryParameters[20];
															memoryParameters[25] = memoryParameters[25] != null ? memoryParameters[25].dispose() : null;
															memorySelectHeight = 720;
															break;
														case 2: //LiIo
														case 3: //LiPo
														case 4: //LiFe
															memoryParameters[7] = memoryParameters[7] != null ? memoryParameters[7].dispose() : null;
															memoryParameters[8] = memoryParameters[8] != null ? memoryParameters[8].dispose() : null;
															memoryParameters[9] = memoryParameters[9] != null ? memoryParameters[9].dispose() : null;
															memoryParameters[12] = memoryParameters[12] != null ? memoryParameters[12].dispose() : null;
															memoryParameters[14] = memoryParameters[14] != null ? memoryParameters[14].dispose() : null;
															memoryParameters[15] = memoryParameters[15] != null ? memoryParameters[15].dispose() : null;
															memoryParameters[20] = memoryParameters[20] != null ? memoryParameters[20].dispose() : null;
															memoryParameters[25] = memoryParameters[25] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 25, "STORE Volts", "1000 ~ 4000", true, 0, 310, 10, 1000) : memoryParameters[25];
															memorySelectHeight = 720 - 30*6;
															break;
														case 5: //Pb
															memoryParameters[7] = memoryParameters[7] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 7, "Delta peak", "0 ~ 25mV", false, 0, 35, 1) : memoryParameters[7];
															memoryParameters[8] = memoryParameters[8] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 8, "Pre peak delay", "1 ~ 20min", false, 1, 30, 1) : memoryParameters[8];
															memoryParameters[9] = memoryParameters[9] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 9, "Trickle current", "0 ~ 550mA (550=OFF)", false, 0, 560, 1) : memoryParameters[9];
															memoryParameters[12] = memoryParameters[12] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 12, "Re-peak cycle", "1 ~ 5", false, 0, 15, 1) : memoryParameters[12];
															memoryParameters[14] = memoryParameters[14] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 14, "Re-peak delay", "1 ~ 30min", false, 1, 40, 1) : memoryParameters[14];
															memoryParameters[15] = memoryParameters[15] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 15, "Flat Limit check", "0=OFF, 1=ON", false, 0, 11, 1) : memoryParameters[15];
															memoryParameters[20] = memoryParameters[20] != null ? memoryParameters[20].dispose() : null;
															memoryParameters[25] = memoryParameters[25] != null ? memoryParameters[25].dispose() : null;
															memorySelectHeight = 720 - 30;
															break;
													}
													memoryComposite.setSize(550, memorySelectHeight);
													memoryComposite.layout(true);
													lastMemorySelectionIndex = memoryValues[0];
												}
											}
										});
									}
									new ParameterHeaderControl(memorySelectComposite, "Parametername    ", 150, "Wert", 50, "Wertebereich, Einheit", 150, 20);
								}
								{
									scrolledMemoryComposite = new ScrolledComposite(memoryBoundsComposite, SWT.BORDER | SWT.V_SCROLL);
									FillLayout scrolledComposite1Layout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
									scrolledMemoryComposite.setLayout(scrolledComposite1Layout);
									FormData scrolledMemoryCompositeLData = new FormData();
									scrolledMemoryCompositeLData.left =  new FormAttachment(0, 1000, 0);
									scrolledMemoryCompositeLData.right =  new FormAttachment(1000, 1000, 0);
									scrolledMemoryCompositeLData.bottom =  new FormAttachment(1000, 1000, 0);
									scrolledMemoryCompositeLData.top =  new FormAttachment(0, 1000, 55);
									scrolledMemoryComposite.setLayoutData(scrolledMemoryCompositeLData);
									FillLayout scrolledMemoryCompositeLayout = new FillLayout();
									scrolledMemoryComposite.setLayout(scrolledMemoryCompositeLayout);
									{
										memoryComposite = new Composite(scrolledMemoryComposite, SWT.NONE);
										FillLayout memoryCompositeLayout = new FillLayout(SWT.VERTICAL);
										memoryComposite.setLayout(memoryCompositeLayout);
	
										memoryParameters[1] = new ParameterConfigControl(memoryComposite, memoryValues, 1, "Number cells", "1 ~ 18", false, 1, 28, 1);
										memoryParameters[2] = new ParameterConfigControl(memoryComposite, memoryValues, 2, "Capacity", "100 ~ 50000", true, 1, 510, 100);
										memoryParameters[3] = new ParameterConfigControl(memoryComposite, memoryValues, 3, "Battery year", "0 ~ 99", false, 0, 109, 1);
										memoryParameters[4] = new ParameterConfigControl(memoryComposite, memoryValues, 4, "Battery month", "1 ~ 12", false, 1, 22, 1);
										memoryParameters[5] = new ParameterConfigControl(memoryComposite, memoryValues, 5, "Battery day", "1 ~ 31", false, 1, 41, 1);
										memoryParameters[6] = new ParameterConfigControl(memoryComposite, memoryValues, 6, "Charge current", "100 ~ 20000", true, 1, 210, 100);
										memoryParameters[10] = new ParameterConfigControl(memoryComposite, memoryValues, 10, "Charge cut-off temperature", "10 ~ 80°C , 50 ~ 176°F", false, 10, 186, 1);
										memoryParameters[11] = new ParameterConfigControl(memoryComposite, memoryValues, 11, "Charge max capacity", "10 ~ 155% (Cd,Mh 155=OFF, Li 125=OFF)", true, 10, 165, 1);
										memoryParameters[13] = new ParameterConfigControl(memoryComposite, memoryValues, 13, "Charge voltage", "1000 ~ 4300", true, 0, 340, 10, 1000);
										memoryParameters[16] = new ParameterConfigControl(memoryComposite, memoryValues, 16, "Discharge current", "100 ~ 10000", true, 1, 110, 100);
										memoryParameters[17] = new ParameterConfigControl(memoryComposite, memoryValues, 17, "Discharge cut-off voltage", "100 ~ 4200", true, 10, 430, 10);
										memoryParameters[18] = new ParameterConfigControl(memoryComposite, memoryValues, 18, "Discharge cut-off temperature", "10 ~ 80°C , 50 ~ 176°F", false, 10, 186, 1);
										memoryParameters[19] = new ParameterConfigControl(memoryComposite, memoryValues, 19, "Discharge max capacity", "10 ~ 105%(105=OFF)", false, 10, 115, 1);
										memoryParameters[21] = new ParameterConfigControl(memoryComposite, memoryValues, 21, "Cycle direction", "0=C->D, 1=D->C, 2=D->C->D", false, 0, 12, 1);
										memoryParameters[22] = new ParameterConfigControl(memoryComposite, memoryValues, 22, "Cycle time", "1 ~ 10", false, 1, 20, 1);
										memoryParameters[23] = new ParameterConfigControl(memoryComposite, memoryValues, 23, "D->Charge delay", "1 ~ 30min", false, 1, 40, 1);
										memoryParameters[24] = new ParameterConfigControl(memoryComposite, memoryValues, 24, "C->Discharge delay", "1 ~ 30min", false, 1, 40, 1);
										memoryParameters[25] = new ParameterConfigControl(memoryComposite, memoryValues, 25, "STORE Volts", "1000 ~ 4000", true, 0, 310, 10, 1000);
										memoryParameters[7] = new ParameterConfigControl(memoryComposite, memoryValues, 7, "Delta peak", "0 ~ 25mV", false, 0, 35, 1);
										memoryParameters[8] = new ParameterConfigControl(memoryComposite, memoryValues, 8, "Pre peak delay", "1 ~ 20min", false, 1, 30, 1);
										memoryParameters[9] = new ParameterConfigControl(memoryComposite, memoryValues, 9, "Trickle current", "0 ~ 550mA (550=OFF)", false, 0, 560, 1);
										memoryParameters[12] = new ParameterConfigControl(memoryComposite, memoryValues, 12, "Re-peak cycle", "1 ~ 5", false, 0, 15, 1);
										memoryParameters[14] = new ParameterConfigControl(memoryComposite, memoryValues, 14, "Re-peak delay", "1 ~ 30min", false, 1, 40, 1);
										memoryParameters[15] = new ParameterConfigControl(memoryComposite, memoryValues, 15, "Flat Limit check", "0=OFF, 1=ON", false, 0, 11, 1);
										memoryParameters[20] = new ParameterConfigControl(memoryComposite, memoryValues, 20, "NiMh matched voltage", "1100 ~ 1300", true, 10, 40, 10, 1000);
									}								
									scrolledMemoryComposite.setContent(memoryComposite);
									memoryComposite.setSize(550, memorySelectHeight);
									scrolledMemoryComposite.addControlListener(new ControlListener() {
										@Override
										public void controlResized(ControlEvent evt) {
											log.log(Level.FINEST, "scrolledMemoryComposite.controlResized, event=" + evt);
											memoryComposite.setSize(scrolledMemoryComposite.getClientArea().width, memorySelectHeight);
											System.out.println("memoryComposite " + memoryComposite.getBounds());
										}
										@Override
										public void controlMoved(ControlEvent evt) {
											log.log(Level.FINEST, "scrolledMemoryComposite.controlMoved, event=" + evt);
											memoryComposite.setSize(scrolledMemoryComposite.getClientArea().width, memorySelectHeight);
										}
									});
								}
							}
						}
						FormData TabFolderLData = new FormData();
						TabFolderLData.width = 549;
						TabFolderLData.height = 466;
						TabFolderLData.left =  new FormAttachment(0, 1000, 0);
						TabFolderLData.top =  new FormAttachment(0, 1000, 37);
						TabFolderLData.right =  new FormAttachment(1000, 1000, 2);
						TabFolderLData.bottom =  new FormAttachment(1000, 1000, -44);
						TabFolder.setLayoutData(TabFolderLData);
						TabFolder.setSelection(0);
					}
					{
						restoreButton = new Button(boundsComposite, SWT.PUSH | SWT.CENTER);
						FormData restoreButtonLData = new FormData();
						restoreButtonLData.width = 118;
						restoreButtonLData.height = 27;
						restoreButtonLData.left =  new FormAttachment(0, 1000, 165);
						restoreButtonLData.bottom =  new FormAttachment(1000, 1000, -4);
						restoreButton.setLayoutData(restoreButtonLData);
						restoreButton.setText("restore");
						restoreButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "restoreButton.widgetSelected, event="+evt);
								//TODO add your code for restoreButton.widgetSelected
							}
						});
					}
					{
						backupButton = new Button(boundsComposite, SWT.PUSH | SWT.CENTER);
						FormData backupButtonLData = new FormData();
						backupButtonLData.width = 118;
						backupButtonLData.height = 27;
						backupButtonLData.left =  new FormAttachment(0, 1000, 29);
						backupButtonLData.bottom =  new FormAttachment(1000, 1000, -4);
						backupButton.setLayoutData(backupButtonLData);
						backupButton.setText("backup");
						backupButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "backupButton.widgetSelected, event="+evt);
								//TODO add your code for backupButton.widgetSelected
							}
						});
					}
					{
						closeButton = new Button(boundsComposite, SWT.PUSH | SWT.CENTER);
						FormData writeButtonLData = new FormData();
						writeButtonLData.width = 118;
						writeButtonLData.height = 27;
						writeButtonLData.bottom =  new FormAttachment(1000, 1000, -4);
						writeButtonLData.right =  new FormAttachment(1000, 1000, -21);
						closeButton.setLayoutData(writeButtonLData);
						closeButton.setText("close");
						closeButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "closeButton.widgetSelected, event="+evt);
								dialogShell.dispose();
							}
						});
					}
					{
						helpButton = new Button(boundsComposite, SWT.PUSH | SWT.CENTER);
						helpButton.setText("help");
						FormData LoadButtonLData = new FormData();
						LoadButtonLData.width = 118;
						LoadButtonLData.height = 27;
						LoadButtonLData.bottom =  new FormAttachment(1000, 1000, -4);
						LoadButtonLData.right =  new FormAttachment(1000, 1000, -158);
						helpButton.setLayoutData(LoadButtonLData);
						helpButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "readButton.widgetSelected, event="+evt);
								try {
									serialPort.readSetup();
								}
								catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						});
					}
					boundsComposite.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							log.log(Level.FINER, "boundsComposite.paintControl() " + evt); //$NON-NLS-1$
						}
					});
				} // end boundsComposite
				dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 300, 100));
				dialogShell.open();
				memoryCombo.select(0);
				cellTypeCombo.select(0);
			}
			else {
				dialogShell.setVisible(true);
				dialogShell.setActive();
			}
			Display display = dialogShell.getDisplay();
			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		finally {
			if (serialPort != null && serialPort.isConnected()) {
				try {
					serialPort.write(UltramatSerialPort.RESET_END);
				}
				catch (IOException e) {
					// ignore
				}
				finally {
					serialPort.close();
				}
			}
		}
	}

	public Shell getDialogShell() {
		return dialogShell;
	}
	
//	public void resetButtons() {
//		if (!isDisposed()) {
//			startCollectDataButton.setEnabled(true);
//			stopCollectDataButton.setEnabled(false);
//		}
//	}

	
}
