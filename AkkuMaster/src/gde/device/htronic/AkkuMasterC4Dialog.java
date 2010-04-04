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
****************************************************************************************/
package osde.device.htronic;

import java.util.HashMap;
import osde.log.Level;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import osde.DE;
import osde.config.Settings;
import osde.data.Channels;
import osde.device.DeviceDialog;
import osde.messages.Messages;
import osde.ui.DataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Dialog class for device AkkuMaster C4
 * @author Winfried Brügmann
 */
public class AkkuMasterC4Dialog extends DeviceDialog {
	final static Logger						log										= Logger.getLogger(AkkuMasterC4Dialog.class.getName());
	static final String						DEVICE_NAME						= "Akkumaster C4";
	static final String						AKKU_MASTER_HELP_DIR	= "AkkuMaster";

	Label													totalDischargeCurrentLabel;
	Label													totalChargeCurrentLabel;
	Label													totalDischargeCurrentText;
	Label													totalChargeCurrentText;
	Label													totalChargeCurrentUnit;
	Label													totalDischargeCurrentUnit;
	Composite											statusComposite;
	CLabel												versionFrontplateTypeLabel;
	CLabel												versionCurrentTypeLabel;
	CLabel												versionDateLabel;
	CLabel												versionNumberLabel;
	Text													versionFrontplateTypeText;
	Text													versionCurrentTypeText;
	Text													versionDateText;
	Text													versionNumberText;
	Composite											versionComposite;
	CTabItem											versionTabItem;
	CTabFolder										tabFolder;
	Button												closeButton;

	final Settings								settings;
	final AkkuMasterC4						device;
	final AkkuMasterC4SerialPort	serialPort;
	final DataExplorer	application;
	final Channels								channels;
	AkkuMasterChannelTab					channel1Tab, channel2Tab, channel3Tab, channel4Tab;

