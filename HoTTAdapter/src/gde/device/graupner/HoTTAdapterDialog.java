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
    
    Copyright (c) 2011 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channels;
import gde.device.DeviceDialog;
import gde.device.graupner.hott.MessageIds;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

import java.util.ArrayList;
import java.util.List;
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
	final static Logger					log									= Logger.getLogger(HoTTAdapterDialog.class.getName());

	CTabFolder									tabFolder;
	CTabItem										serialComTabItem;
	Composite										configMainComosite;
	Button											saveButton, closeButton, helpButton;
	CLabel											timeZoneOffsetUTCLabel, timeZoneOffsetUTCUnit;
	CCombo											timeZoneOffsetUTCCombo;
	Button											inputFileButton;

	final HoTTAdapter						device;																																		// get device specific things, get serial port, ...
	final Settings							settings;																																	// application configuration settings
	final HoTTAdapterSerialPort	serialPort;																																// open/close port execute getData()....

	HoTTAdapterLiveGatherer			lifeGatherer;
	boolean											isVisibilityChanged	= false;

	int													measurementsCount		= 0;
	int													offsetTimeZone			= 0;
	final List<CTabItem>				configurations			= new ArrayList<CTabItem>();
	final String[]							deltaUTC						= { "  -12 ", "  -11 ", "  -10 ", "    -9 ", "    -8 ", "    -7 ", "    -6 ", "    -5 ", "    -4 ", "    -3 ", "    -2 ", "    -1 ", "     0 "
			, "   +1 ", "   +2 ", "   +3 ", "   +4 ", "   +5 ", "   +6 ", "   +7 ", "   +8 ", "   +9 ", " +10 ", " +11 ", " +12 " };

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

		this.offsetTimeZone = this.device.getUTCdelta();
	}

	@Override
	public void open() {
		try {
			this.shellAlpha = Settings.getInstance().getDialogAlphaValue();
			this.isAlphaEnabled = Settings.getInstance().isDeviceDialogAlphaEnabled();

			HoTTAdapterDialog.log.log(java.util.logging.Level.FINE, "dialogShell.isDisposed() " + ((this.dialogShell == null) ? "null" : this.dialogShell.isDisposed())); //$NON-NLS-1$ //$NON-NLS-2$
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
					@Override
					public void widgetDisposed(DisposeEvent evt) {
						HoTTAdapterDialog.log.log(java.util.logging.Level.FINEST, "dialogShell.widgetDisposed, event=" + evt); //$NON-NLS-1$
						if (HoTTAdapterDialog.this.device.isChangePropery()) {
							String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { HoTTAdapterDialog.this.device.getPropertiesFileName() });
							if (HoTTAdapterDialog.this.application.openYesNoMessageDialog(getDialogShell(), msg) == SWT.YES) {
								HoTTAdapterDialog.log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
								HoTTAdapterDialog.this.device.storeDeviceProperties();
								setClosePossible(true);
							}
						}
						HoTTAdapterDialog.this.dispose();
					}
				});
				this.dialogShell.addHelpListener(new HelpListener() {
					@Override
					public void helpRequested(HelpEvent evt) {
						HoTTAdapterDialog.log.log(java.util.logging.Level.FINER, "dialogShell.helpRequested, event=" + evt); //$NON-NLS-1$
						HoTTAdapterDialog.this.application.openHelpDialog("NMEA-Adapter", "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
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
							this.configurations.add(new HoTTAdapterDialogTabItem(this.tabFolder, this, (i + 1), this.device));
						}
					}
					{
						Button startLifeDataCapturing = new Button(this.dialogShell, SWT.None);
						FormData startCapturingButtonLData = new FormData();
						startCapturingButtonLData.height = GDE.IS_MAC ? 33 : 30;
						startCapturingButtonLData.left = new FormAttachment(0, 1000, 210);
						startCapturingButtonLData.width = 200;
						startCapturingButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -43 : -45);
						startLifeDataCapturing.setLayoutData(startCapturingButtonLData);
						startLifeDataCapturing.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						startLifeDataCapturing.setText(Messages.getString(MessageIds.GDE_MSGT2413));
						startLifeDataCapturing.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								HoTTAdapterDialog.log.log(java.util.logging.Level.FINEST, "startLifeDataCapturing.widgetSelected, event=" + evt); //$NON-NLS-1$
								try {
									int channelNumber = HoTTAdapterDialog.this.tabFolder.getSelectionIndex() + 1;
									switch (channelNumber) {
									default:
									case 1: //receiver
										HoTTAdapterDialog.this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER);
										break;
									case 2: //vario
										HoTTAdapterDialog.this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO);
										break;
									case 3: //GPS
										HoTTAdapterDialog.this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS);
										break;
									case 4: //general
										HoTTAdapterDialog.this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL);
										break;
									case 5: //electric
										HoTTAdapterDialog.this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC);
										break;
									}
									HoTTAdapterDialog.this.lifeGatherer = new HoTTAdapterLiveGatherer(HoTTAdapterDialog.this.application, HoTTAdapterDialog.this.device, HoTTAdapterDialog.this.serialPort,
											channelNumber, HoTTAdapterDialog.this);
									HoTTAdapterDialog.this.lifeGatherer.start();
								}
								catch (Exception e) {
									HoTTAdapterDialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
									HoTTAdapterDialog.this.serialPort.close();
								}
							}
						});
					}
					{
						Button stopLifeDataCapturing = new Button(this.dialogShell, SWT.None);
						FormData stopCapturingButtonLData = new FormData();
						stopCapturingButtonLData.height = GDE.IS_MAC ? 33 : 30;
						stopCapturingButtonLData.left = new FormAttachment(0, 1000, 210);
						stopCapturingButtonLData.width = 200;
						stopCapturingButtonLData.bottom = new FormAttachment(1000, 1000, GDE.IS_MAC ? -8 : -10);
						stopLifeDataCapturing.setLayoutData(stopCapturingButtonLData);
						stopLifeDataCapturing.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						stopLifeDataCapturing.setText(Messages.getString(MessageIds.GDE_MSGT2414));
						stopLifeDataCapturing.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								HoTTAdapterDialog.log.log(java.util.logging.Level.FINEST, "stopLifeDataCapturing.widgetSelected, event=" + evt); //$NON-NLS-1$
								HoTTAdapterDialog.this.lifeGatherer.stopTimerThread();
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
								HoTTAdapterDialog.log.log(java.util.logging.Level.FINEST, "inputFileButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								if (HoTTAdapterDialog.this.isVisibilityChanged) {
									String msg = Messages.getString(gde.messages.MessageIds.GDE_MSGI0041, new String[] { HoTTAdapterDialog.this.device.getPropertiesFileName() });
									if (HoTTAdapterDialog.this.application.openYesNoMessageDialog(HoTTAdapterDialog.this.getDialogShell(), msg) == SWT.YES) {
										HoTTAdapterDialog.log.log(java.util.logging.Level.FINE, "SWT.YES"); //$NON-NLS-1$
										HoTTAdapterDialog.this.device.storeDeviceProperties();
									}
								}
								HoTTAdapterDialog.this.device.open_closeCommPort();
							}
						});
					}
					{
						this.timeZoneOffsetUTCLabel = new CLabel(this.dialogShell, SWT.RIGHT);
						this.timeZoneOffsetUTCLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.timeZoneOffsetUTCLabel.setText(Messages.getString(MessageIds.GDE_MSGT2411));
						FormData timeZoneOffsetUTCLabelLData = new FormData();
						timeZoneOffsetUTCLabelLData.width = 100;
						timeZoneOffsetUTCLabelLData.height = 20;
						timeZoneOffsetUTCLabelLData.bottom = new FormAttachment(1000, 1000, -50);
						timeZoneOffsetUTCLabelLData.right = new FormAttachment(1000, 1000, -100);
						this.timeZoneOffsetUTCLabel.setLayoutData(timeZoneOffsetUTCLabelLData);
					}
					{
						this.timeZoneOffsetUTCCombo = new CCombo(this.dialogShell, SWT.RIGHT | SWT.BORDER);
						this.timeZoneOffsetUTCCombo.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.timeZoneOffsetUTCCombo.setItems(this.deltaUTC);
						FormData timeZoneOffsetUTCComboLData = new FormData();
						timeZoneOffsetUTCComboLData.width = 55;
						timeZoneOffsetUTCComboLData.height = 17;
						timeZoneOffsetUTCComboLData.bottom = new FormAttachment(1000, 1000, -50);
						timeZoneOffsetUTCComboLData.right = new FormAttachment(1000, 1000, -40);
						this.timeZoneOffsetUTCCombo.setLayoutData(timeZoneOffsetUTCComboLData);
						this.timeZoneOffsetUTCCombo.select(this.offsetTimeZone + 12);
						this.timeZoneOffsetUTCCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								HoTTAdapterDialog.log.log(java.util.logging.Level.FINEST, "timeZoneOffsetUTCCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
								HoTTAdapterDialog.this.device.setUTCdelta(HoTTAdapterDialog.this.offsetTimeZone = HoTTAdapterDialog.this.timeZoneOffsetUTCCombo.getSelectionIndex() - 12);
								HoTTAdapterDialog.this.saveButton.setEnabled(true);
							}
						});
					}
					{
						this.timeZoneOffsetUTCUnit = new CLabel(this.dialogShell, SWT.RIGHT);
						this.timeZoneOffsetUTCUnit.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.timeZoneOffsetUTCUnit.setText(Messages.getString(MessageIds.GDE_MSGT2412));
						FormData timeZoneOffsetUTCUnitLData = new FormData();
						timeZoneOffsetUTCUnitLData.width = 40;
						timeZoneOffsetUTCUnitLData.height = 20;
						timeZoneOffsetUTCUnitLData.bottom = new FormAttachment(1000, 1000, -50);
						timeZoneOffsetUTCUnitLData.right = new FormAttachment(1000, 1000, -10);
						this.timeZoneOffsetUTCUnit.setLayoutData(timeZoneOffsetUTCUnitLData);
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
								HoTTAdapterDialog.log.log(java.util.logging.Level.FINEST, "saveButton.widgetSelected, event=" + evt); //$NON-NLS-1$
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
								HoTTAdapterDialog.log.log(java.util.logging.Level.FINEST, "helpButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								HoTTAdapterDialog.this.application.openHelpDialog("NMEA-Adapter", "HelpInfo.html"); //$NON-NLS-1$ //$NON-NLS-2$
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
								HoTTAdapterDialog.log.log(java.util.logging.Level.FINEST, "closeButton.widgetSelected, event=" + evt); //$NON-NLS-1$
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
							HoTTAdapterDialog.log.log(java.util.logging.Level.FINEST, "configTabFolder.widgetSelected, event=" + evt); //$NON-NLS-1$
							int channelNumber = HoTTAdapterDialog.this.tabFolder.getSelectionIndex() + 1;
							switch (channelNumber) {
							default:
							case 1: //receiver
								HoTTAdapterDialog.this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_RECEIVER);
								break;
							case 2: //vario
								HoTTAdapterDialog.this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_VARIO);
								break;
							case 3: //GPS
								HoTTAdapterDialog.this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GPS);
								break;
							case 4: //general
								HoTTAdapterDialog.this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_GENERAL);
								break;
							case 5: //electric
								HoTTAdapterDialog.this.serialPort.setSensorType(HoTTAdapter.SENSOR_TYPE_ELECTRIC);
								break;
							}
							if (HoTTAdapterDialog.this.lifeGatherer != null && HoTTAdapterDialog.this.lifeGatherer.isAlive()) {
								try {
									HoTTAdapterDialog.this.lifeGatherer.stopTimerThread();
									HoTTAdapterDialog.this.lifeGatherer = new HoTTAdapterLiveGatherer(HoTTAdapterDialog.this.application, HoTTAdapterDialog.this.device, HoTTAdapterDialog.this.serialPort,
											channelNumber, HoTTAdapterDialog.this);
									HoTTAdapterDialog.this.lifeGatherer.start();
								}
								catch (Exception e) {
									HoTTAdapterDialog.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
									HoTTAdapterDialog.this.serialPort.close();
								}
							}
							//disable moving curves between configurations
							if (channelNumber > 0 && channelNumber <= HoTTAdapterDialog.this.device.getChannelCount()) { // enable other tabs for future use
								String configKey = channelNumber + " : " + ((CTabItem) evt.item).getText(); //$NON-NLS-1$
								Channels.getInstance().switchChannel(configKey);
								
//								Channels channels = Channels.getInstance();
//								Channel activeChannel = channels.getActiveChannel();
//								if (activeChannel != null) {
//									HoTTAdapterDialog.log.log(java.util.logging.Level.FINE, "activeChannel = " + activeChannel.getName() + " configKey = " + configKey); //$NON-NLS-1$ //$NON-NLS-2$
//									RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
//									if (activeRecordSet != null && activeChannel.getNumber() != channelNumber) {
//										int answer = HoTTAdapterDialog.this.application.openYesNoMessageDialog(getDialogShell(), Messages.getString(MessageIds.GDE_MSGI2400));
//										if (answer == SWT.YES) {
//											String recordSetKey = activeRecordSet.getName();
//											Channel tmpChannel = channels.get(channelNumber);
//											if (tmpChannel != null) {
//												HoTTAdapterDialog.log.log(java.util.logging.Level.FINE,	"move record set " + recordSetKey + " to channel/configuration " + channelNumber + GDE.STRING_BLANK_COLON_BLANK + configKey); //$NON-NLS-1$ //$NON-NLS-2$
//												tmpChannel.put(recordSetKey, activeRecordSet.clone(channelNumber));
//												activeChannel.remove(recordSetKey);
//												channels.switchChannel(channelNumber, recordSetKey);
//												RecordSet newActiveRecordSet = channels.get(channelNumber).getActiveRecordSet();
//												if (newActiveRecordSet != null) {
//													HoTTAdapterDialog.this.device.updateVisibilityStatus(newActiveRecordSet, false);
//													HoTTAdapterDialog.this.device.makeInActiveDisplayable(newActiveRecordSet);
//												}
//											}
//										}
//										HoTTAdapterDialog.this.application.updateCurveSelectorTable();
//									}
//								}
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
			Display display = this.dialogShell.getDisplay();
			while (!this.dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			HoTTAdapterDialog.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
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
}
