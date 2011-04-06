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
import gde.device.graupner.UltraDuoPlusSychronizer.SYNC_TYPE;
import gde.device.graupner.UltraDuoPlusType.ChannelData1;
import gde.device.graupner.UltraDuoPlusType.ChannelData2;
import gde.device.graupner.UltraDuoPlusType.MotorRunData;
import gde.device.graupner.UltraDuoPlusType.TireHeaterData;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.log.LogFormatter;
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
	final static Logger				log												= Logger.getLogger(UltraDuoPlusDialog.class.getName());
	static final String				DEVICE_JAR_NAME						= "UltramatUDP";																																																											//$NON-NLS-1$
	static final String				STRING_FORMAT_02d_s				= "%02d - %s";																																																												//$NON-NLS-1$
	static final String				STRING_16_BLANK						= "                ";																																																								//$NON-NLS-1$
	static final String				ULTRA_DUO_PLUS_XSD				= "UltraDuoPlus_V02.xsd";																																																						//$NON-NLS-1$
	static final String				UDP_CONFIGURATION_SUFFIX	= "/UltraDuoPlus_";																																																									//$NON-NLS-1$

	private Text							userNameText;
	private Composite					baseDeviceSetupComposite, baseDeviceSetupComposite1, baseDeviceSetupComposite2;
	private ScrolledComposite	scollableDeviceComposite, scollableDeviceComposite1, scollableDeviceComposite2;
	private CTabItem					channelTabItem2;
	private CTabItem					channelTabItem1;
	private CTabItem					baseSetupTabItem;
	private CTabFolder				channelBaseDataTabFolder;
	private CLabel						userLabel;
	private Button						restoreButton;
	private Button						backupButton;
	private Button						closeButton;
	private Button						helpButton;
	private Button						copyButton;
	private CTabItem					setupTabItem;
	private CTabItem					memorySetupTabItem;
	private CTabFolder				tabFolder;
	private Composite					memoryComposite;
	private Composite					memoryBoundsComposite, memorySelectComposite;
	private CLabel						memorySelectLabel;
	private ScrolledComposite	scrolledMemoryComposite;
	private CCombo						memoryCombo;

	Composite									boundsComposite;

	final Ultramat						device;																																																																												// get device specific things, get serial port, ...
	final UltramatSerialPort	serialPort;																																																																										// open/close port execute getData()....
	final Channels						channels;																																																																											// interaction with channels, source of all records
	final Settings						settings;																																																																											// application configuration settings
	final Listener						memoryParameterChangeListener;

	final static String[]			cellTypeNames							= Messages.getString(MessageIds.GDE_MSGT2246).split(GDE.STRING_COMMA);
	final static String[]			soundTime									= new String[] { Messages.getString(MessageIds.GDE_MSGT2241), "5sec", "15sec", "1min", Messages.getString(MessageIds.GDE_MSGT2240) };	//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	final static String[]			powerOnDisplayType				= new String[] { Messages.getString(MessageIds.GDE_MSGT2244), Messages.getString(MessageIds.GDE_MSGT2245) };
	final static String[]			cycleDirectionTypes				= Messages.getString(MessageIds.GDE_MSGT2292).split(GDE.STRING_COMMA);
	final static String[]			offOnType									= new String[] { Messages.getString(MessageIds.GDE_MSGT2241), Messages.getString(MessageIds.GDE_MSGT2240) };
	final static String[]			onOffType									= new String[] { Messages.getString(MessageIds.GDE_MSGT2240), Messages.getString(MessageIds.GDE_MSGT2241) };
	final static String[]			temperatureDegreeType			= new String[] { DeviceConfiguration.UNIT_DEGREE_CELSIUS, DeviceConfiguration.UNIT_DEGREE_FAHRENHEIT };
	final static String[]			languageTypes							= new String[] { "En", "De", "Fr", "It" };																																														//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	final static String[]			diableEnableType					= new String[] { Messages.getString(MessageIds.GDE_MSGT2243), Messages.getString(MessageIds.GDE_MSGT2242) };
	final static String[]			hourFormatType						= new String[] { "12H", "24H" };																																																			//$NON-NLS-1$ //$NON-NLS-2$

	Schema										schema;
	JAXBContext								jc;
	UltraDuoPlusType					ultraDuoPlusSetup;
	UltraDuoPlusSychronizer		synchronizerRead, synchronizerWrite;

	String										deviceIdentifierName			= "NEW USER NAME";																																																									//$NON-NLS-1$

	int[]											channelValues1						= new int[UltramatSerialPort.SIZE_CHANNEL_1_SETUP];
	int[]											channelValues2						= new int[UltramatSerialPort.SIZE_CHANNEL_2_SETUP];
	ParameterConfigControl[]	channelParameters					= new ParameterConfigControl[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + UltramatSerialPort.SIZE_CHANNEL_2_SETUP];

	String[]									memoryNames								= new String[60];
	int[]											memoryValues							= new int[UltramatSerialPort.SIZE_MEMORY_SETUP];
	ParameterConfigControl[]	memoryParameters					= new ParameterConfigControl[UltramatSerialPort.SIZE_MEMORY_SETUP];
	int												lastMemorySelectionIndex	= -1;
	int												lastCellSelectionIndex		= -1;
	int												memorySelectHeight				= 26 * 26;

	/**
	 * method to test this class
	 * @param args
	 */
	public static void main(String[] args) {
		Handler ch = new ConsoleHandler();
		LogFormatter lf = new LogFormatter();
		ch.setFormatter(lf);
		ch.setLevel(java.util.logging.Level.ALL);
		Logger.getLogger(GDE.STRING_EMPTY).addHandler(ch);
		Logger.getLogger(GDE.STRING_EMPTY).setLevel(Level.TIME);

		String basePath = Settings.getInstance().getApplHomePath();
		UltramatSerialPort serialPort = null;

		try {
			Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
					new StreamSource(UltraDuoPlusDialog.class.getClassLoader().getResourceAsStream("resource/" + UltraDuoPlusDialog.ULTRA_DUO_PLUS_XSD))); //$NON-NLS-1$
			JAXBContext jc = JAXBContext.newInstance("gde.device.graupner"); //$NON-NLS-1$

			UltraDuoPlusType ultraDuoPlusSetup = new ObjectFactory().createUltraDuoPlusType();
			String deviceIdentifierName = GDE.STRING_EMPTY;
			log.log(Level.TIME, "XSD init time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$

			serialPort = new UltramatSerialPort(new DeviceConfiguration(basePath + "/Devices/UltraDuoPlus60.xml")); //$NON-NLS-1$
			if (!serialPort.isConnected()) {
				try {
					long time = new Date().getTime();
					serialPort.open();
					serialPort.write(UltramatSerialPort.RESET_BEGIN);

					deviceIdentifierName = serialPort.readDeviceUserName(); //read the device identifier name to read available cache file

					try {
						Unmarshaller unmarshaller = jc.createUnmarshaller();
						unmarshaller.setSchema(schema);
						ultraDuoPlusSetup = (UltraDuoPlusType) unmarshaller.unmarshal(new File(basePath + UltraDuoPlusDialog.UDP_CONFIGURATION_SUFFIX
								+ deviceIdentifierName.replace(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR) + GDE.FILE_ENDING_DOT_XML));
						log.log(Level.TIME, "read memory setup XML time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - time))); //$NON-NLS-1$ //$NON-NLS-2$
					}
					catch (Exception e) {
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
				marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, UltraDuoPlusDialog.ULTRA_DUO_PLUS_XSD);
				marshaller.marshal(ultraDuoPlusSetup, new FileOutputStream(basePath + UltraDuoPlusDialog.UDP_CONFIGURATION_SUFFIX + deviceIdentifierName.replace(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR)
						+ GDE.FILE_ENDING_DOT_XML));
				log.log(Level.TIME, "write memory setup XML time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$
			}

		}
		catch (Exception e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		catch (Throwable t) {
			log.log(java.util.logging.Level.SEVERE, t.getMessage(), t);
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
		this.serialPort = useDevice.getCommunicationPort();
		this.device = useDevice;
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();

		this.memoryParameterChangeListener = new Listener() {
			public void handleEvent(Event evt) {
				if (UltraDuoPlusDialog.this.lastMemorySelectionIndex >= 0 && UltraDuoPlusDialog.this.lastMemorySelectionIndex < 60) {
					log.log(java.util.logging.Level.FINE, "memoryComposite.handleEvent, (" + UltraDuoPlusDialog.this.lastMemorySelectionIndex + GDE.STRING_RIGHT_PARENTHESIS + UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getName() + " memoryValues[" + evt.index + "] changed"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
					UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData().setChanged(true);
					if(evt.index == 0) {
						updateBatteryMemoryParameter(memoryValues[0]);
					}
					if (log.isLoggable(java.util.logging.Level.FINE)) {
						StringBuffer sb = new StringBuffer();
						for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
							sb.append(UltraDuoPlusDialog.this.memoryValues[i]).append(GDE.STRING_LEFT_BRACKET).append(i).append(GDE.STRING_RIGHT_BRACKET_COMMA);
						}
						log.log(java.util.logging.Level.FINE, sb.toString());
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
			if (this.serialPort != null && !this.serialPort.isConnected()) {
				try {
					this.serialPort.open();
					this.serialPort.write(UltramatSerialPort.RESET_BEGIN);
					this.deviceIdentifierName = this.serialPort.readDeviceUserName();

					this.jc = JAXBContext.newInstance("gde.device.graupner"); //$NON-NLS-1$
					this.schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
							new StreamSource(UltraDuoPlusDialog.class.getClassLoader().getResourceAsStream("resource/" + UltraDuoPlusDialog.ULTRA_DUO_PLUS_XSD))); //$NON-NLS-1$

					try {
						Unmarshaller unmarshaller = this.jc.createUnmarshaller();
						unmarshaller.setSchema(this.schema);
						this.ultraDuoPlusSetup = (UltraDuoPlusType) unmarshaller.unmarshal(new File(this.settings.getApplHomePath() + UltraDuoPlusDialog.UDP_CONFIGURATION_SUFFIX
								+ this.deviceIdentifierName.replace(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR) + GDE.FILE_ENDING_DOT_XML));
					}
					catch (UnmarshalException e) {
						log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
						createUltraDuoPlusSetup(this.deviceIdentifierName);
					}
					catch (Exception e) {
						if (e.getCause() instanceof FileNotFoundException) {
							createUltraDuoPlusSetup(this.deviceIdentifierName);
						}
						else
							throw e;
					}

					this.synchronizerRead = new UltraDuoPlusSychronizer(this, this.serialPort, this.ultraDuoPlusSetup, UltraDuoPlusSychronizer.SYNC_TYPE.READ);
					this.synchronizerRead.start();
				}
				catch (Exception e) {
					log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(null,
							Messages.getString(gde.messages.MessageIds.GDE_MSGE0015, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
					this.application.getDeviceSelectionDialog().open();
					return;
				}
			}
			else {
				log.log(java.util.logging.Level.SEVERE, "serial port == null"); //$NON-NLS-1$
				this.application.openMessageDialog(null, Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
				this.application.getDeviceSelectionDialog().open();
				return;
			}

			log.log(java.util.logging.Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialogShell == null || this.dialogShell.isDisposed()) {
				if (this.settings.isDeviceDialogsModal())
					this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
				else if (this.settings.isDeviceDialogsOnTop())
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
				else
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(this.dialogShell);
				this.dialogShell.setLayout(new FormLayout());
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.layout();
				this.dialogShell.pack();
				this.dialogShell.setSize(645, 650);
				this.dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						log.log(java.util.logging.Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						UltraDuoPlusDialog.this.application.openHelpDialog(UltraDuoPlusDialog.DEVICE_JAR_NAME, "HelpInfo.html"); //$NON-NLS-1$ 
					}
				});
				this.dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (UltraDuoPlusDialog.this.serialPort != null && UltraDuoPlusDialog.this.serialPort.isConnected()) {
							try {
								UltraDuoPlusDialog.this.synchronizerRead.join();
								try {
									UltraDuoPlusDialog.this.synchronizerWrite = new UltraDuoPlusSychronizer(UltraDuoPlusDialog.this, UltraDuoPlusDialog.this.serialPort, UltraDuoPlusDialog.this.ultraDuoPlusSetup,
											UltraDuoPlusSychronizer.SYNC_TYPE.WRITE);
									UltraDuoPlusDialog.this.synchronizerWrite.start();
									UltraDuoPlusDialog.this.synchronizerWrite.join();
								}
								catch (Exception e) {
									e.printStackTrace();
								}
								UltraDuoPlusDialog.this.serialPort.write(UltramatSerialPort.RESET_END);
								saveConfigUDP(UltraDuoPlusDialog.this.settings.getApplHomePath() + UltraDuoPlusDialog.UDP_CONFIGURATION_SUFFIX
										+ UltraDuoPlusDialog.this.deviceIdentifierName.replace(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR) + GDE.FILE_ENDING_DOT_XML);
							}
							catch (Exception e) {
								// ignore
							}
							finally {
								UltraDuoPlusDialog.this.serialPort.close();
								UltraDuoPlusDialog.this.application.resetShellIcon();
							}
						}
					}
				});
				{
					this.boundsComposite = new Composite(this.dialogShell, SWT.NONE);
					FormData boundsCompositeLData = new FormData();
					boundsCompositeLData.left = new FormAttachment(0, 1000, 0);
					boundsCompositeLData.right = new FormAttachment(1000, 1000, 0);
					boundsCompositeLData.top = new FormAttachment(0, 1000, 0);
					boundsCompositeLData.bottom = new FormAttachment(1000, 1000, 0);
					boundsCompositeLData.width = 553;
					boundsCompositeLData.height = 573;
					this.boundsComposite.setLayoutData(boundsCompositeLData);
					this.boundsComposite.setLayout(new FormLayout());
					{
						this.userLabel = new CLabel(this.boundsComposite, SWT.RIGHT);
						this.userLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.userLabel.setText(Messages.getString(MessageIds.GDE_MSGT2289));
						FormData userLabelLData = new FormData();
						userLabelLData.left = new FormAttachment(0, 1000, 12);
						userLabelLData.top = new FormAttachment(0, 1000, 7);
						userLabelLData.width = 280;
						userLabelLData.height = 20;
						this.userLabel.setLayoutData(userLabelLData);
					}
					{
						this.userNameText = new Text(this.boundsComposite, SWT.SINGLE | SWT.BORDER);
						this.userNameText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.userNameText.setText(this.deviceIdentifierName);
						FormData userNameTextLData = new FormData();
						userNameTextLData.width = 120;
						userNameTextLData.height = 16;
						userNameTextLData.left = new FormAttachment(0, 1000, 305);
						userNameTextLData.top = new FormAttachment(0, 1000, 7);
						this.userNameText.setLayoutData(userNameTextLData);
						this.userNameText.addVerifyListener(new VerifyListener() {
							public void verifyText(VerifyEvent evt) {
								log.log(java.util.logging.Level.FINEST, "evt.doit = " + (evt.text.length() <= 16)); //$NON-NLS-1$
								evt.doit = evt.text.length() <= 16;
							}
						});
						this.userNameText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								log.log(java.util.logging.Level.FINEST, "text.keyReleased, event=" + evt); //$NON-NLS-1$
								File oldConfigDataFile = new File(UltraDuoPlusDialog.this.settings.getApplHomePath() + UltraDuoPlusDialog.UDP_CONFIGURATION_SUFFIX
										+ UltraDuoPlusDialog.this.deviceIdentifierName.replace(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR) + GDE.FILE_ENDING_DOT_XML);
								if (oldConfigDataFile.exists()) oldConfigDataFile.delete();
								UltraDuoPlusDialog.this.deviceIdentifierName = (UltraDuoPlusDialog.this.userNameText.getText() + UltraDuoPlusDialog.STRING_16_BLANK).substring(0, 16);
								UltraDuoPlusDialog.this.ultraDuoPlusSetup.setIdentifierName(UltraDuoPlusDialog.this.deviceIdentifierName);
								UltraDuoPlusDialog.this.ultraDuoPlusSetup.setChanged(true);
								int position = UltraDuoPlusDialog.this.userNameText.getCaretPosition();
								UltraDuoPlusDialog.this.userNameText.setText(UltraDuoPlusDialog.this.deviceIdentifierName);
								UltraDuoPlusDialog.this.userNameText.setSelection(position);
							}

							@Override
							public void keyPressed(KeyEvent evt) {
								log.log(java.util.logging.Level.FINEST, "text.keyPressed, event=" + evt); //$NON-NLS-1$
							}
						});
					}
					{
						this.tabFolder = new CTabFolder(this.boundsComposite, SWT.BORDER);
						this.tabFolder.setSimple(false);
						{
							this.setupTabItem = new CTabItem(this.tabFolder, SWT.NONE);
							this.setupTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2290));
							{
								this.channelBaseDataTabFolder = new CTabFolder(this.tabFolder, SWT.NONE);
								this.channelBaseDataTabFolder.setSimple(false);
								this.setupTabItem.setControl(this.channelBaseDataTabFolder);
								{
									this.baseSetupTabItem = new CTabItem(this.channelBaseDataTabFolder, SWT.NONE);
									this.baseSetupTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.baseSetupTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2291));
									{
										this.scollableDeviceComposite = new ScrolledComposite(this.channelBaseDataTabFolder, SWT.BORDER | SWT.V_SCROLL);
										this.baseSetupTabItem.setControl(this.scollableDeviceComposite);
										FillLayout composite2Layout = new FillLayout();
										this.scollableDeviceComposite.setLayout(composite2Layout);
										{
											this.baseDeviceSetupComposite = new Composite(this.scollableDeviceComposite, SWT.NONE);
											FillLayout composite1Layout = new FillLayout(SWT.VERTICAL);
											this.baseDeviceSetupComposite.setLayout(composite1Layout);

											new ParameterHeaderControl(this.baseDeviceSetupComposite, Messages.getString(MessageIds.GDE_MSGT2247), 175, Messages.getString(MessageIds.GDE_MSGT2248), 50, Messages.getString(MessageIds.GDE_MSGT2249), 175, 20);
											this.channelParameters[4] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 4, Messages.getString(MessageIds.GDE_MSGT2293), 175,	"°C - °F", 175, UltraDuoPlusDialog.temperatureDegreeType, 50, 150); //$NON-NLS-1$ 
											this.channelParameters[5] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 5, Messages.getString(MessageIds.GDE_MSGT2294), 175,	Messages.getString(MessageIds.GDE_MSGT2240)+GDE.STRING_MESSAGE_CONCAT+Messages.getString(MessageIds.GDE_MSGT2241), 175, UltraDuoPlusDialog.onOffType, 50, 150); //$NON-NLS-1$ 
											this.channelParameters[6] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 6, Messages.getString(MessageIds.GDE_MSGT2295), 175,	"En - De - Fr - It", 175, UltraDuoPlusDialog.languageTypes, 50, 150); //$NON-NLS-1$ 
											//channelParameters[7] = new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 7, "PC setup", 175, "DISABLE | ENABLE", 175, diableEnableType, 50, 150);
											this.channelParameters[8] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 8, Messages.getString(MessageIds.GDE_MSGT2296), 175,	"120 ~ 150 (12.0 ~ 15.0V)", 175, true, 50, 150, 120, 150, -100); //$NON-NLS-1$ 
											this.channelParameters[9] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 9, Messages.getString(MessageIds.GDE_MSGT2297), 175,	"50 ~ 400 (5 ~ 40A)", 175, true, 50, 150, 50, 400, -50); //$NON-NLS-1$ 
											this.channelParameters[10] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 10, Messages.getString(MessageIds.GDE_MSGT2298), 175,	"1 ~ 31", 175, false, 50, 150, 1, 31); //$NON-NLS-1$ 
											this.channelParameters[11] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 11, Messages.getString(MessageIds.GDE_MSGT2299), 175,	"1 ~ 12", 175, false, 50, 150, 1, 12); //$NON-NLS-1$ 
											this.channelParameters[12] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 12, Messages.getString(MessageIds.GDE_MSGT2300), 175,	"0 ~ 99", 175, false, 50, 150, 2000, 2099, -2000); //$NON-NLS-1$ 
											this.channelParameters[13] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 13, Messages.getString(MessageIds.GDE_MSGT2302), 175,	"0 ~ 12", 175, false, 50, 150, 0, 12); //$NON-NLS-1$ 
											this.channelParameters[14] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 14, Messages.getString(MessageIds.GDE_MSGT2301), 175,	"0 ~ 59", 175, false, 50, 150, 0, 59); //$NON-NLS-1$ 
											this.channelParameters[15] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 15, Messages.getString(MessageIds.GDE_MSGT2303), 175,	"12H - 24H", 175, UltraDuoPlusDialog.hourFormatType, 50, 150); //$NON-NLS-1$ 
										}
										this.scollableDeviceComposite.setContent(this.baseDeviceSetupComposite);
										this.baseDeviceSetupComposite.setSize(615, 360);
										this.scollableDeviceComposite.addControlListener(new ControlListener() {
											public void controlResized(ControlEvent evt) {
												log.log(java.util.logging.Level.FINEST, "baseDeviceSetupComposite.controlResized, event=" + evt); //$NON-NLS-1$
												UltraDuoPlusDialog.this.baseDeviceSetupComposite.setSize(UltraDuoPlusDialog.this.scollableDeviceComposite.getClientArea().width, 390);
											}
											public void controlMoved(ControlEvent evt) {
												log.log(java.util.logging.Level.FINEST, "baseDeviceSetupComposite.controlMoved, event=" + evt); //$NON-NLS-1$
												UltraDuoPlusDialog.this.baseDeviceSetupComposite.setSize(UltraDuoPlusDialog.this.scollableDeviceComposite.getClientArea().width, 390);
											}
										});
									}
								}
								{
									this.channelTabItem1 = new CTabItem(this.channelBaseDataTabFolder, SWT.NONE);
									this.channelTabItem1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.channelTabItem1.setText(Messages.getString(MessageIds.GDE_MSGT2304));
									{

										this.scollableDeviceComposite1 = new ScrolledComposite(this.channelBaseDataTabFolder, SWT.BORDER | SWT.V_SCROLL);
										this.channelTabItem1.setControl(this.scollableDeviceComposite1);
										FillLayout composite2Layout = new FillLayout();
										this.scollableDeviceComposite1.setLayout(composite2Layout);
										{
											this.baseDeviceSetupComposite1 = new Composite(this.scollableDeviceComposite1, SWT.NONE);
											FillLayout composite1Layout = new FillLayout(SWT.VERTICAL);
											this.baseDeviceSetupComposite1.setLayout(composite1Layout);

											new ParameterHeaderControl(this.baseDeviceSetupComposite1, Messages.getString(MessageIds.GDE_MSGT2247), 175, Messages.getString(MessageIds.GDE_MSGT2248), 50,	Messages.getString(MessageIds.GDE_MSGT2249), 175, 20);
											this.channelParameters[0] = new ParameterConfigControl(this.baseDeviceSetupComposite1, this.channelValues1, 0, Messages.getString(MessageIds.GDE_MSGT2306), 175, Messages.getString(MessageIds.GDE_MSGT2313), 175, UltraDuoPlusDialog.soundTime, 50, 150); //$NON-NLS-1$ 
											this.channelParameters[1] = new ParameterConfigControl(this.baseDeviceSetupComposite1, this.channelValues1, 1, Messages.getString(MessageIds.GDE_MSGT2307), 175, "1 ~ 10", 175, false, 50, 150, 1, 10); //$NON-NLS-1$ 
											this.channelParameters[2] = new ParameterConfigControl(this.baseDeviceSetupComposite1, this.channelValues1, 2, Messages.getString(MessageIds.GDE_MSGT2305), 175, "1 ~ 15", 175, false, 50, 150, 1, 15); //$NON-NLS-1$ 
											this.channelParameters[3] = new ParameterConfigControl(this.baseDeviceSetupComposite1, this.channelValues1, 3, Messages.getString(MessageIds.GDE_MSGT2308), 175, Messages.getString(MessageIds.GDE_MSGT2244)+GDE.STRING_MESSAGE_CONCAT+Messages.getString(MessageIds.GDE_MSGT2245), 175, UltraDuoPlusDialog.powerOnDisplayType, 50, 150); //$NON-NLS-1$ 
										}
										this.scollableDeviceComposite1.setContent(this.baseDeviceSetupComposite1);
										this.baseDeviceSetupComposite1.setSize(620, 150);
										this.scollableDeviceComposite1.addControlListener(new ControlListener() {
											public void controlResized(ControlEvent evt) {
												log.log(java.util.logging.Level.FINEST, "baseDeviceSetupComposite1.controlResized, event=" + evt); //$NON-NLS-1$
												UltraDuoPlusDialog.this.baseDeviceSetupComposite1.setSize(UltraDuoPlusDialog.this.scollableDeviceComposite1.getClientArea().width, 150);
											}
											public void controlMoved(ControlEvent evt) {
												log.log(java.util.logging.Level.FINEST, "baseDeviceSetupComposite1.controlMoved, event=" + evt); //$NON-NLS-1$
												UltraDuoPlusDialog.this.baseDeviceSetupComposite1.setSize(UltraDuoPlusDialog.this.scollableDeviceComposite1.getClientArea().width, 150);
											}
										});
									}
								}
								{
									this.channelTabItem2 = new CTabItem(this.channelBaseDataTabFolder, SWT.NONE);
									this.channelTabItem2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
									this.channelTabItem2.setText(Messages.getString(MessageIds.GDE_MSGT2309));
									{
										this.scollableDeviceComposite2 = new ScrolledComposite(this.channelBaseDataTabFolder, SWT.BORDER | SWT.V_SCROLL);
										this.channelTabItem2.setControl(this.scollableDeviceComposite2);
										FillLayout composite2Layout = new FillLayout();
										this.scollableDeviceComposite2.setLayout(composite2Layout);
										{
											this.baseDeviceSetupComposite2 = new Composite(this.scollableDeviceComposite2, SWT.NONE);
											FillLayout composite1Layout = new FillLayout(SWT.VERTICAL);
											this.baseDeviceSetupComposite2.setLayout(composite1Layout);

											new ParameterHeaderControl(this.baseDeviceSetupComposite2, Messages.getString(MessageIds.GDE_MSGT2247), 175, Messages.getString(MessageIds.GDE_MSGT2248), 50,	Messages.getString(MessageIds.GDE_MSGT2249), 175, 20);
											this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + 0] = new ParameterConfigControl(this.baseDeviceSetupComposite2, this.channelValues2, 0, Messages.getString(MessageIds.GDE_MSGT2255), 175, Messages.getString(MessageIds.GDE_MSGT2313), 175, UltraDuoPlusDialog.soundTime, 50, 150); //$NON-NLS-1$ 
											this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + 1] = new ParameterConfigControl(this.baseDeviceSetupComposite2, this.channelValues2, 1, Messages.getString(MessageIds.GDE_MSGT2254), 175, "1 ~ 10", 175, false, 50, 150, 1, 10); //$NON-NLS-1$ 
											this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + 2] = new ParameterConfigControl(this.baseDeviceSetupComposite2, this.channelValues2, 2, Messages.getString(MessageIds.GDE_MSGT2305), 175, "1 ~ 15", 175, false, 50, 150, 1, 15); //$NON-NLS-1$ 
											this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + 3] = new ParameterConfigControl(this.baseDeviceSetupComposite2, this.channelValues2, 3, Messages.getString(MessageIds.GDE_MSGT2308), 175, Messages.getString(MessageIds.GDE_MSGT2244)+GDE.STRING_MESSAGE_CONCAT+Messages.getString(MessageIds.GDE_MSGT2245), 175, UltraDuoPlusDialog.powerOnDisplayType, 50, 150); //$NON-NLS-1$ 
										}
										this.scollableDeviceComposite2.setContent(this.baseDeviceSetupComposite2);
										this.baseDeviceSetupComposite.setSize(620, 150);
										this.scollableDeviceComposite2.addControlListener(new ControlListener() {
											public void controlResized(ControlEvent evt) {
												log.log(java.util.logging.Level.FINEST, "baseDeviceSetupComposite2.controlResized, event=" + evt); //$NON-NLS-1$
												UltraDuoPlusDialog.this.baseDeviceSetupComposite2.setSize(UltraDuoPlusDialog.this.scollableDeviceComposite2.getClientArea().width, 150);
											}
											public void controlMoved(ControlEvent evt) {
												log.log(Level.FINEST, "baseDeviceSetupComposite2.controlMoved, event=" + evt); //$NON-NLS-1$
												UltraDuoPlusDialog.this.baseDeviceSetupComposite2.setSize(UltraDuoPlusDialog.this.scollableDeviceComposite2.getClientArea().width, 150);
											}
										});
									}
								}
								this.channelBaseDataTabFolder.setSelection(0);
							}
						}
						{
							this.memorySetupTabItem = new CTabItem(this.tabFolder, SWT.NONE);
							this.memorySetupTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.memorySetupTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2250));
							{
								this.memoryBoundsComposite = new Composite(this.tabFolder, SWT.NONE);
								this.memorySetupTabItem.setControl(this.memoryBoundsComposite);
								this.memoryBoundsComposite.setLayout(new FormLayout());
								{
									this.memorySelectComposite = new Composite(this.memoryBoundsComposite, SWT.NONE);
									FormData memorySelectLData = new FormData();
									memorySelectLData.height = 55;
									memorySelectLData.left = new FormAttachment(0, 1000, 0);
									memorySelectLData.right = new FormAttachment(1000, 1000, 0);
									memorySelectLData.top = new FormAttachment(0, 1000, 0);
									this.memorySelectComposite.setLayoutData(memorySelectLData);
									RowLayout composite2Layout = new RowLayout(SWT.HORIZONTAL);
									this.memorySelectComposite.setLayout(composite2Layout);
									this.memorySelectComposite.setBackground(SWTResourceManager.getColor(GDE.IS_MAC ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
									{
										this.memorySelectLabel = new CLabel(this.memorySelectComposite, SWT.RIGHT);
										RowData memorySelectLabelLData = new RowData();
										memorySelectLabelLData.width = 120;
										memorySelectLabelLData.height = 20;
										this.memorySelectLabel.setLayoutData(memorySelectLabelLData);
										this.memorySelectLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.memorySelectLabel.setText(Messages.getString(MessageIds.GDE_MSGT2251));
										this.memorySelectLabel.setBackground(SWTResourceManager.getColor(GDE.IS_MAC ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
									}
									{
										updateBatterySetup(1);
										this.memoryCombo = new CCombo(this.memorySelectComposite, SWT.BORDER);
										this.memoryCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.memoryCombo.setItems(this.memoryNames);
										this.memoryCombo.setVisibleItemCount(20);
										RowData memoryComboLData = new RowData();
										memoryComboLData.width = 165;
										memoryComboLData.height = GDE.IS_MAC ? 18 : 16;
										this.memoryCombo.setLayoutData(memoryComboLData);
										this.memoryCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2252));
										this.memoryCombo.select(0);
										this.memoryCombo.setBackground(SWTResourceManager.getColor(GDE.IS_MAC ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
										this.memoryCombo.setEditable(true);
										this.memoryCombo.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												log.log(Level.FINEST, "memoryCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
												try {
													int actualSelectionIndex = UltraDuoPlusDialog.this.memoryCombo.getSelectionIndex();
													if (UltraDuoPlusDialog.this.lastMemorySelectionIndex != actualSelectionIndex) {
														if (UltraDuoPlusDialog.this.lastMemorySelectionIndex >= 0 && UltraDuoPlusDialog.this.lastMemorySelectionIndex < 60) {
															//write memory if setup data has been changed changed (update memory name executed while keyListener)
															if (UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData().isChanged()) {
																UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData().setValue(UltraDuoPlusDialog.this.device.convert2String(UltraDuoPlusDialog.this.memoryValues));
																UltraDuoPlusDialog.this.serialPort.writeConfigData(UltramatSerialPort.WRITE_MEMORY_SETUP,	UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData().getValue().getBytes(),	UltraDuoPlusDialog.this.lastMemorySelectionIndex + 1);
																UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData().changed = null;
															}
															//check for copy selected
															if (UltraDuoPlusDialog.this.copyButton.getSelection()) {
																UltraDuoPlusDialog.this.copyButton.setSelection(false);

																if (SWT.YES == UltraDuoPlusDialog.this.application.openYesNoMessageDialog(
																		UltraDuoPlusDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGI2205,	new Object[] { 
																				UltraDuoPlusDialog.this.lastMemorySelectionIndex, UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getName(),
																				(actualSelectionIndex+1), UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(actualSelectionIndex).getName() }))) {
																	//copy memory name and setup data of lastMemorySelectionIndex to memoryCombo.getSelectionIndex()
																	log.log(Level.FINE, "copy memory: (" + UltraDuoPlusDialog.this.lastMemorySelectionIndex + GDE.STRING_RIGHT_PARENTHESIS + UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getName()//$NON-NLS-1$
																			+ " to (" + (actualSelectionIndex+1) + GDE.STRING_RIGHT_PARENTHESIS + UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(actualSelectionIndex).getName()); //$NON-NLS-1$
																	if (log.isLoggable(Level.FINE)) {
																		StringBuffer sb = new StringBuffer();
																		for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
																			sb.append(UltraDuoPlusDialog.this.memoryValues[i]).append(GDE.STRING_LEFT_BRACKET).append(i).append(GDE.STRING_RIGHT_BRACKET_COMMA);
																		}
																		log.log(Level.FINE, sb.toString());
																	}
																	UltraDuoPlusDialog.this.serialPort.writeConfigData(UltramatSerialPort.WRITE_MEMORY_NAME, UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getName().getBytes(),	actualSelectionIndex + 1);
																	UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).changed = null;
																	UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(actualSelectionIndex).setName(UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getName());
																	
																	UltraDuoPlusDialog.this.serialPort.writeConfigData(UltramatSerialPort.WRITE_MEMORY_SETUP,	UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData().getValue().getBytes(),	actualSelectionIndex + 1);
																	UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData().changed = null;
																	UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(actualSelectionIndex).getSetupData().setValue(UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData().getValue());

																	UltraDuoPlusDialog.this.memoryCombo.setText(String.format(UltraDuoPlusDialog.STRING_FORMAT_02d_s, actualSelectionIndex+1, UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(actualSelectionIndex).getName()));
																}
															}
														}
														updateBatterySetup(actualSelectionIndex);
														UltraDuoPlusDialog.this.lastMemorySelectionIndex = actualSelectionIndex;
													}
												}
												catch (Exception e) {
													log.log(Level.SEVERE, e.getMessage(), e);
												}
											}
										});
										this.memoryCombo.addKeyListener(new KeyAdapter() {
											@Override
											public void keyReleased(KeyEvent evt) {
												log.log(java.util.logging.Level.FINEST, "memoryCombo.keyReleased, event=" + evt); //$NON-NLS-1$
											}

											@Override
											public void keyPressed(KeyEvent evt) {
												log.log(java.util.logging.Level.FINEST, "memoryCombo.keyPressed, event=" + evt); //$NON-NLS-1$
												if (evt.character == SWT.CR) {
													try {
														UltraDuoPlusDialog.this.memoryNames[UltraDuoPlusDialog.this.lastMemorySelectionIndex] = String.format(UltraDuoPlusDialog.STRING_FORMAT_02d_s,
																UltraDuoPlusDialog.this.lastMemorySelectionIndex + 1, (UltraDuoPlusDialog.this.memoryCombo.getText() + UltraDuoPlusDialog.STRING_16_BLANK).substring(5, 16 + 5));
														UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex)
																.setName(UltraDuoPlusDialog.this.memoryNames[UltraDuoPlusDialog.this.lastMemorySelectionIndex].substring(5));
														UltraDuoPlusDialog.this.serialPort.writeConfigData(UltramatSerialPort.WRITE_MEMORY_NAME,
																UltraDuoPlusDialog.this.memoryNames[UltraDuoPlusDialog.this.lastMemorySelectionIndex].substring(5).getBytes(), UltraDuoPlusDialog.this.lastMemorySelectionIndex + 1);
														UltraDuoPlusDialog.this.memoryCombo.setItems(UltraDuoPlusDialog.this.memoryNames);
													}
													catch (Exception e) {
														UltraDuoPlusDialog.this.application.openMessageDialog(UltraDuoPlusDialog.this.dialogShell,
																Messages.getString(gde.messages.MessageIds.GDE_MSGE0007, new String[] { e.getMessage() }));
													}
												}
											}
										});
									}
									{
										CLabel filler = new CLabel(this.memorySelectComposite, SWT.RIGHT);
										filler.setLayoutData(new RowData(100, 20));
										filler.setText("<---------   "); //$NON-NLS-1$
										filler.setBackground(SWTResourceManager.getColor(GDE.IS_MAC ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
									}
									{
										this.copyButton = new Button(this.memorySelectComposite, SWT.CHECK | SWT.LEFT);
										RowData cellTypeSelectLabelLData = new RowData();
										cellTypeSelectLabelLData.width = 190;
										cellTypeSelectLabelLData.height = 20;
										this.copyButton.setLayoutData(cellTypeSelectLabelLData);
										this.copyButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.copyButton.setText(Messages.getString(MessageIds.GDE_MSGT2288));
										this.copyButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2256));
										this.copyButton.setBackground(SWTResourceManager.getColor(GDE.IS_MAC ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
									}
									new ParameterHeaderControl(this.memorySelectComposite, Messages.getString(MessageIds.GDE_MSGT2247), 175, Messages.getString(MessageIds.GDE_MSGT2248), 50,	Messages.getString(MessageIds.GDE_MSGT2249), 180, 20);
								}
								{
									this.scrolledMemoryComposite = new ScrolledComposite(this.memoryBoundsComposite, SWT.BORDER | SWT.V_SCROLL);
									FillLayout scrolledComposite1Layout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
									this.scrolledMemoryComposite.setLayout(scrolledComposite1Layout);
									FormData scrolledMemoryCompositeLData = new FormData();
									scrolledMemoryCompositeLData.left = new FormAttachment(0, 1000, 0);
									scrolledMemoryCompositeLData.right = new FormAttachment(1000, 1000, 0);
									scrolledMemoryCompositeLData.bottom = new FormAttachment(1000, 1000, 0);
									scrolledMemoryCompositeLData.top = new FormAttachment(0, 1000, 55);
									this.scrolledMemoryComposite.setLayoutData(scrolledMemoryCompositeLData);
									FillLayout scrolledMemoryCompositeLayout = new FillLayout();
									this.scrolledMemoryComposite.setLayout(scrolledMemoryCompositeLayout);
									{
										this.memoryComposite = new Composite(this.scrolledMemoryComposite, SWT.NONE);
										FillLayout memoryCompositeLayout = new FillLayout(SWT.VERTICAL);
										this.memoryComposite.setLayout(memoryCompositeLayout);

										this.memoryParameters[0] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 0, Messages.getString(MessageIds.GDE_MSGT2257), 175,	Messages.getString(MessageIds.GDE_MSGT2246), 220, UltraDuoPlusDialog.cellTypeNames, 50, 150);
										this.memoryParameters[1] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 1, Messages.getString(MessageIds.GDE_MSGT2258), 175,	"1 ~ 18", 220, false, 50, 150, 1, 18); //$NON-NLS-1$ 
										this.memoryParameters[2] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 2, Messages.getString(MessageIds.GDE_MSGT2259), 175,	"100 ~ 50000 mAh", 220, true, 50, 150, 100, 50000, -100); //$NON-NLS-1$ 
										this.memoryParameters[3] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 3, Messages.getString(MessageIds.GDE_MSGT2260), 175,	"2000 ~ 2099", 220, false, 50, 150, 2000, 2099, -2000); //$NON-NLS-1$ 
										this.memoryParameters[4] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 4, Messages.getString(MessageIds.GDE_MSGT2261), 175,	"1 ~ 12", 220, false, 50, 150, 1, 12); //$NON-NLS-1$ 
										this.memoryParameters[5] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 5, Messages.getString(MessageIds.GDE_MSGT2262), 175,	"1 ~ 31", 220, false, 50, 150, 1, 31); //$NON-NLS-1$ 
										this.memoryParameters[6] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 6, Messages.getString(MessageIds.GDE_MSGT2263), 175,	"100 ~ 20000 mA", 220, true, 50, 150, 100, 20000, -100); //$NON-NLS-1$ 
										this.memoryParameters[10] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 10, Messages.getString(MessageIds.GDE_MSGT2264), 175,	"10 ~ 80°C , 50 ~ 176°F", 220, false, 50, 150, 10, 176); //$NON-NLS-1$ 
										this.memoryParameters[11] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 11, Messages.getString(MessageIds.GDE_MSGT2265), 175,	Messages.getString(MessageIds.GDE_MSGT2310), 220, true, 50, 150, 10, 155); //$NON-NLS-1$ 
										this.memoryParameters[12] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 12, Messages.getString(MessageIds.GDE_MSGT2266), 175,	"10 ~ 905min (905=off)", 220, false, 50, 150, 10, 905); //$NON-NLS-1$ 
										this.memoryParameters[14] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 14, Messages.getString(MessageIds.GDE_MSGT2267), 175,	"1000 ~ 4300 mV", 220, true, 50, 150, 1000, 4300, -1000); //$NON-NLS-1$ 
										this.memoryParameters[17] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 17, Messages.getString(MessageIds.GDE_MSGT2268), 175,	"100 ~ 10000 mA", 220, true, 50, 150, 100, 10000, -100); //$NON-NLS-1$ 
										this.memoryParameters[18] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 18, Messages.getString(MessageIds.GDE_MSGT2269), 175,	"100 ~ 4200 mV", 220, true, 50, 150, 100, 4200, -100); //$NON-NLS-1$ 
										this.memoryParameters[19] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 19, Messages.getString(MessageIds.GDE_MSGT2270), 175,	"10 ~ 80°C , 50 ~ 176°F", 220, false, 50, 150, 10, 176); //$NON-NLS-1$ 
										this.memoryParameters[20] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 20, Messages.getString(MessageIds.GDE_MSGT2271), 175,	Messages.getString(MessageIds.GDE_MSGT2311), 220, false, 50, 150, 10, 105, -10); //$NON-NLS-1$ 
										this.memoryParameters[22] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 22, Messages.getString(MessageIds.GDE_MSGT2272), 175,	Messages.getString(MessageIds.GDE_MSGT2292), 220, UltraDuoPlusDialog.cycleDirectionTypes, 50, 150);
										this.memoryParameters[23] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 23, Messages.getString(MessageIds.GDE_MSGT2273), 175,	"1 ~ 10", 220, false, 50, 150, 1, 10); //$NON-NLS-1$ 
										this.memoryParameters[24] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 24, Messages.getString(MessageIds.GDE_MSGT2274), 175,	"1 ~ 30min", 220, false, 50, 150, 1, 30); //$NON-NLS-1$ 
										this.memoryParameters[25] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 25, Messages.getString(MessageIds.GDE_MSGT2275), 175,	"1 ~ 30min", 220, false, 50, 150, 1, 30); //$NON-NLS-1$ 
										this.memoryParameters[26] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 26, Messages.getString(MessageIds.GDE_MSGT2276), 175,	"1000 ~ 4000 mV", 220, true, 50, 150, 1000, 4000, -1000); //$NON-NLS-1$ 

										this.memoryParameters[7] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 7, Messages.getString(MessageIds.GDE_MSGT2277), 175,	"0 ~ 25mV", 220, false, 50, 150, 0, 25); //$NON-NLS-1$ 
										this.memoryParameters[8] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 8, Messages.getString(MessageIds.GDE_MSGT2278), 175,	"1 ~ 20min", 220, false, 50, 150, 1, 20); //$NON-NLS-1$ 
										this.memoryParameters[9] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 9, Messages.getString(MessageIds.GDE_MSGT2279), 175,	Messages.getString(MessageIds.GDE_MSGT2312), 220, false, 50, 150, 0, 500); //$NON-NLS-1$ 

										this.memoryParameters[13] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 13, Messages.getString(MessageIds.GDE_MSGT2280), 175,	"1 ~ 5", 220, false, 50, 150, 1, 5); //$NON-NLS-1$ 
										this.memoryParameters[15] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 15, Messages.getString(MessageIds.GDE_MSGT2281), 175,	"1 ~ 30min", 220, false, 50, 150, 1, 30); //$NON-NLS-1$ 
										this.memoryParameters[16] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 16, Messages.getString(MessageIds.GDE_MSGT2282), 175,	Messages.getString(MessageIds.GDE_MSGT2241)+GDE.STRING_MESSAGE_CONCAT+Messages.getString(MessageIds.GDE_MSGT2240), 220, UltraDuoPlusDialog.offOnType, 50, 150); //$NON-NLS-1$ 
										this.memoryParameters[21] = new ParameterConfigControl(this.memoryComposite, this.memoryValues, 21, Messages.getString(MessageIds.GDE_MSGT2283), 175,	"1100 ~ 1300 mV", 220, true, 50, 150, 1100, 1300, -1100); //$NON-NLS-1$ 
									}
									this.scrolledMemoryComposite.setContent(this.memoryComposite);
									this.memoryComposite.setSize(620, this.memorySelectHeight);
									this.scrolledMemoryComposite.addControlListener(new ControlListener() {
										public void controlResized(ControlEvent evt) {
											log.log(Level.FINEST, "scrolledMemoryComposite.controlResized, event=" + evt); //$NON-NLS-1$
											UltraDuoPlusDialog.this.memoryComposite.setSize(UltraDuoPlusDialog.this.scrolledMemoryComposite.getClientArea().width, UltraDuoPlusDialog.this.memorySelectHeight);
										}
										public void controlMoved(ControlEvent evt) {
											log.log(Level.FINEST, "scrolledMemoryComposite.controlMoved, event=" + evt); //$NON-NLS-1$
											UltraDuoPlusDialog.this.memoryComposite.setSize(UltraDuoPlusDialog.this.scrolledMemoryComposite.getClientArea().width, UltraDuoPlusDialog.this.memorySelectHeight);
										}
									});
								}
							}
						}
						FormData TabFolderLData = new FormData();
						TabFolderLData.width = 549;
						TabFolderLData.height = 466;
						TabFolderLData.left = new FormAttachment(0, 1000, 0);
						TabFolderLData.top = new FormAttachment(0, 1000, 37);
						TabFolderLData.right = new FormAttachment(1000, 1000, 2);
						TabFolderLData.bottom = new FormAttachment(1000, 1000, -44);
						this.tabFolder.setLayoutData(TabFolderLData);
						this.tabFolder.setSelection(0);
					}
					{
						this.restoreButton = new Button(this.boundsComposite, SWT.PUSH | SWT.CENTER);
						FormData restoreButtonLData = new FormData();
						restoreButtonLData.width = 118;
						restoreButtonLData.height = GDE.IS_MAC ? 33 : 30;
						restoreButtonLData.left = new FormAttachment(0, 1000, 165);
						restoreButtonLData.bottom = new FormAttachment(1000, 1000, -8);
						this.restoreButton.setLayoutData(restoreButtonLData);
						this.restoreButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.restoreButton.setText(Messages.getString(MessageIds.GDE_MSGT2284));
						this.restoreButton.setEnabled(false);
						this.restoreButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "restoreButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								FileDialog fileDialog = UltraDuoPlusDialog.this.application.openFileOpenDialog(UltraDuoPlusDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGT2284),
										new String[] { GDE.FILE_ENDING_DOT_XML }, UltraDuoPlusDialog.this.settings.getDataFilePath(), GDE.STRING_EMPTY, SWT.SINGLE);
								if (fileDialog.getFileName().length() > 4) {
									try {
										Unmarshaller unmarshaller = UltraDuoPlusDialog.this.jc.createUnmarshaller();
										unmarshaller.setSchema(UltraDuoPlusDialog.this.schema);
										UltraDuoPlusType tmpUltraDuoPlusSetup = (UltraDuoPlusType) unmarshaller.unmarshal(new File(fileDialog.getFilterPath()));
										copyUltraDuoPlusSetup(tmpUltraDuoPlusSetup);

										UltraDuoPlusDialog.this.synchronizerWrite = new UltraDuoPlusSychronizer(UltraDuoPlusDialog.this, UltraDuoPlusDialog.this.serialPort, UltraDuoPlusDialog.this.ultraDuoPlusSetup,
												UltraDuoPlusSychronizer.SYNC_TYPE.WRITE);
										UltraDuoPlusDialog.this.synchronizerWrite.start();
									}
									catch (Exception e) {
										log.log(Level.SEVERE, e.getMessage(), e);
										if (e.getCause() instanceof FileNotFoundException) {
											UltraDuoPlusDialog.this.ultraDuoPlusSetup = new ObjectFactory().createUltraDuoPlusType();
											List<MemoryType> cellMemories = UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory();
											if (cellMemories.size() < 60) { // initially create only base setup data
												for (int i = 0; i < 60; i++) {
													MemoryType cellMemory = new ObjectFactory().createMemoryType();
													cellMemory.setSetupData(new ObjectFactory().createMemoryTypeSetupData());
													cellMemories.add(cellMemory);
												}
											}
										}
										else
											UltraDuoPlusDialog.this.application.openMessageDialog(UltraDuoPlusDialog.this.dialogShell,
													Messages.getString(gde.messages.MessageIds.GDE_MSGE0007, new String[] { e.getMessage() }));
									}
								}
							}
						});
					}
					{
						this.backupButton = new Button(this.boundsComposite, SWT.PUSH | SWT.CENTER);
						FormData backupButtonLData = new FormData();
						backupButtonLData.width = 118;
						backupButtonLData.height = GDE.IS_MAC ? 33 : 30;
						backupButtonLData.left = new FormAttachment(0, 1000, 29);
						backupButtonLData.bottom = new FormAttachment(1000, 1000, -8);
						this.backupButton.setLayoutData(backupButtonLData);
						this.backupButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.backupButton.setText(Messages.getString(MessageIds.GDE_MSGT2286));
						this.backupButton.setEnabled(false);
						this.backupButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "backupButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								FileDialog fileDialog = UltraDuoPlusDialog.this.application.prepareFileSaveDialog(UltraDuoPlusDialog.this.dialogShell, Messages.getString(MessageIds.GDE_MSGT2285),
										new String[] { GDE.FILE_ENDING_DOT_XML }, UltraDuoPlusDialog.this.settings.getDataFilePath(), UltraDuoPlusDialog.this.device.getName() + GDE.STRING_UNDER_BAR);
								String configFilePath = fileDialog.open();
								if (configFilePath != null && fileDialog.getFileName().length() > 4) {
									if (FileUtils.checkFileExist(configFilePath)) {
										if(SWT.YES != UltraDuoPlusDialog.this.application.openYesNoMessageDialog(UltraDuoPlusDialog.this.dialogShell,	Messages.getString(gde.messages.MessageIds.GDE_MSGI0007, new Object[] { configFilePath }))) {
											return;
										}
									}
									saveConfigUDP(configFilePath);
								}
							}
						});
					}
					{
						this.closeButton = new Button(this.boundsComposite, SWT.PUSH | SWT.CENTER);
						FormData writeButtonLData = new FormData();
						writeButtonLData.width = 118;
						writeButtonLData.height = GDE.IS_MAC ? 33 : 30;
						writeButtonLData.bottom = new FormAttachment(1000, 1000, -8);
						writeButtonLData.right = new FormAttachment(1000, 1000, -21);
						this.closeButton.setLayoutData(writeButtonLData);
						this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.closeButton.setText(Messages.getString(MessageIds.GDE_MSGT2287));
						this.closeButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								UltraDuoPlusDialog.this.dialogShell.dispose();
							}
						});
					}
					{
						this.helpButton = new Button(this.boundsComposite, SWT.PUSH | SWT.CENTER);
						this.helpButton.setImage(SWTResourceManager.getImage("gde/resource/QuestionHot.gif")); //$NON-NLS-1$
						FormData LoadButtonLData = new FormData();
						LoadButtonLData.width = 118;
						LoadButtonLData.height = GDE.IS_MAC ? 33 : 30;
						LoadButtonLData.bottom = new FormAttachment(1000, 1000, -8);
						LoadButtonLData.right = new FormAttachment(1000, 1000, -158);
						this.helpButton.setLayoutData(LoadButtonLData);
						this.helpButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "helpButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								UltraDuoPlusDialog.this.application.openHelpDialog(UltraDuoPlusDialog.DEVICE_JAR_NAME, "HelpInfo.html"); //$NON-NLS-1$ 
							}
						});
					}
					this.boundsComposite.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							log.log(Level.FINER, "boundsComposite.paintControl() " + evt); //$NON-NLS-1$
						}
					});
				} // end boundsComposite
				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 300, 100));
				this.dialogShell.open();
				this.lastMemorySelectionIndex = -1;
				this.lastCellSelectionIndex = -1;
				updateBaseSetup();
				this.memoryCombo.notifyListeners(SWT.Selection, new Event());
			}
			else {
				this.dialogShell.setVisible(true);
				this.dialogShell.setActive();
				this.lastMemorySelectionIndex = -1;
				this.lastCellSelectionIndex = -1;
				updateBaseSetup();
				this.memoryCombo.notifyListeners(SWT.Selection, new Event());
			}
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		finally {
			if (this.serialPort != null && this.serialPort.isConnected()) {
				try {
					this.serialPort.write(UltramatSerialPort.RESET_END);
				}
				catch (IOException e) {
					// ignore
				}
				finally {
					this.serialPort.close();
				}
			}
		}
	}

	/**
	 * create minimal ultra duo plus XML data 
	 * @param useDeviceIdentifierName
	 */
	private void createUltraDuoPlusSetup(String useDeviceIdentifierName) {
		this.ultraDuoPlusSetup = new ObjectFactory().createUltraDuoPlusType();
		List<MemoryType> cellMemories = this.ultraDuoPlusSetup.getMemory();
		if (cellMemories.size() < 60) { // initially create only base setup data
			for (int i = 0; i < 60; i++) {
				MemoryType cellMemory = new ObjectFactory().createMemoryType();
				cellMemory.setSetupData(new ObjectFactory().createMemoryTypeSetupData());
				cellMemories.add(cellMemory);
			}
		}
		this.ultraDuoPlusSetup.setIdentifierName(useDeviceIdentifierName);
	}

	/**
	 * update basic setup data from cache or actual red
	 * @throws TimeOutException 
	 * @throws IOException 
	 */
	private void updateBaseSetup() throws IOException, TimeOutException {
		log.log(Level.FINEST, GDE.STRING_ENTRY);
		if (this.ultraDuoPlusSetup.getChannelData1() == null || !this.ultraDuoPlusSetup.getChannelData1().isSynced()) {
			ChannelData1 channelData1 = new ObjectFactory().createUltraDuoPlusTypeChannelData1();
			channelData1.setValue(this.serialPort.readChannelData(1));
			this.ultraDuoPlusSetup.setChannelData1(channelData1);
		}
		if (this.ultraDuoPlusSetup.getChannelData2() == null || !this.ultraDuoPlusSetup.getChannelData2().isSynced()) {
			ChannelData2 channelData2 = new ObjectFactory().createUltraDuoPlusTypeChannelData2();
			channelData2.setValue(this.serialPort.readChannelData(2));
			this.ultraDuoPlusSetup.setChannelData2(channelData2);
		}
		this.device.convert2IntArray(this.channelValues1, this.ultraDuoPlusSetup.channelData1.getValue());
		this.device.convert2IntArray(this.channelValues2, this.ultraDuoPlusSetup.channelData2.getValue());

		if (log.isLoggable(Level.FINEST)) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < UltramatSerialPort.SIZE_CHANNEL_1_SETUP; i++) {
				sb.append(this.channelValues1[i]).append(GDE.STRING_LEFT_BRACKET).append(i).append(GDE.STRING_RIGHT_BRACKET_COMMA);
			}
			sb.append(" : ");//$NON-NLS-1$
			for (int i = 0; i < UltramatSerialPort.SIZE_CHANNEL_2_SETUP; i++) {
				sb.append(this.channelValues2[i]).append(GDE.STRING_LEFT_BRACKET).append(i).append(GDE.STRING_RIGHT_BRACKET_COMMA);
			}
			log.log(Level.FINEST, sb.toString());
		}

		for (int i = 0; i < UltramatSerialPort.SIZE_CHANNEL_1_SETUP; i++) {
			if (this.channelParameters[i] != null) {
				this.channelParameters[i].setSliderSelection(this.channelValues1[i]);
			}
		}
		for (int i = 0; i < UltramatSerialPort.SIZE_CHANNEL_2_SETUP; i++) {
			if (this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + i] != null) {
				this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + i].setSliderSelection(this.channelValues2[i]);
			}
		}
		log.log(Level.FINEST, "add handler"); //$NON-NLS-1$
		this.baseDeviceSetupComposite.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				log.log(Level.FINEST, "baseDeviceSetupComposite.handleEvent, channelValues1[" + evt.index + "] changed"); //$NON-NLS-1$ //$NON-NLS-2$
				UltraDuoPlusDialog.this.ultraDuoPlusSetup.getChannelData1().setChanged(true);
			}
		});
		this.baseDeviceSetupComposite1.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				log.log(Level.FINEST, "baseDeviceSetupComposite1.handleEvent, channelValues1[" + evt.index + "] changed"); //$NON-NLS-1$ //$NON-NLS-2$
				UltraDuoPlusDialog.this.ultraDuoPlusSetup.getChannelData1().setChanged(true);
			}
		});
		this.baseDeviceSetupComposite2.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				log.log(Level.FINEST, "baseDeviceSetupComposite2.handleEvent, channelValues2[" + evt.index + "] changed"); //$NON-NLS-1$ //$NON-NLS-2$
				UltraDuoPlusDialog.this.ultraDuoPlusSetup.getChannelData2().setChanged(true);
			}
		});
		log.log(Level.FINEST, GDE.STRING_EXIT);
	}

	/**
	 * update values by given memory number
	 * @param memoryNumber
	 * @throws IOException
	 * @throws TimeOutException
	 */
	private void updateBatterySetup(int memoryNumber) {
		log.log(Level.FINEST, GDE.STRING_ENTRY);
		try {
			if (this.memoryComposite != null && !this.memoryComposite.isDisposed()) {
				log.log(Level.FINEST, "remove event handler"); //$NON-NLS-1$
				this.memoryComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
			}

			if (this.ultraDuoPlusSetup.getMemory() != null) {
				for (int i = 0; i < 60; i++) {
					if (this.ultraDuoPlusSetup.getMemory().get(i).isSynced())
						this.memoryNames[i] = String.format(UltraDuoPlusDialog.STRING_FORMAT_02d_s, i + 1, this.ultraDuoPlusSetup.getMemory().get(i).getName());
					else if (this.ultraDuoPlusSetup.getMemory().get(i) != null && this.ultraDuoPlusSetup.getMemory().get(i).getName() != null) {
						this.memoryNames[i] = String.format(UltraDuoPlusDialog.STRING_FORMAT_02d_s, i + 1, this.ultraDuoPlusSetup.getMemory().get(i).getName());
					}
					else {
						WaitTimer.delay(100);
						i = i >= 1 ? --i : i;
					}
					log.log(Level.FINEST, "-------> " + this.memoryNames[i]); //$NON-NLS-1$
				}
			}
			else {
				for (int i = 0; i < 60; i++) {
					this.memoryNames[i] = String.format("%02d -  NEW-BATT-NAME", i + 1); //$NON-NLS-1$
				}
				this.memoryNames[memoryNumber] = String.format(UltraDuoPlusDialog.STRING_FORMAT_02d_s, memoryNumber + 1, this.serialPort.readMemoryName(memoryNumber + 1));
			}
			if (this.memoryCombo != null && !this.memoryCombo.isDisposed()) this.memoryCombo.setItems(this.memoryNames);

			if (this.ultraDuoPlusSetup.getMemory().get(memoryNumber).getSetupData() != null && !this.ultraDuoPlusSetup.getMemory().get(memoryNumber).getSetupData().isSynced()) {
				this.ultraDuoPlusSetup.getMemory().get(memoryNumber).getSetupData().setValue(this.serialPort.readMemorySetup(memoryNumber + 1));
			}
			this.device.convert2IntArray(this.memoryValues, this.ultraDuoPlusSetup.getMemory().get(memoryNumber).getSetupData().getValue());
			if (log.isLoggable(Level.FINE)) {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
					sb.append(this.memoryValues[i]).append(GDE.STRING_LEFT_BRACKET).append(i).append(GDE.STRING_RIGHT_BRACKET_COMMA);
				}
				log.log(Level.FINE, sb.toString());
			}
			if (this.memoryParameters[0] != null && !this.memoryParameters[0].getSlider().isDisposed()) updateBatteryMemoryParameter(this.memoryValues[0]);

			for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
				if (this.memoryParameters[i] != null) {
					this.memoryParameters[i].setSliderSelection(this.memoryValues[i]);
				}
			}

			if (this.memoryComposite != null && !this.memoryComposite.isDisposed()) {
				log.log(Level.FINEST, "add event handler"); //$NON-NLS-1$
				this.memoryComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			e.printStackTrace();
		}
		log.log(Level.FINEST, GDE.STRING_EXIT);
	}

	/**
	 * update the memory setup parameter list to cell type dependent
	 */
	private void updateBatteryMemoryParameter(int selectionIndex) {
		log.log(Level.FINEST, GDE.STRING_ENTRY);
		if (this.lastCellSelectionIndex != selectionIndex) {
			this.memoryValues[0] = selectionIndex;
			//update memory parameter table to reflect not edit able parameters for selected cell type
			switch (this.memoryValues[0]) {
			case 0: //NiCd
			case 1: //NiMh
				this.memoryParameters[7] = this.memoryParameters[7] == null ? new ParameterConfigControl(this.memoryComposite, this.memoryValues, 7, Messages.getString(MessageIds.GDE_MSGT2277), 175, "0 ~ 25mV", 220, false, 50, 150, 0, 25) : this.memoryParameters[7]; //$NON-NLS-1$ 
				this.memoryParameters[8] = this.memoryParameters[8] == null ? new ParameterConfigControl(this.memoryComposite, this.memoryValues, 8, Messages.getString(MessageIds.GDE_MSGT2278), 175, "1 ~ 20min", 220, false, 50, 150, 1, 20) : this.memoryParameters[8]; //$NON-NLS-1$ 
				this.memoryParameters[9] = this.memoryParameters[9] == null ? new ParameterConfigControl(this.memoryComposite, this.memoryValues, 9, Messages.getString(MessageIds.GDE_MSGT2279), 175, Messages.getString(MessageIds.GDE_MSGT2312), 220, false, 50, 150, 0, 550) : this.memoryParameters[9]; //$NON-NLS-1$ 
				this.memoryParameters[13] = this.memoryParameters[13] == null ? new ParameterConfigControl(this.memoryComposite, this.memoryValues, 13, Messages.getString(MessageIds.GDE_MSGT2280), 175,	"1 ~ 5", 220, false, 50, 150, 1, 5) : this.memoryParameters[13]; //$NON-NLS-1$ 
				this.memoryParameters[15] = this.memoryParameters[15] == null ? new ParameterConfigControl(this.memoryComposite, this.memoryValues, 15, Messages.getString(MessageIds.GDE_MSGT2281), 175,	"1 ~ 30min", 220, false, 50, 150, 1, 30) : this.memoryParameters[15]; //$NON-NLS-1$ 
				this.memoryParameters[16] = this.memoryParameters[16] == null ? new ParameterConfigControl(this.memoryComposite, this.memoryValues, 16, Messages.getString(MessageIds.GDE_MSGT2282), 175,	Messages.getString(MessageIds.GDE_MSGT2241)+GDE.STRING_MESSAGE_CONCAT+Messages.getString(MessageIds.GDE_MSGT2240), 220, UltraDuoPlusDialog.offOnType, 50, 150) : this.memoryParameters[16]; //$NON-NLS-1$ 
				this.memoryParameters[21] = this.memoryParameters[21] == null ? new ParameterConfigControl(this.memoryComposite, this.memoryValues, 21, Messages.getString(MessageIds.GDE_MSGT2283), 175,	"1100 ~ 1300 mV", 220, true, 50, 150, 1100, 1300, -1100) : this.memoryParameters[21]; //$NON-NLS-1$ 
				this.memoryParameters[26] = this.memoryParameters[26] != null ? this.memoryParameters[26].dispose() : null;
				this.memorySelectHeight = 26 * 26;
				break;
			case 2: //LiIo
			case 3: //LiPo
			case 4: //LiFe
				this.memoryParameters[7] = this.memoryParameters[7] != null ? this.memoryParameters[7].dispose() : null;
				this.memoryParameters[8] = this.memoryParameters[8] != null ? this.memoryParameters[8].dispose() : null;
				this.memoryParameters[9] = this.memoryParameters[9] != null ? this.memoryParameters[9].dispose() : null;
				this.memoryParameters[13] = this.memoryParameters[13] != null ? this.memoryParameters[13].dispose() : null;
				this.memoryParameters[15] = this.memoryParameters[15] != null ? this.memoryParameters[15].dispose() : null;
				this.memoryParameters[16] = this.memoryParameters[16] != null ? this.memoryParameters[16].dispose() : null;
				this.memoryParameters[21] = this.memoryParameters[21] != null ? this.memoryParameters[21].dispose() : null;
				this.memoryParameters[26] = this.memoryParameters[26] == null ? new ParameterConfigControl(this.memoryComposite, this.memoryValues, 26, Messages.getString(MessageIds.GDE_MSGT2276), 175,	"1000 ~ 4000", 220, true, 50, 150, 1000, 4000, -1000) : this.memoryParameters[26]; //$NON-NLS-1$ 
				this.memorySelectHeight = 20 * 26;
				break;
			case 5: //Pb
				this.memoryParameters[7] = this.memoryParameters[7] == null ? new ParameterConfigControl(this.memoryComposite, this.memoryValues, 7, Messages.getString(MessageIds.GDE_MSGT2277), 175, "0 ~ 25 mV", 220, false, 50, 150, 0, 25) : this.memoryParameters[7]; //$NON-NLS-1$ 
				this.memoryParameters[8] = this.memoryParameters[8] == null ? new ParameterConfigControl(this.memoryComposite, this.memoryValues, 8, Messages.getString(MessageIds.GDE_MSGT2278), 175, "1 ~ 20min", 220, false, 50, 150, 1, 20) : this.memoryParameters[8]; //$NON-NLS-1$ 
				this.memoryParameters[9] = this.memoryParameters[9] == null ? new ParameterConfigControl(this.memoryComposite, this.memoryValues, 9, Messages.getString(MessageIds.GDE_MSGT2279), 175, Messages.getString(MessageIds.GDE_MSGT2312), 220, false, 50, 150, 0, 550) : this.memoryParameters[9]; //$NON-NLS-1$ 
				this.memoryParameters[13] = this.memoryParameters[13] == null ? new ParameterConfigControl(this.memoryComposite, this.memoryValues, 13, Messages.getString(MessageIds.GDE_MSGT2280), 175,	"1 ~ 5", 220, false, 50, 150, 1, 5) : this.memoryParameters[13]; //$NON-NLS-1$ 
				this.memoryParameters[15] = this.memoryParameters[15] == null ? new ParameterConfigControl(this.memoryComposite, this.memoryValues, 15, Messages.getString(MessageIds.GDE_MSGT2281), 175,	"1 ~ 30min", 220, false, 50, 150, 1, 30) : this.memoryParameters[15]; //$NON-NLS-1$ 
				this.memoryParameters[16] = this.memoryParameters[16] == null ? new ParameterConfigControl(this.memoryComposite, this.memoryValues, 16, Messages.getString(MessageIds.GDE_MSGT2282), 175,	Messages.getString(MessageIds.GDE_MSGT2241)+GDE.STRING_MESSAGE_CONCAT+Messages.getString(MessageIds.GDE_MSGT2240), 220, UltraDuoPlusDialog.offOnType, 50, 150) : this.memoryParameters[16]; //$NON-NLS-1$ 
				this.memoryParameters[21] = this.memoryParameters[21] != null ? this.memoryParameters[21].dispose() : null;
				this.memoryParameters[26] = this.memoryParameters[26] != null ? this.memoryParameters[26].dispose() : null;
				this.memorySelectHeight = 25 * 26;
				break;
			}
			this.memoryComposite.setSize(this.scrolledMemoryComposite.getClientArea().width, this.memorySelectHeight);
			this.memoryComposite.layout(true);
			this.lastCellSelectionIndex = this.memoryValues[0];
		}
		log.log(Level.FINEST, GDE.STRING_EXIT);
	}

	/**
	 * save ultra duo plus configuration data at given full qualified file path
	 * @param fullQualifiedFilePath
	 */
	private void saveConfigUDP(String fullQualifiedFilePath) {
		try {
			if (this.ultraDuoPlusSetup != null) {
				new UltraDuoPlusSychronizer(this, serialPort, ultraDuoPlusSetup, SYNC_TYPE.WRITE).run();
				
				this.ultraDuoPlusSetup.changed = null;
				//remove synchronized flag
				this.ultraDuoPlusSetup.getChannelData1().synced = null;
				this.ultraDuoPlusSetup.getChannelData2().synced = null;
				Iterator<MemoryType> iterator = this.ultraDuoPlusSetup.getMemory().iterator();
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
				Iterator<TireHeaterData> tireIterator = this.ultraDuoPlusSetup.getTireHeaterData().iterator();
				for (int i = 1; tireIterator.hasNext(); ++i) {
					TireHeaterData tireHeater = tireIterator.next();
					tireHeater.synced = null;
					tireHeater.changed = null;
				}
				Iterator<MotorRunData> motorIterator = this.ultraDuoPlusSetup.getMotorRunData().iterator();
				for (int i = 1; motorIterator.hasNext(); ++i) {
					MotorRunData motorRunData = motorIterator.next();
					motorRunData.synced = null;
					motorRunData.changed = null;
				}

				// store back manipulated XML
				Marshaller marshaller = this.jc.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.valueOf(true));
				marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, UltraDuoPlusDialog.ULTRA_DUO_PLUS_XSD);
				marshaller.marshal(this.ultraDuoPlusSetup, new FileOutputStream(fullQualifiedFilePath));
				log.log(Level.TIME, "write memory setup XML time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.application.openMessageDialogAsync(this.dialogShell != null && !this.dialogShell.isDisposed() ? this.dialogShell : null,
					Messages.getString(gde.messages.MessageIds.GDE_MSGE0007, new String[] { e.getMessage() }));
		}
	}

	/**
	 * enable or disable backup and restore buttons, disable while synchronizing setup configuration with device
	 * @param enable
	 */
	public void setBackupRetoreButtons(final boolean enable) {
		if (!this.dialogShell.isDisposed()) {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					if (UltraDuoPlusDialog.this.restoreButton != null && UltraDuoPlusDialog.this.backupButton != null && !UltraDuoPlusDialog.this.restoreButton.isDisposed()
							&& !UltraDuoPlusDialog.this.backupButton.isDisposed()) {
						UltraDuoPlusDialog.this.restoreButton.setEnabled(enable);
						UltraDuoPlusDialog.this.backupButton.setEnabled(enable);
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
		if (!this.ultraDuoPlusSetup.getIdentifierName().equals(tmpUltraDuoPlusSetup.getIdentifierName())) {
			this.deviceIdentifierName = (tmpUltraDuoPlusSetup.getIdentifierName() + UltraDuoPlusDialog.STRING_16_BLANK).substring(0, 16);
			this.ultraDuoPlusSetup.setIdentifierName(this.deviceIdentifierName);
			this.ultraDuoPlusSetup.setChanged(true);
			this.userNameText.setText(this.deviceIdentifierName);
		}

		//channel base setup
		if (this.ultraDuoPlusSetup.getChannelData1().getValue().equals(tmpUltraDuoPlusSetup.getChannelData1().getValue())) {
			this.ultraDuoPlusSetup.getChannelData1().setValue(tmpUltraDuoPlusSetup.getChannelData1().getValue());
			this.ultraDuoPlusSetup.getChannelData1().setChanged(true);
			this.device.convert2IntArray(this.channelValues1, this.ultraDuoPlusSetup.channelData1.getValue());
			for (int i = 0; i < UltramatSerialPort.SIZE_CHANNEL_1_SETUP; i++) {
				if (this.channelParameters[i] != null) {
					this.channelParameters[i].setSliderSelection(this.channelValues1[i]);
				}
			}
		}
		if (this.ultraDuoPlusSetup.getChannelData2().getValue().equals(tmpUltraDuoPlusSetup.getChannelData2().getValue())) {
			this.ultraDuoPlusSetup.getChannelData2().setValue(tmpUltraDuoPlusSetup.getChannelData2().getValue());
			this.ultraDuoPlusSetup.getChannelData2().setChanged(true);
			this.device.convert2IntArray(this.channelValues2, this.ultraDuoPlusSetup.channelData2.getValue());
			for (int i = 0; i < UltramatSerialPort.SIZE_CHANNEL_2_SETUP; i++) {
				if (this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + i] != null) {
					this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + i].setSliderSelection(this.channelValues2[i]);
				}
			}
		}

		//battery memories
		List<MemoryType> cellMemories = this.ultraDuoPlusSetup.getMemory();
		List<MemoryType> tmpCellMemories = tmpUltraDuoPlusSetup.getMemory();
		Iterator<MemoryType> iterator = cellMemories.iterator();
		Iterator<MemoryType> tmpIterator = tmpCellMemories.iterator();
		for (int i = 1; iterator.hasNext(); ++i) {
			MemoryType cellMemory = iterator.next();
			MemoryType tmpCellMemory = tmpIterator.next();
			//memory name
			if (!cellMemory.getName().equals(tmpCellMemory.getName())) {
				cellMemory.setName((tmpCellMemory.getName() + UltraDuoPlusDialog.STRING_16_BLANK).substring(0, 16));
				cellMemory.setChanged(true);
			}

			//memory setup data
			if (tmpCellMemory.getSetupData() != null && cellMemory.getSetupData() != null && !cellMemory.getSetupData().getValue().equals(tmpCellMemory.getSetupData().getValue())) {
				cellMemory.getSetupData().setValue(tmpCellMemory.getSetupData().getValue());
				cellMemory.getSetupData().setChanged(true);
			}
			//memory step charge data
			if (tmpCellMemory.getStepChargeData() == null && cellMemory.getStepChargeData() != null) {
				cellMemory.setTraceData(null);
			}
			if (tmpCellMemory.getStepChargeData() != null && cellMemory.getStepChargeData() == null) {
				cellMemory.setStepChargeData(new ObjectFactory().createMemoryTypeStepChargeData());
				cellMemory.getStepChargeData().setValue(tmpCellMemory.getStepChargeData().getValue());
				cellMemory.getStepChargeData().setChanged(true);
			}
			if (tmpCellMemory.getStepChargeData() != null && cellMemory.getStepChargeData() != null && !cellMemory.getStepChargeData().getValue().equals(tmpCellMemory.getStepChargeData().getValue())) {
				cellMemory.getStepChargeData().setValue(tmpCellMemory.getStepChargeData().getValue());
				cellMemory.getStepChargeData().setChanged(true);
			}

			//memory trace data
			if (tmpCellMemory.getTraceData() == null && cellMemory.getTraceData() != null) {
				cellMemory.setTraceData(null);
			}
			if (tmpCellMemory.getTraceData() != null && cellMemory.getTraceData() == null) {
				cellMemory.setTraceData(new ObjectFactory().createMemoryTypeTraceData());
				cellMemory.getTraceData().setValue(tmpCellMemory.getTraceData().getValue());
				cellMemory.getTraceData().setChanged(true);
			}
			if (tmpCellMemory.getTraceData() != null && cellMemory.getTraceData() != null && !cellMemory.getTraceData().getValue().equals(tmpCellMemory.getTraceData().getValue())) {
				cellMemory.getTraceData().setValue(tmpCellMemory.getTraceData().getValue());
				cellMemory.getTraceData().setChanged(true);
			}

			//memory cycle data
			if (tmpCellMemory.getCycleData() == null && cellMemory.getCycleData() != null) {
				cellMemory.setCycleData(null);
			}
			if (tmpCellMemory.getCycleData() != null && cellMemory.getCycleData() == null) {
				cellMemory.setCycleData(new ObjectFactory().createMemoryTypeCycleData());
				cellMemory.getCycleData().setValue(tmpCellMemory.getCycleData().getValue());
				cellMemory.getCycleData().setChanged(true);
			}
			if (tmpCellMemory.getCycleData() != null && cellMemory.getCycleData() != null && !cellMemory.getCycleData().getValue().equals(tmpCellMemory.getCycleData().getValue())) {
				cellMemory.getCycleData().setValue(tmpCellMemory.getCycleData().getValue());
				cellMemory.getCycleData().setChanged(true);
			}
		}
		updateBatterySetup(this.memoryCombo.getSelectionIndex());

		//tire heater data
		List<TireHeaterData> tireHeaters = this.ultraDuoPlusSetup.getTireHeaterData();
		List<TireHeaterData> tmpTireHeaters = tmpUltraDuoPlusSetup.getTireHeaterData();
		Iterator<TireHeaterData> tireIterator = tireHeaters.iterator();
		Iterator<TireHeaterData> tmpTireIterator = tmpTireHeaters.iterator();
		for (int i = 1; iterator.hasNext(); ++i) {
			TireHeaterData tireHeaterData = tireIterator.next();
			TireHeaterData tmpTireHeaterData = tmpTireIterator.next();

			if (tmpTireHeaterData == null && tireHeaterData != null) {
				tireHeaterData = null;
			}
			if (tmpTireHeaterData != null && tireHeaterData == null) {
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
		List<MotorRunData> motorRunDatas = this.ultraDuoPlusSetup.getMotorRunData();
		List<MotorRunData> tmpMotorRunDatas = tmpUltraDuoPlusSetup.getMotorRunData();
		Iterator<MotorRunData> motorRunIterator = motorRunDatas.iterator();
		Iterator<MotorRunData> tmpMotorRunIterator = tmpMotorRunDatas.iterator();
		for (int i = 1; iterator.hasNext(); ++i) {
			MotorRunData motorRunData = motorRunIterator.next();
			MotorRunData tmpMotorRunData = tmpMotorRunIterator.next();

			if (tmpMotorRunData == null && motorRunData != null) {
				motorRunData = null;
			}
			if (tmpMotorRunData != null && motorRunData == null) {
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
