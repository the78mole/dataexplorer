package gde.device.junsi.modbus;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.usb.UsbException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
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

import gde.GDE;
import gde.config.Settings;
import gde.data.Channels;
import gde.device.DeviceDialog;
import gde.device.IDevice;
import gde.device.junsi.MessageIds;
import gde.device.junsi.iCharger4010DUO;
import gde.device.junsi.iChargerUsb;
import gde.device.junsi.iChargerX6;
import gde.exception.TimeOutException;
import gde.io.DataParser;
import gde.log.Level;
import gde.log.LogFormatter;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.ParameterConfigControl;

public class ChargerDialog extends DeviceDialog {
	final static Logger								log													= Logger.getLogger(ChargerDialog.class.getName());
	static Handler										logHandler;
	static Logger											rootLogger;

	protected Shell										dialogShell;

	final IDevice											device;
	final static Channels							channels										= Channels.getInstance();
	final static Settings							settings										= Settings.getInstance();
	private ChargerUsbPort						usbPort											= null;
	private ChargerMemory							selectedProgramMemory				= null;
	private ChargerMemory							copiedProgramMemory					= null;
	private int												lastSelectedProgramMemoryIndex;
	byte[]														memoryHeadIndex							= new byte[ChargerMemoryHead.LIST_MEM_MAX];

	private Button										btnAdd, btnCopy, btnEdit, btnWrite, btnDelete;
	private Button										btnCharge, btnStorage, btnDischarge, btnCycle, btnBalance, btnStop;
	private Group											grpProgramMemory, grpBalancerSettings, grpAdvancedRestoreSettings, grpChargeSaftySettings, grpDischargeSaftySettings;
	private Composite									chargeComposite, dischargeComposite, storageComposite, cycleComposite;
	private CTabItem									tbtmStorage;
	private CTabFolder								tabFolder;

	private ParameterConfigControl[]	memoryParameters						= new ParameterConfigControl[50];																													// battery type, number cells, capacity
	private final String							cellTypeNames;
	private final String[]						cellTypeNamesArray;
	private int[]											memoryValues								= new int[50];																																						//values to be used for modifying sliders
	private final boolean							isDuo;
	final Listener										memoryParameterChangeListener;

	final static short		REG_INPUT_INFO_START				= 0x0000;
	//final short REG_INPUT_INFO_NREGS 

	final static short		REG_INPUT_CH1_START					= 0x0100;
	final static short		REG_INPUT_CH1_NREGS					= 0x0100;
	final static short		REG_INPUT_CH2_START					= (0x0100 + REG_INPUT_CH1_START);
	final static short		REG_INPUT_CH2_NREGS					= REG_INPUT_CH1_NREGS;

	final static short		REG_HOLDING_CTRL_START			= (short) 0x8000;
	final static short		REG_HOLDING_CTRL_NREGS			= 7;

	final static short		REG_HOLDING_SYS_START				= (short) 0x8400;
	final static short		REG_HOLDING_SYS_NREGS				= (short) ((ChargerSystem.getSize() + 1) / 2);

	final static short		REG_HOLDING_MEM_HEAD_START	= (short) 0x8800;
	final static short		REG_HOLDING_MEM_HEAD_NREGS	= (short) ((ChargerMemoryHead.getSize() + 1) / 2);

	final static short		REG_HOLDING_MEM_START				= (short) 0x8c00;

	final static short VALUE_ORDER_KEY	=	0x55aa;

	enum Order
	{
		ORDER_STOP,	
		ORDER_RUN,
		ORDER_MODIFY,
		ORDER_WRITE_SYS,
		ORDER_WRITE_MEM_HEAD,
		ORDER_WRITE_MEM,
		ORDER_TRANS_LOG_ON,
		ORDER_TRANS_LOG_OFF,
		ORDER_MSGBOX_YES,
		ORDER_MSGBOX_NO;
	};

	enum Register
	{
		REG_SEL_OP(REG_HOLDING_CTRL_START),
		REG_SEL_MEM((short)(REG_HOLDING_CTRL_START+1)),	
		REG_SEL_CHANNEL((short)(REG_HOLDING_CTRL_START+2)),		
		REG_ORDER_KEY((short)(REG_HOLDING_CTRL_START+3)),	
		REG_ORDER((short)(REG_HOLDING_CTRL_START+4)),	
		REG_CURRENT((short)(REG_HOLDING_CTRL_START+5)),	
		REG_VOLT((short)(REG_HOLDING_CTRL_START+6));
		
		short value;
		public final static Register VALUES[] = values();
		
		Register(short newValue) {
			this.value = newValue;
		}
	};