	int														totalDischargeCurrent	= 0000;			// mA
	int														totalChargeCurrent		= 0000;			// mA
	HashMap<String, String>				version;
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
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.numberChannels = actualDevice.getChannelCount();
		this.settings = Settings.getInstance();
	}

	public void open() {
		this.shellAlpha = Settings.getInstance().getDialogAlphaValue(); 
		this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

		AkkuMasterC4Dialog.log.log(Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.dialogShell == null || this.dialogShell.isDisposed()) {
			if (this.settings.isDeviceDialogsModal())
				this.dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
			else if (this.settings.isDeviceDialogsOnTop())
				this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM | SWT.ON_TOP);
			else
				this.dialogShell = new Shell(this.application.getDisplay(), SWT.DIALOG_TRIM);

			SWTResourceManager.registerResourceUser(this.dialogShell);
			if (this.isAlphaEnabled) this.dialogShell.setAlpha(254);
			this.dialogShell.setLayout(null);
			this.dialogShell.layout();
			this.dialogShell.pack();
			this.dialogShell.setSize(440, 555);
			this.dialogShell.setText(DEVICE_NAME + Messages.getString(osde.messages.MessageIds.OSDE_MSGT0273));
			this.dialogShell.setImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif"));
			this.dialogShell.addMouseTrackListener(new MouseTrackAdapter() {
				public void mouseEnter(MouseEvent evt) {
					log.log(Level.FINER, "dialogShell.mouseEnter, event=" + evt); //$NON-NLS-1$
					fadeOutAplhaBlending(evt, AkkuMasterC4Dialog.this.getDialogShell().getClientArea(), 10, 10, 10, 15);
				}
				public void mouseHover(MouseEvent evt) {
					log.log(Level.FINEST, "dialogShell.mouseHover, event=" + evt); //$NON-NLS-1$
				}
				public void mouseExit(MouseEvent evt) {
					log.log(Level.FINER, "dialogShell.mouseExit, event=" + evt); //$NON-NLS-1$
					fadeInAlpaBlending(evt, AkkuMasterC4Dialog.this.getDialogShell().getClientArea(), 10, 10, -10, 15);
				}
			});
			{
				this.tabFolder = new CTabFolder(this.dialogShell, SWT.NONE);
				this.tabFolder.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.tabFolder.setBounds(0, 0, 430, 425);

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
				if (this.numberChannels > 0) {
					this.channel1Tab = new AkkuMasterChannelTab(this,
							(" 1 " + this.device.getChannelName(1)), AkkuMasterC4SerialPort.channel_1, AkkuMasterC4Dialog.this.serialPort, this.channels.get(1), aCapacity, aCellCount, //$NON-NLS-1$
							aAkkuType, aProgramm, aChargeCurrent_mA, aDischargeCurrent_mA);
					this.channel1Tab.addChannelTab(this.tabFolder);
				}
				if (this.numberChannels > 1) {
					this.channel2Tab = new AkkuMasterChannelTab(this,
							(" 2 " + this.device.getChannelName(2)), AkkuMasterC4SerialPort.channel_2, AkkuMasterC4Dialog.this.serialPort, this.channels.get(2), aCapacity, aCellCount, //$NON-NLS-1$
							aAkkuType, aProgramm, aChargeCurrent_mA, aDischargeCurrent_mA);
					this.channel2Tab.addChannelTab(this.tabFolder);
				}
				if (this.numberChannels > 2) {
					this.channel3Tab = new AkkuMasterChannelTab(this,
							(" 3 " + this.device.getChannelName(3)), AkkuMasterC4SerialPort.channel_3, AkkuMasterC4Dialog.this.serialPort, this.channels.get(3), aCapacity, aCellCount, //$NON-NLS-1$
							aAkkuType, aProgramm, aChargeCurrent_mA, aDischargeCurrent_mA);
					this.channel3Tab.addChannelTab(this.tabFolder);
				}
				if (this.numberChannels > 3) {
					this.channel4Tab = new AkkuMasterChannelTab(this,
							(" 4 " + this.device.getChannelName(4)), AkkuMasterC4SerialPort.channel_4, AkkuMasterC4Dialog.this.serialPort, this.channels.get(4), aCapacity, aCellCount, //$NON-NLS-1$
							aAkkuType, aProgramm, aChargeCurrent_mA, aDischargeCurrent_mA);
					this.channel4Tab.addChannelTab(this.tabFolder);
				}
				///////////////////////////////////////////////////		

				{
					this.versionTabItem = new CTabItem(this.tabFolder, SWT.NONE);
					this.versionTabItem.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.versionTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT1109));
					{
						this.versionComposite = new Composite(this.tabFolder, SWT.NONE);
						this.versionComposite.setLayout(null);
						this.versionTabItem.setControl(this.versionComposite);
						this.versionComposite.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								AkkuMasterC4Dialog.log.log(Level.FINEST, "versionComposite.paintControl, event=" + evt); //$NON-NLS-1$
								if (AkkuMasterC4Dialog.this.version != null) {
									updateVersionText(
											String.format(":    %s", AkkuMasterC4Dialog.this.version.get(AkkuMasterC4SerialPort.VERSION_NUMBER)), //$NON-NLS-1$
											String.format(":    %s", AkkuMasterC4Dialog.this.version.get(AkkuMasterC4SerialPort.VERSION_DATE)), //$NON-NLS-1$
											String.format(":    %s", AkkuMasterC4Dialog.this.version.get(AkkuMasterC4SerialPort.VERSION_TYPE_CURRENT)), //$NON-NLS-1$
											String.format(":    %s", AkkuMasterC4Dialog.this.version.get(AkkuMasterC4SerialPort.VERSION_TYPE_FRONT))); //$NON-NLS-1$

								}
								else {
									updateVersionText(
											String.format(":    %s", Messages.getString(osde.messages.MessageIds.OSDE_MSGT0276)), //$NON-NLS-1$
											String.format(":    %s", Messages.getString(osde.messages.MessageIds.OSDE_MSGT0276)), //$NON-NLS-1$
											String.format(":    %s", Messages.getString(osde.messages.MessageIds.OSDE_MSGT0276)), //$NON-NLS-1$
											String.format(":    %s", Messages.getString(osde.messages.MessageIds.OSDE_MSGT0276))); //$NON-NLS-1$
									startUpdateVersionThread();
								}
							}
						});
						{
							this.versionNumberLabel = new CLabel(this.versionComposite, SWT.LEFT);
							this.versionNumberLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.versionNumberLabel.setBounds(25, 60, 150, 20);
							this.versionNumberLabel.setBackground(DataExplorer.COLOR_LIGHT_GREY);
							this.versionNumberLabel.setForeground(DataExplorer.COLOR_BLACK);
							this.versionNumberLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1122));
						}
						{
							this.versionNumberText = new Text(this.versionComposite, SWT.NONE);
							this.versionNumberText.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.versionNumberText.setBounds(230, 63, 100, 20);
							this.versionNumberText.setBackground(DataExplorer.COLOR_LIGHT_GREY);
							this.versionNumberText.setForeground(DataExplorer.COLOR_BLACK);
							this.versionNumberText.setEditable(false);
						}
						{
							this.versionDateLabel = new CLabel(this.versionComposite, SWT.LEFT);
							this.versionDateLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.versionDateLabel.setBounds(25, 110, 150, 20);
							this.versionDateLabel.setBackground(DataExplorer.COLOR_LIGHT_GREY);
							this.versionDateLabel.setForeground(DataExplorer.COLOR_BLACK);
							this.versionDateLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1123));
						}
						{
							this.versionDateText = new Text(this.versionComposite, SWT.NONE);
							this.versionDateText.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.versionDateText.setBounds(230, 113, 100, 20);
							this.versionDateText.setBackground(DataExplorer.COLOR_LIGHT_GREY);
							this.versionDateText.setForeground(DataExplorer.COLOR_BLACK);
							this.versionDateText.setEditable(false);
						}
						{
							this.versionCurrentTypeLabel = new CLabel(this.versionComposite, SWT.LEFT);
							this.versionCurrentTypeLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.versionCurrentTypeLabel.setBounds(25, 160, 150, 20);
							this.versionCurrentTypeLabel.setBackground(DataExplorer.COLOR_LIGHT_GREY);
							this.versionCurrentTypeLabel.setForeground(DataExplorer.COLOR_BLACK);
							this.versionCurrentTypeLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1124));
						}
						{
							this.versionCurrentTypeText = new Text(this.versionComposite, SWT.NONE);
							this.versionCurrentTypeText.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.versionCurrentTypeText.setBounds(230, 163, 100, 20);
							this.versionCurrentTypeText.setBackground(DataExplorer.COLOR_LIGHT_GREY);
							this.versionCurrentTypeText.setForeground(DataExplorer.COLOR_BLACK);
							this.versionCurrentTypeText.setEditable(false);
						}
						{
							this.versionFrontplateTypeLabel = new CLabel(this.versionComposite, SWT.LEFT);
							this.versionFrontplateTypeLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.versionFrontplateTypeLabel.setBounds(25, 210, 150, 20);
							this.versionFrontplateTypeLabel.setBackground(DataExplorer.COLOR_LIGHT_GREY);
							this.versionFrontplateTypeLabel.setForeground(DataExplorer.COLOR_BLACK);
							this.versionFrontplateTypeLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1125));
						}
						{
							this.versionFrontplateTypeText = new Text(this.versionComposite, SWT.NONE);
							this.versionFrontplateTypeText.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.versionFrontplateTypeText.setBounds(230, 213, 100, 20);
							this.versionFrontplateTypeText.setBackground(DataExplorer.COLOR_LIGHT_GREY);
							this.versionFrontplateTypeText.setForeground(DataExplorer.COLOR_BLACK);
							this.versionFrontplateTypeText.setEditable(false);
						}
					}
				}
				this.tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
			}
			{
				this.statusComposite = new Composite(this.dialogShell, SWT.NONE);
				this.statusComposite.setLayout(null);
				this.statusComposite.setBounds(0, 430, 430, 45);
				this.statusComposite.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						AkkuMasterC4Dialog.log.log(Level.FINEST, "statusComposite.widgetSelected, event=" + evt); //$NON-NLS-1$
						AkkuMasterC4Dialog.this.totalDischargeCurrentLabel.setText("" + AkkuMasterC4Dialog.this.totalDischargeCurrent);
						AkkuMasterC4Dialog.this.totalChargeCurrentLabel.setText("" + AkkuMasterC4Dialog.this.totalChargeCurrent);
					}
				});
				{
					this.totalChargeCurrentText = new Label(this.statusComposite, SWT.RIGHT);
					this.totalChargeCurrentText.setText(Messages.getString(MessageIds.OSDE_MSGT1110));
					this.totalChargeCurrentText.setBackground(DataExplorer.COLOR_LIGHT_GREY);
					this.totalChargeCurrentText.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.totalChargeCurrentText.setBounds(20, 3, 190, 20);
				}
				{
					this.totalChargeCurrentLabel = new Label(this.statusComposite, SWT.RIGHT | SWT.EMBEDDED);
					this.totalChargeCurrentLabel.setBounds(235, 3, 50, 20);
					this.totalChargeCurrentLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.totalChargeCurrentLabel.setText(new Double(this.totalChargeCurrent).toString());
					this.totalChargeCurrentLabel.setBackground(DataExplorer.COLOR_LIGHT_GREY);
				}
				{
					this.totalChargeCurrentUnit = new Label(this.statusComposite, SWT.LEFT);
					this.totalChargeCurrentUnit.setBackground(DataExplorer.COLOR_LIGHT_GREY);
					this.totalChargeCurrentUnit.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.totalChargeCurrentUnit.setText(Messages.getString(MessageIds.OSDE_MSGT1113));
					this.totalChargeCurrentUnit.setBounds(300, 3, 119, 20);
				}
				{
					this.totalDischargeCurrentText = new Label(this.statusComposite, SWT.RIGHT);
					this.totalDischargeCurrentText.setBackground(DataExplorer.COLOR_LIGHT_GREY);
					this.totalDischargeCurrentText.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.totalDischargeCurrentText.setText(Messages.getString(MessageIds.OSDE_MSGT1111));
					this.totalDischargeCurrentText.setBounds(20, 25, 190, 20);
				}
				{
					this.totalDischargeCurrentLabel = new Label(this.statusComposite, SWT.RIGHT | SWT.EMBEDDED);
					this.totalDischargeCurrentLabel.setBounds(235, 25, 50, 20);
					this.totalDischargeCurrentLabel.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.totalDischargeCurrentLabel.setText(new Double(this.totalDischargeCurrent).toString());
					this.totalDischargeCurrentLabel.setBackground(DataExplorer.COLOR_LIGHT_GREY);
				}
				{
					this.totalDischargeCurrentUnit = new Label(this.statusComposite, SWT.LEFT);
					this.totalDischargeCurrentUnit.setBackground(DataExplorer.COLOR_LIGHT_GREY);
					this.totalDischargeCurrentUnit.setText(Messages.getString(MessageIds.OSDE_MSGT1112));
					this.totalDischargeCurrentUnit.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.BOLD));
					this.totalDischargeCurrentUnit.setBounds(300, 25, 119, 20);
				}
			}
			{
				this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
				this.closeButton.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.closeButton.setText(Messages.getString(osde.messages.MessageIds.OSDE_MSGT0188));
				this.closeButton.setBounds(87, 485, 260, 30);
				this.closeButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						AkkuMasterC4Dialog.log.log(Level.FINEST, "closeButton.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (getChannelTab(1).isDataColletionActive() || getChannelTab(2).isDataColletionActive() || getChannelTab(3).isDataColletionActive() || getChannelTab(4).isDataColletionActive())
							setClosePossible(false);
						else
							setClosePossible(true);

						dispose();
					}
				});
			}
			this.dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					AkkuMasterC4Dialog.log.log(Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
					if (AkkuMasterC4Dialog.this.serialPort != null && AkkuMasterC4Dialog.this.serialPort.isConnected()) {
						AkkuMasterC4Dialog.this.serialPort.close();
					}
					Thread thread = AkkuMasterC4Dialog.this.versionThread;
					if (thread != null && thread.isAlive()) thread.interrupt();
				}
			});
			this.dialogShell.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					AkkuMasterC4Dialog.log.log(Level.FINEST, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
					AkkuMasterC4Dialog.this.application.openHelpDialog(AKKU_MASTER_HELP_DIR, "HelpInfo.html"); //$NON-NLS-1$
				}
			});
			this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x/2-220, 70));
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
			if (this.serialPort != null && !this.isWarnedConnectError) {
				if (this.versionThread == null || !this.versionThread.isAlive()) {
					this.versionThread = new Thread() {
						
						public void run() {
							try {
								if (!AkkuMasterC4Dialog.this.serialPort.isConnected()) {
									AkkuMasterC4Dialog.this.serialPort.open();
								}
								AkkuMasterC4Dialog.this.version = AkkuMasterC4Dialog.this.serialPort.getVersion();
								getDialogShell().getDisplay().asyncExec(new Runnable() {
									public void run() {
										AkkuMasterC4Dialog.this.versionComposite.redraw();
									}
								});
							}
							catch (Exception e) {
								AkkuMasterC4Dialog.this.application.openMessageDialog(AkkuMasterC4Dialog.this.getDialogShell(), Messages.getString(osde.messages.MessageIds.OSDE_MSGE0024, new Object[] {e.getClass().getSimpleName(), e.getMessage() } ));
								return;
							}
						}
					};
					try {
						this.versionThread.start();
					}
					catch (RuntimeException e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
				}
			}
		}
		catch (Exception e) {
			AkkuMasterC4Dialog.log.log(Level.WARNING, e.getMessage(), e);
			AkkuMasterC4Dialog.this.application.openMessageDialog(AkkuMasterC4Dialog.this.dialogShell, Messages.getString(osde.messages.MessageIds.OSDE_MSGE0025, new Object[] {e.getClass().getSimpleName(), e.getMessage() } ));
			this.isWarnedConnectError = true;
		}
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
	 * update the version text displayed in version tab
	 */
	void updateVersionText(String versionNumberText, String versionDateText, String versionTypeText, String versionPanelText) {
		this.versionNumberText.setText(versionNumberText);
		this.versionDateText.setText(versionDateText);
		this.versionCurrentTypeText.setText(versionTypeText);
		this.versionFrontplateTypeText.setText(versionPanelText);
	}

	/**
	 * @return the device
	 */
	public AkkuMasterC4 getDevice() {
		return this.device;
	}
	
	public void updateCurrentStatus() {
		DataExplorer.display.asyncExec(new Runnable() {
			public void run() {
					AkkuMasterC4Dialog.this.statusComposite.redraw();
			}
		});
	}
}
