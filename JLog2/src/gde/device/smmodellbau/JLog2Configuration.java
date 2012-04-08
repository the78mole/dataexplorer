package gde.device.smmodellbau;

import gde.GDE;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

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
public class JLog2Configuration extends org.eclipse.swt.widgets.Composite {
	final static Logger						log						= Logger.getLogger(JLog2Configuration.class.getName());

	{
		//Register as a resource user - SWTResourceManager will
		//handle the obtaining and disposing of resources
		SWTResourceManager.registerResourceUser(this);
	}

	private Group									mainConfigGroup;
	private CLabel								jlcUrlLabel;
	private Button								jlcDownloadButton;
	private Button								jlcForumButton;
	private CCombo								jlogConfigurationCombo;
	private Button								directRatioButton;
	private CLabel								logModeLabel;
	private CCombo								paTempMaxCombo;
	private CLabel								paTempMaxLabel;
	private CLabel								voltageLabel;
	private CLabel								mAhLabel;
	private CCombo								voltagebatteryAlarmMaxCombo2;
	private CCombo								voltagebatteryMinCombo1;
	private CLabel								uBatMinAlarmLabel;
	private CCombo								capacityAlarmCombo;
	private CLabel								capacityAlarmLabel;
	private Group									alarmGroup;
	private Button								extRpmButton;
	private Button								logStopButton;
	private Button								highPulsWidthButton;
	private Button								ext1smallerButton, ext2smallerButton, ext3smallerButton, ext4smallerButton, ext5smallerButton;
	private Button								sensorAdapterButton;
	private CCombo								telemetryCombo;
	private Button								telemetryButton;
	private Label									spacer;
	private Button								brushLessButton;
	private Button								motorButton;
	private CCombo								rpmSensorCombo;
	private Button								rpmSensorButton;
	private CCombo								tempSensorTypeCombo;
	private Button								tempSensorTypeButton;
	private CCombo								subDevicesCombo;
	private Button								subDevicesButton;
	private CCombo								line1signalCombo;
	private CLabel								Line1signalLabel;
	private CCombo								alarmLinesCombo;
	private Button								alarmLinesButton;
	private Group									optionalGroup;
	private Button								alarmsClearButton;
	private CCombo								extern1Combo, extern2Combo, extern3Combo, extern4Combo, extern5Combo;
	private CLabel								ext1Label, ext2Label, ext3Label, ext4Label, ext5Label;
	private Button								uBecDipDetectButton;
	private CLabel								temperaureLabel;
	private Button								resetButton;
	private CLabel								flagsLabel;
	private CLabel								mpxAddessesLabel;
	private Text									mainExplanationText;
	private CLabel								mainConfigLabel;
	private CLabel								percentLabel;
	private CCombo								motorShuntCombo;
	private CLabel								motorShuntLabel;
	private CCombo								motorPolsCombo;
	private CCombo								secondgearCombo;
	private CLabel								mainGearLabel;
	private CCombo								mainGearCombo;
	private CCombo								pinionCombo;
	private Button								gearSelectionButton;
	private CCombo								logModeCombo;
	private CCombo								gearRatioMinorCombo;
	private CLabel								dotLabel;
	private CCombo								directRatioCombo;
	private CCombo								sysModeCombo;
	private CLabel								sysModeLabel;
	private CLabel								gearRatioLabel;
	private CLabel								baudrateLabel;
	private CLabel								motorPolsLabel;
	private CCombo								baudrateCombo;
	private CCombo								jlogFirmwareCombo;
	private CCombo								jlogVersionCombo;
	private MpxAddressComposite[]	mpxAddresses	= new MpxAddressComposite[16];

	public class Configuration {

		final String[]	config;
		String					version							= "3.2.2";
		int							configurationLength	= 48;

		Configuration() {
			this.config = new String[48];
			this.config[4] = "2";
			this.config[7] = "10";
			this.config[8] = "10";
			this.config[26] = "9";
		}

		public void setVersion(String newVersion) {
			this.version = newVersion;
			this.config[46] = newVersion.equals("3.1") ? "4" : newVersion.startsWith("3.2") && newVersion.endsWith("3.2.1") ? "36" : "36";
			this.configurationLength = newVersion.equals("3.2.2") ? this.config.length : this.config.length - 1;
		}