	/**
	* main method to test this dialog inside a dialogShell 
	*/
	public static void main(String[] args) {
		try {
			initLogger();
			Display display = Display.getDefault();
			Shell dialogShell = new Shell(display);
			iChargerUsb device = new iCharger4010DUO("c:\\Users\\Winfried\\AppData\\Roaming\\DataExplorer\\Devices\\iCharger406DUO.xml"); //$NON-NLS-1$
			boolean isDuo = true; //X6 = false
			ChargerDialog inst = new ChargerDialog(dialogShell, device);
			inst.open();
			ChargerUsbPort usbPort = new ChargerUsbPort(device, null);
			if (usbPort != null && !usbPort.isConnected()) 
				usbPort.openMbUsbPort();
			
			if (usbPort != null && usbPort.isConnected()) {
				//Read system setup data
				//MasterRead(0,REG_HOLDING_SYS_START,(sizeof(SYSTEM)+1)/2,(BYTE *)&System)		
				short sizeSystem = (short) ((ChargerSystem.getSize() + 1) / 2);
				byte[] systemBuffer = new byte[sizeSystem*2];
				usbPort.masterRead((byte) 0, REG_HOLDING_SYS_START, sizeSystem, systemBuffer);
				log.log(Level.INFO, new ChargerSystem(systemBuffer).toString());
				
				//Read memory structure of original and added/modified program memories
				//MasterWrite(REG_HOLDING_MEM_HEAD_START,(sizeof(MEM_HEAD)+1)/2,(BYTE *)&MemHead)
				short sizeMemHead = (short) ((ChargerMemoryHead.getSize() + 1) / 2);
				byte[] memHeadBuffer = new byte[sizeMemHead*2];
				usbPort.masterRead((byte) 0, REG_HOLDING_MEM_HEAD_START, sizeMemHead, memHeadBuffer);
				ChargerMemoryHead memHead = new ChargerMemoryHead(memHeadBuffer);
				log.log(Level.INFO, memHead.toString());
				
				for (int i = 0; i < memHead.getCount(); ++i) {
					//Read charger program memory after write index selection
					//MasterWrite(REG_SEL_MEM,1,(BYTE *)&Index) != MB_EOK
					byte[] index = new byte[2];
					index[0] = memHead.getIndex()[i];
					log.log(Level.INFO, String.format("select mem index %d", DataParser.parse2Short(index[0], index[1]))); //$NON-NLS-1$
					usbPort.masterWrite(Register.REG_SEL_MEM.value, (short)1, index);
					
					//MasterRead(0,REG_HOLDING_MEM_START,(sizeof(MEMORY)+1)/2,(BYTE *)&Memory) == MB_EOK
					short sizeMemory = (short) ((ChargerMemory.getSize(true) + 1) / 2);
					byte[] memoryBuffer = new byte[sizeMemory*2];
					usbPort.masterRead((byte) 0, REG_HOLDING_MEM_START, sizeMemory, memoryBuffer);
					log.log(Level.INFO, String.format("%02d %s", memHead.getIndex()[i], new ChargerMemory(memoryBuffer, isDuo).getUseFlagAndName())); //$NON-NLS-1$
					log.log(Level.INFO, new ChargerMemory(memoryBuffer, isDuo).toString(isDuo));
				}
				
			}
			
			if (usbPort != null && usbPort.isConnected()) 
				usbPort.closeMbUsbPort();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void readSystem() {
		try {
			if (usbPort != null && !usbPort.isConnected()) usbPort.openMbUsbPort();

			//Read system setup data
			//MasterRead(0,REG_HOLDING_SYS_START,(sizeof(SYSTEM)+1)/2,(BYTE *)&System)		
			short sizeSystem = (short) ((ChargerSystem.getSize() + 1) / 2);
			byte[] systemBuffer = new byte[sizeSystem * 2];
			usbPort.masterRead((byte) 0, REG_HOLDING_SYS_START, sizeSystem, systemBuffer);
			log.log(Level.INFO, new ChargerSystem(systemBuffer).toString());
		}
		catch (UsbException | IllegalStateException | TimeOutException e) {
			e.printStackTrace();
		}
		finally {
			if (usbPort != null && usbPort.isConnected()) try {
				usbPort.closeMbUsbPort();
			}
			catch (UsbException e) {
				e.printStackTrace();
			}
		}

	}
	/**
	 * default constructor initialize all variables required
	 * @param parent dialogShell
	 * @param useDevice device specific class implementation
	 */
	public ChargerDialog(Shell parent, iChargerUsb useDevice) {
		super(parent, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
		this.device = useDevice;
		this.usbPort = new ChargerUsbPort(this.device, null);
		setText(this.device.getName());
		this.isDuo = this.device.getName().toLowerCase().endsWith("duo") ? true : false; //$NON-NLS-1$
		String[] tmpNamesArray = this.isDuo ? iChargerUsb.BatteryTypesDuo.getValues() : iChargerX6.BatteryTypesX.getValues();
		this.cellTypeNamesArray = new String[tmpNamesArray.length - (isDuo ? 2 : 4)];
		System.arraycopy(tmpNamesArray, 1, this.cellTypeNamesArray, 0, this.cellTypeNamesArray.length);		
		this.cellTypeNames = String.join(", ", this.cellTypeNamesArray); //$NON-NLS-1$
		this.memoryParameterChangeListener = addProgramMemoryChangedListener();
		this.readSystem();
	}

	private Listener addProgramMemoryChangedListener() {
		return new Listener() {
			@Override
			public void handleEvent(Event evt) {
				if (selectedProgramMemory != null) {
					btnWrite.setEnabled(true);
					switch (evt.index) {
					case 0: //battery type, disabled can be changed by selection in drop down only
					default:
						break;
					case 1: //cell count
						if (isDuo) {
							switch (selectedProgramMemory.getType()) { //battery type LiPo,LiLo,LiFe,NiMH,Nicd,Pb,NiZn,LiHV
							case 0: //LiPo
							case 1: //LiIo
							case 2: //LiFe
							case 7: //LiHV
								selectedProgramMemory.setLiCell((byte) ChargerDialog.this.memoryValues[1]);
								break;
							case 3: //NiMH
							case 4: //NiCd
								selectedProgramMemory.setNiCell((byte) ChargerDialog.this.memoryValues[1]);
								break;
							case 6: //NiZn
								selectedProgramMemory.setNiZnCell((byte) ChargerDialog.this.memoryValues[1]);
								break;
							case 5: //Pb
								selectedProgramMemory.setPbCell((byte) ChargerDialog.this.memoryValues[1]);
								break;
							default: //unknown
								break;
							}
						}
						else {
							switch (selectedProgramMemory.getType()) { //battery type LiPo,LiLo,LiFe,LiHV,LTO, NiMH,Nicd,NiZn,Pb
							case 0: //LiPo
							case 1: //LiIo
							case 2: //LiFe
							case 3: //LiHV
							case 4: //LTO
								selectedProgramMemory.setLiCell((byte) ChargerDialog.this.memoryValues[1]);
								break;
							case 7: //NiZn
								selectedProgramMemory.setNiZnCell((byte) ChargerDialog.this.memoryValues[1]);
								break;
							case 5: //NiMH
							case 6: //NiCd
								selectedProgramMemory.setNiCell((byte) ChargerDialog.this.memoryValues[1]);
								break;
							case 8: //Pb
								selectedProgramMemory.setPbCell((byte) ChargerDialog.this.memoryValues[1]);
								break;
							default: //Power
							}
						}
						break;
					case 2: //capacity
						selectedProgramMemory.setCapacity(ChargerDialog.this.memoryValues[2]);
						break;
					case 3: // charge parameter current
						selectedProgramMemory.setChargeCurrent((short) (ChargerDialog.this.memoryValues[3] / 10));
						break;
					case 4: // charge parameter modus normal,balance,external,reflex 
						if (isDuo) {
							switch (selectedProgramMemory.getType()) { //battery type LiPo,LiLo,LiFe,NiMH,Nicd,Pb,NiZn,LiHV
							case 0: //LiPo
							case 1: //LiIo
							case 2: //LiFe
							case 6: //NiZn
							case 7: //LiHV
								selectedProgramMemory.setLiModeC((byte) ChargerDialog.this.memoryValues[4]);
								break;
							case 3: //NiMH
							case 4: //NiCd
								selectedProgramMemory.setNiModeC((byte) ChargerDialog.this.memoryValues[4]);
								break;
							case 5: //Pb
								selectedProgramMemory.setPbModeC((byte) ChargerDialog.this.memoryValues[4]);
								break;
							default: //unknown
								break;
							}
						}
						else {
							switch (selectedProgramMemory.getType()) { //battery type LiPo,LiLo,LiFe,LiHV,LTO, NiMH,Nicd,NiZn,Pb
							case 0: //LiPo
							case 1: //LiIo
							case 2: //LiFe
							case 3: //LiHV
							case 4: //LTO
							case 7: //NiZn
								selectedProgramMemory.setLiModeC((byte) ChargerDialog.this.memoryValues[4]);
								break;
							case 5: //NiMH
							case 6: //NiCd
								selectedProgramMemory.setNiModeC((byte) ChargerDialog.this.memoryValues[4]);
								break;
							case 8: //Pb
								selectedProgramMemory.setPbModeC((byte) ChargerDialog.this.memoryValues[4]);
								break;
							default: //Power
							}
						}
						break;
					case 5: // charge parameter balancer Li Setup
						selectedProgramMemory.setLiBalEndMode((byte) ChargerDialog.this.memoryValues[5]); //end current OFF, detect balancer ON
						break;
					case 6: // charge parameter end current
						selectedProgramMemory.setEndCharge((short) ChargerDialog.this.memoryValues[6]);
						break;
					case 7: // charge parameter cell voltage
						if (isDuo) {
							switch (selectedProgramMemory.getType()) { //battery type LiPo,LiLo,LiFe,NiMH,Nicd,Pb,NiZn,LiHV
							case 0: //LiPo
								selectedProgramMemory.setLiPoChgCellVolt((short) ChargerDialog.this.memoryValues[7]); 
								break;
							case 1: //LiIo
								selectedProgramMemory.setLiIoChgCellVolt((short) ChargerDialog.this.memoryValues[7]); 
								break;
							case 2: //LiFe
								selectedProgramMemory.setLiFeChgCellVolt((short) ChargerDialog.this.memoryValues[7]); 
								break;
							case 6: //NiZn
								selectedProgramMemory.setNiZnChgCellVolt((short) ChargerDialog.this.memoryValues[7]); 
								break;
							case 7: //LiHV
							case 3: //NiMH
							case 4: //NiCd
							case 5: //Pb
							default: 
								break;
							}
						}
						else {
							switch (selectedProgramMemory.getType()) { //battery type LiPo,LiLo,LiFe,LiHV,LTO, NiMH,Nicd,NiZn,Pb
							case 0: //LiPo
								selectedProgramMemory.setLiPoChgCellVolt((short) ChargerDialog.this.memoryValues[7]); 
								break;
							case 1: //LiIo
								selectedProgramMemory.setLiIoChgCellVolt((short) ChargerDialog.this.memoryValues[7]); 
								break;
							case 2: //LiFe
								selectedProgramMemory.setLiFeChgCellVolt((short) ChargerDialog.this.memoryValues[7]); 
								break;
							case 7: //NiZn
								selectedProgramMemory.setNiZnChgCellVolt((short) ChargerDialog.this.memoryValues[7]); 
								break;
							case 3: //LiHV
							case 4: //LTO
							case 5: //NiMH
							case 6: //NiCd
							case 8: //Pb
							default: 
							}
						}
						break;
					case 8: // charge cut temperature
						selectedProgramMemory.setSafetyTempC((short) (ChargerDialog.this.memoryValues[8] * 10));
						break;
					case 9: // charge max charge
						selectedProgramMemory.setSafetyCapC((short) ChargerDialog.this.memoryValues[9]);
						break;
					case 10: // charge parameter balancer difference
						selectedProgramMemory.setSafetyTimeC((short) ChargerDialog.this.memoryValues[10]);
						break;
					case 11: // charge parameter balancer
						selectedProgramMemory.setBalSpeed((byte) ChargerDialog.this.memoryValues[11]);
						break;
					case 12: // charge parameter balancer start
						selectedProgramMemory.setBalStartMode((byte) ChargerDialog.this.memoryValues[12]);
						break;
					case 13: // charge parameter balancer difference
						selectedProgramMemory.setBalDiff((byte) ChargerDialog.this.memoryValues[13]);
						break;
					case 14: // charge parameter balancer target
						selectedProgramMemory.setBalSetPoint((byte) ChargerDialog.this.memoryValues[14]);
						break;
					case 15: // charge parameter balancer over charge
						selectedProgramMemory.setBalOverPoint((byte) ChargerDialog.this.memoryValues[15]);
						break;
					case 16: // charge parameter balancer delay
						selectedProgramMemory.setBalDelay((byte) ChargerDialog.this.memoryValues[16]);
						break;
					//discharge begin	
					case 17: // discharge parameter current
						selectedProgramMemory.setDischargeCurrent((short) (ChargerDialog.this.memoryValues[17] / 10));
						break;
					case 19: // discharge end current
						selectedProgramMemory.setEndDischarge((short) ChargerDialog.this.memoryValues[19]);
					case 20: // regenerative mode 
						selectedProgramMemory.setRegDchgMode((short) ChargerDialog.this.memoryValues[20]);
					case 18: // discharge parameter cell voltage
					case 21: // discharge extra
					case 22: // discharge balancer
						if (isDuo) {
							switch (selectedProgramMemory.getType()) { //battery type LiPo,LiLo,LiFe,NiMH,Nicd,Pb,NiZn,LiHV
							case 0: //LiPo
								selectedProgramMemory.setLiPoDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 1: //LiIo
								selectedProgramMemory.setLiIoDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 2: //LiFe
								selectedProgramMemory.setLiFeDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 3: //NiMH
							case 4: //NiCd
								selectedProgramMemory.setNiDischargeVolt((short) ChargerDialog.this.memoryValues[18]);
								selectedProgramMemory.setNiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 5: //Pb
								selectedProgramMemory.setPbDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								selectedProgramMemory.setPbModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 6: //NiZn
								selectedProgramMemory.setNiZnDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 7: //LiHV
								selectedProgramMemory.setLiHVDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							default: 
								break;
							}
						}
						else {
							switch (selectedProgramMemory.getType()) { //battery type LiPo,LiLo,LiFe,LiHV,LTO, NiMH,Nicd,NiZn,Pb
							case 0: //LiPo
								selectedProgramMemory.setLiPoDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 1: //LiIo
								selectedProgramMemory.setLiIoDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 2: //LiFe
								selectedProgramMemory.setLiFeDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 3: //LiHV
								selectedProgramMemory.setLiHVDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 4: //LTO
								selectedProgramMemory.setLtoDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 5: //NiMH
							case 6: //NiCd
								selectedProgramMemory.setNiDischargeVolt((short) ChargerDialog.this.memoryValues[18]);
								selectedProgramMemory.setNiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 7: //NiZn
								selectedProgramMemory.setNiZnDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								selectedProgramMemory.setLiModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							case 8: //Pb
								selectedProgramMemory.setPbDchgCellVolt((short) ChargerDialog.this.memoryValues[18]);
								selectedProgramMemory.setPbModeD((byte) (ChargerDialog.this.memoryValues[21] + (ChargerDialog.this.memoryValues[22] << 1)));
								break;
							default: 
								break;
							}
						}
						break;
					case 23: // discharge parameter cut temperature
						selectedProgramMemory.setSafetyTempD((short) (ChargerDialog.this.memoryValues[23] * 10)); 
						break;
					case 24: // discharge parameter max discharge capacity
						selectedProgramMemory.setSafetyCapD((short) ChargerDialog.this.memoryValues[24]);
						break;
					case 25: // discharge parameter safety timer
						selectedProgramMemory.setSafetyTimeD((short) ChargerDialog.this.memoryValues[25]);
						break;
					case 26: // Ni charge voltage drop
						selectedProgramMemory.setNiPeak((short) ChargerDialog.this.memoryValues[26]);
						break;
					case 27: // Ni charge voltage drop delay
						selectedProgramMemory.setNiPeakDelay((short) ChargerDialog.this.memoryValues[27]);
						break;
					case 28: // Ni charge allow 0V 
						selectedProgramMemory.setNiZeroEnable((short) ChargerDialog.this.memoryValues[28]);
						break;
					case 29: // Ni trickle charge enable 
						selectedProgramMemory.setNiTrickleEnable((short) ChargerDialog.this.memoryValues[29]);
						break;
					case 30: // Ni charge trickle current
						selectedProgramMemory.setNiTrickleCurrent((short) ChargerDialog.this.memoryValues[30]);
						break;
					case 31: // Ni charge trickle timeout
						selectedProgramMemory.setNiTrickleTime((short) ChargerDialog.this.memoryValues[31]);
						break;

					case 32: // charge restore lowest voltage
						selectedProgramMemory.setRestoreVolt((short) ChargerDialog.this.memoryValues[32]);
						break;
					case 33: // charge restore charge time
						selectedProgramMemory.setRestoreTime((short) ChargerDialog.this.memoryValues[33]);
						break;
					case 34: // charge restore charge current
						selectedProgramMemory.setRestoreCurent((short) ChargerDialog.this.memoryValues[34]);
						break;
					case 35: // charge keep charging after done
						selectedProgramMemory.setKeepChargeEnable((byte) ChargerDialog.this.memoryValues[35]);
						break;

					case 36: // storage parameter cell voltage (Li battery type only)
						if (isDuo) {
							switch (selectedProgramMemory.getType()) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
							case 0: //LiPo
								selectedProgramMemory.setLiPoStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 1: //LiIo
								selectedProgramMemory.setLiLoStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 2: //LiFe
								selectedProgramMemory.setLiFeStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 7: //LiHV
								selectedProgramMemory.setLiHVStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 3: //NiMH
							case 4: //NiCd
							case 5: //Pb
							case 6: //NiZn
							default: 
								break;
							}
						}
						else {
							switch (selectedProgramMemory.getType()) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
							case 0: //LiPo
								selectedProgramMemory.setLiPoStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 1: //LiIo
								selectedProgramMemory.setLiLoStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 2: //LiFe
								selectedProgramMemory.setLiFeStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 3: //LiHV
								selectedProgramMemory.setLiHVStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 4: //LTO
								selectedProgramMemory.setLtoStoCellVolt((short) ChargerDialog.this.memoryValues[36]);
								break;
							case 5: //NiMH
							case 6: //NiCd
							case 7: //NiZn
							case 8: //Pb
							default: 
								break;
							}
						}
						break;
					case 37: // storage parameter compensation
						selectedProgramMemory.setStoCompensation( (short) ChargerDialog.this.memoryValues[37]);
						break;
					case 38: // storage acceleration
						selectedProgramMemory.setFastSto( (byte) ChargerDialog.this.memoryValues[38]);
						break;

					case 39: // cycle parameter mode CHG->DCHG
						selectedProgramMemory.setCycleMode((byte) ChargerDialog.this.memoryValues[39]);
						break;
					case 40: // cycle count
						selectedProgramMemory.setCycleCount((short) ChargerDialog.this.memoryValues[40]);
						break;
					case 41: // cycle delay
						selectedProgramMemory.setCycleDelay((short) ChargerDialog.this.memoryValues[41]);
						break;

					}
					memoryValues = selectedProgramMemory.getMemoryValues(memoryValues, isDuo);
					updateMemoryParameterControls();
				}
			}
		};
	}

	private static void initLogger() {
		logHandler = new ConsoleHandler();
		logHandler.setFormatter(new LogFormatter());
		logHandler.setLevel(Level.INFO);
		rootLogger = Logger.getLogger(GDE.STRING_EMPTY);
		// clean up all handlers from outside
		Handler[] handlers = rootLogger.getHandlers();
		for (Handler handler : handlers) {
			rootLogger.removeHandler(handler);
		}
		rootLogger.setLevel(Level.ALL);
		rootLogger.addHandler(logHandler);
	}

	/**
	 * read the program memory structure and initialize build in program memory 0 LiPo
	 * @return string array to fill drop down box
	 */
	private String[] getProgramMemories() {
		List<String> programMemories = new ArrayList<String>();

		try {			
			if (usbPort != null && !usbPort.isConnected()) 
				usbPort.openMbUsbPort();
			
			if (usbPort != null && usbPort.isConnected()) {
				//Read memory structure of original and added/modified program memories
				short sizeMemHead = (short) ((ChargerMemoryHead.getSize() + 1) / 2);
				byte[] memHeadBuffer = new byte[sizeMemHead*2];
				usbPort.masterRead((byte) 0, REG_HOLDING_MEM_HEAD_START, sizeMemHead, memHeadBuffer);			
				ChargerMemoryHead memHead = new ChargerMemoryHead(memHeadBuffer);
				log.log(Level.OFF, memHead.toString());
				
				int i = 0;
				byte[] index = new byte[2];
				short sizeMemory = (short) ((ChargerMemory.getSize(isDuo) + 1) / 2);
				byte[] memoryBuffer;
				memoryHeadIndex = memHead.getIndex();

				for (; i < memHead.getCount(); ++i) {
					//Read charger program memory after writing selected index
					//index[0] = (byte) i; //order number
					index[0] = memoryHeadIndex[i]; //order logical
					log.log(Level.INFO, String.format("select mem index %d", DataParser.parse2Short(index[0], index[1]))); //$NON-NLS-1$
					usbPort.masterWrite(Register.REG_SEL_MEM.value, (short)1, index);
					
					//Read selected charger program memory
					memoryBuffer = new byte[sizeMemory*2];
					usbPort.masterRead((byte) 0, REG_HOLDING_MEM_START, sizeMemory, memoryBuffer);
					programMemories.add(String.format("%02d - %s", memoryHeadIndex[i], new ChargerMemory(memoryBuffer, isDuo).getUseFlagAndName())); //orde logical //$NON-NLS-1$
					log.log(Level.INFO, programMemories.get(programMemories.size() - 1));
				}		
				initProgramMemory(0);
			}					
		}
		catch (IllegalStateException | UsbException | TimeOutException e) {
			if (e instanceof UsbException) {
				application.openMessageDialogAsync(e.getMessage());
			}
			e.printStackTrace();
		}
		finally {
			if (usbPort != null && usbPort.isConnected()) try {
				usbPort.closeMbUsbPort();
			}
			catch (UsbException e) {
				e.printStackTrace();
			}
		}
		return programMemories.size() == 0 ? new String[0] : programMemories.toArray(new String[1]);
	}

	/**
	 * initialize program memory with the given index and set selectedProgram memory local variable
	 * @param selectedProgramMemoryIndex the index of the selected program memory according memory structure
	 * @return byte array containing the program memory
	 */
	private byte[] initProgramMemory(int selectedProgramMemoryIndex) {
		short sizeMemory = (short) ((ChargerMemory.getSize(isDuo) + 1) / 2);
		byte[] memoryBuffer = new byte[sizeMemory*2];
		log.log(Level.OFF, "read using memory buffer length " + memoryBuffer.length); //$NON-NLS-1$
		byte[] index = new byte[2];
		index[0] = (byte) (selectedProgramMemoryIndex & 0xFF);
		try {
			if (usbPort != null && !usbPort.isConnected()) 
				usbPort.openMbUsbPort();

			usbPort.masterWrite(Register.REG_SEL_MEM.value, (short)1, index);
			
			usbPort.masterRead((byte) 0, REG_HOLDING_MEM_START, sizeMemory, memoryBuffer);
			this.selectedProgramMemory = new ChargerMemory(memoryBuffer, isDuo);
			log.log(Level.OFF, this.selectedProgramMemory.toString(isDuo));
		}
		catch (UsbException | IllegalStateException | TimeOutException e) {
			e.printStackTrace();
		}
		finally {
			if (usbPort != null && usbPort.isConnected()) try {
				usbPort.closeMbUsbPort();
			}
			catch (UsbException e) {
				e.printStackTrace();
			}
		}
		return memoryBuffer;
	}

	/**
	 * write modified program memory to selected program memory index of memory head structure
	 * this method will update the useFlag to 0x55AA to differentiate to build in memories
	 * @param selectedProgramMemoryIndex the index of the selected program memory according memory head structure
	 * @param modifiedProgramMemory the modified program memory class
	 */
	private void writeProgramMemory(final int selectedProgramMemoryIndex, ChargerMemory modifiedProgramMemory) {
		short sizeMemory = (short) ((ChargerMemory.getSize(isDuo) + 1) / 2);
		byte[] index = new byte[2];
		index[0] = (byte) (selectedProgramMemoryIndex & 0xFF);
		
		if ((this.isDuo && selectedProgramMemoryIndex < 7) || (!isDuo && selectedProgramMemoryIndex < 10)) {
			log.log(Level.SEVERE, String.format(Messages.getString(MessageIds.GDE_MSGT2621), selectedProgramMemoryIndex)); //$NON-NLS-1$
			this.application.openMessageDialog(dialogShell, Messages.getString(MessageIds.GDE_MSGT2622)); //$NON-NLS-1$
			return;
		}
		
		try {
			if (usbPort != null && !usbPort.isConnected()) usbPort.openMbUsbPort();

			usbPort.masterWrite(Register.REG_SEL_MEM.value, (short) 1, index);

			modifiedProgramMemory.setUseFlag((short) 0x55aa);
			log.log(Level.OFF, String.format("Program memory name = %s", new String(modifiedProgramMemory.getName()).trim())); //$NON-NLS-1$
			log.log(Level.OFF, "write using memory buffer length " + sizeMemory * 2); //$NON-NLS-1$
			//modifiedProgramMemory.setLiCell((byte) ((iChargerUsb) device).getNumberOfLithiumCells());
			//System.arraycopy(new String("Space Pro 4S 2200mAh").getBytes(), 0, modifiedProgramMemory, 2, "Space Pro 4S 2200mAh".length());
			usbPort.masterWrite(REG_HOLDING_MEM_START, sizeMemory, modifiedProgramMemory.getAsByteArray(isDuo));

			transOrder((byte) Order.ORDER_WRITE_MEM.ordinal());
		}
		catch (UsbException | IllegalStateException | TimeOutException e) {
			e.printStackTrace();
		}
		finally {
			if (usbPort != null && usbPort.isConnected()) try {
				usbPort.closeMbUsbPort();
			}
			catch (UsbException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * add an entry to memory head structure 
	 * @param batTypeName defines the index device dependent where to add a new program memory
	 * @return the selected program memory index of the memory head structure
	 */
	private short addEntryMemoryHead(String batTypeName) {
		short newHeadIndex = -1;
		try {			
			if (usbPort != null && !usbPort.isConnected()) 
				usbPort.openMbUsbPort();
			
			if (usbPort != null && usbPort.isConnected()) {
				//Read memory structure of original and added/modified program memories
				short sizeMemHead = (short) ((ChargerMemoryHead.getSize() + 1) / 2);
				byte[] memHeadBuffer = new byte[sizeMemHead*2];
				usbPort.masterRead((byte) 0, REG_HOLDING_MEM_HEAD_START, sizeMemHead, memHeadBuffer);			
				ChargerMemoryHead memHead = new ChargerMemoryHead(memHeadBuffer);
				newHeadIndex = memHead.getCount();
				//log.log(Level.OFF, memHead.toString());
				
				//program memory count = 18
				memHead.setIndex(memHead.addIndexAfter((byte)(((iChargerUsb)device).getBatTypeIndex(batTypeName) - 1)));
				//0, 17, 16, 15, 14, 13, 12, 10, 1, 2, 11, 3, 4, 5, 6, 7, 8, 9,
				memHead.setCount((short)(memHead.getCount() + 1));
				log.log(Level.OFF, memHead.toString());
				
				//write the updated memory head structure to device
				usbPort.masterWrite(REG_HOLDING_MEM_HEAD_START, sizeMemHead, memHead.getAsByteArray());
				transOrder((byte) Order.ORDER_WRITE_MEM_HEAD.ordinal());
			}					
		}
		catch (IllegalStateException | UsbException | TimeOutException e) {
			if (e instanceof UsbException) {
				application.openMessageDialogAsync(e.getMessage());
			}
			e.printStackTrace();
		}
		finally {
			if (usbPort != null && usbPort.isConnected()) try {
				usbPort.closeMbUsbPort();
			}
			catch (UsbException e) {
				e.printStackTrace();
			}
		}
		return newHeadIndex;
	}

	/**
	 * remove given index from program memory head structure
	 * @param removeProgramMemoryIndex the index of the selected program memory according memory structure
	 */
	private void removeMemoryHead(byte removeProgramMemoryIndex) {
		try {			
			if (usbPort != null && !usbPort.isConnected()) 
				usbPort.openMbUsbPort();
			
			if (usbPort != null && usbPort.isConnected()) {
				//Read memory structure of original and added/modified program memories
				short sizeMemHead = (short) ((ChargerMemoryHead.getSize() + 1) / 2);
				byte[] memHeadBuffer = new byte[sizeMemHead*2];
				usbPort.masterRead((byte) 0, REG_HOLDING_MEM_HEAD_START, sizeMemHead, memHeadBuffer);			
				ChargerMemoryHead memHead = new ChargerMemoryHead(memHeadBuffer);
				log.log(Level.OFF, String.format("before modification: %s", memHead.toString())); //$NON-NLS-1$
				
				memHead.setIndex(memHead.removeIndex(removeProgramMemoryIndex));
				memHead.setCount((short)(memHead.getCount() - 1));
				log.log(Level.OFF, String.format("after modification: %s", memHead.toString())); //$NON-NLS-1$
				
				//write the updated memory head structure to device
				usbPort.masterWrite(REG_HOLDING_MEM_HEAD_START, sizeMemHead, memHead.getAsByteArray());
				transOrder((byte) Order.ORDER_WRITE_MEM_HEAD.ordinal());
			}					
		}
		catch (IllegalStateException | UsbException | TimeOutException ex) {
			if (ex instanceof UsbException) {
				application.openMessageDialogAsync(ex.getMessage());
			}
			ex.printStackTrace();
		}
		finally {
			if (usbPort != null && usbPort.isConnected()) try {
				usbPort.closeMbUsbPort();
			}
			catch (UsbException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * confirm the previous written data
	 * @param order order key
	 * @throws IllegalStateException
	 * @throws TimeOutException
	 */
	private void transOrder(byte order) throws IllegalStateException, TimeOutException
	{
		byte[] temp = new byte[4];
		temp[0] = (byte) (VALUE_ORDER_KEY & 0xFF);
		temp[1] = (byte) (VALUE_ORDER_KEY >> 8);
		temp[2] = order;
		usbPort.masterWrite(Register.REG_ORDER_KEY.value, (short)2 , temp);

		temp[0] = temp[1] = 0;
		usbPort.masterWrite(Register.REG_ORDER_KEY.value, (short)1 , temp);
	}
	
	/**
	 * start a program execution at device
	 * @param programType type of program to execute enum {charge, storage, discharge, cycle, balance} 
	 * @param channel number of channel enum {channel1, channel2}
	 * @param programMemoryIndex the index of the selected program memory according memory structure
	 */
	private void startProgramExecution(byte programType, byte channel, byte programMemoryIndex) {
		try {
			if (usbPort != null && !usbPort.isConnected()) usbPort.openMbUsbPort();

			if (usbPort != null && usbPort.isConnected()) {
				byte[] runOrderBuf = new byte[10];
				runOrderBuf[0] = programType;// enum {charge, storage, discharge, cycle, balance};
				runOrderBuf[2] = programMemoryIndex;
				runOrderBuf[4] = channel;
				runOrderBuf[6] = (byte) (VALUE_ORDER_KEY & 0xFF);
				runOrderBuf[7] = (byte) (VALUE_ORDER_KEY >> 8);
				runOrderBuf[8] = (byte) Order.ORDER_RUN.ordinal();
				usbPort.masterWrite(Register.REG_SEL_OP.value, (short) 5, runOrderBuf);
			}
		}
		catch (IllegalStateException | UsbException | TimeOutException e) {
			if (e instanceof UsbException) {
				application.openMessageDialogAsync(e.getMessage());
			}
			e.printStackTrace();
		}
		finally {
			if (usbPort != null && usbPort.isConnected()) try {
				usbPort.closeMbUsbPort();
			}
			catch (UsbException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * stop a program memory execution
	 * @param channel number of channel enum {channel1, channel2}
	 */
	private void stopProgramExecution(byte channel) {
		try {
			if (usbPort != null && !usbPort.isConnected()) usbPort.openMbUsbPort();

			if (usbPort != null && usbPort.isConnected()) {
				byte[] runOrderBuf = new byte[6];
				runOrderBuf[0] = channel;
				runOrderBuf[2] = (byte) (VALUE_ORDER_KEY & 0xFF);
				runOrderBuf[3] = (byte) (VALUE_ORDER_KEY >> 8);
				runOrderBuf[4] = (byte) Order.ORDER_STOP.ordinal();

				usbPort.masterWrite(Register.REG_SEL_CHANNEL.value, (short) 3, runOrderBuf);
			}
		}
		catch (IllegalStateException | UsbException | TimeOutException e) {
			if (e instanceof UsbException) {
				application.openMessageDialogAsync(e.getMessage());
			}
			e.printStackTrace();
		}
		finally {
			if (usbPort != null && usbPort.isConnected()) try {
				usbPort.closeMbUsbPort();
			}
			catch (UsbException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public ChargerDialog(Shell parent, int style) {
		super(parent, style);
		this.device = DataExplorer.getInstance().getActiveDevice();
		this.usbPort = new ChargerUsbPort(this.device, null);
		setText(this.device.getName());
		this.isDuo = this.device.getName().toLowerCase().endsWith("duo") ? true : false; //$NON-NLS-1$
		String[] tmpNamesArray = this.isDuo ? iChargerUsb.BatteryTypesDuo.getValues() : iChargerX6.BatteryTypesX.getValues();
		this.cellTypeNamesArray = new String[tmpNamesArray.length - (isDuo ? 2 : 4)];
		System.arraycopy(tmpNamesArray, 1, this.cellTypeNamesArray, 0, this.cellTypeNamesArray.length);		
		this.cellTypeNames = String.join(", ", this.cellTypeNamesArray); //$NON-NLS-1$
		this.memoryParameterChangeListener = addProgramMemoryChangedListener();
		this.readSystem();
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public void open() {
		createContents();
		dialogShell.setLocation(300,  50);
		dialogShell.open();
		if (((iChargerUsb)device).isDeviceActive()) {
			btnCharge.setEnabled(false);
			btnStorage.setEnabled(false);
			btnDischarge.setEnabled(false);
			btnCycle.setEnabled(false);
			btnBalance.setEnabled(false);
			btnStop.setEnabled(true);
			grpProgramMemory.setEnabled(false);
		}
		dialogShell.layout();
		Display display = getParent().getDisplay();
		while (!dialogShell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		dialogShell = new Shell(getParent(), getStyle());
		dialogShell.setSize(750, 740);
		dialogShell.setText(getText());
		dialogShell.setLayout(new FillLayout());
		
		Composite mainComposite = new Composite(dialogShell, SWT.NONE);
		mainComposite.setLayout(new FormLayout());
		
		grpProgramMemory = new Group(mainComposite, SWT.NONE);
		grpProgramMemory.setText(Messages.getString(MessageIds.GDE_MSGT2623)); //$NON-NLS-1$
		RowLayout rl_grpMemory = new RowLayout(SWT.HORIZONTAL);
		rl_grpMemory.justify = true;
		rl_grpMemory.fill = true;
		grpProgramMemory.setLayout(rl_grpMemory);
		FormData fd_grpMemory = new FormData();
		fd_grpMemory.top = new FormAttachment(0, 10);
		fd_grpMemory.right = new FormAttachment(100, -10);
		fd_grpMemory.bottom = new FormAttachment(0, 75);
		fd_grpMemory.left = new FormAttachment(0, 10);
		grpProgramMemory.setLayoutData(fd_grpMemory);
		
		CCombo combo = new CCombo(grpProgramMemory, SWT.BORDER);
		combo.setLayoutData(new RowData(300, 23));
		combo.setItems(((iChargerUsb)device).isDeviceActive() ? new String[] {Messages.getString(MessageIds.GDE_MSGT2624)} : this.getProgramMemories()); //$NON-NLS-1$
		combo.select(0);
		combo.setBackground(application.COLOR_WHITE);
		combo.setEditable(false);
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				combo.setForeground(application.COLOR_BLACK);
				initProgramMemory(memoryHeadIndex[combo.getSelectionIndex()]);
				memoryValues = selectedProgramMemory.getMemoryValues(memoryValues, isDuo);
				updateMemoryParameterControls();
				lastSelectedProgramMemoryIndex = memoryHeadIndex[combo.getSelectionIndex()];
				if (combo.getText().contains("BUILD IN")) { //$NON-NLS-1$
					btnCopy.setEnabled(true);
					//btnEdit.setEnabled(true); //enable line to fix corrupted memory
				}
				else {
					btnCopy.setEnabled(false);
					btnEdit.setEnabled(true);
					btnDelete.setEnabled(true);
				}
				btnAdd.setEnabled(false);
			}
		});
		combo.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent evt) {
				if (evt.character == SWT.CR) {
					combo.setEditable(false);
					String changedProgramMemoryText = combo.getText().trim();
					if (changedProgramMemoryText.contains(" - ")) { //$NON-NLS-1$
	 					changedProgramMemoryText = changedProgramMemoryText.substring(changedProgramMemoryText.lastIndexOf(" - ")+3); //$NON-NLS-1$
					}
					combo.setText(lastSelectedProgramMemoryIndex + Messages.getString(MessageIds.GDE_MSGT2625) + changedProgramMemoryText);		 //$NON-NLS-1$
					combo.setForeground(application.COLOR_RED);
					if (selectedProgramMemory != null) {
						selectedProgramMemory.setName(changedProgramMemoryText);
					}
					btnEdit.setEnabled(false);
					btnWrite.setEnabled(true);
				}
			}
		});
		
		btnCopy = new Button(grpProgramMemory, SWT.NONE);
		btnCopy.setLayoutData(new RowData(70, SWT.DEFAULT));
		btnCopy.setText(Messages.getString(MessageIds.GDE_MSGT2626));		 //$NON-NLS-1$
		btnCopy.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2627)); //$NON-NLS-1$
		btnCopy.setEnabled(false);
		btnCopy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				combo.setForeground(application.COLOR_RED);
				copiedProgramMemory = new ChargerMemory(selectedProgramMemory, isDuo);
				btnAdd.setEnabled(true);
			}
		});
		
		btnAdd = new Button(grpProgramMemory, SWT.NONE);
		btnAdd.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2628)); //$NON-NLS-1$
		btnAdd.setLayoutData(new RowData(70, SWT.DEFAULT));
		btnAdd.setText(Messages.getString(MessageIds.GDE_MSGT2629)); //$NON-NLS-1$
		btnAdd.setEnabled(false);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (copiedProgramMemory != null) {
					String batteryType = new String(copiedProgramMemory.getName()).trim();
					copiedProgramMemory.setName(batteryType + Messages.getString(MessageIds.GDE_MSGT2620)); //$NON-NLS-1$
					short newIndex = addEntryMemoryHead(batteryType);
					writeProgramMemory(newIndex, copiedProgramMemory);
					copiedProgramMemory = null;
				}
				combo.setForeground(application.COLOR_BLACK);
				combo.setItems(getProgramMemories());
				combo.select(0);
				combo.notifyListeners(SWT.Selection, new Event());				
				btnAdd.setEnabled(false);
			}
		});
		
