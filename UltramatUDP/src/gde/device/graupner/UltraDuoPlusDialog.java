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
import gde.device.DeviceConfiguration;
import gde.device.DeviceDialog;
import gde.device.graupner.UltraDuoPlusType.ChannelData1;
import gde.device.graupner.UltraDuoPlusType.ChannelData2;
import gde.device.graupner.UltraDuoPlusType.MotorRunData;
import gde.device.graupner.UltraDuoPlusType.TireHeaterData;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.log.LogFormatter;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.ParameterConfigControl;
import gde.ui.ParameterHeaderControl;
import gde.ui.SWTResourceManager;
import gde.utils.FileUtils;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Graupner Ultra Duo Plus setup dialog
 * @author Winfried Brügmann
 */
public class UltraDuoPlusDialog extends DeviceDialog {
	private static final String	ULTRA_DUO_PLUS_XSD	= "UltraDuoPlus_V02.xsd";
	final static Logger						log									= Logger.getLogger(UltraDuoPlusDialog.class.getName());
	static final String						DEVICE_JAR_NAME			= "UltramatUDP";

	private Text userNameText;
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
	private Button copyButton;
	private CTabItem setupTabItem;
	private CTabItem memorySetupTabItem;
	private CTabFolder tabFolder;
	private Composite memoryComposite;
	private Composite memoryBoundsComposite, memorySelectComposite;
	private CLabel memorySelectLabel;
	private ScrolledComposite scrolledMemoryComposite;
	private CCombo memoryCombo;

	Composite											boundsComposite;

	final Ultramat								device;						// get device specific things, get serial port, ...
	final UltramatSerialPort			serialPort;				// open/close port execute getData()....
	final Channels								channels;					// interaction with channels, source of all records
	final Settings								settings;					// application configuration settings
	final Listener 								memoryParameterChangeListener;
	
	final static 										String[] cellTypeNames = new String[] {"NiCd", "NiMh", "LiIo", "LiPo", "LiFe", "Pb"};
	final static String[] soundTime = new String[] {"OFF", "5sec", "15sec", "1min", "ON"};
	final static String[] powerOnDisplayType = new String[] {"MOVE", "LAST"};
	final static String[] cycleDirectionTypes = new String[] {"C->D", "D->C", "D->C->D"}; 
	final static String[] offOnType = new String[] {"OFF", "ON"};
	final static String[] onOffType = new String[] {"ON", "OFF"};
	final static String[] temperatureDegreeType  = new String[] {"°C", "°F"};
	final static String[] languageTypes = new String[] {"En", "De", "Fr", "It"}; 
	final static String[] diableEnableType = new String[] {"DISABLE", "ENABLE"}; 
	final static String[] hourFormatType = new String[] {"12H", "24H"}; 


  Schema schema;
	JAXBContext jc;
	UltraDuoPlusType ultraDuoPlusSetup;
	UltraDuoPlusSychronizer synchronizerRead, synchronizerWrite;

	String deviceIdentifierName = "no_name";

	int[] channelValues1 = new int[UltramatSerialPort.SIZE_CHANNEL_1_SETUP];
	int[] channelValues2 = new int[UltramatSerialPort.SIZE_CHANNEL_2_SETUP];
	ParameterConfigControl[] channelParameters = new ParameterConfigControl[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + UltramatSerialPort.SIZE_CHANNEL_2_SETUP];
	
	String[] memoryNames = new String[60];
	int[] memoryValues = new int[UltramatSerialPort.SIZE_MEMORY_SETUP];
	ParameterConfigControl[] memoryParameters = new ParameterConfigControl[UltramatSerialPort.SIZE_MEMORY_SETUP];
	int lastMemorySelectionIndex = -1;
	int lastCellSelectionIndex = -1;
	int memorySelectHeight = 26*26;
	
	/**
	 * method to test this class
	 * @param args
	 */
	public static void main(String[] args) {
		Handler ch = new ConsoleHandler();
		LogFormatter lf = new LogFormatter();
		ch.setFormatter(lf);
		ch.setLevel(Level.ALL);
		Logger.getLogger(GDE.STRING_EMPTY).addHandler(ch);
		Logger.getLogger(GDE.STRING_EMPTY).setLevel(Level.TIME);

		String basePath = Settings.getInstance().getApplHomePath(); //$NON-NLS-1$
		UltramatSerialPort serialPort = null;
		
		try {
      Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new StreamSource(UltraDuoPlusDialog.class.getClassLoader().getResourceAsStream("resource/" + ULTRA_DUO_PLUS_XSD))); //$NON-NLS-1$
			JAXBContext jc = JAXBContext.newInstance("gde.device.graupner"); //$NON-NLS-1$
			
			UltraDuoPlusType ultraDuoPlusSetup = new ObjectFactory().createUltraDuoPlusType();
			String deviceIdentifierName = GDE.STRING_EMPTY;
			log.log(Level.TIME, "XSD init time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$
			
			serialPort = new UltramatSerialPort(new DeviceConfiguration(basePath + "/Devices/UltraDuoPlus60.xml"));
			if (!serialPort.isConnected()) {
				try {
					long time	= new Date().getTime();
					serialPort.open();
					serialPort.write(UltramatSerialPort.RESET_BEGIN);
									
					deviceIdentifierName = serialPort.readDeviceUserName(); //read the device identifier name to read available cache file
					
					try {
						Unmarshaller unmarshaller = jc.createUnmarshaller();
						unmarshaller.setSchema(schema);
						ultraDuoPlusSetup = (UltraDuoPlusType) unmarshaller.unmarshal(new File(basePath + "/UltraDuoPlus_" + deviceIdentifierName.replace(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR) + GDE.FILE_ENDING_DOT_XML));
						log.log(Level.TIME, "read memory setup XML time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - time))); //$NON-NLS-1$ //$NON-NLS-2$
					}
					catch (Exception e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						if (e.getCause() instanceof FileNotFoundException) {
							ultraDuoPlusSetup = new ObjectFactory().createUltraDuoPlusType();
							List<MemoryType> cellMemories = ultraDuoPlusSetup.getMemory();
							if (cellMemories.size() < 60) { // initially create only base setup data
								for (int i = 0; i < 60; i++) {
									MemoryType cellMemory = new ObjectFactory().createMemoryType();
									cellMemory.setSetupData(new ObjectFactory().createMemoryTypeSetupData());
									cellMemories.add(cellMemory);
								}
							}
						}
						else 
							throw e;
					}

					ultraDuoPlusSetup.setIdentifierName(deviceIdentifierName); 
					
					Thread readSynchronizer = new UltraDuoPlusSychronizer(null, serialPort, ultraDuoPlusSetup, UltraDuoPlusSychronizer.SYNC_TYPE.READ);
					readSynchronizer.start();
					readSynchronizer.join();
				}
				catch (Exception e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					return;
				}
				finally {
					if (serialPort.isConnected()) {
						serialPort.write(UltramatSerialPort.RESET_END);
						serialPort.close();
					}

				}
			}

			if (ultraDuoPlusSetup != null) {
				//remove synchronized flag
				ultraDuoPlusSetup.getChannelData1().synced = null;
				ultraDuoPlusSetup.getChannelData2().synced = null;
				Iterator<MemoryType> iterator = ultraDuoPlusSetup.getMemory().iterator();
				for (int i = 1; iterator.hasNext(); ++i) {
					MemoryType cellMemory = iterator.next();
					cellMemory.synced = null;
					cellMemory.getSetupData().synced = null;
					if (cellMemory.getStepChargeData() != null) cellMemory.getStepChargeData().synced = null;
					if (cellMemory.getTraceData() != null) cellMemory.getTraceData().synced = null;
					if (cellMemory.getCycleData() != null) cellMemory.getCycleData().synced = null;
				}
				Iterator<TireHeaterData> tireIterator = ultraDuoPlusSetup.getTireHeaterData().iterator();
				for (int i = 1; tireIterator.hasNext(); ++i) {
					tireIterator.next().synced = null;
				}
				Iterator<MotorRunData> motorIterator = ultraDuoPlusSetup.getMotorRunData().iterator();
				for (int i = 1; motorIterator.hasNext(); ++i) {
					motorIterator.next().synced = null;
				}
				
				// store back manipulated XML
				Marshaller marshaller = jc.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.valueOf(true));
				marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, ULTRA_DUO_PLUS_XSD);
				marshaller.marshal(ultraDuoPlusSetup, new FileOutputStream(basePath + "/UltraDuoPlus_" + deviceIdentifierName.replace(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR) + GDE.FILE_ENDING_DOT_XML)); //$NON-NLS-1$
				log.log(Level.TIME, "write memory setup XML time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$
			}
	    
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (Throwable t) {
			log.log(Level.SEVERE, t.getMessage(), t);
		}
	}


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
		
		memoryParameterChangeListener = new Listener() {
			@Override
			public void handleEvent(Event evt) {
				if (lastMemorySelectionIndex > 0 && lastMemorySelectionIndex < 60) {
					log.log(Level.FINEST, "memoryComposite.handleEvent, (" + lastMemorySelectionIndex + ")" + ultraDuoPlusSetup.getMemory().get(lastMemorySelectionIndex).getName() + " memoryValues[" + evt.index + "] changed");
					ultraDuoPlusSetup.getMemory().get(lastMemorySelectionIndex).getSetupData().setChanged(true);
					if (log.isLoggable(Level.FINEST)) {
						StringBuffer sb = new StringBuffer();
						for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
							sb.append(memoryValues[i]).append("[").append(i).append("], ");
						}
						log.log(Level.FINEST, sb.toString());
					}
				}
			}
		};
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
					deviceIdentifierName = serialPort.readDeviceUserName();
					
