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
package osde.device.htronic;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import osde.config.Settings;
import osde.data.Channels;
import osde.device.DeviceDialog;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Dialog class for device AkkuMaster C4
 * @author Winfried Brügmann
 */
public class AkkuMasterC4Dialog extends DeviceDialog {
	final static Logger						log										= Logger.getLogger(AkkuMasterC4Dialog.class.getName());
	static final String						DEVICE_NAME						= "Akkumaster C4";
	static final String						AKKU_MASTER_HELP_DIR	= "AkkuMaster";

	CLabel												totalDischargeCurrentLabel;
	CLabel												totalChargeCurrentLabel;
	Text													totalDischargeCurrentText;
	Text													totalChargeCurrentText;
	Text													totalChargeCurrentUnit;
	Text													totalDischargeCurrentUnit;
	Composite											statusComposite;
	Text													versionFrontplateType;
	Text													versionCurrentType;
	Text													versionDate;
	Text													versionNumber;
	Composite											versionComposite;
	CTabItem											versionTabItem;
	CTabFolder										tabFolder;
	Button												closeButton;

	final Settings								settings;
	final AkkuMasterC4						device;
	final AkkuMasterC4SerialPort	serialPort;
	final OpenSerialDataExplorer	application;
	final Channels								channels;
	AkkuMasterChannelTab					channel1Tab, channel2Tab, channel3Tab, channel4Tab;

	int														totalDischargeCurrent	= 0000;			// mA
	int														totalChargeCurrent		= 0000;			// mA
	HashMap<String, Object>				version;
	Thread												versionThread;
	final int											numberChannels;
	final int											maxCurrent						= 2000;			// [mA]
	
	boolean 											isWarnedConnectError = false;

	/**
	 * constructor initialize all variables required
	 * @param parent Shell
	 * @param actualDevice AkkuMasterC4 class implementation == IDevice
	 */
	public AkkuMasterC4Dialog(Shell parent, AkkuMasterC4 actualDevice) {
		super(parent);
		this.device = actualDevice;
		this.serialPort = actualDevice.getSerialPort();
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.numberChannels = actualDevice.getChannelCount();
		this.settings = Settings.getInstance();
	}

