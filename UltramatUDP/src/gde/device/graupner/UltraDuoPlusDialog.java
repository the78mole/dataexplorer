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
import gde.ui.DataExplorer;
import gde.ui.ParameterConfigControl;
import gde.ui.SWTResourceManager;
import gde.utils.FileUtils;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
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
	static final String				STRING_FORMAT_02D					= "%02d";
	static final String				STRING_FORMAT_02d_s				= "%02d - %s";																																																												//$NON-NLS-1$
	static final String				STRING_16_BLANK						= "                ";																																																								//$NON-NLS-1$
	static final String				ULTRA_DUO_PLUS_XSD				= "UltraDuoPlus_V02.xsd";																																																						//$NON-NLS-1$
	static final String				UDP_CONFIGURATION_SUFFIX	= "/UltraDuoPlus_";																																																									//$NON-NLS-1$

	private Text							userNameText;
	private Group							baseDeviceSetupGroup, baseDeviceSetupGroup1, baseDeviceSetupGroup2;
	private CLabel						userLabel;
	private Button						restoreButton;
	private Button						backupButton;
	private Button						closeButton;
	private Button						helpButton;
	private Button						copyButton;
	private CTabFolder				mainTabFolder, chargeTypeTabFolder;	
	private CTabItem					setupTabItem, memorySetupTabItem, chargeTabItem, dischargeTabItem;
	private ScrolledComposite	scrolledchargeComposite;
	Composite									boundsComposite, deviceComposite, dischargeCycleComposite;
	Group											chargeGroup, dischargeGroup, cycleGroup;

	private Composite					memoryBoundsComposite, memorySelectComposite;
	private CLabel						memorySelectLabel;
	private CCombo						memoryCombo;


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

	static int                       numberMemories						= 60;
	String[]									memoryNames								= new String[numberMemories];
	int[]											memoryValues							= new int[UltramatSerialPort.SIZE_MEMORY_SETUP];
	int[]											memoryValuesNiCd					= new int[UltramatSerialPort.SIZE_MEMORY_SETUP];
	int[]											memoryValuesNiMh					= new int[UltramatSerialPort.SIZE_MEMORY_SETUP];
	int[]											memoryValuesLiIo					= new int[UltramatSerialPort.SIZE_MEMORY_SETUP];
	int[]											memoryValuesLiPo					= new int[UltramatSerialPort.SIZE_MEMORY_SETUP];
	int[]											memoryValuesLiFe					= new int[UltramatSerialPort.SIZE_MEMORY_SETUP];
	int[]											memoryValuesPb 						= new int[UltramatSerialPort.SIZE_MEMORY_SETUP];
	ParameterConfigControl[]	memoryParameters					= new ParameterConfigControl[UltramatSerialPort.SIZE_MEMORY_SETUP];
	int												lastMemorySelectionIndex	= -1;
	int												lastCellSelectionIndex		= -1;
	int 											parameterSelectHeight			= 30;
	int												chargeSelectHeight				= 11 * parameterSelectHeight;
	int												dischargeSelectHeight			= 5 * parameterSelectHeight;
	int												cycleSelectHeight					= 4 * parameterSelectHeight;

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
							if (cellMemories.size() < numberMemories) { // initially create only base setup data
								for (int i = 0; i < numberMemories; i++) {
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
				if (UltraDuoPlusDialog.this.lastMemorySelectionIndex >= 0 && UltraDuoPlusDialog.this.lastMemorySelectionIndex < numberMemories) {
					if (UltraDuoPlusDialog.this.ultraDuoPlusSetup != null) {
						log.log(java.util.logging.Level.FINE, "memoryComposite.handleEvent, (" + UltraDuoPlusDialog.this.lastMemorySelectionIndex + GDE.STRING_RIGHT_PARENTHESIS + UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getName() + " memoryValues[" + evt.index + "] changed"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
						UltraDuoPlusDialog.this.ultraDuoPlusSetup.getMemory().get(UltraDuoPlusDialog.this.lastMemorySelectionIndex).getSetupData().setChanged(true);
					}
					
					if(evt.index == 0) {
						updateBatteryMemoryParameter(memoryValues[0]);
					}
					else if(evt.index == 2) { // capacity change
						updateBatteryParameterValues();
					}
					if (UltraDuoPlusDialog.this.ultraDuoPlusSetup != null && log.isLoggable(java.util.logging.Level.FINE)) {
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
		long openStartTime = new Date().getTime();
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
					//return;
				}
			}
			else {
				log.log(java.util.logging.Level.SEVERE, "serial port == null"); //$NON-NLS-1$
				this.application.openMessageDialogAsync(null, Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
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
				this.dialogShell.setSize(655, 655);
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
									//this.channelParameters[10] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 10, Messages.getString(MessageIds.GDE_MSGT2298), 175,	"1 ~ 31", 175, false, 50, 150, 1, 31); //$NON-NLS-1$ 
									//this.channelParameters[11] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 11, Messages.getString(MessageIds.GDE_MSGT2299), 175,	"1 ~ 12", 175, false, 50, 150, 1, 12); //$NON-NLS-1$ 
									//this.channelParameters[12] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 12, "%02d", Messages.getString(MessageIds.GDE_MSGT2300), 175,	"0 ~ 99", 175, false, 50, 150, 0, 99); //$NON-NLS-1$ 
									//this.channelParameters[13] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 13, "%02d", Messages.getString(MessageIds.GDE_MSGT2302), 175,	"0 ~ 12", 175, false, 50, 150, 0, 12); //$NON-NLS-1$ 
									//this.channelParameters[14] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 14, "%02d", Messages.getString(MessageIds.GDE_MSGT2301), 175,	"0 ~ 59", 175, false, 50, 150, 0, 59); //$NON-NLS-1$
									//set the date to sync with PC time
									String[] date = new SimpleDateFormat("yy:MM:dd:hh:mm").format(new Date().getTime()).split(GDE.STRING_COLON); //$NON-NLS-1$
									UltraDuoPlusDialog.this.channelValues1[10] = Integer.parseInt(date[0]);
									UltraDuoPlusDialog.this.channelValues1[11] = Integer.parseInt(date[1]);
									UltraDuoPlusDialog.this.channelValues1[12] = Integer.parseInt(date[2]);
									UltraDuoPlusDialog.this.channelValues1[13] = Integer.parseInt(date[3]);
									UltraDuoPlusDialog.this.channelValues1[14] = Integer.parseInt(date[4]);
									ChannelData1 value = new ChannelData1();
									value.setValue(device.convert2String(channelValues1));
									ultraDuoPlusSetup.setChannelData1(value);
									UltraDuoPlusDialog.this.synchronizerWrite = new UltraDuoPlusSychronizer(UltraDuoPlusDialog.this, UltraDuoPlusDialog.this.serialPort, UltraDuoPlusDialog.this.ultraDuoPlusSetup,	UltraDuoPlusSychronizer.SYNC_TYPE.WRITE);
									UltraDuoPlusDialog.this.synchronizerWrite.start();
									UltraDuoPlusDialog.this.synchronizerWrite.join();
								}
								catch (Exception e) {
									e.printStackTrace();
								}
								saveConfigUDP(UltraDuoPlusDialog.this.settings.getApplHomePath() + UltraDuoPlusDialog.UDP_CONFIGURATION_SUFFIX	+ UltraDuoPlusDialog.this.deviceIdentifierName.replace(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR) + GDE.FILE_ENDING_DOT_XML);
								
								UltraDuoPlusDialog.this.serialPort.write(UltramatSerialPort.RESET_END);
							}
							catch (Exception e) {
								e.printStackTrace();
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
						this.mainTabFolder = new CTabFolder(this.boundsComposite, SWT.BORDER);
						this.mainTabFolder.setSimple(false);
						FormData TabFolderLData = new FormData();
						TabFolderLData.left = new FormAttachment(0, 1000, 0);
						TabFolderLData.right = new FormAttachment(1000, 1000, 0);
						TabFolderLData.top = new FormAttachment(0, 1000, 35);
						TabFolderLData.bottom = new FormAttachment(1000, 1000, -45);
						this.mainTabFolder.setLayoutData(TabFolderLData);
						{
							this.setupTabItem = new CTabItem(this.mainTabFolder, SWT.BORDER);
							this.setupTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.setupTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2290));
								{
									this.deviceComposite = new Composite(mainTabFolder, SWT.BORDER);
									GridLayout deviceCompositeLayout = new GridLayout();
									deviceCompositeLayout.makeColumnsEqualWidth = true;
									this.deviceComposite.setLayout(deviceCompositeLayout);
									this.setupTabItem.setControl(this.deviceComposite);
									{
										this.baseDeviceSetupGroup = new Group(this.deviceComposite, SWT.NONE);
										GridData group1LData = new GridData();
										group1LData.horizontalAlignment = GridData.FILL;
										group1LData.verticalAlignment = GridData.BEGINNING;
										group1LData.widthHint = 580;
										group1LData.heightHint = 182;
										this.baseDeviceSetupGroup.setLayoutData(group1LData);
										FillLayout baseDeviceSetupCompositeLayout = new FillLayout(SWT.VERTICAL);
										this.baseDeviceSetupGroup.setLayout(baseDeviceSetupCompositeLayout);
										this.baseDeviceSetupGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.baseDeviceSetupGroup.setText(Messages.getString(MessageIds.GDE_MSGT2291));
	
										//new ParameterHeaderControl(this.baseDeviceSetupComposite, Messages.getString(MessageIds.GDE_MSGT2247), 175, Messages.getString(MessageIds.GDE_MSGT2248), 50, Messages.getString(MessageIds.GDE_MSGT2249), 175, 20);
										this.channelParameters[4] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 4, Messages.getString(MessageIds.GDE_MSGT2293), 175,	"°C - °F", 175, UltraDuoPlusDialog.temperatureDegreeType, 50, 150); //$NON-NLS-1$ 
										this.channelParameters[5] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 5, Messages.getString(MessageIds.GDE_MSGT2294), 175,	Messages.getString(MessageIds.GDE_MSGT2240)+GDE.STRING_MESSAGE_CONCAT+Messages.getString(MessageIds.GDE_MSGT2241), 175, UltraDuoPlusDialog.onOffType, 50, 150); //$NON-NLS-1$ 
										this.channelParameters[6] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 6, Messages.getString(MessageIds.GDE_MSGT2295), 175,	"En - De - Fr - It", 175, UltraDuoPlusDialog.languageTypes, 50, 150); //$NON-NLS-1$ 
										//channelParameters[7] = new ParameterConfigControl(baseDeviceSetupComposite, channelValues1, 7, "PC setup", 175, "DISABLE | ENABLE", 175, diableEnableType, 50, 150);
										this.channelParameters[8] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 8, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2296), 175,	"120 ~ 150 (12.0 ~ 15.0V)", 175, true, 50, 150, 120, 150, -100); //$NON-NLS-1$ 
										this.channelParameters[9] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 9, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2297), 175,	"50 ~ 400 (5 ~ 40A)", 175, true, 50, 150, 50, 400, -50); //$NON-NLS-1$ 
										//this.channelParameters[10] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 10, Messages.getString(MessageIds.GDE_MSGT2298), 175,	"1 ~ 31", 175, false, 50, 150, 1, 31); //$NON-NLS-1$ 
										//this.channelParameters[11] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 11, Messages.getString(MessageIds.GDE_MSGT2299), 175,	"1 ~ 12", 175, false, 50, 150, 1, 12); //$NON-NLS-1$ 
										//this.channelParameters[12] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 12, "%02d", Messages.getString(MessageIds.GDE_MSGT2300), 175,	"0 ~ 99", 175, false, 50, 150, 0, 99); //$NON-NLS-1$ 
										//this.channelParameters[13] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 13, "%02d", Messages.getString(MessageIds.GDE_MSGT2302), 175,	"0 ~ 12", 175, false, 50, 150, 0, 12); //$NON-NLS-1$ 
										//this.channelParameters[14] = new ParameterConfigControl(this.baseDeviceSetupComposite, this.channelValues1, 14, "%02d", Messages.getString(MessageIds.GDE_MSGT2301), 175,	"0 ~ 59", 175, false, 50, 150, 0, 59); //$NON-NLS-1$ 
										this.channelParameters[15] = new ParameterConfigControl(this.baseDeviceSetupGroup, this.channelValues1, 15, Messages.getString(MessageIds.GDE_MSGT2303), 175,	"12H - 24H", 175, UltraDuoPlusDialog.hourFormatType, 50, 150); //$NON-NLS-1$ 
									}
									{
										this.baseDeviceSetupGroup1 = new Group(this.deviceComposite, SWT.NONE);
										GridData group2LData = new GridData();
										group2LData.verticalAlignment = GridData.BEGINNING;
										group2LData.horizontalAlignment = GridData.CENTER;
										group2LData.widthHint = 600;
										group2LData.heightHint = 130;
										this.baseDeviceSetupGroup1.setLayoutData(group2LData);
										FillLayout composite1Layout = new FillLayout(SWT.VERTICAL);
										this.baseDeviceSetupGroup1.setLayout(composite1Layout);
										this.baseDeviceSetupGroup1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.baseDeviceSetupGroup1.setText(Messages.getString(MessageIds.GDE_MSGT2304));
	
										//new ParameterHeaderControl(this.baseDeviceSetupComposite1, Messages.getString(MessageIds.GDE_MSGT2247), 175, Messages.getString(MessageIds.GDE_MSGT2248), 50,	Messages.getString(MessageIds.GDE_MSGT2249), 175, 20);
										this.channelParameters[0] = new ParameterConfigControl(this.baseDeviceSetupGroup1, this.channelValues1, 0, Messages.getString(MessageIds.GDE_MSGT2306), 175, Messages.getString(MessageIds.GDE_MSGT2313), 175, UltraDuoPlusDialog.soundTime, 50, 150); //$NON-NLS-1$ 
										this.channelParameters[1] = new ParameterConfigControl(this.baseDeviceSetupGroup1, this.channelValues1, 1, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2307), 175, "1 ~ 10", 175, false, 50, 150, 1, 10); //$NON-NLS-1$ 
										this.channelParameters[2] = new ParameterConfigControl(this.baseDeviceSetupGroup1, this.channelValues1, 2, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2305), 175, "1 ~ 15", 175, false, 50, 150, 1, 15); //$NON-NLS-1$ 
										this.channelParameters[3] = new ParameterConfigControl(this.baseDeviceSetupGroup1, this.channelValues1, 3, Messages.getString(MessageIds.GDE_MSGT2308), 175, Messages.getString(MessageIds.GDE_MSGT2244)+GDE.STRING_MESSAGE_CONCAT+Messages.getString(MessageIds.GDE_MSGT2245), 175, UltraDuoPlusDialog.powerOnDisplayType, 50, 150); //$NON-NLS-1$ 
									}
									{
										this.baseDeviceSetupGroup2 = new Group(this.deviceComposite, SWT.NONE);
										GridData group3LData = new GridData();
										group3LData.verticalAlignment = GridData.BEGINNING;
										group3LData.horizontalAlignment = GridData.CENTER;
										group3LData.widthHint = 600;
										group3LData.heightHint = 130;
										this.baseDeviceSetupGroup2.setLayoutData(group3LData);
										FillLayout composite1Layout = new FillLayout(SWT.VERTICAL);
										this.baseDeviceSetupGroup2.setLayout(composite1Layout);
										this.baseDeviceSetupGroup2.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.baseDeviceSetupGroup2.setText(Messages.getString(MessageIds.GDE_MSGT2309));
	
										//new ParameterHeaderControl(this.baseDeviceSetupComposite2, Messages.getString(MessageIds.GDE_MSGT2247), 175, Messages.getString(MessageIds.GDE_MSGT2248), 50,	Messages.getString(MessageIds.GDE_MSGT2249), 175, 20);
										this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + 0] = new ParameterConfigControl(this.baseDeviceSetupGroup2, this.channelValues2, 0, Messages.getString(MessageIds.GDE_MSGT2255), 175, Messages.getString(MessageIds.GDE_MSGT2313), 175, UltraDuoPlusDialog.soundTime, 50, 150); //$NON-NLS-1$ 
										this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + 1] = new ParameterConfigControl(this.baseDeviceSetupGroup2, this.channelValues2, 1, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2254), 175, "1 ~ 10", 175, false, 50, 150, 1, 10); //$NON-NLS-1$ 
										this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + 2] = new ParameterConfigControl(this.baseDeviceSetupGroup2, this.channelValues2, 2, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2305), 175, "1 ~ 15", 175, false, 50, 150, 1, 15); //$NON-NLS-1$ 
										this.channelParameters[UltramatSerialPort.SIZE_CHANNEL_1_SETUP + 3] = new ParameterConfigControl(this.baseDeviceSetupGroup2, this.channelValues2, 3, Messages.getString(MessageIds.GDE_MSGT2308), 175, Messages.getString(MessageIds.GDE_MSGT2244)+GDE.STRING_MESSAGE_CONCAT+Messages.getString(MessageIds.GDE_MSGT2245), 175, UltraDuoPlusDialog.powerOnDisplayType, 50, 150); //$NON-NLS-1$ 
									}
								}
						}
						{
							this.memorySetupTabItem = new CTabItem(this.mainTabFolder, SWT.NONE);
							this.memorySetupTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.memorySetupTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2250));
							{
								this.memoryBoundsComposite = new Composite(this.mainTabFolder, SWT.NONE);
								this.memorySetupTabItem.setControl(this.memoryBoundsComposite);
								this.memoryBoundsComposite.setLayout(new FormLayout());
								{
									this.memorySelectComposite = new Composite(this.memoryBoundsComposite, SWT.NONE);
									FormData memorySelectLData = new FormData();
									memorySelectLData.height = 150;
									memorySelectLData.left = new FormAttachment(0, 1000, 0);
									memorySelectLData.right = new FormAttachment(1000, 1000, 0);
									memorySelectLData.top = new FormAttachment(0, 1000, 0);
									this.memorySelectComposite.setLayoutData(memorySelectLData);
									RowLayout composite2Layout = new RowLayout(SWT.HORIZONTAL);
									this.memorySelectComposite.setLayout(composite2Layout);
									this.memorySelectComposite.setBackground(DataExplorer.COLOR_CANVAS_YELLOW);
									{
										this.memorySelectLabel = new CLabel(this.memorySelectComposite, SWT.RIGHT);
										RowData memorySelectLabelLData = new RowData();
										memorySelectLabelLData.width = 120;
										memorySelectLabelLData.height = 20;
										this.memorySelectLabel.setLayoutData(memorySelectLabelLData);
										this.memorySelectLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.memorySelectLabel.setText(Messages.getString(MessageIds.GDE_MSGT2251));
										this.memorySelectLabel.setBackground(DataExplorer.COLOR_CANVAS_YELLOW);
									}
									{
										updateBatterySetup(1);
										this.memoryCombo = new CCombo(this.memorySelectComposite, SWT.BORDER);
										this.memoryCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.memoryCombo.setItems(this.memoryNames);
										this.memoryCombo.setVisibleItemCount(20);
										RowData memoryComboLData = new RowData();
										memoryComboLData.width = 165;
										memoryComboLData.height = GDE.IS_WINDOWS ? 16 : 18;
										this.memoryCombo.setLayoutData(memoryComboLData);
										this.memoryCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2252));
										this.memoryCombo.select(0);
										this.memoryCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
										this.memoryCombo.setEditable(true);
										this.memoryCombo.addSelectionListener(new SelectionAdapter() {
											@Override
											public void widgetSelected(SelectionEvent evt) {
												log.log(Level.FINEST, "memoryCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
												try {
													int actualSelectionIndex = UltraDuoPlusDialog.this.memoryCombo.getSelectionIndex();
													if (UltraDuoPlusDialog.this.lastMemorySelectionIndex != actualSelectionIndex) {
														if (UltraDuoPlusDialog.this.ultraDuoPlusSetup != null && UltraDuoPlusDialog.this.lastMemorySelectionIndex >= 0 && UltraDuoPlusDialog.this.lastMemorySelectionIndex < numberMemories) {
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
														//UltraDuoPlusDialog.this.memoryCombo.setItems(UltraDuoPlusDialog.this.memoryNames);
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
										filler.setLayoutData(new RowData(120, 20));
										filler.setText("<---------   "); //$NON-NLS-1$
										filler.setBackground(DataExplorer.COLOR_CANVAS_YELLOW);
									}
									{
										this.copyButton = new Button(this.memorySelectComposite, SWT.CHECK | SWT.LEFT);
										RowData cellTypeSelectLabelLData = new RowData();
										cellTypeSelectLabelLData.width = 200;
										cellTypeSelectLabelLData.height = 20;
										this.copyButton.setLayoutData(cellTypeSelectLabelLData);
										this.copyButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.copyButton.setText(Messages.getString(MessageIds.GDE_MSGT2288));
										this.copyButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2256));
										this.copyButton.setBackground(DataExplorer.COLOR_CANVAS_YELLOW);
									}
									//cell type
									this.memoryParameters[0] = new ParameterConfigControl(this.memorySelectComposite, this.memoryValues, 0, Messages.getString(MessageIds.GDE_MSGT2257), 175,	Messages.getString(MessageIds.GDE_MSGT2246), 220, UltraDuoPlusDialog.cellTypeNames, 50, 150);
									//number cells
									this.memoryParameters[1] = new ParameterConfigControl(this.memorySelectComposite, this.memoryValues, 1, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2258), 175,	"1 ~ 18", 220, false, 50, 150, 1, 18); //$NON-NLS-1$ 
									//battery capacity
									this.memoryParameters[2] = new ParameterConfigControl(this.memorySelectComposite, this.memoryValues, 2, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2259), 175,	"100 ~ 50000 mAh", 220, true, 50, 150, 100, 50000, -100); //$NON-NLS-1$ 
									//year, month, day
									this.memoryParameters[3] = new ParameterConfigControl(this.memorySelectComposite, this.memoryValues, 3, STRING_FORMAT_02D, Messages.getString(MessageIds.GDE_MSGT2260), 100,	GDE.STRING_EMPTY, 5, false, 50, 70, 0, 99);
									this.memoryParameters[4] = new ParameterConfigControl(this.memorySelectComposite, this.memoryValues, 4, STRING_FORMAT_02D, Messages.getString(MessageIds.GDE_MSGT2261), 50,	GDE.STRING_EMPTY, 5, false, 30, 80, 1, 12);
									this.memoryParameters[5] = new ParameterConfigControl(this.memorySelectComposite, this.memoryValues, 5, STRING_FORMAT_02D, Messages.getString(MessageIds.GDE_MSGT2262), 43,	GDE.STRING_EMPTY, 5, false, 30, 80, 1, 31);
									//new ParameterHeaderControl(this.memorySelectComposite, Messages.getString(MessageIds.GDE_MSGT2247), 175, Messages.getString(MessageIds.GDE_MSGT2248), 50,	Messages.getString(MessageIds.GDE_MSGT2249), 180, 20);
								}
								{
									this.chargeTypeTabFolder = new CTabFolder(this.memoryBoundsComposite, SWT.BORDER);
									this.chargeTypeTabFolder.setSimple(false);
									FormData chargeTypeTabFolderLData = new FormData();
									chargeTypeTabFolderLData.left = new FormAttachment(0, 1000, 0);
									chargeTypeTabFolderLData.right = new FormAttachment(1000, 1000, 0);
									chargeTypeTabFolderLData.bottom = new FormAttachment(1000, 1000, 0);
									chargeTypeTabFolderLData.top = new FormAttachment(0, 1000, 150);
									this.chargeTypeTabFolder.setLayoutData(chargeTypeTabFolderLData);
									this.chargeTypeTabFolder.addSelectionListener(new SelectionAdapter() {
										@Override
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "restoreButton.widgetSelected, event=" + evt); //$NON-NLS-1$
											UltraDuoPlusDialog.this.lastCellSelectionIndex = -1;
											updateBatteryMemoryParameter(memoryValues[0]);
										}
									});
									{
										this.chargeTabItem = new CTabItem(chargeTypeTabFolder, SWT.NONE);
										this.chargeTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.chargeTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2298));
										{
											this.scrolledchargeComposite = new ScrolledComposite(this.chargeTypeTabFolder, SWT.BORDER | SWT.V_SCROLL);
											FillLayout scrolledMemoryCompositeLayout = new FillLayout();
											this.scrolledchargeComposite.setLayout(scrolledMemoryCompositeLayout);
											this.chargeTabItem.setControl(scrolledchargeComposite);
											{
												this.chargeGroup = new Group(this.scrolledchargeComposite, SWT.NONE);
												FillLayout memoryCompositeLayout = new FillLayout(SWT.VERTICAL);
												this.chargeGroup.setLayout(memoryCompositeLayout);
												this.chargeGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
												this.chargeGroup.setText(Messages.getString(MessageIds.GDE_MSGT2299));
												//charge parameter
												this.memoryParameters[6] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 6, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2263), 175,	"100 ~ 20000 mA", 220, true, 50, 150, 100, 20000, -100); //$NON-NLS-1$ 
												this.memoryParameters[11] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 11, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2265), 175,	Messages.getString(MessageIds.GDE_MSGT2310), 220, true, 50, 150, 10, 155); //$NON-NLS-1$ 
												this.memoryParameters[10] = this.channelValues1[4] == 0 //°C
													? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 10, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2264), 175,	"10 ~ 80°C", 220, false, 50, 150, 10, 80) //$NON-NLS-1$ 
													:	new ParameterConfigControl(this.chargeGroup, this.memoryValues, 10, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2264), 175,	"50 ~ 176°F", 220, false, 50, 150, 50, 176); //$NON-NLS-1$ 
												this.memoryParameters[12] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 12, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2266), 175,	"10 ~ 905min (905=off)", 220, false, 50, 150, 10, 905); //$NON-NLS-1$ 
												this.memoryParameters[14] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 14, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2267), 175,	"1000 ~ 4300 mV", 220, true, 50, 150, 1000, 4300, -1000); //$NON-NLS-1$ 
												this.memoryParameters[9] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 9, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2279), 175,	Messages.getString(MessageIds.GDE_MSGT2312), 220, false, 50, 150, 0, 500); //$NON-NLS-1$ 
												this.memoryParameters[26] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 26, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2276), 175,	"1000 ~ 4000 mV", 220, true, 50, 150, 1000, 4000, -1000); //$NON-NLS-1$ 
												this.memoryParameters[7] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 7, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2277), 175,	"0 ~ 25mV", 220, false, 50, 150, 0, 25); //$NON-NLS-1$ 
												this.memoryParameters[8] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 8, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2278), 175,	"1 ~ 20min", 220, false, 50, 150, 1, 20); //$NON-NLS-1$ 
												this.memoryParameters[15] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 15, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2281), 175,	"1 ~ 30min", 220, false, 50, 150, 1, 30); //$NON-NLS-1$ 
												this.memoryParameters[13] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 13, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2280), 175,	"1 ~ 5", 220, false, 50, 150, 1, 5); //$NON-NLS-1$ 
												this.memoryParameters[16] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 16, Messages.getString(MessageIds.GDE_MSGT2282), 175,	Messages.getString(MessageIds.GDE_MSGT2241)+GDE.STRING_MESSAGE_CONCAT+Messages.getString(MessageIds.GDE_MSGT2240), 220, UltraDuoPlusDialog.offOnType, 50, 150); //$NON-NLS-1$ 
											}
											this.scrolledchargeComposite.setContent(this.chargeGroup);
											this.chargeGroup.setSize(620, this.chargeSelectHeight);
											this.scrolledchargeComposite.addControlListener(new ControlListener() {
												public void controlResized(ControlEvent evt) {
													log.log(Level.FINEST, "scrolledMemoryComposite.controlResized, event=" + evt); //$NON-NLS-1$
													UltraDuoPlusDialog.this.chargeGroup.setSize(UltraDuoPlusDialog.this.scrolledchargeComposite.getClientArea().width, UltraDuoPlusDialog.this.chargeSelectHeight);
												}
												public void controlMoved(ControlEvent evt) {
													log.log(Level.FINEST, "scrolledMemoryComposite.controlMoved, event=" + evt); //$NON-NLS-1$
													UltraDuoPlusDialog.this.chargeGroup.setSize(UltraDuoPlusDialog.this.scrolledchargeComposite.getClientArea().width, UltraDuoPlusDialog.this.chargeSelectHeight);
												}
											});
										}
									}
									{
										this.dischargeTabItem = new CTabItem(chargeTypeTabFolder, SWT.NONE);
										this.dischargeTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
										this.dischargeTabItem.setText(Messages.getString(MessageIds.GDE_MSGT2300));
										{
											this.dischargeCycleComposite = new Composite(this.chargeTypeTabFolder, SWT.BORDER);
											RowLayout scrolledComposite1Layout = new RowLayout(org.eclipse.swt.SWT.VERTICAL);
											this.dischargeCycleComposite.setLayout(scrolledComposite1Layout);
											this.dischargeTabItem.setControl(dischargeCycleComposite);
											{
												this.dischargeGroup = new Group(this.dischargeCycleComposite, SWT.NONE);
												RowData dischargeGroupLData = new RowData(620, UltraDuoPlusDialog.this.dischargeSelectHeight);
												this.dischargeGroup.setLayoutData(dischargeGroupLData);
												FillLayout memoryCompositeLayout = new FillLayout(SWT.VERTICAL);
												this.dischargeGroup.setLayout(memoryCompositeLayout);
												this.dischargeGroup.setText(Messages.getString(MessageIds.GDE_MSGT2301));											
												//discharge parameter
												this.memoryParameters[17] = new ParameterConfigControl(this.dischargeGroup, this.memoryValues, 17, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2268), 175,	"100 ~ 10000 mA", 220, true, 50, 150, 100, 10000, -100); //$NON-NLS-1$ 
												this.memoryParameters[18] = new ParameterConfigControl(this.dischargeGroup, this.memoryValues, 18, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2269), 175,	"100 ~ 4200 mV", 220, true, 50, 150, 100, 4200, -100); //$NON-NLS-1$ 
												this.memoryParameters[20] = new ParameterConfigControl(this.dischargeGroup, this.memoryValues, 20, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2271), 175,	Messages.getString(MessageIds.GDE_MSGT2311), 220, false, 50, 150, 10, 105, -10); //$NON-NLS-1$ 
												this.memoryParameters[19] = this.channelValues1[4] == 0 //°C
													? new ParameterConfigControl(this.dischargeGroup, this.memoryValues, 19, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2270), 175,	"10 ~ 80°C", 220, false, 50, 150, 10, 80) //$NON-NLS-1$ 
													: new ParameterConfigControl(this.dischargeGroup, this.memoryValues, 19, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2270), 175,	"50 ~ 176°F", 220, false, 50, 150, 50, 176); //$NON-NLS-1$ 
												this.memoryParameters[21] = new ParameterConfigControl(this.dischargeGroup, this.memoryValues, 21, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2283), 175,	"1100 ~ 1300 mV", 220, true, 50, 150, 1100, 1300, -1100); //$NON-NLS-1$
											}
											{
												this.cycleGroup = new Group(this.dischargeCycleComposite, SWT.NONE);
												RowData cycleGroupLData = new RowData(620, UltraDuoPlusDialog.this.cycleSelectHeight);
												this.cycleGroup.setLayoutData(cycleGroupLData);
												FillLayout memoryCompositeLayout = new FillLayout(SWT.VERTICAL);
												this.cycleGroup.setLayout(memoryCompositeLayout);
												this.cycleGroup.setText(Messages.getString(MessageIds.GDE_MSGT2302));											
												//cycle parameter
												this.memoryParameters[22] = new ParameterConfigControl(this.cycleGroup, this.memoryValues, 22, Messages.getString(MessageIds.GDE_MSGT2272), 175,	Messages.getString(MessageIds.GDE_MSGT2292), 220, UltraDuoPlusDialog.cycleDirectionTypes, 50, 150);
												this.memoryParameters[23] = new ParameterConfigControl(this.cycleGroup, this.memoryValues, 23, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2273), 175,	"1 ~ 10", 220, false, 50, 150, 1, 10); //$NON-NLS-1$ 
												this.memoryParameters[24] = new ParameterConfigControl(this.cycleGroup, this.memoryValues, 24, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2274), 175,	"1 ~ 30min", 220, false, 50, 150, 1, 30); //$NON-NLS-1$ 
												this.memoryParameters[25] = new ParameterConfigControl(this.cycleGroup, this.memoryValues, 25, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2275), 175,	"1 ~ 30min", 220, false, 50, 150, 1, 30); //$NON-NLS-1$ 
											}
										}
									}
									this.chargeTypeTabFolder.setSelection(0);
								}
							}
						}
						this.mainTabFolder.setSelection(1);
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
											if (cellMemories.size() < numberMemories) { // initially create only base setup data
												for (int i = 0; i < numberMemories; i++) {
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
			log.log(Level.TIME, "open dialog time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - openStartTime))); //$NON-NLS-1$ //$NON-NLS-2$
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
		if (cellMemories.size() < numberMemories) { // initially create only base setup data
			for (int i = 0; i < numberMemories; i++) {
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
		if (this.ultraDuoPlusSetup != null) {
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

			log.log(Level.FINEST, "add handler"); //$NON-NLS-1$
			//don't need a change listener handler for baseDeviceSetupGroup and baseDeviceSetupGroup1, it will always written to sync date and time
			this.baseDeviceSetupGroup2.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event evt) {
					log.log(Level.FINEST, "baseDeviceSetupComposite2.handleEvent, channelValues2[" + evt.index + "] changed"); //$NON-NLS-1$ //$NON-NLS-2$
					ChannelData2 value = new ChannelData2();
					value.setValue(device.convert2String(channelValues2));
					ultraDuoPlusSetup.setChannelData2(value);
					UltraDuoPlusDialog.this.ultraDuoPlusSetup.getChannelData2().setChanged(true);
				}
			});
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
			if (this.chargeGroup != null && !this.chargeGroup.isDisposed()) {
				log.log(Level.FINEST, "remove event handler"); //$NON-NLS-1$
				this.memorySelectComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
				this.chargeGroup.removeListener(SWT.Selection, this.memoryParameterChangeListener);
				this.dischargeGroup.removeListener(SWT.Selection, this.memoryParameterChangeListener);
				this.cycleGroup.removeListener(SWT.Selection, this.memoryParameterChangeListener);
			}

			if (this.ultraDuoPlusSetup != null && this.ultraDuoPlusSetup.getMemory() != null) {
				int i = 0;
				while (i < numberMemories) {
					if (this.ultraDuoPlusSetup.getMemory().get(i).isSynced())
						this.memoryNames[i] = String.format(UltraDuoPlusDialog.STRING_FORMAT_02d_s, i + 1, this.ultraDuoPlusSetup.getMemory().get(i).getName());
					else if (this.ultraDuoPlusSetup.getMemory().get(i) != null && this.ultraDuoPlusSetup.getMemory().get(i).getName() != null) {
						this.memoryNames[i] = String.format(UltraDuoPlusDialog.STRING_FORMAT_02d_s, i + 1, this.ultraDuoPlusSetup.getMemory().get(i).getName());
					}
					else {
						WaitTimer.delay(100);
						continue;
					}
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "-------> [" + i + "] "+ this.memoryNames[i]); //$NON-NLS-1$ //$NON-NLS-2$
					++i;
				}

				if (this.ultraDuoPlusSetup.getMemory().get(memoryNumber).getSetupData() != null && !this.ultraDuoPlusSetup.getMemory().get(memoryNumber + 1).getSetupData().isSynced()) {
					this.ultraDuoPlusSetup.getMemory().get(memoryNumber).getSetupData().setValue(this.serialPort.readMemorySetup(memoryNumber + 1));
				}
				this.memoryValues = this.device.convert2IntArray(this.memoryValues, this.ultraDuoPlusSetup.getMemory().get(memoryNumber).getSetupData().getValue());
			}
			else {
				for (int i = 0; i < numberMemories; i++) {
					this.memoryNames[i] = String.format("%02d - NEW-BATT-NAME", i + 1); //$NON-NLS-1$
				}
				//some default values
				this.memoryValues[0] = 0;
				this.memoryValues[1] = 4;
				this.memoryValues[2] = 2000;
				this.memoryValues[3] = 0;
				this.memoryValues[4] = 1;
				this.memoryValues[5] = 1;
			}
			if (this.memoryCombo != null && !this.memoryCombo.isDisposed() && this.memoryCombo.getItems().length == 0) 
				this.memoryCombo.setItems(this.memoryNames);

			if (log.isLoggable(Level.FINE)) {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
					sb.append(String.format("%04d", this.memoryValues[i])).append(GDE.STRING_LEFT_BRACKET).append(i).append(GDE.STRING_RIGHT_BRACKET_COMMA);
				}
				log.log(Level.FINE, sb.toString());
			}
			//if (this.memoryParameters[0] != null && !this.memoryParameters[0].getSlider().isDisposed()) 
				updateBatteryMemoryParameter(this.memoryValues[0]);

			for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
				if (this.memoryParameters[i] != null) {
					this.memoryParameters[i].setSliderSelection(this.memoryValues[i]);
				}
			}

			if (this.chargeGroup != null && !this.chargeGroup.isDisposed()) {
				log.log(Level.FINEST, "add event handler"); //$NON-NLS-1$
				this.memorySelectComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
				this.chargeGroup.addListener(SWT.Selection, this.memoryParameterChangeListener);
				this.dischargeGroup.addListener(SWT.Selection, this.memoryParameterChangeListener);
				this.cycleGroup.addListener(SWT.Selection, this.memoryParameterChangeListener);
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
			log.log(Level.FINE, "cell type changed to : " + cellTypeNames[this.memoryValues[0]]); //$NON-NLS-1$
			this.memoryValues[0] = selectionIndex;
			//update memory parameter table to reflect not edit able parameters for selected cell type
			switch (this.memoryValues[0]) {
			case 0: //NiCd
			case 1: //NiMh
				this.memoryParameters[9] = this.memoryParameters[9] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 9, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2279), 175, Messages.getString(MessageIds.GDE_MSGT2312), 220, false, 50, 150, 0, 550) : this.memoryParameters[9]; //$NON-NLS-1$ 
				this.memoryParameters[7] = this.memoryParameters[7] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 7, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2277), 175, "0 ~ 25mV", 220, false, 50, 150, 0, 25) : this.memoryParameters[7]; //$NON-NLS-1$ 
				this.memoryParameters[8] = this.memoryParameters[8] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 8, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2278), 175, "1 ~ 20min", 220, false, 50, 150, 1, 20) : this.memoryParameters[8]; //$NON-NLS-1$ 
				this.memoryParameters[13] = this.memoryParameters[13] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 13, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2280), 175,	"1 ~ 5", 220, false, 50, 150, 1, 5) : this.memoryParameters[13]; //$NON-NLS-1$ 
				this.memoryParameters[14] = this.memoryParameters[14] != null ? this.memoryParameters[14].dispose() : null;
				this.memoryParameters[15] = this.memoryParameters[15] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 15, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2281), 175,	"1 ~ 30min", 220, false, 50, 150, 1, 30) : this.memoryParameters[15]; //$NON-NLS-1$ 
				this.memoryParameters[16] = this.memoryParameters[16] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 16, Messages.getString(MessageIds.GDE_MSGT2282), 175,	Messages.getString(MessageIds.GDE_MSGT2241)+GDE.STRING_MESSAGE_CONCAT+Messages.getString(MessageIds.GDE_MSGT2240), 220, UltraDuoPlusDialog.offOnType, 50, 150) : this.memoryParameters[16]; //$NON-NLS-1$ 
				this.memoryParameters[21] = this.memoryParameters[21] == null ? new ParameterConfigControl(this.dischargeGroup, this.memoryValues, 21, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2283), 175,	"1100 ~ 1300 mV", 220, true, 50, 150, 1100, 1300, -1100) : this.memoryParameters[21]; //$NON-NLS-1$ 
				this.memoryParameters[26] = this.memoryParameters[26] != null ? this.memoryParameters[26].dispose() : null;
				this.chargeSelectHeight = 10 * this.parameterSelectHeight;
				this.dischargeSelectHeight = 5 * this.parameterSelectHeight;
				this.memoryParameters[11].updateValueRange(Messages.getString(MessageIds.GDE_MSGT2310), 10, 155);
				break;
			case 2: //LiIo
			case 3: //LiPo
			case 4: //LiFe
				this.memoryParameters[7] = this.memoryParameters[7] != null ? this.memoryParameters[7].dispose() : null;
				this.memoryParameters[8] = this.memoryParameters[8] != null ? this.memoryParameters[8].dispose() : null;
				this.memoryParameters[9] = this.memoryParameters[9] != null ? this.memoryParameters[9].dispose() : null;
				this.memoryParameters[13] = this.memoryParameters[13] != null ? this.memoryParameters[13].dispose() : null;
				this.memoryParameters[14] = this.memoryParameters[26] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 14, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2267), 175,	"1000 ~ 4300 mV", 220, true, 50, 150, 1000, 4300, -1000) : this.memoryParameters[14]; //$NON-NLS-1$ 
				this.memoryParameters[15] = this.memoryParameters[15] != null ? this.memoryParameters[15].dispose() : null;
				this.memoryParameters[16] = this.memoryParameters[16] != null ? this.memoryParameters[16].dispose() : null;
				this.memoryParameters[21] = this.memoryParameters[21] != null ? this.memoryParameters[21].dispose() : null;
				this.memoryParameters[26] = this.memoryParameters[26] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 26, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2276), 175,	"1000 ~ 4000", 220, true, 50, 150, 1000, 4000, -1000) : this.memoryParameters[26]; //$NON-NLS-1$ 
				this.chargeSelectHeight = 6 * this.parameterSelectHeight + 10;
				this.dischargeSelectHeight = 4 * this.parameterSelectHeight;
				this.memoryParameters[11].updateValueRange(Messages.getString(MessageIds.GDE_MSGT2314), 10, 125);
				break;
			case 5: //Pb
				this.memoryParameters[7] = this.memoryParameters[7] != null ? this.memoryParameters[7].dispose() : null;
				this.memoryParameters[8] = this.memoryParameters[8] != null ? this.memoryParameters[8].dispose() : null;
				this.memoryParameters[9] = this.memoryParameters[9] != null ? this.memoryParameters[9].dispose() : null;
				this.memoryParameters[13] = this.memoryParameters[13] != null ? this.memoryParameters[13].dispose() : null;
				this.memoryParameters[14] = this.memoryParameters[26] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 14, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2267), 175,	"1000 ~ 4300 mV", 220, true, 50, 150, 1000, 4300, -1000) : this.memoryParameters[14]; //$NON-NLS-1$ 
				this.memoryParameters[15] = this.memoryParameters[15] == null ? new ParameterConfigControl(this.chargeGroup, this.memoryValues, 15, GDE.STRING_EMPTY, Messages.getString(MessageIds.GDE_MSGT2281), 175,	"1 ~ 30min", 220, false, 50, 150, 1, 30) : this.memoryParameters[15]; //$NON-NLS-1$ 
				this.memoryParameters[16] = this.memoryParameters[16] != null ? this.memoryParameters[16].dispose() : null;
				this.memoryParameters[21] = this.memoryParameters[21] != null ? this.memoryParameters[21].dispose() : null;
				this.memoryParameters[26] = this.memoryParameters[26] != null ? this.memoryParameters[26].dispose() : null;
				this.chargeSelectHeight = 6 * this.parameterSelectHeight + 10;
				this.dischargeSelectHeight = 4 * this.parameterSelectHeight;
				this.memoryParameters[11].updateValueRange(Messages.getString(MessageIds.GDE_MSGT2310), 10, 155);
				break;
			}
			this.chargeGroup.setSize(this.scrolledchargeComposite.getClientArea().width, this.chargeSelectHeight);
			this.chargeGroup.layout(true);
			this.scrolledchargeComposite.layout(true);
			this.dischargeGroup.setLayoutData(new RowData(this.dischargeCycleComposite.getClientArea().width, this.dischargeSelectHeight));
			this.dischargeGroup.layout(true);
			this.dischargeCycleComposite.layout(true);
			
			updateBatteryParameterValues();
			this.lastCellSelectionIndex = this.memoryValues[0];
		}
		log.log(Level.FINEST, GDE.STRING_EXIT);
	}
	
	/**
	 * update the memory setup parameter values with dependency to cell type, capacity, charge current
	 * set meaningful initial values
	 */
	private void updateBatteryParameterValues() {
		log.log(Level.FINEST, GDE.STRING_ENTRY);
		//0=cellType,1=numCells,2=capacity,3=year,4=month,5=day,
		//6=chargeCurrent,7=deltaPeak,8=preDeltaPeakDelay,9=trickleCurrent,10=chargeOffTemperature,11=chargeMaxCapacity,12=chargeSafetyTimer,13=rePeakCycle,14=chargeVoltage,15=repaekDelay,16=flatLimitCheck,26=storeVoltage
		//17=dischargeCurrent,18=dischargOffVolage,19=dischargeOffTemp,20=dischargemaxCapacity,21=NiMhMatchVoltage
		//22=cycleDirection,23=cycleCount,24=chargeEndDelay,25=dischargeEndDelay
		this.memoryValues[6] = this.memoryValues[2] / 2; //charge current 0.5 C
		this.memoryValues[17] = this.memoryValues[2]; //discharge current 1.0 C
		this.memoryValues[12] = this.memoryValues[2] / 30 ; //chargeSafetyTimer
		this.memoryValues[12] = this.memoryValues[12] - (this.memoryValues[12] % 10) ; //chargeSafetyTimer
		this.memoryValues[20] = 100; //dischargemaxCapacity
		this.memoryValues[22] = 1; //cycleDirection D->C
		this.memoryValues[23] = 1; //cycleCount
		this.memoryValues[24] = this.memoryValues[25] = (int) (this.memoryValues[2] / 440 + 0.5); //chargeEndDelay, dischargeEndDelay
		switch (this.memoryValues[0]) {
		case 0: //NiCd
			this.memoryValues[11] = 120; //chargeMaxCapacity
			this.memoryValues[10] = this.channelValues1[4] == 0 ? 50 : (9/5)*50+32; //chargeOffTemperature
			this.memoryValues[12] = this.memoryValues[12] + 30; //chargeSafetyTimer
			this.memoryValues[9] = this.memoryValues[2] / 10; //trickleCurrent
			this.memoryValues[7] = 7; //deltaPeak mV
			this.memoryValues[8] = 3; //preDeltaPeakDelay min
			this.memoryValues[13] = 1; //rePeakCycle
			this.memoryValues[15] = 3; //rePaekDelay min
			this.memoryValues[7] = 1; //flatLimitCheck
			this.memoryValues[19] = this.channelValues1[4] == 0 ? 45 : (9/5)*45+32; //dischargeOffTemperature
			break;
		case 1: //NiMh
			this.memoryValues[11] = 120; //chargeMaxCapacity
			this.memoryValues[10] = this.channelValues1[4] == 0 ? 50 : (9/5)*50+32; //chargeOffTemperature
			this.memoryValues[12] = this.memoryValues[12] + 30; //chargeSafetyTimer
			this.memoryValues[9] = this.memoryValues[2] / 10; //trickleCurrent
			this.memoryValues[7] = 5; //deltaPeak mV
			this.memoryValues[8] = 3; //preDeltaPeakDelay min
			this.memoryValues[13] = 1; //rePeakCycle
			this.memoryValues[15] = 3; //rePaekDelay min
			this.memoryValues[7] = 1; //flatLimitCheck
			this.memoryValues[19] = this.channelValues1[4] == 0 ? 45 : (9/5)*45+32; //dischargeOffTemperature
			this.memoryValues[21] = 1250; //NiMhMatchVoltage
			break;
		case 2: //LiIo
			this.memoryValues[11] = 105; //chargeMaxCapacity
			this.memoryValues[10] = this.channelValues1[4] == 0 ? 45 : (9/5)*45+32; //chargeOffTemperature
			this.memoryValues[12] = this.memoryValues[12] + 60; //chargeSafetyTimer
			this.memoryValues[9] = 0; //trickleCurrent
			this.memoryValues[14] = 4100; //chargeVoltage/cell
			this.memoryValues[26] = 3900; //storeVoltage/cell
			this.memoryValues[18] = 3000; //dischargeVoltage/cell
			this.memoryValues[19] = this.channelValues1[4] == 0 ? 40 : (9/5)*40+32; //dischargeOffTemperature
			break;
		case 3: //LiPo
			this.memoryValues[11] = 105; //chargeMaxCapacity
			this.memoryValues[10] = this.channelValues1[4] == 0 ? 45 : (9/5)*45+32; //chargeOffTemperature
			this.memoryValues[12] = this.memoryValues[12] + 60; //chargeSafetyTimer
			this.memoryValues[9] = 0; //trickleCurrent
			this.memoryValues[14] = 4200; //chargeVoltage/cell
			this.memoryValues[26] = 4000; //storeVoltage/cell
			this.memoryValues[18] = 3000; //dischargeVoltage/cell
			this.memoryValues[19] = this.channelValues1[4] == 0 ? 40 : (9/5)*40+32; //dischargeOffTemperature
			break;
		case 4: //LiFe
			this.memoryValues[11] = 125; //chargeMaxCapacity
			this.memoryValues[10] = this.channelValues1[4] == 0 ? 45 : (9/5)*45+32; //chargeOffTemperature
			this.memoryValues[12] = this.memoryValues[12] + 60; //chargeSafetyTimer
			this.memoryValues[9] = 0; //trickleCurrent
			this.memoryValues[14] = 3600; //chargeVoltage/cell
			this.memoryValues[26] = 3500; //storeVoltage/cell
			this.memoryValues[18] = 2500; //dischargeVoltage/cell
			this.memoryValues[19] = this.channelValues1[4] == 0 ? 40 : (9/5)*40+32; //dischargeOffTemperature
			break;
		case 5: //PB
			this.memoryValues[6] = this.memoryValues[2] / 10; //charge current 0.1 C
			this.memoryValues[11] = 155; //chargeMaxCapacity
			this.memoryValues[10] = this.channelValues1[4] == 0 ? 45 : (9/5)*45+32; //chargeOffTemperature
			this.memoryValues[12] = 905; //chargeSafetyTimer
			this.memoryValues[9] = this.memoryValues[2] / 10; //trickleCurrent
			this.memoryValues[14] = 2300; //chargeVoltage/cell
			this.memoryValues[18] = 1800; //dischargeVoltage/cell
			this.memoryValues[19] = this.channelValues1[4] == 0 ? 50 : (9/5)*50+32; //dischargeOffTemperature
			break;
		}
		this.memoryValues[12] = this.memoryValues[12] > 30 ? 30 : this.memoryValues[12];
		//update parameter controls
		for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
			if (this.memoryParameters[i] != null) {
				this.memoryParameters[i].setSliderSelection(this.memoryValues[i]);
			}
		}
		if (log.isLoggable(Level.FINER)) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < UltramatSerialPort.SIZE_MEMORY_SETUP; i++) {
				sb.append(String.format("%04d", this.memoryValues[i])).append(GDE.STRING_LEFT_BRACKET).append(i).append(GDE.STRING_RIGHT_BRACKET_COMMA);
			}
			log.log(Level.FINER, sb.toString());
		}
	}

	/**
	 * save ultra duo plus configuration data at given full qualified file path
	 * @param fullQualifiedFilePath
	 */
	private void saveConfigUDP(String fullQualifiedFilePath) {
		try {
			long startTime = new Date().getTime();
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
				log.log(Level.TIME, "write memory setup XML time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime))); //$NON-NLS-1$ //$NON-NLS-2$
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