					jc = JAXBContext.newInstance("gde.device.graupner"); //$NON-NLS-1$
			    schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new StreamSource(UltraDuoPlusDialog.class.getClassLoader().getResourceAsStream("resource/" + ULTRA_DUO_PLUS_XSD))); //$NON-NLS-1$

					try {
						Unmarshaller unmarshaller = jc.createUnmarshaller();
						unmarshaller.setSchema(schema);
						ultraDuoPlusSetup = (UltraDuoPlusType) unmarshaller.unmarshal(new File(settings.getApplHomePath() + "/UltraDuoPlus_" + deviceIdentifierName.replace(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR) + GDE.FILE_ENDING_DOT_XML));
					}
					catch (UnmarshalException e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						createUltraDuoPlusSetup(deviceIdentifierName);
					}
					catch (Exception e) {
						if (e.getCause() instanceof FileNotFoundException) {
							createUltraDuoPlusSetup(deviceIdentifierName);
						}
						else 
							throw e;
					}

					synchronizerRead = new UltraDuoPlusSychronizer(this, serialPort, ultraDuoPlusSetup, UltraDuoPlusSychronizer.SYNC_TYPE.READ);
					synchronizerRead.start();
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
				dialogShell.setSize(625, 650);
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
								synchronizerRead.join();
								try {
									synchronizerWrite = new UltraDuoPlusSychronizer(UltraDuoPlusDialog.this, serialPort, ultraDuoPlusSetup, UltraDuoPlusSychronizer.SYNC_TYPE.WRITE);
									synchronizerWrite.start();
									synchronizerWrite.join();
								}
								catch (Exception e) {
									e.printStackTrace();
								}
								serialPort.write(UltramatSerialPort.RESET_END);
								saveConfigUDP(settings.getApplHomePath() + "/UltraDuoPlus_" + deviceIdentifierName.replace(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR) + GDE.FILE_ENDING_DOT_XML);
							}
							catch (Exception e) {
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
						userLabel = new CLabel(boundsComposite, SWT.RIGHT);
						userLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						userLabel.setText("Geräte-/Benutzer-Name  :");
						FormData userLabelLData = new FormData();
						userLabelLData.left =  new FormAttachment(0, 1000, 12);
						userLabelLData.top =  new FormAttachment(0, 1000, 7);
						userLabelLData.width = 280;
						userLabelLData.height = 20;
						userLabel.setLayoutData(userLabelLData);
					}
					{
						userNameText = new Text(boundsComposite, SWT.SINGLE | SWT.BORDER);
						userNameText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						userNameText.setText(deviceIdentifierName);
						FormData userNameTextLData = new FormData();
						userNameTextLData.width = 120;
						userNameTextLData.height = 16;
						userNameTextLData.left =  new FormAttachment(0, 1000, 305);
						userNameTextLData.top =  new FormAttachment(0, 1000, 7);
						userNameText.setLayoutData(userNameTextLData);
						userNameText.addVerifyListener(new VerifyListener() {	
							@Override
							public void verifyText(VerifyEvent evt) {
								log.log(Level.FINEST, "evt.doit = " + (evt.text.length() <= 16)); //$NON-NLS-1$
								evt.doit = evt.text.length() <= 16;
							}
						});
						userNameText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								log.log(java.util.logging.Level.FINEST, "text.keyReleased, event=" + evt); //$NON-NLS-1$
								File oldConfigDataFile = new File(settings.getApplHomePath() + "/UltraDuoPlus_" + deviceIdentifierName.replace(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR) + GDE.FILE_ENDING_DOT_XML);
								if (oldConfigDataFile.exists()) oldConfigDataFile.delete();
								deviceIdentifierName = (userNameText.getText() + "                ").substring(0, 16);
								ultraDuoPlusSetup.setIdentifierName(deviceIdentifierName);
								ultraDuoPlusSetup.setChanged(true);
								int position = userNameText.getCaretPosition();
								userNameText.setText(deviceIdentifierName);
								userNameText.setSelection(position);
							}
							@Override
							public void keyPressed(KeyEvent evt) {
								log.log(java.util.logging.Level.FINEST, "text.keyPressed, event=" + evt); //$NON-NLS-1$
							}
						});
					}
					{
						tabFolder = new CTabFolder(boundsComposite, SWT.BORDER);
						{
							setupTabItem = new CTabItem(tabFolder, SWT.NONE);
							setupTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							setupTabItem.setText("Geräteeinstellungen");
							{
								channelBaseDataTabFolder = new CTabFolder(tabFolder, SWT.NONE);
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
											
											new ParameterHeaderControl(baseDeviceSetupComposite, "Parametername    ", 175, "Wert", 50, "Wertebereich, Einheit", 175, 20);
											channelParameters[4] = new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 4, "Temp Scale", 175, "°C | °F", 175, temperatureDegreeType, 50, 150);
											channelParameters[5] = new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 5, "Button Sound", 175, "ON | OFF", 175, onOffType, 50, 150);
											channelParameters[6] = new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 6, "Languages", 175, "En | De | Fr | It", 175, languageTypes, 50, 150);
											//channelParameters[7] = new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 7, "PC setup", 175, "DISABLE | ENABLE", 175, diableEnableType, 50, 150);
											channelParameters[8] = new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 8, "Supply Voltage", 175, "120 ~ 150 (12.0 ~ 15.0V)", 175, true, 50, 150, 120, 150, -100);
											channelParameters[9] = new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 9, "Supply Current", 175, "50 ~ 400 (5 ~ 40A)", 175, true, 50, 150, 50, 400, -50);
											channelParameters[10] = new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 10, "Time Setup – day", 175, "1 ~ 31", 175, false, 50, 150, 1, 31);
											channelParameters[11] = new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 11, "Time Setup – month", 175, "1 ~ 12", 175, false, 50, 150, 1, 12);
											channelParameters[12] = new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 12, "Time Setup – year", 175, "0 ~ 99", 175, false, 50, 150, 2000, 2099, -2000);
											channelParameters[13] = new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 13, "Time Setup – hour", 175, "0 ~ 12", 175, false, 50, 150, 0, 12);
											channelParameters[14] = new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 14, "Time Setup – minute", 175, "0 ~ 59", 175, false, 50, 150, 0, 59);
											channelParameters[15] = new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 15, "Time Setup – Format", 175, "12H | 24H", 175, hourFormatType, 50, 150);
										}
										scollableDeviceComposite.setContent(baseDeviceSetupComposite);
										baseDeviceSetupComposite.setSize(615, 390);
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
											
											new ParameterHeaderControl(baseDeviceSetupComposite1, "Parametername    ", 175, "Wert", 50, "Wertebereich, Einheit", 175, 20);
											channelParameters[0] = new ParameterConfigControl(baseDeviceSetupComposite1, channelValues1, 0, "Finish Sound Time", 175, "OFF, 5s, 15s, 1min, ON", 175, soundTime, 50, 150);
											channelParameters[1] = new ParameterConfigControl(baseDeviceSetupComposite1, channelValues1, 1, "Finish Melody", 175, "1 ~ 10", 175, false, 50, 150, 1, 10);
											channelParameters[2] = new ParameterConfigControl(baseDeviceSetupComposite1, channelValues1, 2, "LCD Contrast", 175, "1 ~ 15", 175, false, 50, 150, 1, 15);
											channelParameters[3] = new ParameterConfigControl(baseDeviceSetupComposite1, channelValues1, 3, "Power On Display", 175, "MOVE | LAST", 175, powerOnDisplayType, 50, 150);
										}
										scollableDeviceComposite1.setContent(baseDeviceSetupComposite1);
										baseDeviceSetupComposite1.setSize(620, 150);
										scollableDeviceComposite1.addControlListener(new ControlListener() {

											@Override
											public void controlResized(ControlEvent evt) {
												log.log(Level.FINEST, "baseDeviceSetupComposite1.controlResized, event=" + evt);
												baseDeviceSetupComposite1.setSize(scollableDeviceComposite1.getClientArea().width, 150);
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
											
											new ParameterHeaderControl(baseDeviceSetupComposite2, "Parametername    ", 175, "Wert", 50, "Wertebereich, Einheit", 175, 20);
											channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + 0] = new ParameterConfigControl(baseDeviceSetupComposite2, channelValues2, 0, "Finish Sound Time", 175, "OFF, 5s, 15s, 1min, ON", 175, soundTime, 50, 150);
											channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + 1] = new ParameterConfigControl(baseDeviceSetupComposite2, channelValues2, 1, "Finish Melody", 175, "1 ~ 10", 175, false, 50, 150, 1, 10);
											channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + 2] = new ParameterConfigControl(baseDeviceSetupComposite2, channelValues2, 2, "LCD Contrast", 175, "1 ~ 15", 175, false, 50, 150, 1, 15);
											channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + 3] = new ParameterConfigControl(baseDeviceSetupComposite2, channelValues2, 3, "Power On Display", 175, "MOVE | LAST", 175, powerOnDisplayType, 50, 150);
										}
										scollableDeviceComposite2.setContent(baseDeviceSetupComposite2);
										baseDeviceSetupComposite.setSize(620, 150);
										scollableDeviceComposite2.addControlListener(new ControlListener() {

											@Override
											public void controlResized(ControlEvent evt) {
												log.log(Level.FINEST, "baseDeviceSetupComposite2.controlResized, event=" + evt);
												baseDeviceSetupComposite2.setSize(scollableDeviceComposite2.getClientArea().width, 150);
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
							memorySetupTabItem = new CTabItem(tabFolder, SWT.NONE);
							memorySetupTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							memorySetupTabItem.setText("Batteriespeicher");
							{
								memoryBoundsComposite = new Composite(tabFolder, SWT.NONE); 
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
										memorySelectLabel.setText("Nummer - Name :");
										memorySelectLabel.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
									}
									{
										updateBatterySetup(1);
										memoryCombo = new CCombo(memorySelectComposite, SWT.BORDER);
										memoryCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										memoryCombo.setItems(memoryNames);
										memoryCombo.setVisibleItemCount(10);
										RowData memoryComboLData = new RowData();
										memoryComboLData.width = 160;
										memoryComboLData.height = 16;
										memoryCombo.setLayoutData(memoryComboLData);
										memoryCombo.setToolTipText("Zur Bestätigung einer Namensänderung Datenfreigabe drücken");
										memoryCombo.select(0);
										memoryCombo.setEditable(true);
										memoryCombo.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												log.log(Level.FINEST, "memoryCombo.widgetSelected, event=" + evt);
												try {
													if(lastMemorySelectionIndex != memoryCombo.getSelectionIndex()) {
														if (lastMemorySelectionIndex >= 0 && lastMemorySelectionIndex < 60) {
															//write memory if setup data has been changed changed (update memory name executed while keyListener)
															if (ultraDuoPlusSetup.getMemory().get(lastMemorySelectionIndex).getSetupData().isChanged()) {
																ultraDuoPlusSetup.getMemory().get(lastMemorySelectionIndex).getSetupData().setValue(device.convert2String(memoryValues));
																serialPort.writeConfigData(UltramatSerialPort.WRITE_MEMORY_SETUP, ultraDuoPlusSetup.getMemory().get(lastMemorySelectionIndex).getSetupData().getValue().getBytes(),	lastMemorySelectionIndex + 1);
																ultraDuoPlusSetup.getMemory().get(lastMemorySelectionIndex).getSetupData().changed = null;
															}
															//check for copy selected
															if (copyButton.getSelection()) {
																copyButton.setSelection(false);
																if (SWT.YES == application.openYesNoMessageDialog(dialogShell, "Soll der aktuell sichtbare Batteriespeichen in den gerade angewählten kopiert werden ?")) {
																	//copy memory name and setup data of lastMemorySelectionIndex to memoryCombo.getSelectionIndex()
																	log.log(Level.OFF, "copy memory: (" + lastMemorySelectionIndex + ")" + ultraDuoPlusSetup.getMemory().get(lastMemorySelectionIndex).getName());
																	if (log.isLoggable(Level.OFF)) {
																		StringBuffer sb = new StringBuffer();
																		for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
																			sb.append(memoryValues[i]).append("[").append(i).append("], ");
																		}
																		log.log(Level.OFF, sb.toString());
																	}
																	serialPort.writeConfigData(UltramatSerialPort.WRITE_MEMORY_NAME, ultraDuoPlusSetup.getMemory().get(lastMemorySelectionIndex).getName().getBytes(),
																			memoryCombo.getSelectionIndex() + 1);
																	ultraDuoPlusSetup.getMemory().get(lastMemorySelectionIndex).changed = null;
																	serialPort.writeConfigData(UltramatSerialPort.WRITE_MEMORY_SETUP, ultraDuoPlusSetup.getMemory().get(lastMemorySelectionIndex).getSetupData().getValue().getBytes(),
																			memoryCombo.getSelectionIndex() + 1);
																	ultraDuoPlusSetup.getMemory().get(lastMemorySelectionIndex).getSetupData().changed = null;
																	memoryNames[memoryCombo.getSelectionIndex()] = String.format("%02d - %s", lastMemorySelectionIndex + 1, ultraDuoPlusSetup.getMemory().get(lastMemorySelectionIndex)
																			.getName());
																	memoryCombo.setItems(memoryNames);
																}
															}
														}
														lastMemorySelectionIndex = memoryCombo.getSelectionIndex();
														updateBatterySetup(lastMemorySelectionIndex);
													}
												}
												catch (Exception e) {
													log.log(Level.SEVERE, e.getMessage(), e);
												}
											}
										});
										memoryCombo.addKeyListener(new KeyAdapter() {
											@Override
											public void keyReleased(KeyEvent evt) {
												log.log(java.util.logging.Level.FINEST, "memoryCombo.keyReleased, event=" + evt); //$NON-NLS-1$
											}
											@Override
											public void keyPressed(KeyEvent evt) {
												log.log(java.util.logging.Level.FINEST, "memoryCombo.keyPressed, event=" + evt); //$NON-NLS-1$
												if(evt.character == SWT.CR) {
													try {
														memoryNames[lastMemorySelectionIndex] = String.format("%02d - %s", lastMemorySelectionIndex + 1, (memoryCombo.getText() + "                ").substring(5, 16+5));
														ultraDuoPlusSetup.getMemory().get(lastMemorySelectionIndex).setName(memoryNames[lastMemorySelectionIndex].substring(5));
														serialPort.writeConfigData(UltramatSerialPort.WRITE_MEMORY_NAME, memoryNames[lastMemorySelectionIndex].substring(5).getBytes(), lastMemorySelectionIndex+1);
														memoryCombo.setItems(memoryNames);
													}
													catch (Exception e) {
														application.openMessageDialog(dialogShell, Messages.getString(gde.messages.MessageIds.GDE_MSGE0007, new String[] {e.getMessage()}));
													}													
												}
											}
										});
									}
									{
										CLabel filler = new CLabel(memorySelectComposite, SWT.RIGHT);
										filler.setLayoutData(new RowData(100, 20));
										filler.setText("<---------   ");
										filler.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
									}
									{
										copyButton = new Button(memorySelectComposite, SWT.CHECK | SWT.LEFT);
										RowData cellTypeSelectLabelLData = new RowData();
										cellTypeSelectLabelLData.width = 150;
										cellTypeSelectLabelLData.height = 20;
										copyButton.setLayoutData(cellTypeSelectLabelLData);
										copyButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										copyButton.setText(" Copy to next selection");
										copyButton.setToolTipText("Copy actual batterie memory content to next selected batterie memory");
										copyButton.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
									}
									new ParameterHeaderControl(memorySelectComposite, "Parametername    ", 175, "Wert", 50, "Wertebereich, Einheit           ", 175, 20);
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
												
										memoryParameters[0] = new ParameterConfigControl(memoryComposite, memoryValues, 0, "Cell Type", 175, "NiCd,NiMh,LiIo,LiPo,LiFe,Pb", 175, cellTypeNames, 50, 150);
										memoryParameters[1] = new ParameterConfigControl(memoryComposite, memoryValues, 1, "Number cells", 175, "1 ~ 18", 175, false, 50, 150, 1, 18);
										memoryParameters[2] = new ParameterConfigControl(memoryComposite, memoryValues, 2, "Capacity", 175, "100 ~ 50000 mAh", 175, true, 50, 150, 100, 50000, -100);
										memoryParameters[3] = new ParameterConfigControl(memoryComposite, memoryValues, 3, "Battery year", 175, "0 ~ 99", 175, false, 50, 150, 2000, 2099, -2000);
										memoryParameters[4] = new ParameterConfigControl(memoryComposite, memoryValues, 4, "Battery month", 175, "1 ~ 12", 175, false, 50, 150, 1, 12);
										memoryParameters[5] = new ParameterConfigControl(memoryComposite, memoryValues, 5, "Battery day", 175, "1 ~ 31", 175, false, 50, 150, 1, 31);
										memoryParameters[6] = new ParameterConfigControl(memoryComposite, memoryValues, 6, "Charge current", 175, "100 ~ 20000 mA", 175, true, 50, 150, 100, 20000, -100);
										memoryParameters[10] = new ParameterConfigControl(memoryComposite, memoryValues, 10, "Charge cut-off temperature", 175, "10 ~ 80°C , 50 ~ 176°F", 175, false, 50, 150, 10, 176);
										memoryParameters[11] = new ParameterConfigControl(memoryComposite, memoryValues, 11, "Charge max capacity", 175, "10 ~ 155% (Cd,Mh 155=OFF, Li 125=OFF)", 175, true, 50, 150, 10, 155);
										memoryParameters[12] = new ParameterConfigControl(memoryComposite, memoryValues, 12, "Charge safety timer", 175, "10 ~ 905min (905=off)", 175, false, 50, 150, 10, 905);
										memoryParameters[14] = new ParameterConfigControl(memoryComposite, memoryValues, 14, "Charge voltage", 175, "1000 ~ 4300 mV", 175, true, 50, 150, 1000, 4300, -1000);
										memoryParameters[17] = new ParameterConfigControl(memoryComposite, memoryValues, 17, "Discharge current", 175, "100 ~ 10000 mA", 175, true, 50, 150, 100, 10000, -100);
										memoryParameters[18] = new ParameterConfigControl(memoryComposite, memoryValues, 18, "Discharge cut-off voltage", 175, "100 ~ 4200 mV", 175, true, 50, 150, 100, 4200, -100);
										memoryParameters[19] = new ParameterConfigControl(memoryComposite, memoryValues, 19, "Discharge cut-off temperature", 175, "10 ~ 80°C , 50 ~ 176°F", 175, false, 50, 150, 10, 176);
										memoryParameters[20] = new ParameterConfigControl(memoryComposite, memoryValues, 20, "Discharge max capacity", 175, "10 ~ 105%(105=OFF)", 175, false, 50, 150, 10, 105, -10);
										memoryParameters[22] = new ParameterConfigControl(memoryComposite, memoryValues, 22, "Cycle direction", 175, "C->D | D->C | D->C->D", 175, cycleDirectionTypes, 50, 150);
										memoryParameters[23] = new ParameterConfigControl(memoryComposite, memoryValues, 23, "Cycle count", 175, "1 ~ 10", 175, false, 50, 150, 1, 10);
										memoryParameters[24] = new ParameterConfigControl(memoryComposite, memoryValues, 24, "D->Charge delay", 175, "1 ~ 30min", 175, false, 50, 150, 1, 30);
										memoryParameters[25] = new ParameterConfigControl(memoryComposite, memoryValues, 25, "C->Discharge delay", 175, "1 ~ 30min, 180", 175, false, 50, 150, 1, 30);
										memoryParameters[26] = new ParameterConfigControl(memoryComposite, memoryValues, 26, "STORE Volts", 175, "1000 ~ 4000 mV", 175, true, 50, 150, 1000, 4000, -1000);
										
										memoryParameters[7] = new ParameterConfigControl(memoryComposite, memoryValues, 7, "Delta peak", 175, "0 ~ 25mV", 175, false, 50, 150, 0, 25);
										memoryParameters[8] = new ParameterConfigControl(memoryComposite, memoryValues, 8, "Pre peak delay", 175, "1 ~ 20min", 175, false, 50, 150, 1, 20);
										memoryParameters[9] = new ParameterConfigControl(memoryComposite, memoryValues, 9, "Trickle current", 175, "0 ~ 550mA (550=OFF)", 175, false, 50, 150, 0, 500);
										
										memoryParameters[13] = new ParameterConfigControl(memoryComposite, memoryValues, 13, "Re-peak cycle", 175, "1 ~ 5", 175, false, 50, 150, 1, 5);
										memoryParameters[15] = new ParameterConfigControl(memoryComposite, memoryValues, 15, "Re-peak delay", 175, "1 ~ 30min", 175, false, 50, 150, 1, 30);
										memoryParameters[16] = new ParameterConfigControl(memoryComposite, memoryValues, 16, "Flat Limit check", 175, "OFF | ON", 175, offOnType, 50, 150);
										memoryParameters[21] = new ParameterConfigControl(memoryComposite, memoryValues, 21, "NiMh matched voltage", 175, "1100 ~ 1300 mV", 175, true, 50, 150, 1100, 1300, -1100);
									}								
									scrolledMemoryComposite.setContent(memoryComposite);
									memoryComposite.setSize(620, memorySelectHeight);
									scrolledMemoryComposite.addControlListener(new ControlListener() {
										@Override
										public void controlResized(ControlEvent evt) {
											log.log(Level.FINEST, "scrolledMemoryComposite.controlResized, event=" + evt);
											memoryComposite.setSize(scrolledMemoryComposite.getClientArea().width, memorySelectHeight);
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
						tabFolder.setLayoutData(TabFolderLData);
						tabFolder.setSelection(0);
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
						restoreButton.setEnabled(false);
						restoreButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "restoreButton.widgetSelected, event="+evt);
								FileDialog fileDialog = application.openFileOpenDialog(dialogShell, "Backup UltraDuoPlus Configuration", new String[]{GDE.FILE_ENDING_DOT_XML}, settings.getDataFilePath(), (device.getName() + GDE.STRING_UNDER_BAR), SWT.SINGLE);
								if (fileDialog.getFileName().length() > 4) {
									try {
										Unmarshaller unmarshaller = jc.createUnmarshaller();
										unmarshaller.setSchema(schema);
										UltraDuoPlusType tmpUltraDuoPlusSetup = (UltraDuoPlusType) unmarshaller.unmarshal(new File(fileDialog.getFilterPath()));
										copyUltraDuoPlusSetup(tmpUltraDuoPlusSetup);

//										synchronizerWrite = new UltraDuoPlusSychronizer(UltraDuoPlusDialog.this, serialPort, ultraDuoPlusSetup, UltraDuoPlusSychronizer.SYNC_TYPE.WRITE);
//										synchronizerWrite.start();
									}
									catch (Exception e) {
										log.log(Level.SEVERE, e.getMessage(), e);
										if (e.getCause() instanceof FileNotFoundException) {
											ultraDuoPlusSetup = new ObjectFactory().createUltraDuoPlusType();
											List<MemoryType> cellMemories = ultraDuoPlusSetup.getMemory();
											if (cellMemories.size() < 60) { // initially create only base setup data
												for (int i = 0; i < 60; i++) {
													MemoryType cellMemory = new ObjectFactory().createMemoryType();
													cellMemory.setSetupData(new ObjectFactory().createMemoryTypeSetupData());
													cellMemories.add(cellMemory);
												}
											}
										}
										else 
											application.openMessageDialog(dialogShell, Messages.getString(gde.messages.MessageIds.GDE_MSGE0007, new String[] {e.getMessage()}));
									}
								}
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
						backupButton.setEnabled(false);
						backupButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "backupButton.widgetSelected, event="+evt);
								FileDialog fileDialog = application.prepareFileSaveDialog(dialogShell, "Backup UltraDuoPlus Configuration", new String[]{GDE.FILE_ENDING_DOT_XML}, settings.getDataFilePath(), device.getName() + GDE.STRING_UNDER_BAR);
								String configFilePath = fileDialog.open();
								if (configFilePath != null && fileDialog.getFileName().length() > 4) {
									if (FileUtils.checkFileExist(configFilePath) && SWT.YES == application.openYesNoMessageDialog(dialogShell, Messages.getString(MessageIds.GDE_MSGI0007, new Object[] { configFilePath }))) {
										saveConfigUDP(configFilePath);
									}
								}
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
								log.log(Level.FINEST, "helpButton.widgetSelected, event="+evt);
								application.openHelpDialog(DEVICE_JAR_NAME, "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
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
				lastMemorySelectionIndex = -1;
				lastCellSelectionIndex = -1;
				updateBaseSetup();
				memoryCombo.notifyListeners(SWT.Selection, new Event());
			}
			else {
				dialogShell.setVisible(true);
				dialogShell.setActive();
				lastMemorySelectionIndex = -1;
				lastCellSelectionIndex = -1;
				updateBaseSetup();
				memoryCombo.notifyListeners(SWT.Selection, new Event());
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

	/**
	 * create minimal ultra duo plus XML data 
	 * @param useDeviceIdentifierName
	 */
	private void createUltraDuoPlusSetup(String useDeviceIdentifierName) {
		ultraDuoPlusSetup = new ObjectFactory().createUltraDuoPlusType();
		List<MemoryType> cellMemories = ultraDuoPlusSetup.getMemory();
		if (cellMemories.size() < 60) { // initially create only base setup data
			for (int i = 0; i < 60; i++) {
				MemoryType cellMemory = new ObjectFactory().createMemoryType();
				cellMemory.setSetupData(new ObjectFactory().createMemoryTypeSetupData());
				cellMemories.add(cellMemory);
			}
		}
		ultraDuoPlusSetup.setIdentifierName(useDeviceIdentifierName);
	}
	
	/**
	 * update basic setup data from cache or actual red
	 * @throws TimeOutException 
	 * @throws IOException 
	 */
	private void updateBaseSetup() throws IOException, TimeOutException {
		log.log(Level.FINEST, "entry");
		if(ultraDuoPlusSetup.getChannelData1() == null || !ultraDuoPlusSetup.getChannelData1().isSynced()) {
			ChannelData1 channelData1 = new ObjectFactory().createUltraDuoPlusTypeChannelData1();
			channelData1.setValue(serialPort.readChannelData(1));
			ultraDuoPlusSetup.setChannelData1(channelData1);
		}
		if(ultraDuoPlusSetup.getChannelData2() == null || !ultraDuoPlusSetup.getChannelData2().isSynced()) {
			ChannelData2 channelData2 = new ObjectFactory().createUltraDuoPlusTypeChannelData2();
			channelData2.setValue(serialPort.readChannelData(2));
			ultraDuoPlusSetup.setChannelData2(channelData2);
		}
		device.convert2IntArray(channelValues1, ultraDuoPlusSetup.channelData1.getValue());
		device.convert2IntArray(channelValues2, ultraDuoPlusSetup.channelData2.getValue());
		
		if (log.isLoggable(Level.FINEST)) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < UltramatSerialPort.SIZE_CHANNEL_1_SETUP; i++) {
				sb.append(channelValues1[i]).append("[").append(i).append("], ");
			}
			sb.append(" : ");
			for (int i = 0; i < UltramatSerialPort.SIZE_CHANNEL_2_SETUP; i++) {
				sb.append(channelValues2[i]).append("[").append(i).append("], ");
			}
			log.log(Level.FINEST, sb.toString());
		}
		
		for (int i = 0; i < UltramatSerialPort.SIZE_CHANNEL_1_SETUP; i++) {
			if(channelParameters[i] != null) {
				channelParameters[i].setSliderSelection(channelValues1[i]);
			}
		}
		for (int i = 0; i < UltramatSerialPort.SIZE_CHANNEL_2_SETUP; i++) {
			if(channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + i] != null) {
				channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + i].setSliderSelection(channelValues2[i]);
			}
		}
		log.log(Level.FINEST, "add handler");
		baseDeviceSetupComposite.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				log.log(Level.FINEST, "baseDeviceSetupComposite.handleEvent, channelValues1["+ evt.index + "] changed");
				ultraDuoPlusSetup.getChannelData1().setChanged(true);
			}
		});
		baseDeviceSetupComposite1.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				log.log(Level.FINEST, "baseDeviceSetupComposite1.handleEvent, channelValues1["+ evt.index + "] changed");
				ultraDuoPlusSetup.getChannelData1().setChanged(true);
			}
		});
		baseDeviceSetupComposite2.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				log.log(Level.FINEST, "baseDeviceSetupComposite2.handleEvent, channelValues2["+ evt.index + "] changed");
				ultraDuoPlusSetup.getChannelData2().setChanged(true);
			}
		});
		log.log(Level.FINEST, "exit");
	}

	/**
	 * update values by given memory number
	 * @param memoryNumber
	 * @throws IOException
	 * @throws TimeOutException
	 */
	private void updateBatterySetup(int memoryNumber) {
		log.log(Level.FINEST, "entry");
		try {			
			if (memoryComposite != null && !memoryComposite.isDisposed()) {
				log.log(Level.FINEST, "remove event handler");
				memoryComposite.removeListener(SWT.Selection, memoryParameterChangeListener);
			}

			if(ultraDuoPlusSetup.getMemory() != null) {
				for (int i = 0; i < 60; i++) {
					if (ultraDuoPlusSetup.getMemory().get(i).isSynced())
						memoryNames[i] = String.format("%02d - %s", i + 1, ultraDuoPlusSetup.getMemory().get(i).getName());
					else if (ultraDuoPlusSetup.getMemory().get(i) != null && ultraDuoPlusSetup.getMemory().get(i).getName() != null) {
						memoryNames[i] = String.format("%02d - %s", i + 1, ultraDuoPlusSetup.getMemory().get(i).getName());
					}
					else {
						WaitTimer.delay(100);
						i = i>=1 ? --i : i;
					}
					log.log(Level.FINEST, "-------> " + memoryNames[i]);
				}
			}
			else {
				for (int i = 0; i < 60; i++) {
					memoryNames[i] = String.format("%02d - NEW-BATT-NAME", i+1);
				}
				memoryNames[memoryNumber] = String.format("%02d - %s", memoryNumber+1, serialPort.readMemoryName(memoryNumber+1));
			}
			if (memoryCombo != null && !memoryCombo.isDisposed()) memoryCombo.setItems(memoryNames);
				
			if (ultraDuoPlusSetup.getMemory().get(memoryNumber).getSetupData() != null && !ultraDuoPlusSetup.getMemory().get(memoryNumber).getSetupData().isSynced()) {
				ultraDuoPlusSetup.getMemory().get(memoryNumber).getSetupData().setValue(serialPort.readMemorySetup(memoryNumber+1));
			}
			device.convert2IntArray(memoryValues, ultraDuoPlusSetup.getMemory().get(memoryNumber).getSetupData().getValue());
			if (log.isLoggable(Level.FINEST)) {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
					sb.append(memoryValues[i]).append("[").append(i).append("], ");
				}
				log.log(Level.FINEST, sb.toString());
			}
			if (memoryParameters[0] != null && !memoryParameters[0].getSlider().isDisposed())
				updateBatteryMemoryParameter(memoryValues[0]);
		
			for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
				if(memoryParameters[i] != null) {
					memoryParameters[i].setSliderSelection(memoryValues[i]);
				}
			}
			
			if (memoryComposite != null && !memoryComposite.isDisposed()) {
				log.log(Level.FINEST, "add event handler");
				memoryComposite.addListener(SWT.Selection, memoryParameterChangeListener);
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			e.printStackTrace();
		}
		log.log(Level.FINEST, "exit");
	}

	/**
	 * update the memory setup parameter list to cell type dependent
	 */
	private void updateBatteryMemoryParameter(int selectionIndex) {
		log.log(Level.FINEST, "entry");
		if(lastCellSelectionIndex != selectionIndex) {
			memoryValues[0] = selectionIndex;
			//update memory parameter table to reflect not edit able parameters for selected cell type
			switch(memoryValues[0]) {
				case 0: //NiCd
				case 1: //NiMh
					memoryParameters[7] = memoryParameters[7] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 7, "Delta peak", 175, "0 ~ 25mV", 175, false, 50, 150, 0, 25) : memoryParameters[7];
					memoryParameters[8] = memoryParameters[8] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 8, "Pre peak delay", 175, "1 ~ 20min", 175, false, 50, 150, 1, 20) : memoryParameters[8];
					memoryParameters[9] = memoryParameters[9] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 9, "Trickle current", 175, "0 ~ 550mA (550=OFF)", 175, false, 50, 150, 0, 550) : memoryParameters[9];
					memoryParameters[13] = memoryParameters[13] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 13, "Re-peak cycle", 175, "1 ~ 5", 175, false, 50, 150, 1, 5) : memoryParameters[13];
					memoryParameters[15] = memoryParameters[15] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 15, "Re-peak delay", 175, "1 ~ 30min", 175, false, 50, 150, 1, 30) : memoryParameters[15];
					memoryParameters[16] = memoryParameters[16] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 16, "Flat Limit check", 175, "OFF | ON", 175, offOnType, 50, 150) : memoryParameters[16];
					memoryParameters[21] = memoryParameters[21] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 21, "NiMh matched voltage", 175, "1100 ~ 1300 mV", 175, true, 50, 150, 1100, 1300, -1100) : memoryParameters[21];
					memoryParameters[26] = memoryParameters[26] != null ? memoryParameters[26].dispose() : null;
					memorySelectHeight = 26*26;
					break;
				case 2: //LiIo
				case 3: //LiPo
				case 4: //LiFe
					memoryParameters[7] = memoryParameters[7] != null ? memoryParameters[7].dispose() : null;
					memoryParameters[8] = memoryParameters[8] != null ? memoryParameters[8].dispose() : null;
					memoryParameters[9] = memoryParameters[9] != null ? memoryParameters[9].dispose() : null;
					memoryParameters[13] = memoryParameters[13] != null ? memoryParameters[13].dispose() : null;
					memoryParameters[15] = memoryParameters[15] != null ? memoryParameters[15].dispose() : null;
					memoryParameters[16] = memoryParameters[16] != null ? memoryParameters[16].dispose() : null;
					memoryParameters[21] = memoryParameters[21] != null ? memoryParameters[21].dispose() : null;
					memoryParameters[26] = memoryParameters[26] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 26, "STORE Volts", 175, "1000 ~ 4000", 175, true, 50, 150, 1000, 4000, -1000) : memoryParameters[26];
					memorySelectHeight = 20*26;
					break;
				case 5: //Pb
					memoryParameters[7] = memoryParameters[7] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 7, "Delta peak", 175, "0 ~ 25 mV", 175, false, 50, 150, 0, 25) : memoryParameters[7];
					memoryParameters[8] = memoryParameters[8] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 8, "Pre peak delay", 175, "1 ~ 20min", 175, false, 50, 150, 1, 20) : memoryParameters[8];
					memoryParameters[9] = memoryParameters[9] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 9, "Trickle current", 175, "0 ~ 550 mA (550=OFF)", 175, false, 50, 150, 0, 550) : memoryParameters[9];
					memoryParameters[13] = memoryParameters[13] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 13, "Re-peak cycle", 175, "1 ~ 5", 175, false, 50, 150, 1, 5) : memoryParameters[13];
					memoryParameters[15] = memoryParameters[15] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 15, "Re-peak delay", 175, "1 ~ 30min", 175, false, 50, 150, 1, 30) : memoryParameters[15];
					memoryParameters[16] = memoryParameters[16] == null ? new ParameterConfigControl(memoryComposite, memoryValues, 16, "Flat Limit check", 175, "OFF | ON", 175, offOnType, 50, 150) : memoryParameters[16];
					memoryParameters[21] = memoryParameters[21] != null ? memoryParameters[21].dispose() : null;
					memoryParameters[26] = memoryParameters[26] != null ? memoryParameters[26].dispose() : null;
					memorySelectHeight = 25*26;
					break;
			}
			memoryComposite.setSize(scrolledMemoryComposite.getClientArea().width, memorySelectHeight);
			memoryComposite.layout(true);
			lastCellSelectionIndex = memoryValues[0];
		}
		log.log(Level.FINEST, "exit");
	}
	
	/**
	 * save ultra duo plus configuration data at given full qualified file path
	 * @param fullQualifiedFilePath
	 */
	private void saveConfigUDP(String fullQualifiedFilePath) {
		try {
			if (ultraDuoPlusSetup != null) {
				ultraDuoPlusSetup.changed = null;
				//remove synchronized flag
				ultraDuoPlusSetup.getChannelData1().synced = null;
				ultraDuoPlusSetup.getChannelData2().synced = null;
				Iterator<MemoryType> iterator = ultraDuoPlusSetup.getMemory().iterator();
				for (int i = 1; iterator.hasNext(); ++i) {
					MemoryType cellMemory = iterator.next();
					cellMemory.synced = null;
					cellMemory.changed = null;
					cellMemory.getSetupData().synced = null;
					cellMemory.getSetupData().changed = null;
					if (cellMemory.getStepChargeData() != null) {
						cellMemory.getStepChargeData().synced = null;
						cellMemory.getStepChargeData().changed = null;
					}
					if (cellMemory.getTraceData() != null) {
						cellMemory.getTraceData().synced = null;
						cellMemory.getTraceData().changed = null;
					}
					if (cellMemory.getCycleData() != null) {
						cellMemory.getCycleData().synced = null;
						cellMemory.getCycleData().changed = null;
					}
				}
				Iterator<TireHeaterData> tireIterator = ultraDuoPlusSetup.getTireHeaterData().iterator();
				for (int i = 1; tireIterator.hasNext(); ++i) {
					TireHeaterData tireHeater = tireIterator.next();
					tireHeater.synced = null;
					tireHeater.changed = null;
				}
				Iterator<MotorRunData> motorIterator = ultraDuoPlusSetup.getMotorRunData().iterator();
				for (int i = 1; motorIterator.hasNext(); ++i) {
					MotorRunData motorRunData = motorIterator.next();
					motorRunData.synced = null;
					motorRunData.changed = null;
				}
				
				// store back manipulated XML
				Marshaller marshaller = jc.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.valueOf(true));
				marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, ULTRA_DUO_PLUS_XSD);
				marshaller.marshal(ultraDuoPlusSetup, new FileOutputStream(fullQualifiedFilePath));
				log.log(Level.TIME, "write memory setup XML time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			application.openMessageDialogAsync(this.dialogShell != null && !this.dialogShell.isDisposed() ? this.dialogShell : null, Messages.getString(gde.messages.MessageIds.GDE_MSGE0007, new String[] {e.getMessage()}));
		}
	}
	
	/**
	 * enable or disable backup and restore buttons, disable while synchronizing setup configuration with device
	 * @param enable
	 */
	public void setBackupRetoreButtons(final boolean enable) {
		if (!dialogShell.isDisposed()) {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					if (restoreButton != null && backupButton != null && !restoreButton.isDisposed() && !backupButton.isDisposed()) {
						restoreButton.setEnabled(enable);
						backupButton.setEnabled(enable);
					}
				}
			});
		}
	}


	/**
	 * deep copy of complete ultraDuoPlusSetup data
	 * @param tmpUltraDuoPlusSetup
	 */
	private void copyUltraDuoPlusSetup(UltraDuoPlusType tmpUltraDuoPlusSetup) {
		if(!ultraDuoPlusSetup.getIdentifierName().equals(tmpUltraDuoPlusSetup.getIdentifierName())) {
			deviceIdentifierName = (tmpUltraDuoPlusSetup.getIdentifierName() + "                ").substring(0, 16);
			ultraDuoPlusSetup.setIdentifierName(deviceIdentifierName);
			ultraDuoPlusSetup.setChanged(true);
			userNameText.setText(deviceIdentifierName);
		}
		
		//channel base setup
		if(ultraDuoPlusSetup.getChannelData1().getValue().equals(tmpUltraDuoPlusSetup.getChannelData1().getValue())) {
			ultraDuoPlusSetup.getChannelData1().setValue(tmpUltraDuoPlusSetup.getChannelData1().getValue());
			ultraDuoPlusSetup.getChannelData1().setChanged(true);
			device.convert2IntArray(channelValues1, ultraDuoPlusSetup.channelData1.getValue());
			for (int i = 0; i < UltramatSerialPort.SIZE_CHANNEL_1_SETUP; i++) {
				if(channelParameters[i] != null) {
					channelParameters[i].setSliderSelection(channelValues1[i]);
				}
			}
		}
		if(ultraDuoPlusSetup.getChannelData2().getValue().equals(tmpUltraDuoPlusSetup.getChannelData2().getValue())) {
			ultraDuoPlusSetup.getChannelData2().setValue(tmpUltraDuoPlusSetup.getChannelData2().getValue());
			ultraDuoPlusSetup.getChannelData2().setChanged(true);
			device.convert2IntArray(channelValues2, ultraDuoPlusSetup.channelData2.getValue());
			for (int i = 0; i < UltramatSerialPort.SIZE_CHANNEL_2_SETUP; i++) {
				if(channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + i] != null) {
					channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + i].setSliderSelection(channelValues2[i]);
				}
			}
		}
		
		//battery memories
		List<MemoryType> cellMemories = ultraDuoPlusSetup.getMemory();
		List<MemoryType> tmpCellMemories = tmpUltraDuoPlusSetup.getMemory();
		Iterator<MemoryType> iterator = cellMemories.iterator();
		Iterator<MemoryType> tmpIterator = tmpCellMemories.iterator();
		for (int i = 1; iterator.hasNext(); ++i) {
			MemoryType cellMemory = iterator.next();
			MemoryType tmpCellMemory = tmpIterator.next();
			//memory name
			if (!cellMemory.getName().equals(tmpCellMemory.getName())) {
				cellMemory.setName((tmpCellMemory.getName() + "                ").substring(0, 16));
				cellMemory.setChanged(true);
			}
			
			//memory setup data
			if (tmpCellMemory.getSetupData() != null && cellMemory.getSetupData() != null && !cellMemory.getSetupData().getValue().equals(tmpCellMemory.getSetupData().getValue())) {
				cellMemory.getSetupData().setValue(tmpCellMemory.getSetupData().getValue());
				cellMemory.getSetupData().setChanged(true);
			}
			//memory step charge data
			if(tmpCellMemory.getStepChargeData() == null && cellMemory.getStepChargeData() != null) {
				cellMemory.setTraceData(null);
			}
			if(tmpCellMemory.getStepChargeData() != null && cellMemory.getStepChargeData() == null) {
				cellMemory.setStepChargeData(new ObjectFactory().createMemoryTypeStepChargeData());
				cellMemory.getStepChargeData().setValue(tmpCellMemory.getStepChargeData().getValue());
				cellMemory.getStepChargeData().setChanged(true);
			}
			if (tmpCellMemory.getStepChargeData() != null && cellMemory.getStepChargeData() != null && !cellMemory.getStepChargeData().getValue().equals(tmpCellMemory.getStepChargeData().getValue())) {
				cellMemory.getStepChargeData().setValue(tmpCellMemory.getStepChargeData().getValue());
				cellMemory.getStepChargeData().setChanged(true);
			}
			
			//memory trace data
			if(tmpCellMemory.getTraceData() == null && cellMemory.getTraceData() != null) {
				cellMemory.setTraceData(null);
			}
			if(tmpCellMemory.getTraceData() != null && cellMemory.getTraceData() == null) {
				cellMemory.setTraceData(new ObjectFactory().createMemoryTypeTraceData());
				cellMemory.getTraceData().setValue(tmpCellMemory.getTraceData().getValue());
				cellMemory.getTraceData().setChanged(true);
			}
			if (tmpCellMemory.getTraceData() != null && cellMemory.getTraceData() != null && !cellMemory.getTraceData().getValue().equals(tmpCellMemory.getTraceData().getValue())) {
				cellMemory.getTraceData().setValue(tmpCellMemory.getTraceData().getValue());
				cellMemory.getTraceData().setChanged(true);
			}
			
			//memory cycle data
			if(tmpCellMemory.getCycleData() == null && cellMemory.getCycleData() != null) {
				cellMemory.setCycleData(null);
			}
			if(tmpCellMemory.getCycleData() != null && cellMemory.getCycleData() == null) {
				cellMemory.setCycleData(new ObjectFactory().createMemoryTypeCycleData());
				cellMemory.getCycleData().setValue(tmpCellMemory.getCycleData().getValue());
				cellMemory.getCycleData().setChanged(true);
			}
			if (tmpCellMemory.getCycleData() != null && cellMemory.getCycleData() != null && !cellMemory.getCycleData().getValue().equals(tmpCellMemory.getCycleData().getValue())) {
				cellMemory.getCycleData().setValue(tmpCellMemory.getCycleData().getValue());
				cellMemory.getCycleData().setChanged(true);
			}
		}
		updateBatterySetup(memoryCombo.getSelectionIndex());

		//tire heater data
		List<TireHeaterData> tireHeaters = ultraDuoPlusSetup.getTireHeaterData();
		List<TireHeaterData> tmpTireHeaters = tmpUltraDuoPlusSetup.getTireHeaterData();
		Iterator<TireHeaterData> tireIterator = tireHeaters.iterator();
		Iterator<TireHeaterData> tmpTireIterator = tmpTireHeaters.iterator();
		for (int i = 1; iterator.hasNext(); ++i) {
			TireHeaterData tireHeaterData = tireIterator.next();
			TireHeaterData tmpTireHeaterData = tmpTireIterator.next();
			
			if(tmpTireHeaterData == null && tireHeaterData != null) {
				tireHeaterData = null;
			}
			if(tmpTireHeaterData != null && tireHeaterData == null) {
				tireHeaterData = new ObjectFactory().createUltraDuoPlusTypeTireHeaterData();
				tireHeaterData.setValue(tmpTireHeaterData.getValue());
				tireHeaterData.setChanged(true);
			}
			if (tmpTireHeaterData != null && tireHeaterData != null && !tireHeaterData.getValue().equals(tmpTireHeaterData.getValue())) {
				tireHeaterData.setValue(tmpTireHeaterData.getValue());
				tireHeaterData.setChanged(true);
			}
		}

		//motor run data
		List<MotorRunData> motorRunDatas = ultraDuoPlusSetup.getMotorRunData();
		List<MotorRunData> tmpMotorRunDatas = tmpUltraDuoPlusSetup.getMotorRunData();
		Iterator<MotorRunData> motorRunIterator = motorRunDatas.iterator();
		Iterator<MotorRunData> tmpMotorRunIterator = tmpMotorRunDatas.iterator();
		for (int i = 1; iterator.hasNext(); ++i) {
			MotorRunData motorRunData = motorRunIterator.next();
			MotorRunData tmpMotorRunData = tmpMotorRunIterator.next();

			if(tmpMotorRunData == null && motorRunData != null) {
				motorRunData = null;
			}
			if(tmpMotorRunData != null && motorRunData == null) {
				motorRunData = new ObjectFactory().createUltraDuoPlusTypeMotorRunData();
				motorRunData.setValue(tmpMotorRunData.getValue());
				motorRunData.setChanged(true);
			}
			if (tmpMotorRunData != null && motorRunData != null && !motorRunData.getValue().equals(tmpMotorRunData.getValue())) {
				motorRunData.setValue(tmpMotorRunData.getValue());
				motorRunData.setChanged(true);
			}
		}
	}
}
