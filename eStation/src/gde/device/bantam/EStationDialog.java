package osde.device.bantam;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import osde.config.Settings;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.DeviceDialog;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * e-Station dialog implementation (902, BC6, BC610, BC8)
 * @author Winfried Brügmann
 */
public class EStationDialog extends DeviceDialog {
	final static Logger						log									= Logger.getLogger(EStationDialog.class.getName());

	Text													infoText;
	Button												okButton;
	Button												stopColletDataButton;
	Button												startCollectDataButton;

	Composite											boundsComposite;
	Point													boundsSize;
	Group													configGroup;
	Composite											composite1;
	Composite											composite2;
	Composite											composite3;

	CLabel												inputPowerLowCutOffLabel;
	CLabel												capacityCutOffLabel;
	CLabel												safetyTimerLabel;
	CLabel												tempCutOffLabel;
	CLabel												waitTimeLabel;
	CLabel												cellTypeLabel;
	CLabel												processingTimeLabel;

	CLabel												inputLowPowerCutOffText;
	CLabel												capacityCutOffText;
	CLabel												safetyTimerText;
	CLabel												tempCutOffText;
	CLabel												waitTimeText;
	CLabel												cellTypeText;
	CLabel												processingTimeText;

	CLabel												inputLowPowerCutOffUnit;
	CLabel												capacityCutOffUnit;
	CLabel												safetyTimerUnit;
	CLabel												tempCutOffUnit;
	CLabel												waitTimeUnit;
	CLabel												cellTypeUnit;
	CLabel												processingTimeUnit;

	String												inputLowPowerCutOff	= "11";
	String												capacityCutOff			= "5000";
	String												safetyTimer					= "120";
	String												tempCutOff					= "80";
	String												waitTime						= "5";
	String												cellType						= "";
	String												processingTime			= "0";

	HashMap<String, String>				configData					= new HashMap<String, String>();
	GathererThread								dataGatherThread;

	final eStation								device;																																		// get device specific things, get serial port, ...
	final EStationSerialPort			serialPort;																																// open/close port execute getData()....
	final OpenSerialDataExplorer	application;																																// interaction with application instance
	final Channels								channels;																																	// interaction with channels, source of all records
	final Settings								settings;																																	// application configuration settings