		public String getConfiguration() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < this.config.length; i++) {
				sb.append(this.config[i]).append(GDE.STRING_COMMA);
			}
			return sb.delete(sb.length() - 1, sb.length() - 1).toString();
		}

		public void setBaudRate(String baudeRate) {
			this.config[0] = baudeRate;
		}

		public void setSysMode(String sysMode) {
			this.config[1] = sysMode;
		}

		public void setLogMode(int logMode) { //0=NEWLOG; 1=SEQLOG
			this.config[2] = GDE.STRING_EMPTY + ((Integer.valueOf(this.config[2]) & 0x00FE) + (logMode & 0x01));
		}

		public void setMotorCalibration(String calibration) {
			this.config[2] = GDE.STRING_EMPTY + (calibration + (Integer.valueOf(this.config[2]) & 0x0001));
		}

		public void setMotorPols(String numMotorPols) {
			this.config[3] = numMotorPols;
		}

		public void setGearRatioMajor(String gearRatioMajor) {
			this.config[5] = gearRatioMajor;
		}

		public void setGearRatioMinor(String gearRatioMinor) {
			this.config[6] = gearRatioMinor;
		}

		public void setHighPwmWarning(int highPwmWarning) {
			this.config[10] = GDE.STRING_EMPTY + (highPwmWarning + (Integer.valueOf(this.config[10]) & 0x0001));
			;
		}

		public void setLogStop(int logStop) {
			this.config[10] = GDE.STRING_EMPTY + ((Integer.valueOf(this.config[10]) & 0x00FE) + (logStop & 0x01));
		}

		public void setReset(String reset) {
			this.config[11] = reset;
		}

		public void setCapacityAlarm(String capacity) {
			this.config[12] = capacity;
		}

		public void setBatteryAlarmMajor(String alarmVoltageMajor) {
			this.config[13] = alarmVoltageMajor;
		}

		public void setBatteryAlarmMinor(String alarmVoltageMinor) {
			this.config[14] = alarmVoltageMinor;
		}

		public void setPaMaxTempAlarm(String pamaxTemperature) {
			this.config[15] = pamaxTemperature;
		}

		public void setBecDip(String isUbecDip) {
			this.config[16] = isUbecDip;
		}

		public void setExtTemp1(String extTemp1) {
			this.config[17] = extTemp1;
		}

		public void setExtTemp2(String extTemp2) {
			this.config[18] = extTemp2;
		}

		public void setExtTemp3(String extTemp3) {
			this.config[19] = extTemp3;
		}

		public void setExtTemp4(String extTemp4) {
			this.config[20] = extTemp4;
		}

		public void setExtTemp5(String extTemp5) {
			this.config[21] = extTemp5;
		}

		public void setExtTemp1LowerThan(int ltExtTemp1) {
			this.config[22] = GDE.STRING_EMPTY + ((Integer.valueOf(this.config[22]) & 0x00FE) + (ltExtTemp1 & 0x01));
			;
		}

		public void setExtTemp2LowerThan(int ltExtTemp2) {
			this.config[22] = GDE.STRING_EMPTY + ((Integer.valueOf(this.config[22]) & 0x00FD) + (ltExtTemp2 & 0x02));
			;
		}

		public void setExtTemp3LowerThan(int ltExtTemp3) {
			this.config[22] = GDE.STRING_EMPTY + ((Integer.valueOf(this.config[22]) & 0x00FB) + (ltExtTemp3 & 0x04));
			;
		}

		public void setExtTemp4LowerThan(int ltExtTemp4) {
			this.config[22] = GDE.STRING_EMPTY + ((Integer.valueOf(this.config[22]) & 0x00F7) + (ltExtTemp4 & 0x08));
			;
		}

		public void setExtTemp5LowerThan(int ltExtTemp5) {
			this.config[22] = GDE.STRING_EMPTY + ((Integer.valueOf(this.config[22]) & 0x00EF) + (ltExtTemp5 & 0x10));
			;
		}

		public void setNumAddressLines(int numAddressLines) {
			this.config[23] = GDE.STRING_EMPTY + ((Integer.valueOf(this.config[22]) & 0x00FD) + (numAddressLines == 0 ? 0 : 3));
			;
		}

		public void setTemperaturSensorType(String tempSensorType) {
			this.config[24] = tempSensorType;
		}

		public void setRpmSensorType(int rpmSensorPulsePerRevolution) {
			this.config[27] = rpmSensorPulsePerRevolution >= 1 ? "1" : "0";
			this.config[28] = rpmSensorPulsePerRevolution >= 1 ? GDE.STRING_EMPTY + rpmSensorPulsePerRevolution : "0";
		}

		public void setIsMotor(boolean isMotor) {
			this.config[29] = isMotor ? GDE.STRING_EMPTY + (Integer.valueOf(this.config[28]) | 0x80) : GDE.STRING_EMPTY + (Integer.valueOf(this.config[29]) & 0xFFBF);
		}

		public void setIsBlMotorPols(int brushLessMotorPols) {
			this.config[29] = GDE.STRING_EMPTY + (Integer.valueOf(this.config[29]) | 0x0180) + brushLessMotorPols;
		}

		public void setMpxSensorAddress0(int mpxSensorAddress) {
			this.config[30] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress;
		}

		public void setMpxSensorAddress1(int mpxSensorAddress) {
			this.config[31] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress;
		}

		public void setMpxSensorAddress2(int mpxSensorAddress) {
			this.config[32] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress;
		}

		public void setMpxSensorAddress3(int mpxSensorAddress) {
			this.config[33] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress;
		}

		public void setMpxSensorAddress4(int mpxSensorAddress) {
			this.config[34] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress;
		}

		public void setMpxSensorAddress5(int mpxSensorAddress) {
			this.config[35] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress;
		}

		public void setMpxSensorAddress6(int mpxSensorAddress) {
			this.config[36] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress;
		}

		public void setMpxSensorAddress7(int mpxSensorAddress) {
			this.config[37] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress;
		}

		public void setMpxSensorAddress8(int mpxSensorAddress) {
			this.config[38] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress;
		}

		public void setMpxSensorAddress9(int mpxSensorAddress) {
			this.config[39] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress;
		}

		public void setMpxSensorAddress10(int mpxSensorAddress) {
			this.config[40] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress;
		}

		public void setMpxSensorAddress11(int mpxSensorAddress) {
			this.config[41] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress;
		}

		public void setMpxSensorAddress12(int mpxSensorAddress) {
			this.config[42] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress;
		}

		public void setMpxSensorAddress13(int mpxSensorAddress) {
			this.config[43] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress;
		}

		public void setMpxSensorAddress14(int mpxSensorAddress) {
			this.config[44] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress;
		}

		public void setMpxSensorAddress15(int mpxSensorAddress) {
			this.config[45] = mpxSensorAddress >= 16 ? "16" : GDE.STRING_EMPTY + mpxSensorAddress;
		}
	}

	public class MpxAddressComposite extends Composite {

		private CCombo	mpxAddressCombo;
		private CLabel	mpxAddressLabel;

		public MpxAddressComposite(Composite parent, int style, final String labelText, final Configuration configuration, final int index) {
			super(parent, style);
			RowLayout mpxAddressCompositeLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
			RowData mpxAddressCompositeLData = new RowData();
			mpxAddressCompositeLData.width = 95;
			mpxAddressCompositeLData.height = 22;
			this.setLayoutData(mpxAddressCompositeLData);
			this.setLayout(mpxAddressCompositeLayout);
			{
				mpxAddressLabel = new CLabel(this, SWT.NONE);
				RowData mpxAddressLabelLData = new RowData();
				mpxAddressLabelLData.width = 50;
				mpxAddressLabelLData.height = 20;
				mpxAddressLabel.setLayoutData(mpxAddressLabelLData);
				mpxAddressLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				mpxAddressLabel.setText(labelText);
			}
			{
				RowData mpxAddressComboLData = new RowData();
				mpxAddressComboLData.width = 35;
				mpxAddressComboLData.height = 16;
				mpxAddressCombo = new CCombo(this, SWT.BORDER);
				mpxAddressCombo.setLayoutData(mpxAddressComboLData);
				mpxAddressCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				mpxAddressCombo.setItems(new String[] { " 0", " 1", " 2", " 3", " 4", " 5", " 6", " 7", " 8", " 9", " 10", " 11", " 12", " 13", " 14", " 15", " --" });
				mpxAddressCombo.select(16);
				mpxAddressCombo.setEnabled(false);
				mpxAddressCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "mpxAddressCombo.widgetSelected, event=" + evt);
						switch (index) {
						case 0:
							configuration.setMpxSensorAddress0(mpxAddressCombo.getSelectionIndex());
							break;
						case 1:
							configuration.setMpxSensorAddress1(mpxAddressCombo.getSelectionIndex());
							break;
						case 2:
							configuration.setMpxSensorAddress2(mpxAddressCombo.getSelectionIndex());
							break;
						case 3:
							configuration.setMpxSensorAddress3(mpxAddressCombo.getSelectionIndex());
							break;
						case 4:
							configuration.setMpxSensorAddress4(mpxAddressCombo.getSelectionIndex());
							break;
						case 5:
							configuration.setMpxSensorAddress5(mpxAddressCombo.getSelectionIndex());
							break;
						case 6:
							configuration.setMpxSensorAddress6(mpxAddressCombo.getSelectionIndex());
							break;
						case 7:
							configuration.setMpxSensorAddress7(mpxAddressCombo.getSelectionIndex());
							break;
						case 8:
							configuration.setMpxSensorAddress8(mpxAddressCombo.getSelectionIndex());
							break;
						case 9:
							configuration.setMpxSensorAddress9(mpxAddressCombo.getSelectionIndex());
							break;
						case 10:
							configuration.setMpxSensorAddress10(mpxAddressCombo.getSelectionIndex());
							break;
						case 11:
							configuration.setMpxSensorAddress11(mpxAddressCombo.getSelectionIndex());
							break;
						case 12:
							configuration.setMpxSensorAddress12(mpxAddressCombo.getSelectionIndex());
							break;
						case 13:
							configuration.setMpxSensorAddress13(mpxAddressCombo.getSelectionIndex());
							break;
						case 14:
							configuration.setMpxSensorAddress14(mpxAddressCombo.getSelectionIndex());
							break;
						case 15:
							configuration.setMpxSensorAddress15(mpxAddressCombo.getSelectionIndex());
							break;
						}
					}
				});
			}
		}
	}

	final Configuration	configuration;

	/**
	* Auto-generated main method to display this 
	* org.eclipse.swt.widgets.Composite inside a new Shell.
	*/
	public static void main(String[] args) {
		showGUI();
	}

	/**
	* Auto-generated method to display this 
	* org.eclipse.swt.widgets.Composite inside a new Shell.
	*/
	public static void showGUI() {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		JLog2Configuration inst = new JLog2Configuration(shell, SWT.NULL);
		Point size = inst.getSize();
		shell.setLayout(new FillLayout());
		shell.layout();
		if (size.x == 0 && size.y == 0) {
			inst.pack();
			shell.pack();
		}
		else {
			Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
			shell.setSize(shellBounds.width, shellBounds.height);
		}
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
	}

	public JLog2Configuration(org.eclipse.swt.widgets.Composite parent, int style) {
		super(parent, style);
		this.configuration = new Configuration();
		initGUI();
	}

	private void initGUI() {
		try {
			this.setLayout(new FormLayout());
			this.setSize(675, 596);
			this.setEnabled(false);
			{
				optionalGroup = new Group(this, SWT.NONE);
				RowLayout optionalGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				optionalGroup.setLayout(optionalGroupLayout);
				FormData optionalGroupLData = new FormData();
				optionalGroupLData.width = 397;
				optionalGroupLData.height = 237;
				optionalGroupLData.left = new FormAttachment(0, 1000, 260);
				optionalGroupLData.top = new FormAttachment(0, 1000, 328);
				optionalGroup.setLayoutData(optionalGroupLData);
				optionalGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				optionalGroup.setText("optional");
				{
					alarmLinesButton = new Button(optionalGroup, SWT.PUSH | SWT.CENTER);
					RowData alarmLinesButtonLData = new RowData();
					alarmLinesButtonLData.width = 118;
					alarmLinesButtonLData.height = 25;
					alarmLinesButton.setLayoutData(alarmLinesButtonLData);
					alarmLinesButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					alarmLinesButton.setText("# alarm lines");
				}
				{
					alarmLinesCombo = new CCombo(optionalGroup, SWT.BORDER);
					RowData alarmLinesComboLData = new RowData();
					alarmLinesComboLData.width = 32;
					alarmLinesComboLData.height = 17;
					alarmLinesCombo.setLayoutData(alarmLinesComboLData);
					alarmLinesCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					alarmLinesCombo.setText("2");
					alarmLinesCombo.setToolTipText("Anzahl der Alarmausgangsleitungen, die Sie haben wollen. Der Alarmpegel ist \"TTL\", low-aktives Signal.");
					alarmLinesCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("alarmLinesCombo.widgetSelected, event=" + evt);
							//TODO add your code for alarmLinesCombo.widgetSelected
						}
					});
					alarmLinesCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("alarmLinesCombo.mouseMove, event=" + evt);
							//TODO add your code for alarmLinesCombo.mouseMove
						}
					});
				}
				{
					Line1signalLabel = new CLabel(optionalGroup, SWT.RIGHT);
					RowData Line1signalLabelLData = new RowData();
					Line1signalLabelLData.width = 126;
					Line1signalLabelLData.height = 20;
					Line1signalLabel.setLayoutData(Line1signalLabelLData);
					Line1signalLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					Line1signalLabel.setText("L1 signal type");
				}
				{
					line1signalCombo = new CCombo(optionalGroup, SWT.BORDER);
					RowData line1signalComboLData = new RowData();
					line1signalComboLData.width = 74;
					line1signalComboLData.height = 17;
					line1signalCombo.setLayoutData(line1signalComboLData);
					line1signalCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					line1signalCombo.setText("switched");
					line1signalCombo
							.setToolTipText("Signaltyp der ersten Alarmausgangsleitung (low-aktiv). Die zweite Leitung ist immer \"switched\". \"switch(ed)\": geschaltet,  \"interval\": 3x on272-off96 off1396 ms \"flash\": 8x on16-off96 off1604 ms\"flash\": 8x on16-off96 off1604 ms \"Morse\": \"C\"=Capacity  \"V\"=Voltage  \"T\"=Temperature  \"B\"=BEC voltage drop  \"X\"=eXternal (temperatures)");
					line1signalCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("line1signalCombo.widgetSelected, event=" + evt);
							//TODO add your code for line1signalCombo.widgetSelected
						}
					});
					line1signalCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("line1signalCombo.mouseMove, event=" + evt);
							//TODO add your code for line1signalCombo.mouseMove
						}
					});
				}
				{
					subDevicesButton = new Button(optionalGroup, SWT.PUSH | SWT.CENTER);
					RowData subDevicesButtonLData = new RowData();
					subDevicesButtonLData.width = 118;
					subDevicesButtonLData.height = 25;
					subDevicesButton.setLayoutData(subDevicesButtonLData);
					subDevicesButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					subDevicesButton.setText("sub devices");
					subDevicesButton.setEnabled(false);
				}
				{
					subDevicesCombo = new CCombo(optionalGroup, SWT.BORDER);
					subDevicesCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					subDevicesCombo.setText("none");
					subDevicesCombo.setEditable(false);
					RowData subDevicesComboLData = new RowData();
					subDevicesComboLData.width = 62;
					subDevicesComboLData.height = 17;
					subDevicesCombo.setLayoutData(subDevicesComboLData);
					subDevicesCombo.setEnabled(false);
					subDevicesCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("subDevicesCombo.widgetSelected, event=" + evt);
							//TODO add your code for subDevicesCombo.widgetSelected
						}
					});
					subDevicesCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("subDevicesCombo.mouseMove, event=" + evt);
							//TODO add your code for subDevicesCombo.mouseMove
						}
					});
				}
				{
					tempSensorTypeButton = new Button(optionalGroup, SWT.PUSH | SWT.CENTER);
					RowData tempSensorTypeButtonLData = new RowData();
					tempSensorTypeButtonLData.width = 97;
					tempSensorTypeButtonLData.height = 25;
					tempSensorTypeButton.setLayoutData(tempSensorTypeButtonLData);
					tempSensorTypeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					tempSensorTypeButton.setText("temp sensor");
				}
				{
					tempSensorTypeCombo = new CCombo(optionalGroup, SWT.BORDER);
					RowData tempSensorTypeComboLData = new RowData();
					tempSensorTypeComboLData.width = 74;
					tempSensorTypeComboLData.height = 17;
					tempSensorTypeCombo.setLayoutData(tempSensorTypeComboLData);
					tempSensorTypeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					tempSensorTypeCombo.setText("digital");
					tempSensorTypeCombo.setToolTipText("Sind externe Temperatursensoren angeschlossen und von welchem Typ? Es kann EIN analoger Sensor angeschlossen sein oder bis zu fünf digitale.");
					tempSensorTypeCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("tempSensorTypeCombo.mouseMove, event=" + evt);
							//TODO add your code for tempSensorTypeCombo.mouseMove
						}
					});
					tempSensorTypeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("tempSensorTypeCombo.widgetSelected, event=" + evt);
							//TODO add your code for tempSensorTypeCombo.widgetSelected
						}
					});
				}
				{
					rpmSensorButton = new Button(optionalGroup, SWT.PUSH | SWT.CENTER);
					RowData rpmSensorButtonLData = new RowData();
					rpmSensorButtonLData.width = 115;
					rpmSensorButtonLData.height = 25;
					rpmSensorButton.setLayoutData(rpmSensorButtonLData);
					rpmSensorButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					rpmSensorButton.setText("RPM sensor");
				}
				{
					rpmSensorCombo = new CCombo(optionalGroup, SWT.BORDER);
					RowData rpmSensorComboLData = new RowData();
					rpmSensorComboLData.width = 62;
					rpmSensorComboLData.height = 17;
					rpmSensorCombo.setLayoutData(rpmSensorComboLData);
					rpmSensorCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					rpmSensorCombo.setText("15");
					rpmSensorCombo.setToolTipText("\"0\"==kein Sensor   \">0\"==Sensor angeschlossen Der Wert >0 gibt die Anzahl der Impulse pro Umdrehung an. Das ist insbesondere für Langsamdreher. ");
					rpmSensorCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("rpmSensorCombo.widgetSelected, event=" + evt);
							//TODO add your code for rpmSensorCombo.widgetSelected
						}
					});
					rpmSensorCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("rpmSensorCombo.mouseMove, event=" + evt);
							//TODO add your code for rpmSensorCombo.mouseMove
						}
					});
				}
				{
					RowData spacerLData = new RowData();
					spacerLData.width = 9;
					spacerLData.height = 19;
					spacer = new Label(optionalGroup, SWT.NONE);
					spacer.setLayoutData(spacerLData);
				}
				{
					motorButton = new Button(optionalGroup, SWT.CHECK | SWT.CENTER);
					RowData motorButtonLData = new RowData();
					motorButtonLData.width = 77;
					motorButtonLData.height = 21;
					motorButton.setLayoutData(motorButtonLData);
					motorButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					motorButton.setText("is motor");
					motorButton.setToolTipText("Wenn die mittlere Drehzahl 2000 UPM übersteigt aktivieren Sie \"Mot\".");
					motorButton.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("motorButton.mouseMove, event=" + evt);
							//TODO add your code for motorButton.mouseMove
						}
					});
					motorButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("motorButton.widgetSelected, event=" + evt);
							//TODO add your code for motorButton.widgetSelected
						}
					});
				}
				{
					brushLessButton = new Button(optionalGroup, SWT.CHECK | SWT.CENTER);
					RowData brushLessButtonLData = new RowData();
					brushLessButtonLData.width = 103;
					brushLessButtonLData.height = 20;
					brushLessButton.setLayoutData(brushLessButtonLData);
					brushLessButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					brushLessButton.setText("is brush less");
					brushLessButton
							.setToolTipText("Ist der Drehzahlsensor für einen bürstenlosen Motor, aktivieren Sie \"BL\". \"PPR\" (Pulse Per Revolution) wird nun nicht mehr gewertet, stattdessen die Polzahl des Motors.");
					brushLessButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("brushLessButton.widgetSelected, event=" + evt);
							//TODO add your code for brushLessButton.widgetSelected
						}
					});
					brushLessButton.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("brushLessButton.mouseMove, event=" + evt);
							//TODO add your code for brushLessButton.mouseMove
						}
					});
				}
				{
					telemetryButton = new Button(optionalGroup, SWT.PUSH | SWT.CENTER);
					RowData telemetryButtonLData = new RowData();
					telemetryButtonLData.width = 114;
					telemetryButtonLData.height = 25;
					telemetryButton.setLayoutData(telemetryButtonLData);
					telemetryButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					telemetryButton.setText("telemetry/live");
				}
				{
					telemetryCombo = new CCombo(optionalGroup, SWT.BORDER);
					RowData telemetryComboLData = new RowData();
					telemetryComboLData.width = 113;
					telemetryComboLData.height = 17;
					telemetryCombo.setLayoutData(telemetryComboLData);
					telemetryCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					telemetryCombo.setText(" --- ");
					telemetryCombo
							.setToolTipText("Telemetrieausgang benutzt und für welches Protokoll? \nDefault: COM ist ungenutzt:\n(Unidisplay: Das Terminal kann nur direkt an den Logger gesteckt werden.\n\"JTX\" im Eigenbau mit 2x XBee. Siehe j-log.net. Die JETIbox kann auch direkt am Logger betrieben werden.)");
					telemetryCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("telemetryCombo.mouseMove, event=" + evt);
							//TODO add your code for telemetryCombo.mouseMove
						}
					});
					telemetryCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("telemetryCombo.widgetSelected, event=" + evt);
							//TODO add your code for telemetryCombo.widgetSelected
						}
					});
				}
				{
					sensorAdapterButton = new Button(optionalGroup, SWT.PUSH | SWT.CENTER);
					RowData sensorAdapterButtonLData = new RowData();
					sensorAdapterButtonLData.width = 141;
					sensorAdapterButtonLData.height = 25;
					sensorAdapterButton.setLayoutData(sensorAdapterButtonLData);
					sensorAdapterButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					sensorAdapterButton.setText("adapter required");
					sensorAdapterButton.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("sensorAdapterButton.mouseMove, event=" + evt);
							//TODO add your code for sensorAdapterButton.mouseMove
						}
					});
					sensorAdapterButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("sensorAdapterButton.widgetSelected, event=" + evt);
							//TODO add your code for sensorAdapterButton.widgetSelected
						}
					});
				}
				{
					mpxAddessesLabel = new CLabel(optionalGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData mpxAddessesLabelLData = new RowData();
					mpxAddessesLabelLData.width = 384;
					mpxAddessesLabelLData.height = 17;
					mpxAddessesLabel.setLayoutData(mpxAddessesLabelLData);
					mpxAddessesLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					mpxAddessesLabel.setText("Multiplex Adress Configuration");

					mpxAddresses[0] = new MpxAddressComposite(optionalGroup, SWT.NONE, "U bec", this.configuration, 0);
					mpxAddresses[1] = new MpxAddressComposite(optionalGroup, SWT.NONE, "I mot", this.configuration, 1);
					mpxAddresses[2] = new MpxAddressComposite(optionalGroup, SWT.NONE, "rpm U", this.configuration, 2);
					mpxAddresses[3] = new MpxAddressComposite(optionalGroup, SWT.NONE, "1 PA", this.configuration, 3);
					mpxAddresses[4] = new MpxAddressComposite(optionalGroup, SWT.NONE, "Capacity", this.configuration, 4);
					mpxAddresses[5] = new MpxAddressComposite(optionalGroup, SWT.NONE, "U bec dip", this.configuration, 5);
					mpxAddresses[6] = new MpxAddressComposite(optionalGroup, SWT.NONE, "I bec", this.configuration, 6);
					mpxAddresses[7] = new MpxAddressComposite(optionalGroup, SWT.NONE, "e RPM", this.configuration, 7);
					mpxAddresses[8] = new MpxAddressComposite(optionalGroup, SWT.NONE, "11", this.configuration, 8);
					mpxAddresses[9] = new MpxAddressComposite(optionalGroup, SWT.NONE, "12", this.configuration, 9);
					mpxAddresses[10] = new MpxAddressComposite(optionalGroup, SWT.NONE, "13", this.configuration, 10);
					mpxAddresses[11] = new MpxAddressComposite(optionalGroup, SWT.NONE, "14", this.configuration, 11);
					mpxAddresses[12] = new MpxAddressComposite(optionalGroup, SWT.NONE, "Pwr int", this.configuration, 12);
					mpxAddresses[13] = new MpxAddressComposite(optionalGroup, SWT.NONE, "THR", this.configuration, 13);
					mpxAddresses[14] = new MpxAddressComposite(optionalGroup, SWT.NONE, "PWM", this.configuration, 14);
					mpxAddresses[15] = new MpxAddressComposite(optionalGroup, SWT.NONE, "rpm Motor", this.configuration, 15);
				}
			}
			{
				alarmGroup = new Group(this, SWT.NONE);
				RowLayout alarmGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				alarmGroup.setLayout(alarmGroupLayout);
				FormData alarmGroupLData = new FormData();
				alarmGroupLData.width = 235;
				alarmGroupLData.height = 237;
				alarmGroupLData.left = new FormAttachment(0, 1000, 7);
				alarmGroupLData.top = new FormAttachment(0, 1000, 328);
				alarmGroup.setLayoutData(alarmGroupLData);
				alarmGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				alarmGroup.setText("alarms");
				{
					alarmsClearButton = new Button(alarmGroup, SWT.PUSH | SWT.CENTER);
					RowData alarmsClearButtonLData = new RowData();
					alarmsClearButtonLData.width = 86;
					alarmsClearButtonLData.height = 25;
					alarmsClearButton.setLayoutData(alarmsClearButtonLData);
					alarmsClearButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					alarmsClearButton.setText("clear alarms");
					alarmsClearButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("alarmsClearButton.widgetSelected, event=" + evt);
							//TODO add your code for alarmsClearButton.widgetSelected
						}
					});
					alarmsClearButton.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("alarmsClearButton.mouseMove, event=" + evt);
							//TODO add your code for alarmsClearButton.mouseMove
						}
					});
				}
				{
					uBecDipDetectButton = new Button(alarmGroup, SWT.CHECK | SWT.CENTER);
					RowData uBecDipDetectButtonLData = new RowData();
					uBecDipDetectButtonLData.width = 138;
					uBecDipDetectButtonLData.height = 21;
					uBecDipDetectButton.setLayoutData(uBecDipDetectButtonLData);
					uBecDipDetectButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					uBecDipDetectButton.setText("Ubec dip detection");
					uBecDipDetectButton.setToolTipText("U-BEC Spannungsdip. Alarm löst aus, wenn der negative Dip > 0,5V ist.");
					uBecDipDetectButton.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("uBecDipDetectButton.mouseMove, event=" + evt);
							//TODO add your code for uBecDipDetectButton.mouseMove
						}
					});
					uBecDipDetectButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("uBecDipDetectButton.widgetSelected, event=" + evt);
							//TODO add your code for uBecDipDetectButton.widgetSelected
						}
					});
				}
				{
					capacityAlarmLabel = new CLabel(alarmGroup, SWT.NONE);
					RowData capacityAlarmLabelLData = new RowData();
					capacityAlarmLabelLData.width = 106;
					capacityAlarmLabelLData.height = 20;
					capacityAlarmLabel.setLayoutData(capacityAlarmLabelLData);
					capacityAlarmLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					capacityAlarmLabel.setText("capacity");
				}
				{
					capacityAlarmCombo = new CCombo(alarmGroup, SWT.BORDER);
					RowData capacityAlarmComboLData = new RowData();
					capacityAlarmComboLData.width = 57;
					capacityAlarmComboLData.height = 17;
					capacityAlarmCombo.setLayoutData(capacityAlarmComboLData);
					capacityAlarmCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					capacityAlarmCombo.setText("2700");
					capacityAlarmCombo.setToolTipText("Verbrauchte Kapazität in Wert * 100mAh. Löst den Alarm aus, wenn der Wert erreicht ist");
					capacityAlarmCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("capacityAlarmCombo.widgetSelected, event=" + evt);
							//TODO add your code for capacityAlarmCombo.widgetSelected
						}
					});
					capacityAlarmCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("capacityAlarmCombo.mouseMove, event=" + evt);
							//TODO add your code for capacityAlarmCombo.mouseMove
						}
					});
				}
				{
					mAhLabel = new CLabel(alarmGroup, SWT.NONE);
					RowData mAhLabelLData = new RowData();
					mAhLabelLData.width = 41;
					mAhLabelLData.height = 20;
					mAhLabel.setLayoutData(mAhLabelLData);
					mAhLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					mAhLabel.setText("[mAh]");
				}
				{
					uBatMinAlarmLabel = new CLabel(alarmGroup, SWT.NONE);
					RowData uBatMinAlarmLabelLData = new RowData();
					uBatMinAlarmLabelLData.width = 106;
					uBatMinAlarmLabelLData.height = 20;
					uBatMinAlarmLabel.setLayoutData(uBatMinAlarmLabelLData);
					uBatMinAlarmLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					uBatMinAlarmLabel.setText("bat voltage min");
				}
				{
					voltagebatteryMinCombo1 = new CCombo(alarmGroup, SWT.BORDER);
					RowData voltagebatteryMinCombo1LData = new RowData();
					voltagebatteryMinCombo1LData.width = 34;
					voltagebatteryMinCombo1LData.height = 17;
					voltagebatteryMinCombo1.setLayoutData(voltagebatteryMinCombo1LData);
					voltagebatteryMinCombo1.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					voltagebatteryMinCombo1.setText("12");
					voltagebatteryMinCombo1.setToolTipText("Akku-Minimalspannung, ganzzahliger Teil. Alarm löst aus, wenn der Wert unterschritten wird.");
					voltagebatteryMinCombo1.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("voltagebatteryMinCombo1.mouseMove, event=" + evt);
							//TODO add your code for voltagebatteryMinCombo1.mouseMove
						}
					});
					voltagebatteryMinCombo1.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("voltagebatteryMinCombo1.widgetSelected, event=" + evt);
							//TODO add your code for voltagebatteryMinCombo1.widgetSelected
						}
					});
				}
				{
					dotLabel = new CLabel(alarmGroup, SWT.NONE);
					dotLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					dotLabel.setText(".");
					RowData RALData = new RowData();
					RALData.width = 7;
					RALData.height = 20;
					dotLabel.setLayoutData(RALData);
					dotLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
				}
				{
					voltagebatteryAlarmMaxCombo2 = new CCombo(alarmGroup, SWT.BORDER);
					RowData voltagebatteryAlarmMaxCombo2LData = new RowData();
					voltagebatteryAlarmMaxCombo2LData.width = 34;
					voltagebatteryAlarmMaxCombo2LData.height = 17;
					voltagebatteryAlarmMaxCombo2.setLayoutData(voltagebatteryAlarmMaxCombo2LData);
					voltagebatteryAlarmMaxCombo2.setText("05");
					voltagebatteryAlarmMaxCombo2.setToolTipText("Akku-Minimalspannung, Zehntel.");
					voltagebatteryAlarmMaxCombo2.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("voltagebatteryAlarmMaxCombo2.widgetSelected, event=" + evt);
							//TODO add your code for voltagebatteryAlarmMaxCombo2.widgetSelected
						}
					});
					voltagebatteryAlarmMaxCombo2.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("voltagebatteryAlarmMaxCombo2.mouseMove, event=" + evt);
							//TODO add your code for voltagebatteryAlarmMaxCombo2.mouseMove
						}
					});
				}
				{
					voltageLabel = new CLabel(alarmGroup, SWT.NONE);
					RowData voltageLabelLData = new RowData();
					voltageLabelLData.width = 23;
					voltageLabelLData.height = 19;
					voltageLabel.setLayoutData(voltageLabelLData);
					voltageLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					voltageLabel.setText("[V]");
				}
				{
					paTempMaxLabel = new CLabel(alarmGroup, SWT.NONE);
					RowData paTempMaxLabelLData = new RowData();
					paTempMaxLabelLData.width = 106;
					paTempMaxLabelLData.height = 20;
					paTempMaxLabel.setLayoutData(paTempMaxLabelLData);
					paTempMaxLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					paTempMaxLabel.setText("PA temp max");
				}
				{
					paTempMaxCombo = new CCombo(alarmGroup, SWT.BORDER);
					RowData paTempMaxComboLData = new RowData();
					paTempMaxComboLData.width = 57;
					paTempMaxComboLData.height = 17;
					paTempMaxCombo.setLayoutData(paTempMaxComboLData);
					paTempMaxCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					paTempMaxCombo.setText("100");
					paTempMaxCombo.setToolTipText("Temperatur (°C) der Endstufen. Alarm löst aus, wenn der Wert überschritten wird.");
					paTempMaxCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("paTempMaxCombo.mouseMove, event=" + evt);
							//TODO add your code for paTempMaxCombo.mouseMove
						}
					});
					paTempMaxCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("paTempMaxCombo.widgetSelected, event=" + evt);
							//TODO add your code for paTempMaxCombo.widgetSelected
						}
					});
				}
				{
					temperaureLabel = new CLabel(alarmGroup, SWT.NONE);
					RowData temperaureLabelLData = new RowData();
					temperaureLabelLData.width = 27;
					temperaureLabelLData.height = 19;
					temperaureLabel.setLayoutData(temperaureLabelLData);
					temperaureLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					temperaureLabel.setText("[°C]");
				}
				{
					ext1Label = new CLabel(alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext1LabelLData = new RowData();
					ext1LabelLData.width = 106;
					ext1LabelLData.height = 20;
					ext1Label.setLayoutData(ext1LabelLData);
					ext1Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					ext1Label.setText("extern Temp 1");
				}
				{
					extern1Combo = new CCombo(alarmGroup, SWT.BORDER);
					RowData extern1ComboLData = new RowData();
					extern1ComboLData.width = 46;
					extern1ComboLData.height = 17;
					extern1Combo.setLayoutData(extern1ComboLData);
					extern1Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					extern1Combo.setText("123");
					extern1Combo.setEnabled(false);
					extern1Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("extern1Combo.widgetSelected, event=" + evt);
							//TODO add your code for extern1Combo.widgetSelected
						}
					});
					extern1Combo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("extern1Combo.mouseMove, event=" + evt);
							//TODO add your code for extern1Combo.mouseMove
						}
					});
				}
				{
					temperaureLabel = new CLabel(alarmGroup, SWT.NONE);
					RowData temperaureLabelLData = new RowData();
					temperaureLabelLData.width = 27;
					temperaureLabelLData.height = 19;
					temperaureLabel.setLayoutData(temperaureLabelLData);
					temperaureLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					temperaureLabel.setText("[°C]");
				}
				{
					ext1smallerButton = new Button(alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext1smallerButtonLData = new RowData();
					ext1smallerButtonLData.width = 31;
					ext1smallerButtonLData.height = 20;
					ext1smallerButton.setLayoutData(ext1smallerButtonLData);
					ext1smallerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					ext1smallerButton.setText(" <");
				}
				{
					ext2Label = new CLabel(alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext2LabelLData = new RowData();
					ext2LabelLData.width = 106;
					ext2LabelLData.height = 20;
					ext2Label.setLayoutData(ext2LabelLData);
					ext2Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					ext2Label.setText("extern Temp 2");
				}
				{
					extern2Combo = new CCombo(alarmGroup, SWT.BORDER);
					RowData extern2ComboLData = new RowData();
					extern2ComboLData.width = 46;
					extern2ComboLData.height = 17;
					extern2Combo.setLayoutData(extern2ComboLData);
					extern2Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					extern2Combo.setText("123");
					extern2Combo.setEnabled(false);
					extern2Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("extern2Combo.widgetSelected, event=" + evt);
							//TODO add your code for extern2Combo.widgetSelected
						}
					});
					extern2Combo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("extern2Combo.mouseMove, event=" + evt);
							//TODO add your code for extern2Combo.mouseMove
						}
					});
				}
				{
					temperaureLabel = new CLabel(alarmGroup, SWT.NONE);
					RowData temperaureLabelLData = new RowData();
					temperaureLabelLData.width = 27;
					temperaureLabelLData.height = 19;
					temperaureLabel.setLayoutData(temperaureLabelLData);
					temperaureLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					temperaureLabel.setText("[°C]");
				}
				{
					ext2smallerButton = new Button(alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext2smallerButtonLData = new RowData();
					ext2smallerButtonLData.width = 31;
					ext2smallerButtonLData.height = 20;
					ext2smallerButton.setLayoutData(ext2smallerButtonLData);
					ext2smallerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					ext2smallerButton.setText(" <");
					ext2smallerButton.setEnabled(false);
					ext2smallerButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("ext2smallerButton.widgetSelected, event=" + evt);
							//TODO add your code for ext2smallerButton.widgetSelected
						}
					});
					ext2smallerButton.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("ext2smallerButton.mouseMove, event=" + evt);
							//TODO add your code for ext2smallerButton.mouseMove
						}
					});
					ext1smallerButton.setText(" <");
					ext1smallerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					ext1smallerButton.setEnabled(false);
					ext1smallerButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("ext1smallerButton.widgetSelected, event=" + evt);
							//TODO add your code for ext1smallerButton.widgetSelected
						}
					});
					ext1smallerButton.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("ext1smallerButton.mouseMove, event=" + evt);
							//TODO add your code for ext1smallerButton.mouseMove
						}
					});
				}
				{
					ext3Label = new CLabel(alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext3LabelLData = new RowData();
					ext3LabelLData.width = 106;
					ext3LabelLData.height = 20;
					ext3Label.setLayoutData(ext3LabelLData);
					ext3Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					ext3Label.setText("extern Temp 3");
				}
				{
					extern3Combo = new CCombo(alarmGroup, SWT.BORDER);
					RowData extern3ComboLData = new RowData();
					extern3ComboLData.width = 46;
					extern3ComboLData.height = 17;
					extern3Combo.setLayoutData(extern3ComboLData);
					extern3Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					extern3Combo.setText("123");
					extern3Combo.setEnabled(false);
					extern3Combo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("extern3Combo.mouseMove, event=" + evt);
							//TODO add your code for extern3Combo.mouseMove
						}
					});
					extern3Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("extern3Combo.widgetSelected, event=" + evt);
							//TODO add your code for extern3Combo.widgetSelected
						}
					});
				}
				{
					temperaureLabel = new CLabel(alarmGroup, SWT.NONE);
					RowData temperaureLabelLData = new RowData();
					temperaureLabelLData.width = 27;
					temperaureLabelLData.height = 19;
					temperaureLabel.setLayoutData(temperaureLabelLData);
					temperaureLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					temperaureLabel.setText("[°C]");
				}
				{
					ext3smallerButton = new Button(alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext3smallerButtonLData = new RowData();
					ext3smallerButtonLData.width = 31;
					ext3smallerButtonLData.height = 20;
					ext3smallerButton.setLayoutData(ext3smallerButtonLData);
					ext3smallerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					ext3smallerButton.setText(" <");
					ext3smallerButton.setEnabled(false);
					ext3smallerButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("ext3smallerButton.widgetSelected, event=" + evt);
							//TODO add your code for ext3smallerButton.widgetSelected
						}
					});
					ext3smallerButton.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("ext3smallerButton.mouseMove, event=" + evt);
							//TODO add your code for ext3smallerButton.mouseMove
						}
					});
				}
				{
					ext4Label = new CLabel(alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext4LabelLData = new RowData();
					ext4LabelLData.width = 106;
					ext4LabelLData.height = 20;
					ext4Label.setLayoutData(ext4LabelLData);
					ext4Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					ext4Label.setText("extern Temp 4");
				}
				{
					extern4Combo = new CCombo(alarmGroup, SWT.BORDER);
					RowData extern4ComboLData = new RowData();
					extern4ComboLData.width = 46;
					extern4ComboLData.height = 17;
					extern4Combo.setLayoutData(extern4ComboLData);
					extern4Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					extern4Combo.setText("123");
					extern4Combo.setEnabled(false);
					extern4Combo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("extern4Combo.mouseMove, event=" + evt);
							//TODO add your code for extern4Combo.mouseMove
						}
					});
					extern4Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("extern4Combo.widgetSelected, event=" + evt);
							//TODO add your code for extern4Combo.widgetSelected
						}
					});
				}
				{
					temperaureLabel = new CLabel(alarmGroup, SWT.NONE);
					RowData temperaureLabelLData = new RowData();
					temperaureLabelLData.width = 27;
					temperaureLabelLData.height = 19;
					temperaureLabel.setLayoutData(temperaureLabelLData);
					temperaureLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					temperaureLabel.setText("[°C]");
				}
				{
					ext4smallerButton = new Button(alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext4smallerButtonLData = new RowData();
					ext4smallerButtonLData.width = 31;
					ext4smallerButtonLData.height = 20;
					ext4smallerButton.setLayoutData(ext4smallerButtonLData);
					ext4smallerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					ext4smallerButton.setText(" <");
					ext4smallerButton.setEnabled(false);
					ext4smallerButton.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("ext4smallerButton.mouseMove, event=" + evt);
							//TODO add your code for ext4smallerButton.mouseMove
						}
					});
					ext4smallerButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("ext4smallerButton.widgetSelected, event=" + evt);
							//TODO add your code for ext4smallerButton.widgetSelected
						}
					});
				}
				{
					ext5Label = new CLabel(alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext5LabelLData = new RowData();
					ext5LabelLData.width = 106;
					ext5LabelLData.height = 20;
					ext5Label.setLayoutData(ext5LabelLData);
					ext5Label.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					ext5Label.setText("extern Temp 5");
				}
				{
					extern5Combo = new CCombo(alarmGroup, SWT.BORDER);
					RowData extern5ComboLData = new RowData();
					extern5ComboLData.width = 46;
					extern5ComboLData.height = 17;
					extern5Combo.setLayoutData(extern5ComboLData);
					extern5Combo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					extern5Combo.setText("123");
					extern5Combo.setEnabled(false);
					extern5Combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("extern5Combo.widgetSelected, event=" + evt);
							//TODO add your code for extern5Combo.widgetSelected
						}
					});
					extern5Combo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("extern5Combo.mouseMove, event=" + evt);
							//TODO add your code for extern5Combo.mouseMove
						}
					});
				}
				{
					temperaureLabel = new CLabel(alarmGroup, SWT.NONE);
					RowData temperaureLabelLData = new RowData();
					temperaureLabelLData.width = 27;
					temperaureLabelLData.height = 19;
					temperaureLabel.setLayoutData(temperaureLabelLData);
					temperaureLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					temperaureLabel.setText("[°C]");
				}
				{
					ext5smallerButton = new Button(alarmGroup, SWT.CHECK | SWT.LEFT);
					RowData ext5smallerButtonLData = new RowData();
					ext5smallerButtonLData.width = 31;
					ext5smallerButtonLData.height = 20;
					ext5smallerButton.setLayoutData(ext5smallerButtonLData);
					ext5smallerButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					ext5smallerButton.setText(" <");
					ext5smallerButton.setEnabled(false);
					ext5smallerButton.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("ext5smallerButton.mouseMove, event=" + evt);
							//TODO add your code for ext5smallerButton.mouseMove
						}
					});
					ext5smallerButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("ext5smallerButton.widgetSelected, event=" + evt);
							//TODO add your code for ext5smallerButton.widgetSelected
						}
					});
				}
			}
			{
				mainConfigGroup = new Group(this, SWT.NONE);
				RowLayout mainConfigGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				mainConfigGroup.setLayout(mainConfigGroupLayout);
				FormData mainConfigGroupLData = new FormData();				mainConfigGroupLData.width = 650;
				mainConfigGroupLData.left = new FormAttachment(0, 1000, 7);
				mainConfigGroupLData.top = new FormAttachment(0, 1000, 7);
				mainConfigGroupLData.right =  new FormAttachment(1000, 1000, -7);
				mainConfigGroupLData.height = 290;
				mainConfigGroup.setLayoutData(mainConfigGroupLData);
				mainConfigGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				mainConfigGroup.setText("main configuration");
				{
					jlcUrlLabel = new CLabel(mainConfigGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData jlcUrlLabelLData = new RowData();
					jlcUrlLabelLData.width = 154;
					jlcUrlLabelLData.height = 25;
					jlcUrlLabel.setLayoutData(jlcUrlLabelLData);
					jlcUrlLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					jlcUrlLabel.setText("JLC links ---->>");
				}
				{
					jlcDownloadButton = new Button(mainConfigGroup, SWT.PUSH | SWT.CENTER);
					RowData jlcDownloadButtonLData = new RowData();
					jlcDownloadButtonLData.width = 114;
					jlcDownloadButtonLData.height = 25;
					jlcDownloadButton.setLayoutData(jlcDownloadButtonLData);
					jlcDownloadButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					jlcDownloadButton.setText("JLC downloads");
				}
				{
					jlcForumButton = new Button(mainConfigGroup, SWT.PUSH | SWT.CENTER);
					RowData jlcForumButtonLData = new RowData();
					jlcForumButtonLData.width = 114;
					jlcForumButtonLData.height = 25;
					jlcForumButton.setLayoutData(jlcForumButtonLData);
					jlcForumButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					jlcForumButton.setText("JCL forum");
				}
				{
					new Label(mainConfigGroup, SWT.NONE).setLayoutData(new RowData(620, 5));
				}
				{
					mainConfigLabel = new CLabel(mainConfigGroup, SWT.NONE);
					RowData label_ILLData = new RowData();
					label_ILLData.width = 71;
					label_ILLData.height = 20;
					mainConfigLabel.setLayoutData(label_ILLData);
					mainConfigLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					mainConfigLabel.setText("Basics :");
				}
				{
					jlogVersionCombo = new CCombo(mainConfigGroup, SWT.BORDER);
					jlogVersionCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					jlogVersionCombo.setText("JLog2");
					jlogVersionCombo.setEditable(false);
					jlogVersionCombo.setBackground(SWTResourceManager.getColor(255, 128, 0));
					jlogVersionCombo.setToolTipText("Select JLog type");
				}
				{
					jlogFirmwareCombo = new CCombo(mainConfigGroup, SWT.BORDER);
					jlogFirmwareCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					jlogFirmwareCombo.setText("3.3.2");
					jlogFirmwareCombo.setBackground(SWTResourceManager.getColor(255, 128, 0));
					jlogFirmwareCombo.setToolTipText("Select JLog firmware version");
					jlogFirmwareCombo.setEditable(false);
					jlogFirmwareCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("cCombo1.widgetSelected, event=" + evt);
							//TODO add your code for cCombo1.widgetSelected
						}
					});
					jlogFirmwareCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("cCombo1.mouseMove, event=" + evt);
							//TODO add your code for cCombo1.mouseMove
						}
					});
				}
				{
					jlogConfigurationCombo = new CCombo(mainConfigGroup, SWT.BORDER);
					RowData jlogConfigurationComboLData = new RowData();
					jlogConfigurationComboLData.width = 200;
					jlogConfigurationComboLData.height = 17;
					jlogConfigurationCombo.setLayoutData(jlogConfigurationComboLData);
					jlogConfigurationCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					jlogConfigurationCombo.setText("--------------------");
					jlogConfigurationCombo.setBackground(SWTResourceManager.getColor(255, 128, 0));
					jlogConfigurationCombo.setToolTipText("select special configurations");
					jlogConfigurationCombo.setEditable(false);
					jlogConfigurationCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("jlogConfigurationCombo.mouseMove, event=" + evt);
							//TODO add your code for jlogConfigurationCombo.mouseMove
						}
					});
					jlogConfigurationCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("jlogConfigurationCombo.widgetSelected, event=" + evt);
							//TODO add your code for jlogConfigurationCombo.widgetSelected
						}
					});
				}
				{
					new Label(mainConfigGroup, SWT.NONE).setLayoutData(new RowData(620, 5));
				}
				{
					baudrateLabel = new CLabel(mainConfigGroup, SWT.NONE);
					RowData baudrateLabelLData = new RowData();
					baudrateLabelLData.width = 105;
					baudrateLabelLData.height = 20;
					baudrateLabel.setLayoutData(baudrateLabelLData);
					baudrateLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					baudrateLabel.setText("Baudrate");
				}
				{
					baudrateCombo = new CCombo(mainConfigGroup, SWT.BORDER);
					RowData baudrateComboLData = new RowData();
					baudrateComboLData.width = 99;
					baudrateComboLData.height = 17;
					baudrateCombo.setLayoutData(baudrateComboLData);
					baudrateCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					baudrateCombo.setText("JIVE");
					baudrateCombo.setToolTipText("selelct asynchronous data rate");
					baudrateCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("baudrateCombo.widgetSelected, event=" + evt);
							//TODO add your code for baudrateCombo.widgetSelected
						}
					});
					baudrateCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("baudrateCombo.mouseMove, event=" + evt);
							//TODO add your code for baudrateCombo.mouseMove
						}
					});
				}
				{
					gearRatioLabel = new CLabel(mainConfigGroup, SWT.CENTER | SWT.EMBEDDED);
					RowData gearRatioLabelLData = new RowData();
					gearRatioLabelLData.width = 328;
					gearRatioLabelLData.height = 20;
					gearRatioLabel.setLayoutData(gearRatioLabelLData);
					gearRatioLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					gearRatioLabel.setText("adjust gear ratio");
				}
				{
					sysModeLabel = new CLabel(mainConfigGroup, SWT.NONE);
					RowData sysModeLabelLData = new RowData();
					sysModeLabelLData.width = 105;
					sysModeLabelLData.height = 20;
					sysModeLabel.setLayoutData(sysModeLabelLData);
					sysModeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					sysModeLabel.setText("SYSmode");
				}
				{
					sysModeCombo = new CCombo(mainConfigGroup, SWT.BORDER);
					RowData sysModeComboLData = new RowData();
					sysModeComboLData.width = 99;
					sysModeComboLData.height = 17;
					sysModeCombo.setLayoutData(sysModeComboLData);
					sysModeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					sysModeCombo.setText("NEWLOG");
					sysModeCombo.setToolTipText("select SYSlog type, NEWLOG starts a new Logfile each session, SEQLOG always appends");
					sysModeCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("sysModeCombo.mouseMove, event=" + evt);
							//TODO add your code for sysModeCombo.mouseMove
						}
					});
					sysModeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("sysModeCombo.widgetSelected, event=" + evt);
							//TODO add your code for sysModeCombo.widgetSelected
						}
					});
				}
				{
					new Label(mainConfigGroup, SWT.NONE).setLayoutData(new RowData(19, 19));
				}
				{
					directRatioButton = new Button(mainConfigGroup, SWT.CHECK | SWT.RIGHT);
					RowData directRatioButtonLData = new RowData();
					directRatioButtonLData.width = 73;
					directRatioButtonLData.height = 21;
					directRatioButton.setLayoutData(directRatioButtonLData);
					directRatioButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					directRatioButton.setText("ratio  1: ");
					directRatioButton.setSelection(true);
					directRatioButton.setToolTipText("configure gear ratio directly");
					directRatioButton.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("directRatioButton.mouseMove, event=" + evt);
							//TODO add your code for directRatioButton.mouseMove
						}
					});
				}
				{
					directRatioCombo = new CCombo(mainConfigGroup, SWT.BORDER);
					RowData directRatioComboLData = new RowData();
					directRatioComboLData.width = 45;
					directRatioComboLData.height = 17;
					directRatioCombo.setLayoutData(directRatioComboLData);
					directRatioCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					directRatioCombo.setText("1");
					directRatioCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("directRatioCombo.widgetSelected, event=" + evt);
							//TODO add your code for directRatioCombo.widgetSelected
						}
					});
					directRatioCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("directRatioCombo.mouseMove, event=" + evt);
							//TODO add your code for directRatioCombo.mouseMove
						}
					});
				}
				{
					dotLabel = new CLabel(mainConfigGroup, SWT.NONE);
					dotLabel.setText(" . ");
					dotLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
				}
				{
					gearRatioMinorCombo = new CCombo(mainConfigGroup, SWT.BORDER);
					RowData gearRatioMinorComboLData = new RowData();
					gearRatioMinorComboLData.width = 45;
					gearRatioMinorComboLData.height = 17;
					gearRatioMinorCombo.setLayoutData(gearRatioMinorComboLData);
					gearRatioMinorCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					gearRatioMinorCombo.setText("0");
					gearRatioMinorCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("gearRatioMinorCombo.mouseMove, event=" + evt);
							//TODO add your code for gearRatioMinorCombo.mouseMove
						}
					});
					gearRatioMinorCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("gearRatioMinorCombo.widgetSelected, event=" + evt);
							//TODO add your code for gearRatioMinorCombo.widgetSelected
						}
					});
				}
				{
					new Label(mainConfigGroup, SWT.NONE).setLayoutData(new RowData(120, 19));
				}
				{
					logModeLabel = new CLabel(mainConfigGroup, SWT.NONE);
					RowData logModeLabelLData = new RowData();
					logModeLabelLData.width = 105;
					logModeLabelLData.height = 20;
					logModeLabel.setLayoutData(logModeLabelLData);
					logModeLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					logModeLabel.setText("LOGmode");
				}
				{
					logModeCombo = new CCombo(mainConfigGroup, SWT.BORDER);
					RowData logModeComboLData = new RowData();
					logModeComboLData.width = 99;
					logModeComboLData.height = 17;
					logModeCombo.setLayoutData(logModeComboLData);
					logModeCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					logModeCombo.setText(" (0) OF/CSV");
					logModeCombo.setToolTipText("select log file type, OF/CSV can imported to DataExplorer only");
					logModeCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("logModeCombo.widgetSelected, event=" + evt);
							//TODO add your code for logModeCombo.widgetSelected
						}
					});
					logModeCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("logModeCombo.mouseMove, event=" + evt);
							//TODO add your code for logModeCombo.mouseMove
						}
					});
				}
				{
					new Label(mainConfigGroup, SWT.NONE).setLayoutData(new RowData(19, 19));
				}
				{
					gearSelectionButton = new Button(mainConfigGroup, SWT.CHECK | SWT.RIGHT);
					RowData gearSelectionButtonLData = new RowData();
					gearSelectionButtonLData.width = 73;
					gearSelectionButtonLData.height = 21;
					gearSelectionButton.setLayoutData(gearSelectionButtonLData);
					gearSelectionButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					gearSelectionButton.setText("pinion");
					gearSelectionButton.setSelection(false);
					gearSelectionButton.setToolTipText("configure gear ratio by known gear wheels");
					gearSelectionButton.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("gearSelectionButton.mouseMove, event=" + evt);
							//TODO add your code for gearSelectionButton.mouseMove
						}
					});
					gearSelectionButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("gearSelectionButton.widgetSelected, event=" + evt);
							//TODO add your code for gearSelectionButton.widgetSelected
						}
					});
				}
				{
					pinionCombo = new CCombo(mainConfigGroup, SWT.BORDER);
					RowData pinionComboLData = new RowData();
					pinionComboLData.width = 45;
					pinionComboLData.height = 17;
					pinionCombo.setLayoutData(pinionComboLData);
					pinionCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					pinionCombo.setText("10");
					pinionCombo.setEnabled(false);
					pinionCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("pinionCombo.widgetSelected, event=" + evt);
							//TODO add your code for pinionCombo.widgetSelected
						}
					});
					pinionCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("pinionCombo.mouseMove, event=" + evt);
							//TODO add your code for pinionCombo.mouseMove
						}
					});
				}
				{
					mainGearLabel = new CLabel(mainConfigGroup, SWT.RIGHT);
					RowData mainGearLabelLData = new RowData();
					mainGearLabelLData.width = 80;
					mainGearLabelLData.height = 20;
					mainGearLabel.setLayoutData(mainGearLabelLData);
					mainGearLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					mainGearLabel.setText("main gear");
				}
				{
					mainGearCombo = new CCombo(mainConfigGroup, SWT.BORDER);
					mainGearCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					mainGearCombo.setText("45");
					RowData mainGearComboLData = new RowData();
					mainGearComboLData.width = 45;
					mainGearComboLData.height = 17;
					mainGearCombo.setLayoutData(mainGearComboLData);
					mainGearCombo.setEnabled(false);
					mainGearCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("mainGearCombo.mouseMove, event=" + evt);
							//TODO add your code for mainGearCombo.mouseMove
						}
					});
					mainGearCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("mainGearCombo.widgetSelected, event=" + evt);
							//TODO add your code for mainGearCombo.widgetSelected
						}
					});
				}
				{
					dotLabel = new CLabel(mainConfigGroup, SWT.NONE);
					dotLabel.setText(".");
					RowData dotLabelLData = new RowData();
					dotLabelLData.width = 8;
					dotLabelLData.height = 20;
					dotLabel.setLayoutData(dotLabelLData);
					dotLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				}
				{
					secondgearCombo = new CCombo(mainConfigGroup, SWT.BORDER);
					secondgearCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					secondgearCombo.setText("27");
					RowData secondgearComboLData = new RowData();
					secondgearComboLData.width = 45;
					secondgearComboLData.height = 17;
					secondgearCombo.setLayoutData(secondgearComboLData);
					secondgearCombo.setEnabled(false);
					secondgearCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("secondgearCombo.widgetSelected, event=" + evt);
							//TODO add your code for secondgearCombo.widgetSelected
						}
					});
					secondgearCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("secondgearCombo.mouseMove, event=" + evt);
							//TODO add your code for secondgearCombo.mouseMove
						}
					});
				}
				{
					motorPolsLabel = new CLabel(mainConfigGroup, SWT.NONE);
					RowData motoPolsLabelLData = new RowData();
					motoPolsLabelLData.width = 105;
					motoPolsLabelLData.height = 20;
					motorPolsLabel.setLayoutData(motoPolsLabelLData);
					motorPolsLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					motorPolsLabel.setText("motor pols");
				}
				{
					motorPolsCombo = new CCombo(mainConfigGroup, SWT.BORDER);
					RowData motorPolsComboLData = new RowData();
					motorPolsComboLData.width = 99;
					motorPolsComboLData.height = 17;
					motorPolsCombo.setLayoutData(motorPolsComboLData);
					motorPolsCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					motorPolsCombo.setText("2 ..32");
					motorPolsCombo.setToolTipText("Configure number of pols of brushless motor");
					motorPolsCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("motorPolsCombo.mouseMove, event=" + evt);
							//TODO add your code for motorPolsCombo.mouseMove
						}
					});
					motorPolsCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("motorPolsCombo.widgetSelected, event=" + evt);
							//TODO add your code for motorPolsCombo.widgetSelected
						}
					});
				}
				{
					motorShuntLabel = new CLabel(mainConfigGroup, SWT.RIGHT);
					RowData motorShuntLabelLData = new RowData();
					motorShuntLabelLData.width = 231;
					motorShuntLabelLData.height = 20;
					motorShuntLabel.setLayoutData(motorShuntLabelLData);
					motorShuntLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					motorShuntLabel.setText("current motor calibration");
				}
				{
					motorShuntCombo = new CCombo(mainConfigGroup, SWT.BORDER);
					RowData motorShuntComboLData = new RowData();
					motorShuntComboLData.width = 45;
					motorShuntComboLData.height = 17;
					motorShuntCombo.setLayoutData(motorShuntComboLData);
					motorShuntCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					motorShuntCombo.setText("-15");
					motorShuntCombo.setToolTipText("configure motor current calibriation by a virtual shunt");
					motorShuntCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("motorShuntCombo.widgetSelected, event=" + evt);
							//TODO add your code for motorShuntCombo.widgetSelected
						}
					});
					motorShuntCombo.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("motorShuntCombo.mouseMove, event=" + evt);
							//TODO add your code for motorShuntCombo.mouseMove
						}
					});
				}
				{
					percentLabel = new CLabel(mainConfigGroup, SWT.NONE);
					percentLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					percentLabel.setText(" % ");
				}
				{
					mainExplanationText = new Text(mainConfigGroup, SWT.BORDER);
					RowData mainExplanationTextLData = new RowData();
					mainExplanationTextLData.width = 640;
					mainExplanationTextLData.height = 80;
					mainExplanationText.setLayoutData(mainExplanationTextLData);
					mainExplanationText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					mainExplanationText.setText("explanation");
					mainExplanationText.setEditable(false);
					mainExplanationText.setBackground(SWTResourceManager.getColor(255, 255, 128));
				}
				{
					flagsLabel = new CLabel(mainConfigGroup, SWT.NONE);
					RowData flagsLabelLData = new RowData();
					flagsLabelLData.width = 93;
					flagsLabelLData.height = 20;
					flagsLabel.setLayoutData(flagsLabelLData);
					flagsLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					flagsLabel.setText("special flags");
				}
				{
					resetButton = new Button(mainConfigGroup, SWT.CHECK | SWT.LEFT);
					RowData rstButtonLData = new RowData();
					rstButtonLData.width = 130;
					rstButtonLData.height = 23;
					resetButton.setLayoutData(rstButtonLData);
					resetButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					resetButton.setText("Reset");
					resetButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.BOLD));
					resetButton.setToolTipText("delete log directory next log start");
					resetButton.setForeground(SWTResourceManager.getColor(255, 0, 0));
					resetButton.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("rstButton.mouseMove, event=" + evt);
							//TODO add your code for rstButton.mouseMove
						}
					});
					resetButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("rstButton.widgetSelected, event=" + evt);
							//TODO add your code for rstButton.widgetSelected
						}
					});
				}
				{
					highPulsWidthButton = new Button(mainConfigGroup, SWT.CHECK | SWT.LEFT);
					RowData hpwButtonLData = new RowData();
					hpwButtonLData.width = 130;
					hpwButtonLData.height = 23;
					highPulsWidthButton.setLayoutData(hpwButtonLData);
					highPulsWidthButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					highPulsWidthButton.setText("High PW warning");
					highPulsWidthButton.setToolTipText("hight puls width warning (Reduziere Gas in einem Governor-Mode)");
					highPulsWidthButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							System.out.println("hpwButton.widgetSelected, event=" + evt);
							//TODO add your code for hpwButton.widgetSelected
						}
					});
					highPulsWidthButton.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("hpwButton.mouseMove, event=" + evt);
							//TODO add your code for hpwButton.mouseMove
						}
					});
				}
				{
					logStopButton = new Button(mainConfigGroup, SWT.CHECK | SWT.LEFT);
					RowData logStopButtonLData = new RowData();
					logStopButtonLData.width = 130;
					logStopButtonLData.height = 23;
					logStopButton.setLayoutData(logStopButtonLData);
					logStopButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					logStopButton.setText("log stop");
					logStopButton.setToolTipText("Schalte LogStop ein/aus. Nicht wirksam in Firmware-Versionen mit Speed-Messungen aus GPS oder/und Prandtl Probe");
					logStopButton.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							System.out.println("logStopButton.mouseMove, event=" + evt);
							//TODO add your code for logStopButton.mouseMove
						}
					});
					logStopButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetDefaultSelected(SelectionEvent evt) {
							System.out.println("logStopButton.widgetDefaultSelected, event=" + evt);
							//TODO add your code for logStopButton.widgetDefaultSelected
						}
					});
				}
				{
					extRpmButton = new Button(mainConfigGroup, SWT.CHECK | SWT.LEFT);
					RowData extRpmButtonLData = new RowData();
					extRpmButtonLData.width = 130;
					extRpmButtonLData.height = 23;
					extRpmButton.setLayoutData(extRpmButtonLData);
					extRpmButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					extRpmButton.setText("ext. RPM");
					extRpmButton.setEnabled(false);
				}
			}
			this.layout();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void enableExternTempearture1(boolean isEnabled) {
		mpxAddresses[0].mpxAddressCombo.setEnabled(isEnabled);
	}

	private void enableExternTempeartureAll(boolean isEnabled) {
		for (MpxAddressComposite tmpMpxAddress : mpxAddresses) {
			tmpMpxAddress.mpxAddressCombo.setEnabled(isEnabled);
		}
	}

	private void setAdapterRequired(boolean isRequired) {
		if (isRequired)
			sensorAdapterButton.setForeground(DataExplorer.COLOR_RED);
		else
			sensorAdapterButton.setForeground(DataExplorer.COLOR_GREY);

	}

	private void setClearAlarms(boolean isEnabled) {
		if (isEnabled)
			alarmsClearButton.setForeground(DataExplorer.COLOR_RED);
		else
			alarmsClearButton.setForeground(DataExplorer.COLOR_GREY);

	}

}
