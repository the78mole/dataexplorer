/**************************************************************************************
  	This file is part of DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DataExplorer.  If not, see <http://www.gnu.org/licenses/>.

    Copyright (c) 2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
****************************************************************************************/
package gde.device.skyrc;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.DeviceDialog;
import gde.device.skyrc.MC3000.SlotSettings;
import gde.device.skyrc.MC3000.SystemSettings;
import gde.device.skyrc.ProgramType.SetupData;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.usb.UsbException;
import javax.usb.UsbInterface;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog class to enable visualization control
 * @author Winfried Brügmann
 */
public class MC3000Dialog extends DeviceDialog {

	final static Logger					log											= Logger.getLogger(MC3000Dialog.class.getName());
	static final int						NUMBER_PROGRAM_ENTRIES	= 30;
	static final int						SIZE_PROGRAM_NAME				= 20;
	static final String					NEW_PROG_NAME						= "NEW-PROG-NAME";																	//$NON-NLS-1$
	static final String					STRING_FORMAT_02d_s			= "%02d - %s";																			//$NON-NLS-1$
	static final String					STRING_FORMAT_02d				= "%02d - ";																				//$NON-NLS-1$
	static final String					STRING_20_BLANK					= "                    ";													//$NON-NLS-1$

	Composite										tabFolder;
	final List<CTabItem>				configurations					= new ArrayList<CTabItem>();
	CTabItem										chargeTabItem;
	ScrolledComposite						scrolledchargeComposite;
	Group												slotsViewGroup;

	//final Listener						memoryParameterChangeListener;

	//ParameterConfigControl[]	memoryParameters					= new ParameterConfigControl[31];
	//int[]											memoryValues							= new int[30];
	int													parameterSelectHeight		= 25;
	int													chargeSelectHeight			= 17 * this.parameterSelectHeight;

	//  static String[]						cellTypeNames							= {"LiIo","LiFe","LiHV","NiMH","NiCd","NiZn","Eneloop","RAM"};
	//	final static String[]			cycleModeNames						= {"D>C","D>C>D","C>D","C>D>C"};
	//	final static String[]			operationModeLi						= {"CHARGE","REFRESH","STORAGE","DISCHARGE","CYCLE"};
	//	final static String[]			operationModeOther				= {"CHARGE","REFRESH","BREAKIN","DISCHARGE","CYCLE"};
	//	final static String[]			restingTimeValues					= {"OFF", "END", "REST"};
	//	final static String[]			cellModelNames						= {"Std AA","Std AAA","Pro/XX AA","Pro/XX AAA","Plus AA","Lite AA","Lite AAA","Std C","Std D"};
	//	final static int[]			  cellModelCapacity					= {2000, 960, 3000, 1500, 2200, 1000, 800, 1200, 1800};
	String[]										programmNames						= new String[MC3000Dialog.NUMBER_PROGRAM_ENTRIES];
	int													lastSelectionIndex[]		= { 0, 0, 0, 0 };
	CCombo[]										programmNameCombos			= new CCombo[4];

	Schema											schema;
	JAXBContext									jc;
	MC3000Type									mc3000Setup;
	static final String					MC3000_XSD							= "MC3000_V01.xsd";																//$NON-NLS-1$

	Button											saveButton, closeButton;
	GathererThread							dataGatherThread;

	final MC3000								device;																																		// get device specific things, get serial port, ...
	final MC3000UsbPort					usbPort;																																		// open/close port execute getData()....
	UsbInterface								usbInterface						= null;
	final Settings							settings;																																	// application configuration settings
	SystemSettings							systemSettings;
	SlotSettings								slotSettings_0;
	SlotSettings								slotSettings_1;
	SlotSettings								slotSettings_2;
	SlotSettings								slotSettings_3;
	int													measurementsCount				= 0;
	int													tabFolderSelectionIndex	= 0;
	protected static final int	USB_QUERY_DELAY					= GDE.IS_WINDOWS ? 70 : 160;
	boolean											isConnectedByDialog			= false;
	boolean 										isConnectedByButton 		= false;


	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public MC3000Dialog(Shell parent, MC3000 useDevice) {
		super(parent);
		this.device = useDevice;
		this.usbPort = useDevice.getCommunicationPort();
		this.settings = Settings.getInstance();
		for (int i = 0; i < MC3000Dialog.NUMBER_PROGRAM_ENTRIES; i++) {
			this.programmNames[i] = String.format("%02d - %s", i + 1, MC3000Dialog.NEW_PROG_NAME); //$NON-NLS-1$
		}
		//disable listener and battery dependent handling of parameters due to inconsistent setting of device program parameters 
		//		this.memoryParameterChangeListener = new Listener() {
		//			@Override
		//			public void handleEvent(Event evt) {
		//				int selectionIndex = 0;
		//				if (memoryParameters[evt.index] != null) 
		//					selectionIndex = memoryParameters[evt.index].getSlider().getSelection();
		//				log.log(java.util.logging.Level.OFF,	"memoryParameterChangeListener (" + " memoryValues[" + evt.index + "] changed -> " + selectionIndex); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
		//				switch (evt.index) {
		//				case 5://Battery Type LiIo NiMH ...
		//					updateBatteryTypeParameter(selectionIndex);					
		//					break;
		//				case 6://Battery Model AAA AA ...
		//					updateBatteryModelParameter(selectionIndex);					
		//					break;
		//				case 7://Operation Mode Charge Discharge ...
		//					updateOperationModeParameter(selectionIndex);					
		//					break;
		//
		//				default:
		//					break;
		//				}
		//			}
		//		};
	}