	/**
	* main method to test this dialog inside a shell 
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			eStation device = new eStationBC6("c:\\Documents and Settings\\user\\Application Data\\OpenSerialDataExplorer\\Geraete\\Htronic Akkumaster C4.ini");
			EStationDialog inst = new EStationDialog(shell, device);
			inst.open();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public EStationDialog(Shell parent, eStation useDevice) {
		super(parent);
		this.serialPort = useDevice.getSerialPort();
		this.device = useDevice;
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	public void open() {
		try {
			EStationDialog.log.fine("dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed()));
			if (this.dialogShell == null || this.dialogShell.isDisposed()) {
				if (this.settings.isDeviceDialogsModal()) 
					this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.TRANSPARENCY_ALPHA);
				else
					this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);

				SWTResourceManager.registerResourceUser(this.dialogShell);
				if(this.isAlphaOn) this.dialogShell.setAlpha(50); // TODO settings
				this.dialogShell.setLayout(new FormLayout());
				this.dialogShell.setText("e-Station BC6 ToolBox");
				this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif"));
				this.dialogShell.layout();
				this.dialogShell.pack();
				this.dialogShell.setSize(350, 465);
				{
					this.boundsComposite = new Composite(this.dialogShell, SWT.NONE);
					FormData boundsCompositeLData = new FormData();
					boundsCompositeLData.left = new FormAttachment(0, 1000, 0);
					boundsCompositeLData.right = new FormAttachment(1000, 1000, 0);
					boundsCompositeLData.top = new FormAttachment(0, 1000, 0);
					boundsCompositeLData.bottom = new FormAttachment(1000, 1000, 0);
					this.boundsComposite.setLayoutData(boundsCompositeLData);
					this.boundsComposite.setLayout(new FormLayout());
					this.boundsComposite.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							log.finer("boundsComposite.paintControl() " + evt);
//							if (EStationDialog.this.dataGatherThread != null && EStationDialog.this.dataGatherThread.isCollectDataStopped) {
//								EStationDialog.this.startCollectDataButton.setEnabled(false);
//								EStationDialog.this.stopColletDataButton.setEnabled(true);
//							}
//							else {
//								EStationDialog.this.startCollectDataButton.setEnabled(true);
//								EStationDialog.this.stopColletDataButton.setEnabled(false);
//							}
						}
					});
					{
						FormData infoTextLData = new FormData();
						infoTextLData.height = 70;
						infoTextLData.left = new FormAttachment(0, 1000, 12);
						infoTextLData.top = new FormAttachment(0, 1000, 20);
						infoTextLData.right = new FormAttachment(1000, 1000, -12);
						this.infoText = new Text(this.boundsComposite, SWT.MULTI | SWT.CENTER | SWT.WRAP);
						this.infoText.setLayoutData(infoTextLData);
						this.infoText.setText("Diese Gerät kann nur Daten lesen.\nDie Schnittstelle schliesst sich automatisch, wenn das Gerät 3 Minuten lang weder laden oder entladen signalisiert.");
						this.infoText.setEditable(false);
					}
					{
						FormData startCollectDataButtonLData = new FormData();
						startCollectDataButtonLData.height = 30;
						startCollectDataButtonLData.left = new FormAttachment(0, 1000, 12);
						startCollectDataButtonLData.top = new FormAttachment(0, 1000, 110);
						startCollectDataButtonLData.right = new FormAttachment(1000, 1000, -180);
						this.startCollectDataButton = new Button(this.boundsComposite, SWT.PUSH | SWT.CENTER);
						this.startCollectDataButton.setLayoutData(startCollectDataButtonLData);
						this.startCollectDataButton.setText("Start");
						this.startCollectDataButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								EStationDialog.log.finest("startCollectDataButton.widgetSelected, event=" + evt);
								try {
									if (Channels.getInstance().getActiveChannel() != null) {
										String channelConfigKey = Channels.getInstance().getActiveChannel().getName();
										EStationDialog.this.startCollectDataButton.setEnabled(false);
										EStationDialog.this.stopColletDataButton.setEnabled(true);
										EStationDialog.this.setClosePossible(false);
										EStationDialog.this.dataGatherThread = new GathererThread(EStationDialog.this.application, EStationDialog.this.device, EStationDialog.this.serialPort, channelConfigKey);
										EStationDialog.this.dataGatherThread.start();
									}
								}
								catch (Exception e) {
									if (EStationDialog.this.dataGatherThread != null && EStationDialog.this.dataGatherThread.isTimerRunning) {
										EStationDialog.this.dataGatherThread.stopTimerThread();
										EStationDialog.this.dataGatherThread.interrupt();
									}
									EStationDialog.this.application.updateGraphicsWindow();
									EStationDialog.this.application.openMessageDialog("Bei der Datenabfrage ist eine Fehler aufgetreten !\n" + e.getClass().getSimpleName() + " - " + e.getMessage());
									EStationDialog.this.resetButtons();
								}
							}
						});
					}
					{
						FormData stopColletDataButtonLData = new FormData();
						stopColletDataButtonLData.height = 30;
						stopColletDataButtonLData.left = new FormAttachment(0, 1000, 170);
						stopColletDataButtonLData.top = new FormAttachment(0, 1000, 110);
						stopColletDataButtonLData.right = new FormAttachment(1000, 1000, -12);
						this.stopColletDataButton = new Button(this.boundsComposite, SWT.PUSH | SWT.CENTER);
						this.stopColletDataButton.setLayoutData(stopColletDataButtonLData);
						this.stopColletDataButton.setText("Stop");
						this.stopColletDataButton.setEnabled(false);
						this.stopColletDataButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								EStationDialog.log.finest("stopColletDataButton.widgetSelected, event=" + evt);
								if (EStationDialog.this.dataGatherThread != null) {
									EStationDialog.this.dataGatherThread.stopTimerThread();
									EStationDialog.this.dataGatherThread.interrupt();
									
									if (Channels.getInstance().getActiveChannel() != null) {
											RecordSet activeRecordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
											if (activeRecordSet != null) {
												// active record set name == life gatherer record name
												EStationDialog.this.dataGatherThread.finalizeRecordSet(activeRecordSet.getName(), true);
											}
									}
								}
								EStationDialog.this.startCollectDataButton.setEnabled(true);
								EStationDialog.this.stopColletDataButton.setEnabled(false);
								EStationDialog.this.setClosePossible(true);
							}
						});
					}
					{
						FormData configGroupLData = new FormData();
						configGroupLData.height = 200;
						configGroupLData.left = new FormAttachment(0, 1000, 12);
						configGroupLData.top = new FormAttachment(0, 1000, 155);
						configGroupLData.right = new FormAttachment(1000, 1000, -12);
						this.configGroup = new Group(this.boundsComposite, SWT.NONE);
						RowLayout configGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						this.configGroup.setLayout(configGroupLayout);
						this.configGroup.setLayoutData(configGroupLData);
						this.configGroup.setText("globale Gerätekonfiguration");
						this.configGroup.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								EStationDialog.log.finest("configGroup.paintControl, event=" + evt);
								EStationDialog.this.inputLowPowerCutOffText.setText(EStationDialog.this.inputLowPowerCutOff);
								EStationDialog.this.capacityCutOffText.setText(EStationDialog.this.capacityCutOff);
								EStationDialog.this.safetyTimerText.setText(EStationDialog.this.safetyTimer);
								EStationDialog.this.tempCutOffText.setText(EStationDialog.this.tempCutOff);
								EStationDialog.this.waitTimeText.setText(EStationDialog.this.waitTime);
							}
						});
						{
							RowData composite1LData = new RowData();
							composite1LData.width = 190;
							composite1LData.height = 195;
							this.composite1 = new Composite(this.configGroup, SWT.NONE);
							FillLayout composite1Layout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
							this.composite1.setLayout(composite1Layout);
							this.composite1.setLayoutData(composite1LData);
							{
								this.inputPowerLowCutOffLabel = new CLabel(this.composite1, SWT.NONE);
								this.inputPowerLowCutOffLabel.setText("Versorgungabschaltspannung");
							}
							{
								this.capacityCutOffLabel = new CLabel(this.composite1, SWT.NONE);
								this.capacityCutOffLabel.setText("Kapazitätsladelimit");
							}
							{
								this.safetyTimerLabel = new CLabel(this.composite1, SWT.NONE);
								this.safetyTimerLabel.setText("Sicherheits Timer");
							}
							{
								this.tempCutOffLabel = new CLabel(this.composite1, SWT.NONE);
								this.tempCutOffLabel.setText("Temp Cut-Off");
							}
							{
								this.waitTimeLabel = new CLabel(this.composite1, SWT.NONE);
								this.waitTimeLabel.setText("Wartezeit");
							}
							{
								this.processingTimeLabel = new CLabel(this.composite1, SWT.NONE);
								this.processingTimeLabel.setText("Prozesszeit");
							}
							{
								this.cellTypeLabel = new CLabel(this.composite1, SWT.NONE);
								this.cellTypeLabel.setText("Zellentype");
							}
						}
						{
							RowData composite2LData = new RowData();
							composite2LData.width = 45;
							composite2LData.height = 195;
							this.composite2 = new Composite(this.configGroup, SWT.NONE);
							FillLayout composite2Layout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
							this.composite2.setLayout(composite2Layout);
							this.composite2.setLayoutData(composite2LData);
							{
								this.inputLowPowerCutOffText = new CLabel(this.composite2, SWT.NONE);
								this.inputLowPowerCutOffText.setText(this.inputLowPowerCutOff);
							}
							{
								this.capacityCutOffText = new CLabel(this.composite2, SWT.NONE);
								this.capacityCutOffText.setText(this.capacityCutOff);
							}
							{
								this.safetyTimerText = new CLabel(this.composite2, SWT.NONE);
								this.safetyTimerText.setText(this.safetyTimer);
							}
							{
								this.tempCutOffText = new CLabel(this.composite2, SWT.NONE);
								this.tempCutOffText.setText(this.tempCutOff);
							}
							{
								this.waitTimeText = new CLabel(this.composite2, SWT.NONE);
								this.waitTimeText.setText(this.waitTime);
							}
							{
								this.processingTimeText = new CLabel(this.composite2, SWT.NONE);
								this.processingTimeText.setText(this.processingTime);
							}
							{
								this.cellTypeText = new CLabel(this.composite2, SWT.NONE);
								this.cellTypeText.setText(this.cellType);
							}
						}
						{
							this.composite3 = new Composite(this.configGroup, SWT.NONE);
							FillLayout composite3Layout = new FillLayout(org.eclipse.swt.SWT.VERTICAL);
							RowData composite3LData = new RowData();
							composite3LData.width = 60;
							composite3LData.height = 195;
							this.composite3.setLayoutData(composite3LData);
							this.composite3.setLayout(composite3Layout);
							{
								this.inputLowPowerCutOffUnit = new CLabel(this.composite3, SWT.NONE);
								this.inputLowPowerCutOffUnit.setText("[V]");
							}
							{
								this.capacityCutOffUnit = new CLabel(this.composite3, SWT.NONE);
								this.capacityCutOffUnit.setText("[mAh]");
							}
							{
								this.safetyTimerUnit = new CLabel(this.composite3, SWT.NONE);
								this.safetyTimerUnit.setText("[min]");
							}
							{
								this.tempCutOffUnit = new CLabel(this.composite3, SWT.NONE);
								this.tempCutOffUnit.setText("[°C]");
							}
							{
								this.waitTimeUnit = new CLabel(this.composite3, SWT.NONE);
								this.waitTimeUnit.setText("[min]");
							}
							{
								this.processingTimeUnit = new CLabel(this.composite3, SWT.NONE);
								this.processingTimeUnit.setText("[HH:mm]");
							}
							{
								this.cellTypeUnit = new CLabel(this.composite3, SWT.NONE);
								this.cellTypeUnit.setText("");
							}
						}
					}
					{
						FormData okButtonLData = new FormData();
						okButtonLData.height = 30;
						okButtonLData.bottom = new FormAttachment(1000, 1000, -12);
						okButtonLData.left = new FormAttachment(0, 1000, 12);
						okButtonLData.right = new FormAttachment(1000, 1000, -12);
						this.okButton = new Button(this.boundsComposite, SWT.PUSH | SWT.CENTER);
						this.okButton.setLayoutData(okButtonLData);
						this.okButton.setText("Schliessen");
						this.okButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								EStationDialog.log.finest("okButton.widgetSelected, event=" + evt);
								close();
							}
						});
					}
					this.boundsComposite.addMouseTrackListener(new MouseTrackAdapter() {
						@Override
						public void mouseEnter(MouseEvent evt) {
							fadeOutAplhaBlending(evt, EStationDialog.this.boundsSize = EStationDialog.this.boundsComposite.getSize(), 10);
						}

						@Override
						public void mouseHover(MouseEvent evt) {
							EStationDialog.log.finest("boundsComposite.mouseHover, event=" + evt);
						}

						@Override
						public void mouseExit(MouseEvent evt) {
							fadeInAlpaBlending(evt, EStationDialog.this.boundsSize = EStationDialog.this.boundsComposite.getSize(), 10);
						}
					});
				} // end boundsComposite

				this.dialogShell.addFocusListener(new FocusAdapter() {
					@Override
					public void focusGained(FocusEvent evt) {
						EStationDialog.log.finer("dialogShell.focusGained, event=" + evt);
						// this is only placed in the focus listener as hint, do not forget place this query 
						if (EStationDialog.this.serialPort != null && !isFailedConnectionWarned()) {
							try {
								EStationDialog.this.configData = EStationDialog.this.device.getConfigurationValues(EStationDialog.this.configData, EStationDialog.this.serialPort.getData());//serialPort.getData());
								EStationDialog.this.inputLowPowerCutOffText.setText(EStationDialog.this.inputLowPowerCutOff = "" + EStationDialog.this.configData.get(eStation.CONFIG_IN_VOLTAGE_CUT_OFF));
								EStationDialog.this.capacityCutOffText.setText(EStationDialog.this.capacityCutOff = "" + EStationDialog.this.configData.get(eStation.CONFIG_SET_CAPASITY));
								EStationDialog.this.safetyTimerText.setText(EStationDialog.this.safetyTimer = "" + EStationDialog.this.configData.get(eStation.CONFIG_SAFETY_TIME));
								EStationDialog.this.tempCutOffText.setText(EStationDialog.this.tempCutOff = "" + EStationDialog.this.configData.get(eStation.CONFIG_EXT_TEMP_CUT_OFF));
								EStationDialog.this.waitTimeText.setText(EStationDialog.this.waitTime = "" + EStationDialog.this.configData.get(eStation.CONFIG_WAIT_TIME));
							}
							catch (Exception e) {
								EStationDialog.this.inputLowPowerCutOffText.setText(EStationDialog.this.inputLowPowerCutOff = "?");
								EStationDialog.this.capacityCutOffText.setText(EStationDialog.this.capacityCutOff = "?");
								EStationDialog.this.safetyTimerText.setText(EStationDialog.this.safetyTimer = "?");
								EStationDialog.this.tempCutOffText.setText(EStationDialog.this.tempCutOff = "?");
								EStationDialog.this.waitTimeText.setText(EStationDialog.this.waitTime = "?");
								EStationDialog.this.application.openMessageDialogAsync("Serieller Port Fehler : " + e.getClass().getSimpleName() + " - " + e.getMessage());
							}
						}
					}
				});
				this.dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						EStationDialog.log.finer("dialogShell.widgetDisposed, event=" + evt);
						//TODO check if some thing to do before exiting
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						EStationDialog.log.finer("dialogShell.helpRequested, event=" + evt);
						EStationDialog.this.application.openHelpDialog("Sample", "HelpInfo.html");
					}
				});
				this.dialogShell.setLocation(getParent().toDisplay(200, 100));
				this.dialogShell.open();
			}
			else {
				this.dialogShell.setVisible(true);
				this.dialogShell.setActive();
			}
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			EStationDialog.log.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	public void resetButtons() {
		this.startCollectDataButton.setEnabled(true);
		this.stopColletDataButton.setEnabled(false);
	}

	/**
	 * fade out alpha blending from 254 to the configured alpha value
	 * @param evt
	 * @param outherBoundSize
	 * @param gapLimit
	 */
	public void fadeOutAplhaBlending(MouseEvent evt, Point outherBoundSize, int gapLimit) {
		log.fine("boundsComposite.mouseEnter, event=" + evt);
		boolean isEnterShellEvt = (evt.x < gapLimit || evt.x > outherBoundSize.x - gapLimit || evt.y < gapLimit || evt.y > outherBoundSize.y - gapLimit) ? true : false;
		log.fine("isEnterShellEvt = " + isEnterShellEvt + " size = " + outherBoundSize);
		if (isEnterShellEvt && isAlphaOn()) {
			setShellAlpha(254);
		}
	}

	/**
	 * fade in alpha blending the configured alpha value to 254
	 * @param evt
	 * @param outherBoundSize
	 * @param gapLimit
	 */
	public void fadeInAlpaBlending(MouseEvent evt, Point outherBoundSize, int gapLimit) {
		log.fine("boundsComposite.mouseExit, event=" + evt);
		EStationDialog.this.boundsSize = EStationDialog.this.boundsComposite.getSize();
		boolean isExitShellEvt = (evt.x < gapLimit || evt.x > outherBoundSize.x - gapLimit || evt.y < gapLimit || evt.y > outherBoundSize.y - gapLimit) ? true : false;
		log.fine("isExitShellEvt = " + isExitShellEvt + " size = " + outherBoundSize);
		if (isExitShellEvt && isAlphaOn()) {
			setShellAlpha(getShellAlpha());
		}
	}
}