	public void open() {
		AkkuMasterC4Dialog.log.fine("dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.dialogShell == null || this.dialogShell.isDisposed()) {
			if (this.settings.isDeviceDialogsModal())
				this.dialogShell = new Shell(this.getApplication().getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
			else
				this.dialogShell = new Shell(getApplication().getDisplay(), SWT.DIALOG_TRIM);

			SWTResourceManager.registerResourceUser(this.dialogShell);
			if (this.isAlphaEnabled) this.dialogShell.setAlpha(254);
			this.dialogShell.setLayout(null);
			this.dialogShell.layout();
			this.dialogShell.pack();
			this.dialogShell.setSize(440, 590);
			this.dialogShell.setText(DEVICE_NAME + Messages.getString(osde.messages.MessageIds.OSDE_MSGT0273));
			this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif"));
			this.dialogShell.addMouseTrackListener(new MouseTrackAdapter() {
				public void mouseEnter(MouseEvent evt) {
					log.finer("dialogShell.mouseEnter, event=" + evt); //$NON-NLS-1$
					fadeOutAplhaBlending(evt, AkkuMasterC4Dialog.this.getDialogShell().getClientArea(), 10, 10, 10, 15);
				}
				public void mouseHover(MouseEvent evt) {
					log.finest("dialogShell.mouseHover, event=" + evt); //$NON-NLS-1$
				}
				public void mouseExit(MouseEvent evt) {
					log.finer("dialogShell.mouseExit, event=" + evt); //$NON-NLS-1$
					fadeInAlpaBlending(evt, AkkuMasterC4Dialog.this.getDialogShell().getClientArea(), 10, 10, -10, 15);
				}
			});
			{
				this.tabFolder = new CTabFolder(this.dialogShell, SWT.NONE);
				this.tabFolder.setBounds(0, 0, 430, 425);
//				this.tabFolder.addMouseTrackListener(new MouseTrackAdapter() {
//					public void mouseEnter(MouseEvent evt) {
//						log.finer("tabFolder.mouseEnter, event=" + evt); //$NON-NLS-1$
//						fadeOutAplhaBlending(evt, AkkuMasterC4Dialog.this.getDialogShell().getClientArea(), 10, 10, -10, -10);
//					}
//					public void mouseHover(MouseEvent evt) {
//						log.finest("tabFolder.mouseHover, event=" + evt); //$NON-NLS-1$
//					}
//					public void mouseExit(MouseEvent evt) {
//						log.finer("tabFolder.mouseExit, event=" + evt); //$NON-NLS-1$
//						fadeInAlpaBlending(evt, AkkuMasterC4Dialog.this.getDialogShell().getClientArea(), 10, 10, -10, -10);
//					}
//				});

				@SuppressWarnings("nls")
				String[] aCapacity = new String[] { "100", "250", "500", "600", "800", "1000", "1250", "1500", "1750", "2000", "2500", "3000", "4000", "5000" };
				String[] aCellCount = new String[] { "1"+Messages.getString(MessageIds.OSDE_MSGT1185), "2"+Messages.getString(MessageIds.OSDE_MSGT1148), "3"+Messages.getString(MessageIds.OSDE_MSGT1148), "4"+Messages.getString(MessageIds.OSDE_MSGT1148), "5"+Messages.getString(MessageIds.OSDE_MSGT1148), 
						"6"+Messages.getString(MessageIds.OSDE_MSGT1148), "7"+Messages.getString(MessageIds.OSDE_MSGT1148), "8"+Messages.getString(MessageIds.OSDE_MSGT1148), "9"+Messages.getString(MessageIds.OSDE_MSGT1148), "10"+Messages.getString(MessageIds.OSDE_MSGT1148), 
						"11"+Messages.getString(MessageIds.OSDE_MSGT1148), "12"+Messages.getString(MessageIds.OSDE_MSGT1148),	"13"+Messages.getString(MessageIds.OSDE_MSGT1148), "14"+Messages.getString(MessageIds.OSDE_MSGT1148) };
				String[] aAkkuType = new String[] { "0 NiCa", "1 NiMh", "2 Pb" };
				String[] aProgramm = new String[] { Messages.getString(MessageIds.OSDE_MSGT1100), Messages.getString(MessageIds.OSDE_MSGT1101), Messages.getString(MessageIds.OSDE_MSGT1102), Messages.getString(MessageIds.OSDE_MSGT1103), Messages.getString(MessageIds.OSDE_MSGT1104), Messages.getString(MessageIds.OSDE_MSGT1105), Messages.getString(MessageIds.OSDE_MSGT1106), Messages.getString(MessageIds.OSDE_MSGT1107),
						Messages.getString(MessageIds.OSDE_MSGT1108) }; 
				String[] aChargeCurrent_mA = new String[] { "50", "100", "150", "200", "250", "300", "400", "500", "750", "900", "1000", "1500", "2000" };
				String[] aDischargeCurrent_mA = aChargeCurrent_mA;

				///////////////////////////////////////////////////				
				if (this.channel1Tab == null && this.numberChannels > 0)
					this.channel1Tab = new AkkuMasterChannelTab(this, (" " + this.device.getChannelName(1)), AkkuMasterC4SerialPort.channel_1, this.getSerialPort(), this.channels.get(1), aCapacity, aCellCount, //$NON-NLS-1$
							aAkkuType, aProgramm, aChargeCurrent_mA, aDischargeCurrent_mA);
				this.channel1Tab.addChannelTab(this.tabFolder);

				if (this.channel2Tab == null && this.numberChannels > 1)
					this.channel2Tab = new AkkuMasterChannelTab(this, (" " + this.device.getChannelName(2)), AkkuMasterC4SerialPort.channel_2, this.getSerialPort(), this.channels.get(2), aCapacity, aCellCount, //$NON-NLS-1$
							aAkkuType, aProgramm, aChargeCurrent_mA, aDischargeCurrent_mA);
				this.channel2Tab.addChannelTab(this.tabFolder);

				if (this.channel3Tab == null && this.numberChannels > 2)
					this.channel3Tab = new AkkuMasterChannelTab(this, (" " + this.device.getChannelName(3)), AkkuMasterC4SerialPort.channel_3, this.getSerialPort(), this.channels.get(3), aCapacity, aCellCount, //$NON-NLS-1$
							aAkkuType, aProgramm, aChargeCurrent_mA, aDischargeCurrent_mA);
				this.channel3Tab.addChannelTab(this.tabFolder);

				if (this.channel4Tab == null && this.numberChannels > 3)
					this.channel4Tab = new AkkuMasterChannelTab(this, (" " + this.device.getChannelName(4)), AkkuMasterC4SerialPort.channel_4, this.getSerialPort(), this.channels.get(4), aCapacity, aCellCount, //$NON-NLS-1$
							aAkkuType, aProgramm, aChargeCurrent_mA, aDischargeCurrent_mA);
				this.channel4Tab.addChannelTab(this.tabFolder);
				///////////////////////////////////////////////////		

				{
					this.versionTabItem = new CTabItem(this.tabFolder, SWT.NONE);
					this.versionTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT1109));
					{
						this.versionComposite = new Composite(this.tabFolder, SWT.NONE);
						this.versionComposite.setLayout(null);
						this.versionTabItem.setControl(this.versionComposite);
						this.versionComposite.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								AkkuMasterC4Dialog.log.finest("versionComposite.paintControl, event=" + evt); //$NON-NLS-1$
								if (getVersion() != null) {
									updateVersionText(
											String.format("%-20s %s:   %s", AkkuMasterC4SerialPort.VERSION_NUMBER, "\t\t", getVersion().get(AkkuMasterC4SerialPort.VERSION_NUMBER)), //$NON-NLS-1$ //$NON-NLS-2$
											String.format("%-20s %s:   %s", AkkuMasterC4SerialPort.VERSION_DATE, "\t\t", getVersion().get(AkkuMasterC4SerialPort.VERSION_DATE)),  //$NON-NLS-1$ //$NON-NLS-2$
											String.format("%-20s %s:   %s", AkkuMasterC4SerialPort.VERSION_TYPE_CURRENT, "\t\t", getVersion().get(AkkuMasterC4SerialPort.VERSION_TYPE_CURRENT)),  //$NON-NLS-1$ //$NON-NLS-2$
											String.format("%-20s %s:   %s", AkkuMasterC4SerialPort.VERSION_TYPE_FRONT, "\t\t", getVersion().get(AkkuMasterC4SerialPort.VERSION_TYPE_FRONT)));  //$NON-NLS-1$ //$NON-NLS-2$

								}
								else {
									updateVersionText(
											String.format("%-20s %s:   %s", AkkuMasterC4SerialPort.VERSION_NUMBER, "\t\t", Messages.getString(osde.messages.MessageIds.OSDE_MSGT0276)), //$NON-NLS-1$ //$NON-NLS-2$
											String.format("%-20s %s:   %s", AkkuMasterC4SerialPort.VERSION_DATE, "\t\t", Messages.getString(osde.messages.MessageIds.OSDE_MSGT0276)),  //$NON-NLS-1$ //$NON-NLS-2$
											String.format("%-20s %s:   %s", AkkuMasterC4SerialPort.VERSION_TYPE_CURRENT, "\t\t", Messages.getString(osde.messages.MessageIds.OSDE_MSGT0276)),  //$NON-NLS-1$ //$NON-NLS-2$
											String.format("%-20s %s:   %s", AkkuMasterC4SerialPort.VERSION_TYPE_FRONT, "\t\t", Messages.getString(osde.messages.MessageIds.OSDE_MSGT0276)));  //$NON-NLS-1$ //$NON-NLS-2$
									startUpdateVersionThread();
								}
							}
						});
						{
							this.versionNumber = new Text(this.versionComposite, SWT.NONE);
							this.versionNumber.setBounds(24, 62, 288, 30);
							this.versionNumber.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.versionNumber.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
							this.versionNumber.setEditable(false);
						}
						{
							this.versionDate = new Text(this.versionComposite, SWT.NONE);
							this.versionDate.setBounds(24, 111, 288, 30);
							this.versionDate.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.versionDate.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
							this.versionDate.setEditable(false);
						}
						{
							this.versionCurrentType = new Text(this.versionComposite, SWT.NONE);
							this.versionCurrentType.setBounds(24, 159, 288, 30);
							this.versionCurrentType.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.versionCurrentType.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
							this.versionCurrentType.setEditable(false);
						}
						{
							this.versionFrontplateType = new Text(this.versionComposite, SWT.NONE);
							this.versionFrontplateType.setBounds(24, 212, 288, 30);
							this.versionFrontplateType.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							this.versionFrontplateType.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
							this.versionFrontplateType.setEditable(false);
						}
					}
				}
				this.tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
			}
			{
				this.statusComposite = new Composite(this.dialogShell, SWT.NONE);
				this.statusComposite.setLayout(null);
				this.statusComposite.setBounds(0, 430, 430, 65);
				this.statusComposite.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						AkkuMasterC4Dialog.log.finest("statusComposite.widgetSelected, event=" + evt); //$NON-NLS-1$
						setTotalDischargeCurrentLabelText(getTotalDischargeCurrent());
						setTotalChargeCurrentLabelText(getTotalChargeCurrent());
					}
				});
				{
					this.totalDischargeCurrentLabel = new CLabel(this.statusComposite, SWT.RIGHT | SWT.EMBEDDED);
					this.totalDischargeCurrentLabel.setBounds(235, 34, 50, 16);
					this.totalDischargeCurrentLabel.setText(new Double(this.totalDischargeCurrent).toString());
					this.totalDischargeCurrentLabel.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
				}
				{
					this.totalChargeCurrentLabel = new CLabel(this.statusComposite, SWT.RIGHT | SWT.EMBEDDED);
					this.totalChargeCurrentLabel.setBounds(235, 8, 50, 16);
					this.totalChargeCurrentLabel.setText(new Double(this.totalChargeCurrent).toString());
					this.totalChargeCurrentLabel.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
				}
				{
					this.totalChargeCurrentText = new Text(this.statusComposite, SWT.LEFT);
					this.totalChargeCurrentText.setText(Messages.getString(MessageIds.OSDE_MSGT1110));
					this.totalChargeCurrentText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					this.totalChargeCurrentText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false)); //$NON-NLS-1$
					this.totalChargeCurrentText.setBounds(20, 10, 190, 20);
				}
				{
					this.totalDischargeCurrentText = new Text(this.statusComposite, SWT.LEFT);
					this.totalDischargeCurrentText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					this.totalDischargeCurrentText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false)); //$NON-NLS-1$
					this.totalDischargeCurrentText.setText(Messages.getString(MessageIds.OSDE_MSGT1111));
					this.totalDischargeCurrentText.setBounds(20, 35, 190, 20);
				}
				{
					this.totalDischargeCurrentUnit = new Text(this.statusComposite, SWT.NONE);
					this.totalDischargeCurrentUnit.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					this.totalDischargeCurrentUnit.setText(Messages.getString(MessageIds.OSDE_MSGT1112));
					this.totalDischargeCurrentUnit.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false)); //$NON-NLS-1$
					this.totalDischargeCurrentUnit.setBounds(300, 10, 119, 20);
				}
				{
					this.totalChargeCurrentUnit = new Text(this.statusComposite, SWT.NONE);
					this.totalChargeCurrentUnit.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					this.totalChargeCurrentUnit.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false)); //$NON-NLS-1$
					this.totalChargeCurrentUnit.setText(Messages.getString(MessageIds.OSDE_MSGT1113));
					this.totalChargeCurrentUnit.setBounds(300, 35, 119, 20);
				}
			}
			this.dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					AkkuMasterC4Dialog.log.finest("dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
					if (getSerialPort() != null && getSerialPort().isConnected()) getSerialPort().close();
					if (getVersionThread() != null && getVersionThread().isAlive()) getVersionThread().interrupt();
				}
			});
			this.dialogShell.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					AkkuMasterC4Dialog.log.finest("dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
					getApplication().openHelpDialog(AKKU_MASTER_HELP_DIR, "HelpInfo.html"); //$NON-NLS-1$
				}
			});
			{
				this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				this.closeButton.setText(Messages.getString(osde.messages.MessageIds.OSDE_MSGT0188));
				this.closeButton.setBounds(82, 509, 260, 30);
				this.closeButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						AkkuMasterC4Dialog.log.finest("closeButton.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (getChannelTab(1).isDataColletionActive() || getChannelTab(2).isDataColletionActive() || getChannelTab(3).isDataColletionActive() || getChannelTab(4).isDataColletionActive())
							setClosePossible(false);
						else
							setClosePossible(true);

						dispose();
					}
				});
			}
			this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x/2-220, 100));
			this.dialogShell.open();
			startUpdateVersionThread();
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

	public void close() {
		if (this.dialogShell != null && !this.dialogShell.isDisposed()) {
			this.channel1Tab.stopTimer();
			this.channel2Tab.stopTimer();
			this.channel3Tab.stopTimer();
			this.channel4Tab.stopTimer();
			this.dialogShell.dispose();
		}
	}

	public boolean isDataColletionActive() {
		return this.channel1Tab.isCollectData() && this.channel1Tab.isCollectDataStopped() && this.channel2Tab.isCollectData() && this.channel2Tab.isCollectDataStopped()
				&& this.channel3Tab.isCollectData() && this.channel3Tab.isCollectDataStopped() && this.channel4Tab.isCollectData() && this.channel4Tab.isCollectDataStopped();
	}

	/**
	 * add current to discharge current sum
	 * @param newCurrent
	 */
	public void addTotalDischargeCurrent(int newCurrent) {
		this.totalDischargeCurrent = this.totalDischargeCurrent + newCurrent;
	}

	/**
	 * subtract current to discharge current sum
	 * @param newCurrent
	 */
	public void subtractTotalDischargeCurrent(int newCurrent) {
		this.totalDischargeCurrent = this.totalDischargeCurrent - newCurrent;
	}

	/**
	 * add current to charge current sum
	 * @param newCurrent
	 */
	public void addTotalChargeCurrent(int newCurrent) {
		this.totalChargeCurrent = this.totalChargeCurrent + newCurrent;
	}

	/**
	 * subtract current to charge current sum
	 * @param newCurrent
	 */
	public void subtractTotalChargeCurrent(int newCurrent) {
		this.totalChargeCurrent = this.totalChargeCurrent - newCurrent;
	}

	/**
	 * method to query sum of charge and discharge current in mA to enable overload check
	 */
	public int getActiveCurrent() {
		return this.totalChargeCurrent + this.totalDischargeCurrent;
	}

	public int getMaxCurrent() {
		return this.maxCurrent;
	}

	/**
	 * update version string 
	 */
	void startUpdateVersionThread() {
		try {
			if (this.getSerialPort() != null && !this.isWarnedConnectError) {
				if (!this.getSerialPort().isConnected()) {
					this.getSerialPort().open();
				}
				if (this.versionThread == null || !this.versionThread.isAlive()) {
					this.versionThread = new Thread() {
						public void run() {
							try {
								setVersion(getSerialPort().getVersion());
								getDialogShell().getDisplay().asyncExec(new Runnable() {
									public void run() {
										versionCompositeRedraw();
									}
								});
							}
							catch (Exception e) {
								getApplication().openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0024, new Object[] {e.getClass().getSimpleName(), e.getMessage() } ));
							}
						}
					};
					this.versionThread.start();
				}
			}
		}
		catch (Exception e) {
			AkkuMasterC4Dialog.log.log(Level.WARNING, e.getMessage(), e);
			this.getApplication().openMessageDialog(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0025, new Object[] {e.getClass().getSimpleName(), e.getMessage() } ));
			this.isWarnedConnectError = true;
		}
	}

	/**
	 * @return the application
	 */
	public OpenSerialDataExplorer getApplication() {
		return this.application;
	}

	/**
	 * @return the serialPort
	 */
	public AkkuMasterC4SerialPort getSerialPort() {
		return this.serialPort;
	}

	/**
	 * @return the version
	 */
	public HashMap<String, Object> getVersion() {
		return this.version;
	}

	/**
	 * @param newVersion the version to set
	 */
	public void setVersion(HashMap<String, Object> newVersion) {
		this.version = new HashMap<String, Object>(newVersion);
	}

	/**
	 * @return the channel1Tab
	 */
	public AkkuMasterChannelTab getChannelTab(int number) {
		AkkuMasterChannelTab channelTab;
		switch (number) {
		case 4:
			channelTab = this.channel4Tab;
			break;
		case 3:
			channelTab = this.channel3Tab;
			break;
		case 2:
			channelTab = this.channel2Tab;
			break;
		default:
		case 1:
			channelTab = this.channel1Tab;
			break;
		}
		return channelTab;
	}

	/**
	 * @param text the totalDischargeCurrentLabel to set
	 */
	public void setTotalDischargeCurrentLabelText(String text) {
		this.totalDischargeCurrentLabel.setText(text);
	}

	/**
	 * @return the totalDischargeCurrent
	 */
	public String getTotalDischargeCurrent() {
		return "" + this.totalDischargeCurrent; //$NON-NLS-1$
	}

	/**
	 * @param text the totalChargeCurrentLabel to set
	 */
	public void setTotalChargeCurrentLabelText(String text) {
		this.totalChargeCurrentLabel.setText(text);
	}

	/**
	 * @return the totalChargeCurrent
	 */
	public String getTotalChargeCurrent() {
		return "" + this.totalChargeCurrent; //$NON-NLS-1$
	}

	/**
	 * redraw the versionComposite
	 */
	public void versionCompositeRedraw() {
		this.versionComposite.redraw();
	}

	/**
	 * @return the versionThread
	 */
	public Thread getVersionThread() {
		return this.versionThread;
	}

	/**
	 * update the version text displayed in version tab
	 */
	void updateVersionText(String versionNumberText, String versionDateText, String versionTypeText, String versionPanelText) {
		this.versionNumber.setText(versionNumberText);
		this.versionDate.setText(versionDateText);
		this.versionCurrentType.setText(versionTypeText);
		this.versionFrontplateType.setText(versionPanelText);
	}

	/**
	 * @return the device
	 */
	public AkkuMasterC4 getDevice() {
		return this.device;
	}
}