		btnEdit = new Button(grpProgramMemory, SWT.NONE);
		btnEdit.setLayoutData(new RowData(70, SWT.DEFAULT));
		btnEdit.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2630)); //$NON-NLS-1$
		btnEdit.setText(Messages.getString(MessageIds.GDE_MSGT2631)); //$NON-NLS-1$
		btnEdit.setEnabled(false);
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				combo.setEditable(true);
			}
		});
		
		btnWrite = new Button(grpProgramMemory, SWT.NONE);
		btnWrite.setLayoutData(new RowData(70, SWT.DEFAULT));
		btnWrite.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2632)); //$NON-NLS-1$
		btnWrite.setText(Messages.getString(MessageIds.GDE_MSGT2633)); //$NON-NLS-1$
		btnWrite.setEnabled(false);
		btnWrite.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (selectedProgramMemory != null)
					writeProgramMemory(lastSelectedProgramMemoryIndex, selectedProgramMemory);
				combo.setForeground(application.COLOR_BLACK);
				combo.setItems(getProgramMemories());
				combo.select(0);
				combo.notifyListeners(SWT.Selection, new Event());				
				btnWrite.setEnabled(false);
			}
		});
		
		btnDelete = new Button(grpProgramMemory, SWT.NONE);
		btnDelete.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2634)); //$NON-NLS-1$
		btnDelete.setLayoutData(new RowData(70, SWT.DEFAULT));
		btnDelete.setText(Messages.getString(MessageIds.GDE_MSGT2635)); //$NON-NLS-1$
		btnDelete.setEnabled(false);
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeMemoryHead((byte) lastSelectedProgramMemoryIndex);
				combo.setForeground(application.COLOR_BLACK);
				combo.setItems(getProgramMemories());
				combo.select(0);
				combo.notifyListeners(SWT.Selection, new Event());				
			}
		});
		
		if(selectedProgramMemory != null)
			this.memoryValues = selectedProgramMemory.getMemoryValues(this.memoryValues, isDuo);
		
		Composite memoryComposite = new Composite(mainComposite, SWT.BORDER);
		memoryComposite.setLayout(new FillLayout(SWT.VERTICAL));
		FormData fd_composite_1 = new FormData();
		fd_composite_1.top = new FormAttachment(0, 80);
		fd_composite_1.bottom = new FormAttachment(0, 190);
		fd_composite_1.right = new FormAttachment(100, -10);
		fd_composite_1.left = new FormAttachment(0, 10);
		memoryComposite.setLayoutData(fd_composite_1);
		memoryComposite.setBackground(this.application.COLOR_CANVAS_YELLOW);
		//battery type
		this.memoryParameters[0] = new ParameterConfigControl(memoryComposite, this.memoryValues, 0, 
				Messages.getString(MessageIds.GDE_MSGT2636), 175, //$NON-NLS-1$
				this.cellTypeNames, 280, 
				this.cellTypeNamesArray, 50, 150);
		this.memoryParameters[0].getSlider().setEnabled(false);
		//number cells
		int maxNumberCells = ((iChargerUsb)this.device).getNumberOfLithiumCells();
		this.memoryParameters[1] = new ParameterConfigControl(memoryComposite, this.memoryValues, 1, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2637), 175, //$NON-NLS-1$
				"0(auto) - " + maxNumberCells, 280,  //$NON-NLS-1$
				false, 50, 150, 0, maxNumberCells); //$NON-NLS-1$
		//battery capacity
		this.memoryParameters[2] = new ParameterConfigControl(memoryComposite, this.memoryValues, 2, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2638), 175, //$NON-NLS-1$
				"0(auto) ~ 65000 mAh", 280,  //$NON-NLS-1$
				true, 50, 150, 0, 65000, 0, true); //$NON-NLS-1$
		memoryComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
	
		tabFolder = new CTabFolder(mainComposite, SWT.NONE);
		FormData fd_tabFolder = new FormData();
		fd_tabFolder.top = new FormAttachment(memoryComposite, 6);
		fd_tabFolder.left = new FormAttachment(0, 10);
		fd_tabFolder.right = new FormAttachment(100, -10);
		tabFolder.setLayoutData(fd_tabFolder);
		tabFolder.setSimple(false);
		
		CTabItem tbtmCharge = new CTabItem(tabFolder, SWT.NONE);
		tbtmCharge.setText(Messages.getString(MessageIds.GDE_MSGT2639));	 //$NON-NLS-1$
		ScrolledComposite scrolledComposite = new ScrolledComposite(tabFolder, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		tbtmCharge.setControl(scrolledComposite);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setLayout(new FillLayout(SWT.VERTICAL));
		chargeComposite = new Composite(scrolledComposite, SWT.NONE);
		chargeComposite.setLayout(new RowLayout(SWT.HORIZONTAL));
		scrolledComposite.setContent(chargeComposite);
		scrolledComposite.setMinSize(chargeComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		chargeComposite.setSize(tbtmCharge.getBounds().width, 880);
		//charge parameter current
		this.memoryParameters[3] = new ParameterConfigControl(chargeComposite, this.memoryValues, 3, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2640), 175, //$NON-NLS-1$
				"100 ~ 20000 mA", 280,  //$NON-NLS-1$
				true, 50, 150, 100, 20000, -100, false); //$NON-NLS-1$
		//charge parameter modus normal,balance,external,reflex 
		this.memoryParameters[4] = new ParameterConfigControl(chargeComposite, this.memoryValues, 4, 
				Messages.getString(MessageIds.GDE_MSGT2641), 175, //$NON-NLS-1$
				String.join(", ", ChargerMemory.LiMode.VALUES), 280,  //$NON-NLS-1$
				ChargerMemory.LiMode.VALUES, 50, 150);
		//charge parameter modus normal,balance,external,reflex 
		this.memoryParameters[5] = new ParameterConfigControl(chargeComposite, this.memoryValues, 5, 
				Messages.getString(MessageIds.GDE_MSGT2642), 175, //$NON-NLS-1$
				String.join(", ", ChargerMemory.BalancerLiSetup.VALUES), 280,  //$NON-NLS-1$
				ChargerMemory.BalancerLiSetup.VALUES, 50, 150);
		//charge parameter end current
		this.memoryParameters[6] = new ParameterConfigControl(chargeComposite, this.memoryValues, 6, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2643), 175, //$NON-NLS-1$
				"10% - 50%", 280,  //$NON-NLS-1$
				true, 50, 150, 10, 50, -10, false); //$NON-NLS-1$
		if (this.memoryValues[5] == 0) this.memoryParameters[6].setEnabled(false);
		//charge parameter cell voltage
		this.memoryParameters[7] = new ParameterConfigControl(chargeComposite, this.memoryValues, 7, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2644), 175, //$NON-NLS-1$
				"4000 mV - 4200 mV", 280,  //$NON-NLS-1$
				true, 50, 150, 4000, 4200, -4000, false); //$NON-NLS-1$

		//Ni charge voltage drop
		this.memoryParameters[26] = new ParameterConfigControl(chargeComposite, this.memoryValues, 26, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2645), 175, //$NON-NLS-1$
				"1 mV - 20 mV", 280,  //$NON-NLS-1$
				true, 50, 150, 1, 20, -1, false); //$NON-NLS-1$
		//Ni charge voltage drop delay
		this.memoryParameters[27] = new ParameterConfigControl(chargeComposite, this.memoryValues, 27, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2646), 175, //$NON-NLS-1$
				"0 Min - 20 Min", 280,  //$NON-NLS-1$
				true, 50, 150, 0, 20, 0, false); //$NON-NLS-1$
		//Ni charge allow 0V 
		this.memoryParameters[28] = new ParameterConfigControl(chargeComposite, this.memoryValues, 28, 
				Messages.getString(MessageIds.GDE_MSGT2647), 175, //$NON-NLS-1$
				Messages.getString(MessageIds.GDE_MSGT2648), 280,  //$NON-NLS-1$
				new String[]{Messages.getString(MessageIds.GDE_MSGT2649), Messages.getString(MessageIds.GDE_MSGT2650)}, 50, 150); //$NON-NLS-1$ //$NON-NLS-2$
		//Ni trickle charge enable 
		this.memoryParameters[29] = new ParameterConfigControl(chargeComposite, this.memoryValues, 29, 
				Messages.getString(MessageIds.GDE_MSGT2651), 175, //$NON-NLS-1$
				"off, on", 280,  //$NON-NLS-1$
				new String[]{"off", "on"}, 50, 150); //$NON-NLS-1$ //$NON-NLS-2$
		//Ni charge trickle current
		this.memoryParameters[30] = new ParameterConfigControl(chargeComposite, this.memoryValues, 30, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2652), 175, //$NON-NLS-1$
				"20mA - 1000mA", 280,  //$NON-NLS-1$
				true, 50, 150, 20, 1000, -20, false); //$NON-NLS-1$
		//Ni charge trickle timeout
		this.memoryParameters[31] = new ParameterConfigControl(chargeComposite, this.memoryValues, 31, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2653), 175, //$NON-NLS-1$
				"1 Min - 999 Min", 280,  //$NON-NLS-1$
				true, 50, 150, 1, 999, -1, false); //$NON-NLS-1$
		chargeComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
		
		grpChargeSaftySettings = new Group(chargeComposite, SWT.NONE);
		grpChargeSaftySettings.setText(Messages.getString(MessageIds.GDE_MSGT2654)); //$NON-NLS-1$
		grpChargeSaftySettings.setBackground(application.COLOR_WHITE);
		grpChargeSaftySettings.setLayout(new RowLayout(SWT.VERTICAL));
		//charge parameter balancer
		this.memoryParameters[8] = new ParameterConfigControl(grpChargeSaftySettings, this.memoryValues, 8, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2655), 175, //$NON-NLS-1$
				"20°C - 80°C", 280,  //$NON-NLS-1$
				true, 50, 150, 20, 80, -20, false); //$NON-NLS-1$
		//charge parameter balancer start
		this.memoryParameters[9] = new ParameterConfigControl(grpChargeSaftySettings, this.memoryValues, 9, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2656), 175, //$NON-NLS-1$
				"50% - 200%", 280,  //$NON-NLS-1$
				true, 50, 150, 50, 200, -50, false); //$NON-NLS-1$
		//charge parameter balancer difference
		this.memoryParameters[10] = new ParameterConfigControl(grpChargeSaftySettings, this.memoryValues, 10, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2657), 175, //$NON-NLS-1$
				"0(OFF) - 9999 Min", 280,  //$NON-NLS-1$
				true, 50, 150, 0, 9999, 0, false); //$NON-NLS-1$
		grpChargeSaftySettings.addListener(SWT.Selection, this.memoryParameterChangeListener);
		
		grpBalancerSettings = new Group(chargeComposite, SWT.NONE);
		grpBalancerSettings.setText(Messages.getString(MessageIds.GDE_MSGT2658)); //$NON-NLS-1$
		grpBalancerSettings.setLayout(new RowLayout(SWT.VERTICAL));
		//charge parameter balancer
		this.memoryParameters[11] = new ParameterConfigControl(grpBalancerSettings, this.memoryValues, 11, 
				Messages.getString(MessageIds.GDE_MSGT2659), 175, //$NON-NLS-1$
				String.join(", ", ChargerMemory.BalancerSpeed.VALUES), 280,  //$NON-NLS-1$
				ChargerMemory.BalancerSpeed.VALUES, 50, 150);
		//charge parameter balancer start
		this.memoryParameters[12] = new ParameterConfigControl(grpBalancerSettings, this.memoryValues, 12, 
				Messages.getString(MessageIds.GDE_MSGT2660), 175, //$NON-NLS-1$
				String.join(", ", ChargerMemory.BalancerStart.VALUES), 280,  //$NON-NLS-1$
				ChargerMemory.BalancerStart.VALUES, 50, 150);
		//charge parameter balancer difference
		this.memoryParameters[13] = new ParameterConfigControl(grpBalancerSettings, this.memoryValues, 13, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2661), 175, //$NON-NLS-1$
				"1 mV - 10 mV", 280,  //$NON-NLS-1$
				true, 50, 150, 1, 10, -1, false); //$NON-NLS-1$
		//charge parameter balancer target
		this.memoryParameters[14] = new ParameterConfigControl(grpBalancerSettings, this.memoryValues, 14, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2662), 175, //$NON-NLS-1$
				"1 mV - 50 mV", 280,  //$NON-NLS-1$
				true, 50, 150, 1, 50, -1, false); //$NON-NLS-1$
		//charge parameter balancer over charge
		this.memoryParameters[15] = new ParameterConfigControl(grpBalancerSettings, this.memoryValues, 15, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2663), 175, //$NON-NLS-1$
				"0 mV - 50 mV", 280,  //$NON-NLS-1$
				true, 50, 150, 0, 50, 0, false); //$NON-NLS-1$
		//charge parameter balancer delay
		this.memoryParameters[16] = new ParameterConfigControl(grpBalancerSettings, this.memoryValues, 16, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2664), 175, //$NON-NLS-1$
				"0 Min - 20 Min", 280,  //$NON-NLS-1$
				true, 50, 150, 0, 20, 0, false); //$NON-NLS-1$
		grpBalancerSettings.addListener(SWT.Selection, this.memoryParameterChangeListener);
		
		//Li, NiZn, Pb only (NiMH, NiCd has charge at 0V enablement)
		grpAdvancedRestoreSettings = new Group(chargeComposite, SWT.NONE);
		grpAdvancedRestoreSettings.setText(Messages.getString(MessageIds.GDE_MSGT2665)); //$NON-NLS-1$
		grpAdvancedRestoreSettings.setLayout(new RowLayout(SWT.VERTICAL));
		//charge restore lowest voltage
		this.memoryParameters[32] = new ParameterConfigControl(grpAdvancedRestoreSettings, this.memoryValues, 32, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2666), 175, //$NON-NLS-1$
				"500 mV - 2500 mV", 280,  //$NON-NLS-1$
				true, 50, 150, 500, 2500, -500, false); //$NON-NLS-1$
		//charge restore charge time
		this.memoryParameters[33] = new ParameterConfigControl(grpAdvancedRestoreSettings, this.memoryValues, 33, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2667), 175, //$NON-NLS-1$
				"1 Min - 5 Min", 280,  //$NON-NLS-1$
				true, 50, 150, 1, 5, -1, false); //$NON-NLS-1$
		//charge restore charge time
		this.memoryParameters[34] = new ParameterConfigControl(grpAdvancedRestoreSettings, this.memoryValues, 34, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2668), 175, //$NON-NLS-1$
				"20 mA - 500 mA", 280,  //$NON-NLS-1$
				true, 50, 150, 20, 500, -20, false); //$NON-NLS-1$
		//charge keep charging after done
		this.memoryParameters[35] = new ParameterConfigControl(grpAdvancedRestoreSettings, this.memoryValues, 35, 
				Messages.getString(MessageIds.GDE_MSGT2669), 175, //$NON-NLS-1$
				"off, on", 280,  //$NON-NLS-1$
				new String[]{"off", "on"}, 50, 150); //$NON-NLS-1$ //$NON-NLS-2$

		grpAdvancedRestoreSettings.addListener(SWT.Selection, this.memoryParameterChangeListener);		
		chargeComposite.layout();
		
		
		CTabItem tbtmDischarge = new CTabItem(tabFolder, SWT.NONE);
		tbtmDischarge.setText(Messages.getString(MessageIds.GDE_MSGT2670));	 //$NON-NLS-1$
		dischargeComposite = new Composite(tabFolder, SWT.NONE);
		tbtmDischarge.setControl(dischargeComposite);
		dischargeComposite.setLayout(new RowLayout(SWT.VERTICAL));
		//discharge parameter current
		this.memoryParameters[17] = new ParameterConfigControl(dischargeComposite, this.memoryValues, 17, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2671), 175, //$NON-NLS-1$
				"50 ~ 30000 mA", 280,  //$NON-NLS-1$
				true, 50, 150, 50, 30000, -50, false); //$NON-NLS-1$
		//discharge parameter cell voltage
		this.memoryParameters[18] = new ParameterConfigControl(dischargeComposite, this.memoryValues, 18, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2672), 175, //$NON-NLS-1$
				"3000mV - 4100mV", 280,  //$NON-NLS-1$
				true, 50, 150, 3000, 4100, -3000, false); //$NON-NLS-1$
		//discharge end current
		this.memoryParameters[19] = new ParameterConfigControl(dischargeComposite, this.memoryValues, 19, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2673), 175, //$NON-NLS-1$
				"1% - 100%", 280,  //$NON-NLS-1$
				true, 50, 150, 1, 100, -1, false); //$NON-NLS-1$
		//20 regenerative mode 
		this.memoryParameters[20] = new ParameterConfigControl(dischargeComposite, this.memoryValues, 20, 
				Messages.getString(MessageIds.GDE_MSGT2674), 175, //$NON-NLS-1$
				"off, on", 280,  //$NON-NLS-1$
				new String[]{"off", "on"}, 50, 150); //$NON-NLS-1$ //$NON-NLS-2$
		//discharge extra
		this.memoryParameters[21] = new ParameterConfigControl(dischargeComposite, this.memoryValues, 21, 
				Messages.getString(MessageIds.GDE_MSGT2675), 175, //$NON-NLS-1$
				"off, on", 280,  //$NON-NLS-1$
				new String[]{"off", "on"}, 50, 150); //$NON-NLS-1$ //$NON-NLS-2$
		//discharge balancer
		this.memoryParameters[22] = new ParameterConfigControl(dischargeComposite, this.memoryValues, 22,
				Messages.getString(MessageIds.GDE_MSGT2676), 175, //$NON-NLS-1$
				"off, on", 280,  //$NON-NLS-1$
				new String[]{"off", "on"}, 50, 150); //$NON-NLS-1$ //$NON-NLS-2$
		dischargeComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
		
		grpDischargeSaftySettings = new Group(dischargeComposite, SWT.NONE);
		grpDischargeSaftySettings.setText(Messages.getString(MessageIds.GDE_MSGT2654)); //$NON-NLS-1$
		grpDischargeSaftySettings.setBackground(application.COLOR_WHITE);
		grpDischargeSaftySettings.setLayout(new RowLayout(SWT.VERTICAL));
		//discharge parameter cut temperature
		this.memoryParameters[23] = new ParameterConfigControl(grpDischargeSaftySettings, this.memoryValues, 23, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2655), 175, //$NON-NLS-1$
				"20°C - 80°C", 280,  //$NON-NLS-1$
				true, 50, 150, 20, 80, -20, false); //$NON-NLS-1$
		//discharge parameter max discharge capacity
		this.memoryParameters[24] = new ParameterConfigControl(grpDischargeSaftySettings, this.memoryValues, 24, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2656), 175, //$NON-NLS-1$
				"50% - 200%", 280,  //$NON-NLS-1$
				true, 50, 150, 50, 200, -50, false); //$NON-NLS-1$
		//discharge parameter safety timer
		this.memoryParameters[25] = new ParameterConfigControl(grpDischargeSaftySettings, this.memoryValues, 25, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2657), 175, //$NON-NLS-1$
				"0(OFF) - 9999 Min", 280,  //$NON-NLS-1$
				true, 50, 150, 0, 9999, 0, false); //$NON-NLS-1$
		grpDischargeSaftySettings.addListener(SWT.Selection, this.memoryParameterChangeListener);
		
		createStorageTabItem(tabFolder);
		
		CTabItem tbtmCycle = new CTabItem(tabFolder, SWT.NONE);
		tbtmCycle.setText(Messages.getString(MessageIds.GDE_MSGT2681));	 //$NON-NLS-1$
		cycleComposite = new Composite(tabFolder, SWT.NONE);
		tbtmCycle.setControl(cycleComposite);
		cycleComposite.setLayout(new RowLayout(SWT.VERTICAL));
		//cycle parameter mode CHG->DCHG
		this.memoryParameters[39] = new ParameterConfigControl(cycleComposite, this.memoryValues, 39,
				Messages.getString(MessageIds.GDE_MSGT2682), 175, //$NON-NLS-1$
				String.join(", ", ChargerMemory.CycleMode.VALUES), 280,  //$NON-NLS-1$
				ChargerMemory.CycleMode.VALUES, 50, 150);
		//cycle count
		this.memoryParameters[40] = new ParameterConfigControl(cycleComposite, this.memoryValues, 40, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2683), 175, //$NON-NLS-1$
				"1 - 99", 280,  //$NON-NLS-1$
				true, 50, 150, 1, 99, -1, false); //$NON-NLS-1$
		//cycle delay
		this.memoryParameters[41] = new ParameterConfigControl(cycleComposite, this.memoryValues, 41, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2684), 175, //$NON-NLS-1$
				"1 Min - 999 Min", 280,  //$NON-NLS-1$
				true, 50, 150, 1, 999, -1, false); //$NON-NLS-1$
		cycleComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
	
		tabFolder.setSelection(0);
		updateMemoryParameterControls();
	

		Group grpRunProgram = new Group(mainComposite, SWT.NONE);
		fd_tabFolder.bottom = new FormAttachment(grpRunProgram, -6);
		grpRunProgram.setText(Messages.getString(MessageIds.GDE_MSGT2685)); //$NON-NLS-1$
		grpRunProgram.setLayout(new FillLayout(SWT.HORIZONTAL));
		FormData fd_grpRunProgram = new FormData();
		fd_grpRunProgram.left = new FormAttachment(0, 10);
		fd_grpRunProgram.right = new FormAttachment(100, -180);
		fd_grpRunProgram.top = new FormAttachment(100, -75);
		fd_grpRunProgram.bottom = new FormAttachment(100, -20);
		grpRunProgram.setLayoutData(fd_grpRunProgram);
		grpRunProgram.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2686)); //$NON-NLS-1$

		
		btnCharge = new Button(grpRunProgram, SWT.NONE);
		btnCharge.setText(Messages.getString(MessageIds.GDE_MSGT2687)); //$NON-NLS-1$
		btnCharge.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				startProgramExecution((byte)0, (byte)(application.getActiveChannelNumber() - 1), (byte)memoryHeadIndex[combo.getSelectionIndex()]);
				device.open_closeCommPort();
				btnCharge.setEnabled(false);
				btnStorage.setEnabled(false);
				btnDischarge.setEnabled(false);
				btnCycle.setEnabled(false);
				btnBalance.setEnabled(false);
				btnStop.setEnabled(true);
				grpProgramMemory.setEnabled(false);
			}
		});
		
		btnStorage = new Button(grpRunProgram, SWT.NONE);
		btnStorage.setText(Messages.getString(MessageIds.GDE_MSGT2688)); //$NON-NLS-1$
		btnStorage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				startProgramExecution((byte)1, (byte)(application.getActiveChannelNumber() - 1), (byte)memoryHeadIndex[combo.getSelectionIndex()]);
				device.open_closeCommPort();
				btnCharge.setEnabled(false);
				btnStorage.setEnabled(false);
				btnDischarge.setEnabled(false);
				btnCycle.setEnabled(false);
				btnBalance.setEnabled(false);
				btnStop.setEnabled(true);
				grpProgramMemory.setEnabled(false);
			}
		});
		
		btnDischarge = new Button(grpRunProgram, SWT.NONE);
		btnDischarge.setText(Messages.getString(MessageIds.GDE_MSGT2689)); //$NON-NLS-1$
		btnDischarge.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				startProgramExecution((byte)2, (byte)(application.getActiveChannelNumber() - 1), (byte)memoryHeadIndex[combo.getSelectionIndex()]);
				device.open_closeCommPort();
				btnCharge.setEnabled(false);
				btnStorage.setEnabled(false);
				btnDischarge.setEnabled(false);
				btnCycle.setEnabled(false);
				btnBalance.setEnabled(false);
				btnStop.setEnabled(true);
				grpProgramMemory.setEnabled(false);
			}
		});
		
		btnCycle = new Button(grpRunProgram, SWT.NONE);
		btnCycle.setText(Messages.getString(MessageIds.GDE_MSGT2690)); //$NON-NLS-1$
		btnCycle.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				startProgramExecution((byte)3, (byte)(application.getActiveChannelNumber() - 1), (byte)memoryHeadIndex[combo.getSelectionIndex()]);
				device.open_closeCommPort();
				btnCharge.setEnabled(false);
				btnStorage.setEnabled(false);
				btnDischarge.setEnabled(false);
				btnCycle.setEnabled(false);
				btnBalance.setEnabled(false);
				btnStop.setEnabled(true);
				grpProgramMemory.setEnabled(false);
			}
		});
		
		btnBalance = new Button(grpRunProgram, SWT.NONE);
		btnBalance.setText(Messages.getString(MessageIds.GDE_MSGT2691)); //$NON-NLS-1$
		btnBalance.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				startProgramExecution((byte)4, (byte)(application.getActiveChannelNumber() - 1), (byte)memoryHeadIndex[combo.getSelectionIndex()]);
				device.open_closeCommPort();
				btnCharge.setEnabled(false);
				btnStorage.setEnabled(false);
				btnDischarge.setEnabled(false);
				btnCycle.setEnabled(false);
				btnBalance.setEnabled(false);
				btnStop.setEnabled(true);
				grpProgramMemory.setEnabled(false);
			}
		});
		
		btnStop = new Button(grpRunProgram, SWT.NONE);
		btnStop.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2692)); //$NON-NLS-1$
		btnStop.setText(Messages.getString(MessageIds.GDE_MSGT2693)); //$NON-NLS-1$
		btnStop.setEnabled(false);
		btnStop.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				stopProgramExecution((byte)(application.getActiveChannelNumber() - 1));
				device.open_closeCommPort();
				btnCharge.setEnabled(true);
				btnStorage.setEnabled(true);
				btnDischarge.setEnabled(true);
				btnCycle.setEnabled(true);
				btnBalance.setEnabled(true);
				grpProgramMemory.setEnabled(true);
			}
		});
		
		Button btnClose = new Button(mainComposite, SWT.NONE);
		FormData fd_btnCancel = new FormData();
		fd_btnCancel.bottom = new FormAttachment(100, -25);
		fd_btnCancel.right = new FormAttachment(100, -10);
		fd_btnCancel.left = new FormAttachment(100, -150);
		btnClose.setLayoutData(fd_btnCancel);
		btnClose.setText(Messages.getString(MessageIds.GDE_MSGT2694)); //$NON-NLS-1$
		btnClose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				log.log(Level.FINEST, "clusterCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
				dialogShell.dispose();
			}
		}); 
	}

	private void createStorageTabItem(CTabFolder tabFolder) {
		tbtmStorage = new CTabItem(tabFolder, SWT.NONE);
		tbtmStorage.setText(Messages.getString(MessageIds.GDE_MSGT2695));	 //$NON-NLS-1$
		storageComposite = new Composite(tabFolder, SWT.NONE);
		tbtmStorage.setControl(storageComposite);
		storageComposite.setLayout(new RowLayout(SWT.VERTICAL));
		//storage parameter cell voltage (Li battery type only)
		this.memoryParameters[36] = new ParameterConfigControl(storageComposite, this.memoryValues, 36, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2696), 175, //$NON-NLS-1$
				"3700mV - 3900mV", 280,  //$NON-NLS-1$
				true, 50, 150, 3700, 3900, -3700, false); //$NON-NLS-1$
		//storage parameter compensation
		this.memoryParameters[37] = new ParameterConfigControl(storageComposite, this.memoryValues, 37, GDE.STRING_EMPTY, 
				Messages.getString(MessageIds.GDE_MSGT2697), 175, //$NON-NLS-1$
				"0mV - 200mV", 280,  //$NON-NLS-1$
				true, 50, 150, 0, 200, 0, false); //$NON-NLS-1$
		//storage acceleration
		this.memoryParameters[38] = new ParameterConfigControl(storageComposite, this.memoryValues, 38,
				Messages.getString(MessageIds.GDE_MSGT2698), 175, //$NON-NLS-1$
				"off, on", 280,  //$NON-NLS-1$
				new String[]{"off", "on"}, 50, 150); //$NON-NLS-1$ //$NON-NLS-2$
		storageComposite.addListener(SWT.Selection, this.memoryParameterChangeListener);
	}

	private void updateMemoryParameterControls() {
		
		if ((isDuo && this.memoryValues[0] < 8) || this.memoryValues[0] < 9) { // X devices up to Power
			int maxNumberCells = this.cellTypeNamesArray[this.memoryValues[0]].startsWith("L") ? ((iChargerUsb) this.device).getNumberOfLithiumCells() //$NON-NLS-1$
					: this.cellTypeNamesArray[this.memoryValues[0]].startsWith("N") ? ChargerMemory.getMaxCellsNi(this.device) //$NON-NLS-1$
							: this.cellTypeNamesArray[this.memoryValues[0]].startsWith("P") ? ChargerMemory.getMaxCellsPb(this.device) : 0; //$NON-NLS-1$
			this.memoryParameters[1].updateValueRange("autom. - " + maxNumberCells, 0, maxNumberCells, 0); //$NON-NLS-1$
			//battery type dependent updates
			this.memoryParameters[5].setEnabled(true);
			this.memoryParameters[6].setEnabled(this.memoryValues[5] != 0);
			this.memoryParameters[7].setEnabled(true);
			this.memoryParameters[7].getSlider().setEnabled(true);
			grpBalancerSettings.setEnabled(true);
			for (int i = 11; i < 17; ++i)
				this.memoryParameters[i].setEnabled(true);
			if (isDuo) {
				switch (this.memoryValues[0]) { //battery type LiPo,LiLo,LiFe,NiMH,Nicd,Pb,NiZn,LiHV
				case 0: //LiPo
					this.memoryParameters[7].updateValueRange("3850 - 4200 mV", 3850, 4200, -3850); //$NON-NLS-1$
					break;
				case 1: //LiIo
					this.memoryParameters[7].updateValueRange("3750 - 4100 mV", 3750, 4100, -3750); //$NON-NLS-1$
					break;
				case 2: //LiFe
					this.memoryParameters[7].updateValueRange("3300 - 3600 mV", 3300, 3600, -3300); //$NON-NLS-1$
					break;
				case 6: //NiZn
					this.memoryParameters[7].updateValueRange("1200 - 2000 mV", 1200, 2000, -1200); //$NON-NLS-1$
					break;
				case 7: //LiHV
					this.memoryParameters[7].updateValueRange("3900 - 4350 mV", 3900, 4350, -3900); //$NON-NLS-1$
					break;
				case 3: //NiMH
				case 4: //NiCd
					this.memoryParameters[4].updateTextFieldValues(ChargerMemory.NiMode.VALUES);
					this.memoryParameters[5].setEnabled(false);
					this.memoryParameters[7].setEnabled(false);
					grpBalancerSettings.setEnabled(false);
					for (int i = 8; i < 14; ++i)
						this.memoryParameters[i].setEnabled(false);
				case 5: //Pb
					this.memoryParameters[4].updateTextFieldValues(ChargerMemory.PbMode.VALUES);
					this.memoryParameters[5].setEnabled(false);
					this.memoryParameters[7].setEnabled(false);
					grpBalancerSettings.setEnabled(false);
					for (int i = 8; i < 14; ++i)
						this.memoryParameters[i].setEnabled(false);
				default: //unknown
					this.memoryParameters[7].setEnabled(false);
					grpBalancerSettings.setEnabled(false);
					break;
				}
			}
			else {
				switch (this.memoryValues[0]) { //battery type LiPo,LiLo,LiFe,LiHV,LTO, NiMH,Nicd,NiZn,Pb
				case 0: //LiPo
					this.memoryParameters[7].updateValueRange("3850 - 4200 mV", 3850, 4200, -3850); //$NON-NLS-1$
					break;
				case 1: //LiIo
					this.memoryParameters[7].updateValueRange("3750 - 4100 mV", 3750, 4100, -3750); //$NON-NLS-1$
					break;
				case 2: //LiFe
					this.memoryParameters[7].updateValueRange("3300 - 3600 mV", 3300, 3600, -3300); //$NON-NLS-1$
					break;
				case 3: //LiHV
					this.memoryParameters[7].updateValueRange("3900 - 4350 mV", 3900, 4350, -3900); //$NON-NLS-1$
					break;
				case 4: //LTO
					this.memoryParameters[7].updateValueRange("2400 - 3100 mV", 2400, 3100, -2400); //$NON-NLS-1$
					break;
				case 7: //NiZn
					this.memoryParameters[7].updateValueRange("1200 - 2000 mV", 1200, 2000, -1200); //$NON-NLS-1$
					break;
				case 5: //NiMH
				case 6: //NiCd
					this.memoryParameters[4].updateTextFieldValues(ChargerMemory.NiMode.VALUES);
					this.memoryParameters[5].setEnabled(false);
					this.memoryParameters[7].setEnabled(false);
					grpBalancerSettings.setEnabled(false);
					for (int i = 11; i < 17; ++i)
						this.memoryParameters[i].setEnabled(false);
					break;
				case 8: //Pb
					this.memoryParameters[4].updateTextFieldValues(ChargerMemory.PbMode.VALUES);
					this.memoryParameters[5].setEnabled(false);
					this.memoryParameters[7].setEnabled(false);
					grpBalancerSettings.setEnabled(false);
					for (int i = 11; i < 17; ++i)
						this.memoryParameters[i].setEnabled(false);
					break;
				default: //Power
					this.memoryParameters[7].setEnabled(false);
					grpBalancerSettings.setEnabled(false);
					for (int i = 11; i < 17; ++i)
						this.memoryParameters[i].setEnabled(false);
				}
			}
			this.memoryParameters[13].setEnabled(this.memoryValues[11] == 3);
			this.memoryParameters[14].setEnabled(this.memoryValues[11] == 3);
			this.memoryParameters[15].setEnabled(this.memoryValues[11] == 3);
			this.memoryParameters[16].setEnabled(this.memoryValues[11] == 3);

			//18 discharge parameter cell voltage is battery type dependent
			//21 discharge extra is battery type dependent
			//22 discharge balancer is battery type dependent
			this.memoryParameters[18].updateNameLabel(Messages.getString(MessageIds.GDE_MSGT2672)); //$NON-NLS-1$
			if (isDuo) {
				switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
				case 0: //LiPo
					this.memoryParameters[18].updateValueRange("3000 - 4100 mV", 3000, 4100, -3000); //$NON-NLS-1$
					this.memoryParameters[21].setEnabled(true);
					this.memoryParameters[22].setEnabled(true);
					break;
				case 1: //LiIo
					this.memoryParameters[18].updateValueRange("2500 - 4000 mV", 2500, 4000, -2500); //$NON-NLS-1$
					this.memoryParameters[21].setEnabled(true);
					this.memoryParameters[22].setEnabled(true);
					break;
				case 2: //LiFe
					this.memoryParameters[18].updateValueRange("2000 - 3500 mV", 2000, 3500, -2000); //$NON-NLS-1$
					this.memoryParameters[21].setEnabled(true);
					this.memoryParameters[22].setEnabled(true);
					break;
				case 3: //NiMH
				case 4: //NiCd
					this.memoryParameters[18].updateNameLabel(Messages.getString(MessageIds.GDE_MSGT2619)); //$NON-NLS-1$
					this.memoryParameters[18].updateValueRange("100 - 35000 mV", 100, 35000, -100); //$NON-NLS-1$
					this.memoryParameters[21].setEnabled(false);
					this.memoryParameters[22].setEnabled(false);
					break;
				case 5: //Pb
					this.memoryParameters[18].updateValueRange("1500 - 2400 mV", 1500, 2400, -1500); //$NON-NLS-1$
					this.memoryParameters[21].setEnabled(false);
					this.memoryParameters[22].setEnabled(false);
					break;
				case 6: //NiZn
					this.memoryParameters[18].updateValueRange("900 - 1600 mV", 900, 1600, -900); //$NON-NLS-1$
					this.memoryParameters[21].setEnabled(true);
					this.memoryParameters[22].setEnabled(true);
					break;
				case 7: //LiHV
					this.memoryParameters[18].updateValueRange("3000 - 4250 mV", 3000, 4250, -3000); //$NON-NLS-1$
					this.memoryParameters[21].setEnabled(true);
					this.memoryParameters[22].setEnabled(true);
					break;
				default: 
					break;
				}
			}
			else {
				switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
				case 0: //LiPo
					this.memoryParameters[18].updateValueRange("3000 - 4100 mV", 3000, 4100, -3000); //$NON-NLS-1$
					this.memoryParameters[21].setEnabled(true);
					this.memoryParameters[22].setEnabled(true);
					break;
				case 1: //LiIo
					this.memoryParameters[18].updateValueRange("2500 - 4000 mV", 2500, 4000, -2500); //$NON-NLS-1$
					this.memoryParameters[21].setEnabled(true);
					this.memoryParameters[22].setEnabled(true);
					break;
				case 2: //LiFe
					this.memoryParameters[18].updateValueRange("2000 - 3500 mV", 2000, 3500, -2000); //$NON-NLS-1$
					this.memoryParameters[21].setEnabled(true);
					this.memoryParameters[22].setEnabled(true);
					break;
				case 3: //LiHV
					this.memoryParameters[18].updateValueRange("3000 - 4250 mV", 3000, 4250, -3000); //$NON-NLS-1$
					this.memoryParameters[21].setEnabled(true);
					this.memoryParameters[22].setEnabled(true);
					break;
				case 4: //LTO
					this.memoryParameters[18].updateValueRange("1500 - 2900 mV", 1500, 2900, -1500); //$NON-NLS-1$
					this.memoryParameters[21].setEnabled(true);
					this.memoryParameters[22].setEnabled(true);
					break;
				case 5: //NiMH
				case 6: //NiCd
					this.memoryParameters[18].updateNameLabel(Messages.getString(MessageIds.GDE_MSGT2619)); //$NON-NLS-1$
					this.memoryParameters[18].updateValueRange("100 - 35000 mV", 100, 35000, -100); //$NON-NLS-1$
					this.memoryParameters[21].setEnabled(false);
					this.memoryParameters[22].setEnabled(false);
					break;
				case 7: //NiZn
					this.memoryParameters[18].updateValueRange("900 - 1600 mV", 900, 1600, -900); //$NON-NLS-1$
					this.memoryParameters[21].setEnabled(true);
					this.memoryParameters[22].setEnabled(true);
					break;
				case 8: //Pb
					this.memoryParameters[18].updateValueRange("1500 - 2400 mV", 1500, 2400, -1500); //$NON-NLS-1$
					this.memoryParameters[21].setEnabled(false);
					this.memoryParameters[22].setEnabled(false);
					break;
				default: 
					break;
				}
			}
			//23 discharge parameter cut temperature
			//24 discharge parameter max discharge capacity
			//25 discharge parameter safety timer

			//26 Ni charge voltage drop
			//27 Ni charge voltage drop delay
			//28 Ni charge allow 0V 
			//29 Ni trickle charge enable 
			//30 Ni charge trickle current
			//31 Ni charge trickle timeout
			if (isDuo) {
				switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
				case 0: //LiPo
				case 1: //LiIo
				case 2: //LiFe
				case 5: //Pb
				case 6: //NiZn
				case 7: //LiHV
				default: 
					for (int i=26; i < 32; ++i)
						this.memoryParameters[i].setEnabled(false);
					break;
				case 3: //NiMH
				case 4: //NiCd
					for (int i=26; i < 32; ++i)
						this.memoryParameters[i].setEnabled(true);
					break;
				}
			}
			else {
				switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
				case 0: //LiPo
				case 1: //LiIo
				case 2: //LiFe
				case 3: //LiHV
				case 4: //LTO
				case 7: //NiZn
				case 8: //Pb
				default: 
					for (int i=26; i < 32; ++i)
						this.memoryParameters[i].setEnabled(false);
					break;
				case 5: //NiMH
				case 6: //NiCd
					for (int i=26; i < 32; ++i)
						this.memoryParameters[i].setEnabled(true);
					break;
				}
			}

			//Li, NiZn, Pb only (NiMH, NiCd has charge at 0V enable)
			//32 charge restore lowest voltage
			//33 charge restore charge time
			//34 charge restore charge current
			//35 charge keep charging after done
			if (isDuo) {
				switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
				case 0: //LiPo
				case 1: //LiIo
				case 2: //LiFe
				case 5: //Pb
				case 6: //NiZn
				case 7: //LiHV
				default: 
					for (int i=32; i < 36; ++i)
						this.memoryParameters[i].setEnabled(true);
					break;
				case 3: //NiMH
				case 4: //NiCd
					for (int i=32; i < 36; ++i)
						this.memoryParameters[i].setEnabled(false);
					break;
				}
			}
			else {
				switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
				case 0: //LiPo
				case 1: //LiIo
				case 2: //LiFe
				case 3: //LiHV
				case 4: //LTO
				case 7: //NiZn
				case 8: //Pb
				default: 
					for (int i=32; i < 36; ++i)
						this.memoryParameters[i].setEnabled(true);
					break;
				case 5: //NiMH
				case 6: //NiCd
					for (int i=32; i < 36; ++i)
						this.memoryParameters[i].setEnabled(false);
					break;
				}
			}
			
			//36 storage parameter cell voltage (Li battery type only)
			//37 storage parameter compensation
			//38 storage acceleration
			if (isDuo) {
				switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
				case 0: //LiPo
				case 1: //LiIo
				case 2: //LiFe
				case 7: //LiHV
				default: 
					if (tbtmStorage == null || tbtmStorage.isDisposed()) {
						createStorageTabItem(tabFolder);						
					}
					switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe
					case 0: //LiPo
						this.memoryParameters[36].updateValueRange("3700 - 3900 mV", 3700, 3900, -3700); //$NON-NLS-1$
						break;
					case 1: //LiIo
						this.memoryParameters[36].updateValueRange("3600 - 3800 mV", 3600, 3800, -3600); //$NON-NLS-1$
						break;
					case 2: //LiFe
						this.memoryParameters[36].updateValueRange("3100 - 3400 mV", 3100, 3400, -3100); //$NON-NLS-1$
						break;
					case 7: //LiHV
						this.memoryParameters[36].updateValueRange("3750 - 4100 mV", 3750, 4100, -3750); //$NON-NLS-1$
						break;
					default: 
						break;
					}
					break;
				case 3: //NiMH
				case 4: //NiCd
				case 5: //Pb
				case 6: //NiZn
					if (tbtmStorage != null && !tbtmStorage.isDisposed()) {
						storageComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
						for (int i = 36; i < 39; ++i) 
							this.memoryParameters[i].dispose();
						storageComposite.dispose();
						tbtmStorage.dispose();
					}
					break;
				}
			}
			else {
				switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe,NiMH,Nicd,Pb
				case 0: //LiPo
				case 1: //LiIo
				case 2: //LiFe
				case 3: //LiHV
				case 4: //LTO
					if (tbtmStorage == null || tbtmStorage.isDisposed()) {
						createStorageTabItem(tabFolder);						
					}
					switch (this.memoryValues[0]) { //LiPo,LiLo,LiFe
					case 0: //LiPo
						this.memoryParameters[36].updateValueRange("3700 - 3900 mV", 3700, 3900, -3700); //$NON-NLS-1$
						break;
					case 1: //LiIo
						this.memoryParameters[36].updateValueRange("3600 - 3800 mV", 3600, 3800, -3600); //$NON-NLS-1$
						break;
					case 2: //LiFe
						this.memoryParameters[36].updateValueRange("3100 - 3400 mV", 3100, 3400, -3100); //$NON-NLS-1$
						break;
					case 3: //LiHV
						this.memoryParameters[36].updateValueRange("3750 - 4100 mV", 3750, 4100, -3750); //$NON-NLS-1$
						break;
					case 4: //LTO
						this.memoryParameters[36].updateValueRange("2200 - 2600 mV", 2200, 2600, -2200); //$NON-NLS-1$
						break;
					default: 
						break;
					}
					break;
				default: 
				case 5: //NiMH
				case 6: //NiCd
				case 7: //NiZn
				case 8: //Pb
					if (tbtmStorage != null && !tbtmStorage.isDisposed()) {
						storageComposite.removeListener(SWT.Selection, this.memoryParameterChangeListener);
						for (int i = 36; i < 39; ++i) 
							this.memoryParameters[i].dispose();
						storageComposite.dispose();
						tbtmStorage.dispose();
					}
					break;
				}
			}

			//
		
			//update parameter controls
			for (int i = 0; i < this.memoryParameters.length; i++) {
				if (this.memoryParameters[i] != null) {
					this.memoryParameters[i].setSliderSelection(this.memoryValues[i]);
				}
			} 
		}
	}
}