	//	/**
	//	 * update the memory setup parameter list to cell type dependent
	//	 */
	//	private void updateBatteryTypeParameter(int selectionIndex) {
	//		log.log(java.util.logging.Level.FINE,	"selectionIndex = " + selectionIndex + " - " + cellTypeNames[selectionIndex]); 
	//		//4=slotNumber 5=batteryType 6=capacity 7=operationMode 8,9=chargeCurrent 10,11=dischargeCurrent 12,13=dischargeEndVoltage 14,15=chargeEndVoltage 
	//		//16,17=chargeEndCurrent 18,19=dischargeEndCurrent 20=cycleCount 21=cycleRestTime 22=cycleMode 23=niVoltageDelta 24=niTrickleCurrent 25=cutTemperature
	//		//26=cutTime 27=restartVoltage
	//		switch (selectionIndex) {
	//		case 0: //"LiIo"
	//			this.memoryParameters[6].setEnabled(false);
	//			this.memoryParameters[7].updateTextFieldValues(operationModeLi);
	//			this.memoryParameters[12].updateValueRange("[V] (2.50 - 3.65 V)", 0, 365, -250);
	//			this.memoryParameters[14].updateValueRange("[V] (4.00 - 4,25 V)", 0, 425, -400); //$NON-NLS-1$
	//			this.memoryParameters[23].setEnabled(false);
	//			this.memoryParameters[24].setEnabled(false);
	//			this.memoryParameters[26].setEnabled(false);
	//			this.memoryParameters[27].setEnabled(false);
	//			break;
	//		case 1: //"LiFe"
	//			this.memoryParameters[1].setEnabled(true);
	//			this.memoryParameters[6].setEnabled(false);
	//			this.memoryParameters[7].updateTextFieldValues(operationModeLi);
	//			this.memoryParameters[12].updateValueRange("[V] (2.50 - 3.65 V)", 0, 365, -250);
	//			this.memoryParameters[14].updateValueRange("[V] (4.00 - 4,25 V)", 0, 425, -400); //$NON-NLS-1$
	//			this.memoryParameters[23].setEnabled(false);
	//			this.memoryParameters[24].setEnabled(false);
	//			this.memoryParameters[26].setEnabled(false);
	//			this.memoryParameters[27].setEnabled(false);
	//			break;
	//		case 2: //"LiHV"
	//			this.memoryParameters[1].setEnabled(true);
	//			this.memoryParameters[6].setEnabled(false);
	//			this.memoryParameters[7].updateTextFieldValues(operationModeLi);
	//			this.memoryParameters[12].updateValueRange("[V] (2.50 - 3.65 V)", 0, 365, -250);
	//			this.memoryParameters[14].updateValueRange("[V] (4.10 - 4,40 V)", 0, 440, -410); //$NON-NLS-1$
	//			this.memoryParameters[23].setEnabled(false);
	//			this.memoryParameters[24].setEnabled(false);
	//			this.memoryParameters[26].setEnabled(false);
	//			this.memoryParameters[27].setEnabled(false); 
	//			break;
	//		case 3: //"NiMH"
	//		case 4: //"NiCd"
	//		case 6: //"Eneloop"
	//			this.memoryParameters[1].setEnabled(false);
	//			this.memoryParameters[6].setEnabled(true);
	//			this.memoryParameters[7].updateTextFieldValues(operationModeOther);
	//			this.memoryParameters[12].updateValueRange("[V] (0.50 - 1 V)", 0, 100, -50);
	//			this.memoryParameters[14].updateValueRange("[V] (1.47 - 1.8 V)", 0, 180, -147); //$NON-NLS-1$
	//			this.memoryParameters[23].setEnabled(true);
	//			this.memoryParameters[24].setEnabled(true);
	//			this.memoryParameters[26].setEnabled(true);
	//			this.memoryParameters[27].setEnabled(true);
	//			break;
	//		case 5: //"NiZn"
	//			
	//			break;
	//		case 7: //"RAM"
	//			
	//			break;
	//
	//		default:
	//			break;
	//		}
	//		this.chargeGroup.setSize(this.scrolledchargeComposite.getClientArea().width, this.chargeSelectHeight);
	//		this.chargeGroup.layout(true);
	//		this.scrolledchargeComposite.layout(true);
	//	}
	//
	/**
	 * update the memory parameter according to system properties
	 */
	private void updateSystemParameter(final SystemSettings sysSettings) {
		if (sysSettings != null) {
			if (sysSettings.getFirmwareVersionAsInt() <= 111) {
				this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGT3650, new String[] { sysSettings.getFirmwareVersion() }));
			}
		}
	}

	//	/**
	//	 * update the memory setup parameter list to cell type dependent
	//	 */
	//	private void updateBatteryModelParameter(int selectionIndex) {
	//		log.log(java.util.logging.Level.FINE,	"selectionIndex = " + selectionIndex + " - " + cellTypeNames[selectionIndex]); 
	//		switch (selectionIndex) {
	//		case 0: //"LiIo"
	//			
	//			break;
	//		case 1: //"LiFe"
	//			
	//			break;
	//		case 2: //"LiHV"
	//			
	//			break;
	//		case 3: //"NiMH"
	//			
	//			break;
	//		case 4: //"NiCd"
	//			
	//			break;
	//		case 5: //"NiZn"
	//			
	//			break;
	//		case 6: //"Eneloop"
	//			
	//			break;
	//		case 7: //"RAM"
	//			
	//			break;
	//
	//		default:
	//			break;
	//		}
	//	}
	//
	//	/**
	//	 * update the memory setup parameter list to cell type dependent
	//	 */
	//	private void updateOperationModeParameter(int selectionIndex) {
	//		log.log(java.util.logging.Level.FINE,	"selectionIndex = " + selectionIndex + " - " + cellTypeNames[selectionIndex]); 
	//		switch (selectionIndex) {
	//		case 0: //"LiIo"
	//			
	//			break;
	//		case 1: //"LiFe"
	//			
	//			break;
	//		case 2: //"LiHV"
	//			
	//			break;
	//		case 3: //"NiMH"
	//			
	//			break;
	//		case 4: //"NiCd"
	//			
	//			break;
	//		case 5: //"NiZn"
	//			
	//			break;
	//		case 6: //"Eneloop"
	//			
	//			break;
	//		case 7: //"RAM"
	//			
	//			break;
	//
	//		default:
	//			break;
	//		}
	//	}
	//
	//	/**
	//	 * update the memory setup parameter list to cell type dependent
	//	 */
	//	private void updateBatteryTypeParameter(int selectionIndex) {
	//		log.log(java.util.logging.Level.FINE,	"selectionIndex = " + selectionIndex + " - " + cellTypeNames[selectionIndex]); 
	//		switch (selectionIndex) {
	//		case 0: //"LiIo"
	//			
	//			break;
	//		case 1: //"LiFe"
	//			
	//			break;
	//		case 2: //"LiHV"
	//			
	//			break;
	//		case 3: //"NiMH"
	//			
	//			break;
	//		case 4: //"NiCd"
	//			
	//			break;
	//		case 5: //"NiZn"
	//			
	//			break;
	//		case 6: //"Eneloop"
	//			
	//			break;
	//		case 7: //"RAM"
	//			
	//			break;
	//
	//		default:
	//			break;
	//		}
	//	}

	@Override
	public void open() {
		if (!this.usbPort.isConnected()) {
			try {

				this.jc = JAXBContext.newInstance("gde.device.skyrc"); //$NON-NLS-1$
				this.schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
						new StreamSource(MC3000Dialog.class.getClassLoader().getResourceAsStream("resource/" + MC3000Dialog.MC3000_XSD))); //$NON-NLS-1$

				try {
					Unmarshaller unmarshaller = this.jc.createUnmarshaller();
					unmarshaller.setSchema(this.schema);
					this.mc3000Setup = (MC3000Type) unmarshaller.unmarshal(new File(this.settings.getApplHomePath() + "/MC3000_Slot_Programs" + GDE.FILE_ENDING_DOT_XML)); //$NON-NLS-1$
					int index = 0;
					for (ProgramType prog : this.mc3000Setup.program) {
						if (!prog.getName().contains(MC3000Dialog.NEW_PROG_NAME)) this.programmNames[index++] = String.format("%02d - %s", index, prog.getName()); //$NON-NLS-1$
					}
				}
				catch (UnmarshalException e) {
					MC3000Dialog.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					createMC3000Setup();
				}
				catch (Exception e) {
					if (e.getCause() instanceof FileNotFoundException) {
						createMC3000Setup();
					}
					else
						throw e;
				}

				if (!this.usbPort.isConnected()) {
					this.usbInterface = this.usbPort.openUsbPort(this.device);				
					this.isConnectedByDialog = true;
				}

				if (this.usbPort.isConnected()) this.slotSettings_0 = this.device.new SlotSettings(this.usbPort.getSlotData(this.usbInterface, MC3000UsbPort.QuerySlotData.SLOT_0.value()));
				WaitTimer.delay(MC3000Dialog.USB_QUERY_DELAY);
				if (this.usbPort.isConnected()) this.slotSettings_1 = this.device.new SlotSettings(this.usbPort.getSlotData(this.usbInterface, MC3000UsbPort.QuerySlotData.SLOT_1.value()));
				WaitTimer.delay(MC3000Dialog.USB_QUERY_DELAY);
				if (this.usbPort.isConnected()) this.slotSettings_2 = this.device.new SlotSettings(this.usbPort.getSlotData(this.usbInterface, MC3000UsbPort.QuerySlotData.SLOT_2.value()));
				WaitTimer.delay(MC3000Dialog.USB_QUERY_DELAY);
				if (this.usbPort.isConnected()) this.slotSettings_3 = this.device.new SlotSettings(this.usbPort.getSlotData(this.usbInterface, MC3000UsbPort.QuerySlotData.SLOT_3.value()));
				WaitTimer.delay(MC3000Dialog.USB_QUERY_DELAY);
				if (this.usbPort.isConnected()) this.systemSettings = this.device.new SystemSettings(this.usbPort.getSystemSettings(this.usbInterface));
			}
			catch (Exception e) {
				MC3000Dialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
				this.application.openMessageDialogAsync(e.getMessage());
			}
			finally {
				if (isConnectedByDialog && this.usbPort != null && this.usbPort.isConnected()) {
					try {
						this.usbPort.closeUsbPort(this.usbInterface != null ? this.usbInterface : null);
					}
					catch (UsbException e) {
						MC3000Dialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
					}
				}
			}
		}
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue();
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			MC3000Dialog.log.log(java.util.logging.Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.dialogShell == null || this.dialogShell.isDisposed()) {
				if (this.settings.isDeviceDialogsModal())
					this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
				else if (this.settings.isDeviceDialogsOnTop())
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
				else
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

				SWTResourceManager.registerResourceUser(this.dialogShell);
				if (this.isAlphaEnabled) this.dialogShell.setAlpha(254);

				FormLayout dialogShellLayout = new FormLayout();
				this.dialogShell.setLayout(dialogShellLayout);
				this.dialogShell.layout();
				this.dialogShell.pack();
				this.dialogShell.setSize(355, 75 + this.chargeSelectHeight);
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.addListener(SWT.Traverse, new Listener() {
					@Override
					public void handleEvent(Event event) {
						switch (event.detail) {
						case SWT.TRAVERSE_ESCAPE:
							MC3000Dialog.this.dialogShell.close();
							event.detail = SWT.TRAVERSE_NONE;
							event.doit = false;
							break;
						}
					}
				});
				this.dialogShell.addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent evt) {
						MC3000Dialog.log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (MC3000Dialog.this.device.isChangePropery()) {
							String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { MC3000Dialog.this.device.getPropertiesFileName() });
							if (MC3000Dialog.this.application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								MC3000Dialog.log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
								MC3000Dialog.this.device.storeDeviceProperties();
								setClosePossible(true);
							}
						}
						MC3000Dialog.this.dispose();
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					@Override
					public void helpRequested(HelpEvent evt) {
						MC3000Dialog.log.log(java.util.logging.Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						MC3000Dialog.this.application.openHelpDialog("SkyRC", "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				{
					this.tabFolder = new Composite(this.dialogShell, SWT.NONE);
					this.tabFolder.setLayout(new FillLayout(SWT.VERTICAL));
					this.tabFolder.setSize(this.dialogShell.getClientArea().width, this.chargeSelectHeight);
					this.tabFolder.layout();
					//					this.tabFolder = new CTabFolder(this.dialogShell, SWT.NONE);
					{
						//						this.chargeTabItem = new CTabItem(this.tabFolder, SWT.NONE);
						//						this.chargeTabItem.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						//						this.chargeTabItem.setText("Slot 1");
						{
							this.scrolledchargeComposite = new ScrolledComposite(this.tabFolder, SWT.BORDER | SWT.V_SCROLL);
							FillLayout scrolledMemoryCompositeLayout = new FillLayout();
							this.scrolledchargeComposite.setLayout(scrolledMemoryCompositeLayout);
							//							this.chargeTabItem.setControl(this.scrolledchargeComposite);
							{
								this.slotsViewGroup = new Group(this.scrolledchargeComposite, SWT.NONE);
								this.slotsViewGroup.setLayout(new FillLayout(SWT.VERTICAL));
								for (int i = 0; i < 4; i++) {
									createSlotGroup(i);
								}
								//								Group slot1 = new Group(this.slotsViewGroup, SWT.NONE);
								//								slot1.setLayout(new RowLayout(SWT.HORIZONTAL));
								//								slot1.setText("Slot 1");
								//								CLabel label1 = new CLabel(slot1, SWT.None);
								//								label1.setText(this.slotSettings_1 != null ? this.slotSettings_1.toString4View() : "no connetion to device");
								//								Group slot2 = new Group(this.slotsViewGroup, SWT.NONE);
								//								slot2.setLayout(new RowLayout(SWT.HORIZONTAL));
								//								slot2.setText("Slot 2");
								//								CLabel label2 = new CLabel(slot2, SWT.None);
								//								label2.setText(this.slotSettings_2 != null ? this.slotSettings_2.toString4View() : "no connetion to device");
								//								Group slot3 = new Group(this.slotsViewGroup, SWT.NONE);
								//								slot3.setLayout(new RowLayout(SWT.HORIZONTAL));
								//								slot3.setText("Slot 3");
								//								CLabel label3 = new CLabel(slot3, SWT.None);
								//								label3.setText(this.slotSettings_3 != null ? this.slotSettings_3.toString4View() : "no connetion to device");

								//								this.chargeGroup.addListener(SWT.Selection, memoryParameterChangeListener);
								//								//charge parameter
								//								//4=slotNumber 5=batteryType 6=capacity 7=operationMode 8,9=chargeCurrent 10,11=dischargeCurrent 12,13=dischargeEndVoltage 14,15=chargeEndVoltage 
								//								//16,17=chargeEndCurrent 18,19=dischargeEndCurrent 20=cycleCount 21=cycleRestTime 22=cycleMode 23=niVoltageDelta 24=niTrickleCurrent 25=cutTemperature
								//								//26,27=cutTime 28,29=restartVoltage
								//								this.memoryParameters[5] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 5, "Battery Type", 175,	GDE.STRING_EMPTY, 190, cellTypeNames, 80, 150);
								//								this.memoryParameters[6] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 6, "Battery Model", 175,	GDE.STRING_EMPTY, 190, cellModelNames, 80, 150);
								//								this.memoryParameters[7] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 7, "Operation Mode", 175,	GDE.STRING_EMPTY, 190, operationModeLi, 80, 150);
								//								this.memoryParameters[1] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 6, GDE.STRING_EMPTY, "Capacity", 175, "[mAh] (0 = OFF)", 190, true, 80, 150, 0, 5000, 0, false); //$NON-NLS-1$ 
								//								this.memoryParameters[8] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 8, "%3.2f", "Charge Current", 175,	"[A] (0.05 - 3 A)", 190, true, 80, 150, 5, 300, 0, false); //$NON-NLS-1$ 
								//								this.memoryParameters[10] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 10, "%3.2f", "Discharge Current", 175, "[A] (0.05 - 2 A)", 190, true, 80, 150, 5, 200, 0, false); //$NON-NLS-1$ 
								//								this.memoryParameters[14] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 11, "%3.2f", "Charge Target Voltage", 175, "[V] (1.47 - 1.8 V)", 190, true, 80, 150, 0, 180, -147, false); //$NON-NLS-1$ 
								//								this.memoryParameters[16] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 8, "%3.2f", "Charge Termination Current", 175, "[A] (0 - 0.49 A, 0.5 = OFF)", 190, true, 80, 150, 0, 50, 0, false); //$NON-NLS-1$ 
								//								this.memoryParameters[18] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 10, "%3.2f", "Discharge End Current", 175,	"[A] (0.01 - 0.99 A 1.00=OFF)", 190, true, 80, 150, 1, 100, 0, false); //$NON-NLS-1$ 
								//								this.memoryParameters[20] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 10, GDE.STRING_EMPTY, "Cycle Count", 175,	"1 - 99", 190, true, 80, 150, 1, 99); //$NON-NLS-1$ 
								//								this.memoryParameters[21] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 12, GDE.STRING_EMPTY, "Cycle Rest Time", 175,	"[min] (0 - 240 min)", 190, true, 80, 150, 0, 240);
								//								this.memoryParameters[22] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 14, "Cycle Mode", 175, GDE.STRING_EMPTY, 190, cycleModeNames, 80, 150);
								//								this.memoryParameters[23] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 9, GDE.STRING_EMPTY, "NiMH/NiCd Delta Peak", 175, "[mV] (0 - 15 mV)", 190, true, 80, 150, 0, 15, 0, false); //$NON-NLS-1$ 
								//								this.memoryParameters[24] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 26, GDE.STRING_EMPTY, "NiMH/NiCd Trickle Current", 175,	"[mA] (OFF = 9) 10 - 200 mA", 190, true, 80, 150, 9, 200, 0, false); //$NON-NLS-1$ 
								//								this.memoryParameters[25] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 7, GDE.STRING_EMPTY, "Cut temperature", 175,"[°C] (20 - 75 °C)", 190, true, 80, 150, 20, 75); //$NON-NLS-1$ 
								//								this.memoryParameters[26] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 26, "NiMH/NiCd Trickle Time", 175,	"OFF - End - Resting", 190, restingTimeValues, 80, 150); //$NON-NLS-1$ 								
								//								this.memoryParameters[12] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 11, "%3.2f", "Discharge Cut Voltage", 175, "[V] (0.50 - 1 V)", 190, true, 80, 150, 0, 100, -50, false); //$NON-NLS-1$
								//								this.memoryParameters[27] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 8, GDE.STRING_EMPTY, "Cut time", 175, "[min] (OFF=0, 1 - 1440 min)", 190, true, 80, 150, 0, 1440); //$NON-NLS-1$ 
								//								this.memoryParameters[28] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 15, "%3.2f", "Restart Voltage", 175,	"[V] (OFF=1.29 1.3 - 1.45 V)", 190, true, 80, 150, 129, 145, -129, false); //$NON-NLS-1$ 
								//								this.memoryParameters[29] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 15, GDE.STRING_EMPTY, "Charge Rest Time", 175,	"[min] (0 - 240 min)", 190, true, 80, 150, 0, 240, 0, false); //$NON-NLS-1$ 
								//								this.memoryParameters[30] = new ParameterConfigControl(this.chargeGroup, this.memoryValues, 15, GDE.STRING_EMPTY, "Discharge Rest Time", 175,	"[min] (0 - 240 min)", 190, true, 80, 150, 0, 240, 0, false); //$NON-NLS-1$ 
							}
							this.scrolledchargeComposite.setContent(this.slotsViewGroup);
							this.slotsViewGroup.setSize(620, this.chargeSelectHeight);
							this.scrolledchargeComposite.addControlListener(new ControlListener() {
								@Override
								public void controlResized(ControlEvent evt) {
									MC3000Dialog.log.log(java.util.logging.Level.FINEST, "scrolledMemoryComposite.controlResized, event=" + evt); //$NON-NLS-1$
									MC3000Dialog.this.slotsViewGroup.setSize(MC3000Dialog.this.scrolledchargeComposite.getClientArea().width, MC3000Dialog.this.chargeSelectHeight);
								}

								@Override
								public void controlMoved(ControlEvent evt) {
									MC3000Dialog.log.log(java.util.logging.Level.FINEST, "scrolledMemoryComposite.controlMoved, event=" + evt); //$NON-NLS-1$
									MC3000Dialog.this.slotsViewGroup.setSize(MC3000Dialog.this.scrolledchargeComposite.getClientArea().width, MC3000Dialog.this.chargeSelectHeight);
								}
							});
							this.slotsViewGroup.setSize(this.tabFolder.getClientArea().width, this.chargeSelectHeight);
							this.slotsViewGroup.layout(true);
							this.scrolledchargeComposite.layout(true);
						}
					}
					//					{
					//						for (int i = 0; i < this.device.getChannelCount(); i++) {
					//							this.configurations.add(new MC3000TabItem(this.tabFolder, this, (i + 1), this.device));
					//						}
					//					}
					{
						this.saveButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
						FormData saveButtonLData = new FormData();
						saveButtonLData.width = 120;
						saveButtonLData.height = 30;
						saveButtonLData.bottom = new FormAttachment(1000, 1000, -10);
						saveButtonLData.left = new FormAttachment(0, 1000, 55);
						this.saveButton.setLayoutData(saveButtonLData);
						this.saveButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.saveButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0486));
						this.saveButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3655));
						this.saveButton.setEnabled(false);
						this.saveButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								MC3000Dialog.log.log(java.util.logging.Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								saveMc3000SetupData();
								//								MC3000Dialog.this.device.storeDeviceProperties();
								//								MC3000Dialog.this.saveButton.setEnabled(false);
							}
						});
					}
					//					{
					//						this.startConfiguration = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
					//						FormData startConfigurationLData = new FormData();
					//						startConfigurationLData.height = 30;
					//						startConfigurationLData.left = new FormAttachment(0, 1000, 200);
					//						startConfigurationLData.right = new FormAttachment(1000, 1000, -200);
					//						startConfigurationLData.bottom = new FormAttachment(1000, 1000, -10);
					//						this.startConfiguration.setLayoutData(startConfigurationLData);
					//						this.startConfiguration.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					//						this.startConfiguration.setText(this.device.usbPort.isConnected() ? "stop configurartion" : "start configurartion");//QcCopterDialog.java
					//						this.startConfiguration.addSelectionListener(new SelectionAdapter() {
					//							@Override
					//							public void widgetSelected(SelectionEvent evt) {
					//								MC3000Dialog.log.log(java.util.logging.Level.FINEST, "startConfiguration.widgetSelected, event=" + evt); //$NON-NLS-1$
					//								MC3000Dialog.this.device.open_closeCommPort();
					//								checkPortStatus();
					//							}
					//						});
					//					}
					{
						this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
						FormData closeButtonLData = new FormData();
						closeButtonLData.width = 120;
						closeButtonLData.height = 30;
						closeButtonLData.right = new FormAttachment(1000, 1000, -55);
						closeButtonLData.bottom = new FormAttachment(1000, 1000, -10);
						this.closeButton.setLayoutData(closeButtonLData);
						this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.closeButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0485));
						this.closeButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								MC3000Dialog.log.log(java.util.logging.Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								MC3000Dialog.this.dialogShell.dispose();
							}
						});
					}
					FormData tabFolderLData = new FormData();
					tabFolderLData.top = new FormAttachment(0, 1000, 0);
					tabFolderLData.left = new FormAttachment(0, 1000, 0);
					tabFolderLData.right = new FormAttachment(1000, 1000, 0);
					tabFolderLData.bottom = new FormAttachment(1000, 1000, -50);
					this.tabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tabFolder.setLayoutData(tabFolderLData);
				}

				Channel activChannel = Channels.getInstance().getActiveChannel();
				if (activChannel != null) {
					RecordSet activeRecordSet = activChannel.getActiveRecordSet();
					if (activeRecordSet != null) {
						//						this.tabFolder.setSelection(activeRecordSet.getChannelConfigNumber());
						//						tabFolderSelectionIndex = tabFolder.getSelectionIndex();
					}
					else {
						if (this.device.usbPort.isMatchAvailablePorts(this.device.getPort()) && !this.device.usbPort.isConnected()) this.device.open_closeCommPort();
					}
				}

				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 175, 100));
				this.dialogShell.open();
				//this.checkPortStatus();
			}
			else {
				this.dialogShell.setVisible(true);
				this.dialogShell.setActive();
			}
			//			updateBatteryTypeParameter(0); //TODO
			updateSystemParameter(this.systemSettings);

			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			MC3000Dialog.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
	}

	private void createSlotGroup(final int index) {
		Group slot = new Group(this.slotsViewGroup, SWT.NONE);
		slot.setLayout(new RowLayout(SWT.HORIZONTAL));
		slot.setText(Messages.getString(MessageIds.GDE_MSGT3652, new Object[] { index }));
		CLabel nameLabel = new CLabel(slot, SWT.None);
		nameLabel.setLayoutData(new RowData(120, 20));
		nameLabel.setText(Messages.getString(MessageIds.GDE_MSGT3652));
		final CCombo programmNameCombo = new CCombo(slot, SWT.BORDER);
		this.programmNameCombos[index] = programmNameCombo;
		final CLabel label0 = new CLabel(slot, SWT.None);
		programmNameCombo.setItems(this.programmNames);
		programmNameCombo.setVisibleItemCount(20);
		programmNameCombo.setTextLimit(5 + MC3000Dialog.SIZE_PROGRAM_NAME);
		programmNameCombo.setLayoutData(new RowData(187, GDE.IS_WINDOWS ? 16 : 18));
		programmNameCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3653));
		programmNameCombo.select(0);
		programmNameCombo.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		programmNameCombo.setEditable(true);
		programmNameCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				MC3000Dialog.log.log(java.util.logging.Level.FINEST, "programmNameCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
				MC3000Dialog.this.lastSelectionIndex[index] = programmNameCombo.getSelectionIndex();
				//load saved values to slot if selected program name does not contain "NEW-PROG-NAME"
				if (MC3000Dialog.this.mc3000Setup == null) createMC3000Setup();
				List<ProgramType> devicePrograms = MC3000Dialog.this.mc3000Setup.getProgram();
				if (!devicePrograms.get(programmNameCombo.getSelectionIndex()).getName().contains(MC3000Dialog.NEW_PROG_NAME)) {
					switch (index) {
					case 0:
						MC3000Dialog.this.slotSettings_0 = MC3000Dialog.this.device.new SlotSettings(devicePrograms.get(programmNameCombo.getSelectionIndex()).getSetupData().getValue());
						MC3000Dialog.this.slotSettings_0.setSlotNumber((byte) index);
						label0.setText(MC3000Dialog.this.slotSettings_0.toString4View());
						break;
					case 1:
						MC3000Dialog.this.slotSettings_1 = MC3000Dialog.this.device.new SlotSettings(devicePrograms.get(programmNameCombo.getSelectionIndex()).getSetupData().getValue());
						MC3000Dialog.this.slotSettings_1.setSlotNumber((byte) index);
						label0.setText(MC3000Dialog.this.slotSettings_1.toString4View());
						break;
					case 2:
						MC3000Dialog.this.slotSettings_2 = MC3000Dialog.this.device.new SlotSettings(devicePrograms.get(programmNameCombo.getSelectionIndex()).getSetupData().getValue());
						MC3000Dialog.this.slotSettings_2.setSlotNumber((byte) index);
						label0.setText(MC3000Dialog.this.slotSettings_2.toString4View());
						break;
					case 3:
						MC3000Dialog.this.slotSettings_3 = MC3000Dialog.this.device.new SlotSettings(devicePrograms.get(programmNameCombo.getSelectionIndex()).getSetupData().getValue());
						MC3000Dialog.this.slotSettings_3.setSlotNumber((byte) index);
						label0.setText(MC3000Dialog.this.slotSettings_3.toString4View());
						break;

					default:
						break;
					}
				}
			}
		});
		programmNameCombo.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent evt) {
				MC3000Dialog.log.log(java.util.logging.Level.FINEST, "memoryCombo.keyReleased, event=" + evt); //$NON-NLS-1$
			}

			@Override
			public void keyPressed(KeyEvent evt) {
				MC3000Dialog.log.log(java.util.logging.Level.FINEST, "memoryCombo.keyPressed, event=" + evt); //$NON-NLS-1$
				if (evt.character == SWT.CR) {
					try {
						if (MC3000Dialog.this.mc3000Setup == null) createMC3000Setup();
						MC3000Dialog.log.log(java.util.logging.Level.OFF, String.format("slot # %d selection index = %d", index, MC3000Dialog.this.lastSelectionIndex[index])); //$NON-NLS-1$
						String nameLeader = String.format(MC3000Dialog.STRING_FORMAT_02d, MC3000Dialog.this.lastSelectionIndex[index] + 1);
						String tmpName = programmNameCombo.getText() + MC3000Dialog.STRING_20_BLANK;
						tmpName = tmpName.startsWith(nameLeader) ? tmpName : (nameLeader + tmpName);
						String newSlotProgramName = String.format(MC3000Dialog.STRING_FORMAT_02d_s, MC3000Dialog.this.lastSelectionIndex[index] + 1, tmpName.substring(5, MC3000Dialog.SIZE_PROGRAM_NAME + 5)
								.trim());
						MC3000Dialog.this.programmNames[MC3000Dialog.this.lastSelectionIndex[index]] = newSlotProgramName;
						programmNameCombo.setText(newSlotProgramName);
						programmNameCombo.setItem(MC3000Dialog.this.lastSelectionIndex[index], newSlotProgramName);

						MC3000Dialog.this.mc3000Setup.getProgram().get(MC3000Dialog.this.lastSelectionIndex[index])
								.setName(MC3000Dialog.this.programmNames[MC3000Dialog.this.lastSelectionIndex[index]].substring(5).trim());
						switch (index) {
						case 0:
							MC3000Dialog.this.mc3000Setup.getProgram().get(MC3000Dialog.this.lastSelectionIndex[index]).getSetupData().setValue(MC3000Dialog.this.slotSettings_0.getBuffer());
							break;
						case 1:
							MC3000Dialog.this.mc3000Setup.getProgram().get(MC3000Dialog.this.lastSelectionIndex[index]).getSetupData().setValue(MC3000Dialog.this.slotSettings_1.getBuffer());
							break;
						case 2:
							MC3000Dialog.this.mc3000Setup.getProgram().get(MC3000Dialog.this.lastSelectionIndex[index]).getSetupData().setValue(MC3000Dialog.this.slotSettings_2.getBuffer());
							break;
						case 3:
							MC3000Dialog.this.mc3000Setup.getProgram().get(MC3000Dialog.this.lastSelectionIndex[index]).getSetupData().setValue(MC3000Dialog.this.slotSettings_3.getBuffer());
							break;

						default:
							break;
						}
						for (CCombo combo : MC3000Dialog.this.programmNameCombos) {
							combo.setItem(MC3000Dialog.this.lastSelectionIndex[index], newSlotProgramName);
							if (combo.getSelectionIndex() == MC3000Dialog.this.lastSelectionIndex[index]) combo.setText(newSlotProgramName);
						}
						MC3000Dialog.this.saveButton.setEnabled(true);
					}
					catch (Exception e) {
						MC3000Dialog.this.application.openMessageDialog(MC3000Dialog.this.dialogShell, Messages.getString(gde.messages.MessageIds.GDE_MSGE0007, new String[] { e.getMessage() }));
					}
				}
			}
		});
		label0.setLayoutData(new RowData(350, 25));
		switch (index) {
		case 0:
			label0.setText(this.slotSettings_0 != null ? this.slotSettings_0.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
			break;
		case 1:
			label0.setText(this.slotSettings_1 != null ? this.slotSettings_1.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
			break;
		case 2:
			label0.setText(this.slotSettings_2 != null ? this.slotSettings_2.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
			break;
		case 3:
			label0.setText(this.slotSettings_3 != null ? this.slotSettings_3.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
			break;

		default:
			break;
		}
		Button writeButton = new Button(slot, SWT.CENTER);
		writeButton.setLayoutData(new RowData(155, 22));
		writeButton.setText(Messages.getString(MessageIds.GDE_MSGT3656));
		writeButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3657));
		writeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				MC3000Dialog.log.log(java.util.logging.Level.FINEST, "writeButton.widgetSelected, event=" + evt); //$NON-NLS-1$);
				try {
					if (!MC3000Dialog.this.usbPort.isConnected()) {
						MC3000Dialog.this.usbInterface = MC3000Dialog.this.usbPort.openUsbPort(MC3000Dialog.this.device);
						isConnectedByButton = true;
					}
					byte[] newProgramBuffer;
					switch (index) {
					case 0:
						newProgramBuffer = MC3000Dialog.this.slotSettings_0.getBuffer((byte)0, MC3000Dialog.this.systemSettings.getFirmwareVersionAsInt());
						MC3000Dialog.this.usbPort.write(newProgramBuffer);
						break;
					case 1:
						newProgramBuffer = MC3000Dialog.this.slotSettings_1.getBuffer((byte)1, MC3000Dialog.this.systemSettings.getFirmwareVersionAsInt());
						MC3000Dialog.this.usbPort.write(newProgramBuffer);
						break;
					case 2:
						newProgramBuffer = MC3000Dialog.this.slotSettings_2.getBuffer((byte)2, MC3000Dialog.this.systemSettings.getFirmwareVersionAsInt());
						MC3000Dialog.this.usbPort.write(newProgramBuffer);
						break;
					case 3:
						newProgramBuffer = MC3000Dialog.this.slotSettings_3.getBuffer((byte)3, MC3000Dialog.this.systemSettings.getFirmwareVersionAsInt());
						MC3000Dialog.this.usbPort.write(newProgramBuffer);
						break;

					default:
						break;
					}
				}
				catch (Exception e) {
					MC3000Dialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
					MC3000Dialog.this.application.openMessageDialogAsync(e.getMessage());
				}
				finally {
					if (isConnectedByButton && MC3000Dialog.this.usbPort != null && MC3000Dialog.this.usbPort.isConnected()) {
						try {
							MC3000Dialog.this.usbPort.closeUsbPort(MC3000Dialog.this.usbInterface != null ? MC3000Dialog.this.usbInterface : null);
						}
						catch (UsbException e) {
							MC3000Dialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
						}
					}
				}
			}
		});
		Button loadButton = new Button(slot, SWT.CENTER);
		loadButton.setLayoutData(new RowData(155, 22));
		loadButton.setText(Messages.getString(MessageIds.GDE_MSGT3658));
		loadButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT3659));
		loadButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				MC3000Dialog.log.log(java.util.logging.Level.FINEST, "loadButton.widgetSelected, event=" + evt); //$NON-NLS-1$);
				try {
					if (!MC3000Dialog.this.usbPort.isConnected()) {
						MC3000Dialog.this.usbInterface = MC3000Dialog.this.usbPort.openUsbPort(MC3000Dialog.this.device);
						isConnectedByButton = true;
					}

					switch (index) {
					case 0:
						if (MC3000Dialog.this.usbPort.isConnected()) MC3000Dialog.this.slotSettings_0 = MC3000Dialog.this.device.new SlotSettings(MC3000Dialog.this.usbPort.getSlotData(MC3000Dialog.this.usbInterface, MC3000UsbPort.QuerySlotData.SLOT_0.value()));
						label0.setText(MC3000Dialog.this.slotSettings_0 != null ? MC3000Dialog.this.slotSettings_0.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
						break;
					case 1:
						if (MC3000Dialog.this.usbPort.isConnected()) MC3000Dialog.this.slotSettings_1 = MC3000Dialog.this.device.new SlotSettings(MC3000Dialog.this.usbPort.getSlotData(MC3000Dialog.this.usbInterface, MC3000UsbPort.QuerySlotData.SLOT_1.value()));
						label0.setText(MC3000Dialog.this.slotSettings_1 != null ? MC3000Dialog.this.slotSettings_1.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
						break;
					case 2:
						if (MC3000Dialog.this.usbPort.isConnected()) MC3000Dialog.this.slotSettings_2 = MC3000Dialog.this.device.new SlotSettings(MC3000Dialog.this.usbPort.getSlotData(MC3000Dialog.this.usbInterface, MC3000UsbPort.QuerySlotData.SLOT_2.value()));
						label0.setText(MC3000Dialog.this.slotSettings_2 != null ? MC3000Dialog.this.slotSettings_2.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
						break;
					case 3:
						if (MC3000Dialog.this.usbPort.isConnected()) MC3000Dialog.this.slotSettings_3 = MC3000Dialog.this.device.new SlotSettings(MC3000Dialog.this.usbPort.getSlotData(MC3000Dialog.this.usbInterface, MC3000UsbPort.QuerySlotData.SLOT_3.value()));
						label0.setText(MC3000Dialog.this.slotSettings_3 != null ? MC3000Dialog.this.slotSettings_3.toString4View() : Messages.getString(MessageIds.GDE_MSGT3654));
						break;

					default:
						break;
					}
				}
				catch (Exception e) {
					MC3000Dialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
					MC3000Dialog.this.application.openMessageDialogAsync(e.getMessage());
				}
				finally {
					if (isConnectedByButton && MC3000Dialog.this.usbPort != null && MC3000Dialog.this.usbPort.isConnected()) {
						try {
							MC3000Dialog.this.usbPort.closeUsbPort(MC3000Dialog.this.usbInterface != null ? MC3000Dialog.this.usbInterface : null);
						}
						catch (UsbException e) {
							MC3000Dialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
						}
					}
				}
			}
		});
		switch (index) {
		case 0:
			boolean isEnabled = this.slotSettings_0 != null && this.slotSettings_0.isBusy() == false ? true : false;
			programmNameCombo.setEnabled(isEnabled);
			loadButton.setEnabled(isEnabled);
			writeButton.setEnabled(isEnabled);
			break;
		case 1:
			isEnabled = this.slotSettings_1 != null && this.slotSettings_1.isBusy() == false ? true : false;
			programmNameCombo.setEnabled(isEnabled);
			loadButton.setEnabled(isEnabled);
			writeButton.setEnabled(isEnabled);
			break;
		case 2:
			isEnabled = this.slotSettings_2 != null && this.slotSettings_2.isBusy() == false ? true : false;
			programmNameCombo.setEnabled(isEnabled);
			loadButton.setEnabled(isEnabled);
			writeButton.setEnabled(isEnabled);
			break;
		case 3:
			isEnabled = this.slotSettings_3 != null && this.slotSettings_3.isBusy() == false ? true : false;
			programmNameCombo.setEnabled(isEnabled);
			loadButton.setEnabled(isEnabled);
			writeButton.setEnabled(isEnabled);
			break;

		default:
			break;
		}

	}

	/**
	 * implementation of noop method from base dialog class
	 */
	@Override
	public void enableSaveButton(boolean enable) {
		this.saveButton.setEnabled(enable);
	}

	/**
	 * @return the tabFolder selection index
	 */
	public Integer getTabFolderSelectionIndex() {
		return this.tabFolderSelectionIndex;
	}

	//	/**
	//	 * toggle the text of start configuration button according comm port state
	//	 */
	//	void checkPortStatus() {
	//		GDE.display.asyncExec(new Runnable() {
	//			public void run() {
	//				if (!MC3000Dialog.this.startConfiguration.isDisposed()) {
	//					if (MC3000Dialog.this.device.usbPort.isConnected()) {
	//						MC3000Dialog.this.startConfiguration.setText("stop configurartion");
	//					}
	//					else {
	//						MC3000Dialog.this.startConfiguration.setText("start configurartion");
	//					}
	//				}
	//			}
	//		});
	//	}

	/**
	 * create minimal MC3000 XML data 
	 */
	private void createMC3000Setup() {
		this.mc3000Setup = new ObjectFactory().createMC3000Type();
		List<ProgramType> devicePrograms = this.mc3000Setup.getProgram();
		if (devicePrograms.size() < MC3000Dialog.NUMBER_PROGRAM_ENTRIES) { // initially create only base setup data
			for (int i = 0; i < MC3000Dialog.NUMBER_PROGRAM_ENTRIES; i++) {
				ProgramType program = new ObjectFactory().createProgramType();
				SetupData setupData = new ObjectFactory().createProgramTypeSetupData();
				setupData.setValue(new byte[64]);
				program.setSetupData(setupData);
				program.setName(this.programmNames[i]);
				devicePrograms.add(program);
			}
		}
	}

	private void saveMc3000SetupData() {
		try {
			// store back manipulated XML
			long startTime = new Date().getTime();
			Marshaller marshaller = this.jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.valueOf(true));
			marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, MC3000Dialog.MC3000_XSD);
			marshaller.marshal(this.mc3000Setup, new FileOutputStream(new File(this.settings.getApplHomePath() + "/MC3000_Slot_Programs" + GDE.FILE_ENDING_DOT_XML))); //$NON-NLS-1$
			MC3000Dialog.log.log(Level.TIME, "write program setup XML time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime))); //$NON-NLS-1$ //$NON-NLS-2$
			this.saveButton.setEnabled(false);
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
