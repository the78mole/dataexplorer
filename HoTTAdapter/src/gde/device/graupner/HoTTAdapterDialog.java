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
    
    Copyright (c) 2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channels;
import gde.device.DeviceDialog;
import gde.device.graupner.hott.MessageIds;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog class to enable visualization control
 * @author Winfried Brügmann
 */
public class HoTTAdapterDialog extends DeviceDialog {
	final static Logger					log										= Logger.getLogger(HoTTAdapterDialog.class.getName());

	CTabFolder									tabFolder;
	CTabItem										serialComTabItem;
	Composite										configMainComosite;
	Button											saveButton, closeButton, helpButton;
	CLabel											protocolTypesLabel, protocolTypesUnitLabel;
	CCombo											protocolTypesCombo;
	Button											inputFileButton;
	Button											startLifeDataCapturing, stopLifeDataCapturing;

	final HoTTAdapter						device;																																			// get device specific things, get serial port, ...
	final Settings							settings;																																		// application configuration settings
	final HoTTAdapterSerialPort	serialPort;																																	// open/close port execute getData()....

	HoTTAdapterLiveGatherer			lifeGatherer;
	boolean											isVisibilityChanged		= false;

	int													measurementsCount			= 0;
	int 												protocolTypeOrdinal				= 0;

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param useDevice device specific class implementation
	 */
	public HoTTAdapterDialog(Shell parent, HoTTAdapter useDevice) {
		super(parent);
		this.device = useDevice;
		this.serialPort = useDevice.getCommunicationPort();
		this.settings = Settings.getInstance();
		for (int i = 1; i <= this.device.getChannelCount(); i++) {
			int actualMeasurementCount = this.device.getMeasurementNames(i).length;
			this.measurementsCount = actualMeasurementCount > this.measurementsCount ? actualMeasurementCount : this.measurementsCount;
		}
		this.protocolTypeOrdinal = this.device.getBaudeRate() == 115200 ? HoTTAdapter.Protocol.TYPE_115200.ordinal() : HoTTAdapter.Protocol.TYPE_19200_N.ordinal(); 
		this.serialPort.setProtocolType(HoTTAdapter.Protocol.values()[this.protocolTypeOrdinal]);
	}

	@Override
	public void open() {
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue();
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			log.log(java.util.logging.Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
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
				//dialogShell.pack();
				this.dialogShell.setSize(620, (30 + 25 + this.measurementsCount * 28 + 40 + 80) / 2); //header + tab + label + this.measurementsCount * 23 + loadButton + save/close buttons
				this.dialogShell.setText(this.device.getName() + Messages.getString(gde.messages.MessageIds.GDE_MSGT0273));
				this.dialogShell.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.dialogShell.setImage(SWTResourceManager.getImage("gde/resource/ToolBoxHot.gif")); //$NON-NLS-1$
				this.dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (HoTTAdapterDialog.this.device.isChangePropery()) {
							String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { HoTTAdapterDialog.this.device.getPropertiesFileName() });
							if (HoTTAdapterDialog.this.application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
								HoTTAdapterDialog.this.device.storeDeviceProperties();
								setClosePossible(true);
							}
						}
						HoTTAdapterDialog.this.dispose();
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						log.log(java.util.logging.Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						HoTTAdapterDialog.this.application.openHelpDialog("HoTTAdapter", "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				// enable fade in/out alpha blending (do not fade-in on top)
				//				this.dialogShell.addMouseTrackListener(new MouseTrackAdapter() {
				//					@Override
				//					public void mouseEnter(MouseEvent evt) {
				//						log.log(java.util.logging.Level.FINER, "dialogShell.mouseEnter, event=" + evt); //$NON-NLS-1$
				//						fadeOutAplhaBlending(evt, getDialogShell().getClientArea(), 20, 20, 20, 25);
				//					}
				//
				//					@Override
				//					public void mouseHover(MouseEvent evt) {
				//						log.log(java.util.logging.Level.FINEST, "dialogShell.mouseHover, event=" + evt); //$NON-NLS-1$
				//					}
				//
				//					@Override
				//					public void mouseExit(MouseEvent evt) {
				//						log.log(java.util.logging.Level.FINER, "dialogShell.mouseExit, event=" + evt); //$NON-NLS-1$
				//						fadeInAlpaBlending(evt, getDialogShell().getClientArea(), 20, 20, -20, 25);
				//					}
				//				});
				{
					this.tabFolder = new CTabFolder(this.dialogShell, SWT.NONE);

					{
						for (int i = 0; i < this.device.getChannelCount(); i++) {
							new HoTTAdapterDialogTabItem(this.tabFolder, this, (i + 1), this.device);
						}
					}
					{
						this.startLifeDataCapturing = new Button(this.dialogShell, SWT.None);
						FormData startCapturingButtonLData = new FormData();
						startCapturingButtonLData.height = GDE.IS_MAC ? 33 : 30;
						startCapturingButtonLData.left = new FormAttachment(0, 1000, 210);
						startCapturingButtonLData.width = 200;
						startCapturingButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -43 : -45);
						this.startLifeDataCapturing.setLayoutData(startCapturingButtonLData);
						this.startLifeDataCapturing.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.startLifeDataCapturing.setText(Messages.getString(MessageIds.GDE_MSGT2413));
						this.startLifeDataCapturing.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(java.util.logging.Level.FINEST, "startLifeDataCapturing.widgetSelected, event=" + evt); //$NON-NLS-1$
								try {
									HoTTAdapterDialog.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, Messages.getString(MessageIds.GDE_MSGT2404), Messages.getString(MessageIds.GDE_MSGT2404));
									HoTTAdapterDialog.this.lifeGatherer = new HoTTAdapterLiveGatherer(HoTTAdapterDialog.this.application, HoTTAdapterDialog.this.device, HoTTAdapterDialog.this.serialPort,
											HoTTAdapterDialog.this);
									HoTTAdapterDialog.this.lifeGatherer.start();
									HoTTAdapterDialog.this.startLifeDataCapturing.setEnabled(false);
									HoTTAdapterDialog.this.stopLifeDataCapturing.setEnabled(true);
									HoTTAdapterDialog.this.protocolTypesCombo.setEnabled(false);
									HoTTAdapterDialog.this.inputFileButton.setEnabled(false);
								}
								catch (Exception e) {
									log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
									HoTTAdapterDialog.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2404), Messages.getString(MessageIds.GDE_MSGT2404));
									HoTTAdapterDialog.this.serialPort.close();
									HoTTAdapterDialog.this.startLifeDataCapturing.setEnabled(true);
									HoTTAdapterDialog.this.stopLifeDataCapturing.setEnabled(false);
									HoTTAdapterDialog.this.protocolTypesCombo.setEnabled(true);
									HoTTAdapterDialog.this.inputFileButton.setEnabled(true);
								}
							}
						});
					}
					{
						this.stopLifeDataCapturing = new Button(this.dialogShell, SWT.None);
						FormData stopCapturingButtonLData = new FormData();
						stopCapturingButtonLData.height = GDE.IS_MAC ? 33 : 30;
						stopCapturingButtonLData.left = new FormAttachment(0, 1000, 210);
						stopCapturingButtonLData.width = 200;
						stopCapturingButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
						this.stopLifeDataCapturing.setLayoutData(stopCapturingButtonLData);
						this.stopLifeDataCapturing.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.stopLifeDataCapturing.setText(Messages.getString(MessageIds.GDE_MSGT2414));
						this.stopLifeDataCapturing.setEnabled(false);
						this.stopLifeDataCapturing.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(java.util.logging.Level.FINEST, "stopLifeDataCapturing.widgetSelected, event=" + evt); //$NON-NLS-1$
								HoTTAdapterDialog.this.serialPort.isInterruptedByUser = true;
							}
						});
					}
					{
						this.inputFileButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
						FormData inputFileButtonLData = new FormData();
						inputFileButtonLData.height = GDE.IS_MAC ? 33 : 30;
						inputFileButtonLData.left = new FormAttachment(0, 1000, 10);
						inputFileButtonLData.width = 160;
						inputFileButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -43 : -45);
						this.inputFileButton.setLayoutData(inputFileButtonLData);
						this.inputFileButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.inputFileButton.setText(Messages.getString(MessageIds.GDE_MSGT2402));
						this.inputFileButton.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2410));
						this.inputFileButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(java.util.logging.Level.FINEST, "inputFileButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								if (HoTTAdapterDialog.this.isVisibilityChanged) {
									String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { HoTTAdapterDialog.this.device.getPropertiesFileName() });
									if (HoTTAdapterDialog.this.application.openYesNoMessageDialog(HoTTAdapterDialog.this.getDialogShell(), msg) == SWT.YES) {
										log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
										HoTTAdapterDialog.this.device.storeDeviceProperties();
									}
								}
								HoTTAdapterDialog.this.device.open_closeCommPort();
							}
						});
					}
					{
						this.protocolTypesLabel = new CLabel(this.dialogShell, SWT.RIGHT);
						this.protocolTypesLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.protocolTypesLabel.setText(Messages.getString(MessageIds.GDE_MSGT2411));
						this.protocolTypesLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2415));
						FormData timeZoneOffsetUTCLabelLData = new FormData();
						timeZoneOffsetUTCLabelLData.width = 80;
						timeZoneOffsetUTCLabelLData.height = 20;
						timeZoneOffsetUTCLabelLData.bottom = new FormAttachment(1000, 1000, -50);
						timeZoneOffsetUTCLabelLData.right = new FormAttachment(1000, 1000, -125);
						this.protocolTypesLabel.setLayoutData(timeZoneOffsetUTCLabelLData);
					}
					{
						this.protocolTypesCombo = new CCombo(this.dialogShell, SWT.RIGHT | SWT.BORDER);
						this.protocolTypesCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.protocolTypesCombo.setItems(HoTTAdapter.Protocol.valuesAsStingArray());
						FormData timeZoneOffsetUTCComboLData = new FormData();
						timeZoneOffsetUTCComboLData.width = 75;
						timeZoneOffsetUTCComboLData.height = 17;
						timeZoneOffsetUTCComboLData.bottom = new FormAttachment(1000, 1000, -50);
						timeZoneOffsetUTCComboLData.right = new FormAttachment(1000, 1000, -40);
						this.protocolTypesCombo.setLayoutData(timeZoneOffsetUTCComboLData);
						this.protocolTypesCombo.select(this.protocolTypeOrdinal);
						this.protocolTypesCombo.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2415));
						this.protocolTypesCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(java.util.logging.Level.FINEST, "timeZoneOffsetUTCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								HoTTAdapterDialog.this.device.setBaudeRate(Integer.parseInt(HoTTAdapterDialog.this.protocolTypesCombo.getText().trim().split(GDE.STRING_BLANK)[0]));
								HoTTAdapterDialog.this.protocolTypeOrdinal = HoTTAdapterDialog.this.protocolTypesCombo.getSelectionIndex();
								HoTTAdapterDialog.this.serialPort.setProtocolType(HoTTAdapter.Protocol.values()[HoTTAdapterDialog.this.protocolTypeOrdinal]);
								HoTTAdapterDialog.this.saveButton.setEnabled(true);
							}
						});
					}
					{
						this.protocolTypesUnitLabel = new CLabel(this.dialogShell, SWT.RIGHT);
						this.protocolTypesUnitLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.protocolTypesUnitLabel.setText(Messages.getString(MessageIds.GDE_MSGT2412));
						this.protocolTypesUnitLabel.setToolTipText(Messages.getString(MessageIds.GDE_MSGT2415));
						FormData timeZoneOffsetUTCUnitLData = new FormData();
						timeZoneOffsetUTCUnitLData.width = 35;
						timeZoneOffsetUTCUnitLData.height = 20;
						timeZoneOffsetUTCUnitLData.bottom = new FormAttachment(1000, 1000, -50);
						timeZoneOffsetUTCUnitLData.right = new FormAttachment(1000, 1000, -3);
						this.protocolTypesUnitLabel.setLayoutData(timeZoneOffsetUTCUnitLData);
					}
					{
						this.saveButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
						FormData saveButtonLData = new FormData();
						saveButtonLData.width = 105;
						saveButtonLData.height = GDE.IS_MAC ? 33 : 30;
						saveButtonLData.left = new FormAttachment(0, 1000, 10);
						saveButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
						this.saveButton.setLayoutData(saveButtonLData);
						this.saveButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.saveButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0486));
						this.saveButton.setEnabled(false);
						this.saveButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(java.util.logging.Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								HoTTAdapterDialog.this.device.storeDeviceProperties();
								HoTTAdapterDialog.this.saveButton.setEnabled(false);
							}
						});
					}
					{
						this.helpButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
						FormData helpButtonLData = new FormData();
						helpButtonLData.width = GDE.IS_MAC ? 50 : 40;
						helpButtonLData.height = GDE.IS_MAC ? 33 : 30;
						helpButtonLData.left = new FormAttachment(0, 1000, GDE.IS_MAC ? 129 : 132);
						helpButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
						this.helpButton.setLayoutData(helpButtonLData);
						this.helpButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.helpButton.setImage(SWTResourceManager.getImage("gde/resource/QuestionHot.gif")); //$NON-NLS-1$
						this.helpButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(java.util.logging.Level.FINEST, "helpButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								HoTTAdapterDialog.this.application.openHelpDialog("HoTTAdapter", "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
							}
						});
					}
					{
						this.closeButton = new Button(this.dialogShell, SWT.PUSH | SWT.CENTER);
						FormData closeButtonLData = new FormData();
						closeButtonLData.width = 160;
						closeButtonLData.height = GDE.IS_MAC ? 33 : 30;
						closeButtonLData.right = new FormAttachment(1000, 1000, -10);
						closeButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
						this.closeButton.setLayoutData(closeButtonLData);
						this.closeButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.closeButton.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0485));
						this.closeButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(java.util.logging.Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								HoTTAdapterDialog.this.dialogShell.dispose();
							}
						});
					}
					FormData tabFolderLData = new FormData();
					tabFolderLData.top = new FormAttachment(0, 1000, 0);
					tabFolderLData.left = new FormAttachment(0, 1000, 0);
					tabFolderLData.right = new FormAttachment(1000, 1000, 0);
					tabFolderLData.bottom = new FormAttachment(1000, 1000, -80);
					this.tabFolder.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					this.tabFolder.setLayoutData(tabFolderLData);
					this.tabFolder.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							log.log(java.util.logging.Level.FINEST, "configTabFolder.widgetSelected, event=" + evt); //$NON-NLS-1$
							int channelNumber = HoTTAdapterDialog.this.tabFolder.getSelectionIndex() + 1;
							//disable moving curves between configurations
							if (channelNumber > 0 && channelNumber <= HoTTAdapterDialog.this.device.getChannelCount()) { // enable other tabs for future use
								String configKey = channelNumber + " : " + ((CTabItem) evt.item).getText(); //$NON-NLS-1$
								Channels.getInstance().switchChannel(configKey);
							}
						}
					});
				}

				try {
					this.tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
				}
				catch (RuntimeException e) {
					this.tabFolder.setSelection(0);
				}

				this.dialogShell.setLocation(getParent().toDisplay(getParent().getSize().x / 2 - 175, 100));
				this.dialogShell.open();
			}
			else {
				this.dialogShell.setVisible(true);
				this.dialogShell.setActive();
			}
			if (this.serialPort.isConnected()) { // check serial port state, if user has closed the dialog while gathering data
				this.startLifeDataCapturing.setEnabled(false);
				this.stopLifeDataCapturing.setEnabled(true);
				this.protocolTypesCombo.setEnabled(false);
				this.inputFileButton.setEnabled(false);
			}
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
		return this.tabFolder.getSelectionIndex();
	}

	/**
	 * reset the button states
	 */
	public void resetButtons() {
		if (this.dialogShell != null && !this.dialogShell.isDisposed()) {
			this.dialogShell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					HoTTAdapterDialog.this.startLifeDataCapturing.setEnabled(true);
					HoTTAdapterDialog.this.stopLifeDataCapturing.setEnabled(false);
					HoTTAdapterDialog.this.protocolTypesCombo.setEnabled(true);
					HoTTAdapterDialog.this.inputFileButton.setEnabled(true);
				}
			});
		}
	}

	/**
	 * switch to the tab when sensor is detected
	 * @param index
	 */
	public void selectTab(final int index) {
		if (HoTTAdapterDialog.this.tabFolder != null && !HoTTAdapterDialog.this.tabFolder.isDisposed()) {
			this.dialogShell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					HoTTAdapterDialog.this.tabFolder.setSelection(index - 1);
				}
			});
		}
	}
}
